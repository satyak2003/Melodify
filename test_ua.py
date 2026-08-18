import urllib.request
import subprocess

res = subprocess.run(['yt-dlp', '-f', 'bestaudio[ext=m4a]', '--get-url', 'https://www.youtube.com/watch?v=LpNVf8sczqU'], capture_output=True, text=True)
url = res.stdout.strip()
print(f"URL: {url[:100]}...")

# Android VR User-Agent
ua = 'com.google.android.youtube/19.30.36 (Linux; U; Android 13; en_US; Pixel 6 Build/TQ3A.230805.001)'
req = urllib.request.Request(url, headers={'User-Agent': ua, 'Accept': '*/*'})
try:
    resp = urllib.request.urlopen(req)
    print(f"Status: {resp.status}, Content-Type: {resp.getheader('Content-Type')}, Length: {resp.getheader('Content-Length')}")
except Exception as e:
    print(f"Error with Android UA: {e}")

# Desktop User-Agent
req2 = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'Accept': '*/*'})
try:
    resp2 = urllib.request.urlopen(req2)
    print(f"Status: {resp2.status}")
except Exception as e:
    print(f"Error with Desktop UA: {e}")

