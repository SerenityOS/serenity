#!/bin/sh

#
# Copyright (c) 2005, 2008, Oracle and/or its affiliates. All rights reserved.
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
# @run shell jrunscript-cpTest.sh
# @summary Test -cp option to set classpath

. ${TESTSRC-.}/common.sh

setup
${JAVA} ${TESTVMOPTS} ${TESTJAVAOPTS} -cp ${TESTCLASSES} CheckEngine
if [ $? -eq 2 ]; then
    echo "No js engine found and engine not required; test vacuously passes."
    exit 0
fi

rm -f Hello.class
${JAVAC} ${TESTTOOLVMOPTS} ${TESTJAVACOPTS} ${TESTSRC}/Hello.java -d .

# we check whether classpath setting for app classes
# work with jrunscript. Script should be able to
# access Java class "Hello".

${JRUNSCRIPT} -l nashorn -cp . <<EOF
var v;  
try { v = new Packages.Hello(); } catch (e) { println(e); exit(1) }
if (v.string != 'hello') { println("Unexpected property value"); exit(1); }
EOF

if [ $? -ne 0 ]; then
   exit 1
fi

# -classpath and -cp are synonyms

${JRUNSCRIPT} -l nashorn -classpath . <<EOF
var v;
try { v = new Packages.Hello(); } catch (e) { println(e); exit(1) }
if (v.string != 'hello') { println("unexpected property value"); exit(1); }
EOF

if [ $? -ne 0 ]; then
   exit 1
fi

rm -f Hello.class
echo "Passed"
exit 0
