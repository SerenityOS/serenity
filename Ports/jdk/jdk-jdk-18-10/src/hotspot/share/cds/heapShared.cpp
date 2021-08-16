/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "cds/filemap.hpp"
#include "cds/heapShared.inline.hpp"
#include "cds/metaspaceShared.hpp"
#include "classfile/classLoaderData.hpp"
#include "classfile/classLoaderDataShared.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/moduleEntry.hpp"
#include "classfile/stringTable.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/systemDictionaryShared.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/gcLocker.hpp"
#include "gc/shared/gcVMOperations.hpp"
#include "logging/log.hpp"
#include "logging/logMessage.hpp"
#include "logging/logStream.hpp"
#include "memory/iterator.inline.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/metaspaceClosure.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/fieldStreams.inline.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/init.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/copy.hpp"
#if INCLUDE_G1GC
#include "gc/g1/g1CollectedHeap.hpp"
#endif

#if INCLUDE_CDS_JAVA_HEAP

bool HeapShared::_closed_regions_mapped = false;
bool HeapShared::_open_regions_mapped = false;
address   HeapShared::_narrow_oop_base;
int       HeapShared::_narrow_oop_shift;
DumpedInternedStrings *HeapShared::_dumped_interned_strings = NULL;

//
// If you add new entries to the following tables, you should know what you're doing!
//

// Entry fields for shareable subgraphs archived in the closed archive heap
// region. Warning: Objects in the subgraphs should not have reference fields
// assigned at runtime.
static ArchivableStaticFieldInfo closed_archive_subgraph_entry_fields[] = {
  {"java/lang/Integer$IntegerCache",              "archivedCache"},
  {"java/lang/Long$LongCache",                    "archivedCache"},
  {"java/lang/Byte$ByteCache",                    "archivedCache"},
  {"java/lang/Short$ShortCache",                  "archivedCache"},
  {"java/lang/Character$CharacterCache",          "archivedCache"},
  {"java/util/jar/Attributes$Name",               "KNOWN_NAMES"},
  {"sun/util/locale/BaseLocale",                  "constantBaseLocales"},
};
// Entry fields for subgraphs archived in the open archive heap region.
static ArchivableStaticFieldInfo open_archive_subgraph_entry_fields[] = {
  {"jdk/internal/module/ArchivedModuleGraph",     "archivedModuleGraph"},
  {"java/util/ImmutableCollections",              "archivedObjects"},
  {"java/lang/ModuleLayer",                       "EMPTY_LAYER"},
  {"java/lang/module/Configuration",              "EMPTY_CONFIGURATION"},
  {"jdk/internal/math/FDBigInteger",              "archivedCaches"},
};

// Entry fields for subgraphs archived in the open archive heap region (full module graph).
static ArchivableStaticFieldInfo fmg_open_archive_subgraph_entry_fields[] = {
  {"jdk/internal/loader/ArchivedClassLoaders",    "archivedClassLoaders"},
  {"jdk/internal/module/ArchivedBootLayer",       "archivedBootLayer"},
  {"java/lang/Module$ArchivedData",               "archivedData"},
};

const static int num_closed_archive_subgraph_entry_fields =
  sizeof(closed_archive_subgraph_entry_fields) / sizeof(ArchivableStaticFieldInfo);
const static int num_open_archive_subgraph_entry_fields =
  sizeof(open_archive_subgraph_entry_fields) / sizeof(ArchivableStaticFieldInfo);
const static int num_fmg_open_archive_subgraph_entry_fields =
  sizeof(fmg_open_archive_subgraph_entry_fields) / sizeof(ArchivableStaticFieldInfo);

GrowableArrayCHeap<oop, mtClassShared>* HeapShared::_pending_roots = NULL;
narrowOop HeapShared::_roots_narrow;
OopHandle HeapShared::_roots;

#ifdef ASSERT
bool HeapShared::is_archived_object_during_dumptime(oop p) {
  assert(HeapShared::is_heap_object_archiving_allowed(), "must be");
  assert(DumpSharedSpaces, "this function is only used with -Xshare:dump");
  return Universe::heap()->is_archived_object(p);
}
#endif

////////////////////////////////////////////////////////////////
//
// Java heap object archiving support
//
////////////////////////////////////////////////////////////////
void HeapShared::fixup_mapped_regions() {
  FileMapInfo *mapinfo = FileMapInfo::current_info();
  mapinfo->fixup_mapped_heap_regions();
  if (is_mapped()) {
    _roots = OopHandle(Universe::vm_global(), decode_from_archive(_roots_narrow));
    if (!MetaspaceShared::use_full_module_graph()) {
      // Need to remove all the archived java.lang.Module objects from HeapShared::roots().
      ClassLoaderDataShared::clear_archived_oops();
    }
  }
  SystemDictionaryShared::update_archived_mirror_native_pointers();
}

unsigned HeapShared::oop_hash(oop const& p) {
  unsigned hash = (unsigned)p->identity_hash();
  return hash;
}

static void reset_states(oop obj, TRAPS) {
  Handle h_obj(THREAD, obj);
  InstanceKlass* klass = InstanceKlass::cast(obj->klass());
  TempNewSymbol method_name = SymbolTable::new_symbol("resetArchivedStates");
  Symbol* method_sig = vmSymbols::void_method_signature();

  while (klass != NULL) {
    Method* method = klass->find_method(method_name, method_sig);
    if (method != NULL) {
      assert(method->is_private(), "must be");
      if (log_is_enabled(Debug, cds)) {
        ResourceMark rm(THREAD);
        log_debug(cds)("  calling %s", method->name_and_sig_as_C_string());
      }
      JavaValue result(T_VOID);
      JavaCalls::call_special(&result, h_obj, klass,
                              method_name, method_sig, CHECK);
    }
    klass = klass->java_super();
  }
}

void HeapShared::reset_archived_object_states(TRAPS) {
  assert(DumpSharedSpaces, "dump-time only");
  log_debug(cds)("Resetting platform loader");
  reset_states(SystemDictionary::java_platform_loader(), CHECK);
  log_debug(cds)("Resetting system loader");
  reset_states(SystemDictionary::java_system_loader(), CHECK);
}

HeapShared::ArchivedObjectCache* HeapShared::_archived_object_cache = NULL;
oop HeapShared::find_archived_heap_object(oop obj) {
  assert(DumpSharedSpaces, "dump-time only");
  ArchivedObjectCache* cache = archived_object_cache();
  oop* p = cache->get(obj);
  if (p != NULL) {
    return *p;
  } else {
    return NULL;
  }
}

int HeapShared::append_root(oop obj) {
  assert(DumpSharedSpaces, "dump-time only");

  // No GC should happen since we aren't scanning _pending_roots.
  assert(Thread::current() == (Thread*)VMThread::vm_thread(), "should be in vm thread");

  if (_pending_roots == NULL) {
    _pending_roots = new GrowableArrayCHeap<oop, mtClassShared>(500);
  }

  return _pending_roots->append(obj);
}

objArrayOop HeapShared::roots() {
  if (DumpSharedSpaces) {
    assert(Thread::current() == (Thread*)VMThread::vm_thread(), "should be in vm thread");
    if (!is_heap_object_archiving_allowed()) {
      return NULL;
    }
  } else {
    assert(UseSharedSpaces, "must be");
  }

  objArrayOop roots = (objArrayOop)_roots.resolve();
  assert(roots != NULL, "should have been initialized");
  return roots;
}

