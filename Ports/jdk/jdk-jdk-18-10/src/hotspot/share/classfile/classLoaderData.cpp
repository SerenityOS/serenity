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

// A ClassLoaderData identifies the full set of class types that a class
// loader's name resolution strategy produces for a given configuration of the
// class loader.
// Class types in the ClassLoaderData may be defined by from class file binaries
// provided by the class loader, or from other class loader it interacts with
// according to its name resolution strategy.
//
// Class loaders that implement a deterministic name resolution strategy
// (including with respect to their delegation behavior), such as the boot, the
// platform, and the system loaders of the JDK's built-in class loader
// hierarchy, always produce the same linkset for a given configuration.
//
// ClassLoaderData carries information related to a linkset (e.g.,
// metaspace holding its klass definitions).
// The System Dictionary and related data structures (e.g., placeholder table,
// loader constraints table) as well as the runtime representation of classes
// only reference ClassLoaderData.
//
// Instances of java.lang.ClassLoader holds a pointer to a ClassLoaderData that
// that represent the loader's "linking domain" in the JVM.
//
// The bootstrap loader (represented by NULL) also has a ClassLoaderData,
// the singleton class the_null_class_loader_data().

#include "precompiled.hpp"
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/classLoaderDataGraph.inline.hpp"
#include "classfile/dictionary.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/moduleEntry.hpp"
#include "classfile/packageEntry.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/classLoaderMetaspace.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/metaspace.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/access.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "oops/weakHandle.inline.hpp"
#include "runtime/arguments.hpp"
#include "runtime/atomic.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/mutex.hpp"
#include "runtime/safepoint.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/macros.hpp"
#include "utilities/ostream.hpp"

ClassLoaderData * ClassLoaderData::_the_null_class_loader_data = NULL;

void ClassLoaderData::init_null_class_loader_data() {
  assert(_the_null_class_loader_data == NULL, "cannot initialize twice");
  assert(ClassLoaderDataGraph::_head == NULL, "cannot initialize twice");

  _the_null_class_loader_data = new ClassLoaderData(Handle(), false);
  ClassLoaderDataGraph::_head = _the_null_class_loader_data;
  assert(_the_null_class_loader_data->is_the_null_class_loader_data(), "Must be");

  LogTarget(Trace, class, loader, data) lt;
  if (lt.is_enabled()) {
    ResourceMark rm;
    LogStream ls(lt);
    ls.print("create ");
    _the_null_class_loader_data->print_value_on(&ls);
    ls.cr();
  }
}

// Obtain and set the class loader's name within the ClassLoaderData so
// it will be available for error messages, logging, JFR, etc.  The name
// and klass are available after the class_loader oop is no longer alive,
// during unloading.
void ClassLoaderData::initialize_name(Handle class_loader) {
  ResourceMark rm;

  // Obtain the class loader's name.  If the class loader's name was not
  // explicitly set during construction, the CLD's _name field will be null.
  oop cl_name = java_lang_ClassLoader::name(class_loader());
  if (cl_name != NULL) {
    const char* cl_instance_name = java_lang_String::as_utf8_string(cl_name);

    if (cl_instance_name != NULL && cl_instance_name[0] != '\0') {
      _name = SymbolTable::new_symbol(cl_instance_name);
    }
  }

  // Obtain the class loader's name and identity hash.  If the class loader's
  // name was not explicitly set during construction, the class loader's name and id
  // will be set to the qualified class name of the class loader along with its
  // identity hash.
  // If for some reason the ClassLoader's constructor has not been run, instead of
  // leaving the _name_and_id field null, fall back to the external qualified class
  // name.  Thus CLD's _name_and_id field should never have a null value.
  oop cl_name_and_id = java_lang_ClassLoader::nameAndId(class_loader());
  const char* cl_instance_name_and_id =
                  (cl_name_and_id == NULL) ? _class_loader_klass->external_name() :
                                             java_lang_String::as_utf8_string(cl_name_and_id);
  assert(cl_instance_name_and_id != NULL && cl_instance_name_and_id[0] != '\0', "class loader has no name and id");
  _name_and_id = SymbolTable::new_symbol(cl_instance_name_and_id);
}

