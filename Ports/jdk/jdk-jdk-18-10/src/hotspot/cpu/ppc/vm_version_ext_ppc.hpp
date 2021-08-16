/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_PPC_VM_VERSION_EXT_PPC_HPP
#define CPU_PPC_VM_VERSION_EXT_PPC_HPP

#include "runtime/vm_version.hpp"
#include "utilities/macros.hpp"

#define CPU_INFO        "cpu_info"
#define CPU_TYPE        "fpu_type"
#define CPU_DESCRIPTION "implementation"
#define CHIP_ID         "chip_id"
#define CORE_ID         "core_id"

class VM_Version_Ext : public VM_Version {
 private:

  static const size_t      CPU_TYPE_DESC_BUF_SIZE = 256;
  static const size_t      CPU_DETAILED_DESC_BUF_SIZE = 4096;

  static int               _no_of_threads;
  static int               _no_of_cores;
  static int               _no_of_sockets;
  static bool              _initialized;
  static char              _cpu_name[CPU_TYPE_DESC_BUF_SIZE];
  static char              _cpu_desc[CPU_DETAILED_DESC_BUF_SIZE];

  static void initialize_cpu_information(void);

 public:

  static int number_of_threads(void);
  static int number_of_cores(void);
  static int number_of_sockets(void);

  static const char* cpu_name(void);
  static const char* cpu_description(void);
};

#endif // CPU_PPC_VM_VERSION_EXT_PPC_HPP
