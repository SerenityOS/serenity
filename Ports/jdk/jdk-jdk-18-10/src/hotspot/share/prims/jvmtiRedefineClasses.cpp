/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "cds/metaspaceShared.hpp"
#include "classfile/classFileStream.hpp"
#include "classfile/classLoaderDataGraph.hpp"
#include "classfile/classLoadInfo.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/metadataOnStackMark.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/klassFactory.hpp"
#include "classfile/verifier.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "code/codeCache.hpp"
#include "compiler/compileBroker.hpp"
#include "interpreter/oopMapCache.hpp"
#include "interpreter/rewriter.hpp"
#include "jfr/jfrEvents.hpp"
#include "logging/logStream.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/annotations.hpp"
#include "oops/constantPool.hpp"
#include "oops/fieldStreams.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/klassVtable.hpp"
#include "oops/oop.inline.hpp"
#include "oops/recordComponent.hpp"
#include "prims/jvmtiImpl.hpp"
#include "prims/jvmtiRedefineClasses.hpp"
#include "prims/jvmtiThreadState.inline.hpp"
#include "prims/resolvedMethodTable.hpp"
#include "prims/methodComparator.hpp"
#include "runtime/atomic.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/relocator.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/events.hpp"

Array<Method*>* VM_RedefineClasses::_old_methods = NULL;
Array<Method*>* VM_RedefineClasses::_new_methods = NULL;
Method**  VM_RedefineClasses::_matching_old_methods = NULL;
Method**  VM_RedefineClasses::_matching_new_methods = NULL;
Method**  VM_RedefineClasses::_deleted_methods      = NULL;
Method**  VM_RedefineClasses::_added_methods        = NULL;
int       VM_RedefineClasses::_matching_methods_length = 0;
int       VM_RedefineClasses::_deleted_methods_length  = 0;
int       VM_RedefineClasses::_added_methods_length    = 0;

// This flag is global as the constructor does not reset it:
bool      VM_RedefineClasses::_has_redefined_Object = false;
u8        VM_RedefineClasses::_id_counter = 0;

VM_RedefineClasses::VM_RedefineClasses(jint class_count,
                                       const jvmtiClassDefinition *class_defs,
                                       JvmtiClassLoadKind class_load_kind) {
  _class_count = class_count;
  _class_defs = class_defs;
  _class_load_kind = class_load_kind;
  _any_class_has_resolved_methods = false;
  _res = JVMTI_ERROR_NONE;
  _the_class = NULL;
  _id = next_id();
}

static inline InstanceKlass* get_ik(jclass def) {
  oop mirror = JNIHandles::resolve_non_null(def);
  return InstanceKlass::cast(java_lang_Class::as_Klass(mirror));
}

// If any of the classes are being redefined, wait
// Parallel constant pool merging leads to indeterminate constant pools.
void VM_RedefineClasses::lock_classes() {
  JvmtiThreadState *state = JvmtiThreadState::state_for(JavaThread::current());
  GrowableArray<Klass*>* redef_classes = state->get_classes_being_redefined();

  MonitorLocker ml(RedefineClasses_lock);

  if (redef_classes == NULL) {
    redef_classes = new(ResourceObj::C_HEAP, mtClass) GrowableArray<Klass*>(1, mtClass);
    state->set_classes_being_redefined(redef_classes);
  }

  bool has_redefined;
  do {
    has_redefined = false;
    // Go through classes each time until none are being redefined. Skip
    // the ones that are being redefined by this thread currently. Class file
    // load hook event may trigger new class redefine when we are redefining
    // a class (after lock_classes()).
    for (int i = 0; i < _class_count; i++) {
      InstanceKlass* ik = get_ik(_class_defs[i].klass);
      // Check if we are currently redefining the class in this thread already.
      if (redef_classes->contains(ik)) {
        assert(ik->is_being_redefined(), "sanity");
      } else {
        if (ik->is_being_redefined()) {
          ml.wait();
          has_redefined = true;
          break;  // for loop
        }
      }
    }
  } while (has_redefined);

  for (int i = 0; i < _class_count; i++) {
    InstanceKlass* ik = get_ik(_class_defs[i].klass);
    redef_classes->push(ik); // Add to the _classes_being_redefined list
    ik->set_is_being_redefined(true);
  }
  ml.notify_all();
}

void VM_RedefineClasses::unlock_classes() {
  JvmtiThreadState *state = JvmtiThreadState::state_for(JavaThread::current());
  GrowableArray<Klass*>* redef_classes = state->get_classes_being_redefined();
  assert(redef_classes != NULL, "_classes_being_redefined is not allocated");

  MonitorLocker ml(RedefineClasses_lock);

  for (int i = _class_count - 1; i >= 0; i--) {
    InstanceKlass* def_ik = get_ik(_class_defs[i].klass);
    if (redef_classes->length() > 0) {
      // Remove the class from _classes_being_redefined list
      Klass* k = redef_classes->pop();
      assert(def_ik == k, "unlocking wrong class");
    }
    assert(def_ik->is_being_redefined(),
           "should be being redefined to get here");

    // Unlock after we finish all redefines for this class within
    // the thread. Same class can be pushed to the list multiple
    // times (not more than once by each recursive redefinition).
    if (!redef_classes->contains(def_ik)) {
      def_ik->set_is_being_redefined(false);
    }
  }
  ml.notify_all();
}

bool VM_RedefineClasses::doit_prologue() {
  if (_class_count == 0) {
    _res = JVMTI_ERROR_NONE;
    return false;
  }
  if (_class_defs == NULL) {
    _res = JVMTI_ERROR_NULL_POINTER;
    return false;
  }

  for (int i = 0; i < _class_count; i++) {
    if (_class_defs[i].klass == NULL) {
      _res = JVMTI_ERROR_INVALID_CLASS;
      return false;
    }
    if (_class_defs[i].class_byte_count == 0) {
      _res = JVMTI_ERROR_INVALID_CLASS_FORMAT;
      return false;
    }
    if (_class_defs[i].class_bytes == NULL) {
      _res = JVMTI_ERROR_NULL_POINTER;
      return false;
    }

    oop mirror = JNIHandles::resolve_non_null(_class_defs[i].klass);
    // classes for primitives, arrays, and hidden classes
    // cannot be redefined.
    if (!is_modifiable_class(mirror)) {
      _res = JVMTI_ERROR_UNMODIFIABLE_CLASS;
      return false;
    }
  }

  // Start timer after all the sanity checks; not quite accurate, but
  // better than adding a bunch of stop() calls.
  if (log_is_enabled(Info, redefine, class, timer)) {
    _timer_vm_op_prologue.start();
  }

  lock_classes();
  // We first load new class versions in the prologue, because somewhere down the
  // call chain it is required that the current thread is a Java thread.
  _res = load_new_class_versions();
  if (_res != JVMTI_ERROR_NONE) {
    // free any successfully created classes, since none are redefined
    for (int i = 0; i < _class_count; i++) {
      if (_scratch_classes[i] != NULL) {
        ClassLoaderData* cld = _scratch_classes[i]->class_loader_data();
        // Free the memory for this class at class unloading time.  Not before
        // because CMS might think this is still live.
        InstanceKlass* ik = get_ik(_class_defs[i].klass);
        if (ik->get_cached_class_file() == _scratch_classes[i]->get_cached_class_file()) {
          // Don't double-free cached_class_file copied from the original class if error.
          _scratch_classes[i]->set_cached_class_file(NULL);
        }
        cld->add_to_deallocate_list(InstanceKlass::cast(_scratch_classes[i]));
      }
    }
    // Free os::malloc allocated memory in load_new_class_version.
    os::free(_scratch_classes);
    _timer_vm_op_prologue.stop();
    unlock_classes();
    return false;
  }

  _timer_vm_op_prologue.stop();
  return true;
}

void VM_RedefineClasses::doit() {
  Thread* current = Thread::current();

#if INCLUDE_CDS
  if (UseSharedSpaces) {
    // Sharing is enabled so we remap the shared readonly space to
    // shared readwrite, private just in case we need to redefine
    // a shared class. We do the remap during the doit() phase of
    // the safepoint to be safer.
    if (!MetaspaceShared::remap_shared_readonly_as_readwrite()) {
      log_info(redefine, class, load)("failed to remap shared readonly space to readwrite, private");
      _res = JVMTI_ERROR_INTERNAL;
      return;
    }
  }
#endif

  // Mark methods seen on stack and everywhere else so old methods are not
  // cleaned up if they're on the stack.
  MetadataOnStackMark md_on_stack(/*walk_all_metadata*/true, /*redefinition_walk*/true);
  HandleMark hm(current);   // make sure any handles created are deleted
                            // before the stack walk again.

  for (int i = 0; i < _class_count; i++) {
    redefine_single_class(current, _class_defs[i].klass, _scratch_classes[i]);
  }

  // Flush all compiled code that depends on the classes redefined.
  flush_dependent_code();

  // Adjust constantpool caches and vtables for all classes
  // that reference methods of the evolved classes.
  // Have to do this after all classes are redefined and all methods that
  // are redefined are marked as old.
  AdjustAndCleanMetadata adjust_and_clean_metadata(current);
  ClassLoaderDataGraph::classes_do(&adjust_and_clean_metadata);

  // JSR-292 support
  if (_any_class_has_resolved_methods) {
    bool trace_name_printed = false;
    ResolvedMethodTable::adjust_method_entries(&trace_name_printed);
  }

  // Increment flag indicating that some invariants are no longer true.
  // See jvmtiExport.hpp for detailed explanation.
  JvmtiExport::increment_redefinition_count();

  // check_class() is optionally called for product bits, but is
  // always called for non-product bits.
#ifdef PRODUCT
  if (log_is_enabled(Trace, redefine, class, obsolete, metadata)) {
#endif
    log_trace(redefine, class, obsolete, metadata)("calling check_class");
    CheckClass check_class(current);
    ClassLoaderDataGraph::classes_do(&check_class);
#ifdef PRODUCT
  }
#endif

  // Clean up any metadata now unreferenced while MetadataOnStackMark is set.
  ClassLoaderDataGraph::clean_deallocate_lists(false);
}

void VM_RedefineClasses::doit_epilogue() {
  unlock_classes();

  // Free os::malloc allocated memory.
  os::free(_scratch_classes);

  // Reset the_class to null for error printing.
  _the_class = NULL;

  if (log_is_enabled(Info, redefine, class, timer)) {
    // Used to have separate timers for "doit" and "all", but the timer
    // overhead skewed the measurements.
    julong doit_time = _timer_rsc_phase1.milliseconds() +
                       _timer_rsc_phase2.milliseconds();
    julong all_time = _timer_vm_op_prologue.milliseconds() + doit_time;

    log_info(redefine, class, timer)
      ("vm_op: all=" JULONG_FORMAT "  prologue=" JULONG_FORMAT "  doit=" JULONG_FORMAT,
       all_time, (julong)_timer_vm_op_prologue.milliseconds(), doit_time);
    log_info(redefine, class, timer)
      ("redefine_single_class: phase1=" JULONG_FORMAT "  phase2=" JULONG_FORMAT,
       (julong)_timer_rsc_phase1.milliseconds(), (julong)_timer_rsc_phase2.milliseconds());
  }
}

bool VM_RedefineClasses::is_modifiable_class(oop klass_mirror) {
  // classes for primitives cannot be redefined
  if (java_lang_Class::is_primitive(klass_mirror)) {
    return false;
  }
  Klass* k = java_lang_Class::as_Klass(klass_mirror);
  // classes for arrays cannot be redefined
  if (k == NULL || !k->is_instance_klass()) {
    return false;
  }

  // Cannot redefine or retransform a hidden class.
  if (InstanceKlass::cast(k)->is_hidden()) {
    return false;
  }
  return true;
}

// Append the current entry at scratch_i in scratch_cp to *merge_cp_p
// where the end of *merge_cp_p is specified by *merge_cp_length_p. For
// direct CP entries, there is just the current entry to append. For
// indirect and double-indirect CP entries, there are zero or more
// referenced CP entries along with the current entry to append.
// Indirect and double-indirect CP entries are handled by recursive
// calls to append_entry() as needed. The referenced CP entries are
// always appended to *merge_cp_p before the referee CP entry. These
// referenced CP entries may already exist in *merge_cp_p in which case
// there is nothing extra to append and only the current entry is
// appended.
void VM_RedefineClasses::append_entry(const constantPoolHandle& scratch_cp,
       int scratch_i, constantPoolHandle *merge_cp_p, int *merge_cp_length_p) {

  // append is different depending on entry tag type
  switch (scratch_cp->tag_at(scratch_i).value()) {

    // The old verifier is implemented outside the VM. It loads classes,
    // but does not resolve constant pool entries directly so we never
    // see Class entries here with the old verifier. Similarly the old
    // verifier does not like Class entries in the input constant pool.
    // The split-verifier is implemented in the VM so it can optionally
    // and directly resolve constant pool entries to load classes. The
    // split-verifier can accept either Class entries or UnresolvedClass
    // entries in the input constant pool. We revert the appended copy
    // back to UnresolvedClass so that either verifier will be happy
    // with the constant pool entry.
    //
    // this is an indirect CP entry so it needs special handling
    case JVM_CONSTANT_Class:
    case JVM_CONSTANT_UnresolvedClass:
    {
      int name_i = scratch_cp->klass_name_index_at(scratch_i);
      int new_name_i = find_or_append_indirect_entry(scratch_cp, name_i, merge_cp_p,
                                                     merge_cp_length_p);

      if (new_name_i != name_i) {
        log_trace(redefine, class, constantpool)
          ("Class entry@%d name_index change: %d to %d",
           *merge_cp_length_p, name_i, new_name_i);
      }

      (*merge_cp_p)->temp_unresolved_klass_at_put(*merge_cp_length_p, new_name_i);
      if (scratch_i != *merge_cp_length_p) {
        // The new entry in *merge_cp_p is at a different index than
        // the new entry in scratch_cp so we need to map the index values.
        map_index(scratch_cp, scratch_i, *merge_cp_length_p);
      }
      (*merge_cp_length_p)++;
    } break;

    // these are direct CP entries so they can be directly appended,
    // but double and long take two constant pool entries
    case JVM_CONSTANT_Double:  // fall through
    case JVM_CONSTANT_Long:
    {
      ConstantPool::copy_entry_to(scratch_cp, scratch_i, *merge_cp_p, *merge_cp_length_p);

      if (scratch_i != *merge_cp_length_p) {
        // The new entry in *merge_cp_p is at a different index than
        // the new entry in scratch_cp so we need to map the index values.
        map_index(scratch_cp, scratch_i, *merge_cp_length_p);
      }
      (*merge_cp_length_p) += 2;
    } break;

    // these are direct CP entries so they can be directly appended
    case JVM_CONSTANT_Float:   // fall through
    case JVM_CONSTANT_Integer: // fall through
    case JVM_CONSTANT_Utf8:    // fall through

    // This was an indirect CP entry, but it has been changed into
    // Symbol*s so this entry can be directly appended.
    case JVM_CONSTANT_String:      // fall through
    {
      ConstantPool::copy_entry_to(scratch_cp, scratch_i, *merge_cp_p, *merge_cp_length_p);

      if (scratch_i != *merge_cp_length_p) {
        // The new entry in *merge_cp_p is at a different index than
        // the new entry in scratch_cp so we need to map the index values.
        map_index(scratch_cp, scratch_i, *merge_cp_length_p);
      }
      (*merge_cp_length_p)++;
    } break;

    // this is an indirect CP entry so it needs special handling
    case JVM_CONSTANT_NameAndType:
    {
      int name_ref_i = scratch_cp->name_ref_index_at(scratch_i);
      int new_name_ref_i = find_or_append_indirect_entry(scratch_cp, name_ref_i, merge_cp_p,
                                                         merge_cp_length_p);

      int signature_ref_i = scratch_cp->signature_ref_index_at(scratch_i);
      int new_signature_ref_i = find_or_append_indirect_entry(scratch_cp, signature_ref_i,
                                                              merge_cp_p, merge_cp_length_p);

      // If the referenced entries already exist in *merge_cp_p, then
      // both new_name_ref_i and new_signature_ref_i will both be 0.
      // In that case, all we are appending is the current entry.
      if (new_name_ref_i != name_ref_i) {
        log_trace(redefine, class, constantpool)
          ("NameAndType entry@%d name_ref_index change: %d to %d",
           *merge_cp_length_p, name_ref_i, new_name_ref_i);
      }
      if (new_signature_ref_i != signature_ref_i) {
        log_trace(redefine, class, constantpool)
          ("NameAndType entry@%d signature_ref_index change: %d to %d",
           *merge_cp_length_p, signature_ref_i, new_signature_ref_i);
      }

      (*merge_cp_p)->name_and_type_at_put(*merge_cp_length_p,
        new_name_ref_i, new_signature_ref_i);
      if (scratch_i != *merge_cp_length_p) {
        // The new entry in *merge_cp_p is at a different index than
        // the new entry in scratch_cp so we need to map the index values.
        map_index(scratch_cp, scratch_i, *merge_cp_length_p);
      }
      (*merge_cp_length_p)++;
    } break;

    // this is a double-indirect CP entry so it needs special handling
    case JVM_CONSTANT_Fieldref:           // fall through
    case JVM_CONSTANT_InterfaceMethodref: // fall through
    case JVM_CONSTANT_Methodref:
    {
      int klass_ref_i = scratch_cp->uncached_klass_ref_index_at(scratch_i);
      int new_klass_ref_i = find_or_append_indirect_entry(scratch_cp, klass_ref_i,
                                                          merge_cp_p, merge_cp_length_p);

      int name_and_type_ref_i = scratch_cp->uncached_name_and_type_ref_index_at(scratch_i);
      int new_name_and_type_ref_i = find_or_append_indirect_entry(scratch_cp, name_and_type_ref_i,
                                                          merge_cp_p, merge_cp_length_p);

      const char *entry_name = NULL;
      switch (scratch_cp->tag_at(scratch_i).value()) {
      case JVM_CONSTANT_Fieldref:
        entry_name = "Fieldref";
        (*merge_cp_p)->field_at_put(*merge_cp_length_p, new_klass_ref_i,
          new_name_and_type_ref_i);
        break;
      case JVM_CONSTANT_InterfaceMethodref:
        entry_name = "IFMethodref";
        (*merge_cp_p)->interface_method_at_put(*merge_cp_length_p,
          new_klass_ref_i, new_name_and_type_ref_i);
        break;
      case JVM_CONSTANT_Methodref:
        entry_name = "Methodref";
        (*merge_cp_p)->method_at_put(*merge_cp_length_p, new_klass_ref_i,
          new_name_and_type_ref_i);
        break;
      default:
        guarantee(false, "bad switch");
        break;
      }

      if (klass_ref_i != new_klass_ref_i) {
        log_trace(redefine, class, constantpool)
          ("%s entry@%d class_index changed: %d to %d", entry_name, *merge_cp_length_p, klass_ref_i, new_klass_ref_i);
      }
      if (name_and_type_ref_i != new_name_and_type_ref_i) {
        log_trace(redefine, class, constantpool)
          ("%s entry@%d name_and_type_index changed: %d to %d",
           entry_name, *merge_cp_length_p, name_and_type_ref_i, new_name_and_type_ref_i);
      }

      if (scratch_i != *merge_cp_length_p) {
        // The new entry in *merge_cp_p is at a different index than
        // the new entry in scratch_cp so we need to map the index values.
        map_index(scratch_cp, scratch_i, *merge_cp_length_p);
      }
      (*merge_cp_length_p)++;
    } break;

    // this is an indirect CP entry so it needs special handling
    case JVM_CONSTANT_MethodType:
    {
      int ref_i = scratch_cp->method_type_index_at(scratch_i);
      int new_ref_i = find_or_append_indirect_entry(scratch_cp, ref_i, merge_cp_p,
                                                    merge_cp_length_p);
      if (new_ref_i != ref_i) {
        log_trace(redefine, class, constantpool)
          ("MethodType entry@%d ref_index change: %d to %d", *merge_cp_length_p, ref_i, new_ref_i);
      }
      (*merge_cp_p)->method_type_index_at_put(*merge_cp_length_p, new_ref_i);
      if (scratch_i != *merge_cp_length_p) {
        // The new entry in *merge_cp_p is at a different index than
        // the new entry in scratch_cp so we need to map the index values.
        map_index(scratch_cp, scratch_i, *merge_cp_length_p);
      }
      (*merge_cp_length_p)++;
    } break;

    // this is an indirect CP entry so it needs special handling
    case JVM_CONSTANT_MethodHandle:
    {
      int ref_kind = scratch_cp->method_handle_ref_kind_at(scratch_i);
      int ref_i = scratch_cp->method_handle_index_at(scratch_i);
      int new_ref_i = find_or_append_indirect_entry(scratch_cp, ref_i, merge_cp_p,
                                                    merge_cp_length_p);
      if (new_ref_i != ref_i) {
        log_trace(redefine, class, constantpool)
          ("MethodHandle entry@%d ref_index change: %d to %d", *merge_cp_length_p, ref_i, new_ref_i);
      }
      (*merge_cp_p)->method_handle_index_at_put(*merge_cp_length_p, ref_kind, new_ref_i);
      if (scratch_i != *merge_cp_length_p) {
        // The new entry in *merge_cp_p is at a different index than
        // the new entry in scratch_cp so we need to map the index values.
        map_index(scratch_cp, scratch_i, *merge_cp_length_p);
      }
      (*merge_cp_length_p)++;
    } break;

    // this is an indirect CP entry so it needs special handling
    case JVM_CONSTANT_Dynamic:  // fall through
    case JVM_CONSTANT_InvokeDynamic:
    {
      // Index of the bootstrap specifier in the operands array
      int old_bs_i = scratch_cp->bootstrap_methods_attribute_index(scratch_i);
      int new_bs_i = find_or_append_operand(scratch_cp, old_bs_i, merge_cp_p,
                                            merge_cp_length_p);
      // The bootstrap method NameAndType_info index
      int old_ref_i = scratch_cp->bootstrap_name_and_type_ref_index_at(scratch_i);
      int new_ref_i = find_or_append_indirect_entry(scratch_cp, old_ref_i, merge_cp_p,
                                                    merge_cp_length_p);
      if (new_bs_i != old_bs_i) {
        log_trace(redefine, class, constantpool)
          ("Dynamic entry@%d bootstrap_method_attr_index change: %d to %d",
           *merge_cp_length_p, old_bs_i, new_bs_i);
      }
      if (new_ref_i != old_ref_i) {
        log_trace(redefine, class, constantpool)
          ("Dynamic entry@%d name_and_type_index change: %d to %d", *merge_cp_length_p, old_ref_i, new_ref_i);
      }

      if (scratch_cp->tag_at(scratch_i).is_dynamic_constant())
        (*merge_cp_p)->dynamic_constant_at_put(*merge_cp_length_p, new_bs_i, new_ref_i);
      else
        (*merge_cp_p)->invoke_dynamic_at_put(*merge_cp_length_p, new_bs_i, new_ref_i);
      if (scratch_i != *merge_cp_length_p) {
        // The new entry in *merge_cp_p is at a different index than
        // the new entry in scratch_cp so we need to map the index values.
        map_index(scratch_cp, scratch_i, *merge_cp_length_p);
      }
      (*merge_cp_length_p)++;
    } break;

    // At this stage, Class or UnresolvedClass could be in scratch_cp, but not
    // ClassIndex
    case JVM_CONSTANT_ClassIndex: // fall through

    // Invalid is used as the tag for the second constant pool entry
    // occupied by JVM_CONSTANT_Double or JVM_CONSTANT_Long. It should
    // not be seen by itself.
    case JVM_CONSTANT_Invalid: // fall through

    // At this stage, String could be here, but not StringIndex
    case JVM_CONSTANT_StringIndex: // fall through

    // At this stage JVM_CONSTANT_UnresolvedClassInError should not be
    // here
    case JVM_CONSTANT_UnresolvedClassInError: // fall through

    default:
    {
      // leave a breadcrumb
      jbyte bad_value = scratch_cp->tag_at(scratch_i).value();
      ShouldNotReachHere();
    } break;
  } // end switch tag value
} // end append_entry()


