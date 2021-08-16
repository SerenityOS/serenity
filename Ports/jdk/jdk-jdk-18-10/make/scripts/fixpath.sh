#!/bin/bash
#
# Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

# Setup the environment fixpath assumes. Read from command line options if
# available, or extract values automatically from the environment if missing.
# This is robust, but slower.
function setup() {
  while getopts "e:p:r:t:c:qmi" opt; do
    case "$opt" in
    e) PATHTOOL="$OPTARG" ;;
    p) DRIVEPREFIX="$OPTARG" ;;
    r) ENVROOT="$OPTARG" ;;
    t) WINTEMP="$OPTARG" ;;
    c) CMD="$OPTARG" ;;
    q) QUIET=true ;;
    m) MIXEDMODE=true ;;
    i) IGNOREFAILURES=true ;;
    ?)
      # optargs found argument error
      exit 2
      ;;
    esac
  done

  shift $((OPTIND-1))
  ACTION="$1"

  # Locate variables ourself if not giving from caller
  if [[ -z ${PATHTOOL+x} ]]; then
    PATHTOOL="$(type -p cygpath)"
    if [[ $PATHTOOL == "" ]]; then
      PATHTOOL="$(type -p wslpath)"
      if [[ $PATHTOOL == "" ]]; then
        if [[ $QUIET != true ]]; then
          echo fixpath: failure: Cannot locate cygpath or wslpath >&2
        fi
        exit 2
      fi
    fi
  fi

  if [[ -z ${DRIVEPREFIX+x} ]]; then
    winroot="$($PATHTOOL -u c:/)"
    DRIVEPREFIX="${winroot%/c/}"
  else
    if [[ $DRIVEPREFIX == "NONE" ]]; then
      DRIVEPREFIX=""
    fi
  fi

  if [[ -z ${ENVROOT+x} ]]; then
    unixroot="$($PATHTOOL -w / 2> /dev/null)"
    # Remove trailing backslash
    ENVROOT="${unixroot%\\}"
  elif [[ "$ENVROOT" == "[unavailable]" ]]; then
    ENVROOT=""
  fi

  if [[ -z ${CMD+x} ]]; then
    CMD="$DRIVEPREFIX/c/windows/system32/cmd.exe"
  fi

  if [[ -z ${WINTEMP+x} ]]; then
    wintemp_win="$($CMD /q /c echo %TEMP% 2>/dev/null | tr -d \\n\\r)"
    WINTEMP="$($PATHTOOL -u "$wintemp_win")"
  fi

  # Make regexp tests case insensitive
  shopt -s nocasematch
  # Prohibit msys2 from meddling with paths
  export MSYS2_ARG_CONV_EXCL="*"
  #  Make sure WSL gets a copy of the path
  export WSLENV=PATH/l
}

# Cleanup handling
TEMPDIRS=""
trap "cleanup" EXIT
function cleanup() {
  if [[ "$TEMPDIRS" != "" ]]; then
    rm -rf $TEMPDIRS
  fi
}

