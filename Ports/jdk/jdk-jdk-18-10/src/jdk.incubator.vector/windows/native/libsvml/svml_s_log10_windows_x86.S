;
; Copyright (c) 2018, 2021, Intel Corporation.
; Intel Short Vector Math Library (SVML) Source Code
;
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
;

INCLUDE globals_vectorApiSupport_windows.S.inc
IFNB __VECTOR_API_MATH_INTRINSICS_WINDOWS
	OPTION DOTNAME

_TEXT	SEGMENT      'CODE'

TXTST0:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log10f4_ha_e9

__svml_log10f4_ha_e9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 232
        vmovaps   xmm5, xmm0
        vmovups   XMMWORD PTR [192+rsp], xmm8
        lea       rax, QWORD PTR [__ImageBase]
        mov       QWORD PTR [208+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vandps    xmm3, xmm5, XMMWORD PTR [__svml_slog10_ha_data_internal+1280]
        and       r13, -64
        vorps     xmm8, xmm3, XMMWORD PTR [__svml_slog10_ha_data_internal+1344]
        vrcpps    xmm3, xmm8
        vcmpltps  xmm4, xmm5, XMMWORD PTR [__svml_slog10_ha_data_internal+1408]
        vcmpnleps xmm2, xmm5, XMMWORD PTR [__svml_slog10_ha_data_internal+1472]
        vmovups   xmm1, XMMWORD PTR [__svml_slog10_ha_data_internal+1600]
        vorps     xmm4, xmm4, xmm2
        vmulps    xmm0, xmm3, xmm1
        vpsrld    xmm3, xmm5, 23
        vcvtdq2ps xmm3, xmm3
        vmovmskps edx, xmm4
        vandps    xmm2, xmm8, XMMWORD PTR [__svml_slog10_ha_data_internal+1536]
        vroundps  xmm0, xmm0, 0
        vsubps    xmm8, xmm8, xmm2
        vmulps    xmm4, xmm0, xmm2
        vmulps    xmm2, xmm0, xmm8
        vsubps    xmm1, xmm4, xmm1
        mov       QWORD PTR [216+rsp], r13
        vaddps    xmm8, xmm2, xmm1
        vpsrld    xmm2, xmm0, 13
        vpextrd   r9d, xmm2, 2
        vpextrd   r10d, xmm2, 3
        vmovd     ecx, xmm2
        movsxd    r9, r9d
        movsxd    r10, r10d
        vpextrd   r8d, xmm2, 1
        movsxd    rcx, ecx
        movsxd    r8, r8d
        vmovq     xmm1, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r9]
        vmovq     xmm2, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r10]
        vunpcklps xmm1, xmm1, xmm2
        vmulps    xmm2, xmm3, XMMWORD PTR [__svml_slog10_ha_data_internal+1664]
        vmulps    xmm3, xmm3, XMMWORD PTR [__svml_slog10_ha_data_internal+1728]
        vmovq     xmm4, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+rcx]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r8]
        vunpcklps xmm0, xmm4, xmm0
        vmovlhps  xmm4, xmm0, xmm1
        vshufps   xmm0, xmm0, xmm1, 238
        vaddps    xmm4, xmm4, xmm2
        vaddps    xmm2, xmm0, xmm3
        vmulps    xmm0, xmm8, XMMWORD PTR [__svml_slog10_ha_data_internal+1088]
        vaddps    xmm3, xmm0, XMMWORD PTR [__svml_slog10_ha_data_internal+1152]
        vmulps    xmm1, xmm8, xmm3
        vaddps    xmm3, xmm8, xmm4
        vaddps    xmm1, xmm1, XMMWORD PTR [__svml_slog10_ha_data_internal+1216]
        vsubps    xmm4, xmm3, xmm4
        vsubps    xmm4, xmm8, xmm4
        vmulps    xmm8, xmm8, xmm1
        vaddps    xmm0, xmm2, xmm8
        vaddps    xmm1, xmm4, xmm0
        vaddps    xmm0, xmm3, xmm1
        test      edx, edx
        jne       _B1_3

_B1_2::

        vmovups   xmm8, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [208+rsp]
        add       rsp, 232
        ret

_B1_3::

        vmovups   XMMWORD PTR [r13], xmm5
        vmovups   XMMWORD PTR [64+r13], xmm0

_B1_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B1_7::

        bt        esi, ebx
        jc        _B1_10

_B1_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B1_7

_B1_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovups   xmm0, XMMWORD PTR [64+r13]
        jmp       _B1_2

_B1_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_slog10_ha_cout_rare_internal
        jmp       _B1_8
        ALIGN     16

_B1_11::

__svml_log10f4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log10f4_ha_e9_B1_B3:
	DD	403201
	DD	1758247
	DD	821272
	DD	1900811

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_log10f4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log10f4_ha_e9_B6_B10:
	DD	658945
	DD	287758
	DD	340999
	DD	821248
	DD	1758208
	DD	1900800

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_6
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_log10f4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log10f8_ha_l9

