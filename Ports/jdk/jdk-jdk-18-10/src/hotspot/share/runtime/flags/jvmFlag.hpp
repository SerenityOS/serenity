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

#ifndef SHARE_RUNTIME_FLAGS_JVMFLAG_HPP
#define SHARE_RUNTIME_FLAGS_JVMFLAG_HPP

#include "utilities/globalDefinitions.hpp"
#include "utilities/enumIterator.hpp"
#include "utilities/macros.hpp"
#include "utilities/vmEnums.hpp"
#include <type_traits>

class outputStream;

enum class JVMFlagOrigin : int {
  // This is the value returned by JVMFlag::get_origin(). It records who
  // has most recently changed the value of a JVMFlag. DEFAULT means that the
  // flag was never changed, or was most recently changed by FLAG_SET_DEFAULT.
  DEFAULT          = 0,
  COMMAND_LINE     = 1,
  ENVIRON_VAR      = 2,
  CONFIG_FILE      = 3,
  MANAGEMENT       = 4,
  ERGONOMIC        = 5,
  ATTACH_ON_DEMAND = 6,
  INTERNAL         = 7,
  JIMAGE_RESOURCE  = 8,
};

ENUMERATOR_RANGE(JVMFlagOrigin, JVMFlagOrigin::DEFAULT, JVMFlagOrigin::JIMAGE_RESOURCE)

class JVMFlag {
  friend class VMStructs;

public:
  enum Flags : int {
    VALUE_ORIGIN_BITS = 4,
    VALUE_ORIGIN_MASK = right_n_bits(VALUE_ORIGIN_BITS),

    // flag kind
    KIND_PRODUCT            = 1 << 4,
    KIND_MANAGEABLE         = 1 << 5,
    KIND_DIAGNOSTIC         = 1 << 6,
    KIND_EXPERIMENTAL       = 1 << 7,
    KIND_NOT_PRODUCT        = 1 << 8,
    KIND_DEVELOP            = 1 << 9,
    KIND_PLATFORM_DEPENDENT = 1 << 10,
    KIND_C1                 = 1 << 11,
    KIND_C2                 = 1 << 12,
    KIND_ARCH               = 1 << 13,
    KIND_LP64_PRODUCT       = 1 << 14,
    KIND_JVMCI              = 1 << 15,

    // Note the difference:
    // f->get_origin() == COMMAND_LINE
    //    f was mostly recently set by the command-line
    // f->_flags & WAS_SET_ON_COMMAND_LINE
    //    f was specified on the command-line (but may have since been updated by
    //    someone else like FLAG_SET_ERGO)
    WAS_SET_ON_COMMAND_LINE = 1 << 17,

    KIND_MASK = ~(VALUE_ORIGIN_MASK | WAS_SET_ON_COMMAND_LINE)
  };

  enum Error {
    // no error
    SUCCESS = 0,
    // flag name is missing
    MISSING_NAME,
    // flag value is missing
    MISSING_VALUE,
    // error parsing the textual form of the value
    WRONG_FORMAT,
    // flag is not writable
    NON_WRITABLE,
    // flag value is outside of its bounds
    OUT_OF_BOUNDS,
    // flag value violates its constraint
    VIOLATES_CONSTRAINT,
    // there is no flag with the given name
    INVALID_FLAG,
    // the flag can only be set only on command line during invocation of the VM
    COMMAND_LINE_ONLY,
    // the flag may only be set once
    SET_ONLY_ONCE,
    // the flag is not writable in this combination of product/debug build
    CONSTANT,
    // other, unspecified error related to setting the flag
    ERR_OTHER
  };

  enum MsgType {
    NONE = 0,
    DIAGNOSTIC_FLAG_BUT_LOCKED,
    EXPERIMENTAL_FLAG_BUT_LOCKED,
    DEVELOPER_FLAG_BUT_PRODUCT_BUILD,
    NOTPRODUCT_FLAG_BUT_PRODUCT_BUILD
  };

#define JVM_FLAG_NON_STRING_TYPES_DO(f) \
    f(bool) \
    f(int) \
    f(uint) \
    f(intx) \
    f(uintx) \
    f(uint64_t) \
    f(size_t) \
    f(double)

#define JVM_FLAG_TYPE_DECLARE(t) \
  TYPE_ ## t,

