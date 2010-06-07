#!/usr/bin/env python

# gala_util.py -- Tools for running experiments using the Gala model
# Copyright (C) 2010 Elias Ponvert
#
# This program is free software: you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation, either version 3 of the License, or (at your option) any later
# version.
#
# This program is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
# PARTICULAR PURPOSE.  See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along with
# this program.  If not, see <http://www.gnu.org/licenses/>.

'''
Tools for running experiments using the Gala model
'''

__author__ = 'Elias Ponvert'
__email__ = 'ponvert@gmail.com'
__version__ = '0.1'
__license__ = 'GPL'

import sys
import csv
from os import listdir
from os.path import basename
from optparse import OptionParser
from collections import defaultdict
from array import array
from nltk import Tree
from nltk.corpus.reader import BracketParseCorpusReader as corpus_reader
from cPickle import load, dump, UnpicklingError
from itertools import izip, count as icount, chain, imap, starmap
from operator import methodcaller
from math import sqrt
from logging import info

# Includes Chinese punctuation
STOPPING_PUNC = ['.', '?', '!', ';', ',', '--', 
                 '\xe3\x80\x81', '\xef\xbc\x8c', '\xe3\x80\x82']
#WSJ_RM_POS = ['-NONE-', ',', '.', ':', '``', "''", '-LRB-', '-RRB-']
WSJ_RM_POS = ['-NONE-', ',', '.', ':', '``', "''", '-LRB-', '-RRB-', '$', '#']
WSJ_PUNC_POS = [',', '.', ':', '``', "''", '-LRB-', '-RRB-']
NEGRA_RM_POS = ['$,','$.','$.-CD','$.-CJ','$*LRB*','$*LRB*-NMC',
                  '$*LRB*-PNC','$*LRB*-UC','$.-NK','$,-NMC','$.-NMC',
                  '$.-PNC','$.-UC', '--']
NEGRA_PUNC_POS = NEGRA_RM_POS
CTB_RM_POS = ['-NONE-', 'PU']
CTB_PUNC_POS = ['PU']
COUNT_IGNORE = ['(', ')', 'nz$','$','m$','#','us$','s$','a$','hk$','c$']

def brak_from_tup(tup): return Bracket(tup[0], tup[1])

def basic_stats(corpus):
  num_s = len(corpus)
  num_w = sum(map(len, corpus))
  num_t = len(set(chain.from_iterable(corpus)))
  return 'sentences: %d tokens: %d types: %d' % (num_s, num_w, num_t)

class Bracket:
  """
  Data structure for a bracket, or constituent, in a syntax tree
  """
  
  def __init__(self,first_index,last_index,label=None,is_pos=False):
    self.label = label
    self.ind = [first_index,last_index]

    if is_pos:
      self.__z = -1
      
    else:
      self.__z = 0
    
  def __repr__(self):
    if self.label:
      return "Bracket(%d,%d,%s)" % (self.ind[0], self.ind[1], self.label)
    else:
      return "Bracket(%d,%d)" % (self.ind[0], self.ind[1])
  
  def __str__(self):
    return repr(self)

  def __cmp__(self, other):
    
    if self.ind[0] == other.ind[0] and self.ind[1] == other.ind[1]:
      return self.__z.__cmp__(other.__z)
    elif self.ind[0] >= other.ind[0] and self.ind[1] <= other.ind[1]:
      return -1
    elif self.ind[0] <= other.ind[0] and self.ind[1] >= other.ind[1]:
      return 1
    else:
      print self, other
      raise CrossingBracketsError

  def __eq__(self, other):
    
    return self.ind[0] == other.ind[0] and \
      self.ind[1] == other.ind[1] and \
      self.label == other.label

  def contains(self, index):
    """
    @return C{True} if C{index} falls within the span of this bracket
    """
    
    return self.ind[0] <= index <= self.ind[1]

  def indices(self):
    return (self.ind[0], self.ind[1])

class Bracketing:
  
  def __init__(self,terms,first_brackets=None):

    self.terms = terms
    n = len(terms)
    self.__opening_brackets = [BrakList(True) for i in xrange(n)]
    self.__closing_brackets = [BrakList(True) for i in xrange(n)]
    self.__all_brackets = []
    self.brackets_which_cover = [BrakList() for i in xrange(n)]

    if first_brackets:
      for bracket in first_brackets:
        self.add_bracket(bracket)
        
  def already_contains(self, brak):

    return any(k == brak for k in self.brackets_which_cover[brak.ind[0]])

  def __str__(self):
    
    s = StringIO()
    n = len(self.terms)
    last = n - 1
    for i in xrange(n):
      for brak in self.__opening_brackets[i]:
        s.write('(')
        if brak.label:
          s.write(brak.label)
          s.write(' ')
      s.write(self.terms[i])
      for j in xrange(len(self.__closing_brackets[i])):
        s.write(')')
      if i < last:
        s.write(' ')
      
    return s.getvalue()

  def __len__(self):

    return len(self.terms)
  
  def lowest_bracket_covers(self, i, k):
    '''
    Returns true if a) there is an bracketing covering i and
    b) the lowest bracketing covering i also covers k
    '''
    
    i_braks = self.brackets_which_cover[i]
    return len(i_braks) and i_braks[0].contains(k)
    

  def extend_to_cover(self, bracket, i):
    """
    Extend C{bracket} to cover the word at index C{i}
    """
    if i < bracket.ind[0]:
      orig_left = bracket.ind[0]
      bracket.ind[0] = i
      self.__opening_brackets[orig_left].remove(bracket)
      self.__opening_brackets[i].append(bracket)
      for x in xrange(i,orig_left):
        self.brackets_which_cover[x].append(bracket)
    
    elif i > bracket.ind[1]:
      orig_right = bracket.ind[1]
      bracket.ind[1] = i
      self.__closing_brackets[orig_right].remove(bracket)
      self.__closing_brackets[i].append(bracket)
      for x in xrange(orig_right+1,i+1):
        self.brackets_which_cover[x].append(bracket)
    
  def add_bracket(self,bracket):
    
    if len(self.__opening_brackets) == 0:
      return

    if not self.already_contains(bracket):
      self.__opening_brackets[bracket.ind[0]].append(bracket)
      self.__closing_brackets[bracket.ind[1]].append(bracket)
      self.__all_brackets.append(bracket)
      
      #for i in xrange(bracket.ind[0], bracket.ind[1]+1):
      for i in xrange(bracket.ind[1], bracket.ind[0]-1, -1):
        self.brackets_which_cover[i].append(bracket)

  def indices_set(self, rm_trivial=False):
    '''
    Returns a set of all (i,j) indices for brackets in the bracketing
    '''

    filt = lambda b:True

    if rm_trivial:
      filt = lambda b:b.ind[1] - b.ind[0] and tuple(b.ind) != (0,len(self)-1)

    ind = [b.indices() for b in self.__all_brackets if filt(b)]

    return set(ind)

