/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "code/codeCache.hpp"
#include "code/compiledIC.hpp"
#include "code/nmethod.hpp"
#include "code/relocInfo.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/flags/flagSetting.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "utilities/align.hpp"
#include "utilities/copy.hpp"

const RelocationHolder RelocationHolder::none; // its type is relocInfo::none


// Implementation of relocInfo

#ifdef ASSERT
relocInfo::relocType relocInfo::check_relocType(relocType type) {
  assert(type != data_prefix_tag, "cannot build a prefix this way");
  assert((type & type_mask) == type, "wrong type");
  return type;
}

void relocInfo::check_offset_and_format(int offset, int format) {
  assert(offset >= 0 && offset < offset_limit(), "offset out off bounds");
  assert(is_aligned(offset, offset_unit), "misaligned offset");
  assert((format & format_mask) == format, "wrong format");
}
#endif // ASSERT

void relocInfo::initialize(CodeSection* dest, Relocation* reloc) {
  relocInfo* data = this+1;  // here's where the data might go
  dest->set_locs_end(data);  // sync end: the next call may read dest.locs_end
  reloc->pack_data_to(dest); // maybe write data into locs, advancing locs_end
  relocInfo* data_limit = dest->locs_end();
  if (data_limit > data) {
    relocInfo suffix = (*this);
    data_limit = this->finish_prefix((short*) data_limit);
    // Finish up with the suffix.  (Hack note: pack_data_to might edit this.)
    *data_limit = suffix;
    dest->set_locs_end(data_limit+1);
  }
}

relocInfo* relocInfo::finish_prefix(short* prefix_limit) {
  assert(sizeof(relocInfo) == sizeof(short), "change this code");
  short* p = (short*)(this+1);
  assert(prefix_limit >= p, "must be a valid span of data");
  int plen = prefix_limit - p;
  if (plen == 0) {
    debug_only(_value = 0xFFFF);
    return this;                         // no data: remove self completely
  }
  if (plen == 1 && fits_into_immediate(p[0])) {
    (*this) = immediate_relocInfo(p[0]); // move data inside self
    return this+1;
  }
  // cannot compact, so just update the count and return the limit pointer
  (*this) = prefix_relocInfo(plen);   // write new datalen
  assert(data() + datalen() == prefix_limit, "pointers must line up");
  return (relocInfo*)prefix_limit;
}

void relocInfo::set_type(relocType t) {
  int old_offset = addr_offset();
  int old_format = format();
  (*this) = relocInfo(t, old_offset, old_format);
  assert(type()==(int)t, "sanity check");
  assert(addr_offset()==old_offset, "sanity check");
  assert(format()==old_format, "sanity check");
}

void relocInfo::change_reloc_info_for_address(RelocIterator *itr, address pc, relocType old_type, relocType new_type) {
  bool found = false;
  while (itr->next() && !found) {
    if (itr->addr() == pc) {
      assert(itr->type()==old_type, "wrong relocInfo type found");
      itr->current()->set_type(new_type);
      found=true;
    }
  }
  assert(found, "no relocInfo found for pc");
}


// ----------------------------------------------------------------------------------------------------
// Implementation of RelocIterator

void RelocIterator::initialize(CompiledMethod* nm, address begin, address limit) {
  initialize_misc();

  if (nm == NULL && begin != NULL) {
    // allow nmethod to be deduced from beginning address
    CodeBlob* cb = CodeCache::find_blob(begin);
    nm = (cb != NULL) ? cb->as_compiled_method_or_null() : NULL;
  }
  guarantee(nm != NULL, "must be able to deduce nmethod from other arguments");

  _code    = nm;
  _current = nm->relocation_begin() - 1;
  _end     = nm->relocation_end();
  _addr    = nm->content_begin();

  // Initialize code sections.
  _section_start[CodeBuffer::SECT_CONSTS] = nm->consts_begin();
  _section_start[CodeBuffer::SECT_INSTS ] = nm->insts_begin() ;
  _section_start[CodeBuffer::SECT_STUBS ] = nm->stub_begin()  ;

  _section_end  [CodeBuffer::SECT_CONSTS] = nm->consts_end()  ;
  _section_end  [CodeBuffer::SECT_INSTS ] = nm->insts_end()   ;
  _section_end  [CodeBuffer::SECT_STUBS ] = nm->stub_end()    ;

  assert(!has_current(), "just checking");
  assert(begin == NULL || begin >= nm->code_begin(), "in bounds");
  assert(limit == NULL || limit <= nm->code_end(),   "in bounds");
  set_limits(begin, limit);
}


