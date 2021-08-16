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
#include "jvm.h"
#include "cds/heapShared.hpp"
#include "classfile/classLoaderData.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/metadataOnStackMark.hpp"
#include "classfile/stringTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "interpreter/bootstrapInfo.hpp"
#include "interpreter/linkResolver.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/metaspaceClosure.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/array.hpp"
#include "oops/constantPool.inline.hpp"
#include "oops/cpCache.inline.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/atomic.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/init.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/signature.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vframe.inline.hpp"
#include "utilities/copy.hpp"

ConstantPool* ConstantPool::allocate(ClassLoaderData* loader_data, int length, TRAPS) {
  Array<u1>* tags = MetadataFactory::new_array<u1>(loader_data, length, 0, CHECK_NULL);
  int size = ConstantPool::size(length);
  return new (loader_data, size, MetaspaceObj::ConstantPoolType, THREAD) ConstantPool(tags);
}

void ConstantPool::copy_fields(const ConstantPool* orig) {
  // Preserve dynamic constant information from the original pool
  if (orig->has_dynamic_constant()) {
    set_has_dynamic_constant();
  }

  set_major_version(orig->major_version());
  set_minor_version(orig->minor_version());

  set_source_file_name_index(orig->source_file_name_index());
  set_generic_signature_index(orig->generic_signature_index());
}

#ifdef ASSERT

// MetaspaceObj allocation invariant is calloc equivalent memory
// simple verification of this here (JVM_CONSTANT_Invalid == 0 )
static bool tag_array_is_zero_initialized(Array<u1>* tags) {
  assert(tags != NULL, "invariant");
  const int length = tags->length();
  for (int index = 0; index < length; ++index) {
    if (JVM_CONSTANT_Invalid != tags->at(index)) {
      return false;
    }
  }
  return true;
}

#endif

ConstantPool::ConstantPool(Array<u1>* tags) :
  _tags(tags),
  _length(tags->length()) {

    assert(_tags != NULL, "invariant");
    assert(tags->length() == _length, "invariant");
    assert(tag_array_is_zero_initialized(tags), "invariant");
    assert(0 == flags(), "invariant");
    assert(0 == version(), "invariant");
    assert(NULL == _pool_holder, "invariant");
}

void ConstantPool::deallocate_contents(ClassLoaderData* loader_data) {
  if (cache() != NULL) {
    MetadataFactory::free_metadata(loader_data, cache());
    set_cache(NULL);
  }

  MetadataFactory::free_array<Klass*>(loader_data, resolved_klasses());
  set_resolved_klasses(NULL);

  MetadataFactory::free_array<jushort>(loader_data, operands());
  set_operands(NULL);

  release_C_heap_structures();

  // free tag array
  MetadataFactory::free_array<u1>(loader_data, tags());
  set_tags(NULL);
}

void ConstantPool::release_C_heap_structures() {
  // walk constant pool and decrement symbol reference counts
  unreference_symbols();
}

void ConstantPool::metaspace_pointers_do(MetaspaceClosure* it) {
  log_trace(cds)("Iter(ConstantPool): %p", this);

  it->push(&_tags, MetaspaceClosure::_writable);
  it->push(&_cache);
  it->push(&_pool_holder);
  it->push(&_operands);
  it->push(&_resolved_klasses, MetaspaceClosure::_writable);

  for (int i = 0; i < length(); i++) {
    // The only MSO's embedded in the CP entries are Symbols:
    //   JVM_CONSTANT_String (normal and pseudo)
    //   JVM_CONSTANT_Utf8
    constantTag ctag = tag_at(i);
    if (ctag.is_string() || ctag.is_utf8()) {
      it->push(symbol_at_addr(i));
    }
  }
}

objArrayOop ConstantPool::resolved_references() const {
  return (objArrayOop)_cache->resolved_references();
}

// Called from outside constant pool resolution where a resolved_reference array
// may not be present.
objArrayOop ConstantPool::resolved_references_or_null() const {
  if (_cache == NULL) {
    return NULL;
  } else {
    return (objArrayOop)_cache->resolved_references();
  }
}

// Create resolved_references array and mapping array for original cp indexes
// The ldc bytecode was rewritten to have the resolved reference array index so need a way
// to map it back for resolving and some unlikely miscellaneous uses.
// The objects created by invokedynamic are appended to this list.
void ConstantPool::initialize_resolved_references(ClassLoaderData* loader_data,
                                                  const intStack& reference_map,
                                                  int constant_pool_map_length,
                                                  TRAPS) {
  // Initialized the resolved object cache.
  int map_length = reference_map.length();
  if (map_length > 0) {
    // Only need mapping back to constant pool entries.  The map isn't used for
    // invokedynamic resolved_reference entries.  For invokedynamic entries,
    // the constant pool cache index has the mapping back to both the constant
    // pool and to the resolved reference index.
    if (constant_pool_map_length > 0) {
      Array<u2>* om = MetadataFactory::new_array<u2>(loader_data, constant_pool_map_length, CHECK);

      for (int i = 0; i < constant_pool_map_length; i++) {
        int x = reference_map.at(i);
        assert(x == (int)(jushort) x, "klass index is too big");
        om->at_put(i, (jushort)x);
      }
      set_reference_map(om);
    }

    // Create Java array for holding resolved strings, methodHandles,
    // methodTypes, invokedynamic and invokehandle appendix objects, etc.
    objArrayOop stom = oopFactory::new_objArray(vmClasses::Object_klass(), map_length, CHECK);
    Handle refs_handle (THREAD, stom);  // must handleize.
    set_resolved_references(loader_data->add_handle(refs_handle));
  }
}

void ConstantPool::allocate_resolved_klasses(ClassLoaderData* loader_data, int num_klasses, TRAPS) {
  // A ConstantPool can't possibly have 0xffff valid class entries,
  // because entry #0 must be CONSTANT_Invalid, and each class entry must refer to a UTF8
  // entry for the class's name. So at most we will have 0xfffe class entries.
  // This allows us to use 0xffff (ConstantPool::_temp_resolved_klass_index) to indicate
  // UnresolvedKlass entries that are temporarily created during class redefinition.
  assert(num_klasses < CPKlassSlot::_temp_resolved_klass_index, "sanity");
  assert(resolved_klasses() == NULL, "sanity");
  Array<Klass*>* rk = MetadataFactory::new_array<Klass*>(loader_data, num_klasses, CHECK);
  set_resolved_klasses(rk);
}

void ConstantPool::initialize_unresolved_klasses(ClassLoaderData* loader_data, TRAPS) {
  int len = length();
  int num_klasses = 0;
  for (int i = 1; i <len; i++) {
    switch (tag_at(i).value()) {
    case JVM_CONSTANT_ClassIndex:
      {
        const int class_index = klass_index_at(i);
        unresolved_klass_at_put(i, class_index, num_klasses++);
      }
      break;
#ifndef PRODUCT
    case JVM_CONSTANT_Class:
    case JVM_CONSTANT_UnresolvedClass:
    case JVM_CONSTANT_UnresolvedClassInError:
      // All of these should have been reverted back to ClassIndex before calling
      // this function.
      ShouldNotReachHere();
#endif
    }
  }
  allocate_resolved_klasses(loader_data, num_klasses, THREAD);
}

// Hidden class support:
void ConstantPool::klass_at_put(int class_index, Klass* k) {
  assert(k != NULL, "must be valid klass");
  CPKlassSlot kslot = klass_slot_at(class_index);
  int resolved_klass_index = kslot.resolved_klass_index();
  Klass** adr = resolved_klasses()->adr_at(resolved_klass_index);
  Atomic::release_store(adr, k);

  // The interpreter assumes when the tag is stored, the klass is resolved
  // and the Klass* non-NULL, so we need hardware store ordering here.
  release_tag_at_put(class_index, JVM_CONSTANT_Class);
}

#if INCLUDE_CDS_JAVA_HEAP
// Archive the resolved references
void ConstantPool::archive_resolved_references() {
  if (_cache == NULL) {
    return; // nothing to do
  }

  InstanceKlass *ik = pool_holder();
  if (!(ik->is_shared_boot_class() || ik->is_shared_platform_class() ||
        ik->is_shared_app_class())) {
    // Archiving resolved references for classes from non-builtin loaders
    // is not yet supported.
    return;
  }

  objArrayOop rr = resolved_references();
  Array<u2>* ref_map = reference_map();
  if (rr != NULL) {
    int ref_map_len = ref_map == NULL ? 0 : ref_map->length();
    int rr_len = rr->length();
    for (int i = 0; i < rr_len; i++) {
      oop obj = rr->obj_at(i);
      rr->obj_at_put(i, NULL);
      if (obj != NULL && i < ref_map_len) {
        int index = object_to_cp_index(i);
        if (tag_at(index).is_string()) {
          oop archived_string = HeapShared::find_archived_heap_object(obj);
          // Update the reference to point to the archived copy
          // of this string.
          // If the string is too large to archive, NULL is
          // stored into rr. At run time, string_at_impl() will create and intern
          // the string.
          rr->obj_at_put(i, archived_string);
        }
      }
    }

    oop archived = HeapShared::archive_object(rr);
    // If the resolved references array is not archived (too large),
    // the 'archived' object is NULL. No need to explicitly check
    // the return value of archive_object() here. At runtime, the
    // resolved references will be created using the normal process
    // when there is no archived value.
    _cache->set_archived_references(archived);
  }
}

void ConstantPool::resolve_class_constants(TRAPS) {
  assert(DumpSharedSpaces, "used during dump time only");
  // The _cache may be NULL if the _pool_holder klass fails verification
  // at dump time due to missing dependencies.
  if (cache() == NULL || reference_map() == NULL) {
    return; // nothing to do
  }

  constantPoolHandle cp(THREAD, this);
  for (int index = 1; index < length(); index++) { // Index 0 is unused
    if (tag_at(index).is_string()) {
      int cache_index = cp->cp_to_object_index(index);
      string_at_impl(cp, index, cache_index, CHECK);
    }
  }
}

void ConstantPool::add_dumped_interned_strings() {
  objArrayOop rr = resolved_references();
  if (rr != NULL) {
    int rr_len = rr->length();
    for (int i = 0; i < rr_len; i++) {
      oop p = rr->obj_at(i);
      if (java_lang_String::is_instance(p)) {
        HeapShared::add_to_dumped_interned_strings(p);
      }
    }
  }
}
#endif

// CDS support. Create a new resolved_references array.
void ConstantPool::restore_unshareable_info(TRAPS) {
  if (!_pool_holder->is_linked() && !_pool_holder->is_rewritten()) {
    return;
  }
  assert(is_constantPool(), "ensure C++ vtable is restored");
  assert(on_stack(), "should always be set for shared constant pools");
  assert(is_shared(), "should always be set for shared constant pools");
  assert(_cache != NULL, "constant pool _cache should not be NULL");

  // Only create the new resolved references array if it hasn't been attempted before
  if (resolved_references() != NULL) return;

  // restore the C++ vtable from the shared archive
  restore_vtable();

  if (vmClasses::Object_klass_loaded()) {
    ClassLoaderData* loader_data = pool_holder()->class_loader_data();
#if INCLUDE_CDS_JAVA_HEAP
    if (HeapShared::open_regions_mapped() &&
        _cache->archived_references() != NULL) {
      oop archived = _cache->archived_references();
      // Create handle for the archived resolved reference array object
      Handle refs_handle(THREAD, archived);
      set_resolved_references(loader_data->add_handle(refs_handle));
      _cache->clear_archived_references();
    } else
#endif
    {
      // No mapped archived resolved reference array
      // Recreate the object array and add to ClassLoaderData.
      int map_length = resolved_reference_length();
      if (map_length > 0) {
        objArrayOop stom = oopFactory::new_objArray(vmClasses::Object_klass(), map_length, CHECK);
        Handle refs_handle(THREAD, stom);  // must handleize.
        set_resolved_references(loader_data->add_handle(refs_handle));
      }
    }
  }
}

