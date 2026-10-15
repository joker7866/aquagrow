# WCAG 2.1 contrast checker for the AquaGrow palette. Run: python tools/contrast_check.py
def L(h):
    h=h.lstrip('#'); c=[int(h[i:i+2],16)/255 for i in (0,2,4)]
    c=[x/12.92 if x<=0.03928 else ((x+0.055)/1.055)**2.4 for x in c]
    return 0.2126*c[0]+0.7152*c[1]+0.0722*c[2]
def cr(a,b):
    a,b=L(a),L(b); return (max(a,b)+0.05)/(min(a,b)+0.05)
pairs={
 'L ink/page':('1C2421','FAF8F4'),'L muted/page':('5B6660','FAF8F4'),'L muted/white':('5B6660','FFFFFF'),
 'L muted/gardenpage':('5B6660','F7F4EE'),'L muted/beige':('5B6660','F4EFE6'),
 'L garden/white':('2F6B3A','FFFFFF'),'L white/garden':('FFFFFF','2F6B3A'),'L gardenOnCont/cont':('1E4A27','E3ECE1'),
 'L garden/container':('2F6B3A','E3ECE1'),'L aqua/white':('0B5D8C','FFFFFF'),'L aqua/container':('0B5D8C','DDF0F4'),
 'L aquaOnCont/cont':('083F60','DDF0F4'),'L warn':('8A4B0F','FCEBD9'),'L success/white':('1B6B3A','FFFFFF'),
 'D ink/page':('E6E9E6','121614'),'D muted/card':('AEB8B1','1B201D'),'D garden/card':('94D09E','1B201D'),
 'D onGarden/garden':('0B3A16','94D09E'),'D gardenOnCont':('D3E8D5','223A27'),'D aqua/card':('85C8F0','1B201D'),
 'D onAqua/aqua':('00324D','85C8F0'),'D aquaOnCont':('CFEAF4','15394B'),'D warn':('F7CFA3','4A2E12'),
 'D success/card':('8FD6A4','1B201D'),'D muted/aquapage':('AEB8B1','101719'),
}
for k,(a,b) in pairs.items(): r=cr(a,b); print(f"{k:22s} {r:5.2f} {'OK' if r>=4.5 else 'FAIL'}")