RelocIterator::RelocIterator(CodeSection* cs, address begin, address limit) {
  initialize_misc();

  _current = cs->locs_start()-1;
  _end     = cs->locs_end();
  _addr    = cs->start();
  _code    = NULL; // Not cb->blob();

  CodeBuffer* cb = cs->outer();
  assert((int) SECT_LIMIT == CodeBuffer::SECT_LIMIT, "my copy must be equal");
  for (int n = (int) CodeBuffer::SECT_FIRST; n < (int) CodeBuffer::SECT_LIMIT; n++) {
    CodeSection* cs = cb->code_section(n);
    _section_start[n] = cs->start();
    _section_end  [n] = cs->end();
  }

  assert(!has_current(), "just checking");

  assert(begin == NULL || begin >= cs->start(), "in bounds");
  assert(limit == NULL || limit <= cs->end(),   "in bounds");
  set_limits(begin, limit);
}

bool RelocIterator::addr_in_const() const {
  const int n = CodeBuffer::SECT_CONSTS;
  return section_start(n) <= addr() && addr() < section_end(n);
}


void RelocIterator::set_limits(address begin, address limit) {
  _limit = limit;

  // the limit affects this next stuff:
  if (begin != NULL) {
    relocInfo* backup;
    address    backup_addr;
    while (true) {
      backup      = _current;
      backup_addr = _addr;
      if (!next() || addr() >= begin) break;
    }
    // At this point, either we are at the first matching record,
    // or else there is no such record, and !has_current().
    // In either case, revert to the immediatly preceding state.
    _current = backup;
    _addr    = backup_addr;
    set_has_current(false);
  }
}


// All the strange bit-encodings are in here.
// The idea is to encode relocation data which are small integers
// very efficiently (a single extra halfword).  Larger chunks of
// relocation data need a halfword header to hold their size.
void RelocIterator::advance_over_prefix() {
  if (_current->is_datalen()) {
    _data    = (short*) _current->data();
    _datalen =          _current->datalen();
    _current += _datalen + 1;   // skip the embedded data & header
  } else {
    _databuf = _current->immediate();
    _data = &_databuf;
    _datalen = 1;
    _current++;                 // skip the header
  }
  // The client will see the following relocInfo, whatever that is.
  // It is the reloc to which the preceding data applies.
}


void RelocIterator::initialize_misc() {
  set_has_current(false);
  for (int i = (int) CodeBuffer::SECT_FIRST; i < (int) CodeBuffer::SECT_LIMIT; i++) {
    _section_start[i] = NULL;  // these will be lazily computed, if needed
    _section_end  [i] = NULL;
  }
}


Relocation* RelocIterator::reloc() {
  // (take the "switch" out-of-line)
  relocInfo::relocType t = type();
  if (false) {}
  #define EACH_TYPE(name)                             \
  else if (t == relocInfo::name##_type) {             \
    return name##_reloc();                            \
  }
  APPLY_TO_RELOCATIONS(EACH_TYPE);
  #undef EACH_TYPE
  assert(t == relocInfo::none, "must be padding");
  return new(_rh) Relocation(t);
}


//////// Methods for flyweight Relocation types


