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
#include "cds/metaspaceShared.hpp"
#include "classfile/classLoaderDataGraph.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmSymbols.hpp"
#include "interpreter/linkResolver.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/klassVtable.hpp"
#include "oops/method.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/flags/flagSetting.hpp"
#include "runtime/java.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "utilities/copy.hpp"

inline InstanceKlass* klassVtable::ik() const {
  return InstanceKlass::cast(_klass);
}

bool klassVtable::is_preinitialized_vtable() {
  return _klass->is_shared() && !MetaspaceShared::remapped_readwrite() && _klass->verified_at_dump_time();
}


// this function computes the vtable size (including the size needed for miranda
// methods) and the number of miranda methods in this class.
// Note on Miranda methods: Let's say there is a class C that implements
// interface I, and none of C's superclasses implements I.
// Let's say there is an abstract method m in I that neither C
// nor any of its super classes implement (i.e there is no method of any access,
// with the same name and signature as m), then m is a Miranda method which is
// entered as a public abstract method in C's vtable.  From then on it should
// treated as any other public method in C for method over-ride purposes.
void klassVtable::compute_vtable_size_and_num_mirandas(
    int* vtable_length_ret, int* num_new_mirandas,
    GrowableArray<Method*>* all_mirandas, const Klass* super,
    Array<Method*>* methods, AccessFlags class_flags, u2 major_version,
    Handle classloader, Symbol* classname, Array<InstanceKlass*>* local_interfaces) {
  NoSafepointVerifier nsv;

  // set up default result values
  int vtable_length = 0;

  // start off with super's vtable length
  vtable_length = super == NULL ? 0 : super->vtable_length();

  // go thru each method in the methods table to see if it needs a new entry
  int len = methods->length();
  for (int i = 0; i < len; i++) {
    Method* method = methods->at(i);

    if (needs_new_vtable_entry(method, super, classloader, classname, class_flags, major_version)) {
      assert(!method->is_private(), "private methods should not need a vtable entry");
      vtable_length += vtableEntry::size(); // we need a new entry
    }
  }

  GrowableArray<Method*> new_mirandas(20);
  // compute the number of mirandas methods that must be added to the end
  get_mirandas(&new_mirandas, all_mirandas, super, methods, NULL, local_interfaces,
               class_flags.is_interface());
  *num_new_mirandas = new_mirandas.length();

  // Interfaces do not need interface methods in their vtables
  // This includes miranda methods and during later processing, default methods
  if (!class_flags.is_interface()) {
     vtable_length += *num_new_mirandas * vtableEntry::size();
  }

  if (Universe::is_bootstrapping() && vtable_length == 0) {
    // array classes don't have their superclass set correctly during
    // bootstrapping
    vtable_length = Universe::base_vtable_size();
  }

  if (super == NULL && vtable_length != Universe::base_vtable_size()) {
    if (Universe::is_bootstrapping()) {
      // Someone is attempting to override java.lang.Object incorrectly on the
      // bootclasspath.  The JVM cannot recover from this error including throwing
      // an exception
      vm_exit_during_initialization("Incompatible definition of java.lang.Object");
    } else {
      // Someone is attempting to redefine java.lang.Object incorrectly.  The
      // only way this should happen is from
      // SystemDictionary::resolve_from_stream(), which will detect this later
      // and throw a security exception.  So don't assert here to let
      // the exception occur.
      vtable_length = Universe::base_vtable_size();
    }
  }
  assert(vtable_length % vtableEntry::size() == 0, "bad vtable length");
  assert(vtable_length >= Universe::base_vtable_size(), "vtable too small");

  *vtable_length_ret = vtable_length;
}

// Copy super class's vtable to the first part (prefix) of this class's vtable,
// and return the number of entries copied.  Expects that 'super' is the Java
// super class (arrays can have "array" super classes that must be skipped).
int klassVtable::initialize_from_super(Klass* super) {
  if (super == NULL) {
    return 0;
  } else if (is_preinitialized_vtable()) {
    // A shared class' vtable is preinitialized at dump time. No need to copy
    // methods from super class for shared class, as that was already done
    // during archiving time. However, if Jvmti has redefined a class,
    // copy super class's vtable in case the super class has changed.
    return super->vtable().length();
  } else {
    // copy methods from superKlass
    klassVtable superVtable = super->vtable();
    assert(superVtable.length() <= _length, "vtable too short");
#ifdef ASSERT
    superVtable.verify(tty, true);
#endif
    superVtable.copy_vtable_to(table());
    if (log_develop_is_enabled(Trace, vtables)) {
      ResourceMark rm;
      log_develop_trace(vtables)("copy vtable from %s to %s size %d",
                                 super->internal_name(), klass()->internal_name(),
                                 _length);
    }
    return superVtable.length();
  }
}

//
// Revised lookup semantics   introduced 1.3 (Kestrel beta)
void klassVtable::initialize_vtable(GrowableArray<InstanceKlass*>* supers) {

  // Note:  Arrays can have intermediate array supers.  Use java_super to skip them.
  InstanceKlass* super = _klass->java_super();

  bool is_shared = _klass->is_shared();
  Thread* current = Thread::current();

  if (!_klass->is_array_klass()) {
    ResourceMark rm(current);
    log_develop_debug(vtables)("Initializing: %s", _klass->name()->as_C_string());
  }

#ifdef ASSERT
  oop* end_of_obj = (oop*)_klass + _klass->size();
  oop* end_of_vtable = (oop*)&table()[_length];
  assert(end_of_vtable <= end_of_obj, "vtable extends beyond end");
#endif

  if (Universe::is_bootstrapping()) {
    assert(!is_shared, "sanity");
    // just clear everything
    for (int i = 0; i < _length; i++) table()[i].clear();
    return;
  }

  int super_vtable_len = initialize_from_super(super);
  if (_klass->is_array_klass()) {
    assert(super_vtable_len == _length, "arrays shouldn't introduce new methods");
  } else {
    assert(_klass->is_instance_klass(), "must be InstanceKlass");

    Array<Method*>* methods = ik()->methods();
    int len = methods->length();
    int initialized = super_vtable_len;

    // Check each of this class's methods against super;
    // if override, replace in copy of super vtable, otherwise append to end
    for (int i = 0; i < len; i++) {
      // update_inherited_vtable can stop for gc - ensure using handles
      methodHandle mh(current, methods->at(i));

      bool needs_new_entry = update_inherited_vtable(current, mh, super_vtable_len, -1, supers);

      if (needs_new_entry) {
        put_method_at(mh(), initialized);
        mh->set_vtable_index(initialized); // set primary vtable index
        initialized++;
      }
    }

    // update vtable with default_methods
    Array<Method*>* default_methods = ik()->default_methods();
    if (default_methods != NULL) {
      len = default_methods->length();
      if (len > 0) {
        Array<int>* def_vtable_indices = ik()->default_vtable_indices();
        assert(def_vtable_indices != NULL, "should be created");
        assert(def_vtable_indices->length() == len, "reinit vtable len?");
        for (int i = 0; i < len; i++) {
          bool needs_new_entry;
          {
            // Reduce the scope of this handle so that it is fetched again.
            // The methodHandle keeps it from being deleted by RedefineClasses while
            // we're using it.
            methodHandle mh(current, default_methods->at(i));
            assert(!mh->is_private(), "private interface method in the default method list");
            needs_new_entry = update_inherited_vtable(current, mh, super_vtable_len, i, supers);
          }

          // needs new entry
          if (needs_new_entry) {
            // Refetch this default method in case of redefinition that might
            // happen during constraint checking in the update_inherited_vtable call above.
            Method* method = default_methods->at(i);
            put_method_at(method, initialized);
            if (is_preinitialized_vtable()) {
              // At runtime initialize_vtable is rerun for a shared class
              // (loaded by the non-boot loader) as part of link_class_impl().
              // The dumptime vtable index should be the same as the runtime index.
              assert(def_vtable_indices->at(i) == initialized,
                     "dump time vtable index is different from runtime index");
            } else {
              def_vtable_indices->at_put(i, initialized); //set vtable index
            }
            initialized++;
          }
        }
      }
    }

    // add miranda methods; it will also return the updated initialized
    // Interfaces do not need interface methods in their vtables
    // This includes miranda methods and during later processing, default methods
    if (!ik()->is_interface()) {
      initialized = fill_in_mirandas(current, initialized);
    }

    // In class hierarchies where the accessibility is not increasing (i.e., going from private ->
    // package_private -> public/protected), the vtable might actually be smaller than our initial
    // calculation, for classfile versions for which we do not do transitive override
    // calculations.
    if (ik()->major_version() >= VTABLE_TRANSITIVE_OVERRIDE_VERSION) {
      assert(initialized == _length, "vtable initialization failed");
    } else {
      assert(initialized <= _length, "vtable initialization failed");
      for(;initialized < _length; initialized++) {
        table()[initialized].clear();
      }
    }
    NOT_PRODUCT(verify(tty, true));
  }
}