void ConstantPool::remove_unshareable_info() {
  // Shared ConstantPools are in the RO region, so the _flags cannot be modified.
  // The _on_stack flag is used to prevent ConstantPools from deallocation during
  // class redefinition. Since shared ConstantPools cannot be deallocated anyway,
  // we always set _on_stack to true to avoid having to change _flags during runtime.
  _flags |= (_on_stack | _is_shared);

  if (!_pool_holder->is_linked() && !_pool_holder->verified_at_dump_time()) {
    return;
  }
  // Resolved references are not in the shared archive.
  // Save the length for restoration.  It is not necessarily the same length
  // as reference_map.length() if invokedynamic is saved. It is needed when
  // re-creating the resolved reference array if archived heap data cannot be map
  // at runtime.
  set_resolved_reference_length(
    resolved_references() != NULL ? resolved_references()->length() : 0);
  set_resolved_references(OopHandle());

  int num_klasses = 0;
  for (int index = 1; index < length(); index++) { // Index 0 is unused
    if (tag_at(index).is_unresolved_klass_in_error()) {
      tag_at_put(index, JVM_CONSTANT_UnresolvedClass);
    } else if (tag_at(index).is_method_handle_in_error()) {
      tag_at_put(index, JVM_CONSTANT_MethodHandle);
    } else if (tag_at(index).is_method_type_in_error()) {
      tag_at_put(index, JVM_CONSTANT_MethodType);
    } else if (tag_at(index).is_dynamic_constant_in_error()) {
      tag_at_put(index, JVM_CONSTANT_Dynamic);
    }
    if (tag_at(index).is_klass()) {
      // This class was resolved as a side effect of executing Java code
      // during dump time. We need to restore it back to an UnresolvedClass,
      // so that the proper class loading and initialization can happen
      // at runtime.
      bool clear_it = true;
      if (pool_holder()->is_hidden() && index == pool_holder()->this_class_index()) {
        // All references to a hidden class's own field/methods are through this
        // index. We cannot clear it. See comments in ClassFileParser::fill_instance_klass.
        clear_it = false;
      }
      if (clear_it) {
        CPKlassSlot kslot = klass_slot_at(index);
        int resolved_klass_index = kslot.resolved_klass_index();
        int name_index = kslot.name_index();
        assert(tag_at(name_index).is_symbol(), "sanity");
        resolved_klasses()->at_put(resolved_klass_index, NULL);
        tag_at_put(index, JVM_CONSTANT_UnresolvedClass);
        assert(klass_name_at(index) == symbol_at(name_index), "sanity");
      }
    }
  }
  if (cache() != NULL) {
    cache()->remove_unshareable_info();
  }
}

int ConstantPool::cp_to_object_index(int cp_index) {
  // this is harder don't do this so much.
  int i = reference_map()->find(cp_index);
  // We might not find the index for jsr292 call.
  return (i < 0) ? _no_index_sentinel : i;
}

void ConstantPool::string_at_put(int which, int obj_index, oop str) {
  resolved_references()->obj_at_put(obj_index, str);
}

void ConstantPool::trace_class_resolution(const constantPoolHandle& this_cp, Klass* k) {
  ResourceMark rm;
  int line_number = -1;
  const char * source_file = NULL;
  if (JavaThread::current()->has_last_Java_frame()) {
    // try to identify the method which called this function.
    vframeStream vfst(JavaThread::current());
    if (!vfst.at_end()) {
      line_number = vfst.method()->line_number_from_bci(vfst.bci());
      Symbol* s = vfst.method()->method_holder()->source_file_name();
      if (s != NULL) {
        source_file = s->as_C_string();
      }
    }
  }
  if (k != this_cp->pool_holder()) {
    // only print something if the classes are different
    if (source_file != NULL) {
      log_debug(class, resolve)("%s %s %s:%d",
                 this_cp->pool_holder()->external_name(),
                 k->external_name(), source_file, line_number);
    } else {
      log_debug(class, resolve)("%s %s",
                 this_cp->pool_holder()->external_name(),
                 k->external_name());
    }
  }
}

Klass* ConstantPool::klass_at_impl(const constantPoolHandle& this_cp, int which,
                                   TRAPS) {
  JavaThread* javaThread = THREAD;

  // A resolved constantPool entry will contain a Klass*, otherwise a Symbol*.
  // It is not safe to rely on the tag bit's here, since we don't have a lock, and
  // the entry and tag is not updated atomicly.
  CPKlassSlot kslot = this_cp->klass_slot_at(which);
  int resolved_klass_index = kslot.resolved_klass_index();
  int name_index = kslot.name_index();
  assert(this_cp->tag_at(name_index).is_symbol(), "sanity");

  // The tag must be JVM_CONSTANT_Class in order to read the correct value from
  // the unresolved_klasses() array.
  if (this_cp->tag_at(which).is_klass()) {
    Klass* klass = this_cp->resolved_klasses()->at(resolved_klass_index);
    if (klass != NULL) {
      return klass;
    }
  }

  // This tag doesn't change back to unresolved class unless at a safepoint.
  if (this_cp->tag_at(which).is_unresolved_klass_in_error()) {
    // The original attempt to resolve this constant pool entry failed so find the
    // class of the original error and throw another error of the same class
    // (JVMS 5.4.3).
    // If there is a detail message, pass that detail message to the error.
    // The JVMS does not strictly require us to duplicate the same detail message,
    // or any internal exception fields such as cause or stacktrace.  But since the
    // detail message is often a class name or other literal string, we will repeat it
    // if we can find it in the symbol table.
    throw_resolution_error(this_cp, which, CHECK_NULL);
    ShouldNotReachHere();
  }

  Handle mirror_handle;
  Symbol* name = this_cp->symbol_at(name_index);
  Handle loader (THREAD, this_cp->pool_holder()->class_loader());
  Handle protection_domain (THREAD, this_cp->pool_holder()->protection_domain());

  Klass* k;
  {
    // Turn off the single stepping while doing class resolution
    JvmtiHideSingleStepping jhss(javaThread);
    k = SystemDictionary::resolve_or_fail(name, loader, protection_domain, true, THREAD);
  } //  JvmtiHideSingleStepping jhss(javaThread);

  if (!HAS_PENDING_EXCEPTION) {
    // preserve the resolved klass from unloading
    mirror_handle = Handle(THREAD, k->java_mirror());
    // Do access check for klasses
    verify_constant_pool_resolve(this_cp, k, THREAD);
  }

  // Failed to resolve class. We must record the errors so that subsequent attempts
  // to resolve this constant pool entry fail with the same error (JVMS 5.4.3).
  if (HAS_PENDING_EXCEPTION) {
    save_and_throw_exception(this_cp, which, constantTag(JVM_CONSTANT_UnresolvedClass), CHECK_NULL);
    // If CHECK_NULL above doesn't return the exception, that means that
    // some other thread has beaten us and has resolved the class.
    // To preserve old behavior, we return the resolved class.
    Klass* klass = this_cp->resolved_klasses()->at(resolved_klass_index);
    assert(klass != NULL, "must be resolved if exception was cleared");
    return klass;
  }

  // logging for class+resolve.
  if (log_is_enabled(Debug, class, resolve)){
    trace_class_resolution(this_cp, k);
  }

  Klass** adr = this_cp->resolved_klasses()->adr_at(resolved_klass_index);
  Atomic::release_store(adr, k);
  // The interpreter assumes when the tag is stored, the klass is resolved
  // and the Klass* stored in _resolved_klasses is non-NULL, so we need
  // hardware store ordering here.
  // We also need to CAS to not overwrite an error from a racing thread.

  jbyte old_tag = Atomic::cmpxchg((jbyte*)this_cp->tag_addr_at(which),
                                  (jbyte)JVM_CONSTANT_UnresolvedClass,
                                  (jbyte)JVM_CONSTANT_Class);

  // We need to recheck exceptions from racing thread and return the same.
  if (old_tag == JVM_CONSTANT_UnresolvedClassInError) {
    // Remove klass.
    this_cp->resolved_klasses()->at_put(resolved_klass_index, NULL);
    throw_resolution_error(this_cp, which, CHECK_NULL);
  }

  return k;
}


// Does not update ConstantPool* - to avoid any exception throwing. Used
// by compiler and exception handling.  Also used to avoid classloads for
// instanceof operations. Returns NULL if the class has not been loaded or
// if the verification of constant pool failed
Klass* ConstantPool::klass_at_if_loaded(const constantPoolHandle& this_cp, int which) {
  CPKlassSlot kslot = this_cp->klass_slot_at(which);
  int resolved_klass_index = kslot.resolved_klass_index();
  int name_index = kslot.name_index();
  assert(this_cp->tag_at(name_index).is_symbol(), "sanity");

  if (this_cp->tag_at(which).is_klass()) {
    Klass* k = this_cp->resolved_klasses()->at(resolved_klass_index);
    assert(k != NULL, "should be resolved");
    return k;
  } else if (this_cp->tag_at(which).is_unresolved_klass_in_error()) {
    return NULL;
  } else {
    Thread* current = Thread::current();
    Symbol* name = this_cp->symbol_at(name_index);
    oop loader = this_cp->pool_holder()->class_loader();
    oop protection_domain = this_cp->pool_holder()->protection_domain();
    Handle h_prot (current, protection_domain);
    Handle h_loader (current, loader);
    Klass* k = SystemDictionary::find_instance_klass(name, h_loader, h_prot);

    // Avoid constant pool verification at a safepoint, as it takes the Module_lock.
    if (k != NULL && current->is_Java_thread()) {
      // Make sure that resolving is legal
      JavaThread* THREAD = JavaThread::cast(current); // For exception macros.
      ExceptionMark em(THREAD);
      // return NULL if verification fails
      verify_constant_pool_resolve(this_cp, k, THREAD);
      if (HAS_PENDING_EXCEPTION) {
        CLEAR_PENDING_EXCEPTION;
        return NULL;
      }
      return k;
    } else {
      return k;
    }
  }
}

Method* ConstantPool::method_at_if_loaded(const constantPoolHandle& cpool,
                                                   int which) {
  if (cpool->cache() == NULL)  return NULL;  // nothing to load yet
  int cache_index = decode_cpcache_index(which, true);
  if (!(cache_index >= 0 && cache_index < cpool->cache()->length())) {
    // FIXME: should be an assert
    log_debug(class, resolve)("bad operand %d in:", which); cpool->print();
    return NULL;
  }
  ConstantPoolCacheEntry* e = cpool->cache()->entry_at(cache_index);
  return e->method_if_resolved(cpool);
}


bool ConstantPool::has_appendix_at_if_loaded(const constantPoolHandle& cpool, int which) {
  if (cpool->cache() == NULL)  return false;  // nothing to load yet
  int cache_index = decode_cpcache_index(which, true);
  ConstantPoolCacheEntry* e = cpool->cache()->entry_at(cache_index);
  return e->has_appendix();
}

oop ConstantPool::appendix_at_if_loaded(const constantPoolHandle& cpool, int which) {
  if (cpool->cache() == NULL)  return NULL;  // nothing to load yet
  int cache_index = decode_cpcache_index(which, true);
  ConstantPoolCacheEntry* e = cpool->cache()->entry_at(cache_index);
  return e->appendix_if_resolved(cpool);
}


