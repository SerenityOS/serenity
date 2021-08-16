/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "code/codeCache.hpp"
#include "runtime/arguments.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/flags/jvmFlagAccess.hpp"
#include "runtime/flags/jvmFlagLimit.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "compiler/compilerDefinitions.hpp"
#include "gc/shared/gcConfig.hpp"
#include "utilities/defaultStream.hpp"

const char* compilertype2name_tab[compiler_number_of_types] = {
  "",
  "c1",
  "c2",
  "jvmci"
};

CompilationModeFlag::Mode CompilationModeFlag::_mode = CompilationModeFlag::Mode::NORMAL;

static void print_mode_unavailable(const char* mode_name, const char* reason) {
  warning("%s compilation mode unavailable because %s.", mode_name, reason);
}

bool CompilationModeFlag::initialize() {
  _mode = Mode::NORMAL;
  // During parsing we want to be very careful not to use any methods of CompilerConfig that depend on
  // CompilationModeFlag.
  if (CompilationMode != NULL) {
    if (strcmp(CompilationMode, "default") == 0 || strcmp(CompilationMode, "normal") == 0) {
      assert(_mode == Mode::NORMAL, "Precondition");
    } else if (strcmp(CompilationMode, "quick-only") == 0) {
      if (!CompilerConfig::has_c1()) {
        print_mode_unavailable("quick-only", "there is no c1 present");
      } else {
        _mode = Mode::QUICK_ONLY;
      }
    } else if (strcmp(CompilationMode, "high-only") == 0) {
      if (!CompilerConfig::has_c2() && !CompilerConfig::is_jvmci_compiler()) {
        print_mode_unavailable("high-only", "there is no c2 or jvmci compiler present");
      } else {
        _mode = Mode::HIGH_ONLY;
      }
    } else if (strcmp(CompilationMode, "high-only-quick-internal") == 0) {
      if (!CompilerConfig::has_c1() || !CompilerConfig::is_jvmci_compiler()) {
        print_mode_unavailable("high-only-quick-internal", "there is no c1 and jvmci compiler present");
      } else {
        _mode = Mode::HIGH_ONLY_QUICK_INTERNAL;
      }
    } else {
      print_error();
      return false;
    }
  }

  // Now that the flag is parsed, we can use any methods of CompilerConfig.
  if (normal()) {
    if (CompilerConfig::is_c1_simple_only()) {
      _mode = Mode::QUICK_ONLY;
    } else if (CompilerConfig::is_c2_or_jvmci_compiler_only()) {
      _mode = Mode::HIGH_ONLY;
    } else if (CompilerConfig::is_jvmci_compiler_enabled() && CompilerConfig::is_c1_enabled() && !TieredCompilation) {
      warning("Disabling tiered compilation with non-native JVMCI compiler is not recommended, "
              "disabling intermediate compilation levels instead. ");
      _mode = Mode::HIGH_ONLY_QUICK_INTERNAL;
    }
  }
  return true;
}

void CompilationModeFlag::print_error() {
  jio_fprintf(defaultStream::error_stream(), "Unsupported compilation mode '%s', available modes are:", CompilationMode);
  bool comma = false;
  if (CompilerConfig::has_c1()) {
    jio_fprintf(defaultStream::error_stream(), "%s quick-only", comma ? "," : "");
    comma = true;
  }
  if (CompilerConfig::has_c2() || CompilerConfig::has_jvmci()) {
    jio_fprintf(defaultStream::error_stream(), "%s high-only", comma ? "," : "");
    comma = true;
  }
  if (CompilerConfig::has_c1() && CompilerConfig::has_jvmci()) {
    jio_fprintf(defaultStream::error_stream(), "%s high-only-quick-internal", comma ? "," : "");
    comma = true;
  }
  jio_fprintf(defaultStream::error_stream(), "\n");
}

// Returns threshold scaled with CompileThresholdScaling
intx CompilerConfig::scaled_compile_threshold(intx threshold) {
  return scaled_compile_threshold(threshold, CompileThresholdScaling);
}

// Returns freq_log scaled with CompileThresholdScaling
intx CompilerConfig::scaled_freq_log(intx freq_log) {
  return scaled_freq_log(freq_log, CompileThresholdScaling);
}

