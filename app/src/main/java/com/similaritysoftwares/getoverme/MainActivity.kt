package com.similaritysoftwares.getoverme

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.similaritysoftwares.getoverme.ui.home.HomeFragment
import com.similaritysoftwares.getoverme.ui.leaderboard.LeaderboardFragment
import com.similaritysoftwares.getoverme.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Use the layout XML directly

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Load the initial fragment
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        // Set up bottom navigation item selection listener
        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> replaceFragment(HomeFragment())
                R.id.navigation_leaderboard -> replaceFragment(LeaderboardFragment())
                R.id.navigation_profile -> replaceFragment(ProfileFragment())
                else -> false
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}