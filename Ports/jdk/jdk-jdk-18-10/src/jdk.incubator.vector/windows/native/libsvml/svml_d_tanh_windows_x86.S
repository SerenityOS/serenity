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
	PUBLIC __svml_tanh4_ha_e9

__svml_tanh4_ha_e9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 552
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [240+rsp], ymm15
        vmovups   YMMWORD PTR [208+rsp], ymm14
        vmovups   YMMWORD PTR [272+rsp], ymm13
        vmovups   YMMWORD PTR [304+rsp], ymm12
        vmovups   YMMWORD PTR [336+rsp], ymm11
        vmovups   YMMWORD PTR [368+rsp], ymm10
        vmovups   YMMWORD PTR [400+rsp], ymm9
        vmovups   YMMWORD PTR [432+rsp], ymm8
        vmovups   YMMWORD PTR [464+rsp], ymm7
        vpxor     xmm8, xmm8, xmm8
        vmovups   YMMWORD PTR [496+rsp], ymm6
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovapd   ymm4, ymm0
        and       r13, -64
        vmovupd   YMMWORD PTR [32+r13], ymm4
        vmovups   xmm12, XMMWORD PTR [__svml_dtanh_ha_data_internal+10688]
        vmovups   xmm1, XMMWORD PTR [__svml_dtanh_ha_data_internal+10752]
        vandpd    ymm0, ymm4, YMMWORD PTR [__svml_dtanh_ha_data_internal+10432]
        vandpd    ymm5, ymm4, YMMWORD PTR [__svml_dtanh_ha_data_internal+10368]
        vmovupd   YMMWORD PTR [r13], ymm5
        mov       QWORD PTR [536+rsp], r13
        vextractf128 xmm3, ymm4, 1
        vshufps   xmm2, xmm4, xmm3, 221
        vpand     xmm9, xmm2, XMMWORD PTR [__svml_dtanh_ha_data_internal+10496]
        vpsubd    xmm6, xmm9, XMMWORD PTR [__svml_dtanh_ha_data_internal+10624]
        vpcmpgtd  xmm14, xmm1, xmm9
        vpcmpgtd  xmm7, xmm6, xmm8
        vpcmpgtd  xmm4, xmm6, xmm12
        vpand     xmm5, xmm6, xmm7
        vblendvps xmm12, xmm5, xmm12, xmm4
        vpcmpgtd  xmm13, xmm9, XMMWORD PTR [__svml_dtanh_ha_data_internal+10560]
        vpsrld    xmm3, xmm12, 10
        vpor      xmm11, xmm13, xmm14
        vmovd     ecx, xmm3
        vmovmskps r8d, xmm11
        vpextrd   eax, xmm3, 1
        movsxd    rcx, ecx
        movsxd    rax, eax
        vpextrd   r9d, xmm3, 2
        vpextrd   r10d, xmm3, 3
        movsxd    r9, r9d
        movsxd    r10, r10d
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+rdx+rcx]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+rdx+rax]
        vmovupd   xmm14, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+rdx+rax]
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+rdx+rax]
        vmovupd   xmm13, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+rdx+rcx]
        vmovupd   xmm6, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+rdx+rcx]
        vinsertf128 ymm10, ymm2, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+rdx+r9], 1
        vinsertf128 ymm15, ymm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+rdx+r10], 1
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+rdx+rcx]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+rdx+rax]
        vunpcklpd ymm9, ymm10, ymm15
        vunpckhpd ymm12, ymm10, ymm15
        vinsertf128 ymm8, ymm13, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+rdx+r9], 1
        vinsertf128 ymm4, ymm6, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+rdx+r9], 1
        vinsertf128 ymm10, ymm2, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+rdx+r9], 1
        vinsertf128 ymm7, ymm14, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+rdx+r10], 1
        vinsertf128 ymm3, ymm5, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+rdx+r10], 1
        vinsertf128 ymm15, ymm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+rdx+r10], 1
        vmovupd   xmm13, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+rdx+rcx]
        vmovupd   xmm14, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+rdx+rax]
        vunpcklpd ymm11, ymm8, ymm7
        vunpckhpd ymm8, ymm8, ymm7
        vunpcklpd ymm7, ymm4, ymm3
        vunpckhpd ymm6, ymm4, ymm3
        vunpcklpd ymm5, ymm10, ymm15
        vunpckhpd ymm4, ymm10, ymm15
        vmovupd   xmm15, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+rdx+rax]
        vmovupd   xmm10, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+rdx+rcx]
        vinsertf128 ymm2, ymm13, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+rdx+r9], 1
        vinsertf128 ymm1, ymm14, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+rdx+r10], 1
        vunpcklpd ymm3, ymm2, ymm1
        vunpckhpd ymm2, ymm2, ymm1
        vinsertf128 ymm13, ymm10, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+rdx+r9], 1
        vinsertf128 ymm14, ymm15, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+rdx+r10], 1
        vunpcklpd ymm1, ymm13, ymm14
        vunpckhpd ymm10, ymm13, ymm14
        vmovupd   xmm13, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+rdx+rcx]
        vmovupd   xmm14, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+rdx+rax]
        vinsertf128 ymm13, ymm13, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+rdx+r9], 1
        vinsertf128 ymm14, ymm14, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+rdx+r10], 1
        vunpcklpd ymm15, ymm13, ymm14
        vaddpd    ymm14, ymm0, ymm15
        vmulpd    ymm0, ymm14, ymm10
        vaddpd    ymm13, ymm0, ymm1
        vmulpd    ymm0, ymm14, ymm13
        vaddpd    ymm1, ymm0, ymm2
        vmulpd    ymm2, ymm14, ymm1
        vaddpd    ymm3, ymm2, ymm3
        vmulpd    ymm10, ymm14, ymm3
        vaddpd    ymm4, ymm10, ymm4
        vmulpd    ymm15, ymm14, ymm4
        vaddpd    ymm5, ymm15, ymm5
        vmulpd    ymm13, ymm14, ymm5
        vaddpd    ymm6, ymm13, ymm6
        vmulpd    ymm13, ymm14, ymm6
        vaddpd    ymm7, ymm13, ymm7
        vmulpd    ymm13, ymm14, ymm7
        vaddpd    ymm8, ymm13, ymm8
        vmulpd    ymm13, ymm14, ymm8
        vmulpd    ymm0, ymm14, ymm13
        vaddpd    ymm2, ymm0, ymm9
        vmovupd   ymm9, YMMWORD PTR [__svml_dtanh_ha_data_internal+10304]
        vandpd    ymm0, ymm11, ymm9
        vandpd    ymm9, ymm14, ymm9
        vmulpd    ymm13, ymm0, ymm9
        vsubpd    ymm1, ymm11, ymm0
        vsubpd    ymm14, ymm14, ymm9
        vaddpd    ymm7, ymm13, ymm12
        vmulpd    ymm11, ymm1, ymm14
        vmulpd    ymm0, ymm0, ymm14
        vmulpd    ymm14, ymm9, ymm1
        vsubpd    ymm4, ymm13, ymm7
        vaddpd    ymm11, ymm11, ymm0
        vaddpd    ymm5, ymm7, ymm4
        vaddpd    ymm3, ymm11, ymm14
        vaddpd    ymm12, ymm4, ymm12
        vsubpd    ymm4, ymm13, ymm5
        vaddpd    ymm6, ymm2, ymm3
        vaddpd    ymm5, ymm12, ymm4
        vaddpd    ymm0, ymm6, ymm5
        vaddpd    ymm1, ymm7, ymm0
        vorpd     ymm0, ymm1, YMMWORD PTR [r13]
        test      r8d, r8d
        jne       _B1_3

_B1_2::

        vmovups   ymm6, YMMWORD PTR [496+rsp]
        vmovups   ymm7, YMMWORD PTR [464+rsp]
        vmovups   ymm8, YMMWORD PTR [432+rsp]
        vmovups   ymm9, YMMWORD PTR [400+rsp]
        vmovups   ymm10, YMMWORD PTR [368+rsp]
        vmovups   ymm11, YMMWORD PTR [336+rsp]
        vmovups   ymm12, YMMWORD PTR [304+rsp]
        vmovups   ymm13, YMMWORD PTR [272+rsp]
        vmovups   ymm14, YMMWORD PTR [208+rsp]
        vmovups   ymm15, YMMWORD PTR [240+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B1_3::

        vmovupd   ymm1, YMMWORD PTR [32+r13]
        vmovupd   YMMWORD PTR [64+r13], ymm0
        vmovupd   YMMWORD PTR [r13], ymm1

_B1_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, r8d

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
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        jmp       _B1_2

_B1_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dtanh_ha_cout_rare_internal
        jmp       _B1_8
        ALIGN     16

_B1_11::

__svml_tanh4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh4_ha_e9_B1_B3:
	DD	1603841
	DD	4379769
	DD	2058353
	DD	1931363
	DD	1804378
	DD	1677393
	DD	1550408
	DD	1423423
	DD	1296438
	DD	1169453
	DD	911396
	DD	1046555
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_tanh4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh4_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_tanh4_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_6
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_tanh4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanh4_ha_l9

__svml_tanh4_ha_l9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L32::

        sub       rsp, 552
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [208+rsp], ymm15
        vmovups   YMMWORD PTR [240+rsp], ymm14
        vmovups   YMMWORD PTR [272+rsp], ymm13
        vmovups   YMMWORD PTR [304+rsp], ymm12
        vpxor     xmm13, xmm13, xmm13
        vmovups   YMMWORD PTR [336+rsp], ymm11
        vmovups   YMMWORD PTR [368+rsp], ymm10
        vmovups   YMMWORD PTR [400+rsp], ymm9
        vmovups   YMMWORD PTR [432+rsp], ymm8
        vmovups   YMMWORD PTR [464+rsp], ymm7
        vmovups   YMMWORD PTR [496+rsp], ymm6
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        and       r13, -64
        vmovupd   YMMWORD PTR [32+r13], ymm0
        vmovups   xmm10, XMMWORD PTR [__svml_dtanh_ha_data_internal+10688]
        vandpd    ymm15, ymm0, YMMWORD PTR [__svml_dtanh_ha_data_internal+10432]
        vandpd    ymm5, ymm0, YMMWORD PTR [__svml_dtanh_ha_data_internal+10368]
        vmovupd   YMMWORD PTR [r13], ymm5
        mov       QWORD PTR [536+rsp], r13
        vextracti128 xmm4, ymm0, 1
        vshufps   xmm3, xmm0, xmm4, 221
        vpand     xmm2, xmm3, XMMWORD PTR [__svml_dtanh_ha_data_internal+10496]
        vpsubd    xmm14, xmm2, XMMWORD PTR [__svml_dtanh_ha_data_internal+10624]
        vpcmpgtd  xmm12, xmm14, xmm13
        vpcmpgtd  xmm9, xmm14, xmm10
        vpand     xmm11, xmm14, xmm12
        vblendvps xmm8, xmm11, xmm10, xmm9
        vpcmpgtd  xmm1, xmm2, XMMWORD PTR [__svml_dtanh_ha_data_internal+10560]
        vpsrld    xmm7, xmm8, 10
        vmovd     ecx, xmm7
        vmovmskps r8d, xmm1
        vpextrd   eax, xmm7, 1
        test      r8d, r8d
        movsxd    rcx, ecx
        movsxd    rax, eax
        vpextrd   r9d, xmm7, 2
        vpextrd   r10d, xmm7, 3
        movsxd    r9, r9d
        movsxd    r10, r10d
        vmovupd   xmm6, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+rdx+rcx]
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+rdx+rax]
        vmovupd   xmm9, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+rdx+rax]
        vmovupd   xmm10, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+rdx+rcx]
        vmovupd   xmm0, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+rdx+rcx]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+rdx+rax]
        vinsertf128 ymm4, ymm6, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+rdx+r9], 1
        vinsertf128 ymm3, ymm5, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+rdx+r10], 1
        vmovupd   xmm6, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+rdx+rcx]
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+rdx+rax]
        vunpcklpd ymm14, ymm4, ymm3
        vunpckhpd ymm13, ymm4, ymm3
        vinsertf128 ymm8, ymm10, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+rdx+r9], 1
        vinsertf128 ymm4, ymm6, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+rdx+r9], 1
        vinsertf128 ymm7, ymm9, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+rdx+r10], 1
        vinsertf128 ymm3, ymm5, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+rdx+r10], 1
        vunpcklpd ymm10, ymm8, ymm7
        vunpckhpd ymm9, ymm8, ymm7
        vunpcklpd ymm8, ymm4, ymm3
        vunpckhpd ymm7, ymm4, ymm3
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+rdx+rcx]
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+rdx+rax]
        vinsertf128 ymm2, ymm0, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+rdx+r9], 1
        vinsertf128 ymm11, ymm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+rdx+r10], 1
        vmovupd   xmm0, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+rdx+rcx]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+rdx+rax]
        vunpcklpd ymm12, ymm2, ymm11
        vunpckhpd ymm11, ymm2, ymm11
        vinsertf128 ymm2, ymm0, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+rdx+r9], 1
        vinsertf128 ymm5, ymm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+rdx+r10], 1
        vunpcklpd ymm6, ymm2, ymm5
        vunpckhpd ymm5, ymm2, ymm5
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+rdx+rax]
        vinsertf128 ymm0, ymm4, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+rdx+r9], 1
        vinsertf128 ymm1, ymm3, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+rdx+r10], 1
        vunpcklpd ymm4, ymm0, ymm1
        vunpckhpd ymm3, ymm0, ymm1
        vmovupd   xmm0, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+rdx+rcx]
        vinsertf128 ymm1, ymm0, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+rdx+r9], 1
        vinsertf128 ymm2, ymm2, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+rdx+r10], 1
        vunpcklpd ymm0, ymm1, ymm2
        vaddpd    ymm15, ymm15, ymm0
        vfmadd213pd ymm3, ymm15, ymm4
        vfmadd213pd ymm3, ymm15, ymm5
        vfmadd213pd ymm3, ymm15, ymm6
        vfmadd213pd ymm3, ymm15, ymm7
        vfmadd213pd ymm3, ymm15, ymm8
        vfmadd213pd ymm3, ymm15, ymm9
        vfmadd213pd ymm3, ymm15, ymm10
        vfmadd213pd ymm3, ymm15, ymm11
        vmulpd    ymm0, ymm15, ymm3
        vmulpd    ymm1, ymm15, ymm12
        vfmadd213pd ymm0, ymm15, ymm14
        vfmsub213pd ymm12, ymm15, ymm1
        vaddpd    ymm6, ymm1, ymm13
        vaddpd    ymm4, ymm0, ymm12
        vsubpd    ymm12, ymm1, ymm6
        vaddpd    ymm2, ymm6, ymm12
        vaddpd    ymm13, ymm12, ymm13
        vsubpd    ymm3, ymm1, ymm2
        vaddpd    ymm5, ymm13, ymm3
        vaddpd    ymm7, ymm4, ymm5
        vaddpd    ymm8, ymm6, ymm7
        vorpd     ymm0, ymm8, YMMWORD PTR [r13]
        jne       _B2_3

