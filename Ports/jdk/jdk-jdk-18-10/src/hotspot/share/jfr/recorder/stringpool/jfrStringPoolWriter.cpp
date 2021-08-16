/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/recorder/stringpool/jfrStringPool.hpp"
#include "jfr/recorder/stringpool/jfrStringPoolWriter.hpp"
#include "jfr/writers/jfrEventWriterHost.inline.hpp"
#include "jfr/writers/jfrMemoryWriterHost.inline.hpp"

JfrStringPoolFlush::JfrStringPoolFlush(Type* old, size_t used, size_t requested, Thread* thread) :
  _result(JfrStringPool::flush(old, used, requested, thread)) {}

JfrStringPoolWriter::JfrStringPoolWriter(Thread* thread) :
  JfrStringPoolWriterBase(JfrStringPool::lease(thread), thread), _nof_strings(0) {}

JfrStringPoolWriter::~JfrStringPoolWriter() {
  assert(this->is_acquired(), "invariant");
  if (!this->is_valid() || this->used_size() == 0) {
    return;
  }
  assert(this->used_size() > 0, "invariant");
  this->storage()->increment(_nof_strings);
  this->commit();
  assert(0 == this->current_offset(), "invariant");
}

void JfrStringPoolWriter::inc_nof_strings() {
  ++_nof_strings;
}
