#
# Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
# @test
# @key headful
# @bug 8055160 8216008
# @summary Unit test for javax.accessibility.AccessibilitySPI
#
# @build Load FooProvider BarProvider UnusedProvider
# @run shell basic.sh

# Command-line usage: sh basic.sh /path/to/build

if [ -z "$TESTJAVA" ]; then
  if [ $# -lt 1 ]; then exit 1; fi
  TESTJAVA="$1"
  TESTSRC=`pwd`
  TESTCLASSES="`pwd`"
fi

JAVA="$TESTJAVA/bin/java"

OS=`uname -s`
case "$OS" in
    Darwin | AIX )
      FS='/'
      SEP=':' ;;
    Linux )
      FS='/'
      SEP=':' ;;
    * )
      FS='\\'
      SEP='\;' ;;
esac

TESTD=x.test
rm -rf $TESTD
mkdir -p $TESTD

mv $TESTCLASSES/FooProvider.class $TESTD
mv $TESTCLASSES/BarProvider.class $TESTD
mv $TESTCLASSES/UnusedProvider.class $TESTD
mkdir -p $TESTD/META-INF/services
echo FooProvider >$TESTD/META-INF/services/javax.accessibility.AccessibilityProvider
echo BarProvider >>$TESTD/META-INF/services/javax.accessibility.AccessibilityProvider
echo UnusedProvider >>$TESTD/META-INF/services/javax.accessibility.AccessibilityProvider


failures=0

go() {
  CP="$TESTCLASSES$SEP$TESTD"
  echo ''
  sh -xc "$JAVA $SECURITY_MANAGER -Djavax.accessibility.assistive_technologies=$PROVIDER1$COMMA$PROVIDER2 -cp $CP Load $1 $2 $3" 2>&1
  if [ $? != 0 ]; then failures=`expr $failures + 1`; fi
}

# find one provider
PROVIDER1="FooProvider"
go pass $PROVIDER1

# start using security manager
SECURITY_MANAGER="-Djava.security.manager -Djava.security.policy=$TESTSRC/accessibilityProvider.sp"

# find one provider (with security manager)
go pass $PROVIDER1
SECURITY_MANAGER=

# fail if no provider found
PROVIDER1="NoProvider"
go fail $PROVIDER1

# pass if none provider found
PROVIDER1=
go pass $PROVIDER1

PROVIDER1=" "
go pass $PROVIDER1

# setup for two providers
COMMA=","

# find two providers, both exist
PROVIDER1="FooProvider"
PROVIDER2="BarProvider"
go pass $PROVIDER1 $PROVIDER2

# find two providers, where second one doesn't exist
PROVIDER1="FooProvider"
PROVIDER2="NoProvider"
go fail $PROVIDER1 $PROVIDER2

# find two providers, where first one doesn't exist
PROVIDER1="NoProvider"
PROVIDER2="BarProvider"
go fail $PROVIDER1 $PROVIDER2

echo ''
if [ $failures -gt 0 ];
  then echo "$failures case(s) failed";
  else echo "All cases passed"; fi
exit $failures

