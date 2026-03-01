package com.dartmoor.tors

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = "Dartmoor Tors"

        recyclerView = findViewById(R.id.tors_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val tors = TorRepository.getAllTors()
        adapter = TorAdapter(tors) { tor ->
            openTorDetail(tor)
        }
        recyclerView.adapter = adapter
    }

    private fun openTorDetail(tor: Tor) {
        val intent = Intent(this, TorDetailActivity::class.java).apply {
            putExtra(TorDetailActivity.EXTRA_TOR_ID, tor.id)
        }
        startActivity(intent)
    }
}
