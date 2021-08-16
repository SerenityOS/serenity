/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "cds/archiveBuilder.hpp"
#include "cds/archiveUtils.hpp"
#include "cds/cppVtables.hpp"
#include "cds/dumpAllocStats.hpp"
#include "cds/metaspaceShared.hpp"
#include "classfile/classLoaderDataShared.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionaryShared.hpp"
#include "classfile/vmClasses.hpp"
#include "interpreter/abstractInterpreter.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allStatic.hpp"
#include "memory/memRegion.hpp"
#include "memory/resourceArea.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/oopHandle.inline.hpp"
#include "runtime/arguments.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/thread.hpp"
#include "utilities/align.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/hashtable.inline.hpp"

ArchiveBuilder* ArchiveBuilder::_current = NULL;

ArchiveBuilder::OtherROAllocMark::~OtherROAllocMark() {
  char* newtop = ArchiveBuilder::current()->_ro_region.top();
  ArchiveBuilder::alloc_stats()->record_other_type(int(newtop - _oldtop), true);
}

ArchiveBuilder::SourceObjList::SourceObjList() : _ptrmap(16 * K) {
  _total_bytes = 0;
  _objs = new (ResourceObj::C_HEAP, mtClassShared) GrowableArray<SourceObjInfo*>(128 * K, mtClassShared);
}

ArchiveBuilder::SourceObjList::~SourceObjList() {
  delete _objs;
}

void ArchiveBuilder::SourceObjList::append(MetaspaceClosure::Ref* enclosing_ref, SourceObjInfo* src_info) {
  // Save this source object for copying
  _objs->append(src_info);

  // Prepare for marking the pointers in this source object
  assert(is_aligned(_total_bytes, sizeof(address)), "must be");
  src_info->set_ptrmap_start(_total_bytes / sizeof(address));
  _total_bytes = align_up(_total_bytes + (uintx)src_info->size_in_bytes(), sizeof(address));
  src_info->set_ptrmap_end(_total_bytes / sizeof(address));

  BitMap::idx_t bitmap_size_needed = BitMap::idx_t(src_info->ptrmap_end());
  if (_ptrmap.size() <= bitmap_size_needed) {
    _ptrmap.resize((bitmap_size_needed + 1) * 2);
  }
}

void ArchiveBuilder::SourceObjList::remember_embedded_pointer(SourceObjInfo* src_info, MetaspaceClosure::Ref* ref) {
  // src_obj contains a pointer. Remember the location of this pointer in _ptrmap,
  // so that we can copy/relocate it later. E.g., if we have
  //    class Foo { intx scala; Bar* ptr; }
  //    Foo *f = 0x100;
  // To mark the f->ptr pointer on 64-bit platform, this function is called with
  //    src_info()->obj() == 0x100
  //    ref->addr() == 0x108
  address src_obj = src_info->obj();
  address* field_addr = ref->addr();
  assert(src_info->ptrmap_start() < _total_bytes, "sanity");
  assert(src_info->ptrmap_end() <= _total_bytes, "sanity");
  assert(*field_addr != NULL, "should have checked");

  intx field_offset_in_bytes = ((address)field_addr) - src_obj;
  DEBUG_ONLY(int src_obj_size = src_info->size_in_bytes();)
  assert(field_offset_in_bytes >= 0, "must be");
  assert(field_offset_in_bytes + intx(sizeof(intptr_t)) <= intx(src_obj_size), "must be");
  assert(is_aligned(field_offset_in_bytes, sizeof(address)), "must be");

  BitMap::idx_t idx = BitMap::idx_t(src_info->ptrmap_start() + (uintx)(field_offset_in_bytes / sizeof(address)));
  _ptrmap.set_bit(BitMap::idx_t(idx));
}

class RelocateEmbeddedPointers : public BitMapClosure {
  ArchiveBuilder* _builder;
  address _dumped_obj;
  BitMap::idx_t _start_idx;
public:
  RelocateEmbeddedPointers(ArchiveBuilder* builder, address dumped_obj, BitMap::idx_t start_idx) :
    _builder(builder), _dumped_obj(dumped_obj), _start_idx(start_idx) {}

  bool do_bit(BitMap::idx_t bit_offset) {
    uintx FLAG_MASK = 0x03; // See comments around MetaspaceClosure::FLAG_MASK
    size_t field_offset = size_t(bit_offset - _start_idx) * sizeof(address);
    address* ptr_loc = (address*)(_dumped_obj + field_offset);

    uintx old_p_and_bits = (uintx)(*ptr_loc);
    uintx flag_bits = (old_p_and_bits & FLAG_MASK);
    address old_p = (address)(old_p_and_bits & (~FLAG_MASK));
    address new_p = _builder->get_dumped_addr(old_p);
    uintx new_p_and_bits = ((uintx)new_p) | flag_bits;

    log_trace(cds)("Ref: [" PTR_FORMAT "] -> " PTR_FORMAT " => " PTR_FORMAT,
                   p2i(ptr_loc), p2i(old_p), p2i(new_p));

    ArchivePtrMarker::set_and_mark_pointer(ptr_loc, (address)(new_p_and_bits));
    return true; // keep iterating the bitmap
  }
};

void ArchiveBuilder::SourceObjList::relocate(int i, ArchiveBuilder* builder) {
  SourceObjInfo* src_info = objs()->at(i);
  assert(src_info->should_copy(), "must be");
  BitMap::idx_t start = BitMap::idx_t(src_info->ptrmap_start()); // inclusive
  BitMap::idx_t end = BitMap::idx_t(src_info->ptrmap_end());     // exclusive

  RelocateEmbeddedPointers relocator(builder, src_info->dumped_addr(), start);
  _ptrmap.iterate(&relocator, start, end);
}