__svml_log10f8_ha_l9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L10::

        sub       rsp, 552
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [368+rsp], ymm13
        vmovups   YMMWORD PTR [400+rsp], ymm10
        vmovups   YMMWORD PTR [496+rsp], ymm9
        vmovups   YMMWORD PTR [464+rsp], ymm8
        vmovups   YMMWORD PTR [432+rsp], ymm7
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [271+rsp]
        vmovaps   ymm4, ymm0
        and       r13, -64
        vandps    ymm2, ymm4, YMMWORD PTR [__svml_slog10_ha_data_internal+1280]
        vorps     ymm2, ymm2, YMMWORD PTR [__svml_slog10_ha_data_internal+1344]
        vrcpps    ymm3, ymm2
        vmovups   ymm9, YMMWORD PTR [__svml_slog10_ha_data_internal+1600]
        vpsrld    ymm5, ymm4, 23
        vcmplt_oqps ymm10, ymm4, YMMWORD PTR [__svml_slog10_ha_data_internal+1408]
        vcmpnle_uqps ymm13, ymm4, YMMWORD PTR [__svml_slog10_ha_data_internal+1472]
        vmulps    ymm1, ymm3, ymm9
        vcvtdq2ps ymm3, ymm5
        vroundps  ymm0, ymm1, 0
        vpsrld    ymm5, ymm0, 13
        vfmsub213ps ymm2, ymm0, ymm9
        vorps     ymm7, ymm10, ymm13
        vmovmskps edx, ymm7
        mov       QWORD PTR [536+rsp], r13
        vmovd     ecx, xmm5
        vextracti128 xmm9, ymm5, 1
        vpextrd   r8d, xmm5, 1
        vpextrd   r9d, xmm5, 2
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movsxd    r9, r9d
        vmovd     r11d, xmm9
        vmovq     xmm10, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+rcx]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r8]
        vpextrd   r10d, xmm5, 3
        vmovq     xmm13, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r9]
        vpextrd   ecx, xmm9, 1
        vpextrd   r8d, xmm9, 2
        vpextrd   r9d, xmm9, 3
        movsxd    r10, r10d
        movsxd    r11, r11d
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movsxd    r9, r9d
        vmovq     xmm1, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r10]
        vmovq     xmm5, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r11]
        vmovq     xmm7, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+rcx]
        vmovq     xmm8, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r8]
        vmovq     xmm9, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r9]
        vunpcklps xmm13, xmm10, xmm13
        vunpcklps xmm10, xmm0, xmm1
        vunpcklps xmm1, xmm5, xmm8
        vunpcklps xmm8, xmm7, xmm9
        vinsertf128 ymm0, ymm13, xmm1, 1
        vinsertf128 ymm1, ymm10, xmm8, 1
        vmovups   ymm10, YMMWORD PTR [__svml_slog10_ha_data_internal+1088]
        vunpcklps ymm7, ymm0, ymm1
        vfmadd213ps ymm10, ymm2, YMMWORD PTR [__svml_slog10_ha_data_internal+1152]
        vfmadd231ps ymm7, ymm3, YMMWORD PTR [__svml_slog10_ha_data_internal+1664]
        vfmadd213ps ymm10, ymm2, YMMWORD PTR [__svml_slog10_ha_data_internal+1216]
        vaddps    ymm13, ymm2, ymm7
        vunpckhps ymm5, ymm0, ymm1
        vfmadd132ps ymm3, ymm5, YMMWORD PTR [__svml_slog10_ha_data_internal+1728]
        vsubps    ymm8, ymm13, ymm7
        vfmadd213ps ymm10, ymm2, ymm3
        vsubps    ymm9, ymm2, ymm8
        vaddps    ymm2, ymm9, ymm10
        vaddps    ymm0, ymm13, ymm2
        test      edx, edx
        jne       _B2_3

_B2_2::

        vmovups   ymm7, YMMWORD PTR [432+rsp]
        vmovups   ymm8, YMMWORD PTR [464+rsp]
        vmovups   ymm9, YMMWORD PTR [496+rsp]
        vmovups   ymm10, YMMWORD PTR [400+rsp]
        vmovups   ymm13, YMMWORD PTR [368+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B2_3::

        vmovups   YMMWORD PTR [r13], ymm4
        vmovups   YMMWORD PTR [64+r13], ymm0

_B2_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [160+rsp], ymm6
        vmovups   YMMWORD PTR [128+rsp], ymm11
        vmovups   YMMWORD PTR [96+rsp], ymm12
        vmovups   YMMWORD PTR [64+rsp], ymm14
        vmovups   YMMWORD PTR [32+rsp], ymm15
        mov       QWORD PTR [200+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [192+rsp], rsi
        mov       esi, edx

_B2_7::

        bt        esi, ebx
        jc        _B2_10

_B2_8::

        inc       ebx
        cmp       ebx, 8
        jl        _B2_7

_B2_9::

        vmovups   ymm6, YMMWORD PTR [160+rsp]
        vmovups   ymm11, YMMWORD PTR [128+rsp]
        vmovups   ymm12, YMMWORD PTR [96+rsp]
        vmovups   ymm14, YMMWORD PTR [64+rsp]
        vmovups   ymm15, YMMWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [200+rsp]
        mov       rsi, QWORD PTR [192+rsp]
        jmp       _B2_2

_B2_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_slog10_ha_cout_rare_internal
        jmp       _B2_8
        ALIGN     16

_B2_11::

__svml_log10f8_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log10f8_ha_l9_B1_B3:
	DD	935681
	DD	4379719
	DD	1800255
	DD	1935414
	DD	2070573
	DD	1681444
	DD	1562651
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_log10f8_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log10f8_ha_l9_B6_B10:
	DD	931873
	DD	1598520
	DD	1651758
	DD	194598
	DD	321568
	DD	444442
	DD	571412
	DD	681995
	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_log10f8_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_6
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_log10f8_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log10f4_ha_l9

__svml_log10f4_ha_l9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L37::

        sub       rsp, 232
        vmovaps   xmm4, xmm0
        vmovups   XMMWORD PTR [192+rsp], xmm14
        lea       rax, QWORD PTR [__ImageBase]
        mov       QWORD PTR [208+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vandps    xmm2, xmm4, XMMWORD PTR [__svml_slog10_ha_data_internal+1280]
        and       r13, -64
        vorps     xmm2, xmm2, XMMWORD PTR [__svml_slog10_ha_data_internal+1344]
        vrcpps    xmm3, xmm2
        vcmpltps  xmm5, xmm4, XMMWORD PTR [__svml_slog10_ha_data_internal+1408]
        vcmpnleps xmm14, xmm4, XMMWORD PTR [__svml_slog10_ha_data_internal+1472]
        vmovups   xmm0, XMMWORD PTR [__svml_slog10_ha_data_internal+1600]
        vorps     xmm5, xmm5, xmm14
        vmulps    xmm1, xmm3, xmm0
        vpsrld    xmm3, xmm4, 23
        vcvtdq2ps xmm3, xmm3
        vmovmskps edx, xmm5
        vroundps  xmm1, xmm1, 0
        vfmsub213ps xmm2, xmm1, xmm0
        vpsrld    xmm1, xmm1, 13
        vmovd     ecx, xmm1
        vpextrd   r8d, xmm1, 1
        vpextrd   r9d, xmm1, 2
        vpextrd   r10d, xmm1, 3
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movsxd    r9, r9d
        movsxd    r10, r10d
        vmovq     xmm14, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+rcx]
        vmovq     xmm5, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r8]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r9]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r10]
        vunpcklps xmm5, xmm14, xmm5
        vunpcklps xmm14, xmm0, xmm1
        vmovlhps  xmm0, xmm5, xmm14
        vfmadd231ps xmm0, xmm3, XMMWORD PTR [__svml_slog10_ha_data_internal+1664]
        vshufps   xmm5, xmm5, xmm14, 238
        vmovups   xmm14, XMMWORD PTR [__svml_slog10_ha_data_internal+1088]
        vfmadd213ps xmm14, xmm2, XMMWORD PTR [__svml_slog10_ha_data_internal+1152]
        vfmadd132ps xmm3, xmm5, XMMWORD PTR [__svml_slog10_ha_data_internal+1728]
        vaddps    xmm5, xmm2, xmm0
        vfmadd213ps xmm14, xmm2, XMMWORD PTR [__svml_slog10_ha_data_internal+1216]
        vsubps    xmm0, xmm5, xmm0
        vfmadd213ps xmm14, xmm2, xmm3
        vsubps    xmm0, xmm2, xmm0
        mov       QWORD PTR [216+rsp], r13
        vaddps    xmm2, xmm0, xmm14
        vaddps    xmm0, xmm5, xmm2
        test      edx, edx
        jne       _B3_3

