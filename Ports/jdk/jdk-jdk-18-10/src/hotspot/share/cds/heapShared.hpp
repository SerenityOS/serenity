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

#ifndef SHARE_CDS_HEAPSHARED_HPP
#define SHARE_CDS_HEAPSHARED_HPP

#include "cds/metaspaceShared.hpp"
#include "classfile/compactHashtable.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/systemDictionary.hpp"
#include "gc/shared/gc_globals.hpp"
#include "memory/allocation.hpp"
#include "oops/compressedOops.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/oop.hpp"
#include "oops/oopHandle.hpp"
#include "oops/typeArrayKlass.hpp"
#include "utilities/bitMap.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/resourceHash.hpp"

#if INCLUDE_CDS_JAVA_HEAP
class DumpedInternedStrings;

struct ArchivableStaticFieldInfo {
  const char* klass_name;
  const char* field_name;
  InstanceKlass* klass;
  int offset;
  BasicType type;
};

// A dump time sub-graph info for Klass _k. It includes the entry points
// (static fields in _k's mirror) of the archived sub-graphs reachable
// from _k's mirror. It also contains a list of Klasses of the objects
// within the sub-graphs.
class KlassSubGraphInfo: public CHeapObj<mtClass> {
 private:
  // The class that contains the static field(s) as the entry point(s)
  // of archived object sub-graph(s).
  Klass* _k;
  // A list of classes need to be loaded and initialized before the archived
  // object sub-graphs can be accessed at runtime.
  GrowableArray<Klass*>* _subgraph_object_klasses;
  // A list of _k's static fields as the entry points of archived sub-graphs.
  // For each entry field, it is a tuple of field_offset, field_value and
  // is_closed_archive flag.
  GrowableArray<int>* _subgraph_entry_fields;

  // Does this KlassSubGraphInfo belong to the archived full module graph
  bool _is_full_module_graph;

  // Does this KlassSubGraphInfo references any classes that were loaded while
  // JvmtiExport::is_early_phase()!=true. If so, this KlassSubGraphInfo cannot be
  // used at runtime if JVMTI ClassFileLoadHook is enabled.
  bool _has_non_early_klasses;
  static bool is_non_early_klass(Klass* k);

 public:
  KlassSubGraphInfo(Klass* k, bool is_full_module_graph) :
    _k(k),  _subgraph_object_klasses(NULL),
    _subgraph_entry_fields(NULL),
    _is_full_module_graph(is_full_module_graph),
    _has_non_early_klasses(false) {}

  ~KlassSubGraphInfo() {
    if (_subgraph_object_klasses != NULL) {
      delete _subgraph_object_klasses;
    }
    if (_subgraph_entry_fields != NULL) {
      delete _subgraph_entry_fields;
    }
  };

  Klass* klass()            { return _k; }
  GrowableArray<Klass*>* subgraph_object_klasses() {
    return _subgraph_object_klasses;
  }
  GrowableArray<int>* subgraph_entry_fields() {
    return _subgraph_entry_fields;
  }
  void add_subgraph_entry_field(int static_field_offset, oop v,
                                bool is_closed_archive);
  void add_subgraph_object_klass(Klass *orig_k);
  int num_subgraph_object_klasses() {
    return _subgraph_object_klasses == NULL ? 0 :
           _subgraph_object_klasses->length();
  }
  bool is_full_module_graph() const { return _is_full_module_graph; }
  bool has_non_early_klasses() const { return _has_non_early_klasses; }
};

// An archived record of object sub-graphs reachable from static
// fields within _k's mirror. The record is reloaded from the archive
// at runtime.
class ArchivedKlassSubGraphInfoRecord {
 private:
  Klass* _k;
  bool _is_full_module_graph;
  bool _has_non_early_klasses;

  // contains pairs of field offset and value for each subgraph entry field
  Array<int>* _entry_field_records;

