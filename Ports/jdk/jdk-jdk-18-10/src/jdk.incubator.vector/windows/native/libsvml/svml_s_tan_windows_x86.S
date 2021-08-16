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
	PUBLIC __svml_tanf4_ha_ex

__svml_tanf4_ha_ex	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 312
        movups    XMMWORD PTR [272+rsp], xmm14
        movups    XMMWORD PTR [208+rsp], xmm13
        pxor      xmm14, xmm14
        movups    XMMWORD PTR [256+rsp], xmm12
        movups    XMMWORD PTR [224+rsp], xmm11
        movups    XMMWORD PTR [240+rsp], xmm10
        movups    XMMWORD PTR [192+rsp], xmm7
        movaps    xmm7, xmm0
        mov       QWORD PTR [288+rsp], r13
        movaps    xmm10, xmm7
        movups    xmm4, XMMWORD PTR [__svml_stan_ha_data_internal+960]
        lea       r13, QWORD PTR [111+rsp]
        andps     xmm10, xmm4
        andnps    xmm4, xmm7
        movaps    xmm3, xmm10
        movaps    xmm5, xmm10
        cvtps2pd  xmm2, xmm10
        cmpnleps  xmm5, XMMWORD PTR [__svml_stan_ha_data_internal+1216]
        movhlps   xmm3, xmm10
        and       r13, -64
        mulps     xmm10, XMMWORD PTR [__svml_stan_ha_data_internal+832]
        cvtps2pd  xmm0, xmm3
        movmskps  eax, xmm5
        movups    xmm1, XMMWORD PTR [__svml_stan_ha_data_internal+1088]
        movups    xmm12, XMMWORD PTR [__svml_stan_ha_data_internal+1344]
        addps     xmm10, xmm1
        movaps    xmm5, xmm10
        subps     xmm10, xmm1
        cvtps2pd  xmm11, xmm10
        movhlps   xmm10, xmm10
        pslld     xmm5, 31
        cvtps2pd  xmm10, xmm10
        pcmpeqd   xmm14, xmm5
        pxor      xmm5, xmm4
        pshufd    xmm1, xmm14, 80
        pshufd    xmm3, xmm14, 250
        movups    xmm14, XMMWORD PTR [__svml_stan_ha_data_internal+1280]
        movaps    xmm13, xmm14
        mulpd     xmm13, xmm11
        mulpd     xmm14, xmm10
        mulpd     xmm11, xmm12
        mulpd     xmm10, xmm12
        subpd     xmm2, xmm13
        subpd     xmm0, xmm14
        subpd     xmm2, xmm11
        subpd     xmm0, xmm10
        movups    xmm13, XMMWORD PTR [__svml_stan_ha_data_internal+1472]
        movaps    xmm10, xmm2
        movaps    xmm11, xmm13
        movaps    xmm14, xmm0
        mulpd     xmm10, xmm2
        mulpd     xmm11, xmm2
        mulpd     xmm14, xmm0
        mulpd     xmm13, xmm0
        mulpd     xmm11, xmm10
        mulpd     xmm13, xmm14
        addpd     xmm2, xmm11
        addpd     xmm0, xmm13
        movups    xmm11, XMMWORD PTR [__svml_stan_ha_data_internal+1664]
        movaps    xmm13, xmm11
        mulpd     xmm13, xmm10
        mulpd     xmm11, xmm14
        movups    xmm12, XMMWORD PTR [__svml_stan_ha_data_internal+1600]
        addpd     xmm13, xmm12
        addpd     xmm12, xmm11
        mulpd     xmm10, xmm13
        mulpd     xmm14, xmm12
        movups    xmm11, XMMWORD PTR [__svml_stan_ha_data_internal+1536]
        movdqa    xmm12, xmm1
        addpd     xmm10, xmm11
        addpd     xmm11, xmm14
        movdqa    xmm14, xmm1
        andps     xmm12, xmm2
        andnps    xmm14, xmm10
        movdqa    xmm13, xmm3
        orps      xmm14, xmm12
        movdqa    xmm12, xmm3
        andnps    xmm13, xmm11
        andps     xmm12, xmm0
        orps      xmm13, xmm12
        movdqa    xmm12, xmm1
        andnps    xmm12, xmm2
        movdqa    xmm2, xmm3
        andps     xmm10, xmm1
        andnps    xmm2, xmm0
        andps     xmm11, xmm3
        orps      xmm12, xmm10
        orps      xmm2, xmm11
        divpd     xmm14, xmm12
        divpd     xmm13, xmm2
        cvtpd2ps  xmm0, xmm14
        cvtpd2ps  xmm1, xmm13
        movlhps   xmm0, xmm1
        mov       QWORD PTR [296+rsp], r13
        pxor      xmm0, xmm5
        test      eax, eax
        jne       _B1_3

_B1_2::

        movups    xmm7, XMMWORD PTR [192+rsp]
        movups    xmm10, XMMWORD PTR [240+rsp]
        movups    xmm11, XMMWORD PTR [224+rsp]
        movups    xmm12, XMMWORD PTR [256+rsp]
        movups    xmm13, XMMWORD PTR [208+rsp]
        movups    xmm14, XMMWORD PTR [272+rsp]
        mov       r13, QWORD PTR [288+rsp]
        add       rsp, 312
        ret

_B1_3::

        movups    XMMWORD PTR [r13], xmm7
        movups    XMMWORD PTR [64+r13], xmm0

_B1_6::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

_B1_7::

        mov       ecx, ebx
        mov       edx, 1
        shl       edx, cl
        test      esi, edx
        jne       _B1_10

_B1_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B1_7

_B1_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [64+r13]
        jmp       _B1_2

_B1_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_stan_ha_cout_rare_internal
        jmp       _B1_8
        ALIGN     16

_B1_11::

__svml_tanf4_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanf4_ha_ex_B1_B3:
	DD	1069057
	DD	2413648
	DD	817221
	DD	1026109
	DD	964660
	DD	1099819
	DD	907293
	DD	1173524
	DD	2556171

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_tanf4_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanf4_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_tanf4_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_6
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_tanf4_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanf4_ha_e9

