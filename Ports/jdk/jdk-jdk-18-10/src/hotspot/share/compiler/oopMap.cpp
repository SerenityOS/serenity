/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "code/codeBlob.hpp"
#include "code/codeCache.hpp"
#include "code/nmethod.hpp"
#include "code/scopeDesc.hpp"
#include "compiler/oopMap.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/iterator.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/compressedOops.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/signature.hpp"
#include "runtime/stackWatermarkSet.inline.hpp"
#include "utilities/align.hpp"
#include "utilities/lockFreeStack.hpp"
#ifdef COMPILER1
#include "c1/c1_Defs.hpp"
#endif
#ifdef COMPILER2
#include "opto/optoreg.hpp"
#endif
#if INCLUDE_JVMCI
#include "jvmci/jvmci_globals.hpp"
#endif

static_assert(sizeof(oop) == sizeof(intptr_t), "Derived pointer sanity check");

static inline intptr_t derived_pointer_value(derived_pointer p) {
  return static_cast<intptr_t>(p);
}

static inline derived_pointer to_derived_pointer(oop obj) {
  return static_cast<derived_pointer>(cast_from_oop<intptr_t>(obj));
}

static inline intptr_t operator-(derived_pointer p, derived_pointer p1) {
  return derived_pointer_value(p) - derived_pointer_value(p1);
}

static inline derived_pointer operator+(derived_pointer p, intptr_t offset) {
  return static_cast<derived_pointer>(derived_pointer_value(p) + offset);
}

// OopMapStream

OopMapStream::OopMapStream(OopMap* oop_map) {
  _stream = new CompressedReadStream(oop_map->write_stream()->buffer());
  _size = oop_map->omv_count();
  _position = 0;
  _valid_omv = false;
}

OopMapStream::OopMapStream(const ImmutableOopMap* oop_map) {
  _stream = new CompressedReadStream(oop_map->data_addr());
  _size = oop_map->count();
  _position = 0;
  _valid_omv = false;
}

void OopMapStream::find_next() {
  if (_position++ < _size) {
    _omv.read_from(_stream);
    _valid_omv = true;
    return;
  }
  _valid_omv = false;
}


// OopMap

// frame_size units are stack-slots (4 bytes) NOT intptr_t; we can name odd
// slots to hold 4-byte values like ints and floats in the LP64 build.
OopMap::OopMap(int frame_size, int arg_count) {
  // OopMaps are usually quite so small, so pick a small initial size
  set_write_stream(new CompressedWriteStream(32));
  set_omv_count(0);

#ifdef ASSERT
  _locs_length = VMRegImpl::stack2reg(0)->value() + frame_size + arg_count;
  _locs_used   = NEW_RESOURCE_ARRAY(OopMapValue::oop_types, _locs_length);
  for(int i = 0; i < _locs_length; i++) _locs_used[i] = OopMapValue::unused_value;
#endif
}


OopMap::OopMap(OopMap::DeepCopyToken, OopMap* source) {
  // This constructor does a deep copy
  // of the source OopMap.
  set_write_stream(new CompressedWriteStream(source->omv_count() * 2));
  set_omv_count(0);
  set_offset(source->offset());

#ifdef ASSERT
  _locs_length = source->_locs_length;
  _locs_used = NEW_RESOURCE_ARRAY(OopMapValue::oop_types, _locs_length);
  for(int i = 0; i < _locs_length; i++) _locs_used[i] = OopMapValue::unused_value;
#endif

  // We need to copy the entries too.
  for (OopMapStream oms(source); !oms.is_done(); oms.next()) {
    OopMapValue omv = oms.current();
    omv.write_on(write_stream());
    increment_count();
  }
}


OopMap* OopMap::deep_copy() {
  return new OopMap(_deep_copy_token, this);
}

void OopMap::copy_data_to(address addr) const {
  memcpy(addr, write_stream()->buffer(), write_stream()->position());
}

