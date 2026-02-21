#!/usr/bin/env python3
"""
Genera el gráfico de funciones para Google Play Store (1024x500 px)
Guardianos Shield — Control Parental
Layout ESTRICTO: texto SOLO en [0..555] | icono SOLO en [570..1024]
"""

from PIL import Image, ImageDraw, ImageFont
import os

BASE_DIR   = os.path.dirname(os.path.abspath(__file__))
ASSETS_DIR = os.path.join(BASE_DIR, "play_store_assets")
ICON_PATH  = os.path.join(ASSETS_DIR, "icon_512x512.png")
OUT_PATH   = os.path.join(ASSETS_DIR, "feature_graphic_1024x500.png")

FONT_BOLD   = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_NORMAL = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

W, H = 1024, 500

BG_L  = (10,  22,  58)
BG_R  = (18,  52, 110)
CYAN  = (0,  188, 212)
GREEN = (0,  230, 118)
WHITE = (255, 255, 255)
DARK  = (13,  27,  64)

# Zonas de layout — garantizan cero solapamiento
TEXT_ZONE_END = 555   # todo texto debe terminar antes de este X
ICON_CX       = 812
ICON_CY       = 250
ICON_SIZE     = 308


def build_gradient():
    img = Image.new("RGB", (W, H))
    d   = ImageDraw.Draw(img)
    for x in range(W):
        t = x / (W - 1)
        c = tuple(int(BG_L[i] + (BG_R[i] - BG_L[i]) * t) for i in range(3))
        d.line([(x, 0), (x, H)], fill=c)
    return img.convert("RGBA")


def add_halo(img):
    """Halo cian detrás del icono — solo zona derecha."""
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for r in range(230, 0, -4):
        a = int(68 * (1 - r / 230))
        cx, cy = ICON_CX, ICON_CY
        d.ellipse([cx - r, cy - r, cx + r, cy + r],
                  fill=(CYAN[0], CYAN[1], CYAN[2], a))
    return Image.alpha_composite(img, layer)


def add_icon(img):
    """Pega el icono en la zona derecha con sombra."""
    if not os.path.exists(ICON_PATH):
        return img
    icon = Image.open(ICON_PATH).convert("RGBA")
    icon = icon.resize((ICON_SIZE, ICON_SIZE), Image.LANCZOS)
    ix = ICON_CX - ICON_SIZE // 2
    iy = ICON_CY - ICON_SIZE // 2
    # Sombra
    sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sh)
    sd.ellipse([ix + 14, iy + 18,
                ix + ICON_SIZE + 14, iy + ICON_SIZE + 18],
               fill=(0, 0, 0, 85))
    img = Image.alpha_composite(img, sh)
    img.alpha_composite(icon, dest=(ix, iy))
    return img


def draw_pill(img, x, y, dot_color, text, font):
    """Píldora — texto renderizado en capa recortada al ancho de la píldora."""
    pw, ph = 290, 36
    # Fondo de la píldora sobre el canvas principal
    d_main = ImageDraw.Draw(img, "RGBA")
    d_main.rounded_rectangle([x, y, x + pw, y + ph],
                              radius=ph // 2,
                              fill=(255, 255, 255, 28))
    # Punto de color
    dr = 6
    d_main.ellipse([x + 12, y + ph // 2 - dr,
                    x + 12 + dr * 2, y + ph // 2 + dr],
                   fill=dot_color)
    # Texto en capa temporal recortada exactamente al ancho de la píldora
    text_layer = Image.new("RGBA", (pw - 30, ph), (0, 0, 0, 0))
    dt = ImageDraw.Draw(text_layer)
    dt.text((0, ph // 2), text, font=font, fill=WHITE, anchor="lm")
    img.alpha_composite(text_layer, dest=(x + 30, y))


def main():
    img = build_gradient()
    img = add_halo(img)
    img = add_icon(img)

    draw = ImageDraw.Draw(img, "RGBA")

    # Fuentes
    try:
        f_title = ImageFont.truetype(FONT_BOLD,   60)
        f_sub   = ImageFont.truetype(FONT_BOLD,   20)
        f_tag   = ImageFont.truetype(FONT_NORMAL, 16)
        f_pill  = ImageFont.truetype(FONT_BOLD,   14)
        f_badge = ImageFont.truetype(FONT_BOLD,   13)
        f_copy  = ImageFont.truetype(FONT_NORMAL, 12)
    except Exception:
        f_title = f_sub = f_tag = f_pill = f_badge = f_copy = \
            ImageFont.load_default()

    P = 48   # padding izquierdo

    # ── Títulos ───────────────────────────────────────────────────────────────
    for offset, color in [((2, 2), (0, 0, 0, 80)), ((0, 0), WHITE)]:
        draw.text((P + offset[0], 72 + offset[1]),
                  "GuardianOS", font=f_title, fill=color, anchor="lm")

    for offset, color in [((2, 2), (0, 0, 0, 80)), ((0, 0), CYAN)]:
        draw.text((P + offset[0], 146 + offset[1]),
                  "Shield", font=f_title, fill=color, anchor="lm")

    # Barra divisora
    draw.rectangle([P, 175, P + 285, 177], fill=CYAN)

    # Subtítulo
    draw.text((P, 196), "Control Parental Inteligente",
              font=f_sub, fill=WHITE, anchor="lm")

    # Tagline en dos líneas cortas
    draw.text((P, 224), "Filtrado DNS · Bloqueo de apps",
              font=f_tag, fill=(185, 218, 255, 185), anchor="lm")
    draw.text((P, 244), "Navegador seguro · Control horario",
              font=f_tag, fill=(185, 218, 255, 185), anchor="lm")

    # ── Pills en columna única — texto recortado al ancho de la píldora ───────
    pills = [
        (CYAN,  "Filtrado DNS CleanBrowsing"),
        (GREEN, "Bloqueo de apps en tiempo real"),
        (CYAN,  "Control de horarios"),
        (GREEN, "Navegador seguro integrado"),
    ]
    py = 282
    for dot_color, text in pills:
        draw_pill(img, P, py, dot_color, text, f_pill)
        py += 43
    draw = ImageDraw.Draw(img, "RGBA")  # refrescar draw tras alpha_composite

    # ── Badges ────────────────────────────────────────────────────────────────
    by, bh = 432, 27
    draw.rounded_rectangle([P, by, P + 76, by + bh], radius=13, fill=GREEN)
    draw.text((P + 38, by + bh // 2), "GRATIS",
              font=f_badge, fill=DARK, anchor="mm")

    draw.rounded_rectangle([P + 88, by, P + 88 + 100, by + bh],
                            radius=13, fill=(255, 214, 0))
    draw.text((P + 138, by + bh // 2), "★ PREMIUM",
              font=f_badge, fill=DARK, anchor="mm")

    # ── Copyright ─────────────────────────────────────────────────────────────
    draw.text((W - 16, H - 14), "GuardianOS · Sevilla, España",
              font=f_copy, fill=(255, 255, 255, 70), anchor="rm")

    # ── Guardar ───────────────────────────────────────────────────────────────
    img.convert("RGB").save(OUT_PATH, "PNG", optimize=True)
    kb = os.path.getsize(OUT_PATH) // 1024
    print(f"✅ {OUT_PATH}  [{W}x{H} px — {kb} KB]")


if __name__ == "__main__":
    main()
