/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_GLOBALS_EXTENSION_HPP
#define SHARE_RUNTIME_GLOBALS_EXTENSION_HPP

#include "runtime/flags/allFlags.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/flags/jvmFlagAccess.hpp"
#include "utilities/macros.hpp"

// Construct enum of Flag_<cmdline-arg> constants.

#define FLAG_MEMBER_ENUM(name) Flag_##name##_enum
#define FLAG_MEMBER_ENUM_(name) FLAG_MEMBER_ENUM(name),

#define DEFINE_FLAG_MEMBER_ENUM(type, name, ...)  FLAG_MEMBER_ENUM_(name)

enum JVMFlagsEnum : int {
  INVALID_JVMFlagsEnum = -1,
  ALL_FLAGS(DEFINE_FLAG_MEMBER_ENUM,
            DEFINE_FLAG_MEMBER_ENUM,
            DEFINE_FLAG_MEMBER_ENUM,
            DEFINE_FLAG_MEMBER_ENUM,
            DEFINE_FLAG_MEMBER_ENUM,
            IGNORE_RANGE,
            IGNORE_CONSTRAINT)
  NUM_JVMFlagsEnum
};

// Construct set functions for all flags

#define FLAG_MEMBER_SETTER(name) Flag_##name##_set
#define FLAG_MEMBER_SETTER_(type, name) \
  inline JVMFlag::Error FLAG_MEMBER_SETTER(name)(type value, JVMFlagOrigin origin) { \
    return JVMFlagAccess::set<JVM_FLAG_TYPE(type)>(FLAG_MEMBER_ENUM(name), value, origin); \
  }

#define DEFINE_FLAG_MEMBER_SETTER(type, name, ...) FLAG_MEMBER_SETTER_(type, name)

ALL_FLAGS(DEFINE_FLAG_MEMBER_SETTER,
          DEFINE_FLAG_MEMBER_SETTER,
          DEFINE_FLAG_MEMBER_SETTER,
          DEFINE_FLAG_MEMBER_SETTER,
          DEFINE_FLAG_MEMBER_SETTER,
          IGNORE_RANGE,
          IGNORE_CONSTRAINT)

#define FLAG_IS_DEFAULT(name)         (JVMFlag::is_default(FLAG_MEMBER_ENUM(name)))
#define FLAG_IS_ERGO(name)            (JVMFlag::is_ergo(FLAG_MEMBER_ENUM(name)))
#define FLAG_IS_CMDLINE(name)         (JVMFlag::is_cmdline(FLAG_MEMBER_ENUM(name)))
#define FLAG_IS_JIMAGE_RESOURCE(name) (JVMFlag::is_jimage_resource(FLAG_MEMBER_ENUM(name)))

#define FLAG_SET_DEFAULT(name, value) ((name) = (value))

#define FLAG_SET_CMDLINE(name, value) (JVMFlag::setOnCmdLine(FLAG_MEMBER_ENUM(name)), \
                                       FLAG_MEMBER_SETTER(name)((value), JVMFlagOrigin::COMMAND_LINE))
#define FLAG_SET_ERGO(name, value)    (FLAG_MEMBER_SETTER(name)((value), JVMFlagOrigin::ERGONOMIC))
#define FLAG_SET_MGMT(name, value)    (FLAG_MEMBER_SETTER(name)((value), JVMFlagOrigin::MANAGEMENT))

#define FLAG_SET_ERGO_IF_DEFAULT(name, value) \
  do {                                        \
    if (FLAG_IS_DEFAULT(name)) {              \
      FLAG_SET_ERGO(name, value);             \
    }                                         \
  } while (0)

#endif // SHARE_RUNTIME_GLOBALS_EXTENSION_HPP