_B2_2::

        vmovups   ymm6, YMMWORD PTR [496+rsp]
        vmovups   ymm7, YMMWORD PTR [464+rsp]
        vmovups   ymm8, YMMWORD PTR [432+rsp]
        vmovups   ymm9, YMMWORD PTR [400+rsp]
        vmovups   ymm10, YMMWORD PTR [368+rsp]
        vmovups   ymm11, YMMWORD PTR [336+rsp]
        vmovups   ymm12, YMMWORD PTR [304+rsp]
        vmovups   ymm13, YMMWORD PTR [272+rsp]
        vmovups   ymm14, YMMWORD PTR [240+rsp]
        vmovups   ymm15, YMMWORD PTR [208+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B2_3::

        vmovupd   ymm1, YMMWORD PTR [32+r13]
        vmovupd   YMMWORD PTR [64+r13], ymm0
        vmovupd   YMMWORD PTR [r13], ymm1

_B2_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, r8d

_B2_7::

        bt        esi, ebx
        jc        _B2_10

_B2_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B2_7

_B2_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        jmp       _B2_2

_B2_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dtanh_ha_cout_rare_internal
        jmp       _B2_8
        ALIGN     16

_B2_11::

__svml_tanh4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh4_ha_l9_B1_B3:
	DD	1603841
	DD	4379769
	DD	2058353
	DD	1931368
	DD	1804383
	DD	1677398
	DD	1550413
	DD	1423428
	DD	1296438
	DD	1169453
	DD	1042468
	DD	915483
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_tanh4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh4_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_tanh4_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_6
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_tanh4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanh1_ha_ex

__svml_tanh1_ha_ex	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L63::

        sub       rsp, 360
        mov       eax, 2147352576
        mov       ecx, 535822336
        mov       r8d, 1069416448
        mov       r9d, 7733248
        lea       r11, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [176+rsp], xmm15
        mov       edx, 2145386496
        movups    XMMWORD PTR [192+rsp], xmm14
        movd      xmm5, eax
        movups    XMMWORD PTR [208+rsp], xmm13
        movd      xmm2, ecx
        movups    XMMWORD PTR [224+rsp], xmm12
        movups    XMMWORD PTR [240+rsp], xmm11
        movd      xmm1, edx
        movups    XMMWORD PTR [256+rsp], xmm10
        movups    XMMWORD PTR [272+rsp], xmm9
        movups    XMMWORD PTR [288+rsp], xmm8
        movups    XMMWORD PTR [304+rsp], xmm7
        movups    XMMWORD PTR [320+rsp], xmm6
        movd      xmm6, r9d
        mov       QWORD PTR [168+rsp], r13
        pxor      xmm7, xmm7
        pshufd    xmm9, xmm0, 85
        lea       r13, QWORD PTR [95+rsp]
        movsd     xmm4, QWORD PTR [__svml_dtanh_ha_data_internal+10368]
        pand      xmm9, xmm5
        movsd     xmm3, QWORD PTR [__svml_dtanh_ha_data_internal+10432]
        movd      xmm5, r8d
        movups    XMMWORD PTR [241+r13], xmm0
        andps     xmm3, xmm0
        andps     xmm4, xmm0
        movdqa    xmm0, xmm9
        pcmpgtd   xmm2, xmm9
        psubd     xmm9, xmm5
        movdqa    xmm10, xmm9
        movdqa    xmm8, xmm9
        pcmpgtd   xmm10, xmm6
        pcmpgtd   xmm8, xmm7
        movups    xmm11, XMMWORD PTR [_2il0floatpacket_17]
        pand      xmm9, xmm8
        movdqa    xmm12, xmm10
        andps     xmm11, xmm10
        andnps    xmm12, xmm9
        pcmpgtd   xmm0, xmm1
        orps      xmm12, xmm11
        por       xmm0, xmm2
        psrld     xmm12, 10
        and       r13, -64
        movd      r10d, xmm12
        movmskps  eax, xmm0
        movsxd    r10, r10d
        and       eax, 1
        mov       QWORD PTR [352+rsp], r13
        movups    xmm14, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+r11+r10]
        movsd     xmm0, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2712+r11+r10]
        addsd     xmm3, xmm14
        unpcklpd  xmm0, xmm0
        mulsd     xmm0, xmm3
        movups    xmm15, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+r11+r10]
        movsd     xmm5, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2696+r11+r10]
        addsd     xmm0, xmm15
        mulsd     xmm0, xmm3
        unpcklpd  xmm5, xmm5
        movups    xmm6, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+r11+r10]
        addsd     xmm0, xmm5
        mulsd     xmm0, xmm3
        movsd     xmm7, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2680+r11+r10]
        addsd     xmm0, xmm6
        mulsd     xmm0, xmm3
        unpcklpd  xmm7, xmm7
        movups    xmm8, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+r11+r10]
        addsd     xmm0, xmm7
        mulsd     xmm0, xmm3
        movsd     xmm9, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2664+r11+r10]
        addsd     xmm0, xmm8
        mulsd     xmm0, xmm3
        unpcklpd  xmm9, xmm9
        movups    xmm10, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+r11+r10]
        addsd     xmm0, xmm9
        mulsd     xmm0, xmm3
        movsd     xmm11, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2648+r11+r10]
        addsd     xmm0, xmm10
        mulsd     xmm0, xmm3
        unpcklpd  xmm11, xmm11
        movups    xmm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+r11+r10]
        addsd     xmm0, xmm11
        mulsd     xmm0, xmm3
        movaps    xmm5, xmm1
        movsd     xmm14, QWORD PTR [__svml_dtanh_ha_data_internal+10304]
        andps     xmm5, xmm14
        andps     xmm14, xmm3
        subsd     xmm1, xmm5
        mulsd     xmm0, xmm3
        subsd     xmm3, xmm14
        movaps    xmm13, xmm1
        movaps    xmm6, xmm5
        mulsd     xmm13, xmm3
        mulsd     xmm5, xmm3
        mulsd     xmm1, xmm14
        mulsd     xmm6, xmm14
        addsd     xmm5, xmm13
        movups    xmm12, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+r11+r10]
        movaps    xmm3, xmm6
        movsd     xmm2, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2632+r11+r10]
        addsd     xmm0, xmm12
        addsd     xmm1, xmm5
        unpcklpd  xmm2, xmm2
        addsd     xmm1, xmm0
        movaps    xmm0, xmm6
        addsd     xmm0, xmm2
        movaps    xmm5, xmm0
        subsd     xmm3, xmm0
        addsd     xmm5, xmm3
        addsd     xmm2, xmm3
        subsd     xmm6, xmm5
        addsd     xmm2, xmm6
        addsd     xmm1, xmm2
        addsd     xmm0, xmm1
        orps      xmm0, xmm4
        jne       _B3_3

_B3_2::

        movups    xmm6, XMMWORD PTR [320+rsp]
        movups    xmm7, XMMWORD PTR [304+rsp]
        movups    xmm8, XMMWORD PTR [288+rsp]
        movups    xmm9, XMMWORD PTR [272+rsp]
        movups    xmm10, XMMWORD PTR [256+rsp]
        movups    xmm11, XMMWORD PTR [240+rsp]
        movups    xmm12, XMMWORD PTR [224+rsp]
        movups    xmm13, XMMWORD PTR [208+rsp]
        movups    xmm14, XMMWORD PTR [192+rsp]
        movups    xmm15, XMMWORD PTR [176+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 360
        ret

_B3_3::

        movups    xmm1, XMMWORD PTR [336+rsp]
        movsd     QWORD PTR [r13], xmm1
        movsd     QWORD PTR [64+r13], xmm0
        jne       _B3_6

_B3_4::

        movsd     xmm0, QWORD PTR [64+r13]
        jmp       _B3_2

_B3_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dtanh_ha_cout_rare_internal
        jmp       _B3_4
        ALIGN     16

_B3_7::

__svml_tanh1_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh1_ha_ex_B1_B6:
	DD	1613313
	DD	1430686
	DD	1337489
	DD	1276041
	DD	1214593
	DD	1153144
	DD	1091695
	DD	1030242
	DD	968793
	DD	907340
	DD	845887
	DD	784433
	DD	2949387

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_7
	DD	imagerel _unwind___svml_tanh1_ha_ex_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanh2_ha_l9

__svml_tanh2_ha_l9	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L88::

        sub       rsp, 392
        lea       r8, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [336+rsp], xmm15
        vmovups   XMMWORD PTR [192+rsp], xmm14
        vmovups   XMMWORD PTR [208+rsp], xmm13
        vmovups   XMMWORD PTR [224+rsp], xmm12
        vmovups   XMMWORD PTR [240+rsp], xmm11
        vmovups   XMMWORD PTR [256+rsp], xmm10
        vpxor     xmm11, xmm11, xmm11
        vmovups   XMMWORD PTR [272+rsp], xmm9
        vmovups   XMMWORD PTR [288+rsp], xmm8
        vmovups   XMMWORD PTR [304+rsp], xmm7
        vmovups   XMMWORD PTR [320+rsp], xmm6
        mov       QWORD PTR [368+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovq     xmm2, QWORD PTR [__svml_dtanh_ha_data_internal+10496]
        and       r13, -64
        vpshufd   xmm5, xmm0, 221
        vmovq     xmm15, QWORD PTR [__svml_dtanh_ha_data_internal+10624]
        vpand     xmm14, xmm5, xmm2
        vpsubd    xmm12, xmm14, xmm15
        vmovq     xmm9, QWORD PTR [__svml_dtanh_ha_data_internal+10688]
        vpcmpgtd  xmm10, xmm12, xmm11
        vpand     xmm8, xmm12, xmm10
        vpcmpgtd  xmm6, xmm12, xmm9
        vblendvps xmm7, xmm8, xmm9, xmm6
        vandpd    xmm4, xmm0, XMMWORD PTR [__svml_dtanh_ha_data_internal+10368]
        vmovupd   XMMWORD PTR [352+rsp], xmm4
        vpsrld    xmm4, xmm7, 10
        vmovd     eax, xmm4
        vmovq     xmm3, QWORD PTR [__svml_dtanh_ha_data_internal+10560]
        vpcmpgtd  xmm1, xmm14, xmm3
        vpextrd   ecx, xmm4, 1
        movsxd    rax, eax
        movsxd    rcx, ecx
        vmovupd   XMMWORD PTR [32+rsp], xmm0
        vandpd    xmm13, xmm0, XMMWORD PTR [__svml_dtanh_ha_data_internal+10432]
        vmovmskps edx, xmm1
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+r8+rax]
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+r8+rax]
        vmovupd   xmm0, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+r8+rax]
        vmovupd   xmm15, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+r8+rax]
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+r8+rcx]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+r8+rcx]
        vmovupd   xmm14, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+r8+rcx]
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+r8+rcx]
        vunpcklpd xmm12, xmm5, xmm2
        vunpckhpd xmm11, xmm5, xmm2
        vunpcklpd xmm10, xmm3, xmm1
        vunpckhpd xmm9, xmm3, xmm1
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+r8+rcx]
        vunpcklpd xmm8, xmm0, xmm14
        vunpckhpd xmm7, xmm0, xmm14
        vmovupd   xmm0, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+r8+rcx]
        vunpcklpd xmm6, xmm15, xmm4
        vunpckhpd xmm5, xmm15, xmm4
        vmovupd   xmm15, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+r8+rcx]
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+r8+rax]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+r8+rax]
        vmovupd   xmm14, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+r8+rax]
        vunpcklpd xmm4, xmm2, xmm3
        vunpckhpd xmm3, xmm2, xmm3
        vunpcklpd xmm2, xmm1, xmm0
        vunpckhpd xmm1, xmm1, xmm0
        vunpcklpd xmm0, xmm14, xmm15
        vaddpd    xmm13, xmm13, xmm0
        vfmadd213pd xmm1, xmm13, xmm2
        vfmadd213pd xmm1, xmm13, xmm3
        vfmadd213pd xmm1, xmm13, xmm4
        vfmadd213pd xmm1, xmm13, xmm5
        vfmadd213pd xmm1, xmm13, xmm6
        vfmadd213pd xmm1, xmm13, xmm7
        vfmadd213pd xmm1, xmm13, xmm8
        vfmadd213pd xmm1, xmm13, xmm9
        vmulpd    xmm0, xmm13, xmm1
        vmulpd    xmm1, xmm13, xmm10
        vfmadd213pd xmm0, xmm13, xmm12
        vfmsub213pd xmm10, xmm13, xmm1
        vaddpd    xmm6, xmm1, xmm11
        vaddpd    xmm4, xmm0, xmm10
        vsubpd    xmm10, xmm1, xmm6
        vaddpd    xmm2, xmm6, xmm10
        vaddpd    xmm11, xmm10, xmm11
        vsubpd    xmm3, xmm1, xmm2
        vaddpd    xmm5, xmm11, xmm3
        vaddpd    xmm7, xmm4, xmm5
        vaddpd    xmm8, xmm6, xmm7
        mov       QWORD PTR [376+rsp], r13
        vorpd     xmm0, xmm8, XMMWORD PTR [352+rsp]
        and       edx, 3
        jne       _B4_3

_B4_2::

        vmovups   xmm6, XMMWORD PTR [320+rsp]
        vmovups   xmm7, XMMWORD PTR [304+rsp]
        vmovups   xmm8, XMMWORD PTR [288+rsp]
        vmovups   xmm9, XMMWORD PTR [272+rsp]
        vmovups   xmm10, XMMWORD PTR [256+rsp]
        vmovups   xmm11, XMMWORD PTR [240+rsp]
        vmovups   xmm12, XMMWORD PTR [224+rsp]
        vmovups   xmm13, XMMWORD PTR [208+rsp]
        vmovups   xmm14, XMMWORD PTR [192+rsp]
        vmovups   xmm15, XMMWORD PTR [336+rsp]
        mov       r13, QWORD PTR [368+rsp]
        add       rsp, 392
        ret

_B4_3::

        vmovupd   xmm1, XMMWORD PTR [32+rsp]
        vmovupd   XMMWORD PTR [r13], xmm1
        vmovupd   XMMWORD PTR [64+r13], xmm0
        je        _B4_2

_B4_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B4_7::

        bt        esi, ebx
        jc        _B4_10

_B4_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B4_7

_B4_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B4_2

_B4_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dtanh_ha_cout_rare_internal
        jmp       _B4_8
        ALIGN     16

