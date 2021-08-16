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
	PUBLIC __svml_expm14_ha_l9

__svml_expm14_ha_l9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 552
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [304+rsp], ymm15
        vmovups   YMMWORD PTR [336+rsp], ymm14
        vmovups   YMMWORD PTR [368+rsp], ymm13
        vmovups   YMMWORD PTR [400+rsp], ymm12
        vmovups   YMMWORD PTR [432+rsp], ymm11
        vmovups   YMMWORD PTR [464+rsp], ymm10
        vmovups   YMMWORD PTR [496+rsp], ymm7
        vmovups   YMMWORD PTR [272+rsp], ymm6
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [175+rsp]
        vmovdqa   ymm6, ymm0
        and       r13, -64
        vmulpd    ymm5, ymm6, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2304]
        vmovupd   ymm4, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2816]
        vmovupd   ymm3, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2368]
        vroundpd  ymm14, ymm5, 0
        vaddpd    ymm7, ymm14, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2496]
        vmulpd    ymm2, ymm14, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2432]
        vfnmadd213pd ymm3, ymm14, ymm6
        vandps    ymm11, ymm7, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2560]
        vandpd    ymm15, ymm6, ymm4
        vcmpnle_uqpd ymm13, ymm15, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2880]
        vsubpd    ymm0, ymm3, ymm2
        vcmpeqpd  ymm1, ymm3, ymm6
        vmovmskpd edx, ymm13
        vorpd     ymm1, ymm1, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2688]
        test      edx, edx
        vandnpd   ymm5, ymm4, ymm6
        mov       QWORD PTR [536+rsp], r13
        vextracti128 xmm15, ymm11, 1
        vmovd     ecx, xmm11
        vmovd     r9d, xmm15
        vpextrd   r8d, xmm11, 2
        vpextrd   r10d, xmm15, 2
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movsxd    r9, r9d
        movsxd    r10, r10d
        vmovupd   xmm12, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+rax+rcx]
        vmovupd   xmm10, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+rax+r8]
        vmovupd   xmm14, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+rax+r9]
        vmovupd   xmm13, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+rax+r10]
        vunpcklpd xmm15, xmm12, xmm10
        vunpcklpd xmm11, xmm14, xmm13
        vunpckhpd xmm13, xmm14, xmm13
        vandps    ymm14, ymm7, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2624]
        vpsllq    ymm7, ymm14, 41
        vunpckhpd xmm12, xmm12, xmm10
        vmovupd   ymm14, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2048]
        vfmadd213pd ymm14, ymm0, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2112]
        vinsertf128 ymm15, ymm15, xmm11, 1
        vmovupd   ymm11, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2176]
        vfmadd213pd ymm11, ymm0, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2240]
        vorpd     ymm15, ymm15, ymm7
        vinsertf128 ymm10, ymm12, xmm13, 1
        vmulpd    ymm12, ymm0, ymm0
        vmulpd    ymm13, ymm10, ymm7
        vfmadd213pd ymm14, ymm12, ymm11
        vandpd    ymm10, ymm3, ymm1
        vmovupd   ymm1, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2752]
        vmulpd    ymm7, ymm15, ymm10
        vsubpd    ymm3, ymm3, ymm10
        vandpd    ymm0, ymm15, ymm4
        vandpd    ymm4, ymm1, ymm4
        vsubpd    ymm2, ymm3, ymm2
        vcmple_oqpd ymm0, ymm0, ymm4
        vfmadd213pd ymm14, ymm12, ymm2
        vaddpd    ymm12, ymm15, ymm1
        vblendvpd ymm2, ymm15, ymm1, ymm0
        vblendvpd ymm3, ymm1, ymm15, ymm0
        vsubpd    ymm4, ymm2, ymm12
        vaddpd    ymm1, ymm7, ymm12
        vaddpd    ymm10, ymm10, ymm14
        vaddpd    ymm11, ymm3, ymm4
        vsubpd    ymm12, ymm12, ymm1
        vaddpd    ymm0, ymm13, ymm11
        vaddpd    ymm7, ymm7, ymm12
        vfmadd213pd ymm13, ymm10, ymm0
        vfmadd213pd ymm15, ymm14, ymm7
        vaddpd    ymm13, ymm15, ymm13
        vaddpd    ymm14, ymm1, ymm13
        vorpd     ymm0, ymm14, ymm5
        jne       _B1_3

_B1_2::

        vmovups   ymm6, YMMWORD PTR [272+rsp]
        vmovups   ymm7, YMMWORD PTR [496+rsp]
        vmovups   ymm10, YMMWORD PTR [464+rsp]
        vmovups   ymm11, YMMWORD PTR [432+rsp]
        vmovups   ymm12, YMMWORD PTR [400+rsp]
        vmovups   ymm13, YMMWORD PTR [368+rsp]
        vmovups   ymm14, YMMWORD PTR [336+rsp]
        vmovups   ymm15, YMMWORD PTR [304+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B1_3::

        vmovupd   YMMWORD PTR [r13], ymm6
        vmovupd   YMMWORD PTR [64+r13], ymm0

_B1_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [64+rsp], ymm8
        vmovups   YMMWORD PTR [32+rsp], ymm9
        mov       QWORD PTR [104+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [96+rsp], rsi
        mov       esi, edx

_B1_7::

        bt        esi, ebx
        jc        _B1_10

_B1_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B1_7

_B1_9::

        vmovups   ymm8, YMMWORD PTR [64+rsp]
        vmovups   ymm9, YMMWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [104+rsp]
        mov       rsi, QWORD PTR [96+rsp]
        jmp       _B1_2

_B1_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dexpm1_ha_cout_rare_internal
        jmp       _B1_8
        ALIGN     16

_B1_11::

__svml_expm14_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm14_ha_l9_B1_B3:
	DD	1335809
	DD	4379746
	DD	1140826
	DD	2062417
	DD	1943624
	DD	1816639
	DD	1689654
	DD	1562669
	DD	1435684
	DD	1308699
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_expm14_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm14_ha_l9_B6_B10:
	DD	530977
	DD	812058
	DD	865299
	DD	169998
	DD	296968
	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_expm14_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_6
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_expm14_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_expm12_ha_e9

__svml_expm12_ha_e9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L28::

        sub       rsp, 296
        vmovapd   xmm1, xmm0
        vmovups   XMMWORD PTR [208+rsp], xmm15
        lea       r8, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [224+rsp], xmm14
        vmovups   XMMWORD PTR [240+rsp], xmm13
        vmovups   XMMWORD PTR [256+rsp], xmm12
        vmovups   XMMWORD PTR [192+rsp], xmm6
        mov       QWORD PTR [272+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmulpd    xmm12, xmm1, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2304]
        vroundpd  xmm12, xmm12, 0
        and       r13, -64
        vmulpd    xmm4, xmm12, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2368]
        vaddpd    xmm13, xmm12, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2496]
        vsubpd    xmm0, xmm1, xmm4
        vmulpd    xmm2, xmm12, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2432]
        vcmpeqpd  xmm15, xmm0, xmm1
        vsubpd    xmm14, xmm0, xmm2
        vmovupd   xmm3, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2816]
        vandpd    xmm5, xmm1, xmm3
        vandnpd   xmm4, xmm3, xmm1
        vcmpnlepd xmm6, xmm5, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2880]
        vmovmskpd edx, xmm6
        vandps    xmm6, xmm13, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2560]
        vorpd     xmm15, xmm15, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2688]
        vmovd     eax, xmm6
        vandpd    xmm15, xmm0, xmm15
        vsubpd    xmm0, xmm0, xmm15
        vpextrd   ecx, xmm6, 2
        movsxd    rax, eax
        movsxd    rcx, ecx
        vsubpd    xmm2, xmm0, xmm2
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+r8+rax]
        vmovupd   xmm6, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+r8+rcx]
        vunpcklpd xmm12, xmm5, xmm6
        vunpckhpd xmm6, xmm5, xmm6
        vpand     xmm5, xmm13, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2624]
        vpsllq    xmm13, xmm5, 41
        vorpd     xmm5, xmm12, xmm13
        vmulpd    xmm12, xmm14, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2048]
        vmulpd    xmm6, xmm6, xmm13
        vmulpd    xmm13, xmm14, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2176]
        vmulpd    xmm14, xmm14, xmm14
        vaddpd    xmm12, xmm12, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2112]
        vaddpd    xmm13, xmm13, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2240]
        vmulpd    xmm12, xmm12, xmm14
        vaddpd    xmm12, xmm13, xmm12
        vmulpd    xmm13, xmm5, xmm15
        vmulpd    xmm14, xmm14, xmm12
        vaddpd    xmm2, xmm2, xmm14
        vmovupd   xmm14, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2752]
        vandpd    xmm0, xmm5, xmm3
        vandpd    xmm3, xmm14, xmm3
        vcmplepd  xmm12, xmm0, xmm3
        vaddpd    xmm3, xmm5, xmm14
        vblendvpd xmm0, xmm5, xmm14, xmm12
        vblendvpd xmm12, xmm14, xmm5, xmm12
        vsubpd    xmm14, xmm0, xmm3
        vmulpd    xmm5, xmm5, xmm2
        vaddpd    xmm0, xmm12, xmm14
        vaddpd    xmm14, xmm13, xmm3
        vaddpd    xmm2, xmm15, xmm2
        vaddpd    xmm12, xmm6, xmm0
        vsubpd    xmm3, xmm3, xmm14
        vmulpd    xmm6, xmm6, xmm2
        vaddpd    xmm13, xmm13, xmm3
        vaddpd    xmm15, xmm12, xmm6
        vaddpd    xmm0, xmm13, xmm5
        vaddpd    xmm0, xmm0, xmm15
        vaddpd    xmm2, xmm14, xmm0
        mov       QWORD PTR [280+rsp], r13
        vorpd     xmm0, xmm2, xmm4
        test      edx, edx
        jne       _B2_3

_B2_2::

        vmovups   xmm6, XMMWORD PTR [192+rsp]
        vmovups   xmm12, XMMWORD PTR [256+rsp]
        vmovups   xmm13, XMMWORD PTR [240+rsp]
        vmovups   xmm14, XMMWORD PTR [224+rsp]
        vmovups   xmm15, XMMWORD PTR [208+rsp]
        mov       r13, QWORD PTR [272+rsp]
        add       rsp, 296
        ret

_B2_3::

        vmovupd   XMMWORD PTR [r13], xmm1
        vmovupd   XMMWORD PTR [64+r13], xmm0

_B2_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B2_7::

        bt        esi, ebx
        jc        _B2_10

_B2_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B2_7

_B2_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B2_2

_B2_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dexpm1_ha_cout_rare_internal
        jmp       _B2_8
        ALIGN     16

_B2_11::

__svml_expm12_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm12_ha_e9_B1_B3:
	DD	936705
	DD	2282571
	DD	813123
	DD	1099834
	DD	1038385
	DD	976936
	DD	915480
	DD	2425099

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_expm12_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm12_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_expm12_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_6
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_expm12_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_expm12_ha_l9

