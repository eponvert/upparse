#!/usr/bin/env python

import sys

for line in sys.stdin:
  line = line.split()
  i = 0
  n = len(line)

  print '(',
  while i < n:

    if line[i] == ';':
      i += 1
   
    elif line[i] in ('DT','CD'):
      start = i
      i += 1

      while i < n and line[i] == 'JJ':
        i += 1

      while i < n and line[i] in ('NN','NNS'):
        i += 1

      if i - start > 1:
        j = start
        print '(',
        for j in range(start,i):
          print line[j],
        print ')',

#    elif line[i] in ('JJ', 'NN','NNS'):
#      start = i
#      i += 1
#
#      if line[i-1] == 'JJ':
#        while i < n and line[i] == 'JJ':
#          i += 1
#
#      while i < n and line[i] in ('NN','NNS'):
#        i += 1
#
#      if i - start > 1:
#        j = start
#        print '(',
#        for j in range(start,i):
#          print line[j],
#        print ')',

    elif line[i] == 'NNP':
      start = i
      i += 1
      while i < n and line[i] == 'NNP':
        i += 1

      if i - start > 1:
        print '(',
        for j in range(start,i):
          print line[j],
        print ')',

    elif i < n: 
      print line[i],
      i += 1

  print ')'
