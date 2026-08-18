import json
import urllib.request
import urllib.error
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

instances = [
    'https://pipedapi.kavin.rocks',
    'https://pipedapi.lunar.icu',
    'https://api.piped.privacydev.net',
    'https://pipedapi.r4fo.com',
    'https://piped-api.garudalinux.org',
    'https://pipedapi.adminforge.de'
]

video_id = "LpNVf8sczqU"

for inst in instances:
    url = f'{inst}/streams/{video_id}'
    req = urllib.request.Request(url, headers={
        'Accept': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    })
    try:
        response = urllib.request.urlopen(req, context=ctx, timeout=5)
        res_data = json.loads(response.read().decode('utf-8'))
        audio_streams = res_data.get('audioStreams', [])
        if audio_streams:
            print(f"{inst} - Success! Best audio URL: {audio_streams[0].get('url')[:100]}...")
            # test stream directly
            test_req = urllib.request.Request(audio_streams[0].get('url'), headers={'User-Agent': 'Mozilla/5.0'})
            try:
                test_resp = urllib.request.urlopen(test_req, context=ctx, timeout=5)
                print(f"  -> Stream accessible! Status: {test_resp.status}")
                break
            except Exception as e:
                print(f"  -> Stream not accessible: {e}")
        else:
            print(f"{inst} - No audio streams found.")
    except Exception as e:
        print(f"{inst} - Error: {e}")
