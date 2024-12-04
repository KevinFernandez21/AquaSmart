package com.example.aquasmart

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.example.aquasmart.databinding.ActivityMainBinding

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.InputStream
import java.io.OutputStream
import java.util.*
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setSupportActionBar(binding.toolbar)

        // val navController = findNavController(R.id.nav_host_fragment_content_main)
        //appBarConfiguration = AppBarConfiguration(navController.graph)
        //setupActionBarWithNavController(navController, appBarConfiguration)

        //binding.fab.setOnClickListener { view ->
        //    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //        .setAction("Action", null)
        //        .setAnchorView(R.id.fab).show()
        //}
    }


}