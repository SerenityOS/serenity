/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "cds/archiveBuilder.hpp"
#include "cds/archiveUtils.inline.hpp"
#include "cds/dynamicArchive.hpp"
#include "cds/lambdaFormInvokers.hpp"
#include "cds/metaspaceShared.hpp"
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionaryShared.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/gcVMOperations.hpp"
#include "gc/shared/gc_globals.hpp"
#include "logging/log.hpp"
#include "memory/metaspaceClosure.hpp"
#include "memory/resourceArea.hpp"
#include "oops/klass.inline.hpp"
#include "runtime/arguments.hpp"
#include "runtime/os.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/vmThread.hpp"
#include "runtime/vmOperations.hpp"
#include "utilities/align.hpp"
#include "utilities/bitMap.inline.hpp"


class DynamicArchiveBuilder : public ArchiveBuilder {
public:
  void mark_pointer(address* ptr_loc) {
    ArchivePtrMarker::mark_pointer(ptr_loc);
  }

  template <typename T> T get_dumped_addr(T obj) {
    return (T)ArchiveBuilder::get_dumped_addr((address)obj);
  }

  static int dynamic_dump_method_comparator(Method* a, Method* b) {
    Symbol* a_name = a->name();
    Symbol* b_name = b->name();

    if (a_name == b_name) {
      return 0;
    }

    u4 a_offset = ArchiveBuilder::current()->any_to_offset_u4(a_name);
    u4 b_offset = ArchiveBuilder::current()->any_to_offset_u4(b_name);

    if (a_offset < b_offset) {
      return -1;
    } else {
      assert(a_offset > b_offset, "must be");
      return 1;
    }
  }

public:
  DynamicArchiveHeader *_header;

  void init_header();
  void release_header();
  void post_dump();
  void sort_methods();
  void sort_methods(InstanceKlass* ik) const;
  void remark_pointers_for_instance_klass(InstanceKlass* k, bool should_mark) const;
  void write_archive(char* serialized_data);

public:
  DynamicArchiveBuilder() : ArchiveBuilder() { }

  // Do this before and after the archive dump to see if any corruption
  // is caused by dynamic dumping.
  void verify_universe(const char* info) {
    if (VerifyBeforeExit) {
      log_info(cds)("Verify %s", info);
      // Among other things, this ensures that Eden top is correct.
      Universe::heap()->prepare_for_verify();
      Universe::verify(info);
    }
  }

  void doit() {
    SystemDictionaryShared::start_dumping();

    verify_universe("Before CDS dynamic dump");
    DEBUG_ONLY(SystemDictionaryShared::NoClassLoadingMark nclm);

    // Block concurrent class unloading from changing the _dumptime_table
    MutexLocker ml(DumpTimeTable_lock, Mutex::_no_safepoint_check_flag);
    SystemDictionaryShared::check_excluded_classes();

    // save dumptime tables
    SystemDictionaryShared::clone_dumptime_tables();

    init_header();
    gather_source_objs();
    reserve_buffer();

    log_info(cds, dynamic)("Copying %d klasses and %d symbols",
                           klasses()->length(), symbols()->length());
    dump_rw_metadata();
    dump_ro_metadata();
    relocate_metaspaceobj_embedded_pointers();
    relocate_roots();

    verify_estimate_size(_estimated_metaspaceobj_bytes, "MetaspaceObjs");

    char* serialized_data;
    {
      // Write the symbol table and system dictionaries to the RO space.
      // Note that these tables still point to the *original* objects, so
      // they would need to call DynamicArchive::original_to_target() to
      // get the correct addresses.
      assert(current_dump_space() == ro_region(), "Must be RO space");
      SymbolTable::write_to_archive(symbols());

      ArchiveBuilder::OtherROAllocMark mark;
      SystemDictionaryShared::write_to_archive(false);

      serialized_data = ro_region()->top();
      WriteClosure wc(ro_region());
      SymbolTable::serialize_shared_table_header(&wc, false);
      SystemDictionaryShared::serialize_dictionary_headers(&wc, false);
    }

    verify_estimate_size(_estimated_hashtable_bytes, "Hashtables");

    sort_methods();

    log_info(cds)("Make classes shareable");
    make_klasses_shareable();

    log_info(cds)("Adjust lambda proxy class dictionary");
    SystemDictionaryShared::adjust_lambda_proxy_class_dictionary();

    relocate_to_requested();

    write_archive(serialized_data);
    release_header();

    post_dump();

    // Restore dumptime tables
    SystemDictionaryShared::restore_dumptime_tables();

    assert(_num_dump_regions_used == _total_dump_regions, "must be");
    verify_universe("After CDS dynamic dump");
  }

  virtual void iterate_roots(MetaspaceClosure* it, bool is_relocating_pointers) {
    FileMapInfo::metaspace_pointers_do(it);
    SystemDictionaryShared::dumptime_classes_do(it);
  }
};

