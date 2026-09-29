package com.ahmed.hlauncher

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.GridView

class MainActivity : Activity() {

    private lateinit var appAdapter: AppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureWallpaperWindow()
        setContentView(R.layout.activity_main)

        val appGrid = findViewById<GridView>(R.id.appGrid)
        appAdapter = AppAdapter(this)
        appGrid.adapter = appAdapter
        appAdapter.loadApps()
    }

    override fun onDestroy() {
        if (::appAdapter.isInitialized) {
            appAdapter.close()
        }
        super.onDestroy()
    }

    private fun configureWallpaperWindow() {
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}