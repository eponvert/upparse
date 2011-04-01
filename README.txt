upparse -- Unsupervised parsing and noun phrase identification.

Elias Ponvert <elias@ponvert.net>
March 23, 2011

This software contains efficient implementations of hidden Markov
models (HMMs) and probablistic right linear grammars (PRLGs) for
unsupervised partial parsing (also known as: unsupervised chunking,
unsupervised NP identification, unsupervsed phrasal segmentation).
These models are particularly effective at noun phrase identification,
and have been evaluated at that task using corpora in English, German
and Chinese. 

In addition, this software package provides a driver script to manage
a cascade of chunkers to create full (unlabeled) constituent trees.
This strategy produces state-of-the-art unsupervised constituent
parsing results when evaluated using labeled constituent trees in
English, German and Chinese -- possibly others, those are just the
ones we tried.

A description of the methods implemented in this project can be found
in the paper "Simple Unsupervised Grammar Induction from Raw Text with
Cascaded Finite State Models" by Elias Ponvert, Jason Baldridge and
Katrin Erk, to appear in Proceedings of the 49th Annual Meeting of the
Association for Computational Linguistics: Human Language
Technologies, June, 2011. 

I. Information

The core of this system is implemented in Java 6 and makes use of
Apanch Ant for project buiding and JUnit for unit testing.  Most
system interaction, and all replicating of reported results, is
accomplished through a driver script (scripts/chunk.py) implemented in
Python 2.6. 

The system is designed to work with the Penn Treebank, the Negra
German treebank, and the Penn Chinese Treebank with minimal data
preparation.  

II. Installation and usage

The following assume you're working in a Unix environment and
interfacing with the OS using bash (Bourne again shell). $ indicates
shell prompt.

A. Installation

To install the most recent version from Bitbucket, run:

$ hg clone http://bitbucket.org/eponvert/upparse

By default, this will create a new directory called 'upparse'. 

To create an executable Jar file, run:

$ cd upparse
$ ant jar

B. Using the convenience script chunk.py

C. Running from the Jar file

III. Replicating published results

If you have downloaded this code from a repository, then to replicate
the results reported in the ACL paper cited above, consider using the
acl-2011 tag, which indicates the version of the code used to generate
the reported results.  To do this, run:

$ hg up acl-2011

A. Data preparation

In the ACL paper cited above, this system is evaluated using the
following resources: 

(WSJ) The Wall Street Journal sections of Treebank-3 (The Penn
Treebank release 3), from the Linguistic Data Corporation LDC99T42. 

http://www.ldc.upenn.edu/Catalog/CatalogEntry.jsp?catalogId=LDC99T42

(Negra) The Negra (German) corpus from Saarland University.

http://www.coli.uni-saarland.de/projects/sfb378/negra-corpus/

(CTB) Chinese Treebank 5.0 from the Linguistic Data Corporation
LDC2005T01.

http://www.ldc.upenn.edu/Catalog/CatalogEntry.jsp?catalogId=LDC2005T01

You can use the system on downloaded treebank data directly (with one
caveat: CTB must be converted to UTF8).  The convenience script
chunk.py assumes that training and evaluation data-sets are single
files, so the following describes the experimental setup, in terms of
choice of these subsets of the data.

1. WSJ

For WSJ we use sections 02-22 for train, section 24 for development
and section 23 for test.  Asssuming the Penn Treebank was downloaded
and unzipped into a directory called penn-treebank-rel3:

