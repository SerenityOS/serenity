#!/bin/bash
#
# Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

# Usage: ./logger.sh theloggfile acommand arg1 arg2
#
# Execute acommand with args, in such a way that
# both stdout and stderr from acommand are appended to
# theloggfile.
#
# Preserve stdout and stderr, so that the stdout
# from logger.sh is the same from acommand and equally
# for stderr.
#
# Propagate the result code from acommand so that
# ./logger.sh exits with the same result code.

# Create a temporary directory to store the result code from
# the wrapped command.
RCDIR=`mktemp -dt jdk-build-logger.tmp.XXXXXX` || exit $?
trap "rm -rf \"$RCDIR\"" EXIT
LOGFILE=$1
shift

# We need to handle command likes like "VAR1=val1 /usr/bin/cmd VAR2=val2".
# Do this by shifting away prepended variable assignments, and export them
# instead.
is_prefix=true
for opt; do
  if [[ "$is_prefix" = true && "$opt" =~ ^.*=.*$ ]]; then
    export $opt
    shift
  else
    is_prefix=false
  fi
done

(exec 3>&1 ; ("$@" 2>&1 1>&3; echo $? > "$RCDIR/rc") | tee -a $LOGFILE 1>&2 ; exec 3>&-) | tee -a $LOGFILE
exit `cat "$RCDIR/rc"`
