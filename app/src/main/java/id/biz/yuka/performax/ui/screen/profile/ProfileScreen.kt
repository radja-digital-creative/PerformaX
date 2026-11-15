package id.biz.yuka.performax.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.biz.yuka.performax.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToEdit: () -> Unit
) {
    val myProfile = viewModel.profile

    Scaffold(
        containerColor = WonderBg,
        topBar = {
            TopAppBar(
                title = { Text("Profil Saya", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg),
                actions = {
                    IconButton(onClick = { onNavigateToEdit() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profil")
                    }
                }
            )
        }
    ) { padding ->
        if (viewModel.isLoading && myProfile == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WonderOrange)
            }
        } else if (myProfile != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. HEADER AVATAR & NAMA
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!myProfile.profileUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = myProfile.profileUrl,
                                contentDescription = "Foto Profil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, WonderOrange, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, WonderOrange, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // NAMA LENGKAP
                        Text(
                            text = myProfile.fullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // ROLE (Jabatan)
                        val roleColor = when (myProfile.roleName.lowercase()) {
                            "owner", "admin" -> WonderOrange
                            "manager" -> Color(0xFF2E7D32)
                            else -> Color(0xFF1976D2)
                        }

                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = roleColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Stars, contentDescription = null, tint = roleColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = myProfile.roleName.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = roleColor
                                    )
                                }
                            }

                        }
                    }
                }

                // 2. KARTU INFORMASI PRIBADI
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Informasi Kontak", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))

                            ProfileRowItem(icon = Icons.Default.Phone, label = "Nomor HP", value = myProfile.phoneNumber)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))

                            ProfileRowItem(icon = Icons.Default.LocationOn, label = "Alamat", value = myProfile.address)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))

                            val genderText = if (myProfile.gender.equals("L", true) || myProfile.gender.lowercase() == "male") "Laki-laki" else "Perempuan"
                            ProfileRowItem(icon = Icons.Default.Wc, label = "Jenis Kelamin", value = genderText)
                        }
                    }
                }

                // 3. KARTU STATUS KEAMANAN / BIOMETRIK
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Keamanan & Verifikasi", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))

                            // LOGIKA PENGECEKAN BIOMETRIK
                            // Asumsi properti ini ada di EmployeeEntity (sesuai struktur DB)
                            val isBiometricReady = myProfile.ktpMatchScore != null && !myProfile.faceEmbedding.isNullOrEmpty()

                            val bioIcon = if (isBiometricReady) Icons.Default.VerifiedUser else Icons.Default.NoAccounts
                            val bioTitle = "Status Biometrik"
                            val bioValue = if (isBiometricReady) "Sudah Terverifikasi" else "Belum Lengkap"
                            val bioColor = if (isBiometricReady) Color(0xFF2E7D32) else Color.Red

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(bioColor.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(bioIcon, null, tint = bioColor, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bioTitle, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
                                    Text(bioValue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = bioColor)
                                }
                            }
                        }
                    }
                }

                // 4. KARTU INFORMASI PEKERJAAN
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Penempatan Kerja", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))

                            val storeName = myProfile.stores.firstOrNull()?.name ?: "Belum ditempatkan"
                            ProfileRowItem(icon = Icons.Default.Storefront, label = "Toko / Cabang", value = storeName)
                        }
                    }
                }
            }
        } else if (viewModel.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = viewModel.errorMessage!!, color = Color.Red)
            }
        }
    }
}

// Custom composable (Tidak berubah)
@Composable
fun ProfileRowItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(WonderBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = WonderOrange, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = WonderGrey)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}