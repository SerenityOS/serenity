#
# Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

###############################################################################
# Appends a string to a path variable, only adding the : when needed.
AC_DEFUN([UTIL_APPEND_TO_PATH],
[
  if test "x$2" != x; then
    if test "x[$]$1" = x; then
      $1="$2"
    else
      $1="[$]$1:$2"
    fi
  fi
])

###############################################################################
# Prepends a string to a path variable, only adding the : when needed.
AC_DEFUN([UTIL_PREPEND_TO_PATH],
[
  if test "x$2" != x; then
    if test "x[$]$1" = x; then
      $1="$2"
    else
      $1="$2:[$]$1"
    fi
  fi
])

###############################################################################
# This will make sure the given variable points to a full and proper
# path. This means:
# 1) There will be no spaces in the path. On unix platforms,
#    spaces in the path will result in an error. On Windows,
#    the path will be rewritten using short-style to be space-free.
# 2) The path will be absolute, and it will be in unix-style (on
#     cygwin).
# $1: The name of the variable to fix
# $2: if NOFAIL, errors will be silently ignored
AC_DEFUN([UTIL_FIXUP_PATH],
[
  # Only process if variable expands to non-empty
  path="[$]$1"
  if test "x$path" != x; then
    if test "x$OPENJDK_BUILD_OS" = "xwindows"; then
      if test "x$2" = "xNOFAIL"; then
        quiet_option="-q"
      fi
      imported_path=`$FIXPATH_BASE $quiet_option import "$path"`
      $FIXPATH_BASE verify "$imported_path"
      if test $? -ne 0; then
        if test "x$2" != "xNOFAIL"; then
          AC_MSG_ERROR([The path of $1, which resolves as "$path", could not be imported.])
        else
          imported_path=""
        fi
      fi
      if test "x$imported_path" != "x$path"; then
        $1="$imported_path"
      fi
    else
      [ if [[ "$path" =~ " " ]]; then ]
        if test "x$2" != "xNOFAIL"; then
          AC_MSG_NOTICE([The path of $1, which resolves as "$path", is invalid.])
          AC_MSG_ERROR([Spaces are not allowed in this path.])
        else
          path=""
        fi
      fi

      # Use eval to expand a potential ~.
      eval new_path="$path"
      if test ! -e "$new_path"; then
        if test "x$2" != "xNOFAIL"; then
          AC_MSG_ERROR([The path of $1, which resolves as "$new_path", is not found.])
        else
          new_path=""
        fi
      fi

      # Make the path absolute
      if test "x$new_path" != x; then
        if test -d "$new_path"; then
          path="`cd "$new_path"; pwd -L`"
        else
          dir="`$DIRNAME "$new_path"`"
          base="`$BASENAME "$new_path"`"
          path="`cd "$dir"; pwd -L`/$base"
        fi
      else
        path=""
      fi

      $1="$path"
    fi
  fi
])

###############################################################################
# Check if the given file is a unix-style or windows-style executable, that is,
# if it expects paths in unix-style or windows-style.
# Returns "windows" or "unix" in $RESULT.
AC_DEFUN([UTIL_CHECK_WINENV_EXEC_TYPE],
[
  # For cygwin and msys2, if it's linked with the correct helper lib, it
  # accept unix paths
  if test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.cygwin" || \
      test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.msys2"; then
    linked_libs=`$LDD $1 2>&1`
    if test $? -ne 0; then
      # Non-binary files (e.g. shell scripts) are unix files
      RESULT=unix
    else
      [ if [[ "$linked_libs" =~ $WINENV_MARKER_DLL ]]; then ]
        RESULT=unix
      else
        RESULT=windows
      fi
    fi
  elif test "x$OPENJDK_BUILD_OS" = "xwindows"; then
    # On WSL, we can check if it is a PE file
    file_type=`$FILE -b $1 2>&1`
    [ if [[ $file_type =~ PE.*Windows ]]; then ]
      RESULT=windows
    else
      RESULT=unix
    fi
  else
    RESULT=unix
  fi
])

