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

#include "precompiled.hpp"
#include "classfile/javaAssertions.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/oopFactory.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "runtime/handles.inline.hpp"

bool                            JavaAssertions::_userDefault = false;
bool                            JavaAssertions::_sysDefault = false;
JavaAssertions::OptionList*     JavaAssertions::_classes = 0;
JavaAssertions::OptionList*     JavaAssertions::_packages = 0;

JavaAssertions::OptionList::OptionList(const char* name, bool enabled,
  OptionList* next) {
  assert(name != 0, "need a name");
  _name = name;
  _enabled = enabled;
  _next = next;
}

int JavaAssertions::OptionList::count(OptionList* p) {
  int rc;
  for (rc = 0; p != 0; p = p->next(), ++rc) /* empty */;
  return rc;
}

void JavaAssertions::addOption(const char* name, bool enable) {
  assert(name != 0, "must have a name");

  // Copy the name.  The storage needs to exist for the the lifetime of the vm;
  // it is never freed, so will be leaked (along with other option strings -
  // e.g., bootclasspath) if a process creates/destroys multiple VMs.
  int len = (int)strlen(name);
  char *name_copy = NEW_C_HEAP_ARRAY(char, len + 1, mtClass);
  strcpy(name_copy, name);

  // Figure out which list the new item should go on.  Names that end in "..."
  // go on the package tree list.
  OptionList** head = &_classes;
  if (len >= 3 && strcmp(name_copy + len - 3, "...") == 0) {
    // Delete the "...".
    len -= 3;
    name_copy[len] = '\0';
    head = &_packages;
  }

  // Convert class/package names to internal format.  Will have to convert back
  // when copying to java in createJavaAssertionStatusDirectives, but that
  // should happen only once.  Alternative would require that
  // JVM_DesiredAssertionStatus pass the external_name() to
  // JavaAssertion::enabled(), but that is done once per loaded class.
  for (int i = 0; i < len; ++i) {
    if (name_copy[i] == JVM_SIGNATURE_DOT) name_copy[i] = JVM_SIGNATURE_SLASH;
  }

  if (TraceJavaAssertions) {
    tty->print_cr("JavaAssertions: adding %s %s=%d",
      head == &_classes ? "class" : "package",
      name_copy[0] != '\0' ? name_copy : "'default'",
      enable);
  }

  // Prepend a new item to the list.  Items added later take precedence, so
  // prepending allows us to stop searching the list after the first match.
  *head = new OptionList(name_copy, enable, *head);
}

oop JavaAssertions::createAssertionStatusDirectives(TRAPS) {
  Symbol* asd_sym = vmSymbols::java_lang_AssertionStatusDirectives();
  Klass* k = SystemDictionary::resolve_or_fail(asd_sym, true, CHECK_NULL);
  InstanceKlass* asd_klass = InstanceKlass::cast(k);
  asd_klass->initialize(CHECK_NULL);
  Handle h = asd_klass->allocate_instance_handle(CHECK_NULL);

  int len;
  typeArrayOop t;
  len = OptionList::count(_packages);
  objArrayOop pn = oopFactory::new_objArray(vmClasses::String_klass(), len, CHECK_NULL);
  objArrayHandle pkgNames (THREAD, pn);
  t = oopFactory::new_typeArray(T_BOOLEAN, len, CHECK_NULL);
  typeArrayHandle pkgEnabled(THREAD, t);
  fillJavaArrays(_packages, len, pkgNames, pkgEnabled, CHECK_NULL);

  len = OptionList::count(_classes);
  objArrayOop cn = oopFactory::new_objArray(vmClasses::String_klass(), len, CHECK_NULL);
  objArrayHandle classNames (THREAD, cn);
  t = oopFactory::new_typeArray(T_BOOLEAN, len, CHECK_NULL);
  typeArrayHandle classEnabled(THREAD, t);
  fillJavaArrays(_classes, len, classNames, classEnabled, CHECK_NULL);

  java_lang_AssertionStatusDirectives::set_packages(h(), pkgNames());
  java_lang_AssertionStatusDirectives::set_packageEnabled(h(), pkgEnabled());
  java_lang_AssertionStatusDirectives::set_classes(h(), classNames());
  java_lang_AssertionStatusDirectives::set_classEnabled(h(), classEnabled());
  java_lang_AssertionStatusDirectives::set_deflt(h(), userClassDefault());
  return h();
}

