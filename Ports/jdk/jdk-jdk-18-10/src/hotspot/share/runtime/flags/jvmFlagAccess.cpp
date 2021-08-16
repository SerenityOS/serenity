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

#include "precompiled.hpp"
#include "jfr/jfrEvents.hpp"
#include "memory/allocation.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/flags/jvmFlagAccess.hpp"
#include "runtime/flags/jvmFlagLimit.hpp"
#include "runtime/flags/jvmFlagConstraintsRuntime.hpp"
#include "runtime/os.hpp"
#include "utilities/macros.hpp"
#include "utilities/ostream.hpp"

template<typename T, typename EVENT>
static void trace_flag_changed(JVMFlag* flag, const T old_value, const T new_value, const JVMFlagOrigin origin) {
  EVENT e;
  e.set_name(flag->name());
  e.set_oldValue(old_value);
  e.set_newValue(new_value);
  e.set_origin(static_cast<u8>(origin));
  e.commit();
}

class FlagAccessImpl {
public:
  JVMFlag::Error set(JVMFlag* flag, void* value, JVMFlagOrigin origin) const {
    return set_impl(flag, value, origin);
  }

  virtual JVMFlag::Error set_impl(JVMFlag* flag, void* value, JVMFlagOrigin origin) const = 0;
  virtual JVMFlag::Error check_range(const JVMFlag* flag, bool verbose) const { return JVMFlag::SUCCESS; }
  virtual void print_range(outputStream* st, const JVMFlagLimit* range) const { ShouldNotReachHere(); }
  virtual void print_default_range(outputStream* st) const { ShouldNotReachHere(); }
  virtual JVMFlag::Error check_constraint(const JVMFlag* flag, void * func, bool verbose) const  { return JVMFlag::SUCCESS; }
};

template <typename T, typename EVENT>
class TypedFlagAccessImpl : public FlagAccessImpl {

public:
  JVMFlag::Error check_constraint_and_set(JVMFlag* flag, void* value_addr, JVMFlagOrigin origin, bool verbose) const {
    T value = *((T*)value_addr);
    const JVMTypedFlagLimit<T>* constraint = (const JVMTypedFlagLimit<T>*)JVMFlagLimit::get_constraint(flag);
    if (constraint != NULL && constraint->phase() <= static_cast<int>(JVMFlagLimit::validating_phase())) {
      JVMFlag::Error err = typed_check_constraint(constraint->constraint_func(), value, verbose);
      if (err != JVMFlag::SUCCESS) {
        return err;
      }
    }

    T old_value = flag->read<T>();
    trace_flag_changed<T, EVENT>(flag, old_value, value, origin);
    flag->write<T>(value);
    *((T*)value_addr) = old_value;
    flag->set_origin(origin);

    return JVMFlag::SUCCESS;
  }

  JVMFlag::Error check_constraint(const JVMFlag* flag, void * func, bool verbose) const  {
    return typed_check_constraint(func, flag->read<T>(), verbose);
  }

  virtual JVMFlag::Error typed_check_constraint(void * func, T value, bool verbose) const = 0;
};

class FlagAccessImpl_bool : public TypedFlagAccessImpl<bool, EventBooleanFlagChanged> {
public:
  JVMFlag::Error set_impl(JVMFlag* flag, void* value_addr, JVMFlagOrigin origin) const {
    bool verbose = JVMFlagLimit::verbose_checks_needed();
    return TypedFlagAccessImpl<bool, EventBooleanFlagChanged>
               ::check_constraint_and_set(flag, value_addr, origin, verbose);
  }

  JVMFlag::Error typed_check_constraint(void* func, bool value, bool verbose) const {
    return ((JVMFlagConstraintFunc_bool)func)(value, verbose);
  }
};

template <typename T, typename EVENT>
class RangedFlagAccessImpl : public TypedFlagAccessImpl<T, EVENT> {
public:
  virtual JVMFlag::Error set_impl(JVMFlag* flag, void* value_addr, JVMFlagOrigin origin) const {
    T value = *((T*)value_addr);
    bool verbose = JVMFlagLimit::verbose_checks_needed();

    const JVMTypedFlagLimit<T>* range = (const JVMTypedFlagLimit<T>*)JVMFlagLimit::get_range(flag);
    if (range != NULL) {
      if ((value < range->min()) || (value > range->max())) {
        range_error(flag->name(), value, range->min(), range->max(), verbose);
        return JVMFlag::OUT_OF_BOUNDS;
      }
    }

    return TypedFlagAccessImpl<T, EVENT>::check_constraint_and_set(flag, value_addr, origin, verbose);
  }

