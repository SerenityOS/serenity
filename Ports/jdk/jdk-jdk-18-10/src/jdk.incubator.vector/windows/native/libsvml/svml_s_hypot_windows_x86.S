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
	PUBLIC __svml_hypotf8_ha_l9

__svml_hypotf8_ha_l9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 616
        vmovups   YMMWORD PTR [496+rsp], ymm15
        vmovups   YMMWORD PTR [528+rsp], ymm14
        vmovups   YMMWORD PTR [560+rsp], ymm13
        vmovups   YMMWORD PTR [464+rsp], ymm8
        mov       QWORD PTR [592+rsp], r13
        lea       r13, QWORD PTR [303+rsp]
        vmovups   ymm2, YMMWORD PTR [__svml_shypot_ha_data_internal+128]
        and       r13, -64
        vmovups   ymm5, YMMWORD PTR [__svml_shypot_ha_data_internal]
        vmovups   ymm8, YMMWORD PTR [__svml_shypot_ha_data_internal+192]
        vmovaps   ymm14, ymm1
        vandps    ymm3, ymm0, ymm2
        vandps    ymm4, ymm14, ymm2
        vmaxps    ymm1, ymm3, ymm4
        vminps    ymm2, ymm4, ymm3
        vpsubd    ymm15, ymm1, ymm2
        vandps    ymm3, ymm1, ymm5
        vsubps    ymm4, ymm1, ymm3
        vaddps    ymm1, ymm1, ymm3
        vpcmpgtd  ymm13, ymm15, YMMWORD PTR [__svml_shypot_ha_data_internal+320]
        vandnps   ymm5, ymm13, ymm5
        vmulps    ymm4, ymm4, ymm1
        vmulps    ymm13, ymm3, ymm3
        vandps    ymm15, ymm2, ymm5
        vsubps    ymm5, ymm2, ymm15
        vfmadd231ps ymm13, ymm15, ymm15
        vfmadd231ps ymm4, ymm2, ymm5
        vmovups   ymm2, YMMWORD PTR [__svml_shypot_ha_data_internal+384]
        vfmadd213ps ymm5, ymm15, ymm4
        vaddps    ymm15, ymm13, ymm5
        vpcmpgtd  ymm1, ymm15, YMMWORD PTR [__svml_shypot_ha_data_internal+448]
        vpcmpgtd  ymm3, ymm2, ymm15
        vpor      ymm2, ymm3, ymm1
        vrsqrtps  ymm3, ymm15
        vmulps    ymm15, ymm3, ymm15
        vmulps    ymm3, ymm3, ymm8
        vfnmadd231ps ymm8, ymm3, ymm15
        vfmadd213ps ymm15, ymm8, ymm15
        vfmadd132ps ymm3, ymm3, ymm8
        vandps    ymm1, ymm15, YMMWORD PTR [__svml_shypot_ha_data_internal+64]
        vfmsub231ps ymm13, ymm1, ymm1
        vsubps    ymm8, ymm15, ymm1
        vfmadd213ps ymm1, ymm8, ymm13
        vfmadd231ps ymm1, ymm8, ymm15
        vmovmskps edx, ymm2
        vsubps    ymm2, ymm1, ymm5
        mov       QWORD PTR [600+rsp], r13
        vfnmadd213ps ymm3, ymm2, ymm15
        test      edx, edx
        jne       _B1_3

_B1_2::

        vmovups   ymm8, YMMWORD PTR [464+rsp]
        vmovups   ymm13, YMMWORD PTR [560+rsp]
        vmovups   ymm14, YMMWORD PTR [528+rsp]
        vmovups   ymm15, YMMWORD PTR [496+rsp]
        mov       r13, QWORD PTR [592+rsp]
        vmovdqa   ymm0, ymm3
        add       rsp, 616
        ret

_B1_3::

        vmovups   YMMWORD PTR [r13], ymm0
        vmovups   YMMWORD PTR [64+r13], ymm14
        vmovups   YMMWORD PTR [128+r13], ymm3

_B1_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [192+rsp], ymm6
        vmovups   YMMWORD PTR [160+rsp], ymm7
        vmovups   YMMWORD PTR [128+rsp], ymm9
        vmovups   YMMWORD PTR [96+rsp], ymm10
        vmovups   YMMWORD PTR [64+rsp], ymm11
        vmovups   YMMWORD PTR [32+rsp], ymm12
        mov       QWORD PTR [232+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [224+rsp], rsi
        mov       esi, edx

_B1_7::

        bt        esi, ebx
        jc        _B1_10

_B1_8::

        inc       ebx
        cmp       ebx, 8
        jl        _B1_7

_B1_9::

        vmovups   ymm6, YMMWORD PTR [192+rsp]
        vmovups   ymm7, YMMWORD PTR [160+rsp]
        vmovups   ymm9, YMMWORD PTR [128+rsp]
        vmovups   ymm10, YMMWORD PTR [96+rsp]
        vmovups   ymm11, YMMWORD PTR [64+rsp]
        vmovups   ymm12, YMMWORD PTR [32+rsp]
        vmovups   ymm3, YMMWORD PTR [128+r13]
        mov       rbx, QWORD PTR [232+rsp]
        mov       rsi, QWORD PTR [224+rsp]
        jmp       _B1_2

_B1_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]
        lea       r8, QWORD PTR [128+r13+rbx*4]

        call      __svml_shypot_ha_cout_rare_internal
        jmp       _B1_8
        ALIGN     16

_B1_11::

__svml_hypotf8_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypotf8_ha_l9_B1_B3:
	DD	800513
	DD	4903991
	DD	1935407
	DD	2349094
	DD	2222109
	DD	2095124
	DD	5046539

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_hypotf8_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypotf8_ha_l9_B6_B10:
	DD	1065249
	DD	1860673
	DD	1913911
	DD	182319
	DD	309289
	DD	436259
	DD	563229
	DD	686100
	DD	813067
	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_hypotf8_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_6
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_hypotf8_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_hypotf4_ha_l9

__svml_hypotf4_ha_l9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L28::

        sub       rsp, 344
        vmovups   XMMWORD PTR [272+rsp], xmm15
        vmovups   XMMWORD PTR [288+rsp], xmm14
        vmovaps   xmm14, xmm1
        vmovups   XMMWORD PTR [304+rsp], xmm13
        vmovups   XMMWORD PTR [256+rsp], xmm8
        mov       QWORD PTR [320+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovups   xmm2, XMMWORD PTR [__svml_shypot_ha_data_internal+128]
        and       r13, -64
        vandps    xmm3, xmm0, xmm2
        vandps    xmm4, xmm14, xmm2
        vmaxps    xmm1, xmm3, xmm4
        vminps    xmm2, xmm4, xmm3
        vmovups   xmm8, XMMWORD PTR [__svml_shypot_ha_data_internal]
        vpsubd    xmm15, xmm1, xmm2
        vandps    xmm3, xmm1, xmm8
        vpcmpgtd  xmm13, xmm15, XMMWORD PTR [__svml_shypot_ha_data_internal+320]
        vsubps    xmm4, xmm1, xmm3
        vaddps    xmm1, xmm1, xmm3
        vandnps   xmm15, xmm13, xmm8
        vandps    xmm15, xmm2, xmm15
        vmulps    xmm4, xmm4, xmm1
        vsubps    xmm13, xmm2, xmm15
        vmulps    xmm8, xmm3, xmm3
        vfmadd231ps xmm4, xmm2, xmm13
        vmovups   xmm2, XMMWORD PTR [__svml_shypot_ha_data_internal+384]
        vmovups   xmm5, XMMWORD PTR [__svml_shypot_ha_data_internal+192]
        vfmadd231ps xmm8, xmm15, xmm15
        vfmadd213ps xmm13, xmm15, xmm4
        mov       QWORD PTR [328+rsp], r13
        vaddps    xmm15, xmm8, xmm13
        vpcmpgtd  xmm1, xmm15, XMMWORD PTR [__svml_shypot_ha_data_internal+448]
        vpcmpgtd  xmm3, xmm2, xmm15
        vpor      xmm2, xmm3, xmm1
        vrsqrtps  xmm3, xmm15
        vmovmskps edx, xmm2
        vmulps    xmm15, xmm3, xmm15
        vmulps    xmm3, xmm3, xmm5
        vfnmadd231ps xmm5, xmm3, xmm15
        vfmadd213ps xmm15, xmm5, xmm15
        vfmadd132ps xmm3, xmm3, xmm5
        vandps    xmm1, xmm15, XMMWORD PTR [__svml_shypot_ha_data_internal+64]
        vfmsub231ps xmm8, xmm1, xmm1
        vsubps    xmm5, xmm15, xmm1
        vfmadd213ps xmm1, xmm5, xmm8
        vfmadd231ps xmm1, xmm5, xmm15
        vsubps    xmm2, xmm1, xmm13
        vfnmadd213ps xmm3, xmm2, xmm15
        test      edx, edx
        jne       _B2_3

_B2_2::

        vmovups   xmm8, XMMWORD PTR [256+rsp]
        vmovaps   xmm0, xmm3
        vmovups   xmm13, XMMWORD PTR [304+rsp]
        vmovups   xmm14, XMMWORD PTR [288+rsp]
        vmovups   xmm15, XMMWORD PTR [272+rsp]
        mov       r13, QWORD PTR [320+rsp]
        add       rsp, 344
        ret

_B2_3::

        vmovups   XMMWORD PTR [r13], xmm0
        vmovups   XMMWORD PTR [64+r13], xmm14
        vmovups   XMMWORD PTR [128+r13], xmm3

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
        vmovups   xmm3, XMMWORD PTR [128+r13]
        jmp       _B2_2

_B2_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]
        lea       r8, QWORD PTR [128+r13+rbx*4]

        call      __svml_shypot_ha_cout_rare_internal
        jmp       _B2_8
        ALIGN     16

