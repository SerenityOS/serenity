/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "cds/cdsoffsets.hpp"
#include "cds/dynamicArchive.hpp"
#include "cds/filemap.hpp"
#include "runtime/os.hpp"
#include "memory/allocation.hpp"
#include "memory/allocation.inline.hpp"
#include "utilities/macros.hpp"

CDSOffsets::CDSOffsets(const char* name, int offset, CDSOffsets* next) {
  _name = NEW_C_HEAP_ARRAY(char, strlen(name) + 1, mtInternal);
  strcpy(_name, name);
  _offset = offset;
  _next = next;
}

CDSOffsets* CDSOffsets::_all = NULL;
#define ADD_NEXT(list, name, value) \
  list->add_end(new CDSOffsets(name, value, NULL))

#define CREATE_OFFSET_MAPS                                                                  \
    _all = new CDSOffsets("size_t_size", sizeof(size_t), NULL);                             \
    ADD_NEXT(_all, "int_size", sizeof(int));                                                \
    ADD_NEXT(_all, "FileMapHeader::_magic", offset_of(FileMapHeader, _magic));              \
    ADD_NEXT(_all, "FileMapHeader::_crc", offset_of(FileMapHeader, _crc));                  \
    ADD_NEXT(_all, "FileMapHeader::_version", offset_of(FileMapHeader, _version));          \
    ADD_NEXT(_all, "FileMapHeader::_jvm_ident", offset_of(FileMapHeader, _jvm_ident));      \
    ADD_NEXT(_all, "FileMapHeader::_space[0]", offset_of(FileMapHeader, _space));           \
    ADD_NEXT(_all, "CDSFileMapRegion::_crc", offset_of(CDSFileMapRegion, _crc));            \
    ADD_NEXT(_all, "CDSFileMapRegion::_used", offset_of(CDSFileMapRegion, _used));          \
    ADD_NEXT(_all, "file_header_size", sizeof(FileMapHeader));                              \
    ADD_NEXT(_all, "DynamicArchiveHeader::_base_region_crc", offset_of(DynamicArchiveHeader, _base_region_crc)); \
    ADD_NEXT(_all, "CDSFileMapRegion_size", sizeof(CDSFileMapRegion));

int CDSOffsets::find_offset(const char* name) {
  if (_all == NULL) {
    CREATE_OFFSET_MAPS
  }
  CDSOffsets* it = _all;
  while(it) {
    if (!strcmp(name, it->get_name())) {
      return it->get_offset();
    }
    it = it->next();
  }
  return -1; // not found
}

void CDSOffsets::add_end(CDSOffsets* n) {
  CDSOffsets* p = this;
  while(p && p->_next) { p = p->_next; }
  p->_next = n;
}