bool ConstantPool::has_local_signature_at_if_loaded(const constantPoolHandle& cpool, int which) {
  if (cpool->cache() == NULL)  return false;  // nothing to load yet
  int cache_index = decode_cpcache_index(which, true);
  ConstantPoolCacheEntry* e = cpool->cache()->entry_at(cache_index);
  return e->has_local_signature();
}

Symbol* ConstantPool::impl_name_ref_at(int which, bool uncached) {
  int name_index = name_ref_index_at(impl_name_and_type_ref_index_at(which, uncached));
  return symbol_at(name_index);
}


Symbol* ConstantPool::impl_signature_ref_at(int which, bool uncached) {
  int signature_index = signature_ref_index_at(impl_name_and_type_ref_index_at(which, uncached));
  return symbol_at(signature_index);
}

int ConstantPool::impl_name_and_type_ref_index_at(int which, bool uncached) {
  int i = which;
  if (!uncached && cache() != NULL) {
    if (ConstantPool::is_invokedynamic_index(which)) {
      // Invokedynamic index is index into the constant pool cache
      int pool_index = invokedynamic_bootstrap_ref_index_at(which);
      pool_index = bootstrap_name_and_type_ref_index_at(pool_index);
      assert(tag_at(pool_index).is_name_and_type(), "");
      return pool_index;
    }
    // change byte-ordering and go via cache
    i = remap_instruction_operand_from_cache(which);
  } else {
    if (tag_at(which).has_bootstrap()) {
      int pool_index = bootstrap_name_and_type_ref_index_at(which);
      assert(tag_at(pool_index).is_name_and_type(), "");
      return pool_index;
    }
  }
  assert(tag_at(i).is_field_or_method(), "Corrupted constant pool");
  assert(!tag_at(i).has_bootstrap(), "Must be handled above");
  jint ref_index = *int_at_addr(i);
  return extract_high_short_from_int(ref_index);
}

constantTag ConstantPool::impl_tag_ref_at(int which, bool uncached) {
  int pool_index = which;
  if (!uncached && cache() != NULL) {
    if (ConstantPool::is_invokedynamic_index(which)) {
      // Invokedynamic index is index into resolved_references
      pool_index = invokedynamic_bootstrap_ref_index_at(which);
    } else {
      // change byte-ordering and go via cache
      pool_index = remap_instruction_operand_from_cache(which);
    }
  }
  return tag_at(pool_index);
}

int ConstantPool::impl_klass_ref_index_at(int which, bool uncached) {
  guarantee(!ConstantPool::is_invokedynamic_index(which),
            "an invokedynamic instruction does not have a klass");
  int i = which;
  if (!uncached && cache() != NULL) {
    // change byte-ordering and go via cache
    i = remap_instruction_operand_from_cache(which);
  }
  assert(tag_at(i).is_field_or_method(), "Corrupted constant pool");
  jint ref_index = *int_at_addr(i);
  return extract_low_short_from_int(ref_index);
}



int ConstantPool::remap_instruction_operand_from_cache(int operand) {
  int cpc_index = operand;
  DEBUG_ONLY(cpc_index -= CPCACHE_INDEX_TAG);
  assert((int)(u2)cpc_index == cpc_index, "clean u2");
  int member_index = cache()->entry_at(cpc_index)->constant_pool_index();
  return member_index;
}


void ConstantPool::verify_constant_pool_resolve(const constantPoolHandle& this_cp, Klass* k, TRAPS) {
  if (!(k->is_instance_klass() || k->is_objArray_klass())) {
    return;  // short cut, typeArray klass is always accessible
  }
  Klass* holder = this_cp->pool_holder();
  LinkResolver::check_klass_accessibility(holder, k, CHECK);
}


int ConstantPool::name_ref_index_at(int which_nt) {
  jint ref_index = name_and_type_at(which_nt);
  return extract_low_short_from_int(ref_index);
}


int ConstantPool::signature_ref_index_at(int which_nt) {
  jint ref_index = name_and_type_at(which_nt);
  return extract_high_short_from_int(ref_index);
}


Klass* ConstantPool::klass_ref_at(int which, TRAPS) {
  return klass_at(klass_ref_index_at(which), THREAD);
}

Symbol* ConstantPool::klass_name_at(int which) const {
  return symbol_at(klass_slot_at(which).name_index());
}

Symbol* ConstantPool::klass_ref_at_noresolve(int which) {
  jint ref_index = klass_ref_index_at(which);
  return klass_at_noresolve(ref_index);
}

Symbol* ConstantPool::uncached_klass_ref_at_noresolve(int which) {
  jint ref_index = uncached_klass_ref_index_at(which);
  return klass_at_noresolve(ref_index);
}

char* ConstantPool::string_at_noresolve(int which) {
  return unresolved_string_at(which)->as_C_string();
}

BasicType ConstantPool::basic_type_for_signature_at(int which) const {
  return Signature::basic_type(symbol_at(which));
}


void ConstantPool::resolve_string_constants_impl(const constantPoolHandle& this_cp, TRAPS) {
  for (int index = 1; index < this_cp->length(); index++) { // Index 0 is unused
    if (this_cp->tag_at(index).is_string()) {
      this_cp->string_at(index, CHECK);
    }
  }
}

static Symbol* exception_message(const constantPoolHandle& this_cp, int which, constantTag tag, oop pending_exception) {
  // Dig out the detailed message to reuse if possible
  Symbol* message = java_lang_Throwable::detail_message(pending_exception);
  if (message != NULL) {
    return message;
  }

  // Return specific message for the tag
  switch (tag.value()) {
  case JVM_CONSTANT_UnresolvedClass:
    // return the class name in the error message
    message = this_cp->klass_name_at(which);
    break;
  case JVM_CONSTANT_MethodHandle:
    // return the method handle name in the error message
    message = this_cp->method_handle_name_ref_at(which);
    break;
  case JVM_CONSTANT_MethodType:
    // return the method type signature in the error message
    message = this_cp->method_type_signature_at(which);
    break;
  case JVM_CONSTANT_Dynamic:
    // return the name of the condy in the error message
    message = this_cp->uncached_name_ref_at(which);
    break;
  default:
    ShouldNotReachHere();
  }

  return message;
}

static void add_resolution_error(const constantPoolHandle& this_cp, int which,
                                 constantTag tag, oop pending_exception) {

  Symbol* error = pending_exception->klass()->name();
  oop cause = java_lang_Throwable::cause(pending_exception);

  // Also dig out the exception cause, if present.
  Symbol* cause_sym = NULL;
  Symbol* cause_msg = NULL;
  if (cause != NULL && cause != pending_exception) {
    cause_sym = cause->klass()->name();
    cause_msg = java_lang_Throwable::detail_message(cause);
  }

  Symbol* message = exception_message(this_cp, which, tag, pending_exception);
  SystemDictionary::add_resolution_error(this_cp, which, error, message, cause_sym, cause_msg);
}


void ConstantPool::throw_resolution_error(const constantPoolHandle& this_cp, int which, TRAPS) {
  ResourceMark rm(THREAD);
  Symbol* message = NULL;
  Symbol* cause = NULL;
  Symbol* cause_msg = NULL;
  Symbol* error = SystemDictionary::find_resolution_error(this_cp, which, &message, &cause, &cause_msg);
  assert(error != NULL, "checking");
  const char* cause_str = cause_msg != NULL ? cause_msg->as_C_string() : NULL;

  CLEAR_PENDING_EXCEPTION;
  if (message != NULL) {
    char* msg = message->as_C_string();
    if (cause != NULL) {
      Handle h_cause = Exceptions::new_exception(THREAD, cause, cause_str);
      THROW_MSG_CAUSE(error, msg, h_cause);
    } else {
      THROW_MSG(error, msg);
    }
  } else {
    if (cause != NULL) {
      Handle h_cause = Exceptions::new_exception(THREAD, cause, cause_str);
      THROW_CAUSE(error, h_cause);
    } else {
      THROW(error);
    }
  }
}

// If resolution for Class, Dynamic constant, MethodHandle or MethodType fails, save the
// exception in the resolution error table, so that the same exception is thrown again.
void ConstantPool::save_and_throw_exception(const constantPoolHandle& this_cp, int which,
                                            constantTag tag, TRAPS) {

  int error_tag = tag.error_value();

  if (!PENDING_EXCEPTION->
    is_a(vmClasses::LinkageError_klass())) {
    // Just throw the exception and don't prevent these classes from
    // being loaded due to virtual machine errors like StackOverflow
    // and OutOfMemoryError, etc, or if the thread was hit by stop()
    // Needs clarification to section 5.4.3 of the VM spec (see 6308271)
  } else if (this_cp->tag_at(which).value() != error_tag) {
    add_resolution_error(this_cp, which, tag, PENDING_EXCEPTION);
    // CAS in the tag.  If a thread beat us to registering this error that's fine.
    // If another thread resolved the reference, this is a race condition. This
    // thread may have had a security manager or something temporary.
    // This doesn't deterministically get an error.   So why do we save this?
    // We save this because jvmti can add classes to the bootclass path after
    // this error, so it needs to get the same error if the error is first.
    jbyte old_tag = Atomic::cmpxchg((jbyte*)this_cp->tag_addr_at(which),
                                    (jbyte)tag.value(),
                                    (jbyte)error_tag);
    if (old_tag != error_tag && old_tag != tag.value()) {
      // MethodHandles and MethodType doesn't change to resolved version.
      assert(this_cp->tag_at(which).is_klass(), "Wrong tag value");
      // Forget the exception and use the resolved class.
      CLEAR_PENDING_EXCEPTION;
    }
  } else {
    // some other thread put this in error state
    throw_resolution_error(this_cp, which, CHECK);
  }
}

constantTag ConstantPool::constant_tag_at(int which) {
  constantTag tag = tag_at(which);
  if (tag.is_dynamic_constant() ||
      tag.is_dynamic_constant_in_error()) {
    BasicType bt = basic_type_for_constant_at(which);
    // dynamic constant could return an array, treat as object
    return constantTag::ofBasicType(is_reference_type(bt) ? T_OBJECT : bt);
  }
  return tag;
}

BasicType ConstantPool::basic_type_for_constant_at(int which) {
  constantTag tag = tag_at(which);
  if (tag.is_dynamic_constant() ||
      tag.is_dynamic_constant_in_error()) {
    // have to look at the signature for this one
    Symbol* constant_type = uncached_signature_ref_at(which);
    return Signature::basic_type(constant_type);
  }
  return tag.basic_type();
}

