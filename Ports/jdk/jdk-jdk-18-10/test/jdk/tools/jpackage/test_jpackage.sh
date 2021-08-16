#!/bin/bash

# Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
# Complete testing of jpackage platform-specific packaging.
#
# The script does the following:
# 1. Create packages.
# 2. Install created packages.
# 3. Verifies packages are installed.
# 4. Uninstall created packages.
# 5. Verifies packages are uninstalled.
#
# For the list of accepted command line arguments see `run_tests.sh` script.
#

# Fail fast
set -e; set -o pipefail;

# Script debug
dry_run=${JPACKAGE_TEST_DRY_RUN}

# Default directory where jpackage should write bundle files
output_dir=~/jpackage_bundles


set_args ()
{
  args=()
  local arg_is_output_dir=
  local arg_is_mode=
  local output_dir_set=
  local with_append_actions=yes
  for arg in "$@"; do
    if [ "$arg" == "-o" ]; then
      arg_is_output_dir=yes
      output_dir_set=yes
    elif [ "$arg" == "-m" ]; then
      arg_is_mode=yes
      continue
    elif [ "$arg" == '--' ]; then
      append_actions
      with_append_actions=
      continue
    elif ! case "$arg" in -Djpackage.test.action=*) false;; esac; then
      continue
    elif [ -n "$arg_is_output_dir" ]; then
      arg_is_output_dir=
      output_dir="$arg"
    elif [ -n "$arg_is_mode" ]; then
      arg_is_mode=
      continue
    fi

    args+=( "$arg" )
  done
  [ -n "$output_dir_set" ] || args=( -o "$output_dir" "${args[@]}" )
  [ -z "$with_append_actions" ] || append_actions
}


append_actions ()
{
  args+=( '--' '-Djpackage.test.action=create,install,verify-install,uninstall,verify-uninstall' )
}


exec_command ()
{
  if [ -n "$dry_run" ]; then
    echo "$@"
  else
    eval "$@"
  fi
}

set_args "$@"
basedir="$(dirname $0)"
exec_command ${SHELL} "$basedir/run_tests.sh" -m create "${args[@]}"
