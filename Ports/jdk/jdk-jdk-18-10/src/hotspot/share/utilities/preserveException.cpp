/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/resourceArea.hpp"
#include "oops/oop.hpp"
#include "runtime/handles.inline.hpp"
#include "utilities/preserveException.hpp"

PreserveExceptionMark::PreserveExceptionMark(Thread* thread) {
  _thread = thread;
  _preserved_exception_oop = Handle(thread, _thread->pending_exception());
  _preserved_exception_line = _thread->exception_line();
  _preserved_exception_file = _thread->exception_file();
  _thread->clear_pending_exception(); // Pending exceptions are checked in the destructor
}


PreserveExceptionMark::~PreserveExceptionMark() {
  if (_thread->has_pending_exception()) {
    oop exception = _thread->pending_exception();
    _thread->clear_pending_exception(); // Needed to avoid infinite recursion
    ResourceMark rm(_thread);
    assert(false, "PreserveExceptionMark destructor expects no pending exceptions %s",
                  exception->print_string());
  }

  if (_preserved_exception_oop() != NULL) {
    _thread->set_pending_exception(_preserved_exception_oop(), _preserved_exception_file, _preserved_exception_line);
  }
}

void WeakPreserveExceptionMark::preserve() {
  _preserved_exception_oop = Handle(_thread, _thread->pending_exception());
  _preserved_exception_line = _thread->exception_line();
  _preserved_exception_file = _thread->exception_file();
  _thread->clear_pending_exception();
}

void WeakPreserveExceptionMark::restore() {
  if (!_thread->has_pending_exception()) {
    _thread->set_pending_exception(_preserved_exception_oop(), _preserved_exception_file, _preserved_exception_line);
  }
}
