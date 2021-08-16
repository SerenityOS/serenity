#!/bin/sh

#
# Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#


AGENT="$1"
APP="$2"

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

OS=`uname -s`
case "$OS" in
   Linux )
      PATHSEP=":"
      ;;

   Windows* | CYGWIN*)
      PATHSEP=";"
      ;;

   # catch all other OSs
   * )
      echo "Unrecognized system!  $OS"
      fail "Unrecognized system!  $OS"
      ;;
esac

JAVAC="${COMPILEJAVA}/bin/javac -g"
JAR="${COMPILEJAVA}/bin/jar"

cp ${TESTSRC}/${AGENT}.java .
cp ${TESTSRC}/${APP}.java .
rm -rf asmlib
mkdir asmlib
cp ${TESTSRC}/asmlib/*.java asmlib
rm -rf bootpath
mkdir -p bootpath/bootreporter
cp ${TESTSRC}/bootreporter/*.java bootpath/bootreporter

cd bootpath
${JAVAC} ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} bootreporter/*.java
cd ..

${JAVAC} ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} \
    --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED ${AGENT}.java asmlib/*.java
${JAVAC} ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -classpath .${PATHSEP}bootpath ${APP}.java

echo "Manifest-Version: 1.0"    >  ${AGENT}.mf
echo Premain-Class: ${AGENT} >> ${AGENT}.mf
echo Boot-Class-Path: bootpath >> ${AGENT}.mf
shift 2
while [ $# != 0 ] ; do
  echo $1 >> ${AGENT}.mf
  shift
done

${JAR} ${TESTTOOLVMOPTS} cvfm ${AGENT}.jar ${AGENT}.mf ${AGENT}*.class asmlib/*.class

# rm -rf  ${AGENT}.java asmlib ${AGENT}.mf ${AGENT}*.class
