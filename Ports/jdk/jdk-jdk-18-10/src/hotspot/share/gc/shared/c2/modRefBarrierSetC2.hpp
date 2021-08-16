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

#ifndef SHARE_GC_SHARED_C2_MODREFBARRIERSETC2_HPP
#define SHARE_GC_SHARED_C2_MODREFBARRIERSETC2_HPP

#include "gc/shared/c2/barrierSetC2.hpp"

class TypeOopPtr;

class ModRefBarrierSetC2: public BarrierSetC2 {
protected:
  virtual void pre_barrier(GraphKit* kit,
                           bool do_load,
                           Node* ctl,
                           Node* obj,
                           Node* adr,
                           uint adr_idx,
                           Node* val,
                           const TypeOopPtr* val_type,
                           Node* pre_val,
                           BasicType bt) const {}

  virtual void post_barrier(GraphKit* kit,
                            Node* ctl,
                            Node* store,
                            Node* obj,
                            Node* adr,
                            uint adr_idx,
                            Node* val,
                            BasicType bt,
                            bool use_precise) const {}

  virtual Node* store_at_resolved(C2Access& access, C2AccessValue& val) const;

  virtual Node* atomic_cmpxchg_val_at_resolved(C2AtomicParseAccess& access, Node* expected_val,
                                               Node* new_val, const Type* value_type) const;
  virtual Node* atomic_cmpxchg_bool_at_resolved(C2AtomicParseAccess& access, Node* expected_val,
                                                Node* new_val, const Type* value_type) const;
  virtual Node* atomic_xchg_at_resolved(C2AtomicParseAccess& access, Node* new_val, const Type* value_type) const;
};

#endif // SHARE_GC_SHARED_C2_MODREFBARRIERSETC2_HPP
