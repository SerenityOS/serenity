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

#ifndef SHARE_JFR_RECORDER_CHECKPOINT_TYPES_JFRTHREADSTATE_HPP
#define SHARE_JFR_RECORDER_CHECKPOINT_TYPES_JFRTHREADSTATE_HPP

#include "memory/allocation.hpp"

class JfrCheckpointWriter;
class Thread;

class JfrThreadState : public AllStatic {
 public:
  static void serialize(JfrCheckpointWriter& writer);
};

class JfrThreadId : public AllStatic {
public:
  static traceid id(const Thread* t);
  static traceid os_id(const Thread* t);
  static traceid jfr_id(const Thread* t);
};

class JfrThreadName : public AllStatic {
 public:
  // Requires a ResourceMark for get_thread_name/as_utf8
  static const char* name(const Thread* t);
};

#endif // SHARE_JFR_RECORDER_CHECKPOINT_TYPES_JFRTHREADSTATE_HPP
