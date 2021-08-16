/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_METADATA_HPP
#define SHARE_OOPS_METADATA_HPP

#include "utilities/exceptions.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

// This is the base class for an internal Class related metadata
class Metadata : public MetaspaceObj {
  // Debugging hook to check that the metadata has not been deleted.
  NOT_PRODUCT(int _valid;)
 public:
  NOT_PRODUCT(Metadata() : _valid(0) {})
  NOT_PRODUCT(bool is_valid() const { return _valid == 0; })

  int identity_hash()                { return (int)(uintptr_t)this; }

  virtual bool is_metadata()           const { return true; }
  virtual bool is_klass()              const { return false; }
  virtual bool is_method()             const { return false; }
  virtual bool is_methodData()         const { return false; }
  virtual bool is_constantPool()       const { return false; }
  virtual bool is_methodCounters()     const { return false; }
  virtual int  size()                  const = 0;
  virtual MetaspaceObj::Type type()    const = 0;
  virtual const char* internal_name()  const = 0;
  virtual void metaspace_pointers_do(MetaspaceClosure* iter) {}

  void print()       const;
  void print_value() const;

  static void print_value_on_maybe_null(outputStream* st, const Metadata* m) {
    if (NULL == m)
      st->print("NULL");
    else
      m->print_value_on(st);
  }

  virtual void print_on(outputStream* st) const;       // First level print
  virtual void print_value_on(outputStream* st) const = 0; // Second level print

  char* print_value_string() const;

  // Used to keep metadata alive during class redefinition
  // Can't assert because is called for delete functions (as an assert)
  virtual bool on_stack() const { return false; }
  virtual void set_on_stack(const bool value);

  // Set on_stack bit, so that the metadata is not cleared
  // during class redefinition.  This is a virtual call because only methods
  // and constant pools need to be set, but someday instanceKlasses might also.
  static void mark_on_stack(Metadata* m) { m->set_on_stack(true); }
};

#endif // SHARE_OOPS_METADATA_HPP
