#!/usr/bin/env python

'''
Wrapper around upparse.jar chunk
'''

import sys
from os.path import dirname, basename, exists
from os import makedirs, listdir, sep
from shutil import rmtree
from optparse import OptionParser
from subprocess import Popen, PIPE, STDOUT
from collections import defaultdict
from filecmp import cmp as filecmp

class PhrasalTerms:

  def __init__(self, output_fname):
    d = defaultdict(lambda:0)
    for line in open(output_fname):
      for chunk in line.split():
        for word in chunk.split('_'):
          d[word] += 1
    self._dict = d

  def term(self, chunk):
    words = chunk.split('_')
    if len(words) == 1:
      return words[0]

    elif len(words) > 1:
      return '=' + self._argmax(words)

    else:
      raise RuntimeError('unexpected number of terms ' + str(words))

  def write_new_dataset(self, in_fname, out_fname):
    out_fh = open(out_fname, 'w')
    for sentence in open(in_fname):
      for chunk in sentence.split():
        print >>out_fh, self.term(chunk),
      print >>out_fh
    out_fh.close()

  def _argmax(self, terms):
    maxval = 0
    argmax = ''
    for term in terms:
      val = self._dict[term]
      if val >= maxval:
        maxval, argmax = val, term
    return argmax
    

# TODO try to import this function from ps_wrd_up
def guess_input_type(fname):
  if fname.endswith('.mrg'): 
    return 'WSJ'
  
  elif fname.endswith('.penn'):
    return 'NEGRA'

  elif fname.endswith('.fid'):
    return 'CTB'

  else:
    return 'SPL'

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

def log(st):
  print >>sys.stderr, st

class OptionHelper:
  
  def __init__(self):
    op = OptionParser()
  
    op.add_option('-t', '--train')
    op.add_option('-T', '--input_type')
    op.add_option('-s', '--test')
    op.add_option('-o', '--output')
    op.add_option('-r', '--reverse', action='store_true')
    op.add_option('-f', '--filter_test', type='int', default='-1')
    op.add_option('-m', '--model', default='prlg-uni')
    op.add_option('-M', '--memflag', default='-Xmx1g')
    op.add_option('-c', '--coding', default='BIO')
    op.add_option('-P', '--nopunc', action='store_true')
    op.add_option('-E', '--emdelta', type='float', default=.0001)
    op.add_option('-C', '--cascade', action='store_true')
  
    opt, args = op.parse_args()

    self.opt = opt

    self._input_type = None

  def cascade(self):
    return self.opt.cascade

  def output(self):
    return self.opt.output

  def set_output(self, outp):
    self.opt.output = outp

  def input_type(self):
    if self._input_type is None:
      input_type = self.opt.input_type or guess_input_type(self.opt.test)
      log('guessing input type = ' + input_type)
      self._input_type = input_type
    return self._input_type

  def check_output(self):
    opt = self.opt
    if opt.output is not None:
      if exists(opt.output):
        answer = 'x'
        yn = ['y','n']
        while answer not in yn:
          answer = raw_input('Overwrite diretory ' + opt.output + '? [y/n] ').strip()
          if answer not in yn:
            print "Answer 'y' or 'n'"
  
        if answer == 'n':
          sys.exit(0)
  
        else:
          rmtree(opt.output)

  def filter_flag(self):
    opt = self.opt
    filter_flag = ''
    if opt.filter_test > 0: 
      filter_flag = ' -filterTest %d ' % opt.filter_test
    return filter_flag
  
  def seg_flag(self):
    opt = self.opt
    seg_flag = ''
    if opt.nopunc:
      seg_flag = ' -noSeg '
    return seg_flag

  def model_flag(self):
    opt = self.opt
    if opt.model == 'prlg-uni':
      model_flag = ' -chunkerType PRLG -chunkingStrategy UNIFORM '
    elif opt.model == 'hmm-uni':
      model_flag = ' -chunkerType HMM -chunkingStrategy UNIFORM '
    elif opt.model == 'prlg-2st':
      model_flag = ' -chunkerType PRLG -chunkingStrategy TWOSTAGE -F 2 '
    elif opt.model == 'hmm-2st':
      model_flag = ' -chunkerType HMM -chunkingStrategy TWOSTAGE -F 2 '
    elif opt.model == 'prlg-sup-clump':
      model_flag = ' -chunkerType PRLG -chunkingStrategy SUPERVISED_CLUMP -iterations 0 '
    elif opt.model == 'hmm-sup-clump':
      model_flag = ' -chunkerType HMM -chunkingStrategy SUPERVISED_CLUMP -iterations 0 '
    elif opt.model == 'prlg-sup-nps':
      model_flag = ' -chunkerType PRLG -chunkingStrategy SUPERVISED_NPS -iterations 0 '
    elif opt.model == 'hmm-sup-nps':
      model_flag = ' -chunkerType HMM -chunkingStrategy SUPERVISED_NPS -iterations 0 '
    else:
      print >>sys.stderr, 'Unexpected model option:', opt.model
      sys.exit(1)

    return model_flag

  def coding_flag(self):
    opt = self.opt
    assert opt.coding in ['BIO','BILO','BIO_GP','BIO_GP_NOSTOP']
    coding_flag = ' -G ' + opt.coding
    return coding_flag

  def java_cmd(self):
    return 'java -ea ' + self.opt.memflag + ' -jar upparse.jar chunk'

  def emdelta_flag(self):
    return ' -emdelta %f' % self.opt.emdelta

  def smooth_flag(self):
    return ' -smooth .1'

  def reverse_flag(self):
    return self.opt.reverse and ' -reverse' or ''

  def basic_cmd(self):
    cmd = self.java_cmd()
    cmd += self.model_flag()
    cmd += self.coding_flag()
    cmd += self.seg_flag()
    cmd += self.emdelta_flag()
    cmd += self.smooth_flag()
    cmd += self.reverse_flag()
    return cmd

  def starter_train(self):
    opt = self.opt
    cmd = ' -train ' + opt.train
    cmd += ' -trainFileType ' + self.input_type()
    return cmd

  def starter_test(self):
    opt = self.opt
    cmd = ' -test ' + opt.test
    cmd += ' -testFileType ' + self.input_type()
    return cmd

  def starter_train_out(self):
    opt = self.opt
    cmd = ' -test ' + opt.train
    cmd += ' -testFileType ' + self.input_type()
    return cmd

