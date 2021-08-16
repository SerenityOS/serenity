/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdBits.inline.hpp"
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdEpoch.hpp"
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdMacros.hpp"
#include "jfr/support/jfrMethodLookup.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/method.inline.hpp"

// The InstanceKlass is assumed to be the method holder for the method to be looked up.
static const Method* lookup_method(InstanceKlass* ik, int orig_method_id_num) {
  assert(ik != NULL, "invariant");
  assert(orig_method_id_num >= 0, "invariant");
  assert(orig_method_id_num < ik->methods()->length(), "invariant");
  const Method* const m = ik->method_with_orig_idnum(orig_method_id_num);
  assert(m != NULL, "invariant");
  assert(m->orig_method_idnum() == orig_method_id_num, "invariant");
  assert(!m->is_obsolete(), "invariant");
  assert(ik == m->method_holder(), "invariant");
  return m;
}

const Method* JfrMethodLookup::lookup(const InstanceKlass* ik, traceid method_id) {
  assert(ik != NULL, "invariant");
  return lookup_method(const_cast<InstanceKlass*>(ik), method_id_num(method_id));
}

int JfrMethodLookup::method_id_num(traceid method_id) {
  return (int)(method_id & METHOD_ID_NUM_MASK);
}

traceid JfrMethodLookup::method_id(const Method* method) {
  assert(method != NULL, "invariant");
  return METHOD_ID(method->method_holder(), method);
}

traceid JfrMethodLookup::klass_id(traceid method_id) {
  return method_id >> TRACE_ID_SHIFT;
}

traceid JfrMethodLookup::klass_id(const Method* method) {
  return klass_id(method_id(method));
}
