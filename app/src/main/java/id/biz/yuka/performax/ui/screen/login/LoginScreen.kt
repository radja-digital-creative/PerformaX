package id.biz.yuka.performax.ui.screen.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderLime
import id.biz.yuka.performax.ui.theme.WonderOrange

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    var passwordVisible by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = true) {
        viewModel.loginSuccess.collect {
            onLoginSuccess()
        }
    }

    Scaffold(
        containerColor = WonderBg // Latar belakang dasar khas performaX yang menyatukan seluruh layar
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- 1. ELEMEN DEKORATIF BACKGROUND ---
            // Lingkaran oranye pudar di pojok kiri atas
            Box(
                modifier = Modifier
                    .offset(x = (-50).dp, y = (-40).dp)
                    .size(200.dp)
                    .background(WonderOrange.copy(alpha = 0.15f), CircleShape)
            )
            // Lingkaran lime pudar di kanan atas
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = 120.dp)
                    .size(150.dp)
                    .background(WonderLime.copy(alpha = 0.2f), CircleShape)
            )

            // --- 2. KONTEN UTAMA (Menyatu tanpa sekat) ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start, // Rata kiri untuk teks
                verticalArrangement = Arrangement.Center // Memusatkan seluruh elemen di tengah layar secara vertikal
            ) {

                // HEADER PENYAMBUTAN
                Text(
                    text = "performaX",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = WonderOrange,
                        letterSpacing = (-2).sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Selamat Datang,",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                )
                Text(
                    text = "Silakan masuk untuk mulai mengelola\nperforma tim dan bisnis Anda.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = WonderGrey,
                        lineHeight = 20.sp
                    )
                )

                Spacer(modifier = Modifier.height(48.dp)) // Jarak lega antara teks dan form

                // --- FORM INPUT ---
                // Form Input: Email
                OutlinedTextField(
                    value = viewModel.email,
                    onValueChange = { viewModel.email = it },
                    label = { Text("Alamat Email", color = WonderGrey) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WonderBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = WonderOrange, modifier = Modifier.size(18.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White, // Menggunakan putih murni agar menonjol dari latar belakang WonderBg
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = WonderOrange,
                        unfocusedBorderColor = Color.Transparent, // Border disembunyikan agar terlihat seperti melayang
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Form Input: Password
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it },
                    label = { Text("Password", color = WonderGrey) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WonderBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = WonderOrange, modifier = Modifier.size(18.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    singleLine = true,
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        val description = if (passwordVisible) "Hide password" else "Show password"

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = WonderOrange,
                        unfocusedBorderColor = Color.Transparent,
                    )
                )

                // Pesan Error
                if (viewModel.errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = viewModel.errorMessage!!,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Tombol Login Oranye
                Button(
                    onClick = { viewModel.login() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !viewModel.isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WonderOrange,
                        contentColor = Color.Black, // Teks tebal berwarna hitam untuk kontras yang sempurna
                        disabledContainerColor = Color.LightGray
                    )
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            text = "Login Sekarang",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}