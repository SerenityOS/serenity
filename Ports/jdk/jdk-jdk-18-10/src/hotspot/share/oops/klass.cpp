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
#include "jvm_io.h"
#include "cds/heapShared.hpp"
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/classLoaderDataGraph.inline.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/moduleEntry.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/systemDictionaryShared.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "logging/log.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/metaspaceClosure.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/arguments.hpp"
#include "runtime/atomic.hpp"
#include "runtime/handles.inline.hpp"
#include "utilities/macros.hpp"
#include "utilities/powerOfTwo.hpp"
#include "utilities/stack.inline.hpp"

void Klass::set_java_mirror(Handle m) {
  assert(!m.is_null(), "New mirror should never be null.");
  assert(_java_mirror.is_empty(), "should only be used to initialize mirror");
  _java_mirror = class_loader_data()->add_handle(m);
}

oop Klass::java_mirror_no_keepalive() const {
  return _java_mirror.peek();
}

void Klass::replace_java_mirror(oop mirror) {
  _java_mirror.replace(mirror);
}

bool Klass::is_cloneable() const {
  return _access_flags.is_cloneable_fast() ||
         is_subtype_of(vmClasses::Cloneable_klass());
}

void Klass::set_is_cloneable() {
  if (name() == vmSymbols::java_lang_invoke_MemberName()) {
    assert(is_final(), "no subclasses allowed");
    // MemberName cloning should not be intrinsified and always happen in JVM_Clone.
  } else if (is_instance_klass() && InstanceKlass::cast(this)->reference_type() != REF_NONE) {
    // Reference cloning should not be intrinsified and always happen in JVM_Clone.
  } else {
    _access_flags.set_is_cloneable_fast();
  }
}

void Klass::set_name(Symbol* n) {
  _name = n;
  if (_name != NULL) _name->increment_refcount();

  if (Arguments::is_dumping_archive() && is_instance_klass()) {
    SystemDictionaryShared::init_dumptime_info(InstanceKlass::cast(this));
  }
}

bool Klass::is_subclass_of(const Klass* k) const {
  // Run up the super chain and check
  if (this == k) return true;

  Klass* t = const_cast<Klass*>(this)->super();

  while (t != NULL) {
    if (t == k) return true;
    t = t->super();
  }
  return false;
}

void Klass::release_C_heap_structures() {
  if (_name != NULL) _name->decrement_refcount();
}

bool Klass::search_secondary_supers(Klass* k) const {
  // Put some extra logic here out-of-line, before the search proper.
  // This cuts down the size of the inline method.

  // This is necessary, since I am never in my own secondary_super list.
  if (this == k)
    return true;
  // Scan the array-of-objects for a match
  int cnt = secondary_supers()->length();
  for (int i = 0; i < cnt; i++) {
    if (secondary_supers()->at(i) == k) {
      ((Klass*)this)->set_secondary_super_cache(k);
      return true;
    }
  }
  return false;
}

// Return self, except for abstract classes with exactly 1
// implementor.  Then return the 1 concrete implementation.
Klass *Klass::up_cast_abstract() {
  Klass *r = this;
  while( r->is_abstract() ) {   // Receiver is abstract?
    Klass *s = r->subklass();   // Check for exactly 1 subklass
    if (s == NULL || s->next_sibling() != NULL) // Oops; wrong count; give up
      return this;              // Return 'this' as a no-progress flag
    r = s;                    // Loop till find concrete class
  }
  return r;                   // Return the 1 concrete class
}

// Find LCA in class hierarchy
Klass *Klass::LCA( Klass *k2 ) {
  Klass *k1 = this;
  while( 1 ) {
    if( k1->is_subtype_of(k2) ) return k2;
    if( k2->is_subtype_of(k1) ) return k1;
    k1 = k1->super();
    k2 = k2->super();
  }
}


void Klass::check_valid_for_instantiation(bool throwError, TRAPS) {
  ResourceMark rm(THREAD);
  THROW_MSG(throwError ? vmSymbols::java_lang_InstantiationError()
            : vmSymbols::java_lang_InstantiationException(), external_name());
}


