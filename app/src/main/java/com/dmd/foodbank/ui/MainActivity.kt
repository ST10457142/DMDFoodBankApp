package com.dmd.foodbank.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dmd.foodbank.R
import com.dmd.foodbank.data.repository.SessionManager
import com.dmd.foodbank.databinding.ActivityMainBinding
import com.dmd.foodbank.ui.auth.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        binding.bottomNav.setupWithNavController(navController)

        // Show/hide bottom nav based on destination
        navController.addOnDestinationChangedListener { _, _, _ ->
            // Hide bottom nav on screens that don't need it if any
            val hideBottomNav = false 
            binding.bottomNav.visibility = if (hideBottomNav) View.GONE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh bottom nav badge for donations
    }
}
