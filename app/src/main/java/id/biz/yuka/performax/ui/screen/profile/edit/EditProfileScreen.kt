package id.biz.yuka.performax.ui.screen.profile.edit

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel() // Menggunakan ViewModel Baru
) {
    val context = LocalContext.current

    // Helper untuk decode URI ke Bitmap[cite: 5]
    fun uriToBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

    // Launchers[cite: 5]
    val profileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.profileBitmap = uriToBitmap(uri)
    }

    val ktpLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.ktpBitmap = uriToBitmap(uri)
    }

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Langkah ${viewModel.currentStep} dari 4", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (viewModel.currentStep < 4) {
                        IconButton(onClick = {
                            if (viewModel.currentStep == 1) onBack() else viewModel.previousStep()
                        }) { Icon(Icons.Default.ArrowBack, null) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = viewModel.currentStep, label = "Stepper") { step ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp).then(if (step == 1) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        1 -> { // STEP 1: DATA DIRI
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    id.biz.yuka.performax.ui.screen.account.AccountSecurityFields(
                                        currentEmail = viewModel.currentEmail,
                                        email = viewModel.editEmail, onEmailChange = { viewModel.editEmail = it },
                                        password = viewModel.editPassword, onPasswordChange = { viewModel.editPassword = it },
                                        saving = viewModel.isSavingAccount, onSave = viewModel::saveAccount
                                    )
                                    viewModel.accountMessage?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Informasi Data Diri", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                    OutlinedTextField(value = viewModel.editFullName, onValueChange = { viewModel.editFullName = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = viewModel.editPhoneNumber, onValueChange = { viewModel.editPhoneNumber = it }, label = { Text("Nomor HP") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = viewModel.editAddress, onValueChange = { viewModel.editAddress = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

                                    // TAMBAHKAN KODE INI UNTUK PILIHAN GENDER
                                    Text("Jenis Kelamin", fontWeight = FontWeight.SemiBold, color = WonderGrey, modifier = Modifier.padding(top = 8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Pilihan Laki-laki
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.editGender = "L" }) {
                                            RadioButton(
                                                selected = (viewModel.editGender == "L" || viewModel.editGender == "male"),
                                                onClick = { viewModel.editGender = "male" },
                                                colors = RadioButtonDefaults.colors(selectedColor = WonderOrange)
                                            )
                                            Text("Laki-laki")
                                        }

                                        Spacer(modifier = Modifier.width(24.dp))

                                        // Pilihan Perempuan
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.editGender = "P" }) {
                                            RadioButton(
                                                selected = (viewModel.editGender == "P" || viewModel.editGender == "female"),
                                                onClick = { viewModel.editGender = "female" },
                                                colors = RadioButtonDefaults.colors(selectedColor = WonderOrange)
                                            )
                                            Text("Perempuan")
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.nextStep() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WonderOrange)
                            ) { Text("Selanjutnya") }
                        }

                        2 -> { // STEP 2: FOTO PROFIL
                            Text("Unggah Foto Profil", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("Foto ini akan digunakan sebagai foto identitas utama Anda.", textAlign = TextAlign.Center, color = WonderGrey, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))

                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray.copy(alpha = 0.3f))
                                    .border(2.dp, WonderOrange, CircleShape)
                                    .clickable { profileLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (viewModel.profileBitmap != null) {
                                    Image(bitmap = viewModel.profileBitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddAPhoto, null, tint = WonderGrey, modifier = Modifier.size(40.dp))
                                        Text("Pilih Foto", color = WonderGrey)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { viewModel.nextStep() },
                                enabled = viewModel.profileBitmap != null,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WonderOrange)
                            ) { Text("Selanjutnya") }
                        }

                        3 -> { // STEP 3: FOTO KTP & PROSES VERIFIKASI
                            Text("Unggah Foto KTP", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("Kami akan memverifikasi KTP Anda dengan Foto Profil AI.", textAlign = TextAlign.Center, color = WonderGrey, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.LightGray.copy(alpha = 0.3f))
                                    .border(2.dp, WonderOrange, RoundedCornerShape(16.dp))
                                    .clickable { ktpLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (viewModel.ktpBitmap != null) {
                                    Image(bitmap = viewModel.ktpBitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddAPhoto, null, tint = WonderGrey, modifier = Modifier.size(40.dp))
                                        Text("Pilih Foto KTP", color = WonderGrey)
                                    }
                                }
                            }

                            if (viewModel.errorMessage != null) {
                                Text(text = viewModel.errorMessage!!, color = Color.Red, modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center)
                            }

                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = { viewModel.processAndSaveAll() },
                                enabled = viewModel.ktpBitmap != null && !viewModel.isLoading,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WonderOrange)
                            ) {
                                if (viewModel.isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Verifikasi & Simpan", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        4 -> { // STEP 4: SUKSES
                            Spacer(modifier = Modifier.height(40.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(100.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Profil Berhasil Diperbarui!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Data Anda, Foto Profil, dan KTP telah berhasil diverifikasi dan disimpan.", textAlign = TextAlign.Center, color = WonderGrey, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

                            Button(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WonderOrange)
                            ) { Text("Selesai") }
                        }
                    }
                }
            }
        }
    }
}