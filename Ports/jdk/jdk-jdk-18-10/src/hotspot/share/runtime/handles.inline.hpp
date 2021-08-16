/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_HANDLES_INLINE_HPP
#define SHARE_RUNTIME_HANDLES_INLINE_HPP

#include "runtime/handles.hpp"

#include "runtime/thread.hpp"
#include "oops/metadata.hpp"
#include "oops/oop.hpp"

// these inline functions are in a separate file to break an include cycle
// between Thread and Handle

inline Handle::Handle(Thread* thread, oop obj) {
  assert(thread == Thread::current(), "sanity check");
  if (obj == NULL) {
    _handle = NULL;
  } else {
    _handle = thread->handle_area()->allocate_handle(obj);
  }
}

// Inline constructors for Specific Handles for different oop types
#define DEF_HANDLE_CONSTR(type, is_a)                   \
inline type##Handle::type##Handle (Thread* thread, type##Oop obj) : Handle(thread, (oop)obj) { \
  assert(is_null() || ((oop)obj)->is_a(), "illegal type");                \
}

DEF_HANDLE_CONSTR(instance , is_instance_noinline )
DEF_HANDLE_CONSTR(array    , is_array_noinline    )
DEF_HANDLE_CONSTR(objArray , is_objArray_noinline )
DEF_HANDLE_CONSTR(typeArray, is_typeArray_noinline)

// Constructor for metadata handles
#define DEF_METADATA_HANDLE_FN(name, type) \
inline name##Handle::name##Handle(Thread* thread, type* obj) : _value(obj), _thread(thread) { \
  if (obj != NULL) {                                                   \
    assert(((Metadata*)obj)->is_valid(), "obj is valid");              \
    assert(_thread == Thread::current(), "thread must be current");    \
    assert(_thread->is_in_live_stack((address)this), "not on stack?"); \
    _thread->metadata_handles()->push((Metadata*)obj);                 \
  }                                                                    \
}                                                                      \

DEF_METADATA_HANDLE_FN(method, Method)
DEF_METADATA_HANDLE_FN(constantPool, ConstantPool)

inline void HandleMark::push() {
  // This is intentionally a NOP. pop_and_restore will reset
  // values to the HandleMark further down the stack, typically
  // in JavaCalls::call_helper.
  debug_only(_area->_handle_mark_nesting++);
}

inline void HandleMark::pop_and_restore() {
  // Delete later chunks
  if(_chunk->next() != NULL) {
    assert(_area->size_in_bytes() > size_in_bytes(), "Sanity check");
    chop_later_chunks();
  } else {
    assert(_area->size_in_bytes() == size_in_bytes(), "Sanity check");
  }
  // Roll back arena to saved top markers
  _area->_chunk = _chunk;
  _area->_hwm = _hwm;
  _area->_max = _max;
  debug_only(_area->_handle_mark_nesting--);
}

inline HandleMarkCleaner::HandleMarkCleaner(Thread* thread) {
  _thread = thread;
  _thread->last_handle_mark()->push();
}

inline HandleMarkCleaner::~HandleMarkCleaner() {
  _thread->last_handle_mark()->pop_and_restore();
}

#endif // SHARE_RUNTIME_HANDLES_INLINE_HPP
