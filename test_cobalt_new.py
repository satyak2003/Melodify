import json
import urllib.request
import urllib.error

def test_cobalt(video_id):
    url = 'https://co.wuk.sh/api/json'
    data = {
        "url": f"https://www.youtube.com/watch?v={video_id}",
        "isAudioOnly": True,
        "aFormat": "best"
    }
    
    req = urllib.request.Request(url, json.dumps(data).encode('utf-8'), headers={
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
    })
    try:
        response = urllib.request.urlopen(req)
        res_data = json.loads(response.read().decode('utf-8'))
        print(f"Cobalt success: {res_data.get('url')[:100]}...")
        # test stream directly
        test_req = urllib.request.Request(res_data.get('url'), headers={'User-Agent': 'Mozilla/5.0'})
        test_resp = urllib.request.urlopen(test_req, timeout=5)
        print(f"Stream accessible! Status: {test_resp.status}")
    except Exception as e:
        print(f"Cobalt Error: {e}")

test_cobalt("LpNVf8sczqU")