_B2_11::

__svml_hypotf4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypotf4_ha_l9_B1_B3:
	DD	801537
	DD	2675771
	DD	1083443
	DD	1300522
	DD	1239069
	DD	1177620
	DD	2818315

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_hypotf4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypotf4_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_hypotf4_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_6
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_hypotf4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_hypotf8_ha_e9

__svml_hypotf8_ha_e9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L43::

        sub       rsp, 616
        vmovups   YMMWORD PTR [432+rsp], ymm15
        vmovups   YMMWORD PTR [496+rsp], ymm14
        vmovups   YMMWORD PTR [464+rsp], ymm11
        vmovups   YMMWORD PTR [400+rsp], ymm9
        vmovups   YMMWORD PTR [560+rsp], ymm8
        vmovups   YMMWORD PTR [528+rsp], ymm7
        mov       QWORD PTR [592+rsp], r13
        lea       r13, QWORD PTR [239+rsp]
        vmovups   ymm2, YMMWORD PTR [__svml_shypot_ha_data_internal+128]
        and       r13, -64
        vmovups   ymm15, YMMWORD PTR [__svml_shypot_ha_data_internal]
        vmovups   ymm7, YMMWORD PTR [__svml_shypot_ha_data_internal+192]
        vmovaps   ymm9, ymm1
        vandps    ymm14, ymm0, ymm2
        vandps    ymm11, ymm9, ymm2
        vmaxps    ymm4, ymm14, ymm11
        vminps    ymm1, ymm11, ymm14
        vmovups   xmm11, XMMWORD PTR [__svml_shypot_ha_data_internal+320]
        vandps    ymm2, ymm4, ymm15
        vsubps    ymm3, ymm4, ymm2
        mov       QWORD PTR [600+rsp], r13
        vpsubd    xmm8, xmm4, xmm1
        vextractf128 xmm14, ymm4, 1
        vpcmpgtd  xmm8, xmm8, xmm11
        vextractf128 xmm5, ymm1, 1
        vpsubd    xmm5, xmm14, xmm5
        vpcmpgtd  xmm11, xmm5, xmm11
        vinsertf128 ymm14, ymm8, xmm11, 1
        vandnps   ymm15, ymm14, ymm15
        vandps    ymm11, ymm1, ymm15
        vsubps    ymm5, ymm1, ymm11
        vmovups   xmm15, XMMWORD PTR [__svml_shypot_ha_data_internal+448]
        vmulps    ymm14, ymm2, ymm2
        vmulps    ymm8, ymm11, ymm11
        vmulps    ymm11, ymm11, ymm5
        vaddps    ymm2, ymm4, ymm2
        vaddps    ymm8, ymm14, ymm8
        vmulps    ymm4, ymm1, ymm5
        vmulps    ymm3, ymm3, ymm2
        vaddps    ymm14, ymm3, ymm4
        vmovups   xmm3, XMMWORD PTR [__svml_shypot_ha_data_internal+384]
        vaddps    ymm5, ymm14, ymm11
        vaddps    ymm14, ymm8, ymm5
        vpcmpgtd  xmm2, xmm3, xmm14
        vpcmpgtd  xmm4, xmm14, xmm15
        vpor      xmm11, xmm2, xmm4
        vrsqrtps  ymm2, ymm14
        vextractf128 xmm1, ymm14, 1
        vpcmpgtd  xmm3, xmm3, xmm1
        vpcmpgtd  xmm15, xmm1, xmm15
        vpor      xmm1, xmm3, xmm15
        vpackssdw xmm11, xmm11, xmm1
        vpxor     xmm15, xmm15, xmm15
        vpacksswb xmm1, xmm11, xmm15
        vmulps    ymm15, ymm2, ymm14
        vmulps    ymm3, ymm2, ymm7
        vpmovmskb edx, xmm1
        vmulps    ymm14, ymm15, ymm3
        vsubps    ymm11, ymm7, ymm14
        vmulps    ymm1, ymm15, ymm11
        vmulps    ymm7, ymm3, ymm11
        vaddps    ymm11, ymm15, ymm1
        vaddps    ymm14, ymm3, ymm7
        vandps    ymm3, ymm11, YMMWORD PTR [__svml_shypot_ha_data_internal+64]
        vsubps    ymm7, ymm11, ymm3
        vmulps    ymm2, ymm3, ymm3
        vmulps    ymm4, ymm3, ymm7
        vmulps    ymm1, ymm11, ymm7
        vsubps    ymm8, ymm2, ymm8
        vaddps    ymm15, ymm8, ymm4
        vaddps    ymm2, ymm15, ymm1
        vsubps    ymm5, ymm2, ymm5
        vmulps    ymm14, ymm14, ymm5
        vsubps    ymm1, ymm11, ymm14
        test      dl, dl
        jne       _B3_3

_B3_2::

        vmovups   ymm7, YMMWORD PTR [528+rsp]
        vmovups   ymm8, YMMWORD PTR [560+rsp]
        vmovups   ymm9, YMMWORD PTR [400+rsp]
        vmovups   ymm11, YMMWORD PTR [464+rsp]
        vmovups   ymm14, YMMWORD PTR [496+rsp]
        vmovups   ymm15, YMMWORD PTR [432+rsp]
        mov       r13, QWORD PTR [592+rsp]
        vmovaps   ymm0, ymm1
        add       rsp, 616
        ret

_B3_3::

        vmovups   YMMWORD PTR [r13], ymm0
        vmovups   YMMWORD PTR [64+r13], ymm9
        vmovups   YMMWORD PTR [128+r13], ymm1
        test      edx, edx
        je        _B3_2

_B3_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [128+rsp], ymm6
        vmovups   YMMWORD PTR [96+rsp], ymm10
        vmovups   YMMWORD PTR [64+rsp], ymm12
        vmovups   YMMWORD PTR [32+rsp], ymm13
        mov       QWORD PTR [168+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [160+rsp], rsi
        mov       esi, edx

_B3_7::

        bt        esi, ebx
        jc        _B3_10

_B3_8::

        inc       ebx
        cmp       ebx, 8
        jl        _B3_7

_B3_9::

        vmovups   ymm6, YMMWORD PTR [128+rsp]
        vmovups   ymm10, YMMWORD PTR [96+rsp]
        vmovups   ymm12, YMMWORD PTR [64+rsp]
        vmovups   ymm13, YMMWORD PTR [32+rsp]
        vmovups   ymm1, YMMWORD PTR [128+r13]
        mov       rbx, QWORD PTR [168+rsp]
        mov       rsi, QWORD PTR [160+rsp]
        jmp       _B3_2

_B3_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]
        lea       r8, QWORD PTR [128+r13+rbx*4]

        call      __svml_shypot_ha_cout_rare_internal
        jmp       _B3_8
        ALIGN     16

