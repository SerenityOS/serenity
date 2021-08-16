/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CIMETADATA_HPP
#define SHARE_CI_CIMETADATA_HPP

#include "ci/ciBaseObject.hpp"
#include "ci/ciClassList.hpp"
#include "runtime/handles.hpp"

// ciMetadata
//
// Compiler interface to metadata object in the VM, not Java object.

class ciMetadata: public ciBaseObject {
  CI_PACKAGE_ACCESS
  friend class ciEnv;

 protected:
  Metadata* _metadata;

  ciMetadata(): _metadata(NULL) {}
  ciMetadata(Metadata* o): _metadata(o) {}

  virtual bool is_classless() const;
 public:
  bool is_loaded() const { return _metadata != NULL || is_classless(); }

  virtual bool is_metadata() const          { return true; }

  virtual bool is_type() const              { return false; }
  virtual bool is_return_address() const    { return false; }
  virtual bool is_method() const            { return false; }
  virtual bool is_method_data() const       { return false; }
  virtual bool is_klass() const             { return false; }
  virtual bool is_instance_klass() const    { return false; }
  virtual bool is_array_klass() const       { return false; }
  virtual bool is_obj_array_klass() const   { return false; }
  virtual bool is_type_array_klass() const  { return false; }
  virtual void dump_replay_data(outputStream* st) { /* do nothing */ }

  ciMethod*                as_method() {
    assert(is_method(), "bad cast");
    return (ciMethod*)this;
  }
  ciMethodData*            as_method_data() {
    assert(is_method_data(), "bad cast");
    return (ciMethodData*)this;
  }
  ciSymbol*                as_symbol() {
    assert(is_symbol(), "bad cast");
    return (ciSymbol*)this;
  }
  ciType*                  as_type() {
    assert(is_type(), "bad cast");
    return (ciType*)this;
  }
  ciReturnAddress*         as_return_address() {
    assert(is_return_address(), "bad cast");
    return (ciReturnAddress*)this;
  }
  ciKlass*                 as_klass() {
    assert(is_klass(), "bad cast");
    return (ciKlass*)this;
  }
  ciInstanceKlass*         as_instance_klass() {
    assert(is_instance_klass(), "bad cast");
    return (ciInstanceKlass*)this;
  }
  ciArrayKlass*            as_array_klass() {
    assert(is_array_klass(), "bad cast");
    return (ciArrayKlass*)this;
  }
  ciObjArrayKlass*         as_obj_array_klass() {
    assert(is_obj_array_klass(), "bad cast");
    return (ciObjArrayKlass*)this;
  }
  ciTypeArrayKlass*        as_type_array_klass() {
    assert(is_type_array_klass(), "bad cast");
    return (ciTypeArrayKlass*)this;
  }

  Metadata* constant_encoding() { return _metadata; }

  bool equals(ciMetadata* obj) const { return (this == obj); }

  int hash() { return ident() * 31; } // ???

  void print(outputStream* st);
  virtual void print_impl(outputStream* st) {}
  virtual const char* type_string() { return "ciMetadata"; }

  void print()  { print(tty); }
  void print_metadata(outputStream* st = tty);

};
#endif // SHARE_CI_CIMETADATA_HPP