_B3_2::

        vmovups   xmm14, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [208+rsp]
        add       rsp, 232
        ret

_B3_3::

        vmovups   XMMWORD PTR [r13], xmm4
        vmovups   XMMWORD PTR [64+r13], xmm0

_B3_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B3_7::

        bt        esi, ebx
        jc        _B3_10

_B3_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B3_7

_B3_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovups   xmm0, XMMWORD PTR [64+r13]
        jmp       _B3_2

_B3_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_slog10_ha_cout_rare_internal
        jmp       _B3_8
        ALIGN     16

_B3_11::

__svml_log10f4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log10f4_ha_l9_B1_B3:
	DD	403201
	DD	1758247
	DD	845848
	DD	1900811

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_log10f4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log10f4_ha_l9_B6_B10:
	DD	658945
	DD	287758
	DD	340999
	DD	845824
	DD	1758208
	DD	1900800

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_6
	DD	imagerel _B3_11
	DD	imagerel _unwind___svml_log10f4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log10f4_ha_ex

__svml_log10f4_ha_ex	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L46::

        sub       rsp, 232
        movaps    xmm5, xmm0
        movups    XMMWORD PTR [192+rsp], xmm8
        movaps    xmm4, xmm5
        mov       QWORD PTR [208+rsp], r13
        movaps    xmm2, xmm5
        movups    xmm8, XMMWORD PTR [__svml_slog10_ha_data_internal+1280]
        lea       rax, QWORD PTR [__ImageBase]
        andps     xmm8, xmm5
        lea       r13, QWORD PTR [111+rsp]
        orps      xmm8, XMMWORD PTR [__svml_slog10_ha_data_internal+1344]
        and       r13, -64
        rcpps     xmm0, xmm8
        cmpltps   xmm4, XMMWORD PTR [__svml_slog10_ha_data_internal+1408]
        cmpnleps  xmm2, XMMWORD PTR [__svml_slog10_ha_data_internal+1472]
        movups    xmm3, XMMWORD PTR [__svml_slog10_ha_data_internal+1600]
        orps      xmm4, xmm2
        mulps     xmm0, xmm3
        movmskps  edx, xmm4
        movups    xmm1, XMMWORD PTR [_2il0floatpacket_20]
        movups    xmm4, XMMWORD PTR [__svml_slog10_ha_data_internal+1536]
        addps     xmm0, xmm1
        andps     xmm4, xmm8
        subps     xmm0, xmm1
        subps     xmm8, xmm4
        mulps     xmm4, xmm0
        mulps     xmm8, xmm0
        subps     xmm4, xmm3
        psrld     xmm0, 13
        movaps    xmm1, xmm5
        pshufd    xmm2, xmm0, 1
        psrld     xmm1, 23
        movd      ecx, xmm0
        addps     xmm8, xmm4
        cvtdq2ps  xmm1, xmm1
        movd      r8d, xmm2
        pshufd    xmm2, xmm0, 2
        pshufd    xmm0, xmm0, 3
        movd      r9d, xmm2
        movd      r10d, xmm0
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movsxd    r9, r9d
        movsxd    r10, r10d
        movq      xmm4, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+rcx]
        movq      xmm3, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r8]
        movq      xmm2, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r9]
        movq      xmm0, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r10]
        unpcklps  xmm4, xmm3
        unpcklps  xmm2, xmm0
        movaps    xmm3, xmm4
        movlhps   xmm3, xmm2
        movaps    xmm0, xmm8
        shufps    xmm4, xmm2, 238
        movups    xmm2, XMMWORD PTR [__svml_slog10_ha_data_internal+1664]
        mulps     xmm2, xmm1
        mulps     xmm1, XMMWORD PTR [__svml_slog10_ha_data_internal+1728]
        addps     xmm3, xmm2
        addps     xmm4, xmm1
        addps     xmm0, xmm3
        movups    xmm1, XMMWORD PTR [__svml_slog10_ha_data_internal+1088]
        movaps    xmm2, xmm0
        mulps     xmm1, xmm8
        subps     xmm2, xmm3
        movaps    xmm3, xmm8
        addps     xmm1, XMMWORD PTR [__svml_slog10_ha_data_internal+1152]
        subps     xmm3, xmm2
        mulps     xmm1, xmm8
        mov       QWORD PTR [216+rsp], r13
        addps     xmm1, XMMWORD PTR [__svml_slog10_ha_data_internal+1216]
        mulps     xmm8, xmm1
        addps     xmm4, xmm8
        addps     xmm3, xmm4
        addps     xmm0, xmm3
        test      edx, edx
        jne       _B4_3

_B4_2::

        movups    xmm8, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [208+rsp]
        add       rsp, 232
        ret

_B4_3::

        movups    XMMWORD PTR [r13], xmm5
        movups    XMMWORD PTR [64+r13], xmm0

_B4_6::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B4_7::

        mov       ecx, ebx
        mov       eax, 1
        shl       eax, cl
        test      esi, eax
        jne       _B4_10

_B4_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B4_7

_B4_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [64+r13]
        jmp       _B4_2

_B4_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_slog10_ha_cout_rare_internal
        jmp       _B4_8
        ALIGN     16

_B4_11::

__svml_log10f4_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log10f4_ha_ex_B1_B3:
	DD	401921
	DD	1758242
	DD	821271
	DD	1900811

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_log10f4_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log10f4_ha_ex_B6_B10:
	DD	658945
	DD	287758
	DD	340999
	DD	821248
	DD	1758208
	DD	1900800

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_6
	DD	imagerel _B4_11
	DD	imagerel _unwind___svml_log10f4_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log10f16_ha_z0