_B3_11::

__svml_hypotf8_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypotf8_ha_e9_B1_B3:
	DD	1067265
	DD	4904009
	DD	2193473
	DD	2328632
	DD	1677359
	DD	1947686
	DD	2091037
	DD	1832980
	DD	5046539

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_hypotf8_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypotf8_ha_e9_B6_B10:
	DD	798497
	DD	1336367
	DD	1389605
	DD	186397
	DD	313367
	DD	436241
	DD	550923
	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_hypotf8_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_6
	DD	imagerel _B3_11
	DD	imagerel _unwind___svml_hypotf8_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_hypotf4_ha_ex

__svml_hypotf4_ha_ex	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L70::

        sub       rsp, 360
        movups    XMMWORD PTR [288+rsp], xmm14
        movups    XMMWORD PTR [320+rsp], xmm11
        movaps    xmm11, xmm0
        movups    XMMWORD PTR [256+rsp], xmm10
        movups    XMMWORD PTR [272+rsp], xmm8
        movups    XMMWORD PTR [304+rsp], xmm7
        mov       QWORD PTR [336+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movups    xmm5, XMMWORD PTR [__svml_shypot_ha_data_internal+128]
        and       r13, -64
        andps     xmm0, xmm5
        andps     xmm5, xmm1
        movaps    xmm7, xmm0
        maxps     xmm7, xmm5
        minps     xmm5, xmm0
        movaps    xmm3, xmm7
        movaps    xmm4, xmm7
        psubd     xmm3, xmm5
        movaps    xmm10, xmm7
        movups    xmm2, XMMWORD PTR [__svml_shypot_ha_data_internal]
        pcmpgtd   xmm3, XMMWORD PTR [__svml_shypot_ha_data_internal+320]
        andps     xmm4, xmm2
        andnps    xmm3, xmm2
        movaps    xmm2, xmm5
        andps     xmm3, xmm5
        movaps    xmm8, xmm4
        movaps    xmm0, xmm3
        subps     xmm10, xmm4
        mulps     xmm8, xmm4
        subps     xmm2, xmm3
        addps     xmm7, xmm4
        mulps     xmm0, xmm3
        mulps     xmm5, xmm2
        mulps     xmm10, xmm7
        addps     xmm8, xmm0
        mulps     xmm3, xmm2
        addps     xmm10, xmm5
        movaps    xmm0, xmm8
        addps     xmm10, xmm3
        movdqu    xmm4, XMMWORD PTR [__svml_shypot_ha_data_internal+384]
        addps     xmm0, xmm10
        rsqrtps   xmm2, xmm0
        movups    xmm14, XMMWORD PTR [__svml_shypot_ha_data_internal+192]
        pcmpgtd   xmm4, xmm0
        movaps    xmm3, xmm0
        mulps     xmm0, xmm2
        mulps     xmm2, xmm14
        movaps    xmm5, xmm0
        mulps     xmm5, xmm2
        pcmpgtd   xmm3, XMMWORD PTR [__svml_shypot_ha_data_internal+448]
        subps     xmm14, xmm5
        por       xmm4, xmm3
        movaps    xmm3, xmm14
        mulps     xmm14, xmm0
        mulps     xmm3, xmm2
        movmskps  eax, xmm4
        addps     xmm0, xmm14
        addps     xmm2, xmm3
        movups    xmm4, XMMWORD PTR [__svml_shypot_ha_data_internal+64]
        movaps    xmm7, xmm0
        andps     xmm4, xmm0
        movaps    xmm14, xmm4
        subps     xmm7, xmm4
        mulps     xmm14, xmm4
        mulps     xmm4, xmm7
        subps     xmm14, xmm8
        mulps     xmm7, xmm0
        addps     xmm14, xmm4
        mov       QWORD PTR [344+rsp], r13
        addps     xmm14, xmm7
        subps     xmm14, xmm10
        mulps     xmm2, xmm14
        subps     xmm0, xmm2
        test      eax, eax
        jne       _B4_3

_B4_2::

        movups    xmm7, XMMWORD PTR [304+rsp]
        movups    xmm8, XMMWORD PTR [272+rsp]
        movups    xmm10, XMMWORD PTR [256+rsp]
        movups    xmm11, XMMWORD PTR [320+rsp]
        movups    xmm14, XMMWORD PTR [288+rsp]
        mov       r13, QWORD PTR [336+rsp]
        add       rsp, 360
        ret

_B4_3::

        movups    XMMWORD PTR [r13], xmm11
        movups    XMMWORD PTR [64+r13], xmm1
        movups    XMMWORD PTR [128+r13], xmm0

_B4_6::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

_B4_7::

        mov       ecx, ebx
        mov       edx, 1
        shl       edx, cl
        test      esi, edx
        jne       _B4_10

_B4_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B4_7

_B4_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [128+r13]
        jmp       _B4_2

_B4_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]
        lea       r8, QWORD PTR [128+r13+rbx*4]

        call      __svml_shypot_ha_cout_rare_internal
        jmp       _B4_8
        ALIGN     16

_B4_11::

__svml_hypotf4_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypotf4_ha_ex_B1_B3:
	DD	934657
	DD	2806851
	DD	1275963
	DD	1148979
	DD	1091626
	DD	1357853
	DD	1239060
	DD	2949387

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_hypotf4_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypotf4_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_hypotf4_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_6
	DD	imagerel _B4_11
	DD	imagerel _unwind___svml_hypotf4_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_hypotf16_ha_z0

__svml_hypotf16_ha_z0	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L87::

        sub       rsp, 1400
        vmovaps   zmm2, zmm1
        vmulps    zmm23, zmm0, zmm0 {rn-sae}
        vmulps    zmm24, zmm2, zmm2 {rn-sae}
        vgetexpps zmm3, zmm0 {sae}
        vgetexpps zmm4, zmm2 {sae}
        vaddps    zmm1, zmm23, zmm24 {rn-sae}
        vmaxps    zmm25, zmm23, zmm24 {sae}
        vmaxps    zmm5, zmm3, zmm4 {sae}
        vrsqrt14ps zmm4, zmm1
        vminps    zmm26, zmm23, zmm24 {sae}
        vsubps    zmm27, zmm1, zmm25 {rn-sae}
        vmovaps   zmm28, zmm0
        vfmsub213ps zmm28, zmm0, zmm23 {rn-sae}
        vmulps    zmm23, zmm1, zmm4 {rn-sae}
        vsubps    zmm30, zmm26, zmm27 {rn-sae}
        vfnmadd231ps zmm1, zmm23, zmm23 {rn-sae}
        mov       QWORD PTR [1384+rsp], r13
        lea       r13, QWORD PTR [1183+rsp]
        vmovups   zmm3, ZMMWORD PTR [__svml_shypot_ha_data_internal+192]
        and       r13, -64
        vandps    zmm22, zmm5, ZMMWORD PTR [__svml_shypot_ha_data_internal+128]
        vmovaps   zmm29, zmm2
        vfmsub213ps zmm29, zmm2, zmm24 {rn-sae}
        vpcmpd    k0, zmm22, ZMMWORD PTR [__svml_shypot_ha_data_internal+512], 5
        vmulps    zmm22, zmm3, zmm4 {rn-sae}
        vaddps    zmm31, zmm28, zmm29 {rn-sae}
        kmovw     r8d, k0
        vaddps    zmm5, zmm30, zmm31 {rn-sae}
        vaddps    zmm1, zmm1, zmm5 {rn-sae}
        mov       QWORD PTR [1392+rsp], r13
        vfmadd213ps zmm1, zmm22, zmm23 {rn-sae}
        test      r8d, r8d
        jne       _B5_3

