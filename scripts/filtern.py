#!/usr/bin/env python

import sys

n = int(sys.argv[1])
inp = open(sys.argv[2])
outp = open(sys.argv[3], 'w')

for l in inp:
  l = l.strip()
  line = l.replace('(','( ').replace(')',' )').split()
  if len([x for x in line if x not in ('(',')')]) <= n:
    print >>outp, l
inp.close()
outp.close()
