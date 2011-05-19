#!/usr/bin/env python

'''
Wrapper around upparse.jar cclp-eval
'''

import sys
from os.path import dirname, basename, exists
from optparse import OptionParser
from subprocess import Popen, PIPE, STDOUT
from glob import glob

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

def main():

  op = OptionParser()

  op.add_option('-T', '--input_type')
  op.add_option('-s', '--test')
  op.add_option('-o', '--output')
  op.add_option('-f', '--filter_test', type='int', default='-1')
  op.add_option('-M', '--memflag', default='-Xmx1g')
  op.add_option('-v', '--verbose', action='store_true')

  opt, args = op.parse_args()

  tst = []
  for g in opt.test.split():
    tst.extend(glob(g))
  tst.sort()
  tst = ' '.join(tst)

  input_type = opt.input_type or guess_input_type(tst)
  log('guessing input type = ' + input_type)
  log('running initial chunking')

  filter_flag = ''
  if opt.filter_test > 0: 
    filter_flag = ' -filterTest %d ' % opt.filter_test

  cmd = 'java -ea ' + opt.memflag + ' -jar upparse.jar cclp-eval'
  cmd += ' -test ' + tst
  cmd += ' -testFileType ' + input_type
  cmd += ' -cclpOutput ' + opt.output
  cmd += filter_flag
  if opt.verbose:
    log('cmd: ' + cmd)
  run_cmd(cmd)

if __name__ == '__main__':
  main()
