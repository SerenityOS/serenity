#
# Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

########################################################################
# This file handles detection of the Boot JDK. The Boot JDK detection
# process has been developed as a response to solve a complex real-world
# problem. Initially, it was simple, but it has grown as platform after
# platform, idiosyncracy after idiosyncracy has been supported.
#
# The basic idea is this:
# 1) You need an acceptable *) JDK to use as a Boot JDK
# 2) There are several ways to locate a JDK, that are mostly platform
#    dependent **)
# 3) You can have multiple JDKs installed
# 4) If possible, configure should try to dig out an acceptable JDK
#    automatically, without having to resort to command-line options
#
# *)  acceptable means e.g. JDK7 for building JDK8, a complete JDK (with
#     javac) and not a JRE, etc.
#
# **) On Windows we typically use a well-known path.
#     On MacOSX we typically use the tool java_home.
#     On Linux we typically find javac in the $PATH, and then follow a
#     chain of symlinks that often ends up in a real JDK.
#
# This leads to the code where we check in different ways to locate a
# JDK, and if one is found, check if it is acceptable. If not, we print
# our reasons for rejecting it (useful when debugging non-working
# configure situations) and continue checking the next one.
########################################################################

# Execute the check given as argument, and verify the result
# If the Boot JDK was previously found, do nothing
# $1 A command line (typically autoconf macro) to execute
AC_DEFUN([BOOTJDK_DO_CHECK],
[
  if test "x$BOOT_JDK_FOUND" = xno; then
    # Now execute the test
    $1

    # If previous step claimed to have found a JDK, check it to see if it seems to be valid.
    if test "x$BOOT_JDK_FOUND" = xmaybe; then
      # Do we have a bin/java?
      if test ! -x "$BOOT_JDK/bin/java" && test ! -x "$BOOT_JDK/bin/java.exe"; then
        AC_MSG_NOTICE([Potential Boot JDK found at $BOOT_JDK did not contain bin/java; ignoring])
        BOOT_JDK_FOUND=no
      else
        # Do we have a bin/javac?
        if test ! -x "$BOOT_JDK/bin/javac" && test ! -x "$BOOT_JDK/bin/javac.exe"; then
          AC_MSG_NOTICE([Potential Boot JDK found at $BOOT_JDK did not contain bin/javac; ignoring])
          AC_MSG_NOTICE([(This might be an JRE instead of an JDK)])
          BOOT_JDK_FOUND=no
        else
          # Oh, this is looking good! We probably have found a proper JDK. Is it the correct version?
          java_to_test="$BOOT_JDK/bin/java"
          UTIL_FIXUP_EXECUTABLE(java_to_test)
          BOOT_JDK_VERSION_OUTPUT=`$java_to_test $USER_BOOT_JDK_OPTIONS -version 2>&1`
          # Additional [] needed to keep m4 from mangling shell constructs.
          [ BOOT_JDK_VERSION=`echo $BOOT_JDK_VERSION_OUTPUT | $AWK '/version "[0-9a-zA-Z\._\-]+"/ {print $ 0; exit;}'` ]

          if [ [[ "$BOOT_JDK_VERSION" =~ "Picked up" ]] ]; then
            AC_MSG_NOTICE([You have _JAVA_OPTIONS or JAVA_TOOL_OPTIONS set. This can mess up the build. Please use --with-boot-jdk-jvmargs instead.])
            AC_MSG_NOTICE([Java reports: "$BOOT_JDK_VERSION".])
            AC_MSG_ERROR([Cannot continue])
          fi
          if [ [[ "$BOOT_JDK_VERSION" =~ "Unrecognized option" ]] ]; then
            AC_MSG_NOTICE([The specified --with-boot-jdk-jvmargs is invalid for the tested java])
            AC_MSG_NOTICE([Error message: "$BOOT_JDK_VERSION".])
            AC_MSG_NOTICE([Please fix arguments, or point to an explicit boot JDK which accept these arguments])
            AC_MSG_ERROR([Cannot continue])
          fi

          # Extra M4 quote needed to protect [] in grep expression.
          [FOUND_CORRECT_VERSION=`$ECHO $BOOT_JDK_VERSION \
              | $EGREP "\"(${DEFAULT_ACCEPTABLE_BOOT_VERSIONS// /|})([\.+-].*)?\""`]

          if test "x$BOOT_JDK_VERSION" = x; then
            AC_MSG_NOTICE([Potential Boot JDK found at $BOOT_JDK is not a working JDK; ignoring])
            AC_MSG_NOTICE([Output from java -version was: $BOOT_JDK_VERSION_OUTPUT])
            BOOT_JDK_FOUND=no
          elif test "x$FOUND_CORRECT_VERSION" = x; then
            AC_MSG_NOTICE([Potential Boot JDK found at $BOOT_JDK is incorrect JDK version ($BOOT_JDK_VERSION); ignoring])
            AC_MSG_NOTICE([(Your Boot JDK version must be one of: $DEFAULT_ACCEPTABLE_BOOT_VERSIONS)])
            BOOT_JDK_FOUND=no
          else
            # We're done! :-)
            BOOT_JDK_FOUND=yes
            UTIL_FIXUP_PATH(BOOT_JDK)
            AC_MSG_CHECKING([for Boot JDK])
            AC_MSG_RESULT([$BOOT_JDK])
            AC_MSG_CHECKING([Boot JDK version])
            BOOT_JDK_VERSION=`$java_to_test $USER_BOOT_JDK_OPTIONS -version 2>&1 | $TR -d '\r'`
            # This is not a no-op; it will portably convert newline to space
            BOOT_JDK_VERSION=`$ECHO $BOOT_JDK_VERSION`
            AC_MSG_RESULT([$BOOT_JDK_VERSION])
          fi # end check jdk version
        fi # end check javac
      fi # end check java
    fi # end check boot jdk found
  fi
])

