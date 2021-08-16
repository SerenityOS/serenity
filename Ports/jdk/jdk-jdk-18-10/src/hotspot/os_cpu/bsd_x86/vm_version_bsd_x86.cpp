/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifdef __APPLE__

#include <sys/types.h>
#include <sys/sysctl.h>

bool VM_Version::is_cpu_emulated() {
  int ret = 0;
  size_t size = sizeof(ret);
  // Is this process being ran in Rosetta (i.e. emulation) mode on macOS?
  if (sysctlbyname("sysctl.proc_translated", &ret, &size, NULL, 0) == -1) {
    // errno == ENOENT is a valid response, but anything else is a real error
    if (errno != ENOENT) {
      warning("unable to lookup sysctl.proc_translated");
    }
  }
  return (ret==1);
}

#endif

