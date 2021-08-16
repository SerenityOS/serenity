/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHJFRSUPPORT_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHJFRSUPPORT_HPP

#include "runtime/vmOperations.hpp"

class VM_ShenandoahSendHeapRegionInfoEvents : public VM_Operation {
public:
  virtual void doit();
  virtual VMOp_Type type() const { return VMOp_HeapIterateOperation; }
};

class ShenandoahJFRSupport {
public:
  static void register_jfr_type_serializers();
};

#endif // SHARE_VM_GC_SHENANDOAH_SHENANDOAHJFRSUPPORT_HPP