// Returns threshold scaled with the value of scale.
// If scale < 0.0, threshold is returned without scaling.
intx CompilerConfig::scaled_compile_threshold(intx threshold, double scale) {
  if (scale == 1.0 || scale < 0.0) {
    return threshold;
  } else {
    return (intx)(threshold * scale);
  }
}

// Returns freq_log scaled with the value of scale.
// Returned values are in the range of [0, InvocationCounter::number_of_count_bits + 1].
// If scale < 0.0, freq_log is returned without scaling.
intx CompilerConfig::scaled_freq_log(intx freq_log, double scale) {
  // Check if scaling is necessary or if negative value was specified.
  if (scale == 1.0 || scale < 0.0) {
    return freq_log;
  }
  // Check values to avoid calculating log2 of 0.
  if (scale == 0.0 || freq_log == 0) {
    return 0;
  }
  // Determine the maximum notification frequency value currently supported.
  // The largest mask value that the interpreter/C1 can handle is
  // of length InvocationCounter::number_of_count_bits. Mask values are always
  // one bit shorter then the value of the notification frequency. Set
  // max_freq_bits accordingly.
  int max_freq_bits = InvocationCounter::number_of_count_bits + 1;
  intx scaled_freq = scaled_compile_threshold((intx)1 << freq_log, scale);

  if (scaled_freq == 0) {
    // Return 0 right away to avoid calculating log2 of 0.
    return 0;
  } else {
    return MIN2(log2i(scaled_freq), max_freq_bits);
  }
}

void CompilerConfig::set_client_emulation_mode_flags() {
  assert(has_c1(), "Must have C1 compiler present");
  CompilationModeFlag::set_quick_only();

  FLAG_SET_ERGO(ProfileInterpreter, false);
#if INCLUDE_JVMCI
  FLAG_SET_ERGO(EnableJVMCI, false);
  FLAG_SET_ERGO(UseJVMCICompiler, false);
#endif
  if (FLAG_IS_DEFAULT(NeverActAsServerClassMachine)) {
    FLAG_SET_ERGO(NeverActAsServerClassMachine, true);
  }
  if (FLAG_IS_DEFAULT(InitialCodeCacheSize)) {
    FLAG_SET_ERGO(InitialCodeCacheSize, 160*K);
  }
  if (FLAG_IS_DEFAULT(ReservedCodeCacheSize)) {
    FLAG_SET_ERGO(ReservedCodeCacheSize, 32*M);
  }
  if (FLAG_IS_DEFAULT(NonProfiledCodeHeapSize)) {
    FLAG_SET_ERGO(NonProfiledCodeHeapSize, 27*M);
  }
  if (FLAG_IS_DEFAULT(ProfiledCodeHeapSize)) {
    FLAG_SET_ERGO(ProfiledCodeHeapSize, 0);
  }
  if (FLAG_IS_DEFAULT(NonNMethodCodeHeapSize)) {
    FLAG_SET_ERGO(NonNMethodCodeHeapSize, 5*M);
  }
  if (FLAG_IS_DEFAULT(CodeCacheExpansionSize)) {
    FLAG_SET_ERGO(CodeCacheExpansionSize, 32*K);
  }
  if (FLAG_IS_DEFAULT(MaxRAM)) {
    // Do not use FLAG_SET_ERGO to update MaxRAM, as this will impact
    // heap setting done based on available phys_mem (see Arguments::set_heap_size).
    FLAG_SET_DEFAULT(MaxRAM, 1ULL*G);
  }
  if (FLAG_IS_DEFAULT(CICompilerCount)) {
    FLAG_SET_ERGO(CICompilerCount, 1);
  }
}

bool CompilerConfig::is_compilation_mode_selected() {
  return !FLAG_IS_DEFAULT(TieredCompilation) ||
         !FLAG_IS_DEFAULT(TieredStopAtLevel) ||
         !FLAG_IS_DEFAULT(CompilationMode)
         JVMCI_ONLY(|| !FLAG_IS_DEFAULT(EnableJVMCI)
                    || !FLAG_IS_DEFAULT(UseJVMCICompiler));
}