void Klass::copy_array(arrayOop s, int src_pos, arrayOop d, int dst_pos, int length, TRAPS) {
  ResourceMark rm(THREAD);
  assert(s != NULL, "Throw NPE!");
  THROW_MSG(vmSymbols::java_lang_ArrayStoreException(),
            err_msg("arraycopy: source type %s is not an array", s->klass()->external_name()));
}


void Klass::initialize(TRAPS) {
  ShouldNotReachHere();
}

Klass* Klass::find_field(Symbol* name, Symbol* sig, fieldDescriptor* fd) const {
#ifdef ASSERT
  tty->print_cr("Error: find_field called on a klass oop."
                " Likely error: reflection method does not correctly"
                " wrap return value in a mirror object.");
#endif
  ShouldNotReachHere();
  return NULL;
}

Method* Klass::uncached_lookup_method(const Symbol* name, const Symbol* signature,
                                      OverpassLookupMode overpass_mode,
                                      PrivateLookupMode private_mode) const {
#ifdef ASSERT
  tty->print_cr("Error: uncached_lookup_method called on a klass oop."
                " Likely error: reflection method does not correctly"
                " wrap return value in a mirror object.");
#endif
  ShouldNotReachHere();
  return NULL;
}

void* Klass::operator new(size_t size, ClassLoaderData* loader_data, size_t word_size, TRAPS) throw() {
  return Metaspace::allocate(loader_data, word_size, MetaspaceObj::ClassType, THREAD);
}

// "Normal" instantiation is preceeded by a MetaspaceObj allocation
// which zeros out memory - calloc equivalent.
// The constructor is also used from CppVtableCloner,
// which doesn't zero out the memory before calling the constructor.
Klass::Klass(KlassID id) : _id(id),
                           _shared_class_path_index(-1) {
  CDS_ONLY(_shared_class_flags = 0;)
  CDS_JAVA_HEAP_ONLY(_archived_mirror_index = -1;)
  _primary_supers[0] = this;
  set_super_check_offset(in_bytes(primary_supers_offset()));
}

jint Klass::array_layout_helper(BasicType etype) {
  assert(etype >= T_BOOLEAN && etype <= T_OBJECT, "valid etype");
  // Note that T_ARRAY is not allowed here.
  int  hsize = arrayOopDesc::base_offset_in_bytes(etype);
  int  esize = type2aelembytes(etype);
  bool isobj = (etype == T_OBJECT);
  int  tag   =  isobj ? _lh_array_tag_obj_value : _lh_array_tag_type_value;
  int lh = array_layout_helper(tag, hsize, etype, exact_log2(esize));

  assert(lh < (int)_lh_neutral_value, "must look like an array layout");
  assert(layout_helper_is_array(lh), "correct kind");
  assert(layout_helper_is_objArray(lh) == isobj, "correct kind");
  assert(layout_helper_is_typeArray(lh) == !isobj, "correct kind");
  assert(layout_helper_header_size(lh) == hsize, "correct decode");
  assert(layout_helper_element_type(lh) == etype, "correct decode");
  assert(1 << layout_helper_log2_element_size(lh) == esize, "correct decode");

  return lh;
}

bool Klass::can_be_primary_super_slow() const {
  if (super() == NULL)
    return true;
  else if (super()->super_depth() >= primary_super_limit()-1)
    return false;
  else
    return true;
}

