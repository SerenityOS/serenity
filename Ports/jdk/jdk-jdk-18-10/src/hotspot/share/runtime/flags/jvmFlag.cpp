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
#include "jvm_io.h"
#include "jfr/jfrEvents.hpp"
#include "memory/allocation.inline.hpp"
#include "runtime/arguments.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/flags/jvmFlagAccess.hpp"
#include "runtime/flags/jvmFlagLookup.hpp"
#include "runtime/globals_extension.hpp"
#include "utilities/defaultStream.hpp"
#include "utilities/stringUtils.hpp"

static bool is_product_build() {
#ifdef PRODUCT
  return true;
#else
  return false;
#endif
}

void JVMFlag::set_origin(JVMFlagOrigin new_origin) {
  int old_flags = _flags;
  int origin = static_cast<int>(new_origin);
  assert((origin & VALUE_ORIGIN_MASK) == origin, "sanity");
  int was_in_cmdline = (new_origin == JVMFlagOrigin::COMMAND_LINE) ? WAS_SET_ON_COMMAND_LINE : 0;
  _flags = Flags((_flags & ~VALUE_ORIGIN_MASK) | origin | was_in_cmdline);
  if ((old_flags & WAS_SET_ON_COMMAND_LINE) != 0) {
    assert((_flags & WAS_SET_ON_COMMAND_LINE) != 0, "once initialized, should never change");
  }
}

/**
 * Returns if this flag is a constant in the binary.  Right now this is
 * true for notproduct and develop flags in product builds.
 */
bool JVMFlag::is_constant_in_binary() const {
#ifdef PRODUCT
  return is_notproduct() || is_develop();
#else
  return false;
#endif
}

bool JVMFlag::is_unlocker() const {
  return strcmp(_name, "UnlockDiagnosticVMOptions") == 0 ||
         strcmp(_name, "UnlockExperimentalVMOptions") == 0;
}

bool JVMFlag::is_unlocked() const {
  if (is_diagnostic()) {
    return UnlockDiagnosticVMOptions;
  }
  if (is_experimental()) {
    return UnlockExperimentalVMOptions;
  }
  return true;
}

void JVMFlag::clear_diagnostic() {
  assert(is_diagnostic(), "sanity");
  _flags = Flags(_flags & ~KIND_DIAGNOSTIC);
  assert(!is_diagnostic(), "sanity");
}

void JVMFlag::clear_experimental() {
  assert(is_experimental(), "sanity");
  _flags = Flags(_flags & ~KIND_EXPERIMENTAL);
  assert(!is_experimental(), "sanity");
}

void JVMFlag::set_product() {
  assert(!is_product(), "sanity");
  _flags = Flags(_flags | KIND_PRODUCT);
  assert(is_product(), "sanity");
}

// Get custom message for this locked flag, or NULL if
// none is available. Returns message type produced.
JVMFlag::MsgType JVMFlag::get_locked_message(char* buf, int buflen) const {
  buf[0] = '\0';
  if (is_diagnostic() && !is_unlocked()) {
    jio_snprintf(buf, buflen,
                 "Error: VM option '%s' is diagnostic and must be enabled via -XX:+UnlockDiagnosticVMOptions.\n"
                 "Error: The unlock option must precede '%s'.\n",
                 _name, _name);
    return JVMFlag::DIAGNOSTIC_FLAG_BUT_LOCKED;
  }
  if (is_experimental() && !is_unlocked()) {
    jio_snprintf(buf, buflen,
                 "Error: VM option '%s' is experimental and must be enabled via -XX:+UnlockExperimentalVMOptions.\n"
                 "Error: The unlock option must precede '%s'.\n",
                 _name, _name);
    return JVMFlag::EXPERIMENTAL_FLAG_BUT_LOCKED;
  }
  if (is_develop() && is_product_build()) {
    jio_snprintf(buf, buflen, "Error: VM option '%s' is develop and is available only in debug version of VM.\n",
                 _name);
    return JVMFlag::DEVELOPER_FLAG_BUT_PRODUCT_BUILD;
  }
  if (is_notproduct() && is_product_build()) {
    jio_snprintf(buf, buflen, "Error: VM option '%s' is notproduct and is available only in debug version of VM.\n",
                 _name);
    return JVMFlag::NOTPRODUCT_FLAG_BUT_PRODUCT_BUILD;
  }
  return JVMFlag::NONE;
}

