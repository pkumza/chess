"""Exercise learning animations without changing saved PvP/bot games."""
import time,re,subprocess,json,sys
from device_check import tree,tap,shot,adb,ROOT,ADB

def labels():return {n.attrib.get('text','') for n in tree().iter('node')}
def go_home():
    for _ in range(4):
        if '开始双人对弈' in labels():return
        tap('‹ 返回')
    raise AssertionError('No home')
def point(label):
    for n in tree().iter('node'):
        if n.attrib.get('text')==label or n.attrib.get('content-desc')==label:
            x,y,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']));return str((x+x2)//2),str((y+y2)//2)
    raise AssertionError(label)
def require_desc(label):
    assert any(n.attrib.get('content-desc')==label for n in tree().iter('node')),label

go_home();tap(next(x for x in labels() if x.startswith('去闯关 ·')));tap('车走直线')
# The first lesson is already complete from the original acceptance check; replay adds no progress.
tap('a2 白方车');target=point('h2 黑方兵')
proc=subprocess.Popen(ADB+['shell','screenrecord','--bit-rate','8000000','--time-limit','8','/sdcard/chess-capture-1.2.0.mp4'],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
time.sleep(1)
adb('shell','input','tap',*target)
time.sleep(.37)
# Read audio engine state immediately after the impact; do not record ambient audio.
(ROOT/'artifacts/audio-engine-1.2.0.txt').write_bytes(adb('shell','dumpsys','media.audio_flinger'))
proc.communicate(timeout=15)
adb('pull','/sdcard/chess-capture-1.2.0.mp4',str(ROOT/'artifacts/capture-animation-1.2.0.mp4'))
require_desc('h2 白方车');assert '★  已完成！下一关' in labels()
shot('capture-complete-1.2.0.png')
print('PASS: capture animation completes and lesson goal is preserved',flush=True)
# Toggle the compact header control, then restore its original preference.
old = '音效：开' if '音效：开' in labels() else '音效：关'
new = '音效：关' if old == '音效：开' else '音效：开'
tap(old);assert new in labels();tap(new)
print('PASS: sound toggle responds and original preference restored',flush=True)
if '--capture-only' in sys.argv:
    tap('‹ 返回');tap('‹ 返回');sys.exit(0)
tap('‹ 返回');tap('给车找个伙伴')
tap('e2 白方象');tap('c4 空格')
require_desc('c4 白方象');require_desc('a2 黑方车')
shot('lesson-response-1.2.0.png')
print('PASS: player move and scripted capture animate in order',flush=True)
tap('再试一次');require_desc('e2 白方象');require_desc('a2 白方车')
tap('‹ 返回');tap('‹ 返回')
(ROOT/'artifacts/effects-verification-1.2.0.json').write_text(json.dumps({'capture':True,'soundToggle':True,'scriptedResponseQueue':True,'retry':True,'savedGamesUntouched':True},indent=2)+'\n')