int OopMap::heap_size() const {
  int size = sizeof(OopMap);
  int align = sizeof(void *) - 1;
  size += write_stream()->position();
  // Align to a reasonable ending point
  size = ((size+align) & ~align);
  return size;
}

// frame_size units are stack-slots (4 bytes) NOT intptr_t; we can name odd
// slots to hold 4-byte values like ints and floats in the LP64 build.
void OopMap::set_xxx(VMReg reg, OopMapValue::oop_types x, VMReg optional) {

  assert(reg->value() < _locs_length, "too big reg value for stack size");
  assert( _locs_used[reg->value()] == OopMapValue::unused_value, "cannot insert twice" );
  debug_only( _locs_used[reg->value()] = x; )

  OopMapValue o(reg, x, optional);
  o.write_on(write_stream());
  increment_count();
}


void OopMap::set_oop(VMReg reg) {
  set_xxx(reg, OopMapValue::oop_value, VMRegImpl::Bad());
}


void OopMap::set_narrowoop(VMReg reg) {
  set_xxx(reg, OopMapValue::narrowoop_value, VMRegImpl::Bad());
}


void OopMap::set_callee_saved(VMReg reg, VMReg caller_machine_register ) {
  set_xxx(reg, OopMapValue::callee_saved_value, caller_machine_register);
}


void OopMap::set_derived_oop(VMReg reg, VMReg derived_from_local_register ) {
  if( reg == derived_from_local_register ) {
    // Actually an oop, derived shares storage with base,
    set_oop(reg);
  } else {
    set_xxx(reg, OopMapValue::derived_oop_value, derived_from_local_register);
  }
}

// OopMapSet

OopMapSet::OopMapSet() : _list(MinOopMapAllocation) {}

void OopMapSet::add_gc_map(int pc_offset, OopMap *map ) {
  map->set_offset(pc_offset);

#ifdef ASSERT
  if(_list.length() > 0) {
    OopMap* last = _list.last();
    if (last->offset() == map->offset() ) {
      fatal("OopMap inserted twice");
    }
    if (last->offset() > map->offset()) {
      tty->print_cr( "WARNING, maps not sorted: pc[%d]=%d, pc[%d]=%d",
                      _list.length(),last->offset(),_list.length()+1,map->offset());
    }
  }
#endif // ASSERT

  add(map);
}

static void add_derived_oop(oop* base, derived_pointer* derived, OopClosure* oop_fn) {
#if COMPILER2_OR_JVMCI
  DerivedPointerTable::add(derived, base);
#endif // COMPILER2_OR_JVMCI
}

static void ignore_derived_oop(oop* base, derived_pointer* derived, OopClosure* oop_fn) {
}

static void process_derived_oop(oop* base, derived_pointer* derived, OopClosure* oop_fn) {
  // All derived pointers must be processed before the base pointer of any derived pointer is processed.
  // Otherwise, if two derived pointers use the same base, the second derived pointer will get an obscured
  // offset, if the base pointer is processed in the first derived pointer.
  derived_pointer derived_base = to_derived_pointer(*base);
  intptr_t offset = *derived - derived_base;
  *derived = derived_base;
  oop_fn->do_oop((oop*)derived);
  *derived = *derived + offset;
}


#ifndef PRODUCT
static void trace_codeblob_maps(const frame *fr, const RegisterMap *reg_map) {
  // Print oopmap and regmap
  tty->print_cr("------ ");
  CodeBlob* cb = fr->cb();
  const ImmutableOopMapSet* maps = cb->oop_maps();
  const ImmutableOopMap* map = cb->oop_map_for_return_address(fr->pc());
  map->print();
  if( cb->is_nmethod() ) {
    nmethod* nm = (nmethod*)cb;
    // native wrappers have no scope data, it is implied
    if (nm->is_native_method()) {
      tty->print("bci: 0 (native)");
    } else {
      ScopeDesc* scope  = nm->scope_desc_at(fr->pc());
      tty->print("bci: %d ",scope->bci());
    }
  }
  tty->cr();
  fr->print_on(tty);
  tty->print("     ");
  cb->print_value_on(tty);  tty->cr();
  reg_map->print();
  tty->print_cr("------ ");

}
#endif // PRODUCT

