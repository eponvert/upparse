upparse -- Unsupervised parsing and noun phrase identification.

Elias Ponvert <elias@ponvert.net>
April 15, 2011

This software contains efficient implementations of hidden Markov
models (HMMs) and probabilistic right linear grammars (PRLGs) for
unsupervised partial parsing (also known as: unsupervised chunking,
unsupervised NP identification, unsupervised phrasal segmentation).
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
Apache Ant for project building and JUnit for unit testing.  Most
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

A. Getting the source

If you are using the compiled distribution of this software, then this
section is not relevant to your needs.  Also, if you have already
acquired the source code for this project via a source distribution (a
zip or a tarball, in other words), then you can skip to II.B
Installation. 

To acquire the most recent changes to this project, use Mercurial SCM,
for info see

http://mercurial.selenic.com/

The following assumes you have Mercurial installed, and hg refers to
the Mercurial command, as usual.  To install the most recent version
from Bitbucket, run:

$ hg clone http://bitbucket.org/eponvert/upparse

By default, this will create a new directory called 'upparse'. 

B. Installation

Using the command line, make sure you are in the source code
directory, e.g.:

$ cd upparse

To create an executable Jar file, run:

$ ant jar

And that's it. 

C. Using the convenience script chunk.py

For most purposes, including replicating reported results, the
convenience script chunk.py is the easiest way to use the system. 

1. Chunking

To simply run the system on train and evaluation datasets, let's call
them WSJ-TRAIN.mrg and WSJ-EVAL.mrg, the command is:

$ ./scripts/chunk.py -t WSJ-TRAIN.mrg -s WSJ-EVAL.mrg

You have to be in the project directory to run that command, at
present.  Also: the script determines the file type from the file name
suffix:

.mrg  : Penn Treebank merged annotated files (POS and brackets)
.fid  : Penn Chinese Treebank bracketed files (in UTF-8, see below)
.penn : Negra corpus in Penn Treebank format

Any other files are assumed to be tokenized, UTF-8, tokens separated
by white-space, one-sentence-per-line.

The command above prints numerical results of the experiment.  To
actually get chunker output, use the -o flag to specify an output
directory: 

$ ./scripts/chunk.py -t WSJ-TRAIN.mrg -s WSJ-EVAL.mrg -o out

If that directory already exists, you will be prompted to make sure
you wish to overwrite.  This command creates the directory named `out`
and puts some information in there:

out/README  : some information about the parameters used in this
              experiment 

out/STATUS  : the iterations and some information about the experiment
              run; to track the progress of the experiment, run
             
              $ tail -f out/STATUS

out/RESULTS : evaluation results of the experiment

out/OUTPUT  : the output of the model on the test dataset -- unlabeled
              chunk data where chunks are wrapped in parentheses

Typical RESULTS output is like:

            Chunker-CLUMP    Iter-78 : 76.2 / 63.9 / 69.5 (   7354 /   2301 /   4147 ) [G = 2.46, P = 2.60]

              Chunker-NPS    Iter-78 : 76.8 / 76.7 / 76.7 (   7414 /   2241 /   2251 ) [G = 2.69, P = 2.60]

This reports on the two evaluations described in the ACL paper,
constituent chunking (here called CLUMP) and NP identification (NPS).
Iter-N reports the number N of iterations of EM required before
convergence.  The next three numbers are constituent precision,
recall, and F-score respectively (here 76.2 / 63.9 / 69.5).  The
following three numbers (here 7354 / 2301 / 4147) are the raw counts
of true positives, false positives and false negatives, respectively.
Finally, in the square brackets is constituent length information
(here, G = 2.46, P = 2.60).  This refers to the average constituent
length of the gold standard annotations (G) and the predicted
constituents (P).  The predicted constituent lengths is the same for
the two evaluations, since its the same output being evaluated against
different annotations.

The chunk.py script comes with a number of command-line options:

-h              Print help message
--help

-t FILE         File input for training
--train=FILE

-s FILE         File input for evaluation and output
--test=FILE

-o DIR          Directory to write output.
--output=DIR

-T X            File type. X may be WSJ, NEGRA, CTB or SPL for
-input_type=X     sentence-per-line.

-m MODEL        Model type.  This may be:
--model=MODEL
                prlg-uni: PRLG with uniform parameter initialization
                
                hmm-uni: HMM with uniform parameter initialization

                prlg-2st: PRLG with "two-stage" initialization. This
                  isn't discussed in the ACL paper, but what this
                  means is that the sequence model is trained in a
                  pseudo-supervised fashion using the output of a
                  simple heuristic model.

                hmm-2st: HMM with two-stage initialization.

                prlg-sup-clump: Train a PRLG model for constituent
                  chunking using gold standard treebank annotations;
                  this uses maximum likelihood model estimation with
                  no iterations of EM

                hmm-sup-clump: Train an HMM model for constituent
                  chunking using gold standard treebank annotations.

                prlg-sup-nps:
                hmm-sup-nps: Train PRLG and HMM models for supervised
                  NP identification.