void HeapShared::set_roots(narrowOop roots) {
  assert(UseSharedSpaces, "runtime only");
  assert(open_regions_mapped(), "must be");
  _roots_narrow = roots;
}

// Returns an objArray that contains all the roots of the archived objects
oop HeapShared::get_root(int index, bool clear) {
  assert(index >= 0, "sanity");
  if (DumpSharedSpaces) {
    assert(Thread::current() == (Thread*)VMThread::vm_thread(), "should be in vm thread");
    assert(_pending_roots != NULL, "sanity");
    return _pending_roots->at(index);
  } else {
    assert(UseSharedSpaces, "must be");
    assert(!_roots.is_empty(), "must have loaded shared heap");
    oop result = roots()->obj_at(index);
    if (clear) {
      clear_root(index);
    }
    return result;
  }
}

void HeapShared::clear_root(int index) {
  assert(index >= 0, "sanity");
  assert(UseSharedSpaces, "must be");
  if (open_regions_mapped()) {
    if (log_is_enabled(Debug, cds, heap)) {
      oop old = roots()->obj_at(index);
      log_debug(cds, heap)("Clearing root %d: was " PTR_FORMAT, index, p2i(old));
    }
    roots()->obj_at_put(index, NULL);
  }
}

oop HeapShared::archive_object(oop obj) {
  assert(DumpSharedSpaces, "dump-time only");

  oop ao = find_archived_heap_object(obj);
  if (ao != NULL) {
    // already archived
    return ao;
  }

  int len = obj->size();
  if (G1CollectedHeap::heap()->is_archive_alloc_too_large(len)) {
    log_debug(cds, heap)("Cannot archive, object (" PTR_FORMAT ") is too large: " SIZE_FORMAT,
                         p2i(obj), (size_t)obj->size());
    return NULL;
  }

  oop archived_oop = cast_to_oop(G1CollectedHeap::heap()->archive_mem_allocate(len));
  if (archived_oop != NULL) {
    Copy::aligned_disjoint_words(cast_from_oop<HeapWord*>(obj), cast_from_oop<HeapWord*>(archived_oop), len);
    // Reinitialize markword to remove age/marking/locking/etc.
    //
    // We need to retain the identity_hash, because it may have been used by some hashtables
    // in the shared heap. This also has the side effect of pre-initializing the
    // identity_hash for all shared objects, so they are less likely to be written
    // into during run time, increasing the potential of memory sharing.
    int hash_original = obj->identity_hash();
    archived_oop->set_mark(markWord::prototype().copy_set_hash(hash_original));
    assert(archived_oop->mark().is_unlocked(), "sanity");

    DEBUG_ONLY(int hash_archived = archived_oop->identity_hash());
    assert(hash_original == hash_archived, "Different hash codes: original %x, archived %x", hash_original, hash_archived);

    ArchivedObjectCache* cache = archived_object_cache();
    cache->put(obj, archived_oop);
    if (log_is_enabled(Debug, cds, heap)) {
      ResourceMark rm;
      log_debug(cds, heap)("Archived heap object " PTR_FORMAT " ==> " PTR_FORMAT " : %s",
                           p2i(obj), p2i(archived_oop), obj->klass()->external_name());
    }
  } else {
    log_error(cds, heap)(
      "Cannot allocate space for object " PTR_FORMAT " in archived heap region",
      p2i(obj));
    vm_direct_exit(-1,
      err_msg("Out of memory. Please run with a larger Java heap, current MaxHeapSize = "
              SIZE_FORMAT "M", MaxHeapSize/M));
  }
  return archived_oop;
}

void HeapShared::archive_klass_objects() {
  GrowableArray<Klass*>* klasses = ArchiveBuilder::current()->klasses();
  assert(klasses != NULL, "sanity");
  for (int i = 0; i < klasses->length(); i++) {
    Klass* k = ArchiveBuilder::get_relocated_klass(klasses->at(i));

    // archive mirror object
    java_lang_Class::archive_mirror(k);

    // archive the resolved_referenes array
    if (k->is_instance_klass()) {
      InstanceKlass* ik = InstanceKlass::cast(k);
      ik->constants()->archive_resolved_references();
    }
  }
}

void HeapShared::run_full_gc_in_vm_thread() {
  if (is_heap_object_archiving_allowed()) {
    // Avoid fragmentation while archiving heap objects.
    // We do this inside a safepoint, so that no further allocation can happen after GC
    // has finished.
    if (GCLocker::is_active()) {
      // Just checking for safety ...
      // This should not happen during -Xshare:dump. If you see this, probably the Java core lib
      // has been modified such that JNI code is executed in some clean up threads after
      // we have finished class loading.
      log_warning(cds)("GC locker is held, unable to start extra compacting GC. This may produce suboptimal results.");
    } else {
      log_info(cds)("Run GC ...");
      Universe::heap()->collect_as_vm_thread(GCCause::_archive_time_gc);
      log_info(cds)("Run GC done");
    }
  }
}

void HeapShared::archive_objects(GrowableArray<MemRegion>* closed_regions,
                                 GrowableArray<MemRegion>* open_regions) {

  G1HeapVerifier::verify_ready_for_archiving();

  {
    NoSafepointVerifier nsv;

    // Cache for recording where the archived objects are copied to
    create_archived_object_cache();

    log_info(cds)("Heap range = [" PTR_FORMAT " - "  PTR_FORMAT "]",
                  p2i(CompressedOops::begin()), p2i(CompressedOops::end()));
    log_info(cds)("Dumping objects to closed archive heap region ...");
    copy_closed_objects(closed_regions);

    log_info(cds)("Dumping objects to open archive heap region ...");
    copy_open_objects(open_regions);

    destroy_archived_object_cache();
  }

  G1HeapVerifier::verify_archive_regions();
}

void HeapShared::copy_closed_objects(GrowableArray<MemRegion>* closed_regions) {
  assert(is_heap_object_archiving_allowed(), "Cannot archive java heap objects");

  G1CollectedHeap::heap()->begin_archive_alloc_range();

  // Archive interned string objects
  StringTable::write_to_archive(_dumped_interned_strings);

  archive_object_subgraphs(closed_archive_subgraph_entry_fields,
                           num_closed_archive_subgraph_entry_fields,
                           true /* is_closed_archive */,
                           false /* is_full_module_graph */);

  G1CollectedHeap::heap()->end_archive_alloc_range(closed_regions,
                                                   os::vm_allocation_granularity());
}

void HeapShared::copy_open_objects(GrowableArray<MemRegion>* open_regions) {
  assert(is_heap_object_archiving_allowed(), "Cannot archive java heap objects");

  G1CollectedHeap::heap()->begin_archive_alloc_range(true /* open */);

  java_lang_Class::archive_basic_type_mirrors();

  archive_klass_objects();

  archive_object_subgraphs(open_archive_subgraph_entry_fields,
                           num_open_archive_subgraph_entry_fields,
                           false /* is_closed_archive */,
                           false /* is_full_module_graph */);
  if (MetaspaceShared::use_full_module_graph()) {
    archive_object_subgraphs(fmg_open_archive_subgraph_entry_fields,
                             num_fmg_open_archive_subgraph_entry_fields,
                             false /* is_closed_archive */,
                             true /* is_full_module_graph */);
    ClassLoaderDataShared::init_archived_oops();
  }

  copy_roots();

  G1CollectedHeap::heap()->end_archive_alloc_range(open_regions,
                                                   os::vm_allocation_granularity());
}

