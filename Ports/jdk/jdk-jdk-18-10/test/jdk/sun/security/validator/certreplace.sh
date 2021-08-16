#
# Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
# @bug 6948803
# @summary CertPath validation regression caused by SHA1 replacement root
#  and MD2 disable feature
# @modules java.base/sun.security.validator
#

if [ "${TESTSRC}" = "" ] ; then
  TESTSRC="."
fi
if [ "${TESTJAVA}" = "" ] ; then
  JAVAC_CMD=`which javac`
  TESTJAVA=`dirname $JAVAC_CMD`/..
  COMPILEJAVA="${TESTJAVA}"
fi

# set platform-dependent variables
OS=`uname -s`
case "$OS" in
  Windows_* )
    FS="\\"
    ;;
  * )
    FS="/"
    ;;
esac

KT="$TESTJAVA${FS}bin${FS}keytool ${TESTTOOLVMOPTS} -storepass changeit \
    -keypass changeit -keystore certreplace.jks -keyalg rsa"
JAVAC=$COMPILEJAVA${FS}bin${FS}javac
JAVA=$TESTJAVA${FS}bin${FS}java

rm -rf certreplace.jks 2> /dev/null

# 1. Generate 3 aliases in a keystore: ca, int, user

$KT -genkeypair -alias ca -dname CN=CA -keyalg rsa -sigalg md2withrsa -ext bc
$KT -genkeypair -alias int -dname CN=Int -keyalg rsa
$KT -genkeypair -alias user -dname CN=User -keyalg rsa

# 2. Signing: ca -> int -> user

$KT -certreq -alias int | $KT -gencert -rfc -alias ca -ext bc \
    | $KT -import -alias int
$KT -certreq -alias user | $KT -gencert -rfc -alias int \
    | $KT -import -alias user

# 3. Create the certchain file

$KT -export -alias user -rfc > certreplace.certs
$KT -export -rfc -alias int >> certreplace.certs
$KT -export -rfc -alias ca >> certreplace.certs

# 4. Upgrade ca from MD2withRSA to SHA256withRSA, remove other aliases and
# make this keystore the cacerts file

$KT -selfcert -alias ca
$KT -delete -alias int
$KT -delete -alias user

# 5. Build and run test

EXTRAOPTS="--add-exports java.base/sun.security.validator=ALL-UNNAMED"
$JAVAC ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} ${EXTRAOPTS} -d . ${TESTSRC}${FS}CertReplace.java
$JAVA ${TESTVMOPTS} ${EXTRAOPTS} CertReplace certreplace.jks certreplace.certs
