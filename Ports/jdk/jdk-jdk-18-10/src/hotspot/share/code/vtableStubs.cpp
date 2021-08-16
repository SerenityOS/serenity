/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "code/vtableStubs.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/disassembler.hpp"
#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/klassVtable.hpp"
#include "oops/oop.inline.hpp"
#include "prims/forte.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/sharedRuntime.hpp"
#include "utilities/align.hpp"
#include "utilities/powerOfTwo.hpp"
#ifdef COMPILER2
#include "opto/matcher.hpp"
#endif

// -----------------------------------------------------------------------------------------
// Implementation of VtableStub

address VtableStub::_chunk             = NULL;
address VtableStub::_chunk_end         = NULL;
VMReg   VtableStub::_receiver_location = VMRegImpl::Bad();


void* VtableStub::operator new(size_t size, int code_size) throw() {
  assert_lock_strong(VtableStubs_lock);
  assert(size == sizeof(VtableStub), "mismatched size");
  // compute real VtableStub size (rounded to nearest word)
  const int real_size = align_up(code_size + (int)sizeof(VtableStub), wordSize);
  // malloc them in chunks to minimize header overhead
  const int chunk_factor = 32;
  if (_chunk == NULL || _chunk + real_size > _chunk_end) {
    const int bytes = chunk_factor * real_size + pd_code_alignment();

   // There is a dependency on the name of the blob in src/share/vm/prims/jvmtiCodeBlobEvents.cpp
   // If changing the name, update the other file accordingly.
    VtableBlob* blob = VtableBlob::create("vtable chunks", bytes);
    if (blob == NULL) {
      return NULL;
    }
    _chunk = blob->content_begin();
    _chunk_end = _chunk + bytes;
    Forte::register_stub("vtable stub", _chunk, _chunk_end);
    align_chunk();
  }
  assert(_chunk + real_size <= _chunk_end, "bad allocation");
  void* res = _chunk;
  _chunk += real_size;
  align_chunk();
 return res;
}


void VtableStub::print_on(outputStream* st) const {
  st->print("vtable stub (index = %d, receiver_location = " INTX_FORMAT ", code = [" INTPTR_FORMAT ", " INTPTR_FORMAT "])",
             index(), p2i(receiver_location()), p2i(code_begin()), p2i(code_end()));
}

void VtableStub::print() const { print_on(tty); }

// -----------------------------------------------------------------------------------------
// Implementation of VtableStubs
//
// For each hash value there's a linked list of vtable stubs (with that
// hash value). Each list is anchored in a little hash _table, indexed
// by that hash value.

VtableStub* VtableStubs::_table[VtableStubs::N];
int VtableStubs::_number_of_vtable_stubs = 0;
int VtableStubs::_vtab_stub_size = 0;
int VtableStubs::_itab_stub_size = 0;

#if defined(PRODUCT)
  // These values are good for the PRODUCT case (no tracing).
  static const int first_vtableStub_size =  64;
  static const int first_itableStub_size = 256;
#else
  // These values are good for the non-PRODUCT case (when tracing can be switched on).
  // To find out, run test workload with
  //   -Xlog:vtablestubs=Trace -XX:+CountCompiledCalls -XX:+DebugVtables
  // and use the reported "estimate" value.
  // Here is a list of observed worst-case values:
  //               vtable  itable
  // aarch64:         460     324
  // arm:               ?       ?
  // ppc (linux, BE): 404     288
  // ppc (linux, LE): 356     276
  // ppc (AIX):       416     296
  // s390x:           408     256
  // Solaris-sparc:   792     348
  // x86 (Linux):     670     309
  // x86 (MacOS):     682     321
  static const int first_vtableStub_size = 1024;
  static const int first_itableStub_size =  512;
#endif


void VtableStubs::initialize() {
  VtableStub::_receiver_location = SharedRuntime::name_for_receiver();
  {
    MutexLocker ml(VtableStubs_lock, Mutex::_no_safepoint_check_flag);
    assert(_number_of_vtable_stubs == 0, "potential performance bug: VtableStubs initialized more than once");
    assert(is_power_of_2(int(N)), "N must be a power of 2");
    for (int i = 0; i < N; i++) {
      _table[i] = NULL;
    }
  }
}


