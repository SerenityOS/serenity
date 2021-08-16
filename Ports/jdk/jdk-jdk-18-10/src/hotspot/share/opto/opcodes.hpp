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

#ifndef SHARE_OPTO_OPCODES_HPP
#define SHARE_OPTO_OPCODES_HPP

// Build a big enum of class names to give them dense integer indices
#define macro(x) Op_##x,
#define optionalmacro(x) macro(x)
enum Opcodes {
  Op_Node = 0,
  macro(Set)                    // Instruction selection match rule
  macro(RegN)                   // Machine narrow oop register
  macro(RegI)                   // Machine integer register
  macro(RegP)                   // Machine pointer register
  macro(RegF)                   // Machine float   register
  macro(RegD)                   // Machine double  register
  macro(RegL)                   // Machine long    register
  macro(VecA)                   // Machine vectora register
  macro(VecS)                   // Machine vectors register
  macro(VecD)                   // Machine vectord register
  macro(VecX)                   // Machine vectorx register
  macro(VecY)                   // Machine vectory register
  macro(VecZ)                   // Machine vectorz register
  macro(RegVectMask)            // Vector mask/predicate register
  macro(RegFlags)               // Machine flags   register
  _last_machine_leaf,           // Split between regular opcodes and machine
#include "classes.hpp"
  _last_opcode
};
#undef macro
#undef optionalmacro

// Table of names, indexed by Opcode
extern const char *NodeClassNames[];

#endif // SHARE_OPTO_OPCODES_HPP