  // klasses of objects in archived sub-graphs referenced from the entry points
  // (static fields) in the containing class
  Array<Klass*>* _subgraph_object_klasses;
 public:
  ArchivedKlassSubGraphInfoRecord() :
    _k(NULL), _entry_field_records(NULL), _subgraph_object_klasses(NULL) {}
  void init(KlassSubGraphInfo* info);
  Klass* klass() const { return _k; }
  Array<int>* entry_field_records() const { return _entry_field_records; }
  Array<Klass*>* subgraph_object_klasses() const { return _subgraph_object_klasses; }
  bool is_full_module_graph() const { return _is_full_module_graph; }
  bool has_non_early_klasses() const { return _has_non_early_klasses; }
};
#endif // INCLUDE_CDS_JAVA_HEAP

class HeapShared: AllStatic {
  friend class VerifySharedOopClosure;
 private:

#if INCLUDE_CDS_JAVA_HEAP
  static bool _closed_regions_mapped;
  static bool _open_regions_mapped;
  static DumpedInternedStrings *_dumped_interned_strings;

public:
  static unsigned oop_hash(oop const& p);
  static unsigned string_oop_hash(oop const& string) {
    return java_lang_String::hash_code(string);
  }

private:
  typedef ResourceHashtable<oop, oop,
      15889, // prime number
      ResourceObj::C_HEAP,
      mtClassShared,
      HeapShared::oop_hash> ArchivedObjectCache;
  static ArchivedObjectCache* _archived_object_cache;

  static unsigned klass_hash(Klass* const& klass) {
    // Generate deterministic hashcode even if SharedBaseAddress is changed due to ASLR.
    return primitive_hash<address>(address(klass) - SharedBaseAddress);
  }

  class DumpTimeKlassSubGraphInfoTable
    : public ResourceHashtable<Klass*, KlassSubGraphInfo,
                               137, // prime number
                               ResourceObj::C_HEAP,
                               mtClassShared,
                               HeapShared::klass_hash> {
  public:
    int _count;
  };

public: // solaris compiler wants this for RunTimeKlassSubGraphInfoTable
  inline static bool record_equals_compact_hashtable_entry(
       const ArchivedKlassSubGraphInfoRecord* value, const Klass* key, int len_unused) {
    return (value->klass() == key);
  }

private:
  typedef OffsetCompactHashtable<
    const Klass*,
    const ArchivedKlassSubGraphInfoRecord*,
    record_equals_compact_hashtable_entry
    > RunTimeKlassSubGraphInfoTable;

  static DumpTimeKlassSubGraphInfoTable* _dump_time_subgraph_info_table;
  static RunTimeKlassSubGraphInfoTable _run_time_subgraph_info_table;

  static void check_closed_region_object(InstanceKlass* k);

  static void archive_object_subgraphs(ArchivableStaticFieldInfo fields[],
                                       int num,
                                       bool is_closed_archive,
                                       bool is_full_module_graph);

  // Archive object sub-graph starting from the given static field
  // in Klass k's mirror.
  static void archive_reachable_objects_from_static_field(
    InstanceKlass* k, const char* klass_name,
    int field_offset, const char* field_name,
    bool is_closed_archive);

  static void verify_subgraph_from_static_field(
    InstanceKlass* k, int field_offset) PRODUCT_RETURN;
  static void verify_reachable_objects_from(oop obj, bool is_archived) PRODUCT_RETURN;
  static void verify_subgraph_from(oop orig_obj) PRODUCT_RETURN;

  static KlassSubGraphInfo* init_subgraph_info(Klass *k, bool is_full_module_graph);
  static KlassSubGraphInfo* get_subgraph_info(Klass *k);

  static void init_subgraph_entry_fields(TRAPS) NOT_CDS_JAVA_HEAP_RETURN;
  static void init_subgraph_entry_fields(ArchivableStaticFieldInfo fields[],
                                         int num, TRAPS);

  // Used by decode_from_archive
  static address _narrow_oop_base;
  static int     _narrow_oop_shift;

