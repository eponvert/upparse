#!/bin/sh

if [ -z $JAVA_MEM_FLAG ] ; then JAVA_MEM_FLAG=-Xmx125m ; fi

java $JAVA_MEM_FLAG -cp classes -ea upparse.Main $@
