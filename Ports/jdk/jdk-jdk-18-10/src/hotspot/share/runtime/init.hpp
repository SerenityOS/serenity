/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_INIT_HPP
#define SHARE_RUNTIME_INIT_HPP

#include "utilities/globalDefinitions.hpp"

// init_globals replaces C++ global objects so we can use the standard linker
// to link Delta (which is at least twice as fast as using the GNU C++ linker).
// Also, init.c gives explicit control over the sequence of initialization.

// Programming convention: instead of using a global object (e,g, "Foo foo;"),
// use "Foo* foo;", create a function init_foo() in foo.c, and add a call
// to init_foo in init.cpp.

jint init_globals();     // call constructors at startup (main Java thread)
void vm_init_globals();  // call constructors at startup (VM thread)
void exit_globals();     // call destructors before exit

bool is_init_completed();     // returns true when bootstrapping has completed
void wait_init_completed();   // wait until set_init_completed() has been called
void set_init_completed();    // set basic init to completed

#endif // SHARE_RUNTIME_INIT_HPP
