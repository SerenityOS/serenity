/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_SAFEPOINTVERIFIERS_HPP
#define SHARE_RUNTIME_SAFEPOINTVERIFIERS_HPP

#include "memory/allocation.hpp"
#include "runtime/thread.hpp"

// A NoSafepointVerifier object will throw an assertion failure if
// the current thread passes a possible safepoint while this object is
// instantiated. A safepoint, will either be: an oop allocation, blocking
// on a Mutex or JavaLock, or executing a VM operation.
//
class NoSafepointVerifier : public StackObj {
 friend class PauseNoSafepointVerifier;

 private:
  Thread *_thread;
 public:
  NoSafepointVerifier()  NOT_DEBUG_RETURN;
  ~NoSafepointVerifier() NOT_DEBUG_RETURN;
};

// A PauseNoSafepointVerifier is used to temporarily pause the
// behavior of a NoSafepointVerifier object.

class PauseNoSafepointVerifier : public StackObj {
 private:
  NoSafepointVerifier* _nsv;

 public:
  PauseNoSafepointVerifier(NoSafepointVerifier* nsv) NOT_DEBUG_RETURN;
  ~PauseNoSafepointVerifier() NOT_DEBUG_RETURN;
};

#endif // SHARE_RUNTIME_SAFEPOINTVERIFIERS_HPP
