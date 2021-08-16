#!/bin/bash
#
# Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
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

VECTORTESTS_HOME="$(pwd)"
JDK_SRC_HOME="./../../../../../"
JAVA="${JAVA_HOME}/bin/java"
JAVAC="${JAVA_HOME}/bin/javac"
BUILDLOG_FILE="./build.log"
SPP_CLASSNAME="build.tools.spp.Spp"
# Windows: Classpath Separator is ';'
# Linux: ':'
SEPARATOR=":"
TYPEPREFIX=""
TEMPLATE_FILE="unit_tests.template"
TESTNG_JAR="${TESTNG_PLUGIN}/plugins/org.testng.source_6.13.1.r201712040515.jar"
TESTNG_RUN_JAR="${TESTNG_PLUGIN}/plugins/org.testng_6.13.1.r201712040515.jar"
JCOMMANDER_JAR="${TESTNG_PLUGIN}/plugins/com.beust.jcommander_1.72.0.jar"
TEST_ITER_COUNT=100

PERF_TEMPLATE_FILE="perf_tests.template"
PERF_SCALAR_TEMPLATE_FILE="perf_scalar_tests.template"
PERF_DEST="benchmark/src/main/java/benchmark/jdk/incubator/vector/"

function Log () {
  if [ $1 == true ]; then
    echo "$2"
  fi
  echo "$2" >> $BUILDLOG_FILE
}

function LogRun () {
  if [ $1 == true ]; then
    echo -ne "$2"
  fi
  echo -ne "$2" >> $BUILDLOG_FILE
}

# Determine which delimiter to use based on the OS.
# Windows uses ";", while Unix-based OSes use ":"
uname_s=$(uname -s)
VECTORTESTS_HOME_CP=$VECTORTESTS_HOME
if [ "x${VAR_OS_ENV}" == "xwindows.cygwin" ]; then
  VECTORTESTS_HOME_CP=$(cygpath -pw $VECTORTESTS_HOME)
fi

if [ "$uname_s" == "Linux" ] || [ "$uname_s" == "Darwin" ]; then
  SEPARATOR=":"
else
  SEPARATOR=";"
fi