bool CompilerConfig::is_interpreter_only() {
  return Arguments::is_interpreter_only() || TieredStopAtLevel == CompLevel_none;
}

static bool check_legacy_flags() {
  JVMFlag* compile_threshold_flag = JVMFlag::flag_from_enum(FLAG_MEMBER_ENUM(CompileThreshold));
  if (JVMFlagAccess::check_constraint(compile_threshold_flag, JVMFlagLimit::get_constraint(compile_threshold_flag)->constraint_func(), false) != JVMFlag::SUCCESS) {
    return false;
  }
  JVMFlag* on_stack_replace_percentage_flag = JVMFlag::flag_from_enum(FLAG_MEMBER_ENUM(OnStackReplacePercentage));
  if (JVMFlagAccess::check_constraint(on_stack_replace_percentage_flag, JVMFlagLimit::get_constraint(on_stack_replace_percentage_flag)->constraint_func(), false) != JVMFlag::SUCCESS) {
    return false;
  }
  JVMFlag* interpreter_profile_percentage_flag = JVMFlag::flag_from_enum(FLAG_MEMBER_ENUM(InterpreterProfilePercentage));
  if (JVMFlagAccess::check_range(interpreter_profile_percentage_flag, false) != JVMFlag::SUCCESS) {
    return false;
  }
  return true;
}

void CompilerConfig::set_legacy_emulation_flags() {
  // Any legacy flags set?
  if (!FLAG_IS_DEFAULT(CompileThreshold)         ||
      !FLAG_IS_DEFAULT(OnStackReplacePercentage) ||
      !FLAG_IS_DEFAULT(InterpreterProfilePercentage)) {
    if (CompilerConfig::is_c1_only() || CompilerConfig::is_c2_or_jvmci_compiler_only()) {
      // This function is called before these flags are validated. In order to not confuse the user with extraneous
      // error messages, we check the validity of these flags here and bail out if any of them are invalid.
      if (!check_legacy_flags()) {
        return;
      }
      // Note, we do not scale CompileThreshold before this because the tiered flags are
      // all going to be scaled further in set_compilation_policy_flags().
      const intx threshold = CompileThreshold;
      const intx profile_threshold = threshold * InterpreterProfilePercentage / 100;
      const intx osr_threshold = threshold * OnStackReplacePercentage / 100;
      const intx osr_profile_threshold = osr_threshold * InterpreterProfilePercentage / 100;

      const intx threshold_log = log2i_graceful(CompilerConfig::is_c1_only() ? threshold : profile_threshold);
      const intx osr_threshold_log = log2i_graceful(CompilerConfig::is_c1_only() ? osr_threshold : osr_profile_threshold);

      if (Tier0InvokeNotifyFreqLog > threshold_log) {
        FLAG_SET_ERGO(Tier0InvokeNotifyFreqLog, MAX2<intx>(0, threshold_log));
      }

      // Note: Emulation oddity. The legacy policy limited the amount of callbacks from the
      // interpreter for backedge events to once every 1024 counter increments.
      // We simulate this behavior by limiting the backedge notification frequency to be
      // at least 2^10.
      if (Tier0BackedgeNotifyFreqLog > osr_threshold_log) {
        FLAG_SET_ERGO(Tier0BackedgeNotifyFreqLog, MAX2<intx>(10, osr_threshold_log));
      }
      // Adjust the tiered policy flags to approximate the legacy behavior.
      if (CompilerConfig::is_c1_only()) {
        FLAG_SET_ERGO(Tier3InvocationThreshold, threshold);
        FLAG_SET_ERGO(Tier3MinInvocationThreshold, threshold);
        FLAG_SET_ERGO(Tier3CompileThreshold, threshold);
        FLAG_SET_ERGO(Tier3BackEdgeThreshold, osr_threshold);
      } else {
        FLAG_SET_ERGO(Tier4InvocationThreshold, threshold);
        FLAG_SET_ERGO(Tier4MinInvocationThreshold, threshold);
        FLAG_SET_ERGO(Tier4CompileThreshold, threshold);
        FLAG_SET_ERGO(Tier4BackEdgeThreshold, osr_threshold);
        FLAG_SET_ERGO(Tier0ProfilingStartPercentage, InterpreterProfilePercentage);
      }
    } else {
      // Normal tiered mode, ignore legacy flags
    }
  }
  // Scale CompileThreshold
  // CompileThresholdScaling == 0.0 is equivalent to -Xint and leaves CompileThreshold unchanged.
  if (!FLAG_IS_DEFAULT(CompileThresholdScaling) && CompileThresholdScaling > 0.0 && CompileThreshold > 0) {
    FLAG_SET_ERGO(CompileThreshold, scaled_compile_threshold(CompileThreshold));
  }
}


