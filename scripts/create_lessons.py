"""Build sourced lessons offline; never synthesize or simplify a position.
PYTHONPATH=.tools/python-chess python3 scripts/create_lessons.py
Inputs: the original CC0 Lichess CSV rows and the reviewed selection order.
"""
import csv
import hashlib
import json
from collections import Counter
from pathlib import Path
import chess
import chess.pgn

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'curriculum/lichess-puzzles.csv'
DATASET = 'https://database.lichess.org/#puzzles'
NAMES = {chess.KING: '王', chess.QUEEN: '后', chess.ROOK: '车', chess.BISHOP: '象', chess.KNIGHT: '马', chess.PAWN: '兵'}


def mates_in_one(board):
    result = []
    for move in list(board.legal_moves):
        board.push(move)
        if board.is_checkmate():
            result.append(move.uci())
        board.pop()
    return result


def ordered(items, preferred):
    return sorted(items, key=lambda value: (value != preferred, value))


def solutions(board, mate_in, source_line):
    immediate = mates_in_one(board)
    if mate_in == 1:
        assert immediate
        return [[m] for m in ordered(immediate, source_line[0])]
    assert mate_in == 2 and not immediate, 'A two-move lesson must actually need two moves'
    lines = []
    # AND over every legal defence, OR over every mating reply. No cooperative line.
    for first in ordered([m.uci() for m in board.legal_moves], source_line[0]):
        board.push_uci(first)
        replies = list(board.legal_moves)
        branches = []
        if replies and not board.is_game_over():
            for reply in ordered([m.uci() for m in replies], source_line[1]):
                board.push_uci(reply)
                finishes = mates_in_one(board) if not board.is_game_over() else []
                board.pop()
                if not finishes:
                    branches = []
                    break
                branches.extend([[first, reply, last] for last in ordered(finishes, source_line[2])])
        board.pop()
        lines.extend(branches)
    assert source_line in lines, 'Official solution must be a proved forcing mate'
    return lines


def describe(board, uci):
    m = chess.Move.from_uci(uci)
    piece = NAMES[board.piece_type_at(m.from_square)]
    suffix = f'，升变成{NAMES[m.promotion]}' if m.promotion else ''
    return f'{piece}从 {chess.square_name(m.from_square)} 走到 {chess.square_name(m.to_square)}{suffix}'