__svml_tanf4_ha_e9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L20::

        sub       rsp, 232
        vmovaps   xmm5, xmm0
        vmovups   XMMWORD PTR [192+rsp], xmm14
        mov       QWORD PTR [208+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovups   xmm3, XMMWORD PTR [__svml_stan_ha_data_internal+960]
        and       r13, -64
        vandps    xmm14, xmm5, xmm3
        vandnps   xmm3, xmm3, xmm5
        vcmpnleps xmm4, xmm14, XMMWORD PTR [__svml_stan_ha_data_internal+1216]
        vcvtps2pd ymm1, xmm14
        vmovups   xmm2, XMMWORD PTR [__svml_stan_ha_data_internal+1088]
        vmovmskps edx, xmm4
        vmulps    xmm4, xmm14, XMMWORD PTR [__svml_stan_ha_data_internal+832]
        mov       QWORD PTR [216+rsp], r13
        vaddps    xmm0, xmm2, xmm4
        vpslld    xmm4, xmm0, 31
        vsubps    xmm0, xmm0, xmm2
        vpxor     xmm2, xmm2, xmm2
        vxorps    xmm3, xmm4, xmm3
        vpcmpeqd  xmm14, xmm4, xmm2
        vpshufd   xmm2, xmm14, 80
        vpshufd   xmm14, xmm14, 250
        vinsertf128 ymm2, ymm2, xmm14, 1
        vcvtps2pd ymm14, xmm0
        vmulpd    ymm0, ymm14, YMMWORD PTR [__svml_stan_ha_data_internal+1280]
        vmulpd    ymm14, ymm14, YMMWORD PTR [__svml_stan_ha_data_internal+1344]
        vsubpd    ymm1, ymm1, ymm0
        vsubpd    ymm14, ymm1, ymm14
        vmulpd    ymm0, ymm14, ymm14
        vmulpd    ymm1, ymm14, YMMWORD PTR [__svml_stan_ha_data_internal+1472]
        vmulpd    ymm1, ymm0, ymm1
        vaddpd    ymm1, ymm14, ymm1
        vmulpd    ymm14, ymm0, YMMWORD PTR [__svml_stan_ha_data_internal+1664]
        vaddpd    ymm14, ymm14, YMMWORD PTR [__svml_stan_ha_data_internal+1600]
        vmulpd    ymm0, ymm0, ymm14
        vaddpd    ymm0, ymm0, YMMWORD PTR [__svml_stan_ha_data_internal+1536]
        vblendvpd ymm14, ymm0, ymm1, ymm2
        vblendvpd ymm2, ymm1, ymm0, ymm2
        vdivpd    ymm0, ymm14, ymm2
        vcvtpd2ps xmm1, ymm0
        vxorps    xmm0, xmm1, xmm3
        test      edx, edx
        jne       _B2_3

_B2_2::

        vzeroupper
        vmovups   xmm14, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [208+rsp]
        add       rsp, 232
        ret

_B2_3::

        vmovups   XMMWORD PTR [r13], xmm5
        vmovups   XMMWORD PTR [64+r13], xmm0

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
        cmp       ebx, 4
        jl        _B2_7

_B2_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovups   xmm0, XMMWORD PTR [64+r13]
        jmp       _B2_2

_B2_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_stan_ha_cout_rare_internal
        jmp       _B2_8
        ALIGN     16

_B2_11::

__svml_tanf4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanf4_ha_e9_B1_B3:
	DD	401409
	DD	1758240
	DD	845848
	DD	1900811

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_tanf4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanf4_ha_e9_B6_B10:
	DD	658945
	DD	287758
	DD	340999
	DD	845824
	DD	1758208
	DD	1900800

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_6
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_tanf4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanf16_ha_z0

__svml_tanf16_ha_z0	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L29::

        sub       rsp, 1336
        mov       QWORD PTR [1320+rsp], r13
        lea       r13, QWORD PTR [1183+rsp]
        vmovups   zmm1, ZMMWORD PTR [__svml_stan_ha_data_internal]
        and       r13, -64
        vmovups   zmm23, ZMMWORD PTR [__svml_stan_ha_data_internal+1088]
        vmovups   zmm24, ZMMWORD PTR [__svml_stan_ha_data_internal+64]
        vmovups   zmm26, ZMMWORD PTR [__svml_stan_ha_data_internal+256]
        vmovups   zmm27, ZMMWORD PTR [__svml_stan_ha_data_internal+320]
        vmovups   zmm3, ZMMWORD PTR [__svml_stan_ha_data_internal+768]
        vmovups   zmm2, ZMMWORD PTR [__svml_stan_ha_data_internal+704]
        vmovups   zmm4, ZMMWORD PTR [__svml_stan_ha_data_internal+384]
        vmovaps   zmm22, zmm0
        vfmadd213ps zmm1, zmm22, zmm23 {rn-sae}
        vmovups   zmm0, ZMMWORD PTR [__svml_stan_ha_data_internal+640]
        vandps    zmm31, zmm22, ZMMWORD PTR [__svml_stan_ha_data_internal+960]
        vsubps    zmm30, zmm1, zmm23 {rn-sae}
        vpermt2ps zmm4, zmm1, ZMMWORD PTR [__svml_stan_ha_data_internal+448]
        vcmpps    k0, zmm31, zmm3, 22 {sae}
        vfnmadd213ps zmm24, zmm30, zmm22 {rn-sae}
        vmovups   zmm3, ZMMWORD PTR [__svml_stan_ha_data_internal+512]
        kmovw     r8d, k0
        vpermt2ps zmm3, zmm1, ZMMWORD PTR [__svml_stan_ha_data_internal+576]
        vmovaps   zmm25, zmm30
        vfnmadd213ps zmm25, zmm26, zmm24 {rn-sae}
        vmovaps   zmm5, zmm30
        vfnmadd213ps zmm5, zmm27, zmm25 {rn-sae}
        vsubps    zmm29, zmm24, zmm25 {rn-sae}
        vmulps    zmm31, zmm5, zmm5 {rn-sae}
        vfnmadd231ps zmm29, zmm26, zmm30 {rn-sae}
        vsubps    zmm28, zmm5, zmm25 {rn-sae}
        vfmadd231ps zmm0, zmm2, zmm31 {rn-sae}
        vfmadd213ps zmm30, zmm27, zmm28 {rn-sae}
        vmovups   zmm28, ZMMWORD PTR [__svml_stan_ha_data_internal+1152]
        vmulps    zmm24, zmm0, zmm5 {rn-sae}
        vsubps    zmm23, zmm29, zmm30 {rn-sae}
        vfnmsub213ps zmm24, zmm31, zmm23 {rn-sae}
        vsubps    zmm2, zmm5, zmm24 {rn-sae}
        vmulps    zmm30, zmm4, zmm2 {rn-sae}
        vsubps    zmm5, zmm5, zmm2 {rn-sae}
        vaddps    zmm1, zmm2, zmm4 {rn-sae}
        vsubps    zmm23, zmm28, zmm30 {rn-sae}
        vsubps    zmm0, zmm5, zmm24 {rn-sae}
        vsubps    zmm25, zmm1, zmm4 {rn-sae}
        vsubps    zmm29, zmm23, zmm28 {rn-sae}
        vsubps    zmm26, zmm2, zmm25 {rn-sae}
        vaddps    zmm28, zmm29, zmm30 {rn-sae}
        vaddps    zmm27, zmm26, zmm3 {rn-sae}
        vmovaps   zmm5, zmm4
        vfnmadd213ps zmm5, zmm2, zmm30 {rn-sae}
        vaddps    zmm24, zmm27, zmm0 {rn-sae}
        vsubps    zmm29, zmm28, zmm5 {rn-sae}
        vfmadd213ps zmm4, zmm0, zmm29 {rn-sae}
        vfmadd213ps zmm3, zmm2, zmm4 {rn-sae}
        vrcp14ps  zmm4, zmm23
        vmulps    zmm2, zmm4, zmm1 {rn-sae}
        vfmsub213ps zmm23, zmm2, zmm1 {rn-sae}
        vfnmadd213ps zmm3, zmm2, zmm23 {rn-sae}
        vsubps    zmm0, zmm3, zmm24 {rn-sae}
        mov       QWORD PTR [1328+rsp], r13
        vfnmadd213ps zmm0, zmm4, zmm2 {rn-sae}
        test      r8d, r8d
        jne       _B3_3

_B3_2::

        mov       r13, QWORD PTR [1320+rsp]
        add       rsp, 1336
        ret

_B3_3::

        vstmxcsr  DWORD PTR [1312+rsp]

_B3_4::

        movzx     edx, WORD PTR [1312+rsp]
        mov       eax, edx
        or        eax, 8064
        cmp       edx, eax
        je        _B3_6

_B3_5::

        mov       DWORD PTR [1312+rsp], eax
        vldmxcsr  DWORD PTR [1312+rsp]

_B3_6::

        vmovups   ZMMWORD PTR [r13], zmm22
        vmovups   ZMMWORD PTR [64+r13], zmm0
        test      r8d, r8d
        jne       _B3_11

_B3_7::

        cmp       edx, eax
        je        _B3_2

_B3_8::

        vstmxcsr  DWORD PTR [1312+rsp]
        mov       eax, DWORD PTR [1312+rsp]

_B3_9::

        and       eax, -8065
        or        eax, edx
        mov       DWORD PTR [1312+rsp], eax
        vldmxcsr  DWORD PTR [1312+rsp]
        jmp       _B3_2

_B3_11::

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

_B3_12::

        bt        edi, ebx
        jc        _B3_15

_B3_13::

        inc       ebx
        cmp       ebx, 16
        jl        _B3_12

_B3_14::

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
        jmp       _B3_7

_B3_15::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_stan_ha_cout_rare_internal
        jmp       _B3_13
        ALIGN     16

_B3_16::

__svml_tanf16_ha_z0 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanf16_ha_z0_B1_B9:
	DD	267009
	DD	10867731
	DD	10944779

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_11
	DD	imagerel _unwind___svml_tanf16_ha_z0_B1_B9

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanf16_ha_z0_B11_B15:
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

	DD	imagerel _B3_11
	DD	imagerel _B3_16
	DD	imagerel _unwind___svml_tanf16_ha_z0_B11_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanf8_ha_e9

__svml_tanf8_ha_e9	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L80::

        sub       rsp, 552
        vmovups   YMMWORD PTR [400+rsp], ymm15
        vpxor     xmm2, xmm2, xmm2
        vmovups   YMMWORD PTR [368+rsp], ymm14
        vmovups   YMMWORD PTR [336+rsp], ymm13
        vmovups   YMMWORD PTR [432+rsp], ymm12
        vmovups   YMMWORD PTR [464+rsp], ymm11
        vmovups   YMMWORD PTR [496+rsp], ymm10
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [239+rsp]
        vmovups   ymm11, YMMWORD PTR [__svml_stan_ha_data_internal+960]
        and       r13, -64
        vmovaps   ymm13, ymm0
        vandps    ymm12, ymm13, ymm11
        vcmpnle_uqps ymm3, ymm12, YMMWORD PTR [__svml_stan_ha_data_internal+1216]
        vandnps   ymm11, ymm11, ymm13
        mov       QWORD PTR [536+rsp], r13
        vextractf128 xmm0, ymm12, 1
        vmulps    ymm10, ymm12, YMMWORD PTR [__svml_stan_ha_data_internal+832]
        vcvtps2pd ymm15, xmm0
        vcvtps2pd ymm14, xmm12
        vmovups   ymm0, YMMWORD PTR [__svml_stan_ha_data_internal+1088]
        vextractf128 xmm5, ymm3, 1
        vpackssdw xmm4, xmm3, xmm5
        vaddps    ymm3, ymm0, ymm10
        vpacksswb xmm1, xmm4, xmm2
        vpmovmskb edx, xmm1
        vpslld    xmm1, xmm3, 31
        vextractf128 xmm5, ymm3, 1
        vsubps    ymm4, ymm3, ymm0
        vpslld    xmm5, xmm5, 31
        vpcmpeqd  xmm10, xmm5, xmm2
        vpcmpeqd  xmm0, xmm1, xmm2
        vinsertf128 ymm12, ymm1, xmm5, 1
        vpshufd   xmm1, xmm10, 80
        vpshufd   xmm10, xmm10, 250
        vpshufd   xmm2, xmm0, 80
        vpshufd   xmm3, xmm0, 250
        vxorps    ymm11, ymm12, ymm11
        vinsertf128 ymm10, ymm1, xmm10, 1
        vmovupd   ymm1, YMMWORD PTR [__svml_stan_ha_data_internal+1280]
        vinsertf128 ymm5, ymm2, xmm3, 1
        vcvtps2pd ymm3, xmm4
        vextractf128 xmm0, ymm4, 1
        vmulpd    ymm4, ymm3, ymm1
        vcvtps2pd ymm2, xmm0
        vsubpd    ymm0, ymm14, ymm4
        vmulpd    ymm14, ymm2, ymm1
        vsubpd    ymm4, ymm15, ymm14
        vmovupd   ymm14, YMMWORD PTR [__svml_stan_ha_data_internal+1344]
        vmulpd    ymm15, ymm3, ymm14
        vmovupd   ymm3, YMMWORD PTR [__svml_stan_ha_data_internal+1472]
        vsubpd    ymm1, ymm0, ymm15
        vmulpd    ymm0, ymm2, ymm14
        vmulpd    ymm15, ymm1, ymm1
        vsubpd    ymm2, ymm4, ymm0
        vmulpd    ymm4, ymm3, ymm1
        vmulpd    ymm0, ymm2, ymm2
        vmulpd    ymm3, ymm3, ymm2
        vmulpd    ymm4, ymm15, ymm4
        vaddpd    ymm4, ymm1, ymm4
        vmulpd    ymm1, ymm0, ymm3
        vaddpd    ymm3, ymm2, ymm1
        vmovupd   ymm2, YMMWORD PTR [__svml_stan_ha_data_internal+1664]
        vmovupd   ymm1, YMMWORD PTR [__svml_stan_ha_data_internal+1600]
        vmulpd    ymm14, ymm2, ymm15
        vmulpd    ymm2, ymm2, ymm0
        vaddpd    ymm14, ymm1, ymm14
        vaddpd    ymm1, ymm1, ymm2
        vmovupd   ymm2, YMMWORD PTR [__svml_stan_ha_data_internal+1536]
        vmulpd    ymm15, ymm15, ymm14
        vmulpd    ymm0, ymm0, ymm1
        vaddpd    ymm15, ymm2, ymm15
        vaddpd    ymm1, ymm2, ymm0
        vblendvpd ymm2, ymm15, ymm4, ymm5
        vblendvpd ymm5, ymm4, ymm15, ymm5
        vblendvpd ymm0, ymm1, ymm3, ymm10
        vblendvpd ymm10, ymm3, ymm1, ymm10
        vdivpd    ymm1, ymm2, ymm5
        vdivpd    ymm2, ymm0, ymm10
        vcvtpd2ps xmm0, ymm1
        vcvtpd2ps xmm3, ymm2
        vinsertf128 ymm4, ymm0, xmm3, 1
        vxorps    ymm0, ymm4, ymm11
        test      dl, dl
        jne       _B4_3

_B4_2::

        vmovups   ymm10, YMMWORD PTR [496+rsp]
        vmovups   ymm11, YMMWORD PTR [464+rsp]
        vmovups   ymm12, YMMWORD PTR [432+rsp]
        vmovups   ymm13, YMMWORD PTR [336+rsp]
        vmovups   ymm14, YMMWORD PTR [368+rsp]
        vmovups   ymm15, YMMWORD PTR [400+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B4_3::

        vmovups   YMMWORD PTR [r13], ymm13
        vmovups   YMMWORD PTR [64+r13], ymm0
        test      edx, edx
        je        _B4_2

_B4_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [128+rsp], ymm6
        vmovups   YMMWORD PTR [96+rsp], ymm7
        vmovups   YMMWORD PTR [64+rsp], ymm8
        vmovups   YMMWORD PTR [32+rsp], ymm9
        mov       QWORD PTR [168+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [160+rsp], rsi
        mov       esi, edx

_B4_7::

        bt        esi, ebx
        jc        _B4_10

_B4_8::

        inc       ebx
        cmp       ebx, 8
        jl        _B4_7

_B4_9::

        vmovups   ymm6, YMMWORD PTR [128+rsp]
        vmovups   ymm7, YMMWORD PTR [96+rsp]
        vmovups   ymm8, YMMWORD PTR [64+rsp]
        vmovups   ymm9, YMMWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [168+rsp]
        mov       rsi, QWORD PTR [160+rsp]
        jmp       _B4_2

_B4_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_stan_ha_cout_rare_internal
        jmp       _B4_8
        ALIGN     16

_B4_11::

__svml_tanf8_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanf8_ha_e9_B1_B3:
	DD	1068289
	DD	4379725
	DD	2074693
	DD	1947708
	DD	1820723
	DD	1431594
	DD	1566753
	DD	1701908
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_tanf8_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanf8_ha_e9_B6_B10:
	DD	798497
	DD	1336367
	DD	1389605
	DD	170013
	DD	296983
	DD	423953
	DD	550923
	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_tanf8_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_6
	DD	imagerel _B4_11
	DD	imagerel _unwind___svml_tanf8_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanf4_ha_l9

__svml_tanf4_ha_l9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L107::

        sub       rsp, 312
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [192+rsp], xmm15
        vmovups   XMMWORD PTR [208+rsp], xmm14
        vmovups   XMMWORD PTR [224+rsp], xmm13
        vmovups   XMMWORD PTR [240+rsp], xmm12
        vmovups   XMMWORD PTR [256+rsp], xmm11
        vmovups   XMMWORD PTR [272+rsp], xmm9
        mov       QWORD PTR [288+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovups   xmm15, XMMWORD PTR [__svml_stan_ha_data_internal]
        and       r13, -64
        vmovups   xmm3, XMMWORD PTR [__svml_stan_ha_data_internal+1088]
        vfmadd213ps xmm15, xmm0, xmm3
        vmovups   xmm1, XMMWORD PTR [__svml_stan_ha_data_internal+64]
        vandps    xmm2, xmm0, XMMWORD PTR [__svml_stan_ha_data_internal+960]
        vmovups   xmm4, XMMWORD PTR [__svml_stan_ha_data_internal+256]
        vcmpnleps xmm12, xmm2, XMMWORD PTR [__svml_stan_ha_data_internal+768]
        vsubps    xmm13, xmm15, xmm3
        vmovmskps r8d, xmm12
        vmovaps   xmm5, xmm13
        vpslld    xmm12, xmm15, 2
        vpand     xmm11, xmm12, XMMWORD PTR [_2il0floatpacket_26]
        vmovaps   xmm9, xmm13
        vfnmadd213ps xmm1, xmm13, xmm0
        vmovups   xmm2, XMMWORD PTR [__svml_stan_ha_data_internal+320]
        vmovd     ecx, xmm11
        vfnmadd213ps xmm5, xmm4, xmm1
        vpextrd   r9d, xmm11, 2
        vsubps    xmm1, xmm1, xmm5
        vfnmadd213ps xmm9, xmm2, xmm5
        vfnmadd231ps xmm1, xmm4, xmm13
        vsubps    xmm3, xmm9, xmm5
        movsxd    r9, r9d
        vpextrd   r10d, xmm11, 3
        movsxd    r10, r10d
        movsxd    rcx, ecx
        vpextrd   eax, xmm11, 1
        vmovd     xmm4, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+384+rdx+r9]
        movsxd    rax, eax
        vpinsrd   xmm12, xmm4, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+384+rdx+r10], 1
        vmulps    xmm4, xmm9, xmm9
        vfmadd213ps xmm13, xmm2, xmm3
        vmovd     xmm5, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+384+rdx+rcx]
        vmovd     xmm2, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+512+rdx+rcx]
        vmovd     xmm3, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+512+rdx+r9]
        vpinsrd   xmm15, xmm5, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+384+rdx+rax], 1
        vsubps    xmm14, xmm1, xmm13
        vpinsrd   xmm1, xmm2, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+512+rdx+rax], 1
        vpinsrd   xmm11, xmm3, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+512+rdx+r10], 1
        vmovups   xmm5, XMMWORD PTR [__svml_stan_ha_data_internal+704]
        vfmadd213ps xmm5, xmm4, XMMWORD PTR [__svml_stan_ha_data_internal+640]
        vunpcklpd xmm13, xmm15, xmm12
        vpunpcklqdq xmm12, xmm1, xmm11
        vmulps    xmm2, xmm9, xmm5
        vfnmsub213ps xmm2, xmm4, xmm14
        vmovapd   xmm4, xmm13
        vmovups   xmm1, XMMWORD PTR [__svml_stan_ha_data_internal+1152]
        mov       QWORD PTR [296+rsp], r13
        vsubps    xmm11, xmm9, xmm2
        vsubps    xmm9, xmm9, xmm11
        vsubps    xmm5, xmm9, xmm2
        vaddps    xmm9, xmm13, xmm11
        vsubps    xmm14, xmm9, xmm13
        vsubps    xmm2, xmm11, xmm14
        vmulps    xmm14, xmm13, xmm11
        vfnmadd213ps xmm4, xmm11, xmm14
        vaddps    xmm3, xmm12, xmm2
        vaddps    xmm2, xmm5, xmm3
        vsubps    xmm3, xmm1, xmm14
        vsubps    xmm15, xmm3, xmm1
        vaddps    xmm14, xmm14, xmm15
        vsubps    xmm4, xmm14, xmm4
        vfmadd213ps xmm13, xmm5, xmm4
        vrcpps    xmm5, xmm3
        vfmadd213ps xmm12, xmm11, xmm13
        vfnmadd231ps xmm1, xmm3, xmm5
        vfmadd231ps xmm1, xmm12, xmm5
        vfmadd213ps xmm5, xmm1, xmm5
        vmulps    xmm1, xmm9, xmm5
        vfmsub213ps xmm3, xmm1, xmm9
        vfnmadd213ps xmm12, xmm1, xmm3
        vsubps    xmm2, xmm12, xmm2
        vfnmadd213ps xmm2, xmm5, xmm1
        test      r8d, r8d
        jne       _B5_3

_B5_2::

        vmovups   xmm9, XMMWORD PTR [272+rsp]
        vmovaps   xmm0, xmm2
        vmovups   xmm11, XMMWORD PTR [256+rsp]
        vmovups   xmm12, XMMWORD PTR [240+rsp]
        vmovups   xmm13, XMMWORD PTR [224+rsp]
        vmovups   xmm14, XMMWORD PTR [208+rsp]
        vmovups   xmm15, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [288+rsp]
        add       rsp, 312
        ret

_B5_3::

        vmovups   XMMWORD PTR [r13], xmm0
        vmovups   XMMWORD PTR [64+r13], xmm2

_B5_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, r8d

_B5_7::

        bt        esi, ebx
        jc        _B5_10

_B5_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B5_7

_B5_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovups   xmm2, XMMWORD PTR [64+r13]
        jmp       _B5_2

_B5_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_stan_ha_cout_rare_internal
        jmp       _B5_8
        ALIGN     16

_B5_11::

__svml_tanf4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanf4_ha_l9_B1_B3:
	DD	1069057
	DD	2413648
	DD	1153096
	DD	1095743
	DD	1034294
	DD	972845
	DD	911396
	DD	849947
	DD	2556171

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_tanf4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanf4_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_tanf4_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_6
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_tanf4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanf8_ha_l9