void Klass::initialize_supers(Klass* k, Array<InstanceKlass*>* transitive_interfaces, TRAPS) {
  if (k == NULL) {
    set_super(NULL);
    _primary_supers[0] = this;
    assert(super_depth() == 0, "Object must already be initialized properly");
  } else if (k != super() || k == vmClasses::Object_klass()) {
    assert(super() == NULL || super() == vmClasses::Object_klass(),
           "initialize this only once to a non-trivial value");
    set_super(k);
    Klass* sup = k;
    int sup_depth = sup->super_depth();
    juint my_depth  = MIN2(sup_depth + 1, (int)primary_super_limit());
    if (!can_be_primary_super_slow())
      my_depth = primary_super_limit();
    for (juint i = 0; i < my_depth; i++) {
      _primary_supers[i] = sup->_primary_supers[i];
    }
    Klass* *super_check_cell;
    if (my_depth < primary_super_limit()) {
      _primary_supers[my_depth] = this;
      super_check_cell = &_primary_supers[my_depth];
    } else {
      // Overflow of the primary_supers array forces me to be secondary.
      super_check_cell = &_secondary_super_cache;
    }
    set_super_check_offset((address)super_check_cell - (address) this);

#ifdef ASSERT
    {
      juint j = super_depth();
      assert(j == my_depth, "computed accessor gets right answer");
      Klass* t = this;
      while (!t->can_be_primary_super()) {
        t = t->super();
        j = t->super_depth();
      }
      for (juint j1 = j+1; j1 < primary_super_limit(); j1++) {
        assert(primary_super_of_depth(j1) == NULL, "super list padding");
      }
      while (t != NULL) {
        assert(primary_super_of_depth(j) == t, "super list initialization");
        t = t->super();
        --j;
      }
      assert(j == (juint)-1, "correct depth count");
    }
#endif
  }

  if (secondary_supers() == NULL) {

    // Now compute the list of secondary supertypes.
    // Secondaries can occasionally be on the super chain,
    // if the inline "_primary_supers" array overflows.
    int extras = 0;
    Klass* p;
    for (p = super(); !(p == NULL || p->can_be_primary_super()); p = p->super()) {
      ++extras;
    }

    ResourceMark rm(THREAD);  // need to reclaim GrowableArrays allocated below

    // Compute the "real" non-extra secondaries.
    GrowableArray<Klass*>* secondaries = compute_secondary_supers(extras, transitive_interfaces);
    if (secondaries == NULL) {
      // secondary_supers set by compute_secondary_supers
      return;
    }

    GrowableArray<Klass*>* primaries = new GrowableArray<Klass*>(extras);

    for (p = super(); !(p == NULL || p->can_be_primary_super()); p = p->super()) {
      int i;                    // Scan for overflow primaries being duplicates of 2nd'arys

      // This happens frequently for very deeply nested arrays: the
      // primary superclass chain overflows into the secondary.  The
      // secondary list contains the element_klass's secondaries with
      // an extra array dimension added.  If the element_klass's
      // secondary list already contains some primary overflows, they
      // (with the extra level of array-ness) will collide with the
      // normal primary superclass overflows.
      for( i = 0; i < secondaries->length(); i++ ) {
        if( secondaries->at(i) == p )
          break;
      }
      if( i < secondaries->length() )
        continue;               // It's a dup, don't put it in
      primaries->push(p);
    }
    // Combine the two arrays into a metadata object to pack the array.
    // The primaries are added in the reverse order, then the secondaries.
    int new_length = primaries->length() + secondaries->length();
    Array<Klass*>* s2 = MetadataFactory::new_array<Klass*>(
                                       class_loader_data(), new_length, CHECK);
    int fill_p = primaries->length();
    for (int j = 0; j < fill_p; j++) {
      s2->at_put(j, primaries->pop());  // add primaries in reverse order.
    }
    for( int j = 0; j < secondaries->length(); j++ ) {
      s2->at_put(j+fill_p, secondaries->at(j));  // add secondaries on the end.
    }

  #ifdef ASSERT
      // We must not copy any NULL placeholders left over from bootstrap.
    for (int j = 0; j < s2->length(); j++) {
      assert(s2->at(j) != NULL, "correct bootstrapping order");
    }
  #endif

    set_secondary_supers(s2);
  }
}

GrowableArray<Klass*>* Klass::compute_secondary_supers(int num_extra_slots,
                                                       Array<InstanceKlass*>* transitive_interfaces) {
  assert(num_extra_slots == 0, "override for complex klasses");
  assert(transitive_interfaces == NULL, "sanity");
  set_secondary_supers(Universe::the_empty_klass_array());
  return NULL;
}


// superklass links
InstanceKlass* Klass::superklass() const {
  assert(super() == NULL || super()->is_instance_klass(), "must be instance klass");
  return _super == NULL ? NULL : InstanceKlass::cast(_super);
}

