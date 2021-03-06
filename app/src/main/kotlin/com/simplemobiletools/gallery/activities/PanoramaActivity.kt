package com.simplemobiletools.gallery.activities

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.RelativeLayout
import com.google.vr.sdk.widgets.pano.VrPanoramaEventListener
import com.google.vr.sdk.widgets.pano.VrPanoramaView
import com.simplemobiletools.commons.extensions.beVisible
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.PATH
import kotlinx.android.synthetic.main.activity_panorama.*

open class PanoramaActivity : SimpleActivity() {
    private val CARDBOARD_DISPLAY_MODE = 3

    private var isFullScreen = false
    private var isExploreEnabled = true
    private var isRendering = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panorama)
        supportActionBar?.hide()
        setupButtonMargins()

        cardboard.setOnClickListener {
            panorama_view.displayMode = CARDBOARD_DISPLAY_MODE
        }

        explore.setOnClickListener {
            isExploreEnabled = !isExploreEnabled
            panorama_view.setPureTouchTracking(isExploreEnabled)
            explore.setImageResource(if (isExploreEnabled) R.drawable.ic_explore else R.drawable.ic_explore_off)
        }

        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                checkIntent()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        panorama_view.resumeRendering()
        isRendering = true
        if (config.blackBackground) {
            updateStatusbarColor(Color.BLACK)
        }
    }

    override fun onPause() {
        super.onPause()
        panorama_view.pauseRendering()
        isRendering = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRendering) {
            panorama_view.shutdown()
        }
    }

    private fun checkIntent() {
        val path = intent.getStringExtra(PATH)
        if (path == null) {
            toast(R.string.invalid_image_path)
            finish()
            return
        }

        intent.removeExtra(PATH)

        try {
            val options = VrPanoramaView.Options()
            options.inputType = VrPanoramaView.Options.TYPE_MONO
            Thread {
                val bitmap = getBitmapToLoad(path)
                runOnUiThread {
                    panorama_view.apply {
                        beVisible()
                        loadImageFromBitmap(bitmap, options)
                        setFlingingEnabled(true)
                        setPureTouchTracking(true)
                        setEventListener(object : VrPanoramaEventListener() {
                            override fun onClick() {
                                handleClick()
                            }
                        })

                        // add custom buttons so we can position them and toggle visibility as desired
                        setFullscreenButtonEnabled(false)
                        setInfoButtonEnabled(false)
                        setTransitionViewEnabled(false)
                        setStereoModeButtonEnabled(false)

                        setOnClickListener {
                            handleClick()
                        }
                    }
                }
            }.start()
        } catch (e: Exception) {
            showErrorToast(e)
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            isFullScreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            toggleButtonVisibility()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        setupButtonMargins()
    }

    private fun getBitmapToLoad(path: String): Bitmap? {
        val options = BitmapFactory.Options()
        options.inSampleSize = 1
        var bitmap: Bitmap? = null

        for (i in 0..10) {
            try {
                bitmap = BitmapFactory.decodeFile(path, options)
                break
            } catch (e: OutOfMemoryError) {
                options.inSampleSize *= 2
            }
        }

        return bitmap
    }

    private fun setupButtonMargins() {
        (cardboard.layoutParams as RelativeLayout.LayoutParams).apply {
            bottomMargin = navigationBarHeight
            rightMargin = navigationBarWidth
        }

        (explore.layoutParams as RelativeLayout.LayoutParams).bottomMargin = navigationBarHeight
    }

    private fun toggleButtonVisibility() {
        cardboard.animate().alpha(if (isFullScreen) 0f else 1f)
        cardboard.isClickable = !isFullScreen

        explore.animate().alpha(if (isFullScreen) 0f else 1f)
        explore.isClickable = !isFullScreen
    }

    private fun handleClick() {
        isFullScreen = !isFullScreen
        toggleButtonVisibility()
        if (isFullScreen) {
            hideSystemUI(false)
        } else {
            showSystemUI(false)
        }
    }
}
