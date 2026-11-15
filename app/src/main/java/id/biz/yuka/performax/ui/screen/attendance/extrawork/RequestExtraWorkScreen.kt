package id.biz.yuka.performax.ui.screen.attendance.extrawork

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.*
import id.biz.yuka.performax.ui.components.GlobalErrorPopup
import id.biz.yuka.performax.ui.theme.WonderBg
import id.biz.yuka.performax.ui.theme.WonderGrey
import id.biz.yuka.performax.ui.theme.WonderOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestExtraWorkScreen(
    onBack: () -> Unit,
    onSuccessNavigateToHome: () -> Unit = {},
    viewModel: RequestExtraWorkViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var expandedStore by remember { mutableStateOf(false) }
    var expandedShift by remember { mutableStateOf(false) }

    var hasLocationPermission by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.capturedImage = bitmap
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission || !hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA))
        }
    }

    DisposableEffect(hasLocationPermission) {
        var locationCallback: LocationCallback? = null
        if (hasLocationPermission) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { viewModel.updateLocation(it.latitude, it.longitude) }
                }
            }
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            }
        }
        onDispose { locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) } }
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.successMessage = null
            onSuccessNavigateToHome()
        }
    }

    Scaffold(
        containerColor = WonderBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ajukan Masuk Kerja", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WonderBg)
            )
        }
    ) { padding ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WonderOrange)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Pilih Toko & Shift
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = WonderOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Penempatan Toko & Shift", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Dropdown Toko
                            ExposedDropdownMenuBox(
                                expanded = expandedStore,
                                onExpandedChange = { expandedStore = it }
                            ) {
                                OutlinedTextField(
                                    value = viewModel.selectedStore?.name ?: "Pilih Toko",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Pilih Toko") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStore) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedStore,
                                    onDismissRequest = { expandedStore = false }
                                ) {
                                    viewModel.availableStores.forEach { store ->
                                        DropdownMenuItem(
                                            text = { Text(store.name) },
                                            onClick = {
                                                viewModel.onStoreSelected(store)
                                                expandedStore = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Dropdown Shift
                            ExposedDropdownMenuBox(
                                expanded = expandedShift,
                                onExpandedChange = { expandedShift = it }
                            ) {
                                val selectedShiftText = viewModel.selectedShift?.let { shift ->
                                    "${shift.shiftName} (${shift.startTime.take(5)} - ${shift.endTime.take(5)})"
                                } ?: if (viewModel.isShiftsLoading) "Memuat shift..." else "Pilih Shift"

                                OutlinedTextField(
                                    value = selectedShiftText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Pilih Shift Kerja") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedShift) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedShift,
                                    onDismissRequest = { expandedShift = false }
                                ) {
                                    if (viewModel.availableShifts.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Tidak ada shift tersedia") },
                                            onClick = { expandedShift = false }
                                        )
                                    } else {
                                        viewModel.availableShifts.forEach { shift ->
                                            DropdownMenuItem(
                                                text = { Text("${shift.shiftName} (${shift.startTime.take(5)} - ${shift.endTime.take(5)})") },
                                                onClick = {
                                                    viewModel.onShiftSelected(shift)
                                                    expandedShift = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Alasan Masuk Kerja
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = WonderOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Alasan Masuk Kerja", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = viewModel.reason,
                                onValueChange = { viewModel.reason = it },
                                placeholder = { Text("Contoh: Menggantikan teman shift, lembur mendadak, dll.") },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // 3. Foto Bukti & Lokasi GPS
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = WonderOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Foto Bukti & Lokasi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (viewModel.capturedImage != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.LightGray)
                                ) {
                                    Image(
                                        bitmap = viewModel.capturedImage!!.asImageBitmap(),
                                        contentDescription = "Preview Foto Bukti",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { viewModel.capturedImage = null },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        if (hasCameraPermission) {
                                            cameraLauncher.launch()
                                        } else {
                                            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WonderOrange)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ambil Foto Kamera Bukti", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Info GPS
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = WonderOrange, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                val locText = viewModel.userLocation?.let {
                                    "${String.format(java.util.Locale.US, "%.5f", it.lat)}, ${String.format(java.util.Locale.US, "%.5f", it.lng)}"
                                } ?: "Mencari lokasi GPS..."
                                Text("GPS: $locText", fontSize = 12.sp, color = WonderGrey, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // 4. Tombol Submit
                item {
                    Button(
                        onClick = { viewModel.submitRequest() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WonderOrange),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !viewModel.isSubmitting
                    ) {
                        if (viewModel.isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Kirim Pengajuan Masuk Kerja", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    GlobalErrorPopup(
        errorMessage = viewModel.errorMessage,
        onDismiss = { viewModel.errorMessage = null }
    )
}
