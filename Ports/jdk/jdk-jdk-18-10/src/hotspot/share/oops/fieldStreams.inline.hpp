/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_FIELDSTREAMS_INLINE_HPP
#define SHARE_OOPS_FIELDSTREAMS_INLINE_HPP

#include "oops/fieldStreams.hpp"

#include "runtime/thread.inline.hpp"

FieldStreamBase::FieldStreamBase(Array<u2>* fields, ConstantPool* constants, int start, int limit) : _fields(fields),
         _constants(constantPoolHandle(Thread::current(), constants)), _index(start) {
  _index = start;
  int num_fields = init_generic_signature_start_slot();
  if (limit < start) {
    _limit = num_fields;
  } else {
    _limit = limit;
  }
}

FieldStreamBase::FieldStreamBase(Array<u2>* fields, ConstantPool* constants) : _fields(fields),
         _constants(constantPoolHandle(Thread::current(), constants)), _index(0) {
  _limit = init_generic_signature_start_slot();
}

FieldStreamBase::FieldStreamBase(InstanceKlass* klass) : _fields(klass->fields()),
         _constants(constantPoolHandle(Thread::current(), klass->constants())), _index(0),
         _limit(klass->java_fields_count()) {
  init_generic_signature_start_slot();
  assert(klass == field_holder(), "");
}

#endif // SHARE_OOPS_FIELDSTREAMS_INLINE_HPP
