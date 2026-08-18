import urllib.request
import subprocess

ua = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
print("Getting URL...")
res = subprocess.run(['yt-dlp', '--js-runtimes', './deno.exe', '--user-agent', ua, '-f', 'bestaudio[ext=m4a]', '--get-url', 'https://www.youtube.com/watch?v=LpNVf8sczqU'], capture_output=True, text=True)
url = res.stdout.strip()
print(f"URL: {url[:100]}...")
print(f"STDERR: {res.stderr}")

req = urllib.request.Request(url, headers={'User-Agent': ua, 'Accept': '*/*'})
try:
    resp = urllib.request.urlopen(req, timeout=5)
    print(f"Status: {resp.status}")
except Exception as e:
    print(f"Error: {e}")
