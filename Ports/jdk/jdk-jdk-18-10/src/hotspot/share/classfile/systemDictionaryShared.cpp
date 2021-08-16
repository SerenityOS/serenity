/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "cds/archiveUtils.hpp"
#include "cds/archiveBuilder.hpp"
#include "cds/classListParser.hpp"
#include "cds/classListWriter.hpp"
#include "cds/dynamicArchive.hpp"
#include "cds/filemap.hpp"
#include "cds/heapShared.hpp"
#include "cds/cdsProtectionDomain.hpp"
#include "cds/dumpTimeClassInfo.hpp"
#include "cds/metaspaceShared.hpp"
#include "cds/runTimeClassInfo.hpp"
#include "classfile/classFileStream.hpp"
#include "classfile/classLoader.hpp"
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/classLoaderDataGraph.hpp"
#include "classfile/classLoaderExt.hpp"
#include "classfile/dictionary.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/systemDictionaryShared.hpp"
#include "classfile/verificationType.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "interpreter/bootstrapInfo.hpp"
#include "jfr/jfrEvents.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/metaspaceClosure.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "runtime/arguments.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/hashtable.inline.hpp"
#include "utilities/resourceHash.hpp"
#include "utilities/stringUtils.hpp"

DumpTimeSharedClassTable* SystemDictionaryShared::_dumptime_table = NULL;
DumpTimeSharedClassTable* SystemDictionaryShared::_cloned_dumptime_table = NULL;
DumpTimeLambdaProxyClassDictionary* SystemDictionaryShared::_dumptime_lambda_proxy_class_dictionary = NULL;
DumpTimeLambdaProxyClassDictionary* SystemDictionaryShared::_cloned_dumptime_lambda_proxy_class_dictionary = NULL;
// SystemDictionaries in the base layer static archive
RunTimeSharedDictionary SystemDictionaryShared::_builtin_dictionary;
RunTimeSharedDictionary SystemDictionaryShared::_unregistered_dictionary;
// SystemDictionaries in the top layer dynamic archive
RunTimeSharedDictionary SystemDictionaryShared::_dynamic_builtin_dictionary;
RunTimeSharedDictionary SystemDictionaryShared::_dynamic_unregistered_dictionary;

LambdaProxyClassDictionary SystemDictionaryShared::_lambda_proxy_class_dictionary;
LambdaProxyClassDictionary SystemDictionaryShared::_dynamic_lambda_proxy_class_dictionary;

DEBUG_ONLY(bool SystemDictionaryShared::_no_class_loading_should_happen = false;)
bool SystemDictionaryShared::_dump_in_progress = false;

InstanceKlass* SystemDictionaryShared::load_shared_class_for_builtin_loader(
                 Symbol* class_name, Handle class_loader, TRAPS) {
  assert(UseSharedSpaces, "must be");
  InstanceKlass* ik = find_builtin_class(class_name);

  if (ik != NULL && !ik->shared_loading_failed()) {
    if ((SystemDictionary::is_system_class_loader(class_loader()) && ik->is_shared_app_class())  ||
        (SystemDictionary::is_platform_class_loader(class_loader()) && ik->is_shared_platform_class())) {
      SharedClassLoadingMark slm(THREAD, ik);
      PackageEntry* pkg_entry = CDSProtectionDomain::get_package_entry_from_class(ik, class_loader);
      Handle protection_domain =
        CDSProtectionDomain::init_security_info(class_loader, ik, pkg_entry, CHECK_NULL);
      return load_shared_class(ik, class_loader, protection_domain, NULL, pkg_entry, THREAD);
    }
  }
  return NULL;
}

// This function is called for loading only UNREGISTERED classes
InstanceKlass* SystemDictionaryShared::lookup_from_stream(Symbol* class_name,
                                                          Handle class_loader,
                                                          Handle protection_domain,
                                                          const ClassFileStream* cfs,
                                                          TRAPS) {
  if (!UseSharedSpaces) {
    return NULL;
  }
  if (class_name == NULL) {  // don't do this for hidden classes
    return NULL;
  }
  if (class_loader.is_null() ||
      SystemDictionary::is_system_class_loader(class_loader()) ||
      SystemDictionary::is_platform_class_loader(class_loader())) {
    // Do nothing for the BUILTIN loaders.
    return NULL;
  }

  const RunTimeClassInfo* record = find_record(&_unregistered_dictionary, &_dynamic_unregistered_dictionary, class_name);
  if (record == NULL) {
    return NULL;
  }

  int clsfile_size  = cfs->length();
  int clsfile_crc32 = ClassLoader::crc32(0, (const char*)cfs->buffer(), cfs->length());

  if (!record->matches(clsfile_size, clsfile_crc32)) {
    return NULL;
  }

  return acquire_class_for_current_thread(record->_klass, class_loader,
                                          protection_domain, cfs,
                                          THREAD);
}

InstanceKlass* SystemDictionaryShared::acquire_class_for_current_thread(
                   InstanceKlass *ik,
                   Handle class_loader,
                   Handle protection_domain,
                   const ClassFileStream *cfs,
                   TRAPS) {
  ClassLoaderData* loader_data = ClassLoaderData::class_loader_data(class_loader());

  {
    MutexLocker mu(THREAD, SharedDictionary_lock);
    if (ik->class_loader_data() != NULL) {
      //    ik is already loaded (by this loader or by a different loader)
      // or ik is being loaded by a different thread (by this loader or by a different loader)
      return NULL;
    }

    // No other thread has acquired this yet, so give it to *this thread*
    ik->set_class_loader_data(loader_data);
  }

  // No longer holding SharedDictionary_lock
  // No need to lock, as <ik> can be held only by a single thread.
  loader_data->add_class(ik);

  // Get the package entry.
  PackageEntry* pkg_entry = CDSProtectionDomain::get_package_entry_from_class(ik, class_loader);

  // Load and check super/interfaces, restore unsharable info
  InstanceKlass* shared_klass = load_shared_class(ik, class_loader, protection_domain,
                                                  cfs, pkg_entry, THREAD);
  if (shared_klass == NULL || HAS_PENDING_EXCEPTION) {
    // TODO: clean up <ik> so it can be used again
    return NULL;
  }

  return shared_klass;
}

void SystemDictionaryShared::start_dumping() {
  MutexLocker ml(DumpTimeTable_lock, Mutex::_no_safepoint_check_flag);
  _dump_in_progress = true;
}

DumpTimeClassInfo* SystemDictionaryShared::find_or_allocate_info_for(InstanceKlass* k) {
  MutexLocker ml(DumpTimeTable_lock, Mutex::_no_safepoint_check_flag);
  return find_or_allocate_info_for_locked(k);
}

DumpTimeClassInfo* SystemDictionaryShared::find_or_allocate_info_for_locked(InstanceKlass* k) {
  assert_lock_strong(DumpTimeTable_lock);
  if (_dumptime_table == NULL) {
    _dumptime_table = new (ResourceObj::C_HEAP, mtClass) DumpTimeSharedClassTable;
  }
  return _dumptime_table->find_or_allocate_info_for(k, _dump_in_progress);
}

bool SystemDictionaryShared::check_for_exclusion(InstanceKlass* k, DumpTimeClassInfo* info) {
  if (MetaspaceShared::is_in_shared_metaspace(k)) {
    // We have reached a super type that's already in the base archive. Treat it
    // as "not excluded".
    assert(DynamicDumpSharedSpaces, "must be");
    return false;
  }

  if (info == NULL) {
    info = _dumptime_table->get(k);
    assert(info != NULL, "supertypes of any classes in _dumptime_table must either be shared, or must also be in _dumptime_table");
  }

  if (!info->has_checked_exclusion()) {
    if (check_for_exclusion_impl(k)) {
      info->set_excluded();
    }
    info->set_has_checked_exclusion();
  }

  return info->is_excluded();
}

// Returns true so the caller can do:    return warn_excluded(".....");
bool SystemDictionaryShared::warn_excluded(InstanceKlass* k, const char* reason) {
  ResourceMark rm;
  log_warning(cds)("Skipping %s: %s", k->name()->as_C_string(), reason);
  return true;
}

bool SystemDictionaryShared::is_jfr_event_class(InstanceKlass *k) {
  while (k) {
    if (k->name()->equals("jdk/internal/event/Event")) {
      return true;
    }
    k = k->java_super();
  }
  return false;
}

bool SystemDictionaryShared::is_registered_lambda_proxy_class(InstanceKlass* ik) {
  DumpTimeClassInfo* info = _dumptime_table->get(ik);
  return (info != NULL) ? info->_is_archived_lambda_proxy : false;
}

