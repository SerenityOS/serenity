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

AC_DEFUN_ONCE([HELP_SETUP_DEPENDENCY_HELP],
[
  UTIL_LOOKUP_PROGS(PKGHANDLER, zypper apt-get yum brew port pkgutil pkgadd pacman)
])

AC_DEFUN([HELP_MSG_MISSING_DEPENDENCY],
[
  # Print a helpful message on how to acquire the necessary build dependency.
  # $1 is the help tag: cups, alsa etc
  MISSING_DEPENDENCY=$1

  if test "x$MISSING_DEPENDENCY" = "xopenjdk"; then
    HELP_MSG="OpenJDK distributions are available at http://jdk.java.net/."
  elif test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.cygwin"; then
    cygwin_help $MISSING_DEPENDENCY
  else
    PKGHANDLER_COMMAND=

    case $PKGHANDLER in
      apt-get)
        apt_help     $MISSING_DEPENDENCY ;;
      yum)
        yum_help     $MISSING_DEPENDENCY ;;
      brew)
        brew_help    $MISSING_DEPENDENCY ;;
      port)
        port_help    $MISSING_DEPENDENCY ;;
      pkgutil)
        pkgutil_help $MISSING_DEPENDENCY ;;
      pkgadd)
        pkgadd_help  $MISSING_DEPENDENCY ;;
      zypper)
        zypper_help  $MISSING_DEPENDENCY ;;
      pacman)
        pacman_help  $MISSING_DEPENDENCY ;;
    esac

    if test "x$PKGHANDLER_COMMAND" != x; then
      HELP_MSG="You might be able to fix this by running '$PKGHANDLER_COMMAND'."
    fi
  fi
])

cygwin_help() {
  case $1 in
    unzip)
      PKGHANDLER_COMMAND="( cd <location of cygwin setup.exe> && cmd /c setup -q -P unzip )"
      HELP_MSG="You might be able to fix this by running '$PKGHANDLER_COMMAND'."
      ;;
    zip)
      PKGHANDLER_COMMAND="( cd <location of cygwin setup.exe> && cmd /c setup -q -P zip )"
      HELP_MSG="You might be able to fix this by running '$PKGHANDLER_COMMAND'."
      ;;
    make)
      PKGHANDLER_COMMAND="( cd <location of cygwin setup.exe> && cmd /c setup -q -P make )"
      HELP_MSG="You might be able to fix this by running '$PKGHANDLER_COMMAND'."
      ;;
  esac
}

apt_help() {
  case $1 in
    reduced)
      PKGHANDLER_COMMAND="sudo apt-get install gcc-multilib g++-multilib" ;;
    devkit)
      PKGHANDLER_COMMAND="sudo apt-get install build-essential" ;;
    alsa)
      PKGHANDLER_COMMAND="sudo apt-get install libasound2-dev" ;;
    cups)
      PKGHANDLER_COMMAND="sudo apt-get install libcups2-dev" ;;
    fontconfig)
      PKGHANDLER_COMMAND="sudo apt-get install libfontconfig1-dev" ;;
    freetype)
      PKGHANDLER_COMMAND="sudo apt-get install libfreetype6-dev" ;;
    harfbuzz)
      PKGHANDLER_COMMAND="sudo apt-get install libharfbuzz-dev" ;;
    ffi)
      PKGHANDLER_COMMAND="sudo apt-get install libffi-dev" ;;
    x11)
      PKGHANDLER_COMMAND="sudo apt-get install libx11-dev libxext-dev libxrender-dev libxrandr-dev libxtst-dev libxt-dev" ;;
    ccache)
      PKGHANDLER_COMMAND="sudo apt-get install ccache" ;;
    dtrace)
      PKGHANDLER_COMMAND="sudo apt-get install systemtap-sdt-dev" ;;
  esac
}

zypper_help() {
  case $1 in
    devkit)
      PKGHANDLER_COMMAND="sudo zypper install gcc gcc-c++" ;;
    alsa)
      PKGHANDLER_COMMAND="sudo zypper install alsa-devel" ;;
    cups)
      PKGHANDLER_COMMAND="sudo zypper install cups-devel" ;;
    fontconfig)
      PKGHANDLER_COMMAND="sudo zypper install fontconfig-devel" ;;
    freetype)
      PKGHANDLER_COMMAND="sudo zypper install freetype-devel" ;;
    harfbuzz)
      PKGHANDLER_COMMAND="sudo zypper install harfbuzz-devel" ;;
    x11)
      PKGHANDLER_COMMAND="sudo zypper install libX11-devel libXext-devel libXrender-devel libXrandr-devel libXtst-devel libXt-devel libXi-devel" ;;
    ccache)
      PKGHANDLER_COMMAND="sudo zypper install ccache" ;;
  esac
}

