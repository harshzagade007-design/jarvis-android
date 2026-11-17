package com.example.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val http = OkHttpClient()
    private lateinit var status: TextView
    private lateinit var input: EditText
    private lateinit var tts: TextToSpeech
    private var ttsReady = false

    // Replace with your deployed HTTPS backend URL. Keep the trailing slash off.
    private val backend = "https://YOUR-RENDER-SERVICE.onrender.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildScreen()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 10)
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 36, 28, 28)
            setBackgroundColor(android.graphics.Color.rgb(7, 17, 31))
        }
        val heading = TextView(this).apply {
            text = "J.A.R.V.I.S"
            textSize = 32f
            setTextColor(android.graphics.Color.rgb(80, 180, 255))
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "Systems online. Press the microphone and speak."
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }
        input = EditText(this).apply {
            hint = "Type a command…"
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.LTGRAY)
        }
        val speak = Button(this).apply {
            text = "🎙  SPEAK TO JARVIS"
            setOnClickListener { listenOnce() }
        }
        val send = Button(this).apply {
            text = "SEND COMMAND"
            setOnClickListener { if (input.text.isNotBlank()) sendToBrain(input.text.toString()) }
        }
        root.addView(heading)
        root.addView(status)
        root.addView(input, LinearLayout.LayoutParams(-1, -2))
        root.addView(speak)
        root.addView(send)
        setContentView(root)
    }

    private fun listenOnce() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            say("Speech recognition is not available on this phone.")
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
        }
        try { startActivityForResult(intent, 20) }
        catch (_: Exception) { say("I could not start speech recognition.") }
    }

    @Deprecated("Activity result API kept simple for beginner starter")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 20 && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                input.setText(spoken)
                sendToBrain(spoken)
            }
        }
    }

    private fun sendToBrain(command: String) {
        status.text = "JARVIS is thinking…"
        val json = JSONObject().put("text", command).toString()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url("$backend/command").post(body).build()
        http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { status.text = "Connection error: ${e.message}"; say("I cannot reach my cloud brain.") }
            }
            override fun onResponse(call: Call, response: Response) {
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    runOnUiThread { status.text = "Server error: $raw"; say("There was a server error.") }
                    return
                }
                try {
                    val result = JSONObject(raw)
                    runOnUiThread { handleResult(result) }
                } catch (_: Exception) {
                    runOnUiThread { status.text = "I received an invalid response." }
                }
            }
        })
    }

    private fun handleResult(r: JSONObject) {
        val action = r.optString("action", "answer")
        val reply = r.optString("reply", "Ready.")
        status.text = reply
        when (action) {
            "youtube" -> {
                val q = r.optString("query").ifBlank { "music" }
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(q)}")))
            }
            "dial" -> {
                val number = r.optString("phone")
                if (number.isNotBlank()) startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")))
                else say(reply)
            }
            "sms" -> {
                val number = r.optString("phone")
                val message = r.optString("message")
                if (number.isNotBlank()) {
                    startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).putExtra("sms_body", message))
                } else say(reply)
            }
            "open_app" -> openApp(r.optString("app"), reply)
            else -> say(reply)
        }
    }

    private fun openApp(name: String, fallback: String) {
        val pkg = when (name.lowercase()) {
            "youtube" -> "com.google.android.youtube"
            "whatsapp" -> "com.whatsapp"
            "chrome" -> "com.android.chrome"
            "maps" -> "com.google.android.apps.maps"
            "settings" -> "com.android.settings"
            else -> ""
        }
        val launch = if (pkg.isNotBlank()) packageManager.getLaunchIntentForPackage(pkg) else null
        if (launch != null) startActivity(launch) else say(fallback.ifBlank { "That app is not installed." })
    }

    private fun say(text: String) {
        if (ttsReady && text.isNotBlank()) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    override fun onInit(statusCode: Int) {
        ttsReady = statusCode == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts.language = Locale.US
            tts.setPitch(0.82f) // Lower pitch; actual male voice depends on installed Android TTS engine.
            tts.setSpeechRate(0.92f)
        }
    }

    override fun onDestroy() {
        tts.stop(); tts.shutdown()
        http.dispatcher.cancelAll()
        super.onDestroy()
    }
}