bool SystemDictionaryShared::is_early_klass(InstanceKlass* ik) {
  DumpTimeClassInfo* info = _dumptime_table->get(ik);
  return (info != NULL) ? info->is_early_klass() : false;
}

bool SystemDictionaryShared::is_hidden_lambda_proxy(InstanceKlass* ik) {
  assert(ik->is_shared(), "applicable to only a shared class");
  if (ik->is_hidden()) {
    return true;
  } else {
    return false;
  }
}

bool SystemDictionaryShared::check_for_exclusion_impl(InstanceKlass* k) {
  if (k->is_in_error_state()) {
    return warn_excluded(k, "In error state");
  }
  if (k->is_scratch_class()) {
    return warn_excluded(k, "A scratch class");
  }
  if (!k->is_loaded()) {
    return warn_excluded(k, "Not in loaded state");
  }
  if (has_been_redefined(k)) {
    return warn_excluded(k, "Has been redefined");
  }
  if (!k->is_hidden() && k->shared_classpath_index() < 0 && is_builtin(k)) {
    // These are classes loaded from unsupported locations (such as those loaded by JVMTI native
    // agent during dump time).
    return warn_excluded(k, "Unsupported location");
  }
  if (k->signers() != NULL) {
    // We cannot include signed classes in the archive because the certificates
    // used during dump time may be different than those used during
    // runtime (due to expiration, etc).
    return warn_excluded(k, "Signed JAR");
  }
  if (is_jfr_event_class(k)) {
    // We cannot include JFR event classes because they need runtime-specific
    // instrumentation in order to work with -XX:FlightRecorderOptions:retransform=false.
    // There are only a small number of these classes, so it's not worthwhile to
    // support them and make CDS more complicated.
    return warn_excluded(k, "JFR event class");
  }

  if (!k->is_linked()) {
    if (has_class_failed_verification(k)) {
      return warn_excluded(k, "Failed verification");
    }
  } else {
    if (!k->can_be_verified_at_dumptime()) {
      // We have an old class that has been linked (e.g., it's been executed during
      // dump time). This class has been verified using the old verifier, which
      // doesn't save the verification constraints, so check_verification_constraints()
      // won't work at runtime.
      // As a result, we cannot store this class. It must be loaded and fully verified
      // at runtime.
      return warn_excluded(k, "Old class has been linked");
    }
  }

  if (k->is_hidden() && !is_registered_lambda_proxy_class(k)) {
    ResourceMark rm;
    log_debug(cds)("Skipping %s: Hidden class", k->name()->as_C_string());
    return true;
  }

  InstanceKlass* super = k->java_super();
  if (super != NULL && check_for_exclusion(super, NULL)) {
    ResourceMark rm;
    log_warning(cds)("Skipping %s: super class %s is excluded", k->name()->as_C_string(), super->name()->as_C_string());
    return true;
  }

  Array<InstanceKlass*>* interfaces = k->local_interfaces();
  int len = interfaces->length();
  for (int i = 0; i < len; i++) {
    InstanceKlass* intf = interfaces->at(i);
    if (check_for_exclusion(intf, NULL)) {
      log_warning(cds)("Skipping %s: interface %s is excluded", k->name()->as_C_string(), intf->name()->as_C_string());
      return true;
    }
  }

  return false; // false == k should NOT be excluded
}

bool SystemDictionaryShared::is_builtin_loader(ClassLoaderData* loader_data) {
  oop class_loader = loader_data->class_loader();
  return (class_loader == NULL ||
          SystemDictionary::is_system_class_loader(class_loader) ||
          SystemDictionary::is_platform_class_loader(class_loader));
}

bool SystemDictionaryShared::has_platform_or_app_classes() {
  if (FileMapInfo::current_info()->has_platform_or_app_classes()) {
    return true;
  }
  if (DynamicArchive::is_mapped() &&
      FileMapInfo::dynamic_info()->has_platform_or_app_classes()) {
    return true;
  }
  return false;
}

// The following stack shows how this code is reached:
//
//   [0] SystemDictionaryShared::find_or_load_shared_class()
//   [1] JVM_FindLoadedClass
//   [2] java.lang.ClassLoader.findLoadedClass0()
//   [3] java.lang.ClassLoader.findLoadedClass()
//   [4] jdk.internal.loader.BuiltinClassLoader.loadClassOrNull()
//   [5] jdk.internal.loader.BuiltinClassLoader.loadClass()
//   [6] jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(), or
//       jdk.internal.loader.ClassLoaders$PlatformClassLoader.loadClass()
//
// AppCDS supports fast class loading for these 2 built-in class loaders:
//    jdk.internal.loader.ClassLoaders$PlatformClassLoader
//    jdk.internal.loader.ClassLoaders$AppClassLoader
// with the following assumptions (based on the JDK core library source code):
//
// [a] these two loaders use the BuiltinClassLoader.loadClassOrNull() to
//     load the named class.
// [b] BuiltinClassLoader.loadClassOrNull() first calls findLoadedClass(name).
// [c] At this point, if we can find the named class inside the
//     shared_dictionary, we can perform further checks (see
//     SystemDictionary::is_shared_class_visible) to ensure that this class
//     was loaded by the same class loader during dump time.
//
// Given these assumptions, we intercept the findLoadedClass() call to invoke
// SystemDictionaryShared::find_or_load_shared_class() to load the shared class from
// the archive for the 2 built-in class loaders. This way,
// we can improve start-up because we avoid decoding the classfile,
// and avoid delegating to the parent loader.
//
// NOTE: there's a lot of assumption about the Java code. If any of that change, this
// needs to be redesigned.

InstanceKlass* SystemDictionaryShared::find_or_load_shared_class(
                 Symbol* name, Handle class_loader, TRAPS) {
  InstanceKlass* k = NULL;
  if (UseSharedSpaces) {
    if (!has_platform_or_app_classes()) {
      return NULL;
    }

    if (SystemDictionary::is_system_class_loader(class_loader()) ||
        SystemDictionary::is_platform_class_loader(class_loader())) {
      // Fix for 4474172; see evaluation for more details
      class_loader = Handle(
        THREAD, java_lang_ClassLoader::non_reflection_class_loader(class_loader()));
      ClassLoaderData *loader_data = register_loader(class_loader);
      Dictionary* dictionary = loader_data->dictionary();
      unsigned int d_hash = dictionary->compute_hash(name);

      // Note: currently, find_or_load_shared_class is called only from
      // JVM_FindLoadedClass and used for PlatformClassLoader and AppClassLoader,
      // which are parallel-capable loaders, so a lock here is NOT taken.
      assert(get_loader_lock_or_null(class_loader) == NULL, "ObjectLocker not required");
      {
        MutexLocker mu(THREAD, SystemDictionary_lock);
        InstanceKlass* check = dictionary->find_class(d_hash, name);
        if (check != NULL) {
          return check;
        }
      }

      k = load_shared_class_for_builtin_loader(name, class_loader, THREAD);
      if (k != NULL) {
        SharedClassLoadingMark slm(THREAD, k);
        k = find_or_define_instance_class(name, class_loader, k, CHECK_NULL);
      }
    }
  }
  return k;
}

class UnregisteredClassesTable : public ResourceHashtable<
  Symbol*, InstanceKlass*,
  15889, // prime number
  ResourceObj::C_HEAP> {};

static UnregisteredClassesTable* _unregistered_classes_table = NULL;

bool SystemDictionaryShared::add_unregistered_class(Thread* current, InstanceKlass* klass) {
  // We don't allow duplicated unregistered classes with the same name.
  // We only archive the first class with that name that succeeds putting
  // itself into the table.
  assert(Arguments::is_dumping_archive() || ClassListWriter::is_enabled(), "sanity");
  MutexLocker ml(current, UnregisteredClassesTable_lock);
  Symbol* name = klass->name();
  if (_unregistered_classes_table == NULL) {
    _unregistered_classes_table = new (ResourceObj::C_HEAP, mtClass)UnregisteredClassesTable();
  }
  bool created;
  InstanceKlass** v = _unregistered_classes_table->put_if_absent(name, klass, &created);
  if (created) {
    name->increment_refcount();
  }
  return (klass == *v);
}

// true == class was successfully added; false == a duplicated class (with the same name) already exists.
bool SystemDictionaryShared::add_unregistered_class_for_static_archive(Thread* current, InstanceKlass* k) {
  assert(DumpSharedSpaces, "only when dumping");
  if (add_unregistered_class(current, k)) {
    MutexLocker mu_r(current, Compile_lock); // add_to_hierarchy asserts this.
    SystemDictionary::add_to_hierarchy(k);
    return true;
  } else {
    return false;
  }
}

