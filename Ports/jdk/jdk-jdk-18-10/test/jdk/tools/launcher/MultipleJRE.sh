#!/bin/sh
# @test MultipleJRE.sh
# @bug 4811102 4953711 4955505 4956301 4991229 4998210 5018605 6387069 6733959 8058407 8067421
# @build PrintVersion
# @build UglyPrintVersion
# @build ZipMeUp
# @run shell MultipleJRE.sh
# @summary Verify Multiple JRE version support has been removed
# @author Joseph E. Kowalski
#
# Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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

# Verify directory context variables are set
if [ "${TESTJAVA}" = "" ]
then
  echo "TESTJAVA not set.  Test cannot execute.  Failed."
  exit 1
fi

if [ "${COMPILEJAVA}" = "" ]; then
  COMPILEJAVA="${TESTJAVA}"
fi

if [ "${TESTSRC}" = "" ]
then
  echo "TESTSRC not set.  Test cannot execute.  Failed."
  exit 1
fi

if [ "${TESTCLASSES}" = "" ]
then
  echo "TESTCLASSES not set.  Test cannot execute.  Failed."
  exit 1
fi

JAVAEXE="$TESTJAVA/bin/java ${TESTVMOPTS}"
JAVA="$TESTJAVA/bin/java ${TESTVMOPTS} -classpath $TESTCLASSES"
JAR="$COMPILEJAVA/bin/jar ${TESTTOOLVMOPTS}"
OS=`uname -s`;

#
# Tests whether we are on windows (true) or not.
#
IsWindows() {
    case "$OS" in
        Windows* | CYGWIN* )
            printf "true"
	;;
	* )
            printf "false"
	;;
    esac
}

#
# Shell routine to test for the proper rejection of syntactically incorrect
# version specifications.
#
TestSyntax() {
	mess="`$JAVA -version:\"$1\" -version 2>&1`"
	if [ $? -eq 0 ]; then
		echo "Invalid version syntax $1 accepted"
		exit 1
	fi
	prefix="`echo "$mess" | cut -d ' ' -f 1-3`"
	if [ "$prefix" != "Error: Syntax error" ]; then
		echo "Unexpected error message for invalid syntax $1"
		exit 1
	fi
}

#
# Just as the name says.  We sprinkle these in the appropriate location
# in the test file system and they just say who they are pretending to be.
#
CreateMockVM() {
	mkdir -p jdk/j2re$1/bin
	echo "#!/bin/sh"    > jdk/j2re$1/bin/java
	echo "echo \"$1\"" >> jdk/j2re$1/bin/java
	chmod +x jdk/j2re$1/bin/java
}

#
# Constructs the jar file needed by these tests.
#
CreateJar() {
	mkdir -p META-INF
	echo "Manifest-Version: 1.0" > META-INF/MANIFEST.MF
	echo "Main-Class: PrintVersion" >> META-INF/MANIFEST.MF
	if [ "$1" != "" ]; then
		echo "JRE-Version: $1" >> META-INF/MANIFEST.MF
	fi
	cp $TESTCLASSES/PrintVersion.class .
	$JAR $2cmf META-INF/MANIFEST.MF PrintVersion PrintVersion.class
}

#
# Constructs a jar file using zip.
#
CreateZippyJar() {
	mkdir -p META-INF
	echo "Manifest-Version: 1.0" > META-INF/MANIFEST.MF
	echo "Main-Class: PrintVersion" >> META-INF/MANIFEST.MF
	if [ "$1" != "" ]; then
		echo "JRE-Version: $1" >> META-INF/MANIFEST.MF
	fi
	cp $TESTCLASSES/PrintVersion.class .
	/usr/bin/zip $2 PrintVersion META-INF/MANIFEST.MF PrintVersion.class
}

