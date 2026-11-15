package id.biz.yuka.performax.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Data model khusus untuk marker karyawan pada LiveLocationLeafletMap.
 */
@Serializable
data class LiveLocationMapMarker(
    val userId: String = "",
    val lat: Double,
    val lng: Double,
    val title: String = "",
    val photoUrl: String = "",
    val phone: String = "",
    val timeText: String = ""
)

/**
 * Data model khusus untuk lokasi toko & radius absen pada LiveLocationLeafletMap.
 */
@Serializable
data class StoreMapMarker(
    val storeId: String = "",
    val name: String = "",
    val lat: Double,
    val lng: Double,
    val radiusInMeters: Double = 50.0
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveLocationLeafletMap(
    modifier: Modifier = Modifier,
    markers: List<LiveLocationMapMarker> = emptyList(),
    stores: List<StoreMapMarker> = emptyList(),
    isShowStores: Boolean = false,
    zoomLevel: Int = 15,
    onMapClick: ((lat: Double, lng: Double) -> Unit)? = null,
    isInteractive: Boolean = true,
    selectedUserId: String? = null
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
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>

            <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.4.1/dist/MarkerCluster.css" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.4.1/dist/MarkerCluster.Default.css" />
            <script src="https://unpkg.com/leaflet.markercluster@1.4.1/dist/leaflet.markercluster.js"></script>

            <style>
                * { box-sizing: border-box; }
                html, body { height: 100%; width: 100%; margin: 0; padding: 0; background-color: #f1f5f9; }
                #map { height: 100%; width: 100%; touch-action: ${if (isInteractive) "auto" else "none"} !important; }

                /* 🟢 Circular Marker Style - Bulletproof Rounding */
                .leaflet-marker-icon.custom-leaflet-marker { background: none !important; border: none !important; }
                .profile-marker-pin { position: relative; width: 52px; height: 60px; display: flex; flex-direction: column; align-items: center; }
                
                .profile-marker-avatar {
                    width: 48px; height: 48px; border-radius: 50%; border: 3.5px solid #2563eb;
                    background-size: cover; background-position: center; background-repeat: no-repeat;
                    background-color: #e2e8f0; box-shadow: 0 3px 8px rgba(0,0,0,0.3);
                }
                .profile-marker-arrow { 
                    width: 0; height: 0; border-left: 8px solid transparent; border-right: 8px solid transparent; 
                    border-top: 10px solid #2563eb; margin-top: -2px; 
                }

                .store-badge { background: rgba(30, 41, 59, 0.9); color: #fff; font-size: 10px; padding: 2px 8px; border-radius: 4px; border: 1px solid #FE8F33; white-space: nowrap; box-shadow: 0 2px 4px rgba(0,0,0,0.3); }

                /* 👥 Cluster Style */
                .cluster-container { display: flex; flex-direction: column; align-items: center; justify-content: center; }
                .cluster-avatars { display: flex; align-items: center; background: #ffffff; padding: 3px; border-radius: 30px; border: 2.5px solid #2563eb; box-shadow: 0 4px 12px rgba(0,0,0,0.25); }
                .cluster-avatar-item { 
                    width: 34px; height: 34px; border-radius: 50%; border: 2px solid #fff; margin-left: -14px;
                    background-size: cover; background-position: center; background-repeat: no-repeat; background-color: #f1f5f9;
                }
                .cluster-avatar-item:first-child { margin-left: 0; }
                .cluster-more { width: 34px; height: 34px; border-radius: 50%; background: #e2e8f0; color: #334155; font-size: 0.75rem; display: flex; align-items: center; justify-content: center; font-weight: bold; border: 2px solid #fff; margin-left: -14px; }
                .cluster-count-badge { margin-top: -6px; background: #ef4444; color: white; font-size: 0.7rem; font-weight: bold; padding: 2px 8px; border-radius: 12px; z-index: 10; box-shadow: 0 2px 4px rgba(0,0,0,0.2); }

                /* 💳 Popup Card Style */
                .info-card { min-width: 240px; padding: 4px; display: flex; flex-direction: column; }
                .info-card-header { display: flex; align-items: center; gap: 12px; }
                .info-card .name { font-weight: 800; font-size: 1.05rem; color: #1e293b; margin: 0; display: block; }
                .info-card .phone { font-size: 0.85rem; color: #64748b; font-weight: 500; display: block; margin-top: 1px; }
                .info-card .time { font-size: 0.75rem; color: #2563eb; font-weight: 600; margin-top: 3px; display: block; }
                .btn-maps { display: block; margin-top: 12px; padding: 10px; background: #eff6ff; color: #2563eb; text-align: center; border-radius: 10px; text-decoration: none; font-weight: 800; font-size: 0.85rem; border: 1.5px solid #bfdbfe; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var DEFAULT_AVATAR = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100' viewBox='0 0 100 100'><circle cx='50' cy='50' r='50' fill='%23cbd5e1'/><circle cx='50' cy='38' r='20' fill='%2364748b'/><path d='M 18 88 C 18 68, 32 60, 50 60 C 68 60, 82 68, 82 88 Z' fill='%2364748b'/></svg>";
                var map, markerMap = {}, storeLayer, markersGroup;

                function init() {
                    map = L.map('map', { zoomControl: false }).setView([-7.5787, 110.7248], $zoomLevel);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
                    storeLayer = L.layerGroup().addTo(map);
                    markersGroup = L.markerClusterGroup({
                        maxClusterRadius: 45,
                        iconCreateFunction: function(cluster) {
                            var markers = cluster.getAllChildMarkers();
                            var count = markers.length;
                            var html = '<div class="cluster-container"><div class="cluster-avatars">';
                            for(var i=0; i<Math.min(count, 3); i++) {
                                var img = markers[i].options.profileUrl || DEFAULT_AVATAR;
                                html += '<div class="cluster-avatar-item" style="background-image: url(\'' + img + '\')"></div>';
                            }
                            if(count > 3) html += '<div class="cluster-more">+' + (count-3) + '</div>';
                            html += '</div><div class="cluster-count-badge">' + count + ' Karyawan</div></div>';
                            return L.divIcon({ html: html, className: '', iconSize: [80, 60], iconAnchor: [40, 30] });
                        }
                    }).addTo(map);

                    map.on('click', function(e) { if(window.Android) window.Android.onMapClick(e.latlng.lat, e.latlng.lng); });
                }

                window.updateMarkers = function(b64) {
                    try {
                        var json = decodeURIComponent(escape(atob(b64)));
                        var data = JSON.parse(json);
                        markersGroup.clearLayers();
                        markerMap = {};
                        if(data.length === 0) return;

                        var bounds = L.latLngBounds();
                        data.forEach(function(d) {
                            if(!d.lat || !d.lng || d.lat === 0) return;
                            
                            var imgUrl = d.photoUrl || DEFAULT_AVATAR;
                            var icon = L.divIcon({
                                className: '',
                                html: '<div class="profile-marker-pin"><div class="profile-marker-avatar" style="background-image: url(\'' + imgUrl + '\')"></div><div class="profile-marker-arrow"></div></div>',
                                iconSize: [52, 60], iconAnchor: [26, 60], popupAnchor: [0, -55]
                            });

                            var popup = '<div class="info-card">' +
                                '<div class="info-card-header">' +
                                '<div class="profile-marker-avatar" style="width:52px; height:52px; border-width:2.5px; background-image: url(\'' + imgUrl + '\')"></div>' +
                                '<div>' +
                                '<span class="name">' + d.title + '</span>' +
                                '<span class="phone">' + (d.phone || '-') + '</span>' +
                                '<span class="time">' + d.timeText + '</span>' +
                                '</div>' +
                                '</div>' +
                                '<a href="#" onclick="window.Android.openGoogleMaps(' + d.lat + ',' + d.lng + ');return false;" class="btn-maps">Buka Google Maps</a>' +
                                '</div>';
                            
                            var m = L.marker([d.lat, d.lng], { icon: icon, profileUrl: d.photoUrl });
                            m.bindPopup(popup);
                            markersGroup.addLayer(m);
                            markerMap[d.userId] = m;
                            bounds.extend([d.lat, d.lng]);
                        });

                        if(data.length > 1) map.fitBounds(bounds, {padding: [70, 70]});
                        else if(data.length === 1) map.setView([data[0].lat, data[0].lng], 16);
                    } catch(e) { console.error("JS Error updateMarkers:", e.message); }
                }

                window.updateStores = function(b64, show) {
                    storeLayer.clearLayers();
                    if(!show) return;
                    try {
                        var data = JSON.parse(decodeURIComponent(escape(atob(b64))));
                        data.forEach(function(s) {
                            L.circle([s.lat, s.lng], { color: '#FE8F33', radius: s.radiusInMeters || 50, weight: 2, fillOpacity: 0.1 }).addTo(storeLayer);
                            var icon = L.divIcon({ className: '', html: '<div class="store-badge">' + s.name + '</div>', iconAnchor: [45, 12] });
                            L.marker([s.lat, s.lng], { icon: icon }).addTo(storeLayer);
                        });
                    } catch(e) { console.error("JS Error updateStores:", e.message); }
                }

                window.focusUser = function(id) {
                    var m = markerMap[id];
                    if(m) { markersGroup.zoomToShowLayer(m, function() { m.openPopup(); }); }
                }

                init();
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    LaunchedEffect(markers, isMapLoaded) {
        if (isMapLoaded && markers.isNotEmpty()) {
            try {
                val json = Json.encodeToString(markers)
                val b64 = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
                delay(300)
                webViewRef?.evaluateJavascript("window.updateMarkers('$b64')", null)
            } catch (e: Exception) {
                Log.e("LiveLocationMap", "Failed to encode markers: ${e.message}")
            }
        }
    }

    LaunchedEffect(stores, isShowStores, isMapLoaded) {
        if (isMapLoaded && stores.isNotEmpty()) {
            try {
                val json = Json.encodeToString(stores)
                val b64 = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
                webViewRef?.evaluateJavascript("window.updateStores('$b64', $isShowStores)", null)
            } catch (e: Exception) {
                Log.e("LiveLocationMap", "Failed to encode stores: ${e.message}")
            }
        }
    }

    LaunchedEffect(selectedUserId, isMapLoaded) {
        if (isMapLoaded && !selectedUserId.isNullOrEmpty()) {
            webViewRef?.evaluateJavascript("window.focusUser('$selectedUserId')", null)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                addJavascriptInterface(object {
                    @JavascriptInterface fun onMapClick(lat: Double, lng: Double) { currentOnMapClick?.invoke(lat, lng) }
                    @JavascriptInterface fun openGoogleMaps(lat: Double, lng: Double) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng")).apply {
                            setPackage("com.google.android.apps.maps")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try { context.startActivity(intent) } catch (e: Exception) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                        }
                    }
                }, "Android")
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                        Log.d("LiveLocationMap_JS", "${cm?.message()} -- line ${cm?.lineNumber()}")
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
        }
    )
}