// This function is called to lookup the super/interfaces of shared classes for
// unregistered loaders. E.g., SharedClass in the below example
// where "super:" (and optionally "interface:") have been specified.
//
// java/lang/Object id: 0
// Interface    id: 2 super: 0 source: cust.jar
// SharedClass  id: 4 super: 0 interfaces: 2 source: cust.jar
InstanceKlass* SystemDictionaryShared::lookup_super_for_unregistered_class(
    Symbol* class_name, Symbol* super_name, bool is_superclass) {

  assert(DumpSharedSpaces, "only when static dumping");

  if (!ClassListParser::is_parsing_thread()) {
    // Unregistered classes can be created only by ClassListParser::_parsing_thread.

    return NULL;
  }

  ClassListParser* parser = ClassListParser::instance();
  if (parser == NULL) {
    // We're still loading the well-known classes, before the ClassListParser is created.
    return NULL;
  }
  if (class_name->equals(parser->current_class_name())) {
    // When this function is called, all the numbered super and interface types
    // must have already been loaded. Hence this function is never recursively called.
    if (is_superclass) {
      return parser->lookup_super_for_current_class(super_name);
    } else {
      return parser->lookup_interface_for_current_class(super_name);
    }
  } else {
    // The VM is not trying to resolve a super type of parser->current_class_name().
    // Instead, it's resolving an error class (because parser->current_class_name() has
    // failed parsing or verification). Don't do anything here.
    return NULL;
  }
}

void SystemDictionaryShared::set_shared_class_misc_info(InstanceKlass* k, ClassFileStream* cfs) {
  Arguments::assert_is_dumping_archive();
  assert(!is_builtin(k), "must be unregistered class");
  DumpTimeClassInfo* info = find_or_allocate_info_for(k);
  if (info != NULL) {
    info->_clsfile_size  = cfs->length();
    info->_clsfile_crc32 = ClassLoader::crc32(0, (const char*)cfs->buffer(), cfs->length());
  }
}

void SystemDictionaryShared::init_dumptime_info(InstanceKlass* k) {
  (void)find_or_allocate_info_for(k);
}

void SystemDictionaryShared::remove_dumptime_info(InstanceKlass* k) {
  MutexLocker ml(DumpTimeTable_lock, Mutex::_no_safepoint_check_flag);
  DumpTimeClassInfo* p = _dumptime_table->get(k);
  if (p == NULL) {
    return;
  }
  if (p->_verifier_constraints != NULL) {
    for (int i = 0; i < p->_verifier_constraints->length(); i++) {
      DumpTimeClassInfo::DTVerifierConstraint constraint = p->_verifier_constraints->at(i);
      if (constraint._name != NULL ) {
        constraint._name->decrement_refcount();
      }
      if (constraint._from_name != NULL ) {
        constraint._from_name->decrement_refcount();
      }
    }
    FREE_C_HEAP_ARRAY(DumpTimeClassInfo::DTVerifierConstraint, p->_verifier_constraints);
    p->_verifier_constraints = NULL;
    FREE_C_HEAP_ARRAY(char, p->_verifier_constraint_flags);
    p->_verifier_constraint_flags = NULL;
  }
  if (p->_loader_constraints != NULL) {
    for (int i = 0; i < p->_loader_constraints->length(); i++) {
      DumpTimeClassInfo::DTLoaderConstraint ld =  p->_loader_constraints->at(i);
      if (ld._name != NULL) {
        ld._name->decrement_refcount();
      }
    }
    FREE_C_HEAP_ARRAY(DumpTimeClassInfo::DTLoaderConstraint, p->_loader_constraints);
    p->_loader_constraints = NULL;
  }
  _dumptime_table->remove(k);
}

void SystemDictionaryShared::handle_class_unloading(InstanceKlass* klass) {
  if (Arguments::is_dumping_archive()) {
    remove_dumptime_info(klass);
  }

  if (_unregistered_classes_table != NULL) {
    // Remove the class from _unregistered_classes_table: keep the entry but
    // set it to NULL. This ensure no classes with the same name can be
    // added again.
    MutexLocker ml(Thread::current(), UnregisteredClassesTable_lock);
    InstanceKlass** v = _unregistered_classes_table->get(klass->name());
    if (v != NULL) {
      *v = NULL;
    }
  }

  if (ClassListWriter::is_enabled()) {
    ClassListWriter cw;
    cw.handle_class_unloading((const InstanceKlass*)klass);
  }
}

// Check if a class or any of its supertypes has been redefined.
bool SystemDictionaryShared::has_been_redefined(InstanceKlass* k) {
  if (k->has_been_redefined()) {
    return true;
  }
  if (k->java_super() != NULL && has_been_redefined(k->java_super())) {
    return true;
  }
  Array<InstanceKlass*>* interfaces = k->local_interfaces();
  int len = interfaces->length();
  for (int i = 0; i < len; i++) {
    if (has_been_redefined(interfaces->at(i))) {
      return true;
    }
  }
  return false;
}

// k is a class before relocating by ArchiveBuilder
void SystemDictionaryShared::validate_before_archiving(InstanceKlass* k) {
  ResourceMark rm;
  const char* name = k->name()->as_C_string();
  DumpTimeClassInfo* info = _dumptime_table->get(k);
  assert(_no_class_loading_should_happen, "class loading must be disabled");
  guarantee(info != NULL, "Class %s must be entered into _dumptime_table", name);
  guarantee(!info->is_excluded(), "Should not attempt to archive excluded class %s", name);
  if (is_builtin(k)) {
    if (k->is_hidden()) {
      assert(is_registered_lambda_proxy_class(k), "unexpected hidden class %s", name);
    }
    guarantee(!k->is_shared_unregistered_class(),
              "Class loader type must be set for BUILTIN class %s", name);

  } else {
    guarantee(k->is_shared_unregistered_class(),
              "Class loader type must not be set for UNREGISTERED class %s", name);
  }
}

class UnregisteredClassesDuplicationChecker : StackObj {
  GrowableArray<InstanceKlass*> _list;
  Thread* _thread;
public:
  UnregisteredClassesDuplicationChecker() : _thread(Thread::current()) {}

  bool do_entry(InstanceKlass* k, DumpTimeClassInfo& info) {
    if (!SystemDictionaryShared::is_builtin(k)) {
      _list.append(k);
    }
    return true;  // keep on iterating
  }

  static int compare_by_loader(InstanceKlass** a, InstanceKlass** b) {
    ClassLoaderData* loader_a = a[0]->class_loader_data();
    ClassLoaderData* loader_b = b[0]->class_loader_data();

    if (loader_a != loader_b) {
      return intx(loader_a) - intx(loader_b);
    } else {
      return intx(a[0]) - intx(b[0]);
    }
  }

  void mark_duplicated_classes() {
    // Two loaders may load two identical or similar hierarchies of classes. If we
    // check for duplication in random order, we may end up excluding important base classes
    // in both hierarchies, causing most of the classes to be excluded.
    // We sort the classes by their loaders. This way we're likely to archive
    // all classes in the one of the two hierarchies.
    _list.sort(compare_by_loader);
    for (int i = 0; i < _list.length(); i++) {
      InstanceKlass* k = _list.at(i);
      bool i_am_first = SystemDictionaryShared::add_unregistered_class(_thread, k);
      if (!i_am_first) {
        SystemDictionaryShared::warn_excluded(k, "Duplicated unregistered class");
        SystemDictionaryShared::set_excluded_locked(k);
      }
    }
  }
};

class ExcludeDumpTimeSharedClasses : StackObj {
public:
  bool do_entry(InstanceKlass* k, DumpTimeClassInfo& info) {
    SystemDictionaryShared::check_for_exclusion(k, &info);
    return true; // keep on iterating
  }
};

void SystemDictionaryShared::check_excluded_classes() {
  assert(no_class_loading_should_happen(), "sanity");
  assert_lock_strong(DumpTimeTable_lock);

  if (DynamicDumpSharedSpaces) {
    // Do this first -- if a base class is excluded due to duplication,
    // all of its subclasses will also be excluded by ExcludeDumpTimeSharedClasses
    ResourceMark rm;
    UnregisteredClassesDuplicationChecker dup_checker;
    _dumptime_table->iterate(&dup_checker);
    dup_checker.mark_duplicated_classes();
  }

  ExcludeDumpTimeSharedClasses excl;
  _dumptime_table->iterate(&excl);
  _dumptime_table->update_counts();
}

