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
	PUBLIC __svml_sinh1_ha_e9

__svml_sinh1_ha_e9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 312
        vmovapd   xmm5, xmm0
        vmovups   XMMWORD PTR [208+rsp], xmm15
        mov       eax, 1082453555
        vmovups   XMMWORD PTR [192+rsp], xmm14
        lea       rcx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [240+rsp], xmm13
        vmovups   XMMWORD PTR [256+rsp], xmm12
        vmovups   XMMWORD PTR [272+rsp], xmm11
        vmovd     xmm12, eax
        vmovups   XMMWORD PTR [176+rsp], xmm8
        vmovups   XMMWORD PTR [288+rsp], xmm7
        vmovups   XMMWORD PTR [224+rsp], xmm6
        mov       QWORD PTR [168+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        vmovsd    xmm1, QWORD PTR [__svml_dsinh_ha_data_internal+1408]
        and       r13, -64
        vandpd    xmm4, xmm1, xmm5
        vxorpd    xmm8, xmm4, xmm5
        vmulsd    xmm7, xmm8, QWORD PTR [__svml_dsinh_ha_data_internal+1216]
        vmovsd    xmm11, QWORD PTR [__svml_dsinh_ha_data_internal+512]
        vmovsd    xmm13, QWORD PTR [__svml_dsinh_ha_data_internal+576]
        vaddsd    xmm0, xmm7, xmm11
        vpshufd   xmm14, xmm8, 85
        vcmpeqsd  xmm7, xmm13, xmm0
        vblendvpd xmm13, xmm0, xmm11, xmm7
        vpcmpgtd  xmm6, xmm14, xmm12
        vmovq     xmm15, QWORD PTR [__svml_dsinh_ha_data_internal+704]
        vpand     xmm0, xmm13, xmm15
        vsubsd    xmm12, xmm13, xmm11
        vmovmskps eax, xmm6
        vmovsd    xmm11, QWORD PTR [__svml_dsinh_ha_data_internal+1280]
        vpsllq    xmm15, xmm0, 3
        vmulsd    xmm11, xmm11, xmm12
        vmovd     edx, xmm15
        vmovsd    xmm2, QWORD PTR [__svml_dsinh_ha_data_internal+1088]
        vsubsd    xmm11, xmm8, xmm11
        vmovsd    xmm8, QWORD PTR [__svml_dsinh_ha_data_internal+1344]
        vmulsd    xmm8, xmm8, xmm12
        vpxor     xmm12, xmm13, xmm0
        movsxd    rdx, edx
        vpsllq    xmm12, xmm12, 48
        vmovsd    xmm3, QWORD PTR [__svml_dsinh_ha_data_internal+1152]
        vsubsd    xmm8, xmm11, xmm8
        vmovsd    xmm15, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+rcx+rdx]
        vmovsd    xmm14, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+rcx+rdx]
        vpaddq    xmm0, xmm15, xmm12
        vpsubq    xmm13, xmm14, xmm12
        vmulsd    xmm11, xmm8, xmm8
        vsubsd    xmm15, xmm0, xmm13
        vaddsd    xmm14, xmm0, xmm13
        vmulsd    xmm2, xmm2, xmm11
        vsubsd    xmm0, xmm15, xmm0
        vmulsd    xmm3, xmm3, xmm11
        vmovsd    xmm7, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+rcx+rdx]
        vandnpd   xmm1, xmm1, xmm7
        vpsubq    xmm7, xmm7, xmm12
        vcmpltsd  xmm1, xmm1, xmm12
        vmovsd    xmm6, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+rcx+rdx]
        vpaddq    xmm6, xmm6, xmm12
        vandnpd   xmm12, xmm1, xmm7
        vaddsd    xmm1, xmm13, xmm0
        vaddsd    xmm0, xmm2, QWORD PTR [__svml_dsinh_ha_data_internal+960]
        vaddsd    xmm2, xmm3, QWORD PTR [__svml_dsinh_ha_data_internal+1024]
        vsubsd    xmm6, xmm6, xmm1
        vmulsd    xmm1, xmm0, xmm11
        vmulsd    xmm3, xmm2, xmm11
        vsubsd    xmm7, xmm6, xmm12
        vaddsd    xmm13, xmm1, QWORD PTR [__svml_dsinh_ha_data_internal+832]
        vaddsd    xmm6, xmm3, QWORD PTR [__svml_dsinh_ha_data_internal+896]
        vmulsd    xmm1, xmm13, xmm11
        vmulsd    xmm12, xmm6, xmm11
        mov       QWORD PTR [304+rsp], r13
        vaddsd    xmm0, xmm12, QWORD PTR [__svml_dsinh_ha_data_internal+768]
        vmulsd    xmm11, xmm0, xmm11
        vmulsd    xmm0, xmm8, xmm14
        vmulsd    xmm8, xmm1, xmm0
        vaddsd    xmm14, xmm8, xmm7
        vmulsd    xmm7, xmm11, xmm15
        vaddsd    xmm1, xmm7, xmm14
        vaddsd    xmm2, xmm0, xmm1
        vaddsd    xmm15, xmm2, xmm15
        vorpd     xmm0, xmm4, xmm15
        and       eax, 1
        jne       _B1_3

_B1_2::

        vmovups   xmm6, XMMWORD PTR [224+rsp]
        vmovups   xmm7, XMMWORD PTR [288+rsp]
        vmovups   xmm8, XMMWORD PTR [176+rsp]
        vmovups   xmm11, XMMWORD PTR [272+rsp]
        vmovups   xmm12, XMMWORD PTR [256+rsp]
        vmovups   xmm13, XMMWORD PTR [240+rsp]
        vmovups   xmm14, XMMWORD PTR [192+rsp]
        vmovups   xmm15, XMMWORD PTR [208+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 312
        ret

_B1_3::

        vmovsd    QWORD PTR [r13], xmm5
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B1_6

_B1_4::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B1_2

_B1_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dsinh_ha_cout_rare_internal
        jmp       _B1_4
        ALIGN     16

_B1_7::

__svml_sinh1_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh1_ha_e9_B1_B6:
	DD	1339137
	DD	1430639
	DD	944231
	DD	1210462
	DD	755797
	DD	1161288
	DD	1099839
	DD	1038390
	DD	845862
	DD	915480
	DD	2556171

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_7
	DD	imagerel _unwind___svml_sinh1_ha_e9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinh4_ha_e9

__svml_sinh4_ha_e9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L20::

        sub       rsp, 552
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [240+rsp], ymm15
        vmovups   YMMWORD PTR [304+rsp], ymm14
        vmovups   YMMWORD PTR [368+rsp], ymm13
        vmovups   YMMWORD PTR [400+rsp], ymm12
        vmovups   YMMWORD PTR [432+rsp], ymm11
        vmovups   YMMWORD PTR [464+rsp], ymm10
        vmovups   YMMWORD PTR [496+rsp], ymm9
        vmovups   YMMWORD PTR [208+rsp], ymm8
        vmovups   YMMWORD PTR [336+rsp], ymm7
        vmovups   YMMWORD PTR [272+rsp], ymm6
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovupd   ymm3, YMMWORD PTR [__svml_dsinh_ha_data_internal+1408]
        and       r13, -64
        vmovupd   YMMWORD PTR [32+r13], ymm0
        vmovupd   ymm15, YMMWORD PTR [__svml_dsinh_ha_data_internal+512]
        vmovupd   xmm11, XMMWORD PTR [__svml_dsinh_ha_data_internal+704]
        vandpd    ymm5, ymm3, ymm0
        vxorpd    ymm0, ymm5, ymm0
        vmulpd    ymm8, ymm0, YMMWORD PTR [__svml_dsinh_ha_data_internal+1216]
        vmovupd   YMMWORD PTR [r13], ymm5
        vaddpd    ymm12, ymm15, ymm8
        vcmpeqpd  ymm10, ymm12, YMMWORD PTR [__svml_dsinh_ha_data_internal+576]
        vblendvpd ymm2, ymm12, ymm15, ymm10
        mov       QWORD PTR [536+rsp], r13
        vpand     xmm5, xmm2, xmm11
        vextractf128 xmm13, ymm0, 1
        vpsllq    xmm8, xmm5, 3
        vextractf128 xmm4, ymm2, 1
        vmovd     ecx, xmm8
        vshufps   xmm9, xmm0, xmm13, 221
        vandps    xmm6, xmm4, xmm11
        vpcmpgtd  xmm1, xmm9, XMMWORD PTR [__svml_dsinh_ha_data_internal+640]
        vpsllq    xmm9, xmm6, 3
        vmovd     r9d, xmm9
        vxorps    xmm4, xmm4, xmm6
        vmovmskps r8d, xmm1
        vpsllq    xmm4, xmm4, 48
        movsxd    r9, r9d
        vpextrd   r10d, xmm9, 2
        movsxd    r10, r10d
        vpextrd   eax, xmm8, 2
        vmovsd    xmm1, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+rdx+r9]
        movsxd    rcx, ecx
        vmovhpd   xmm8, xmm1, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+rdx+r10]
        vmovsd    xmm1, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+rdx+r9]
        vmovhpd   xmm12, xmm1, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+rdx+r10]
        vsubpd    ymm1, ymm2, ymm15
        vpxor     xmm2, xmm2, xmm5
        vmovsd    xmm13, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+rdx+rcx]
        vpsllq    xmm2, xmm2, 48
        vmovsd    xmm14, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+rdx+rcx]
        vmulpd    ymm15, ymm1, YMMWORD PTR [__svml_dsinh_ha_data_internal+1280]
        vmulpd    ymm1, ymm1, YMMWORD PTR [__svml_dsinh_ha_data_internal+1344]
        vmovsd    xmm11, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+rdx+r9]
        vpaddq    xmm5, xmm8, xmm4
        vpaddq    xmm12, xmm12, xmm4
        vsubpd    ymm0, ymm0, ymm15
        vmovhpd   xmm10, xmm11, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+rdx+r10]
        vpsubq    xmm8, xmm10, xmm4
        vsubpd    ymm0, ymm0, ymm1
        movsxd    rax, eax
        vmulpd    ymm1, ymm0, ymm0
        vmovhpd   xmm7, xmm13, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+rdx+rax]
        vmovhpd   xmm9, xmm14, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+rdx+rax]
        vmovsd    xmm13, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+rdx+rcx]
        vmovsd    xmm14, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+rdx+rcx]
        vpaddq    xmm7, xmm7, xmm2
        vpsubq    xmm6, xmm9, xmm2
        vmulpd    ymm15, ymm1, YMMWORD PTR [__svml_dsinh_ha_data_internal+1152]
        vmovhpd   xmm11, xmm13, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+rdx+rax]
        vmovhpd   xmm13, xmm14, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+rdx+rax]
        vmovsd    xmm14, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+rdx+r9]
        vmovhpd   xmm14, xmm14, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+rdx+r10]
        vmovupd   XMMWORD PTR [32+rsp], xmm14
        vinsertf128 ymm7, ymm7, xmm5, 1
        vpaddq    xmm5, xmm11, xmm2
        vinsertf128 ymm14, ymm13, xmm14, 1
        vinsertf128 ymm12, ymm5, xmm12, 1
        vinsertf128 ymm5, ymm2, xmm4, 1
        vpsubq    xmm13, xmm13, xmm2
        vmovupd   xmm2, XMMWORD PTR [32+rsp]
        vpsubq    xmm4, xmm2, xmm4
        vandnpd   ymm3, ymm3, ymm14
        vmulpd    ymm14, ymm1, YMMWORD PTR [__svml_dsinh_ha_data_internal+1088]
        vcmplt_oqpd ymm3, ymm3, ymm5
        vaddpd    ymm2, ymm14, YMMWORD PTR [__svml_dsinh_ha_data_internal+960]
        vinsertf128 ymm10, ymm6, xmm8, 1
        vinsertf128 ymm6, ymm13, xmm4, 1
        vaddpd    ymm4, ymm15, YMMWORD PTR [__svml_dsinh_ha_data_internal+1024]
        vsubpd    ymm8, ymm7, ymm10
        vaddpd    ymm9, ymm7, ymm10
        vmulpd    ymm5, ymm1, ymm4
        vsubpd    ymm7, ymm8, ymm7
        vandnpd   ymm11, ymm3, ymm6
        vmulpd    ymm3, ymm1, ymm2
        vmulpd    ymm2, ymm0, ymm9
        vaddpd    ymm10, ymm10, ymm7
        vaddpd    ymm6, ymm5, YMMWORD PTR [__svml_dsinh_ha_data_internal+896]
        vsubpd    ymm12, ymm12, ymm10
        vaddpd    ymm10, ymm3, YMMWORD PTR [__svml_dsinh_ha_data_internal+832]
        vmulpd    ymm7, ymm1, ymm6
        vsubpd    ymm13, ymm12, ymm11
        vmulpd    ymm12, ymm1, ymm10
        vaddpd    ymm11, ymm7, YMMWORD PTR [__svml_dsinh_ha_data_internal+768]
        vmulpd    ymm0, ymm12, ymm2
        vmulpd    ymm1, ymm1, ymm11
        vaddpd    ymm9, ymm13, ymm0
        vmulpd    ymm0, ymm8, ymm1
        vaddpd    ymm1, ymm9, ymm0
        vaddpd    ymm3, ymm2, ymm1
        vaddpd    ymm8, ymm8, ymm3
        vorpd     ymm0, ymm8, YMMWORD PTR [r13]
        test      r8d, r8d
        jne       _B2_3

_B2_2::

        vmovups   ymm6, YMMWORD PTR [272+rsp]
        vmovups   ymm7, YMMWORD PTR [336+rsp]
        vmovups   ymm8, YMMWORD PTR [208+rsp]
        vmovups   ymm9, YMMWORD PTR [496+rsp]
        vmovups   ymm10, YMMWORD PTR [464+rsp]
        vmovups   ymm11, YMMWORD PTR [432+rsp]
        vmovups   ymm12, YMMWORD PTR [400+rsp]
        vmovups   ymm13, YMMWORD PTR [368+rsp]
        vmovups   ymm14, YMMWORD PTR [304+rsp]
        vmovups   ymm15, YMMWORD PTR [240+rsp]
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

        call      __svml_dsinh_ha_cout_rare_internal
        jmp       _B2_8
        ALIGN     16

_B2_11::

__svml_sinh4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh4_ha_e9_B1_B3:
	DD	1602561
	DD	4379764
	DD	1140844
	DD	1407075
	DD	886874
	DD	2070609
	DD	1943624
	DD	1816639
	DD	1689654
	DD	1562669
	DD	1304612
	DD	1046555
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_sinh4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh4_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_sinh4_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_6
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_sinh4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinh2_ha_ex

__svml_sinh2_ha_ex	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L53::

        sub       rsp, 344
        lea       r8, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [192+rsp], xmm13
        movups    XMMWORD PTR [208+rsp], xmm12
        movups    XMMWORD PTR [224+rsp], xmm11
        movaps    xmm11, xmm0
        movups    XMMWORD PTR [240+rsp], xmm10
        movups    XMMWORD PTR [256+rsp], xmm9
        movups    XMMWORD PTR [272+rsp], xmm8
        movups    XMMWORD PTR [288+rsp], xmm7
        movups    XMMWORD PTR [304+rsp], xmm6
        mov       QWORD PTR [320+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movups    xmm4, XMMWORD PTR [__svml_dsinh_ha_data_internal+1408]
        and       r13, -64
        movaps    xmm0, xmm4
        andps     xmm0, xmm11
        movaps    xmm10, xmm0
        movups    xmm3, XMMWORD PTR [__svml_dsinh_ha_data_internal+1216]
        pxor      xmm10, xmm11
        mulpd     xmm3, xmm10
        movups    xmm13, XMMWORD PTR [__svml_dsinh_ha_data_internal+512]
        addpd     xmm3, xmm13
        movups    xmm1, XMMWORD PTR [__svml_dsinh_ha_data_internal+576]
        cmpeqpd   xmm1, xmm3
        movaps    xmm12, xmm1
        andps     xmm1, xmm13
        andnps    xmm12, xmm3
        movq      xmm2, QWORD PTR [__svml_dsinh_ha_data_internal+640]
        orps      xmm12, xmm1
        movdqu    xmm1, XMMWORD PTR [__svml_dsinh_ha_data_internal+704]
        pshufd    xmm5, xmm10, 221
        pand      xmm1, xmm12
        pcmpgtd   xmm5, xmm2
        movmskps  eax, xmm5
        movdqa    xmm5, xmm1
        psllq     xmm5, 3
        pshufd    xmm8, xmm5, 2
        movd      ecx, xmm8
        movaps    xmm8, xmm12
        movups    xmm6, XMMWORD PTR [__svml_dsinh_ha_data_internal+1152]
        pxor      xmm12, xmm1
        movups    xmm7, XMMWORD PTR [__svml_dsinh_ha_data_internal+1088]
        psllq     xmm12, 48
        subpd     xmm8, xmm13
        movd      edx, xmm5
        movups    xmm13, XMMWORD PTR [__svml_dsinh_ha_data_internal+1280]
        mulpd     xmm13, xmm8
        subpd     xmm10, xmm13
        movups    xmm13, XMMWORD PTR [__svml_dsinh_ha_data_internal+1344]
        and       eax, 3
        mulpd     xmm13, xmm8
        subpd     xmm10, xmm13
        movaps    xmm8, xmm10
        mulpd     xmm8, xmm10
        mulpd     xmm6, xmm8
        mulpd     xmm7, xmm8
        addpd     xmm6, XMMWORD PTR [__svml_dsinh_ha_data_internal+1024]
        addpd     xmm7, XMMWORD PTR [__svml_dsinh_ha_data_internal+960]
        mulpd     xmm6, xmm8
        mulpd     xmm7, xmm8
        addpd     xmm6, XMMWORD PTR [__svml_dsinh_ha_data_internal+896]
        addpd     xmm7, XMMWORD PTR [__svml_dsinh_ha_data_internal+832]
        mulpd     xmm6, xmm8
        mulpd     xmm7, xmm8
        addpd     xmm6, XMMWORD PTR [__svml_dsinh_ha_data_internal+768]
        movsxd    rdx, edx
        movsxd    rcx, ecx
        mulpd     xmm8, xmm6
        movsd     xmm2, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+r8+rdx]
        movhpd    xmm2, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+r8+rcx]
        movsd     xmm5, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+r8+rdx]
        movhpd    xmm5, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+r8+rcx]
        paddq     xmm2, xmm12
        psubq     xmm5, xmm12
        movaps    xmm1, xmm2
        movaps    xmm13, xmm2
        subpd     xmm1, xmm5
        addpd     xmm13, xmm5
        mulpd     xmm8, xmm1
        mulpd     xmm10, xmm13
        movsd     xmm3, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+r8+rdx]
        movsd     xmm9, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+r8+rdx]
        movhpd    xmm3, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+r8+rcx]
        movhpd    xmm9, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+r8+rcx]
        andnps    xmm4, xmm3
        paddq     xmm9, xmm12
        psubq     xmm3, xmm12
        cmpltpd   xmm4, xmm12
        mulpd     xmm7, xmm10
        movaps    xmm12, xmm1
        andnps    xmm4, xmm3
        subpd     xmm12, xmm2
        addpd     xmm5, xmm12
        subpd     xmm9, xmm5
        subpd     xmm9, xmm4
        addpd     xmm9, xmm7
        addpd     xmm9, xmm8
        addpd     xmm10, xmm9
        addpd     xmm1, xmm10
        mov       QWORD PTR [328+rsp], r13
        orps      xmm0, xmm1
        jne       _B3_3

_B3_2::

        movups    xmm6, XMMWORD PTR [304+rsp]
        movups    xmm7, XMMWORD PTR [288+rsp]
        movups    xmm8, XMMWORD PTR [272+rsp]
        movups    xmm9, XMMWORD PTR [256+rsp]
        movups    xmm10, XMMWORD PTR [240+rsp]
        movups    xmm11, XMMWORD PTR [224+rsp]
        movups    xmm12, XMMWORD PTR [208+rsp]
        movups    xmm13, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [320+rsp]
        add       rsp, 344
        ret

_B3_3::

        movups    XMMWORD PTR [r13], xmm11
        movups    XMMWORD PTR [64+r13], xmm0
        je        _B3_2

_B3_6::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

_B3_7::

        mov       ecx, ebx
        mov       edx, 1
        shl       edx, cl
        test      esi, edx
        jne       _B3_10

_B3_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B3_7

_B3_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [64+r13]
        jmp       _B3_2

_B3_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dsinh_ha_cout_rare_internal
        jmp       _B3_8
        ALIGN     16

_B3_11::

__svml_sinh2_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh2_ha_ex_B1_B3:
	DD	1336321
	DD	2675812
	DD	1271900
	DD	1210452
	DD	1149004
	DD	1087555
	DD	1026106
	DD	964653
	DD	903204
	DD	841755
	DD	2818315

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_sinh2_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh2_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_sinh2_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_6
	DD	imagerel _B3_11
	DD	imagerel _unwind___svml_sinh2_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinh1_ha_l9