ClassLoaderData::ClassLoaderData(Handle h_class_loader, bool has_class_mirror_holder) :
  _metaspace(NULL),
  _metaspace_lock(new Mutex(Mutex::leaf+1, "Metaspace allocation lock", true,
                            Mutex::_safepoint_check_never)),
  _unloading(false), _has_class_mirror_holder(has_class_mirror_holder),
  _modified_oops(true),
  // A non-strong hidden class loader data doesn't have anything to keep
  // it from being unloaded during parsing of the non-strong hidden class.
  // The null-class-loader should always be kept alive.
  _keep_alive((has_class_mirror_holder || h_class_loader.is_null()) ? 1 : 0),
  _claim(0),
  _handles(),
  _klasses(NULL), _packages(NULL), _modules(NULL), _unnamed_module(NULL), _dictionary(NULL),
  _jmethod_ids(NULL),
  _deallocate_list(NULL),
  _next(NULL),
  _class_loader_klass(NULL), _name(NULL), _name_and_id(NULL) {

  if (!h_class_loader.is_null()) {
    _class_loader = _handles.add(h_class_loader());
    _class_loader_klass = h_class_loader->klass();
    initialize_name(h_class_loader);
  }

  if (!has_class_mirror_holder) {
    // The holder is initialized later for non-strong hidden classes,
    // and before calling anything that call class_loader().
    initialize_holder(h_class_loader);

    // A ClassLoaderData created solely for a non-strong hidden class should never
    // have a ModuleEntryTable or PackageEntryTable created for it.
    _packages = new PackageEntryTable(PackageEntryTable::_packagetable_entry_size);
    if (h_class_loader.is_null()) {
      // Create unnamed module for boot loader
      _unnamed_module = ModuleEntry::create_boot_unnamed_module(this);
    } else {
      // Create unnamed module for all other loaders
      _unnamed_module = ModuleEntry::create_unnamed_module(this);
    }
    _dictionary = create_dictionary();
  }

  NOT_PRODUCT(_dependency_count = 0); // number of class loader dependencies

  JFR_ONLY(INIT_ID(this);)
}

ClassLoaderData::ChunkedHandleList::~ChunkedHandleList() {
  Chunk* c = _head;
  while (c != NULL) {
    Chunk* next = c->_next;
    delete c;
    c = next;
  }
}

OopHandle ClassLoaderData::ChunkedHandleList::add(oop o) {
  if (_head == NULL || _head->_size == Chunk::CAPACITY) {
    Chunk* next = new Chunk(_head);
    Atomic::release_store(&_head, next);
  }
  oop* handle = &_head->_data[_head->_size];
  NativeAccess<IS_DEST_UNINITIALIZED>::oop_store(handle, o);
  Atomic::release_store(&_head->_size, _head->_size + 1);
  return OopHandle(handle);
}

int ClassLoaderData::ChunkedHandleList::count() const {
  int count = 0;
  Chunk* chunk = _head;
  while (chunk != NULL) {
    count += chunk->_size;
    chunk = chunk->_next;
  }
  return count;
}

inline void ClassLoaderData::ChunkedHandleList::oops_do_chunk(OopClosure* f, Chunk* c, const juint size) {
  for (juint i = 0; i < size; i++) {
    if (c->_data[i] != NULL) {
      f->do_oop(&c->_data[i]);
    }
  }
}

void ClassLoaderData::ChunkedHandleList::oops_do(OopClosure* f) {
  Chunk* head = Atomic::load_acquire(&_head);
  if (head != NULL) {
    // Must be careful when reading size of head
    oops_do_chunk(f, head, Atomic::load_acquire(&head->_size));
    for (Chunk* c = head->_next; c != NULL; c = c->_next) {
      oops_do_chunk(f, c, c->_size);
    }
  }
}

class VerifyContainsOopClosure : public OopClosure {
  oop  _target;
  bool _found;

 public:
  VerifyContainsOopClosure(oop target) : _target(target), _found(false) {}

  void do_oop(oop* p) {
    if (p != NULL && NativeAccess<AS_NO_KEEPALIVE>::oop_load(p) == _target) {
      _found = true;
    }
  }

  void do_oop(narrowOop* p) {
    // The ChunkedHandleList should not contain any narrowOop
    ShouldNotReachHere();
  }

  bool found() const {
    return _found;
  }
};

bool ClassLoaderData::ChunkedHandleList::contains(oop p) {
  VerifyContainsOopClosure cl(p);
  oops_do(&cl);
  return cl.found();
}

#ifndef PRODUCT
bool ClassLoaderData::ChunkedHandleList::owner_of(oop* oop_handle) {
  Chunk* chunk = _head;
  while (chunk != NULL) {
    if (&(chunk->_data[0]) <= oop_handle && oop_handle < &(chunk->_data[chunk->_size])) {
      return true;
    }
    chunk = chunk->_next;
  }
  return false;
}
#endif // PRODUCT

void ClassLoaderData::clear_claim(int claim) {
  for (;;) {
    int old_claim = Atomic::load(&_claim);
    if ((old_claim & claim) == 0) {
      return;
    }
    int new_claim = old_claim & ~claim;
    if (Atomic::cmpxchg(&_claim, old_claim, new_claim) == old_claim) {
      return;
    }
  }
}

