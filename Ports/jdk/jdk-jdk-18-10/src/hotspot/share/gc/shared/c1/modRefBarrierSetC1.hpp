/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_C1_MODREFBARRIERSETC1_HPP
#define SHARE_GC_SHARED_C1_MODREFBARRIERSETC1_HPP

#include "gc/shared/c1/barrierSetC1.hpp"

// The ModRefBarrierSetC1 filters away accesses on BasicTypes other
// than T_OBJECT/T_ARRAY (oops). The oop accesses call one of the protected
// accesses, which are overridden in the concrete BarrierSetAssembler.

class ModRefBarrierSetC1 : public BarrierSetC1 {
protected:
  virtual void pre_barrier(LIRAccess& access, LIR_Opr addr_opr,
                           LIR_Opr pre_val, CodeEmitInfo* info) {}
  virtual void post_barrier(LIRAccess& access, LIR_OprDesc* addr,
                            LIR_OprDesc* new_val) {}

  virtual LIR_Opr resolve_address(LIRAccess& access, bool resolve_in_register);

  virtual void store_at_resolved(LIRAccess& access, LIR_Opr value);

  virtual LIR_Opr atomic_cmpxchg_at_resolved(LIRAccess& access, LIRItem& cmp_value, LIRItem& new_value);

  virtual LIR_Opr atomic_xchg_at_resolved(LIRAccess& access, LIRItem& value);
};

#endif // SHARE_GC_SHARED_C1_MODREFBARRIERSETC1_HPP
