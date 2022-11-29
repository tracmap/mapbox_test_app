package com.tracmap.mapboxtestapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import kotlinx.coroutines.*
import org.jetbrains.anko.db.insertOrThrow
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

private const val DATABASE_INSERT_DELAY_MS = 20L

private const val INITIAL_LATITUDE = 10.0
private const val INITIAL_LONGITUDE = -5.0

private val LOG_TAG = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    private val mapInstance: MapboxMap by lazy { findViewById<MapView>(R.id.map_view).getMapboxMap() }

    var running = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        insertTestDataIntoDatabase()

        mapInstance.loadStyleUri(Style.SATELLITE)
        simulatePanningMap()
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    private fun insertTestDataIntoDatabase() {
        launch(Dispatchers.IO) {
            while (running) {
                dbHelper.use {
                    insertOrThrow(TEST_TABLE_NAME, COLUMN_1 to UUID.randomUUID().toString())
                }
                delay(DATABASE_INSERT_DELAY_MS)
            }
        }
    }



    private fun simulatePanningMap() {
        launch {
            var lat = INITIAL_LATITUDE
            var lon = INITIAL_LONGITUDE
            val deltaLat = 0.005
            val deltaLon = 0.01

            while (running) {
                moveCamera(lat, lon)
                lat += deltaLat
                lon += deltaLon

                /** Reset screen if we are getting close to panning to an invalid lat, lon */
                if (!isValidCoords(lon + 1, lat + 1)) {
                    Log.d(LOG_TAG, "Resetting coords")
                    lat = INITIAL_LATITUDE
                    lon = INITIAL_LONGITUDE
                }
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

    private fun isValidCoords(lon: Double, lat: Double): Boolean {
        return abs(lat) < 90.0 && abs(lon) < 360.0
    }
}