void CompilerConfig::set_compilation_policy_flags() {
  if (is_tiered()) {
    // Increase the code cache size - tiered compiles a lot more.
    if (FLAG_IS_DEFAULT(ReservedCodeCacheSize)) {
      FLAG_SET_ERGO(ReservedCodeCacheSize,
                    MIN2(CODE_CACHE_DEFAULT_LIMIT, (size_t)ReservedCodeCacheSize * 5));
    }
    // Enable SegmentedCodeCache if tiered compilation is enabled, ReservedCodeCacheSize >= 240M
    // and the code cache contains at least 8 pages (segmentation disables advantage of huge pages).
    if (FLAG_IS_DEFAULT(SegmentedCodeCache) && ReservedCodeCacheSize >= 240*M &&
        8 * CodeCache::page_size() <= ReservedCodeCacheSize) {
      FLAG_SET_ERGO(SegmentedCodeCache, true);
    }
    if (Arguments::is_compiler_only()) { // -Xcomp
      // Be much more aggressive in tiered mode with -Xcomp and exercise C2 more.
      // We will first compile a level 3 version (C1 with full profiling), then do one invocation of it and
      // compile a level 4 (C2) and then continue executing it.
      if (FLAG_IS_DEFAULT(Tier3InvokeNotifyFreqLog)) {
        FLAG_SET_CMDLINE(Tier3InvokeNotifyFreqLog, 0);
      }
      if (FLAG_IS_DEFAULT(Tier4InvocationThreshold)) {
        FLAG_SET_CMDLINE(Tier4InvocationThreshold, 0);
      }
    }
  }


  if (CompileThresholdScaling < 0) {
    vm_exit_during_initialization("Negative value specified for CompileThresholdScaling", NULL);
  }

  if (CompilationModeFlag::disable_intermediate()) {
    if (FLAG_IS_DEFAULT(Tier0ProfilingStartPercentage)) {
      FLAG_SET_DEFAULT(Tier0ProfilingStartPercentage, 33);
    }

    if (FLAG_IS_DEFAULT(Tier4InvocationThreshold)) {
      FLAG_SET_DEFAULT(Tier4InvocationThreshold, 5000);
    }
    if (FLAG_IS_DEFAULT(Tier4MinInvocationThreshold)) {
      FLAG_SET_DEFAULT(Tier4MinInvocationThreshold, 600);
    }
    if (FLAG_IS_DEFAULT(Tier4CompileThreshold)) {
      FLAG_SET_DEFAULT(Tier4CompileThreshold, 10000);
    }
    if (FLAG_IS_DEFAULT(Tier4BackEdgeThreshold)) {
      FLAG_SET_DEFAULT(Tier4BackEdgeThreshold, 15000);
    }
  }

  // Scale tiered compilation thresholds.
  // CompileThresholdScaling == 0.0 is equivalent to -Xint and leaves compilation thresholds unchanged.
  if (!FLAG_IS_DEFAULT(CompileThresholdScaling) && CompileThresholdScaling > 0.0) {
    FLAG_SET_ERGO(Tier0InvokeNotifyFreqLog, scaled_freq_log(Tier0InvokeNotifyFreqLog));
    FLAG_SET_ERGO(Tier0BackedgeNotifyFreqLog, scaled_freq_log(Tier0BackedgeNotifyFreqLog));

    FLAG_SET_ERGO(Tier3InvocationThreshold, scaled_compile_threshold(Tier3InvocationThreshold));
    FLAG_SET_ERGO(Tier3MinInvocationThreshold, scaled_compile_threshold(Tier3MinInvocationThreshold));
    FLAG_SET_ERGO(Tier3CompileThreshold, scaled_compile_threshold(Tier3CompileThreshold));
    FLAG_SET_ERGO(Tier3BackEdgeThreshold, scaled_compile_threshold(Tier3BackEdgeThreshold));

    // Tier2{Invocation,MinInvocation,Compile,Backedge}Threshold should be scaled here
    // once these thresholds become supported.

    FLAG_SET_ERGO(Tier2InvokeNotifyFreqLog, scaled_freq_log(Tier2InvokeNotifyFreqLog));
    FLAG_SET_ERGO(Tier2BackedgeNotifyFreqLog, scaled_freq_log(Tier2BackedgeNotifyFreqLog));

    FLAG_SET_ERGO(Tier3InvokeNotifyFreqLog, scaled_freq_log(Tier3InvokeNotifyFreqLog));
    FLAG_SET_ERGO(Tier3BackedgeNotifyFreqLog, scaled_freq_log(Tier3BackedgeNotifyFreqLog));

    FLAG_SET_ERGO(Tier23InlineeNotifyFreqLog, scaled_freq_log(Tier23InlineeNotifyFreqLog));

    FLAG_SET_ERGO(Tier4InvocationThreshold, scaled_compile_threshold(Tier4InvocationThreshold));
    FLAG_SET_ERGO(Tier4MinInvocationThreshold, scaled_compile_threshold(Tier4MinInvocationThreshold));
    FLAG_SET_ERGO(Tier4CompileThreshold, scaled_compile_threshold(Tier4CompileThreshold));
    FLAG_SET_ERGO(Tier4BackEdgeThreshold, scaled_compile_threshold(Tier4BackEdgeThreshold));
  }

#ifdef COMPILER1
  // Reduce stack usage due to inlining of methods which require much stack.
  // (High tier compiler can inline better based on profiling information.)
  if (FLAG_IS_DEFAULT(C1InlineStackLimit) &&
      TieredStopAtLevel == CompLevel_full_optimization && !CompilerConfig::is_c1_only()) {
    FLAG_SET_DEFAULT(C1InlineStackLimit, 5);
  }
#endif

  if (CompilerConfig::is_tiered() && CompilerConfig::is_c2_enabled()) {
#ifdef COMPILER2
    // Some inlining tuning
#ifdef X86
    if (FLAG_IS_DEFAULT(InlineSmallCode)) {
      FLAG_SET_DEFAULT(InlineSmallCode, 2500);
    }
#endif

#if defined AARCH64
    if (FLAG_IS_DEFAULT(InlineSmallCode)) {
      FLAG_SET_DEFAULT(InlineSmallCode, 2500);
    }
#endif
#endif // COMPILER2
  }

}

