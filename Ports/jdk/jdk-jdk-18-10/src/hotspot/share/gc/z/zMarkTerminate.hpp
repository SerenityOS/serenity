/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef SHARE_GC_Z_ZMARKTERMINATE_HPP
#define SHARE_GC_Z_ZMARKTERMINATE_HPP

#include "gc/z/zGlobals.hpp"
#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

class ZMarkTerminate {
private:
  uint                         _nworkers;
  ZCACHE_ALIGNED volatile uint _nworking_stage0;
  volatile uint                _nworking_stage1;

  bool enter_stage(volatile uint* nworking_stage);
  void exit_stage(volatile uint* nworking_stage);
  bool try_exit_stage(volatile uint* nworking_stage);

public:
  ZMarkTerminate();

  void reset(uint nworkers);

  bool enter_stage0();
  void exit_stage0();
  bool try_exit_stage0();

  bool enter_stage1();
  bool try_exit_stage1();
};

#endif // SHARE_GC_Z_ZMARKTERMINATE_HPP
