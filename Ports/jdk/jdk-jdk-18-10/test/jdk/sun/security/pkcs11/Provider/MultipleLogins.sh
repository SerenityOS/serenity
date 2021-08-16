#
# Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
# @bug 8240256 8269034
# @summary
# @library /test/lib/
# @build jdk.test.lib.util.ForceGC
# @run shell MultipleLogins.sh

# set a few environment variables so that the shell-script can run stand-alone
# in the source directory

# if running by hand on windows, change TESTSRC and TESTCLASSES to "."
if [ "${TESTSRC}" = "" ] ; then
    TESTSRC=`pwd`
fi
if [ "${TESTCLASSES}" = "" ] ; then
    TESTCLASSES=`pwd`
fi

if [ "${TESTCLASSPATH}" = "" ] ; then
    TESTCLASSPATH=`pwd`
fi

if [ "${COMPILEJAVA}" = "" ]; then
    COMPILEJAVA="${TESTJAVA}"
fi
echo TESTSRC=${TESTSRC}
echo TESTCLASSES=${TESTCLASSES}
echo TESTJAVA=${TESTJAVA}
echo COMPILEJAVA=${COMPILEJAVA}
echo ""

# let java test exit if platform unsupported

OS=`uname -s`
case "$OS" in
  Linux )
    FS="/"
    PS=":"
    CP="${FS}bin${FS}cp"
    CHMOD="${FS}bin${FS}chmod"
    ;;
  Darwin )
    FS="/"
    PS=":"
    CP="${FS}bin${FS}cp"
    CHMOD="${FS}bin${FS}chmod"
    ;;
  AIX )
    FS="/"
    PS=":"
    CP="${FS}bin${FS}cp"
    CHMOD="${FS}bin${FS}chmod"
    ;;
  Windows* )
    FS="\\"
    PS=";"
    CP="cp"
    CHMOD="chmod"
    ;;
  CYGWIN* )
    FS="/"
    PS=";"
    CP="cp"
    CHMOD="chmod"
    #
    # javac does not like /cygdrive produced by `pwd`
    #
    TESTSRC=`cygpath -d ${TESTSRC}`
    ;;
  * )
    echo "Unrecognized system!"
    exit 1;
    ;;
esac

# first make cert/key DBs writable

${CP} ${TESTSRC}${FS}..${FS}nss${FS}db${FS}cert8.db ${TESTCLASSES}
${CHMOD} +w ${TESTCLASSES}${FS}cert8.db

${CP} ${TESTSRC}${FS}..${FS}nss${FS}db${FS}key3.db ${TESTCLASSES}
${CHMOD} +w ${TESTCLASSES}${FS}key3.db

# compile test
${COMPILEJAVA}${FS}bin${FS}javac ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} \
        -classpath ${TESTCLASSPATH} \
        -d ${TESTCLASSES} \
        --add-modules jdk.crypto.cryptoki \
        --add-exports jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED \
        ${TESTSRC}${FS}..${FS}..${FS}..${FS}..${FS}..${FS}lib${FS}jdk${FS}test${FS}lib${FS}artifacts${FS}*.java \
        ${TESTSRC}${FS}MultipleLogins.java \
        ${TESTSRC}${FS}..${FS}PKCS11Test.java

TEST_ARGS="${TESTVMOPTS} -classpath ${TESTCLASSPATH} \
        --add-modules jdk.crypto.cryptoki \
        --add-exports jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED \
        -DCUSTOM_DB_DIR=${TESTCLASSES} \
        -DCUSTOM_P11_CONFIG=${TESTSRC}${FS}MultipleLogins-nss.txt \
        -DNO_DEFAULT=true \
        -DNO_DEIMOS=true \
        -Dtest.src=${TESTSRC} \
        -Dtest.classes=${TESTCLASSES} \
        -Djava.security.debug=${DEBUG}"

# run test without security manager
${TESTJAVA}${FS}bin${FS}java ${TEST_ARGS} MultipleLogins || exit 10

# run test with security manager
${TESTJAVA}${FS}bin${FS}java ${TEST_ARGS} MultipleLogins useSimplePolicy || exit 11

echo Done
exit 0