_B5_2::

        vmovaps   zmm0, zmm1
        mov       r13, QWORD PTR [1384+rsp]
        add       rsp, 1400
        ret

_B5_3::

        vstmxcsr  DWORD PTR [1376+rsp]

_B5_4::

        movzx     edx, WORD PTR [1376+rsp]
        mov       eax, edx
        or        eax, 8064
        cmp       edx, eax
        je        _B5_6

_B5_5::

        mov       DWORD PTR [1376+rsp], eax
        vldmxcsr  DWORD PTR [1376+rsp]

_B5_6::

        vmovups   ZMMWORD PTR [r13], zmm0
        vmovups   ZMMWORD PTR [64+r13], zmm2
        vmovups   ZMMWORD PTR [128+r13], zmm1
        test      r8d, r8d
        jne       _B5_11

_B5_7::

        cmp       edx, eax
        je        _B5_2

_B5_8::

        vstmxcsr  DWORD PTR [1376+rsp]
        mov       eax, DWORD PTR [1376+rsp]

_B5_9::

        and       eax, -8065
        or        eax, edx
        mov       DWORD PTR [1376+rsp], eax
        vldmxcsr  DWORD PTR [1376+rsp]
        jmp       _B5_2

_B5_11::

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

_B5_12::

        bt        edi, ebx
        jc        _B5_15

_B5_13::

        inc       ebx
        cmp       ebx, 16
        jl        _B5_12

_B5_14::

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
        vmovups   zmm1, ZMMWORD PTR [128+r13]
        mov       rbx, QWORD PTR [1104+rsp]
        mov       edx, esi
        mov       rsi, QWORD PTR [1096+rsp]
        mov       rdi, QWORD PTR [1088+rsp]
        mov       rbp, QWORD PTR [1112+rsp]
        jmp       _B5_7

_B5_15::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]
        lea       r8, QWORD PTR [128+r13+rbx*4]

        call      __svml_shypot_ha_cout_rare_internal
        jmp       _B5_13
        ALIGN     16

_B5_16::

__svml_hypotf16_ha_z0 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypotf16_ha_z0_B1_B9:
	DD	291585
	DD	11392115
	DD	11469067

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_hypotf16_ha_z0_B1_B9

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypotf16_ha_z0_B11_B15:
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
	DD	11392000
	DD	11469056

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_11
	DD	imagerel _B5_16
	DD	imagerel _unwind___svml_hypotf16_ha_z0_B11_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_hypotf4_ha_e9

__svml_hypotf4_ha_e9	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L138::

        sub       rsp, 360
        vmovaps   xmm3, xmm1
        vmovups   XMMWORD PTR [320+rsp], xmm15
        vmovups   XMMWORD PTR [256+rsp], xmm11
        vmovups   XMMWORD PTR [288+rsp], xmm10
        vmovups   XMMWORD PTR [304+rsp], xmm8
        vmovups   XMMWORD PTR [272+rsp], xmm6
        mov       QWORD PTR [336+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovups   xmm2, XMMWORD PTR [__svml_shypot_ha_data_internal+128]
        and       r13, -64
        vandps    xmm6, xmm0, xmm2
        vandps    xmm4, xmm3, xmm2
        vmaxps    xmm8, xmm6, xmm4
        vminps    xmm4, xmm4, xmm6
        vpsubd    xmm2, xmm8, xmm4
        vmovups   xmm5, XMMWORD PTR [__svml_shypot_ha_data_internal]
        vpcmpgtd  xmm11, xmm2, XMMWORD PTR [__svml_shypot_ha_data_internal+320]
        vandps    xmm6, xmm8, xmm5
        vandnps   xmm5, xmm11, xmm5
        vsubps    xmm10, xmm8, xmm6
        vandps    xmm15, xmm4, xmm5
        vmulps    xmm5, xmm6, xmm6
        vsubps    xmm11, xmm4, xmm15
        vaddps    xmm6, xmm8, xmm6
        vmulps    xmm2, xmm15, xmm15
        vmulps    xmm4, xmm4, xmm11
        vmulps    xmm10, xmm10, xmm6
        vaddps    xmm2, xmm5, xmm2
        vmulps    xmm15, xmm15, xmm11
        vaddps    xmm8, xmm10, xmm4
        vmovups   xmm4, XMMWORD PTR [__svml_shypot_ha_data_internal+384]
        vaddps    xmm10, xmm8, xmm15
        vmovups   xmm1, XMMWORD PTR [__svml_shypot_ha_data_internal+192]
        vaddps    xmm11, xmm2, xmm10
        vrsqrtps  xmm15, xmm11
        vpcmpgtd  xmm5, xmm11, XMMWORD PTR [__svml_shypot_ha_data_internal+448]
        vpcmpgtd  xmm6, xmm4, xmm11
        vpor      xmm8, xmm6, xmm5
        vmovmskps edx, xmm8
        vmulps    xmm8, xmm15, xmm11
        vmulps    xmm6, xmm15, xmm1
        vmulps    xmm4, xmm8, xmm6
        mov       QWORD PTR [344+rsp], r13
        vsubps    xmm4, xmm1, xmm4
        vmulps    xmm5, xmm8, xmm4
        vmulps    xmm1, xmm6, xmm4
        vaddps    xmm4, xmm8, xmm5
        vaddps    xmm6, xmm6, xmm1
        vandps    xmm8, xmm4, XMMWORD PTR [__svml_shypot_ha_data_internal+64]
        vmulps    xmm1, xmm8, xmm8
        vsubps    xmm5, xmm4, xmm8
        vmulps    xmm11, xmm8, xmm5
        vsubps    xmm2, xmm1, xmm2
        vmulps    xmm1, xmm4, xmm5
        vaddps    xmm15, xmm2, xmm11
        vaddps    xmm2, xmm15, xmm1
        vsubps    xmm10, xmm2, xmm10
        vmulps    xmm6, xmm6, xmm10
        vsubps    xmm1, xmm4, xmm6
        test      edx, edx
        jne       _B6_3

_B6_2::

        vmovups   xmm6, XMMWORD PTR [272+rsp]
        vmovaps   xmm0, xmm1
        vmovups   xmm8, XMMWORD PTR [304+rsp]
        vmovups   xmm10, XMMWORD PTR [288+rsp]
        vmovups   xmm11, XMMWORD PTR [256+rsp]
        vmovups   xmm15, XMMWORD PTR [320+rsp]
        mov       r13, QWORD PTR [336+rsp]
        add       rsp, 360
        ret

_B6_3::

        vmovups   XMMWORD PTR [r13], xmm0
        vmovups   XMMWORD PTR [64+r13], xmm3
        vmovups   XMMWORD PTR [128+r13], xmm1

_B6_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B6_7::

        bt        esi, ebx
        jc        _B6_10

_B6_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B6_7

_B6_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovups   xmm1, XMMWORD PTR [128+r13]
        jmp       _B6_2

_B6_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]
        lea       r8, QWORD PTR [128+r13+rbx*4]

        call      __svml_shypot_ha_cout_rare_internal
        jmp       _B6_8
        ALIGN     16

_B6_11::

__svml_hypotf4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypotf4_ha_e9_B1_B3:
	DD	934913
	DD	2806852
	DD	1140796
	DD	1280051
	DD	1222698
	DD	1095713
	DD	1374232
	DD	2949387

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_hypotf4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypotf4_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_hypotf4_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_6
	DD	imagerel _B6_11
	DD	imagerel _unwind___svml_hypotf4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_shypot_ha_cout_rare_internal

__svml_shypot_ha_cout_rare_internal	PROC

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L155::

        push      rbx
        sub       rsp, 128
        mov       r9, r8
        movzx     eax, WORD PTR [2+rcx]
        lea       r8, QWORD PTR [__ImageBase]
        and       eax, 32640
        movups    XMMWORD PTR [32+rsp], xmm15
        movups    XMMWORD PTR [48+rsp], xmm14
        cmp       eax, 32640
        je        _B7_14

