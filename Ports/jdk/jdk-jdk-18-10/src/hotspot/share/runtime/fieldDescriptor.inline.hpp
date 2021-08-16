/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_FIELDDESCRIPTOR_INLINE_HPP
#define SHARE_RUNTIME_FIELDDESCRIPTOR_INLINE_HPP

#include "runtime/fieldDescriptor.hpp"

#include "runtime/handles.inline.hpp"
#include "runtime/signature.hpp"

// All fieldDescriptor inline functions that (directly or indirectly) use "_cp()" or "_cp->"
// must be put in this file, as they require runtime/handles.inline.hpp.

inline Symbol* fieldDescriptor::name() const {
  return field()->name(_cp());
}

inline Symbol* fieldDescriptor::signature() const {
  return field()->signature(_cp());
}

inline InstanceKlass* fieldDescriptor::field_holder() const {
  return _cp->pool_holder();
}

inline ConstantPool* fieldDescriptor::constants() const {
  return _cp();
}

inline FieldInfo* fieldDescriptor::field() const {
  InstanceKlass* ik = field_holder();
  return ik->field(_index);
}

inline int fieldDescriptor::offset()                    const    { return field()->offset(); }
inline bool fieldDescriptor::has_initial_value()        const    { return field()->initval_index() != 0; }
inline int fieldDescriptor::initial_value_index()       const    { return field()->initval_index(); }

inline void fieldDescriptor::update_klass_field_access_flag() {
  InstanceKlass* ik = field_holder();
  ik->field(index())->set_access_flags(_access_flags.as_short());
}

inline void fieldDescriptor::set_is_field_access_watched(const bool value) {
  _access_flags.set_is_field_access_watched(value);
  update_klass_field_access_flag();
}

inline void fieldDescriptor::set_is_field_modification_watched(const bool value) {
  _access_flags.set_is_field_modification_watched(value);
  update_klass_field_access_flag();
}

inline void fieldDescriptor::set_has_initialized_final_update(const bool value) {
  _access_flags.set_has_field_initialized_final_update(value);
  update_klass_field_access_flag();
}

inline BasicType fieldDescriptor::field_type() const {
  return Signature::basic_type(signature());
}

#endif // SHARE_RUNTIME_FIELDDESCRIPTOR_INLINE_HPP
