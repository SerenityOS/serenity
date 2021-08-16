#!/bin/ksh -p
#
# Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#
#   @test
#   @bug        8257809
#   @summary    Tests that there are no JNI warnings.
#   @compile GetImageJNICheck.java
#   @run shell/timeout=300 GetImageJNICheck.sh
#
OS=`uname`

if [ "${TESTJAVA}" = "" ]
then
  echo "TESTJAVA not set.  Test cannot execute.  Failed."
  exit 1
fi

# pick up the compiled class files.
if [ -z "${TESTCLASSES}" ]; then
  CP="."
  $TESTJAVA/bin/javac GetImageJNICheck.java
else
  CP="${TESTCLASSES}"
fi

$TESTJAVA/bin/java ${TESTVMOPTS} \
    -cp "${CP}" -Xcheck:jni GetImageJNICheck | grep ReleasePrimitiveArrayCritical > "${CP}"/log.txt

#if [ $? -ne 0 ]
#    then
#      echo "Test fails: exception thrown!"
#      exit 1
#fi

# any messages logged indicate a failure.
if [ -s "${CP}"/log.txt ]; then
    echo "Test failed"
    cat "${CP}"/log.txt
    exit 1
fi

echo "Test passed"
exit 0