# Test: Is bootjdk explicitly set by command line arguments?
AC_DEFUN([BOOTJDK_CHECK_ARGUMENTS],
[
  if test "x$with_boot_jdk" != x; then
    if test -d "$with_boot_jdk"; then
      BOOT_JDK=$with_boot_jdk
      BOOT_JDK_FOUND=maybe
    elif test -f "$with_boot_jdk"; then
      case "$with_boot_jdk" in
        *.tar.gz )
            BOOT_JDK_SUPPORT_DIR=$CONFIGURESUPPORT_OUTPUTDIR/boot-jdk
            $RM -rf $BOOT_JDK_SUPPORT_DIR
            $MKDIR -p $BOOT_JDK_SUPPORT_DIR
            $GUNZIP -c $with_boot_jdk | $TAR xf - -C $BOOT_JDK_SUPPORT_DIR

            # Try to find javac to determine BOOT_JDK path
            BOOT_JDK_JAVAC_PATH=`$FIND $BOOT_JDK_SUPPORT_DIR | $GREP "/bin/javac"`
            if test "x$BOOT_JDK_JAVAC_PATH" != x; then
              BOOT_JDK_FOUND=maybe
              BOOT_JDK=$($DIRNAME $($DIRNAME $BOOT_JDK_JAVAC_PATH))
            else
              BOOT_JDK_FOUND=no
            fi
          ;;
        * )
            BOOT_JDK_FOUND=no
          ;;
      esac
    else
      BOOT_JDK_FOUND=no
    fi
    AC_MSG_NOTICE([Found potential Boot JDK using configure arguments])
  fi
])

# Test: Is $JAVA_HOME set?
AC_DEFUN([BOOTJDK_CHECK_JAVA_HOME],
[
  if test "x$JAVA_HOME" != x; then
    JAVA_HOME_PROCESSED="$JAVA_HOME"
    UTIL_FIXUP_PATH(JAVA_HOME_PROCESSED, NOFAIL)
    if test "x$JAVA_HOME_PROCESSED" = x || test ! -d "$JAVA_HOME_PROCESSED"; then
      AC_MSG_NOTICE([Your JAVA_HOME points to a non-existing directory!])
    else
      # Aha, the user has set a JAVA_HOME
      # let us use that as the Boot JDK.
      BOOT_JDK="$JAVA_HOME_PROCESSED"
      BOOT_JDK_FOUND=maybe
      AC_MSG_NOTICE([Found potential Boot JDK using JAVA_HOME])
    fi
  fi
])