__svml_expm12_ha_l9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L45::

        sub       rsp, 296
        vmovapd   xmm1, xmm0
        vmovups   XMMWORD PTR [256+rsp], xmm15
        lea       r8, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [208+rsp], xmm9
        vmovups   XMMWORD PTR [224+rsp], xmm8
        vmovups   XMMWORD PTR [240+rsp], xmm7
        vmovups   XMMWORD PTR [192+rsp], xmm6
        mov       QWORD PTR [272+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmulpd    xmm6, xmm1, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2304]
        vroundpd  xmm6, xmm6, 0
        and       r13, -64
        vaddpd    xmm8, xmm6, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2496]
        vmulpd    xmm2, xmm6, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2432]
        vmovupd   xmm0, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2368]
        vmovupd   xmm3, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2816]
        vandps    xmm7, xmm8, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2560]
        vandpd    xmm5, xmm1, xmm3
        vfnmadd213pd xmm0, xmm6, xmm1
        vandnpd   xmm4, xmm3, xmm1
        vpand     xmm8, xmm8, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2624]
        vpsllq    xmm8, xmm8, 41
        vmovd     eax, xmm7
        vcmpnlepd xmm5, xmm5, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2880]
        vcmpeqpd  xmm15, xmm0, xmm1
        vsubpd    xmm9, xmm0, xmm2
        vmovmskpd edx, xmm5
        vorpd     xmm15, xmm15, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2688]
        vpextrd   ecx, xmm7, 2
        vandpd    xmm15, xmm0, xmm15
        movsxd    rax, eax
        movsxd    rcx, ecx
        vsubpd    xmm0, xmm0, xmm15
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+r8+rax]
        vmovupd   xmm6, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+r8+rcx]
        vunpcklpd xmm7, xmm5, xmm6
        vunpckhpd xmm6, xmm5, xmm6
        vorpd     xmm5, xmm7, xmm8
        vmulpd    xmm6, xmm6, xmm8
        vsubpd    xmm0, xmm0, xmm2
        vmulpd    xmm2, xmm5, xmm15
        vmovupd   xmm7, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2048]
        vmovupd   xmm8, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2176]
        vfmadd213pd xmm7, xmm9, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2112]
        vfmadd213pd xmm8, xmm9, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2240]
        vmulpd    xmm9, xmm9, xmm9
        vfmadd213pd xmm7, xmm9, xmm8
        vandpd    xmm8, xmm5, xmm3
        vfmadd213pd xmm7, xmm9, xmm0
        vmovupd   xmm9, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2752]
        vandpd    xmm3, xmm9, xmm3
        vcmplepd  xmm0, xmm8, xmm3
        vaddpd    xmm3, xmm5, xmm9
        vblendvpd xmm8, xmm5, xmm9, xmm0
        vblendvpd xmm0, xmm9, xmm5, xmm0
        vsubpd    xmm9, xmm8, xmm3
        vaddpd    xmm0, xmm0, xmm9
        vaddpd    xmm8, xmm6, xmm0
        vaddpd    xmm0, xmm2, xmm3
        vsubpd    xmm3, xmm3, xmm0
        vaddpd    xmm2, xmm2, xmm3
        vfmadd213pd xmm5, xmm7, xmm2
        vaddpd    xmm2, xmm15, xmm7
        vfmadd213pd xmm6, xmm2, xmm8
        vaddpd    xmm3, xmm5, xmm6
        vaddpd    xmm0, xmm0, xmm3
        mov       QWORD PTR [280+rsp], r13
        vorpd     xmm0, xmm0, xmm4
        test      edx, edx
        jne       _B3_3

_B3_2::

        vmovups   xmm6, XMMWORD PTR [192+rsp]
        vmovups   xmm7, XMMWORD PTR [240+rsp]
        vmovups   xmm8, XMMWORD PTR [224+rsp]
        vmovups   xmm9, XMMWORD PTR [208+rsp]
        vmovups   xmm15, XMMWORD PTR [256+rsp]
        mov       r13, QWORD PTR [272+rsp]
        add       rsp, 296
        ret

_B3_3::

        vmovupd   XMMWORD PTR [r13], xmm1
        vmovupd   XMMWORD PTR [64+r13], xmm0

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
        cmp       ebx, 2
        jl        _B3_7

_B3_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B3_2

_B3_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dexpm1_ha_cout_rare_internal
        jmp       _B3_8
        ALIGN     16

_B3_11::

__svml_expm12_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm12_ha_l9_B1_B3:
	DD	936705
	DD	2282571
	DD	813123
	DD	1013818
	DD	952369
	DD	890920
	DD	1112088
	DD	2425099

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_expm12_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm12_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_expm12_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_6
	DD	imagerel _B3_11
	DD	imagerel _unwind___svml_expm12_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_expm11_ha_ex

__svml_expm11_ha_ex	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L62::

        sub       rsp, 280
        lea       rdx, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [208+rsp], xmm10
        movaps    xmm10, xmm0
        movups    XMMWORD PTR [176+rsp], xmm12
        movaps    xmm3, xmm10
        movups    XMMWORD PTR [192+rsp], xmm11
        movups    XMMWORD PTR [224+rsp], xmm9
        movups    XMMWORD PTR [240+rsp], xmm8
        movups    XMMWORD PTR [256+rsp], xmm7
        mov       QWORD PTR [168+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        mulsd     xmm3, QWORD PTR [__svml_dexpm1_ha_data_internal+2304]
        and       r13, -64
        movups    xmm4, XMMWORD PTR [_2il0floatpacket_33]
        addpd     xmm3, xmm4
        subpd     xmm3, xmm4
        movaps    xmm5, xmm3
        movaps    xmm8, xmm3
        mulsd     xmm5, QWORD PTR [__svml_dexpm1_ha_data_internal+2368]
        addsd     xmm3, QWORD PTR [__svml_dexpm1_ha_data_internal+2496]
        mulsd     xmm8, QWORD PTR [__svml_dexpm1_ha_data_internal+2432]
        subsd     xmm0, xmm5
        movaps    xmm1, xmm0
        movaps    xmm11, xmm0
        movq      xmm7, QWORD PTR [__svml_dexpm1_ha_data_internal+2560]
        subsd     xmm1, xmm8
        cmpeqsd   xmm11, xmm10
        pand      xmm7, xmm3
        movd      eax, xmm7
        movsd     xmm7, QWORD PTR [__svml_dexpm1_ha_data_internal+2048]
        movsd     xmm2, QWORD PTR [__svml_dexpm1_ha_data_internal+2816]
        mulsd     xmm7, xmm1
        movaps    xmm4, xmm2
        movq      xmm12, QWORD PTR [__svml_dexpm1_ha_data_internal+2624]
        andps     xmm4, xmm10
        pand      xmm3, xmm12
        movaps    xmm9, xmm2
        movsd     xmm12, QWORD PTR [__svml_dexpm1_ha_data_internal+2176]
        psllq     xmm3, 41
        movsxd    rax, eax
        andnps    xmm9, xmm10
        mulsd     xmm12, xmm1
        cmpnlesd  xmm4, QWORD PTR [__svml_dexpm1_ha_data_internal+2880]
        addsd     xmm7, QWORD PTR [__svml_dexpm1_ha_data_internal+2112]
        mulsd     xmm1, xmm1
        movmskpd  ecx, xmm4
        addsd     xmm12, QWORD PTR [__svml_dexpm1_ha_data_internal+2240]
        mulsd     xmm7, xmm1
        movsd     xmm4, QWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+8+rdx+rax]
        and       ecx, 1
        movups    xmm5, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+rdx+rax]
        addsd     xmm7, xmm12
        unpcklpd  xmm4, xmm4
        orps      xmm5, xmm3
        mulsd     xmm4, xmm3
        movaps    xmm12, xmm2
        mulsd     xmm7, xmm1
        movsd     xmm3, QWORD PTR [__svml_dexpm1_ha_data_internal+2688]
        andps     xmm12, xmm5
        orps      xmm3, xmm11
        andps     xmm3, xmm0
        mov       QWORD PTR [272+rsp], r13
        subsd     xmm0, xmm3
        subsd     xmm0, xmm8
        movaps    xmm8, xmm5
        mulsd     xmm8, xmm3
        addsd     xmm7, xmm0
        movsd     xmm0, QWORD PTR [__svml_dexpm1_ha_data_internal+2752]
        andps     xmm2, xmm0
        movaps    xmm1, xmm0
        cmplesd   xmm12, xmm2
        movaps    xmm11, xmm12
        andps     xmm1, xmm12
        andnps    xmm11, xmm5
        movaps    xmm2, xmm12
        orps      xmm11, xmm1
        movaps    xmm1, xmm5
        andnps    xmm2, xmm0
        andps     xmm12, xmm5
        orps      xmm2, xmm12
        addsd     xmm1, xmm0
        mulsd     xmm5, xmm7
        addsd     xmm7, xmm3
        subsd     xmm11, xmm1
        movaps    xmm0, xmm1
        addsd     xmm11, xmm2
        addsd     xmm0, xmm8
        addsd     xmm11, xmm4
        mulsd     xmm4, xmm7
        subsd     xmm1, xmm0
        addsd     xmm4, xmm11
        addsd     xmm8, xmm1
        addsd     xmm5, xmm8
        addsd     xmm5, xmm4
        addsd     xmm0, xmm5
        orps      xmm0, xmm9
        jne       _B4_3