_B4_11::

__svml_tanh2_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh2_ha_l9_B1_B3:
	DD	1603841
	DD	3069049
	DD	1337457
	DD	1276008
	DD	1214559
	DD	1153110
	DD	1091656
	DD	1030207
	DD	968758
	DD	907309
	DD	845860
	DD	1439771
	DD	3211531

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_tanh2_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh2_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_tanh2_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_6
	DD	imagerel _B4_11
	DD	imagerel _unwind___svml_tanh2_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanh1_ha_l9

__svml_tanh1_ha_l9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L119::

        sub       rsp, 360
        mov       eax, 2147352576
        mov       ecx, 1069416448
        mov       r8d, 7733248
        vmovups   XMMWORD PTR [304+rsp], xmm15
        mov       edx, 2145386496
        vmovups   XMMWORD PTR [320+rsp], xmm14
        lea       r10, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [176+rsp], xmm13
        vmovd     xmm4, eax
        vmovups   XMMWORD PTR [192+rsp], xmm12
        vmovups   XMMWORD PTR [208+rsp], xmm11
        vmovd     xmm5, edx
        vmovups   XMMWORD PTR [224+rsp], xmm10
        vmovd     xmm10, ecx
        vmovups   XMMWORD PTR [240+rsp], xmm9
        vmovups   XMMWORD PTR [256+rsp], xmm8
        vpxor     xmm9, xmm9, xmm9
        vmovups   XMMWORD PTR [272+rsp], xmm7
        vmovups   XMMWORD PTR [288+rsp], xmm6
        mov       QWORD PTR [168+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        vpshufd   xmm3, xmm0, 85
        and       r13, -64
        vmovsd    xmm2, QWORD PTR [__svml_dtanh_ha_data_internal+10368]
        vpand     xmm15, xmm3, xmm4
        vmovsd    xmm1, QWORD PTR [__svml_dtanh_ha_data_internal+10432]
        vandpd    xmm12, xmm0, xmm2
        vmovupd   XMMWORD PTR [336+rsp], xmm0
        vandpd    xmm13, xmm0, xmm1
        vpsubd    xmm0, xmm15, xmm10
        vmovd     xmm1, r8d
        vpcmpgtd  xmm8, xmm0, xmm9
        vpcmpgtd  xmm3, xmm0, xmm1
        vpand     xmm2, xmm0, xmm8
        vpcmpgtd  xmm14, xmm15, xmm5
        vblendvps xmm4, xmm2, XMMWORD PTR [_2il0floatpacket_17], xmm3
        vmovmskps eax, xmm14
        vpsrld    xmm5, xmm4, 10
        vmovd     r9d, xmm5
        movsxd    r9, r9d
        mov       QWORD PTR [352+rsp], r13
        vmovupd   xmm15, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+r10+r9]
        vmovupd   xmm0, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+r10+r9]
        vaddsd    xmm13, xmm13, xmm15
        vmovddup  xmm11, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2712+r10+r9]
        vfmadd213sd xmm11, xmm13, xmm0
        vmovddup  xmm1, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2696+r10+r9]
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+r10+r9]
        vmovddup  xmm3, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2680+r10+r9]
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+r10+r9]
        vfmadd213sd xmm11, xmm13, xmm1
        vmovddup  xmm5, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2664+r10+r9]
        vmovupd   xmm6, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+r10+r9]
        vmovddup  xmm7, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2648+r10+r9]
        vmovupd   xmm9, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+r10+r9]
        vfmadd213sd xmm11, xmm13, xmm2
        vmovddup  xmm10, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2632+r10+r9]
        vfmadd213sd xmm11, xmm13, xmm3
        vmovupd   xmm8, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+r10+r9]
        vfmadd213sd xmm11, xmm13, xmm4
        vfmadd213sd xmm11, xmm13, xmm5
        vfmadd213sd xmm11, xmm13, xmm6
        vfmadd213sd xmm11, xmm13, xmm7
        vmulsd    xmm6, xmm11, xmm13
        vmulsd    xmm11, xmm9, xmm13
        vfmadd213sd xmm6, xmm13, xmm8
        vfmsub213sd xmm9, xmm13, xmm11
        vaddsd    xmm2, xmm11, xmm10
        vsubsd    xmm7, xmm11, xmm2
        vaddsd    xmm1, xmm9, xmm6
        vaddsd    xmm0, xmm2, xmm7
        vaddsd    xmm7, xmm10, xmm7
        vsubsd    xmm10, xmm11, xmm0
        vaddsd    xmm11, xmm7, xmm10
        vaddsd    xmm3, xmm1, xmm11
        vaddsd    xmm4, xmm2, xmm3
        vorpd     xmm0, xmm4, xmm12
        and       eax, 1
        jne       _B5_3

_B5_2::

        vmovups   xmm6, XMMWORD PTR [288+rsp]
        vmovups   xmm7, XMMWORD PTR [272+rsp]
        vmovups   xmm8, XMMWORD PTR [256+rsp]
        vmovups   xmm9, XMMWORD PTR [240+rsp]
        vmovups   xmm10, XMMWORD PTR [224+rsp]
        vmovups   xmm11, XMMWORD PTR [208+rsp]
        vmovups   xmm12, XMMWORD PTR [192+rsp]
        vmovups   xmm13, XMMWORD PTR [176+rsp]
        vmovups   xmm14, XMMWORD PTR [320+rsp]
        vmovups   xmm15, XMMWORD PTR [304+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 360
        ret

_B5_3::

        vmovupd   xmm1, XMMWORD PTR [336+rsp]
        vmovsd    QWORD PTR [r13], xmm1
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B5_6

_B5_4::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B5_2

_B5_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dtanh_ha_cout_rare_internal
        jmp       _B5_4
        ALIGN     16

_B5_7::

__svml_tanh1_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh1_ha_l9_B1_B6:
	DD	1612289
	DD	1430682
	DD	1206418
	DD	1144969
	DD	1083515
	DD	1022066
	DD	960613
	DD	899160
	DD	837711
	DD	776258
	DD	1370162
	DD	1308708
	DD	2949387

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_7
	DD	imagerel _unwind___svml_tanh1_ha_l9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanh2_ha_ex

__svml_tanh2_ha_ex	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L144::

        sub       rsp, 392
        lea       r8, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [272+rsp], xmm15
        movups    XMMWORD PTR [288+rsp], xmm14
        movups    XMMWORD PTR [320+rsp], xmm13
        pxor      xmm14, xmm14
        movups    XMMWORD PTR [336+rsp], xmm12
        movups    XMMWORD PTR [192+rsp], xmm11
        movups    XMMWORD PTR [208+rsp], xmm10
        movups    XMMWORD PTR [304+rsp], xmm9
        movups    XMMWORD PTR [224+rsp], xmm8
        movups    XMMWORD PTR [240+rsp], xmm7
        movups    XMMWORD PTR [256+rsp], xmm6
        mov       QWORD PTR [368+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movq      xmm4, QWORD PTR [__svml_dtanh_ha_data_internal+10496]
        and       r13, -64
        pshufd    xmm11, xmm0, 221
        movq      xmm13, QWORD PTR [__svml_dtanh_ha_data_internal+10624]
        pand      xmm11, xmm4
        movq      xmm2, QWORD PTR [__svml_dtanh_ha_data_internal+10752]
        movdqa    xmm3, xmm11
        movups    xmm1, XMMWORD PTR [__svml_dtanh_ha_data_internal+10368]
        pcmpgtd   xmm2, xmm11
        psubd     xmm11, xmm13
        andps     xmm1, xmm0
        movq      xmm9, QWORD PTR [__svml_dtanh_ha_data_internal+10688]
        movdqa    xmm15, xmm11
        movups    XMMWORD PTR [352+rsp], xmm1
        movdqa    xmm1, xmm11
        pcmpgtd   xmm1, xmm9
        pcmpgtd   xmm15, xmm14
        pand      xmm11, xmm15
        movdqa    xmm10, xmm1
        andnps    xmm10, xmm11
        andps     xmm9, xmm1
        orps      xmm10, xmm9
        psrld     xmm10, 10
        movd      edx, xmm10
        pshufd    xmm8, xmm10, 1
        movq      xmm5, QWORD PTR [__svml_dtanh_ha_data_internal+10560]
        movd      ecx, xmm8
        pcmpgtd   xmm3, xmm5
        por       xmm3, xmm2
        movups    xmm12, XMMWORD PTR [__svml_dtanh_ha_data_internal+10432]
        movsxd    rdx, edx
        andps     xmm12, xmm0
        movsxd    rcx, ecx
        movups    XMMWORD PTR [32+rsp], xmm0
        movmskps  eax, xmm3
        movups    xmm11, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+r8+rdx]
        movups    xmm8, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+r8+rdx]
        movaps    xmm10, xmm11
        movups    xmm6, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+r8+rcx]
        movaps    xmm9, xmm8
        movups    xmm7, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+r8+rcx]
        and       eax, 3
        unpcklpd  xmm10, xmm6
        unpckhpd  xmm11, xmm6
        movups    xmm6, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+r8+rdx]
        unpcklpd  xmm9, xmm7
        unpckhpd  xmm8, xmm7
        movaps    xmm7, xmm6
        movups    xmm4, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+r8+rcx]
        unpcklpd  xmm7, xmm4
        unpckhpd  xmm6, xmm4
        movups    xmm4, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+r8+rdx]
        movups    xmm2, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+r8+rcx]
        movaps    xmm5, xmm4
        unpcklpd  xmm5, xmm2
        unpckhpd  xmm4, xmm2
        movups    xmm2, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+r8+rdx]
        movups    xmm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+r8+rdx]
        movaps    xmm3, xmm2
        movups    xmm0, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+r8+rcx]
        movups    xmm13, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+r8+rcx]
        unpcklpd  xmm3, xmm0
        unpckhpd  xmm2, xmm0
        movaps    xmm0, xmm1
        unpcklpd  xmm0, xmm13
        unpckhpd  xmm1, xmm13
        movups    xmm13, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+r8+rdx]
        movups    xmm14, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+r8+rcx]
        movaps    xmm15, xmm13
        unpcklpd  xmm15, xmm14
        addpd     xmm12, xmm15
        mulpd     xmm1, xmm12
        addpd     xmm1, xmm0
        mulpd     xmm1, xmm12
        addpd     xmm1, xmm2
        mulpd     xmm1, xmm12
        addpd     xmm1, xmm3
        mulpd     xmm1, xmm12
        addpd     xmm1, xmm4
        mulpd     xmm1, xmm12
        addpd     xmm1, xmm5
        mulpd     xmm1, xmm12
        addpd     xmm1, xmm6
        mulpd     xmm1, xmm12
        addpd     xmm1, xmm7
        mulpd     xmm1, xmm12
        addpd     xmm1, xmm8
        mulpd     xmm1, xmm12
        movups    xmm5, XMMWORD PTR [__svml_dtanh_ha_data_internal+10304]
        movaps    xmm3, xmm5
        andps     xmm3, xmm9
        andps     xmm5, xmm12
        subpd     xmm9, xmm3
        mulpd     xmm1, xmm12
        subpd     xmm12, xmm5
        addpd     xmm1, xmm10
        movaps    xmm0, xmm9
        movaps    xmm2, xmm3
        mulpd     xmm0, xmm12
        mulpd     xmm3, xmm12
        mulpd     xmm2, xmm5
        mulpd     xmm5, xmm9
        addpd     xmm0, xmm3
        addpd     xmm0, xmm5
        addpd     xmm1, xmm0
        movaps    xmm0, xmm11
        movaps    xmm3, xmm2
        addpd     xmm0, xmm2
        subpd     xmm3, xmm0
        movaps    xmm12, xmm0
        addpd     xmm12, xmm3
        addpd     xmm3, xmm11
        subpd     xmm2, xmm12
        addpd     xmm3, xmm2
        addpd     xmm1, xmm3
        addpd     xmm0, xmm1
        mov       QWORD PTR [376+rsp], r13
        orps      xmm0, XMMWORD PTR [352+rsp]
        jne       _B6_3

_B6_2::

        movups    xmm6, XMMWORD PTR [256+rsp]
        movups    xmm7, XMMWORD PTR [240+rsp]
        movups    xmm8, XMMWORD PTR [224+rsp]
        movups    xmm9, XMMWORD PTR [304+rsp]
        movups    xmm10, XMMWORD PTR [208+rsp]
        movups    xmm11, XMMWORD PTR [192+rsp]
        movups    xmm12, XMMWORD PTR [336+rsp]
        movups    xmm13, XMMWORD PTR [320+rsp]
        movups    xmm14, XMMWORD PTR [288+rsp]
        movups    xmm15, XMMWORD PTR [272+rsp]
        mov       r13, QWORD PTR [368+rsp]
        add       rsp, 392
        ret

_B6_3::

        movups    xmm1, XMMWORD PTR [32+rsp]
        movups    XMMWORD PTR [r13], xmm1
        movups    XMMWORD PTR [64+r13], xmm0
        je        _B6_2

_B6_6::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

_B6_7::

        mov       ecx, ebx
        mov       edx, 1
        shl       edx, cl
        test      esi, edx
        jne       _B6_10

_B6_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B6_7

_B6_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [64+r13]
        jmp       _B6_2

_B6_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dtanh_ha_cout_rare_internal
        jmp       _B6_8
        ALIGN     16

_B6_11::

__svml_tanh2_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh2_ha_ex_B1_B3:
	DD	1603329
	DD	3069047
	DD	1075311
	DD	1013863
	DD	952415
	DD	1284182
	DD	895053
	DD	833604
	DD	1427515
	DD	1366061
	DD	1239076
	DD	1177627
	DD	3211531

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_tanh2_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh2_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_tanh2_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_6
	DD	imagerel _B6_11
	DD	imagerel _unwind___svml_tanh2_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanh2_ha_e9