RelocationHolder RelocationHolder::plus(int offset) const {
  if (offset != 0) {
    switch (type()) {
    case relocInfo::none:
      break;
    case relocInfo::oop_type:
      {
        oop_Relocation* r = (oop_Relocation*)reloc();
        return oop_Relocation::spec(r->oop_index(), r->offset() + offset);
      }
    case relocInfo::metadata_type:
      {
        metadata_Relocation* r = (metadata_Relocation*)reloc();
        return metadata_Relocation::spec(r->metadata_index(), r->offset() + offset);
      }
    default:
      ShouldNotReachHere();
    }
  }
  return (*this);
}

// some relocations can compute their own values
address Relocation::value() {
  ShouldNotReachHere();
  return NULL;
}


void Relocation::set_value(address x) {
  ShouldNotReachHere();
}

void Relocation::const_set_data_value(address x) {
#ifdef _LP64
  if (format() == relocInfo::narrow_oop_in_const) {
    *(narrowOop*)addr() = CompressedOops::encode(cast_to_oop(x));
  } else {
#endif
    *(address*)addr() = x;
#ifdef _LP64
  }
#endif
}

void Relocation::const_verify_data_value(address x) {
#ifdef _LP64
  if (format() == relocInfo::narrow_oop_in_const) {
    guarantee(*(narrowOop*)addr() == CompressedOops::encode(cast_to_oop(x)), "must agree");
  } else {
#endif
    guarantee(*(address*)addr() == x, "must agree");
#ifdef _LP64
  }
#endif
}


RelocationHolder Relocation::spec_simple(relocInfo::relocType rtype) {
  if (rtype == relocInfo::none)  return RelocationHolder::none;
  relocInfo ri = relocInfo(rtype, 0);
  RelocIterator itr;
  itr.set_current(ri);
  itr.reloc();
  return itr._rh;
}

address Relocation::old_addr_for(address newa,
                                 const CodeBuffer* src, CodeBuffer* dest) {
  int sect = dest->section_index_of(newa);
  guarantee(sect != CodeBuffer::SECT_NONE, "lost track of this address");
  address ostart = src->code_section(sect)->start();
  address nstart = dest->code_section(sect)->start();
  return ostart + (newa - nstart);
}

address Relocation::new_addr_for(address olda,
                                 const CodeBuffer* src, CodeBuffer* dest) {
  debug_only(const CodeBuffer* src0 = src);
  int sect = CodeBuffer::SECT_NONE;
  // Look for olda in the source buffer, and all previous incarnations
  // if the source buffer has been expanded.
  for (; src != NULL; src = src->before_expand()) {
    sect = src->section_index_of(olda);
    if (sect != CodeBuffer::SECT_NONE)  break;
  }
  guarantee(sect != CodeBuffer::SECT_NONE, "lost track of this address");
  address ostart = src->code_section(sect)->start();
  address nstart = dest->code_section(sect)->start();
  return nstart + (olda - ostart);
}

void Relocation::normalize_address(address& addr, const CodeSection* dest, bool allow_other_sections) {
  address addr0 = addr;
  if (addr0 == NULL || dest->allocates2(addr0))  return;
  CodeBuffer* cb = dest->outer();
  addr = new_addr_for(addr0, cb, cb);
  assert(allow_other_sections || dest->contains2(addr),
         "addr must be in required section");
}


void CallRelocation::set_destination(address x) {
  pd_set_call_destination(x);
}

void CallRelocation::fix_relocation_after_move(const CodeBuffer* src, CodeBuffer* dest) {
  // Usually a self-relative reference to an external routine.
  // On some platforms, the reference is absolute (not self-relative).
  // The enhanced use of pd_call_destination sorts this all out.
  address orig_addr = old_addr_for(addr(), src, dest);
  address callee    = pd_call_destination(orig_addr);
  // Reassert the callee address, this time in the new copy of the code.
  pd_set_call_destination(callee);
}


//// pack/unpack methods

void oop_Relocation::pack_data_to(CodeSection* dest) {
  short* p = (short*) dest->locs_end();
  p = pack_2_ints_to(p, _oop_index, _offset);
  dest->set_locs_end((relocInfo*) p);
}


