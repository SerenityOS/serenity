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

#ifndef SHARE_CI_CIFLAGS_HPP
#define SHARE_CI_CIFLAGS_HPP

#include "jvm_constants.h"
#include "ci/ciClassList.hpp"
#include "utilities/accessFlags.hpp"
#include "utilities/ostream.hpp"

// ciFlags
//
// This class represents klass or method flags.
class ciFlags {
private:
  friend class ciInstanceKlass;
  friend class ciField;
  friend class ciMethod;

  jint _flags;

  ciFlags()                  { _flags = 0; }
  ciFlags(AccessFlags flags) { _flags = flags.as_int(); }

public:
  // Java access flags
  bool is_public               () const { return (_flags & JVM_ACC_PUBLIC                    ) != 0; }
  bool is_private              () const { return (_flags & JVM_ACC_PRIVATE                   ) != 0; }
  bool is_protected            () const { return (_flags & JVM_ACC_PROTECTED                 ) != 0; }
  bool is_static               () const { return (_flags & JVM_ACC_STATIC                    ) != 0; }
  bool is_final                () const { return (_flags & JVM_ACC_FINAL                     ) != 0; }
  bool is_synchronized         () const { return (_flags & JVM_ACC_SYNCHRONIZED              ) != 0; }
  bool is_super                () const { return (_flags & JVM_ACC_SUPER                     ) != 0; }
  bool is_volatile             () const { return (_flags & JVM_ACC_VOLATILE                  ) != 0; }
  bool is_transient            () const { return (_flags & JVM_ACC_TRANSIENT                 ) != 0; }
  bool is_native               () const { return (_flags & JVM_ACC_NATIVE                    ) != 0; }
  bool is_interface            () const { return (_flags & JVM_ACC_INTERFACE                 ) != 0; }
  bool is_abstract             () const { return (_flags & JVM_ACC_ABSTRACT                  ) != 0; }
  bool is_stable               () const { return (_flags & JVM_ACC_FIELD_STABLE              ) != 0; }
  // In case the current object represents a field, return true if
  // the field is modified outside of instance initializer methods
  // (or class/initializer methods if the field is static) and false
  // otherwise.
  bool has_initialized_final_update() const { return (_flags & JVM_ACC_FIELD_INITIALIZED_FINAL_UPDATE) != 0; };

  // Conversion
  jint   as_int()                      { return _flags; }

  void print_klass_flags(outputStream* st = tty);
  void print_member_flags(outputStream* st = tty);
  void print(outputStream* st = tty);
};

#endif // SHARE_CI_CIFLAGS_HPP