  typedef ResourceHashtable<oop, bool,
      15889, // prime number
      ResourceObj::C_HEAP,
      mtClassShared,
      HeapShared::oop_hash> SeenObjectsTable;

  static SeenObjectsTable *_seen_objects_table;

  static GrowableArrayCHeap<oop, mtClassShared>* _pending_roots;
  static narrowOop _roots_narrow;
  static OopHandle _roots;

  static void init_seen_objects_table() {
    assert(_seen_objects_table == NULL, "must be");
    _seen_objects_table = new (ResourceObj::C_HEAP, mtClass)SeenObjectsTable();
  }
  static void delete_seen_objects_table() {
    assert(_seen_objects_table != NULL, "must be");
    delete _seen_objects_table;
    _seen_objects_table = NULL;
  }

  // Statistics (for one round of start_recording_subgraph ... done_recording_subgraph)
  static int _num_new_walked_objs;
  static int _num_new_archived_objs;
  static int _num_old_recorded_klasses;

  // Statistics (for all archived subgraphs)
  static int _num_total_subgraph_recordings;
  static int _num_total_walked_objs;
  static int _num_total_archived_objs;
  static int _num_total_recorded_klasses;
  static int _num_total_verifications;

  static void start_recording_subgraph(InstanceKlass *k, const char* klass_name,
                                       bool is_full_module_graph);
  static void done_recording_subgraph(InstanceKlass *k, const char* klass_name);

  static bool has_been_seen_during_subgraph_recording(oop obj);
  static void set_has_been_seen_during_subgraph_recording(oop obj);

  static void check_module_oop(oop orig_module_obj);
  static void copy_roots();

  static void resolve_classes_for_subgraphs(ArchivableStaticFieldInfo fields[],
                                            int num, JavaThread* THREAD);
  static void resolve_classes_for_subgraph_of(Klass* k, JavaThread* THREAD);
  static void clear_archived_roots_of(Klass* k);
  static const ArchivedKlassSubGraphInfoRecord*
               resolve_or_init_classes_for_subgraph_of(Klass* k, bool do_init, TRAPS);
  static void resolve_or_init(Klass* k, bool do_init, TRAPS);
  static void init_archived_fields_for(Klass* k, const ArchivedKlassSubGraphInfoRecord* record);
 public:
  static void reset_archived_object_states(TRAPS);
  static void create_archived_object_cache() {
    _archived_object_cache =
      new (ResourceObj::C_HEAP, mtClass)ArchivedObjectCache();
  }
  static void destroy_archived_object_cache() {
    delete _archived_object_cache;
    _archived_object_cache = NULL;
  }
  static ArchivedObjectCache* archived_object_cache() {
    return _archived_object_cache;
  }

  static oop find_archived_heap_object(oop obj);
  static oop archive_object(oop obj);

  static void archive_klass_objects();

  static void archive_objects(GrowableArray<MemRegion>* closed_regions,
                              GrowableArray<MemRegion>* open_regions);
  static void copy_closed_objects(GrowableArray<MemRegion>* closed_regions);
  static void copy_open_objects(GrowableArray<MemRegion>* open_regions);

  static oop archive_reachable_objects_from(int level,
                                            KlassSubGraphInfo* subgraph_info,
                                            oop orig_obj,
                                            bool is_closed_archive);

  static ResourceBitMap calculate_oopmap(MemRegion region);
  static void add_to_dumped_interned_strings(oop string);

  // We use the HeapShared::roots() array to make sure that objects stored in the
  // archived heap regions are not prematurely collected. These roots include:
  //
  //    - mirrors of classes that have not yet been loaded.
  //    - ConstantPool::resolved_references() of classes that have not yet been loaded.
  //    - ArchivedKlassSubGraphInfoRecords that have not been initialized
  //    - java.lang.Module objects that have not yet been added to the module graph
  //
  // When a mirror M becomes referenced by a newly loaded class K, M will be removed
  // from HeapShared::roots() via clear_root(), and K will be responsible for
  // keeping M alive.
  //
  // Other types of roots are also cleared similarly when they become referenced.