bool SystemDictionaryShared::is_excluded_class(InstanceKlass* k) {
  assert(_no_class_loading_should_happen, "sanity");
  assert_lock_strong(DumpTimeTable_lock);
  Arguments::assert_is_dumping_archive();
  DumpTimeClassInfo* p = find_or_allocate_info_for_locked(k);
  return (p == NULL) ? true : p->is_excluded();
}

void SystemDictionaryShared::set_excluded_locked(InstanceKlass* k) {
  assert_lock_strong(DumpTimeTable_lock);
  Arguments::assert_is_dumping_archive();
  DumpTimeClassInfo* info = find_or_allocate_info_for_locked(k);
  if (info != NULL) {
    info->set_excluded();
  }
}

void SystemDictionaryShared::set_excluded(InstanceKlass* k) {
  Arguments::assert_is_dumping_archive();
  DumpTimeClassInfo* info = find_or_allocate_info_for(k);
  if (info != NULL) {
    info->set_excluded();
  }
}

void SystemDictionaryShared::set_class_has_failed_verification(InstanceKlass* ik) {
  Arguments::assert_is_dumping_archive();
  DumpTimeClassInfo* p = find_or_allocate_info_for(ik);
  if (p != NULL) {
    p->set_failed_verification();
  }
}

bool SystemDictionaryShared::has_class_failed_verification(InstanceKlass* ik) {
  Arguments::assert_is_dumping_archive();
  if (_dumptime_table == NULL) {
    assert(DynamicDumpSharedSpaces, "sanity");
    assert(ik->is_shared(), "must be a shared class in the static archive");
    return false;
  }
  DumpTimeClassInfo* p = _dumptime_table->get(ik);
  return (p == NULL) ? false : p->failed_verification();
}

class IterateDumpTimeSharedClassTable : StackObj {
  MetaspaceClosure *_it;
public:
  IterateDumpTimeSharedClassTable(MetaspaceClosure* it) : _it(it) {}

  bool do_entry(InstanceKlass* k, DumpTimeClassInfo& info) {
    assert_lock_strong(DumpTimeTable_lock);
    if (k->is_loader_alive() && !info.is_excluded()) {
      info.metaspace_pointers_do(_it);
    }
    return true; // keep on iterating
  }
};

class IterateDumpTimeLambdaProxyClassDictionary : StackObj {
  MetaspaceClosure *_it;
public:
  IterateDumpTimeLambdaProxyClassDictionary(MetaspaceClosure* it) : _it(it) {}

  bool do_entry(LambdaProxyClassKey& key, DumpTimeLambdaProxyClassInfo& info) {
    assert_lock_strong(DumpTimeTable_lock);
    if (key.caller_ik()->is_loader_alive()) {
      info.metaspace_pointers_do(_it);
      key.metaspace_pointers_do(_it);
    }
    return true; // keep on iterating
  }
};

void SystemDictionaryShared::dumptime_classes_do(class MetaspaceClosure* it) {
  assert_lock_strong(DumpTimeTable_lock);
  IterateDumpTimeSharedClassTable iter(it);
  _dumptime_table->iterate(&iter);
  if (_dumptime_lambda_proxy_class_dictionary != NULL) {
    IterateDumpTimeLambdaProxyClassDictionary iter_lambda(it);
    _dumptime_lambda_proxy_class_dictionary->iterate(&iter_lambda);
  }
}

bool SystemDictionaryShared::add_verification_constraint(InstanceKlass* k, Symbol* name,
         Symbol* from_name, bool from_field_is_protected, bool from_is_array, bool from_is_object) {
  Arguments::assert_is_dumping_archive();
  DumpTimeClassInfo* info = find_or_allocate_info_for(k);
  if (info != NULL) {
    info->add_verification_constraint(k, name, from_name, from_field_is_protected,
                                      from_is_array, from_is_object);
  } else {
    return true;
  }
  if (DynamicDumpSharedSpaces) {
    // For dynamic dumping, we can resolve all the constraint classes for all class loaders during
    // the initial run prior to creating the archive before vm exit. We will also perform verification
    // check when running with the archive.
    return false;
  } else {
    if (is_builtin(k)) {
      // For builtin class loaders, we can try to complete the verification check at dump time,
      // because we can resolve all the constraint classes. We will also perform verification check
      // when running with the archive.
      return false;
    } else {
      // For non-builtin class loaders, we cannot complete the verification check at dump time,
      // because at dump time we don't know how to resolve classes for such loaders.
      return true;
    }
  }
}

void SystemDictionaryShared::add_to_dump_time_lambda_proxy_class_dictionary(LambdaProxyClassKey& key,
                                                           InstanceKlass* proxy_klass) {
  assert_lock_strong(DumpTimeTable_lock);
  if (_dumptime_lambda_proxy_class_dictionary == NULL) {
    _dumptime_lambda_proxy_class_dictionary =
      new (ResourceObj::C_HEAP, mtClass) DumpTimeLambdaProxyClassDictionary;
  }
  DumpTimeLambdaProxyClassInfo* lambda_info = _dumptime_lambda_proxy_class_dictionary->get(key);
  if (lambda_info == NULL) {
    DumpTimeLambdaProxyClassInfo info;
    info.add_proxy_klass(proxy_klass);
    _dumptime_lambda_proxy_class_dictionary->put(key, info);
    //lambda_info = _dumptime_lambda_proxy_class_dictionary->get(key);
    //assert(lambda_info->_proxy_klass == proxy_klass, "must be"); // debug only -- remove
    ++_dumptime_lambda_proxy_class_dictionary->_count;
  } else {
    lambda_info->add_proxy_klass(proxy_klass);
  }
}

void SystemDictionaryShared::add_lambda_proxy_class(InstanceKlass* caller_ik,
                                                    InstanceKlass* lambda_ik,
                                                    Symbol* invoked_name,
                                                    Symbol* invoked_type,
                                                    Symbol* method_type,
                                                    Method* member_method,
                                                    Symbol* instantiated_method_type,
                                                    TRAPS) {

  assert(caller_ik->class_loader() == lambda_ik->class_loader(), "mismatched class loader");
  assert(caller_ik->class_loader_data() == lambda_ik->class_loader_data(), "mismatched class loader data");
  assert(java_lang_Class::class_data(lambda_ik->java_mirror()) == NULL, "must not have class data");

  MutexLocker ml(DumpTimeTable_lock, Mutex::_no_safepoint_check_flag);

  lambda_ik->assign_class_loader_type();
  lambda_ik->set_shared_classpath_index(caller_ik->shared_classpath_index());
  InstanceKlass* nest_host = caller_ik->nest_host(CHECK);
  assert(nest_host != NULL, "unexpected NULL nest_host");

  DumpTimeClassInfo* info = _dumptime_table->get(lambda_ik);
  if (info != NULL && !lambda_ik->is_non_strong_hidden() && is_builtin(lambda_ik) && is_builtin(caller_ik)
      // Don't include the lambda proxy if its nest host is not in the "linked" state.
      && nest_host->is_linked()) {
    // Set _is_archived_lambda_proxy in DumpTimeClassInfo so that the lambda_ik
    // won't be excluded during dumping of shared archive. See ExcludeDumpTimeSharedClasses.
    info->_is_archived_lambda_proxy = true;
    info->set_nest_host(nest_host);

    LambdaProxyClassKey key(caller_ik,
                            invoked_name,
                            invoked_type,
                            method_type,
                            member_method,
                            instantiated_method_type);
    add_to_dump_time_lambda_proxy_class_dictionary(key, lambda_ik);
  }
}

InstanceKlass* SystemDictionaryShared::get_shared_lambda_proxy_class(InstanceKlass* caller_ik,
                                                                     Symbol* invoked_name,
                                                                     Symbol* invoked_type,
                                                                     Symbol* method_type,
                                                                     Method* member_method,
                                                                     Symbol* instantiated_method_type) {
  MutexLocker ml(CDSLambda_lock, Mutex::_no_safepoint_check_flag);
  LambdaProxyClassKey key(caller_ik, invoked_name, invoked_type,
                          method_type, member_method, instantiated_method_type);
  const RunTimeLambdaProxyClassInfo* info = _lambda_proxy_class_dictionary.lookup(&key, key.hash(), 0);
  if (info == NULL) {
    // Try lookup from the dynamic lambda proxy class dictionary.
    info = _dynamic_lambda_proxy_class_dictionary.lookup(&key, key.hash(), 0);
  }
  InstanceKlass* proxy_klass = NULL;
  if (info != NULL) {
    InstanceKlass* curr_klass = info->proxy_klass_head();
    InstanceKlass* prev_klass = curr_klass;
    if (curr_klass->lambda_proxy_is_available()) {
      while (curr_klass->next_link() != NULL) {
        prev_klass = curr_klass;
        curr_klass = InstanceKlass::cast(curr_klass->next_link());
      }
      assert(curr_klass->is_hidden(), "must be");
      assert(curr_klass->lambda_proxy_is_available(), "must be");

      prev_klass->set_next_link(NULL);
      proxy_klass = curr_klass;
      proxy_klass->clear_lambda_proxy_is_available();
      if (log_is_enabled(Debug, cds)) {
        ResourceMark rm;
        log_debug(cds)("Loaded lambda proxy: %s ", proxy_klass->external_name());
      }
    } else {
      if (log_is_enabled(Debug, cds)) {
        ResourceMark rm;
        log_debug(cds)("Used all archived lambda proxy classes for: %s %s%s",
                       caller_ik->external_name(), invoked_name->as_C_string(), invoked_type->as_C_string());
      }
    }
  }
  return proxy_klass;
}

