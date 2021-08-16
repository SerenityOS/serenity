/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/os.hpp"
#include "runtime/vm_version.hpp"

# include <sys/utsname.h>

// Use uname() to find the architecture version
void VM_Version::get_os_cpu_info() {
  struct utsname name;
  static bool done = false;

  // Support for multiple calls in the init phase
  if (done) return;
  done = true;

  uname(&name);
  if (strncmp(name.machine, "aarch64", 7) == 0) {
    _arm_arch = 8;
  } else if (strncmp(name.machine, "armv", 4) == 0 &&
      name.machine[4] >= '5' && name.machine[4] <= '9') {
    _arm_arch = (int)(name.machine[4] - '0');
  }
}