def build():
    with SOURCE.open() as file:
        sources = {row['PuzzleId']: row for row in csv.DictReader(file)}
    selection = json.loads((ROOT / 'curriculum/selection.json').read_text())
    assert len(selection) == len(sources) == 100
    assert len({x['puzzleId'] for x in selection}) == 100
    lessons, audits = [], []
    chapter_indices = Counter()
    layouts = set()
    for number, item in enumerate(selection, 1):
        row = sources[item['puzzleId']]
        board = chess.Board(row['FEN'])
        assert board.is_valid()
        raw_moves = row['Moves'].split()
        assert chess.Move.from_uci(raw_moves[0]) in board.legal_moves
        board.push_uci(raw_moves[0])  # Official FEN precedes the opponent's setup move.
        assert board.is_valid() and board.turn == chess.WHITE and not board.is_game_over()
        assert 4 <= len(board.piece_map()) <= 7
        assert board.board_fen() not in layouts
        layouts.add(board.board_fen())
        themes = row['Themes'].split()
        assert 'endgame' in themes
        mate_in = 1 if 'mateIn1' in themes else 2
        assert f'mateIn{mate_in}' in themes
        assert int(row['Rating']) <= 1200 and int(row['Popularity']) >= 90 and int(row['NbPlays']) >= 1000
        source_line = raw_moves[1:]
        assert len(source_line) == 2 * mate_in - 1
        replay = board.copy()
        san = []
        for uci in source_line:
            move = chess.Move.from_uci(uci)
            assert move in replay.legal_moves
            san.append(replay.san(move))
            replay.push(move)
        assert replay.is_checkmate(), row['PuzzleId']
        lines = solutions(board, mate_in, source_line)
        for line in lines:
            replay = board.copy()
            for i, uci in enumerate(line):
                assert not replay.is_game_over(), (row['PuzzleId'], line, i)
                replay.push_uci(uci)
            assert replay.is_checkmate()
        first = chess.Move.from_uci(source_line[0])
        kind = NAMES[board.piece_type_at(first.from_square)]
        action = describe(board, source_line[0])
        chapter = item['chapter']
        chapter_indices[chapter] += 1
        motif = '升变将杀' if first.promotion else ('小兵将杀' if kind == '兵' else f'{kind}的将杀')
        if mate_in == 2:
            motif = '再想一步'
        title = f'{motif} · {chapter_indices[chapter]:02d}'
        king = chess.square_name(board.king(chess.BLACK))
        intro = f'你用白棋，黑王在 {king}。' + ('找一步将军，让它无路可走。' if mate_in == 1 else '你走两步，中间黑方走一步。每次都看看黑王能躲到哪里。')
        first_hint = f'先看看白{kind}。' + ('将军后，对手还能逃、挡或吃吗？' if mate_in == 1 else '先缩小黑王的选择，再找最后的将军。')
        explanation = '黑王被将军，而且没有任何合法应对，这才是将杀。'
        if mate_in == 2:
            explanation = '你走到了真正的将杀！第一步之后，黑方换一种合法应对，白方也能在下一步将杀。'
        source = dict(name='Lichess 公开题库', puzzleId=row['PuzzleId'], url=f'https://lichess.org/training/{row["PuzzleId"]}',
                      gameUrl=row['GameUrl'], datasetUrl=DATASET, license='CC0-1.0', retrievedAt='2026-10-01',
                      originalFen=row['FEN'], originalMoves=raw_moves, rating=int(row['Rating']),
                      popularity=int(row['Popularity']), plays=int(row['NbPlays']), themes=themes)
        lessons.append(dict(id='lichess-'+row['PuzzleId'], number=number, chapter=chapter, title=title, intro=intro,
                            fen=board.fen(), goal=f'白方{ "一步" if mate_in == 1 else "两步" }将杀', mateIn=mate_in,
                            lines=lines, hints=[first_hint, f'试试{action}。'], explanation=explanation,
                            question=None, answer=None, source=source))
        audits.append(dict(number=number, puzzleId=row['PuzzleId'], pieces=len(board.piece_map()), mateIn=mate_in,
                           acceptedFirstMoves=len({line[0] for line in lines}), terminalLines=len(lines),
                           legalDefences=sum(len({line[1] for line in lines if line[0] == root}) for root in {line[0] for line in lines}) if mate_in == 2 else 0,
                           sourceSolutionSan=' '.join(san), sourceSolutionVerified=True, everyDefenceVerified=True))
    (ROOT / 'app/src/main/assets/lessons.json').write_text(json.dumps(lessons, ensure_ascii=False, indent=2)+'\n')
    report = dict(source=DATASET, sourceCsvSha256=hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
                  lessons=len(lessons), mateIn1=sum(x['mateIn']==1 for x in lessons), mateIn2=sum(x['mateIn']==2 for x in lessons),
                  validation='Exhaustive legal-move enumeration with python-chess; every terminal is checkmate; no position edited.', puzzles=audits)
    (ROOT / 'curriculum/validation.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    # Export notation using python-chess to include correct move numbers and turns.
    games = []
    for lesson in lessons:
        b = chess.Board(lesson['fen']); game = chess.pgn.Game(); game.setup(b)
        game.headers['Event'] = 'Lichess puzzle '+lesson['source']['puzzleId']
        game.headers['Site'] = lesson['source']['url']; game.headers['Result'] = '1-0'
        node = game
        for uci in lesson['source']['originalMoves'][1:]: node = node.add_variation(chess.Move.from_uci(uci))
        games.append(str(game))
    (ROOT / 'curriculum/selected-puzzles.pgn').write_text('\n\n'.join(games)+'\n')
    print(f'Verified {len(lessons)} unchanged sourced positions; {sum(len(x["lines"]) for x in lessons)} mate-ending lines', flush=True)


if __name__ == '__main__':
    build()
