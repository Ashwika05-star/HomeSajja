package com.homesajja.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.homesajja.app.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/*
 * The map uses OpenStreetMap through osmdroid: no API key and no billing account, unlike Google Maps.
 * Vendors choose their shop location here, and the public vendor profile shows it as a pin.
 */

private const val SHOP_ZOOM = 16.0
private const val CITY_ZOOM = 12.0

/** Read-only map with one pin, for a vendor's public profile. */
@Composable
fun ShopMap(latitude: Double, longitude: Double, modifier: Modifier = Modifier) {
    val point = GeoPoint(latitude, longitude)
    OsmMap(modifier = modifier, center = point, zoom = SHOP_ZOOM) { map ->
        map.overlays.clear()
        map.overlays.add(
            Marker(map).apply {
                position = point
                icon = ContextCompat.getDrawable(map.context, R.drawable.ic_shop_pin)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            },
        )
        map.controller.setCenter(point)
    }
}

/**
 * Map for choosing a location: a pin stays fixed in the middle and the person moves the map under
 * it. [onCenterChanged] reports the coordinates under the pin whenever the map stops moving.
 */
@Composable
fun LocationPickerMap(
    latitude: Double,
    longitude: Double,
    hasPin: Boolean,
    onCenterChanged: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        OsmMap(
            modifier = Modifier.matchParentSize(),
            center = GeoPoint(latitude, longitude),
            zoom = if (hasPin) SHOP_ZOOM else CITY_ZOOM,
            onListen = { map ->
                map.addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        val centre = map.mapCenter
                        onCenterChanged(centre.latitude, centre.longitude)
                        return false
                    }

                    override fun onZoom(event: ZoomEvent?): Boolean = false
                })
            },
        )
        // Offset up by half its height so the pin's tip, not its middle, marks the centre.
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = "Shop location pin",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Center).offset(y = (-20).dp).size(40.dp),
        )
    }
}

/** Shared osmdroid MapView wrapper: tile config, lifecycle, and letting the map take over touch from a scrolling parent. */
@SuppressLint("ClickableViewAccessibility")
@Composable
private fun OsmMap(
    center: GeoPoint,
    zoom: Double,
    modifier: Modifier = Modifier,
    onListen: (MapView) -> Unit = {},
    onUpdate: (MapView) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        configureOsm(context)
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(zoom)
            controller.setCenter(center)
            // Without this, a vertically scrolling parent steals the drag before the map sees it.
            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) view.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
            onListen(this)
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier, update = { onUpdate(it) })
}

/**
 * Points osmdroid at the app's cache folder (so no storage permission is needed) and gives tile servers a user agent.
 * It touches the disk, so the app calls it once on a background thread at start-up; later calls do nothing.
 */
object OsmSetup {
    @Volatile
    private var configured = false

    @Synchronized
    fun configure(context: Context) {
        if (configured) return
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = context.cacheDir.resolve("osm-tiles")
        }
        // Opening the tile cache database is the slow part of the first map; do it now, where it can't stall the screen.
        runCatching { org.osmdroid.tileprovider.modules.SqlTileWriter().onDetach() }
        configured = true
    }
}

private fun configureOsm(context: Context) = OsmSetup.configure(context)
