/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_C1_BARRIERSETC1_HPP
#define SHARE_GC_SHARED_C1_BARRIERSETC1_HPP

#include "c1/c1_Decorators.hpp"
#include "c1/c1_LIRGenerator.hpp"
#include "c1/c1_Instruction.hpp"
#include "c1/c1_LIR.hpp"
#include "memory/allocation.hpp"

class LIRGenerator;
class LIRItem;

// The LIRAddressOpr comprises either a LIRItem or a LIR_Opr to describe elements
// of an access in the C1 Access API. Both of them allow asking for the opr() which
// will correspond to either _item.result() or _opr if there is no _item.
class LIRAddressOpr: public StackObj {
  LIRItem* _item;
  LIR_Opr  _opr;
public:
  LIRAddressOpr(LIRItem& item) : _item(&item), _opr(NULL) {}
  LIRAddressOpr(LIR_Opr opr) : _item(NULL), _opr(opr) {}
  LIRAddressOpr(const LIRAddressOpr& other) : _item(other._item), _opr(other._opr) {}

  LIRItem& item() const {
    assert(_item != NULL, "sanity");
    return *_item;
  }

  LIR_Opr opr() const {
    if (_item == NULL) {
      return _opr;
    } else {
      return _item->result();
    }
  }
};

// The LIRAccess class wraps shared context parameters required for performing
// the right access in C1. This includes the address of the offset and the decorators.
class LIRAccess: public StackObj {
  LIRGenerator* _gen;
  DecoratorSet  _decorators;
  LIRAddressOpr _base;
  LIRAddressOpr _offset;
  BasicType     _type;
  LIR_Opr       _resolved_addr;
  CodeEmitInfo* _patch_emit_info;
  CodeEmitInfo* _access_emit_info;

public:
  LIRAccess(LIRGenerator* gen, DecoratorSet decorators,
            LIRAddressOpr base, LIRAddressOpr offset, BasicType type,
            CodeEmitInfo* patch_emit_info = NULL, CodeEmitInfo* access_emit_info = NULL) :
    _gen(gen),
    _decorators(AccessInternal::decorator_fixup(decorators)),
    _base(base),
    _offset(offset),
    _type(type),
    _resolved_addr(NULL),
    _patch_emit_info(patch_emit_info),
    _access_emit_info(access_emit_info) {}

  void load_base()   { _base.item().load_item(); }
  void load_offset() { _offset.item().load_nonconstant(); }

  void load_address() {
    load_base();
    load_offset();
  }

  LIRGenerator* gen() const              { return _gen; }
  CodeEmitInfo*& patch_emit_info()       { return _patch_emit_info; }
  CodeEmitInfo*& access_emit_info()      { return _access_emit_info; }
  LIRAddressOpr& base()                  { return _base; }
  LIRAddressOpr& offset()                { return _offset; }
  BasicType type() const                 { return _type; }
  LIR_Opr resolved_addr() const          { return _resolved_addr; }
  void set_resolved_addr(LIR_Opr addr)   { _resolved_addr = addr; }
  bool is_oop() const                    { return is_reference_type(_type); }
  DecoratorSet decorators() const        { return _decorators; }
  void clear_decorators(DecoratorSet ds) { _decorators &= ~ds; }
  bool is_raw() const                    { return (_decorators & AS_RAW) != 0; }
};

// The BarrierSetC1 class is the main entry point for the GC backend of the Access API in C1.
// It is called by the LIRGenerator::access_* functions, which is the main entry poing for
// access calls in C1.

class BarrierSetC1: public CHeapObj<mtGC> {
protected:
  virtual LIR_Opr resolve_address(LIRAccess& access, bool resolve_in_register);

  virtual void generate_referent_check(LIRAccess& access, LabelObj* cont);

  // Accesses with resolved address
  virtual void store_at_resolved(LIRAccess& access, LIR_Opr value);
  virtual void load_at_resolved(LIRAccess& access, LIR_Opr result);

  virtual LIR_Opr atomic_cmpxchg_at_resolved(LIRAccess& access, LIRItem& cmp_value, LIRItem& new_value);

  virtual LIR_Opr atomic_xchg_at_resolved(LIRAccess& access, LIRItem& value);
  virtual LIR_Opr atomic_add_at_resolved(LIRAccess& access, LIRItem& value);

public:
  virtual void store_at(LIRAccess& access, LIR_Opr value);
  virtual void load_at(LIRAccess& access, LIR_Opr result);
  virtual void load(LIRAccess& access, LIR_Opr result);

  virtual LIR_Opr atomic_cmpxchg_at(LIRAccess& access, LIRItem& cmp_value, LIRItem& new_value);

  virtual LIR_Opr atomic_xchg_at(LIRAccess& access, LIRItem& value);
  virtual LIR_Opr atomic_add_at(LIRAccess& access, LIRItem& value);

  virtual void generate_c1_runtime_stubs(BufferBlob* buffer_blob) {}
};

#endif // SHARE_GC_SHARED_C1_BARRIERSETC1_HPP
