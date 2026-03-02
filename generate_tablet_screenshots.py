#!/usr/bin/env python3
"""
Genera capturas para Tablet 7" y Tablet 10" para Google Play Store.

• Tablet 7"  → 1080×1920 (9:16) — copia directa de las de teléfono (válido: lados 320–3840 px)
• Tablet 10" → 1920×1080 (16:9) landscpae — composición con el mockup de teléfono a la izquierda
                                              y texto de característica a la derecha.
                                              (válido: lados 1080–7680 px)
"""

from PIL import Image, ImageDraw, ImageFont
import os, shutil, glob

BASE      = os.path.dirname(os.path.abspath(__file__))
ASSETS    = os.path.join(BASE, "play_store_assets")
IN_PHONE  = os.path.join(ASSETS, "screenshots", "phone")
OUT_TAB7  = os.path.join(ASSETS, "screenshots", "tablet7")
OUT_TAB10 = os.path.join(ASSETS, "screenshots", "tablet10")
ICON_PATH = os.path.join(ASSETS, "icon_512x512.png")

os.makedirs(OUT_TAB7,  exist_ok=True)
os.makedirs(OUT_TAB10, exist_ok=True)

FONT_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_N = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

# ── Paleta ────────────────────────────────────────────────────────────────────
BG_DARK  = (8,  18,  45)
BG_CARD  = (13, 27,  64)
BG_CARD2 = (18, 38,  85)
ACCENT   = (0,  188, 212)
GREEN    = (0,  200, 100)
WHITE    = (255, 255, 255)
GREY_L   = (180, 200, 230)
GREY_M   = (100, 130, 170)

# ── Características por pantalla (mismo orden que generate_mockup_screenshots.py)
FEATURES = [
    {
        "title":    "Panel de control",
        "subtitle": "Protección VPN + DNS\nactiva en tiempo real",
        "bullets":  ["✅  VPN DNS activa", "✅  CleanBrowsing Adult Filter", "✅  185.228.168.168 / .169"],
        "tag":      "PROTECCIÓN",
    },
    {
        "title":    "Bloqueos en directo",
        "subtitle": "Historial de todos los\naccesos bloqueados",
        "bullets":  ["🚫  Adultos bloqueados", "🚫  Redes sociales", "🚫  Malware y phishing"],
        "tag":      "SEGURIDAD",
    },
    {
        "title":    "Estadísticas",
        "subtitle": "Gráficos semanales\ny desglose por categoría",
        "bullets":  ["📊  Barras por día", "📊  Categorías en %", "📊  Comparativa ayer/hoy"],
        "tag":      "ESTADÍSTICAS",
    },
    {
        "title":    "Control Parental",
        "subtitle": "Perfiles, horarios\ny categorías por hijo",
        "bullets":  ["👤  Perfil por hijo", "⏰  Horario permitido", "🔞  Categorías on/off"],
        "tag":      "PARENTAL",
    },
    {
        "title":    "Navegador Seguro",
        "subtitle": "Doble capa: DNS +\nfiltrado por URL local",
        "bullets":  ["🔒  HTTPS verificado", "🚫  Bloqueo por dominio", "📋  Lista personalizada"],
        "tag":      "NAVEGADOR",
    },
    {
        "title":    "Pacto Digital — Hijo",
        "subtitle": "El niño pide permisos\ny acumula confianza",
        "bullets":  ["📩  Solicitudes al padre", "⭐  Sistema de niveles", "🏅  Recompensas"],
        "tag":      "PACTO HIJO",
    },
    {
        "title":    "Pacto Digital — Padre",
        "subtitle": "Aprueba, asciende\ny premia a tu hijo",
        "bullets":  ["✅  Aprobar / Denegar", "🎮  Bonos de gaming", "🔄  Gestión de niveles"],
        "tag":      "PACTO PADRE",
    },
    {
        "title":    "Configuración",
        "subtitle": "PIN, DNS, perfiles\ny registros de actividad",
        "bullets":  ["🔐  PIN anti-desinstalación", "🛡️  DNS configurables", "📓  Exportar registros"],
        "tag":      "AJUSTES",
    },
]