void OopMapSet::oops_do(const frame *fr, const RegisterMap* reg_map, OopClosure* f, DerivedPointerIterationMode mode) {
  switch (mode) {
  case DerivedPointerIterationMode::_directly:
    all_do(fr, reg_map, f, process_derived_oop);
    break;
  case DerivedPointerIterationMode::_with_table:
    all_do(fr, reg_map, f, add_derived_oop);
    break;
  case DerivedPointerIterationMode::_ignore:
    all_do(fr, reg_map, f, ignore_derived_oop);
    break;
  }
}


void OopMapSet::all_do(const frame *fr, const RegisterMap *reg_map,
                       OopClosure* oop_fn, void derived_oop_fn(oop*, derived_pointer*, OopClosure*)) {
  CodeBlob* cb = fr->cb();
  assert(cb != NULL, "no codeblob");

  NOT_PRODUCT(if (TraceCodeBlobStacks) trace_codeblob_maps(fr, reg_map);)

  const ImmutableOopMap* map = cb->oop_map_for_return_address(fr->pc());
  assert(map != NULL, "no ptr map found");

  // handle derived pointers first (otherwise base pointer may be
  // changed before derived pointer offset has been collected)
  {
    for (OopMapStream oms(map); !oms.is_done(); oms.next()) {
      OopMapValue omv = oms.current();
      if (omv.type() != OopMapValue::derived_oop_value) {
        continue;
      }

#ifndef COMPILER2
      COMPILER1_PRESENT(ShouldNotReachHere();)
#if INCLUDE_JVMCI
      if (UseJVMCICompiler) {
        ShouldNotReachHere();
      }
#endif
#endif // !COMPILER2
      derived_pointer* derived_loc = (derived_pointer*)fr->oopmapreg_to_location(omv.reg(),reg_map);
      guarantee(derived_loc != NULL, "missing saved register");
      oop* base_loc = fr->oopmapreg_to_oop_location(omv.content_reg(), reg_map);
      // Ignore NULL oops and decoded NULL narrow oops which
      // equal to CompressedOops::base() when a narrow oop
      // implicit null check is used in compiled code.
      // The narrow_oop_base could be NULL or be the address
      // of the page below heap depending on compressed oops mode.
      if (base_loc != NULL && *base_loc != NULL && !CompressedOops::is_base(*base_loc)) {
        derived_oop_fn(base_loc, derived_loc, oop_fn);
      }
    }
  }

  {
    // We want coop and oop oop_types
    for (OopMapStream oms(map); !oms.is_done(); oms.next()) {
      OopMapValue omv = oms.current();
      oop* loc = fr->oopmapreg_to_oop_location(omv.reg(),reg_map);
      // It should be an error if no location can be found for a
      // register mentioned as contained an oop of some kind.  Maybe
      // this was allowed previously because value_value items might
      // be missing?
      guarantee(loc != NULL, "missing saved register");
      if ( omv.type() == OopMapValue::oop_value ) {
        oop val = *loc;
        if (val == NULL || CompressedOops::is_base(val)) {
          // Ignore NULL oops and decoded NULL narrow oops which
          // equal to CompressedOops::base() when a narrow oop
          // implicit null check is used in compiled code.
          // The narrow_oop_base could be NULL or be the address
          // of the page below heap depending on compressed oops mode.
          continue;
        }
        oop_fn->do_oop(loc);
      } else if ( omv.type() == OopMapValue::narrowoop_value ) {
        narrowOop *nl = (narrowOop*)loc;
#ifndef VM_LITTLE_ENDIAN
        VMReg vmReg = omv.reg();
        if (!vmReg->is_stack()) {
          // compressed oops in registers only take up 4 bytes of an
          // 8 byte register but they are in the wrong part of the
          // word so adjust loc to point at the right place.
          nl = (narrowOop*)((address)nl + 4);
        }
#endif
        oop_fn->do_oop(nl);
      }
    }
  }
}