int VM_RedefineClasses::find_or_append_indirect_entry(const constantPoolHandle& scratch_cp,
      int ref_i, constantPoolHandle *merge_cp_p, int *merge_cp_length_p) {

  int new_ref_i = ref_i;
  bool match = (ref_i < *merge_cp_length_p) &&
               scratch_cp->compare_entry_to(ref_i, *merge_cp_p, ref_i);

  if (!match) {
    // forward reference in *merge_cp_p or not a direct match
    int found_i = scratch_cp->find_matching_entry(ref_i, *merge_cp_p);
    if (found_i != 0) {
      guarantee(found_i != ref_i, "compare_entry_to() and find_matching_entry() do not agree");
      // Found a matching entry somewhere else in *merge_cp_p so just need a mapping entry.
      new_ref_i = found_i;
      map_index(scratch_cp, ref_i, found_i);
    } else {
      // no match found so we have to append this entry to *merge_cp_p
      append_entry(scratch_cp, ref_i, merge_cp_p, merge_cp_length_p);
      // The above call to append_entry() can only append one entry
      // so the post call query of *merge_cp_length_p is only for
      // the sake of consistency.
      new_ref_i = *merge_cp_length_p - 1;
    }
  }

  return new_ref_i;
} // end find_or_append_indirect_entry()


// Append a bootstrap specifier into the merge_cp operands that is semantically equal
// to the scratch_cp operands bootstrap specifier passed by the old_bs_i index.
// Recursively append new merge_cp entries referenced by the new bootstrap specifier.
void VM_RedefineClasses::append_operand(const constantPoolHandle& scratch_cp, int old_bs_i,
       constantPoolHandle *merge_cp_p, int *merge_cp_length_p) {

  int old_ref_i = scratch_cp->operand_bootstrap_method_ref_index_at(old_bs_i);
  int new_ref_i = find_or_append_indirect_entry(scratch_cp, old_ref_i, merge_cp_p,
                                                merge_cp_length_p);
  if (new_ref_i != old_ref_i) {
    log_trace(redefine, class, constantpool)
      ("operands entry@%d bootstrap method ref_index change: %d to %d", _operands_cur_length, old_ref_i, new_ref_i);
  }

  Array<u2>* merge_ops = (*merge_cp_p)->operands();
  int new_bs_i = _operands_cur_length;
  // We have _operands_cur_length == 0 when the merge_cp operands is empty yet.
  // However, the operand_offset_at(0) was set in the extend_operands() call.
  int new_base = (new_bs_i == 0) ? (*merge_cp_p)->operand_offset_at(0)
                                 : (*merge_cp_p)->operand_next_offset_at(new_bs_i - 1);
  int argc     = scratch_cp->operand_argument_count_at(old_bs_i);

  ConstantPool::operand_offset_at_put(merge_ops, _operands_cur_length, new_base);
  merge_ops->at_put(new_base++, new_ref_i);
  merge_ops->at_put(new_base++, argc);

  for (int i = 0; i < argc; i++) {
    int old_arg_ref_i = scratch_cp->operand_argument_index_at(old_bs_i, i);
    int new_arg_ref_i = find_or_append_indirect_entry(scratch_cp, old_arg_ref_i, merge_cp_p,
                                                      merge_cp_length_p);
    merge_ops->at_put(new_base++, new_arg_ref_i);
    if (new_arg_ref_i != old_arg_ref_i) {
      log_trace(redefine, class, constantpool)
        ("operands entry@%d bootstrap method argument ref_index change: %d to %d",
         _operands_cur_length, old_arg_ref_i, new_arg_ref_i);
    }
  }
  if (old_bs_i != _operands_cur_length) {
    // The bootstrap specifier in *merge_cp_p is at a different index than
    // that in scratch_cp so we need to map the index values.
    map_operand_index(old_bs_i, new_bs_i);
  }
  _operands_cur_length++;
} // end append_operand()


int VM_RedefineClasses::find_or_append_operand(const constantPoolHandle& scratch_cp,
      int old_bs_i, constantPoolHandle *merge_cp_p, int *merge_cp_length_p) {

  int new_bs_i = old_bs_i; // bootstrap specifier index
  bool match = (old_bs_i < _operands_cur_length) &&
               scratch_cp->compare_operand_to(old_bs_i, *merge_cp_p, old_bs_i);

  if (!match) {
    // forward reference in *merge_cp_p or not a direct match
    int found_i = scratch_cp->find_matching_operand(old_bs_i, *merge_cp_p,
                                                    _operands_cur_length);
    if (found_i != -1) {
      guarantee(found_i != old_bs_i, "compare_operand_to() and find_matching_operand() disagree");
      // found a matching operand somewhere else in *merge_cp_p so just need a mapping
      new_bs_i = found_i;
      map_operand_index(old_bs_i, found_i);
    } else {
      // no match found so we have to append this bootstrap specifier to *merge_cp_p
      append_operand(scratch_cp, old_bs_i, merge_cp_p, merge_cp_length_p);
      new_bs_i = _operands_cur_length - 1;
    }
  }
  return new_bs_i;
} // end find_or_append_operand()


void VM_RedefineClasses::finalize_operands_merge(const constantPoolHandle& merge_cp, TRAPS) {
  if (merge_cp->operands() == NULL) {
    return;
  }
  // Shrink the merge_cp operands
  merge_cp->shrink_operands(_operands_cur_length, CHECK);

  if (log_is_enabled(Trace, redefine, class, constantpool)) {
    // don't want to loop unless we are tracing
    int count = 0;
    for (int i = 1; i < _operands_index_map_p->length(); i++) {
      int value = _operands_index_map_p->at(i);
      if (value != -1) {
        log_trace(redefine, class, constantpool)("operands_index_map[%d]: old=%d new=%d", count, i, value);
        count++;
      }
    }
  }
  // Clean-up
  _operands_index_map_p = NULL;
  _operands_cur_length = 0;
  _operands_index_map_count = 0;
} // end finalize_operands_merge()

// Symbol* comparator for qsort
// The caller must have an active ResourceMark.
static int symcmp(const void* a, const void* b) {
  char* astr = (*(Symbol**)a)->as_C_string();
  char* bstr = (*(Symbol**)b)->as_C_string();
  return strcmp(astr, bstr);
}

// The caller must have an active ResourceMark.
static jvmtiError check_attribute_arrays(const char* attr_name,
           InstanceKlass* the_class, InstanceKlass* scratch_class,
           Array<u2>* the_array, Array<u2>* scr_array) {
  bool the_array_exists = the_array != Universe::the_empty_short_array();
  bool scr_array_exists = scr_array != Universe::the_empty_short_array();

  int array_len = the_array->length();
  if (the_array_exists && scr_array_exists) {
    if (array_len != scr_array->length()) {
      log_trace(redefine, class)
        ("redefined class %s attribute change error: %s len=%d changed to len=%d",
         the_class->external_name(), attr_name, array_len, scr_array->length());
      return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED;
    }

    // The order of entries in the attribute array is not specified so we
    // have to explicitly check for the same contents. We do this by copying
    // the referenced symbols into their own arrays, sorting them and then
    // comparing each element pair.

    Symbol** the_syms = NEW_RESOURCE_ARRAY_RETURN_NULL(Symbol*, array_len);
    Symbol** scr_syms = NEW_RESOURCE_ARRAY_RETURN_NULL(Symbol*, array_len);

    if (the_syms == NULL || scr_syms == NULL) {
      return JVMTI_ERROR_OUT_OF_MEMORY;
    }

    for (int i = 0; i < array_len; i++) {
      int the_cp_index = the_array->at(i);
      int scr_cp_index = scr_array->at(i);
      the_syms[i] = the_class->constants()->klass_name_at(the_cp_index);
      scr_syms[i] = scratch_class->constants()->klass_name_at(scr_cp_index);
    }

    qsort(the_syms, array_len, sizeof(Symbol*), symcmp);
    qsort(scr_syms, array_len, sizeof(Symbol*), symcmp);

    for (int i = 0; i < array_len; i++) {
      if (the_syms[i] != scr_syms[i]) {
        log_info(redefine, class)
          ("redefined class %s attribute change error: %s[%d]: %s changed to %s",
           the_class->external_name(), attr_name, i,
           the_syms[i]->as_C_string(), scr_syms[i]->as_C_string());
        return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED;
      }
    }
  } else if (the_array_exists ^ scr_array_exists) {
    const char* action_str = (the_array_exists) ? "removed" : "added";
    log_info(redefine, class)
      ("redefined class %s attribute change error: %s attribute %s",
       the_class->external_name(), attr_name, action_str);
    return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED;
  }
  return JVMTI_ERROR_NONE;
}

static jvmtiError check_nest_attributes(InstanceKlass* the_class,
                                        InstanceKlass* scratch_class) {
  // Check whether the class NestHost attribute has been changed.
  Thread* thread = Thread::current();
  ResourceMark rm(thread);
  u2 the_nest_host_idx = the_class->nest_host_index();
  u2 scr_nest_host_idx = scratch_class->nest_host_index();

  if (the_nest_host_idx != 0 && scr_nest_host_idx != 0) {
    Symbol* the_sym = the_class->constants()->klass_name_at(the_nest_host_idx);
    Symbol* scr_sym = scratch_class->constants()->klass_name_at(scr_nest_host_idx);
    if (the_sym != scr_sym) {
      log_info(redefine, class, nestmates)
        ("redefined class %s attribute change error: NestHost class: %s replaced with: %s",
         the_class->external_name(), the_sym->as_C_string(), scr_sym->as_C_string());
      return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED;
    }
  } else if ((the_nest_host_idx == 0) ^ (scr_nest_host_idx == 0)) {
    const char* action_str = (the_nest_host_idx != 0) ? "removed" : "added";
    log_info(redefine, class, nestmates)
      ("redefined class %s attribute change error: NestHost attribute %s",
       the_class->external_name(), action_str);
    return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED;
  }

  // Check whether the class NestMembers attribute has been changed.
  return check_attribute_arrays("NestMembers",
                                the_class, scratch_class,
                                the_class->nest_members(),
                                scratch_class->nest_members());
}

// Return an error status if the class Record attribute was changed.
static jvmtiError check_record_attribute(InstanceKlass* the_class, InstanceKlass* scratch_class) {
  // Get lists of record components.
  Array<RecordComponent*>* the_record = the_class->record_components();
  Array<RecordComponent*>* scr_record = scratch_class->record_components();
  bool the_record_exists = the_record != NULL;
  bool scr_record_exists = scr_record != NULL;

  if (the_record_exists && scr_record_exists) {
    int the_num_components = the_record->length();
    int scr_num_components = scr_record->length();
    if (the_num_components != scr_num_components) {
      log_info(redefine, class, record)
        ("redefined class %s attribute change error: Record num_components=%d changed to num_components=%d",
         the_class->external_name(), the_num_components, scr_num_components);
      return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED;
    }

    // Compare each field in each record component.
    ConstantPool* the_cp =  the_class->constants();
    ConstantPool* scr_cp =  scratch_class->constants();
    for (int x = 0; x < the_num_components; x++) {
      RecordComponent* the_component = the_record->at(x);
      RecordComponent* scr_component = scr_record->at(x);
      const Symbol* const the_name = the_cp->symbol_at(the_component->name_index());
      const Symbol* const scr_name = scr_cp->symbol_at(scr_component->name_index());
      const Symbol* const the_descr = the_cp->symbol_at(the_component->descriptor_index());
      const Symbol* const scr_descr = scr_cp->symbol_at(scr_component->descriptor_index());
      if (the_name != scr_name || the_descr != scr_descr) {
        log_info(redefine, class, record)
          ("redefined class %s attribute change error: Record name_index, descriptor_index, and/or attributes_count changed",
           the_class->external_name());
        return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED;
      }

      int the_gen_sig = the_component->generic_signature_index();
      int scr_gen_sig = scr_component->generic_signature_index();
      const Symbol* const the_gen_sig_sym = (the_gen_sig == 0 ? NULL :
        the_cp->symbol_at(the_component->generic_signature_index()));
      const Symbol* const scr_gen_sig_sym = (scr_gen_sig == 0 ? NULL :
        scr_cp->symbol_at(scr_component->generic_signature_index()));
      if (the_gen_sig_sym != scr_gen_sig_sym) {
        log_info(redefine, class, record)
          ("redefined class %s attribute change error: Record generic_signature attribute changed",
           the_class->external_name());
        return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED;
      }

      // It's okay if a record component's annotations were changed.
    }

  } else if (the_record_exists ^ scr_record_exists) {
    const char* action_str = (the_record_exists) ? "removed" : "added";
    log_info(redefine, class, record)
      ("redefined class %s attribute change error: Record attribute %s",
       the_class->external_name(), action_str);
    return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED;
  }

  return JVMTI_ERROR_NONE;
}


static jvmtiError check_permitted_subclasses_attribute(InstanceKlass* the_class,
                                                       InstanceKlass* scratch_class) {
  Thread* thread = Thread::current();
  ResourceMark rm(thread);

  // Check whether the class PermittedSubclasses attribute has been changed.
  return check_attribute_arrays("PermittedSubclasses",
                                the_class, scratch_class,
                                the_class->permitted_subclasses(),
                                scratch_class->permitted_subclasses());
}

static bool can_add_or_delete(Method* m) {
      // Compatibility mode
  return (AllowRedefinitionToAddDeleteMethods &&
          (m->is_private() && (m->is_static() || m->is_final())));
}

