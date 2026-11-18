package com.example

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    companion object {
        const val CHANNEL_ID = "mudhakirati_alerts_v2"
        const val OLD_CHANNEL_ID = "mudhakirati_notifications"

        /** true while the activity is visible (the page handles its own alerts then) */
        @Volatile
        var inForeground = false
    }

    private var webView: WebView? = null
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uriResult = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        fileChooserCallback?.onReceiveValue(uriResult)
        fileChooserCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extend into display cutout area (camera notch)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Hide status bar (WiFi, battery, clock) and navigation bar for full immersion
        hideSystemBars()

        // Same colour as the page background, so there is no white flash while the page loads
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val bg = if (night) 0xFF0A0E1C.toInt() else 0xFFF1F3F9.toInt()
        window.setBackgroundDrawable(ColorDrawable(bg))

        // Plain WebView as the content view (no Compose layer in between = faster start, less work per frame)
        val wv = buildWebView(bg)
        webView = wv
        setContentView(wv)

        onBackPressedDispatcher.addCallback(this, backCallback)

        // Everything that is not needed for the first frame runs after it
        window.decorView.post {
            createNotificationChannel()
            requestNotificationPermission()
            FcmService.fetchToken(applicationContext)
        }
    }

    private var lastBackAt = 0L

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val wv = webView
            if (wv == null) { exitApp(); return }
            // Ask the page first: it closes dialogs/sheets or returns to the home tab
            wv.evaluateJavascript(
                "(function(){try{return window.__appBack?window.__appBack():false}catch(e){return false}})()"
            ) { handled ->
                if (handled == "true") return@evaluateJavascript
                // Home tab and nothing open: press back twice to exit
                val now = System.currentTimeMillis()
                if (now - lastBackAt < 2000) {
                    exitApp()
                } else {
                    lastBackAt = now
                    Toast.makeText(this@MainActivity, "اسحب مرة ثانية للخروج", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun exitApp() {
        backCallback.isEnabled = false
        onBackPressedDispatcher.onBackPressed()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildWebView(bgColor: Int): WebView {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        return WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(bgColor)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isLongClickable = false
            isHapticFeedbackEnabled = false
            setOnLongClickListener { true }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mediaPlaybackRequiresUserGesture = false
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                setGeolocationEnabled(false)
                // Safe-Browsing does a lookup on every navigation; the page is local, so skip it
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = false
            }

            addJavascriptInterface(WebAppInterface(this@MainActivity, this), "AndroidBridge")

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val url = request.url.toString()
                    if (url.startsWith("https://appassets.androidplatform.net/") ||
                        url.startsWith("blob:") || url.startsWith("data:")
                    ) {
                        return false
                    }
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        true
                    } catch (e: Exception) {
                        false
                    }
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    // Hook bridge for printing, vibration, notifications, and file downloads
                    view.evaluateJavascript(BRIDGE_JS, null)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = filePathCallback
                    val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                    }
                    try {
                        fileChooserLauncher.launch(intent)
                    } catch (e: Exception) {
                        fileChooserCallback?.onReceiveValue(null)
                        fileChooserCallback = null
                    }
                    return true
                }
            }

            loadUrl("https://appassets.androidplatform.net/assets/index.html")
        }
    }

    override fun onResume() {
        super.onResume()
        inForeground = true
    }

    override fun onPause() {
        inForeground = false
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun createNotificationChannel() {
        Notifier.ensureChannel(this)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    fun showAndroidNotification(title: String, body: String, tag: String?) {
        runOnUiThread {
            try {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )

                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
                    .setSound(Notifier.soundUri(this))
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                val notificationManager = NotificationManagerCompat.from(this)
                val notificationId = tag?.hashCode() ?: System.currentTimeMillis().toInt()
                notificationManager.notify(notificationId, notification)
            } catch (e: SecurityException) {
                // Permission not granted yet
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    inner class WebAppInterface(private val context: Context, private val wv: WebView) {

        @JavascriptInterface
        fun print() {
            runOnUiThread {
                val printManager = getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = wv.createPrintDocumentAdapter("مذاكرتي_${System.currentTimeMillis()}")
                printManager?.print("مذاكرتي", printAdapter, PrintAttributes.Builder().build())
            }
        }

        @JavascriptInterface
        fun share(text: String, title: String?) {
            runOnUiThread {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                    if (!title.isNullOrEmpty()) {
                        putExtra(Intent.EXTRA_SUBJECT, title)
                    }
                }
                context.startActivity(Intent.createChooser(shareIntent, title ?: "مشاركة"))
            }
        }

        @JavascriptInterface
        fun saveFile(fileName: String, mimeType: String, base64Data: String) {
            runOnUiThread {
                try {
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        val resolver = context.contentResolver
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { os ->
                                os.write(bytes)
                            }
                            Toast.makeText(context, "تم حفظ الملف: $fileName", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val file = File(downloadsDir, fileName)
                        FileOutputStream(file).use { os ->
                            os.write(bytes)
                        }
                        Toast.makeText(context, "تم حفظ الملف: $fileName", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /** FCM device token ("" until Firebase has issued one). */
        @JavascriptInterface
        fun getFcmToken(): String = FcmService.savedToken(context.applicationContext)

        @JavascriptInterface
        fun showNotification(title: String, body: String, tag: String?) {
            showAndroidNotification(title, body, tag)
        }

        @JavascriptInterface
        fun scheduleTimer(endAtMs: Long, title: String, body: String) {
            AlarmScheduler.scheduleTimer(context.applicationContext, endAtMs, title, body)
        }

        @JavascriptInterface
        fun setBreakChain(ms: Long, title: String, body: String) {
            AlarmScheduler.setBreakChain(context.applicationContext, ms, title, body)
        }

        @JavascriptInterface
        fun scheduleNewDay(title: String, body: String) {
            AlarmScheduler.scheduleNewDay(context.applicationContext, title, body)
        }

        @JavascriptInterface
        fun scheduleStreakWarn(atMs: Long, title: String, body: String, body2: String) {
            AlarmScheduler.scheduleStreakWarn(context.applicationContext, atMs, title, body, body2)
        }

        @JavascriptInterface
        fun cancelStreakWarn() {
            AlarmScheduler.cancelStreakWarn(context.applicationContext)
        }

        @JavascriptInterface
        fun cancelNewDay() {
            AlarmScheduler.cancelNewDay(context.applicationContext)
        }

        @JavascriptInterface
        fun cancelTimer() {
            AlarmScheduler.cancelTimer(context.applicationContext)
        }

        @JavascriptInterface
        fun scheduleDaily(hour: Int, minute: Int, title: String, body: String) {
            AlarmScheduler.scheduleDaily(context.applicationContext, hour, minute, title, body)
        }

        @JavascriptInterface
        fun cancelDaily() {
            AlarmScheduler.cancelDaily(context.applicationContext)
        }

        @JavascriptInterface
        fun vibrate(durationMs: Long) {
            val vibrator = getVibratorService()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        }

        @JavascriptInterface
        fun vibratePattern(patternJson: String) {
            try {
                val jsonArr = JSONArray(patternJson)
                val pattern = LongArray(jsonArr.length())
                for (i in 0 until jsonArr.length()) {
                    pattern[i] = jsonArr.getLong(i)
                }
                val vibrator = getVibratorService()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun getVibratorService(): Vibrator? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
    }
}

private const val BRIDGE_JS = """
(function() {
    if (window.AndroidBridge) {
        if (window.AndroidBridge.print) {
            window.print = function() { window.AndroidBridge.print(); };
        }
        if (window.AndroidBridge.vibrate) {
            navigator.vibrate = function(pattern) {
                if (Array.isArray(pattern)) {
                    window.AndroidBridge.vibratePattern(JSON.stringify(pattern));
                } else if (typeof pattern === 'number') {
                    window.AndroidBridge.vibrate(pattern);
                }
                return true;
            };
        }
        document.addEventListener('click', function(e) {
            var a = e.target.closest && e.target.closest('a[download]');
            if (a && a.href && a.href.indexOf('blob:') === 0 && window.AndroidBridge.saveFile) {
                var filename = a.getAttribute('download') || 'download';
                fetch(a.href).then(function(r) { return r.blob(); }).then(function(b) {
                    var reader = new FileReader();
                    reader.onloadend = function() {
                        var b64 = reader.result.split(',')[1];
                        window.AndroidBridge.saveFile(filename, b.type || 'application/octet-stream', b64);
                    };
                    reader.readAsDataURL(b);
                }).catch(function(err){});
            }
        }, true);
    }
})();
"""