  // Dump-time only. Returns the index of the root, which can be used at run time to read
  // the root using get_root(index, ...).
  static int append_root(oop obj);

  // Dump-time and runtime
  static objArrayOop roots();
  static oop get_root(int index, bool clear=false);

  // Run-time only
  static void set_roots(narrowOop roots);
  static void clear_root(int index);
#endif // INCLUDE_CDS_JAVA_HEAP

 public:
  static void run_full_gc_in_vm_thread() NOT_CDS_JAVA_HEAP_RETURN;

  static bool is_heap_object_archiving_allowed() {
    CDS_JAVA_HEAP_ONLY(return (UseG1GC && UseCompressedOops && UseCompressedClassPointers);)
    NOT_CDS_JAVA_HEAP(return false;)
  }

  static bool is_heap_region(int idx) {
    CDS_JAVA_HEAP_ONLY(return (idx >= MetaspaceShared::first_closed_heap_region &&
                               idx <= MetaspaceShared::last_open_heap_region);)
    NOT_CDS_JAVA_HEAP_RETURN_(false);
  }

  static void set_closed_regions_mapped() {
    CDS_JAVA_HEAP_ONLY(_closed_regions_mapped = true;)
    NOT_CDS_JAVA_HEAP_RETURN;
  }
  static bool closed_regions_mapped() {
    CDS_JAVA_HEAP_ONLY(return _closed_regions_mapped;)
    NOT_CDS_JAVA_HEAP_RETURN_(false);
  }
  static void set_open_regions_mapped() {
    CDS_JAVA_HEAP_ONLY(_open_regions_mapped = true;)
    NOT_CDS_JAVA_HEAP_RETURN;
  }
  static bool open_regions_mapped() {
    CDS_JAVA_HEAP_ONLY(return _open_regions_mapped;)
    NOT_CDS_JAVA_HEAP_RETURN_(false);
  }
  static bool is_mapped() {
    return closed_regions_mapped() && open_regions_mapped();
  }

  static void fixup_mapped_regions() NOT_CDS_JAVA_HEAP_RETURN;

  static bool is_archived_object_during_dumptime(oop p) NOT_CDS_JAVA_HEAP_RETURN_(false);

  static void resolve_classes(JavaThread* THREAD) NOT_CDS_JAVA_HEAP_RETURN;
  static void initialize_from_archived_subgraph(Klass* k, JavaThread* THREAD) NOT_CDS_JAVA_HEAP_RETURN;

  // NarrowOops stored in the CDS archive may use a different encoding scheme
  // than CompressedOops::{base,shift} -- see FileMapInfo::map_heap_regions_impl.
  // To decode them, do not use CompressedOops::decode_not_null. Use this
  // function instead.
  inline static oop decode_from_archive(narrowOop v) NOT_CDS_JAVA_HEAP_RETURN_(NULL);

  static void init_narrow_oop_decoding(address base, int shift) NOT_CDS_JAVA_HEAP_RETURN;

  static void patch_embedded_pointers(MemRegion region, address oopmap,
                                      size_t oopmap_in_bits) NOT_CDS_JAVA_HEAP_RETURN;

  static void init_for_dumping(TRAPS) NOT_CDS_JAVA_HEAP_RETURN;
  static void write_subgraph_info_table() NOT_CDS_JAVA_HEAP_RETURN;
  static void serialize_subgraph_info_table_header(SerializeClosure* soc) NOT_CDS_JAVA_HEAP_RETURN;
};

#if INCLUDE_CDS_JAVA_HEAP
class DumpedInternedStrings :
  public ResourceHashtable<oop, bool,
                           15889, // prime number
                           ResourceObj::C_HEAP,
                           mtClassShared,
                           HeapShared::string_oop_hash>
{};
#endif

#endif // SHARE_CDS_HEAPSHARED_HPP
