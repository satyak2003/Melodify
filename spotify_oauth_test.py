import requests
import base64

client_id = '3a52ed8ee5544ed8983d75d4bb229f90'
client_secret = '04f5b6f99bd54823a7cf61402e136bb2'

auth_header = base64.b64encode(f"{client_id}:{client_secret}".encode()).decode()
res = requests.post('https://accounts.spotify.com/api/token', 
                    headers={'Authorization': f'Basic {auth_header}'},
                    data={'grant_type': 'client_credentials'})
token = res.json().get('access_token')

res2 = requests.get('https://api.spotify.com/v1/playlists/37i9dQZF1DXcBWIGoYBM5M/tracks', 
                    headers={'Authorization': f'Bearer {token}'})
print("Status:", res2.status_code)
try:
    print(res2.json())
except Exception as e:
    print(res2.text)
