/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "classfile/classLoaderData.hpp"
#include "jfr/jfrEvents.hpp"
#include "memory/metaspaceTracer.hpp"
#include "oops/oop.inline.hpp"

void MetaspaceTracer::report_gc_threshold(size_t old_val,
                                          size_t new_val,
                                          MetaspaceGCThresholdUpdater::Type updater) const {
  EventMetaspaceGCThreshold event;
  if (event.should_commit()) {
    event.set_oldValue(old_val);
    event.set_newValue(new_val);
    event.set_updater((u1)updater);
    event.commit();
  }
}

void MetaspaceTracer::report_metaspace_allocation_failure(ClassLoaderData *cld,
                                                          size_t word_size,
                                                          MetaspaceObj::Type objtype,
                                                          Metaspace::MetadataType mdtype) const {
  send_allocation_failure_event<EventMetaspaceAllocationFailure>(cld, word_size, objtype, mdtype);
}

void MetaspaceTracer::report_metadata_oom(ClassLoaderData *cld,
                                         size_t word_size,
                                         MetaspaceObj::Type objtype,
                                         Metaspace::MetadataType mdtype) const {
  send_allocation_failure_event<EventMetaspaceOOM>(cld, word_size, objtype, mdtype);
}

template <typename E>
void MetaspaceTracer::send_allocation_failure_event(ClassLoaderData *cld,
                                                    size_t word_size,
                                                    MetaspaceObj::Type objtype,
                                                    Metaspace::MetadataType mdtype) const {
  E event;
  if (event.should_commit()) {
    event.set_classLoader(cld);
    event.set_hiddenClassLoader(cld->has_class_mirror_holder());
    event.set_size(word_size * BytesPerWord);
    event.set_metadataType((u1) mdtype);
    event.set_metaspaceObjectType((u1) objtype);
    event.commit();
  }
}
