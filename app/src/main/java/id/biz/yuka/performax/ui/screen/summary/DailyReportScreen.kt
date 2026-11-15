package id.biz.yuka.performax.ui.screen.summary

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.ui.theme.WonderLime
import id.biz.yuka.performax.ui.theme.WonderOrange
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportScreen(
    onBack: () -> Unit,
    viewModel: DailyReportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val markdownContent = viewModel.markdownContent
    val reportEntity = viewModel.reportEntity
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage

    // Launcher untuk save file .md ke Downloads
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        uri?.let { saveMarkdownToUri(context, it, markdownContent ?: "") }
    }

    val formattedDate = try {
        val d = LocalDate.parse(viewModel.todayStr)
        d.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID")))
    } catch (e: Exception) {
        viewModel.todayStr
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Report",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Tombol Refresh
                    IconButton(
                        onClick = { viewModel.generateReport() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Perbarui Laporan",
                            tint = WonderOrange
                        )
                    }

                    // Tombol Download .md — hanya aktif jika konten tersedia
                    IconButton(
                        onClick = {
                            val fileName = "daily-report-${viewModel.todayStr}.md"
                            saveLauncher.launch(fileName)
                        },
                        enabled = markdownContent != null && !isLoading
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Unduh .md",
                            tint = if (markdownContent != null) WonderOrange else Color.LightGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // ── Loading State ──────────────────────────────────────────
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = WonderOrange)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Memuat laporan...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                        )
                    }
                }

                // ── Error State ────────────────────────────────────────────
                errorMessage != null && markdownContent == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Gagal memuat laporan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.loadReport() },
                            colors = ButtonDefaults.buttonColors(containerColor = WonderOrange)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }

                // ── Empty State (belum ada report) ─────────────────────────
                markdownContent == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📋", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Laporan Belum Tersedia",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Laporan harian akan dibuat otomatis jam 19.00 WIB.\nKamu juga bisa membuatnya sekarang.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, lineHeight = 20.sp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.generateReport() },
                            colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buat Sekarang", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── Content: Markdown Reader ───────────────────────────────
                else -> {
                    AnimatedVisibility(
                        visible = !isLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        MarkdownReader(
                            markdown = markdownContent!!,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARKDOWN READER — render markdown menjadi LazyColumn native Compose
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MarkdownReader(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val lines = markdown.lines()

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(lines.size) { index ->
            val line = lines[index]
            MarkdownLine(line = line)
        }
        // Padding bawah agar tidak terpotong FAB
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun MarkdownLine(line: String) {
    when {
        // H1 — # Judul
        line.startsWith("# ") -> {
            val text = line.removePrefix("# ")
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp
                ),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        // H2 — ## Section
        line.startsWith("## ") -> {
            val text = line.removePrefix("## ")
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                )
                Divider(
                    modifier = Modifier.padding(top = 4.dp),
                    color = Color(0xFFE0E0E0),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // H3 — ### Sub-section
        line.startsWith("### ") -> {
            val text = line.removePrefix("### ")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF444444)
                ),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // Horizontal rule — ---
        line.trim() == "---" -> {
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Blockquote — > teks
        line.startsWith("> ") -> {
            val text = line.removePrefix("> ")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .background(WonderOrange, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = renderInlineMarkdown(text),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF333333)
                    )
                )
            }
        }

        // List item — - teks atau * teks
        line.startsWith("- ") || line.startsWith("* ") -> {
            val text = line.removePrefix("- ").removePrefix("* ")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = WonderOrange,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                )
                Text(
                    text = renderInlineMarkdown(text),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bold line — **teks**
        line.trim().startsWith("**") && line.trim().endsWith("**") -> {
            Text(
                text = line.trim().removeSurrounding("**"),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
        }

        // Baris kosong
        line.isBlank() -> {
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Teks biasa (dengan kemungkinan inline bold/backtick)
        else -> {
            Text(
                text = renderInlineMarkdown(line),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
            )
        }
    }
}

/**
 * Render inline markdown: **bold**, `code`, dan teks biasa jadi AnnotatedString.
 */
@Composable
fun renderInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val boldRegex = Regex("""\*\*(.+?)\*\*""")
        val codeRegex = Regex("""`(.+?)`""")

        var lastIndex = 0
        // Gabungkan semua matches dan proses berurutan
        val allMatches = (boldRegex.findAll(text) + codeRegex.findAll(text))
            .sortedBy { it.range.first }

        for (match in allMatches) {
            if (match.range.first < lastIndex) continue
            // Teks sebelum match
            append(text.substring(lastIndex, match.range.first))

            when {
                match.value.startsWith("**") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groupValues[1])
                    }
                }
                match.value.startsWith("`") -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF555555),
                            background = Color(0xFFF0F0F0)
                        )
                    ) {
                        append(" ${match.groupValues[1]} ")
                    }
                }
            }
            lastIndex = match.range.last + 1
        }
        // Sisa teks setelah match terakhir
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPER: Simpan konten markdown ke URI yang dipilih user
// ─────────────────────────────────────────────────────────────────────────────
private fun saveMarkdownToUri(context: Context, uri: Uri, content: String) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            OutputStreamWriter(stream).use { writer ->
                writer.write(content)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
