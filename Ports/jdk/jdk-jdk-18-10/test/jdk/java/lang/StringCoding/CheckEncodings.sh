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
# @summary Verify that unsupported encodings are handled gracefully.
# @bug 4629543 4785473
#
# @run shell/timeout=300 CheckEncodings.sh

# set platform-dependent variables
OS=`uname -s`
case "$OS" in
  Linux | Darwin | AIX ) ;;
  Windows* | CYGWIN* )
    echo "Passed"; exit 0 ;;
  * ) echo "Unrecognized system!" ;  exit 1 ;;
esac

expectPass() {
  if [ $1 -eq 0 ]
  then echo "--- passed as expected"
  else
    echo "--- failed"
    exit $1
  fi
}

runTest() {
  echo "Testing:" ${1}
  set LC_ALL="${1}"; export LC_ALL
  locale
  ${TESTJAVA}/bin/java ${TESTVMOPTS} -version 2>&1
  expectPass $?
}


locale -a > machine_locales.txt

# ${TESTSRC}/locales.txt contains the list of "fully supported" locales
# as defined by the i18n doc for 1.4
cat ${TESTSRC}/locales.txt machine_locales.txt | sort | uniq > locale_union.txt

for i in `xargs < locale_union.txt` ; do
  runTest ${i}
done

# random strings
for i in FOO 1234 ZZ; do
  runTest ${i}
done
