#
# Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
# @bug 7174978
# @summary Redefine a class with a method stored in a backtrace.
# @author Stefan Karlsson
#
# @modules java.instrument
#          jdk.management
# @run shell MakeJAR3.sh RedefineMethodInBacktraceAgent 'Can-Redefine-Classes: true'
# @run build RedefineMethodInBacktraceTarget RedefineMethodInBacktraceApp
# @run shell RedefineMethodInBacktrace.sh
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

cp "${TESTSRC}"/RedefineMethodInBacktraceTarget_2.java \
    RedefineMethodInBacktraceTarget.java
"${JAVAC}" ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d . RedefineMethodInBacktraceTarget.java

cp "${TESTSRC}"/RedefineMethodInBacktraceTargetB_2.java \
    RedefineMethodInBacktraceTargetB.java
"${JAVAC}" ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d . RedefineMethodInBacktraceTargetB.java

"${JAVA}" ${TESTVMOPTS} -javaagent:RedefineMethodInBacktraceAgent.jar \
    -XX:+AllowRedefinitionToAddDeleteMethods \
    -classpath "${TESTCLASSES}" RedefineMethodInBacktraceApp > output.log 2>&1
RUN_RESULT=$?

if [ $RUN_RESULT != 0 ]; then
    echo "FAIL: the run failed with exit code '$RUN_RESULT'"
	exit $RUN_RESULT
fi

cat output.log

MESG="Test failed"
grep "$MESG" output.log
result=$?
if [ "$result" = 0 ]; then
    echo "FAIL: found '$MESG' in the test output"
    result=1
else
    echo "PASS: did NOT find '$MESG' in the test output"
    result=0
fi

exit $result
