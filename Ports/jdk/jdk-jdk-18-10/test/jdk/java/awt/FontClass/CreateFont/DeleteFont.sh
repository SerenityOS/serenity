# Copyright (c) 2004, 2008, Oracle and/or its affiliates. All rights reserved.
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

# @test
# @bug 6189812 6380357 6632886 8249142
# @key intermittent
# @summary Verify that temporary font files are deleted on exit.

if [ -z "${TESTSRC}" ]; then
  echo "TESTSRC undefined: defaulting to ."
  TESTSRC=.
fi

if [ -z "${TESTCLASSES}" ]; then
  echo "TESTCLASSES undefined: defaulting to ."
  TESTCLASSES=.
fi

if [ -z "${TESTJAVA}" ]; then
  echo "TESTJAVA undefined: can't continue."
  exit 1
fi

echo "TESTJAVA=${TESTJAVA}"
echo "TESTSRC=${TESTSRC}"
echo "TESTCLASSES=${TESTCLASSES}"
cd ${TESTSRC}
${TESTJAVA}/bin/javac -d ${TESTCLASSES} DeleteFont.java

cd ${TESTCLASSES}

numfiles0=`ls ${TESTCLASSES} | wc -l`
${TESTJAVA}/bin/java ${TESTVMOPTS} -Djava.io.tmpdir=${TESTCLASSES} DeleteFont

if [ $? -ne 0 ]
    then
      echo "Test fails: exception thrown!"
      exit 1
fi

numfiles1=`ls ${TESTCLASSES} | wc -l`

if [ $numfiles0 -ne $numfiles1 ]
    then
      echo "Test fails: tmp file exists!"
      ls ${TESTCLASSES}
      exit 1
fi
exit 0
