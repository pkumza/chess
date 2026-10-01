"""Check actual tablet geometry without changing moves or learning progress."""
import time,re,json
from device_check import tree,tap,shot,adb,ROOT

def texts(): return {n.attrib.get('text','') for n in tree().iter('node')}
def home():
    for _ in range(3):
        if '开始双人对弈' in texts(): return
        tap('‹ 返回')
    raise AssertionError('Cannot return home')
def check(name,landscape):
    root=tree()
    nodes=list(root.iter('node'))
    squares=[n for n in nodes if re.match(r'^[a-h][1-8] ',n.attrib.get('content-desc',''))]
    assert len(squares)==64, len(squares)
    boxes=[list(map(int,re.findall(r'\d+',n.attrib['bounds']))) for n in squares]
    bounds=[min(b[0] for b in boxes),min(b[1] for b in boxes),max(b[2] for b in boxes),max(b[3] for b in boxes)]
    w,h=bounds[2]-bounds[0],bounds[3]-bounds[1]
    assert abs(w-h)<=1, bounds
    assert w>=2110, ('Board should fill 2136-pixel short edge, minus border',bounds)
    labels=[n.attrib.get('text','') for n in nodes]
    assert '一起下棋' not in labels
    back=next(n for n in nodes if n.attrib.get('text')=='‹ 返回')
    bb=list(map(int,re.findall(r'\d+',back.attrib['bounds'])))
    assert (bb[0]>=bounds[2] if landscape else bb[1]>=bounds[3]), (bounds,bb)
    shot(name+'.png')
    print(name, bounds, flush=True)
    return {'screen':name,'boardBounds':bounds,'boardSidePixels':w,'returnButtonBounds':bb}

original_auto=adb('shell','settings','get','system','accelerometer_rotation').decode().strip()
original_rotation=adb('shell','settings','get','system','user_rotation').decode().strip()
results=[]
try:
    home()
    adb('shell','settings','put','system','accelerometer_rotation','0')
    adb('shell','settings','put','system','user_rotation','1')
    tap('开始双人对弈');time.sleep(1)
    results.append(check('max-board-landscape',True))
    tap('翻转棋盘');tap('翻转棋盘')
    adb('shell','settings','put','system','user_rotation','0');time.sleep(1)
    results.append(check('max-board-portrait',False))
    home()
    entry=next(t for t in texts() if t.startswith('去闯关 ·'))
    tap(entry);tap('车走直线')
    adb('shell','settings','put','system','user_rotation','1');time.sleep(1)
    results.append(check('max-board-lesson',True))
finally:
    adb('shell','settings','put','system','user_rotation',original_rotation)
    adb('shell','settings','put','system','accelerometer_rotation',original_auto)
(ROOT/'artifacts/layout-verification-1.1.0.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
print('PASS: maximum square, no duplicate header, side/bottom navigation, lesson layout; original orientation settings restored.',flush=True)
