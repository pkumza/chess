"""Select unchanged official puzzles; export a reproducible, legal-move-checked PGN pack.
PYTHONPATH=.tools/puzzle-deps:.tools/python-chess python3 scripts/collect_advanced_puzzles.py INPUT.zst
The input may be a prefix of the official dump; its final incomplete row is ignored.
"""
import csv
import hashlib
import json
import sys
from collections import Counter
from pathlib import Path
import chess
import chess.pgn
import zstandard

ROOT = Path(__file__).resolve().parents[1]
GROUPS = {
    'hangingPiece': '吃无保护棋子', 'fork': '双重攻击', 'pin': '牵制',
    'skewer': '串击', 'discoveredAttack': '闪击', 'defensiveMove': '防守',
    'capturingDefender': '消除保护', 'deflection': '引离防守',
    'promotion': '升变', 'intermezzo': '中间着',
}


def main():
    source = Path(sys.argv[1])
    out = ROOT / 'curriculum/advanced-1000'
    out.mkdir(parents=True, exist_ok=True)
    excluded = {r['PuzzleId'] for r in csv.DictReader((ROOT / 'curriculum/lichess-puzzles.csv').open())}
    pools = {theme: [] for theme in GROUPS}
    scanned = 0
    # Decompress incrementally, retaining only complete newline-terminated rows.
    decoder = zstandard.ZstdDecompressor().decompressobj()
    pending = b''
    header = None
    with source.open('rb') as stream:
        while chunk := stream.read(1024 * 1024):
            while chunk:
                pending += decoder.decompress(chunk)
                chunk = decoder.unused_data if decoder.eof else b''
                if decoder.eof:
                    decoder = zstandard.ZstdDecompressor().decompressobj()
            lines = pending.split(b'\n')
            pending = lines.pop()
            for raw in lines:
                values = next(csv.reader([raw.decode()]))
                if header is None:
                    header = values
                    continue
                row = dict(zip(header, values))
                scanned += 1
                if row['PuzzleId'] in excluded:
                    continue
                tags = set(row['Themes'].split())
                if 'mate' in tags or not 600 <= int(row['Rating']) <= 1800:
                    continue
                if int(row['Popularity']) < 90 or int(row['NbPlays']) < 1000:
                    continue
                if not 2 <= len(row['Moves'].split()) <= 6:
                    continue
                for theme in GROUPS:
                    if theme in tags:
                        pools[theme].append(row)
    selected, ids, positions = [], set(), set()
    # Allocate the scarcest themes first, avoiding duplicate positions across themes.
    by_theme = {}
    for theme in sorted(GROUPS, key=lambda t: len(pools[t])):
        chosen = []
        for row in sorted(pools[theme], key=lambda r: (int(r['Rating']), -int(r['NbPlays']), r['PuzzleId'])):
            if row['PuzzleId'] in ids:
                continue
            board = chess.Board(row['FEN'])
            assert board.is_valid()
            moves = row['Moves'].split()
            for uci in moves:
                move = chess.Move.from_uci(uci)
                assert move in board.legal_moves, row['PuzzleId']
                board.push(move)
                assert board.is_valid()
            initial = chess.Board(row['FEN'])
            initial.push_uci(moves[0])
            key = initial.fen().split(' ')[:4]
            key = ' '.join(key)
            if key in positions:
                continue
            assert not initial.is_game_over()
            ids.add(row['PuzzleId'])
            positions.add(key)
            chosen.append(row)
            if len(chosen) == 100:
                break
        assert len(chosen) == 100, (theme, len(chosen), len(pools[theme]))
        by_theme[theme] = chosen
    games, index = [], []
    for theme, label in GROUPS.items():
        chapter_games = []
        for row in by_theme[theme]:
            selected.append(row)
            moves = row['Moves'].split()
            board = chess.Board(row['FEN'])
            board.push_uci(moves[0])
            game = chess.pgn.Game()
            game.setup(board)
            game.headers.update(Event=f'{label} / Lichess {row["PuzzleId"]}',
                                Site=f'https://lichess.org/training/{row["PuzzleId"]}',
                                Result='*', PuzzleId=row['PuzzleId'],
                                PuzzleRating=row['Rating'], Themes=row['Themes'],
                                SourceGame=row['GameUrl'], License='CC0-1.0')
            game.comment = 'Official solution. Completion means finishing this tactical line, not winning the whole game.'
            node = game
            for uci in moves[1:]:
                node = node.add_variation(chess.Move.from_uci(uci))
            text = str(game)
            games.append(text)
            chapter_games.append(text)
            index.append(dict(number=len(index)+1, chapter=label, theme=theme,
                              puzzleId=row['PuzzleId'], fen=board.fen(), moves=moves[1:],
                              side='white' if board.turn else 'black', rating=int(row['Rating']),
                              url=game.headers['Site'], gameUrl=row['GameUrl']))
        (out / f'{theme}.pgn').write_text('\n\n'.join(chapter_games)+'\n')
    (out / 'advanced-1000.pgn').write_text('\n\n'.join(games)+'\n')
    with (out / 'source.csv').open('w') as file:
        writer = csv.DictWriter(file, fieldnames=header)
        writer.writeheader()
        writer.writerows(selected)
    (out / 'index.json').write_text(json.dumps(index, ensure_ascii=False, indent=2)+'\n')
    report = dict(source='https://database.lichess.org/#puzzles', license='CC0-1.0',
                  retrievedAt='2026-10-01', inputSha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                  scannedCompleteRows=scanned, selected=len(index),
                  chapters={GROUPS[t]: len(by_theme[t]) for t in GROUPS},
                  sides=dict(Counter(i['side'] for i in index)),
                  validation='Original positions and official lines unchanged; all setup and solution moves legal; all positions valid; no duplicate starting positions; original 100 puzzle IDs excluded.',
                  limitations='Legality validation does not independently prove optimality or every defensive branch. Themes and answers supplied by Lichess. Not a complete annotated teaching course.')
    (out / 'validation.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
