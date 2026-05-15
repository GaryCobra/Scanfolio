import urllib.request
import os
import zipfile
import tempfile

url = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.12%2B7/OpenJDK17U-jdk_x64_windows_hotspot_17.0.12_7.zip"
out = os.path.join(tempfile.gettempdir(), "jdk17.zip")
extract_dir = r"C:\Java"

print("Downloading JDK 17...")
urllib.request.urlretrieve(url, out)
print(f"Downloaded: {os.path.getsize(out)} bytes")

os.makedirs(extract_dir, exist_ok=True)
print("Extracting...")
with zipfile.ZipFile(out, "r") as zf:
    zf.extractall(extract_dir)
os.remove(out)

for d in os.listdir(extract_dir):
    if d.startswith("jdk"):
        jdk_home = os.path.join(extract_dir, d)
        print(f"JDK at: {jdk_home}")
        java_exe = os.path.join(jdk_home, "bin", "java.exe")
        if os.path.exists(java_exe):
            print("java.exe OK")
        with open(os.path.join(extract_dir, "jdk_home.txt"), "w") as f:
            f.write(jdk_home)
        break