  enum FlagType : int {
    JVM_FLAG_NON_STRING_TYPES_DO(JVM_FLAG_TYPE_DECLARE)
    // The two string types are a bit irregular: is_ccstr() returns true for both types.
    TYPE_ccstr,
    TYPE_ccstrlist,
    NUM_FLAG_TYPES
  };

private:
  void* _addr;
  const char* _name;
  Flags _flags;
  int   _type;

  NOT_PRODUCT(const char* _doc;)

public:
  // points to all Flags static array
  static JVMFlag* flags;

  // number of flags
  static size_t numFlags;

private:
  static JVMFlag* find_flag(const char* name, size_t length, bool allow_locked, bool return_flag);

public:
  constexpr JVMFlag() : _addr(), _name(), _flags(), _type() NOT_PRODUCT(COMMA _doc()) {}

  constexpr JVMFlag(int flag_enum, FlagType type, const char* name,
                    void* addr, int flags, int extra_flags, const char* doc);

  constexpr JVMFlag(int flag_enum,  FlagType type, const char* name,
                    void* addr, int flags, const char* doc);

  static JVMFlag* find_flag(const char* name) {
    return find_flag(name, strlen(name), false, false);
  }
  static JVMFlag* find_declared_flag(const char* name, size_t length) {
    return find_flag(name, length, true, true);
  }
  static JVMFlag* find_declared_flag(const char* name) {
    return find_declared_flag(name, strlen(name));
  }

  static JVMFlag* fuzzy_match(const char* name, size_t length, bool allow_locked = false);

  static void assert_valid_flag_enum(JVMFlagsEnum i) NOT_DEBUG_RETURN;
  static void check_all_flag_declarations() NOT_DEBUG_RETURN;

  inline JVMFlagsEnum flag_enum() const {
    JVMFlagsEnum i = static_cast<JVMFlagsEnum>(this - JVMFlag::flags);
    assert_valid_flag_enum(i);
    return i;
  }

  static JVMFlag* flag_from_enum(JVMFlagsEnum flag_enum) {
    assert_valid_flag_enum(flag_enum);
    return &JVMFlag::flags[flag_enum];
  }

#define JVM_FLAG_TYPE_ACCESSOR(t)                                                                 \
  bool is_##t() const                      { return _type == TYPE_##t;}                           \
  t get_##t() const                        { assert(is_##t(), "sanity"); return *((t*) _addr); }

  JVM_FLAG_NON_STRING_TYPES_DO(JVM_FLAG_TYPE_ACCESSOR)

  bool is_ccstr()                      const { return _type == TYPE_ccstr || _type == TYPE_ccstrlist; }
  bool ccstr_accumulates()             const { return _type == TYPE_ccstrlist; }
  ccstr get_ccstr()                    const { assert(is_ccstr(), "sanity"); return *((ccstr*) _addr); }
  void set_ccstr(ccstr value)                { assert(is_ccstr(), "sanity"); *((ccstr*) _addr) = value; }

#define JVM_FLAG_AS_STRING(t) \
  case TYPE_##t: return STR(t);

  const char* type_string() const {
    return type_string_for((FlagType)_type);
  }

  static const char* type_string_for(FlagType t) {
    switch(t) {
    JVM_FLAG_NON_STRING_TYPES_DO(JVM_FLAG_AS_STRING)
    case TYPE_ccstr:     return "ccstr";
    case TYPE_ccstrlist: return "ccstrlist";
    default:
        ShouldNotReachHere();
        return "unknown";
    }
  }

  int type() const { return _type; }
  const char* name() const { return _name; }

  // Do not use JVMFlag::read() or JVMFlag::write() directly unless you know
  // what you're doing. Use FLAG_SET_XXX macros or JVMFlagAccess instead.
  template <typename T> T read() const {
    assert_compatible_type<T>(_type);
    return *static_cast<T*>(_addr);
  }

  template <typename T> void write(T value) {
    assert_compatible_type<T>(_type);
    *static_cast<T*>(_addr) = value;
  }

