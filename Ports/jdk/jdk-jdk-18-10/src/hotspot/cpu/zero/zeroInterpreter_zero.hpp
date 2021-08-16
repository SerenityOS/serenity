/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008, 2010, 2011 Red Hat, Inc.
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

#ifndef CPU_ZERO_CPPINTERPRETER_ZERO_HPP
#define CPU_ZERO_CPPINTERPRETER_ZERO_HPP

 protected:
  // Size of interpreter code
  const static int InterpreterCodeSize = 6 * K;

 public:
  // Method entries
  static int normal_entry(Method* method, intptr_t UNUSED, TRAPS);
  static int native_entry(Method* method, intptr_t UNUSED, TRAPS);
  static int getter_entry(Method* method, intptr_t UNUSED, TRAPS);
  static int setter_entry(Method* method, intptr_t UNUSED, TRAPS);
  static int empty_entry(Method* method, intptr_t UNUSED, TRAPS);
  static int Reference_get_entry(Method* method, intptr_t UNUSED, TRAPS);

 public:
  // Main loop of normal_entry
  static void main_loop(int recurse, TRAPS);

#endif // CPU_ZERO_CPPINTERPRETER_ZERO_HPP
