from PIL import Image, ImageDraw, ImageFont

SIZE = 1024
BLUE = (21, 101, 192)        # #1565C0
BLUE_LIGHT = (30, 136, 229)  # #1E88E5
WHITE = (255, 255, 255)
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
OUT = "/store/KI/KI Projekt/M-Dateimanager"

def draw_M_centered(draw, cx, cy, size, fill):
    font = ImageFont.truetype(FONT, size)
    bbox = draw.textbbox((0, 0), "M", font=font)
    w = bbox[2] - bbox[0]
    h = bbox[3] - bbox[1]
    x = cx - w / 2 - bbox[0]
    y = cy - h / 2 - bbox[1]
    draw.text((x, y), "M", font=font, fill=fill)

# ---- Logo 1: Ordner (Folder) ----
img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.rounded_rectangle([160, 320, 864, 800], radius=70, fill=BLUE)       # body
d.rounded_rectangle([160, 185, 540, 365], radius=48, fill=BLUE_LIGHT) # tab
draw_M_centered(d, 512, 555, 360, WHITE)
img.save(f"{OUT}/logo-1-ordner.png")

# ---- Logo 2: Kreis (Circle monogram) ----
img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.ellipse([120, 120, 904, 904], fill=BLUE)
draw_M_centered(d, 512, 512, 480, WHITE)
img.save(f"{OUT}/logo-2-kreis.png")

# ---- Logo 3: Kachel (Squircle tile + M + line) ----
img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.rounded_rectangle([100, 100, 924, 924], radius=200, fill=BLUE)
draw_M_centered(d, 512, 460, 360, WHITE)
d.rounded_rectangle([396, 640, 628, 692], radius=26, fill=WHITE)      # file hint line
img.save(f"{OUT}/logo-3-kachel.png")

print("DONE")
for n in ["logo-1-ordner.png", "logo-2-kreis.png", "logo-3-kachel.png"]:
    print(n)