_B4_2::

        movups    xmm7, XMMWORD PTR [256+rsp]
        movups    xmm8, XMMWORD PTR [240+rsp]
        movups    xmm9, XMMWORD PTR [224+rsp]
        movups    xmm10, XMMWORD PTR [208+rsp]
        movups    xmm11, XMMWORD PTR [192+rsp]
        movups    xmm12, XMMWORD PTR [176+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 280
        ret

_B4_3::

        movsd     QWORD PTR [r13], xmm10
        movsd     QWORD PTR [64+r13], xmm0
        jne       _B4_6

_B4_4::

        movsd     xmm0, QWORD PTR [64+r13]
        jmp       _B4_2

_B4_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dexpm1_ha_cout_rare_internal
        jmp       _B4_4
        ALIGN     16

_B4_7::

__svml_expm11_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm11_ha_ex_B1_B6:
	DD	1070849
	DD	1430615
	DD	1079375
	DD	1017927
	DD	956478
	DD	833589
	DD	772136
	DD	895003
	DD	2294027

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_7
	DD	imagerel _unwind___svml_expm11_ha_ex_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_expm12_ha_ex

__svml_expm12_ha_ex	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L77::

        sub       rsp, 296
        lea       r8, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [192+rsp], xmm10
        movups    XMMWORD PTR [208+rsp], xmm9
        movups    XMMWORD PTR [224+rsp], xmm8
        movups    XMMWORD PTR [240+rsp], xmm7
        movups    XMMWORD PTR [256+rsp], xmm6
        movaps    xmm6, xmm0
        mov       QWORD PTR [272+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movups    xmm4, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2304]
        and       r13, -64
        mulpd     xmm4, xmm6
        movups    xmm2, XMMWORD PTR [_2il0floatpacket_33]
        addpd     xmm4, xmm2
        subpd     xmm4, xmm2
        movups    xmm3, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2368]
        movaps    xmm2, xmm6
        mulpd     xmm3, xmm4
        movups    xmm0, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2816]
        movups    xmm10, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2496]
        movaps    xmm1, xmm0
        addpd     xmm10, xmm4
        subpd     xmm2, xmm3
        andps     xmm1, xmm6
        movaps    xmm8, xmm2
        cmpnlepd  xmm1, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2880]
        movups    xmm7, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2432]
        movaps    xmm3, xmm2
        mulpd     xmm7, xmm4
        movmskpd  eax, xmm1
        movdqu    xmm9, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2560]
        movaps    xmm5, xmm0
        pand      xmm9, xmm10
        andnps    xmm5, xmm6
        movd      edx, xmm9
        pshufd    xmm1, xmm9, 2
        pand      xmm10, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2624]
        movd      ecx, xmm1
        psllq     xmm10, 41
        subpd     xmm8, xmm7
        cmpeqpd   xmm3, xmm6
        movsxd    rdx, edx
        movsxd    rcx, ecx
        orps      xmm3, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2688]
        andps     xmm3, xmm2
        movups    xmm4, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+r8+rdx]
        movups    xmm9, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+r8+rcx]
        movaps    xmm1, xmm4
        unpcklpd  xmm1, xmm9
        unpckhpd  xmm4, xmm9
        orps      xmm1, xmm10
        mulpd     xmm4, xmm10
        subpd     xmm2, xmm3
        movups    xmm10, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2048]
        mulpd     xmm10, xmm8
        subpd     xmm2, xmm7
        addpd     xmm10, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2112]
        movups    xmm9, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2176]
        movaps    xmm7, xmm1
        mulpd     xmm9, xmm8
        mulpd     xmm8, xmm8
        addpd     xmm9, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2240]
        mulpd     xmm10, xmm8
        mulpd     xmm7, xmm3
        addpd     xmm9, xmm10
        mulpd     xmm8, xmm9
        movups    xmm10, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2752]
        movaps    xmm9, xmm0
        andps     xmm9, xmm1
        andps     xmm0, xmm10
        cmplepd   xmm9, xmm0
        addpd     xmm2, xmm8
        movaps    xmm0, xmm9
        movaps    xmm8, xmm10
        andnps    xmm0, xmm1
        andps     xmm8, xmm9
        orps      xmm0, xmm8
        movaps    xmm8, xmm9
        andnps    xmm8, xmm10
        andps     xmm9, xmm1
        addpd     xmm10, xmm1
        addpd     xmm3, xmm2
        mulpd     xmm1, xmm2
        subpd     xmm0, xmm10
        orps      xmm8, xmm9
        addpd     xmm8, xmm0
        movaps    xmm0, xmm10
        addpd     xmm0, xmm7
        addpd     xmm8, xmm4
        mulpd     xmm4, xmm3
        subpd     xmm10, xmm0
        addpd     xmm8, xmm4
        addpd     xmm7, xmm10
        addpd     xmm7, xmm1
        addpd     xmm7, xmm8
        addpd     xmm0, xmm7
        mov       QWORD PTR [280+rsp], r13
        orps      xmm0, xmm5
        test      eax, eax
        jne       _B5_3

_B5_2::

        movups    xmm6, XMMWORD PTR [256+rsp]
        movups    xmm7, XMMWORD PTR [240+rsp]
        movups    xmm8, XMMWORD PTR [224+rsp]
        movups    xmm9, XMMWORD PTR [208+rsp]
        movups    xmm10, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [272+rsp]
        add       rsp, 296
        ret

_B5_3::

        movups    XMMWORD PTR [r13], xmm6
        movups    XMMWORD PTR [64+r13], xmm0

_B5_6::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

_B5_7::

        mov       ecx, ebx
        mov       edx, 1
        shl       edx, cl
        test      esi, edx
        jne       _B5_10

_B5_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B5_7

_B5_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [64+r13]
        jmp       _B5_2

_B5_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dexpm1_ha_cout_rare_internal
        jmp       _B5_8
        ALIGN     16

_B5_11::

__svml_expm12_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm12_ha_ex_B1_B3:
	DD	935937
	DD	2282568
	DD	1075261
	DD	1013813
	DD	952365
	DD	890916
	DD	829467
	DD	2425099

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_expm12_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm12_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_expm12_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_6
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_expm12_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_expm11_ha_l9

__svml_expm11_ha_l9	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L94::

        sub       rsp, 280
        vmovapd   xmm4, xmm0
        vmovups   XMMWORD PTR [224+rsp], xmm15
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [240+rsp], xmm14
        vmovups   XMMWORD PTR [256+rsp], xmm13
        vmovups   XMMWORD PTR [192+rsp], xmm10
        vmovups   XMMWORD PTR [208+rsp], xmm9
        vmovups   XMMWORD PTR [176+rsp], xmm6
        mov       QWORD PTR [168+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        vmulsd    xmm3, xmm4, QWORD PTR [__svml_dexpm1_ha_data_internal+2304]
        and       r13, -64
        vroundsd  xmm1, xmm3, xmm3, 0
        vmovsd    xmm2, QWORD PTR [__svml_dexpm1_ha_data_internal+2816]
        vmovapd   xmm13, xmm1
        vmovq     xmm6, QWORD PTR [__svml_dexpm1_ha_data_internal+2560]
        vandpd    xmm5, xmm4, xmm2
        vfnmadd132sd xmm13, xmm4, QWORD PTR [__svml_dexpm1_ha_data_internal+2368]
        vandnpd   xmm3, xmm2, xmm4
        vmulsd    xmm14, xmm1, QWORD PTR [__svml_dexpm1_ha_data_internal+2432]
        vaddsd    xmm10, xmm1, QWORD PTR [__svml_dexpm1_ha_data_internal+2496]
        vcmpnlesd xmm0, xmm5, QWORD PTR [__svml_dexpm1_ha_data_internal+2880]
        vcmpeqsd  xmm9, xmm13, xmm4
        vsubsd    xmm15, xmm13, xmm14
        vmovmskpd ecx, xmm0
        vpand     xmm5, xmm10, xmm6
        vmovd     eax, xmm5
        vmovq     xmm0, QWORD PTR [__svml_dexpm1_ha_data_internal+2624]
        vpand     xmm6, xmm10, xmm0
        movsxd    rax, eax
        vpsllq    xmm5, xmm6, 41
        vmovsd    xmm10, QWORD PTR [__svml_dexpm1_ha_data_internal+2176]
        vfmadd213sd xmm10, xmm15, QWORD PTR [__svml_dexpm1_ha_data_internal+2240]
        vmulsd    xmm0, xmm15, xmm15
        vmovddup  xmm1, QWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+8+rdx+rax]
        vorpd     xmm6, xmm5, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+rdx+rax]
        vmulsd    xmm1, xmm1, xmm5
        vmovsd    xmm5, QWORD PTR [__svml_dexpm1_ha_data_internal+2048]
        vfmadd213sd xmm5, xmm15, QWORD PTR [__svml_dexpm1_ha_data_internal+2112]
        vmovsd    xmm15, QWORD PTR [__svml_dexpm1_ha_data_internal+2688]
        vorpd     xmm9, xmm15, xmm9
        vandpd    xmm9, xmm13, xmm9
        vmulsd    xmm15, xmm6, xmm9
        vsubsd    xmm13, xmm13, xmm9
        vfmadd213sd xmm5, xmm0, xmm10
        vmovsd    xmm10, QWORD PTR [__svml_dexpm1_ha_data_internal+2752]
        vsubsd    xmm14, xmm13, xmm14
        vfmadd213sd xmm5, xmm0, xmm14
        vandpd    xmm0, xmm6, xmm2
        vandpd    xmm2, xmm10, xmm2
        vaddsd    xmm14, xmm6, xmm10
        vcmplesd  xmm0, xmm0, xmm2
        vblendvpd xmm2, xmm6, xmm10, xmm0
        vblendvpd xmm13, xmm10, xmm6, xmm0
        vsubsd    xmm10, xmm2, xmm14
        mov       QWORD PTR [272+rsp], r13
        vaddsd    xmm0, xmm10, xmm13
        vaddsd    xmm2, xmm0, xmm1
        vaddsd    xmm0, xmm14, xmm15
        vsubsd    xmm14, xmm14, xmm0
        vaddsd    xmm15, xmm15, xmm14
        vfmadd213sd xmm6, xmm5, xmm15
        vaddsd    xmm5, xmm5, xmm9
        vfmadd213sd xmm1, xmm5, xmm2
        vaddsd    xmm1, xmm6, xmm1
        vaddsd    xmm6, xmm0, xmm1
        vorpd     xmm0, xmm6, xmm3
        and       ecx, 1
        jne       _B6_3

_B6_2::

        vmovups   xmm6, XMMWORD PTR [176+rsp]
        vmovups   xmm9, XMMWORD PTR [208+rsp]
        vmovups   xmm10, XMMWORD PTR [192+rsp]
        vmovups   xmm13, XMMWORD PTR [256+rsp]
        vmovups   xmm14, XMMWORD PTR [240+rsp]
        vmovups   xmm15, XMMWORD PTR [224+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 280
        ret

_B6_3::

        vmovsd    QWORD PTR [r13], xmm4
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B6_6

_B6_4::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B6_2

_B6_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dexpm1_ha_cout_rare_internal
        jmp       _B6_4
        ALIGN     16

_B6_7::

__svml_expm11_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm11_ha_l9_B1_B6:
	DD	1070081
	DD	1430612
	DD	747596
	DD	890947
	DD	829498
	DD	1103921
	DD	1042472
	DD	981016
	DD	2294027

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_expm11_ha_l9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_expm14_ha_e9

__svml_expm14_ha_e9	PROC	EXPORT

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L109::

        sub       rsp, 552
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [240+rsp], ymm15
        vmovups   YMMWORD PTR [272+rsp], ymm14
        vmovups   YMMWORD PTR [304+rsp], ymm13
        vmovups   YMMWORD PTR [336+rsp], ymm12
        vmovups   YMMWORD PTR [368+rsp], ymm11
        vmovups   YMMWORD PTR [400+rsp], ymm10
        vmovups   YMMWORD PTR [432+rsp], ymm9
        vmovups   YMMWORD PTR [464+rsp], ymm8
        vmovups   YMMWORD PTR [496+rsp], ymm7
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [143+rsp]
        vmovapd   ymm9, ymm0
        and       r13, -64
        vmulpd    ymm4, ymm9, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2304]
        vroundpd  ymm12, ymm4, 0
        vmovupd   ymm4, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2816]
        vmulpd    ymm2, ymm12, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2368]
        vandpd    ymm5, ymm9, ymm4
        vcmpnle_uqpd ymm11, ymm5, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2880]
        vmovupd   xmm5, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2560]
        vsubpd    ymm3, ymm9, ymm2
        vmulpd    ymm2, ymm12, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2432]
        vcmpeqpd  ymm1, ymm3, ymm9
        vsubpd    ymm0, ymm3, ymm2
        vorpd     ymm1, ymm1, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2688]
        vandnpd   ymm8, ymm4, ymm9
        mov       QWORD PTR [536+rsp], r13
        vextractf128 xmm10, ymm11, 1
        vshufps   xmm7, xmm11, xmm10, 221
        vaddpd    ymm11, ymm12, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2496]
        vmovmskps edx, xmm7
        vandps    xmm13, xmm11, xmm5
        vextractf128 xmm14, ymm11, 1
        vmovd     ecx, xmm13
        vandps    xmm10, xmm14, xmm5
        vmovd     r9d, xmm10
        vpextrd   r8d, xmm13, 2
        movsxd    rcx, ecx
        movsxd    r8, r8d
        vpextrd   r10d, xmm10, 2
        movsxd    r9, r9d
        movsxd    r10, r10d
        vmovupd   xmm15, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+rax+rcx]
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+rax+r8]
        vmovupd   xmm13, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+rax+r9]
        vmovupd   xmm12, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+rax+r10]
        vunpcklpd xmm7, xmm15, xmm5
        vunpckhpd xmm15, xmm15, xmm5
        vmovupd   xmm5, XMMWORD PTR [__svml_dexpm1_ha_data_internal+2624]
        vunpcklpd xmm10, xmm13, xmm12
        vandps    xmm11, xmm11, xmm5
        vandps    xmm14, xmm14, xmm5
        vinsertf128 ymm7, ymm7, xmm10, 1
        vunpckhpd xmm10, xmm13, xmm12
        vpsllq    xmm12, xmm11, 41
        vpsllq    xmm11, xmm14, 41
        vinsertf128 ymm13, ymm12, xmm11, 1
        vmulpd    ymm11, ymm0, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2176]
        vorpd     ymm5, ymm7, ymm13
        vinsertf128 ymm10, ymm15, xmm10, 1
        vmulpd    ymm7, ymm10, ymm13
        vmulpd    ymm10, ymm0, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2048]
        vaddpd    ymm15, ymm11, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2240]
        vmulpd    ymm11, ymm0, ymm0
        vaddpd    ymm14, ymm10, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2112]
        vandpd    ymm0, ymm3, ymm1
        vsubpd    ymm10, ymm3, ymm0
        vmulpd    ymm3, ymm14, ymm11
        vmovupd   ymm14, YMMWORD PTR [__svml_dexpm1_ha_data_internal+2752]
        vmulpd    ymm13, ymm5, ymm0
        vaddpd    ymm12, ymm15, ymm3
        vsubpd    ymm1, ymm10, ymm2
        vmulpd    ymm11, ymm11, ymm12
        vandpd    ymm2, ymm5, ymm4
        vandpd    ymm4, ymm14, ymm4
        vcmple_oqpd ymm3, ymm2, ymm4
        vaddpd    ymm12, ymm1, ymm11
        vaddpd    ymm1, ymm5, ymm14
        vblendvpd ymm15, ymm5, ymm14, ymm3
        vblendvpd ymm11, ymm14, ymm5, ymm3
        vsubpd    ymm10, ymm15, ymm1
        vaddpd    ymm0, ymm0, ymm12
        vmulpd    ymm5, ymm5, ymm12
        vaddpd    ymm15, ymm11, ymm10
        vaddpd    ymm11, ymm13, ymm1
        vaddpd    ymm10, ymm7, ymm15
        vsubpd    ymm2, ymm1, ymm11
        vmulpd    ymm7, ymm7, ymm0
        vaddpd    ymm13, ymm13, ymm2
        vaddpd    ymm0, ymm10, ymm7
        vaddpd    ymm1, ymm13, ymm5
        vaddpd    ymm2, ymm1, ymm0
        vaddpd    ymm3, ymm11, ymm2
        vorpd     ymm0, ymm3, ymm8
        test      edx, edx
        jne       _B7_3

