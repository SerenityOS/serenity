/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2018 SAP SE. All rights reserved.
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

#ifndef CPU_PPC_RELOCINFO_PPC_HPP
#define CPU_PPC_RELOCINFO_PPC_HPP

  // machine-dependent parts of class relocInfo
 private:
  enum {
    // Since Power instructions are whole words,
    // the two low-order offset bits can always be discarded.
    offset_unit        =  4,

    // There is no need for format bits; the instructions are
    // sufficiently self-identifying.
#ifndef _LP64
    format_width       =  0
#else
    // Except narrow oops in 64-bits VM.
    format_width       =  1
#endif
  };

 public:

  // This platform has no oops in the code that are not also
  // listed in the oop section.
  static bool mustIterateImmediateOopsInCode() { return false; }

#endif // CPU_PPC_RELOCINFO_PPC_HPP