void oop_Relocation::unpack_data() {
  unpack_2_ints(_oop_index, _offset);
}

void metadata_Relocation::pack_data_to(CodeSection* dest) {
  short* p = (short*) dest->locs_end();
  p = pack_2_ints_to(p, _metadata_index, _offset);
  dest->set_locs_end((relocInfo*) p);
}


void metadata_Relocation::unpack_data() {
  unpack_2_ints(_metadata_index, _offset);
}


void virtual_call_Relocation::pack_data_to(CodeSection* dest) {
  short*  p     = (short*) dest->locs_end();
  address point =          dest->locs_point();

  normalize_address(_cached_value, dest);
  jint x0 = scaled_offset_null_special(_cached_value, point);
  p = pack_2_ints_to(p, x0, _method_index);
  dest->set_locs_end((relocInfo*) p);
}


void virtual_call_Relocation::unpack_data() {
  jint x0 = 0;
  unpack_2_ints(x0, _method_index);
  address point = addr();
  _cached_value = x0==0? NULL: address_from_scaled_offset(x0, point);
}

void runtime_call_w_cp_Relocation::pack_data_to(CodeSection * dest) {
  short* p = pack_1_int_to((short *)dest->locs_end(), (jint)(_offset >> 2));
  dest->set_locs_end((relocInfo*) p);
}

void runtime_call_w_cp_Relocation::unpack_data() {
  _offset = unpack_1_int() << 2;
}

void static_stub_Relocation::pack_data_to(CodeSection* dest) {
  short* p = (short*) dest->locs_end();
  CodeSection* insts = dest->outer()->insts();
  normalize_address(_static_call, insts);
  p = pack_1_int_to(p, scaled_offset(_static_call, insts->start()));
  dest->set_locs_end((relocInfo*) p);
}

void static_stub_Relocation::unpack_data() {
  address base = binding()->section_start(CodeBuffer::SECT_INSTS);
  jint offset = unpack_1_int();
  _static_call = address_from_scaled_offset(offset, base);
}

void trampoline_stub_Relocation::pack_data_to(CodeSection* dest ) {
  short* p = (short*) dest->locs_end();
  CodeSection* insts = dest->outer()->insts();
  normalize_address(_owner, insts);
  p = pack_1_int_to(p, scaled_offset(_owner, insts->start()));
  dest->set_locs_end((relocInfo*) p);
}

void trampoline_stub_Relocation::unpack_data() {
  address base = binding()->section_start(CodeBuffer::SECT_INSTS);
  _owner = address_from_scaled_offset(unpack_1_int(), base);
}

void external_word_Relocation::pack_data_to(CodeSection* dest) {
  short* p = (short*) dest->locs_end();
#ifndef _LP64
  p = pack_1_int_to(p, (int32_t) (intptr_t)_target);
#else
  jlong t = (jlong) _target;
  int32_t lo = low(t);
  int32_t hi = high(t);
  p = pack_2_ints_to(p, lo, hi);
#endif /* _LP64 */
  dest->set_locs_end((relocInfo*) p);
}


void external_word_Relocation::unpack_data() {
#ifndef _LP64
  _target = (address) (intptr_t)unpack_1_int();
#else
  jint lo, hi;
  unpack_2_ints(lo, hi);
  jlong t = jlong_from(hi, lo);;
  _target = (address) t;
#endif /* _LP64 */
}


