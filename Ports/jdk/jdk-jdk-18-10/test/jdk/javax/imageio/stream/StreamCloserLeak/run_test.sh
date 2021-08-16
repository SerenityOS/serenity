#!/bin/ksh -p
#
# Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
#   @test
#   @bug        6788096
#   @key        intermittent
#   @summary    Test simulates the case of multiple applets executed in
#               the same VM and verifies that ImageIO shutdown hook
#               StreamCloser does not cause a leak of classloaders.
#
#   @modules java.desktop/sun.awt
#   @build      test.Main
#   @build      testapp.Main
#   @run shell  run_test.sh

# There are several resources which need to be present before many
#  shell scripts can run.  Following are examples of how to check for
#  many common ones.
#
# Note that the shell used is the Korn Shell, KSH
#
# Also note, it is recommended that make files NOT be used.  Rather,
#  put the individual commands directly into this file.  That way,
#  it is possible to use command line arguments and other shell tech-
#  niques to find the compiler, etc on different systems.  For example,
#  a different path could be used depending on whether this were a
#  Solaris or Win32 machine, which is more difficult (if even possible)
#  in a make file.


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

   AIX )
      VAR="A different value for AIX"
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


###############  YOUR TEST CODE HERE!!!!!!!  #############

#All files required for the test should be in the same directory with
# this file.  If converting a standalone test to run with the harness,
# as long as all files are in the same directory and it returns 0 for
# pass, you should be able to cut and paste it into here and it will
# run with the test harness.

# This is an example of running something -- test
# The stuff below catches the exit status of test then passes or fails
# this shell test as appropriate ( 0 status is considered a pass here )

echo "Create TestApp.jar..."

if [ -f TestApp.jar ] ; then
    rm -f TestApp.jar
fi

${TESTJAVA}/bin/jar -cvf TestApp.jar -C ${TESTCLASSES} testapp

if [ $? -ne "0" ] ; then
    fail "Failed to create TestApp.jar"
fi

echo "Create Test.jar..."
if [ -f Test.jar ] ; then
    rm -f Test.jar
fi

${TESTJAVA}/bin/jar -cvf Test.jar -C ${TESTCLASSES} test

if [ $? -ne 0 ] ; then
    fail "Failed to create Test.jar"
fi

# Prepare temp dir for cahce files
mkdir ./tmp
if [ $? -ne 0 ] ; then
    fail "Unable to create temp directory."
fi

# Verify that all classloaders are destroyed
${TESTJAVA}/bin/java --add-exports java.desktop/sun.awt=ALL-UNNAMED ${TESTVMOPTS} -cp Test.jar test.Main
if [ $? -ne 0 ] ; then
    fail "Test FAILED: some classloaders weren't destroyed."
fi


# Verify that ImageIO shutdown hook works correcly
${TESTJAVA}/bin/java --add-exports java.desktop/sun.awt=ALL-UNNAMED ${TESTVMOPTS} \
    -cp Test.jar -DforgetSomeStreams=true test.Main
if [ $? -ne 0 ] ; then
    fail "Test FAILED: some classloaders weren't destroyed of shutdown hook failed."
fi

# sanity check: verify that all cache files were deleted
cache_files=`ls tmp`

if [ "x${cache_files}" != "x" ] ; then
    echo "WARNING: some cache files was not deleted: ${cache_files}"
fi

echo "Test done."

status=$?

if [ $status -eq "0" ] ; then
    pass ""
else
    fail "Test failed due to test plugin was not found."
fi

