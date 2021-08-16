/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_JFRRECORDER_HPP
#define SHARE_JFR_RECORDER_JFRRECORDER_HPP

#include "jfr/utilities/jfrAllocation.hpp"

class JavaThread;
class Thread;

//
// Represents the singleton instance of Flight Recorder.
// Lifecycle management of recorder components.
//
class JfrRecorder : public JfrCHeapObj {
  friend class Jfr;
  friend void recorderthread_entry(JavaThread*, JavaThread*);
 private:
  static bool on_create_vm_1();
  static bool on_create_vm_2();
  static bool on_create_vm_3();
  static bool create_checkpoint_manager();
  static bool create_chunk_repository();
  static bool create_java_event_writer();
  static bool create_jvmti_agent();
  static bool create_oop_storages();
  static bool create_os_interface();
  static bool create_post_box();
  static bool create_recorder_thread();
  static bool create_stacktrace_repository();
  static bool create_storage();
  static bool create_stringpool();
  static bool create_thread_sampling();
  static bool create_event_throttler();
  static bool create_components();
  static void destroy_components();
  static void on_recorder_thread_exit();

 public:
  static bool is_enabled();
  static bool is_disabled();
  static bool create(bool simulate_failure);
  static bool is_created();
  static void destroy();
  static void start_recording();
  static bool is_recording();
  static void stop_recording();
};

#endif // SHARE_JFR_RECORDER_JFRRECORDER_HPP