_B7_2::

        movzx     eax, WORD PTR [2+rdx]
        and       eax, 32640
        cmp       eax, 32640
        je        _B7_13

_B7_3::

        pxor      xmm1, xmm1
        pxor      xmm0, xmm0
        cvtss2sd  xmm1, DWORD PTR [rcx]
        cvtss2sd  xmm0, DWORD PTR [rdx]
        movsd     xmm4, QWORD PTR [_vmlsHypotHATab+4096]
        ucomisd   xmm1, xmm4
        jp        _B7_4
        je        _B7_11

_B7_4::

        movsd     QWORD PTR [120+rsp], xmm1
        movsd     QWORD PTR [88+rsp], xmm0
        and       BYTE PTR [127+rsp], 127
        and       BYTE PTR [95+rsp], 127
        movsd     xmm0, QWORD PTR [120+rsp]
        movsd     xmm3, QWORD PTR [88+rsp]
        comisd    xmm3, xmm0
        jbe       _B7_6

_B7_5::

        movaps    xmm1, xmm0
        movaps    xmm0, xmm3
        movsd     QWORD PTR [120+rsp], xmm3
        movaps    xmm3, xmm1

_B7_6::

        movzx     edx, WORD PTR [126+rsp]
        and       edx, 32752
        shr       edx, 4
        neg       edx
        movzx     r10d, WORD PTR [_vmlsHypotHATab+4102]
        and       r10d, -32753
        movsd     QWORD PTR [80+rsp], xmm4
        movsd     xmm2, QWORD PTR [_vmlsHypotHATab+4128]
        lea       ebx, DWORD PTR [1025+rdx]
        neg       ebx
        add       ebx, 1000
        shr       ebx, 31
        imul      eax, ebx, -23
        lea       ebx, DWORD PTR [1025+rax+rdx]
        lea       ecx, DWORD PTR [1023+rbx]
        and       ecx, 2047
        shl       ecx, 4
        or        r10d, ecx
        mov       WORD PTR [86+rsp], r10w
        movsd     xmm1, QWORD PTR [80+rsp]
        mulsd     xmm0, xmm1
        mulsd     xmm3, xmm1
        mulsd     xmm2, xmm0
        movsd     QWORD PTR [64+rsp], xmm2
        movsd     xmm5, QWORD PTR [64+rsp]
        movsd     QWORD PTR [120+rsp], xmm0
        subsd     xmm5, xmm0
        movsd     QWORD PTR [72+rsp], xmm5
        movsd     xmm15, QWORD PTR [64+rsp]
        movsd     xmm14, QWORD PTR [72+rsp]
        movsd     QWORD PTR [88+rsp], xmm3
        subsd     xmm15, xmm14
        movsd     QWORD PTR [64+rsp], xmm15
        movsd     xmm1, QWORD PTR [64+rsp]
        movzx     eax, WORD PTR [126+rsp]
        subsd     xmm0, xmm1
        movzx     r11d, WORD PTR [94+rsp]
        and       eax, 32752
        and       r11d, 32752
        shr       eax, 4
        shr       r11d, 4
        movsd     QWORD PTR [72+rsp], xmm0
        sub       eax, r11d
        movsd     xmm2, QWORD PTR [64+rsp]
        movsd     xmm0, QWORD PTR [72+rsp]
        cmp       eax, 6
        jle       _B7_8

_B7_7::

        movaps    xmm14, xmm3
        jmp       _B7_9

_B7_8::

        movsd     xmm3, QWORD PTR [88+rsp]
        movsd     xmm1, QWORD PTR [_vmlsHypotHATab+4128]
        mulsd     xmm1, xmm3
        movsd     QWORD PTR [64+rsp], xmm1
        movaps    xmm1, xmm3
        movsd     xmm4, QWORD PTR [64+rsp]
        subsd     xmm4, QWORD PTR [88+rsp]
        movsd     QWORD PTR [72+rsp], xmm4
        movsd     xmm14, QWORD PTR [64+rsp]
        movsd     xmm5, QWORD PTR [72+rsp]
        subsd     xmm14, xmm5
        movsd     QWORD PTR [64+rsp], xmm14
        movsd     xmm15, QWORD PTR [64+rsp]
        subsd     xmm1, xmm15
        movsd     QWORD PTR [72+rsp], xmm1
        movsd     xmm4, QWORD PTR [64+rsp]
        movsd     xmm14, QWORD PTR [72+rsp]

_B7_9::

        movsd     xmm5, QWORD PTR [120+rsp]
        movaps    xmm1, xmm2
        mulsd     xmm3, xmm14
        addsd     xmm5, xmm2
        mulsd     xmm1, xmm2
        mulsd     xmm0, xmm5
        movaps    xmm15, xmm4
        neg       ebx
        mulsd     xmm15, xmm4
        addsd     xmm0, xmm3
        mulsd     xmm4, xmm14
        addsd     xmm1, xmm15
        addsd     xmm0, xmm4
        movaps    xmm14, xmm1
        add       ebx, 1023
        mov       rax, QWORD PTR [_vmlsHypotHATab+4112]
        mov       r11, rax
        shr       r11, 48
        and       ebx, 2047
        shl       ebx, 4
        addsd     xmm14, xmm0
        movsd     QWORD PTR [80+rsp], xmm14
        and       r11d, -32753
        movzx     ecx, WORD PTR [86+rsp]
        and       ecx, 32752
        shr       ecx, 4
        add       ecx, -1023
        mov       edx, ecx
        and       edx, 1
        sub       ecx, edx
        shr       ecx, 1
        movsd     QWORD PTR [88+rsp], xmm14
        movzx     r10d, WORD PTR [94+rsp]
        and       r10d, -32753
        add       r10d, 16368
        mov       WORD PTR [94+rsp], r10w
        lea       r10d, DWORD PTR [1023+rcx]
        add       ecx, ecx
        and       r10d, 2047
        neg       ecx
        add       ecx, 1023
        and       ecx, 2047
        shl       r10d, 4
        shl       ecx, 4
        or        r10d, r11d
        movsd     xmm5, QWORD PTR [88+rsp]
        or        ecx, r11d
        or        r11d, ebx
        mov       ebx, DWORD PTR [92+rsp]
        mulsd     xmm5, QWORD PTR [imagerel(_vmlsHypotHATab)+4112+r8+rdx*8]
        and       ebx, 1048575
        shr       ebx, 12
        shl       edx, 8
        add       ebx, edx
        movsd     xmm2, QWORD PTR [imagerel(_vmlsHypotHATab)+r8+rbx*8]
        movsd     xmm14, QWORD PTR [_vmlsHypotHATab+4104]
        mulsd     xmm5, xmm2
        mulsd     xmm14, xmm2
        movaps    xmm3, xmm5
        mulsd     xmm3, xmm14
        movsd     xmm2, QWORD PTR [_vmlsHypotHATab+4104]
        mov       QWORD PTR [104+rsp], rax
        subsd     xmm2, xmm3
        movaps    xmm4, xmm2
        mulsd     xmm2, xmm5
        mulsd     xmm4, xmm14
        addsd     xmm5, xmm2
        addsd     xmm14, xmm4
        movaps    xmm3, xmm5
        movaps    xmm15, xmm14
        mulsd     xmm3, xmm14
        movsd     xmm2, QWORD PTR [_vmlsHypotHATab+4104]
        mov       WORD PTR [110+rsp], cx
        subsd     xmm2, xmm3
        mulsd     xmm15, xmm2
        mulsd     xmm2, xmm5
        addsd     xmm14, xmm15
        addsd     xmm5, xmm2
        movaps    xmm3, xmm5
        movaps    xmm4, xmm14
        mulsd     xmm3, xmm14
        movsd     xmm2, QWORD PTR [_vmlsHypotHATab+4104]
        mov       QWORD PTR [96+rsp], rax
        subsd     xmm2, xmm3
        mulsd     xmm4, xmm2
        mulsd     xmm2, xmm5
        addsd     xmm14, xmm4
        addsd     xmm5, xmm2
        movsd     xmm3, QWORD PTR [_vmlsHypotHATab+4128]
        mulsd     xmm3, xmm5
        movsd     QWORD PTR [64+rsp], xmm3
        movsd     xmm15, QWORD PTR [64+rsp]
        mov       WORD PTR [102+rsp], r10w
        subsd     xmm15, xmm5
        movsd     QWORD PTR [72+rsp], xmm15
        movsd     xmm3, QWORD PTR [64+rsp]
        movsd     xmm2, QWORD PTR [72+rsp]
        mov       QWORD PTR [112+rsp], rax
        subsd     xmm3, xmm2
        movsd     QWORD PTR [64+rsp], xmm3
        movaps    xmm2, xmm5
        movsd     xmm4, QWORD PTR [64+rsp]
        mov       WORD PTR [118+rsp], r11w
        subsd     xmm2, xmm4
        movsd     QWORD PTR [72+rsp], xmm2
        movsd     xmm2, QWORD PTR [64+rsp]
        movsd     xmm4, QWORD PTR [104+rsp]
        movaps    xmm15, xmm2
        mulsd     xmm1, xmm4
        mulsd     xmm15, xmm2
        mulsd     xmm0, xmm4
        subsd     xmm1, xmm15
        movsd     xmm15, QWORD PTR [_vmlsHypotHATab+4120]
        mulsd     xmm15, xmm2
        movsd     xmm3, QWORD PTR [72+rsp]
        mulsd     xmm15, xmm3
        mulsd     xmm3, xmm3
        subsd     xmm1, xmm15
        subsd     xmm1, xmm3
        addsd     xmm1, xmm0
        mulsd     xmm14, xmm1
        addsd     xmm5, xmm14
        mulsd     xmm5, QWORD PTR [96+rsp]
        mulsd     xmm5, QWORD PTR [112+rsp]
        cvtsd2ss  xmm5, xmm5
        movss     DWORD PTR [r9], xmm5