__svml_tanf8_ha_l9	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L126::

        push      rsi
        push      r15
        push      rbp
        sub       rsp, 528
        lea       r10, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [320+rsp], ymm15
        vmovups   YMMWORD PTR [352+rsp], ymm14
        vmovups   YMMWORD PTR [384+rsp], ymm13
        vmovups   YMMWORD PTR [416+rsp], ymm12
        vmovups   YMMWORD PTR [448+rsp], ymm11
        vmovups   YMMWORD PTR [480+rsp], ymm10
        mov       QWORD PTR [512+rsp], r13
        lea       r13, QWORD PTR [223+rsp]
        vmovups   ymm15, YMMWORD PTR [__svml_stan_ha_data_internal]
        and       r13, -64
        vmovups   ymm4, YMMWORD PTR [__svml_stan_ha_data_internal+1088]
        vmovups   ymm2, YMMWORD PTR [__svml_stan_ha_data_internal+64]
        vmovups   ymm1, YMMWORD PTR [__svml_stan_ha_data_internal+256]
        vfmadd213ps ymm15, ymm0, ymm4
        vsubps    ymm13, ymm15, ymm4
        vfnmadd213ps ymm2, ymm13, ymm0
        vandps    ymm3, ymm0, YMMWORD PTR [__svml_stan_ha_data_internal+960]
        vcmpnle_uqps ymm12, ymm3, YMMWORD PTR [__svml_stan_ha_data_internal+768]
        vmovups   ymm3, YMMWORD PTR [__svml_stan_ha_data_internal+320]
        vmovaps   ymm5, ymm1
        vfnmadd213ps ymm5, ymm13, ymm2
        vmovaps   ymm10, ymm3
        vmovmskps ebp, ymm12
        vpslld    ymm12, ymm15, 2
        vfnmadd213ps ymm10, ymm13, ymm5
        vsubps    ymm2, ymm2, ymm5
        vpand     ymm11, ymm12, YMMWORD PTR [_2il0floatpacket_27]
        vsubps    ymm4, ymm10, ymm5
        vfnmadd231ps ymm2, ymm1, ymm13
        vfmadd213ps ymm13, ymm3, ymm4
        vsubps    ymm14, ymm2, ymm13
        mov       QWORD PTR [520+rsp], r13
        vextracti128 xmm4, ymm11, 1
        vmovd     r11d, xmm11
        vmovd     ecx, xmm4
        vpextrd   r8d, xmm11, 2
        vpextrd   eax, xmm4, 2
        movsxd    r11, r11d
        vpextrd   r9d, xmm11, 1
        movsxd    r8, r8d
        vpextrd   esi, xmm11, 3
        movsxd    rcx, ecx
        vpextrd   edx, xmm4, 1
        movsxd    rax, eax
        vpextrd   r15d, xmm4, 3
        movsxd    r9, r9d
        movsxd    rsi, esi
        movsxd    rdx, edx
        movsxd    r15, r15d
        vmovd     xmm5, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+384+r10+r11]
        vmovd     xmm1, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+384+r10+r8]
        vmovd     xmm2, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+384+r10+rcx]
        vmovd     xmm13, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+384+r10+rax]
        vpinsrd   xmm15, xmm5, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+384+r10+r9], 1
        vpinsrd   xmm12, xmm1, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+384+r10+rsi], 1
        vpinsrd   xmm5, xmm2, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+384+r10+rdx], 1
        vpinsrd   xmm1, xmm13, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+384+r10+r15], 1
        vpunpcklqdq xmm3, xmm15, xmm12
        vmovd     xmm12, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+512+r10+r11]
        vpunpcklqdq xmm15, xmm5, xmm1
        vpinsrd   xmm4, xmm12, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+512+r10+r9], 1
        vmovd     xmm5, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+512+r10+rcx]
        vmovd     xmm1, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+512+r10+rax]
        vpinsrd   xmm12, xmm1, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+512+r10+r15], 1
        vinsertf128 ymm13, ymm3, xmm15, 1
        vmovd     xmm3, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+512+r10+r8]
        vpinsrd   xmm2, xmm3, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+512+r10+rsi], 1
        vpinsrd   xmm15, xmm5, DWORD PTR [imagerel(__svml_stan_ha_data_internal)+512+r10+rdx], 1
        vpunpcklqdq xmm11, xmm4, xmm2
        vmulps    ymm2, ymm10, ymm10
        vmovups   ymm4, YMMWORD PTR [__svml_stan_ha_data_internal+704]
        vpunpcklqdq xmm3, xmm15, xmm12
        vfmadd213ps ymm4, ymm2, YMMWORD PTR [__svml_stan_ha_data_internal+640]
        vmovaps   ymm1, ymm13
        vinsertf128 ymm12, ymm11, xmm3, 1
        vmulps    ymm3, ymm10, ymm4
        vfnmsub213ps ymm3, ymm2, ymm14
        vmovups   ymm2, YMMWORD PTR [__svml_stan_ha_data_internal+1152]
        vsubps    ymm11, ymm10, ymm3
        vsubps    ymm10, ymm10, ymm11
        vsubps    ymm5, ymm10, ymm3
        vaddps    ymm10, ymm13, ymm11
        vsubps    ymm14, ymm10, ymm13
        vsubps    ymm3, ymm11, ymm14
        vmulps    ymm14, ymm13, ymm11
        vaddps    ymm4, ymm12, ymm3
        vfnmadd213ps ymm1, ymm11, ymm14
        vaddps    ymm3, ymm5, ymm4
        vsubps    ymm4, ymm2, ymm14
        vsubps    ymm15, ymm4, ymm2
        vaddps    ymm14, ymm14, ymm15
        vsubps    ymm1, ymm14, ymm1
        vfmadd213ps ymm13, ymm5, ymm1
        vrcpps    ymm5, ymm4
        vfmadd213ps ymm12, ymm11, ymm13
        vfnmadd231ps ymm2, ymm4, ymm5
        vfmadd231ps ymm2, ymm12, ymm5
        vfmadd213ps ymm5, ymm2, ymm5
        vmulps    ymm2, ymm10, ymm5
        vfmsub213ps ymm4, ymm2, ymm10
        vfnmadd213ps ymm12, ymm2, ymm4
        vsubps    ymm1, ymm12, ymm3
        vfnmadd213ps ymm1, ymm5, ymm2
        test      ebp, ebp
        jne       _B6_3

_B6_2::

        vmovups   ymm10, YMMWORD PTR [480+rsp]
        vmovups   ymm11, YMMWORD PTR [448+rsp]
        vmovups   ymm12, YMMWORD PTR [416+rsp]
        vmovups   ymm13, YMMWORD PTR [384+rsp]
        vmovups   ymm14, YMMWORD PTR [352+rsp]
        vmovups   ymm15, YMMWORD PTR [320+rsp]
        mov       r13, QWORD PTR [512+rsp]
        vmovdqa   ymm0, ymm1
        add       rsp, 528
        pop       rbp
        pop       r15
        pop       rsi
        ret

_B6_3::

        vmovups   YMMWORD PTR [r13], ymm0
        vmovups   YMMWORD PTR [64+r13], ymm1

_B6_6::

        vmovups   YMMWORD PTR [128+rsp], ymm6
        xor       esi, esi
        vmovups   YMMWORD PTR [96+rsp], ymm7
        vmovups   YMMWORD PTR [64+rsp], ymm8
        vmovups   YMMWORD PTR [32+rsp], ymm9

_B6_7::

        bt        ebp, esi
        jc        _B6_10

_B6_8::

        inc       esi
        cmp       esi, 8
        jl        _B6_7

_B6_9::

        vmovups   ymm6, YMMWORD PTR [128+rsp]
        vmovups   ymm7, YMMWORD PTR [96+rsp]
        vmovups   ymm8, YMMWORD PTR [64+rsp]
        vmovups   ymm9, YMMWORD PTR [32+rsp]
        vmovups   ymm1, YMMWORD PTR [64+r13]
        jmp       _B6_2

_B6_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rsi*4]
        lea       rdx, QWORD PTR [64+r13+rsi*4]

        call      __svml_stan_ha_cout_rare_internal
        jmp       _B6_8
        ALIGN     16

_B6_11::

__svml_tanf8_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanf8_ha_l9_B1_B3:
	DD	1266689
	DD	4248660
	DD	2009164
	DD	1882179
	DD	1755194
	DD	1628209
	DD	1501224
	DD	1374239
	DD	4325647
	DD	4027011080
	DD	24581

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_tanf8_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanf8_ha_l9_B6_B10:
	DD	531745
	DD	170013
	DD	296983
	DD	423953
	DD	550921
	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_tanf8_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_6
	DD	imagerel _B6_11
	DD	imagerel _unwind___svml_tanf8_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_stan_ha_cout_rare_internal

__svml_stan_ha_cout_rare_internal	PROC

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L149::

        push      rbp
        push      r15
        sub       rsp, 168
        lea       rbp, QWORD PTR [32+rsp]
        pxor      xmm1, xmm1
        movss     xmm0, DWORD PTR [rcx]
        mov       r15, rdx
        cvtss2sd  xmm1, xmm0
        movzx     r8d, WORD PTR [2+rcx]
        lea       r11, QWORD PTR [__ImageBase]
        movsd     QWORD PTR [8+rbp], xmm1
        and       r8d, 32640
        and       BYTE PTR [15+rbp], 127
        xor       eax, eax
        movups    XMMWORD PTR [96+rbp], xmm15
        movups    XMMWORD PTR [112+rbp], xmm14
        movups    XMMWORD PTR [80+rbp], xmm9
        cmp       r8d, 32640
        je        _B7_17

_B7_2::

        movzx     ecx, WORD PTR [14+rbp]
        mov       edx, ecx
        and       edx, 32752
        cmp       edx, 12336
        jl        _B7_13

_B7_3::

        cmp       ecx, 16585
        jge       _B7_5

_B7_4::

        movsd     xmm14, QWORD PTR [_vmldTanHATab+96]
        xor       r8d, r8d
        jmp       _B7_12

_B7_5::

        movsd     QWORD PTR [32+rbp], xmm1
        mov       dl, BYTE PTR [39+rbp]
        mov       r9b, dl
        and       dl, 127
        and       r9b, -128
        mov       BYTE PTR [39+rbp], dl
        movzx     ecx, WORD PTR [38+rbp]
        mov       edx, ecx
        and       edx, 32752
        shr       edx, 4
        shr       r9b, 7
        cmp       edx, 1053
        jge       _B7_10

_B7_6::

        movsd     xmm3, QWORD PTR [32+rbp]
        pxor      xmm2, xmm2
        movsd     xmm0, QWORD PTR [_vmlReductionTab+64]
        mulsd     xmm0, xmm3
        movsd     QWORD PTR [24+rbp], xmm0
        movzx     ecx, WORD PTR [30+rbp]
        mov       r8d, DWORD PTR [28+rbp]
        shr       ecx, 4
        or        r8d, -1048576
        mov       r10d, DWORD PTR [24+rbp]
        neg       ecx
        shl       r8d, 11
        add       ecx, 30
        shr       r10d, 21
        or        r8d, r10d
        shr       r8d, cl
        mov       r10d, r8d
        and       r10d, 1
        add       r10d, r8d
        cvtsi2sd  xmm2, r10d
        cmp       edx, 1046
        jge       _B7_8

_B7_7::

        movsd     xmm1, QWORD PTR [_vmlReductionTab+80]
        mulsd     xmm1, xmm2
        movsd     xmm4, QWORD PTR [_vmlReductionTab+88]
        subsd     xmm3, xmm1
        mulsd     xmm4, xmm2
        movsd     QWORD PTR [16+rbp], xmm3
        movsd     xmm1, QWORD PTR [16+rbp]
        movsd     xmm0, QWORD PTR [16+rbp]
        movsd     xmm14, QWORD PTR [_vmlReductionTab+96]
        subsd     xmm0, xmm4
        mulsd     xmm14, xmm2
        movsd     QWORD PTR [16+rbp], xmm0
        movsd     xmm3, QWORD PTR [16+rbp]
        movsd     xmm15, QWORD PTR [16+rbp]
        subsd     xmm1, xmm3
        movsd     xmm5, QWORD PTR [16+rbp]
        subsd     xmm1, xmm4
        subsd     xmm5, xmm14
        movsd     QWORD PTR [16+rbp], xmm5
        movsd     xmm9, QWORD PTR [16+rbp]
        movsd     xmm0, QWORD PTR [_vmlReductionTab+104]
        subsd     xmm15, xmm9
        mulsd     xmm2, xmm0
        subsd     xmm15, xmm14
        addsd     xmm1, xmm15
        subsd     xmm1, xmm2
        jmp       _B7_9

_B7_8::

        movsd     xmm1, QWORD PTR [_vmlReductionTab+112]
        mulsd     xmm1, xmm2
        movsd     xmm5, QWORD PTR [_vmlReductionTab+120]
        subsd     xmm3, xmm1
        mulsd     xmm5, xmm2
        movsd     QWORD PTR [16+rbp], xmm3
        movsd     xmm0, QWORD PTR [16+rbp]
        movsd     xmm3, QWORD PTR [16+rbp]
        movsd     xmm15, QWORD PTR [_vmlReductionTab+128]
        subsd     xmm3, xmm5
        mulsd     xmm15, xmm2
        movsd     QWORD PTR [16+rbp], xmm3
        movsd     xmm4, QWORD PTR [16+rbp]
        movsd     xmm1, QWORD PTR [16+rbp]
        subsd     xmm0, xmm4
        movsd     xmm9, QWORD PTR [16+rbp]
        subsd     xmm0, xmm5
        subsd     xmm9, xmm15
        movsd     QWORD PTR [16+rbp], xmm9
        movsd     xmm14, QWORD PTR [16+rbp]
        movsd     xmm9, QWORD PTR [_vmlReductionTab+136]
        subsd     xmm1, xmm14
        mulsd     xmm9, xmm2
        subsd     xmm1, xmm15
        movsd     xmm15, QWORD PTR [_vmlReductionTab+144]
        addsd     xmm0, xmm1
        mulsd     xmm2, xmm15
        movsd     xmm1, QWORD PTR [16+rbp]
        movsd     xmm3, QWORD PTR [16+rbp]
        addsd     xmm3, xmm0
        movsd     QWORD PTR [16+rbp], xmm3
        movsd     xmm4, QWORD PTR [16+rbp]
        movsd     xmm14, QWORD PTR [16+rbp]
        subsd     xmm1, xmm4
        addsd     xmm1, xmm0
        movsd     xmm0, QWORD PTR [16+rbp]
        subsd     xmm0, xmm9
        movsd     QWORD PTR [16+rbp], xmm0
        movsd     xmm5, QWORD PTR [16+rbp]
        subsd     xmm14, xmm5
        subsd     xmm14, xmm9
        addsd     xmm1, xmm14
        subsd     xmm1, xmm2

_B7_9::

        movzx     edx, r9b
        inc       r8d
        movsd     xmm14, QWORD PTR [16+rbp]
        shr       r8d, 1
        movsd     xmm0, QWORD PTR [imagerel(_vmlReductionTab)+16+r11+rdx*8]
        mulsd     xmm14, xmm0
        mulsd     xmm0, xmm1
        movsd     QWORD PTR [16+rbp], xmm14
        movsd     xmm14, QWORD PTR [16+rbp]
        movaps    xmm1, xmm14
        addsd     xmm1, xmm0
        subsd     xmm14, xmm1
        addsd     xmm14, xmm0
        jmp       _B7_12