__svml_tanh2_ha_e9	PROC	EXPORT

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L175::

        sub       rsp, 392
        vmovapd   xmm4, xmm0
        vmovups   XMMWORD PTR [336+rsp], xmm15
        lea       r8, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [192+rsp], xmm14
        vmovups   XMMWORD PTR [208+rsp], xmm13
        vmovups   XMMWORD PTR [224+rsp], xmm12
        vmovups   XMMWORD PTR [240+rsp], xmm11
        vmovups   XMMWORD PTR [256+rsp], xmm10
        vmovups   XMMWORD PTR [272+rsp], xmm9
        vmovups   XMMWORD PTR [288+rsp], xmm8
        vmovups   XMMWORD PTR [304+rsp], xmm7
        vpxor     xmm8, xmm8, xmm8
        vmovups   XMMWORD PTR [320+rsp], xmm6
        mov       QWORD PTR [368+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovq     xmm2, QWORD PTR [__svml_dtanh_ha_data_internal+10496]
        and       r13, -64
        vpshufd   xmm5, xmm4, 221
        vmovq     xmm11, QWORD PTR [__svml_dtanh_ha_data_internal+10624]
        vpand     xmm10, xmm5, xmm2
        vpsubd    xmm6, xmm10, xmm11
        vmovq     xmm15, QWORD PTR [__svml_dtanh_ha_data_internal+10688]
        vpcmpgtd  xmm9, xmm6, xmm8
        vmovupd   XMMWORD PTR [32+rsp], xmm4
        vpand     xmm7, xmm6, xmm9
        vandpd    xmm0, xmm4, XMMWORD PTR [__svml_dtanh_ha_data_internal+10432]
        vandpd    xmm1, xmm4, XMMWORD PTR [__svml_dtanh_ha_data_internal+10368]
        vpcmpgtd  xmm4, xmm6, xmm15
        vblendvps xmm5, xmm7, xmm15, xmm4
        vmovq     xmm3, QWORD PTR [__svml_dtanh_ha_data_internal+10560]
        vpsrld    xmm2, xmm5, 10
        vmovd     eax, xmm2
        vmovq     xmm13, QWORD PTR [__svml_dtanh_ha_data_internal+10752]
        vmovupd   XMMWORD PTR [352+rsp], xmm1
        vpcmpgtd  xmm1, xmm10, xmm3
        vpextrd   ecx, xmm2, 1
        vpcmpgtd  xmm12, xmm13, xmm10
        movsxd    rax, eax
        vpor      xmm14, xmm1, xmm12
        movsxd    rcx, ecx
        vmovmskps edx, xmm14
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+r8+rax]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+r8+rax]
        vmovupd   xmm15, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+r8+rcx]
        vmovupd   xmm10, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+r8+rcx]
        vmovupd   xmm9, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+r8+rcx]
        vmovupd   xmm7, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+r8+rcx]
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+r8+rcx]
        vunpcklpd xmm14, xmm3, xmm15
        vunpckhpd xmm13, xmm3, xmm15
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+r8+rcx]
        vunpcklpd xmm12, xmm1, xmm10
        vunpckhpd xmm10, xmm1, xmm10
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+r8+rcx]
        vmovupd   xmm8, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+r8+rax]
        vmovupd   xmm6, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+r8+rax]
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+r8+rax]
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+r8+rax]
        vmovupd   xmm15, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+r8+rax]
        vunpcklpd xmm11, xmm8, xmm9
        vunpckhpd xmm8, xmm8, xmm9
        vunpcklpd xmm9, xmm6, xmm7
        vunpckhpd xmm6, xmm6, xmm7
        vunpcklpd xmm7, xmm4, xmm5
        vunpckhpd xmm4, xmm4, xmm5
        vunpcklpd xmm5, xmm2, xmm3
        vunpckhpd xmm3, xmm2, xmm3
        vunpcklpd xmm2, xmm15, xmm1
        vaddpd    xmm1, xmm0, xmm2
        vmulpd    xmm0, xmm1, xmm3
        vaddpd    xmm0, xmm0, xmm5
        vmulpd    xmm2, xmm1, xmm0
        vaddpd    xmm3, xmm2, xmm4
        vmulpd    xmm4, xmm1, xmm3
        vaddpd    xmm5, xmm4, xmm7
        vmulpd    xmm7, xmm1, xmm5
        vaddpd    xmm6, xmm7, xmm6
        vmulpd    xmm15, xmm1, xmm6
        vaddpd    xmm9, xmm15, xmm9
        vmulpd    xmm0, xmm1, xmm9
        vaddpd    xmm8, xmm0, xmm8
        vmulpd    xmm0, xmm1, xmm8
        vaddpd    xmm11, xmm0, xmm11
        vmulpd    xmm0, xmm1, xmm11
        vaddpd    xmm10, xmm0, xmm10
        vmulpd    xmm0, xmm1, xmm10
        vmulpd    xmm2, xmm1, xmm0
        vaddpd    xmm5, xmm2, xmm14
        vmovupd   xmm14, XMMWORD PTR [__svml_dtanh_ha_data_internal+10304]
        vandpd    xmm2, xmm12, xmm14
        vandpd    xmm14, xmm1, xmm14
        vmulpd    xmm0, xmm2, xmm14
        vsubpd    xmm3, xmm12, xmm2
        vaddpd    xmm10, xmm0, xmm13
        vmulpd    xmm4, xmm14, xmm3
        vsubpd    xmm7, xmm0, xmm10
        vsubpd    xmm12, xmm1, xmm14
        vaddpd    xmm8, xmm10, xmm7
        vmulpd    xmm1, xmm3, xmm12
        vmulpd    xmm2, xmm2, xmm12
        vsubpd    xmm0, xmm0, xmm8
        vaddpd    xmm12, xmm1, xmm2
        vaddpd    xmm6, xmm12, xmm4
        vaddpd    xmm13, xmm7, xmm13
        vaddpd    xmm9, xmm5, xmm6
        vaddpd    xmm7, xmm13, xmm0
        vaddpd    xmm1, xmm9, xmm7
        vaddpd    xmm2, xmm10, xmm1
        mov       QWORD PTR [376+rsp], r13
        vorpd     xmm0, xmm2, XMMWORD PTR [352+rsp]
        and       edx, 3
        jne       _B7_3

_B7_2::

        vmovups   xmm6, XMMWORD PTR [320+rsp]
        vmovups   xmm7, XMMWORD PTR [304+rsp]
        vmovups   xmm8, XMMWORD PTR [288+rsp]
        vmovups   xmm9, XMMWORD PTR [272+rsp]
        vmovups   xmm10, XMMWORD PTR [256+rsp]
        vmovups   xmm11, XMMWORD PTR [240+rsp]
        vmovups   xmm12, XMMWORD PTR [224+rsp]
        vmovups   xmm13, XMMWORD PTR [208+rsp]
        vmovups   xmm14, XMMWORD PTR [192+rsp]
        vmovups   xmm15, XMMWORD PTR [336+rsp]
        mov       r13, QWORD PTR [368+rsp]
        add       rsp, 392
        ret

_B7_3::

        vmovupd   xmm1, XMMWORD PTR [32+rsp]
        vmovupd   XMMWORD PTR [r13], xmm1
        vmovupd   XMMWORD PTR [64+r13], xmm0
        je        _B7_2

_B7_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B7_7::

        bt        esi, ebx
        jc        _B7_10

_B7_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B7_7

_B7_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B7_2

_B7_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dtanh_ha_cout_rare_internal
        jmp       _B7_8
        ALIGN     16

_B7_11::

__svml_tanh2_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh2_ha_e9_B1_B3:
	DD	1604865
	DD	3069053
	DD	1337461
	DD	1276007
	DD	1214558
	DD	1153109
	DD	1091660
	DD	1030211
	DD	968762
	DD	907313
	DD	845864
	DD	1439768
	DD	3211531

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_6
	DD	imagerel _unwind___svml_tanh2_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh2_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B7_1
	DD	imagerel _B7_6
	DD	imagerel _unwind___svml_tanh2_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_6
	DD	imagerel _B7_11
	DD	imagerel _unwind___svml_tanh2_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST7:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanh1_ha_e9

__svml_tanh1_ha_e9	PROC	EXPORT

_B8_1::

        DB        243
        DB        15
        DB        30
        DB        250
L206::

        sub       rsp, 360
        mov       eax, 2147352576
        mov       r8d, 1069416448
        mov       r9d, 7733248
        vpshufd   xmm4, xmm0, 85
        mov       edx, 2145386496
        vmovups   XMMWORD PTR [304+rsp], xmm15
        mov       ecx, 535822336
        vmovd     xmm3, eax
        lea       r11, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [320+rsp], xmm14
        vmovups   XMMWORD PTR [288+rsp], xmm13
        vmovd     xmm2, edx
        vmovups   XMMWORD PTR [176+rsp], xmm12
        vmovups   XMMWORD PTR [192+rsp], xmm11
        vmovups   XMMWORD PTR [208+rsp], xmm10
        vpand     xmm10, xmm4, xmm3
        vmovups   XMMWORD PTR [224+rsp], xmm9
        vmovd     xmm9, r8d
        vmovups   XMMWORD PTR [240+rsp], xmm8
        vpcmpgtd  xmm13, xmm10, xmm2
        vmovups   XMMWORD PTR [256+rsp], xmm7
        vpxor     xmm8, xmm8, xmm8
        vmovups   XMMWORD PTR [272+rsp], xmm6
        vpsubd    xmm6, xmm10, xmm9
        mov       QWORD PTR [168+rsp], r13
        vpcmpgtd  xmm7, xmm6, xmm8
        vmovsd    xmm5, QWORD PTR [__svml_dtanh_ha_data_internal+10368]
        vpand     xmm4, xmm6, xmm7
        vandpd    xmm15, xmm0, xmm5
        vmovd     xmm5, r9d
        vpcmpgtd  xmm3, xmm6, xmm5
        lea       r13, QWORD PTR [95+rsp]
        vblendvps xmm2, xmm4, XMMWORD PTR [_2il0floatpacket_17], xmm3
        and       r13, -64
        vmovsd    xmm1, QWORD PTR [__svml_dtanh_ha_data_internal+10432]
        vpsrld    xmm11, xmm2, 10
        vandpd    xmm14, xmm0, xmm1
        vmovd     r10d, xmm11
        vmovupd   XMMWORD PTR [336+rsp], xmm0
        vmovd     xmm0, ecx
        vpcmpgtd  xmm1, xmm0, xmm10
        movsxd    r10, r10d
        vpor      xmm12, xmm13, xmm1
        vmovmskps eax, xmm12
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2720+r11+r10]
        vaddsd    xmm1, xmm14, xmm1
        vmovddup  xmm11, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2712+r11+r10]
        vmulsd    xmm14, xmm11, xmm1
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2704+r11+r10]
        vaddsd    xmm0, xmm14, xmm2
        vmulsd    xmm2, xmm0, xmm1
        vmovddup  xmm3, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2696+r11+r10]
        vaddsd    xmm3, xmm2, xmm3
        vmulsd    xmm11, xmm3, xmm1
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2688+r11+r10]
        vaddsd    xmm4, xmm11, xmm4
        vmulsd    xmm14, xmm4, xmm1
        vmovddup  xmm5, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2680+r11+r10]
        vaddsd    xmm5, xmm14, xmm5
        vmulsd    xmm0, xmm5, xmm1
        vmovupd   xmm6, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2672+r11+r10]
        vaddsd    xmm6, xmm0, xmm6
        vmulsd    xmm0, xmm6, xmm1
        vmovddup  xmm7, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2664+r11+r10]
        vaddsd    xmm7, xmm0, xmm7
        vmulsd    xmm0, xmm7, xmm1
        vmovupd   xmm8, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2656+r11+r10]
        vaddsd    xmm8, xmm0, xmm8
        vmulsd    xmm0, xmm8, xmm1
        vmovddup  xmm9, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2648+r11+r10]
        vaddsd    xmm9, xmm0, xmm9
        vmulsd    xmm0, xmm9, xmm1
        vmulsd    xmm2, xmm0, xmm1
        vmovupd   xmm10, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2624+r11+r10]
        vaddsd    xmm5, xmm2, xmm10
        vmovupd   xmm12, XMMWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2640+r11+r10]
        vmovsd    xmm10, QWORD PTR [__svml_dtanh_ha_data_internal+10304]
        vandpd    xmm2, xmm12, xmm10
        vandpd    xmm4, xmm1, xmm10
        vmulsd    xmm0, xmm2, xmm4
        vsubsd    xmm3, xmm12, xmm2
        vsubsd    xmm1, xmm1, xmm4
        vmovddup  xmm13, QWORD PTR [imagerel(__svml_dtanh_ha_data_internal)+2632+r11+r10]
        vaddsd    xmm9, xmm0, xmm13
        vmulsd    xmm10, xmm3, xmm1
        vmulsd    xmm12, xmm2, xmm1
        vmulsd    xmm1, xmm3, xmm4
        vsubsd    xmm6, xmm0, xmm9
        vaddsd    xmm2, xmm12, xmm10
        vaddsd    xmm7, xmm9, xmm6
        vaddsd    xmm12, xmm1, xmm2
        vsubsd    xmm0, xmm0, xmm7
        vaddsd    xmm8, xmm12, xmm5
        vaddsd    xmm13, xmm13, xmm6
        mov       QWORD PTR [352+rsp], r13
        vaddsd    xmm6, xmm13, xmm0
        vaddsd    xmm1, xmm8, xmm6
        vaddsd    xmm2, xmm9, xmm1
        vorpd     xmm0, xmm2, xmm15
        and       eax, 1
        jne       _B8_3