// Helper function for JVMFlag::print_on().
// Fills current line up to requested position.
// Should the current position already be past the requested position,
// one separator blank is enforced.
void fill_to_pos(outputStream* st, unsigned int req_pos) {
  if ((unsigned int)st->position() < req_pos) {
    st->fill_to(req_pos);  // need to fill with blanks to reach req_pos
  } else {
    st->print(" ");        // enforce blank separation. Previous field too long.
  }
}

void JVMFlag::print_on(outputStream* st, bool withComments, bool printRanges) const {
  // Don't print notproduct and develop flags in a product build.
  if (is_constant_in_binary()) {
    return;
  }

  if (!printRanges) {
    // The command line options -XX:+PrintFlags* cause this function to be called
    // for each existing flag to print information pertinent to this flag. The data
    // is displayed in columnar form, with the following layout:
    //  col1 - data type, right-justified
    //  col2 - name,      left-justified
    //  col3 - ' ='       double-char, leading space to align with possible '+='
    //  col4 - value      left-justified
    //  col5 - kind       right-justified
    //  col6 - origin     left-justified
    //  col7 - comments   left-justified
    //
    //  The column widths are fixed. They are defined such that, for most cases,
    //  an eye-pleasing tabular output is created.
    //
    //  Sample output:
    //       bool ThreadPriorityVerbose                    = false                                     {product} {default}
    //      uintx ThresholdTolerance                       = 10                                        {product} {default}
    //     size_t TLABSize                                 = 0                                         {product} {default}
    //      uintx SurvivorRatio                            = 8                                         {product} {default}
    //     double InitialRAMPercentage                     = 1.562500                                  {product} {default}
    //      ccstr CompileCommandFile                       = MyFile.cmd                                {product} {command line}
    //  ccstrlist CompileOnly                              = Method1
    //            CompileOnly                             += Method2                                   {product} {command line}
    //  |         |                                       |  |                              |                    |               |
    //  |         |                                       |  |                              |                    |               +-- col7
    //  |         |                                       |  |                              |                    +-- col6
    //  |         |                                       |  |                              +-- col5
    //  |         |                                       |  +-- col4
    //  |         |                                       +-- col3
    //  |         +-- col2
    //  +-- col1

    const unsigned int col_spacing = 1;
    const unsigned int col1_pos    = 0;
    const unsigned int col1_width  = 9;
    const unsigned int col2_pos    = col1_pos + col1_width + col_spacing;
    const unsigned int col2_width  = 39;
    const unsigned int col3_pos    = col2_pos + col2_width + col_spacing;
    const unsigned int col3_width  = 2;
    const unsigned int col4_pos    = col3_pos + col3_width + col_spacing;
    const unsigned int col4_width  = 30;
    const unsigned int col5_pos    = col4_pos + col4_width + col_spacing;
    const unsigned int col5_width  = 20;
    const unsigned int col6_pos    = col5_pos + col5_width + col_spacing;
    const unsigned int col6_width  = 15;
    const unsigned int col7_pos    = col6_pos + col6_width + col_spacing;
    const unsigned int col7_width  = 1;

    st->fill_to(col1_pos);
    st->print("%*s", col1_width, type_string());  // right-justified, therefore width is required.

    fill_to_pos(st, col2_pos);
    st->print("%s", _name);

    fill_to_pos(st, col3_pos);
    st->print(" =");  // use " =" for proper alignment with multiline ccstr output.

    fill_to_pos(st, col4_pos);
    if (is_bool()) {
      st->print("%s", get_bool() ? "true" : "false");
    } else if (is_int()) {
      st->print("%d", get_int());
    } else if (is_uint()) {
      st->print("%u", get_uint());
    } else if (is_intx()) {
      st->print(INTX_FORMAT, get_intx());
    } else if (is_uintx()) {
      st->print(UINTX_FORMAT, get_uintx());
    } else if (is_uint64_t()) {
      st->print(UINT64_FORMAT, get_uint64_t());
    } else if (is_size_t()) {
      st->print(SIZE_FORMAT, get_size_t());
    } else if (is_double()) {
      st->print("%f", get_double());
    } else if (is_ccstr()) {
      // Honor <newline> characters in ccstr: print multiple lines.
      const char* cp = get_ccstr();
      if (cp != NULL) {
        const char* eol;
        while ((eol = strchr(cp, '\n')) != NULL) {
          size_t llen = pointer_delta(eol, cp, sizeof(char));
          st->print("%.*s", (int)llen, cp);
          st->cr();
          cp = eol+1;
          fill_to_pos(st, col2_pos);
          st->print("%s", _name);
          fill_to_pos(st, col3_pos);
          st->print("+=");
          fill_to_pos(st, col4_pos);
        }
        st->print("%s", cp);
      }
    } else {
      st->print("unhandled  type %s", type_string());
      st->cr();
      return;
    }

    fill_to_pos(st, col5_pos);
    print_kind(st, col5_width);

    fill_to_pos(st, col6_pos);
    print_origin(st, col6_width);

#ifndef PRODUCT
    if (withComments) {
      fill_to_pos(st, col7_pos);
      st->print("%s", _doc);
    }
#endif
    st->cr();
  } else if (!is_bool() && !is_ccstr()) {
    // The command line options -XX:+PrintFlags* cause this function to be called
    // for each existing flag to print information pertinent to this flag. The data
    // is displayed in columnar form, with the following layout:
    //  col1 - data type, right-justified
    //  col2 - name,      left-justified
    //  col4 - range      [ min ... max]
    //  col5 - kind       right-justified
    //  col6 - origin     left-justified
    //  col7 - comments   left-justified
    //
    //  The column widths are fixed. They are defined such that, for most cases,
    //  an eye-pleasing tabular output is created.
    //
    //  Sample output:
    //       intx MinPassesBeforeFlush                               [ 0                         ...       9223372036854775807 ]                         {diagnostic} {default}
    //      uintx MinRAMFraction                                     [ 1                         ...      18446744073709551615 ]                            {product} {default}
    //     double MinRAMPercentage                                   [ 0.000                     ...                   100.000 ]                            {product} {default}
    //      uintx MinSurvivorRatio                                   [ 3                         ...      18446744073709551615 ]                            {product} {default}
    //     size_t MinTLABSize                                        [ 1                         ...       9223372036854775807 ]                            {product} {default}
    //       intx MaxInlineSize                                      [ 0                         ...                2147483647 ]                            {product} {default}
    //  |         |                                                  |                                                           |                                    |               |
    //  |         |                                                  |                                                           |                                    |               +-- col7
    //  |         |                                                  |                                                           |                                    +-- col6
    //  |         |                                                  |                                                           +-- col5
    //  |         |                                                  +-- col4
    //  |         +-- col2
    //  +-- col1

    const unsigned int col_spacing = 1;
    const unsigned int col1_pos    = 0;
    const unsigned int col1_width  = 9;
    const unsigned int col2_pos    = col1_pos + col1_width + col_spacing;
    const unsigned int col2_width  = 49;
    const unsigned int col3_pos    = col2_pos + col2_width + col_spacing;
    const unsigned int col3_width  = 0;
    const unsigned int col4_pos    = col3_pos + col3_width + col_spacing;
    const unsigned int col4_width  = 60;
    const unsigned int col5_pos    = col4_pos + col4_width + col_spacing;
    const unsigned int col5_width  = 35;
    const unsigned int col6_pos    = col5_pos + col5_width + col_spacing;
    const unsigned int col6_width  = 15;
    const unsigned int col7_pos    = col6_pos + col6_width + col_spacing;
    const unsigned int col7_width  = 1;

    st->fill_to(col1_pos);
    st->print("%*s", col1_width, type_string());  // right-justified, therefore width is required.

    fill_to_pos(st, col2_pos);
    st->print("%s", _name);

    fill_to_pos(st, col4_pos);
    JVMFlagAccess::print_range(st, this);

    fill_to_pos(st, col5_pos);
    print_kind(st, col5_width);

    fill_to_pos(st, col6_pos);
    print_origin(st, col6_width);

#ifndef PRODUCT
    if (withComments) {
      fill_to_pos(st, col7_pos);
      st->print("%s", _doc);
    }
#endif
    st->cr();
  }
}