_B7_10::

        and       ecx, -32753
        lea       r10d, DWORD PTR [-200+rdx]
        and       r10d, 2047
        lea       r8d, DWORD PTR [-1052+rdx]
        shl       r10d, 4
        or        ecx, r10d
        imul      r10d, r8d, 83886
        mov       WORD PTR [38+rbp], cx
        movsd     xmm1, QWORD PTR [32+rbp]
        mov       ecx, DWORD PTR [32+rbp]
        movsd     QWORD PTR [8+rbp], xmm1
        and       ecx, -134217728
        sar       r10d, 21
        mov       DWORD PTR [8+rbp], ecx
        movsxd    r10, r10d
        subsd     xmm1, QWORD PTR [8+rbp]
        imul      ecx, r10d, -25
        movsd     xmm0, QWORD PTR [imagerel(_vmlReductionTab)+160+r11+r10*8]
        mulsd     xmm0, xmm1
        lea       ecx, DWORD PTR [-1052+rcx+rdx]
        movsd     QWORD PTR [16+rbp], xmm0
        neg       ecx
        movsd     xmm0, QWORD PTR [8+rbp]
        add       ecx, 38
        movaps    xmm4, xmm0
        mulsd     xmm4, QWORD PTR [imagerel(_vmlReductionTab)+168+r11+r10*8]
        movsd     xmm2, QWORD PTR [16+rbp]
        movsd     xmm3, QWORD PTR [16+rbp]
        mov       rdx, QWORD PTR [_vmlReductionTab+152]
        addsd     xmm3, xmm4
        movsd     QWORD PTR [16+rbp], xmm3
        movsd     xmm15, QWORD PTR [16+rbp]
        movsd     xmm5, QWORD PTR [16+rbp]
        subsd     xmm2, xmm15
        shl       rdx, cl
        addsd     xmm2, xmm4
        movsd     xmm14, QWORD PTR [16+rbp]
        addsd     xmm5, xmm2
        movd      r8, xmm5
        movaps    xmm5, xmm0
        movsd     xmm4, QWORD PTR [imagerel(_vmlReductionTab)+168+r11+r10*8]
        mulsd     xmm4, xmm1
        mulsd     xmm5, QWORD PTR [imagerel(_vmlReductionTab)+184+r11+r10*8]
        and       r8, rdx
        mov       QWORD PTR [24+rbp], r8
        movzx     r9d, r9b
        subsd     xmm14, QWORD PTR [24+rbp]
        movsd     QWORD PTR [16+rbp], xmm14
        movsd     xmm14, QWORD PTR [16+rbp]
        movsd     xmm3, QWORD PTR [16+rbp]
        addsd     xmm3, xmm4
        movsd     QWORD PTR [16+rbp], xmm3
        movaps    xmm3, xmm0
        movsd     xmm9, QWORD PTR [16+rbp]
        mulsd     xmm3, QWORD PTR [imagerel(_vmlReductionTab)+176+r11+r10*8]
        subsd     xmm14, xmm9
        movsd     xmm9, QWORD PTR [16+rbp]
        addsd     xmm14, xmm4
        movsd     xmm4, QWORD PTR [imagerel(_vmlReductionTab)+176+r11+r10*8]
        addsd     xmm14, xmm2
        mulsd     xmm4, xmm1
        movsd     xmm2, QWORD PTR [16+rbp]
        addsd     xmm2, xmm3
        movsd     QWORD PTR [16+rbp], xmm2
        movsd     xmm15, QWORD PTR [16+rbp]
        subsd     xmm9, xmm15
        addsd     xmm9, xmm3
        addsd     xmm9, xmm14
        movsd     xmm14, QWORD PTR [16+rbp]
        movsd     xmm2, QWORD PTR [16+rbp]
        addsd     xmm14, xmm9
        addsd     xmm14, xmm4
        addsd     xmm14, xmm5
        movsd     QWORD PTR [24+rbp], xmm14
        movzx     ecx, WORD PTR [30+rbp]
        shr       ecx, 4
        movd      rdx, xmm14
        neg       ecx
        add       ecx, 51
        shr       rdx, cl
        mov       r8d, edx
        shl       rdx, cl
        mov       QWORD PTR [24+rbp], rdx
        mov       edx, r8d
        and       edx, 1
        inc       r8d
        shr       r8d, 1
        subsd     xmm2, QWORD PTR [24+rbp]
        movsd     QWORD PTR [16+rbp], xmm2
        movsd     xmm3, QWORD PTR [16+rbp]
        movsd     xmm15, QWORD PTR [16+rbp]
        addsd     xmm15, xmm9
        movsd     QWORD PTR [16+rbp], xmm15
        movsd     xmm14, QWORD PTR [16+rbp]
        subsd     xmm3, xmm14
        addsd     xmm3, xmm9
        movsd     xmm9, QWORD PTR [16+rbp]
        addsd     xmm9, QWORD PTR [imagerel(_vmlReductionTab)+r11+rdx*8]
        movsd     QWORD PTR [16+rbp], xmm9
        movsd     xmm2, QWORD PTR [16+rbp]
        movsd     xmm14, QWORD PTR [16+rbp]
        addsd     xmm14, xmm4
        movsd     QWORD PTR [16+rbp], xmm14
        movsd     xmm9, QWORD PTR [16+rbp]
        movsd     xmm14, QWORD PTR [16+rbp]
        subsd     xmm2, xmm9
        addsd     xmm2, xmm4
        movsd     xmm4, QWORD PTR [16+rbp]
        addsd     xmm4, xmm5
        addsd     xmm2, xmm3
        movsd     QWORD PTR [16+rbp], xmm4
        movsd     xmm3, QWORD PTR [16+rbp]
        movsd     xmm15, QWORD PTR [16+rbp]
        subsd     xmm14, xmm3
        movsd     xmm3, QWORD PTR [imagerel(_vmlReductionTab)+184+r11+r10*8]
        addsd     xmm14, xmm5
        mulsd     xmm3, xmm1
        addsd     xmm14, xmm2
        movsd     xmm5, QWORD PTR [16+rbp]
        addsd     xmm5, xmm3
        movsd     QWORD PTR [16+rbp], xmm5
        movsd     xmm2, QWORD PTR [16+rbp]
        movsd     xmm9, QWORD PTR [16+rbp]
        subsd     xmm15, xmm2
        movaps    xmm2, xmm0
        addsd     xmm15, xmm3
        mulsd     xmm2, QWORD PTR [imagerel(_vmlReductionTab)+192+r11+r10*8]
        addsd     xmm15, xmm14
        movsd     xmm14, QWORD PTR [16+rbp]
        addsd     xmm14, xmm2
        movsd     QWORD PTR [16+rbp], xmm14
        movsd     xmm4, QWORD PTR [16+rbp]
        movsd     xmm5, QWORD PTR [16+rbp]
        subsd     xmm9, xmm4
        addsd     xmm9, xmm2
        movsd     xmm2, QWORD PTR [imagerel(_vmlReductionTab)+192+r11+r10*8]
        mulsd     xmm2, xmm1
        addsd     xmm9, xmm15
        movsd     xmm15, QWORD PTR [16+rbp]
        addsd     xmm15, xmm2
        movsd     QWORD PTR [16+rbp], xmm15
        movsd     xmm14, QWORD PTR [16+rbp]
        movsd     xmm4, QWORD PTR [16+rbp]
        subsd     xmm5, xmm14
        movaps    xmm14, xmm0
        addsd     xmm5, xmm2
        mulsd     xmm14, QWORD PTR [imagerel(_vmlReductionTab)+200+r11+r10*8]
        addsd     xmm5, xmm9
        movsd     xmm3, QWORD PTR [16+rbp]
        movaps    xmm2, xmm0
        movsd     xmm15, QWORD PTR [imagerel(_vmlReductionTab)+200+r11+r10*8]
        addsd     xmm3, xmm14
        mulsd     xmm2, QWORD PTR [imagerel(_vmlReductionTab)+208+r11+r10*8]
        mulsd     xmm15, xmm1
        movsd     QWORD PTR [16+rbp], xmm3
        addsd     xmm15, xmm2
        movsd     xmm9, QWORD PTR [16+rbp]
        movsd     xmm3, QWORD PTR [16+rbp]
        subsd     xmm4, xmm9
        movsd     xmm9, QWORD PTR [imagerel(_vmlReductionTab)+208+r11+r10*8]
        addsd     xmm4, xmm14
        mulsd     xmm9, xmm1
        addsd     xmm4, xmm5
        movsd     xmm5, QWORD PTR [16+rbp]
        addsd     xmm5, xmm15
        movsd     QWORD PTR [16+rbp], xmm5
        movsd     xmm14, QWORD PTR [16+rbp]
        movsd     xmm2, QWORD PTR [16+rbp]
        subsd     xmm3, xmm14
        movsd     xmm14, QWORD PTR [16+rbp]
        addsd     xmm3, xmm15
        addsd     xmm3, xmm4
        movaps    xmm4, xmm0
        mulsd     xmm4, QWORD PTR [imagerel(_vmlReductionTab)+216+r11+r10*8]
        mulsd     xmm0, QWORD PTR [imagerel(_vmlReductionTab)+224+r11+r10*8]
        addsd     xmm9, xmm4
        addsd     xmm14, xmm9
        movsd     QWORD PTR [16+rbp], xmm14
        movsd     xmm5, QWORD PTR [16+rbp]
        movsd     xmm14, QWORD PTR [16+rbp]
        subsd     xmm2, xmm5
        addsd     xmm2, xmm9
        addsd     xmm2, xmm3
        movsd     xmm3, QWORD PTR [imagerel(_vmlReductionTab)+216+r11+r10*8]
        mulsd     xmm1, xmm3
        addsd     xmm1, xmm0
        movsd     xmm0, QWORD PTR [16+rbp]
        addsd     xmm0, xmm1
        movsd     QWORD PTR [16+rbp], xmm0
        movsd     xmm4, QWORD PTR [16+rbp]
        mulsd     xmm4, QWORD PTR [_vmlReductionTab+32]
        movsd     xmm3, QWORD PTR [16+rbp]
        addsd     xmm3, xmm4
        movsd     QWORD PTR [rbp], xmm3
        movsd     xmm5, QWORD PTR [rbp]
        subsd     xmm5, xmm4
        movsd     QWORD PTR [16+rbp], xmm5
        movsd     xmm0, QWORD PTR [16+rbp]
        subsd     xmm14, xmm0
        addsd     xmm14, xmm1
        movsd     xmm1, QWORD PTR [16+rbp]
        addsd     xmm14, xmm2
        movsd     xmm2, QWORD PTR [_vmlReductionTab+48]
        mulsd     xmm2, xmm14
        addsd     xmm14, xmm1
        mulsd     xmm14, QWORD PTR [_vmlReductionTab+56]
        addsd     xmm2, xmm14
        movsd     xmm14, QWORD PTR [16+rbp]
        mulsd     xmm14, QWORD PTR [_vmlReductionTab+48]
        movsd     QWORD PTR [16+rbp], xmm14
        movsd     xmm14, QWORD PTR [16+rbp]
        movsd     xmm0, QWORD PTR [16+rbp]
        addsd     xmm0, xmm2
        movsd     QWORD PTR [16+rbp], xmm0
        movsd     xmm1, QWORD PTR [16+rbp]
        subsd     xmm14, xmm1
        movsd     xmm1, QWORD PTR [16+rbp]
        mulsd     xmm1, QWORD PTR [imagerel(_vmlReductionTab)+16+r11+r9*8]
        addsd     xmm14, xmm2
        mulsd     xmm14, QWORD PTR [imagerel(_vmlReductionTab)+16+r11+r9*8]

_B7_12::

        movsd     xmm9, QWORD PTR [_vmldTanHATab]
        mulsd     xmm9, xmm1
        movsd     xmm3, QWORD PTR [_vmldTanHATab+8]
        movsd     xmm0, QWORD PTR [_vmldTanHATab+16]
        addsd     xmm3, xmm9
        addsd     xmm9, xmm0
        movsd     QWORD PTR [40+rbp], xmm3
        movsd     xmm3, QWORD PTR [40+rbp]
        movsd     QWORD PTR [48+rbp], xmm9
        movsd     xmm9, QWORD PTR [48+rbp]
        movsd     xmm15, QWORD PTR [_vmldTanHATab+48]
        movsd     xmm2, QWORD PTR [_vmldTanHATab+56]
        movsd     xmm5, QWORD PTR [_vmldTanHATab+64]
        movsd     xmm4, QWORD PTR [_vmldTanHATab+24]
        movsd     xmm0, QWORD PTR [_vmldTanHATab+40]
        shl       r8d, 4
        subsd     xmm3, QWORD PTR [_vmldTanHATab+8]
        subsd     xmm9, QWORD PTR [_vmldTanHATab+16]
        mulsd     xmm15, xmm3
        mulsd     xmm2, xmm3
        mulsd     xmm3, xmm5
        mulsd     xmm4, xmm9
        movsd     xmm5, QWORD PTR [_vmldTanHATab+32]
        mulsd     xmm5, xmm9
        mulsd     xmm9, xmm0
        movaps    xmm0, xmm1
        subsd     xmm1, xmm4
        subsd     xmm0, xmm15
        movaps    xmm15, xmm0
        add       r8d, DWORD PTR [40+rbp]
        subsd     xmm15, xmm2
        and       r8d, 31
        subsd     xmm0, xmm15
        subsd     xmm0, xmm2
        movaps    xmm2, xmm1
        subsd     xmm2, xmm5
        subsd     xmm0, xmm3
        subsd     xmm1, xmm2
        addsd     xmm0, xmm14
        subsd     xmm1, xmm5
        movaps    xmm3, xmm2
        lea       edx, DWORD PTR [r8+r8*4]
        shl       edx, 2
        subsd     xmm1, xmm9
        addsd     xmm1, xmm14
        movaps    xmm14, xmm15
        addsd     xmm14, xmm0
        addsd     xmm3, xmm1
        movsd     QWORD PTR [56+rbp], xmm14
        movsd     xmm4, QWORD PTR [56+rbp]
        subsd     xmm15, xmm4
        movsd     QWORD PTR [64+rbp], xmm15
        movsd     xmm15, QWORD PTR [64+rbp]
        movsd     xmm14, QWORD PTR [56+rbp]
        addsd     xmm15, xmm0
        movsd     QWORD PTR [56+rbp], xmm3
        movaps    xmm5, xmm14
        movsd     xmm0, QWORD PTR [56+rbp]
        movsd     xmm3, QWORD PTR [_vmldTanHATab+88]
        subsd     xmm2, xmm0
        mulsd     xmm5, xmm14
        movsd     QWORD PTR [64+rbp], xmm2
        movaps    xmm9, xmm3
        movsd     xmm4, QWORD PTR [64+rbp]
        addsd     xmm4, xmm1
        movsd     xmm1, QWORD PTR [56+rbp]
        movsd     QWORD PTR [8+rbp], xmm1
        mov       r8d, DWORD PTR [12+rbp]
        mov       ecx, DWORD PTR [8+rbp]
        and       ecx, -262144
        mov       DWORD PTR [24+rbp], ecx
        mov       DWORD PTR [28+rbp], r8d
        movsd     xmm0, QWORD PTR [24+rbp]
        divsd     xmm9, xmm0
        movsd     QWORD PTR [32+rbp], xmm9
        subsd     xmm1, xmm0
        mov       r9d, DWORD PTR [32+rbp]
        addsd     xmm4, xmm1
        mov       r10d, DWORD PTR [36+rbp]
        and       r9d, DWORD PTR [imagerel(_vmldTanHATab)+280+r11+rdx*8]
        and       r10d, DWORD PTR [imagerel(_vmldTanHATab)+284+r11+rdx*8]
        mov       DWORD PTR [72+rbp], r9d
        mov       DWORD PTR [76+rbp], r10d
        movsd     xmm2, QWORD PTR [72+rbp]
        mulsd     xmm0, xmm2
        mulsd     xmm4, xmm9
        subsd     xmm0, xmm3
        movsd     xmm1, QWORD PTR [imagerel(_vmldTanHATab)+272+r11+rdx*8]
        addsd     xmm0, xmm4
        mulsd     xmm1, xmm9
        movsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+224+r11+rdx*8]
        mulsd     xmm4, xmm14
        mulsd     xmm1, xmm0
        addsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+208+r11+rdx*8]
        mulsd     xmm4, xmm14
        movsd     xmm3, QWORD PTR [imagerel(_vmldTanHATab)+264+r11+rdx*8]
        movsd     xmm2, QWORD PTR [imagerel(_vmldTanHATab)+256+r11+rdx*8]
        addsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+192+r11+rdx*8]
        mulsd     xmm4, xmm14
        addsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+176+r11+rdx*8]
        mulsd     xmm4, xmm14
        addsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+160+r11+rdx*8]
        mulsd     xmm4, xmm14
        addsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+144+r11+rdx*8]
        mulsd     xmm4, xmm14
        addsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+136+r11+rdx*8]
        mulsd     xmm4, xmm14
        addsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+128+r11+rdx*8]
        mulsd     xmm4, xmm14
        addsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+232+r11+rdx*8]
        mulsd     xmm4, xmm14
        addsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+216+r11+rdx*8]
        mulsd     xmm4, xmm14
        addsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+200+r11+rdx*8]
        mulsd     xmm4, xmm14
        addsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+184+r11+rdx*8]
        mulsd     xmm4, xmm14
        addsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+168+r11+rdx*8]
        mulsd     xmm4, xmm14
        addsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+152+r11+rdx*8]
        mulsd     xmm5, xmm4
        movaps    xmm4, xmm14
        mulsd     xmm14, xmm3
        mulsd     xmm4, xmm2
        mulsd     xmm3, xmm15
        mulsd     xmm2, xmm15
        movaps    xmm0, xmm14
        addsd     xmm0, xmm4
        movsd     QWORD PTR [56+rbp], xmm0
        movsd     xmm9, QWORD PTR [56+rbp]
        subsd     xmm4, xmm9
        movsd     QWORD PTR [64+rbp], xmm4
        movsd     xmm9, QWORD PTR [56+rbp]
        movsd     xmm0, QWORD PTR [64+rbp]
        movsd     xmm4, QWORD PTR [imagerel(_vmldTanHATab)+240+r11+rdx*8]
        addsd     xmm0, xmm14
        subsd     xmm4, QWORD PTR [72+rbp]
        addsd     xmm0, xmm5
        movaps    xmm5, xmm9
        addsd     xmm5, xmm4
        movsd     QWORD PTR [56+rbp], xmm5
        movsd     xmm14, QWORD PTR [56+rbp]
        subsd     xmm4, xmm14
        movsd     QWORD PTR [64+rbp], xmm4
        movsd     xmm14, QWORD PTR [56+rbp]
        movsd     xmm5, QWORD PTR [64+rbp]
        addsd     xmm9, xmm5
        addsd     xmm0, xmm9
        movsd     QWORD PTR [56+rbp], xmm0
        movsd     xmm0, QWORD PTR [56+rbp]
        addsd     xmm0, xmm1
        movsd     QWORD PTR [64+rbp], xmm0
        movsd     xmm1, QWORD PTR [64+rbp]
        addsd     xmm1, xmm3
        movsd     QWORD PTR [56+rbp], xmm1
        movsd     xmm3, QWORD PTR [56+rbp]
        addsd     xmm3, QWORD PTR [imagerel(_vmldTanHATab)+248+r11+rdx*8]
        movsd     QWORD PTR [64+rbp], xmm3
        movsd     xmm9, QWORD PTR [64+rbp]
        addsd     xmm9, xmm2
        addsd     xmm9, xmm14
        cvtsd2ss  xmm9, xmm9
        movss     DWORD PTR [r15], xmm9
        jmp       _B7_16

