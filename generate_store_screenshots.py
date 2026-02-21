#!/usr/bin/env python3
"""
Genera capturas de pantalla para Google Play Store con marco de marca.

Salidas:
  play_store_assets/screenshots/phone/     → 1080×1920 (9:16)  — OPPO A80
  play_store_assets/screenshots/tablet7/   → 1080×1920 (9:16)  — OANGCC A15 (7")
  play_store_assets/screenshots/tablet10/  → 1600×2560 (5:8)   — simulado 10"
"""

from PIL import Image, ImageDraw, ImageFont
import os, glob

# ── Rutas ────────────────────────────────────────────────────────────────────
BASE        = os.path.dirname(os.path.abspath(__file__))
SHOTS_DIR   = os.path.join(BASE, "Screenshots")
ICON_PATH   = os.path.join(BASE, "play_store_assets", "icon_512x512.png")
OUT_PHONE   = os.path.join(BASE, "play_store_assets", "screenshots", "phone")
OUT_TAB7    = os.path.join(BASE, "play_store_assets", "screenshots", "tablet7")
OUT_TAB10   = os.path.join(BASE, "play_store_assets", "screenshots", "tablet10")

FONT_BOLD   = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_NORMAL = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

# ── Colores ───────────────────────────────────────────────────────────────────
BG      = (10, 22, 58)
BG2     = (18, 52, 110)
CYAN    = (0, 188, 212)
GREEN   = (0, 230, 118)
WHITE   = (255, 255, 255)
DARK    = (13, 27, 64)

# ── Captions por índice (0-based) ─────────────────────────────────────────────
CAPTIONS = [
    ("🛡️ Protección activa",        "VPN DNS con CleanBrowsing"),
    ("🚫 Bloqueo en tiempo real",    "Contenido adulto, redes sociales y malware"),
    ("📱 Control de aplicaciones",   "Monitorización de apps en segundo plano"),
    ("🌐 Navegador seguro",          "Doble capa de filtrado por URL y dominio"),
    ("📊 Estadísticas de bloqueo",   "Historial de intentos y categorías"),
    ("⏰ Control de horarios",       "Define cuándo pueden navegar tus hijos"),
    ("👤 Perfiles por hijo",         "Configuración independiente por perfil"),
    ("🔐 PIN de seguridad",          "Protege la app contra desactivaciones"),
]

def gradient_strip(w, h, left=BG, right=BG2):
    """Franja con degradado horizontal."""
    strip = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(strip)
    for x in range(w):
        t = x / max(w - 1, 1)
        c = tuple(int(left[i] + (right[i] - left[i]) * t) for i in range(3))
        d.line([(x, 0), (x, h)], fill=c)
    return strip

def load_fonts(scale=1.0):
    s = scale
    try:
        return {
            "big":   ImageFont.truetype(FONT_BOLD,   int(38 * s)),
            "small": ImageFont.truetype(FONT_NORMAL, int(24 * s)),
            "tiny":  ImageFont.truetype(FONT_NORMAL, int(18 * s)),
        }
    except Exception:
        f = ImageFont.load_default()
        return {"big": f, "small": f, "tiny": f}

