package com.utsman.osmandcompose

import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.OverlayItem

class HeatmapNode(
    val map: OsmMapView,
    val heatMap: HeatMap
) : OsmAndNode {


    override fun onRemoved() {
        super.onRemoved()
        map.overlayManager.remove(heatMap.heatPoints)
    }
}

open class HeatMap(val map: MapView, var geoPoints: List<GeoPoint> = listOf<GeoPoint>()) {
    var heatPoints: ItemizedIconOverlay<OverlayItem>? = null
    var enabled: Boolean = true
        set(value) {
            heatPoints?.isEnabled = value
        }
    var groupList = mutableListOf<PointGroup>()
    val COLOR_ARRAY = arrayOf("#AA00FFAA", "#AA00FF00", "#AAFFFF00", "#AAFF0000")

    //private ItemizedIconOverlay heatPoints;
    fun calculateHeatMap() {
        checkGroup(groupList, geoPoints)

        val list = mutableListOf<OverlayItem>()

        for (group in groupList) {
            val min = minOf(4, group.points.size)
            val colorArr = IntArray(min + 2)
            for (i in 0 until min) {
                colorArr[i + 1] = android.graphics.Color.parseColor(COLOR_ARRAY[min - 1 - i])
            }
            colorArr[min] = android.graphics.Color.parseColor("#000000FF")
            colorArr[0] = android.graphics.Color.parseColor(COLOR_ARRAY[min - 1])

            val baseDrawable = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, colorArr)
            baseDrawable.gradientType = GradientDrawable.RADIAL_GRADIENT
            baseDrawable.setSize(group.radius * 2, group.radius * 2)
            baseDrawable.gradientRadius = group.radius.toFloat()

            val geoPoint = map.projection.fromPixels(
                group.centerLatitude,
                group.centerLongitude + group.radius
            ) as GeoPoint
            val overlayItem = OverlayItem("", "", geoPoint)
            overlayItem.setMarker(baseDrawable)
            list.add(overlayItem)
        }

        heatPoints = ItemizedIconOverlay<OverlayItem>(
            map.context!!,
            list,
            object : OnItemGestureListener<OverlayItem> {
                override fun onItemSingleTapUp(
                    index: Int,
                    item: OverlayItem?
                ): Boolean {
                    return false
                }

                override fun onItemLongPress(
                    index: Int,
                    item: OverlayItem?
                ): Boolean {
                    return false
                }

            })

        // Random rnd = new Random()

//        loadItemsToMap()
    }

    private fun loadItemsToMap() {
        map.overlays.clear()
//    if (elemHeatmap.isChecked) {
        map.overlays.add(heatPoints)
//    }
        // heatmap.visibility = if (elemHeatmap.isChecked) View.VISIBLE else View.INVISIBLE
//    if (elemPolyline.isChecked) {
//        map.overlays.add(polyline)
//    }
//    if (elemMarker.isChecked) {
//        map.overlays.addAll(listMarker)
//    }
        map.invalidate()
    }

    private fun checkGroup(
        groupList: MutableList<PointGroup> = mutableListOf(),
        geoPointList: List<GeoPoint>
    ) {
        groupList.clear()
        val list = mutableListOf<Point>()

        for (geoPoint in geoPointList) {
            val point = pointFromGeoPoint(geoPoint)
            point?.let { list.add(it) }
        }

        if (list.isNotEmpty()) {
            val pointGroup = PointGroup()
            pointGroup.checkPoint(list[0])
            groupList.add(pointGroup)
        }

        for (point in list) {
            var found = false
            for (pointGroup in groupList) {
                if (pointGroup.checkPoint(point)) found = true
            }
            if (!found) {
                val pointGroup = PointGroup()
                pointGroup.checkPoint(point)
                groupList.add(pointGroup)
            }
        }
    }

    private fun pointFromGeoPoint(gp: GeoPoint): Point? {
        val rtnPoint = Point()
        val projection = map.projection
        projection.toPixels(gp, rtnPoint)
        // Get the top left GeoPoint
        val geoPointTopLeft = projection.fromPixels(0, 0) as GeoPoint
        val topLeftPoint = Point()
        // Get the top left Point (includes osmdroid offsets)
        projection.toPixels(geoPointTopLeft, topLeftPoint)
        rtnPoint.x -= topLeftPoint.x // remove offsets
        rtnPoint.y -= topLeftPoint.y

        if (rtnPoint.x > map.width || rtnPoint.y > map.height ||
            rtnPoint.x < 0 || rtnPoint.y < 0
        ) {
            return null // gp must be off the screen
        }
        return rtnPoint
    }


    open class MapPoint : GeoPoint {

        var info: String? = null

        constructor(aLatitude: Double, aLongitude: Double) : super(aLatitude, aLongitude)

        constructor(aLatitude: Double, aLongitude: Double, aAltitude: Double) : super(
            aLatitude,
            aLongitude,
            aAltitude
        )

        constructor(aLocation: Location) : super(aLocation)

        constructor(aGeopoint: GeoPoint) : super(aGeopoint)

        constructor(pGeopoint: IGeoPoint) : super(pGeopoint)
    }

    inner class PointGroup() {
        var centerLatitude: Int = 0
        var centerLongitude: Int = 0
        var radius: Int = 50
        var groupRadius: Int = 100
        var points: MutableList<Point> = mutableListOf()

        fun checkPoint(geoPoint: Point): Boolean {
            if (points.size > 5) {
                return false
            }
            if (points.isEmpty()) {
                points.add(geoPoint)
                validateProperties()
                return true
            }

            val calcRadius = Math.hypot(
                (centerLatitude - geoPoint.x).toDouble(),
                (centerLongitude - geoPoint.y).toDouble()
            )
            if (calcRadius <= groupRadius) {
                points.add(geoPoint)
                validateProperties()
                return true
            }
            return false
        }

        private fun validateProperties() {
            if (points.size == 1) {
                centerLatitude = points[0].x
                centerLongitude = points[0].y
            }

            var minLat = map.width
            var minLong = map.height
            var maxLat = 0
            var maxLong = 0
            var maxRadius = radius

            for (point in points) {
                if (point.x <= minLat) minLat = point.x
                if (point.x >= maxLat) maxLat = point.x
                if (point.y <= minLong) minLong = point.y
                if (point.y >= maxLong) maxLong = point.y
            }
            centerLatitude = (minLat + maxLat) / 2
            centerLongitude = (minLong + maxLong) / 2

            for (point in points) {
                val calcRadius = Math.hypot(
                    (centerLatitude - point.x).toDouble(),
                    (centerLongitude - point.y).toDouble()
                ).toInt()
                radius = maxOf(maxRadius, calcRadius * 3)
                groupRadius = radius
                if (points.size > 1) {
                    Log.d("MAP", "validateProperties: ")
                }
            }
        }
    }
}