_B7_10::

        movups    xmm14, XMMWORD PTR [48+rsp]
        xor       eax, eax
        movups    xmm15, XMMWORD PTR [32+rsp]
        add       rsp, 128
        pop       rbx
        ret

_B7_11::

        ucomisd   xmm0, xmm4
        jne       _B7_4
        jp        _B7_4

_B7_12::

        cvtsd2ss  xmm4, xmm4
        movss     DWORD PTR [r9], xmm4
        jmp       _B7_10

_B7_13::

        movss     xmm0, DWORD PTR [rdx]
        mulss     xmm0, xmm0
        movss     DWORD PTR [r9], xmm0
        jmp       _B7_10

_B7_14::

        movzx     eax, WORD PTR [2+rdx]
        and       eax, 32640
        cmp       eax, 32640
        je        _B7_16

_B7_15::

        movss     xmm0, DWORD PTR [rcx]
        mulss     xmm0, xmm0
        movss     DWORD PTR [r9], xmm0
        jmp       _B7_10

_B7_16::

        test      DWORD PTR [rcx], 8388607
        je        _B7_15

_B7_17::

        test      DWORD PTR [rdx], 8388607
        je        _B7_13

_B7_18::

        movss     xmm0, DWORD PTR [rcx]
        mulss     xmm0, DWORD PTR [rdx]
        movss     DWORD PTR [r9], xmm0
        jmp       _B7_10
        ALIGN     16

_B7_21::

__svml_shypot_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_shypot_ha_cout_rare_internal_B1_B18:
	DD	404225
	DD	256043
	DD	194597
	DD	805696012

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_21
	DD	imagerel _unwind___svml_shypot_ha_cout_rare_internal_B1_B18

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_shypot_ha_data_internal
__svml_shypot_ha_data_internal	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294443008
	DD	4294959104
	DD	4294959104
	DD	4294959104
	DD	4294959104
	DD	4294959104
	DD	4294959104
	DD	4294959104
	DD	4294959104
	DD	4294959104
	DD	4294959104
	DD	4294959104
	DD	4294959104
	DD	4294959104
	DD	4294959104
	DD	4294959104
	DD	4294959104
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
	DD	33554432
	DD	33554432
	DD	33554432
	DD	33554432
	DD	33554432
	DD	33554432
	DD	33554432
	DD	33554432
	DD	33554432
	DD	33554432
	DD	33554432
	DD	33554432
	DD	33554432
	DD	33554432
	DD	33554432
	DD	33554432
	DD	506462208
	DD	506462208
	DD	506462208
	DD	506462208
	DD	506462208
	DD	506462208
	DD	506462208
	DD	506462208
	DD	506462208
	DD	506462208
	DD	506462208
	DD	506462208
	DD	506462208
	DD	506462208
	DD	506462208
	DD	506462208
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1621098496
	DD	1115422720
	DD	1115422720
	DD	1115422720
	DD	1115422720
	DD	1115422720
	DD	1115422720
	DD	1115422720
	DD	1115422720
	DD	1115422720
	DD	1115422720
	DD	1115422720
	DD	1115422720
	DD	1115422720
	DD	1115422720
	DD	1115422720
	DD	1115422720
