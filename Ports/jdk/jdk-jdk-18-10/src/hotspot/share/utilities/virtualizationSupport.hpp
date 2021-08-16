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

#ifndef SHARE_UTILITIES_VIRTUALIZATIONSUPPORT_HPP
#define SHARE_UTILITIES_VIRTUALIZATIONSUPPORT_HPP

#include "utilities/ostream.hpp"

typedef enum {
  VMGUESTLIB_ERROR_SUCCESS = 0, // no error occured
  VMGUESTLIB_ERROR_OTHER,
  VMGUESTLIB_ERROR_NOT_RUNNING_IN_VM,
  VMGUESTLIB_ERROR_NOT_ENABLED,
  VMGUESTLIB_ERROR_NOT_AVAILABLE,
  VMGUESTLIB_ERROR_NO_INFO,
  VMGUESTLIB_ERROR_MEMORY,
  VMGUESTLIB_ERROR_BUFFER_TOO_SMALL,
  VMGUESTLIB_ERROR_INVALID_HANDLE,
  VMGUESTLIB_ERROR_INVALID_ARG,
  VMGUESTLIB_ERROR_UNSUPPORTED_VERSION
} VMGuestLibError;

// new SDK functions from VMWare SDK 6.0; need VMware Tools version 9.10 installed
typedef VMGuestLibError (*GuestLib_StatGet_t)(const char*, const char*, char**, size_t*);
typedef VMGuestLibError (*GuestLib_StatFree_t)(char*, size_t);

class VirtualizationSupport {
 public:
  static void initialize();
  static void print_virtualization_info(outputStream* st);
};

#endif

