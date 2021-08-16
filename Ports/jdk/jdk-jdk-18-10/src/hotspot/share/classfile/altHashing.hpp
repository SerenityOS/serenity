/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_ALTHASHING_HPP
#define SHARE_CLASSFILE_ALTHASHING_HPP

#include "jni.h"
#include "memory/allocation.hpp"

/**
 * Implementation of alternate more secure hashing.
 */

class AltHashing : AllStatic {
  friend class AltHashingTest;

  // For the seed computation
  static uint64_t halfsiphash_64(const uint32_t* data, int len);
  static uint64_t halfsiphash_64(uint64_t seed, const uint32_t* data, int len);

 public:
  static uint64_t compute_seed();

  // For Symbols
  static uint32_t halfsiphash_32(uint64_t seed, const uint8_t* data, int len);
  // For Strings
  static uint32_t halfsiphash_32(uint64_t seed, const uint16_t* data, int len);
};
#endif // SHARE_CLASSFILE_ALTHASHING_HPP
