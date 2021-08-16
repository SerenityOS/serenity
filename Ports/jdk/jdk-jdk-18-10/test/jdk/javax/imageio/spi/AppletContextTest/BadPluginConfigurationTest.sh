#!/bin/ksh -p
# Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
#   @test
#
#   @bug        6342404 7078379 8167503 8183351
#
#   @summary    Test verifies that incorrectly configured ImageIO plugin spi
#               does not affect registration of other ImageIO plugin in the
#               applet context.
#
#
#   @compile    IIOPluginTest.java
#   @compile    DummyReaderPluginSpi.java
#   @run shell  BadPluginConfigurationTest.sh

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
   clean
   exit $status
 } #end of fail()

#Call this from anywhere to pass the test with a message
# usage: pass "reason why the test passed if applicable"
pass()
 { echo "The test passed!!!"
   echo "$*" 1>&2
   clean
   exit 0
 } #end of pass()

#Clean up the test_ext directory (PLUGINDST_DIR) before leaving
clean()
 {
 echo "Removing PLUGINDST_DIR ${PLUGINDST_DIR}"
 if [ -n "${PLUGINDST_DIR}" -a -d "${PLUGINDST_DIR}" ] ; then
 rm -rf "${PLUGINDST_DIR}"
 fi
 }

# end of subroutines


# The beginning of the script proper

# Checking for proper OS
OS=`uname -s`
MKTEMP="mktemp"
case "$OS" in
   AIX )
      FILESEP="/"
      PATHSEP=":"
      TMP=`cd /tmp; pwd -P`

      type ${MKTEMP} > /dev/null 2>&1

      if ! [ $? -ne 0 ] ; then 
        MKTEMP="/opt/freeware/bin/mktemp"
      fi
      if ! [ -e ${MKTEMP} ] ; then 
        pass "Test skipped because no mktemp found on this machine"
      fi
      ;;

   Darwin | Linux )
      FILESEP="/"
      PATHSEP=":"
      TMP=`cd /tmp; pwd -P`
      ;;

   Windows* )
      FILESEP="\\"
      PATHSEP=";"
      TMP=`cd "${SystemRoot}/Temp"; echo ${PWD}`
      ;;

   CYGWIN* )
      FILESEP="/"
      PATHSEP=";"
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
      else fail "no JDK specified on command line!"
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
      ${COMPILEJAVA}/bin/javac ./*.java ;
   fi
   # else in harness so copy all the class files from where jtreg put them
   # over to the scratch directory this test is running in.
   else cp ${TESTCLASSES}/*.class . ;
fi

#if in test harness, then copy the entire directory that the test is in over
# to the scratch directory.  This catches any support files needed by the test.
if [ -z "${STANDALONE}" ] ;
   then cp ${TESTSRC}/*.java .
fi

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

echo
echo ------ PREPARE TEST PLUGIN ---------

# note that we can not use some subdirectory of the
# scratch dir as the plugin dst dir because the test
# app have file read permission for all subdirs of the
# scratch dir

PLUGINDST_DIR=$(${MKTEMP} -d ${TMP}/iio_test.XXXXXXXX)
echo "Created PLUGINDST_DIR as ${PLUGINDST_DIR}"

TEST_PLUGIN=dummy.jar

# remove old service declaration
if [ -d META-INF ] ; then
    rm -rf META-INF
fi

# generate the service declaration
if [ ! -d META_INF ] ; then
     mkdir META-INF
     mkdir META-INF/services
fi

# add wrong record to the service configuration
echo "BadReaderPluginSpi" >  META-INF/services/javax.imageio.spi.ImageReaderSpi

echo "DummyReaderPluginSpi" >> META-INF/services/javax.imageio.spi.ImageReaderSpi


${TESTJAVA}/bin/jar -cvf ${TEST_PLUGIN} DummyReaderPluginSpi*.class META-INF/services/javax.imageio.spi.ImageReaderSpi

echo ----- TEST PLUGIN IS READY --------
echo
echo ----- INSTALL PLUGIN --------
echo "Install test plugin to ${PLUGINDST_DIR}"
if [ -f ${PLUGINDST_DIR}/${TEST_PLUGIN} ] ; then
    echo "Remove old plugin..."
    rm -f ${PLUGINDST_DIR}/${TEST_PLUGIN}
fi
mv -f ${TEST_PLUGIN} ${PLUGINDST_DIR}
if [ -f ${PLUGINDST_DIR}/${TEST_PLUGIN} ] ; then
    echo Test plugin is installed.
else
    fail "Unable to install test plugin to $PLUGINDST_DIR"
fi
echo ----- PLUGIN IS INSTALLED ------
echo
echo ----- CLEAN PLUGIN TEMPORARY FILES -----
rm -rf DummyReaderPluginSpi*.class META-INF
echo ----- CLEANING IS COMPLETE -------
echo


case "$OS" in
   CYGWIN* )
      TEST_CODEBASE=$(cygpath -m ${PWD})
      TEST_PLUGIN_JAR=$(cygpath -m ${PLUGINDST_DIR}${FILESEP}${TEST_PLUGIN})
      ;;

   # catch all other OSs
   * )
      TEST_CODEBASE=${PWD}
      TEST_PLUGIN_JAR=${PLUGINDST_DIR}${FILESEP}${TEST_PLUGIN}
      ;;
esac


# Update policy file to grant read permission
echo "grant codeBase \"file:${TEST_CODEBASE}\" {" > classpath.policy
echo " permission java.io.FilePermission \"${TEST_PLUGIN_JAR}\", \"read\";" >> classpath.policy
echo " permission java.util.PropertyPermission \"test.5076692.property\", \"read\";" >> classpath.policy
echo "};" >> classpath.policy
echo "grant codeBase \"file:${TEST_PLUGIN_JAR}\" {" >> classpath.policy
echo " permission java.util.PropertyPermission \"test.5076692.property\", \"read\";" >> classpath.policy
echo "};" >> classpath.policy

echo ---------------------
echo --- Applet policy ---
echo ---------------------
cat classpath.policy
echo ---------------------
echo

echo -------------------------------
echo ---  Applet Classpath Test  ---
echo -------------------------------
#
# please note that we need to use "==" in setup of the java.security.policy
# property in order to overwrite policies defined in the user policy file
# For more details see:
#  http://java.sun.com/j2se/1.5.0/docs/guide/security/PolicyFiles.html)
#

${TESTJAVA}/bin/java ${TESTVMOPTS} -cp ".${PATHSEP}${TEST_PLUGIN_JAR}" \
    -Djava.security.policy==classpath.policy \
    -Djava.security.manager IIOPluginTest

status=$?

if [ $status -eq "0" ] ; then
    pass ""
else
    fail "Test failed due to test plugin was not found."
fi

