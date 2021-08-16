#
# Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
# @bug 7122253 8016838
# @summary Retransform a big class.
# @author Daniel D. Daugherty
#
# @key intermittent
# @modules java.instrument
#          java.management
# @run shell MakeJAR4.sh RetransformBigClassAgent SimpleIdentityTransformer 'Can-Retransform-Classes: true'
# @run build BigClass RetransformBigClassApp NMTHelper
# @run shell/timeout=600 RetransformBigClass.sh
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

# Does this VM support the 'detail' level of NMT?
"${JAVA}" ${TESTVMOPTS} -XX:NativeMemoryTracking=detail -version
if [ "$?" = 0 ]; then
    NMT=-XX:NativeMemoryTracking=detail
else
    NMT=-XX:NativeMemoryTracking=summary
fi

"${JAVA}" ${TESTVMOPTS} \
    -Xlog:redefine+class+load=debug,redefine+class+load+exceptions=info ${NMT} \
    -javaagent:RetransformBigClassAgent.jar=BigClass.class \
    -classpath "${TESTCLASSES}" RetransformBigClassApp \
    > output.log 2>&1
result=$?

cat output.log

if [ "$result" = 0 ]; then
    echo "PASS: RetransformBigClassApp exited with status of 0."
else
    echo "FAIL: RetransformBigClassApp exited with status of $result"
    exit "$result"
fi

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