// subklass links.  Used by the compiler (and vtable initialization)
// May be cleaned concurrently, so must use the Compile_lock.
// The log parameter is for clean_weak_klass_links to report unlinked classes.
Klass* Klass::subklass(bool log) const {
  // Need load_acquire on the _subklass, because it races with inserts that
  // publishes freshly initialized data.
  for (Klass* chain = Atomic::load_acquire(&_subklass);
       chain != NULL;
       // Do not need load_acquire on _next_sibling, because inserts never
       // create _next_sibling edges to dead data.
       chain = Atomic::load(&chain->_next_sibling))
  {
    if (chain->is_loader_alive()) {
      return chain;
    } else if (log) {
      if (log_is_enabled(Trace, class, unload)) {
        ResourceMark rm;
        log_trace(class, unload)("unlinking class (subclass): %s", chain->external_name());
      }
    }
  }
  return NULL;
}

Klass* Klass::next_sibling(bool log) const {
  // Do not need load_acquire on _next_sibling, because inserts never
  // create _next_sibling edges to dead data.
  for (Klass* chain = Atomic::load(&_next_sibling);
       chain != NULL;
       chain = Atomic::load(&chain->_next_sibling)) {
    // Only return alive klass, there may be stale klass
    // in this chain if cleaned concurrently.
    if (chain->is_loader_alive()) {
      return chain;
    } else if (log) {
      if (log_is_enabled(Trace, class, unload)) {
        ResourceMark rm;
        log_trace(class, unload)("unlinking class (sibling): %s", chain->external_name());
      }
    }
  }
  return NULL;
}

void Klass::set_subklass(Klass* s) {
  assert(s != this, "sanity check");
  Atomic::release_store(&_subklass, s);
}

void Klass::set_next_sibling(Klass* s) {
  assert(s != this, "sanity check");
  // Does not need release semantics. If used by cleanup, it will link to
  // already safely published data, and if used by inserts, will be published
  // safely using cmpxchg.
  Atomic::store(&_next_sibling, s);
}

void Klass::append_to_sibling_list() {
  if (Universe::is_fully_initialized()) {
    assert_locked_or_safepoint(Compile_lock);
  }
  debug_only(verify();)
  // add ourselves to superklass' subklass list
  InstanceKlass* super = superklass();
  if (super == NULL) return;        // special case: class Object
  assert((!super->is_interface()    // interfaces cannot be supers
          && (super->superklass() == NULL || !is_interface())),
         "an interface can only be a subklass of Object");

  // Make sure there is no stale subklass head
  super->clean_subklass();

  for (;;) {
    Klass* prev_first_subklass = Atomic::load_acquire(&_super->_subklass);
    if (prev_first_subklass != NULL) {
      // set our sibling to be the superklass' previous first subklass
      assert(prev_first_subklass->is_loader_alive(), "May not attach not alive klasses");
      set_next_sibling(prev_first_subklass);
    }
    // Note that the prev_first_subklass is always alive, meaning no sibling_next links
    // are ever created to not alive klasses. This is an important invariant of the lock-free
    // cleaning protocol, that allows us to safely unlink dead klasses from the sibling list.
    if (Atomic::cmpxchg(&super->_subklass, prev_first_subklass, this) == prev_first_subklass) {
      return;
    }
  }
  debug_only(verify();)
}

void Klass::clean_subklass() {
  for (;;) {
    // Need load_acquire, due to contending with concurrent inserts
    Klass* subklass = Atomic::load_acquire(&_subklass);
    if (subklass == NULL || subklass->is_loader_alive()) {
      return;
    }
    // Try to fix _subklass until it points at something not dead.
    Atomic::cmpxchg(&_subklass, subklass, subklass->next_sibling());
  }
}

