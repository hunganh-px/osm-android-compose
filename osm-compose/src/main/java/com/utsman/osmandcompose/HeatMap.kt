package com.utsman.osmandcompose

import android.graphics.Paint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.platform.LocalContext
import org.osmdroid.util.GeoPoint

@Composable
@OsmAndroidComposable
fun HeatMap(
    geoPoints: List<GeoPoint>,
    zoom: Double,
    visible: Boolean = true,
    onHeatmapLoaded: (Paint) -> Unit = {},
) {

    val context = LocalContext.current
    val applier =
        currentComposer.applier as? MapApplier ?: throw IllegalStateException("Invalid Applier")

    ComposeNode<HeatmapNode, MapApplier>(
        factory = {
            val mapView = applier.mapView
            val heatMap = HeatMap(mapView, geoPoints)
            heatMap.apply {
//                setPoints(geoPoints)
//                outlinePaint.color = color.toArgb()
//                outlinePaint.strokeWidth = width
//
//                outlinePaint.strokeCap = when (cap) {
//                    PolylineCap.BUTT -> Paint.Cap.BUTT
//                    PolylineCap.ROUND -> Paint.Cap.ROUND
//                    PolylineCap.SQUARE -> Paint.Cap.SQUARE
//                }
//
//                isVisible = visible
//                id?.let { this.id = id }
//                calculateHeatMap()
//                mapView.overlayManager.add(0, this.heatPoints)
//                onHeatmapLoaded.invoke(outlinePaint)

//                infoWindow = null
            }

//            val composeView = ComposeView(context)
//                .apply {
//                    setContent {
//                        infoWindowContent.invoke(InfoWindowData(title.orEmpty(), snippet.orEmpty()))
//                    }
//                }
//
//            val infoWindow = OsmInfoWindow(composeView, mapView)
//            infoWindow.view.setOnClickListener {
//                if (infoWindow.isOpen) infoWindow.close()
//            }
//            polyline.infoWindow = infoWindow

            HeatmapNode(mapView, heatMap)
        }, update = {
            set(geoPoints) {
                heatMap.geoPoints = it
                map.overlayManager.remove(heatMap.heatPoints)
                heatMap.calculateHeatMap()
                map.overlayManager.add(0, heatMap.heatPoints)
            }
            set(zoom) {
                map.overlayManager.remove(heatMap.heatPoints)
                heatMap.calculateHeatMap()
                map.overlayManager.add(0, heatMap.heatPoints)
            }

            update(visible) {
                heatMap.heatPoints?.isEnabled = visible
            }
        })
}