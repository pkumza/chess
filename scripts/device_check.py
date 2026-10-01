#!/usr/bin/env python3
"""ADB UI helper scoped to the family's chess application and one serial."""
import subprocess, sys, pathlib, xml.etree.ElementTree as ET, re, time
ROOT = pathlib.Path(__file__).resolve().parents[1]
ADB = [str(ROOT/'scripts/adb.sh'), '-s', '6fa3dae4']
def adb(*args):
    return subprocess.check_output(ADB + list(args))
def tree():
    adb('shell','uiautomator','dump','/sdcard/parentchess-ui.xml')
    data = adb('exec-out','cat','/sdcard/parentchess-ui.xml').decode()
    data = data[data.index('<?xml'):]
    data = data[:data.index('</hierarchy>') + len('</hierarchy>')]
    root = ET.fromstring(data)
    if not any(n.attrib.get('package') == 'cn.parentchess' for n in root.iter('node')):
        raise RuntimeError('Chess app is not foreground; will not interact with other applications')
    return root

def nodes():
    for n in tree().iter('node'):
        a=n.attrib
        if a.get('text') or a.get('content-desc'):
            print(a.get('text') or a.get('content-desc'), a['bounds'])
def tap(label):
    for n in tree().iter('node'):
        a=n.attrib
        if a.get('text') == label or a.get('content-desc') == label:
            x1,y1,x2,y2=map(int,re.findall(r'\d+',a['bounds']))
            adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2)); return
    raise RuntimeError('Missing UI element: '+label)
def shot(name):
    tree()
    (ROOT/'artifacts'/name).write_bytes(adb('exec-out','screencap','-p'))
if __name__ == '__main__':
    if sys.argv[1] == 'nodes': nodes()
    elif sys.argv[1] == 'tap': tap(sys.argv[2])
    elif sys.argv[1] == 'shot': shot(sys.argv[2])
