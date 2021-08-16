# Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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
# @bug 4262583 4418997 4795136
# @summary Check support for jar file members with sizes > 2GB
# @author Martin Buchholz
#
# @build FileBuilder
# @run shell 3GBZipFiles.sh 9986
# @ignore runs for hours and eats up 7 Gigabytes of disk space
# @run shell/timeout=604800 3GBZipFiles.sh 3141592653
# @key randomness

# Command-line usage:
# javac FileBuilder.java && sh 3GBZipFiles.sh /path/to/jdk filesize

# -------------------------------------------------------------------
# Testing strategy: We want to test for size limits on the Jar file
# itself, as well as on the compressed and uncompressed sizes of the
# files stored therein.  All of these limits should be 4GB and should
# be tested in the 2GB-4GB range.  We also want to check that it is
# possible to store more than 6GB of actual data in a zip file, if we
# have two files of size 3GB which compress nicely.  We also want to
# test both the "STORED" and "DEFLATED" compression methods.
# -------------------------------------------------------------------

die () { echo "$1" >&2; exit 1; }

sys () { "$@" || die "Command $@ failed: rc=$?"; }

set -u

myName=`printf %s "$0" | sed 's:.*[/\\]::'`;

if test -z "${TESTJAVA-}"; then
  test "$#" -eq 2 || die "Usage: $myName /path/to/jdk filesize"
  TESTJAVA="$1"; shift
  TESTCLASSES="`pwd`"
fi

hugeSize="$1"; shift
tinySize=42

JAVA="$TESTJAVA/bin/java"
JAR="$TESTJAVA/bin/jar"

currentDir="`pwd`"
tmpDir="$myName.tmp"

cleanup () { cd "$currentDir" && rm -rf "$tmpDir"; }

trap cleanup 0 1 2 15

sys rm -rf "$tmpDir"
sys mkdir "$tmpDir"
cd "$tmpDir"

buildFile ()
{
  filetype_="$1"
  filename_="$2"
  case "$filename_" in
    huge-*) filesize_="$hugeSize" ;;
    tiny-*) filesize_="$tinySize" ;;
  esac
  sys "$JAVA" ${TESTVMOPTS} "-cp" "$TESTCLASSES" "FileBuilder" \
   "$filetype_" "$filename_" "$filesize_"
}

testJarFile ()
{
  echo "-------------------------------------------------------"
  echo "Testing $1 $2"
  echo "-------------------------------------------------------"

  filetype="$1"
  if test "$2" = "STORED"; then jarOpt="0"; else jarOpt=""; fi
  filelist="$3"
  jarFile="$myName.jar"

  for file in $filelist; do
    buildFile "$filetype" "$file"
  done

  sys "$JAR" cvM${jarOpt}f "$jarFile" $filelist
  sys ls -l "$jarFile"
  sys "$JAR" tvf "$jarFile"

  for file in $filelist; do
    case "$file" in
      huge-*) size="$hugeSize" ;;
      tiny-*) size="$tinySize" ;;
    esac
    case "`$JAR tvf $jarFile $file`" in
      *"$size"*"$file"*) : ;;
      *) die "Output of \"jar tvf\" is incorrect." ;;
    esac
    # Try to minimize disk space used while verifying the jar file.
    sum1="`sum $file`"
    sys rm "$file"
    sys "$JAR" xvf "$jarFile" "$file"
    sum2="`sum $file`"
    test "$sum1" = "$sum2" || die "Jar File is corrupted."
    sys rm "$file"
    # unzip $jarFile $file
    # sys rm "$file"
  done

  sys rm "$jarFile"
}

testJarFile "MostlyEmpty" "DEFLATED" "tiny-1 huge-1 tiny-2 huge-2 tiny-3"
testJarFile "MostlyEmpty" "STORED" "tiny-1 huge-1 tiny-2"
testJarFile "SlightlyCompressible" "DEFLATED" "tiny-1 huge-1 tiny-2"

cleanup

exit 0
