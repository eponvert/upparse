#!/usr/bin/env python

'''
This script takes chunked sentence-per-line input from an unsupervised parsing
program, makes pseudowords based on term frequency and creates a new dataset to 
chunk.  This allows the chunking system to be extended to "full" parsing 
out-of-the-box.
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

def read_chunker_output(inp):
  'Create chunk symbols from chunker output'

  fh_in = open(inp)
  corpus = []

  try:
    for line in fh_in:
      sentence = []
      in_chunk = False
      for w in chunk_line_split(line):
        if in_chunk:
          if w == ')':
            chunk = intern('(' + ('_'.join(curr_chunk)) + ')')
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

  finally:
    fh_in.close()

  return corpus

def is_chunk(w):

  return w.startswith('(')

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

def chunk_tag(i):
  return '__' + str(i)

def is_chunk_tag(w):
  return w[:2] == '__'

def mk_parse_str(sent):
  return '(' + (' '.join(sent).replace('( ','(').replace(' )',')')) + ')'

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

def read_cmd(fname):

  s = ''
  for l in open(fname):
    if l.strip()[0] != '#':
      s +=l
  return s.replace('\\', '').replace('\n',' ').strip()

# TODO
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
    return any(w in self.word_freq for w in seq[1:-1].split('_'))

  def __getitem__(self, seq):
    seq = seq[1:-1].split('_')
    w = None
    v = -1
    for _w in seq:
      if self.word_freq[_w] > v:
        v = self.word_freq[_w]
        w = _w
    return w

def guess_input_type(fname):
  if fname.endswith('.mrg'): 
    return 'WSJ'
  
  elif fname.endswith('.penn'):
    return 'PENN'

  elif fname.endswith('.fid'):
    return 'CTB'

  else:
    return 'SPL'

def get_output_fname(output_dir):
  files = [x for x in listdir(output_dir) if x[0] == 'I']
  assert len(files) == 1
  return output_dir + '/' + files[0]

def chunk_line_split(line):
  return line.replace('(','( ').replace(')',' )').split()

def underlying_text_corpus(corpus_fname):
  
  sentences = []
  for line in open(corpus_fname):
    sentence = []
    for w in chunk_line_split(line):
      if w not in ('(', ')'):
        sentence.append(w)
    sentences.append(sentence)
  return sentences

def main():

  op = OptionParser()

  op.add_option('-t', '--train')
  op.add_option('-T', '--input_type')
  op.add_option('-s', '--test')
  op.add_option('-o', '--output')
  op.add_option('-u', '--upparse_script')

  opt, args = op.parse_args()

  input_type = opt.input_type or guess_input_type(opt.train)
  log('guessing input type = ' + input_type)
  log('running initial chunking')

  base_cmd = read_cmd(opt.upparse_script)

  init_train_output = opt.output + '-train-01'
  if exists(init_train_output):
    log('Initial train output %s exists' % init_train_output)

  else:
    log('chunking to create next level training')
    cmd = base_cmd
    cmd += ' -train ' + opt.train
    cmd += ' -trainFileType ' + input_type
    cmd += ' -test ' + opt.train
    cmd += ' -testFileType ' + input_type
    cmd += ' -output ' + init_train_output

    run_cmd(cmd)

  train_output_fname = get_output_fname(init_train_output)

  init_eval_output = opt.output + '-eval-01'

  if exists(init_eval_output):
    log('Initial eval output %s exists' % init_eval_output)

  else:
    log('chunking to create next level eval')
    cmd = base_cmd
    cmd += ' -train ' + opt.train
    cmd += ' -trainFileType ' + input_type
    cmd += ' -test ' + opt.test
    cmd += ' -testFileType ' + input_type
    cmd += ' -output ' + init_eval_output

    run_cmd(cmd)

  eval_output_fname = get_output_fname(init_eval_output)

  log('building word frequencies')
  chunk_cl = WordFreqClusterCl(underlying_text_corpus(train_output_fname))

  log('reading in initial chunker output')
  train_corpus_orig = read_chunker_output(train_output_fname)
  eval_corpus_orig = read_chunker_output(eval_output_fname)
 
  log('creating corpora with pseudowords')
  train_corpus_new = create_new_corpus(train_corpus_orig, chunk_cl)
  eval_corpus_new = create_new_corpus(eval_corpus_orig, chunk_cl)

  log('writing new corpora with pseudowords to disk')
  new_train_fname = init_train_output + '-pw'

  log('writing to ' + new_train_fname)
  new_train_fh = open(new_train_fname, 'w')
  for sent in train_corpus_new:
    print >>new_train_fh, ' '.join(sent)
  new_train_fh.close()

  new_eval_fname = init_eval_output + '-pw'

  log('writing to ' + new_eval_fname)
  new_eval_fh = open(new_eval_fname, 'w')
  for sent in eval_corpus_new:
    print >>new_eval_fh, ' '.join(sent)
  new_eval_fh.close()

  log('chunking level 2')
  sec_train_output = opt.output + '-train-02'
  sec_eval_output = opt.output + '-eval-02'

  if exists(sec_train_output):
    log('%s exists' % sec_train_output)

  else:
    cmd = base_cmd
    cmd += ' -train ' + new_train_fname
    cmd += ' -trainFileType SPL'
    cmd += ' -test ' + new_train_fname
    cmd += ' -testFileType SPL'
    cmd += ' -output ' + sec_train_output

    run_cmd(cmd)

  if exists(sec_eval_output):
    log('%s exists' % sec_eval_output)

  else:
    cmd = base_cmd
    cmd += ' -train ' + new_train_fname
    cmd += ' -trainFileType SPL'
    cmd += ' -test ' + new_eval_fname
    cmd += ' -testFileType SPL'
    cmd += ' -output ' + sec_eval_output

    run_cmd(cmd)



  sys.exit(0)

  if opt.cheat_cluster:

    chunk_cl = WordFreqClusterCl(corpus)
    intermediate_fname = opt.intermediate or \
        dirname(opt.input) + '/../cheat_interm.txt'

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

if __name__ == '__main__':
  main()

