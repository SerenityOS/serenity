/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_JVMTIUTIL_HPP
#define SHARE_PRIMS_JVMTIUTIL_HPP

#include "jvmtifiles/jvmti.h"
#include "memory/resourceArea.hpp"
#include "prims/jvmtiEventController.hpp"

///////////////////////////////////////////////////////////////
//
// class JvmtiUtil
//
// class for miscellaneous jvmti utility static methods
//

class JvmtiUtil : AllStatic {

  static ResourceArea* _single_threaded_resource_area;

  static const char* _error_names[];
  static const bool  _event_threaded[];

public:

  static ResourceArea* single_threaded_resource_area();

  static const char* error_name(int num)    { return _error_names[num]; }    // To Do: add range checking

  static const bool has_event_capability(jvmtiEvent event_type, const jvmtiCapabilities* capabilities_ptr);

  static const bool  event_threaded(int num) {
    if (num >= JVMTI_MIN_EVENT_TYPE_VAL && num <= JVMTI_MAX_EVENT_TYPE_VAL) {
      return _event_threaded[num];
    }
    if (num >= EXT_MIN_EVENT_TYPE_VAL && num <= EXT_MAX_EVENT_TYPE_VAL) {
      return false;
    }
    ShouldNotReachHere();
    return false;
  }
};


///////////////////////////////////////////////////////////////
//
// class SafeResourceMark
//
// ResourceMarks that work before threads exist
//

class SafeResourceMark : public ResourceMark {

  ResourceArea* safe_resource_area() {
    Thread* thread;

    if (Threads::number_of_threads() == 0) {
      return JvmtiUtil::single_threaded_resource_area();
    }
    thread = Thread::current_or_null();
    if (thread == NULL) {
      return JvmtiUtil::single_threaded_resource_area();
    }
    return thread->resource_area();
  }

 public:

  SafeResourceMark() : ResourceMark(safe_resource_area()) {}

};

#endif // SHARE_PRIMS_JVMTIUTIL_HPP