// Called to resolve constants in the constant pool and return an oop.
// Some constant pool entries cache their resolved oop. This is also
// called to create oops from constants to use in arguments for invokedynamic
oop ConstantPool::resolve_constant_at_impl(const constantPoolHandle& this_cp,
                                           int index, int cache_index,
                                           bool* status_return, TRAPS) {
  oop result_oop = NULL;
  Handle throw_exception;

  if (cache_index == _possible_index_sentinel) {
    // It is possible that this constant is one which is cached in the objects.
    // We'll do a linear search.  This should be OK because this usage is rare.
    // FIXME: If bootstrap specifiers stress this code, consider putting in
    // a reverse index.  Binary search over a short array should do it.
    assert(index > 0, "valid index");
    cache_index = this_cp->cp_to_object_index(index);
  }
  assert(cache_index == _no_index_sentinel || cache_index >= 0, "");
  assert(index == _no_index_sentinel || index >= 0, "");

  if (cache_index >= 0) {
    result_oop = this_cp->resolved_references()->obj_at(cache_index);
    if (result_oop != NULL) {
      if (result_oop == Universe::the_null_sentinel()) {
        DEBUG_ONLY(int temp_index = (index >= 0 ? index : this_cp->object_to_cp_index(cache_index)));
        assert(this_cp->tag_at(temp_index).is_dynamic_constant(), "only condy uses the null sentinel");
        result_oop = NULL;
      }
      if (status_return != NULL)  (*status_return) = true;
      return result_oop;
      // That was easy...
    }
    index = this_cp->object_to_cp_index(cache_index);
  }

  jvalue prim_value;  // temp used only in a few cases below

  constantTag tag = this_cp->tag_at(index);

  if (status_return != NULL) {
    // don't trigger resolution if the constant might need it
    switch (tag.value()) {
    case JVM_CONSTANT_Class:
    {
      CPKlassSlot kslot = this_cp->klass_slot_at(index);
      int resolved_klass_index = kslot.resolved_klass_index();
      if (this_cp->resolved_klasses()->at(resolved_klass_index) == NULL) {
        (*status_return) = false;
        return NULL;
      }
      // the klass is waiting in the CP; go get it
      break;
    }
    case JVM_CONSTANT_String:
    case JVM_CONSTANT_Integer:
    case JVM_CONSTANT_Float:
    case JVM_CONSTANT_Long:
    case JVM_CONSTANT_Double:
      // these guys trigger OOM at worst
      break;
    default:
      (*status_return) = false;
      return NULL;
    }
    // from now on there is either success or an OOME
    (*status_return) = true;
  }

  switch (tag.value()) {

  case JVM_CONSTANT_UnresolvedClass:
  case JVM_CONSTANT_UnresolvedClassInError:
  case JVM_CONSTANT_Class:
    {
      assert(cache_index == _no_index_sentinel, "should not have been set");
      Klass* resolved = klass_at_impl(this_cp, index, CHECK_NULL);
      // ldc wants the java mirror.
      result_oop = resolved->java_mirror();
      break;
    }

  case JVM_CONSTANT_Dynamic:
    {
      // Resolve the Dynamically-Computed constant to invoke the BSM in order to obtain the resulting oop.
      BootstrapInfo bootstrap_specifier(this_cp, index);

      // The initial step in resolving an unresolved symbolic reference to a
      // dynamically-computed constant is to resolve the symbolic reference to a
      // method handle which will be the bootstrap method for the dynamically-computed
      // constant. If resolution of the java.lang.invoke.MethodHandle for the bootstrap
      // method fails, then a MethodHandleInError is stored at the corresponding
      // bootstrap method's CP index for the CONSTANT_MethodHandle_info. No need to
      // set a DynamicConstantInError here since any subsequent use of this
      // bootstrap method will encounter the resolution of MethodHandleInError.
      // Both the first, (resolution of the BSM and its static arguments), and the second tasks,
      // (invocation of the BSM), of JVMS Section 5.4.3.6 occur within invoke_bootstrap_method()
      // for the bootstrap_specifier created above.
      SystemDictionary::invoke_bootstrap_method(bootstrap_specifier, THREAD);
      Exceptions::wrap_dynamic_exception(/* is_indy */ false, THREAD);
      if (HAS_PENDING_EXCEPTION) {
        // Resolution failure of the dynamically-computed constant, save_and_throw_exception
        // will check for a LinkageError and store a DynamicConstantInError.
        save_and_throw_exception(this_cp, index, tag, CHECK_NULL);
      }
      result_oop = bootstrap_specifier.resolved_value()();
      BasicType type = Signature::basic_type(bootstrap_specifier.signature());
      if (!is_reference_type(type)) {
        // Make sure the primitive value is properly boxed.
        // This is a JDK responsibility.
        const char* fail = NULL;
        if (result_oop == NULL) {
          fail = "null result instead of box";
        } else if (!is_java_primitive(type)) {
          // FIXME: support value types via unboxing
          fail = "can only handle references and primitives";
        } else if (!java_lang_boxing_object::is_instance(result_oop, type)) {
          fail = "primitive is not properly boxed";
        }
        if (fail != NULL) {
          // Since this exception is not a LinkageError, throw exception
          // but do not save a DynamicInError resolution result.
          // See section 5.4.3 of the VM spec.
          THROW_MSG_NULL(vmSymbols::java_lang_InternalError(), fail);
        }
      }

      LogTarget(Debug, methodhandles, condy) lt_condy;
      if (lt_condy.is_enabled()) {
        LogStream ls(lt_condy);
        bootstrap_specifier.print_msg_on(&ls, "resolve_constant_at_impl");
      }
      break;
    }

  case JVM_CONSTANT_String:
    assert(cache_index != _no_index_sentinel, "should have been set");
    result_oop = string_at_impl(this_cp, index, cache_index, CHECK_NULL);
    break;

  case JVM_CONSTANT_DynamicInError:
  case JVM_CONSTANT_MethodHandleInError:
  case JVM_CONSTANT_MethodTypeInError:
    {
      throw_resolution_error(this_cp, index, CHECK_NULL);
      break;
    }

  case JVM_CONSTANT_MethodHandle:
    {
      int ref_kind                 = this_cp->method_handle_ref_kind_at(index);
      int callee_index             = this_cp->method_handle_klass_index_at(index);
      Symbol*  name =      this_cp->method_handle_name_ref_at(index);
      Symbol*  signature = this_cp->method_handle_signature_ref_at(index);
      constantTag m_tag  = this_cp->tag_at(this_cp->method_handle_index_at(index));
      { ResourceMark rm(THREAD);
        log_debug(class, resolve)("resolve JVM_CONSTANT_MethodHandle:%d [%d/%d/%d] %s.%s",
                              ref_kind, index, this_cp->method_handle_index_at(index),
                              callee_index, name->as_C_string(), signature->as_C_string());
      }

      Klass* callee = klass_at_impl(this_cp, callee_index, CHECK_NULL);

      // Check constant pool method consistency
      if ((callee->is_interface() && m_tag.is_method()) ||
          ((!callee->is_interface() && m_tag.is_interface_method()))) {
        ResourceMark rm(THREAD);
        stringStream ss;
        ss.print("Inconsistent constant pool data in classfile for class %s. "
                 "Method '", callee->name()->as_C_string());
        signature->print_as_signature_external_return_type(&ss);
        ss.print(" %s(", name->as_C_string());
        signature->print_as_signature_external_parameters(&ss);
        ss.print(")' at index %d is %s and should be %s",
                 index,
                 callee->is_interface() ? "CONSTANT_MethodRef" : "CONSTANT_InterfaceMethodRef",
                 callee->is_interface() ? "CONSTANT_InterfaceMethodRef" : "CONSTANT_MethodRef");
        THROW_MSG_NULL(vmSymbols::java_lang_IncompatibleClassChangeError(), ss.as_string());
      }

      Klass* klass = this_cp->pool_holder();
      Handle value = SystemDictionary::link_method_handle_constant(klass, ref_kind,
                                                                   callee, name, signature,
                                                                   THREAD);
      result_oop = value();
      if (HAS_PENDING_EXCEPTION) {
        save_and_throw_exception(this_cp, index, tag, CHECK_NULL);
      }
      break;
    }

  case JVM_CONSTANT_MethodType:
    {
      Symbol*  signature = this_cp->method_type_signature_at(index);
      { ResourceMark rm(THREAD);
        log_debug(class, resolve)("resolve JVM_CONSTANT_MethodType [%d/%d] %s",
                              index, this_cp->method_type_index_at(index),
                              signature->as_C_string());
      }
      Klass* klass = this_cp->pool_holder();
      Handle value = SystemDictionary::find_method_handle_type(signature, klass, THREAD);
      result_oop = value();
      if (HAS_PENDING_EXCEPTION) {
        save_and_throw_exception(this_cp, index, tag, CHECK_NULL);
      }
      break;
    }

  case JVM_CONSTANT_Integer:
    assert(cache_index == _no_index_sentinel, "should not have been set");
    prim_value.i = this_cp->int_at(index);
    result_oop = java_lang_boxing_object::create(T_INT, &prim_value, CHECK_NULL);
    break;

  case JVM_CONSTANT_Float:
    assert(cache_index == _no_index_sentinel, "should not have been set");
    prim_value.f = this_cp->float_at(index);
    result_oop = java_lang_boxing_object::create(T_FLOAT, &prim_value, CHECK_NULL);
    break;

  case JVM_CONSTANT_Long:
    assert(cache_index == _no_index_sentinel, "should not have been set");
    prim_value.j = this_cp->long_at(index);
    result_oop = java_lang_boxing_object::create(T_LONG, &prim_value, CHECK_NULL);
    break;

  case JVM_CONSTANT_Double:
    assert(cache_index == _no_index_sentinel, "should not have been set");
    prim_value.d = this_cp->double_at(index);
    result_oop = java_lang_boxing_object::create(T_DOUBLE, &prim_value, CHECK_NULL);
    break;

  default:
    DEBUG_ONLY( tty->print_cr("*** %p: tag at CP[%d/%d] = %d",
                              this_cp(), index, cache_index, tag.value()));
    assert(false, "unexpected constant tag");
    break;
  }

  if (cache_index >= 0) {
    // Benign race condition:  resolved_references may already be filled in.
    // The important thing here is that all threads pick up the same result.
    // It doesn't matter which racing thread wins, as long as only one
    // result is used by all threads, and all future queries.
    oop new_result = (result_oop == NULL ? Universe::the_null_sentinel() : result_oop);
    oop old_result = this_cp->resolved_references()
      ->atomic_compare_exchange_oop(cache_index, new_result, NULL);
    if (old_result == NULL) {
      return result_oop;  // was installed
    } else {
      // Return the winning thread's result.  This can be different than
      // the result here for MethodHandles.
      if (old_result == Universe::the_null_sentinel())
        old_result = NULL;
      return old_result;
    }
  } else {
    assert(result_oop != Universe::the_null_sentinel(), "");
    return result_oop;
  }
}

oop ConstantPool::uncached_string_at(int which, TRAPS) {
  Symbol* sym = unresolved_string_at(which);
  oop str = StringTable::intern(sym, CHECK_(NULL));
  assert(java_lang_String::is_instance(str), "must be string");
  return str;
}

void ConstantPool::copy_bootstrap_arguments_at_impl(const constantPoolHandle& this_cp, int index,
                                                    int start_arg, int end_arg,
                                                    objArrayHandle info, int pos,
                                                    bool must_resolve, Handle if_not_available,
                                                    TRAPS) {
  int argc;
  int limit = pos + end_arg - start_arg;
  // checks: index in range [0..this_cp->length),
  // tag at index, start..end in range [0..argc],
  // info array non-null, pos..limit in [0..info.length]
  if ((0 >= index    || index >= this_cp->length())  ||
      !(this_cp->tag_at(index).is_invoke_dynamic()    ||
        this_cp->tag_at(index).is_dynamic_constant()) ||
      (0 > start_arg || start_arg > end_arg) ||
      (end_arg > (argc = this_cp->bootstrap_argument_count_at(index))) ||
      (0 > pos       || pos > limit)         ||
      (info.is_null() || limit > info->length())) {
    // An index or something else went wrong; throw an error.
    // Since this is an internal API, we don't expect this,
    // so we don't bother to craft a nice message.
    THROW_MSG(vmSymbols::java_lang_LinkageError(), "bad BSM argument access");
  }
  // now we can loop safely
  int info_i = pos;
  for (int i = start_arg; i < end_arg; i++) {
    int arg_index = this_cp->bootstrap_argument_index_at(index, i);
    oop arg_oop;
    if (must_resolve) {
      arg_oop = this_cp->resolve_possibly_cached_constant_at(arg_index, CHECK);
    } else {
      bool found_it = false;
      arg_oop = this_cp->find_cached_constant_at(arg_index, found_it, CHECK);
      if (!found_it)  arg_oop = if_not_available();
    }
    info->obj_at_put(info_i++, arg_oop);
  }
}