def make_corpus(fh, alpha):
  '''
  Using the supplied alphabet data-structure, read in a sentence-per-line
  corpus from the supplied file handle and place arrays of term-indices in
  memory.
  '''

  info('building corpus from %s' % fh.name)

  assert alpha != None
  a = alpha.__getitem__
  corpus = [array('i', imap(a, s.split())) for s in fh]

  info('done building corpus')
  info(basic_stats(corpus))

  return corpus

class Alpha:
  '''
  Alphabet data structure: transforms string terms back-and-forth into term
  indices. Involves a bit of syntactic sugar, as illustrated.

  Example
  -------
  >>> a = Alpha()
  >>> k = a['kitten']
  >>> assert k == 0
  >>> j = a['puppy']
  >>> assert j == 1
  >>> assert a(k) == 'kitten'
  >>> assert a(j) == 'puppy'
  '''

  def __init__(self,size=0):

    self.stoi = {}
    self.itos = []

  def __call__(self, i):
    assert i < len(self.itos)
    return self.itos[i]

  def __getitem__(self, w):

    if self.contains(w):
      val = self.stoi[w]

    else:
      val = len(self.itos)
      self.stoi[w] = val
      self.itos.append(w)

    return val

  def __len__(self): return len(self.itos)

  def __repr__(self): return 'Alpha(size=%d)' % len(self)

  def __le__(self, other):
    assert other.__class__ == self.__class__
    n = len(self)
    return n <= len(other) and self.itos == other.itos[:n]

  def contains(self, w): return self.stoi.has_key(w)

def str_remove_ignore(s, stop='__stop__'):
  # dumb hack to avoid killing personal pronouns in pos output
  s = s.replace('prp$', 'prp__')
  for t in COUNT_IGNORE + [stop]: s = s.replace(t,'')
  s = s.replace('prp__', 'prp$')
  return s

def split_separate_setences(s):
  sentences = []
  num_parens = 0
  curr_index = -1
  s = s.strip()

  for ch in s:
    if ch == '(':
      if num_parens == 0:
        sentences.append([])
        curr_index += 1

      num_parens += 1

    elif ch == ')':
      num_parens -= 1
    
    sentences[curr_index].append(ch)

  for i in xrange(len(sentences)):
    sentences[i] = ''.join(sentences[i])

  return sentences

def pprint_nonodes(tree, out):
  if type(tree) == str or type(tree) == unicode:
    out.write(tree)
    return

  out.write('(')

  pprint_nonodes(tree[0], out)
  for t in tree[1:]:
    out.write(' ')
    pprint_nonodes(t, out)

  out.write(')')

class Corpus:

  def __init__(self, tree_iter=None, punc=None, alpha=None, filt=None,
         lex_filt=lambda x:x.lower(), msg_out=None, stop_sym='__stop__',
         use_pos=False):

    self.corpus = []
    self.corpus_punc = []
    self.stop_sym = stop_sym
    self.alpha = alpha
    self.lex_filt = lex_filt
    self.use_pos = use_pos

    if tree_iter and file and punc and filt:
      self.populate_from_treebank(\
        tree_iter, filt, punc, lex_filt=lex_filt, msg_out=msg_out)

  def __getitem__(self, i):

    return self.corpus[i]

  def __len__(self):

    return len(self.corpus)

  def __iter__(self):

    for i, s in enumerate(self.corpus):

      if self.stop_sym:
        items = [self.stop_sym]
        for j, word in enumerate(map(self.alpha, s)):
          if j in self.corpus_punc[i]:
            items.append(self.stop_sym)
          items.append(word)
        items.append(self.stop_sym)
        yield ' '.join(items)

      else:
        yield ' '.join(map(self.alpha, s))

  def populate_from_treebank(self, tree_iter, filt, punc, lex_filt=None,
      msg_out=None):

    wp_index = self.use_pos and 1 or 0

    if not self.alpha:
      self.alpha = Alpha()

    for n, tree in enumerate(tree_iter):
      sent_lst = []
      punc_lst = []
      i = 0
      tree_pos = tree.pos()
      for wp in tree_pos:
        if wp[0] in STOPPING_PUNC:
          punc_lst.append(i)

        elif not (punc(*wp) or filt(*wp)):
          w = self.alpha[self.lex_filt(wp[wp_index])]
          sent_lst.append(w)
          i += 1

      if len(sent_lst) == 0 and False:
        sent_lst = [self.alpha[self.lex_filt(wp[0])] for wp in tree.pos()]
        if len(sent_lst) > 3:
          print >>sys.stderr, 'sentence of len', len(sent_lst)
        punc_lst = range(len(sent_lst))

      while len(punc_lst) and punc_lst[-1] >= len(sent_lst)-1:
        punc_lst.pop()

      self.corpus.append(sent_lst)
      self.corpus_punc.append(punc_lst)

      if msg_out and n % 1000 == 0:
        print >>msg_out, n

def ctb_fname_cmp(f1,f2):
 
  f1, f2 = map(basename, (f1,f2))
  return cmp(int(f1[5:-4]), int(f2[5:-4]))

def ctb_tree_iter(files):

  files.sort(ctb_fname_cmp)

  return chain.from_iterable(imap(ctb_tree_iter_f, files))

def ctb_tree_iter_f(corpus_root):
  in_s = False
  pieces = []
  print >>sys.stderr, corpus_root
  for line in open(corpus_root):
    lowered = line.strip().lower()

    if lowered.startswith('<s '):
      in_s = True

    elif lowered.startswith('</s>'):
      s = ''.join(pieces).strip()
      if len(s):
        # In a couple instances of the CTB, there are two sentences
        # contained in a single <S> node. Deal with that here

        for s1 in split_separate_setences(s):
          yield Tree.parse(s1)

      in_s = False
      pieces = []

    elif in_s:
      pieces.append(line)

def negra_tree_iter(corpus_root):
  pieces = []
  for line in open(corpus_root):
    if line.startswith('%'):
      s = ''.join(pieces).strip()
      if len(s):
        yield Tree.parse(s)

      pieces = []

    else:
      pieces.append(line)

  if len(pieces):
    s = ''.join(pieces).strip()
    yield Tree.parse(s)

def wsj_tree_iter(corpus_root, corpus_ids):
  corpus = corpus_reader(corpus_root, corpus_ids)
  for tree in corpus.parsed_sents():
    yield tree

def tolower_tree(tree):
  for i in xrange(len(tree.leaves())):
    p = tree.leaf_treeposition(i)
    tree[p] = tree[p].lower()

