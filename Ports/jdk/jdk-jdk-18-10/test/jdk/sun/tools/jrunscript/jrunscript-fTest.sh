#!/bin/sh

#
# Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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
# @bug 6265810 6705893
# @build CheckEngine
# @run shell jrunscript-fTest.sh
# @summary Test that output of 'jrunscript -f' matches the dash-f.out file

. ${TESTSRC-.}/common.sh

setup
${JAVA} ${TESTVMOPTS} ${TESTJAVAOPTS} -cp ${TESTCLASSES} CheckEngine
if [ $? -eq 2 ]; then
    echo "No js engine found and engine not required; test vacuously passes."
    exit 0
fi

# -f option used with JavaScript as language chosen explicitly
# with -l option

rm -f jrunscript-fTest.out 2>/dev/null
${JRUNSCRIPT} -J-Dnashorn.args.prepend=--no-deprecation-warning -J-Djava.awt.headless=true -l nashorn -f ${TESTSRC}/hello.js > jrunscript-fTest.out 2>&1

$golden_diff jrunscript-fTest.out ${TESTSRC}/dash-f.out
if [ $? != 0 ]
then
  echo "Output of jrunscript -f differ from expected output. Failed."
  rm -f jrunscript-fTest.out 2>/dev/null
  exit 1
fi

rm -f jrunscript-fTest.out
echo "Passed"
exit 0
