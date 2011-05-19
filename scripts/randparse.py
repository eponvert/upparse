#!/usr/bin/env python

'''
produces random (uniform) binary parses of strings
'''

from sys import stdin, stderr, exit
from random import randint

PUNC=';'
PUNC_SET = [".", "?", "!", ";", ",", "--", u"\u3002", u"\u3001", u"\uFF0C"]

def randparse(s):
  if len(s) <= 2:
    return s
  new_parse = [None,None]
  split = randint(1,len(s)-1)
  new_parse[0] = randparse(s[:split])
  new_parse[1] = randparse(s[split:])
  return new_parse

def rbparse(s):
  if len(s) <= 2:
    return s

  else:
    return [s[0], rbparse(s[1:])]

def spparse(s):
  return s

def pieces(s):
  if type(s) == str:
    yield s
  elif type(s) == list:
    if len(s) == 1:
      yield s[0]

    else:
      yield '('
      for _s in s:
        for __s in pieces(_s):
          yield __s
      yield ')'

  else:
    print >>stderr, 'wtf', s
    exit(1)

def pprint(s):
  if len(s) == 0:
    print

  elif len(s) == 1 and type(s[0]) == str:
    print '(' + s[0] + ')'

  else:
    s = ' '.join(pieces(s))
    print s.replace('( ','(').replace(' )',')')

if __name__ == '__main__':
  from optparse import OptionParser
  op = OptionParser()
  op.add_option('-e','--eval',default='rand')
  opt, args = op.parse_args()

  dispatch = dict(rand = randparse, rb = rbparse, sp = spparse)
  f = dispatch[opt.eval]

  for s in stdin:
    s = s.split()
    
    for i in range(len(s)):
      if s[i] in PUNC_SET:
        s[i] = PUNC

    if PUNC not in s:
      pprint(f(s))
      continue

    start = 0
    new_s = []
    while PUNC in s:
      end = s.index(PUNC)
      if start != end:
        new_s.append(f(s[start:end]))
      del s[end]
      start = end

    if len(new_s) == 1:
      new_s = new_s[0]

    pprint(new_s)