void Klass::clean_weak_klass_links(bool unloading_occurred, bool clean_alive_klasses) {
  if (!ClassUnloading || !unloading_occurred) {
    return;
  }

  Klass* root = vmClasses::Object_klass();
  Stack<Klass*, mtGC> stack;

  stack.push(root);
  while (!stack.is_empty()) {
    Klass* current = stack.pop();

    assert(current->is_loader_alive(), "just checking, this should be live");

    // Find and set the first alive subklass
    Klass* sub = current->subklass(true);
    current->clean_subklass();
    if (sub != NULL) {
      stack.push(sub);
    }

    // Find and set the first alive sibling
    Klass* sibling = current->next_sibling(true);
    current->set_next_sibling(sibling);
    if (sibling != NULL) {
      stack.push(sibling);
    }

    // Clean the implementors list and method data.
    if (clean_alive_klasses && current->is_instance_klass()) {
      InstanceKlass* ik = InstanceKlass::cast(current);
      ik->clean_weak_instanceklass_links();

      // JVMTI RedefineClasses creates previous versions that are not in
      // the class hierarchy, so process them here.
      while ((ik = ik->previous_versions()) != NULL) {
        ik->clean_weak_instanceklass_links();
      }
    }
  }
}

void Klass::metaspace_pointers_do(MetaspaceClosure* it) {
  if (log_is_enabled(Trace, cds)) {
    ResourceMark rm;
    log_trace(cds)("Iter(Klass): %p (%s)", this, external_name());
  }

  it->push(&_name);
  it->push(&_secondary_super_cache);
  it->push(&_secondary_supers);
  for (int i = 0; i < _primary_super_limit; i++) {
    it->push(&_primary_supers[i]);
  }
  it->push(&_super);
  if (!Arguments::is_dumping_archive()) {
    // If dumping archive, these may point to excluded classes. There's no need
    // to follow these pointers anyway, as they will be set to NULL in
    // remove_unshareable_info().
    it->push((Klass**)&_subklass);
    it->push((Klass**)&_next_sibling);
    it->push(&_next_link);
  }

  vtableEntry* vt = start_of_vtable();
  for (int i=0; i<vtable_length(); i++) {
    it->push(vt[i].method_addr());
  }
}

void Klass::remove_unshareable_info() {
  assert (Arguments::is_dumping_archive(),
          "only called during CDS dump time");
  JFR_ONLY(REMOVE_ID(this);)
  if (log_is_enabled(Trace, cds, unshareable)) {
    ResourceMark rm;
    log_trace(cds, unshareable)("remove: %s", external_name());
  }

  set_subklass(NULL);
  set_next_sibling(NULL);
  set_next_link(NULL);

  // Null out class_loader_data because we don't share that yet.
  set_class_loader_data(NULL);
  set_is_shared();
}

void Klass::remove_java_mirror() {
  Arguments::assert_is_dumping_archive();
  if (log_is_enabled(Trace, cds, unshareable)) {
    ResourceMark rm;
    log_trace(cds, unshareable)("remove java_mirror: %s", external_name());
  }
  // Just null out the mirror.  The class_loader_data() no longer exists.
  clear_java_mirror_handle();
}

void Klass::restore_unshareable_info(ClassLoaderData* loader_data, Handle protection_domain, TRAPS) {
  assert(is_klass(), "ensure C++ vtable is restored");
  assert(is_shared(), "must be set");
  JFR_ONLY(RESTORE_ID(this);)
  if (log_is_enabled(Trace, cds, unshareable)) {
    ResourceMark rm(THREAD);
    log_trace(cds, unshareable)("restore: %s", external_name());
  }

  // If an exception happened during CDS restore, some of these fields may already be
  // set.  We leave the class on the CLD list, even if incomplete so that we don't
  // modify the CLD list outside a safepoint.
  if (class_loader_data() == NULL) {
    set_class_loader_data(loader_data);

    // Add to class loader list first before creating the mirror
    // (same order as class file parsing)
    loader_data->add_class(this);
  }

  Handle loader(THREAD, loader_data->class_loader());
  ModuleEntry* module_entry = NULL;
  Klass* k = this;
  if (k->is_objArray_klass()) {
    k = ObjArrayKlass::cast(k)->bottom_klass();
  }
  // Obtain klass' module.
  if (k->is_instance_klass()) {
    InstanceKlass* ik = (InstanceKlass*) k;
    module_entry = ik->module();
  } else {
    module_entry = ModuleEntryTable::javabase_moduleEntry();
  }
  // Obtain java.lang.Module, if available
  Handle module_handle(THREAD, ((module_entry != NULL) ? module_entry->module() : (oop)NULL));

  if (this->has_archived_mirror_index()) {
    ResourceMark rm(THREAD);
    log_debug(cds, mirror)("%s has raw archived mirror", external_name());
    if (HeapShared::open_regions_mapped()) {
      bool present = java_lang_Class::restore_archived_mirror(this, loader, module_handle,
                                                              protection_domain,
                                                              CHECK);
      if (present) {
        return;
      }
    }

    // No archived mirror data
    log_debug(cds, mirror)("No archived mirror data for %s", external_name());
    clear_java_mirror_handle();
    this->clear_archived_mirror_index();
  }

  // Only recreate it if not present.  A previous attempt to restore may have
  // gotten an OOM later but keep the mirror if it was created.
  if (java_mirror() == NULL) {
    ResourceMark rm(THREAD);
    log_trace(cds, mirror)("Recreate mirror for %s", external_name());
    java_lang_Class::create_mirror(this, loader, module_handle, protection_domain, Handle(), CHECK);
  }
}

