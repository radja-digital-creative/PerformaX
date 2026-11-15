package id.biz.yuka.performax.ui.screen.task.detail

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.biz.yuka.performax.ui.components.GlobalErrorPopup
import id.biz.yuka.libperformx.entity.TaskCommentEntity
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.TaskEntity
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderLime
import id.biz.yuka.performax.ui.theme.WonderOrange
import id.biz.yuka.performax.utils.PermissionUtils
import io.github.jan.supabase.gotrue.auth
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit = {},
    onNavigateToRoutineDetail: (String) -> Unit = {},
    onNavigateToUrgentDetail: (String) -> Unit = {},
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val task = viewModel.task
    val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id
    val context = LocalContext.current

    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // ✅ 1. SETUP PULL TO REFRESH SEPERTI DI HOME SCREEN
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = viewModel.isLoading

    LaunchedEffect(viewModel.isDeleted) {
        if (viewModel.isDeleted) {
            Toast.makeText(context, "Tugas berhasil dihapus.", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    // Auto Refresh ketika kembali dari Edit
    LaunchedEffect(Unit) {
        viewModel.loadTaskDetail()
    }

    DisposableEffect(viewModel) {
        viewModel.setAsActiveRoom()
        onDispose { viewModel.clearActiveRoom() }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(task?.comments?.size) {
        if ((task?.comments?.size ?: 0) > 0) {
            listState.animateScrollToItem(task!!.comments.size - 1)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addSelectedImage(context, it) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        viewModel.handleCameraResult(context, bitmap)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted) {
            when (pendingAction) {
                "gallery" -> galleryLauncher.launch("image/*")
                "camera" -> cameraLauncher.launch()
            }
        } else {
            Toast.makeText(context, "Izin diperlukan untuk menggunakan fitur ini.", Toast.LENGTH_SHORT).show()
        }
        pendingAction = ""
    }

    Scaffold(
        containerColor = Color(0xFFE5DDD5),
        topBar = {
            Surface(
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = Color.White
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = task?.title ?: "Memuat...",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
                        },
                        actions = {
                            if (viewModel.isCreator) {
                                IconButton(onClick = { showDeleteConfirm = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Tugas", tint = Color.Red)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )

                    if (task != null) {
                        ExpandableTaskHeader(
                            task = task,
                            onImageClick = { fullscreenImageUrl = it },
                            currentUserId = currentUserId,
                            onUpdateStatus = { assignId, status -> viewModel.updateAssignmentStatus(assignId, status) },
                            onEditClick = { onNavigateToEdit(task.id) },
                            isManager = viewModel.isManager,
                            onLabelClick = { labelName ->
                                if (labelName.equals("Routine", ignoreCase = true) && task.routineId != null) {
                                    task.routineId?.let { onNavigateToRoutineDetail(it) }
                                } else if (labelName.equals("Urgent", ignoreCase = true) && viewModel.urgentId != null) {
                                    onNavigateToUrgentDetail(viewModel.urgentId!!)
                                }
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (task != null && viewModel.canComment) {
                CommentInputBar(
                    text = viewModel.commentText,
                    selectedImages = viewModel.selectedImages,
                    onTextChange = { viewModel.commentText = it },
                    onSend = { viewModel.sendComment(context) },
                    onRemoveImage = { viewModel.removeSelectedImage(it) },
                    onAttachGallery = {
                        if (PermissionUtils.hasGalleryPermission(context)) {
                            galleryLauncher.launch("image/*")
                        } else {
                            pendingAction = "gallery"
                            permissionLauncher.launch(PermissionUtils.GALLERY_PERMISSIONS)
                        }
                    },
                    onAttachCamera = {
                        if (PermissionUtils.hasCameraPermission(context)) {
                            cameraLauncher.launch()
                        } else {
                            pendingAction = "camera"
                            permissionLauncher.launch(arrayOf(PermissionUtils.CAMERA_PERMISSION))
                        }
                    },
                    isSending = viewModel.isSendingComment
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ✅ 2. BUNGKUS DENGAN PULL TO REFRESH BOX
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.loadTaskDetail() },
                state = pullToRefreshState,
                indicator = {
                    if (isRefreshing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().zIndex(1f),
                            color = WonderOrange,
                            trackColor = WonderLime
                        )
                    }
                }
            ) {
                if (task != null) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(task.comments) { comment ->
                            ChatBubble(
                                comment = comment,
                                isMine = comment.userId == currentUserId,
                                onImageClick = { fullscreenImageUrl = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Tugas?", fontWeight = FontWeight.Bold) },
            text = { Text("Tugas yang dihapus tidak dapat dikembalikan. Lanjutkan?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteTask()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Hapus", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal", color = Color.Gray) }
            },
            containerColor = Color.White
        )
    }

    if (fullscreenImageUrl != null) {
        Dialog(onDismissRequest = { fullscreenImageUrl = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(model = fullscreenImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                IconButton(onClick = { fullscreenImageUrl = null }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Tugas?", fontWeight = FontWeight.Bold) },
            text = { Text("Tugas yang dihapus tidak dapat dikembalikan. Lanjutkan?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteTask()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Hapus", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal", color = Color.Gray) }
            },
            containerColor = Color.White
        )
    }

    GlobalErrorPopup(
        errorMessage = viewModel.errorMessage,
        onDismiss = { viewModel.errorMessage = null }
    )
}

@Composable
fun ExpandableTaskHeader(
    task: TaskEntity,
    onImageClick: (String) -> Unit,
    currentUserId: String?,
    onUpdateStatus: (String, String) -> Unit,
    onEditClick: () -> Unit = {},
    onLabelClick: (String) -> Unit = {},
    isManager: Boolean = false
) {
    val isCreator = task.creatorId == currentUserId
    val canApprove = isCreator || isManager
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val myAssignment = task.assignees.find { it.userId == currentUserId }
    val isTaskActive = myAssignment?.status == "pending" || myAssignment?.status == "in_progress"
    val isVerifiedLocked = task.assignees.any { it.status == "verified" }
    val showEditButton = isCreator && !isVerifiedLocked

    var isOverdue = false
    var isTaskDoneOverdue = false

    val formattedDeadline = try {
        val deadlineDate = LocalDate.parse(task.dueDate.substring(0, 10))

        if (deadlineDate.isBefore(LocalDate.now()) && myAssignment?.isCompleted != true) {
            isOverdue = true
        }
        if (myAssignment?.isCompleted == true){
            val complatedAtDate = LocalDate.parse(myAssignment.completedAt?.substring(0, 10))
            if (deadlineDate.isBefore(complatedAtDate)) {
                isTaskDoneOverdue = true
            }
        }
        val zdt = java.time.ZonedDateTime.parse(task.dueDate)
        zdt.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", java.util.Locale("id", "ID")))
    } catch (e: Exception) { task.dueDate }

    fun parseColorSafe(hex: String): Color {
        return try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (!expanded) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, null, tint = WonderOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = formattedDeadline, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = WonderOrange)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showEditButton) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Tugas", tint = WonderGrey, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Lihat Detail ▾", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                }
            }
        }

        if (expanded) {
            Column(modifier = Modifier.fillMaxWidth()) {

                if (isOverdue && isTaskActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = "Peringatan",
                                tint = WonderOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Tugas melewati tenggat waktu! Segera selesaikan, atau poin akan dianggap hangus.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(12.dp)
                                .offset(y = (-6).dp)
                                .graphicsLayer { rotationZ = 45f }
                                .background(Color(0xFFFFF3E0))
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (isOverdue) Color.Red else WonderOrange,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "Tenggat: $formattedDeadline",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverdue) Color.Red else WonderOrange
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            if (task.labels.isNotEmpty()) {
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(task.labels) { label ->
                                        val labelColor = parseColorSafe(label.colorHex)
                                        Surface(
                                            color = labelColor.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(50),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, labelColor),
                                            modifier = Modifier.clickable { onLabelClick(label.name) }
                                        ) {
                                            Text(
                                                text = label.name,
                                                color = labelColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showEditButton) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Tugas", tint = WonderGrey, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Text(text = task.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                if (task.attachments.isNotEmpty()) {
                    Text("Lampiran:", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        items(task.attachments) { attachment ->
                            coil.compose.AsyncImage(
                                model = attachment.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).clickable { onImageClick(attachment.imageUrl) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Dibuat oleh:", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=4.dp)) {
                            val creatorUrl = task.creatorProfileUrl
                            if (!creatorUrl.isNullOrEmpty()) {
                                coil.compose.AsyncImage(
                                    model = creatorUrl,
                                    contentDescription = "Creator",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(28.dp).clip(CircleShape)
                                )
                            } else {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Creator", tint = WonderOrange, modifier = Modifier.size(28.dp))
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            Text(task.creatorName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Ditugaskan ke:", style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                        Row(horizontalArrangement = Arrangement.spacedBy(-8.dp), modifier = Modifier.padding(top=4.dp)) {
                            task.assignees.forEach { assignee ->

                                var showTooltip by remember { mutableStateOf(false) }

                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(1.dp, WonderGrey, CircleShape)
                                            .clickable { showTooltip = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val assigneeUrl = assignee.profileUrl
                                        if (!assigneeUrl.isNullOrEmpty()) {
                                            coil.compose.AsyncImage(
                                                model = assigneeUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                                        }
                                    }

                                    if (showTooltip) {
                                        androidx.compose.ui.window.Popup(
                                            alignment = Alignment.TopCenter,
                                            offset = androidx.compose.ui.unit.IntOffset(0, -100),
                                            onDismissRequest = { showTooltip = false }
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFF333333),
                                                shadowElevation = 4.dp
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(assignee.userName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                                    if (canApprove) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("•  ${assignee.earnedPoints} Poin", color = WonderOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (myAssignment != null) {

                    Surface(
                        color = WonderOrange.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Poin & Waktu:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                                // ✅ Tampilkan poin 0 jika terlambat, jika tidak tampilkan poin asli
                                val finalPoints = myAssignment.earnedPoints
                                Text("$finalPoints Poin", fontWeight = FontWeight.ExtraBold, color = if(isTaskDoneOverdue) Color.Red else WonderOrange)
                            }

                            // Tampilkan perbandingan waktu
                            Text("Selesai: ${myAssignment.completedAt?.take(10) ?: "-"}",
                                style = MaterialTheme.typography.labelSmall)

                            // ✅ Catatan jika hangus
                            if (isTaskDoneOverdue) {
                                Text("Catatan: Poin hangus dikarenakan penyelesaian tugas melewati batas waktu.",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Red, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }

                    when (myAssignment.status) {
                        "pending" -> {
                            Button(
                                onClick = { onUpdateStatus(myAssignment.assignmentId, "in_progress") },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Mulai Kerjakan", fontWeight = FontWeight.Bold) }
                        }
                        "in_progress" -> {
                            Button(
                                onClick = { onUpdateStatus(myAssignment.assignmentId, "completed") },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Tandai Selesai", fontWeight = FontWeight.Bold) }
                        }
                        "completed" -> {
                            Button(
                                onClick = { }, enabled = false,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Menunggu Review Atasan") }
                        }
                        "verified" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp), // Memberikan sedikit ruang nafas
                                horizontalArrangement = Arrangement.Center, // Tetap berada di tengah
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32), // Warna hijau yang lebih elegan untuk teks/ikon
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Telah Diverifikasi Selesai",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                )
                            }
                        }
                    }
                }

                // ==========================================================
                // ✅ TAMPILAN KHUSUS UNTUK CREATOR/MANAGER (DIPERBARUI DENGAN STATUS)
                // ==========================================================
                if (canApprove && myAssignment == null) {
                    val primaryAssignment = task.assignees.firstOrNull() // Ambil baris pertama

                    if (primaryAssignment != null) {
                        // Tentukan warna dan teks status berdasarkan status baris pertama
                        val (statusText, statusColor) = when (primaryAssignment.status) {
                            "pending" -> "Status: Menunggu Pengerjaan" to Color.Gray
                            "in_progress" -> "Status: Sedang Dikerjakan" to Color(0xFF2196F3)
                            "completed" -> "Status: Menunggu Review Anda" to WonderOrange
                            "verified" -> "Status: Tugas Selesai" to Color(0xFF4CAF50)
                            else -> "Status: ${primaryAssignment.status}" to Color.Gray
                        }

                        // Tampilkan teks status yang elegan
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Jika status COMPLETED, tampilkan tombol Review (Setujui/Revisi)
                        if (primaryAssignment.status == "completed") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onUpdateStatus(primaryAssignment.assignmentId, "in_progress") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) { Text("Revisi") }

                                Button(
                                    onClick = { onUpdateStatus(primaryAssignment.assignmentId, "verified") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) { Text("Setujui") }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Tutup Detail ▴", style = MaterialTheme.typography.labelSmall, color = WonderGrey, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(Color(0xFFE0E0E0)))
        }
    }
}

@Composable
fun ChatBubble(comment: TaskCommentEntity, isMine: Boolean, onImageClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            val commentUrl = comment.userProfileUrl
            if (!commentUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = commentUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(28.dp).padding(bottom = 4.dp).clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp).padding(bottom = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            if (!isMine) {
                Text(
                    text = comment.userName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = WonderGrey,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Surface(
                color = if (isMine) Color(0xFFDCF8C6) else Color.White,
                contentColor = Color.Black,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMine) 16.dp else 0.dp,
                    bottomEnd = if (isMine) 0.dp else 16.dp
                ),
                shadowElevation = 1.dp,
                modifier = Modifier.widthIn(max = 260.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (comment.commentImages.isNotEmpty()) {
                        AsyncImage(
                            model = comment.commentImages.first(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageClick(comment.commentImages.first()) },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(text = comment.commentText, style = MaterialTheme.typography.bodyMedium)

                    Text(
                        text = comment.createdAt.take(16).substring(11),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CommentInputBar(
    text: String,
    selectedImages: List<Uri>,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onAttachGallery: () -> Unit,
    onAttachCamera: () -> Unit,
    isSending: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImages) { uri ->
                        Box(modifier = Modifier.size(80.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { onRemoveImage(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAttachCamera) { Icon(Icons.Default.PhotoCamera, null, tint = WonderGrey) }
                IconButton(onClick = onAttachGallery) { Icon(Icons.Default.Image, null, tint = WonderGrey) }

                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("Tulis pesan...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { onSend() },
                    enabled = !isSending && (text.isNotBlank() || selectedImages.isNotEmpty()),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (!isSending && (text.isNotBlank() || selectedImages.isNotEmpty())) WonderOrange else Color.LightGray)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}