InstanceKlass* SystemDictionaryShared::get_shared_nest_host(InstanceKlass* lambda_ik) {
  assert(!DumpSharedSpaces && UseSharedSpaces, "called at run time with CDS enabled only");
  RunTimeClassInfo* record = RunTimeClassInfo::get_for(lambda_ik);
  return record->nest_host();
}

InstanceKlass* SystemDictionaryShared::prepare_shared_lambda_proxy_class(InstanceKlass* lambda_ik,
                                                                         InstanceKlass* caller_ik, TRAPS) {
  Handle class_loader(THREAD, caller_ik->class_loader());
  Handle protection_domain;
  PackageEntry* pkg_entry = caller_ik->package();
  if (caller_ik->class_loader() != NULL) {
    protection_domain = CDSProtectionDomain::init_security_info(class_loader, caller_ik, pkg_entry, CHECK_NULL);
  }

  InstanceKlass* shared_nest_host = get_shared_nest_host(lambda_ik);
  assert(shared_nest_host != NULL, "unexpected NULL _nest_host");

  InstanceKlass* loaded_lambda =
    SystemDictionary::load_shared_lambda_proxy_class(lambda_ik, class_loader, protection_domain, pkg_entry, CHECK_NULL);

  if (loaded_lambda == NULL) {
    return NULL;
  }

  // Ensures the nest host is the same as the lambda proxy's
  // nest host recorded at dump time.
  InstanceKlass* nest_host = caller_ik->nest_host(THREAD);
  assert(nest_host == shared_nest_host, "mismatched nest host");

  EventClassLoad class_load_start_event;
  {
    MutexLocker mu_r(THREAD, Compile_lock);

    // Add to class hierarchy, and do possible deoptimizations.
    SystemDictionary::add_to_hierarchy(loaded_lambda);
    // But, do not add to dictionary.
  }
  loaded_lambda->link_class(CHECK_NULL);
  // notify jvmti
  if (JvmtiExport::should_post_class_load()) {
    JvmtiExport::post_class_load(THREAD, loaded_lambda);
  }
  if (class_load_start_event.should_commit()) {
    SystemDictionary::post_class_load_event(&class_load_start_event, loaded_lambda, ClassLoaderData::class_loader_data(class_loader()));
  }

  loaded_lambda->initialize(CHECK_NULL);

  return loaded_lambda;
}

void SystemDictionaryShared::check_verification_constraints(InstanceKlass* klass,
                                                            TRAPS) {
  assert(!DumpSharedSpaces && UseSharedSpaces, "called at run time with CDS enabled only");
  RunTimeClassInfo* record = RunTimeClassInfo::get_for(klass);

  int length = record->_num_verifier_constraints;
  if (length > 0) {
    for (int i = 0; i < length; i++) {
      RunTimeClassInfo::RTVerifierConstraint* vc = record->verifier_constraint_at(i);
      Symbol* name      = vc->name();
      Symbol* from_name = vc->from_name();
      char c            = record->verifier_constraint_flag(i);

      if (log_is_enabled(Trace, cds, verification)) {
        ResourceMark rm(THREAD);
        log_trace(cds, verification)("check_verification_constraint: %s: %s must be subclass of %s [0x%x]",
                                     klass->external_name(), from_name->as_klass_external_name(),
                                     name->as_klass_external_name(), c);
      }

      bool from_field_is_protected = (c & SystemDictionaryShared::FROM_FIELD_IS_PROTECTED) ? true : false;
      bool from_is_array           = (c & SystemDictionaryShared::FROM_IS_ARRAY)           ? true : false;
      bool from_is_object          = (c & SystemDictionaryShared::FROM_IS_OBJECT)          ? true : false;

      bool ok = VerificationType::resolve_and_check_assignability(klass, name,
         from_name, from_field_is_protected, from_is_array, from_is_object, CHECK);
      if (!ok) {
        ResourceMark rm(THREAD);
        stringStream ss;

        ss.print_cr("Bad type on operand stack");
        ss.print_cr("Exception Details:");
        ss.print_cr("  Location:\n    %s", klass->name()->as_C_string());
        ss.print_cr("  Reason:\n    Type '%s' is not assignable to '%s'",
                    from_name->as_quoted_ascii(), name->as_quoted_ascii());
        THROW_MSG(vmSymbols::java_lang_VerifyError(), ss.as_string());
      }
    }
  }
}

static oop get_class_loader_by(char type) {
  if (type == (char)ClassLoader::BOOT_LOADER) {
    return (oop)NULL;
  } else if (type == (char)ClassLoader::PLATFORM_LOADER) {
    return SystemDictionary::java_platform_loader();
  } else {
    assert (type == (char)ClassLoader::APP_LOADER, "Sanity");
    return SystemDictionary::java_system_loader();
  }
}

// Record class loader constraints that are checked inside
// InstanceKlass::link_class(), so that these can be checked quickly
// at runtime without laying out the vtable/itables.
void SystemDictionaryShared::record_linking_constraint(Symbol* name, InstanceKlass* klass,
                                                    Handle loader1, Handle loader2) {
  // A linking constraint check is executed when:
  //   - klass extends or implements type S
  //   - klass overrides method S.M(...) with X.M
  //     - If klass defines the method M, X is
  //       the same as klass.
  //     - If klass does not define the method M,
  //       X must be a supertype of klass and X.M is
  //       a default method defined by X.
  //   - loader1 = X->class_loader()
  //   - loader2 = S->class_loader()
  //   - loader1 != loader2
  //   - M's paramater(s) include an object type T
  // We require that
  //   - whenever loader1 and loader2 try to
  //     resolve the type T, they must always resolve to
  //     the same InstanceKlass.
  // NOTE: type T may or may not be currently resolved in
  // either of these two loaders. The check itself does not
  // try to resolve T.
  oop klass_loader = klass->class_loader();

  if (!is_system_class_loader(klass_loader) &&
      !is_platform_class_loader(klass_loader)) {
    // If klass is loaded by system/platform loaders, we can
    // guarantee that klass and S must be loaded by the same
    // respective loader between dump time and run time, and
    // the exact same check on (name, loader1, loader2) will
    // be executed. Hence, we can cache this check and execute
    // it at runtime without walking the vtable/itables.
    //
    // This cannot be guaranteed for classes loaded by other
    // loaders, so we bail.
    return;
  }

  if (DumpSharedSpaces && !is_builtin(klass)) {
    // During static dump, unregistered classes (those intended for
    // custom loaders) are loaded by the boot loader. Need to
    // exclude these for the same reason as above.
    // This should be fixed by JDK-8261941.
    return;
  }

  assert(klass_loader != NULL, "should not be called for boot loader");
  assert(loader1 != loader2, "must be");

  if (DynamicDumpSharedSpaces && Thread::current()->is_VM_thread()) {
    // We are re-laying out the vtable/itables of the *copy* of
    // a class during the final stage of dynamic dumping. The
    // linking constraints for this class has already been recorded.
    return;
  }
  assert(!Thread::current()->is_VM_thread(), "must be");

  Arguments::assert_is_dumping_archive();
  DumpTimeClassInfo* info = find_or_allocate_info_for(klass);
  if (info != NULL) {
    info->record_linking_constraint(name, loader1, loader2);
  }
}

