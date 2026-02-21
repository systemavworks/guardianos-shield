#!/usr/bin/env python3
"""
Genera 8 capturas de pantalla mockup para Google Play Store — v1.1.0
Formato: 1080×1920 px (phone), sin necesitar screenshots reales.
Cada imagen simula una pantalla de la app con UI de Jetpack Compose / Material 3.
"""

from PIL import Image, ImageDraw, ImageFont
import os, math

BASE        = os.path.dirname(os.path.abspath(__file__))
ASSETS_DIR  = os.path.join(BASE, "play_store_assets")
OUT_PHONE   = os.path.join(ASSETS_DIR, "screenshots", "phone")
ICON_PATH   = os.path.join(ASSETS_DIR, "icon_512x512.png")

os.makedirs(OUT_PHONE, exist_ok=True)

# ── Paleta de colores (GuardianOS Shield dark theme) ─────────────────────────
BG_DARK   = (8,  18,  45)      # fondo principal azul profundo
BG_CARD   = (13, 27,  64)      # tarjetas
BG_CARD2  = (18, 38,  85)      # tarjetas elevadas
ACCENT    = (0,  188, 212)     # cyan
GREEN     = (0,  200, 100)     # éxito
RED       = (220, 68,  68)     # alerta
ORANGE    = (255, 165,  50)    # advertencia
YELLOW    = (248, 196,   0)    # caution
WHITE     = (255, 255, 255)
GREY_L    = (180, 200, 230)
GREY_M    = (100, 130, 170)
BLUE_L    = (70,  130, 200)

W, H = 1080, 1920

FONT_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_N = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

def fnt(size, bold=False):
    try:
        return ImageFont.truetype(FONT_B if bold else FONT_N, size)
    except:
        return ImageFont.load_default()

def gradient_bg(w, h, top=(8,18,45), bot=(4,10,30)):
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / (h - 1)
        c = tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3))
        d.line([(0, y), (w, y)], fill=c)
    return img

def rounded_rect(draw, xy, radius, fill, outline=None, outline_w=2):
    x0, y0, x1, y1 = xy
    draw.rounded_rectangle([x0, y0, x1, y1], radius=radius, fill=fill,
                            outline=outline, width=outline_w)

def status_bar(draw, y=0):
    """Barra de estado simulada."""
    draw.text((60, y+32), "9:41", font=fnt(36, True), fill=WHITE, anchor="lm")
    # batería
    bx, by = W - 80, y+22
    rounded_rect(draw, [bx, by, bx+55, by+26], 5, fill=None, outline=GREY_L, outline_w=2)
    draw.rectangle([bx+2, by+2, bx+44, by+24], fill=GREEN)
    draw.rectangle([bx+55, by+8, bx+63, by+18], fill=GREY_L)
    # señal
    for i, h2 in enumerate([8, 14, 20, 26]):
        sx = W - 165 + i*16
        draw.rectangle([sx, by+26-h2, sx+10, by+26], fill=WHITE)

def top_bar(canvas, draw, title, subtitle=None, show_back=True):
    """Barra superior de la app."""
    bar_h = 160
    draw.rectangle([0, 80, W, 80 + bar_h], fill=BG_CARD2)
    y_center = 80 + bar_h // 2
    if show_back:
        # flecha atrás
        pts = [(60, y_center), (85, y_center-20), (85, y_center+20)]
        draw.polygon(pts, fill=ACCENT)
    draw.text((120 if show_back else 60, y_center - (14 if subtitle else 0)),
              title, font=fnt(52, True), fill=WHITE, anchor="lm")
    if subtitle:
        draw.text((120 if show_back else 60, y_center + 30),
                  subtitle, font=fnt(32), fill=GREY_M, anchor="lm")
    return 80 + bar_h + 20