#if INCLUDE_CDS_JAVA_HEAP
oop Klass::archived_java_mirror() {
  assert(has_archived_mirror_index(), "must have archived mirror");
  return HeapShared::get_root(_archived_mirror_index);
}

void Klass::clear_archived_mirror_index() {
  if (_archived_mirror_index >= 0) {
    HeapShared::clear_root(_archived_mirror_index);
  }
  _archived_mirror_index = -1;
}

// No GC barrier
void Klass::set_archived_java_mirror(oop m) {
  assert(DumpSharedSpaces, "called only during runtime");
  _archived_mirror_index = HeapShared::append_root(m);
}
#endif // INCLUDE_CDS_JAVA_HEAP

void Klass::check_array_allocation_length(int length, int max_length, TRAPS) {
  if (length > max_length) {
    if (!THREAD->in_retryable_allocation()) {
      report_java_out_of_memory("Requested array size exceeds VM limit");
      JvmtiExport::post_array_size_exhausted();
      THROW_OOP(Universe::out_of_memory_error_array_size());
    } else {
      THROW_OOP(Universe::out_of_memory_error_retry());
    }
  } else if (length < 0) {
    THROW_MSG(vmSymbols::java_lang_NegativeArraySizeException(), err_msg("%d", length));
  }
}

// Replace the last '+' char with '/'.
static char* convert_hidden_name_to_java(Symbol* name) {
  size_t name_len = name->utf8_length();
  char* result = NEW_RESOURCE_ARRAY(char, name_len + 1);
  name->as_klass_external_name(result, (int)name_len + 1);
  for (int index = (int)name_len; index > 0; index--) {
    if (result[index] == '+') {
      result[index] = JVM_SIGNATURE_SLASH;
      break;
    }
  }
  return result;
}

// In product mode, this function doesn't have virtual function calls so
// there might be some performance advantage to handling InstanceKlass here.
const char* Klass::external_name() const {
  if (is_instance_klass()) {
    const InstanceKlass* ik = static_cast<const InstanceKlass*>(this);
    if (ik->is_hidden()) {
      char* result = convert_hidden_name_to_java(name());
      return result;
    }
  } else if (is_objArray_klass() && ObjArrayKlass::cast(this)->bottom_klass()->is_hidden()) {
    char* result = convert_hidden_name_to_java(name());
    return result;
  }
  if (name() == NULL)  return "<unknown>";
  return name()->as_klass_external_name();
}

const char* Klass::signature_name() const {
  if (name() == NULL)  return "<unknown>";
  if (is_objArray_klass() && ObjArrayKlass::cast(this)->bottom_klass()->is_hidden()) {
    size_t name_len = name()->utf8_length();
    char* result = NEW_RESOURCE_ARRAY(char, name_len + 1);
    name()->as_C_string(result, (int)name_len + 1);
    for (int index = (int)name_len; index > 0; index--) {
      if (result[index] == '+') {
        result[index] = JVM_SIGNATURE_DOT;
        break;
      }
    }
    return result;
  }
  return name()->as_C_string();
}

