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
from glob import glob

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
    

def guess_input_type(fname):
  if fname.endswith('.mrg'): 
    return 'WSJ'
  
  elif fname.endswith('.penn'):
    return 'NEGRA'

  elif fname.endswith('.fid'):
    return 'CTB'

  else:
    return 'SPL'

def run_cmd(cmd, fh=None, verbose=False):
  if verbose: log('cmd: ' + cmd)
  p = Popen(cmd, **dict(stdout=PIPE, shell=True))
  while True:
    o = p.stdout.read(1)
    if o == '':
      break
    else:
      sys.stdout.write(o)
      if fh is not None:
        fh.write(o)

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
    op.add_option('-p', '--pos', action='store_true')
    op.add_option('-P', '--nopunc', action='store_true')
    op.add_option('-E', '--emdelta', type='float', default=.0001)
    op.add_option('-C', '--cascade', action='store_true')
    op.add_option('-S', '--smooth', type='float', default=.1)
    op.add_option('-I', '--iter', type='int', default=-1)
    op.add_option('-v', '--verbose', action='store_true')
    op.add_option('-O', '--stdout', action='store_true')
    op.add_option('-x', '--output_type', default='CLUMP')
    op.add_option('-N', '--numtrain', type='int', default=-1)
    op.add_option('-A', '--output_all', action='store_true')
    op.add_option('-X', '--continuous_eval', action='store_true')
  
    opt, args = op.parse_args()

    self.opt = opt

    self._input_type = None

  def verbose(self):
    return self.opt.verbose

  def stdout(self):
    return self.opt.stdout

  def cascade(self):
    return self.opt.cascade

  def output(self):
    return self.opt.output

  def output_type(self):
    return self.opt.output_type

  def set_output(self, outp):
    self.opt.output = outp

  def input_type(self):
    if self._input_type is None:
      input_type = self.opt.input_type \
        or guess_input_type(self._get_test_str())
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
          answer = raw_input("Overwrite diretory '" \
                             + opt.output + "'? [y/n] ").strip()
          if answer not in yn:
            print "Answer 'y' or 'n'"
  
        if answer == 'n':
          sys.exit(0)
  
        else:
          rmtree(opt.output)

  def numtrain_flag(self):
    n = self.opt.numtrain
    return n >= 0 and (' -numtrain %d' % n) or ''

  def filter_flag(self):
    n = self.opt.filter_test
    return n >= 0 and (' -filterTest %d' % n) or ''
  
  def seg_flag(self):
    return self.opt.nopunc and ' -noSeg ' or ''

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

    if opt.iter >= 0:
      model_flag += ' -iterations %d' % opt.iter

    return model_flag

  def coding_flag(self):
    opt = self.opt
    assert opt.coding in ['BIO','BILO','BIO_GP','BIO_GP_NOSTOP','BEO']
    coding_flag = ' -G ' + opt.coding
    return coding_flag

  def java_cmd(self):
    return 'java -ea ' + self.opt.memflag + ' -jar upparse.jar'

  def chunk_cmd(self):
    return self.java_cmd() + ' chunk'

  def eval_cmd(self):
    return self.java_cmd() + ' cclp-eval -E PRCL '

  def emdelta_flag(self):
    if self.opt.iter >= 0:
      return ' -emdelta 1E-100'
    else:
      return ' -emdelta %f' % self.opt.emdelta

  def smooth_flag(self):
    return ' -smooth %f' % self.opt.smooth

  def reverse_flag(self):
    return self.opt.reverse and ' -reverse' or ''

  def pos_flag(self):
    return self.opt.pos and ' -outputPos' or ''

  def output_all_flag(self):
    return self.opt.output_all and ' -outputAll' or ''

  def continuous_eval_flag(self):
    return self.opt.continuous_eval and ' -continuousEval' or ''

  def basic_cmd(self):
    cmd = self.chunk_cmd()
    cmd += self.model_flag()
    cmd += self.coding_flag()
    cmd += self.seg_flag()
    cmd += self.output_all_flag()
    cmd += self.continuous_eval_flag()
    cmd += self.emdelta_flag()
    cmd += self.smooth_flag()
    cmd += self.reverse_flag()
    cmd += self.pos_flag()
    cmd += self.numtrain_flag()
    return cmd

  def _get_train_str(self):
    return self._get_glob_expanded(self.opt.train)

  def _get_test_str(self):
    if self.opt.test is None:
      self.opt.test = self.opt.train

    return self._get_glob_expanded(self.opt.test)

  def _get_glob_expanded(self, fname_glob):
    fnames = []
    for g in fname_glob.split():
      fnames.extend(glob(g))
    fnames.sort()
    return ' '.join(fnames)

  def starter_train(self):
    cmd = ' -train ' + self._get_train_str()
    cmd += ' -trainFileType ' + self.input_type()
    return cmd

  def starter_test(self):
    cmd = ' -test ' + self._get_test_str()
    cmd += ' -testFileType ' + self.input_type()
    return cmd

  def starter_train_out(self):
    cmd = ' -test ' + self._get_train_str()
    cmd += ' -testFileType ' + self.input_type()
    return cmd

