#!/bin/sh
# @test
# @bug  6510337
# @run shell ClassPathWildCard.sh 
# @summary A very basic/rudimentary test for classpath wildcards
# @author Kumar Srinivasan 

#
# Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

# An elaborate wildcard testing is done by test/tools/javac/Paths/wcMineField.sh, 
# this test is a small subset, primarily to ensure that java and javaw launcher
# behave consistently, while we are it , doesn't hurt to test other platforms
# as well.

# For debugging
# set -x
# _JAVA_LAUNCHER_DEBUG=true ; export _JAVA_LAUNCHER_DEBUG

# Verify directory context variables are set
if [ "${TESTJAVA}" = "" ]; then
  echo "TESTJAVA not set.  Test cannot execute.  Failed."
  exit 1
fi

if [ "${COMPILEJAVA}" = "" ]; then
  COMPILEJAVA="${TESTJAVA}"
fi

if [ "${TESTSRC}" = "" ]; then
  echo "TESTSRC not set.  Test cannot execute.  Failed."
  exit 1
fi

if [ "${TESTCLASSES}" = "" ]; then
  echo "TESTCLASSES not set.  Test cannot execute.  Failed."
  exit 1
fi

JAVA=$TESTJAVA/bin/java
JAVAC=$COMPILEJAVA/bin/javac
JAR=$COMPILEJAVA/bin/jar

OUTEXT=".out"

# We write out a test file, as javaw does not have any notion about
# stdout or stderr.

EmitJavaFile() {
    fullName=$1
    fileName=`basename $fullName .java`

(
    printf "import java.io.*;\n"
    printf "public class %s {\n" $fileName
    printf "   public static void main(String[] args) {"
    printf "       String m = \"%s:\";\n" $fileName
    printf "       m = m.concat(\"java.class.path=\");\n"
    printf "       m = m.concat(System.getProperty(\"java.class.path\",\"NONE\"));\n"
    printf "       System.out.println(m);\n"
    printf "       try {\n"
    printf "          PrintStream ps = new PrintStream(\"%s\");\n" $fileName$OUTEXT
    printf "          ps.println(m);\n"
    printf "          ps.flush(); ps.close();\n"
    printf "       } catch (Exception e) {\n"
    printf "           System.out.println(e.getMessage());\n"
    printf "           System.exit(1);\n"
    printf "       }\n"
    printf "   }\n"
    printf "}\n"
) > $fullName
}

CreateClassFiles() {
  Exp=$1
  [ -d Test${Exp} ] || mkdir Test${Exp} 
  EmitJavaFile Test${Exp}/Test${Exp}.java
  $JAVAC ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d Test${Exp} Test${Exp}/Test${Exp}.java || exit 1
}

CreateJarFiles() {
  Exp=$1
  [ -d JarDir ] || mkdir JarDir
  CreateClassFiles $Exp
  $JAR ${TESTTOOLVMOPTS} -cvf JarDir/Test${Exp}.jar -C Test${Exp} . || exit 1
}

CheckFail() {
  if [ ! -f ${1}${OUTEXT} ]; then
     printf "Error: %s fails\n" "$1"
     exit 1
  fi
}

# Note: see CR:6328875 this is why we use the NOOP variable 
# below on Windows

ExecJava() {
  variant=$1
  NOOP=$2

  # Test JAR files first
  rm -f TestA${OUTEXT}
  $JAVA${variant} -classpath JarDir/"*"$NOOP TestA || exit 1
  CheckFail TestA

  rm -f TestB${OUTEXT}
  $JAVA${variant} -cp JarDir/"*"$NOOP TestB || exit 1
  CheckFail TestB


  # Throw some class files into the mix
  cp TestC/*.class JarDir
  cp TestD/*.class JarDir

  rm -f TestC${OUTEXT}
  $JAVA${variant} --class-path JarDir${PATHSEP}JarDir/"*"$NOOP TestC || exit 1
  CheckFail TestC

  rm -f TestD${OUTEXT}
  $JAVA${variant} --class-path=JarDir${PATHSEP}JarDir/"*"$NOOP TestD || exit 1
  CheckFail TestD
}

CreateJarFiles A
CreateJarFiles B
CreateClassFiles C
CreateClassFiles D

OS=`uname -s`
case $OS in 
    Windows*|CYGWIN*)
        PATHSEP=";"
        ExecJava "" "${PATHSEP}NOOPDIR"
        ExecJava "w" "${PATHSEP}NOOPDIR"
        break
    ;;

    *)
        PATHSEP=":"
        ExecJava "" ""
	break
    ;;
esac

exit 0