# Test: Is there a java or javac in the PATH, which is a symlink to the JDK?
AC_DEFUN([BOOTJDK_CHECK_JAVA_IN_PATH_IS_SYMLINK],
[
  UTIL_LOOKUP_PROGS(JAVAC_CHECK, javac, , NOFIXPATH)
  UTIL_LOOKUP_PROGS(JAVA_CHECK, java, , NOFIXPATH)
  BINARY="$JAVAC_CHECK"
  if test "x$JAVAC_CHECK" = x; then
    BINARY="$JAVA_CHECK"
  fi
  if test "x$BINARY" != x; then
    # So there is a java(c) binary, it might be part of a JDK.
    # Lets find the JDK/JRE directory by following symbolic links.
    # Linux/GNU systems often have links from /usr/bin/java to
    # /etc/alternatives/java to the real JDK binary.
    UTIL_REMOVE_SYMBOLIC_LINKS(BINARY)
    BOOT_JDK=`dirname "$BINARY"`
    BOOT_JDK=`cd "$BOOT_JDK/.."; pwd`
    if test -x "$BOOT_JDK/bin/javac" && test -x "$BOOT_JDK/bin/java"; then
      # Looks like we found ourselves an JDK
      BOOT_JDK_FOUND=maybe
      AC_MSG_NOTICE([Found potential Boot JDK using java(c) in PATH])
    fi
  fi
])

# Test: Is there a /usr/libexec/java_home? (Typically on MacOSX)
# $1: Argument to the java_home binary (optional)
AC_DEFUN([BOOTJDK_CHECK_LIBEXEC_JAVA_HOME],
[
  if test -x /usr/libexec/java_home; then
    BOOT_JDK=`/usr/libexec/java_home $1`
    BOOT_JDK_FOUND=maybe
    AC_MSG_NOTICE([Found potential Boot JDK using /usr/libexec/java_home $1])
  fi
])

# Test: On MacOS X, can we find a boot jdk using /usr/libexec/java_home?
AC_DEFUN([BOOTJDK_CHECK_MACOSX_JAVA_LOCATOR],
[
  if test "x$OPENJDK_TARGET_OS" = xmacosx; then
    # First check at user selected default
    BOOTJDK_DO_CHECK([BOOTJDK_CHECK_LIBEXEC_JAVA_HOME()])
    # If that did not work out (e.g. too old), try explicit versions instead
    for ver in $DEFAULT_ACCEPTABLE_BOOT_VERSIONS ; do
      BOOTJDK_DO_CHECK([BOOTJDK_CHECK_LIBEXEC_JAVA_HOME([-v $ver])])
    done
  fi
])

# Look for a jdk in the given path. If there are multiple, try to select the newest.
# If found, set BOOT_JDK and BOOT_JDK_FOUND.
# $1 = Path to directory containing jdk installations.
# $2 = String to append to the found JDK directory to get the proper JDK home
AC_DEFUN([BOOTJDK_FIND_BEST_JDK_IN_DIRECTORY],
[
  BOOT_JDK_PREFIX="$1"
  BOOT_JDK_SUFFIX="$2"
  ALL_JDKS_FOUND=`$LS "$BOOT_JDK_PREFIX" 2> /dev/null | $SORT -r`
  if test "x$ALL_JDKS_FOUND" != x; then
    for JDK_TO_TRY in $ALL_JDKS_FOUND ; do
      BOOTJDK_DO_CHECK([
        BOOT_JDK="${BOOT_JDK_PREFIX}/${JDK_TO_TRY}${BOOT_JDK_SUFFIX}"
        if test -d "$BOOT_JDK"; then
          BOOT_JDK_FOUND=maybe
          AC_MSG_NOTICE([Found potential Boot JDK using well-known locations (in $BOOT_JDK_PREFIX/$JDK_TO_TRY)])
        fi
      ])
    done
  fi
])

# Call BOOTJDK_FIND_BEST_JDK_IN_DIRECTORY, but use the given
# environmental variable as base for where to look.
# $1 Name of an environmal variable, assumed to point to the Program Files directory.
AC_DEFUN([BOOTJDK_FIND_BEST_JDK_IN_WINDOWS_VIRTUAL_DIRECTORY],
[
  if test "x[$]$1" != x; then
    VIRTUAL_DIR="[$]$1/Java"
    UTIL_FIXUP_PATH(VIRTUAL_DIR, NOFAIL)
    if test "x$VIRTUAL_DIR" != x; then
      BOOTJDK_FIND_BEST_JDK_IN_DIRECTORY($VIRTUAL_DIR)
    fi
  fi
])