  virtual JVMFlag::Error check_range(const JVMFlag* flag, bool verbose) const {
    const JVMTypedFlagLimit<T>* range = (const JVMTypedFlagLimit<T>*)JVMFlagLimit::get_range(flag);
    if (range != NULL) {
      T value = flag->read<T>();
      if ((value < range->min()) || (value > range->max())) {
        range_error(flag->name(), value, range->min(), range->max(), verbose);
        return JVMFlag::OUT_OF_BOUNDS;
      }
    }
    return JVMFlag::SUCCESS;
  }

  virtual void print_range(outputStream* st, const JVMFlagLimit* range) const {
    const JVMTypedFlagLimit<T>* r = (const JVMTypedFlagLimit<T>*)range;
    print_range_impl(st, r->min(), r->max());
  }

  virtual void range_error(const char* name, T value, T min, T max, bool verbose) const = 0;
  virtual void print_range_impl(outputStream* st, T min, T max) const = 0;
};

class FlagAccessImpl_int : public RangedFlagAccessImpl<int, EventIntFlagChanged> {
public:
  void range_error(const char* name, int value, int min, int max, bool verbose) const {
    JVMFlag::printError(verbose,
                        "int %s=%d is outside the allowed range "
                        "[ %d ... %d ]\n",
                        name, value, min, max);
  }
  JVMFlag::Error typed_check_constraint(void* func, int value, bool verbose) const {
    return ((JVMFlagConstraintFunc_int)func)(value, verbose);
  }
  void print_range_impl(outputStream* st, int min, int max) const {
    st->print("[ %-25d ... %25d ]", min, max);
  }
  void print_default_range(outputStream* st) const {
    st->print("[ " INT32_FORMAT_W(-25) " ... " INT32_FORMAT_W(25) " ]", INT_MIN, INT_MAX);
  }
};

class FlagAccessImpl_uint : public RangedFlagAccessImpl<uint, EventUnsignedIntFlagChanged> {
public:
  void range_error(const char* name, uint value, uint min, uint max, bool verbose) const {
    JVMFlag::printError(verbose,
                        "uint %s=%u is outside the allowed range "
                        "[ %u ... %u ]\n",
                        name, value, min, max);
  }
  JVMFlag::Error typed_check_constraint(void* func, uint value, bool verbose) const {
    return ((JVMFlagConstraintFunc_uint)func)(value, verbose);
  }
  void print_range_impl(outputStream* st, uint min, uint max) const {
    st->print("[ %-25u ... %25u ]", min, max);
  }
  void print_default_range(outputStream* st) const {
    st->print("[ " UINT32_FORMAT_W(-25) " ... " UINT32_FORMAT_W(25) " ]", 0, UINT_MAX);
  }
};

class FlagAccessImpl_intx : public RangedFlagAccessImpl<intx, EventLongFlagChanged> {
public:
  void range_error(const char* name, intx value, intx min, intx max, bool verbose) const {
    JVMFlag::printError(verbose,
                        "intx %s=" INTX_FORMAT " is outside the allowed range "
                        "[ " INTX_FORMAT " ... " INTX_FORMAT " ]\n",
                        name, value, min, max);
  }
  JVMFlag::Error typed_check_constraint(void* func, intx value, bool verbose) const {
    return ((JVMFlagConstraintFunc_intx)func)(value, verbose);
  }
  void print_range_impl(outputStream* st, intx min, intx max) const {
    st->print("[ " INTX_FORMAT_W(-25) " ... " INTX_FORMAT_W(25) " ]", min, max);
  }
  void print_default_range(outputStream* st) const {
    st->print("[ " INTX_FORMAT_W(-25) " ... " INTX_FORMAT_W(25) " ]", min_intx, max_intx);
  }
};

