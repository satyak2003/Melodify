import requests
from bs4 import BeautifulSoup
import re

url = 'https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M'
r = requests.get(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
html = r.text

soup = BeautifulSoup(html, 'html.parser')
for script in soup.find_all('script'):
    content = script.string
    if content and 'accessToken' in content:
        print("Found accessToken in script!")
        match = re.search(r'\"accessToken\":\"(.*?)\"', content)
        if match:
            print("Token:", match.group(1))

# Search for any token-like string
match = re.search(r'\"accessToken\":\"([^\"]+)\"', html)
if match:
    print("Regex found token:", match.group(1)[:20])
else:
    print("No accessToken string anywhere in HTML")