#if INCLUDE_JVMCI
void CompilerConfig::set_jvmci_specific_flags() {
  if (UseJVMCICompiler) {
    if (FLAG_IS_DEFAULT(TypeProfileWidth)) {
      FLAG_SET_DEFAULT(TypeProfileWidth, 8);
    }
    if (FLAG_IS_DEFAULT(TypeProfileLevel)) {
      FLAG_SET_DEFAULT(TypeProfileLevel, 0);
    }

    if (UseJVMCINativeLibrary) {
      // SVM compiled code requires more stack space
      if (FLAG_IS_DEFAULT(CompilerThreadStackSize)) {
        // Duplicate logic in the implementations of os::create_thread
        // so that we can then double the computed stack size. Once
        // the stack size requirements of SVM are better understood,
        // this logic can be pushed down into os::create_thread.
        int stack_size = CompilerThreadStackSize;
        if (stack_size == 0) {
          stack_size = VMThreadStackSize;
        }
        if (stack_size != 0) {
          FLAG_SET_DEFAULT(CompilerThreadStackSize, stack_size * 2);
        }
      }
    } else {
      // JVMCI needs values not less than defaults
      if (FLAG_IS_DEFAULT(ReservedCodeCacheSize)) {
        FLAG_SET_DEFAULT(ReservedCodeCacheSize, MAX2(64*M, ReservedCodeCacheSize));
      }
      if (FLAG_IS_DEFAULT(InitialCodeCacheSize)) {
        FLAG_SET_DEFAULT(InitialCodeCacheSize, MAX2(16*M, InitialCodeCacheSize));
      }
      if (FLAG_IS_DEFAULT(NewSizeThreadIncrease)) {
        FLAG_SET_DEFAULT(NewSizeThreadIncrease, MAX2(4*K, NewSizeThreadIncrease));
      }
      if (FLAG_IS_DEFAULT(Tier3DelayOn)) {
        // This effectively prevents the compile broker scheduling tier 2
        // (i.e., limited C1 profiling) compilations instead of tier 3
        // (i.e., full C1 profiling) compilations when the tier 4 queue
        // backs up (which is quite likely when using a non-AOT compiled JVMCI
        // compiler). The observation based on jargraal is that the downside
        // of skipping full profiling is much worse for performance than the
        // queue backing up.
        FLAG_SET_DEFAULT(Tier3DelayOn, 100000);
      }
    } // !UseJVMCINativeLibrary
  } // UseJVMCICompiler
}
#endif // INCLUDE_JVMCI

