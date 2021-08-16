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

#ifndef SHARE_COMPILER_COMPILERDEFINITIONS_HPP
#define SHARE_COMPILER_COMPILERDEFINITIONS_HPP

#include "compiler/compiler_globals.hpp"
#include "jvmci/jvmci_globals.hpp"
#include "memory/allocation.hpp"
#include "runtime/globals.hpp"

// The (closed set) of concrete compiler classes.
enum CompilerType {
  compiler_none,
  compiler_c1,
  compiler_c2,
  compiler_jvmci,
  compiler_number_of_types
};

extern const char* compilertype2name_tab[compiler_number_of_types];     // Map CompilerType to its name
inline const char* compilertype2name(CompilerType t) { return (uint)t < compiler_number_of_types ? compilertype2name_tab[t] : NULL; }

// Handy constants for deciding which compiler mode to use.
enum MethodCompilation {
  InvocationEntryBci   = -1,     // i.e., not a on-stack replacement compilation
  BeforeBci            = InvocationEntryBci,
  AfterBci             = -2,
  UnwindBci            = -3,
  AfterExceptionBci    = -4,
  UnknownBci           = -5,
  InvalidFrameStateBci = -6
};

// Enumeration to distinguish tiers of compilation
enum CompLevel {
  CompLevel_any               = -1,        // Used for querying the state
  CompLevel_all               = -1,        // Used for changing the state
  CompLevel_none              = 0,         // Interpreter
  CompLevel_simple            = 1,         // C1
  CompLevel_limited_profile   = 2,         // C1, invocation & backedge counters
  CompLevel_full_profile      = 3,         // C1, invocation & backedge counters + mdo
  CompLevel_full_optimization = 4          // C2 or JVMCI
};

class CompilationModeFlag : AllStatic {
  enum class Mode {
    NORMAL,
    QUICK_ONLY,
    HIGH_ONLY,
    HIGH_ONLY_QUICK_INTERNAL
  };
  static Mode _mode;
  static void print_error();
public:
  static bool initialize();
  static bool normal()                   { return _mode == Mode::NORMAL;                   }
  static bool quick_only()               { return _mode == Mode::QUICK_ONLY;               }
  static bool high_only()                { return _mode == Mode::HIGH_ONLY;                }
  static bool high_only_quick_internal() { return _mode == Mode::HIGH_ONLY_QUICK_INTERNAL; }

  static bool disable_intermediate()     { return high_only() || high_only_quick_internal(); }
  static bool quick_internal()           { return !high_only(); }

  static void set_high_only_quick_internal() { _mode = Mode::HIGH_ONLY_QUICK_INTERNAL; }
  static void set_quick_only()               { _mode = Mode::QUICK_ONLY;               }
  static void set_high_only()                { _mode = Mode::HIGH_ONLY;                }
};

inline bool is_c1_compile(int comp_level) {
  return comp_level > CompLevel_none && comp_level < CompLevel_full_optimization;
}

inline bool is_c2_compile(int comp_level) {
  return comp_level == CompLevel_full_optimization;
}

inline bool is_compile(int comp_level) {
  return is_c1_compile(comp_level) || is_c2_compile(comp_level);
}


// States of Restricted Transactional Memory usage.
enum RTMState {
  NoRTM      = 0x2, // Don't use RTM
  UseRTM     = 0x1, // Use RTM
  ProfileRTM = 0x0  // Use RTM with abort ratio calculation
};

#ifndef INCLUDE_RTM_OPT
#define INCLUDE_RTM_OPT 0
#endif
#if INCLUDE_RTM_OPT
#define RTM_OPT_ONLY(code) code
#else
#define RTM_OPT_ONLY(code)
#endif

class CompilerConfig : public AllStatic {
public:
  // Scale compile thresholds
  // Returns threshold scaled with CompileThresholdScaling
  static intx scaled_compile_threshold(intx threshold, double scale);
  static intx scaled_compile_threshold(intx threshold);

  // Returns freq_log scaled with CompileThresholdScaling
  static intx scaled_freq_log(intx freq_log, double scale);
  static intx scaled_freq_log(intx freq_log);

  static bool check_args_consistency(bool status);

  static void ergo_initialize();

  // Which compilers are baked in?
  constexpr static bool has_c1()     { return COMPILER1_PRESENT(true) NOT_COMPILER1(false); }
  constexpr static bool has_c2()     { return COMPILER2_PRESENT(true) NOT_COMPILER2(false); }
  constexpr static bool has_jvmci()  { return JVMCI_ONLY(true) NOT_JVMCI(false);            }
  constexpr static bool has_tiered() { return has_c1() && (has_c2() || has_jvmci());        }

