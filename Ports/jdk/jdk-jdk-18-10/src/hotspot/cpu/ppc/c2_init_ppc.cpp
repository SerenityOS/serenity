/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2020 SAP SE. All rights reserved.
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
#include "opto/compile.hpp"
#include "opto/node.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/debug.hpp"

// Processor dependent initialization of C2 compiler for ppc.

void Compile::pd_compiler2_init() {

  // Power7 and later.
  if (PowerArchitecturePPC64 > 6) {
    if (FLAG_IS_DEFAULT(UsePopCountInstruction)) {
      FLAG_SET_ERGO(UsePopCountInstruction, true);
    }
  }

  if (!VM_Version::has_isel() && FLAG_IS_DEFAULT(ConditionalMoveLimit)) {
    FLAG_SET_ERGO(ConditionalMoveLimit, 0);
  }

  if (OptimizeFill) {
    warning("OptimizeFill is not supported on this CPU.");
    FLAG_SET_DEFAULT(OptimizeFill, false);
  }

}
