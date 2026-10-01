"""Verify the installed release on the connected family tablet without resetting user data.
PYTHONPATH=.tools/python-chess python3 scripts/check_release_device.py
Uses an already completed review puzzle for completion testing, preserving progress.
Fails if no completed visible review puzzle is available; never invents a passing result.
"""
import json
import re
import time
import chess
from device_check import ROOT, adb, tree, tap, shot

NAMES = {chess.KING: '王', chess.QUEEN: '后', chess.ROOK: '车', chess.BISHOP: '象', chess.KNIGHT: '马', chess.PAWN: '兵'}
VERSION = re.search(r'versionName = "([^"]+)"', (ROOT / 'app/build.gradle.kts').read_text()).group(1)


def labels():
    return {n.attrib.get('text', '') for n in tree().iter('node')}


def home():
    for _ in range(4):
        if any(x.startswith('去闯关 ·') for x in labels()):
            return
        tap('‹ 返回')
    raise AssertionError('Home not reached')


def square_label(board, square):
    piece = board.piece_at(square)
    return chess.square_name(square) + (' 空格' if piece is None else f' {"白方" if piece.color else "黑方"}{NAMES[piece.piece_type]}')


def play(board, uci):
    move = chess.Move.from_uci(uci)
    tap(square_label(board, move.from_square))
    tap(square_label(board, move.to_square))
    if move.promotion:
        tap(NAMES[move.promotion])


def verify_reveal(uci):
    texts = labels()
    assert any('试试' in x and uci[:2].upper() in x and uci[2:4].upper() in x for x in texts), (uci, texts)


adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
adb('shell', 'wm', 'dismiss-keyguard')
adb('shell', 'am', 'force-stop', 'cn.parentchess')
adb('shell', 'am', 'start', '-W', '-n', 'cn.parentchess/.MainActivity')
home()
entry = next(x for x in labels() if x.startswith('去闯关 ·'))
initial_progress = entry
print('PASS: cold startup and saved progress loaded', flush=True)
tap(entry)
assert '观察热身' in labels() and '原题复习' in labels()
tap('发现机会')
lessons = json.loads((ROOT / 'app/src/main/assets/progression-lessons.json').read_text())
lesson = next(l for l in lessons if l['chapter'] == '02 · 发现机会')
tap(lesson['title'])
board = chess.Board(lesson['fen'])
# Legal wrong move must roll back without marking completion.
accepted = {line[0] for line in lesson['lines']}
wrong = next(m for m in board.legal_moves if m.uci() not in accepted and m.promotion is None)
play(board, wrong.uci())
time.sleep(1.5)
assert '★  已完成！下一关' not in labels()
assert square_label(board, wrong.from_square) in {n.attrib.get('content-desc') for n in tree().iter('node')}
tap('再试一次')
tap('给我提示')
assert not any(x.startswith('试试') for x in labels())
tap('给我提示')
line = lesson['lines'][0]
verify_reveal(line[0])
# Revealing selects the moving piece, so tap only its destination.
first = chess.Move.from_uci(line[0])
tap(square_label(board, first.to_square))
board.push(first)
board.push_uci(line[1])
time.sleep(1.5)
assert '★  已完成！下一关' not in labels()
tap('给我提示')
assert not any(x.startswith('试试') for x in labels())
tap('给我提示')
verify_reveal(line[2])
shot('release-current-position-hint.png')
print('PASS: legal wrong move rolled back; hints follow the opponent response', flush=True)
tap('‹ 返回')
# Replay a completed old puzzle to test completion without incrementing user progress.
tap('原题复习')
review = tree()
completed_title = None
for node in review.iter('node'):
    if node.attrib.get('clickable') != 'true':
        continue
    texts = {n.attrib.get('text', '') for n in node.iter('node')}
    if '★' in texts:
        completed_title = next((x for x in texts if ' · ' in x and not x.startswith('原题复习')), None)
        if completed_title:
            break
assert completed_title, 'No completed visible review puzzle; completion test requires one'
old = json.loads((ROOT / 'app/src/main/assets/lessons.json').read_text())
lesson = next(l for l in old if l['title'] == completed_title)
tap(completed_title)
board = chess.Board(lesson['fen'])
line = lesson['lines'][0]
for i in range(0, len(line), 2):
    play(board, line[i])
    board.push_uci(line[i])
    if i + 1 < len(line):
        board.push_uci(line[i+1])
    time.sleep(1.5)
assert '★  已完成！下一关' in labels()
shot('release-completed-review.png')
print('PASS: completed review puzzle reaches completion', flush=True)
home()
assert initial_progress in labels()
# Restart the process and verify persisted progress without clearing data.
adb('shell', 'am', 'force-stop', 'cn.parentchess')
adb('shell', 'am', 'start', '-W', '-n', 'cn.parentchess/.MainActivity')
home()
assert initial_progress in labels()
tap('关于')
assert f'一起下棋 · {VERSION}' in labels()
assert any('author：pkumza' in x for x in labels())
tap('知道了')
report = dict(startup=True, catalog=True, legalWrongMoveRollback=True,
              currentPositionTwoLevelHints=True, completedReviewReplay=True,
              progressPreserved=True, coldRestart=True, authorVisible=True,
              initialProgress=initial_progress)
(ROOT / 'artifacts' / f'release-device-check-{VERSION}.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
print(json.dumps(report, ensure_ascii=False, indent=2), flush=True)
