/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_JAVAASSERTIONS_HPP
#define SHARE_CLASSFILE_JAVAASSERTIONS_HPP

#include "oops/objArrayOop.hpp"
#include "oops/typeArrayOop.hpp"
#include "runtime/handles.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/ostream.hpp"

class JavaAssertions: AllStatic {
public:
  static inline bool userClassDefault();
  static inline void setUserClassDefault(bool enabled);
  static inline bool systemClassDefault();
  static inline void setSystemClassDefault(bool enabled);

  // Add a command-line option.  A name ending in "..." applies to a package and
  // any subpackages; other names apply to a single class.
  static void addOption(const char* name, bool enable);

  // Return true if command-line options have enabled assertions for the named
  // class.  Should be called only after all command-line options have been
  // processed.  Note:  this only consults command-line options and does not
  // account for any dynamic changes to assertion status.
  static bool enabled(const char* classname, bool systemClass);

  // Create an instance of java.lang.AssertionStatusDirectives and fill in the
  // fields based on the command-line assertion options.
  static oop createAssertionStatusDirectives(TRAPS);

private:
  class OptionList;
  static void fillJavaArrays(const OptionList* p, int len, objArrayHandle names,
    typeArrayHandle status, TRAPS);

  static inline void trace(const char* name, const char* typefound,
    const char* namefound, bool enabled);

  static inline OptionList*     match_class(const char* classname);
  static OptionList*            match_package(const char* classname);

  static bool           _userDefault;   // User class default (-ea/-da).
  static bool           _sysDefault;    // System class default (-esa/-dsa).
  static OptionList*    _classes;       // Options for classes.
  static OptionList*    _packages;      // Options for package trees.
};

class JavaAssertions::OptionList: public CHeapObj<mtClass> {
public:
  inline OptionList(const char* name, bool enable, OptionList* next);

  inline const char*    name() const    { return _name; }
  inline bool           enabled() const { return _enabled; }
  inline OptionList*    next() const    { return _next; }

  static int count(OptionList* p);

private:
  const char*   _name;
  OptionList*   _next;
  bool          _enabled;
};

inline bool JavaAssertions::userClassDefault() {
  return _userDefault;
}

inline void JavaAssertions::setUserClassDefault(bool enabled) {
  if (TraceJavaAssertions)
    tty->print_cr("JavaAssertions::setUserClassDefault(%d)", enabled);
  _userDefault = enabled;
}

inline bool JavaAssertions::systemClassDefault() {
  return _sysDefault;
}

inline void JavaAssertions::setSystemClassDefault(bool enabled) {
  if (TraceJavaAssertions)
    tty->print_cr("JavaAssertions::setSystemClassDefault(%d)", enabled);
  _sysDefault = enabled;
}

#endif // SHARE_CLASSFILE_JAVAASSERTIONS_HPP
