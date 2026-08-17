import requests
import json
import re

url = 'https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M'
r = requests.get(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
html = r.text

match = re.search(r'\"accessToken\":\"(.*?)\"', html)
if match:
    token = match.group(1)
    print('Found token:', token[:20], '...')
    
    api_url = 'https://api.spotify.com/v1/playlists/37i9dQZF1DXcBWIGoYBM5M/tracks?offset=50&limit=50'
    res = requests.get(api_url, headers={'Authorization': f'Bearer {token}'})
    print('API Status:', res.status_code)
    if res.status_code == 200:
        data = res.json()
        print('Fetched items:', len(data.get('items', [])))
        print('Total items:', data.get('total'))
else:
    print('No token found')