bool CompilerConfig::check_args_consistency(bool status) {
  // Check lower bounds of the code cache
  // Template Interpreter code is approximately 3X larger in debug builds.
  uint min_code_cache_size = CodeCacheMinimumUseSpace DEBUG_ONLY(* 3);
  if (ReservedCodeCacheSize < InitialCodeCacheSize) {
    jio_fprintf(defaultStream::error_stream(),
                "Invalid ReservedCodeCacheSize: %dK. Must be at least InitialCodeCacheSize=%dK.\n",
                ReservedCodeCacheSize/K, InitialCodeCacheSize/K);
    status = false;
  } else if (ReservedCodeCacheSize < min_code_cache_size) {
    jio_fprintf(defaultStream::error_stream(),
                "Invalid ReservedCodeCacheSize=%dK. Must be at least %uK.\n", ReservedCodeCacheSize/K,
                min_code_cache_size/K);
    status = false;
  } else if (ReservedCodeCacheSize > CODE_CACHE_SIZE_LIMIT) {
    // Code cache size larger than CODE_CACHE_SIZE_LIMIT is not supported.
    jio_fprintf(defaultStream::error_stream(),
                "Invalid ReservedCodeCacheSize=%dM. Must be at most %uM.\n", ReservedCodeCacheSize/M,
                CODE_CACHE_SIZE_LIMIT/M);
    status = false;
  } else if (NonNMethodCodeHeapSize < min_code_cache_size) {
    jio_fprintf(defaultStream::error_stream(),
                "Invalid NonNMethodCodeHeapSize=%dK. Must be at least %uK.\n", NonNMethodCodeHeapSize/K,
                min_code_cache_size/K);
    status = false;
  }

#ifdef _LP64
  if (!FLAG_IS_DEFAULT(CICompilerCount) && !FLAG_IS_DEFAULT(CICompilerCountPerCPU) && CICompilerCountPerCPU) {
    warning("The VM option CICompilerCountPerCPU overrides CICompilerCount.");
  }
#endif

  if (BackgroundCompilation && ReplayCompiles) {
    if (!FLAG_IS_DEFAULT(BackgroundCompilation)) {
      warning("BackgroundCompilation disabled due to ReplayCompiles option.");
    }
    FLAG_SET_CMDLINE(BackgroundCompilation, false);
  }

#ifdef COMPILER2
  if (PostLoopMultiversioning && !RangeCheckElimination) {
    if (!FLAG_IS_DEFAULT(PostLoopMultiversioning)) {
      warning("PostLoopMultiversioning disabled because RangeCheckElimination is disabled.");
    }
    FLAG_SET_CMDLINE(PostLoopMultiversioning, false);
  }
#endif // COMPILER2

  if (CompilerConfig::is_interpreter_only()) {
    if (UseCompiler) {
      if (!FLAG_IS_DEFAULT(UseCompiler)) {
        warning("UseCompiler disabled due to -Xint.");
      }
      FLAG_SET_CMDLINE(UseCompiler, false);
    }
    if (ProfileInterpreter) {
      if (!FLAG_IS_DEFAULT(ProfileInterpreter)) {
        warning("ProfileInterpreter disabled due to -Xint.");
      }
      FLAG_SET_CMDLINE(ProfileInterpreter, false);
    }
    if (TieredCompilation) {
      if (!FLAG_IS_DEFAULT(TieredCompilation)) {
        warning("TieredCompilation disabled due to -Xint.");
      }
      FLAG_SET_CMDLINE(TieredCompilation, false);
    }
#if INCLUDE_JVMCI
    if (EnableJVMCI) {
      if (!FLAG_IS_DEFAULT(EnableJVMCI) || !FLAG_IS_DEFAULT(UseJVMCICompiler)) {
        warning("JVMCI Compiler disabled due to -Xint.");
      }
      FLAG_SET_CMDLINE(EnableJVMCI, false);
      FLAG_SET_CMDLINE(UseJVMCICompiler, false);
    }
#endif
  } else {
#if INCLUDE_JVMCI
    status = status && JVMCIGlobals::check_jvmci_flags_are_consistent();
#endif
  }

  return status;
}

