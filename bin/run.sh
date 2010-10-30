#!/bin/bash



LIB=../lib

CP=../classes
for i in $LIB/*.jar
do
    CP=$CP:$i
done


LOG="-Dlog4j.configuration=file:$HOME/log4j.properties"

java -classpath $CP $LOG $FLAGS $XFLAGS $PROF -Dresolve.dns=false org.jboss.client.ChatClient $*

