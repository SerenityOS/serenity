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
 *
 */

#include "precompiled.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/flags/jvmFlagLookup.hpp"
#include "utilities/defaultStream.hpp"

#define DO_FLAG(type, name,...) DO_HASH(FLAG_MEMBER_ENUM(name), XSTR(name))

#define DO_HASH(flag_enum, flag_name) {          \
  unsigned int hash = hash_code(flag_name);      \
  int bucket_index = (int)(hash % NUM_BUCKETS);  \
  _hashes[flag_enum] = (u2)(hash);               \
  _table[flag_enum] = _buckets[bucket_index];    \
  _buckets[bucket_index] = (short)flag_enum;     \
}

constexpr JVMFlagLookup::JVMFlagLookup() : _buckets(), _table(), _hashes() {
  for (int i = 0; i < NUM_BUCKETS; i++) {
    _buckets[i] = -1;
  }

  ALL_FLAGS(DO_FLAG,
            DO_FLAG,
            DO_FLAG,
            DO_FLAG,
            DO_FLAG,
            IGNORE_RANGE,
            IGNORE_CONSTRAINT)
}

constexpr JVMFlagLookup _flag_lookup_table;

JVMFlag* JVMFlagLookup::find_impl(const char* name, size_t length) const {
  unsigned int hash = hash_code(name, length);
  int bucket_index = (int)(hash % NUM_BUCKETS);
  for (int flag_enum = _buckets[bucket_index]; flag_enum >= 0; ) {
    if (_hashes[flag_enum] == (u2)hash) {
      JVMFlag* flag = JVMFlag::flags + flag_enum;
      if (strncmp(name, flag->name(), length) == 0) {
        // We know flag->name() has at least <length> bytes.
        // Make sure it has exactly <length> bytes
        if (flag->name()[length] == 0) {
          return flag;
        }
      }
    }
    flag_enum = (int)_table[flag_enum];
  }

  return NULL;
}

JVMFlag* JVMFlagLookup::find(const char* name, size_t length) {
  return _flag_lookup_table.find_impl(name, length);
}
