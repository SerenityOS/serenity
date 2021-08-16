/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_OBJECTCOUNTEVENTSENDER_HPP
#define SHARE_GC_SHARED_OBJECTCOUNTEVENTSENDER_HPP

#include "gc/shared/gcTrace.hpp"
#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include "utilities/ticks.hpp"

#if INCLUDE_SERVICES

class KlassInfoEntry;
class Klass;

class ObjectCountEventSender : public AllStatic {
  static bool _should_send_requestable_event;

  template <typename T>
  static void send_event_if_enabled(Klass* klass, jlong count, julong size, const Ticks& timestamp);

 public:
  static void enable_requestable_event();
  static void disable_requestable_event();

  static void send(const KlassInfoEntry* entry, const Ticks& timestamp);
  static bool should_send_event();
};

#endif // INCLUDE_SERVICES

#endif // SHARE_GC_SHARED_OBJECTCOUNTEVENTSENDER_HPP