# Test: Is there a JDK installed in default, well-known locations?
AC_DEFUN([BOOTJDK_CHECK_WELL_KNOWN_LOCATIONS],
[
  if test "x$OPENJDK_TARGET_OS" = xwindows; then
    BOOTJDK_DO_CHECK([BOOTJDK_FIND_BEST_JDK_IN_WINDOWS_VIRTUAL_DIRECTORY([ProgramW6432])])
    BOOTJDK_DO_CHECK([BOOTJDK_FIND_BEST_JDK_IN_WINDOWS_VIRTUAL_DIRECTORY([PROGRAMW6432])])
    BOOTJDK_DO_CHECK([BOOTJDK_FIND_BEST_JDK_IN_WINDOWS_VIRTUAL_DIRECTORY([PROGRAMFILES])])
    BOOTJDK_DO_CHECK([BOOTJDK_FIND_BEST_JDK_IN_WINDOWS_VIRTUAL_DIRECTORY([ProgramFiles])])
    BOOTJDK_DO_CHECK([BOOTJDK_FIND_BEST_JDK_IN_DIRECTORY([/cygdrive/c/Program Files/Java])])
  elif test "x$OPENJDK_TARGET_OS" = xmacosx; then
    BOOTJDK_DO_CHECK([BOOTJDK_FIND_BEST_JDK_IN_DIRECTORY([/Library/Java/JavaVirtualMachines],[/Contents/Home])])
    BOOTJDK_DO_CHECK([BOOTJDK_FIND_BEST_JDK_IN_DIRECTORY([/System/Library/Java/JavaVirtualMachines],[/Contents/Home])])
  elif test "x$OPENJDK_TARGET_OS" = xlinux; then
    BOOTJDK_DO_CHECK([BOOTJDK_FIND_BEST_JDK_IN_DIRECTORY([/usr/lib/jvm])])
  fi
])

# Check that a command-line tool in the Boot JDK is correct
# $1 = name of variable to assign
# $2 = name of binary
AC_DEFUN([BOOTJDK_CHECK_TOOL_IN_BOOTJDK],
[
  # Use user overridden value if available, otherwise locate tool in the Boot JDK.
  UTIL_REQUIRE_SPECIAL($1,
    [
      AC_MSG_CHECKING([for $2 [[Boot JDK]]])
      $1=$BOOT_JDK/bin/$2
      if test ! -x [$]$1 && test ! -x [$]$1.exe; then
        AC_MSG_RESULT(not found)
        AC_MSG_NOTICE([Your Boot JDK seems broken. This might be fixed by explicitly setting --with-boot-jdk])
        AC_MSG_ERROR([Could not find $2 in the Boot JDK])
      fi
      AC_MSG_RESULT(\[$]BOOT_JDK/bin/$2)
      UTIL_FIXUP_EXECUTABLE($1)
      AC_SUBST($1)
    ])
])

###############################################################################
#
# We need a Boot JDK to bootstrap the build.
#

