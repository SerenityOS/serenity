#!/bin/bash
#
# Copyright 2015 Google, Inc.  All Rights Reserved.
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

usage() {
  (
    echo "$0 DIR ..."
    echo "Modifies in place all the java source files found"
    echo "in the given directories so that all java language modifiers"
    echo "are in the canonical order given by Modifier#toString()."
    echo "Tries to get it right even within javadoc comments,"
    echo "and even if the list of modifiers spans 2 lines."
    echo
    echo "See:"
    echo "https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Modifier.html#toString-int-"
    echo
    echo "Example:"
    echo "$0 jdk/src/java.base jdk/test/java/{util,io,lang}"
  ) >&2
  exit 1
}

set -eu
declare -ar dirs=("$@")
[[ "${#dirs[@]}" > 0 ]] || usage
for dir in "${dirs[@]}"; do [[ -d "$dir" ]] || usage; done

declare -ar modifiers=(
  public protected private
  abstract static final transient
  volatile synchronized native strictfp
)
declare -r SAVE_IFS="$IFS"
for ((i = 3; i < "${#modifiers[@]}"; i++)); do
  IFS='|'; x="${modifiers[*]:0:i}" y="${modifiers[*]:i}"; IFS="$SAVE_IFS"
  if [[ -n "$x" && -n "$y" ]]; then
    find "${dirs[@]}" -name '*.java' -type f -print0 | \
      xargs -0 perl -0777 -p -i -e \
      "do {} while s/^([A-Za-z@* ]*)\b($y)(\s|(?:\s|\n\s+\*)*\s)($x)\b/\1\4\3\2/mg"
  fi
done