__svml_log10f16_ha_z0	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L55::

        vmovaps   zmm2, zmm0
        vgetmantps zmm4, zmm2, 10 {sae}
        vgetexpps zmm23, zmm2 {sae}
        vfpclassps k1, zmm2, 94
        vrcp14ps  zmm3, zmm4
        vrndscaleps zmm5, zmm3, 88 {sae}
        vmovups   zmm1, ZMMWORD PTR [__svml_slog10_ha_data_internal_avx512+256]
        vgetexpps zmm24, zmm5 {sae}
        vmovups   zmm0, ZMMWORD PTR [__svml_slog10_ha_data_internal_avx512]
        vmovups   zmm25, ZMMWORD PTR [__svml_slog10_ha_data_internal_avx512+640]
        vmovups   zmm28, ZMMWORD PTR [__svml_slog10_ha_data_internal_avx512+320]
        vfmsub231ps zmm1, zmm4, zmm5 {rn-sae}
        vmovups   zmm4, ZMMWORD PTR [__svml_slog10_ha_data_internal_avx512+384]
        vmovups   zmm29, ZMMWORD PTR [__svml_slog10_ha_data_internal_avx512+448]
        vmovups   zmm31, ZMMWORD PTR [__svml_slog10_ha_data_internal_avx512+512]
        vmovups   zmm27, ZMMWORD PTR [__svml_slog10_ha_data_internal_avx512+128]
        vmovups   zmm26, ZMMWORD PTR [__svml_slog10_ha_data_internal_avx512+704]
        vfmadd231ps zmm4, zmm28, zmm1 {rn-sae}
        vmovups   zmm28, ZMMWORD PTR [__svml_slog10_ha_data_internal_avx512+576]
        vfmadd231ps zmm31, zmm29, zmm1 {rn-sae}
        vmulps    zmm30, zmm1, zmm1 {rn-sae}
        vpsrld    zmm22, zmm5, 18
        vsubps    zmm3, zmm23, zmm24 {rn-sae}
        vfmadd213ps zmm4, zmm30, zmm31 {rn-sae}
        vpermt2ps zmm0, zmm22, ZMMWORD PTR [__svml_slog10_ha_data_internal_avx512+64]
        vpermt2ps zmm27, zmm22, ZMMWORD PTR [__svml_slog10_ha_data_internal_avx512+192]
        vfmadd231ps zmm0, zmm25, zmm3 {rn-sae}
        vfmadd213ps zmm3, zmm26, zmm27 {rn-sae}
        vmovaps   zmm5, zmm1
        vfmadd213ps zmm5, zmm28, zmm0 {rn-sae}
        vfmadd213ps zmm4, zmm1, zmm3 {rn-sae}
        vsubps    zmm0, zmm5, zmm0 {rn-sae}
        vxorps    zmm0{k1}, zmm0, zmm0
        vfmsub213ps zmm1, zmm28, zmm0 {rn-sae}
        vaddps    zmm1, zmm4, zmm1 {rn-sae}
        vaddps    zmm0, zmm5, zmm1 {rn-sae}
        ret
        ALIGN     16

_B5_2::

__svml_log10f16_ha_z0 ENDP

_TEXT	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log10f8_ha_e9

__svml_log10f8_ha_e9	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L56::

        sub       rsp, 552
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [496+rsp], ymm13
        vmovups   YMMWORD PTR [368+rsp], ymm10
        vmovups   YMMWORD PTR [432+rsp], ymm9
        vmovups   YMMWORD PTR [400+rsp], ymm8
        vmovups   YMMWORD PTR [464+rsp], ymm7
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [271+rsp]
        vmovaps   ymm13, ymm0
        and       r13, -64
        vandps    ymm2, ymm13, YMMWORD PTR [__svml_slog10_ha_data_internal+1280]
        vorps     ymm4, ymm2, YMMWORD PTR [__svml_slog10_ha_data_internal+1344]
        vrcpps    ymm9, ymm4
        vmovups   ymm3, YMMWORD PTR [__svml_slog10_ha_data_internal+1600]
        vmulps    ymm8, ymm9, ymm3
        vroundps  ymm8, ymm8, 0
        vandps    ymm7, ymm4, YMMWORD PTR [__svml_slog10_ha_data_internal+1536]
        vsubps    ymm4, ymm4, ymm7
        mov       QWORD PTR [536+rsp], r13
        vpsrld    xmm2, xmm13, 23
        vcmplt_oqps ymm5, ymm13, YMMWORD PTR [__svml_slog10_ha_data_internal+1408]
        vcmpnle_uqps ymm1, ymm13, YMMWORD PTR [__svml_slog10_ha_data_internal+1472]
        vextractf128 xmm10, ymm13, 1
        vorps     ymm1, ymm5, ymm1
        vpsrld    xmm10, xmm10, 23
        vextractf128 xmm5, ymm1, 1
        vinsertf128 ymm0, ymm2, xmm10, 1
        vpackssdw xmm10, xmm1, xmm5
        vpxor     xmm2, xmm2, xmm2
        vcvtdq2ps ymm9, ymm0
        vpacksswb xmm0, xmm10, xmm2
        vmulps    ymm10, ymm8, ymm7
        vmulps    ymm2, ymm8, ymm4
        vpmovmskb edx, xmm0
        vsubps    ymm3, ymm10, ymm3
        vaddps    ymm10, ymm2, ymm3
        vpsrld    xmm3, xmm8, 13
        vmovd     ecx, xmm3
        vextractf128 xmm2, ymm8, 1
        vpextrd   r8d, xmm3, 1
        vpsrld    xmm7, xmm2, 13
        vpextrd   r9d, xmm3, 2
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movsxd    r9, r9d
        vmovd     r11d, xmm7
        vmovq     xmm0, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+rcx]
        vmovq     xmm2, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r8]
        vpextrd   r10d, xmm3, 3
        vmovq     xmm1, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r9]
        vpextrd   ecx, xmm7, 1
        vpextrd   r8d, xmm7, 2
        vpextrd   r9d, xmm7, 3
        movsxd    r10, r10d
        movsxd    r11, r11d
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movsxd    r9, r9d
        vmovq     xmm3, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r10]
        vmovq     xmm4, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r11]
        vmovq     xmm8, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+rcx]
        vmovq     xmm5, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r8]
        vmovq     xmm7, QWORD PTR [imagerel(__svml_slog10_ha_data_internal)-136944+rax+r9]
        vunpcklps xmm2, xmm2, xmm3
        vunpcklps xmm0, xmm0, xmm1
        vunpcklps xmm1, xmm4, xmm5
        vunpcklps xmm4, xmm8, xmm7
        vinsertf128 ymm7, ymm2, xmm4, 1
        vmulps    ymm2, ymm10, YMMWORD PTR [__svml_slog10_ha_data_internal+1088]
        vaddps    ymm3, ymm2, YMMWORD PTR [__svml_slog10_ha_data_internal+1152]
        vmulps    ymm4, ymm10, ymm3
        vinsertf128 ymm5, ymm0, xmm1, 1
        vmulps    ymm0, ymm9, YMMWORD PTR [__svml_slog10_ha_data_internal+1664]
        vmulps    ymm9, ymm9, YMMWORD PTR [__svml_slog10_ha_data_internal+1728]
        vunpcklps ymm8, ymm5, ymm7
        vaddps    ymm8, ymm8, ymm0
        vaddps    ymm0, ymm10, ymm8
        vunpckhps ymm1, ymm5, ymm7
        vaddps    ymm7, ymm4, YMMWORD PTR [__svml_slog10_ha_data_internal+1216]
        vsubps    ymm5, ymm0, ymm8
        vaddps    ymm9, ymm1, ymm9
        vsubps    ymm5, ymm10, ymm5
        vmulps    ymm10, ymm10, ymm7
        vaddps    ymm7, ymm9, ymm10
        vaddps    ymm1, ymm5, ymm7
        vaddps    ymm0, ymm0, ymm1
        test      dl, dl
        jne       _B6_3