AC_DEFUN_ONCE([BOOTJDK_SETUP_BOOT_JDK],
[
  BOOT_JDK_FOUND=no
  AC_ARG_WITH(boot-jdk, [AS_HELP_STRING([--with-boot-jdk],
      [path to Boot JDK (used to bootstrap build) @<:@probed@:>@])])

  AC_ARG_WITH(boot-jdk-jvmargs, [AS_HELP_STRING([--with-boot-jdk-jvmargs],
  [specify additional arguments to be passed to Boot JDK tools @<:@none@:>@])])

  USER_BOOT_JDK_OPTIONS="$with_boot_jdk_jvmargs"

  # We look for the Boot JDK through various means, going from more certain to
  # more of a guess-work. After each test, BOOT_JDK_FOUND is set to "yes" if
  # we detected something (if so, the path to the jdk is in BOOT_JDK). But we
  # must check if this is indeed valid; otherwise we'll continue looking.

  # Test: Is bootjdk explicitly set by command line arguments?
  BOOTJDK_DO_CHECK([BOOTJDK_CHECK_ARGUMENTS])
  if test "x$with_boot_jdk" != x && test "x$BOOT_JDK_FOUND" = xno; then
    # Having specified an argument which is incorrect will produce an instant failure;
    # we should not go on looking
    AC_MSG_ERROR([The path given by --with-boot-jdk does not contain a valid Boot JDK])
  fi

  # Test: Is $JAVA_HOME set?
  BOOTJDK_DO_CHECK([BOOTJDK_CHECK_JAVA_HOME])

  # Test: On MacOS X, can we find a boot jdk using /usr/libexec/java_home?
  BOOTJDK_DO_CHECK([BOOTJDK_CHECK_MACOSX_JAVA_LOCATOR])

  # Test: Is there a java or javac in the PATH, which is a symlink to the JDK?
  BOOTJDK_DO_CHECK([BOOTJDK_CHECK_JAVA_IN_PATH_IS_SYMLINK])

  # Test: Is there a JDK installed in default, well-known locations?
  BOOTJDK_DO_CHECK([BOOTJDK_CHECK_WELL_KNOWN_LOCATIONS])

  # If we haven't found anything yet, we've truly lost. Give up.
  if test "x$BOOT_JDK_FOUND" = xno; then
    HELP_MSG_MISSING_DEPENDENCY([openjdk])
    AC_MSG_NOTICE([Could not find a valid Boot JDK. $HELP_MSG])
    AC_MSG_NOTICE([This might be fixed by explicitly setting --with-boot-jdk])
    AC_MSG_ERROR([Cannot continue])
  fi

  AC_SUBST(BOOT_JDK)

  # Setup tools from the Boot JDK.
  BOOTJDK_CHECK_TOOL_IN_BOOTJDK(JAVA, java)
  BOOTJDK_CHECK_TOOL_IN_BOOTJDK(JAVAC, javac)
  BOOTJDK_CHECK_TOOL_IN_BOOTJDK(JAVADOC, javadoc)
  BOOTJDK_CHECK_TOOL_IN_BOOTJDK(JAR, jar)

  # Finally, set some other options...

  # When compiling code to be executed by the Boot JDK, force compatibility with the
  # oldest supported bootjdk.
  OLDEST_BOOT_JDK=`$ECHO $DEFAULT_ACCEPTABLE_BOOT_VERSIONS \
      | $TR " " "\n" | $SORT -n | $HEAD -n1`
  # -Xlint:-options is added to avoid "warning: [options] system modules path not set in conjunction with -source"
  BOOT_JDK_SOURCETARGET="-source $OLDEST_BOOT_JDK -target $OLDEST_BOOT_JDK -Xlint:-options"
  AC_SUBST(BOOT_JDK_SOURCETARGET)

  # Check if the boot jdk is 32 or 64 bit
  if $JAVA -version 2>&1 | $GREP -q "64-Bit"; then
    BOOT_JDK_BITS="64"
  else
    BOOT_JDK_BITS="32"
  fi
  AC_MSG_CHECKING([if Boot JDK is 32 or 64 bits])
  AC_MSG_RESULT([$BOOT_JDK_BITS])

  # Try to enable CDS
  AC_MSG_CHECKING([for local Boot JDK Class Data Sharing (CDS)])
  BOOT_JDK_CDS_ARCHIVE=$CONFIGURESUPPORT_OUTPUTDIR/classes.jsa
  UTIL_ADD_JVM_ARG_IF_OK([-XX:+UnlockDiagnosticVMOptions -XX:-VerifySharedSpaces -XX:SharedArchiveFile=$BOOT_JDK_CDS_ARCHIVE],boot_jdk_cds_args,[$JAVA])

  if test "x$boot_jdk_cds_args" != x; then
    # Try creating a CDS archive
    $JAVA $boot_jdk_cds_args -Xshare:dump > /dev/null 2>&1
    if test $? -eq 0; then
      BOOTJDK_USE_LOCAL_CDS=true
      AC_MSG_RESULT([yes, created])
    else
      # Generation failed, don't use CDS.
      BOOTJDK_USE_LOCAL_CDS=false
      AC_MSG_RESULT([no, creation failed])
    fi
  else
    BOOTJDK_USE_LOCAL_CDS=false
    AC_MSG_RESULT([no, -XX:SharedArchiveFile not supported])
  fi
])

