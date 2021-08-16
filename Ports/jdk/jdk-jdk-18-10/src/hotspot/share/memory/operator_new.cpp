/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "utilities/debug.hpp"

#include <new>

//--------------------------------------------------------------------------------------
// Non-product code

#ifndef PRODUCT
// The global operator new should never be called since it will usually indicate
// a memory leak.  Use CHeapObj as the base class of such objects to make it explicit
// that they're allocated on the C heap.
// Commented out in product version to avoid conflicts with third-party C++ native code.
//
// In C++98/03 the throwing new operators are defined with the following signature:
//
// void* operator new(std::size_tsize) throw(std::bad_alloc);
// void* operator new[](std::size_tsize) throw(std::bad_alloc);
//
// while all the other (non-throwing) new and delete operators are defined with an empty
// throw clause (i.e. "operator delete(void* p) throw()") which means that they do not
// throw any exceptions (see section 18.4 of the C++ standard).
//
// In the new C++11/14 standard, the signature of the throwing new operators was changed
// by completely omitting the throw clause (which effectively means they could throw any
// exception) while all the other new/delete operators where changed to have a 'nothrow'
// clause instead of an empty throw clause.
//
// Unfortunately, the support for exception specifications among C++ compilers is still
// very fragile. While some more strict compilers like AIX xlC or HP aCC reject to
// override the default throwing new operator with a user operator with an empty throw()
// clause, the MS Visual C++ compiler warns for every non-empty throw clause like
// throw(std::bad_alloc) that it will ignore the exception specification. The following
// operator definitions have been checked to correctly work with all currently supported
// compilers and they should be upwards compatible with C++11/14. Therefore
// PLEASE BE CAREFUL if you change the signature of the following operators!

static void * zero = (void *) 0;

void* operator new(size_t size) /* throw(std::bad_alloc) */ {
  fatal("Should not call global operator new");
  return zero;
}

void* operator new [](size_t size) /* throw(std::bad_alloc) */ {
  fatal("Should not call global operator new[]");
  return zero;
}

void* operator new(size_t size, const std::nothrow_t&  nothrow_constant) throw() {
  fatal("Should not call global operator new");
  return 0;
}

void* operator new [](size_t size, std::nothrow_t&  nothrow_constant) throw() {
  fatal("Should not call global operator new[]");
  return 0;
}

void operator delete(void* p) throw() {
  fatal("Should not call global delete");
}

void operator delete [](void* p) throw() {
  fatal("Should not call global delete []");
}

#ifdef __GNUG__
// Warning disabled for gcc 5.4
PRAGMA_DIAG_PUSH
PRAGMA_DISABLE_GCC_WARNING("-Wc++14-compat")
#endif // __GNUG__

void operator delete(void* p, size_t size) throw() {
  fatal("Should not call global sized delete");
}

void operator delete [](void* p, size_t size) throw() {
  fatal("Should not call global sized delete []");
}

#ifdef __GNUG__
PRAGMA_DIAG_POP
#endif // __GNUG__

#endif // Non-product
