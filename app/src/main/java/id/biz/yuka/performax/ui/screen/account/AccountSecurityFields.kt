package id.biz.yuka.performax.ui.screen.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun NewPasswordField(value: String, onValueChange: (String) -> Unit, label: String = "Password Baru (Opsional)", enabled: Boolean = true) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(value.isEmpty()) { if (value.isEmpty()) visible = false }
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        enabled = enabled, singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Sembunyikan password baru" else "Tampilkan password baru")
            }
        }
    )
}

@Composable
fun AccountSecurityFields(currentEmail: String, email: String, onEmailChange: (String) -> Unit,
                          password: String, onPasswordChange: (String) -> Unit,
                          saving: Boolean, onSave: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Akun & Keamanan", style = MaterialTheme.typography.titleMedium)
        Text("Email saat ini: ${currentEmail.ifBlank { "Belum tersedia" }}")
        OutlinedTextField(value = email, onValueChange = onEmailChange,
            label = { Text("Email Baru (Opsional)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !saving, modifier = Modifier.fillMaxWidth())
        NewPasswordField(password, onPasswordChange, enabled = !saving)
        Text("Kosongkan kolom yang tidak ingin diubah. Password lama tidak dapat ditampilkan.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = onSave, enabled = !saving && (email.isNotBlank() || password.isNotBlank())) {
            Text(if (saving) "Menyimpan…" else "Simpan Akun")
        }
    }
}
