package com.tracmap.mapboxtestapp

import android.app.Activity
import android.os.Bundle
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.db.insertOrThrow
import java.util.*
import kotlin.coroutines.CoroutineContext

private const val DATABASE_INSERT_DELAY_MS = 20L

private val LOG_TAG = MainActivity::class.java.simpleName

class MainActivity : Activity(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    private val mapInstance: MapboxMap by lazy { findViewById<MapView>(R.id.map_view).getMapboxMap() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        insertTestDataIntoDatabase()

        mapInstance.loadStyleUri(Style.SATELLITE)
        simulatePanningMap()
    }

    private fun insertTestDataIntoDatabase() {
        launch(Dispatchers.IO) {
            while (true) {
                dbHelper.use {
                    insertOrThrow(TEST_TABLE_NAME, COLUMN_1 to UUID.randomUUID().toString())
                }
                delay(DATABASE_INSERT_DELAY_MS)
            }
        }
    }

    private fun simulatePanningMap() {
        launch {
            var lat = 10.0
            var lon = -5.0
            val deltaLat = 0.005
            val deltaLon = 0.01

            while (true) {
                moveCamera(lat, lon)
                lat += deltaLat
                lon += deltaLon
                /** Delay for 16ms to simulate screen running at 60fps */
                delay(16)
            }
        }
    }

    private fun moveCamera(topLeftLat: Double, topLeftLon: Double) {
        val bottomRightLat = topLeftLat + 0.1
        val bottomRightLon = topLeftLon + 0.1

        val cameraOptions = mapInstance.cameraForCoordinates(
            listOf(fromLngLat(topLeftLon, topLeftLat), fromLngLat(bottomRightLon, bottomRightLat)),
            EdgeInsets(0.0, 0.0, 0.0, 0.0),
            0.0,
            0.0
        )

        mapInstance.setCamera(cameraOptions)
        /** Uncomment this line, and comment out the one above for testing with older mapbox versions */
//        mapInstance.jumpTo(cameraOptions)
    }
}