_B7_13::

        ucomisd   xmm1, QWORD PTR [_vmldTanHATab+96]
        jp        _B7_14
        je        _B7_15

_B7_14::

        movsd     xmm0, QWORD PTR [_vmldTanHATab+112]
        mulsd     xmm0, xmm1
        subsd     xmm0, xmm1
        cvtsd2ss  xmm0, xmm0
        cvtss2sd  xmm0, xmm0
        mulsd     xmm0, QWORD PTR [_vmldTanHATab+120]
        cvtsd2ss  xmm0, xmm0
        movss     DWORD PTR [r15], xmm0
        jmp       _B7_16

_B7_15::

        movss     DWORD PTR [r15], xmm0

_B7_16::

        movups    xmm9, XMMWORD PTR [80+rbp]
        movups    xmm14, XMMWORD PTR [112+rbp]
        movups    xmm15, XMMWORD PTR [96+rbp]
        lea       rsp, QWORD PTR [136+rbp]
        pop       r15
        pop       rbp
        ret

_B7_17::

        cmp       DWORD PTR [8+rbp], 0
        jne       _B7_20

_B7_18::

        cmp       DWORD PTR [12+rbp], 2146435072
        jne       _B7_20

_B7_19::

        movsd     xmm0, QWORD PTR [_vmldTanHATab+96]
        mov       eax, 1
        mulsd     xmm1, xmm0
        cvtsd2ss  xmm1, xmm1
        movss     DWORD PTR [r15], xmm1
        jmp       _B7_16

_B7_20::

        mulss     xmm0, xmm0
        movss     DWORD PTR [r15], xmm0
        jmp       _B7_16
        ALIGN     16

_B7_21::

