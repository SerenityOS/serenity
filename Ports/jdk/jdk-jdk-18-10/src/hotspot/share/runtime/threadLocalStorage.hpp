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

#ifndef SHARE_RUNTIME_THREADLOCALSTORAGE_HPP
#define SHARE_RUNTIME_THREADLOCALSTORAGE_HPP

#include "memory/allocation.hpp"

// forward-decl as we can't have an include cycle
class Thread;

// Wrapper class for library-based (as opposed to compiler-based)
// thread-local storage (TLS). All platforms require this for
// signal-handler based TLS access (which while not strictly async-signal
// safe in theory, is and has-been for a long time, in practice).
// Platforms without compiler-based TLS (i.e. __thread storage-class modifier)
// will use this implementation for all TLS access - see thread.hpp/cpp

class ThreadLocalStorage : AllStatic {

 // Exported API
 public:
  static Thread* thread(); // return current thread, if attached
  static void    set_thread(Thread* thread); // set current thread
  static void    init();
  static bool    is_initialized(); // can't use TLS prior to initialization
};

#endif // SHARE_RUNTIME_THREADLOCALSTORAGE_HPP