def remove_nodes(tree, filt, rm_trivial=True):

  pos = tree.pos()
  if len(pos) > 1:
    ltp = tree.leaf_treeposition
    todelete = [ltp(i) for i, wp in enumerate(pos) if filt(*wp)]
    todelete.reverse()

    for x in todelete:
      del tree[x]
      j = -1
      while len(tree[x[:j]]) == 0:
        if x[:j] == ():
          return False

        del tree[x[:j]]
        j -= 1

  if rm_trivial:
    to_check = [()]
    while to_check:
      treepos = to_check.pop()
      tr = tree[treepos]
      if type(tr) != str:
        daugpos = treepos + (0,)
        if len(tr) == 1 and type(tree[daugpos]) != str:
          tree[treepos].extend(tree[daugpos])
          del tree[daugpos]
          to_check.append(treepos)

        else:
          to_check.extend(
            treepos + (i,) for i in range(len(tree[treepos])))

  return True

def remove_tags(tree):
  leafs = tree.leaves()

  toremove = []
  for i in xrange(len(leafs)):
    p = tree.leaf_treeposition(i)
    if p[-1] != 0:
      orig_index = p[:-1] + (0,)
      tree[orig_index] = tree[orig_index] + u' ' + tree[p]
      toremove.append(p)

  if toremove:
    for p in toremove:
      del tree[p]

    leafs = tree.leaves()

  for i in xrange(len(leafs)):
    p = tree.leaf_treeposition(i)
    if p[:-1] != ():
      tree[p[:-1]] = leafs[i]

def clean_tree(tree, filt, rm_trivial=True):

  if not remove_nodes(tree, filt, rm_trivial):
    return False

  tolower_tree(tree)
  remove_tags(tree)
  return True

def lower(w): return w.lower()

def array_to_chars(arr):

  for x in arr[:-1]:
    for c in str(x):
      yield c
    yield ' '
  if len(arr):
    for c in str(arr[-1]):
      yield c

def chunk_index(sen):

  lbr = rbr = -1
  i = 0
  _sen = []
  _ind1 = []
  _ind2 = []
  for w in sen:
    if w == '(': _ind1.append(i)

    elif w == ')': _ind2.append(i)

    else:
      _sen.append(w)
      i += 1

  assert len(_ind1) == len(_ind2), 'mismatching brackets\n%s' % sen
  return _sen, zip(_ind1, _ind2)

def not_comment(s): return not comment(s)

def comment(s): return s.startswith('##')

def chunk_index_corpus(filename):

  corpus = filter(not_comment, open(filename).readlines())
  corpus = [s.replace('(', ' ( ').replace(')', ' ) ') for s in corpus]
  corpus = [s.split() for s in corpus]

  return zip(*map(chunk_index, corpus))

def tree_to_brak(tree, label=False):

  if type(tree) == str or type(tree) == unicode: 
    return [tree]

  open_paren = '('
  close_paren = ')'
  if label:
    node_label = '_' + tree.node
    open_paren += node_label
    close_paren += node_label

  items = [open_paren]
  items.extend(tree_to_brak(tree[0], label))

  for t in tree[1:]:
    items.extend(tree_to_brak(t, label))

  items.append(close_paren)

  return items

def just_phrases(tree):

  return just_phrases_from_items(tree_to_brak(tree))

def just_phrases_from_items(pieces):

  tokens = [w for w in pieces if w not in ['(', ')']]
  n = len(tokens) + 1
  opened = [False] * n
  closed = [False] * n

  i = 0
  lbr = -1

  for p in pieces:

    if p == '(': lbr = i

    elif p == ')':
      if lbr != -1:
        if i - lbr != 1:
          opened[lbr] = closed[i] = True
        lbr = -1

    else: i += 1

  items = []
  for i in range(len(tokens)):
    if closed[i]:
      items.append(')')

    if opened[i]:
      items.append('(')

    items.append(tokens[i])

  if len(items) and closed[n - 1]: items.append(')')

  assert \
    len([i for i in items if i == '(']) == \
    len([i for i in items if i == ')']), \
    str(items)

  return items

def phrase_paren_keeper(cats):
  
  def keep(item):
    
    if item[0] in ['(',')']:
      icat = item[2:]
      return any(icat.startswith(cat) for cat in cats)
    
    return True

  return keep

def cat_phrases(tree, keeper):
 
  parts = just_phrases_from_items(tree_to_brak(tree, label=True))
  parts = filter(keeper, parts)
  parts = map(lambda a:a[0] in ['(',')'] and a[0] or a, parts)
  parts = just_phrases_from_items(parts)
  return parts

def chunk2parts(line, csent, stop_val):
  toks = line.split()
  terms, chunks = chunk_index(toks)
  n = len(terms) 
  
  for c1, c2 in chunks:
    pieces = ['(']
    for i in xrange(c1,c2):
      pieces.append(terms[i])
      terms[i] = None

    pieces.append(')')

    terms[c1] = ' '.join(pieces)

  i = 0
  part = []
  psep = []

  for c in csent:
    if c == stop_val: 
      if len(part):
        psep.append(part)
        part = []
    else:
      if terms[i] != None:
        part.append(terms[i])

      i += 1

  if part:
    psep.append(part)

  return psep

def split_tup(m,n):
 
  return ((i,i+1) for i in range(m,n))

def split_chunks(tups):
  
  return list(chain.from_iterable(starmap(split_tup, tups)))

def exit():
  print 'actions:'
  print '  gala_util.py compare gold-standard output [-o output.csv]'
  print '  gala_util.py exp2csv gold-standard output pos-corpus treebank.csv [-o output.csv]'
  print '  gala_util.py analysis-len gold-standard output'
  print '  gala_util.py analysis-pos gold-standard output pos-corpus'
  print '  gala_util.py analysis-cat gold-standard output treebank-file'
  print '  gala_util.py compare-bigrams gold-standard output > experiment-file'
  print '  gala_util.py [--stop_sym __stop__] wsj2spl corpus-files > output'
  print '  gala_util.py [--stop_sym __stop__] wsj2posspl corpus-files > output'
  print '  gala_util.py [--stop_sym __stop__] pos2txt pos-output txt-orig > output'
  print '  gala_util.py wsj-tree-gold-standard corpus-files > output'
  print '  gala_util.py wsj-chunk-gold-standard corpus-files > output'
  print '  gala_util.py wsj-nps-gold-standard corpus-files > output'
  print '  gala_util.py wsj2csv corpus-files > output.csv'
  print '  gala_util.py [--stop_sym __stop__] negra2spl corpus-files > output'
  print '  gala_util.py negra-tree-gold-standard corpus-files > output'
  print '  gala_util.py negra-chunk-gold-standard corpus-files > output'
  print '  gala_util.py [--stop_sym __stop__] ctb2spl corpus-files > output'
  print '  gala_util.py ctb-tree-gold-standard corpus-files > output'
  print '  gala_util.py ctb-chunk-gold-standard corpus-files > output'
  print '  gala_util.py seg2chunk parser-output > chunk-output'
  print '  gala_util.py chunk2rb chunk-output corpus-file > tree-output'
  print '  gala_util.py subset corpus-file N > corpus-fileN'
  print '  gala_util.py soft-hmm-train corpus-file > output'
  print '  gala_util.py bio2chunk bio-output > chunk-output'
  print '  gala_util.py chunk2bio chunk-output orig-text > bio-output'
  print '  gala_util.py tags2doubletags tag-input > output'
  print '  gala_util.py spl2wpl text > output'
  print '  gala_util.py doubletags2tags tag-input > output'
  sys.exit(0)