void JVMFlag::print_kind(outputStream* st, unsigned int width) const {
  struct Data {
    int flag;
    const char* name;
  };

  Data data[] = {
    { KIND_JVMCI, "JVMCI" },
    { KIND_C1, "C1" },
    { KIND_C2, "C2" },
    { KIND_ARCH, "ARCH" },
    { KIND_PLATFORM_DEPENDENT, "pd" },
    { KIND_PRODUCT, "product" },
    { KIND_MANAGEABLE, "manageable" },
    { KIND_DIAGNOSTIC, "diagnostic" },
    { KIND_EXPERIMENTAL, "experimental" },
    { KIND_NOT_PRODUCT, "notproduct" },
    { KIND_DEVELOP, "develop" },
    { KIND_LP64_PRODUCT, "lp64_product" },
    { -1, "" }
  };

  if ((_flags & KIND_MASK) != 0) {
    bool is_first = true;
    const size_t buffer_size = 64;
    size_t buffer_used = 0;
    char kind[buffer_size];

    jio_snprintf(kind, buffer_size, "{");
    buffer_used++;
    for (int i = 0; data[i].flag != -1; i++) {
      Data d = data[i];
      if ((_flags & d.flag) != 0) {
        if (is_first) {
          is_first = false;
        } else {
          assert(buffer_used + 1 < buffer_size, "Too small buffer");
          jio_snprintf(kind + buffer_used, buffer_size - buffer_used, " ");
          buffer_used++;
        }
        size_t length = strlen(d.name);
        assert(buffer_used + length < buffer_size, "Too small buffer");
        jio_snprintf(kind + buffer_used, buffer_size - buffer_used, "%s", d.name);
        buffer_used += length;
      }
    }
    assert(buffer_used + 2 <= buffer_size, "Too small buffer");
    jio_snprintf(kind + buffer_used, buffer_size - buffer_used, "}");
    st->print("%*s", width, kind);
  }
}

