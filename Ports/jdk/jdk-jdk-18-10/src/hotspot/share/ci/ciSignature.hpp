/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CISIGNATURE_HPP
#define SHARE_CI_CISIGNATURE_HPP

#include "ci/ciClassList.hpp"
#include "ci/ciSymbol.hpp"
#include "interpreter/bytecodes.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/growableArray.hpp"

// ciSignature
//
// This class represents the signature of a method.
class ciSignature : public ResourceObj {
private:
  ciSymbol* _symbol;
  ciKlass*  _accessing_klass;

  GrowableArray<ciType*> _types; // parameter types
  ciType* _return_type;
  int _size;   // number of stack slots required for arguments

  friend class ciMethod;
  friend class ciBytecodeStream;
  friend class ciObjectFactory;

  ciSignature(ciKlass* accessing_klass, const constantPoolHandle& cpool, ciSymbol* signature);

  Symbol* get_symbol() const                     { return _symbol->get_symbol(); }

public:
  ciSymbol* as_symbol() const                    { return _symbol; }
  ciKlass*  accessing_klass() const              { return _accessing_klass; }

  ciType*   return_type() const                  { return _return_type; }
  ciType*   type_at(int index) const             { return _types.at(index); }

  int       size() const                         { return _size; }
  int       count() const                        { return _types.length(); }

  int       arg_size_for_bc(Bytecodes::Code bc)  { return size() + (Bytecodes::has_receiver(bc) ? 1 : 0); }

  bool equals(ciSignature* that);

  void print_signature();
  void print();
};

#endif // SHARE_CI_CISIGNATURE_HPP
