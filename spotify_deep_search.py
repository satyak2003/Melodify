import requests
import json
import re

url = 'https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M'
r = requests.get(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'})
html = r.text

print("Downloaded HTML size:", len(html))
match = re.search(r'\"accessToken\":\"([^\"]+)\"', html)
if match:
    print("Found token:", match.group(1)[:30])
else:
    # try searching for 'token'
    idx = html.find('accessToken')
    if idx != -1:
        print("Found word accessToken at", idx)
        print(html[idx-20:idx+100])
    else:
        print("No accessToken word found.")
        
    idx = html.find('session')
    if idx != -1:
        print("Found word session at", idx)
        print(html[idx-20:idx+100])

