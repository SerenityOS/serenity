#
# Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
# @bug 7022100
# @summary Method annotations are incorrectly set when redefining classes.
# @author Stefan Karlsson
#
# @run shell MakeJAR3.sh RedefineMethodWithAnnotationsAgent 'Can-Redefine-Classes: true'
# @run build RedefineMethodWithAnnotationsTarget RedefineMethodWithAnnotationsApp RedefineMethodWithAnnotationsAnnotations
# @run shell RedefineMethodWithAnnotations.sh
#

if [ "${TESTJAVA}" = "" ]
then
  echo "TESTJAVA not set.  Test cannot execute.  Failed."
  exit 1
fi

if [ "${COMPILEJAVA}" = "" ]
then
  COMPILEJAVA="${TESTJAVA}"
fi
echo "COMPILEJAVA=${COMPILEJAVA}"

if [ "${TESTSRC}" = "" ]
then
  echo "TESTSRC not set.  Test cannot execute.  Failed."
  exit 1
fi

if [ "${TESTCLASSES}" = "" ]
then
  echo "TESTCLASSES not set.  Test cannot execute.  Failed."
  exit 1
fi

JAVAC="${COMPILEJAVA}"/bin/javac
JAVA="${TESTJAVA}"/bin/java

cp "${TESTSRC}"/RedefineMethodWithAnnotationsTarget_2.java \
    RedefineMethodWithAnnotationsTarget.java
cp "${TESTSRC}"/RedefineMethodWithAnnotationsAnnotations.java \
    RedefineMethodWithAnnotationsAnnotations.java

"${JAVAC}" ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d . \
    RedefineMethodWithAnnotationsTarget.java \
    RedefineMethodWithAnnotationsAnnotations.java

"${JAVA}" ${TESTVMOPTS} -javaagent:RedefineMethodWithAnnotationsAgent.jar \
    -XX:+UnlockDiagnosticVMOptions -XX:+StressLdcRewrite -XX:+IgnoreUnrecognizedVMOptions \
    -cp "${TESTCLASSES}" RedefineMethodWithAnnotationsApp > output.log 2>&1
cat output.log

MESG="Exception|fatal"
egrep "$MESG" output.log
result=$?
if [ "$result" = 0 ]; then
    echo "FAIL: found '$MESG' in the test output"
    result=1
else
    echo "PASS: did NOT find '$MESG' in the test output"
    result=0
fi

exit $result