jvmtiError VM_RedefineClasses::compare_and_normalize_class_versions(
             InstanceKlass* the_class,
             InstanceKlass* scratch_class) {
  int i;

  // Check superclasses, or rather their names, since superclasses themselves can be
  // requested to replace.
  // Check for NULL superclass first since this might be java.lang.Object
  if (the_class->super() != scratch_class->super() &&
      (the_class->super() == NULL || scratch_class->super() == NULL ||
       the_class->super()->name() !=
       scratch_class->super()->name())) {
    log_info(redefine, class, normalize)
      ("redefined class %s superclass change error: superclass changed from %s to %s.",
       the_class->external_name(),
       the_class->super() == NULL ? "NULL" : the_class->super()->external_name(),
       scratch_class->super() == NULL ? "NULL" : scratch_class->super()->external_name());
    return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED;
  }

  // Check if the number, names and order of directly implemented interfaces are the same.
  // I think in principle we should just check if the sets of names of directly implemented
  // interfaces are the same, i.e. the order of declaration (which, however, if changed in the
  // .java file, also changes in .class file) should not matter. However, comparing sets is
  // technically a bit more difficult, and, more importantly, I am not sure at present that the
  // order of interfaces does not matter on the implementation level, i.e. that the VM does not
  // rely on it somewhere.
  Array<InstanceKlass*>* k_interfaces = the_class->local_interfaces();
  Array<InstanceKlass*>* k_new_interfaces = scratch_class->local_interfaces();
  int n_intfs = k_interfaces->length();
  if (n_intfs != k_new_interfaces->length()) {
    log_info(redefine, class, normalize)
      ("redefined class %s interfaces change error: number of implemented interfaces changed from %d to %d.",
       the_class->external_name(), n_intfs, k_new_interfaces->length());
    return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED;
  }
  for (i = 0; i < n_intfs; i++) {
    if (k_interfaces->at(i)->name() !=
        k_new_interfaces->at(i)->name()) {
      log_info(redefine, class, normalize)
          ("redefined class %s interfaces change error: interface changed from %s to %s.",
           the_class->external_name(),
           k_interfaces->at(i)->external_name(), k_new_interfaces->at(i)->external_name());
      return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED;
    }
  }

  // Check whether class is in the error init state.
  if (the_class->is_in_error_state()) {
    log_info(redefine, class, normalize)
      ("redefined class %s is in error init state.", the_class->external_name());
    // TBD #5057930: special error code is needed in 1.6
    return JVMTI_ERROR_INVALID_CLASS;
  }

  // Check whether the nest-related attributes have been changed.
  jvmtiError err = check_nest_attributes(the_class, scratch_class);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }

  // Check whether the Record attribute has been changed.
  err = check_record_attribute(the_class, scratch_class);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }

  // Check whether the PermittedSubclasses attribute has been changed.
  err = check_permitted_subclasses_attribute(the_class, scratch_class);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }

  // Check whether class modifiers are the same.
  jushort old_flags = (jushort) the_class->access_flags().get_flags();
  jushort new_flags = (jushort) scratch_class->access_flags().get_flags();
  if (old_flags != new_flags) {
    log_info(redefine, class, normalize)
        ("redefined class %s modifiers change error: modifiers changed from %d to %d.",
         the_class->external_name(), old_flags, new_flags);
    return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED;
  }

  // Check if the number, names, types and order of fields declared in these classes
  // are the same.
  JavaFieldStream old_fs(the_class);
  JavaFieldStream new_fs(scratch_class);
  for (; !old_fs.done() && !new_fs.done(); old_fs.next(), new_fs.next()) {
    // name and signature
    Symbol* name_sym1 = the_class->constants()->symbol_at(old_fs.name_index());
    Symbol* sig_sym1 = the_class->constants()->symbol_at(old_fs.signature_index());
    Symbol* name_sym2 = scratch_class->constants()->symbol_at(new_fs.name_index());
    Symbol* sig_sym2 = scratch_class->constants()->symbol_at(new_fs.signature_index());
    if (name_sym1 != name_sym2 || sig_sym1 != sig_sym2) {
      log_info(redefine, class, normalize)
          ("redefined class %s fields change error: field %s %s changed to %s %s.",
           the_class->external_name(),
           sig_sym1->as_C_string(), name_sym1->as_C_string(),
           sig_sym2->as_C_string(), name_sym2->as_C_string());
      return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED;
    }
    // offset
    if (old_fs.offset() != new_fs.offset()) {
      log_info(redefine, class, normalize)
          ("redefined class %s field %s change error: offset changed from %d to %d.",
           the_class->external_name(), name_sym2->as_C_string(), old_fs.offset(), new_fs.offset());
      return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED;
    }
    // access
    old_flags = old_fs.access_flags().as_short();
    new_flags = new_fs.access_flags().as_short();
    if ((old_flags ^ new_flags) & JVM_RECOGNIZED_FIELD_MODIFIERS) {
      log_info(redefine, class, normalize)
          ("redefined class %s field %s change error: modifiers changed from %d to %d.",
           the_class->external_name(), name_sym2->as_C_string(), old_flags, new_flags);
      return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED;
    }
  }

  // If both streams aren't done then we have a differing number of
  // fields.
  if (!old_fs.done() || !new_fs.done()) {
    const char* action = old_fs.done() ? "added" : "deleted";
    log_info(redefine, class, normalize)
        ("redefined class %s fields change error: some fields were %s.",
         the_class->external_name(), action);
    return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED;
  }

  // Do a parallel walk through the old and new methods. Detect
  // cases where they match (exist in both), have been added in
  // the new methods, or have been deleted (exist only in the
  // old methods).  The class file parser places methods in order
  // by method name, but does not order overloaded methods by
  // signature.  In order to determine what fate befell the methods,
  // this code places the overloaded new methods that have matching
  // old methods in the same order as the old methods and places
  // new overloaded methods at the end of overloaded methods of
  // that name. The code for this order normalization is adapted
  // from the algorithm used in InstanceKlass::find_method().
  // Since we are swapping out of order entries as we find them,
  // we only have to search forward through the overloaded methods.
  // Methods which are added and have the same name as an existing
  // method (but different signature) will be put at the end of
  // the methods with that name, and the name mismatch code will
  // handle them.
  Array<Method*>* k_old_methods(the_class->methods());
  Array<Method*>* k_new_methods(scratch_class->methods());
  int n_old_methods = k_old_methods->length();
  int n_new_methods = k_new_methods->length();
  Thread* thread = Thread::current();

  int ni = 0;
  int oi = 0;
  while (true) {
    Method* k_old_method;
    Method* k_new_method;
    enum { matched, added, deleted, undetermined } method_was = undetermined;

    if (oi >= n_old_methods) {
      if (ni >= n_new_methods) {
        break; // we've looked at everything, done
      }
      // New method at the end
      k_new_method = k_new_methods->at(ni);
      method_was = added;
    } else if (ni >= n_new_methods) {
      // Old method, at the end, is deleted
      k_old_method = k_old_methods->at(oi);
      method_was = deleted;
    } else {
      // There are more methods in both the old and new lists
      k_old_method = k_old_methods->at(oi);
      k_new_method = k_new_methods->at(ni);
      if (k_old_method->name() != k_new_method->name()) {
        // Methods are sorted by method name, so a mismatch means added
        // or deleted
        if (k_old_method->name()->fast_compare(k_new_method->name()) > 0) {
          method_was = added;
        } else {
          method_was = deleted;
        }
      } else if (k_old_method->signature() == k_new_method->signature()) {
        // Both the name and signature match
        method_was = matched;
      } else {
        // The name matches, but the signature doesn't, which means we have to
        // search forward through the new overloaded methods.
        int nj;  // outside the loop for post-loop check
        for (nj = ni + 1; nj < n_new_methods; nj++) {
          Method* m = k_new_methods->at(nj);
          if (k_old_method->name() != m->name()) {
            // reached another method name so no more overloaded methods
            method_was = deleted;
            break;
          }
          if (k_old_method->signature() == m->signature()) {
            // found a match so swap the methods
            k_new_methods->at_put(ni, m);
            k_new_methods->at_put(nj, k_new_method);
            k_new_method = m;
            method_was = matched;
            break;
          }
        }

        if (nj >= n_new_methods) {
          // reached the end without a match; so method was deleted
          method_was = deleted;
        }
      }
    }

    switch (method_was) {
    case matched:
      // methods match, be sure modifiers do too
      old_flags = (jushort) k_old_method->access_flags().get_flags();
      new_flags = (jushort) k_new_method->access_flags().get_flags();
      if ((old_flags ^ new_flags) & ~(JVM_ACC_NATIVE)) {
        log_info(redefine, class, normalize)
          ("redefined class %s  method %s modifiers error: modifiers changed from %d to %d",
           the_class->external_name(), k_old_method->name_and_sig_as_C_string(), old_flags, new_flags);
        return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED;
      }
      {
        u2 new_num = k_new_method->method_idnum();
        u2 old_num = k_old_method->method_idnum();
        if (new_num != old_num) {
          Method* idnum_owner = scratch_class->method_with_idnum(old_num);
          if (idnum_owner != NULL) {
            // There is already a method assigned this idnum -- switch them
            // Take current and original idnum from the new_method
            idnum_owner->set_method_idnum(new_num);
            idnum_owner->set_orig_method_idnum(k_new_method->orig_method_idnum());
          }
          // Take current and original idnum from the old_method
          k_new_method->set_method_idnum(old_num);
          k_new_method->set_orig_method_idnum(k_old_method->orig_method_idnum());
          if (thread->has_pending_exception()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
          }
        }
      }
      log_trace(redefine, class, normalize)
        ("Method matched: new: %s [%d] == old: %s [%d]",
         k_new_method->name_and_sig_as_C_string(), ni, k_old_method->name_and_sig_as_C_string(), oi);
      // advance to next pair of methods
      ++oi;
      ++ni;
      break;
    case added:
      // method added, see if it is OK
      if (!can_add_or_delete(k_new_method)) {
        log_info(redefine, class, normalize)
          ("redefined class %s methods error: added method: %s [%d]",
           the_class->external_name(), k_new_method->name_and_sig_as_C_string(), ni);
        return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED;
      }
      {
        u2 num = the_class->next_method_idnum();
        if (num == ConstMethod::UNSET_IDNUM) {
          // cannot add any more methods
          log_info(redefine, class, normalize)
            ("redefined class %s methods error: can't create ID for new method %s [%d]",
             the_class->external_name(), k_new_method->name_and_sig_as_C_string(), ni);
          return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED;
        }
        u2 new_num = k_new_method->method_idnum();
        Method* idnum_owner = scratch_class->method_with_idnum(num);
        if (idnum_owner != NULL) {
          // There is already a method assigned this idnum -- switch them
          // Take current and original idnum from the new_method
          idnum_owner->set_method_idnum(new_num);
          idnum_owner->set_orig_method_idnum(k_new_method->orig_method_idnum());
        }
        k_new_method->set_method_idnum(num);
        k_new_method->set_orig_method_idnum(num);
        if (thread->has_pending_exception()) {
          return JVMTI_ERROR_OUT_OF_MEMORY;
        }
      }
      log_trace(redefine, class, normalize)
        ("Method added: new: %s [%d]", k_new_method->name_and_sig_as_C_string(), ni);
      ++ni; // advance to next new method
      break;
    case deleted:
      // method deleted, see if it is OK
      if (!can_add_or_delete(k_old_method)) {
        log_info(redefine, class, normalize)
          ("redefined class %s methods error: deleted method %s [%d]",
           the_class->external_name(), k_old_method->name_and_sig_as_C_string(), oi);
        return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED;
      }
      log_trace(redefine, class, normalize)
        ("Method deleted: old: %s [%d]", k_old_method->name_and_sig_as_C_string(), oi);
      ++oi; // advance to next old method
      break;
    default:
      ShouldNotReachHere();
    }
  }

  return JVMTI_ERROR_NONE;
}


// Find new constant pool index value for old constant pool index value
// by seaching the index map. Returns zero (0) if there is no mapped
// value for the old constant pool index.
int VM_RedefineClasses::find_new_index(int old_index) {
  if (_index_map_count == 0) {
    // map is empty so nothing can be found
    return 0;
  }

  if (old_index < 1 || old_index >= _index_map_p->length()) {
    // The old_index is out of range so it is not mapped. This should
    // not happen in regular constant pool merging use, but it can
    // happen if a corrupt annotation is processed.
    return 0;
  }

  int value = _index_map_p->at(old_index);
  if (value == -1) {
    // the old_index is not mapped
    return 0;
  }

  return value;
} // end find_new_index()


// Find new bootstrap specifier index value for old bootstrap specifier index
// value by seaching the index map. Returns unused index (-1) if there is
// no mapped value for the old bootstrap specifier index.
int VM_RedefineClasses::find_new_operand_index(int old_index) {
  if (_operands_index_map_count == 0) {
    // map is empty so nothing can be found
    return -1;
  }

  if (old_index == -1 || old_index >= _operands_index_map_p->length()) {
    // The old_index is out of range so it is not mapped.
    // This should not happen in regular constant pool merging use.
    return -1;
  }

  int value = _operands_index_map_p->at(old_index);
  if (value == -1) {
    // the old_index is not mapped
    return -1;
  }

  return value;
} // end find_new_operand_index()


// Returns true if the current mismatch is due to a resolved/unresolved
// class pair. Otherwise, returns false.
bool VM_RedefineClasses::is_unresolved_class_mismatch(const constantPoolHandle& cp1,
       int index1, const constantPoolHandle& cp2, int index2) {

  jbyte t1 = cp1->tag_at(index1).value();
  if (t1 != JVM_CONSTANT_Class && t1 != JVM_CONSTANT_UnresolvedClass) {
    return false;  // wrong entry type; not our special case
  }

  jbyte t2 = cp2->tag_at(index2).value();
  if (t2 != JVM_CONSTANT_Class && t2 != JVM_CONSTANT_UnresolvedClass) {
    return false;  // wrong entry type; not our special case
  }

  if (t1 == t2) {
    return false;  // not a mismatch; not our special case
  }

  char *s1 = cp1->klass_name_at(index1)->as_C_string();
  char *s2 = cp2->klass_name_at(index2)->as_C_string();
  if (strcmp(s1, s2) != 0) {
    return false;  // strings don't match; not our special case
  }

  return true;  // made it through the gauntlet; this is our special case
} // end is_unresolved_class_mismatch()


// The bug 6214132 caused the verification to fail.
// 1. What's done in RedefineClasses() before verification:
//  a) A reference to the class being redefined (_the_class) and a
//     reference to new version of the class (_scratch_class) are
//     saved here for use during the bytecode verification phase of
//     RedefineClasses.
//  b) The _java_mirror field from _the_class is copied to the
//     _java_mirror field in _scratch_class. This means that a jclass
//     returned for _the_class or _scratch_class will refer to the
//     same Java mirror. The verifier will see the "one true mirror"
//     for the class being verified.
// 2. See comments in JvmtiThreadState for what is done during verification.

class RedefineVerifyMark : public StackObj {
 private:
  JvmtiThreadState* _state;
  Klass*            _scratch_class;
  Handle            _scratch_mirror;

 public:

  RedefineVerifyMark(Klass* the_class, Klass* scratch_class,
                     JvmtiThreadState* state) : _state(state), _scratch_class(scratch_class)
  {
    _state->set_class_versions_map(the_class, scratch_class);
    _scratch_mirror = Handle(_state->get_thread(), _scratch_class->java_mirror());
    _scratch_class->replace_java_mirror(the_class->java_mirror());
  }

  ~RedefineVerifyMark() {
    // Restore the scratch class's mirror, so when scratch_class is removed
    // the correct mirror pointing to it can be cleared.
    _scratch_class->replace_java_mirror(_scratch_mirror());
    _state->clear_class_versions_map();
  }
};


jvmtiError VM_RedefineClasses::load_new_class_versions() {

  // For consistency allocate memory using os::malloc wrapper.
  _scratch_classes = (InstanceKlass**)
    os::malloc(sizeof(InstanceKlass*) * _class_count, mtClass);
  if (_scratch_classes == NULL) {
    return JVMTI_ERROR_OUT_OF_MEMORY;
  }
  // Zero initialize the _scratch_classes array.
  for (int i = 0; i < _class_count; i++) {
    _scratch_classes[i] = NULL;
  }

  JavaThread* current = JavaThread::current();
  ResourceMark rm(current);

  JvmtiThreadState *state = JvmtiThreadState::state_for(current);
  // state can only be NULL if the current thread is exiting which
  // should not happen since we're trying to do a RedefineClasses
  guarantee(state != NULL, "exiting thread calling load_new_class_versions");
  for (int i = 0; i < _class_count; i++) {
    // Create HandleMark so that any handles created while loading new class
    // versions are deleted. Constant pools are deallocated while merging
    // constant pools
    HandleMark hm(current);
    InstanceKlass* the_class = get_ik(_class_defs[i].klass);

    log_debug(redefine, class, load)
      ("loading name=%s kind=%d (avail_mem=" UINT64_FORMAT "K)",
       the_class->external_name(), _class_load_kind, os::available_memory() >> 10);

    ClassFileStream st((u1*)_class_defs[i].class_bytes,
                       _class_defs[i].class_byte_count,
                       "__VM_RedefineClasses__",
                       ClassFileStream::verify);

    // Set redefined class handle in JvmtiThreadState class.
    // This redefined class is sent to agent event handler for class file
    // load hook event.
    state->set_class_being_redefined(the_class, _class_load_kind);

    JavaThread* THREAD = current; // For exception macros.
    ExceptionMark em(THREAD);
    Handle protection_domain(THREAD, the_class->protection_domain());
    ClassLoadInfo cl_info(protection_domain);
    // Parse and create a class from the bytes, but this class isn't added
    // to the dictionary, so do not call resolve_from_stream.
    InstanceKlass* scratch_class = KlassFactory::create_from_stream(&st,
                                                      the_class->name(),
                                                      the_class->class_loader_data(),
                                                      cl_info,
                                                      THREAD);

    // Clear class_being_redefined just to be sure.
    state->clear_class_being_redefined();

    // TODO: if this is retransform, and nothing changed we can skip it

    // Need to clean up allocated InstanceKlass if there's an error so assign
    // the result here. Caller deallocates all the scratch classes in case of
    // an error.
    _scratch_classes[i] = scratch_class;

    if (HAS_PENDING_EXCEPTION) {
      Symbol* ex_name = PENDING_EXCEPTION->klass()->name();
      log_info(redefine, class, load, exceptions)("create_from_stream exception: '%s'", ex_name->as_C_string());
      CLEAR_PENDING_EXCEPTION;

      if (ex_name == vmSymbols::java_lang_UnsupportedClassVersionError()) {
        return JVMTI_ERROR_UNSUPPORTED_VERSION;
      } else if (ex_name == vmSymbols::java_lang_ClassFormatError()) {
        return JVMTI_ERROR_INVALID_CLASS_FORMAT;
      } else if (ex_name == vmSymbols::java_lang_ClassCircularityError()) {
        return JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION;
      } else if (ex_name == vmSymbols::java_lang_NoClassDefFoundError()) {
        // The message will be "XXX (wrong name: YYY)"
        return JVMTI_ERROR_NAMES_DONT_MATCH;
      } else if (ex_name == vmSymbols::java_lang_OutOfMemoryError()) {
        return JVMTI_ERROR_OUT_OF_MEMORY;
      } else {  // Just in case more exceptions can be thrown..
        return JVMTI_ERROR_FAILS_VERIFICATION;
      }
    }

    // Ensure class is linked before redefine
    if (!the_class->is_linked()) {
      the_class->link_class(THREAD);
      if (HAS_PENDING_EXCEPTION) {
        Symbol* ex_name = PENDING_EXCEPTION->klass()->name();
        oop message = java_lang_Throwable::message(PENDING_EXCEPTION);
        if (message != NULL) {
          char* ex_msg = java_lang_String::as_utf8_string(message);
          log_info(redefine, class, load, exceptions)("link_class exception: '%s %s'",
                   ex_name->as_C_string(), ex_msg);
        } else {
          log_info(redefine, class, load, exceptions)("link_class exception: '%s'",
                   ex_name->as_C_string());
        }
        CLEAR_PENDING_EXCEPTION;
        if (ex_name == vmSymbols::java_lang_OutOfMemoryError()) {
          return JVMTI_ERROR_OUT_OF_MEMORY;
        } else if (ex_name == vmSymbols::java_lang_NoClassDefFoundError()) {
          return JVMTI_ERROR_INVALID_CLASS;
        } else {
          return JVMTI_ERROR_INTERNAL;
        }
      }
    }

    // Do the validity checks in compare_and_normalize_class_versions()
    // before verifying the byte codes. By doing these checks first, we
    // limit the number of functions that require redirection from
    // the_class to scratch_class. In particular, we don't have to
    // modify JNI GetSuperclass() and thus won't change its performance.
    jvmtiError res = compare_and_normalize_class_versions(the_class,
                       scratch_class);
    if (res != JVMTI_ERROR_NONE) {
      return res;
    }

    // verify what the caller passed us
    {
      // The bug 6214132 caused the verification to fail.
      // Information about the_class and scratch_class is temporarily
      // recorded into jvmtiThreadState. This data is used to redirect
      // the_class to scratch_class in the JVM_* functions called by the
      // verifier. Please, refer to jvmtiThreadState.hpp for the detailed
      // description.
      RedefineVerifyMark rvm(the_class, scratch_class, state);
      Verifier::verify(scratch_class, true, THREAD);
    }

    if (HAS_PENDING_EXCEPTION) {
      Symbol* ex_name = PENDING_EXCEPTION->klass()->name();
      log_info(redefine, class, load, exceptions)("verify_byte_codes exception: '%s'", ex_name->as_C_string());
      CLEAR_PENDING_EXCEPTION;
      if (ex_name == vmSymbols::java_lang_OutOfMemoryError()) {
        return JVMTI_ERROR_OUT_OF_MEMORY;
      } else {
        // tell the caller the bytecodes are bad
        return JVMTI_ERROR_FAILS_VERIFICATION;
      }
    }

    res = merge_cp_and_rewrite(the_class, scratch_class, THREAD);
    if (HAS_PENDING_EXCEPTION) {
      Symbol* ex_name = PENDING_EXCEPTION->klass()->name();
      log_info(redefine, class, load, exceptions)("merge_cp_and_rewrite exception: '%s'", ex_name->as_C_string());
      CLEAR_PENDING_EXCEPTION;
      if (ex_name == vmSymbols::java_lang_OutOfMemoryError()) {
        return JVMTI_ERROR_OUT_OF_MEMORY;
      } else {
        return JVMTI_ERROR_INTERNAL;
      }
    }

#ifdef ASSERT
    {
      // verify what we have done during constant pool merging
      {
        RedefineVerifyMark rvm(the_class, scratch_class, state);
        Verifier::verify(scratch_class, true, THREAD);
      }

      if (HAS_PENDING_EXCEPTION) {
        Symbol* ex_name = PENDING_EXCEPTION->klass()->name();
        log_info(redefine, class, load, exceptions)
          ("verify_byte_codes post merge-CP exception: '%s'", ex_name->as_C_string());
        CLEAR_PENDING_EXCEPTION;
        if (ex_name == vmSymbols::java_lang_OutOfMemoryError()) {
          return JVMTI_ERROR_OUT_OF_MEMORY;
        } else {
          // tell the caller that constant pool merging screwed up
          return JVMTI_ERROR_INTERNAL;
        }
      }
    }
#endif // ASSERT

    Rewriter::rewrite(scratch_class, THREAD);
    if (!HAS_PENDING_EXCEPTION) {
      scratch_class->link_methods(THREAD);
    }
    if (HAS_PENDING_EXCEPTION) {
      Symbol* ex_name = PENDING_EXCEPTION->klass()->name();
      log_info(redefine, class, load, exceptions)
        ("Rewriter::rewrite or link_methods exception: '%s'", ex_name->as_C_string());
      CLEAR_PENDING_EXCEPTION;
      if (ex_name == vmSymbols::java_lang_OutOfMemoryError()) {
        return JVMTI_ERROR_OUT_OF_MEMORY;
      } else {
        return JVMTI_ERROR_INTERNAL;
      }
    }

    log_debug(redefine, class, load)
      ("loaded name=%s (avail_mem=" UINT64_FORMAT "K)", the_class->external_name(), os::available_memory() >> 10);
  }

  return JVMTI_ERROR_NONE;
}


// Map old_index to new_index as needed. scratch_cp is only needed
// for log calls.
void VM_RedefineClasses::map_index(const constantPoolHandle& scratch_cp,
       int old_index, int new_index) {
  if (find_new_index(old_index) != 0) {
    // old_index is already mapped
    return;
  }

  if (old_index == new_index) {
    // no mapping is needed
    return;
  }

  _index_map_p->at_put(old_index, new_index);
  _index_map_count++;

  log_trace(redefine, class, constantpool)
    ("mapped tag %d at index %d to %d", scratch_cp->tag_at(old_index).value(), old_index, new_index);
} // end map_index()


// Map old_index to new_index as needed.
void VM_RedefineClasses::map_operand_index(int old_index, int new_index) {
  if (find_new_operand_index(old_index) != -1) {
    // old_index is already mapped
    return;
  }

  if (old_index == new_index) {
    // no mapping is needed
    return;
  }

  _operands_index_map_p->at_put(old_index, new_index);
  _operands_index_map_count++;

  log_trace(redefine, class, constantpool)("mapped bootstrap specifier at index %d to %d", old_index, new_index);
} // end map_index()