// Copy _pending_archive_roots into an objArray
void HeapShared::copy_roots() {
  int length = _pending_roots != NULL ? _pending_roots->length() : 0;
  int size = objArrayOopDesc::object_size(length);
  Klass* k = Universe::objectArrayKlassObj(); // already relocated to point to archived klass
  HeapWord* mem = G1CollectedHeap::heap()->archive_mem_allocate(size);

  memset(mem, 0, size * BytesPerWord);
  {
    // This is copied from MemAllocator::finish
    oopDesc::set_mark(mem, markWord::prototype());
    oopDesc::release_set_klass(mem, k);
  }
  {
    // This is copied from ObjArrayAllocator::initialize
    arrayOopDesc::set_length(mem, length);
  }

  _roots = OopHandle(Universe::vm_global(), cast_to_oop(mem));
  for (int i = 0; i < length; i++) {
    roots()->obj_at_put(i, _pending_roots->at(i));
  }
  log_info(cds)("archived obj roots[%d] = %d words, klass = %p, obj = %p", length, size, k, mem);
}

void HeapShared::init_narrow_oop_decoding(address base, int shift) {
  _narrow_oop_base = base;
  _narrow_oop_shift = shift;
}

//
// Subgraph archiving support
//
HeapShared::DumpTimeKlassSubGraphInfoTable* HeapShared::_dump_time_subgraph_info_table = NULL;
HeapShared::RunTimeKlassSubGraphInfoTable   HeapShared::_run_time_subgraph_info_table;

// Get the subgraph_info for Klass k. A new subgraph_info is created if
// there is no existing one for k. The subgraph_info records the relocated
// Klass* of the original k.
KlassSubGraphInfo* HeapShared::init_subgraph_info(Klass* k, bool is_full_module_graph) {
  assert(DumpSharedSpaces, "dump time only");
  bool created;
  Klass* relocated_k = ArchiveBuilder::get_relocated_klass(k);
  KlassSubGraphInfo* info =
    _dump_time_subgraph_info_table->put_if_absent(relocated_k, KlassSubGraphInfo(relocated_k, is_full_module_graph),
                                                  &created);
  assert(created, "must not initialize twice");
  return info;
}

KlassSubGraphInfo* HeapShared::get_subgraph_info(Klass* k) {
  assert(DumpSharedSpaces, "dump time only");
  Klass* relocated_k = ArchiveBuilder::get_relocated_klass(k);
  KlassSubGraphInfo* info = _dump_time_subgraph_info_table->get(relocated_k);
  assert(info != NULL, "must have been initialized");
  return info;
}

// Add an entry field to the current KlassSubGraphInfo.
void KlassSubGraphInfo::add_subgraph_entry_field(
      int static_field_offset, oop v, bool is_closed_archive) {
  assert(DumpSharedSpaces, "dump time only");
  if (_subgraph_entry_fields == NULL) {
    _subgraph_entry_fields =
      new(ResourceObj::C_HEAP, mtClass) GrowableArray<int>(10, mtClass);
  }
  _subgraph_entry_fields->append(static_field_offset);
  _subgraph_entry_fields->append(HeapShared::append_root(v));
}

// Add the Klass* for an object in the current KlassSubGraphInfo's subgraphs.
// Only objects of boot classes can be included in sub-graph.
void KlassSubGraphInfo::add_subgraph_object_klass(Klass* orig_k) {
  assert(DumpSharedSpaces, "dump time only");
  Klass* relocated_k = ArchiveBuilder::get_relocated_klass(orig_k);

  if (_subgraph_object_klasses == NULL) {
    _subgraph_object_klasses =
      new(ResourceObj::C_HEAP, mtClass) GrowableArray<Klass*>(50, mtClass);
  }

  assert(ArchiveBuilder::current()->is_in_buffer_space(relocated_k), "must be a shared class");

  if (_k == relocated_k) {
    // Don't add the Klass containing the sub-graph to it's own klass
    // initialization list.
    return;
  }

  if (relocated_k->is_instance_klass()) {
    assert(InstanceKlass::cast(relocated_k)->is_shared_boot_class(),
          "must be boot class");
    // vmClasses::xxx_klass() are not updated, need to check
    // the original Klass*
    if (orig_k == vmClasses::String_klass() ||
        orig_k == vmClasses::Object_klass()) {
      // Initialized early during VM initialization. No need to be added
      // to the sub-graph object class list.
      return;
    }
  } else if (relocated_k->is_objArray_klass()) {
    Klass* abk = ObjArrayKlass::cast(relocated_k)->bottom_klass();
    if (abk->is_instance_klass()) {
      assert(InstanceKlass::cast(abk)->is_shared_boot_class(),
            "must be boot class");
    }
    if (relocated_k == Universe::objectArrayKlassObj()) {
      // Initialized early during Universe::genesis. No need to be added
      // to the list.
      return;
    }
  } else {
    assert(relocated_k->is_typeArray_klass(), "must be");
    // Primitive type arrays are created early during Universe::genesis.
    return;
  }

  if (log_is_enabled(Debug, cds, heap)) {
    if (!_subgraph_object_klasses->contains(relocated_k)) {
      ResourceMark rm;
      log_debug(cds, heap)("Adding klass %s", orig_k->external_name());
    }
  }

  _subgraph_object_klasses->append_if_missing(relocated_k);
  _has_non_early_klasses |= is_non_early_klass(orig_k);
}

bool KlassSubGraphInfo::is_non_early_klass(Klass* k) {
  if (k->is_objArray_klass()) {
    k = ObjArrayKlass::cast(k)->bottom_klass();
  }
  if (k->is_instance_klass()) {
    if (!SystemDictionaryShared::is_early_klass(InstanceKlass::cast(k))) {
      ResourceMark rm;
      log_info(cds, heap)("non-early: %s", k->external_name());
      return true;
    } else {
      return false;
    }
  } else {
    return false;
  }
}

