/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ZERO_JNITYPES_ZERO_HPP
#define CPU_ZERO_JNITYPES_ZERO_HPP

#include "jni.h"
#include "memory/allocation.hpp"
#include "oops/oop.hpp"

// This file holds platform-dependent routines used to write primitive jni
// types to the array of arguments passed into JavaCalls::call

class JNITypes : AllStatic {
  // These functions write a java primitive type (in native format)
  // to a java stack slot array to be passed as an argument to JavaCalls:calls.
  // I.e., they are functionally 'push' operations if they have a 'pos'
  // formal parameter.  Note that jlong's and jdouble's are written
  // _in reverse_ of the order in which they appear in the interpreter
  // stack.  This is because call stubs (see stubGenerator_zero.cpp)
  // reverse the argument list constructed by JavaCallArguments (see
  // javaCalls.hpp).

private:
  // Helper routines.
  static inline void    put_int2 (jint *from, jint *to)           { to[0] = from[0]; to[1] = from[1]; }
  static inline void    put_int2 (jint *from, jint *to, int& pos) { put_int2 (from, (jint *)((intptr_t *)to + pos)); pos += 2; }
  static inline void    put_int2r(jint *from, jint *to)           { to[0] = from[1]; to[1] = from[0]; }
  static inline void    put_int2r(jint *from, jint *to, int& pos) { put_int2r(from, (jint *)((intptr_t *)to + pos)); pos += 2; }

public:
  // Ints are stored in native format in one JavaCallArgument slot at *to.
  static inline void    put_int(jint  from, intptr_t *to)               { *(jint *)(to +   0  ) =  from; }
  static inline void    put_int(jint  from, intptr_t *to, int& pos)     { *(jint *)(to + pos++) =  from; }
  static inline void    put_int(jint *from, intptr_t *to, int& pos)     { *(jint *)(to + pos++) = *from; }

#ifdef _LP64
  // Longs are stored in native format in one JavaCallArgument slot at *(to+1).
  static inline void    put_long(jlong  from, intptr_t *to)             { *(jlong *)(to + 1 +   0) =  from; }
  static inline void    put_long(jlong  from, intptr_t *to, int& pos)   { *(jlong *)(to + 1 + pos) =  from; pos += 2; }
  static inline void    put_long(jlong *from, intptr_t *to, int& pos)   { *(jlong *)(to + 1 + pos) = *from; pos += 2; }
#else
  // Longs are stored in reversed native word format in two JavaCallArgument slots at *to.
  // The high half is in *(to+1) and the low half in *to.
  static inline void    put_long(jlong  from, intptr_t *to)            { put_int2r((jint *)&from, (jint *)to); }
  static inline void    put_long(jlong  from, intptr_t *to, int& pos)  { put_int2r((jint *)&from, (jint *)to, pos); }
  static inline void    put_long(jlong *from, intptr_t *to, int& pos)  { put_int2r((jint *) from, (jint *)to, pos); }
#endif

  // Oops are stored in native format in one JavaCallArgument slot at *to.
  static inline void    put_obj(const Handle& from_handle, intptr_t *to, int& pos) { *(to + pos++) =  (intptr_t)from_handle.raw_value(); }
  static inline void    put_obj(jobject       from_handle, intptr_t *to, int& pos) { *(to + pos++) =  (intptr_t)from_handle; }

  // Floats are stored in native format in one JavaCallArgument slot at *to.
  static inline void    put_float(jfloat  from, intptr_t *to)           { *(jfloat *)(to +   0  ) =  from; }
  static inline void    put_float(jfloat  from, intptr_t *to, int& pos) { *(jfloat *)(to + pos++) =  from; }
  static inline void    put_float(jfloat *from, intptr_t *to, int& pos) { *(jfloat *)(to + pos++) = *from; }

#ifdef _LP64
  // Doubles are stored in native word format in one JavaCallArgument slot at *(to+1).
  static inline void    put_double(jdouble  from, intptr_t *to)           { *(jdouble *)(to + 1 +   0) =  from; }
  static inline void    put_double(jdouble  from, intptr_t *to, int& pos) { *(jdouble *)(to + 1 + pos) =  from; pos += 2; }
  static inline void    put_double(jdouble *from, intptr_t *to, int& pos) { *(jdouble *)(to + 1 + pos) = *from; pos += 2; }
#else
  // Doubles are stored in reversed native word format in two JavaCallArgument slots at *to.
  static inline void    put_double(jdouble  from, intptr_t *to)           { put_int2r((jint *)&from, (jint *)to); }
  static inline void    put_double(jdouble  from, intptr_t *to, int& pos) { put_int2r((jint *)&from, (jint *)to, pos); }
  static inline void    put_double(jdouble *from, intptr_t *to, int& pos) { put_int2r((jint *) from, (jint *)to, pos); }
#endif

  // The get_xxx routines, on the other hand, actually _do_ fetch
  // java primitive types from the interpreter stack.
  static inline jint    get_int(intptr_t *from)         { return *(jint *)from; }

#ifdef _LP64
  static inline jlong   get_long(intptr_t *from)        { return *(jlong *)from; }
#else
  static inline jlong   get_long(intptr_t *from)        { return ((jlong)(*(  signed int *)((jint *)from    )) << 32) |
                                                                 ((jlong)(*(unsigned int *)((jint *)from + 1)) <<  0); }
#endif

  static inline oop     get_obj(intptr_t *from)         { return *(oop *)from; }
  static inline jfloat  get_float(intptr_t *from)       { return *(jfloat *)from; }

#ifdef _LP64
  static inline jdouble get_double(intptr_t *from)      { return *(jdouble *)from; }
#else
  static inline jdouble get_double(intptr_t *from)      { jlong jl = ((jlong)(*(  signed int *)((jint *)from    )) << 32) |
                                                                     ((jlong)(*(unsigned int *)((jint *)from + 1)) <<  0);
                                                          return *(jdouble *)&jl; }
#endif

};

#endif // CPU_ZERO_JNITYPES_ZERO_HPP
