#!/bin/sh

#
# Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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
# @test
# @bug 4940642
# @summary Check for -help and -X flags
# @author Martin Buchholz
#
# @run shell Help.sh

# To run this test manually, simply do ./MineField.sh


. ${TESTSRC-.}/Util.sh

set -u

DiagnosticsInEnglishPlease

HELP="`\"$javac\" ${TESTTOOLVMOPTS} -help 2>&1`"
XHELP="`\"$javac\" ${TESTTOOLVMOPTS} -X 2>&1`"

#----------------------------------------------------------------
# Standard options
#----------------------------------------------------------------
for opt in \
    "-X " \
    "-J" \
    "-classpath " \
    "-cp " \
    "-bootclasspath " \
    "-sourcepath "; 
do
    case "$HELP" in *"$opt"*) ;; *) Fail "Bad help output" ;; esac
done

#----------------------------------------------------------------
# Non-standard options
#----------------------------------------------------------------
for opt in \
    "-Xbootclasspath/p:"; 
do
    case "$XHELP" in *"$opt"*) ;; *) Fail "Bad help output" ;; esac
done

Bottom Line
