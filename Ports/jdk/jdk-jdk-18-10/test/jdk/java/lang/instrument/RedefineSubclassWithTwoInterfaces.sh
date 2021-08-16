#
# Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
# @bug 7182152
# @bug 8007935
# @summary Redefine a subclass that implements two interfaces and
#   verify that the right methods are called.
# @author Daniel D. Daugherty
#
# @run shell MakeJAR3.sh RedefineSubclassWithTwoInterfacesAgent 'Can-Redefine-Classes: true'
# @run build RedefineSubclassWithTwoInterfacesApp
# @run shell RedefineSubclassWithTwoInterfaces.sh
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

echo "INFO: building the replacement classes."

cp "${TESTSRC}"/RedefineSubclassWithTwoInterfacesTarget_1.java \
    RedefineSubclassWithTwoInterfacesTarget.java
cp "${TESTSRC}"/RedefineSubclassWithTwoInterfacesImpl_1.java \
    RedefineSubclassWithTwoInterfacesImpl.java
"${JAVAC}" ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} \
    -cp "${TESTCLASSES}" -d . \
    RedefineSubclassWithTwoInterfacesTarget.java \
    RedefineSubclassWithTwoInterfacesImpl.java 
status="$?"
if [ "$status" != 0 ]; then
    echo "FAIL: compile of *_1.java files failed."
    exit "$status"
fi

mv RedefineSubclassWithTwoInterfacesTarget.java \
    RedefineSubclassWithTwoInterfacesTarget_1.java
mv RedefineSubclassWithTwoInterfacesTarget.class \
    RedefineSubclassWithTwoInterfacesTarget_1.class
mv RedefineSubclassWithTwoInterfacesImpl.java \
    RedefineSubclassWithTwoInterfacesImpl_1.java
mv RedefineSubclassWithTwoInterfacesImpl.class \
    RedefineSubclassWithTwoInterfacesImpl_1.class

echo "INFO: launching RedefineSubclassWithTwoInterfacesApp"

"${JAVA}" ${TESTVMOPTS} \
    -Xlog:redefine+class+load=trace,redefine+class+load+exceptions=trace,redefine+class+timer=trace,redefine+class+obsolete=trace,redefine+class+obsolete+metadata=trace,redefine+class+constantpool=trace \
    -javaagent:RedefineSubclassWithTwoInterfacesAgent.jar \
    -classpath "${TESTCLASSES}" \
    RedefineSubclassWithTwoInterfacesApp > output.log 2>&1
status="$?"

echo "INFO: <begin output.log>"
cat output.log
echo "INFO: <end output.log>"

if [ "$status" != 0 ]; then
    echo "FAIL: RedefineSubclassWithTwoInterfacesApp failed."
    exit "$status"
fi

# When this bug manifests, RedefineClasses() will fail to update
# one of the itable entries to refer to the new method. The log
# will include the following line when the bug occurs:
#
#     guarantee(false) failed: OLD and/or OBSOLETE method(s) found
#
# If this guarantee happens, the test should fail in the status
# check above, but just in case it doesn't, we check for "guarantee".
#

FAIL_MESG="guarantee"
grep "$FAIL_MESG" output.log
status=$?
if [ "$status" = 0 ]; then
    echo "FAIL: found '$FAIL_MESG' in the test output."
    result=1
else
    echo "INFO: did NOT find '$FAIL_MESG' in the test output."
    # be optimistic here
    result=0
fi

PASS1_MESG="before any redefines"
cnt=`grep "$PASS1_MESG" output.log | grep 'version-0' | wc -l`
# no quotes around $cnt so any whitespace from 'wc -l' is ignored
if [ $cnt = 2 ]; then
    echo "INFO: found 2 version-0 '$PASS1_MESG' mesgs."
else
    echo "FAIL: did NOT find 2 version-0 '$PASS1_MESG' mesgs."
    echo "INFO: cnt='$cnt'"
    echo "INFO: grep '$PASS1_MESG' output:"
    grep "$PASS1_MESG" output.log
    result=1
fi

PASS2_MESG="after redefine"
cnt=`grep "$PASS2_MESG" output.log | grep 'version-1' | wc -l`
# no quotes around $cnt so any whitespace from 'wc -l' is ignored
if [ $cnt = 2 ]; then
    echo "INFO: found 2 version-1 '$PASS2_MESG' mesgs."
else
    echo "FAIL: did NOT find 2 version-1 '$PASS2_MESG' mesgs."
    echo "INFO: cnt='$cnt'"
    echo "INFO: grep '$PASS2_MESG' output:"
    grep "$PASS2_MESG" output.log
    result=1
fi

if [ "$result" = 0 ]; then
    echo "PASS: test passed both positive and negative output checks."
fi

exit $result