-f N            Evaluate using only sentences length N or less.
--filter_test=N

-P              Evaluate ignoring all phrasal punctuation as
--nopunc          indicators of phrasal boundaries.  Sentence
                  boundaries are not ignored.

-r              Run model as a right-to-left HMM (or PRLG)
--reverse

-M              Java memory flag, e.g. -Xmx1g. Other Java options can
--memflag=M       be specified with this option

-E X            Run EM until the full dataset likelihood (negative log
--emdelta=X       likelihood is less than X; default = .0001

-S X            Smoothing value, default = .1
--smooth=X

-c X            The coding used to encode constituents as tags.
--coding=X        Options include:

                  BIO: Beginning-inside-outside tagset
                  
                  BILO: Beginning-inside-last-outside tagset

                  BIO_GP: Simulate a second-order sequence model by
                    using current-tag/last-tag pairs

                  BIO_GP_NOSTOP: Simulate a second-order sequence
                    model by using current-tag/last-tag pairs, only
                    not using paired tags for STOP symbols (sentence
                    boundaries and phrasal punctuation)

-I N            Run only N iterations
--iter=N

-C              Run a cascade of chunkers to produce tree-output; this
--cascade         produces different output

2. Cascaded parsing

Running the chunk.py script with the -C (--cascade) option creates a
cascade of chunkers to produce unlabeled constituent tree output (or,
hierarchical bracket output).  Each of the models in the cascade share
the same parameters -- smoothing, tagset, etc -- as specified by the
other command-line options.  The -o parameter still instructs the
script to write output to a specified directory, but a different set
of output is written:

Assuming 'out' is the specified output directory, several
subdirectories are created, of the following form are created:

out/cascade00
out/cascade01

etc.  Each contains further subdirectories:

out/cascade01/train-out
out/cascade01/test-out

These each contain the same chunking information fields as before
(OUTPUT, README, RESULTS, and STATUS, though RESULTS is empty for
most).  Each cascade directory also contains updated train and
evaluation files, e.g.

out/cascade01/next-train
out/cascade01/next-test

These are the datasets modified with pseudowords as stand-ins for
chunks, as described in the ACL paper. 

The expanded constituency parsing output on the evaluation data -- in
other words, the full bracketing for each level of the cascade -- is
written into each subdirectory, e.g. at

out/cascade01/test-eval

Empirical evaluation for all levels is ultimately written to
out/results.  This is a little difficult to read, since the different
levels are not strictly indicated.  But it becomes easier when you
filter it by evaluation.  For instance, to get PARSEEVAL evaluation of
each level, run:

$ grep 'asTrees' < out/results
                  asTrees    asTrees : 53.8 / 16.8 / 25.6 
                  asTrees    asTrees : 53.7 / 25.7 / 34.8 
                  asTrees    asTrees : 51.1 / 30.4 / 38.1 
                  asTrees    asTrees : 50.5 / 32.6 / 39.7 
                  asTrees    asTrees : 50.4 / 32.8 / 39.8 
                  asTrees    asTrees : 50.4 / 32.8 / 39.8 

For NP and PP identification at each level:

$ grep 'NPs Recall' < out/results
               NPs Recall            : 19.6 / 30.9 / 24.0 
               NPs Recall            : 13.0 / 31.6 / 18.5 
               NPs Recall            : 10.8 / 32.3 / 16.1 
               NPs Recall            : 10.1 / 33.0 / 15.4 
               NPs Recall            : 10.0 / 33.0 / 15.4 
               NPs Recall            : 10.0 / 33.0 / 15.4 

$ grep 'PPs Recall' < out/results
               PPs Recall            : 8.1 / 33.6 / 13.1 
               PPs Recall            : 7.4 / 47.1 / 12.9 
               PPs Recall            : 6.1 / 47.9 / 10.8 
               PPs Recall            : 5.9 / 50.5 / 10.6 
               PPs Recall            : 5.9 / 51.1 / 10.6 
               PPs Recall            : 5.9 / 51.1 / 10.6 

Since these evaluations are considering all constituents output by the
model, the interesting metric here is recall (the middle one, here
33.6 to 51.1 for PP recall).

The last cascade directory -- in this example run, out/cascade06 -- is
empty except for the train-out subdirectory.  This serves to indicate
that the model as converged: the model will produce no new
constituents at subsequent cascade levels. 

So, the final model output on the evaluation data is in the
second-to-last cascade directory, in test-eval.  This is
sentence-per-line, with constituents indicated by parentheses.  An
additional bracket for the sentence root is added to each sentence. 

To see a sample, run

$ head out/cascade05/test-eval

assuming that cascade05 is the second-to-last cascade directory, as
here. 

D. Running from the Jar file

Running `ant jar` creates an executable jar file, which has much the
same functionality as the chunk.py script, but offers a couple more
options.  Do use it, run something like:

$ java -Xmx1g -jar upparse.jar chunk \
    -chunkerType PRLG \
    -chunkingStrategy UNIFORM \
    -encoderType BIO \
    -emdelta .0001 \
    -smooth .1 \
    -output testout \
    -train wsj/train/*.mrg \
    -test wsj/23/*.mrg \
    -trainFileType WSJ \
    -testFileType WSJ \
    
First of all, note that you can pass multiple files to upparse.jar,
unlike the chunk script.  Using Bash (indeed, most shells), this means
you can use file name patterns like *.mrg.  On the other hand, you
have to specify the file types directly using -trainFileType and
-testFileType.  

Here are most of the command line options for calling 
`java upparse.jar chunk`:

-chunkerType       HMM or PRLG (see 'model' option above)

-chunkingStrategy  TWOSTAGE or UNIFORM (see 'model' option above)

-encoderType T     Use tagset T to encode constituents e.g. BIO, BILO,
-G T                 etc. (see 'coding' option above)

-noSeg             Evaluate without using phrasal punctuation (see
                     'nopunc' option above) 

-train FILES       Train using specified files

-test FILES        Evaluate using specified files

-trainFileType     WSJ, NEGRA, CTB or SPL (for tokenized
                     sentence-per-line) 

-numtrain N        Train only using the first N sentences

-filterTrain L     Train only using sentences length L or less

-filterTest L      Evaluate on sentences only length L or less

-output D          Output results to directory D (see above)

-iterations N      Train using N iterations of EM

-emdelta X         Train until EM converges, where convergence is when 
                     the percent change in full dataset perplexity
                     (negative log likelihood) is less than X

-smooth V          Set smoothing value to V (see above, and ACL paper)

-evalReportType X  Evaluation report type. Possible values are:
-E X                 PR    : Precision, recall and F-score
                     PRL   : Precision, recall, F-score and information
                             about constituent length
                     PRC   : Precision, recall, F-score and raw counts
                             for true positives, false positives and
                             false negatives
                     PRCL  : Precision, recall, F-score, raw counts and
                             constituent length information
                     PRLcsv: PRL output in CSV format to import into
                             a spreadsheet

-evalTypes E1,E2.. Evaluation types.  This also dictates the format
-e E1,E2..           used in the output. Possible values are:
                     CLUMP : Evaluate using constituent chunks (see 
                       ACL paper)
                     NPS : Evaluate using NP identification
		     PPS : Evaluate using prepositional phrase
                           identification 
                     TREEBANKPREC : Evaluate constituents against a
                       treebank as precision on all treebank
                       constituents. This will also output recall and
                       F-score: ignore these.

-outputType T      This parameter uses many of the same values as
		     -evalTypes. Values CLUMP, NPS and TREEBANKPREC 
		     all output basic chunker output, using
		     parentheses to indicate chunk boundaries.  Other 
		     output formats are specified with:
		     
		     UNDERSCORE : Output chunks as words separated by
		       underscore 

		     UNDERSCORE4CCL : Output chunks as word separated
		       by underscore, also indicate phrasal
		       punctuation by include a semicolon (;)
		       character

-continuousEval    Train model performance on the evaluation dataset
                     through the learning process -- that is, after each
                     iteration of EM, evaluate.  This is interesting to
                     see the model's performance improve (or degrade) as
                     it converges.  NOTE when using this option,
                     predictions will be slightly different, since the
                     vocabulary count V incorporates terms only seen
                     in the evaluation dataset.  For this reason, we
                     do not use this option in experiments whose
                     numerical results we report in the paper.  The
                     different numbers are reported in out/RESULTS.

-outputAll        Write model output on the evaluation data to disk
                     for each iteration of EM.  Each model output is 
                     out/Iter-N for iteration N.  This can create a
                     lot of files and eats up some amount of disk over
                     time.

-reverse          Evaluate as a right-to-left sequence model


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
and section 23 for test.  Assuming the Penn Treebank was downloaded
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
Devel: 886-931 ; 1148 - 1151
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

Assuming the TRAIN is the training file (for WSJ, Negra or CTB), and
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

testout/OUTPUT is the final output of the system on the TEST dataset.

To run experiments on the datasets, but completely ignoring
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
cascade, and once to run on TEST to evaluate.  (This is obviously not
an optimal setup, since the chunker has to train twice at each level
in the cascade.)

This script will print numerical results to screen as it operates.
For info about final model output, and the saved results file, see
above. 

V. Extending and contributing

upparse is open-source software, under an Apache license.  The project
is currently hosted on Bitbucket:

https://bitbucket.org/eponvert/upparse

From there you can download source, clone or the project.  There is an
issue tracker for this project available at

https://bitbucket.org/eponvert/upparse/issues

There is also a project Wiki at 

https://bitbucket.org/eponvert/upparse/wiki

though this resource contains much the same information as this
README.

VI. License

This code is released under the Apache License Version 2.0.  See
LICENSE.txt for details. 

VII. Citation

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