def fnt(size, bold=False):
    try:
        return ImageFont.truetype(FONT_B if bold else FONT_N, size)
    except Exception:
        return ImageFont.load_default()


def gradient_bg(w, h, top=BG_DARK, bot=(4, 10, 30)):
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(h - 1, 1)
        c = tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3))
        d.line([(0, y), (w, y)], fill=c)
    return img


def rounded_rect(draw, xy, radius, fill=None, outline=None, outline_w=2):
    x0, y0, x1, y1 = xy
    draw.rounded_rectangle([x0, y0, x1, y1], radius=radius,
                            fill=fill, outline=outline, width=outline_w)


# ─────────────────────────────────────────────────────────────────────────────
# TABLET 7" — copiar directamente las de teléfono (1080×1920, 9:16)
# Ambos lados dentro de [320, 3840] px y relación 9:16 ✓
# ─────────────────────────────────────────────────────────────────────────────
def generate_tablet7():
    print("\n📟 Generando capturas TABLET 7\" (1080×1920) — copia de teléfono…")
    phone_files = sorted(glob.glob(os.path.join(IN_PHONE, "*.png")))
    for src in phone_files:
        dst = os.path.join(OUT_TAB7, os.path.basename(src))
        shutil.copy2(src, dst)
        img = Image.open(dst)
        kb = os.path.getsize(dst) // 1024
        print(f"  ✅ {os.path.basename(dst)}  [{img.width}×{img.height} — {kb} KB]")
    print(f"  Total: {len(phone_files)} capturas → {OUT_TAB7}")


