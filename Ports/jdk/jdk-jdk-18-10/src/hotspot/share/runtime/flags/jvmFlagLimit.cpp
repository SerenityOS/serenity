/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.inline.hpp"
#include "gc/shared/jvmFlagConstraintsGC.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/flags/jvmFlagAccess.hpp"
#include "runtime/flags/jvmFlagLimit.hpp"
#include "runtime/flags/jvmFlagConstraintsCompiler.hpp"
#include "runtime/flags/jvmFlagConstraintsRuntime.hpp"
#include "runtime/globals_extension.hpp"
#include "gc/shared/referenceProcessor.hpp"
#include "oops/markWord.hpp"
#include "runtime/task.hpp"

//----------------------------------------------------------------------
// Build flagLimitTable[]

#define CONSTRAINT_ENUM(func)         constraint_enum_ ## func
#define CONSTRAINT_ENUM_(type, func)  CONSTRAINT_ENUM(func),
#define CONSTRAINT_FUNC(type, func)   (void*)&func,

enum JVMFlagConstraintsEnum : int {
  ALL_CONSTRAINTS(CONSTRAINT_ENUM_)
  NUM_JVMFlagConstraintsEnum
};

static void* const flagConstraintTable[NUM_JVMFlagConstraintsEnum] = {
  ALL_CONSTRAINTS(CONSTRAINT_FUNC)
};

void* JVMFlagLimit::constraint_func() const {
  int i = _constraint_func;
  assert(0 <= i && i < NUM_JVMFlagConstraintsEnum, "sanity");
  return flagConstraintTable[i];
}

struct DummyLimit {
  char dummy;
  constexpr DummyLimit(...) : dummy() {}
};

template <typename T>
class LimitGetter {
public:
  // These functions return NULL for develop flags in a PRODUCT build
  static constexpr const JVMFlagLimit* no_limit(...) {
    return NULL;
  }

  // This is for flags that have neither range no constraint. We don't need the JVMFlagLimit struct.
  static constexpr const JVMFlagLimit* get_limit(const JVMTypedFlagLimit<T>* p, int dummy) {
    return NULL;
  }

  static constexpr const JVMFlagLimit* get_limit(const JVMTypedFlagLimit<T>* p, int dummy, T min, T max) {
    return p;
  }
  static constexpr const JVMFlagLimit* get_limit(const JVMTypedFlagLimit<T>* p, int dummy, ConstraintMarker dummy2, short func, int phase) {
    return p;
  }
  static constexpr const JVMFlagLimit* get_limit(const JVMTypedFlagLimit<T>* p, int dummy, T min, T max, ConstraintMarker dummy2, short func, int phase) {
    return p;
  }
  static constexpr const JVMFlagLimit* get_limit(const JVMTypedFlagLimit<T>* p, int dummy, ConstraintMarker dummy2, short func, int phase, T min, T max) {
    return p;
  }
};

//           macro body starts here -------------------+
//                                                     |
//                                                     v
#define FLAG_LIMIT_DEFINE(      type, name, ...)       ); constexpr JVMTypedFlagLimit<type> limit_##name(JVMFlag::TYPE_##type
#define FLAG_LIMIT_DEFINE_DUMMY(type, name, ...)       ); constexpr DummyLimit nolimit_##name(0
#define FLAG_LIMIT_PTR(         type, name, ...)       ), LimitGetter<type>::get_limit(&limit_##name, 0
#define FLAG_LIMIT_PTR_NONE(    type, name, ...)       ), LimitGetter<type>::no_limit(0
#define APPLY_FLAG_RANGE(...)                          , __VA_ARGS__
#define APPLY_FLAG_CONSTRAINT(func, phase)             , next_two_args_are_constraint, (short)CONSTRAINT_ENUM(func), int(JVMFlagConstraintPhase::phase)