#
# Constructs a jar file with a Main-Class attribute of greater than
# 80 characters to validate the continuation line processing.
#
# Make this just long enough to require two continuation lines.  Longer
# paths take too much away from the restricted Windows maximum path length.
# Note: see the variable UGLYCLASS and its check for path length.
#
# Make sure that 5018605 remains fixed by including additional sections
# in the Manifest which contain the same names as those allowed in the
# main section.
#
PACKAGE=reallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallylongpackagename
UGLYCLASS=$TESTCLASSES/$PACKAGE/UglyPrintVersion.class
CreateUglyJar() {
	mkdir -p META-INF
	echo "Manifest-Version: 1.0" > META-INF/MANIFEST.MF
	echo "Main-Class: $PACKAGE.UglyPrintVersion" >> META-INF/MANIFEST.MF
	if [ "$1" != "" ]; then
		echo "JRE-Version: $1" >> META-INF/MANIFEST.MF
	fi
	echo "" >> META-INF/MANIFEST.MF
	echo "Name: NotToBeFound.class" >> META-INF/MANIFEST.MF
	echo "Main-Class: NotToBeFound" >> META-INF/MANIFEST.MF
	mkdir -p $PACKAGE
	cp $UGLYCLASS $PACKAGE
	$JAR $2cmf META-INF/MANIFEST.MF PrintVersion \
	    $PACKAGE/UglyPrintVersion.class
}

#
# Constructs a jar file with a fair number of "zip directory" entries and
# the MANIFEST.MF entry at or near the end of that directory to validate
# the ability to transverse that directory.
#
CreateFullJar() {
	mkdir -p META-INF
	echo "Manifest-Version: 1.0" > META-INF/MANIFEST.MF
	echo "Main-Class: PrintVersion" >> META-INF/MANIFEST.MF
	if [ "$1" != "" ]; then
	    echo "JRE-Version: $1" >> META-INF/MANIFEST.MF
	fi
	cp $TESTCLASSES/PrintVersion.class .
	for i in 0 1 2 3 4 5 6 7 8 9 ; do
		for j in 0 1 2 3 4 5 6 7 8 9 ; do
			touch AfairlyLongNameEatsUpDirectorySpaceBetter$i$j
		done
	done
	$JAR $2cMf PrintVersion PrintVersion.class AfairlyLong*
	$JAR $2umf META-INF/MANIFEST.MF PrintVersion
	rm -f AfairlyLong*
}

#
# Creates a jar file with the attributes which caused the failure
# described in 4991229.
#
# Generate a bunch of CENTAB entries, each of which is 64 bytes long
# which practically guarentees we will hit the appropriate power of
# two buffer (initially 1K).  Note that due to the perversity of
# zip/jar files, the first entry gets extra stuff so it needs a
# shorter name to compensate.
#
CreateAlignedJar() {
	mkdir -p META-INF
	echo "Manifest-Version: 1.0" > META-INF/MANIFEST.MF
	echo "Main-Class: PrintVersion" >> META-INF/MANIFEST.MF
	if [ "$1" != "" ]; then
	    echo "JRE-Version: $1" >> META-INF/MANIFEST.MF
	fi
	cp $TESTCLASSES/PrintVersion.class .
	touch 57BytesSpecial
	for i in 0 1 2 3 4 5 6 7 8 9 ; do
		for j in 0 1 2 3 4 5 6 7 8 9 ; do
			touch 64BytesPerEntry-$i$j
		done
	done
	$JAR $2cMf PrintVersion 57* 64* PrintVersion.class
	$JAR $2umf META-INF/MANIFEST.MF PrintVersion
	rm -f 57* 64*
}

