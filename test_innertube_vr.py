import json
import urllib.request

url = "https://www.youtube.com/youtubei/v1/player"

clients = [
    {"clientName": "ANDROID_VR", "clientVersion": "1.33.25"}
]

for client in clients:
    data = {
        "context": {
            "client": client
        },
        "videoId": "LpNVf8sczqU"
    }
    
    req = urllib.request.Request(url, json.dumps(data).encode('utf-8'), headers={
        'Content-Type': 'application/json'
    })
    
    try:
        resp = urllib.request.urlopen(req)
        res_data = json.loads(resp.read().decode('utf-8'))
        
        streamingData = res_data.get('streamingData', {})
        formats = streamingData.get('formats', []) + streamingData.get('adaptiveFormats', [])
        print(f"\n--- {client['clientName']} ---")
        if formats:
            first = formats[-1]
            stream_url = first.get('url')
            if stream_url:
                print(f"URL: {stream_url[:100]}...")
                
                # Test URL
                test_req = urllib.request.Request(stream_url, headers={'User-Agent': 'com.google.android.youtube/19.30.36 (Linux; U; Android 13; en_US; Pixel 6 Build/TQ3A.230805.001)'})
                try:
                    test_resp = urllib.request.urlopen(test_req, timeout=5)
                    print(f"Stream Status: {test_resp.status}")
                except Exception as e:
                    print(f"Stream Error: {e}")
            else:
                print(f"No URL. Signature cipher? {'Yes' if 'signatureCipher' in first else 'No'}")
        else:
            print("No formats found.")
            print(json.dumps(res_data)[:200])
            
    except Exception as e:
        print(f"Error for {client['clientName']}: {e}")
