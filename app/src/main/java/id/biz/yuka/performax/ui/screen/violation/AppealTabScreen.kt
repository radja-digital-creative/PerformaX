package id.biz.yuka.performax.ui.screen.violation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.biz.yuka.libperformx.entity.AppealCommentEntity
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange
import id.biz.yuka.performax.utils.chatBackgroundPattern
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppealTabContent(viewModel: AppealViewModel, pullToRefreshState: PullToRefreshState) {
    if (viewModel.selectedAppeal != null) {
        AppealChatRoom(viewModel)
    } else {
        PullToRefreshBox(
            isRefreshing = viewModel.isLoading,
            onRefresh = { viewModel.loadActiveAppeals() },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (viewModel.isOwner) {
                    if (viewModel.ownerActiveAppeals.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Text("Tidak ada pengajuan banding baru.", color = WonderGrey, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        item { Text("Daftar Banding Aktif", fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)) }
                        items(viewModel.ownerActiveAppeals) { appeal ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp).clip(RoundedCornerShape(16.dp)).clickable { viewModel.selectAppealAndLoadChats(appeal) },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            text = appeal.employeeName ?: "Banding ID: ${appeal.id.take(8)}...",
                                            fontWeight = FontWeight.Bold
                                        )

                                        if (appeal.isDataSufficient) {
                                            Icon(Icons.Default.CheckCircle, "Data Cukup", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                        } else {
                                            Icon(Icons.Default.Pending, "AI Sedang Interogasi", tint = WonderOrange, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Dikirim: ${appeal.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Ketuk untuk melihat percakapan / eksekusi banding yang dilakukan AI HR Agent.", color = WonderOrange, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Tidak ada banding aktif.", color = WonderGrey, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
fun AppealChatRoom(viewModel: AppealViewModel) {
    val appeal = viewModel.selectedAppeal ?: return
    val violation = viewModel.appealedViolation
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }

    // ==========================================
    // PERBAIKAN BUG AUTO-RELOAD (FCM)
    // ==========================================
    // DisposableEffect akan memaksa pendaftaran Active Room saat Chat ini tampil di layar,
    // dan otomatis menghapusnya (clear) saat user menekan back / pindah layar.
    DisposableEffect(appeal.id) {
        viewModel.setAsActiveRoom()
        onDispose {
            viewModel.clearActiveRoom()
        }
    }

    val formattedDate = try {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale("id", "ID"))
        val date = LocalDate.parse(appeal.createdAt.substring(0, 10))
        date.format(formatter)
    } catch (e: Exception) {
        appeal.createdAt.take(10)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFE5DDD5)).chatBackgroundPattern()) {

        if (viewModel.isOwner) {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                contentColor = Color.Black
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.resolveAppeal(isApproved = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp)
                    ) { Text("Terima Banding", fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { viewModel.resolveAppeal(isApproved = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                        modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp)
                    ) { Text("Tolak Banding", fontWeight = FontWeight.Bold) }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
        ) {
            val sortedComments = viewModel.comments.sortedByDescending { it.createdAt }
            items(sortedComments) { comment ->
                val isMine = if (viewModel.isOwner) comment.senderType == "owner" else comment.senderType == "employee"
                AppealChatBubble(comment, isMine, onImageClick = { fullscreenImageUrl = it })
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            text = formattedDate,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5A6670),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (violation != null) {
                        Surface(
                            color = Color(0xFFFFF5E1),
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Detail Pelanggaran yang Dibanding",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF5A6670)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Kejadian pada: ${violation.violationDate.take(10)}\nSanksi: ${violation.appliedPenalty}\nCatatan: ${violation.violationNotes ?: "-"}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF5A6670),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (fullscreenImageUrl != null) {
            CustomImageOverlay(
                imageUrl = fullscreenImageUrl,
                onDismiss = { fullscreenImageUrl = null }
            )
        }
    }
}

@Composable
fun CustomImageOverlay(imageUrl: String?, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = imageUrl != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Fullscreen Image",
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    contentScale = ContentScale.Fit
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding()
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Close, "Tutup", tint = Color.White)
            }
        }
    }
}

@Composable
fun AppealChatBubble(comment: AppealCommentEntity, isMine: Boolean, onImageClick: (String) -> Unit) {
    val isOwner = comment.senderType == "owner"
    val isEmployee = comment.senderType == "employee"
    val isAi = !isOwner && !isEmployee

    val bubbleColor = when {
        isMine -> Color(0xFFDCF8C6)
        isAi -> Color(0xFFE1F5FE)
        else -> Color.White
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            val icon = if (isAi) Icons.Default.SmartToy else Icons.Default.Person
            val tint = if (isAi) Color(0xFF03A9F4) else Color.Gray
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp).padding(bottom = 4.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            if (!isMine) {
                val senderName = when {
                    isOwner -> "Manajemen"
                    isEmployee -> "Karyawan"
                    else -> "AI HR Agent"
                }
                val senderColor = if (isAi) Color(0xFF0288D1) else WonderGrey

                Text(
                    text = senderName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = senderColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Surface(
                color = bubbleColor,
                contentColor = Color.Black,
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isMine) 16.dp else 0.dp,
                    bottomEnd = if (isMine) 0.dp else 16.dp
                ),
                shadowElevation = 1.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (!comment.images.isNullOrEmpty()) {
                        comment.images.forEach { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Lampiran Chat",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onImageClick(imageUrl) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Text(text = comment.message, style = MaterialTheme.typography.bodyMedium)

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppealInputBar(
    text: String,
    selectedImages: List<Uri>,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onAttachGallery: () -> Unit,
    onAttachCamera: () -> Unit,
    isSending: Boolean
) {
    val isImeVisible = WindowInsets.isImeVisible

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = if (isImeVisible) 24.dp else 0.dp),
        shadowElevation = 8.dp,
        color = Color(0xFFE5DDD5)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImages) { uri ->
                        Box(modifier = Modifier.size(80.dp)) {
                            AsyncImage(
                                model = uri, contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { onRemoveImage(uri) },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onAttachCamera, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.PhotoCamera, null, tint = WonderGrey)
                    }
                    IconButton(onClick = onAttachGallery, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.AttachFile, null, tint = WonderGrey)
                    }

                    Box(
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) { Text("Ketik pesan...", color = Color.Gray, fontSize = 16.sp) }
                        androidx.compose.foundation.text.BasicTextField(
                            value = text, onValueChange = onTextChange, modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color.Black),
                            maxLines = 4, cursorBrush = androidx.compose.ui.graphics.SolidColor(WonderOrange)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSend,
                    enabled = !isSending && (text.isNotBlank() || selectedImages.isNotEmpty()),
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (!isSending && (text.isNotBlank() || selectedImages.isNotEmpty())) WonderOrange else Color.LightGray)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(24.dp).offset(x = 2.dp))
                    }
                }
            }
        }
    }
}