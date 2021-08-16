; Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
; DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
;
; This code is free software; you can redistribute it and/or modify it
; under the terms of the GNU General Public License version 2 only, as
; published by the Free Software Foundation.
;
; This code is distributed in the hope that it will be useful, but WITHOUT
; ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
; FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
; version 2 for more details (a copy is included in the LICENSE file that
; accompanied this code).
;
; You should have received a copy of the GNU General Public License version
; 2 along with this work; if not, write to the Free Software Foundation,
; Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
;
; Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
; or visit www.oracle.com if you need additional information or have any
; questions.

; This file contains duplicate entries as globalDefinitions_vecApi.hpp
; It is intended for inclusion in .s files compiled with masm

; Used to check whether building on x86_64 architecture. Equivalent to checking in regular hpp file for #ifdef _WIN64
IFDEF RAX

; @Version is defined by MASM to determine the Visual Studio version. 1410 is the version for VS17
IF @Version GE 1410
__VECTOR_API_MATH_INTRINSICS_WINDOWS TEXTEQU <"vector_api">
ELSE
__VECTOR_API_MATH_INTRINSICS_WINDOWS TEXTEQU <>
ENDIF

ELSE
__VECTOR_API_MATH_INTRINSICS_WINDOWS TEXTEQU <>
ENDIF
