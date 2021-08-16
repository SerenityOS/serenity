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
# Terminology used in this file:
#
# Valid features      == All possible features that the JVM knows about.
# Deprecated features == Previously known features (not considered valid).
# Available features  == Features that are possible to use in this configuration.
# Default features    == Features that are on by default in this configuration.
# Enabled features    == Features requested by the user to be present.
# Disabled features   == Features excluded from being used by the user.
# Active features     == The exact set of features to be used for a JVM variant.
#
# All valid features are considered available, unless listed as unavailable.
# All available features will be turned on as default, unless listed in a filter.
###############################################################################

# We need these as m4 defines to be able to loop over them using m4 later on.

# All valid JVM features, regardless of platform
m4_define(jvm_features_valid, m4_normalize( \
    ifdef([custom_jvm_features_valid], custom_jvm_features_valid) \
    \
    cds compiler1 compiler2 dtrace epsilongc g1gc jfr jni-check \
    jvmci jvmti link-time-opt management minimal nmt opt-size parallelgc \
    serialgc services shenandoahgc static-build vm-structs zero zgc \
))

# Deprecated JVM features (these are ignored, but with a warning)
m4_define(jvm_features_deprecated, m4_normalize(
    cmsgc trace \
))

# Feature descriptions
m4_define(jvm_feature_desc_cds, [enable class data sharing (CDS)])
m4_define(jvm_feature_desc_compiler1, [enable hotspot compiler C1])
m4_define(jvm_feature_desc_compiler2, [enable hotspot compiler C2])
m4_define(jvm_feature_desc_dtrace, [enable dtrace support])
m4_define(jvm_feature_desc_epsilongc, [include the epsilon (no-op) garbage collector])
m4_define(jvm_feature_desc_g1gc, [include the G1 garbage collector])
m4_define(jvm_feature_desc_jfr, [enable JDK Flight Recorder (JFR)])
m4_define(jvm_feature_desc_jni_check, [enable -Xcheck:jni support])
m4_define(jvm_feature_desc_jvmci, [enable JVM Compiler Interface (JVMCI)])
m4_define(jvm_feature_desc_jvmti, [enable Java Virtual Machine Tool Interface (JVM TI)])
m4_define(jvm_feature_desc_link_time_opt, [enable link time optimization])
m4_define(jvm_feature_desc_management, [enable java.lang.management API support])
m4_define(jvm_feature_desc_minimal, [support building variant 'minimal'])
m4_define(jvm_feature_desc_nmt, [include native memory tracking (NMT)])
m4_define(jvm_feature_desc_opt_size, [optimize the JVM library for size])
m4_define(jvm_feature_desc_parallelgc, [include the parallel garbage collector])
m4_define(jvm_feature_desc_serialgc, [include the serial garbage collector])
m4_define(jvm_feature_desc_services, [enable diagnostic services and client attaching])
m4_define(jvm_feature_desc_shenandoahgc, [include the Shenandoah garbage collector])
m4_define(jvm_feature_desc_static_build, [build static library instead of dynamic])
m4_define(jvm_feature_desc_vm_structs, [export JVM structures to the Serviceablility Agent])
m4_define(jvm_feature_desc_zero, [support building variant 'zero'])
m4_define(jvm_feature_desc_zgc, [include the Z garbage collector])

