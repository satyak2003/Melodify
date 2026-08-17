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
        if trackList:
            print(json.dumps(trackList[0], indent=2))
    except Exception as e:
        print('Error:', e)
