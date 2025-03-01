package com.utsman.osmandcompose

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Runnable
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import java.util.concurrent.ThreadLocalRandom

internal class MapPropertiesNode(
    val mapViewComposed: OsmMapView,
    val mapListeners: MapListeners,
    private val cameraState: CameraState,
    overlayManagerState: OverlayManagerState
) : OsmAndNode {

    private var delayedMapListener: DelayedMapListener? = null
    private var zoomChangeListener: MapListener? = null
    private var eventOverlay: MapEventsOverlay? = null

    private var handler = Handler(Looper.getMainLooper())
    private var zoomCallback: Runnable? = null

    init {
        overlayManagerState.setMap(mapViewComposed)
        cameraState.setMap(mapViewComposed)
        zoomCallback = Runnable {
            mapListeners.onZoomChanged(ThreadLocalRandom.current().nextDouble(0.0, 20.0))
        }
    }

    override fun onAttached() {
        mapViewComposed.controller.setCenter(cameraState.geoPoint)
        mapViewComposed.controller.setZoom(cameraState.zoom)

        delayedMapListener = DelayedMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                val currentGeoPoint =
                    mapViewComposed.let { GeoPoint(it.mapCenter.latitude, it.mapCenter.longitude) }
                cameraState.geoPoint = currentGeoPoint
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                val currentZoom = mapViewComposed.zoomLevelDouble
                cameraState.zoom = currentZoom
                return false
            }
        }, 1000L)

        mapViewComposed.addMapListener(delayedMapListener)

        zoomChangeListener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                zoomCallback?.let {
                    handler.removeCallbacks(it)
                    handler.postDelayed(it, 1000)
                }


                return false
            }

        }

        mapViewComposed.addMapListener(zoomChangeListener)

        val eventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { mapListeners.onMapClick.invoke(it) }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let { mapListeners.onMapLongClick.invoke(it) }
                return true
            }
        }

        eventOverlay = MapEventsOverlay(eventsReceiver)

        mapViewComposed.overlayManager.add(eventOverlay)

        if (mapViewComposed.isLayoutOccurred) {
            mapListeners.onFirstLoadListener.invoke("")
        }
    }

    override fun onCleared() {
        super.onCleared()
        delayedMapListener?.let { mapViewComposed.removeMapListener(it) }
        zoomChangeListener?.let { mapViewComposed.removeMapListener(it) }
        eventOverlay?.let { mapViewComposed.overlayManager.remove(eventOverlay) }
    }

    override fun onRemoved() {
        super.onRemoved()
        delayedMapListener?.let { mapViewComposed.removeMapListener(it) }
        zoomChangeListener?.let { mapViewComposed.removeMapListener(it) }
        eventOverlay?.let { mapViewComposed.overlayManager.remove(eventOverlay) }
    }
}