yum_help() {
  case $1 in
    devkit)
      PKGHANDLER_COMMAND="sudo yum groupinstall \"Development Tools\"" ;;
    alsa)
      PKGHANDLER_COMMAND="sudo yum install alsa-lib-devel" ;;
    cups)
      PKGHANDLER_COMMAND="sudo yum install cups-devel" ;;
    fontconfig)
      PKGHANDLER_COMMAND="sudo yum install fontconfig-devel" ;;
    freetype)
      PKGHANDLER_COMMAND="sudo yum install freetype-devel" ;;
    harfbuzz)
      PKGHANDLER_COMMAND="sudo yum install harfbuzz-devel" ;;
    x11)
      PKGHANDLER_COMMAND="sudo yum install libXtst-devel libXt-devel libXrender-devel libXrandr-devel libXi-devel" ;;
    ccache)
      PKGHANDLER_COMMAND="sudo yum install ccache" ;;
  esac
}

brew_help() {
  case $1 in
    freetype)
      PKGHANDLER_COMMAND="brew install freetype" ;;
    ccache)
      PKGHANDLER_COMMAND="brew install ccache" ;;
  esac
}

pacman_help() {
  case $1 in
    unzip)
      PKGHANDLER_COMMAND="sudo pacman -S unzip" ;;
    zip)
      PKGHANDLER_COMMAND="sudo pacman -S zip" ;;
    make)
      PKGHANDLER_COMMAND="sudo pacman -S make" ;;
  esac
}

port_help() {
  PKGHANDLER_COMMAND=""
}

pkgutil_help() {
  PKGHANDLER_COMMAND=""
}

pkgadd_help() {
  PKGHANDLER_COMMAND=""
}

# This function will check if we're called from the "configure" wrapper while
# printing --help. If so, we will print out additional information that can
# only be extracted within the autoconf script, and then exit. This must be
# called at the very beginning in configure.ac.
AC_DEFUN_ONCE([HELP_PRINT_ADDITIONAL_HELP_AND_EXIT],
[
  if test "x$CONFIGURE_PRINT_ADDITIONAL_HELP" != x; then

    # Print available toolchains
    $PRINTF "The following toolchains are valid as arguments to --with-toolchain-type.\n"
    $PRINTF "Which are available to use depends on the build platform.\n"
    for toolchain in $VALID_TOOLCHAINS_all; do
      # Use indirect variable referencing
      toolchain_var_name=TOOLCHAIN_DESCRIPTION_$toolchain
      TOOLCHAIN_DESCRIPTION=${!toolchain_var_name}
      $PRINTF "  %-22s  %s\n" $toolchain "$TOOLCHAIN_DESCRIPTION"
    done
    $PRINTF "\n"

    # Print available JVM features
    $PRINTF "The following JVM features are valid as arguments to --with-jvm-features.\n"
    $PRINTF "Which are available to use depends on the environment and JVM variant.\n"
    m4_foreach(FEATURE, m4_split(jvm_features_valid), [
      # Create an m4 variable containing the description for FEATURE.
      m4_define(FEATURE_DESCRIPTION, [jvm_feature_desc_]m4_translit(FEATURE, -, _))
      $PRINTF "  %-22s  %s\n" FEATURE "FEATURE_DESCRIPTION"
      m4_undefine([FEATURE_DESCRIPTION])
    ])

    # And now exit directly
    exit 0
  fi
])

