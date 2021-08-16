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

#ifndef SHARE_CLASSFILE_SYSTEMDICTIONARYSHARED_HPP
#define SHARE_CLASSFILE_SYSTEMDICTIONARYSHARED_HPP

#include "cds/filemap.hpp"
#include "cds/dumpTimeClassInfo.hpp"
#include "cds/lambdaProxyClassDictionary.hpp"
#include "cds/runTimeClassInfo.hpp"
#include "classfile/classLoaderData.hpp"
#include "classfile/packageEntry.hpp"
#include "classfile/systemDictionary.hpp"
#include "oops/klass.hpp"
#include "oops/oopHandle.hpp"


/*===============================================================================

    Handling of the classes in the AppCDS archive

    To ensure safety and to simplify the implementation, archived classes are
    "segregated" into 2 types. The following rules describe how they
    are stored and looked up.

[1] Category of archived classes

    There are 2 disjoint groups of classes stored in the AppCDS archive:

    BUILTIN:              These classes may be defined ONLY by the BOOT/PLATFORM/APP
                          loaders.

    UNREGISTERED:         These classes may be defined ONLY by a ClassLoader
                          instance that's not listed above (using fingerprint matching)

[2] How classes from different categories are specified in the classlist:

    Starting from JDK9, each class in the classlist may be specified with
    these keywords: "id", "super", "interfaces", "loader" and "source".


    BUILTIN               Only the "id" keyword may be (optionally) specified. All other
                          keywords are forbidden.

                          The named class is looked up from the jimage and from
                          Xbootclasspath/a and CLASSPATH.

    UNREGISTERED:         The "id", "super", and "source" keywords must all be
                          specified.

                          The "interfaces" keyword must be specified if the class implements
                          one or more local interfaces. The "interfaces" keyword must not be
                          specified if the class does not implement local interfaces.

                          The named class is looked up from the location specified in the
                          "source" keyword.

    Example classlist:

    # BUILTIN
    java/lang/Object id: 0
    java/lang/Cloneable id: 1
    java/lang/String

    # UNREGISTERED
    Bar id: 3 super: 0 interfaces: 1 source: /foo.jar


[3] Identifying the category of archived classes

    BUILTIN:              (C->shared_classpath_index() >= 0)
    UNREGISTERED:         (C->shared_classpath_index() == UNREGISTERED_INDEX (-9999))

[4] Lookup of archived classes at run time:

    (a) BUILTIN loaders:

        search _builtin_dictionary

    (b) UNREGISTERED loaders:

        search _unregistered_dictionary for an entry that matches the
        (name, clsfile_len, clsfile_crc32).

===============================================================================*/
#define UNREGISTERED_INDEX -9999

class BootstrapInfo;
class ClassFileStream;
class Dictionary;
class DumpTimeClassInfo;
class DumpTimeSharedClassTable;
class LambdaProxyClassDictionary;
class RunTimeClassInfo;
class RunTimeSharedDictionary;
class DumpTimeLambdaProxyClassDictionary;
class LambdaProxyClassKey;

class SharedClassLoadingMark {
 private:
  Thread* THREAD;
  InstanceKlass* _klass;
 public:
  SharedClassLoadingMark(Thread* current, InstanceKlass* ik) : THREAD(current), _klass(ik) {}
  ~SharedClassLoadingMark() {
    assert(THREAD != NULL, "Current thread is NULL");
    assert(_klass != NULL, "InstanceKlass is NULL");
    if (HAS_PENDING_EXCEPTION) {
      if (_klass->is_shared()) {
        _klass->set_shared_loading_failed();
      }
    }
  }
};

class SystemDictionaryShared: public SystemDictionary {
  friend class ExcludeDumpTimeSharedClasses;
public:
  enum {
    FROM_FIELD_IS_PROTECTED = 1 << 0,
    FROM_IS_ARRAY           = 1 << 1,
    FROM_IS_OBJECT          = 1 << 2
  };

private:

  static DumpTimeSharedClassTable* _dumptime_table;
  static DumpTimeSharedClassTable* _cloned_dumptime_table;
  static DumpTimeLambdaProxyClassDictionary* _dumptime_lambda_proxy_class_dictionary;
  static DumpTimeLambdaProxyClassDictionary* _cloned_dumptime_lambda_proxy_class_dictionary;
  // SystemDictionaries in the base layer static archive
  static RunTimeSharedDictionary _builtin_dictionary;
  static RunTimeSharedDictionary _unregistered_dictionary;
  static LambdaProxyClassDictionary _lambda_proxy_class_dictionary;
  // SystemDictionaries in the top layer dynamic archive
  static RunTimeSharedDictionary _dynamic_builtin_dictionary;
  static RunTimeSharedDictionary _dynamic_unregistered_dictionary;
  static LambdaProxyClassDictionary _dynamic_lambda_proxy_class_dictionary;

