/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Azul Systems, Inc. All rights reserved.
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

#ifndef SHARE_RUNTIME_THREADWXSETTERS_INLINE_HPP
#define SHARE_RUNTIME_THREADWXSETTERS_INLINE_HPP

// No threadWXSetters.hpp

#if defined(__APPLE__) && defined(AARCH64)

#include "runtime/thread.inline.hpp"

class ThreadWXEnable  {
  Thread* _thread;
  WXMode _old_mode;
public:
  ThreadWXEnable(WXMode new_mode, Thread* thread) :
    _thread(thread),
    _old_mode(_thread ? _thread->enable_wx(new_mode) : WXWrite)
  { }
  ~ThreadWXEnable() {
    if (_thread) {
      _thread->enable_wx(_old_mode);
    }
  }
};
#endif // __APPLE__ && AARCH64

#endif // SHARE_RUNTIME_THREADWXSETTERS_INLINE_HPP

