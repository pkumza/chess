# 100 道实战残局将杀题：来源与核对

来源：[Lichess 官方公开题库](https://database.lichess.org/#puzzles)，数据采用 [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/)。2026-10-01 从官方 `lichess_db_puzzle.csv.zst` 的前 64 MiB 压缩数据中读取了 1,343,564 条完整记录，筛选后保留 100 条原始 CSV 记录；并抽查官方 API 的 CtI2H、97FFE 原题解法，与下载记录一致。

这是一组**残局将杀练习**，不是完整的残局战略教材，也未经儿童教学专家认证。中文标题、提示和排序由应用编写；棋盘和原题解法均来自题库，未删子、换位或改变执棋方。

## 选题标准

- 均带官方 `endgame` 标签，题面 4–7 枚棋子，白方走。
- 70 道一步将杀、30 道两步将杀；无相同棋子布局的重复题。
- 下载时题目评分不高于 1200、受欢迎度至少 90、解题记录至少 1000 次。网站当前评分可能变化。
- 先少子、后稍复杂；按车、后、小兵与升变、混合将杀、两步将杀分组。评分不代表儿童适龄认证。
- 不引入需要长期评估“优势”的题；只有黑王实际被将杀才通关。

## 核对方法

1. `lichess-puzzles.csv` 是未经改动的官方 CSV 字段；`selection.json` 指定顺序。
2. 官方 CSV 的 FEN 在对手铺垫着之前，先执行 Moves 的第一步才是题面。其余走法是原解。
3. python-chess 验证题面合法、原解合法且以将杀结束；穷举全部一步将杀。
4. 两步题对每个候选首着枚举**全部**合法防守，每种防守必须都有下一步将杀；所有正确首着和将杀续着都收录。应用优先播放原题防守。
5. Android 单元测试用独立的 Chesslib 再核对原始棋盘、原解、所有防守、替代解法和通关时机。
6. 新题使用 `lichess-题号` 标识。旧 24 关完成记录保留在存档中，不冒充新题进度。

运行：`PYTHONPATH=.tools/python-chess python3 scripts/create_lessons.py`；验证结果见 [validation.json](validation.json)，可导入棋谱软件的原解见 [selected-puzzles.pgn](selected-puzzles.pgn)。旧自编题仅作审计归档：[legacy-original-24.json](legacy-original-24.json)，不再作为课程使用。

原始 CSV SHA-256：`606a6e61929ca006a8289f5049fb2d1749f58d8e1679afb8be82ebe1bc349038`

## 逐题清单

| 关卡 | 原题 | 子数 | 目标 | 原题解法（SAN） | 实战 |
|---|---|---:|---|---|---|
| 1 | [CtI2H](https://lichess.org/training/CtI2H) | 4 | 白方一步将杀 | Ra1# | [对局](https://lichess.org/o6hMbq6O/black#130) |
| 2 | [BMNr4](https://lichess.org/training/BMNr4) | 4 | 白方一步将杀 | Rh8# | [对局](https://lichess.org/2zbFGhQy/black#194) |
| 3 | [4kdlf](https://lichess.org/training/4kdlf) | 4 | 白方一步将杀 | Ra1# | [对局](https://lichess.org/VXDo6eFA/black#162) |
| 4 | [2ekIB](https://lichess.org/training/2ekIB) | 4 | 白方一步将杀 | Rh1# | [对局](https://lichess.org/4knvLVzv/black#146) |
| 5 | [7jc43](https://lichess.org/training/7jc43) | 4 | 白方一步将杀 | Rb1# | [对局](https://lichess.org/5gUdRBW3/black#130) |
| 6 | [5MQS3](https://lichess.org/training/5MQS3) | 4 | 白方一步将杀 | Rg8# | [对局](https://lichess.org/8Ru3bC0J/black#188) |
| 7 | [DX4LM](https://lichess.org/training/DX4LM) | 4 | 白方一步将杀 | Ra8# | [对局](https://lichess.org/aXElUmu0/black#156) |
| 8 | [B2NNH](https://lichess.org/training/B2NNH) | 4 | 白方一步将杀 | Rh1# | [对局](https://lichess.org/CSgcg8wz/black#128) |
| 9 | [44Joh](https://lichess.org/training/44Joh) | 4 | 白方一步将杀 | Rh1# | [对局](https://lichess.org/TUKh4LqN/black#182) |
| 10 | [AiuDs](https://lichess.org/training/AiuDs) | 4 | 白方一步将杀 | Ra1# | [对局](https://lichess.org/jS1F9MOL/black#148) |
| 11 | [5Rly6](https://lichess.org/training/5Rly6) | 4 | 白方一步将杀 | Rc1# | [对局](https://lichess.org/mgj2tv17/black#120) |
| 12 | [62rvb](https://lichess.org/training/62rvb) | 4 | 白方一步将杀 | Ra1# | [对局](https://lichess.org/2GYXO2b5/black#168) |
| 13 | [DVClj](https://lichess.org/training/DVClj) | 4 | 白方一步将杀 | Ra1# | [对局](https://lichess.org/EQ4SR3kl/black#162) |
| 14 | [D1Rfg](https://lichess.org/training/D1Rfg) | 4 | 白方一步将杀 | Rh2# | [对局](https://lichess.org/qmI6p7l8/black#146) |
| 15 | [2rYQq](https://lichess.org/training/2rYQq) | 5 | 白方一步将杀 | Re1# | [对局](https://lichess.org/68qRvpZI/black#130) |
| 16 | [Bhuv7](https://lichess.org/training/Bhuv7) | 5 | 白方一步将杀 | Re1# | [对局](https://lichess.org/o904s4zb/black#158) |
| 17 | [D5YvG](https://lichess.org/training/D5YvG) | 5 | 白方一步将杀 | Re8# | [对局](https://lichess.org/WXC5dzLK/black#124) |
| 18 | [3zJv1](https://lichess.org/training/3zJv1) | 5 | 白方一步将杀 | Rh3# | [对局](https://lichess.org/mRoilAmH/black#138) |
| 19 | [3Lt8K](https://lichess.org/training/3Lt8K) | 5 | 白方一步将杀 | Re8# | [对局](https://lichess.org/Fq3g8ydi/black#140) |
| 20 | [Cwx3J](https://lichess.org/training/Cwx3J) | 5 | 白方一步将杀 | Rh6# | [对局](https://lichess.org/l1RpvDH8/black#102) |
| 21 | [CEpsC](https://lichess.org/training/CEpsC) | 5 | 白方一步将杀 | Rh1# | [对局](https://lichess.org/7NRwrOad/black#102) |
| 22 | [8Julq](https://lichess.org/training/8Julq) | 5 | 白方一步将杀 | Rh8# | [对局](https://lichess.org/8BwUmq4P/black#94) |
| 23 | [0Og6f](https://lichess.org/training/0Og6f) | 5 | 白方一步将杀 | Rf8# | [对局](https://lichess.org/4ixEGS3u/black#104) |
| 24 | [70rTN](https://lichess.org/training/70rTN) | 5 | 白方一步将杀 | Re1# | [对局](https://lichess.org/H2IFI8Gw/black#158) |
| 25 | [2UVpX](https://lichess.org/training/2UVpX) | 5 | 白方一步将杀 | Ra8# | [对局](https://lichess.org/O9cBrFQ9/black#152) |
| 26 | [ApTEb](https://lichess.org/training/ApTEb) | 4 | 白方一步将杀 | Qa6# | [对局](https://lichess.org/xqvL3HiO/black#120) |
| 27 | [BhrRA](https://lichess.org/training/BhrRA) | 4 | 白方一步将杀 | Qxg1# | [对局](https://lichess.org/v6H9ise7/black#132) |
| 28 | [00T85](https://lichess.org/training/00T85) | 4 | 白方一步将杀 | Qd2# | [对局](https://lichess.org/j3dOR5SJ/black#106) |
| 29 | [3mc1O](https://lichess.org/training/3mc1O) | 4 | 白方一步将杀 | Qg5# | [对局](https://lichess.org/29fdiqQn/black#152) |
| 30 | [ADpdf](https://lichess.org/training/ADpdf) | 4 | 白方一步将杀 | Qd2# | [对局](https://lichess.org/DXNBSMyK/black#124) |
| 31 | [1N1SD](https://lichess.org/training/1N1SD) | 5 | 白方一步将杀 | Qxb1# | [对局](https://lichess.org/LnRZ6UL1/black#124) |
| 32 | [BcUcc](https://lichess.org/training/BcUcc) | 5 | 白方一步将杀 | Qh3# | [对局](https://lichess.org/6Z8jC9k2/black#110) |
| 33 | [1jNl1](https://lichess.org/training/1jNl1) | 5 | 白方一步将杀 | Qc3# | [对局](https://lichess.org/a3UuHpEl/black#130) |
| 34 | [D5DQh](https://lichess.org/training/D5DQh) | 5 | 白方一步将杀 | Qb1# | [对局](https://lichess.org/sSVVvn2B/black#124) |
| 35 | [DQVPs](https://lichess.org/training/DQVPs) | 6 | 白方一步将杀 | Qe1# | [对局](https://lichess.org/uYlUig97/black#180) |
| 36 | [8Xev6](https://lichess.org/training/8Xev6) | 6 | 白方一步将杀 | Qf2# | [对局](https://lichess.org/R9nwIC7F/black#152) |
| 37 | [C4F8F](https://lichess.org/training/C4F8F) | 6 | 白方一步将杀 | Qh4# | [对局](https://lichess.org/QycZqjh4/black#122) |
| 38 | [65EcB](https://lichess.org/training/65EcB) | 6 | 白方一步将杀 | Qxh6# | [对局](https://lichess.org/LRslG8lF/black#134) |
| 39 | [Ct19J](https://lichess.org/training/Ct19J) | 6 | 白方一步将杀 | Qh3# | [对局](https://lichess.org/NyGc92Ak/black#124) |
| 40 | [5D4HG](https://lichess.org/training/5D4HG) | 6 | 白方一步将杀 | Qg3# | [对局](https://lichess.org/uHb3YyyC/black#148) |
| 41 | [3FsA0](https://lichess.org/training/3FsA0) | 6 | 白方一步将杀 | Qf1# | [对局](https://lichess.org/YvFIt8jp/black#146) |
| 42 | [18gFE](https://lichess.org/training/18gFE) | 6 | 白方一步将杀 | Qb6# | [对局](https://lichess.org/GhuKFlHX/black#194) |
| 43 | [6kFAO](https://lichess.org/training/6kFAO) | 6 | 白方一步将杀 | Qg6# | [对局](https://lichess.org/825da6CB/black#168) |
| 44 | [3OB0O](https://lichess.org/training/3OB0O) | 6 | 白方一步将杀 | Qd1# | [对局](https://lichess.org/hweUlbiE/black#126) |
| 45 | [0u5Sh](https://lichess.org/training/0u5Sh) | 6 | 白方一步将杀 | Qg5# | [对局](https://lichess.org/ew9JAXJH/black#100) |
| 46 | [7yoiw](https://lichess.org/training/7yoiw) | 6 | 白方一步将杀 | Qg3# | [对局](https://lichess.org/34wmSwA7/black#138) |
| 47 | [D7HIG](https://lichess.org/training/D7HIG) | 7 | 白方一步将杀 | Qh8# | [对局](https://lichess.org/b3Tf5Hnh/black#102) |
| 48 | [3tDIc](https://lichess.org/training/3tDIc) | 7 | 白方一步将杀 | Qf4# | [对局](https://lichess.org/fcemyoW3/black#104) |
| 49 | [4Cssw](https://lichess.org/training/4Cssw) | 7 | 白方一步将杀 | Qg5# | [对局](https://lichess.org/Rye68O5t/black#110) |
| 50 | [92VgE](https://lichess.org/training/92VgE) | 7 | 白方一步将杀 | Qb3# | [对局](https://lichess.org/8bbPNWWN/black#134) |
| 51 | [BYPi5](https://lichess.org/training/BYPi5) | 5 | 白方一步将杀 | c7# | [对局](https://lichess.org/pcADwV0I/black#152) |
| 52 | [DcHYc](https://lichess.org/training/DcHYc) | 5 | 白方一步将杀 | g7# | [对局](https://lichess.org/PbYsk2ph/black#130) |
| 53 | [BRAoL](https://lichess.org/training/BRAoL) | 6 | 白方一步将杀 | g4# | [对局](https://lichess.org/J4wBG8e6/black#120) |
| 54 | [28RzL](https://lichess.org/training/28RzL) | 6 | 白方一步将杀 | h8=Q# | [对局](https://lichess.org/rkU0BM1v/black#140) |
| 55 | [BHIzD](https://lichess.org/training/BHIzD) | 6 | 白方一步将杀 | a4# | [对局](https://lichess.org/BKspUaMp/black#116) |
| 56 | [7zZnW](https://lichess.org/training/7zZnW) | 7 | 白方一步将杀 | g7# | [对局](https://lichess.org/gJvIqxAK/black#112) |
| 57 | [1Sd2F](https://lichess.org/training/1Sd2F) | 7 | 白方一步将杀 | a7# | [对局](https://lichess.org/NZyefE4r/black#100) |
| 58 | [0CCsd](https://lichess.org/training/0CCsd) | 7 | 白方一步将杀 | h8=Q# | [对局](https://lichess.org/qtHGh0ki/black#102) |
| 59 | [2GH4Q](https://lichess.org/training/2GH4Q) | 7 | 白方一步将杀 | c8=Q# | [对局](https://lichess.org/j7JYYWtC/black#100) |
| 60 | [0abZ0](https://lichess.org/training/0abZ0) | 7 | 白方一步将杀 | g3# | [对局](https://lichess.org/mRdHKRD0/black#132) |
| 61 | [1bJcc](https://lichess.org/training/1bJcc) | 5 | 白方一步将杀 | Nf7# | [对局](https://lichess.org/vUSwYXjq/black#120) |
| 62 | [0YMqu](https://lichess.org/training/0YMqu) | 5 | 白方一步将杀 | Nf6# | [对局](https://lichess.org/eAg8BKXJ/black#126) |
| 63 | [CwKl7](https://lichess.org/training/CwKl7) | 6 | 白方一步将杀 | Rxh6# | [对局](https://lichess.org/bvLXEPII/black#84) |
| 64 | [3GpAW](https://lichess.org/training/3GpAW) | 6 | 白方一步将杀 | Ra6# | [对局](https://lichess.org/Jnqjuysc/black#88) |
| 65 | [CwKnP](https://lichess.org/training/CwKnP) | 6 | 白方一步将杀 | Ra1# | [对局](https://lichess.org/qWBz8u8A/black#104) |
| 66 | [5gQSn](https://lichess.org/training/5gQSn) | 6 | 白方一步将杀 | Rxh1# | [对局](https://lichess.org/VyxbEdmu/black#110) |
| 67 | [1hh2J](https://lichess.org/training/1hh2J) | 6 | 白方一步将杀 | Rh1# | [对局](https://lichess.org/3EtLWDNG/black#102) |
| 68 | [41OUC](https://lichess.org/training/41OUC) | 6 | 白方一步将杀 | Rh1# | [对局](https://lichess.org/OeJDb50p/black#86) |
| 69 | [2Vfqn](https://lichess.org/training/2Vfqn) | 6 | 白方一步将杀 | Rc1# | [对局](https://lichess.org/kLfTtiQT/black#118) |
| 70 | [5JXA1](https://lichess.org/training/5JXA1) | 6 | 白方一步将杀 | Rh3# | [对局](https://lichess.org/TZJ8Z2MN/black#132) |
| 71 | [97FFE](https://lichess.org/training/97FFE) | 4 | 白方两步将杀 | Ra8+ Ra7 Rxa7# | [对局](https://lichess.org/W5O6LEri/black#134) |
| 72 | [07BVD](https://lichess.org/training/07BVD) | 4 | 白方两步将杀 | Rh2+ Rh5 Rxh5# | [对局](https://lichess.org/MdGayTFL/black#154) |
| 73 | [6B2vQ](https://lichess.org/training/6B2vQ) | 4 | 白方两步将杀 | Rh8+ Rg8 Rxg8# | [对局](https://lichess.org/O0nqJJbO/black#182) |
| 74 | [84Pkq](https://lichess.org/training/84Pkq) | 4 | 白方两步将杀 | Rh2+ Rh4 Rxh4# | [对局](https://lichess.org/dA1NM2B9/black#78) |
| 75 | [BTIx6](https://lichess.org/training/BTIx6) | 4 | 白方两步将杀 | Rh7+ Rh3 Rxh3# | [对局](https://lichess.org/yhXMAb6X/black#156) |
| 76 | [4Qrzs](https://lichess.org/training/4Qrzs) | 4 | 白方两步将杀 | Ra1+ Ra4 Rxa4# | [对局](https://lichess.org/VYuF0zTC/black#164) |
| 77 | [7Wlzo](https://lichess.org/training/7Wlzo) | 4 | 白方两步将杀 | Ra8+ Ra3 Rxa3# | [对局](https://lichess.org/4szN2XT5/black#128) |
| 78 | [9Jbiu](https://lichess.org/training/9Jbiu) | 4 | 白方两步将杀 | Rb1+ Qc1 Rxc1# | [对局](https://lichess.org/oCJtnCNG/black#138) |
| 79 | [9hDJu](https://lichess.org/training/9hDJu) | 4 | 白方两步将杀 | Ra2+ Ra4 Rxa4# | [对局](https://lichess.org/xZt8pYhu/black#136) |
| 80 | [7f14Q](https://lichess.org/training/7f14Q) | 4 | 白方两步将杀 | Rh1+ Rh3 Rxh3# | [对局](https://lichess.org/65mVdF15/black#154) |
| 81 | [04XDG](https://lichess.org/training/04XDG) | 5 | 白方两步将杀 | Rh8+ Rg8 Rxg8# | [对局](https://lichess.org/bblAngeH/black#102) |
| 82 | [6LqyB](https://lichess.org/training/6LqyB) | 5 | 白方两步将杀 | Ra1+ Ne1 Rxe1# | [对局](https://lichess.org/ZTCdTCo2/black#128) |
| 83 | [7rtI7](https://lichess.org/training/7rtI7) | 5 | 白方两步将杀 | Ra1+ Rc1 Rxc1# | [对局](https://lichess.org/2ugLuIHq/black#112) |
| 84 | [0RJRO](https://lichess.org/training/0RJRO) | 5 | 白方两步将杀 | Rh8+ Rh7 Rxh7# | [对局](https://lichess.org/ZL30WVL7/black#122) |
| 85 | [0Zw2D](https://lichess.org/training/0Zw2D) | 5 | 白方两步将杀 | Ra6+ Ra5 Rxa5# | [对局](https://lichess.org/SM8dAuBs/black#128) |
| 86 | [AbjPe](https://lichess.org/training/AbjPe) | 4 | 白方两步将杀 | g7+ Kh7 g8=Q# | [对局](https://lichess.org/6bQi2Qya/black#140) |
| 87 | [345YU](https://lichess.org/training/345YU) | 4 | 白方两步将杀 | g8=Q+ Kh6 Qg6# | [对局](https://lichess.org/tY1WM54a/black#128) |
| 88 | [69J9r](https://lichess.org/training/69J9r) | 4 | 白方两步将杀 | Qe5+ Qb2+ Qxb2# | [对局](https://lichess.org/wty1VA9V/black#142) |
| 89 | [3mOIj](https://lichess.org/training/3mOIj) | 5 | 白方两步将杀 | h6 Nc6 h7# | [对局](https://lichess.org/yKqpW2lT/black#126) |
| 90 | [A3jPy](https://lichess.org/training/A3jPy) | 5 | 白方两步将杀 | Qh7+ Kg4 Qh3# | [对局](https://lichess.org/NkH8E2cD/black#138) |
| 91 | [6tOfh](https://lichess.org/training/6tOfh) | 5 | 白方两步将杀 | Qh2+ Kg5 Qh6# | [对局](https://lichess.org/uu2S5juP/black#146) |
| 92 | [CcThi](https://lichess.org/training/CcThi) | 5 | 白方两步将杀 | Qe2+ Kc1 Qc2# | [对局](https://lichess.org/eCvcu0hz/black#114) |
| 93 | [5vjga](https://lichess.org/training/5vjga) | 5 | 白方两步将杀 | Qh6+ Kg3 Qh2# | [对局](https://lichess.org/gL0qyJzO/black#166) |
| 94 | [0XJfs](https://lichess.org/training/0XJfs) | 6 | 白方两步将杀 | b7 b2 b8=Q# | [对局](https://lichess.org/fPjX5wLG/black#106) |
| 95 | [21A4C](https://lichess.org/training/21A4C) | 6 | 白方两步将杀 | c6 a3 c7# | [对局](https://lichess.org/obAmNgs5/black#128) |
| 96 | [1LDYI](https://lichess.org/training/1LDYI) | 6 | 白方两步将杀 | g8=Q+ Kh6 Qg6# | [对局](https://lichess.org/qP7EyBjs/black#136) |
| 97 | [3Zny0](https://lichess.org/training/3Zny0) | 6 | 白方两步将杀 | h7+ Kh8 Rf8# | [对局](https://lichess.org/sURSa0Qz/black#128) |
| 98 | [5cyUn](https://lichess.org/training/5cyUn) | 6 | 白方两步将杀 | Kg6 a3 h7# | [对局](https://lichess.org/N12WboJw/black#118) |
| 99 | [5cqv8](https://lichess.org/training/5cqv8) | 6 | 白方两步将杀 | b6+ Kb8 Rf8# | [对局](https://lichess.org/mnpZK6GB/black#132) |
| 100 | [A5PSv](https://lichess.org/training/A5PSv) | 6 | 白方两步将杀 | g5+ Kh5 Rxh7# | [对局](https://lichess.org/pI5xNaO7/black#100) |