bool ClassLoaderData::try_claim(int claim) {
  for (;;) {
    int old_claim = Atomic::load(&_claim);
    if ((old_claim & claim) == claim) {
      return false;
    }
    int new_claim = old_claim | claim;
    if (Atomic::cmpxchg(&_claim, old_claim, new_claim) == old_claim) {
      return true;
    }
  }
}

// Non-strong hidden classes have their own ClassLoaderData that is marked to keep alive
// while the class is being parsed, and if the class appears on the module fixup list.
// Due to the uniqueness that no other class shares the hidden class' name or
// ClassLoaderData, no other non-GC thread has knowledge of the hidden class while
// it is being defined, therefore _keep_alive is not volatile or atomic.
void ClassLoaderData::inc_keep_alive() {
  if (has_class_mirror_holder()) {
    if (!Arguments::is_dumping_archive()) {
      assert(_keep_alive > 0, "Invalid keep alive increment count");
    }
    _keep_alive++;
  }
}

void ClassLoaderData::dec_keep_alive() {
  if (has_class_mirror_holder()) {
    assert(_keep_alive > 0, "Invalid keep alive decrement count");
    _keep_alive--;
  }
}

void ClassLoaderData::oops_do(OopClosure* f, int claim_value, bool clear_mod_oops) {
  if (claim_value != ClassLoaderData::_claim_none && !try_claim(claim_value)) {
    return;
  }

  // Only clear modified_oops after the ClassLoaderData is claimed.
  if (clear_mod_oops) {
    clear_modified_oops();
  }

  _handles.oops_do(f);
}

void ClassLoaderData::classes_do(KlassClosure* klass_closure) {
  // Lock-free access requires load_acquire
  for (Klass* k = Atomic::load_acquire(&_klasses); k != NULL; k = k->next_link()) {
    klass_closure->do_klass(k);
    assert(k != k->next_link(), "no loops!");
  }
}

void ClassLoaderData::classes_do(void f(Klass * const)) {
  // Lock-free access requires load_acquire
  for (Klass* k = Atomic::load_acquire(&_klasses); k != NULL; k = k->next_link()) {
    f(k);
    assert(k != k->next_link(), "no loops!");
  }
}

void ClassLoaderData::methods_do(void f(Method*)) {
  // Lock-free access requires load_acquire
  for (Klass* k = Atomic::load_acquire(&_klasses); k != NULL; k = k->next_link()) {
    if (k->is_instance_klass() && InstanceKlass::cast(k)->is_loaded()) {
      InstanceKlass::cast(k)->methods_do(f);
    }
  }
}

void ClassLoaderData::loaded_classes_do(KlassClosure* klass_closure) {
  // Lock-free access requires load_acquire
  for (Klass* k = Atomic::load_acquire(&_klasses); k != NULL; k = k->next_link()) {
    // Do not filter ArrayKlass oops here...
    if (k->is_array_klass() || (k->is_instance_klass() && InstanceKlass::cast(k)->is_loaded())) {
#ifdef ASSERT
      oop m = k->java_mirror();
      assert(m != NULL, "NULL mirror");
      assert(m->is_a(vmClasses::Class_klass()), "invalid mirror");
#endif
      klass_closure->do_klass(k);
    }
  }
}

void ClassLoaderData::classes_do(void f(InstanceKlass*)) {
  // Lock-free access requires load_acquire
  for (Klass* k = Atomic::load_acquire(&_klasses); k != NULL; k = k->next_link()) {
    if (k->is_instance_klass()) {
      f(InstanceKlass::cast(k));
    }
    assert(k != k->next_link(), "no loops!");
  }
}

void ClassLoaderData::modules_do(void f(ModuleEntry*)) {
  assert_locked_or_safepoint(Module_lock);
  if (_unnamed_module != NULL) {
    f(_unnamed_module);
  }
  if (_modules != NULL) {
    for (int i = 0; i < _modules->table_size(); i++) {
      for (ModuleEntry* entry = _modules->bucket(i);
           entry != NULL;
           entry = entry->next()) {
        f(entry);
      }
    }
  }
}

void ClassLoaderData::packages_do(void f(PackageEntry*)) {
  assert_locked_or_safepoint(Module_lock);
  if (_packages != NULL) {
    for (int i = 0; i < _packages->table_size(); i++) {
      for (PackageEntry* entry = _packages->bucket(i);
           entry != NULL;
           entry = entry->next()) {
        f(entry);
      }
    }
  }
}

