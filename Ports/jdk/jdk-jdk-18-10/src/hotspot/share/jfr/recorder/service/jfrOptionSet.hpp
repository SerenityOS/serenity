/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_SERVICE_JFROPTIONSET_HPP
#define SHARE_JFR_RECORDER_SERVICE_JFROPTIONSET_HPP

#include "jni.h"
#include "memory/allocation.hpp"
#include "utilities/exceptions.hpp"

template <typename>
class GrowableArray;

//
// Command-line options and defaults
//
class JfrOptionSet : public AllStatic {
  friend class JfrRecorder;
 private:
  static jlong _max_chunk_size;
  static jlong _global_buffer_size;
  static jlong _thread_buffer_size;
  static jlong _memory_size;
  static jlong _num_global_buffers;
  static jlong _old_object_queue_size;
  static u4 _stack_depth;
  static jboolean _sample_threads;
  static jboolean _retransform;
  static jboolean _sample_protection;

  static bool initialize(JavaThread* thread);
  static bool configure(TRAPS);
  static bool adjust_memory_options();

 public:
  static jlong max_chunk_size();
  static void set_max_chunk_size(jlong value);
  static jlong global_buffer_size();
  static void set_global_buffer_size(jlong value);
  static jlong thread_buffer_size();
  static void set_thread_buffer_size(jlong value);
  static jlong memory_size();
  static void set_memory_size(jlong value);
  static jlong num_global_buffers();
  static void set_num_global_buffers(jlong value);
  static jint old_object_queue_size();
  static void set_old_object_queue_size(jlong value);
  static u4 stackdepth();
  static void set_stackdepth(u4 depth);
  static bool sample_threads();
  static void set_sample_threads(jboolean sample);
  static bool can_retransform();
  static void set_retransform(jboolean value);
  static bool compressed_integers();
  static bool allow_retransforms();
  static bool allow_event_retransforms();
  static bool sample_protection();
  DEBUG_ONLY(static void set_sample_protection(jboolean protection);)

  static bool parse_flight_recorder_option(const JavaVMOption** option, char* delimiter);
  static bool parse_start_flight_recording_option(const JavaVMOption** option, char* delimiter);
  static const GrowableArray<const char*>* start_flight_recording_options();
  static void release_start_flight_recording_options();
};

#endif // SHARE_JFR_RECORDER_SERVICE_JFROPTIONSET_HPP
