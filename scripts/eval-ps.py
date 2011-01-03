#!/usr/bin/env python

'''
Wrapper around upparse.jar cclp-eval
'''

import sys
from os.path import dirname, basename, exists
from optparse import OptionParser
from subprocess import Popen, PIPE, STDOUT

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

def main():

  op = OptionParser()

  op.add_option('-T', '--input_type')
  op.add_option('-s', '--test')
  op.add_option('-o', '--output')
  op.add_option('-f', '--filter_test', type='int', default='-1')
  op.add_option('-M', '--memflag', default='-Xmx1g')

  opt, args = op.parse_args()

  input_type = opt.input_type or guess_input_type(opt.test)
  log('guessing input type = ' + input_type)
  log('running initial chunking')

  filter_flag = ''
  if opt.filter_test > 0: 
    filter_flag = ' -filterTest %d ' % opt.filter_test

  cmd = 'java ' + opt.memflag + ' -jar upparse.jar cclp-eval'
  cmd += ' -test ' + opt.test
  cmd += ' -testFileType ' + input_type
  cmd += ' -cclpOutput ' + opt.output
  cmd += filter_flag
  run_cmd(cmd)

if __name__ == '__main__':
  main()