_B6_2::

        vmovups   ymm7, YMMWORD PTR [464+rsp]
        vmovups   ymm8, YMMWORD PTR [400+rsp]
        vmovups   ymm9, YMMWORD PTR [432+rsp]
        vmovups   ymm10, YMMWORD PTR [368+rsp]
        vmovups   ymm13, YMMWORD PTR [496+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B6_3::

        vmovups   YMMWORD PTR [r13], ymm13
        vmovups   YMMWORD PTR [64+r13], ymm0
        test      edx, edx
        je        _B6_2

_B6_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [160+rsp], ymm6
        vmovups   YMMWORD PTR [128+rsp], ymm11
        vmovups   YMMWORD PTR [96+rsp], ymm12
        vmovups   YMMWORD PTR [64+rsp], ymm14
        vmovups   YMMWORD PTR [32+rsp], ymm15
        mov       QWORD PTR [200+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [192+rsp], rsi
        mov       esi, edx

_B6_7::

        bt        esi, ebx
        jc        _B6_10

_B6_8::

        inc       ebx
        cmp       ebx, 8
        jl        _B6_7

_B6_9::

        vmovups   ymm6, YMMWORD PTR [160+rsp]
        vmovups   ymm11, YMMWORD PTR [128+rsp]
        vmovups   ymm12, YMMWORD PTR [96+rsp]
        vmovups   ymm14, YMMWORD PTR [64+rsp]
        vmovups   ymm15, YMMWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [200+rsp]
        mov       rsi, QWORD PTR [192+rsp]
        jmp       _B6_2

_B6_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_slog10_ha_cout_rare_internal
        jmp       _B6_8
        ALIGN     16

_B6_11::

__svml_log10f8_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log10f8_ha_e9_B1_B3:
	DD	935681
	DD	4379719
	DD	1931327
	DD	1673270
	DD	1808429
	DD	1550372
	DD	2086939
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_log10f8_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log10f8_ha_e9_B6_B10:
	DD	931873
	DD	1598520
	DD	1651758
	DD	194598
	DD	321568
	DD	444442
	DD	571412
	DD	681995
	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_log10f8_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_6
	DD	imagerel _B6_11
	DD	imagerel _unwind___svml_log10f8_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_slog10_ha_cout_rare_internal

__svml_slog10_ha_cout_rare_internal	PROC

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L83::

        sub       rsp, 56
        mov       r9, rcx
        xor       eax, eax
        movzx     ecx, WORD PTR [2+r9]
        mov       r8d, ecx
        and       r8d, 32640
        cmp       r8d, 32640
        je        _B7_12

_B7_2::

        movss     xmm0, DWORD PTR [r9]
        xor       r8d, r8d
        movss     DWORD PTR [40+rsp], xmm0
        test      ecx, 32640
        jne       _B7_4

_B7_3::

        mulss     xmm0, DWORD PTR [_2il0floatpacket_93]
        mov       r8d, -40
        movss     DWORD PTR [40+rsp], xmm0

_B7_4::

        pxor      xmm1, xmm1
        comiss    xmm0, xmm1
        jbe       _B7_8

_B7_5::

        movaps    xmm2, xmm0
        subss     xmm2, DWORD PTR [_2il0floatpacket_109]
        movss     DWORD PTR [36+rsp], xmm2
        and       BYTE PTR [39+rsp], 127
        movss     xmm1, DWORD PTR [36+rsp]
        comiss    xmm1, DWORD PTR [_2il0floatpacket_94]
        jbe       _B7_7

_B7_6::

        movzx     ecx, WORD PTR [42+rsp]
        pxor      xmm3, xmm3
        and       ecx, 32640
        shr       ecx, 7
        movss     DWORD PTR [36+rsp], xmm0
        movzx     r9d, WORD PTR [38+rsp]
        and       r9d, -32641
        add       r9d, 16256
        lea       r8d, DWORD PTR [-127+r8+rcx]
        cvtsi2ss  xmm3, r8d
        mov       WORD PTR [38+rsp], r9w
        lea       rcx, QWORD PTR [__ImageBase]
        movss     xmm4, DWORD PTR [36+rsp]
        mov       r9, rcx
        movaps    xmm0, xmm4
        mov       r8, rcx
        movss     xmm2, DWORD PTR [_2il0floatpacket_95]
        movss     xmm1, DWORD PTR [_2il0floatpacket_96]
        movss     xmm5, DWORD PTR [_2il0floatpacket_98]
        mulss     xmm2, xmm3
        addss     xmm0, DWORD PTR [_2il0floatpacket_97]
        mulss     xmm3, xmm1
        movaps    xmm1, xmm5
        movss     DWORD PTR [32+rsp], xmm0
        addss     xmm1, xmm4
        mov       r10d, DWORD PTR [32+rsp]
        movss     DWORD PTR [32+rsp], xmm1
        and       r10d, 127
        movss     xmm1, DWORD PTR [32+rsp]
        subss     xmm1, xmm5
        lea       r11d, DWORD PTR [r10+r10*2]
        movss     xmm0, DWORD PTR [imagerel(_vmldLgHATab)+rcx+r11*4]
        subss     xmm4, xmm1
        addss     xmm3, DWORD PTR [imagerel(_vmldLgHATab)+8+r9+r11*4]
        addss     xmm2, DWORD PTR [imagerel(_vmldLgHATab)+4+r8+r11*4]
        mulss     xmm1, xmm0
        mulss     xmm0, xmm4
        subss     xmm1, DWORD PTR [_2il0floatpacket_99]
        movaps    xmm5, xmm1
        addss     xmm2, xmm1
        addss     xmm5, xmm0
        movss     xmm4, DWORD PTR [_2il0floatpacket_100]
        mulss     xmm4, xmm5
        addss     xmm4, DWORD PTR [_2il0floatpacket_101]
        mulss     xmm4, xmm5
        addss     xmm4, DWORD PTR [_2il0floatpacket_102]
        mulss     xmm4, xmm5
        addss     xmm4, DWORD PTR [_2il0floatpacket_103]
        mulss     xmm4, xmm5
        addss     xmm4, DWORD PTR [_2il0floatpacket_104]
        mulss     xmm4, xmm5
        addss     xmm4, DWORD PTR [_2il0floatpacket_105]
        mulss     xmm4, xmm5
        addss     xmm4, DWORD PTR [_2il0floatpacket_106]
        mulss     xmm4, xmm5
        movaps    xmm5, xmm0
        addss     xmm4, DWORD PTR [_2il0floatpacket_107]
        mulss     xmm5, xmm4
        mulss     xmm1, xmm4
        addss     xmm3, xmm5
        addss     xmm0, xmm3
        addss     xmm0, xmm1
        addss     xmm2, xmm0
        movss     DWORD PTR [rdx], xmm2
        add       rsp, 56
        ret

_B7_7::

        movss     xmm0, DWORD PTR [_2il0floatpacket_99]
        mulss     xmm2, xmm0
        movss     xmm1, DWORD PTR [_2il0floatpacket_100]
        mulss     xmm1, xmm2
        addss     xmm1, DWORD PTR [_2il0floatpacket_101]
        mulss     xmm1, xmm2
        addss     xmm1, DWORD PTR [_2il0floatpacket_102]
        mulss     xmm1, xmm2
        addss     xmm1, DWORD PTR [_2il0floatpacket_103]
        mulss     xmm1, xmm2
        addss     xmm1, DWORD PTR [_2il0floatpacket_104]
        mulss     xmm1, xmm2
        addss     xmm1, DWORD PTR [_2il0floatpacket_105]
        mulss     xmm1, xmm2
        addss     xmm1, DWORD PTR [_2il0floatpacket_106]
        mulss     xmm1, xmm2
        addss     xmm1, DWORD PTR [_2il0floatpacket_107]
        mulss     xmm1, xmm2
        addss     xmm2, xmm1
        movss     DWORD PTR [rdx], xmm2
        add       rsp, 56
        ret

_B7_8::

        ucomiss   xmm0, xmm1
        jp        _B7_9
        je        _B7_11

_B7_9::

        divss     xmm1, xmm1
        movss     DWORD PTR [rdx], xmm1
        mov       eax, 1

_B7_10::

        add       rsp, 56
        ret

_B7_11::

        movss     xmm0, DWORD PTR [_2il0floatpacket_108]
        mov       eax, 2
        divss     xmm0, xmm1
        movss     DWORD PTR [rdx], xmm0
        add       rsp, 56
        ret

_B7_12::

        mov       cl, BYTE PTR [3+r9]
        and       cl, -128
        cmp       cl, -128
        je        _B7_14

_B7_13::

        movss     xmm0, DWORD PTR [r9]
        mulss     xmm0, xmm0
        movss     DWORD PTR [rdx], xmm0
        add       rsp, 56
        ret

_B7_14::

        test      DWORD PTR [r9], 8388607
        jne       _B7_13

_B7_15::

        mov       eax, 1
        pxor      xmm1, xmm1
        pxor      xmm0, xmm0
        divss     xmm1, xmm0
        movss     DWORD PTR [rdx], xmm1
        add       rsp, 56
        ret
        ALIGN     16

_B7_16::

__svml_slog10_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_slog10_ha_cout_rare_internal_B1_B15:
	DD	67585
	DD	25096

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_16
	DD	imagerel _unwind___svml_slog10_ha_cout_rare_internal_B1_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_slog10_ha_data_internal_avx512
__svml_slog10_ha_data_internal_avx512	DD	1050288128
	DD	1049839616
	DD	1049405440
	DD	1048981504
	DD	1048567808
	DD	1047769088
	DD	1046990848
	DD	1046233088
	DD	1045495808
	DD	1044779008
	DD	1044074496
	DD	1043390464
	DD	1042718720
	DD	1042063360
	DD	1041424384
	DD	1040797696
	DD	1040179200
	DD	1038974976
	DD	1037803520
	DD	1036648448
	DD	1035509760
	DD	1034403840
	DD	1033314304
	DD	1032241152
	DD	1030586368
	DD	1028521984
	DD	1026490368
	DD	1024507904
	DD	1021673472
	DD	1017839616
	DD	1013055488
	DD	1004535808
	DD	916096252
	DD	922116432
	DD	3080225825
	DD	937573077
	DD	3035959522
	DD	907149878
	DD	932083293
	DD	937852153
	DD	932873468
	DD	3083585687
	DD	923506544
	DD	3080111720
	DD	922618327
	DD	929046259
	DD	3073359178
	DD	3075170456
	DD	3027570914
	DD	932146099
	DD	3086063431
	DD	3082882946
	DD	937977627
	DD	3064614024
	DD	3065085585
	DD	934503987
	DD	923757491
	DD	927716856
	DD	937794272
	DD	3074368796
	DD	929297206
	DD	3083361151
	DD	3065901602
	DD	912917177
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	3185469915
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	1041515222
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	3193854937
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	2990071104
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	1050288128
	DD	916096252
	DD	916096252
	DD	916096252
	DD	916096252
	DD	916096252
	DD	916096252
	DD	916096252
	DD	916096252
	DD	916096252
	DD	916096252
	DD	916096252
	DD	916096252
	DD	916096252
	DD	916096252
	DD	916096252
	DD	916096252
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	124
	DD	124
	DD	124
	DD	124
	DD	124
	DD	124
	DD	124
	DD	124
	DD	124
	DD	124
	DD	124
	DD	124
	DD	124
	DD	124
	DD	124
	DD	124
	PUBLIC __svml_slog10_ha_data_internal
__svml_slog10_ha_data_internal	DD	3256334460
	DD	969394728
	DD	3256334974
	DD	969699354
	DD	3256335486
	DD	970041911
	DD	3256335982
	DD	968584717
	DD	3256336494
	DD	969519438
	DD	3256336990
	DD	968649168
	DD	3256337502
	DD	970165641
	DD	3256337998
	DD	969872020
	DD	3256338494
	DD	969862955
	DD	3256338990
	DD	970135976
	DD	3256339470
	DD	968591496
	DD	3256339966
	DD	969421411
	DD	3256340462
	DD	970526195
	DD	3256340942
	DD	969806353
	DD	3256341422
	DD	969356721
	DD	3256341902
	DD	969175014
	DD	3256342382
	DD	969258977
	DD	3256342862
	DD	969606381
	DD	3256343342
	DD	970215027
	DD	3256343806
	DD	968985590
	DD	3256344286
	DD	970110228
	DD	3256344750
	DD	969392519
	DD	3256345214
	DD	968927522
	DD	3256345678
	DD	968713169
	DD	3256346142
	DD	968747418
	DD	3256346606
	DD	969028253
	DD	3256347070
	DD	969553681
	DD	3256347534
	DD	970321733
	DD	3256347982
	DD	969233313
	DD	3256348446
	DD	970480802
	DD	3256348894
	DD	969867999
	DD	3256349342
	DD	969490179
	DD	3256349790
	DD	969345487
	DD	3256350238
	DD	969432093
	DD	3256350686
	DD	969748186
	DD	3256351566
	DD	968964541
	DD	3256352446
	DD	969077639
	DD	3256353326
	DD	970073736
	DD	3256354190
	DD	969842251
	DD	3256355054
	DD	970467209
	DD	3256355902
	DD	969838628
	DD	3256356750
	DD	970041115
	DD	3256357582
	DD	968965252
	DD	3256358414
	DD	968696192
	DD	3256359246
	DD	969222201
	DD	3256360078
	DD	970531798
	DD	3256360894
	DD	970516595
	DD	3256361694
	DD	969165599
	DD	3256362494
	DD	968565200
	DD	3256363294
	DD	968704863
	DD	3256364094
	DD	969574273
	DD	3256364878
	DD	969066179
	DD	3256365662
	DD	969267840
	DD	3256366446
	DD	970169567
	DD	3256367214
	DD	969664714
	DD	3256367982
	DD	969841134
	DD	3256368734
	DD	968592559
	DD	3256369502
	DD	970104362
	DD	3256370254
	DD	970173483
	DD	3256370990
	DD	968791339
	DD	3256371742
	DD	970143819
	DD	3256372478
	DD	970028365
	DD	3256373214
	DD	970534037
	DD	3256373934
	DD	969555746
	DD	3256374654
	DD	969182856
	DD	3256375374
	DD	969407729
	DD	3256376094
	DD	970222869
	DD	3256376798
	DD	969523769
	DD	3256377502
	DD	969400361
	DD	3256378206
	DD	969845561
	DD	3256378894
	DD	968755260
	DD	3256379598
	DD	970316935
	DD	3256380286
	DD	970329575
	DD	3256380958
	DD	968786598
	DD	3256381646
	DD	969875841
	DD	3256382318
	DD	969396649
	DD	3256382990
	DD	969439935
	DD	3256383662
	DD	969999571
	DD	3256384318
	DD	968972383
	DD	3256384990
	DD	970546758
	DD	3256385646
	DD	970522577
	DD	3256386286
	DD	968894127
	DD	3256386942
	DD	969850093
	DD	3256387582
	DD	969190651
	DD	3256388222
	DD	969007524
	DD	3256388862
	DD	969295375
	DD	3256389502
	DD	970048957
	DD	3256390126
	DD	969165958
	DD	3256390750
	DD	968738453
	DD	3256391374
	DD	968761452
	DD	3256391998
	DD	969230043
	DD	3256392622
	DD	970139398
	DD	3256393230
	DD	969387611
	DD	3256393838
	DD	969067159
	DD	3256394446
	DD	969173441
	DD	3256395054
	DD	969701929
	DD	3256395646
	DD	968551015
	DD	3256396254
	DD	969910618
	DD	3256396846
	DD	969582116
	DD	3256397438
	DD	969658411
	DD	3256398030
	DD	970135319
	DD	3256398606
	DD	968911569
	DD	3256399198
	DD	970177409
	DD	3256399774
	DD	969734540
	DD	3256400350
	DD	969676181
	DD	3256400926
	DD	969998457
	DD	3256401486
	DD	968600401
	DD	3256402062
	DD	969672558
	DD	3256402622
	DD	969016921
	DD	3256403182
	DD	968726993
	DD	3256403742
	DD	968799182
	DD	3256404302
	DD	969229944
	DD	3256404862
	DD	970015791
	DD	3256405406
	DD	969056131
	DD	3256405966
	DD	970541879
	DD	3256406510
	DD	970275391
	DD	3256407054
	DD	970350525
	DD	3256407582
	DD	968666884
	DD	3256408126
	DD	969415571
	DD	3256408670
	DD	970496282
	DD	3256409198
	DD	969808752
	DD	3256409726
	DD	969447067
	DD	3256410254
	DD	969408203
	DD	3256410782
	DD	969689179
	DD	3256411310
	DD	970287052
	DD	3256411822
	DD	969101770
	DD	3256412350
	DD	970324777
	DD	3256412862
	DD	969758945
	DD	3256413374
	DD	969498644
	DB 0
	ORG $+54
	DB	0
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	1071862019
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	3214137316
	DD	986960742
	DD	986960742
	DD	986960742
	DD	986960742
	DD	986960742
	DD	986960742
	DD	986960742
	DD	986960742
	DD	986960742
	DD	986960742
	DD	986960742
	DD	986960742
	DD	986960742
	DD	986960742
	DD	986960742
	DD	986960742
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	989855744
	DD	989855744
	DD	989855744
	DD	989855744
	DD	989855744
	DD	989855744
	DD	989855744
	DD	989855744
	DD	989855744
	DD	989855744
	DD	989855744
	DD	989855744
	DD	989855744
	DD	989855744
	DD	989855744
	DD	989855744
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	8388608
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	2139095039
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	4294967040
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1054736384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	1050288384
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	3058365952
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	1059760811
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	8388607
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1065353216
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	1031824308
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	3179872371
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	1031230231
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	3180460839
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	1035078550
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	3185471008
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	1041513701
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	3193854917
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1054759897
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	1036894503
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	3186945393
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	1041464766
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	3193833762
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1054760110
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	1050288283
	DD	2139095040
	DD	4286578688
	DB 0
	ORG $+54
	DB	0
	DD	1065353216
	DD	3212836864
	DB 0
	ORG $+54
	DB	0
	DD	0
	DD	2147483648
	DB 0
	ORG $+54
	DB	0
_vmldLgHATab	DD	1121868800
	DD	0
	DD	0
	DD	1121641104
	DD	1004535808
	DD	912917177
	DD	1121413408
	DD	1013055488
	DD	3065901602
	DD	1121185712
	DD	1017839616
	DD	3083361151
	DD	1120958016
	DD	1021673472
	DD	929297206
	DD	1120844168
	DD	1023524864
	DD	3077496589
	DD	1120616472
	DD	1025499136
	DD	3070500046
	DD	1120388776
	DD	1027506176
	DD	912271551
	DD	1120274928
	DD	1028521984
	DD	927716856
	DD	1120047232
	DD	1030586368
	DD	923757491
	DD	1119933384
	DD	1031634944
	DD	3056752848
	DD	1119705688
	DD	1032775680
	DD	917029265
	DD	1119591840
	DD	1033314304
	DD	3065085585
	DD	1119364144
	DD	1034403840
	DD	3064614024
	DD	1119250296
	DD	1034954752
	DD	921091539
	DD	1119136448
	DD	1035513856
	DD	3057436454
	DD	1118908752
	DD	1036644352
	DD	922468856
	DD	1118794904
	DD	1037219840
	DD	3049155845
	DD	1118681056
	DD	1037799424
	DD	904301451
	DD	1118567208
	DD	1038385152
	DD	908617625
	DD	1118453360
	DD	1038977024
	DD	905362229
	DD	1118225664
	DD	1040179200
	DD	3027570914
	DD	1118111816
	DD	1040488448
	DD	882280038
	DD	1117997968
	DD	1040796672
	DD	911375775
	DD	1117884120
	DD	1041108480
	DD	904500572
	DD	1117770272
	DD	1041423872
	DD	3057579304
	DD	1117656424
	DD	1041742336
	DD	3053334705
	DD	1117542576
	DD	1042064384
	DD	3053389931
	DD	1117428728
	DD	1042390016
	DD	3051561465
	DD	1117314880
	DD	1042719232
	DD	3011187895
	DD	1117201032
	DD	1043052544
	DD	3059907089
	DD	1117087184
	DD	1043389440
	DD	3057005374
	DD	1116973336
	DD	1043729920
	DD	911932638
	DD	1116859488
	DD	1044075008
	DD	892958461
	DD	1116859488
	DD	1044075008
	DD	892958461
	DD	1116745640
	DD	1044424192
	DD	3048660547
	DD	1116631792
	DD	1044777472
	DD	3049032043
	DD	1116517944
	DD	1045134848
	DD	906867152
	DD	1116404096
	DD	1045496832
	DD	911484894
	DD	1116404096
	DD	1045496832
	DD	911484894
	DD	1116290248
	DD	1045863424
	DD	912580963
	DD	1116176400
	DD	1046235136
	DD	3058440244
	DD	1116062552
	DD	1046610944
	DD	895945194
	DD	1116062552
	DD	1046610944
	DD	895945194
	DD	1115948704
	DD	1046991872
	DD	904357324
	DD	1115834856
	DD	1047377920
	DD	902293870
	DD	1115721008
	DD	1047769088
	DD	907149878
	DD	1115721008
	DD	1047769088
	DD	907149878
	DD	1115529456
	DD	1048165888
	DD	3052029263
	DD	1115301760
	DD	1048567808
	DD	3035959522
	DD	1115301760
	DD	1048567808
	DD	3035959522
	DD	1115074064
	DD	1048775680
	DD	892998645
	DD	1115074064
	DD	1048775680
	DD	892998645
	DD	1114846368
	DD	1048982400
	DD	881767775
	DD	1114618672
	DD	1049192064
	DD	893839142
	DD	1114618672
	DD	1049192064
	DD	893839142
	DD	1114390976
	DD	1049404800
	DD	896498651
	DD	1114390976
	DD	1049404800
	DD	896498651
	DD	1114163280
	DD	1049620736
	DD	3033695903
	DD	1114163280
	DD	1049620736
	DD	3033695903
	DD	1113935584
	DD	1049839872
	DD	3029986056
	DD	1113935584
	DD	1049839872
	DD	3029986056
	DD	1113707888
	DD	1050062336
	DD	884671939
	DD	1113707888
	DD	1050062336
	DD	884671939
	DD	1113480192
	DD	1050288256
	DD	894707678
	DD	1050279936
	DD	964848148
	DD	1207959616
	DD	1174405120
	DD	1002438656
	DD	1400897536
	DD	0
	DD	1065353216
	DD	1121868800
	DD	3212771328
	DD	3079888218
	DD	870463078
	DD	2957202361
	DD	749987585
	DD	2838272395
	DD	631921661
	DD	2720751022
_2il0floatpacket_20	DD	04b400000H,04b400000H,04b400000H,04b400000H
_2il0floatpacket_93	DD	053800000H
_2il0floatpacket_94	DD	03bc00000H
_2il0floatpacket_95	DD	03e9a0000H
_2il0floatpacket_96	DD	039826a14H
_2il0floatpacket_97	DD	048000040H
_2il0floatpacket_98	DD	046000000H
_2il0floatpacket_99	DD	042de5c00H
_2il0floatpacket_100	DD	0a22b5daeH
_2il0floatpacket_101	DD	025aa5bfdH
_2il0floatpacket_102	DD	0a92c998bH
_2il0floatpacket_103	DD	02cb3e701H
_2il0floatpacket_104	DD	0b04353b9H
_2il0floatpacket_105	DD	033e23666H
_2il0floatpacket_106	DD	0b7935d5aH
_2il0floatpacket_107	DD	0bf7f0000H
_2il0floatpacket_108	DD	0bf800000H
_2il0floatpacket_109	DD	03f800000H
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
