/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_BYTES_HPP
#define SHARE_UTILITIES_BYTES_HPP

#include "utilities/macros.hpp"

class Endian : AllStatic {
public:
  enum Order {
    LITTLE,
    BIG,
    JAVA = BIG,
    NATIVE =
#ifdef VM_LITTLE_ENDIAN
    LITTLE
#else
    BIG
#endif
  };

  // Returns true, if the byte ordering used by Java is different from
  // the native byte ordering of the underlying machine.
  static inline bool is_Java_byte_ordering_different() {
    return NATIVE != JAVA;
  }
};

#include CPU_HEADER(bytes)

#endif // SHARE_UTILITIES_BYTES_HPP
