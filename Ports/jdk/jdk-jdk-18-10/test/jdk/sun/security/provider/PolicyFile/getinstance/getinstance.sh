#! /bin/sh

#
# Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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
# @author  Ram Marti
# @bug 4350951
# @summary 4350951 assumes permission constructor with 2 string params

# set a few environment variables so that the shell-script can run stand-alone
# in the source directory
if [ "${TESTSRC}" = "" ] ; then
   TESTSRC="."
fi

if [ "${TESTCLASSES}" = "" ] ; then
   TESTCLASSES="."
fi

if [ "${TESTJAVA}" = "" ] ; then
   echo "TESTJAVA not set.  Test cannot execute."
   echo "FAILED!!!"
   exit 1
fi

if [ "${COMPILEJAVA}" = "" ]; then
    COMPILEJAVA="${TESTJAVA}"
fi

# set platform-dependent variables
OS=`uname -s`
case "$OS" in
  Linux )
    PS=":"
    FS="/"
    ;;
  Darwin )
    PS=":"
    FS="/"
    ;;
  AIX )
    PS=":"
    FS="/"
    ;;
  CYGWIN* )
    PS=";"
    FS="/"
    ;;
  Windows* )
    PS=";"
    FS="\\"
    ;;
  * )
    echo "Unrecognized system!"
    exit 1;
    ;;
esac

if [ ! -d ${TESTCLASSES}${FS}boot ]; then
        mkdir -p ${TESTCLASSES}${FS}boot
fi
if [ ! -d ${TESTCLASSES}${FS}app ]; then
        mkdir -p ${TESTCLASSES}${FS}app
fi

cd ${TESTSRC}${FS}
${COMPILEJAVA}${FS}bin${FS}javac ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d ${TESTCLASSES}${FS}boot \
        ${TESTSRC}${FS}NoArgPermission.java
${COMPILEJAVA}${FS}bin${FS}javac ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d ${TESTCLASSES}${FS}boot \
        ${TESTSRC}${FS}OneArgPermission.java
${COMPILEJAVA}${FS}bin${FS}javac ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d ${TESTCLASSES}${FS}boot \
        ${TESTSRC}${FS}TwoArgPermission.java
${COMPILEJAVA}${FS}bin${FS}javac ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d ${TESTCLASSES}${FS}boot \
        ${TESTSRC}${FS}TwoArgNullActionsPermission.java
${COMPILEJAVA}${FS}bin${FS}javac ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d ${TESTCLASSES}${FS}app \
        ${TESTSRC}${FS}GetInstance.java

${TESTJAVA}${FS}bin${FS}java ${TESTVMOPTS}  \
-Xbootclasspath/a:"${TESTCLASSES}${FS}boot" \
-classpath "${TESTCLASSES}${FS}app" -Djava.security.manager \
-Djava.security.policy=GetInstance.policy \
GetInstance

# Save error status
status1=$?

# print error message
if [ $status1 -ne 0 ]; then
     echo "Failed on first test"
fi

${TESTJAVA}${FS}bin${FS}java ${TESTVMOPTS}  \
-classpath "${TESTCLASSES}${FS}boot${PS}${TESTCLASSES}${FS}app" \
-Djava.security.manager \
-Djava.security.policy=GetInstance.policy \
GetInstance

# Save error status
status2=$?

# print error message
if [ $status2 -ne 0 ]; then
     echo "Failed on second test"
fi

#
# Exit ok?
#
if [ $status1 -ne 0 ]; then
     exit $status1
fi

if [ $status2 -ne 0 ]; then
     exit $status2
fi
