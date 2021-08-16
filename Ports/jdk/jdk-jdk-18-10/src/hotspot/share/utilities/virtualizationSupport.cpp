/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019 SAP SE. All rights reserved.
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
#include "runtime/globals.hpp"
#include "runtime/os.hpp"
#include "utilities/virtualizationSupport.hpp"

static void *dlHandle = NULL;

static GuestLib_StatGet_t GuestLib_StatGet = NULL;
static GuestLib_StatFree_t GuestLib_StatFree = NULL;

static bool has_host_information = false;
static bool has_resource_information = false;

// host + resource information; avoid the session and other special info vectors
static char host_information[300];
static char extended_resource_info_at_startup[600];

void VirtualizationSupport::initialize() {
  if (!ExtensiveErrorReports) return;

  // open vmguestlib and bind SDK functions
  char ebuf[1024];
  dlHandle = os::dll_load("vmGuestLib", ebuf, sizeof ebuf);

#ifdef LINUX
  if (dlHandle == NULL) {
    // the open-vm-tools have a different guest lib name
    // on some distros e.g. SLES12 the open-vm-tools are the default,
    // so use the different libname as a fallback
    dlHandle = os::dll_load("/usr/lib64/libguestlib.so.0", ebuf, sizeof ebuf);
  }
#endif
  if (dlHandle == NULL) {
    return;
  }

  GuestLib_StatGet = CAST_TO_FN_PTR(GuestLib_StatGet_t, os::dll_lookup(dlHandle, "VMGuestLib_StatGet"));
  GuestLib_StatFree = CAST_TO_FN_PTR(GuestLib_StatFree_t, os::dll_lookup(dlHandle, "VMGuestLib_StatFree"));

  if (GuestLib_StatGet != NULL && GuestLib_StatFree != NULL) {
    char* result_info = NULL;
    size_t result_size = 0;
    VMGuestLibError sg_error = GuestLib_StatGet("text", "resources", &result_info, &result_size);
    if (sg_error == VMGUESTLIB_ERROR_SUCCESS) {
      has_resource_information = true;
      os::snprintf(extended_resource_info_at_startup, sizeof(extended_resource_info_at_startup), "%s", result_info);
      GuestLib_StatFree(result_info, result_size);
    }
    sg_error = GuestLib_StatGet("text", "host", &result_info, &result_size);
    if (sg_error == VMGUESTLIB_ERROR_SUCCESS) {
      has_host_information = true;
      os::snprintf(host_information, sizeof(host_information), "%s", result_info);
      GuestLib_StatFree(result_info, result_size);
    }
  }
}

void VirtualizationSupport::print_virtualization_info(outputStream* st) {
  if (has_host_information) {
    st->print_cr("vSphere host information:");
    st->print_cr("%s", host_information);
  }
  // resource info at startup
  if (has_resource_information) {
    st->print_cr("vSphere resource information collected at VM startup:");
    st->print_cr("%s", extended_resource_info_at_startup);
  }
  // current resource info
  if (GuestLib_StatGet != NULL && GuestLib_StatFree != NULL) {
    char* result_info = NULL;
    size_t result_size = 0;
    VMGuestLibError sg_error = GuestLib_StatGet("text", "resources", &result_info, &result_size);
    if (sg_error == VMGUESTLIB_ERROR_SUCCESS) {
      st->print_cr("vSphere resource information available now:");
      st->print_cr("%s", result_info);
      GuestLib_StatFree(result_info, result_size);
    }
  }
}
