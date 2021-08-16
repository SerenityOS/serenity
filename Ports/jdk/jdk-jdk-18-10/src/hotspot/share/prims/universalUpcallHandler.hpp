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
 */

#ifndef SHARE_VM_PRIMS_UNIVERSALUPCALLHANDLER_HPP
#define SHARE_VM_PRIMS_UNIVERSALUPCALLHANDLER_HPP

#include "asm/codeBuffer.hpp"
#include "code/codeBlob.hpp"
#include "prims/foreign_globals.hpp"

class JavaThread;

class ProgrammableUpcallHandler {
private:
  static constexpr CodeBuffer::csize_t upcall_stub_size = 1024;

  struct UpcallMethod {
    Klass* klass;
    Symbol* name;
    Symbol* sig;
  } upcall_method;

  ProgrammableUpcallHandler();

  static const ProgrammableUpcallHandler& instance();

  static void upcall_helper(JavaThread* thread, jobject rec, address buff);
  static void attach_thread_and_do_upcall(jobject rec, address buff);

  static void handle_uncaught_exception(oop exception);
  static JavaThread* maybe_attach_and_get_thread(bool* should_detach);
  static void detach_current_thread();

  static JavaThread* on_entry(OptimizedEntryBlob::FrameData* context);
  static void on_exit(OptimizedEntryBlob::FrameData* context);
public:
  static address generate_optimized_upcall_stub(jobject mh, Method* entry, jobject jabi, jobject jconv);
  static address generate_upcall_stub(jobject rec, jobject abi, jobject buffer_layout);
  static bool supports_optimized_upcalls();
};

#endif // SHARE_VM_PRIMS_UNIVERSALUPCALLHANDLER_HPP
