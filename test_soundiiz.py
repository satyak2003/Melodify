import requests
import json

url = 'https://www.tunemymusic.com/api/v2/Playlist/Load'
# Usually requires some parameters. Let's try finding the endpoints.
# Actually, TuneMyMusic is heavily protected by Cloudflare. 

print("Testing Soundiiz public API instead...")
url2 = 'https://soundiiz.com/webapp/playlist/spotify/37i9dQZF1DXcBWIGoYBM5M'
r = requests.get(url2, headers={'User-Agent': 'Mozilla/5.0'})
print("Soundiiz status:", r.status_code)
if r.status_code == 200:
    print("Soundiiz length:", len(r.text))
