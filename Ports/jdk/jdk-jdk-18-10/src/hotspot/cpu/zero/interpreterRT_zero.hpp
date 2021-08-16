/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008 Red Hat, Inc.
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

#ifndef CPU_ZERO_INTERPRETERRT_ZERO_HPP
#define CPU_ZERO_INTERPRETERRT_ZERO_HPP

// This is included in the middle of class Interpreter.
// Do not include files here.


class SignatureHandler {
 public:
  static SignatureHandler *from_handlerAddr(address handlerAddr) {
    return (SignatureHandler *) handlerAddr;
  }

 public:
  ffi_cif* cif() const {
    return (ffi_cif *) this;
  }

  int argument_count() const {
    return cif()->nargs;
  }

  ffi_type** argument_types() const {
    return (ffi_type**) (cif() + 1);
  }

  ffi_type* argument_type(int i) const {
    return argument_types()[i];
  }

  ffi_type* result_type() const {
    return *(argument_types() + argument_count());
  }

 protected:
  friend class InterpreterRuntime;
  friend class SignatureHandlerLibrary;

  void finalize();
};

class SignatureHandlerGeneratorBase : public NativeSignatureIterator {
 private:
  ffi_cif* _cif;

 protected:
  SignatureHandlerGeneratorBase(const methodHandle& method, ffi_cif *cif)
    : NativeSignatureIterator(method), _cif(cif) {
    _cif->nargs = 0;
  }

  ffi_cif *cif() const {
    return _cif;
  }

 public:
  void generate(uint64_t fingerprint);

 private:
  void pass_int();
  void pass_long();
  void pass_float();
  void pass_double();
  void pass_object();

 private:
  void push(BasicType type);
  virtual void push(intptr_t value) = 0;
};

class SignatureHandlerGenerator : public SignatureHandlerGeneratorBase {
 private:
  CodeBuffer* _cb;

 public:
  SignatureHandlerGenerator(const methodHandle& method, CodeBuffer* buffer);

 private:
  void push(intptr_t value);
};

class SlowSignatureHandlerGenerator : public SignatureHandlerGeneratorBase {
 private:
  intptr_t *_dst;

 public:
  SlowSignatureHandlerGenerator(const methodHandle& method, intptr_t* buf)
    : SignatureHandlerGeneratorBase(method, (ffi_cif *) buf) {
    _dst = (intptr_t *) (cif() + 1);
  }

 private:
  void push(intptr_t value) {
    *(_dst++) = value;
  }

 public:
  SignatureHandler *handler() const {
    return (SignatureHandler *) cif();
  }
};

#endif // CPU_ZERO_INTERPRETERRT_ZERO_HPP
