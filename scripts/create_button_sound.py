"""Original soft, rising bubble click, generated offline."""
import math
import struct
import wave
from pathlib import Path

rate = 44100
duration = .085
samples = []
for i in range(int(rate * duration)):
    t = i / rate
    envelope = min(1, t / .004) * math.exp(-t / .023) * min(1, (duration - t) / .012)
    phase = 2 * math.pi * (780 * t + 1800 * t * t)
    samples.append(envelope * (math.sin(phase) + .18 * math.sin(phase * 2)))
path = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw/button_click.wav'
with wave.open(str(path), 'wb') as out:
    out.setnchannels(1)
    out.setsampwidth(2)
    out.setframerate(rate)
    out.writeframes(b''.join(struct.pack('<h', round(x * .42 * 32767)) for x in samples))
