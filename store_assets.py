# -*- coding: utf-8 -*-
"""Store-Grafiken für M-Dateimanager: Icon 512x512 + Feature-Graphic 1024x500."""
import os
from PIL import Image, ImageDraw, ImageFont

TOP = (0x42, 0xA5, 0xF5)    # #42A5F5
BOTTOM = (0x0D, 0x47, 0xA1)  # #0D47A1
WHITE = (255, 255, 255)

OUT_DIR = "/store/KI/KI Projekt/M-Dateimanager/store-assets"
os.makedirs(OUT_DIR, exist_ok=True)


def vgrad(w, h, top=TOP, bottom=BOTTOM):
    col = Image.new("RGB", (1, h))
    for y in range(h):
        t = y / (h - 1)
        col.putpixel((0, y), (
            int(top[0] + (bottom[0] - top[0]) * t),
            int(top[1] + (bottom[1] - top[1]) * t),
            int(top[2] + (bottom[2] - top[2]) * t)))
    return col.resize((w, h))


def draw_m(draw, cx, cy, scale):
    pts = [(34, 80), (34, 34), (54, 62), (74, 34), (74, 80)]
    xy = [(cx + (x - 54) * scale, cy + (y - 54) * scale) for (x, y) in pts]
    w = int(11 * scale)
    draw.line(xy, fill=WHITE, width=w, joint="curve")


def load_font(size, bold=True):
    for p in ["/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
              "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
              "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"]:
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


# ============ 1) App-Icon 512x512 ============
S = 512
img = vgrad(S, S)
mask = Image.new("L", (S, S), 0)
ImageDraw.Draw(mask).ellipse([0, 0, S, S], fill=255)
icon = Image.new("RGBA", (S, S), (0, 0, 0, 0))
icon.paste(img, (0, 0), mask)
d = ImageDraw.Draw(icon)
draw_m(d, S // 2, S // 2, S / 108.0)
icon.save(os.path.join(OUT_DIR, "icon-512.png"))
print("Icon:", os.path.join(OUT_DIR, "icon-512.png"))

# ============ 2) Feature-Graphic 1024x500 ============
W, H = 1024, 500
fg = vgrad(W, H, top=(0x0D, 0x47, 0xA1), bottom=(0x1A, 0x6B, 0xC8))
d = ImageDraw.Draw(fg)

# Logo-Kreis links (Durchmesser 300)
R = 150
lcx, lcy = 195, H // 2
logo = vgrad(R * 2, R * 2)
lmask = Image.new("L", (R * 2, R * 2), 0)
ImageDraw.Draw(lmask).ellipse([0, 0, R * 2, R * 2], fill=255)
fg.paste(logo, (lcx - R, lcy - R), lmask)
ld = ImageDraw.Draw(fg)
draw_m(ld, lcx, lcy, (2 * R) / 108.0)

# Text mit automatischer Breitenanpassung
tx = 390
maxw = W - tx - 30

def fit(text, max_size, max_w, bold=True):
    size = max_size
    while size > 16:
        f = load_font(size, bold)
        if d.textlength(text, font=f) <= max_w:
            return f, size
        size -= 2
    return load_font(16, bold), 16

ft, _ = fit("M-Dateimanager", 100, maxw, True)
fs, _ = fit("Dateimanager \u00b7 APK-Extraktor \u00b7 Netzwerk", 38, maxw, False)
d.text((tx, 155), "M-Dateimanager", fill=WHITE, font=ft)
d.text((tx, 285), "Dateimanager \u00b7 APK-Extraktor \u00b7 Netzwerk",
       fill=(0xCD, 0xD8, 0xEA), font=fs)
fg.save(os.path.join(OUT_DIR, "feature-graphic-1024x500.png"))
print("Feature-Graphic:", os.path.join(OUT_DIR, "feature-graphic-1024x500.png"))
print("FERTIG")
