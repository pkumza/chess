import re, time
from device_check import tap, tree, shot, adb

def require(label):
    assert any(n.attrib.get('text') == label or n.attrib.get('content-desc') == label for n in tree().iter('node')), label

def points():
    result={}
    for n in tree().iter('node'):
        a=n.attrib; label=a.get('text') or a.get('content-desc')
        if label:
            x1,y1,x2,y2=map(int,re.findall(r'\d+',a['bounds']))
            result[label] = (str((x1+x2)//2),str((y1+y2)//2))
    return result

def fast(p, label):
    adb('shell','input','tap',*p[label])
    time.sleep(0.08)

labels = {n.attrib.get('text') for n in tree().iter('node')}
if '‹ 返回' in labels: tap('‹ 返回')
labels = {n.attrib.get('text') for n in tree().iter('node')}
if '继续机器人陪练' in labels:
    tap('继续机器人陪练'); tap('重新开始'); tap('确定')
else:
    tap('开始机器人陪练'); tap('开始新棋局')
p=points()
fast(p,'e2 白方兵'); fast(p,'e4 空格'); fast(p,'‹ 返回')
time.sleep(1)
require('今天，下一盘吧。')
tap('继续机器人陪练'); time.sleep(1.5)
require('棋谱 · 2 步'); require('轮到白方走棋')
print('PASS: leave during bot turn; resume executes exactly one reply', flush=True)
tap('悔一步'); require('棋谱 · 0 步')
tap('重新开始'); tap('确定')
p=points()
fast(p,'e2 白方兵'); fast(p,'e4 空格'); fast(p,'悔一步')
time.sleep(1)
require('棋谱 · 0 步'); require('e2 白方兵')
print('PASS: undo during bot turn rejects stale result', flush=True)
# Reset test feedback before caching stable button coordinates.
tap('重新开始'); tap('确定')
# Cache confirmation coordinates, then reset immediately after a human move.
tap('重新开始'); dialog=points(); tap('继续下')
p=points()
fast(p,'e2 白方兵'); fast(p,'e4 空格'); fast(p,'重新开始'); fast(dialog,'确定')
time.sleep(1)
require('棋谱 · 0 步'); require('e2 白方兵')
print('PASS: new game during bot turn rejects stale result', flush=True)
tap('更换对手'); tap('练习'); tap('我用黑方'); tap('开始新棋局'); time.sleep(1.5)
require('棋谱 · 1 步'); require('轮到黑方走棋')
tap('e7 黑方兵'); tap('e5 空格'); time.sleep(1.5)
require('棋谱 · 3 步'); require('轮到黑方走棋'); shot('bot-human-black.png')
tap('悔一步'); require('棋谱 · 1 步'); require('e7 黑方兵')
print('PASS: practice difficulty, human black, bot opening, paired undo', flush=True)
tap('更换对手'); tap('初学'); tap('我用白方'); tap('开始新棋局')
tap('‹ 返回')
