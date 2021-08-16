/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciField.hpp"
#include "ci/ciInstanceKlass.hpp"
#include "ci/ciSymbols.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/vmClasses.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "interpreter/linkResolver.hpp"
#include "oops/klass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/reflectionUtils.hpp"

// ciField
//
// This class represents the result of a field lookup in the VM.
// The lookup may not succeed, in which case the information in
// the ciField will be incomplete.

// The ciObjectFactory cannot create circular data structures in one query.
// To avoid vicious circularities, we initialize ciField::_type to NULL
// for reference types and derive it lazily from the ciField::_signature.
// Primitive types are eagerly initialized, and basic layout queries
// can succeed without initialization, using only the BasicType of the field.

// Notes on bootstrapping and shared CI objects:  A field is shared if and
// only if it is (a) non-static and (b) declared by a shared instance klass.
// This allows non-static field lists to be cached on shared types.
// Because the _type field is lazily initialized, however, there is a
// special restriction that a shared field cannot cache an unshared type.
// This puts a small performance penalty on shared fields with unshared
// types, such as StackTraceElement[] Throwable.stackTrace.
// (Throwable is shared because ClassCastException is shared, but
// StackTraceElement is not presently shared.)

// It is not a vicious circularity for a ciField to recursively create
// the ciSymbols necessary to represent its name and signature.
// Therefore, these items are created eagerly, and the name and signature
// of a shared field are themselves shared symbols.  This somewhat
// pollutes the set of shared CI objects:  It grows from 50 to 93 items,
// with all of the additional 43 being uninteresting shared ciSymbols.
// This adds at most one step to the binary search, an amount which
// decreases for complex compilation tasks.

// ------------------------------------------------------------------
// ciField::ciField
ciField::ciField(ciInstanceKlass* klass, int index) :
    _known_to_link_with_put(NULL), _known_to_link_with_get(NULL) {
  ASSERT_IN_VM;
  CompilerThread *THREAD = CompilerThread::current();

  assert(ciObjectFactory::is_initialized(), "not a shared field");

  assert(klass->get_instanceKlass()->is_linked(), "must be linked before using its constant-pool");

  constantPoolHandle cpool(THREAD, klass->get_instanceKlass()->constants());

  // Get the field's name, signature, and type.
  Symbol* name  = cpool->name_ref_at(index);
  _name = ciEnv::current(THREAD)->get_symbol(name);

  int nt_index = cpool->name_and_type_ref_index_at(index);
  int sig_index = cpool->signature_ref_index_at(nt_index);
  Symbol* signature = cpool->symbol_at(sig_index);
  _signature = ciEnv::current(THREAD)->get_symbol(signature);

  BasicType field_type = Signature::basic_type(signature);

  // If the field is a pointer type, get the klass of the
  // field.
  if (is_reference_type(field_type)) {
    bool ignore;
    // This is not really a class reference; the index always refers to the
    // field's type signature, as a symbol.  Linkage checks do not apply.
    _type = ciEnv::current(THREAD)->get_klass_by_index(cpool, sig_index, ignore, klass);
  } else {
    _type = ciType::make(field_type);
  }

  _name = (ciSymbol*)ciEnv::current(THREAD)->get_symbol(name);

  // Get the field's declared holder.
  //
  // Note: we actually create a ciInstanceKlass for this klass,
  // even though we may not need to.
  int holder_index = cpool->klass_ref_index_at(index);
  bool holder_is_accessible;

  ciKlass* generic_declared_holder = ciEnv::current(THREAD)->get_klass_by_index(cpool, holder_index,
                                                                                holder_is_accessible,
                                                                                klass);

  if (generic_declared_holder->is_array_klass()) {
    // If the declared holder of the field is an array class, assume that
    // the canonical holder of that field is java.lang.Object. Arrays
    // do not have fields; java.lang.Object is the only supertype of an
    // array type that can declare fields and is therefore the canonical
    // holder of the array type.
    //
    // Furthermore, the compilers assume that java.lang.Object does not
    // have any fields. Therefore, the field is not looked up. Instead,
    // the method returns partial information that will trigger special
    // handling in ciField::will_link and will result in a
    // java.lang.NoSuchFieldError exception being thrown by the compiled
    // code (the expected behavior in this case).
    _holder = ciEnv::current(THREAD)->Object_klass();
    _offset = -1;
    _is_constant = false;
    return;
  }

  ciInstanceKlass* declared_holder = generic_declared_holder->as_instance_klass();

  // The declared holder of this field may not have been loaded.
  // Bail out with partial field information.
  if (!holder_is_accessible) {
    // _type has already been set.
    // The default values for _flags and _constant_value will suffice.
    // We need values for _holder, _offset,  and _is_constant,
    _holder = declared_holder;
    _offset = -1;
    _is_constant = false;
    return;
  }

  InstanceKlass* loaded_decl_holder = declared_holder->get_instanceKlass();

  // Perform the field lookup.
  fieldDescriptor field_desc;
  Klass* canonical_holder =
    loaded_decl_holder->find_field(name, signature, &field_desc);
  if (canonical_holder == NULL) {
    // Field lookup failed.  Will be detected by will_link.
    _holder = declared_holder;
    _offset = -1;
    _is_constant = false;
    return;
  }

  // Access check based on declared_holder. canonical_holder should not be used
  // to check access because it can erroneously succeed. If this check fails,
  // propagate the declared holder to will_link() which in turn will bail out
  // compilation for this field access.
  bool can_access = Reflection::verify_member_access(klass->get_Klass(),
                                                     declared_holder->get_Klass(),
                                                     canonical_holder,
                                                     field_desc.access_flags(),
                                                     true, false, THREAD);
  if (!can_access) {
    _holder = declared_holder;
    _offset = -1;
    _is_constant = false;
    // It's possible the access check failed due to a nestmate access check
    // encountering an exception. We can't propagate the exception from here
    // so we have to clear it. If the access check happens again in a different
    // context then the exception will be thrown there.
    if (HAS_PENDING_EXCEPTION) {
      CLEAR_PENDING_EXCEPTION;
    }
    return;
  }

  assert(canonical_holder == field_desc.field_holder(), "just checking");
  initialize_from(&field_desc);
}