class FlagAccessImpl_uintx : public RangedFlagAccessImpl<uintx, EventUnsignedLongFlagChanged> {
public:
  void range_error(const char* name, uintx value, uintx min, uintx max, bool verbose) const {
    JVMFlag::printError(verbose,
                        "uintx %s=" UINTX_FORMAT " is outside the allowed range "
                        "[ " UINTX_FORMAT " ... " UINTX_FORMAT " ]\n",
                        name, value, min, max);
  }
  JVMFlag::Error typed_check_constraint(void* func, uintx value, bool verbose) const {
    return ((JVMFlagConstraintFunc_uintx)func)(value, verbose);
  }
  void print_range_impl(outputStream* st, uintx min, uintx max) const {
    st->print("[ " UINTX_FORMAT_W(-25) " ... " UINTX_FORMAT_W(25) " ]", min, max);
  }
  void print_default_range(outputStream* st) const {
    st->print("[ " UINTX_FORMAT_W(-25) " ... " UINTX_FORMAT_W(25) " ]", uintx(0), max_uintx);
  }
};

class FlagAccessImpl_uint64_t : public RangedFlagAccessImpl<uint64_t, EventUnsignedLongFlagChanged> {
public:
  void range_error(const char* name, uint64_t value, uint64_t min, uint64_t max, bool verbose) const {
    JVMFlag::printError(verbose,
                        "uint64_t %s=" UINT64_FORMAT " is outside the allowed range "
                        "[ " UINT64_FORMAT " ... " UINT64_FORMAT " ]\n",
                        name, value, min, max);
  }
  JVMFlag::Error typed_check_constraint(void* func, uint64_t value, bool verbose) const {
    return ((JVMFlagConstraintFunc_uint64_t)func)(value, verbose);
  }
  void print_range_impl(outputStream* st, uint64_t min, uint64_t max) const {
    st->print("[ " UINT64_FORMAT_W(-25) " ... " UINT64_FORMAT_W(25) " ]", min, max);
  }
  void print_default_range(outputStream* st) const {
    st->print("[ " UINT64_FORMAT_W(-25) " ... " UINT64_FORMAT_W(25) " ]", uint64_t(0), uint64_t(max_juint));
  }
};

class FlagAccessImpl_size_t : public RangedFlagAccessImpl<size_t, EventUnsignedLongFlagChanged> {
public:
  void range_error(const char* name, size_t value, size_t min, size_t max, bool verbose) const {
    JVMFlag::printError(verbose,
                        "size_t %s=" SIZE_FORMAT " is outside the allowed range "
                        "[ " SIZE_FORMAT " ... " SIZE_FORMAT " ]\n",
                        name, value, min, max);
  }
  JVMFlag::Error typed_check_constraint(void* func, size_t value, bool verbose) const {
    return ((JVMFlagConstraintFunc_size_t)func)(value, verbose);
  }
  void print_range_impl(outputStream* st, size_t min, size_t max) const {
    st->print("[ " SIZE_FORMAT_W(-25) " ... " SIZE_FORMAT_W(25) " ]", min, max);
  }
  void print_default_range(outputStream* st) const {
    st->print("[ " SIZE_FORMAT_W(-25) " ... " SIZE_FORMAT_W(25) " ]", size_t(0), size_t(SIZE_MAX));
  }
};

class FlagAccessImpl_double : public RangedFlagAccessImpl<double, EventDoubleFlagChanged> {
public:
  void range_error(const char* name, double value, double min, double max, bool verbose) const {
    JVMFlag::printError(verbose,
                          "double %s=%f is outside the allowed range "
                          "[ %f ... %f ]\n",
                        name, value, min, max);
  }
  JVMFlag::Error typed_check_constraint(void* func, double value, bool verbose) const {
    return ((JVMFlagConstraintFunc_double)func)(value, verbose);
  }
  void print_range_impl(outputStream* st, double min, double max) const {
    st->print("[ %-25.3f ... %25.3f ]", min, max);
  }
  void print_default_range(outputStream* st) const {
    st->print("[ %-25.3f ... %25.3f ]", DBL_MIN, DBL_MAX);
  }
};

#define FLAG_ACCESS_IMPL_INIT(t) \
  static FlagAccessImpl_ ## t   flag_access_ ## t;

#define FLAG_ACCESS_IMPL_ADDR(t) \
  &flag_access_ ## t,

JVM_FLAG_NON_STRING_TYPES_DO(FLAG_ACCESS_IMPL_INIT)

static const FlagAccessImpl* flag_accesss[JVMFlag::NUM_FLAG_TYPES] = {
  JVM_FLAG_NON_STRING_TYPES_DO(FLAG_ACCESS_IMPL_ADDR)
  // ccstr and ccstrlist have special setter
};