# ─────────────────────────────────────────────────────────────────────────────
# TABLET 10" — 1920×1080 landscape (16:9)
# Ambos lados dentro de [1080, 7680] px y relación 16:9 ✓
# Layout: mockup de teléfono a la izquierda + texto de características a la derecha
# ─────────────────────────────────────────────────────────────────────────────
def make_tablet10_frame(phone_img: Image.Image, feature: dict) -> Image.Image:
    W, H = 1920, 1080

    canvas = gradient_bg(W, H)
    d = ImageDraw.Draw(canvas)

    # ── Panel derecho: franja degradado suave ──────────────────────────────
    for x in range(W // 2, W):
        t = (x - W // 2) / (W // 2)
        c = tuple(int(BG_DARK[i] + (BG_CARD[i] - BG_DARK[i]) * t) for i in range(3))
        d.line([(x, 0), (x, H)], fill=c)

    # Separador vertical
    d.rectangle([W // 2 - 1, 60, W // 2 + 1, H - 60], fill=(ACCENT[0], ACCENT[1], ACCENT[2]))

    # ── Mockup de teléfono a la izquierda ─────────────────────────────────
    left_w = W // 2
    # Escalar el mockup para que quepa con padding
    pad = 60
    avail_w = left_w - 2 * pad
    avail_h = H - 2 * pad
    ratio = min(avail_w / phone_img.width, avail_h / phone_img.height)
    sw = int(phone_img.width * ratio)
    sh = int(phone_img.height * ratio)
    sx = (left_w - sw) // 2
    sy = (H - sh) // 2

    # Sombra
    for offset in range(10, 0, -1):
        a = int(50 * (1 - offset / 10))
        d.rectangle([sx + offset, sy + offset, sx + sw + offset, sy + sh + offset],
                    fill=(0, 0, 0, a) if False else (
                        max(0, BG_DARK[0] - 5),
                        max(0, BG_DARK[1] - 5),
                        max(0, BG_DARK[2] - 5)))
    phone_scaled = phone_img.resize((sw, sh), Image.LANCZOS)
    canvas.paste(phone_scaled, (sx, sy))
    # Borde sutil
    d.rectangle([sx - 1, sy - 1, sx + sw + 1, sy + sh + 1],
                outline=(80, 120, 200), width=1)

    # ── Panel derecho: contenido ───────────────────────────────────────────
    rx = W // 2 + 80  # inicio x del panel derecho
    rw = W - rx - 80  # ancho disponible panel derecho

    ry = 80

    # Icono de la app
    try:
        icon = Image.open(ICON_PATH).convert("RGBA").resize((90, 90))
        canvas.paste(icon, (rx, ry), icon)
        d.text((rx + 110, ry + 20), "GuardianOS Shield", font=fnt(38, True), fill=ACCENT, anchor="lm")
        d.text((rx + 110, ry + 62), "Control Parental Avanzado", font=fnt(26), fill=GREY_M, anchor="lm")
    except Exception:
        d.text((rx, ry + 20), "GuardianOS Shield", font=fnt(38, True), fill=ACCENT, anchor="lm")

    ry += 120

    # Tag
    tag = feature["tag"]
    tw = fnt(26, True).getbbox(tag)[2] + 40
    rounded_rect(d, [rx, ry, rx + tw, ry + 48], 24, fill=ACCENT)
    d.text((rx + tw // 2, ry + 24), tag, font=fnt(26, True), fill=WHITE, anchor="mm")
    ry += 68

    # Título grande
    d.text((rx, ry), feature["title"], font=fnt(72, True), fill=WHITE, anchor="lm")
    ry += 90

    # Subtítulo (puede tener \n)
    for line in feature["subtitle"].split("\n"):
        d.text((rx, ry), line, font=fnt(36), fill=GREY_L, anchor="lm")
        ry += 50
    ry += 20

    # Separador
    d.line([(rx, ry), (rx + rw, ry)], fill=(40, 70, 130), width=2)
    ry += 30

    # Bullets
    for bullet in feature["bullets"]:
        rounded_rect(d, [rx, ry, rx + rw, ry + 70], 14, fill=BG_CARD2)
        d.text((rx + 30, ry + 35), bullet, font=fnt(34), fill=WHITE, anchor="lm")
        ry += 86

    ry += 20

    # Footer
    d.text((rx, H - 60), "guardianOS.shield  ·  Control Parental para Android",
           font=fnt(26), fill=GREY_M, anchor="lm")

    return canvas


def generate_tablet10():
    print("\n🖥️  Generando capturas TABLET 10\" (1920×1080, 16:9 landscape)…")
    phone_files = sorted(glob.glob(os.path.join(IN_PHONE, "*.png")))
    for i, src in enumerate(phone_files):
        phone_img = Image.open(src).convert("RGB")
        feature = FEATURES[i % len(FEATURES)]
        out_img = make_tablet10_frame(phone_img, feature)
        name = os.path.basename(src)
        dst = os.path.join(OUT_TAB10, name)
        out_img.save(dst, "PNG", optimize=True)
        kb = os.path.getsize(dst) // 1024
        print(f"  ✅ {name}  [1920×1080 — {kb} KB]")
    print(f"  Total: {len(phone_files)} capturas → {OUT_TAB10}")


# ─────────────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    generate_tablet7()
    generate_tablet10()

    print("\n✅ Proceso completo.")
    print("""
📋 Resumen para Play Console — Sección Gráficos:

  Icono de aplicación (512×512):
    → play_store_assets/icon_512x512.png                  (102 KB ≤ 1 MB ✓)

  Gráfico de funciones (1024×500):
    → play_store_assets/feature_graphic_1024x500.png      (138 KB ≤ 15 MB ✓)

  Capturas de teléfono (1080×1920, 9:16):
    → play_store_assets/screenshots/phone/  (8 capturas, ≥ 1080 px ✓, ≤ 3840 px ✓)

  Capturas tablet 7\" (1080×1920, 9:16):
    → play_store_assets/screenshots/tablet7/  (8 capturas, lados 320–3840 px ✓)

  Capturas tablet 10\" (1920×1080, 16:9):
    → play_store_assets/screenshots/tablet10/ (8 capturas, lados ≥ 1080 px ✓)
""")
