#!/usr/bin/env python

import sys

n = int(sys.argv[1])
inp = len(sys.argv) > 2 and open(sys.argv[2]) or sys.stdin
outp = len(sys.argv) > 3 and open(sys.argv[3], 'w') or sys.stdout
PUNC_SET = [".", "?", "!", ";", ",", "--", u"\u3002", u"\u3001", u"\uFF0C"]

for l in inp:
  l = l.strip()
  line = l.replace('(','( ').replace(')',' )').split()
  if len([x for x in line if x not in ['(',')'] + PUNC_SET]) <= n:
    print >>outp, l
inp.close()
outp.close()