// Update callee-saved register info for the following frame
void OopMapSet::update_register_map(const frame *fr, RegisterMap *reg_map) {
  ResourceMark rm;
  CodeBlob* cb = fr->cb();
  assert(cb != NULL, "no codeblob");

  // Any reg might be saved by a safepoint handler (see generate_handler_blob).
  assert( reg_map->_update_for_id == NULL || fr->is_older(reg_map->_update_for_id),
         "already updated this map; do not 'update' it twice!" );
  debug_only(reg_map->_update_for_id = fr->id());

  // Check if caller must update oop argument
  assert((reg_map->include_argument_oops() ||
          !cb->caller_must_gc_arguments(reg_map->thread())),
         "include_argument_oops should already be set");

  // Scan through oopmap and find location of all callee-saved registers
  // (we do not do update in place, since info could be overwritten)

  address pc = fr->pc();
  const ImmutableOopMap* map  = cb->oop_map_for_return_address(pc);
  assert(map != NULL, "no ptr map found");
  DEBUG_ONLY(int nof_callee = 0;)

  for (OopMapStream oms(map); !oms.is_done(); oms.next()) {
    OopMapValue omv = oms.current();
    if (omv.type() == OopMapValue::callee_saved_value) {
      VMReg reg = omv.content_reg();
      oop* loc = fr->oopmapreg_to_oop_location(omv.reg(), reg_map);
      reg_map->set_location(reg, (address) loc);
      DEBUG_ONLY(nof_callee++;)
    }
  }

  // Check that runtime stubs save all callee-saved registers
#ifdef COMPILER2
  assert(cb->is_compiled_by_c1() || cb->is_compiled_by_jvmci() || !cb->is_runtime_stub() ||
         (nof_callee >= SAVED_ON_ENTRY_REG_COUNT || nof_callee >= C_SAVED_ON_ENTRY_REG_COUNT),
         "must save all");
#endif // COMPILER2
}

// Printing code is present in product build for -XX:+PrintAssembly.

static
void print_register_type(OopMapValue::oop_types x, VMReg optional,
                         outputStream* st) {
  switch( x ) {
  case OopMapValue::oop_value:
    st->print("Oop");
    break;
  case OopMapValue::narrowoop_value:
    st->print("NarrowOop");
    break;
  case OopMapValue::callee_saved_value:
    st->print("Callers_");
    optional->print_on(st);
    break;
  case OopMapValue::derived_oop_value:
    st->print("Derived_oop_");
    optional->print_on(st);
    break;
  default:
    ShouldNotReachHere();
  }
}

void OopMapValue::print_on(outputStream* st) const {
  reg()->print_on(st);
  st->print("=");
  print_register_type(type(),content_reg(),st);
  st->print(" ");
}

void OopMapValue::print() const { print_on(tty); }

void ImmutableOopMap::print_on(outputStream* st) const {
  OopMapValue omv;
  st->print("ImmutableOopMap {");
  for(OopMapStream oms(this); !oms.is_done(); oms.next()) {
    omv = oms.current();
    omv.print_on(st);
  }
  st->print("}");
}

void ImmutableOopMap::print() const { print_on(tty); }

void OopMap::print_on(outputStream* st) const {
  OopMapValue omv;
  st->print("OopMap {");
  for(OopMapStream oms((OopMap*)this); !oms.is_done(); oms.next()) {
    omv = oms.current();
    omv.print_on(st);
  }
  // Print hex offset in addition.
  st->print("off=%d/0x%x}", (int) offset(), (int) offset());
}

void OopMap::print() const { print_on(tty); }

