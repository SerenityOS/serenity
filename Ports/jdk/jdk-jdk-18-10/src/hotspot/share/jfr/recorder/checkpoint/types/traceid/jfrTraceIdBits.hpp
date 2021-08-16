/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDBITS_HPP
#define SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDBITS_HPP

#include "jni.h"
#include "jfr/utilities/jfrTypes.hpp"
#include "memory/allocation.hpp"

class JfrTraceIdBits : AllStatic {
 public:
  template <typename T>
  static traceid load(const T* ptr);

  template <typename T>
  static void store(jbyte bits, const T* ptr);

  template <typename T>
  static void cas(jbyte bits, const T* ptr);

  template <typename T>
  static void meta_store(jbyte bits, const T* ptr);

  template <typename T>
  static void mask_store(jbyte mask, const T* ptr);

  template <typename T>
  static void meta_mask_store(jbyte mask, const T* ptr);

  template <typename T>
  static void clear(jbyte bits, const T* ptr);

  template <typename T>
  static void clear_cas(jbyte bits, const T* ptr);

  template <typename T>
  static void meta_clear(jbyte bits, const T* ptr);
};

#endif // SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDBITS_HPP
