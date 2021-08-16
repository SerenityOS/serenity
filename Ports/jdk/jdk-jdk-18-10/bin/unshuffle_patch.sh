#!/bin/bash
#
# Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

# Script for updating a patch file as per the shuffled/unshuffled source location.

usage() {
  echo "Usage: $0 [-h|--help] [-v|--verbose] [-to9|-to10] [-r <repo>] <input_patch> <output_patch>"
  echo "where:"
  echo "  -to9            create patches appropriate for a JDK 9 source tree"
  echo "                  When going to 9, the output patches will be suffixed with the"
  echo "                  repo name"
  echo "  -to10           create patches appropriate for a JDK 10 source tree"
  echo "  -r <repo>       specify repo for source patch, set to 'top' for top repo"
  echo "  <input_patch>   is the input patch file, that needs shuffling/unshuffling"
  echo "  <output_patch>  is the updated patch file "
  echo " "
  exit 1
}

SCRIPT_DIR=`dirname $0`
UNSHUFFLE_LIST=$SCRIPT_DIR"/unshuffle_list.txt"

if [ ! -f "$UNSHUFFLE_LIST" ] ; then
  echo "FATAL: cannot find $UNSHUFFLE_LIST" >&2
  exit 1
fi

vflag="false"
while [ $# -gt 0 ]
do
  case $1 in
    -h | --help )
      usage
      ;;

    -v | --verbose )
      vflag="true"
      ;;

    -r)
      repo="$2"
      shift
      ;;

    -to9)
      shuffle_to=9
      ;;

    -to10)
      shuffle_to=10
      ;;

    -*)  # bad option
      usage
      ;;

    * )  # non option
      break
      ;;
  esac
  shift
done

# Make sure we have the right number of arguments
if [ ! $# -eq 2 ] ; then
  echo "ERROR: Invalid number of arguments." >&2
  usage
fi

# Check the given repo
repos="top corba jaxp jaxws jdk langtools nashorn hotspot"
found="false"
if [ -n "$repo" ]; then
  for r in $repos ; do
    if [ $repo = "$r" ] ; then
      found="true"
      break;
    fi
  done
  if [ $found = "false" ] ; then
    echo "ERROR: Unknown repo: $repo. Should be one of [$repos]." >&2
    usage
  fi
fi

if [ "$shuffle_to" != "9" -a "$shuffle_to" != "10" ]; then
  echo "ERROR: Must pick either -to9 or -to10"
  exit 1
fi

# When going to 10, a repo must be specified for the source patch
if [ "$shuffle_to" = "10" -a -z "$repo" ]; then
  echo "ERROR: Must specify src repo for JDK 9 patch"
  exit 1
fi

# Check given input/output files
input="$1"
if [ "x$input" = "x-" ] ; then
  input="/dev/stdin"
fi

if [ ! -f $input -a "x$input" != "x/dev/stdin" ] ; then
  echo "ERROR: Cannot find input patch file: $input" >&2
  exit 1
fi

output="$2"
if [ "x$output" = "x-" ] ; then
  output="/dev/stdout"
fi
base_output="$output"

if [ "$shuffle_to" = "10" ]; then
  if [ -f $output -a "x$output" != "x/dev/stdout" ] ; then
    echo "ERROR: Output patch already exists: $output" >&2
    exit 1
  fi
else
  for r in $repos; do
    if [ -f "$output.$r" ]; then
      echo "ERROR: Output patch already exists: $output.$r" >&2
      exit 1
    fi
  done
fi

verbose() {
  if [ ${vflag} = "true" ] ; then
    echo "$@" >&2
  fi
}

unshuffle() {
  line=$@
  verbose "Attempting to rewrite: \"$line\""

  # Retrieve the file name
  path=
  if echo "$line" | egrep '^diff' > /dev/null ; then
    if ! echo "$line" | egrep '\-\-git' > /dev/null ; then
      echo "ERROR: Only git patches supported. Please use 'hg export --git ...'." >&2
      exit 1
    fi
    path="`echo "$line" | sed -e s@'diff --git a/'@@ -e s@' b/.*$'@@`"
  elif echo "$line" | egrep '^\-\-\-' > /dev/null ; then
    path="`echo "$line" | sed -e s@'--- a/'@@`"
  elif echo "$line" | egrep '^\+\+\+' > /dev/null ; then
    path="`echo "$line" | sed s@'+++ b/'@@`"
  fi
  verbose "Extracted path: \"$path\""

  # Find the most specific matches in the shuffle list
  matches=
  if [ -n "$repo" -a "$repo" != "top" ]; then
    matchpath="$repo"/"$path"/x
  else
    matchpath="$path"/x
  fi
  while [ "$matchpath" != "" ] ; do
    matchpath="`echo $matchpath | sed s@'\(.*\)/.*$'@'\1'@`"

    if [ "$shuffle_to" =  "10" ] ; then
      pattern=": $matchpath$"
    else
      pattern="^$matchpath :"
    fi
    verbose "Attempting to find \"$matchpath\""
    matches=`egrep "$pattern" "$UNSHUFFLE_LIST"`
    if ! [ "x${matches}" = "x" ] ; then
      verbose "Got matches: [$matches]"
      break;
    fi

    if ! echo "$matchpath" | egrep '.*/.*' > /dev/null ; then
      break;
    fi
  done

  # Rewrite the line, if we have a match
  if ! [ "x${matches}" = "x" ] ; then
    shuffled="${matches%% : *}"
    unshuffled="${matches#* : }"
    patch_suffix_9=""
    for r in $repos; do
      if [ "$unshuffled" != "${unshuffled#$r}" ]; then
        unshuffled="${unshuffled#$r\/}"
        patch_suffix_9=".$r"
      fi
    done
    verbose "shuffled: $shuffled"
    verbose "unshuffled: $unshuffled"
    verbose "patch_suffix_9: $patch_suffix_9"
    if [ "$shuffle_to" =  "10" ] ; then
      newline="`echo "$line" | sed -e s@"$unshuffled"@"$shuffled"@g`"
    else
      newline="`echo "$line" | sed -e s@"$shuffled"@"$unshuffled"@g`"
      output=$base_output$patch_suffix_9
      verbose "Writing to $output"
    fi
    verbose "Rewriting to \"$newline\""
    echo "$newline" >> $output
  else
    echo "WARNING: no match found for $path"
    echo "$line" >> $output
  fi
}

while IFS= read -r line
do
  if echo "$line" | egrep '^diff|^\-\-\-|^\+\+\+' > /dev/null ; then
    unshuffle "$line"
  else
    printf "%s\n" "$line" >> $output
  fi
done < "$input"
