/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/metaspaceClosure.hpp"

// Update the reference to point to new_loc.
void MetaspaceClosure::Ref::update(address new_loc) const {
  log_trace(cds)("Ref: [" PTR_FORMAT "] -> " PTR_FORMAT " => " PTR_FORMAT,
                 p2i(mpp()), p2i(obj()), p2i(new_loc));
  uintx p = (uintx)new_loc;
  p |= flag_bits(); // Make sure the flag bits are copied to the new pointer.
  *(address*)mpp() = (address)p;
}

void MetaspaceClosure::push_impl(MetaspaceClosure::Ref* ref) {
  if (_nest_level < MAX_NEST_LEVEL) {
    do_push(ref);
    if (!ref->keep_after_pushing()) {
      delete ref;
    }
  } else {
    do_pending_ref(ref);
    ref->set_next(_pending_refs);
    _pending_refs = ref;
  }
}

void MetaspaceClosure::do_push(MetaspaceClosure::Ref* ref) {
  if (ref->not_null()) {
    bool read_only;
    Writability w = ref->writability();
    switch (w) {
    case _writable:
      read_only = false;
      break;
    case _not_writable:
      read_only = true;
      break;
    default:
      assert(w == _default, "must be");
      read_only = ref->is_read_only_by_default();
    }
    if (_nest_level == 0) {
      assert(_enclosing_ref == NULL, "must be");
    }
    _nest_level ++;
    if (do_ref(ref, read_only)) { // true means we want to iterate the embedded pointer in <ref>
      Ref* saved = _enclosing_ref;
      _enclosing_ref = ref;
      ref->metaspace_pointers_do(this);
      _enclosing_ref = saved;
    }
    _nest_level --;
  }
}

void MetaspaceClosure::finish() {
  assert(_nest_level == 0, "must be");
  while (_pending_refs != NULL) {
    Ref* ref = _pending_refs;
    _pending_refs = _pending_refs->next();
    do_push(ref);
    if (!ref->keep_after_pushing()) {
      delete ref;
    }
  }
}

MetaspaceClosure::~MetaspaceClosure() {
  assert(_pending_refs == NULL,
         "you must explicitly call MetaspaceClosure::finish() to process all refs!");
}

bool UniqueMetaspaceClosure::do_ref(MetaspaceClosure::Ref* ref, bool read_only) {
  bool created;
  _has_been_visited.put_if_absent(ref->obj(), read_only, &created);
  if (!created) {
    return false; // Already visited: no need to iterate embedded pointers.
  } else {
    if (_has_been_visited.maybe_grow()) {
      log_info(cds, hashtables)("Expanded _has_been_visited table to %d", _has_been_visited.table_size());
    }
    return do_unique_ref(ref, read_only);
  }
}