inline const FlagAccessImpl* JVMFlagAccess::access_impl(const JVMFlag* flag) {
  int type = flag->type();
  int max = (int)(sizeof(flag_accesss)/sizeof(flag_accesss[0]));
  assert(type >= 0 && type < max , "sanity");

  return flag_accesss[type];
}

JVMFlag::Error JVMFlagAccess::set_impl(JVMFlag* flag, void* value, JVMFlagOrigin origin) {
  if (flag->is_ccstr()) {
    return set_ccstr(flag, (ccstr*)value, origin);
  } else {
    return access_impl(flag)->set(flag, value, origin);
  }
}

JVMFlag::Error JVMFlagAccess::set_ccstr(JVMFlag* flag, ccstr* value, JVMFlagOrigin origin) {
  if (flag == NULL) return JVMFlag::INVALID_FLAG;
  if (!flag->is_ccstr()) return JVMFlag::WRONG_FORMAT;
  ccstr old_value = flag->get_ccstr();
  trace_flag_changed<ccstr, EventStringFlagChanged>(flag, old_value, *value, origin);
  char* new_value = NULL;
  if (*value != NULL) {
    new_value = os::strdup_check_oom(*value);
  }
  flag->set_ccstr(new_value);
  if (!flag->is_default() && old_value != NULL) {
    // Old value is heap allocated so free it.
    FREE_C_HEAP_ARRAY(char, old_value);
  }
  // Unlike the other APIs, the old vale is NOT returned, so the caller won't need to free it.
  // The callers typically don't care what the old value is.
  // If the caller really wants to know the old value, read it (and make a copy if necessary)
  // before calling this API.
  *value = NULL;
  flag->set_origin(origin);
  return JVMFlag::SUCCESS;
}

// This is called by the FLAG_SET_XXX macros.
JVMFlag::Error JVMFlagAccess::set_or_assert(JVMFlagsEnum flag_enum, int type_enum, void* value, JVMFlagOrigin origin) {
  JVMFlag* flag = JVMFlag::flag_from_enum(flag_enum);
  if (type_enum == JVMFlag::TYPE_ccstr || type_enum == JVMFlag::TYPE_ccstrlist) {
    assert(flag->is_ccstr(), "must be");
    return set_ccstr(flag, (ccstr*)value, origin);
  } else {
    assert(flag->type() == type_enum, "wrong flag type");
    return set_impl(flag, value, origin);
  }
}

JVMFlag::Error JVMFlagAccess::check_range(const JVMFlag* flag, bool verbose) {
  return access_impl(flag)->check_range(flag, verbose);
}

JVMFlag::Error JVMFlagAccess::check_constraint(const JVMFlag* flag, void * func, bool verbose) {
  const int type_enum = flag->type();
  if (type_enum == JVMFlag::TYPE_ccstr || type_enum == JVMFlag::TYPE_ccstrlist) {
    // ccstr and ccstrlist are the same type.
    return ((JVMFlagConstraintFunc_ccstr)func)(flag->get_ccstr(), verbose);
  }

  return access_impl(flag)->check_constraint(flag, func, verbose);
}

void JVMFlagAccess::print_range(outputStream* st, const JVMFlag* flag, const JVMFlagLimit* range) {
  return access_impl(flag)->print_range(st, range);
}

void JVMFlagAccess::print_range(outputStream* st, const JVMFlag* flag) {
  const JVMFlagLimit* range = JVMFlagLimit::get_range(flag);
  if (range != NULL) {
    print_range(st, flag, range);
  } else {
    const JVMFlagLimit* limit = JVMFlagLimit::get_constraint(flag);
    if (limit != NULL) {
      void* func = limit->constraint_func();

      // Two special cases where the lower limit of the range is defined by an os:: function call
      // and cannot be initialized at compile time with constexpr.
      if (func == (void*)VMPageSizeConstraintFunc) {
        uintx min = (uintx)os::vm_page_size();
        uintx max = max_uintx;

        JVMTypedFlagLimit<uintx> tmp(0, min, max);
        access_impl(flag)->print_range(st, &tmp);
      } else if (func == (void*)NUMAInterleaveGranularityConstraintFunc) {
        size_t min = os::vm_allocation_granularity();
        size_t max = NOT_LP64(2*G) LP64_ONLY(8192*G);

        JVMTypedFlagLimit<size_t> tmp(0, min, max);
        access_impl(flag)->print_range(st, &tmp);
      } else {
        access_impl(flag)->print_default_range(st);
      }
    } else {
      st->print("[                           ...                           ]");
    }
  }
}