#
# Adds comments to a jar/zip file.  This serves two purposes:
#
#   1)	Make sure zip file comments (both per file and per archive) are
#	properly processed and ignored.
#
#   2)	A long file comment creates a mondo "Central Directory" entry in
#	the zip file. Such a "mondo" entry could also be due to a very
#	long file name (path) or a long "Ext" entry, but adding the long
#	comment is the easiest way.
#
MONDO=" Mondo comment line 00 is designed to take up space - lots and lots of space.  Mondo comment line 01 is designed to take up space - lots and lots of space.  Mondo comment line 02 is designed to take up space - lots and lots of space.  Mondo comment line 03 is designed to take up space - lots and lots of space.  Mondo comment line 04 is designed to take up space - lots and lots of space.  Mondo comment line 05 is designed to take up space - lots and lots of space.  Mondo comment line 06 is designed to take up space - lots and lots of space.  Mondo comment line 07 is designed to take up space - lots and lots of space.  Mondo comment line 08 is designed to take up space - lots and lots of space.  Mondo comment line 09 is designed to take up space - lots and lots of space.  Mondo comment line 0a is designed to take up space - lots and lots of space.  Mondo comment line 0b is designed to take up space - lots and lots of space.  Mondo comment line 0c is designed to take up space - lots and lots of space.  Mondo comment line 0d is designed to take up space - lots and lots of space.  Mondo comment line 0e is designed to take up space - lots and lots of space.  Mondo comment line 0f is designed to take up space - lots and lots of space.  Mondo comment line 10 is designed to take up space - lots and lots of space.  Mondo comment line 11 is designed to take up space - lots and lots of space.  Mondo comment line 12 is designed to take up space - lots and lots of space.  Mondo comment line 13 is designed to take up space - lots and lots of space.  Mondo comment line 14 is designed to take up space - lots and lots of space.  Mondo comment line 15 is designed to take up space - lots and lots of space.  Mondo comment line 16 is designed to take up space - lots and lots of space.  Mondo comment line 17 is designed to take up space - lots and lots of space.  Mondo comment line 18 is designed to take up space - lots and lots of space.  Mondo comment line 19 is designed to take up space - lots and lots of space.  Mondo comment line 1a is designed to take up space - lots and lots of space.  Mondo comment line 1b is designed to take up space - lots and lots of space.  Mondo comment line 1c is designed to take up space - lots and lots of space.  Mondo comment line 1d is designed to take up space - lots and lots of space.  Mondo comment line 1e is designed to take up space - lots and lots of space.  Mondo comment line 1f is designed to take up space - lots and lots of space.  Mondo comment line 20 is designed to take up space - lots and lots of space.  Mondo comment line 21 is designed to take up space - lots and lots of space.  Mondo comment line 22 is designed to take up space - lots and lots of space.  Mondo comment line 23 is designed to take up space - lots and lots of space.  Mondo comment line 24 is designed to take up space - lots and lots of space.  Mondo comment line 25 is designed to take up space - lots and lots of space.  Mondo comment line 26 is designed to take up space - lots and lots of space.  Mondo comment line 27 is designed to take up space - lots and lots of space.  Mondo comment line 28 is designed to take up space - lots and lots of space.  Mondo comment line 29 is designed to take up space - lots and lots of space.  Mondo comment line 2a is designed to take up space - lots and lots of space.  Mondo comment line 2b is designed to take up space - lots and lots of space.  Mondo comment line 2c is designed to take up space - lots and lots of space.  Mondo comment line 2d is designed to take up space - lots and lots of space.  Mondo comment line 2e is designed to take up space - lots and lots of space.  Mondo comment line 2f is designed to take up space - lots and lots of space.  Mondo comment line 30 is designed to take up space - lots and lots of space.  Mondo comment line 31 is designed to take up space - lots and lots of space.  Mondo comment line 32 is designed to take up space - lots and lots of space.  Mondo comment line 33 is designed to take up space - lots and lots of space.  Mondo comment line 34 is designed to take up space - lots and lots of space.  Mondo comment line 35 is designed to take up space - lots and lots of space.  Mondo comment line 36 is designed to take up space - lots and lots of space.  Mondo comment line 37 is designed to take up space - lots and lots of space.  Mondo comment line 38 is designed to take up space - lots and lots of space.  Mondo comment line 39 is designed to take up space - lots and lots of space.  Mondo comment line 3a is designed to take up space - lots and lots of space.  Mondo comment line 3b is designed to take up space - lots and lots of space.  Mondo comment line 3c is designed to take up space - lots and lots of space.  Mondo comment line 3d is designed to take up space - lots and lots of space.  Mondo comment line 3e is designed to take up space - lots and lots of space.  Mondo comment line 3f is designed to take up space - lots and lots of space.  Mondo comment line 40 is designed to take up space - lots and lots of space.  Mondo comment line 41 is designed to take up space - lots and lots of space.  Mondo comment line 42 is designed to take up space - lots and lots of space.  Mondo comment line 43 is designed to take up space - lots and lots of space.  Mondo comment line 44 is designed to take up space - lots and lots of space.  Mondo comment line 45 is designed to take up space - lots and lots of space.  Mondo comment line 46 is designed to take up space - lots and lots of space.  Mondo comment line 47 is designed to take up space - lots and lots of space.  Mondo comment line 48 is designed to take up space - lots and lots of space.  Mondo comment line 49 is designed to take up space - lots and lots of space.  Mondo comment line 4a is designed to take up space - lots and lots of space.  Mondo comment line 4b is designed to take up space - lots and lots of space.  Mondo comment line 4c is designed to take up space - lots and lots of space.  Mondo comment line 4d is designed to take up space - lots and lots of space.  Mondo comment line 4e is designed to take up space - lots and lots of space.  Mondo comment line 4f is designed to take up space - lots and lots of space.  Mondo comment line 50 is designed to take up space - lots and lots of space.  Mondo comment line 51 is designed to take up space - lots and lots of space.  Mondo comment line 52 is designed to take up space - lots and lots of space.  Mondo comment line 53 is designed to take up space - lots and lots of space.  Mondo comment line 54 is designed to take up space - lots and lots of space.  Mondo comment line 55 is designed to take up space - lots and lots of space.  Mondo comment line 56 is designed to take up space - lots and lots of space.  Mondo comment line 57 is designed to take up space - lots and lots of space.  Mondo comment line 58 is designed to take up space - lots and lots of space.  Mondo comment line 59 is designed to take up space - lots and lots of space.  Mondo comment line 5a is designed to take up space - lots and lots of space.  Mondo comment line 5b is designed to take up space - lots and lots of space.  Mondo comment line 5c is designed to take up space - lots and lots of space.  Mondo comment line 5d is designed to take up space - lots and lots of space.  Mondo comment line 5e is designed to take up space - lots and lots of space.  Mondo comment line 5f is designed to take up space - lots and lots of space.  Mondo comment line 60 is designed to take up space - lots and lots of space.  Mondo comment line 61 is designed to take up space - lots and lots of space.  Mondo comment line 62 is designed to take up space - lots and lots of space.  Mondo comment line 63 is designed to take up space - lots and lots of space.  Mondo comment line 64 is designed to take up space - lots and lots of space.  Mondo comment line 65 is designed to take up space - lots and lots of space.  Mondo comment line 66 is designed to take up space - lots and lots of space.  Mondo comment line 67 is designed to take up space - lots and lots of space.  Mondo comment line 68 is designed to take up space - lots and lots of space.  Mondo comment line 69 is designed to take up space - lots and lots of space.  Mondo comment line 6a is designed to take up space - lots and lots of space.  Mondo comment line 6b is designed to take up space - lots and lots of space.  Mondo comment line 6c is designed to take up space - lots and lots of space.  Mondo comment line 6d is designed to take up space - lots and lots of space.  Mondo comment line 6e is designed to take up space - lots and lots of space.  Mondo comment line 6f is designed to take up space - lots and lots of space.  Mondo comment line 70 is designed to take up space - lots and lots of space.  Mondo comment line 71 is designed to take up space - lots and lots of space.  Mondo comment line 72 is designed to take up space - lots and lots of space.  Mondo comment line 73 is designed to take up space - lots and lots of space.  Mondo comment line 74 is designed to take up space - lots and lots of space.  Mondo comment line 75 is designed to take up space - lots and lots of space.  Mondo comment line 76 is designed to take up space - lots and lots of space.  Mondo comment line 77 is designed to take up space - lots and lots of space.  Mondo comment line 78 is designed to take up space - lots and lots of space.  Mondo comment line 79 is designed to take up space - lots and lots of space.  Mondo comment line 7a is designed to take up space - lots and lots of space.  Mondo comment line 7b is designed to take up space - lots and lots of space.  Mondo comment line 7c is designed to take up space - lots and lots of space.  Mondo comment line 7d is designed to take up space - lots and lots of space.  Mondo comment line 7e is designed to take up space - lots and lots of space.  Mondo comment line 7f is designed to take up space - lots and lots of space.  Mondo comment line 80 is designed to take up space - lots and lots of space.  Mondo comment line 81 is designed to take up space - lots and lots of space.  Mondo comment line 82 is designed to take up space - lots and lots of space.  Mondo comment line 83 is designed to take up space - lots and lots of space.  Mondo comment line 84 is designed to take up space - lots and lots of space.  Mondo comment line 85 is designed to take up space - lots and lots of space.  Mondo comment line 86 is designed to take up space - lots and lots of space.  Mondo comment line 87 is designed to take up space - lots and lots of space.  Mondo comment line 88 is designed to take up space - lots and lots of space.  Mondo comment line 89 is designed to take up space - lots and lots of space.  Mondo comment line 8a is designed to take up space - lots and lots of space.  Mondo comment line 8b is designed to take up space - lots and lots of space.  Mondo comment line 8c is designed to take up space - lots and lots of space.  Mondo comment line 8d is designed to take up space - lots and lots of space.  Mondo comment line 8e is designed to take up space - lots and lots of space.  Mondo comment line 8f is designed to take up space - lots and lots of space.  Mondo comment line 90 is designed to take up space - lots and lots of space.  Mondo comment line 91 is designed to take up space - lots and lots of space.  Mondo comment line 92 is designed to take up space - lots and lots of space.  Mondo comment line 93 is designed to take up space - lots and lots of space.  Mondo comment line 94 is designed to take up space - lots and lots of space.  Mondo comment line 95 is designed to take up space - lots and lots of space.  Mondo comment line 96 is designed to take up space - lots and lots of space.  Mondo comment line 97 is designed to take up space - lots and lots of space.  Mondo comment line 98 is designed to take up space - lots and lots of space.  Mondo comment line 99 is designed to take up space - lots and lots of space.  Mondo comment line 9a is designed to take up space - lots and lots of space.  Mondo comment line 9b is designed to take up space - lots and lots of space.  Mondo comment line 9c is designed to take up space - lots and lots of space.  Mondo comment line 9d is designed to take up space - lots and lots of space.  Mondo comment line 9e is designed to take up space - lots and lots of space.  Mondo comment line 9f is designed to take up space - lots and lots of space.  Mondo comment line a0 is designed to take up space - lots and lots of space.  Mondo comment line a1 is designed to take up space - lots and lots of space.  Mondo comment line a2 is designed to take up space - lots and lots of space.  Mondo comment line a3 is designed to take up space - lots and lots of space.  Mondo comment line a4 is designed to take up space - lots and lots of space.  Mondo comment line a5 is designed to take up space - lots and lots of space.  Mondo comment line a6 is designed to take up space - lots and lots of space.  Mondo comment line a7 is designed to take up space - lots and lots of space.  Mondo comment line a8 is designed to take up space - lots and lots of space.  Mondo comment line a9 is designed to take up space - lots and lots of space.  Mondo comment line aa is designed to take up space - lots and lots of space.  Mondo comment line ab is designed to take up space - lots and lots of space.  Mondo comment line ac is designed to take up space - lots and lots of space.  Mondo comment line ad is designed to take up space - lots and lots of space.  Mondo comment line ae is designed to take up space - lots and lots of space.  Mondo comment line af is designed to take up space - lots and lots of space.  Mondo comment line b0 is designed to take up space - lots and lots of space.  Mondo comment line b1 is designed to take up space - lots and lots of space.  Mondo comment line b2 is designed to take up space - lots and lots of space.  Mondo comment line b3 is designed to take up space - lots and lots of space.  Mondo comment line b4 is designed to take up space - lots and lots of space.  Mondo comment line b5 is designed to take up space - lots and lots of space.  Mondo comment line b6 is designed to take up space - lots and lots of space.  Mondo comment line b7 is designed to take up space - lots and lots of space.  Mondo comment line b8 is designed to take up space - lots and lots of space.  Mondo comment line b9 is designed to take up space - lots and lots of space.  Mondo comment line ba is designed to take up space - lots and lots of space.  Mondo comment line bb is designed to take up space - lots and lots of space.  Mondo comment line bc is designed to take up space - lots and lots of space.  Mondo comment line bd is designed to take up space - lots and lots of space.  Mondo comment line be is designed to take up space - lots and lots of space.  Mondo comment line bf is designed to take up space - lots and lots of space.  Mondo comment line c0 is designed to take up space - lots and lots of space.  Mondo comment line c1 is designed to take up space - lots and lots of space.  Mondo comment line c2 is designed to take up space - lots and lots of space.  Mondo comment line c3 is designed to take up space - lots and lots of space.  Mondo comment line c4 is designed to take up space - lots and lots of space.  Mondo comment line c5 is designed to take up space - lots and lots of space.  Mondo comment line c6 is designed to take up space - lots and lots of space.  Mondo comment line c7 is designed to take up space - lots and lots of space.  Mondo comment line c8 is designed to take up space - lots and lots of space.  Mondo comment line c9 is designed to take up space - lots and lots of space.  Mondo comment line ca is designed to take up space - lots and lots of space.  Mondo comment line cb is designed to take up space - lots and lots of space.  Mondo comment line cc is designed to take up space - lots and lots of space.  Mondo comment line cd is designed to take up space - lots and lots of space.  Mondo comment line ce is designed to take up space - lots and lots of space.  Mondo comment line cf is designed to take up space - lots and lots of space.  Mondo comment line d0 is designed to take up space - lots and lots of space.  Mondo comment line d1 is designed to take up space - lots and lots of space.  Mondo comment line d2 is designed to take up space - lots and lots of space.  Mondo comment line d3 is designed to take up space - lots and lots of space.  Mondo comment line d4 is designed to take up space - lots and lots of space.  Mondo comment line d5 is designed to take up space - lots and lots of space.  Mondo comment line d6 is designed to take up space - lots and lots of space.  Mondo comment line d7 is designed to take up space - lots and lots of space.  Mondo comment line d8 is designed to take up space - lots and lots of space.  Mondo comment line d9 is designed to take up space - lots and lots of space.  Mondo comment line da is designed to take up space - lots and lots of space.  Mondo comment line db is designed to take up space - lots and lots of space.  Mondo comment line dc is designed to take up space - lots and lots of space.  Mondo comment line dd is designed to take up space - lots and lots of space.  Mondo comment line de is designed to take up space - lots and lots of space.  Mondo comment line df is designed to take up space - lots and lots of space.  Mondo comment line e0 is designed to take up space - lots and lots of space.  Mondo comment line e1 is designed to take up space - lots and lots of space.  Mondo comment line e2 is designed to take up space - lots and lots of space.  Mondo comment line e3 is designed to take up space - lots and lots of space.  Mondo comment line e4 is designed to take up space - lots and lots of space.  Mondo comment line e5 is designed to take up space - lots and lots of space.  Mondo comment line e6 is designed to take up space - lots and lots of space.  Mondo comment line e7 is designed to take up space - lots and lots of space.  Mondo comment line e8 is designed to take up space - lots and lots of space.  Mondo comment line e9 is designed to take up space - lots and lots of space.  Mondo comment line ea is designed to take up space - lots and lots of space.  Mondo comment line eb is designed to take up space - lots and lots of space.  Mondo comment line ec is designed to take up space - lots and lots of space.  Mondo comment line ed is designed to take up space - lots and lots of space.  Mondo comment line ee is designed to take up space - lots and lots of space.  Mondo comment line ef is designed to take up space - lots and lots of space.  Mondo comment line f0 is designed to take up space - lots and lots of space.  Mondo comment line f1 is designed to take up space - lots and lots of space.  Mondo comment line f2 is designed to take up space - lots and lots of space.  Mondo comment line f3 is designed to take up space - lots and lots of space.  Mondo comment line f4 is designed to take up space - lots and lots of space.  Mondo comment line f5 is designed to take up space - lots and lots of space.  Mondo comment line f6 is designed to take up space - lots and lots of space.  Mondo comment line f7 is designed to take up space - lots and lots of space.  Mondo comment line f8 is designed to take up space - lots and lots of space.  Mondo comment line f9 is designed to take up space - lots and lots of space.  Mondo comment line fa is designed to take up space - lots and lots of space.  Mondo comment line fb is designed to take up space - lots and lots of space.  Mondo comment line fc is designed to take up space - lots and lots of space.  Mondo comment line fd is designed to take up space - lots and lots of space.  Mondo comment line fe is designed to take up space - lots and lots of space.  Mondo comment line ff is designed to take up space - lots and lots of space."
CommentZipFile() {
	mkdir -p META-INF
	echo "Manifest-Version: 1.0" > META-INF/MANIFEST.MF
	echo "Main-Class: PrintVersion" >> META-INF/MANIFEST.MF
	if [ "$1" != "" ]; then
	    echo "JRE-Version: $1" >> META-INF/MANIFEST.MF
	fi
	cp $TESTCLASSES/PrintVersion.class .

	# The remaining code in CommentZipFile essentially replaces the
	#   following code, which added comments to the jar file.
	#   Unfortunately zipnote has been broken since 3.0 [ 2008 ] and
	#   there has been no new [ fixed ] version.  zipnote has probably
	#   always failed, or failed for a long time without causing the
	#   test to fail.  So no comments were added to the file.
	#   The comments are added using zip(1) during the creation of the
	#   zip file.
	#
	# NOTE:
	#   It seems the original intent of this test was to add a very long
	#   comment for one file.  But zip allows a max of 256 characters, so
	#   we settle for adding 256-character comments to lots of files.
	#
	# $JAR $2cMf PrintVersion PrintVersion.class AfairlyLong*
	# $JAR $2umf META-INF/MANIFEST.MF PrintVersion
	# /usr/bin/zipnote PrintVersion.zip > zipout
	# ... code to modify zipout adding comments
	# /usr/bin/zipnote -w PrintVersion.zip < zipin
	# mv PrintVersion.zip PrintVersion
	#


	for i in 0 1 2 3 4 5 6 7 8 9 ; do
		for j in 0 1 2 3 4 5 6 7 8 9 ; do
			touch AfairlyLongNameEatsUpDirectorySpaceBetter$i$j
		done
	done

        zip -$2c PrintVersion.zip PrintVersion.class AfairlyLong* META-INF/MANIFEST.MF << FINI
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
File Comment Line.
$MONDO
File Comment Line.
File Comment Line.
File Comment Line.
FINI

	rm -f AfairlyLong*

    mv PrintVersion.zip PrintVersion

}

