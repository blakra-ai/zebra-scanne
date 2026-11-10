package com.stankovic.zebrascanner

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.stankovic.zebrascanner.config.ScanningConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZebraScannerApplication : Application() {

	private val PROFILE_NAME = "ZebraScannerProfile"
	private val DW_INTENT = "com.symbol.datawedge.api.ACTION"

	override fun onCreate() {
		super.onCreate()
		try {
			configureDataWedgeProfile()
		} catch (e: Exception) {
			Log.w("ZebraApp", "Failed to create/configure DataWedge profile: ${e.message}")
		}
	}

	private fun configureDataWedgeProfile() {
		// Build config bundle according to DataWedge API
		val config = Bundle().apply {
			putString("PROFILE_NAME", PROFILE_NAME)
			putString("PROFILE_ENABLED", "true")
			putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST") // creates the profile, or updates it if it already exists

			// Configure barcode plugin
			val barcodePlugin = Bundle().apply {
				putString("PLUGIN_NAME", "BARCODE")
				putString("RESET_CONFIG", "true")
				val barcodeProps = Bundle().apply {
					putString("scanner_selection", "auto")
					putString("scanner_input_enabled", "true")
				}
				putBundle("PARAM_LIST", barcodeProps)
			}

			// Intent output must be its own plugin inside PLUGIN_CONFIG, otherwise DataWedge ignores it
			val intentPlugin = Bundle().apply {
				putString("PLUGIN_NAME", "INTENT")
				putString("RESET_CONFIG", "true")
				val intentProps = Bundle().apply {
					putString("intent_output_enabled", "true")
					putString("intent_action", ScanningConfig.APP_SCANNER_INTENT)
					putString("intent_category", "android.intent.category.DEFAULT")
					putString("intent_delivery", "2") // Broadcast
				}
				putBundle("PARAM_LIST", intentProps)
			}

			// Stop DataWedge from also typing the barcode as keystrokes
			val keystrokePlugin = Bundle().apply {
				putString("PLUGIN_NAME", "KEYSTROKE")
				putString("RESET_CONFIG", "true")
				putBundle("PARAM_LIST", Bundle().apply { putString("keystroke_output_enabled", "false") })
			}
			putParcelableArray("PLUGIN_CONFIG", arrayOf(barcodePlugin, intentPlugin, keystrokePlugin))

			// Associate this app (all activities) with the profile
			val appConfig = Bundle().apply {
				putString("PACKAGE_NAME", packageName)
				putStringArray("ACTIVITY_LIST", arrayOf("*"))
			}
			putParcelableArray("APP_LIST", arrayOf(appConfig))
		}

		val i = Intent()
		i.action = DW_INTENT
		i.putExtra("com.symbol.datawedge.api.SET_CONFIG", config)
		sendBroadcast(i)
		Log.i("ZebraApp", "Sent SET_CONFIG for $PROFILE_NAME")
	}
}