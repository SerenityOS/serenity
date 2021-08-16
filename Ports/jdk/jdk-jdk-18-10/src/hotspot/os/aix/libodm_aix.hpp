/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2015, 2019 SAP SE. All rights reserved.
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

// Encapsulates the libodm library and provides more convenient interfaces.

#ifndef OS_AIX_LIBODM_AIX_HPP
#define OS_AIX_LIBODM_AIX_HPP

#include <odmi.h>


// The purpose of this code is to dynamically load the libodm library
// instead of statically linking against it. The library is AIX-specific.
// It only exists on AIX, not on PASE. In order to share binaries
// between AIX and PASE, we can't directly link against it.

typedef int          (*fun_odm_initialize )(void);
typedef char*        (*fun_odm_set_path   )(char*);
typedef CLASS_SYMBOL (*fun_odm_mount_class)(char*);
typedef void*        (*fun_odm_get_obj    )(CLASS_SYMBOL, char*, void*, int);
typedef int          (*fun_odm_terminate  )(void);

class dynamicOdm {
  void *_libhandle;
 protected:
  fun_odm_initialize  _odm_initialize;
  fun_odm_set_path    _odm_set_path;
  fun_odm_mount_class _odm_mount_class;
  fun_odm_get_obj     _odm_get_obj;
  fun_odm_terminate   _odm_terminate;
 public:
  dynamicOdm();
  ~dynamicOdm();
  bool odm_loaded() {return _libhandle != NULL; }
};


// We provide a more convenient interface for odm access and
// especially to determine the exact AIX kernel version.

class odmWrapper : private dynamicOdm {
  CLASS_SYMBOL _odm_class;
  char *_data;
  bool _initialized;
  void clean_data();

 public:
  // Make sure everything gets initialized and cleaned up properly.
  explicit odmWrapper(const char* odm_class_name, const char* odm_path = NULL) : _odm_class((CLASS_SYMBOL)-1),
                                                                     _data(NULL), _initialized(false) {
    if (!odm_loaded()) { return; }
    _initialized = ((*_odm_initialize)() != -1);
    if (_initialized) {
      // should we free what odm_set_path returns, man page suggests it
      // see https://www.ibm.com/support/knowledgecenter/en/ssw_aix_71/o_bostechref/odm_set_path.html
      if (odm_path) { (*_odm_set_path)((char*)odm_path); }
      _odm_class = (*_odm_mount_class)((char*)odm_class_name);
    }
  }
  ~odmWrapper() {
    if (_initialized) { (*_odm_terminate)(); clean_data(); }
  }

  CLASS_SYMBOL odm_class() { return _odm_class; }
  bool has_class() { return odm_class() != (CLASS_SYMBOL)-1; }
  int class_offset(const char *field, bool is_aix_5);
  char* data() { return _data; }

  char* retrieve_obj(const char* name = NULL) {
    clean_data();
    char *cnp = (char*)(void*)(*_odm_get_obj)(odm_class(), (char*) name, NULL, (name == NULL) ? ODM_NEXT : ODM_FIRST);
    if (cnp != (char*)-1) { _data = cnp; }
    return data();
  }

  int read_short(int offs) {
    short *addr = (short*)(data() + offs);
    return *addr;
  }

  // Determine the exact AIX kernel version as 4 byte value.
  // The high order 2 bytes must be initialized already. They can be determined by uname.
  static void determine_os_kernel_version(uint32_t* p_ver);
};

#endif // OS_AIX_LIBODM_AIX_HPP
