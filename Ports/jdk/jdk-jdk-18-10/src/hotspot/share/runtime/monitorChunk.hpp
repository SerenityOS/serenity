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

#ifndef SHARE_RUNTIME_MONITORCHUNK_HPP
#define SHARE_RUNTIME_MONITORCHUNK_HPP

#include "runtime/synchronizer.hpp"

// Data structure for holding monitors for one activation during
// deoptimization.

class MonitorChunk: public CHeapObj<mtSynchronizer> {
 private:
  int              _number_of_monitors;
  BasicObjectLock* _monitors;
  BasicObjectLock* monitors() const { return _monitors; }
  MonitorChunk*    _next;
 public:
  // Constructor
  MonitorChunk(int number_on_monitors);
  ~MonitorChunk();

  // link operations
  MonitorChunk* next() const                { return _next; }
  void set_next(MonitorChunk* next)         { _next = next; }

  // Tells whether the monitor chunk is linked into the JavaThread
  bool is_linked() const                    { return next() != NULL; }

  // Returns the number of monitors
  int number_of_monitors() const { return _number_of_monitors; }

  // Returns the index'th monitor
  BasicObjectLock* at(int index)            { assert(index >= 0 && index < number_of_monitors(), "out of bounds check"); return &monitors()[index]; }


  // Memory management
  void oops_do(OopClosure* f);

  // Tells whether the addr point into the monitors.
  bool contains(void* addr) const           { return (addr >= (void*) monitors()) && (addr <  (void*) (monitors() + number_of_monitors())); }
};

#endif // SHARE_RUNTIME_MONITORCHUNK_HPP