void DynamicArchiveBuilder::init_header() {
  FileMapInfo* mapinfo = new FileMapInfo(false);
  assert(FileMapInfo::dynamic_info() == mapinfo, "must be");
  _header = mapinfo->dynamic_header();

  FileMapInfo* base_info = FileMapInfo::current_info();
  _header->set_base_header_crc(base_info->crc());
  for (int i = 0; i < MetaspaceShared::n_regions; i++) {
    _header->set_base_region_crc(i, base_info->space_crc(i));
  }
  _header->populate(base_info, base_info->core_region_alignment());
}

void DynamicArchiveBuilder::release_header() {
  // We temporarily allocated a dynamic FileMapInfo for dumping, which makes it appear we
  // have mapped a dynamic archive, but we actually have not. We are in a safepoint now.
  // Let's free it so that if class loading happens after we leave the safepoint, nothing
  // bad will happen.
  assert(SafepointSynchronize::is_at_safepoint(), "must be");
  FileMapInfo *mapinfo = FileMapInfo::dynamic_info();
  assert(mapinfo != NULL && _header == mapinfo->dynamic_header(), "must be");
  delete mapinfo;
  assert(!DynamicArchive::is_mapped(), "must be");
  _header = NULL;
}

void DynamicArchiveBuilder::post_dump() {
  ArchivePtrMarker::reset_map_and_vs();
}

void DynamicArchiveBuilder::sort_methods() {
  InstanceKlass::disable_method_binary_search();
  for (int i = 0; i < klasses()->length(); i++) {
    Klass* k = klasses()->at(i);
    if (k->is_instance_klass()) {
      sort_methods(InstanceKlass::cast(k));
    }
  }
}

// The address order of the copied Symbols may be different than when the original
// klasses were created. Re-sort all the tables. See Method::sort_methods().
void DynamicArchiveBuilder::sort_methods(InstanceKlass* ik) const {
  assert(ik != NULL, "DynamicArchiveBuilder currently doesn't support dumping the base archive");
  if (MetaspaceShared::is_in_shared_metaspace(ik)) {
    // We have reached a supertype that's already in the base archive
    return;
  }

  if (ik->java_mirror() == NULL) {
    // NULL mirror means this class has already been visited and methods are already sorted
    return;
  }
  ik->remove_java_mirror();

  if (log_is_enabled(Debug, cds, dynamic)) {
    ResourceMark rm;
    log_debug(cds, dynamic)("sorting methods for " PTR_FORMAT " (" PTR_FORMAT ") %s",
                            p2i(ik), p2i(to_requested(ik)), ik->external_name());
  }

  // Method sorting may re-layout the [iv]tables, which would change the offset(s)
  // of the locations in an InstanceKlass that would contain pointers. Let's clear
  // all the existing pointer marking bits, and re-mark the pointers after sorting.
  remark_pointers_for_instance_klass(ik, false);

  // Make sure all supertypes have been sorted
  sort_methods(ik->java_super());
  Array<InstanceKlass*>* interfaces = ik->local_interfaces();
  int len = interfaces->length();
  for (int i = 0; i < len; i++) {
    sort_methods(interfaces->at(i));
  }

#ifdef ASSERT
  if (ik->methods() != NULL) {
    for (int m = 0; m < ik->methods()->length(); m++) {
      Symbol* name = ik->methods()->at(m)->name();
      assert(MetaspaceShared::is_in_shared_metaspace(name) || is_in_buffer_space(name), "must be");
    }
  }
  if (ik->default_methods() != NULL) {
    for (int m = 0; m < ik->default_methods()->length(); m++) {
      Symbol* name = ik->default_methods()->at(m)->name();
      assert(MetaspaceShared::is_in_shared_metaspace(name) || is_in_buffer_space(name), "must be");
    }
  }
#endif

  Method::sort_methods(ik->methods(), /*set_idnums=*/true, dynamic_dump_method_comparator);
  if (ik->default_methods() != NULL) {
    Method::sort_methods(ik->default_methods(), /*set_idnums=*/false, dynamic_dump_method_comparator);
  }
  if (ik->is_linked()) {
    // If the class has already been linked, we must relayout the i/v tables, whose order depends
    // on the method sorting order.
    // If the class is unlinked, we cannot layout the i/v tables yet. This is OK, as the
    // i/v tables will be initialized at runtime after bytecode verification.
    ik->vtable().initialize_vtable();
    ik->itable().initialize_itable();
  }

  // Set all the pointer marking bits after sorting.
  remark_pointers_for_instance_klass(ik, true);
}

template<bool should_mark>
class PointerRemarker: public MetaspaceClosure {
public:
  virtual bool do_ref(Ref* ref, bool read_only) {
    if (should_mark) {
      ArchivePtrMarker::mark_pointer(ref->addr());
    } else {
      ArchivePtrMarker::clear_pointer(ref->addr());
    }
    return false; // don't recurse
  }
};

void DynamicArchiveBuilder::remark_pointers_for_instance_klass(InstanceKlass* k, bool should_mark) const {
  if (should_mark) {
    PointerRemarker<true> marker;
    k->metaspace_pointers_do(&marker);
    marker.finish();
  } else {
    PointerRemarker<false> marker;
    k->metaspace_pointers_do(&marker);
    marker.finish();
  }
}