oop ConstantPool::string_at_impl(const constantPoolHandle& this_cp, int which, int obj_index, TRAPS) {
  // If the string has already been interned, this entry will be non-null
  oop str = this_cp->resolved_references()->obj_at(obj_index);
  assert(str != Universe::the_null_sentinel(), "");
  if (str != NULL) return str;
  Symbol* sym = this_cp->unresolved_string_at(which);
  str = StringTable::intern(sym, CHECK_(NULL));
  this_cp->string_at_put(which, obj_index, str);
  assert(java_lang_String::is_instance(str), "must be string");
  return str;
}


bool ConstantPool::klass_name_at_matches(const InstanceKlass* k, int which) {
  // Names are interned, so we can compare Symbol*s directly
  Symbol* cp_name = klass_name_at(which);
  return (cp_name == k->name());
}


// Iterate over symbols and decrement ones which are Symbol*s
// This is done during GC.
// Only decrement the UTF8 symbols. Strings point to
// these symbols but didn't increment the reference count.
void ConstantPool::unreference_symbols() {
  for (int index = 1; index < length(); index++) { // Index 0 is unused
    constantTag tag = tag_at(index);
    if (tag.is_symbol()) {
      symbol_at(index)->decrement_refcount();
    }
  }
}


// Compare this constant pool's entry at index1 to the constant pool
// cp2's entry at index2.
bool ConstantPool::compare_entry_to(int index1, const constantPoolHandle& cp2,
       int index2) {

  // The error tags are equivalent to non-error tags when comparing
  jbyte t1 = tag_at(index1).non_error_value();
  jbyte t2 = cp2->tag_at(index2).non_error_value();

  if (t1 != t2) {
    // Not the same entry type so there is nothing else to check. Note
    // that this style of checking will consider resolved/unresolved
    // class pairs as different.
    // From the ConstantPool* API point of view, this is correct
    // behavior. See VM_RedefineClasses::merge_constant_pools() to see how this
    // plays out in the context of ConstantPool* merging.
    return false;
  }

  switch (t1) {
  case JVM_CONSTANT_Class:
  {
    Klass* k1 = resolved_klass_at(index1);
    Klass* k2 = cp2->resolved_klass_at(index2);
    if (k1 == k2) {
      return true;
    }
  } break;

  case JVM_CONSTANT_ClassIndex:
  {
    int recur1 = klass_index_at(index1);
    int recur2 = cp2->klass_index_at(index2);
    if (compare_entry_to(recur1, cp2, recur2)) {
      return true;
    }
  } break;

  case JVM_CONSTANT_Double:
  {
    jdouble d1 = double_at(index1);
    jdouble d2 = cp2->double_at(index2);
    if (d1 == d2) {
      return true;
    }
  } break;

  case JVM_CONSTANT_Fieldref:
  case JVM_CONSTANT_InterfaceMethodref:
  case JVM_CONSTANT_Methodref:
  {
    int recur1 = uncached_klass_ref_index_at(index1);
    int recur2 = cp2->uncached_klass_ref_index_at(index2);
    bool match = compare_entry_to(recur1, cp2, recur2);
    if (match) {
      recur1 = uncached_name_and_type_ref_index_at(index1);
      recur2 = cp2->uncached_name_and_type_ref_index_at(index2);
      if (compare_entry_to(recur1, cp2, recur2)) {
        return true;
      }
    }
  } break;

  case JVM_CONSTANT_Float:
  {
    jfloat f1 = float_at(index1);
    jfloat f2 = cp2->float_at(index2);
    if (f1 == f2) {
      return true;
    }
  } break;

  case JVM_CONSTANT_Integer:
  {
    jint i1 = int_at(index1);
    jint i2 = cp2->int_at(index2);
    if (i1 == i2) {
      return true;
    }
  } break;

  case JVM_CONSTANT_Long:
  {
    jlong l1 = long_at(index1);
    jlong l2 = cp2->long_at(index2);
    if (l1 == l2) {
      return true;
    }
  } break;

  case JVM_CONSTANT_NameAndType:
  {
    int recur1 = name_ref_index_at(index1);
    int recur2 = cp2->name_ref_index_at(index2);
    if (compare_entry_to(recur1, cp2, recur2)) {
      recur1 = signature_ref_index_at(index1);
      recur2 = cp2->signature_ref_index_at(index2);
      if (compare_entry_to(recur1, cp2, recur2)) {
        return true;
      }
    }
  } break;

  case JVM_CONSTANT_StringIndex:
  {
    int recur1 = string_index_at(index1);
    int recur2 = cp2->string_index_at(index2);
    if (compare_entry_to(recur1, cp2, recur2)) {
      return true;
    }
  } break;

  case JVM_CONSTANT_UnresolvedClass:
  {
    Symbol* k1 = klass_name_at(index1);
    Symbol* k2 = cp2->klass_name_at(index2);
    if (k1 == k2) {
      return true;
    }
  } break;

  case JVM_CONSTANT_MethodType:
  {
    int k1 = method_type_index_at(index1);
    int k2 = cp2->method_type_index_at(index2);
    if (compare_entry_to(k1, cp2, k2)) {
      return true;
    }
  } break;

  case JVM_CONSTANT_MethodHandle:
  {
    int k1 = method_handle_ref_kind_at(index1);
    int k2 = cp2->method_handle_ref_kind_at(index2);
    if (k1 == k2) {
      int i1 = method_handle_index_at(index1);
      int i2 = cp2->method_handle_index_at(index2);
      if (compare_entry_to(i1, cp2, i2)) {
        return true;
      }
    }
  } break;

  case JVM_CONSTANT_Dynamic:
  {
    int k1 = bootstrap_name_and_type_ref_index_at(index1);
    int k2 = cp2->bootstrap_name_and_type_ref_index_at(index2);
    int i1 = bootstrap_methods_attribute_index(index1);
    int i2 = cp2->bootstrap_methods_attribute_index(index2);
    bool match_entry = compare_entry_to(k1, cp2, k2);
    bool match_operand = compare_operand_to(i1, cp2, i2);
    return (match_entry && match_operand);
  } break;

  case JVM_CONSTANT_InvokeDynamic:
  {
    int k1 = bootstrap_name_and_type_ref_index_at(index1);
    int k2 = cp2->bootstrap_name_and_type_ref_index_at(index2);
    int i1 = bootstrap_methods_attribute_index(index1);
    int i2 = cp2->bootstrap_methods_attribute_index(index2);
    bool match_entry = compare_entry_to(k1, cp2, k2);
    bool match_operand = compare_operand_to(i1, cp2, i2);
    return (match_entry && match_operand);
  } break;

  case JVM_CONSTANT_String:
  {
    Symbol* s1 = unresolved_string_at(index1);
    Symbol* s2 = cp2->unresolved_string_at(index2);
    if (s1 == s2) {
      return true;
    }
  } break;

  case JVM_CONSTANT_Utf8:
  {
    Symbol* s1 = symbol_at(index1);
    Symbol* s2 = cp2->symbol_at(index2);
    if (s1 == s2) {
      return true;
    }
  } break;

  // Invalid is used as the tag for the second constant pool entry
  // occupied by JVM_CONSTANT_Double or JVM_CONSTANT_Long. It should
  // not be seen by itself.
  case JVM_CONSTANT_Invalid: // fall through

  default:
    ShouldNotReachHere();
    break;
  }

  return false;
} // end compare_entry_to()


// Resize the operands array with delta_len and delta_size.
// Used in RedefineClasses for CP merge.
void ConstantPool::resize_operands(int delta_len, int delta_size, TRAPS) {
  int old_len  = operand_array_length(operands());
  int new_len  = old_len + delta_len;
  int min_len  = (delta_len > 0) ? old_len : new_len;

  int old_size = operands()->length();
  int new_size = old_size + delta_size;
  int min_size = (delta_size > 0) ? old_size : new_size;

  ClassLoaderData* loader_data = pool_holder()->class_loader_data();
  Array<u2>* new_ops = MetadataFactory::new_array<u2>(loader_data, new_size, CHECK);

  // Set index in the resized array for existing elements only
  for (int idx = 0; idx < min_len; idx++) {
    int offset = operand_offset_at(idx);                       // offset in original array
    operand_offset_at_put(new_ops, idx, offset + 2*delta_len); // offset in resized array
  }
  // Copy the bootstrap specifiers only
  Copy::conjoint_memory_atomic(operands()->adr_at(2*old_len),
                               new_ops->adr_at(2*new_len),
                               (min_size - 2*min_len) * sizeof(u2));
  // Explicitly deallocate old operands array.
  // Note, it is not needed for 7u backport.
  if ( operands() != NULL) { // the safety check
    MetadataFactory::free_array<u2>(loader_data, operands());
  }
  set_operands(new_ops);
} // end resize_operands()


// Extend the operands array with the length and size of the ext_cp operands.
// Used in RedefineClasses for CP merge.
void ConstantPool::extend_operands(const constantPoolHandle& ext_cp, TRAPS) {
  int delta_len = operand_array_length(ext_cp->operands());
  if (delta_len == 0) {
    return; // nothing to do
  }
  int delta_size = ext_cp->operands()->length();

  assert(delta_len  > 0 && delta_size > 0, "extended operands array must be bigger");

  if (operand_array_length(operands()) == 0) {
    ClassLoaderData* loader_data = pool_holder()->class_loader_data();
    Array<u2>* new_ops = MetadataFactory::new_array<u2>(loader_data, delta_size, CHECK);
    // The first element index defines the offset of second part
    operand_offset_at_put(new_ops, 0, 2*delta_len); // offset in new array
    set_operands(new_ops);
  } else {
    resize_operands(delta_len, delta_size, CHECK);
  }

} // end extend_operands()


// Shrink the operands array to a smaller array with new_len length.
// Used in RedefineClasses for CP merge.
void ConstantPool::shrink_operands(int new_len, TRAPS) {
  int old_len = operand_array_length(operands());
  if (new_len == old_len) {
    return; // nothing to do
  }
  assert(new_len < old_len, "shrunken operands array must be smaller");

  int free_base  = operand_next_offset_at(new_len - 1);
  int delta_len  = new_len - old_len;
  int delta_size = 2*delta_len + free_base - operands()->length();

  resize_operands(delta_len, delta_size, CHECK);

} // end shrink_operands()