void ClassLoaderData::record_dependency(const Klass* k) {
  assert(k != NULL, "invariant");

  ClassLoaderData * const from_cld = this;
  ClassLoaderData * const to_cld = k->class_loader_data();

  // Do not need to record dependency if the dependency is to a class whose
  // class loader data is never freed.  (i.e. the dependency's class loader
  // is one of the three builtin class loaders and the dependency's class
  // loader data has a ClassLoader holder, not a Class holder.)
  if (to_cld->is_permanent_class_loader_data()) {
    return;
  }

  oop to;
  if (to_cld->has_class_mirror_holder()) {
    // Just return if a non-strong hidden class class is attempting to record a dependency
    // to itself.  (Note that every non-strong hidden class has its own unique class
    // loader data.)
    if (to_cld == from_cld) {
      return;
    }
    // Hidden class dependencies are through the mirror.
    to = k->java_mirror();
  } else {
    to = to_cld->class_loader();
    oop from = from_cld->class_loader();

    // Just return if this dependency is to a class with the same or a parent
    // class_loader.
    if (from == to || java_lang_ClassLoader::isAncestor(from, to)) {
      return; // this class loader is in the parent list, no need to add it.
    }
  }

  // It's a dependency we won't find through GC, add it.
  if (!_handles.contains(to)) {
    NOT_PRODUCT(Atomic::inc(&_dependency_count));
    LogTarget(Trace, class, loader, data) lt;
    if (lt.is_enabled()) {
      ResourceMark rm;
      LogStream ls(lt);
      ls.print("adding dependency from ");
      print_value_on(&ls);
      ls.print(" to ");
      to_cld->print_value_on(&ls);
      ls.cr();
    }
    Handle dependency(Thread::current(), to);
    add_handle(dependency);
    // Added a potentially young gen oop to the ClassLoaderData
    record_modified_oops();
  }
}

void ClassLoaderData::add_class(Klass* k, bool publicize /* true */) {
  {
    MutexLocker ml(metaspace_lock(), Mutex::_no_safepoint_check_flag);
    Klass* old_value = _klasses;
    k->set_next_link(old_value);
    // Link the new item into the list, making sure the linked class is stable
    // since the list can be walked without a lock
    Atomic::release_store(&_klasses, k);
    if (k->is_array_klass()) {
      ClassLoaderDataGraph::inc_array_classes(1);
    } else {
      ClassLoaderDataGraph::inc_instance_classes(1);
    }
  }

  if (publicize) {
    LogTarget(Trace, class, loader, data) lt;
    if (lt.is_enabled()) {
      ResourceMark rm;
      LogStream ls(lt);
      ls.print("Adding k: " PTR_FORMAT " %s to ", p2i(k), k->external_name());
      print_value_on(&ls);
      ls.cr();
    }
  }
}

void ClassLoaderData::initialize_holder(Handle loader_or_mirror) {
  if (loader_or_mirror() != NULL) {
    assert(_holder.is_null(), "never replace holders");
    _holder = WeakHandle(Universe::vm_weak(), loader_or_mirror);
  }
}

// Remove a klass from the _klasses list for scratch_class during redefinition
// or parsed class in the case of an error.
void ClassLoaderData::remove_class(Klass* scratch_class) {
  assert_locked_or_safepoint(ClassLoaderDataGraph_lock);

  // Adjust global class iterator.
  ClassLoaderDataGraph::adjust_saved_class(scratch_class);

  Klass* prev = NULL;
  for (Klass* k = _klasses; k != NULL; k = k->next_link()) {
    if (k == scratch_class) {
      if (prev == NULL) {
        _klasses = k->next_link();
      } else {
        Klass* next = k->next_link();
        prev->set_next_link(next);
      }

      if (k->is_array_klass()) {
        ClassLoaderDataGraph::dec_array_classes(1);
      } else {
        ClassLoaderDataGraph::dec_instance_classes(1);
      }

      return;
    }
    prev = k;
    assert(k != k->next_link(), "no loops!");
  }
  ShouldNotReachHere();   // should have found this class!!
}