void internal_word_Relocation::pack_data_to(CodeSection* dest) {
  short* p = (short*) dest->locs_end();
  normalize_address(_target, dest, true);

  // Check whether my target address is valid within this section.
  // If not, strengthen the relocation type to point to another section.
  int sindex = _section;
  if (sindex == CodeBuffer::SECT_NONE && _target != NULL
      && (!dest->allocates(_target) || _target == dest->locs_point())) {
    sindex = dest->outer()->section_index_of(_target);
    guarantee(sindex != CodeBuffer::SECT_NONE, "must belong somewhere");
    relocInfo* base = dest->locs_end() - 1;
    assert(base->type() == this->type(), "sanity");
    // Change the written type, to be section_word_type instead.
    base->set_type(relocInfo::section_word_type);
  }

  // Note: An internal_word relocation cannot refer to its own instruction,
  // because we reserve "0" to mean that the pointer itself is embedded
  // in the code stream.  We use a section_word relocation for such cases.

  if (sindex == CodeBuffer::SECT_NONE) {
    assert(type() == relocInfo::internal_word_type, "must be base class");
    guarantee(_target == NULL || dest->allocates2(_target), "must be within the given code section");
    jint x0 = scaled_offset_null_special(_target, dest->locs_point());
    assert(!(x0 == 0 && _target != NULL), "correct encoding of null target");
    p = pack_1_int_to(p, x0);
  } else {
    assert(_target != NULL, "sanity");
    CodeSection* sect = dest->outer()->code_section(sindex);
    guarantee(sect->allocates2(_target), "must be in correct section");
    address base = sect->start();
    jint offset = scaled_offset(_target, base);
    assert((uint)sindex < (uint)CodeBuffer::SECT_LIMIT, "sanity");
    assert(CodeBuffer::SECT_LIMIT <= (1 << section_width), "section_width++");
    p = pack_1_int_to(p, (offset << section_width) | sindex);
  }

  dest->set_locs_end((relocInfo*) p);
}


void internal_word_Relocation::unpack_data() {
  jint x0 = unpack_1_int();
  _target = x0==0? NULL: address_from_scaled_offset(x0, addr());
  _section = CodeBuffer::SECT_NONE;
}


void section_word_Relocation::unpack_data() {
  jint    x      = unpack_1_int();
  jint    offset = (x >> section_width);
  int     sindex = (x & ((1<<section_width)-1));
  address base   = binding()->section_start(sindex);

  _section = sindex;
  _target  = address_from_scaled_offset(offset, base);
}

//// miscellaneous methods
oop* oop_Relocation::oop_addr() {
  int n = _oop_index;
  if (n == 0) {
    // oop is stored in the code stream
    return (oop*) pd_address_in_code();
  } else {
    // oop is stored in table at nmethod::oops_begin
    return code()->oop_addr_at(n);
  }
}


oop oop_Relocation::oop_value() {
  // clean inline caches store a special pseudo-null
  if (Universe::contains_non_oop_word(oop_addr())) {
    return NULL;
  }
  return *oop_addr();
}


void oop_Relocation::fix_oop_relocation() {
  if (!oop_is_immediate()) {
    // get the oop from the pool, and re-insert it into the instruction:
    set_value(value());
  }
}


void oop_Relocation::verify_oop_relocation() {
  if (!oop_is_immediate()) {
    // get the oop from the pool, and re-insert it into the instruction:
    verify_value(value());
  }
}

// meta data versions
Metadata** metadata_Relocation::metadata_addr() {
  int n = _metadata_index;
  if (n == 0) {
    // metadata is stored in the code stream
    return (Metadata**) pd_address_in_code();
    } else {
    // metadata is stored in table at nmethod::metadatas_begin
    return code()->metadata_addr_at(n);
    }
  }


Metadata* metadata_Relocation::metadata_value() {
  Metadata* v = *metadata_addr();
  // clean inline caches store a special pseudo-null
  if (v == (Metadata*)Universe::non_oop_word())  v = NULL;
  return v;
  }


void metadata_Relocation::fix_metadata_relocation() {
  if (!metadata_is_immediate()) {
    // get the metadata from the pool, and re-insert it into the instruction:
    pd_fix_value(value());
  }
}

address virtual_call_Relocation::cached_value() {
  assert(_cached_value != NULL && _cached_value < addr(), "must precede ic_call");
  return _cached_value;
}

Method* virtual_call_Relocation::method_value() {
  CompiledMethod* cm = code();
  if (cm == NULL) return (Method*)NULL;
  Metadata* m = cm->metadata_at(_method_index);
  assert(m != NULL || _method_index == 0, "should be non-null for non-zero index");
  assert(m == NULL || m->is_method(), "not a method");
  return (Method*)m;
}

