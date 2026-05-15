import urllib.request
import os
import socket

socket.setdefaulttimeout(300)

url = "https://mirror.ghproxy.com/https://github.com/tesseract-ocr/tessdata_fast/raw/main/chi_sim.traineddata"
out = r"D:\workspace\Scanfolio\app\src\main\assets\tessdata\chi_sim.traineddata"

print("Downloading chi_sim.traineddata via mirror...")
try:
    urllib.request.urlretrieve(url, out)
    sz = os.path.getsize(out)
    print(f"OK: {sz} bytes ({sz//1024}KB)")
except Exception as e:
    print(f"Failed: {e}")
