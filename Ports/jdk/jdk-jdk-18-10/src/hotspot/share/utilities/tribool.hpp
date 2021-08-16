/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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

#ifndef SHARE_UTILITIES_TRIBOOL_HPP
#define SHARE_UTILITIES_TRIBOOL_HPP

#include "utilities/globalDefinitions.hpp"

// 2-bit boolean type: H|L
// the high-bit H is set if it's not default value.
// the low-bit L represents true and false.
class TriBool{
  template <size_t SZ, typename T>
  friend class TriBoolArray;

 private:
  unsigned int _value : 2;
  explicit TriBool(u1 raw) : _value(raw & 3) {}

 public:
  TriBool() : _value(0) {}
  TriBool(bool value) : _value(((u1)value) | 2) {}
  TriBool(const TriBool& o): _value(o._value) {}

  TriBool& operator=(bool value) {
    _value = ((u1)value) | 2;
    return *this;
  }

  TriBool& operator=(const TriBool& o) {
    _value = o._value;
    return *this;
  }

  bool is_default() const {
    return !static_cast<bool>(_value >> 1);
  }

  /*explicit*/ operator bool() const {
    return (_value & 1);
  }
};

// compacted array of TriBool
template <size_t SZ, typename T>
class TriBoolArray {
 private:
  class TriBoolAssigner : public TriBool {
   public:
    TriBoolAssigner(T& slot, size_t offset) : TriBool(static_cast<u1>(slot >> offset)),
                                              _slot(slot), _offset(offset) {}

    TriBoolAssigner& operator=(bool newval) {
      _slot ^= ((u1)_value) << _offset;  // reset the tribool
      _value = (u1)newval| 2;
      _slot |= ((u1)_value) << _offset;
      return *this;
    };

    TriBoolAssigner& operator=(TriBool tb) {
      _slot ^= ((u1)_value) << _offset;  // reset the tribool
      _value = (u1)tb._value;
      _slot |= ((u1)_value) << _offset;
      return *this;
    }

   private:
    T& _slot;
    size_t _offset;
  };

 public:
  TriBoolArray() {}

  TriBoolArray(const TriBool& init) {
    fill_in(init);
  }

  TriBool operator[](size_t x) const {
    size_t index = x / (_slot_size);
    size_t offset = x % (_slot_size);
    T raw = (_array[index] >> (2 * offset)) & 3;
    return TriBool(static_cast<u1>(raw));
  }

  TriBoolAssigner operator[](size_t x) {
    size_t index = x / (_slot_size);
    size_t offset = x % (_slot_size);
    return TriBoolAssigner(_array[index], 2 * offset);
  }

  void fill_in(const TriBool& val) {
      if (val.is_default()) {
        memset(_array, 0, _size * sizeof(T));
      }
      else {
        for (size_t i = 0; i < SZ; ++i) {
          (*this)[i] = val;
        }
      }
  }

  void fill_in(const TriBool* beg, const TriBool* end) {
      size_t i = 0;

      while (i < SZ && beg != end) {
        (*this)[i++] = *beg++;
      }
  }

 private:
  const static size_t _bits_in_T = sizeof(T) * 8;   // bits in a byte
  const static size_t _slot_size = _bits_in_T >> 1; // one TriBool occupies 2bits
  const static size_t _size = (2 * SZ + _bits_in_T - 1) / (_bits_in_T);
  T _array[_size];
};

#endif // SHARE_UTILITIES_TRIBOOL_HPP
