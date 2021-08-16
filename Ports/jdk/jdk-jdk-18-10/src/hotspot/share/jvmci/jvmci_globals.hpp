/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef SHARE_JVMCI_JVMCI_GLOBALS_HPP
#define SHARE_JVMCI_JVMCI_GLOBALS_HPP

#include "runtime/globals_shared.hpp"
#include "utilities/vmEnums.hpp"

class fileStream;

#define LIBJVMCI_ERR_FILE "hs_err_pid%p_libjvmci.log"

//
// Declare all global flags used by the JVMCI compiler. Only flags that need
// to be accessible to the JVMCI C++ code should be defined here.
//
#define JVMCI_FLAGS(develop,                                                \
                    develop_pd,                                             \
                    product,                                                \
                    product_pd,                                             \
                    notproduct,                                             \
                    range,                                                  \
                    constraint)                                             \
                                                                            \
  product(bool, EnableJVMCI, false, EXPERIMENTAL,                           \
          "Enable JVMCI")                                                   \
                                                                            \
  product(bool, EnableJVMCIProduct, false, EXPERIMENTAL,                    \
          "Allow JVMCI to be used in product mode. This alters a subset of "\
          "JVMCI flags to be non-experimental, defaults UseJVMCICompiler "  \
          "and EnableJVMCI to true and defaults UseJVMCINativeLibrary "     \
          "to true if a JVMCI native library is available.")                \
                                                                            \
  product(bool, UseJVMCICompiler, false, EXPERIMENTAL,                      \
          "Use JVMCI as the default compiler. Defaults to true if "         \
          "EnableJVMCIProduct is true.")                                    \
                                                                            \
  product(bool, JVMCIPrintProperties, false, EXPERIMENTAL,                  \
          "Prints properties used by the JVMCI compiler and exits")         \
                                                                            \
  product(bool, BootstrapJVMCI, false, EXPERIMENTAL,                        \
          "Bootstrap JVMCI before running Java main method. This "          \
          "initializes the compile queue with a small set of methods "      \
          "and processes the queue until it is empty. Combining this with " \
          "-XX:-TieredCompilation makes JVMCI compile more of itself.")     \
                                                                            \
  product(bool, EagerJVMCI, false, EXPERIMENTAL,                            \
          "Force eager JVMCI initialization")                               \
                                                                            \
  product(bool, PrintBootstrap, true, EXPERIMENTAL,                         \
          "Print JVMCI bootstrap progress and summary")                     \
                                                                            \
  product(intx, JVMCIThreads, 1, EXPERIMENTAL,                              \
          "Force number of JVMCI compiler threads to use. Ignored if "      \
          "UseJVMCICompiler is false.")                                     \
          range(1, max_jint)                                                \
                                                                            \
  product(intx, JVMCIHostThreads, 1, EXPERIMENTAL,                          \
          "Force number of C1 compiler threads. Ignored if "                \
          "UseJVMCICompiler is false.")                                     \
          range(1, max_jint)                                                \
                                                                            \
  NOT_COMPILER2(product(intx, MaxVectorSize, 64,                            \
          "Max vector size in bytes, "                                      \
          "actual size could be less depending on elements type")           \
          range(0, max_jint))                                               \
                                                                            \
  NOT_COMPILER2(product(bool, ReduceInitialCardMarks, true,                 \
          "Defer write barriers of young objects"))                         \
                                                                            \
  product(intx, JVMCIEventLogLevel, 1, EXPERIMENTAL,                        \
          "Event log level for JVMCI")                                      \
          range(0, 4)                                                       \
                                                                            \
  product(intx, JVMCITraceLevel, 0, EXPERIMENTAL,                           \
          "Trace level for JVMCI")                                          \
          range(0, 4)                                                       \
                                                                            \
  product(intx, JVMCICounterSize, 0, EXPERIMENTAL,                          \
          "Reserved size for benchmark counters")                           \
          range(0, 1000000)                                                 \
                                                                            \
  product(bool, JVMCICountersExcludeCompiler, true, EXPERIMENTAL,           \
          "Exclude JVMCI compiler threads from benchmark counters")         \
                                                                            \
  develop(bool, JVMCIUseFastLocking, true,                                  \
          "Use fast inlined locking code")                                  \
                                                                            \
  product(intx, JVMCINMethodSizeLimit, (80*K)*wordSize, EXPERIMENTAL,       \
          "Maximum size of a compiled method.")                             \
          range(0, max_jint)                                                \
                                                                            \
  product(ccstr, JVMCILibPath, NULL, EXPERIMENTAL,                          \
          "LD path for loading the JVMCI shared library")                   \
                                                                            \
  product(ccstr, JVMCILibDumpJNIConfig, NULL, EXPERIMENTAL,                 \
          "Dumps to the given file a description of the classes, fields "   \
          "and methods the JVMCI shared library must provide")              \
                                                                            \
  product(bool, UseJVMCINativeLibrary, false, EXPERIMENTAL,                 \
          "Execute JVMCI Java code from a shared library "                  \
          "instead of loading it from class files and executing it "        \
          "on the HotSpot heap. Defaults to true if EnableJVMCIProduct is " \
          "true and a JVMCI native library is available.")                  \
                                                                            \
  product(ccstr, JVMCINativeLibraryErrorFile, NULL, EXPERIMENTAL,           \
          "If an error in the JVMCI native library occurs, save the "       \
          "error data to this file"                                         \
          "[default: ./" LIBJVMCI_ERR_FILE "] (%p replaced with pid)")      \
                                                                            \
  NOT_COMPILER2(product(bool, UseMultiplyToLenIntrinsic, false, DIAGNOSTIC, \
          "Enables intrinsification of BigInteger.multiplyToLen()"))        \
                                                                            \
  NOT_COMPILER2(product(bool, UseSquareToLenIntrinsic, false, DIAGNOSTIC,   \
          "Enables intrinsification of BigInteger.squareToLen()"))          \
                                                                            \
  NOT_COMPILER2(product(bool, UseMulAddIntrinsic, false, DIAGNOSTIC,        \
          "Enables intrinsification of BigInteger.mulAdd()"))               \
                                                                            \
  NOT_COMPILER2(product(bool, UseMontgomeryMultiplyIntrinsic, false, DIAGNOSTIC, \
          "Enables intrinsification of BigInteger.montgomeryMultiply()"))   \
                                                                            \
  NOT_COMPILER2(product(bool, UseMontgomerySquareIntrinsic, false, DIAGNOSTIC, \
          "Enables intrinsification of BigInteger.montgomerySquare()"))

// end of JVMCI_FLAGS

DECLARE_FLAGS(JVMCI_FLAGS)

// The base name for the shared library containing the JVMCI based compiler
#define JVMCI_SHARED_LIBRARY_NAME "jvmcicompiler"

class JVMCIGlobals {
 private:
  static fileStream* _jni_config_file;
 public:

  // Returns true if jvmci flags are consistent. If not consistent,
  // an error message describing the inconsistency is printed before
  // returning false.
  static bool check_jvmci_flags_are_consistent();

  // Convert JVMCI experimental flags to product
  static bool enable_jvmci_product_mode(JVMFlagOrigin);

  // Returns true iff the GC fully supports JVMCI.
  static bool gc_supports_jvmci();

  // Check and turn off EnableJVMCI if selected GC does not support JVMCI.
  static void check_jvmci_supported_gc();

  static fileStream* get_jni_config_file() { return _jni_config_file; }
};
#endif // SHARE_JVMCI_JVMCI_GLOBALS_HPP
