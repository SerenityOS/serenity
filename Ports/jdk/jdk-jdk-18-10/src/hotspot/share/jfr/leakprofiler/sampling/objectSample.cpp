/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/leakprofiler/sampling/objectSample.hpp"
#include "jfr/leakprofiler/sampling/objectSampler.hpp"
#include "oops/weakHandle.inline.hpp"
#include "runtime/handles.inline.hpp"

void ObjectSample::reset() {
  release();
  set_stack_trace_id(0);
  set_stack_trace_hash(0);
  release_references();
}

const oop ObjectSample::object() const {
  return _object.resolve();
}

bool ObjectSample::is_dead() const {
  return _object.peek() == NULL;
}

const oop* ObjectSample::object_addr() const {
  return _object.ptr_raw();
}

void ObjectSample::set_object(oop object) {
  assert(_object.is_empty(), "should be empty");
  Handle h(Thread::current(), object);
  _object = WeakHandle(ObjectSampler::oop_storage(), h);
}

void ObjectSample::release() {
  _object.release(ObjectSampler::oop_storage());
  _object = WeakHandle();
}
