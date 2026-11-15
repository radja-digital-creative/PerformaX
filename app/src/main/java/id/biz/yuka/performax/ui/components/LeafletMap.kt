package id.biz.yuka.performax.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

data class MapMarker(
    val lat: Double,
    val lng: Double,
    val title: String = ""
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMap(
    modifier: Modifier = Modifier,
    markers: List<MapMarker> = emptyList(),
    radiusInMeters: Double? = null,
    circleCenter: MapMarker? = null,
    zoomLevel: Int = 15,
    onMapClick: ((lat: Double, lng: Double) -> Unit)? = null,
    isInteractive: Boolean = true // ✅ TAMBAHAN: Kontrol Interaktivitas
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    
    val currentOnMapClick by rememberUpdatedState(onMapClick)

    val htmlContent = remember(isInteractive) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" crossorigin=""/>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" crossorigin=""></script>
            <style>
                html, body { height: 100%; width: 100%; margin: 0; padding: 0; background-color: #e5e5e5; }
                #map { height: 100%; width: 100%; touch-action: ${if (isInteractive) "auto" else "none"} !important; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                console.log("Memulai inisialisasi script JS...");
                try {
                    var map = L.map('map', {
                        zoomControl: false,
                        dragging: $isInteractive,
                        touchZoom: $isInteractive,
                        scrollWheelZoom: $isInteractive,
                        doubleClickZoom: $isInteractive
                    }).setView([0, 0], $zoomLevel);
                    
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap'
                    }).addTo(map);

                    var markerList = [];
                    var currentCircle = null;

                    map.on('click', function(e) {
                        var lat = e.latlng.lat;
                        var lng = e.latlng.lng;
                        console.log("Map clicked: " + lat + ", " + lng);
                        if (window.Android) {
                            window.Android.onMapClick(lat, lng);
                        }
                    });

                    window.setMarkers = function(markersJson) {
                        console.log("Menerima data marker: " + markersJson);
                        markerList.forEach(m => map.removeLayer(m));
                        markerList = [];
                        
                        var markers = JSON.parse(markersJson);
                        if (markers.length > 0) {
                            var group = [];
                            markers.forEach(data => {
                                var m = L.marker([data.lat, data.lng]).addTo(map);
                                if (data.title) m.bindPopup(data.title);
                                markerList.push(m);
                                group.push([data.lat, data.lng]);
                            });
                            
                            if (group.length > 0) {
                                if (group.length === 1) {
                                    map.setView(group[0], map.getZoom());
                                } else {
                                    map.fitBounds(group, { padding: [50, 50] });
                                }
                            }
                        }
                    }

                    window.setCircle = function(lat, lng, radius) {
                        console.log("Menggambar radius " + radius + "m di " + lat + "," + lng);
                        if (currentCircle) map.removeLayer(currentCircle);
                        if (radius > 0) {
                            currentCircle = L.circle([lat, lng], {
                                color: '#FE8F33',
                                fillColor: '#FE8F33',
                                fillOpacity: 0.2,
                                radius: radius
                            }).addTo(map);
                        }
                    }
                    console.log("Inisialisasi JS Leaflet sukses.");
                } catch(e) {
                    console.error("CRASH di JS: " + e.message);
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    LaunchedEffect(markers, isMapLoaded) {
        if (isMapLoaded) {
            val json = markers.joinToString(prefix = "[", postfix = "]") {
                "{\"lat\":${it.lat}, \"lng\":${it.lng}, \"title\":\"${it.title}\"}"
            }
            webViewRef?.evaluateJavascript("window.setMarkers('$json')", null)
        }
    }

    LaunchedEffect(radiusInMeters, circleCenter, isMapLoaded) {
        if (isMapLoaded && circleCenter != null && radiusInMeters != null) {
            webViewRef?.evaluateJavascript("window.setCircle(${circleCenter.lat}, ${circleCenter.lng}, $radiusInMeters)", null)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onMapClick(lat: Double, lng: Double) {
                        currentOnMapClick?.invoke(lat, lng)
                    }
                }, "Android")

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let { Log.d("Leaflet_JS", "${it.message()} -- line ${it.lineNumber()}") }
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isMapLoaded = true
                    }
                }

                loadDataWithBaseURL("http://localhost", htmlContent, "text/html", "UTF-8", null)
                webViewRef = this
            }
        },
        update = { webView ->
            // Update base URL if needed when interactive state changes
        }
    )
}
