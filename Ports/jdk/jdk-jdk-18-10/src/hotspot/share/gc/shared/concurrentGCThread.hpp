/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_CONCURRENTGCTHREAD_HPP
#define SHARE_GC_SHARED_CONCURRENTGCTHREAD_HPP

#include "runtime/nonJavaThread.hpp"
#include "runtime/thread.hpp"

class ConcurrentGCThread: public NamedThread {
private:
  volatile bool _should_terminate;
  volatile bool _has_terminated;

protected:
  void create_and_start(ThreadPriority prio = NearMaxPriority);

  virtual void run_service() = 0;
  virtual void stop_service() = 0;

public:
  ConcurrentGCThread();

  virtual bool is_ConcurrentGC_thread() const { return true; }

  virtual void run();
  virtual void stop();

  bool should_terminate() const;
  bool has_terminated() const;

  // Printing
  const char* type_name() const { return "ConcurrentGCThread"; }
};

#endif // SHARE_GC_SHARED_CONCURRENTGCTHREAD_HPP