_vmlsHypotHATab	DD	0
	DD	1072693248
	DD	0
	DD	1072689152
	DD	0
	DD	1072685056
	DD	0
	DD	1072680960
	DD	0
	DD	1072676864
	DD	0
	DD	1072672768
	DD	0
	DD	1072668672
	DD	0
	DD	1072665600
	DD	0
	DD	1072661504
	DD	0
	DD	1072657408
	DD	0
	DD	1072653312
	DD	0
	DD	1072649216
	DD	0
	DD	1072646144
	DD	0
	DD	1072642048
	DD	0
	DD	1072637952
	DD	0
	DD	1072634880
	DD	0
	DD	1072630784
	DD	0
	DD	1072626688
	DD	0
	DD	1072623616
	DD	0
	DD	1072619520
	DD	0
	DD	1072615424
	DD	0
	DD	1072612352
	DD	0
	DD	1072608256
	DD	0
	DD	1072605184
	DD	0
	DD	1072601088
	DD	0
	DD	1072598016
	DD	0
	DD	1072593920
	DD	0
	DD	1072590848
	DD	0
	DD	1072586752
	DD	0
	DD	1072583680
	DD	0
	DD	1072580608
	DD	0
	DD	1072576512
	DD	0
	DD	1072573440
	DD	0
	DD	1072570368
	DD	0
	DD	1072566272
	DD	0
	DD	1072563200
	DD	0
	DD	1072560128
	DD	0
	DD	1072556032
	DD	0
	DD	1072552960
	DD	0
	DD	1072549888
	DD	0
	DD	1072546816
	DD	0
	DD	1072542720
	DD	0
	DD	1072539648
	DD	0
	DD	1072536576
	DD	0
	DD	1072533504
	DD	0
	DD	1072530432
	DD	0
	DD	1072527360
	DD	0
	DD	1072523264
	DD	0
	DD	1072520192
	DD	0
	DD	1072517120
	DD	0
	DD	1072514048
	DD	0
	DD	1072510976
	DD	0
	DD	1072507904
	DD	0
	DD	1072504832
	DD	0
	DD	1072501760
	DD	0
	DD	1072498688
	DD	0
	DD	1072495616
	DD	0
	DD	1072492544
	DD	0
	DD	1072489472
	DD	0
	DD	1072486400
	DD	0
	DD	1072483328
	DD	0
	DD	1072480256
	DD	0
	DD	1072478208
	DD	0
	DD	1072475136
	DD	0
	DD	1072472064
	DD	0
	DD	1072468992
	DD	0
	DD	1072465920
	DD	0
	DD	1072462848
	DD	0
	DD	1072459776
	DD	0
	DD	1072457728
	DD	0
	DD	1072454656
	DD	0
	DD	1072451584
	DD	0
	DD	1072448512
	DD	0
	DD	1072446464
	DD	0
	DD	1072443392
	DD	0
	DD	1072440320
	DD	0
	DD	1072437248
	DD	0
	DD	1072435200
	DD	0
	DD	1072432128
	DD	0
	DD	1072429056
	DD	0
	DD	1072427008
	DD	0
	DD	1072423936
	DD	0
	DD	1072420864
	DD	0
	DD	1072418816
	DD	0
	DD	1072415744
	DD	0
	DD	1072412672
	DD	0
	DD	1072410624
	DD	0
	DD	1072407552
	DD	0
	DD	1072405504
	DD	0
	DD	1072402432
	DD	0
	DD	1072400384
	DD	0
	DD	1072397312
	DD	0
	DD	1072395264
	DD	0
	DD	1072392192
	DD	0
	DD	1072390144
	DD	0
	DD	1072387072
	DD	0
	DD	1072385024
	DD	0
	DD	1072381952
	DD	0
	DD	1072379904
	DD	0
	DD	1072376832
	DD	0
	DD	1072374784
	DD	0
	DD	1072371712
	DD	0
	DD	1072369664
	DD	0
	DD	1072366592
	DD	0
	DD	1072364544
	DD	0
	DD	1072362496
	DD	0
	DD	1072359424
	DD	0
	DD	1072357376
	DD	0
	DD	1072355328
	DD	0
	DD	1072352256
	DD	0
	DD	1072350208
	DD	0
	DD	1072347136
	DD	0
	DD	1072345088
	DD	0
	DD	1072343040
	DD	0
	DD	1072340992
	DD	0
	DD	1072337920
	DD	0
	DD	1072335872
	DD	0
	DD	1072333824
	DD	0
	DD	1072330752
	DD	0
	DD	1072328704
	DD	0
	DD	1072326656
	DD	0
	DD	1072324608
	DD	0
	DD	1072321536
	DD	0
	DD	1072319488
	DD	0
	DD	1072317440
	DD	0
	DD	1072315392
	DD	0
	DD	1072313344
	DD	0
	DD	1072310272
	DD	0
	DD	1072308224
	DD	0
	DD	1072306176
	DD	0
	DD	1072304128
	DD	0
	DD	1072302080
	DD	0
	DD	1072300032
	DD	0
	DD	1072296960
	DD	0
	DD	1072294912
	DD	0
	DD	1072292864
	DD	0
	DD	1072290816
	DD	0
	DD	1072288768
	DD	0
	DD	1072286720
	DD	0
	DD	1072284672
	DD	0
	DD	1072282624
	DD	0
	DD	1072280576
	DD	0
	DD	1072278528
	DD	0
	DD	1072275456
	DD	0
	DD	1072273408
	DD	0
	DD	1072271360
	DD	0
	DD	1072269312
	DD	0
	DD	1072267264
	DD	0
	DD	1072265216
	DD	0
	DD	1072263168
	DD	0
	DD	1072261120
	DD	0
	DD	1072259072
	DD	0
	DD	1072257024
	DD	0
	DD	1072254976
	DD	0
	DD	1072252928
	DD	0
	DD	1072250880
	DD	0
	DD	1072248832
	DD	0
	DD	1072246784
	DD	0
	DD	1072244736
	DD	0
	DD	1072243712
	DD	0
	DD	1072241664
	DD	0
	DD	1072239616
	DD	0
	DD	1072237568
	DD	0
	DD	1072235520
	DD	0
	DD	1072233472
	DD	0
	DD	1072231424
	DD	0
	DD	1072229376
	DD	0
	DD	1072227328
	DD	0
	DD	1072225280
	DD	0
	DD	1072223232
	DD	0
	DD	1072222208
	DD	0
	DD	1072220160
	DD	0
	DD	1072218112
	DD	0
	DD	1072216064
	DD	0
	DD	1072214016
	DD	0
	DD	1072211968
	DD	0
	DD	1072210944
	DD	0
	DD	1072208896
	DD	0
	DD	1072206848
	DD	0
	DD	1072204800
	DD	0
	DD	1072202752
	DD	0
	DD	1072201728
	DD	0
	DD	1072199680
	DD	0
	DD	1072197632
	DD	0
	DD	1072195584
	DD	0
	DD	1072193536
	DD	0
	DD	1072192512
	DD	0
	DD	1072190464
	DD	0
	DD	1072188416
	DD	0
	DD	1072186368
	DD	0
	DD	1072185344
	DD	0
	DD	1072183296
	DD	0
	DD	1072181248
	DD	0
	DD	1072179200
	DD	0
	DD	1072178176
	DD	0
	DD	1072176128
	DD	0
	DD	1072174080
	DD	0
	DD	1072173056
	DD	0
	DD	1072171008
	DD	0
	DD	1072168960
	DD	0
	DD	1072167936
	DD	0
	DD	1072165888
	DD	0
	DD	1072163840
	DD	0
	DD	1072161792
	DD	0
	DD	1072160768
	DD	0
	DD	1072158720
	DD	0
	DD	1072157696
	DD	0
	DD	1072155648
	DD	0
	DD	1072153600
	DD	0
	DD	1072152576
	DD	0
	DD	1072150528
	DD	0
	DD	1072148480
	DD	0
	DD	1072147456
	DD	0
	DD	1072145408
	DD	0
	DD	1072143360
	DD	0
	DD	1072142336
	DD	0
	DD	1072140288
	DD	0
	DD	1072139264
	DD	0
	DD	1072137216
	DD	0
	DD	1072135168
	DD	0
	DD	1072134144
	DD	0
	DD	1072132096
	DD	0
	DD	1072131072
	DD	0
	DD	1072129024
	DD	0
	DD	1072128000
	DD	0
	DD	1072125952
	DD	0
	DD	1072124928
	DD	0
	DD	1072122880
	DD	0
	DD	1072120832
	DD	0
	DD	1072119808
	DD	0
	DD	1072117760
	DD	0
	DD	1072116736
	DD	0
	DD	1072114688
	DD	0
	DD	1072113664
	DD	0
	DD	1072111616
	DD	0
	DD	1072110592
	DD	0
	DD	1072108544
	DD	0
	DD	1072107520
	DD	0
	DD	1072105472
	DD	0
	DD	1072104448
	DD	0
	DD	1072102400
	DD	0
	DD	1072101376
	DD	0
	DD	1072099328
	DD	0
	DD	1072098304
	DD	0
	DD	1072096256
	DD	0
	DD	1072095232
	DD	0
	DD	1072094208
	DD	0
	DD	1072092160
	DD	0
	DD	1072091136
	DD	0
	DD	1072089088
	DD	0
	DD	1072088064
	DD	0
	DD	1072086016
	DD	0
	DD	1072084992
	DD	0
	DD	1072082944
	DD	0
	DD	1072081920
	DD	0
	DD	1072080896
	DD	0
	DD	1072078848
	DD	0
	DD	1072075776
	DD	0
	DD	1072073728
	DD	0
	DD	1072070656
	DD	0
	DD	1072067584
	DD	0
	DD	1072064512
	DD	0
	DD	1072061440
	DD	0
	DD	1072059392
	DD	0
	DD	1072056320
	DD	0
	DD	1072053248
	DD	0
	DD	1072051200
	DD	0
	DD	1072048128
	DD	0
	DD	1072045056
	DD	0
	DD	1072043008
	DD	0
	DD	1072039936
	DD	0
	DD	1072037888
	DD	0
	DD	1072034816
	DD	0
	DD	1072031744
	DD	0
	DD	1072029696
	DD	0
	DD	1072026624
	DD	0
	DD	1072024576
	DD	0
	DD	1072021504
	DD	0
	DD	1072019456
	DD	0
	DD	1072016384
	DD	0
	DD	1072014336
	DD	0
	DD	1072011264
	DD	0
	DD	1072009216
	DD	0
	DD	1072006144
	DD	0
	DD	1072004096
	DD	0
	DD	1072002048
	DD	0
	DD	1071998976
	DD	0
	DD	1071996928
	DD	0
	DD	1071993856
	DD	0
	DD	1071991808
	DD	0
	DD	1071989760
	DD	0
	DD	1071986688
	DD	0
	DD	1071984640
	DD	0
	DD	1071982592
	DD	0
	DD	1071979520
	DD	0
	DD	1071977472
	DD	0
	DD	1071975424
	DD	0
	DD	1071972352
	DD	0
	DD	1071970304
	DD	0
	DD	1071968256
	DD	0
	DD	1071966208
	DD	0
	DD	1071964160
	DD	0
	DD	1071961088
	DD	0
	DD	1071959040
	DD	0
	DD	1071956992
	DD	0
	DD	1071954944
	DD	0
	DD	1071952896
	DD	0
	DD	1071949824
	DD	0
	DD	1071947776
	DD	0
	DD	1071945728
	DD	0
	DD	1071943680
	DD	0
	DD	1071941632
	DD	0
	DD	1071939584
	DD	0
	DD	1071937536
	DD	0
	DD	1071935488
	DD	0
	DD	1071933440
	DD	0
	DD	1071930368
	DD	0
	DD	1071928320
	DD	0
	DD	1071926272
	DD	0
	DD	1071924224
	DD	0
	DD	1071922176
	DD	0
	DD	1071920128
	DD	0
	DD	1071918080
	DD	0
	DD	1071916032
	DD	0
	DD	1071913984
	DD	0
	DD	1071911936
	DD	0
	DD	1071909888
	DD	0
	DD	1071907840
	DD	0
	DD	1071905792
	DD	0
	DD	1071903744
	DD	0
	DD	1071901696
	DD	0
	DD	1071900672
	DD	0
	DD	1071898624
	DD	0
	DD	1071896576
	DD	0
	DD	1071894528
	DD	0
	DD	1071892480
	DD	0
	DD	1071890432
	DD	0
	DD	1071888384
	DD	0
	DD	1071886336
	DD	0
	DD	1071884288
	DD	0
	DD	1071883264
	DD	0
	DD	1071881216
	DD	0
	DD	1071879168
	DD	0
	DD	1071877120
	DD	0
	DD	1071875072
	DD	0
	DD	1071873024
	DD	0
	DD	1071872000
	DD	0
	DD	1071869952
	DD	0
	DD	1071867904
	DD	0
	DD	1071865856
	DD	0
	DD	1071864832
	DD	0
	DD	1071862784
	DD	0
	DD	1071860736
	DD	0
	DD	1071858688
	DD	0
	DD	1071856640
	DD	0
	DD	1071855616
	DD	0
	DD	1071853568
	DD	0
	DD	1071851520
	DD	0
	DD	1071850496
	DD	0
	DD	1071848448
	DD	0
	DD	1071846400
	DD	0
	DD	1071844352
	DD	0
	DD	1071843328
	DD	0
	DD	1071841280
	DD	0
	DD	1071839232
	DD	0
	DD	1071838208
	DD	0
	DD	1071836160
	DD	0
	DD	1071834112
	DD	0
	DD	1071833088
	DD	0
	DD	1071831040
	DD	0
	DD	1071830016
	DD	0
	DD	1071827968
	DD	0
	DD	1071825920
	DD	0
	DD	1071824896
	DD	0
	DD	1071822848
	DD	0
	DD	1071821824
	DD	0
	DD	1071819776
	DD	0
	DD	1071817728
	DD	0
	DD	1071816704
	DD	0
	DD	1071814656
	DD	0
	DD	1071813632
	DD	0
	DD	1071811584
	DD	0
	DD	1071810560
	DD	0
	DD	1071808512
	DD	0
	DD	1071806464
	DD	0
	DD	1071805440
	DD	0
	DD	1071803392
	DD	0
	DD	1071802368
	DD	0
	DD	1071800320
	DD	0
	DD	1071799296
	DD	0
	DD	1071797248
	DD	0
	DD	1071796224
	DD	0
	DD	1071794176
	DD	0
	DD	1071793152
	DD	0
	DD	1071791104
	DD	0
	DD	1071790080
	DD	0
	DD	1071788032
	DD	0
	DD	1071787008
	DD	0
	DD	1071784960
	DD	0
	DD	1071783936
	DD	0
	DD	1071782912
	DD	0
	DD	1071780864
	DD	0
	DD	1071779840
	DD	0
	DD	1071777792
	DD	0
	DD	1071776768
	DD	0
	DD	1071774720
	DD	0
	DD	1071773696
	DD	0
	DD	1071772672
	DD	0
	DD	1071770624
	DD	0
	DD	1071769600
	DD	0
	DD	1071767552
	DD	0
	DD	1071766528
	DD	0
	DD	1071765504
	DD	0
	DD	1071763456
	DD	0
	DD	1071762432
	DD	0
	DD	1071760384
	DD	0
	DD	1071759360
	DD	0
	DD	1071758336
	DD	0
	DD	1071756288
	DD	0
	DD	1071755264
	DD	0
	DD	1071754240
	DD	0
	DD	1071752192
	DD	0
	DD	1071751168
	DD	0
	DD	1071750144
	DD	0
	DD	1071748096
	DD	0
	DD	1071747072
	DD	0
	DD	1071746048
	DD	0
	DD	1071744000
	DD	0
	DD	1071742976
	DD	0
	DD	1071741952
	DD	0
	DD	1071739904
	DD	0
	DD	1071738880
	DD	0
	DD	1071737856
	DD	0
	DD	1071736832
	DD	0
	DD	1071734784
	DD	0
	DD	1071733760
	DD	0
	DD	1071732736
	DD	0
	DD	1071730688
	DD	0
	DD	1071729664
	DD	0
	DD	1071728640
	DD	0
	DD	1071727616
	DD	0
	DD	1071725568
	DD	0
	DD	1071724544
	DD	0
	DD	1071723520
	DD	0
	DD	1071722496
	DD	0
	DD	1071720448
	DD	0
	DD	1071719424
	DD	0
	DD	1071718400
	DD	0
	DD	1071717376
	DD	0
	DD	1071715328
	DD	0
	DD	1071714304
	DD	0
	DD	1071713280
	DD	0
	DD	1071712256
	DD	0
	DD	1071711232
	DD	0
	DD	1071709184
	DD	0
	DD	1071708160
	DD	0
	DD	1071707136
	DD	0
	DD	1071706112
	DD	0
	DD	1071705088
	DD	0
	DD	1071704064
	DD	0
	DD	1071702016
	DD	0
	DD	1071700992
	DD	0
	DD	1071699968
	DD	0
	DD	1071698944
	DD	0
	DD	1071697920
	DD	0
	DD	1071696896
	DD	0
	DD	1071694848
	DD	0
	DD	1071693824
	DD	0
	DD	1071692800
	DD	0
	DD	1071691776
	DD	0
	DD	1071690752
	DD	0
	DD	1071689728
	DD	0
	DD	1071688704
	DD	0
	DD	1071686656
	DD	0
	DD	1071685632
	DD	0
	DD	1071684608
	DD	0
	DD	1071683584
	DD	0
	DD	1071682560
	DD	0
	DD	1071681536
	DD	0
	DD	1071680512
	DD	0
	DD	1071679488
	DD	0
	DD	1071677440
	DD	0
	DD	1071676416
	DD	0
	DD	1071675392
	DD	0
	DD	1071674368
	DD	0
	DD	1071673344
	DD	0
	DD	1071672320
	DD	0
	DD	1071671296
	DD	0
	DD	1071670272
	DD	0
	DD	1071669248
	DD	0
	DD	1071668224
	DD	0
	DD	1071667200
	DD	0
	DD	1071666176
	DD	0
	DD	1071665152
	DD	0
	DD	1071663104
	DD	0
	DD	1071662080
	DD	0
	DD	1071661056
	DD	0
	DD	1071660032
	DD	0
	DD	1071659008
	DD	0
	DD	1071657984
	DD	0
	DD	1071656960
	DD	0
	DD	1071655936
	DD	0
	DD	1071654912
	DD	0
	DD	1071653888
	DD	0
	DD	1071652864
	DD	0
	DD	1071651840
	DD	0
	DD	1071650816
	DD	0
	DD	1071649792
	DD	0
	DD	1071648768
	DD	0
	DD	1071647744
	DD	0
	DD	1071646720
	DD	0
	DD	1071645696
	DD	0
	DD	0
	DD	0
	DD	1071644672
	DD	0
	DD	1072693248
	DD	0
	DD	1073741824
	DD	33554432
	DD	1101004800
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
