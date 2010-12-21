#!/usr/bin/env python

'''
This script takes chunked sentence-per-line input from an unsupervised parsing
program, clusters the output and creates a new dataset to chunk.  This allows
the chunking system to be extended to "full" parsing out-of-the-box.
'''

import sys
from os import listdir, sep
from os.path import dirname, basename, exists
from optparse import OptionParser
from collections import defaultdict
from array import array
from struct import pack_into
from subprocess import Popen, PIPE, STDOUT

def ident_obj(obj):
  return intern('(' + ('_'.join(obj)) + ')')

def zero():
  return 0.0

def use_stdio(fname):

  return fname == '-'

def log(s):

  print >>sys.stderr, '+', s

def read_chunker_output(input):
  'Create chunk symbols from chunker output'

  if use_stdio(input):
    fh_in = sys.stdin

  else:
    fh_in = open(input)

  chunks = set()
  corpus = []

  try:
    for line in fh_in:
      sentence = []
      line = line.replace('(', '( ').replace(')', ' )').split()
      in_chunk = False
      new_line = []
      for w in line:
        if in_chunk:
          if w == ')':
            chunk = intern('(' + ('_'.join(curr_chunk)) + ')')
            chunks.add(chunk)
            sentence.append(chunk)
            in_chunk = False

          else:
            curr_chunk.append(w)

        else:
          assert w != ')'

          if w == '(':
            in_chunk = True
            curr_chunk = []

          else:
            sentence.append(w)

      corpus.append(sentence)

  except IOError:
    pass

  finally:
    if not use_stdio(input):
      fh_in.close()

  return corpus, chunks

def is_chunk(w):

  return w[0] == '('

def make_graph(graph_fname, chunks, minlink, maxlink, weight):
  'Creates initial graph filename for MCL'

  chunk_sets = defaultdict(list)

  for ch in chunks:
    for w in ch[1:-1].split('_'):
      chunk_sets[w].append(ch)

  if maxlink > 1:
    filt = lambda x:minlink <= len(x) <= maxlink
  else:
    filt = lambda x:minlink <= len(x)

  cliques = [cl for cl in chunk_sets.values() if filt(cl)]

  log('done outputting chunk index and building graph list')

  chunk_graph = defaultdict(zero)
  log('building graph, to do %d cliques' % len(cliques))
  numlinks = 0
  c = 1
  for chunk_set in cliques:
    for c1 in chunk_set:
      for c2 in chunk_set:
        numlinks += 1

        if weight is None or weight == 'one':
          chunk_graph[c1,c2] = 1

        elif weight == 'num':
          chunk_graph[c1,c2] += 1

        else:
          raise RuntimeException('unrecognized: ' + weight)

    if c % 1000 == 0:
      log(str(c))

    c += 1

  log('done building graph')
  log('total initial links %d' % numlinks)

  graph_fh = open(graph_fname, 'w')
  for c1, c2 in chunk_graph:
    print >>graph_fh, '%s\t%s\t%f' % (c1, c2, chunk_graph[c1,c2])

  graph_fh.close()

  log('done writing graph')

def graph_initial_fname(fname, maxlink):

  suffix = maxlink > 0 and '-' + str(maxlink) or ''
  return dirname(fname) + '/../' + basename(fname) + '_graph' + suffix

def graph_output_fname(graph_fname):

  return '%s/out.%s' % (dirname(graph_fname), basename(graph_fname))

def run_cmd(cmd):
  log('cmd: ' + cmd)
  p = Popen(cmd, **dict(stdout=PIPE, stderr=STDOUT, shell=True))
  while True:
    o = p.stdout.read(1)
    if o == '':
      break
    else:
      sys.stderr.write(o)

  p.wait()
  assert p.returncode == 0


def cluster(mcl, graph_in, mcl_i, graph_out):
  'Rum MCL cluster algoitms'

  log('clustering...')
  cmd = '%s %s -I %f --abc -o %s' % (mcl, graph_in, mcl_i, graph_out)
  run_cmd(cmd)
  log('done clustering')

def chunk_tag(i):
  return '__' + str(i)

def create_new_corpus(corpus, chunk_cl):
  '''
  Create a new corpus based on the one passed in, replacing chunks with
  pseudowords from the chunk clustering passed in
  '''

  new_corpus = []
  for sentence in corpus:
    new_sentence = []
    for w in sentence:
      if is_chunk(w):
        if w in chunk_cl:
          chunk_id = chunk_tag(chunk_cl[w])
        else:
          chunk_id = w
        new_sentence.append(chunk_id)
      else:
        new_sentence.append(w)

    new_corpus.append(new_sentence)
  return new_corpus

def read_chunk_clusters(graph_out_fname):

  chunk_cl = dict()
  i = 0
  for line in open(graph_out_fname):
    for ch in line.split():
      chunk_cl[ch] = i
    i += 1

  return chunk_cl

def read_cmd(fname):

  s = ''
  for l in open(fname):
    if l.strip()[0] != '#':
      s +=l
  return s.strip()