const char* Klass::external_kind() const {
  if (is_interface()) return "interface";
  if (is_abstract()) return "abstract class";
  return "class";
}

// Unless overridden, jvmti_class_status has no flags set.
jint Klass::jvmti_class_status() const {
  return 0;
}


// Printing

void Klass::print_on(outputStream* st) const {
  ResourceMark rm;
  // print title
  st->print("%s", internal_name());
  print_address_on(st);
  st->cr();
}

#define BULLET  " - "

// Caller needs ResourceMark
void Klass::oop_print_on(oop obj, outputStream* st) {
  // print title
  st->print_cr("%s ", internal_name());
  obj->print_address_on(st);

  if (WizardMode) {
     // print header
     obj->mark().print_on(st);
     st->cr();
  }

  // print class
  st->print(BULLET"klass: ");
  obj->klass()->print_value_on(st);
  st->cr();
}

void Klass::oop_print_value_on(oop obj, outputStream* st) {
  // print title
  ResourceMark rm;              // Cannot print in debug mode without this
  st->print("%s", internal_name());
  obj->print_address_on(st);
}

// Verification

void Klass::verify_on(outputStream* st) {

  // This can be expensive, but it is worth checking that this klass is actually
  // in the CLD graph but not in production.
  assert(Metaspace::contains((address)this), "Should be");

  guarantee(this->is_klass(),"should be klass");

  if (super() != NULL) {
    guarantee(super()->is_klass(), "should be klass");
  }
  if (secondary_super_cache() != NULL) {
    Klass* ko = secondary_super_cache();
    guarantee(ko->is_klass(), "should be klass");
  }
  for ( uint i = 0; i < primary_super_limit(); i++ ) {
    Klass* ko = _primary_supers[i];
    if (ko != NULL) {
      guarantee(ko->is_klass(), "should be klass");
    }
  }

  if (java_mirror_no_keepalive() != NULL) {
    guarantee(oopDesc::is_oop(java_mirror_no_keepalive()), "should be instance");
  }
}

void Klass::oop_verify_on(oop obj, outputStream* st) {
  guarantee(oopDesc::is_oop(obj),  "should be oop");
  guarantee(obj->klass()->is_klass(), "klass field is not a klass");
}

bool Klass::is_valid(Klass* k) {
  if (!is_aligned(k, sizeof(MetaWord))) return false;
  if ((size_t)k < os::min_page_size()) return false;

  if (!os::is_readable_range(k, k + 1)) return false;
  if (!Metaspace::contains(k)) return false;

  if (!Symbol::is_valid(k->name())) return false;
  return ClassLoaderDataGraph::is_valid(k->class_loader_data());
}

Method* Klass::method_at_vtable(int index)  {
#ifndef PRODUCT
  assert(index >= 0, "valid vtable index");
  if (DebugVtables) {
    verify_vtable_index(index);
  }
#endif
  return start_of_vtable()[index].method();
}


#ifndef PRODUCT

bool Klass::verify_vtable_index(int i) {
  int limit = vtable_length()/vtableEntry::size();
  assert(i >= 0 && i < limit, "index %d out of bounds %d", i, limit);
  return true;
}

#endif // PRODUCT

// Caller needs ResourceMark
// joint_in_module_of_loader provides an optimization if 2 classes are in
// the same module to succinctly print out relevant information about their
// module name and class loader's name_and_id for error messages.
// Format:
//   <fully-qualified-external-class-name1> and <fully-qualified-external-class-name2>
//                      are in module <module-name>[@<version>]
//                      of loader <loader-name_and_id>[, parent loader <parent-loader-name_and_id>]
const char* Klass::joint_in_module_of_loader(const Klass* class2, bool include_parent_loader) const {
  assert(module() == class2->module(), "classes do not have the same module");
  const char* class1_name = external_name();
  size_t len = strlen(class1_name) + 1;

  const char* class2_description = class2->class_in_module_of_loader(true, include_parent_loader);
  len += strlen(class2_description);

  len += strlen(" and ");

  char* joint_description = NEW_RESOURCE_ARRAY_RETURN_NULL(char, len);

  // Just return the FQN if error when allocating string
  if (joint_description == NULL) {
    return class1_name;
  }

  jio_snprintf(joint_description, len, "%s and %s",
               class1_name,
               class2_description);

  return joint_description;
}