void ConstantPool::copy_operands(const constantPoolHandle& from_cp,
                                 const constantPoolHandle& to_cp,
                                 TRAPS) {

  int from_oplen = operand_array_length(from_cp->operands());
  int old_oplen  = operand_array_length(to_cp->operands());
  if (from_oplen != 0) {
    ClassLoaderData* loader_data = to_cp->pool_holder()->class_loader_data();
    // append my operands to the target's operands array
    if (old_oplen == 0) {
      // Can't just reuse from_cp's operand list because of deallocation issues
      int len = from_cp->operands()->length();
      Array<u2>* new_ops = MetadataFactory::new_array<u2>(loader_data, len, CHECK);
      Copy::conjoint_memory_atomic(
          from_cp->operands()->adr_at(0), new_ops->adr_at(0), len * sizeof(u2));
      to_cp->set_operands(new_ops);
    } else {
      int old_len  = to_cp->operands()->length();
      int from_len = from_cp->operands()->length();
      int old_off  = old_oplen * sizeof(u2);
      int from_off = from_oplen * sizeof(u2);
      // Use the metaspace for the destination constant pool
      Array<u2>* new_operands = MetadataFactory::new_array<u2>(loader_data, old_len + from_len, CHECK);
      int fillp = 0, len = 0;
      // first part of dest
      Copy::conjoint_memory_atomic(to_cp->operands()->adr_at(0),
                                   new_operands->adr_at(fillp),
                                   (len = old_off) * sizeof(u2));
      fillp += len;
      // first part of src
      Copy::conjoint_memory_atomic(from_cp->operands()->adr_at(0),
                                   new_operands->adr_at(fillp),
                                   (len = from_off) * sizeof(u2));
      fillp += len;
      // second part of dest
      Copy::conjoint_memory_atomic(to_cp->operands()->adr_at(old_off),
                                   new_operands->adr_at(fillp),
                                   (len = old_len - old_off) * sizeof(u2));
      fillp += len;
      // second part of src
      Copy::conjoint_memory_atomic(from_cp->operands()->adr_at(from_off),
                                   new_operands->adr_at(fillp),
                                   (len = from_len - from_off) * sizeof(u2));
      fillp += len;
      assert(fillp == new_operands->length(), "");

      // Adjust indexes in the first part of the copied operands array.
      for (int j = 0; j < from_oplen; j++) {
        int offset = operand_offset_at(new_operands, old_oplen + j);
        assert(offset == operand_offset_at(from_cp->operands(), j), "correct copy");
        offset += old_len;  // every new tuple is preceded by old_len extra u2's
        operand_offset_at_put(new_operands, old_oplen + j, offset);
      }

      // replace target operands array with combined array
      to_cp->set_operands(new_operands);
    }
  }
} // end copy_operands()


// Copy this constant pool's entries at start_i to end_i (inclusive)
// to the constant pool to_cp's entries starting at to_i. A total of
// (end_i - start_i) + 1 entries are copied.
void ConstantPool::copy_cp_to_impl(const constantPoolHandle& from_cp, int start_i, int end_i,
       const constantPoolHandle& to_cp, int to_i, TRAPS) {


  int dest_i = to_i;  // leave original alone for debug purposes

  for (int src_i = start_i; src_i <= end_i; /* see loop bottom */ ) {
    copy_entry_to(from_cp, src_i, to_cp, dest_i);

    switch (from_cp->tag_at(src_i).value()) {
    case JVM_CONSTANT_Double:
    case JVM_CONSTANT_Long:
      // double and long take two constant pool entries
      src_i += 2;
      dest_i += 2;
      break;

    default:
      // all others take one constant pool entry
      src_i++;
      dest_i++;
      break;
    }
  }
  copy_operands(from_cp, to_cp, CHECK);

} // end copy_cp_to_impl()


// Copy this constant pool's entry at from_i to the constant pool
// to_cp's entry at to_i.
void ConstantPool::copy_entry_to(const constantPoolHandle& from_cp, int from_i,
                                        const constantPoolHandle& to_cp, int to_i) {

  int tag = from_cp->tag_at(from_i).value();
  switch (tag) {
  case JVM_CONSTANT_ClassIndex:
  {
    jint ki = from_cp->klass_index_at(from_i);
    to_cp->klass_index_at_put(to_i, ki);
  } break;

  case JVM_CONSTANT_Double:
  {
    jdouble d = from_cp->double_at(from_i);
    to_cp->double_at_put(to_i, d);
    // double takes two constant pool entries so init second entry's tag
    to_cp->tag_at_put(to_i + 1, JVM_CONSTANT_Invalid);
  } break;

  case JVM_CONSTANT_Fieldref:
  {
    int class_index = from_cp->uncached_klass_ref_index_at(from_i);
    int name_and_type_index = from_cp->uncached_name_and_type_ref_index_at(from_i);
    to_cp->field_at_put(to_i, class_index, name_and_type_index);
  } break;

  case JVM_CONSTANT_Float:
  {
    jfloat f = from_cp->float_at(from_i);
    to_cp->float_at_put(to_i, f);
  } break;

  case JVM_CONSTANT_Integer:
  {
    jint i = from_cp->int_at(from_i);
    to_cp->int_at_put(to_i, i);
  } break;

  case JVM_CONSTANT_InterfaceMethodref:
  {
    int class_index = from_cp->uncached_klass_ref_index_at(from_i);
    int name_and_type_index = from_cp->uncached_name_and_type_ref_index_at(from_i);
    to_cp->interface_method_at_put(to_i, class_index, name_and_type_index);
  } break;

  case JVM_CONSTANT_Long:
  {
    jlong l = from_cp->long_at(from_i);
    to_cp->long_at_put(to_i, l);
    // long takes two constant pool entries so init second entry's tag
    to_cp->tag_at_put(to_i + 1, JVM_CONSTANT_Invalid);
  } break;

  case JVM_CONSTANT_Methodref:
  {
    int class_index = from_cp->uncached_klass_ref_index_at(from_i);
    int name_and_type_index = from_cp->uncached_name_and_type_ref_index_at(from_i);
    to_cp->method_at_put(to_i, class_index, name_and_type_index);
  } break;

  case JVM_CONSTANT_NameAndType:
  {
    int name_ref_index = from_cp->name_ref_index_at(from_i);
    int signature_ref_index = from_cp->signature_ref_index_at(from_i);
    to_cp->name_and_type_at_put(to_i, name_ref_index, signature_ref_index);
  } break;

  case JVM_CONSTANT_StringIndex:
  {
    jint si = from_cp->string_index_at(from_i);
    to_cp->string_index_at_put(to_i, si);
  } break;

  case JVM_CONSTANT_Class:
  case JVM_CONSTANT_UnresolvedClass:
  case JVM_CONSTANT_UnresolvedClassInError:
  {
    // Revert to JVM_CONSTANT_ClassIndex
    int name_index = from_cp->klass_slot_at(from_i).name_index();
    assert(from_cp->tag_at(name_index).is_symbol(), "sanity");
    to_cp->klass_index_at_put(to_i, name_index);
  } break;

  case JVM_CONSTANT_String:
  {
    Symbol* s = from_cp->unresolved_string_at(from_i);
    to_cp->unresolved_string_at_put(to_i, s);
  } break;

  case JVM_CONSTANT_Utf8:
  {
    Symbol* s = from_cp->symbol_at(from_i);
    // Need to increase refcount, the old one will be thrown away and deferenced
    s->increment_refcount();
    to_cp->symbol_at_put(to_i, s);
  } break;

  case JVM_CONSTANT_MethodType:
  case JVM_CONSTANT_MethodTypeInError:
  {
    jint k = from_cp->method_type_index_at(from_i);
    to_cp->method_type_index_at_put(to_i, k);
  } break;

  case JVM_CONSTANT_MethodHandle:
  case JVM_CONSTANT_MethodHandleInError:
  {
    int k1 = from_cp->method_handle_ref_kind_at(from_i);
    int k2 = from_cp->method_handle_index_at(from_i);
    to_cp->method_handle_index_at_put(to_i, k1, k2);
  } break;

  case JVM_CONSTANT_Dynamic:
  case JVM_CONSTANT_DynamicInError:
  {
    int k1 = from_cp->bootstrap_methods_attribute_index(from_i);
    int k2 = from_cp->bootstrap_name_and_type_ref_index_at(from_i);
    k1 += operand_array_length(to_cp->operands());  // to_cp might already have operands
    to_cp->dynamic_constant_at_put(to_i, k1, k2);
  } break;

  case JVM_CONSTANT_InvokeDynamic:
  {
    int k1 = from_cp->bootstrap_methods_attribute_index(from_i);
    int k2 = from_cp->bootstrap_name_and_type_ref_index_at(from_i);
    k1 += operand_array_length(to_cp->operands());  // to_cp might already have operands
    to_cp->invoke_dynamic_at_put(to_i, k1, k2);
  } break;

  // Invalid is used as the tag for the second constant pool entry
  // occupied by JVM_CONSTANT_Double or JVM_CONSTANT_Long. It should
  // not be seen by itself.
  case JVM_CONSTANT_Invalid: // fall through

  default:
  {
    ShouldNotReachHere();
  } break;
  }
} // end copy_entry_to()

// Search constant pool search_cp for an entry that matches this
// constant pool's entry at pattern_i. Returns the index of a
// matching entry or zero (0) if there is no matching entry.
int ConstantPool::find_matching_entry(int pattern_i,
      const constantPoolHandle& search_cp) {

  // index zero (0) is not used
  for (int i = 1; i < search_cp->length(); i++) {
    bool found = compare_entry_to(pattern_i, search_cp, i);
    if (found) {
      return i;
    }
  }

  return 0;  // entry not found; return unused index zero (0)
} // end find_matching_entry()


// Compare this constant pool's bootstrap specifier at idx1 to the constant pool
// cp2's bootstrap specifier at idx2.
bool ConstantPool::compare_operand_to(int idx1, const constantPoolHandle& cp2, int idx2) {
  int k1 = operand_bootstrap_method_ref_index_at(idx1);
  int k2 = cp2->operand_bootstrap_method_ref_index_at(idx2);
  bool match = compare_entry_to(k1, cp2, k2);

  if (!match) {
    return false;
  }
  int argc = operand_argument_count_at(idx1);
  if (argc == cp2->operand_argument_count_at(idx2)) {
    for (int j = 0; j < argc; j++) {
      k1 = operand_argument_index_at(idx1, j);
      k2 = cp2->operand_argument_index_at(idx2, j);
      match = compare_entry_to(k1, cp2, k2);
      if (!match) {
        return false;
      }
    }
    return true;           // got through loop; all elements equal
  }
  return false;
} // end compare_operand_to()

// Search constant pool search_cp for a bootstrap specifier that matches
// this constant pool's bootstrap specifier data at pattern_i index.
// Return the index of a matching bootstrap attribute record or (-1) if there is no match.
int ConstantPool::find_matching_operand(int pattern_i,
                    const constantPoolHandle& search_cp, int search_len) {
  for (int i = 0; i < search_len; i++) {
    bool found = compare_operand_to(pattern_i, search_cp, i);
    if (found) {
      return i;
    }
  }
  return -1;  // bootstrap specifier data not found; return unused index (-1)
} // end find_matching_operand()


#ifndef PRODUCT

const char* ConstantPool::printable_name_at(int which) {

  constantTag tag = tag_at(which);

  if (tag.is_string()) {
    return string_at_noresolve(which);
  } else if (tag.is_klass() || tag.is_unresolved_klass()) {
    return klass_name_at(which)->as_C_string();
  } else if (tag.is_symbol()) {
    return symbol_at(which)->as_C_string();
  }
  return "";
}

#endif // PRODUCT


// JVMTI GetConstantPool support

// For debugging of constant pool
const bool debug_cpool = false;

#define DBG(code) do { if (debug_cpool) { (code); } } while(0)

