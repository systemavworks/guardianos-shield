#!/usr/bin/env python3
"""
Transforma las capturas ORIGINALES para cumplir los requisitos de Play Console.

- NO modifica ninguna imagen original
- Genera versiones en carpetas nuevas *_playstore/

  phone_playstore/   → canvas 1080×1920 (9:16) portrait  — las originales centradas
  tablet7_playstore/ → canvas 1080×1920 (9:16) portrait  — ídem
  tablet10_playstore/→ canvas 1920×1080 (16:9) landscape — las originales centradas

Fondo: color corporativo (8, 18, 45) que coincide con el dark theme de la app.
No se recorta ningún contenido: la imagen original cabe completa dentro del canvas.
"""

from PIL import Image
import os, glob, shutil

BASE   = os.path.dirname(os.path.abspath(__file__))
SHOTS  = os.path.join(BASE, "play_store_assets", "screenshots")

SRC_PHONE   = os.path.join(SHOTS, "phone")
SRC_TAB7    = os.path.join(SHOTS, "tablet7")
SRC_TAB10   = os.path.join(SHOTS, "tablet10")

DST_PHONE   = os.path.join(SHOTS, "phone_playstore")
DST_TAB7    = os.path.join(SHOTS, "tablet7_playstore")
DST_TAB10   = os.path.join(SHOTS, "tablet10_playstore")

BG_COLOR = (8, 18, 45)   # azul oscuro del dark theme


def fit_into_canvas(src_path: str, canvas_w: int, canvas_h: int, dst_path: str):
    """
    Abre src_path, escala la imagen para que quepa COMPLETAMENTE dentro del
    canvas (sin recortar, sin deformar), la centra sobre fondo BG_COLOR y
    guarda en dst_path como JPG calidad 95.
    """
    img = Image.open(src_path).convert("RGB")
    iw, ih = img.size

    # escala para que quepa completa manteniendo aspect ratio
    ratio = min(canvas_w / iw, canvas_h / ih)
    new_w = int(iw * ratio)
    new_h = int(ih * ratio)

    img_scaled = img.resize((new_w, new_h), Image.LANCZOS)

    canvas = Image.new("RGB", (canvas_w, canvas_h), BG_COLOR)
    offset_x = (canvas_w - new_w) // 2
    offset_y = (canvas_h - new_h) // 2
    canvas.paste(img_scaled, (offset_x, offset_y))

    # guardar como JPG (≤ 8 MB sin problema)
    dst_jpg = os.path.splitext(dst_path)[0] + ".jpg"
    canvas.save(dst_jpg, "JPEG", quality=95, optimize=True)
    kb = os.path.getsize(dst_jpg) // 1024
    name = os.path.basename(src_path)
    print(f"  ✅ {name:35s} {iw}x{ih} → canvas {canvas_w}x{canvas_h}  ({kb} KB)")
    return dst_jpg


def process(src_dir: str, dst_dir: str, canvas_w: int, canvas_h: int, label: str):
    os.makedirs(dst_dir, exist_ok=True)
    files = sorted(
        glob.glob(os.path.join(src_dir, "*.jpg")) +
        glob.glob(os.path.join(src_dir, "*.png"))
    )
    if not files:
        print(f"  ⚠️  No se encontraron imágenes en {src_dir}")
        return
    print(f"\n{'='*60}")
    print(f"  {label}  →  canvas {canvas_w}×{canvas_h}")
    print(f"  Origen : {src_dir}")
    print(f"  Destino: {dst_dir}")
    print(f"{'='*60}")
    for f in files:
        dst_name = os.path.splitext(os.path.basename(f))[0] + ".jpg"
        dst_path = os.path.join(dst_dir, dst_name)
        fit_into_canvas(f, canvas_w, canvas_h, dst_path)
    print(f"  → {len(files)} imágenes generadas")


def main():
    print("\n🎨  Transformando capturas para Play Console (SIN modificar originales)…\n")

    # Teléfono: 1080×1920 portrait (9:16)
    process(SRC_PHONE, DST_PHONE, 1080, 1920, "📱 TELÉFONO")

    # Tablet 7": 1080×1920 portrait (9:16)
    process(SRC_TAB7, DST_TAB7, 1080, 1920, '📟 TABLET 7"')

    # Tablet 10": 1920×1080 landscape (16:9)
    process(SRC_TAB10, DST_TAB10, 1920, 1080, '🖥️  TABLET 10" (landscape)')

    print("\n✅  Proceso completado. Originales intactos.")
    print("""
📋  Archivos listos para subir a Play Console:

  Teléfono  (1080×1920, 9:16):   play_store_assets/screenshots/phone_playstore/
  Tablet 7" (1080×1920, 9:16):   play_store_assets/screenshots/tablet7_playstore/
  Tablet 10"(1920×1080, 16:9):   play_store_assets/screenshots/tablet10_playstore/

  (Las carpetas phone/, tablet7/, tablet10/ no han sido modificadas)
""")


if __name__ == "__main__":
    main()
