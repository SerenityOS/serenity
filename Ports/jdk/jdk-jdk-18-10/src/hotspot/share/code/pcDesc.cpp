/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "code/debugInfoRec.hpp"
#include "code/nmethod.hpp"
#include "code/pcDesc.hpp"
#include "code/scopeDesc.hpp"
#include "memory/resourceArea.hpp"

PcDesc::PcDesc(int pc_offset, int scope_decode_offset, int obj_decode_offset) {
  _pc_offset           = pc_offset;
  _scope_decode_offset = scope_decode_offset;
  _obj_decode_offset   = obj_decode_offset;
  _flags               = 0;
}

address PcDesc::real_pc(const CompiledMethod* code) const {
  return code->code_begin() + pc_offset();
}

void PcDesc::print_on(outputStream* st, CompiledMethod* code) {
#ifndef PRODUCT
  ResourceMark rm;
  st->print_cr("PcDesc(pc=" PTR_FORMAT " offset=%x bits=%x):", p2i(real_pc(code)), pc_offset(), _flags);

  if (scope_decode_offset() == DebugInformationRecorder::serialized_null) {
    return;
  }

  for (ScopeDesc* sd = code->scope_desc_at(real_pc(code));
       sd != NULL;
       sd = sd->sender()) {
    sd->print_on(st);
  }
#endif
}

bool PcDesc::verify(CompiledMethod* code) {
  //Unimplemented();
  return true;
}