void JVMFlag::print_origin(outputStream* st, unsigned int width) const {
  st->print("{");
  switch(get_origin()) {
    case JVMFlagOrigin::DEFAULT:
      st->print("default"); break;
    case JVMFlagOrigin::COMMAND_LINE:
      st->print("command line"); break;
    case JVMFlagOrigin::ENVIRON_VAR:
      st->print("environment"); break;
    case JVMFlagOrigin::CONFIG_FILE:
      st->print("config file"); break;
    case JVMFlagOrigin::MANAGEMENT:
      st->print("management"); break;
    case JVMFlagOrigin::ERGONOMIC:
      if (_flags & WAS_SET_ON_COMMAND_LINE) {
        st->print("command line, ");
      }
      st->print("ergonomic"); break;
    case JVMFlagOrigin::ATTACH_ON_DEMAND:
      st->print("attach"); break;
    case JVMFlagOrigin::INTERNAL:
      st->print("internal"); break;
    case JVMFlagOrigin::JIMAGE_RESOURCE:
      st->print("jimage"); break;
  }
  st->print("}");
}

void JVMFlag::print_as_flag(outputStream* st) const {
  if (is_bool()) {
    st->print("-XX:%s%s", get_bool() ? "+" : "-", _name);
  } else if (is_int()) {
    st->print("-XX:%s=%d", _name, get_int());
  } else if (is_uint()) {
    st->print("-XX:%s=%u", _name, get_uint());
  } else if (is_intx()) {
    st->print("-XX:%s=" INTX_FORMAT, _name, get_intx());
  } else if (is_uintx()) {
    st->print("-XX:%s=" UINTX_FORMAT, _name, get_uintx());
  } else if (is_uint64_t()) {
    st->print("-XX:%s=" UINT64_FORMAT, _name, get_uint64_t());
  } else if (is_size_t()) {
    st->print("-XX:%s=" SIZE_FORMAT, _name, get_size_t());
  } else if (is_double()) {
    st->print("-XX:%s=%f", _name, get_double());
  } else if (is_ccstr()) {
    st->print("-XX:%s=", _name);
    const char* cp = get_ccstr();
    if (cp != NULL) {
      // Need to turn embedded '\n's back into separate arguments
      // Not so efficient to print one character at a time,
      // but the choice is to do the transformation to a buffer
      // and print that.  And this need not be efficient.
      for (; *cp != '\0'; cp += 1) {
        switch (*cp) {
          default:
            st->print("%c", *cp);
            break;
          case '\n':
            st->print(" -XX:%s=", _name);
            break;
        }
      }
    }
  } else {
    ShouldNotReachHere();
  }
}