  static InstanceKlass* load_shared_class_for_builtin_loader(
                                               Symbol* class_name,
                                               Handle class_loader,
                                               TRAPS);
  static InstanceKlass* acquire_class_for_current_thread(
                                 InstanceKlass *ik,
                                 Handle class_loader,
                                 Handle protection_domain,
                                 const ClassFileStream* cfs,
                                 TRAPS);
  static DumpTimeClassInfo* find_or_allocate_info_for(InstanceKlass* k);
  static DumpTimeClassInfo* find_or_allocate_info_for_locked(InstanceKlass* k);
  static void write_dictionary(RunTimeSharedDictionary* dictionary,
                               bool is_builtin);
  static void write_lambda_proxy_class_dictionary(LambdaProxyClassDictionary* dictionary);
  static bool is_jfr_event_class(InstanceKlass *k);
  static bool is_registered_lambda_proxy_class(InstanceKlass* ik);
  static bool check_for_exclusion_impl(InstanceKlass* k);
  static void remove_dumptime_info(InstanceKlass* k) NOT_CDS_RETURN;
  static bool has_been_redefined(InstanceKlass* k);

  static bool _dump_in_progress;
  DEBUG_ONLY(static bool _no_class_loading_should_happen;)
  static void print_on(const char* prefix,
                       RunTimeSharedDictionary* builtin_dictionary,
                       RunTimeSharedDictionary* unregistered_dictionary,
                       LambdaProxyClassDictionary* lambda_dictionary,
                       outputStream* st) NOT_CDS_RETURN;

public:
  static bool is_hidden_lambda_proxy(InstanceKlass* ik);
  static bool is_early_klass(InstanceKlass* k);   // Was k loaded while JvmtiExport::is_early_phase()==true
  static InstanceKlass* find_builtin_class(Symbol* class_name);

  static const RunTimeClassInfo* find_record(RunTimeSharedDictionary* static_dict,
                                                   RunTimeSharedDictionary* dynamic_dict,
                                                   Symbol* name);

  static bool has_platform_or_app_classes();

  // Called by PLATFORM/APP loader only
  static InstanceKlass* find_or_load_shared_class(Symbol* class_name,
                                               Handle class_loader,
                                               TRAPS);


  static void allocate_shared_data_arrays(int size, TRAPS);

  static bool is_builtin_loader(ClassLoaderData* loader_data);

  static bool add_unregistered_class_for_static_archive(Thread* current, InstanceKlass* k);
  static InstanceKlass* lookup_super_for_unregistered_class(Symbol* class_name,
                                                            Symbol* super_name,  bool is_superclass);

  static void init_dumptime_info(InstanceKlass* k) NOT_CDS_RETURN;
  static void handle_class_unloading(InstanceKlass* k) NOT_CDS_RETURN;

  static Dictionary* boot_loader_dictionary() {
    return ClassLoaderData::the_null_class_loader_data()->dictionary();
  }

  static void update_shared_entry(InstanceKlass* klass, int id);
  static void set_shared_class_misc_info(InstanceKlass* k, ClassFileStream* cfs);