// returns true IFF there's no need to re-initialize the i/v-tables for klass for
// the purpose of checking class loader constraints.
bool SystemDictionaryShared::check_linking_constraints(Thread* current, InstanceKlass* klass) {
  assert(!DumpSharedSpaces && UseSharedSpaces, "called at run time with CDS enabled only");
  LogTarget(Info, class, loader, constraints) log;
  if (klass->is_shared_boot_class()) {
    // No class loader constraint check performed for boot classes.
    return true;
  }
  if (klass->is_shared_platform_class() || klass->is_shared_app_class()) {
    RunTimeClassInfo* info = RunTimeClassInfo::get_for(klass);
    assert(info != NULL, "Sanity");
    if (info->_num_loader_constraints > 0) {
      HandleMark hm(current);
      for (int i = 0; i < info->_num_loader_constraints; i++) {
        RunTimeClassInfo::RTLoaderConstraint* lc = info->loader_constraint_at(i);
        Symbol* name = lc->constraint_name();
        Handle loader1(current, get_class_loader_by(lc->_loader_type1));
        Handle loader2(current, get_class_loader_by(lc->_loader_type2));
        if (log.is_enabled()) {
          ResourceMark rm(current);
          log.print("[CDS add loader constraint for class %s symbol %s loader[0] %s loader[1] %s",
                    klass->external_name(), name->as_C_string(),
                    ClassLoaderData::class_loader_data(loader1())->loader_name_and_id(),
                    ClassLoaderData::class_loader_data(loader2())->loader_name_and_id());
        }
        if (!SystemDictionary::add_loader_constraint(name, klass, loader1, loader2)) {
          // Loader constraint violation has been found. The caller
          // will re-layout the vtable/itables to produce the correct
          // exception.
          if (log.is_enabled()) {
            log.print(" failed]");
          }
          return false;
        }
        if (log.is_enabled()) {
            log.print(" succeeded]");
        }
      }
      return true; // for all recorded constraints added successully.
    }
  }
  if (log.is_enabled()) {
    ResourceMark rm(current);
    log.print("[CDS has not recorded loader constraint for class %s]", klass->external_name());
  }
  return false;
}

bool SystemDictionaryShared::is_supported_invokedynamic(BootstrapInfo* bsi) {
  LogTarget(Debug, cds, lambda) log;
  if (bsi->arg_values() == NULL || !bsi->arg_values()->is_objArray()) {
    if (log.is_enabled()) {
      LogStream log_stream(log);
      log.print("bsi check failed");
      log.print("    bsi->arg_values().not_null() %d", bsi->arg_values().not_null());
      if (bsi->arg_values().not_null()) {
        log.print("    bsi->arg_values()->is_objArray() %d", bsi->arg_values()->is_objArray());
        bsi->print_msg_on(&log_stream);
      }
    }
    return false;
  }

  Handle bsm = bsi->bsm();
  if (bsm.is_null() || !java_lang_invoke_DirectMethodHandle::is_instance(bsm())) {
    if (log.is_enabled()) {
      log.print("bsm check failed");
      log.print("    bsm.is_null() %d", bsm.is_null());
      log.print("    java_lang_invoke_DirectMethodHandle::is_instance(bsm()) %d",
        java_lang_invoke_DirectMethodHandle::is_instance(bsm()));
    }
    return false;
  }

  oop mn = java_lang_invoke_DirectMethodHandle::member(bsm());
  Method* method = java_lang_invoke_MemberName::vmtarget(mn);
  if (method->klass_name()->equals("java/lang/invoke/LambdaMetafactory") &&
      method->name()->equals("metafactory") &&
      method->signature()->equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;")) {
      return true;
  } else {
    if (log.is_enabled()) {
      ResourceMark rm;
      log.print("method check failed");
      log.print("    klass_name() %s", method->klass_name()->as_C_string());
      log.print("    name() %s", method->name()->as_C_string());
      log.print("    signature() %s", method->signature()->as_C_string());
    }
  }

  return false;
}

class EstimateSizeForArchive : StackObj {
  size_t _shared_class_info_size;
  int _num_builtin_klasses;
  int _num_unregistered_klasses;

public:
  EstimateSizeForArchive() {
    _shared_class_info_size = 0;
    _num_builtin_klasses = 0;
    _num_unregistered_klasses = 0;
  }

  bool do_entry(InstanceKlass* k, DumpTimeClassInfo& info) {
    if (!info.is_excluded()) {
      size_t byte_size = RunTimeClassInfo::byte_size(info._klass, info.num_verifier_constraints(), info.num_loader_constraints());
      _shared_class_info_size += align_up(byte_size, SharedSpaceObjectAlignment);
    }
    return true; // keep on iterating
  }

  size_t total() {
    return _shared_class_info_size;
  }
};

size_t SystemDictionaryShared::estimate_size_for_archive() {
  EstimateSizeForArchive est;
  _dumptime_table->iterate(&est);
  size_t total_size = est.total() +
    CompactHashtableWriter::estimate_size(_dumptime_table->count_of(true)) +
    CompactHashtableWriter::estimate_size(_dumptime_table->count_of(false));
  if (_dumptime_lambda_proxy_class_dictionary != NULL) {
    size_t bytesize = align_up(sizeof(RunTimeLambdaProxyClassInfo), SharedSpaceObjectAlignment);
    total_size +=
      (bytesize * _dumptime_lambda_proxy_class_dictionary->_count) +
      CompactHashtableWriter::estimate_size(_dumptime_lambda_proxy_class_dictionary->_count);
  } else {
    total_size += CompactHashtableWriter::estimate_size(0);
  }
  return total_size;
}

unsigned int SystemDictionaryShared::hash_for_shared_dictionary(address ptr) {
  if (ArchiveBuilder::is_active()) {
    uintx offset = ArchiveBuilder::current()->any_to_offset(ptr);
    unsigned int hash = primitive_hash<uintx>(offset);
    DEBUG_ONLY({
        if (MetaspaceObj::is_shared((const MetaspaceObj*)ptr)) {
          assert(hash == SystemDictionaryShared::hash_for_shared_dictionary_quick(ptr), "must be");
        }
      });
    return hash;
  } else {
    return SystemDictionaryShared::hash_for_shared_dictionary_quick(ptr);
  }
}

class CopyLambdaProxyClassInfoToArchive : StackObj {
  CompactHashtableWriter* _writer;
  ArchiveBuilder* _builder;
public:
  CopyLambdaProxyClassInfoToArchive(CompactHashtableWriter* writer)
  : _writer(writer), _builder(ArchiveBuilder::current()) {}
  bool do_entry(LambdaProxyClassKey& key, DumpTimeLambdaProxyClassInfo& info) {
    // In static dump, info._proxy_klasses->at(0) is already relocated to point to the archived class
    // (not the original class).
    //
    // The following check has been moved to SystemDictionaryShared::check_excluded_classes(), which
    // happens before the classes are copied.
    //
    // if (SystemDictionaryShared::is_excluded_class(info._proxy_klasses->at(0))) {
    //  return true;
    //}
    ResourceMark rm;
    log_info(cds,dynamic)("Archiving hidden %s", info._proxy_klasses->at(0)->external_name());
    size_t byte_size = sizeof(RunTimeLambdaProxyClassInfo);
    RunTimeLambdaProxyClassInfo* runtime_info =
        (RunTimeLambdaProxyClassInfo*)ArchiveBuilder::ro_region_alloc(byte_size);
    runtime_info->init(key, info);
    unsigned int hash = runtime_info->hash();
    u4 delta = _builder->any_to_offset_u4((void*)runtime_info);
    _writer->add(hash, delta);
    return true;
  }
};

class AdjustLambdaProxyClassInfo : StackObj {
public:
  AdjustLambdaProxyClassInfo() {}
  bool do_entry(LambdaProxyClassKey& key, DumpTimeLambdaProxyClassInfo& info) {
    int len = info._proxy_klasses->length();
    if (len > 1) {
      for (int i = 0; i < len-1; i++) {
        InstanceKlass* ok0 = info._proxy_klasses->at(i+0); // this is original klass
        InstanceKlass* ok1 = info._proxy_klasses->at(i+1); // this is original klass
        assert(ArchiveBuilder::current()->is_in_buffer_space(ok0), "must be");
        assert(ArchiveBuilder::current()->is_in_buffer_space(ok1), "must be");
        InstanceKlass* bk0 = ok0;
        InstanceKlass* bk1 = ok1;
        assert(bk0->next_link() == 0, "must be called after Klass::remove_unshareable_info()");
        assert(bk1->next_link() == 0, "must be called after Klass::remove_unshareable_info()");
        bk0->set_next_link(bk1);
        bk1->set_lambda_proxy_is_available();
        ArchivePtrMarker::mark_pointer(bk0->next_link_addr());
      }
    }
    info._proxy_klasses->at(0)->set_lambda_proxy_is_available();

    return true;
  }
};