// Returns true iff super_method can be overridden by a method in targetclassname
// See JLS 3rd edition 8.4.6.1
// Assumes name-signature match
// Note that the InstanceKlass of the method in the targetclassname has not always been created yet
static bool can_be_overridden(Method* super_method, Handle targetclassloader, Symbol* targetclassname) {
   // Private methods can not be overridden
   assert(!super_method->is_private(), "shouldn't call with a private method");

   // If super method is accessible, then override
   if ((super_method->is_protected()) ||
       (super_method->is_public())) {
     return true;
   }
   // Package-private methods are not inherited outside of package
   assert(super_method->is_package_private(), "must be package private");
   return(super_method->method_holder()->is_same_class_package(targetclassloader(), targetclassname));
}


// Called for cases where a method does not override its superclass' vtable entry
// For bytecodes not produced by javac together it is possible that a method does not override
// the superclass's method, but might indirectly override a super-super class's vtable entry
// If none found, return a null superk, else return the superk of the method this does override
// For public and protected methods: if they override a superclass, they will
// also be overridden themselves appropriately.
// Private methods do not override, and are not overridden and are not in the vtable.
// Package Private methods are trickier:
// e.g. P1.A, pub m
// P2.B extends A, package private m
// P1.C extends B, public m
// P1.C.m needs to override P1.A.m and can not override P2.B.m
// Therefore: all package private methods need their own vtable entries for
// them to be the root of an inheritance overriding decision
// Package private methods may also override other vtable entries
InstanceKlass* klassVtable::find_transitive_override(InstanceKlass* initialsuper,
                                                     const methodHandle& target_method,
                                                     int vtable_index,
                                                     Handle target_loader,
                                                     Symbol* target_classname) {

  InstanceKlass* superk = initialsuper;
  while (superk != NULL && superk->super() != NULL) {
    klassVtable ssVtable = (superk->super())->vtable();
    if (vtable_index < ssVtable.length()) {
      Method* super_method = ssVtable.method_at(vtable_index);
#ifndef PRODUCT
      Symbol* name= target_method()->name();
      Symbol* signature = target_method()->signature();
      assert(super_method->name() == name && super_method->signature() == signature, "vtable entry name/sig mismatch");
#endif

      if (can_be_overridden(super_method, target_loader, target_classname)) {
        if (log_develop_is_enabled(Trace, vtables)) {
          ResourceMark rm;
          LogTarget(Trace, vtables) lt;
          LogStream ls(lt);
          char* sig = target_method()->name_and_sig_as_C_string();
          ls.print("transitive overriding superclass %s with %s index %d, original flags: ",
                       super_method->method_holder()->internal_name(),
                       sig, vtable_index);
          super_method->print_linkage_flags(&ls);
          ls.print("overriders flags: ");
          target_method->print_linkage_flags(&ls);
          ls.cr();
        }

        break; // return found superk
      }
    } else  {
      // super class has no vtable entry here, stop transitive search
      superk = (InstanceKlass*)NULL;
      break;
    }
    // if no override found yet, continue to search up
    superk = superk->super() == NULL ? NULL : InstanceKlass::cast(superk->super());
  }

  return superk;
}

static void log_vtables(int i, bool overrides, const methodHandle& target_method,
                        Klass* target_klass, Method* super_method) {
#ifndef PRODUCT
  if (log_develop_is_enabled(Trace, vtables)) {
    ResourceMark rm;
    LogTarget(Trace, vtables) lt;
    LogStream ls(lt);
    char* sig = target_method()->name_and_sig_as_C_string();
    if (overrides) {
      ls.print("overriding with %s index %d, original flags: ",
                   sig, i);
    } else {
      ls.print("NOT overriding with %s index %d, original flags: ",
                   sig, i);
    }
    super_method->print_linkage_flags(&ls);
    ls.print("overriders flags: ");
    target_method->print_linkage_flags(&ls);
    ls.cr();
  }
#endif
}

// Update child's copy of super vtable for overrides
// OR return true if a new vtable entry is required.
// Only called for InstanceKlass's, i.e. not for arrays
// If that changed, could not use _klass as handle for klass
bool klassVtable::update_inherited_vtable(Thread* current,
                                          const methodHandle& target_method,
                                          int super_vtable_len, int default_index,
                                          GrowableArray<InstanceKlass*>* supers) {
  bool allocate_new = true;

  InstanceKlass* klass = ik();

  Array<int>* def_vtable_indices = NULL;
  bool is_default = false;

  // default methods are non-private concrete methods in superinterfaces which are added
  // to the vtable with their real method_holder.
  // Since vtable and itable indices share the same storage, don't touch
  // the default method's real vtable/itable index.
  // default_vtable_indices stores the vtable value relative to this inheritor
  if (default_index >= 0 ) {
    is_default = true;
    def_vtable_indices = klass->default_vtable_indices();
    assert(!target_method->is_private(), "private interface method flagged as default");
    assert(def_vtable_indices != NULL, "def vtable alloc?");
    assert(default_index <= def_vtable_indices->length(), "def vtable len?");
  } else {
    assert(klass == target_method->method_holder(), "caller resp.");
    // Initialize the method's vtable index to "nonvirtual".
    // If we allocate a vtable entry, we will update it to a non-negative number.
    target_method->set_vtable_index(Method::nonvirtual_vtable_index);
  }

  // Private, static and <init> methods are never in
  if (target_method->is_private() || target_method->is_static() ||
      (target_method->name()->fast_compare(vmSymbols::object_initializer_name()) == 0)) {
    return false;
  }

  if (target_method->is_final_method(klass->access_flags())) {
    // a final method never needs a new entry; final methods can be statically
    // resolved and they have to be present in the vtable only if they override
    // a super's method, in which case they re-use its entry
    allocate_new = false;
  } else if (klass->is_interface()) {
    allocate_new = false;  // see note below in needs_new_vtable_entry
    // An interface never allocates new vtable slots, only inherits old ones.
    // This method will either be assigned its own itable index later,
    // or be assigned an inherited vtable index in the loop below.
    // default methods inherited by classes store their vtable indices
    // in the inheritor's default_vtable_indices.
    // default methods inherited by interfaces may already have a
    // valid itable index, if so, don't change it.
    // Overpass methods in an interface will be assigned an itable index later
    // by an inheriting class.
    if ((!is_default || !target_method->has_itable_index())) {
      target_method->set_vtable_index(Method::pending_itable_index);
    }
  }

  // we need a new entry if there is no superclass
  Klass* super = klass->super();
  if (super == NULL) {
    return allocate_new;
  }

  // search through the vtable and update overridden entries
  // Since check_signature_loaders acquires SystemDictionary_lock
  // which can block for gc, once we are in this loop, use handles
  // For classfiles built with >= jdk7, we now look for transitive overrides

  Symbol* name = target_method->name();
  Symbol* signature = target_method->signature();

  Klass* target_klass = target_method->method_holder();
  assert(target_klass != NULL, "impossible");
  if (target_klass == NULL) {
    target_klass = _klass;
  }

  HandleMark hm(current);
  Handle target_loader(current, target_klass->class_loader());

  Symbol* target_classname = target_klass->name();
  for(int i = 0; i < super_vtable_len; i++) {
    Method* super_method;
    if (is_preinitialized_vtable()) {
      // If this is a shared class, the vtable is already in the final state (fully
      // initialized). Need to look at the super's vtable.
      klassVtable superVtable = super->vtable();
      super_method = superVtable.method_at(i);
    } else {
      super_method = method_at(i);
    }
    // Check if method name matches.  Ignore match if klass is an interface and the
    // matching method is a non-public java.lang.Object method.  (See JVMS 5.4.3.4)
    // This is safe because the method at this slot should never get invoked.
    // (TBD: put in a method to throw NoSuchMethodError if this slot is ever used.)
    if (super_method->name() == name && super_method->signature() == signature &&
        (!klass->is_interface() ||
         !SystemDictionary::is_nonpublic_Object_method(super_method))) {

      // get super_klass for method_holder for the found method
      InstanceKlass* super_klass =  super_method->method_holder();

      // Whether the method is being overridden
      bool overrides = false;

      // private methods are also never overridden
      if (!super_method->is_private() &&
          (is_default ||
           can_be_overridden(super_method, target_loader, target_classname) ||
           (klass->major_version() >= VTABLE_TRANSITIVE_OVERRIDE_VERSION &&
             (super_klass = find_transitive_override(super_klass,
                                                     target_method, i, target_loader,
                                                     target_classname)) != NULL))) {

        // Package private methods always need a new entry to root their own
        // overriding. They may also override other methods.
        if (!target_method->is_package_private()) {
          allocate_new = false;
        }

        // Set the vtable index before the constraint check safepoint, which potentially
        // redefines this method if this method is a default method belonging to a
        // super class or interface.
        put_method_at(target_method(), i);
        // Save super for constraint checking.
        if (supers != NULL) {
          supers->at_put(i, super_klass);
        }

        overrides = true;
        if (!is_default) {
          target_method->set_vtable_index(i);
        } else {
          if (def_vtable_indices != NULL) {
            if (is_preinitialized_vtable()) {
              // At runtime initialize_vtable is rerun as part of link_class_impl()
              // for a shared class loaded by the non-boot loader.
              // The dumptime vtable index should be the same as the runtime index.
              assert(def_vtable_indices->at(default_index) == i,
                     "dump time vtable index is different from runtime index");
            } else {
              def_vtable_indices->at_put(default_index, i);
            }
          }
          assert(super_method->is_default_method() || super_method->is_overpass()
                 || super_method->is_abstract(), "default override error");
        }
      } else {
        overrides = false;
      }
      log_vtables(i, overrides, target_method, target_klass, super_method);
    }
  }
  return allocate_new;
}