$ cat penn-treebank-rel3/parsed/mrg/wsj/0[2-9]/*.mrg \
      penn-treebank-rel3/parsed/mrg/wsj/1[0-9]/*.mrg \
      penn-treebank-rel3/parsed/mrg/wsj/2[0-2]/*.mrg \
      > wsj-train.mrg
$ cat penn-treebank-rel3/parsed/mrg/wsj/24/*.mrg > wsj-devel.mrg
$ cat penn-treebank-rel3/parsed/mrg/wsj/23/*.mrg > wsj-test.mrg

2. Negra

The Negra corpus comes as one big file.  For train we use the first
18602 sentences, for test we use the penultimate 1000 sentences and
for development we use the last 1000 sentences.  Assuming that the
corpus is downloaded and unzipped into a directory called negra2:

$ head -n 663736 negra2/negra-corpus.penn > negra-train.penn
$ tail -n +663737 negra2/negra-corpus.penn \
  | head -n $((698585-663737)) > negra-test.penn
$ tail -n +698586 negra2/negra-corpus.penn > negra-devel.penn

3. CTB

Assuming the CTB corpus is downloaded and unzipped into a directory
called ctb5.  We use the bracket annotations in ctb5/data/bracketed.
To create the UTF8 version of this resource that this code works with,
use the included gb2unicode.py script as follows:

$ mkdir ctb5-utf8
$ python scripts/gb2unicode.py ctb5/data/bracketed ctb5-utf8

For the train/development/test splits used in the ACL paper, we used
the split used by Duan et al in 

Xiangyu Duan, Jun Zhao, and Bo Xu. 2007. "Probabilistic models for
    action-based Chinese dependency parsing."  In Proceedings of
    ECML/ECPPKDD, Warsaw, Poland, September.

Specifically:

Train: 001-815 ; 1001-1136 
Dev: 886-931 ; 1148 - 1151
Test: 816-885 ; 1137 - 1147

To create these, run

$ cat ctb5-utf8/chtb_[0-7][0-9][0-9].fid \
      ctb5-utf8/chtb_81[0-5].fid         \
      ctb5-utf8/chtb_10[0-9][0-9].fid    \
      ctb5-utf8/chtb_11[0-2][0-9].fid    \
      ctb5-utf8/chtb_113[0-6].fid        \
      > ctb-train.fid

$ cat ctb5-utf8/chtb_9[0-2][0-9].fid \
      ctb5-utf8/chtb_93[01].fid      \
      ctb5-utf8/chtb_114[89].fid     \ 
      ctb5-utf8/chtb_115[01].fid     \
      > ctb-dev.fid

there are no 886-900

$ cat ctb5-utf8/chtb_81[6-9].fid     \
      ctb5-utf8/chtb_8[2-7][0-9].fid \
      ctb5-utf8/chtb_88[0-5].fid     \
      ctb5-utf8/chtb_113[7-9].fid    \
      ctb5-utf8/chtb_114[0-7].fid    \
      > ctb-test.fid

Quick tip: Keep the file extensions that are used in the corpus files
themselves: .mrg for WSJ, .penn for Negra, and .fid for CTB.  The
chunk.py script uses these extensions to guess the corpus type, if not
specified otherwise.

B. Replicating chunking results

Assuing the TRAIN is the training file (for WSJ, Negra or CTB), and
TEST is the test file (or development file), then chunking results
from the ACL paper are replicated by executing the following:

for the PRLG:

$ ./scripts/chunk.py -t TRAIN -s TEST -m prlg-uni

for the HMM:

$ ./scripts/chunk.py -t TRAIN -s TEST -m hmm-uni

But these commands just print out final evaluation numbers.  To see
the output of the runs, choose an output directory (e.g. testout --
but don't create the directory) and run:

for the PRLG:

$ ./scripts/chunk.py -t TRAIN -s TEST -m prlg-uni -o testout

for the HMM:

$ ./scripts/chunk.py -t TRAIN -s TEST -m hmm-uni -o testout

Several files are created in the testout directory: 

testout/RESULTS is a text file with the results of the experiment

testout/README is a text file with some information about the
    parameters used

testout/STATUS is the output of the experimental run (progress of the
    experiment, the iterations and the model's estimate of the dataset
    complexity for each iteration of EM), and any error output.  While
    running experiments, you can track progress by running
    
    $ tail -f testout/STATUS

testout/Iter-NNN is the final output of the system on the TEST
    dataset, where NNN is the number of iterations of EM run before
    convergence. 

To run experiments on the datasets, but completely ignorning
punctuation, use the -P flag, e.g.:

$ ./scripts/chunk.py -t TRAIN -s TEST -m prlg-uni -P

To vary the degree of smoothing, use the -S flag, e.g.:

$ ./scripts/chunk.py -t TRAIN -s TEST -m prlg-uni -S .001

C. Replicating cascaded-chunking/parsing results

The same chunk.py script is used to drive the cascaded-chunking
experiments, using the -C flag.  An output directory is required, but
if none is specified, the script will use the directory named 'out' by
default.  All of the options for chunking are available for cascaded
chunking, since each step in the cascade is a chunker initialized as
before.  In fact, for each step in the cascade, the chunker is run
twice: once to generate training material for the next step in the
cascade, and once to run on TEST to evaluate.  (I'll admit this is
inefficient and pretty dumb, since the model has to re-train.)

TODO output

V. Licencse

VI. Citation

If you use this system in academic research with published results,
please cite the following paper:

Elias Ponvert, Jason Baldridge and Katrin Erk (2011), "Simple
    Unsupervised Grammar Induction from Raw Text with Cascaded Finite
    State Models" in Proceedings of the 49th Annual Meeting of the
    Association for Computational Linguistics: Human Language
    Technologies, Portland, Oregon, USA, June 2011.

or use this Bibtex:

@InProceedings{ponvert-baldridge-erk:2011:ACL,
  author    = {Ponvert, Elias and Baldridge, Jason and Erk, Katrin},
  title     = {Simple Unsupervised Grammar Induction from Raw Text with Cascaded Finite State Models},
  booktitle = {Proceedings of the 49th Annual Meeting of the Association for Computational Linguistics: Human Language Technologies},
  month     = {June},
  year      = {2011},
  address   = {Portland, Oregon, USA},
  publisher = {Association for Computational Linguistics}
}