ciField::ciField(fieldDescriptor *fd) :
    _known_to_link_with_put(NULL), _known_to_link_with_get(NULL) {
  ASSERT_IN_VM;

  // Get the field's name, signature, and type.
  ciEnv* env = CURRENT_ENV;
  _name = env->get_symbol(fd->name());
  _signature = env->get_symbol(fd->signature());

  BasicType field_type = fd->field_type();

  // If the field is a pointer type, get the klass of the
  // field.
  if (is_reference_type(field_type)) {
    _type = NULL;  // must call compute_type on first access
  } else {
    _type = ciType::make(field_type);
  }

  initialize_from(fd);

  // Either (a) it is marked shared, or else (b) we are done bootstrapping.
  assert(is_shared() || ciObjectFactory::is_initialized(),
         "bootstrap classes must not create & cache unshared fields");
}

static bool trust_final_non_static_fields(ciInstanceKlass* holder) {
  if (holder == NULL)
    return false;
  if (holder->name() == ciSymbols::java_lang_System())
    // Never trust strangely unstable finals:  System.out, etc.
    return false;
  // Even if general trusting is disabled, trust system-built closures in these packages.
  if (holder->is_in_package("java/lang/invoke") || holder->is_in_package("sun/invoke") ||
      holder->is_in_package("jdk/internal/foreign") || holder->is_in_package("jdk/incubator/foreign") ||
      holder->is_in_package("jdk/internal/vm/vector") || holder->is_in_package("jdk/incubator/vector") ||
      holder->is_in_package("java/lang"))
    return true;
  // Trust hidden classes. They are created via Lookup.defineHiddenClass and
  // can't be serialized, so there is no hacking of finals going on with them.
  if (holder->is_hidden())
    return true;
  // Trust final fields in all boxed classes
  if (holder->is_box_klass())
    return true;
  // Trust final fields in records
  if (holder->is_record())
    return true;
  // Trust final fields in String
  if (holder->name() == ciSymbols::java_lang_String())
    return true;
  // Trust Atomic*FieldUpdaters: they are very important for performance, and make up one
  // more reason not to use Unsafe, if their final fields are trusted. See more in JDK-8140483.
  if (holder->name() == ciSymbols::java_util_concurrent_atomic_AtomicIntegerFieldUpdater_Impl() ||
      holder->name() == ciSymbols::java_util_concurrent_atomic_AtomicLongFieldUpdater_CASUpdater() ||
      holder->name() == ciSymbols::java_util_concurrent_atomic_AtomicLongFieldUpdater_LockedUpdater() ||
      holder->name() == ciSymbols::java_util_concurrent_atomic_AtomicReferenceFieldUpdater_Impl()) {
    return true;
  }
  return TrustFinalNonStaticFields;
}

