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
# @run shell jrunscript-argsTest.sh
# @summary Test passing of script arguments from command line

. ${TESTSRC-.}/common.sh

setup
${JAVA} ${TESTVMOPTS} ${TESTJAVAOPTS} -cp ${TESTCLASSES} CheckEngine
if [ $? -eq 2 ]; then
    echo "No js engine found and engine not required; test vacuously passes."
    exit 0
fi

# we check whether "excess" args are passed as script arguments

${JRUNSCRIPT} -l nashorn -J-Djava.awt.headless=true -f - hello world <<EOF

if (typeof(arguments) == 'undefined') { println("arguments expected"); exit(1); }

if (arguments.length != 2) { println("2 arguments are expected here"); exit(1); }

if (arguments[0] != 'hello') { println("First arg should be 'hello'"); exit(1); }
  
if (arguments[1] != 'world') { println("Second arg should be 'world'"); exit(1); }

println("Passed");
exit(0);
EOF

if [ $? -ne 0 ]; then
    exit 1
fi
