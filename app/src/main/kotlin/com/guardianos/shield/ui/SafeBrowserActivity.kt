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
import com.guardianos.shield.billing.FreeTierLimits
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
    /** Si el usuario es premium (pasado por intent o DataStore) */
    private var isPremium = false

    companion object {
        private const val CHANNEL_ID = "GuardianShield_Blocked"
        private const val NOTIFICATION_ID_BASE = 2000
        
        fun createIntent(context: Context, isPremium: Boolean = false): Intent {
            return Intent(context, SafeBrowserActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("is_premium", isPremium)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = GuardianRepository(this, GuardianDatabase.getDatabase(this))
        isPremium = intent.getBooleanExtra("is_premium", false)

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

                    // Banner FREE: historial limitado
                    if (!isPremium) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "FREE plan: last ${FreeTierLimits.MAX_BROWSER_HISTORY_FREE} URLs.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.weight(1f)
                                )
                            }
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
            // Configuración compatible Android 12-15+
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                
                // Android 12+: Configuraciones adicionales de seguridad
                if (Build.VERSION.SDK_INT >= 31) {
                    // Deshabilitar autofill para evitar leaks de datos
                    try {
                        @Suppress("DEPRECATION")
                        savePassword = false
                        @Suppress("DEPRECATION")
                        saveFormData = false
                    } catch (e: Exception) {
                        Log.w("SafeBrowser", "No se pudo configurar autofill: ${e.message}")
                    }
                }
                
                // Android 13+: Mejor manejo de medios
                if (Build.VERSION.SDK_INT >= 33) {
                    mediaPlaybackRequiresUserGesture = true
                }
            }

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
                        Log.d("SafeBrowser", "🔍 Perfil actual: ${currentProfile?.name} | scheduleEnabled=${currentProfile?.scheduleEnabled} | isActive=${currentProfile?.isActive}")
                        if (currentProfile != null) {
                            val withinTime = currentProfile.isWithinAllowedTime()
                            Log.d("SafeBrowser", "⏰ Horario permitido: $withinTime | start=${currentProfile.startTimeMinutes/60}:${currentProfile.startTimeMinutes%60} | end=${currentProfile.endTimeMinutes/60}:${currentProfile.endTimeMinutes%60}")
                            if (!withinTime) {
                                showBlockedPage(url, "Fuera del horario permitido")
                                Log.i("SafeBrowser", "⏰ BLOQUEADO POR HORARIO: $domain")
                                showBlockNotification("Horario no permitido")
                                return@launch
                            }
                        } else {
                            Log.w("SafeBrowser", "⚠️ No hay perfil activo - permitiendo navegación")
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
                            // ── YouTube: forzar Restricted Mode ───────────────────────────
                            val safeUrl = applySafeSearchParams(url, domain)
                            withContext(Dispatchers.Main) {
                                view?.loadUrl(safeUrl)
                            }
                            addToHistory(safeUrl)
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
            Log.d("SafeBrowser", "🔍 loadUrlSafely - Perfil: ${currentProfile?.name} | scheduleEnabled=${currentProfile?.scheduleEnabled}")
            if (currentProfile != null) {
                val withinTime = currentProfile.isWithinAllowedTime()
                Log.d("SafeBrowser", "⏰ loadUrlSafely - Horario: $withinTime | start=${currentProfile.startTimeMinutes/60}:${currentProfile.startTimeMinutes%60} | end=${currentProfile.endTimeMinutes/60}:${currentProfile.endTimeMinutes%60}")
                if (!withinTime) {
                    showBlockedPage(finalUrl, "Fuera del horario permitido")
                    Log.i("SafeBrowser", "⏰ BLOQUEADO POR HORARIO: $domain")
                    showBlockNotification("Horario no permitido")
                    return@launch
                }
            } else {
                Log.w("SafeBrowser", "⚠️ loadUrlSafely - No hay perfil activo")
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

    /**
     * Aplica parámetros de SafeSearch/RestrictedMode según el dominio:
     *  • YouTube  → añade restrict=strict  (Modo Restringido de YouTube)
     *  • Google   → añade safe=strict      (SafeSearch estricto de Google)
     *
     * El Modo Restringido de YouTube oculta vídeos marcados como no aptos para menores.
     * No es 100% perfecto, pero bloquea la gran mayoría de contenido inapropiado.
     * Para filtrado completo, usar la app YouTube Kids desde el bloqueo de apps.
     */
    private fun applySafeSearchParams(url: String, domain: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            val builder = uri.buildUpon()

            when {
                domain.contains("youtube.com") || domain == "youtu.be" -> {
                    // Restricted Mode: oculta contenido no apto para menores
                    if (uri.getQueryParameter("restrict") != "strict") {
                        builder.appendQueryParameter("restrict", "strict")
                    }
                    val safeUrl = builder.build().toString()
                    Log.d("SafeBrowser", "🎬 YouTube RestrictedMode aplicado: $safeUrl")
                    safeUrl
                }
                domain.contains("google.") && uri.path?.contains("/search") == true -> {
                    // SafeSearch estricto en Google
                    if (uri.getQueryParameter("safe") != "strict") {
                        builder.appendQueryParameter("safe", "strict")
                    }
                    val safeUrl = builder.build().toString()
                    Log.d("SafeBrowser", "🔍 Google SafeSearch aplicado: $safeUrl")
                    safeUrl
                }
                domain.contains("bing.com") && uri.path?.contains("/search") == true -> {
                    // SafeSearch estricto en Bing
                    if (uri.getQueryParameter("safeSearch") != "Strict") {
                        builder.appendQueryParameter("safeSearch", "Strict")
                    }
                    val safeUrl = builder.build().toString()
                    Log.d("SafeBrowser", "🔍 Bing SafeSearch aplicado: $safeUrl")
                    safeUrl
                }
                else -> url
            }
        } catch (e: Exception) {
            url // Si falla el parsing, devolver la URL original sin modificar
        }
    }

    private fun addToHistory(url: String) {
        if (!browsingHistory.contains(url)) {
            browsingHistory.add(0, url)
            val max = FreeTierLimits.maxBrowserHistory(isPremium)
            if (browsingHistory.size > max) {
                browsingHistory.removeLast()
            }
        }
    }

    private fun showBlockedPage(url: String, reason: String) {
        val domain = extractDomain(url)
        val icon = if (reason.contains("horario", ignoreCase = true)) "⏰" else "🛡️"

        // Detectar categoría para contenido educativo
        val categoria = when {
            reason.contains("horario", ignoreCase = true) -> "HORARIO"
            domain.contains("facebook") || domain.contains("instagram") ||
            domain.contains("tiktok") || domain.contains("twitter") ||
            domain.contains("snapchat") || domain.contains("discord") -> "RRSS"
            domain.contains("porn") || domain.contains("xxx") || domain.contains("adult") ||
            domain.contains("sex") || domain.contains("erotic") -> "ADULTO"
            domain.contains("casino") || domain.contains("bet") ||
            domain.contains("gambling") || domain.contains("poker") -> "APUESTAS"
            else -> "GENERAL"
        }

        val (tituloEducativo, explicacion, alternativas) = contenidoEducativo(categoria)

        val html = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: -apple-system, 'Segoe UI', Arial, sans-serif;
                        background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
                        color: white;
                        min-height: 100vh;
                        padding: 16px;
                    }
                    .container {
                        max-width: 540px;
                        margin: 24px auto;
                    }
                    .shield-icon {
                        font-size: 64px;
                        text-align: center;
                        margin-bottom: 12px;
                    }
                    h1 {
                        text-align: center;
                        font-size: 22px;
                        margin-bottom: 4px;
                    }
                    .url-badge {
                        background: rgba(255,255,255,0.12);
                        border-radius: 8px;
                        padding: 8px 14px;
                        font-family: monospace;
                        font-size: 13px;
                        word-break: break-all;
                        text-align: center;
                        margin: 12px 0;
                        opacity: 0.85;
                    }
                    .card {
                        background: rgba(255,255,255,0.07);
                        border-radius: 14px;
                        padding: 18px;
                        margin: 12px 0;
                        border-left: 4px solid rgba(255,255,255,0.3);
                    }
                    .card.educativo { border-left-color: #42a5f5; }
                    .card.alternativas { border-left-color: #66bb6a; }
                    .card-title {
                        font-size: 13px;
                        font-weight: 700;
                        text-transform: uppercase;
                        letter-spacing: 0.8px;
                        opacity: 0.65;
                        margin-bottom: 8px;
                    }
                    .card-title.blue { color: #90caf9; }
                    .card-title.green { color: #a5d6a7; }
                    p { font-size: 14px; line-height: 1.6; opacity: 0.9; }
                    ul { padding-left: 18px; }
                    li { font-size: 14px; line-height: 1.8; opacity: 0.85; }
                    .footer {
                        text-align: center;
                        font-size: 11px;
                        opacity: 0.35;
                        margin-top: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="shield-icon">$icon</div>
                    <h1>Sitio bloqueado</h1>
                    <div class="url-badge">$url</div>

                    <div class="card educativo">
                        <div class="card-title blue">📚 $tituloEducativo</div>
                        <p>$explicacion</p>
                    </div>

                    <div class="card alternativas">
                        <div class="card-title green">✅ ¿Qué puedo hacer?</div>
                        <ul>$alternativas</ul>
                    </div>

                    <div class="footer">GuardianOS Shield — protección 100% local, sin envío de datos</div>
                </div>
            </body>
            </html>
        """.trimIndent()

        runOnUiThread {
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }

    /**
     * Devuelve contenido educativo contextualizado por categoría.
     * Triple: (título, explicación, alternativas en HTML <li>)
     */
    private fun contenidoEducativo(categoria: String): Triple<String, String, String> = when (categoria) {
        "RRSS" -> Triple(
            "¿Por qué se bloquean las redes sociales?",
            "Las redes sociales están diseñadas para mantenerte conectado el mayor tiempo posible. " +
            "Los algoritmos muestran contenido que genera reacciones fuertes, lo que puede afectar " +
            "el estado de ánimo, el sueño y la concentración, sobre todo en menores de edad.",
            "<li>Habla con tus amigos en persona o por llamada</li>" +
            "<li>Escríbeles un mensaje de texto o WhatsApp (si está permitido)</li>" +
            "<li>Haz una actividad offline: deporte, lectura, dibujo</li>" +
            "<li>Si necesitas las redes para algo concreto, pide permiso a tu padre/madre</li>"
        )
        "ADULTO" -> Triple(
            "Contenido no apropiado para tu edad",
            "Este sitio contiene contenido para adultos que no es adecuado para menores. " +
            "Ver este tipo de contenido a edades tempranas puede generar expectativas poco " +
            "realistas y afectar negativamente al desarrollo emocional y relacional.",
            "<li>Si tienes dudas sobre sexualidad, habla con un adulto de confianza</li>" +
            "<li>Consulta recursos educativos de sanidad o tu médico de familia</li>" +
            "<li>En clase de educación física o tutoría también puedes resolver dudas</li>"
        )
        "APUESTAS" -> Triple(
            "Las apuestas online son un riesgo real",
            "Los sitios de apuestas y casinos online están diseñados para que pierdas dinero. " +
            "La adicción al juego puede desarrollarse a cualquier edad y afecta gravemente " +
            "a las finanzas, las relaciones y la salud mental.",
            "<li>Si sientes curiosidad por el riesgo, prueba videojuegos de estrategia</li>" +
            "<li>Habla con tu familia si ves publicidad de apuestas y te atrae</li>" +
            "<li>Los juegos de azar están prohibidos para menores por ley en España</li>"
        )
        "HORARIO" -> Triple(
            "Fuera del horario de uso",
            "Tu padre o madre ha establecido un horario de uso del dispositivo. " +
            "El descanso digital es tan importante como el descanso físico: " +
            "la pantalla antes de dormir afecta al sueño y a la concentración al día siguiente.",
            "<li>Aprovecha para leer un libro o revista</li>" +
            "<li>Sal a dar un paseo o practica deporte</li>" +
            "<li>Habla con tu familia sobre el horario si crees que debería ajustarse</li>" +
            "<li>Usa la función \"Pedir permiso\" de GuardianOS si necesitas una excepción</li>"
        )
        else -> Triple(
            "Sitio no permitido",
            "Este sitio ha sido bloqueado por la configuración de control parental activa " +
            "en este dispositivo. Si crees que el bloqueo es un error, puedes pedirle " +
            "a tu padre o madre que revise la configuración.",
            "<li>Comprueba si hay una alternativa educativa al contenido que buscabas</li>" +
            "<li>Pide a tu padre/madre que revise los filtros si crees que es un error</li>" +
            "<li>Prueba con el buscador de <a href='https://www.google.es' style='color:#90caf9'>Google</a> para encontrar fuentes seguras</li>"
        )
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
                .bigText("Access to the following has been blocked:\n$domain\n\nCategory: $category"))
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