AC_DEFUN_ONCE([BOOTJDK_SETUP_BOOT_JDK_ARGUMENTS],
[
  ##############################################################################
  #
  # Specify jvm options for anything that is run with the Boot JDK.
  # Not all JVM:s accept the same arguments on the command line.
  #
  AC_MSG_CHECKING([flags for boot jdk java command] )

  # Force en-US environment
  UTIL_ADD_JVM_ARG_IF_OK([-Duser.language=en -Duser.country=US],boot_jdk_jvmargs,[$JAVA])

  if test "x$BOOTJDK_USE_LOCAL_CDS" = xtrue; then
    # Use our own CDS archive
    UTIL_ADD_JVM_ARG_IF_OK([$boot_jdk_cds_args -Xshare:auto],boot_jdk_jvmargs,[$JAVA])
  else
    # Otherwise optimistically use the system-wide one, if one is present
    UTIL_ADD_JVM_ARG_IF_OK([-Xshare:auto],boot_jdk_jvmargs,[$JAVA])
  fi

  # Finally append user provided options to allow them to override.
  UTIL_ADD_JVM_ARG_IF_OK([$USER_BOOT_JDK_OPTIONS],boot_jdk_jvmargs,[$JAVA])

  AC_MSG_RESULT([$boot_jdk_jvmargs])

  # For now, general JAVA_FLAGS are the same as the boot jdk jvmargs
  JAVA_FLAGS=$boot_jdk_jvmargs
  AC_SUBST(JAVA_FLAGS)

  AC_MSG_CHECKING([flags for boot jdk java command for big workloads])

  # Starting amount of heap memory.
  UTIL_ADD_JVM_ARG_IF_OK([-Xms64M],boot_jdk_jvmargs_big,[$JAVA])
  BOOTCYCLE_JVM_ARGS_BIG=-Xms64M

  # Maximum amount of heap memory.
  JVM_HEAP_LIMIT_32="768"
  # Running a 64 bit JVM allows for and requires a bigger heap
  JVM_HEAP_LIMIT_64="1600"
  JVM_HEAP_LIMIT_GLOBAL=`expr $MEMORY_SIZE / 2`
  if test "$JVM_HEAP_LIMIT_GLOBAL" -lt "$JVM_HEAP_LIMIT_32"; then
    JVM_HEAP_LIMIT_32=$JVM_HEAP_LIMIT_GLOBAL
  fi
  if test "$JVM_HEAP_LIMIT_GLOBAL" -lt "$JVM_HEAP_LIMIT_64"; then
    JVM_HEAP_LIMIT_64=$JVM_HEAP_LIMIT_GLOBAL
  fi
  if test "$JVM_HEAP_LIMIT_GLOBAL" -lt "512"; then
    JVM_HEAP_LIMIT_32=512
    JVM_HEAP_LIMIT_64=512
  fi

  if test "x$BOOT_JDK_BITS" = "x32"; then
    JVM_MAX_HEAP=$JVM_HEAP_LIMIT_32
  else
    JVM_MAX_HEAP=$JVM_HEAP_LIMIT_64
  fi
  UTIL_ADD_JVM_ARG_IF_OK([-Xmx${JVM_MAX_HEAP}M],boot_jdk_jvmargs_big,[$JAVA])

  AC_MSG_RESULT([$boot_jdk_jvmargs_big])

  JAVA_FLAGS_BIG=$boot_jdk_jvmargs_big
  AC_SUBST(JAVA_FLAGS_BIG)

  if test "x$OPENJDK_TARGET_CPU_BITS" = "x32"; then
    BOOTCYCLE_MAX_HEAP=$JVM_HEAP_LIMIT_32
  else
    BOOTCYCLE_MAX_HEAP=$JVM_HEAP_LIMIT_64
  fi
  BOOTCYCLE_JVM_ARGS_BIG="$BOOTCYCLE_JVM_ARGS_BIG -Xmx${BOOTCYCLE_MAX_HEAP}M"
  AC_MSG_CHECKING([flags for bootcycle boot jdk java command for big workloads])
  AC_MSG_RESULT([$BOOTCYCLE_JVM_ARGS_BIG])
  AC_SUBST(BOOTCYCLE_JVM_ARGS_BIG)

  AC_MSG_CHECKING([flags for boot jdk java command for small workloads])

  # Use serial gc for small short lived tools if possible
  UTIL_ADD_JVM_ARG_IF_OK([-XX:+UseSerialGC],boot_jdk_jvmargs_small,[$JAVA])
  UTIL_ADD_JVM_ARG_IF_OK([-Xms32M],boot_jdk_jvmargs_small,[$JAVA])
  UTIL_ADD_JVM_ARG_IF_OK([-Xmx512M],boot_jdk_jvmargs_small,[$JAVA])
  UTIL_ADD_JVM_ARG_IF_OK([-XX:TieredStopAtLevel=1],boot_jdk_jvmargs_small,[$JAVA])

  AC_MSG_RESULT([$boot_jdk_jvmargs_small])

  JAVA_FLAGS_SMALL=$boot_jdk_jvmargs_small
  AC_SUBST(JAVA_FLAGS_SMALL)

  # Don't presuppose SerialGC is present in the buildjdk. Also, we cannot test
  # the buildjdk, but on the other hand we know what it will support.
  BUILDJDK_JAVA_FLAGS_SMALL="-Xms32M -Xmx512M -XX:TieredStopAtLevel=1"
  AC_SUBST(BUILDJDK_JAVA_FLAGS_SMALL)

  JAVA_TOOL_FLAGS_SMALL=""
  for f in $JAVA_FLAGS_SMALL; do
    JAVA_TOOL_FLAGS_SMALL="$JAVA_TOOL_FLAGS_SMALL -J$f"
  done
  AC_SUBST(JAVA_TOOL_FLAGS_SMALL)
])

