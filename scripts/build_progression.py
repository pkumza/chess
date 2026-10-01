"""Build a short, interleaved beginner course from unchanged official puzzles.
PYTHONPATH=.tools/python-chess python3 scripts/build_progression.py
"""
import csv
import hashlib
import json
from collections import Counter
from pathlib import Path
import chess
import chess.pgn
from create_lessons import solutions, mates_in_one, describe

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'curriculum/progression'
LABELS = {'mateIn1': '一步将杀', 'mateIn2': '两步将杀', 'hangingPiece': '吃子机会',
          'fork': '双重攻击', 'promotion': '升变', 'defensiveMove': '防守',
          'pin': '牵制', 'skewer': '串击', 'discoveredAttack': '闪击'}
GUIDANCE = {
    'mateIn1': '找一步将军，再检查对手能不能逃、挡或吃。',
    'mateIn2': '先看将军和对手的应对，再找最后一步将杀。',
    'hangingPiece': '观察对手哪些棋子缺少保护。吃子前，也看看会不会被吃回来。',
    'fork': '留意能同时攻击两个目标的机会，这叫双重攻击。',
    'promotion': '观察快到底线的兵，想想升变以后能做什么。',
    'defensiveMove': '先找对手的威胁，再想怎么化解。',
    'pin': '看看同一直线上的棋子。前面的棋子走开，会不会露出后面的目标？',
    'skewer': '看看同一直线上的两个目标。前面的棋子躲开后，后面的还安全吗？',
    'discoveredAttack': '想想移开一个棋子以后，会不会露出另一个棋子的攻击路线。',
}
# Six examples per skill is a coverage ceiling, not a total puzzle quota.
STAGES = [
    ('01 · 观察热身', 400, 850, 10, 1, ['mateIn1']),
    ('02 · 发现机会', 600, 950, 12, 2, ['hangingPiece', 'fork', 'promotion', 'mateIn1']),
    ('03 · 看看对手', 700, 1050, 14, 2, ['defensiveMove', 'hangingPiece', 'fork', 'mateIn1']),
    ('04 · 两子配合', 800, 1150, 16, 2, ['pin', 'skewer', 'discoveredAttack', 'fork', 'mateIn2']),
    ('05 · 综合挑战', 900, 1200, 18, 2, ['defensiveMove', 'promotion', 'fork', 'pin', 'skewer', 'mateIn2']),
]
EXCLUDE = {'sacrifice', 'intermezzo', 'zugzwang', 'underPromotion', 'enPassant', 'castling', 'attraction'}