void ClassLoaderData::unload() {
  _unloading = true;

  LogTarget(Trace, class, loader, data) lt;
  if (lt.is_enabled()) {
    ResourceMark rm;
    LogStream ls(lt);
    ls.print("unload");
    print_value_on(&ls);
    ls.cr();
  }

  // Some items on the _deallocate_list need to free their C heap structures
  // if they are not already on the _klasses list.
  free_deallocate_list_C_heap_structures();

  // Clean up class dependencies and tell serviceability tools
  // these classes are unloading.  Must be called
  // after erroneous classes are released.
  classes_do(InstanceKlass::unload_class);

  // Method::clear_jmethod_ids only sets the jmethod_ids to NULL without
  // releasing the memory for related JNIMethodBlocks and JNIMethodBlockNodes.
  // This is done intentionally because native code (e.g. JVMTI agent) holding
  // jmethod_ids may access them after the associated classes and class loader
  // are unloaded. The Java Native Interface Specification says "method ID
  // does not prevent the VM from unloading the class from which the ID has
  // been derived. After the class is unloaded, the method or field ID becomes
  // invalid". In real world usages, the native code may rely on jmethod_ids
  // being NULL after class unloading. Hence, it is unsafe to free the memory
  // from the VM side without knowing when native code is going to stop using
  // them.
  if (_jmethod_ids != NULL) {
    Method::clear_jmethod_ids(this);
  }

  // Clean up global class iterator for compiler
  ClassLoaderDataGraph::adjust_saved_class(this);
}

ModuleEntryTable* ClassLoaderData::modules() {
  // Lazily create the module entry table at first request.
  // Lock-free access requires load_acquire.
  ModuleEntryTable* modules = Atomic::load_acquire(&_modules);
  if (modules == NULL) {
    MutexLocker m1(Module_lock);
    // Check if _modules got allocated while we were waiting for this lock.
    if ((modules = _modules) == NULL) {
      modules = new ModuleEntryTable(ModuleEntryTable::_moduletable_entry_size);

      {
        MutexLocker m1(metaspace_lock(), Mutex::_no_safepoint_check_flag);
        // Ensure _modules is stable, since it is examined without a lock
        Atomic::release_store(&_modules, modules);
      }
    }
  }
  return modules;
}

const int _boot_loader_dictionary_size    = 1009;
const int _default_loader_dictionary_size = 107;

Dictionary* ClassLoaderData::create_dictionary() {
  assert(!has_class_mirror_holder(), "class mirror holder cld does not have a dictionary");
  int size;
  bool resizable = false;
  if (_the_null_class_loader_data == NULL) {
    size = _boot_loader_dictionary_size;
    resizable = true;
  } else if (class_loader()->is_a(vmClasses::reflect_DelegatingClassLoader_klass())) {
    size = 1;  // there's only one class in relection class loader and no initiated classes
  } else if (is_system_class_loader_data()) {
    size = _boot_loader_dictionary_size;
    resizable = true;
  } else {
    size = _default_loader_dictionary_size;
    resizable = true;
  }
  if (!DynamicallyResizeSystemDictionaries || DumpSharedSpaces) {
    resizable = false;
  }
  return new Dictionary(this, size, resizable);
}

// Tell the GC to keep this klass alive while iterating ClassLoaderDataGraph
oop ClassLoaderData::holder_phantom() const {
  // A klass that was previously considered dead can be looked up in the
  // CLD/SD, and its _java_mirror or _class_loader can be stored in a root
  // or a reachable object making it alive again. The SATB part of G1 needs
  // to get notified about this potential resurrection, otherwise the marking
  // might not find the object.
  if (!_holder.is_null()) {  // NULL class_loader
    return _holder.resolve();
  } else {
    return NULL;
  }
}

// Let the GC read the holder without keeping it alive.
oop ClassLoaderData::holder_no_keepalive() const {
  if (!_holder.is_null()) {  // NULL class_loader
    return _holder.peek();
  } else {
    return NULL;
  }
}

// Unloading support
bool ClassLoaderData::is_alive() const {
  bool alive = keep_alive()         // null class loader and incomplete non-strong hidden class.
      || (_holder.peek() != NULL);  // and not cleaned by the GC weak handle processing.

  return alive;
}

class ReleaseKlassClosure: public KlassClosure {
private:
  size_t  _instance_class_released;
  size_t  _array_class_released;
public:
  ReleaseKlassClosure() : _instance_class_released(0), _array_class_released(0) { }

  size_t instance_class_released() const { return _instance_class_released; }
  size_t array_class_released()    const { return _array_class_released;    }

  void do_klass(Klass* k) {
    if (k->is_array_klass()) {
      _array_class_released ++;
    } else {
      assert(k->is_instance_klass(), "Must be");
      _instance_class_released ++;
    }
    k->release_C_heap_structures();
  }
};

