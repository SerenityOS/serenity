/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEID_HPP
#define SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEID_HPP

#include "jni.h"
#include "jfr/utilities/jfrTypes.hpp"
#include "memory/allocation.hpp"

class ClassLoaderData;
class Klass;
class Method;
class ModuleEntry;
class PackageEntry;
class Thread;

/*
 * JfrTraceId is a means of tagging, e.g. marking, specific instances as being actively in-use.
 * The most common situation is a committed event that has a field that is referring to a specific instance.
 * Now there exist a relation between an event (field) and an artifact of some kind.
 * We track this relation during runtime using the JfrTraceId mechanism in order to reify it into the chunk
 * where the event is finally written.
 *
 * It is the event commit mechanism that tags instances as in-use. The tag routines return the untagged traceid
 * as a mapping key, and the commit mechanism writes the key into the event field.
 * Consequently, the mechanism is opaque and not something a user needs to know about.
 * Indeed, the API promotes using well-known JVM concepts directly in events, such as having a Klass* as an event field.
 *
 * Tagging allows for many-to-one mappings of constants, lazy evaluation / collection of tags during chunk rotation
 * and concurrency (by using an epoch relative tagging scheme).
 *
 * JfrTraceId(s) have been added to support tagging instances of classes such as:
 *
 *   Klass (includes Method)
 *   ClassLoaderData
 *   ModuleEntry
 *   PackageEntry
 *
 * These classes have been extended to include a _traceid field (64-bits).
 *
 * Each instance is uniquely identified by a type-relative monotonic counter that is unique over the VM lifecycle.
 * "Tagging an instance" essentially means to set contextually determined (by epoch) marker bits in the _traceid field.
 * The constants associated with a tagged instance is a set of which is determined by a constant type definition,
 * and these constants are then serialized in an upcoming checkpoint event for the relevant chunk.
 *
 * Note that a "tagging" is relative to a chunk. Having serialized the tagged instance, the tag bits are reset (for that epoch).
 * As mentioned previously, the returned traceid is always the untagged value.
 *
 * We also use the _traceid field in Klass to quickly identify (bit check) if a newly loaded klass is of type jdk.jfr.Event.
 * (see jfr/instrumentation/jfrEventClassTransformer.cpp)
 *
 *
 * _traceid bit layout and description planned to go here
 *
 *
 */

class JfrTraceId : public AllStatic {
 public:
  static void assign(const Klass* klass);
  static void assign(const ModuleEntry* module);
  static void assign(const PackageEntry* package);
  static void assign(const ClassLoaderData* cld);
  static traceid assign_primitive_klass_id();
  static traceid assign_thread_id();

  // through load barrier
  static traceid load(const Klass* klass);
  static traceid load(jclass jc, bool raw = false);
  static traceid load(const Method* method);
  static traceid load(const Klass* klass, const Method* method);
  static traceid load(const ModuleEntry* module);
  static traceid load(const PackageEntry* package);
  static traceid load(const ClassLoaderData* cld);
  static traceid load_leakp(const Klass* klass, const Method* method); // leak profiler

  // load barrier elision
  static traceid load_raw(const Klass* klass);
  static traceid load_raw(jclass jc);
  static traceid load_raw(const Thread* thread);
  static traceid load_raw(const Method* method);
  static traceid load_raw(const ModuleEntry* module);
  static traceid load_raw(const PackageEntry* package);
  static traceid load_raw(const ClassLoaderData* cld);

  static void remove(const Klass* klass);
  static void remove(const Method* method);
  static void restore(const Klass* klass);

  // set of event classes made visible to java
  static bool in_visible_set(const Klass* k);
  static bool in_visible_set(const jclass jc);

  // jdk.jfr.Event
  static bool is_jdk_jfr_event(const Klass* k);
  static bool is_jdk_jfr_event(const jclass jc);
  static void tag_as_jdk_jfr_event(const Klass* k);

  // jdk.jfr.Event subklasses
  static bool is_jdk_jfr_event_sub(const Klass* k);
  static bool is_jdk_jfr_event_sub(const jclass jc);
  static void tag_as_jdk_jfr_event_sub(const Klass* k);
  static void tag_as_jdk_jfr_event_sub(const jclass jc);

  static bool in_jdk_jfr_event_hierarchy(const Klass* k);
  static bool in_jdk_jfr_event_hierarchy(const jclass jc);

  // klasses that host an event
  static bool is_event_host(const Klass* k);
  static bool is_event_host(const jclass jc);
  static void tag_as_event_host(const Klass* k);
  static void tag_as_event_host(const jclass jc);
};

#endif // SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEID_HPP
