import urllib.request
import json
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

try:
    req = urllib.request.Request('https://instances.cobalt.tools/instances.json')
    resp = urllib.request.urlopen(req, context=ctx)
    data = json.loads(resp.read())
    print("Found instances!")
    for inst in data:
        print(inst.get('url'))
except Exception as e:
    print("Error:", e)