def main():
    rows = list(csv.DictReader((OUT / 'candidates.csv').open()))
    old = {r['PuzzleId'] for r in csv.DictReader((ROOT / 'curriculum/lichess-puzzles.csv').open())}
    used, layouts, games_used = set(old), set(), set()
    lessons, originals, audit, games = [], [], [], []
    last_keys = []
    for chapter, low, high, pieces_max, steps, themes in STAGES:
        pools = {}
        for theme in themes:
            pool = []
            for row in rows:
                tags = set(row['Themes'].split())
                if theme not in tags or tags & EXCLUDE or row['PuzzleId'] in used:
                    continue
                if theme not in ('mateIn1', 'mateIn2') and 'mate' in tags:
                    continue
                if not low <= int(row['Rating']) <= high or len(row['Moves'].split()) > steps*2:
                    continue
                n = sum(c.isalpha() for c in row['FEN'].split()[0])
                if n > pieces_max:
                    continue
                pool.append(row)
            # Favor a clear board and easy rating; plenty of plays breaks ties.
            pools[theme] = sorted(pool, key=lambda r: (int(r['Rating']) + 18*sum(c.isalpha() for c in r['FEN'].split()[0]), -int(r['NbPlays']), r['PuzzleId']))
        covered = Counter()
        stage = []
        # Warm-up varies mating piece rather than repeating a rook for a whole chapter.
        targets = [('mateIn1', kind) for kind in ('q', 'r', 'n', 'b', 'p')] if steps == 1 else [(t, None) for t in themes]
        for cycle in range(6):
            for theme, required_kind in targets:
                candidates = []
                for row in pools[theme]:
                    if row['PuzzleId'] in used:
                        continue
                    board = chess.Board(row['FEN'])
                    raw = row['Moves'].split()
                    if not board.is_valid():
                        continue
                    move = chess.Move.from_uci(raw[0])
                    if move not in board.legal_moves:
                        continue
                    board.push(move)
                    key = ' '.join(board.fen().split()[:4])
                    source_game = row['GameUrl'].split('#')[0].replace('/black', '')
                    if key in layouts or source_game in games_used or board.is_game_over():
                        continue
                    first = chess.Move.from_uci(raw[1])
                    piece = board.piece_at(first.from_square)
                    if not piece or (required_kind and piece.symbol().lower() != required_kind):
                        continue
                    kind = piece.symbol().lower()
                    if len(last_keys) >= 2 and last_keys[-1][1] == last_keys[-2][1] == kind:
                        continue
                    if len(last_keys) >= 2 and last_keys[-1][0] == last_keys[-2][0] == theme and steps != 1:
                        continue
                    # Prefer alternating sides and new piece/skill, without using those as hard rules.
                    penalty = 0
                    if last_keys and last_keys[-1][2] == board.turn:
                        penalty += 100
                    if last_keys and last_keys[-1][1] == kind:
                        penalty += 50
                    penalty += covered[(theme, kind)]*80
                    candidates.append((penalty, row, board, key, source_game, kind))
                    if len(candidates) >= 80:
                        break
                if not candidates:
                    continue
                _, row, board, key, source_game, kind = min(candidates, key=lambda x: x[0])
                raw = row['Moves'].split()
                replay = board.copy()
                san = []
                for uci in raw[1:]:
                    move = chess.Move.from_uci(uci)
                    assert move in replay.legal_moves, row['PuzzleId']
                    san.append(replay.san(move))
                    replay.push(move)
                    assert replay.is_valid()
                mate_in = 1 if theme == 'mateIn1' else 2 if theme == 'mateIn2' else 0
                if mate_in:
                    assert replay.is_checkmate()
                    accepted = solutions(board, mate_in, raw[1:])
                else:
                    assert not replay.is_stalemate()
                    accepted = [raw[1:]]
                side = '白' if board.turn else '黑'
                goal = f'{side}方{LABELS[theme]}' if mate_in else f'{side}方走 {len(raw[1:])//2+1} 步，完成战术'
                source = dict(name='Lichess 公开题库', puzzleId=row['PuzzleId'], url=f'https://lichess.org/training/{row["PuzzleId"]}',
                              gameUrl=row['GameUrl'], datasetUrl='https://database.lichess.org/#puzzles', license='CC0-1.0',
                              retrievedAt='2026-10-01', originalFen=row['FEN'], originalMoves=raw,
                              rating=int(row['Rating']), popularity=int(row['Popularity']), plays=int(row['NbPlays']), themes=row['Themes'].split())
                number = len(lessons)+1
                # Titles do not reveal the motif before a review puzzle.
                lesson = dict(id='lichess-'+row['PuzzleId'], number=number, chapter=chapter,
                              title=f'小挑战 {number}', intro=f'你用{side}棋。先看看双方的王和受到攻击的棋子。',
                              fen=board.fen(), goal=goal, mateIn=mate_in, completion='checkmate' if mate_in else 'sourceLine',
                              lines=accepted, hints=[GUIDANCE[theme], f'试试{describe(board, raw[1])}。'],
                              explanation=('王被将军，而且没有合法应对，这才是将杀。' if mate_in else '你完成了原题的战术解法。可以再走一遍，观察每一步发生了什么。'),
                              question=None, answer=None, source=source)
                lessons.append(lesson)
                originals.append(row)
                stage.append(lesson)
                used.add(row['PuzzleId']); layouts.add(key); games_used.add(source_game)
                covered[(theme, kind)] += 1
                last_keys.append((theme, kind, board.turn))
                audit.append(dict(number=number, puzzleId=row['PuzzleId'], theme=theme, pieces=len(board.piece_map()),
                                  rating=int(row['Rating']), playerMoves=len(raw)//2, side=side,
                                  originalLineLegal=True, mateAllDefencesVerified=bool(mate_in), san=san))
                g = chess.pgn.Game(); g.setup(board)
                g.headers.update(Event=chapter, Site=source['url'], Result='*', PuzzleId=row['PuzzleId'],
                                 PuzzleRating=row['Rating'], Themes=row['Themes'], License='CC0-1.0')
                node = g
                for uci in raw[1:]: node = node.add_variation(chess.Move.from_uci(uci))
                games.append(str(g))
        assert len(stage) >= 12, (chapter, len(stage))
        print(chapter, len(stage), Counter(x['theme'] for x in audit if x['number'] in {l['number'] for l in stage}), flush=True)
    asset = ROOT / 'app/src/main/assets/progression-lessons.json'
    asset.write_text(json.dumps(lessons, ensure_ascii=False, indent=2)+'\n')
    (OUT / 'selected.pgn').write_text('\n\n'.join(games)+'\n')
    with (OUT / 'source.csv').open('w') as file:
        writer = csv.DictWriter(file, fieldnames=list(originals[0])); writer.writeheader(); writer.writerows(originals)
    report = dict(lessons=len(lessons), chapters=dict(Counter(l['chapter'] for l in lessons)),
                  learner=dict(ageYears=5.5, learningMonths=6, currentTopic='王车易位', mastery='未评估'),
                  source='https://database.lichess.org/#puzzles',
                  inventorySha256=hashlib.sha256((OUT / 'database-inventory.json').read_bytes()).hexdigest(),
                  validation='All original positions and official solution moves legal. Mate lessons enumerate all legal defences and alternate mating solutions. Non-mating lessons use the official line, without independently re-proving optimality.',
                  puzzles=audit)
    (OUT / 'validation.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print('Selected',len(lessons),flush=True)


if __name__ == '__main__':
    main()