#
# Attempt to launch a vm using a version specifier and make sure the
# resultant launch (probably a "mock vm") is appropriate.
#
LaunchVM() {
	if [ "$1" != "" ]; then
		mess="`$JAVA \"$1\" -jar PrintVersion 2>&1`"
		if [ $? -eq 0 ]; then
			echo "Unexpected success of -Version:$1"
			echo "$mess"
			exit 1
		fi
	else
		mess="`$JAVA -jar PrintVersion 2>&1`"
		if [ $? -ne 0 ]; then
			prefix=`echo "$mess" | cut -d ' ' -f 1-3`
			if [ "$prefix" != "Unable to locate" ]; then
				echo "$mess"
				exit 1
			fi
			echo "Unexpected error in attempting to locate $1"
			exit 1
		fi

	fi

	echo $mess | grep "$2" > /dev/null 2>&1
	if [ $? != 0 ]; then
	    echo "Launched $mess, expected $1"
	    exit 1
	fi
}

# Tests very long Main-Class attribute in the jar
TestLongMainClass() {
    JVER=$1
    if [ "$JVER" = "mklink" ]; then
        JVER=XX
        JDKXX=jdk/j2re$JVER
        rm -rf jdk
        mkdir jdk
        ln -s $TESTJAVA $JDKXX
        JAVA_VERSION_PATH="`pwd`/jdk"
        export JAVA_VERSION_PATH
    fi
    $JAVAEXE -cp $TESTCLASSES ZipMeUp UglyBetty.jar 4097 
    message="`$JAVAEXE -version:$JVER -jar UglyBetty.jar 2>&1`"
    echo $message | grep "Error: main-class: attribute exceeds system limits" > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        printf "Long manifest test did not get expected error"
        exit 1
    fi
    unset JAVA_VERSION_PATH
    rm -rf jdk
}

