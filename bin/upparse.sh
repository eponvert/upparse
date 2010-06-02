#!/bin/sh

if [ -z $JAVA_MEM_FLAG ] ; then JAVA_MEM_FLAG="-Xmx1024M -Xms1024M" ; fi

java $JAVA_MEM_FLAG -cp classes -ea upparse.Main $@
