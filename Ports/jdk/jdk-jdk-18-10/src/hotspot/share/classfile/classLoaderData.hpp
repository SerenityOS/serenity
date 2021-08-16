/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_CLASSLOADERDATA_HPP
#define SHARE_CLASSFILE_CLASSLOADERDATA_HPP

#include "memory/allocation.hpp"
#include "oops/oopHandle.hpp"
#include "oops/weakHandle.hpp"
#include "runtime/atomic.hpp"
#include "runtime/mutex.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/macros.hpp"
#if INCLUDE_JFR
#include "jfr/support/jfrTraceIdExtension.hpp"
#endif

// external name (synthetic) for the primordial "bootstrap" class loader instance
#define BOOTSTRAP_LOADER_NAME "bootstrap"
#define BOOTSTRAP_LOADER_NAME_LEN 9

//
// A class loader represents a linkset. Conceptually, a linkset identifies
// the complete transitive closure of resolved links that a dynamic linker can
// produce.
//
// A ClassLoaderData also encapsulates the allocation space, called a metaspace,
// used by the dynamic linker to allocate the runtime representation of all
// the types it defines.
//
// ClassLoaderData are stored in the runtime representation of classes,
// and provides iterators for root tracing and other GC operations.

class ClassLoaderDataGraph;
class JNIMethodBlock;
class ModuleEntry;
class PackageEntry;
class ModuleEntryTable;
class PackageEntryTable;
class DictionaryEntry;
class Dictionary;
class ClassLoaderMetaspace;

// ClassLoaderData class

class ClassLoaderData : public CHeapObj<mtClass> {
  friend class VMStructs;

 private:
  class ChunkedHandleList {
    struct Chunk : public CHeapObj<mtClass> {
      static const size_t CAPACITY = 32;

      oop _data[CAPACITY];
      volatile juint _size;
      Chunk* _next;

      Chunk(Chunk* c) : _size(0), _next(c) { }
    };

    Chunk* volatile _head;

    void oops_do_chunk(OopClosure* f, Chunk* c, const juint size);

   public:
    ChunkedHandleList() : _head(NULL) {}
    ~ChunkedHandleList();

    // Only one thread at a time can add, guarded by ClassLoaderData::metaspace_lock().
    // However, multiple threads can execute oops_do concurrently with add.
    OopHandle add(oop o);
    bool contains(oop p);
    NOT_PRODUCT(bool owner_of(oop* p);)
    void oops_do(OopClosure* f);

    int count() const;
  };

  friend class ClassLoaderDataGraph;
  friend class ClassLoaderDataGraphIterator;
  friend class ClassLoaderDataGraphKlassIteratorAtomic;
  friend class ClassLoaderDataGraphKlassIteratorStatic;
  friend class ClassLoaderDataGraphMetaspaceIterator;
  friend class Klass;
  friend class MetaDataFactory;
  friend class Method;

  static ClassLoaderData * _the_null_class_loader_data;

  WeakHandle _holder;       // The oop that determines lifetime of this class loader
  OopHandle  _class_loader; // The instance of java/lang/ClassLoader associated with
                            // this ClassLoaderData

  ClassLoaderMetaspace * volatile _metaspace;  // Meta-space where meta-data defined by the
                                    // classes in the class loader are allocated.
  Mutex* _metaspace_lock;  // Locks the metaspace for allocations and setup.
  bool _unloading;         // true if this class loader goes away
  bool _has_class_mirror_holder; // If true, CLD is dedicated to one class and that class determines
                                 // the CLDs lifecycle.  For example, a non-strong hidden class.
                                 // Arrays of these classes are also assigned
                                 // to these class loader datas.

  // Remembered sets support for the oops in the class loader data.
  bool _modified_oops;     // Card Table Equivalent

  int _keep_alive;         // if this CLD is kept alive.
                           // Used for non-strong hidden classes and the
                           // boot class loader. _keep_alive does not need to be volatile or
                           // atomic since there is one unique CLD per non-strong hidden class.

