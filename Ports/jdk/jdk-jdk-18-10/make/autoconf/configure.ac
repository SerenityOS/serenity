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

###############################################################################
#
# Includes and boilerplate
#
###############################################################################


AC_PREREQ([2.69])
AC_INIT(OpenJDK, openjdk, build-dev@openjdk.java.net,,http://openjdk.java.net)

AC_CONFIG_AUX_DIR([$TOPDIR/make/autoconf/build-aux])
m4_include([build-aux/pkg.m4])

AC_DEFUN_ONCE([CUSTOM_EARLY_HOOK])
AC_DEFUN_ONCE([CUSTOM_LATE_HOOK])
AC_DEFUN_ONCE([CUSTOM_CONFIG_OUTPUT_GENERATED_HOOK])
AC_DEFUN_ONCE([CUSTOM_SUMMARY_AND_WARNINGS_HOOK])

# This line needs to be here, verbatim, after the dummy hook definitions but
# before all includes. It is replaced with custom functionality when building
# custom sources.
#CUSTOM_AUTOCONF_INCLUDE

# Include these first...
m4_include([util.m4])
# ... then the rest
m4_include([basic.m4])
m4_include([boot-jdk.m4])
m4_include([build-performance.m4])
m4_include([flags.m4])
m4_include([help.m4])
m4_include([hotspot.m4])
m4_include([jdk-options.m4])
m4_include([jdk-version.m4])
m4_include([jvm-features.m4])
m4_include([libraries.m4])
m4_include([platform.m4])
m4_include([source-dirs.m4])
m4_include([toolchain.m4])

###############################################################################
#
# Initialization / Boot-strapping
#
# The bootstrapping process needs to solve the "chicken or the egg" problem,
# thus it jumps back and forth, each time gaining something needed later on.
#
###############################################################################

# If we are requested to print additional help, do that and then exit.
# This must be the very first call.
HELP_PRINT_ADDITIONAL_HELP_AND_EXIT

# Basic initialization that must happen first of all in the normal process.
BASIC_INIT
BASIC_SETUP_FUNDAMENTAL_TOOLS

# Now we can determine OpenJDK build and target platforms. This is required to
# have early on.
PLATFORM_SETUP_OPENJDK_BUILD_AND_TARGET

# Continue setting up basic stuff. Most remaining code require fundamental tools.
BASIC_SETUP_PATHS

# Check if it's a pure open build or if custom sources are to be used.
JDKOPT_SETUP_OPEN_OR_CUSTOM

# These are needed to be able to create a configuration name (and thus the output directory)
JDKOPT_SETUP_DEBUG_LEVEL
HOTSPOT_SETUP_JVM_VARIANTS

# With basic setup done, call the custom early hook.
CUSTOM_EARLY_HOOK

# This only needs debug level to be setup
JDKOPT_ALLOW_ABSOLUTE_PATHS_IN_OUTPUT

# Check if we have devkits, extra paths or sysroot set.
BASIC_SETUP_DEVKIT

# To properly create a configuration name, we need to have the OpenJDK target
# and options (variants and debug level) parsed.
BASIC_SETUP_OUTPUT_DIR
# After we have the output dir we can finalize the fixpath wrapper
BASIC_WINDOWS_FINALIZE_FIXPATH

# Must be done before we can call HELP_MSG_MISSING_DEPENDENCY.
HELP_SETUP_DEPENDENCY_HELP

# Setup tools that requires more complex handling, or that is not needed by the configure script.
BASIC_SETUP_COMPLEX_TOOLS
BASIC_CHECK_BASH_OPTIONS

# Check if pkg-config is available.
PKG_PROG_PKG_CONFIG

# After basic tools have been setup, we can check build os specific details.
PLATFORM_SETUP_OPENJDK_BUILD_OS_VERSION

# Misc basic settings
BASIC_SETUP_DEFAULT_MAKE_TARGET
BASIC_SETUP_DEFAULT_LOG

###############################################################################
#
# Determine OpenJDK variants and version numbers.
#
###############################################################################

# We need build & target for this.
JDKOPT_SETUP_JLINK_OPTIONS
JDKVER_SETUP_JDK_VERSION_NUMBERS

###############################################################################
#
# Setup BootJDK, used to bootstrap the build.
#
###############################################################################

BOOTJDK_SETUP_BOOT_JDK
BOOTJDK_SETUP_BUILD_JDK
BOOTJDK_SETUP_DOCS_REFERENCE_JDK

###############################################################################
#
# Determine JDK specific build time options.
#
###############################################################################

JDKOPT_SETUP_JDK_OPTIONS

###############################################################################
#
# Configure the sources to use. We can add or override individual directories.
#
###############################################################################

SRCDIRS_SETUP_DIRS
SRCDIRS_SETUP_IMPORT_MODULES

###############################################################################
#
# Setup the toolchain (compilers etc), i.e. tools used to compile and process
# native code.
#
###############################################################################

# See if we are doing a complete static build or not
JDKOPT_SETUP_STATIC_BUILD

# First determine the toolchain type (compiler family)
TOOLCHAIN_DETERMINE_TOOLCHAIN_TYPE

# The global flags are needed for configure to be able to run the compilers
# correctly.
FLAGS_PRE_TOOLCHAIN

# Then detect the actual binaries needed
TOOLCHAIN_PRE_DETECTION
TOOLCHAIN_DETECT_TOOLCHAIN_CORE
TOOLCHAIN_DETECT_TOOLCHAIN_EXTRA
TOOLCHAIN_POST_DETECTION

# Finally do some processing after the detection phase
TOOLCHAIN_SETUP_BUILD_COMPILERS
TOOLCHAIN_MISC_CHECKS

# Setup the JTReg Regression Test Harness.
TOOLCHAIN_SETUP_JTREG

# Setup the Java Microbenchmark Harness (JMH)
LIB_TESTS_SETUP_JMH

# Setup Jib dependency tool
TOOLCHAIN_SETUP_JIB

# After toolchain setup, we need to process some flags to be able to continue.
FLAGS_POST_TOOLCHAIN

# Now we can test some aspects on the target using configure macros.
PLATFORM_SETUP_OPENJDK_TARGET_BITS
PLATFORM_SETUP_OPENJDK_TARGET_ENDIANNESS

# Configure flags for the tools. Need to know if we should build reproducible.
JDKOPT_SETUP_REPRODUCIBLE_BUILD
FLAGS_SETUP_FLAGS

# Setup debug symbols (need objcopy from the toolchain for that)
JDKOPT_SETUP_DEBUG_SYMBOLS
JDKOPT_SETUP_CODE_COVERAGE

# AddressSanitizer
JDKOPT_SETUP_ADDRESS_SANITIZER

###############################################################################
#
# Check dependencies for external and internal libraries.
#
###############################################################################

LIB_DETERMINE_DEPENDENCIES
LIB_SETUP_LIBRARIES

###############################################################################
#
# Setup hotspot and JVM features (needs toolchain).
#
###############################################################################

JVM_FEATURES_PARSE_OPTIONS
JVM_FEATURES_SETUP

HOTSPOT_SETUP_MISC

###############################################################################
#
# We need to do some final tweaking, when everything else is done.
#
###############################################################################

JDKOPT_ENABLE_DISABLE_FAILURE_HANDLER
JDKOPT_ENABLE_DISABLE_GENERATE_CLASSLIST
JDKOPT_EXCLUDE_TRANSLATIONS
JDKOPT_ENABLE_DISABLE_MANPAGES
JDKOPT_ENABLE_DISABLE_CDS_ARCHIVE
JDKOPT_ENABLE_DISABLE_COMPATIBLE_CDS_ALIGNMENT

###############################################################################
#
# Configure parts of the build that only affect the build performance,
# not the result.
#
###############################################################################

BPERF_SETUP_BUILD_CORES
BPERF_SETUP_BUILD_MEMORY
BPERF_SETUP_BUILD_JOBS
BPERF_SETUP_TEST_JOBS
BPERF_SETUP_JAVAC_SERVER

# Setup arguments for the boot jdk (after cores and memory have been setup)
BOOTJDK_SETUP_BOOT_JDK_ARGUMENTS

# Setup use of icecc if requested
BPERF_SETUP_ICECC

# Can the C/C++ compiler use precompiled headers?
BPERF_SETUP_PRECOMPILED_HEADERS

# Setup use of ccache, if available
BPERF_SETUP_CCACHE

###############################################################################
#
# And now the finish...
#
###############################################################################

# Check for some common pitfalls
BASIC_TEST_USABILITY_ISSUES

# At the end, call the custom hook. (Dummy macro if no custom sources available)
CUSTOM_LATE_HOOK

# Did user specify any unknown variables?
BASIC_CHECK_LEFTOVER_OVERRIDDEN

# We're messing a bit with internal autoconf variables to put the config.status
# in the output directory instead of the current directory.
CONFIG_STATUS="$CONFIGURESUPPORT_OUTPUTDIR/config.status"

# Create the actual output files. Now the main work of configure is done.
BASIC_WINDOWS_FINALIZE
AC_OUTPUT

# After AC_OUTPUT, we need to do final work
CUSTOM_CONFIG_OUTPUT_GENERATED_HOOK
BASIC_POST_CONFIG_OUTPUT

# Finally output some useful information to the user
HELP_PRINT_SUMMARY_AND_WARNINGS
CUSTOM_SUMMARY_AND_WARNINGS_HOOK
HELP_REPEAT_WARNINGS
