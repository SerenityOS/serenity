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

#ifndef SHARE_JFR_SUPPORT_JFRJDKJFREVENT_HPP
#define SHARE_JFR_SUPPORT_JFRJDKJFREVENT_HPP

#include "jni.h"
#include "memory/allocation.hpp"
#include "utilities/exceptions.hpp"

class Klass;

//
// For convenient access to the event klass hierarchy:
//
//  - jdk.internal.event.Event (java.base)
//    - jdk.jfr.Event (jdk.jfr)
//      - sub klasses (...)
//
//  Although the top level klass is really jdk.internal.event.Event,
//  its role is primarily to allow event programming in module java.base.
//  We still call it the jdk.jfr.Event klass hierarchy, including
//  jdk.internal.event.Event.
//
class JdkJfrEvent : AllStatic {
 public:
  // jdk.jfr.Event
  static bool is(const Klass* k);
  static bool is(const jclass jc);
  static void tag_as(const Klass* k);

  // jdk.jfr.Event subklasses
  static bool is_subklass(const Klass* k);
  static bool is_subklass(const jclass jc);
  static void tag_as_subklass(const Klass* k);
  static void tag_as_subklass(const jclass jc);

  // jdk.jfr.Event hierarchy
  static bool is_a(const Klass* k);
  static bool is_a(const jclass jc);

  // klasses that host a jdk.jfr.Event
  static bool is_host(const Klass* k);
  static bool is_host(const jclass jc);
  static void tag_as_host(const Klass* k);
  static void tag_as_host(const jclass jc);

  // in the set of classes made visible to java
  static bool is_visible(const Klass* k);
  static bool is_visible(const jclass jc);

  // all klasses in the hierarchy
  static jobject get_all_klasses(TRAPS);
};

#endif // SHARE_JFR_SUPPORT_JFRJDKJFREVENT_HPP