  static bool is_jvmci_compiler()    { return JVMCI_ONLY(has_jvmci() && UseJVMCICompiler) NOT_JVMCI(false); }
  static bool is_jvmci()             { return JVMCI_ONLY(has_jvmci() && EnableJVMCI) NOT_JVMCI(false);      }
  static bool is_interpreter_only();

  // is_*_only() functions describe situations in which the JVM is in one way or another
  // forced to use a particular compiler or their combination. The constraint functions
  // deliberately ignore the fact that there may also be methods installed
  // through JVMCI (where the JVMCI compiler was invoked not through the broker). Be sure
  // to check for those (using is_jvmci()) in situations where it matters.
  //

  // Is the JVM in a configuration that permits only c1-compiled methods (level 1,2,3)?
  static bool is_c1_only() {
    if (!is_interpreter_only() && has_c1()) {
      const bool c1_only = !has_c2() && !is_jvmci_compiler();
      const bool tiered_degraded_to_c1_only = TieredCompilation && TieredStopAtLevel >= CompLevel_simple && TieredStopAtLevel < CompLevel_full_optimization;
      const bool c1_only_compilation_mode = CompilationModeFlag::quick_only();
      return c1_only || tiered_degraded_to_c1_only || c1_only_compilation_mode;
    }
    return false;
  }

  static bool is_c1_or_interpreter_only_no_jvmci() {
    assert(is_jvmci_compiler() && is_jvmci() || !is_jvmci_compiler(), "JVMCI compiler implies enabled JVMCI");
    return !is_jvmci() && (is_interpreter_only() || is_c1_only());
  }

  static bool is_c1_only_no_jvmci() {
    return is_c1_only() && !is_jvmci();
  }

  // Is the JVM in a configuration that permits only c1-compiled methods at level 1?
  static bool is_c1_simple_only() {
    if (is_c1_only()) {
      const bool tiered_degraded_to_level_1 = TieredCompilation && TieredStopAtLevel == CompLevel_simple;
      const bool c1_only_compilation_mode = CompilationModeFlag::quick_only();
      const bool tiered_off = !TieredCompilation;
      return tiered_degraded_to_level_1 || c1_only_compilation_mode || tiered_off;
    }
    return false;
  }

  static bool is_c2_enabled() {
    return has_c2() && !is_interpreter_only() && !is_c1_only() && !is_jvmci_compiler();
  }

  static bool is_jvmci_compiler_enabled() {
    return is_jvmci_compiler() && !is_interpreter_only() && !is_c1_only();
  }
  // Is the JVM in a configuration that permits only c2-compiled methods?
  static bool is_c2_only() {
    if (is_c2_enabled()) {
      const bool c2_only = !has_c1();
      // There is no JVMCI compiler to replace C2 in the broker, and the user (or ergonomics)
      // is forcing C1 off.
      const bool c2_only_compilation_mode = CompilationModeFlag::high_only();
      const bool tiered_off = !TieredCompilation;
      return c2_only || c2_only_compilation_mode || tiered_off;
    }
    return false;
  }

  // Is the JVM in a configuration that permits only jvmci-compiled methods?
  static bool is_jvmci_compiler_only() {
    if (is_jvmci_compiler_enabled()) {
      const bool jvmci_compiler_only = !has_c1();
      // JVMCI compiler replaced C2 and the user (or ergonomics) is forcing C1 off.
      const bool jvmci_only_compilation_mode = CompilationModeFlag::high_only();
      const bool tiered_off = !TieredCompilation;
      return jvmci_compiler_only || jvmci_only_compilation_mode || tiered_off;
    }
    return false;
  }

  static bool is_c2_or_jvmci_compiler_only() {
    return is_c2_only() || is_jvmci_compiler_only();
  }

  // Tiered is basically C1 & (C2 | JVMCI) minus all the odd cases with restrictions.
  static bool is_tiered() {
    assert(is_c1_simple_only() && is_c1_only() || !is_c1_simple_only(), "c1 simple mode must imply c1-only mode");
    return has_tiered() && !is_interpreter_only() && !is_c1_only() && !is_c2_or_jvmci_compiler_only();
  }

  static bool is_c1_enabled() {
    return has_c1() && !is_interpreter_only() && !is_c2_or_jvmci_compiler_only();
  }

  static bool is_c1_profiling() {
    const bool c1_only_profiling = is_c1_only() && !is_c1_simple_only();
    const bool tiered = is_tiered();
    return c1_only_profiling || tiered;
  }


  static bool is_c2_or_jvmci_compiler_enabled() {
    return is_c2_enabled() || is_jvmci_compiler_enabled();
  }


private:
  static bool is_compilation_mode_selected();
  static void set_compilation_policy_flags();
  static void set_jvmci_specific_flags();
  static void set_legacy_emulation_flags();
  static void set_client_emulation_mode_flags();
};

#endif // SHARE_COMPILER_COMPILERDEFINITIONS_HPP
