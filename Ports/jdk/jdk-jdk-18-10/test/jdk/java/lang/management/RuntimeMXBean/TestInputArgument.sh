#
# Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
# @test
# @bug     4530538
# @summary
# @author  Mandy Chung
#
# @run compile InputArgument.java
# @run shell TestInputArgument.sh
#

#Set appropriate jdk

if [ ! -z "${TESTJAVA}" ] ; then
     jdk="$TESTJAVA"
else
     echo "--Error: TESTJAVA must be defined as the pathname of a jdk to test."
     exit 1
fi

runOne()
{
   echo "runOne $@"
   $TESTJAVA/bin/java $TESTVMOPTS -classpath $TESTCLASSES "$@" || exit 2
}

runOne InputArgument

runOne -XX:+UseFastJNIAccessors -Xlog:gc*=debug InputArgument
runOne -XX:+UseFastJNIAccessors -Xlog:gc*=debug InputArgument -XX:+UseFastJNIAccessors
runOne "-Dprops=one two three" InputArgument "-Dprops=one two three"

exit 0
