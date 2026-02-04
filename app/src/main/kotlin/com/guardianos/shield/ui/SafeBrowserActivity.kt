package com.guardianos.shield.ui

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.guardianos.shield.R
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository
import com.guardianos.shield.ui.theme.GuardianShieldTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class SafeBrowserActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var repository: GuardianRepository
    private var isLoading by mutableStateOf(false)
    private var canGoBack by mutableStateOf(false)
    private var canGoForward by mutableStateOf(false)
    private var currentUrl by mutableStateOf("https://www.google.com")
    private var searchQuery by mutableStateOf("")
    private var showHistory by mutableStateOf(false)
    private var browsingHistory = mutableStateListOf<String>()

    companion object {
        private const val CHANNEL_ID = "GuardianShield_Blocked"
        private const val NOTIFICATION_ID_BASE = 2000
        
        fun createIntent(context: Context): Intent {
            return Intent(context, SafeBrowserActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = GuardianRepository(this, GuardianDatabase.getDatabase(this))

        intent.getStringExtra("redirected_from")?.let { sourceApp ->
            Log.d("SafeBrowser", "Redirigido desde: $sourceApp")
        }

        setContent {
            GuardianShieldTheme {
                SimpleSafeBrowserScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SimpleSafeBrowserScreen() {
        Scaffold(
            topBar = {
                SimpleBrowserTopBar()
            },
            bottomBar = {
                SimpleBrowserBottomBar()
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webView = this
                            setupWebView()
                            loadUrl(currentUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (showHistory) {
                    SimpleHistoryPanel()
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun SimpleBrowserTopBar() {
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar o ingresar URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()) {
                                val url = if (searchQuery.startsWith("http")) {
                                    searchQuery
                                } else {
                                    "https://www.google.com/search?q=${Uri.encode(searchQuery)}"
                                }
                                loadUrlSafely(url)
                                searchQuery = ""
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            navigationIcon = {
                IconButton(onClick = { if (canGoBack) webView.goBack() else finish() }) {
                    Icon(Icons.Rounded.ArrowBack, "Atrás")
                }
            }
        )
    }

    @Composable
    private fun SimpleBrowserBottomBar() {
        NavigationBar {
            NavigationBarItem(
                selected = false,
                onClick = { if (canGoBack) webView.goBack() },
                icon = { Icon(Icons.Rounded.ArrowBack, null) },
                enabled = canGoBack
            )
            NavigationBarItem(
                selected = false,
                onClick = { if (canGoForward) webView.goForward() },
                icon = { Icon(Icons.Rounded.ArrowForward, null) },
                enabled = canGoForward
            )
            NavigationBarItem(
                selected = false,
                onClick = { webView.reload() },
                icon = { Icon(Icons.Rounded.Refresh, null) }
            )
            NavigationBarItem(
                selected = false,
                onClick = { loadUrlSafely("https://www.google.com") },
                icon = { Icon(Icons.Rounded.Home, null) }
            )
            NavigationBarItem(
                selected = showHistory,
                onClick = { showHistory = !showHistory },
                icon = { Icon(Icons.Rounded.History, null) }
            )
        }
    }

    @Composable
    private fun SimpleHistoryPanel() {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable { showHistory = false },
            color = Color.Black.copy(alpha = 0.7f)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Historial",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showHistory = false }) {
                            Icon(Icons.Rounded.Close, "Cerrar")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    LazyColumn {
                        items(browsingHistory) { url ->
                            ListItem(
                                headlineContent = { Text(url) },
                                leadingContent = { Icon(Icons.Rounded.Language, null) },
                                modifier = Modifier.clickable {
                                    loadUrlSafely(url)
                                    showHistory = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    val domain = extractDomain(url)

                    lifecycleScope.launch {
                        // VERIFICAR HORARIO PERMITIDO
                        val currentProfile = repository.getActiveProfile()
                        if (currentProfile != null && !currentProfile.isWithinAllowedTime()) {
                            showBlockedPage(url, "Fuera del horario permitido")
                            Log.i("SafeBrowser", "⏰ BLOQUEADO POR HORARIO: $domain")
                            showBlockNotification("Horario no permitido")
                            return@launch
                        }
                        
                        val isBlocked = isDomainBlocked(domain)
                        if (isBlocked) {
                            showBlockedPage(url, "Sitio restringido por control parental")
                            repository.addBlockedSite(
                                domain = domain,
                                category = "user_attempt",
                                threatLevel = 1
                            )
                            // 🔔 NOTIFICACIÓN DE BLOQUEO
                            showBlockNotification(domain)
                        } else {
                            withContext(Dispatchers.Main) {
                                view?.loadUrl(url)
                            }
                            addToHistory(url)
                        }
                    }
                    return true
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    isLoading = true
                    currentUrl = url ?: ""
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    isLoading = false
                    canGoBack = view?.canGoBack() ?: false
                    canGoForward = view?.canGoForward() ?: false
                }
            }

            webChromeClient = WebChromeClient()
        }
    }

    private fun loadUrlSafely(url: String) {
        lifecycleScope.launch {
            val finalUrl = if (url.startsWith("http")) url else "https://$url"
            val domain = extractDomain(finalUrl)

            if (domain.isEmpty()) {
                return@launch
            }
            
            // VERIFICAR HORARIO PERMITIDO
            val currentProfile = repository.getActiveProfile()
            if (currentProfile != null && !currentProfile.isWithinAllowedTime()) {
                showBlockedPage(finalUrl, "Fuera del horario permitido")
                Log.i("SafeBrowser", "⏰ BLOQUEADO POR HORARIO: $domain")
                showBlockNotification("Horario no permitido")
                return@launch
            }

            if (isDomainBlocked(domain)) {
                showBlockedPage(finalUrl, "Sitio bloqueado")
                repository.addBlockedSite(
                    domain = domain,
                    category = "blocked",
                    threatLevel = 2
                )
            } else {
                withContext(Dispatchers.Main) {
                    webView.loadUrl(finalUrl)
                    addToHistory(finalUrl)
                }
            }
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            URL(url).host.lowercase().removePrefix("www.")
        } catch (e: Exception) {
            ""
        }
    }

    private fun addToHistory(url: String) {
        if (!browsingHistory.contains(url)) {
            browsingHistory.add(0, url)
            if (browsingHistory.size > 20) {
                browsingHistory.removeLast()
            }
        }
    }

    private fun showBlockedPage(url: String, reason: String) {
        val icon = if (reason.contains("horario", ignoreCase = true)) "⏰" else "🛡️"
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        text-align: center;
                        padding: 20px;
                        margin: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 50px auto;
                        background: rgba(255,255,255,0.1);
                        backdrop-filter: blur(10px);
                        border-radius: 20px;
                        padding: 40px;
                        box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37);
                    }
                    .shield-icon {
                        font-size: 80px;
                        margin-bottom: 20px;
                    }
                    h1 {
                        margin: 20px 0;
                        font-size: 28px;
                    }
                    p {
                        font-size: 16px;
                        opacity: 0.9;
                        line-height: 1.6;
                    }
                    .url {
                        background: rgba(0,0,0,0.2);
                        padding: 15px;
                        border-radius: 10px;
                        margin: 20px 0;
                        word-break: break-all;
                        font-family: monospace;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="shield-icon">$icon</div>
                    <h1>Sitio Bloqueado</h1>
                    <p>$reason</p>
                    <div class="url">$url</div>
                    <p>Este sitio ha sido bloqueado por GuardianOS Shield para tu protección.</p>
                </div>
            </body>
            </html>
        """.trimIndent()

        runOnUiThread {
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }

    private suspend fun isDomainBlocked(domain: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val adultKeywords = listOf("porn", "xxx", "adult", "sex", "nude", "camgirl", "xvideos", "pornhub")
            val gamblingKeywords = listOf("casino", "poker", "betting", "gamble", "lottery", "bet365")
            
            // BLOQUEO DE REDES SOCIALES (FORZADO)
            val socialMediaDomains = setOf(
                "facebook.com", "www.facebook.com", "m.facebook.com", "fb.com", "fb.me",
                "instagram.com", "www.instagram.com", "m.instagram.com",
                "tiktok.com", "www.tiktok.com", "m.tiktok.com",
                "twitter.com", "www.twitter.com", "m.twitter.com", "x.com",
                "discord.com", "www.discord.com", "discordapp.com",
                "snapchat.com", "www.snapchat.com",
                "reddit.com", "www.reddit.com",
                "whatsapp.com", "web.whatsapp.com",
                "telegram.org", "web.telegram.org"
            )
            
            if (socialMediaDomains.any { domain.equals(it, ignoreCase = true) || domain.endsWith(".$it") }) {
                Log.i("SafeBrowser", "🚫 RED SOCIAL BLOQUEADA: $domain")
                return@withContext true
            }
            
            if (adultKeywords.any { domain.contains(it, ignoreCase = true) }) {
                return@withContext true
            }
            
            if (gamblingKeywords.any { domain.contains(it, ignoreCase = true) }) {
                return@withContext true
            }
            
            // Verificar filtros personalizados (BLACKLIST = isActive)
            val customFilters = repository.getAllCustomFilters()
            if (customFilters.any { filter -> 
                filter.isActive && (
                    domain.equals(filter.domain, ignoreCase = true) ||
                    domain.endsWith(".${filter.domain}") ||
                    domain.contains(filter.pattern.ifEmpty { filter.domain }, ignoreCase = true)
                )
            }) {
                Log.i("SafeBrowser", "🚫 DOMINIO BLOQUEADO (filtro personalizado): $domain")
                return@withContext true
            }
            
            false
        } catch (e: Exception) {
            Log.e("SafeBrowser", "Error checking blocked domain", e)
            false
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
    
    // 🔔 SISTEMA DE NOTIFICACIONES
    private fun showBlockNotification(domain: String) {
        createNotificationChannel()
        
        val category = when {
            domain.contains("facebook") || domain.contains("instagram") || 
            domain.contains("tiktok") || domain.contains("twitter") || 
            domain.contains("discord") -> "Red Social"
            domain.contains("porn") || domain.contains("xxx") || 
            domain.contains("adult") || domain.contains("sex") -> "Contenido Adulto"
            domain.contains("casino") || domain.contains("bet") -> "Apuestas"
            else -> "Sitio Restringido"
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("🚫 Sitio bloqueado")
            .setContentText("$category: $domain")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Se ha bloqueado el acceso a:\n$domain\n\nCategoría: $category"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID_BASE + domain.hashCode(), notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sitios Bloqueados",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando se bloquea un sitio web"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
