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
			createDataWedgeProfile()
			configureDataWedgeProfile()
		} catch (e: Exception) {
			Log.w("ZebraApp", "Failed to create/configure DataWedge profile: ${e.message}")
		}
	}

	private fun createDataWedgeProfile() {
		val i = Intent()
		i.action = DW_INTENT
		i.putExtra("com.symbol.datawedge.api.CREATE_PROFILE", PROFILE_NAME)
		sendBroadcast(i)
		Log.i("ZebraApp", "Sent CREATE_PROFILE for $PROFILE_NAME")
	}

	private fun configureDataWedgeProfile() {
		// Build config bundle according to DataWedge API
		val config = Bundle().apply {
			putString("PROFILE_NAME", PROFILE_NAME)
			putString("PROFILE_ENABLED", "true")
			putString("CONFIG_MODE", "UPDATE")

			// Configure barcode plugin
			val barcodePlugin = Bundle().apply {
				putString("PLUGIN_NAME", "BARCODE")
				val barcodeProps = Bundle().apply {
					putString("scanner_selection", "auto")
					putString("scanner_input_enabled", "true")
				}
				putBundle("PARAM_LIST", barcodeProps)
			}
			putParcelableArray("PLUGIN_CONFIG", arrayOf(barcodePlugin))

			// Configure intent output to this app
			val intentOutput = Bundle().apply {
				putString("intent_output_enabled", "true")
				putString("intent_action", ScanningConfig.APP_SCANNER_INTENT)
				putString("intent_delivery", "2") // Broadcast
			}
			putBundle("INTENT", intentOutput)

			// Associate this app with the profile
			val appConfig = Bundle().apply {
				putString("PACKAGE_NAME", packageName)
				putStringArray("ACTIVITY_LIST", arrayOf(""))
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