def get_output_fname(output_dir):
  files = [f for f in listdir(output_dir) if f.startswith('Iter')]
  assert len(files) == 1
  return output_dir + sep + files[0]
   
def main():

  opt_h = OptionHelper()

  if opt_h.cascade():
    input_type = opt_h.input_type()

    if opt_h.output() is None:
      opt_h.set_output('out')
    opt_h.check_output()
    cascade_dir = '%s/cascade00' % opt_h.output()
    makedirs(cascade_dir)
    cascade_train_out = '%s/train-out' % cascade_dir
    cascade_test_out = '%s/test-out' % cascade_dir

    basic_cmd = opt_h.basic_cmd()
    output_file_type = ' -outputType UNDERSCORE4CCL'

    log('running initial chunking')
    run_cmd(basic_cmd \
            + opt_h.starter_train() \
            + opt_h.starter_train_out() \
            + output_file_type \
            + ' -output ' + cascade_train_out)

    run_cmd(basic_cmd \
            + opt_h.starter_train() \
            + opt_h.starter_test() \
            + opt_h.filter_flag() \
            + output_file_type \
            + ' -output ' + cascade_test_out)

    cascade_iter = 1

    new_cascade_train_out_fname = get_output_fname(cascade_train_out)
    while True:

      # convert test output to trees
      cascade_test_out_fname = get_output_fname(cascade_train_out)

      # evaluate test output as trees

      # build term frequency map from last train output
      cascade_train_out_fname = new_cascade_train_out_fname
      phrasal_terms = PhrasalTerms(cascade_train_out_fname)

      # create next-run train
      next_run_train_fname = cascade_dir + '/next-train'
      phrasal_terms.write_new_dataset(cascade_train_out_fname, \
                                      next_run_train_fname)

      # run chunker, output re-chunked train
      new_cascade_dir = '%s/cascade%02d' % (opt_h.output(), cascade_iter)
      makedirs(new_cascade_dir)
      cascade_train_out = '%s/train-out' % new_cascade_dir
      run_cmd(basic_cmd \
              + ' -train ' + next_run_train_fname \
              + ' -trainFileType SPL ' \
              + ' -test ' + next_run_train_fname \
              + ' -testFileType SPL ' \
              + output_file_type \
              + ' -output ' + cascade_train_out)

      # if re-chunked train is the same as orig, break
      new_cascade_train_out_fname = get_output_fname(cascade_train_out)
      if filecmp(cascade_train_out_fname, new_cascade_train_out_fname): 
        break

      # create next-run test
      cascade_test_out = '%s/test-out' % new_cascade_dir
      next_run_test_fname = cascade_dir + '/next-test'
      phrasal_terms.write_new_dataset(cascade_test_out_fname, \
                                      next_run_test_fname)

      # run the chunker, output re-chunked test

      run_cmd(basic_cmd \
              + ' -train ' + next_run_train_fname \
              + ' -trainFileType SPL ' \
              + ' -test ' + next_run_test_fname \
              + ' -testFileType SPL ' \
              + output_file_type \
              + ' -output ' + cascade_test_out)

      cascade_dir = new_cascade_dir
      cascade_iter += 1

  else:
    cmd = opt_h.basic_cmd()

    output_flag = ''
    if opt_h.output() is not None:
      opt_h.check_output()
      output_flag = ' -output ' + opt_h.output()
 
    cmd += output_flag
    cmd += opt_h.starter_train()
    cmd += opt_h.starter_test()
    cmd += opt_h.filter_flag()

    cmd += ' -E PRCL -e CLUMP,NPS'
    run_cmd(cmd)

if __name__ == '__main__':
  main()
