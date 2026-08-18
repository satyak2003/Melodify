import subprocess
import json
import urllib.request

res = subprocess.run(['yt-dlp', '-J', 'https://www.youtube.com/watch?v=LpNVf8sczqU'], capture_output=True, text=True)
data = json.loads(res.stdout)

# find best audio
audio_formats = [f for f in data.get('formats', []) if f.get('acodec') != 'none' and f.get('vcodec') == 'none']
best_audio = audio_formats[-1] if audio_formats else None

if best_audio:
    url = best_audio.get('url')
    headers = best_audio.get('http_headers', {})
    print("URL:", url[:100])
    print("HEADERS:", headers)
    
    # Try to request it using Python
    req = urllib.request.Request(url, headers=headers)
    try:
        resp = urllib.request.urlopen(req, timeout=5)
        print("Status:", resp.status)
    except Exception as e:
        print("Error:", e)
