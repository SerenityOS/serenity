
/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "cds/archiveBuilder.hpp"
#include "cds/runTimeClassInfo.hpp"

void RunTimeClassInfo::init(DumpTimeClassInfo& info) {
  ArchiveBuilder* builder = ArchiveBuilder::current();
  assert(builder->is_in_buffer_space(info._klass), "must be");
  _klass = info._klass;
  if (!SystemDictionaryShared::is_builtin(_klass)) {
    CrcInfo* c = crc();
    c->_clsfile_size = info._clsfile_size;
    c->_clsfile_crc32 = info._clsfile_crc32;
  }
  _num_verifier_constraints = info.num_verifier_constraints();
  _num_loader_constraints   = info.num_loader_constraints();
  int i;
  if (_num_verifier_constraints > 0) {
    RTVerifierConstraint* vf_constraints = verifier_constraints();
    char* flags = verifier_constraint_flags();
    for (i = 0; i < _num_verifier_constraints; i++) {
      vf_constraints[i]._name      = builder->any_to_offset_u4(info._verifier_constraints->at(i)._name);
      vf_constraints[i]._from_name = builder->any_to_offset_u4(info._verifier_constraints->at(i)._from_name);
    }
    for (i = 0; i < _num_verifier_constraints; i++) {
      flags[i] = info._verifier_constraint_flags->at(i);
    }
  }

  if (_num_loader_constraints > 0) {
    RTLoaderConstraint* ld_constraints = loader_constraints();
    for (i = 0; i < _num_loader_constraints; i++) {
      ld_constraints[i]._name = builder->any_to_offset_u4(info._loader_constraints->at(i)._name);
      ld_constraints[i]._loader_type1 = info._loader_constraints->at(i)._loader_type1;
      ld_constraints[i]._loader_type2 = info._loader_constraints->at(i)._loader_type2;
    }
  }

  if (_klass->is_hidden()) {
    InstanceKlass* n_h = info.nest_host();
    set_nest_host(n_h);
  }
  ArchivePtrMarker::mark_pointer(&_klass);
}

size_t RunTimeClassInfo::crc_size(InstanceKlass* klass) {
  if (!SystemDictionaryShared::is_builtin(klass)) {
    return sizeof(CrcInfo);
  } else {
    return 0;
  }
}