__svml_stan_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_stan_ha_cout_rare_internal_B1_B20:
	DD	621498113
	DD	497743
	DD	649290
	DD	587845
	DD	17695507
	DD	4026990613
	DD	20485

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_21
	DD	imagerel _unwind___svml_stan_ha_cout_rare_internal_B1_B20

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_stan_ha_data_internal
__svml_stan_ha_data_internal	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1092811139
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	1036586970
	DD	832708968
	DD	832708968
	DD	832708968
	DD	832708968
	DD	832708968
	DD	832708968
	DD	832708968
	DD	832708968
	DD	832708968
	DD	832708968
	DD	832708968
	DD	832708968
	DD	832708968
	DD	832708968
	DD	832708968
	DD	832708968
	DD	633484485
	DD	633484485
	DD	633484485
	DD	633484485
	DD	633484485
	DD	633484485
	DD	633484485
	DD	633484485
	DD	633484485
	DD	633484485
	DD	633484485
	DD	633484485
	DD	633484485
	DD	633484485
	DD	633484485
	DD	633484485
	DD	832708608
	DD	832708608
	DD	832708608
	DD	832708608
	DD	832708608
	DD	832708608
	DD	832708608
	DD	832708608
	DD	832708608
	DD	832708608
	DD	832708608
	DD	832708608
	DD	832708608
	DD	832708608
	DD	832708608
	DD	832708608
	DD	708075802
	DD	708075802
	DD	708075802
	DD	708075802
	DD	708075802
	DD	708075802
	DD	708075802
	DD	708075802
	DD	708075802
	DD	708075802
	DD	708075802
	DD	708075802
	DD	708075802
	DD	708075802
	DD	708075802
	DD	708075802
	DD	2147483648
	DD	1036629468
	DD	1045147567
	DD	1050366018
	DD	1054086093
	DD	1057543609
	DD	1059786177
	DD	1062344705
	DD	1065353216
	DD	1067186156
	DD	1069519047
	DD	1072658590
	DD	1075479162
	DD	1079179983
	DD	1084284919
	DD	1092776803
	DD	4286578687
	DD	3240260451
	DD	3231768567
	DD	3226663631
	DD	3222962810
	DD	3220142238
	DD	3217002695
	DD	3214669804
	DD	3212836864
	DD	3209828353
	DD	3207269825
	DD	3205027257
	DD	3201569741
	DD	3197849666
	DD	3192631215
	DD	3184113116
	DD	2147483648
	DD	826651354
	DD	791306928
	DD	2989111746
	DD	2982175258
	DD	2992568675
	DD	850100121
	DD	850281093
	DD	0
	DD	861435400
	DD	840342808
	DD	3003924160
	DD	3016492578
	DD	865099790
	DD	856723932
	DD	3025444934
	DD	4085252096
	DD	877961286
	DD	3004207580
	DD	3012583438
	DD	869008930
	DD	856440512
	DD	2987826456
	DD	3008919048
	DD	0
	DD	2997764741
	DD	2997583769
	DD	845085027
	DD	834691610
	DD	841628098
	DD	2938790576
	DD	2974135002
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1051372198
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1040758920
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1174470656
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	1059256707
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483648
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2147483647
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	2139095040
	DD	1262485504
	DD	1262485504
	DD	1262485504
	DD	1262485504
	DD	1262485504
	DD	1262485504
	DD	1262485504
	DD	1262485504
	DD	1262485504
	DD	1262485504
	DD	1262485504
	DD	1262485504
	DD	1262485504
	DD	1262485504
	DD	1262485504
	DD	1262485504
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
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1176256512
	DD	1413742592
	DD	1073291771
	DD	1413742592
	DD	1073291771
	DD	1413742592
	DD	1073291771
	DD	1413742592
	DD	1073291771
	DD	1413742592
	DD	1073291771
	DD	1413742592
	DD	1073291771
	DD	1413742592
	DD	1073291771
	DD	1413742592
	DD	1073291771
	DD	1280075305
	DD	1032227875
	DD	1280075305
	DD	1032227875
	DD	1280075305
	DD	1032227875
	DD	1280075305
	DD	1032227875
	DD	1280075305
	DD	1032227875
	DD	1280075305
	DD	1032227875
	DD	1280075305
	DD	1032227875
	DD	1280075305
	DD	1032227875
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
	DD	116221458
	DD	3216541303
	DD	116221458
	DD	3216541303
	DD	116221458
	DD	3216541303
	DD	116221458
	DD	3216541303
	DD	116221458
	DD	3216541303
	DD	116221458
	DD	3216541303
	DD	116221458
	DD	3216541303
	DD	116221458
	DD	3216541303
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
	DD	3891301393
	DD	3218831093
	DD	3891301393
	DD	3218831093
	DD	3891301393
	DD	3218831093
	DD	3891301393
	DD	3218831093
	DD	3891301393
	DD	3218831093
	DD	3891301393
	DD	3218831093
	DD	3891301393
	DD	3218831093
	DD	3891301393
	DD	3218831093
	DD	2796173298
	DD	1065608814
	DD	2796173298
	DD	1065608814
	DD	2796173298
	DD	1065608814
	DD	2796173298
	DD	1065608814
	DD	2796173298
	DD	1065608814
	DD	2796173298
	DD	1065608814
	DD	2796173298
	DD	1065608814
	DD	2796173298
	DD	1065608814
	DD	1070141403
	DD	3007036718
	DD	0
	DD	0
	DD	0
	DD	1065353216
	DD	0
	DD	0
	DD	1051372765
	DD	0
	DD	1069935515
	DD	853435276
	DD	0
	DD	1019812401
	DD	797871386
	DD	1065353216
	DD	975043072
	DD	1019820333
	DD	1051400329
	DD	1015569723
	DD	1069729628
	DD	2999697034
	DD	0
	DD	1028208956
	DD	816029531
	DD	1065353216
	DD	991832832
	DD	1028240852
	DD	1051479824
	DD	1023251493
	DD	1069523740
	DD	860164016
	DD	0
	DD	1033310670
	DD	827321128
	DD	1065353216
	DD	1001540608
	DD	1033364538
	DD	1051617929
	DD	1028458464
	DD	1069317853
	DD	2977958621
	DD	0
	DD	1036629468
	DD	826649990
	DD	1065353216
	DD	1008660256
	DD	1036757738
	DD	1051807326
	DD	1032162226
	DD	1069111966
	DD	3009745511
	DD	0
	DD	1039964354
	DD	2964214364
	DD	1065353216
	DD	1014578464
	DD	1040201797
	DD	1052059423
	DD	1034708638
	DD	1068906078
	DD	848017692
	DD	0
	DD	1041753444
	DD	2982519524
	DD	1065353216
	DD	1018446032
	DD	1041972480
	DD	1052374628
	DD	1037453248
	DD	1068700191
	DD	3004118141
	DD	0
	DD	1043443277
	DD	2985501265
	DD	1065353216
	DD	1022797056
	DD	1043793882
	DD	1052746889
	DD	1039915463
	DD	1068494303
	DD	857455223
	DD	0
	DD	1045147567
	DD	791292384
	DD	1065353216
	DD	1025642520
	DD	1045675728
	DD	1053195814
	DD	1041590498
	DD	1068288416
	DD	2992986704
	DD	0
	DD	1046868583
	DD	833925599
	DD	1065353216
	DD	1028557712
	DD	1047628490
	DD	1053716836
	DD	1043186017
	DD	1068082528
	DD	863082593
	DD	0
	DD	1048592340
	DD	2988940902
	DD	1065353216
	DD	1031831496
	DD	1049119700
	DD	1054310701
	DD	1044788971
	DD	1067876641
	DD	837040812
	DD	0
	DD	1049473154
	DD	2972885556
	DD	1065353216
	DD	1033689040
	DD	1050184288
	DD	1054999523
	DD	1046698028
	DD	1067670754
	DD	3006826934
	DD	0
	DD	1050366018
	DD	2989112046
	DD	1065353216
	DD	1035760784
	DD	1051302645
	DD	1055777031
	DD	1048635818
	DD	1067464866
	DD	853854846
	DD	0
	DD	1051272279
	DD	817367088
	DD	1065353216
	DD	1038057984
	DD	1052482025
	DD	1056656040
	DD	1049723582
	DD	1067258979
	DD	2999277465
	DD	0
	DD	1052193360
	DD	2986510371
	DD	1065353216
	DD	1040390392
	DD	1053730424
	DD	1057307751
	DD	1050943059
	DD	1067053091
	DD	860373800
	DD	0
	DD	1053130765
	DD	2987705281
	DD	1065353216
	DD	1041784404
	DD	1055056706
	DD	1057868403
	DD	1052298273
	DD	1066847204
	DD	2974604846
	DD	0
	DD	1054086093
	DD	2982175058
	DD	1065353216
	DD	1043312844
	DD	1056470731
	DD	1058502663
	DD	1053852727
	DD	1066641317
	DD	3009535726
	DD	0
	DD	1055061049
	DD	2985572766
	DD	1065353216
	DD	1044984860
	DD	1057474074
	DD	1059214863
	DD	1055565854
	DD	1066435429
	DD	848437261
	DD	0
	DD	1056057456
	DD	844263924
	DD	1065353216
	DD	1046810746
	DD	1058286064
	DD	1060014844
	DD	1057227928
	DD	1066229542
	DD	3003908357
	DD	0
	DD	1057020941
	DD	2987700082
	DD	1065353216
	DD	1048689044
	DD	1059160627
	DD	1060914481
	DD	1058313864
	DD	1066023654
	DD	857665008
	DD	0
	DD	1057543609
	DD	2992568718
	DD	1065353216
	DD	1049773965
	DD	1060105673
	DD	1061932376
	DD	1059565214
	DD	1065817767
	DD	2992147565
	DD	0
	DD	1058080175
	DD	854607280
	DD	1065353216
	DD	1050955490
	DD	1061130203
	DD	1063075792
	DD	1060964899
	DD	1065611879
	DD	863292377
	DD	0
	DD	1058631876
	DD	848316488
	DD	1065353216
	DD	1052241912
	DD	1062244476
	DD	1064374250
	DD	1062608877
	DD	1065405992
	DD	838719090
	DD	0
	DD	1059200055
	DD	2987155932
	DD	1065353216
	DD	1053642609
	DD	1063460266
	DD	1065596017
	DD	1064468970
	DD	1065046993
	DD	848647046
	DD	0
	DD	1059786177
	DD	850099898
	DD	1065353216
	DD	1055168194
	DD	1064791104
	DD	1066427841
	DD	1065988022
	DD	1064635218
	DD	854274415
	DD	0
	DD	1060391849
	DD	2998448362
	DD	1065353216
	DD	1056830711
	DD	1065802920
	DD	1067373883
	DD	1067237086
	DD	1064223444
	DD	2998857895
	DD	0
	DD	1061018831
	DD	852661766
	DD	1073741824
	DD	3202769007
	DD	1066608086
	DD	1068453481
	DD	1068697612
	DD	1063811669
	DD	2991727995
	DD	0
	DD	1061669068
	DD	2986407194
	DD	1073741824
	DD	3200789612
	DD	1067498217
	DD	1069688111
	DD	1070408903
	DD	1063399894
	DD	2971248290
	DD	0
	DD	1062344705
	DD	850280824
	DD	1073741824
	DD	3198626104
	DD	1068485666
	DD	1071103306
	DD	1072410651
	DD	1062988119
	DD	839209514
	DD	0
	DD	1063048126
	DD	826671880
	DD	1073741824
	DD	3196257989
	DD	1069584946
	DD	1072731698
	DD	1074256640
	DD	1062576344
	DD	848856831
	DD	0
	DD	1063781982
	DD	845614362
	DD	1073741824
	DD	3191263702
	DD	1070813191
	DD	1074178145
	DD	1075661786
	DD	1062164569
	DD	854484200
	DD	0
	DD	1064549237
	DD	855412877
	DD	1073741824
	DD	3183449264
	DD	1072190735
	DD	1075269479
	DD	1077331464
	DD	1061752795
	DD	2998648110
	DD	1065353216
	DD	3196839438
	DD	839748996
	DD	1056964608
	DD	3187152817
	DD	3179496939
	DD	1025375660
	DD	3159543663
	DD	1061341020
	DD	2991308426
	DD	1065353216
	DD	3196528703
	DD	2993207654
	DD	1056964608
	DD	3187565865
	DD	3178961235
	DD	1025040649
	DD	3158667440
	DD	1060929245
	DD	2969570013
	DD	1065353216
	DD	3196220448
	DD	839617357
	DD	1048576000
	DD	1039897640
	DD	3178234548
	DD	1024731756
	DD	3157936127
	DD	1060517470
	DD	839629084
	DD	1065353216
	DD	3195769474
	DD	2972943314
	DD	1048576000
	DD	1039520273
	DD	3177530035
	DD	1024452069
	DD	3157392148
	DD	1060105695
	DD	849066615
	DD	1065353216
	DD	3195162227
	DD	824230882
	DD	1048576000
	DD	1039159939
	DD	3176846430
	DD	1024176063
	DD	3156719803
	DD	1059693920
	DD	854693985
	DD	1065353216
	DD	3194559300
	DD	837912886
	DD	1048576000
	DD	1038816139
	DD	3176182519
	DD	1023917626
	DD	3156100775
	DD	1059282146
	DD	2998438326
	DD	1065353216
	DD	3193960492
	DD	2976936506
	DD	1048576000
	DD	1038488404
	DD	3175537158
	DD	1023672824
	DD	3155484691
	DD	1058870371
	DD	2990888857
	DD	1065353216
	DD	3193365611
	DD	837021697
	DD	1048576000
	DD	1038176293
	DD	3174909264
	DD	1023428141
	DD	3154717848
	DD	1058458596
	DD	2966216238
	DD	1065353216
	DD	3192774465
	DD	2981011604
	DD	1048576000
	DD	1037879388
	DD	3174297790
	DD	1023026096
	DD	3154246903
	DD	1058046821
	DD	840048653
	DD	1065353216
	DD	3192186872
	DD	2982847435
	DD	1048576000
	DD	1037597300
	DD	3173701765
	DD	1022609285
	DD	3153191752
	DD	1057635046
	DD	849276400
	DD	1065353216
	DD	3191602652
	DD	2972865050
	DD	1048576000
	DD	1037329660
	DD	3173120241
	DD	1022242934
	DD	3152466531
	DD	1057223271
	DD	854903769
	DD	1065353216
	DD	3191021630
	DD	838792638
	DD	1048576000
	DD	1037076124
	DD	3172552332
	DD	1021893801
	DD	3151682133
	DD	1056658385
	DD	840258438
	DD	1065353216
	DD	3190443633
	DD	2979855596
	DD	1048576000
	DD	1036836369
	DD	3171997189
	DD	1021543079
	DD	3150495127
	DD	1055834836
	DD	2990469287
	DD	1065353216
	DD	3189868496
	DD	2981611511
	DD	1048576000
	DD	1036610091
	DD	3171453986
	DD	1021220110
	DD	3149437649
	DD	1055011286
	DD	2962859682
	DD	1065353216
	DD	3189296055
	DD	2950857776
	DD	1048576000
	DD	1036397006
	DD	3170921933
	DD	1020942892
	DD	3148919762
	DD	1054187736
	DD	840468223
	DD	1065353216
	DD	3188726149
	DD	2955915960
	DD	1048576000
	DD	1036196851
	DD	3169906765
	DD	1020660679
	DD	3147905210
	DD	1053364187
	DD	2990259502
	DD	1065353216
	DD	3188158621
	DD	2978622083
	DD	1048576000
	DD	1036009378
	DD	3168882838
	DD	1020421234
	DD	3147436656
	DD	1052540637
	DD	2961181405
	DD	1065353216
	DD	3187515595
	DD	789904544
	DD	1048576000
	DD	1035834359
	DD	3167876891
	DD	1020189995
	DD	3146799430
	DD	1051717087
	DD	840678007
	DD	1065353216
	DD	3186389132
	DD	2974324164
	DD	1048576000
	DD	1035671582
	DD	3166887590
	DD	1019957287
	DD	3145677161
	DD	1050893538
	DD	2990049718
	DD	1065353216
	DD	3185266517
	DD	821445502
	DD	1048576000
	DD	1035520850
	DD	3165913616
	DD	1019751749
	DD	3143905397
	DD	1050069988
	DD	2957827630
	DD	1065353216
	DD	3184147455
	DD	823956970
	DD	1048576000
	DD	1035381982
	DD	3164953691
	DD	1019591684
	DD	3143870825
	DD	1049246438
	DD	840887792
	DD	1065353216
	DD	3183031657
	DD	2948197632
	DD	1048576000
	DD	1035254815
	DD	3164006661
	DD	1019406069
	DD	3141406886
	DD	1048269777
	DD	831869830
	DD	1065353216
	DD	3181918839
	DD	829265530
	DD	1048576000
	DD	1035139196
	DD	3163071263
	DD	1019275107
	DD	3141473894
	DD	1046622678
	DD	2954471074
	DD	1065353216
	DD	3180808717
	DD	2974758491
	DD	1048576000
	DD	1035034991
	DD	3161787608
	DD	1019131285
	DD	3139614851
	DD	1044975579
	DD	2981870894
	DD	1065353216
	DD	3179701015
	DD	2951749952
	DD	1048576000
	DD	1034942077
	DD	3159956688
	DD	1019002541
	DD	3137649644
	DD	1043328479
	DD	832289399
	DD	1065353216
	DD	3177908479
	DD	2968441398
	DD	1048576000
	DD	1034860345
	DD	3158142289
	DD	1018906717
	DD	3137336762
	DD	1041681380
	DD	2949439022
	DD	1065353216
	DD	3175701100
	DD	2963548093
	DD	1048576000
	DD	1034789701
	DD	3156342344
	DD	1018810804
	DD	3133887847
	DD	1039881169
	DD	823481222
	DD	1065353216
	DD	3173496918
	DD	2969038481
	DD	1048576000
	DD	1034730062
	DD	3154554595
	DD	1018750428
	DD	3136028910
	DD	1036586971
	DD	2973482286
	DD	1065353216
	DD	3171295395
	DD	2968300352
	DD	1048576000
	DD	1034681361
	DD	3151437839
	DD	1018664053
	DD	3123383004
	DD	1033292772
	DD	2941050414
	DD	1065353216
	DD	3167298168
	DD	808398440
	DD	1048576000
	DD	1034643540
	DD	3147899215
	DD	1018610153
	DD	943964915
	DD	1028198363
	DD	2965093678
	DD	1065353216
	DD	3162902549
	DD	2950073902
	DD	1048576000
	DD	1034616555
	DD	3143016255
	DD	1018603598
	DD	3133555092
	DD	1019809755
	DD	2956705070
	DD	1065353216
	DD	3154512883
	DD	803361198
	DD	1048576000
	DD	1034600377
	DD	3134618720
	DD	1018580133
	DD	3134056577
	DD	0
	DD	0
	DD	1065353216
	DD	0
	DD	0
	DD	1048576000
	DD	1034594987
	DD	0
	DD	1018552971
	DD	0
	DD	3167293403
	DD	809221422
	DD	1065353216
	DD	1007029235
	DD	2950844846
	DD	1048576000
	DD	1034600377
	DD	987135072
	DD	1018580133
	DD	986572929
	DD	3175682011
	DD	817610030
	DD	1065353216
	DD	1015418901
	DD	802590254
	DD	1048576000
	DD	1034616555
	DD	995532607
	DD	1018603598
	DD	986071444
	DD	3180776420
	DD	793566766
	DD	1065353216
	DD	1019814520
	DD	2955882088
	DD	1048576000
	DD	1034643540
	DD	1000415567
	DD	1018610153
	DD	3091448562
	DD	3184070619
	DD	825998638
	DD	1065353216
	DD	1023811747
	DD	820816704
	DD	1048576000
	DD	1034681361
	DD	1003954191
	DD	1018664053
	DD	975899356
	DD	3187364817
	DD	2970964870
	DD	1065353216
	DD	1026013270
	DD	821554833
	DD	1048576000
	DD	1034730062
	DD	1007070947
	DD	1018750428
	DD	988545262
	DD	3189165028
	DD	801955374
	DD	1065353216
	DD	1028217452
	DD	816064445
	DD	1048576000
	DD	1034789701
	DD	1008858696
	DD	1018810804
	DD	986404199
	DD	3190812127
	DD	2979773047
	DD	1065353216
	DD	1030424831
	DD	820957750
	DD	1048576000
	DD	1034860345
	DD	1010658641
	DD	1018906717
	DD	989853114
	DD	3192459227
	DD	834387246
	DD	1065353216
	DD	1032217367
	DD	804266304
	DD	1048576000
	DD	1034942077
	DD	1012473040
	DD	1019002541
	DD	990165996
	DD	3194106326
	DD	806987426
	DD	1065353216
	DD	1033325069
	DD	827274843
	DD	1048576000
	DD	1035034991
	DD	1014303960
	DD	1019131285
	DD	992131203
	DD	3195753425
	DD	2979353478
	DD	1065353216
	DD	1034435191
	DD	2976749178
	DD	1048576000
	DD	1035139196
	DD	1015587615
	DD	1019275107
	DD	993990246
	DD	3196730086
	DD	2988371440
	DD	1065353216
	DD	1035548009
	DD	800713984
	DD	1048576000
	DD	1035254815
	DD	1016523013
	DD	1019406069
	DD	993923238
	DD	3197553636
	DD	810343982
	DD	1065353216
	DD	1036663807
	DD	2971440618
	DD	1048576000
	DD	1035381982
	DD	1017470043
	DD	1019591684
	DD	996387177
	DD	3198377186
	DD	842566070
	DD	1065353216
	DD	1037782869
	DD	2968929150
	DD	1048576000
	DD	1035520850
	DD	1018429968
	DD	1019751749
	DD	996421749
	DD	3199200735
	DD	2988161655
	DD	1065353216
	DD	1038905484
	DD	826840516
	DD	1048576000
	DD	1035671582
	DD	1019403942
	DD	1019957287
	DD	998193513
	DD	3200024285
	DD	813697757
	DD	1065353216
	DD	1040031947
	DD	2937388192
	DD	1048576000
	DD	1035834359
	DD	1020393243
	DD	1020189995
	DD	999315782
	DD	3200847835
	DD	842775854
	DD	1065353216
	DD	1040674973
	DD	831138435
	DD	1048576000
	DD	1036009378
	DD	1021399190
	DD	1020421234
	DD	999953008
	DD	3201671384
	DD	2987951871
	DD	1065353216
	DD	1041242501
	DD	808432312
	DD	1048576000
	DD	1036196851
	DD	1022423117
	DD	1020660679
	DD	1000421562
	DD	3202494934
	DD	815376034
	DD	1065353216
	DD	1041812407
	DD	803374128
	DD	1048576000
	DD	1036397006
	DD	1023438285
	DD	1020942892
	DD	1001436114
	DD	3203318484
	DD	842985639
	DD	1065353216
	DD	1042384848
	DD	834127863
	DD	1048576000
	DD	1036610091
	DD	1023970338
	DD	1021220110
	DD	1001954001
	DD	3204142033
	DD	2987742086
	DD	1065353216
	DD	1042959985
	DD	832371948
	DD	1048576000
	DD	1036836369
	DD	1024513541
	DD	1021543079
	DD	1003011479
	DD	3204706919
	DD	3002387417
	DD	1065353216
	DD	1043537982
	DD	2986276286
	DD	1048576000
	DD	1037076124
	DD	1025068684
	DD	1021893801
	DD	1004198485
	DD	3205118694
	DD	2996760048
	DD	1065353216
	DD	1044119004
	DD	825381402
	DD	1048576000
	DD	1037329660
	DD	1025636593
	DD	1022242934
	DD	1004982883
	DD	3205530469
	DD	2987532301
	DD	1065353216
	DD	1044703224
	DD	835363787
	DD	1048576000
	DD	1037597300
	DD	1026218117
	DD	1022609285
	DD	1005708104
	DD	3205942244
	DD	818732590
	DD	1065353216
	DD	1045290817
	DD	833527956
	DD	1048576000
	DD	1037879388
	DD	1026814142
	DD	1023026096
	DD	1006763255
	DD	3206354019
	DD	843405209
	DD	1065353216
	DD	1045881963
	DD	2984505345
	DD	1048576000
	DD	1038176293
	DD	1027425616
	DD	1023428141
	DD	1007234200
	DD	3206765794
	DD	850954678
	DD	1065353216
	DD	1046476844
	DD	829452858
	DD	1048576000
	DD	1038488404
	DD	1028053510
	DD	1023672824
	DD	1008001043
	DD	3207177568
	DD	3002177633
	DD	1065353216
	DD	1047075652
	DD	2985396534
	DD	1048576000
	DD	1038816139
	DD	1028698871
	DD	1023917626
	DD	1008617127
	DD	3207589343
	DD	2996550263
	DD	1065353216
	DD	1047678579
	DD	2971714530
	DD	1048576000
	DD	1039159939
	DD	1029362782
	DD	1024176063
	DD	1009236155
	DD	3208001118
	DD	2987112732
	DD	1065353216
	DD	1048285826
	DD	825459666
	DD	1048576000
	DD	1039520273
	DD	1030046387
	DD	1024452069
	DD	1009908500
	DD	3208412893
	DD	822086365
	DD	1065353216
	DD	1048736800
	DD	2987101005
	DD	1048576000
	DD	1039897640
	DD	1030750900
	DD	1024731756
	DD	1010452479
	DD	3208824668
	DD	843824778
	DD	1065353216
	DD	1049045055
	DD	845724006
	DD	1056964608
	DD	3187565865
	DD	1031477587
	DD	1025040649
	DD	1011183792
	DD	3209236443
	DD	851164462
	DD	0
	DD	3212836864
	DD	725680128
	DD	1073741824
	DD	3003121664
	DD	3221225472
	DD	1076541384
	DD	3226821083
	DD	3209648217
	DD	3001967848
	DD	0
	DD	3212032885
	DD	3002896525
	DD	1073741824
	DD	3183449264
	DD	3219674383
	DD	1075269479
	DD	3224815112
	DD	3210059992
	DD	2996340479
	DD	0
	DD	3211265630
	DD	2993098010
	DD	1073741824
	DD	3191263702
	DD	3218296839
	DD	1074178145
	DD	3223145434
	DD	3210471767
	DD	2986693162
	DD	0
	DD	3210531774
	DD	2974155528
	DD	1073741824
	DD	3196257989
	DD	3217068594
	DD	1072731698
	DD	3221740288
	DD	3210883542
	DD	823764642
	DD	0
	DD	3209828353
	DD	2997764472
	DD	1073741824
	DD	3198626104
	DD	3215969314
	DD	1071103306
	DD	3219894299
	DD	3211295317
	DD	844244347
	DD	0
	DD	3209152716
	DD	838923546
	DD	1073741824
	DD	3200789612
	DD	3214981865
	DD	1069688111
	DD	3217892551
	DD	3211707092
	DD	851374247
	DD	0
	DD	3208502479
	DD	3000145414
	DD	1073741824
	DD	3202769007
	DD	3214091734
	DD	1068453481
	DD	3216181260
	DD	3212118866
	DD	3001758063
	DD	0
	DD	3207875497
	DD	850964714
	DD	1065353216
	DD	1056830711
	DD	3213286568
	DD	1067373883
	DD	3214720734
	DD	3212530641
	DD	2996130694
	DD	0
	DD	3207269825
	DD	2997583546
	DD	1065353216
	DD	1055168194
	DD	3212274752
	DD	1066427841
	DD	3213471670
	DD	3212889640
	DD	2986202738
	DD	0
	DD	3206683703
	DD	839672284
	DD	1065353216
	DD	1053642609
	DD	3210943914
	DD	1065596017
	DD	3211952618
	DD	3213095527
	DD	3010776025
	DD	0
	DD	3206115524
	DD	2995800136
	DD	1065353216
	DD	1052241912
	DD	3209728124
	DD	1064374250
	DD	3210092525
	DD	3213301415
	DD	844663917
	DD	0
	DD	3205563823
	DD	3002090928
	DD	1065353216
	DD	1050955490
	DD	3208613851
	DD	1063075792
	DD	3208448547
	DD	3213507302
	DD	3005148656
	DD	0
	DD	3205027257
	DD	845085070
	DD	1065353216
	DD	1049773965
	DD	3207589321
	DD	1061932376
	DD	3207048862
	DD	3213713190
	DD	856424709
	DD	0
	DD	3204504589
	DD	840216434
	DD	1065353216
	DD	1048689044
	DD	3206644275
	DD	1060914481
	DD	3205797512
	DD	3213919077
	DD	2995920909
	DD	0
	DD	3203541104
	DD	2991747572
	DD	1065353216
	DD	1046810746
	DD	3205769712
	DD	1060014844
	DD	3204711576
	DD	3214124965
	DD	862052078
	DD	0
	DD	3202544697
	DD	838089118
	DD	1065353216
	DD	1044984860
	DD	3204957722
	DD	1059214863
	DD	3203049502
	DD	3214330852
	DD	827121198
	DD	0
	DD	3201569741
	DD	834691410
	DD	1065353216
	DD	1043312844
	DD	3203954379
	DD	1058502663
	DD	3201336375
	DD	3214536739
	DD	3007857448
	DD	0
	DD	3200614413
	DD	840221633
	DD	1065353216
	DD	1041784404
	DD	3202540354
	DD	1057868403
	DD	3199781921
	DD	3214742627
	DD	851793817
	DD	0
	DD	3199677008
	DD	839026723
	DD	1065353216
	DD	1040390392
	DD	3201214072
	DD	1057307751
	DD	3198426707
	DD	3214948514
	DD	3001338494
	DD	0
	DD	3198755927
	DD	2964850736
	DD	1065353216
	DD	1038057984
	DD	3199965673
	DD	1056656040
	DD	3197207230
	DD	3215154402
	DD	859343286
	DD	0
	DD	3197849666
	DD	841628398
	DD	1065353216
	DD	1035760784
	DD	3198786293
	DD	1055777031
	DD	3196119466
	DD	3215360289
	DD	2984524460
	DD	0
	DD	3196956802
	DD	825401908
	DD	1065353216
	DD	1033689040
	DD	3197667936
	DD	1054999523
	DD	3194181676
	DD	3215566176
	DD	3010566241
	DD	0
	DD	3196075988
	DD	841457254
	DD	1065353216
	DD	1031831496
	DD	3196603348
	DD	1054310701
	DD	3192272619
	DD	3215772064
	DD	845503056
	DD	0
	DD	3194352231
	DD	2981409247
	DD	1065353216
	DD	1028557712
	DD	3195112138
	DD	1053716836
	DD	3190669665
	DD	3215977951
	DD	3004938871
	DD	0
	DD	3192631215
	DD	2938776032
	DD	1065353216
	DD	1025642520
	DD	3193159376
	DD	1053195814
	DD	3189074146
	DD	3216183839
	DD	856634493
	DD	0
	DD	3190926925
	DD	838017617
	DD	1065353216
	DD	1022797056
	DD	3191277530
	DD	1052746889
	DD	3187399111
	DD	3216389726
	DD	2995501340
	DD	0
	DD	3189237092
	DD	835035876
	DD	1065353216
	DD	1018446032
	DD	3189456128
	DD	1052374628
	DD	3184936896
	DD	3216595614
	DD	862261863
	DD	0
	DD	3187448002
	DD	816730716
	DD	1065353216
	DD	1014578464
	DD	3187685445
	DD	1052059423
	DD	3182192286
	DD	3216801501
	DD	830474973
	DD	0
	DD	3184113116
	DD	2974133638
	DD	1065353216
	DD	1008660256
	DD	3184241386
	DD	1051807326
	DD	3179645874
	DD	3217007388
	DD	3007647664
	DD	0
	DD	3180794318
	DD	2974804776
	DD	1065353216
	DD	1001540608
	DD	3180848186
	DD	1051617929
	DD	3175942112
	DD	3217213276
	DD	852213386
	DD	0
	DD	3175692604
	DD	2963513179
	DD	1065353216
	DD	991832832
	DD	3175724500
	DD	1051479824
	DD	3170735141
	DD	3217419163
	DD	3000918924
	DD	0
	DD	3167296049
	DD	2945355034
	DD	1065353216
	DD	975043072
	DD	3167303981
	DD	1051400329
	DD	3163053371