  volatile int _claim; // non-zero if claimed, for example during GC traces.
                       // To avoid applying oop closure more than once.
  ChunkedHandleList _handles; // Handles to constant pool arrays, Modules, etc, which
                              // have the same life cycle of the corresponding ClassLoader.

  NOT_PRODUCT(volatile int _dependency_count;)  // number of class loader dependencies

  Klass* volatile _klasses;              // The classes defined by the class loader.
  PackageEntryTable* volatile _packages; // The packages defined by the class loader.
  ModuleEntryTable*  volatile _modules;  // The modules defined by the class loader.
  ModuleEntry* _unnamed_module;          // This class loader's unnamed module.
  Dictionary*  _dictionary;              // The loaded InstanceKlasses, including initiated by this class loader

  // These method IDs are created for the class loader and set to NULL when the
  // class loader is unloaded.  They are rarely freed, only for redefine classes
  // and if they lose a data race in InstanceKlass.
  JNIMethodBlock*                  _jmethod_ids;

  // Metadata to be deallocated when it's safe at class unloading, when
  // this class loader isn't unloaded itself.
  GrowableArray<Metadata*>*      _deallocate_list;

  // Support for walking class loader data objects
  ClassLoaderData* _next; /// Next loader_datas created

  Klass*  _class_loader_klass;
  Symbol* _name;
  Symbol* _name_and_id;
  JFR_ONLY(DEFINE_TRACE_ID_FIELD;)

  void set_next(ClassLoaderData* next) { _next = next; }
  ClassLoaderData* next() const        { return Atomic::load(&_next); }

  ClassLoaderData(Handle h_class_loader, bool has_class_mirror_holder);
  ~ClassLoaderData();

  // The CLD are not placed in the Heap, so the Card Table or
  // the Mod Union Table can't be used to mark when CLD have modified oops.
  // The CT and MUT bits saves this information for the whole class loader data.
  void clear_modified_oops()             { _modified_oops = false; }
 public:
  void record_modified_oops()            { _modified_oops = true; }
  bool has_modified_oops()               { return _modified_oops; }

  oop holder_no_keepalive() const;
  oop holder_phantom() const;

 private:
  void unload();
  bool keep_alive() const       { return _keep_alive > 0; }

  void classes_do(void f(Klass* const));
  void loaded_classes_do(KlassClosure* klass_closure);
  void classes_do(void f(InstanceKlass*));
  void methods_do(void f(Method*));
  void modules_do(void f(ModuleEntry*));
  void packages_do(void f(PackageEntry*));

  // Deallocate free list during class unloading.
  void free_deallocate_list();                      // for the classes that are not unloaded
  void free_deallocate_list_C_heap_structures();    // for the classes that are unloaded

  Dictionary* create_dictionary();

  void initialize_name(Handle class_loader);

 public:
  // GC interface.

  // The "claim" is typically used to check if oops_do needs to be applied on
  // the CLD or not. Most GCs only perform strong marking during the marking phase.
  enum Claim {
    _claim_none         = 0,
    _claim_finalizable  = 2,
    _claim_strong       = 3,
    _claim_other        = 4
  };
  void clear_claim() { _claim = 0; }
  void clear_claim(int claim);
  bool claimed() const { return _claim != 0; }
  bool claimed(int claim) const { return (_claim & claim) == claim; }
  bool try_claim(int claim);

  // Computes if the CLD is alive or not. This is safe to call in concurrent
  // contexts.
  bool is_alive() const;

  // Accessors
  ClassLoaderMetaspace* metaspace_or_null() const { return _metaspace; }

  static ClassLoaderData* the_null_class_loader_data() {
    return _the_null_class_loader_data;
  }

  Mutex* metaspace_lock() const { return _metaspace_lock; }

  bool has_class_mirror_holder() const { return _has_class_mirror_holder; }

  static void init_null_class_loader_data();