// Initialize an archived subgraph_info_record from the given KlassSubGraphInfo.
void ArchivedKlassSubGraphInfoRecord::init(KlassSubGraphInfo* info) {
  _k = info->klass();
  _entry_field_records = NULL;
  _subgraph_object_klasses = NULL;
  _is_full_module_graph = info->is_full_module_graph();

  if (_is_full_module_graph) {
    // Consider all classes referenced by the full module graph as early -- we will be
    // allocating objects of these classes during JVMTI early phase, so they cannot
    // be processed by (non-early) JVMTI ClassFileLoadHook
    _has_non_early_klasses = false;
  } else {
    _has_non_early_klasses = info->has_non_early_klasses();
  }

  if (_has_non_early_klasses) {
    ResourceMark rm;
    log_info(cds, heap)(
          "Subgraph of klass %s has non-early klasses and cannot be used when JVMTI ClassFileLoadHook is enabled",
          _k->external_name());
  }

  // populate the entry fields
  GrowableArray<int>* entry_fields = info->subgraph_entry_fields();
  if (entry_fields != NULL) {
    int num_entry_fields = entry_fields->length();
    assert(num_entry_fields % 2 == 0, "sanity");
    _entry_field_records =
      ArchiveBuilder::new_ro_array<int>(num_entry_fields);
    for (int i = 0 ; i < num_entry_fields; i++) {
      _entry_field_records->at_put(i, entry_fields->at(i));
    }
  }

  // the Klasses of the objects in the sub-graphs
  GrowableArray<Klass*>* subgraph_object_klasses = info->subgraph_object_klasses();
  if (subgraph_object_klasses != NULL) {
    int num_subgraphs_klasses = subgraph_object_klasses->length();
    _subgraph_object_klasses =
      ArchiveBuilder::new_ro_array<Klass*>(num_subgraphs_klasses);
    for (int i = 0; i < num_subgraphs_klasses; i++) {
      Klass* subgraph_k = subgraph_object_klasses->at(i);
      if (log_is_enabled(Info, cds, heap)) {
        ResourceMark rm;
        log_info(cds, heap)(
          "Archived object klass %s (%2d) => %s",
          _k->external_name(), i, subgraph_k->external_name());
      }
      _subgraph_object_klasses->at_put(i, subgraph_k);
      ArchivePtrMarker::mark_pointer(_subgraph_object_klasses->adr_at(i));
    }
  }

  ArchivePtrMarker::mark_pointer(&_k);
  ArchivePtrMarker::mark_pointer(&_entry_field_records);
  ArchivePtrMarker::mark_pointer(&_subgraph_object_klasses);
}

struct CopyKlassSubGraphInfoToArchive : StackObj {
  CompactHashtableWriter* _writer;
  CopyKlassSubGraphInfoToArchive(CompactHashtableWriter* writer) : _writer(writer) {}

  bool do_entry(Klass* klass, KlassSubGraphInfo& info) {
    if (info.subgraph_object_klasses() != NULL || info.subgraph_entry_fields() != NULL) {
      ArchivedKlassSubGraphInfoRecord* record =
        (ArchivedKlassSubGraphInfoRecord*)ArchiveBuilder::ro_region_alloc(sizeof(ArchivedKlassSubGraphInfoRecord));
      record->init(&info);

      unsigned int hash = SystemDictionaryShared::hash_for_shared_dictionary((address)klass);
      u4 delta = ArchiveBuilder::current()->any_to_offset_u4(record);
      _writer->add(hash, delta);
    }
    return true; // keep on iterating
  }
};

// Build the records of archived subgraph infos, which include:
// - Entry points to all subgraphs from the containing class mirror. The entry
//   points are static fields in the mirror. For each entry point, the field
//   offset, value and is_closed_archive flag are recorded in the sub-graph
//   info. The value is stored back to the corresponding field at runtime.
// - A list of klasses that need to be loaded/initialized before archived
//   java object sub-graph can be accessed at runtime.
void HeapShared::write_subgraph_info_table() {
  // Allocate the contents of the hashtable(s) inside the RO region of the CDS archive.
  DumpTimeKlassSubGraphInfoTable* d_table = _dump_time_subgraph_info_table;
  CompactHashtableStats stats;

  _run_time_subgraph_info_table.reset();

  CompactHashtableWriter writer(d_table->_count, &stats);
  CopyKlassSubGraphInfoToArchive copy(&writer);
  d_table->iterate(&copy);

  writer.dump(&_run_time_subgraph_info_table, "subgraphs");
}

void HeapShared::serialize_subgraph_info_table_header(SerializeClosure* soc) {
  _run_time_subgraph_info_table.serialize_header(soc);
}

static void verify_the_heap(Klass* k, const char* which) {
  if (VerifyArchivedFields) {
    ResourceMark rm;
    log_info(cds, heap)("Verify heap %s initializing static field(s) in %s",
                        which, k->external_name());

    VM_Verify verify_op;
    VMThread::execute(&verify_op);

    if (!FLAG_IS_DEFAULT(VerifyArchivedFields)) {
      // If VerifyArchivedFields has a non-default value (e.g., specified on the command-line), do
      // more expensive checks.
      if (is_init_completed()) {
        FlagSetting fs1(VerifyBeforeGC, true);
        FlagSetting fs2(VerifyDuringGC, true);
        FlagSetting fs3(VerifyAfterGC,  true);
        Universe::heap()->collect(GCCause::_java_lang_system_gc);
      }
    }
  }
}

// Before GC can execute, we must ensure that all oops reachable from HeapShared::roots()
// have a valid klass. I.e., oopDesc::klass() must have already been resolved.
//
// Note: if a ArchivedKlassSubGraphInfoRecord contains non-early classes, and JVMTI
// ClassFileLoadHook is enabled, it's possible for this class to be dynamically replaced. In
// this case, we will not load the ArchivedKlassSubGraphInfoRecord and will clear its roots.
void HeapShared::resolve_classes(JavaThread* THREAD) {
  if (!is_mapped()) {
    return; // nothing to do
  }
  resolve_classes_for_subgraphs(closed_archive_subgraph_entry_fields,
                                num_closed_archive_subgraph_entry_fields,
                                THREAD);
  resolve_classes_for_subgraphs(open_archive_subgraph_entry_fields,
                                num_open_archive_subgraph_entry_fields,
                                THREAD);
  resolve_classes_for_subgraphs(fmg_open_archive_subgraph_entry_fields,
                                num_fmg_open_archive_subgraph_entry_fields,
                                THREAD);
}

void HeapShared::resolve_classes_for_subgraphs(ArchivableStaticFieldInfo fields[],
                                               int num, JavaThread* THREAD) {
  for (int i = 0; i < num; i++) {
    ArchivableStaticFieldInfo* info = &fields[i];
    TempNewSymbol klass_name = SymbolTable::new_symbol(info->klass_name);
    InstanceKlass* k = SystemDictionaryShared::find_builtin_class(klass_name);
    assert(k != NULL && k->is_shared_boot_class(), "sanity");
    resolve_classes_for_subgraph_of(k, THREAD);
  }
}

void HeapShared::resolve_classes_for_subgraph_of(Klass* k, JavaThread* THREAD) {
  ExceptionMark em(THREAD);
  const ArchivedKlassSubGraphInfoRecord* record =
   resolve_or_init_classes_for_subgraph_of(k, /*do_init=*/false, THREAD);
  if (HAS_PENDING_EXCEPTION) {
   CLEAR_PENDING_EXCEPTION;
  }
  if (record == NULL) {
   clear_archived_roots_of(k);
  }
}

void HeapShared::initialize_from_archived_subgraph(Klass* k, JavaThread* THREAD) {
  if (!is_mapped()) {
    return; // nothing to do
  }

  ExceptionMark em(THREAD);
  const ArchivedKlassSubGraphInfoRecord* record =
    resolve_or_init_classes_for_subgraph_of(k, /*do_init=*/true, THREAD);

  if (HAS_PENDING_EXCEPTION) {
    CLEAR_PENDING_EXCEPTION;
    // None of the field value will be set if there was an exception when initializing the classes.
    // The java code will not see any of the archived objects in the
    // subgraphs referenced from k in this case.
    return;
  }

  if (record != NULL) {
    init_archived_fields_for(k, record);
  }
}

