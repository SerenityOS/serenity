#
# Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

# Utilities for shell tests

: ${TESTSRC=.} ${TESTCLASSES=.}
  java="${TESTJAVA+${TESTJAVA}/bin/}java${EXE_SUFFIX}"
 javac="${TESTJAVA+${TESTJAVA}/bin/}javac${EXE_SUFFIX}"
   jar="${TESTJAVA+${TESTJAVA}/bin/}jar${EXE_SUFFIX}"
jimage="${TESTJAVA+${TESTJAVA}/bin/}jimage${EXE_SUFFIX}"

case `uname -s` in
  Windows*|CYGWIN*)
    WindowsOnly() { "$@"; }
    UnixOnly() { :; }
    PS=";" ;;
  *)
    UnixOnly() { "$@"; }
    WindowsOnly() { :; }
    PS=":";;
esac

failed=""
Fail() { echo "FAIL: $1"; failed="${failed}."; }

Die() { printf "%s\n" "$*"; exit 1; }

Sys() {
    printf "%s\n" "$*"; "$@"; rc="$?";
    test "$rc" -eq 0 || Die "Command \"$*\" failed with exitValue $rc";
}

CheckFiles() {
    for f in "$@"; do test -r "$f" || Die "File $f not found"; done
}

Report() {
    test "$#" != 2 && Die "Usage: Report success|failure rc"

    if   test "$1" = "success" -a "$2" = 0; then
        echo "PASS: succeeded as expected"
    elif test "$1" = "failure" -a "$2" != 0; then
        echo "PASS: failed as expected"
    elif test "$1" = "success" -a "$2" != 0; then
        Fail "test failed unexpectedly"
    elif test "$1" = "failure" -a "$2" = 0; then
        Fail "test succeeded unexpectedly"
    else
        Die "Usage: Report success|failure rc"
    fi
}

MkManifestWithClassPath() {
    (echo "Manifest-Version: 1.0"; echo "Class-Path: $*") > MANIFEST.MF
}

HorizontalRule() {
    echo "-----------------------------------------------------------------"
}

Test() {
    HorizontalRule
    expectedResult="$1"; shift
    printf "%s\n" "$*"
    "$@"
    Report "$expectedResult" "$?"
}

Failure() { Test failure "$@"; }
Success() { Test success "$@"; }

Bottom() {
    test "$#" = 1 -a "$1" = "Line" || Die "Usage: Bottom Line"

    if test -n "$failed"; then
        count=`printf "%s" "$failed" | wc -c | tr -d ' '`
        echo "FAIL: $count tests failed"
        exit 1
    else
        echo "PASS: all tests gave expected results"
        exit 0
    fi
}

BadJarFile() {
    for jarfilename in "$@"; do pwd > "$jarfilename"; done
}


#----------------------------------------------------------------
# Foil message localization
#----------------------------------------------------------------
DiagnosticsInEnglishPlease() {
    LANG="C" LC_ALL="C" LC_MESSAGES="C"; export LANG LC_ALL LC_MESSAGES
}
