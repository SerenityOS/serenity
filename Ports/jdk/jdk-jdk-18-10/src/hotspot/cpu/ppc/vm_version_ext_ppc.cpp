/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "jvm.h"
#include "memory/allocation.hpp"
#include "memory/allocation.inline.hpp"
#include "runtime/vm_version.hpp"
#include "vm_version_ext_ppc.hpp"

// VM_Version_Ext statics
int   VM_Version_Ext::_no_of_threads = 0;
int   VM_Version_Ext::_no_of_cores = 0;
int   VM_Version_Ext::_no_of_sockets = 0;
bool  VM_Version_Ext::_initialized = false;
char  VM_Version_Ext::_cpu_name[CPU_TYPE_DESC_BUF_SIZE] = {0};
char  VM_Version_Ext::_cpu_desc[CPU_DETAILED_DESC_BUF_SIZE] = {0};

// get cpu information.
void VM_Version_Ext::initialize_cpu_information(void) {
  // do nothing if cpu info has been initialized
  if (_initialized) {
    return;
  }

  _no_of_cores  = os::processor_count();
  _no_of_threads = _no_of_cores;
  _no_of_sockets = _no_of_cores;
  snprintf(_cpu_name, CPU_TYPE_DESC_BUF_SIZE, "PowerPC POWER%lu", PowerArchitecturePPC64);
  snprintf(_cpu_desc, CPU_DETAILED_DESC_BUF_SIZE, "PPC %s", features_string());
  _initialized = true;
}

int VM_Version_Ext::number_of_threads(void) {
  initialize_cpu_information();
  return _no_of_threads;
}

int VM_Version_Ext::number_of_cores(void) {
  initialize_cpu_information();
  return _no_of_cores;
}

int VM_Version_Ext::number_of_sockets(void) {
  initialize_cpu_information();
  return _no_of_sockets;
}

const char* VM_Version_Ext::cpu_name(void) {
  initialize_cpu_information();
  char* tmp = NEW_C_HEAP_ARRAY_RETURN_NULL(char, CPU_TYPE_DESC_BUF_SIZE, mtTracing);
  if (NULL == tmp) {
    return NULL;
  }
  strncpy(tmp, _cpu_name, CPU_TYPE_DESC_BUF_SIZE);
  return tmp;
}

const char* VM_Version_Ext::cpu_description(void) {
  initialize_cpu_information();
  char* tmp = NEW_C_HEAP_ARRAY_RETURN_NULL(char, CPU_DETAILED_DESC_BUF_SIZE, mtTracing);
  if (NULL == tmp) {
    return NULL;
  }
  strncpy(tmp, _cpu_desc, CPU_DETAILED_DESC_BUF_SIZE);
  return tmp;
}
