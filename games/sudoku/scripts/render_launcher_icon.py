"""Renders the legacy (pre-API 26) launcher mipmap PNGs to match the adaptive icon's design:
a diagonal blue gradient with a white 3x3 sudoku grid and one gold highlighted cell.

Run with: python scripts/render_launcher_icon.py
Requires Pillow (pip install Pillow).
"""
import math
import os

from PIL import Image, ImageDraw

MASTER = 512
SCALE = MASTER / 108.0

BG_START = (0x4A, 0x93, 0xB8)
BG_END = (0x1B, 0x43, 0x56)
GRID_COLOR = (0xFD, 0xFE, 0xFF)
ACCENT_COLOR = (0xF5, 0xB9, 0x42)

OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")
DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def render_master() -> Image.Image:
    img = Image.new("RGB", (MASTER, MASTER))
    px = img.load()
    # diagonal linear gradient, top-left (BG_START) -> bottom-right (BG_END)
    denom = 2 * (MASTER - 1)
    for y in range(MASTER):
        for x in range(MASTER):
            t = (x + y) / denom
            r = round(BG_START[0] + (BG_END[0] - BG_START[0]) * t)
            g = round(BG_START[1] + (BG_END[1] - BG_START[1]) * t)
            b = round(BG_START[2] + (BG_END[2] - BG_START[2]) * t)
            px[x, y] = (r, g, b)

    draw = ImageDraw.Draw(img)

    def s(v: float) -> float:
        return v * SCALE

    # highlighted cell (top-right box of the grid)
    draw.rectangle([s(67.3), s(22), s(86), s(40.7)], fill=ACCENT_COLOR)

    # outer border
    border_w = max(2, round(s(5.2)))
    draw.rectangle([s(20), s(20), s(88), s(88)], outline=GRID_COLOR, width=border_w)

    # inner grid lines
    inner_w = max(1, round(s(3.4)))
    draw.line([(s(42.7), s(20)), (s(42.7), s(88))], fill=GRID_COLOR, width=inner_w)
    draw.line([(s(65.3), s(20)), (s(65.3), s(88))], fill=GRID_COLOR, width=inner_w)
    draw.line([(s(20), s(42.7)), (s(88), s(42.7))], fill=GRID_COLOR, width=inner_w)
    draw.line([(s(20), s(65.3)), (s(88), s(65.3))], fill=GRID_COLOR, width=inner_w)

    return img


def masked(img: Image.Image, mask: Image.Image) -> Image.Image:
    out = Image.new("RGBA", img.size)
    out.paste(img.convert("RGBA"), (0, 0), mask)
    return out


def circle_mask(size: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    return mask


def squircle_mask(size: int, margin_frac: float = 0.05, radius_frac: float = 0.2) -> Image.Image:
    # legacy (pre-API26) launcher icons must not be a full-bleed square: inset with a
    # rounded-rect silhouette so there is transparent padding, per the launcher icon guidelines.
    mask = Image.new("L", (size, size), 0)
    margin = round(size * margin_frac)
    radius = round(size * radius_frac)
    ImageDraw.Draw(mask).rounded_rectangle(
        [margin, margin, size - 1 - margin, size - 1 - margin], radius=radius, fill=255,
    )
    return mask


def main() -> None:
    master = render_master()
    for folder, size in DENSITIES.items():
        square = master.resize((size, size), Image.LANCZOS)
        icon = masked(square, squircle_mask(size))
        round_icon = masked(square, circle_mask(size))
        out_folder = os.path.join(OUT_DIR, folder)
        os.makedirs(out_folder, exist_ok=True)
        icon.save(os.path.join(out_folder, "ic_launcher.png"), format="PNG")
        round_icon.save(os.path.join(out_folder, "ic_launcher_round.png"), format="PNG")
        print("wrote", folder)


if __name__ == "__main__":
    main()