int VtableStubs::code_size_limit(bool is_vtable_stub) {
  if (is_vtable_stub) {
    return _vtab_stub_size > 0 ? _vtab_stub_size : first_vtableStub_size;
  } else { // itable stub
    return _itab_stub_size > 0 ? _itab_stub_size : first_itableStub_size;
  }
}   // code_size_limit


void VtableStubs::check_and_set_size_limit(bool is_vtable_stub,
                                           int  code_size,
                                           int  padding) {
  const char* name = is_vtable_stub ? "vtable" : "itable";

  guarantee(code_size <= code_size_limit(is_vtable_stub),
            "buffer overflow in %s stub, code_size is %d, limit is %d", name, code_size, code_size_limit(is_vtable_stub));

  if (is_vtable_stub) {
    if (log_is_enabled(Trace, vtablestubs)) {
      if ( (_vtab_stub_size > 0) && ((code_size + padding) > _vtab_stub_size) ) {
        log_trace(vtablestubs)("%s size estimate needed adjustment from %d to %d bytes",
                               name, _vtab_stub_size, code_size + padding);
      }
    }
    if ( (code_size + padding) > _vtab_stub_size ) {
      _vtab_stub_size = code_size + padding;
    }
  } else {  // itable stub
    if (log_is_enabled(Trace, vtablestubs)) {
      if ( (_itab_stub_size > 0) && ((code_size + padding) > _itab_stub_size) ) {
        log_trace(vtablestubs)("%s size estimate needed adjustment from %d to %d bytes",
                               name, _itab_stub_size, code_size + padding);
      }
    }
    if ( (code_size + padding) > _itab_stub_size ) {
      _itab_stub_size = code_size + padding;
    }
  }
  return;
}   // check_and_set_size_limit


void VtableStubs::bookkeeping(MacroAssembler* masm, outputStream* out, VtableStub* s,
                              address npe_addr, address ame_addr,   bool is_vtable_stub,
                              int     index,    int     slop_bytes, int  index_dependent_slop) {
  const char* name        = is_vtable_stub ? "vtable" : "itable";
  const int   stub_length = code_size_limit(is_vtable_stub);

  if (log_is_enabled(Trace, vtablestubs)) {
    log_trace(vtablestubs)("%s #%d at " PTR_FORMAT ": size: %d, estimate: %d, slop area: %d",
                           name, index, p2i(s->code_begin()),
                           (int)(masm->pc() - s->code_begin()),
                           stub_length,
                           (int)(s->code_end() - masm->pc()));
  }
  guarantee(masm->pc() <= s->code_end(), "%s #%d: overflowed buffer, estimated len: %d, actual len: %d, overrun: %d",
                                         name, index, stub_length,
                                         (int)(masm->pc() - s->code_begin()),
                                         (int)(masm->pc() - s->code_end()));
  assert((masm->pc() + index_dependent_slop) <= s->code_end(), "%s #%d: spare space for 32-bit offset: required = %d, available = %d",
                                         name, index, index_dependent_slop,
                                         (int)(s->code_end() - masm->pc()));

  // After the first vtable/itable stub is generated, we have a much
  // better estimate for the stub size. Remember/update this
  // estimate after some sanity checks.
  check_and_set_size_limit(is_vtable_stub, masm->offset(), slop_bytes);
  s->set_exception_points(npe_addr, ame_addr);
}


