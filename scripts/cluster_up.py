#!/usr/bin/env python

'''
This script takes chunked sentence-per-line input from an unsupervised parsing 
program, clusters the output and creates a new dataset to chunk.  This allows
the chunking system to be extended to "full" parsing out-of-the-box.
'''

import sys
from optparse import OptionParser
from collections import defaultdict
from array import array
from struct import pack_into
from subprocess import Popen, PIPE, STDOUT

def ident_obj(obj): 
  return ident_hsh(hash(obj))

def ident_hsh(hsh):
  return '_' + str(hsh)

def main():

  op = OptionParser()

  op.add_option('-o', '--output', default=None)
  op.add_option('-g', '--graph', default='graph.txt')
  op.add_option('-c', '--chunks', default='chunks.txt')
  op.add_option('-I', '--mclI', default=2.0, type='float')
  op.add_option('-l', '--minlinking', default=2, type='int')
  op.add_option('-L', '--maxlinking', default=100, type='int')
  op.add_option('-m', '--mcl', default='mcl')

  opt, args = op.parse_args()

  have_input_file = len(args) > 0
  have_output_file = opt.output is not None

  minlink = opt.minlinking
  maxlink = opt.maxlinking

  chunks = set()

  if have_input_file:
    fh_in = open(args[0])

  else:
    fh_in = sys.stdin

  if have_output_file:
    fh_out = open(opt.output, 'w')

  else:
    fh_out = sys.stdout

  try:
    for line in fh_in:
      line = line.replace('(', '( ').replace(')', ' )').split()
      in_chunk = False
      new_line = []
      for w in line:
        if in_chunk:
          if w == ')':
            chunk = tuple(curr_chunk)
            print >>fh_out, ident_obj(chunk),
            chunks.add(chunk)
            in_chunk = False

          else:
            curr_chunk.append(w)

        else:
          assert not in_chunk
          assert w != ')'

          if w == '(':
            in_chunk = True
            curr_chunk = []

          else:
            print >>fh_out, w,

      print >>fh_out

  except IOError:
    pass

  finally:
    if have_input_file:
      fh_in.close()

    if have_output_file:
      fh_out.close()

  print >>sys.stderr, 'done outputting training 1'

  print >>sys.stderr, 'number of chunk types:', len(chunks)

  chunk_sets = defaultdict(list)

  chunk_fh = open(opt.chunks, 'w')
  graph_fh = open(opt.graph, 'w')

  for ch in chunks:
    print >>chunk_fh, ident_obj(ch), ' '.join(ch)

    for w in ch:
      chunk_sets[w].append(hash(ch))

  chunk_fh.close()

  cliques = [cl for cl in chunk_sets.values() if minlink <= len(cl) <= maxlink]

  print >>sys.stderr, 'done outputting chunk index and building graph list'

  chunk_graph = set()
  print >>sys.stderr, 'building graph, to do %d cliques' % len(cliques)
  numlinks = 0
  c = 1
  for chunk_set in cliques:
    for c1_hsh in chunk_set:
      for c2_hsh in chunk_set:
        numlinks += 1
        chunk_graph.add((c1_hsh, c2_hsh))

    if c % 1000 == 0:
      print >>sys.stderr, str(c) # , len(n1) # len(chunk_graph)

    c += 1

  print >>sys.stderr, 'Total initial links', str(numlinks)

  print >>sys.stderr, 'done building graph'

  for c1_hsh, c2_hsh in chunk_graph:
    print >>graph_fh, '%s\t%s\t%d' % (ident_obj(c1_hsh), ident_obj(c2_hsh), 1)
  graph_fh.close()

  print >>sys.stderr, 'done writing graph'

  print >>sys.stderr, 'clustering...'
  cmd = '%s %s -I %f --abc' % (opt.mcl, opt.graph, opt.mclI)
  print >>sys.stderr, 'cmd:', cmd
  p = Popen(cmd, **dict(stdout=PIPE, stderr=STDOUT, shell=True))
  while True:
    o = p.stdout.read(1)
    if o == '': 
      break
    else:
      sys.stderr.write(o)

  print >>sys.stderr, 'quit!'
  p.wait()
  
  assert p.returncode == 0

  print >>sys.stderr, 'done clustering'

if __name__ == '__main__':
  main()
