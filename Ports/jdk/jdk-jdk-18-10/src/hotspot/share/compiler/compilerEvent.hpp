/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef SHARE_COMPILER_COMPILEREVENT_HPP
#define SHARE_COMPILER_COMPILEREVENT_HPP

#include "jni.h"
#include "compiler/compilerDefinitions.hpp"
#include "memory/allocation.hpp"
#include "utilities/macros.hpp"
#include "utilities/ticks.hpp"

#if INCLUDE_JFR
#include "jfr/utilities/jfrTime.hpp"
#endif

class ciMethod;
template <typename>
class GrowableArray;
class Method;
class EventCompilation;
class EventCompilationFailure;
class EventCompilerInlining;
class EventCompilerPhase;
struct JfrStructCalleeMethod;

class CompilerEvent : AllStatic {
 public:
  static jlong ticksNow() {
    // Using Ticks for consistent usage outside JFR folder.
    JFR_ONLY(return JfrTime::is_ft_enabled() ? Ticks::now().ft_value() : Ticks::now().value();) NOT_JFR_RETURN_(0L);
  }

  class CompilationEvent : AllStatic {
   public:
    static void post(EventCompilation& event, int compile_id, CompilerType type, Method* method, int compile_level, bool success, bool is_osr, int code_size, int inlined_bytecodes) NOT_JFR_RETURN();
  };

  class CompilationFailureEvent : AllStatic {
   public:
    static void post(EventCompilationFailure& event, int compile_id, const char* reason) NOT_JFR_RETURN();
  };

  class PhaseEvent : AllStatic {
    friend class CompilerPhaseTypeConstant;
   public:

    // Gets a unique identifier for `phase_name`, computing and registering it first if necessary.
    // If `may_exist` is true, then current registrations are searched first. If false, then
    // there must not be an existing registration for `phase_name`.
    // If `use_strdup` is true, then `phase_name` is strdup'ed before registration.
    // If `sync` is true, then access to the registration table is synchronized.
    static int get_phase_id(const char* phase_name, bool may_exist, bool use_strdup, bool sync) NOT_JFR_RETURN_(-1);

    static void post(EventCompilerPhase& event, const Ticks& start_time, int phase, int compile_id, int level) NOT_JFR_RETURN();
    static void post(EventCompilerPhase& event, jlong start_time, int phase, int compile_id, int level) {
      JFR_ONLY(post(event, Ticks(start_time), phase, compile_id, level);)
    }
  };

  class InlineEvent : AllStatic {
    static void post(EventCompilerInlining& event, int compile_id, Method* caller, const JfrStructCalleeMethod& callee, bool success, const char* msg, int bci) NOT_JFR_RETURN();
   public:
    static void post(EventCompilerInlining& event, int compile_id, Method* caller, Method* callee, bool success, const char* msg, int bci) NOT_JFR_RETURN();
    static void post(EventCompilerInlining& event, int compile_id, Method* caller, ciMethod* callee, bool success, const char* msg, int bci) NOT_JFR_RETURN();
  };
};
#endif // SHARE_COMPILER_COMPILEREVENT_HPP