// Caller needs ResourceMark
// class_in_module_of_loader provides a standard way to include
// relevant information about a class, such as its module name as
// well as its class loader's name_and_id, in error messages and logging.
// Format:
//   <fully-qualified-external-class-name> is in module <module-name>[@<version>]
//                                         of loader <loader-name_and_id>[, parent loader <parent-loader-name_and_id>]
const char* Klass::class_in_module_of_loader(bool use_are, bool include_parent_loader) const {
  // 1. fully qualified external name of class
  const char* klass_name = external_name();
  size_t len = strlen(klass_name) + 1;

  // 2. module name + @version
  const char* module_name = "";
  const char* version = "";
  bool has_version = false;
  bool module_is_named = false;
  const char* module_name_phrase = "";
  const Klass* bottom_klass = is_objArray_klass() ?
                                ObjArrayKlass::cast(this)->bottom_klass() : this;
  if (bottom_klass->is_instance_klass()) {
    ModuleEntry* module = InstanceKlass::cast(bottom_klass)->module();
    if (module->is_named()) {
      module_is_named = true;
      module_name_phrase = "module ";
      module_name = module->name()->as_C_string();
      len += strlen(module_name);
      // Use version if exists and is not a jdk module
      if (module->should_show_version()) {
        has_version = true;
        version = module->version()->as_C_string();
        // Include stlen(version) + 1 for the "@"
        len += strlen(version) + 1;
      }
    } else {
      module_name = UNNAMED_MODULE;
      len += UNNAMED_MODULE_LEN;
    }
  } else {
    // klass is an array of primitives, module is java.base
    module_is_named = true;
    module_name_phrase = "module ";
    module_name = JAVA_BASE_NAME;
    len += JAVA_BASE_NAME_LEN;
  }

  // 3. class loader's name_and_id
  ClassLoaderData* cld = class_loader_data();
  assert(cld != NULL, "class_loader_data should not be null");
  const char* loader_name_and_id = cld->loader_name_and_id();
  len += strlen(loader_name_and_id);

  // 4. include parent loader information
  const char* parent_loader_phrase = "";
  const char* parent_loader_name_and_id = "";
  if (include_parent_loader &&
      !cld->is_builtin_class_loader_data()) {
    oop parent_loader = java_lang_ClassLoader::parent(class_loader());
    ClassLoaderData *parent_cld = ClassLoaderData::class_loader_data_or_null(parent_loader);
    // The parent loader's ClassLoaderData could be null if it is
    // a delegating class loader that has never defined a class.
    // In this case the loader's name must be obtained via the parent loader's oop.
    if (parent_cld == NULL) {
      oop cl_name_and_id = java_lang_ClassLoader::nameAndId(parent_loader);
      if (cl_name_and_id != NULL) {
        parent_loader_name_and_id = java_lang_String::as_utf8_string(cl_name_and_id);
      }
    } else {
      parent_loader_name_and_id = parent_cld->loader_name_and_id();
    }
    parent_loader_phrase = ", parent loader ";
    len += strlen(parent_loader_phrase) + strlen(parent_loader_name_and_id);
  }

  // Start to construct final full class description string
  len += ((use_are) ? strlen(" are in ") : strlen(" is in "));
  len += strlen(module_name_phrase) + strlen(" of loader ");

  char* class_description = NEW_RESOURCE_ARRAY_RETURN_NULL(char, len);

  // Just return the FQN if error when allocating string
  if (class_description == NULL) {
    return klass_name;
  }

  jio_snprintf(class_description, len, "%s %s in %s%s%s%s of loader %s%s%s",
               klass_name,
               (use_are) ? "are" : "is",
               module_name_phrase,
               module_name,
               (has_version) ? "@" : "",
               (has_version) ? version : "",
               loader_name_and_id,
               parent_loader_phrase,
               parent_loader_name_and_id);

  return class_description;
}
