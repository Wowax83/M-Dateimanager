from PIL import Image, ImageDraw, ImageFilter, ImageFont

SIZE = 1024
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
OUT = "/store/KI/KI Projekt/M-Dateimanager"

TOP = (66, 165, 245)      # #42A5F5 light blue
BOTTOM = (13, 71, 161)     # #0D47A1 deep blue
WHITE = (255, 255, 255)

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def gradient_canvas(top, bottom):
    img = Image.new("RGB", (SIZE, SIZE))
    d = ImageDraw.Draw(img)
    for y in range(SIZE):
        d.line([(0, y), (SIZE, y)], fill=lerp(top, bottom, y / SIZE))
    return img

def rrect_mask(box, radius):
    m = Image.new("L", (SIZE, SIZE), 0)
    ImageDraw.Draw(m).rounded_rectangle(box, radius=radius, fill=255)
    return m

def ellipse_mask(box):
    m = Image.new("L", (SIZE, SIZE), 0)
    ImageDraw.Draw(m).ellipse(box, fill=255)
    return m

def gradient_shape(mask):
    grad = gradient_canvas(TOP, BOTTOM)
    out = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    out.paste(grad, (0, 0), mask)
    return out

def shadow(mask, offset_y=26, blur=32, alpha=110):
    colored = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    color_img = Image.new("RGBA", (SIZE, SIZE), (8, 40, 92, alpha))
    colored.paste(color_img, (0, 0), mask)
    colored = colored.filter(ImageFilter.GaussianBlur(blur))
    sh = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    sh.paste(colored, (0, offset_y))
    return sh

def draw_M(draw, cx, cy, size, fill):
    font = ImageFont.truetype(FONT, size)
    bbox = draw.textbbox((0, 0), "M", font=font)
    w = bbox[2] - bbox[0]
    h = bbox[3] - bbox[1]
    draw.text((cx - w / 2 - bbox[0], cy - h / 2 - bbox[1]), "M", font=font, fill=fill)

def render(mask, m_size, m_cy, name):
    base = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    base.alpha_composite(shadow(mask))
    base.alpha_composite(gradient_shape(mask))
    d = ImageDraw.Draw(base)
    draw_M(d, SIZE // 2, m_cy, m_size, WHITE)
    base.save(f"{OUT}/{name}")

# Logo 1: Ordner (modern)
mask = rrect_mask([160, 320, 864, 800], 70)
tab = rrect_mask([160, 185, 540, 365], 48)
mask = Image.composite(Image.new("L", (SIZE, SIZE), 255), mask, tab)
render(mask, 340, 555, "logo-modern-1-ordner.png")

# Logo 2: Kachel/Squircle (modern)
mask = rrect_mask([130, 130, 894, 894], 210)
render(mask, 380, 512, "logo-modern-2-kachel.png")

# Logo 3: Kreis (modern)
mask = ellipse_mask([140, 140, 884, 884])
render(mask, 440, 512, "logo-modern-3-kreis.png")

print("DONE")