def get_output_fname(output_dir):
  return output_dir + '/OUTPUT'
   
def main():

  opt_h = OptionHelper()

  if opt_h.cascade():
    input_type = opt_h.input_type()

    if opt_h.output() is None:
      opt_h.set_output('out')
    opt_h.check_output()
    cascade_dir = '%s/cascade00' % opt_h.output()
    makedirs(cascade_dir)
    results_fh = open('%s/results' % opt_h.output(), 'w')
    cascade_train_out = '%s/train-out' % cascade_dir
    cascade_test_out = '%s/test-out' % cascade_dir

    basic_cmd = opt_h.basic_cmd()
    output_file_type = ' -outputType UNDERSCORE4CCL'

    log('running initial chunking')
    run_cmd(basic_cmd \
            + opt_h.starter_train() \
            + opt_h.starter_train_out() \
            + output_file_type \
            + ' -output ' + cascade_train_out, \
            verbose=opt_h.verbose())

    run_cmd(basic_cmd \
            + opt_h.starter_train() \
            + opt_h.starter_test() \
            + opt_h.filter_flag() \
            + output_file_type \
            + ' -output ' + cascade_test_out, \
            verbose=opt_h.verbose())

    cascade_iter = 1

    new_cascade_train_out_fname = get_output_fname(cascade_train_out)
    cascade_expand_last = None
    while True:

      # convert test output to trees
      cascade_test_out_fname = get_output_fname(cascade_test_out)
      cascade_expand = []
      log('building corpus record from ' + cascade_test_out)
      for s_ind, sentence in enumerate(open(cascade_test_out_fname)):
        i = 0
        sentence_str = []
        for chunk in sentence.split():
          chunk = chunk.split('_')
          chunk_str = []
          for word in chunk:
            if word.startswith('=') and len(word) > 1:
              chunk_str.append(cascade_expand_last[s_ind][i])
            else:
              chunk_str.append(word)

            i += 1

          if len(chunk) == 1:
            sentence_str.append(chunk_str[0])

          else:
            sentence_str.append('(' + (' '.join(chunk_str)) + ')')

        cascade_expand.append(sentence_str)

      cascade_test_eval_fname = cascade_dir + '/test-eval'
      eval_fh = open(cascade_test_eval_fname, 'w')
      for sent in cascade_expand:
        print >>eval_fh, '(' + (' '.join(sent)).replace(' ;', '') + ')'
      eval_fh.close()

      # evaluate test output as trees

      run_cmd(opt_h.eval_cmd() \
              + opt_h.starter_test() \
              + ' -cclpOutput ' + cascade_test_eval_fname \
              + opt_h.filter_flag(), fh=results_fh, \
              verbose=opt_h.verbose())


      cascade_expand_last = cascade_expand

      log('running cascade level ' + str(cascade_iter))

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
              + ' -output ' + cascade_train_out,
              verbose=opt_h.verbose())

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
              + ' -output ' + cascade_test_out,
              verbose=opt_h.verbose())

      cascade_dir = new_cascade_dir
      cascade_iter += 1

    results_fh.close()

  else:
    cmd = opt_h.basic_cmd()

    output_flag = ''
    if opt_h.stdout():
      output_flag = ' -output -'

    elif opt_h.output() is not None:
      opt_h.check_output()
      output_flag = ' -output ' + opt_h.output()

    cmd += ' -outputType ' + opt_h.output_type()
 
    cmd += output_flag
    cmd += opt_h.starter_train()
    cmd += opt_h.starter_test()
    cmd += opt_h.filter_flag()

    cmd += ' -E PRCL -e CLUMP,NPS,TREEBANKPREC'
    run_cmd(cmd, verbose=opt_h.verbose())

if __name__ == '__main__':
  main()
