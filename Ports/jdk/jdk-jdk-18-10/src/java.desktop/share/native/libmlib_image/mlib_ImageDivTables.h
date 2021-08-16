/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */


#ifndef __MLIB_IMAGEDIVTABLES_H
#define __MLIB_IMAGEDIVTABLES_H

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#ifdef __DIV_TABLE_DEFINED

const mlib_u16 mlib_div6_tab[];
const mlib_u16 mlib_div1_tab[];
const mlib_f32 mlib_HSL2RGB_L2[];
const mlib_f32 mlib_HSL2RGB_F[];
const mlib_f32 mlib_U82F32[];
const mlib_d64 mlib_U82D64[];
const mlib_u32 mlib_FlipAndFixRotateTable[];

#else

extern const mlib_u16 mlib_div6_tab[];
extern const mlib_u16 mlib_div1_tab[];
extern const mlib_f32 mlib_HSL2RGB_L2[];
extern const mlib_f32 mlib_HSL2RGB_F[];
extern const mlib_f32 mlib_U82F32[];
extern const mlib_d64 mlib_U82D64[];
extern const mlib_u32 mlib_FlipAndFixRotateTable[];

#endif /* __DIV_TABLE_DEFINED */

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __MLIB_IMAGEDIVTABLES_H */

/***************************************************************/
