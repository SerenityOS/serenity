/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef SHARE_GC_Z_C2_ZBARRIERSETC2_HPP
#define SHARE_GC_Z_C2_ZBARRIERSETC2_HPP

#include "gc/shared/c2/barrierSetC2.hpp"
#include "memory/allocation.hpp"
#include "opto/node.hpp"
#include "utilities/growableArray.hpp"

const uint8_t ZLoadBarrierElided      = 0;
const uint8_t ZLoadBarrierStrong      = 1;
const uint8_t ZLoadBarrierWeak        = 2;
const uint8_t ZLoadBarrierPhantom     = 4;
const uint8_t ZLoadBarrierNoKeepalive = 8;

class ZLoadBarrierStubC2 : public ResourceObj {
private:
  const MachNode* _node;
  const Address   _ref_addr;
  const Register  _ref;
  const Register  _tmp;
  const uint8_t   _barrier_data;
  Label           _entry;
  Label           _continuation;

  ZLoadBarrierStubC2(const MachNode* node, Address ref_addr, Register ref, Register tmp, uint8_t barrier_data);

public:
  static ZLoadBarrierStubC2* create(const MachNode* node, Address ref_addr, Register ref, Register tmp, uint8_t barrier_data);

  Address ref_addr() const;
  Register ref() const;
  Register tmp() const;
  address slow_path() const;
  RegMask& live() const;
  Label* entry();
  Label* continuation();
};

class ZBarrierSetC2 : public BarrierSetC2 {
private:
  void compute_liveness_at_stubs() const;
  void analyze_dominating_barriers() const;

protected:
  virtual Node* load_at_resolved(C2Access& access, const Type* val_type) const;
  virtual Node* atomic_cmpxchg_val_at_resolved(C2AtomicParseAccess& access,
                                               Node* expected_val,
                                               Node* new_val,
                                               const Type* val_type) const;
  virtual Node* atomic_cmpxchg_bool_at_resolved(C2AtomicParseAccess& access,
                                                Node* expected_val,
                                                Node* new_val,
                                                const Type* value_type) const;
  virtual Node* atomic_xchg_at_resolved(C2AtomicParseAccess& access,
                                        Node* new_val,
                                        const Type* val_type) const;

public:
  virtual void* create_barrier_state(Arena* comp_arena) const;
  virtual bool array_copy_requires_gc_barriers(bool tightly_coupled_alloc,
                                               BasicType type,
                                               bool is_clone,
                                               bool is_clone_instance,
                                               ArrayCopyPhase phase) const;
  virtual void clone_at_expansion(PhaseMacroExpand* phase,
                                  ArrayCopyNode* ac) const;

  virtual void late_barrier_analysis() const;
  virtual int estimate_stub_size() const;
  virtual void emit_stubs(CodeBuffer& cb) const;
};

#endif // SHARE_GC_Z_C2_ZBARRIERSETC2_HPP
