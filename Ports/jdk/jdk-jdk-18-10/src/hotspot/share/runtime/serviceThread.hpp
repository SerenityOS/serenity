/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_SERVICETHREAD_HPP
#define SHARE_RUNTIME_SERVICETHREAD_HPP

#include "prims/jvmtiImpl.hpp"
#include "runtime/thread.hpp"

// A hidden from external view JavaThread for JVMTI compiled-method-load
// events, oop storage cleanup, and the maintainance of string, symbol,
// protection domain, and resolved method tables.
class JvmtiDeferredEvent;

class ServiceThread : public JavaThread {
  friend class VMStructs;
 private:
  DEBUG_ONLY(static JavaThread* _instance;)
  static JvmtiDeferredEvent* _jvmti_event;
  static JvmtiDeferredEventQueue _jvmti_service_queue;

  static void service_thread_entry(JavaThread* thread, TRAPS);
  ServiceThread(ThreadFunction entry_point) : JavaThread(entry_point) {};

 public:
  static void initialize();

  // Hide this thread from external view.
  bool is_hidden_from_external_view() const      { return true; }
  bool is_service_thread() const                 { return true; }

  // Add event to the service thread event queue.
  static void enqueue_deferred_event(JvmtiDeferredEvent* event);
  static void add_oop_handle_release(OopHandle handle);

  // GC support
  void oops_do_no_frames(OopClosure* f, CodeBlobClosure* cf);
  void nmethods_do(CodeBlobClosure* cf);
};

#endif // SHARE_RUNTIME_SERVICETHREAD_HPP