ArchiveBuilder::ArchiveBuilder() :
  _current_dump_space(NULL),
  _buffer_bottom(NULL),
  _last_verified_top(NULL),
  _num_dump_regions_used(0),
  _other_region_used_bytes(0),
  _requested_static_archive_bottom(NULL),
  _requested_static_archive_top(NULL),
  _requested_dynamic_archive_bottom(NULL),
  _requested_dynamic_archive_top(NULL),
  _mapped_static_archive_bottom(NULL),
  _mapped_static_archive_top(NULL),
  _buffer_to_requested_delta(0),
  _rw_region("rw", MAX_SHARED_DELTA),
  _ro_region("ro", MAX_SHARED_DELTA),
  _rw_src_objs(),
  _ro_src_objs(),
  _src_obj_table(INITIAL_TABLE_SIZE, MAX_TABLE_SIZE),
  _total_closed_heap_region_size(0),
  _total_open_heap_region_size(0),
  _estimated_metaspaceobj_bytes(0),
  _estimated_hashtable_bytes(0)
{
  _klasses = new (ResourceObj::C_HEAP, mtClassShared) GrowableArray<Klass*>(4 * K, mtClassShared);
  _symbols = new (ResourceObj::C_HEAP, mtClassShared) GrowableArray<Symbol*>(256 * K, mtClassShared);
  _special_refs = new (ResourceObj::C_HEAP, mtClassShared) GrowableArray<SpecialRefInfo>(24 * K, mtClassShared);

  assert(_current == NULL, "must be");
  _current = this;
}

ArchiveBuilder::~ArchiveBuilder() {
  assert(_current == this, "must be");
  _current = NULL;

  clean_up_src_obj_table();

  for (int i = 0; i < _symbols->length(); i++) {
    _symbols->at(i)->decrement_refcount();
  }

  delete _klasses;
  delete _symbols;
  delete _special_refs;
  if (_shared_rs.is_reserved()) {
    _shared_rs.release();
  }
}

bool ArchiveBuilder::is_dumping_full_module_graph() {
  return DumpSharedSpaces && MetaspaceShared::use_full_module_graph();
}

class GatherKlassesAndSymbols : public UniqueMetaspaceClosure {
  ArchiveBuilder* _builder;

public:
  GatherKlassesAndSymbols(ArchiveBuilder* builder) : _builder(builder) {}

  virtual bool do_unique_ref(Ref* ref, bool read_only) {
    return _builder->gather_klass_and_symbol(ref, read_only);
  }
};

bool ArchiveBuilder::gather_klass_and_symbol(MetaspaceClosure::Ref* ref, bool read_only) {
  if (ref->obj() == NULL) {
    return false;
  }
  if (get_follow_mode(ref) != make_a_copy) {
    return false;
  }
  if (ref->msotype() == MetaspaceObj::ClassType) {
    Klass* klass = (Klass*)ref->obj();
    assert(klass->is_klass(), "must be");
    if (!is_excluded(klass)) {
      _klasses->append(klass);
    }
    // See RunTimeClassInfo::get_for()
    _estimated_metaspaceobj_bytes += align_up(BytesPerWord, SharedSpaceObjectAlignment);
  } else if (ref->msotype() == MetaspaceObj::SymbolType) {
    // Make sure the symbol won't be GC'ed while we are dumping the archive.
    Symbol* sym = (Symbol*)ref->obj();
    sym->increment_refcount();
    _symbols->append(sym);
  }

  int bytes = ref->size() * BytesPerWord;
  _estimated_metaspaceobj_bytes += align_up(bytes, SharedSpaceObjectAlignment);

  return true; // recurse
}

void ArchiveBuilder::gather_klasses_and_symbols() {
  ResourceMark rm;
  log_info(cds)("Gathering classes and symbols ... ");
  GatherKlassesAndSymbols doit(this);
  iterate_roots(&doit, /*is_relocating_pointers=*/false);
#if INCLUDE_CDS_JAVA_HEAP
  if (is_dumping_full_module_graph()) {
    ClassLoaderDataShared::iterate_symbols(&doit);
  }
#endif
  doit.finish();

  if (DumpSharedSpaces) {
    // To ensure deterministic contents in the static archive, we need to ensure that
    // we iterate the MetaspaceObjs in a deterministic order. It doesn't matter where
    // the MetaspaceObjs are located originally, as they are copied sequentially into
    // the archive during the iteration.
    //
    // The only issue here is that the symbol table and the system directories may be
    // randomly ordered, so we copy the symbols and klasses into two arrays and sort
    // them deterministically.
    //
    // During -Xshare:dump, the order of Symbol creation is strictly determined by
    // the SharedClassListFile (class loading is done in a single thread and the JIT
    // is disabled). Also, Symbols are allocated in monotonically increasing addresses
    // (see Symbol::operator new(size_t, int)). So if we iterate the Symbols by
    // ascending address order, we ensure that all Symbols are copied into deterministic
    // locations in the archive.
    //
    // TODO: in the future, if we want to produce deterministic contents in the
    // dynamic archive, we might need to sort the symbols alphabetically (also see
    // DynamicArchiveBuilder::sort_methods()).
    sort_symbols_and_fix_hash();
    sort_klasses();

    // TODO -- we need a proper estimate for the archived modules, etc,
    // but this should be enough for now
    _estimated_metaspaceobj_bytes += 200 * 1024 * 1024;
  }
}

int ArchiveBuilder::compare_symbols_by_address(Symbol** a, Symbol** b) {
  if (a[0] < b[0]) {
    return -1;
  } else {
    assert(a[0] > b[0], "Duplicated symbol %s unexpected", (*a)->as_C_string());
    return 1;
  }
}

void ArchiveBuilder::sort_symbols_and_fix_hash() {
  log_info(cds)("Sorting symbols and fixing identity hash ... ");
  os::init_random(0x12345678);
  _symbols->sort(compare_symbols_by_address);
  for (int i = 0; i < _symbols->length(); i++) {
    assert(_symbols->at(i)->is_permanent(), "archived symbols must be permanent");
    _symbols->at(i)->update_identity_hash();
  }
}

int ArchiveBuilder::compare_klass_by_name(Klass** a, Klass** b) {
  return a[0]->name()->fast_compare(b[0]->name());
}

void ArchiveBuilder::sort_klasses() {
  log_info(cds)("Sorting classes ... ");
  _klasses->sort(compare_klass_by_name);
}

size_t ArchiveBuilder::estimate_archive_size() {
  // size of the symbol table and two dictionaries, plus the RunTimeClassInfo's
  size_t symbol_table_est = SymbolTable::estimate_size_for_archive();
  size_t dictionary_est = SystemDictionaryShared::estimate_size_for_archive();
  _estimated_hashtable_bytes = symbol_table_est + dictionary_est;

  size_t total = 0;

  total += _estimated_metaspaceobj_bytes;
  total += _estimated_hashtable_bytes;

  // allow fragmentation at the end of each dump region
  total += _total_dump_regions * MetaspaceShared::core_region_alignment();

  log_info(cds)("_estimated_hashtable_bytes = " SIZE_FORMAT " + " SIZE_FORMAT " = " SIZE_FORMAT,
                symbol_table_est, dictionary_est, _estimated_hashtable_bytes);
  log_info(cds)("_estimated_metaspaceobj_bytes = " SIZE_FORMAT, _estimated_metaspaceobj_bytes);
  log_info(cds)("total estimate bytes = " SIZE_FORMAT, total);

  return align_up(total, MetaspaceShared::core_region_alignment());
}