void DynamicArchiveBuilder::write_archive(char* serialized_data) {
  Array<u8>* table = FileMapInfo::saved_shared_path_table().table();
  SharedPathTable runtime_table(table, FileMapInfo::shared_path_table().size());
  _header->set_shared_path_table(runtime_table);
  _header->set_serialized_data(serialized_data);

  FileMapInfo* dynamic_info = FileMapInfo::dynamic_info();
  assert(dynamic_info != NULL, "Sanity");

  dynamic_info->open_for_write(Arguments::GetSharedDynamicArchivePath());
  ArchiveBuilder::write_archive(dynamic_info, NULL, NULL, NULL, NULL);

  address base = _requested_dynamic_archive_bottom;
  address top  = _requested_dynamic_archive_top;
  size_t file_size = pointer_delta(top, base, sizeof(char));

  log_info(cds, dynamic)("Written dynamic archive " PTR_FORMAT " - " PTR_FORMAT
                         " [" SIZE_FORMAT " bytes header, " SIZE_FORMAT " bytes total]",
                         p2i(base), p2i(top), _header->header_size(), file_size);

  log_info(cds, dynamic)("%d klasses; %d symbols", klasses()->length(), symbols()->length());
}

class VM_PopulateDynamicDumpSharedSpace: public VM_GC_Sync_Operation {
  DynamicArchiveBuilder builder;
public:
  VM_PopulateDynamicDumpSharedSpace() : VM_GC_Sync_Operation() {}
  VMOp_Type type() const { return VMOp_PopulateDumpSharedSpace; }
  void doit() {
    ResourceMark rm;
    if (SystemDictionaryShared::is_dumptime_table_empty()) {
      log_warning(cds, dynamic)("There is no class to be included in the dynamic archive.");
      return;
    }
    if (AllowArchivingWithJavaAgent) {
      warning("This archive was created with AllowArchivingWithJavaAgent. It should be used "
              "for testing purposes only and should not be used in a production environment");
    }
    FileMapInfo::check_nonempty_dir_in_shared_path_table();

    builder.doit();
  }
};

void DynamicArchive::prepare_for_dynamic_dumping() {
  EXCEPTION_MARK;
  ResourceMark rm(THREAD);
  MetaspaceShared::link_shared_classes(THREAD);
  if (HAS_PENDING_EXCEPTION) {
    log_error(cds)("Dynamic dump has failed");
    log_error(cds)("%s: %s", PENDING_EXCEPTION->klass()->external_name(),
                   java_lang_String::as_utf8_string(java_lang_Throwable::message(PENDING_EXCEPTION)));
    // We cannot continue to dump the archive anymore.
    DynamicDumpSharedSpaces = false;
    CLEAR_PENDING_EXCEPTION;
  }
}

void DynamicArchive::dump(const char* archive_name, TRAPS) {
  assert(UseSharedSpaces && RecordDynamicDumpInfo, "already checked in arguments.cpp?");
  assert(ArchiveClassesAtExit == nullptr, "already checked in arguments.cpp?");
  ArchiveClassesAtExit = archive_name;
  if (Arguments::init_shared_archive_paths()) {
    prepare_for_dynamic_dumping();
    if (DynamicDumpSharedSpaces) {
      dump(CHECK);
    }
  } else {
    ArchiveClassesAtExit = nullptr;
    THROW_MSG(vmSymbols::java_lang_RuntimeException(),
              "Could not setup SharedDynamicArchivePath");
  }
  // prevent do dynamic dump at exit.
  ArchiveClassesAtExit = nullptr;
  if (!Arguments::init_shared_archive_paths()) {
    THROW_MSG(vmSymbols::java_lang_RuntimeException(),
              "Could not restore SharedDynamicArchivePath");
  }
}

void DynamicArchive::dump(TRAPS) {
  if (Arguments::GetSharedDynamicArchivePath() == NULL) {
    log_warning(cds, dynamic)("SharedDynamicArchivePath is not specified");
    return;
  }

  // copy shared path table to saved.
  FileMapInfo::clone_shared_path_table(CHECK);

  VM_PopulateDynamicDumpSharedSpace op;
  VMThread::execute(&op);
}

bool DynamicArchive::validate(FileMapInfo* dynamic_info) {
  assert(!dynamic_info->is_static(), "must be");
  // Check if the recorded base archive matches with the current one
  FileMapInfo* base_info = FileMapInfo::current_info();
  DynamicArchiveHeader* dynamic_header = dynamic_info->dynamic_header();

  // Check the header crc
  if (dynamic_header->base_header_crc() != base_info->crc()) {
    FileMapInfo::fail_continue("Dynamic archive cannot be used: static archive header checksum verification failed.");
    return false;
  }

  // Check each space's crc
  for (int i = 0; i < MetaspaceShared::n_regions; i++) {
    if (dynamic_header->base_region_crc(i) != base_info->space_crc(i)) {
      FileMapInfo::fail_continue("Dynamic archive cannot be used: static archive region #%d checksum verification failed.", i);
      return false;
    }
  }

  return true;
}
