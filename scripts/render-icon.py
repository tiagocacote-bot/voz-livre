"""Render the store/README icon PNG (same design as res/drawable/icon_foreground.xml): python scripts/render-icon.py out.png 512"""
import sys
from PIL import Image,ImageDraw
def bez(p0,p1,p2,p3,n=80):
    return [tuple((1-t)**3*a+3*(1-t)**2*t*b+3*(1-t)*t**2*c+t**3*d for a,b,c,d in zip(p0,p1,p2,p3)) for t in [i/n for i in range(n+1)]]
LOOP=[((54,48),(61,38),(77,38),(77,48)),((77,48),(77,58),(61,58),(54,48)),((54,48),(47,38),(31,38),(31,48)),((31,48),(31,58),(47,58),(54,48))]
RAIN=[(228,3,3),(255,140,0),(255,214,0),(0,160,70),(0,90,220),(120,40,170)]
def color(x):
    t=min(max((x-27)/54,0),.9999)*(len(RAIN)-1);i=int(t);f=t-i
    return tuple(int(RAIN[i][k]+(RAIN[i+1][k]-RAIN[i][k])*f) for k in range(3))
def render(size,out):
    S=4*size;s=S/108
    im=Image.new('RGB',(S,S),(8,127,140));d=ImageDraw.Draw(im)
    d.rounded_rectangle([20*s,22*s,88*s,74*s],radius=13*s,fill='white')
    d.polygon([(30*s,70*s),(46*s,70*s),(28*s,88*s)],fill='white')
    w=7.5*s
    for seg in LOOP:
        pts=bez(*seg)
        for a,b in zip(pts,pts[1:]):
            c=color((a[0]+b[0])/2)
            d.line([(a[0]*s,a[1]*s),(b[0]*s,b[1]*s)],fill=c,width=int(w))
            d.ellipse([a[0]*s-w/2,a[1]*s-w/2,a[0]*s+w/2,a[1]*s+w/2],fill=c)
    im.resize((size,size),Image.LANCZOS).save(out)
render(int(sys.argv[2]),sys.argv[1])
