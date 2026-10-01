"""Inventory the complete official dump and retain short, well-established candidates.
PYTHONPATH=.tools/puzzle-deps python3 scripts/index_puzzle_database.py
"""
import csv
import hashlib
import io
import json
from collections import Counter
from pathlib import Path
import zstandard

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / '.tools/downloads/lichess_db_puzzle.csv.zst'
OUT = ROOT / 'curriculum/progression'


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    counts, ratings, lengths = Counter(), Counter(), Counter()
    count = kept = 0
    with SOURCE.open('rb') as compressed, (OUT / 'candidates.csv').open('w') as dest:
        reader = csv.DictReader(io.TextIOWrapper(zstandard.ZstdDecompressor().stream_reader(compressed)))
        writer = csv.DictWriter(dest, fieldnames=reader.fieldnames)
        writer.writeheader()
        for row in reader:
            count += 1
            assert all(row.get(k) for k in ('PuzzleId', 'FEN', 'Moves', 'Rating', 'Themes'))
            tags = row['Themes'].split()
            rating = int(row['Rating'])
            moves = row['Moves'].split()
            counts.update(tags)
            ratings[(rating // 200) * 200] += 1
            lengths[len(moves)-1] += 1
            if (400 <= rating <= 1600 and int(row['Popularity']) >= 90
                    and int(row['NbPlays']) >= 1000 and int(row['RatingDeviation']) <= 100
                    and len(moves) in (2, 4, 6)):
                writer.writerow(row)
                kept += 1
            if count % 1000000 == 0:
                print(f'Scanned {count:,}; retained {kept:,}', flush=True)
    assert count > 6000000, 'Not the complete expected dataset'
    with SOURCE.open('rb') as file:
        digest = hashlib.sha256()
        for chunk in iter(lambda: file.read(1024*1024), b''):
            digest.update(chunk)
    report = dict(source='https://database.lichess.org/#puzzles', retrievedAt='2026-10-01',
                  bytes=SOURCE.stat().st_size, sha256=digest.hexdigest(), rows=count,
                  retainedCandidates=kept, themes=dict(counts.most_common()),
                  ratingBands=dict(sorted(ratings.items())), solutionPlies=dict(sorted(lengths.items())),
                  filters=dict(rating=[400,1600], popularityMin=90, playsMin=1000,
                               ratingDeviationMax=100, playerMoves=[1,2,3]))
    (OUT / 'database-inventory.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(f'Complete inventory: {count:,} official puzzles; {kept:,} short candidates', flush=True)


if __name__ == '__main__':
    main()
