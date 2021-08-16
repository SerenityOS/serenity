/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "prims/jvmtiDeferredUpdates.hpp"

void JvmtiDeferredUpdates::create_for(JavaThread* thread) {
  assert(thread->deferred_updates() == NULL, "already allocated");
  thread->set_deferred_updates(new JvmtiDeferredUpdates());
}

JvmtiDeferredUpdates::~JvmtiDeferredUpdates() {
  while (_deferred_locals_updates.length() != 0) {
    jvmtiDeferredLocalVariableSet* dlv = _deferred_locals_updates.pop();
    // individual jvmtiDeferredLocalVariableSet are CHeapObj's
    delete dlv;
  }
}

void JvmtiDeferredUpdates::inc_relock_count_after_wait(JavaThread* thread) {
  if (thread->deferred_updates() == NULL) {
    create_for(thread);
  }
  thread->deferred_updates()->inc_relock_count_after_wait();
}

int JvmtiDeferredUpdates::get_and_reset_relock_count_after_wait(JavaThread* jt) {
  JvmtiDeferredUpdates* updates = jt->deferred_updates();
  int result = 0;
  if (updates != NULL) {
    result = updates->get_and_reset_relock_count_after_wait();
    if (updates->count() == 0) {
      delete updates;
      jt->set_deferred_updates(NULL);
    }
  }
  return result;
}

void JvmtiDeferredUpdates::delete_updates_for_frame(JavaThread* jt, intptr_t* frame_id) {
  JvmtiDeferredUpdates* updates = jt->deferred_updates();
  if (updates != NULL) {
    GrowableArray<jvmtiDeferredLocalVariableSet*>* list = updates->deferred_locals();
    assert(list->length() > 0, "Updates holder not deleted");
    int i = 0;
    do {
      // Because of inlining we could have multiple vframes for a single frame
      // and several of the vframes could have deferred writes. Find them all.
      jvmtiDeferredLocalVariableSet* dlv = list->at(i);
      if (dlv->id() == frame_id) {
        list->remove_at(i);
        // individual jvmtiDeferredLocalVariableSet are CHeapObj's
        delete dlv;
      } else {
        i++;
      }
    } while ( i < list->length() );
    if (updates->count() == 0) {
      jt->set_deferred_updates(NULL);
      // Free deferred updates.
      // Note, the 'list' of local variable updates is embedded in 'updates'.
      delete updates;
    }
  }
}
