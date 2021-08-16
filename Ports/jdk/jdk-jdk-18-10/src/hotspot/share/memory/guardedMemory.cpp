/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/guardedMemory.hpp"
#include "runtime/os.hpp"

void* GuardedMemory::wrap_copy(const void* ptr, const size_t len, const void* tag) {
  size_t total_sz = GuardedMemory::get_total_size(len);
  void* outerp = os::malloc(total_sz, mtInternal);
  if (outerp != NULL) {
    GuardedMemory guarded(outerp, len, tag);
    void* innerp = guarded.get_user_ptr();
    memcpy(innerp, ptr, len);
    return innerp;
  }
  return NULL; // OOM
}

bool GuardedMemory::free_copy(void* p) {
  if (p == NULL) {
    return true;
  }
  GuardedMemory guarded((u_char*)p);
  bool verify_ok = guarded.verify_guards();

  /* always attempt to free, pass problem on to any nested memchecker */
  os::free(guarded.release_for_freeing());

  return verify_ok;
}

void GuardedMemory::print_on(outputStream* st) const {
  if (_base_addr == NULL) {
    st->print_cr("GuardedMemory(" PTR_FORMAT ") not associated to any memory", p2i(this));
    return;
  }
  st->print_cr("GuardedMemory(" PTR_FORMAT ") base_addr=" PTR_FORMAT
      " tag=" PTR_FORMAT " user_size=" SIZE_FORMAT " user_data=" PTR_FORMAT,
      p2i(this), p2i(_base_addr), p2i(get_tag()), get_user_size(), p2i(get_user_ptr()));

  Guard* guard = get_head_guard();
  st->print_cr("  Header guard @" PTR_FORMAT " is %s", p2i(guard), (guard->verify() ? "OK" : "BROKEN"));
  guard = get_tail_guard();
  st->print_cr("  Trailer guard @" PTR_FORMAT " is %s", p2i(guard), (guard->verify() ? "OK" : "BROKEN"));

  u_char udata = *get_user_ptr();
  switch (udata) {
  case uninitBlockPad:
    st->print_cr("  User data appears unused");
    break;
  case freeBlockPad:
    st->print_cr("  User data appears to have been freed");
    break;
  default:
    st->print_cr("  User data appears to be in use");
    break;
  }
}