  bool is_the_null_class_loader_data() const {
    return this == _the_null_class_loader_data;
  }

  // Returns true if this class loader data is for the system class loader.
  // (Note that the class loader data may be for a non-strong hidden class)
  bool is_system_class_loader_data() const;

  // Returns true if this class loader data is for the platform class loader.
  // (Note that the class loader data may be for a non-strong hidden class)
  bool is_platform_class_loader_data() const;

  // Returns true if this class loader data is for the boot class loader.
  // (Note that the class loader data may be for a non-strong hidden class)
  inline bool is_boot_class_loader_data() const;

  bool is_builtin_class_loader_data() const;
  bool is_permanent_class_loader_data() const;

  // The Metaspace is created lazily so may be NULL.  This
  // method will allocate a Metaspace if needed.
  ClassLoaderMetaspace* metaspace_non_null();

  inline oop class_loader() const;

  // Returns true if this class loader data is for a loader going away.
  // Note that this is only safe after the GC has computed if the CLD is
  // unloading or not. In concurrent contexts where there are no such
  // guarantees, is_alive() should be used instead.
  bool is_unloading() const     {
    assert(!(is_the_null_class_loader_data() && _unloading), "The null class loader can never be unloaded");
    return _unloading;
  }

  // Used to refcount a non-strong hidden class's s CLD in order to indicate their aliveness.
  void inc_keep_alive();
  void dec_keep_alive();

  void initialize_holder(Handle holder);

  void oops_do(OopClosure* f, int claim_value, bool clear_modified_oops = false);

  void classes_do(KlassClosure* klass_closure);
  Klass* klasses() { return _klasses; }

  JNIMethodBlock* jmethod_ids() const              { return _jmethod_ids; }
  void set_jmethod_ids(JNIMethodBlock* new_block)  { _jmethod_ids = new_block; }

  void print() const;
  void print_on(outputStream* out) const PRODUCT_RETURN;
  void print_value() const;
  void print_value_on(outputStream* out) const;
  void verify();

  OopHandle add_handle(Handle h);
  void remove_handle(OopHandle h);
  void init_handle_locked(OopHandle& pd, Handle h);  // used for concurrent access to ModuleEntry::_pd field
  void add_class(Klass* k, bool publicize = true);
  void remove_class(Klass* k);
  bool contains_klass(Klass* k);
  void record_dependency(const Klass* to);
  PackageEntryTable* packages() { return _packages; }
  ModuleEntry* unnamed_module() { return _unnamed_module; }
  ModuleEntryTable* modules();
  bool modules_defined() { return (_modules != NULL); }

  // Offsets
  static ByteSize holder_offset()     { return in_ByteSize(offset_of(ClassLoaderData, _holder)); }
  static ByteSize keep_alive_offset() { return in_ByteSize(offset_of(ClassLoaderData, _keep_alive)); }

  // Loaded class dictionary
  Dictionary* dictionary() const { return _dictionary; }

  void add_to_deallocate_list(Metadata* m);

  static ClassLoaderData* class_loader_data(oop loader);
  static ClassLoaderData* class_loader_data_or_null(oop loader);

  // Returns Klass* of associated class loader, or NULL if associated loader is 'bootstrap'.
  // Also works if unloading.
  Klass* class_loader_klass() const { return _class_loader_klass; }

  // Returns the class loader's explict name as specified during
  // construction or the class loader's qualified class name.
  // Works during unloading.
  const char* loader_name() const;
  // Returns the explicitly specified class loader name or NULL.
  Symbol* name() const { return _name; }

  // Obtain the class loader's _name_and_id, works during unloading.
  const char* loader_name_and_id() const;
  Symbol* name_and_id() const { return _name_and_id; }

  unsigned identity_hash() const {
    return (unsigned)((uintptr_t)this >> LogBytesPerWord);
  }

  JFR_ONLY(DEFINE_TRACE_ID_METHODS;)
};

#endif // SHARE_CLASSFILE_CLASSLOADERDATA_HPP
