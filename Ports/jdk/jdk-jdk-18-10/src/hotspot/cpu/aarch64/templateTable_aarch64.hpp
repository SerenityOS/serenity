/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, Red Hat Inc. All rights reserved.
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

#ifndef CPU_AARCH64_TEMPLATETABLE_AARCH64_HPP
#define CPU_AARCH64_TEMPLATETABLE_AARCH64_HPP

static void prepare_invoke(int byte_no,
                             Register method,         // linked method (or i-klass)
                             Register index = noreg,  // itable index, MethodType, etc.
                             Register recv  = noreg,  // if caller wants to see it
                             Register flags = noreg   // if caller wants to test it
                             );
  static void invokevirtual_helper(Register index, Register recv,
                                   Register flags);

  // Helpers
  static void index_check(Register array, Register index);
  static void index_check_without_pop(Register array, Register index);

#endif // CPU_AARCH64_TEMPLATETABLE_AARCH64_HPP
