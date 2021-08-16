/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDLOADBARRIER_HPP
#define SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDLOADBARRIER_HPP

#include "jfr/utilities/jfrTypes.hpp"
#include "memory/allocation.hpp"

class ClassLoaderData;
class Klass;
class Method;
class ModuleEntry;
class PackageEntry;

/*
 * The JFR equivalent of a 'GC Load Barrier' where, instead of tracking object accesses on the heap,
 * we track accesses to JVM internal objects in native memory iff it stand in a relation to JFR events.
 *
 * Events can have fields referring to VM internal objects, for example Klass*, Method*, ClassLoaderData*, etc.
 * At an event site, objects, or more specifically pointers to objects, are stored into the event just before
 * the event is committed. As part of committing the event to the recording stream, instead of serializing these
 * pointers directly, the writer mechanism writes a unique value of type traceid used by JFR to represent it.
 * Conceptually, this is very similar to representing a reference using a foreign key.
 *
 * After this relation has been established, the JFR system must have a way to later locate the object in order to
 * serialize the information it represents, i.e to produce "tables" containing information related to foreign keys.
 * The information in these tables then materialize as constants in the recording stream delivered as part of Checkpoint events,
 * letting events containing references become resolvable.
 *
 * The 'load barrier' is a means to accomplish this: it intercepts loading of traceid values from JVM internal objects,
 * allowing JFR to keep track.
 *
 * Once intercepted, this tracking is implemented using two mechanisms:
 *
 * 'Tagging':
 * ----------
 * The barrier determines if the object needs to be marked, or tagged, and if so in what way.
 * Tagging is a function of the current epoch and is implemented as a bit pattern installed into the traceid field of the object.
 *
 * 'Root set' of Klasses:
 * ----------
 * JFR collects the set of tagged JVM internal objects at certain intervals. This set is derived from a subset, or 'root set',
 * consisting of incrementally tagged klasses for the epoch. The barrier enqueues a newly tagged klass, as a root, to an epoch-relative,
 * distributed queue. The collection step will use the queue to process the root set, from which most artifacts tagged can be discovered.
 *
 */
class JfrTraceIdLoadBarrier : AllStatic {
  friend class Jfr;
  friend class JfrCheckpointManager;
 private:
  static bool initialize();
  static void clear();
  static void destroy();
  static void enqueue(const Klass* klass);
  static void load_barrier(const Klass* klass);
 public:
  static traceid load(const ClassLoaderData* cld);
  static traceid load(const Klass* klass);
  static traceid load(const Klass* klass, const Method* method);
  static traceid load(const Method* method);
  static traceid load(const ModuleEntry* module);
  static traceid load(const PackageEntry* package);
  static traceid load_leakp(const Klass* klass, const Method* method); // leak profiler
  static void do_klasses(void f(Klass*), bool previous_epoch = false);
};

#endif // SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDLOADBARRIER_HPP