const char* JVMFlag::flag_error_str(JVMFlag::Error error) {
  switch (error) {
    case JVMFlag::MISSING_NAME: return "MISSING_NAME";
    case JVMFlag::MISSING_VALUE: return "MISSING_VALUE";
    case JVMFlag::NON_WRITABLE: return "NON_WRITABLE";
    case JVMFlag::OUT_OF_BOUNDS: return "OUT_OF_BOUNDS";
    case JVMFlag::VIOLATES_CONSTRAINT: return "VIOLATES_CONSTRAINT";
    case JVMFlag::INVALID_FLAG: return "INVALID_FLAG";
    case JVMFlag::ERR_OTHER: return "ERR_OTHER";
    case JVMFlag::SUCCESS: return "SUCCESS";
    default: ShouldNotReachHere(); return "NULL";
  }
}

//----------------------------------------------------------------------
// Build flagTable[]

// Find out the number of LP64/ARCH/JVMCI/COMPILER1/COMPILER2 flags,
// for JVMFlag::flag_group()

#define ENUM_F(type, name, ...)  enum_##name,
#define IGNORE_F(...)

//                                                  dev     dev-pd  pro     pro-pd  notpro  range     constraint
enum FlagCounter_LP64  { LP64_RUNTIME_FLAGS(        ENUM_F, ENUM_F, ENUM_F, ENUM_F, ENUM_F, IGNORE_F, IGNORE_F)  num_flags_LP64   };
enum FlagCounter_ARCH  { ARCH_FLAGS(                ENUM_F,         ENUM_F,         ENUM_F, IGNORE_F, IGNORE_F)  num_flags_ARCH   };
enum FlagCounter_JVMCI { JVMCI_ONLY(JVMCI_FLAGS(    ENUM_F, ENUM_F, ENUM_F, ENUM_F, ENUM_F, IGNORE_F, IGNORE_F)) num_flags_JVMCI  };
enum FlagCounter_C1    { COMPILER1_PRESENT(C1_FLAGS(ENUM_F, ENUM_F, ENUM_F, ENUM_F, ENUM_F, IGNORE_F, IGNORE_F)) num_flags_C1     };
enum FlagCounter_C2    { COMPILER2_PRESENT(C2_FLAGS(ENUM_F, ENUM_F, ENUM_F, ENUM_F, ENUM_F, IGNORE_F, IGNORE_F)) num_flags_C2     };

const int first_flag_enum_LP64   = 0;
const int first_flag_enum_ARCH   = first_flag_enum_LP64  + num_flags_LP64;
const int first_flag_enum_JVMCI  = first_flag_enum_ARCH  + num_flags_ARCH;
const int first_flag_enum_C1     = first_flag_enum_JVMCI + num_flags_JVMCI;
const int first_flag_enum_C2     = first_flag_enum_C1    + num_flags_C1;
const int first_flag_enum_other  = first_flag_enum_C2    + num_flags_C2;

static constexpr int flag_group(int flag_enum) {
  if (flag_enum < first_flag_enum_ARCH)  return JVMFlag::KIND_LP64_PRODUCT;
  if (flag_enum < first_flag_enum_JVMCI) return JVMFlag::KIND_ARCH;
  if (flag_enum < first_flag_enum_C1)    return JVMFlag::KIND_JVMCI;
  if (flag_enum < first_flag_enum_C2)    return JVMFlag::KIND_C1;
  if (flag_enum < first_flag_enum_other) return JVMFlag::KIND_C2;

  return 0;
}