address ArchiveBuilder::reserve_buffer() {
  size_t buffer_size = estimate_archive_size();
  ReservedSpace rs(buffer_size, MetaspaceShared::core_region_alignment(), os::vm_page_size());
  if (!rs.is_reserved()) {
    log_error(cds)("Failed to reserve " SIZE_FORMAT " bytes of output buffer.", buffer_size);
    vm_direct_exit(0);
  }

  // buffer_bottom is the lowest address of the 2 core regions (rw, ro) when
  // we are copying the class metadata into the buffer.
  address buffer_bottom = (address)rs.base();
  log_info(cds)("Reserved output buffer space at " PTR_FORMAT " [" SIZE_FORMAT " bytes]",
                p2i(buffer_bottom), buffer_size);
  _shared_rs = rs;

  _buffer_bottom = buffer_bottom;
  _last_verified_top = buffer_bottom;
  _current_dump_space = &_rw_region;
  _num_dump_regions_used = 1;
  _other_region_used_bytes = 0;
  _current_dump_space->init(&_shared_rs, &_shared_vs);

  ArchivePtrMarker::initialize(&_ptrmap, &_shared_vs);

  // The bottom of the static archive should be mapped at this address by default.
  _requested_static_archive_bottom = (address)MetaspaceShared::requested_base_address();

  // The bottom of the archive (that I am writing now) should be mapped at this address by default.
  address my_archive_requested_bottom;

  if (DumpSharedSpaces) {
    my_archive_requested_bottom = _requested_static_archive_bottom;
  } else {
    _mapped_static_archive_bottom = (address)MetaspaceObj::shared_metaspace_base();
    _mapped_static_archive_top  = (address)MetaspaceObj::shared_metaspace_top();
    assert(_mapped_static_archive_top >= _mapped_static_archive_bottom, "must be");
    size_t static_archive_size = _mapped_static_archive_top - _mapped_static_archive_bottom;

    // At run time, we will mmap the dynamic archive at my_archive_requested_bottom
    _requested_static_archive_top = _requested_static_archive_bottom + static_archive_size;
    my_archive_requested_bottom = align_up(_requested_static_archive_top, MetaspaceShared::core_region_alignment());

    _requested_dynamic_archive_bottom = my_archive_requested_bottom;
  }

  _buffer_to_requested_delta = my_archive_requested_bottom - _buffer_bottom;

  address my_archive_requested_top = my_archive_requested_bottom + buffer_size;
  if (my_archive_requested_bottom <  _requested_static_archive_bottom ||
      my_archive_requested_top    <= _requested_static_archive_bottom) {
    // Size overflow.
    log_error(cds)("my_archive_requested_bottom = " INTPTR_FORMAT, p2i(my_archive_requested_bottom));
    log_error(cds)("my_archive_requested_top    = " INTPTR_FORMAT, p2i(my_archive_requested_top));
    log_error(cds)("SharedBaseAddress (" INTPTR_FORMAT ") is too high. "
                   "Please rerun java -Xshare:dump with a lower value", p2i(_requested_static_archive_bottom));
    vm_direct_exit(0);
  }

  if (DumpSharedSpaces) {
    // We don't want any valid object to be at the very bottom of the archive.
    // See ArchivePtrMarker::mark_pointer().
    rw_region()->allocate(16);
  }

  return buffer_bottom;
}

void ArchiveBuilder::iterate_sorted_roots(MetaspaceClosure* it, bool is_relocating_pointers) {
  int i;

  if (!is_relocating_pointers) {
    // Don't relocate _symbol, so we can safely call decrement_refcount on the
    // original symbols.
    int num_symbols = _symbols->length();
    for (i = 0; i < num_symbols; i++) {
      it->push(_symbols->adr_at(i));
    }
  }

  int num_klasses = _klasses->length();
  for (i = 0; i < num_klasses; i++) {
    it->push(_klasses->adr_at(i));
  }

  iterate_roots(it, is_relocating_pointers);
}

class GatherSortedSourceObjs : public MetaspaceClosure {
  ArchiveBuilder* _builder;

public:
  GatherSortedSourceObjs(ArchiveBuilder* builder) : _builder(builder) {}

  virtual bool do_ref(Ref* ref, bool read_only) {
    return _builder->gather_one_source_obj(enclosing_ref(), ref, read_only);
  }

  virtual void push_special(SpecialRef type, Ref* ref, intptr_t* p) {
    assert(type == _method_entry_ref, "only special type allowed for now");
    address src_obj = ref->obj();
    size_t field_offset = pointer_delta(p, src_obj,  sizeof(u1));
    _builder->add_special_ref(type, src_obj, field_offset);
  };

  virtual void do_pending_ref(Ref* ref) {
    if (ref->obj() != NULL) {
      _builder->remember_embedded_pointer_in_copied_obj(enclosing_ref(), ref);
    }
  }
};

bool ArchiveBuilder::gather_one_source_obj(MetaspaceClosure::Ref* enclosing_ref,
                                           MetaspaceClosure::Ref* ref, bool read_only) {
  address src_obj = ref->obj();
  if (src_obj == NULL) {
    return false;
  }
  ref->set_keep_after_pushing();
  remember_embedded_pointer_in_copied_obj(enclosing_ref, ref);

  FollowMode follow_mode = get_follow_mode(ref);
  SourceObjInfo src_info(ref, read_only, follow_mode);
  bool created;
  SourceObjInfo* p = _src_obj_table.put_if_absent(src_obj, src_info, &created);
  if (created) {
    if (_src_obj_table.maybe_grow()) {
      log_info(cds, hashtables)("Expanded _src_obj_table table to %d", _src_obj_table.table_size());
    }
  }

  assert(p->read_only() == src_info.read_only(), "must be");

  if (created && src_info.should_copy()) {
    ref->set_user_data((void*)p);
    if (read_only) {
      _ro_src_objs.append(enclosing_ref, p);
    } else {
      _rw_src_objs.append(enclosing_ref, p);
    }
    return true; // Need to recurse into this ref only if we are copying it
  } else {
    return false;
  }
}

