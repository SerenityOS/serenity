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

#include "precompiled.hpp"
#include "classfile/metadataOnStackMark.hpp"
#include "code/codeCache.hpp"
#include "compiler/compileBroker.hpp"
#include "oops/metadata.hpp"
#include "prims/jvmtiImpl.hpp"
#include "runtime/synchronizer.hpp"
#include "runtime/thread.hpp"
#include "services/threadService.hpp"
#include "utilities/chunkedList.hpp"
#if INCLUDE_JVMCI
#include "jvmci/jvmci.hpp"
#endif

MetadataOnStackBuffer* MetadataOnStackMark::_used_buffers = NULL;
MetadataOnStackBuffer* MetadataOnStackMark::_free_buffers = NULL;

MetadataOnStackBuffer* MetadataOnStackMark::_current_buffer = NULL;
NOT_PRODUCT(bool MetadataOnStackMark::_is_active = false;)

class MetadataOnStackClosure : public MetadataClosure {
  void do_metadata(Metadata* m) { Metadata::mark_on_stack(m); }
};

// Walk metadata on the stack and mark it so that redefinition doesn't delete
// it.  Class unloading only deletes in-error class files, methods created by
// the relocator and dummy constant pools.  None of these appear anywhere except
// in metadata Handles.
MetadataOnStackMark::MetadataOnStackMark(bool walk_all_metadata, bool redefinition_walk) {
  assert(SafepointSynchronize::is_at_safepoint(), "sanity check");
  assert(_used_buffers == NULL, "sanity check");
  assert(!_is_active, "MetadataOnStackMarks do not nest");
  assert(!redefinition_walk || walk_all_metadata,
         "walk_all_metadata must be true for redefinition_walk");
  NOT_PRODUCT(_is_active = true;)

  Threads::metadata_handles_do(Metadata::mark_on_stack);

  if (walk_all_metadata) {
    MetadataOnStackClosure md_on_stack;
    Threads::metadata_do(&md_on_stack);
    if (redefinition_walk) {
      // We have to walk the whole code cache during redefinition.
      CodeCache::metadata_do(&md_on_stack);
    } else {
      CodeCache::old_nmethods_do(&md_on_stack);
    }
    CompileBroker::mark_on_stack();
    ThreadService::metadata_do(Metadata::mark_on_stack);
#if INCLUDE_JVMCI
    JVMCI::metadata_do(Metadata::mark_on_stack);
#endif
  }
}

MetadataOnStackMark::~MetadataOnStackMark() {
  assert(SafepointSynchronize::is_at_safepoint(), "sanity check");
  // Unmark everything that was marked.   Can't do the same walk because
  // redefine classes messes up the code cache so the set of methods
  // might not be the same.
  retire_current_buffer();

  MetadataOnStackBuffer* buffer = _used_buffers;
  while (buffer != NULL) {
    // Clear on stack state for all metadata.
    size_t size = buffer->size();
    for (size_t i  = 0; i < size; i++) {
      Metadata* md = buffer->at(i);
      md->set_on_stack(false);
    }

    MetadataOnStackBuffer* next = buffer->next_used();

    // Move the buffer to the free list.
    buffer->clear();
    buffer->set_next_used(NULL);
    buffer->set_next_free(_free_buffers);
    _free_buffers = buffer;

    // Step to next used buffer.
    buffer = next;
  }

  _used_buffers = NULL;

  NOT_PRODUCT(_is_active = false;)
}

void MetadataOnStackMark::retire_buffer(MetadataOnStackBuffer* buffer) {
  if (buffer == NULL) {
    return;
  }
  buffer->set_next_used(_used_buffers);
  _used_buffers = buffer;
}

// Current buffer is full or we're ready to walk them, add it to the used list.
void MetadataOnStackMark::retire_current_buffer() {
  retire_buffer(_current_buffer);
  _current_buffer = NULL;
}

// Get buffer off free list.
MetadataOnStackBuffer* MetadataOnStackMark::allocate_buffer() {
  MetadataOnStackBuffer* allocated = _free_buffers;

  if (allocated != NULL) {
    _free_buffers = allocated->next_free();
  }

  if (allocated == NULL) {
    allocated = new MetadataOnStackBuffer();
  }

  assert(!allocated->is_full(), "Should not be full: " PTR_FORMAT, p2i(allocated));

  return allocated;
}

// Record which objects are marked so we can unmark the same objects.
void MetadataOnStackMark::record(Metadata* m) {
  assert(_is_active, "metadata on stack marking is active");

  MetadataOnStackBuffer* buffer = _current_buffer;

  if (buffer != NULL && buffer->is_full()) {
    retire_buffer(buffer);
    buffer = NULL;
  }

  if (buffer == NULL) {
    buffer = allocate_buffer();
    _current_buffer = buffer;
  }

  buffer->push(m);
}
