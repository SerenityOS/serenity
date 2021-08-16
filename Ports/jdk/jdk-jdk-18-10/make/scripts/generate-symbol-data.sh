#!/bin/sh
#
# Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

# This script allows to easily generate (add or update) "--release N" data for JDK N + 1.
# N must be 11 or greater. This script works on Linux. To create new data, or update existing
# data, it is necessary to:
# - download a binary build of OpenJDK N for Linux, API of which will be recorded. It is strongly recommended
#   to use an official build, not a custom build, to avoid any chance of including unofficial changes.
#   The binary build should never be a build newer than the GA for JDK N. Install the build. The installation
#   directory will be denoted as "${JDK_N_INSTALL}" in the further text.
# - have a checkout the JDK to which the data should be added (or in which the data should be updated).
#   The checkout directory will be denoted as "${JDK_CHECKOUT}" in the further text.
#   The checkout must not have any local changes that could interfere with the new data. In particular,
#   there must be absolutely no changed, new or removed files under the ${JDK_CHECKOUT}/make/data/symbols
#   directory.
# - open a terminal program and run these commands:
#     cd "${JDK_CHECKOUT}"/make/data/symbols
#     bash ../../scripts/generate-symbol-data.sh "${JDK_N_INSTALL}"
# - this command will generate or update data for "--release N" into the ${JDK_CHECKOUT}/make/data/symbols
#   directory, updating all registration necessary. If the goal was to update the data, and there are no
#   new or changed files in the ${JDK_CHECKOUT}/make/data/symbols directory after running this script,
#   there were no relevant changes and no further action is necessary. Note that version for N > 9 are encoded
#   using capital letters, i.e. A represents version 10, B represents 11, and so on. The version numbers are in
#   the names of the files in the ${JDK_CHECKOUT}/make/data/symbols directory, as well as in
#   the ${JDK_CHECKOUT}/make/data/symbols/symbols file.
# - if there are any changed/new files in the ${JDK_CHECKOUT}/make/data/symbols directory after running this script,
#   then all the changes in this directory, including any new files, need to be sent for review and eventually pushed.
#   The commit message should specify which binary build was installed in the ${JDK_N_INSTALL} directory and also
#   include the SCM state that was used to build it, which can be found in ${JDK_N_INSTALL}/release,
#   in property "SOURCE".

if [ "$1x" = "x" ] ; then
    echo "Must provide the target JDK as a parameter:" >&2
    echo "$0 <target-jdk>" >&2
    exit 1
fi;

if [ ! -f symbols ] ; then
    echo "Must run inside the make/data/symbols directory" >&2
    exit 1
fi;

if [ "`git status --porcelain=v1 .`x" != "x" ] ; then
    echo "The make/data/symbols directory contains local changes!" >&2
    exit 1
fi;

$1/bin/java --add-exports jdk.jdeps/com.sun.tools.classfile=ALL-UNNAMED \
            --add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
            --add-exports jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED \
            --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
            --add-modules jdk.jdeps \
            ../../../make/langtools/src/classes/build/tools/symbolgenerator/CreateSymbols.java \
            build-description-incremental symbols include.list
