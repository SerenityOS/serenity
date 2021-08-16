/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciKlass.hpp"
#include "ci/ciSymbol.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/oop.inline.hpp"

// ciKlass
//
// This class represents a Klass* in the HotSpot virtual
// machine.

// ------------------------------------------------------------------
// ciKlass::ciKlass
ciKlass::ciKlass(Klass* k) : ciType(k) {
  assert(get_Klass()->is_klass(), "wrong type");
  Klass* klass = get_Klass();
  _layout_helper = klass->layout_helper();
  Symbol* klass_name = klass->name();
  assert(klass_name != NULL, "wrong ciKlass constructor");
  _name = CURRENT_ENV->get_symbol(klass_name);
}

// ------------------------------------------------------------------
// ciKlass::ciKlass
//
// Nameless klass variant.
ciKlass::ciKlass(Klass* k, ciSymbol* name) : ciType(k) {
  assert(get_Klass()->is_klass(), "wrong type");
  _name = name;
  _layout_helper = Klass::_lh_neutral_value;
}

// ------------------------------------------------------------------
// ciKlass::ciKlass
//
// Unloaded klass variant.
ciKlass::ciKlass(ciSymbol* name, BasicType bt) : ciType(bt) {
  _name = name;
  _layout_helper = Klass::_lh_neutral_value;
}

// ------------------------------------------------------------------
// ciKlass::is_subtype_of
bool ciKlass::is_subtype_of(ciKlass* that) {
  assert(this->is_loaded(), "must be loaded: %s", this->name()->as_quoted_ascii());
  assert(that->is_loaded(), "must be loaded: %s", that->name()->as_quoted_ascii());

  // Check to see if the klasses are identical.
  if (this == that) {
    return true;
  }

  bool is_subtype;
  GUARDED_VM_ENTRY(is_subtype = get_Klass()->is_subtype_of(that->get_Klass());)

  // Ensure consistency with ciInstanceKlass::has_subklass().
  assert(!that->is_instance_klass() || // array klasses are irrelevant
          that->is_interface()      || // has_subklass is always false for interfaces
         !is_subtype || that->as_instance_klass()->has_subklass(), "inconsistent");

  return is_subtype;
}

// ------------------------------------------------------------------
// ciKlass::is_subclass_of
bool ciKlass::is_subclass_of(ciKlass* that) {
  assert(this->is_loaded(), "must be loaded: %s", this->name()->as_quoted_ascii());
  assert(that->is_loaded(), "must be loaded: %s", that->name()->as_quoted_ascii());

  // Check to see if the klasses are identical.
  if (this == that) {
    return true;
  }

  bool is_subclass;
  GUARDED_VM_ENTRY(is_subclass = get_Klass()->is_subclass_of(that->get_Klass());)

  // Ensure consistency with ciInstanceKlass::has_subklass().
  assert(!that->is_instance_klass() || // array klasses are irrelevant
          that->is_interface()      || // has_subklass is always false for interfaces
         !is_subclass || that->as_instance_klass()->has_subklass(), "inconsistent");

  return is_subclass;
}

// ------------------------------------------------------------------
// ciKlass::super_depth
juint ciKlass::super_depth() {
  assert(is_loaded(), "must be loaded");

  VM_ENTRY_MARK;
  Klass* this_klass = get_Klass();
  return this_klass->super_depth();
}

// ------------------------------------------------------------------
// ciKlass::super_check_offset
juint ciKlass::super_check_offset() {
  assert(is_loaded(), "must be loaded");

  VM_ENTRY_MARK;
  Klass* this_klass = get_Klass();
  return this_klass->super_check_offset();
}

// ------------------------------------------------------------------
// ciKlass::super_of_depth
ciKlass* ciKlass::super_of_depth(juint i) {
  assert(is_loaded(), "must be loaded");

  VM_ENTRY_MARK;
  Klass* this_klass = get_Klass();
  Klass* super = this_klass->primary_super_of_depth(i);
  return (super != NULL) ? CURRENT_THREAD_ENV->get_klass(super) : NULL;
}

// ------------------------------------------------------------------
// ciKlass::least_common_ancestor
//
// Get the shared parent of two klasses.
//
// Implementation note: this method currently goes "over the wall"
// and does all of the work on the VM side.  It could be rewritten
// to use the super() method and do all of the work (aside from the
// lazy computation of super()) in native mode.  This may be
// worthwhile if the compiler is repeatedly requesting the same lca
// computation or possibly if most of the superklasses have already
// been created as ciObjects anyway.  Something to think about...
ciKlass*
ciKlass::least_common_ancestor(ciKlass* that) {
  assert(is_loaded() && that->is_loaded(), "must be loaded");
  // Check to see if the klasses are identical.
  if (this == that) {
    return this;
  }

  VM_ENTRY_MARK;
  Klass* this_klass = get_Klass();
  Klass* that_klass = that->get_Klass();
  Klass* lca        = this_klass->LCA(that_klass);

  // Many times the LCA will be either this_klass or that_klass.
  // Treat these as special cases.
  if (lca == that_klass) {
    assert(this->is_subtype_of(that), "sanity");
    return that;
  }
  if (this_klass == lca) {
    assert(that->is_subtype_of(this), "sanity");
    return this;
  }

  // Create the ciInstanceKlass for the lca.
  ciKlass* result =
    CURRENT_THREAD_ENV->get_klass(lca);

  assert(this->is_subtype_of(result) && that->is_subtype_of(result), "sanity");
  return result;
}

// ------------------------------------------------------------------
// ciKlass::find_klass
//
// Find a klass using this klass's class loader.
ciKlass* ciKlass::find_klass(ciSymbol* klass_name) {
  assert(is_loaded(), "cannot find_klass through an unloaded klass");
  return CURRENT_ENV->get_klass_by_name(this,
                                        klass_name, false);
}

// ------------------------------------------------------------------
// ciKlass::java_mirror
//
// Get the instance of java.lang.Class corresponding to this klass.
// If it is an unloaded instance or array klass, return an unloaded
// mirror object of type Class.
ciInstance* ciKlass::java_mirror() {
  GUARDED_VM_ENTRY(
    if (!is_loaded())
      return ciEnv::current()->get_unloaded_klass_mirror(this);
    oop java_mirror = get_Klass()->java_mirror();
    return CURRENT_ENV->get_instance(java_mirror);
  )
}

// ------------------------------------------------------------------
// ciKlass::modifier_flags
jint ciKlass::modifier_flags() {
  assert(is_loaded(), "not loaded");
  GUARDED_VM_ENTRY(
    return get_Klass()->modifier_flags();
  )
}

// ------------------------------------------------------------------
// ciKlass::access_flags
jint ciKlass::access_flags() {
  assert(is_loaded(), "not loaded");
  GUARDED_VM_ENTRY(
    return get_Klass()->access_flags().as_int();
  )
}

// ------------------------------------------------------------------
// ciKlass::print_impl
//
// Implementation of the print method
void ciKlass::print_impl(outputStream* st) {
  st->print(" name=");
  print_name_on(st);
}

// ------------------------------------------------------------------
// ciKlass::print_name
//
// Print the name of this klass
void ciKlass::print_name_on(outputStream* st) {
  name()->print_symbol_on(st);
}

const char* ciKlass::external_name() const {
  GUARDED_VM_ENTRY(
    return get_Klass()->external_name();
  )
}