# Import a single path
# Result: imported path returned in $result
function import_path() {
  path="$1"
  # Strip trailing and leading space
  path="${path#"${path%%[![:space:]]*}"}"
  path="${path%"${path##*[![:space:]]}"}"

  if [[ $path =~ ^.:[/\\].*$ ]] || [[ "$path" =~ ^"$ENVROOT"\\.*$ ]] ; then
    # We got a Windows path as input; use pathtool to convert to unix path
    path="$($PATHTOOL -u "$path")"
    # Path will now be absolute
  else
    # Make path absolute, and resolve embedded '..' in path
    dirpart="$(dirname "$path")"
    dirpart="$(cd "$dirpart" 2>&1 > /dev/null && pwd)"
    if [[ $? -ne 0 ]]; then
      if [[ $QUIET != true ]]; then
        echo fixpath: failure: Directory containing path "'"$path"'" does not exist >&2
      fi
      if [[ $IGNOREFAILURES != true ]]; then
        exit 1
      else
        path=""
      fi
    else
      basepart="$(basename "$path")"
      if [[ $dirpart == / ]]; then
        # Avoid double leading /
        dirpart=""
      fi
      if [[ $basepart == / ]]; then
        # Avoid trailing /
        basepart=""
      fi
      path="$dirpart/$basepart"
    fi
  fi

  if [[ "$path" != "" ]]; then
    # Store current unix path
    unixpath="$path"
    # Now turn it into a windows path
    winpath="$($PATHTOOL -w "$path" 2>/dev/null)"
    # If it fails, try again with an added .exe (needed on WSL)
    if [[ $? -ne 0 ]]; then
      unixpath="$unixpath.exe"
      winpath="$($PATHTOOL -w "$unixpath" 2>/dev/null)"
    fi
    if [[ $? -eq 0 ]]; then
      if [[ ! "$winpath" =~ ^"$ENVROOT"\\.*$ ]] ; then
        # If it is not in envroot, it's a generic windows path
        if [[ ! $winpath =~ ^[-_.:\\a-zA-Z0-9]*$ ]] ; then
          # Path has forbidden characters, rewrite as short name
          # This monster of a command uses the %~s support from cmd.exe to
          # reliably convert to short paths on all winenvs.
          shortpath="$($CMD /q /c for %I in \( "$winpath" \) do echo %~sI 2>/dev/null | tr -d \\n\\r)"
          unixpath="$($PATHTOOL -u "$shortpath")"
          # unixpath is based on short name
        fi
        # Make it lower case
        path="$(echo "$unixpath" | tr '[:upper:]' '[:lower:]')"
      fi
    else
      # On WSL1, PATHTOOL will fail for files in envroot. If the unix path
      # exists, we assume that $path is a valid unix path.

      if [[ ! -e $path ]]; then
        if [[ -e $path.exe ]]; then
          path="$path.exe"
        else
          if [[ $QUIET != true ]]; then
            echo fixpath: warning: Path "'"$path"'" does not exist >&2
          fi
          # This is not a fatal error, maybe the path will be created later on
        fi
      fi
    fi
  fi

  if [[ "$path" =~ " " ]]; then
    # Our conversion attempts failed. Perhaps the path did not exists, and thus
    # we could not convert it to short name.
    if [[ $QUIET != true ]]; then
      echo fixpath: failure: Path "'"$path"'" contains space >&2
    fi
    if [[ $IGNOREFAILURES != true ]]; then
      exit 1
    else
      path=""
    fi
  fi

  result="$path"
}

# Import a single path, or a pathlist in Windows style (i.e. ; separated)
# Incoming paths can be in Windows or unix style.
# Returns in $result a converted path or path list
function import_command_line() {
  imported=""

  old_ifs="$IFS"
  IFS=";"
  for arg in $1; do
    if ! [[ $arg =~ ^" "+$ ]]; then
      import_path "$arg"

      if [[ "$result" != "" && "$imported" = "" ]]; then
        imported="$result"
      else
        imported="$imported:$result"
      fi
    fi
  done
  IFS="$old_ifs"

  result="$imported"
}

# If argument seems to be colon separated path list, and all elements
# are possible to convert to paths, make a windows path list
# Return 0 if successful with converted path list in $result, or
# 1 if it was not a path list.
function convert_pathlist() {
  converted_list=""
  pathlist_args="$1"

  IFS=':' read -r -a arg_array <<< "$pathlist_args"
  for arg in "${arg_array[@]}"; do
    winpath=""
    # Start looking for drive prefix
    if [[ $arg =~ ^($DRIVEPREFIX/)([a-z])(/[^/]+.*$) ]] ; then
      winpath="${BASH_REMATCH[2]}:${BASH_REMATCH[3]}"
      # Change slash to backslash (or vice versa if mixed mode)
      if [[ $MIXEDMODE != true ]]; then
        winpath="${winpath//'/'/'\'}"
      else
        winpath="${winpath//'\'/'/'}"
      fi
    elif [[ $arg =~ ^(/[-_.*a-zA-Z0-9]+(/[-_.*a-zA-Z0-9]+)+.*$) ]] ; then
      # This looks like a unix path, like /foo/bar
      pathmatch="${BASH_REMATCH[1]}"
      if [[ $ENVROOT == "" ]]; then
        if [[ $QUIET != true ]]; then
          echo fixpath: failure: Path "'"$pathmatch"'" cannot be converted to Windows path >&2
        fi
        exit 1
      fi
      winpath="$ENVROOT$pathmatch"
      # Change slash to backslash (or vice versa if mixed mode)
      if [[ $MIXEDMODE != true ]]; then
        winpath="${winpath//'/'/'\'}"
      else
        winpath="${winpath//'\'/'/'}"
      fi
    else
      # This does not look like a path, so assume this is not a proper pathlist.
      # Flag this to caller.
      result=""
      return 1
    fi

    if [[ "$converted_list" = "" ]]; then
      converted_list="$winpath"
    else
      converted_list="$converted_list;$winpath"
    fi
  done

  result="$converted_list"
  return 0
}

# The central conversion function. Convert a single argument, so that any
# contained paths are converted to Windows style paths. Result is returned
# in $result. If it is a path list, convert it as one.
function convert_path() {
  if [[ $1 =~ : ]]; then
    convert_pathlist "$1"
    if [[ $? -eq 0 ]]; then
      return 0
    fi
    # Not all elements was possible to convert to Windows paths, so we
    # presume it is not a pathlist. Continue using normal conversion.
  fi

  arg="$1"
  winpath=""
  # Start looking for drive prefix. Also allow /xxxx prefixes (typically options
  # for Visual Studio tools), and embedded file:// URIs.
  if [[ $arg =~ ^([^/]*|-[^:=]*[:=]|.*file://|/[a-zA-Z:]{1,3}:?)($DRIVEPREFIX/)([a-z])(/[^/]+.*$) ]] ; then
    prefix="${BASH_REMATCH[1]}"
    winpath="${BASH_REMATCH[3]}:${BASH_REMATCH[4]}"
    # Change slash to backslash (or vice versa if mixed mode)
    if [[ $MIXEDMODE != true ]]; then
      winpath="${winpath//'/'/'\'}"
    else
      winpath="${winpath//'\'/'/'}"
    fi
  elif [[ $arg =~ ^([^/]*|-[^:=]*[:=]|(.*file://))(/([-_.+a-zA-Z0-9]+)(/[-_.+a-zA-Z0-9]+)+)(.*)?$ ]] ; then
    # This looks like a unix path, like /foo/bar. Also embedded file:// URIs.
    prefix="${BASH_REMATCH[1]}"
    pathmatch="${BASH_REMATCH[3]}"
    firstdir="${BASH_REMATCH[4]}"
    suffix="${BASH_REMATCH[6]}"

    # We only believe this is a path if the first part is an existing directory
    if [[ -d "/$firstdir" ]];  then
      if [[ $ENVROOT == "" ]]; then
        if [[ $QUIET != true ]]; then
          echo fixpath: failure: Path "'"$pathmatch"'" cannot be converted to Windows path >&2
        fi
        exit 1
      fi
      winpath="$ENVROOT$pathmatch"
      # Change slash to backslash (or vice versa if mixed mode)
      if [[ $MIXEDMODE != true ]]; then
        winpath="${winpath//'/'/'\'}"
      else
        winpath="${winpath//'\'/'/'}"
      fi
      winpath="$winpath$suffix"
    fi
  fi

  if [[ $winpath != "" ]]; then
    result="$prefix$winpath"
  else
    # Return the arg unchanged
    result="$arg"
  fi
}

# Treat $1 as name of a file containg paths. Convert those paths to Windows style,
# in a new temporary file, and return a string "@<temp file>" pointing to that
# new file.
function convert_at_file() {
  infile="$1"
  if [[ -e $infile ]] ; then
    tempdir=$(mktemp -dt fixpath.XXXXXX -p "$WINTEMP")
    TEMPDIRS="$TEMPDIRS $tempdir"

    while read line; do
      convert_path "$line"
      echo "$result" >> $tempdir/atfile
    done < $infile
    convert_path "$tempdir/atfile"
    result="@$result"
  else
    result="@$infile"
  fi
}

# Convert an entire command line, replacing all unix paths with Windows paths,
# and all unix-style path lists (colon separated) with Windows-style (semicolon
# separated).
function print_command_line() {
  converted_args=""
  for arg in "$@" ; do
    if [[ $arg =~ ^@(.*$) ]] ; then
      # This is an @-file with paths that need converting
      convert_at_file "${BASH_REMATCH[1]}"
    else
      convert_path "$arg"
    fi
    converted_args="$converted_args$result "
  done
  result="${converted_args% }"
}

# Check if the winenv will allow us to start a Windows program when we are
# standing in the current directory
function verify_current_dir() {
  arg="$PWD"
  if [[ $arg =~ ^($DRIVEPREFIX/)([a-z])(/[^/]+.*$) ]] ; then
    return 0
  elif [[ $arg =~ ^(/[^/]+.*$) ]] ; then
    if [[ $ENVROOT == "" || $ENVROOT =~ ^\\\\.* ]]; then
      # This is a WSL1 or WSL2 environment
      return 1
    fi
    return 0
  fi
  # This should not happen
  return 1
}

# The core functionality of fixpath. Take the given command line, and convert
# it and execute it, so that all paths are converted to Windows style.
# The return code is the return code of the executed command.
function exec_command_line() {
  # Check that Windows can handle our current directory (only an issue for WSL)
  verify_current_dir

  if [[ $? -ne 0 ]]; then
    # WSL1 will just forcefully put us in C:\Windows\System32 if we execute this from
    # a unix directory. WSL2 will do the same, and print a warning. In both cases,
    # we prefer to take control.
    cd "$WINTEMP"
    if [[ $QUIET != true ]]; then
      echo fixpath: warning: Changing directory to $WINTEMP >&2
    fi
  fi

  collected_args=()
  command=""
  for arg in "$@" ; do
    if [[ $command == "" ]]; then
      # We have not yet located the command to run
      if [[ $arg =~ ^(.*)=(.*)$ ]]; then
        # It's a leading env variable assignment (FOO=bar)
        key="${BASH_REMATCH[1]}"
        arg="${BASH_REMATCH[2]}"
        convert_path "$arg"
        # Set the variable to the converted result
        export $key="$result"
        # While this is only needed on WSL, it does not hurt to do everywhere
        export WSLENV=$WSLENV:$key/w
      else
        # The actual command will be executed by bash, so don't convert it
        command="$arg"
      fi
    else
      # Now we are collecting arguments; they all need converting
      if [[ $arg =~ ^@(.*$) ]] ; then
        # This is an @-file with paths that need converting
        convert_at_file "${BASH_REMATCH[1]}"
      else
        convert_path "$arg"
      fi
      collected_args=("${collected_args[@]}" "$result")
    fi
  done

  # Now execute it
  if [[ -v DEBUG_FIXPATH ]]; then
    echo fixpath: debug: input: "$@" >&2
    echo fixpath: debug: output: "$command" "${collected_args[@]}" >&2
  fi

  if [[ ! -e "$command" ]]; then
    if [[ -e "$command.exe" ]]; then
      command="$command.exe"
    fi
  fi

  if [[ $ENVROOT != "" || ! -x /bin/grep ]]; then
    "$command" "${collected_args[@]}"
  else
    # For WSL1, automatically strip away warnings from WSLENV=PATH/l
    "$command" "${collected_args[@]}" 2> >(/bin/grep -v "ERROR: UtilTranslatePathList" 1>&2)
  fi
}

# Check that the input represents a path that is reachable from Windows
function verify_command_line() {
  arg="$1"
  if [[ $arg =~ ^($DRIVEPREFIX/)([a-z])(/[^/]+.*$) ]] ; then
    return 0
  elif [[ $arg =~ ^(/[^/]+/[^/]+.*$) ]] ; then
    if [[ $ENVROOT != "" ]]; then
      return 0
    fi
  fi
  return 1
}

#### MAIN FUNCTION

setup "$@"
# Shift away the options processed in setup
shift $((OPTIND))

if [[ "$ACTION" == "import" ]] ; then
  import_command_line "$@"
  echo "$result"
elif [[ "$ACTION" == "print" ]] ; then
  print_command_line "$@"
  echo "$result"
elif [[ "$ACTION" == "exec" ]] ; then
  exec_command_line "$@"
  # Propagate exit code
  exit $?
elif [[ "$ACTION" == "verify" ]] ; then
  verify_command_line "$@"
  exit $?
else
  if [[ $QUIET != true ]]; then
    echo Unknown operation: "$ACTION" >&2
    echo Supported operations: import print exec verify >&2
  fi
  exit 2
fi
