#!/bin/sh

#
# Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
#  @test
#  @bug 4981536
#  @summary TTY: .jdbrc is read twice if jdb is run in the user's home dir
#  @author jjh
#  @run shell JdbReadTwiceTest.sh

#Set appropriate jdk 
if [ ! -z "$TESTJAVA" ] ; then
     jdk="$TESTJAVA"
else
     echo "--Error: TESTJAVA must be defined as the pathname of a jdk to test."
     exit 1
fi

if [ -z "$TESTCLASSES" ] ; then
     echo "--Error: TESTCLASSES must be defined."
     exit 1
fi

case `uname -s` in
    Linux)
      # Need this to convert to the /.automount/... form which
      # is what jdb will report when it reads an init file.
      echo TESTCLASSES=$TESTCLASSES
      TESTCLASSES=`(cd $TESTCLASSES; /bin/pwd)`
      echo TESTCLASSES=$TESTCLASSES
      ;;
esac

# All output will go under this dir.  We define HOME to
# be under here too, and pass it into jdb, to avoid problems
# with java choosing a value of HOME.
baseDir="$TESTCLASSES/jdbRead$$"
HOME="$baseDir/home"
mkdir -p "$HOME"

tmpResult="$baseDir/result"
fred="$baseDir/fred"
here="$baseDir"
jdbFiles="$HOME/jdb.ini $HOME/.jdbrc $here/jdb.ini $here/.jdbrc $tmpResult $fred"

cd $here
failed=


mkFiles()
{
    touch "$@"
}

doit()
{
    echo quit | $TESTJAVA/bin/jdb -J-Duser.home=$HOME > $tmpResult 2>&1
}

failIfNot()
{
    # $1 is the expected number of occurances of $2 in the jdb output.
    count=$1
    shift
    if [ -r c:/ ] ; then
       sed -e 's@\\@/@g' $tmpResult > $tmpResult.1
       mv $tmpResult.1 $tmpResult
    fi
    xx=`fgrep -i "$*" $tmpResult | wc -l`
    if [ $xx != $count ] ; then
        echo "Failed: Expected $count, got $xx: $*"
        echo "-----"
        cat $tmpResult
        echo "-----"
        failed=1
    else
        echo "Passed: Expected $count, got $xx: $*"
    fi        
}

clean()
{
    rm -f $jdbFiles
}

# Note:  If jdb reads a file, it outputs a message containing
#         from: filename
# If jdb can't read a file, it outputs a message containing
#         open: filename


echo
echo "+++++++++++++++++++++++++++++++++++"
echo "Verify each individual file is read"
mkFiles $HOME/jdb.ini
    doit
    failIfNot 1 "from $HOME/jdb.ini"
    clean

mkFiles $HOME/.jdbrc
    doit
    failIfNot 1 "from $HOME/.jdbrc"
    clean

mkFiles $here/jdb.ini
    doit
    failIfNot 1 "from $here/jdb.ini"
    clean

mkFiles $here/.jdbrc
    doit
    failIfNot 1 "from $here/.jdbrc"
    clean


cd $HOME
echo
echo "+++++++++++++++++++++++++++++++++++"
echo "Verify files are not read twice if cwd is ~"
mkFiles $HOME/jdb.ini
    doit
    failIfNot 1 "from $HOME/jdb.ini"
    clean

mkFiles $HOME/.jdbrc
    doit
    failIfNot 1 "from $HOME/.jdbrc"
    clean
cd $here


echo
echo "+++++++++++++++++++++++++++++++++++"
echo "If jdb.ini and both .jdbrc exist, don't read .jdbrc"
mkFiles $HOME/jdb.ini $HOME/.jdbrc
    doit
    failIfNot 1  "from $HOME/jdb.ini" 
    failIfNot 0  "from $HOME/.jdbrc"
    clean


echo
echo "+++++++++++++++++++++++++++++++++++"
echo "If files exist in both ~ and ., read both"
mkFiles $HOME/jdb.ini $here/.jdbrc
    doit
    failIfNot 1  "from $HOME/jdb.ini" 
    failIfNot 1  "from $here/.jdbrc"
    clean

mkFiles $HOME/.jdbrc $here/jdb.ini
    doit
    failIfNot 1  "from $HOME/.jdbrc"
    failIfNot 1  "from $here/jdb.ini"
    clean


if [ ! -r c:/ ] ; then
    # No symlinks on windows.
    echo
    echo "+++++++++++++++++++++++++++++++++++"
    echo "Don't read a . file that is a symlink to a ~ file"
    mkFiles $HOME/jdb.ini
    ln -s $HOME/jdb.ini $here/.jdbrc
    doit
    failIfNot 1  "from $HOME/jdb.ini"
    failIfNot 0  "from $here/.jdbrc"
    clean
fi


if [ ! -r c:/ ] ; then
    # No symlinks on windows.
    echo
    echo "+++++++++++++++++++++++++++++++++++"
    echo "Don't read a . file that is a target symlink of a ~ file"
    mkFiles $here/jdb.ini
    ln -s $here/jdbini $HOME/.jdbrc
    doit
    failIfNot 1  "from $here/jdb.ini"
    failIfNot 0  "from $HOME/.jdbrc"
    clean
fi

echo
echo "+++++++++++++++++++++++++++++++++++"
echo "Read a directory - verify the read fails"
# If the file (IE. directory) exists, we try to read it.  The
# read will fail.
mkdir $HOME/jdb.ini
    doit
    failIfNot 1 "open: $HOME/jdb.ini"
    rmdir $HOME/jdb.ini
    

echo "read $fred" > $here/jdb.ini
    echo
    echo "+++++++++++++++++++++++++++++++++++"
    echo "Verify the jdb read command still works"
    touch $fred
    doit
    failIfNot 1 "from $fred"

    if [ "$canMakeUnreadable" = "Yes" ]
    then
        chmod a-r $fred
        doit
        failIfNot 1 "open: $fred"
    fi
    rm -f $fred
    mkdir $fred
    doit
    failIfNot 1 "open: $fred"
    rmdir $fred

clean


if [ "$failed" = 1 ] ; then
    echo "One or more tests failed"
    exit 1
fi

echo "All tests passed"
