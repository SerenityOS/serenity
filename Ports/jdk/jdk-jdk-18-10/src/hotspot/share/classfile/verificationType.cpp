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
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/systemDictionaryShared.hpp"
#include "classfile/verificationType.hpp"
#include "classfile/verifier.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "logging/log.hpp"
#include "oops/klass.inline.hpp"
#include "runtime/arguments.hpp"
#include "runtime/handles.inline.hpp"

VerificationType VerificationType::from_tag(u1 tag) {
  switch (tag) {
    case ITEM_Top:     return bogus_type();
    case ITEM_Integer: return integer_type();
    case ITEM_Float:   return float_type();
    case ITEM_Double:  return double_type();
    case ITEM_Long:    return long_type();
    case ITEM_Null:    return null_type();
    default:
      ShouldNotReachHere();
      return bogus_type();
  }
}

bool VerificationType::resolve_and_check_assignability(InstanceKlass* klass, Symbol* name,
         Symbol* from_name, bool from_field_is_protected, bool from_is_array, bool from_is_object, TRAPS) {
  HandleMark hm(THREAD);
  Klass* this_class;
  if (klass->is_hidden() && klass->name() == name) {
    this_class = klass;
  } else {
    this_class = SystemDictionary::resolve_or_fail(
      name, Handle(THREAD, klass->class_loader()),
      Handle(THREAD, klass->protection_domain()), true, CHECK_false);
    if (log_is_enabled(Debug, class, resolve)) {
      Verifier::trace_class_resolution(this_class, klass);
    }
  }

  if (this_class->is_interface() && (!from_field_is_protected ||
      from_name != vmSymbols::java_lang_Object())) {
    // If we are not trying to access a protected field or method in
    // java.lang.Object then, for arrays, we only allow assignability
    // to interfaces java.lang.Cloneable and java.io.Serializable.
    // Otherwise, we treat interfaces as java.lang.Object.
    return !from_is_array ||
      this_class == vmClasses::Cloneable_klass() ||
      this_class == vmClasses::Serializable_klass();
  } else if (from_is_object) {
    Klass* from_class;
    if (klass->is_hidden() && klass->name() == from_name) {
      from_class = klass;
    } else {
      from_class = SystemDictionary::resolve_or_fail(
        from_name, Handle(THREAD, klass->class_loader()),
        Handle(THREAD, klass->protection_domain()), true, CHECK_false);
      if (log_is_enabled(Debug, class, resolve)) {
        Verifier::trace_class_resolution(from_class, klass);
      }
    }
    return from_class->is_subclass_of(this_class);
  }

  return false;
}

bool VerificationType::is_reference_assignable_from(
    const VerificationType& from, ClassVerifier* context,
    bool from_field_is_protected, TRAPS) const {
  InstanceKlass* klass = context->current_class();
  if (from.is_null()) {
    // null is assignable to any reference
    return true;
  } else if (is_null()) {
    return false;
  } else if (name() == from.name()) {
    return true;
  } else if (is_object()) {
    // We need check the class hierarchy to check assignability
    if (name() == vmSymbols::java_lang_Object()) {
      // any object or array is assignable to java.lang.Object
      return true;
    }

    if (Arguments::is_dumping_archive()) {
      if (SystemDictionaryShared::add_verification_constraint(klass,
              name(), from.name(), from_field_is_protected, from.is_array(),
              from.is_object())) {
        // If add_verification_constraint() returns true, the resolution/check should be
        // delayed until runtime.
        return true;
      }
    }

    return resolve_and_check_assignability(klass, name(), from.name(),
          from_field_is_protected, from.is_array(), from.is_object(), THREAD);
  } else if (is_array() && from.is_array()) {
    VerificationType comp_this = get_component(context);
    VerificationType comp_from = from.get_component(context);
    if (!comp_this.is_bogus() && !comp_from.is_bogus()) {
      return comp_this.is_component_assignable_from(comp_from, context,
                                                    from_field_is_protected, THREAD);
    }
  }
  return false;
}

VerificationType VerificationType::get_component(ClassVerifier *context) const {
  assert(is_array() && name()->utf8_length() >= 2, "Must be a valid array");
  SignatureStream ss(name(), false);
  ss.skip_array_prefix(1);
  switch (ss.type()) {
    case T_BOOLEAN: return VerificationType(Boolean);
    case T_BYTE:    return VerificationType(Byte);
    case T_CHAR:    return VerificationType(Char);
    case T_SHORT:   return VerificationType(Short);
    case T_INT:     return VerificationType(Integer);
    case T_LONG:    return VerificationType(Long);
    case T_FLOAT:   return VerificationType(Float);
    case T_DOUBLE:  return VerificationType(Double);
    case T_ARRAY:
    case T_OBJECT: {
      guarantee(ss.is_reference(), "unchecked verifier input?");
      Symbol* component = ss.as_symbol();
      // Create another symbol to save as signature stream unreferences this symbol.
      Symbol* component_copy = context->create_temporary_symbol(component);
      assert(component_copy == component, "symbols don't match");
      return VerificationType::reference_type(component_copy);
   }
   default:
     // Met an invalid type signature, e.g. [X
     return VerificationType::bogus_type();
  }
}

void VerificationType::print_on(outputStream* st) const {
  switch (_u._data) {
    case Bogus:            st->print("top"); break;
    case Category1:        st->print("category1"); break;
    case Category2:        st->print("category2"); break;
    case Category2_2nd:    st->print("category2_2nd"); break;
    case Boolean:          st->print("boolean"); break;
    case Byte:             st->print("byte"); break;
    case Short:            st->print("short"); break;
    case Char:             st->print("char"); break;
    case Integer:          st->print("integer"); break;
    case Float:            st->print("float"); break;
    case Long:             st->print("long"); break;
    case Double:           st->print("double"); break;
    case Long_2nd:         st->print("long_2nd"); break;
    case Double_2nd:       st->print("double_2nd"); break;
    case Null:             st->print("null"); break;
    case ReferenceQuery:   st->print("reference type"); break;
    case Category1Query:   st->print("category1 type"); break;
    case Category2Query:   st->print("category2 type"); break;
    case Category2_2ndQuery: st->print("category2_2nd type"); break;
    default:
      if (is_uninitialized_this()) {
        st->print("uninitializedThis");
      } else if (is_uninitialized()) {
        st->print("uninitialized %d", bci());
      } else {
        if (name() != NULL) {
          name()->print_value_on(st);
        } else {
          st->print_cr("NULL");
        }
      }
  }
}
