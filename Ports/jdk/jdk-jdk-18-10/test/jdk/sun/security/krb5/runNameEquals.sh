#
# Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
# @bug 6317711 6944847 8024046
# @summary Ensure the GSSName has the correct impl which respects
# the contract for equals and hashCode across different configurations.

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

NATIVE=false

# set platform-dependent variables
OS=`uname -s`
case "$OS" in
  Linux | Darwin )
    PATHSEP=":"
    FILESEP="/"
    NATIVE=true
    # Not all *nix has native GSS libs installed
    krb5-config --libs 2> /dev/null
    if [ $? != 0 ]; then
        # Fedora has a different path
        /usr/kerberos/bin/krb5-config --libs 2> /dev/null
        if [ $? != 0 ]; then
            NATIVE=false
        fi
    fi
    ;;
  AIX )
    PATHSEP=":"
    FILESEP="/"
    ;;
  CYGWIN* )
    PATHSEP=";"
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

TEST=Krb5NameEquals

${COMPILEJAVA}${FILESEP}bin${FILESEP}javac ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} \
    -d ${TESTCLASSES}${FILESEP} \
    ${TESTSRC}${FILESEP}${TEST}.java

EXIT_STATUS=0

if [ "${NATIVE}" = "true" ] ; then
    echo "Testing native provider"
    ${TESTJAVA}${FILESEP}bin${FILESEP}java ${TESTVMOPTS} \
        -classpath ${TESTCLASSES} \
        -Dsun.security.jgss.native=true \
        ${TEST}
    if [ $? != 0 ] ; then
        echo "Native provider fails"
        EXIT_STATUS=1
        if [ "$OS" = "Linux" -a `arch` = "x86_64" ]; then
            ${TESTJAVA}${FILESEP}bin${FILESEP}java -XshowSettings:properties -version 2> allprop
            cat allprop | grep sun.arch.data.model | grep 32
            if [ "$?" = "0" ]; then
                echo "Running 32-bit JDK on 64-bit Linux. Maybe only 64-bit library is installed."
                echo "Please manually check if this is the case. Treated as PASSED now."
                EXIT_STATUS=0
            fi
        fi
    fi
fi

echo "Testing java provider"
${TESTJAVA}${FILESEP}bin${FILESEP}java ${TESTVMOPTS} \
        -classpath ${TESTCLASSES} \
        -Djava.security.krb5.realm=R \
        -Djava.security.krb5.kdc=127.0.0.1 \
        ${TEST}
if [ $? != 0 ] ; then
    echo "Java provider fails"
    EXIT_STATUS=1
fi

exit ${EXIT_STATUS}