#
# Main test sequence starts here
#

RELEASE=`$JAVA -version 2>&1 | head -n 1 | cut -d ' ' -f 3 | \
  sed -e "s/\"//g"`
BASE_RELEASE=`echo $RELEASE | sed -e "s/-.*//g"`

#
# Make sure that the generic jar/manifest reading code works. Test both
# compressed and "stored" jar files.
#
# The "Ugly" jar (long manifest line) tests are only run if the combination
# of the file name length restrictions and the length of the cwd allow it.
#
CreateJar "" ""
LaunchVM "" "${RELEASE}"
CreateJar "" "0"
LaunchVM "" "${RELEASE}"
if [ `IsWindows` = "true" ]; then
    MAXIMUM_PATH=255;
else
    MAXIMUM_PATH=1024;
fi

PATH_LENGTH=`printf "%s" "$UGLYCLASS" | wc -c`
if [ ${PATH_LENGTH} -lt ${MAXIMUM_PATH} ]; then
	CreateUglyJar "" ""
	LaunchVM "" "${RELEASE}"
	CreateUglyJar "" "0"
	LaunchVM "" "${RELEASE}"
else
    printf "Warning: Skipped UglyJar test, path length exceeded, %d" $MAXIMUM_PATH
    printf " allowed, the current path is %d\n" $PATH_LENGTH