###############################################################################
# This will make sure the given variable points to a executable
# with a full and proper path. This means:
# 1) There will be no spaces in the path. On unix platforms,
#    spaces in the path will result in an error. On Windows,
#    the path will be rewritten using short-style to be space-free.
# 2) The path will be absolute, and it will be in unix-style (on
#     cygwin).
# Any arguments given to the executable is preserved.
# If the input variable does not have a directory specification, then
# it need to be in the PATH.
# $1: The name of the variable to fix
# $2: Where to look for the command (replaces $PATH)
# $3: set to NOFIXPATH to skip prefixing FIXPATH, even if needed on platform
AC_DEFUN([UTIL_FIXUP_EXECUTABLE],
[
  input="[$]$1"

  # Only process if variable expands to non-empty
  if test "x$input" != x; then
    # First separate the path from the arguments. This will split at the first
    # space.
    [ if [[ "$OPENJDK_BUILD_OS" = "windows" && input =~ ^$FIXPATH ]]; then
      line="${input#$FIXPATH }"
      fixpath_prefix="$FIXPATH "
    else
      line="$input"
      fixpath_prefix=""
    fi ]
    path="${line%% *}"
    arguments="${line#"$path"}"

    [ if ! [[ "$path" =~ /|\\ ]]; then ]
      # This is a command without path (e.g. "gcc" or "echo")
      command_type=`type -t "$path"`
      if test "x$command_type" = xbuiltin || test "x$command_type" = xkeyword; then
        # Shell builtin or keyword; we're done here
        new_path="$path"
      else
        # Search in $PATH using bash built-in 'type -p'.
        saved_path="$PATH"
        if test "x$2" != x; then
          PATH="$2"
        fi
        new_path=`type -p "$path"`
        if test "x$new_path" = x && test "x$OPENJDK_BUILD_OS" = "xwindows"; then
          # Try again with .exe
          new_path="`type -p "$path.exe"`"
        fi
        PATH="$saved_path"

        if test "x$new_path" = x; then
          AC_MSG_NOTICE([The command for $1, which resolves as "$input", is not found in the PATH.])
          AC_MSG_ERROR([Cannot locate $path])
        fi
      fi
    else
      # This is a path with slashes, don't look at $PATH
      if test "x$OPENJDK_BUILD_OS" = "xwindows"; then
        # fixpath.sh import will do all heavy lifting for us
        new_path=`$FIXPATH_BASE import "$path"`

        if test ! -e $new_path; then
          # It failed, but maybe spaces were part of the path and not separating
          # the command and argument. Retry using that assumption.
          new_path=`$FIXPATH_BASE import "$input"`
          if test ! -e $new_path; then
            AC_MSG_NOTICE([The command for $1, which resolves as "$input", can not be found.])
            AC_MSG_ERROR([Cannot locate $input])
          fi
          # It worked, clear all "arguments"
          arguments=""
        fi
      else # on unix
        # Make absolute
        $1="$path"
        UTIL_FIXUP_PATH($1, NOFAIL)
        new_path="[$]$1"

        if test ! -e $new_path; then
          AC_MSG_NOTICE([The command for $1, which resolves as "$input", is not found])
          [ if [[ "$path" =~ " " ]]; then ]
            AC_MSG_NOTICE([This might be caused by spaces in the path, which is not allowed.])
          fi
          AC_MSG_ERROR([Cannot locate $path])
        fi
        if test ! -x $new_path; then
          AC_MSG_NOTICE([The command for $1, which resolves as "$input", is not executable.])
          AC_MSG_ERROR([Cannot execute command at $path])
        fi
      fi # end on unix
    fi # end with or without slashes

    # Now we have a usable command as new_path, with arguments in arguments
    if test "x$OPENJDK_BUILD_OS" = "xwindows"; then
      if test "x$fixpath_prefix" = x; then
        # Only mess around if fixpath_prefix was not given
        UTIL_CHECK_WINENV_EXEC_TYPE("$new_path")
        if test "x$RESULT" = xwindows; then
          fixpath_prefix="$FIXPATH "
          # make sure we have an .exe suffix (but not two)
          new_path="${new_path%.exe}.exe"
        else
          # If we have gotten a .exe suffix, remove it
          new_path="${new_path%.exe}"
        fi
      fi
    fi

    if test "x$3" = xNOFIXPATH; then
      fixpath_prefix=""
    fi

    # Now join together the path and the arguments once again
    new_complete="$fixpath_prefix$new_path$arguments"
    $1="$new_complete"
  fi
])

