/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_DEFAULTMETHODS_HPP
#define SHARE_CLASSFILE_DEFAULTMETHODS_HPP

#include "runtime/handles.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/exceptions.hpp"

class InstanceKlass;
class Symbol;
class Method;

class DefaultMethods : AllStatic {
 public:

  // Analyzes class and determines which default methods are inherited
  // from interfaces (and has no other implementation).  For each method
  // (and each different signature the method could have), create an
  // "overpass" method that is an instance method that redirects to the
  // default method.  Overpass methods are added to the methods lists for
  // the class.
  static void generate_default_methods(
      InstanceKlass* klass, const GrowableArray<Method*>* mirandas, TRAPS);
};
#endif // SHARE_CLASSFILE_DEFAULTMETHODS_HPP
