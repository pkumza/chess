import time
from device_check import tap, tree, shot, adb

def has(text):
    return any(n.attrib.get('text') == text or n.attrib.get('content-desc') == text for n in tree().iter('node'))
def require(text):
    assert has(text), text

tap('重新开始'); tap('确定')
tap('e2 白方兵'); tap('e4 空格'); require('轮到黑方走棋')
tap('e7 黑方兵'); tap('e5 空格'); require('轮到白方走棋')
require('棋谱 · 2 步')
tap('悔一步'); require('棋谱 · 1 步'); require('e7 黑方兵')
tap('e7 黑方兵'); tap('e5 空格')
shot('pvp-played.png')
# Leave and relaunch the process; no uninstall or data clearing.
adb('shell','input','keyevent','KEYCODE_HOME')
time.sleep(0.6)
adb('shell','am','force-stop','cn.parentchess')
adb('shell','am','start','-n','cn.parentchess/.MainActivity')
time.sleep(1)
require('棋谱 · 2 步'); require('e4 白方兵'); require('e5 黑方兵')
print('PASS: two-player moves, undo, persisted game after process restart', flush=True)
tap('‹ 返回'); tap('开始机器人陪练'); tap('开始新棋局')
tap('e2 白方兵'); tap('e4 空格'); time.sleep(1.5)
require('轮到白方走棋'); require('棋谱 · 2 步')
shot('bot-played.png')
tap('悔一步'); require('棋谱 · 0 步'); require('e2 白方兵')
print('PASS: bot responds legally, paired undo returns to human turn', flush=True)
tap('‹ 返回'); tap('去闯关 · 0 / 24'); tap('车走直线')
tap('a2 白方车'); tap('a3 空格')
require('a2 白方车')
tap('给我提示'); tap('a2 白方车'); tap('h2 黑方兵')
require('★  已完成！下一关')
shot('lesson-completed.png')
tap('‹ 返回'); require('24 个小挑战 · 已完成 1 关 · 随时可以复习')
print('PASS: wrong answer preserves board; hint and correct answer complete lesson', flush=True)
adb('shell','input','keyevent','KEYCODE_HOME'); time.sleep(0.5)
adb('shell','am','force-stop','cn.parentchess')
adb('shell','am','start','-n','cn.parentchess/.MainActivity'); time.sleep(1)
require('24 个小挑战 · 已完成 1 关 · 随时可以复习')
print('PASS: learning progress survives process restart', flush=True)
tap('‹ 返回')