__svml_sinh1_ha_l9	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L76::

        sub       rsp, 312
        mov       eax, 1082453555
        vmovups   XMMWORD PTR [256+rsp], xmm15
        lea       rcx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [272+rsp], xmm14
        vmovups   XMMWORD PTR [240+rsp], xmm11
        vmovups   XMMWORD PTR [288+rsp], xmm10
        vmovd     xmm11, eax
        vmovups   XMMWORD PTR [176+rsp], xmm9
        vmovapd   xmm9, xmm0
        vmovups   XMMWORD PTR [192+rsp], xmm8
        vmovups   XMMWORD PTR [208+rsp], xmm7
        vmovups   XMMWORD PTR [224+rsp], xmm6
        mov       QWORD PTR [168+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        vmovsd    xmm1, QWORD PTR [__svml_dsinh_ha_data_internal+1408]
        and       r13, -64
        vandpd    xmm8, xmm1, xmm9
        vxorpd    xmm4, xmm8, xmm9
        vmovsd    xmm3, QWORD PTR [__svml_dsinh_ha_data_internal+512]
        vmovapd   xmm15, xmm4
        vfmadd132sd xmm15, xmm3, QWORD PTR [__svml_dsinh_ha_data_internal+1216]
        vpshufd   xmm2, xmm4, 85
        vmovsd    xmm7, QWORD PTR [__svml_dsinh_ha_data_internal+576]
        vpcmpgtd  xmm0, xmm2, xmm11
        vmovq     xmm14, QWORD PTR [__svml_dsinh_ha_data_internal+704]
        vmovsd    xmm6, QWORD PTR [__svml_dsinh_ha_data_internal+1152]
        vmovmskps eax, xmm0
        vcmpeqsd  xmm2, xmm7, xmm15
        vblendvpd xmm10, xmm15, xmm3, xmm2
        vmovsd    xmm15, QWORD PTR [__svml_dsinh_ha_data_internal+1280]
        vpand     xmm11, xmm10, xmm14
        vsubsd    xmm3, xmm10, xmm3
        vpsllq    xmm14, xmm11, 3
        vpxor     xmm10, xmm10, xmm11
        vmovd     edx, xmm14
        vpsllq    xmm10, xmm10, 48
        vmovsd    xmm5, QWORD PTR [__svml_dsinh_ha_data_internal+1088]
        vfnmadd213sd xmm15, xmm3, xmm4
        vmovsd    xmm4, QWORD PTR [__svml_dsinh_ha_data_internal+1344]
        movsxd    rdx, edx
        vfnmadd213sd xmm4, xmm3, xmm15
        vmovsd    xmm14, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+rcx+rdx]
        vmovsd    xmm2, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+rcx+rdx]
        vmovsd    xmm0, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+rcx+rdx]
        vpaddq    xmm15, xmm14, xmm10
        vpsubq    xmm2, xmm2, xmm10
        vmulsd    xmm3, xmm4, xmm4
        vsubsd    xmm11, xmm15, xmm2
        vaddsd    xmm14, xmm15, xmm2
        vfmadd213sd xmm6, xmm3, QWORD PTR [__svml_dsinh_ha_data_internal+1024]
        vfmadd213sd xmm5, xmm3, QWORD PTR [__svml_dsinh_ha_data_internal+960]
        vandnpd   xmm1, xmm1, xmm0
        vpsubq    xmm0, xmm0, xmm10
        vcmpltsd  xmm1, xmm1, xmm10
        vfmadd213sd xmm6, xmm3, QWORD PTR [__svml_dsinh_ha_data_internal+896]
        vfmadd213sd xmm5, xmm3, QWORD PTR [__svml_dsinh_ha_data_internal+832]
        vandnpd   xmm0, xmm1, xmm0
        vsubsd    xmm1, xmm11, xmm15
        vfmadd213sd xmm6, xmm3, QWORD PTR [__svml_dsinh_ha_data_internal+768]
        vmovsd    xmm7, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+rcx+rdx]
        vaddsd    xmm2, xmm2, xmm1
        vpaddq    xmm7, xmm7, xmm10
        mov       QWORD PTR [304+rsp], r13
        vsubsd    xmm7, xmm7, xmm2
        vsubsd    xmm10, xmm7, xmm0
        vmulsd    xmm0, xmm5, xmm3
        vmulsd    xmm5, xmm6, xmm3
        vfmadd213sd xmm0, xmm4, xmm4
        vfmadd213sd xmm5, xmm11, xmm10
        vfmadd213sd xmm0, xmm14, xmm5
        vaddsd    xmm1, xmm0, xmm11
        vorpd     xmm0, xmm8, xmm1
        and       eax, 1
        jne       _B4_3

_B4_2::

        vmovups   xmm6, XMMWORD PTR [224+rsp]
        vmovups   xmm7, XMMWORD PTR [208+rsp]
        vmovups   xmm8, XMMWORD PTR [192+rsp]
        vmovups   xmm9, XMMWORD PTR [176+rsp]
        vmovups   xmm10, XMMWORD PTR [288+rsp]
        vmovups   xmm11, XMMWORD PTR [240+rsp]
        vmovups   xmm14, XMMWORD PTR [272+rsp]
        vmovups   xmm15, XMMWORD PTR [256+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 312
        ret

_B4_3::

        vmovsd    QWORD PTR [r13], xmm9
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B4_6

_B4_4::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B4_2

_B4_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dsinh_ha_cout_rare_internal
        jmp       _B4_4
        ALIGN     16

_B4_7::

__svml_sinh1_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh1_ha_l9_B1_B6:
	DD	1339137
	DD	1430639
	DD	944231
	DD	882782
	DD	821333
	DD	759880
	DD	1222715
	DD	1030194
	DD	1173545
	DD	1112089
	DD	2556171

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_7
	DD	imagerel _unwind___svml_sinh1_ha_l9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinh2_ha_l9

__svml_sinh2_ha_l9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L95::

        sub       rsp, 344
        lea       r8, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [272+rsp], xmm15
        vmovups   XMMWORD PTR [288+rsp], xmm14
        vmovups   XMMWORD PTR [304+rsp], xmm13
        vmovups   XMMWORD PTR [256+rsp], xmm11
        vmovups   XMMWORD PTR [192+rsp], xmm10
        vmovups   XMMWORD PTR [208+rsp], xmm9
        vmovups   XMMWORD PTR [224+rsp], xmm8
        vmovapd   xmm8, xmm0
        vmovups   XMMWORD PTR [240+rsp], xmm6
        mov       QWORD PTR [320+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovupd   xmm9, XMMWORD PTR [__svml_dsinh_ha_data_internal+1408]
        and       r13, -64
        vandpd    xmm6, xmm9, xmm8
        vmovupd   xmm11, XMMWORD PTR [__svml_dsinh_ha_data_internal+1216]
        vxorpd    xmm13, xmm6, xmm8
        vmovupd   xmm14, XMMWORD PTR [__svml_dsinh_ha_data_internal+512]
        vfmadd213pd xmm11, xmm13, xmm14
        vmovq     xmm2, QWORD PTR [__svml_dsinh_ha_data_internal+640]
        vpshufd   xmm1, xmm13, 221
        vpcmpgtd  xmm3, xmm1, xmm2
        vmovupd   xmm4, XMMWORD PTR [__svml_dsinh_ha_data_internal+1152]
        vcmpeqpd  xmm15, xmm11, XMMWORD PTR [__svml_dsinh_ha_data_internal+576]
        vmovmskps edx, xmm3
        vblendvpd xmm3, xmm11, xmm14, xmm15
        vmovupd   xmm5, XMMWORD PTR [__svml_dsinh_ha_data_internal+1088]
        vsubpd    xmm14, xmm3, xmm14
        vpand     xmm0, xmm3, XMMWORD PTR [__svml_dsinh_ha_data_internal+704]
        vpsllq    xmm1, xmm0, 3
        vpxor     xmm0, xmm3, xmm0
        vfnmadd231pd xmm13, xmm14, XMMWORD PTR [__svml_dsinh_ha_data_internal+1280]
        vmovd     eax, xmm1
        vfnmadd231pd xmm13, xmm14, XMMWORD PTR [__svml_dsinh_ha_data_internal+1344]
        movsxd    rax, eax
        vpextrd   ecx, xmm1, 2
        movsxd    rcx, ecx
        vmulpd    xmm14, xmm13, xmm13
        vmovsd    xmm2, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+r8+rax]
        vmovsd    xmm11, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+r8+rax]
        vmovsd    xmm15, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+r8+rax]
        vmovhpd   xmm1, xmm2, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+r8+rcx]
        vmovhpd   xmm2, xmm11, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+r8+rcx]
        vmovhpd   xmm11, xmm15, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+r8+rcx]
        vpsllq    xmm15, xmm0, 48
        vpaddq    xmm3, xmm1, xmm15
        vpsubq    xmm0, xmm2, xmm15
        vfmadd213pd xmm4, xmm14, XMMWORD PTR [__svml_dsinh_ha_data_internal+1024]
        vfmadd213pd xmm5, xmm14, XMMWORD PTR [__svml_dsinh_ha_data_internal+960]
        vandnpd   xmm9, xmm9, xmm11
        vmovsd    xmm10, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+r8+rax]
        vmovhpd   xmm10, xmm10, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+r8+rcx]
        vpaddq    xmm10, xmm10, xmm15
        vpsubq    xmm11, xmm11, xmm15
        vsubpd    xmm2, xmm3, xmm0
        vaddpd    xmm1, xmm3, xmm0
        vcmpltpd  xmm9, xmm9, xmm15
        vfmadd213pd xmm4, xmm14, XMMWORD PTR [__svml_dsinh_ha_data_internal+896]
        vfmadd213pd xmm5, xmm14, XMMWORD PTR [__svml_dsinh_ha_data_internal+832]
        vsubpd    xmm3, xmm2, xmm3
        vfmadd213pd xmm4, xmm14, XMMWORD PTR [__svml_dsinh_ha_data_internal+768]
        vmulpd    xmm5, xmm14, xmm5
        vaddpd    xmm0, xmm0, xmm3
        vmulpd    xmm4, xmm14, xmm4
        vsubpd    xmm3, xmm10, xmm0
        vfmadd213pd xmm5, xmm13, xmm13
        vandnpd   xmm11, xmm9, xmm11
        vsubpd    xmm9, xmm3, xmm11
        vfmadd213pd xmm4, xmm2, xmm9
        vfmadd213pd xmm5, xmm1, xmm4
        vaddpd    xmm0, xmm2, xmm5
        mov       QWORD PTR [328+rsp], r13
        vorpd     xmm0, xmm6, xmm0
        and       edx, 3
        jne       _B5_3

_B5_2::

        vmovups   xmm6, XMMWORD PTR [240+rsp]
        vmovups   xmm8, XMMWORD PTR [224+rsp]
        vmovups   xmm9, XMMWORD PTR [208+rsp]
        vmovups   xmm10, XMMWORD PTR [192+rsp]
        vmovups   xmm11, XMMWORD PTR [256+rsp]
        vmovups   xmm13, XMMWORD PTR [304+rsp]
        vmovups   xmm14, XMMWORD PTR [288+rsp]
        vmovups   xmm15, XMMWORD PTR [272+rsp]
        mov       r13, QWORD PTR [320+rsp]
        add       rsp, 344
        ret

_B5_3::

        vmovupd   XMMWORD PTR [r13], xmm8
        vmovupd   XMMWORD PTR [64+r13], xmm0
        je        _B5_2

_B5_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B5_7::

        bt        esi, ebx
        jc        _B5_10

_B5_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B5_7

_B5_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B5_2

_B5_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dsinh_ha_cout_rare_internal
        jmp       _B5_8
        ALIGN     16

_B5_11::

__svml_sinh2_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh2_ha_l9_B1_B3:
	DD	1336833
	DD	2675814
	DD	1009758
	DD	952401
	DD	890952
	DD	829503
	DD	1095734
	DD	1300525
	DD	1239076
	DD	1177627
	DD	2818315

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_sinh2_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh2_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_sinh2_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_6
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_sinh2_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinh1_ha_ex

__svml_sinh1_ha_ex	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L118::

        sub       rsp, 312
        movaps    xmm5, xmm0
        movups    XMMWORD PTR [176+rsp], xmm13
        mov       eax, 1082453555
        movups    XMMWORD PTR [192+rsp], xmm12
        lea       rcx, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [208+rsp], xmm11
        movups    XMMWORD PTR [224+rsp], xmm10
        movups    XMMWORD PTR [256+rsp], xmm9
        movd      xmm4, eax
        movups    XMMWORD PTR [272+rsp], xmm8
        movups    XMMWORD PTR [288+rsp], xmm7
        movups    XMMWORD PTR [240+rsp], xmm6
        mov       QWORD PTR [168+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        movsd     xmm7, QWORD PTR [__svml_dsinh_ha_data_internal+1408]
        and       r13, -64
        movaps    xmm0, xmm7
        andps     xmm0, xmm5
        movaps    xmm12, xmm0
        pxor      xmm12, xmm5
        movaps    xmm3, xmm12
        mulsd     xmm3, QWORD PTR [__svml_dsinh_ha_data_internal+1216]
        movsd     xmm13, QWORD PTR [__svml_dsinh_ha_data_internal+512]
        movsd     xmm8, QWORD PTR [__svml_dsinh_ha_data_internal+576]
        addsd     xmm3, xmm13
        pshufd    xmm2, xmm12, 85
        cmpeqsd   xmm8, xmm3
        pcmpgtd   xmm2, xmm4
        movmskps  eax, xmm2
        movaps    xmm2, xmm8
        andnps    xmm2, xmm3
        andps     xmm8, xmm13
        orps      xmm2, xmm8
        movaps    xmm3, xmm2
        and       eax, 1
        movq      xmm1, QWORD PTR [__svml_dsinh_ha_data_internal+704]
        subsd     xmm3, xmm13
        movsd     xmm13, QWORD PTR [__svml_dsinh_ha_data_internal+1280]
        pand      xmm1, xmm2
        mulsd     xmm13, xmm3
        movdqa    xmm9, xmm1
        psllq     xmm9, 3
        pxor      xmm2, xmm1
        movd      edx, xmm9
        psllq     xmm2, 48
        movsd     xmm11, QWORD PTR [__svml_dsinh_ha_data_internal+1152]
        subsd     xmm12, xmm13
        movsd     xmm13, QWORD PTR [__svml_dsinh_ha_data_internal+1344]
        mulsd     xmm13, xmm3
        movsxd    rdx, edx
        subsd     xmm12, xmm13
        movaps    xmm3, xmm12
        mulsd     xmm3, xmm12
        mulsd     xmm11, xmm3
        movsd     xmm10, QWORD PTR [__svml_dsinh_ha_data_internal+1088]
        movsd     xmm4, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+rcx+rdx]
        mulsd     xmm10, xmm3
        addsd     xmm11, QWORD PTR [__svml_dsinh_ha_data_internal+1024]
        paddq     xmm4, xmm2
        mulsd     xmm11, xmm3
        addsd     xmm10, QWORD PTR [__svml_dsinh_ha_data_internal+960]
        movsd     xmm8, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+rcx+rdx]
        movaps    xmm1, xmm4
        movsd     xmm6, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+rcx+rdx]
        movaps    xmm13, xmm4
        psubq     xmm8, xmm2
        mulsd     xmm10, xmm3
        addsd     xmm11, QWORD PTR [__svml_dsinh_ha_data_internal+896]
        subsd     xmm1, xmm8
        addsd     xmm13, xmm8
        mulsd     xmm11, xmm3
        addsd     xmm10, QWORD PTR [__svml_dsinh_ha_data_internal+832]
        mulsd     xmm12, xmm13
        mulsd     xmm10, xmm3
        addsd     xmm11, QWORD PTR [__svml_dsinh_ha_data_internal+768]
        andnps    xmm7, xmm6
        psubq     xmm6, xmm2
        mulsd     xmm11, xmm3
        cmpltsd   xmm7, xmm2
        mulsd     xmm10, xmm12
        mulsd     xmm11, xmm1
        andnps    xmm7, xmm6
        movaps    xmm6, xmm1
        movsd     xmm9, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+rcx+rdx]
        subsd     xmm6, xmm4
        paddq     xmm9, xmm2
        addsd     xmm8, xmm6
        mov       QWORD PTR [304+rsp], r13
        subsd     xmm9, xmm8
        subsd     xmm9, xmm7
        addsd     xmm10, xmm9
        addsd     xmm11, xmm10
        addsd     xmm12, xmm11
        addsd     xmm12, xmm1
        orps      xmm0, xmm12
        jne       _B6_3

_B6_2::

        movups    xmm6, XMMWORD PTR [240+rsp]
        movups    xmm7, XMMWORD PTR [288+rsp]
        movups    xmm8, XMMWORD PTR [272+rsp]
        movups    xmm9, XMMWORD PTR [256+rsp]
        movups    xmm10, XMMWORD PTR [224+rsp]
        movups    xmm11, XMMWORD PTR [208+rsp]
        movups    xmm12, XMMWORD PTR [192+rsp]
        movups    xmm13, XMMWORD PTR [176+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 312
        ret

_B6_3::

        movsd     QWORD PTR [r13], xmm5
        movsd     QWORD PTR [64+r13], xmm0
        jne       _B6_6

_B6_4::

        movsd     xmm0, QWORD PTR [64+r13]
        jmp       _B6_2

_B6_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dsinh_ha_cout_rare_internal
        jmp       _B6_4
        ALIGN     16

_B6_7::

__svml_sinh1_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh1_ha_ex_B1_B6:
	DD	1338369
	DD	1430636
	DD	1009764
	DD	1210460
	DD	1149012
	DD	1087559
	DD	960574
	DD	899125
	DD	837669
	DD	776215
	DD	2556171

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_sinh1_ha_ex_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinh4_ha_l9

__svml_sinh4_ha_l9	PROC	EXPORT

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L137::

        sub       rsp, 552
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [464+rsp], ymm15
        vmovups   YMMWORD PTR [496+rsp], ymm14
        vmovups   YMMWORD PTR [336+rsp], ymm11
        vmovups   YMMWORD PTR [400+rsp], ymm10
        vmovups   YMMWORD PTR [304+rsp], ymm9
        vmovups   YMMWORD PTR [368+rsp], ymm8
        vmovups   YMMWORD PTR [432+rsp], ymm7
        vmovups   YMMWORD PTR [272+rsp], ymm6
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [175+rsp]
        vmovdqa   ymm4, ymm0
        and       r13, -64
        vmovupd   ymm0, YMMWORD PTR [__svml_dsinh_ha_data_internal+1408]
        vmovupd   ymm8, YMMWORD PTR [__svml_dsinh_ha_data_internal+1216]
        vmovupd   ymm9, YMMWORD PTR [__svml_dsinh_ha_data_internal+512]
        vmovupd   ymm1, YMMWORD PTR [__svml_dsinh_ha_data_internal+1152]
        vmovupd   ymm2, YMMWORD PTR [__svml_dsinh_ha_data_internal+1088]
        vandpd    ymm3, ymm0, ymm4
        vxorpd    ymm7, ymm3, ymm4
        vfmadd213pd ymm8, ymm7, ymm9
        vcmpeqpd  ymm6, ymm8, YMMWORD PTR [__svml_dsinh_ha_data_internal+576]
        mov       QWORD PTR [536+rsp], r13
        vextracti128 xmm5, ymm7, 1
        vshufps   xmm14, xmm7, xmm5, 221
        vpcmpgtd  xmm15, xmm14, XMMWORD PTR [__svml_dsinh_ha_data_internal+640]
        vmovmskps r8d, xmm15
        vblendvpd ymm15, ymm8, ymm9, ymm6
        vandps    ymm11, ymm15, YMMWORD PTR [__svml_dsinh_ha_data_internal+704]
        test      r8d, r8d
        vpsllq    ymm5, ymm11, 3
        vsubpd    ymm9, ymm15, ymm9
        vfnmadd231pd ymm7, ymm9, YMMWORD PTR [__svml_dsinh_ha_data_internal+1280]
        vfnmadd231pd ymm7, ymm9, YMMWORD PTR [__svml_dsinh_ha_data_internal+1344]
        vxorps    ymm11, ymm15, ymm11
        vpsllq    ymm9, ymm11, 48
        vextracti128 xmm8, ymm5, 1
        vmovd     ecx, xmm5
        vmovd     r9d, xmm8
        movsxd    rcx, ecx
        vpextrd   eax, xmm5, 2
        movsxd    r9, r9d
        vpextrd   r10d, xmm8, 2
        movsxd    rax, eax
        movsxd    r10, r10d
        vmovsd    xmm10, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+rdx+rcx]
        vmovsd    xmm6, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+rdx+r9]
        vmovsd    xmm8, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+rdx+rcx]
        vmovhpd   xmm14, xmm10, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+rdx+rax]
        vmovhpd   xmm10, xmm6, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+rdx+r10]
        vmovhpd   xmm6, xmm8, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+rdx+rax]
        vmovsd    xmm5, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+rdx+r9]
        vmovsd    xmm8, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+rdx+rcx]
        vinsertf128 ymm10, ymm14, xmm10, 1
        vmovhpd   xmm14, xmm5, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+rdx+r10]
        vmovhpd   xmm5, xmm8, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+rdx+rax]
        vmovsd    xmm8, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+rdx+r9]
        vpaddq    ymm10, ymm10, ymm9
        vinsertf128 ymm14, ymm6, xmm14, 1
        vmovhpd   xmm6, xmm8, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+rdx+r10]
        vmovsd    xmm8, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+rdx+rcx]
        vmovhpd   xmm8, xmm8, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+rdx+rax]
        vpsubq    ymm11, ymm14, ymm9
        vsubpd    ymm15, ymm10, ymm11
        vaddpd    ymm14, ymm10, ymm11
        vinsertf128 ymm5, ymm5, xmm6, 1
        vmovsd    xmm6, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+rdx+r9]
        vmovhpd   xmm6, xmm6, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+rdx+r10]
        vpaddq    ymm5, ymm5, ymm9
        vinsertf128 ymm6, ymm8, xmm6, 1
        vmulpd    ymm8, ymm7, ymm7
        vfmadd213pd ymm1, ymm8, YMMWORD PTR [__svml_dsinh_ha_data_internal+1024]
        vfmadd213pd ymm2, ymm8, YMMWORD PTR [__svml_dsinh_ha_data_internal+960]
        vfmadd213pd ymm1, ymm8, YMMWORD PTR [__svml_dsinh_ha_data_internal+896]
        vfmadd213pd ymm2, ymm8, YMMWORD PTR [__svml_dsinh_ha_data_internal+832]
        vfmadd213pd ymm1, ymm8, YMMWORD PTR [__svml_dsinh_ha_data_internal+768]
        vmulpd    ymm2, ymm8, ymm2
        vmulpd    ymm1, ymm8, ymm1
        vfmadd213pd ymm2, ymm7, ymm7
        vandnpd   ymm0, ymm0, ymm6
        vpsubq    ymm6, ymm6, ymm9
        vcmplt_oqpd ymm0, ymm0, ymm9
        vandnpd   ymm6, ymm0, ymm6
        vsubpd    ymm0, ymm15, ymm10
        vaddpd    ymm9, ymm11, ymm0
        vsubpd    ymm5, ymm5, ymm9
        vsubpd    ymm10, ymm5, ymm6
        vfmadd213pd ymm1, ymm15, ymm10
        vfmadd213pd ymm2, ymm14, ymm1
        vaddpd    ymm0, ymm15, ymm2
        vorpd     ymm0, ymm3, ymm0
        jne       _B7_3

