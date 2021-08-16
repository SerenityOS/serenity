/*
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

#ifndef _IMMEDIATE_H
#define _IMMEDIATE_H

#include <sys/types.h>

/*
 * functions to map backwards and forwards between logical or floating
 * point immediates and their corresponding encodings. the mapping
 * from encoding to immediate is required by the simulator. the reverse
 * mapping is required by the OpenJDK assembler.
 *
 * a logical immediate value supplied to or returned from a map lookup
 * is always 64 bits. this is sufficient for looking up 32 bit
 * immediates or their encodings since a 32 bit immediate has the same
 * encoding as the 64 bit immediate produced by concatenating the
 * immediate with itself.
 *
 * a logical immediate encoding is 13 bits N:immr:imms (3 fields of
 * widths 1:6:6 -- see the arm spec). they appear as bits [22:10] of a
 * logical immediate instruction. encodings are supplied and returned
 * as 32 bit values. if a given 13 bit immediate has no corresponding
 * encoding then a map lookup will return 0xffffffff.
 */

uint64_t logical_immediate_for_encoding(uint32_t encoding);
uint32_t encoding_for_logical_immediate(uint64_t immediate);
uint64_t fp_immediate_for_encoding(uint32_t imm8, int is_dp);
uint32_t encoding_for_fp_immediate(float immediate);

#endif // _IMMEDIATE_H