def make_framed(src_img, out_w, out_h, caption_idx, accent=CYAN):
    """
    Encuadra src_img en un canvas out_w×out_h con:
      - Header (degradado + título + subtítulo)
      - Cuerpo: screenshot escalado con márgenes laterales
      - Footer: pequeño con nombre app e icono
    """
    header_h = int(out_h * 0.14)
    footer_h = int(out_h * 0.065)
    body_h   = out_h - header_h - footer_h

    scale = out_w / max(src_img.width, 1)
    fonts = load_fonts(scale=out_w / 1080)

    canvas = Image.new("RGB", (out_w, out_h), BG)
    draw   = ImageDraw.Draw(canvas)

    # ── Header ────────────────────────────────────────────────────────────────
    hdr = gradient_strip(out_w, header_h)
    canvas.paste(hdr, (0, 0))
    draw = ImageDraw.Draw(canvas)

    title, subtitle = CAPTIONS[caption_idx % len(CAPTIONS)]
    pad = int(out_w * 0.055)
    cy_title = int(header_h * 0.38)
    cy_sub   = int(header_h * 0.72)

    # Barra de acento izquierda
    bar_w = max(4, int(out_w * 0.007))
    draw.rectangle([pad, int(header_h*0.18), pad + bar_w, int(header_h*0.82)],
                   fill=accent)

    draw.text((pad + bar_w + int(out_w*0.02), cy_title),
              title, font=fonts["big"], fill=WHITE, anchor="lm")
    draw.text((pad + bar_w + int(out_w*0.02), cy_sub),
              subtitle, font=fonts["small"], fill=(185, 220, 255), anchor="lm")

    # ── Cuerpo: screenshot ────────────────────────────────────────────────────
    margin_x = int(out_w * 0.04)
    avail_w  = out_w - 2 * margin_x
    avail_h  = body_h - int(out_h * 0.02)

    # Escalar manteniendo ratio
    ratio = min(avail_w / src_img.width, avail_h / src_img.height)
    sw = int(src_img.width  * ratio)
    sh = int(src_img.height * ratio)
    sx = (out_w - sw) // 2
    sy = header_h + (avail_h - sh) // 2

    scaled = src_img.resize((sw, sh), Image.LANCZOS)

    # Sombra suave detrás del screenshot
    shadow_layer = Image.new("RGBA", (out_w, out_h), (0,0,0,0))
    sd = ImageDraw.Draw(shadow_layer)
    for offset in range(8, 0, -1):
        a = int(40 * (1 - offset/8))
        sd.rectangle([sx+offset, sy+offset, sx+sw+offset, sy+sh+offset],
                     fill=(0,0,0,a))
    canvas_rgba = canvas.convert("RGBA")
    canvas_rgba = Image.alpha_composite(canvas_rgba, shadow_layer)
    canvas = canvas_rgba.convert("RGB")
    canvas.paste(scaled, (sx, sy))
    draw = ImageDraw.Draw(canvas)

    # Borde sutil alrededor del screenshot
    draw.rectangle([sx-1, sy-1, sx+sw+1, sy+sh+1],
                   outline=(255,255,255,60), width=1)

    # ── Footer ────────────────────────────────────────────────────────────────
    fy = out_h - footer_h
    ftr = gradient_strip(out_w, footer_h, left=(8,18,48), right=(15,45,95))
    canvas.paste(ftr, (0, fy))
    draw = ImageDraw.Draw(canvas)

    # Icono pequeño en footer
    if os.path.exists(ICON_PATH):
        ico = Image.open(ICON_PATH).convert("RGBA")
        ico_h = int(footer_h * 0.65)
        ico = ico.resize((ico_h, ico_h), Image.LANCZOS)
        ico_y = fy + (footer_h - ico_h) // 2
        canvas.paste(ico, (pad, ico_y), ico)
        text_x = pad + ico_h + int(out_w * 0.02)
    else:
        text_x = pad

    draw.text((text_x, fy + footer_h // 2),
              "GuardianOS Shield  ·  Control Parental",
              font=fonts["tiny"], fill=(180, 210, 255), anchor="lm")

    # Punto de acento derecha
    dot_r = max(4, int(footer_h * 0.12))
    draw.ellipse([out_w - pad - dot_r*2, fy + footer_h//2 - dot_r,
                  out_w - pad,           fy + footer_h//2 + dot_r],
                 fill=accent)

    return canvas


def process_batch(src_files, out_dir, out_w, out_h, label, max_shots=6,
                  accent=CYAN):
    os.makedirs(out_dir, exist_ok=True)
    files = src_files[:max_shots]
    for i, path in enumerate(files):
        src = Image.open(path).convert("RGB")
        out = make_framed(src, out_w, out_h, caption_idx=i, accent=accent)
        name = f"{label}_{i+1:02d}.jpg"
        dest = os.path.join(out_dir, name)
        out.save(dest, "JPEG", quality=92, optimize=True)
        kb = os.path.getsize(dest) // 1024
        print(f"  ✅ {dest}  [{out_w}×{out_h} — {kb} KB]")


def main():
    pngs = sorted(glob.glob(os.path.join(SHOTS_DIR, "*.png")))
    jpgs = sorted(glob.glob(os.path.join(SHOTS_DIR, "*.jpg")))

    print(f"\nPNGs (OPPO A80 — teléfono): {len(pngs)} archivos")
    print(f"JPGs (OANGCC A15 — tablet):  {len(jpgs)} archivos\n")

    # ── Teléfono → 1080×1920 (9:16) ─────────────────────────────────────────
    print("📱 Generando capturas de TELÉFONO (1080×1920)…")
    process_batch(pngs, OUT_PHONE, 1080, 1920, "phone", max_shots=6,
                  accent=CYAN)

    # ── Tablet 7" → 1080×1920 (mismas dimensiones, capturas reales diferentes)
    print("\n📟 Generando capturas de TABLET 7\" (1080×1920)…")
    process_batch(jpgs, OUT_TAB7, 1080, 1920, "tablet7", max_shots=6,
                  accent=GREEN)

    # ── Tablet 10" → 1600×2560 (capturas ampliadas del tablet)
    print("\n🖥️  Generando capturas de TABLET 10\" (1600×2560)…")
    process_batch(jpgs, OUT_TAB10, 1600, 2560, "tablet10", max_shots=6,
                  accent=CYAN)

    print("\n✅ Proceso completo.")
    print(f"   Phone   → {OUT_PHONE}")
    print(f"   Tablet7 → {OUT_TAB7}")
    print(f"   Tablet10→ {OUT_TAB10}")


if __name__ == "__main__":
    main()
