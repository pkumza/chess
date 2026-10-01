"""Check the sourced curriculum on the connected family device without completing lessons."""
import json
import time
from device_check import ROOT, tree, tap, shot, adb


def labels():
    return {n.attrib.get('text', '') for n in tree().iter('node')}


def require_piece(label):
    for _ in range(5):
        if any(n.attrib.get('content-desc') == label for n in tree().iter('node')):
            return
        time.sleep(.5)
    raise AssertionError(label)


# Navigate only within the foreground chess application.
for _ in range(4):
    if any(x.startswith('去闯关 ·') for x in labels()):
        break
    tap('‹ 返回')
else:
    raise AssertionError('Could not reach the home screen')
home = labels()
entry = next(x for x in home if x.startswith('去闯关 ·'))
assert entry.endswith('/ 100'), entry
initial_progress = entry
shot('curriculum-home-1.3.0.png')
tap(entry)
time.sleep(.6)
assert any('100 个残局挑战' in x for x in labels())
tap('车的将杀 · 01')
time.sleep(.6)
require_piece('a6 白方车')
require_piece('g1 黑方王')
require_piece('h1 黑方后')
tap('题目出处')
time.sleep(.6)
assert any('CtI2H' in x for x in labels())
assert '查看原题（联网）' in labels()
assert '查看原始对局（联网）' in labels()
shot('curriculum-source-1.3.0.png')
tap('回到棋盘')
# A legal non-solution must animate back, preserve the puzzle, and offer a hint.
tap('a6 白方车')
tap('a5 空格')
time.sleep(2.2)
require_piece('a6 白方车')
require_piece('a5 空格')
assert '★  已完成！下一关' not in labels()
shot('curriculum-retry-1.3.0.png')
tap('‹ 返回')
tap('‹ 返回')
assert initial_progress in labels()
(ROOT / 'artifacts/curriculum-device-check-1.3.0.json').write_text(json.dumps({
    'homeCount': 100, 'sourcePuzzle': 'CtI2H', 'sourceDialog': True,
    'wrongMoveReturns': True, 'progressUnchanged': True,
}, indent=2)+'\n')
print('PASS: 100 lessons, source links, original first position, wrong-move rollback, unchanged progress')
