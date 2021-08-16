#!/bin/sh
AGENT="$1"
OTHER="$2"
shift 2

if [ "${TESTSRC}" = "" ]
then
  echo "TESTSRC not set.  Test cannot execute.  Failed."
  exit 1
fi
echo "TESTSRC=${TESTSRC}"

if [ "${TESTJAVA}" = "" ]
then
  echo "TESTJAVA not set.  Test cannot execute.  Failed."
  exit 1
fi
echo "TESTJAVA=${TESTJAVA}"

if [ "${COMPILEJAVA}" = "" ]
then
  COMPILEJAVA="${TESTJAVA}"
fi
echo "COMPILEJAVA=${COMPILEJAVA}"

if [ "${TESTCLASSES}" = "" ]
then
  echo "TESTCLASSES not set.  Test cannot execute.  Failed."
  exit 1
fi

JAVAC="${COMPILEJAVA}/bin/javac -g"
JAR="${COMPILEJAVA}/bin/jar"

cp ${TESTSRC}/${AGENT}.java ${TESTSRC}/${OTHER}.java .
${JAVAC} ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} ${AGENT}.java ${OTHER}.java

echo "Manifest-Version: 1.0"    >  ${AGENT}.mf
echo Premain-Class: ${AGENT} >> ${AGENT}.mf
while [ $# != 0 ] ; do
  echo $1 >> ${AGENT}.mf
  shift
done


${JAR} ${TESTTOOLVMOPTS} cvfm ${AGENT}.jar ${AGENT}.mf ${AGENT}*.class ${OTHER}*.java
