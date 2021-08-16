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
	PUBLIC __svml_expf4_ha_l9

__svml_expf4_ha_l9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 232
        vmovaps   xmm5, xmm0
        vmovups   XMMWORD PTR [192+rsp], xmm14
        mov       QWORD PTR [208+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovups   xmm14, XMMWORD PTR [__svml_sexp_ha_data_internal]
        and       r13, -64
        vmovups   xmm4, XMMWORD PTR [__svml_sexp_ha_data_internal+64]
        vfmadd213ps xmm14, xmm5, xmm4
        vmovups   xmm2, XMMWORD PTR [__svml_sexp_ha_data_internal+256]
        vandps    xmm3, xmm5, XMMWORD PTR [__svml_sexp_ha_data_internal+128]
        vmovups   xmm0, XMMWORD PTR [__svml_sexp_ha_data_internal+384]
        vcmpnleps xmm1, xmm3, XMMWORD PTR [__svml_sexp_ha_data_internal+192]
        vsubps    xmm4, xmm14, xmm4
        vmovmskps edx, xmm1
        vmovups   xmm1, XMMWORD PTR [__svml_sexp_ha_data_internal+448]
        vfnmadd213ps xmm2, xmm4, xmm5
        vfnmadd132ps xmm4, xmm2, XMMWORD PTR [__svml_sexp_ha_data_internal+320]
        vpermilps xmm3, xmm0, xmm14
        vmovups   xmm0, XMMWORD PTR [__svml_sexp_ha_data_internal+512]
        vfmadd213ps xmm0, xmm4, XMMWORD PTR [__svml_sexp_ha_data_internal+576]
        vmulps    xmm2, xmm4, xmm4
        vfmadd213ps xmm0, xmm4, XMMWORD PTR [__svml_sexp_ha_data_internal+640]
        vfmadd213ps xmm0, xmm2, xmm3
        vpermilps xmm1, xmm1, xmm14
        vpslld    xmm14, xmm14, 21
        vandps    xmm14, xmm14, XMMWORD PTR [__svml_sexp_ha_data_internal+704]
        vaddps    xmm0, xmm4, xmm0
        vmulps    xmm2, xmm1, xmm14
        mov       QWORD PTR [216+rsp], r13
        vfmadd213ps xmm0, xmm2, xmm2
        test      edx, edx
        jne       _B1_3

_B1_2::

        vmovups   xmm14, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [208+rsp]
        add       rsp, 232
        ret

_B1_3::

        vcmpgtps  xmm1, xmm5, XMMWORD PTR [__svml_sexp_ha_data_internal+2560]
        vcmpltps  xmm2, xmm5, XMMWORD PTR [__svml_sexp_ha_data_internal+2624]
        vblendvps xmm0, xmm0, XMMWORD PTR [_2il0floatpacket_37], xmm1
        vorps     xmm3, xmm1, xmm2
        vmovmskps eax, xmm3
        vandnps   xmm0, xmm2, xmm0
        andn      edx, eax, edx
        je        _B1_2

_B1_4::

        vmovups   XMMWORD PTR [r13], xmm5
        vmovups   XMMWORD PTR [64+r13], xmm0
        je        _B1_2

_B1_7::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B1_8::

        bt        esi, ebx
        jc        _B1_11

_B1_9::

        inc       ebx
        cmp       ebx, 4
        jl        _B1_8

_B1_10::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovups   xmm0, XMMWORD PTR [64+r13]
        jmp       _B1_2

_B1_11::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_sexp_ha_cout_rare_internal
        jmp       _B1_9
        ALIGN     16

_B1_12::

__svml_expf4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expf4_ha_l9_B1_B4:
	DD	401409
	DD	1758240
	DD	845848
	DD	1900811

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_7
	DD	imagerel _unwind___svml_expf4_ha_l9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expf4_ha_l9_B7_B11:
	DD	658945
	DD	287758
	DD	340999
	DD	845824
	DD	1758208
	DD	1900800

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_7
	DD	imagerel _B1_12
	DD	imagerel _unwind___svml_expf4_ha_l9_B7_B11

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_expf4_ha_e9

__svml_expf4_ha_e9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L10::

        sub       rsp, 232
        vmovups   XMMWORD PTR [192+rsp], xmm9
        mov       QWORD PTR [208+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmulps    xmm3, xmm0, XMMWORD PTR [__svml_sexp_ha_data_internal]
        and       r13, -64
        vroundps  xmm3, xmm3, 0
        vmulps    xmm4, xmm3, XMMWORD PTR [__svml_sexp_ha_data_internal+256]
        vaddps    xmm9, xmm3, XMMWORD PTR [__svml_sexp_ha_data_internal+64]
        vmulps    xmm3, xmm3, XMMWORD PTR [__svml_sexp_ha_data_internal+320]
        vsubps    xmm5, xmm0, xmm4
        vandps    xmm1, xmm0, XMMWORD PTR [__svml_sexp_ha_data_internal+128]
        vcmpnleps xmm2, xmm1, XMMWORD PTR [__svml_sexp_ha_data_internal+192]
        vsubps    xmm1, xmm5, xmm3
        vmovmskps eax, xmm2
        vmovups   xmm4, XMMWORD PTR [__svml_sexp_ha_data_internal+448]
        vmovups   xmm2, XMMWORD PTR [__svml_sexp_ha_data_internal+384]
        vmulps    xmm5, xmm1, XMMWORD PTR [__svml_sexp_ha_data_internal+512]
        vpermilps xmm3, xmm4, xmm9
        vmulps    xmm4, xmm1, xmm1
        vaddps    xmm5, xmm5, XMMWORD PTR [__svml_sexp_ha_data_internal+576]
        vmulps    xmm5, xmm1, xmm5
        vpermilps xmm2, xmm2, xmm9
        vpslld    xmm9, xmm9, 21
        vandps    xmm9, xmm9, XMMWORD PTR [__svml_sexp_ha_data_internal+704]
        mov       QWORD PTR [216+rsp], r13
        vaddps    xmm5, xmm5, XMMWORD PTR [__svml_sexp_ha_data_internal+640]
        vmulps    xmm4, xmm4, xmm5
        vaddps    xmm2, xmm2, xmm4
        vaddps    xmm1, xmm1, xmm2
        vmulps    xmm1, xmm3, xmm1
        vaddps    xmm2, xmm3, xmm1
        vmulps    xmm1, xmm9, xmm2
        test      eax, eax
        jne       _B2_3

_B2_2::

        vmovups   xmm9, XMMWORD PTR [192+rsp]
        vmovaps   xmm0, xmm1
        mov       r13, QWORD PTR [208+rsp]
        add       rsp, 232
        ret

_B2_3::

        vcmpgtps  xmm2, xmm0, XMMWORD PTR [__svml_sexp_ha_data_internal+2560]
        vcmpltps  xmm3, xmm0, XMMWORD PTR [__svml_sexp_ha_data_internal+2624]
        vblendvps xmm1, xmm1, XMMWORD PTR [_2il0floatpacket_37], xmm2
        vorps     xmm4, xmm2, xmm3
        vmovmskps edx, xmm4
        vandnps   xmm1, xmm3, xmm1
        not       edx
        and       edx, eax
        je        _B2_2

_B2_4::

        vmovups   XMMWORD PTR [r13], xmm0
        vmovups   XMMWORD PTR [64+r13], xmm1
        je        _B2_2

_B2_7::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B2_8::

        bt        esi, ebx
        jc        _B2_11

_B2_9::

        inc       ebx
        cmp       ebx, 4
        jl        _B2_8

_B2_10::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovups   xmm1, XMMWORD PTR [64+r13]
        jmp       _B2_2

_B2_11::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_sexp_ha_cout_rare_internal
        jmp       _B2_9
        ALIGN     16

_B2_12::

__svml_expf4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expf4_ha_e9_B1_B4:
	DD	400385
	DD	1758236
	DD	825364
	DD	1900811

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_7
	DD	imagerel _unwind___svml_expf4_ha_e9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expf4_ha_e9_B7_B11:
	DD	658945
	DD	287758
	DD	340999
	DD	825344
	DD	1758208
	DD	1900800

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_7
	DD	imagerel _B2_12
	DD	imagerel _unwind___svml_expf4_ha_e9_B7_B11

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_expf8_ha_l9

__svml_expf8_ha_l9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L19::

        sub       rsp, 552
        vmovups   YMMWORD PTR [496+rsp], ymm14
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [399+rsp]
        vmovups   ymm14, YMMWORD PTR [__svml_sexp_ha_data_internal]
        and       r13, -64
        vmovups   ymm4, YMMWORD PTR [__svml_sexp_ha_data_internal+64]
        vmovups   ymm2, YMMWORD PTR [__svml_sexp_ha_data_internal+256]
        vmovdqa   ymm5, ymm0
        vfmadd213ps ymm14, ymm5, ymm4
        vmovups   ymm0, YMMWORD PTR [__svml_sexp_ha_data_internal+448]
        vsubps    ymm4, ymm14, ymm4
        vandps    ymm1, ymm5, YMMWORD PTR [__svml_sexp_ha_data_internal+128]
        vcmpnle_uqps ymm3, ymm1, YMMWORD PTR [__svml_sexp_ha_data_internal+192]
        vfnmadd213ps ymm2, ymm4, ymm5
        vmovups   ymm1, YMMWORD PTR [__svml_sexp_ha_data_internal+384]
        vfnmadd132ps ymm4, ymm2, YMMWORD PTR [__svml_sexp_ha_data_internal+320]
        vmovmskps edx, ymm3
        vpermilps ymm3, ymm1, ymm14
        test      edx, edx
        vmovups   ymm1, YMMWORD PTR [__svml_sexp_ha_data_internal+512]
        vfmadd213ps ymm1, ymm4, YMMWORD PTR [__svml_sexp_ha_data_internal+576]
        vfmadd213ps ymm1, ymm4, YMMWORD PTR [__svml_sexp_ha_data_internal+640]
        vpermilps ymm2, ymm0, ymm14
        vmulps    ymm0, ymm4, ymm4
        vpslld    ymm14, ymm14, 21
        vfmadd213ps ymm1, ymm0, ymm3
        vandps    ymm14, ymm14, YMMWORD PTR [__svml_sexp_ha_data_internal+704]
        vmulps    ymm2, ymm2, ymm14
        vaddps    ymm0, ymm4, ymm1
        mov       QWORD PTR [536+rsp], r13
        vfmadd213ps ymm0, ymm2, ymm2
        jne       _B3_3

_B3_2::

        vmovups   ymm14, YMMWORD PTR [496+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B3_3::

        vcmpgt_oqps ymm1, ymm5, YMMWORD PTR [__svml_sexp_ha_data_internal+2560]
        vcmplt_oqps ymm2, ymm5, YMMWORD PTR [__svml_sexp_ha_data_internal+2624]
        vblendvps ymm0, ymm0, YMMWORD PTR [_2il0floatpacket_38], ymm1
        vorps     ymm3, ymm1, ymm2
        vmovmskps eax, ymm3
        vandnps   ymm0, ymm2, ymm0
        andn      edx, eax, edx
        je        _B3_2

_B3_4::

        vmovups   YMMWORD PTR [r13], ymm5
        vmovups   YMMWORD PTR [64+r13], ymm0
        je        _B3_2

_B3_7::

        xor       eax, eax
        vmovups   YMMWORD PTR [288+rsp], ymm6
        vmovups   YMMWORD PTR [256+rsp], ymm7
        vmovups   YMMWORD PTR [224+rsp], ymm8
        vmovups   YMMWORD PTR [192+rsp], ymm9
        vmovups   YMMWORD PTR [160+rsp], ymm10
        vmovups   YMMWORD PTR [128+rsp], ymm11
        vmovups   YMMWORD PTR [96+rsp], ymm12
        vmovups   YMMWORD PTR [64+rsp], ymm13
        vmovups   YMMWORD PTR [32+rsp], ymm15
        mov       QWORD PTR [328+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [320+rsp], rsi
        mov       esi, edx

_B3_8::

        bt        esi, ebx
        jc        _B3_11

_B3_9::

        inc       ebx
        cmp       ebx, 8
        jl        _B3_8

_B3_10::

        vmovups   ymm6, YMMWORD PTR [288+rsp]
        vmovups   ymm7, YMMWORD PTR [256+rsp]
        vmovups   ymm8, YMMWORD PTR [224+rsp]
        vmovups   ymm9, YMMWORD PTR [192+rsp]
        vmovups   ymm10, YMMWORD PTR [160+rsp]
        vmovups   ymm11, YMMWORD PTR [128+rsp]
        vmovups   ymm12, YMMWORD PTR [96+rsp]
        vmovups   ymm13, YMMWORD PTR [64+rsp]
        vmovups   ymm15, YMMWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [328+rsp]
        mov       rsi, QWORD PTR [320+rsp]
        jmp       _B3_2

_B3_11::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_sexp_ha_cout_rare_internal
        jmp       _B3_9
        ALIGN     16

_B3_12::

__svml_expf8_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expf8_ha_l9_B1_B4:
	DD	400385
	DD	4379676
	DD	2091028
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_7
	DD	imagerel _unwind___svml_expf8_ha_l9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expf8_ha_l9_B7_B11:
	DD	1858561
	DD	2647132
	DD	2700370
	DD	194634
	DD	317508
	DD	444478
	DD	571448
	DD	698415
	DD	825382
	DD	952349
	DD	1079316
	DD	1206283
	DD	2091008
	DD	4379648
	DD	4522240

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_7
	DD	imagerel _B3_12
	DD	imagerel _unwind___svml_expf8_ha_l9_B7_B11

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_expf16_ha_z0

__svml_expf16_ha_z0	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L46::

        vmovups   zmm4, ZMMWORD PTR [__svml_sexp_ha_data_internal_avx512+256]
        vmovups   zmm1, ZMMWORD PTR [__svml_sexp_ha_data_internal_avx512+320]
        vmovups   zmm2, ZMMWORD PTR [__svml_sexp_ha_data_internal_avx512+384]
        vmovups   zmm3, ZMMWORD PTR [__svml_sexp_ha_data_internal_avx512+448]
        vmovups   zmm22, ZMMWORD PTR [__svml_sexp_ha_data_internal_avx512]
        vmovups   zmm5, ZMMWORD PTR [__svml_sexp_ha_data_internal_avx512+576]
        vfmadd213ps zmm4, zmm0, zmm1 {rz-sae}
        vmovups   zmm24, ZMMWORD PTR [__svml_sexp_ha_data_internal_avx512+640]
        vmovups   zmm26, ZMMWORD PTR [__svml_sexp_ha_data_internal_avx512+128]
        vsubps    zmm29, zmm4, zmm1 {rn-sae}
        vpermt2ps zmm22, zmm4, ZMMWORD PTR [__svml_sexp_ha_data_internal_avx512+64]
        vpermt2ps zmm26, zmm4, ZMMWORD PTR [__svml_sexp_ha_data_internal_avx512+192]
        vfnmadd231ps zmm0, zmm2, zmm29 {rn-sae}
        vfnmadd231ps zmm0, zmm3, zmm29 {rn-sae}
        vandps    zmm23, zmm0, ZMMWORD PTR [__svml_sexp_ha_data_internal_avx512+512]
        vmulps    zmm27, zmm23, zmm23 {rn-sae}
        vfmadd231ps zmm24, zmm5, zmm23 {rn-sae}
        vaddps    zmm25, zmm22, zmm23 {rn-sae}
        vfmadd213ps zmm27, zmm24, zmm25 {rn-sae}
        vfmadd213ps zmm27, zmm26, zmm26 {rn-sae}
        vandps    zmm28, zmm27, ZMMWORD PTR [__svml_sexp_ha_data_internal_avx512+704]
        vscalefps zmm0, zmm28, zmm29 {rn-sae}
        ret
        ALIGN     16

_B4_2::

__svml_expf16_ha_z0 ENDP

_TEXT	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_expf8_ha_e9

__svml_expf8_ha_e9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L47::

        sub       rsp, 552
        vmovups   YMMWORD PTR [496+rsp], ymm10
        vmovups   YMMWORD PTR [464+rsp], ymm6
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [367+rsp]
        vmovaps   ymm6, ymm0
        and       r13, -64
        vmulps    ymm3, ymm6, YMMWORD PTR [__svml_sexp_ha_data_internal]
        vandps    ymm2, ymm6, YMMWORD PTR [__svml_sexp_ha_data_internal+128]
        vcmpnle_uqps ymm5, ymm2, YMMWORD PTR [__svml_sexp_ha_data_internal+192]
        vroundps  ymm4, ymm3, 0
        vpxor     xmm3, xmm3, xmm3
        vaddps    ymm0, ymm4, YMMWORD PTR [__svml_sexp_ha_data_internal+64]
        mov       QWORD PTR [536+rsp], r13
        vextractf128 xmm1, ymm5, 1
        vpackssdw xmm10, xmm5, xmm1
        vmulps    ymm5, ymm4, YMMWORD PTR [__svml_sexp_ha_data_internal+256]
        vmulps    ymm4, ymm4, YMMWORD PTR [__svml_sexp_ha_data_internal+320]
        vpacksswb xmm2, xmm10, xmm3
        vmovups   ymm10, YMMWORD PTR [__svml_sexp_ha_data_internal+384]
        vsubps    ymm1, ymm6, ymm5
        vpmovmskb edx, xmm2
        vsubps    ymm3, ymm1, ymm4
        vmovups   ymm4, YMMWORD PTR [__svml_sexp_ha_data_internal+448]
        vmulps    ymm5, ymm3, YMMWORD PTR [__svml_sexp_ha_data_internal+512]
        vaddps    ymm1, ymm5, YMMWORD PTR [__svml_sexp_ha_data_internal+576]
        vmulps    ymm5, ymm3, ymm3
        vpermilps ymm2, ymm10, ymm0
        vmulps    ymm10, ymm3, ymm1
        vaddps    ymm1, ymm10, YMMWORD PTR [__svml_sexp_ha_data_internal+640]
        vmulps    ymm1, ymm5, ymm1
        vaddps    ymm2, ymm2, ymm1
        vaddps    ymm3, ymm3, ymm2
        vpermilps ymm4, ymm4, ymm0
        vmulps    ymm5, ymm4, ymm3
        vaddps    ymm4, ymm4, ymm5
        vpslld    xmm10, xmm0, 21
        vextractf128 xmm0, ymm0, 1
        vpslld    xmm0, xmm0, 21
        vinsertf128 ymm10, ymm10, xmm0, 1
        vandps    ymm10, ymm10, YMMWORD PTR [__svml_sexp_ha_data_internal+704]
        vmulps    ymm0, ymm10, ymm4
        test      dl, dl
        jne       _B5_12

_B5_2::

        test      dl, dl
        jne       _B5_4

_B5_3::

        vmovups   ymm6, YMMWORD PTR [464+rsp]
        vmovups   ymm10, YMMWORD PTR [496+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B5_4::

        vmovups   YMMWORD PTR [r13], ymm6
        vmovups   YMMWORD PTR [64+r13], ymm0
        test      edx, edx
        je        _B5_3

_B5_7::

        xor       eax, eax
        vmovups   YMMWORD PTR [256+rsp], ymm7
        vmovups   YMMWORD PTR [224+rsp], ymm8
        vmovups   YMMWORD PTR [192+rsp], ymm9
        vmovups   YMMWORD PTR [160+rsp], ymm11
        vmovups   YMMWORD PTR [128+rsp], ymm12
        vmovups   YMMWORD PTR [96+rsp], ymm13
        vmovups   YMMWORD PTR [64+rsp], ymm14
        vmovups   YMMWORD PTR [32+rsp], ymm15
        mov       QWORD PTR [296+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [288+rsp], rsi
        mov       esi, edx

_B5_8::

        bt        esi, ebx
        jc        _B5_11

_B5_9::

        inc       ebx
        cmp       ebx, 8
        jl        _B5_8

_B5_10::

        vmovups   ymm7, YMMWORD PTR [256+rsp]
        vmovups   ymm8, YMMWORD PTR [224+rsp]
        vmovups   ymm9, YMMWORD PTR [192+rsp]
        vmovups   ymm11, YMMWORD PTR [160+rsp]
        vmovups   ymm12, YMMWORD PTR [128+rsp]
        vmovups   ymm13, YMMWORD PTR [96+rsp]
        vmovups   ymm14, YMMWORD PTR [64+rsp]
        vmovups   ymm15, YMMWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [296+rsp]
        mov       rsi, QWORD PTR [288+rsp]
        jmp       _B5_3

_B5_11::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_sexp_ha_cout_rare_internal
        jmp       _B5_9

_B5_12::

        vcmpgt_oqps ymm1, ymm6, YMMWORD PTR [__svml_sexp_ha_data_internal+2560]
        vcmplt_oqps ymm2, ymm6, YMMWORD PTR [__svml_sexp_ha_data_internal+2624]
        vpxor     xmm10, xmm10, xmm10
        vblendvps ymm0, ymm0, YMMWORD PTR [_2il0floatpacket_38], ymm1
        vorps     ymm3, ymm1, ymm2
        vandnps   ymm0, ymm2, ymm0
        vextractf128 xmm4, ymm3, 1
        vpackssdw xmm5, xmm3, xmm4
        vpacksswb xmm1, xmm5, xmm10
        vpmovmskb eax, xmm1
        not       eax
        and       eax, edx
        movzx     edx, al
        jmp       _B5_2
        ALIGN     16

_B5_13::

__svml_expf8_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expf8_ha_e9_B1_B4:
	DD	533761
	DD	4379685
	DD	1927197
	DD	2074644
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_7
	DD	imagerel _unwind___svml_expf8_ha_e9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expf8_ha_e9_B7_B11:
	DD	1332001
	DD	2384979
	DD	2438217
	DD	194625
	DD	321595
	DD	448565
	DD	575535
	DD	702502
	DD	825373
	DD	952340
	DD	1079307
	DD	imagerel _B5_1
	DD	imagerel _B5_7
	DD	imagerel _unwind___svml_expf8_ha_e9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_7
	DD	imagerel _B5_12
	DD	imagerel _unwind___svml_expf8_ha_e9_B7_B11

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expf8_ha_e9_B12_B12:
	DD	33
	DD	imagerel _B5_1
	DD	imagerel _B5_7
	DD	imagerel _unwind___svml_expf8_ha_e9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_12
	DD	imagerel _B5_13
	DD	imagerel _unwind___svml_expf8_ha_e9_B12_B12

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_expf4_ha_ex

__svml_expf4_ha_ex	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L74::

        sub       rsp, 264
        movaps    xmm3, xmm0
        movups    XMMWORD PTR [224+rsp], xmm14
        lea       rax, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [192+rsp], xmm13
        movups    XMMWORD PTR [208+rsp], xmm12
        mov       QWORD PTR [240+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movups    xmm0, XMMWORD PTR [__svml_sexp_ha_data_internal+1792]
        and       r13, -64
        mulps     xmm0, xmm3
        movups    xmm5, XMMWORD PTR [__svml_sexp_ha_data_internal+1856]
        movdqu    xmm13, XMMWORD PTR [__svml_sexp_ha_data_internal+2432]
        addps     xmm0, xmm5
        pand      xmm13, xmm3
        movaps    xmm4, xmm0
        pcmpgtd   xmm13, XMMWORD PTR [__svml_sexp_ha_data_internal+2496]
        subps     xmm4, xmm5
        movmskps  edx, xmm13
        movdqu    xmm13, XMMWORD PTR [__svml_sexp_ha_data_internal+2176]
        pand      xmm13, xmm0
        pslld     xmm13, 3
        pshufd    xmm14, xmm13, 1
        movd      ecx, xmm13
        movd      r8d, xmm14
        movups    xmm2, XMMWORD PTR [__svml_sexp_ha_data_internal+1920]
        movups    xmm1, XMMWORD PTR [__svml_sexp_ha_data_internal+1984]
        movsxd    rcx, ecx
        movsxd    r8, r8d
        mulps     xmm2, xmm4
        mulps     xmm1, xmm4
        movq      xmm5, QWORD PTR [imagerel(__svml_sexp_ha_data_internal)+768+rax+rcx]
        movq      xmm14, QWORD PTR [imagerel(__svml_sexp_ha_data_internal)+768+rax+r8]
        unpcklps  xmm5, xmm14
        movaps    xmm14, xmm3
        pshufd    xmm12, xmm13, 2
        subps     xmm14, xmm2
        movd      r9d, xmm12
        pshufd    xmm13, xmm13, 3
        subps     xmm14, xmm1
        movd      r10d, xmm13
        movaps    xmm1, xmm14
        mulps     xmm1, xmm14
        mulps     xmm1, XMMWORD PTR [__svml_sexp_ha_data_internal+2368]
        movsxd    r9, r9d
        addps     xmm14, xmm1
        movsxd    r10, r10d
        movq      xmm12, QWORD PTR [imagerel(__svml_sexp_ha_data_internal)+768+rax+r9]
        paddd     xmm0, XMMWORD PTR [__svml_sexp_ha_data_internal+2048]
        pslld     xmm0, 16
        movq      xmm13, QWORD PTR [imagerel(__svml_sexp_ha_data_internal)+768+rax+r10]
        unpcklps  xmm12, xmm13
        movaps    xmm13, xmm5
        movlhps   xmm13, xmm12
        mulps     xmm14, xmm13
        shufps    xmm5, xmm12, 238
        pand      xmm0, XMMWORD PTR [__svml_sexp_ha_data_internal+2112]
        addps     xmm5, xmm14
        mov       QWORD PTR [248+rsp], r13
        addps     xmm13, xmm5
        mulps     xmm0, xmm13
        test      edx, edx
        jne       _B6_3

_B6_2::

        movups    xmm12, XMMWORD PTR [208+rsp]
        movups    xmm13, XMMWORD PTR [192+rsp]
        movups    xmm14, XMMWORD PTR [224+rsp]
        mov       r13, QWORD PTR [240+rsp]
        add       rsp, 264
        ret

_B6_3::

        movups    xmm12, XMMWORD PTR [__svml_sexp_ha_data_internal+2560]
        movaps    xmm5, xmm3
        mov       eax, 2139095040
        cmpltps   xmm5, XMMWORD PTR [__svml_sexp_ha_data_internal+2624]
        cmpltps   xmm12, xmm3
        movd      xmm1, eax
        movaps    xmm4, xmm12
        pshufd    xmm2, xmm1, 0
        andnps    xmm4, xmm0
        andps     xmm2, xmm12
        orps      xmm12, xmm5
        movmskps  eax, xmm12
        orps      xmm4, xmm2
        movaps    xmm0, xmm5
        andnps    xmm0, xmm4
        not       eax
        and       eax, edx
        je        _B6_2

_B6_4::

        movups    XMMWORD PTR [r13], xmm3
        movups    XMMWORD PTR [64+r13], xmm0
        je        _B6_2

_B6_7::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

_B6_8::

        mov       ecx, ebx
        mov       edx, 1
        shl       edx, cl
        test      esi, edx
        jne       _B6_11

_B6_9::

        inc       ebx
        cmp       ebx, 4
        jl        _B6_8

_B6_10::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [64+r13]
        jmp       _B6_2

_B6_11::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_sexp_ha_cout_rare_internal
        jmp       _B6_9
        ALIGN     16

_B6_12::

__svml_expf4_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expf4_ha_ex_B1_B4:
	DD	669697
	DD	2020408
	DD	903216
	DD	841767
	DD	976919
	DD	2162955

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_expf4_ha_ex_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_expf4_ha_ex_B7_B11:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_expf4_ha_ex_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_7
	DD	imagerel _B6_12
	DD	imagerel _unwind___svml_expf4_ha_ex_B7_B11

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sexp_ha_cout_rare_internal

__svml_sexp_ha_cout_rare_internal	PROC

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L87::

        sub       rsp, 104
        mov       r9, rdx
        movzx     edx, WORD PTR [2+rcx]
        xor       eax, eax
        and       edx, 32640
        shr       edx, 7
        cmp       edx, 255
        je        _B7_17

_B7_2::

        pxor      xmm1, xmm1
        cvtss2sd  xmm1, DWORD PTR [rcx]
        cmp       edx, 74
        jle       _B7_15

_B7_3::

        movsd     xmm0, QWORD PTR [_vmldExpHATab+1080]
        comisd    xmm0, xmm1
        jb        _B7_14

_B7_4::

        comisd    xmm1, QWORD PTR [_vmldExpHATab+1096]
        jb        _B7_13

_B7_5::

        movsd     xmm2, QWORD PTR [_vmldExpHATab+1024]
        mulsd     xmm2, xmm1
        movsd     QWORD PTR [88+rsp], xmm2
        movaps    xmm2, xmm1
        movsd     xmm3, QWORD PTR [88+rsp]
        movsd     xmm0, QWORD PTR [_vmldExpHATab+1136]
        movsd     QWORD PTR [80+rsp], xmm0
        addsd     xmm3, QWORD PTR [_vmldExpHATab+1032]
        movsd     QWORD PTR [96+rsp], xmm3
        movsd     xmm4, QWORD PTR [96+rsp]
        mov       r10d, DWORD PTR [96+rsp]
        mov       r8d, r10d
        and       r10d, 63
        subsd     xmm4, QWORD PTR [_vmldExpHATab+1032]
        movsd     QWORD PTR [88+rsp], xmm4
        lea       edx, DWORD PTR [r10+r10]
        movsd     xmm5, QWORD PTR [88+rsp]
        lea       r11d, DWORD PTR [1+r10+r10]
        mulsd     xmm5, QWORD PTR [_vmldExpHATab+1104]
        lea       r10, QWORD PTR [__ImageBase]
        movsd     xmm0, QWORD PTR [88+rsp]
        subsd     xmm2, xmm5
        mulsd     xmm0, QWORD PTR [_vmldExpHATab+1112]
        shr       r8d, 6
        subsd     xmm2, xmm0
        comisd    xmm1, QWORD PTR [_vmldExpHATab+1088]
        movsd     xmm0, QWORD PTR [_vmldExpHATab+1072]
        lea       ecx, DWORD PTR [1023+r8]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [_vmldExpHATab+1064]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [_vmldExpHATab+1056]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [_vmldExpHATab+1048]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [_vmldExpHATab+1040]
        mulsd     xmm0, xmm2
        mulsd     xmm0, xmm2
        addsd     xmm0, xmm2
        movsd     xmm2, QWORD PTR [imagerel(_vmldExpHATab)+r10+rdx*8]
        addsd     xmm0, QWORD PTR [imagerel(_vmldExpHATab)+r10+r11*8]
        mulsd     xmm0, xmm2
        jb        _B7_9

_B7_6::

        and       ecx, 2047
        addsd     xmm0, xmm2
        cmp       ecx, 2046
        ja        _B7_8

_B7_7::

        movzx     edx, WORD PTR [_vmldExpHATab+1142]
        shl       ecx, 4
        and       edx, -32753
        or        edx, ecx
        mov       WORD PTR [86+rsp], dx
        movsd     xmm1, QWORD PTR [80+rsp]
        mulsd     xmm0, xmm1
        cvtsd2ss  xmm0, xmm0
        movss     DWORD PTR [r9], xmm0
        add       rsp, 104
        ret

_B7_8::

        dec       ecx
        and       ecx, 2047
        movzx     edx, WORD PTR [86+rsp]
        shl       ecx, 4
        and       edx, -32753
        or        edx, ecx
        mov       WORD PTR [86+rsp], dx
        movsd     xmm1, QWORD PTR [80+rsp]
        mulsd     xmm0, xmm1
        mulsd     xmm0, QWORD PTR [_vmldExpHATab+1152]
        cvtsd2ss  xmm0, xmm0
        movss     DWORD PTR [r9], xmm0
        add       rsp, 104
        ret

_B7_9::

        add       r8d, 1083
        and       r8d, 2047
        mov       eax, r8d
        movzx     edx, WORD PTR [86+rsp]
        shl       eax, 4
        and       edx, -32753
        or        edx, eax
        mov       WORD PTR [86+rsp], dx
        movsd     xmm3, QWORD PTR [80+rsp]
        mulsd     xmm0, xmm3
        mulsd     xmm3, xmm2
        movaps    xmm1, xmm0
        addsd     xmm1, xmm3
        cmp       r8d, 50
        ja        _B7_11

_B7_10::

        movsd     xmm0, QWORD PTR [_vmldExpHATab+1160]
        mulsd     xmm1, xmm0
        cvtsd2ss  xmm1, xmm1
        jmp       _B7_12

_B7_11::

        movsd     QWORD PTR [32+rsp], xmm1
        movsd     xmm1, QWORD PTR [32+rsp]
        subsd     xmm3, xmm1
        movsd     QWORD PTR [40+rsp], xmm3
        movsd     xmm2, QWORD PTR [40+rsp]
        addsd     xmm2, xmm0
        movsd     QWORD PTR [40+rsp], xmm2
        movsd     xmm0, QWORD PTR [32+rsp]
        mulsd     xmm0, QWORD PTR [_vmldExpHATab+1168]
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
        mulsd     xmm0, QWORD PTR [_vmldExpHATab+1160]
        movsd     QWORD PTR [64+rsp], xmm0
        movsd     xmm1, QWORD PTR [72+rsp]
        mulsd     xmm1, QWORD PTR [_vmldExpHATab+1160]
        movsd     QWORD PTR [72+rsp], xmm1
        movsd     xmm1, QWORD PTR [64+rsp]
        movsd     xmm5, QWORD PTR [72+rsp]
        addsd     xmm1, xmm5
        cvtsd2ss  xmm1, xmm1

_B7_12::

        movss     DWORD PTR [r9], xmm1
        mov       eax, 4
        add       rsp, 104
        ret

_B7_13::

        movsd     xmm0, QWORD PTR [_vmldExpHATab+1120]
        mov       eax, 4
        mulsd     xmm0, xmm0
        cvtsd2ss  xmm0, xmm0
        movss     DWORD PTR [r9], xmm0
        add       rsp, 104
        ret

_B7_14::

        movsd     xmm0, QWORD PTR [_vmldExpHATab+1128]
        mov       eax, 3
        mulsd     xmm0, xmm0
        cvtsd2ss  xmm0, xmm0
        movss     DWORD PTR [r9], xmm0
        add       rsp, 104
        ret

_B7_15::

        movsd     xmm0, QWORD PTR [_vmldExpHATab+1144]
        addsd     xmm0, xmm1
        cvtsd2ss  xmm0, xmm0
        movss     DWORD PTR [r9], xmm0

_B7_16::

        add       rsp, 104
        ret

_B7_17::

        mov       dl, BYTE PTR [3+rcx]
        and       dl, -128
        cmp       dl, -128
        je        _B7_19

_B7_18::

        movss     xmm0, DWORD PTR [rcx]
        mulss     xmm0, xmm0
        movss     DWORD PTR [r9], xmm0
        add       rsp, 104
        ret

_B7_19::

        test      DWORD PTR [rcx], 8388607
        jne       _B7_18

_B7_20::

        movsd     xmm0, QWORD PTR [_vmldExpHATab+1136]
        cvtsd2ss  xmm0, xmm0
        movss     DWORD PTR [r9], xmm0
        add       rsp, 104
        ret
        ALIGN     16

_B7_21::

__svml_sexp_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sexp_ha_cout_rare_internal_B1_B20:
	DD	67585
	DD	49672

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_21
	DD	imagerel _unwind___svml_sexp_ha_cout_rare_internal_B1_B20

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_sexp_ha_data_internal_avx512
__svml_sexp_ha_data_internal_avx512	DD	0
	DD	3007986186
	DD	860277610
	DD	3010384254
	DD	2991457809
	DD	3008462297
	DD	860562562
	DD	3004532446
	DD	856238081
	DD	3001480295
	DD	857441778
	DD	815380209
	DD	3003456168
	DD	3001196762
	DD	2986372182
	DD	3006683458
	DD	848495278
	DD	851809756
	DD	3003311522
	DD	2995654817
	DD	833868005
	DD	3004843819
	DD	835836658
	DD	3003498340
	DD	2994528642
	DD	3002229827
	DD	2981408986
	DD	2983889551
	DD	2983366846
	DD	3000350873
	DD	833659207
	DD	2987748092
	DD	1065353216
	DD	1065536903
	DD	1065724611
	DD	1065916431
	DD	1066112450
	DD	1066312762
	DD	1066517459
	DD	1066726640
	DD	1066940400
	DD	1067158842
	DD	1067382066
	DD	1067610179
	DD	1067843287
	DD	1068081499
	DD	1068324927
	DD	1068573686
	DD	1068827891
	DD	1069087663
	DD	1069353124
	DD	1069624397
	DD	1069901610
	DD	1070184894
	DD	1070474380
	DD	1070770206
	DD	1071072509
	DD	1071381432
	DD	1071697119
	DD	1072019719
	DD	1072349383
	DD	1072686266
	DD	1073030525
	DD	1073382323
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1069066811
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1220542464
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	1060205080
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	2969756424
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	3221225471
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1042983923
	DD	1056964854
	DD	1056964854
	DD	1056964854
	DD	1056964854
	DD	1056964854
	DD	1056964854
	DD	1056964854
	DD	1056964854
	DD	1056964854
	DD	1056964854
	DD	1056964854
	DD	1056964854
	DD	1056964854
	DD	1056964854
	DD	1056964854
	DD	1056964854
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
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	796917760
	DD	796917760
	DD	796917760
	DD	796917760
	DD	796917760
	DD	796917760
	DD	796917760
	DD	796917760
	DD	796917760
	DD	796917760
	DD	796917760
	DD	796917760
	DD	796917760
	DD	796917760
	DD	796917760
	DD	796917760
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
	PUBLIC __svml_sexp_ha_data_internal
__svml_sexp_ha_data_internal	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1085844027
	DD	1262486012
	DD	1262486012
	DD	1262486012
	DD	1262486012
	DD	1262486012
	DD	1262486012
	DD	1262486012
	DD	1262486012
	DD	1262486012
	DD	1262486012
	DD	1262486012
	DD	1262486012
	DD	1262486012
	DD	1262486012
	DD	1262486012
	DD	1262486012
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
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	1043427328
	DD	923139572
	DD	923139572
	DD	923139572
	DD	923139572
	DD	923139572
	DD	923139572
	DD	923139572
	DD	923139572
	DD	923139572
	DD	923139572
	DD	923139572
	DD	923139572
	DD	923139572
	DD	923139572
	DD	923139572
	DD	923139572
	DD	0
	DD	856238081
	DD	848495278
	DD	2994528642
	DD	0
	DD	856238081
	DD	848495278
	DD	2994528642
	DD	0
	DD	856238081
	DD	848495278
	DD	2994528642
	DD	0
	DD	856238081
	DD	848495278
	DD	2994528642
	DD	1065353216
	DD	1066940400
	DD	1068827891
	DD	1071072509
	DD	1065353216
	DD	1066940400
	DD	1068827891
	DD	1071072509
	DD	1065353216
	DD	1066940400
	DD	1068827891
	DD	1071072509
	DD	1065353216
	DD	1066940400
	DD	1068827891
	DD	1071072509
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1026210578
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1042987794
	DD	1056964607
	DD	1056964607
	DD	1056964607
	DD	1056964607
	DD	1056964607
	DD	1056964607
	DD	1056964607
	DD	1056964607
	DD	1056964607
	DD	1056964607
	DD	1056964607
	DD	1056964607
	DD	1056964607
	DD	1056964607
	DD	1056964607
	DD	1056964607
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
	DD	1065353216
	DD	0
	DD	1065398765
	DD	857715507
	DD	1065444562
	DD	2982428624
	DD	1065490607
	DD	856090616
	DD	1065536903
	DD	3008276393
	DD	1065583449
	DD	857595758
	DD	1065630249
	DD	3002158215
	DD	1065677302
	DD	858550072
	DD	1065724611
	DD	860854417
	DD	1065772177
	DD	859251839
	DD	1065820002
	DD	3008545510
	DD	1065868085
	DD	864021810
	DD	1065916431
	DD	3011435083
	DD	1065965038
	DD	819487640
	DD	1066013909
	DD	862622430
	DD	1066063046
	DD	856316133
	DD	1066112450
	DD	2992679841
	DD	1066162122
	DD	3001970243
	DD	1066212063
	DD	859241655
	DD	1066262276
	DD	847285146
	DD	1066312762
	DD	3010032741
	DD	1066363521
	DD	843003463
	DD	1066414556
	DD	856041677
	DD	1066465869
	DD	3009946167
	DD	1066517459
	DD	862410276
	DD	1066569330
	DD	863709796
	DD	1066621483
	DD	851201948
	DD	1066673919
	DD	838513088
	DD	1066726640
	DD	3006136850
	DD	1066779647
	DD	3010613863
	DD	1066832941
	DD	853311585
	DD	1066886525
	DD	856941301
	DD	1066940400
	DD	857938801
	DD	1066994568
	DD	2993474036
	DD	1067049030
	DD	3009003152
	DD	1067103787
	DD	2967126046
	DD	1067158842
	DD	3003929955
	DD	1067214195
	DD	860904838
	DD	1067269850
	DD	3006375425
	DD	1067325806
	DD	843377209
	DD	1067382066
	DD	859906882
	DD	1067438632
	DD	848662531
	DD	1067495505
	DD	838571797
	DD	1067552687
	DD	3000363757
	DD	1067610179
	DD	818090586
	DD	1067667983
	DD	860999685
	DD	1067726102
	DD	3005475891
	DD	1067784536
	DD	3010626242
	DD	1067843287
	DD	3006045534
	DD	1067902357
	DD	2998944862
	DD	1067961748
	DD	3004575030
	DD	1068021461
	DD	842258832
	DD	1068081499
	DD	3004574472
	DD	1068141862
	DD	858118433
	DD	1068202554
	DD	3004476802
	DD	1068263575
	DD	3006694272
	DD	1068324927
	DD	2989353718
	DD	1068386612
	DD	858100843
	DD	1068448633
	DD	3005558251
	DD	1068510990
	DD	3000050815
	DD	1068573686
	DD	3011271336
	DD	1068636722
	DD	3006477262
	DD	1068700100
	DD	840255625
	DD	1068763823
	DD	3007001961
	DD	1068827891
	DD	852486010
	DD	1068892308
	DD	3006218836
	DD	1068957074
	DD	2993076596
	DD	1069022192
	DD	3000356208
	DD	1069087663
	DD	856606199
	DD	1069153490
	DD	856315927
	DD	1069219675
	DD	3004946819
	DD	1069286218
	DD	863888852
	DD	1069353124
	DD	3007401960
	DD	1069420392
	DD	832069785
	DD	1069488026
	DD	3004369690
	DD	1069556027
	DD	3007061599
	DD	1069624397
	DD	3000395326
	DD	1069693138
	DD	851736822
	DD	1069762253
	DD	2996268450
	DD	1069831743
	DD	2999890733
	DD	1069901610
	DD	839559223
	DD	1069971857
	DD	3004476343
	DD	1070042485
	DD	3000848665
	DD	1070113496
	DD	859381756
	DD	1070184894
	DD	3010667426
	DD	1070256678
	DD	859604257
	DD	1070328853
	DD	2953234114
	DD	1070401420
	DD	3010682756
	DD	1070474380
	DD	841546788
	DD	1070547737
	DD	2999163804
	DD	1070621492
	DD	2996061011
	DD	1070695647
	DD	860136147
	DD	1070770206
	DD	3009158570
	DD	1070845169
	DD	3008165066
	DD	1070920539
	DD	3005767716
	DD	1070996318
	DD	2955948569
	DD	1071072509
	DD	3000280458
	DD	1071149113
	DD	857267046
	DD	1071226134
	DD	3002473793
	DD	1071303572
	DD	861820308
	DD	1071381432
	DD	3008383516
	DD	1071459714
	DD	3010850715
	DD	1071538421
	DD	3011199971
	DD	1071617555
	DD	2995067967
	DD	1071697119
	DD	2988354393
	DD	1071777115
	DD	2889750092
	DD	1071857545
	DD	862289173
	DD	1071938413
	DD	3007287685
	DD	1072019719
	DD	2990808038
	DD	1072101467
	DD	3005883328
	DD	1072183658
	DD	857766040
	DD	1072266296
	DD	855693471
	DD	1072349383
	DD	2990610142
	DD	1072432921
	DD	3004393415
	DD	1072516912
	DD	846646433
	DD	1072601360
	DD	3008357562
	DD	1072686266
	DD	3007858250
	DD	1072771633
	DD	3006309869
	DD	1072857463
	DD	854681599
	DD	1072943760
	DD	2995197552
	DD	1073030525
	DD	841557046
	DD	1073117761
	DD	861464191
	DD	1073205471
	DD	862328226
	DD	1073293658
	DD	3003728983
	DD	1073382323
	DD	2995926872
	DD	1073471469
	DD	861522913
	DD	1073561100
	DD	835668076
	DD	1073651217
	DD	859371295
	DD	1127787067
	DD	1127787067
	DD	1127787067
	DD	1127787067
	DD	1127787067
	DD	1127787067
	DD	1127787067
	DD	1127787067
	DD	1127787067
	DD	1127787067
	DD	1127787067
	DD	1127787067
	DD	1127787067
	DD	1127787067
	DD	1127787067
	DD	1127787067
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
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	1001488384
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	3051257987
	DD	16256
	DD	16256
	DD	16256
	DD	16256
	DD	16256
	DD	16256
	DD	16256
	DD	16256
	DD	16256
	DD	16256
	DD	16256
	DD	16256
	DD	16256
	DD	16256
	DD	16256
	DD	16256
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
	DD	127
	DD	127
	DD	127
	DD	127
	DD	127
	DD	127
	DD	127
	DD	127
	DD	127
	DD	127
	DD	127
	DD	127
	DD	127
	DD	127
	DD	127
	DD	127
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	4294967232
	DD	864026624
	DD	864026624
	DD	864026624
	DD	864026624
	DD	864026624
	DD	864026624
	DD	864026624
	DD	864026624
	DD	864026624
	DD	864026624
	DD	864026624
	DD	864026624
	DD	864026624
	DD	864026624
	DD	864026624
	DD	864026624
	DD	1056964608
	DD	1056964608
	DD	1056964608
	DD	1056964608
	DD	1056964608
	DD	1056964608
	DD	1056964608
	DD	1056964608
	DD	1056964608
	DD	1056964608
	DD	1056964608
	DD	1056964608
	DD	1056964608
	DD	1056964608
	DD	1056964608
	DD	1056964608
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
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118743631
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	1118925335
	DD	3268407732
	DD	3268407732
	DD	3268407732
	DD	3268407732
	DD	3268407732
	DD	3268407732
	DD	3268407732
	DD	3268407732
	DD	3268407732
	DD	3268407732
	DD	3268407732
	DD	3268407732
	DD	3268407732
	DD	3268407732
	DD	3268407732
	DD	3268407732
_2il0floatpacket_38	DD	07f800000H,07f800000H,07f800000H,07f800000H,07f800000H,07f800000H,07f800000H,07f800000H
_vmldExpHATab	DD	0
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
	DD	3758096384
	DD	1079389762
	DD	3758096384
	DD	3226850697
	DD	2147483648
	DD	3227123254
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
_2il0floatpacket_37	DD	07f800000H,07f800000H,07f800000H,07f800000H
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
