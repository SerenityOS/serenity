#
# Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

# @test
# @bug 8081729 8140314
# @summary Test external plugin as classpath jar and as a modular jar.
#          Test both cases with and without a security manager.

set -e

exception=0

if [ -z "$TESTJAVA" ]; then
  if [ $# -lt 1 ]; then  echo "No Java path specified. Exiting."; fi
  if [ $# -lt 1 ]; then exit 1; fi
  TESTJAVA="$1"; shift
  COMPILEJAVA="${TESTJAVA}"
  TESTSRC="`pwd`"
  TESTCLASSES="`pwd`"
fi

JAVAC="$COMPILEJAVA/bin/javac"
JAR="$COMPILEJAVA/bin/jar"
JAVA="$TESTJAVA/bin/java ${TESTVMOPTS}"

TESTDIR="$TESTCLASSES/classes"
PLUGINDIR="$TESTCLASSES/classes"
mkdir -p $TESTDIR
$JAVAC -d $TESTDIR `find $TESTSRC/src/simptest -name "*.java"`

# compile the plugin java sources and services file into a temp location.

mkdir -p $TESTCLASSES/tmpdir/simp
cp -r $TESTSRC/src/simp/META-INF $TESTCLASSES/tmpdir
$JAVAC -d $TESTCLASSES/tmpdir `find $TESTSRC/src/simp -name "*.java"`

# create modular jar file (inc. module-info.java) from the class files.
mkdir -p $PLUGINDIR
$JAR cf $PLUGINDIR/simp.jar -C $TESTCLASSES/tmpdir META-INF/services \
    -C $TESTCLASSES/tmpdir module-info.class -C $TESTCLASSES/tmpdir simp

OS=`uname -s`
case "$OS" in
  Windows_* | CYGWIN* )
CPSEP=";"
  ;;
  * )
CPSEP=":"
  ;;
esac

# expect to find SimpReader via jar on classpath.
# Will be treated as a regular jar.
echo "Test classpath jar .. "
$JAVA  -cp ${TESTDIR}${CPSEP}${PLUGINDIR}/simp.jar simptest.TestSIMPPlugin
if [ $? -ne 0 ]; then
    exception=1
      echo "Classpath test failed: exception thrown!"
fi
echo "Test classpath jar with security manager .."
$JAVA -Djava.security.manager -cp .${CPSEP}${TESTDIR}${CPSEP}${PLUGINDIR}/simp.jar simptest.TestSIMPPlugin
if [ $? -ne 0 ]; then
    exception=1
    echo "Classpath + SecurityManager test failed: exception thrown!"
fi

# expect to find SimpReader on module path
echo "Test modular jar .. "
$JAVA --module-path $PLUGINDIR -cp $TESTDIR simptest.TestSIMPPlugin

if [ $? -ne 0 ]; then
    exception=1
    echo "modular jar test failed: exception thrown!"
fi

echo "Test modular jar with security manager .."
$JAVA -Djava.security.manager --module-path $PLUGINDIR -cp $TESTDIR simptest.TestSIMPPlugin
if [ $? -ne 0 ]; then
    exception=1
    echo "modular jar with security manager test failed: exception thrown!"
fi

if [ $exception -ne 0 ]; then
    echo "TEST FAILED"
    exit 1
fi
exit 0