void ciField::initialize_from(fieldDescriptor* fd) {
  // Get the flags, offset, and canonical holder of the field.
  _flags = ciFlags(fd->access_flags());
  _offset = fd->offset();
  Klass* field_holder = fd->field_holder();
  assert(field_holder != NULL, "null field_holder");
  _holder = CURRENT_ENV->get_instance_klass(field_holder);

  // Check to see if the field is constant.
  Klass* k = _holder->get_Klass();
  bool is_stable_field = FoldStableValues && is_stable();
  if ((is_final() && !has_initialized_final_update()) || is_stable_field) {
    if (is_static()) {
      // This field just may be constant.  The only case where it will
      // not be constant is when the field is a *special* static & final field
      // whose value may change.  The three examples are java.lang.System.in,
      // java.lang.System.out, and java.lang.System.err.
      assert(vmClasses::System_klass() != NULL, "Check once per vm");
      if (k == vmClasses::System_klass()) {
        // Check offsets for case 2: System.in, System.out, or System.err
        if (_offset == java_lang_System::in_offset()  ||
            _offset == java_lang_System::out_offset() ||
            _offset == java_lang_System::err_offset()) {
          _is_constant = false;
          return;
        }
      }
      _is_constant = true;
    } else {
      // An instance field can be constant if it's a final static field or if
      // it's a final non-static field of a trusted class (classes in
      // java.lang.invoke and sun.invoke packages and subpackages).
      _is_constant = is_stable_field || trust_final_non_static_fields(_holder);
    }
  } else {
    // For CallSite objects treat the target field as a compile time constant.
    assert(vmClasses::CallSite_klass() != NULL, "should be already initialized");
    if (k == vmClasses::CallSite_klass() &&
        _offset == java_lang_invoke_CallSite::target_offset()) {
      assert(!has_initialized_final_update(), "CallSite is not supposed to have writes to final fields outside initializers");
      _is_constant = true;
    } else {
      // Non-final & non-stable fields are not constants.
      _is_constant = false;
    }
  }
}

// ------------------------------------------------------------------
// ciField::constant_value
// Get the constant value of a this static field.
ciConstant ciField::constant_value() {
  assert(is_static() && is_constant(), "illegal call to constant_value()");
  if (!_holder->is_initialized()) {
    return ciConstant(); // Not initialized yet
  }
  if (_constant_value.basic_type() == T_ILLEGAL) {
    // Static fields are placed in mirror objects.
    VM_ENTRY_MARK;
    ciInstance* mirror = CURRENT_ENV->get_instance(_holder->get_Klass()->java_mirror());
    _constant_value = mirror->field_value_impl(type()->basic_type(), offset());
  }
  if (FoldStableValues && is_stable() && _constant_value.is_null_or_zero()) {
    return ciConstant();
  }
  return _constant_value;
}

// ------------------------------------------------------------------
// ciField::constant_value_of
// Get the constant value of non-static final field in the given object.
ciConstant ciField::constant_value_of(ciObject* object) {
  assert(!is_static() && is_constant(), "only if field is non-static constant");
  assert(object->is_instance(), "must be instance");
  ciConstant field_value = object->as_instance()->field_value(this);
  if (FoldStableValues && is_stable() && field_value.is_null_or_zero()) {
    return ciConstant();
  }
  return field_value;
}

// ------------------------------------------------------------------
// ciField::compute_type
//
// Lazily compute the type, if it is an instance klass.
ciType* ciField::compute_type() {
  GUARDED_VM_ENTRY(return compute_type_impl();)
}

