/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_WRITERS_JFRPOSITION_INLINE_HPP
#define SHARE_JFR_WRITERS_JFRPOSITION_INLINE_HPP

#include "jfr/writers/jfrPosition.hpp"

template <typename AP>
inline const u1* Position<AP>::start_pos() const {
  return _start_pos;
}

template <typename AP>
inline void Position<AP>::set_start_pos(const u1* position) {
  _start_pos = position;
}

template <typename AP>
inline u1* Position<AP>::current_pos() {
  return _current_pos;
}

template <typename AP>
inline void Position<AP>::set_current_pos(const u1* new_position) {
  _current_pos = const_cast<u1*>(new_position);
}

template <typename AP>
inline void Position<AP>::set_current_pos(size_t size) {
  _current_pos += size;
}

template <typename AP>
inline const u1* Position<AP>::end_pos() const {
  return _end_pos;
}

template <typename AP>
inline void Position<AP>::set_end_pos(const u1* position) {
  _end_pos = position;
}

template <typename AP>
inline Position<AP>::Position(const u1* start_pos, size_t size) :
  AP(),
  _start_pos(start_pos),
  _current_pos(const_cast<u1*>(start_pos)),
  _end_pos(start_pos + size) {
}

template <typename AP>
inline Position<AP>::Position() : _start_pos(NULL), _current_pos(NULL), _end_pos(NULL) {
}

template <typename AP>
inline size_t Position<AP>::available_size() const {
  return _end_pos - _current_pos;
}

template <typename AP>
inline int64_t Position<AP>::used_offset() const {
  return _current_pos - _start_pos;
}

template <typename AP>
inline int64_t Position<AP>::current_offset() const {
  return this->used_offset();
}

template <typename AP>
inline size_t Position<AP>::used_size() const {
  return (size_t)used_offset();
}

template <typename AP>
inline void Position<AP>::reset() {
  set_current_pos(_start_pos);
}

#endif // SHARE_JFR_WRITERS_JFRPOSITION_INLINE_HPP