address VtableStubs::find_stub(bool is_vtable_stub, int vtable_index) {
  assert(vtable_index >= 0, "must be positive");

  VtableStub* s;
  {
    MutexLocker ml(VtableStubs_lock, Mutex::_no_safepoint_check_flag);
    s = lookup(is_vtable_stub, vtable_index);
    if (s == NULL) {
      if (is_vtable_stub) {
        s = create_vtable_stub(vtable_index);
      } else {
        s = create_itable_stub(vtable_index);
      }

      // Creation of vtable or itable can fail if there is not enough free space in the code cache.
      if (s == NULL) {
        return NULL;
      }

      enter(is_vtable_stub, vtable_index, s);
      if (PrintAdapterHandlers) {
        tty->print_cr("Decoding VtableStub %s[%d]@" INTX_FORMAT,
                      is_vtable_stub? "vtbl": "itbl", vtable_index, p2i(VtableStub::receiver_location()));
        Disassembler::decode(s->code_begin(), s->code_end());
      }
      // Notify JVMTI about this stub. The event will be recorded by the enclosing
      // JvmtiDynamicCodeEventCollector and posted when this thread has released
      // all locks. Only post this event if a new state is not required. Creating a new state would
      // cause a safepoint and the caller of this code has a NoSafepointVerifier.
      if (JvmtiExport::should_post_dynamic_code_generated()) {
        JvmtiExport::post_dynamic_code_generated_while_holding_locks(is_vtable_stub? "vtable stub": "itable stub",
                                                                     s->code_begin(), s->code_end());
      }
    }
  }
  return s->entry_point();
}


inline uint VtableStubs::hash(bool is_vtable_stub, int vtable_index){
  // Assumption: receiver_location < 4 in most cases.
  int hash = ((vtable_index << 2) ^ VtableStub::receiver_location()->value()) + vtable_index;
  return (is_vtable_stub ? ~hash : hash)  & mask;
}


VtableStub* VtableStubs::lookup(bool is_vtable_stub, int vtable_index) {
  assert_lock_strong(VtableStubs_lock);
  unsigned hash = VtableStubs::hash(is_vtable_stub, vtable_index);
  VtableStub* s = _table[hash];
  while( s && !s->matches(is_vtable_stub, vtable_index)) s = s->next();
  return s;
}


void VtableStubs::enter(bool is_vtable_stub, int vtable_index, VtableStub* s) {
  assert_lock_strong(VtableStubs_lock);
  assert(s->matches(is_vtable_stub, vtable_index), "bad vtable stub");
  unsigned int h = VtableStubs::hash(is_vtable_stub, vtable_index);
  // enter s at the beginning of the corresponding list
  s->set_next(_table[h]);
  _table[h] = s;
  _number_of_vtable_stubs++;
}

VtableStub* VtableStubs::entry_point(address pc) {
  MutexLocker ml(VtableStubs_lock, Mutex::_no_safepoint_check_flag);
  VtableStub* stub = (VtableStub*)(pc - VtableStub::entry_offset());
  uint hash = VtableStubs::hash(stub->is_vtable_stub(), stub->index());
  VtableStub* s;
  for (s = _table[hash]; s != NULL && s != stub; s = s->next()) {}
  return (s == stub) ? s : NULL;
}

bool VtableStubs::contains(address pc) {
  // simple solution for now - we may want to use
  // a faster way if this function is called often
  return stub_containing(pc) != NULL;
}


VtableStub* VtableStubs::stub_containing(address pc) {
  // Note: No locking needed since any change to the data structure
  //       happens with an atomic store into it (we don't care about
  //       consistency with the _number_of_vtable_stubs counter).
  for (int i = 0; i < N; i++) {
    for (VtableStub* s = _table[i]; s != NULL; s = s->next()) {
      if (s->contains(pc)) return s;
    }
  }
  return NULL;
}

void vtableStubs_init() {
  VtableStubs::initialize();
}

void VtableStubs::vtable_stub_do(void f(VtableStub*)) {
    for (int i = 0; i < N; i++) {
        for (VtableStub* s = _table[i]; s != NULL; s = s->next()) {
            f(s);
        }
    }
}


//-----------------------------------------------------------------------------------------------------
// Non-product code
#ifndef PRODUCT

extern "C" void bad_compiled_vtable_index(JavaThread* thread, oop receiver, int index) {
  ResourceMark rm;
  Klass* klass = receiver->klass();
  InstanceKlass* ik = InstanceKlass::cast(klass);
  klassVtable vt = ik->vtable();
  ik->print();
  fatal("bad compiled vtable dispatch: receiver " INTPTR_FORMAT ", "
        "index %d (vtable length %d)",
        p2i(receiver), index, vt.length());
}

#endif // PRODUCT
