#
# Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
# @bug 8011697
# @summary test to check consistency in discovering and returning script engine 
# by ScriptEngineManager
#
# @run shell ScriptEngineOrder.sh

set -x
if [ "${TESTSRC}" = "" ]
then
  echo "TESTSRC not set.  Test cannot execute.  Failed."
  exit 1
fi

. ${TESTSRC}/CommonSetup.sh

echo "Building dummy script engine modules.."
#test to check the consistency in returning engines by ScriptEngineManager
$JAVAC --limit-modules java.base,java.logging,java.scripting,jdk.scripting.dummyNashorn,jdk.scripting.dummyRhino,jdk.scripting.testEngines -d ${TESTCLASSES}/mods --module-source-path ${TESTSRC}/multiEngines $(find ${TESTSRC}/multiEngines -name *.java)

echo "Running script engine test.."
$JAVA --limit-modules java.base,java.logging,java.scripting,jdk.scripting.dummyNashorn,jdk.scripting.dummyRhino,jdk.scripting.testEngines --module-path ${TESTCLASSES}/mods --module jdk.scripting.testEngines/jdk.test.engines.ScriptEngineTest

ret=$?
if [ $ret -ne 0 ]
then
  exit $ret
fi

