#!/bin/sh

if [ -z "$JAVA_MEM_FLAG" ] ; then JAVA_MEM_FLAG="-Xmx512M -Xms512M" ; fi

java $JAVA_MEM_FLAG -cp classes -ea upparse.clinterface.Main $@
