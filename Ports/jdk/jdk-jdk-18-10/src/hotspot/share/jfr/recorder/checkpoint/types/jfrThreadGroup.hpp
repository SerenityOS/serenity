/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_CHECKPOINT_TYPES_JFRTHREADGROUP_HPP
#define SHARE_JFR_RECORDER_CHECKPOINT_TYPES_JFRTHREADGROUP_HPP

#include "jni.h"
#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrTypes.hpp"

class JfrCheckpointWriter;
template <typename>
class GrowableArray;
class JfrThreadGroupsHelper;
class JfrThreadGroupPointers;

class JfrThreadGroup : public JfrCHeapObj {
  friend class JfrCheckpointThreadClosure;
 private:
  static JfrThreadGroup* _instance;
  class JfrThreadGroupEntry;
  GrowableArray<JfrThreadGroupEntry*>* _list;

  JfrThreadGroup();
  JfrThreadGroupEntry* find_entry(const JfrThreadGroupPointers& ptrs) const;
  JfrThreadGroupEntry* new_entry(JfrThreadGroupPointers& ptrs);
  int add_entry(JfrThreadGroupEntry* const tge);

  void write_thread_group_entries(JfrCheckpointWriter& writer) const;
  void write_selective_thread_group(JfrCheckpointWriter* writer, traceid thread_group_id) const;

  static traceid thread_group_id_internal(JfrThreadGroupsHelper& helper);
  static JfrThreadGroup* instance();
  static void set_instance(JfrThreadGroup* new_instance);

 public:
  ~JfrThreadGroup();
  static void serialize(JfrCheckpointWriter& w);
  static void serialize(JfrCheckpointWriter* w, traceid thread_group_id);
  static traceid thread_group_id(JavaThread* thread);
  static traceid thread_group_id(const JavaThread* thread, Thread* current);
};

#endif // SHARE_JFR_RECORDER_CHECKPOINT_TYPES_JFRTHREADGROUP_HPP