_2il0floatpacket_27	DD	00000007cH,00000007cH,00000007cH,00000007cH,00000007cH,00000007cH,00000007cH,00000007cH
_vmldTanHATab	DD	1841940611
	DD	1076125488
	DD	0
	DD	1127743488
	DD	0
	DD	1131937792
	DD	1413758976
	DD	1069097467
	DD	1734819840
	DD	3174229945
	DD	923219018
	DD	984130272
	DD	1413742592
	DD	1069097467
	DD	1280049152
	DD	1028033571
	DD	57701189
	DD	988383790
	DD	1734816687
	DD	1026746297
	DD	4294705152
	DD	4294967295
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	2146435072
	DD	0
	DD	1130364928
	DD	0
	DD	1015021568
	DD	0
	DD	0
	DD	2284589306
	DD	1066820852
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1441186365
	DD	1065494243
	DD	1431655765
	DD	1070945621
	DD	0
	DD	0
	DD	0
	DD	0
	DD	236289504
	DD	1064135997
	DD	286331153
	DD	1069617425
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1160476131
	DD	1062722102
	DD	463583772
	DD	1068212666
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
	DD	1313038235
	DD	1066745731
	DD	1013878342
	DD	1067152618
	DD	3663426833
	DD	1065725283
	DD	3693284251
	DD	1069118808
	DD	650852232
	DD	1065882376
	DD	1996245381
	DD	1071000265
	DD	2008746170
	DD	1064664197
	DD	3055842593
	DD	1068578846
	DD	1495406348
	DD	1064652437
	DD	2269530157
	DD	1069711235
	DD	285563696
	DD	1063576465
	DD	1046897440
	DD	1067705865
	DD	233429731
	DD	1063453151
	DD	522045958
	DD	1068476590
	DD	2354785698
	DD	1069102779
	DD	1317599141
	DD	1012432133
	DD	0
	DD	1072693248
	DD	2828230105
	DD	1065606626
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1512545955
	DD	1068119047
	DD	1127048698
	DD	1067909459
	DD	2300200450
	DD	1067254767
	DD	3593250296
	DD	1070233561
	DD	3009365544
	DD	1066902117
	DD	1127373050
	DD	1071173457
	DD	3046103305
	DD	1066371299
	DD	24583402
	DD	1069723988
	DD	4082511758
	DD	1065914199
	DD	3223889699
	DD	1070020367
	DD	548927984
	DD	1065415756
	DD	558065897
	DD	1068949418
	DD	680073315
	DD	1064940726
	DD	388873200
	DD	1068944270
	DD	3763679576
	DD	1070167541
	DD	1497360404
	DD	1009710547
	DD	0
	DD	1072693248
	DD	64931152
	DD	1067729411
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2467582782
	DD	1069256389
	DD	162150096
	DD	1068946420
	DD	3702794237
	DD	1068579152
	DD	3631919291
	DD	1070936926
	DD	3456821413
	DD	1068217218
	DD	2031366438
	DD	1071495745
	DD	1596664020
	DD	1067799281
	DD	1509038701
	DD	1070601643
	DD	583171477
	DD	1067510148
	DD	3785344682
	DD	1070618476
	DD	2402036048
	DD	1067075736
	DD	3233018412
	DD	1069913186
	DD	411280568
	DD	1066710556
	DD	1065584192
	DD	1069747896
	DD	895247324
	DD	1070819848
	DD	500078909
	DD	3161288781
	DD	0
	DD	1072693248
	DD	729983843
	DD	1068994194
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1458794562
	DD	1070398550
	DD	2857777489
	DD	1070137637
	DD	1024359517
	DD	1069876531
	DD	2616040238
	DD	1071582937
	DD	1609024636
	DD	1069675088
	DD	2529240549
	DD	1071836633
	DD	1510128600
	DD	1069440113
	DD	2251697184
	DD	1071253687
	DD	1262761453
	DD	1069142850
	DD	1263091857
	DD	1071190461
	DD	3043383486
	DD	1068885191
	DD	2476932470
	DD	1070842002
	DD	3659995028
	DD	1068669200
	DD	855891755
	DD	1070696894
	DD	2583490354
	DD	1071284857
	DD	3062633575
	DD	1014008623
	DD	0
	DD	1072693248
	DD	2550940471
	DD	1069938201
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3422807297
	DD	1071640847
	DD	1151658053
	DD	1071494715
	DD	929607071
	DD	1071346340
	DD	1037049034
	DD	1072037305
	DD	2786928657
	DD	1071215282
	DD	1447406859
	DD	1072265209
	DD	3490952107
	DD	1071090851
	DD	3205232916
	DD	1071968658
	DD	1297344304
	DD	1070977120
	DD	1066110976
	DD	1071946035
	DD	3803721480
	DD	1070871082
	DD	1496754229
	DD	1071807201
	DD	2982550683
	DD	1070773243
	DD	4014441989
	DD	1071736222
	DD	419968236
	DD	1071717047
	DD	3451266538
	DD	3163444811
	DD	0
	DD	1072693248
	DD	2960267235
	DD	1070745841
	DD	0
	DD	0
	DD	0
	DD	0
	DD	724322768
	DD	1072881308
	DD	643153048
	DD	1072905816
	DD	4285079458
	DD	1072928558
	DD	3912524733
	DD	1072622983
	DD	118362272
	DD	1072952754
	DD	4107767972
	DD	1072827408
	DD	2689502883
	DD	1072976922
	DD	946523347
	DD	1072772766
	DD	573204189
	DD	1073001761
	DD	581531518
	DD	1072826391
	DD	1386236526
	DD	1073026959
	DD	3718905905
	DD	1072832823
	DD	1145558140
	DD	1073052673
	DD	513572637
	DD	1072861969
	DD	716700048
	DD	1071997368
	DD	547126769
	DD	1015523525
	DD	0
	DD	1072693248
	DD	1097907398
	DD	1071420120
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3349892442
	DD	1074290212
	DD	3913197405
	DD	1074501181
	DD	2494034522
	DD	1074739170
	DD	1264738763
	DD	1073084804
	DD	1520293906
	DD	1074899632
	DD	1958936600
	DD	1073411493
	DD	2133649635
	DD	1075052171
	DD	4270740730
	DD	1073574708
	DD	1728930189
	DD	1075224844
	DD	1303998552
	DD	1073799186
	DD	618611933
	DD	1075420255
	DD	1769828046
	DD	1073938542
	DD	2200537986
	DD	1075641421
	DD	433361110
	DD	1074105369
	DD	719595600
	DD	1072317184
	DD	294527206
	DD	3162140088
	DD	0
	DD	1073741824
	DD	3811788216
	DD	3218400550
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1704352102
	DD	1075943001
	DD	2284589306
	DD	1076258036
	DD	2211264291
	DD	1076659010
	DD	0
	DD	1073741824
	DD	1441186365
	DD	1077028579
	DD	1431655765
	DD	1074091349
	DD	876943673
	DD	1077353622
	DD	2863311531
	DD	1074440874
	DD	236289504
	DD	1077767485
	DD	286331153
	DD	1074860305
	DD	2805473311
	DD	1078115278
	DD	95443718
	DD	1075163227
	DD	1160476131
	DD	1078450742
	DD	463583772
	DD	1075552698
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	1073741824
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1330165971
	DD	3207850745
	DD	217536623
	DD	1059109098
	DD	3492120849
	DD	3205151475
	DD	602185705
	DD	3215678092
	DD	760422958
	DD	1056312597
	DD	555127889
	DD	1067545266
	DD	3139784124
	DD	3202470837
	DD	3690544014
	DD	3213150171
	DD	95707915
	DD	1053635428
	DD	4003114407
	DD	1064581412
	DD	2034926231
	DD	3199711161
	DD	3759536023
	DD	3210559989
	DD	3826928214
	DD	1050893819
	DD	3837960785
	DD	1061790379
	DD	1526325248
	DD	3217967566
	DD	2356426521
	DD	1025423456
	DD	0
	DD	0
	DD	457728975
	DD	1071088276
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	1398462608
	DD	3207303968
	DD	26205983
	DD	1058461213
	DD	56226238
	DD	3204528612
	DD	2754706541
	DD	3215359511
	DD	2187799823
	DD	1055634437
	DD	790323742
	DD	1067402587
	DD	1372385848
	DD	3201651479
	DD	4097292716
	DD	3212856302
	DD	3348210357
	DD	1052830099
	DD	2442796466
	DD	1064337602
	DD	862608142
	DD	3198830754
	DD	170296152
	DD	3210060867
	DD	3755571428
	DD	1049933343
	DD	3614866008
	DD	1061361670
	DD	719978496
	DD	3217669096
	DD	1998842465
	DD	3174703977
	DD	0
	DD	0
	DD	3749156607
	DD	1071048258
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	3120498638
	DD	3206749304
	DD	2773578114
	DD	1058009312
	DD	2030783676
	DD	3203817873
	DD	2223654598
	DD	3215071936
	DD	2976134650
	DD	1054987244
	DD	706390066
	DD	1067217386
	DD	4258437615
	DD	3200900378
	DD	1066252975
	DD	3212391267
	DD	815777514
	DD	1051989462
	DD	3202745457
	DD	1064010682
	DD	2493556375
	DD	3198004753
	DD	1046243251
	DD	3209678971
	DD	2593078846
	DD	1049017717
	DD	2763962276
	DD	1060970161
	DD	701480960
	DD	3217377742
	DD	3205862232
	DD	3174660915
	DD	0
	DD	0
	DD	2267016812
	DD	1071015664
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	2107155798
	DD	3206166872
	DD	2642992129
	DD	1057424578
	DD	1936992811
	DD	3203204426
	DD	1485063559
	DD	3214682643
	DD	1432914553
	DD	1054319398
	DD	3996381654
	DD	1067075828
	DD	2833029256
	DD	3200223545
	DD	2866066872
	DD	3211982662
	DD	2432888737
	DD	1051234178
	DD	3669764559
	DD	1063748136
	DD	2458496952
	DD	3197170774
	DD	1948234989
	DD	3209098147
	DD	2843698787
	DD	1048163519
	DD	3398041407
	DD	1060559728
	DD	2829230080
	DD	3217092115
	DD	1034046433
	DD	3174271903
	DD	0
	DD	0
	DD	298675305
	DD	1070989821
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	437603223
	DD	3205589761
	DD	759330352
	DD	1057048511
	DD	3107463368
	DD	3202507988
	DD	3144465176
	DD	3214191500
	DD	2290961810
	DD	1053841035
	DD	1618153340
	DD	1066971547
	DD	3836869393
	DD	3199400272
	DD	584032116
	DD	3211469261
	DD	1245704358
	DD	1050626462
	DD	4247487438
	DD	1063561943
	DD	1669034927
	DD	3196274812
	DD	3844233498
	DD	3208626322
	DD	2706958524
	DD	1047411374
	DD	3857199098
	DD	1060281647
	DD	3593904128
	DD	3216590719
	DD	3267547836
	DD	3172163321
	DD	0
	DD	0
	DD	4076712227
	DD	1070970214
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	3290090340
	DD	3204793485
	DD	3685760367
	DD	1056668370
	DD	2655163949
	DD	3201674917
	DD	628750575
	DD	3213566872
	DD	680140505
	DD	1053299777
	DD	2954464709
	DD	1066900026
	DD	803201619
	DD	3198516435
	DD	1466315631
	DD	3210837162
	DD	1611220163
	DD	1049972438
	DD	2766187256
	DD	1063437894
	DD	1804579484
	DD	3195331491
	DD	3695969289
	DD	3207854418
	DD	2617238373
	DD	1046675948
	DD	3095830084
	DD	1060095334
	DD	3789570048
	DD	3216034914
	DD	23826559
	DD	3172048060
	DD	0
	DD	0
	DD	3870939386
	DD	1070956467
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	1571758758
	DD	3203672535
	DD	113026373
	DD	1056416381
	DD	1913766298
	DD	3200523326
	DD	2507068734
	DD	3212502004
	DD	4000648818
	DD	1053003803
	DD	2446607349
	DD	1066858259
	DD	912662124
	DD	3197333001
	DD	1349489537
	DD	3209765608
	DD	3412972607
	DD	1049641401
	DD	1721283327
	DD	1063366855
	DD	1466691883
	DD	3194116746
	DD	3852528092
	DD	3206760861
	DD	285443293
	DD	1046158380
	DD	1758739894
	DD	1059895449
	DD	1858781184
	DD	3214984212
	DD	3447575948
	DD	1024675855
	DD	0
	DD	0
	DD	2242038011
	DD	1070948320
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	0
	DD	0
	DD	737611454
	DD	1056336527
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3594790527
	DD	1052911621
	DD	381774871
	DD	1066844524
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3303051618
	DD	1049456050
	DD	3154187623
	DD	1063343722
	DD	0
	DD	0
	DD	0
	DD	0
	DD	528061788
	DD	1045944910
	DD	2469719819
	DD	1059831159
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1431655765
	DD	1070945621
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	1571758758
	DD	1056188887
	DD	113026373
	DD	1056416381
	DD	1913766298
	DD	1053039678
	DD	2507068734
	DD	1065018356
	DD	4000648818
	DD	1053003803
	DD	2446607349
	DD	1066858259
	DD	912662124
	DD	1049849353
	DD	1349489537
	DD	1062281960
	DD	3412972607
	DD	1049641401
	DD	1721283327
	DD	1063366855
	DD	1466691883
	DD	1046633098
	DD	3852528092
	DD	1059277213
	DD	285443293
	DD	1046158380
	DD	1758739894
	DD	1059895449
	DD	1858781184
	DD	1067500564
	DD	3447575948
	DD	3172159503
	DD	0
	DD	0
	DD	2242038011
	DD	1070948320
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	3290090340
	DD	1057309837
	DD	3685760367
	DD	1056668370
	DD	2655163949
	DD	1054191269
	DD	628750575
	DD	1066083224
	DD	680140505
	DD	1053299777
	DD	2954464709
	DD	1066900026
	DD	803201619
	DD	1051032787
	DD	1466315631
	DD	1063353514
	DD	1611220163
	DD	1049972438
	DD	2766187256
	DD	1063437894
	DD	1804579484
	DD	1047847843
	DD	3695969289
	DD	1060370770
	DD	2617238373
	DD	1046675948
	DD	3095830084
	DD	1060095334
	DD	3789570048
	DD	1068551266
	DD	23826559
	DD	1024564412
	DD	0
	DD	0
	DD	3870939386
	DD	1070956467
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	437603223
	DD	1058106113
	DD	759330352
	DD	1057048511
	DD	3107463368
	DD	1055024340
	DD	3144465176
	DD	1066707852
	DD	2290961810
	DD	1053841035
	DD	1618153340
	DD	1066971547
	DD	3836869393
	DD	1051916624
	DD	584032116
	DD	1063985613
	DD	1245704358
	DD	1050626462
	DD	4247487438
	DD	1063561943
	DD	1669034927
	DD	1048791164
	DD	3844233498
	DD	1061142674
	DD	2706958524
	DD	1047411374
	DD	3857199098
	DD	1060281647
	DD	3593904128
	DD	1069107071
	DD	3267547836
	DD	1024679673
	DD	0
	DD	0
	DD	4076712227
	DD	1070970214
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	2107155798
	DD	1058683224
	DD	2642992129
	DD	1057424578
	DD	1936992811
	DD	1055720778
	DD	1485063559
	DD	1067198995
	DD	1432914553
	DD	1054319398
	DD	3996381654
	DD	1067075828
	DD	2833029256
	DD	1052739897
	DD	2866066872
	DD	1064499014
	DD	2432888737
	DD	1051234178
	DD	3669764559
	DD	1063748136
	DD	2458496952
	DD	1049687126
	DD	1948234989
	DD	1061614499
	DD	2843698787
	DD	1048163519
	DD	3398041407
	DD	1060559728
	DD	2829230080
	DD	1069608467
	DD	1034046433
	DD	1026788255
	DD	0
	DD	0
	DD	298675305
	DD	1070989821
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	3120498638
	DD	1059265656
	DD	2773578114
	DD	1058009312
	DD	2030783676
	DD	1056334225
	DD	2223654598
	DD	1067588288
	DD	2976134650
	DD	1054987244
	DD	706390066
	DD	1067217386
	DD	4258437615
	DD	1053416730
	DD	1066252975
	DD	1064907619
	DD	815777514
	DD	1051989462
	DD	3202745457
	DD	1064010682
	DD	2493556375
	DD	1050521105
	DD	1046243251
	DD	1062195323
	DD	2593078846
	DD	1049017717
	DD	2763962276
	DD	1060970161
	DD	701480960
	DD	1069894094
	DD	3205862232
	DD	1027177267
	DD	0
	DD	0
	DD	2267016812
	DD	1071015664
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	1398462608
	DD	1059820320
	DD	26205983
	DD	1058461213
	DD	56226238
	DD	1057044964
	DD	2754706541
	DD	1067875863
	DD	2187799823
	DD	1055634437
	DD	790323742
	DD	1067402587
	DD	1372385848
	DD	1054167831
	DD	4097292716
	DD	1065372654
	DD	3348210357
	DD	1052830099
	DD	2442796466
	DD	1064337602
	DD	862608142
	DD	1051347106
	DD	170296152
	DD	1062577219
	DD	3755571428
	DD	1049933343
	DD	3614866008
	DD	1061361670
	DD	719978496
	DD	1070185448
	DD	1998842465
	DD	1027220329
	DD	0
	DD	0
	DD	3749156607
	DD	1071048258
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	1330165971
	DD	1060367097
	DD	217536623
	DD	1059109098
	DD	3492120849
	DD	1057667827
	DD	602185705
	DD	1068194444
	DD	760422958
	DD	1056312597
	DD	555127889
	DD	1067545266
	DD	3139784124
	DD	1054987189
	DD	3690544014
	DD	1065666523
	DD	95707915
	DD	1053635428
	DD	4003114407
	DD	1064581412
	DD	2034926231
	DD	1052227513
	DD	3759536023
	DD	1063076341
	DD	3826928214
	DD	1050893819
	DD	3837960785
	DD	1061790379
	DD	1526325248
	DD	1070483918
	DD	2356426521
	DD	3172907104
	DD	0
	DD	0
	DD	457728975
	DD	1071088276
	DD	0
	DD	1072693248
	DD	0
	DD	4294967288
	DD	1704352102
	DD	3223426649
	DD	2284589306
	DD	1076258036
	DD	2211264291
	DD	3224142658
	DD	0
	DD	3221225472
	DD	1441186365
	DD	1077028579
	DD	1431655765
	DD	1074091349
	DD	876943673
	DD	3224837270
	DD	2863311531
	DD	3221924522
	DD	236289504
	DD	1077767485
	DD	286331153
	DD	1074860305
	DD	2805473311
	DD	3225598926
	DD	95443718
	DD	3222646875
	DD	1160476131
	DD	1078450742
	DD	463583772
	DD	1075552698
	DD	0
	DD	3220176896
	DD	0
	DD	0
	DD	0
	DD	1073741824
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3349892442
	DD	3221773860
	DD	3913197405
	DD	1074501181
	DD	2494034522
	DD	3222222818
	DD	1264738763
	DD	3220568452
	DD	1520293906
	DD	1074899632
	DD	1958936600
	DD	1073411493
	DD	2133649635
	DD	3222535819
	DD	4270740730
	DD	3221058356
	DD	1728930189
	DD	1075224844
	DD	1303998552
	DD	1073799186
	DD	618611933
	DD	3222903903
	DD	1769828046
	DD	3221422190
	DD	2200537986
	DD	1075641421
	DD	433361110
	DD	1074105369
	DD	719595600
	DD	3219800832
	DD	294527206
	DD	1014656440
	DD	0
	DD	1073741824
	DD	3811788216
	DD	3218400550
	DD	0
	DD	0
	DD	0
	DD	0
	DD	724322768
	DD	3220364956
	DD	643153048
	DD	1072905816
	DD	4285079458
	DD	3220412206
	DD	3912524733
	DD	3220106631
	DD	118362272
	DD	1072952754
	DD	4107767972
	DD	1072827408
	DD	2689502883
	DD	3220460570
	DD	946523347
	DD	3220256414
	DD	573204189
	DD	1073001761
	DD	581531518
	DD	1072826391
	DD	1386236526
	DD	3220510607
	DD	3718905905
	DD	3220316471
	DD	1145558140
	DD	1073052673
	DD	513572637
	DD	1072861969
	DD	716700048
	DD	3219481016
	DD	547126769
	DD	3163007173
	DD	0
	DD	1072693248
	DD	1097907398
	DD	1071420120
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3422807297
	DD	3219124495
	DD	1151658053
	DD	1071494715
	DD	929607071
	DD	3218829988
	DD	1037049034
	DD	3219520953
	DD	2786928657
	DD	1071215282
	DD	1447406859
	DD	1072265209
	DD	3490952107
	DD	3218574499
	DD	3205232916
	DD	3219452306
	DD	1297344304
	DD	1070977120
	DD	1066110976
	DD	1071946035
	DD	3803721480
	DD	3218354730
	DD	1496754229
	DD	3219290849
	DD	2982550683
	DD	1070773243
	DD	4014441989
	DD	1071736222
	DD	419968236
	DD	3219200695
	DD	3451266538
	DD	1015961163
	DD	0
	DD	1072693248
	DD	2960267235
	DD	1070745841
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1458794562
	DD	3217882198
	DD	2857777489
	DD	1070137637
	DD	1024359517
	DD	3217360179
	DD	2616040238
	DD	3219066585
	DD	1609024636
	DD	1069675088
	DD	2529240549
	DD	1071836633
	DD	1510128600
	DD	3216923761
	DD	2251697184
	DD	3218737335
	DD	1262761453
	DD	1069142850
	DD	1263091857
	DD	1071190461
	DD	3043383486
	DD	3216368839
	DD	2476932470
	DD	3218325650
	DD	3659995028
	DD	1068669200
	DD	855891755
	DD	1070696894
	DD	2583490354
	DD	3218768505
	DD	3062633575
	DD	3161492271
	DD	0
	DD	1072693248
	DD	2550940471
	DD	1069938201
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2467582782
	DD	3216740037
	DD	162150096
	DD	1068946420
	DD	3702794237
	DD	3216062800
	DD	3631919291
	DD	3218420574
	DD	3456821413
	DD	1068217218
	DD	2031366438
	DD	1071495745
	DD	1596664020
	DD	3215282929
	DD	1509038701
	DD	3218085291
	DD	583171477
	DD	1067510148
	DD	3785344682
	DD	1070618476
	DD	2402036048
	DD	3214559384
	DD	3233018412
	DD	3217396834
	DD	411280568
	DD	1066710556
	DD	1065584192
	DD	1069747896
	DD	895247324
	DD	3218303496
	DD	500078909
	DD	1013805133
	DD	0
	DD	1072693248
	DD	729983843
	DD	1068994194
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1512545955
	DD	3215602695
	DD	1127048698
	DD	1067909459
	DD	2300200450
	DD	3214738415
	DD	3593250296
	DD	3217717209
	DD	3009365544
	DD	1066902117
	DD	1127373050
	DD	1071173457
	DD	3046103305
	DD	3213854947
	DD	24583402
	DD	3217207636
	DD	4082511758
	DD	1065914199
	DD	3223889699
	DD	1070020367
	DD	548927984
	DD	3212899404
	DD	558065897
	DD	3216433066
	DD	680073315
	DD	1064940726
	DD	388873200
	DD	1068944270
	DD	3763679576
	DD	3217651189
	DD	1497360404
	DD	3157194195
	DD	0
	DD	1072693248
	DD	64931152
	DD	1067729411
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1313038235
	DD	3214229379
	DD	1013878342
	DD	1067152618
	DD	3663426833
	DD	3213208931
	DD	3693284251
	DD	3216602456
	DD	650852232
	DD	1065882376
	DD	1996245381
	DD	1071000265
	DD	2008746170
	DD	3212147845
	DD	3055842593
	DD	3216062494
	DD	1495406348
	DD	1064652437
	DD	2269530157
	DD	1069711235
	DD	285563696
	DD	3211060113
	DD	1046897440
	DD	3215189513
	DD	233429731
	DD	1063453151
	DD	522045958
	DD	1068476590
	DD	2354785698
	DD	3216586427
	DD	1317599141
	DD	3159915781
	DD	0
	DD	1072693248
	DD	2828230105
	DD	1065606626
	DD	0
	DD	0
	DD	0
	DD	0
