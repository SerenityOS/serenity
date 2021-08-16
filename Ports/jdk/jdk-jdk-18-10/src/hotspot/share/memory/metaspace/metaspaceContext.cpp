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

#include "precompiled.hpp"
#include "memory/metaspace/chunkManager.hpp"
#include "memory/metaspace/commitLimiter.hpp"
#include "memory/metaspace/metaspaceContext.hpp"
#include "memory/metaspace/virtualSpaceList.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

namespace metaspace {

MetaspaceContext* MetaspaceContext::_class_space_context = NULL;
MetaspaceContext* MetaspaceContext::_nonclass_space_context = NULL;

// Destroys the context: deletes chunkmanager and virtualspacelist.
//  If this is a non-expandable context over an existing space, that space remains
//  untouched, otherwise all memory is unmapped.
// Note: the standard metaspace contexts (non-class context and class context) are
//  never deleted. This code only exists for the sake of tests and for future reuse
//  of metaspace contexts in different scenarios.
MetaspaceContext::~MetaspaceContext() {
  delete _cm;
  delete _vslist;
}

// Create a new, empty, expandable metaspace context.
MetaspaceContext* MetaspaceContext::create_expandable_context(const char* name, CommitLimiter* commit_limiter) {
  VirtualSpaceList* vsl = new VirtualSpaceList(name, commit_limiter);
  ChunkManager* cm = new ChunkManager(name, vsl);
  return new MetaspaceContext(name, vsl, cm);
}

// Create a new, empty, non-expandable metaspace context atop of an externally provided space.
MetaspaceContext* MetaspaceContext::create_nonexpandable_context(const char* name, ReservedSpace rs, CommitLimiter* commit_limiter) {
  VirtualSpaceList* vsl = new VirtualSpaceList(name, rs, commit_limiter);
  ChunkManager* cm = new ChunkManager(name, vsl);
  return new MetaspaceContext(name, vsl, cm);
}

void MetaspaceContext::initialize_class_space_context(ReservedSpace rs) {
  _class_space_context = create_nonexpandable_context("class-space", rs, CommitLimiter::globalLimiter());
}

void MetaspaceContext::initialize_nonclass_space_context() {
  _nonclass_space_context = create_expandable_context("non-class-space", CommitLimiter::globalLimiter());
}

void MetaspaceContext::print_on(outputStream* st) const {
  _vslist->print_on(st);
  _cm->print_on(st);
}

#ifdef ASSERT
void MetaspaceContext::verify() const {
  _vslist->verify();
  _cm->verify();
}
#endif // ASSERT

} // namespace metaspace

