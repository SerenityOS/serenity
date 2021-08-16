/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_JNIFASTGETFIELD_HPP
#define SHARE_PRIMS_JNIFASTGETFIELD_HPP

#include "memory/allocation.hpp"
#include "prims/jvm_misc.hpp"

// Basic logic of a fast version of jni_Get<Primitive>Field:
//
// (See safepoint.hpp for a description of _safepoint_counter)
//
// load _safepoint_counter into old_counter
// IF old_counter is odd THEN
//   a safepoint is going on, return jni_GetXXXField
// ELSE
//   load the primitive field value into result (speculatively)
//   load _safepoint_counter into new_counter
//   IF (old_counter == new_counter) THEN
//     no safepoint happened during the field access, return result
//   ELSE
//     a safepoint might have happened in-between, return jni_GetXXXField()
//   ENDIF
// ENDIF
//
// LoadLoad membars to maintain the load order may be necessary
// for some platforms.
//
// The fast versions don't check for pending suspension request.
// This is fine since it's totally read-only and doesn't create new race.
//
// There is a hypothetical safepoint counter wraparound. But it's not
// a practical concern.

class JNI_FastGetField : AllStatic {
 private:
  enum { LIST_CAPACITY = 40 };      // a conservative number for the number of
                                    // speculative loads on all the platforms
  static address speculative_load_pclist [];
  static address slowcase_entry_pclist   [];
  static int     count;

  static address generate_fast_get_int_field0(BasicType type);
  static address generate_fast_get_float_field0(BasicType type);

#ifdef AARCH64
  template<int BType>
  static address generate_fast_get_int_field1();
#endif // AARCH64

 public:
#if defined(_WINDOWS) && !defined(_WIN64)
  static GetBooleanField_t jni_fast_GetBooleanField_fp;
  static GetByteField_t    jni_fast_GetByteField_fp;
  static GetCharField_t    jni_fast_GetCharField_fp;
  static GetShortField_t   jni_fast_GetShortField_fp;
  static GetIntField_t     jni_fast_GetIntField_fp;
  static GetLongField_t    jni_fast_GetLongField_fp;
  static GetFloatField_t   jni_fast_GetFloatField_fp;
  static GetDoubleField_t  jni_fast_GetDoubleField_fp;
#endif

  static address generate_fast_get_boolean_field();
  static address generate_fast_get_byte_field();
  static address generate_fast_get_char_field();
  static address generate_fast_get_short_field();
  static address generate_fast_get_int_field();
  static address generate_fast_get_long_field();
  static address generate_fast_get_float_field();
  static address generate_fast_get_double_field();

  // If pc is in speculative_load_pclist, return the corresponding
  // slow case entry pc. Otherwise, return -1.
  // This is used by signal/exception handler to handle such case:
  // After an even safepoint counter is loaded and a fast field access
  // is about to begin, a GC kicks in and shrinks the heap. Then the
  // field access may fault. The signal/exception handler needs to
  // return to the slow case.
  //
  // The GC may decide to temporarily stuff some bad values into handles,
  // for example, for debugging purpose, in which case we need the mapping also.
  static address find_slowcase_pc(address pc);
};

#endif // SHARE_PRIMS_JNIFASTGETFIELD_HPP