def intermediate_chunk(new_corpus, intermediate_fname, interm_out_dirname, \
    upparse_cmd):

  intermediate_out_fh = open(intermediate_fname, 'w')
  log('writing to ' + intermediate_fname)
  for s in new_corpus:
    for w in s:
      print >>intermediate_out_fh, w,
    print >>intermediate_out_fh
  intermediate_out_fh.close()

  log('reading upparse command')
  cmd = read_cmd(upparse_cmd)
  cmd += ' -train %s -output %s' % (intermediate_fname, interm_out_dirname)
  run_cmd(cmd)

def make_word_freq(corpus):
  
  word_freq = defaultdict(zero)
  for s in corpus:
    for w in s:
      word_freq[w] += 1
  
  return word_freq

class WordFreqClusterCl:
  
  def __init__(self, corpus):
    self.word_freq = make_word_freq(corpus)

  def __contains__(self, seq):
    return any(w in self.word_freq for w in seq)

  def __getitem__(self, seq):
    seq = seq[1:-1].split('_')
    w = None
    v = -1
    for _w in seq:
      if self.word_freq[_w] > v:
        v = self.word_freq[_w]
        w = _w
    return w

def main():

  op = OptionParser()

  op.add_option('-i', '--input', default='-')
  op.add_option('-g', '--graph')
  op.add_option('-G', '--graph_output')
  op.add_option('-I', '--mcl_i', default=2.0, type='float')
  op.add_option('-l', '--minlinking', default=2, type='int')
  op.add_option('-L', '--maxlinking', default=-1, type='int')
  op.add_option('-m', '--mcl', default='mcl')
  op.add_option('-c', '--chunk_script')
  op.add_option('-w', '--weight')
  op.add_option('-t', '--intermediate')
  op.add_option('-u', '--upparse_cmd')
  op.add_option('-o', '--final_output')
  op.add_option('-C', '--cheat_cluster', action='store_true')

  opt, args = op.parse_args()

  log('reading chunker output from ' + opt.input)
  corpus, chunks = read_chunker_output(opt.input)

  if opt.cheat_cluster:

    chunk_cl = WordFreqClusterCl(corpus)
    intermediate_fname = opt.intermediate or \
        dirname(opt.input) + '/../cheat_interm.txt'

  else:
    graph_init_fname = opt.graph or graph_initial_fname( \
        opt.input, opt.maxlinking)
    graph_out_fname = opt.graph_output or graph_output_fname(graph_init_fname)

    if exists(graph_out_fname):
      log('%s exists' % graph_out_fname)

    else:
      if exists(graph_init_fname):
        log(' %s exists' % graph_init_fname)

      else:
        log('creating initial graph')
        log('number of chunk types: %d' % len(chunks))

        make_graph(graph_init_fname, chunks, opt.minlinking, opt.maxlinking, \
          opt.weight)

      cluster(opt.mcl, graph_init_fname, opt.mcl_i, graph_out_fname)

    log('reading chunk clusters from MCL output')
    chunk_cl = read_chunk_clusters(graph_out_fname)

    log('creating new corpus with psedowords')
    intermediate_fname = opt.intermediate or graph_init_fname + '.upp.out-01'

  new_corpus = create_new_corpus(corpus, chunk_cl)

  log('creating training for upparse')
  interm_out_dirname = intermediate_fname + '-out'

  if exists(interm_out_dirname):
    log('%s exists' % interm_out_dirname)

  else:
    intermediate_chunk(new_corpus, intermediate_fname, interm_out_dirname, \
      opt.upparse_cmd)

  log('getting the chunker output file')
  chunk_output_files = [x for x in listdir(interm_out_dirname) if x[0] == 'I']
  assert len(chunk_output_files) == 1
  interm_chunk_output = chunk_output_files[0]
  new_chunked_corpus = [l.replace('(', '( ').replace(')',' )').split() \
    for l in open(interm_out_dirname + sep + interm_chunk_output)]

  # print new_chunked_corpus[0]
  # print corpus[0]

  for i in range(len(new_chunked_corpus)):
    new_sent = new_chunked_corpus[i]
    orig_sent = corpus[i]
    w = 0
    for j in range(len(new_sent)):
      if new_sent[j] not in ('(', ')'):
        new_sent[j] = orig_sent[w].replace('_', ' ')
        w += 1

  # print \
     # '(' + \
     # (' '.join(new_chunked_corpus[0])).replace('( ', '(').replace(' )', ')') + \
     # ')'

  log('writing final output to ' + opt.final_output)
  if opt.final_output is None or opt.final_output == '-':
    final_output_fh = sys.stdout
  else:
    final_output_fh = open(opt.final_output, 'w')

  for sent in new_chunked_corpus:
    print >>final_output_fh, mk_parse_str(sent)

  if final_output_fh is not sys.stdout:
    final_output_fh.close()

  log('done')

def is_chunk_tag(w):
  return w[:2] == '__'

def mk_parse_str(sent):
  return '(' + (' '.join(sent).replace('( ','(').replace(' )',')')) + ')'

if __name__ == '__main__':
  main()