// Merge old_cp and scratch_cp and return the results of the merge via
// merge_cp_p. The number of entries in *merge_cp_p is returned via
// merge_cp_length_p. The entries in old_cp occupy the same locations
// in *merge_cp_p. Also creates a map of indices from entries in
// scratch_cp to the corresponding entry in *merge_cp_p. Index map
// entries are only created for entries in scratch_cp that occupy a
// different location in *merged_cp_p.
bool VM_RedefineClasses::merge_constant_pools(const constantPoolHandle& old_cp,
       const constantPoolHandle& scratch_cp, constantPoolHandle *merge_cp_p,
       int *merge_cp_length_p, TRAPS) {

  if (merge_cp_p == NULL) {
    assert(false, "caller must provide scratch constantPool");
    return false; // robustness
  }
  if (merge_cp_length_p == NULL) {
    assert(false, "caller must provide scratch CP length");
    return false; // robustness
  }
  // Worst case we need old_cp->length() + scratch_cp()->length(),
  // but the caller might be smart so make sure we have at least
  // the minimum.
  if ((*merge_cp_p)->length() < old_cp->length()) {
    assert(false, "merge area too small");
    return false; // robustness
  }

  log_info(redefine, class, constantpool)("old_cp_len=%d, scratch_cp_len=%d", old_cp->length(), scratch_cp->length());

  {
    // Pass 0:
    // The old_cp is copied to *merge_cp_p; this means that any code
    // using old_cp does not have to change. This work looks like a
    // perfect fit for ConstantPool*::copy_cp_to(), but we need to
    // handle one special case:
    // - revert JVM_CONSTANT_Class to JVM_CONSTANT_UnresolvedClass
    // This will make verification happy.

    int old_i;  // index into old_cp

    // index zero (0) is not used in constantPools
    for (old_i = 1; old_i < old_cp->length(); old_i++) {
      // leave debugging crumb
      jbyte old_tag = old_cp->tag_at(old_i).value();
      switch (old_tag) {
      case JVM_CONSTANT_Class:
      case JVM_CONSTANT_UnresolvedClass:
        // revert the copy to JVM_CONSTANT_UnresolvedClass
        // May be resolving while calling this so do the same for
        // JVM_CONSTANT_UnresolvedClass (klass_name_at() deals with transition)
        (*merge_cp_p)->temp_unresolved_klass_at_put(old_i,
          old_cp->klass_name_index_at(old_i));
        break;

      case JVM_CONSTANT_Double:
      case JVM_CONSTANT_Long:
        // just copy the entry to *merge_cp_p, but double and long take
        // two constant pool entries
        ConstantPool::copy_entry_to(old_cp, old_i, *merge_cp_p, old_i);
        old_i++;
        break;

      default:
        // just copy the entry to *merge_cp_p
        ConstantPool::copy_entry_to(old_cp, old_i, *merge_cp_p, old_i);
        break;
      }
    } // end for each old_cp entry

    ConstantPool::copy_operands(old_cp, *merge_cp_p, CHECK_false);
    (*merge_cp_p)->extend_operands(scratch_cp, CHECK_false);

    // We don't need to sanity check that *merge_cp_length_p is within
    // *merge_cp_p bounds since we have the minimum on-entry check above.
    (*merge_cp_length_p) = old_i;
  }

  // merge_cp_len should be the same as old_cp->length() at this point
  // so this trace message is really a "warm-and-breathing" message.
  log_debug(redefine, class, constantpool)("after pass 0: merge_cp_len=%d", *merge_cp_length_p);

  int scratch_i;  // index into scratch_cp
  {
    // Pass 1a:
    // Compare scratch_cp entries to the old_cp entries that we have
    // already copied to *merge_cp_p. In this pass, we are eliminating
    // exact duplicates (matching entry at same index) so we only
    // compare entries in the common indice range.
    int increment = 1;
    int pass1a_length = MIN2(old_cp->length(), scratch_cp->length());
    for (scratch_i = 1; scratch_i < pass1a_length; scratch_i += increment) {
      switch (scratch_cp->tag_at(scratch_i).value()) {
      case JVM_CONSTANT_Double:
      case JVM_CONSTANT_Long:
        // double and long take two constant pool entries
        increment = 2;
        break;

      default:
        increment = 1;
        break;
      }

      bool match = scratch_cp->compare_entry_to(scratch_i, *merge_cp_p, scratch_i);
      if (match) {
        // found a match at the same index so nothing more to do
        continue;
      } else if (is_unresolved_class_mismatch(scratch_cp, scratch_i,
                                              *merge_cp_p, scratch_i)) {
        // The mismatch in compare_entry_to() above is because of a
        // resolved versus unresolved class entry at the same index
        // with the same string value. Since Pass 0 reverted any
        // class entries to unresolved class entries in *merge_cp_p,
        // we go with the unresolved class entry.
        continue;
      }

      int found_i = scratch_cp->find_matching_entry(scratch_i, *merge_cp_p);
      if (found_i != 0) {
        guarantee(found_i != scratch_i,
          "compare_entry_to() and find_matching_entry() do not agree");

        // Found a matching entry somewhere else in *merge_cp_p so
        // just need a mapping entry.
        map_index(scratch_cp, scratch_i, found_i);
        continue;
      }

      // The find_matching_entry() call above could fail to find a match
      // due to a resolved versus unresolved class or string entry situation
      // like we solved above with the is_unresolved_*_mismatch() calls.
      // However, we would have to call is_unresolved_*_mismatch() over
      // all of *merge_cp_p (potentially) and that doesn't seem to be
      // worth the time.

      // No match found so we have to append this entry and any unique
      // referenced entries to *merge_cp_p.
      append_entry(scratch_cp, scratch_i, merge_cp_p, merge_cp_length_p);
    }
  }

  log_debug(redefine, class, constantpool)
    ("after pass 1a: merge_cp_len=%d, scratch_i=%d, index_map_len=%d",
     *merge_cp_length_p, scratch_i, _index_map_count);

  if (scratch_i < scratch_cp->length()) {
    // Pass 1b:
    // old_cp is smaller than scratch_cp so there are entries in
    // scratch_cp that we have not yet processed. We take care of
    // those now.
    int increment = 1;
    for (; scratch_i < scratch_cp->length(); scratch_i += increment) {
      switch (scratch_cp->tag_at(scratch_i).value()) {
      case JVM_CONSTANT_Double:
      case JVM_CONSTANT_Long:
        // double and long take two constant pool entries
        increment = 2;
        break;

      default:
        increment = 1;
        break;
      }

      int found_i =
        scratch_cp->find_matching_entry(scratch_i, *merge_cp_p);
      if (found_i != 0) {
        // Found a matching entry somewhere else in *merge_cp_p so
        // just need a mapping entry.
        map_index(scratch_cp, scratch_i, found_i);
        continue;
      }

      // No match found so we have to append this entry and any unique
      // referenced entries to *merge_cp_p.
      append_entry(scratch_cp, scratch_i, merge_cp_p, merge_cp_length_p);
    }

    log_debug(redefine, class, constantpool)
      ("after pass 1b: merge_cp_len=%d, scratch_i=%d, index_map_len=%d",
       *merge_cp_length_p, scratch_i, _index_map_count);
  }
  finalize_operands_merge(*merge_cp_p, CHECK_false);

  return true;
} // end merge_constant_pools()


// Scoped object to clean up the constant pool(s) created for merging
class MergeCPCleaner {
  ClassLoaderData*   _loader_data;
  ConstantPool*      _cp;
  ConstantPool*      _scratch_cp;
 public:
  MergeCPCleaner(ClassLoaderData* loader_data, ConstantPool* merge_cp) :
                 _loader_data(loader_data), _cp(merge_cp), _scratch_cp(NULL) {}
  ~MergeCPCleaner() {
    _loader_data->add_to_deallocate_list(_cp);
    if (_scratch_cp != NULL) {
      _loader_data->add_to_deallocate_list(_scratch_cp);
    }
  }
  void add_scratch_cp(ConstantPool* scratch_cp) { _scratch_cp = scratch_cp; }
};

// Merge constant pools between the_class and scratch_class and
// potentially rewrite bytecodes in scratch_class to use the merged
// constant pool.
jvmtiError VM_RedefineClasses::merge_cp_and_rewrite(
             InstanceKlass* the_class, InstanceKlass* scratch_class,
             TRAPS) {
  // worst case merged constant pool length is old and new combined
  int merge_cp_length = the_class->constants()->length()
        + scratch_class->constants()->length();

  // Constant pools are not easily reused so we allocate a new one
  // each time.
  // merge_cp is created unsafe for concurrent GC processing.  It
  // should be marked safe before discarding it. Even though
  // garbage,  if it crosses a card boundary, it may be scanned
  // in order to find the start of the first complete object on the card.
  ClassLoaderData* loader_data = the_class->class_loader_data();
  ConstantPool* merge_cp_oop =
    ConstantPool::allocate(loader_data,
                           merge_cp_length,
                           CHECK_(JVMTI_ERROR_OUT_OF_MEMORY));
  MergeCPCleaner cp_cleaner(loader_data, merge_cp_oop);

  HandleMark hm(THREAD);  // make sure handles are cleared before
                          // MergeCPCleaner clears out merge_cp_oop
  constantPoolHandle merge_cp(THREAD, merge_cp_oop);

  // Get constants() from the old class because it could have been rewritten
  // while we were at a safepoint allocating a new constant pool.
  constantPoolHandle old_cp(THREAD, the_class->constants());
  constantPoolHandle scratch_cp(THREAD, scratch_class->constants());

  // If the length changed, the class was redefined out from under us. Return
  // an error.
  if (merge_cp_length != the_class->constants()->length()
         + scratch_class->constants()->length()) {
    return JVMTI_ERROR_INTERNAL;
  }

  // Update the version number of the constant pools (may keep scratch_cp)
  merge_cp->increment_and_save_version(old_cp->version());
  scratch_cp->increment_and_save_version(old_cp->version());

  ResourceMark rm(THREAD);
  _index_map_count = 0;
  _index_map_p = new intArray(scratch_cp->length(), scratch_cp->length(), -1);

  _operands_cur_length = ConstantPool::operand_array_length(old_cp->operands());
  _operands_index_map_count = 0;
  int operands_index_map_len = ConstantPool::operand_array_length(scratch_cp->operands());
  _operands_index_map_p = new intArray(operands_index_map_len, operands_index_map_len, -1);

  // reference to the cp holder is needed for copy_operands()
  merge_cp->set_pool_holder(scratch_class);
  bool result = merge_constant_pools(old_cp, scratch_cp, &merge_cp,
                  &merge_cp_length, THREAD);
  merge_cp->set_pool_holder(NULL);

  if (!result) {
    // The merge can fail due to memory allocation failure or due
    // to robustness checks.
    return JVMTI_ERROR_INTERNAL;
  }

  // Set dynamic constants attribute from the original CP.
  if (old_cp->has_dynamic_constant()) {
    scratch_cp->set_has_dynamic_constant();
  }
  // Copy attributes from scratch_cp to merge_cp
  merge_cp->copy_fields(scratch_cp());

  log_info(redefine, class, constantpool)("merge_cp_len=%d, index_map_len=%d", merge_cp_length, _index_map_count);

  if (_index_map_count == 0) {
    // there is nothing to map between the new and merged constant pools

    if (old_cp->length() == scratch_cp->length()) {
      // The old and new constant pools are the same length and the
      // index map is empty. This means that the three constant pools
      // are equivalent (but not the same). Unfortunately, the new
      // constant pool has not gone through link resolution nor have
      // the new class bytecodes gone through constant pool cache
      // rewriting so we can't use the old constant pool with the new
      // class.

      // toss the merged constant pool at return
    } else if (old_cp->length() < scratch_cp->length()) {
      // The old constant pool has fewer entries than the new constant
      // pool and the index map is empty. This means the new constant
      // pool is a superset of the old constant pool. However, the old
      // class bytecodes have already gone through constant pool cache
      // rewriting so we can't use the new constant pool with the old
      // class.

      // toss the merged constant pool at return
    } else {
      // The old constant pool has more entries than the new constant
      // pool and the index map is empty. This means that both the old
      // and merged constant pools are supersets of the new constant
      // pool.

      // Replace the new constant pool with a shrunken copy of the
      // merged constant pool
      set_new_constant_pool(loader_data, scratch_class, merge_cp, merge_cp_length,
                            CHECK_(JVMTI_ERROR_OUT_OF_MEMORY));
      // The new constant pool replaces scratch_cp so have cleaner clean it up.
      // It can't be cleaned up while there are handles to it.
      cp_cleaner.add_scratch_cp(scratch_cp());
    }
  } else {
    if (log_is_enabled(Trace, redefine, class, constantpool)) {
      // don't want to loop unless we are tracing
      int count = 0;
      for (int i = 1; i < _index_map_p->length(); i++) {
        int value = _index_map_p->at(i);

        if (value != -1) {
          log_trace(redefine, class, constantpool)("index_map[%d]: old=%d new=%d", count, i, value);
          count++;
        }
      }
    }

    // We have entries mapped between the new and merged constant pools
    // so we have to rewrite some constant pool references.
    if (!rewrite_cp_refs(scratch_class)) {
      return JVMTI_ERROR_INTERNAL;
    }

    // Replace the new constant pool with a shrunken copy of the
    // merged constant pool so now the rewritten bytecodes have
    // valid references; the previous new constant pool will get
    // GCed.
    set_new_constant_pool(loader_data, scratch_class, merge_cp, merge_cp_length,
                          CHECK_(JVMTI_ERROR_OUT_OF_MEMORY));
    // The new constant pool replaces scratch_cp so have cleaner clean it up.
    // It can't be cleaned up while there are handles to it.
    cp_cleaner.add_scratch_cp(scratch_cp());
  }

  return JVMTI_ERROR_NONE;
} // end merge_cp_and_rewrite()


// Rewrite constant pool references in klass scratch_class.
bool VM_RedefineClasses::rewrite_cp_refs(InstanceKlass* scratch_class) {

  // rewrite constant pool references in the nest attributes:
  if (!rewrite_cp_refs_in_nest_attributes(scratch_class)) {
    // propagate failure back to caller
    return false;
  }

  // rewrite constant pool references in the Record attribute:
  if (!rewrite_cp_refs_in_record_attribute(scratch_class)) {
    // propagate failure back to caller
    return false;
  }

  // rewrite constant pool references in the PermittedSubclasses attribute:
  if (!rewrite_cp_refs_in_permitted_subclasses_attribute(scratch_class)) {
    // propagate failure back to caller
    return false;
  }

  // rewrite constant pool references in the methods:
  if (!rewrite_cp_refs_in_methods(scratch_class)) {
    // propagate failure back to caller
    return false;
  }

  // rewrite constant pool references in the class_annotations:
  if (!rewrite_cp_refs_in_class_annotations(scratch_class)) {
    // propagate failure back to caller
    return false;
  }

  // rewrite constant pool references in the fields_annotations:
  if (!rewrite_cp_refs_in_fields_annotations(scratch_class)) {
    // propagate failure back to caller
    return false;
  }

  // rewrite constant pool references in the methods_annotations:
  if (!rewrite_cp_refs_in_methods_annotations(scratch_class)) {
    // propagate failure back to caller
    return false;
  }

  // rewrite constant pool references in the methods_parameter_annotations:
  if (!rewrite_cp_refs_in_methods_parameter_annotations(scratch_class)) {
    // propagate failure back to caller
    return false;
  }

  // rewrite constant pool references in the methods_default_annotations:
  if (!rewrite_cp_refs_in_methods_default_annotations(scratch_class)) {
    // propagate failure back to caller
    return false;
  }

  // rewrite constant pool references in the class_type_annotations:
  if (!rewrite_cp_refs_in_class_type_annotations(scratch_class)) {
    // propagate failure back to caller
    return false;
  }

  // rewrite constant pool references in the fields_type_annotations:
  if (!rewrite_cp_refs_in_fields_type_annotations(scratch_class)) {
    // propagate failure back to caller
    return false;
  }

  // rewrite constant pool references in the methods_type_annotations:
  if (!rewrite_cp_refs_in_methods_type_annotations(scratch_class)) {
    // propagate failure back to caller
    return false;
  }

  // There can be type annotations in the Code part of a method_info attribute.
  // These annotations are not accessible, even by reflection.
  // Currently they are not even parsed by the ClassFileParser.
  // If runtime access is added they will also need to be rewritten.

  // rewrite source file name index:
  u2 source_file_name_idx = scratch_class->source_file_name_index();
  if (source_file_name_idx != 0) {
    u2 new_source_file_name_idx = find_new_index(source_file_name_idx);
    if (new_source_file_name_idx != 0) {
      scratch_class->set_source_file_name_index(new_source_file_name_idx);
    }
  }

  // rewrite class generic signature index:
  u2 generic_signature_index = scratch_class->generic_signature_index();
  if (generic_signature_index != 0) {
    u2 new_generic_signature_index = find_new_index(generic_signature_index);
    if (new_generic_signature_index != 0) {
      scratch_class->set_generic_signature_index(new_generic_signature_index);
    }
  }

  return true;
} // end rewrite_cp_refs()

// Rewrite constant pool references in the NestHost and NestMembers attributes.
bool VM_RedefineClasses::rewrite_cp_refs_in_nest_attributes(
       InstanceKlass* scratch_class) {

  u2 cp_index = scratch_class->nest_host_index();
  if (cp_index != 0) {
    scratch_class->set_nest_host_index(find_new_index(cp_index));
  }
  Array<u2>* nest_members = scratch_class->nest_members();
  for (int i = 0; i < nest_members->length(); i++) {
    u2 cp_index = nest_members->at(i);
    nest_members->at_put(i, find_new_index(cp_index));
  }
  return true;
}

// Rewrite constant pool references in the Record attribute.
bool VM_RedefineClasses::rewrite_cp_refs_in_record_attribute(InstanceKlass* scratch_class) {
  Array<RecordComponent*>* components = scratch_class->record_components();
  if (components != NULL) {
    for (int i = 0; i < components->length(); i++) {
      RecordComponent* component = components->at(i);
      u2 cp_index = component->name_index();
      component->set_name_index(find_new_index(cp_index));
      cp_index = component->descriptor_index();
      component->set_descriptor_index(find_new_index(cp_index));
      cp_index = component->generic_signature_index();
      if (cp_index != 0) {
        component->set_generic_signature_index(find_new_index(cp_index));
      }

      AnnotationArray* annotations = component->annotations();
      if (annotations != NULL && annotations->length() != 0) {
        int byte_i = 0;  // byte index into annotations
        if (!rewrite_cp_refs_in_annotations_typeArray(annotations, byte_i)) {
          log_debug(redefine, class, annotation)("bad record_component_annotations at %d", i);
          // propagate failure back to caller
          return false;
        }
      }

      AnnotationArray* type_annotations = component->type_annotations();
      if (type_annotations != NULL && type_annotations->length() != 0) {
        int byte_i = 0;  // byte index into annotations
        if (!rewrite_cp_refs_in_annotations_typeArray(type_annotations, byte_i)) {
          log_debug(redefine, class, annotation)("bad record_component_type_annotations at %d", i);
          // propagate failure back to caller
          return false;
        }
      }
    }
  }
  return true;
}

// Rewrite constant pool references in the PermittedSubclasses attribute.
bool VM_RedefineClasses::rewrite_cp_refs_in_permitted_subclasses_attribute(
       InstanceKlass* scratch_class) {

  Array<u2>* permitted_subclasses = scratch_class->permitted_subclasses();
  assert(permitted_subclasses != NULL, "unexpected null permitted_subclasses");
  for (int i = 0; i < permitted_subclasses->length(); i++) {
    u2 cp_index = permitted_subclasses->at(i);
    permitted_subclasses->at_put(i, find_new_index(cp_index));
  }
  return true;
}

// Rewrite constant pool references in the methods.
bool VM_RedefineClasses::rewrite_cp_refs_in_methods(InstanceKlass* scratch_class) {

  Array<Method*>* methods = scratch_class->methods();

  if (methods == NULL || methods->length() == 0) {
    // no methods so nothing to do
    return true;
  }

  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  ExceptionMark em(THREAD);

  // rewrite constant pool references in the methods:
  for (int i = methods->length() - 1; i >= 0; i--) {
    methodHandle method(THREAD, methods->at(i));
    methodHandle new_method;
    rewrite_cp_refs_in_method(method, &new_method, THREAD);
    if (!new_method.is_null()) {
      // the method has been replaced so save the new method version
      // even in the case of an exception.  original method is on the
      // deallocation list.
      methods->at_put(i, new_method());
    }
    if (HAS_PENDING_EXCEPTION) {
      Symbol* ex_name = PENDING_EXCEPTION->klass()->name();
      log_info(redefine, class, load, exceptions)("rewrite_cp_refs_in_method exception: '%s'", ex_name->as_C_string());
      // Need to clear pending exception here as the super caller sets
      // the JVMTI_ERROR_INTERNAL if the returned value is false.
      CLEAR_PENDING_EXCEPTION;
      return false;
    }
  }

  return true;
}