void JavaAssertions::fillJavaArrays(const OptionList* p, int len,
                                    objArrayHandle names,
                                    typeArrayHandle enabled, TRAPS) {
  // Fill in the parallel names and enabled (boolean) arrays.  Start at the end
  // of the array and work backwards, so the order of items in the arrays
  // matches the order on the command line (the list is in reverse order, since
  // it was created by prepending successive items from the command line).
  int index;
  for (index = len - 1; p != 0; p = p->next(), --index) {
    assert(index >= 0, "length does not match list");
    TempNewSymbol name = SymbolTable::new_symbol(p->name());
    Handle s = java_lang_String::externalize_classname(name, CHECK);
    names->obj_at_put(index, s());
    enabled->bool_at_put(index, p->enabled());
  }
  assert(index == -1, "length does not match list");
}

inline JavaAssertions::OptionList*
JavaAssertions::match_class(const char* classname) {
  for (OptionList* p = _classes; p != 0; p = p->next()) {
    if (strcmp(p->name(), classname) == 0) {
      return p;
    }
  }
  return 0;
}

JavaAssertions::OptionList*
JavaAssertions::match_package(const char* classname) {
  // Search the package list for any items that apply to classname.  Each
  // sub-package in classname is checked, from most-specific to least, until one
  // is found.
  if (_packages == 0) return 0;

  // Find the length of the "most-specific" package in classname.  If classname
  // does not include a package, length will be 0 which will match items for the
  // default package (from options "-ea:..."  or "-da:...").
  size_t len = strlen(classname);
  for (/* empty */; len > 0 && classname[len] != JVM_SIGNATURE_SLASH; --len) /* empty */;

  do {
    assert(len == 0 || classname[len] == JVM_SIGNATURE_SLASH, "not a package name");
    for (OptionList* p = _packages; p != 0; p = p->next()) {
      if (strncmp(p->name(), classname, len) == 0 && p->name()[len] == '\0') {
        return p;
      }
    }

    // Find the length of the next package, taking care to avoid decrementing
    // past 0 (len is unsigned).
    while (len > 0 && classname[--len] != JVM_SIGNATURE_SLASH) /* empty */;
  } while (len > 0);

  return 0;
}

inline void JavaAssertions::trace(const char* name,
const char* typefound, const char* namefound, bool enabled) {
  if (TraceJavaAssertions) {
    tty->print_cr("JavaAssertions:  search for %s found %s %s=%d",
      name, typefound, namefound[0] != '\0' ? namefound : "'default'", enabled);
  }
}

bool JavaAssertions::enabled(const char* classname, bool systemClass) {
  assert(classname != 0, "must have a classname");

  // This will be slow if the number of assertion options on the command line is
  // large--it traverses two lists, one of them multiple times.  Could use a
  // single n-ary tree instead of lists if someone ever notices.

  // First check options that apply to classes.  If we find a match we're done.
  OptionList* p;
  if ((p = match_class(classname))) {
    trace(classname, "class", p->name(), p->enabled());
    return p->enabled();
  }

  // Now check packages, from most specific to least.
  if ((p = match_package(classname))) {
    trace(classname, "package", p->name(), p->enabled());
    return p->enabled();
  }

  // No match.  Return the default status.
  bool result = systemClass ? systemClassDefault() : userClassDefault();
  trace(classname, systemClass ? "system" : "user", "default", result);
  return result;
}