void ArchiveBuilder::add_special_ref(MetaspaceClosure::SpecialRef type, address src_obj, size_t field_offset) {
  _special_refs->append(SpecialRefInfo(type, src_obj, field_offset));
}

void ArchiveBuilder::remember_embedded_pointer_in_copied_obj(MetaspaceClosure::Ref* enclosing_ref,
                                                             MetaspaceClosure::Ref* ref) {
  assert(ref->obj() != NULL, "should have checked");

  if (enclosing_ref != NULL) {
    SourceObjInfo* src_info = (SourceObjInfo*)enclosing_ref->user_data();
    if (src_info == NULL) {
      // source objects of point_to_it/set_to_null types are not copied
      // so we don't need to remember their pointers.
    } else {
      if (src_info->read_only()) {
        _ro_src_objs.remember_embedded_pointer(src_info, ref);
      } else {
        _rw_src_objs.remember_embedded_pointer(src_info, ref);
      }
    }
  }
}

void ArchiveBuilder::gather_source_objs() {
  ResourceMark rm;
  log_info(cds)("Gathering all archivable objects ... ");
  gather_klasses_and_symbols();
  GatherSortedSourceObjs doit(this);
  iterate_sorted_roots(&doit, /*is_relocating_pointers=*/false);
  doit.finish();
}

bool ArchiveBuilder::is_excluded(Klass* klass) {
  if (klass->is_instance_klass()) {
    InstanceKlass* ik = InstanceKlass::cast(klass);
    return SystemDictionaryShared::is_excluded_class(ik);
  } else if (klass->is_objArray_klass()) {
    if (DynamicDumpSharedSpaces) {
      // Don't support archiving of array klasses for now (WHY???)
      return true;
    }
    Klass* bottom = ObjArrayKlass::cast(klass)->bottom_klass();
    if (bottom->is_instance_klass()) {
      return SystemDictionaryShared::is_excluded_class(InstanceKlass::cast(bottom));
    }
  }

  return false;
}

ArchiveBuilder::FollowMode ArchiveBuilder::get_follow_mode(MetaspaceClosure::Ref *ref) {
  address obj = ref->obj();
  if (MetaspaceShared::is_in_shared_metaspace(obj)) {
    // Don't dump existing shared metadata again.
    return point_to_it;
  } else if (ref->msotype() == MetaspaceObj::MethodDataType) {
    return set_to_null;
  } else {
    if (ref->msotype() == MetaspaceObj::ClassType) {
      Klass* klass = (Klass*)ref->obj();
      assert(klass->is_klass(), "must be");
      if (is_excluded(klass)) {
        ResourceMark rm;
        log_debug(cds, dynamic)("Skipping class (excluded): %s", klass->external_name());
        return set_to_null;
      }
    }

    return make_a_copy;
  }
}

void ArchiveBuilder::start_dump_space(DumpRegion* next) {
  address bottom = _last_verified_top;
  address top = (address)(current_dump_space()->top());
  _other_region_used_bytes += size_t(top - bottom);

  current_dump_space()->pack(next);
  _current_dump_space = next;
  _num_dump_regions_used ++;

  _last_verified_top = (address)(current_dump_space()->top());
}

void ArchiveBuilder::verify_estimate_size(size_t estimate, const char* which) {
  address bottom = _last_verified_top;
  address top = (address)(current_dump_space()->top());
  size_t used = size_t(top - bottom) + _other_region_used_bytes;
  int diff = int(estimate) - int(used);

  log_info(cds)("%s estimate = " SIZE_FORMAT " used = " SIZE_FORMAT "; diff = %d bytes", which, estimate, used, diff);
  assert(diff >= 0, "Estimate is too small");

  _last_verified_top = top;
  _other_region_used_bytes = 0;
}

void ArchiveBuilder::dump_rw_metadata() {
  ResourceMark rm;
  log_info(cds)("Allocating RW objects ... ");
  make_shallow_copies(&_rw_region, &_rw_src_objs);

#if INCLUDE_CDS_JAVA_HEAP
  if (is_dumping_full_module_graph()) {
    // Archive the ModuleEntry's and PackageEntry's of the 3 built-in loaders
    char* start = rw_region()->top();
    ClassLoaderDataShared::allocate_archived_tables();
    alloc_stats()->record_modules(rw_region()->top() - start, /*read_only*/false);
  }
#endif
}

void ArchiveBuilder::dump_ro_metadata() {
  ResourceMark rm;
  log_info(cds)("Allocating RO objects ... ");

  start_dump_space(&_ro_region);
  make_shallow_copies(&_ro_region, &_ro_src_objs);

#if INCLUDE_CDS_JAVA_HEAP
  if (is_dumping_full_module_graph()) {
    char* start = ro_region()->top();
    ClassLoaderDataShared::init_archived_tables();
    alloc_stats()->record_modules(ro_region()->top() - start, /*read_only*/true);
  }
#endif
}

void ArchiveBuilder::make_shallow_copies(DumpRegion *dump_region,
                                         const ArchiveBuilder::SourceObjList* src_objs) {
  for (int i = 0; i < src_objs->objs()->length(); i++) {
    make_shallow_copy(dump_region, src_objs->objs()->at(i));
  }
  log_info(cds)("done (%d objects)", src_objs->objs()->length());
}

void ArchiveBuilder::make_shallow_copy(DumpRegion *dump_region, SourceObjInfo* src_info) {
  MetaspaceClosure::Ref* ref = src_info->ref();
  address src = ref->obj();
  int bytes = src_info->size_in_bytes();
  char* dest;
  char* oldtop;
  char* newtop;

  oldtop = dump_region->top();
  if (ref->msotype() == MetaspaceObj::ClassType) {
    // Save a pointer immediate in front of an InstanceKlass, so
    // we can do a quick lookup from InstanceKlass* -> RunTimeClassInfo*
    // without building another hashtable. See RunTimeClassInfo::get_for()
    // in systemDictionaryShared.cpp.
    Klass* klass = (Klass*)src;
    if (klass->is_instance_klass()) {
      SystemDictionaryShared::validate_before_archiving(InstanceKlass::cast(klass));
      dump_region->allocate(sizeof(address));
    }
  }
  dest = dump_region->allocate(bytes);
  newtop = dump_region->top();

  memcpy(dest, src, bytes);

  intptr_t* archived_vtable = CppVtables::get_archived_vtable(ref->msotype(), (address)dest);
  if (archived_vtable != NULL) {
    *(address*)dest = (address)archived_vtable;
    ArchivePtrMarker::mark_pointer((address*)dest);
  }

  log_trace(cds)("Copy: " PTR_FORMAT " ==> " PTR_FORMAT " %d", p2i(src), p2i(dest), bytes);
  src_info->set_dumped_addr((address)dest);

  _alloc_stats.record(ref->msotype(), int(newtop - oldtop), src_info->read_only());
}

