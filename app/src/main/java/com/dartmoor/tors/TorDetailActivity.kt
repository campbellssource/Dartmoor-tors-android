package com.dartmoor.tors

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TorDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TOR_ID = "extra_tor_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tor_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val torId = intent.getIntExtra(EXTRA_TOR_ID, -1)
        val tor = TorRepository.getTorById(torId)

        if (tor != null) {
            supportActionBar?.title = tor.name
            
            findViewById<TextView>(R.id.detail_name).text = tor.name
            findViewById<TextView>(R.id.detail_height).text = 
                "Height: ${tor.heightMeters}m (${tor.heightFeet}ft)"
            findViewById<TextView>(R.id.detail_grid_ref).text = 
                "Grid Reference: ${tor.gridReference}"
            findViewById<TextView>(R.id.detail_coordinates).text = 
                "Coordinates: ${String.format("%.4f", tor.latitude)}, ${String.format("%.4f", tor.longitude)}"
            findViewById<TextView>(R.id.detail_description).text = tor.description
        } else {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
