#! /bin/sh

#
# Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
    PATHSEP=":"
    FILESEP="/"
    ;;
  Windows* )
    PATHSEP=";"
    FILESEP="\\"
    ;;
  * )
    echo "Unrecognized system!"
    exit 1;
    ;;
esac

# compile the test program
cd ${TESTSRC}${FILESEP}
rm GrantAllPermToExtWhenNoPolicy.class
${COMPILEJAVA}${FILESEP}bin${FILESEP}javac ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} \
    -d ${TESTSRC}${FILESEP} ${TESTSRC}${FILESEP}SomeExtensionClass.java
${COMPILEJAVA}${FILESEP}bin${FILESEP}javac ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} \
    -d ${TESTSRC}${FILESEP} ${TESTSRC}${FILESEP}GrantAllPermToExtWhenNoPolicy.java

# create the extension JAR file
cd ${TESTCLASSES}
${COMPILEJAVA}${FILESEP}bin${FILESEP}jar cvf SomeExt.jar SomeExtensionClass*.class
rm SomeExtensionClass.class

# move the extension JAR file to the extension directory
mv SomeExt.jar ${TESTJAVA}${FILESEP}jre${FILESEP}lib${FILESEP}ext

# remove the system policy file
mv \
 ${TESTJAVA}${FILESEP}jre${FILESEP}lib${FILESEP}security${FILESEP}java.policy \
 ${TESTJAVA}${FILESEP}jre${FILESEP}lib${FILESEP}security${FILESEP}tmp_pol

# run the test program
${TESTJAVA}${FILESEP}bin${FILESEP}java ${TESTVMOPTS} -Djava.security.manager \
 GrantAllPermToExtWhenNoPolicy

# save error status
status=$?

# restore system policy and remove extension JAR file
mv ${TESTJAVA}${FILESEP}jre${FILESEP}lib${FILESEP}security${FILESEP}tmp_pol \
 ${TESTJAVA}${FILESEP}jre${FILESEP}lib${FILESEP}security${FILESEP}java.policy
rm ${TESTJAVA}${FILESEP}jre${FILESEP}lib${FILESEP}ext${FILESEP}SomeExt.jar

exit $status