_vmlReductionTab	DD	0
	DD	0
	DD	0
	DD	3220176896
	DD	0
	DD	1072693248
	DD	0
	DD	3220176896
	DD	0
	DD	1100480512
	DD	0
	DD	1101004800
	DD	1073741824
	DD	1072243195
	DD	407279769
	DD	1046758445
	DD	1841940611
	DD	1072979760
	DD	0
	DD	1085276160
	DD	1409286144
	DD	1072243195
	DD	301989888
	DD	1040255814
	DD	1006632960
	DD	3156637299
	DD	2207917344
	DD	979464219
	DD	0
	DD	1072243194
	DD	0
	DD	1051018307
	DD	0
	DD	3174514122
	DD	0
	DD	3153310618
	DD	3773204808
	DD	981752838
	DD	4294967295
	DD	4294967295
	DD	0
	DD	0
	DD	1610612736
	DD	1282694960
	DD	0
	DD	1256952721
	DD	536870912
	DD	1229269500
	DD	3221225472
	DD	1202544455
	DD	0
	DD	1176818551
	DD	2147483648
	DD	1148939346
	DD	536870912
	DD	1124701124
	DD	3758096384
	DD	1099498527
	DD	3758096384
	DD	1071929578
	DD	1342177280
	DD	1046982385
	DD	805306368
	DD	1020320658
	DD	2147483648
	DD	993817732
	DD	0
	DD	968598976
	DD	2684354560
	DD	942220475
	DD	2415919104
	DD	915426956
	DD	0
	DD	885849629
	DD	536870912
	DD	863855510
	DD	1610612736
	DD	836031391
	DD	4026531840
	DD	810828058
	DD	1073741824
	DD	784674491
	DD	1610612736
	DD	757207974
	DD	3489660928
	DD	732020890
	DD	0
	DD	703061904
	DD	1610612736
	DD	679713053
	DD	2147483648
	DD	652001705
	DD	1073741824
	DD	626850382
	DD	2147483648
	DD	597786158
	DD	805306368
	DD	575535400
	DD	536870912
	DD	548814833
	DD	268435456
	DD	523239288
	DD	3758096384
	DD	495550718
	DD	2952790016
	DD	469954840
	DD	1073741824
	DD	442925723
	DD	1073741824
	DD	416247094
	DD	3758096384
	DD	392128403
	DD	2147483648
	DD	364254062
	DD	3221225472
	DD	339643518
	DD	2684354560
	DD	313162111
	DD	805306368
	DD	286354345
	DD	2952790016
	DD	260811902
	DD	1610612736
	DD	234667567
	DD	3758096384
	DD	207520668
	DD	1073741824
	DD	182175017
	DD	4026531840
	DD	155380331
	DD	805306368
	DD	129417058
	DD	536870912
	DD	103691636
	DD	0
	DD	73760972
	DD	3221225472
	DD	48348958
	DD	536870912
	DD	23784188
_2il0floatpacket_26	DD	00000007cH,00000007cH,00000007cH,00000007cH
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