ClassLoaderData::~ClassLoaderData() {
  // Release C heap structures for all the classes.
  ReleaseKlassClosure cl;
  classes_do(&cl);

  ClassLoaderDataGraph::dec_array_classes(cl.array_class_released());
  ClassLoaderDataGraph::dec_instance_classes(cl.instance_class_released());

  // Release the WeakHandle
  _holder.release(Universe::vm_weak());

  // Release C heap allocated hashtable for all the packages.
  if (_packages != NULL) {
    // Destroy the table itself
    delete _packages;
    _packages = NULL;
  }

  // Release C heap allocated hashtable for all the modules.
  if (_modules != NULL) {
    // Destroy the table itself
    delete _modules;
    _modules = NULL;
  }

  // Release C heap allocated hashtable for the dictionary
  if (_dictionary != NULL) {
    // Destroy the table itself
    delete _dictionary;
    _dictionary = NULL;
  }

  if (_unnamed_module != NULL) {
    _unnamed_module->delete_unnamed_module();
    _unnamed_module = NULL;
  }

  // release the metaspace
  ClassLoaderMetaspace *m = _metaspace;
  if (m != NULL) {
    _metaspace = NULL;
    delete m;
  }

  // Delete lock
  delete _metaspace_lock;

  // Delete free list
  if (_deallocate_list != NULL) {
    delete _deallocate_list;
  }

  // Decrement refcounts of Symbols if created.
  if (_name != NULL) {
    _name->decrement_refcount();
  }
  if (_name_and_id != NULL) {
    _name_and_id->decrement_refcount();
  }
}

// Returns true if this class loader data is for the app class loader
// or a user defined system class loader.  (Note that the class loader
// data may have a Class holder.)
bool ClassLoaderData::is_system_class_loader_data() const {
  return SystemDictionary::is_system_class_loader(class_loader());
}

// Returns true if this class loader data is for the platform class loader.
// (Note that the class loader data may have a Class holder.)
bool ClassLoaderData::is_platform_class_loader_data() const {
  return SystemDictionary::is_platform_class_loader(class_loader());
}

// Returns true if the class loader for this class loader data is one of
// the 3 builtin (boot application/system or platform) class loaders,
// including a user-defined system class loader.  Note that if the class
// loader data is for a non-strong hidden class then it may
// get freed by a GC even if its class loader is one of these loaders.
bool ClassLoaderData::is_builtin_class_loader_data() const {
  return (is_boot_class_loader_data() ||
          SystemDictionary::is_system_class_loader(class_loader()) ||
          SystemDictionary::is_platform_class_loader(class_loader()));
}

// Returns true if this class loader data is a class loader data
// that is not ever freed by a GC.  It must be the CLD for one of the builtin
// class loaders and not the CLD for a non-strong hidden class.
bool ClassLoaderData::is_permanent_class_loader_data() const {
  return is_builtin_class_loader_data() && !has_class_mirror_holder();
}

ClassLoaderMetaspace* ClassLoaderData::metaspace_non_null() {
  // If the metaspace has not been allocated, create a new one.  Might want
  // to create smaller arena for Reflection class loaders also.
  // The reason for the delayed allocation is because some class loaders are
  // simply for delegating with no metadata of their own.
  // Lock-free access requires load_acquire.
  ClassLoaderMetaspace* metaspace = Atomic::load_acquire(&_metaspace);
  if (metaspace == NULL) {
    MutexLocker ml(_metaspace_lock,  Mutex::_no_safepoint_check_flag);
    // Check if _metaspace got allocated while we were waiting for this lock.
    if ((metaspace = _metaspace) == NULL) {
      if (this == the_null_class_loader_data()) {
        assert (class_loader() == NULL, "Must be");
        metaspace = new ClassLoaderMetaspace(_metaspace_lock, Metaspace::BootMetaspaceType);
      } else if (has_class_mirror_holder()) {
        metaspace = new ClassLoaderMetaspace(_metaspace_lock, Metaspace::ClassMirrorHolderMetaspaceType);
      } else if (class_loader()->is_a(vmClasses::reflect_DelegatingClassLoader_klass())) {
        metaspace = new ClassLoaderMetaspace(_metaspace_lock, Metaspace::ReflectionMetaspaceType);
      } else {
        metaspace = new ClassLoaderMetaspace(_metaspace_lock, Metaspace::StandardMetaspaceType);
      }
      // Ensure _metaspace is stable, since it is examined without a lock
      Atomic::release_store(&_metaspace, metaspace);
    }
  }
  return metaspace;
}

OopHandle ClassLoaderData::add_handle(Handle h) {
  MutexLocker ml(metaspace_lock(),  Mutex::_no_safepoint_check_flag);
  record_modified_oops();
  return _handles.add(h());
}

void ClassLoaderData::remove_handle(OopHandle h) {
  assert(!is_unloading(), "Do not remove a handle for a CLD that is unloading");
  oop* ptr = h.ptr_raw();
  if (ptr != NULL) {
    assert(_handles.owner_of(ptr), "Got unexpected handle " PTR_FORMAT, p2i(ptr));
    NativeAccess<>::oop_store(ptr, oop(NULL));
  }
}

