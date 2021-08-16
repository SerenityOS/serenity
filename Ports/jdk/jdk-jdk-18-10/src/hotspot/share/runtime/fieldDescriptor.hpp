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

#ifndef SHARE_RUNTIME_FIELDDESCRIPTOR_HPP
#define SHARE_RUNTIME_FIELDDESCRIPTOR_HPP

#include "oops/constantPool.hpp"
#include "oops/fieldInfo.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/symbol.hpp"
#include "utilities/accessFlags.hpp"
#include "utilities/constantTag.hpp"

// A fieldDescriptor describes the attributes of a single field (instance or class variable).
// It needs the class constant pool to work (because it only holds indices into the pool
// rather than the actual info).

class fieldDescriptor {
 private:
  AccessFlags         _access_flags;
  int                 _index; // the field index
  constantPoolHandle  _cp;

  // update the access_flags for the field in the klass
  inline void update_klass_field_access_flag();

  inline FieldInfo* field() const;

 public:
  fieldDescriptor() {
    DEBUG_ONLY(_index = badInt);
  }
  fieldDescriptor(InstanceKlass* ik, int index) {
    DEBUG_ONLY(_index = badInt);
    reinitialize(ik, index);
  }
  inline Symbol* name() const;
  inline Symbol* signature() const;
  inline InstanceKlass* field_holder() const;
  inline ConstantPool* constants() const;

  AccessFlags access_flags()      const    { return _access_flags; }
  oop loader()                    const;
  // Offset (in words) of field from start of instanceOop / Klass*
  inline int offset()             const;
  Symbol* generic_signature()     const;
  int index()                     const    { return _index; }
  AnnotationArray* annotations()  const;
  AnnotationArray* type_annotations()  const;

  // Initial field value
  inline bool has_initial_value()        const;
  inline int initial_value_index()       const;
  constantTag initial_value_tag() const;  // The tag will return true on one of is_int(), is_long(), is_single(), is_double()
  jint int_initial_value()        const;
  jlong long_initial_value()      const;
  jfloat float_initial_value()    const;
  jdouble double_initial_value()  const;
  oop string_initial_value(TRAPS) const;

  // Field signature type
  inline BasicType field_type() const;

  // Access flags
  bool is_public()                const    { return access_flags().is_public(); }
  bool is_private()               const    { return access_flags().is_private(); }
  bool is_protected()             const    { return access_flags().is_protected(); }
  bool is_package_private()       const    { return !is_public() && !is_private() && !is_protected(); }

  bool is_static()                const    { return access_flags().is_static(); }
  bool is_final()                 const    { return access_flags().is_final(); }
  bool is_stable()                const    { return access_flags().is_stable(); }
  bool is_volatile()              const    { return access_flags().is_volatile(); }
  bool is_transient()             const    { return access_flags().is_transient(); }

  bool is_synthetic()             const    { return access_flags().is_synthetic(); }

  bool is_field_access_watched()  const    { return access_flags().is_field_access_watched(); }
  bool is_field_modification_watched() const
                                           { return access_flags().is_field_modification_watched(); }
  bool has_initialized_final_update() const { return access_flags().has_field_initialized_final_update(); }
  bool has_generic_signature()    const    { return access_flags().field_has_generic_signature(); }

  bool is_trusted_final()         const;

  inline void set_is_field_access_watched(const bool value);
  inline void set_is_field_modification_watched(const bool value);
  inline void set_has_initialized_final_update(const bool value);

  // Initialization
  void reinitialize(InstanceKlass* ik, int index);

  // Print
  void print() const;
  void print_on(outputStream* st) const;
  void print_on_for(outputStream* st, oop obj);
  void verify() const                           PRODUCT_RETURN;
};

#endif // SHARE_RUNTIME_FIELDDESCRIPTOR_HPP
