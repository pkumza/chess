"""Original short brass-like major fanfare, synthesized locally; no borrowed samples."""
import math
import struct
import wave
from pathlib import Path

rate = 44100
duration = 1.08
notes = [(0, .18, 523.25), (.20, .18, 659.25), (.40, .18, 783.99), (.61, .43, 1046.50)]
samples = []
for i in range(int(rate * duration)):
    t = i / rate
    value = 0.0
    for start, length, frequency in notes:
        u = t - start
        if 0 <= u < length:
            attack = min(1, u / .018)
            release = min(1, (length - u) / .085)
            envelope = attack * release * (.8 + .2 * math.exp(-u / .045))
            phase = 2 * math.pi * frequency * u
            # Rounded brass harmonics; soften the highest partials for small ears.
            tone = sum(g * math.sin(h * phase) for h, g in [(1, .62), (2, .23), (3, .11), (4, .035)])
            value += envelope * tone
    samples.append(value)
path = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw/lesson_success.wav'
with wave.open(str(path), 'wb') as out:
    out.setnchannels(1); out.setsampwidth(2); out.setframerate(rate)
    out.writeframes(b''.join(struct.pack('<h', round(x * .55 * 32767)) for x in samples))
