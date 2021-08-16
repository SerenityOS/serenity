#!/bin/bash
#
# Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

# Run the "tidy" program over the files in a directory.
# The "tidy" program must be on your PATH.
#
# Usage:
#   sh tidy.sh <dir>
#
# The "tidy" program will be run on each HTML file in <dir>,
# and the output placed in the corresponding location in a new
# directory <dir>.tidy.  The console output from running "tidy" will
# be saved in a corresponding file with an additional .tidy extension.
#
# Non-HTML files will be copied without modification from <dir> to
# <dir>.tidy, so that relative links within the directory tree are
# unaffected.

dir=$1
odir=$dir.tidy

( cd $dir ; find . -type f ) | \
    while read file ; do
        mkdir -p $odir/$(dirname $file)
        case $file in
            *.html )
                cat $dir/$file | tidy 1>$odir/$file 2>$odir/$file.tidy
                ;;
            * ) cp $dir/$file $odir/$file
                ;;
        esac
    done