###############################################################################
# Parse command line options for JVM feature selection. After this function
# has run $JVM_FEATURES_ENABLED, $JVM_FEATURES_DISABLED and $JVM_FEATURES_VALID
# can be used.
#
AC_DEFUN_ONCE([JVM_FEATURES_PARSE_OPTIONS],
[
  # Setup shell variables from the m4 lists
  UTIL_SORT_LIST(JVM_FEATURES_VALID, "jvm_features_valid")
  UTIL_SORT_LIST(JVM_FEATURES_DEPRECATED, "jvm_features_deprecated")

  # For historical reasons, some jvm features have their own, shorter names.
  # Keep those as aliases for the --enable-jvm-feature-* style arguments.
  UTIL_ALIASED_ARG_ENABLE(cds, --enable-jvm-feature-cds)
  UTIL_ALIASED_ARG_ENABLE(dtrace, --enable-jvm-feature-dtrace)

  # First check for features using the
  # --with-jvm-features="<[-]feature>[,<[-]feature> ...]" syntax.
  AC_ARG_WITH([jvm-features], [AS_HELP_STRING([--with-jvm-features],
      [JVM features to enable (foo) or disable (-foo), separated by comma. Use
      '--help' to show possible values @<:@none@:>@])])
  if test "x$with_jvm_features" != x; then
    # Replace ","  with " ".
    user_jvm_feature_list=${with_jvm_features//,/ }
    JVM_FEATURES_ENABLED=`$ECHO $user_jvm_feature_list | \
        $AWK '{ for (i=1; i<=NF; i++) if (!match($i, /^-.*/)) printf("%s ", $i) }'`
    JVM_FEATURES_DISABLED=`$ECHO $user_jvm_feature_list | \
        $AWK '{ for (i=1; i<=NF; i++) if (match($i, /^-.*/)) printf("%s ", substr($i, 2))}'`

    # Verify that the user has provided only valid (or deprecated) features
    UTIL_GET_NON_MATCHING_VALUES(invalid_features, $JVM_FEATURES_ENABLED \
        $JVM_FEATURES_DISABLED, $JVM_FEATURES_VALID $JVM_FEATURES_DEPRECATED)
    if test "x$invalid_features" != x; then
      AC_MSG_NOTICE([Unknown JVM features specified: '$invalid_features'])
      AC_MSG_NOTICE([The available JVM features are: '$JVM_FEATURES_VALID'])
      AC_MSG_ERROR([Cannot continue])
    fi

    # Check if the user has provided deprecated features
    UTIL_GET_MATCHING_VALUES(deprecated_features, $JVM_FEATURES_ENABLED \
        $JVM_FEATURES_DISABLED, $JVM_FEATURES_DEPRECATED)
    if test "x$deprecated_features" != x; then
      AC_MSG_WARN([Deprecated JVM features specified (will be ignored): '$deprecated_features'])
      # Filter out deprecated features
      UTIL_GET_NON_MATCHING_VALUES(JVM_FEATURES_ENABLED, \
          $JVM_FEATURES_ENABLED, $deprecated_features)
      UTIL_GET_NON_MATCHING_VALUES(JVM_FEATURES_DISABLED, \
          $JVM_FEATURES_DISABLED, $deprecated_features)
    fi
  fi

  # Then check for features using the "--enable-jvm-feature-<feature>" syntax.
  # Using m4, loop over all features with the variable FEATURE.
  m4_foreach(FEATURE, m4_split(jvm_features_valid), [
    # Create an m4 variable containing a shell variable name (like
    # "enable_jvm_feature_static_build"), and the description.
    m4_define(FEATURE_SHELL, [enable_jvm_feature_]m4_translit(FEATURE, -, _))
    m4_define(FEATURE_DESCRIPTION, [jvm_feature_desc_]m4_translit(FEATURE, -, _))

    AC_ARG_ENABLE(jvm-feature-FEATURE, AS_HELP_STRING(
        [--enable-jvm-feature-FEATURE], [enable jvm feature 'FEATURE' (FEATURE_DESCRIPTION)]))

    if test "x$FEATURE_SHELL" = xyes; then
      JVM_FEATURES_ENABLED="$JVM_FEATURES_ENABLED FEATURE"
    elif test "x$FEATURE_SHELL" = xno; then
      JVM_FEATURES_DISABLED="$JVM_FEATURES_DISABLED FEATURE"
    elif test "x$FEATURE_SHELL" != x; then
      AC_MSG_ERROR([Invalid value for --enable-jvm-feature-FEATURE: '$FEATURE_SHELL'])
    fi

    m4_undefine([FEATURE_SHELL])
    m4_undefine([FEATURE_DESCRIPTION])
  ])

  # Likewise, check for deprecated arguments.
  m4_foreach(FEATURE, m4_split(jvm_features_deprecated), [
    AC_ARG_ENABLE(jvm-feature-FEATURE, AS_HELP_STRING(
        [--enable-jvm-feature-FEATURE],
        [Deprecated. Option is kept for backwards compatibility and is ignored]))

    m4_define(FEATURE_SHELL, [enable_jvm_feature_]m4_translit(FEATURE, -, _))

    if test "x$FEATURE_SHELL" != x; then
      AC_MSG_WARN([Deprecated JVM feature, will be ignored: --enable-jvm-feature-FEATURE])
    fi

    m4_undefine([FEATURE_SHELL])
  ])

  # Check if the user has both enabled and disabled a feature
  UTIL_GET_MATCHING_VALUES(enabled_and_disabled, $JVM_FEATURES_ENABLED, \
      $JVM_FEATURES_DISABLED)
  if test "x$enabled_and_disabled" != x; then
    AC_MSG_NOTICE([These feature are both enabled and disabled: '$enabled_and_disabled'])
    AC_MSG_NOTICE([This can happen if you mix --with-jvm-features and --enable-jvm-feature-*])
    AC_MSG_NOTICE([The recommendation is to only use --enable-jvm-feature-*])
    AC_MSG_ERROR([Cannot continue])
  fi

  # Clean up lists and announce results to user
  UTIL_SORT_LIST(JVM_FEATURES_ENABLED, $JVM_FEATURES_ENABLED)
  AC_MSG_CHECKING([for JVM features enabled by the user])
  if test "x$JVM_FEATURES_ENABLED" != x; then
    AC_MSG_RESULT(['$JVM_FEATURES_ENABLED'])
  else
    AC_MSG_RESULT([none])
  fi

  UTIL_SORT_LIST(JVM_FEATURES_DISABLED, $JVM_FEATURES_DISABLED)
  AC_MSG_CHECKING([for JVM features disabled by the user])
  if test "x$JVM_FEATURES_DISABLED" != x; then
    AC_MSG_RESULT(['$JVM_FEATURES_DISABLED'])
  else
    AC_MSG_RESULT([none])
  fi

  # Makefiles use VALID_JVM_FEATURES in check-jvm-feature to verify correctness.
  VALID_JVM_FEATURES="$JVM_FEATURES_VALID"
  AC_SUBST(VALID_JVM_FEATURES)
])

###############################################################################
# Helper function for the JVM_FEATURES_CHECK_* suite.
# The code in the code block should assign 'false' to the variable AVAILABLE
# if the feature is not available, and this function will handle everything
# else that is needed.
#
# arg 1: The name of the feature to test
# arg 2: The code block to execute
#
AC_DEFUN([JVM_FEATURES_CHECK_AVAILABILITY],
[
  # Assume that feature is available
  AVAILABLE=true

  # Execute feature test block
  $2

  AC_MSG_CHECKING([if JVM feature '$1' is available])
  if test "x$AVAILABLE" = "xtrue"; then
    AC_MSG_RESULT([yes])
  else
    AC_MSG_RESULT([no])
    JVM_FEATURES_PLATFORM_UNAVAILABLE="$JVM_FEATURES_PLATFORM_UNAVAILABLE $1"
  fi
])

###############################################################################
# Check if the feature 'cds' is available on this platform.
#
AC_DEFUN_ONCE([JVM_FEATURES_CHECK_CDS],
[
  JVM_FEATURES_CHECK_AVAILABILITY(cds, [
    AC_MSG_CHECKING([if platform is supported by CDS])
    if test "x$OPENJDK_TARGET_OS" = xaix; then
      AC_MSG_RESULT([no, $OPENJDK_TARGET_OS-$OPENJDK_TARGET_CPU])
      AVAILABLE=false
    else
      AC_MSG_RESULT([yes])
    fi
  ])
])

###############################################################################
# Check if the feature 'dtrace' is available on this platform.
#
AC_DEFUN_ONCE([JVM_FEATURES_CHECK_DTRACE],
[
  JVM_FEATURES_CHECK_AVAILABILITY(dtrace, [
    AC_MSG_CHECKING([for dtrace tool])
    if test "x$DTRACE" != "x" && test -x "$DTRACE"; then
      AC_MSG_RESULT([$DTRACE])
    else
      AC_MSG_RESULT([no])
      AVAILABLE=false
    fi

    AC_CHECK_HEADERS([sys/sdt.h], [dtrace_headers_ok=true])
    if test "x$dtrace_headers_ok" != "xtrue"; then
      HELP_MSG_MISSING_DEPENDENCY([dtrace])
      AC_MSG_NOTICE([Cannot enable dtrace with missing dependencies. See above.])
      AVAILABLE=false
    fi
  ])
])

###############################################################################
# Check if the feature 'jfr' is available on this platform.
#
AC_DEFUN_ONCE([JVM_FEATURES_CHECK_JFR],
[
  JVM_FEATURES_CHECK_AVAILABILITY(jfr, [
    AC_MSG_CHECKING([if platform is supported by JFR])
    if test "x$OPENJDK_TARGET_OS" = xaix; then
      AC_MSG_RESULT([no, $OPENJDK_TARGET_OS-$OPENJDK_TARGET_CPU])
      AVAILABLE=false
    else
      AC_MSG_RESULT([yes])
    fi
  ])
])

###############################################################################
# Check if the feature 'jvmci' is available on this platform.
#
AC_DEFUN_ONCE([JVM_FEATURES_CHECK_JVMCI],
[
  JVM_FEATURES_CHECK_AVAILABILITY(jvmci, [
    AC_MSG_CHECKING([if platform is supported by JVMCI])
    if test "x$OPENJDK_TARGET_CPU" = "xx86_64"; then
      AC_MSG_RESULT([yes])
    elif test "x$OPENJDK_TARGET_CPU" = "xaarch64"; then
      AC_MSG_RESULT([yes])
    else
      AC_MSG_RESULT([no, $OPENJDK_TARGET_CPU])
      AVAILABLE=false
    fi
  ])
])

###############################################################################
# Check if the feature 'shenandoahgc' is available on this platform.
#
AC_DEFUN_ONCE([JVM_FEATURES_CHECK_SHENANDOAHGC],
[
  JVM_FEATURES_CHECK_AVAILABILITY(shenandoahgc, [
    AC_MSG_CHECKING([if platform is supported by Shenandoah])
    if test "x$OPENJDK_TARGET_CPU_ARCH" = "xx86" || \
        test "x$OPENJDK_TARGET_CPU" = "xaarch64" ; then
      AC_MSG_RESULT([yes])
    else
      AC_MSG_RESULT([no, $OPENJDK_TARGET_CPU])
      AVAILABLE=false
    fi
  ])
])

###############################################################################
# Check if the feature 'static-build' is available on this platform.
#
AC_DEFUN_ONCE([JVM_FEATURES_CHECK_STATIC_BUILD],
[
  JVM_FEATURES_CHECK_AVAILABILITY(static-build, [
    AC_MSG_CHECKING([if static-build is enabled in configure])
    if test "x$STATIC_BUILD" = "xtrue"; then
      AC_MSG_RESULT([yes])
    else
      AC_MSG_RESULT([no, use --enable-static-build to enable static build.])
      AVAILABLE=false
    fi
  ])
])

###############################################################################
# Check if the feature 'zgc' is available on this platform.
#
AC_DEFUN_ONCE([JVM_FEATURES_CHECK_ZGC],
[
  JVM_FEATURES_CHECK_AVAILABILITY(zgc, [
    AC_MSG_CHECKING([if platform is supported by ZGC])
    if test "x$OPENJDK_TARGET_CPU" = "xx86_64"; then
      if test "x$OPENJDK_TARGET_OS" = "xlinux" || \
          test "x$OPENJDK_TARGET_OS" = "xwindows" || \
          test "x$OPENJDK_TARGET_OS" = "xmacosx"; then
        AC_MSG_RESULT([yes])
      else
        AC_MSG_RESULT([no, $OPENJDK_TARGET_OS-$OPENJDK_TARGET_CPU])
        AVAILABLE=false
      fi
    elif test "x$OPENJDK_TARGET_CPU" = "xaarch64"; then
      if test "x$OPENJDK_TARGET_OS" = "xlinux" || \
          test "x$OPENJDK_TARGET_OS" = "xwindows" || \
          test "x$OPENJDK_TARGET_OS" = "xmacosx"; then
        AC_MSG_RESULT([yes])
      else
        AC_MSG_RESULT([no, $OPENJDK_TARGET_OS-$OPENJDK_TARGET_CPU])
        AVAILABLE=false
      fi
    else
      AC_MSG_RESULT([no, $OPENJDK_TARGET_OS-$OPENJDK_TARGET_CPU])
      AVAILABLE=false
    fi

    if test "x$OPENJDK_TARGET_OS" = "xwindows"; then
      AC_MSG_CHECKING([if Windows APIs required for ZGC is present])
      AC_COMPILE_IFELSE(
        [AC_LANG_PROGRAM([[#include <windows.h>]],
          [[struct MEM_EXTENDED_PARAMETER x;]])
        ],
        [
          AC_MSG_RESULT([yes])
        ],
        [
          AC_MSG_RESULT([no, missing required APIs])
          AVAILABLE=false
        ]
      )
    fi
  ])
])

###############################################################################
# Setup JVM_FEATURES_PLATFORM_UNAVAILABLE and JVM_FEATURES_PLATFORM_FILTER
# to contain those features that are unavailable, or should be off by default,
# for this platform, regardless of JVM variant.
#
AC_DEFUN_ONCE([JVM_FEATURES_PREPARE_PLATFORM],
[
  # The checks below should add unavailable features to
  # JVM_FEATURES_PLATFORM_UNAVAILABLE.

  JVM_FEATURES_CHECK_CDS
  JVM_FEATURES_CHECK_DTRACE
  JVM_FEATURES_CHECK_JFR
  JVM_FEATURES_CHECK_JVMCI
  JVM_FEATURES_CHECK_SHENANDOAHGC
  JVM_FEATURES_CHECK_STATIC_BUILD
  JVM_FEATURES_CHECK_ZGC

  # Filter out features by default for all variants on certain platforms.
  # Make sure to just add to JVM_FEATURES_PLATFORM_FILTER, since it could
  # have a value already from custom extensions.
  if test "x$OPENJDK_TARGET_OS" = xaix; then
    JVM_FEATURES_PLATFORM_FILTER="$JVM_FEATURES_PLATFORM_FILTER jfr"
  fi
])

###############################################################################
# Setup JVM_FEATURES_VARIANT_UNAVAILABLE and JVM_FEATURES_VARIANT_FILTER
# to contain those features that are unavailable, or should be off by default,
# for this particular JVM variant.
#
# arg 1: JVM variant
#
AC_DEFUN([JVM_FEATURES_PREPARE_VARIANT],
[
  variant=$1

  # Check which features are unavailable for this JVM variant.
  # This means that is not possible to build these features for this variant.
  if test "x$variant" = "xminimal"; then
    JVM_FEATURES_VARIANT_UNAVAILABLE="cds zero"
  elif test "x$variant" = "xcore"; then
    JVM_FEATURES_VARIANT_UNAVAILABLE="cds minimal zero"
  elif test "x$variant" = "xzero"; then
    JVM_FEATURES_VARIANT_UNAVAILABLE="cds compiler1 compiler2 \
        jvmci minimal zgc"
  else
    JVM_FEATURES_VARIANT_UNAVAILABLE="minimal zero"
  fi

  # Check which features should be off by default for this JVM variant.
  if test "x$variant" = "xclient"; then
    JVM_FEATURES_VARIANT_FILTER="compiler2 jvmci link-time-opt opt-size"
  elif test "x$variant" = "xminimal"; then
    JVM_FEATURES_VARIANT_FILTER="cds compiler2 dtrace epsilongc g1gc \
        jfr jni-check jvmci jvmti management nmt parallelgc services \
        shenandoahgc vm-structs zgc"
    if test "x$OPENJDK_TARGET_CPU" = xarm ; then
      JVM_FEATURES_VARIANT_FILTER="$JVM_FEATURES_VARIANT_FILTER opt-size"
    else
      # Only arm-32 should have link-time-opt enabled as default.
      JVM_FEATURES_VARIANT_FILTER="$JVM_FEATURES_VARIANT_FILTER \
          link-time-opt"
    fi
  elif test "x$variant" = "xcore"; then
    JVM_FEATURES_VARIANT_FILTER="compiler1 compiler2 jvmci \
        link-time-opt opt-size"
  elif test "x$variant" = "xzero"; then
    JVM_FEATURES_VARIANT_FILTER="jfr link-time-opt opt-size"
  else
    JVM_FEATURES_VARIANT_FILTER="link-time-opt opt-size"
  fi
])

###############################################################################
# Calculate the actual set of active JVM features for this JVM variant. Store
# the result in JVM_FEATURES_ACTIVE.
#
# arg 1: JVM variant
#
AC_DEFUN([JVM_FEATURES_CALCULATE_ACTIVE],
[
  variant=$1

  # The default is set to all valid features except those unavailable or listed
  # in a filter.
  if test "x$variant" != xcustom; then
    UTIL_GET_NON_MATCHING_VALUES(default_for_variant, $JVM_FEATURES_VALID, \
        $JVM_FEATURES_PLATFORM_UNAVAILABLE $JVM_FEATURES_VARIANT_UNAVAILABLE \
        $JVM_FEATURES_PLATFORM_FILTER $JVM_FEATURES_VARIANT_FILTER)
  else
    # Except for the 'custom' variant, where the default is to start with an
    # empty set.
    default_for_variant=""
  fi

  # Verify that explicitly enabled features are available
  UTIL_GET_MATCHING_VALUES(enabled_but_unavailable, $JVM_FEATURES_ENABLED, \
      $JVM_FEATURES_PLATFORM_UNAVAILABLE $JVM_FEATURES_VARIANT_UNAVAILABLE)
  if test "x$enabled_but_unavailable" != x; then
    AC_MSG_NOTICE([ERROR: Unavailable JVM features explicitly enabled for '$variant': '$enabled_but_unavailable'])
    AC_MSG_ERROR([Cannot continue])
  fi

  # Notify the user if their command line options has no real effect
  UTIL_GET_MATCHING_VALUES(enabled_but_default, $JVM_FEATURES_ENABLED, \
      $default_for_variant)
  if test "x$enabled_but_default" != x; then
    AC_MSG_NOTICE([Default JVM features explicitly enabled for '$variant': '$enabled_but_default'])
  fi
  UTIL_GET_MATCHING_VALUES(disabled_but_unavailable, $JVM_FEATURES_DISABLED, \
      $JVM_FEATURES_PLATFORM_UNAVAILABLE $JVM_FEATURES_VARIANT_UNAVAILABLE)
  if test "x$disabled_but_unavailable" != x; then
    AC_MSG_NOTICE([Unavailable JVM features explicitly disabled for '$variant': '$disabled_but_unavailable'])
  fi

  # JVM_FEATURES_ACTIVE is the set of all default features and all explicitly
  # enabled features, with the explicitly disabled features filtered out.
  UTIL_GET_NON_MATCHING_VALUES(JVM_FEATURES_ACTIVE, $default_for_variant \
      $JVM_FEATURES_ENABLED, $JVM_FEATURES_DISABLED)
])

###############################################################################
# Helper function for JVM_FEATURES_VERIFY. Check if the specified JVM
# feature is active. To be used in shell if constructs, like this:
# 'if JVM_FEATURES_IS_ACTIVE(jvmti); then'
#
# Definition kept in one line to allow inlining in if statements.
# Additional [] needed to keep m4 from mangling shell constructs.
AC_DEFUN([JVM_FEATURES_IS_ACTIVE],
[ [ [[ " $JVM_FEATURES_ACTIVE " =~ ' '$1' ' ]] ] ])

###############################################################################
# Verify that the resulting set of features is consistent and legal.
#
# arg 1: JVM variant
#
AC_DEFUN([JVM_FEATURES_VERIFY],
[
  variant=$1

  if JVM_FEATURES_IS_ACTIVE(jvmci) && ! (JVM_FEATURES_IS_ACTIVE(compiler1) || \
      JVM_FEATURES_IS_ACTIVE(compiler2)); then
    AC_MSG_ERROR([Specified JVM feature 'jvmci' requires feature 'compiler2' or 'compiler1' for variant '$variant'])
  fi

  if JVM_FEATURES_IS_ACTIVE(jvmti) && ! JVM_FEATURES_IS_ACTIVE(services); then
    AC_MSG_ERROR([Specified JVM feature 'jvmti' requires feature 'services' for variant '$variant'])
  fi

  if JVM_FEATURES_IS_ACTIVE(management) && ! JVM_FEATURES_IS_ACTIVE(nmt); then
    AC_MSG_ERROR([Specified JVM feature 'management' requires feature 'nmt' for variant '$variant'])
  fi

  # For backwards compatibility, disable a feature "globally" if one variant
  # is missing the feature.
  if ! JVM_FEATURES_IS_ACTIVE(cds); then
    ENABLE_CDS="false"
  fi
  if ! JVM_FEATURES_IS_ACTIVE(jvmci); then
    INCLUDE_JVMCI="false"
  fi
  if JVM_FEATURES_IS_ACTIVE(compiler2); then
    INCLUDE_COMPILER2="true"
  fi

  # Verify that we have at least one gc selected (i.e., feature named "*gc").
  if ! JVM_FEATURES_IS_ACTIVE(.*gc); then
      AC_MSG_NOTICE([At least one gc needed for variant '$variant'.])
      AC_MSG_NOTICE([Specified features: '$JVM_FEATURES_ACTIVE'])
      AC_MSG_ERROR([Cannot continue])
  fi
])

###############################################################################
# Set up all JVM features for each enabled JVM variant. Requires that
# JVM_FEATURES_PARSE_OPTIONS has been called.
#
AC_DEFUN_ONCE([JVM_FEATURES_SETUP],
[
  # Set up variant-independent factors
  JVM_FEATURES_PREPARE_PLATFORM

  # For backwards compatibility, tentatively enable these features "globally",
  # and disable them in JVM_FEATURES_VERIFY if a variant is found that are
  # missing any of them.
  ENABLE_CDS="true"
  INCLUDE_JVMCI="true"
  INCLUDE_COMPILER2="false"

  for variant in $JVM_VARIANTS; do
    # Figure out if any features are unavailable, or should be filtered out
    # by default, for this variant.
    # Store the result in JVM_FEATURES_VARIANT_UNAVAILABLE and
    # JVM_FEATURES_VARIANT_FILTER.
    JVM_FEATURES_PREPARE_VARIANT($variant)

    # Calculate the resulting set of enabled features for this variant.
    # The result is stored in JVM_FEATURES_ACTIVE.
    JVM_FEATURES_CALCULATE_ACTIVE($variant)

    # Verify consistency for JVM_FEATURES_ACTIVE.
    JVM_FEATURES_VERIFY($variant)

    # Keep feature list sorted and free of duplicates
    UTIL_SORT_LIST(JVM_FEATURES_ACTIVE, $JVM_FEATURES_ACTIVE)
    AC_MSG_CHECKING([JVM features to use for variant '$variant'])
    AC_MSG_RESULT(['$JVM_FEATURES_ACTIVE'])

    # Save this as e.g. JVM_FEATURES_server, using indirect variable
    # referencing.
    features_var_name=JVM_FEATURES_$variant
    eval $features_var_name=\"$JVM_FEATURES_ACTIVE\"
  done

  # Unfortunately AC_SUBST does not work with non-literally named variables,
  # so list all variants here.
  AC_SUBST(JVM_FEATURES_server)
  AC_SUBST(JVM_FEATURES_client)
  AC_SUBST(JVM_FEATURES_minimal)
  AC_SUBST(JVM_FEATURES_core)
  AC_SUBST(JVM_FEATURES_zero)
  AC_SUBST(JVM_FEATURES_custom)

  AC_SUBST(INCLUDE_JVMCI)
  AC_SUBST(INCLUDE_COMPILER2)

])