def main():

  op = OptionParser()
  op.add_option('-s', '--stop_sym', default='__stop__')
  op.add_option('-o', '--output', default=None)

  opt, args = op.parse_args()

  if not len(args): exit()

  elif args[0] == 'soft-hmm-train':
  
    BEGIN_OF_SEG, END_OF_SEG, IN_SEG = range(3)
    codings = ['B 0.5 O 0.5', 
               'I 0.5 O 0.5',
               'B 0.33333 I 0.33333 O 0.33333']

    state = BEGIN_OF_SEG
    stop_sym = opt.stop_sym

    fh = open(args[1])
    try:
      for sent in fh:
        terms = sent.split()
        last = len(terms) - 1
        for i, term in enumerate(terms):
          if i == 0 or terms[i-1] == stop_sym:
            state = BEGIN_OF_SEG
  
          elif i == last or terms[i+1] == stop_sym:
            state = END_OF_SEG
  
          else:
            state = IN_SEG
  
          if term == stop_sym:
            print '__stop__ STOP 1.0'
  
          else:
            print term, codings[state]
  
        print '__eos__ STOP 1.0'

    except IOError:
      pass

    fh.close()

  elif args[0] == 'segments2chunks':
   
    fh = open(args[1])
    for line in fh:
      is_open = False
      try:
        items = line.split()[:-1]
        for item in items:
          if item[0] == item[-1] == '"':
            print item[1:-1], 
          elif item[0] == '"':
            print '( ' + item[1:],
            is_open = True
          elif item[-1] == '"':
            print item[:-1] + ' )',
            assert is_open
            is_open = False
          else:
            print item,
        if is_open:
          print ')',
        print 
      except IOError:
        fh.close()


  elif args[0] == 'bio2productions':
    
    fh = len(args) > 1 and open(args[1]) or sys.stdin
    prev_wrd, prev_tag = fh.next().split()

    no_rule = ['EOS']

    try:
      for wrd, tag in imap(methodcaller('split'), fh):
      
        if prev_tag in no_rule:
          print '%s|%s' % (prev_tag, prev_wrd)

        else:
          print '%s|%s %s' % (prev_tag, prev_wrd, tag)

        prev_tag, prev_wrd = tag, wrd

      print '%s|%s' % (prev_tag, prev_wrd)

    except IOError:
      pass

  elif args[0] == 'spl2wpl':
    fh = len(args) > 1 and open(args[1]) or sys.stdin

    print '__start__'
    for line in fh:
      wrds = line.split()
      while len(wrds) > 0 and wrds[0] == '__stop__':
        del wrds[0]
      
      while len(wrds) > 0 and wrds[-1] == '__stop__':
        del wrds[-1]

      for wrd in wrds:
        print wrd
      print '__eos__'

  elif args[0] == 'tags2doubletags':

    prev_tag = 'BOS'
    no_double = ['BOS','EOS','STOP']
   
    fh = len(args) > 1 and open(args[1]) or sys.stdin
    for wrd, tag in imap(methodcaller('split'), fh):

      if tag in no_double:
        print wrd, tag
      else:
        print wrd, tag + '^' + prev_tag

      prev_tag = tag

  elif args[0] == 'doubletags2tags':
    
    fh = len(args) > 1 and open(args[1]) or sys.stdin
    for wrd, tag in imap(methodcaller('split'), fh):
      print wrd, tag.split('^')[0]

  elif args[0] == 'bio2chunk':

    fh = len(args) > 1 and open(args[1]) or sys.stdin

    open_parens = False

    try:
      assert fh.next().strip() == '__start__ STOP'
      for line in fh:
        word, tag = line.split()

        if tag != 'I' and open_parens:
          print ')',
          open_parens = False

        if tag == 'B':
          print '(',
          open_parens = True

        if tag in ['B','I','O'] and word != '__stop__':
          print word,
      
        if word == '__eos__':
          print

    except IOError:
      pass

  elif args[0] == 'chunk2bio':

    fh = open(args[1])
    stop_sym = opt.stop_sym or '__stop__'

    txt_fh = open(args[2])

    try:
      print '__start__ STOP'

      for line, txt_line in izip(fh, txt_fh):
        beg_chunk = in_chunk = False

        chunk_items = line.split()
        txt_items = txt_line.split()

        txt_item_ind = 0

        is_bos = True

        for chunk_item in chunk_items:
          
          if chunk_item in ['(',')']:
            
            if chunk_item == '(':
              beg_chunk = True

            elif chunk_item == ')':
              beg_chunk = in_chunk = False

            else:
              raise RuntimeError

          else:
            
            while txt_items[txt_item_ind] == stop_sym:
              if in_chunk:
                print '__stop__ I'
              elif not is_bos:
                print '__stop__ STOP'
              txt_item_ind += 1
              is_stop = True

            assert txt_items[txt_item_ind] == chunk_item, \
              'AssertionError: txt and chunk do not match: %s %s' % \
              (txt_items[txt_item_ind], chunk_item)

            if beg_chunk:
              print '%s B' % chunk_item
              beg_chunk = False
              in_chunk = True

            elif in_chunk:
              print '%s I' % chunk_item

            else:
              print '%s O' % chunk_item

            txt_item_ind += 1

            is_bos = False

        if len(txt_items) <= txt_item_ind:
          assert len(txt_items) == txt_item_ind + 1
          assert txt_items[txt_item_ind] == stop_sym

        print '__eos__ STOP'

    except IOError:
      pass

  elif args[0] == 'wsj2spl':

    files = args[1:]
    tree_iter = wsj_tree_iter('./', files)
    filt = lambda w,p:p in WSJ_RM_POS
    punc = lambda w,p:p in WSJ_PUNC_POS

    corpus = Corpus(tree_iter = tree_iter,
                    filt = filt,
                    punc = punc,
                    stop_sym = opt.stop_sym)

    try:
      for s in corpus: print s

    except IOError:
      pass

  elif args[0] == 'wsj2csv':

    if opt.output:
      fh = open(opt.output, 'wb')
    else:
      fh = sys.stdout

    files = args[1:]
    tree_iter = wsj_tree_iter('./', files)
    filt = lambda w,p:p in WSJ_RM_POS

    header = ['Sentence', 'StartIndex', 'EndIndex', 'Category', 'CategoryShort']
    data = [header]
    for n, tree in enumerate(tree_iter):
      clean_tree(tree, filt)
      v = len(tree.leaves())
      for x in xrange(v):
        tree[tree.leaf_treeposition(x)] = x
      for subtr in tree.subtrees():
        leaves = subtr.leaves()
        cat = subtr.node
        cat_short = cat.split('-')[0]
        data.append([n,leaves[0],leaves[-1]+1,cat,cat_short])

    writer = csv.writer(fh)
    writer.writerows(data)

    if opt.output:
      fh.close()

  elif args[0] == 'wsj2posspl':

    files = args[1:]
    tree_iter = wsj_tree_iter('./', files)
    filt = lambda w,p:p in WSJ_RM_POS
    punc = lambda w,p:p in WSJ_PUNC_POS

    corpus = Corpus(tree_iter = tree_iter,
                    filt = filt,
                    punc = punc,
                    use_pos=True,
                    stop_sym = opt.stop_sym)

    try:
      for s in corpus: print s

    except IOError:
      pass

  elif args[0] == 'mkposmerged':
    
    fh1, fh2 = map(open, args[1:3])

    for l1, l2 in izip(fh1, fh2):
      
      l1 = l1.split()
      l2 = l2.split()

      assert len(l1) == len(l2)

      for w1, w2 in izip(l1, l2):
        
        if w1 == opt.stop_sym:
          print w1,

        else:
          print w1 + '-' + w2,

      print

  elif args[0] == 'pos2txt':
    
    pos_fh = open(args[1])
    txt_fh = open(args[2])

    for pos_line, txt_line in izip(pos_fh, txt_fh):
      
      pos = pos_line.split()
      txt = [k for k in txt_line.split() if k != opt.stop_sym]

      i = 0
      for w in pos:
        if w in ['(',')']:
          print w,
        
        else:
          print txt[i],
          i += 1

      print

  elif args[0] == 'subset':
   
    n = int(args[2])
    try:
      for line in open(args[1]):
        if len(str_remove_ignore(line, opt.stop_sym).split()) <= n:
          print line.rstrip()

    except IOError:
      pass

  elif args[0] == 'wsj-nps-gold-standard':
    files = args[1:]
    tree_iter = wsj_tree_iter('./', files)
    filt = lambda w,p:p in WSJ_RM_POS

    try:
      keeper = phrase_paren_keeper(['NP','QP','WHNP'])
      for tree in tree_iter:
        clean_tree(tree, filt)
        assert len(tree.leaves())
        for x in cat_phrases(tree, keeper):
          print x,
        print

    except IOError:
      pass

  elif args[0] == 'wsj-chunk-gold-standard':
    files = args[1:]
    tree_iter = wsj_tree_iter('./', files)
    filt = lambda w,p:p in WSJ_RM_POS

    try:
      for tree in tree_iter:
        clean_tree(tree, filt)
        assert len(tree.leaves())
        for x in just_phrases(tree):
          print x,
        print

    except IOError:
      pass

  elif args[0] == 'wsj-tree-gold-standard':
    files = args[1:]
    tree_iter = wsj_tree_iter('./', files)
    filt = lambda w,p:p in WSJ_RM_POS 

    try:
      for tree in tree_iter:
        clean_tree(tree, filt)
        assert len(tree.leaves())
        pprint_nonodes(tree, sys.stdout)
        print

    except IOError:
      pass

  elif args[0] == 'wsj-corpus-study':

    files = args[1:]
    tree_iter = wsj_tree_iter('./', files)
    filt = lambda w,p:p in WSJ_RM_POS

    d = defaultdict(lambda:0)
    for tree in tree_iter:
      clean_tree(tree, filt)
      assert len(tree.leaves())
      phrases = just_phrases(tree)
      i = 0
      chunks = []
      for item in phrases:
        if item == '(':
          openb = i
        elif item == ')':
          label = tree[tree.treeposition_spanning_leaves(openb,i)].node
          d[label] += 1
        else:
          i += 1

    total = float(sum(d.values()))
    print 'fine-grained:'
    for label, val in d.iteritems(): 
      print '%9s %5d   %2.1f %%' % (label, val, 100.0 * val / total)
    print

    print 'coarse-grained:'
    d1 = defaultdict(lambda:0)
    for label, val in d.iteritems():
      d1[label.split('-')[0].split('=')[0]] += val
    for label, val in d1.iteritems(): 
      print '%9s %5d   %2.1f %%' % (label, val, 100.0 * val / total)

  elif args[0] == 'negra2spl':

    fname = args[1]
    tree_iter = negra_tree_iter(fname)
    filt = lambda w,p:p.startswith('*') or p in NEGRA_RM_POS or w in STOPPING_PUNC

    corpus = Corpus(tree_iter = tree_iter,
                    filt = filt,
                    punc = lambda w,p:False,
                    stop_sym = opt.stop_sym)

    try:
      for s in corpus: print s

    except IOError:
      pass

  elif args[0] == 'negra-tree-gold-standard':

    fname = args[1]
    tree_iter = negra_tree_iter(fname)
    filt = lambda w,p:p.startswith('*') or p in NEGRA_RM_POS or w in STOPPING_PUNC

    try:
      for tree in tree_iter:
        clean_tree(tree, filt)
        assert len(tree.leaves())
        pprint_nonodes(tree, sys.stdout)
        print

    except IOError:
      pass

  elif args[0] == 'negra-chunk-gold-standard':

    fname = args[1]
    tree_iter = negra_tree_iter(fname)
    filt = lambda w,p:p.startswith('*') or p in NEGRA_RM_POS or w in STOPPING_PUNC
    punc = lambda w,p:p in NEGRA_PUNC_POS

    try:
      for tree in tree_iter:
        clean_tree(tree, filt)
        assert len(tree.leaves())
        for x in just_phrases(tree):
          print x,
        print 

    except IOError:
      pass

  elif args[0] == 'negra-nps-gold-standard':

    fname = args[1]
    tree_iter = negra_tree_iter(fname)
    filt = lambda w,p:p.startswith('*') or p in NEGRA_RM_POS or w in STOPPING_PUNC
    punc = lambda w,p:p in NEGRA_PUNC_POS

    try:
      keeper = phrase_paren_keeper(['NP','CNP'])
      for tree in tree_iter:
        clean_tree(tree, filt)
        assert len(tree.leaves())
        for x in cat_phrases(tree, keeper):
          print x,
        print 

    except IOError:
      pass

  elif args[0] == 'negra-corpus-study':

    fname = args[1]
    tree_iter = negra_tree_iter(fname)
    filt = lambda w,p:p.startswith('*') or p in NEGRA_RM_POS or w in STOPPING_PUNC
    punc = lambda w,p:p in NEGRA_PUNC_POS

    d = defaultdict(lambda:0)
    for tree in tree_iter:
      clean_tree(tree, filt)
      assert len(tree.leaves())
      phrases = just_phrases(tree)
      i = 0
      chunks = []
      for item in phrases:
        if item == '(':
          openb = i
        elif item == ')':
          label = tree[tree.treeposition_spanning_leaves(openb,i)].node
          d[label] += 1
        else:
          i += 1

    total = float(sum(d.values()))
    print 'fine-grained:'
    for label, val in d.iteritems(): 
      print '%9s %5d   %2.1f %%' % (label, val, 100.0 * val / total)
    print

    print 'coarse-grained:'
    d1 = defaultdict(lambda:0)
    for label, val in d.iteritems():
      d1[label.split('-')[0]] += val
    for label, val in d1.iteritems(): 
      print '%9s %5d   %2.1f %%' % (label, val, 100.0 * val / total)

  elif args[0] == 'ctb2spl':

    files = args[1:]
    tree_iter = ctb_tree_iter(files)
    filt = lambda w,p:p in CTB_RM_POS or w in STOPPING_PUNC
    punc = lambda w,p:p in CTB_PUNC_POS

    corpus = Corpus(tree_iter = tree_iter,
                    filt = filt,
                    punc = punc,
                    stop_sym = opt.stop_sym)

    try:
      for s in corpus: 
        if len(s):
          print s
        else:
          print ''

    except IOError:
      pass

  elif args[0] == 'clean-up-output':
    
    for line in sys.stdin:
      parts = line.split()
      parts = [w.lower() for w in parts if w not in STOPPING_PUNC]
      print ' '.join(parts)

  elif args[0] == 'ctb-tree-gold-standard':

    files = args[1:]
    tree_iter = ctb_tree_iter(files)
    filt = lambda w,p:p in CTB_RM_POS or w in STOPPING_PUNC
    punc = lambda w,p:p in CTB_PUNC_POS

    try:
      for tree in tree_iter:
        clean_tree(tree, filt)
        if len(tree.leaves()):
          pprint_nonodes(tree, sys.stdout)
          print
        else:
          print ''

    except IOError:
      pass

  elif args[0] == 'ctb-chunk-gold-standard':

    files = args[1:]
    tree_iter = ctb_tree_iter(files)
    filt = lambda w,p:p in CTB_RM_POS or w in STOPPING_PUNC
    punc = lambda w,p:p in CTB_PUNC_POS

    try:
      for tree in tree_iter:
        clean_tree(tree, filt)
        if len(tree.leaves()):
          for x in just_phrases(tree): print x,
          print

        else:
          print ''

    except IOError:
      pass

  elif args[0] == 'ctb-nps-gold-standard':

    files = args[1:]
    tree_iter = ctb_tree_iter(files)
    filt = lambda w,p:p in CTB_RM_POS or w in STOPPING_PUNC
    punc = lambda w,p:p in CTB_PUNC_POS

    keeper = phrase_paren_keeper(['DP','NP','DNP','QP'])

    try:
      for tree in tree_iter:
        clean_tree(tree, filt)
        if len(tree.leaves()):
          for x in cat_phrases(tree,keeper) : print x,
          print

        else:
          print ''

    except IOError:
      pass

  elif args[0] == 'ctb-corpus-study':
        
    files = args[1:]
    tree_iter = ctb_tree_iter(files)
    filt = lambda w,p:p in CTB_RM_POS or w in STOPPING_PUNC
    punc = lambda w,p:p in CTB_PUNC_POS

    d = defaultdict(lambda:0)
    for tree in tree_iter:
      clean_tree(tree, filt)
      if len(tree.leaves()):
        phrases = just_phrases(tree)
        i = 0
        chunks = []
        for item in phrases:
          if item == '(':
            openb = i
          elif item == ')':
            label = tree[tree.treeposition_spanning_leaves(openb,i)].node
            d[label] += 1
          else:
            i += 1

    total = float(sum(d.values()))
    print 'fine-grained:'
    for label, val in d.iteritems(): 
      print '%9s %5d   %2.1f %%' % (label, val, 100.0 * val / total)
    print

    print 'coarse-grained:'
    d1 = defaultdict(lambda:0)
    for label, val in d.iteritems():
      d1[label.split('-')[0]] += val
    for label, val in d1.iteritems(): 
      print '%9s %5d   %2.1f %%' % (label, val, 100.0 * val / total)

  elif args[0] == 'chunk2sp':

    try:
      alpha, corpus = load(open(args[2], 'r', -1))

    except UnpicklingError:
      alpha = Alpha()
      corpus = make_corpus(open(args[2], 'r', -1), alpha)

    try:
      for s, line in enumerate(open(args[1], 'r', -1)):
  
        toks = line.split()
        terms, chunks = chunk_index(toks)
        n = len(terms) 
  
        if opt.stop_sym == None: 
          opt.stop_sym = "__stop__"

        stop_val = alpha[opt.stop_sym]
        j = -1
        codes = corpus[s][:]
        bracks = []


        try:
          while codes[0] == stop_val:
            codes = codes[1:]
          open_b = 0
          for code in codes:
            if code == stop_val:
              if j < n:
                bracks.append((open_b,j))
                open_b = j+1
            else:
              j += 1

          bracks.extend([(i,j-1) for i,j in chunks])

          b = Bracketing(terms, map(brak_from_tup, bracks))
          print b

        except IndexError:
          print 

    except IOError:
      pass

  elif args[0] == 'chunk2lb':

    try:
      alpha, corpus = load(open(args[2], 'r', -1))

    except UnpicklingError:
      alpha = Alpha()
      corpus = make_corpus(open(args[2], 'r', -1), alpha)

    if opt.stop_sym == None: opt.stop_sym = '__stop__'
    stop_val = alpha[opt.stop_sym]
  
    fh = open(args[1], 'r', -1)
    try:
      for s, line in enumerate(fh):
        parts = chunk2parts(line, corpus[s], stop_val)
        
        for part in parts:
          if len(part) > 1:

            for _ in xrange(len(part) - 1):
              print '(',

            print part[0],

            for p in part[1:]:
              print p,
              print ')',

          else:
            print part[0],

        print

    except IOError:
      fh.close()

  elif args[0] == 'chunk2rb':

    try:
      alpha, corpus = load(open(args[2], 'r', -1))

    except UnpicklingError:
      alpha = Alpha()
      corpus = make_corpus(open(args[2], 'r', -1), alpha)
    
    if opt.stop_sym == None: opt.stop_sym = '__stop__'
    stop_val = alpha[opt.stop_sym]
   
    fh = open(args[1], 'r', -1)
    try:
      for s, line in enumerate(fh):
        parts = chunk2parts(line, corpus[s], stop_val)

        for part in parts:
          print '(',
          if len(part) > 1:
            for p in part[:-1]:
              print '(',
              print p,

            print part[-1],
            for _ in xrange(len(part) - 1):
              print ')',

          else:
            print part[0],

        print ') ' * len(parts)

    except IOError:
      fh.close()
  
  elif args[0] == 'seg2chunk':
    
    sentences = []
    curr = []
    for line in open(args[1], 'r', -1):
      if line[0] != '#':
        line = line.strip().replace('(','( ').replace(')',' )')
        if len(line) == 0:
          if len(curr) != 0:
            sentences.append(curr)
            curr = []
        
        else:
          curr.extend(line.split())

    for brak in sentences:
      for x in just_phrases_from_items(brak): print x,
      print

  elif args[0] == 'seg2tree':
    
    sentences = []
    curr = []
    for line in open(args[1], 'r', -1):
      if line[0] != '#':
        line = line.strip().replace('(','( ').replace(')',' )')
        if len(line) == 0:
          if len(curr) != 0:
            sentences.append(curr)
            curr = []
        
        else:
          curr.extend(line.split())

    for brak in sentences:
      print ' '.join(brak)

  elif args[0] == 'compare-bigrams':

    assert len(args) == 3, 'require gold-standard and output files'
    for c in filter(comment, open(args[2]).readlines()): print c.strip()

    words, gold = chunk_index_corpus(args[1])
    words1, outp = chunk_index_corpus(args[2])

    for i, s1, s2 in izip(icount(1), words, words1):
      assert s1 == s2, 'sentences do not match\n' +\
        '%d\n%s\n%s' % (i, ' '.join(s1), ' '.join(s2))

    gold = map(split_chunks, gold)
    outp = map(split_chunks, outp)

    n_true_pos = 0
    n_false_pos = 0
    n_false_neg = 0

    for i in range(len(gold)):

      gold_phrases = set(gold[i])
      outp_phrases = set(outp[i])

      true_pos = gold_phrases & outp_phrases
      false_pos = outp_phrases - gold_phrases
      false_neg = gold_phrases - outp_phrases

      # Some error analysis can be done here

      n_true_pos += len(true_pos)
      n_false_pos += len(false_pos)
      n_false_neg += len(false_neg)

    try:
      prec = 100. * n_true_pos / (n_true_pos + n_false_pos)
      rec = 100. * n_true_pos / (n_true_pos + n_false_neg)
      f1 = 2. * prec * rec / (prec + rec)
    except ZeroDivisionError:
      prec = rec = f1 = 0.

    print 'P = %.2f\tR = %.2f\tF = %.2f' % (prec, rec, f1)

    print 'TP = %d\tFP = %d\tFN = %d' % (n_true_pos, n_false_pos, n_false_neg)

  elif args[0] == 'exp2csv':

    if opt.output:
      fh = open(opt.output, 'wb')
    else:
      fh = sys.stdout

    header = ['Sentence', 'StartIndex', 'EndIndex', 'Terms', 'PosSeq', 'Category',
              'CategoryShort', 'Result', 'ErrorType']

    gold_words, exp_gold = chunk_index_corpus(args[1])
    outp_words, exp_outp = chunk_index_corpus(args[2])

    assert outp_words == gold_words

    pos_corpus = [[w for w in s.split() if w != '__stop__'] for s in open(args[3]).readlines()]
    treebank = list(iter(csv.reader(open(args[4]))))
    for i in xrange(1,len(treebank)):
        for j in xrange(3):
            treebank[i][j] = int(treebank[i][j])
    cat_dict = dict([(tuple(s[:3]), s[3]) for s in treebank])
    cat_short_dict = dict([(tuple(s[:3]), s[4]) for s in treebank])

    data = [header]

    for n, words, pos, gold_p, outp_p in \
        izip(icount(), outp_words, pos_corpus, exp_gold, exp_outp):

      gold_p, outp_p = set(gold_p), set(outp_p)
      tp, fp, fn = gold_p & outp_p, outp_p - gold_p, gold_p - outp_p

      for sta, end in tp:
        terms = ' '.join(words[sta:end])
        pos_seq = '-'.join(pos[sta:end])
        cat = cat_dict[n,sta,end]
        cat_short = cat_short_dict[n,sta,end]
        data.append([n,sta,end,terms,pos_seq,cat,cat_short,'TP','NA'])

      for sta, end in fp:
        terms = ' '.join(words[sta:end])
        pos_seq = '-'.join(pos[sta:end])

        error_type = 'NoOverlap'
        for sta1, end1 in fn:
          if sta1 <= sta and end <= end1:
            error_type = 'Sub'
            break
          elif (sta < sta1 and end < end1) or (sta1 < sta or end1 < end):
            error_type = 'Crossing'
            break
          elif sta <= sta1 and end1 <= end:
            error_type = 'Super'
            break
    
        data.append([n,sta,end,terms,pos_seq,'NA','NA','FP',error_type])

      for sta, end in fn:
        terms = ' '.join(words[sta:end])
        pos_seq = '-'.join(pos[sta:end])
        cat = cat_dict[n,sta,end]
        cat_short = cat_short_dict[n,sta,end]

        error_type = 'unset'
        for sta1, end1 in fn:
          if sta1 <= sta and end <= end1:
            error_type = 'Sub'
            break
          elif (sta < sta1 and end < end1) or (sta1 < sta or end1 < end):
            error_type = 'Crossing'
            break
          elif sta <= sta1 and end1 <= end:
            error_type = 'Super'
            break

        assert error_type != 'unset'

        data.append([n,sta,end,terms,pos_seq,cat,cat_short,'FN',error_type])

    writer = csv.writer(fh)
    writer.writerows(data)

    if opt.output:
      fh.close()

  elif args[0] == 'compare':

    notstop = lambda x:x != '__stop__'

    if opt.output:
      output = open(opt.output, 'w')
    else:
      output = None

    assert len(args) >= 3, 'require gold-standard and output files'
    for c in filter(comment, open(args[2]).readlines()): print c.strip()

    words, gold = chunk_index_corpus(args[1])
    words1, outp = chunk_index_corpus(args[2])

    if len(args) > 3:
      pos_fh = open(args[3])
      pos_tp = defaultdict(lambda:0)
      pos_fp = defaultdict(lambda:0)
      pos_fn = defaultdict(lambda:0)
    else:
      pos_fh = False

    for i, s1, s2 in izip(icount(1), words, words1):
      assert [w.lower() for w in s1] == [w.lower() for w in s2], 'sentences do not match\n' +\
        '%d\n%s\n%s' % (i, ' '.join(s1), ' '.join(s2))

    count = [0] * 10
    lens = [0] * 5

    count_by_len = [[0] * 5 for i in xrange(5)]

    by_pos = [defaultdict(lambda:0) for i in xrange(5)]

    tp_id, fp_id, fn_id, all_gold, all_pred, \
    tp_big_id, fp_big_id, fn_big_id, all_gold_big, all_pred_big = range(10)

    substring_count = 0
    supstring_count = 0

    alt_count = [0] * 5

    for i in range(len(gold)):

      gold_phrases = set(gold[i])
      outp_phrases = set(outp[i])

      true_pos = gold_phrases & outp_phrases
      false_pos = outp_phrases - gold_phrases
      false_neg = gold_phrases - outp_phrases

      for x in false_pos:
        for y in false_neg:
          if y[0] <= x[0] and x[1] <= y[1]:
            substring_count += 1
            break

      for y in false_neg:
        for x in false_pos:
          if y[0] <= x[0] and x[1] <= y[1]:
            supstring_count += 1
            break

      gold_big = set(split_chunks(gold_phrases))
      outp_big = set(split_chunks(outp_phrases))
      tp_big = gold_big & outp_big
      fp_big = outp_big - gold_big
      fn_big = gold_big - outp_big

      if pos_fh:
        pos = filter(notstop, pos_fh.next().split())

      for id, data in zip(range(5), [true_pos, false_pos, false_neg, gold_phrases, outp_phrases]):

        for x in data:
          l = x[1] - x[0]
          if l <= 1:
            print >>sys.stderr, 'len %d clump -- ignoring' % l
          else:
            lens[id] += l
            count[id] += 1

            nlen = min(l,6)
            nlen -= 2
            count_by_len[id][nlen] += 1

            if pos_fh:
              alt_count[id] += 1
              by_pos[id]['-'.join(pos[x[0]:x[1]])] += 1

      for id, data in zip(range(5,10), [tp_big, fp_big, fn_big, gold_big, outp_big]):
        count[id] += len(data)

    if pos_fh: assert alt_count == count[:5]

    prec = 100. * count[tp_id] / count[all_pred]
    rec = 100. * count[tp_id] / count[all_gold]
    f1 = 2. * prec * rec / (prec + rec)

    prec_big = 100. * count[tp_big_id] / count[all_pred_big]
    rec_big = 100. * count[tp_big_id] / count[all_gold_big]
    f1_big = 2. * prec_big * rec_big / (prec_big + rec_big)

    print 'Summary %.2f / %.2f / %.2f ( %d / %d / %d )' % ((prec, rec, f1) + tuple(count[:3]))
    print 'PerBigr %.2f / %.2f / %.2f ( %d / %d / %d )' % ((prec_big, rec_big, f1_big) + tuple(count[5:8]))
    print 'Substring prop %.2f ( %d )' % (100. * substring_count/count[fp_id], substring_count)
    print 'Supstring prop %.2f ( %d )' % (100. * supstring_count/count[fn_id], supstring_count)

    if output:
      print >>output, 'Summary,TP,FP,FN,,'
      print >>output, 'Acc,%d,%d,%d,,' % tuple(count[:3])
      print >>output, 'Per bigr,%d,%d,%d,,' % (tuple(count[5:8]))
      print >>output, ',,,,,'

    means = tuple([float(lens[id])/count[id] for id in range(5)])
    print 'Mean Len : TP %.2f / FP %.2f / FN %.2f / Gold %.2f / Pred %.2f' % means

    if output:
      print >>output, 'Mean Len,TP,FP,FN,All pred,All gold'
      print >>output, ',%.2f,%.2f,%.2f,%.2f,%.2f' % means
      print >>output, ',,,,,'
      print >>output, ',Sub FP,All FP,Sup FN,All FN,'
      print >>output, ',%d,%d,%d,%d,' % (substring_count, count[fp_id], supstring_count, count[fn_id])
      print >>output, ',,,,,'


    if output:
      print >>output, 'By clump len,Len,TP,FP,FN'
    len_names = map(str, range(2,6)) + ['>5']

    for nlen in xrange(5):
      tp_by_len, fp_by_len, fn_by_len = (count_by_len[id][nlen] for id in range(3))
      if tp_by_len == 0:
        nlen_prec = 0.
        nlen_rec = 0.
        nlen_f = 0.
      else:
        nlen_prec = 100. * tp_by_len / (tp_by_len + fp_by_len)
        nlen_rec = 100. * tp_by_len / (tp_by_len + fn_by_len)
        nlen_f = 2. * nlen_prec * nlen_rec / (nlen_prec + nlen_rec)

      print 'NLEN %s %.2f / %.2f / %.2f ( %d / %d / %d )' % \
        (len_names[nlen], nlen_prec, nlen_rec, nlen_f, tp_by_len, fp_by_len, fn_by_len)

      if output:
        print >>output, ',%s,%d,%d,%d,' % (len_names[nlen], tp_by_len, fp_by_len, fn_by_len)

    if output:
      print >>output, ',,,,,'
        

    if pos_fh:
      pos_lists = [[(y,x) for (x,y) in by_pos[id].items()] for id in range(3)]
      for ls in pos_lists: ls.sort()

      names = ['POS-TP','POS-FP','POS-FN']
      for i in xrange(3):
        assert sum(p[0] for p in pos_lists[i]) == count[i]
        assert sum(by_pos[i].values()) == count[i]

      for name, d_list in zip(names, pos_lists):
        print '==',name,'=='
        if output:
          print >>output, '%s,POS,TP,FP,FN' % name
        for i in xrange(10):
          p = d_list[-1-i][1]
          print '%s %s\t%d' % (name, p, d_list[-1-i][0]),
          tp, fp, fn = tuple(by_pos[id][p] for id in range(3))
          prec = tp and (100. * tp / (tp + fp)) or 0
          rec = tp and (100. * tp / (tp + fn)) or 0
          f = tp and (2 * prec * rec / (prec + rec)) or 0
          print '\t%.2f / %.2f / %.2f ( %d / %d / %d )' % (prec, rec, f, tp, fp, fn)
          if output:
            print >>output, ',%s,%d,%d,%d' % (p, tp, fp, fn)

        donotcount = [x[1] for x in d_list[-10:]]
        tp, fp, fn = \
          tuple([sum([x[1] for x in by_pos[id].items() if x[0] not in donotcount]) \
                 for id in range(3)])
        print '%s Others\t%d' % (name, sum(x[0] for x in d_list[:-10])),
        prec = tp and (100. * tp / (tp + fp)) or 0
        rec = tp and (100. * tp / (tp + fn)) or 0
        f = tp and (2 * prec * rec / (prec + rec)) or 0
        print '\t%.2f / %.2f / %.2f ( %d / %d / %d )' % (prec, rec, f, tp, fp, fn)
        if output:
          print >>output, ',Others,%d,%d,%d,' % (tp, fp, fn)
          print >>output, ',,,,,'

    if output:
      output.close()

  else:
    print >>sys.stderr, 'unexpected action', args[0]
    sys.exit(1)

if __name__ == '__main__': main()