void klassVtable::put_method_at(Method* m, int index) {
  assert(!m->is_private(), "private methods should not be in vtable");
  JVMTI_ONLY(assert(!m->is_old() || ik()->is_being_redefined(), "old methods should not be in vtable"));
  if (is_preinitialized_vtable()) {
    // At runtime initialize_vtable is rerun as part of link_class_impl()
    // for shared class loaded by the non-boot loader to obtain the loader
    // constraints based on the runtime classloaders' context. The dumptime
    // method at the vtable index should be the same as the runtime method.
    assert(table()[index].method() == m,
           "archived method is different from the runtime method");
  } else {
    if (log_develop_is_enabled(Trace, vtables)) {
      ResourceMark rm;
      LogTarget(Trace, vtables) lt;
      LogStream ls(lt);
      const char* sig = (m != NULL) ? m->name_and_sig_as_C_string() : "<NULL>";
      ls.print("adding %s at index %d, flags: ", sig, index);
      if (m != NULL) {
        m->print_linkage_flags(&ls);
      }
      ls.cr();
    }
    table()[index].set(m);
  }
}

void klassVtable::check_constraints(GrowableArray<InstanceKlass*>* supers, TRAPS) {
  assert(supers->length() == length(), "lengths are different");
  // For each method in the vtable, check constraints against any super class
  // if overridden.
  for (int i = 0; i < length(); i++) {
    methodHandle target_method(THREAD, unchecked_method_at(i));
    InstanceKlass* super_klass = supers->at(i);
    if (target_method() != NULL && super_klass != NULL) {
      // Do not check loader constraints for overpass methods because overpass
      // methods are created by the jvm to throw exceptions.
      if (!target_method->is_overpass()) {
        // Override vtable entry if passes loader constraint check
        // if loader constraint checking requested
        // No need to visit his super, since he and his super
        // have already made any needed loader constraints.
        // Since loader constraints are transitive, it is enough
        // to link to the first super, and we get all the others.
        Handle super_loader(THREAD, super_klass->class_loader());
        InstanceKlass* target_klass = target_method->method_holder();
        Handle target_loader(THREAD, target_klass->class_loader());

        if (target_loader() != super_loader()) {
          ResourceMark rm(THREAD);
          Symbol* failed_type_symbol =
            SystemDictionary::check_signature_loaders(target_method->signature(),
                                                      _klass,
                                                      target_loader, super_loader,
                                                      true);
          if (failed_type_symbol != NULL) {
            stringStream ss;
            ss.print("loader constraint violation for class %s: when selecting "
                     "overriding method '", _klass->external_name());
            target_method->print_external_name(&ss),
            ss.print("' the class loader %s of the "
                     "selected method's type %s, and the class loader %s for its super "
                     "type %s have different Class objects for the type %s used in the signature (%s; %s)",
                     target_klass->class_loader_data()->loader_name_and_id(),
                     target_klass->external_name(),
                     super_klass->class_loader_data()->loader_name_and_id(),
                     super_klass->external_name(),
                     failed_type_symbol->as_klass_external_name(),
                     target_klass->class_in_module_of_loader(false, true),
                     super_klass->class_in_module_of_loader(false, true));
            THROW_MSG(vmSymbols::java_lang_LinkageError(), ss.as_string());
          }
        }
      }
    }
  }
}

void klassVtable::initialize_vtable_and_check_constraints(TRAPS) {
  // Save a superclass from each vtable entry to do constraint checking
  ResourceMark rm(THREAD);
  GrowableArray<InstanceKlass*>* supers = new GrowableArray<InstanceKlass*>(_length, _length, NULL);
  initialize_vtable(supers);
  check_constraints(supers, CHECK);
}


