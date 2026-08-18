import subprocess
import os

print("Downloading to file...")
res = subprocess.run(['yt-dlp', '-f', 'bestaudio[ext=m4a]', '-o', 'temp_audio.m4a', 'https://www.youtube.com/watch?v=LpNVf8sczqU'], capture_output=True, text=True)
print(f"Exit code: {res.returncode}")
print(f"File exists: {os.path.exists('temp_audio.m4a')}")
if os.path.exists('temp_audio.m4a'):
    print(f"File size: {os.path.getsize('temp_audio.m4a')}")
    os.remove('temp_audio.m4a')
if res.stderr:
    print(f"STDERR: {res.stderr}")
