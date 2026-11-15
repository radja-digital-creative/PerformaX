//package id.biz.yuka.performax.ui.screen.profile
//
//import android.graphics.Bitmap
//import android.graphics.ImageDecoder
//import android.net.Uri
//import android.os.Build
//import android.provider.MediaStore
//import android.util.Log
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.PickVisualMediaRequest
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.activity.result.launch
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.aspectRatio
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.AddAPhoto
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.asImageBitmap
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import id.biz.yuka.performax.ui.theme.WonderBg
//import id.biz.yuka.performax.ui.theme.WonderGrey
//import id.biz.yuka.performax.ui.theme.WonderOrange
//
//@Composable
//fun VerificationSection(
//    viewModel: ProfileViewModel,
//    modifier: Modifier = Modifier
//) {
//    val context = LocalContext.current
//    var ktpBitmap by remember { mutableStateOf<Bitmap?>(null) }
//    var selfieBitmap by remember { mutableStateOf<Bitmap?>(null) }
//
//    // Launcher untuk ambil KTP
//    val ktpLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.PickVisualMedia()
//    ) { uri: Uri? ->
//        if (uri != null) {
//            Log.d("KYC_DEBUG", "KTP URI terpilih: $uri")
//            ktpBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
//            } else {
//                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
//            }
//        } else {
//            Log.d("KYC_DEBUG", "KTP Gagal dipilih/dibatalkan")
//        }
//    }
//
//    // Launcher untuk ambil Selfie
//    val selfieLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.TakePicturePreview()
//    ) { bitmap: Bitmap? ->
//        if (bitmap != null) {
//            Log.d("KYC_DEBUG", "Selfie Berhasil diambil")
//            selfieBitmap = bitmap
//        } else {
//            Log.d("KYC_DEBUG", "Selfie Gagal/dibatalkan")
//        }
//    }
//
//    Card(
//        modifier = modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White)
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Text("Verifikasi Identitas", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//                // Kotak KTP
//                VerificationBox(
//                    label = "Foto KTP",
//                    bitmap = ktpBitmap,
//                    onClick = { ktpLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
//                    modifier = Modifier.weight(1f)
//                )
//
//                // Kotak Selfie
//                VerificationBox(
//                    label = "Selfie Live",
//                    bitmap = selfieBitmap,
//                    onClick = { selfieLauncher.launch() },
//                    modifier = Modifier.weight(1f)
//                )
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            // Tombol Submit
//            Button(
//                onClick = {
//                    if (ktpBitmap != null && selfieBitmap != null) {
//                        viewModel.processKyc(ktpBitmap!!, selfieBitmap!!)
//                    }
//                },
//                enabled = !viewModel.isKycLoading && ktpBitmap != null && selfieBitmap != null,
//                modifier = Modifier.fillMaxWidth().height(50.dp),
//                shape = RoundedCornerShape(12.dp),
//                colors = ButtonDefaults.buttonColors(containerColor = WonderOrange)
//            ) {
//                if (viewModel.isKycLoading) {
//                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
//                } else {
//                    Text("Verifikasi Sekarang", fontWeight = FontWeight.Bold)
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun VerificationBox(label: String, bitmap: Bitmap?, onClick: () -> Unit, modifier: Modifier) {
//    Box(
//        modifier = modifier
//            .aspectRatio(1f)
//            .clip(RoundedCornerShape(12.dp))
//            .background(WonderBg)
//            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
//            .clickable { onClick() },
//        contentAlignment = Alignment.Center
//    ) {
//        if (bitmap != null) {
//            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
//        } else {
//            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                Icon(Icons.Default.AddAPhoto, null, tint = WonderGrey)
//                Text(label, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
//            }
//        }
//    }
//}