// Find out if a method "m" with superclass "super", loader "classloader" and
// name "classname" needs a new vtable entry.  Let P be a class package defined
// by "classloader" and "classname".
// NOTE: The logic used here is very similar to the one used for computing
// the vtables indices for a method. We cannot directly use that function because,
// we allocate the InstanceKlass at load time, and that requires that the
// superclass has been loaded.
// However, the vtable entries are filled in at link time, and therefore
// the superclass' vtable may not yet have been filled in.
bool klassVtable::needs_new_vtable_entry(Method* target_method,
                                         const Klass* super,
                                         Handle classloader,
                                         Symbol* classname,
                                         AccessFlags class_flags,
                                         u2 major_version) {
  if (class_flags.is_interface()) {
    // Interfaces do not use vtables, except for java.lang.Object methods,
    // so there is no point to assigning
    // a vtable index to any of their local methods.  If we refrain from doing this,
    // we can use Method::_vtable_index to hold the itable index
    return false;
  }

  if (target_method->is_final_method(class_flags) ||
      // a final method never needs a new entry; final methods can be statically
      // resolved and they have to be present in the vtable only if they override
      // a super's method, in which case they re-use its entry
      (target_method->is_private()) ||
      // private methods don't need to be in vtable
      (target_method->is_static()) ||
      // static methods don't need to be in vtable
      (target_method->name()->fast_compare(vmSymbols::object_initializer_name()) == 0)
      // <init> is never called dynamically-bound
      ) {
    return false;
  }

  // Concrete interface methods do not need new entries, they override
  // abstract method entries using default inheritance rules
  if (target_method->method_holder() != NULL &&
      target_method->method_holder()->is_interface()  &&
      !target_method->is_abstract()) {
    assert(target_method->is_default_method(),
           "unexpected interface method type");
    return false;
  }

  // we need a new entry if there is no superclass
  if (super == NULL) {
    return true;
  }

  // Package private methods always need a new entry to root their own
  // overriding. This allows transitive overriding to work.
  if (target_method->is_package_private()) {
    return true;
  }

  // search through the super class hierarchy to see if we need
  // a new entry
  Symbol* name = target_method->name();
  Symbol* signature = target_method->signature();
  const Klass* k = super;
  Method* super_method = NULL;
  InstanceKlass *holder = NULL;
  Method* recheck_method =  NULL;
  bool found_pkg_prvt_method = false;
  while (k != NULL) {
    // lookup through the hierarchy for a method with matching name and sign.
    super_method = InstanceKlass::cast(k)->lookup_method(name, signature);
    if (super_method == NULL) {
      break; // we still have to search for a matching miranda method
    }
    // get the class holding the matching method
    InstanceKlass* superk = super_method->method_holder();
    // we want only instance method matches
    // ignore private methods found via lookup_method since they do not participate in overriding,
    // and since we do override around them: e.g. a.m pub/b.m private/c.m pub,
    // ignore private, c.m pub does override a.m pub
    // For classes that were not javac'd together, we also do transitive overriding around
    // methods that have less accessibility
    if (!super_method->is_static() &&
        !super_method->is_private()) {
      if (can_be_overridden(super_method, classloader, classname)) {
        return false;
        // else keep looking for transitive overrides
      }
      // If we get here then one of the super classes has a package private method
      // that will not get overridden because it is in a different package.  But,
      // that package private method does "override" any matching methods in super
      // interfaces, so there will be no miranda vtable entry created.  So, set flag
      // to TRUE for use below, in case there are no methods in super classes that
      // this target method overrides.
      assert(super_method->is_package_private(), "super_method must be package private");
      assert(!superk->is_same_class_package(classloader(), classname),
             "Must be different packages");
      found_pkg_prvt_method = true;
    }

    // Start with lookup result and continue to search up, for versions supporting transitive override
    if (major_version >= VTABLE_TRANSITIVE_OVERRIDE_VERSION) {
      k = superk->super(); // haven't found an override match yet; continue to look
    } else {
      break;
    }
  }

  // If found_pkg_prvt_method is set, then the ONLY matching method in the
  // superclasses is package private in another package. That matching method will
  // prevent a miranda vtable entry from being created. Because the target method can not
  // override the package private method in another package, then it needs to be the root
  // for its own vtable entry.
  if (found_pkg_prvt_method) {
     return true;
  }

  // if the target method is public or protected it may have a matching
  // miranda method in the super, whose entry it should re-use.
  // Actually, to handle cases that javac would not generate, we need
  // this check for all access permissions.
  const InstanceKlass *sk = InstanceKlass::cast(super);
  if (sk->has_miranda_methods()) {
    if (sk->lookup_method_in_all_interfaces(name, signature, Klass::DefaultsLookupMode::find) != NULL) {
      return false; // found a matching miranda; we do not need a new entry
    }
  }
  return true; // found no match; we need a new entry
}

// Support for miranda methods

// get the vtable index of a miranda method with matching "name" and "signature"
int klassVtable::index_of_miranda(Symbol* name, Symbol* signature) {
  // search from the bottom, might be faster
  for (int i = (length() - 1); i >= 0; i--) {
    Method* m = table()[i].method();
    if (is_miranda_entry_at(i) &&
        m->name() == name && m->signature() == signature) {
      return i;
    }
  }
  return Method::invalid_vtable_index;
}

// check if an entry at an index is miranda
// requires that method m at entry be declared ("held") by an interface.
bool klassVtable::is_miranda_entry_at(int i) {
  Method* m = method_at(i);
  InstanceKlass* holder = m->method_holder();

  // miranda methods are public abstract instance interface methods in a class's vtable
  if (holder->is_interface()) {
    assert(m->is_public(), "should be public");
    assert(ik()->implements_interface(holder) , "this class should implement the interface");
    if (is_miranda(m, ik()->methods(), ik()->default_methods(), ik()->super(), klass()->is_interface())) {
      return true;
    }
  }
  return false;
}

// Check if a method is a miranda method, given a class's methods array,
// its default_method table and its super class.
// "Miranda" means an abstract non-private method that would not be
// overridden for the local class.
// A "miranda" method should only include non-private interface
// instance methods, i.e. not private methods, not static methods,
// not default methods (concrete interface methods), not overpass methods.
// If a given class already has a local (including overpass) method, a
// default method, or any of its superclasses has the same which would have
// overridden an abstract method, then this is not a miranda method.
//
// Miranda methods are checked multiple times.
// Pass 1: during class load/class file parsing: before vtable size calculation:
// include superinterface abstract and default methods (non-private instance).
// We include potential default methods to give them space in the vtable.
// During the first run, the current instanceKlass has not yet been
// created, the superclasses and superinterfaces do have instanceKlasses
// but may not have vtables, the default_methods list is empty, no overpasses.
// Default method generation uses the all_mirandas array as the starter set for
// maximally-specific default method calculation.  So, for both classes and
// interfaces, it is necessary that the first pass will find all non-private
// interface instance methods, whether or not they are concrete.
//
// Pass 2: recalculated during vtable initialization: only include abstract methods.
// The goal of pass 2 is to walk through the superinterfaces to see if any of
// the superinterface methods (which were all abstract pre-default methods)
// need to be added to the vtable.
// With the addition of default methods, we have three new challenges:
// overpasses, static interface methods and private interface methods.
// Static and private interface methods do not get added to the vtable and
// are not seen by the method resolution process, so we skip those.
// Overpass methods are already in the vtable, so vtable lookup will
// find them and we don't need to add a miranda method to the end of
// the vtable. So we look for overpass methods and if they are found we
// return false. Note that we inherit our superclasses vtable, so
// the superclass' search also needs to use find_overpass so that if
// one is found we return false.
// False means - we don't need a miranda method added to the vtable.
//
// During the second run, default_methods is set up, so concrete methods from
// superinterfaces with matching names/signatures to default_methods are already
// in the default_methods list and do not need to be appended to the vtable
// as mirandas. Abstract methods may already have been handled via
// overpasses - either local or superclass overpasses, which may be
// in the vtable already.
//
// Pass 3: They are also checked by link resolution and selection,
// for invocation on a method (not interface method) reference that
// resolves to a method with an interface as its method_holder.
// Used as part of walking from the bottom of the vtable to find
// the vtable index for the miranda method.
//
// Part of the Miranda Rights in the US mean that if you do not have
// an attorney one will be appointed for you.
bool klassVtable::is_miranda(Method* m, Array<Method*>* class_methods,
                             Array<Method*>* default_methods, const Klass* super,
                             bool is_interface) {
  if (m->is_static() || m->is_private() || m->is_overpass()) {
    return false;
  }
  Symbol* name = m->name();
  Symbol* signature = m->signature();

  // First look in local methods to see if already covered
  if (InstanceKlass::find_local_method(class_methods, name, signature,
                                       Klass::OverpassLookupMode::find,
                                       Klass::StaticLookupMode::skip,
                                       Klass::PrivateLookupMode::skip) != NULL)
  {
    return false;
  }

  // Check local default methods
  if ((default_methods != NULL) &&
    (InstanceKlass::find_method(default_methods, name, signature) != NULL))
   {
     return false;
   }

  // Iterate on all superclasses, which should be InstanceKlasses.
  // Note that we explicitly look for overpasses at each level.
  // Overpasses may or may not exist for supers for pass 1,
  // they should have been created for pass 2 and later.

  for (const Klass* cursuper = super; cursuper != NULL; cursuper = cursuper->super())
  {
     Method* found_mth = InstanceKlass::cast(cursuper)->find_local_method(name, signature,
                                                                          Klass::OverpassLookupMode::find,
                                                                          Klass::StaticLookupMode::skip,
                                                                          Klass::PrivateLookupMode::skip);
     // Ignore non-public methods in java.lang.Object if klass is an interface.
     if (found_mth != NULL && (!is_interface ||
         !SystemDictionary::is_nonpublic_Object_method(found_mth))) {
       return false;
     }
  }

  return true;
}