// Rewrite constant pool references in the specific method. This code
// was adapted from Rewriter::rewrite_method().
void VM_RedefineClasses::rewrite_cp_refs_in_method(methodHandle method,
       methodHandle *new_method_p, TRAPS) {

  *new_method_p = methodHandle();  // default is no new method

  // We cache a pointer to the bytecodes here in code_base. If GC
  // moves the Method*, then the bytecodes will also move which
  // will likely cause a crash. We create a NoSafepointVerifier
  // object to detect whether we pass a possible safepoint in this
  // code block.
  NoSafepointVerifier nsv;

  // Bytecodes and their length
  address code_base = method->code_base();
  int code_length = method->code_size();

  int bc_length;
  for (int bci = 0; bci < code_length; bci += bc_length) {
    address bcp = code_base + bci;
    Bytecodes::Code c = (Bytecodes::Code)(*bcp);

    bc_length = Bytecodes::length_for(c);
    if (bc_length == 0) {
      // More complicated bytecodes report a length of zero so
      // we have to try again a slightly different way.
      bc_length = Bytecodes::length_at(method(), bcp);
    }

    assert(bc_length != 0, "impossible bytecode length");

    switch (c) {
      case Bytecodes::_ldc:
      {
        int cp_index = *(bcp + 1);
        int new_index = find_new_index(cp_index);

        if (StressLdcRewrite && new_index == 0) {
          // If we are stressing ldc -> ldc_w rewriting, then we
          // always need a new_index value.
          new_index = cp_index;
        }
        if (new_index != 0) {
          // the original index is mapped so we have more work to do
          if (!StressLdcRewrite && new_index <= max_jubyte) {
            // The new value can still use ldc instead of ldc_w
            // unless we are trying to stress ldc -> ldc_w rewriting
            log_trace(redefine, class, constantpool)
              ("%s@" INTPTR_FORMAT " old=%d, new=%d", Bytecodes::name(c), p2i(bcp), cp_index, new_index);
            *(bcp + 1) = new_index;
          } else {
            log_trace(redefine, class, constantpool)
              ("%s->ldc_w@" INTPTR_FORMAT " old=%d, new=%d", Bytecodes::name(c), p2i(bcp), cp_index, new_index);
            // the new value needs ldc_w instead of ldc
            u_char inst_buffer[4]; // max instruction size is 4 bytes
            bcp = (address)inst_buffer;
            // construct new instruction sequence
            *bcp = Bytecodes::_ldc_w;
            bcp++;
            // Rewriter::rewrite_method() does not rewrite ldc -> ldc_w.
            // See comment below for difference between put_Java_u2()
            // and put_native_u2().
            Bytes::put_Java_u2(bcp, new_index);

            Relocator rc(method, NULL /* no RelocatorListener needed */);
            methodHandle m;
            {
              PauseNoSafepointVerifier pnsv(&nsv);

              // ldc is 2 bytes and ldc_w is 3 bytes
              m = rc.insert_space_at(bci, 3, inst_buffer, CHECK);
            }

            // return the new method so that the caller can update
            // the containing class
            *new_method_p = method = m;
            // switch our bytecode processing loop from the old method
            // to the new method
            code_base = method->code_base();
            code_length = method->code_size();
            bcp = code_base + bci;
            c = (Bytecodes::Code)(*bcp);
            bc_length = Bytecodes::length_for(c);
            assert(bc_length != 0, "sanity check");
          } // end we need ldc_w instead of ldc
        } // end if there is a mapped index
      } break;

      // these bytecodes have a two-byte constant pool index
      case Bytecodes::_anewarray      : // fall through
      case Bytecodes::_checkcast      : // fall through
      case Bytecodes::_getfield       : // fall through
      case Bytecodes::_getstatic      : // fall through
      case Bytecodes::_instanceof     : // fall through
      case Bytecodes::_invokedynamic  : // fall through
      case Bytecodes::_invokeinterface: // fall through
      case Bytecodes::_invokespecial  : // fall through
      case Bytecodes::_invokestatic   : // fall through
      case Bytecodes::_invokevirtual  : // fall through
      case Bytecodes::_ldc_w          : // fall through
      case Bytecodes::_ldc2_w         : // fall through
      case Bytecodes::_multianewarray : // fall through
      case Bytecodes::_new            : // fall through
      case Bytecodes::_putfield       : // fall through
      case Bytecodes::_putstatic      :
      {
        address p = bcp + 1;
        int cp_index = Bytes::get_Java_u2(p);
        int new_index = find_new_index(cp_index);
        if (new_index != 0) {
          // the original index is mapped so update w/ new value
          log_trace(redefine, class, constantpool)
            ("%s@" INTPTR_FORMAT " old=%d, new=%d", Bytecodes::name(c),p2i(bcp), cp_index, new_index);
          // Rewriter::rewrite_method() uses put_native_u2() in this
          // situation because it is reusing the constant pool index
          // location for a native index into the ConstantPoolCache.
          // Since we are updating the constant pool index prior to
          // verification and ConstantPoolCache initialization, we
          // need to keep the new index in Java byte order.
          Bytes::put_Java_u2(p, new_index);
        }
      } break;
      default:
        break;
    }
  } // end for each bytecode

  // We also need to rewrite the parameter name indexes, if there is
  // method parameter data present
  if(method->has_method_parameters()) {
    const int len = method->method_parameters_length();
    MethodParametersElement* elem = method->method_parameters_start();

    for (int i = 0; i < len; i++) {
      const u2 cp_index = elem[i].name_cp_index;
      const u2 new_cp_index = find_new_index(cp_index);
      if (new_cp_index != 0) {
        elem[i].name_cp_index = new_cp_index;
      }
    }
  }
} // end rewrite_cp_refs_in_method()


// Rewrite constant pool references in the class_annotations field.
bool VM_RedefineClasses::rewrite_cp_refs_in_class_annotations(InstanceKlass* scratch_class) {

  AnnotationArray* class_annotations = scratch_class->class_annotations();
  if (class_annotations == NULL || class_annotations->length() == 0) {
    // no class_annotations so nothing to do
    return true;
  }

  log_debug(redefine, class, annotation)("class_annotations length=%d", class_annotations->length());

  int byte_i = 0;  // byte index into class_annotations
  return rewrite_cp_refs_in_annotations_typeArray(class_annotations, byte_i);
}


// Rewrite constant pool references in an annotations typeArray. This
// "structure" is adapted from the RuntimeVisibleAnnotations_attribute
// that is described in section 4.8.15 of the 2nd-edition of the VM spec:
//
// annotations_typeArray {
//   u2 num_annotations;
//   annotation annotations[num_annotations];
// }
//
bool VM_RedefineClasses::rewrite_cp_refs_in_annotations_typeArray(
       AnnotationArray* annotations_typeArray, int &byte_i_ref) {

  if ((byte_i_ref + 2) > annotations_typeArray->length()) {
    // not enough room for num_annotations field
    log_debug(redefine, class, annotation)("length() is too small for num_annotations field");
    return false;
  }

  u2 num_annotations = Bytes::get_Java_u2((address)
                         annotations_typeArray->adr_at(byte_i_ref));
  byte_i_ref += 2;

  log_debug(redefine, class, annotation)("num_annotations=%d", num_annotations);

  int calc_num_annotations = 0;
  for (; calc_num_annotations < num_annotations; calc_num_annotations++) {
    if (!rewrite_cp_refs_in_annotation_struct(annotations_typeArray, byte_i_ref)) {
      log_debug(redefine, class, annotation)("bad annotation_struct at %d", calc_num_annotations);
      // propagate failure back to caller
      return false;
    }
  }
  assert(num_annotations == calc_num_annotations, "sanity check");

  return true;
} // end rewrite_cp_refs_in_annotations_typeArray()


// Rewrite constant pool references in the annotation struct portion of
// an annotations_typeArray. This "structure" is from section 4.8.15 of
// the 2nd-edition of the VM spec:
//
// struct annotation {
//   u2 type_index;
//   u2 num_element_value_pairs;
//   {
//     u2 element_name_index;
//     element_value value;
//   } element_value_pairs[num_element_value_pairs];
// }
//
bool VM_RedefineClasses::rewrite_cp_refs_in_annotation_struct(
       AnnotationArray* annotations_typeArray, int &byte_i_ref) {
  if ((byte_i_ref + 2 + 2) > annotations_typeArray->length()) {
    // not enough room for smallest annotation_struct
    log_debug(redefine, class, annotation)("length() is too small for annotation_struct");
    return false;
  }

  u2 type_index = rewrite_cp_ref_in_annotation_data(annotations_typeArray,
                    byte_i_ref, "type_index");

  u2 num_element_value_pairs = Bytes::get_Java_u2((address)
                                 annotations_typeArray->adr_at(byte_i_ref));
  byte_i_ref += 2;

  log_debug(redefine, class, annotation)
    ("type_index=%d  num_element_value_pairs=%d", type_index, num_element_value_pairs);

  int calc_num_element_value_pairs = 0;
  for (; calc_num_element_value_pairs < num_element_value_pairs;
       calc_num_element_value_pairs++) {
    if ((byte_i_ref + 2) > annotations_typeArray->length()) {
      // not enough room for another element_name_index, let alone
      // the rest of another component
      log_debug(redefine, class, annotation)("length() is too small for element_name_index");
      return false;
    }

    u2 element_name_index = rewrite_cp_ref_in_annotation_data(
                              annotations_typeArray, byte_i_ref,
                              "element_name_index");

    log_debug(redefine, class, annotation)("element_name_index=%d", element_name_index);

    if (!rewrite_cp_refs_in_element_value(annotations_typeArray, byte_i_ref)) {
      log_debug(redefine, class, annotation)("bad element_value at %d", calc_num_element_value_pairs);
      // propagate failure back to caller
      return false;
    }
  } // end for each component
  assert(num_element_value_pairs == calc_num_element_value_pairs,
    "sanity check");

  return true;
} // end rewrite_cp_refs_in_annotation_struct()


// Rewrite a constant pool reference at the current position in
// annotations_typeArray if needed. Returns the original constant
// pool reference if a rewrite was not needed or the new constant
// pool reference if a rewrite was needed.
u2 VM_RedefineClasses::rewrite_cp_ref_in_annotation_data(
     AnnotationArray* annotations_typeArray, int &byte_i_ref,
     const char * trace_mesg) {

  address cp_index_addr = (address)
    annotations_typeArray->adr_at(byte_i_ref);
  u2 old_cp_index = Bytes::get_Java_u2(cp_index_addr);
  u2 new_cp_index = find_new_index(old_cp_index);
  if (new_cp_index != 0) {
    log_debug(redefine, class, annotation)("mapped old %s=%d", trace_mesg, old_cp_index);
    Bytes::put_Java_u2(cp_index_addr, new_cp_index);
    old_cp_index = new_cp_index;
  }
  byte_i_ref += 2;
  return old_cp_index;
}


// Rewrite constant pool references in the element_value portion of an
// annotations_typeArray. This "structure" is from section 4.8.15.1 of
// the 2nd-edition of the VM spec:
//
// struct element_value {
//   u1 tag;
//   union {
//     u2 const_value_index;
//     {
//       u2 type_name_index;
//       u2 const_name_index;
//     } enum_const_value;
//     u2 class_info_index;
//     annotation annotation_value;
//     struct {
//       u2 num_values;
//       element_value values[num_values];
//     } array_value;
//   } value;
// }
//
bool VM_RedefineClasses::rewrite_cp_refs_in_element_value(
       AnnotationArray* annotations_typeArray, int &byte_i_ref) {

  if ((byte_i_ref + 1) > annotations_typeArray->length()) {
    // not enough room for a tag let alone the rest of an element_value
    log_debug(redefine, class, annotation)("length() is too small for a tag");
    return false;
  }

  u1 tag = annotations_typeArray->at(byte_i_ref);
  byte_i_ref++;
  log_debug(redefine, class, annotation)("tag='%c'", tag);

  switch (tag) {
    // These BaseType tag values are from Table 4.2 in VM spec:
    case JVM_SIGNATURE_BYTE:
    case JVM_SIGNATURE_CHAR:
    case JVM_SIGNATURE_DOUBLE:
    case JVM_SIGNATURE_FLOAT:
    case JVM_SIGNATURE_INT:
    case JVM_SIGNATURE_LONG:
    case JVM_SIGNATURE_SHORT:
    case JVM_SIGNATURE_BOOLEAN:

    // The remaining tag values are from Table 4.8 in the 2nd-edition of
    // the VM spec:
    case 's':
    {
      // For the above tag values (including the BaseType values),
      // value.const_value_index is right union field.

      if ((byte_i_ref + 2) > annotations_typeArray->length()) {
        // not enough room for a const_value_index
        log_debug(redefine, class, annotation)("length() is too small for a const_value_index");
        return false;
      }

      u2 const_value_index = rewrite_cp_ref_in_annotation_data(
                               annotations_typeArray, byte_i_ref,
                               "const_value_index");

      log_debug(redefine, class, annotation)("const_value_index=%d", const_value_index);
    } break;

    case 'e':
    {
      // for the above tag value, value.enum_const_value is right union field

      if ((byte_i_ref + 4) > annotations_typeArray->length()) {
        // not enough room for a enum_const_value
        log_debug(redefine, class, annotation)("length() is too small for a enum_const_value");
        return false;
      }

      u2 type_name_index = rewrite_cp_ref_in_annotation_data(
                             annotations_typeArray, byte_i_ref,
                             "type_name_index");

      u2 const_name_index = rewrite_cp_ref_in_annotation_data(
                              annotations_typeArray, byte_i_ref,
                              "const_name_index");

      log_debug(redefine, class, annotation)
        ("type_name_index=%d  const_name_index=%d", type_name_index, const_name_index);
    } break;

    case 'c':
    {
      // for the above tag value, value.class_info_index is right union field

      if ((byte_i_ref + 2) > annotations_typeArray->length()) {
        // not enough room for a class_info_index
        log_debug(redefine, class, annotation)("length() is too small for a class_info_index");
        return false;
      }

      u2 class_info_index = rewrite_cp_ref_in_annotation_data(
                              annotations_typeArray, byte_i_ref,
                              "class_info_index");

      log_debug(redefine, class, annotation)("class_info_index=%d", class_info_index);
    } break;

    case '@':
      // For the above tag value, value.attr_value is the right union
      // field. This is a nested annotation.
      if (!rewrite_cp_refs_in_annotation_struct(annotations_typeArray, byte_i_ref)) {
        // propagate failure back to caller
        return false;
      }
      break;

    case JVM_SIGNATURE_ARRAY:
    {
      if ((byte_i_ref + 2) > annotations_typeArray->length()) {
        // not enough room for a num_values field
        log_debug(redefine, class, annotation)("length() is too small for a num_values field");
        return false;
      }

      // For the above tag value, value.array_value is the right union
      // field. This is an array of nested element_value.
      u2 num_values = Bytes::get_Java_u2((address)
                        annotations_typeArray->adr_at(byte_i_ref));
      byte_i_ref += 2;
      log_debug(redefine, class, annotation)("num_values=%d", num_values);

      int calc_num_values = 0;
      for (; calc_num_values < num_values; calc_num_values++) {
        if (!rewrite_cp_refs_in_element_value(annotations_typeArray, byte_i_ref)) {
          log_debug(redefine, class, annotation)("bad nested element_value at %d", calc_num_values);
          // propagate failure back to caller
          return false;
        }
      }
      assert(num_values == calc_num_values, "sanity check");
    } break;

    default:
      log_debug(redefine, class, annotation)("bad tag=0x%x", tag);
      return false;
  } // end decode tag field

  return true;
} // end rewrite_cp_refs_in_element_value()


// Rewrite constant pool references in a fields_annotations field.
bool VM_RedefineClasses::rewrite_cp_refs_in_fields_annotations(
       InstanceKlass* scratch_class) {

  Array<AnnotationArray*>* fields_annotations = scratch_class->fields_annotations();

  if (fields_annotations == NULL || fields_annotations->length() == 0) {
    // no fields_annotations so nothing to do
    return true;
  }

  log_debug(redefine, class, annotation)("fields_annotations length=%d", fields_annotations->length());

  for (int i = 0; i < fields_annotations->length(); i++) {
    AnnotationArray* field_annotations = fields_annotations->at(i);
    if (field_annotations == NULL || field_annotations->length() == 0) {
      // this field does not have any annotations so skip it
      continue;
    }

    int byte_i = 0;  // byte index into field_annotations
    if (!rewrite_cp_refs_in_annotations_typeArray(field_annotations, byte_i)) {
      log_debug(redefine, class, annotation)("bad field_annotations at %d", i);
      // propagate failure back to caller
      return false;
    }
  }

  return true;
} // end rewrite_cp_refs_in_fields_annotations()


// Rewrite constant pool references in a methods_annotations field.
bool VM_RedefineClasses::rewrite_cp_refs_in_methods_annotations(
       InstanceKlass* scratch_class) {

  for (int i = 0; i < scratch_class->methods()->length(); i++) {
    Method* m = scratch_class->methods()->at(i);
    AnnotationArray* method_annotations = m->constMethod()->method_annotations();

    if (method_annotations == NULL || method_annotations->length() == 0) {
      // this method does not have any annotations so skip it
      continue;
    }

    int byte_i = 0;  // byte index into method_annotations
    if (!rewrite_cp_refs_in_annotations_typeArray(method_annotations, byte_i)) {
      log_debug(redefine, class, annotation)("bad method_annotations at %d", i);
      // propagate failure back to caller
      return false;
    }
  }

  return true;
} // end rewrite_cp_refs_in_methods_annotations()


// Rewrite constant pool references in a methods_parameter_annotations
// field. This "structure" is adapted from the
// RuntimeVisibleParameterAnnotations_attribute described in section
// 4.8.17 of the 2nd-edition of the VM spec:
//
// methods_parameter_annotations_typeArray {
//   u1 num_parameters;
//   {
//     u2 num_annotations;
//     annotation annotations[num_annotations];
//   } parameter_annotations[num_parameters];
// }
//
bool VM_RedefineClasses::rewrite_cp_refs_in_methods_parameter_annotations(
       InstanceKlass* scratch_class) {

  for (int i = 0; i < scratch_class->methods()->length(); i++) {
    Method* m = scratch_class->methods()->at(i);
    AnnotationArray* method_parameter_annotations = m->constMethod()->parameter_annotations();
    if (method_parameter_annotations == NULL
        || method_parameter_annotations->length() == 0) {
      // this method does not have any parameter annotations so skip it
      continue;
    }

    if (method_parameter_annotations->length() < 1) {
      // not enough room for a num_parameters field
      log_debug(redefine, class, annotation)("length() is too small for a num_parameters field at %d", i);
      return false;
    }

    int byte_i = 0;  // byte index into method_parameter_annotations

    u1 num_parameters = method_parameter_annotations->at(byte_i);
    byte_i++;

    log_debug(redefine, class, annotation)("num_parameters=%d", num_parameters);

    int calc_num_parameters = 0;
    for (; calc_num_parameters < num_parameters; calc_num_parameters++) {
      if (!rewrite_cp_refs_in_annotations_typeArray(method_parameter_annotations, byte_i)) {
        log_debug(redefine, class, annotation)("bad method_parameter_annotations at %d", calc_num_parameters);
        // propagate failure back to caller
        return false;
      }
    }
    assert(num_parameters == calc_num_parameters, "sanity check");
  }

  return true;
} // end rewrite_cp_refs_in_methods_parameter_annotations()


// Rewrite constant pool references in a methods_default_annotations
// field. This "structure" is adapted from the AnnotationDefault_attribute
// that is described in section 4.8.19 of the 2nd-edition of the VM spec:
//
// methods_default_annotations_typeArray {
//   element_value default_value;
// }
//
bool VM_RedefineClasses::rewrite_cp_refs_in_methods_default_annotations(
       InstanceKlass* scratch_class) {

  for (int i = 0; i < scratch_class->methods()->length(); i++) {
    Method* m = scratch_class->methods()->at(i);
    AnnotationArray* method_default_annotations = m->constMethod()->default_annotations();
    if (method_default_annotations == NULL
        || method_default_annotations->length() == 0) {
      // this method does not have any default annotations so skip it
      continue;
    }

    int byte_i = 0;  // byte index into method_default_annotations

    if (!rewrite_cp_refs_in_element_value(
           method_default_annotations, byte_i)) {
      log_debug(redefine, class, annotation)("bad default element_value at %d", i);
      // propagate failure back to caller
      return false;
    }
  }

  return true;
} // end rewrite_cp_refs_in_methods_default_annotations()


// Rewrite constant pool references in a class_type_annotations field.
bool VM_RedefineClasses::rewrite_cp_refs_in_class_type_annotations(
       InstanceKlass* scratch_class) {

  AnnotationArray* class_type_annotations = scratch_class->class_type_annotations();
  if (class_type_annotations == NULL || class_type_annotations->length() == 0) {
    // no class_type_annotations so nothing to do
    return true;
  }

  log_debug(redefine, class, annotation)("class_type_annotations length=%d", class_type_annotations->length());

  int byte_i = 0;  // byte index into class_type_annotations
  return rewrite_cp_refs_in_type_annotations_typeArray(class_type_annotations,
      byte_i, "ClassFile");
} // end rewrite_cp_refs_in_class_type_annotations()


// Rewrite constant pool references in a fields_type_annotations field.
bool VM_RedefineClasses::rewrite_cp_refs_in_fields_type_annotations(InstanceKlass* scratch_class) {

  Array<AnnotationArray*>* fields_type_annotations = scratch_class->fields_type_annotations();
  if (fields_type_annotations == NULL || fields_type_annotations->length() == 0) {
    // no fields_type_annotations so nothing to do
    return true;
  }

  log_debug(redefine, class, annotation)("fields_type_annotations length=%d", fields_type_annotations->length());

  for (int i = 0; i < fields_type_annotations->length(); i++) {
    AnnotationArray* field_type_annotations = fields_type_annotations->at(i);
    if (field_type_annotations == NULL || field_type_annotations->length() == 0) {
      // this field does not have any annotations so skip it
      continue;
    }

    int byte_i = 0;  // byte index into field_type_annotations
    if (!rewrite_cp_refs_in_type_annotations_typeArray(field_type_annotations,
           byte_i, "field_info")) {
      log_debug(redefine, class, annotation)("bad field_type_annotations at %d", i);
      // propagate failure back to caller
      return false;
    }
  }

  return true;
} // end rewrite_cp_refs_in_fields_type_annotations()


// Rewrite constant pool references in a methods_type_annotations field.
bool VM_RedefineClasses::rewrite_cp_refs_in_methods_type_annotations(
       InstanceKlass* scratch_class) {

  for (int i = 0; i < scratch_class->methods()->length(); i++) {
    Method* m = scratch_class->methods()->at(i);
    AnnotationArray* method_type_annotations = m->constMethod()->type_annotations();

    if (method_type_annotations == NULL || method_type_annotations->length() == 0) {
      // this method does not have any annotations so skip it
      continue;
    }

    log_debug(redefine, class, annotation)("methods type_annotations length=%d", method_type_annotations->length());

    int byte_i = 0;  // byte index into method_type_annotations
    if (!rewrite_cp_refs_in_type_annotations_typeArray(method_type_annotations,
           byte_i, "method_info")) {
      log_debug(redefine, class, annotation)("bad method_type_annotations at %d", i);
      // propagate failure back to caller
      return false;
    }
  }

  return true;
} // end rewrite_cp_refs_in_methods_type_annotations()


