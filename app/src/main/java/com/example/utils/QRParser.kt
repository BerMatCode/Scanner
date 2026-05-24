package com.example.utils

import android.net.Uri

sealed class ParsedQR {
    data class Link(val url: String) : ParsedQR()
    data class Text(val content: String) : ParsedQR()
    data class Wifi(val ssid: String, val security: String, val pass: String) : ParsedQR()
    data class Phone(val number: String) : ParsedQR()
    data class Email(val address: String, val subject: String?, val body: String?) : ParsedQR()
    data class Whatsapp(val number: String, val message: String?) : ParsedQR()
    data class Yape(val content: String, val extraInfo: String?) : ParsedQR()
}

object QRParser {
    fun parse(raw: String): ParsedQR {
        val trimmed = raw.trim()

        // 1. WhatsApp detection
        if (trimmed.startsWith("whatsapp://", ignoreCase = true) ||
            trimmed.startsWith("https://wa.me/", ignoreCase = true) ||
            trimmed.startsWith("https://api.whatsapp.com/send", ignoreCase = true)
        ) {
            val phone = extractWhatsappPhone(trimmed)
            val msg = extractWhatsappMessage(trimmed)
            return ParsedQR.Whatsapp(phone, msg)
        }

        // 2. Yape payment detection (Yape shares typically contain yape.com.pe, qr.yape, or 'yape' inside a URL or raw text)
        if (trimmed.contains("yape.com.pe", ignoreCase = true) ||
            trimmed.contains("qr.yape", ignoreCase = true) ||
            trimmed.startsWith("yape:", ignoreCase = true)
        ) {
            return ParsedQR.Yape(trimmed, "Pago con Yape")
        }

        // 3. Wi-Fi QR Code (WIFI:S:SSID;T:WPA;P:PASSWORD;;)
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            val ssid = extractWifiParam(trimmed, "S:")
            val pass = extractWifiParam(trimmed, "P:")
            val security = extractWifiParam(trimmed, "T:")
            return ParsedQR.Wifi(ssid, security, pass)
        }

        // 4. Telephone
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            return ParsedQR.Phone(trimmed.substring(4))
        }

        // 5. Emails
        if (trimmed.startsWith("mailto:", ignoreCase = true)) {
            val cleanMail = trimmed.substring(7)
            val parts = cleanMail.split("?")
            val address = parts[0]
            var subject: String? = null
            var body: String? = null
            if (parts.size > 1) {
                val queries = parts[1].split("&")
                for (q in queries) {
                    if (q.startsWith("subject=", ignoreCase = true)) subject = Uri.decode(q.substring(8))
                    if (q.startsWith("body=", ignoreCase = true)) body = Uri.decode(q.substring(5))
                }
            }
            return ParsedQR.Email(address, subject, body)
        }

        // 6. Generic Links
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return ParsedQR.Link(trimmed)
        }

        // 7. Check if plain text looks like a phone number
        if (trimmed.matches(Regex("^[+]?[0-9\\s]{7,15}$"))) {
            return ParsedQR.Phone(trimmed)
        }

        // Fallback to text
        return ParsedQR.Text(trimmed)
    }

    private fun extractWifiParam(wifiStr: String, param: String): String {
        val startIndex = wifiStr.indexOf(param, ignoreCase = true)
        if (startIndex == -1) return ""
        val contentStart = startIndex + param.length
        
        // Find ending semicolon that is NOT escaped
        var curr = contentStart
        val len = wifiStr.length
        val sb = java.lang.StringBuilder()
        while (curr < len) {
            val char = wifiStr[curr]
            if (char == ';') {
                if (curr > 0 && wifiStr[curr - 1] == '\\') {
                    sb.deleteCharAt(sb.length - 1) // Remove escape
                    sb.append(char)
                } else {
                    break // Semicolon terminator
                }
            } else {
                sb.append(char)
            }
            curr++
        }
        return sb.toString().trim()
    }

    private fun extractWhatsappPhone(url: String): String {
        // e.g. https://wa.me/51999999999?text=Hola
        return try {
            val uri = Uri.parse(url)
            if (url.startsWith("https://wa.me/", ignoreCase = true)) {
                val path = uri.path?.removePrefix("/") ?: ""
                path.split("?")[0]
            } else if (url.startsWith("https://api.whatsapp.com/", ignoreCase = true)) {
                uri.getQueryParameter("phone") ?: ""
            } else {
                uri.schemeSpecificPart?.split("?")?.get(0) ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractWhatsappMessage(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            uri.getQueryParameter("text")
        } catch (e: Exception) {
            null
        }
    }
}