// Scans current_interface_methods for miranda methods that do not
// already appear in new_mirandas, or default methods,  and are also not defined-and-non-private
// in super (superclass).  These mirandas are added to all_mirandas if it is
// not null; in addition, those that are not duplicates of miranda methods
// inherited by super from its interfaces are added to new_mirandas.
// Thus, new_mirandas will be the set of mirandas that this class introduces,
// all_mirandas will be the set of all mirandas applicable to this class
// including all defined in superclasses.
void klassVtable::add_new_mirandas_to_lists(
    GrowableArray<Method*>* new_mirandas, GrowableArray<Method*>* all_mirandas,
    Array<Method*>* current_interface_methods, Array<Method*>* class_methods,
    Array<Method*>* default_methods, const Klass* super, bool is_interface) {

  // iterate thru the current interface's method to see if it a miranda
  int num_methods = current_interface_methods->length();
  for (int i = 0; i < num_methods; i++) {
    Method* im = current_interface_methods->at(i);
    bool is_duplicate = false;
    int num_of_current_mirandas = new_mirandas->length();
    // check for duplicate mirandas in different interfaces we implement
    for (int j = 0; j < num_of_current_mirandas; j++) {
      Method* miranda = new_mirandas->at(j);
      if ((im->name() == miranda->name()) &&
          (im->signature() == miranda->signature())) {
        is_duplicate = true;
        break;
      }
    }

    if (!is_duplicate) { // we don't want duplicate miranda entries in the vtable
      if (is_miranda(im, class_methods, default_methods, super, is_interface)) { // is it a miranda at all?
        const InstanceKlass *sk = InstanceKlass::cast(super);
        // check if it is a duplicate of a super's miranda
        if (sk->lookup_method_in_all_interfaces(im->name(), im->signature(), Klass::DefaultsLookupMode::find) == NULL) {
          new_mirandas->append(im);
        }
        if (all_mirandas != NULL) {
          all_mirandas->append(im);
        }
      }
    }
  }
}

void klassVtable::get_mirandas(GrowableArray<Method*>* new_mirandas,
                               GrowableArray<Method*>* all_mirandas,
                               const Klass* super,
                               Array<Method*>* class_methods,
                               Array<Method*>* default_methods,
                               Array<InstanceKlass*>* local_interfaces,
                               bool is_interface) {
  assert((new_mirandas->length() == 0) , "current mirandas must be 0");

  // iterate thru the local interfaces looking for a miranda
  int num_local_ifs = local_interfaces->length();
  for (int i = 0; i < num_local_ifs; i++) {
    InstanceKlass *ik = local_interfaces->at(i);
    add_new_mirandas_to_lists(new_mirandas, all_mirandas,
                              ik->methods(), class_methods,
                              default_methods, super, is_interface);
    // iterate thru each local's super interfaces
    Array<InstanceKlass*>* super_ifs = ik->transitive_interfaces();
    int num_super_ifs = super_ifs->length();
    for (int j = 0; j < num_super_ifs; j++) {
      InstanceKlass *sik = super_ifs->at(j);
      add_new_mirandas_to_lists(new_mirandas, all_mirandas,
                                sik->methods(), class_methods,
                                default_methods, super, is_interface);
    }
  }
}

// Discover miranda methods ("miranda" = "interface abstract, no binding"),
// and append them into the vtable starting at index initialized,
// return the new value of initialized.
// Miranda methods use vtable entries, but do not get assigned a vtable_index
// The vtable_index is discovered by searching from the end of the vtable
int klassVtable::fill_in_mirandas(Thread* current, int initialized) {
  ResourceMark rm(current);
  GrowableArray<Method*> mirandas(20);
  get_mirandas(&mirandas, NULL, ik()->super(), ik()->methods(),
               ik()->default_methods(), ik()->local_interfaces(),
               klass()->is_interface());
  for (int i = 0; i < mirandas.length(); i++) {
    if (log_develop_is_enabled(Trace, vtables)) {
      Method* meth = mirandas.at(i);
      LogTarget(Trace, vtables) lt;
      LogStream ls(lt);
      if (meth != NULL) {
        char* sig = meth->name_and_sig_as_C_string();
        ls.print("fill in mirandas with %s index %d, flags: ",
                     sig, initialized);
        meth->print_linkage_flags(&ls);
        ls.cr();
      }
    }
    put_method_at(mirandas.at(i), initialized);
    ++initialized;
  }
  return initialized;
}

// Copy this class's vtable to the vtable beginning at start.
// Used to copy superclass vtable to prefix of subclass's vtable.
void klassVtable::copy_vtable_to(vtableEntry* start) {
  Copy::disjoint_words((HeapWord*)table(), (HeapWord*)start, _length * vtableEntry::size());
}

#if INCLUDE_JVMTI
bool klassVtable::adjust_default_method(int vtable_index, Method* old_method, Method* new_method) {
  // If old_method is default, find this vtable index in default_vtable_indices
  // and replace that method in the _default_methods list
  bool updated = false;

  Array<Method*>* default_methods = ik()->default_methods();
  if (default_methods != NULL) {
    int len = default_methods->length();
    for (int idx = 0; idx < len; idx++) {
      if (vtable_index == ik()->default_vtable_indices()->at(idx)) {
        if (default_methods->at(idx) == old_method) {
          default_methods->at_put(idx, new_method);
          updated = true;
        }
        break;
      }
    }
  }
  return updated;
}

// search the vtable for uses of either obsolete or EMCP methods
void klassVtable::adjust_method_entries(bool * trace_name_printed) {
  int prn_enabled = 0;
  ResourceMark rm;

  for (int index = 0; index < length(); index++) {
    Method* old_method = unchecked_method_at(index);
    if (old_method == NULL || !old_method->is_old()) {
      continue; // skip uninteresting entries
    }
    assert(!old_method->is_deleted(), "vtable methods may not be deleted");

    Method* new_method = old_method->get_new_method();
    put_method_at(new_method, index);

    // For default methods, need to update the _default_methods array
    // which can only have one method entry for a given signature
    bool updated_default = false;
    if (old_method->is_default_method()) {
      updated_default = adjust_default_method(index, old_method, new_method);
    }

    if (!(*trace_name_printed)) {
      log_info(redefine, class, update)
        ("adjust: klassname=%s for methods from name=%s",
         _klass->external_name(), old_method->method_holder()->external_name());
      *trace_name_printed = true;
    }
    log_trace(redefine, class, update, vtables)
      ("vtable method update: class: %s method: %s, updated default = %s",
       _klass->external_name(), new_method->external_name(), updated_default ? "true" : "false");
  }
}

// a vtable should never contain old or obsolete methods
bool klassVtable::check_no_old_or_obsolete_entries() {
  ResourceMark rm;

  for (int i = 0; i < length(); i++) {
    Method* m = unchecked_method_at(i);
    if (m != NULL &&
        (NOT_PRODUCT(!m->is_valid() ||) m->is_old() || m->is_obsolete())) {
      log_trace(redefine, class, update, vtables)
        ("vtable check found old method entry: class: %s old: %d obsolete: %d, method: %s",
         _klass->external_name(), m->is_old(), m->is_obsolete(), m->external_name());
      return false;
    }
  }
  return true;
}

void klassVtable::dump_vtable() {
  tty->print_cr("vtable dump --");
  for (int i = 0; i < length(); i++) {
    Method* m = unchecked_method_at(i);
    if (m != NULL) {
      tty->print("      (%5d)  ", i);
      m->access_flags().print_on(tty);
      if (m->is_default_method()) {
        tty->print("default ");
      }
      if (m->is_overpass()) {
        tty->print("overpass");
      }
      tty->print(" --  ");
      m->print_name(tty);
      tty->cr();
    }
  }
}
#endif // INCLUDE_JVMTI