bool virtual_call_Relocation::clear_inline_cache() {
  // No stubs for ICs
  // Clean IC
  ResourceMark rm;
  CompiledIC* icache = CompiledIC_at(this);
  return icache->set_to_clean();
}


void opt_virtual_call_Relocation::pack_data_to(CodeSection* dest) {
  short* p = (short*) dest->locs_end();
  p = pack_1_int_to(p, _method_index);
  dest->set_locs_end((relocInfo*) p);
}

void opt_virtual_call_Relocation::unpack_data() {
  _method_index = unpack_1_int();
}

Method* opt_virtual_call_Relocation::method_value() {
  CompiledMethod* cm = code();
  if (cm == NULL) return (Method*)NULL;
  Metadata* m = cm->metadata_at(_method_index);
  assert(m != NULL || _method_index == 0, "should be non-null for non-zero index");
  assert(m == NULL || m->is_method(), "not a method");
  return (Method*)m;
}

template<typename CompiledICorStaticCall>
static bool set_to_clean_no_ic_refill(CompiledICorStaticCall* ic) {
  guarantee(ic->set_to_clean(), "Should not need transition stubs");
  return true;
}

bool opt_virtual_call_Relocation::clear_inline_cache() {
  // No stubs for ICs
  // Clean IC
  ResourceMark rm;
  CompiledIC* icache = CompiledIC_at(this);
  return set_to_clean_no_ic_refill(icache);
}

address opt_virtual_call_Relocation::static_stub() {
  // search for the static stub who points back to this static call
  address static_call_addr = addr();
  RelocIterator iter(code());
  while (iter.next()) {
    if (iter.type() == relocInfo::static_stub_type) {
      static_stub_Relocation* stub_reloc = iter.static_stub_reloc();
      if (stub_reloc->static_call() == static_call_addr) {
        return iter.addr();
      }
    }
  }
  return NULL;
}

Method* static_call_Relocation::method_value() {
  CompiledMethod* cm = code();
  if (cm == NULL) return (Method*)NULL;
  Metadata* m = cm->metadata_at(_method_index);
  assert(m != NULL || _method_index == 0, "should be non-null for non-zero index");
  assert(m == NULL || m->is_method(), "not a method");
  return (Method*)m;
}

void static_call_Relocation::pack_data_to(CodeSection* dest) {
  short* p = (short*) dest->locs_end();
  p = pack_1_int_to(p, _method_index);
  dest->set_locs_end((relocInfo*) p);
}

void static_call_Relocation::unpack_data() {
  _method_index = unpack_1_int();
}

bool static_call_Relocation::clear_inline_cache() {
  // Safe call site info
  CompiledStaticCall* handler = this->code()->compiledStaticCall_at(this);
  return set_to_clean_no_ic_refill(handler);
}


address static_call_Relocation::static_stub() {
  // search for the static stub who points back to this static call
  address static_call_addr = addr();
  RelocIterator iter(code());
  while (iter.next()) {
    if (iter.type() == relocInfo::static_stub_type) {
      static_stub_Relocation* stub_reloc = iter.static_stub_reloc();
      if (stub_reloc->static_call() == static_call_addr) {
        return iter.addr();
      }
    }
  }
  return NULL;
}

// Finds the trampoline address for a call. If no trampoline stub is
// found NULL is returned which can be handled by the caller.
address trampoline_stub_Relocation::get_trampoline_for(address call, nmethod* code) {
  // There are no relocations available when the code gets relocated
  // because of CodeBuffer expansion.
  if (code->relocation_size() == 0)
    return NULL;

  RelocIterator iter(code, call);
  while (iter.next()) {
    if (iter.type() == relocInfo::trampoline_stub_type) {
      if (iter.trampoline_stub_reloc()->owner() == call) {
        return iter.addr();
      }
    }
  }

  return NULL;
}

