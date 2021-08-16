/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/assembler.hpp"
#include "asm/register.hpp"
#include "interp_masm_arm.hpp"
#include "register_arm.hpp"

REGISTER_DEFINITION(Register, noreg);
REGISTER_DEFINITION(FloatRegister, fnoreg);


REGISTER_DEFINITION(FloatRegister, S0);
REGISTER_DEFINITION(FloatRegister, S1_reg);
REGISTER_DEFINITION(FloatRegister, S2_reg);
REGISTER_DEFINITION(FloatRegister, S3_reg);
REGISTER_DEFINITION(FloatRegister, S4_reg);
REGISTER_DEFINITION(FloatRegister, S5_reg);
REGISTER_DEFINITION(FloatRegister, S6_reg);
REGISTER_DEFINITION(FloatRegister, S7);
REGISTER_DEFINITION(FloatRegister, S8);
REGISTER_DEFINITION(FloatRegister, S9);
REGISTER_DEFINITION(FloatRegister, S10);
REGISTER_DEFINITION(FloatRegister, S11);
REGISTER_DEFINITION(FloatRegister, S12);
REGISTER_DEFINITION(FloatRegister, S13);
REGISTER_DEFINITION(FloatRegister, S14);
REGISTER_DEFINITION(FloatRegister, S15);
REGISTER_DEFINITION(FloatRegister, S16);
REGISTER_DEFINITION(FloatRegister, S17);
REGISTER_DEFINITION(FloatRegister, S18);
REGISTER_DEFINITION(FloatRegister, S19);
REGISTER_DEFINITION(FloatRegister, S20);
REGISTER_DEFINITION(FloatRegister, S21);
REGISTER_DEFINITION(FloatRegister, S22);
REGISTER_DEFINITION(FloatRegister, S23);
REGISTER_DEFINITION(FloatRegister, S24);
REGISTER_DEFINITION(FloatRegister, S25);
REGISTER_DEFINITION(FloatRegister, S26);
REGISTER_DEFINITION(FloatRegister, S27);
REGISTER_DEFINITION(FloatRegister, S28);
REGISTER_DEFINITION(FloatRegister, S29);
REGISTER_DEFINITION(FloatRegister, S30);
REGISTER_DEFINITION(FloatRegister, S31);
REGISTER_DEFINITION(FloatRegister, Stemp);
REGISTER_DEFINITION(FloatRegister, D0);
REGISTER_DEFINITION(FloatRegister, D1);
REGISTER_DEFINITION(FloatRegister, D2);
REGISTER_DEFINITION(FloatRegister, D3);
REGISTER_DEFINITION(FloatRegister, D4);
REGISTER_DEFINITION(FloatRegister, D5);
REGISTER_DEFINITION(FloatRegister, D6);
REGISTER_DEFINITION(FloatRegister, D7);
REGISTER_DEFINITION(FloatRegister, D8);
REGISTER_DEFINITION(FloatRegister, D9);
REGISTER_DEFINITION(FloatRegister, D10);
REGISTER_DEFINITION(FloatRegister, D11);
REGISTER_DEFINITION(FloatRegister, D12);
REGISTER_DEFINITION(FloatRegister, D13);
REGISTER_DEFINITION(FloatRegister, D14);
REGISTER_DEFINITION(FloatRegister, D15);
REGISTER_DEFINITION(FloatRegister, D16);
REGISTER_DEFINITION(FloatRegister, D17);
REGISTER_DEFINITION(FloatRegister, D18);
REGISTER_DEFINITION(FloatRegister, D19);
REGISTER_DEFINITION(FloatRegister, D20);
REGISTER_DEFINITION(FloatRegister, D21);
REGISTER_DEFINITION(FloatRegister, D22);
REGISTER_DEFINITION(FloatRegister, D23);
REGISTER_DEFINITION(FloatRegister, D24);
REGISTER_DEFINITION(FloatRegister, D25);
REGISTER_DEFINITION(FloatRegister, D26);
REGISTER_DEFINITION(FloatRegister, D27);
REGISTER_DEFINITION(FloatRegister, D28);
REGISTER_DEFINITION(FloatRegister, D29);
REGISTER_DEFINITION(FloatRegister, D30);
REGISTER_DEFINITION(FloatRegister, D31);