  static InstanceKlass* lookup_from_stream(Symbol* class_name,
                                           Handle class_loader,
                                           Handle protection_domain,
                                           const ClassFileStream* st,
                                           TRAPS);
  // "verification_constraints" are a set of checks performed by
  // VerificationType::is_reference_assignable_from when verifying a shared class during
  // dump time.
  //
  // With AppCDS, it is possible to override archived classes by calling
  // ClassLoader.defineClass() directly. SystemDictionary::load_shared_class() already
  // ensures that you cannot load a shared class if its super type(s) are changed. However,
  // we need an additional check to ensure that the verification_constraints did not change
  // between dump time and runtime.
  static bool add_verification_constraint(InstanceKlass* k, Symbol* name,
                  Symbol* from_name, bool from_field_is_protected,
                  bool from_is_array, bool from_is_object) NOT_CDS_RETURN_(false);
  static void check_verification_constraints(InstanceKlass* klass,
                                             TRAPS) NOT_CDS_RETURN;
  static void set_class_has_failed_verification(InstanceKlass* ik) NOT_CDS_RETURN;
  static bool has_class_failed_verification(InstanceKlass* ik) NOT_CDS_RETURN_(false);
  static void add_lambda_proxy_class(InstanceKlass* caller_ik,
                                     InstanceKlass* lambda_ik,
                                     Symbol* invoked_name,
                                     Symbol* invoked_type,
                                     Symbol* method_type,
                                     Method* member_method,
                                     Symbol* instantiated_method_type, TRAPS) NOT_CDS_RETURN;
  static void add_to_dump_time_lambda_proxy_class_dictionary(LambdaProxyClassKey& key,
                                                             InstanceKlass* proxy_klass) NOT_CDS_RETURN;
  static InstanceKlass* get_shared_lambda_proxy_class(InstanceKlass* caller_ik,
                                                      Symbol* invoked_name,
                                                      Symbol* invoked_type,
                                                      Symbol* method_type,
                                                      Method* member_method,
                                                      Symbol* instantiated_method_type) NOT_CDS_RETURN_(NULL);
  static InstanceKlass* get_shared_nest_host(InstanceKlass* lambda_ik) NOT_CDS_RETURN_(NULL);
  static InstanceKlass* prepare_shared_lambda_proxy_class(InstanceKlass* lambda_ik,
                                                          InstanceKlass* caller_ik, TRAPS) NOT_CDS_RETURN_(NULL);
  static bool check_linking_constraints(Thread* current, InstanceKlass* klass) NOT_CDS_RETURN_(false);
  static void record_linking_constraint(Symbol* name, InstanceKlass* klass,
                                     Handle loader1, Handle loader2) NOT_CDS_RETURN;
  static bool is_builtin(InstanceKlass* k) {
    return (k->shared_classpath_index() != UNREGISTERED_INDEX);
  }
  static bool add_unregistered_class(Thread* current, InstanceKlass* k);

  // For repeatable dumping, we
  //   1. clone DumpTimeSharedClassTable, same for DumpTimeLambdaProxyClassDictionary
  //      clone SharedClassPathTable
  //   2. do dumping
  //   3. restore DumpTimeSharedClassTable, DumpTimeLambdaProxyClassDictionary and SharedClassPathTable
  //      from cloned versions.
  static void clone_dumptime_tables();
  static void restore_dumptime_tables();

  static void check_excluded_classes();
  static bool check_for_exclusion(InstanceKlass* k, DumpTimeClassInfo* info);
  static void validate_before_archiving(InstanceKlass* k);
  static bool is_excluded_class(InstanceKlass* k);
  static void set_excluded(InstanceKlass* k);
  static void set_excluded_locked(InstanceKlass* k);
  static bool warn_excluded(InstanceKlass* k, const char* reason);
  static void dumptime_classes_do(class MetaspaceClosure* it);
  static size_t estimate_size_for_archive();
  static void write_to_archive(bool is_static_archive = true);
  static void adjust_lambda_proxy_class_dictionary();
  static void serialize_dictionary_headers(class SerializeClosure* soc,
                                           bool is_static_archive = true);
  static void serialize_vm_classes(class SerializeClosure* soc);
  static void print() { return print_on(tty); }
  static void print_on(outputStream* st) NOT_CDS_RETURN;
  static void print_shared_archive(outputStream* st, bool is_static = true) NOT_CDS_RETURN;
  static void print_table_statistics(outputStream* st) NOT_CDS_RETURN;
  static bool is_dumptime_table_empty() NOT_CDS_RETURN_(true);
  static void start_dumping() NOT_CDS_RETURN;
  static bool is_supported_invokedynamic(BootstrapInfo* bsi) NOT_CDS_RETURN_(false);
  DEBUG_ONLY(static bool no_class_loading_should_happen() {return _no_class_loading_should_happen;})

#ifdef ASSERT
  class NoClassLoadingMark: public StackObj {
  public:
    NoClassLoadingMark() {
      assert(!_no_class_loading_should_happen, "must not be nested");
      _no_class_loading_should_happen = true;
    }
    ~NoClassLoadingMark() {
      _no_class_loading_should_happen = false;
    }
  };
#endif

  template <typename T>
  static unsigned int hash_for_shared_dictionary_quick(T* ptr) {
    assert(MetaspaceObj::is_shared((const MetaspaceObj*)ptr), "must be");
    assert(ptr > (T*)SharedBaseAddress, "must be");
    uintx offset = uintx(ptr) - uintx(SharedBaseAddress);
    return primitive_hash<uintx>(offset);
  }

  static unsigned int hash_for_shared_dictionary(address ptr);

#if INCLUDE_CDS_JAVA_HEAP
private:
  static void update_archived_mirror_native_pointers_for(RunTimeSharedDictionary* dict);
  static void update_archived_mirror_native_pointers_for(LambdaProxyClassDictionary* dict);
public:
  static void update_archived_mirror_native_pointers() NOT_CDS_RETURN;
#endif
};

#endif // SHARE_CLASSFILE_SYSTEMDICTIONARYSHARED_HPP
