#
# Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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

EXTRAOPTS="--add-exports java.base/jdk.internal.math=ALL-UNNAMED"
LOCALEOPTS="-Djava.locale.providers=CLDR"
${COMPILEJAVA}/bin/javac ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} ${EXTRAOPTS} -cp ${TESTSRC} -d . \
    ${TESTSRC}/Basic.java

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
  TZ="${1}"; export TZ
  echo "  " $TZ
  ${TESTJAVA}/bin/java ${TESTVMOPTS} ${EXTRAOPTS} ${LOCALEOPTS} Basic
  expectPass $?
}

runTest US/Pacific
runTest Asia/Novosibirsk