address ArchiveBuilder::get_dumped_addr(address src_obj) const {
  SourceObjInfo* p = _src_obj_table.get(src_obj);
  assert(p != NULL, "must be");

  return p->dumped_addr();
}

void ArchiveBuilder::relocate_embedded_pointers(ArchiveBuilder::SourceObjList* src_objs) {
  for (int i = 0; i < src_objs->objs()->length(); i++) {
    src_objs->relocate(i, this);
  }
}

void ArchiveBuilder::update_special_refs() {
  for (int i = 0; i < _special_refs->length(); i++) {
    SpecialRefInfo s = _special_refs->at(i);
    size_t field_offset = s.field_offset();
    address src_obj = s.src_obj();
    address dst_obj = get_dumped_addr(src_obj);
    intptr_t* src_p = (intptr_t*)(src_obj + field_offset);
    intptr_t* dst_p = (intptr_t*)(dst_obj + field_offset);
    assert(s.type() == MetaspaceClosure::_method_entry_ref, "only special type allowed for now");

    assert(*src_p == *dst_p, "must be a copy");
    ArchivePtrMarker::mark_pointer((address*)dst_p);
  }
}

class RefRelocator: public MetaspaceClosure {
  ArchiveBuilder* _builder;

public:
  RefRelocator(ArchiveBuilder* builder) : _builder(builder) {}

  virtual bool do_ref(Ref* ref, bool read_only) {
    if (ref->not_null()) {
      ref->update(_builder->get_dumped_addr(ref->obj()));
      ArchivePtrMarker::mark_pointer(ref->addr());
    }
    return false; // Do not recurse.
  }
};

void ArchiveBuilder::relocate_roots() {
  log_info(cds)("Relocating external roots ... ");
  ResourceMark rm;
  RefRelocator doit(this);
  iterate_sorted_roots(&doit, /*is_relocating_pointers=*/true);
  doit.finish();
  log_info(cds)("done");
}

void ArchiveBuilder::relocate_metaspaceobj_embedded_pointers() {
  log_info(cds)("Relocating embedded pointers in core regions ... ");
  relocate_embedded_pointers(&_rw_src_objs);
  relocate_embedded_pointers(&_ro_src_objs);
  update_special_refs();
}

// We must relocate vmClasses::_klasses[] only after we have copied the
// java objects in during dump_java_heap_objects(): during the object copy, we operate on
// old objects which assert that their klass is the original klass.
void ArchiveBuilder::relocate_vm_classes() {
  log_info(cds)("Relocating vmClasses::_klasses[] ... ");
  ResourceMark rm;
  RefRelocator doit(this);
  vmClasses::metaspace_pointers_do(&doit);
}

void ArchiveBuilder::make_klasses_shareable() {
  int num_instance_klasses = 0;
  int num_boot_klasses = 0;
  int num_platform_klasses = 0;
  int num_app_klasses = 0;
  int num_hidden_klasses = 0;
  int num_unlinked_klasses = 0;
  int num_unregistered_klasses = 0;
  int num_obj_array_klasses = 0;
  int num_type_array_klasses = 0;

  for (int i = 0; i < klasses()->length(); i++) {
    const char* type;
    const char* unlinked = "";
    const char* hidden = "";
    Klass* k = klasses()->at(i);
    k->remove_java_mirror();
    if (k->is_objArray_klass()) {
      // InstanceKlass and TypeArrayKlass will in turn call remove_unshareable_info
      // on their array classes.
      num_obj_array_klasses ++;
      type = "array";
    } else if (k->is_typeArray_klass()) {
      num_type_array_klasses ++;
      type = "array";
      k->remove_unshareable_info();
    } else {
      assert(k->is_instance_klass(), " must be");
      num_instance_klasses ++;
      InstanceKlass* ik = InstanceKlass::cast(k);
      if (DynamicDumpSharedSpaces) {
        // For static dump, class loader type are already set.
        ik->assign_class_loader_type();
      }
      if (ik->is_shared_boot_class()) {
        type = "boot";
        num_boot_klasses ++;
      } else if (ik->is_shared_platform_class()) {
        type = "plat";
        num_platform_klasses ++;
      } else if (ik->is_shared_app_class()) {
        type = "app";
        num_app_klasses ++;
      } else {
        assert(ik->is_shared_unregistered_class(), "must be");
        type = "unreg";
        num_unregistered_klasses ++;
      }

      if (!ik->is_linked()) {
        num_unlinked_klasses ++;
        unlinked = " ** unlinked";
      }

      if (ik->is_hidden()) {
        num_hidden_klasses ++;
        hidden = " ** hidden";
      }

      MetaspaceShared::rewrite_nofast_bytecodes_and_calculate_fingerprints(Thread::current(), ik);
      ik->remove_unshareable_info();
    }

    if (log_is_enabled(Debug, cds, class)) {
      ResourceMark rm;
      log_debug(cds, class)("klasses[%5d] = " PTR_FORMAT " %-5s %s%s%s", i, p2i(to_requested(k)), type, k->external_name(), hidden, unlinked);
    }
  }

  log_info(cds)("Number of classes %d", num_instance_klasses + num_obj_array_klasses + num_type_array_klasses);
  log_info(cds)("    instance classes   = %5d", num_instance_klasses);
  log_info(cds)("      boot             = %5d", num_boot_klasses);
  log_info(cds)("      app              = %5d", num_app_klasses);
  log_info(cds)("      platform         = %5d", num_platform_klasses);
  log_info(cds)("      unregistered     = %5d", num_unregistered_klasses);
  log_info(cds)("      (hidden)         = %5d", num_hidden_klasses);
  log_info(cds)("      (unlinked)       = %5d", num_unlinked_klasses);
  log_info(cds)("    obj array classes  = %5d", num_obj_array_klasses);
  log_info(cds)("    type array classes = %5d", num_type_array_klasses);
  log_info(cds)("               symbols = %5d", _symbols->length());
}

