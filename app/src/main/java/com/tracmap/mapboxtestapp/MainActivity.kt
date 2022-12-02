package com.tracmap.mapboxtestapp

import android.content.SharedPreferences
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

private val LOG_TAG = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    private lateinit var prefs: SharedPreferences

    private val mapInstance: MapboxMap by lazy { findViewById<MapView>(R.id.map_view).getMapboxMap() }

    private var running = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = applicationContext.getSharedPreferences("save", MODE_PRIVATE)
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
            // testing done by starting the app over and over, to detect db corruption:
            // store expected rows in prefs and don't run if inconsistency found
            var rows = prefs.getLong("rows", 0L)
            var inserts = 0
            while (running && inserts <= 70) {
                // query keeps the db open so not going to clash with okhttp/okio sockets
                dbHelper.query {
                    val row = insertOrThrow(TEST_TABLE_NAME, COLUMN_1 to UUID.randomUUID().toString())
                    if (rows == 0L) assert(row == 1L) // first insert should be 1
                    prefs.edit().apply() {
                        putLong("rows", row)
                        apply()
                    }
                    rows += 1
                    inserts += 1
                    assert(abs(row - rows) < 10) // a few ids are sometimes skipped after restart

                    // also test closing db once while app is running and stopping the loop
                    // android studio should run project every 6 seconds for test
                    if (inserts == 70) {
                        println("=== closing ===")
                        if (Math.random() > 0.5) close()
                    }
                }
                delay(DATABASE_INSERT_DELAY_MS)
            }
        }
    }



    private fun simulatePanningMap() {
        launch {
            // random start and direction added to prevent cache re-use on automated short runs
            val lat0 = Math.random() * 45 - 22.5
            val lon0 = Math.random() * 90 - 45
            var lat = lat0
            var lon = lon0
            val deltaLat = 0.005 * if (Math.random() > 0.5) -1 else 1
            val deltaLon = 0.01 * if (Math.random() > 0.5) -1 else 1

            while (running) {
                moveCamera(lat, lon)
                lat += deltaLat
                lon += deltaLon

                /** Reset screen if we are getting close to panning to an invalid lat, lon */
                if (!isValidCoords(lon + 1, lat + 1)) {
                    Log.d(LOG_TAG, "Resetting coords")
                    lat = lat0
                    lon = lon0
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