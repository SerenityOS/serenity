#!/bin/ksh -p

#
# Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
#   @test       ShowExitTest.sh
#   @key        headful
#   @bug        6513421
#   @summary    Java process does not terminate on closing the Main Application Frame
#
#   @compile ShowExitTest.java
#   @run shell/timeout=60 ShowExitTest.sh

# NOTE: The following error message means that the regression test failed:
#       "Execution failed: Program `sh' interrupted! (timed out?)"

# Beginning of subroutines:
status=1

#Call this from anywhere to fail the test with an error message
# usage: fail "reason why the test failed"
fail()
 { echo "The test failed :-("
   echo "$*" 1>&2
   echo "exit status was $status"
   exit $status
 } #end of fail()

#Call this from anywhere to pass the test with a message
# usage: pass "reason why the test passed if applicable"
pass()
 { echo "The test passed!!!"
   echo "$*" 1>&2
   exit 0
 } #end of pass()

# end of subroutines


# The beginning of the script proper

# Checking for proper OS
OS=`uname -s`
case "$OS" in
   Linux )
      VAR="A different value for Linux"
      DEFAULT_JDK=/
      FILESEP="/"
      PATHSEP=":"
      TMP="/tmp"
      ;;

   Darwin )
      VAR="A different value for MacOSX"
      DEFAULT_JDK=/usr
      FILESEP="/"
      PATHSEP=":"
      TMP="/tmp"
      ;;

   Windows* )
      VAR="A different value for Win32"
      DEFAULT_JDK="C:/Program Files/Java/jdk1.8.0"
      FILESEP="\\"
      PATHSEP=";"
      TMP=`cd "${SystemRoot}/Temp"; echo ${PWD}`
      ;;

    CYGWIN* )
      VAR="A different value for Cygwin"
      DEFAULT_JDK="/cygdrive/c/Program\ Files/Java/jdk1.8.0"
      FILESEP="/"
      PATHSEP=";"
      TMP=`cd "${SystemRoot}/Temp"; echo ${PWD}`
      ;;

   AIX )
      VAR="A different value for AIX"
      DEFAULT_JDK=/
      FILESEP="/"
      PATHSEP=":"
      TMP="/tmp"
      ;;

   # catch all other OSs
   * )
      echo "Unrecognized system!  $OS"
      fail "Unrecognized system!  $OS"
      ;;
esac


# Want this test to run standalone as well as in the harness, so do the
#  following to copy the test's directory into the harness's scratch directory
#  and set all appropriate variables:

if [ -z "${TESTJAVA}" ] ; then
   # TESTJAVA is not set, so the test is running stand-alone.
   # TESTJAVA holds the path to the root directory of the build of the JDK
   # to be tested.  That is, any java files run explicitly in this shell
   # should use TESTJAVA in the path to the java interpreter.
   # So, we'll set this to the JDK spec'd on the command line.  If none
   # is given on the command line, tell the user that and use a cheesy
   # default.
   # THIS IS THE JDK BEING TESTED.
   if [ -n "$1" ] ;
      then TESTJAVA=$1
      else echo "no JDK specified on command line so using default!"
     TESTJAVA=$DEFAULT_JDK
   fi
   TESTSRC=.
   TESTCLASSES=.
   STANDALONE=1;
fi
echo "JDK under test is: $TESTJAVA"

#Deal with .class files:
if [ -n "${STANDALONE}" ] ;
   then
   #if standalone, remind user to cd to dir. containing test before running it
   echo "Just a reminder: cd to the dir containing this test when running it"
   # then compile all .java files (if there are any) into .class files
   if [ -a *.java ] ;
      then echo "Reminder, this test should be in its own directory with all"
      echo "supporting files it needs in the directory with it."
      ${TESTJAVA}/bin/javac ./*.java ;
   fi
   # else in harness so copy all the class files from where jtreg put them
   # over to the scratch directory this test is running in.
   else cp ${TESTCLASSES}/*.class . ;
fi

#if in test harness, then copy the entire directory that the test is in over
# to the scratch directory.  This catches any support files needed by the test.
if [ -z "${STANDALONE}" ] ;
   then cp ${TESTSRC}/* .
fi

#Just before executing anything, make sure it has executable permission!
chmod 777 ./*

###############  YOUR TEST CODE HERE!!!!!!!  #############

#All files required for the test should be in the same directory with
# this file.  If converting a standalone test to run with the harness,
# as long as all files are in the same directory and it returns 0 for
# pass, you should be able to cut and paste it into here and it will
# run with the test harness.

${TESTJAVA}/bin/java ${TESTVMOPTS} ShowExitTest

###############  END YOUR TEST CODE !!!!! ############
#Be sure the last command executed above this line returns 0 for success,
# something non-zero for failure.
status=$?

# pass or fail the test based on status of the command
if [ $status -eq "0" ];
   then pass ""

   else fail "The program didn't terminate automatically!"
fi

#For additional examples of how to write platform independent KSH scripts,
# see the jtreg file itself.  It is a KSH script for both Solaris and Win32