const ArchivedKlassSubGraphInfoRecord*
HeapShared::resolve_or_init_classes_for_subgraph_of(Klass* k, bool do_init, TRAPS) {
  assert(!DumpSharedSpaces, "Should not be called with DumpSharedSpaces");

  if (!k->is_shared()) {
    return NULL;
  }
  unsigned int hash = SystemDictionaryShared::hash_for_shared_dictionary_quick(k);
  const ArchivedKlassSubGraphInfoRecord* record = _run_time_subgraph_info_table.lookup(k, hash, 0);

  // Initialize from archived data. Currently this is done only
  // during VM initialization time. No lock is needed.
  if (record != NULL) {
    if (record->is_full_module_graph() && !MetaspaceShared::use_full_module_graph()) {
      if (log_is_enabled(Info, cds, heap)) {
        ResourceMark rm(THREAD);
        log_info(cds, heap)("subgraph %s cannot be used because full module graph is disabled",
                            k->external_name());
      }
      return NULL;
    }

    if (record->has_non_early_klasses() && JvmtiExport::should_post_class_file_load_hook()) {
      if (log_is_enabled(Info, cds, heap)) {
        ResourceMark rm(THREAD);
        log_info(cds, heap)("subgraph %s cannot be used because JVMTI ClassFileLoadHook is enabled",
                            k->external_name());
      }
      return NULL;
    }

    resolve_or_init(k, do_init, CHECK_NULL);

    // Load/link/initialize the klasses of the objects in the subgraph.
    // NULL class loader is used.
    Array<Klass*>* klasses = record->subgraph_object_klasses();
    if (klasses != NULL) {
      for (int i = 0; i < klasses->length(); i++) {
        Klass* klass = klasses->at(i);
        if (!klass->is_shared()) {
          return NULL;
        }
        resolve_or_init(klass, do_init, CHECK_NULL);
      }
    }
  }

  return record;
}

void HeapShared::resolve_or_init(Klass* k, bool do_init, TRAPS) {
  if (!do_init) {
    if (k->class_loader_data() == NULL) {
      Klass* resolved_k = SystemDictionary::resolve_or_null(k->name(), CHECK);
      assert(resolved_k == k, "classes used by archived heap must not be replaced by JVMTI ClassFileLoadHook");
    }
  } else {
    assert(k->class_loader_data() != NULL, "must have been resolved by HeapShared::resolve_classes");
    if (k->is_instance_klass()) {
      InstanceKlass* ik = InstanceKlass::cast(k);
      ik->initialize(CHECK);
    } else if (k->is_objArray_klass()) {
      ObjArrayKlass* oak = ObjArrayKlass::cast(k);
      oak->initialize(CHECK);
    }
  }
}

void HeapShared::init_archived_fields_for(Klass* k, const ArchivedKlassSubGraphInfoRecord* record) {
  verify_the_heap(k, "before");

  // Load the subgraph entry fields from the record and store them back to
  // the corresponding fields within the mirror.
  oop m = k->java_mirror();
  Array<int>* entry_field_records = record->entry_field_records();
  if (entry_field_records != NULL) {
    int efr_len = entry_field_records->length();
    assert(efr_len % 2 == 0, "sanity");
    for (int i = 0; i < efr_len; i += 2) {
      int field_offset = entry_field_records->at(i);
      int root_index = entry_field_records->at(i+1);
      oop v = get_root(root_index, /*clear=*/true);
      m->obj_field_put(field_offset, v);
      log_debug(cds, heap)("  " PTR_FORMAT " init field @ %2d = " PTR_FORMAT, p2i(k), field_offset, p2i(v));
    }

    // Done. Java code can see the archived sub-graphs referenced from k's
    // mirror after this point.
    if (log_is_enabled(Info, cds, heap)) {
      ResourceMark rm;
      log_info(cds, heap)("initialize_from_archived_subgraph %s " PTR_FORMAT "%s",
                          k->external_name(), p2i(k), JvmtiExport::is_early_phase() ? " (early)" : "");
    }
  }

  verify_the_heap(k, "after ");
}

void HeapShared::clear_archived_roots_of(Klass* k) {
  unsigned int hash = SystemDictionaryShared::hash_for_shared_dictionary_quick(k);
  const ArchivedKlassSubGraphInfoRecord* record = _run_time_subgraph_info_table.lookup(k, hash, 0);
  if (record != NULL) {
    Array<int>* entry_field_records = record->entry_field_records();
    if (entry_field_records != NULL) {
      int efr_len = entry_field_records->length();
      assert(efr_len % 2 == 0, "sanity");
      for (int i = 0; i < efr_len; i += 2) {
        int root_index = entry_field_records->at(i+1);
        clear_root(root_index);
      }
    }
  }
}

class WalkOopAndArchiveClosure: public BasicOopIterateClosure {
  int _level;
  bool _is_closed_archive;
  bool _record_klasses_only;
  KlassSubGraphInfo* _subgraph_info;
  oop _orig_referencing_obj;
  oop _archived_referencing_obj;
 public:
  WalkOopAndArchiveClosure(int level,
                           bool is_closed_archive,
                           bool record_klasses_only,
                           KlassSubGraphInfo* subgraph_info,
                           oop orig, oop archived) :
    _level(level), _is_closed_archive(is_closed_archive),
    _record_klasses_only(record_klasses_only),
    _subgraph_info(subgraph_info),
    _orig_referencing_obj(orig), _archived_referencing_obj(archived) {}
  void do_oop(narrowOop *p) { WalkOopAndArchiveClosure::do_oop_work(p); }
  void do_oop(      oop *p) { WalkOopAndArchiveClosure::do_oop_work(p); }

 protected:
  template <class T> void do_oop_work(T *p) {
    oop obj = RawAccess<>::oop_load(p);
    if (!CompressedOops::is_null(obj)) {
      assert(!HeapShared::is_archived_object_during_dumptime(obj),
             "original objects must not point to archived objects");

      size_t field_delta = pointer_delta(p, _orig_referencing_obj, sizeof(char));
      T* new_p = (T*)(cast_from_oop<address>(_archived_referencing_obj) + field_delta);

      if (!_record_klasses_only && log_is_enabled(Debug, cds, heap)) {
        ResourceMark rm;
        log_debug(cds, heap)("(%d) %s[" SIZE_FORMAT "] ==> " PTR_FORMAT " size %d %s", _level,
                             _orig_referencing_obj->klass()->external_name(), field_delta,
                             p2i(obj), obj->size() * HeapWordSize, obj->klass()->external_name());
        LogTarget(Trace, cds, heap) log;
        LogStream out(log);
        obj->print_on(&out);
      }

      oop archived = HeapShared::archive_reachable_objects_from(
          _level + 1, _subgraph_info, obj, _is_closed_archive);
      assert(archived != NULL, "VM should have exited with unarchivable objects for _level > 1");
      assert(HeapShared::is_archived_object_during_dumptime(archived), "must be");

      if (!_record_klasses_only) {
        // Update the reference in the archived copy of the referencing object.
        log_debug(cds, heap)("(%d) updating oop @[" PTR_FORMAT "] " PTR_FORMAT " ==> " PTR_FORMAT,
                             _level, p2i(new_p), p2i(obj), p2i(archived));
        RawAccess<IS_NOT_NULL>::oop_store(new_p, archived);
      }
    }
  }
};