//-----------------------------------------------------------------------------------------
// Itable code

// Initialize a itableMethodEntry
void itableMethodEntry::initialize(InstanceKlass* klass, Method* m) {
  if (m == NULL) return;

#ifdef ASSERT
  if (MetaspaceShared::is_in_shared_metaspace((void*)&_method) &&
     !MetaspaceShared::remapped_readwrite() &&
     m->method_holder()->verified_at_dump_time() &&
     klass->verified_at_dump_time()) {
    // At runtime initialize_itable is rerun as part of link_class_impl()
    // for a shared class loaded by the non-boot loader.
    // The dumptime itable method entry should be the same as the runtime entry.
    // For a shared old class which was not linked during dump time, we can't compare the dumptime
    // itable method entry with the runtime entry.
    assert(_method == m, "sanity");
  }
#endif
  _method = m;
}

klassItable::klassItable(InstanceKlass* klass) {
  _klass = klass;

  if (klass->itable_length() > 0) {
    itableOffsetEntry* offset_entry = (itableOffsetEntry*)klass->start_of_itable();
    if (offset_entry  != NULL && offset_entry->interface_klass() != NULL) { // Check that itable is initialized
      // First offset entry points to the first method_entry
      intptr_t* method_entry  = (intptr_t *)(((address)klass) + offset_entry->offset());
      intptr_t* end         = klass->end_of_itable();

      _table_offset      = (intptr_t*)offset_entry - (intptr_t*)klass;
      _size_offset_table = (method_entry - ((intptr_t*)offset_entry)) / itableOffsetEntry::size();
      _size_method_table = (end - method_entry)                  / itableMethodEntry::size();
      assert(_table_offset >= 0 && _size_offset_table >= 0 && _size_method_table >= 0, "wrong computation");
      return;
    }
  }

  // The length of the itable was either zero, or it has not yet been initialized.
  _table_offset      = 0;
  _size_offset_table = 0;
  _size_method_table = 0;
}

static int initialize_count = 0;

// Initialization
void klassItable::initialize_itable(GrowableArray<Method*>* supers) {
  if (_klass->is_interface()) {
    // This needs to go after vtable indices are assigned but
    // before implementors need to know the number of itable indices.
    assign_itable_indices_for_interface(InstanceKlass::cast(_klass));
  }

  // Cannot be setup doing bootstrapping, interfaces don't have
  // itables, and klass with only ones entry have empty itables
  if (Universe::is_bootstrapping() ||
      _klass->is_interface() ||
      _klass->itable_length() == itableOffsetEntry::size()) return;

  // There's alway an extra itable entry so we can null-terminate it.
  guarantee(size_offset_table() >= 1, "too small");
  int num_interfaces = size_offset_table() - 1;
  if (num_interfaces > 0) {
    if (log_develop_is_enabled(Debug, itables)) {
      ResourceMark rm;
      log_develop_debug(itables)("%3d: Initializing itables for %s", ++initialize_count,
                       _klass->name()->as_C_string());
    }

    // Iterate through all interfaces
    for(int i = 0; i < num_interfaces; i++) {
      itableOffsetEntry* ioe = offset_entry(i);
      InstanceKlass* interf = ioe->interface_klass();
      assert(interf != NULL && ioe->offset() != 0, "bad offset entry in itable");
      initialize_itable_for_interface(ioe->offset(), interf, supers,
                       (ioe->offset() - offset_entry(0)->offset())/wordSize);
    }
  }
  // Check that the last entry is empty
  itableOffsetEntry* ioe = offset_entry(size_offset_table() - 1);
  guarantee(ioe->interface_klass() == NULL && ioe->offset() == 0, "terminator entry missing");
}

void klassItable::check_constraints(GrowableArray<Method*>* supers, TRAPS) {

  assert(_size_method_table == supers->length(), "wrong size");
  itableMethodEntry* ime = method_entry(0);
  for (int i = 0; i < _size_method_table; i++) {
    Method* target = ime->method();
    Method* interface_method = supers->at(i); // method overridden

    if (target != NULL && interface_method != NULL) {
      InstanceKlass* method_holder = target->method_holder();
      InstanceKlass* interf = interface_method->method_holder();
      HandleMark hm(THREAD);
      Handle method_holder_loader(THREAD, method_holder->class_loader());
      Handle interface_loader(THREAD, interf->class_loader());

      if (method_holder_loader() != interface_loader()) {
        ResourceMark rm(THREAD);
        Symbol* failed_type_symbol =
          SystemDictionary::check_signature_loaders(target->signature(),
                                                    _klass,
                                                    method_holder_loader,
                                                    interface_loader,
                                                    true);
        if (failed_type_symbol != NULL) {
          stringStream ss;
          ss.print("loader constraint violation in interface itable"
                   " initialization for class %s: when selecting method '",
                   _klass->external_name());
          interface_method->print_external_name(&ss),
          ss.print("' the class loader %s for super interface %s, and the class"
                   " loader %s of the selected method's %s, %s have"
                   " different Class objects for the type %s used in the signature (%s; %s)",
                   interf->class_loader_data()->loader_name_and_id(),
                   interf->external_name(),
                   method_holder->class_loader_data()->loader_name_and_id(),
                   method_holder->external_kind(),
                   method_holder->external_name(),
                   failed_type_symbol->as_klass_external_name(),
                   interf->class_in_module_of_loader(false, true),
                   method_holder->class_in_module_of_loader(false, true));
          THROW_MSG(vmSymbols::java_lang_LinkageError(), ss.as_string());
        }
      }
    }
    ime++;
  }
}

void klassItable::initialize_itable_and_check_constraints(TRAPS) {
  // Save a super interface from each itable entry to do constraint checking
  ResourceMark rm(THREAD);
  GrowableArray<Method*>* supers =
    new GrowableArray<Method*>(_size_method_table, _size_method_table, NULL);
  initialize_itable(supers);
  check_constraints(supers, CHECK);
}

inline bool interface_method_needs_itable_index(Method* m) {
  if (m->is_static())           return false;   // e.g., Stream.empty
  if (m->is_initializer())      return false;   // <init> or <clinit>
  if (m->is_private())          return false;   // uses direct call
  // If an interface redeclares a method from java.lang.Object,
  // it should already have a vtable index, don't touch it.
  // e.g., CharSequence.toString (from initialize_vtable)
  // if (m->has_vtable_index())  return false; // NO!
  return true;
}

int klassItable::assign_itable_indices_for_interface(InstanceKlass* klass) {
  // an interface does not have an itable, but its methods need to be numbered
  if (log_develop_is_enabled(Trace, itables)) {
    ResourceMark rm;
    log_develop_debug(itables)("%3d: Initializing itable indices for interface %s",
                               ++initialize_count, klass->name()->as_C_string());
  }

  Array<Method*>* methods = klass->methods();
  int nof_methods = methods->length();
  int ime_num = 0;
  for (int i = 0; i < nof_methods; i++) {
    Method* m = methods->at(i);
    if (interface_method_needs_itable_index(m)) {
      assert(!m->is_final_method(), "no final interface methods");
      // If m is already assigned a vtable index, do not disturb it.
      if (log_develop_is_enabled(Trace, itables)) {
        ResourceMark rm;
        LogTarget(Trace, itables) lt;
        LogStream ls(lt);
        assert(m != NULL, "methods can never be null");
        const char* sig = m->name_and_sig_as_C_string();
        if (m->has_vtable_index()) {
          ls.print("vtable index %d for method: %s, flags: ", m->vtable_index(), sig);
        } else {
          ls.print("itable index %d for method: %s, flags: ", ime_num, sig);
        }
        m->print_linkage_flags(&ls);
        ls.cr();
      }
      if (!m->has_vtable_index()) {
        // A shared method could have an initialized itable_index that
        // is < 0.
        assert(m->vtable_index() == Method::pending_itable_index ||
               m->is_shared(),
               "set by initialize_vtable");
        m->set_itable_index(ime_num);
        // Progress to next itable entry
        ime_num++;
      }
    }
  }
  assert(ime_num == method_count_for_interface(klass), "proper sizing");
  return ime_num;
}