void CompilerConfig::ergo_initialize() {
#if !COMPILER1_OR_COMPILER2
  return;
#endif

  if (has_c1()) {
    if (!is_compilation_mode_selected()) {
#if defined(_WINDOWS) && !defined(_LP64)
      if (FLAG_IS_DEFAULT(NeverActAsServerClassMachine)) {
        FLAG_SET_ERGO(NeverActAsServerClassMachine, true);
      }
#endif
      if (NeverActAsServerClassMachine) {
        set_client_emulation_mode_flags();
      }
    } else if (!has_c2() && !is_jvmci_compiler()) {
      set_client_emulation_mode_flags();
    }
  }

  set_legacy_emulation_flags();
  set_compilation_policy_flags();

#if INCLUDE_JVMCI
  // Check that JVMCI supports selected GC.
  // Should be done after GCConfig::initialize() was called.
  JVMCIGlobals::check_jvmci_supported_gc();

  // Do JVMCI specific settings
  set_jvmci_specific_flags();
#endif

  if (FLAG_IS_DEFAULT(SweeperThreshold)) {
    if ((SweeperThreshold * ReservedCodeCacheSize / 100) > (1.2 * M)) {
      // Cap default SweeperThreshold value to an equivalent of 1.2 Mb
      FLAG_SET_ERGO(SweeperThreshold, (1.2 * M * 100) / ReservedCodeCacheSize);
    }
  }

  if (UseOnStackReplacement && !UseLoopCounter) {
    warning("On-stack-replacement requires loop counters; enabling loop counters");
    FLAG_SET_DEFAULT(UseLoopCounter, true);
  }

  if (ProfileInterpreter && CompilerConfig::is_c1_simple_only()) {
    if (!FLAG_IS_DEFAULT(ProfileInterpreter)) {
        warning("ProfileInterpreter disabled due to client emulation mode");
    }
    FLAG_SET_CMDLINE(ProfileInterpreter, false);
  }

#ifdef COMPILER2
  if (!EliminateLocks) {
    EliminateNestedLocks = false;
  }
  if (!Inline || !IncrementalInline) {
    IncrementalInline = false;
    IncrementalInlineMH = false;
    IncrementalInlineVirtual = false;
  }
#ifndef PRODUCT
  if (!IncrementalInline) {
    AlwaysIncrementalInline = false;
  }
  if (FLAG_IS_CMDLINE(PrintIdealGraph) && !PrintIdealGraph) {
    FLAG_SET_ERGO(PrintIdealGraphLevel, -1);
  }
#endif
  if (!UseTypeSpeculation && FLAG_IS_DEFAULT(TypeProfileLevel)) {
    // nothing to use the profiling, turn if off
    FLAG_SET_DEFAULT(TypeProfileLevel, 0);
  }
  if (!FLAG_IS_DEFAULT(OptoLoopAlignment) && FLAG_IS_DEFAULT(MaxLoopPad)) {
    FLAG_SET_DEFAULT(MaxLoopPad, OptoLoopAlignment-1);
  }
  if (FLAG_IS_DEFAULT(LoopStripMiningIterShortLoop)) {
    // blind guess
    LoopStripMiningIterShortLoop = LoopStripMiningIter / 10;
  }
#endif // COMPILER2
}