class CopySharedClassInfoToArchive : StackObj {
  CompactHashtableWriter* _writer;
  bool _is_builtin;
  ArchiveBuilder *_builder;
public:
  CopySharedClassInfoToArchive(CompactHashtableWriter* writer,
                               bool is_builtin)
    : _writer(writer), _is_builtin(is_builtin), _builder(ArchiveBuilder::current()) {}

  bool do_entry(InstanceKlass* k, DumpTimeClassInfo& info) {
    if (!info.is_excluded() && info.is_builtin() == _is_builtin) {
      size_t byte_size = RunTimeClassInfo::byte_size(info._klass, info.num_verifier_constraints(), info.num_loader_constraints());
      RunTimeClassInfo* record;
      record = (RunTimeClassInfo*)ArchiveBuilder::ro_region_alloc(byte_size);
      record->init(info);

      unsigned int hash;
      Symbol* name = info._klass->name();
      hash = SystemDictionaryShared::hash_for_shared_dictionary((address)name);
      u4 delta = _builder->buffer_to_offset_u4((address)record);
      if (_is_builtin && info._klass->is_hidden()) {
        // skip
      } else {
        _writer->add(hash, delta);
      }
      if (log_is_enabled(Trace, cds, hashtables)) {
        ResourceMark rm;
        log_trace(cds,hashtables)("%s dictionary: %s", (_is_builtin ? "builtin" : "unregistered"), info._klass->external_name());
      }

      // Save this for quick runtime lookup of InstanceKlass* -> RunTimeClassInfo*
      RunTimeClassInfo::set_for(info._klass, record);
    }
    return true; // keep on iterating
  }
};

void SystemDictionaryShared::write_lambda_proxy_class_dictionary(LambdaProxyClassDictionary *dictionary) {
  CompactHashtableStats stats;
  dictionary->reset();
  CompactHashtableWriter writer(_dumptime_lambda_proxy_class_dictionary->_count, &stats);
  CopyLambdaProxyClassInfoToArchive copy(&writer);
  _dumptime_lambda_proxy_class_dictionary->iterate(&copy);
  writer.dump(dictionary, "lambda proxy class dictionary");
}

void SystemDictionaryShared::write_dictionary(RunTimeSharedDictionary* dictionary,
                                              bool is_builtin) {
  CompactHashtableStats stats;
  dictionary->reset();
  CompactHashtableWriter writer(_dumptime_table->count_of(is_builtin), &stats);
  CopySharedClassInfoToArchive copy(&writer, is_builtin);
  assert_lock_strong(DumpTimeTable_lock);
  _dumptime_table->iterate(&copy);
  writer.dump(dictionary, is_builtin ? "builtin dictionary" : "unregistered dictionary");
}

void SystemDictionaryShared::write_to_archive(bool is_static_archive) {
  if (is_static_archive) {
    write_dictionary(&_builtin_dictionary, true);
    write_dictionary(&_unregistered_dictionary, false);
  } else {
    write_dictionary(&_dynamic_builtin_dictionary, true);
    write_dictionary(&_dynamic_unregistered_dictionary, false);
  }
  if (_dumptime_lambda_proxy_class_dictionary != NULL) {
    write_lambda_proxy_class_dictionary(&_lambda_proxy_class_dictionary);
  }
}

void SystemDictionaryShared::adjust_lambda_proxy_class_dictionary() {
  if (_dumptime_lambda_proxy_class_dictionary != NULL) {
    AdjustLambdaProxyClassInfo adjuster;
    _dumptime_lambda_proxy_class_dictionary->iterate(&adjuster);
  }
}

void SystemDictionaryShared::serialize_dictionary_headers(SerializeClosure* soc,
                                                          bool is_static_archive) {
  FileMapInfo *dynamic_mapinfo = FileMapInfo::dynamic_info();
  if (is_static_archive) {
    _builtin_dictionary.serialize_header(soc);
    _unregistered_dictionary.serialize_header(soc);
    if (dynamic_mapinfo == NULL || DynamicDumpSharedSpaces || (dynamic_mapinfo != NULL && UseSharedSpaces)) {
      _lambda_proxy_class_dictionary.serialize_header(soc);
    }
  } else {
    _dynamic_builtin_dictionary.serialize_header(soc);
    _dynamic_unregistered_dictionary.serialize_header(soc);
    if (DynamicDumpSharedSpaces) {
      _lambda_proxy_class_dictionary.serialize_header(soc);
    } else {
      _dynamic_lambda_proxy_class_dictionary.serialize_header(soc);
    }
  }
}

void SystemDictionaryShared::serialize_vm_classes(SerializeClosure* soc) {
  for (auto id : EnumRange<vmClassID>{}) {
    soc->do_ptr((void**)vmClasses::klass_addr_at(id));
  }
}

const RunTimeClassInfo*
SystemDictionaryShared::find_record(RunTimeSharedDictionary* static_dict, RunTimeSharedDictionary* dynamic_dict, Symbol* name) {
  if (!UseSharedSpaces || !name->is_shared()) {
    // The names of all shared classes must also be a shared Symbol.
    return NULL;
  }

  unsigned int hash = SystemDictionaryShared::hash_for_shared_dictionary_quick(name);
  const RunTimeClassInfo* record = NULL;
  if (DynamicArchive::is_mapped()) {
    // Those regenerated holder classes are in dynamic archive
    if (name == vmSymbols::java_lang_invoke_Invokers_Holder() ||
        name == vmSymbols::java_lang_invoke_DirectMethodHandle_Holder() ||
        name == vmSymbols::java_lang_invoke_LambdaForm_Holder() ||
        name == vmSymbols::java_lang_invoke_DelegatingMethodHandle_Holder()) {
      record = dynamic_dict->lookup(name, hash, 0);
      if (record != nullptr) {
        return record;
      }
    }
  }

  if (!MetaspaceShared::is_shared_dynamic(name)) {
    // The names of all shared classes in the static dict must also be in the
    // static archive
    record = static_dict->lookup(name, hash, 0);
  }

  if (record == NULL && DynamicArchive::is_mapped()) {
    record = dynamic_dict->lookup(name, hash, 0);
  }

  return record;
}

InstanceKlass* SystemDictionaryShared::find_builtin_class(Symbol* name) {
  const RunTimeClassInfo* record = find_record(&_builtin_dictionary, &_dynamic_builtin_dictionary, name);
  if (record != NULL) {
    assert(!record->_klass->is_hidden(), "hidden class cannot be looked up by name");
    assert(check_alignment(record->_klass), "Address not aligned");
    return record->_klass;
  } else {
    return NULL;
  }
}

void SystemDictionaryShared::update_shared_entry(InstanceKlass* k, int id) {
  assert(DumpSharedSpaces, "supported only when dumping");
  DumpTimeClassInfo* info = find_or_allocate_info_for(k);
  info->_id = id;
}

const char* class_loader_name_for_shared(Klass* k) {
  assert(k != nullptr, "Sanity");
  assert(k->is_shared(), "Must be");
  assert(k->is_instance_klass(), "Must be");
  InstanceKlass* ik = InstanceKlass::cast(k);
  if (ik->is_shared_boot_class()) {
    return "boot_loader";
  } else if (ik->is_shared_platform_class()) {
    return "platform_loader";
  } else if (ik->is_shared_app_class()) {
    return "app_loader";
  } else if (ik->is_shared_unregistered_class()) {
    return "unregistered_loader";
  } else {
    return "unknown loader";
  }
}

class SharedDictionaryPrinter : StackObj {
  outputStream* _st;
  int _index;
public:
  SharedDictionaryPrinter(outputStream* st) : _st(st), _index(0) {}

  void do_value(const RunTimeClassInfo* record) {
    ResourceMark rm;
    _st->print_cr("%4d: %s %s", _index++, record->_klass->external_name(),
        class_loader_name_for_shared(record->_klass));
  }
  int index() const { return _index; }
};

class SharedLambdaDictionaryPrinter : StackObj {
  outputStream* _st;
  int _index;
public:
  SharedLambdaDictionaryPrinter(outputStream* st, int idx) : _st(st), _index(idx) {}

  void do_value(const RunTimeLambdaProxyClassInfo* record) {
    if (record->proxy_klass_head()->lambda_proxy_is_available()) {
      ResourceMark rm;
      Klass* k = record->proxy_klass_head();
      while (k != nullptr) {
        _st->print_cr("%4d: %s %s", _index++, k->external_name(),
                      class_loader_name_for_shared(k));
        k = k->next_link();
      }
    }
  }
};

