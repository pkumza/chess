import hashlib, json, pathlib, subprocess, re
from device_check import adb, shot, ROOT
apk = ROOT/'artifacts/parent-chess-1.2.0.apk'
package_path = adb('shell','pm','path','cn.parentchess').decode().strip().removeprefix('package:')
assert package_path.startswith('/data/app/') and package_path.endswith('/base.apk')
installed_hash = adb('shell','sha256sum',package_path).decode().split()[0]
local_hash = hashlib.sha256(apk.read_bytes()).hexdigest()
assert installed_hash == local_hash, 'Installed APK differs from deliverable'
pid = adb('shell','pidof','cn.parentchess').decode().strip()
assert pid, 'Application not running'
audio_path = ROOT/'artifacts/audio-engine-1.2.0.txt'
audio_tracks = [line.strip() for line in audio_path.read_text().splitlines() if re.search(r'\b'+re.escape(pid)+r'/\s*\d+',line)] if audio_path.exists() else []
report = {
    'applicationId': 'cn.parentchess', 'name': '一起下棋', 'version': '1.2.0',
    'deviceModel': adb('shell','getprop','ro.product.model').decode().strip(),
    'androidVersion': adb('shell','getprop','ro.build.version.release').decode().strip(),
    'apkSha256': local_hash, 'installedApkMatchesArtifact': True,
    'applicationRunning': True,
    'audioPlaybackObserved': any('yes' in line for line in audio_tracks),
    'ownAudioTracks': audio_tracks,
    'rotationSettings': {
        'accelerometer_rotation': adb('shell','settings','get','system','accelerometer_rotation').decode().strip(),
        'user_rotation': adb('shell','settings','get','system','user_rotation').decode().strip()
    }
}
(ROOT/'artifacts/installation-verification-1.2.0.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
shot('layout-final-1.2.0.png')
print(json.dumps(report, ensure_ascii=False, indent=2))
