#!/bin/sh

# Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
# @bug 6332666 6863624 7180362 8003846 8074350 8074351 8130246 8149735 7102969
#      8157138 8190904
# @summary tests the capability of replacing the currency data with user
#     specified currency properties file
# @build PropertiesTest
# @run shell/timeout=600 PropertiesTest.sh

if [ "${TESTSRC}" = "" ]
then
  echo "TESTSRC not set.  Test cannot execute.  Failed."
  exit 1
fi
echo "TESTSRC=${TESTSRC}"
if [ "${TESTJAVA}" = "" ]
then
  echo "TESTJAVA not set.  Test cannot execute.  Failed."
  exit 1
fi
echo "TESTJAVA=${TESTJAVA}"
if [ "${TESTCLASSES}" = "" ]
then
  echo "TESTCLASSES not set.  Test cannot execute.  Failed."
  exit 1
fi
echo "TESTCLASSES=${TESTCLASSES}"
echo "CLASSPATH=${CLASSPATH}"

# set platform-dependent variables
OS=`uname -s`
case "$OS" in
  Linux | Darwin | AIX )
    PS=":"
    FS="/"
    ;;
  Windows* )
    PS=";"
    FS="/"
    ;;
  CYGWIN* )
    PS=";"
    FS="/"
    TESTJAVA=`cygpath -u ${TESTJAVA}`
    ;;
  * )
    echo "Unrecognized system!"
    exit 1;
    ;;
esac

failures=0

run() {
    echo ''
    ${TESTJAVA}${FS}bin${FS}java ${TESTVMOPTS} -cp ${TESTCLASSES} $* 2>&1
    if [ $? != 0 ]; then failures=`expr $failures + 1`; fi
}

PROPS=${TESTSRC}${FS}currency.properties


# Dump built-in currency data

run PropertiesTest -d dump1
if [ ! -f dump1 ]; then  echo "file dump1 not created. Test cannot execute.  Failed."; exit 1; fi

# Dump built-in currency data + overrides in properties file specified
# by system property.

run -Djava.util.currency.data=${PROPS} PropertiesTest -d dump2
if [ ! -f dump2 ]; then  echo "file dump2 not created. Test cannot execute.  Failed."; exit 1; fi
run PropertiesTest -c dump1 dump2 ${PROPS}


# Dump built-in currency data + overrides in properties file copied into
# JRE image.

# Make a private copy of the jdk so we can write to the properties file location
# without disturbing other users, including concurrently executing tests.
WRITABLEJDK=.${FS}testjava
cp -H -R $TESTJAVA $WRITABLEJDK || exit 1
PROPLOCATION=${WRITABLEJDK}${FS}lib
chmod -R u+w $WRITABLEJDK || exit 1
cp ${PROPS} $PROPLOCATION || exit 1
echo "Properties location: ${PROPLOCATION}"

# run
echo ''
${WRITABLEJDK}${FS}bin${FS}java ${TESTVMOPTS} -cp ${TESTCLASSES} PropertiesTest -d dump3
if [ $? != 0 ]; then failures=`expr $failures + 1`; fi
if [ ! -f dump3 ]; then  echo "file dump3 not created. Test cannot execute.  Failed."; exit 1; fi

# run bug7102969 test
echo ''
${WRITABLEJDK}${FS}bin${FS}java ${TESTVMOPTS} -cp ${TESTCLASSES} PropertiesTest bug7102969
if [ $? != 0 ]; then failures=`expr $failures + 1`; fi

# run bug8157138 test
echo ''
${WRITABLEJDK}${FS}bin${FS}java ${TESTVMOPTS} -cp ${TESTCLASSES} PropertiesTest bug8157138
if [ $? != 0 ]; then failures=`expr $failures + 1`; fi

# run bug8190904 test
echo ''
${WRITABLEJDK}${FS}bin${FS}java ${TESTVMOPTS} -cp ${TESTCLASSES} PropertiesTest bug8190904
if [ $? != 0 ]; then failures=`expr $failures + 1`; fi

# Cleanup
rm -rf $WRITABLEJDK

# compare the two dump files
run PropertiesTest -c dump1 dump3 ${PROPS}


# Results
echo ''
if [ $failures -gt 0 ];
  then echo "$failures tests failed";
  else echo "All tests passed"; fi
exit $failures
