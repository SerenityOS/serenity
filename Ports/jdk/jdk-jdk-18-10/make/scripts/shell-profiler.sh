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

# Usage: sh shell-tracer.sh <TIME_CMD_TYPE> <TIME_CMD> <OUTPUT_FILE> <shell command line>
#
# This shell script is supposed to be set as a replacement for SHELL in make,
# causing it to be called whenever make wants to execute shell commands.
# The <shell command line> is suitable for passing on to the old shell,
# typically beginning with -c.
#
# This script will run the shell command line and it will also store a simple
# log of the the time it takes to execute the command in the OUTPUT_FILE, using
# utility for time measure specified with TIME_CMD option.
#
# Type of time measure utility is specified with TIME_CMD_TYPE option.
# Recognized options values of TIME_CMD_TYPE option: "gnutime", "flock".

TIME_CMD_TYPE="$1"
TIME_CMD="$2"
OUTPUT_FILE="$3"
shift
shift
shift
if [ "$TIME_CMD_TYPE" = "gnutime" ]; then
  # Escape backslashes (\) and percent chars (%). See man for GNU 'time'.
  msg=${@//\\/\\\\}
  msg=${msg//%/%%}
  "$TIME_CMD" -f "[TIME:%E] $msg" -a -o "$OUTPUT_FILE" "$@"
elif [ "$TIME_CMD_TYPE" = "flock" ]; then
  # Emulated GNU 'time' with 'flock' and 'date'.
  ts=`date +%s%3N`
  "$@"
  status=$?
  ts2=`date +%s%3N`
  millis=$((ts2 - ts))
  ms=$(($millis % 1000))
  seconds=$((millis / 1000))
  ss=$(($seconds % 60))
  minutes=$(($seconds / 60))
  mm=$(($minutes % 60))
  hh=$(($minutes / 60)):
  [ $hh != "0:" ] || hh=
  # Synchronize on this script.
  flock -w 10 "$0" printf "[TIME:${hh}${mm}:${ss}.%.2s] %s\n" $ms "$*" >> "$OUTPUT_FILE" || true
  exit $status
else
  "$@"
fi