AC_DEFUN_ONCE([HELP_PRINT_SUMMARY_AND_WARNINGS],
[
  # Finally output some useful information to the user

  printf "\n"
  printf "====================================================\n"
  if test "x$no_create" != "xyes"; then
    if test "x$IS_RECONFIGURE" != "xyes"; then
      printf "A new configuration has been successfully created in\n%s\n" "$OUTPUTDIR"
    else
      printf "The existing configuration has been successfully updated in\n%s\n" "$OUTPUTDIR"
    fi
  else
    if test "x$IS_RECONFIGURE" != "xyes"; then
      printf "A configuration has been successfully checked but not created\n"
    else
      printf "The existing configuration has been successfully checked in\n%s\n" "$OUTPUTDIR"
    fi
  fi
  if test "x$CONFIGURE_COMMAND_LINE" != x; then
    printf "using configure arguments '$CONFIGURE_COMMAND_LINE'.\n"
  else
    printf "using default settings.\n"
  fi

  printf "\n"
  printf "Configuration summary:\n"
  printf "* Name:           $CONF_NAME\n"
  printf "* Debug level:    $DEBUG_LEVEL\n"
  printf "* HS debug level: $HOTSPOT_DEBUG_LEVEL\n"
  printf "* JVM variants:   $JVM_VARIANTS\n"
  printf "* JVM features:   "

  for variant in $JVM_VARIANTS; do
    features_var_name=JVM_FEATURES_$variant
    JVM_FEATURES_FOR_VARIANT=${!features_var_name}
    printf "$variant: \'$JVM_FEATURES_FOR_VARIANT\' "
  done
  printf "\n"

  printf "* OpenJDK target: OS: $OPENJDK_TARGET_OS, CPU architecture: $OPENJDK_TARGET_CPU_ARCH, address length: $OPENJDK_TARGET_CPU_BITS\n"
  printf "* Version string: $VERSION_STRING ($VERSION_SHORT)\n"

  printf "\n"
  printf "Tools summary:\n"
  if test "x$OPENJDK_BUILD_OS" = "xwindows"; then
    printf "* Environment:    %s version %s; windows version %s; prefix \"%s\"; root \"%s\"\n" \
        "$WINENV_VENDOR" "$WINENV_VERSION" "$WINDOWS_VERSION" "$WINENV_PREFIX" "$WINENV_ROOT"
  fi
  printf "* Boot JDK:       $BOOT_JDK_VERSION (at $BOOT_JDK)\n"
  printf "* Toolchain:      $TOOLCHAIN_TYPE ($TOOLCHAIN_DESCRIPTION)\n"
  printf "* C Compiler:     Version $CC_VERSION_NUMBER (at ${CC#"$FIXPATH "})\n"
  printf "* C++ Compiler:   Version $CXX_VERSION_NUMBER (at ${CXX#"$FIXPATH "})\n"

  printf "\n"
  printf "Build performance summary:\n"
  printf "* Cores to use:   $JOBS\n"
  printf "* Memory limit:   $MEMORY_SIZE MB\n"
  if test "x$CCACHE_STATUS" != "x"; then
    printf "* ccache status:  $CCACHE_STATUS\n"
  fi
  printf "\n"

  if test "x$BUILDING_MULTIPLE_JVM_VARIANTS" = "xtrue"; then
    printf "NOTE: You have requested to build more than one version of the JVM, which\n"
    printf "will result in longer build times.\n"
    printf "\n"
  fi

  if test "x$FOUND_ALT_VARIABLES" != "x"; then
    printf "WARNING: You have old-style ALT_ environment variables set.\n"
    printf "These are not respected, and will be ignored. It is recommended\n"
    printf "that you clean your environment. The following variables are set:\n"
    printf "$FOUND_ALT_VARIABLES\n"
    printf "\n"
  fi

  if test "x$OUTPUT_DIR_IS_LOCAL" != "xyes"; then
    printf "WARNING: Your build output directory is not on a local disk.\n"
    printf "This will severely degrade build performance!\n"
    printf "It is recommended that you create an output directory on a local disk,\n"
    printf "and run the configure script again from that directory.\n"
    printf "\n"
  fi

  if test "x$IS_RECONFIGURE" = "xyes" && test "x$no_create" != "xyes"; then
    printf "WARNING: The result of this configuration has overridden an older\n"
    printf "configuration. You *should* run 'make clean' to make sure you get a\n"
    printf "proper build. Failure to do so might result in strange build problems.\n"
    printf "\n"
  fi

  if test "x$IS_RECONFIGURE" != "xyes" && test "x$no_create" = "xyes"; then
    printf "WARNING: The result of this configuration was not saved.\n"
    printf "You should run without '--no-create | -n' to create the configuration.\n"
    printf "\n"
  fi

  if test "x$UNSUPPORTED_TOOLCHAIN_VERSION" = "xyes"; then
    printf "WARNING: The toolchain version used is known to have issues. Please\n"
    printf "consider using a supported version unless you know what you are doing.\n"
    printf "\n"
  fi
])

AC_DEFUN_ONCE([HELP_REPEAT_WARNINGS],
[
  # Locate config.log.
  if test -e "$CONFIGURESUPPORT_OUTPUTDIR/config.log"; then
    CONFIG_LOG_PATH="$CONFIGURESUPPORT_OUTPUTDIR"
  elif test -e "./config.log"; then
    CONFIG_LOG_PATH="."
  fi

  if test -e "$CONFIG_LOG_PATH/config.log"; then
    $GREP '^configure:.*: WARNING:' "$CONFIG_LOG_PATH/config.log" > /dev/null 2>&1
    if test $? -eq 0; then
      printf "The following warnings were produced. Repeated here for convenience:\n"
      # We must quote sed expression (using []) to stop m4 from eating the [].
      $GREP '^configure:.*: WARNING:' "$CONFIG_LOG_PATH/config.log" | $SED -e [ 's/^configure:[0-9]*: //' ]
      printf "\n"
    fi
  fi
])
