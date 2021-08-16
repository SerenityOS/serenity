/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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

#ifndef SHARE_MEMORY_METASPACE_METASPACECONTEXT_HPP
#define SHARE_MEMORY_METASPACE_METASPACECONTEXT_HPP

#include "memory/allocation.hpp"
#include "memory/virtualspace.hpp"
#include "utilities/debug.hpp"

class outputStream;

namespace metaspace {

class ChunkManager;
class VirtualSpaceList;
class CommitLimiter;

// MetaspaceContext is a convenience bracket around:
//
// - a VirtualSpaceList managing a memory area used for Metaspace
// - a ChunkManager sitting atop of that which manages chunk freelists
//
// In a normal VM only one or two of these contexts ever exist: one for the metaspace, and
//  optionally another one for the compressed class space.
//
// For tests more contexts may be created, and this would also be a way to use Metaspace
//  for things other than class metadata. We would have to work on the naming then.
//
// - (Future TODO): Context should own a lock to guard it. Currently this stuff is guarded
//     by one global lock, the slightly misnamed Metaspace_expandlock, but that one
//     should be split into one per context.
// - (Future TODO): Context can/should have its own allocation alignment. That way we
//     can have different alignment between class space and non-class metaspace. That could
//     help optimize compressed class pointer encoding, see discussion for JDK-8244943).

class MetaspaceContext : public CHeapObj<mtMetaspace> {

  const char* const _name;
  VirtualSpaceList* const _vslist;
  ChunkManager* const _cm;

  MetaspaceContext(const char* name, VirtualSpaceList* vslist, ChunkManager* cm) :
    _name(name),
    _vslist(vslist),
    _cm(cm)
  {}

  static MetaspaceContext* _nonclass_space_context;
  static MetaspaceContext* _class_space_context;

public:

  // Destroys the context: deletes chunkmanager and virtualspacelist.
  // If this is a non-expandable context over an existing space, that space remains
  // untouched, otherwise all memory is unmapped.
  ~MetaspaceContext();

  VirtualSpaceList* vslist() { return _vslist; }
  ChunkManager* cm() { return _cm; }

  // Create a new, empty, expandable metaspace context.
  static MetaspaceContext* create_expandable_context(const char* name, CommitLimiter* commit_limiter);

  // Create a new, empty, non-expandable metaspace context atop of an externally provided space.
  static MetaspaceContext* create_nonexpandable_context(const char* name, ReservedSpace rs, CommitLimiter* commit_limiter);

  void print_on(outputStream* st) const;

  DEBUG_ONLY(void verify() const;)

  static void initialize_class_space_context(ReservedSpace rs);
  static void initialize_nonclass_space_context();

  // Returns pointer to the global metaspace context.
  // If compressed class space is active, this contains the non-class-space allocations.
  // If compressed class space is inactive, this contains all metaspace allocations.
  static MetaspaceContext* context_nonclass()     { return _nonclass_space_context; }

  // Returns pointer to the global class space context, if compressed class space is active,
  // NULL otherwise.
  static MetaspaceContext* context_class()        { return _class_space_context; }

};

} // end namespace

#endif // SHARE_MEMORY_METASPACE_METASPACECONTEXT_HPP