constexpr JVMFlag::JVMFlag(int flag_enum, FlagType type, const char* name,
                           void* addr, int flags, int extra_flags, const char* doc) :
  _addr(addr), _name(name), _flags(), _type(type) NOT_PRODUCT(COMMA _doc(doc)) {
  flags = flags | extra_flags | static_cast<int>(JVMFlagOrigin::DEFAULT) | flag_group(flag_enum);
  if ((flags & JVMFlag::KIND_PRODUCT) != 0) {
    if (flags & (JVMFlag::KIND_DIAGNOSTIC | JVMFlag::KIND_MANAGEABLE | JVMFlag::KIND_EXPERIMENTAL)) {
      // Backwards compatibility. This will be relaxed in JDK-7123237.
      flags &= ~(JVMFlag::KIND_PRODUCT);
    }
  }
  _flags = static_cast<Flags>(flags);
}

constexpr JVMFlag::JVMFlag(int flag_enum,  FlagType type, const char* name,
                           void* addr, int flags, const char* doc) :
  JVMFlag(flag_enum, type, name, addr, flags, /*extra_flags*/0, doc) {}

const int PRODUCT_KIND     = JVMFlag::KIND_PRODUCT;
const int PRODUCT_KIND_PD  = JVMFlag::KIND_PRODUCT | JVMFlag::KIND_PLATFORM_DEPENDENT;
const int DEVELOP_KIND     = JVMFlag::KIND_DEVELOP;
const int DEVELOP_KIND_PD  = JVMFlag::KIND_DEVELOP | JVMFlag::KIND_PLATFORM_DEPENDENT;
const int NOTPROD_KIND     = JVMFlag::KIND_NOT_PRODUCT;

#define FLAG_TYPE(type) (JVMFlag::TYPE_ ## type)
#define INITIALIZE_DEVELOP_FLAG(   type, name, value, ...) JVMFlag(FLAG_MEMBER_ENUM(name), FLAG_TYPE(type), XSTR(name), (void*)&name, DEVELOP_KIND,    __VA_ARGS__),
#define INITIALIZE_DEVELOP_FLAG_PD(type, name,        ...) JVMFlag(FLAG_MEMBER_ENUM(name), FLAG_TYPE(type), XSTR(name), (void*)&name, DEVELOP_KIND_PD, __VA_ARGS__),
#define INITIALIZE_PRODUCT_FLAG(   type, name, value, ...) JVMFlag(FLAG_MEMBER_ENUM(name), FLAG_TYPE(type), XSTR(name), (void*)&name, PRODUCT_KIND,    __VA_ARGS__),
#define INITIALIZE_PRODUCT_FLAG_PD(type, name,        ...) JVMFlag(FLAG_MEMBER_ENUM(name), FLAG_TYPE(type), XSTR(name), (void*)&name, PRODUCT_KIND_PD, __VA_ARGS__),
#define INITIALIZE_NOTPROD_FLAG(   type, name, value, ...) JVMFlag(FLAG_MEMBER_ENUM(name), FLAG_TYPE(type), XSTR(name), (void*)&name, NOTPROD_KIND,    __VA_ARGS__),

// Handy aliases to match the symbols used in the flag specification macros.
const int DIAGNOSTIC   = JVMFlag::KIND_DIAGNOSTIC;
const int MANAGEABLE   = JVMFlag::KIND_MANAGEABLE;
const int EXPERIMENTAL = JVMFlag::KIND_EXPERIMENTAL;

#define MATERIALIZE_ALL_FLAGS      \
  ALL_FLAGS(INITIALIZE_DEVELOP_FLAG,     \
            INITIALIZE_DEVELOP_FLAG_PD,  \
            INITIALIZE_PRODUCT_FLAG,     \
            INITIALIZE_PRODUCT_FLAG_PD,  \
            INITIALIZE_NOTPROD_FLAG,     \
            IGNORE_RANGE,          \
            IGNORE_CONSTRAINT)

