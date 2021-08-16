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

#include "libodm_aix.hpp"
#include "misc_aix.hpp"
#include <stdlib.h>
#include <dlfcn.h>
#include <string.h>
#include "runtime/arguments.hpp"


dynamicOdm::dynamicOdm() {
  const char *libodmname = "/usr/lib/libodm.a(shr_64.o)";
  _libhandle = dlopen(libodmname, RTLD_MEMBER | RTLD_NOW);
  if (!_libhandle) {
    trcVerbose("Couldn't open %s", libodmname);
    return;
  }
  _odm_initialize  = (fun_odm_initialize )dlsym(_libhandle, "odm_initialize" );
  _odm_set_path    = (fun_odm_set_path   )dlsym(_libhandle, "odm_set_path"   );
  _odm_mount_class = (fun_odm_mount_class)dlsym(_libhandle, "odm_mount_class");
  _odm_get_obj     = (fun_odm_get_obj    )dlsym(_libhandle, "odm_get_obj"    );
  _odm_terminate   = (fun_odm_terminate  )dlsym(_libhandle, "odm_terminate"  );
  if (!_odm_initialize || !_odm_set_path || !_odm_mount_class || !_odm_get_obj || !_odm_terminate) {
    trcVerbose("Couldn't find all required odm symbols from %s", libodmname);
    dlclose(_libhandle);
    _libhandle = NULL;
    return;
  }
}

dynamicOdm::~dynamicOdm() {
  if (_libhandle) { dlclose(_libhandle); }
}


void odmWrapper::clean_data() { if (_data) { free(_data); _data = NULL; } }


int odmWrapper::class_offset(const char *field, bool is_aix_5)
{
  assert(has_class(), "initialization");
  for (int i = 0; i < odm_class()->nelem; i++) {
    if (strcmp(odm_class()->elem[i].elemname, field) == 0) {
      int offset = odm_class()->elem[i].offset;
      if (is_aix_5) { offset += LINK_VAL_OFFSET; }
      return offset;
    }
  }
  return -1;
}


void odmWrapper::determine_os_kernel_version(uint32_t* p_ver) {
  int major_aix_version = ((*p_ver) >> 24) & 0xFF,
      minor_aix_version = ((*p_ver) >> 16) & 0xFF;
  assert(*p_ver, "must be initialized");

  odmWrapper odm("product", "/usr/lib/objrepos"); // could also use "lpp"
  if (!odm.has_class()) {
    trcVerbose("try_determine_os_kernel_version: odm init problem");
    return;
  }
  int voff, roff, moff, foff;
  bool is_aix_5 = (major_aix_version == 5);
  voff = odm.class_offset("ver", is_aix_5);
  roff = odm.class_offset("rel", is_aix_5);
  moff = odm.class_offset("mod", is_aix_5);
  foff = odm.class_offset("fix", is_aix_5);
  if (voff == -1 || roff == -1 || moff == -1 || foff == -1) {
    trcVerbose("try_determine_os_kernel_version: could not get offsets");
    return;
  }
  if (!odm.retrieve_obj("name='bos.mp64'")) {
    trcVerbose("try_determine_os_kernel_version: odm_get_obj failed");
    return;
  }
  int version, release, modification, fix_level;
  do {
    version      = odm.read_short(voff);
    release      = odm.read_short(roff);
    modification = odm.read_short(moff);
    fix_level    = odm.read_short(foff);
    trcVerbose("odm found version: %d.%d.%d.%d", version, release, modification, fix_level);
    if (version >> 8 != 0 || release >> 8 != 0 || modification >> 8 != 0 || fix_level >> 8 != 0) {
      trcVerbose("8 bit numbers expected");
      return;
    }
  } while (odm.retrieve_obj());

  if (version != major_aix_version || release != minor_aix_version) {
    trcVerbose("version determined by odm does not match uname");
    return;
  }
  *p_ver = version << 24 | release << 16 | modification << 8 | fix_level;
}
