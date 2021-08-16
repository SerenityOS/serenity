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

################################################################################
# The order of these defines the priority by which we try to find them.
VALID_VS_VERSIONS="2019 2017"

VS_DESCRIPTION_2017="Microsoft Visual Studio 2017"
VS_VERSION_INTERNAL_2017=141
VS_MSVCR_2017=vcruntime140.dll
VS_MSVCP_2017=msvcp140.dll
VS_ENVVAR_2017="VS150COMNTOOLS"
VS_USE_UCRT_2017="true"
VS_VS_INSTALLDIR_2017="Microsoft Visual Studio/2017"
VS_EDITIONS_2017="BuildTools Community Professional Enterprise"
VS_SDK_INSTALLDIR_2017=
VS_VS_PLATFORM_NAME_2017="v141"
VS_SDK_PLATFORM_NAME_2017=
VS_SUPPORTED_2017=true
VS_TOOLSET_SUPPORTED_2017=true

VS_DESCRIPTION_2019="Microsoft Visual Studio 2019"
VS_VERSION_INTERNAL_2019=142
VS_MSVCR_2019=vcruntime140.dll
VS_VCRUNTIME_1_2019=vcruntime140_1.dll
VS_MSVCP_2019=msvcp140.dll
VS_ENVVAR_2019="VS160COMNTOOLS"
VS_USE_UCRT_2019="true"
VS_VS_INSTALLDIR_2019="Microsoft Visual Studio/2019"
VS_EDITIONS_2019="BuildTools Community Professional Enterprise"
VS_SDK_INSTALLDIR_2019=
VS_VS_PLATFORM_NAME_2019="v142"
VS_SDK_PLATFORM_NAME_2019=
VS_SUPPORTED_2019=true
VS_TOOLSET_SUPPORTED_2019=true

################################################################################

AC_DEFUN([TOOLCHAIN_CHECK_POSSIBLE_VISUAL_STUDIO_ROOT],
[
  if test "x$VS_ENV_CMD" = x; then
    TARGET_CPU="$1"
    VS_VERSION="$2"
    VS_BASE="$3"
    METHOD="$4"

    UTIL_FIXUP_PATH(VS_BASE, NOFAIL)

    if test "x$VS_BASE" != x && test -d "$VS_BASE"; then
      # In VS 2017 and VS 2019, the default installation is in a subdir named after the edition.
      # Find the first one present and use that.
      if test "x$VS_EDITIONS" != x; then
        for edition in $VS_EDITIONS; do
          if test -d "$VS_BASE/$edition"; then
            VS_BASE="$VS_BASE/$edition"
            break
          fi
        done
      fi

      AC_MSG_NOTICE([Found Visual Studio installation at $VS_BASE using $METHOD])
      if test "x$TARGET_CPU" = xx86; then
        VCVARSFILES="vc/bin/vcvars32.bat vc/auxiliary/build/vcvars32.bat"
      elif test "x$TARGET_CPU" = xx86_64; then
        VCVARSFILES="vc/bin/amd64/vcvars64.bat vc/bin/x86_amd64/vcvarsx86_amd64.bat \
            vc/auxiliary/build/vcvarsx86_amd64.bat vc/auxiliary/build/vcvars64.bat"
      elif test "x$TARGET_CPU" = xaarch64; then
        # for host x86-64, target aarch64
        VCVARSFILES="vc/auxiliary/build/vcvarsamd64_arm64.bat \
            vc/auxiliary/build/vcvarsx86_arm64.bat"
      fi

      for VCVARSFILE in $VCVARSFILES; do
        if test -f "$VS_BASE/$VCVARSFILE"; then
          VS_ENV_CMD="$VS_BASE/$VCVARSFILE"
          break
        fi
      done

      if test "x$VS_ENV_CMD" = x; then
        AC_MSG_NOTICE([Warning: None of $VCVARSFILES were found, Visual Studio installation not recognized. Ignoring])
      else
        # PLATFORM_TOOLSET is used during the compilation of the freetype sources
        # (see 'LIB_BUILD_FREETYPE' in libraries.m4) and must be one of 'v100',
        # 'v110' or 'v120' for VS 2010, 2012 or VS2013
        eval PLATFORM_TOOLSET="\${VS_VS_PLATFORM_NAME_${VS_VERSION}}"
      fi
    fi
  fi
])

################################################################################

