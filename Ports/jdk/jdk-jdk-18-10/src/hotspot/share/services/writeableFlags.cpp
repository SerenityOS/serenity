/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/flags/jvmFlagAccess.hpp"
#include "runtime/flags/jvmFlagLimit.hpp"
#include "runtime/java.hpp"
#include "runtime/jniHandles.hpp"
#include "services/writeableFlags.hpp"

#define TEMP_BUF_SIZE 80

static void buffer_concat(char* buffer, const char* src) {
  strncat(buffer, src, TEMP_BUF_SIZE - 1 - strlen(buffer));
}

static void print_flag_error_message_bounds(const JVMFlag* flag, char* buffer) {
  if (JVMFlagLimit::get_range(flag) != NULL) {
    buffer_concat(buffer, "must have value in range ");

    stringStream stream;
    JVMFlagAccess::print_range(&stream, flag);
    const char* range_string = stream.as_string();
    size_t j = strlen(buffer);
    for (size_t i=0; j<TEMP_BUF_SIZE-1; i++) {
      if (range_string[i] == '\0') {
        break;
      } else if (range_string[i] != ' ') {
        buffer[j] = range_string[i];
        j++;
      }
    }
    buffer[j] = '\0';
  }
}

static void print_flag_error_message_if_needed(JVMFlag::Error error, const JVMFlag* flag, FormatBuffer<80>& err_msg) {
  if (error == JVMFlag::SUCCESS) {
    return;
  }

  const char* name = flag->name();
  char buffer[TEMP_BUF_SIZE] = {'\0'};
  if ((error != JVMFlag::MISSING_NAME) && (name != NULL)) {
    buffer_concat(buffer, name);
    buffer_concat(buffer, " error: ");
  } else {
    buffer_concat(buffer, "Error: ");
  }
  switch (error) {
    case JVMFlag::MISSING_NAME:
      buffer_concat(buffer, "flag name is missing."); break;
    case JVMFlag::MISSING_VALUE:
      buffer_concat(buffer, "parsing the textual form of the value."); break;
    case JVMFlag::NON_WRITABLE:
      buffer_concat(buffer, "flag is not writeable."); break;
    case JVMFlag::OUT_OF_BOUNDS:
      if (name != NULL) { print_flag_error_message_bounds(flag, buffer); } break;
    case JVMFlag::VIOLATES_CONSTRAINT:
      buffer_concat(buffer, "value violates its flag's constraint."); break;
    case JVMFlag::INVALID_FLAG:
      buffer_concat(buffer, "there is no flag with the given name."); break;
    case JVMFlag::ERR_OTHER:
      buffer_concat(buffer, "other, unspecified error related to setting the flag."); break;
    case JVMFlag::SUCCESS:
      break;
    default:
      break;
  }

  err_msg.print("%s", buffer);
}

template <typename T, int type_enum>
JVMFlag::Error WriteableFlags::set_flag_impl(const char* name, T value, JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  JVMFlag* flag = JVMFlag::find_flag(name);
  JVMFlag::Error err = JVMFlagAccess::set<T, type_enum>(flag, &value, origin);
  print_flag_error_message_if_needed(err, flag, err_msg);
  return err;
}


// set a boolean global flag
JVMFlag::Error WriteableFlags::set_bool_flag(const char* name, const char* arg, JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  if ((strcasecmp(arg, "true") == 0) || (*arg == '1' && *(arg + 1) == 0)) {
    return set_flag_impl<JVM_FLAG_TYPE(bool)>(name, true, origin, err_msg);
  } else if ((strcasecmp(arg, "false") == 0) || (*arg == '0' && *(arg + 1) == 0)) {
    return set_flag_impl<JVM_FLAG_TYPE(bool)>(name, false, origin, err_msg);
  }
  err_msg.print("flag value must be a boolean (1/0 or true/false)");
  return JVMFlag::WRONG_FORMAT;
}

// set a int global flag
JVMFlag::Error WriteableFlags::set_int_flag(const char* name, const char* arg, JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  int value;

  if (sscanf(arg, "%d", &value) == 1) {
    return set_flag_impl<JVM_FLAG_TYPE(int)>(name, value, origin, err_msg);
  }
  err_msg.print("flag value must be an integer");
  return JVMFlag::WRONG_FORMAT;
}

