/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_MODE_SHENANDOAHMODE_HPP
#define SHARE_GC_SHENANDOAH_MODE_SHENANDOAHMODE_HPP

#include "memory/allocation.hpp"

class ShenandoahHeuristics;

#define SHENANDOAH_CHECK_FLAG_SET(name)                                     \
  do {                                                                      \
    if (!(name)) {                                                          \
      err_msg message("GC mode needs -XX:+" #name " to work correctly");    \
      vm_exit_during_initialization("Error", message);                      \
    }                                                                       \
  } while (0)

#define SHENANDOAH_CHECK_FLAG_UNSET(name)                                   \
  do {                                                                      \
    if ((name)) {                                                           \
      err_msg message("GC mode needs -XX:-" #name " to work correctly");    \
      vm_exit_during_initialization("Error", message);                      \
    }                                                                       \
  } while (0)

class ShenandoahMode : public CHeapObj<mtGC> {
public:
  virtual void initialize_flags() const = 0;
  virtual ShenandoahHeuristics* initialize_heuristics() const = 0;
  virtual const char* name() = 0;
  virtual bool is_diagnostic() = 0;
  virtual bool is_experimental() = 0;
};

#endif // SHARE_GC_SHENANDOAH_MODE_SHENANDOAHMODE_HPP
