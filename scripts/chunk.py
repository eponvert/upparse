#!/usr/bin/env python

'''
Wrapper around upparse.jar chunk
'''

import sys
from os.path import dirname, basename, exists
from shutil import rmtree
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

  op.add_option('-t', '--train')
  op.add_option('-T', '--input_type')
  op.add_option('-s', '--test')
  op.add_option('-o', '--output')
  op.add_option('-r', '--reverse', action='store_true')
  op.add_option('-f', '--filter_test', type='int', default='-1')
  op.add_option('-M', '--memflag', default='-Xmx1g')

  opt, args = op.parse_args()

  input_type = opt.input_type or guess_input_type(opt.test)
  log('guessing input type = ' + input_type)
  log('running initial chunking')

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

  filter_flag = ''
  if opt.filter_test > 0: 
    filter_flag = ' -filterTest %d ' % opt.filter_test

  cmd = 'java -ea ' + opt.memflag + ' -jar upparse.jar chunk'
  cmd += ' -chunkingStrategy UNIFORM '
  cmd += ' -chunkerType PRLG '
  cmd += ' -emdelta .0001 '
  cmd += ' -smooth .1 '

  cmd += ' -train ' + opt.train
  cmd += ' -trainFileType ' + input_type
  cmd += ' -test ' + opt.test
  cmd += ' -testFileType ' + input_type
  cmd += ' -output ' + opt.output
  cmd += ' -E PRCL -e CLUMP,NPS'
  if opt.reverse:
    cmd += ' -reverse '
  cmd += filter_flag
  run_cmd(cmd)

if __name__ == '__main__':
  main()