// set a uint global flag
JVMFlag::Error WriteableFlags::set_uint_flag(const char* name, const char* arg, JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  uint value;

  if (sscanf(arg, "%u", &value) == 1) {
    return set_flag_impl<JVM_FLAG_TYPE(uint)>(name, value, origin, err_msg);
  }
  err_msg.print("flag value must be an unsigned integer");
  return JVMFlag::WRONG_FORMAT;
}

// set a intx global flag
JVMFlag::Error WriteableFlags::set_intx_flag(const char* name, const char* arg, JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  intx value;

  if (sscanf(arg, INTX_FORMAT, &value) == 1) {
    return set_flag_impl<JVM_FLAG_TYPE(intx)>(name, value, origin, err_msg);
  }
  err_msg.print("flag value must be an integer");
  return JVMFlag::WRONG_FORMAT;
}

// set a uintx global flag
JVMFlag::Error WriteableFlags::set_uintx_flag(const char* name, const char* arg, JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  uintx value;

  if (sscanf(arg, UINTX_FORMAT, &value) == 1) {
    return set_flag_impl<JVM_FLAG_TYPE(uintx)>(name, value, origin, err_msg);
  }
  err_msg.print("flag value must be an unsigned integer");
  return JVMFlag::WRONG_FORMAT;
}

// set a uint64_t global flag
JVMFlag::Error WriteableFlags::set_uint64_t_flag(const char* name, const char* arg, JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  uint64_t value;

  if (sscanf(arg, UINT64_FORMAT, &value) == 1) {
    return set_flag_impl<JVM_FLAG_TYPE(uint64_t)>(name, value, origin, err_msg);
  }
  err_msg.print("flag value must be an unsigned 64-bit integer");
  return JVMFlag::WRONG_FORMAT;
}

// set a size_t global flag
JVMFlag::Error WriteableFlags::set_size_t_flag(const char* name, const char* arg, JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  size_t value;

  if (sscanf(arg, SIZE_FORMAT, &value) == 1) {
    return set_flag_impl<JVM_FLAG_TYPE(size_t)>(name, value, origin, err_msg);
  }
  err_msg.print("flag value must be an unsigned integer");
  return JVMFlag::WRONG_FORMAT;
}

// set a double global flag
JVMFlag::Error WriteableFlags::set_double_flag(const char* name, const char* arg, JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  double value;

  if (sscanf(arg, "%lf", &value) == 1) {
    return set_flag_impl<JVM_FLAG_TYPE(double)>(name, value, origin, err_msg);
  }
  err_msg.print("flag value must be a double");
  return JVMFlag::WRONG_FORMAT;
}

// set a string global flag using value from AttachOperation
JVMFlag::Error WriteableFlags::set_ccstr_flag(const char* name, const char* value, JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  JVMFlag* flag = JVMFlag::find_flag(name);
  JVMFlag::Error err = JVMFlagAccess::set_ccstr(flag, &value, origin);
  if (err == JVMFlag::SUCCESS) {
    assert(value == NULL, "old value is freed automatically and not returned");
  }
  print_flag_error_message_if_needed(err, flag, err_msg);
  return err;
}

/* sets a writeable flag to the provided value
 *
 * - return status is one of the WriteableFlags::err enum values
 * - an eventual error message will be generated to the provided err_msg buffer
 */
JVMFlag::Error WriteableFlags::set_flag(const char* flag_name, const char* flag_value, JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  return set_flag(flag_name, &flag_value, set_flag_from_char, origin, err_msg);
}

/* sets a writeable flag to the provided value
 *
 * - return status is one of the WriteableFlags::err enum values
 * - an eventual error message will be generated to the provided err_msg buffer
 */
JVMFlag::Error WriteableFlags::set_flag(const char* flag_name, jvalue flag_value, JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  return set_flag(flag_name, &flag_value, set_flag_from_jvalue, origin, err_msg);
}

// a writeable flag setter accepting either 'jvalue' or 'char *' values
JVMFlag::Error WriteableFlags::set_flag(const char* name, const void* value, JVMFlag::Error(*setter)(JVMFlag*,const void*,JVMFlagOrigin,FormatBuffer<80>&), JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  if (name == NULL) {
    err_msg.print("flag name is missing");
    return JVMFlag::MISSING_NAME;
  }
  if (value == NULL) {
    err_msg.print("flag value is missing");
    return JVMFlag::MISSING_VALUE;
  }

  JVMFlag* f = JVMFlag::find_flag(name);
  if (f) {
    // only writeable flags are allowed to be set
    if (f->is_writeable()) {
      return setter(f, value, origin, err_msg);
    } else {
      err_msg.print("only 'writeable' flags can be set");
      return JVMFlag::NON_WRITABLE;
    }
  }

  err_msg.print("flag %s does not exist", name);
  return JVMFlag::INVALID_FLAG;
}

