#
# Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
# @bug 4673442
# @summary remove doPrivileged when creatingting the NotifyHandshakeThread
# @run shell NotifyHandshakeTest.sh
# @author Brad Wetmore
#
# To run independently:  sh NotifyHandshakeTest.sh

if [ "${TESTJAVA}" = "" ]
then
        echo "TESTJAVA not set.  Test cannot execute.  Failed."
        exit 1
fi

if [ "${COMPILEJAVA}" = "" ]; then
        COMPILEJAVA="${TESTJAVA}"
fi

if [ "${TESTSRC}" = "" ]
then
        TESTSRC="."
fi

OS=`uname -s`
case "$OS" in
    Linux | Darwin | AIX )
        FILESEP="/"
        PATHSEP=":"
        ;;

    CYGWIN* )
        FILESEP="/"
        PATHSEP=";"
        ;;

    Windows* )
        FILESEP="\\"
        PATHSEP=";"
        ;;
esac

set -ex

#
# Compile the tests, package into their respective jars
#
${COMPILEJAVA}${FILESEP}bin${FILESEP}javac ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d . \
    ${TESTSRC}${FILESEP}NotifyHandshakeTest.java \
    ${TESTSRC}${FILESEP}NotifyHandshakeTestHeyYou.java
${COMPILEJAVA}${FILESEP}bin${FILESEP}jar ${TESTTOOLVMOPTS} -cvf com.jar \
    com${FILESEP}NotifyHandshakeTest*.class
${COMPILEJAVA}${FILESEP}bin${FILESEP}jar ${TESTTOOLVMOPTS} -cvf edu.jar \
    edu${FILESEP}NotifyHandshakeTestHeyYou.class

#
# Don't want the original class files to be used, because
# we want the jar files with the associated contexts to
# be used.
#
rm -rf com edu

#
# This is the only thing we really care about as far as
# test status goes.
#
${TESTJAVA}${FILESEP}bin${FILESEP}java ${TESTVMOPTS} \
    -Dtest.src=${TESTSRC} \
    -classpath "com.jar${PATHSEP}edu.jar" \
    -Djava.security.manager \
    -Djava.security.policy=${TESTSRC}${FILESEP}NotifyHandshakeTest.policy \
    com.NotifyHandshakeTest
retval=$?

rm com.jar edu.jar

exit $retval
