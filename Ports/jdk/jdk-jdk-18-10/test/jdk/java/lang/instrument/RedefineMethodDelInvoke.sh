#
# Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
# @bug 8042796
# @summary jvmtiRedefineClasses.cpp: guarantee(false) failed: OLD and/or OBSOLETE method(s) found
# @author Daniel D. Daugherty
# @author Serguei Spitsyn
#
# @run shell MakeJAR3.sh RedefineMethodDelInvokeAgent 'Can-Redefine-Classes: true'
# @run build RedefineMethodDelInvokeApp
# @run shell RedefineMethodDelInvoke.sh
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

cp "${TESTSRC}"/RedefineMethodDelInvokeTarget_1.java \
    RedefineMethodDelInvokeTarget.java
"${JAVAC}" ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d . RedefineMethodDelInvokeTarget.java
mv RedefineMethodDelInvokeTarget.java RedefineMethodDelInvokeTarget_1.java
mv RedefineMethodDelInvokeTarget.class RedefineMethodDelInvokeTarget_1.class

cp "${TESTSRC}"/RedefineMethodDelInvokeTarget_2.java \
    RedefineMethodDelInvokeTarget.java
"${JAVAC}" ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d . RedefineMethodDelInvokeTarget.java
mv RedefineMethodDelInvokeTarget.java RedefineMethodDelInvokeTarget_2.java
mv RedefineMethodDelInvokeTarget.class RedefineMethodDelInvokeTarget_2.class

"${JAVA}" ${TESTVMOPTS} -javaagent:RedefineMethodDelInvokeAgent.jar \
    -XX:+AllowRedefinitionToAddDeleteMethods \
    -classpath "${TESTCLASSES}" RedefineMethodDelInvokeApp > output.log 2>&1

result=$?
if [ "$result" = 0 ]; then
    echo "The test returned expected exit code: $result"
else
    echo "FAIL: the test returned unexpected exit code: $result"
    exit $result
fi

cat output.log

MESG="Exception"
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