constexpr JVMTypedFlagLimit<int> limit_dummy
(
#ifdef PRODUCT
 ALL_FLAGS(FLAG_LIMIT_DEFINE_DUMMY,
           FLAG_LIMIT_DEFINE_DUMMY,
           FLAG_LIMIT_DEFINE,
           FLAG_LIMIT_DEFINE,
           FLAG_LIMIT_DEFINE_DUMMY,
           APPLY_FLAG_RANGE,
           APPLY_FLAG_CONSTRAINT)
#else
 ALL_FLAGS(FLAG_LIMIT_DEFINE,
           FLAG_LIMIT_DEFINE,
           FLAG_LIMIT_DEFINE,
           FLAG_LIMIT_DEFINE,
           FLAG_LIMIT_DEFINE,
           APPLY_FLAG_RANGE,
           APPLY_FLAG_CONSTRAINT)
#endif
);

static constexpr const JVMFlagLimit* const flagLimitTable[1 + NUM_JVMFlagsEnum] = {
  // Because FLAG_LIMIT_PTR must start with an "),", we have to place a dummy element here.
  LimitGetter<int>::get_limit(NULL, 0

#ifdef PRODUCT
  ALL_FLAGS(FLAG_LIMIT_PTR_NONE,
            FLAG_LIMIT_PTR_NONE,
            FLAG_LIMIT_PTR,
            FLAG_LIMIT_PTR,
            FLAG_LIMIT_PTR_NONE,
            APPLY_FLAG_RANGE,
            APPLY_FLAG_CONSTRAINT)
#else
  ALL_FLAGS(FLAG_LIMIT_PTR,
            FLAG_LIMIT_PTR,
            FLAG_LIMIT_PTR,
            FLAG_LIMIT_PTR,
            FLAG_LIMIT_PTR,
            APPLY_FLAG_RANGE,
            APPLY_FLAG_CONSTRAINT)
#endif
  )
};

JVMFlagsEnum JVMFlagLimit::_last_checked = INVALID_JVMFlagsEnum;
JVMFlagConstraintPhase JVMFlagLimit::_validating_phase = JVMFlagConstraintPhase::AtParse;

const JVMFlagLimit* const* JVMFlagLimit::flagLimits = &flagLimitTable[1]; // excludes dummy

const JVMFlag* JVMFlagLimit::last_checked_flag() {
  if (_last_checked != INVALID_JVMFlagsEnum) {
    return JVMFlag::flag_from_enum(_last_checked);
  } else {
    return NULL;
  }
}

bool JVMFlagLimit::check_all_ranges() {
  bool status = true;
  for (int i = 0; i < NUM_JVMFlagsEnum; i++) {
    JVMFlagsEnum flag_enum = static_cast<JVMFlagsEnum>(i);
    if (get_range_at(flag_enum) != NULL &&
        JVMFlagAccess::check_range(JVMFlag::flag_from_enum(flag_enum), true) != JVMFlag::SUCCESS) {
      status = false;
    }
  }
  return status;
}

// Check constraints for specific constraint phase.
bool JVMFlagLimit::check_all_constraints(JVMFlagConstraintPhase phase) {
  guarantee(phase > _validating_phase, "Constraint check is out of order.");
  _validating_phase = phase;

  bool status = true;
  for (int i = 0; i < NUM_JVMFlagsEnum; i++) {
    JVMFlagsEnum flag_enum = static_cast<JVMFlagsEnum>(i);
    const JVMFlagLimit* constraint = get_constraint_at(flag_enum);
    if (constraint != NULL && constraint->phase() == static_cast<int>(phase) &&
        JVMFlagAccess::check_constraint(JVMFlag::flag_from_enum(flag_enum),
                                        constraint->constraint_func(), true) != JVMFlag::SUCCESS) {
      status = false;
    }
  }
  return status;
}

void JVMFlagLimit::print_range(outputStream* st, const JVMFlag* flag) const {
  JVMFlagAccess::print_range(st, flag, this);
}
