/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "oops/compiledICHolder.hpp"
#include "runtime/atomic.hpp"

volatile int CompiledICHolder::_live_count;
volatile int CompiledICHolder::_live_not_claimed_count;


CompiledICHolder::CompiledICHolder(Metadata* metadata, Klass* klass, bool is_method)
  : _holder_metadata(metadata), _holder_klass(klass), _is_metadata_method(is_method) {
#ifdef ASSERT
  Atomic::inc(&_live_count);
  Atomic::inc(&_live_not_claimed_count);
#endif // ASSERT
}

#ifdef ASSERT
CompiledICHolder::~CompiledICHolder() {
  assert(_live_count > 0, "underflow");
  Atomic::dec(&_live_count);
}
#endif // ASSERT

// Printing

void CompiledICHolder::print_on(outputStream* st) const {
  st->print("%s", internal_name());
  st->print(" - metadata: "); holder_metadata()->print_value_on(st); st->cr();
  st->print(" - klass:    "); holder_klass()->print_value_on(st); st->cr();
}

void CompiledICHolder::print_value_on(outputStream* st) const {
  st->print("%s", internal_name());
}


// Verification

void CompiledICHolder::verify_on(outputStream* st) {
  guarantee(holder_metadata()->is_method() || holder_metadata()->is_klass(), "should be method or klass");
  guarantee(holder_klass()->is_klass(),   "should be klass");
}

#ifdef ASSERT

void CompiledICHolder::claim() {
  Atomic::dec(&_live_not_claimed_count);
}

#endif // ASSERT
