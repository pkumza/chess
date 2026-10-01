import time
from device_check import tap, tree, shot, adb

def require(label):
    assert any(n.attrib.get('text') == label or n.attrib.get('content-desc') == label for n in tree().iter('node')), label

original_auto = adb('shell','settings','get','system','accelerometer_rotation').decode().strip()
original_rotation = adb('shell','settings','get','system','user_rotation').decode().strip()
try:
    adb('shell','settings','put','system','accelerometer_rotation','0')
    adb('shell','settings','put','system','user_rotation','0')
    time.sleep(1.5)
    shot('home-portrait.png')
    tap('开始双人对弈'); require('棋谱 · 2 步')
    require('e4 白方兵'); require('e5 黑方兵')
    shot('pvp-portrait.png')
    adb('shell','settings','put','system','user_rotation','1'); time.sleep(1.5)
    require('棋谱 · 2 步'); shot('pvp-landscape-final.png')
    tap('翻转棋盘'); require('e4 白方兵'); shot('pvp-flipped.png'); tap('翻转棋盘')
    tap('‹ 返回')
    print('PASS: portrait, landscape, board flip and position preservation', flush=True)
finally:
    adb('shell','settings','put','system','user_rotation',original_rotation)
    adb('shell','settings','put','system','accelerometer_rotation',original_auto)
    print('Restored original device rotation settings', flush=True)