bool static_stub_Relocation::clear_inline_cache() {
  // Call stub is only used when calling the interpreted code.
  // It does not really need to be cleared, except that we want to clean out the methodoop.
  CompiledDirectStaticCall::set_stub_to_clean(this);
  return true;
}


void external_word_Relocation::fix_relocation_after_move(const CodeBuffer* src, CodeBuffer* dest) {
  if (_target != NULL) {
    // Probably this reference is absolute,  not relative, so the following is
    // probably a no-op.
    set_value(_target);
  }
  // If target is NULL, this is  an absolute embedded reference to an external
  // location, which means  there is nothing to fix here.  In either case, the
  // resulting target should be an "external" address.
  postcond(src->section_index_of(target()) == CodeBuffer::SECT_NONE);
  postcond(dest->section_index_of(target()) == CodeBuffer::SECT_NONE);
}


address external_word_Relocation::target() {
  address target = _target;
  if (target == NULL) {
    target = pd_get_address_from_code();
  }
  return target;
}


void internal_word_Relocation::fix_relocation_after_move(const CodeBuffer* src, CodeBuffer* dest) {
  address target = _target;
  if (target == NULL) {
    target = new_addr_for(this->target(), src, dest);
  }
  set_value(target);
}


address internal_word_Relocation::target() {
  address target = _target;
  if (target == NULL) {
    if (addr_in_const()) {
      target = *(address*)addr();
    } else {
      target = pd_get_address_from_code();
    }
  }
  return target;
}

//---------------------------------------------------------------------------------
// Non-product code

#ifndef PRODUCT

static const char* reloc_type_string(relocInfo::relocType t) {
  switch (t) {
  #define EACH_CASE(name) \
  case relocInfo::name##_type: \
    return #name;

  APPLY_TO_RELOCATIONS(EACH_CASE);
  #undef EACH_CASE

  case relocInfo::none:
    return "none";
  case relocInfo::data_prefix_tag:
    return "prefix";
  default:
    return "UNKNOWN RELOC TYPE";
  }
}


