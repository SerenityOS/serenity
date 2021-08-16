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

#ifndef SHARE_JFR_JFR_HPP
#define SHARE_JFR_JFR_HPP

#include "jni.h"
#include "memory/allocation.hpp"

class JavaThread;
class Thread;
class Klass;

extern "C" void JNICALL jfr_register_natives(JNIEnv*, jclass);

//
// The VM interface to Flight Recorder.
//
class Jfr : AllStatic {
 public:
  static bool is_enabled();
  static bool is_disabled();
  static bool is_recording();
  static void on_create_vm_1();
  static void on_create_vm_2();
  static void on_create_vm_3();
  static void on_unloading_classes();
  static void on_thread_start(Thread* thread);
  static void on_thread_exit(Thread* thread);
  static void on_vm_shutdown(bool exception_handler = false);
  static bool on_flight_recorder_option(const JavaVMOption** option, char* delimiter);
  static bool on_start_flight_recording_option(const JavaVMOption** option, char* delimiter);
  static void on_vm_error_report(outputStream* st);
  static void exclude_thread(Thread* thread);
  static bool is_excluded(Thread* thread);
  static void include_thread(Thread* thread);

  // intrinsic support
  static void get_class_id_intrinsic(const Klass* klass);
  static address epoch_address();
  static address signal_address();
};

#endif // SHARE_JFR_JFR_HPP