static void print_cpool_bytes(jint cnt, u1 *bytes) {
  const char* WARN_MSG = "Must not be such entry!";
  jint size = 0;
  u2   idx1, idx2;

  for (jint idx = 1; idx < cnt; idx++) {
    jint ent_size = 0;
    u1   tag  = *bytes++;
    size++;                       // count tag

    printf("const #%03d, tag: %02d ", idx, tag);
    switch(tag) {
      case JVM_CONSTANT_Invalid: {
        printf("Invalid");
        break;
      }
      case JVM_CONSTANT_Unicode: {
        printf("Unicode      %s", WARN_MSG);
        break;
      }
      case JVM_CONSTANT_Utf8: {
        u2 len = Bytes::get_Java_u2(bytes);
        char str[128];
        if (len > 127) {
           len = 127;
        }
        strncpy(str, (char *) (bytes+2), len);
        str[len] = '\0';
        printf("Utf8          \"%s\"", str);
        ent_size = 2 + len;
        break;
      }
      case JVM_CONSTANT_Integer: {
        u4 val = Bytes::get_Java_u4(bytes);
        printf("int          %d", *(int *) &val);
        ent_size = 4;
        break;
      }
      case JVM_CONSTANT_Float: {
        u4 val = Bytes::get_Java_u4(bytes);
        printf("float        %5.3ff", *(float *) &val);
        ent_size = 4;
        break;
      }
      case JVM_CONSTANT_Long: {
        u8 val = Bytes::get_Java_u8(bytes);
        printf("long         " INT64_FORMAT, (int64_t) *(jlong *) &val);
        ent_size = 8;
        idx++; // Long takes two cpool slots
        break;
      }
      case JVM_CONSTANT_Double: {
        u8 val = Bytes::get_Java_u8(bytes);
        printf("double       %5.3fd", *(jdouble *)&val);
        ent_size = 8;
        idx++; // Double takes two cpool slots
        break;
      }
      case JVM_CONSTANT_Class: {
        idx1 = Bytes::get_Java_u2(bytes);
        printf("class        #%03d", idx1);
        ent_size = 2;
        break;
      }
      case JVM_CONSTANT_String: {
        idx1 = Bytes::get_Java_u2(bytes);
        printf("String       #%03d", idx1);
        ent_size = 2;
        break;
      }
      case JVM_CONSTANT_Fieldref: {
        idx1 = Bytes::get_Java_u2(bytes);
        idx2 = Bytes::get_Java_u2(bytes+2);
        printf("Field        #%03d, #%03d", (int) idx1, (int) idx2);
        ent_size = 4;
        break;
      }
      case JVM_CONSTANT_Methodref: {
        idx1 = Bytes::get_Java_u2(bytes);
        idx2 = Bytes::get_Java_u2(bytes+2);
        printf("Method       #%03d, #%03d", idx1, idx2);
        ent_size = 4;
        break;
      }
      case JVM_CONSTANT_InterfaceMethodref: {
        idx1 = Bytes::get_Java_u2(bytes);
        idx2 = Bytes::get_Java_u2(bytes+2);
        printf("InterfMethod #%03d, #%03d", idx1, idx2);
        ent_size = 4;
        break;
      }
      case JVM_CONSTANT_NameAndType: {
        idx1 = Bytes::get_Java_u2(bytes);
        idx2 = Bytes::get_Java_u2(bytes+2);
        printf("NameAndType  #%03d, #%03d", idx1, idx2);
        ent_size = 4;
        break;
      }
      case JVM_CONSTANT_ClassIndex: {
        printf("ClassIndex  %s", WARN_MSG);
        break;
      }
      case JVM_CONSTANT_UnresolvedClass: {
        printf("UnresolvedClass: %s", WARN_MSG);
        break;
      }
      case JVM_CONSTANT_UnresolvedClassInError: {
        printf("UnresolvedClassInErr: %s", WARN_MSG);
        break;
      }
      case JVM_CONSTANT_StringIndex: {
        printf("StringIndex: %s", WARN_MSG);
        break;
      }
    }
    printf(";\n");
    bytes += ent_size;
    size  += ent_size;
  }
  printf("Cpool size: %d\n", size);
  fflush(0);
  return;
} /* end print_cpool_bytes */


// Returns size of constant pool entry.
jint ConstantPool::cpool_entry_size(jint idx) {
  switch(tag_at(idx).value()) {
    case JVM_CONSTANT_Invalid:
    case JVM_CONSTANT_Unicode:
      return 1;

    case JVM_CONSTANT_Utf8:
      return 3 + symbol_at(idx)->utf8_length();

    case JVM_CONSTANT_Class:
    case JVM_CONSTANT_String:
    case JVM_CONSTANT_ClassIndex:
    case JVM_CONSTANT_UnresolvedClass:
    case JVM_CONSTANT_UnresolvedClassInError:
    case JVM_CONSTANT_StringIndex:
    case JVM_CONSTANT_MethodType:
    case JVM_CONSTANT_MethodTypeInError:
      return 3;

    case JVM_CONSTANT_MethodHandle:
    case JVM_CONSTANT_MethodHandleInError:
      return 4; //tag, ref_kind, ref_index

    case JVM_CONSTANT_Integer:
    case JVM_CONSTANT_Float:
    case JVM_CONSTANT_Fieldref:
    case JVM_CONSTANT_Methodref:
    case JVM_CONSTANT_InterfaceMethodref:
    case JVM_CONSTANT_NameAndType:
      return 5;

    case JVM_CONSTANT_Dynamic:
    case JVM_CONSTANT_DynamicInError:
    case JVM_CONSTANT_InvokeDynamic:
      // u1 tag, u2 bsm, u2 nt
      return 5;

    case JVM_CONSTANT_Long:
    case JVM_CONSTANT_Double:
      return 9;
  }
  assert(false, "cpool_entry_size: Invalid constant pool entry tag");
  return 1;
} /* end cpool_entry_size */


// SymbolHashMap is used to find a constant pool index from a string.
// This function fills in SymbolHashMaps, one for utf8s and one for
// class names, returns size of the cpool raw bytes.
jint ConstantPool::hash_entries_to(SymbolHashMap *symmap,
                                          SymbolHashMap *classmap) {
  jint size = 0;

  for (u2 idx = 1; idx < length(); idx++) {
    u2 tag = tag_at(idx).value();
    size += cpool_entry_size(idx);

    switch(tag) {
      case JVM_CONSTANT_Utf8: {
        Symbol* sym = symbol_at(idx);
        symmap->add_entry(sym, idx);
        DBG(printf("adding symbol entry %s = %d\n", sym->as_utf8(), idx));
        break;
      }
      case JVM_CONSTANT_Class:
      case JVM_CONSTANT_UnresolvedClass:
      case JVM_CONSTANT_UnresolvedClassInError: {
        Symbol* sym = klass_name_at(idx);
        classmap->add_entry(sym, idx);
        DBG(printf("adding class entry %s = %d\n", sym->as_utf8(), idx));
        break;
      }
      case JVM_CONSTANT_Long:
      case JVM_CONSTANT_Double: {
        idx++; // Both Long and Double take two cpool slots
        break;
      }
    }
  }
  return size;
} /* end hash_utf8_entries_to */


// Copy cpool bytes.
// Returns:
//    0, in case of OutOfMemoryError
//   -1, in case of internal error
//  > 0, count of the raw cpool bytes that have been copied
int ConstantPool::copy_cpool_bytes(int cpool_size,
                                          SymbolHashMap* tbl,
                                          unsigned char *bytes) {
  u2   idx1, idx2;
  jint size  = 0;
  jint cnt   = length();
  unsigned char *start_bytes = bytes;

  for (jint idx = 1; idx < cnt; idx++) {
    u1   tag      = tag_at(idx).value();
    jint ent_size = cpool_entry_size(idx);

    assert(size + ent_size <= cpool_size, "Size mismatch");

    *bytes = tag;
    DBG(printf("#%03hd tag=%03hd, ", (short)idx, (short)tag));
    switch(tag) {
      case JVM_CONSTANT_Invalid: {
        DBG(printf("JVM_CONSTANT_Invalid"));
        break;
      }
      case JVM_CONSTANT_Unicode: {
        assert(false, "Wrong constant pool tag: JVM_CONSTANT_Unicode");
        DBG(printf("JVM_CONSTANT_Unicode"));
        break;
      }
      case JVM_CONSTANT_Utf8: {
        Symbol* sym = symbol_at(idx);
        char*     str = sym->as_utf8();
        // Warning! It's crashing on x86 with len = sym->utf8_length()
        int       len = (int) strlen(str);
        Bytes::put_Java_u2((address) (bytes+1), (u2) len);
        for (int i = 0; i < len; i++) {
            bytes[3+i] = (u1) str[i];
        }
        DBG(printf("JVM_CONSTANT_Utf8: %s ", str));
        break;
      }
      case JVM_CONSTANT_Integer: {
        jint val = int_at(idx);
        Bytes::put_Java_u4((address) (bytes+1), *(u4*)&val);
        break;
      }
      case JVM_CONSTANT_Float: {
        jfloat val = float_at(idx);
        Bytes::put_Java_u4((address) (bytes+1), *(u4*)&val);
        break;
      }
      case JVM_CONSTANT_Long: {
        jlong val = long_at(idx);
        Bytes::put_Java_u8((address) (bytes+1), *(u8*)&val);
        idx++;             // Long takes two cpool slots
        break;
      }
      case JVM_CONSTANT_Double: {
        jdouble val = double_at(idx);
        Bytes::put_Java_u8((address) (bytes+1), *(u8*)&val);
        idx++;             // Double takes two cpool slots
        break;
      }
      case JVM_CONSTANT_Class:
      case JVM_CONSTANT_UnresolvedClass:
      case JVM_CONSTANT_UnresolvedClassInError: {
        *bytes = JVM_CONSTANT_Class;
        Symbol* sym = klass_name_at(idx);
        idx1 = tbl->symbol_to_value(sym);
        assert(idx1 != 0, "Have not found a hashtable entry");
        Bytes::put_Java_u2((address) (bytes+1), idx1);
        DBG(printf("JVM_CONSTANT_Class: idx=#%03hd, %s", idx1, sym->as_utf8()));
        break;
      }
      case JVM_CONSTANT_String: {
        *bytes = JVM_CONSTANT_String;
        Symbol* sym = unresolved_string_at(idx);
        idx1 = tbl->symbol_to_value(sym);
        assert(idx1 != 0, "Have not found a hashtable entry");
        Bytes::put_Java_u2((address) (bytes+1), idx1);
        DBG(printf("JVM_CONSTANT_String: idx=#%03hd, %s", idx1, sym->as_utf8()));
        break;
      }
      case JVM_CONSTANT_Fieldref:
      case JVM_CONSTANT_Methodref:
      case JVM_CONSTANT_InterfaceMethodref: {
        idx1 = uncached_klass_ref_index_at(idx);
        idx2 = uncached_name_and_type_ref_index_at(idx);
        Bytes::put_Java_u2((address) (bytes+1), idx1);
        Bytes::put_Java_u2((address) (bytes+3), idx2);
        DBG(printf("JVM_CONSTANT_Methodref: %hd %hd", idx1, idx2));
        break;
      }
      case JVM_CONSTANT_NameAndType: {
        idx1 = name_ref_index_at(idx);
        idx2 = signature_ref_index_at(idx);
        Bytes::put_Java_u2((address) (bytes+1), idx1);
        Bytes::put_Java_u2((address) (bytes+3), idx2);
        DBG(printf("JVM_CONSTANT_NameAndType: %hd %hd", idx1, idx2));
        break;
      }
      case JVM_CONSTANT_ClassIndex: {
        *bytes = JVM_CONSTANT_Class;
        idx1 = klass_index_at(idx);
        Bytes::put_Java_u2((address) (bytes+1), idx1);
        DBG(printf("JVM_CONSTANT_ClassIndex: %hd", idx1));
        break;
      }
      case JVM_CONSTANT_StringIndex: {
        *bytes = JVM_CONSTANT_String;
        idx1 = string_index_at(idx);
        Bytes::put_Java_u2((address) (bytes+1), idx1);
        DBG(printf("JVM_CONSTANT_StringIndex: %hd", idx1));
        break;
      }
      case JVM_CONSTANT_MethodHandle:
      case JVM_CONSTANT_MethodHandleInError: {
        *bytes = JVM_CONSTANT_MethodHandle;
        int kind = method_handle_ref_kind_at(idx);
        idx1 = method_handle_index_at(idx);
        *(bytes+1) = (unsigned char) kind;
        Bytes::put_Java_u2((address) (bytes+2), idx1);
        DBG(printf("JVM_CONSTANT_MethodHandle: %d %hd", kind, idx1));
        break;
      }
      case JVM_CONSTANT_MethodType:
      case JVM_CONSTANT_MethodTypeInError: {
        *bytes = JVM_CONSTANT_MethodType;
        idx1 = method_type_index_at(idx);
        Bytes::put_Java_u2((address) (bytes+1), idx1);
        DBG(printf("JVM_CONSTANT_MethodType: %hd", idx1));
        break;
      }
      case JVM_CONSTANT_Dynamic:
      case JVM_CONSTANT_DynamicInError: {
        *bytes = tag;
        idx1 = extract_low_short_from_int(*int_at_addr(idx));
        idx2 = extract_high_short_from_int(*int_at_addr(idx));
        assert(idx2 == bootstrap_name_and_type_ref_index_at(idx), "correct half of u4");
        Bytes::put_Java_u2((address) (bytes+1), idx1);
        Bytes::put_Java_u2((address) (bytes+3), idx2);
        DBG(printf("JVM_CONSTANT_Dynamic: %hd %hd", idx1, idx2));
        break;
      }
      case JVM_CONSTANT_InvokeDynamic: {
        *bytes = tag;
        idx1 = extract_low_short_from_int(*int_at_addr(idx));
        idx2 = extract_high_short_from_int(*int_at_addr(idx));
        assert(idx2 == bootstrap_name_and_type_ref_index_at(idx), "correct half of u4");
        Bytes::put_Java_u2((address) (bytes+1), idx1);
        Bytes::put_Java_u2((address) (bytes+3), idx2);
        DBG(printf("JVM_CONSTANT_InvokeDynamic: %hd %hd", idx1, idx2));
        break;
      }
    }
    DBG(printf("\n"));
    bytes += ent_size;
    size  += ent_size;
  }
  assert(size == cpool_size, "Size mismatch");

  // Keep temorarily for debugging until it's stable.
  DBG(print_cpool_bytes(cnt, start_bytes));
  return (int)(bytes - start_bytes);
} /* end copy_cpool_bytes */

