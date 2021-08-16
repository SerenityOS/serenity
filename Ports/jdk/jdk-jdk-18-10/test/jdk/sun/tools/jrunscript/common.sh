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

#

setup() {
    # Verify directory context variables are set
    if [ "${TESTJAVA}" = "" ] ; then
        echo "TESTJAVA not set. Test cannot execute.  Failed."
        exit 1
    fi

    if [ "${TESTCLASSES}" = "" ] ; then
        TESTCLASSES="."
    fi

    if [ "${TESTSRC}" = "" ] ; then
        TESTSRC="."
    fi

    OS=`uname -s`
    case ${OS} in
    Windows_*)
        PS=";"
        FS="\\"
        # MKS diff deals with trailing CRs automatically
        golden_diff="diff"
        ;;
    CYGWIN*)
        PS=":"
        FS="/"
        # Cygwin diff needs to be told to ignore trailing CRs
        golden_diff="diff --strip-trailing-cr"
        ;;
    *)
        PS=":"
        FS="/"
        # Assume any other platform doesn't have the trailing CR stuff
        golden_diff="diff"
        ;;
    esac

    JRUNSCRIPT="${TESTJAVA}/bin/jrunscript"
    JAVAC="${TESTJAVA}/bin/javac"
    JAVA="${TESTJAVA}/bin/java"
}
