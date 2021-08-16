/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CIFIELD_HPP
#define SHARE_CI_CIFIELD_HPP

#include "ci/ciClassList.hpp"
#include "ci/ciConstant.hpp"
#include "ci/ciFlags.hpp"
#include "ci/ciInstance.hpp"
#include "ci/ciUtilities.hpp"

// ciField
//
// This class represents the result of a field lookup in the VM.
// The lookup may not succeed, in which case the information in
// the ciField will be incomplete.
class ciField : public ResourceObj {
  CI_PACKAGE_ACCESS
  friend class ciEnv;
  friend class ciInstanceKlass;

private:
  ciFlags          _flags;
  ciInstanceKlass* _holder;
  ciSymbol*        _name;
  ciSymbol*        _signature;
  ciType*          _type;
  int              _offset;
  bool             _is_constant;
  ciMethod*        _known_to_link_with_put;
  ciInstanceKlass* _known_to_link_with_get;
  ciConstant       _constant_value;

  ciType* compute_type();
  ciType* compute_type_impl();

  ciField(ciInstanceKlass* klass, int index);
  ciField(fieldDescriptor* fd);

  // shared constructor code
  void initialize_from(fieldDescriptor* fd);

public:
  ciFlags flags() const { return _flags; }

  // Of which klass is this field a member?
  //
  // Usage note: the declared holder of a field is the class
  // referenced by name in the bytecodes.  The canonical holder
  // is the most general class which holds the field.  This
  // method returns the canonical holder.  The declared holder
  // can be accessed via a method in ciBytecodeStream.
  //
  // Ex.
  //     class A {
  //       public int f = 7;
  //     }
  //     class B extends A {
  //       public void test() {
  //         System.out.println(f);
  //       }
  //     }
  //
  //   A java compiler is permitted to compile the access to
  //   field f as:
  //
  //     getfield B.f
  //
  //   In that case the declared holder of f would be B and
  //   the canonical holder of f would be A.
  ciInstanceKlass* holder() const { return _holder; }

  // Name of this field?
  ciSymbol* name() const { return _name; }

  // Signature of this field?
  ciSymbol* signature() const { return _signature; }

  // Of what type is this field?
  ciType* type() { return (_type == NULL) ? compute_type() : _type; }

  // How is this field actually stored in memory?
  BasicType layout_type() { return type2field[(_type == NULL) ? T_OBJECT : _type->basic_type()]; }

  // How big is this field in memory?
  int size_in_bytes() { return type2aelembytes(layout_type()); }

  // What is the offset of this field?
  int offset() const {
    assert(_offset >= 1, "illegal call to offset()");
    return _offset;
  }

  // Same question, explicit units.  (Fields are aligned to the byte level.)
  int offset_in_bytes() const {
    return offset();
  }

  // Is this field shared?
  bool is_shared() {
    // non-static fields of shared holders are cached
    return _holder->is_shared() && !is_static();
  }

  // Is this field a constant?
  //
  // Clarification: A field is considered constant if:
  //   1. The field is both static and final
  //   2. The field is not one of the special static/final
  //      non-constant fields.  These are java.lang.System.in
  //      and java.lang.System.out.  Abomination.
  //
  // A field is also considered constant if
  // - it is marked @Stable and is non-null (or non-zero, if a primitive) or
  // - it is trusted or
  // - it is the target field of a CallSite object.
  //
  // See ciField::initialize_from() for more details.
  //
  // A user should also check the field value (constant_value().is_valid()), since
  // constant fields of non-initialized classes don't have values yet.
  bool is_constant() const { return _is_constant; }

  // Get the constant value of the static field.
  ciConstant constant_value();

  bool is_static_constant() {
    return is_static() && is_constant() && constant_value().is_valid();
  }

  // Get the constant value of non-static final field in the given
  // object.
  ciConstant constant_value_of(ciObject* object);

  // Check for link time errors.  Accessing a field from a
  // certain method via a certain bytecode may or may not be legal.
  // This call checks to see if an exception may be raised by
  // an access of this field.
  //
  // Usage note: if the same field is accessed multiple times
  // in the same compilation, will_link will need to be checked
  // at each point of access.
  bool will_link(ciMethod* accessing_method,
                 Bytecodes::Code bc);

  // Java access flags
  bool is_public               () const { return flags().is_public(); }
  bool is_private              () const { return flags().is_private(); }
  bool is_protected            () const { return flags().is_protected(); }
  bool is_static               () const { return flags().is_static(); }
  bool is_final                () const { return flags().is_final(); }
  bool is_stable               () const { return flags().is_stable(); }
  bool is_volatile             () const { return flags().is_volatile(); }
  bool is_transient            () const { return flags().is_transient(); }
  // The field is modified outside of instance initializer methods
  // (or class/initializer methods if the field is static).
  bool has_initialized_final_update() const { return flags().has_initialized_final_update(); }

  bool is_call_site_target();

  bool is_autobox_cache();

  // Debugging output
  void print();
  void print_name_on(outputStream* st);
};

#endif // SHARE_CI_CIFIELD_HPP