// a writeable flag setter accepting 'char *' values
JVMFlag::Error WriteableFlags::set_flag_from_char(JVMFlag* f, const void* value, JVMFlagOrigin origin, FormatBuffer<80>& err_msg) {
  char* flag_value = *(char**)value;
  if (flag_value == NULL) {
    err_msg.print("flag value is missing");
    return JVMFlag::MISSING_VALUE;
  }
  if (f->is_bool()) {
    return set_bool_flag(f->name(), flag_value, origin, err_msg);
  } else if (f->is_int()) {
    return set_int_flag(f->name(), flag_value, origin, err_msg);
  } else if (f->is_uint()) {
    return set_uint_flag(f->name(), flag_value, origin, err_msg);
  } else if (f->is_intx()) {
    return set_intx_flag(f->name(), flag_value, origin, err_msg);
  } else if (f->is_uintx()) {
    return set_uintx_flag(f->name(), flag_value, origin, err_msg);
  } else if (f->is_uint64_t()) {
    return set_uint64_t_flag(f->name(), flag_value, origin, err_msg);
  } else if (f->is_size_t()) {
    return set_size_t_flag(f->name(), flag_value, origin, err_msg);
  } else if (f->is_double()) {
    return set_double_flag(f->name(), flag_value, origin, err_msg);
  } else if (f->is_ccstr()) {
    return set_ccstr_flag(f->name(), flag_value, origin, err_msg);
  } else {
    ShouldNotReachHere();
  }
  return JVMFlag::ERR_OTHER;
}

// a writeable flag setter accepting 'jvalue' values
JVMFlag::Error WriteableFlags::set_flag_from_jvalue(JVMFlag* f, const void* value, JVMFlagOrigin origin,
                                                 FormatBuffer<80>& err_msg) {
  jvalue new_value = *(jvalue*)value;
  if (f->is_bool()) {
    bool bvalue = (new_value.z == JNI_TRUE ? true : false);
    return set_flag_impl<JVM_FLAG_TYPE(bool)>(f->name(), bvalue, origin, err_msg);
  } else if (f->is_int()) {
    int ivalue = (int)new_value.j;
    return set_flag_impl<JVM_FLAG_TYPE(int)>(f->name(), ivalue, origin, err_msg);
  } else if (f->is_uint()) {
    uint uvalue = (uint)new_value.j;
    return set_flag_impl<JVM_FLAG_TYPE(uint)>(f->name(), uvalue, origin, err_msg);
  } else if (f->is_intx()) {
    intx ivalue = (intx)new_value.j;
    return set_flag_impl<JVM_FLAG_TYPE(intx)>(f->name(), ivalue, origin, err_msg);
  } else if (f->is_uintx()) {
    uintx uvalue = (uintx)new_value.j;
    return set_flag_impl<JVM_FLAG_TYPE(uintx)>(f->name(), uvalue, origin, err_msg);
  } else if (f->is_uint64_t()) {
    uint64_t uvalue = (uint64_t)new_value.j;
    return set_flag_impl<JVM_FLAG_TYPE(uint64_t)>(f->name(), uvalue, origin, err_msg);
  } else if (f->is_size_t()) {
    size_t svalue = (size_t)new_value.j;
    return set_flag_impl<JVM_FLAG_TYPE(size_t)>(f->name(), svalue, origin, err_msg);
  } else if (f->is_double()) {
    double dvalue = (double)new_value.d;
    return set_flag_impl<JVM_FLAG_TYPE(double)>(f->name(), dvalue, origin, err_msg);
  } else if (f->is_ccstr()) {
    oop str = JNIHandles::resolve_external_guard(new_value.l);
    if (str == NULL) {
      err_msg.print("flag value is missing");
      return JVMFlag::MISSING_VALUE;
    }
    ResourceMark rm;
    ccstr svalue = java_lang_String::as_utf8_string(str);
    JVMFlag::Error ret = WriteableFlags::set_ccstr_flag(f->name(), svalue, origin, err_msg);
    return ret;
  } else {
    ShouldNotReachHere();
  }
  return JVMFlag::ERR_OTHER;
}