# BUILD_JDK: the location of the latest JDK that can run
#   on the host system and supports the target class file version
#   generated in this JDK build.  This variable should only be
#   used after the launchers are built.
#

# Execute the check given as argument, and verify the result.
# If the JDK was previously found, do nothing.
# $1 A command line (typically autoconf macro) to execute
AC_DEFUN([BOOTJDK_CHECK_BUILD_JDK],
[
  if test "x$BUILD_JDK_FOUND" = xno; then
    # Execute the test
    $1

    # If previous step claimed to have found a JDK, check it to see if it seems to be valid.
    if test "x$BUILD_JDK_FOUND" = xmaybe; then
      # Do we have a bin/java?
      if test ! -x "$BUILD_JDK/bin/java"; then
        AC_MSG_NOTICE([Potential Build JDK found at $BUILD_JDK did not contain bin/java; ignoring])
        BUILD_JDK_FOUND=no
      elif test ! -x "$BUILD_JDK/bin/jlink"; then
        AC_MSG_NOTICE([Potential Build JDK found at $BUILD_JDK did not contain bin/jlink; ignoring])
        BUILD_JDK_FOUND=no
      elif test ! -x "$BUILD_JDK/bin/jmod"; then
        AC_MSG_NOTICE([Potential Build JDK found at $BUILD_JDK did not contain bin/jmod; ignoring])
        BUILD_JDK_FOUND=no
      elif test ! -x "$BUILD_JDK/bin/javac"; then
        # Do we have a bin/javac?
        AC_MSG_NOTICE([Potential Build JDK found at $BUILD_JDK did not contain bin/javac; ignoring])
        AC_MSG_NOTICE([(This might be a JRE instead of an JDK)])
        BUILD_JDK_FOUND=no
      else
        # Oh, this is looking good! We probably have found a proper JDK. Is it the correct version?
        # Additional [] needed to keep m4 from mangling shell constructs.
        [ BUILD_JDK_VERSION=`"$BUILD_JDK/bin/java" -version 2>&1 | $AWK '/version "[0-9a-zA-Z\._\-]+"/ {print $ 0; exit;}'` ]

        # Extra M4 quote needed to protect [] in grep expression.
        [FOUND_CORRECT_VERSION=`echo $BUILD_JDK_VERSION | $EGREP "\"$VERSION_FEATURE([\.+-].*)?\""`]
        if test "x$FOUND_CORRECT_VERSION" = x; then
          AC_MSG_NOTICE([Potential Build JDK found at $BUILD_JDK is incorrect JDK version ($BUILD_JDK_VERSION); ignoring])
          AC_MSG_NOTICE([(Your Build JDK must be version $VERSION_FEATURE)])
          BUILD_JDK_FOUND=no
        else
          # We're done!
          BUILD_JDK_FOUND=yes
          UTIL_FIXUP_PATH(BUILD_JDK)
          AC_MSG_CHECKING([for Build JDK])
          AC_MSG_RESULT([$BUILD_JDK])
          AC_MSG_CHECKING([Build JDK version])
          BUILD_JDK_VERSION=`"$BUILD_JDK/bin/java" -version 2>&1 | $TR '\n\r' '  '`
          AC_MSG_RESULT([$BUILD_JDK_VERSION])
        fi # end check jdk version
      fi # end check java
    fi # end check build jdk found
  fi
])