static JVMFlag flagTable[NUM_JVMFlagsEnum + 1] = {
  MATERIALIZE_ALL_FLAGS
  JVMFlag() // The iteration code wants a flag with a NULL name at the end of the table.
};

// We want flagTable[] to be completely initialized at C++ compilation time, which requires
// that all arguments passed to JVMFlag() constructors be constexpr. The following line
// checks for this -- if any non-constexpr arguments are passed, the C++ compiler will
// generate an error.
//
// constexpr implies internal linkage. This means the flagTable_verify_constexpr[] variable
// will not be included in jvmFlag.o, so there's no footprint cost for having this variable.
//
// Note that we cannot declare flagTable[] as constexpr because JVMFlag::_flags is modified
// at runtime.
constexpr JVMFlag flagTable_verify_constexpr[] = { MATERIALIZE_ALL_FLAGS };

JVMFlag* JVMFlag::flags = flagTable;
size_t JVMFlag::numFlags = (sizeof(flagTable) / sizeof(JVMFlag));

#define JVM_FLAG_TYPE_SIGNATURE(t) JVMFlag::type_signature<t>(),

const int JVMFlag::type_signatures[] = {
  JVM_FLAG_NON_STRING_TYPES_DO(JVM_FLAG_TYPE_SIGNATURE)
  JVMFlag::type_signature<ccstr>(),
  JVMFlag::type_signature<ccstr>()
};

// Search the flag table for a named flag
JVMFlag* JVMFlag::find_flag(const char* name, size_t length, bool allow_locked, bool return_flag) {
  JVMFlag* flag = JVMFlagLookup::find(name, length);
  if (flag != NULL) {
    // Found a matching entry.
    // Don't report notproduct and develop flags in product builds.
    if (flag->is_constant_in_binary()) {
      return (return_flag ? flag : NULL);
    }
    // Report locked flags only if allowed.
    if (!(flag->is_unlocked() || flag->is_unlocker())) {
      if (!allow_locked) {
        // disable use of locked flags, e.g. diagnostic, experimental,
        // etc. until they are explicitly unlocked
        return NULL;
      }
    }
    return flag;
  }
  // JVMFlag name is not in the flag table
  return NULL;
}

JVMFlag* JVMFlag::fuzzy_match(const char* name, size_t length, bool allow_locked) {
  float VMOptionsFuzzyMatchSimilarity = 0.7f;
  JVMFlag* match = NULL;
  float score;
  float max_score = -1;

  for (JVMFlag* current = &flagTable[0]; current->_name != NULL; current++) {
    score = StringUtils::similarity(current->_name, strlen(current->_name), name, length);
    if (score > max_score) {
      max_score = score;
      match = current;
    }
  }

  if (match == NULL) {
    return NULL;
  }

  if (!(match->is_unlocked() || match->is_unlocker())) {
    if (!allow_locked) {
      return NULL;
    }
  }

  if (max_score < VMOptionsFuzzyMatchSimilarity) {
    return NULL;
  }

  return match;
}

bool JVMFlag::is_default(JVMFlagsEnum flag) {
  return flag_from_enum(flag)->is_default();
}

bool JVMFlag::is_ergo(JVMFlagsEnum flag) {
  return flag_from_enum(flag)->is_ergonomic();
}

bool JVMFlag::is_cmdline(JVMFlagsEnum flag) {
  return flag_from_enum(flag)->is_command_line();
}

bool JVMFlag::is_jimage_resource(JVMFlagsEnum flag) {
  return flag_from_enum(flag)->is_jimage_resource();
}

void JVMFlag::setOnCmdLine(JVMFlagsEnum flag) {
  flag_from_enum(flag)->set_command_line();
}

extern "C" {
  static int compare_flags(const void* void_a, const void* void_b) {
    return strcmp((*((JVMFlag**) void_a))->name(), (*((JVMFlag**) void_b))->name());
  }
}