def card(draw, x, y, w, h, title=None, title_color=ACCENT, radius=24, fill=BG_CARD,
         outline=None, outline_w=2):
    rounded_rect(draw, [x, y, x+w, y+h], radius, fill=fill,
                 outline=outline, outline_w=outline_w)
    ty = y + 44
    if title:
        draw.text((x + 32, ty), title, font=fnt(36, True), fill=title_color, anchor="lm")
    return y + h + 20

def progress_bar(draw, x, y, w, h, pct, fill_color, bg_color=(25,45,90)):
    rounded_rect(draw, [x, y, x+w, y+h], h//2, bg_color)
    fw = max(h, int(w * pct))
    rounded_rect(draw, [x, y, x+fw, y+h], h//2, fill_color)

def badge(draw, x, y, text, color):
    tw = fnt(30).getbbox(text)[2] + 40
    rounded_rect(draw, [x, y, x+tw, y+44], 22, color)
    draw.text((x + tw//2, y+22), text, font=fnt(30, True), fill=WHITE, anchor="mm")
    return x + tw + 16

# ─────────────────────────────────────────────────────────────────────────────
# PANTALLA 1: Dashboard principal (modo Recomendado activo)
# ─────────────────────────────────────────────────────────────────────────────
def screen_dashboard():
    img = gradient_bg(W, H)
    d = ImageDraw.Draw(img)
    status_bar(d)

    # Logo + nombre
    try:
        icon = Image.open(ICON_PATH).convert("RGBA").resize((120, 120))
        img.paste(icon, (W//2-60, 110), icon)
    except: pass
    d.text((W//2, 250), "GuardianOS Shield", font=fnt(54, True), fill=WHITE, anchor="mm")
    d.text((W//2, 310), "Control Parental Avanzado", font=fnt(34), fill=GREY_L, anchor="mm")

    # Estado VPN
    rounded_rect(d, [60, 360, W-60, 540], 28, fill=BG_CARD,
                 outline=GREEN, outline_w=3)
    d.ellipse([90, 395, 140, 445], fill=GREEN)
    d.text((170, 408), "Protección ACTIVA", font=fnt(44, True), fill=GREEN, anchor="lm")
    d.text((170, 462), "VPN DNS • CleanBrowsing Adult Filter", font=fnt(30), fill=GREY_L, anchor="lm")

    # Modos de protección
    y = 580
    d.text((60, y), "Modo de Protección", font=fnt(38, True), fill=GREY_L, anchor="lm")
    y += 60

    modes = [("🛡️ Recomendado", GREEN, True), ("🔒 Avanzado", BLUE_L, False), ("⚙️ Manual", GREY_M, False)]
    mx = 60
    for label, col, selected in modes:
        bw = (W - 120 - 30) // 3
        fc = col if selected else BG_CARD2
        outline_c = col if selected else None
        rounded_rect(d, [mx, y, mx+bw, y+90], 20, fill=fc,
                     outline=outline_c, outline_w=2)
        d.text((mx+bw//2, y+45), label, font=fnt(28, bold=selected), fill=WHITE, anchor="mm")
        mx += bw + 15
    y += 115

    # Stats rápidas
    stats = [("247", "Bloqueados hoy", RED), ("98%", "Uptime VPN", GREEN), ("3", "Perfiles", ACCENT)]
    sx = 60
    for val, lbl, col in stats:
        sw = (W - 120 - 40) // 3
        rounded_rect(d, [sx, y, sx+sw, y+150], 20, BG_CARD)
        d.text((sx+sw//2, y+55), val, font=fnt(60, True), fill=col, anchor="mm")
        d.text((sx+sw//2, y+110), lbl, font=fnt(26), fill=GREY_M, anchor="mm")
        sx += sw + 20
    y += 180

    # DNS Info
    rounded_rect(d, [60, y, W-60, y+130], 20, BG_CARD2)
    d.text((100, y+30), "🌐  DNS Primario:", font=fnt(32), fill=GREY_L, anchor="lm")
    d.text((100, y+78), "185.228.168.168  (CleanBrowsing Adult)", font=fnt(32, True), fill=ACCENT, anchor="lm")
    y += 160

    # Bottom nav
    nav_y = H - 130
    rounded_rect(d, [0, nav_y, W, H], 0, BG_CARD2)
    nav_items = [("🏠", "Inicio", True), ("👤", "Perfiles", False),
                 ("📊", "Stats", False), ("🤝", "Pacto", False), ("⚙️", "Ajustes", False)]
    nw = W // len(nav_items)
    for i, (emoji, lbl, active) in enumerate(nav_items):
        nx = i * nw + nw // 2
        col = ACCENT if active else GREY_M
        d.text((nx, nav_y + 38), emoji, font=fnt(40), fill=col, anchor="mm")
        d.text((nx, nav_y + 88), lbl, font=fnt(26, bold=active), fill=col, anchor="mm")
        if active:
            d.rectangle([nx - 30, nav_y + 4, nx + 30, nav_y + 10], fill=ACCENT)

    return img

# ─────────────────────────────────────────────────────────────────────────────
# PANTALLA 2: Bloqueo en tiempo real (lista de bloqueados)
# ─────────────────────────────────────────────────────────────────────────────
def screen_blocked_list():
    img = gradient_bg(W, H)
    d = ImageDraw.Draw(img)
    status_bar(d)
    y = top_bar(img, d, "Bloqueados en tiempo real", "Últimas 24 horas")

    items = [
        ("pornhub.com",      "Contenido Adulto", RED,    "ALTO",   "hace 2m"),
        ("tiktok.com",       "Redes Sociales",   ORANGE, "MEDIO",  "hace 8m"),
        ("xvideos.com",      "Contenido Adulto", RED,    "CRÍTICO","hace 15m"),
        ("discord.com",      "Redes Sociales",   ORANGE, "MEDIO",  "hace 23m"),
        ("bet365.com",       "Juegos de Azar",   YELLOW, "ALTO",   "hace 1h"),
        ("instagram.com",    "Redes Sociales",   ORANGE, "MEDIO",  "hace 1h"),
        ("twitter.com",      "Redes Sociales",   ORANGE, "BAJO",   "hace 2h"),
    ]

    for domain, cat, col, level, time_ago in items:
        rounded_rect(d, [40, y, W-40, y+140], 18, BG_CARD)
        # barra izquierda de color
        rounded_rect(d, [40, y, 54, y+140], 9, col)
        d.text((80, y+36), "🚫 " + domain, font=fnt(38, True), fill=WHITE, anchor="lm")
        badge(d, 80, y+90, cat, col)
        badge(d, 80 + fnt(30).getbbox(cat)[2] + 90, y+90, level, BG_CARD2)
        d.text((W-60, y+90), time_ago, font=fnt(28), fill=GREY_M, anchor="rm")
        y += 160

    # Contador
    rounded_rect(d, [40, y, W-40, y+110], 18, fill=RED, outline=None)
    d.text((W//2, y+55), "247 bloqueos hoy  •  +12% vs ayer", font=fnt(36, True), fill=WHITE, anchor="mm")

    return img

# ─────────────────────────────────────────────────────────────────────────────
# PANTALLA 3: Estadísticas
# ─────────────────────────────────────────────────────────────────────────────
def screen_statistics():
    img = gradient_bg(W, H)
    d = ImageDraw.Draw(img)
    status_bar(d)
    y = top_bar(img, d, "Estadísticas", "Esta semana")

    # Chips de período
    periods = ["Hoy", "7 días", "30 días"]
    px = 60
    for i, p in enumerate(periods):
        bw = 180
        fc = ACCENT if i == 1 else BG_CARD2
        rounded_rect(d, [px, y, px+bw, y+72], 36, fc)
        d.text((px+bw//2, y+36), p, font=fnt(32, i==1), fill=WHITE, anchor="mm")
        px += bw + 20
    y += 100

    # Gráfico de barras semanal
    rounded_rect(d, [40, y, W-40, y+340], 20, BG_CARD)
    d.text((80, y+28), "Bloqueos por día", font=fnt(38, True), fill=WHITE, anchor="lm")
    data = [120, 247, 189, 312, 198, 165, 230]
    days = ["L", "M", "X", "J", "V", "S", "D"]
    bar_w = 90
    bar_max_h = 210
    bx0 = 80
    chart_top = y + 90
    max_val = max(data)
    for i, (val, day) in enumerate(zip(data, days)):
        bx = bx0 + i * (bar_w + 20)
        bh = int(bar_max_h * val / max_val)
        col = ACCENT if i == 6 else BLUE_L
        rounded_rect(d, [bx, chart_top + bar_max_h - bh, bx+bar_w, chart_top+bar_max_h], 8, col)
        d.text((bx+bar_w//2, chart_top+bar_max_h+22), day, font=fnt(28), fill=GREY_L, anchor="mm")
        d.text((bx+bar_w//2, chart_top + bar_max_h - bh - 18), str(val), font=fnt(22), fill=GREY_L, anchor="mm")
    y += 360

    # Desglose categorías
    rounded_rect(d, [40, y, W-40, y+48], 10, BG_CARD2)
    d.text((80, y+24), "Desglose por categoría", font=fnt(38, True), fill=WHITE, anchor="lm")
    y += 70

    cats = [
        ("🔞 Adultos",       0.52, RED,    "52%"),
        ("📱 Redes Sociales",0.28, ORANGE, "28%"),
        ("🎰 Juegos Azar",   0.10, YELLOW, "10%"),
        ("🎮 Gaming",        0.06, (100,200,100), "6%"),
        ("🦠 Malware",       0.04, (180,80,200),  "4%"),
    ]
    for label, pct, col, pct_str in cats:
        rounded_rect(d, [40, y, W-40, y+110], 14, BG_CARD)
        d.text((80, y+24), label, font=fnt(34, True), fill=WHITE, anchor="lm")
        d.text((W-60, y+24), pct_str, font=fnt(34, True), fill=col, anchor="rm")
        progress_bar(d, 80, y+66, W-160, 22, pct, col)
        y += 130

    return img

# ─────────────────────────────────────────────────────────────────────────────
# PANTALLA 4: Control Parental (perfiles + horarios)
# ─────────────────────────────────────────────────────────────────────────────
def screen_parental():
    img = gradient_bg(W, H)
    d = ImageDraw.Draw(img)
    status_bar(d)
    y = top_bar(img, d, "Control Parental", "Gestiona perfiles y horarios")

    # Perfil activo
    rounded_rect(d, [40, y, W-40, y+200], 24, BG_CARD, outline=ACCENT, outline_w=2)
    d.ellipse([80, y+40, 180, y+160], fill=ACCENT)
    d.text((130, y+100), "👦", font=fnt(68), fill=WHITE, anchor="mm")
    d.text((210, y+65), "Perfil: Alejandro", font=fnt(44, True), fill=WHITE, anchor="lm")
    d.text((210, y+120), "10 años • Modo Colegio", font=fnt(32), fill=GREY_L, anchor="lm")
    badge(d, 210, y+155, "ACTIVO", GREEN)
    y += 230

    # Control de horarios
    rounded_rect(d, [40, y, W-40, y+200], 20, BG_CARD)
    d.text((80, y+28), "⏰  Horario de navegación", font=fnt(38, True), fill=WHITE, anchor="lm")
    d.text((80, y+82), "Permitido:", font=fnt(32), fill=GREY_M, anchor="lm")
    d.text((260, y+82), "16:00 – 21:00", font=fnt(36, True), fill=ACCENT, anchor="lm")
    d.text((80, y+134), "Hoy queda:", font=fnt(32), fill=GREY_M, anchor="lm")
    progress_bar(d, 80, y+160, W-160, 24, 0.63, GREEN)
    y += 228

    # Categorías bloqueadas
    rounded_rect(d, [40, y, W-40, y+48], 10, BG_CARD2)
    d.text((80, y+24), "Categorías bloqueadas", font=fnt(38, True), fill=WHITE, anchor="lm")
    y += 70

    toggles = [
        ("🔞 Contenido Adulto",  True,  RED),
        ("📱 Redes Sociales",    True,  ORANGE),
        ("🎰 Juegos de Azar",    True,  YELLOW),
        ("🎮 Videojuegos",       False, GREEN),
        ("🦠 Malware / Phishing",True,  RED),
    ]
    for label, on, col in toggles:
        rounded_rect(d, [40, y, W-40, y+100], 14, BG_CARD)
        d.text((80, y+50), label, font=fnt(36), fill=WHITE, anchor="lm")
        # toggle switch
        tx, ty = W-130, y+42
        rounded_rect(d, [tx, ty, tx+90, ty+36], 18, col if on else GREY_M)
        cx = tx+66 if on else tx+22
        d.ellipse([cx-14, ty+4, cx+14, ty+32], fill=WHITE)
        y += 120

    return img

# ─────────────────────────────────────────────────────────────────────────────
# PANTALLA 5: Navegador Seguro
# ─────────────────────────────────────────────────────────────────────────────
def screen_browser():
    img = gradient_bg(W, H)
    d = ImageDraw.Draw(img)
    status_bar(d)
    y = top_bar(img, d, "Navegador Seguro", "guardianOS • filtrado doble capa")

    # Barra de dirección
    rounded_rect(d, [40, y, W-40, y+100], 50, BG_CARD)
    d.text((95, y+50), "🔒", font=fnt(40), fill=GREEN, anchor="lm")
    d.text((160, y+50), "wikipedia.org", font=fnt(38), fill=WHITE, anchor="lm")
    d.text((W-80, y+50), "⟳", font=fnt(44), fill=GREY_L, anchor="rm")
    y += 128

    # Pantalla bloqueada
    rounded_rect(d, [40, y, W-40, H-180], 28, BG_CARD, outline=RED, outline_w=3)
    cy = y + (H - 180 - y) // 2
    d.text((W//2, cy - 120), "🚫", font=fnt(110), fill=RED, anchor="mm")
    d.text((W//2, cy + 10), "Sitio Bloqueado", font=fnt(56, True), fill=RED, anchor="mm")
    d.text((W//2, cy + 80), "Este contenido no está permitido", font=fnt(34), fill=GREY_L, anchor="mm")
    d.text((W//2, cy + 130), "en el perfil actual", font=fnt(34), fill=GREY_L, anchor="mm")
    d.text((W//2, cy + 196), "Categoría: Contenido Adulto", font=fnt(32), fill=ORANGE, anchor="mm")

    # Botones
    btn_y = cy + 260
    rounded_rect(d, [W//2-300, btn_y, W//2+300, btn_y+90], 45, ACCENT)
    d.text((W//2, btn_y+45), "Solicitar permiso al padre", font=fnt(36, True), fill=WHITE, anchor="mm")
    rounded_rect(d, [W//2-200, btn_y+110, W//2+200, btn_y+190], 45, BG_CARD2)
    d.text((W//2, btn_y+150), "Volver al inicio", font=fnt(34), fill=GREY_L, anchor="mm")

    return img

# ─────────────────────────────────────────────────────────────────────────────
# PANTALLA 6: Pacto Digital — Panel del hijo
# ─────────────────────────────────────────────────────────────────────────────
def screen_pact_child():
    img = gradient_bg(W, H)
    d = ImageDraw.Draw(img)
    status_bar(d)
    y = top_bar(img, d, "Pacto Digital Familiar", show_back=False)

    # TrustFlow badge
    rounded_rect(d, [40, y, W-40, y+220], 28, BG_CARD, outline=YELLOW, outline_w=3)
    d.text((80, y+38), "⚡ Modo Explorador", font=fnt(46, True), fill=YELLOW, anchor="lm")
    d.text((80, y+100), "Racha actual:", font=fnt(32), fill=GREY_M, anchor="lm")
    d.text((300, y+100), "12 días", font=fnt(36, True), fill=YELLOW, anchor="lm")
    d.text((80, y+150), "Próximo nivel en:", font=fnt(32), fill=GREY_M, anchor="lm")
    d.text((330, y+150), "18 días", font=fnt(36, True), fill=GREEN, anchor="lm")
    progress_bar(d, 80, y+190, W-160, 20, 12/30, YELLOW)
    y += 252

    # Solicitudes enviadas
    d.text((60, y), "📩 Mis solicitudes", font=fnt(40, True), fill=WHITE, anchor="lm")
    y += 60

    petitions = [
        ("Ver YouTube", "Aprobada", GREEN),
        ("Jugar Roblox 1h", "Pendiente", YELLOW),
        ("Instagram 30min", "Denegada", RED),
    ]
    for title, status, col in petitions:
        rounded_rect(d, [40, y, W-40, y+120], 16, BG_CARD)
        d.text((80, y+36), title, font=fnt(38), fill=WHITE, anchor="lm")
        tx = W - 60 - fnt(32, True).getbbox(status)[2] - 30
        rounded_rect(d, [tx - 14, y+28, W-60, y+88], 20, col)
        d.text((tx + (W-60-tx)//2, y+58), status, font=fnt(32, True), fill=WHITE, anchor="mm")
        y += 140

    # Nueva solicitud
    rounded_rect(d, [40, y, W-40, y+110], 28, ACCENT)
    d.text((W//2, y+55), "+ Nueva solicitud al padre", font=fnt(40, True), fill=WHITE, anchor="mm")

    return img

# ─────────────────────────────────────────────────────────────────────────────
# PANTALLA 7: Pacto Digital — Panel del padre (nivel y recompensas)
# ─────────────────────────────────────────────────────────────────────────────
def screen_pact_parent():
    img = gradient_bg(W, H)
    d = ImageDraw.Draw(img)
    status_bar(d)
    y = top_bar(img, d, "Panel del Padre 🔐", "Gestión y recompensas")

    # TrustLevel card
    rounded_rect(d, [40, y, W-40, y+250], 28, BG_CARD, outline=YELLOW, outline_w=3)
    d.text((80, y+36), "⚡ Alejandro — Explorador", font=fnt(44, True), fill=YELLOW, anchor="lm")
    d.text((80, y+98), "12 de 30 días para nivel Guardián", font=fnt(32), fill=GREY_L, anchor="lm")
    progress_bar(d, 80, y+148, W-160, 24, 12/30, YELLOW)
    d.text((80, y+196), "Progreso actual: 40%", font=fnt(30), fill=GREY_M, anchor="lm")
    y += 280

    # Acciones del padre
    d.text((60, y), "⚡ Acciones del padre", font=fnt(40, True), fill=WHITE, anchor="lm")
    y += 60

    btns = [
        ("🎮 Dar tiempo de gaming", BLUE_L, "Concede minutos extra hoy"),
        ("🏅 Ascender de nivel", GREEN,  "Reconoce el esfuerzo → sube nivel"),
        ("🔄 Resetear racha hoy", RED,   "Solo en caso de incumplimiento"),
    ]
    for label, col, help_text in btns:
        rounded_rect(d, [40, y, W-40, y+140], 18, BG_CARD, outline=col, outline_w=2)
        d.text((80, y+44), label, font=fnt(40, True), fill=col, anchor="lm")
        d.text((80, y+98), help_text, font=fnt(30), fill=GREY_M, anchor="lm")
        y += 162

    # Bonus gaming activo
    rounded_rect(d, [40, y, W-40, y+110], 16, (10,30,80), outline=(0,120,200), outline_w=2)
    d.text((80, y+30), "🎮 Bonus gaming activo hoy", font=fnt(34, True), fill=(100,180,255), anchor="lm")
    d.text((80, y+76), "45 min disponibles — se gastan minuto a minuto", font=fnt(28), fill=GREY_M, anchor="lm")

    return img

# ─────────────────────────────────────────────────────────────────────────────
# PANTALLA 8: Configuración y seguridad
# ─────────────────────────────────────────────────────────────────────────────
def screen_settings():
    img = gradient_bg(W, H)
    d = ImageDraw.Draw(img)
    status_bar(d)
    y = top_bar(img, d, "Configuración", show_back=False)

    # Premium badge
    rounded_rect(d, [40, y, W-40, y+110], 20, (40,30,10), outline=YELLOW, outline_w=2)
    d.text((80, y+28), "⭐ GuardianOS Premium", font=fnt(40, True), fill=YELLOW, anchor="lm")
    d.text((80, y+74), "Activo · Todas las funciones desbloqueadas", font=fnt(30), fill=GREY_L, anchor="lm")
    y += 140

    sections = [
        ("🔐 Seguridad y PIN",         ["Cambiar PIN", "Bloqueo biométrico", "Anti-desinstalación"]),
        ("🛡️ Protección DNS",          ["DNS principal: 185.228.168.168", "DNS secundario: 185.228.169.168", "Protocolo: UDP/53"]),
        ("👤 Perfiles",                ["Perfil activo: Alejandro", "Crear nuevo perfil", "Importar / Exportar"]),
        ("📓 Registros y privacidad",  ["Limpiar logs automáticamente", "Exportar estadísticas", "Restablecer base de datos"]),
    ]

    for section_title, items in sections:
        rounded_rect(d, [40, y, W-40, y+40], 0, BG_CARD2)
        d.text((60, y+20), section_title, font=fnt(32, True), fill=ACCENT, anchor="lm")
        y += 52
        for item in items:
            rounded_rect(d, [40, y, W-40, y+86], 0, BG_CARD)
            d.line([(40, y+86), (W-40, y+86)], fill=(30,50,90), width=1)
            d.text((80, y+43), item, font=fnt(34), fill=WHITE, anchor="lm")
            d.text((W-70, y+43), "›", font=fnt(44), fill=GREY_M, anchor="rm")
            y += 88
        y += 12

    return img

# ─────────────────────────────────────────────────────────────────────────────
# GUARDAR TODO
# ─────────────────────────────────────────────────────────────────────────────

screens = [
    ("01_dashboard.png",         screen_dashboard),
    ("02_blocked_list.png",       screen_blocked_list),
    ("03_statistics.png",         screen_statistics),
    ("04_parental_control.png",   screen_parental),
    ("05_safe_browser.png",       screen_browser),
    ("06_pact_child.png",         screen_pact_child),
    ("07_pact_parent.png",        screen_pact_parent),
    ("08_settings.png",           screen_settings),
]

print("🎨 Generando mockup screenshots para Play Store...\n")
for filename, fn in screens:
    out = os.path.join(OUT_PHONE, filename)
    img = fn()
    img.save(out, "PNG", optimize=True)
    print(f"  ✅ {filename}  →  {out}")

print(f"\n✨ {len(screens)} capturas generadas en: {OUT_PHONE}")
print("\n📋 Resumen Play Store:")
print("   • 512×512 icon          →  play_store_assets/icon_512x512.png")
print("   • 1024×500 feature gfx  →  play_store_assets/feature_graphic_1024x500.png")
print(f"  • {len(screens)} phone screenshots  →  play_store_assets/screenshots/phone/")
