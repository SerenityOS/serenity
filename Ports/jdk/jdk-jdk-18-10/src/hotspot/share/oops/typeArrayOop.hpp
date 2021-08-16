/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_TYPEARRAYOOP_HPP
#define SHARE_OOPS_TYPEARRAYOOP_HPP

#include "oops/arrayOop.hpp"
#include "oops/typeArrayKlass.hpp"

// A typeArrayOop is an array containing basic types (non oop elements).
// It is used for arrays of {characters, singles, doubles, bytes, shorts, integers, longs}
#include <limits.h>

namespace TypeToBT {
  template<typename T> BasicType to_basic_type();
  template<> inline BasicType to_basic_type<jboolean>() { return T_BOOLEAN; }
  template<> inline BasicType to_basic_type<jbyte>()    { return T_BYTE;    }
  template<> inline BasicType to_basic_type<jchar>()    { return T_CHAR;    }
  template<> inline BasicType to_basic_type<jshort>()   { return T_SHORT;   }
  template<> inline BasicType to_basic_type<jint>()     { return T_INT;     }
  template<> inline BasicType to_basic_type<jlong>()    { return T_LONG;    }
  template<> inline BasicType to_basic_type<jfloat>()   { return T_FLOAT;   }
  template<> inline BasicType to_basic_type<jdouble>()  { return T_DOUBLE;  }
};

class typeArrayOopDesc : public arrayOopDesc {
private:
  template <typename T>
  static BasicType bt() { return TypeToBT::to_basic_type<T>(); }

 protected:
  jchar*    char_base()   const;
  jboolean* bool_base()   const;
  jbyte*    byte_base()   const;
  jint*     int_base()    const;
  jlong*    long_base()   const;
  jshort*   short_base()  const;
  jfloat*   float_base()  const;
  jdouble*  double_base() const;

  friend class TypeArrayKlass;

 public:
  template <typename T>
  static ptrdiff_t element_offset(int index) {
    return arrayOopDesc::base_offset_in_bytes(bt<T>()) + sizeof(T) * index;
  }

  jbyte* byte_at_addr(int which) const;
  jboolean* bool_at_addr(int which) const;
  jchar* char_at_addr(int which) const;
  jint* int_at_addr(int which) const;
  jshort* short_at_addr(int which) const;
  jushort* ushort_at_addr(int which) const;
  jlong* long_at_addr(int which) const;
  jfloat* float_at_addr(int which) const;
  jdouble* double_at_addr(int which) const;

  jbyte byte_at(int which) const;
  void byte_at_put(int which, jbyte contents);

  jboolean bool_at(int which) const;
  void bool_at_put(int which, jboolean contents);

  jchar char_at(int which) const;
  void char_at_put(int which, jchar contents);

  jint int_at(int which) const;
  void int_at_put(int which, jint contents);

  jshort short_at(int which) const;
  void short_at_put(int which, jshort contents);

  jushort ushort_at(int which) const;
  void ushort_at_put(int which, jushort contents);

  jlong long_at(int which) const;
  void long_at_put(int which, jlong contents);

  jfloat float_at(int which) const;
  void float_at_put(int which, jfloat contents);

  jdouble double_at(int which) const;
  void double_at_put(int which, jdouble contents);

  jbyte byte_at_acquire(int which) const;
  void release_byte_at_put(int which, jbyte contents);

  Symbol* symbol_at(int which) const;
  void symbol_at_put(int which, Symbol* contents);

  // Sizing

  // Returns the number of words necessary to hold an array of "len"
  // elements each of the given "byte_size".
 private:
  static int object_size(int lh, int length) {
    int instance_header_size = Klass::layout_helper_header_size(lh);
    int element_shift = Klass::layout_helper_log2_element_size(lh);
    DEBUG_ONLY(BasicType etype = Klass::layout_helper_element_type(lh));
    assert(length <= arrayOopDesc::max_array_length(etype), "no overflow");

    julong size_in_bytes = (juint)length;
    size_in_bytes <<= element_shift;
    size_in_bytes += instance_header_size;
    julong size_in_words = ((size_in_bytes + (HeapWordSize-1)) >> LogHeapWordSize);
    assert(size_in_words <= (julong)max_jint, "no overflow");

    return align_object_size((intptr_t)size_in_words);
  }

 public:
  inline int object_size(const TypeArrayKlass* tk) const;
};

#endif // SHARE_OOPS_TYPEARRAYOOP_HPP
