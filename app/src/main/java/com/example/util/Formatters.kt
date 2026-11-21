package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.R
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object Formatters {
    private const val XAF_PER_USD = 605.0

    fun formatPrice(amountXaf: Long, currencyCode: String): String {
        return if (currencyCode == "USD") {
            val usd = amountXaf / XAF_PER_USD
            val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
                maximumFractionDigits = 0
            }
            formatter.format(usd)
        } else {
            val formatted = NumberFormat.getNumberInstance(Locale.US).format(amountXaf)
            "$formatted FCFA"
        }
    }

    fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun resolveDrawableRes(imageUri: String): Int? {
        return when (imageUri) {
            "drawable://img_land_estate" -> R.drawable.img_land_estate
            "drawable://img_products_showcase" -> R.drawable.img_products_showcase
            "drawable://img_hero_gideon" -> R.drawable.img_hero_gideon
            "drawable://img_verified_seal" -> R.drawable.img_verified_seal
            else -> null
        }
    }

    fun copyPickedImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "listing_images").apply { mkdirs() }
            val destFile = File(dir, "listing_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            Uri.fromFile(destFile).toString()
        } catch (_: Exception) {
            null
        }
    }

    fun dialPhone(context: Context, phone: String) {
        val cleaned = phone.replace(" ", "")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned"))
        runCatching { context.startActivity(intent) }.onFailure {
            Toast.makeText(context, "Phone dialer unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmail(context: Context, email: String, subject: String, body: String = "") {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            if (body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, body)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            Toast.makeText(context, "Email app unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
}