  JVMFlagOrigin get_origin() const {  return JVMFlagOrigin(_flags & VALUE_ORIGIN_MASK);   }
  void set_origin(JVMFlagOrigin origin);

  bool is_default() const         { return (get_origin() == JVMFlagOrigin::DEFAULT);          }
  bool is_ergonomic() const       { return (get_origin() == JVMFlagOrigin::ERGONOMIC);        }
  bool is_command_line() const    { return (_flags & WAS_SET_ON_COMMAND_LINE) != 0;           }
  void set_command_line()         { _flags = Flags(_flags | WAS_SET_ON_COMMAND_LINE);         }
  bool is_jimage_resource() const { return (get_origin() == JVMFlagOrigin::JIMAGE_RESOURCE);  }
  bool is_product() const         { return (_flags & KIND_PRODUCT) != 0;                      }
  bool is_manageable() const      { return (_flags & KIND_MANAGEABLE) != 0;                   }
  bool is_diagnostic() const      { return (_flags & KIND_DIAGNOSTIC) != 0;                   }
  bool is_experimental() const    { return (_flags & KIND_EXPERIMENTAL) != 0;                 }
  bool is_notproduct() const      { return (_flags & KIND_NOT_PRODUCT) != 0;                  }
  bool is_develop() const         { return (_flags & KIND_DEVELOP) != 0;                      }

  bool is_constant_in_binary() const;

  bool is_unlocker() const;
  bool is_unlocked() const;

  // Only manageable flags can be accessed by writeableFlags.cpp
  bool is_writeable() const       { return is_manageable();                      }
  // All flags except "manageable" are assumed to be internal flags.
  bool is_external() const        { return is_manageable();                      }

  void clear_diagnostic();
  void clear_experimental();
  void set_product();

  JVMFlag::MsgType get_locked_message(char*, int) const;

  static bool is_default(JVMFlagsEnum flag);
  static bool is_ergo(JVMFlagsEnum flag);
  static bool is_cmdline(JVMFlagsEnum flag);
  static bool is_jimage_resource(JVMFlagsEnum flag);
  static void setOnCmdLine(JVMFlagsEnum flag);


  // printRanges will print out flags type, name and range values as expected by -XX:+PrintFlagsRanges
  void print_on(outputStream* st, bool withComments = false, bool printRanges = false) const;
  void print_kind(outputStream* st, unsigned int width) const;
  void print_origin(outputStream* st, unsigned int width) const;
  void print_as_flag(outputStream* st) const;

  static const char* flag_error_str(JVMFlag::Error error);

private:
  // type checking - the following functions make sure you access *_addr as
  // the correct type <T>

  static void assert_valid_type_enum(int type_enum) {
    assert(0 <= type_enum && type_enum < NUM_FLAG_TYPES, "sanity");
  }

  // The following computation is not universal, but should be correct
  // for the limited number of types that can be stored inside a JVMFlag.
  template <typename T>
  static constexpr int type_signature() {
    return int(sizeof(T)) |
           (((std::is_integral<T>::value) ? 1 : 0) << 8) |
           (((std::is_signed<T>::value)   ? 1 : 0) << 9) |
           (((std::is_pointer<T>::value)  ? 1 : 0) << 10);
  }

  static const int type_signatures[];

public:
  template <typename T>
  static void assert_compatible_type(int type_enum) {
    assert(is_compatible_type<T>(type_enum), "must be");
  }

  template <typename T>
  static bool is_compatible_type(int type_enum) {
    assert_valid_type_enum(type_enum);
    return type_signatures[type_enum] == type_signature<T>();
  }

public:
  static void printSetFlags(outputStream* out);

  // printRanges will print out flags type, name and range values as expected by -XX:+PrintFlagsRanges
  static void printFlags(outputStream* out, bool withComments, bool printRanges = false, bool skipDefaults = false);
  static void printError(bool verbose, const char* msg, ...) ATTRIBUTE_PRINTF(2, 3);

  static void verify() PRODUCT_RETURN;
};

#define DECLARE_CONSTRAINT(type, func) JVMFlag::Error func(type value, bool verbose);

#endif // SHARE_RUNTIME_FLAGS_JVMFLAG_HPP
