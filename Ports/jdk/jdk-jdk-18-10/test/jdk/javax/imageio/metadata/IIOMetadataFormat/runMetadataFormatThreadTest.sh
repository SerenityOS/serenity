#!/bin/ksh -p
#
# Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
#   @bug        4929170 7078379
#   @summary    Tests that user-supplied IIOMetadata implementations 
#                loaded by separate classloader in separate thread  
#                is able to load correspnding IIOMetadataFormat 
#                implementations.
#   @author     Andrew Brygin
#
#   @compile    UserPluginMetadataFormatTest.java MetadataFormatThreadTest.java MetadataTest.java
#   @run shell/timeout=60 runMetadataFormatThreadTest.sh

# Note!!!! JavaCodeForYourTest_CHANGE_THIS.java must be changed or deleted.  
# If there is any java code which will be executed during the test, it must 
# be compiled by the line above.  If multiple .java files, separate the 
# files by spaces on that line.  See testing page of AWT home page for
# pointers to the testharness spec. and FAQ.
# Note!!!! Change AppletDeadlock.sh to the name of your test!!!!

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
   exit 1
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
   Linux | Darwin | AIX )
      VAR="A different value for Linux"
      DEFAULT_JDK=/none
      #DEFAULT_JDK=/usr/local/java/jdk1.4/linux-i386
      FILESEP="/"
      ;;

   Windows_95 | Windows_98 | Windows_NT | Windows_ME )
      VAR="A different value for Win32"
      DEFAULT_JDK=/none
      #DEFAULT_JDK=/usr/local/java/jdk1.2/win32
      FILESEP="\\"
      ;;
    
    CYGWIN* )
      VAR="A different value for CYGWIN"
      DEFAULT_JDK=/none
      FILESEP="/"
      ;;

   # catch all other OSs
   * )
      echo "Unrecognized system!  $OS"
      fail "Unrecognized system!  $OS"
      ;;
esac

# check that some executable or other file you need is available, abort if not
#  note that the name of the executable is in the fail string as well.
# this is how to check for presence of the compiler, etc.
#RESOURCE=`whence SomeProgramOrFileNeeded`
#if [ "${RESOURCE}" = "" ] ; 
#   then fail "Need SomeProgramOrFileNeeded to perform the test" ; 
#fi

# IT'S FINE TO DELETE THIS IF NOT NEEDED!
# check if an environment variable you need is set, give it a default if not
#if [ -z "${NEEDED_VAR}" ] ; then
#   # The var is NOT set, so give it a default
#   NEEDED_VAR=/some/default/value/such/as/a/path
#fi

# IT'S FINE TO DELETE THIS IF NOT NEEDED!
#if [ -z "${NEEDED_LATER_VAR}" ] ; then
#   # The var is NOT set, so give it a default
#   # will need it in other scripts called from this one, so export it
#   NEEDED_LATER_VAR="/a/different/path/note/the/quotes"
#   export NEEDED_LATER_VAR
#fi

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

#if [ -z "${STANDALONE}" ] ; 
#   then cp ${TESTSRC}/* . 
#fi

#Just before executing anything, make sure it has executable permission!
chmod 777 ./*

###############  YOUR TEST CODE HERE!!!!!!!  #############

#All files required for the test should be in the same directory with
# this file.  If converting a standalone test to run with the harness,
# as long as all files are in the same directory and it returns 0 for
# pass, you should be able to cut and paste it into here and it will
# run with the test harness.

# This is an example of running something -- test
# The stuff below catches the exit status of test then passes or fails
# this shell test as appropriate ( 0 status is considered a pass here )
#./test # DELETE THIS LINE AND REPLACE WITH YOUR OWN COMMAND!!!

if [ -d ./test_classes ] ; then 
    rm -rf ./test_calsses
fi

mkdir ./test_classes
 
# split application classes and test plugin classes
mv ./UserPluginMetadataFormatTest*.class ./test_classes

$TESTJAVA/bin/java ${TESTVMOPTS} \
    MetadataFormatThreadTest test_classes UserPluginMetadataFormatTest

###############  END YOUR TEST CODE !!!!! ############
status=$?

# pass or fail the test based on status of the command
if [ $status -eq "0" ];
   then pass "Test passed - no stack trace printing"

   else fail "Test failure - stack trace was printed"
fi

#For additional examples of how to write platform independent KSH scripts,
# see the jtreg file itself.  It is a KSH script for both Solaris and Win32