uintx ArchiveBuilder::buffer_to_offset(address p) const {
  address requested_p = to_requested(p);
  assert(requested_p >= _requested_static_archive_bottom, "must be");
  return requested_p - _requested_static_archive_bottom;
}

uintx ArchiveBuilder::any_to_offset(address p) const {
  if (is_in_mapped_static_archive(p)) {
    assert(DynamicDumpSharedSpaces, "must be");
    return p - _mapped_static_archive_bottom;
  }
  return buffer_to_offset(p);
}

// Update a Java object to point its Klass* to the new location after
// shared archive has been compacted.
void ArchiveBuilder::relocate_klass_ptr(oop o) {
  assert(DumpSharedSpaces, "sanity");
  Klass* k = get_relocated_klass(o->klass());
  Klass* requested_k = to_requested(k);
  narrowKlass nk = CompressedKlassPointers::encode_not_null(requested_k, _requested_static_archive_bottom);
  o->set_narrow_klass(nk);
}

// RelocateBufferToRequested --- Relocate all the pointers in rw/ro,
// so that the archive can be mapped to the "requested" location without runtime relocation.
//
// - See ArchiveBuilder header for the definition of "buffer", "mapped" and "requested"
// - ArchivePtrMarker::ptrmap() marks all the pointers in the rw/ro regions
// - Every pointer must have one of the following values:
//   [a] NULL:
//       No relocation is needed. Remove this pointer from ptrmap so we don't need to
//       consider it at runtime.
//   [b] Points into an object X which is inside the buffer:
//       Adjust this pointer by _buffer_to_requested_delta, so it points to X
//       when the archive is mapped at the requested location.
//   [c] Points into an object Y which is inside mapped static archive:
//       - This happens only during dynamic dump
//       - Adjust this pointer by _mapped_to_requested_static_archive_delta,
//         so it points to Y when the static archive is mapped at the requested location.
template <bool STATIC_DUMP>
class RelocateBufferToRequested : public BitMapClosure {
  ArchiveBuilder* _builder;
  address _buffer_bottom;
  intx _buffer_to_requested_delta;
  intx _mapped_to_requested_static_archive_delta;
  size_t _max_non_null_offset;

 public:
  RelocateBufferToRequested(ArchiveBuilder* builder) {
    _builder = builder;
    _buffer_bottom = _builder->buffer_bottom();
    _buffer_to_requested_delta = builder->buffer_to_requested_delta();
    _mapped_to_requested_static_archive_delta = builder->requested_static_archive_bottom() - builder->mapped_static_archive_bottom();
    _max_non_null_offset = 0;

    address bottom = _builder->buffer_bottom();
    address top = _builder->buffer_top();
    address new_bottom = bottom + _buffer_to_requested_delta;
    address new_top = top + _buffer_to_requested_delta;
    log_debug(cds)("Relocating archive from [" INTPTR_FORMAT " - " INTPTR_FORMAT "] to "
                   "[" INTPTR_FORMAT " - " INTPTR_FORMAT "]",
                   p2i(bottom), p2i(top),
                   p2i(new_bottom), p2i(new_top));
  }

  bool do_bit(size_t offset) {
    address* p = (address*)_buffer_bottom + offset;
    assert(_builder->is_in_buffer_space(p), "pointer must live in buffer space");

    if (*p == NULL) {
      // todo -- clear bit, etc
      ArchivePtrMarker::ptrmap()->clear_bit(offset);
    } else {
      if (STATIC_DUMP) {
        assert(_builder->is_in_buffer_space(*p), "old pointer must point inside buffer space");
        *p += _buffer_to_requested_delta;
        assert(_builder->is_in_requested_static_archive(*p), "new pointer must point inside requested archive");
      } else {
        if (_builder->is_in_buffer_space(*p)) {
          *p += _buffer_to_requested_delta;
          // assert is in requested dynamic archive
        } else {
          assert(_builder->is_in_mapped_static_archive(*p), "old pointer must point inside buffer space or mapped static archive");
          *p += _mapped_to_requested_static_archive_delta;
          assert(_builder->is_in_requested_static_archive(*p), "new pointer must point inside requested archive");
        }
      }
      _max_non_null_offset = offset;
    }

    return true; // keep iterating
  }

  void doit() {
    ArchivePtrMarker::ptrmap()->iterate(this);
    ArchivePtrMarker::compact(_max_non_null_offset);
  }
};


void ArchiveBuilder::relocate_to_requested() {
  ro_region()->pack();

  size_t my_archive_size = buffer_top() - buffer_bottom();

  if (DumpSharedSpaces) {
    _requested_static_archive_top = _requested_static_archive_bottom + my_archive_size;
    RelocateBufferToRequested<true> patcher(this);
    patcher.doit();
  } else {
    assert(DynamicDumpSharedSpaces, "must be");
    _requested_dynamic_archive_top = _requested_dynamic_archive_bottom + my_archive_size;
    RelocateBufferToRequested<false> patcher(this);
    patcher.doit();
  }
}

// Write detailed info to a mapfile to analyze contents of the archive.
// static dump:
//   java -Xshare:dump -Xlog:cds+map=trace:file=cds.map:none:filesize=0
// dynamic dump:
//   java -cp MyApp.jar -XX:ArchiveClassesAtExit=MyApp.jsa \
//        -Xlog:cds+map=trace:file=cds.map:none:filesize=0 MyApp
//
// We need to do some address translation because the buffers used at dump time may be mapped to
// a different location at runtime. At dump time, the buffers may be at arbitrary locations
// picked by the OS. At runtime, we try to map at a fixed location (SharedBaseAddress). For
// consistency, we log everything using runtime addresses.
class ArchiveBuilder::CDSMapLogger : AllStatic {
  static intx buffer_to_runtime_delta() {
    // Translate the buffers used by the RW/RO regions to their eventual (requested) locations
    // at runtime.
    return ArchiveBuilder::current()->buffer_to_requested_delta();
  }

  // rw/ro regions only
  static void write_dump_region(const char* name, DumpRegion* region) {
    address region_base = address(region->base());
    address region_top  = address(region->top());
    write_region(name, region_base, region_top, region_base + buffer_to_runtime_delta());
  }

#define _LOG_PREFIX PTR_FORMAT ": @@ %-17s %d"