void HeapShared::check_closed_region_object(InstanceKlass* k) {
  // Check fields in the object
  for (JavaFieldStream fs(k); !fs.done(); fs.next()) {
    if (!fs.access_flags().is_static()) {
      BasicType ft = fs.field_descriptor().field_type();
      if (!fs.access_flags().is_final() && is_reference_type(ft)) {
        ResourceMark rm;
        log_warning(cds, heap)(
          "Please check reference field in %s instance in closed archive heap region: %s %s",
          k->external_name(), (fs.name())->as_C_string(),
          (fs.signature())->as_C_string());
      }
    }
  }
}

void HeapShared::check_module_oop(oop orig_module_obj) {
  assert(DumpSharedSpaces, "must be");
  assert(java_lang_Module::is_instance(orig_module_obj), "must be");
  ModuleEntry* orig_module_ent = java_lang_Module::module_entry_raw(orig_module_obj);
  if (orig_module_ent == NULL) {
    // These special Module objects are created in Java code. They are not
    // defined via Modules::define_module(), so they don't have a ModuleEntry:
    //     java.lang.Module::ALL_UNNAMED_MODULE
    //     java.lang.Module::EVERYONE_MODULE
    //     jdk.internal.loader.ClassLoaders$BootClassLoader::unnamedModule
    assert(java_lang_Module::name(orig_module_obj) == NULL, "must be unnamed");
    log_info(cds, heap)("Module oop with No ModuleEntry* @[" PTR_FORMAT "]", p2i(orig_module_obj));
  } else {
    ClassLoaderData* loader_data = orig_module_ent->loader_data();
    assert(loader_data->is_builtin_class_loader_data(), "must be");
  }
}


// (1) If orig_obj has not been archived yet, archive it.
// (2) If orig_obj has not been seen yet (since start_recording_subgraph() was called),
//     trace all  objects that are reachable from it, and make sure these objects are archived.
// (3) Record the klasses of all orig_obj and all reachable objects.
oop HeapShared::archive_reachable_objects_from(int level,
                                               KlassSubGraphInfo* subgraph_info,
                                               oop orig_obj,
                                               bool is_closed_archive) {
  assert(orig_obj != NULL, "must be");
  assert(!is_archived_object_during_dumptime(orig_obj), "sanity");

  if (!JavaClasses::is_supported_for_archiving(orig_obj)) {
    // This object has injected fields that cannot be supported easily, so we disallow them for now.
    // If you get an error here, you probably made a change in the JDK library that has added
    // these objects that are referenced (directly or indirectly) by static fields.
    ResourceMark rm;
    log_error(cds, heap)("Cannot archive object of class %s", orig_obj->klass()->external_name());
    vm_direct_exit(1);
  }

  // java.lang.Class instances cannot be included in an archived object sub-graph. We only support
  // them as Klass::_archived_mirror because they need to be specially restored at run time.
  //
  // If you get an error here, you probably made a change in the JDK library that has added a Class
  // object that is referenced (directly or indirectly) by static fields.
  if (java_lang_Class::is_instance(orig_obj)) {
    log_error(cds, heap)("(%d) Unknown java.lang.Class object is in the archived sub-graph", level);
    vm_direct_exit(1);
  }

  oop archived_obj = find_archived_heap_object(orig_obj);
  if (java_lang_String::is_instance(orig_obj) && archived_obj != NULL) {
    // To save time, don't walk strings that are already archived. They just contain
    // pointers to a type array, whose klass doesn't need to be recorded.
    return archived_obj;
  }

  if (has_been_seen_during_subgraph_recording(orig_obj)) {
    // orig_obj has already been archived and traced. Nothing more to do.
    return archived_obj;
  } else {
    set_has_been_seen_during_subgraph_recording(orig_obj);
  }

  bool record_klasses_only = (archived_obj != NULL);
  if (archived_obj == NULL) {
    ++_num_new_archived_objs;
    archived_obj = archive_object(orig_obj);
    if (archived_obj == NULL) {
      // Skip archiving the sub-graph referenced from the current entry field.
      ResourceMark rm;
      log_error(cds, heap)(
        "Cannot archive the sub-graph referenced from %s object ("
        PTR_FORMAT ") size %d, skipped.",
        orig_obj->klass()->external_name(), p2i(orig_obj), orig_obj->size() * HeapWordSize);
      if (level == 1) {
        // Don't archive a subgraph root that's too big. For archives static fields, that's OK
        // as the Java code will take care of initializing this field dynamically.
        return NULL;
      } else {
        // We don't know how to handle an object that has been archived, but some of its reachable
        // objects cannot be archived. Bail out for now. We might need to fix this in the future if
        // we have a real use case.
        vm_direct_exit(1);
      }
    }

    if (java_lang_Module::is_instance(orig_obj)) {
      check_module_oop(orig_obj);
      java_lang_Module::set_module_entry(archived_obj, NULL);
      java_lang_Module::set_loader(archived_obj, NULL);
    } else if (java_lang_ClassLoader::is_instance(orig_obj)) {
      // class_data will be restored explicitly at run time.
      guarantee(orig_obj == SystemDictionary::java_platform_loader() ||
                orig_obj == SystemDictionary::java_system_loader() ||
                java_lang_ClassLoader::loader_data(orig_obj) == NULL, "must be");
      java_lang_ClassLoader::release_set_loader_data(archived_obj, NULL);
    }
  }

  assert(archived_obj != NULL, "must be");
  Klass *orig_k = orig_obj->klass();
  subgraph_info->add_subgraph_object_klass(orig_k);

  WalkOopAndArchiveClosure walker(level, is_closed_archive, record_klasses_only,
                                  subgraph_info, orig_obj, archived_obj);
  orig_obj->oop_iterate(&walker);
  if (is_closed_archive && orig_k->is_instance_klass()) {
    check_closed_region_object(InstanceKlass::cast(orig_k));
  }
  return archived_obj;
}

//
// Start from the given static field in a java mirror and archive the
// complete sub-graph of java heap objects that are reached directly
// or indirectly from the starting object by following references.
// Sub-graph archiving restrictions (current):
//
// - All classes of objects in the archived sub-graph (including the
//   entry class) must be boot class only.
// - No java.lang.Class instance (java mirror) can be included inside
//   an archived sub-graph. Mirror can only be the sub-graph entry object.
//
// The Java heap object sub-graph archiving process (see
// WalkOopAndArchiveClosure):
//
// 1) Java object sub-graph archiving starts from a given static field
// within a Class instance (java mirror). If the static field is a
// refererence field and points to a non-null java object, proceed to
// the next step.
//
// 2) Archives the referenced java object. If an archived copy of the
// current object already exists, updates the pointer in the archived
// copy of the referencing object to point to the current archived object.
// Otherwise, proceed to the next step.
//
// 3) Follows all references within the current java object and recursively
// archive the sub-graph of objects starting from each reference.
//
// 4) Updates the pointer in the archived copy of referencing object to
// point to the current archived object.
//
// 5) The Klass of the current java object is added to the list of Klasses
// for loading and initialzing before any object in the archived graph can
// be accessed at runtime.
//
void HeapShared::archive_reachable_objects_from_static_field(InstanceKlass *k,
                                                             const char* klass_name,
                                                             int field_offset,
                                                             const char* field_name,
                                                             bool is_closed_archive) {
  assert(DumpSharedSpaces, "dump time only");
  assert(k->is_shared_boot_class(), "must be boot class");

  oop m = k->java_mirror();

  KlassSubGraphInfo* subgraph_info = get_subgraph_info(k);
  oop f = m->obj_field(field_offset);

  log_debug(cds, heap)("Start archiving from: %s::%s (" PTR_FORMAT ")", klass_name, field_name, p2i(f));

  if (!CompressedOops::is_null(f)) {
    if (log_is_enabled(Trace, cds, heap)) {
      LogTarget(Trace, cds, heap) log;
      LogStream out(log);
      f->print_on(&out);
    }

    oop af = archive_reachable_objects_from(1, subgraph_info, f, is_closed_archive);

    if (af == NULL) {
      log_error(cds, heap)("Archiving failed %s::%s (some reachable objects cannot be archived)",
                           klass_name, field_name);
    } else {
      // Note: the field value is not preserved in the archived mirror.
      // Record the field as a new subGraph entry point. The recorded
      // information is restored from the archive at runtime.
      subgraph_info->add_subgraph_entry_field(field_offset, af, is_closed_archive);
      log_info(cds, heap)("Archived field %s::%s => " PTR_FORMAT, klass_name, field_name, p2i(af));
    }
  } else {
    // The field contains null, we still need to record the entry point,
    // so it can be restored at runtime.
    subgraph_info->add_subgraph_entry_field(field_offset, NULL, false);
  }
}

