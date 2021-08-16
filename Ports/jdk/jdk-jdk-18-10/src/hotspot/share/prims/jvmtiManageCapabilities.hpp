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

#ifndef SHARE_PRIMS_JVMTIMANAGECAPABILITIES_HPP
#define SHARE_PRIMS_JVMTIMANAGECAPABILITIES_HPP

#include "jvmtifiles/jvmti.h"
#include "memory/allocation.hpp"

class JvmtiManageCapabilities : public AllStatic {

private:

  // these four capabilities sets represent all potentially
  // available capabilities.  They are disjoint, covering
  // the four cases: (OnLoad vs OnLoad+live phase) X
  // (one environment vs any environment).
  static jvmtiCapabilities always_capabilities;
  static jvmtiCapabilities onload_capabilities;
  static jvmtiCapabilities always_solo_capabilities;
  static jvmtiCapabilities onload_solo_capabilities;

  // solo capabilities that have not been grabbed
  static jvmtiCapabilities always_solo_remaining_capabilities;
  static jvmtiCapabilities onload_solo_remaining_capabilities;

  // all capabilities ever acquired
  static jvmtiCapabilities acquired_capabilities;

  // basic intenal operations
  static jvmtiCapabilities *either(const jvmtiCapabilities *a, const jvmtiCapabilities *b, jvmtiCapabilities *result);
  static jvmtiCapabilities *both(const jvmtiCapabilities *a, const jvmtiCapabilities *b, jvmtiCapabilities *result);
  static jvmtiCapabilities *exclude(const jvmtiCapabilities *a, const jvmtiCapabilities *b, jvmtiCapabilities *result);
  static bool has_some(const jvmtiCapabilities *a);
  static void update();

  // init functions
  static jvmtiCapabilities init_always_capabilities();
  static jvmtiCapabilities init_onload_capabilities();
  static jvmtiCapabilities init_always_solo_capabilities();
  static jvmtiCapabilities init_onload_solo_capabilities();

public:
  static void initialize();

  // queries and actions
  static void get_potential_capabilities(const jvmtiCapabilities *current,
                                         const jvmtiCapabilities *prohibited,
                                         jvmtiCapabilities *result);
  static jvmtiError add_capabilities(const jvmtiCapabilities *current,
                                     const jvmtiCapabilities *prohibited,
                                     const jvmtiCapabilities *desired,
                                     jvmtiCapabilities *result);
  static void relinquish_capabilities(const jvmtiCapabilities *current,
                                      const jvmtiCapabilities *unwanted,
                                      jvmtiCapabilities *result);
  static void copy_capabilities(const jvmtiCapabilities *from, jvmtiCapabilities *to);

#ifndef PRODUCT
  static void print(const jvmtiCapabilities* caps);
#endif
};

#endif // SHARE_PRIMS_JVMTIMANAGECAPABILITIES_HPP
