import sys
from os import listdir, sep

indir = sys.argv[1]
files = listdir(indir)
outdir = sys.argv[2]

for f in files:
	try:
		fh = open(indir + sep + f, 'rb')
		content = unicode(fh.read(), 'gb18030')
		fh.close()
		fh = open(outdir + sep + f, 'wb')
		fh.write(content.encode('utf-8'))
		fh.close()

	except UnicodeDecodeError:

		fh.close()
		print >>sys.stderr, "Problem with file", f
		fh1 = open(indir + sep + f, 'rb')
		fh2 = open(outdir + sep + f, 'wb')

		while True:
			c = fh1.read(1)
			if c == '':
				break
			
			try:
				fh2.write(unicode(c,'gb18030').encode('utf-8'))

			except UnicodeDecodeError:

				c += fh1.read(1)
				if c == '':
					break
					
				try:
					fh2.write(unicode(c, 'gb18030').encode('utf-8'))

				except UnicodeDecodeError:
					fh2.write(u'X')

		fh1.close()
		fh2.close()