void ImmutableOopMapSet::print_on(outputStream* st) const {
  const ImmutableOopMap* last = NULL;
  const int len = count();

  st->print_cr("ImmutableOopMapSet contains %d OopMaps", len);

  for (int i = 0; i < len; i++) {
    const ImmutableOopMapPair* pair = pair_at(i);
    const ImmutableOopMap* map = pair->get_from(this);
    if (map != last) {
      st->cr();
      map->print_on(st);
      st->print(" pc offsets: ");
    }
    last = map;
    st->print("%d ", pair->pc_offset());
  }
  st->cr();
}

void ImmutableOopMapSet::print() const { print_on(tty); }

void OopMapSet::print_on(outputStream* st) const {
  const int len = _list.length();

  st->print_cr("OopMapSet contains %d OopMaps", len);

  for( int i = 0; i < len; i++) {
    OopMap* m = at(i);
    st->print_cr("#%d ",i);
    m->print_on(st);
    st->cr();
  }
  st->cr();
}

void OopMapSet::print() const { print_on(tty); }

bool OopMap::equals(const OopMap* other) const {
  if (other->_omv_count != _omv_count) {
    return false;
  }
  if (other->write_stream()->position() != write_stream()->position()) {
    return false;
  }
  if (memcmp(other->write_stream()->buffer(), write_stream()->buffer(), write_stream()->position()) != 0) {
    return false;
  }
  return true;
}

const ImmutableOopMap* ImmutableOopMapSet::find_map_at_offset(int pc_offset) const {
  ImmutableOopMapPair* pairs = get_pairs();
  ImmutableOopMapPair* last  = NULL;

  for (int i = 0; i < _count; ++i) {
    if (pairs[i].pc_offset() >= pc_offset) {
      last = &pairs[i];
      break;
    }
  }

  // Heal Coverity issue: potential index out of bounds access.
  guarantee(last != NULL, "last may not be null");
  assert(last->pc_offset() == pc_offset, "oopmap not found");
  return last->get_from(this);
}

const ImmutableOopMap* ImmutableOopMapPair::get_from(const ImmutableOopMapSet* set) const {
  return set->oopmap_at_offset(_oopmap_offset);
}

ImmutableOopMap::ImmutableOopMap(const OopMap* oopmap) : _count(oopmap->count()) {
  address addr = data_addr();
  oopmap->copy_data_to(addr);
}

#ifdef ASSERT
int ImmutableOopMap::nr_of_bytes() const {
  OopMapStream oms(this);

  while (!oms.is_done()) {
    oms.next();
  }
  return sizeof(ImmutableOopMap) + oms.stream_position();
}
#endif

ImmutableOopMapBuilder::ImmutableOopMapBuilder(const OopMapSet* set) : _set(set), _empty(NULL), _last(NULL), _empty_offset(-1), _last_offset(-1), _offset(0), _required(-1), _new_set(NULL) {
  _mapping = NEW_RESOURCE_ARRAY(Mapping, _set->size());
}

int ImmutableOopMapBuilder::size_for(const OopMap* map) const {
  return align_up((int)sizeof(ImmutableOopMap) + map->data_size(), 8);
}

int ImmutableOopMapBuilder::heap_size() {
  int base = sizeof(ImmutableOopMapSet);
  base = align_up(base, 8);

  // all of ours pc / offset pairs
  int pairs = _set->size() * sizeof(ImmutableOopMapPair);
  pairs = align_up(pairs, 8);

  for (int i = 0; i < _set->size(); ++i) {
    int size = 0;
    OopMap* map = _set->at(i);

    if (is_empty(map)) {
      /* only keep a single empty map in the set */
      if (has_empty()) {
        _mapping[i].set(Mapping::OOPMAP_EMPTY, _empty_offset, 0, map, _empty);
      } else {
        _empty_offset = _offset;
        _empty = map;
        size = size_for(map);
        _mapping[i].set(Mapping::OOPMAP_NEW, _offset, size, map);
      }
    } else if (is_last_duplicate(map)) {
      /* if this entry is identical to the previous one, just point it there */
      _mapping[i].set(Mapping::OOPMAP_DUPLICATE, _last_offset, 0, map, _last);
    } else {
      /* not empty, not an identical copy of the previous entry */
      size = size_for(map);
      _mapping[i].set(Mapping::OOPMAP_NEW, _offset, size, map);
      _last_offset = _offset;
      _last = map;
    }

    assert(_mapping[i]._map == map, "check");
    _offset += size;
  }

  int total = base + pairs + _offset;
  DEBUG_ONLY(total += 8);
  _required = total;
  return total;
}

