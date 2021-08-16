#
# Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
# Utility Shell Script for generating .properties files or .password files
# or .access files from a list of input .in files.
#
# Source in this GeneratePropertyPassword.sh and call the function
# generatePropertyPasswordFiles.
# Call restoreFilePermissions to restore file permissions after the test completes
#


OS=`uname -s`
UMASK=`umask`

case $OS in
CYGWIN_NT*)
    OS="Windows_NT"
    if [ -z "$SystemRoot" ] ;  then
        SystemRoot=`cygpath $SYSTEMROOT`
    fi
esac

case $OS in
Linux | Darwin | AIX )
    PATHSEP=":"
    FILESEP="/"
    DFILESEP=$FILESEP
    TMP_FILE=${TESTCLASSES}${FILESEP}${TESTCLASS}.sed.tmpfile

cat <<EOF > ${TMP_FILE}
s^@TEST-SRC@/^${TESTCLASSES}${DFILESEP}^g
EOF
    ;;
Windows_95 | Windows_98 | Windows_NT | Windows_ME | CYGWIN*)
    PATHSEP=";"
    FILESEP="\\"
    DFILESEP=$FILESEP$FILESEP
    TMP_FILE=${TESTCLASSES}${FILESEP}${TESTCLASS}.sed.tmpfile

cat <<EOF > ${TMP_FILE}0
s^@TEST-SRC@/^${TESTCLASSES}${DFILESEP}^g
EOF
    # Need to put double backslash in the .properties files
    cat ${TMP_FILE}0 | sed -e 's^\\\\^ZZZZ^g' | \
        sed -e 's^\\^ZZZZ^g' | \
        sed -e 's^ZZZZ^\\\\\\\\^g' > ${TMP_FILE}

    if [ "$OS" = "Windows_NT" ]; then
        USER=`id -u -n`
        CACLS="$SystemRoot/system32/cacls.exe"
        REVOKEALL="$TESTNATIVEPATH/revokeall.exe"
        if [ ! -x "$REVOKEALL" ] ; then
            echo "$REVOKEALL doesn't exist or is not executable"
            exit 1
        fi
    fi
    ;;
*)
    echo "Unrecognized system! $OS"
    exit 1
    ;;
esac

generatePropertyPasswordFiles()
{
   for f in $@
   do
        echo processing $f
        suffix=`basename $f .in`
        f2="${TESTCLASSES}${FILESEP}${suffix}"

        if [ -f "$f2" ] ; then
            rm -f $f2 || echo WARNING: $f2 already exits - unable to remove old copy
        fi

        echo creating $f2
        sed -f $TMP_FILE $f > $f2

        if [ "$OS" = "Windows_NT" ]; then
            chown $USER $f2
            # Grant this user full access
            echo Y|$CACLS $f2 \/E \/G $USER:F
            # Revoke everyone else
            $REVOKEALL $f2
            # Display ACLs
            $CACLS $f2
        else
            chmod 600 $f2
        fi
   done
}

restoreFilePermissions()
{
    for f in $@
    do
        suffix=`basename $f .in`
        f2="${TESTCLASSES}${FILESEP}${suffix}"

        if [ "$OS" = "Windows_NT" ]; then
            # Grant everyone full control
            $CACLS $f2 \/E \/G Everyone:F
        else
            chmod 777 $f2
        fi

    done
}