#ifndef PRODUCT
class VerifySharedOopClosure: public BasicOopIterateClosure {
 private:
  bool _is_archived;

 public:
  VerifySharedOopClosure(bool is_archived) : _is_archived(is_archived) {}

  void do_oop(narrowOop *p) { VerifySharedOopClosure::do_oop_work(p); }
  void do_oop(      oop *p) { VerifySharedOopClosure::do_oop_work(p); }

 protected:
  template <class T> void do_oop_work(T *p) {
    oop obj = RawAccess<>::oop_load(p);
    if (!CompressedOops::is_null(obj)) {
      HeapShared::verify_reachable_objects_from(obj, _is_archived);
    }
  }
};

void HeapShared::verify_subgraph_from_static_field(InstanceKlass* k, int field_offset) {
  assert(DumpSharedSpaces, "dump time only");
  assert(k->is_shared_boot_class(), "must be boot class");

  oop m = k->java_mirror();
  oop f = m->obj_field(field_offset);
  if (!CompressedOops::is_null(f)) {
    verify_subgraph_from(f);
  }
}

void HeapShared::verify_subgraph_from(oop orig_obj) {
  oop archived_obj = find_archived_heap_object(orig_obj);
  if (archived_obj == NULL) {
    // It's OK for the root of a subgraph to be not archived. See comments in
    // archive_reachable_objects_from().
    return;
  }

  // Verify that all objects reachable from orig_obj are archived.
  init_seen_objects_table();
  verify_reachable_objects_from(orig_obj, false);
  delete_seen_objects_table();

  // Note: we could also verify that all objects reachable from the archived
  // copy of orig_obj can only point to archived objects, with:
  //      init_seen_objects_table();
  //      verify_reachable_objects_from(archived_obj, true);
  //      init_seen_objects_table();
  // but that's already done in G1HeapVerifier::verify_archive_regions so we
  // won't do it here.
}

void HeapShared::verify_reachable_objects_from(oop obj, bool is_archived) {
  _num_total_verifications ++;
  if (!has_been_seen_during_subgraph_recording(obj)) {
    set_has_been_seen_during_subgraph_recording(obj);

    if (is_archived) {
      assert(is_archived_object_during_dumptime(obj), "must be");
      assert(find_archived_heap_object(obj) == NULL, "must be");
    } else {
      assert(!is_archived_object_during_dumptime(obj), "must be");
      assert(find_archived_heap_object(obj) != NULL, "must be");
    }

    VerifySharedOopClosure walker(is_archived);
    obj->oop_iterate(&walker);
  }
}
#endif

HeapShared::SeenObjectsTable* HeapShared::_seen_objects_table = NULL;
int HeapShared::_num_new_walked_objs;
int HeapShared::_num_new_archived_objs;
int HeapShared::_num_old_recorded_klasses;

int HeapShared::_num_total_subgraph_recordings = 0;
int HeapShared::_num_total_walked_objs = 0;
int HeapShared::_num_total_archived_objs = 0;
int HeapShared::_num_total_recorded_klasses = 0;
int HeapShared::_num_total_verifications = 0;

bool HeapShared::has_been_seen_during_subgraph_recording(oop obj) {
  return _seen_objects_table->get(obj) != NULL;
}

void HeapShared::set_has_been_seen_during_subgraph_recording(oop obj) {
  assert(!has_been_seen_during_subgraph_recording(obj), "sanity");
  _seen_objects_table->put(obj, true);
  ++ _num_new_walked_objs;
}

void HeapShared::start_recording_subgraph(InstanceKlass *k, const char* class_name, bool is_full_module_graph) {
  log_info(cds, heap)("Start recording subgraph(s) for archived fields in %s", class_name);
  init_subgraph_info(k, is_full_module_graph);
  init_seen_objects_table();
  _num_new_walked_objs = 0;
  _num_new_archived_objs = 0;
  _num_old_recorded_klasses = get_subgraph_info(k)->num_subgraph_object_klasses();
}

void HeapShared::done_recording_subgraph(InstanceKlass *k, const char* class_name) {
  int num_new_recorded_klasses = get_subgraph_info(k)->num_subgraph_object_klasses() -
    _num_old_recorded_klasses;
  log_info(cds, heap)("Done recording subgraph(s) for archived fields in %s: "
                      "walked %d objs, archived %d new objs, recorded %d classes",
                      class_name, _num_new_walked_objs, _num_new_archived_objs,
                      num_new_recorded_klasses);

  delete_seen_objects_table();

  _num_total_subgraph_recordings ++;
  _num_total_walked_objs      += _num_new_walked_objs;
  _num_total_archived_objs    += _num_new_archived_objs;
  _num_total_recorded_klasses +=  num_new_recorded_klasses;
}

class ArchivableStaticFieldFinder: public FieldClosure {
  InstanceKlass* _ik;
  Symbol* _field_name;
  bool _found;
  int _offset;
public:
  ArchivableStaticFieldFinder(InstanceKlass* ik, Symbol* field_name) :
    _ik(ik), _field_name(field_name), _found(false), _offset(-1) {}

  virtual void do_field(fieldDescriptor* fd) {
    if (fd->name() == _field_name) {
      assert(!_found, "fields cannot be overloaded");
      assert(is_reference_type(fd->field_type()), "can archive only fields that are references");
      _found = true;
      _offset = fd->offset();
    }
  }
  bool found()     { return _found;  }
  int offset()     { return _offset; }
};

void HeapShared::init_subgraph_entry_fields(ArchivableStaticFieldInfo fields[],
                                            int num, TRAPS) {
  for (int i = 0; i < num; i++) {
    ArchivableStaticFieldInfo* info = &fields[i];
    TempNewSymbol klass_name =  SymbolTable::new_symbol(info->klass_name);
    TempNewSymbol field_name =  SymbolTable::new_symbol(info->field_name);

    Klass* k = SystemDictionary::resolve_or_fail(klass_name, true, CHECK);
    InstanceKlass* ik = InstanceKlass::cast(k);
    assert(InstanceKlass::cast(ik)->is_shared_boot_class(),
           "Only support boot classes");
    ik->initialize(CHECK);

    ArchivableStaticFieldFinder finder(ik, field_name);
    ik->do_local_static_fields(&finder);
    assert(finder.found(), "field must exist");

    info->klass = ik;
    info->offset = finder.offset();
  }
}

