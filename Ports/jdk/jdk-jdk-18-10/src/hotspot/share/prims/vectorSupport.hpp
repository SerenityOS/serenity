/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_VECTORSUPPORT_HPP
#define SHARE_PRIMS_VECTORSUPPORT_HPP

#include "jni.h"
#include "code/debugInfo.hpp"
#include "memory/allocation.hpp"
#include "oops/typeArrayOop.hpp"
#include "runtime/registerMap.hpp"
#include "utilities/exceptions.hpp"

extern "C" {
  void JNICALL JVM_RegisterVectorSupportMethods(JNIEnv* env, jclass vsclass);
}

class VectorSupport : AllStatic {
 private:
  static Handle allocate_vector_payload(InstanceKlass* ik, frame* fr, RegisterMap* reg_map, ScopeValue* payload, TRAPS);
  static Handle allocate_vector_payload_helper(InstanceKlass* ik, frame* fr, RegisterMap* reg_map, Location location, TRAPS);

  static void init_payload_element(typeArrayOop arr, BasicType elem_bt, int index, address addr);

  static BasicType klass2bt(InstanceKlass* ik);
  static jint klass2length(InstanceKlass* ik);

 public:

   // Should be aligned with constants in jdk.internal.vm.vector.VectorSupport
  enum VectorOperation {
    // Unary
    VECTOR_OP_ABS     = 0,
    VECTOR_OP_NEG     = 1,
    VECTOR_OP_SQRT    = 2,

    // Binary
    VECTOR_OP_ADD     = 4,
    VECTOR_OP_SUB     = 5,
    VECTOR_OP_MUL     = 6,
    VECTOR_OP_DIV     = 7,
    VECTOR_OP_MIN     = 8,
    VECTOR_OP_MAX     = 9,
    VECTOR_OP_AND     = 10,
    VECTOR_OP_OR      = 11,
    VECTOR_OP_XOR     = 12,

    // Ternary
    VECTOR_OP_FMA     = 13,

    // Broadcast int
    VECTOR_OP_LSHIFT  = 14,
    VECTOR_OP_RSHIFT  = 15,
    VECTOR_OP_URSHIFT = 16,

    // Convert
    VECTOR_OP_CAST        = 17,
    VECTOR_OP_REINTERPRET = 18,

    // Mask manipulation operations
    VECTOR_OP_MASK_TRUECOUNT = 19,
    VECTOR_OP_MASK_FIRSTTRUE = 20,
    VECTOR_OP_MASK_LASTTRUE  = 21,

    // Vector Math Library
    VECTOR_OP_TAN   = 101,
    VECTOR_OP_TANH  = 102,
    VECTOR_OP_SIN   = 103,
    VECTOR_OP_SINH  = 104,
    VECTOR_OP_COS   = 105,
    VECTOR_OP_COSH  = 106,
    VECTOR_OP_ASIN  = 107,
    VECTOR_OP_ACOS  = 108,
    VECTOR_OP_ATAN  = 109,
    VECTOR_OP_ATAN2 = 110,
    VECTOR_OP_CBRT  = 111,
    VECTOR_OP_LOG   = 112,
    VECTOR_OP_LOG10 = 113,
    VECTOR_OP_LOG1P = 114,
    VECTOR_OP_POW   = 115,
    VECTOR_OP_EXP   = 116,
    VECTOR_OP_EXPM1 = 117,
    VECTOR_OP_HYPOT = 118,

    VECTOR_OP_SVML_START = VECTOR_OP_TAN,
    VECTOR_OP_SVML_END   = VECTOR_OP_HYPOT,
    NUM_SVML_OP = VECTOR_OP_SVML_END - VECTOR_OP_SVML_START + 1
  };

  enum {
    VEC_SIZE_64 = 0,
    VEC_SIZE_128 = 1,
    VEC_SIZE_256 = 2,
    VEC_SIZE_512 = 3,
    NUM_VEC_SIZES = 4
  };

  static const char* svmlname[VectorSupport::NUM_SVML_OP];

  static int vop2ideal(jint vop, BasicType bt);

  static instanceOop allocate_vector(InstanceKlass* holder, frame* fr, RegisterMap* reg_map, ObjectValue* sv, TRAPS);

  static bool is_vector(Klass* klass);
  static bool is_vector_mask(Klass* klass);
  static bool is_vector_shuffle(Klass* klass);
};
#endif // SHARE_PRIMS_VECTORSUPPORT_HPP