AC_DEFUN([TOOLCHAIN_CHECK_POSSIBLE_WIN_SDK_ROOT],
[
  if test "x$VS_ENV_CMD" = x; then
    TARGET_CPU="$1"
    VS_VERSION="$2"
    WIN_SDK_BASE="$3"
    METHOD="$4"

    UTIL_FIXUP_PATH(WIN_SDK_BASE, NOFAIL)

    if test "x$WIN_SDK_BASE" != x && test -d "$WIN_SDK_BASE"; then
      # There have been cases of partial or broken SDK installations. A missing
      # lib dir is not going to work.
      if test ! -d "$WIN_SDK_BASE/lib"; then
        AC_MSG_NOTICE([Found Windows SDK installation at $WIN_SDK_BASE using $METHOD])
        AC_MSG_NOTICE([Warning: Installation is broken, lib dir is missing. Ignoring])
      elif test -f "$WIN_SDK_BASE/bin/setenv.cmd"; then
        AC_MSG_NOTICE([Found Windows SDK installation at $WIN_SDK_BASE using $METHOD])
        VS_ENV_CMD="$WIN_SDK_BASE/bin/setenv.cmd"
        if test "x$TARGET_CPU" = xx86; then
          VS_ENV_ARGS="/x86"
        elif test "x$TARGET_CPU" = xx86_64; then
          VS_ENV_ARGS="/x64"
        elif test "x$TARGET_CPU" = xaarch64; then
          VS_ENV_ARGS="/arm64"
        fi
        # PLATFORM_TOOLSET is used during the compilation of the freetype sources (see
        # 'LIB_BUILD_FREETYPE' in libraries.m4) and must be 'Windows7.1SDK' for Windows7.1SDK
        # TODO: improve detection for other versions of SDK
        eval PLATFORM_TOOLSET="\${VS_SDK_PLATFORM_NAME_${VS_VERSION}}"
      else
        AC_MSG_NOTICE([Found Windows SDK installation at $WIN_SDK_BASE using $METHOD])
        AC_MSG_NOTICE([Warning: Installation is broken, SetEnv.Cmd is missing. Ignoring])
      fi
    fi
  fi
])

################################################################################
# Finds the bat or cmd file in Visual Studio or the SDK that sets up a proper
# build environment and assigns it to VS_ENV_CMD
AC_DEFUN([TOOLCHAIN_FIND_VISUAL_STUDIO_BAT_FILE],
[
  # VS2017 provides the option to install previous minor versions of the MSVC
  # toolsets. It is not possible to directly download earlier minor versions of
  # VS2017 and in order to build with a previous minor compiler toolset version,
  # it is now possible to compile with earlier minor versions by passing
  # -vcvars_ver=<toolset_version> argument to vcvarsall.bat.
  AC_ARG_WITH(msvc-toolset-version, [AS_HELP_STRING([--with-msvc-toolset-version],
      [specific MSVC toolset version to use, passed as -vcvars_ver argument to
       pass to vcvarsall.bat (Windows only)])])

  TARGET_CPU="$1"
  VS_VERSION="$2"
  eval VS_COMNTOOLS_VAR="\${VS_ENVVAR_${VS_VERSION}}"
  eval VS_COMNTOOLS="\$${VS_COMNTOOLS_VAR}"
  eval VS_INSTALL_DIR="\${VS_VS_INSTALLDIR_${VS_VERSION}}"
  eval VS_EDITIONS="\${VS_EDITIONS_${VS_VERSION}}"
  eval SDK_INSTALL_DIR="\${VS_SDK_INSTALLDIR_${VS_VERSION}}"
  eval VS_ENV_ARGS="\${VS_ENV_ARGS_${VS_VERSION}}"
  eval VS_TOOLSET_SUPPORTED="\${VS_TOOLSET_SUPPORTED_${VS_VERSION}}"

  VS_ENV_CMD=""

  # When using --with-tools-dir, assume it points to the correct and default
  # version of Visual Studio or that --with-toolchain-version was also set.
  if test "x$with_tools_dir" != x; then
    TOOLCHAIN_CHECK_POSSIBLE_VISUAL_STUDIO_ROOT([$TARGET_CPU], [$VS_VERSION],
        [$with_tools_dir/../..], [--with-tools-dir])
    TOOLCHAIN_CHECK_POSSIBLE_VISUAL_STUDIO_ROOT([$TARGET_CPU], [$VS_VERSION],
        [$with_tools_dir/../../..], [--with-tools-dir])
    if test "x$VS_ENV_CMD" = x; then
      # Having specified an argument which is incorrect will produce an instant failure;
      # we should not go on looking
      AC_MSG_NOTICE([The path given by --with-tools-dir does not contain a valid])
      AC_MSG_NOTICE([Visual Studio installation. Please point to the VC/bin or VC/bin/amd64])
      AC_MSG_NOTICE([directory within the Visual Studio installation])
      AC_MSG_ERROR([Cannot locate a valid Visual Studio installation])
    fi
  fi

  if test "x$VS_COMNTOOLS" != x; then
    TOOLCHAIN_CHECK_POSSIBLE_VISUAL_STUDIO_ROOT([$TARGET_CPU], [$VS_VERSION],
        [$VS_COMNTOOLS/../..], [$VS_COMNTOOLS_VAR variable])
  fi
  if test "x$PROGRAMFILES" != x; then
    TOOLCHAIN_CHECK_POSSIBLE_VISUAL_STUDIO_ROOT([$TARGET_CPU], [$VS_VERSION],
        [$PROGRAMFILES/$VS_INSTALL_DIR], [well-known name])
  fi
  # Work around the insanely named ProgramFiles(x86) env variable
  PROGRAMFILES_X86="`env | $SED -n 's/^ProgramFiles(x86)=//p'`"
  if test "x$PROGRAMFILES_X86" != x; then
    TOOLCHAIN_CHECK_POSSIBLE_VISUAL_STUDIO_ROOT([$TARGET_CPU], [$VS_VERSION],
        [$PROGRAMFILES_X86/$VS_INSTALL_DIR], [well-known name])
  fi
  TOOLCHAIN_CHECK_POSSIBLE_VISUAL_STUDIO_ROOT([$TARGET_CPU], [$VS_VERSION],
      [c:/program files/$VS_INSTALL_DIR], [well-known name])
  TOOLCHAIN_CHECK_POSSIBLE_VISUAL_STUDIO_ROOT([$TARGET_CPU], [$VS_VERSION],
      [c:/program files (x86)/$VS_INSTALL_DIR], [well-known name])
  if test "x$SDK_INSTALL_DIR" != x; then
    if test "x$ProgramW6432" != x; then
      TOOLCHAIN_CHECK_POSSIBLE_WIN_SDK_ROOT([$TARGET_CPU], [$VS_VERSION],
          [$ProgramW6432/$SDK_INSTALL_DIR], [well-known name])
    fi
    if test "x$PROGRAMW6432" != x; then
      TOOLCHAIN_CHECK_POSSIBLE_WIN_SDK_ROOT([$TARGET_CPU], [$VS_VERSION],
          [$PROGRAMW6432/$SDK_INSTALL_DIR], [well-known name])
    fi
    if test "x$PROGRAMFILES" != x; then
      TOOLCHAIN_CHECK_POSSIBLE_WIN_SDK_ROOT([$TARGET_CPU], [$VS_VERSION],
          [$PROGRAMFILES/$SDK_INSTALL_DIR], [well-known name])
    fi
    TOOLCHAIN_CHECK_POSSIBLE_WIN_SDK_ROOT([$TARGET_CPU], [$VS_VERSION],
        [c:/program files/$SDK_INSTALL_DIR], [well-known name])
    TOOLCHAIN_CHECK_POSSIBLE_WIN_SDK_ROOT([$TARGET_CPU], [$VS_VERSION],
        [c:/program files (x86)/$SDK_INSTALL_DIR], [well-known name])
  fi

  VCVARS_VER=auto
  if test "x$VS_TOOLSET_SUPPORTED" != x; then
    if test "x$with_msvc_toolset_version" != x; then
      VCVARS_VER="$with_msvc_toolset_version"
    fi
  fi
])

