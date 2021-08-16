/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "code/codeBehaviours.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/safepoint.hpp"

CompiledICProtectionBehaviour* CompiledICProtectionBehaviour::_current = NULL;

bool DefaultICProtectionBehaviour::lock(CompiledMethod* method) {
  if (is_safe(method)) {
    return false;
  }
  CompiledIC_lock->lock_without_safepoint_check();
  return true;
}

void DefaultICProtectionBehaviour::unlock(CompiledMethod* method) {
  CompiledIC_lock->unlock();
}

bool DefaultICProtectionBehaviour::is_safe(CompiledMethod* method) {
  return SafepointSynchronize::is_at_safepoint() || CompiledIC_lock->owned_by_self();
}
