import pytesseract
from PIL import Image
import os

test_dir = r"D:\workspace\Scanfolio\test_input"
files = sorted([f for f in os.listdir(test_dir) if f.lower().endswith(('.jpg','.jpeg','.png'))])

print(f"=== Found {len(files)} images ===")

for fname in files:
    path = os.path.join(test_dir, fname)
    img = Image.open(path)
    print(f"\n=== {fname} ===")
    print(f"Size: {img.size}, Mode: {img.mode}")
    
    try:
        text = pytesseract.image_to_string(img, lang='chi_sim+eng')
        print(f"OCR text ({len(text)} chars):")
        print(text)
    except Exception as e:
        print(f"OCR error: {e}")
