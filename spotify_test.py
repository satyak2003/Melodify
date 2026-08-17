import requests
import json
import re

url = 'https://open.spotify.com/embed/playlist/37i9dQZF1DXcBWIGoYBM5M'
r = requests.get(url)
html = r.text

start_marker = '<script id="__NEXT_DATA__" type="application/json">'
start_idx = html.find(start_marker)

if start_idx != -1:
    json_start = html.find('>', start_idx) + 1
    json_end = html.find('</script>', json_start)
    try:
        data = json.loads(html[json_start:json_end])
        entity = data.get('props', {}).get('pageProps', {}).get('state', {}).get('data', {}).get('entity', {})
        trackList = entity.get('trackList', [])
        print('__NEXT_DATA__ trackList length:', len(trackList))
        if trackList:
            print('First track:', json.dumps(trackList[0])[:200])
    except Exception as e:
        print('Error parsing __NEXT_DATA__:', e)
else:
    print('__NEXT_DATA__ not found in Spotify embed HTML')
    
# Alternative: looking for initial-state or similar
match = re.search(r'<script id="initial-state" type="text/plain">(.*?)</script>', html)
if match:
    import urllib.parse
    import base64
    raw_b64 = match.group(1)
    # decode base64
    decoded = base64.b64decode(raw_b64).decode('utf-8')
    try:
        data = json.loads(decoded)
        print("Found initial-state data keys:", list(data.keys()))
    except Exception as e:
        print("Failed to decode initial-state", e)
