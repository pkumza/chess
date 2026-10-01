"""Generate original short wooden chess sounds; no external audio assets."""
import math,random,struct,wave
from pathlib import Path
random.seed(42)
rate=44100
for name,capture in [('piece_move',False),('piece_capture',True)]:
    duration=.18 if not capture else .29
    samples=[]
    for i in range(int(rate*duration)):
        t=i/rate
        def strike(t,freq,decay):
            if t<0:return 0
            attack=min(1,t/.0015)
            body=(math.sin(2*math.pi*freq*t)*.55+math.sin(2*math.pi*freq*1.73*t)*.22+random.uniform(-1,1)*.23)
            return attack*math.exp(-t/decay)*body
        value=strike(t,620 if not capture else 390,.029 if not capture else .04)
        if capture:
            value+=.55*strike(t-.045,820,.035)
            # Several very short woody cracks and a softer scattering tail.
            for onset, gain, frequency in [(0,.32,1800),(.018,.22,2350),(.04,.18,1450),(.078,.10,2700),(.115,.06,1950)]:
                value+=gain*strike(t-onset,frequency,.008)
        value*=min(1,(duration-t)/.012)
        samples.append(value)
    peak=max(abs(x) for x in samples)
    pcm=b''.join(struct.pack('<h',round(x/peak*.55*32767)) for x in samples)
    with wave.open(str(Path('app/src/main/res/raw')/(name+'.wav')),'wb') as out:
        out.setnchannels(1);out.setsampwidth(2);out.setframerate(rate);out.writeframes(pcm)