// Rewrite constant pool references in a type_annotations
// field. This "structure" is adapted from the
// RuntimeVisibleTypeAnnotations_attribute described in
// section 4.7.20 of the Java SE 8 Edition of the VM spec:
//
// type_annotations_typeArray {
//   u2              num_annotations;
//   type_annotation annotations[num_annotations];
// }
//
bool VM_RedefineClasses::rewrite_cp_refs_in_type_annotations_typeArray(
       AnnotationArray* type_annotations_typeArray, int &byte_i_ref,
       const char * location_mesg) {

  if ((byte_i_ref + 2) > type_annotations_typeArray->length()) {
    // not enough room for num_annotations field
    log_debug(redefine, class, annotation)("length() is too small for num_annotations field");
    return false;
  }

  u2 num_annotations = Bytes::get_Java_u2((address)
                         type_annotations_typeArray->adr_at(byte_i_ref));
  byte_i_ref += 2;

  log_debug(redefine, class, annotation)("num_type_annotations=%d", num_annotations);

  int calc_num_annotations = 0;
  for (; calc_num_annotations < num_annotations; calc_num_annotations++) {
    if (!rewrite_cp_refs_in_type_annotation_struct(type_annotations_typeArray,
           byte_i_ref, location_mesg)) {
      log_debug(redefine, class, annotation)("bad type_annotation_struct at %d", calc_num_annotations);
      // propagate failure back to caller
      return false;
    }
  }
  assert(num_annotations == calc_num_annotations, "sanity check");

  if (byte_i_ref != type_annotations_typeArray->length()) {
    log_debug(redefine, class, annotation)
      ("read wrong amount of bytes at end of processing type_annotations_typeArray (%d of %d bytes were read)",
       byte_i_ref, type_annotations_typeArray->length());
    return false;
  }

  return true;
} // end rewrite_cp_refs_in_type_annotations_typeArray()


// Rewrite constant pool references in a type_annotation
// field. This "structure" is adapted from the
// RuntimeVisibleTypeAnnotations_attribute described in
// section 4.7.20 of the Java SE 8 Edition of the VM spec:
//
// type_annotation {
//   u1 target_type;
//   union {
//     type_parameter_target;
//     supertype_target;
//     type_parameter_bound_target;
//     empty_target;
//     method_formal_parameter_target;
//     throws_target;
//     localvar_target;
//     catch_target;
//     offset_target;
//     type_argument_target;
//   } target_info;
//   type_path target_path;
//   annotation anno;
// }
//
bool VM_RedefineClasses::rewrite_cp_refs_in_type_annotation_struct(
       AnnotationArray* type_annotations_typeArray, int &byte_i_ref,
       const char * location_mesg) {

  if (!skip_type_annotation_target(type_annotations_typeArray,
         byte_i_ref, location_mesg)) {
    return false;
  }

  if (!skip_type_annotation_type_path(type_annotations_typeArray, byte_i_ref)) {
    return false;
  }

  if (!rewrite_cp_refs_in_annotation_struct(type_annotations_typeArray, byte_i_ref)) {
    return false;
  }

  return true;
} // end rewrite_cp_refs_in_type_annotation_struct()


// Read, verify and skip over the target_type and target_info part
// so that rewriting can continue in the later parts of the struct.
//
// u1 target_type;
// union {
//   type_parameter_target;
//   supertype_target;
//   type_parameter_bound_target;
//   empty_target;
//   method_formal_parameter_target;
//   throws_target;
//   localvar_target;
//   catch_target;
//   offset_target;
//   type_argument_target;
// } target_info;
//
bool VM_RedefineClasses::skip_type_annotation_target(
       AnnotationArray* type_annotations_typeArray, int &byte_i_ref,
       const char * location_mesg) {

  if ((byte_i_ref + 1) > type_annotations_typeArray->length()) {
    // not enough room for a target_type let alone the rest of a type_annotation
    log_debug(redefine, class, annotation)("length() is too small for a target_type");
    return false;
  }

  u1 target_type = type_annotations_typeArray->at(byte_i_ref);
  byte_i_ref += 1;
  log_debug(redefine, class, annotation)("target_type=0x%.2x", target_type);
  log_debug(redefine, class, annotation)("location=%s", location_mesg);

  // Skip over target_info
  switch (target_type) {
    case 0x00:
    // kind: type parameter declaration of generic class or interface
    // location: ClassFile
    case 0x01:
    // kind: type parameter declaration of generic method or constructor
    // location: method_info

    {
      // struct:
      // type_parameter_target {
      //   u1 type_parameter_index;
      // }
      //
      if ((byte_i_ref + 1) > type_annotations_typeArray->length()) {
        log_debug(redefine, class, annotation)("length() is too small for a type_parameter_target");
        return false;
      }

      u1 type_parameter_index = type_annotations_typeArray->at(byte_i_ref);
      byte_i_ref += 1;

      log_debug(redefine, class, annotation)("type_parameter_target: type_parameter_index=%d", type_parameter_index);
    } break;

    case 0x10:
    // kind: type in extends clause of class or interface declaration
    //       or in implements clause of interface declaration
    // location: ClassFile

    {
      // struct:
      // supertype_target {
      //   u2 supertype_index;
      // }
      //
      if ((byte_i_ref + 2) > type_annotations_typeArray->length()) {
        log_debug(redefine, class, annotation)("length() is too small for a supertype_target");
        return false;
      }

      u2 supertype_index = Bytes::get_Java_u2((address)
                             type_annotations_typeArray->adr_at(byte_i_ref));
      byte_i_ref += 2;

      log_debug(redefine, class, annotation)("supertype_target: supertype_index=%d", supertype_index);
    } break;

    case 0x11:
    // kind: type in bound of type parameter declaration of generic class or interface
    // location: ClassFile
    case 0x12:
    // kind: type in bound of type parameter declaration of generic method or constructor
    // location: method_info

    {
      // struct:
      // type_parameter_bound_target {
      //   u1 type_parameter_index;
      //   u1 bound_index;
      // }
      //
      if ((byte_i_ref + 2) > type_annotations_typeArray->length()) {
        log_debug(redefine, class, annotation)("length() is too small for a type_parameter_bound_target");
        return false;
      }

      u1 type_parameter_index = type_annotations_typeArray->at(byte_i_ref);
      byte_i_ref += 1;
      u1 bound_index = type_annotations_typeArray->at(byte_i_ref);
      byte_i_ref += 1;

      log_debug(redefine, class, annotation)
        ("type_parameter_bound_target: type_parameter_index=%d, bound_index=%d", type_parameter_index, bound_index);
    } break;

    case 0x13:
    // kind: type in field declaration
    // location: field_info
    case 0x14:
    // kind: return type of method, or type of newly constructed object
    // location: method_info
    case 0x15:
    // kind: receiver type of method or constructor
    // location: method_info

    {
      // struct:
      // empty_target {
      // }
      //
      log_debug(redefine, class, annotation)("empty_target");
    } break;

    case 0x16:
    // kind: type in formal parameter declaration of method, constructor, or lambda expression
    // location: method_info

    {
      // struct:
      // formal_parameter_target {
      //   u1 formal_parameter_index;
      // }
      //
      if ((byte_i_ref + 1) > type_annotations_typeArray->length()) {
        log_debug(redefine, class, annotation)("length() is too small for a formal_parameter_target");
        return false;
      }

      u1 formal_parameter_index = type_annotations_typeArray->at(byte_i_ref);
      byte_i_ref += 1;

      log_debug(redefine, class, annotation)
        ("formal_parameter_target: formal_parameter_index=%d", formal_parameter_index);
    } break;

    case 0x17:
    // kind: type in throws clause of method or constructor
    // location: method_info

    {
      // struct:
      // throws_target {
      //   u2 throws_type_index
      // }
      //
      if ((byte_i_ref + 2) > type_annotations_typeArray->length()) {
        log_debug(redefine, class, annotation)("length() is too small for a throws_target");
        return false;
      }

      u2 throws_type_index = Bytes::get_Java_u2((address)
                               type_annotations_typeArray->adr_at(byte_i_ref));
      byte_i_ref += 2;

      log_debug(redefine, class, annotation)("throws_target: throws_type_index=%d", throws_type_index);
    } break;

    case 0x40:
    // kind: type in local variable declaration
    // location: Code
    case 0x41:
    // kind: type in resource variable declaration
    // location: Code

    {
      // struct:
      // localvar_target {
      //   u2 table_length;
      //   struct {
      //     u2 start_pc;
      //     u2 length;
      //     u2 index;
      //   } table[table_length];
      // }
      //
      if ((byte_i_ref + 2) > type_annotations_typeArray->length()) {
        // not enough room for a table_length let alone the rest of a localvar_target
        log_debug(redefine, class, annotation)("length() is too small for a localvar_target table_length");
        return false;
      }

      u2 table_length = Bytes::get_Java_u2((address)
                          type_annotations_typeArray->adr_at(byte_i_ref));
      byte_i_ref += 2;

      log_debug(redefine, class, annotation)("localvar_target: table_length=%d", table_length);

      int table_struct_size = 2 + 2 + 2; // 3 u2 variables per table entry
      int table_size = table_length * table_struct_size;

      if ((byte_i_ref + table_size) > type_annotations_typeArray->length()) {
        // not enough room for a table
        log_debug(redefine, class, annotation)("length() is too small for a table array of length %d", table_length);
        return false;
      }

      // Skip over table
      byte_i_ref += table_size;
    } break;

    case 0x42:
    // kind: type in exception parameter declaration
    // location: Code

    {
      // struct:
      // catch_target {
      //   u2 exception_table_index;
      // }
      //
      if ((byte_i_ref + 2) > type_annotations_typeArray->length()) {
        log_debug(redefine, class, annotation)("length() is too small for a catch_target");
        return false;
      }

      u2 exception_table_index = Bytes::get_Java_u2((address)
                                   type_annotations_typeArray->adr_at(byte_i_ref));
      byte_i_ref += 2;

      log_debug(redefine, class, annotation)("catch_target: exception_table_index=%d", exception_table_index);
    } break;

    case 0x43:
    // kind: type in instanceof expression
    // location: Code
    case 0x44:
    // kind: type in new expression
    // location: Code
    case 0x45:
    // kind: type in method reference expression using ::new
    // location: Code
    case 0x46:
    // kind: type in method reference expression using ::Identifier
    // location: Code

    {
      // struct:
      // offset_target {
      //   u2 offset;
      // }
      //
      if ((byte_i_ref + 2) > type_annotations_typeArray->length()) {
        log_debug(redefine, class, annotation)("length() is too small for a offset_target");
        return false;
      }

      u2 offset = Bytes::get_Java_u2((address)
                    type_annotations_typeArray->adr_at(byte_i_ref));
      byte_i_ref += 2;

      log_debug(redefine, class, annotation)("offset_target: offset=%d", offset);
    } break;

    case 0x47:
    // kind: type in cast expression
    // location: Code
    case 0x48:
    // kind: type argument for generic constructor in new expression or
    //       explicit constructor invocation statement
    // location: Code
    case 0x49:
    // kind: type argument for generic method in method invocation expression
    // location: Code
    case 0x4A:
    // kind: type argument for generic constructor in method reference expression using ::new
    // location: Code
    case 0x4B:
    // kind: type argument for generic method in method reference expression using ::Identifier
    // location: Code

    {
      // struct:
      // type_argument_target {
      //   u2 offset;
      //   u1 type_argument_index;
      // }
      //
      if ((byte_i_ref + 3) > type_annotations_typeArray->length()) {
        log_debug(redefine, class, annotation)("length() is too small for a type_argument_target");
        return false;
      }

      u2 offset = Bytes::get_Java_u2((address)
                    type_annotations_typeArray->adr_at(byte_i_ref));
      byte_i_ref += 2;
      u1 type_argument_index = type_annotations_typeArray->at(byte_i_ref);
      byte_i_ref += 1;

      log_debug(redefine, class, annotation)
        ("type_argument_target: offset=%d, type_argument_index=%d", offset, type_argument_index);
    } break;

    default:
      log_debug(redefine, class, annotation)("unknown target_type");
#ifdef ASSERT
      ShouldNotReachHere();
#endif
      return false;
  }

  return true;
} // end skip_type_annotation_target()


// Read, verify and skip over the type_path part so that rewriting
// can continue in the later parts of the struct.
//
// type_path {
//   u1 path_length;
//   {
//     u1 type_path_kind;
//     u1 type_argument_index;
//   } path[path_length];
// }
//
bool VM_RedefineClasses::skip_type_annotation_type_path(
       AnnotationArray* type_annotations_typeArray, int &byte_i_ref) {

  if ((byte_i_ref + 1) > type_annotations_typeArray->length()) {
    // not enough room for a path_length let alone the rest of the type_path
    log_debug(redefine, class, annotation)("length() is too small for a type_path");
    return false;
  }

  u1 path_length = type_annotations_typeArray->at(byte_i_ref);
  byte_i_ref += 1;

  log_debug(redefine, class, annotation)("type_path: path_length=%d", path_length);

  int calc_path_length = 0;
  for (; calc_path_length < path_length; calc_path_length++) {
    if ((byte_i_ref + 1 + 1) > type_annotations_typeArray->length()) {
      // not enough room for a path
      log_debug(redefine, class, annotation)
        ("length() is too small for path entry %d of %d", calc_path_length, path_length);
      return false;
    }

    u1 type_path_kind = type_annotations_typeArray->at(byte_i_ref);
    byte_i_ref += 1;
    u1 type_argument_index = type_annotations_typeArray->at(byte_i_ref);
    byte_i_ref += 1;

    log_debug(redefine, class, annotation)
      ("type_path: path[%d]: type_path_kind=%d, type_argument_index=%d",
       calc_path_length, type_path_kind, type_argument_index);

    if (type_path_kind > 3 || (type_path_kind != 3 && type_argument_index != 0)) {
      // not enough room for a path
      log_debug(redefine, class, annotation)("inconsistent type_path values");
      return false;
    }
  }
  assert(path_length == calc_path_length, "sanity check");

  return true;
} // end skip_type_annotation_type_path()


// Rewrite constant pool references in the method's stackmap table.
// These "structures" are adapted from the StackMapTable_attribute that
// is described in section 4.8.4 of the 6.0 version of the VM spec
// (dated 2005.10.26):
// file:///net/quincunx.sfbay/export/gbracha/ClassFile-Java6.pdf
//
// stack_map {
//   u2 number_of_entries;
//   stack_map_frame entries[number_of_entries];
// }
//
void VM_RedefineClasses::rewrite_cp_refs_in_stack_map_table(
       const methodHandle& method) {

  if (!method->has_stackmap_table()) {
    return;
  }

  AnnotationArray* stackmap_data = method->stackmap_data();
  address stackmap_p = (address)stackmap_data->adr_at(0);
  address stackmap_end = stackmap_p + stackmap_data->length();

  assert(stackmap_p + 2 <= stackmap_end, "no room for number_of_entries");
  u2 number_of_entries = Bytes::get_Java_u2(stackmap_p);
  stackmap_p += 2;

  log_debug(redefine, class, stackmap)("number_of_entries=%u", number_of_entries);

  // walk through each stack_map_frame
  u2 calc_number_of_entries = 0;
  for (; calc_number_of_entries < number_of_entries; calc_number_of_entries++) {
    // The stack_map_frame structure is a u1 frame_type followed by
    // 0 or more bytes of data:
    //
    // union stack_map_frame {
    //   same_frame;
    //   same_locals_1_stack_item_frame;
    //   same_locals_1_stack_item_frame_extended;
    //   chop_frame;
    //   same_frame_extended;
    //   append_frame;
    //   full_frame;
    // }

    assert(stackmap_p + 1 <= stackmap_end, "no room for frame_type");
    u1 frame_type = *stackmap_p;
    stackmap_p++;

    // same_frame {
    //   u1 frame_type = SAME; /* 0-63 */
    // }
    if (frame_type <= 63) {
      // nothing more to do for same_frame
    }

    // same_locals_1_stack_item_frame {
    //   u1 frame_type = SAME_LOCALS_1_STACK_ITEM; /* 64-127 */
    //   verification_type_info stack[1];
    // }
    else if (frame_type >= 64 && frame_type <= 127) {
      rewrite_cp_refs_in_verification_type_info(stackmap_p, stackmap_end,
        calc_number_of_entries, frame_type);
    }

    // reserved for future use
    else if (frame_type >= 128 && frame_type <= 246) {
      // nothing more to do for reserved frame_types
    }

    // same_locals_1_stack_item_frame_extended {
    //   u1 frame_type = SAME_LOCALS_1_STACK_ITEM_EXTENDED; /* 247 */
    //   u2 offset_delta;
    //   verification_type_info stack[1];
    // }
    else if (frame_type == 247) {
      stackmap_p += 2;
      rewrite_cp_refs_in_verification_type_info(stackmap_p, stackmap_end,
        calc_number_of_entries, frame_type);
    }

    // chop_frame {
    //   u1 frame_type = CHOP; /* 248-250 */
    //   u2 offset_delta;
    // }
    else if (frame_type >= 248 && frame_type <= 250) {
      stackmap_p += 2;
    }

    // same_frame_extended {
    //   u1 frame_type = SAME_FRAME_EXTENDED; /* 251*/
    //   u2 offset_delta;
    // }
    else if (frame_type == 251) {
      stackmap_p += 2;
    }

    // append_frame {
    //   u1 frame_type = APPEND; /* 252-254 */
    //   u2 offset_delta;
    //   verification_type_info locals[frame_type - 251];
    // }
    else if (frame_type >= 252 && frame_type <= 254) {
      assert(stackmap_p + 2 <= stackmap_end,
        "no room for offset_delta");
      stackmap_p += 2;
      u1 len = frame_type - 251;
      for (u1 i = 0; i < len; i++) {
        rewrite_cp_refs_in_verification_type_info(stackmap_p, stackmap_end,
          calc_number_of_entries, frame_type);
      }
    }

    // full_frame {
    //   u1 frame_type = FULL_FRAME; /* 255 */
    //   u2 offset_delta;
    //   u2 number_of_locals;
    //   verification_type_info locals[number_of_locals];
    //   u2 number_of_stack_items;
    //   verification_type_info stack[number_of_stack_items];
    // }
    else if (frame_type == 255) {
      assert(stackmap_p + 2 + 2 <= stackmap_end,
        "no room for smallest full_frame");
      stackmap_p += 2;

      u2 number_of_locals = Bytes::get_Java_u2(stackmap_p);
      stackmap_p += 2;

      for (u2 locals_i = 0; locals_i < number_of_locals; locals_i++) {
        rewrite_cp_refs_in_verification_type_info(stackmap_p, stackmap_end,
          calc_number_of_entries, frame_type);
      }

      // Use the largest size for the number_of_stack_items, but only get
      // the right number of bytes.
      u2 number_of_stack_items = Bytes::get_Java_u2(stackmap_p);
      stackmap_p += 2;

      for (u2 stack_i = 0; stack_i < number_of_stack_items; stack_i++) {
        rewrite_cp_refs_in_verification_type_info(stackmap_p, stackmap_end,
          calc_number_of_entries, frame_type);
      }
    }
  } // end while there is a stack_map_frame
  assert(number_of_entries == calc_number_of_entries, "sanity check");
} // end rewrite_cp_refs_in_stack_map_table()


// Rewrite constant pool references in the verification type info
// portion of the method's stackmap table. These "structures" are
// adapted from the StackMapTable_attribute that is described in
// section 4.8.4 of the 6.0 version of the VM spec (dated 2005.10.26):
// file:///net/quincunx.sfbay/export/gbracha/ClassFile-Java6.pdf
//
// The verification_type_info structure is a u1 tag followed by 0 or
// more bytes of data:
//
// union verification_type_info {
//   Top_variable_info;
//   Integer_variable_info;
//   Float_variable_info;
//   Long_variable_info;
//   Double_variable_info;
//   Null_variable_info;
//   UninitializedThis_variable_info;
//   Object_variable_info;
//   Uninitialized_variable_info;
// }
//
void VM_RedefineClasses::rewrite_cp_refs_in_verification_type_info(
       address& stackmap_p_ref, address stackmap_end, u2 frame_i,
       u1 frame_type) {

  assert(stackmap_p_ref + 1 <= stackmap_end, "no room for tag");
  u1 tag = *stackmap_p_ref;
  stackmap_p_ref++;

  switch (tag) {
  // Top_variable_info {
  //   u1 tag = ITEM_Top; /* 0 */
  // }
  // verificationType.hpp has zero as ITEM_Bogus instead of ITEM_Top
  case 0:  // fall through

  // Integer_variable_info {
  //   u1 tag = ITEM_Integer; /* 1 */
  // }
  case ITEM_Integer:  // fall through

  // Float_variable_info {
  //   u1 tag = ITEM_Float; /* 2 */
  // }
  case ITEM_Float:  // fall through

  // Double_variable_info {
  //   u1 tag = ITEM_Double; /* 3 */
  // }
  case ITEM_Double:  // fall through

  // Long_variable_info {
  //   u1 tag = ITEM_Long; /* 4 */
  // }
  case ITEM_Long:  // fall through

  // Null_variable_info {
  //   u1 tag = ITEM_Null; /* 5 */
  // }
  case ITEM_Null:  // fall through

  // UninitializedThis_variable_info {
  //   u1 tag = ITEM_UninitializedThis; /* 6 */
  // }
  case ITEM_UninitializedThis:
    // nothing more to do for the above tag types
    break;

  // Object_variable_info {
  //   u1 tag = ITEM_Object; /* 7 */
  //   u2 cpool_index;
  // }
  case ITEM_Object:
  {
    assert(stackmap_p_ref + 2 <= stackmap_end, "no room for cpool_index");
    u2 cpool_index = Bytes::get_Java_u2(stackmap_p_ref);
    u2 new_cp_index = find_new_index(cpool_index);
    if (new_cp_index != 0) {
      log_debug(redefine, class, stackmap)("mapped old cpool_index=%d", cpool_index);
      Bytes::put_Java_u2(stackmap_p_ref, new_cp_index);
      cpool_index = new_cp_index;
    }
    stackmap_p_ref += 2;

    log_debug(redefine, class, stackmap)
      ("frame_i=%u, frame_type=%u, cpool_index=%d", frame_i, frame_type, cpool_index);
  } break;

  // Uninitialized_variable_info {
  //   u1 tag = ITEM_Uninitialized; /* 8 */
  //   u2 offset;
  // }
  case ITEM_Uninitialized:
    assert(stackmap_p_ref + 2 <= stackmap_end, "no room for offset");
    stackmap_p_ref += 2;
    break;

  default:
    log_debug(redefine, class, stackmap)("frame_i=%u, frame_type=%u, bad tag=0x%x", frame_i, frame_type, tag);
    ShouldNotReachHere();
    break;
  } // end switch (tag)
} // end rewrite_cp_refs_in_verification_type_info()