_B7_2::

        vmovups   ymm6, YMMWORD PTR [272+rsp]
        vmovups   ymm7, YMMWORD PTR [432+rsp]
        vmovups   ymm8, YMMWORD PTR [368+rsp]
        vmovups   ymm9, YMMWORD PTR [304+rsp]
        vmovups   ymm10, YMMWORD PTR [400+rsp]
        vmovups   ymm11, YMMWORD PTR [336+rsp]
        vmovups   ymm14, YMMWORD PTR [496+rsp]
        vmovups   ymm15, YMMWORD PTR [464+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B7_3::

        vmovupd   YMMWORD PTR [r13], ymm4
        vmovupd   YMMWORD PTR [64+r13], ymm0

_B7_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [64+rsp], ymm12
        vmovups   YMMWORD PTR [32+rsp], ymm13
        mov       QWORD PTR [104+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [96+rsp], rsi
        mov       esi, r8d

_B7_7::

        bt        esi, ebx
        jc        _B7_10

_B7_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B7_7

_B7_9::

        vmovups   ymm12, YMMWORD PTR [64+rsp]
        vmovups   ymm13, YMMWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [104+rsp]
        mov       rsi, QWORD PTR [96+rsp]
        jmp       _B7_2

_B7_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dsinh_ha_cout_rare_internal
        jmp       _B7_8
        ALIGN     16

_B7_11::

__svml_sinh4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh4_ha_l9_B1_B3:
	DD	1335809
	DD	4379746
	DD	1140826
	DD	1800273
	DD	1542216
	DD	1284159
	DD	1681462
	DD	1423405
	DD	2091044
	DD	1964059
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_6
	DD	imagerel _unwind___svml_sinh4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh4_ha_l9_B6_B10:
	DD	530977
	DD	812058
	DD	865299
	DD	186382
	DD	313352
	DD	imagerel _B7_1
	DD	imagerel _B7_6
	DD	imagerel _unwind___svml_sinh4_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_6
	DD	imagerel _B7_11
	DD	imagerel _unwind___svml_sinh4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST7:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinh2_ha_e9

__svml_sinh2_ha_e9	PROC	EXPORT

_B8_1::

        DB        243
        DB        15
        DB        30
        DB        250
L164::

        sub       rsp, 312
        vmovapd   xmm2, xmm0
        vmovups   XMMWORD PTR [224+rsp], xmm14
        lea       r8, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [240+rsp], xmm13
        vmovups   XMMWORD PTR [256+rsp], xmm12
        vmovups   XMMWORD PTR [272+rsp], xmm11
        vmovups   XMMWORD PTR [208+rsp], xmm7
        vmovups   XMMWORD PTR [192+rsp], xmm6
        mov       QWORD PTR [288+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovupd   xmm0, XMMWORD PTR [__svml_dsinh_ha_data_internal+1408]
        and       r13, -64
        vandpd    xmm1, xmm0, xmm2
        vxorpd    xmm7, xmm1, xmm2
        vmulpd    xmm5, xmm7, XMMWORD PTR [__svml_dsinh_ha_data_internal+1216]
        vmovupd   xmm6, XMMWORD PTR [__svml_dsinh_ha_data_internal+512]
        vaddpd    xmm5, xmm6, xmm5
        vcmpeqpd  xmm13, xmm5, XMMWORD PTR [__svml_dsinh_ha_data_internal+576]
        vblendvpd xmm13, xmm5, xmm6, xmm13
        vmovq     xmm3, QWORD PTR [__svml_dsinh_ha_data_internal+640]
        vpand     xmm12, xmm13, XMMWORD PTR [__svml_dsinh_ha_data_internal+704]
        vpshufd   xmm4, xmm7, 221
        vpcmpgtd  xmm14, xmm4, xmm3
        vpsllq    xmm4, xmm12, 3
        vmovd     eax, xmm4
        vpxor     xmm12, xmm13, xmm12
        vmovmskps edx, xmm14
        movsxd    rax, eax
        vpextrd   ecx, xmm4, 2
        movsxd    rcx, ecx
        mov       QWORD PTR [296+rsp], r13
        vmovsd    xmm3, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+r8+rax]
        vmovsd    xmm5, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+r8+rax]
        vmovhpd   xmm11, xmm3, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+r8+rcx]
        vmovhpd   xmm3, xmm5, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+128+r8+rcx]
        vsubpd    xmm5, xmm13, xmm6
        vmulpd    xmm6, xmm5, XMMWORD PTR [__svml_dsinh_ha_data_internal+1280]
        vmulpd    xmm5, xmm5, XMMWORD PTR [__svml_dsinh_ha_data_internal+1344]
        vsubpd    xmm7, xmm7, xmm6
        vmovsd    xmm14, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+r8+rax]
        vmovhpd   xmm14, xmm14, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+256+r8+rcx]
        vsubpd    xmm5, xmm7, xmm5
        vpsllq    xmm7, xmm12, 48
        vpaddq    xmm11, xmm11, xmm7
        vpsubq    xmm12, xmm14, xmm7
        vpaddq    xmm3, xmm3, xmm7
        vsubpd    xmm14, xmm11, xmm12
        vaddpd    xmm13, xmm11, xmm12
        vmulpd    xmm6, xmm5, xmm5
        vsubpd    xmm11, xmm14, xmm11
        vmovsd    xmm4, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+r8+rax]
        vmovhpd   xmm4, xmm4, QWORD PTR [imagerel(__svml_dsinh_ha_data_internal)+384+r8+rcx]
        vandnpd   xmm0, xmm0, xmm4
        vcmpltpd  xmm0, xmm0, xmm7
        vpsubq    xmm7, xmm4, xmm7
        vandnpd   xmm0, xmm0, xmm7
        vaddpd    xmm7, xmm12, xmm11
        vsubpd    xmm4, xmm3, xmm7
        vmulpd    xmm3, xmm6, XMMWORD PTR [__svml_dsinh_ha_data_internal+1152]
        vsubpd    xmm7, xmm4, xmm0
        vmulpd    xmm0, xmm6, XMMWORD PTR [__svml_dsinh_ha_data_internal+1088]
        vaddpd    xmm11, xmm0, XMMWORD PTR [__svml_dsinh_ha_data_internal+960]
        vaddpd    xmm0, xmm3, XMMWORD PTR [__svml_dsinh_ha_data_internal+1024]
        vmulpd    xmm12, xmm6, xmm11
        vmulpd    xmm3, xmm6, xmm0
        vaddpd    xmm4, xmm12, XMMWORD PTR [__svml_dsinh_ha_data_internal+832]
        vaddpd    xmm11, xmm3, XMMWORD PTR [__svml_dsinh_ha_data_internal+896]
        vmulpd    xmm4, xmm6, xmm4
        vmulpd    xmm12, xmm6, xmm11
        vaddpd    xmm0, xmm12, XMMWORD PTR [__svml_dsinh_ha_data_internal+768]
        vmulpd    xmm6, xmm6, xmm0
        vmulpd    xmm0, xmm5, xmm13
        vmulpd    xmm5, xmm4, xmm0
        vmulpd    xmm4, xmm14, xmm6
        vaddpd    xmm13, xmm7, xmm5
        vaddpd    xmm3, xmm13, xmm4
        vaddpd    xmm5, xmm0, xmm3
        vaddpd    xmm14, xmm14, xmm5
        vorpd     xmm0, xmm1, xmm14
        and       edx, 3
        jne       _B8_3

_B8_2::

        vmovups   xmm6, XMMWORD PTR [192+rsp]
        vmovups   xmm7, XMMWORD PTR [208+rsp]
        vmovups   xmm11, XMMWORD PTR [272+rsp]
        vmovups   xmm12, XMMWORD PTR [256+rsp]
        vmovups   xmm13, XMMWORD PTR [240+rsp]
        vmovups   xmm14, XMMWORD PTR [224+rsp]
        mov       r13, QWORD PTR [288+rsp]
        add       rsp, 312
        ret

_B8_3::

        vmovupd   XMMWORD PTR [r13], xmm2
        vmovupd   XMMWORD PTR [64+r13], xmm0
        je        _B8_2

_B8_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B8_7::

        bt        esi, ebx
        jc        _B8_10

_B8_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B8_7

_B8_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B8_2

_B8_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dsinh_ha_cout_rare_internal
        jmp       _B8_8
        ALIGN     16

_B8_11::

__svml_sinh2_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh2_ha_e9_B1_B3:
	DD	1070081
	DD	2413652
	DD	813132
	DD	882755
	DD	1161274
	DD	1099825
	DD	1038376
	DD	976920
	DD	2556171

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_1
	DD	imagerel _B8_6
	DD	imagerel _unwind___svml_sinh2_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh2_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B8_1
	DD	imagerel _B8_6
	DD	imagerel _unwind___svml_sinh2_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_6
	DD	imagerel _B8_11
	DD	imagerel _unwind___svml_sinh2_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST8:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinh8_ha_z0

__svml_sinh8_ha_z0	PROC	EXPORT

_B9_1::

        DB        243
        DB        15
        DB        30
        DB        250
L183::

        sub       rsp, 1336
        vmovups   ZMMWORD PTR [1136+rsp], zmm9
        vmovups   ZMMWORD PTR [1200+rsp], zmm8
        vmovups   ZMMWORD PTR [1264+rsp], zmm6
        mov       QWORD PTR [1128+rsp], r13
        lea       r13, QWORD PTR [991+rsp]
        vmovups   zmm2, ZMMWORD PTR [__svml_dsinh_ha_data_internal+1408]
        and       r13, -64
        vmovups   zmm26, ZMMWORD PTR [__svml_dsinh_ha_data_internal+1216]
        vmovups   zmm27, ZMMWORD PTR [__svml_dsinh_ha_data_internal+512]
        vmovups   zmm31, ZMMWORD PTR [__svml_dsinh_ha_data_internal+576]
        vmovups   zmm29, ZMMWORD PTR [__svml_dsinh_ha_data_internal]
        vmovups   zmm30, ZMMWORD PTR [__svml_dsinh_ha_data_internal+256]
        vmovups   zmm25, ZMMWORD PTR [__svml_dsinh_ha_data_internal+1024]
        vmovups   zmm4, ZMMWORD PTR [__svml_dsinh_ha_data_internal+1152]
        vmovups   zmm3, ZMMWORD PTR [__svml_dsinh_ha_data_internal+1088]
        vmovups   zmm6, ZMMWORD PTR [__svml_dsinh_ha_data_internal+896]
        vmovups   zmm24, ZMMWORD PTR [__svml_dsinh_ha_data_internal+960]
        vmovups   zmm8, ZMMWORD PTR [__svml_dsinh_ha_data_internal+768]
        vmovups   zmm5, ZMMWORD PTR [__svml_dsinh_ha_data_internal+832]
        vmovaps   zmm23, zmm0
        vandpd    zmm22, zmm2, zmm23
        vxorpd    zmm9, zmm22, zmm23
        vfmadd213pd zmm26, zmm9, zmm27 {rn-sae}
        vpsrlq    zmm28, zmm9, 32
        vcmppd    k1, zmm31, zmm26, 0 {sae}
        vpmovqd   ymm0, zmm28
        vblendmpd zmm28{k1}, zmm26, zmm27
        vmovups   zmm26, ZMMWORD PTR [__svml_dsinh_ha_data_internal+384]
        vpcmpgtd  ymm1, ymm0, YMMWORD PTR [__svml_dsinh_ha_data_internal+640]
        vmovmskps r8d, ymm1
        vmovups   zmm1, ZMMWORD PTR [__svml_dsinh_ha_data_internal+128]
        mov       QWORD PTR [1328+rsp], r13
        vpandq    zmm0, zmm28, ZMMWORD PTR [__svml_dsinh_ha_data_internal+704]
        vpxorq    zmm31, zmm28, zmm0
        vpermt2pd zmm29, zmm0, ZMMWORD PTR [__svml_dsinh_ha_data_internal+64]
        vpermt2pd zmm30, zmm0, ZMMWORD PTR [__svml_dsinh_ha_data_internal+320]
        vpermt2pd zmm1, zmm0, ZMMWORD PTR [__svml_dsinh_ha_data_internal+192]
        vpermt2pd zmm26, zmm0, ZMMWORD PTR [__svml_dsinh_ha_data_internal+448]
        vsubpd    zmm0, zmm28, zmm27 {rn-sae}
        vmovups   zmm27, ZMMWORD PTR [__svml_dsinh_ha_data_internal+1280]
        vpsllq    zmm28, zmm31, 48
        vandnpd   zmm2, zmm2, zmm26
        vfnmadd231pd zmm9, zmm0, zmm27 {rn-sae}
        vmovups   zmm27, ZMMWORD PTR [__svml_dsinh_ha_data_internal+1344]
        vcmppd    k2, zmm2, zmm28, 17 {sae}
        vfnmadd231pd zmm9, zmm0, zmm27 {rn-sae}
        vpaddq    zmm29, zmm29, zmm28
        vpsubq    zmm30, zmm30, zmm28
        vpaddq    zmm1, zmm1, zmm28
        vpsubq    zmm26, zmm26, zmm28
        vsubpd    zmm0, zmm29, zmm30 {rn-sae}
        vaddpd    zmm31, zmm29, zmm30 {rn-sae}
        vxorpd    zmm26{k2}, zmm26, zmm26
        vsubpd    zmm2, zmm0, zmm29 {rn-sae}
        vmulpd    zmm27, zmm9, zmm9 {rn-sae}
        vaddpd    zmm29, zmm30, zmm2 {rn-sae}
        vfmadd231pd zmm25, zmm4, zmm27 {rn-sae}
        vfmadd231pd zmm24, zmm3, zmm27 {rn-sae}
        vsubpd    zmm1, zmm1, zmm29 {rn-sae}
        vfmadd213pd zmm25, zmm27, zmm6 {rn-sae}
        vfmadd213pd zmm24, zmm27, zmm5 {rn-sae}
        vsubpd    zmm30, zmm1, zmm26 {rn-sae}
        vfmadd213pd zmm25, zmm27, zmm8 {rn-sae}
        vmulpd    zmm3, zmm24, zmm27 {rn-sae}
        vmulpd    zmm25, zmm25, zmm27 {rn-sae}
        vfmadd213pd zmm3, zmm9, zmm9 {rn-sae}
        vfmadd213pd zmm25, zmm0, zmm30 {rn-sae}
        vfmadd213pd zmm3, zmm31, zmm25 {rn-sae}
        vaddpd    zmm4, zmm3, zmm0 {rn-sae}
        vorpd     zmm0, zmm22, zmm4
        test      r8d, r8d
        jne       _B9_3

_B9_2::

        vmovups   zmm6, ZMMWORD PTR [1264+rsp]
        vmovups   zmm8, ZMMWORD PTR [1200+rsp]
        vmovups   zmm9, ZMMWORD PTR [1136+rsp]
        mov       r13, QWORD PTR [1128+rsp]
        add       rsp, 1336
        ret

_B9_3::

        vstmxcsr  DWORD PTR [1120+rsp]

_B9_4::

        movzx     edx, WORD PTR [1120+rsp]
        mov       eax, edx
        or        eax, 8064
        cmp       edx, eax
        je        _B9_6

_B9_5::

        mov       DWORD PTR [1120+rsp], eax
        vldmxcsr  DWORD PTR [1120+rsp]

_B9_6::

        vmovups   ZMMWORD PTR [r13], zmm23
        vmovups   ZMMWORD PTR [64+r13], zmm0
        test      r8d, r8d
        jne       _B9_11

_B9_7::

        cmp       edx, eax
        je        _B9_2

_B9_8::

        vstmxcsr  DWORD PTR [1120+rsp]
        mov       eax, DWORD PTR [1120+rsp]

_B9_9::

        and       eax, -8065
        or        eax, edx
        mov       DWORD PTR [1120+rsp], eax
        vldmxcsr  DWORD PTR [1120+rsp]
        jmp       _B9_2

_B9_11::

        xor       ecx, ecx
        kmovw     WORD PTR [888+rsp], k4
        kmovw     WORD PTR [880+rsp], k5
        kmovw     WORD PTR [872+rsp], k6
        kmovw     WORD PTR [864+rsp], k7
        vmovups   ZMMWORD PTR [800+rsp], zmm7
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
        mov       QWORD PTR [912+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [904+rsp], rsi
        mov       esi, edx
        mov       QWORD PTR [896+rsp], rdi
        mov       edi, r8d
        mov       QWORD PTR [920+rsp], rbp
        mov       ebp, eax

_B9_12::

        bt        edi, ebx
        jc        _B9_15

_B9_13::

        inc       ebx
        cmp       ebx, 8
        jl        _B9_12

_B9_14::

        kmovw     k4, WORD PTR [888+rsp]
        mov       eax, ebp
        kmovw     k5, WORD PTR [880+rsp]
        kmovw     k6, WORD PTR [872+rsp]
        kmovw     k7, WORD PTR [864+rsp]
        vmovups   zmm7, ZMMWORD PTR [800+rsp]
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
        mov       rbx, QWORD PTR [912+rsp]
        mov       edx, esi
        mov       rsi, QWORD PTR [904+rsp]
        mov       rdi, QWORD PTR [896+rsp]
        mov       rbp, QWORD PTR [920+rsp]
        jmp       _B9_7

_B9_15::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dsinh_ha_cout_rare_internal
        jmp       _B9_13
        ALIGN     16

_B9_16::

__svml_sinh8_ha_z0 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh8_ha_z0_B1_B9:
	DD	668673
	DD	9294900
	DD	5204012
	DD	4950049
	DD	4691990
	DD	10944779

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_1
	DD	imagerel _B9_11
	DD	imagerel _unwind___svml_sinh8_ha_z0_B1_B9

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinh8_ha_z0_B11_B15:
	DD	2808865
	DD	7558364
	DD	7369937
	DD	7431367
	DD	7484605
	DD	153781
	DD	411818
	DD	669855
	DD	927892
	DD	1185929
	DD	1443966
	DD	1767539
	DD	2025576
	DD	2283613
	DD	2541650
	DD	2799687
	DD	3057724
	DD	3307569
	DD	7109414
	DD	7170845
	DD	7232276
	DD	7293707
	DD	imagerel _B9_1
	DD	imagerel _B9_11
	DD	imagerel _unwind___svml_sinh8_ha_z0_B1_B9

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_11
	DD	imagerel _B9_16
	DD	imagerel _unwind___svml_sinh8_ha_z0_B11_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST9:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_dsinh_ha_cout_rare_internal

__svml_dsinh_ha_cout_rare_internal	PROC

_B10_1::

        DB        243
        DB        15
        DB        30
        DB        250
L234::

        push      r14
        sub       rsp, 224
        xor       eax, eax
        movzx     r8d, WORD PTR [6+rcx]
        and       r8d, 32752
        shr       r8d, 4
        movsd     xmm2, QWORD PTR [rcx]
        mov       r14b, BYTE PTR [7+rcx]
        movups    XMMWORD PTR [80+rsp], xmm15
        movups    XMMWORD PTR [96+rsp], xmm14
        movups    XMMWORD PTR [112+rsp], xmm13
        movups    XMMWORD PTR [128+rsp], xmm12
        movups    XMMWORD PTR [144+rsp], xmm11
        movups    XMMWORD PTR [160+rsp], xmm10
        movups    XMMWORD PTR [176+rsp], xmm9
        movups    XMMWORD PTR [192+rsp], xmm8
        movsd     QWORD PTR [208+rsp], xmm2
        cmp       r8d, 2047
        je        _B10_17

_B10_2::

        test      r8d, r8d
        jle       _B10_16

_B10_3::

        mov       cl, r14b
        and       cl, 127
        movsd     QWORD PTR [56+rsp], xmm2
        mov       BYTE PTR [63+rsp], cl
        cmp       r8d, 969
        jle       _B10_14

_B10_4::

        movsd     xmm0, QWORD PTR [56+rsp]
        movsd     xmm1, QWORD PTR [_vmldSinhHATab+1136]
        comisd    xmm1, xmm0
        jbe       _B10_13

_B10_5::

        movsd     xmm1, QWORD PTR [_vmldSinhHATab+1184]
        comisd    xmm1, xmm0
        jbe       _B10_9

_B10_6::

        comisd    xmm0, QWORD PTR [_vmldSinhHATab+1176]
        jb        _B10_8

_B10_7::

        movsd     xmm8, QWORD PTR [_vmldSinhHATab+1112]
        and       r14b, -128
        mulsd     xmm8, xmm0
        movsd     xmm5, QWORD PTR [_vmldSinhHATab+1144]
        mov       rcx, QWORD PTR [_vmldSinhHATab+8]
        mov       r9, rcx
        shr       r9, 48
        addsd     xmm8, QWORD PTR [_vmldSinhHATab+1120]
        movsd     QWORD PTR [48+rsp], xmm8
        and       r9d, -32753
        movsd     xmm10, QWORD PTR [48+rsp]
        mov       r11d, DWORD PTR [48+rsp]
        mov       r8d, r11d
        shr       r8d, 6
        and       r11d, 63
        mov       QWORD PTR [72+rsp], rcx
        subsd     xmm10, QWORD PTR [_vmldSinhHATab+1120]
        mulsd     xmm5, xmm10
        lea       r10d, DWORD PTR [1023+r8]
        xorps     xmm10, XMMWORD PTR [_2il0floatpacket_105]
        add       r8d, 1022
        mulsd     xmm10, QWORD PTR [_vmldSinhHATab+1152]
        subsd     xmm0, xmm5
        movaps    xmm2, xmm0
        movaps    xmm4, xmm0
        movsd     xmm5, QWORD PTR [_vmldSinhHATab+1128]
        and       r8d, 2047
        shl       r8d, 4
        lea       ecx, DWORD PTR [r11+r11]
        or        r9d, r8d
        lea       r8, QWORD PTR [__ImageBase]
        neg       r10d
        lea       r11d, DWORD PTR [1+r11+r11]
        add       r10d, -4
        addsd     xmm2, xmm10
        movsd     QWORD PTR [64+rsp], xmm2
        and       r10d, 2047
        movsd     xmm9, QWORD PTR [64+rsp]
        mov       WORD PTR [78+rsp], r9w
        and       r9d, -32753
        shl       r10d, 4
        subsd     xmm4, xmm9
        movsd     QWORD PTR [32+rsp], xmm4
        or        r9d, r10d
        movsd     xmm3, QWORD PTR [64+rsp]
        movsd     xmm1, QWORD PTR [32+rsp]
        addsd     xmm3, xmm1
        movsd     QWORD PTR [40+rsp], xmm3
        movsd     xmm14, QWORD PTR [32+rsp]
        addsd     xmm10, xmm14
        movsd     xmm14, QWORD PTR [72+rsp]
        movsd     QWORD PTR [32+rsp], xmm10
        movsd     xmm15, QWORD PTR [40+rsp]
        mov       WORD PTR [78+rsp], r9w
        subsd     xmm0, xmm15
        movsd     QWORD PTR [40+rsp], xmm0
        mov       r9, r8
        movsd     xmm8, QWORD PTR [32+rsp]
        movsd     xmm0, QWORD PTR [40+rsp]
        movsd     xmm10, QWORD PTR [72+rsp]
        addsd     xmm8, xmm0
        movsd     QWORD PTR [40+rsp], xmm8
        movsd     xmm3, QWORD PTR [64+rsp]
        mulsd     xmm5, xmm3
        movaps    xmm13, xmm3
        mulsd     xmm13, xmm3
        movsd     xmm1, QWORD PTR [40+rsp]
        movaps    xmm4, xmm3
        movsd     QWORD PTR [64+rsp], xmm5
        movsd     xmm2, QWORD PTR [64+rsp]
        movsd     xmm0, QWORD PTR [_vmldSinhHATab+1072]
        subsd     xmm2, xmm3
        mulsd     xmm0, xmm13
        movsd     QWORD PTR [32+rsp], xmm2
        movsd     xmm11, QWORD PTR [64+rsp]
        movsd     xmm12, QWORD PTR [32+rsp]
        subsd     xmm11, xmm12
        movsd     xmm12, QWORD PTR [imagerel(_vmldSinhHATab)+r8+r11*8]
        mulsd     xmm12, xmm14
        addsd     xmm0, QWORD PTR [_vmldSinhHATab+1056]
        movsd     QWORD PTR [64+rsp], xmm11
        movsd     xmm9, QWORD PTR [64+rsp]
        mulsd     xmm0, xmm13
        subsd     xmm4, xmm9
        movsd     QWORD PTR [32+rsp], xmm4
        movsd     xmm4, QWORD PTR [_vmldSinhHATab+1064]
        mulsd     xmm4, xmm13
        addsd     xmm0, QWORD PTR [_vmldSinhHATab+1040]
        mulsd     xmm0, xmm13
        addsd     xmm4, QWORD PTR [_vmldSinhHATab+1048]
        mulsd     xmm4, xmm13
        movsd     xmm13, QWORD PTR [imagerel(_vmldSinhHATab)+r8+rcx*8]
        neg       ecx
        mulsd     xmm13, xmm14
        mulsd     xmm4, xmm3
        movaps    xmm8, xmm13
        lea       r10d, DWORD PTR [128+rcx]
        movsd     xmm11, QWORD PTR [imagerel(_vmldSinhHATab)+r8+r10*8]
        add       ecx, 129
        mulsd     xmm11, xmm10
        movsd     xmm5, QWORD PTR [64+rsp]
        subsd     xmm8, xmm11
        movsd     xmm2, QWORD PTR [32+rsp]
        movsd     QWORD PTR [64+rsp], xmm8
        movaps    xmm8, xmm13
        movsd     xmm9, QWORD PTR [64+rsp]
        movsd     xmm15, QWORD PTR [imagerel(_vmldSinhHATab)+r9+rcx*8]
        subsd     xmm8, xmm9
        mulsd     xmm15, xmm10
        subsd     xmm8, xmm11
        movsd     QWORD PTR [32+rsp], xmm8
        movsd     xmm9, QWORD PTR [64+rsp]
        movsd     xmm8, QWORD PTR [32+rsp]
        movaps    xmm14, xmm9
        subsd     xmm8, xmm15
        addsd     xmm8, xmm12
        addsd     xmm12, xmm15
        addsd     xmm14, xmm8
        movsd     QWORD PTR [64+rsp], xmm14
        movaps    xmm14, xmm13
        movsd     xmm10, QWORD PTR [64+rsp]
        addsd     xmm14, xmm11
        subsd     xmm9, xmm10
        addsd     xmm9, xmm8
        movsd     QWORD PTR [32+rsp], xmm9
        movsd     xmm8, QWORD PTR [64+rsp]
        movsd     xmm9, QWORD PTR [32+rsp]
        movsd     QWORD PTR [64+rsp], xmm14
        movsd     xmm10, QWORD PTR [64+rsp]
        subsd     xmm13, xmm10
        addsd     xmm13, xmm11
        movsd     QWORD PTR [32+rsp], xmm13
        movsd     xmm13, QWORD PTR [64+rsp]
        movsd     xmm11, QWORD PTR [32+rsp]
        addsd     xmm11, xmm12
        movaps    xmm12, xmm13
        addsd     xmm12, xmm11
        movsd     QWORD PTR [64+rsp], xmm12
        movsd     xmm15, QWORD PTR [64+rsp]
        movsd     xmm12, QWORD PTR [_vmldSinhHATab+1128]
        subsd     xmm13, xmm15
        addsd     xmm13, xmm11
        movsd     QWORD PTR [32+rsp], xmm13
        movsd     xmm14, QWORD PTR [64+rsp]
        mulsd     xmm12, xmm14
        movsd     xmm13, QWORD PTR [32+rsp]
        movsd     QWORD PTR [64+rsp], xmm12
        movsd     xmm10, QWORD PTR [64+rsp]
        mulsd     xmm3, xmm13
        subsd     xmm10, xmm14
        movsd     QWORD PTR [32+rsp], xmm10
        movaps    xmm10, xmm1
        movsd     xmm11, QWORD PTR [64+rsp]
        movsd     xmm15, QWORD PTR [32+rsp]
        mulsd     xmm10, xmm13
        subsd     xmm11, xmm15
        mulsd     xmm1, xmm14
        movaps    xmm15, xmm13
        mulsd     xmm15, xmm4
        mulsd     xmm4, xmm14
        addsd     xmm10, xmm15
        movaps    xmm15, xmm9
        mulsd     xmm15, xmm0
        mulsd     xmm0, xmm8
        addsd     xmm10, xmm15
        movsd     QWORD PTR [64+rsp], xmm11
        movaps    xmm11, xmm14
        movsd     xmm12, QWORD PTR [64+rsp]
        addsd     xmm10, xmm3
        subsd     xmm11, xmm12
        addsd     xmm10, xmm1
        movaps    xmm1, xmm4
        addsd     xmm9, xmm10
        addsd     xmm1, xmm0
        movsd     QWORD PTR [32+rsp], xmm11
        movsd     xmm12, QWORD PTR [64+rsp]
        movsd     xmm11, QWORD PTR [32+rsp]
        movsd     QWORD PTR [64+rsp], xmm1
        movsd     xmm3, QWORD PTR [64+rsp]
        subsd     xmm4, xmm3
        addsd     xmm4, xmm0
        movsd     QWORD PTR [32+rsp], xmm4
        movsd     xmm1, QWORD PTR [64+rsp]
        movsd     xmm4, QWORD PTR [32+rsp]
        addsd     xmm4, xmm9
        movaps    xmm9, xmm2
        mulsd     xmm9, xmm11
        mulsd     xmm11, xmm5
        mulsd     xmm2, xmm12
        addsd     xmm4, xmm9
        mulsd     xmm5, xmm12
        addsd     xmm4, xmm11
        addsd     xmm4, xmm2
        movaps    xmm2, xmm5
        addsd     xmm2, xmm1
        movsd     QWORD PTR [64+rsp], xmm2
        movsd     xmm0, QWORD PTR [64+rsp]
        subsd     xmm5, xmm0
        addsd     xmm5, xmm1
        movsd     QWORD PTR [32+rsp], xmm5
        movaps    xmm5, xmm8
        movsd     xmm2, QWORD PTR [64+rsp]
        movsd     xmm10, QWORD PTR [32+rsp]
        addsd     xmm5, xmm2
        addsd     xmm10, xmm4
        movsd     QWORD PTR [64+rsp], xmm5
        movsd     xmm0, QWORD PTR [64+rsp]
        subsd     xmm8, xmm0
        addsd     xmm8, xmm2
        movsd     QWORD PTR [32+rsp], xmm8
        movsd     xmm3, QWORD PTR [64+rsp]
        movsd     xmm8, QWORD PTR [32+rsp]
        addsd     xmm8, xmm10
        addsd     xmm8, xmm3
        movsd     QWORD PTR [56+rsp], xmm8
        mov       r11b, BYTE PTR [63+rsp]
        and       r11b, 127
        or        r11b, r14b
        mov       BYTE PTR [63+rsp], r11b
        mov       r14, QWORD PTR [56+rsp]
        mov       QWORD PTR [rdx], r14
        jmp       _B10_15

_B10_8::

        movaps    xmm2, xmm0
        mulsd     xmm2, xmm0
        movsd     xmm1, QWORD PTR [_vmldSinhHATab+1104]
        mulsd     xmm1, xmm2
        mov       cl, BYTE PTR [215+rsp]
        and       cl, -128
        addsd     xmm1, QWORD PTR [_vmldSinhHATab+1096]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_vmldSinhHATab+1088]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_vmldSinhHATab+1080]
        mulsd     xmm2, xmm1
        mulsd     xmm2, xmm0
        addsd     xmm0, xmm2
        movsd     QWORD PTR [56+rsp], xmm0
        mov       r8b, BYTE PTR [63+rsp]
        and       r8b, 127
        or        r8b, cl
        mov       BYTE PTR [63+rsp], r8b
        mov       r9, QWORD PTR [56+rsp]
        mov       QWORD PTR [rdx], r9
        jmp       _B10_15

