package id.biz.yuka.performax.ui.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import id.biz.yuka.libperformx.entity.PayrollType
import id.biz.yuka.libperformx.entity.SalaryDetailEntity
import java.text.NumberFormat
import java.util.Locale

/**
 * Generator PDF Slip Gaji menggunakan android.graphics.pdf.PdfDocument (tanpa library eksternal).
 *
 * Dipakai untuk:
 * - [generateSlipGaji] → Slip pribadi 1 karyawan
 * - [generateRekapSlipGaji] → Rekap semua karyawan (multi-halaman)
 */
object SalaryPdfGenerator {

    // ─── Ukuran Halaman A4 (landscape jika rekap, portrait untuk slip) ───────
    private const val PAGE_WIDTH = 595   // A4 portrait width di 72dpi
    private const val PAGE_HEIGHT = 842  // A4 portrait height di 72dpi
    private const val MARGIN = 40f

    // ─── Palet Warna ─────────────────────────────────────────────────────────
    private val COLOR_PRIMARY = Color.parseColor("#38B29C")    // Teal (sama dgn app)
    private val COLOR_ORANGE  = Color.parseColor("#FF8F00")
    private val COLOR_RED     = Color.parseColor("#E57373")
    private val COLOR_DARK    = Color.parseColor("#1A1A2E")
    private val COLOR_GRAY    = Color.parseColor("#757575")
    private val COLOR_LIGHT   = Color.parseColor("#F5F5F5")
    private val COLOR_WHITE   = Color.WHITE
    private val COLOR_DIVIDER = Color.parseColor("#E0E0E0")
    private val COLOR_INCOME_BG  = Color.parseColor("#E8F5E9")
    private val COLOR_DEDUCT_BG  = Color.parseColor("#FFEBEE")

