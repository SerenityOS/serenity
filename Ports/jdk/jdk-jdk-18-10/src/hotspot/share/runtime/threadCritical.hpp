/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_THREADCRITICAL_HPP
#define SHARE_RUNTIME_THREADCRITICAL_HPP

#include "memory/allocation.hpp"

// ThreadCritical is used to protect short non-blocking critical sections.
// This class must use no vm facilities that require initialization.
// It is used very early in the vm's initialization, in allocation
// code and other areas. ThreadCritical regions are reentrant.
//
// Due to race conditions during vm exit, some of the os level
// synchronization primitives may not be deallocated at exit. It
// is a good plan to implement the platform dependent sections of
// code with resources that are recoverable during process
// cleanup by the os. Calling the initialize method before use
// is also problematic, it is best to use preinitialized primitives
// if possible. As an example:
//
// mutex_t  mp  =  DEFAULTMUTEX;
//
// Also note that this class is declared as a StackObj to enforce
// block structured short locks. It cannot be declared a ResourceObj
// or CHeapObj, due to initialization issues.

class ThreadCritical : public StackObj {
 public:
  ThreadCritical();
  ~ThreadCritical();
};

#endif // SHARE_RUNTIME_THREADCRITICAL_HPP