// Change the constant pool associated with klass scratch_class to
// scratch_cp. If shrink is true, then scratch_cp_length elements
// are copied from scratch_cp to a smaller constant pool and the
// smaller constant pool is associated with scratch_class.
void VM_RedefineClasses::set_new_constant_pool(
       ClassLoaderData* loader_data,
       InstanceKlass* scratch_class, constantPoolHandle scratch_cp,
       int scratch_cp_length, TRAPS) {
  assert(scratch_cp->length() >= scratch_cp_length, "sanity check");

  // scratch_cp is a merged constant pool and has enough space for a
  // worst case merge situation. We want to associate the minimum
  // sized constant pool with the klass to save space.
  ConstantPool* cp = ConstantPool::allocate(loader_data, scratch_cp_length, CHECK);
  constantPoolHandle smaller_cp(THREAD, cp);

  // preserve version() value in the smaller copy
  int version = scratch_cp->version();
  assert(version != 0, "sanity check");
  smaller_cp->set_version(version);

  // attach klass to new constant pool
  // reference to the cp holder is needed for copy_operands()
  smaller_cp->set_pool_holder(scratch_class);

  smaller_cp->copy_fields(scratch_cp());

  scratch_cp->copy_cp_to(1, scratch_cp_length - 1, smaller_cp, 1, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    // Exception is handled in the caller
    loader_data->add_to_deallocate_list(smaller_cp());
    return;
  }
  scratch_cp = smaller_cp;

  // attach new constant pool to klass
  scratch_class->set_constants(scratch_cp());
  scratch_cp->initialize_unresolved_klasses(loader_data, CHECK);

  int i;  // for portability

  // update each field in klass to use new constant pool indices as needed
  for (JavaFieldStream fs(scratch_class); !fs.done(); fs.next()) {
    jshort cur_index = fs.name_index();
    jshort new_index = find_new_index(cur_index);
    if (new_index != 0) {
      log_trace(redefine, class, constantpool)("field-name_index change: %d to %d", cur_index, new_index);
      fs.set_name_index(new_index);
    }
    cur_index = fs.signature_index();
    new_index = find_new_index(cur_index);
    if (new_index != 0) {
      log_trace(redefine, class, constantpool)("field-signature_index change: %d to %d", cur_index, new_index);
      fs.set_signature_index(new_index);
    }
    cur_index = fs.initval_index();
    new_index = find_new_index(cur_index);
    if (new_index != 0) {
      log_trace(redefine, class, constantpool)("field-initval_index change: %d to %d", cur_index, new_index);
      fs.set_initval_index(new_index);
    }
    cur_index = fs.generic_signature_index();
    new_index = find_new_index(cur_index);
    if (new_index != 0) {
      log_trace(redefine, class, constantpool)("field-generic_signature change: %d to %d", cur_index, new_index);
      fs.set_generic_signature_index(new_index);
    }
  } // end for each field

  // Update constant pool indices in the inner classes info to use
  // new constant indices as needed. The inner classes info is a
  // quadruple:
  // (inner_class_info, outer_class_info, inner_name, inner_access_flags)
  InnerClassesIterator iter(scratch_class);
  for (; !iter.done(); iter.next()) {
    int cur_index = iter.inner_class_info_index();
    if (cur_index == 0) {
      continue;  // JVM spec. allows null inner class refs so skip it
    }
    int new_index = find_new_index(cur_index);
    if (new_index != 0) {
      log_trace(redefine, class, constantpool)("inner_class_info change: %d to %d", cur_index, new_index);
      iter.set_inner_class_info_index(new_index);
    }
    cur_index = iter.outer_class_info_index();
    new_index = find_new_index(cur_index);
    if (new_index != 0) {
      log_trace(redefine, class, constantpool)("outer_class_info change: %d to %d", cur_index, new_index);
      iter.set_outer_class_info_index(new_index);
    }
    cur_index = iter.inner_name_index();
    new_index = find_new_index(cur_index);
    if (new_index != 0) {
      log_trace(redefine, class, constantpool)("inner_name change: %d to %d", cur_index, new_index);
      iter.set_inner_name_index(new_index);
    }
  } // end for each inner class

  // Attach each method in klass to the new constant pool and update
  // to use new constant pool indices as needed:
  Array<Method*>* methods = scratch_class->methods();
  for (i = methods->length() - 1; i >= 0; i--) {
    methodHandle method(THREAD, methods->at(i));
    method->set_constants(scratch_cp());

    int new_index = find_new_index(method->name_index());
    if (new_index != 0) {
      log_trace(redefine, class, constantpool)
        ("method-name_index change: %d to %d", method->name_index(), new_index);
      method->set_name_index(new_index);
    }
    new_index = find_new_index(method->signature_index());
    if (new_index != 0) {
      log_trace(redefine, class, constantpool)
        ("method-signature_index change: %d to %d", method->signature_index(), new_index);
      method->set_signature_index(new_index);
    }
    new_index = find_new_index(method->generic_signature_index());
    if (new_index != 0) {
      log_trace(redefine, class, constantpool)
        ("method-generic_signature_index change: %d to %d", method->generic_signature_index(), new_index);
      method->set_generic_signature_index(new_index);
    }

    // Update constant pool indices in the method's checked exception
    // table to use new constant indices as needed.
    int cext_length = method->checked_exceptions_length();
    if (cext_length > 0) {
      CheckedExceptionElement * cext_table =
        method->checked_exceptions_start();
      for (int j = 0; j < cext_length; j++) {
        int cur_index = cext_table[j].class_cp_index;
        int new_index = find_new_index(cur_index);
        if (new_index != 0) {
          log_trace(redefine, class, constantpool)("cext-class_cp_index change: %d to %d", cur_index, new_index);
          cext_table[j].class_cp_index = (u2)new_index;
        }
      } // end for each checked exception table entry
    } // end if there are checked exception table entries

    // Update each catch type index in the method's exception table
    // to use new constant pool indices as needed. The exception table
    // holds quadruple entries of the form:
    //   (beg_bci, end_bci, handler_bci, klass_index)

    ExceptionTable ex_table(method());
    int ext_length = ex_table.length();

    for (int j = 0; j < ext_length; j ++) {
      int cur_index = ex_table.catch_type_index(j);
      int new_index = find_new_index(cur_index);
      if (new_index != 0) {
        log_trace(redefine, class, constantpool)("ext-klass_index change: %d to %d", cur_index, new_index);
        ex_table.set_catch_type_index(j, new_index);
      }
    } // end for each exception table entry

    // Update constant pool indices in the method's local variable
    // table to use new constant indices as needed. The local variable
    // table hold sextuple entries of the form:
    // (start_pc, length, name_index, descriptor_index, signature_index, slot)
    int lvt_length = method->localvariable_table_length();
    if (lvt_length > 0) {
      LocalVariableTableElement * lv_table =
        method->localvariable_table_start();
      for (int j = 0; j < lvt_length; j++) {
        int cur_index = lv_table[j].name_cp_index;
        int new_index = find_new_index(cur_index);
        if (new_index != 0) {
          log_trace(redefine, class, constantpool)("lvt-name_cp_index change: %d to %d", cur_index, new_index);
          lv_table[j].name_cp_index = (u2)new_index;
        }
        cur_index = lv_table[j].descriptor_cp_index;
        new_index = find_new_index(cur_index);
        if (new_index != 0) {
          log_trace(redefine, class, constantpool)("lvt-descriptor_cp_index change: %d to %d", cur_index, new_index);
          lv_table[j].descriptor_cp_index = (u2)new_index;
        }
        cur_index = lv_table[j].signature_cp_index;
        new_index = find_new_index(cur_index);
        if (new_index != 0) {
          log_trace(redefine, class, constantpool)("lvt-signature_cp_index change: %d to %d", cur_index, new_index);
          lv_table[j].signature_cp_index = (u2)new_index;
        }
      } // end for each local variable table entry
    } // end if there are local variable table entries

    rewrite_cp_refs_in_stack_map_table(method);
  } // end for each method
} // end set_new_constant_pool()


// Unevolving classes may point to methods of the_class directly
// from their constant pool caches, itables, and/or vtables. We
// use the ClassLoaderDataGraph::classes_do() facility and this helper
// to fix up these pointers.  MethodData also points to old methods and
// must be cleaned.

// Adjust cpools and vtables closure
void VM_RedefineClasses::AdjustAndCleanMetadata::do_klass(Klass* k) {

  // This is a very busy routine. We don't want too much tracing
  // printed out.
  bool trace_name_printed = false;

  // If the class being redefined is java.lang.Object, we need to fix all
  // array class vtables also. The _has_redefined_Object flag is global.
  // Once the java.lang.Object has been redefined (by the current or one
  // of the previous VM_RedefineClasses operations) we have to always
  // adjust method entries for array classes.
  if (k->is_array_klass() && _has_redefined_Object) {
    k->vtable().adjust_method_entries(&trace_name_printed);

  } else if (k->is_instance_klass()) {
    HandleMark hm(_thread);
    InstanceKlass *ik = InstanceKlass::cast(k);

    // Clean MethodData of this class's methods so they don't refer to
    // old methods that are no longer running.
    Array<Method*>* methods = ik->methods();
    int num_methods = methods->length();
    for (int index = 0; index < num_methods; ++index) {
      if (methods->at(index)->method_data() != NULL) {
        methods->at(index)->method_data()->clean_weak_method_links();
      }
    }

    // Adjust all vtables, default methods and itables, to clean out old methods.
    ResourceMark rm(_thread);
    if (ik->vtable_length() > 0) {
      ik->vtable().adjust_method_entries(&trace_name_printed);
      ik->adjust_default_methods(&trace_name_printed);
    }

    if (ik->itable_length() > 0) {
      ik->itable().adjust_method_entries(&trace_name_printed);
    }

    // The constant pools in other classes (other_cp) can refer to
    // old methods.  We have to update method information in
    // other_cp's cache. If other_cp has a previous version, then we
    // have to repeat the process for each previous version. The
    // constant pool cache holds the Method*s for non-virtual
    // methods and for virtual, final methods.
    //
    // Special case: if the current class is being redefined by the current
    // VM_RedefineClasses operation, then new_cp has already been attached
    // to the_class and old_cp has already been added as a previous version.
    // The new_cp doesn't have any cached references to old methods so it
    // doesn't need to be updated and we could optimize by skipping it.
    // However, the current class can be marked as being redefined by another
    // VM_RedefineClasses operation which has already executed its doit_prologue
    // and needs cpcache method entries adjusted. For simplicity, the cpcache
    // update is done unconditionally. It should result in doing nothing for
    // classes being redefined by the current VM_RedefineClasses operation.
    // Method entries in the previous version(s) are adjusted as well.
    ConstantPoolCache* cp_cache;

    // this klass' constant pool cache may need adjustment
    ConstantPool* other_cp = ik->constants();
    cp_cache = other_cp->cache();
    if (cp_cache != NULL) {
      cp_cache->adjust_method_entries(&trace_name_printed);
    }

    // the previous versions' constant pool caches may need adjustment
    for (InstanceKlass* pv_node = ik->previous_versions();
         pv_node != NULL;
         pv_node = pv_node->previous_versions()) {
      cp_cache = pv_node->constants()->cache();
      if (cp_cache != NULL) {
        cp_cache->adjust_method_entries(&trace_name_printed);
      }
    }
  }
}

void VM_RedefineClasses::update_jmethod_ids() {
  for (int j = 0; j < _matching_methods_length; ++j) {
    Method* old_method = _matching_old_methods[j];
    jmethodID jmid = old_method->find_jmethod_id_or_null();
    if (jmid != NULL) {
      // There is a jmethodID, change it to point to the new method
      Method* new_method = _matching_new_methods[j];
      Method::change_method_associated_with_jmethod_id(jmid, new_method);
      assert(Method::resolve_jmethod_id(jmid) == _matching_new_methods[j],
             "should be replaced");
    }
  }
}

int VM_RedefineClasses::check_methods_and_mark_as_obsolete() {
  int emcp_method_count = 0;
  int obsolete_count = 0;
  int old_index = 0;
  for (int j = 0; j < _matching_methods_length; ++j, ++old_index) {
    Method* old_method = _matching_old_methods[j];
    Method* new_method = _matching_new_methods[j];
    Method* old_array_method;

    // Maintain an old_index into the _old_methods array by skipping
    // deleted methods
    while ((old_array_method = _old_methods->at(old_index)) != old_method) {
      ++old_index;
    }

    if (MethodComparator::methods_EMCP(old_method, new_method)) {
      // The EMCP definition from JSR-163 requires the bytecodes to be
      // the same with the exception of constant pool indices which may
      // differ. However, the constants referred to by those indices
      // must be the same.
      //
      // We use methods_EMCP() for comparison since constant pool
      // merging can remove duplicate constant pool entries that were
      // present in the old method and removed from the rewritten new
      // method. A faster binary comparison function would consider the
      // old and new methods to be different when they are actually
      // EMCP.
      //
      // The old and new methods are EMCP and you would think that we
      // could get rid of one of them here and now and save some space.
      // However, the concept of EMCP only considers the bytecodes and
      // the constant pool entries in the comparison. Other things,
      // e.g., the line number table (LNT) or the local variable table
      // (LVT) don't count in the comparison. So the new (and EMCP)
      // method can have a new LNT that we need so we can't just
      // overwrite the new method with the old method.
      //
      // When this routine is called, we have already attached the new
      // methods to the_class so the old methods are effectively
      // overwritten. However, if an old method is still executing,
      // then the old method cannot be collected until sometime after
      // the old method call has returned. So the overwriting of old
      // methods by new methods will save us space except for those
      // (hopefully few) old methods that are still executing.
      //
      // A method refers to a ConstMethod* and this presents another
      // possible avenue to space savings. The ConstMethod* in the
      // new method contains possibly new attributes (LNT, LVT, etc).
      // At first glance, it seems possible to save space by replacing
      // the ConstMethod* in the old method with the ConstMethod*
      // from the new method. The old and new methods would share the
      // same ConstMethod* and we would save the space occupied by
      // the old ConstMethod*. However, the ConstMethod* contains
      // a back reference to the containing method. Sharing the
      // ConstMethod* between two methods could lead to confusion in
      // the code that uses the back reference. This would lead to
      // brittle code that could be broken in non-obvious ways now or
      // in the future.
      //
      // Another possibility is to copy the ConstMethod* from the new
      // method to the old method and then overwrite the new method with
      // the old method. Since the ConstMethod* contains the bytecodes
      // for the method embedded in the oop, this option would change
      // the bytecodes out from under any threads executing the old
      // method and make the thread's bcp invalid. Since EMCP requires
      // that the bytecodes be the same modulo constant pool indices, it
      // is straight forward to compute the correct new bcp in the new
      // ConstMethod* from the old bcp in the old ConstMethod*. The
      // time consuming part would be searching all the frames in all
      // of the threads to find all of the calls to the old method.
      //
      // It looks like we will have to live with the limited savings
      // that we get from effectively overwriting the old methods
      // when the new methods are attached to the_class.

      // Count number of methods that are EMCP.  The method will be marked
      // old but not obsolete if it is EMCP.
      emcp_method_count++;

      // An EMCP method is _not_ obsolete. An obsolete method has a
      // different jmethodID than the current method. An EMCP method
      // has the same jmethodID as the current method. Having the
      // same jmethodID for all EMCP versions of a method allows for
      // a consistent view of the EMCP methods regardless of which
      // EMCP method you happen to have in hand. For example, a
      // breakpoint set in one EMCP method will work for all EMCP
      // versions of the method including the current one.
    } else {
      // mark obsolete methods as such
      old_method->set_is_obsolete();
      obsolete_count++;

      // obsolete methods need a unique idnum so they become new entries in
      // the jmethodID cache in InstanceKlass
      assert(old_method->method_idnum() == new_method->method_idnum(), "must match");
      u2 num = InstanceKlass::cast(_the_class)->next_method_idnum();
      if (num != ConstMethod::UNSET_IDNUM) {
        old_method->set_method_idnum(num);
      }

      // With tracing we try not to "yack" too much. The position of
      // this trace assumes there are fewer obsolete methods than
      // EMCP methods.
      if (log_is_enabled(Trace, redefine, class, obsolete, mark)) {
        ResourceMark rm;
        log_trace(redefine, class, obsolete, mark)
          ("mark %s(%s) as obsolete", old_method->name()->as_C_string(), old_method->signature()->as_C_string());
      }
    }
    old_method->set_is_old();
  }
  for (int i = 0; i < _deleted_methods_length; ++i) {
    Method* old_method = _deleted_methods[i];

    assert(!old_method->has_vtable_index(),
           "cannot delete methods with vtable entries");;

    // Mark all deleted methods as old, obsolete and deleted
    old_method->set_is_deleted();
    old_method->set_is_old();
    old_method->set_is_obsolete();
    ++obsolete_count;
    // With tracing we try not to "yack" too much. The position of
    // this trace assumes there are fewer obsolete methods than
    // EMCP methods.
    if (log_is_enabled(Trace, redefine, class, obsolete, mark)) {
      ResourceMark rm;
      log_trace(redefine, class, obsolete, mark)
        ("mark deleted %s(%s) as obsolete", old_method->name()->as_C_string(), old_method->signature()->as_C_string());
    }
  }
  assert((emcp_method_count + obsolete_count) == _old_methods->length(),
    "sanity check");
  log_trace(redefine, class, obsolete, mark)("EMCP_cnt=%d, obsolete_cnt=%d", emcp_method_count, obsolete_count);
  return emcp_method_count;
}

// This internal class transfers the native function registration from old methods
// to new methods.  It is designed to handle both the simple case of unchanged
// native methods and the complex cases of native method prefixes being added and/or
// removed.
// It expects only to be used during the VM_RedefineClasses op (a safepoint).
//
// This class is used after the new methods have been installed in "the_class".
//
// So, for example, the following must be handled.  Where 'm' is a method and
// a number followed by an underscore is a prefix.
//
//                                      Old Name    New Name
// Simple transfer to new method        m       ->  m
// Add prefix                           m       ->  1_m
// Remove prefix                        1_m     ->  m
// Simultaneous add of prefixes         m       ->  3_2_1_m
// Simultaneous removal of prefixes     3_2_1_m ->  m
// Simultaneous add and remove          1_m     ->  2_m
// Same, caused by prefix removal only  3_2_1_m ->  3_2_m
//
class TransferNativeFunctionRegistration {
 private:
  InstanceKlass* the_class;
  int prefix_count;
  char** prefixes;

  // Recursively search the binary tree of possibly prefixed method names.
  // Iteration could be used if all agents were well behaved. Full tree walk is
  // more resilent to agents not cleaning up intermediate methods.
  // Branch at each depth in the binary tree is:
  //    (1) without the prefix.
  //    (2) with the prefix.
  // where 'prefix' is the prefix at that 'depth' (first prefix, second prefix,...)
  Method* search_prefix_name_space(int depth, char* name_str, size_t name_len,
                                     Symbol* signature) {
    TempNewSymbol name_symbol = SymbolTable::probe(name_str, (int)name_len);
    if (name_symbol != NULL) {
      Method* method = the_class->lookup_method(name_symbol, signature);
      if (method != NULL) {
        // Even if prefixed, intermediate methods must exist.
        if (method->is_native()) {
          // Wahoo, we found a (possibly prefixed) version of the method, return it.
          return method;
        }
        if (depth < prefix_count) {
          // Try applying further prefixes (other than this one).
          method = search_prefix_name_space(depth+1, name_str, name_len, signature);
          if (method != NULL) {
            return method; // found
          }

          // Try adding this prefix to the method name and see if it matches
          // another method name.
          char* prefix = prefixes[depth];
          size_t prefix_len = strlen(prefix);
          size_t trial_len = name_len + prefix_len;
          char* trial_name_str = NEW_RESOURCE_ARRAY(char, trial_len + 1);
          strcpy(trial_name_str, prefix);
          strcat(trial_name_str, name_str);
          method = search_prefix_name_space(depth+1, trial_name_str, trial_len,
                                            signature);
          if (method != NULL) {
            // If found along this branch, it was prefixed, mark as such
            method->set_is_prefixed_native();
            return method; // found
          }
        }
      }
    }
    return NULL;  // This whole branch bore nothing
  }

  // Return the method name with old prefixes stripped away.
  char* method_name_without_prefixes(Method* method) {
    Symbol* name = method->name();
    char* name_str = name->as_utf8();

    // Old prefixing may be defunct, strip prefixes, if any.
    for (int i = prefix_count-1; i >= 0; i--) {
      char* prefix = prefixes[i];
      size_t prefix_len = strlen(prefix);
      if (strncmp(prefix, name_str, prefix_len) == 0) {
        name_str += prefix_len;
      }
    }
    return name_str;
  }

  // Strip any prefixes off the old native method, then try to find a
  // (possibly prefixed) new native that matches it.
  Method* strip_and_search_for_new_native(Method* method) {
    ResourceMark rm;
    char* name_str = method_name_without_prefixes(method);
    return search_prefix_name_space(0, name_str, strlen(name_str),
                                    method->signature());
  }

 public:

  // Construct a native method transfer processor for this class.
  TransferNativeFunctionRegistration(InstanceKlass* _the_class) {
    assert(SafepointSynchronize::is_at_safepoint(), "sanity check");

    the_class = _the_class;
    prefixes = JvmtiExport::get_all_native_method_prefixes(&prefix_count);
  }

