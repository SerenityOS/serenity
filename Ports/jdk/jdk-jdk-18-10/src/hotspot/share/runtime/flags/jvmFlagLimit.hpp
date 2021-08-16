/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_FLAGS_JVMFLAGLIMIT_HPP
#define SHARE_RUNTIME_FLAGS_JVMFLAGLIMIT_HPP

#include "runtime/flags/jvmFlag.hpp"

class outputStream;
template <typename T> class JVMTypedFlagLimit;

enum class JVMFlagConstraintPhase : int {
  // Will be validated during argument processing (Arguments::parse_argument).
  AtParse         = 0,
  // Will be validated inside Threads::create_vm(), right after Arguments::apply_ergo().
  AfterErgo       = 1,
  // Will be validated inside universe_init(), right after Metaspace::global_initialize().
  AfterMemoryInit = 2
};


typedef JVMFlag::Error (*JVMFlagConstraintFunc_bool)(bool value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_int)(int value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_intx)(intx value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_uint)(uint value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_uintx)(uintx value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_uint64_t)(uint64_t value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_size_t)(size_t value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_double)(double value, bool verbose);
typedef JVMFlag::Error (*JVMFlagConstraintFunc_ccstr)(ccstr value, bool verbose);

template <typename T> class JVMTypedFlagLimit;

// A JVMFlagLimit is created for each JVMFlag that has a range() and/or constraint() in its declaration in
// the globals_xxx.hpp file.
//
// To query the range information of a JVMFlag:
//     JVMFlagLimit::get_range(JVMFlag*)
//     JVMFlagLimit::get_range_at(int flag_enum)
// If the given flag doesn't have a range, NULL is returned.
//
// To query the constraint information of a JVMFlag:
//     JVMFlagLimit::get_constraint(JVMFlag*)
//     JVMFlagLimit::get_constraint_at(int flag_enum)
// If the given flag doesn't have a constraint, NULL is returned.

class JVMFlagLimit {
  short _constraint_func;
  char  _phase;
  char  _kind;

#ifdef ASSERT
  int   _type_enum;
#endif

  static const JVMFlagLimit* const* flagLimits;
  static JVMFlagsEnum _last_checked;
  static JVMFlagConstraintPhase _validating_phase;

protected:
  static constexpr int HAS_RANGE = 1;
  static constexpr int HAS_CONSTRAINT = 2;

private:
  static const JVMFlagLimit* get_kind_at(JVMFlagsEnum flag_enum, int required_kind) {
    const JVMFlagLimit* limit = at(flag_enum);
    if (limit != NULL && (limit->_kind & required_kind) != 0) {
      _last_checked = flag_enum;
      return limit;
    } else {
      return NULL;
    }
  }

  static const JVMFlagLimit* at(JVMFlagsEnum flag_enum) {
    JVMFlag::assert_valid_flag_enum(flag_enum);
    return flagLimits[static_cast<int>(flag_enum)];
  }

public:
  void* constraint_func() const;
  char phase() const { return _phase; }
  char kind()  const { return _kind; }

  constexpr JVMFlagLimit(int type_enum, short func, short phase, short kind)
    : _constraint_func(func), _phase(phase), _kind(kind) DEBUG_ONLY(COMMA _type_enum(type_enum)) {}

  static const JVMFlagLimit* get_range(const JVMFlag* flag) {
    return get_range_at(flag->flag_enum());
  }
  static const JVMFlagLimit* get_range_at(JVMFlagsEnum flag_enum) {
    return get_kind_at(flag_enum, HAS_RANGE);
  }

  static const JVMFlagLimit* get_constraint(const JVMFlag* flag) {
    return get_constraint_at(flag->flag_enum());
  }
  static const JVMFlagLimit* get_constraint_at(JVMFlagsEnum flag_enum) {
    return get_kind_at(flag_enum, HAS_CONSTRAINT);
  }

  static const JVMFlag* last_checked_flag();

  // Is the current value of each JVM flag within the allowed range (if specified)
  static bool check_all_ranges();
  void print_range(outputStream* st, const JVMFlag* flag) const;

  // Does the current value of each JVM flag satisfy the specified constraint
  static bool check_all_constraints(JVMFlagConstraintPhase phase);

  // If range/constraint checks fail, print verbose error messages only if we are parsing
  // arguments from the command-line. Silently ignore any invalid values that are
  // set programmatically via FLAG_SET_ERGO, etc.
  static bool verbose_checks_needed() {
    return _validating_phase == JVMFlagConstraintPhase::AtParse;
  }

  static JVMFlagConstraintPhase validating_phase() { return _validating_phase; }

  template <typename T>
  const JVMTypedFlagLimit<T>* cast() const;
};

enum ConstraintMarker {
  next_two_args_are_constraint,
};

template <typename T>
class JVMTypedFlagLimit : public JVMFlagLimit {
  const T _min;
  const T _max;

public:
  // dummy - no range or constraint. This object will not be emitted into the .o file
  // because we declare it as "const" but has no reference to it.
  constexpr JVMTypedFlagLimit(int type_enum) :
  JVMFlagLimit(0, 0, 0, 0), _min(0), _max(0) {}

  // range only
  constexpr JVMTypedFlagLimit(int type_enum, T min, T max) :
    JVMFlagLimit(type_enum, 0, 0, HAS_RANGE), _min(min), _max(max) {}

  // constraint only
  constexpr JVMTypedFlagLimit(int type_enum, ConstraintMarker dummy2, short func, int phase) :
    JVMFlagLimit(type_enum, func, phase, HAS_CONSTRAINT), _min(0), _max(0) {}

  // range and constraint
  constexpr JVMTypedFlagLimit(int type_enum, T min, T max, ConstraintMarker dummy2, short func, int phase)  :
    JVMFlagLimit(type_enum, func, phase, HAS_RANGE | HAS_CONSTRAINT), _min(min), _max(max) {}

  // constraint and range
  constexpr JVMTypedFlagLimit(int type_enum, ConstraintMarker dummy2, short func, int phase, T min, T max)  :
    JVMFlagLimit(type_enum, func, phase, HAS_RANGE | HAS_CONSTRAINT), _min(min), _max(max) {}

  T min() const { return _min; }
  T max() const { return _max; }
};

template <typename T>
const JVMTypedFlagLimit<T>* JVMFlagLimit::cast() const {
  DEBUG_ONLY(JVMFlag::assert_compatible_type<T>(_type_enum));
  return static_cast<const JVMTypedFlagLimit<T>*>(this);
}

#endif // SHARE_RUNTIME_FLAGS_JVMFLAGLIMIT_HPP