_B10_9::

        movsd     xmm1, QWORD PTR [_vmldSinhHATab+1112]
        lea       r9, QWORD PTR [__ImageBase]
        mulsd     xmm1, xmm0
        mov       r14, r9
        movsd     xmm2, QWORD PTR [_vmldSinhHATab+1144]
        movsd     xmm3, QWORD PTR [_vmldSinhHATab+1152]
        mov       rcx, QWORD PTR [_vmldSinhHATab+8]
        mov       QWORD PTR [72+rsp], rcx
        addsd     xmm1, QWORD PTR [_vmldSinhHATab+1120]
        movsd     QWORD PTR [48+rsp], xmm1
        movsd     xmm4, QWORD PTR [48+rsp]
        movsd     xmm1, QWORD PTR [_vmldSinhHATab+1072]
        mov       ecx, DWORD PTR [48+rsp]
        mov       r10d, ecx
        and       r10d, 63
        subsd     xmm4, QWORD PTR [_vmldSinhHATab+1120]
        mulsd     xmm2, xmm4
        lea       r8d, DWORD PTR [r10+r10]
        mulsd     xmm4, xmm3
        subsd     xmm0, xmm2
        shr       ecx, 6
        lea       r11d, DWORD PTR [1+r10+r10]
        add       ecx, 1022
        subsd     xmm0, xmm4
        mulsd     xmm1, xmm0
        and       ecx, 2047
        addsd     xmm1, QWORD PTR [_vmldSinhHATab+1064]
        mulsd     xmm1, xmm0
        addsd     xmm1, QWORD PTR [_vmldSinhHATab+1056]
        mulsd     xmm1, xmm0
        addsd     xmm1, QWORD PTR [_vmldSinhHATab+1048]
        mulsd     xmm1, xmm0
        addsd     xmm1, QWORD PTR [_vmldSinhHATab+1040]
        mulsd     xmm1, xmm0
        mulsd     xmm1, xmm0
        addsd     xmm1, xmm0
        movsd     xmm0, QWORD PTR [imagerel(_vmldSinhHATab)+r9+r8*8]
        mulsd     xmm1, xmm0
        addsd     xmm1, QWORD PTR [imagerel(_vmldSinhHATab)+r14+r11*8]
        addsd     xmm1, xmm0
        cmp       ecx, 2046
        ja        _B10_11

_B10_10::

        mov       r8, QWORD PTR [_vmldSinhHATab+8]
        shr       r8, 48
        shl       ecx, 4
        and       r8d, -32753
        or        r8d, ecx
        mov       WORD PTR [78+rsp], r8w
        movsd     xmm0, QWORD PTR [72+rsp]
        mulsd     xmm1, xmm0
        movsd     QWORD PTR [56+rsp], xmm1
        jmp       _B10_12

_B10_11::

        dec       ecx
        and       ecx, 2047
        movzx     r8d, WORD PTR [78+rsp]
        shl       ecx, 4
        and       r8d, -32753
        or        r8d, ecx
        mov       WORD PTR [78+rsp], r8w
        movsd     xmm0, QWORD PTR [72+rsp]
        mulsd     xmm1, xmm0
        mulsd     xmm1, QWORD PTR [_vmldSinhHATab+1024]
        movsd     QWORD PTR [56+rsp], xmm1

_B10_12::

        mov       r8b, BYTE PTR [63+rsp]
        mov       cl, BYTE PTR [215+rsp]
        and       r8b, 127
        and       cl, -128
        or        r8b, cl
        mov       BYTE PTR [63+rsp], r8b
        mov       r9, QWORD PTR [56+rsp]
        mov       QWORD PTR [rdx], r9
        jmp       _B10_15

_B10_13::

        movsd     xmm0, QWORD PTR [_vmldSinhHATab+1168]
        mov       eax, 3
        mulsd     xmm0, xmm2
        movsd     QWORD PTR [rdx], xmm0
        jmp       _B10_15

_B10_14::

        movsd     xmm0, QWORD PTR [_vmldSinhHATab]
        addsd     xmm0, QWORD PTR [_vmldSinhHATab+1160]
        mulsd     xmm0, xmm2
        movsd     QWORD PTR [rdx], xmm0

_B10_15::

        movups    xmm8, XMMWORD PTR [192+rsp]
        movups    xmm9, XMMWORD PTR [176+rsp]
        movups    xmm10, XMMWORD PTR [160+rsp]
        movups    xmm11, XMMWORD PTR [144+rsp]
        movups    xmm12, XMMWORD PTR [128+rsp]
        movups    xmm13, XMMWORD PTR [112+rsp]
        movups    xmm14, XMMWORD PTR [96+rsp]
        movups    xmm15, XMMWORD PTR [80+rsp]
        add       rsp, 224
        pop       r14
        ret

_B10_16::

        movsd     xmm0, QWORD PTR [_vmldSinhHATab+1160]
        mulsd     xmm2, xmm0
        movsd     QWORD PTR [64+rsp], xmm2
        movsd     xmm1, QWORD PTR [64+rsp]
        mov       rcx, QWORD PTR [_vmldSinhHATab+8]
        mov       QWORD PTR [72+rsp], rcx
        addsd     xmm1, QWORD PTR [208+rsp]
        movsd     QWORD PTR [rdx], xmm1
        jmp       _B10_15

_B10_17::

        addsd     xmm2, xmm2
        movsd     QWORD PTR [rdx], xmm2
        jmp       _B10_15
        ALIGN     16

_B10_18::