void ClassLoaderData::init_handle_locked(OopHandle& dest, Handle h) {
  MutexLocker ml(metaspace_lock(),  Mutex::_no_safepoint_check_flag);
  if (dest.resolve() != NULL) {
    return;
  } else {
    record_modified_oops();
    dest = _handles.add(h());
  }
}

// Add this metadata pointer to be freed when it's safe.  This is only during
// a safepoint which checks if handles point to this metadata field.
void ClassLoaderData::add_to_deallocate_list(Metadata* m) {
  // Metadata in shared region isn't deleted.
  if (!m->is_shared()) {
    MutexLocker ml(metaspace_lock(),  Mutex::_no_safepoint_check_flag);
    if (_deallocate_list == NULL) {
      _deallocate_list = new (ResourceObj::C_HEAP, mtClass) GrowableArray<Metadata*>(100, mtClass);
    }
    _deallocate_list->append_if_missing(m);
    log_debug(class, loader, data)("deallocate added for %s", m->print_value_string());
    ClassLoaderDataGraph::set_should_clean_deallocate_lists();
  }
}

// Deallocate free metadata on the free list.  How useful the PermGen was!
void ClassLoaderData::free_deallocate_list() {
  // This must be called at a safepoint because it depends on metadata walking at
  // safepoint cleanup time.
  assert(SafepointSynchronize::is_at_safepoint(), "only called at safepoint");
  assert(!is_unloading(), "only called for ClassLoaderData that are not unloading");
  if (_deallocate_list == NULL) {
    return;
  }
  // Go backwards because this removes entries that are freed.
  for (int i = _deallocate_list->length() - 1; i >= 0; i--) {
    Metadata* m = _deallocate_list->at(i);
    if (!m->on_stack()) {
      _deallocate_list->remove_at(i);
      // There are only three types of metadata that we deallocate directly.
      // Cast them so they can be used by the template function.
      if (m->is_method()) {
        MetadataFactory::free_metadata(this, (Method*)m);
      } else if (m->is_constantPool()) {
        MetadataFactory::free_metadata(this, (ConstantPool*)m);
      } else if (m->is_klass()) {
        MetadataFactory::free_metadata(this, (InstanceKlass*)m);
      } else {
        ShouldNotReachHere();
      }
    } else {
      // Metadata is alive.
      // If scratch_class is on stack then it shouldn't be on this list!
      assert(!m->is_klass() || !((InstanceKlass*)m)->is_scratch_class(),
             "scratch classes on this list should be dead");
      // Also should assert that other metadata on the list was found in handles.
      // Some cleaning remains.
      ClassLoaderDataGraph::set_should_clean_deallocate_lists();
    }
  }
}

// This is distinct from free_deallocate_list.  For class loader data that are
// unloading, this frees the C heap memory for items on the list, and unlinks
// scratch or error classes so that unloading events aren't triggered for these
// classes. The metadata is removed with the unloading metaspace.
// There isn't C heap memory allocated for methods, so nothing is done for them.
void ClassLoaderData::free_deallocate_list_C_heap_structures() {
  assert_locked_or_safepoint(ClassLoaderDataGraph_lock);
  assert(is_unloading(), "only called for ClassLoaderData that are unloading");
  if (_deallocate_list == NULL) {
    return;
  }
  // Go backwards because this removes entries that are freed.
  for (int i = _deallocate_list->length() - 1; i >= 0; i--) {
    Metadata* m = _deallocate_list->at(i);
    _deallocate_list->remove_at(i);
    if (m->is_constantPool()) {
      ((ConstantPool*)m)->release_C_heap_structures();
    } else if (m->is_klass()) {
      InstanceKlass* ik = (InstanceKlass*)m;
      // also releases ik->constants() C heap memory
      ik->release_C_heap_structures();
      // Remove the class so unloading events aren't triggered for
      // this class (scratch or error class) in do_unloading().
      remove_class(ik);
    }
  }
}

// Caller needs ResourceMark
// If the class loader's _name has not been explicitly set, the class loader's
// qualified class name is returned.
const char* ClassLoaderData::loader_name() const {
   if (_class_loader_klass == NULL) {
     return BOOTSTRAP_LOADER_NAME;
   } else if (_name != NULL) {
     return _name->as_C_string();
   } else {
     return _class_loader_klass->external_name();
   }
}

// Caller needs ResourceMark
// Format of the _name_and_id is as follows:
//   If the defining loader has a name explicitly set then '<loader-name>' @<id>
//   If the defining loader has no name then <qualified-class-name> @<id>
//   If built-in loader, then omit '@<id>' as there is only one instance.
const char* ClassLoaderData::loader_name_and_id() const {
  if (_class_loader_klass == NULL) {
    return "'" BOOTSTRAP_LOADER_NAME "'";
  } else if (_name_and_id != NULL) {
    return _name_and_id->as_C_string();
  } else {
    // May be called in a race before _name_and_id is initialized.
    return _class_loader_klass->external_name();
  }
}