void ImmutableOopMapBuilder::fill_pair(ImmutableOopMapPair* pair, const OopMap* map, int offset, const ImmutableOopMapSet* set) {
  assert(offset < set->nr_of_bytes(), "check");
  new ((address) pair) ImmutableOopMapPair(map->offset(), offset);
}

int ImmutableOopMapBuilder::fill_map(ImmutableOopMapPair* pair, const OopMap* map, int offset, const ImmutableOopMapSet* set) {
  fill_pair(pair, map, offset, set);
  address addr = (address) pair->get_from(_new_set); // location of the ImmutableOopMap

  new (addr) ImmutableOopMap(map);
  return size_for(map);
}

void ImmutableOopMapBuilder::fill(ImmutableOopMapSet* set, int sz) {
  ImmutableOopMapPair* pairs = set->get_pairs();

  for (int i = 0; i < set->count(); ++i) {
    const OopMap* map = _mapping[i]._map;
    ImmutableOopMapPair* pair = NULL;
    int size = 0;

    if (_mapping[i]._kind == Mapping::OOPMAP_NEW) {
      size = fill_map(&pairs[i], map, _mapping[i]._offset, set);
    } else if (_mapping[i]._kind == Mapping::OOPMAP_DUPLICATE || _mapping[i]._kind == Mapping::OOPMAP_EMPTY) {
      fill_pair(&pairs[i], map, _mapping[i]._offset, set);
    }

    const ImmutableOopMap* nv = set->find_map_at_offset(map->offset());
    assert(memcmp(map->data(), nv->data_addr(), map->data_size()) == 0, "check identity");
  }
}

#ifdef ASSERT
void ImmutableOopMapBuilder::verify(address buffer, int size, const ImmutableOopMapSet* set) {
  for (int i = 0; i < 8; ++i) {
    assert(buffer[size - 8 + i] == (unsigned char) 0xff, "overwritten memory check");
  }

  for (int i = 0; i < set->count(); ++i) {
    const ImmutableOopMapPair* pair = set->pair_at(i);
    assert(pair->oopmap_offset() < set->nr_of_bytes(), "check size");
    const ImmutableOopMap* map = pair->get_from(set);
    int nr_of_bytes = map->nr_of_bytes();
    assert(pair->oopmap_offset() + nr_of_bytes <= set->nr_of_bytes(), "check size + size");
  }
}
#endif

ImmutableOopMapSet* ImmutableOopMapBuilder::generate_into(address buffer) {
  DEBUG_ONLY(memset(&buffer[_required-8], 0xff, 8));

  _new_set = new (buffer) ImmutableOopMapSet(_set, _required);
  fill(_new_set, _required);

  DEBUG_ONLY(verify(buffer, _required, _new_set));

  return _new_set;
}

ImmutableOopMapSet* ImmutableOopMapBuilder::build() {
  _required = heap_size();

  // We need to allocate a chunk big enough to hold the ImmutableOopMapSet and all of its ImmutableOopMaps
  address buffer = NEW_C_HEAP_ARRAY(unsigned char, _required, mtCode);
  return generate_into(buffer);
}

ImmutableOopMapSet* ImmutableOopMapSet::build_from(const OopMapSet* oopmap_set) {
  ResourceMark mark;
  ImmutableOopMapBuilder builder(oopmap_set);
  return builder.build();
}


//------------------------------DerivedPointerTable---------------------------

#if COMPILER2_OR_JVMCI