  // Attempt to transfer any of the old or deleted methods that are native
  void transfer_registrations(Method** old_methods, int methods_length) {
    for (int j = 0; j < methods_length; j++) {
      Method* old_method = old_methods[j];

      if (old_method->is_native() && old_method->has_native_function()) {
        Method* new_method = strip_and_search_for_new_native(old_method);
        if (new_method != NULL) {
          // Actually set the native function in the new method.
          // Redefine does not send events (except CFLH), certainly not this
          // behind the scenes re-registration.
          new_method->set_native_function(old_method->native_function(),
                              !Method::native_bind_event_is_interesting);
        }
      }
    }
  }
};

// Don't lose the association between a native method and its JNI function.
void VM_RedefineClasses::transfer_old_native_function_registrations(InstanceKlass* the_class) {
  TransferNativeFunctionRegistration transfer(the_class);
  transfer.transfer_registrations(_deleted_methods, _deleted_methods_length);
  transfer.transfer_registrations(_matching_old_methods, _matching_methods_length);
}

// Deoptimize all compiled code that depends on the classes redefined.
//
// If the can_redefine_classes capability is obtained in the onload
// phase then the compiler has recorded all dependencies from startup.
// In that case we need only deoptimize and throw away all compiled code
// that depends on the class.
//
// If can_redefine_classes is obtained sometime after the onload
// phase then the dependency information may be incomplete. In that case
// the first call to RedefineClasses causes all compiled code to be
// thrown away. As can_redefine_classes has been obtained then
// all future compilations will record dependencies so second and
// subsequent calls to RedefineClasses need only throw away code
// that depends on the class.
//

void VM_RedefineClasses::flush_dependent_code() {
  assert(SafepointSynchronize::is_at_safepoint(), "sanity check");

  bool deopt_needed;

  // This is the first redefinition, mark all the nmethods for deoptimization
  if (!JvmtiExport::all_dependencies_are_recorded()) {
    log_debug(redefine, class, nmethod)("Marked all nmethods for deopt");
    CodeCache::mark_all_nmethods_for_evol_deoptimization();
    deopt_needed = true;
  } else {
    int deopt = CodeCache::mark_dependents_for_evol_deoptimization();
    log_debug(redefine, class, nmethod)("Marked %d dependent nmethods for deopt", deopt);
    deopt_needed = (deopt != 0);
  }

  if (deopt_needed) {
    CodeCache::flush_evol_dependents();
  }

  // From now on we know that the dependency information is complete
  JvmtiExport::set_all_dependencies_are_recorded(true);
}

void VM_RedefineClasses::compute_added_deleted_matching_methods() {
  Method* old_method;
  Method* new_method;

  _matching_old_methods = NEW_RESOURCE_ARRAY(Method*, _old_methods->length());
  _matching_new_methods = NEW_RESOURCE_ARRAY(Method*, _old_methods->length());
  _added_methods        = NEW_RESOURCE_ARRAY(Method*, _new_methods->length());
  _deleted_methods      = NEW_RESOURCE_ARRAY(Method*, _old_methods->length());

  _matching_methods_length = 0;
  _deleted_methods_length  = 0;
  _added_methods_length    = 0;

  int nj = 0;
  int oj = 0;
  while (true) {
    if (oj >= _old_methods->length()) {
      if (nj >= _new_methods->length()) {
        break; // we've looked at everything, done
      }
      // New method at the end
      new_method = _new_methods->at(nj);
      _added_methods[_added_methods_length++] = new_method;
      ++nj;
    } else if (nj >= _new_methods->length()) {
      // Old method, at the end, is deleted
      old_method = _old_methods->at(oj);
      _deleted_methods[_deleted_methods_length++] = old_method;
      ++oj;
    } else {
      old_method = _old_methods->at(oj);
      new_method = _new_methods->at(nj);
      if (old_method->name() == new_method->name()) {
        if (old_method->signature() == new_method->signature()) {
          _matching_old_methods[_matching_methods_length  ] = old_method;
          _matching_new_methods[_matching_methods_length++] = new_method;
          ++nj;
          ++oj;
        } else {
          // added overloaded have already been moved to the end,
          // so this is a deleted overloaded method
          _deleted_methods[_deleted_methods_length++] = old_method;
          ++oj;
        }
      } else { // names don't match
        if (old_method->name()->fast_compare(new_method->name()) > 0) {
          // new method
          _added_methods[_added_methods_length++] = new_method;
          ++nj;
        } else {
          // deleted method
          _deleted_methods[_deleted_methods_length++] = old_method;
          ++oj;
        }
      }
    }
  }
  assert(_matching_methods_length + _deleted_methods_length == _old_methods->length(), "sanity");
  assert(_matching_methods_length + _added_methods_length == _new_methods->length(), "sanity");
}


void VM_RedefineClasses::swap_annotations(InstanceKlass* the_class,
                                          InstanceKlass* scratch_class) {
  // Swap annotation fields values
  Annotations* old_annotations = the_class->annotations();
  the_class->set_annotations(scratch_class->annotations());
  scratch_class->set_annotations(old_annotations);
}


// Install the redefinition of a class:
//    - house keeping (flushing breakpoints and caches, deoptimizing
//      dependent compiled code)
//    - replacing parts in the_class with parts from scratch_class
//    - adding a weak reference to track the obsolete but interesting
//      parts of the_class
//    - adjusting constant pool caches and vtables in other classes
//      that refer to methods in the_class. These adjustments use the
//      ClassLoaderDataGraph::classes_do() facility which only allows
//      a helper method to be specified. The interesting parameters
//      that we would like to pass to the helper method are saved in
//      static global fields in the VM operation.
void VM_RedefineClasses::redefine_single_class(Thread* current, jclass the_jclass,
                                               InstanceKlass* scratch_class) {

  HandleMark hm(current);   // make sure handles from this call are freed

  if (log_is_enabled(Info, redefine, class, timer)) {
    _timer_rsc_phase1.start();
  }

  InstanceKlass* the_class = get_ik(the_jclass);

  // Set a flag to control and optimize adjusting method entries
  _has_redefined_Object |= the_class == vmClasses::Object_klass();

  // Remove all breakpoints in methods of this class
  JvmtiBreakpoints& jvmti_breakpoints = JvmtiCurrentBreakpoints::get_jvmti_breakpoints();
  jvmti_breakpoints.clearall_in_class_at_safepoint(the_class);

  _old_methods = the_class->methods();
  _new_methods = scratch_class->methods();
  _the_class = the_class;
  compute_added_deleted_matching_methods();
  update_jmethod_ids();

  _any_class_has_resolved_methods = the_class->has_resolved_methods() || _any_class_has_resolved_methods;

  // Attach new constant pool to the original klass. The original
  // klass still refers to the old constant pool (for now).
  scratch_class->constants()->set_pool_holder(the_class);

#if 0
  // In theory, with constant pool merging in place we should be able
  // to save space by using the new, merged constant pool in place of
  // the old constant pool(s). By "pool(s)" I mean the constant pool in
  // the klass version we are replacing now and any constant pool(s) in
  // previous versions of klass. Nice theory, doesn't work in practice.
  // When this code is enabled, even simple programs throw NullPointer
  // exceptions. I'm guessing that this is caused by some constant pool
  // cache difference between the new, merged constant pool and the
  // constant pool that was just being used by the klass. I'm keeping
  // this code around to archive the idea, but the code has to remain
  // disabled for now.

  // Attach each old method to the new constant pool. This can be
  // done here since we are past the bytecode verification and
  // constant pool optimization phases.
  for (int i = _old_methods->length() - 1; i >= 0; i--) {
    Method* method = _old_methods->at(i);
    method->set_constants(scratch_class->constants());
  }

  // NOTE: this doesn't work because you can redefine the same class in two
  // threads, each getting their own constant pool data appended to the
  // original constant pool.  In order for the new methods to work when they
  // become old methods, they need to keep their updated copy of the constant pool.

  {
    // walk all previous versions of the klass
    InstanceKlass *ik = the_class;
    PreviousVersionWalker pvw(ik);
    do {
      ik = pvw.next_previous_version();
      if (ik != NULL) {

        // attach previous version of klass to the new constant pool
        ik->set_constants(scratch_class->constants());

        // Attach each method in the previous version of klass to the
        // new constant pool
        Array<Method*>* prev_methods = ik->methods();
        for (int i = prev_methods->length() - 1; i >= 0; i--) {
          Method* method = prev_methods->at(i);
          method->set_constants(scratch_class->constants());
        }
      }
    } while (ik != NULL);
  }
#endif

  // Replace methods and constantpool
  the_class->set_methods(_new_methods);
  scratch_class->set_methods(_old_methods);     // To prevent potential GCing of the old methods,
                                          // and to be able to undo operation easily.

  Array<int>* old_ordering = the_class->method_ordering();
  the_class->set_method_ordering(scratch_class->method_ordering());
  scratch_class->set_method_ordering(old_ordering);

  ConstantPool* old_constants = the_class->constants();
  the_class->set_constants(scratch_class->constants());
  scratch_class->set_constants(old_constants);  // See the previous comment.
#if 0
  // We are swapping the guts of "the new class" with the guts of "the
  // class". Since the old constant pool has just been attached to "the
  // new class", it seems logical to set the pool holder in the old
  // constant pool also. However, doing this will change the observable
  // class hierarchy for any old methods that are still executing. A
  // method can query the identity of its "holder" and this query uses
  // the method's constant pool link to find the holder. The change in
  // holding class from "the class" to "the new class" can confuse
  // things.
  //
  // Setting the old constant pool's holder will also cause
  // verification done during vtable initialization below to fail.
  // During vtable initialization, the vtable's class is verified to be
  // a subtype of the method's holder. The vtable's class is "the
  // class" and the method's holder is gotten from the constant pool
  // link in the method itself. For "the class"'s directly implemented
  // methods, the method holder is "the class" itself (as gotten from
  // the new constant pool). The check works fine in this case. The
  // check also works fine for methods inherited from super classes.
  //
  // Miranda methods are a little more complicated. A miranda method is
  // provided by an interface when the class implementing the interface
  // does not provide its own method.  These interfaces are implemented
  // internally as an InstanceKlass. These special instanceKlasses
  // share the constant pool of the class that "implements" the
  // interface. By sharing the constant pool, the method holder of a
  // miranda method is the class that "implements" the interface. In a
  // non-redefine situation, the subtype check works fine. However, if
  // the old constant pool's pool holder is modified, then the check
  // fails because there is no class hierarchy relationship between the
  // vtable's class and "the new class".

  old_constants->set_pool_holder(scratch_class());
#endif

  // track number of methods that are EMCP for add_previous_version() call below
  int emcp_method_count = check_methods_and_mark_as_obsolete();
  transfer_old_native_function_registrations(the_class);

  // The class file bytes from before any retransformable agents mucked
  // with them was cached on the scratch class, move to the_class.
  // Note: we still want to do this if nothing needed caching since it
  // should get cleared in the_class too.
  if (the_class->get_cached_class_file() == 0) {
    // the_class doesn't have a cache yet so copy it
    the_class->set_cached_class_file(scratch_class->get_cached_class_file());
  }
  else if (scratch_class->get_cached_class_file() !=
           the_class->get_cached_class_file()) {
    // The same class can be present twice in the scratch classes list or there
    // are multiple concurrent RetransformClasses calls on different threads.
    // In such cases we have to deallocate scratch_class cached_class_file.
    os::free(scratch_class->get_cached_class_file());
  }

  // NULL out in scratch class to not delete twice.  The class to be redefined
  // always owns these bytes.
  scratch_class->set_cached_class_file(NULL);

  // Replace inner_classes
  Array<u2>* old_inner_classes = the_class->inner_classes();
  the_class->set_inner_classes(scratch_class->inner_classes());
  scratch_class->set_inner_classes(old_inner_classes);

  // Initialize the vtable and interface table after
  // methods have been rewritten
  // no exception should happen here since we explicitly
  // do not check loader constraints.
  // compare_and_normalize_class_versions has already checked:
  //  - classloaders unchanged, signatures unchanged
  //  - all instanceKlasses for redefined classes reused & contents updated
  the_class->vtable().initialize_vtable();
  the_class->itable().initialize_itable();

  // Leave arrays of jmethodIDs and itable index cache unchanged

  // Copy the "source file name" attribute from new class version
  the_class->set_source_file_name_index(
    scratch_class->source_file_name_index());

  // Copy the "source debug extension" attribute from new class version
  the_class->set_source_debug_extension(
    scratch_class->source_debug_extension(),
    scratch_class->source_debug_extension() == NULL ? 0 :
    (int)strlen(scratch_class->source_debug_extension()));

  // Use of javac -g could be different in the old and the new
  if (scratch_class->access_flags().has_localvariable_table() !=
      the_class->access_flags().has_localvariable_table()) {

    AccessFlags flags = the_class->access_flags();
    if (scratch_class->access_flags().has_localvariable_table()) {
      flags.set_has_localvariable_table();
    } else {
      flags.clear_has_localvariable_table();
    }
    the_class->set_access_flags(flags);
  }

  swap_annotations(the_class, scratch_class);

  // Replace minor version number of class file
  u2 old_minor_version = the_class->constants()->minor_version();
  the_class->constants()->set_minor_version(scratch_class->constants()->minor_version());
  scratch_class->constants()->set_minor_version(old_minor_version);

  // Replace major version number of class file
  u2 old_major_version = the_class->constants()->major_version();
  the_class->constants()->set_major_version(scratch_class->constants()->major_version());
  scratch_class->constants()->set_major_version(old_major_version);

  // Replace CP indexes for class and name+type of enclosing method
  u2 old_class_idx  = the_class->enclosing_method_class_index();
  u2 old_method_idx = the_class->enclosing_method_method_index();
  the_class->set_enclosing_method_indices(
    scratch_class->enclosing_method_class_index(),
    scratch_class->enclosing_method_method_index());
  scratch_class->set_enclosing_method_indices(old_class_idx, old_method_idx);

  the_class->set_has_been_redefined();

  // keep track of previous versions of this class
  the_class->add_previous_version(scratch_class, emcp_method_count);

  _timer_rsc_phase1.stop();
  if (log_is_enabled(Info, redefine, class, timer)) {
    _timer_rsc_phase2.start();
  }

  if (the_class->oop_map_cache() != NULL) {
    // Flush references to any obsolete methods from the oop map cache
    // so that obsolete methods are not pinned.
    the_class->oop_map_cache()->flush_obsolete_entries();
  }

  increment_class_counter(the_class);

  if (EventClassRedefinition::is_enabled()) {
    EventClassRedefinition event;
    event.set_classModificationCount(java_lang_Class::classRedefinedCount(the_class->java_mirror()));
    event.set_redefinedClass(the_class);
    event.set_redefinitionId(_id);
    event.commit();
  }

  {
    ResourceMark rm(current);
    // increment the classRedefinedCount field in the_class and in any
    // direct and indirect subclasses of the_class
    log_info(redefine, class, load)
      ("redefined name=%s, count=%d (avail_mem=" UINT64_FORMAT "K)",
       the_class->external_name(), java_lang_Class::classRedefinedCount(the_class->java_mirror()), os::available_memory() >> 10);
    Events::log_redefinition(current, "redefined class name=%s, count=%d",
                             the_class->external_name(),
                             java_lang_Class::classRedefinedCount(the_class->java_mirror()));

  }
  _timer_rsc_phase2.stop();

} // end redefine_single_class()


// Increment the classRedefinedCount field in the specific InstanceKlass
// and in all direct and indirect subclasses.
void VM_RedefineClasses::increment_class_counter(InstanceKlass* ik) {
  for (ClassHierarchyIterator iter(ik); !iter.done(); iter.next()) {
    // Only update instanceKlasses
    Klass* sub = iter.klass();
    if (sub->is_instance_klass()) {
      oop class_mirror = InstanceKlass::cast(sub)->java_mirror();
      Klass* class_oop = java_lang_Class::as_Klass(class_mirror);
      int new_count = java_lang_Class::classRedefinedCount(class_mirror) + 1;
      java_lang_Class::set_classRedefinedCount(class_mirror, new_count);

      if (class_oop != _the_class) {
        // _the_class count is printed at end of redefine_single_class()
        log_debug(redefine, class, subclass)("updated count in subclass=%s to %d", ik->external_name(), new_count);
      }
    }
  }
}

void VM_RedefineClasses::CheckClass::do_klass(Klass* k) {
  bool no_old_methods = true;  // be optimistic

  // Both array and instance classes have vtables.
  // a vtable should never contain old or obsolete methods
  ResourceMark rm(_thread);
  if (k->vtable_length() > 0 &&
      !k->vtable().check_no_old_or_obsolete_entries()) {
    if (log_is_enabled(Trace, redefine, class, obsolete, metadata)) {
      log_trace(redefine, class, obsolete, metadata)
        ("klassVtable::check_no_old_or_obsolete_entries failure -- OLD or OBSOLETE method found -- class: %s",
         k->signature_name());
      k->vtable().dump_vtable();
    }
    no_old_methods = false;
  }

  if (k->is_instance_klass()) {
    HandleMark hm(_thread);
    InstanceKlass *ik = InstanceKlass::cast(k);

    // an itable should never contain old or obsolete methods
    if (ik->itable_length() > 0 &&
        !ik->itable().check_no_old_or_obsolete_entries()) {
      if (log_is_enabled(Trace, redefine, class, obsolete, metadata)) {
        log_trace(redefine, class, obsolete, metadata)
          ("klassItable::check_no_old_or_obsolete_entries failure -- OLD or OBSOLETE method found -- class: %s",
           ik->signature_name());
        ik->itable().dump_itable();
      }
      no_old_methods = false;
    }

    // the constant pool cache should never contain non-deleted old or obsolete methods
    if (ik->constants() != NULL &&
        ik->constants()->cache() != NULL &&
        !ik->constants()->cache()->check_no_old_or_obsolete_entries()) {
      if (log_is_enabled(Trace, redefine, class, obsolete, metadata)) {
        log_trace(redefine, class, obsolete, metadata)
          ("cp-cache::check_no_old_or_obsolete_entries failure -- OLD or OBSOLETE method found -- class: %s",
           ik->signature_name());
        ik->constants()->cache()->dump_cache();
      }
      no_old_methods = false;
    }
  }

  // print and fail guarantee if old methods are found.
  if (!no_old_methods) {
    if (log_is_enabled(Trace, redefine, class, obsolete, metadata)) {
      dump_methods();
    } else {
      log_trace(redefine, class)("Use the '-Xlog:redefine+class*:' option "
        "to see more info about the following guarantee() failure.");
    }
    guarantee(false, "OLD and/or OBSOLETE method(s) found");
  }
}

u8 VM_RedefineClasses::next_id() {
  while (true) {
    u8 id = _id_counter;
    u8 next_id = id + 1;
    u8 result = Atomic::cmpxchg(&_id_counter, id, next_id);
    if (result == id) {
      return next_id;
    }
  }
}

void VM_RedefineClasses::dump_methods() {
  int j;
  log_trace(redefine, class, dump)("_old_methods --");
  for (j = 0; j < _old_methods->length(); ++j) {
    LogStreamHandle(Trace, redefine, class, dump) log_stream;
    Method* m = _old_methods->at(j);
    log_stream.print("%4d  (%5d)  ", j, m->vtable_index());
    m->access_flags().print_on(&log_stream);
    log_stream.print(" --  ");
    m->print_name(&log_stream);
    log_stream.cr();
  }
  log_trace(redefine, class, dump)("_new_methods --");
  for (j = 0; j < _new_methods->length(); ++j) {
    LogStreamHandle(Trace, redefine, class, dump) log_stream;
    Method* m = _new_methods->at(j);
    log_stream.print("%4d  (%5d)  ", j, m->vtable_index());
    m->access_flags().print_on(&log_stream);
    log_stream.print(" --  ");
    m->print_name(&log_stream);
    log_stream.cr();
  }
  log_trace(redefine, class, dump)("_matching_methods --");
  for (j = 0; j < _matching_methods_length; ++j) {
    LogStreamHandle(Trace, redefine, class, dump) log_stream;
    Method* m = _matching_old_methods[j];
    log_stream.print("%4d  (%5d)  ", j, m->vtable_index());
    m->access_flags().print_on(&log_stream);
    log_stream.print(" --  ");
    m->print_name();
    log_stream.cr();

    m = _matching_new_methods[j];
    log_stream.print("      (%5d)  ", m->vtable_index());
    m->access_flags().print_on(&log_stream);
    log_stream.cr();
  }
  log_trace(redefine, class, dump)("_deleted_methods --");
  for (j = 0; j < _deleted_methods_length; ++j) {
    LogStreamHandle(Trace, redefine, class, dump) log_stream;
    Method* m = _deleted_methods[j];
    log_stream.print("%4d  (%5d)  ", j, m->vtable_index());
    m->access_flags().print_on(&log_stream);
    log_stream.print(" --  ");
    m->print_name(&log_stream);
    log_stream.cr();
  }
  log_trace(redefine, class, dump)("_added_methods --");
  for (j = 0; j < _added_methods_length; ++j) {
    LogStreamHandle(Trace, redefine, class, dump) log_stream;
    Method* m = _added_methods[j];
    log_stream.print("%4d  (%5d)  ", j, m->vtable_index());
    m->access_flags().print_on(&log_stream);
    log_stream.print(" --  ");
    m->print_name(&log_stream);
    log_stream.cr();
  }
}

void VM_RedefineClasses::print_on_error(outputStream* st) const {
  VM_Operation::print_on_error(st);
  if (_the_class != NULL) {
    ResourceMark rm;
    st->print_cr(", redefining class %s", _the_class->external_name());
  }
}