void RelocIterator::print_current() {
  if (!has_current()) {
    tty->print_cr("(no relocs)");
    return;
  }
  tty->print("relocInfo@" INTPTR_FORMAT " [type=%d(%s) addr=" INTPTR_FORMAT " offset=%d",
             p2i(_current), type(), reloc_type_string((relocInfo::relocType) type()), p2i(_addr), _current->addr_offset());
  if (current()->format() != 0)
    tty->print(" format=%d", current()->format());
  if (datalen() == 1) {
    tty->print(" data=%d", data()[0]);
  } else if (datalen() > 0) {
    tty->print(" data={");
    for (int i = 0; i < datalen(); i++) {
      tty->print("%04x", data()[i] & 0xFFFF);
    }
    tty->print("}");
  }
  tty->print("]");
  switch (type()) {
  case relocInfo::oop_type:
    {
      oop_Relocation* r = oop_reloc();
      oop* oop_addr  = NULL;
      oop  raw_oop   = NULL;
      oop  oop_value = NULL;
      if (code() != NULL || r->oop_is_immediate()) {
        oop_addr  = r->oop_addr();
        raw_oop   = *oop_addr;
        oop_value = r->oop_value();
      }
      tty->print(" | [oop_addr=" INTPTR_FORMAT " *=" INTPTR_FORMAT " offset=%d]",
                 p2i(oop_addr), p2i(raw_oop), r->offset());
      // Do not print the oop by default--we want this routine to
      // work even during GC or other inconvenient times.
      if (WizardMode && oop_value != NULL) {
        tty->print("oop_value=" INTPTR_FORMAT ": ", p2i(oop_value));
        if (oopDesc::is_oop(oop_value)) {
          oop_value->print_value_on(tty);
        }
      }
      break;
    }
  case relocInfo::metadata_type:
    {
      metadata_Relocation* r = metadata_reloc();
      Metadata** metadata_addr  = NULL;
      Metadata*    raw_metadata   = NULL;
      Metadata*    metadata_value = NULL;
      if (code() != NULL || r->metadata_is_immediate()) {
        metadata_addr  = r->metadata_addr();
        raw_metadata   = *metadata_addr;
        metadata_value = r->metadata_value();
      }
      tty->print(" | [metadata_addr=" INTPTR_FORMAT " *=" INTPTR_FORMAT " offset=%d]",
                 p2i(metadata_addr), p2i(raw_metadata), r->offset());
      if (metadata_value != NULL) {
        tty->print("metadata_value=" INTPTR_FORMAT ": ", p2i(metadata_value));
        metadata_value->print_value_on(tty);
      }
      break;
    }
  case relocInfo::external_word_type:
  case relocInfo::internal_word_type:
  case relocInfo::section_word_type:
    {
      DataRelocation* r = (DataRelocation*) reloc();
      tty->print(" | [target=" INTPTR_FORMAT "]", p2i(r->value())); //value==target
      break;
    }
  case relocInfo::static_call_type:
    {
      static_call_Relocation* r = (static_call_Relocation*) reloc();
      tty->print(" | [destination=" INTPTR_FORMAT " metadata=" INTPTR_FORMAT "]",
                 p2i(r->destination()), p2i(r->method_value()));
      break;
    }
  case relocInfo::runtime_call_type:
  case relocInfo::runtime_call_w_cp_type:
    {
      CallRelocation* r = (CallRelocation*) reloc();
      tty->print(" | [destination=" INTPTR_FORMAT "]", p2i(r->destination()));
      break;
    }
  case relocInfo::virtual_call_type:
    {
      virtual_call_Relocation* r = (virtual_call_Relocation*) reloc();
      tty->print(" | [destination=" INTPTR_FORMAT " cached_value=" INTPTR_FORMAT " metadata=" INTPTR_FORMAT "]",
                 p2i(r->destination()), p2i(r->cached_value()), p2i(r->method_value()));
      break;
    }
  case relocInfo::static_stub_type:
    {
      static_stub_Relocation* r = (static_stub_Relocation*) reloc();
      tty->print(" | [static_call=" INTPTR_FORMAT "]", p2i(r->static_call()));
      break;
    }
  case relocInfo::trampoline_stub_type:
    {
      trampoline_stub_Relocation* r = (trampoline_stub_Relocation*) reloc();
      tty->print(" | [trampoline owner=" INTPTR_FORMAT "]", p2i(r->owner()));
      break;
    }
  case relocInfo::opt_virtual_call_type:
    {
      opt_virtual_call_Relocation* r = (opt_virtual_call_Relocation*) reloc();
      tty->print(" | [destination=" INTPTR_FORMAT " metadata=" INTPTR_FORMAT "]",
                 p2i(r->destination()), p2i(r->method_value()));
      break;
    }
  default:
    break;
  }
  tty->cr();
}


void RelocIterator::print() {
  RelocIterator save_this = (*this);
  relocInfo* scan = _current;
  if (!has_current())  scan += 1;  // nothing to scan here!

  bool skip_next = has_current();
  bool got_next;
  while (true) {
    got_next = (skip_next || next());
    skip_next = false;

    tty->print("         @" INTPTR_FORMAT ": ", p2i(scan));
    relocInfo* newscan = _current+1;
    if (!has_current())  newscan -= 1;  // nothing to scan here!
    while (scan < newscan) {
      tty->print("%04x", *(short*)scan & 0xFFFF);
      scan++;
    }
    tty->cr();

    if (!got_next)  break;
    print_current();
  }

  (*this) = save_this;
}

// For the debugger:
extern "C"
void print_blob_locs(nmethod* nm) {
  nm->print();
  RelocIterator iter(nm);
  iter.print();
}
extern "C"
void print_buf_locs(CodeBuffer* cb) {
  FlagSetting fs(PrintRelocations, true);
  cb->print();
}
#endif // !PRODUCT