__svml_dsinh_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_dsinh_ha_cout_rare_internal_B1_B17:
	DD	1271297
	DD	821350
	DD	759901
	DD	698452
	DD	637003
	DD	575554
	DD	514105
	DD	452659
	DD	391213
	DD	1835277
	DD	57350

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B10_1
	DD	imagerel _B10_18
	DD	imagerel _unwind___svml_dsinh_ha_cout_rare_internal_B1_B17

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_dsinh_ha_data_internal
__svml_dsinh_ha_data_internal	DD	0
	DD	1071644672
	DD	1828292879
	DD	1071691096
	DD	1014845819
	DD	1071739576
	DD	1853186616
	DD	1071790202
	DD	171030293
	DD	1071843070
	DD	1276261410
	DD	1071898278
	DD	3577096743
	DD	1071955930
	DD	3712504873
	DD	1072016135
	DD	1719614413
	DD	1072079006
	DD	1944781191
	DD	1072144660
	DD	1110089947
	DD	1072213221
	DD	2191782032
	DD	1072284817
	DD	2572866477
	DD	1072359583
	DD	3716502172
	DD	1072437659
	DD	3707479175
	DD	1072519192
	DD	2728693978
	DD	1072604335
	DD	0
	DD	0
	DD	1255956747
	DD	1015588398
	DD	3117910646
	DD	3161559105
	DD	3066496371
	DD	1015656574
	DD	3526460132
	DD	1014428778
	DD	300981948
	DD	1014684169
	DD	2951496418
	DD	1013793687
	DD	88491949
	DD	1015427660
	DD	330458198
	DD	3163282740
	DD	3993278767
	DD	3161724279
	DD	1451641639
	DD	1015474673
	DD	2960257726
	DD	1013742662
	DD	878562433
	DD	1015521741
	DD	2303740125
	DD	1014042725
	DD	3613079303
	DD	1014164738
	DD	396109971
	DD	3163462691
	DD	0
	DD	1071644672
	DD	2728693978
	DD	1071555759
	DD	3707479175
	DD	1071470616
	DD	3716502172
	DD	1071389083
	DD	2572866477
	DD	1071311007
	DD	2191782032
	DD	1071236241
	DD	1110089947
	DD	1071164645
	DD	1944781191
	DD	1071096084
	DD	1719614413
	DD	1071030430
	DD	3712504873
	DD	1070967559
	DD	3577096743
	DD	1070907354
	DD	1276261410
	DD	1070849702
	DD	171030293
	DD	1070794494
	DD	1853186616
	DD	1070741626
	DD	1014845819
	DD	1070691000
	DD	1828292879
	DD	1070642520
	DD	0
	DD	0
	DD	396109971
	DD	3162414115
	DD	3613079303
	DD	1013116162
	DD	2303740125
	DD	1012994149
	DD	878562433
	DD	1014473165
	DD	2960257726
	DD	1012694086
	DD	1451641639
	DD	1014426097
	DD	3993278767
	DD	3160675703
	DD	330458198
	DD	3162234164
	DD	88491949
	DD	1014379084
	DD	2951496418
	DD	1012745111
	DD	300981948
	DD	1013635593
	DD	3526460132
	DD	1013380202
	DD	3066496371
	DD	1014607998
	DD	3117910646
	DD	3160510529
	DD	1255956747
	DD	1014539822
	DD	0
	DD	1123549184
	DD	0
	DD	1123549184
	DD	0
	DD	1123549184
	DD	0
	DD	1123549184
	DD	0
	DD	1123549184
	DD	0
	DD	1123549184
	DD	0
	DD	1123549184
	DD	0
	DD	1123549184
	DD	1
	DD	1123549184
	DD	1
	DD	1123549184
	DD	1
	DD	1123549184
	DD	1
	DD	1123549184
	DD	1
	DD	1123549184
	DD	1
	DD	1123549184
	DD	1
	DD	1123549184
	DD	1
	DD	1123549184
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	1082453555
	DD	15
	DD	0
	DD	15
	DD	0
	DD	15
	DD	0
	DD	15
	DD	0
	DD	15
	DD	0
	DD	15
	DD	0
	DD	15
	DD	0
	DD	15
	DD	0
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
	DD	1431657638
	DD	1069897045
	DD	1431657638
	DD	1069897045
	DD	1431657638
	DD	1069897045
	DD	1431657638
	DD	1069897045
	DD	1431657638
	DD	1069897045
	DD	1431657638
	DD	1069897045
	DD	1431657638
	DD	1069897045
	DD	1431657638
	DD	1069897045
	DD	1431653196
	DD	1067799893
	DD	1431653196
	DD	1067799893
	DD	1431653196
	DD	1067799893
	DD	1431653196
	DD	1067799893
	DD	1431653196
	DD	1067799893
	DD	1431653196
	DD	1067799893
	DD	1431653196
	DD	1067799893
	DD	1431653196
	DD	1067799893
	DD	249327322
	DD	1065423121
	DD	249327322
	DD	1065423121
	DD	249327322
	DD	1065423121
	DD	249327322
	DD	1065423121
	DD	249327322
	DD	1065423121
	DD	249327322
	DD	1065423121
	DD	249327322
	DD	1065423121
	DD	249327322
	DD	1065423121
	DD	419584011
	DD	1062650220
	DD	419584011
	DD	1062650220
	DD	419584011
	DD	1062650220
	DD	419584011
	DD	1062650220
	DD	419584011
	DD	1062650220
	DD	419584011
	DD	1062650220
	DD	419584011
	DD	1062650220
	DD	419584011
	DD	1062650220
	DD	100753094
	DD	1059717741
	DD	100753094
	DD	1059717741
	DD	100753094
	DD	1059717741
	DD	100753094
	DD	1059717741
	DD	100753094
	DD	1059717741
	DD	100753094
	DD	1059717741
	DD	100753094
	DD	1059717741
	DD	100753094
	DD	1059717741
	DD	461398617
	DD	1056571820
	DD	461398617
	DD	1056571820
	DD	461398617
	DD	1056571820
	DD	461398617
	DD	1056571820
	DD	461398617
	DD	1056571820
	DD	461398617
	DD	1056571820
	DD	461398617
	DD	1056571820
	DD	461398617
	DD	1056571820
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
	DD	4277796864
	DD	1072049730
	DD	4277796864
	DD	1072049730
	DD	4277796864
	DD	1072049730
	DD	4277796864
	DD	1072049730
	DD	4277796864
	DD	1072049730
	DD	4277796864
	DD	1072049730
	DD	4277796864
	DD	1072049730
	DD	4277796864
	DD	1072049730
	DD	3164486458
	DD	1031600026
	DD	3164486458
	DD	1031600026
	DD	3164486458
	DD	1031600026
	DD	3164486458
	DD	1031600026
	DD	3164486458
	DD	1031600026
	DD	3164486458
	DD	1031600026
	DD	3164486458
	DD	1031600026
	DD	3164486458
	DD	1031600026
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
	DD	1072693248
	DD	1431655765
	DD	1069897045
	DD	286334782
	DD	1065423121
	DD	381082983
	DD	1059717536
	DD	2235373439
	DD	1053236796
	DD	971595069
	DD	1045573268
	DD	1394307292
	DD	1048884517
	DD	2766897055
	DD	997797591
	DD	0
	DD	1072693248
	DD	1431655765
	DD	1069897045
	DD	286333517
	DD	1065423121
	DD	407944761
	DD	1059717536
	DD	3876143447
	DD	1053236746
	DD	1085026835
	DD	1045917151
	DD	3838336303
	DD	1047437007
	DD	4128322810
	DD	1069959864
	DD	3949957238
	DD	1072709030
	DD	4128323011
	DD	1068911288
	DD	971639678
	DD	1069918089
	DD	2624579608
	DD	1065203958
	DD	822211925
	DD	1065439956
	DD	1030168058
	DD	1060091065
	DD	56186104
	DD	1059743035
	DD	1749136243
	DD	1070650064
	DD	3139812683
	DD	1072728870
	DD	1749135426
	DD	1069601488
	DD	1323087056
	DD	1069944542
	DD	904573411
	DD	1065774699
	DD	3069035452
	DD	1065461118
	DD	2025435543
	DD	1060629881
	DD	4176073593
	DD	1059774207
	DD	1719614413
	DD	1071030430
	DD	3437194458
	DD	1072756854
	DD	1719611085
	DD	1069981854
	DD	1719593135
	DD	1069981854
	DD	3767935290
	DD	1066281853
	DD	1746815117
	DD	1065490968
	DD	635147253
	DD	1061164846
	DD	4090120640
	DD	1059819266
	DD	2938050448
	DD	1071421935
	DD	1449319160
	DD	1072793193
	DD	2938047542
	DD	1070373359
	DD	3364062362
	DD	1070030305
	DD	1975058862
	DD	1066602826
	DD	2155069416
	DD	1065529729
	DD	450571058
	DD	1061443274
	DD	485907960
	DD	1059878574
	DD	2487351331
	DD	1071736096
	DD	1329190813
	DD	1072838159
	DD	2487349750
	DD	1070687520
	DD	1772223462
	DD	1070090260
	DD	3333182165
	DD	1066873216
	DD	1054730831
	DD	1065577693
	DD	284500812
	DD	1061731702
	DD	3231799560
	DD	1059950957
	DD	3200056266
	DD	1071947452
	DD	2107451043
	DD	1072892090
	DD	3200054203
	DD	1070898876
	DD	2809911099
	DD	1070162168
	DD	4289767168
	DD	1067155024
	DD	3501442301
	DD	1065635219
	DD	598006601
	DD	1062032296
	DD	2038415185
	DD	1060039020
	DD	0
	DD	1072168960
	DD	0
	DD	1072955392
	DD	415
	DD	1071120384
	DD	2863310733
	DD	1070246570
	DD	4273731122
	DD	1067450367
	DD	1446964110
	DD	1065702741
	DD	1489023795
	DD	1062277465
	DD	1171118343
	DD	1060126972
	DD	1102821844
	DD	1072402282
	DD	1478280716
	DD	1073028539
	DD	1102821797
	DD	1071353706
	DD	1971033038
	DD	1070344100
	DD	727485122
	DD	1067605916
	DD	845580336
	DD	1065780765
	DD	4255115201
	DD	1062443383
	DD	3620060089
	DD	1060186176
	DD	772585791
	DD	1072649172
	DD	4250734987
	DD	1073112081
	DD	772586472
	DD	1071600596
	DD	2804330614
	DD	1070455490
	DD	1931665979
	DD	1067770509
	DD	1452324392
	DD	1065869877
	DD	1391866018
	DD	1062618974
	DD	3013494930
	DD	1060254211
	DD	753980940
	DD	1072802366
	DD	2105245252
	DD	1073206647
	DD	753978640
	DD	1071753790
	DD	4238603591
	DD	1070581577
	DD	1034108977
	DD	1067945384
	DD	1560353218
	DD	1065970747
	DD	4175683972
	DD	1062805415
	DD	3375885113
	DD	1060329560
	DD	2578404435
	DD	1072942218
	DD	860824390
	DD	1073312946
	DD	2578404727
	DD	1071893642
	DD	3437195612
	DD	1070659702
	DD	557897458
	DD	1068131854
	DD	2035738069
	DD	1066084132
	DD	3990308259
	DD	1063004428
	DD	1264618213
	DD	1060417649
	DD	3884178326
	DD	1073091817
	DD	2630968863
	DD	1073431776
	DD	3884182706
	DD	1072043241
	DD	322344088
	DD	1070738923
	DD	3659529551
	DD	1068331319
	DD	3252816661
	DD	1066210884
	DD	3282148564
	DD	1063217362
	DD	698592167
	DD	1060515757
	DD	3625229515
	DD	1073252287
	DD	1520503441
	DD	1073564031
	DD	3625227566
	DD	1072203711
	DD	3876964346
	DD	1070827092
	DD	3857858996
	DD	1068522111
	DD	3302636947
	DD	1066351956
	DD	1934225563
	DD	1063350709
	DD	1172945773
	DD	1060620467
	DD	3580623448
	DD	1073424833
	DD	3834334903
	DD	1073710703
	DD	3580622127
	DD	1072376257
	DD	2556214130
	DD	1070924874
	DD	2390331068
	DD	1068637142
	DD	3061732144
	DD	1066455099
	DD	132457325
	DD	1063473428
	DD	77267609
	DD	1060740146
	DD	0
	DD	1073610752
	DD	0
	DD	1073807360
	DD	519
	DD	1072562176
	DD	2863310855
	DD	1071033002
	DD	4281691395
	DD	1068761087
	DD	585618131
	DD	1066541602
	DD	2974320400
	DD	1063605679
	DD	678988630
	DD	1060872621
	DD	1972728919
	DD	1073776631
	DD	56962720
	DD	1073896825
	DD	1972726129
	DD	1072728055
	DD	1507574747
	DD	1071152289
	DD	1236474596
	DD	1068894879
	DD	2596242240
	DD	1066637031
	DD	2551117528
	DD	1063748280
	DD	2378695948
	DD	1061015700
	DD	224845944
	DD	1073885113
	DD	117214643
	DD	1073995331
	DD	224844225
	DD	1072836537
	DD	3019581943
	DD	1071283630
	DD	1750030683
	DD	1069039521
	DD	2695514049
	DD	1066742104
	DD	3624170218
	DD	1063902615
	DD	1107335813
	DD	1061167917
	DD	2871317792
	DD	1074002547
	DD	4282875693
	DD	1074103617
	DD	2871320350
	DD	1072953971
	DD	1415562586
	DD	1071428013
	DD	3773014827
	DD	1069196100
	DD	2382174280
	DD	1066857610
	DD	129163764
	DD	1064069809
	DD	3640675859
	DD	1061257602
	DD	1934311918
	DD	1074129817
	DD	1504916907
	DD	1074222499
	DD	1934313764
	DD	1073081241
	DD	3438212916
	DD	1071586521
	DD	3966326130
	DD	1069365793
	DD	1845065342
	DD	1066984417
	DD	3389728721
	DD	1064250797
	DD	3527403372
	DD	1061353326
	DD	1803612042
	DD	1074267878
	DD	416567852
	DD	1074352868
	DD	1803613615
	DD	1073219302
	DD	1709375870
	DD	1071702509
	DD	2613564055
	DD	1069548697
	DD	1615778159
	DD	1067123477
	DD	4047280426
	DD	1064375895
	DD	706407907
	DD	1061459709
	DD	2835957237
	DD	1074417767
	DD	2309775719
	DD	1074495703
	DD	2835955196
	DD	1073369191
	DD	108179025
	DD	1071797733
	DD	3331934321
	DD	1069648623
	DD	1549280459
	DD	1067275835
	DD	231503331
	DD	1064482420
	DD	4215494979
	DD	1061574077
	DD	454539771
	DD	1074580611
	DD	2665451282
	DD	1074652078
	DD	454542900
	DD	1073532035
	DD	345326668
	DD	1071901983
	DD	267850325
	DD	1069757186
	DD	924938247
	DD	1067442635
	DD	2202586481
	DD	1064598326
	DD	500518451
	DD	1061703394
	DD	0
	DD	1074757632
	DD	0
	DD	1074806784
	DD	545
	DD	1073709056
	DD	2863310885
	DD	1072016042
	DD	4281026953
	DD	1069875199
	DD	1444035183
	DD	1067537749
	DD	1197943083
	DD	1064724165
	DD	1077022065
	DD	1061841306
	DD	180574770
	DD	1074870280
	DD	1849116868
	DD	1074900328
	DD	180573421
	DD	1073821704
	DD	2465482804
	DD	1072140768
	DD	253611511
	DD	1070003552
	DD	385242732
	DD	1067637530
	DD	1129514441
	DD	1064861019
	DD	2031501120
	DD	1061992766
	DD	3405709678
	DD	1074975020
	DD	1231318205
	DD	1075002575
	DD	3405708510
	DD	1073926444
	DD	3073397463
	DD	1072277097
	DD	1686448159
	DD	1070143206
	DD	1867096400
	DD	1067746593
	DD	1953490747
	DD	1065009999
	DD	3754852836
	DD	1062158409
	DD	179426533
	DD	1075089025
	DD	2679799656
	DD	1075114292
	DD	179430305
	DD	1074040449
	DD	709782363
	DD	1072426054
	DD	162786634
	DD	1070295212
	DD	1813467522
	DD	1067865758
	DD	1117834422
	DD	1065172339
	DD	1589567099
	DD	1062275417
	DD	699546965
	DD	1075213149
	DD	2739681860
	DD	1075236319
	DD	699547067
	DD	1074164573
	DD	3652911786
	DD	1072588756
	DD	3781056825
	DD	1070460710
	DD	2893910397
	DD	1067995920
	DD	2126742205
	DD	1065348739
	DD	4146131465
	DD	1062373562
	DD	2357212295
	DD	1075348325
	DD	4157934896
	DD	1075369572
	DD	2357213166
	DD	1074299749
	DD	4203608045
	DD	1072729837
	DD	125162258
	DD	1070618521
	DD	2552197783
	DD	1068138057
	DD	2468663921
	DD	1065447123
	DD	1434477412
	DD	1062481574
	DD	2638639167
	DD	1075495569
	DD	2507093787
	DD	1075515053
	DD	2638641368
	DD	1074446993
	DD	239748246
	DD	1072826825
	DD	3164087932
	DD	1070716683
	DD	3550045066
	DD	1068293236
	DD	916187271
	DD	1065551862
	DD	3816237083
	DD	1062600652
	DD	1820502500
	DD	1075655987
	DD	1299488554
	DD	1075673854
	DD	1820504609
	DD	1074607411
	DD	866328203
	DD	1072932692
	DD	4050380138
	DD	1070823628
	DD	1285083134
	DD	1068462624
	DD	3773794448
	DD	1065665941
	DD	3992431453
	DD	1062729361
	DD	0
	DD	1075830784
	DD	0
	DD	1075843072
	DD	551
	DD	1074782208
	DD	2863310890
	DD	1073048234
	DD	4280866933
	DD	1070940159
	DD	584957920
	DD	1068573218
	DD	2832093524
	DD	1065790218
	DD	888085363
	DD	1062869909
	DD	806278057
	DD	1075930124
	DD	1223413582
	DD	1075937636
	DD	806274936
	DD	1074881548
	DD	1631187813
	DD	1073174320
	DD	1117996735
	DD	1071067152
	DD	3539687907
	DD	1068674086
	DD	1879685196
	DD	1065925553
	DD	1160881408
	DD	1063021332
	DD	3127183787
	DD	1076033929
	DD	1509844094
	DD	1076040818
	DD	3127187021
	DD	1074985353
	DD	2013156894
	DD	1073311896
	DD	2671582213
	DD	1071205559
	DD	1953547779
	DD	1068784147
	DD	1400782066
	DD	1066073442
	DD	3995115867
	DD	1063193683
	DD	1653937367
	DD	1076147076
	DD	1205288824
	DD	1076153393
	DD	1653935046
	DD	1075098500
	DD	1607033566
	DD	1073461996
	DD	3664268179
	DD	1071356421
	DD	2469699602
	DD	1068904227
	DD	1532116762
	DD	1066234156
	DD	2977079648
	DD	1063314464
	DD	390855727
	DD	1076270414
	DD	3048373099
	DD	1076276206
	DD	390853261
	DD	1075221838
	DD	2632821043
	DD	1073625747
	DD	549867025
	DD	1071520872
	DD	2471230975
	DD	1069035228
	DD	956813323
	DD	1066405683
	DD	3149969443
	DD	1063414189
	DD	1421870534
	DD	1076404869
	DD	798309360
	DD	1076410181
	DD	1421873362
	DD	1075356293
	DD	532213845
	DD	1073773102
	DD	3779846496
	DD	1071672408
	DD	2302200646
	DD	1069178134
	DD	3166036895
	DD	1066501403
	DD	121344502
	DD	1063524534
	DD	441826002
	DD	1076551452
	DD	408939657
	DD	1076556323
	DD	441824999
	DD	1075502876
	DD	272623207
	DD	1073870530
	DD	3158485477
	DD	1071770130
	DD	1424774778
	DD	1069334019
	DD	1119737346
	DD	1066605572
	DD	2920460864
	DD	1063642371
	DD	2161993182
	DD	1076711263
	DD	957997871
	DD	1076715730
	DD	2161997168
	DD	1075662687
	DD	2070335460
	DD	1073976801
	DD	2830352426
	DD	1071876671
	DD	1964908623
	DD	1069504053
	DD	4115624093
	DD	1066719318
	DD	3794372293
	DD	1063773346
	DD	0
	DD	1076885504
	DD	0
	DD	1076888576
	DD	553
	DD	1075836928
	DD	2863310894
	DD	1074092714
	DD	4280820838
	DD	1071992831
	DD	1443864459
	DD	1069618517
	DD	88635046
	DD	1066843164
	DD	1252176869
	DD	1063913492
	DD	962703878
	DD	1076981517
	DD	1066987759
	DD	1076983395
	DD	962706711
	DD	1075932941
	DD	1422665967
	DD	1074219140
	DD	1225009594
	DD	1072119484
	DD	3437844028
	DD	1069719657
	DD	2037998457
	DD	1066978366
	DD	2730254678
	DD	1064068959
	DD	4131294139
	DD	1077085088
	DD	505733744
	DD	1077086811
	DD	4131292077
	DD	1076036512
	DD	674296617
	DD	1074357028
	DD	4100678826
	DD	1072257579
	DD	811946276
	DD	1069829968
	DD	2503873247
	DD	1067125487
	DD	3711859275
	DD	1064234493
	DD	948823251
	DD	1077198021
	DD	1910402939
	DD	1077199600
	DD	948823671
	DD	1076149445
	DD	3978864649
	DD	1074507413
	DD	1245700928
	DD	1072408156
	DD	3113056604
	DD	1069950276
	DD	2574888035
	DD	1067286207
	DD	4135901481
	DD	1064362011
	DD	1387424741
	DD	1077321162
	DD	2051804084
	DD	1077322610
	DD	1387425898
	DD	1076272586
	DD	1304084744
	DD	1074671427
	DD	1816822804
	DD	1072572344
	DD	1865285038
	DD	1070081487
	DD	429722136
	DD	1067455874
	DD	2577837837
	DD	1064461937
	DD	1188035094
	DD	1077455437
	DD	1032144801
	DD	1077456765
	DD	1188036279
	DD	1076406861
	DD	688092076
	DD	1074820350
	DD	3637955290
	DD	1072722312
	DD	3797092908
	DD	1070224585
	DD	2283066980
	DD	1067551364
	DD	3220833869
	DD	1064570645
	DD	3113848182
	DD	1077601854
	DD	2031884771
	DD	1077603072
	DD	3113852775
	DD	1076553278
	DD	1354609713
	DD	1074917888
	DD	2028801321
	DD	1072819924
	DD	3224671877
	DD	1070380646
	DD	2229652729
	DD	1067655555
	DD	687475109
	DD	1064691259
	DD	2247365853
	DD	1077761514
	DD	872625202
	DD	1077762631
	DD	2247366044
	DD	1076712938
	DD	3445052219
	DD	1075024260
	DD	1487959742
	DD	1072926364
	DD	4081984271
	DD	1070550842
	DD	3194887604
	DD	1067769012
	DD	1428358124
	DD	1064818848
	DD	0
	DD	1077935616
	DD	0
	DD	1077936384
	DD	553
	DD	1076887040
	DD	2863310893
	DD	1075140266
	DD	4280815405
	DD	1073042431
	DD	584898172
	DD	1070666274
	DD	1481010731
	DD	1067892832
	DD	3449978604
	DD	1064960819
	DD	2075552158
	DD	1078030797
	DD	4249106777
	DD	1078031266
	DD	2075550082
	DD	1076982221
	DD	1370477227
	DD	1075266777
	DD	1360858426
	DD	1073168999
	DD	3322122933
	DD	1070767482
	DD	894790874
	DD	1068027754
	DD	313386866
	DD	1065112862
	DD	3308579903
	DD	1078134310
	DD	1328447981
	DD	1078134741
	DD	3308576517
	DD	1077085734
	DD	339568777
	DD	1075404743
	DD	162985674
	DD	1073307017
	DD	1786761066
	DD	1070877855
	DD	3853563277
	DD	1068174930
	DD	630026056
	DD	1065280361
	DD	1846286546
	DD	1078247189
	DD	1012939644
	DD	1078247584
	DD	1846287652
	DD	1077198613
	DD	1350590555
	DD	1075555200
	DD	3862266317
	DD	1073457521
	DD	146086404
	DD	1070998221
	DD	1969531125
	DD	1068335652
	DD	2173124931
	DD	1065410138
	DD	1636566995
	DD	1078370281
	DD	1802661831
	DD	1078370643
	DD	1636564793
	DD	1077321705
	DD	971872480
	DD	1075719279
	DD	2206286067
	DD	1073621644
	DD	1139809035
	DD	1071129484
	DD	1369988394
	DD	1068504771
	DD	3343567141
	DD	1065509148
	DD	1129576234
	DD	1078504511
	DD	1090603661
	DD	1078504843
	DD	1129577008
	DD	1077455935
	DD	727064831
	DD	1075868594
	DD	3602485534
	DD	1073771220
	DD	3003617391
	DD	1071272630
	DD	3101442371
	DD	1068600286
	DD	255129745
	DD	1065618798
	DD	1634370080
	DD	1078650887
	DD	3511362876
	DD	1078651191
	DD	1634367542
	DD	1077602311
	DD	3772549016
	DD	1075966159
	DD	3966594334
	DD	1073868804
	DD	3973919909
	DD	1071428735
	DD	1360835337
	DD	1068704318
	DD	3229532341
	DD	1065736828
	DD	1194967197
	DD	1078810509
	DD	1925023859
	DD	1078810788
	DD	1194964307
	DD	1077761933
	DD	2714984140
	DD	1076072557
	DD	2244282950
	DD	1073975219
	DD	1592981707
	DD	1071598972
	DD	4054940274
	DD	1068817826
	DD	2174666341
	DD	1065866173
	DD	0
	DD	1078984576
	DD	0
	DD	1078984768
	DD	554
	DD	1077936000
	DD	2863310896
	DD	1076188586
	DD	4280801865
	DD	1074091263
	DD	1443817676
	DD	1071714645
	DD	1967595988
	DD	1068941681
	DD	489391739
	DD	1066009084
	DD	2353764228
	DD	1079079549
	DD	3970894707
	DD	1079079666
	DD	2353760925
	DD	1078030973
	DD	2431184657
	DD	1076315118
	DD	321072719
	DD	1074217810
	DD	4179967377
	DD	1071815870
	DD	678236900
	DD	1069076533
	DD	2141802380
	DD	1066161039
	DD	955417696
	DD	1079183048
	DD	3681610188
	DD	1079183155
	DD	955413979
	DD	1078134472
	DD	3477118682
	DD	1076453103
	DD	1326046035
	DD	1074355808
	DD	863299713
	DD	1071926259
	DD	3117225129
	DD	1069223723
	DD	2396986094
	DD	1066328644
	DD	2070652370
	DD	1079295913
	DD	788573821
	DD	1079296012
	DD	2070651514
	DD	1078247337
	DD	3914723815
	DD	1076603578
	DD	1331571755
	DD	1074506295
	DD	868203065
	DD	1072046639
	DD	431241892
	DD	1069384363
	DD	2246818571
	DD	1066457734
	DD	625110734
	DD	1079418993
	DD	2814118091
	DD	1079419083
	DD	625111957
	DD	1078370417
	DD	888847596
	DD	1076767674
	DD	2230927568
	DD	1074670401
	DD	1532605793
	DD	1072177915
	DD	532982727
	DD	1069553510
	DD	3846699990
	DD	1066558540
	DD	3262445167
	DD	1079553211
	DD	3252702024
	DD	1079553294
	DD	3262445839
	DD	1078504635
	DD	736808022
	DD	1076917087
	DD	3593608959
	DD	1074819879
	DD	3878877473
	DD	1072321073
	DD	1262415275
	DD	1069648949
	DD	1893888386
	DD	1066667268
	DD	2338242378
	DD	1079699577
	DD	2807490577
	DD	1079699653
	DD	2338242322
	DD	1078651001
	DD	3303312501
	DD	1077014659
	DD	119713135
	DD	1074917457
	DD	253619889
	DD	1072477190
	DD	2219021147
	DD	1069753023
	DD	4162054000
	DD	1066786194
	DD	4153093004
	DD	1079859189
	DD	3261865345
	DD	1079859259
	DD	4153097873
	DD	1078810613
	DD	3606246712
	DD	1077121063
	DD	1286900661
	DD	1075023865
	DD	1838975761
	DD	1072647436
	DD	2091142296
	DD	1069866627
	DD	3637235953
	DD	1066917327
	DD	0
	DD	1080033248
	DD	0
	DD	1080033296
	DD	554
	DD	1078984672
	DD	2863310896
	DD	1077237098
	DD	4280801526
	DD	1075139903
	DD	584824499
	DD	1072763170
	DD	3128350829
	DD	1069990325
	DD	388973629
	DD	1067057582
	DD	2423317245
	DD	1080128169
	DD	3901341689
	DD	1080128198
	DD	2423317900
	DD	1079079593
	DD	3770125126
	DD	1077363635
	DD	3209627449
	DD	1075266444
	DD	3988311033
	DD	1072864399
	DD	3848672452
	DD	1070125324
	DD	3118379075
	DD	1067211445
	DD	2514610792
	DD	1080231664
	DD	2122417091
	DD	1080231691
	DD	2514609124
	DD	1079183088
	DD	4261529832
	DD	1077501625
	DD	506722372
	DD	1075404438
	DD	242677220
	DD	1072974792
	DD	1687936003
	DD	1070272436
	DD	3170278816
	DD	1067378880
	DD	2126743826
	DD	1080344526
	DD	732482365
	DD	1080344551
	DD	2126742480
	DD	1079295950
	DD	2408279879
	DD	1077652105
	DD	2846369583
	DD	1075554920
	DD	3102695869
	DD	1073095175
	DD	3406344199
	DD	1070432972
	DD	334424787
	DD	1067506258
	DD	372246669
	DD	1080467603
	DD	3066982156
	DD	1080467625
	DD	372246615
	DD	1079419027
	DD	4089312338
	DD	1077816204
	DD	3347213243
	DD	1075719022
	DD	666900058
	DD	1073226455
	DD	2349197098
	DD	1070602085
	DD	939743989
	DD	1067607030
	DD	3795662400
	DD	1080601818
	DD	2719484790
	DD	1080601839
	DD	3795665178
	DD	1079553242
	DD	1812997480
	DD	1077965642
	DD	2499477572
	DD	1075868476
	DD	2634225501
	DD	1073369616
	DD	682305439
	DD	1070697588
	DD	2631503722
	DD	1067716684
	DD	366726804
	DD	1080748182
	DD	484038853
	DD	1080748201
	DD	366731634
	DD	1079699606
	DD	3186023857
	DD	1078063216
	DD	195363368
	DD	1075966052
	DD	1858310082
	DD	1073525735
	DD	1465317937
	DD	1070801714
	DD	2313583288
	DD	1067836510
	DD	597657160
	DD	1080907792
	DD	2522333893
	DD	1080907809
	DD	597661836
	DD	1079859216
	DD	1681570062
	DD	1078169622
	DD	3213224383
	DD	1076072458
	DD	2197034374
	DD	1073695984
	DD	1547434940
	DD	1070915218
	DD	1991814041
	DD	1067965873
	DD	0
	DD	1081081848
	DD	0
	DD	1081081860
	DD	554
	DD	1080033272
	DD	2863310896
	DD	1078285658
	DD	4280801441
	DD	1076188495
	DD	1443818027
	DD	1073811733
	DD	3418544190
	DD	1071038918
	DD	2511418032
	DD	1068106138
	DD	2440705499
	DD	1081176756
	DD	3883953434
	DD	1081176763
	DD	2440709276
	DD	1080128180
	DD	883652058
	DD	1078412197
	DD	1747917283
	DD	1076315035
	DD	422689944
	DD	1073912964
	DD	1456304861
	DD	1071174037
	DD	3459587548
	DD	1068261828
	DD	2904409066
	DD	1081280250
	DD	1732618817
	DD	1081280257
	DD	2904407911
	DD	1080231674
	DD	2310142585
	DD	1078550188
	DD	2449356831
	DD	1076453027
	DD	1254527275
	DD	1074023357
	DD	2612104548
	DD	1071321046
	DD	169532989
	DD	1068427488
	DD	3214508514
	DD	1081393111
	DD	3939684973
	DD	1081393117
	DD	3214507046
	DD	1080344535
	DD	957927075
	DD	1078700669
	DD	3831387
	DD	1076603509
	DD	3661221253
	DD	1074143741
	DD	1067371691
	DD	1071481557
	DD	971005243
	DD	1068554821
	DD	2456514301
	DD	1081516187
	DD	982714525
	DD	1081516193
	DD	2456511796
	DD	1080467611
	DD	2741921202
	DD	1078864769
	DD	441406119
	DD	1076767610
	DD	840224812
	DD	1074275022
	DD	741498441
	DD	1071650620
	DD	3872266127
	DD	1068654717
	DD	2855224885
	DD	1081650402
	DD	3659922307
	DD	1081650407
	DD	2855221793
	DD	1080601826
	DD	1008277094
	DD	1079014213
	DD	3354222170
	DD	1076917057
	DD	2138587760
	DD	1074418184
	DD	621363220
	DD	1071746056
	DD	3621085838
	DD	1068763446
	DD	947589735
	DD	1081796765
	DD	4198143219
	DD	1081796769
	DD	947591521
	DD	1080748189
	DD	4230426231
	DD	1079111787
	DD	3471872690
	DD	1077014632
	DD	3926297451
	DD	1074574303
	DD	2245132281
	DD	1071850236
	DD	2649906091
	DD	1068884171
	DD	2930023671
	DD	1081956374
	DD	189967382
	DD	1081956379
	DD	2930028299
	DD	1080907798
	DD	126659077
	DD	1079218194
	DD	473576796
	DD	1077121039
	DD	2286477392
	DD	1074744553
	DD	372390273
	DD	1071963798
	DD	236850944
	DD	1069014442
	DD	0
	DD	1082130430
	DD	0
	DD	1082130433
	DD	554
	DD	1081081854
	DD	2863310896
	DD	1079334230
	DD	4280801420
	DD	1077237075
	DD	584824587
	DD	1074860306
	DD	269862407
	DD	1072087499
	DD	3041954663
	DD	1069154709
	DD	1371310739
	DD	1082225335
	DD	658380899
	DD	1082225337
	DD	1371311032
	DD	1081176759
	DD	2309489262
	DD	1079460769
	DD	381466140
	DD	1077363615
	DD	1104482025
	DD	1074961537
	DD	1997887223
	DD	1072222482
	DD	2422405383
	DD	1069308542
	DD	854374986
	DD	1082328829
	DD	3782652895
	DD	1082328830
	DD	854378224
	DD	1081280253
	DD	748601306
	DD	1079598761
	DD	3936032960
	DD	1077501606
	DD	1801490377
	DD	1075071930
	DD	698907663
	DD	1072369796
	DD	3117308375
	DD	1069479539
	DD	265224214
	DD	1082441690
	DD	2594001977
	DD	1082441691
	DD	265222715
	DD	1081393114
	DD	595338870
	DD	1079749242
	DD	366950843
	DD	1077652088
	DD	1653466738
	DD	1075192315
	DD	1417893004
	DD	1072530135
	DD	3237255031
	DD	1069603393
	DD	2977581209
	DD	1082564765
	DD	461647618
	DD	1082564767
	DD	2977578091
	DD	1081516189
	DD	3478796063
	DD	1079913342
	DD	4009927721
	DD	1077816188
	DD	90082880
	DD	1075323596
	DD	3526213041
	DD	1072699185
	DD	4038648974
	DD	1069702494
	DD	2620115506
	DD	1082698980
	DD	3895031686
	DD	1082698981
	DD	2620113079
	DD	1081650404
	DD	4028327917
	DD	1080062787
	DD	328500247
	DD	1077965635
	DD	1811589807
	DD	1075466758
	DD	1697998849
	DD	1072794646
	DD	4063787973
	DD	1069812051
	DD	19063644
	DD	1082845343
	DD	831702015
	DD	1082845344
	DD	19062537
	DD	1081796767
	DD	3417776359
	DD	1080160362
	DD	14209235
	DD	1078063210
	DD	444716692
	DD	1075622878
	DD	3564927921
	DD	1072898757
	DD	1585452687
	DD	1069931844
	DD	1365631651
	DD	1083004952
	DD	1754359403
	DD	1083004953
	DD	1365634135
	DD	1081956376
	DD	4032886789
	DD	1080266768
	DD	880583233
	DD	1078169616
	DD	3772380090
	DD	1075793127
	DD	3350968953
	DD	1073012333
	DD	1825857751
	DD	1070062149
	DD	2147483648
	DD	1083179007
	DD	1073741824
	DD	1083179008
	DD	2147484202
	DD	1082130431
	DD	2863310896
	DD	1080382805
	DD	4280801414
	DD	1078285652
	DD	1443818045
	DD	1075908881
	DD	556447737
	DD	1073136076
	DD	2101070372
	DD	1070203284
	DD	30220225
	DD	1083273912
	DD	1999471413
	DD	1083273912
	DD	30219647
	DD	1082225336
	DD	2665954955
	DD	1080509344
	DD	39853355
	DD	1078412192
	DD	2255272812
	DD	1076010112
	DD	3207010299
	DD	1073271025
	DD	2006307835
	DD	1070357037
	DD	3563091938
	DD	1083377405
	DD	1073935943
	DD	1083377406
	DD	3563096274
	DD	1082328829
	DD	1431945023
	DD	1080647336
	DD	3233966257
	DD	1078550183
	DD	3198848380
	DD	1076120505
	DD	2298886726
	DD	1073418415
	DD	1364133258
	DD	1070528215
	DD	2749128611
	DD	1083490266
	DD	110097580
	DD	1083490267
	DD	2749127104
	DD	1082441690
	DD	1578433641
	DD	1080797817
	DD	3678962269
	DD	1078700664
	DD	3299054964
	DD	1076240890
	DD	3583777588
	DD	1073578711
	DD	3308485264
	DD	1070651968
	DD	960364288
	DD	1083613342
	DD	2478864539
	DD	1083613342
	DD	960361017
	DD	1082564766
	DD	441795701
	DD	1080961918
	DD	2754568384
	DD	1078864765
	DD	1956566063
	DD	1076372171
	DD	1035776069
	DD	1073747759
	DD	2596065895
	DD	1070751063
	DD	413854513
	DD	1083747557
	DD	1806325382
	DD	1083747557
	DD	413854384
	DD	1082698981
	DD	2635868811
	DD	1081111363
	DD	1701379949
	DD	1079014211
	DD	3487595464
	DD	1076515333
	DD	807696162
	DD	1073843267
	DD	3490667642
	DD	1070861501
	DD	1934415769
	DD	1083893919
	DD	3211317186
	DD	1083893919
	DD	1934416071
	DD	1082845343
	DD	2140874317
	DD	1081208938
	DD	1279097463
	DD	1079111786
	DD	2685929170
	DD	1076671453
	DD	1730921990
	DD	1073947361
	DD	2936264088
	DD	1070980484
	DD	3122017294
	DD	1084053528
	DD	4292941056
	DD	1084053528
	DD	3122019241
	DD	1083004952
	DD	2861963261
	DD	1081315344
	DD	2056088848
	DD	1079218192
	DD	1903168657
	DD	1076841703
	DD	2883380270
	DD	1074060899
	DD	4210992368
	DD	1071110699
	DD	3758096384
	DD	1084227583
	DD	268435456
	DD	1084227584
	DD	3758096938
	DD	1083179007
	DD	1789569072
	DD	1081431381
	DD	1059575941
	DD	1079334229
	DD	584824589
	DD	1076957457
	DD	1701826592
	DD	1074184652
	DD	791976915
	DD	1071251860
	DD	768689420
	DD	1084322488
	DD	1261002217
	DD	1084322488
	DD	768692889
	DD	1083273912
	DD	1681351342
	DD	1081557920
	DD	955467668
	DD	1079460768
	DD	1063106328
	DD	1077058688
	DD	2438888433
	DD	1074319758
	DD	368525864
	DD	1071407523
	DD	4240271176
	DD	1084425981
	DD	396756705
	DD	1084425982
	DD	4240275786
	DD	1083377405
	DD	529039124
	DD	1081695912
	DD	4132203586
	DD	1079598759
	DD	2474549528
	DD	1077169081
	DD	1486666934
	DD	1074467002
	DD	2869201860
	DD	1071576815
	DD	3370104710
	DD	1084538842
	DD	3784088776
	DD	1084538842
	DD	3370105334
	DD	1083490266
	DD	750489193
	DD	1081846393
	DD	175620448
	DD	1079749241
	DD	2246720186
	DD	1077289466
	DD	2152491105
	DD	1074627370
	DD	3465128389
	DD	1071701411
	DD	1529801881
	DD	1084661918
	DD	1909426943
	DD	1084661918
	DD	1529804969
	DD	1083613342
	DD	3977583942
	DD	1082010493
	DD	3405380863
	DD	1079913341
	DD	179822948
	DD	1077420747
	DD	1506659931
	DD	1074796458
	DD	2985677691
	DD	1071802238
	DD	936031089
	DD	1084796133
	DD	1284148807
	DD	1084796133
	DD	936029403
	DD	1083747557
	DD	2287739003
	DD	1082159939
	DD	2062770294
	DD	1080062787
	DD	3315945067
	DD	1077563909
	DD	705469102
	DD	1074891813
	DD	2396211301
	DD	1071909237
	DD	2413253800
	DD	1084942495
	DD	2732479154
	DD	1084942495
	DD	2413256587
	DD	1083893919
	DD	1821660648
	DD	1082257514
	DD	1577133874
	DD	1080160362
	DD	1782509562
	DD	1077720029
	DD	2398915295
	DD	1074995985
	DD	66228759
	DD	1072029944
	DD	3561113705
	DD	1085102104
	DD	3853844646
	DD	1085102104
	DD	3561113386
	DD	1084053528
	DD	2569220542
	DD	1082363920
	DD	2368138716
	DD	1080266768
	DD	1825628382
	DD	1077890279
	DD	3925963295
	DD	1075109431
	DD	4047704299
	DD	1072158402
	DD	4160749568
	DD	1085276159
	DD	67108864
	DD	1085276160
	DD	4160750122
	DD	1084227583
	DD	1521133616
	DD	1082479957
	DD	1328011397
	DD	1080382805
	DD	370076228
	DD	1078006033
	DD	1988166656
	DD	1075233228
	DD	464592383
	DD	1072300436
	DD	953306719
	DD	1085371064
	DD	1076384918
	DD	1085371064
	DD	953309067
	DD	1084322488
	DD	1435195934
	DD	1082606496
	DD	1220742541
	DD	1080509344
	DD	874797354
	DD	1078107264
	DD	1067557893
	DD	1075368291
	DD	2716721913
	DD	1072455995
	DD	114598690
	DD	1085474558
	DD	227461896
	DD	1085474558
	DD	114599104
	DD	1084425982
	DD	303290865
	DD	1082744488
	DD	134513846
	DD	1080647336
	DD	2699514174
	DD	1078217657
	DD	275778966
	DD	1075515416
	DD	1264773403
	DD	1072623468
	DD	3525348735
	DD	1085587418
	DD	3628844752
	DD	1085587418
	DD	3525347759
	DD	1084538842
	DD	543473007
	DD	1082894969
	DD	409898107
	DD	1080797817
	DD	2466993696
	DD	1078338042
	DD	2762938322
	DD	1075675884
	DD	595062760
	DD	1072749145
	DD	1672161280
	DD	1085710494
	DD	1767067546
	DD	1085710494
	DD	1672159560
	DD	1084661918
	DD	3787724531
	DD	1083059069
	DD	3677185676
	DD	1080961917
	DD	811952329
	DD	1078469323
	DD	1535376778
	DD	1075844941
	DD	2275213375
	DD	1072849055
	DD	1066575233
	DD	1085844709
	DD	1153604663
	DD	1085844709
	DD	1066573157
	DD	1084796133
	DD	2200709744
	DD	1083208515
	DD	2153127016
	DD	1081111363
	DD	3179780105
	DD	1078612485
	DD	2723537752
	DD	1075940381
	DD	1446496431
	DD	1072957795
	DD	2532963308
	DD	1085991071
	DD	2612769647
	DD	1085991071
	DD	2532964584
	DD	1084942495
	DD	1741842196
	DD	1083306090
	DD	1669822531
	DD	1081208938
	DD	2039902703
	DD	1078768605
	DD	1508680971
	DD	1076044532
	DD	3045798495
	DD	1073077681
	DD	3670887808
	DD	1086150680
	DD	3744070544
	DD	1086150680
	DD	3670884790
	DD	1085102104
	DD	2496026219
	DD	1083412496
	DD	2464333784
	DD	1081315344
	DD	2102719453
	DD	1078938855
	DD	3094728764
	DD	1076157955
	DD	4014581870
	DD	1073206085
	DD	4261412864
	DD	1086324735
	DD	16777216
	DD	1086324736
	DD	4261413418
	DD	1085276159
	DD	1454024752
	DD	1083528533
	DD	1395120260
	DD	1081431381
	DD	316389129
	DD	1079054609
	DD	2059770273
	DD	1076281804
	DD	383071603
	DD	1073349012
	DD	999461044
	DD	1086419640
	DD	1030230594
	DD	1086419640
	DD	999460980
	DD	1085371064
	DD	1373633409
	DD	1083655072
	DD	1323408188
	DD	1081557920
	DD	1217459964
	DD	1079155840
	DD	4117422823
	DD	1076416773
	DD	2277811950
	DD	1073502812
	DD	156922392
	DD	1086523134
	DD	185138193
	DD	1086523134
	DD	156926022
	DD	1085474558
	DD	246881981
	DD	1083793064
	DD	136102831
	DD	1081695912
	DD	2256207783
	DD	1079266233
	DD	2193094862
	DD	1076564116
	DD	4088070846
	DD	1073673877
	DD	3564159741
	DD	1086635994
	DD	3590033745
	DD	1086635994
	DD	3564160497
	DD	1085587418
	DD	491749028
	DD	1083943545
	DD	432114504
	DD	1081846393
	DD	2038862927
	DD	1079386618
	DD	2813288435
	DD	1076724527
	DD	2065802453
	DD	1073798569
	DD	1707751129
	DD	1086759070
	DD	1731477695
	DD	1086759070
	DD	1707754605
	DD	1085710494
	DD	3740317935
	DD	1084107645
	DD	3636029094
	DD	1082010493
	DD	4281998316
	DD	1079517898
	DD	3813677248
	DD	1076893617
	DD	2344318102
	DD	1073899408
	DD	1099211269
	DD	1086893285
	DD	1120968627
	DD	1086893285
	DD	1099209096
	DD	1085844709
	DD	2178952431
	DD	1084257091
	DD	2175710106
	DD	1082159939
	DD	3145652441
	DD	1079661061
	DD	3297293607
	DD	1076988955
	DD	52434176
	DD	1074006367
	DD	2562890685
	DD	1087039647
	DD	2582842270
	DD	1087039647
	DD	2562891583
	DD	1085991071
	DD	1721890778
	DD	1084354666
	DD	1692997742
	DD	1082257514
	DD	2010900862
	DD	1079817181
	DD	177733124
	DD	1077093101
	DD	1007048313
	DD	1074126240
	DD	3698331333
	DD	1087199256
	DD	3716627017
	DD	1087199256
	DD	3698334037
	DD	1086150680
	DD	2477753566
	DD	1084461072
	DD	2433843884
	DD	1082363920
	DD	1282665025
	DD	1079987431
	DD	1763731007
	DD	1077206642
	DD	2805159360
	DD	1074256462
	DD	4286578688
	DD	1087373311
	DD	4194304
	DD	1087373312
	DD	4286579242
	DD	1086324735
	DD	1437247536
	DD	1084577109
	DD	1411897476
	DD	1082479957
	DD	302967356
	DD	1080103185
	DD	2077666527
	DD	1077330380
	DD	362626136
	DD	1074397588
	DD	1010999625
	DD	1087468216
	DD	1018692012
	DD	1087468216
	DD	1011001090
	DD	1086419640
	DD	1358272845
	DD	1084703648
	DD	1312721582
	DD	1082606496
	DD	819932153
	DD	1080204416
	DD	482660068
	DD	1077465409
	DD	3200054132
	DD	1074553066
	DD	167503318
	DD	1087571710
	DD	174557269
	DD	1087571710
	DD	167503487
	DD	1086523134
	DD	232726010
	DD	1084841640
	DD	209230477
	DD	1082744488
	DD	3018553297
	DD	1080314809
	DD	2599950208
	DD	1077612558
	DD	3730748960
	DD	1074719059
	DD	3573862493
	DD	1087684570
	DD	3580330995
	DD	1087684570
	DD	3573859418
	DD	1086635994
	DD	478764291
	DD	1084992121
	DD	510374640
	DD	1082894969
	DD	2804812444
	DD	1080435194
	DD	1956633881
	DD	1077772955
	DD	3531840445
	DD	1074845431
	DD	1716648592
	DD	1087807646
	DD	1722580234
	DD	1087807646
	DD	1716646969
	DD	1086759070
	DD	3728408029
	DD	1085156221
	DD	3734847734
	DD	1083059069
	DD	763760032
	DD	1080566475
	DD	1038386958
	DD	1077942095
	DD	4200661441
	DD	1074946211
	DD	1107370278
	DD	1087941861
	DD	1112809618
	DD	1087941861
	DD	1107368080
	DD	1086893285
	DD	2173513100
	DD	1085305667
	DD	2181365015
	DD	1083208515
	DD	3137255856
	DD	1080709637
	DD	1189376933
	DD	1078037531
	DD	2450715105
	DD	1075054941
	DD	2570372529
	DD	1088088223
	DD	2575360425
	DD	1088088223
	DD	2570375465
	DD	1087039647
	DD	1716914763
	DD	1085403242
	DD	1680608944
	DD	1083306090
	DD	1613763855
	DD	1080865757
	DD	2010613428
	DD	1078141716
	DD	3755500550
	DD	1075175678
	DD	3705192215
	DD	1088247832
	DD	3709766136
	DD	1088247832
	DD	3705189953
	DD	1087199256
	DD	2473169065
	DD	1085509648
	DD	2480757032
	DD	1083412496
	DD	1686687141
	DD	1081036007
	DD	1515024379
	DD	1078255122
	DD	3444278726
	DD	1075304041
	DD	4292870144
	DD	1088421887
	DD	1048576
	DD	1088421888
	DD	4292870698
	DD	1087373311
	DD	1433053232
	DD	1085625685
	DD	1416091780
	DD	1083528533
	DD	299611913
	DD	1081151761
	DD	2082140591
	DD	1078378956
	DD	357505597
	DD	1075446164
	DD	1013884270
	DD	1088516792
	DD	1015807366
	DD	1088516792
	DD	1013888250
	DD	1087468216
	DD	1354449993
	DD	1085752224
	DD	1273678638
	DD	1083655072
	DD	424013800
	DD	1081252992
	DD	1826968645
	DD	1078514082
	DD	481040262
	DD	1075603412
	DD	170148549
	DD	1088620286
	DD	171912036
	DD	1088620286
	DD	170152118
	DD	1087571710
	DD	229247162
	DD	1085890216
	DD	154775900
	DD	1083793064
	DD	2242502590
	DD	1081363385
	DD	695859775
	DD	1078661266
	DD	2809642103
	DD	1075771024
	DD	3576288180
	DD	1088733146
	DD	3577905305
	DD	1088733146
	DD	3576292676
	DD	1087684570
	DD	475600029
	DD	1086040697
	DD	384509323
	DD	1083943545
	DD	1623790536
	DD	1081483770
	DD	1541299999
	DD	1078821824
	DD	1479151397
	DD	1075896662
	DD	1718872957
	DD	1088856222
	DD	1720355867
	DD	1088856222
	DD	1718876457
	DD	1087807646
	DD	3725488808
	DD	1086204797
	DD	3650450699
	DD	1084107645
	DD	4269987770
	DD	1081615050
	DD	1507326798
	DD	1078990770
	DD	2391922729
	DD	1075996561
	DD	1109410030
	DD	1088990437
	DD	1110769865
	DD	1088990437
	DD	1109409959
	DD	1087941861
	DD	2172165110
	DD	1086354243
	DD	2164587006
	DD	1084257091
	DD	2745112118
	DD	1081758213
	DD	1858126594
	DD	1079086148
	DD	2734186292
	DD	1076104384
	DD	2572242990
	DD	1089136799
	DD	2573489964
	DD	1089136799
	DD	2572246435
	DD	1088088223
	DD	1715667561
	DD	1086451818
	DD	1677517834
	DD	1084354666
	DD	1607987786
	DD	1081914333
	DD	1325871784
	DD	1079190302
	DD	62862654
	DD	1076224278
	DD	3706907435
	DD	1089296408
	DD	3708050915
	DD	1089296408
	DD	3706908196
	DD	1088247832
	DD	2472037028
	DD	1086558224
	DD	2456126208
	DD	1084461072
	DD	1288248598
	DD	1082084583
	DD	3567360497
	DD	1079303756
	DD	2429673483
	DD	1076353525
	DD	4294443008
	DD	1089470463
	DD	262144
	DD	1089470464
	DD	4294443562
	DD	1088421887
	DD	1432004656
	DD	1086674261
	DD	1417140356
	DD	1084577109
	DD	298773052
	DD	1082200337
	DD	2083259107
	DD	1079427532
	DD	356234643
	DD	1076494740
	DD	1014605432
	DD	1089565368
	DD	1015086207
	DD	1089565368
	DD	1014603643
	DD	1088516792
	DD	1353416848
	DD	1086800800
	DD	1373019593
	DD	1084703648
	DD	1588164491
	DD	1082301568
	DD	911338360
	DD	1079562435
	DD	2425809233
	DD	1076646843
	DD	170809857
	DD	1089668862
	DD	171250729
	DD	1089668862
	DD	170812143
	DD	1088620286
	DD	228340982
	DD	1086938792
	DD	177539634
	DD	1084841640
	DD	2625295036
	DD	1082411961
	DD	2192656029
	DD	1079709792
	DD	1173511898
	DD	1076817944
	DD	3576894602
	DD	1089781722
	DD	3577298883
	DD	1089781722
	DD	3576898859
	DD	1088733146
	DD	474791683
	DD	1087089273
	DD	389389924
	DD	1084992121
	DD	1624864514
	DD	1082532346
	DD	535168506
	DD	1079870391
	DD	2644939051
	DD	1076945227
	DD	1719429049
	DD	1089904798
	DD	1719799777
	DD	1089904798
	DD	1719427432
	DD	1088856222
	DD	3724700747
	DD	1087253373
	DD	3738453135
	DD	1085156221
	DD	760763076
	DD	1082663627
	DD	1535543495
	DD	1080039247
	DD	929725033
	DD	1077043364
	DD	1109919968
	DD	1090039013
	DD	1110259927
	DD	1090039013
	DD	1109920428
	DD	1088990437
	DD	2171824913
	DD	1087402819
	DD	2160401639
	DD	1085305667
	DD	2740644634
	DD	1082806789
	DD	2995207291
	DD	1080134734
	DD	3113481135
	DD	1077152984
	DD	2572710606
	DD	1090185375
	DD	2573022350
	DD	1090185375
	DD	2572707781
	DD	1089136799
	DD	1715333027
	DD	1087500394
	DD	1731289815
	DD	1085403242
	DD	2402558359
	DD	1082962909
	DD	61134361
	DD	1080238757
	DD	3566035781
	DD	1077271027
	DD	3707336240
	DD	1090344984
	DD	3707622110
	DD	1090344984
	DD	3707337757
	DD	1089296408
	DD	2471750824
	DD	1087606800
	DD	2449965456
	DD	1085509648
	DD	1281977735
	DD	1083133159
	DD	1967594370
	DD	1080352347
	DD	1861523814
	DD	1077402136
	DD	4294836224
	DD	1090519039
	DD	65536
	DD	1090519040
	DD	4294836778
	DD	1089470463
	DD	1431742512
	DD	1087722837
	DD	1417402500
	DD	1085625685
	DD	298563337
	DD	1083248913
	DD	2083538736
	DD	1080476108
	DD	355907724
	DD	1077543316
	DD	1014785722
	DD	1090613944
	DD	1014905916
	DD	1090613944
	DD	1014786756
	DD	1089565368
	DD	1353199530
	DD	1087849376
	DD	1325118339
	DD	1085752224
	DD	1192773544
	DD	1083350144
	DD	1897932215
	DD	1080611120
	DD	1232933086
	DD	1077697217
	DD	170975184
	DD	1090717438
	DD	171085402
	DD	1090717438
	DD	170977150
	DD	1089668862
	DD	228120836
	DD	1087987368
	DD	183212298
	DD	1085890216
	DD	2627424704
	DD	1083460537
	DD	627049929
	DD	1080758356
	DD	2294847832
	DD	1077866491
	DD	3577046208
	DD	1090830298
	DD	3577147278
	DD	1090830298
	DD	3577046140
	DD	1089781722
	DD	474567805
	DD	1088137849
	DD	463346570
	DD	1086040697
	DD	2031347468
	DD	1083580922
	DD	3363044532
	DD	1080918799
	DD	3187696535
	DD	1077992835
	DD	1719568072
	DD	1090953374
	DD	1719660754
	DD	1090953374
	DD	1719565176
	DD	1089904798
	DD	3724516519
	DD	1088301949
	DD	3760447655
	DD	1086204797
	DD	770323340
	DD	1083712203
	DD	2650944499
	DD	1081087798
	DD	2345789816
	DD	1078091881
	DD	1110047453
	DD	1091087589
	DD	1110132443
	DD	1091087589
	DD	1110043781
	DD	1090039013
	DD	2171728970
	DD	1088451395
	DD	2195717454
	DD	1086354243
	DD	3145644424
	DD	1083855365
	DD	3277819497
	DD	1081183230
	DD	3500208489
	DD	1078200601
	DD	2572827509
	DD	1091233951
	DD	2572905445
	DD	1091233951
	DD	2572831646
	DD	1090185375
	DD	1715277573
	DD	1088548970
	DD	1672008496
	DD	1086451818
	DD	1602169577
	DD	1084011485
	DD	2969504976
	DD	1081287467
	DD	2627268862
	DD	1078321461
	DD	3707443441
	DD	1091393560
	DD	3707514908
	DD	1091393560
	DD	3707447279
	DD	1090344984
	DD	2471691110
	DD	1088655376
	DD	2430248758
	DD	1086558224
	DD	890643997
	DD	1084181735
	DD	1516543114
	DD	1081400968
	DD	3616768931
	DD	1078451587
	DD	4294934528
	DD	1091567615
	DD	16384
	DD	1091567616
	DD	4294935082
	DD	1090519039
	DD	1431676976
	DD	1088771413
	DD	1417468036
	DD	1086674261
	DD	298510908
	DD	1084297489
	DD	2083608643
	DD	1081524684
	DD	355835171
	DD	1078591892
	DD	1014830795
	DD	1091662520
	DD	1014860844
	DD	1091662520
	DD	1014828270
	DD	1090613944
	DD	1353117021
	DD	1088897952
	DD	1385867339
	DD	1086800800
	DD	1593441513
	DD	1084398720
	DD	3215006955
	DD	1081659558
	DD	1675787922
	DD	1078743928
	DD	171016516
	DD	1091766014
	DD	171044071
	DD	1091766014
	DD	171016269
	DD	1090717438
	DD	228042115
	DD	1089035944
	DD	221007845
	DD	1086938792
	DD	3017975541
	DD	1084509113
	DD	2208415334
	DD	1081806846
	DD	2754871653
	DD	1078913325
	DD	3577084109
	DD	1091878874
	DD	3577109376
	DD	1091878874
	DD	3577087225
	DD	1090830298
	DD	474540019
	DD	1089186425
	DD	409099237
	DD	1087089273
	DD	1633354712
	DD	1084629498
	DD	4211806470
	DD	1081967498
	DD	845776843
	DD	1079042327
	DD	1719602827
	DD	1092001950
	DD	1719625997
	DD	1092001950
	DD	1719606009
	DD	1090953374
	DD	3724515932
	DD	1089350525
	DD	3656844589
	DD	1087253373
	DD	4271591071
	DD	1084760778
	DD	871341381
	DD	1082136492
	DD	214436730
	DD	1079142275
	DD	1110079324
	DD	1092136165
	DD	1110100572
	DD	1092136165
	DD	1110076752
	DD	1091087589
	DD	2171707238
	DD	1089499971
	DD	2186357716
	DD	1087402819
	DD	3137095798
	DD	1084903941
	DD	214629270
	DD	1082231828
	DD	2959808010
	DD	1079249228
	DD	2572856735
	DD	1092282527
	DD	2572876219
	DD	1092282527
	DD	2572860481
	DD	1091233951
	DD	1715258267
	DD	1089597546
	DD	1675358587
	DD	1087500394
	DD	1604919815
	DD	1085060061
	DD	595696934
	DD	1082336036
	DD	624673770
	DD	1079370020
	DD	3707470242
	DD	1092442136
	DD	3707488109
	DD	1092442136
	DD	3707468263
	DD	1091393560
	DD	2471650253
	DD	1089703952
	DD	2479864341
	DD	1087606800
	DD	1682178719
	DD	1085230311
	DD	3531472556
	DD	1082449431
	DD	2611633522
	DD	1079498358
	DD	4294959104
	DD	1092616191
	DD	4096
	DD	1092616192
	DD	4294959658
	DD	1091567615
	DD	1431660592
	DD	1089819989
	DD	1417484420
	DD	1087722837
	DD	298497801
	DD	1085346065
	DD	2083626120
	DD	1082573260
	DD	355807856
	DD	1079640468
	DD	1014842063
	DD	1092711096
	DD	1014849575
	DD	1092711096
	DD	1014840780
	DD	1091662520
	DD	1353126459
	DD	1089946528
	DD	1364707661
	DD	1087849376
	DD	1210463930
	DD	1085447296
	DD	3372789207
	DD	1082708182
	DD	3852029519
	DD	1079794155
	DD	171026849
	DD	1092814590
	DD	171033738
	DD	1092814590
	DD	171026049
	DD	1091766014
	DD	228028830
	DD	1090084520
	DD	230450642
	DD	1087987368
	DD	3022136903
	DD	1085557689
	DD	525497389
	DD	1082855401
	DD	148126673
	DD	1079961851
	DD	3577093585
	DD	1092927450
	DD	3577099902
	DD	1092927450
	DD	3577091100
	DD	1091878874
	DD	474481216
	DD	1090235001
	DD	504620827
	DD	1088137849
	DD	2423209871
	DD	1085678074
	DD	1232450644
	DD	1083015858
	DD	77442837
	DD	1080089108
	DD	1719611516
	DD	1093050526
	DD	1719617308
	DD	1093050526
	DD	1719614085
	DD	1092001950
	DD	3724504891
	DD	1090399101
	DD	3667309025
	DD	1088301949
	DD	4276237595
	DD	1085809354
	DD	1482039118
	DD	1083185056
	DD	3967229838
	DD	1080190822
	DD	1110087291
	DD	1093184741
	DD	1110092603
	DD	1093184741
	DD	1110091391
	DD	1092136165
	DD	2171724537
	DD	1090548547
	DD	2129476069
	DD	1088451395
	DD	2339015690
	DD	1085952517
	DD	507750428
	DD	1083280533
	DD	815659794
	DD	1080299649
	DD	2572864042
	DD	1093331103
	DD	2572868913
	DD	1093331103
	DD	2572863425
	DD	1092282527
	DD	1715242544
	DD	1090646122
	DD	1712564357
	DD	1088548970
	DD	2011855921
	DD	1086108637
	DD	3152573735
	DD	1083384527
	DD	2153072651
	DD	1080417626
	DD	3707476942
	DD	1093490712
	DD	3707481409
	DD	1093490712
	DD	3707475642
	DD	1092442136
	DD	2471645489
	DD	1090752528
	DD	2474076499
	DD	1088655376
	DD	1676838604
	DD	1086278887
	DD	4157220213
	DD	1083498020
	DD	881611026
	DD	1080546966
	DD	4294965248
	DD	1093664767
	DD	1024
	DD	1093664768
	DD	4294965802
	DD	1092616191
	DD	1431656496
	DD	1090868565
	DD	1417488516
	DD	1088771413
	DD	298494524
	DD	1086394641
	DD	2083630489
	DD	1083621836
	DD	355810204
	DD	1080689044
	DD	1014844880
	DD	1093759672
	DD	1014846758
	DD	1093759672
	DD	1014843908
	DD	1092711096
	DD	1353122430
	DD	1090995104
	DD	1359405560
	DD	1088897952
	DD	1208032152
	DD	1086495872
	DD	3550721845
	DD	1083756770
	DD	3313961802
	DD	1080842760
	DD	171029432
	DD	1093863166
	DD	171031154
	DD	1093863166
	DD	171030626
	DD	1092814590
	DD	228049186
	DD	1091133096
	DD	196452232
	DD	1089035944
	DD	2633333922
	DD	1086606265
	DD	1145505718
	DD	1083904054
	DD	726399779
	DD	1081012148
	DD	3577095953
	DD	1093976026
	DD	3577097532
	DD	1093976026
	DD	3577100597
	DD	1092927450
	DD	474522872
	DD	1091283577
	DD	383058688
	DD	1089186425
	DD	1621696826
	DD	1086726650
	DD	425004090
	DD	1084064710
	DD	1746787218
	DD	1081139549
	DD	1719613688
	DD	1094099102
	DD	1719615136
	DD	1094099102
	DD	1719618236
	DD	1093050526
	DD	3724500239
	DD	1091447677
	DD	3633566020
	DD	1089350525
	DD	4261174779
	DD	1086857930
	DD	2691998327
	DD	1084233670
	DD	3441503036
	DD	1081239489
	DD	1110089283
	DD	1094233317
	DD	1110090611
	DD	1094233317
	DD	1110092918
	DD	1093184741
	DD	2171723412
	DD	1091597123
	DD	2133444349
	DD	1089499971
	DD	2342693282
	DD	1087001093
	DD	493648718
	DD	1084329100
	DD	2069066397
	DD	1081348203
	DD	2572865868
	DD	1094379679
	DD	2572867085
	DD	1094379679
	DD	2572870557
	DD	1093331103
	DD	1715264541
	DD	1091694698
	DD	1667327133
	DD	1089597546
	DD	1224262760
	DD	1087157213
	DD	1594852686
	DD	1084433206
	DD	260135111
	DD	1081467984
	DD	3707478617
	DD	1094539288
	DD	3707479734
	DD	1094539288
	DD	3707477486
	DD	1093490712
	DD	2471644294
	DD	1091801104
	DD	2472638675
	DD	1089703952
	DD	1675695734
	DD	1087327463
	DD	988559666
	DD	1084546600
	DD	2579358114
	DD	1081595549
	DD	4294966784
	DD	1094713343
	DD	256
	DD	1094713344
	DD	21
	DD	1093664768
	DD	1431655472
	DD	1091917141
	DD	1417489540
	DD	1089819989
	DD	298493705
	DD	1087443217
	DD	2083631581
	DD	1084670412
	DD	355801606
	DD	1081737620
	DD	1014845584
	DD	1094808248
	DD	1014846053
	DD	1094808248
	DD	1014846822
	DD	1093759672
	DD	1353145100
	DD	1092043680
	DD	1321720924
	DD	1089946528
	DD	817580875
	DD	1087544448
	DD	1414745249
	DD	1084805432
	DD	3336072390
	DD	1081893077
	DD	171030078
	DD	1094911742
	DD	171030509
	DD	1094911742
	DD	171029638
	DD	1093863166
	DD	228024203
	DD	1092181672
	DD	224317828
	DD	1090084520
	DD	3019441476
	DD	1087654841
	DD	190554934
	DD	1084952567
	DD	3683266053
	DD	1082059035
	DD	3577096546
	DD	1095024602
	DD	3577096941
	DD	1095024602
	DD	3577094443
	DD	1093976026
	DD	474476929
	DD	1092332153
	DD	498110689
	DD	1090235001
	DD	2420301076
	DD	1087775226
	DD	285764000
	DD	1085113025
	DD	2388641430
	DD	1082186277
	DD	1719614232
	DD	1095147678
	DD	1719614595
	DD	1095147678
	DD	1719610745
	DD	1094099102
	DD	3724429930
	DD	1092496253
	DD	3770584984
	DD	1090399101
	DD	1148342889
	DD	1087906507
	DD	809090904
	DD	1085282091
	DD	2331282844
	DD	1082285389
	DD	1110089781
	DD	1095281893
	DD	1110090113
	DD	1095281893
	DD	1110093300
	DD	1094233317
	DD	2171723132
	DD	1092645699
	DD	2134433374
	DD	1090548547
	DD	2343552424
	DD	1088049669
	DD	3745963458
	DD	1085377673
	DD	915246751
	DD	1082396774
	DD	2572866325
	DD	1095428255
	DD	2572866629
	DD	1095428255
	DD	2572868076
	DD	1094379679
	DD	1715252755
	DD	1092743274
	DD	1692376938
	DD	1090646122
	DD	1620253050
	DD	1088205789
	DD	2312135315
	DD	1085481725
	DD	546660203
	DD	1082515656
	DD	3707479036
	DD	1095587864
	DD	3707479316
	DD	1095587864
	DD	3707475815
	DD	1094539288
	DD	2471632157
	DD	1092849680
	DD	2490458773
	DD	1090752528
	DD	2065236278
	DD	1088376039
	DD	3434133632
	DD	1085595135
	DD	2573995247
	DD	1082643260
	DD	4294967168
	DD	1095761919
	DD	64
	DD	1095761920
	DD	213
	DD	1094713344
	DD	1431655216
	DD	1092965717
	DD	1417489796
	DD	1090868565
	DD	298493500
	DD	1088491793
	DD	2083631854
	DD	1085718988
	DD	355808637
	DD	1082786196
	DD	1014845760
	DD	1095856824
	DD	1014845877
	DD	1095856824
	DD	1014847550
	DD	1094808248
	DD	1353144371
	DD	1093092256
	DD	1312311946
	DD	1090995104
	DD	813476255
	DD	1088593024
	DD	2889757753
	DD	1085854029
	DD	4245160544
	DD	1082941703
	DD	171030239
	DD	1095960318
	DD	171030346
	DD	1095960318
	DD	171033655
	DD	1094911742
	DD	228071704
	DD	1093230248
	DD	158566009
	DD	1091133096
	DD	2242876999
	DD	1088703417
	DD	959541354
	DD	1086001292
	DD	3627031457
	DD	1083111041
	DD	3577096694
	DD	1096073178
	DD	3577096793
	DD	1096073178
	DD	3577095036
	DD	1095024602
	DD	474476331
	DD	1093380729
	DD	490526759
	DD	1091283577
	DD	2417038778
	DD	1088823802
	DD	1153252389
	DD	1086161618
	DD	2911272706
	DD	1083234873
	DD	1719614367
	DD	1096196254
	DD	1719614457
	DD	1096196254
	DD	1719617401
	DD	1095147678
	DD	3724500675
	DD	1093544829
	DD	3659385013
	DD	1091447677
	DD	4272740135
	DD	1088955082
	DD	1449988639
	DD	1086330793
	DD	710311906
	DD	1083336572
	DD	1110089906
	DD	1096330469
	DD	1110089989
	DD	1096330469
	DD	1110089132
	DD	1095281893
	DD	2171712172
	DD	1093694275
	DD	2171033651
	DD	1091597123
	DD	2749703086
	DD	1089098245
	DD	3587505039
	DD	1086426166
	DD	1886197718
	DD	1083444384
	DD	2572866439
	DD	1096476831
	DD	2572866515
	DD	1096476831
	DD	2572869588
	DD	1095428255
	DD	1715252060
	DD	1093791850
	DD	1680456788
	DD	1091694698
	DD	1609561203
	DD	1089254365
	DD	2509599475
	DD	1086530328
	DD	1964978055
	DD	1083564296
	DD	3707479140
	DD	1096636440
	DD	3707479210
	DD	1096636440
	DD	3707481794
	DD	1095587864
	DD	2471655053
	DD	1093898256
	DD	2440365996
	DD	1091801104
	DD	1273170287
	DD	1089424615
	DD	1952430961
	DD	1086643825
	DD	2795738593
	DD	1083693644
	DD	4294967264
	DD	1096810495
	DD	16
	DD	1096810496
	DD	261
	DD	1095761920
	DD	1431655152
	DD	1094014293
	DD	1417489860
	DD	1091917141
	DD	298493449
	DD	1089540369
	DD	2083631923
	DD	1086767564
	DD	355801227
	DD	1083834772
	DD	1014845804
	DD	1096905400
	DD	1014845833
	DD	1096905400
	DD	1014847733
	DD	1095856824
	DD	1353144197
	DD	1094140832
	DD	1309935339
	DD	1092043680
	DD	812248835
	DD	1089641600
	DD	314249476
	DD	1086902611
	DD	2610079324
	DD	1083990293
	DD	171030280
	DD	1097008894
	DD	171030307
	DD	1097008894
	DD	171028263
	DD	1095960318
	DD	228025330
	DD	1094278824
	DD	251211477
	DD	1092181672
	DD	3031505317
	DD	1089751993
	DD	107734986
	DD	1087049658
	DD	3034747437
	DD	1084156042
	DD	3577096731
	DD	1097121754
	DD	3577096756
	DD	1097121754
	DD	3577095185
	DD	1096073178
	DD	474476188
	DD	1094429305
	DD	488612505
	DD	1092332153
	DD	2416065154
	DD	1089872378
	DD	2651582387
	DD	1087210198
	DD	541517163
	DD	1084283455
	DD	1719614401
	DD	1097244830
	DD	1719614423
	DD	1097244830
	DD	1719616933
	DD	1096196254
	DD	3724501077
	DD	1094593405
	DD	3667944131
	DD	1092496253
	DD	4276519179
	DD	1090003658
	DD	2700442756
	DD	1087379359
	DD	931613300
	DD	1084385125
	DD	1110089937
	DD	1097379045
	DD	1110089958
	DD	1097379045
	DD	1110090221
	DD	1096330469
	DD	2171711677
	DD	1094742851
	DD	2162016345
	DD	1092645699
	DD	2741863994
	DD	1090146821
	DD	1245458209
	DD	1087474763
	DD	2097929072
	DD	1084493008
	DD	2572866468
	DD	1097525407
	DD	2572866487
	DD	1097525407
	DD	2572865702
	DD	1096476831
	DD	1715240994
	DD	1094840426
	DD	1713835862
	DD	1092743274
	DD	2012933276
	DD	1090302941
	DD	3665668716
	DD	1087578828
	DD	3415169568
	DD	1084611923
	DD	3707479166
	DD	1097685016
	DD	3707479183
	DD	1097685016
	DD	3707483288
	DD	1096636440
	DD	2471667167
	DD	1094946832
	DD	2427851938
	DD	1092849680
	DD	888456928
	DD	1090473191
	DD	3625621214
	DD	1087692429
	DD	3663259990
	DD	1084743056
	DD	4294967288
	DD	1097859071
	DD	4
	DD	1097859072
	DD	273
	DD	1096810496
	DD	1431655136
	DD	1095062869
	DD	1417489876
	DD	1092965717
	DD	298493436
	DD	1090588945
	DD	2083631940
	DD	1087816140
	DD	355808550
	DD	1084883348
	DD	1014845815
	DD	1097953976
	DD	1014845822
	DD	1097953976
	DD	1014847778
	DD	1096905400
	DD	1353144148
	DD	1095189408
	DD	1309359459
	DD	1093092256
	DD	812077300
	DD	1090690176
	DD	1610135328
	DD	1087951188
	DD	1252860071
	DD	1085038872
	DD	171030290
	DD	1098057470
	DD	171030297
	DD	1098057470
	DD	171029048
	DD	1097008894
	DD	228024638
	DD	1095327400
	DD	237989369
	DD	1093230248
	DD	3025415875
	DD	1090800569
	DD	138804687
	DD	1088098264
	DD	3643470356
	DD	1085204690
	DD	3577096740
	DD	1098170330
	DD	3577096746
	DD	1098170330
	DD	3577097354
	DD	1097121754
	DD	474499828
	DD	1095477881
	DD	451780922
	DD	1093380729
	DD	2026015950
	DD	1090920954
	DD	776452488
	DD	1088258858
	DD	1204131141
	DD	1085332899
	DD	1719614410
	DD	1098293406
	DD	1719614416
	DD	1098293406
	DD	1719612551
	DD	1097244830
	DD	3724453814
	DD	1095641981
	DD	3742826493
	DD	1093544829
	DD	762401611
	DD	1091052235
	DD	2907564309
	DD	1088427850
	DD	502993765
	DD	1085431961
	DD	1110089945
	DD	1098427621
	DD	1110089951
	DD	1098427621
	DD	1110088362
	DD	1097379045
	DD	2171699718
	DD	1095791427
	DD	2177932438
	DD	1093694275
	DD	3129583820
	DD	1091195397
	DD	780294918
	DD	1088523303
	DD	170683064
	DD	1085540730
	DD	2572866475
	DD	1098573983
	DD	2572866480
	DD	1098573983
	DD	2572866862
	DD	1097525407
	DD	1715240475
	DD	1095889002
	DD	1704007165
	DD	1093791850
	DD	2004279021
	DD	1091351517
	DD	647746018
	DD	1088627427
	DD	1886180147
	DD	1085660552
	DD	3707479173
	DD	1098733592
	DD	3707479178
	DD	1098733592
	DD	3707479398
	DD	1097685016
	DD	2471643324
	DD	1095995408
	DD	2461076443
	DD	1093898256
	DD	1665277865
	DD	1091521767
	DD	1998664073
	DD	1088740930
	DD	498206358
	DD	1085789916
	DD	4294967294
	DD	1098907647
	DD	1
	DD	1098907648
	DD	276
	DD	1097859072
	DD	1431655132
	DD	1096111445
	DD	1417489880
	DD	1094014293
	DD	298493433
	DD	1091637521
	DD	2083631944
	DD	1088864716
	DD	355801196
	DD	1085931924
	DD	1014845818
	DD	1099002552
	DD	1014845820
	DD	1099002552
	DD	1014845657
	DD	1097953976
	DD	1353120456
	DD	1096237984
	DD	1345580689
	DD	1094140832
	DD	1201937999
	DD	1091738752
	DD	824125633
	DD	1088999682
	DD	3691484490
	DD	1086085714
	DD	171030292
	DD	1099106046
	DD	171030293
	DD	1099106046
	DD	171033508
	DD	1098057470
	DD	228071817
	DD	1096375976
	DD	161971713
	DD	1094278824
	DD	2244267125
	DD	1091849145
	DD	2158827651
	DD	1089147012
	DD	600071589
	DD	1086256752
	DD	3577096742
	DD	1099218906
	DD	3577096743
	DD	1099218906
	DD	3577100028
	DD	1098170330
	DD	474523020
	DD	1096526457
	DD	406220006
	DD	1094429305
	DD	1632136669
	DD	1091969530
	DD	2352939251
	DD	1089307537
	DD	3345746864
	DD	1086382366
	DD	1719614412
	DD	1099341982
	DD	1719614414
	DD	1099341982
	DD	1719613588
	DD	1098293406
	DD	3724452896
	DD	1096690557
	DD	3725175790
	DD	1094593405
	DD	754459631
	DD	1092100811
	DD	3012112000
	DD	1089476446
	DD	3479767501
	DD	1086480584
	DD	1110089947
	DD	1099476197
	DD	1110089949
	DD	1099476197
	DD	1110087897
	DD	1098427621
	DD	2171699924
	DD	1096840003
	DD	2181914507
	DD	1094742851
	DD	3133129538
	DD	1092243973
	DD	629375254
	DD	1089571870
	DD	2644154789
	DD	1086589284
	DD	2572866477
	DD	1099622559
	DD	2572866479
	DD	1099622559
	DD	2572865020
	DD	1098573983
	DD	1715228507
	DD	1096937578
	DD	1719729545
	DD	1094840426
	DD	2391941724
	DD	1092400093
	DD	2057257972
	DD	1089675967
	DD	3221519396
	DD	1086708274
	DD	3707479175
	DD	1099782168
	DD	3707479177
	DD	1099782168
	DD	3707476293
	DD	1098733592
	DD	2471631915
	DD	1097043984
	DD	2487568215
	DD	1094946832
	DD	2062609080
	DD	1092570343
	DD	1539176086
	DD	1089789446
	DD	1226576577
	DD	1086837580
	DD	4294967295
	DD	1099956223
	DD	0
	DD	1099956224
	DD	2409
	DD	1098907648
	DD	1431666026
	DD	1097160021
	DD	1381124679
	DD	1095062869
	DD	95404972
	DD	1092686097
	DD	4267392907
	DD	1089913374
	DD	744273338
	DD	1086981465
	DD	1014845819
	DD	1100051128
	DD	1014845820
	DD	1100051128
	DD	1014842995
	DD	1099002552
	DD	1353097250
	DD	1097286560
	DD	1390989017
	DD	1095189408
	DD	1595792887
	DD	1092787328
	DD	729832970
	DD	1090048155
	DD	2003384808
	DD	1087132508
	DD	171030293
	DD	1100154622
	DD	171030294
	DD	1100154622
	DD	171030359
	DD	1099106046
	DD	228023473
	DD	1097424552
	DD	215685517
	DD	1095327400
	DD	3015470348
	DD	1092897721
	DD	2729851489
	DD	1090195466
	DD	4013155943
	DD	1087301962
	DD	3577096743
	DD	1100267482
	DD	3577096744
	DD	1100267482
	DD	3577096433
	DD	1099218906
	DD	474475076
	DD	1097575033
	DD	467535814
	DD	1095477881
	DD	2406643378
	DD	1093018106
	DD	1877814210
	DD	1090355974
	DD	3967364979
	DD	1087429239
	DD	1719614412
	DD	1100390558
	DD	1719614412
	DD	1100390558
	DD	1719618112
	DD	1099341982
	DD	3724500027
	DD	1097739133
	DD	3648026623
	DD	1095641981
	DD	4267608097
	DD	1093149386
	DD	961630931
	DD	1090525110
	DD	3904090335
	DD	1087530906
	DD	1110089947
	DD	1100524773
	DD	1110089947
	DD	1100524773
	DD	1110092045
	DD	1099476197
	DD	2171723653
	DD	1097888579
	DD	2146547869
	DD	1095791427
	DD	2354314515
	DD	1093292549
	DD	1667016450
	DD	1090620526
	DD	384884403
	DD	1087639589
	DD	2572866477
	DD	1100671135
	DD	2572866477
	DD	1100671135
	DD	2572868824
	DD	1099622559
	DD	1715252390
	DD	1097986154
	DD	1687294941
	DD	1095889002
	DD	1615685301
	DD	1093448669
	DD	298372850
	DD	1090724617
	DD	2476072344
	DD	1087758563
	DD	3707479175
	DD	1100830744
	DD	3707479175
	DD	1100830744
	DD	3707479781
	DD	1099782168
	DD	2471655937
	DD	1098092560
	DD	2457829004
	DD	1095995408
	DD	1288807310
	DD	1093618919
	DD	3573393579
	DD	1090838089
	DD	2043174239
	DD	1087887854
	DD	0
	DD	1101004800
	DD	0
	DD	1101004800
	DD	4294966783
	DD	1099956223
	DD	1431655600
	DD	1098208597
	DD	1426590319
	DD	1096111445
	DD	302639379
	DD	1093734673
	DD	3581289218
	DD	1090961847
	DD	2803312407
	DD	1088029026
	DD	1014845819
	DD	1101099704
	DD	1014845819
	DD	1101099704
	DD	1014844462
	DD	1100051128
	DD	1353121523
	DD	1098335136
	DD	1365969809
	DD	1096237984
	DD	1210888042
	DD	1093835904
	DD	4032953612
	DD	1091096787
	DD	3351349617
	DD	1088182757
	DD	171030293
	DD	1101203198
	DD	171030293
	DD	1101203198
	DD	171031703
	DD	1100154622
	DD	228047842
	DD	1098473128
	DD	192773132
	DD	1096375976
	DD	2631782123
	DD	1093946297
	DD	2631854033
	DD	1091244094
	DD	2315550364
	DD	1088352199
	DD	3577096743
	DD	1101316058
	DD	3577096743
	DD	1101316058
	DD	3577097666
	DD	1100267482
	DD	474499550
	DD	1098623609
	DD	446511749
	DD	1096526457
	DD	2023660504
	DD	1094066682
	DD	583015094
	DD	1091404598
	DD	2060630322
	DD	1088478641
	DD	1719614413
	DD	1101439134
	DD	1719614414
	DD	1101439134
	DD	1719610714
	DD	1100390558
	DD	3724429877
	DD	1098787709
	DD	3774200135
	DD	1096690557
	DD	1149951221
	DD	1094197963
	DD	376496729
	DD	1091573543
	DD	3520625071
	DD	1088576835
	DD	1110089947
	DD	1101573349
	DD	1110089947
	DD	1101573349
	DD	1110093082
	DD	1100524773
	DD	2171723193
	DD	1098937155
	DD	2137706208
	DD	1096840003
	DD	2346420191
	DD	1094341125
	DD	1926455040
	DD	1091669122
	DD	3075683983
	DD	1088688212
	DD	2572866477
	DD	1101719711
	DD	2572866477
	DD	1101719711
	DD	2572869775
	DD	1100671135
	DD	1715251969
	DD	1099034730
	DD	1679186288
	DD	1096937578
	DD	1608407888
	DD	1094497245
	DD	2006172809
	DD	1091773211
	DD	1497125046
	DD	1088807183
	DD	3707479175
	DD	1101879320
	DD	3707479175
	DD	1101879320
	DD	3707480653
	DD	1100830744
	DD	2471655550
	DD	1099141136
	DD	2450394200
	DD	1097043984
	DD	1282177669
	DD	1094667495
	DD	3008229769
	DD	1091886682
	DD	2158328475
	DD	1088936470
	DD	0
	DD	1102053376
	DD	0
	DD	1102053376
	DD	143
	DD	1101004800
	DD	1431655243
	DD	1099257173
	DD	1419780217
	DD	1097160021
	DD	299652155
	DD	1094783249
	DD	1211207709
	DD	1092010439
	DD	329765955
	DD	1089077639
	DD	1014845819
	DD	1102148280
	DD	1014845819
	DD	1102148280
	DD	1014844828
	DD	1101099704
	DD	1353121192
	DD	1099383712
	DD	1359733278
	DD	1097286560
	DD	1208235969
	DD	1094884480
	DD	356060089
	DD	1092145378
	DD	3107959578
	DD	1089231366
	DD	171030293
	DD	1102251774
	DD	171030293
	DD	1102251774
	DD	171032040
	DD	1101203198
	DD	228047551
	DD	1099521704
	DD	187020672
	DD	1097424552
	DD	2629046472
	DD	1094994873
	DD	2884342378
	DD	1092292683
	DD	1506755341
	DD	1089400807
	DD	3577096743
	DD	1102364634
	DD	3577096743
	DD	1102364634
	DD	3577097974
	DD	1101316058
	DD	474499274
	DD	1099672185
	DD	441261822
	DD	1097575033
	DD	2021374080
	DD	1095115258
	DD	190100264
	DD	1092453186
	DD	2056125696
	DD	1089527231
	DD	1719614413
	DD	1102487710
	DD	1719614414
	DD	1102487710
	DD	1719610997
	DD	1101439134
	DD	3724429628
	DD	1099836285
	DD	3769372221
	DD	1097739133
	DD	1147753795
	DD	1095246539
	DD	2430449986
	DD	1092622124
	DD	3974077509
	DD	1089625424
	DD	1110089947
	DD	1102621925
	DD	1110089947
	DD	1102621925
	DD	1110093341
	DD	1101573349
	DD	2171723077
	DD	1099985731
	DD	2135498839
	DD	1097888579
	DD	2344495521
	DD	1095389701
	DD	1956676740
	DD	1092717703
	DD	2117256085
	DD	1089736800
	DD	2572866477
	DD	1102768287
	DD	2572866477
	DD	1102768287
	DD	2572870013
	DD	1101719711
	DD	1715251864
	DD	1100083306
	DD	1677156080
	DD	1097986154
	DD	1606573745
	DD	1095545821
	DD	320249197
	DD	1092821792
	DD	1439390188
	DD	1089855770
	DD	3707479175
	DD	1102927896
	DD	3707479175
	DD	1102927896
	DD	3707480871
	DD	1101879320
	DD	2471655453
	DD	1100189712
	DD	2448535499
	DD	1098092560
	DD	1280531624
	DD	1095716071
	DD	3940680640
	DD	1092935262
	DD	2063813777
	DD	1089985056
	DD	0
	DD	1103101952
	DD	0
	DD	1103101952
	DD	243
	DD	1102053376
	DD	1431655155
	DD	1100305749
	DD	1418074647
	DD	1098208597
	DD	298875225
	DD	1095831825
	DD	653292726
	DD	1093059019
	DD	1465284600
	DD	1090126224
	DD	1014845819
	DD	1103196856
	DD	1014845819
	DD	1103196856
	DD	1014844920
	DD	1102148280
	DD	1353121114
	DD	1100432288
	DD	1358161964
	DD	1098335136
	DD	1207458112
	DD	1095933056
	DD	2796544215
	DD	1093193957
	DD	2424182090
	DD	1090279951
	DD	171030293
	DD	1103300350
	DD	171030293
	DD	1103300350
	DD	171032124
	DD	1102251774
	DD	228047476
	DD	1100570280
	DD	185588648
	DD	1098473128
	DD	2628417140
	DD	1096043449
	DD	3951958296
	DD	1093341262
	DD	66960268
	DD	1090449391
	DD	3577096743
	DD	1103413210
	DD	3577096743
	DD	1103413210
	DD	3577098051
	DD	1102364634
	DD	474499205
	DD	1100720761
	DD	439949341
	DD	1098623609
	DD	2020802481
	DD	1096163834
	DD	91857605
	DD	1093501765
	DD	4202353141
	DD	1090575810
	DD	1719614413
	DD	1103536286
	DD	1719614414
	DD	1103536286
	DD	1719611067
	DD	1102487710
	DD	3724429560
	DD	1100884861
	DD	3768183514
	DD	1098787709
	DD	1147345439
	DD	1096295115
	DD	3913822085
	DD	1093670701
	DD	3551604025
	DD	1090674003
	DD	1110089947
	DD	1103670501
	DD	1110089947
	DD	1103670501
	DD	1110093406
	DD	1102621925
	DD	2171723049
	DD	1101034307
	DD	2134943951
	DD	1098937155
	DD	2343965451
	DD	1096438277
	DD	3072602636
	DD	1093766280
	DD	2434876993
	DD	1090785379
	DD	2572866477
	DD	1103816863
	DD	2572866477
	DD	1103816863
	DD	2572870072
	DD	1102768287
	DD	1715251836
	DD	1101131882
	DD	1676654618
	DD	1099034730
	DD	1606201624
	DD	1096594397
	DD	903280726
	DD	1093870369
	DD	3655494515
	DD	1090904348
	DD	3707479175
	DD	1103976472
	DD	3707479175
	DD	1103976472
	DD	3707480926
	DD	1102927896
	DD	2471655431
	DD	1101238288
	DD	2448064733
	DD	1099141136
	DD	1280010955
	DD	1096764647
	DD	4243036701
	DD	1093983839
	DD	1130227930
	DD	1091033635
	DD	0
	DD	1104150528
	DD	0
	DD	1104150528
	DD	268
	DD	1103101952
	DD	1431655133
	DD	1101354325
	DD	1417648254
	DD	1099257173
	DD	298680988
	DD	1096880401
	DD	513823282
	DD	1094107596
	DD	2823073373
	DD	1091174802
	DD	1014845819
	DD	1104245432
	DD	1014845819
	DD	1104245432
	DD	1014844943
	DD	1103196856
	DD	1353121094
	DD	1101480864
	DD	1357769135
	DD	1099383712
	DD	1207275009
	DD	1096981632
	DD	2332932724
	DD	1094242534
	DD	3080521966
	DD	1091328529
	DD	171030293
	DD	1104348926
	DD	171030293
	DD	1104348926
	DD	171032145
	DD	1103300350
	DD	228047457
	DD	1101618856
	DD	185230641
	DD	1099521704
	DD	2628265482
	DD	1097092025
	DD	3145139053
	DD	1094389839
	DD	3878959594
	DD	1091497968
	DD	3577096743
	DD	1104461786
	DD	3577096743
	DD	1104461786
	DD	3577098071
	DD	1103413210
	DD	474499194
	DD	1101769337
	DD	439602948
	DD	1099672185
	DD	2020507204
	DD	1097212410
	DD	3496261741
	DD	1094550341
	DD	1103296215
	DD	1091624388
	DD	1719614413
	DD	1104584862
	DD	1719614414
	DD	1104584862
	DD	1719611085
	DD	1103536286
	DD	3724429547
	DD	1101933437
	DD	3767874156
	DD	1099836285
	DD	1147145559
	DD	1097343691
	DD	1132680656
	DD	1094719278
	DD	3486439013
	DD	1091722580
	DD	1110089947
	DD	1104719077
	DD	1110089947
	DD	1104719077
	DD	1110093422
	DD	1103670501
	DD	2171723041
	DD	1102082883
	DD	2134808274
	DD	1099985731
	DD	2343881823
	DD	1097486853
	DD	95743942
	DD	1094814857
	DD	883554237
	DD	1091833956
	DD	2572866477
	DD	1104865439
	DD	2572866477
	DD	1104865439
	DD	2572870087
	DD	1103816863
	DD	1715251830
	DD	1102180458
	DD	1676526208
	DD	1100083306
	DD	1606059712
	DD	1097642973
	DD	2157385827
	DD	1094918945
	DD	2618878781
	DD	1091952925
	DD	3707479175
	DD	1105025048
	DD	3707479175
	DD	1105025048
	DD	3707480939
	DD	1103976472
	DD	2471655422
	DD	1102286864
	DD	2447956178
	DD	1100189712
	DD	1280050214
	DD	1097813223
	DD	993528255
	DD	1095032416
	DD	2199985578
	DD	1092082211
	DD	0
	DD	1124597760
	DD	0
	DD	1124597760
	DD	0
	DD	1124597760
	DD	0
	DD	1124597760
	DD	0
	DD	1124597760
	DD	0
	DD	1124597760
	DD	0
	DD	1124597760
	DD	0
	DD	1124597760
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	1082535401
	DD	255
	DD	255
	DD	255
	DD	255
	DD	255
	DD	255
	DD	255
	DD	255
	DD	255
	DD	255
	DD	255
	DD	255
	DD	255
	DD	255
	DD	255
	DD	255
	DD	1
	DD	1124597760
	DD	1
	DD	1124597760
	DD	1
	DD	1124597760
	DD	1
	DD	1124597760
	DD	1
	DD	1124597760
	DD	1
	DD	1124597760
	DD	1
	DD	1124597760
	DD	1
	DD	1124597760