int klassItable::method_count_for_interface(InstanceKlass* interf) {
  assert(interf->is_interface(), "must be");
  Array<Method*>* methods = interf->methods();
  int nof_methods = methods->length();
  int length = 0;
  while (nof_methods > 0) {
    Method* m = methods->at(nof_methods-1);
    if (m->has_itable_index()) {
      length = m->itable_index() + 1;
      break;
    }
    nof_methods -= 1;
  }
#ifdef ASSERT
  int nof_methods_copy = nof_methods;
  while (nof_methods_copy > 0) {
    Method* mm = methods->at(--nof_methods_copy);
    assert(!mm->has_itable_index() || mm->itable_index() < length, "");
  }
#endif //ASSERT
  // return the rightmost itable index, plus one; or 0 if no methods have
  // itable indices
  return length;
}


void klassItable::initialize_itable_for_interface(int method_table_offset, InstanceKlass* interf,
                                                  GrowableArray<Method*>* supers,
                                                  int start_offset) {
  assert(interf->is_interface(), "must be");
  Array<Method*>* methods = interf->methods();
  int nof_methods = methods->length();

  int ime_count = method_count_for_interface(interf);
  for (int i = 0; i < nof_methods; i++) {
    Method* m = methods->at(i);
    Method* target = NULL;
    if (m->has_itable_index()) {
      // This search must match the runtime resolution, i.e. selection search for invokeinterface
      // to correctly enforce loader constraints for interface method inheritance.
      // Private methods are skipped as a private class method can never be the implementation
      // of an interface method.
      // Invokespecial does not perform selection based on the receiver, so it does not use
      // the cached itable.
      target = LinkResolver::lookup_instance_method_in_klasses(_klass, m->name(), m->signature(),
                                                               Klass::PrivateLookupMode::skip);
    }
    if (target == NULL || !target->is_public() || target->is_abstract() || target->is_overpass()) {
      assert(target == NULL || !target->is_overpass() || target->is_public(),
             "Non-public overpass method!");
      // Entry does not resolve. Leave it empty for AbstractMethodError or other error.
      if (!(target == NULL) && !target->is_public()) {
        // Stuff an IllegalAccessError throwing method in there instead.
        itableOffsetEntry::method_entry(_klass, method_table_offset)[m->itable_index()].
            initialize(_klass, Universe::throw_illegal_access_error());
      }
    } else {

      int ime_num = m->itable_index();
      assert(ime_num < ime_count, "oob");

      // Save super interface method to perform constraint checks.
      // The method is in the error message, that's why.
      if (supers != NULL) {
        supers->at_put(start_offset + ime_num, m);
      }

      itableOffsetEntry::method_entry(_klass, method_table_offset)[ime_num].initialize(_klass, target);
      if (log_develop_is_enabled(Trace, itables)) {
        ResourceMark rm;
        if (target != NULL) {
          LogTarget(Trace, itables) lt;
          LogStream ls(lt);
          char* sig = target->name_and_sig_as_C_string();
          ls.print("interface: %s, ime_num: %d, target: %s, method_holder: %s ",
                       interf->internal_name(), ime_num, sig,
                       target->method_holder()->internal_name());
          ls.print("target_method flags: ");
          target->print_linkage_flags(&ls);
          ls.cr();
        }
      }
    }
  }
}

#if INCLUDE_JVMTI
// search the itable for uses of either obsolete or EMCP methods
void klassItable::adjust_method_entries(bool * trace_name_printed) {
  ResourceMark rm;
  itableMethodEntry* ime = method_entry(0);

  for (int i = 0; i < _size_method_table; i++, ime++) {
    Method* old_method = ime->method();
    if (old_method == NULL || !old_method->is_old()) {
      continue; // skip uninteresting entries
    }
    assert(!old_method->is_deleted(), "itable methods may not be deleted");
    Method* new_method = old_method->get_new_method();
    ime->initialize(_klass, new_method);

    if (!(*trace_name_printed)) {
      log_info(redefine, class, update)("adjust: name=%s", old_method->method_holder()->external_name());
      *trace_name_printed = true;
    }
    log_trace(redefine, class, update, itables)
      ("itable method update: class: %s method: %s", _klass->external_name(), new_method->external_name());
  }
}

// an itable should never contain old or obsolete methods
bool klassItable::check_no_old_or_obsolete_entries() {
  ResourceMark rm;
  itableMethodEntry* ime = method_entry(0);

  for (int i = 0; i < _size_method_table; i++) {
    Method* m = ime->method();
    if (m != NULL &&
        (NOT_PRODUCT(!m->is_valid() ||) m->is_old() || m->is_obsolete())) {
      log_trace(redefine, class, update, itables)
        ("itable check found old method entry: class: %s old: %d obsolete: %d, method: %s",
         _klass->external_name(), m->is_old(), m->is_obsolete(), m->external_name());
      return false;
    }
    ime++;
  }
  return true;
}

void klassItable::dump_itable() {
  itableMethodEntry* ime = method_entry(0);
  tty->print_cr("itable dump --");
  for (int i = 0; i < _size_method_table; i++) {
    Method* m = ime->method();
    if (m != NULL) {
      tty->print("      (%5d)  ", i);
      m->access_flags().print_on(tty);
      if (m->is_default_method()) {
        tty->print("default ");
      }
      tty->print(" --  ");
      m->print_name(tty);
      tty->cr();
    }
    ime++;
  }
}
#endif // INCLUDE_JVMTI

// Setup
class InterfaceVisiterClosure : public StackObj {
 public:
  virtual void doit(InstanceKlass* intf, int method_count) = 0;
};

// Visit all interfaces with at least one itable method
void visit_all_interfaces(Array<InstanceKlass*>* transitive_intf, InterfaceVisiterClosure *blk) {
  // Handle array argument
  for(int i = 0; i < transitive_intf->length(); i++) {
    InstanceKlass* intf = transitive_intf->at(i);
    assert(intf->is_interface(), "sanity check");

    // Find no. of itable methods
    int method_count = 0;
    // method_count = klassItable::method_count_for_interface(intf);
    Array<Method*>* methods = intf->methods();
    if (methods->length() > 0) {
      for (int i = methods->length(); --i >= 0; ) {
        if (interface_method_needs_itable_index(methods->at(i))) {
          method_count++;
        }
      }
    }

    // Visit all interfaces which either have any methods or can participate in receiver type check.
    // We do not bother to count methods in transitive interfaces, although that would allow us to skip
    // this step in the rare case of a zero-method interface extending another zero-method interface.
    if (method_count > 0 || intf->transitive_interfaces()->length() > 0) {
      blk->doit(intf, method_count);
    }
  }
}

class CountInterfacesClosure : public InterfaceVisiterClosure {
 private:
  int _nof_methods;
  int _nof_interfaces;
 public:
   CountInterfacesClosure() { _nof_methods = 0; _nof_interfaces = 0; }

   int nof_methods() const    { return _nof_methods; }
   int nof_interfaces() const { return _nof_interfaces; }

   void doit(InstanceKlass* intf, int method_count) { _nof_methods += method_count; _nof_interfaces++; }
};

class SetupItableClosure : public InterfaceVisiterClosure  {
 private:
  itableOffsetEntry* _offset_entry;
  itableMethodEntry* _method_entry;
  address            _klass_begin;
 public:
  SetupItableClosure(address klass_begin, itableOffsetEntry* offset_entry, itableMethodEntry* method_entry) {
    _klass_begin  = klass_begin;
    _offset_entry = offset_entry;
    _method_entry = method_entry;
  }

  itableMethodEntry* method_entry() const { return _method_entry; }

  void doit(InstanceKlass* intf, int method_count) {
    int offset = ((address)_method_entry) - _klass_begin;
    _offset_entry->initialize(intf, offset);
    _offset_entry++;
    _method_entry += method_count;
  }
};