void HeapShared::init_subgraph_entry_fields(TRAPS) {
  assert(is_heap_object_archiving_allowed(), "Sanity check");
  _dump_time_subgraph_info_table = new (ResourceObj::C_HEAP, mtClass)DumpTimeKlassSubGraphInfoTable();
  init_subgraph_entry_fields(closed_archive_subgraph_entry_fields,
                             num_closed_archive_subgraph_entry_fields,
                             CHECK);
  init_subgraph_entry_fields(open_archive_subgraph_entry_fields,
                             num_open_archive_subgraph_entry_fields,
                             CHECK);
  if (MetaspaceShared::use_full_module_graph()) {
    init_subgraph_entry_fields(fmg_open_archive_subgraph_entry_fields,
                               num_fmg_open_archive_subgraph_entry_fields,
                               CHECK);
  }
}

void HeapShared::init_for_dumping(TRAPS) {
  if (is_heap_object_archiving_allowed()) {
    _dumped_interned_strings = new (ResourceObj::C_HEAP, mtClass)DumpedInternedStrings();
    init_subgraph_entry_fields(CHECK);
  }
}

void HeapShared::archive_object_subgraphs(ArchivableStaticFieldInfo fields[],
                                          int num, bool is_closed_archive,
                                          bool is_full_module_graph) {
  _num_total_subgraph_recordings = 0;
  _num_total_walked_objs = 0;
  _num_total_archived_objs = 0;
  _num_total_recorded_klasses = 0;
  _num_total_verifications = 0;

  // For each class X that has one or more archived fields:
  // [1] Dump the subgraph of each archived field
  // [2] Create a list of all the class of the objects that can be reached
  //     by any of these static fields.
  //     At runtime, these classes are initialized before X's archived fields
  //     are restored by HeapShared::initialize_from_archived_subgraph().
  int i;
  for (i = 0; i < num; ) {
    ArchivableStaticFieldInfo* info = &fields[i];
    const char* klass_name = info->klass_name;
    start_recording_subgraph(info->klass, klass_name, is_full_module_graph);

    // If you have specified consecutive fields of the same klass in
    // fields[], these will be archived in the same
    // {start_recording_subgraph ... done_recording_subgraph} pass to
    // save time.
    for (; i < num; i++) {
      ArchivableStaticFieldInfo* f = &fields[i];
      if (f->klass_name != klass_name) {
        break;
      }

      archive_reachable_objects_from_static_field(f->klass, f->klass_name,
                                                  f->offset, f->field_name,
                                                  is_closed_archive);
    }
    done_recording_subgraph(info->klass, klass_name);
  }

  log_info(cds, heap)("Archived subgraph records in %s archive heap region = %d",
                      is_closed_archive ? "closed" : "open",
                      _num_total_subgraph_recordings);
  log_info(cds, heap)("  Walked %d objects", _num_total_walked_objs);
  log_info(cds, heap)("  Archived %d objects", _num_total_archived_objs);
  log_info(cds, heap)("  Recorded %d klasses", _num_total_recorded_klasses);

#ifndef PRODUCT
  for (int i = 0; i < num; i++) {
    ArchivableStaticFieldInfo* f = &fields[i];
    verify_subgraph_from_static_field(f->klass, f->offset);
  }
  log_info(cds, heap)("  Verified %d references", _num_total_verifications);
#endif
}

// Not all the strings in the global StringTable are dumped into the archive, because
// some of those strings may be only referenced by classes that are excluded from
// the archive. We need to explicitly mark the strings that are:
//   [1] used by classes that WILL be archived;
//   [2] included in the SharedArchiveConfigFile.
void HeapShared::add_to_dumped_interned_strings(oop string) {
  assert_at_safepoint(); // DumpedInternedStrings uses raw oops
  bool created;
  _dumped_interned_strings->put_if_absent(string, true, &created);
}

// At dump-time, find the location of all the non-null oop pointers in an archived heap
// region. This way we can quickly relocate all the pointers without using
// BasicOopIterateClosure at runtime.
class FindEmbeddedNonNullPointers: public BasicOopIterateClosure {
  narrowOop* _start;
  BitMap *_oopmap;
  int _num_total_oops;
  int _num_null_oops;
 public:
  FindEmbeddedNonNullPointers(narrowOop* start, BitMap* oopmap)
    : _start(start), _oopmap(oopmap), _num_total_oops(0),  _num_null_oops(0) {}

  virtual void do_oop(narrowOop* p) {
    _num_total_oops ++;
    narrowOop v = *p;
    if (!CompressedOops::is_null(v)) {
      size_t idx = p - _start;
      _oopmap->set_bit(idx);
    } else {
      _num_null_oops ++;
    }
  }
  virtual void do_oop(oop *p) {
    ShouldNotReachHere();
  }
  int num_total_oops() const { return _num_total_oops; }
  int num_null_oops()  const { return _num_null_oops; }
};

ResourceBitMap HeapShared::calculate_oopmap(MemRegion region) {
  assert(UseCompressedOops, "must be");
  size_t num_bits = region.byte_size() / sizeof(narrowOop);
  ResourceBitMap oopmap(num_bits);

  HeapWord* p   = region.start();
  HeapWord* end = region.end();
  FindEmbeddedNonNullPointers finder((narrowOop*)p, &oopmap);
  ArchiveBuilder* builder = DumpSharedSpaces ? ArchiveBuilder::current() : NULL;

  int num_objs = 0;
  while (p < end) {
    oop o = cast_to_oop(p);
    o->oop_iterate(&finder);
    p += o->size();
    if (DumpSharedSpaces) {
      builder->relocate_klass_ptr(o);
    }
    ++ num_objs;
  }

  log_info(cds, heap)("calculate_oopmap: objects = %6d, embedded oops = %7d, nulls = %7d",
                      num_objs, finder.num_total_oops(), finder.num_null_oops());
  return oopmap;
}

// Patch all the embedded oop pointers inside an archived heap region,
// to be consistent with the runtime oop encoding.
class PatchEmbeddedPointers: public BitMapClosure {
  narrowOop* _start;

 public:
  PatchEmbeddedPointers(narrowOop* start) : _start(start) {}

  bool do_bit(size_t offset) {
    narrowOop* p = _start + offset;
    narrowOop v = *p;
    assert(!CompressedOops::is_null(v), "null oops should have been filtered out at dump time");
    oop o = HeapShared::decode_from_archive(v);
    RawAccess<IS_NOT_NULL>::oop_store(p, o);
    return true;
  }
};

// Patch all the non-null pointers that are embedded in the archived heap objects
// in this region
void HeapShared::patch_embedded_pointers(MemRegion region, address oopmap,
                                         size_t oopmap_size_in_bits) {
  BitMapView bm((BitMap::bm_word_t*)oopmap, oopmap_size_in_bits);

#ifndef PRODUCT
  ResourceMark rm;
  ResourceBitMap checkBm = calculate_oopmap(region);
  assert(bm.is_same(checkBm), "sanity");
#endif

  PatchEmbeddedPointers patcher((narrowOop*)region.start());
  bm.iterate(&patcher);
}

#endif // INCLUDE_CDS_JAVA_HEAP