_B8_2::

        vmovups   xmm6, XMMWORD PTR [272+rsp]
        vmovups   xmm7, XMMWORD PTR [256+rsp]
        vmovups   xmm8, XMMWORD PTR [240+rsp]
        vmovups   xmm9, XMMWORD PTR [224+rsp]
        vmovups   xmm10, XMMWORD PTR [208+rsp]
        vmovups   xmm11, XMMWORD PTR [192+rsp]
        vmovups   xmm12, XMMWORD PTR [176+rsp]
        vmovups   xmm13, XMMWORD PTR [288+rsp]
        vmovups   xmm14, XMMWORD PTR [320+rsp]
        vmovups   xmm15, XMMWORD PTR [304+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 360
        ret

_B8_3::

        vmovupd   xmm1, XMMWORD PTR [336+rsp]
        vmovsd    QWORD PTR [r13], xmm1
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B8_6

_B8_4::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B8_2

_B8_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dtanh_ha_cout_rare_internal
        jmp       _B8_4
        ALIGN     16

_B8_7::

__svml_tanh1_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh1_ha_e9_B1_B6:
	DD	1618689
	DD	1430707
	DD	1140902
	DD	1079448
	DD	1017995
	DD	956541
	DD	895088
	DD	833639
	DD	772190
	DD	1235025
	DD	1370184
	DD	1308719
	DD	2949387

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_1
	DD	imagerel _B8_7
	DD	imagerel _unwind___svml_tanh1_ha_e9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST8:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanh8_ha_z0

__svml_tanh8_ha_z0	PROC	EXPORT

_B9_1::

        DB        243
        DB        15
        DB        30
        DB        250
L231::

        sub       rsp, 1336
        vpsrlq    zmm23, zmm0, 32
        vpxor     ymm5, ymm5, ymm5
        vmovups   ZMMWORD PTR [1072+rsp], zmm21
        vmovups   ZMMWORD PTR [1136+rsp], zmm20
        vmovups   ZMMWORD PTR [1200+rsp], zmm19
        vmovups   ZMMWORD PTR [1264+rsp], zmm18
        vpmovqd   ymm1, zmm23
        mov       QWORD PTR [1064+rsp], r13
        lea       r13, QWORD PTR [927+rsp]
        vpand     ymm3, ymm1, YMMWORD PTR [__svml_dtanh_ha_data_internal+2432]
        and       r13, -64
        vmovups   zmm25, ZMMWORD PTR [__svml_dtanh_ha_data_internal]
        vmovups   zmm26, ZMMWORD PTR [__svml_dtanh_ha_data_internal+2176]
        vmovups   zmm18, ZMMWORD PTR [__svml_dtanh_ha_data_internal+2304]
        vmovups   zmm27, ZMMWORD PTR [__svml_dtanh_ha_data_internal+2048]
        vmovups   zmm28, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1920]
        vmovups   zmm29, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1792]
        vmovups   zmm30, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1664]
        vmovups   zmm31, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1536]
        vmovups   zmm23, ZMMWORD PTR [__svml_dtanh_ha_data_internal+768]
        vmovups   zmm20, ZMMWORD PTR [__svml_dtanh_ha_data_internal+384]
        vmovups   zmm21, ZMMWORD PTR [__svml_dtanh_ha_data_internal+128]
        vpsubd    ymm4, ymm3, YMMWORD PTR [__svml_dtanh_ha_data_internal+2496]
        vandpd    zmm24, zmm0, ZMMWORD PTR [__svml_dtanh_ha_data_internal+10432]
        vpcmpgtd  ymm2, ymm3, YMMWORD PTR [__svml_dtanh_ha_data_internal+10560]
        vpmaxsd   ymm1, ymm4, ymm5
        vmovmskps r8d, ymm2
        vpminsd   ymm2, ymm1, YMMWORD PTR [__svml_dtanh_ha_data_internal+2560]
        vpsrld    ymm3, ymm2, 19
        vmovups   zmm1, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1408]
        vmovups   zmm2, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1280]
        vmovups   zmm4, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1024]
        vmovups   zmm5, ZMMWORD PTR [__svml_dtanh_ha_data_internal+896]
        vpmovzxdq zmm22, ymm3
        vmovups   zmm3, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1152]
        vpermt2pd zmm25, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+64]
        vpermt2pd zmm26, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+2240]
        vpermt2pd zmm18, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+2368]
        vsubpd    zmm19, zmm24, zmm25 {rn-sae}
        vpermt2pd zmm27, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+2112]
        vpermt2pd zmm28, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1984]
        vpermt2pd zmm29, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1856]
        vpermt2pd zmm30, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1728]
        vpermt2pd zmm31, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1600]
        vpermt2pd zmm1, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1472]
        vpermt2pd zmm2, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1344]
        vpermt2pd zmm3, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1216]
        vpermt2pd zmm4, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+1088]
        vpermt2pd zmm5, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+960]
        vpermt2pd zmm23, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+832]
        vpermt2pd zmm20, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+448]
        vpermt2pd zmm21, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+192]
        vfmadd213pd zmm18, zmm19, zmm26 {rn-sae}
        vmovups   zmm24, ZMMWORD PTR [__svml_dtanh_ha_data_internal+640]
        vmovups   zmm25, ZMMWORD PTR [__svml_dtanh_ha_data_internal+512]
        vmovups   zmm26, ZMMWORD PTR [__svml_dtanh_ha_data_internal+256]
        vfmadd213pd zmm18, zmm19, zmm27 {rn-sae}
        vpermt2pd zmm24, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+704]
        vpermt2pd zmm25, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+576]
        vpermt2pd zmm26, zmm22, ZMMWORD PTR [__svml_dtanh_ha_data_internal+320]
        vfmadd213pd zmm18, zmm19, zmm28 {rn-sae}
        vfmadd213pd zmm18, zmm19, zmm29 {rn-sae}
        vfmadd213pd zmm18, zmm19, zmm30 {rn-sae}
        vfmadd213pd zmm18, zmm19, zmm31 {rn-sae}
        vfmadd213pd zmm18, zmm19, zmm1 {rn-sae}
        vfmadd213pd zmm18, zmm19, zmm2 {rn-sae}
        vfmadd213pd zmm18, zmm19, zmm3 {rn-sae}
        vfmadd213pd zmm18, zmm19, zmm4 {rn-sae}
        vfmadd213pd zmm18, zmm19, zmm5 {rn-sae}
        vfmadd213pd zmm18, zmm19, zmm23 {rn-sae}
        vfmadd213pd zmm18, zmm19, zmm24 {rn-sae}
        vfmadd213pd zmm18, zmm19, zmm25 {rn-sae}
        vmulpd    zmm18, zmm18, zmm19 {rn-sae}
        vfmadd213pd zmm18, zmm19, zmm26 {rn-sae}
        vfmadd213pd zmm20, zmm19, zmm18 {rn-sae}
        vaddpd    zmm1, zmm20, zmm21 {rn-sae}
        mov       QWORD PTR [1328+rsp], r13
        vpternlogq zmm1, zmm0, ZMMWORD PTR [__svml_dtanh_ha_data_internal+10368], 248
        test      r8d, r8d
        jne       _B9_3

_B9_2::

        vmovups   zmm18, ZMMWORD PTR [1264+rsp]
        vmovups   zmm19, ZMMWORD PTR [1200+rsp]
        vmovups   zmm20, ZMMWORD PTR [1136+rsp]
        vmovups   zmm21, ZMMWORD PTR [1072+rsp]
        mov       r13, QWORD PTR [1064+rsp]
        vmovaps   zmm0, zmm1
        add       rsp, 1336
        ret

_B9_3::

        vstmxcsr  DWORD PTR [1056+rsp]

_B9_4::

        movzx     edx, WORD PTR [1056+rsp]
        mov       eax, edx
        or        eax, 8064
        cmp       edx, eax
        je        _B9_6

_B9_5::

        mov       DWORD PTR [1056+rsp], eax
        vldmxcsr  DWORD PTR [1056+rsp]

_B9_6::

        vmovups   ZMMWORD PTR [r13], zmm0
        vmovups   ZMMWORD PTR [64+r13], zmm1
        test      r8d, r8d
        jne       _B9_11

_B9_7::

        cmp       edx, eax
        je        _B9_2

_B9_8::

        vstmxcsr  DWORD PTR [1056+rsp]
        mov       eax, DWORD PTR [1056+rsp]

_B9_9::

        and       eax, -8065
        or        eax, edx
        mov       DWORD PTR [1056+rsp], eax
        vldmxcsr  DWORD PTR [1056+rsp]
        jmp       _B9_2

