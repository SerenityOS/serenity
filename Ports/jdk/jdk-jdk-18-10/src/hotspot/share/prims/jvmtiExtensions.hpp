/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_JVMTIEXTENSIONS_HPP
#define SHARE_PRIMS_JVMTIEXTENSIONS_HPP

#include "jvmtifiles/jvmti.h"
#include "jvmtifiles/jvmtiEnv.hpp"
#include "memory/allocation.hpp"

// JvmtiExtensions
//
// Maintains the list of extension functions and events in this JVMTI
// implementation. The list of functions and events can be obtained by
// the profiler using the JVMTI GetExtensionFunctions and
// GetExtensionEvents functions.

class JvmtiExtensions : public AllStatic {
 private:
  static GrowableArray<jvmtiExtensionFunctionInfo*>* _ext_functions;
  static GrowableArray<jvmtiExtensionEventInfo*>* _ext_events;

 public:
  // register extensions function
  static void register_extensions();

  // returns the list of extension functions
  static jvmtiError get_functions(JvmtiEnv* env, jint* extension_count_ptr,
                                  jvmtiExtensionFunctionInfo** extensions);

  // returns the list of extension events
  static jvmtiError get_events(JvmtiEnv* env, jint* extension_count_ptr,
                               jvmtiExtensionEventInfo** extensions);

  // sets the callback function for an extension event and enables the event
  static jvmtiError set_event_callback(JvmtiEnv* env, jint extension_event_index,
                                       jvmtiExtensionEvent callback);
};

#endif // SHARE_PRIMS_JVMTIEXTENSIONS_HPP
