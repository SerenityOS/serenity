#!/bin/bash -e
#
# Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
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
# Create a bundle in the current directory, containing what's needed to run
# the 'pandoc' program by the OpenJDK build.

TMPDIR=`mktemp -d -t pandocbundle-XXXX`
trap "rm -rf \"$TMPDIR\"" EXIT

ORIG_DIR=`pwd`
cd "$TMPDIR"
PANDOC_VERSION=2.3.1
PACKAGE_VERSION=1.0
TARGET_PLATFORM=linux_x64

if [[ $TARGET_PLATFORM == linux_x64 ]] ; then
  PANDOC_PLATFORM=linux
  PANDOC_SUFFIX=tar.gz
elif [[ $TARGET_PLATFORM == macosx_x64 ]] ; then
  PANDOC_PLATFORM=macOS
  PANDOC_SUFFIX=zip
elif [[ $TARGET_PLATFORM == windows_x64 ]] ; then
  PANDOC_PLATFORM=windows-x86_64
  PANDOC_SUFFIX=zip
else
  echo "Unknown platform"
  exit 1
fi
BUNDLE_NAME=pandoc-$TARGET_PLATFORM-$PANDOC_VERSION+$PACKAGE_VERSION.tar.gz

wget https://github.com/jgm/pandoc/releases/download/$PANDOC_VERSION/pandoc-$PANDOC_VERSION-$PANDOC_PLATFORM.$PANDOC_SUFFIX

mkdir tmp
cd tmp
if [[ $PANDOC_SUFFIX == zip ]]; then
  unzip ../pandoc-$PANDOC_VERSION-$PANDOC_PLATFORM.$PANDOC_SUFFIX
else
  tar xzf ../pandoc-$PANDOC_VERSION-$PANDOC_PLATFORM.$PANDOC_SUFFIX
fi
cd ..

mkdir pandoc
if [[ $TARGET_PLATFORM == windows_x64 ]] ; then
  cp tmp/pandoc-$PANDOC_VERSION-$PANDOC_PLATFORM/pandoc.exe pandoc
  chmod +x pandoc/pandoc.exe
else
  cp tmp/pandoc-$PANDOC_VERSION/bin/pandoc pandoc
fi

tar -cvzf ../$BUNDLE_NAME pandoc
cp ../$BUNDLE_NAME "$ORIG_DIR"
