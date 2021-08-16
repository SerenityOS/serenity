/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1BARRIERSETRUNTIME_HPP
#define SHARE_GC_G1_G1BARRIERSETRUNTIME_HPP

#include "gc/g1/g1CardTable.hpp"
#include "memory/allStatic.hpp"
#include "oops/oopsHierarchy.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

class oopDesc;
class JavaThread;

class G1BarrierSetRuntime: public AllStatic {
public:
  using CardValue = G1CardTable::CardValue;

  // Arraycopy stub generator
  static void write_ref_array_pre_oop_entry(oop* dst, size_t length);
  static void write_ref_array_pre_narrow_oop_entry(narrowOop* dst, size_t length);
  static void write_ref_array_post_entry(HeapWord* dst, size_t length);

  // C2 slow-path runtime calls.
  static void write_ref_field_pre_entry(oopDesc* orig, JavaThread *thread);
  static void write_ref_field_post_entry(volatile CardValue* card_addr, JavaThread* thread);
};

#endif // SHARE_GC_G1_G1BARRIERSETRUNTIME_HPP