_B7_2::

        vmovups   ymm7, YMMWORD PTR [496+rsp]
        vmovups   ymm8, YMMWORD PTR [464+rsp]
        vmovups   ymm9, YMMWORD PTR [432+rsp]
        vmovups   ymm10, YMMWORD PTR [400+rsp]
        vmovups   ymm11, YMMWORD PTR [368+rsp]
        vmovups   ymm12, YMMWORD PTR [336+rsp]
        vmovups   ymm13, YMMWORD PTR [304+rsp]
        vmovups   ymm14, YMMWORD PTR [272+rsp]
        vmovups   ymm15, YMMWORD PTR [240+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B7_3::

        vmovupd   YMMWORD PTR [r13], ymm9
        vmovupd   YMMWORD PTR [64+r13], ymm0

_B7_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [32+rsp], ymm6
        mov       QWORD PTR [72+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [64+rsp], rsi
        mov       esi, edx

_B7_7::

        bt        esi, ebx
        jc        _B7_10

_B7_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B7_7

_B7_9::

        vmovups   ymm6, YMMWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [72+rsp]
        mov       rsi, QWORD PTR [64+rsp]
        jmp       _B7_2

_B7_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dexpm1_ha_cout_rare_internal
        jmp       _B7_8
        ALIGN     16

_B7_11::

__svml_expm14_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm14_ha_e9_B1_B3:
	DD	1469185
	DD	4379755
	DD	2062435
	DD	1935450
	DD	1808465
	DD	1681480
	DD	1554495
	DD	1427510
	DD	1300525
	DD	1173540
	DD	1046555
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_6
	DD	imagerel _unwind___svml_expm14_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm14_ha_e9_B6_B10:
	DD	398369
	DD	549908
	DD	603149
	DD	157704
	DD	imagerel _B7_1
	DD	imagerel _B7_6
	DD	imagerel _unwind___svml_expm14_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_6
	DD	imagerel _B7_11
	DD	imagerel _unwind___svml_expm14_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST7:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_expm18_ha_z0

__svml_expm18_ha_z0	PROC	EXPORT

_B8_1::

        DB        243
        DB        15
        DB        30
        DB        250
L136::

        sub       rsp, 1336
        mov       QWORD PTR [1320+rsp], r13
        lea       r13, QWORD PTR [1183+rsp]
        vmovups   zmm24, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+256]
        and       r13, -64
        vmovups   zmm23, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+320]
        vmovups   zmm25, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+512]
        vmovups   zmm26, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+576]
        vmovups   zmm28, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+640]
        vmovups   zmm22, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+384]
        vmovups   zmm29, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512]
        vmovups   zmm4, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+128]
        vmovaps   zmm5, zmm0
        vfmadd213pd zmm24, zmm5, zmm23 {rn-sae}
        vmovups   zmm0, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+704]
        vcmppd    k0, zmm5, zmm22, 21 {sae}
        vmovups   zmm22, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+768]
        vsubpd    zmm27, zmm24, zmm23 {rn-sae}
        vpermt2pd zmm29, zmm24, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+64]
        vpermt2pd zmm4, zmm24, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+192]
        vfnmadd213pd zmm25, zmm27, zmm5 {rn-sae}
        vmovups   zmm23, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+896]
        vmovups   zmm24, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+1024]
        vmaxpd    zmm30, zmm27, zmm28 {sae}
        kmovw     r8d, k0
        vscalefpd zmm3, zmm29, zmm30 {rn-sae}
        vmovups   zmm30, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+1216]
        vmovaps   zmm31, zmm27
        vfnmadd213pd zmm31, zmm26, zmm25 {rn-sae}
        vcmppd    k1, zmm3, zmm30, 17 {sae}
        vsubpd    zmm1, zmm25, zmm31 {rn-sae}
        vandpd    zmm2, zmm31, zmm0
        vmovups   zmm25, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+960]
        vfnmadd231pd zmm1, zmm26, zmm27 {rn-sae}
        vmovups   zmm26, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+1088]
        vmovups   zmm27, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+1152]
        vmovups   zmm31, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+1280]
        vfmadd231pd zmm25, zmm23, zmm2 {rn-sae}
        vfmadd231pd zmm26, zmm24, zmm2 {rn-sae}
        vblendmpd zmm23{k1}, zmm31, zmm3
        vandpd    zmm28, zmm1, zmm0
        vaddpd    zmm24, zmm3, zmm31 {rn-sae}
        vmulpd    zmm0, zmm2, zmm2 {rn-sae}
        vmovups   zmm1, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+832]
        vaddpd    zmm29, zmm28, zmm4 {rn-sae}
        vfmadd231pd zmm1, zmm22, zmm2 {rn-sae}
        vfmadd213pd zmm4, zmm2, zmm29 {rn-sae}
        vblendmpd zmm22{k1}, zmm3, zmm31
        vfmadd213pd zmm1, zmm0, zmm25 {rn-sae}
        vsubpd    zmm31, zmm22, zmm24 {rn-sae}
        vfmadd213pd zmm1, zmm0, zmm26 {rn-sae}
        vaddpd    zmm25, zmm31, zmm23 {rn-sae}
        vfmadd213pd zmm1, zmm2, zmm27 {rn-sae}
        vfmadd213pd zmm1, zmm0, zmm4 {rn-sae}
        vmovaps   zmm0, zmm24
        vfmadd231pd zmm0, zmm2, zmm3 {rn-sae}
        vsubpd    zmm4, zmm0, zmm24 {rn-sae}
        vfmsub231pd zmm4, zmm2, zmm3 {rn-sae}
        vfmadd213pd zmm3, zmm1, zmm25 {rn-sae}
        vaddpd    zmm2, zmm3, zmm4 {rn-sae}
        vaddpd    zmm0, zmm0, zmm2 {rn-sae}
        mov       QWORD PTR [1328+rsp], r13
        vpternlogq zmm0, zmm5, ZMMWORD PTR [__svml_dexpm1_ha_data_internal_avx512+448], 248
        test      r8d, r8d
        jne       _B8_3

_B8_2::

        mov       r13, QWORD PTR [1320+rsp]
        add       rsp, 1336
        ret

_B8_3::

        vstmxcsr  DWORD PTR [1312+rsp]

_B8_4::

        movzx     edx, WORD PTR [1312+rsp]
        mov       eax, edx
        or        eax, 8064
        cmp       edx, eax
        je        _B8_6

_B8_5::

        mov       DWORD PTR [1312+rsp], eax
        vldmxcsr  DWORD PTR [1312+rsp]

_B8_6::

        vmovups   ZMMWORD PTR [r13], zmm5
        vmovups   ZMMWORD PTR [64+r13], zmm0
        test      r8d, r8d
        jne       _B8_11

_B8_7::

        cmp       edx, eax
        je        _B8_2

_B8_8::

        vstmxcsr  DWORD PTR [1312+rsp]
        mov       eax, DWORD PTR [1312+rsp]

_B8_9::

        and       eax, -8065
        or        eax, edx
        mov       DWORD PTR [1312+rsp], eax
        vldmxcsr  DWORD PTR [1312+rsp]
        jmp       _B8_2