_B9_11::

        xor       ecx, ecx
        kmovw     WORD PTR [824+rsp], k4
        kmovw     WORD PTR [816+rsp], k5
        kmovw     WORD PTR [808+rsp], k6
        kmovw     WORD PTR [800+rsp], k7
        vmovups   ZMMWORD PTR [736+rsp], zmm6
        vmovups   ZMMWORD PTR [672+rsp], zmm7
        vmovups   ZMMWORD PTR [608+rsp], zmm8
        vmovups   ZMMWORD PTR [544+rsp], zmm9
        vmovups   ZMMWORD PTR [480+rsp], zmm10
        vmovups   ZMMWORD PTR [416+rsp], zmm11
        vmovups   ZMMWORD PTR [352+rsp], zmm12
        vmovups   ZMMWORD PTR [288+rsp], zmm13
        vmovups   ZMMWORD PTR [224+rsp], zmm14
        vmovups   ZMMWORD PTR [160+rsp], zmm15
        vmovups   ZMMWORD PTR [96+rsp], zmm16
        vmovups   ZMMWORD PTR [32+rsp], zmm17
        mov       QWORD PTR [848+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [840+rsp], rsi
        mov       esi, edx
        mov       QWORD PTR [832+rsp], rdi
        mov       edi, r8d
        mov       QWORD PTR [856+rsp], rbp
        mov       ebp, eax

_B9_12::

        bt        edi, ebx
        jc        _B9_15

_B9_13::

        inc       ebx
        cmp       ebx, 8
        jl        _B9_12

_B9_14::

        kmovw     k4, WORD PTR [824+rsp]
        mov       eax, ebp
        kmovw     k5, WORD PTR [816+rsp]
        kmovw     k6, WORD PTR [808+rsp]
        kmovw     k7, WORD PTR [800+rsp]
        vmovups   zmm6, ZMMWORD PTR [736+rsp]
        vmovups   zmm7, ZMMWORD PTR [672+rsp]
        vmovups   zmm8, ZMMWORD PTR [608+rsp]
        vmovups   zmm9, ZMMWORD PTR [544+rsp]
        vmovups   zmm10, ZMMWORD PTR [480+rsp]
        vmovups   zmm11, ZMMWORD PTR [416+rsp]
        vmovups   zmm12, ZMMWORD PTR [352+rsp]
        vmovups   zmm13, ZMMWORD PTR [288+rsp]
        vmovups   zmm14, ZMMWORD PTR [224+rsp]
        vmovups   zmm15, ZMMWORD PTR [160+rsp]
        vmovups   zmm16, ZMMWORD PTR [96+rsp]
        vmovups   zmm17, ZMMWORD PTR [32+rsp]
        vmovups   zmm1, ZMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [848+rsp]
        mov       edx, esi
        mov       rsi, QWORD PTR [840+rsp]
        mov       rdi, QWORD PTR [832+rsp]
        mov       rbp, QWORD PTR [856+rsp]
        jmp       _B9_7

_B9_15::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dtanh_ha_cout_rare_internal
        jmp       _B9_13
        ALIGN     16

_B9_16::

__svml_tanh8_ha_z0 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh8_ha_z0_B1_B9:
	DD	806913
	DD	8770640
	DD	5187650
	DD	4929591
	DD	4671532
	DD	4413473
	DD	10944779

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_1
	DD	imagerel _B9_11
	DD	imagerel _unwind___svml_tanh8_ha_z0_B1_B9

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanh8_ha_z0_B11_B15:
	DD	2674977
	DD	7034065
	DD	6845638
	DD	6907068
	DD	6960306
	DD	137386
	DD	395423
	DD	718996
	DD	977033
	DD	1235070
	DD	1493107
	DD	1751144
	DD	2009181
	DD	2267218
	DD	2525255
	DD	2783292
	DD	3041329
	DD	6585126
	DD	6646557
	DD	6707988
	DD	6769419
	DD	imagerel _B9_1
	DD	imagerel _B9_11
	DD	imagerel _unwind___svml_tanh8_ha_z0_B1_B9

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_11
	DD	imagerel _B9_16
	DD	imagerel _unwind___svml_tanh8_ha_z0_B11_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST9:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_dtanh_ha_cout_rare_internal

__svml_dtanh_ha_cout_rare_internal	PROC

_B10_1::

        DB        243
        DB        15
        DB        30
        DB        250
L282::

        mov       r10, rcx
        lea       r8, QWORD PTR [__ImageBase]
        mov       al, BYTE PTR [7+r10]
        and       al, -128
        shr       al, 7
        movzx     r9d, al
        movzx     eax, WORD PTR [6+r10]
        and       eax, 32752
        shr       eax, 4
        mov       rcx, QWORD PTR [imagerel(_imldTanhHATab)+r8+r9*8]
        cmp       eax, 2047
        je        _B10_6

_B10_2::

        cmp       eax, 2046
        jne       _B10_4

_B10_3::

        mov       QWORD PTR [rdx], rcx
        jmp       _B10_5

_B10_4::

        movsd     xmm1, QWORD PTR [r10]
        movsd     xmm0, QWORD PTR [_imldTanhHATab]
        addsd     xmm0, xmm1
        mulsd     xmm1, xmm0
        movsd     QWORD PTR [rdx], xmm1

_B10_5::

        xor       eax, eax
        ret

_B10_6::

        test      DWORD PTR [4+r10], 1048575
        jne       _B10_9

_B10_7::

        cmp       DWORD PTR [r10], 0
        je        _B10_3

_B10_9::

        movsd     xmm0, QWORD PTR [r10]
        addsd     xmm0, xmm0
        movsd     QWORD PTR [rdx], xmm0
        jmp       _B10_5
        ALIGN     16

_B10_10::

__svml_dtanh_ha_cout_rare_internal ENDP

_TEXT	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_dtanh_ha_data_internal
__svml_dtanh_ha_data_internal	DD	0
	DD	0
	DD	0
	DD	1070333952
	DD	0
	DD	1070858240
	DD	0
	DD	1071382528
	DD	0
	DD	1071906816
	DD	0
	DD	1072431104
	DD	0
	DD	1072955392
	DD	0
	DD	1073479680
	DD	0
	DD	1074003968
	DD	0
	DD	1074528256
	DD	0
	DD	1075052544
	DD	0
	DD	1075576832
	DD	0
	DD	1076101120
	DD	0
	DD	1076625408
	DD	0
	DD	1077149696
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1097497746
	DD	1070305232
	DD	2699715854
	DD	1070817176
	DD	3997728823
	DD	1071273769
	DD	3938160533
	DD	1071759175
	DD	193732629
	DD	1072072293
	DD	3144363502
	DD	1072375075
	DD	2548249895
	DD	1072570303
	DD	638218690
	DD	1072665176
	DD	3325726839
	DD	1072689426
	DD	2523455249
	DD	1072693057
	DD	2200391922
	DD	1072693244
	DD	4257836853
	DD	1072693247
	DD	4294954840
	DD	1072693247
	DD	0
	DD	1072693248
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	3167993022
	DD	1013310014
	DD	2889539328
	DD	3159121931
	DD	394645568
	DD	1013728265
	DD	2799920971
	DD	1015161303
	DD	754366556
	DD	1013577885
	DD	3960180508
	DD	1014215657
	DD	69774042
	DD	3162475851
	DD	2161066944
	DD	3162264280
	DD	923362432
	DD	1014879086
	DD	2160345802
	DD	1014714211
	DD	3729195158
	DD	3161626453
	DD	675671997
	DD	1015336837
	DD	1141527416
	DD	1013957883
	DD	2209699952
	DD	3160643600
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	2750448946
	DD	1072596012
	DD	1257606939
	DD	1072501079
	DD	2619663609
	DD	1072338011
	DD	863303701
	DD	1072048204
	DD	2694810990
	DD	1071654144
	DD	837003456
	DD	1070723665
	DD	472162617
	DD	1069359818
	DD	532307062
	DD	1067137790
	DD	431106849
	DD	1064162173
	DD	329904022
	DD	1059572972
	DD	4080160942
	DD	1053550275
	DD	1402271583
	DD	1044493446
	DD	467296040
	DD	1032344560
	DD	26734424
	DD	1014207804
	DD	0
	DD	0
	DD	1071622681
	DD	3153114090
	DD	3948133409
	DD	3217705130
	DD	4096956694
	DD	3218184481
	DD	2616164369
	DD	3218465008
	DD	2620770434
	DD	3218641845
	DD	3075958072
	DD	3218520700
	DD	1950184789
	DD	3217978001
	DD	2686385024
	DD	3216731525
	DD	1489441818
	DD	3214597550
	DD	1992796697
	DD	3211642259
	DD	1952936976
	DD	3207056478
	DD	3904162951
	DD	3201033920
	DD	1382745445
	DD	3191977094
	DD	468673444
	DD	3179828208
	DD	3843669553
	DD	3161691483
	DD	0
	DD	0
	DD	1431655765
	DD	3218429269
	DD	3264395793
	DD	3218178991
	DD	60595194
	DD	3217824331
	DD	1837960166
	DD	3217124894
	DD	1964918946
	DD	3214033516
	DD	2998587684
	DD	1068822866
	DD	3421467326
	DD	1069267520
	DD	952003133
	DD	1068506016
	DD	1215426504
	DD	1066495476
	DD	2650572912
	DD	1063503494
	DD	4178958376
	DD	1059043758
	DD	3799805457
	DD	1052940753
	DD	3704238924
	DD	1043831645
	DD	2076585114
	DD	1031813109
	DD	1352847351
	DD	1013591371
	DD	0
	DD	0
	DD	3830371894
	DD	3169224254
	DD	74893150
	DD	1069567437
	DD	1235750664
	DD	1069825332
	DD	916043751
	DD	1069924975
	DD	4138411315
	DD	1069655686
	DD	829220656
	DD	1068441660
	DD	663246204
	DD	3213477792
	DD	652268865
	DD	3214415230
	DD	3251098232
	DD	3212868437
	DD	1480792335
	DD	3209929101
	DD	3365608027
	DD	3205478075
	DD	3330582883
	DD	3199375817
	DD	3573234945
	DD	3190266717
	DD	2061970086
	DD	3178248181
	DD	3148320390
	DD	3160026274
	DD	0
	DD	0
	DD	286337717
	DD	1069617425
	DD	4261152941
	DD	1068876190
	DD	3694459820
	DD	1067928728
	DD	2399079031
	DD	3212381546
	DD	394897286
	DD	3215735810
	DD	3073534041
	DD	3215860118
	DD	301960234
	DD	3214124960
	DD	3353887502
	DD	1064191753
	DD	4200665425
	DD	1063741482
	DD	2003926207
	DD	1061090030
	DD	2196865207
	DD	1056528964
	DD	2107880963
	DD	1050526402
	DD	3656860478
	DD	1041425071
	DD	881228218
	DD	1029305120
	DD	2612840768
	DD	1011136029
	DD	0
	DD	0
	DD	3722294196
	DD	3181505049
	DD	2573001951
	DD	3216029919
	DD	4095639908
	DD	3216151900
	DD	3272210374
	DD	3215957253
	DD	3439280785
	DD	3214441767
	DD	628273304
	DD	1066085542
	DD	3354767370
	DD	1066343670
	DD	300170709
	DD	1063270296
	DD	1870156670
	DD	3209139074
	DD	1580606479
	DD	3206808937
	DD	3818710870
	DD	3202408589
	DD	3126817102
	DD	3196311305
	DD	3320734688
	DD	3187224127
	DD	821170446
	DD	3175173312
	DD	3906480775
	DD	3156975650
	DD	0
	DD	0
	DD	428888587
	DD	3215696314
	DD	3125999356
	DD	3214336891
	DD	407196569
	DD	1053816799
	DD	886258254
	DD	1066874408
	DD	2149075781
	DD	1067351939
	DD	3888390356
	DD	1065784643
	DD	1226056234
	DD	3211994813
	DD	178100474
	DD	3210723675
	DD	4201249718
	DD	1058328572
	DD	3854015760
	DD	1057324616
	DD	3621220964
	DD	1052982118
	DD	1468766992
	DD	1046916174
	DD	2369608770
	DD	1037879115
	DD	327127732
	DD	1025754505
	DD	747046817
	DD	1007714190
	DD	0
	DD	0
	DD	4036362527
	DD	3191151783
	DD	839661649
	DD	1067363059
	DD	4269154241
	DD	1067251747
	DD	391446303
	DD	1066410535
	DD	2090623151
	DD	3211993063
	DD	1751510141
	DD	3213141508
	DD	4072216875
	DD	3209470961
	DD	3769618983
	DD	1061976030
	DD	3462945146
	DD	1057228123
	DD	3030849095
	DD	3202531084
	DD	4269010901
	DD	3198361258
	DD	742615277
	DD	3192302512
	DD	3397417437
	DD	3183265609
	DD	943110610
	DD	3171141000
	DD	371608300
	DD	3153099348
	DD	0
	DD	0
	DD	1315619150
	DD	1066820857
	DD	1001273821
	DD	3214201652
	DD	3859675203
	DD	3212560200
	DD	725858949
	DD	3213658423
	DD	2464052346
	DD	3212913056
	DD	1297319750
	DD	1063307355
	DD	563735576
	DD	1062988089
	DD	2756222736
	DD	3207203944
	DD	31207338
	DD	3204466214
	DD	434022900
	DD	1052227234
	DD	2370591882
	DD	1048628172
	DD	588930601
	DD	1042556347
	DD	3198977634
	DD	1033474724
	DD	1590950759
	DD	1021415866
	DD	195904708
	DD	1003000389
	DD	0
	DD	0
	DD	4252521214
	DD	3198731457
	DD	2969857811
	DD	3217870358
	DD	575387574
	DD	3212943727
	DD	67550217
	DD	3210922992
	DD	2955736731
	DD	1064678043
	DD	4193848343
	DD	1063288304
	DD	1845975253
	DD	3209397546
	DD	1025213509
	DD	3204479174
	DD	3713384058
	DD	1055658730
	DD	4120057883
	DD	3194988032
	DD	1573797757
	DD	3193584787
	DD	2514726550
	DD	3187678317
	DD	15343571
	DD	3178526042
	DD	104576940
	DD	3166444652
	DD	1147207168
	DD	3148070554
	DD	0
	DD	0
	DD	1467656669
	DD	3212977156
	DD	4154993315
	DD	1077775111
	DD	2465966858
	DD	3214886059
	DD	71777642
	DD	1068690118
	DD	2419763912
	DD	1061550205
	DD	1896047360
	DD	3210612806
	DD	3723555648
	DD	1058651288
	DD	3163703016
	DD	1057833732
	DD	1601936705
	DD	3201383489
	DD	2535509424
	DD	3195153293
	DD	3610885824
	DD	1043474022
	DD	1031698712
	DD	1037527637
	DD	1497459257
	DD	1028514042
	DD	3476455860
	DD	1016366870
	DD	758110873
	DD	998719391
	DD	0
	DD	0
	DD	572446067
	DD	3204307354
	DD	926268084
	DD	1081104698
	DD	1313112926
	DD	3217861477
	DD	3660716
	DD	1070677720
	DD	124568711
	DD	3210757561
	DD	2123022704
	DD	1059096046
	DD	576783408
	DD	1059279430
	DD	1651052980
	DD	3204387494
	DD	3164866735
	DD	1051430920
	DD	409335328
	DD	1046695415
	DD	3481520755
	DD	3188046619
	DD	1140549474
	DD	3182373569
	DD	708689751
	DD	3173247717
	DD	2627769694
	DD	3161153086
	DD	3804346926
	DD	3143551592
	DD	0
	DD	0
	DD	2302818369
	DD	1064188902
	DD	526101185
	DD	3235013457
	DD	2975776348
	DD	1075224435
	DD	1103981749
	DD	3223699933
	DD	4261798097
	DD	3210280329
	DD	30781306
	DD	1064564655
	DD	3939597931
	DD	3206430909
	DD	1816466405
	DD	1055007949
	DD	3868125859
	DD	3190076997
	DD	4218600579
	DD	3192569835
	DD	4167655123
	DD	1037376568
	DD	952533803
	DD	1032000428
	DD	895641221
	DD	1022851193
	DD	1237761065
	DD	1010835452
	DD	2902086315
	DD	3133082401
	DD	0
	DD	0
	DD	1899646778
	DD	3207205638
	DD	2434183270
	DD	3238288976
	DD	621380814
	DD	1078065849
	DD	247717525
	DD	3225783561
	DD	1611742563
	DD	3212088477
	DD	537725662
	DD	1065131990
	DD	3769436831
	DD	1057148224
	DD	3759797009
	DD	3196422840
	DD	842759416
	DD	3195613094
	DD	1736926210
	DD	1043198029
	DD	3915271468
	DD	3180709675
	DD	807416070
	DD	3176507548
	DD	3147759461
	DD	3167409843
	DD	3443382404
	DD	3155325020
	DD	1202615797
	DD	3129870924
	DD	0
	DD	0
	DD	1841653873
	DD	3210074087
	DD	2157744327
	DD	1095928888
	DD	3038317314
	DD	3229013375
	DD	2291108570
	DD	1082519711
	DD	707775397
	DD	1067599411
	DD	445214669
	DD	3216153989
	DD	3815354898
	DD	1054410330
	DD	1285070896
	DD	3199787450
	DD	1722630166
	DD	1047526663
	DD	2672844635
	DD	3188483010
	DD	1805520457
	DD	3179260705
	DD	542550567
	DD	1026041526
	DD	392361251
	DD	1017320419
	DD	562647833
	DD	1005205418
	DD	4253488278
	DD	988137457
	DD	0
	DD	0
	DD	3077187303
	DD	1060497018
	DD	1652392454
	DD	1099206368
	DD	414484972
	DD	3231848150
	DD	130592591
	DD	1084602513
	DD	1951534810
	DD	1068932483
	DD	2677544726
	DD	3216895313
	DD	2338009969
	DD	3203411240
	DD	302629286
	DD	1051889816
	DD	1274412910
	DD	3193339538
	DD	3731558070
	DD	1038350327
	DD	3135499196
	DD	1030352152
	DD	1329461873
	DD	3170325324
	DD	4058709792
	DD	3161656179
	DD	2167788642
	DD	3149540607
	DD	1602064437
	DD	3132466971
	DD	0
	DD	0
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	2146959360
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	1069547520
	DD	7864320
	DD	7864320
	DD	7864320
	DD	7864320
	DD	7864320
	DD	7864320
	DD	7864320
	DD	7864320
	DD	7864320
	DD	7864320
	DD	7864320
	DD	7864320
	DD	7864320
	DD	7864320
	DD	7864320
	DD	7864320
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	3616958675
	DD	3172564458
	DD	1431547708
	DD	3218429269
	DD	3390261318
	DD	3188010876
	DD	1446529494
	DD	1069617425
	DD	913571762
	DD	3199219810
	DD	1583612462
	DD	3215695720
	DD	2995724807
	DD	3207222498
	DD	847913742
	DD	1066913721
	DD	1634876930
	DD	3212167789
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1694159514
	DD	1010239653
	DD	966318664
	DD	1069606551
	DD	2992375944
	DD	1072656686
	DD	1870405289
	DD	3217070889
	DD	1411308967
	DD	3218333047
	DD	4096824853
	DD	1068863484
	DD	2220740425
	DD	1069365950
	DD	1401698298
	DD	3215430111
	DD	4137473768
	DD	3215259762
	DD	2666938667
	DD	1066889956
	DD	121190665
	DD	1066187784
	DD	821637913
	DD	3213226090
	DD	0
	DD	3217096704
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1463410348
	DD	1012925678
	DD	2389577058
	DD	1069735062
	DD	1617794783
	DD	1072647710
	DD	1889094329
	DD	3217191869
	DD	1210518828
	DD	3218309813
	DD	1479174953
	DD	1069010221
	DD	3435917531
	DD	1069290104
	DD	291210913
	DD	3215575029
	DD	464478606
	DD	3215159746
	DD	1063797118
	DD	1067014292
	DD	3489481042
	DD	1065955541
	DD	2916293494
	DD	3213319415
	DD	0
	DD	3217227776
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1308961311
	DD	1014934498
	DD	2204208241
	DD	1069862983
	DD	2945950899
	DD	1072637797
	DD	1107689125
	DD	3217310565
	DD	545938327
	DD	3218284334
	DD	3174275192
	DD	1069150773
	DD	3754729793
	DD	1069207728
	DD	1611554958
	DD	3215708601
	DD	2936527704
	DD	3215052478
	DD	2983784402
	DD	1067121823
	DD	1327150338
	DD	1065710404
	DD	3371320326
	DD	3213391099
	DD	0
	DD	3217358848
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3546392464
	DD	1015001497
	DD	631120375
	DD	1069990256
	DD	1201634405
	DD	1072626967
	DD	266657677
	DD	3217426771
	DD	1567732958
	DD	3218256710
	DD	883708059
	DD	1069284653
	DD	1008115966
	DD	1069119372
	DD	2657338981
	DD	3215830093
	DD	3402640736
	DD	3214939036
	DD	1000796573
	DD	1067211764
	DD	53805889
	DD	1065455799
	DD	1736607114
	DD	3213440608
	DD	0
	DD	3217489920
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2741128528
	DD	1013617020
	DD	3111451323
	DD	1070116823
	DD	1649040643
	DD	1072615239
	DD	3411009101
	DD	3217540290
	DD	3408666525
	DD	3218227049
	DD	60831764
	DD	1069411415
	DD	64016149
	DD	1069025616
	DD	1202785467
	DD	3215938891
	DD	1072151579
	DD	3214707060
	DD	1534357116
	DD	1067283570
	DD	4218468492
	DD	1065037194
	DD	2285827787
	DD	3213467810
	DD	0
	DD	3217620992
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2659584459
	DD	1014368295
	DD	3537749475
	DD	1070242630
	DD	3072983457
	DD	1072602635
	DD	3507245872
	DD	3217650938
	DD	3434758212
	DD	3218195466
	DD	3801643091
	DD	1069530660
	DD	1128653951
	DD	1068927067
	DD	3580298628
	DD	3216008547
	DD	1645082338
	DD	3214462237
	DD	1048857889
	DD	1067336943
	DD	21547694
	DD	1064510970
	DD	1433152914
	DD	3213472968
	DD	0
	DD	3217752064
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2718912981
	DD	1013914074
	DD	1823051285
	DD	1070367623
	DD	1714227149
	DD	1072589179
	DD	2128046799
	DD	3217758540
	DD	2655098722
	DD	3218162081
	DD	1690074008
	DD	1069594780
	DD	353091525
	DD	1068824353
	DD	4206393496
	DD	3216049578
	DD	824478721
	DD	3214211899
	DD	3850924188
	DD	1067371825
	DD	2738209029
	DD	1063668369
	DD	853664366
	DD	3213456718
	DD	0
	DD	3217883136
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2312638749
	DD	1013630664
	DD	4267025360
	DD	1070491748
	DD	3776362539
	DD	1072574894
	DD	3063840907
	DD	3217862932
	DD	2436606365
	DD	3218127019
	DD	582931594
	DD	1069646387
	DD	3079837843
	DD	1068718114
	DD	3430470362
	DD	3216083715
	DD	1015897693
	DD	3213958348
	DD	765047087
	DD	1067388396
	DD	2337193368
	DD	1061824569
	DD	3002775972
	DD	3213420044
	DD	0
	DD	3218014208
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2001712669
	DD	1015665334
	DD	1330879460
	DD	1070636148
	DD	2956987714
	DD	1072551971
	DD	4281360332
	DD	3218013175
	DD	3304213057
	DD	3218063389
	DD	3261945160
	DD	1069715874
	DD	3866284424
	DD	1068553570
	DD	3423706630
	DD	3216121886
	DD	259493169
	DD	3213268437
	DD	4223676832
	DD	1067379852
	DD	2765317642
	DD	3210752240
	DD	2292494069
	DD	3213329490
	DD	0
	DD	3218145280
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1632443533
	DD	1015769771
	DD	3788472163
	DD	1070757367
	DD	2406795724
	DD	1072518757
	DD	1173083542
	DD	3218140352
	DD	3726086528
	DD	3217906251
	DD	1205028711
	DD	1069793280
	DD	2231197855
	DD	1068156878
	DD	2368637763
	DD	3216148628
	DD	2866127296
	DD	3211617797
	DD	2424606359
	DD	1067309831
	DD	2444940724
	DD	3212180962
	DD	3308128888
	DD	3213151909
	DD	0
	DD	3218276352
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	478834383
	DD	1014053288
	DD	1342399495
	DD	1070876422
	DD	2650660168
	DD	1072482726
	DD	976724127
	DD	3218226669
	DD	962417089
	DD	3217740546
	DD	1060150306
	DD	1069852926
	DD	411739190
	DD	1067700577
	DD	3846786712
	DD	3216148687
	DD	4007187252
	DD	1064073475
	DD	3455779574
	DD	1067180067
	DD	1865169557
	DD	3212900393
	DD	1200620699
	DD	3212923615
	DD	0
	DD	3218407424
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	267289397
	DD	1014452734
	DD	302318249
	DD	1070993143
	DD	1373990511
	DD	1072444121
	DD	1606419704
	DD	3218305061
	DD	3955669825
	DD	3217568496
	DD	2701083439
	DD	1069894809
	DD	3425188888
	DD	1067047616
	DD	2305426029
	DD	3216123827
	DD	1692531481
	DD	1065641523
	DD	232815703
	DD	1067000535
	DD	3949954748
	DD	3213214884
	DD	558890519
	DD	3212487521
	DD	0
	DD	3218538496
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1719941932
	DD	1006528498
	DD	368840470
	DD	1071107377
	DD	1825320027
	DD	1072403193
	DD	538136722
	DD	3218375283
	DD	1431312010
	DD	3217392305
	DD	2586725425
	DD	1069919291
	DD	2680871675
	DD	1065941593
	DD	4123661982
	DD	3216076488
	DD	4235496382
	DD	1066406926
	DD	2618960092
	DD	1066782660
	DD	2333865044
	DD	3213444845
	DD	1545458959
	DD	3211934181
	DD	0
	DD	3218669568
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	26286236
	DD	1013191219
	DD	990113957
	DD	1071218987
	DD	3284199501
	DD	1072360200
	DD	2981906127
	DD	3218437190
	DD	3154396333
	DD	3217214106
	DD	4182117656
	DD	1069927061
	DD	903677379
	DD	3207343530
	DD	384743261
	DD	3216009637
	DD	1679228359
	DD	1066734193
	DD	3407026595
	DD	1066538544
	DD	784962854
	DD	3213588186
	DD	1956733412
	DD	3210979700
	DD	0
	DD	3218800640
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1468217372
	DD	1015658399
	DD	2570814109
	DD	1071327852
	DD	532296332
	DD	1072315404
	DD	318213600
	DD	3218490738
	DD	3661105766
	DD	3217035931
	DD	4085840862
	DD	1069919095
	DD	2798312316
	DD	3213370099
	DD	1668326589
	DD	3215870599
	DD	3158013712
	DD	1066998409
	DD	673205579
	DD	1066158659
	DD	486665227
	DD	3213647762
	DD	3317145528
	DD	3208570948
	DD	0
	DD	3218931712
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2060955015
	DD	1014233667
	DD	2567098181
	DD	1071433868
	DD	1159081245
	DD	1072269064
	DD	1492598184
	DD	3218535971
	DD	1567055841
	DD	3216688180
	DD	2821222425
	DD	1069896605
	DD	3691290783
	DD	3214336992
	DD	343679101
	DD	3215679175
	DD	1878686296
	DD	1067197462
	DD	125933636
	DD	1065636281
	DD	421076939
	DD	3213630573
	DD	3748848474
	DD	1062499186
	DD	0
	DD	3219062784
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1860475216
	DD	1015320544
	DD	439764829
	DD	1071587361
	DD	3870821058
	DD	1072197223
	DD	1430736283
	DD	3218588540
	DD	1597812790
	DD	3216174065
	DD	2398544810
	DD	1069838732
	DD	961452807
	DD	3215095800
	DD	716310499
	DD	3215360049
	DD	2337792646
	DD	1067375770
	DD	3863538422
	DD	1064417477
	DD	2203480844
	DD	3213482785
	DD	2389621902
	DD	1063978354
	DD	0
	DD	3219193856
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	517838046
	DD	1015294339
	DD	2114713104
	DD	1071713012
	DD	2046328558
	DD	1072098392
	DD	29352448
	DD	3218631376
	DD	1533416325
	DD	3215079684
	DD	765247815
	DD	1069724759
	DD	24381189
	DD	3215564623
	DD	1213155449
	DD	3214886044
	DD	379420126
	DD	1067409218
	DD	3127061143
	DD	3210809777
	DD	3193663073
	DD	3213128287
	DD	2557278876
	DD	1064581282
	DD	0
	DD	3219324928
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	201012485
	DD	1013704982
	DD	4147262407
	DD	1071803766
	DD	3048814903
	DD	1071997795
	DD	1260857726
	DD	3218645540
	DD	270462819
	DD	3209873967
	DD	874660781
	DD	1069580732
	DD	1251156804
	DD	3215866075
	DD	3568210118
	DD	3214014484
	DD	3784557811
	DD	1067255146
	DD	47772576
	DD	3212562613
	DD	2075700783
	DD	3212539455
	DD	1121561449
	DD	1064698735
	DD	0
	DD	3219456000
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	72370286
	DD	1015768239
	DD	2345366573
	DD	1071888223
	DD	3155310239
	DD	1071897123
	DD	4105462806
	DD	3218634383
	DD	2329529114
	DD	1067280331
	DD	3078782452
	DD	1069291148
	DD	2210998062
	DD	3215997483
	DD	1498585052
	DD	3212353515
	DD	3032692199
	DD	1066974465
	DD	809329973
	DD	3213081308
	DD	2713838579
	DD	3211547879
	DD	1266611175
	DD	1064568889
	DD	0
	DD	3219587072
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2137918782
	DD	1013598293
	DD	2511343836
	DD	1071966424
	DD	4205808243
	DD	1071797842
	DD	2776384587
	DD	3218601667
	DD	3824787134
	DD	1068202086
	DD	4101819712
	DD	1068956189
	DD	3547601806
	DD	3216004360
	DD	4156237724
	DD	1064058621
	DD	3714924071
	DD	1066627770
	DD	2925917146
	DD	3213234133
	DD	4211598888
	DD	3209111151
	DD	2569808389
	DD	1064277859
	DD	0
	DD	3219718144
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2979324843
	DD	1013692066
	DD	2595126306
	DD	1072038496
	DD	4263058559
	DD	1071701178
	DD	2217257467
	DD	3218551298
	DD	2310932059
	DD	1068685603
	DD	3368327571
	DD	1068627625
	DD	3037419246
	DD	3215935424
	DD	3509936675
	DD	1065724141
	DD	1557247226
	DD	1066131548
	DD	3830787958
	DD	3213205743
	DD	1781883284
	DD	1062575914
	DD	3918078093
	DD	1063614197
	DD	0
	DD	3219849216
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3630992244
	DD	1015034507
	DD	461360001
	DD	1072104635
	DD	2866201612
	DD	1071571556
	DD	1021729265
	DD	3218487113
	DD	1039036234
	DD	1068940858
	DD	1685105679
	DD	1068140011
	DD	1856275853
	DD	3215769620
	DD	2211306181
	DD	1066373046
	DD	3739405201
	DD	1065456917
	DD	3870269089
	DD	3213053509
	DD	427599213
	DD	1063593231
	DD	40698732
	DD	1062709094
	DD	0
	DD	3219980288
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	4017840557
	DD	1014067070
	DD	2764932206
	DD	1072165089
	DD	3362970633
	DD	1071394124
	DD	2677206355
	DD	3218412713
	DD	1827861303
	DD	1069122666
	DD	2476388705
	DD	1067583638
	DD	523365901
	DD	3215556224
	DD	1203249285
	DD	1066574111
	DD	4264074292
	DD	1064402288
	DD	3556167213
	DD	3212827889
	DD	3894081206
	DD	1063908871
	DD	2161178761
	DD	1061130844
	DD	0
	DD	3220111360
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3622330478
	DD	1016273425
	DD	923638641
	DD	1072245755
	DD	239419665
	DD	1071146518
	DD	4085259706
	DD	3218288969
	DD	1284806809
	DD	1069276013
	DD	2806747971
	DD	1066232498
	DD	75259250
	DD	3215197393
	DD	2597116185
	DD	1066648701
	DD	1680670491
	DD	3208755029
	DD	446818184
	DD	3212096816
	DD	1529495144
	DD	1063888972
	DD	808619025
	DD	3208443000
	DD	0
	DD	3220242432
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	462410646
	DD	1015565639
	DD	3157363066
	DD	1072336316
	DD	87541994
	DD	1070853747
	DD	2905067058
	DD	3218115077
	DD	1081050294
	DD	1069306453
	DD	4130581086
	DD	3212259234
	DD	1279737796
	DD	3214531982
	DD	901138781
	DD	1066501065
	DD	978916480
	DD	3211806490
	DD	1307294116
	DD	3210755549
	DD	1154728319
	DD	1063409950
	DD	983243444
	DD	3209435485
	DD	0
	DD	3220373504
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2617212772
	DD	1016126748
	DD	2762378177
	DD	1072409936
	DD	1926160805
	DD	1070604218
	DD	4131898582
	DD	3217810482
	DD	3068505203
	DD	1069203346
	DD	2854543895
	DD	3214027139
	DD	1276437050
	DD	3213652513
	DD	523800203
	DD	1066060621
	DD	3030576699
	DD	3212054264
	DD	210618624
	DD	3205409267
	DD	3515290542
	DD	1062456384
	DD	1613351841
	DD	3209185464
	DD	0
	DD	3220504576
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2178033281
	DD	1016051223
	DD	859883711
	DD	1072469258
	DD	4248327203
	DD	1070195167
	DD	4170103331
	DD	3217497647
	DD	3497702842
	DD	1069026027
	DD	669705965
	DD	3214426190
	DD	548733038
	DD	3212258725
	DD	1756337187
	DD	1065503890
	DD	1830841059
	DD	3211930343
	DD	1445563742
	DD	1061912703
	DD	2113494314
	DD	1060991234
	DD	1734825467
	DD	3208559895
	DD	0
	DD	3220635648
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1674478116
	DD	1016412476
	DD	1178764976
	DD	1072516719
	DD	1119346206
	DD	1069851736
	DD	1526584272
	DD	3217221512
	DD	3575463915
	DD	1068817773
	DD	2913683612
	DD	3214542291
	DD	1135909212
	DD	3207879094
	DD	1952394810
	DD	1064725296
	DD	508910559
	DD	3211537545
	DD	225204077
	DD	1062311155
	DD	1009857186
	DD	1056234420
	DD	2872841632
	DD	3207480811
	DD	0
	DD	3220766720
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1768234410
	DD	1014305046
	DD	1496797583
	DD	1072554475
	DD	3351833521
	DD	1069572393
	DD	68183265
	DD	3216938851
	DD	4178655528
	DD	1068606905
	DD	60791550
	DD	3214483781
	DD	1856281737
	DD	1063701265
	DD	4260560897
	DD	1063778674
	DD	2539586291
	DD	3210979253
	DD	2272785608
	DD	1062198907
	DD	1986161572
	DD	3206910344
	DD	1016667904
	DD	3205797138
	DD	0
	DD	3220897792
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3074583847
	DD	1017063845
	DD	4092570620
	DD	1072584374
	DD	3645618684
	DD	1069147119
	DD	1980755111
	DD	3216542681
	DD	889928399
	DD	1068320928
	DD	1360064809
	DD	3214330986
	DD	2266432388
	DD	1064407878
	DD	4147854841
	DD	1062471610
	DD	1812350685
	DD	3210287970
	DD	3710399832
	DD	1061728481
	DD	2458127659
	DD	3207279138
	DD	287360833
	DD	3197756422
	DD	0
	DD	3221028864
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	68970445
	DD	1013643458
	DD	3990219923
	DD	1072607967
	DD	97498680
	DD	1068787106
	DD	4136450559
	DD	3216216395
	DD	147179316
	DD	1067971098
	DD	1625987424
	DD	3214138005
	DD	3965878798
	DD	1064539455
	DD	3777445436
	DD	1059539413
	DD	3029913178
	DD	3209512624
	DD	2162291908
	DD	1061245910
	DD	351053474
	DD	3207087984
	DD	3766283083
	DD	1056943188
	DD	0
	DD	3221159936
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3139580402
	DD	1014663922
	DD	3748810696
	DD	1072634260
	DD	3154943320
	DD	1068262833
	DD	3181856712
	DD	3215694135
	DD	3656356636
	DD	1067539266
	DD	3897588284
	DD	3213798616
	DD	1461831298
	DD	1064461217
	DD	2900114226
	DD	3208814642
	DD	2606420357
	DD	3207868903
	DD	1741152094
	DD	1060222230
	DD	1469206701
	DD	3206514441
	DD	3518993813
	DD	1057090958
	DD	0
	DD	3221291008
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3285908565
	DD	1015477239
	DD	1797956315
	DD	1072657271
	DD	3302471936
	DD	1067543167
	DD	2151339553
	DD	3215007235
	DD	362228095
	DD	1066797401
	DD	3986406156
	DD	3213131380
	DD	388353381
	DD	1064042359
	DD	4147910906
	DD	3209239839
	DD	1739922885
	DD	1056259812
	DD	3188561056
	DD	1058406709
	DD	489122368
	DD	3205182155
	DD	202560853
	DD	1056234231
	DD	0
	DD	3221422080
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3806705628
	DD	1008327668
	DD	884432573
	DD	1072671353
	DD	137405484
	DD	1066747168
	DD	3531994812
	DD	3214216262
	DD	3217445183
	DD	1066105333
	DD	2910288024
	DD	3212464301
	DD	3196212707
	DD	1063467545
	DD	3156563895
	DD	3208963593
	DD	3591285453
	DD	1058733242
	DD	2889132271
	DD	1055392886
	DD	1038377961
	DD	3203561698
	DD	4084672077
	DD	1055001082
	DD	0
	DD	3221553152
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1881957148
	DD	1016569186
	DD	3451706443
	DD	1072679940
	DD	143468186
	DD	1066002557
	DD	553724800
	DD	3213475431
	DD	1049442771
	DD	1065415105
	DD	3378521943
	DD	3211821787
	DD	3176008209
	DD	1062800361
	DD	4016898691
	DD	3208498219
	DD	1548390021
	DD	1058670598
	DD	2097418483
	DD	3202689041
	DD	2756703589
	DD	3201351283
	DD	506736184
	DD	1053405377
	DD	0
	DD	3221684224
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	450339788
	DD	1015040915
	DD	2959639058
	DD	1072685166
	DD	2289443405
	DD	1065320893
	DD	3331959139
	DD	3212796584
	DD	724199976
	DD	1064616734
	DD	938566183
	DD	3211030741
	DD	1640535667
	DD	1062186735
	DD	187996035
	DD	3207841256
	DD	822311531
	DD	1058246461
	DD	160890851
	DD	3203087480
	DD	3163291388
	DD	1050479733
	DD	578249940
	DD	1051474021
	DD	0
	DD	3221815296
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	539445753
	DD	1012635531
	DD	3085578536
	DD	1072688342
	DD	2019637246
	DD	1064510347
	DD	2901018414
	DD	3211991061
	DD	2171427566
	DD	1063868144
	DD	678185093
	DD	3210287638
	DD	2685165718
	DD	1061401571
	DD	710336199
	DD	3207152667
	DD	2733135798
	DD	1057659331
	DD	886948177
	DD	3202751664
	DD	3345834247
	DD	1052218043
	DD	908728048
	DD	1047925874
	DD	0
	DD	3221946368
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	4240302093
	DD	1016867082
	DD	1832260410
	DD	1072690271
	DD	154153694
	DD	1063730412
	DD	2094548181
	DD	3211211898
	DD	1392727515
	DD	1063180837
	DD	3132890025
	DD	3209604411
	DD	483611698
	DD	1060651750
	DD	4246355421
	DD	3206519479
	DD	1424637421
	DD	1057044161
	DD	2138185318
	DD	3202290304
	DD	2276282642
	DD	1052095798
	DD	4227780935
	DD	3196067472
	DD	0
	DD	3222077440
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2485733495
	DD	1017000498
	DD	484092514
	DD	1072691442
	DD	1653085170
	DD	1063007344
	DD	2893019346
	DD	3210489400
	DD	878866243
	DD	1062388018
	DD	2113174452
	DD	3208818852
	DD	2654141437
	DD	1059959432
	DD	3578550869
	DD	3205727739
	DD	315005006
	DD	1056288680
	DD	3246956604
	DD	3201593495
	DD	2197286540
	DD	1051718329
	DD	3044885069
	DD	3196227269
	DD	0
	DD	3222208512
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2794994668
	DD	1016833037
	DD	3287420517
	DD	1072692394
	DD	4133778764
	DD	1061857404
	DD	689562148
	DD	3209340342
	DD	1404089106
	DD	1061273627
	DD	1292441425
	DD	3207706805
	DD	93671116
	DD	1058816787
	DD	2903327974
	DD	3204626398
	DD	4279279273
	DD	1055202414
	DD	134688023
	DD	3200552187
	DD	3315379764
	DD	1050761310
	DD	2945780649
	DD	3195568939
	DD	0
	DD	3222339584
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2135621136
	DD	1016053539
	DD	309961636
	DD	1072692934
	DD	672792810
	DD	1060347512
	DD	2960305506
	DD	3207830967
	DD	1703867620
	DD	1059726750
	DD	824905914
	DD	3206160796
	DD	3036017847
	DD	1057284422
	DD	923304464
	DD	3203122673
	DD	1848642304
	DD	1053791859
	DD	2215350763
	DD	3199158388
	DD	3049848127
	DD	1049324210
	DD	3861893815
	DD	3194293196
	DD	0
	DD	3222470656
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2236028675
	DD	1016891036
	DD	2177293363
	DD	1072693132
	DD	776830057
	DD	1058856794
	DD	4190004158
	DD	3206340337
	DD	209955488
	DD	1058225857
	DD	845130443
	DD	3204660651
	DD	4204313304
	DD	1055835544
	DD	364525198
	DD	3201597210
	DD	3889299905
	DD	1052205563
	DD	1514389355
	DD	3197586647
	DD	1706817756
	DD	1047834665
	DD	3817417318
	DD	3192934132
	DD	0
	DD	3222601728
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1074033072
	DD	1013200912
	DD	2197899301
	DD	1072693205
	DD	1097614282
	DD	1057308273
	DD	209489097
	DD	3204791893
	DD	3641526339
	DD	1056723664
	DD	1792794946
	DD	3203158586
	DD	584598707
	DD	1054254910
	DD	253996240
	DD	3200135633
	DD	642640562
	DD	1050754580
	DD	3147361740
	DD	3196139610
	DD	1167319222
	DD	1046395158
	DD	3488053038
	DD	3191370264
	DD	0
	DD	3222732800
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	871679747
	DD	1016222468
	DD	1586311569
	DD	1072693232
	DD	4199724405
	DD	1055867613
	DD	3041006250
	DD	3203351246
	DD	482130003
	DD	1055184672
	DD	1689676855
	DD	3201619703
	DD	116121201
	DD	1052814264
	DD	4166318198
	DD	3198564764
	DD	388552649
	DD	1049191609
	DD	1384400086
	DD	3194577312
	DD	135589376
	DD	1044819515
	DD	2497367318
	DD	3189906305
	DD	0
	DD	3222863872
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	752986014
	DD	1014740322
	DD	1072834312
	DD	1072693242
	DD	1389704451
	DD	1054277685
	DD	817998738
	DD	3201761329
	DD	996777029
	DD	1053731553
	DD	811547911
	DD	3200166603
	DD	1604093935
	DD	1051232383
	DD	2381858127
	DD	3197131472
	DD	806055999
	DD	1047703656
	DD	443662424
	DD	3193089938
	DD	2855612429
	DD	1043379518
	DD	3671581230
	DD	3188373004
	DD	0
	DD	3222994944
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	4161630806
	DD	1015796140
	DD	3799386689
	DD	1072693245
	DD	516062805
	DD	1052830799
	DD	6188716
	DD	3200314446
	DD	79447568
	DD	1052151909
	DD	223529141
	DD	3198586975
	DD	1557009707
	DD	1049758991
	DD	1527834451
	DD	3195539792
	DD	3841571054
	DD	1046184222
	DD	3228035136
	DD	3191570603
	DD	2497745717
	DD	1041799395
	DD	3127975351
	DD	3186863029
	DD	0
	DD	3223126016
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3013228433
	DD	1015734301
	DD	952591976
	DD	1072693247
	DD	3849195912
	DD	1051256594
	DD	1248135057
	DD	3198740242
	DD	1658384276
	DD	1050712587
	DD	3312197895
	DD	3197147657
	DD	2426751598
	DD	1048219658
	DD	2075412918
	DD	3194074453
	DD	1194511818
	DD	1044659399
	DD	2861395540
	DD	3190045864
	DD	1105252788
	DD	1040325059
	DD	278204179
	DD	3185374362
	DD	0
	DD	3223257088
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	4084683796
	DD	1014352664
	DD	3549182448
	DD	1072693247
	DD	4170486715
	DD	1048984034
	DD	3652359522
	DD	3196467682
	DD	1780445294
	DD	1048420995
	DD	3329441198
	DD	3194856066
	DD	663245309
	DD	1045935418
	DD	1918070306
	DD	3191839818
	DD	4225866973
	DD	1042419329
	DD	1974315224
	DD	3187805832
	DD	847480060
	DD	1038120500
	DD	2386310431
	DD	3183105031
	DD	0
	DD	3223388160
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	951119343
	DD	1016641415
	DD	4194036288
	DD	1072693247
	DD	4207053894
	DD	1045958742
	DD	4131013457
	DD	3193442390
	DD	2503178506
	DD	1045433060
	DD	2309798544
	DD	3191868132
	DD	1503762043
	DD	1042918157
	DD	762244907
	DD	3188792499
	DD	3745081608
	DD	1039371450
	DD	3106729171
	DD	3184757959
	DD	3799011378
	DD	1035063995
	DD	693003136
	DD	3180102041
	DD	0
	DD	3223519232
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1712896388
	DD	1016000193
	DD	4281307769
	DD	1072693247
	DD	3495080264
	DD	1042943408
	DD	3483806577
	DD	3190427056
	DD	3577360645
	DD	1042374261
	DD	3557467263
	DD	3188809333
	DD	3692227868
	DD	1039911516
	DD	1459944482
	DD	3185739496
	DD	624248087
	DD	1036331657
	DD	3930021706
	DD	3181718167
	DD	439009527
	DD	1032014849
	DD	3184212578
	DD	3177110789
	DD	0
	DD	3223650304
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1958475666
	DD	1013700788
	DD	4293118680
	DD	1072693247
	DD	3829159519
	DD	1039938855
	DD	3827364885
	DD	3187422503
	DD	2374004141
	DD	1039322650
	DD	2380228874
	DD	3185757722
	DD	853065064
	DD	1036916376
	DD	3897809499
	DD	3182694159
	DD	2467115425
	DD	1033300621
	DD	2966460473
	DD	3178687133
	DD	4249027489
	DD	1028973684
	DD	3391824522
	DD	3174085926
	DD	0
	DD	3223781376
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1028808661
	DD	1012328597
	DD	4294717113
	DD	1072693247
	DD	2759857858
	DD	1036945975
	DD	2759440340
	DD	3184429623
	DD	217750550
	DD	1036278821
	DD	228557927
	DD	3182713893
	DD	868996329
	DD	1033914811
	DD	130294465
	DD	3179657124
	DD	3979034581
	DD	1030279068
	DD	1690522291
	DD	3175665582
	DD	141102418
	DD	1025941166
	DD	3942643114
	DD	3171030731
	DD	0
	DD	3223912448
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2461075155
	DD	1015729939
	DD	4294933437
	DD	1072693247
	DD	2559161110
	DD	1033930834
	DD	2559046852
	DD	3181414482
	DD	3206412049
	DD	1033243416
	DD	3218709064
	DD	3179678488
	DD	2091270467
	DD	1030857342
	DD	245853585
	DD	3176629075
	DD	1555900931
	DD	1027267783
	DD	1186881303
	DD	3172654298
	DD	1695278520
	DD	1022918007
	DD	1853146834
	DD	3167983022
	DD	0
	DD	3224043520
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2502502488
	DD	1016572066
	DD	4294962713
	DD	1072693247
	DD	3137376149
	DD	1030874690
	DD	3137268820
	DD	3178358338
	DD	1097103169
	DD	1030217134
	DD	1110504267
	DD	3176652206
	DD	1068377398
	DD	1027807171
	DD	222176953
	DD	3173610756
	DD	3440315131
	DD	1024267613
	DD	1199778592
	DD	3169654130
	DD	257981480
	DD	1019904983
	DD	1388437918
	DD	3164943417
	DD	0
	DD	3224174592
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3418697838
	DD	1016821878
	DD	4294966675
	DD	1072693247
	DD	3798207862
	DD	1027825953
	DD	3798094058
	DD	3175309601
	DD	3391459718
	DD	1027200727
	DD	3405981646
	DD	3173635799
	DD	3694208074
	DD	1024764900
	DD	2192272311
	DD	3170602971
	DD	1464408928
	DD	1021279479
	DD	2201370875
	DD	3166665997
	DD	4139632468
	DD	1016902930
	DD	2981161402
	DD	3161912586
	DD	0
	DD	3224305664
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3589768515
	DD	1013972501
	DD	4294967265
	DD	1072693247
	DD	2293865510
	DD	1023336450
	DD	2097549026
	DD	3170820098
	DD	2721138850
	DD	1022661962
	DD	1571631120
	DD	3169097035
	DD	3716649917
	DD	1020295299
	DD	3146231247
	DD	3166041588
	DD	84506245
	DD	1016656297
	DD	2231398946
	DD	3162043093
	DD	3305646943
	DD	1012441980
	DD	402214167
	DD	3157503609
	DD	0
	DD	3224436736
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2152156943
	DD	1016184605
	DD	4294967294
	DD	1072693247
	DD	1074684533
	DD	1018634353
	DD	1074437943
	DD	3166118001
	DD	967276073
	DD	1018090988
	DD	995296768
	DD	3164526060
	DD	4275132894
	DD	1015589675
	DD	304133116
	DD	3161485853
	DD	1232215992
	DD	1012058464
	DD	559363548
	DD	3157444977
	DD	1487618473
	DD	1007759094
	DD	2355811294
	DD	3152771929
	DD	3381626085
	DD	3224537056
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	4294967295
	DD	0
	DD	4294967295
	DD	0
	DD	4294967295
	DD	0
	DD	4294967295
	DD	0
	DD	4294967295
	DD	0
	DD	4294967295
	DD	0
	DD	4294967295
	DD	0
	DD	4294967295
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
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2147352576
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	1069416448
	DD	7733248
	DD	7733248
	DD	7733248
	DD	7733248
	DD	7733248
	DD	7733248
	DD	7733248
	DD	7733248
	DD	7733248
	DD	7733248
	DD	7733248
	DD	7733248
	DD	7733248
	DD	7733248
	DD	7733248
	DD	7733248
	DD	535822336
	DD	535822336
	DD	535822336
	DD	535822336
	DD	535822336
	DD	535822336
	DD	535822336
	DD	535822336
	DD	535822336
	DD	535822336
	DD	535822336
	DD	535822336
	DD	535822336
	DD	535822336
	DD	535822336
	DD	535822336
_2il0floatpacket_17	DD	000760000H,000000000H,000000000H,000000000H
_imldTanhHATab	DD	0
	DD	1072693248
	DD	0
	DD	3220176896
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