fi
CreateAlignedJar "" ""
LaunchVM "" "${RELEASE}"
CreateFullJar "" ""
LaunchVM "" "${RELEASE}"

#
# 4998210 shows that some very strange behaviors are semi-supported.
# In this case, it's the ability to prepend any kind of stuff to the
# jar file and require that the jar file still work.  Note that this
# "interface" isn't publically supported and we may choose to break
# it in the future, but this test guarantees that we won't break it
# without informed consent. We take advantage the fact that the
# "FullJar" we just tested is probably the best jar to begin with
# for this test.
#
echo "This is just meaningless bytes to prepend to the jar" > meaningless
mv PrintVersion meaningfull
cat meaningless meaningfull > PrintVersion
LaunchVM "" "${RELEASE}" 
rm meaningless meaningfull

#
# Officially, one must use "the jar command to create a jar file.  However,
# all the comments about jar commands **imply** that jar files and zip files
# are equivalent.  (Note: this isn't true due to the "0xcafe" insertion.)
# On systems which have a command line zip, test the ability to use zip
# to construct a jar and then use it (6387069).
#
if [ -x /usr/bin/zip ]; then
	CreateZippyJar "" "-q"
	LaunchVM "" "${RELEASE}"
fi

#
# jar files shouldn't have comments, but it is possible that somebody added
# one by using zip -c, zip -z, zipnote or a similar utility.  On systems
# that have "zipnote", verify this functionality.
#
# This serves a dual purpose of creating a very large "central directory
# entry" which validates to code to read such entries.
#
if [ -x /usr/bin/zipnote ]; then
	CreateFullJar "" ""
	CommentZipFile "AfairlyLongNameEatsUpDirectorySpaceBetter20"
	LaunchVM "" "${RELEASE}"
fi

exit 0
