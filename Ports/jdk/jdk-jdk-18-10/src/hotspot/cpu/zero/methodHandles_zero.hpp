/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2011 Red Hat, Inc.
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


// Adapters
enum /* platform_dependent_constants */ {
  adapter_code_size = sizeof(ZeroEntry) * (Interpreter::method_handle_invoke_LAST - Interpreter::method_handle_invoke_FIRST + 1)
};

private:
  static oop popFromStack(TRAPS);
  static void invoke_target(Method* method, TRAPS);
  static void setup_frame_anchor(JavaThread* thread);
  static void teardown_frame_anchor(JavaThread* thread);
  static void throw_AME(Klass* rcvr, Method* interface_method, TRAPS);
  static void throw_NPE(TRAPS);
  static int method_handle_entry_invokeBasic(Method* method, intptr_t UNUSED, TRAPS);
  static int method_handle_entry_linkToStaticOrSpecial(Method* method, intptr_t UNUSED, TRAPS);
  static int method_handle_entry_linkToVirtual(Method* method, intptr_t UNUSED, TRAPS);
  static int method_handle_entry_linkToInterface(Method* method, intptr_t UNUSED, TRAPS);
  static int method_handle_entry_invalid(Method* method, intptr_t UNUSED, TRAPS);