void SystemDictionaryShared::print_on(const char* prefix,
                                      RunTimeSharedDictionary* builtin_dictionary,
                                      RunTimeSharedDictionary* unregistered_dictionary,
                                      LambdaProxyClassDictionary* lambda_dictionary,
                                      outputStream* st) {
  st->print_cr("%sShared Dictionary", prefix);
  SharedDictionaryPrinter p(st);
  st->print_cr("%sShared Builtin Dictionary", prefix);
  builtin_dictionary->iterate(&p);
  st->print_cr("%sShared Unregistered Dictionary", prefix);
  unregistered_dictionary->iterate(&p);
  if (!lambda_dictionary->empty()) {
    st->print_cr("%sShared Lambda Dictionary", prefix);
    SharedLambdaDictionaryPrinter ldp(st, p.index());
    lambda_dictionary->iterate(&ldp);
  }
}

void SystemDictionaryShared::print_shared_archive(outputStream* st, bool is_static) {
  if (UseSharedSpaces) {
    if (is_static) {
      print_on("", &_builtin_dictionary, &_unregistered_dictionary, &_lambda_proxy_class_dictionary, st);
    } else {
      if (DynamicArchive::is_mapped()) {
        print_on("", &_dynamic_builtin_dictionary, &_dynamic_unregistered_dictionary,
               &_dynamic_lambda_proxy_class_dictionary, st);
      }
    }
  }
}

void SystemDictionaryShared::print_on(outputStream* st) {
  if (UseSharedSpaces) {
    print_on("", &_builtin_dictionary, &_unregistered_dictionary, &_lambda_proxy_class_dictionary, st);
    if (DynamicArchive::is_mapped()) {
      print_on("", &_dynamic_builtin_dictionary, &_dynamic_unregistered_dictionary,
               &_dynamic_lambda_proxy_class_dictionary, st);
    }
  }
}

void SystemDictionaryShared::print_table_statistics(outputStream* st) {
  if (UseSharedSpaces) {
    _builtin_dictionary.print_table_statistics(st, "Builtin Shared Dictionary");
    _unregistered_dictionary.print_table_statistics(st, "Unregistered Shared Dictionary");
    _lambda_proxy_class_dictionary.print_table_statistics(st, "Lambda Shared Dictionary");
    if (DynamicArchive::is_mapped()) {
      _dynamic_builtin_dictionary.print_table_statistics(st, "Dynamic Builtin Shared Dictionary");
      _dynamic_unregistered_dictionary.print_table_statistics(st, "Unregistered Shared Dictionary");
      _dynamic_lambda_proxy_class_dictionary.print_table_statistics(st, "Dynamic Lambda Shared Dictionary");
    }
  }
}

bool SystemDictionaryShared::is_dumptime_table_empty() {
  if (_dumptime_table == NULL) {
    return true;
  }
  _dumptime_table->update_counts();
  if (_dumptime_table->count_of(true) == 0 && _dumptime_table->count_of(false) == 0){
    return true;
  }
  return false;
}

class CloneDumpTimeClassTable: public StackObj {
  DumpTimeSharedClassTable* _table;
  DumpTimeSharedClassTable* _cloned_table;
 public:
  CloneDumpTimeClassTable(DumpTimeSharedClassTable* table, DumpTimeSharedClassTable* clone) :
                      _table(table), _cloned_table(clone) {
    assert(_table != NULL, "_dumptime_table is NULL");
    assert(_cloned_table != NULL, "_cloned_table is NULL");
  }
  bool do_entry(InstanceKlass* k, DumpTimeClassInfo& info) {
    if (!info.is_excluded()) {
      bool created;
      _cloned_table->put_if_absent(k, info.clone(), &created);
    }
    return true; // keep on iterating
  }
};

class CloneDumpTimeLambdaProxyClassTable: StackObj {
  DumpTimeLambdaProxyClassDictionary* _table;
  DumpTimeLambdaProxyClassDictionary* _cloned_table;
 public:
  CloneDumpTimeLambdaProxyClassTable(DumpTimeLambdaProxyClassDictionary* table,
                                     DumpTimeLambdaProxyClassDictionary* clone) :
                      _table(table), _cloned_table(clone) {
    assert(_table != NULL, "_dumptime_table is NULL");
    assert(_cloned_table != NULL, "_cloned_table is NULL");
  }

  bool do_entry(LambdaProxyClassKey& key, DumpTimeLambdaProxyClassInfo& info) {
    assert_lock_strong(DumpTimeTable_lock);
    bool created;
    // make copies then store in _clone_table
    LambdaProxyClassKey keyCopy = key;
    _cloned_table->put_if_absent(keyCopy, info.clone(), &created);
    ++ _cloned_table->_count;
    return true; // keep on iterating
  }
};

void SystemDictionaryShared::clone_dumptime_tables() {
  Arguments::assert_is_dumping_archive();
  assert_lock_strong(DumpTimeTable_lock);
  if (_dumptime_table != NULL) {
    assert(_cloned_dumptime_table == NULL, "_cloned_dumptime_table must be cleaned");
    _cloned_dumptime_table = new (ResourceObj::C_HEAP, mtClass) DumpTimeSharedClassTable;
    CloneDumpTimeClassTable copy_classes(_dumptime_table, _cloned_dumptime_table);
    _dumptime_table->iterate(&copy_classes);
    _cloned_dumptime_table->update_counts();
  }
  if (_dumptime_lambda_proxy_class_dictionary != NULL) {
    assert(_cloned_dumptime_lambda_proxy_class_dictionary == NULL,
           "_cloned_dumptime_lambda_proxy_class_dictionary must be cleaned");
    _cloned_dumptime_lambda_proxy_class_dictionary =
                                          new (ResourceObj::C_HEAP, mtClass) DumpTimeLambdaProxyClassDictionary;
    CloneDumpTimeLambdaProxyClassTable copy_proxy_classes(_dumptime_lambda_proxy_class_dictionary,
                                                          _cloned_dumptime_lambda_proxy_class_dictionary);
    _dumptime_lambda_proxy_class_dictionary->iterate(&copy_proxy_classes);
  }
}

void SystemDictionaryShared::restore_dumptime_tables() {
  assert_lock_strong(DumpTimeTable_lock);
  delete _dumptime_table;
  _dumptime_table = _cloned_dumptime_table;
  _cloned_dumptime_table = NULL;
  delete _dumptime_lambda_proxy_class_dictionary;
  _dumptime_lambda_proxy_class_dictionary = _cloned_dumptime_lambda_proxy_class_dictionary;
  _cloned_dumptime_lambda_proxy_class_dictionary = NULL;
}

#if INCLUDE_CDS_JAVA_HEAP

class ArchivedMirrorPatcher {
protected:
  static void update(Klass* k) {
    if (k->has_archived_mirror_index()) {
      oop m = k->archived_java_mirror();
      if (m != NULL) {
        java_lang_Class::update_archived_mirror_native_pointers(m);
      }
    }
  }

public:
  static void update_array_klasses(Klass* ak) {
    while (ak != NULL) {
      update(ak);
      ak = ArrayKlass::cast(ak)->higher_dimension();
    }
  }

  void do_value(const RunTimeClassInfo* info) {
    InstanceKlass* ik = info->_klass;
    update(ik);
    update_array_klasses(ik->array_klasses());
  }
};

class ArchivedLambdaMirrorPatcher : public ArchivedMirrorPatcher {
public:
  void do_value(const RunTimeLambdaProxyClassInfo* info) {
    InstanceKlass* ik = info->proxy_klass_head();
    while (ik != NULL) {
      update(ik);
      Klass* k = ik->next_link();
      ik = (k != NULL) ? InstanceKlass::cast(k) : NULL;
    }
  }
};

void SystemDictionaryShared::update_archived_mirror_native_pointers_for(RunTimeSharedDictionary* dict) {
  ArchivedMirrorPatcher patcher;
  dict->iterate(&patcher);
}

void SystemDictionaryShared::update_archived_mirror_native_pointers_for(LambdaProxyClassDictionary* dict) {
  ArchivedLambdaMirrorPatcher patcher;
  dict->iterate(&patcher);
}

void SystemDictionaryShared::update_archived_mirror_native_pointers() {
  if (!HeapShared::open_regions_mapped()) {
    return;
  }
  if (MetaspaceShared::relocation_delta() == 0) {
    return;
  }
  update_archived_mirror_native_pointers_for(&_builtin_dictionary);
  update_archived_mirror_native_pointers_for(&_unregistered_dictionary);
  update_archived_mirror_native_pointers_for(&_lambda_proxy_class_dictionary);

  for (int t = T_BOOLEAN; t <= T_LONG; t++) {
    Klass* k = Universe::typeArrayKlassObj((BasicType)t);
    ArchivedMirrorPatcher::update_array_klasses(k);
  }
}
#endif