# By default the BUILD_JDK is the JDK_OUTPUTDIR.  If the target architecture
# is different than the host system doing the build (e.g. cross-compilation),
# a special BUILD_JDK is built as part of the build process.  An external
# prebuilt BUILD_JDK can also be supplied.
AC_DEFUN([BOOTJDK_SETUP_BUILD_JDK],
[
  AC_ARG_WITH(build-jdk, [AS_HELP_STRING([--with-build-jdk],
      [path to JDK of same version as is being built@<:@the newly built JDK@:>@])])

  CREATE_BUILDJDK=false
  EXTERNAL_BUILDJDK=false
  BUILD_JDK_FOUND="no"
  if test "x$with_build_jdk" != "x"; then
    BOOTJDK_CHECK_BUILD_JDK([
       if test "x$with_build_jdk" != x; then
         BUILD_JDK=$with_build_jdk
         BUILD_JDK_FOUND=maybe
         AC_MSG_NOTICE([Found potential Build JDK using configure arguments])
       fi])
    EXTERNAL_BUILDJDK=true
  else
    if test "x$COMPILE_TYPE" = "xcross"; then
      BUILD_JDK="\$(BUILDJDK_OUTPUTDIR)/jdk"
      BUILD_JDK_FOUND=yes
      CREATE_BUILDJDK=true
      AC_MSG_CHECKING([for Build JDK])
      AC_MSG_RESULT([yes, will build it for the host platform])
    else
      BUILD_JDK="\$(JDK_OUTPUTDIR)"
      BUILD_JDK_FOUND=yes
      AC_MSG_CHECKING([for Build JDK])
      AC_MSG_RESULT([yes, will use output dir])
    fi
  fi

  # Since these tools do not yet exist, we cannot use UTIL_FIXUP_EXECUTABLE to
  # detect the need of fixpath
  JMOD="$BUILD_JDK/bin/jmod"
  UTIL_ADD_FIXPATH(JMOD)
  JLINK="$BUILD_JDK/bin/jlink"
  UTIL_ADD_FIXPATH(JLINK)
  AC_SUBST(JMOD)
  AC_SUBST(JLINK)

  if test "x$BUILD_JDK_FOUND" != "xyes"; then
    AC_MSG_CHECKING([for Build JDK])
    AC_MSG_RESULT([no])
    AC_MSG_ERROR([Could not find a suitable Build JDK])
  fi

  AC_SUBST(CREATE_BUILDJDK)
  AC_SUBST(BUILD_JDK)
  AC_SUBST(EXTERNAL_BUILDJDK)
])

# The docs-reference JDK is used to run javadoc for the docs-reference targets.
# If not set, the reference docs will be built using the interim javadoc.
AC_DEFUN([BOOTJDK_SETUP_DOCS_REFERENCE_JDK],
[
  AC_ARG_WITH(docs-reference-jdk, [AS_HELP_STRING([--with-docs-reference-jdk],
      [path to JDK to use for building the reference documentation])])

  AC_MSG_CHECKING([for docs-reference JDK])
  if test "x$with_docs_reference_jdk" != "x"; then
    DOCS_REFERENCE_JDK="$with_docs_reference_jdk"
    AC_MSG_RESULT([$DOCS_REFERENCE_JDK])
    DOCS_REFERENCE_JAVADOC="$DOCS_REFERENCE_JDK/bin/javadoc"
    if test ! -x "$DOCS_REFERENCE_JAVADOC"; then
      AC_MSG_ERROR([docs-reference JDK found at $DOCS_REFERENCE_JDK did not contain bin/javadoc])
    fi
    UTIL_FIXUP_EXECUTABLE(DOCS_REFERENCE_JAVADOC)
  else
    AC_MSG_RESULT([no, using interim javadoc for the docs-reference targets])
    # By leaving this empty, Docs.gmk will revert to the default interim javadoc
    DOCS_REFERENCE_JAVADOC=
  fi

  AC_SUBST(DOCS_REFERENCE_JAVADOC)
])
