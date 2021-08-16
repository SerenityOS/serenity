/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_COMPILEDICHOLDER_HPP
#define SHARE_OOPS_COMPILEDICHOLDER_HPP

#include "oops/oop.hpp"
#include "utilities/macros.hpp"
#include "oops/klass.hpp"
#include "oops/method.hpp"

// A CompiledICHolder* is a helper object for the inline cache implementation.
// It holds:
//   (1) (method+klass pair) when converting from compiled to an interpreted call
//   (2) (klass+klass pair) when calling itable stub from megamorphic compiled call
//
// These are always allocated in the C heap and are freed during a
// safepoint by the ICBuffer logic.  It's unsafe to free them earlier
// since they might be in use.
//


class CompiledICHolder : public CHeapObj<mtCompiler> {
  friend class VMStructs;
 private:
  static volatile int _live_count; // allocated
  static volatile int _live_not_claimed_count; // allocated but not yet in use so not
                                               // reachable by iterating over nmethods

  Metadata* _holder_metadata;
  Klass*    _holder_klass;    // to avoid name conflict with oopDesc::_klass
  CompiledICHolder* _next;
  bool _is_metadata_method;

 public:
  // Constructor
  CompiledICHolder(Metadata* metadata, Klass* klass, bool is_method = true);
  ~CompiledICHolder() NOT_DEBUG_RETURN;

  static int live_count() { return _live_count; }
  static int live_not_claimed_count() { return _live_not_claimed_count; }

  // accessors
  Klass*    holder_klass()  const     { return _holder_klass; }
  Metadata* holder_metadata() const   { return _holder_metadata; }

  static int holder_metadata_offset() { return offset_of(CompiledICHolder, _holder_metadata); }
  static int holder_klass_offset()    { return offset_of(CompiledICHolder, _holder_klass); }

  CompiledICHolder* next()     { return _next; }
  void set_next(CompiledICHolder* n) { _next = n; }

  inline bool is_loader_alive();

  // Verify
  void verify_on(outputStream* st);

  // Printing
  void print_on(outputStream* st) const;
  void print_value_on(outputStream* st) const;

  const char* internal_name() const { return "{compiledICHolder}"; }

  void claim() NOT_DEBUG_RETURN;
};

#endif // SHARE_OOPS_COMPILEDICHOLDER_HPP
