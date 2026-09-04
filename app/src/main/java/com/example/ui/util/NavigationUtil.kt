package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object NavigationUtil {

    fun openInGoogleMaps(context: Context, latitude: Double, longitude: Double, label: String) {
        try {
            val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            try {
                val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                context.startActivity(browserIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "No se pudo abrir Google Maps", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sharePlace(context: Context, name: String, address: String, mapsUrl: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "¡Mira este lugar recomendado!\n$name\n$address\nVer en Google Maps: $mapsUrl")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Compartir lugar")
        context.startActivity(shareIntent)
    }
}