  static void write_klass(Klass* k, address runtime_dest, const char* type_name, int bytes, Thread* current) {
    ResourceMark rm(current);
    log_debug(cds, map)(_LOG_PREFIX " %s",
                        p2i(runtime_dest), type_name, bytes, k->external_name());
  }
  static void write_method(Method* m, address runtime_dest, const char* type_name, int bytes, Thread* current) {
    ResourceMark rm(current);
    log_debug(cds, map)(_LOG_PREFIX " %s",
                        p2i(runtime_dest), type_name, bytes,  m->external_name());
  }

  // rw/ro regions only
  static void write_objects(DumpRegion* region, const ArchiveBuilder::SourceObjList* src_objs) {
    address last_obj_base = address(region->base());
    address last_obj_end  = address(region->base());
    address region_end    = address(region->end());
    Thread* current = Thread::current();
    for (int i = 0; i < src_objs->objs()->length(); i++) {
      SourceObjInfo* src_info = src_objs->at(i);
      address src = src_info->orig_obj();
      address dest = src_info->dumped_addr();
      write_data(last_obj_base, dest, last_obj_base + buffer_to_runtime_delta());
      address runtime_dest = dest + buffer_to_runtime_delta();
      int bytes = src_info->size_in_bytes();

      MetaspaceObj::Type type = src_info->msotype();
      const char* type_name = MetaspaceObj::type_name(type);

      switch (type) {
      case MetaspaceObj::ClassType:
        write_klass((Klass*)src, runtime_dest, type_name, bytes, current);
        break;
      case MetaspaceObj::ConstantPoolType:
        write_klass(((ConstantPool*)src)->pool_holder(),
                    runtime_dest, type_name, bytes, current);
        break;
      case MetaspaceObj::ConstantPoolCacheType:
        write_klass(((ConstantPoolCache*)src)->constant_pool()->pool_holder(),
                    runtime_dest, type_name, bytes, current);
        break;
      case MetaspaceObj::MethodType:
        write_method((Method*)src, runtime_dest, type_name, bytes, current);
        break;
      case MetaspaceObj::ConstMethodType:
        write_method(((ConstMethod*)src)->method(), runtime_dest, type_name, bytes, current);
        break;
      case MetaspaceObj::SymbolType:
        {
          ResourceMark rm(current);
          Symbol* s = (Symbol*)src;
          log_debug(cds, map)(_LOG_PREFIX " %s", p2i(runtime_dest), type_name, bytes,
                              s->as_quoted_ascii());
        }
        break;
      default:
        log_debug(cds, map)(_LOG_PREFIX, p2i(runtime_dest), type_name, bytes);
        break;
      }

      last_obj_base = dest;
      last_obj_end  = dest + bytes;
    }

    write_data(last_obj_base, last_obj_end, last_obj_base + buffer_to_runtime_delta());
    if (last_obj_end < region_end) {
      log_debug(cds, map)(PTR_FORMAT ": @@ Misc data " SIZE_FORMAT " bytes",
                          p2i(last_obj_end + buffer_to_runtime_delta()),
                          size_t(region_end - last_obj_end));
      write_data(last_obj_end, region_end, last_obj_end + buffer_to_runtime_delta());
    }
  }

#undef _LOG_PREFIX

  // Write information about a region, whose address at dump time is [base .. top). At
  // runtime, this region will be mapped to runtime_base.  runtime_base is 0 if this
  // region will be mapped at os-selected addresses (such as the bitmap region), or will
  // be accessed with os::read (the header).
  static void write_region(const char* name, address base, address top, address runtime_base) {
    size_t size = top - base;
    base = runtime_base;
    top = runtime_base + size;
    log_info(cds, map)("[%-18s " PTR_FORMAT " - " PTR_FORMAT " " SIZE_FORMAT_W(9) " bytes]",
                       name, p2i(base), p2i(top), size);
  }

  // open and closed archive regions
  static void write_heap_region(const char* which, GrowableArray<MemRegion> *regions) {
    for (int i = 0; i < regions->length(); i++) {
      address start = address(regions->at(i).start());
      address end = address(regions->at(i).end());
      write_region(which, start, end, start);
      write_data(start, end, start);
    }
  }

  // Dump all the data [base...top). Pretend that the base address
  // will be mapped to runtime_base at run-time.
  static void write_data(address base, address top, address runtime_base) {
    assert(top >= base, "must be");

    LogStreamHandle(Trace, cds, map) lsh;
    if (lsh.is_enabled()) {
      os::print_hex_dump(&lsh, base, top, sizeof(address), 32, runtime_base);
    }
  }

  static void write_header(FileMapInfo* mapinfo) {
    LogStreamHandle(Info, cds, map) lsh;
    if (lsh.is_enabled()) {
      mapinfo->print(&lsh);
    }
  }

public:
  static void write(ArchiveBuilder* builder, FileMapInfo* mapinfo,
             GrowableArray<MemRegion> *closed_heap_regions,
             GrowableArray<MemRegion> *open_heap_regions,
             char* bitmap, size_t bitmap_size_in_bytes) {
    log_info(cds, map)("%s CDS archive map for %s", DumpSharedSpaces ? "Static" : "Dynamic", mapinfo->full_path());

    address header = address(mapinfo->header());
    address header_end = header + mapinfo->header()->header_size();
    write_region("header", header, header_end, 0);
    write_header(mapinfo);
    write_data(header, header_end, 0);

    DumpRegion* rw_region = &builder->_rw_region;
    DumpRegion* ro_region = &builder->_ro_region;

    write_dump_region("rw region", rw_region);
    write_objects(rw_region, &builder->_rw_src_objs);

    write_dump_region("ro region", ro_region);
    write_objects(ro_region, &builder->_ro_src_objs);

    address bitmap_end = address(bitmap + bitmap_size_in_bytes);
    write_region("bitmap", address(bitmap), bitmap_end, 0);
    write_data(header, header_end, 0);

    if (closed_heap_regions != NULL) {
      write_heap_region("closed heap region", closed_heap_regions);
    }
    if (open_heap_regions != NULL) {
      write_heap_region("open heap region", open_heap_regions);
    }

    log_info(cds, map)("[End of CDS archive map]");
  }
};

void ArchiveBuilder::print_stats() {
  _alloc_stats.print_stats(int(_ro_region.used()), int(_rw_region.used()));
}

void ArchiveBuilder::clean_up_src_obj_table() {
  SrcObjTableCleaner cleaner;
  _src_obj_table.iterate(&cleaner);
}

