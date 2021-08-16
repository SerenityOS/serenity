/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_JFIELDIDWORKAROUND_HPP
#define SHARE_RUNTIME_JFIELDIDWORKAROUND_HPP

class jfieldIDWorkaround: AllStatic {
  // This workaround is because JVMTI doesn't have distinct entry points
  // for methods that use static jfieldIDs and instance jfieldIDs.
  // The workaround is to steal a low-order bit:
  //   a 1 means the jfieldID is an instance jfieldID,
  //             and the rest of the word is the offset of the field.
  //   a 0 means the jfieldID is a static jfieldID,
  //             and the rest of the word is the JNIid*.
  //
  // Another low-order bit is used to mark if an instance field
  // is accompanied by an indication of which class it applies to.
  //
  // Bit-format of a jfieldID (most significant first):
  //  address:30        instance=0:1 checked=0:1
  //  offset:30         instance=1:1 checked=0:1
  //  klass:23 offset:7 instance=1:1 checked=1:1
  //
  // If the offset does not fit in 7 bits, or if the fieldID is
  // not checked, then the checked bit is zero and the rest of
  // the word (30 bits) contains only the offset.
  //
 private:
  enum {
    checked_bits           = 1,
    instance_bits          = 1,
    address_bits           = BitsPerWord - checked_bits - instance_bits,

    large_offset_bits      = address_bits,  // unioned with address
    small_offset_bits      = 7,
    klass_bits             = address_bits - small_offset_bits,

    checked_shift          = 0,
    instance_shift         = checked_shift  + checked_bits,
    address_shift          = instance_shift + instance_bits,

    offset_shift           = address_shift,  // unioned with address
    klass_shift            = offset_shift + small_offset_bits,

    checked_mask_in_place  = right_n_bits(checked_bits)  << checked_shift,
    instance_mask_in_place = right_n_bits(instance_bits) << instance_shift,
#ifndef _WIN64
    large_offset_mask      = right_n_bits(large_offset_bits),
    small_offset_mask      = right_n_bits(small_offset_bits),
    klass_mask             = right_n_bits(klass_bits)
#endif
    };

#ifdef _WIN64
    // These values are too big for Win64
    const static uintptr_t large_offset_mask = right_n_bits(large_offset_bits);
    const static uintptr_t small_offset_mask = right_n_bits(small_offset_bits);
    const static uintptr_t klass_mask        = right_n_bits(klass_bits);
#endif

  // helper routines:
  static bool is_checked_jfieldID(jfieldID id) {
    uintptr_t as_uint = (uintptr_t) id;
    return ((as_uint & checked_mask_in_place) != 0);
  }
  static intptr_t raw_instance_offset(jfieldID id) {
    uintptr_t result = (uintptr_t) id >> address_shift;
    if (VerifyJNIFields && is_checked_jfieldID(id)) {
      result &= small_offset_mask;  // cut off the hash bits
    }
    return (intptr_t)result;
  }
  static intptr_t encode_klass_hash(Klass* k, intptr_t offset);
  static bool             klass_hash_ok(Klass* k, jfieldID id);
  static void  verify_instance_jfieldID(Klass* k, jfieldID id);

 public:
  static bool is_valid_jfieldID(Klass* k, jfieldID id);

  static bool is_instance_jfieldID(Klass* k, jfieldID id) {
    uintptr_t as_uint = (uintptr_t) id;
    return ((as_uint & instance_mask_in_place) != 0);
  }
  static bool is_static_jfieldID(jfieldID id) {
    uintptr_t as_uint = (uintptr_t) id;
    return ((as_uint & instance_mask_in_place) == 0);
  }

  static jfieldID to_instance_jfieldID(Klass* k, int offset) {
    intptr_t as_uint = ((offset & large_offset_mask) << offset_shift) | instance_mask_in_place;
    if (VerifyJNIFields) {
      as_uint |= encode_klass_hash(k, offset);
    }
    jfieldID result = (jfieldID) as_uint;
#ifndef ASSERT
    // always verify in debug mode; switchable in anything else
    if (VerifyJNIFields)
#endif // ASSERT
    {
      verify_instance_jfieldID(k, result);
    }
    assert(raw_instance_offset(result) == (offset & large_offset_mask), "extract right offset");
    return result;
  }

  static intptr_t from_instance_jfieldID(Klass* k, jfieldID id) {
#ifndef ASSERT
    // always verify in debug mode; switchable in anything else
    if (VerifyJNIFields)
#endif // ASSERT
    {
      verify_instance_jfieldID(k, id);
    }
    return raw_instance_offset(id);
  }

  static jfieldID to_static_jfieldID(JNIid* id) {
    assert(id->is_static_field_id(), "from_JNIid, but not static field id");
    jfieldID result = (jfieldID) id;
    assert(from_static_jfieldID(result) == id, "must produce the same static id");
    return result;
  }

  static JNIid* from_static_jfieldID(jfieldID id) {
    assert(jfieldIDWorkaround::is_static_jfieldID(id),
           "to_JNIid, but not static jfieldID");
    JNIid* result = (JNIid*) id;
    assert(result->is_static_field_id(), "to_JNIid, but not static field id");
    return result;
  }

  static jfieldID to_jfieldID(InstanceKlass* k, int offset, bool is_static) {
    if (is_static) {
      JNIid *id = k->jni_id_for(offset);
      debug_only(id->set_is_static_field_id());
      return jfieldIDWorkaround::to_static_jfieldID(id);
    } else {
      return jfieldIDWorkaround::to_instance_jfieldID(k, offset);
    }
  }
};

#endif // SHARE_RUNTIME_JFIELDIDWORKAROUND_HPP
