#!/bin/sh

#
# Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

if [ "${TESTSRC}" = "" ]
then
  echo "TESTSRC not set.  Test cannot execute.  Failed."
  exit 1
fi
echo "TESTSRC=${TESTSRC}"

if [ "${TESTJAVA}" = "" ]
then
  echo "TESTJAVA not set.  Test cannot execute.  Failed."
  exit 1
fi
echo "TESTJAVA=${TESTJAVA}"

if [ "${COMPILEJAVA}" = "" ]
then
  COMPILEJAVA="${TESTJAVA}"
fi
echo "COMPILEJAVA=${COMPILEJAVA}"

if [ "${TESTCLASSES}" = "" ]
then
  echo "TESTCLASSES not set.  Test cannot execute.  Failed."
  exit 1
fi

echo "TESTCLASSES=${TESTCLASSES}"
echo "CLASSPATH=${CLASSPATH}"

JAVAC="${COMPILEJAVA}/bin/javac -g"
JAR="${COMPILEJAVA}/bin/jar"

mkdir agentclasses
touch agent.mf
echo "Manifest-Version: 1.0" >> agent.mf
echo "Boot-Class-Path: agent.jar" >> agent.mf
echo "Premain-Class: p.BootstrapClassPathAgent" >> agent.mf

cp ${TESTSRC}/BootstrapClassPathAgent.java BootstrapClassPathAgent.java
${JAVAC} ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d agentclasses BootstrapClassPathAgent.java
echo "JAR=${JAR}"
${JAR} ${TESTTOOLVMOPTS} cvfm agent.jar agent.mf -C agentclasses .
rm -f BootstrapClassPathAgent.class BootstrapClassPathAgent.java