void JVMFlag::printSetFlags(outputStream* out) {
  // Print which flags were set on the command line
  // note: this method is called before the thread structure is in place
  //       which means resource allocation cannot be used.

  // The last entry is the null entry.
  const size_t length = JVMFlag::numFlags - 1;

  // Sort
  JVMFlag** array = NEW_C_HEAP_ARRAY(JVMFlag*, length, mtArguments);
  for (size_t i = 0; i < length; i++) {
    array[i] = &flagTable[i];
  }
  qsort(array, length, sizeof(JVMFlag*), compare_flags);

  // Print
  for (size_t i = 0; i < length; i++) {
    if (array[i]->get_origin() != JVMFlagOrigin::DEFAULT) {
      array[i]->print_as_flag(out);
      out->print(" ");
    }
  }
  out->cr();
  FREE_C_HEAP_ARRAY(JVMFlag*, array);
}

#ifndef PRODUCT

void JVMFlag::verify() {
  assert(Arguments::check_vm_args_consistency(), "Some flag settings conflict");
}

#endif // PRODUCT

#ifdef ASSERT

void JVMFlag::assert_valid_flag_enum(JVMFlagsEnum i) {
  assert(0 <= int(i) && int(i) < NUM_JVMFlagsEnum, "must be");
}

void JVMFlag::check_all_flag_declarations() {
  for (JVMFlag* current = &flagTable[0]; current->_name != NULL; current++) {
    int flags = static_cast<int>(current->_flags);
    // Backwards compatibility. This will be relaxed/removed in JDK-7123237.
    int mask = JVMFlag::KIND_DIAGNOSTIC | JVMFlag::KIND_MANAGEABLE | JVMFlag::KIND_EXPERIMENTAL;
    if ((flags & mask) != 0) {
      assert((flags & mask) == JVMFlag::KIND_DIAGNOSTIC ||
             (flags & mask) == JVMFlag::KIND_MANAGEABLE ||
             (flags & mask) == JVMFlag::KIND_EXPERIMENTAL,
             "%s can be declared with at most one of "
             "DIAGNOSTIC, MANAGEABLE or EXPERIMENTAL", current->_name);
      assert((flags & KIND_NOT_PRODUCT) == 0 &&
             (flags & KIND_DEVELOP) == 0,
             "%s has an optional DIAGNOSTIC, MANAGEABLE or EXPERIMENTAL "
             "attribute; it must be declared as a product flag", current->_name);
    }
  }
}

#endif // ASSERT

void JVMFlag::printFlags(outputStream* out, bool withComments, bool printRanges, bool skipDefaults) {
  // Print the flags sorted by name
  // Note: This method may be called before the thread structure is in place
  //       which means resource allocation cannot be used. Also, it may be
  //       called as part of error reporting, so handle native OOMs gracefully.

  // The last entry is the null entry.
  const size_t length = JVMFlag::numFlags - 1;

  // Print
  if (!printRanges) {
    out->print_cr("[Global flags]");
  } else {
    out->print_cr("[Global flags ranges]");
  }

  // Sort
  JVMFlag** array = NEW_C_HEAP_ARRAY_RETURN_NULL(JVMFlag*, length, mtArguments);
  if (array != NULL) {
    for (size_t i = 0; i < length; i++) {
      array[i] = &flagTable[i];
    }
    qsort(array, length, sizeof(JVMFlag*), compare_flags);

    for (size_t i = 0; i < length; i++) {
      if (array[i]->is_unlocked() && !(skipDefaults && array[i]->is_default())) {
        array[i]->print_on(out, withComments, printRanges);
      }
    }
    FREE_C_HEAP_ARRAY(JVMFlag*, array);
  } else {
    // OOM? Print unsorted.
    for (size_t i = 0; i < length; i++) {
      if (flagTable[i].is_unlocked() && !(skipDefaults && flagTable[i].is_default())) {
        flagTable[i].print_on(out, withComments, printRanges);
      }
    }
  }
}

void JVMFlag::printError(bool verbose, const char* msg, ...) {
  if (verbose) {
    va_list listPointer;
    va_start(listPointer, msg);
    jio_vfprintf(defaultStream::error_stream(), msg, listPointer);
    va_end(listPointer);
  }
}