    // ─── Paints ───────────────────────────────────────────────────────────────
    private fun titlePaint(size: Float = 18f, bold: Boolean = true) = Paint().apply {
        color = COLOR_DARK
        textSize = size
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        isAntiAlias = true
    }
    private fun textPaint(size: Float = 10f, color: Int = COLOR_DARK, bold: Boolean = false) = Paint().apply {
        this.color = color
        textSize = size
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        isAntiAlias = true
    }
    private fun fillPaint(color: Int) = Paint().apply {
        this.color = color
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private fun strokePaint(color: Int, strokeWidth: Float = 1f) = Paint().apply {
        this.color = color
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        isAntiAlias = true
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generate slip gaji untuk 1 karyawan (1 halaman PDF).
     *
     * @param salary       Entity slip gaji dari SalaryViewModel
     * @param employeeName Nama karyawan (dari SalaryViewModel.employeeName atau salary.userName)
     * @param periodLabel  Label periode, contoh: "Juli 2026"
     * @param periodRange  Range tanggal, contoh: "1 Juli 2026 - 31 Juli 2026"
     * @param storeName    Nama toko utama (dari DB)
     * @param totalPresent Jumlah hari hadir
     * @param totalAlfa    Jumlah hari alfa
     * @param totalSchedules Total jadwal kerja
     * @param totalIncome  Total pemasukan hasil kalkulasi ViewModel
     * @param totalDeduction Total potongan hasil kalkulasi ViewModel
     * @param takeHomePay  Take home pay bersih
     * @param incomeItems  List komponen pemasukan (nama, jumlah)
     * @param deductionItems List komponen potongan (nama, jumlah)
     */
    fun generateSlipGaji(
        salary: SalaryDetailEntity,
        employeeName: String,
        periodLabel: String,
        periodRange: String,
        storeName: String,
        totalPresent: Int,
        totalAlfa: Int,
        totalSchedules: Int,
        totalIncome: Double,
        totalDeduction: Double,
        takeHomePay: Double,
        incomeItems: List<Pair<String, Double>>,
        deductionItems: List<Pair<String, Double>>
    ): PdfDocument {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        drawSlipPage(
            canvas = page.canvas,
            employeeName = employeeName,
            roleName = "",   // role tidak ada di SalaryDetailEntity
            periodLabel = periodLabel,
            periodRange = periodRange,
            storeName = storeName,
            payrollType = salary.payrollType,
            totalPresent = totalPresent,
            totalAlfa = totalAlfa,
            totalSchedules = totalSchedules,
            totalIncome = totalIncome,
            totalDeduction = totalDeduction,
            takeHomePay = takeHomePay,
            incomeItems = incomeItems,
            deductionItems = deductionItems,
            pageNumber = null
        )
        document.finishPage(page)
        return document
    }

    /**
     * Generate rekap slip gaji untuk semua karyawan (N halaman PDF).
     * Tiap halaman = 1 karyawan.
     *
     * @param slips List data slip per karyawan (sudah diproses oleh caller agar sesuai format)
     * @param periodLabel Label periode untuk header
     * @param periodRange Range tanggal untuk sub-header
     * @param storeName Nama toko utama
     */
    fun generateRekapSlipGaji(
        slips: List<SlipData>,
        periodLabel: String,
        periodRange: String,
        storeName: String
    ): PdfDocument {
        val document = PdfDocument()
        slips.forEachIndexed { index, slip ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
            val page = document.startPage(pageInfo)
            drawSlipPage(
                canvas = page.canvas,
                employeeName = slip.employeeName,
                roleName = "",
                periodLabel = periodLabel,
                periodRange = periodRange,
                storeName = storeName,
                payrollType = slip.payrollType,
                totalPresent = slip.totalPresent,
                totalAlfa = slip.totalAlfa,
                totalSchedules = slip.totalSchedules,
                totalIncome = slip.totalIncome,
                totalDeduction = slip.totalDeduction,
                takeHomePay = slip.takeHomePay,
                incomeItems = slip.incomeItems,
                deductionItems = slip.deductionItems,
                pageNumber = "${index + 1} / ${slips.size}"
            )
            document.finishPage(page)
        }
        return document
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL — Draw satu halaman slip ke Canvas
    // ─────────────────────────────────────────────────────────────────────────
    private fun drawSlipPage(
        canvas: Canvas,
        employeeName: String,
        roleName: String,
        periodLabel: String,
        periodRange: String,
        storeName: String,
        payrollType: PayrollType,
        totalPresent: Int,
        totalAlfa: Int,
        totalSchedules: Int,
        totalIncome: Double,
        totalDeduction: Double,
        takeHomePay: Double,
        incomeItems: List<Pair<String, Double>>,
        deductionItems: List<Pair<String, Double>>,
        pageNumber: String?
    ) {
        var y = MARGIN

        // ── HEADER BANNER ──────────────────────────────────────────────────
        val headerHeight = 70f
        canvas.drawRoundRect(
            RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + headerHeight),
            12f, 12f,
            fillPaint(COLOR_PRIMARY)
        )
        // Icon lingkaran putih
        canvas.drawCircle(MARGIN + 30f, y + headerHeight / 2, 20f, fillPaint(Color.WHITE.withAlpha(40)))
        // Title
        canvas.drawText("SLIP GAJI RESMI", MARGIN + 16f, y + 28f, titlePaint(16f).apply { color = COLOR_WHITE })
        canvas.drawText(storeName.ifBlank { "PerformaX" }, MARGIN + 16f, y + 46f, textPaint(10f, COLOR_WHITE))
        canvas.drawText("Periode: $periodLabel  |  $periodRange", MARGIN + 16f, y + 60f, textPaint(8f, COLOR_WHITE))
        // Page number (jika ada)
        pageNumber?.let {
            val numPaint = textPaint(8f, COLOR_WHITE)
            val numWidth = numPaint.measureText("Hal. $it")
            canvas.drawText("Hal. $it", PAGE_WIDTH - MARGIN - numWidth, y + 44f, numPaint)
        }
        y += headerHeight + 16f

        // ── INFO KARYAWAN ──────────────────────────────────────────────────
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 64f), 8f, 8f, fillPaint(COLOR_LIGHT))
        canvas.drawText("Nama Karyawan", MARGIN + 12f, y + 16f, textPaint(8f, COLOR_GRAY))
        canvas.drawText(employeeName.ifBlank { "-" }, MARGIN + 12f, y + 30f, textPaint(11f, COLOR_DARK, bold = true))
        val payrollLabel = if (payrollType == PayrollType.WEEKLY) "Mingguan (Weekly)" else "Bulanan (Monthly)"
        canvas.drawText("Sistem Penggajian: $payrollLabel", MARGIN + 12f, y + 46f, textPaint(9f, COLOR_GRAY))
        // Kehadiran di sisi kanan
        val attendText = "Hadir: $totalPresent  |  Alfa: $totalAlfa  |  Jadwal: $totalSchedules"
        val attPaint = textPaint(9f, COLOR_GRAY)
        val attWidth = attPaint.measureText(attendText)
        canvas.drawText(attendText, PAGE_WIDTH - MARGIN - attWidth - 12f, y + 30f, attPaint)
        y += 64f + 16f

        // ── TABEL PEMASUKAN ────────────────────────────────────────────────
        y = drawTableSection(
            canvas = canvas,
            y = y,
            title = "PEMASUKAN",
            items = incomeItems,
            total = totalIncome,
            totalLabel = "Total Pemasukan",
            bgColor = COLOR_INCOME_BG,
            totalColor = COLOR_PRIMARY
        )
        y += 12f

        // ── TABEL POTONGAN ─────────────────────────────────────────────────
        y = drawTableSection(
            canvas = canvas,
            y = y,
            title = "POTONGAN",
            items = deductionItems,
            total = totalDeduction,
            totalLabel = "Total Potongan",
            bgColor = COLOR_DEDUCT_BG,
            totalColor = COLOR_RED
        )
        y += 16f

        // ── TAKE HOME PAY ──────────────────────────────────────────────────
        val thpHeight = 52f
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + thpHeight), 10f, 10f, fillPaint(COLOR_PRIMARY))
        canvas.drawText("TAKE HOME PAY BERSIH", MARGIN + 16f, y + 20f, textPaint(9f, COLOR_WHITE, bold = true))
        val thpText = formatRupiah(takeHomePay)
        val thpPaint = titlePaint(18f).apply { color = COLOR_WHITE }
        val thpWidth = thpPaint.measureText(thpText)
        canvas.drawText(thpText, PAGE_WIDTH - MARGIN - thpWidth - 16f, y + 34f, thpPaint)
        y += thpHeight + 24f

        // ── FOOTER ────────────────────────────────────────────────────────
        val footerPaint = textPaint(7f, COLOR_GRAY)
        val footerText = "Dokumen ini digenerate secara otomatis oleh sistem PerformaX. Sah tanpa tanda tangan."
        val footerWidth = footerPaint.measureText(footerText)
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN.toFloat(), y, strokePaint(COLOR_DIVIDER))
        canvas.drawText(footerText, (PAGE_WIDTH - footerWidth) / 2, y + 14f, footerPaint)
    }

    /**
     * Draw section tabel (pemasukan / potongan) dan return Y posisi setelah section.
     */
    private fun drawTableSection(
        canvas: Canvas,
        y: Float,
        title: String,
        items: List<Pair<String, Double>>,
        total: Double,
        totalLabel: String,
        bgColor: Int,
        totalColor: Int
    ): Float {
        var currentY = y
        val sectionLeft = MARGIN
        val sectionRight = PAGE_WIDTH - MARGIN
        val colSplit = sectionRight - 130f   // posisi kolom nilai

        // Header section
        val headerH = 22f
        canvas.drawRoundRect(
            RectF(sectionLeft, currentY, sectionRight, currentY + headerH),
            6f, 6f, fillPaint(bgColor)
        )
        canvas.drawText(title, sectionLeft + 10f, currentY + 15f, textPaint(10f, COLOR_DARK, bold = true))
        currentY += headerH

        // Baris item
        items.forEach { (name, amount) ->
            if (amount <= 0.0) return@forEach
            val rowH = 20f
            // Zebra stripe ringan
            canvas.drawRect(RectF(sectionLeft, currentY, sectionRight, currentY + rowH), fillPaint(COLOR_WHITE))
            canvas.drawLine(sectionLeft, currentY + rowH, sectionRight, currentY + rowH, strokePaint(COLOR_DIVIDER, 0.5f))
            canvas.drawText(name, sectionLeft + 10f, currentY + 14f, textPaint(9f, COLOR_DARK))
            val amtText = formatRupiah(amount)
            val amtWidth = textPaint(9f, COLOR_DARK, bold = true).measureText(amtText)
            canvas.drawText(amtText, sectionRight - amtWidth - 10f, currentY + 14f, textPaint(9f, COLOR_DARK, bold = true))
            currentY += rowH
        }

        // Baris total
        val totalH = 24f
        canvas.drawRoundRect(RectF(sectionLeft, currentY, sectionRight, currentY + totalH), 0f, 0f, fillPaint(bgColor))
        canvas.drawText(totalLabel, sectionLeft + 10f, currentY + 17f, textPaint(10f, totalColor, bold = true))
        val totalText = formatRupiah(total)
        val totalWidth = textPaint(10f, totalColor, bold = true).measureText(totalText)
        canvas.drawText(totalText, sectionRight - totalWidth - 10f, currentY + 17f, textPaint(10f, totalColor, bold = true))
        currentY += totalH

        return currentY
    }

    private fun Int.withAlpha(alpha: Int): Int {
        return Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))
    }

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount).replace("Rp", "Rp ").substringBefore(",")
    }
}

/** Model data untuk 1 karyawan dalam rekap owner */
data class SlipData(
    val employeeName: String,
    val payrollType: PayrollType,
    val totalPresent: Int,
    val totalAlfa: Int,
    val totalSchedules: Int,
    val totalIncome: Double,
    val totalDeduction: Double,
    val takeHomePay: Double,
    val incomeItems: List<Pair<String, Double>>,
    val deductionItems: List<Pair<String, Double>>
)