int klassItable::compute_itable_size(Array<InstanceKlass*>* transitive_interfaces) {
  // Count no of interfaces and total number of interface methods
  CountInterfacesClosure cic;
  visit_all_interfaces(transitive_interfaces, &cic);

  // There's alway an extra itable entry so we can null-terminate it.
  int itable_size = calc_itable_size(cic.nof_interfaces() + 1, cic.nof_methods());

  // Statistics
  update_stats(itable_size * wordSize);

  return itable_size;
}


// Fill out offset table and interface klasses into the itable space
void klassItable::setup_itable_offset_table(InstanceKlass* klass) {
  if (klass->itable_length() == 0) return;
  assert(!klass->is_interface(), "Should have zero length itable");

  // Count no of interfaces and total number of interface methods
  CountInterfacesClosure cic;
  visit_all_interfaces(klass->transitive_interfaces(), &cic);
  int nof_methods    = cic.nof_methods();
  int nof_interfaces = cic.nof_interfaces();

  // Add one extra entry so we can null-terminate the table
  nof_interfaces++;

  assert(compute_itable_size(klass->transitive_interfaces()) ==
         calc_itable_size(nof_interfaces, nof_methods),
         "mismatch calculation of itable size");

  // Fill-out offset table
  itableOffsetEntry* ioe = (itableOffsetEntry*)klass->start_of_itable();
  itableMethodEntry* ime = (itableMethodEntry*)(ioe + nof_interfaces);
  intptr_t* end               = klass->end_of_itable();
  assert((oop*)(ime + nof_methods) <= (oop*)klass->start_of_nonstatic_oop_maps(), "wrong offset calculation (1)");
  assert((oop*)(end) == (oop*)(ime + nof_methods),                      "wrong offset calculation (2)");

  // Visit all interfaces and initialize itable offset table
  SetupItableClosure sic((address)klass, ioe, ime);
  visit_all_interfaces(klass->transitive_interfaces(), &sic);

#ifdef ASSERT
  ime  = sic.method_entry();
  oop* v = (oop*) klass->end_of_itable();
  assert( (oop*)(ime) == v, "wrong offset calculation (2)");
#endif
}

void klassVtable::verify(outputStream* st, bool forced) {
  // make sure table is initialized
  if (!Universe::is_fully_initialized()) return;
#ifndef PRODUCT
  // avoid redundant verifies
  if (!forced && _verify_count == Universe::verify_count()) return;
  _verify_count = Universe::verify_count();
#endif
  oop* end_of_obj = (oop*)_klass + _klass->size();
  oop* end_of_vtable = (oop *)&table()[_length];
  if (end_of_vtable > end_of_obj) {
    ResourceMark rm;
    fatal("klass %s: klass object too short (vtable extends beyond end)",
          _klass->internal_name());
  }

  for (int i = 0; i < _length; i++) table()[i].verify(this, st);
  // verify consistency with superKlass vtable
  Klass* super = _klass->super();
  if (super != NULL) {
    InstanceKlass* sk = InstanceKlass::cast(super);
    klassVtable vt = sk->vtable();
    for (int i = 0; i < vt.length(); i++) {
      verify_against(st, &vt, i);
    }
  }
}

void klassVtable::verify_against(outputStream* st, klassVtable* vt, int index) {
  vtableEntry* vte = &vt->table()[index];
  if (vte->method()->name()      != table()[index].method()->name() ||
      vte->method()->signature() != table()[index].method()->signature()) {
    fatal("mismatched name/signature of vtable entries");
  }
}

#ifndef PRODUCT
void klassVtable::print() {
  ResourceMark rm;
  tty->print("klassVtable for klass %s (length %d):\n", _klass->internal_name(), length());
  for (int i = 0; i < length(); i++) {
    table()[i].print();
    tty->cr();
  }
}
#endif

void vtableEntry::verify(klassVtable* vt, outputStream* st) {
  Klass* vtklass = vt->klass();
  if (vtklass->is_instance_klass() &&
     (InstanceKlass::cast(vtklass)->major_version() >= klassVtable::VTABLE_TRANSITIVE_OVERRIDE_VERSION)) {
    assert(method() != NULL, "must have set method");
  }
  if (method() != NULL) {
    method()->verify();
    // we sub_type, because it could be a miranda method
    if (!vtklass->is_subtype_of(method()->method_holder())) {
#ifndef PRODUCT
      print();
#endif
      fatal("vtableEntry " PTR_FORMAT ": method is from subclass", p2i(this));
    }
 }
}

#ifndef PRODUCT

void vtableEntry::print() {
  ResourceMark rm;
  tty->print("vtableEntry %s: ", method()->name()->as_C_string());
  if (Verbose) {
    tty->print("m " PTR_FORMAT " ", p2i(method()));
  }
}

class VtableStats : AllStatic {
 public:
  static int no_klasses;                // # classes with vtables
  static int no_array_klasses;          // # array classes
  static int no_instance_klasses;       // # instanceKlasses
  static int sum_of_vtable_len;         // total # of vtable entries
  static int sum_of_array_vtable_len;   // total # of vtable entries in array klasses only
  static int fixed;                     // total fixed overhead in bytes
  static int filler;                    // overhead caused by filler bytes
  static int entries;                   // total bytes consumed by vtable entries
  static int array_entries;             // total bytes consumed by array vtable entries

  static void do_class(Klass* k) {
    Klass* kl = k;
    klassVtable vt = kl->vtable();
    no_klasses++;
    if (kl->is_instance_klass()) {
      no_instance_klasses++;
      kl->array_klasses_do(do_class);
    }
    if (kl->is_array_klass()) {
      no_array_klasses++;
      sum_of_array_vtable_len += vt.length();
    }
    sum_of_vtable_len += vt.length();
  }

  static void compute() {
    LockedClassesDo locked_do_class(&do_class);
    ClassLoaderDataGraph::classes_do(&locked_do_class);
    fixed  = no_klasses * oopSize;      // vtable length
    // filler size is a conservative approximation
    filler = oopSize * (no_klasses - no_instance_klasses) * (sizeof(InstanceKlass) - sizeof(ArrayKlass) - 1);
    entries = sizeof(vtableEntry) * sum_of_vtable_len;
    array_entries = sizeof(vtableEntry) * sum_of_array_vtable_len;
  }
};

int VtableStats::no_klasses = 0;
int VtableStats::no_array_klasses = 0;
int VtableStats::no_instance_klasses = 0;
int VtableStats::sum_of_vtable_len = 0;
int VtableStats::sum_of_array_vtable_len = 0;
int VtableStats::fixed = 0;
int VtableStats::filler = 0;
int VtableStats::entries = 0;
int VtableStats::array_entries = 0;

void klassVtable::print_statistics() {
  ResourceMark rm;
  VtableStats::compute();
  tty->print_cr("vtable statistics:");
  tty->print_cr("%6d classes (%d instance, %d array)", VtableStats::no_klasses, VtableStats::no_instance_klasses, VtableStats::no_array_klasses);
  int total = VtableStats::fixed + VtableStats::filler + VtableStats::entries;
  tty->print_cr("%6d bytes fixed overhead (refs + vtable object header)", VtableStats::fixed);
  tty->print_cr("%6d bytes filler overhead", VtableStats::filler);
  tty->print_cr("%6d bytes for vtable entries (%d for arrays)", VtableStats::entries, VtableStats::array_entries);
  tty->print_cr("%6d bytes total", total);
}

int    klassItable::_total_classes;   // Total no. of classes with itables
size_t klassItable::_total_size;      // Total no. of bytes used for itables

void klassItable::print_statistics() {
 tty->print_cr("itable statistics:");
 tty->print_cr("%6d classes with itables", _total_classes);
 tty->print_cr(SIZE_FORMAT_W(6) " K uses for itables (average by class: " SIZE_FORMAT " bytes)",
               _total_size / K, _total_size / _total_classes);
}

#endif // PRODUCT