#undef DBG


void ConstantPool::set_on_stack(const bool value) {
  if (value) {
    // Only record if it's not already set.
    if (!on_stack()) {
      assert(!is_shared(), "should always be set for shared constant pools");
      _flags |= _on_stack;
      MetadataOnStackMark::record(this);
    }
  } else {
    // Clearing is done single-threadedly.
    if (!is_shared()) {
      _flags &= ~_on_stack;
    }
  }
}

// Printing

void ConstantPool::print_on(outputStream* st) const {
  assert(is_constantPool(), "must be constantPool");
  st->print_cr("%s", internal_name());
  if (flags() != 0) {
    st->print(" - flags: 0x%x", flags());
    if (has_preresolution()) st->print(" has_preresolution");
    if (on_stack()) st->print(" on_stack");
    st->cr();
  }
  if (pool_holder() != NULL) {
    st->print_cr(" - holder: " INTPTR_FORMAT, p2i(pool_holder()));
  }
  st->print_cr(" - cache: " INTPTR_FORMAT, p2i(cache()));
  st->print_cr(" - resolved_references: " INTPTR_FORMAT, p2i(resolved_references()));
  st->print_cr(" - reference_map: " INTPTR_FORMAT, p2i(reference_map()));
  st->print_cr(" - resolved_klasses: " INTPTR_FORMAT, p2i(resolved_klasses()));

  for (int index = 1; index < length(); index++) {      // Index 0 is unused
    ((ConstantPool*)this)->print_entry_on(index, st);
    switch (tag_at(index).value()) {
      case JVM_CONSTANT_Long :
      case JVM_CONSTANT_Double :
        index++;   // Skip entry following eigth-byte constant
    }

  }
  st->cr();
}

// Print one constant pool entry
void ConstantPool::print_entry_on(const int index, outputStream* st) {
  EXCEPTION_MARK;
  st->print(" - %3d : ", index);
  tag_at(index).print_on(st);
  st->print(" : ");
  switch (tag_at(index).value()) {
    case JVM_CONSTANT_Class :
      { Klass* k = klass_at(index, CATCH);
        guarantee(k != NULL, "need klass");
        k->print_value_on(st);
        st->print(" {" PTR_FORMAT "}", p2i(k));
      }
      break;
    case JVM_CONSTANT_Fieldref :
    case JVM_CONSTANT_Methodref :
    case JVM_CONSTANT_InterfaceMethodref :
      st->print("klass_index=%d", uncached_klass_ref_index_at(index));
      st->print(" name_and_type_index=%d", uncached_name_and_type_ref_index_at(index));
      break;
    case JVM_CONSTANT_String :
      unresolved_string_at(index)->print_value_on(st);
      break;
    case JVM_CONSTANT_Integer :
      st->print("%d", int_at(index));
      break;
    case JVM_CONSTANT_Float :
      st->print("%f", float_at(index));
      break;
    case JVM_CONSTANT_Long :
      st->print_jlong(long_at(index));
      break;
    case JVM_CONSTANT_Double :
      st->print("%lf", double_at(index));
      break;
    case JVM_CONSTANT_NameAndType :
      st->print("name_index=%d", name_ref_index_at(index));
      st->print(" signature_index=%d", signature_ref_index_at(index));
      break;
    case JVM_CONSTANT_Utf8 :
      symbol_at(index)->print_value_on(st);
      break;
    case JVM_CONSTANT_ClassIndex: {
        int name_index = *int_at_addr(index);
        st->print("klass_index=%d ", name_index);
        symbol_at(name_index)->print_value_on(st);
      }
      break;
    case JVM_CONSTANT_UnresolvedClass :               // fall-through
    case JVM_CONSTANT_UnresolvedClassInError: {
        CPKlassSlot kslot = klass_slot_at(index);
        int resolved_klass_index = kslot.resolved_klass_index();
        int name_index = kslot.name_index();
        assert(tag_at(name_index).is_symbol(), "sanity");
        symbol_at(name_index)->print_value_on(st);
      }
      break;
    case JVM_CONSTANT_MethodHandle :
    case JVM_CONSTANT_MethodHandleInError :
      st->print("ref_kind=%d", method_handle_ref_kind_at(index));
      st->print(" ref_index=%d", method_handle_index_at(index));
      break;
    case JVM_CONSTANT_MethodType :
    case JVM_CONSTANT_MethodTypeInError :
      st->print("signature_index=%d", method_type_index_at(index));
      break;
    case JVM_CONSTANT_Dynamic :
    case JVM_CONSTANT_DynamicInError :
      {
        st->print("bootstrap_method_index=%d", bootstrap_method_ref_index_at(index));
        st->print(" type_index=%d", bootstrap_name_and_type_ref_index_at(index));
        int argc = bootstrap_argument_count_at(index);
        if (argc > 0) {
          for (int arg_i = 0; arg_i < argc; arg_i++) {
            int arg = bootstrap_argument_index_at(index, arg_i);
            st->print((arg_i == 0 ? " arguments={%d" : ", %d"), arg);
          }
          st->print("}");
        }
      }
      break;
    case JVM_CONSTANT_InvokeDynamic :
      {
        st->print("bootstrap_method_index=%d", bootstrap_method_ref_index_at(index));
        st->print(" name_and_type_index=%d", bootstrap_name_and_type_ref_index_at(index));
        int argc = bootstrap_argument_count_at(index);
        if (argc > 0) {
          for (int arg_i = 0; arg_i < argc; arg_i++) {
            int arg = bootstrap_argument_index_at(index, arg_i);
            st->print((arg_i == 0 ? " arguments={%d" : ", %d"), arg);
          }
          st->print("}");
        }
      }
      break;
    default:
      ShouldNotReachHere();
      break;
  }
  st->cr();
}

void ConstantPool::print_value_on(outputStream* st) const {
  assert(is_constantPool(), "must be constantPool");
  st->print("constant pool [%d]", length());
  if (has_preresolution()) st->print("/preresolution");
  if (operands() != NULL)  st->print("/operands[%d]", operands()->length());
  print_address_on(st);
  if (pool_holder() != NULL) {
    st->print(" for ");
    pool_holder()->print_value_on(st);
    bool extra = (pool_holder()->constants() != this);
    if (extra)  st->print(" (extra)");
  }
  if (cache() != NULL) {
    st->print(" cache=" PTR_FORMAT, p2i(cache()));
  }
}

// Verification

void ConstantPool::verify_on(outputStream* st) {
  guarantee(is_constantPool(), "object must be constant pool");
  for (int i = 0; i< length();  i++) {
    constantTag tag = tag_at(i);
    if (tag.is_klass() || tag.is_unresolved_klass()) {
      guarantee(klass_name_at(i)->refcount() != 0, "should have nonzero reference count");
    } else if (tag.is_symbol()) {
      CPSlot entry = slot_at(i);
      guarantee(entry.get_symbol()->refcount() != 0, "should have nonzero reference count");
    } else if (tag.is_string()) {
      CPSlot entry = slot_at(i);
      guarantee(entry.get_symbol()->refcount() != 0, "should have nonzero reference count");
    }
  }
  if (pool_holder() != NULL) {
    // Note: pool_holder() can be NULL in temporary constant pools
    // used during constant pool merging
    guarantee(pool_holder()->is_klass(),    "should be klass");
  }
}


SymbolHashMap::~SymbolHashMap() {
  SymbolHashMapEntry* next;
  for (int i = 0; i < _table_size; i++) {
    for (SymbolHashMapEntry* cur = bucket(i); cur != NULL; cur = next) {
      next = cur->next();
      delete(cur);
    }
  }
  FREE_C_HEAP_ARRAY(SymbolHashMapBucket, _buckets);
}

void SymbolHashMap::add_entry(Symbol* sym, u2 value) {
  char *str = sym->as_utf8();
  unsigned int hash = compute_hash(str, sym->utf8_length());
  unsigned int index = hash % table_size();

  // check if already in map
  // we prefer the first entry since it is more likely to be what was used in
  // the class file
  for (SymbolHashMapEntry *en = bucket(index); en != NULL; en = en->next()) {
    assert(en->symbol() != NULL, "SymbolHashMapEntry symbol is NULL");
    if (en->hash() == hash && en->symbol() == sym) {
        return;  // already there
    }
  }

  SymbolHashMapEntry* entry = new SymbolHashMapEntry(hash, sym, value);
  entry->set_next(bucket(index));
  _buckets[index].set_entry(entry);
  assert(entry->symbol() != NULL, "SymbolHashMapEntry symbol is NULL");
}

SymbolHashMapEntry* SymbolHashMap::find_entry(Symbol* sym) {
  assert(sym != NULL, "SymbolHashMap::find_entry - symbol is NULL");
  char *str = sym->as_utf8();
  int   len = sym->utf8_length();
  unsigned int hash = SymbolHashMap::compute_hash(str, len);
  unsigned int index = hash % table_size();
  for (SymbolHashMapEntry *en = bucket(index); en != NULL; en = en->next()) {
    assert(en->symbol() != NULL, "SymbolHashMapEntry symbol is NULL");
    if (en->hash() == hash && en->symbol() == sym) {
      return en;
    }
  }
  return NULL;
}

void SymbolHashMap::initialize_table(int table_size) {
  _table_size = table_size;
  _buckets = NEW_C_HEAP_ARRAY(SymbolHashMapBucket, table_size, mtSymbol);
  for (int index = 0; index < table_size; index++) {
    _buckets[index].clear();
  }
}