################################################################################

AC_DEFUN([TOOLCHAIN_FIND_VISUAL_STUDIO],
[
  AC_ARG_WITH(toolchain-version, [AS_HELP_STRING([--with-toolchain-version],
      [the version of the toolchain to look for, use '--help' to show possible values @<:@platform dependent@:>@])])

  if test "x$with_toolchain_version" = xlist; then
    # List all toolchains
    AC_MSG_NOTICE([The following toolchain versions are valid on this platform:])
    for version in $VALID_VS_VERSIONS; do
      eval VS_DESCRIPTION=\${VS_DESCRIPTION_$version}
      $PRINTF "  %-10s  %s\n" $version "$VS_DESCRIPTION"
    done

    exit 0
  elif test "x$DEVKIT_VS_VERSION" != x; then
    VS_VERSION=$DEVKIT_VS_VERSION
    TOOLCHAIN_VERSION=$VS_VERSION
    # If the devkit has a name, use that as description
    VS_DESCRIPTION="$DEVKIT_NAME"
    if test "x$VS_DESCRIPTION" = x; then
      eval VS_DESCRIPTION="\${VS_DESCRIPTION_${VS_VERSION}}"
    fi
    eval VS_VERSION_INTERNAL="\${VS_VERSION_INTERNAL_${VS_VERSION}}"
    eval MSVCR_NAME="\${VS_MSVCR_${VS_VERSION}}"
    eval VCRUNTIME_1_NAME="\${VS_VCRUNTIME_1_${VS_VERSION}}"
    eval MSVCP_NAME="\${VS_MSVCP_${VS_VERSION}}"
    eval USE_UCRT="\${VS_USE_UCRT_${VS_VERSION}}"
    eval VS_SUPPORTED="\${VS_SUPPORTED_${VS_VERSION}}"
    eval PLATFORM_TOOLSET="\${VS_VS_PLATFORM_NAME_${VS_VERSION}}"

    # For historical reasons, paths are separated by ; in devkit.info
    VS_INCLUDE=${DEVKIT_VS_INCLUDE//;/:}
    VS_LIB=${DEVKIT_VS_LIB//;/:}

    AC_MSG_NOTICE([Found devkit $VS_DESCRIPTION])

  elif test "x$with_toolchain_version" != x; then
    # User override; check that it is valid
    if test "x${VALID_VS_VERSIONS/$with_toolchain_version/}" = "x${VALID_VS_VERSIONS}"; then
      AC_MSG_NOTICE([Visual Studio version $with_toolchain_version is not valid.])
      AC_MSG_NOTICE([Valid Visual Studio versions: $VALID_VS_VERSIONS.])
      AC_MSG_ERROR([Cannot continue.])
    fi
    VS_VERSIONS_PROBE_LIST="$with_toolchain_version"
  else
    # No flag given, use default
    VS_VERSIONS_PROBE_LIST="$VALID_VS_VERSIONS"
  fi

  for VS_VERSION in $VS_VERSIONS_PROBE_LIST; do
    TOOLCHAIN_FIND_VISUAL_STUDIO_BAT_FILE($OPENJDK_TARGET_CPU, [$VS_VERSION])
    if test "x$VS_ENV_CMD" != x; then
      TOOLCHAIN_VERSION=$VS_VERSION
      eval VS_DESCRIPTION="\${VS_DESCRIPTION_${VS_VERSION}}"
      eval VS_VERSION_INTERNAL="\${VS_VERSION_INTERNAL_${VS_VERSION}}"
      eval MSVCR_NAME="\${VS_MSVCR_${VS_VERSION}}"
      eval VCRUNTIME_1_NAME="\${VS_VCRUNTIME_1_${VS_VERSION}}"
      eval MSVCP_NAME="\${VS_MSVCP_${VS_VERSION}}"
      eval USE_UCRT="\${VS_USE_UCRT_${VS_VERSION}}"
      eval VS_SUPPORTED="\${VS_SUPPORTED_${VS_VERSION}}"
      # The rest of the variables are already evaled while probing
      AC_MSG_NOTICE([Found $VS_DESCRIPTION])
      break
    fi
  done

  TOOLCHAIN_DESCRIPTION="$VS_DESCRIPTION"
  if test "x$VS_SUPPORTED" = "xfalse"; then
    UNSUPPORTED_TOOLCHAIN_VERSION=yes
  fi
])

AC_DEFUN([TOOLCHAIN_EXTRACT_VISUAL_STUDIO_ENV],
[
  TARGET_CPU=$1

  AC_MSG_NOTICE([Trying to extract Visual Studio environment variables for $TARGET_CPU])
  AC_MSG_NOTICE([using $VS_ENV_CMD $VS_ENV_ARGS])

  VS_ENV_TMP_DIR="$CONFIGURESUPPORT_OUTPUTDIR/vs-env-$TARGET_CPU"
  $MKDIR -p $VS_ENV_TMP_DIR

  # Cannot use the VS10 setup script directly (since it only updates the DOS subshell environment).
  # Instead create a shell script which will set the relevant variables when run.

  OLDPATH="$PATH"
  # Make sure we only capture additions to PATH needed by VS.
  # Clear out path, but need system dir present for vsvars cmd file to be able to run
  export PATH=$WINENV_PREFIX/c/windows/system32
  # The "| cat" is to stop SetEnv.Cmd to mess with system colors on some systems
  # We can't pass -vcvars_ver=$VCVARS_VER here because cmd.exe eats all '='
  # in bat file arguments. :-(
  $FIXPATH $CMD /c "$TOPDIR/make/scripts/extract-vs-env.cmd" "$VS_ENV_CMD" \
      "$VS_ENV_TMP_DIR/set-vs-env.sh" $VCVARS_VER $VS_ENV_ARGS \
      > $VS_ENV_TMP_DIR/extract-vs-env.log | $CAT 2>&1
  PATH="$OLDPATH"

  if test ! -s $VS_ENV_TMP_DIR/set-vs-env.sh; then
    AC_MSG_NOTICE([Could not succesfully extract the environment variables needed for the VS setup.])
    AC_MSG_NOTICE([Try setting --with-tools-dir to the VC/bin directory within the VS installation.])
    AC_MSG_NOTICE([To analyze the problem, see extract-vs-env.log and extract-vs-env.bat in])
    AC_MSG_NOTICE([$VS_ENV_TMP_DIR.])
    AC_MSG_ERROR([Cannot continue])
  fi

  # Remove windows line endings
  $SED -i -e 's|\r||g' $VS_ENV_TMP_DIR/set-vs-env.sh

  # Now set all paths and other env variables by executing the generated
  # shell script. This will allow the rest of the configure script to find
  # and run the compiler in the proper way.
  AC_MSG_NOTICE([Setting extracted environment variables for $TARGET_CPU])
  . $VS_ENV_TMP_DIR/set-vs-env.sh

  # Extract only what VS_ENV_CMD added to the PATH
  VS_PATH=${PATH_AFTER/"$PATH_BEFORE"}
  VS_PATH=${VS_PATH//::/:}

  # Remove any paths containing # (typically F#) as that messes up make. This
  # is needed if visual studio was installed with F# support.
  [ VS_PATH=`$ECHO "$VS_PATH" | $SED 's/[^:#]*#[^:]*://g'` ]

  # Sometimes case is off
  if test -z "$WINDOWSSDKDIR"; then
    WINDOWSSDKDIR="$WindowsSdkDir"
  fi
  # Now we have VS_PATH, VS_INCLUDE, VS_LIB. For further checking, we
  # also define VCINSTALLDIR and WINDOWSSDKDIR. All are in
  # unix style.
])

################################################################################
# Check if the VS env variables were setup prior to running configure.
# If not, then find vcvarsall.bat and run it automatically, and integrate
# the set env variables into the spec file.
AC_DEFUN([TOOLCHAIN_SETUP_VISUAL_STUDIO_ENV],
[
  # Locate the vsvars bat file and save it as VS_ENV_CMD
  TOOLCHAIN_FIND_VISUAL_STUDIO

  # If we have a devkit, we don't need to run VS_ENV_CMD
  if test "x$DEVKIT_VS_VERSION" = x; then
    if test "x$VS_ENV_CMD" != x; then
      # We have found a Visual Studio environment on disk, let's extract variables
      # from the vsvars bat file into shell variables in the configure script.
      TOOLCHAIN_EXTRACT_VISUAL_STUDIO_ENV($OPENJDK_TARGET_CPU)

      # Now we have VS_PATH, VS_INCLUDE, VS_LIB. For further checking, we
      # also define VCINSTALLDIR and WINDOWSSDKDIR. All are in
      # unix style.
    else
      # We did not find a vsvars bat file.
      AC_MSG_ERROR([Cannot locate a valid Visual Studio installation])
    fi
  fi

  # At this point, we should have correct variables in the environment
  AC_MSG_CHECKING([that Visual Studio variables have been correctly extracted])

  if test "x$VCINSTALLDIR" != x || test "x$WINDOWSSDKDIR" != x \
      || test "x$DEVKIT_NAME" != x; then
    if test "x$VS_INCLUDE" = x || test "x$VS_LIB" = x; then
      AC_MSG_RESULT([no; Visual Studio present but broken])
      AC_MSG_ERROR([Your VC command prompt seems broken, INCLUDE and/or LIB is missing.])
    else
      AC_MSG_RESULT([ok])

      # Turn VS_PATH into TOOLCHAIN_PATH
      TOOLCHAIN_PATH="$TOOLCHAIN_PATH:$VS_PATH"

      # Convert VS_INCLUDE and VS_LIB into sysroot flags
      TOOLCHAIN_SETUP_VISUAL_STUDIO_SYSROOT_FLAGS
    fi
  else
    AC_MSG_RESULT([not found])

    if test "x$VS_ENV_CMD" = x; then
      AC_MSG_NOTICE([Cannot locate a valid Visual Studio or Windows SDK installation on disk])
    else
      AC_MSG_NOTICE([Running the extraction script failed])
    fi
    AC_MSG_NOTICE([Try setting --with-tools-dir to the VC/bin directory within the VS installation.])
    AC_MSG_NOTICE([To analyze the problem, see extract-vs-env.log and extract-vs-env.bat in])
    AC_MSG_NOTICE([$VS_ENV_TMP_DIR.])
    AC_MSG_ERROR([Cannot continue])
  fi
])

AC_DEFUN([TOOLCHAIN_CHECK_POSSIBLE_MSVC_DLL],
[
  DLL_NAME="$1"
  POSSIBLE_MSVC_DLL="$2"
  METHOD="$3"
  if test -n "$POSSIBLE_MSVC_DLL" -a -e "$POSSIBLE_MSVC_DLL"; then
    AC_MSG_NOTICE([Found $DLL_NAME at $POSSIBLE_MSVC_DLL using $METHOD])

    # Need to check if the found msvcr is correct architecture
    AC_MSG_CHECKING([found $DLL_NAME architecture])
    MSVC_DLL_FILETYPE=`$FILE -b "$POSSIBLE_MSVC_DLL"`
    if test "x$OPENJDK_TARGET_CPU" = xx86; then
      CORRECT_MSVCR_ARCH=386
    elif test "x$OPENJDK_TARGET_CPU" = xx86_64; then
      CORRECT_MSVCR_ARCH=x86-64
    elif test "x$OPENJDK_TARGET_CPU" = xaarch64; then
      # The cygwin 'file' command only returns "PE32+ executable (DLL) (console), for MS Windows",
      # without specifying which architecture it is for specifically. This has been fixed upstream.
      # https://github.com/file/file/commit/b849b1af098ddd530094bf779b58431395db2e10#diff-ff2eced09e6860de75057dd731d092aeR142
      CORRECT_MSVCR_ARCH="PE32+ executable"
    fi
    if $ECHO "$MSVC_DLL_FILETYPE" | $GREP "$CORRECT_MSVCR_ARCH" 2>&1 > /dev/null; then
      AC_MSG_RESULT([ok])
      MSVC_DLL="$POSSIBLE_MSVC_DLL"
      AC_MSG_CHECKING([for $DLL_NAME])
      AC_MSG_RESULT([$MSVC_DLL])
    else
      AC_MSG_RESULT([incorrect, ignoring])
      AC_MSG_NOTICE([The file type of the located $DLL_NAME is $MSVC_DLL_FILETYPE])
    fi
  fi
])

AC_DEFUN([TOOLCHAIN_SETUP_MSVC_DLL],
[
  DLL_NAME="$1"
  MSVC_DLL=

  if test "x$OPENJDK_TARGET_CPU" = xx86; then
    vs_target_cpu=x86
  elif test "x$OPENJDK_TARGET_CPU" = xx86_64; then
    vs_target_cpu=x64
  elif test "x$OPENJDK_TARGET_CPU" = xaarch64; then
    vs_target_cpu=arm64
  fi

  if test "x$MSVC_DLL" = x; then
    if test "x$VCINSTALLDIR" != x; then
      if test "$VS_VERSION" -lt 2017; then
        # Probe: Using well-known location from Visual Studio 12.0 and older
        POSSIBLE_MSVC_DLL="$VCINSTALLDIR/redist/$vs_target_cpu/microsoft.vc${VS_VERSION_INTERNAL}.crt/$DLL_NAME"
      else
        # Probe: Using well-known location from VS 2017 and VS 2019
        POSSIBLE_MSVC_DLL="`ls $VCToolsRedistDir/$vs_target_cpu/microsoft.vc${VS_VERSION_INTERNAL}.crt/$DLL_NAME 2> /dev/null`"
      fi
      # In case any of the above finds more than one file, loop over them.
      for possible_msvc_dll in $POSSIBLE_MSVC_DLL; do
        TOOLCHAIN_CHECK_POSSIBLE_MSVC_DLL([$DLL_NAME], [$possible_msvc_dll],
            [well-known location in VCINSTALLDIR])
      done
    fi
  fi

  if test "x$MSVC_DLL" = x; then
    # Probe: Check in the Boot JDK directory.
    POSSIBLE_MSVC_DLL="$BOOT_JDK/bin/$DLL_NAME"
    TOOLCHAIN_CHECK_POSSIBLE_MSVC_DLL([$DLL_NAME], [$POSSIBLE_MSVC_DLL],
        [well-known location in Boot JDK])
  fi

  if test "x$MSVC_DLL" = x; then
    # Probe: Look in the Windows system32 directory
    WIN_SYSTEMROOT="$SYSTEMROOT"
    UTIL_FIXUP_PATH(WIN_SYSTEMROOT, NOFAIL)
    if test "x$WIN_SYSTEMROOT" != x; then
      POSSIBLE_MSVC_DLL="$WIN_SYSTEMROOT/system32/$DLL_NAME"
      TOOLCHAIN_CHECK_POSSIBLE_MSVC_DLL([$DLL_NAME], [$POSSIBLE_MSVC_DLL],
          [well-known location in SYSTEMROOT])
    fi
  fi

  if test "x$MSVC_DLL" = x; then
    # Probe: If Visual Studio Express is installed, there is usually one with the debugger
    if test "x$VS100COMNTOOLS" != x; then
      WIN_VS_TOOLS_DIR="$VS100COMNTOOLS/.."
      UTIL_FIXUP_PATH(WIN_VS_TOOLS_DIR, NOFAIL)
      if test "x$WIN_VS_TOOLS_DIR" != x; then
        POSSIBLE_MSVC_DLL=`$FIND "$WIN_VS_TOOLS_DIR" -name $DLL_NAME \
        | $GREP -i /$vs_target_cpu/ | $HEAD --lines 1`
        TOOLCHAIN_CHECK_POSSIBLE_MSVC_DLL([$DLL_NAME], [$POSSIBLE_MSVC_DLL],
            [search of VS100COMNTOOLS])
      fi
    fi
  fi

  if test "x$MSVC_DLL" = x; then
    # Probe: Search wildly in the VCINSTALLDIR. We've probably lost by now.
    # (This was the original behaviour; kept since it might turn something up)
    if test "x$VCINSTALLDIR" != x; then
      if test "x$OPENJDK_TARGET_CPU" = xx86; then
        POSSIBLE_MSVC_DLL=`$FIND "$VCINSTALLDIR" -name $DLL_NAME \
        | $GREP x86 | $GREP -v ia64 | $GREP -v x64 | $GREP -v arm64 | $HEAD --lines 1`
        if test "x$POSSIBLE_MSVC_DLL" = x; then
          # We're grasping at straws now...
          POSSIBLE_MSVC_DLL=`$FIND "$VCINSTALLDIR" -name $DLL_NAME \
          | $HEAD --lines 1`
        fi
      else
        POSSIBLE_MSVC_DLL=`$FIND "$VCINSTALLDIR" -name $DLL_NAME \
        | $GREP x64 | $HEAD --lines 1`
      fi

      TOOLCHAIN_CHECK_POSSIBLE_MSVC_DLL([$DLL_NAME], [$POSSIBLE_MSVC_DLL],
          [search of VCINSTALLDIR])
    fi
  fi

  if test "x$MSVC_DLL" = x; then
    AC_MSG_CHECKING([for $DLL_NAME])
    AC_MSG_RESULT([no])
    AC_MSG_ERROR([Could not find $DLL_NAME. Please specify using --with-msvcr-dll.])
  fi
])

AC_DEFUN([TOOLCHAIN_SETUP_VS_RUNTIME_DLLS],
[
  AC_ARG_WITH(msvcr-dll, [AS_HELP_STRING([--with-msvcr-dll],
      [path to microsoft C runtime dll (msvcr*.dll) (Windows only) @<:@probed@:>@])])

  if test "x$with_msvcr_dll" != x; then
    # If given explicitly by user, do not probe. If not present, fail directly.
    TOOLCHAIN_CHECK_POSSIBLE_MSVC_DLL($MSVCR_NAME, [$with_msvcr_dll], [--with-msvcr-dll])
    if test "x$MSVC_DLL" = x; then
      AC_MSG_ERROR([Could not find a proper $MSVCR_NAME as specified by --with-msvcr-dll])
    fi
    MSVCR_DLL="$MSVC_DLL"
  elif test "x$DEVKIT_MSVCR_DLL" != x; then
    TOOLCHAIN_CHECK_POSSIBLE_MSVC_DLL($MSVCR_NAME, [$DEVKIT_MSVCR_DLL], [devkit])
    if test "x$MSVC_DLL" = x; then
      AC_MSG_ERROR([Could not find a proper $MSVCR_NAME as specified by devkit])
    fi
    MSVCR_DLL="$MSVC_DLL"
  else
    TOOLCHAIN_SETUP_MSVC_DLL([${MSVCR_NAME}])
    MSVCR_DLL="$MSVC_DLL"
  fi
  AC_SUBST(MSVCR_DLL)

  AC_ARG_WITH(msvcp-dll, [AS_HELP_STRING([--with-msvcp-dll],
      [path to microsoft C++ runtime dll (msvcp*.dll) (Windows only) @<:@probed@:>@])])

  if test "x$MSVCP_NAME" != "x"; then
    if test "x$with_msvcp_dll" != x; then
      # If given explicitly by user, do not probe. If not present, fail directly.
      TOOLCHAIN_CHECK_POSSIBLE_MSVC_DLL($MSVCP_NAME, [$with_msvcp_dll], [--with-msvcp-dll])
      if test "x$MSVC_DLL" = x; then
        AC_MSG_ERROR([Could not find a proper $MSVCP_NAME as specified by --with-msvcp-dll])
      fi
      MSVCP_DLL="$MSVC_DLL"
    elif test "x$DEVKIT_MSVCP_DLL" != x; then
      TOOLCHAIN_CHECK_POSSIBLE_MSVC_DLL($MSVCP_NAME, [$DEVKIT_MSVCP_DLL], [devkit])
      if test "x$MSVC_DLL" = x; then
        AC_MSG_ERROR([Could not find a proper $MSVCP_NAME as specified by devkit])
      fi
      MSVCP_DLL="$MSVC_DLL"
    else
      TOOLCHAIN_SETUP_MSVC_DLL([${MSVCP_NAME}])
      MSVCP_DLL="$MSVC_DLL"
    fi
    AC_SUBST(MSVCP_DLL)
  fi

  AC_ARG_WITH(vcruntime-1-dll, [AS_HELP_STRING([--with-vcruntime-1-dll],
      [path to microsoft C++ runtime dll (vcruntime*_1.dll) (Windows x64 only) @<:@probed@:>@])])

  if test "x$VCRUNTIME_1_NAME" != "x" && test "x$OPENJDK_TARGET_CPU" = xx86_64; then
    if test "x$with_vcruntime_1_dll" != x; then
      # If given explicitly by user, do not probe. If not present, fail directly.
      TOOLCHAIN_CHECK_POSSIBLE_MSVC_DLL($VCRUNTIME_1_NAME, [$with_vcruntime_1_dll],
          [--with-vcruntime-1-dll])
      if test "x$MSVC_DLL" = x; then
        AC_MSG_ERROR([Could not find a proper $VCRUNTIME_1_NAME as specified by --with-vcruntime-1-dll])
      fi
      VCRUNTIME_1_DLL="$MSVC_DLL"
    elif test "x$DEVKIT_VCRUNTIME_1_DLL" != x; then
      TOOLCHAIN_CHECK_POSSIBLE_MSVC_DLL($VCRUNTIME_1_NAME, [$DEVKIT_VCRUNTIME_1_DLL], [devkit])
      if test "x$MSVC_DLL" = x; then
        AC_MSG_ERROR([Could not find a proper $VCRUNTIME_1_NAME as specified by devkit])
      fi
      VCRUNTIME_1_DLL="$MSVC_DLL"
    else
      TOOLCHAIN_SETUP_MSVC_DLL([${VCRUNTIME_1_NAME}])
      VCRUNTIME_1_DLL="$MSVC_DLL"
    fi
  fi
  AC_SUBST(VCRUNTIME_1_DLL)

  AC_ARG_WITH(ucrt-dll-dir, [AS_HELP_STRING([--with-ucrt-dll-dir],
      [path to Microsoft Windows Kit UCRT DLL dir (Windows only) @<:@probed@:>@])])

  if test "x$USE_UCRT" = "xtrue" && test "x$OPENJDK_TARGET_CPU" != xaarch64; then
    AC_MSG_CHECKING([for UCRT DLL dir])
    if test "x$with_ucrt_dll_dir" != x; then
      if test -z "$(ls -d "$with_ucrt_dll_dir/"*.dll 2> /dev/null)"; then
        AC_MSG_RESULT([no])
        AC_MSG_ERROR([Could not find any dlls in $with_ucrt_dll_dir])
      else
        AC_MSG_RESULT([$with_ucrt_dll_dir])
        UCRT_DLL_DIR="$with_ucrt_dll_dir"
        UTIL_FIXUP_PATH([UCRT_DLL_DIR])
      fi
    elif test "x$DEVKIT_UCRT_DLL_DIR" != "x"; then
      UCRT_DLL_DIR="$DEVKIT_UCRT_DLL_DIR"
      AC_MSG_RESULT($UCRT_DLL_DIR)
    else
      dll_subdir=$OPENJDK_TARGET_CPU
      if test "x$dll_subdir" = "xaarch64"; then
        dll_subdir="arm64"
      elif test "x$dll_subdir" = "xx86_64"; then
        dll_subdir="x64"
      fi
      UCRT_DLL_DIR="$WINDOWSSDKDIR/redist/ucrt/dlls/$dll_subdir"
      if test -z "$(ls -d "$UCRT_DLL_DIR/"*.dll 2> /dev/null)"; then
        # Try with version subdir
        UCRT_DLL_DIR="`ls -d $WINDOWSSDKDIR/redist/*/ucrt/dlls/$dll_subdir \
            2> /dev/null | $SORT -d | $HEAD -n1`"
        if test -z "$UCRT_DLL_DIR" \
            || test -z "$(ls -d "$UCRT_DLL_DIR/"*.dll 2> /dev/null)"; then
          AC_MSG_RESULT([no])
          AC_MSG_ERROR([Could not find any dlls in $UCRT_DLL_DIR])
        else
          AC_MSG_RESULT($UCRT_DLL_DIR)
        fi
      else
        AC_MSG_RESULT($UCRT_DLL_DIR)
      fi
    fi
  else
    UCRT_DLL_DIR=
  fi
  AC_SUBST(UCRT_DLL_DIR)
])

# Setup the sysroot flags and add them to global CFLAGS and LDFLAGS so
# that configure can use them while detecting compilers.
# TOOLCHAIN_TYPE is available here.
# Param 1 - Optional prefix to SYSROOT variables. (e.g BUILD_)
# Param 2 - Optional prefix to VS variables. (e.g BUILD_)
AC_DEFUN([TOOLCHAIN_SETUP_VISUAL_STUDIO_SYSROOT_FLAGS],
[
  OLDIFS="$IFS"
  IFS=":"

  # Convert VS_INCLUDE into SYSROOT_CFLAGS
  for ipath in [$]$2VS_INCLUDE; do
    $1SYSROOT_CFLAGS="[$]$1SYSROOT_CFLAGS -I$ipath"
  done

  # Convert VS_LIB into SYSROOT_LDFLAGS
  for libpath in [$]$2VS_LIB; do
    $1SYSROOT_LDFLAGS="[$]$1SYSROOT_LDFLAGS -libpath:$libpath"
  done

  IFS="$OLDIFS"

  AC_SUBST($1SYSROOT_CFLAGS)
  AC_SUBST($1SYSROOT_LDFLAGS)
])
