/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_MEMORY_METASPACETRACER_HPP
#define SHARE_MEMORY_METASPACETRACER_HPP

#include "memory/allocation.hpp"
#include "memory/metaspace.hpp"
#include "memory/metaspaceUtils.hpp"

class ClassLoaderData;

class MetaspaceTracer : public CHeapObj<mtTracing> {
  template <typename E>
  void send_allocation_failure_event(ClassLoaderData *cld,
                                     size_t word_size,
                                     MetaspaceObj::Type objtype,
                                     Metaspace::MetadataType mdtype) const;
 public:
  void report_gc_threshold(size_t old_val,
                           size_t new_val,
                           MetaspaceGCThresholdUpdater::Type updater) const;
  void report_metaspace_allocation_failure(ClassLoaderData *cld,
                                           size_t word_size,
                                           MetaspaceObj::Type objtype,
                                           Metaspace::MetadataType mdtype) const;
  void report_metadata_oom(ClassLoaderData *cld,
                           size_t word_size,
                           MetaspaceObj::Type objtype,
                           Metaspace::MetadataType mdtype) const;

};

#endif // SHARE_MEMORY_METASPACETRACER_HPP