void ArchiveBuilder::write_archive(FileMapInfo* mapinfo,
                                   GrowableArray<MemRegion>* closed_heap_regions,
                                   GrowableArray<MemRegion>* open_heap_regions,
                                   GrowableArray<ArchiveHeapOopmapInfo>* closed_heap_oopmaps,
                                   GrowableArray<ArchiveHeapOopmapInfo>* open_heap_oopmaps) {
  // Make sure NUM_CDS_REGIONS (exported in cds.h) agrees with
  // MetaspaceShared::n_regions (internal to hotspot).
  assert(NUM_CDS_REGIONS == MetaspaceShared::n_regions, "sanity");

  write_region(mapinfo, MetaspaceShared::rw, &_rw_region, /*read_only=*/false,/*allow_exec=*/false);
  write_region(mapinfo, MetaspaceShared::ro, &_ro_region, /*read_only=*/true, /*allow_exec=*/false);

  size_t bitmap_size_in_bytes;
  char* bitmap = mapinfo->write_bitmap_region(ArchivePtrMarker::ptrmap(), closed_heap_oopmaps, open_heap_oopmaps,
                                              bitmap_size_in_bytes);

  if (closed_heap_regions != NULL) {
    _total_closed_heap_region_size = mapinfo->write_heap_regions(
                                        closed_heap_regions,
                                        closed_heap_oopmaps,
                                        MetaspaceShared::first_closed_heap_region,
                                        MetaspaceShared::max_closed_heap_region);
    _total_open_heap_region_size = mapinfo->write_heap_regions(
                                        open_heap_regions,
                                        open_heap_oopmaps,
                                        MetaspaceShared::first_open_heap_region,
                                        MetaspaceShared::max_open_heap_region);
  }

  print_region_stats(mapinfo, closed_heap_regions, open_heap_regions);

  mapinfo->set_requested_base((char*)MetaspaceShared::requested_base_address());
  if (mapinfo->header()->magic() == CDS_DYNAMIC_ARCHIVE_MAGIC) {
    mapinfo->set_header_base_archive_name_size(strlen(Arguments::GetSharedArchivePath()) + 1);
    mapinfo->set_header_base_archive_is_default(FLAG_IS_DEFAULT(SharedArchiveFile));
  }
  mapinfo->set_header_crc(mapinfo->compute_header_crc());
  // After this point, we should not write any data into mapinfo->header() since this
  // would corrupt its checksum we have calculated before.
  mapinfo->write_header();
  mapinfo->close();

  if (log_is_enabled(Info, cds)) {
    print_stats();
  }

  if (log_is_enabled(Info, cds, map)) {
    CDSMapLogger::write(this, mapinfo, closed_heap_regions, open_heap_regions,
                        bitmap, bitmap_size_in_bytes);
  }
  FREE_C_HEAP_ARRAY(char, bitmap);
}

void ArchiveBuilder::write_region(FileMapInfo* mapinfo, int region_idx, DumpRegion* dump_region, bool read_only,  bool allow_exec) {
  mapinfo->write_region(region_idx, dump_region->base(), dump_region->used(), read_only, allow_exec);
}

void ArchiveBuilder::print_region_stats(FileMapInfo *mapinfo,
                                        GrowableArray<MemRegion>* closed_heap_regions,
                                        GrowableArray<MemRegion>* open_heap_regions) {
  // Print statistics of all the regions
  const size_t bitmap_used = mapinfo->space_at(MetaspaceShared::bm)->used();
  const size_t bitmap_reserved = mapinfo->space_at(MetaspaceShared::bm)->used_aligned();
  const size_t total_reserved = _ro_region.reserved()  + _rw_region.reserved() +
                                bitmap_reserved +
                                _total_closed_heap_region_size +
                                _total_open_heap_region_size;
  const size_t total_bytes = _ro_region.used()  + _rw_region.used() +
                             bitmap_used +
                             _total_closed_heap_region_size +
                             _total_open_heap_region_size;
  const double total_u_perc = percent_of(total_bytes, total_reserved);

  _rw_region.print(total_reserved);
  _ro_region.print(total_reserved);

  print_bitmap_region_stats(bitmap_used, total_reserved);

  if (closed_heap_regions != NULL) {
    print_heap_region_stats(closed_heap_regions, "ca", total_reserved);
    print_heap_region_stats(open_heap_regions, "oa", total_reserved);
  }

  log_debug(cds)("total    : " SIZE_FORMAT_W(9) " [100.0%% of total] out of " SIZE_FORMAT_W(9) " bytes [%5.1f%% used]",
                 total_bytes, total_reserved, total_u_perc);
}

void ArchiveBuilder::print_bitmap_region_stats(size_t size, size_t total_size) {
  log_debug(cds)("bm  space: " SIZE_FORMAT_W(9) " [ %4.1f%% of total] out of " SIZE_FORMAT_W(9) " bytes [100.0%% used]",
                 size, size/double(total_size)*100.0, size);
}

void ArchiveBuilder::print_heap_region_stats(GrowableArray<MemRegion>* regions,
                                             const char *name, size_t total_size) {
  int arr_len = regions == NULL ? 0 : regions->length();
  for (int i = 0; i < arr_len; i++) {
      char* start = (char*)regions->at(i).start();
      size_t size = regions->at(i).byte_size();
      char* top = start + size;
      log_debug(cds)("%s%d space: " SIZE_FORMAT_W(9) " [ %4.1f%% of total] out of " SIZE_FORMAT_W(9) " bytes [100.0%% used] at " INTPTR_FORMAT,
                     name, i, size, size/double(total_size)*100.0, size, p2i(start));
  }
}

void ArchiveBuilder::report_out_of_space(const char* name, size_t needed_bytes) {
  // This is highly unlikely to happen on 64-bits because we have reserved a 4GB space.
  // On 32-bit we reserve only 256MB so you could run out of space with 100,000 classes
  // or so.
  _rw_region.print_out_of_space_msg(name, needed_bytes);
  _ro_region.print_out_of_space_msg(name, needed_bytes);

  vm_exit_during_initialization(err_msg("Unable to allocate from '%s' region", name),
                                "Please reduce the number of shared classes.");
}


#ifndef PRODUCT
void ArchiveBuilder::assert_is_vm_thread() {
  assert(Thread::current()->is_VM_thread(), "ArchiveBuilder should be used only inside the VMThread");
}
#endif