_vmldSinhHATab	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	1048019041
	DD	1072704666
	DD	1398474845
	DD	3161559171
	DD	3541402996
	DD	1072716208
	DD	2759177317
	DD	1015903202
	DD	410360776
	DD	1072727877
	DD	1269990655
	DD	1013024446
	DD	1828292879
	DD	1072739672
	DD	1255956747
	DD	1016636974
	DD	852742562
	DD	1072751596
	DD	667253587
	DD	1010842135
	DD	3490863953
	DD	1072763649
	DD	960797498
	DD	3163997456
	DD	2930322912
	DD	1072775834
	DD	2599499422
	DD	3163762623
	DD	1014845819
	DD	1072788152
	DD	3117910646
	DD	3162607681
	DD	3949972341
	DD	1072800603
	DD	2068408548
	DD	1015962444
	DD	828946858
	DD	1072813191
	DD	10642492
	DD	1016988014
	DD	2288159958
	DD	1072825915
	DD	2169144469
	DD	1015924597
	DD	1853186616
	DD	1072838778
	DD	3066496371
	DD	1016705150
	DD	1709341917
	DD	1072851781
	DD	2571168217
	DD	1015201075
	DD	4112506593
	DD	1072864925
	DD	2947355221
	DD	1015419624
	DD	2799960843
	DD	1072878213
	DD	1423655381
	DD	1016070727
	DD	171030293
	DD	1072891646
	DD	3526460132
	DD	1015477354
	DD	2992903935
	DD	1072905224
	DD	2218154406
	DD	1016276769
	DD	926591435
	DD	1072918951
	DD	3208833762
	DD	3163962090
	DD	887463927
	DD	1072932827
	DD	3596744163
	DD	3161842742
	DD	1276261410
	DD	1072946854
	DD	300981948
	DD	1015732745
	DD	569847338
	DD	1072961034
	DD	472945272
	DD	3160339305
	DD	1617004845
	DD	1072975368
	DD	82804944
	DD	1011391354
	DD	3049340112
	DD	1072989858
	DD	3062915824
	DD	1014219171
	DD	3577096743
	DD	1073004506
	DD	2951496418
	DD	1014842263
	DD	1990012071
	DD	1073019314
	DD	3529070563
	DD	3163861769
	DD	1453150082
	DD	1073034283
	DD	498154669
	DD	3162536638
	DD	917841882
	DD	1073049415
	DD	18715565
	DD	1016707884
	DD	3712504873
	DD	1073064711
	DD	88491949
	DD	1016476236
	DD	363667784
	DD	1073080175
	DD	813753950
	DD	1016833785
	DD	2956612997
	DD	1073095806
	DD	2118169751
	DD	3163784129
	DD	2186617381
	DD	1073111608
	DD	2270764084
	DD	3164321289
	DD	1719614413
	DD	1073127582
	DD	330458198
	DD	3164331316
	DD	1013258799
	DD	1073143730
	DD	1748797611
	DD	3161177658
	DD	3907805044
	DD	1073160053
	DD	2257091225
	DD	3162598983
	DD	1447192521
	DD	1073176555
	DD	1462857171
	DD	3163563097
	DD	1944781191
	DD	1073193236
	DD	3993278767
	DD	3162772855
	DD	919555682
	DD	1073210099
	DD	3121969534
	DD	1013996802
	DD	2571947539
	DD	1073227145
	DD	3558159064
	DD	3164425245
	DD	2604962541
	DD	1073244377
	DD	2614425274
	DD	3164587768
	DD	1110089947
	DD	1073261797
	DD	1451641639
	DD	1016523249
	DD	2568320822
	DD	1073279406
	DD	2732824428
	DD	1015401491
	DD	2966275557
	DD	1073297207
	DD	2176155324
	DD	3160891335
	DD	2682146384
	DD	1073315202
	DD	2082178513
	DD	3164411995
	DD	2191782032
	DD	1073333393
	DD	2960257726
	DD	1014791238
	DD	2069751141
	DD	1073351782
	DD	1562170675
	DD	3163773257
	DD	2990417245
	DD	1073370371
	DD	3683467745
	DD	3164417902
	DD	1434058175
	DD	1073389163
	DD	251133233
	DD	1016134345
	DD	2572866477
	DD	1073408159
	DD	878562433
	DD	1016570317
	DD	3092190715
	DD	1073427362
	DD	814012168
	DD	3160571998
	DD	4076559943
	DD	1073446774
	DD	2119478331
	DD	3161806927
	DD	2420883922
	DD	1073466398
	DD	2049810052
	DD	1015168464
	DD	3716502172
	DD	1073486235
	DD	2303740125
	DD	1015091301
	DD	777507147
	DD	1073506289
	DD	4282924205
	DD	1016236109
	DD	3706687593
	DD	1073526560
	DD	3521726939
	DD	1014301643
	DD	1242007932
	DD	1073547053
	DD	1132034716
	DD	3164388407
	DD	3707479175
	DD	1073567768
	DD	3613079303
	DD	1015213314
	DD	64696965
	DD	1073588710
	DD	1768797490
	DD	1016865536
	DD	863738719
	DD	1073609879
	DD	1326992220
	DD	3163661773
	DD	3884662774
	DD	1073631278
	DD	2158611599
	DD	1015258761
	DD	2728693978
	DD	1073652911
	DD	396109971
	DD	3164511267
	DD	3999357479
	DD	1073674779
	DD	2258941616
	DD	1016973300
	DD	1533953344
	DD	1073696886
	DD	769171851
	DD	1016714209
	DD	2174652632
	DD	1073719233
	DD	4087714590
	DD	1015498835
	DD	0
	DD	1073741824
	DD	0
	DD	0
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
	DD	1431655765
	DD	1069897045
	DD	286331153
	DD	1065423121
	DD	436314138
	DD	1059717536
	DD	2773927732
	DD	1053236707
	DD	1697350398
	DD	1079448903
	DD	0
	DD	1127743488
	DD	33554432
	DD	1101004800
	DD	2411329662
	DD	1082536910
	DD	4277796864
	DD	1065758274
	DD	3164486458
	DD	1025308570
	DD	1
	DD	1048576
	DD	4294967295
	DD	2146435071
	DD	3671843104
	DD	1067178892
	DD	3875694624
	DD	1077247184
	DD 2 DUP (0H)	
_2il0floatpacket_105	DD	000000000H,080000000H,000000000H,000000000H
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
