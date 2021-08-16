/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CDS_DYNAMICARCHIVE_HPP
#define SHARE_CDS_DYNAMICARCHIVE_HPP

#include "cds/filemap.hpp"
#include "classfile/compactHashtable.hpp"
#include "memory/allocation.hpp"
#include "memory/memRegion.hpp"
#include "memory/virtualspace.hpp"
#include "oops/oop.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/macros.hpp"
#include "utilities/resourceHash.hpp"

#if INCLUDE_CDS

class DynamicArchiveHeader : public FileMapHeader {
  friend class CDSOffsets;
private:
  int _base_header_crc;
  int _base_region_crc[MetaspaceShared::n_regions];

public:
  int base_header_crc() const { return _base_header_crc; }
  int base_region_crc(int i) const {
    assert(is_valid_region(i), "must be");
    return _base_region_crc[i];
  }

  void set_base_header_crc(int c) { _base_header_crc = c; }
  void set_base_region_crc(int i, int c) {
    assert(is_valid_region(i), "must be");
    _base_region_crc[i] = c;
  }
};

class DynamicArchive : AllStatic {
public:
  static void prepare_for_dynamic_dumping();
  static void dump(const char* archive_name, TRAPS);
  static void dump(TRAPS);
  static bool is_mapped() { return FileMapInfo::dynamic_info() != NULL; }
  static bool validate(FileMapInfo* dynamic_info);
};
#endif // INCLUDE_CDS
#endif // SHARE_CDS_DYNAMICARCHIVE_HPP