_B8_11::

        xor       ecx, ecx
        kmovw     WORD PTR [1080+rsp], k4
        kmovw     WORD PTR [1072+rsp], k5
        kmovw     WORD PTR [1064+rsp], k6
        kmovw     WORD PTR [1056+rsp], k7
        vmovups   ZMMWORD PTR [992+rsp], zmm6
        vmovups   ZMMWORD PTR [928+rsp], zmm7
        vmovups   ZMMWORD PTR [864+rsp], zmm8
        vmovups   ZMMWORD PTR [800+rsp], zmm9
        vmovups   ZMMWORD PTR [736+rsp], zmm10
        vmovups   ZMMWORD PTR [672+rsp], zmm11
        vmovups   ZMMWORD PTR [608+rsp], zmm12
        vmovups   ZMMWORD PTR [544+rsp], zmm13
        vmovups   ZMMWORD PTR [480+rsp], zmm14
        vmovups   ZMMWORD PTR [416+rsp], zmm15
        vmovups   ZMMWORD PTR [352+rsp], zmm16
        vmovups   ZMMWORD PTR [288+rsp], zmm17
        vmovups   ZMMWORD PTR [224+rsp], zmm18
        vmovups   ZMMWORD PTR [160+rsp], zmm19
        vmovups   ZMMWORD PTR [96+rsp], zmm20
        vmovups   ZMMWORD PTR [32+rsp], zmm21
        mov       QWORD PTR [1104+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [1096+rsp], rsi
        mov       esi, edx
        mov       QWORD PTR [1088+rsp], rdi
        mov       edi, r8d
        mov       QWORD PTR [1112+rsp], rbp
        mov       ebp, eax

_B8_12::

        bt        edi, ebx
        jc        _B8_15

_B8_13::

        inc       ebx
        cmp       ebx, 8
        jl        _B8_12

_B8_14::

        kmovw     k4, WORD PTR [1080+rsp]
        mov       eax, ebp
        kmovw     k5, WORD PTR [1072+rsp]
        kmovw     k6, WORD PTR [1064+rsp]
        kmovw     k7, WORD PTR [1056+rsp]
        vmovups   zmm6, ZMMWORD PTR [992+rsp]
        vmovups   zmm7, ZMMWORD PTR [928+rsp]
        vmovups   zmm8, ZMMWORD PTR [864+rsp]
        vmovups   zmm9, ZMMWORD PTR [800+rsp]
        vmovups   zmm10, ZMMWORD PTR [736+rsp]
        vmovups   zmm11, ZMMWORD PTR [672+rsp]
        vmovups   zmm12, ZMMWORD PTR [608+rsp]
        vmovups   zmm13, ZMMWORD PTR [544+rsp]
        vmovups   zmm14, ZMMWORD PTR [480+rsp]
        vmovups   zmm15, ZMMWORD PTR [416+rsp]
        vmovups   zmm16, ZMMWORD PTR [352+rsp]
        vmovups   zmm17, ZMMWORD PTR [288+rsp]
        vmovups   zmm18, ZMMWORD PTR [224+rsp]
        vmovups   zmm19, ZMMWORD PTR [160+rsp]
        vmovups   zmm20, ZMMWORD PTR [96+rsp]
        vmovups   zmm21, ZMMWORD PTR [32+rsp]
        vmovups   zmm0, ZMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [1104+rsp]
        mov       edx, esi
        mov       rsi, QWORD PTR [1096+rsp]
        mov       rdi, QWORD PTR [1088+rsp]
        mov       rbp, QWORD PTR [1112+rsp]
        jmp       _B8_7

_B8_15::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dexpm1_ha_cout_rare_internal
        jmp       _B8_13
        ALIGN     16

_B8_16::

__svml_expm18_ha_z0 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm18_ha_z0_B1_B9:
	DD	267009
	DD	10867731
	DD	10944779

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_1
	DD	imagerel _B8_11
	DD	imagerel _unwind___svml_expm18_ha_z0_B1_B9

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm18_ha_z0_B11_B15:
	DD	3472641
	DD	9131261
	DD	8942834
	DD	9004264
	DD	9057502
	DD	153814
	DD	411851
	DD	669888
	DD	927925
	DD	1185962
	DD	1443999
	DD	1767572
	DD	2025609
	DD	2283646
	DD	2541683
	DD	2799720
	DD	3057757
	DD	3315794
	DD	3573831
	DD	3831868
	DD	4089905
	DD	8682278
	DD	8743709
	DD	8805140
	DD	8866571
	DD	10867712
	DD	10944768

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_11
	DD	imagerel _B8_16
	DD	imagerel _unwind___svml_expm18_ha_z0_B11_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST8:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_expm11_ha_e9

__svml_expm11_ha_e9	PROC	EXPORT

_B9_1::

        DB        243
        DB        15
        DB        30
        DB        250
L187::

        sub       rsp, 280
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [176+rsp], xmm15
        vmovups   XMMWORD PTR [192+rsp], xmm14
        vmovups   XMMWORD PTR [208+rsp], xmm13
        vmovapd   xmm13, xmm0
        vmovups   XMMWORD PTR [224+rsp], xmm12
        vmovups   XMMWORD PTR [240+rsp], xmm11
        vmovups   XMMWORD PTR [256+rsp], xmm6
        mov       QWORD PTR [168+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        vmovsd    xmm11, QWORD PTR [__svml_dexpm1_ha_data_internal+2816]
        and       r13, -64
        vmulsd    xmm12, xmm13, QWORD PTR [__svml_dexpm1_ha_data_internal+2304]
        vandpd    xmm5, xmm13, xmm11
        vcmpnlesd xmm6, xmm5, QWORD PTR [__svml_dexpm1_ha_data_internal+2880]
        vroundsd  xmm1, xmm12, xmm12, 0
        vandnpd   xmm12, xmm11, xmm13
        vmulsd    xmm3, xmm1, QWORD PTR [__svml_dexpm1_ha_data_internal+2368]
        vmulsd    xmm2, xmm1, QWORD PTR [__svml_dexpm1_ha_data_internal+2432]
        vmovmskpd ecx, xmm6
        vaddsd    xmm6, xmm1, QWORD PTR [__svml_dexpm1_ha_data_internal+2496]
        vsubsd    xmm0, xmm13, xmm3
        vmovq     xmm4, QWORD PTR [__svml_dexpm1_ha_data_internal+2560]
        vsubsd    xmm14, xmm0, xmm2
        vcmpeqsd  xmm15, xmm0, xmm13
        vpand     xmm3, xmm6, xmm4
        vmovd     eax, xmm3
        vmovq     xmm1, QWORD PTR [__svml_dexpm1_ha_data_internal+2624]
        vpand     xmm4, xmm6, xmm1
        movsxd    rax, eax
        vpsllq    xmm3, xmm4, 41
        vmovsd    xmm1, QWORD PTR [__svml_dexpm1_ha_data_internal+2048]
        vmulsd    xmm4, xmm1, xmm14
        vmovddup  xmm5, QWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+8+rdx+rax]
        vorpd     xmm6, xmm3, XMMWORD PTR [imagerel(__svml_dexpm1_ha_data_internal)+rdx+rax]
        vmulsd    xmm5, xmm5, xmm3
        vaddsd    xmm4, xmm4, QWORD PTR [__svml_dexpm1_ha_data_internal+2112]
        vmovsd    xmm3, QWORD PTR [__svml_dexpm1_ha_data_internal+2176]
        vmulsd    xmm3, xmm3, xmm14
        mov       QWORD PTR [272+rsp], r13
        vaddsd    xmm1, xmm3, QWORD PTR [__svml_dexpm1_ha_data_internal+2240]
        vmulsd    xmm3, xmm14, xmm14
        vmovsd    xmm14, QWORD PTR [__svml_dexpm1_ha_data_internal+2688]
        vorpd     xmm15, xmm14, xmm15
        vandpd    xmm15, xmm0, xmm15
        vsubsd    xmm14, xmm0, xmm15
        vmulsd    xmm0, xmm4, xmm3
        vmovsd    xmm4, QWORD PTR [__svml_dexpm1_ha_data_internal+2752]
        vaddsd    xmm0, xmm0, xmm1
        vsubsd    xmm1, xmm14, xmm2
        vmulsd    xmm2, xmm6, xmm15
        vmulsd    xmm3, xmm0, xmm3
        vandpd    xmm14, xmm6, xmm11
        vandpd    xmm11, xmm4, xmm11
        vaddsd    xmm0, xmm3, xmm1
        vcmplesd  xmm3, xmm14, xmm11
        vaddsd    xmm1, xmm6, xmm4
        vblendvpd xmm11, xmm6, xmm4, xmm3
        vblendvpd xmm14, xmm4, xmm6, xmm3
        vsubsd    xmm4, xmm11, xmm1
        vaddsd    xmm3, xmm4, xmm14
        vaddsd    xmm11, xmm3, xmm5
        vaddsd    xmm3, xmm1, xmm2
        vsubsd    xmm1, xmm1, xmm3
        vaddsd    xmm1, xmm2, xmm1
        vmulsd    xmm2, xmm6, xmm0
        vaddsd    xmm0, xmm0, xmm15
        vaddsd    xmm2, xmm2, xmm1
        vmulsd    xmm5, xmm5, xmm0
        vaddsd    xmm4, xmm5, xmm11
        vaddsd    xmm6, xmm2, xmm4
        vaddsd    xmm3, xmm3, xmm6
        vorpd     xmm0, xmm3, xmm12
        and       ecx, 1
        jne       _B9_3

_B9_2::

        vmovups   xmm6, XMMWORD PTR [256+rsp]
        vmovups   xmm11, XMMWORD PTR [240+rsp]
        vmovups   xmm12, XMMWORD PTR [224+rsp]
        vmovups   xmm13, XMMWORD PTR [208+rsp]
        vmovups   xmm14, XMMWORD PTR [192+rsp]
        vmovups   xmm15, XMMWORD PTR [176+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 280
        ret

_B9_3::

        vmovsd    QWORD PTR [r13], xmm13
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B9_6

_B9_4::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B9_2

_B9_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dexpm1_ha_cout_rare_internal
        jmp       _B9_4
        ALIGN     16

_B9_7::

__svml_expm11_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expm11_ha_e9_B1_B6:
	DD	1070081
	DD	1430612
	DD	1075276
	DD	1030211
	DD	968762
	DD	907309
	DD	845860
	DD	784411
	DD	2294027

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_1
	DD	imagerel _B9_7
	DD	imagerel _unwind___svml_expm11_ha_e9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST9:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_dexpm1_ha_cout_rare_internal

__svml_dexpm1_ha_cout_rare_internal	PROC

_B10_1::

        DB        243
        DB        15
        DB        30
        DB        250
L202::

        sub       rsp, 104
        mov       r9, rdx
        movsd     xmm1, QWORD PTR [rcx]
        pxor      xmm0, xmm0
        movzx     edx, WORD PTR [6+rcx]
        xor       eax, eax
        comisd    xmm0, xmm1
        ja        _B10_18

_B10_2::

        and       edx, 32752
        shr       edx, 4
        movsd     QWORD PTR [96+rsp], xmm1
        cmp       edx, 2047
        je        _B10_19

_B10_3::

        cmp       edx, 970
        jle       _B10_16

_B10_4::

        movsd     xmm0, QWORD PTR [_imldExpHATab+1080]
        comisd    xmm0, xmm1
        jb        _B10_15

_B10_5::

        comisd    xmm1, QWORD PTR [_imldExpHATab+1096]
        jb        _B10_14

_B10_6::

        movsd     xmm0, QWORD PTR [_imldExpHATab+1024]
        lea       r11, QWORD PTR [__ImageBase]
        mulsd     xmm0, xmm1
        movsd     QWORD PTR [80+rsp], xmm0
        movsd     xmm2, QWORD PTR [80+rsp]
        movsd     xmm0, QWORD PTR [_imldExpHATab+1072]
        mov       rcx, QWORD PTR [_imldExpHATab+1136]
        mov       QWORD PTR [96+rsp], rcx
        addsd     xmm2, QWORD PTR [_imldExpHATab+1032]
        movsd     QWORD PTR [88+rsp], xmm2
        movaps    xmm2, xmm1
        movsd     xmm3, QWORD PTR [88+rsp]
        mov       edx, DWORD PTR [88+rsp]
        mov       r8d, edx
        and       edx, 63
        subsd     xmm3, QWORD PTR [_imldExpHATab+1032]
        movsd     QWORD PTR [80+rsp], xmm3
        lea       r10d, DWORD PTR [rdx+rdx]
        movsd     xmm4, QWORD PTR [80+rsp]
        lea       edx, DWORD PTR [1+rdx+rdx]
        mulsd     xmm4, QWORD PTR [_imldExpHATab+1104]
        movsd     xmm5, QWORD PTR [80+rsp]
        subsd     xmm2, xmm4
        mulsd     xmm5, QWORD PTR [_imldExpHATab+1112]
        shr       r8d, 6
        subsd     xmm2, xmm5
        comisd    xmm1, QWORD PTR [_imldExpHATab+1088]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [_imldExpHATab+1064]
        mulsd     xmm0, xmm2
        lea       ecx, DWORD PTR [1023+r8]
        addsd     xmm0, QWORD PTR [_imldExpHATab+1056]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [_imldExpHATab+1048]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [_imldExpHATab+1040]
        mulsd     xmm0, xmm2
        mulsd     xmm0, xmm2
        addsd     xmm0, xmm2
        movsd     xmm2, QWORD PTR [imagerel(_imldExpHATab)+r11+r10*8]
        addsd     xmm0, QWORD PTR [imagerel(_imldExpHATab)+r11+rdx*8]
        mulsd     xmm0, xmm2
        jb        _B10_10

_B10_7::

        and       ecx, 2047
        addsd     xmm0, xmm2
        cmp       ecx, 2046
        ja        _B10_9

_B10_8::

        mov       rdx, QWORD PTR [_imldExpHATab+1136]
        shr       rdx, 48
        shl       ecx, 4
        and       edx, -32753
        or        edx, ecx
        mov       WORD PTR [102+rsp], dx
        movsd     xmm1, QWORD PTR [96+rsp]
        mulsd     xmm0, xmm1
        movsd     QWORD PTR [r9], xmm0
        add       rsp, 104
        ret

_B10_9::

        dec       ecx
        and       ecx, 2047
        movzx     edx, WORD PTR [102+rsp]
        shl       ecx, 4
        and       edx, -32753
        or        edx, ecx
        mov       WORD PTR [102+rsp], dx
        movsd     xmm1, QWORD PTR [96+rsp]
        mulsd     xmm0, xmm1
        mulsd     xmm0, QWORD PTR [_imldExpHATab+1152]
        movsd     QWORD PTR [r9], xmm0
        add       rsp, 104
        ret

_B10_10::

        add       r8d, 1083
        and       r8d, 2047
        mov       eax, r8d
        movzx     edx, WORD PTR [102+rsp]
        shl       eax, 4
        and       edx, -32753
        or        edx, eax
        mov       WORD PTR [102+rsp], dx
        movsd     xmm3, QWORD PTR [96+rsp]
        mulsd     xmm0, xmm3
        mulsd     xmm3, xmm2
        movaps    xmm1, xmm3
        addsd     xmm1, xmm0
        cmp       r8d, 50
        ja        _B10_12

_B10_11::

        mulsd     xmm1, QWORD PTR [_imldExpHATab+1160]
        movsd     QWORD PTR [r9], xmm1
        jmp       _B10_13

_B10_12::

        movsd     QWORD PTR [32+rsp], xmm1
        movsd     xmm1, QWORD PTR [32+rsp]
        subsd     xmm3, xmm1
        movsd     QWORD PTR [40+rsp], xmm3
        movsd     xmm2, QWORD PTR [40+rsp]
        addsd     xmm2, xmm0
        movsd     QWORD PTR [40+rsp], xmm2
        movsd     xmm0, QWORD PTR [32+rsp]
        mulsd     xmm0, QWORD PTR [_imldExpHATab+1168]
        movsd     QWORD PTR [48+rsp], xmm0
        movsd     xmm4, QWORD PTR [32+rsp]
        movsd     xmm3, QWORD PTR [48+rsp]
        addsd     xmm4, xmm3
        movsd     QWORD PTR [56+rsp], xmm4
        movsd     xmm0, QWORD PTR [56+rsp]
        movsd     xmm5, QWORD PTR [48+rsp]
        subsd     xmm0, xmm5
        movsd     QWORD PTR [64+rsp], xmm0
        movsd     xmm2, QWORD PTR [32+rsp]
        movsd     xmm1, QWORD PTR [64+rsp]
        subsd     xmm2, xmm1
        movsd     QWORD PTR [72+rsp], xmm2
        movsd     xmm4, QWORD PTR [40+rsp]
        movsd     xmm3, QWORD PTR [72+rsp]
        addsd     xmm4, xmm3
        movsd     QWORD PTR [72+rsp], xmm4
        movsd     xmm0, QWORD PTR [64+rsp]
        mulsd     xmm0, QWORD PTR [_imldExpHATab+1160]
        movsd     QWORD PTR [64+rsp], xmm0
        movsd     xmm1, QWORD PTR [72+rsp]
        mulsd     xmm1, QWORD PTR [_imldExpHATab+1160]
        movsd     QWORD PTR [72+rsp], xmm1
        movsd     xmm2, QWORD PTR [64+rsp]
        movsd     xmm5, QWORD PTR [72+rsp]
        addsd     xmm2, xmm5
        movsd     QWORD PTR [r9], xmm2

_B10_13::

        mov       eax, 4
        add       rsp, 104
        ret

_B10_14::

        movsd     xmm0, QWORD PTR [_imldExpHATab+1120]
        mov       eax, 4
        mulsd     xmm0, xmm0
        movsd     QWORD PTR [r9], xmm0
        add       rsp, 104
        ret

_B10_15::

        movsd     xmm0, QWORD PTR [_imldExpHATab+1128]
        mov       eax, 3
        mulsd     xmm0, xmm0
        movsd     QWORD PTR [r9], xmm0
        add       rsp, 104
        ret

_B10_16::

        movsd     xmm0, QWORD PTR [_imldExpHATab+1144]
        addsd     xmm0, xmm1
        movsd     QWORD PTR [r9], xmm0

_B10_17::

        add       rsp, 104
        ret

_B10_18::

        mov       rax, 0bff0000000000000H
        mov       QWORD PTR [r9], rax
        xor       eax, eax
        add       rsp, 104
        ret

_B10_19::

        mov       dl, BYTE PTR [103+rsp]
        and       dl, -128
        cmp       dl, -128
        je        _B10_21

_B10_20::

        mulsd     xmm1, xmm1
        movsd     QWORD PTR [r9], xmm1
        add       rsp, 104
        ret

_B10_21::

        test      DWORD PTR [100+rsp], 1048575
        jne       _B10_20

_B10_22::

        cmp       DWORD PTR [96+rsp], 0
        jne       _B10_20

_B10_23::

        mov       rdx, QWORD PTR [_imldExpHATab+1136]
        mov       QWORD PTR [r9], rdx
        add       rsp, 104
        ret
        ALIGN     16

_B10_24::

__svml_dexpm1_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_dexpm1_ha_cout_rare_internal_B1_B23:
	DD	67585
	DD	49672

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B10_1
	DD	imagerel _B10_24
	DD	imagerel _unwind___svml_dexpm1_ha_cout_rare_internal_B1_B23

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_dexpm1_ha_data_internal_avx512
__svml_dexpm1_ha_data_internal_avx512	DD	0
	DD	1072693248
	DD	1828292879
	DD	1072739672
	DD	1014845819
	DD	1072788152
	DD	1853186616
	DD	1072838778
	DD	171030293
	DD	1072891646
	DD	1276261410
	DD	1072946854
	DD	3577096743
	DD	1073004506
	DD	3712504873
	DD	1073064711
	DD	1719614413
	DD	1073127582
	DD	1944781191
	DD	1073193236
	DD	1110089947
	DD	1073261797
	DD	2191782032
	DD	1073333393
	DD	2572866477
	DD	1073408159
	DD	3716502172
	DD	1073486235
	DD	3707479175
	DD	1073567768
	DD	2728693978
	DD	1073652911
	DD	0
	DD	0
	DD	1568897901
	DD	1016568486
	DD	3936719688
	DD	3162512149
	DD	3819481236
	DD	1016499965
	DD	1303423926
	DD	1015238005
	DD	2804567149
	DD	1015390024
	DD	3145379760
	DD	1014403278
	DD	3793507337
	DD	1016095713
	DD	3210617384
	DD	3163796463
	DD	3108873501
	DD	3162190556
	DD	3253791412
	DD	1015920431
	DD	730975783
	DD	1014083580
	DD	2462790535
	DD	1015814775
	DD	816778419
	DD	1014197934
	DD	2789017511
	DD	1014276997
	DD	2413007344
	DD	3163551506
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	0
	DD	2147483648
	DD	0
	DD	2147483648
	DD	0
	DD	2147483648
	DD	0
	DD	2147483648
	DD	0
	DD	2147483648
	DD	0
	DD	2147483648
	DD	0
	DD	2147483648
	DD	0
	DD	2147483648
	DD	4277811695
	DD	1072049730
	DD	4277811695
	DD	1072049730
	DD	4277811695
	DD	1072049730
	DD	4277811695
	DD	1072049730
	DD	4277811695
	DD	1072049730
	DD	4277811695
	DD	1072049730
	DD	4277811695
	DD	1072049730
	DD	4277811695
	DD	1072049730
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
	DD	0
	DD	3227516928
	DD	0
	DD	3227516928
	DD	0
	DD	3227516928
	DD	0
	DD	3227516928
	DD	0
	DD	3227516928
	DD	0
	DD	3227516928
	DD	0
	DD	3227516928
	DD	0
	DD	3227516928
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4106095538
	DD	1056571896
	DD	4106095538
	DD	1056571896
	DD	4106095538
	DD	1056571896
	DD	4106095538
	DD	1056571896
	DD	4106095538
	DD	1056571896
	DD	4106095538
	DD	1056571896
	DD	4106095538
	DD	1056571896
	DD	4106095538
	DD	1056571896
	DD	4105251267
	DD	1059717624
	DD	4105251267
	DD	1059717624
	DD	4105251267
	DD	1059717624
	DD	4105251267
	DD	1059717624
	DD	4105251267
	DD	1059717624
	DD	4105251267
	DD	1059717624
	DD	4105251267
	DD	1059717624
	DD	4105251267
	DD	1059717624
	DD	372262401
	DD	1062650220
	DD	372262401
	DD	1062650220
	DD	372262401
	DD	1062650220
	DD	372262401
	DD	1062650220
	DD	372262401
	DD	1062650220
	DD	372262401
	DD	1062650220
	DD	372262401
	DD	1062650220
	DD	372262401
	DD	1062650220
	DD	279336721
	DD	1065423121
	DD	279336721
	DD	1065423121
	DD	279336721
	DD	1065423121
	DD	279336721
	DD	1065423121
	DD	279336721
	DD	1065423121
	DD	279336721
	DD	1065423121
	DD	279336721
	DD	1065423121
	DD	279336721
	DD	1065423121
	DD	1431655946
	DD	1067799893
	DD	1431655946
	DD	1067799893
	DD	1431655946
	DD	1067799893
	DD	1431655946
	DD	1067799893
	DD	1431655946
	DD	1067799893
	DD	1431655946
	DD	1067799893
	DD	1431655946
	DD	1067799893
	DD	1431655946
	DD	1067799893
	DD	1431655929
	DD	1069897045
	DD	1431655929
	DD	1069897045
	DD	1431655929
	DD	1069897045
	DD	1431655929
	DD	1069897045
	DD	1431655929
	DD	1069897045
	DD	1431655929
	DD	1069897045
	DD	1431655929
	DD	1069897045
	DD	1431655929
	DD	1069897045
	DD	0
	DD	1071644672
	DD	0
	DD	1071644672
	DD	0
	DD	1071644672
	DD	0
	DD	1071644672
	DD	0
	DD	1071644672
	DD	0
	DD	1071644672
	DD	0
	DD	1071644672
	DD	0
	DD	1071644672
	DD	0
	DD	1072693248
	DD	0
	DD	1072693248
	DD	0
	DD	1072693248
	DD	0
	DD	1072693248
	DD	0
	DD	1072693248
	DD	0
	DD	1072693248
	DD	0
	DD	1072693248
	DD	0
	DD	1072693248
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	PUBLIC __svml_dexpm1_ha_data_internal
__svml_dexpm1_ha_data_internal	DD	0
	DD	0
	DD	0
	DD	0
	DD	2818572288
	DD	5693
	DD	1457015399
	DD	1044362035
	DD	1073741824
	DD	11418
	DD	4044949557
	DD	3191375865
	DD	3892314112
	DD	17173
	DD	794616807
	DD	1041997793
	DD	3489660928
	DD	22960
	DD	2715237930
	DD	1044950059
	DD	671088640
	DD	28779
	DD	3698181124
	DD	1044242285
	DD	402653184
	DD	34629
	DD	36755401
	DD	1042114290
	DD	3355443200
	DD	40510
	DD	438898435
	DD	1044789148
	DD	1879048192
	DD	46424
	DD	2230008375
	DD	3192402871
	DD	671088640
	DD	52370
	DD	3120814979
	DD	1045140031
	DD	805306368
	DD	58348
	DD	269498903
	DD	1044815501
	DD	2952790016
	DD	64358
	DD	3705630242
	DD	3182612048
	DD	3489660928
	DD	70401
	DD	2766913307
	DD	1039293264
	DD	3221225472
	DD	76477
	DD	4276399797
	DD	1041960050
	DD	2952790016
	DD	82586
	DD	80474087
	DD	3191172386
	DD	3355443200
	DD	88728
	DD	613423790
	DD	1042592202
	DD	1073741824
	DD	94904
	DD	689505308
	DD	3192657268
	DD	939524096
	DD	101113
	DD	930606615
	DD	1042387389
	DD	3892314112
	DD	107355
	DD	2850403528
	DD	1045134939
	DD	2281701376
	DD	113632
	DD	1663725767
	DD	3192904985
	DD	805306368
	DD	119943
	DD	2810207104
	DD	1043762074
	DD	536870912
	DD	126288
	DD	3854830848
	DD	1044899528
	DD	2281701376
	DD	132667
	DD	2397289153
	DD	1041802037
	DD	2415919104
	DD	139081
	DD	1649749971
	DD	1043848649
	DD	1879048192
	DD	145530
	DD	2039734354
	DD	3191384540
	DD	1342177280
	DD	152014
	DD	849302817
	DD	3188938352
	DD	1744830464
	DD	158533
	DD	383003846
	DD	3191925785
	DD	3758096384
	DD	165087
	DD	158134621
	DD	1044338232
	DD	4160749568
	DD	171677
	DD	4137603445
	DD	3192324360
	DD	3489660928
	DD	178303
	DD	4290499725
	DD	1043028785
	DD	2818572288
	DD	184965
	DD	1275031083
	DD	3190931407
	DD	2818572288
	DD	191663
	DD	1629266164
	DD	1043587829
	DD	134217728
	DD	198398
	DD	2842642093
	DD	1044483512
	DD	0
	DD	205169
	DD	1985360263
	DD	3192756542
	DD	2952790016
	DD	211976
	DD	4200916017
	DD	1044586679
	DD	1610612736
	DD	218821
	DD	3450763054
	DD	3189463043
	DD	939524096
	DD	225703
	DD	2870834528
	DD	3190336198
	DD	1879048192
	DD	232622
	DD	3553800616
	DD	3192377660
	DD	939524096
	DD	239579
	DD	1219436983
	DD	3192443648
	DD	3221225472
	DD	246573
	DD	606077177
	DD	1044946247
	DD	1342177280
	DD	253606
	DD	3998375791
	DD	3192876638
	DD	134217728
	DD	260677
	DD	586810495
	DD	3192560639
	DD	536870912
	DD	267786
	DD	2676240988
	DD	1044345570
	DD	3623878656
	DD	274933
	DD	1841759300
	DD	1043663497
	DD	1610612736
	DD	282120
	DD	1086643152
	DD	1041785419
	DD	4026531840
	DD	289345
	DD	1148024454
	DD	3192330237
	DD	3087007744
	DD	296610
	DD	2137125602
	DD	3191993881
	DD	4026531840
	DD	303914
	DD	3437605242
	DD	1043004027
	DD	3623878656
	DD	311258
	DD	3340100419
	DD	3192278702
	DD	2550136832
	DD	318642
	DD	3594204911
	DD	1044372944
	DD	2013265920
	DD	326066
	DD	2502738549
	DD	3191221557
	DD	2684354560
	DD	333530
	DD	235444137
	DD	1044806450
	DD	1476395008
	DD	341035
	DD	3792656324
	DD	3191220999
	DD	3355443200
	DD	348580
	DD	1982428721
	DD	1044573328
	DD	939524096
	DD	356167
	DD	1502688512
	DD	3191123330
	DD	3623878656
	DD	363794
	DD	383164906
	DD	3192603072
	DD	3758096384
	DD	371463
	DD	3040458367
	DD	3192241502
	DD	2281701376
	DD	379174
	DD	3087934862
	DD	1044564533
	DD	402653184
	DD	386927
	DD	3163234522
	DD	3192035061
	DD	3087007744
	DD	394721
	DD	2332520281
	DD	1043819968
	DD	2952790016
	DD	402558
	DD	1492679939
	DD	1041050306
	DD	939524096
	DD	410438
	DD	29656007
	DD	3192494567
	DD	2147483648
	DD	418360
	DD	612974287
	DD	1044556049
	DD	3623878656
	DD	426325
	DD	1740578119
	DD	3192756916
	DD	1744830464
	DD	434334
	DD	922176773
	DD	3191344195
	DD	2013265920
	DD	442386
	DD	143936179
	DD	3192365354
	DD	1073741824
	DD	450482
	DD	2288974058
	DD	3192706862
	DD	4160749568
	DD	458621
	DD	1022918171
	DD	1043667272
	DD	3892314112
	DD	466805
	DD	2074373662
	DD	1043172334
	DD	1207959552
	DD	475034
	DD	2007733066
	DD	1042591790
	DD	1476395008
	DD	483307
	DD	1946752598
	DD	3191593347
	DD	1342177280
	DD	491625
	DD	1328713708
	DD	3187724640
	DD	1879048192
	DD	499988
	DD	918464641
	DD	1045387276
	DD	0
	DD	508397
	DD	667194164
	DD	1043532819
	DD	939524096
	DD	516851
	DD	3740938196
	DD	3191016217
	DD	1476395008
	DD	525351
	DD	1917817036
	DD	3192786735
	DD	2550136832
	DD	533897
	DD	682424459
	DD	1043647713
	DD	1207959552
	DD	542490
	DD	857395348
	DD	3191718789
	DD	2550136832
	DD	551129
	DD	1678188781
	DD	1045046423
	DD	3623878656
	DD	559815
	DD	2523214013
	DD	1043900009
	DD	1073741824
	DD	568549
	DD	3671932459
	DD	1044468998
	DD	402653184
	DD	577330
	DD	1091392995
	DD	3191122871
	DD	2550136832
	DD	586158
	DD	1656324724
	DD	1043421043
	DD	134217728
	DD	595035
	DD	742731994
	DD	1045204990
	DD	2952790016
	DD	603959
	DD	2659845000
	DD	1042921660
	DD	3355443200
	DD	612932
	DD	2001576987
	DD	1045316240
	DD	2684354560
	DD	621954
	DD	976271096
	DD	3187726552
	DD	1879048192
	DD	631025
	DD	927342903
	DD	1042890999
	DD	2147483648
	DD	640145
	DD	2162418230
	DD	1044717444
	DD	402653184
	DD	649315
	DD	830622888
	DD	1044263474
	DD	2013265920
	DD	658534
	DD	630511316
	DD	1045098283
	DD	4026531840
	DD	667803
	DD	1698296944
	DD	3192762006
	DD	2952790016
	DD	677123
	DD	3831108133
	DD	1044508970
	DD	268435456
	DD	686494
	DD	3279515609
	DD	1045005722
	DD	1476395008
	DD	695915
	DD	98608862
	DD	3192139794
	DD	3221225472
	DD	705387
	DD	529675467
	DD	3188065859
	DD	2550136832
	DD	714911
	DD	3588780877
	DD	1043705146
	DD	671088640
	DD	724487
	DD	1493713581
	DD	1043913574
	DD	3087007744
	DD	734114
	DD	3182425146
	DD	1041483134
	DD	2415919104
	DD	743794
	DD	864959479
	DD	3191919926
	DD	4026531840
	DD	753526
	DD	928333188
	DD	1044896498
	DD	805306368
	DD	763312
	DD	813799033
	DD	1042555081
	DD	2415919104
	DD	773150
	DD	2300504125
	DD	1041428596
	DD	1476395008
	DD	783042
	DD	1142965944
	DD	1045346544
	DD	3758096384
	DD	792987
	DD	518977959
	DD	3192116587
	DD	1610612736
	DD	802987
	DD	1972387576
	DD	3179791049
	DD	805306368
	DD	813041
	DD	1264446592
	DD	3191505643
	DD	2550136832
	DD	823149
	DD	1467128350
	DD	3192899778
	DD	3758096384
	DD	833312
	DD	3075989921
	DD	3192423292
	DD	1476395008
	DD	843531
	DD	836600757
	DD	3192197600
	DD	1207959552
	DD	853805
	DD	3697834264
	DD	1044397131
	DD	134217728
	DD	864135
	DD	364651635
	DD	1038816227
	DD	3758096384
	DD	874520
	DD	3335598035
	DD	3192398555
	DD	402653184
	DD	884963
	DD	2219290723
	DD	3191039942
	DD	0
	DD	895462
	DD	730095629
	DD	1045354900
	DD	4026531840
	DD	906017
	DD	39537391
	DD	1044909475
	DD	805306368
	DD	916631
	DD	4123739734
	DD	1045159130
	DD	402653184
	DD	927302
	DD	3136734448
	DD	3192410870
	DD	3892314112
	DD	938030
	DD	1982905152
	DD	3189583874
	DD	4160749568
	DD	948817
	DD	442147929
	DD	1045314148
	DD	2684354560
	DD	959663
	DD	3425467293
	DD	1044718726
	DD	805306368
	DD	970568
	DD	2073198199
	DD	3192097984
	DD	4026531840
	DD	981531
	DD	2291008222
	DD	3191466589
	DD	939524096
	DD	992555
	DD	372190496
	DD	3189934253
	DD	1476395008
	DD	1003638
	DD	54164518
	DD	1045131818
	DD	2952790016
	DD	1014781
	DD	1672962650
	DD	3192068623
	DD	2147483648
	DD	1025985
	DD	2196310654
	DD	1043982605
	DD	671088640
	DD	1037250
	DD	2286661074
	DD	1045199759
	DD	1753710392
	DD	1065423121
	DD	1753710392
	DD	1065423121
	DD	1753710392
	DD	1065423121
	DD	1753710392
	DD	1065423121
	DD	1753710392
	DD	1065423121
	DD	1753710392
	DD	1065423121
	DD	1753710392
	DD	1065423121
	DD	1753710392
	DD	1065423121
	DD	3265904883
	DD	1067799893
	DD	3265904883
	DD	1067799893
	DD	3265904883
	DD	1067799893
	DD	3265904883
	DD	1067799893
	DD	3265904883
	DD	1067799893
	DD	3265904883
	DD	1067799893
	DD	3265904883
	DD	1067799893
	DD	3265904883
	DD	1067799893
	DD	1431655453
	DD	1069897045
	DD	1431655453
	DD	1069897045
	DD	1431655453
	DD	1069897045
	DD	1431655453
	DD	1069897045
	DD	1431655453
	DD	1069897045
	DD	1431655453
	DD	1069897045
	DD	1431655453
	DD	1069897045
	DD	1431655453
	DD	1069897045
	DD	4294966876
	DD	1071644671
	DD	4294966876
	DD	1071644671
	DD	4294966876
	DD	1071644671
	DD	4294966876
	DD	1071644671
	DD	4294966876
	DD	1071644671
	DD	4294966876
	DD	1071644671
	DD	4294966876
	DD	1071644671
	DD	4294966876
	DD	1071644671
	DD	1697350398
	DD	1080497479
	DD	1697350398
	DD	1080497479
	DD	1697350398
	DD	1080497479
	DD	1697350398
	DD	1080497479
	DD	1697350398
	DD	1080497479
	DD	1697350398
	DD	1080497479
	DD	1697350398
	DD	1080497479
	DD	1697350398
	DD	1080497479
	DD	4277665792
	DD	1064709698
	DD	4277665792
	DD	1064709698
	DD	4277665792
	DD	1064709698
	DD	4277665792
	DD	1064709698
	DD	4277665792
	DD	1064709698
	DD	4277665792
	DD	1064709698
	DD	4277665792
	DD	1064709698
	DD	4277665792
	DD	1064709698
	DD	2882134964
	DD	1027723129
	DD	2882134964
	DD	1027723129
	DD	2882134964
	DD	1027723129
	DD	2882134964
	DD	1027723129
	DD	2882134964
	DD	1027723129
	DD	2882134964
	DD	1027723129
	DD	2882134964
	DD	1027723129
	DD	2882134964
	DD	1027723129
	DD	2095104
	DD	1123549184
	DD	2095104
	DD	1123549184
	DD	2095104
	DD	1123549184
	DD	2095104
	DD	1123549184
	DD	2095104
	DD	1123549184
	DD	2095104
	DD	1123549184
	DD	2095104
	DD	1123549184
	DD	2095104
	DD	1123549184
	DD	2032
	DD	0
	DD	2032
	DD	0
	DD	2032
	DD	0
	DD	2032
	DD	0
	DD	2032
	DD	0
	DD	2032
	DD	0
	DD	2032
	DD	0
	DD	2032
	DD	0
	DD	4192256
	DD	0
	DD	4192256
	DD	0
	DD	4192256
	DD	0
	DD	4192256
	DD	0
	DD	4192256
	DD	0
	DD	4192256
	DD	0
	DD	4192256
	DD	0
	DD	4192256
	DD	0
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	DD	0
	DD	3220176896
	DD	4294967295
	DD	2147483647
	DD	4294967295
	DD	2147483647
	DD	4294967295
	DD	2147483647
	DD	4294967295
	DD	2147483647
	DD	4294967295
	DD	2147483647
	DD	4294967295
	DD	2147483647
	DD	4294967295
	DD	2147483647
	DD	4294967295
	DD	2147483647
	DD	1287323203
	DD	1082531232
	DD	1287323203
	DD	1082531232
	DD	1287323203
	DD	1082531232
	DD	1287323203
	DD	1082531232
	DD	1287323203
	DD	1082531232
	DD	1287323203
	DD	1082531232
	DD	1287323203
	DD	1082531232
	DD	1287323203
	DD	1082531232
	DD	4277811695
	DD	1064709698
	DD	4277811695
	DD	1064709698
	DD	4277811695
	DD	1064709698
	DD	4277811695
	DD	1064709698
	DD	4277811695
	DD	1064709698
	DD	4277811695
	DD	1064709698
	DD	4277811695
	DD	1064709698
	DD	4277811695
	DD	1064709698
	DD	1047552
	DD	1124597760
	DD	1047552
	DD	1124597760
	DD	1047552
	DD	1124597760
	DD	1047552
	DD	1124597760
	DD	1047552
	DD	1124597760
	DD	1047552
	DD	1124597760
	DD	1047552
	DD	1124597760
	DD	1047552
	DD	1124597760
	DD	1016
	DD	0
	DD	1016
	DD	0
	DD	1016
	DD	0
	DD	1016
	DD	0
	DD	1016
	DD	0
	DD	1016
	DD	0
	DD	1016
	DD	0
	DD	1016
	DD	0
	DD	2096128
	DD	0
	DD	2096128
	DD	0
	DD	2096128
	DD	0
	DD	2096128
	DD	0
	DD	2096128
	DD	0
	DD	2096128
	DD	0
	DD	2096128
	DD	0
	DD	2096128
	DD	0
_imldExpHATab	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	1048019041
	DD	1072704666
	DD	2631457885
	DD	3161546771
	DD	3541402996
	DD	1072716208
	DD	896005651
	DD	1015861842
	DD	410360776
	DD	1072727877
	DD	1642514529
	DD	1012987726
	DD	1828292879
	DD	1072739672
	DD	1568897901
	DD	1016568486
	DD	852742562
	DD	1072751596
	DD	1882168529
	DD	1010744893
	DD	3490863953
	DD	1072763649
	DD	707771662
	DD	3163903570
	DD	2930322912
	DD	1072775834
	DD	3117806614
	DD	3163670819
	DD	1014845819
	DD	1072788152
	DD	3936719688
	DD	3162512149
	DD	3949972341
	DD	1072800603
	DD	1058231231
	DD	1015777676
	DD	828946858
	DD	1072813191
	DD	1044000608
	DD	1016786167
	DD	2288159958
	DD	1072825915
	DD	1151779725
	DD	1015705409
	DD	1853186616
	DD	1072838778
	DD	3819481236
	DD	1016499965
	DD	1709341917
	DD	1072851781
	DD	2552227826
	DD	1015039787
	DD	4112506593
	DD	1072864925
	DD	1829350193
	DD	1015216097
	DD	2799960843
	DD	1072878213
	DD	1913391796
	DD	1015756674
	DD	171030293
	DD	1072891646
	DD	1303423926
	DD	1015238005
	DD	2992903935
	DD	1072905224
	DD	1574172746
	DD	1016061241
	DD	926591435
	DD	1072918951
	DD	3427487848
	DD	3163704045
	DD	887463927
	DD	1072932827
	DD	1049900754
	DD	3161575912
	DD	1276261410
	DD	1072946854
	DD	2804567149
	DD	1015390024
	DD	569847338
	DD	1072961034
	DD	1209502043
	DD	3159926671
	DD	1617004845
	DD	1072975368
	DD	1623370769
	DD	1011049453
	DD	3049340112
	DD	1072989858
	DD	3667985273
	DD	1013894369
	DD	3577096743
	DD	1073004506
	DD	3145379760
	DD	1014403278
	DD	1990012071
	DD	1073019314
	DD	7447438
	DD	3163526196
	DD	1453150082
	DD	1073034283
	DD	3171891295
	DD	3162037958
	DD	917841882
	DD	1073049415
	DD	419288974
	DD	1016280325
	DD	3712504873
	DD	1073064711
	DD	3793507337
	DD	1016095713
	DD	363667784
	DD	1073080175
	DD	728023093
	DD	1016345318
	DD	2956612997
	DD	1073095806
	DD	1005538728
	DD	3163304901
	DD	2186617381
	DD	1073111608
	DD	2018924632
	DD	3163803357
	DD	1719614413
	DD	1073127582
	DD	3210617384
	DD	3163796463
	DD	1013258799
	DD	1073143730
	DD	3094194670
	DD	3160631279
	DD	3907805044
	DD	1073160053
	DD	2119843535
	DD	3161988964
	DD	1447192521
	DD	1073176555
	DD	508946058
	DD	3162904882
	DD	1944781191
	DD	1073193236
	DD	3108873501
	DD	3162190556
	DD	919555682
	DD	1073210099
	DD	2882956373
	DD	1013312481
	DD	2571947539
	DD	1073227145
	DD	4047189812
	DD	3163777462
	DD	2604962541
	DD	1073244377
	DD	3631372142
	DD	3163870288
	DD	1110089947
	DD	1073261797
	DD	3253791412
	DD	1015920431
	DD	2568320822
	DD	1073279406
	DD	1509121860
	DD	1014756995
	DD	2966275557
	DD	1073297207
	DD	2339118633
	DD	3160254904
	DD	2682146384
	DD	1073315202
	DD	586480042
	DD	3163702083
	DD	2191782032
	DD	1073333393
	DD	730975783
	DD	1014083580
	DD	2069751141
	DD	1073351782
	DD	576856675
	DD	3163014404
	DD	2990417245
	DD	1073370371
	DD	3552361237
	DD	3163667409
	DD	1434058175
	DD	1073389163
	DD	1853053619
	DD	1015310724
	DD	2572866477
	DD	1073408159
	DD	2462790535
	DD	1015814775
	DD	3092190715
	DD	1073427362
	DD	1457303226
	DD	3159737305
	DD	4076559943
	DD	1073446774
	DD	950899508
	DD	3160987380
	DD	2420883922
	DD	1073466398
	DD	174054861
	DD	1014300631
	DD	3716502172
	DD	1073486235
	DD	816778419
	DD	1014197934
	DD	777507147
	DD	1073506289
	DD	3507050924
	DD	1015341199
	DD	3706687593
	DD	1073526560
	DD	1821514088
	DD	1013410604
	DD	1242007932
	DD	1073547053
	DD	1073740399
	DD	3163532637
	DD	3707479175
	DD	1073567768
	DD	2789017511
	DD	1014276997
	DD	64696965
	DD	1073588710
	DD	3586233004
	DD	1015962192
	DD	863738719
	DD	1073609879
	DD	129252895
	DD	3162690849
	DD	3884662774
	DD	1073631278
	DD	1614448851
	DD	1014281732
	DD	2728693978
	DD	1073652911
	DD	2413007344
	DD	3163551506
	DD	3999357479
	DD	1073674779
	DD	1101668360
	DD	1015989180
	DD	1533953344
	DD	1073696886
	DD	835814894
	DD	1015702697
	DD	2174652632
	DD	1073719233
	DD	1301400989
	DD	1014466875
	DD	1697350398
	DD	1079448903
	DD	0
	DD	1127743488
	DD	0
	DD	1071644672
	DD	1431652600
	DD	1069897045
	DD	1431670732
	DD	1067799893
	DD	984555731
	DD	1065423122
	DD	472530941
	DD	1062650218
	DD	4277811695
	DD	1082535490
	DD	3715808466
	DD	3230016299
	DD	3576508497
	DD	3230091536
	DD	4277796864
	DD	1065758274
	DD	3164486458
	DD	1025308570
	DD	1
	DD	1048576
	DD	4294967295
	DD	2146435071
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	1073741824
	DD	0
	DD	1009778688
	DD	0
	DD	1106771968
	DD 2 DUP (0H)	
_2il0floatpacket_33	DD	000000000H,043380000H,000000000H,043380000H
_2il0floatpacket_92	DD	000000000H,0bff00000H
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
