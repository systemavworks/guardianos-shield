#!/usr/bin/env python3
"""
Genera los iconos PNG para todas las densidades de Guardianos Shield.
Diseño: fondo azul redondeado + escudo blanco + checkmark azul
Ejecutar desde la raíz del proyecto: python3 generate_icons.py
"""

try:
    from PIL import Image, ImageDraw
except ImportError:
    import subprocess, sys
    subprocess.check_call([sys.executable, "-m", "pip", "install", "Pillow"])
    from PIL import Image, ImageDraw

import os

# ──────────────────────────────────────────────
# Tamaños requeridos para Play Store / Android
# ──────────────────────────────────────────────
SIZES = {
    "mipmap-mdpi":     48,
    "mipmap-hdpi":     72,
    "mipmap-xhdpi":    96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}
PLAY_STORE_SIZE = 512   # Para subir manualmente en Play Console
BASE_RES = "app/src/main/res"
BLUE_BG   = (21, 101, 192, 255)   # #1565C0
BLUE_MARK = (21, 101, 192, 255)   # #1565C0
WHITE     = (255, 255, 255, 255)


def draw_icon(canvas_size: int, circular: bool = False) -> Image.Image:
    """Dibuja el icono a resolución 4x y lo reduce (antialiasing)."""
    SS = 4                            # supersampling
    W  = canvas_size * SS
    vp = 108.0                        # viewport original
    sc = W / vp                       # escala

    img  = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # ── Fondo ──────────────────────────────────
    if circular:
        draw.ellipse([0, 0, W - 1, W - 1], fill=BLUE_BG)
    else:
        r = int(W * 0.22)
        try:
            draw.rounded_rectangle([0, 0, W - 1, W - 1], radius=r, fill=BLUE_BG)
        except AttributeError:          # Pillow < 8.2
            draw.ellipse([0, 0, W - 1, W - 1], fill=BLUE_BG)

    # ── Escudo (blanco) ─────────────────────────
    # Coordenadas en espacio 108×108
    shield_pts = [
        (54, 22), (78, 32), (78, 55),
        (72, 68), (54, 83), (36, 68),
        (30, 55), (30, 32),
    ]
    shield_scaled = [(x * sc, y * sc) for x, y in shield_pts]
    draw.polygon(shield_scaled, fill=WHITE)

    # ── Checkmark (azul, trazo grueso) ──────────
    stroke = max(3, int(6.5 * sc))
    pts = [(40 * sc, 55 * sc), (50 * sc, 66 * sc), (70 * sc, 41 * sc)]
    draw.line([pts[0], pts[1]], fill=BLUE_MARK, width=stroke)
    draw.line([pts[1], pts[2]], fill=BLUE_MARK, width=stroke)
    # Extremos redondeados
    cap = stroke // 2
    for px, py in pts:
        draw.ellipse([px - cap, py - cap, px + cap, py + cap], fill=BLUE_MARK)

    # ── Reducir con LANCZOS ────────────────────
    return img.resize((canvas_size, canvas_size), Image.LANCZOS)


def save(img: Image.Image, path: str) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, "PNG", optimize=True)
    size = img.size[0]
    print(f"  ✅  {path}  ({size}×{size})")


# ──────────────────────────────────────────────
# Generar mipmap-* para dispositivos Android
# ──────────────────────────────────────────────
print("\n🎨  Generando iconos para Guardianos Shield...\n")

for folder, size in SIZES.items():
    res_dir = os.path.join(BASE_RES, folder)
    save(draw_icon(size, circular=False), os.path.join(res_dir, "ic_launcher.png"))
    save(draw_icon(size, circular=True),  os.path.join(res_dir, "ic_launcher_round.png"))

# ──────────────────────────────────────────────
# Icono 512×512 para Play Store
# ──────────────────────────────────────────────
play_dir = "play_store_assets"
save(draw_icon(PLAY_STORE_SIZE, circular=False),
     os.path.join(play_dir, "icon_512x512.png"))
save(draw_icon(PLAY_STORE_SIZE, circular=True),
     os.path.join(play_dir, "icon_512x512_round.png"))

print("\n🎉  ¡Iconos generados!")
print(f"    → Mipmaps en {BASE_RES}/mipmap-*/")
print(f"    → Icono Play Store en {play_dir}/icon_512x512.png")
print("    → Sube 'icon_512x512.png' manualmente en Google Play Console\n")