void ClassLoaderData::print_value_on(outputStream* out) const {
  if (!is_unloading() && class_loader() != NULL) {
    out->print("loader data: " INTPTR_FORMAT " for instance ", p2i(this));
    class_loader()->print_value_on(out);  // includes loader_name_and_id() and address of class loader instance
  } else {
    // loader data: 0xsomeaddr of 'bootstrap'
    out->print("loader data: " INTPTR_FORMAT " of %s", p2i(this), loader_name_and_id());
  }
  if (_has_class_mirror_holder) {
    out->print(" has a class holder");
  }
}

void ClassLoaderData::print_value() const { print_value_on(tty); }

#ifndef PRODUCT
class PrintKlassClosure: public KlassClosure {
  outputStream* _out;
public:
  PrintKlassClosure(outputStream* out): _out(out) { }

  void do_klass(Klass* k) {
    ResourceMark rm;
    _out->print("%s,", k->external_name());
  }
};

void ClassLoaderData::print_on(outputStream* out) const {
  ResourceMark rm;
  out->print_cr("ClassLoaderData(" INTPTR_FORMAT ")", p2i(this));
  out->print_cr(" - name                %s", loader_name_and_id());
  if (!_holder.is_null()) {
    out->print   (" - holder              ");
    _holder.print_on(out);
    out->print_cr("");
  }
  out->print_cr(" - class loader        " INTPTR_FORMAT, p2i(_class_loader.ptr_raw()));
  out->print_cr(" - metaspace           " INTPTR_FORMAT, p2i(_metaspace));
  out->print_cr(" - unloading           %s", _unloading ? "true" : "false");
  out->print_cr(" - class mirror holder %s", _has_class_mirror_holder ? "true" : "false");
  out->print_cr(" - modified oops       %s", _modified_oops ? "true" : "false");
  out->print_cr(" - keep alive          %d", _keep_alive);
  out->print   (" - claim               ");
  switch(_claim) {
    case _claim_none:       out->print_cr("none"); break;
    case _claim_finalizable:out->print_cr("finalizable"); break;
    case _claim_strong:     out->print_cr("strong"); break;
    case _claim_other:      out->print_cr("other"); break;
    default:                ShouldNotReachHere();
  }
  out->print_cr(" - handles             %d", _handles.count());
  out->print_cr(" - dependency count    %d", _dependency_count);
  out->print   (" - klasses             {");
  PrintKlassClosure closure(out);
  ((ClassLoaderData*)this)->classes_do(&closure);
  out->print_cr(" }");
  out->print_cr(" - packages            " INTPTR_FORMAT, p2i(_packages));
  out->print_cr(" - module              " INTPTR_FORMAT, p2i(_modules));
  out->print_cr(" - unnamed module      " INTPTR_FORMAT, p2i(_unnamed_module));
  out->print_cr(" - dictionary          " INTPTR_FORMAT, p2i(_dictionary));
  if (_jmethod_ids != NULL) {
    out->print   (" - jmethod count       ");
    Method::print_jmethod_ids_count(this, out);
    out->print_cr("");
  }
  out->print_cr(" - deallocate list     " INTPTR_FORMAT, p2i(_deallocate_list));
  out->print_cr(" - next CLD            " INTPTR_FORMAT, p2i(_next));
}
#endif // PRODUCT

void ClassLoaderData::print() const { print_on(tty); }

void ClassLoaderData::verify() {
  assert_locked_or_safepoint(_metaspace_lock);
  oop cl = class_loader();

  guarantee(this == class_loader_data(cl) || has_class_mirror_holder(), "Must be the same");
  guarantee(cl != NULL || this == ClassLoaderData::the_null_class_loader_data() || has_class_mirror_holder(), "must be");

  // Verify the integrity of the allocated space.
#ifdef ASSERT
  if (metaspace_or_null() != NULL) {
    metaspace_or_null()->verify();
  }
#endif

  for (Klass* k = _klasses; k != NULL; k = k->next_link()) {
    guarantee(k->class_loader_data() == this, "Must be the same");
    k->verify();
    assert(k != k->next_link(), "no loops!");
  }
}

bool ClassLoaderData::contains_klass(Klass* klass) {
  // Lock-free access requires load_acquire
  for (Klass* k = Atomic::load_acquire(&_klasses); k != NULL; k = k->next_link()) {
    if (k == klass) return true;
  }
  return false;
}
