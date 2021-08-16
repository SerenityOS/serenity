/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "classfile/classLoaderData.hpp"
#include "jvmci/metadataHandles.hpp"
#include "runtime/atomic.hpp"

jmetadata MetadataHandles::allocate_metadata_handle(Metadata* obj) {
  assert(obj->is_valid() && obj->is_metadata(), "must be");

  if (_head == NULL) {
    // This is the first allocation.
    _head = new MetadataHandleBlock();
    _last = _head;
    _num_blocks++;
  }

  HandleRecord* handle = get_handle();

  if (handle != NULL) {
    handle->set_value(obj);
#ifdef METADATA_TRACK_NAMES
    handle->set_name(obj->print_value_string());
#endif
    return (jmetadata) handle;
  }

  // Check if an unused block follows last
  if (_last->_next != NULL) {
    // update last and retry
    _last = _last->_next;
    return allocate_metadata_handle(obj);
  }

  // No space available, we have to rebuild free list or expand
  if (_allocate_before_rebuild == 0) {
    rebuild_free_list(); // updates _allocate_before_rebuild counter
  } else {
    // Append new block
    _last->_next = new MetadataHandleBlock();
    _last = _last->_next;
    _allocate_before_rebuild--;
    _num_blocks++;
  }
  return allocate_metadata_handle(obj);  // retry
}


void MetadataHandles::rebuild_free_list() {
  assert(_allocate_before_rebuild == 0 && _free_list == 0, "just checking");
  int free = 0;
  int blocks = 0;
  for (MetadataHandleBlock* current = _head; current != NULL; current = current->_next) {
    for (int index = 0; index < current->_top; index++) {
      HandleRecord* handle = &(current->_handles)[index];
      if (handle->value() == NULL) {
        // this handle was cleared out by a delete call, reuse it
        chain_free_list(handle);
        free++;
      }
    }
    // we should not rebuild free list if there are unused handles at the end
    assert(current->_top == MetadataHandleBlock::block_size_in_handles, "just checking");
    blocks++;
  }
  assert(_num_blocks == blocks, "%d != %d", _num_blocks, blocks);
  assert(_num_free_handles == free, "%d != %d", _num_free_handles, free);
  // Heuristic: if more than half of the handles are NOT free we rebuild next time
  // as well, otherwise we append a corresponding number of new blocks before
  // attempting a free list rebuild again.
  int total = blocks * MetadataHandleBlock::block_size_in_handles;
  int extra = total - 2*free;
  if (extra > 0) {
    // Not as many free handles as we would like - compute number of new blocks to append
    _allocate_before_rebuild = (extra + MetadataHandleBlock::block_size_in_handles - 1) / MetadataHandleBlock::block_size_in_handles;
  }
}

void MetadataHandles::clear() {
  _free_list = 0;
  _last = _head;
  if (_head != NULL) {
    for (MetadataHandleBlock* block = _head; block != NULL; block = block->_next) {
      block->_top = 0;
    }
  }
  _num_handles = 0;
  _num_free_handles = 0;
}

void MetadataHandles::metadata_do(void f(Metadata*)) {
  for (MetadataHandleBlock* current = _head; current != NULL; current = current->_next) {
    for (int index = 0; index < current->_top; index++) {
      HandleRecord* root = &(current->_handles)[index];
      Metadata* value = root->value();
      // traverse heap pointers only, not deleted handles or free list
      // pointers
      if (value != NULL && ((intptr_t) value & ptr_tag) == 0) {
        assert(value->is_valid(), "invalid metadata %s", current->get_name(index));
        f(value);
      }
    }
    // the next handle block is valid only if current block is full
    if (current->_top < MetadataHandleBlock::block_size_in_handles) {
      break;
    }
  }
}

// Visit any live metadata handles and clean them up.  Since clearing of these handles is driven by
// weak references they will be cleared at some point in the future when the reference cleaning logic is run.
void MetadataHandles::do_unloading() {
  for (MetadataHandleBlock* current = _head; current != NULL; current = current->_next) {
    for (int index = 0; index < current->_top; index++) {
      HandleRecord* handle = &(current->_handles)[index];
      Metadata* value = handle->value();
      // traverse heap pointers only, not deleted handles or free list
      // pointers
      if (value != NULL && ((intptr_t) value & ptr_tag) == 0) {
        Klass* klass = NULL;
        if (value->is_klass()) {
          klass = (Klass*)value;
        } else if (value->is_method()) {
          Method* m = (Method*)value;
          klass = m->method_holder();
        } else if (value->is_constantPool()) {
          ConstantPool* cp = (ConstantPool*)value;
          klass = cp->pool_holder();
        } else {
          ShouldNotReachHere();
        }
        if (klass->class_loader_data()->is_unloading()) {
          // This needs to be marked so that it's no longer scanned
          // but can't be put on the free list yet. The
          // HandleCleaner will set this to NULL and
          // put it on the free list.
          jlong old_value = Atomic::cmpxchg((jlong*)handle, (jlong) value, (jlong) (ptr_tag));
          if (old_value == (jlong) value) {
            // Success
          } else {
            guarantee(old_value == 0, "only other possible value");
          }
        }
      }
    }
    // the next handle block is valid only if current block is full
    if (current->_top < MetadataHandleBlock::block_size_in_handles) {
      break;
    }
  }
}