class DerivedPointerTable::Entry : public CHeapObj<mtCompiler> {
  derived_pointer* _location; // Location of derived pointer, also pointing to base
  intptr_t         _offset;   // Offset from base pointer
  Entry* volatile  _next;

  static Entry* volatile* next_ptr(Entry& entry) { return &entry._next; }

public:
  Entry(derived_pointer* location, intptr_t offset) :
    _location(location), _offset(offset), _next(NULL) {}

  derived_pointer* location() const { return _location; }
  intptr_t offset() const { return _offset; }
  Entry* next() const { return _next; }

  typedef LockFreeStack<Entry, &next_ptr> List;
  static List* _list;
};

DerivedPointerTable::Entry::List* DerivedPointerTable::Entry::_list = NULL;
bool DerivedPointerTable::_active = false;

bool DerivedPointerTable::is_empty() {
  return Entry::_list == NULL || Entry::_list->empty();
}

void DerivedPointerTable::clear() {
  // The first time, we create the list.  Otherwise it should be
  // empty.  If not, then we have probably forgotton to call
  // update_pointers after last GC/Scavenge.
  assert (!_active, "should not be active");
  assert(is_empty(), "table not empty");
  if (Entry::_list == NULL) {
    void* mem = NEW_C_HEAP_OBJ(Entry::List, mtCompiler);
    Entry::_list = ::new (mem) Entry::List();
  }
  _active = true;
}

void DerivedPointerTable::add(derived_pointer* derived_loc, oop *base_loc) {
  assert(Universe::heap()->is_in_or_null(*base_loc), "not an oop");
  assert(derived_loc != (void*)base_loc, "Base and derived in same location");
  derived_pointer base_loc_as_derived_pointer =
    static_cast<derived_pointer>(reinterpret_cast<intptr_t>(base_loc));
  assert(*derived_loc != base_loc_as_derived_pointer, "location already added");
  assert(Entry::_list != NULL, "list must exist");
  assert(is_active(), "table must be active here");
  intptr_t offset = *derived_loc - to_derived_pointer(*base_loc);
  // This assert is invalid because derived pointers can be
  // arbitrarily far away from their base.
  // assert(offset >= -1000000, "wrong derived pointer info");

  if (TraceDerivedPointers) {
    tty->print_cr(
      "Add derived pointer@" INTPTR_FORMAT
      " - Derived: " INTPTR_FORMAT
      " Base: " INTPTR_FORMAT " (@" INTPTR_FORMAT ") (Offset: " INTX_FORMAT ")",
      p2i(derived_loc), derived_pointer_value(*derived_loc), p2i(*base_loc), p2i(base_loc), offset
    );
  }
  // Set derived oop location to point to base.
  *derived_loc = base_loc_as_derived_pointer;
  Entry* entry = new Entry(derived_loc, offset);
  Entry::_list->push(*entry);
}

void DerivedPointerTable::update_pointers() {
  assert(Entry::_list != NULL, "list must exist");
  Entry* entries = Entry::_list->pop_all();
  while (entries != NULL) {
    Entry* entry = entries;
    entries = entry->next();
    derived_pointer* derived_loc = entry->location();
    intptr_t offset  = entry->offset();
    // The derived oop was setup to point to location of base
    oop base = **reinterpret_cast<oop**>(derived_loc);
    assert(Universe::heap()->is_in_or_null(base), "must be an oop");

    derived_pointer derived_base = to_derived_pointer(base);
    *derived_loc = derived_base + offset;
    assert(*derived_loc - derived_base == offset, "sanity check");

    if (TraceDerivedPointers) {
      tty->print_cr("Updating derived pointer@" INTPTR_FORMAT
                    " - Derived: " INTPTR_FORMAT "  Base: " INTPTR_FORMAT " (Offset: " INTX_FORMAT ")",
                    p2i(derived_loc), derived_pointer_value(*derived_loc), p2i(base), offset);
    }

    // Delete entry
    delete entry;
  }
  assert(Entry::_list->empty(), "invariant");
  _active = false;
}

#endif // COMPILER2_OR_JVMCI