###############################################################################
# Setup a tool for the given variable. If correctly specified by the user,
# use that value, otherwise search for the tool using the supplied code snippet.
# $1: variable to set
# $2: code snippet to call to look for the tool
# $3: code snippet to call if variable was used to find tool
AC_DEFUN([UTIL_SETUP_TOOL],
[
  # Publish this variable in the help.
  AC_ARG_VAR($1, [Override default value for $1])

  if [[ -z "${$1+x}" ]]; then
    # The variable is not set by user, try to locate tool using the code snippet
    $2
  else
    # The variable is set, but is it from the command line or the environment?

    # Try to remove the string !$1! from our list.
    try_remove_var=${CONFIGURE_OVERRIDDEN_VARIABLES//!$1!/}
    if test "x$try_remove_var" = "x$CONFIGURE_OVERRIDDEN_VARIABLES"; then
      # If it failed, the variable was not from the command line. Ignore it,
      # but warn the user (except for BASH, which is always set by the calling BASH).
      if test "x$1" != xBASH; then
        AC_MSG_WARN([Ignoring value of $1 from the environment. Use command line variables instead.])
      fi
      # Try to locate tool using the code snippet
      $2
    else
      # If it succeeded, then it was overridden by the user. We will use it
      # for the tool.

      # First remove it from the list of overridden variables, so we can test
      # for unknown variables in the end.
      CONFIGURE_OVERRIDDEN_VARIABLES="$try_remove_var"

      tool_override=[$]$1

      # Check if we try to supply an empty value
      if test "x$tool_override" = x; then
        AC_MSG_CHECKING([for $1])
        AC_MSG_RESULT([[[disabled by user]]])
      else
        # Split up override in command part and argument part
        tool_and_args=($tool_override)
        [ tool_command=${tool_and_args[0]} ]
        [ unset 'tool_and_args[0]' ]
        [ tool_args=${tool_and_args[@]} ]

        # Check if the provided tool contains a complete path.
        tool_basename="${tool_command##*/}"
        if test "x$tool_basename" = "x$tool_command"; then
          # A command without a complete path is provided, search $PATH.
          AC_MSG_NOTICE([Will search for user supplied tool "$tool_basename"])
          AC_PATH_PROGS($1, $tool_basename ${tool_basename}.exe)
          tool_command="[$]$1"
          if test "x$tool_command" = x; then
            AC_MSG_ERROR([User supplied tool $1="$tool_basename" could not be found in PATH])
          fi
        else
          # Otherwise we believe it is a complete path. Use it as it is.
          if test ! -x "$tool_command" && test ! -x "${tool_command}.exe"; then
            AC_MSG_ERROR([User supplied tool $1="$tool_command" does not exist or is not executable])
          fi
          if test ! -x "$tool_command"; then
            tool_command="${tool_command}.exe"
          fi
          $1="$tool_command"
        fi
        if test "x$tool_args" != x; then
          # If we got arguments, re-append them to the command after the fixup.
          $1="[$]$1 $tool_args"
        fi
        AC_MSG_CHECKING([for $1])
        AC_MSG_RESULT([[$]$1 [[user supplied]]])
      fi
    fi
    $3
  fi
])

###############################################################################
# Locate a tool using proper methods.
# $1: variable to set
# $2: executable name (or list of names) to look for
# $3: [path]
# $4: set to NOFIXPATH to skip prefixing FIXPATH, even if needed on platform
AC_DEFUN([UTIL_LOOKUP_PROGS],
[
  UTIL_SETUP_TOOL($1, [
    $1=""

    if test "x$3" != x; then
      old_path="$PATH"
      PATH="$3"
    fi

    for name in $2; do
      AC_MSG_CHECKING(for $name)

      command_type=`type -t "$name"`
      if test "x$command_type" = xbuiltin || test "x$command_type" = xkeyword; then
        # Shell builtin or keyword; we're done here
        full_path="$name"
        $1="$full_path"
        AC_MSG_RESULT([[$full_path [builtin]]])
        break
      else
        # Search in $PATH
        old_ifs="$IFS"
        IFS=":"
        for elem in $PATH; do
          IFS="$old_ifs"
          if test "x$elem" = x; then
            continue
          fi
          full_path="$elem/$name"
          if test ! -e "$full_path" && test "x$OPENJDK_BUILD_OS" = "xwindows"; then
            # Try again with .exe
            full_path="$elem/$name.exe"
          fi
          if test -x "$full_path" && test ! -d "$full_path" ; then
            $1="$full_path"
            UTIL_FIXUP_EXECUTABLE($1, $3, $4)
            result="[$]$1"

            # If we have FIXPATH enabled, strip all instances of it and prepend
            # a single one, to avoid double fixpath prefixing.
            if test "x$4" != xNOFIXPATH; then
              [ if [[ $FIXPATH != "" && $result =~ ^"$FIXPATH " ]]; then ]
                result="\$FIXPATH ${result#"$FIXPATH "}"
              fi
            fi
            AC_MSG_RESULT([$result])
            break 2;
          fi
        done
        IFS="$old_ifs"
      fi
      AC_MSG_RESULT([[[not found]]])
    done

    if test "x$3" != x; then
      PATH="$old_path"
    fi
  ])
])

###############################################################################
# Call UTIL_SETUP_TOOL with AC_CHECK_TOOLS to locate the tool. This will look
# first for cross-compilation tools.
# $1: variable to set
# $2: executable name (or list of names) to look for
# $3: [path]
AC_DEFUN([UTIL_LOOKUP_TOOLCHAIN_PROGS],
[
  if test "x$ac_tool_prefix" = x; then
    UTIL_LOOKUP_PROGS($1, $2, $3)
  else
    prefixed_names=$(for name in $2; do echo ${ac_tool_prefix}${name} $name; done)
    UTIL_LOOKUP_PROGS($1, $prefixed_names, $3)
  fi
])

###############################################################################
# Test that variable $1 denoting a program is not empty. If empty, exit with an error.
# $1: variable to check
AC_DEFUN([UTIL_CHECK_NONEMPTY],
[
  if test "x[$]$1" = x; then
    AC_MSG_ERROR([Could not find required tool for $1])
  fi
])

###############################################################################
# Like UTIL_LOOKUP_PROGS but fails if no tool was found.
# $1: variable to set
# $2: executable name (or list of names) to look for
# $3: [path]
AC_DEFUN([UTIL_REQUIRE_PROGS],
[
  UTIL_LOOKUP_PROGS($1, $2, $3)
  UTIL_CHECK_NONEMPTY($1)
])

###############################################################################
# Like UTIL_LOOKUP_PROGS but fails if no tool was found.
# $1: variable to set
# $2: executable name (or list of names) to look for
# $3: [path]
AC_DEFUN([UTIL_REQUIRE_TOOLCHAIN_PROGS],
[
  UTIL_LOOKUP_TOOLCHAIN_PROGS($1, $2, $3)
  UTIL_CHECK_NONEMPTY($1)
])


###############################################################################
# Like UTIL_SETUP_TOOL but fails if no tool was found.
# $1: variable to set
# $2: autoconf macro to call to look for the special tool
AC_DEFUN([UTIL_REQUIRE_SPECIAL],
[
  UTIL_SETUP_TOOL($1, [$2])
  UTIL_CHECK_NONEMPTY($1)
  # The special macro will return an absolute path, and is only used for
  # unix tools. No further processing needed.
])

###############################################################################
# Add FIXPATH prefix to variable. Normally this is done by UTIL_LOOKUP_PROGS
# or UTIL_FIXUP_EXECUTABLE, but in some circumstances this has to be done
# explicitly, such as when the command in question does not exist yet.
#
# $1: variable to add fixpath to
AC_DEFUN([UTIL_ADD_FIXPATH],
[
  if test "x$FIXPATH" != x; then
    $1="$FIXPATH [$]$1"
  fi
])

###############################################################################
AC_DEFUN([UTIL_REMOVE_SYMBOLIC_LINKS],
[
  if test "x$OPENJDK_BUILD_OS" != xwindows; then
    # Follow a chain of symbolic links. Use readlink
    # where it exists, else fall back to horribly
    # complicated shell code.
    if test "x$READLINK_TESTED" != yes; then
      # On MacOSX there is a readlink tool with a different
      # purpose than the GNU readlink tool. Check the found readlink.
      READLINK_ISGNU=`$READLINK --version 2>&1 | $GREP GNU`
      # If READLINK_ISGNU is empty, then it's a non-GNU readlink. Don't use it.
      READLINK_TESTED=yes
    fi

    if test "x$READLINK" != x && test "x$READLINK_ISGNU" != x; then
      $1=`$READLINK -f [$]$1`
    else
      # Save the current directory for restoring afterwards
      STARTDIR=$PWD
      COUNTER=0
      sym_link_dir=`$DIRNAME [$]$1`
      sym_link_file=`$BASENAME [$]$1`
      cd $sym_link_dir
      # Use -P flag to resolve symlinks in directories.
      cd `pwd -P`
      sym_link_dir=`pwd -P`
      # Resolve file symlinks
      while test $COUNTER -lt 20; do
        ISLINK=`$LS -l $sym_link_dir/$sym_link_file | $GREP '\->' | $SED -e 's/.*-> \(.*\)/\1/'`
        if test "x$ISLINK" == x; then
          # This is not a symbolic link! We are done!
          break
        fi
        # Again resolve directory symlinks since the target of the just found
        # link could be in a different directory
        cd `$DIRNAME $ISLINK`
        sym_link_dir=`pwd -P`
        sym_link_file=`$BASENAME $ISLINK`
        let COUNTER=COUNTER+1
      done
      cd $STARTDIR
      $1=$sym_link_dir/$sym_link_file
    fi
  fi
])
