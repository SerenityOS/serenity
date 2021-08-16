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

#ifndef SHARE_PRIMS_JVMTICODEBLOBEVENTS_HPP
#define SHARE_PRIMS_JVMTICODEBLOBEVENTS_HPP

#include "jvmtifiles/jvmti.h"

// forward declaration
class JvmtiEnv;


// JVMTI code blob event support
// -- used by GenerateEvents to generate CompiledMethodLoad and
//    DynamicCodeGenerated events
// -- also provide utility function build_jvmti_addr_location_map to create
//    a jvmtiAddrLocationMap list for a nmethod.

class JvmtiCodeBlobEvents : public AllStatic {
 public:

  // generate a DYNAMIC_CODE_GENERATED_EVENT event for each non-nmethod
  // code blob in the code cache.
  static jvmtiError generate_dynamic_code_events(JvmtiEnv* env);

  // generate a COMPILED_METHOD_LOAD event for each nmethod
  // code blob in the code cache.
  static jvmtiError generate_compiled_method_load_events(JvmtiEnv* env);

  // create a C-heap allocated address location map for an nmethod
  static void build_jvmti_addr_location_map(nmethod *nm, jvmtiAddrLocationMap** map,
                                            jint *map_length);
};

#endif // SHARE_PRIMS_JVMTICODEBLOBEVENTS_HPP