ciType* ciField::compute_type_impl() {
  ciKlass* type = CURRENT_ENV->get_klass_by_name_impl(_holder, constantPoolHandle(), _signature, false);
  if (!type->is_primitive_type() && is_shared()) {
    // We must not cache a pointer to an unshared type, in a shared field.
    bool type_is_also_shared = false;
    if (type->is_type_array_klass()) {
      type_is_also_shared = true;  // int[] etc. are explicitly bootstrapped
    } else if (type->is_instance_klass()) {
      type_is_also_shared = type->as_instance_klass()->is_shared();
    } else {
      // Currently there is no 'shared' query for array types.
      type_is_also_shared = !ciObjectFactory::is_initialized();
    }
    if (!type_is_also_shared)
      return type;              // Bummer.
  }
  _type = type;
  return type;
}


// ------------------------------------------------------------------
// ciField::will_link
//
// Can a specific access to this field be made without causing
// link errors?
bool ciField::will_link(ciMethod* accessing_method,
                        Bytecodes::Code bc) {
  VM_ENTRY_MARK;
  assert(bc == Bytecodes::_getstatic || bc == Bytecodes::_putstatic ||
         bc == Bytecodes::_getfield  || bc == Bytecodes::_putfield,
         "unexpected bytecode");

  if (_offset == -1) {
    // at creation we couldn't link to our holder so we need to
    // maintain that stance, otherwise there's no safe way to use this
    // ciField.
    return false;
  }

  // Check for static/nonstatic mismatch
  bool is_static = (bc == Bytecodes::_getstatic || bc == Bytecodes::_putstatic);
  if (is_static != this->is_static()) {
    return false;
  }

  // Get and put can have different accessibility rules
  bool is_put    = (bc == Bytecodes::_putfield  || bc == Bytecodes::_putstatic);
  if (is_put) {
    if (_known_to_link_with_put == accessing_method) {
      return true;
    }
  } else {
    if (_known_to_link_with_get == accessing_method->holder()) {
      return true;
    }
  }

  LinkInfo link_info(_holder->get_instanceKlass(),
                     _name->get_symbol(), _signature->get_symbol(),
                     methodHandle(THREAD, accessing_method->get_Method()));
  fieldDescriptor result;
  LinkResolver::resolve_field(result, link_info, bc, false, CHECK_AND_CLEAR_(false));

  // update the hit-cache, unless there is a problem with memory scoping:
  if (accessing_method->holder()->is_shared() || !is_shared()) {
    if (is_put) {
      _known_to_link_with_put = accessing_method;
    } else {
      _known_to_link_with_get = accessing_method->holder();
    }
  }

  return true;
}

bool ciField::is_call_site_target() {
  ciInstanceKlass* callsite_klass = CURRENT_ENV->CallSite_klass();
  if (callsite_klass == NULL)
    return false;
  return (holder()->is_subclass_of(callsite_klass) && (name() == ciSymbols::target_name()));
}

bool ciField::is_autobox_cache() {
  ciSymbol* klass_name = holder()->name();
  return (name() == ciSymbols::cache_field_name() &&
          holder()->uses_default_loader() &&
          (klass_name == ciSymbols::java_lang_Character_CharacterCache() ||
            klass_name == ciSymbols::java_lang_Byte_ByteCache() ||
            klass_name == ciSymbols::java_lang_Short_ShortCache() ||
            klass_name == ciSymbols::java_lang_Integer_IntegerCache() ||
            klass_name == ciSymbols::java_lang_Long_LongCache()));
}

// ------------------------------------------------------------------
// ciField::print
void ciField::print() {
  tty->print("<ciField name=");
  _holder->print_name();
  tty->print(".");
  _name->print_symbol();
  tty->print(" signature=");
  _signature->print_symbol();
  tty->print(" offset=%d type=", _offset);
  if (_type != NULL)
    _type->print_name();
  else
    tty->print("(reference)");
  tty->print(" flags=%04x", flags().as_int());
  tty->print(" is_constant=%s", bool_to_str(_is_constant));
  if (_is_constant && is_static()) {
    tty->print(" constant_value=");
    _constant_value.print();
  }
  tty->print(">");
}

// ------------------------------------------------------------------
// ciField::print_name_on
//
// Print the name of this field
void ciField::print_name_on(outputStream* st) {
  name()->print_symbol_on(st);
}
