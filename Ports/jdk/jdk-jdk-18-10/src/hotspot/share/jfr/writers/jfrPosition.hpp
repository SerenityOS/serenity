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

#ifndef SHARE_JFR_WRITERS_JFRPOSITION_HPP
#define SHARE_JFR_WRITERS_JFRPOSITION_HPP

#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

template <typename AP> // AllocationPolicy
class Position : public AP {
 private:
  const u1* _start_pos; // logical start
  u1* _current_pos;
  const u1* _end_pos;

 protected:
  const u1* start_pos() const;
  void set_start_pos(const u1* position);
  u1* current_pos();
  void set_current_pos(const u1* new_position);
  void set_current_pos(size_t size);
  const u1* end_pos() const;
  void set_end_pos(const u1* position);
  Position(const u1* start_pos, size_t size);
  Position();

 public:
  size_t available_size() const;
  int64_t used_offset() const;
  int64_t current_offset() const;
  size_t used_size() const;
  void reset();
};

#endif // SHARE_JFR_WRITERS_JFRPOSITION_HPP
