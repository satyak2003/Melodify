import subprocess
import os

clients = ['android', 'ios', 'tv', 'web']
for client in clients:
    print(f"Testing client: {client}")
    res = subprocess.run(['yt-dlp', '-f', 'bestaudio[ext=m4a]', '--extractor-args', f'youtube:player_client={client}', '-o', f'temp_{client}.m4a', 'https://www.youtube.com/watch?v=LpNVf8sczqU'], capture_output=True, text=True)
    if os.path.exists(f'temp_{client}.m4a'):
        print(f"SUCCESS with {client}!")
        os.remove(f'temp_{client}.m4a')
    else:
        print(f"FAILED with {client}. Exit: {res.returncode}")
        # print stderr snippet
        err = res.stderr.strip().split('\n')[-2:]
        print(err)
