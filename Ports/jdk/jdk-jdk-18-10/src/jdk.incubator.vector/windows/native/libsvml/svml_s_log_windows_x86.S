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
	PUBLIC __svml_logf4_ha_ex

__svml_logf4_ha_ex	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 216
        movaps    xmm3, xmm0
        mov       QWORD PTR [192+rsp], r13
        movaps    xmm5, xmm3
        movdqu    xmm4, XMMWORD PTR [__svml_slog_ha_data_internal+576]
        lea       r13, QWORD PTR [111+rsp]
        movdqu    xmm2, XMMWORD PTR [__svml_slog_ha_data_internal+640]
        paddd     xmm4, xmm3
        pcmpgtd   xmm2, xmm4
        and       r13, -64
        movdqu    xmm4, XMMWORD PTR [__svml_slog_ha_data_internal+704]
        movdqu    xmm0, XMMWORD PTR [__svml_slog_ha_data_internal+768]
        psubd     xmm5, xmm4
        pand      xmm0, xmm5
        psrad     xmm5, 23
        paddd     xmm0, xmm4
        movmskps  eax, xmm2
        subps     xmm0, XMMWORD PTR [__svml_slog_ha_data_internal+832]
        cvtdq2ps  xmm1, xmm5
        movups    xmm2, XMMWORD PTR [__svml_slog_ha_data_internal+512]
        mulps     xmm2, xmm0
        movups    xmm4, XMMWORD PTR [__svml_slog_ha_data_internal+960]
        mulps     xmm4, xmm1
        addps     xmm2, XMMWORD PTR [__svml_slog_ha_data_internal+448]
        mulps     xmm2, xmm0
        movups    xmm5, XMMWORD PTR [__svml_slog_ha_data_internal+896]
        mulps     xmm1, xmm5
        addps     xmm2, XMMWORD PTR [__svml_slog_ha_data_internal+384]
        mulps     xmm2, xmm0
        mov       QWORD PTR [200+rsp], r13
        addps     xmm2, XMMWORD PTR [__svml_slog_ha_data_internal+320]
        mulps     xmm2, xmm0
        addps     xmm2, XMMWORD PTR [__svml_slog_ha_data_internal+256]
        mulps     xmm2, xmm0
        addps     xmm2, XMMWORD PTR [__svml_slog_ha_data_internal+192]
        mulps     xmm2, xmm0
        addps     xmm2, XMMWORD PTR [__svml_slog_ha_data_internal+128]
        mulps     xmm2, xmm0
        addps     xmm2, XMMWORD PTR [__svml_slog_ha_data_internal+64]
        mulps     xmm2, xmm0
        addps     xmm2, XMMWORD PTR [__svml_slog_ha_data_internal]
        mulps     xmm2, xmm0
        mulps     xmm2, xmm0
        addps     xmm0, xmm2
        addps     xmm0, xmm4
        addps     xmm0, xmm1
        test      eax, eax
        jne       _B1_3

_B1_2::

        mov       r13, QWORD PTR [192+rsp]
        add       rsp, 216
        ret

_B1_3::

        movups    XMMWORD PTR [r13], xmm3
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

        call      __svml_slog_ha_cout_rare_internal
        jmp       _B1_8
        ALIGN     16

_B1_11::

__svml_logf4_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_logf4_ha_ex_B1_B3:
	DD	267777
	DD	1627158
	DD	1769739

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_logf4_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_logf4_ha_ex_B6_B10:
	DD	527873
	DD	287758
	DD	340999
	DD	1627136
	DD	1769728

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_6
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_logf4_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_logf4_ha_e9

__svml_logf4_ha_e9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L8::

        sub       rsp, 216
        vmovaps   xmm5, xmm0
        mov       QWORD PTR [192+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vpaddd    xmm0, xmm5, XMMWORD PTR [__svml_slog_ha_data_internal+576]
        and       r13, -64
        vmovups   xmm1, XMMWORD PTR [__svml_slog_ha_data_internal+640]
        vmovups   xmm4, XMMWORD PTR [__svml_slog_ha_data_internal+704]
        vpcmpgtd  xmm2, xmm1, xmm0
        vpsubd    xmm1, xmm5, xmm4
        vpand     xmm3, xmm1, XMMWORD PTR [__svml_slog_ha_data_internal+768]
        vpsrad    xmm0, xmm1, 23
        vpaddd    xmm3, xmm3, xmm4
        vcvtdq2ps xmm1, xmm0
        vsubps    xmm0, xmm3, XMMWORD PTR [__svml_slog_ha_data_internal+832]
        vmovmskps edx, xmm2
        vmulps    xmm2, xmm0, XMMWORD PTR [__svml_slog_ha_data_internal+512]
        mov       QWORD PTR [200+rsp], r13
        vaddps    xmm4, xmm2, XMMWORD PTR [__svml_slog_ha_data_internal+448]
        vmulps    xmm2, xmm0, xmm4
        vaddps    xmm3, xmm2, XMMWORD PTR [__svml_slog_ha_data_internal+384]
        vmulps    xmm4, xmm0, xmm3
        vaddps    xmm2, xmm4, XMMWORD PTR [__svml_slog_ha_data_internal+320]
        vmulps    xmm3, xmm0, xmm2
        vaddps    xmm4, xmm3, XMMWORD PTR [__svml_slog_ha_data_internal+256]
        vmulps    xmm2, xmm0, xmm4
        vaddps    xmm3, xmm2, XMMWORD PTR [__svml_slog_ha_data_internal+192]
        vmulps    xmm4, xmm0, xmm3
        vaddps    xmm2, xmm4, XMMWORD PTR [__svml_slog_ha_data_internal+128]
        vmulps    xmm3, xmm0, xmm2
        vaddps    xmm4, xmm3, XMMWORD PTR [__svml_slog_ha_data_internal+64]
        vmulps    xmm2, xmm0, xmm4
        vaddps    xmm3, xmm2, XMMWORD PTR [__svml_slog_ha_data_internal]
        vmulps    xmm4, xmm0, xmm3
        vmulps    xmm2, xmm0, xmm4
        vaddps    xmm0, xmm0, xmm2
        vmulps    xmm2, xmm1, XMMWORD PTR [__svml_slog_ha_data_internal+960]
        vmulps    xmm1, xmm1, XMMWORD PTR [__svml_slog_ha_data_internal+896]
        vaddps    xmm0, xmm0, xmm2
        vaddps    xmm0, xmm0, xmm1
        test      edx, edx
        jne       _B2_3

_B2_2::

        mov       r13, QWORD PTR [192+rsp]
        add       rsp, 216
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

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_slog_ha_cout_rare_internal
        jmp       _B2_8
        ALIGN     16

_B2_11::

__svml_logf4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_logf4_ha_e9_B1_B3:
	DD	268033
	DD	1627159
	DD	1769739

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_logf4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_logf4_ha_e9_B6_B10:
	DD	527873
	DD	287758
	DD	340999
	DD	1627136
	DD	1769728

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_6
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_logf4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_logf16_ha_z0

__svml_logf16_ha_z0	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L15::

        vmovaps   zmm23, zmm0
        vrcp14ps  zmm22, zmm23
        vpaddd    zmm4, zmm22, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512]
        vmovups   zmm1, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+128]
        vmovups   zmm2, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+256]
        vmovups   zmm22, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+640]
        vmovups   zmm3, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+704]
        vpandd    zmm24, zmm4, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+64]
        vpsrld    zmm5, zmm24, 18
        vgetexpps zmm0, zmm24 {sae}
        vmovups   zmm4, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+960]
        vpermt2ps zmm1, zmm5, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+192]
        vpermt2ps zmm2, zmm5, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+320]
        vfpclassps k2, zmm24, 255
        vmovups   zmm5, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+896]
        vfmsub213ps zmm24, zmm23, zmm22 {rn-sae}
        kortestw  k2, k2
        vfnmadd231ps zmm1, zmm5, zmm0 {rn-sae}
        vfnmadd213ps zmm0, zmm4, zmm2 {rn-sae}
        vmovups   zmm2, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+768]
        vmulps    zmm26, zmm24, zmm24 {rn-sae}
        vaddps    zmm29, zmm1, zmm24 {rn-sae}
        vmovaps   zmm27, zmm24
        vfmadd213ps zmm27, zmm3, zmm2 {rn-sae}
        vsubps    zmm25, zmm29, zmm1 {rn-sae}
        vmovups   zmm1, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+832]
        vsubps    zmm28, zmm24, zmm25 {rn-sae}
        vfmadd213ps zmm27, zmm24, zmm1 {rn-sae}
        vfmadd213ps zmm27, zmm26, zmm0 {rn-sae}
        vaddps    zmm30, zmm27, zmm28 {rn-sae}
        vaddps    zmm0, zmm29, zmm30 {rn-sae}
        jne       _B3_3

_B3_2::

        ret

_B3_3::

        vgetmantps zmm25, zmm23, 10 {sae}
        vgetexpps zmm28, zmm23 {sae}
        vmovups   zmm31, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+384]
        vmovups   zmm30, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+512]
        vrcp14ps  zmm24, zmm25
        vfpclassps k1, zmm23, 223
        vrndscaleps zmm26, zmm24, 88 {sae}
        vgetexpps zmm29, zmm26 {sae}
        vfmsub231ps zmm22, zmm25, zmm26 {rn-sae}
        vpsrld    zmm27, zmm26, 18
        vsubps    zmm24, zmm28, zmm29 {rn-sae}
        vfmadd231ps zmm2, zmm3, zmm22 {rn-sae}
        vmulps    zmm3, zmm22, zmm22 {rn-sae}
        vfmadd213ps zmm2, zmm22, zmm1 {rn-sae}
        vpermt2ps zmm31, zmm27, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+448]
        vpermt2ps zmm30, zmm27, ZMMWORD PTR [__svml_slog_ha_data_internal_avx512+576]
        vfmadd231ps zmm31, zmm5, zmm24 {rn-sae}
        vfmadd213ps zmm24, zmm4, zmm30 {rn-sae}
        vaddps    zmm1, zmm31, zmm22 {rn-sae}
        vfmadd213ps zmm2, zmm3, zmm24 {rn-sae}
        vsubps    zmm4, zmm1, zmm31 {rn-sae}
        vxorps    zmm4{k1}, zmm4, zmm4
        vsubps    zmm22, zmm22, zmm4 {rn-sae}
        vaddps    zmm2, zmm2, zmm22 {rn-sae}
        vaddps    zmm0{k2}, zmm1, zmm2 {rn-sae}
        ret
        ALIGN     16

_B3_4::

__svml_logf16_ha_z0 ENDP

_TEXT	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_logf4_ha_l9

__svml_logf4_ha_l9	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L16::

        sub       rsp, 216
        vmovaps   xmm4, xmm0
        mov       QWORD PTR [192+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vpaddd    xmm0, xmm4, XMMWORD PTR [__svml_slog_ha_data_internal+576]
        and       r13, -64
        vmovups   xmm5, XMMWORD PTR [__svml_slog_ha_data_internal+640]
        vmovups   xmm3, XMMWORD PTR [__svml_slog_ha_data_internal+704]
        vpcmpgtd  xmm1, xmm5, xmm0
        vpsubd    xmm5, xmm4, xmm3
        vpand     xmm2, xmm5, XMMWORD PTR [__svml_slog_ha_data_internal+768]
        vpsrad    xmm0, xmm5, 23
        vpaddd    xmm2, xmm2, xmm3
        vmovmskps edx, xmm1
        vsubps    xmm3, xmm2, XMMWORD PTR [__svml_slog_ha_data_internal+832]
        vcvtdq2ps xmm0, xmm0
        vmovups   xmm1, XMMWORD PTR [__svml_slog_ha_data_internal+512]
        vfmadd213ps xmm1, xmm3, XMMWORD PTR [__svml_slog_ha_data_internal+448]
        vfmadd213ps xmm1, xmm3, XMMWORD PTR [__svml_slog_ha_data_internal+384]
        vfmadd213ps xmm1, xmm3, XMMWORD PTR [__svml_slog_ha_data_internal+320]
        vfmadd213ps xmm1, xmm3, XMMWORD PTR [__svml_slog_ha_data_internal+256]
        vfmadd213ps xmm1, xmm3, XMMWORD PTR [__svml_slog_ha_data_internal+192]
        vfmadd213ps xmm1, xmm3, XMMWORD PTR [__svml_slog_ha_data_internal+128]
        vfmadd213ps xmm1, xmm3, XMMWORD PTR [__svml_slog_ha_data_internal+64]
        vfmadd213ps xmm1, xmm3, XMMWORD PTR [__svml_slog_ha_data_internal]
        vmulps    xmm1, xmm3, xmm1
        vfmadd213ps xmm1, xmm3, xmm3
        vfmadd231ps xmm1, xmm0, XMMWORD PTR [__svml_slog_ha_data_internal+960]
        vfmadd132ps xmm0, xmm1, XMMWORD PTR [__svml_slog_ha_data_internal+896]
        mov       QWORD PTR [200+rsp], r13
        test      edx, edx
        jne       _B4_3

_B4_2::

        mov       r13, QWORD PTR [192+rsp]
        add       rsp, 216
        ret

_B4_3::

        vmovups   XMMWORD PTR [r13], xmm4
        vmovups   XMMWORD PTR [64+r13], xmm0

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
        cmp       ebx, 4
        jl        _B4_7

_B4_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovups   xmm0, XMMWORD PTR [64+r13]
        jmp       _B4_2

_B4_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_slog_ha_cout_rare_internal
        jmp       _B4_8
        ALIGN     16

_B4_11::

__svml_logf4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_logf4_ha_l9_B1_B3:
	DD	268033
	DD	1627159
	DD	1769739

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_logf4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_logf4_ha_l9_B6_B10:
	DD	527873
	DD	287758
	DD	340999
	DD	1627136
	DD	1769728

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_6
	DD	imagerel _B4_11
	DD	imagerel _unwind___svml_logf4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_logf8_ha_e9

__svml_logf8_ha_e9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L23::

        sub       rsp, 552
        vmovups   YMMWORD PTR [432+rsp], ymm15
        vmovups   YMMWORD PTR [464+rsp], ymm14
        vmovups   YMMWORD PTR [496+rsp], ymm9
        vmovups   YMMWORD PTR [400+rsp], ymm6
        vmovaps   ymm15, ymm0
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [303+rsp]
        vmovups   xmm3, XMMWORD PTR [__svml_slog_ha_data_internal+704]
        and       r13, -64
        vmovups   xmm4, XMMWORD PTR [__svml_slog_ha_data_internal+768]
        vmovups   xmm0, XMMWORD PTR [__svml_slog_ha_data_internal+576]
        vmovups   xmm1, XMMWORD PTR [__svml_slog_ha_data_internal+640]
        mov       QWORD PTR [536+rsp], r13
        vpsubd    xmm5, xmm15, xmm3
        vextractf128 xmm2, ymm15, 1
        vpsrad    xmm9, xmm5, 23
        vpand     xmm5, xmm5, xmm4
        vpaddd    xmm5, xmm5, xmm3
        vpsubd    xmm6, xmm2, xmm3
        vpaddd    xmm2, xmm0, xmm2
        vpand     xmm4, xmm6, xmm4
        vpsrad    xmm14, xmm6, 23
        vpaddd    xmm3, xmm4, xmm3
        vpaddd    xmm4, xmm15, xmm0
        vinsertf128 ymm3, ymm5, xmm3, 1
        vpcmpgtd  xmm5, xmm1, xmm4
        vpcmpgtd  xmm1, xmm1, xmm2
        vpackssdw xmm0, xmm5, xmm1
        vpxor     xmm2, xmm2, xmm2
        vsubps    ymm1, ymm3, YMMWORD PTR [__svml_slog_ha_data_internal+832]
        vpacksswb xmm4, xmm0, xmm2
        vpmovmskb edx, xmm4
        vmulps    ymm3, ymm1, YMMWORD PTR [__svml_slog_ha_data_internal+512]
        vaddps    ymm6, ymm3, YMMWORD PTR [__svml_slog_ha_data_internal+448]
        vinsertf128 ymm14, ymm9, xmm14, 1
        vcvtdq2ps ymm9, ymm14
        vmulps    ymm14, ymm1, ymm6
        vaddps    ymm0, ymm14, YMMWORD PTR [__svml_slog_ha_data_internal+384]
        vmulps    ymm2, ymm1, ymm0
        vaddps    ymm3, ymm2, YMMWORD PTR [__svml_slog_ha_data_internal+320]
        vmulps    ymm4, ymm1, ymm3
        vaddps    ymm5, ymm4, YMMWORD PTR [__svml_slog_ha_data_internal+256]
        vmulps    ymm6, ymm1, ymm5
        vaddps    ymm14, ymm6, YMMWORD PTR [__svml_slog_ha_data_internal+192]
        vmulps    ymm0, ymm1, ymm14
        vaddps    ymm2, ymm0, YMMWORD PTR [__svml_slog_ha_data_internal+128]
        vmulps    ymm3, ymm1, ymm2
        vaddps    ymm4, ymm3, YMMWORD PTR [__svml_slog_ha_data_internal+64]
        vmulps    ymm5, ymm1, ymm4
        vaddps    ymm6, ymm5, YMMWORD PTR [__svml_slog_ha_data_internal]
        vmulps    ymm14, ymm1, ymm6
        vmulps    ymm0, ymm1, ymm14
        vaddps    ymm1, ymm1, ymm0
        vmulps    ymm0, ymm9, YMMWORD PTR [__svml_slog_ha_data_internal+960]
        vmulps    ymm9, ymm9, YMMWORD PTR [__svml_slog_ha_data_internal+896]
        vaddps    ymm1, ymm1, ymm0
        vaddps    ymm0, ymm1, ymm9
        test      dl, dl
        jne       _B5_3

_B5_2::

        vmovups   ymm6, YMMWORD PTR [400+rsp]
        vmovups   ymm9, YMMWORD PTR [496+rsp]
        vmovups   ymm14, YMMWORD PTR [464+rsp]
        vmovups   ymm15, YMMWORD PTR [432+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B5_3::

        vmovups   YMMWORD PTR [r13], ymm15
        vmovups   YMMWORD PTR [64+r13], ymm0
        test      edx, edx
        je        _B5_2

_B5_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [192+rsp], ymm7
        vmovups   YMMWORD PTR [160+rsp], ymm8
        vmovups   YMMWORD PTR [128+rsp], ymm10
        vmovups   YMMWORD PTR [96+rsp], ymm11
        vmovups   YMMWORD PTR [64+rsp], ymm12
        vmovups   YMMWORD PTR [32+rsp], ymm13
        mov       QWORD PTR [232+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [224+rsp], rsi
        mov       esi, edx

_B5_7::

        bt        esi, ebx
        jc        _B5_10

_B5_8::

        inc       ebx
        cmp       ebx, 8
        jl        _B5_7

_B5_9::

        vmovups   ymm7, YMMWORD PTR [192+rsp]
        vmovups   ymm8, YMMWORD PTR [160+rsp]
        vmovups   ymm10, YMMWORD PTR [128+rsp]
        vmovups   ymm11, YMMWORD PTR [96+rsp]
        vmovups   ymm12, YMMWORD PTR [64+rsp]
        vmovups   ymm13, YMMWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [232+rsp]
        mov       rsi, QWORD PTR [224+rsp]
        jmp       _B5_2

_B5_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_slog_ha_cout_rare_internal
        jmp       _B5_8
        ALIGN     16

_B5_11::

__svml_logf8_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_logf8_ha_e9_B1_B3:
	DD	801537
	DD	4379707
	DD	1665071
	DD	2070566
	DD	1959965
	DD	1832980
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_logf8_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_logf8_ha_e9_B6_B10:
	DD	1065249
	DD	1860673
	DD	1913911
	DD	186415
	DD	313385
	DD	440355
	DD	567325
	DD	690196
	DD	817163
	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_logf8_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_6
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_logf8_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_logf8_ha_l9

__svml_logf8_ha_l9	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L50::

        sub       rsp, 552
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [431+rsp]
        vmovaps   ymm4, ymm0
        and       r13, -64
        vmovups   ymm5, YMMWORD PTR [__svml_slog_ha_data_internal+640]
        vmovups   ymm1, YMMWORD PTR [__svml_slog_ha_data_internal+704]
        vpaddd    ymm0, ymm4, YMMWORD PTR [__svml_slog_ha_data_internal+576]
        vpcmpgtd  ymm2, ymm5, ymm0
        vpsubd    ymm5, ymm4, ymm1
        vpand     ymm3, ymm5, YMMWORD PTR [__svml_slog_ha_data_internal+768]
        vpsrad    ymm0, ymm5, 23
        vpaddd    ymm1, ymm3, ymm1
        vcvtdq2ps ymm0, ymm0
        vsubps    ymm3, ymm1, YMMWORD PTR [__svml_slog_ha_data_internal+832]
        vmovmskps edx, ymm2
        vmovups   ymm2, YMMWORD PTR [__svml_slog_ha_data_internal+512]
        vfmadd213ps ymm2, ymm3, YMMWORD PTR [__svml_slog_ha_data_internal+448]
        vfmadd213ps ymm2, ymm3, YMMWORD PTR [__svml_slog_ha_data_internal+384]
        vfmadd213ps ymm2, ymm3, YMMWORD PTR [__svml_slog_ha_data_internal+320]
        vfmadd213ps ymm2, ymm3, YMMWORD PTR [__svml_slog_ha_data_internal+256]
        vfmadd213ps ymm2, ymm3, YMMWORD PTR [__svml_slog_ha_data_internal+192]
        vfmadd213ps ymm2, ymm3, YMMWORD PTR [__svml_slog_ha_data_internal+128]
        vfmadd213ps ymm2, ymm3, YMMWORD PTR [__svml_slog_ha_data_internal+64]
        vfmadd213ps ymm2, ymm3, YMMWORD PTR [__svml_slog_ha_data_internal]
        vmulps    ymm1, ymm3, ymm2
        vfmadd213ps ymm1, ymm3, ymm3
        vfmadd231ps ymm1, ymm0, YMMWORD PTR [__svml_slog_ha_data_internal+960]
        vfmadd132ps ymm0, ymm1, YMMWORD PTR [__svml_slog_ha_data_internal+896]
        mov       QWORD PTR [536+rsp], r13
        test      edx, edx
        jne       _B6_3

_B6_2::

        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B6_3::

        vmovups   YMMWORD PTR [r13], ymm4
        vmovups   YMMWORD PTR [64+r13], ymm0

_B6_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [320+rsp], ymm6
        vmovups   YMMWORD PTR [288+rsp], ymm7
        vmovups   YMMWORD PTR [256+rsp], ymm8
        vmovups   YMMWORD PTR [224+rsp], ymm9
        vmovups   YMMWORD PTR [192+rsp], ymm10
        vmovups   YMMWORD PTR [160+rsp], ymm11
        vmovups   YMMWORD PTR [128+rsp], ymm12
        vmovups   YMMWORD PTR [96+rsp], ymm13
        vmovups   YMMWORD PTR [64+rsp], ymm14
        vmovups   YMMWORD PTR [32+rsp], ymm15
        mov       QWORD PTR [360+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [352+rsp], rsi
        mov       esi, edx

_B6_7::

        bt        esi, ebx
        jc        _B6_10

_B6_8::

        inc       ebx
        cmp       ebx, 8
        jl        _B6_7

_B6_9::

        vmovups   ymm6, YMMWORD PTR [320+rsp]
        vmovups   ymm7, YMMWORD PTR [288+rsp]
        vmovups   ymm8, YMMWORD PTR [256+rsp]
        vmovups   ymm9, YMMWORD PTR [224+rsp]
        vmovups   ymm10, YMMWORD PTR [192+rsp]
        vmovups   ymm11, YMMWORD PTR [160+rsp]
        vmovups   ymm12, YMMWORD PTR [128+rsp]
        vmovups   ymm13, YMMWORD PTR [96+rsp]
        vmovups   ymm14, YMMWORD PTR [64+rsp]
        vmovups   ymm15, YMMWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [360+rsp]
        mov       rsi, QWORD PTR [352+rsp]
        jmp       _B6_2

_B6_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_slog_ha_cout_rare_internal
        jmp       _B6_8
        ALIGN     16

_B6_11::

__svml_logf8_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_logf8_ha_l9_B1_B3:
	DD	267009
	DD	4379667
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_logf8_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_logf8_ha_l9_B6_B10:
	DD	1860865
	DD	2909285
	DD	2962523
	DD	194643
	DD	321613
	DD	448583
	DD	575553
	DD	702520
	DD	829487
	DD	956454
	DD	1083421
	DD	1210388
	DD	1337355
	DD	4379648
	DD	4522240

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_6
	DD	imagerel _B6_11
	DD	imagerel _unwind___svml_logf8_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_slog_ha_cout_rare_internal

__svml_slog_ha_cout_rare_internal	PROC

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L77::

        sub       rsp, 56
        mov       r9, rcx
        xor       eax, eax
        movzx     r8d, WORD PTR [2+r9]
        and       r8d, 32640
        cmp       r8d, 32640
        je        _B7_12

_B7_2::

        pxor      xmm3, xmm3
        xor       r8d, r8d
        cvtss2sd  xmm3, DWORD PTR [r9]
        movsd     QWORD PTR [48+rsp], xmm3
        movzx     ecx, WORD PTR [54+rsp]
        test      ecx, 32752
        jne       _B7_4

_B7_3::

        mulsd     xmm3, QWORD PTR [_imlsLnHATab+1600]
        mov       r8d, -60
        movsd     QWORD PTR [48+rsp], xmm3

_B7_4::

        movsd     xmm0, QWORD PTR [_imlsLnHATab+1608]
        comisd    xmm3, xmm0
        jbe       _B7_8

_B7_5::

        movsd     xmm0, QWORD PTR [_2il0floatpacket_86]
        movaps    xmm2, xmm3
        subsd     xmm2, xmm0
        movsd     QWORD PTR [40+rsp], xmm2
        and       BYTE PTR [47+rsp], 127
        movsd     xmm1, QWORD PTR [40+rsp]
        comisd    xmm1, QWORD PTR [_imlsLnHATab+1592]
        jbe       _B7_7

_B7_6::

        movsd     QWORD PTR [40+rsp], xmm3
        pxor      xmm1, xmm1
        movzx     r9d, WORD PTR [46+rsp]
        and       r9d, -32753
        add       r9d, 16368
        mov       WORD PTR [46+rsp], r9w
        movsd     xmm5, QWORD PTR [40+rsp]
        movaps    xmm3, xmm5
        movzx     ecx, WORD PTR [54+rsp]
        and       ecx, 32752
        addsd     xmm3, QWORD PTR [_imlsLnHATab+1576]
        shr       ecx, 4
        movsd     QWORD PTR [32+rsp], xmm3
        movaps    xmm3, xmm5
        mov       r10d, DWORD PTR [32+rsp]
        and       r10d, 127
        addsd     xmm3, QWORD PTR [_imlsLnHATab+1584]
        movsd     QWORD PTR [32+rsp], xmm3
        lea       r8d, DWORD PTR [-1023+r8+rcx]
        cvtsi2sd  xmm1, r8d
        movsd     xmm3, QWORD PTR [32+rsp]
        lea       rcx, QWORD PTR [__ImageBase]
        movsd     xmm2, QWORD PTR [_imlsLnHATab+1560]
        lea       r11d, DWORD PTR [r10+r10*2]
        movsd     xmm4, QWORD PTR [_imlsLnHATab+1568]
        mov       r9, rcx
        mov       r8, rcx
        subsd     xmm3, QWORD PTR [_imlsLnHATab+1584]
        mulsd     xmm2, xmm1
        mulsd     xmm1, xmm4
        subsd     xmm5, xmm3
        addsd     xmm2, QWORD PTR [imagerel(_imlsLnHATab)+8+r8+r11*8]
        addsd     xmm1, QWORD PTR [imagerel(_imlsLnHATab)+16+r9+r11*8]
        movsd     xmm4, QWORD PTR [imagerel(_imlsLnHATab)+rcx+r11*8]
        mulsd     xmm3, xmm4
        mulsd     xmm4, xmm5
        subsd     xmm3, xmm0
        movaps    xmm0, xmm3
        addsd     xmm2, xmm3
        addsd     xmm0, xmm4
        addsd     xmm4, xmm1
        movsd     xmm5, QWORD PTR [_imlsLnHATab+1672]
        mulsd     xmm5, xmm0
        addsd     xmm5, QWORD PTR [_imlsLnHATab+1664]
        mulsd     xmm5, xmm0
        addsd     xmm5, QWORD PTR [_imlsLnHATab+1656]
        mulsd     xmm5, xmm0
        addsd     xmm5, QWORD PTR [_imlsLnHATab+1648]
        mulsd     xmm5, xmm0
        addsd     xmm5, QWORD PTR [_imlsLnHATab+1640]
        mulsd     xmm5, xmm0
        addsd     xmm5, QWORD PTR [_imlsLnHATab+1632]
        mulsd     xmm5, xmm0
        mulsd     xmm0, xmm0
        addsd     xmm5, QWORD PTR [_imlsLnHATab+1624]
        mulsd     xmm5, xmm0
        addsd     xmm4, xmm5
        addsd     xmm2, xmm4
        cvtsd2ss  xmm2, xmm2
        movss     DWORD PTR [rdx], xmm2
        add       rsp, 56
        ret

_B7_7::

        movsd     xmm1, QWORD PTR [_imlsLnHATab+1672]
        movaps    xmm0, xmm2
        mulsd     xmm1, xmm2
        mulsd     xmm0, xmm2
        addsd     xmm1, QWORD PTR [_imlsLnHATab+1664]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_imlsLnHATab+1656]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_imlsLnHATab+1648]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_imlsLnHATab+1640]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_imlsLnHATab+1632]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_imlsLnHATab+1624]
        mulsd     xmm1, xmm0
        addsd     xmm1, xmm2
        cvtsd2ss  xmm1, xmm1
        movss     DWORD PTR [rdx], xmm1
        add       rsp, 56
        ret

_B7_8::

        ucomisd   xmm3, xmm0
        jp        _B7_9
        je        _B7_11

_B7_9::

        divsd     xmm0, xmm0
        cvtsd2ss  xmm0, xmm0
        movss     DWORD PTR [rdx], xmm0
        mov       eax, 1

_B7_10::

        add       rsp, 56
        ret

_B7_11::

        movsd     xmm1, QWORD PTR [_imlsLnHATab+1616]
        mov       eax, 2
        xorps     xmm1, XMMWORD PTR [_2il0floatpacket_87]
        divsd     xmm1, xmm0
        cvtsd2ss  xmm1, xmm1
        movss     DWORD PTR [rdx], xmm1
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

        movsd     xmm0, QWORD PTR [_imlsLnHATab+1608]
        mov       eax, 1
        divsd     xmm0, xmm0
        cvtsd2ss  xmm0, xmm0
        movss     DWORD PTR [rdx], xmm0
        add       rsp, 56
        ret
        ALIGN     16

_B7_16::

__svml_slog_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_slog_ha_cout_rare_internal_B1_B15:
	DD	67585
	DD	25096

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_16
	DD	imagerel _unwind___svml_slog_ha_cout_rare_internal_B1_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_slog_ha_data_internal_avx512
__svml_slog_ha_data_internal_avx512	DD	131072
	DD	131072
	DD	131072
	DD	131072
	DD	131072
	DD	131072
	DD	131072
	DD	131072
	DD	131072
	DD	131072
	DD	131072
	DD	131072
	DD	131072
	DD	131072
	DD	131072
	DD	131072
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	4294705152
	DD	0
	DD	3170631680
	DD	3178782720
	DD	3182919680
	DD	3186704384
	DD	3189024768
	DD	3190814720
	DD	3192557568
	DD	3194257408
	DD	3195914240
	DD	3196795904
	DD	3197585408
	DD	3198356480
	DD	3199110144
	DD	3199848448
	DD	3200569344
	DD	3201275904
	DD	3201968128
	DD	3202646016
	DD	3203310592
	DD	3203961856
	DD	3204524544
	DD	3204838400
	DD	3205146112
	DD	3205448192
	DD	3205745152
	DD	3206036992
	DD	3206324224
	DD	3206605824
	DD	3206883328
	DD	3207156224
	DD	3207424512
	DD	0
	DD	3072770974
	DD	929538039
	DD	3075640037
	DD	930648533
	DD	3072716864
	DD	3066151582
	DD	3073718761
	DD	897812054
	DD	3057871602
	DD	923619665
	DD	921315575
	DD	3057394118
	DD	3078028640
	DD	924853521
	DD	3075349253
	DD	3071259390
	DD	906511159
	DD	906200662
	DD	917494258
	DD	3061185264
	DD	3071618351
	DD	919715245
	DD	3052715317
	DD	3078288258
	DD	3077608526
	DD	3074005625
	DD	930702671
	DD	3070133351
	DD	913590776
	DD	924241186
	DD	3021499198
	DD	1060205056
	DD	1059688960
	DD	1059187712
	DD	1058701824
	DD	1058229248
	DD	1057769472
	DD	1057321984
	DD	1056807936
	DD	1055958016
	DD	1055129600
	DD	1054320640
	DD	1053531136
	DD	1052760064
	DD	1052006400
	DD	1051268096
	DD	1050547200
	DD	1049840640
	DD	1049148416
	DD	1048365056
	DD	1047035904
	DD	1045733376
	DD	1044455424
	DD	1043200000
	DD	1041969152
	DD	1040760832
	DD	1038958592
	DD	1036623872
	DD	1034330112
	DD	1032073216
	DD	1027907584
	DD	1023541248
	DD	1015087104
	DD	901758606
	DD	3071200204
	DD	931108809
	DD	3074069268
	DD	3077535321
	DD	3071146094
	DD	3063010043
	DD	3072147991
	DD	908173938
	DD	3049723733
	DD	925190435
	DD	923601997
	DD	3048768765
	DD	3076457870
	DD	926424291
	DD	3073778483
	DD	3069146713
	DD	912794238
	DD	912483742
	DD	920635797
	DD	3054902185
	DD	3069864633
	DD	922801832
	DD	3033791132
	DD	3076717488
	DD	3076037756
	DD	3072434855
	DD	3077481184
	DD	3066991812
	DD	917116064
	DD	925811956
	DD	900509991
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
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	3196061712
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	1051373854
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
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
	DD	262144
	DD	262144
	DD	262144
	DD	262144
	DD	262144
	DD	262144
	DD	262144
	DD	262144
	DD	262144
	DD	262144
	DD	262144
	DD	262144
	DD	262144
	DD	262144
	DD	262144
	DD	262144
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
	DD	4294443008
	DD	0
	DD	3178782720
	DD	3186704384
	DD	3190814720
	DD	3194257408
	DD	3196795904
	DD	3198356480
	DD	3199848448
	DD	3201275904
	DD	3202646016
	DD	3203961856
	DD	3204838400
	DD	3205448192
	DD	3206036992
	DD	3206605824
	DD	3207156224
	DD	0
	DD	929538039
	DD	930648533
	DD	3066151582
	DD	897812054
	DD	923619665
	DD	3057394118
	DD	924853521
	DD	3071259390
	DD	906200662
	DD	3061185264
	DD	919715245
	DD	3078288258
	DD	3074005625
	DD	3070133351
	DD	924241186
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
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	1045236958
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	3196066480
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	1051372199
	DD	3204448254
	DD	3204448254
	DD	3204448254
	DD	3204448254
	DD	3204448254
	DD	3204448254
	DD	3204448254
	DD	3204448254
	DD	3204448254
	DD	3204448254
	DD	3204448254
	DD	3204448254
	DD	3204448254
	DD	3204448254
	DD	3204448254
	DD	3204448254
	PUBLIC __svml_slog_ha_data_internal
__svml_slog_ha_data_internal	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	3204448256
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	1051372163
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	3196059512
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	1045227540
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	3190476518
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	1041197964
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	3187247262
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	1041183581
	DD	3187933896
	DD	3187933896
	DD	3187933896
	DD	3187933896
	DD	3187933896
	DD	3187933896
	DD	3187933896
	DD	3187933896
	DD	3187933896
	DD	3187933896
	DD	3187933896
	DD	3187933896
	DD	3187933896
	DD	3187933896
	DD	3187933896
	DD	3187933896
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
	DD	16777216
	DD	16777216
	DD	16777216
	DD	16777216
	DD	16777216
	DD	16777216
	DD	16777216
	DD	16777216
	DD	16777216
	DD	16777216
	DD	16777216
	DD	16777216
	DD	16777216
	DD	16777216
	DD	16777216
	DD	16777216
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
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	1060205056
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
	DD	901758606
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
_imlsLnHATab	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1072660480
	DD	1486880768
	DD	1066410070
	DD	1813744607
	DD	3179892593
	DD	0
	DD	1072629760
	DD	377487360
	DD	1067416219
	DD	919019713
	DD	3179241129
	DD	0
	DD	1072599040
	DD	1513619456
	DD	1067944025
	DD	874573033
	DD	3178512940
	DD	0
	DD	1072570368
	DD	3221749760
	DD	1068427825
	DD	4181665006
	DD	3177478212
	DD	0
	DD	1072541696
	DD	4162322432
	DD	1068708823
	DD	627020255
	DD	1028629941
	DD	0
	DD	1072513024
	DD	183107584
	DD	1068957907
	DD	2376703469
	DD	1030233118
	DD	0
	DD	1072486400
	DD	1053425664
	DD	1069192557
	DD	696277142
	DD	1030474863
	DD	0
	DD	1072459776
	DD	3996123136
	DD	1069430535
	DD	2630798680
	DD	1028792016
	DD	0
	DD	1072435200
	DD	3452764160
	DD	1069600382
	DD	624954044
	DD	3177101741
	DD	0
	DD	1072409600
	DD	207650816
	DD	1069717971
	DD	3272735636
	DD	3175176575
	DD	0
	DD	1072386048
	DD	2647228416
	DD	1069827627
	DD	3594228712
	DD	1029303785
	DD	0
	DD	1072362496
	DD	2712010752
	DD	1069938736
	DD	3653242769
	DD	3176839013
	DD	0
	DD	1072338944
	DD	374439936
	DD	1070051337
	DD	4072775574
	DD	3176577495
	DD	0
	DD	1072316416
	DD	3707174912
	DD	1070160474
	DD	1486946159
	DD	1023930920
	DD	0
	DD	1072294912
	DD	1443954688
	DD	1070265993
	DD	293532967
	DD	3176278277
	DD	0
	DD	1072273408
	DD	127762432
	DD	1070372856
	DD	3404145447
	DD	3177023955
	DD	0
	DD	1072252928
	DD	2053832704
	DD	1070475911
	DD	1575076358
	DD	1029048544
	DD	0
	DD	1072232448
	DD	3194093568
	DD	1070580248
	DD	1864169120
	DD	1026866084
	DD	0
	DD	1072212992
	DD	3917201408
	DD	1070638340
	DD	2362145246
	DD	3175606197
	DD	0
	DD	1072193536
	DD	3417112576
	DD	1070689116
	DD	70087871
	DD	3174183577
	DD	0
	DD	1072175104
	DD	4226777088
	DD	1070737793
	DD	1620410586
	DD	3174700065
	DD	0
	DD	1072156672
	DD	3168870400
	DD	1070787042
	DD	311238082
	DD	1025781772
	DD	0
	DD	1072139264
	DD	2150580224
	DD	1070834092
	DD	1664262457
	DD	3175299224
	DD	0
	DD	1072120832
	DD	4095672320
	DD	1070884491
	DD	1657121015
	DD	3174674199
	DD	0
	DD	1072104448
	DD	2595577856
	DD	1070929805
	DD	2014006823
	DD	3175423830
	DD	0
	DD	1072087040
	DD	3747176448
	DD	1070978493
	DD	144991708
	DD	3171552042
	DD	0
	DD	1072070656
	DD	1050435584
	DD	1071024840
	DD	3386227432
	DD	1027876916
	DD	0
	DD	1072055296
	DD	255516672
	DD	1071068760
	DD	2637594316
	DD	1028049573
	DD	0
	DD	1072038912
	DD	1640783872
	DD	1071116120
	DD	893247007
	DD	1028452162
	DD	0
	DD	1072023552
	DD	2940411904
	DD	1071161011
	DD	813240633
	DD	1027664048
	DD	0
	DD	1072009216
	DD	882917376
	DD	1071203348
	DD	2376597551
	DD	3175828767
	DD	0
	DD	1071993856
	DD	213966848
	DD	1071249188
	DD	2977204125
	DD	1028350609
	DD	0
	DD	1071979520
	DD	2921504768
	DD	1071292428
	DD	523218347
	DD	1028007004
	DD	0
	DD	1071965184
	DD	3186655232
	DD	1071336119
	DD	2352907891
	DD	1026967097
	DD	0
	DD	1071951872
	DD	2653364224
	DD	1071377101
	DD	2453418583
	DD	3174349512
	DD	0
	DD	1071938560
	DD	3759783936
	DD	1071418487
	DD	3685870403
	DD	3175415611
	DD	0
	DD	1071925248
	DD	2468364288
	DD	1071460286
	DD	1578908842
	DD	3175510517
	DD	0
	DD	1071911936
	DD	81903616
	DD	1071502506
	DD	770710269
	DD	1026742353
	DD	0
	DD	1071899648
	DD	2799321088
	DD	1071541858
	DD	3822266185
	DD	1028434427
	DD	0
	DD	1071886336
	DD	2142265344
	DD	1071584911
	DD	175901806
	DD	3173871540
	DD	0
	DD	1071874048
	DD	2944024576
	DD	1071625048
	DD	2747360403
	DD	1027672159
	DD	0
	DD	1071862784
	DD	3434301440
	DD	1071653426
	DD	4194662196
	DD	3173893003
	DD	0
	DD	1071850496
	DD	1547755520
	DD	1071673870
	DD	4248764681
	DD	3172759087
	DD	0
	DD	1071839232
	DD	4246986752
	DD	1071692786
	DD	2840205638
	DD	3174430911
	DD	0
	DD	1071826944
	DD	3418390528
	DD	1071713619
	DD	3041880823
	DD	1025440860
	DD	0
	DD	1071816704
	DD	4143093760
	DD	1071731139
	DD	2727587401
	DD	3173965207
	DD	0
	DD	1071805440
	DD	3121326080
	DD	1071750582
	DD	3173887692
	DD	3174190163
	DD	0
	DD	1071794176
	DD	1852893184
	DD	1071770207
	DD	3951060252
	DD	1027348295
	DD	0
	DD	1071783936
	DD	3636379648
	DD	1071788208
	DD	1684924001
	DD	3174777086
	DD	0
	DD	1071773696
	DD	516505600
	DD	1071806366
	DD	429181199
	DD	3173211033
	DD	0
	DD	1071763456
	DD	4186185728
	DD	1071824681
	DD	2044904577
	DD	3174967132
	DD	0
	DD	1071753216
	DD	877596672
	DD	1071843159
	DD	1396318105
	DD	3173959727
	DD	0
	DD	1071742976
	DD	2912784384
	DD	1071861800
	DD	448136789
	DD	3174814192
	DD	0
	DD	1071733760
	DD	3722825728
	DD	1071878720
	DD	714165913
	DD	3173439560
	DD	0
	DD	1071723520
	DD	2522374144
	DD	1071897682
	DD	3227240353
	DD	3173394323
	DD	0
	DD	1071714304
	DD	4165410816
	DD	1071914895
	DD	1365684961
	DD	3174365060
	DD	0
	DD	1071705088
	DD	3477135360
	DD	1071932251
	DD	368482985
	DD	3174140821
	DD	0
	DD	1071695872
	DD	2079455232
	DD	1071949752
	DD	1320576317
	DD	1026822714
	DD	0
	DD	1071687680
	DD	851795968
	DD	1071965432
	DD	3702467026
	DD	1025224125
	DD	0
	DD	1071678464
	DD	647743488
	DD	1071983213
	DD	772992109
	DD	3174038459
	DD	0
	DD	1071670272
	DD	26537984
	DD	1071999146
	DD	2360214276
	DD	3174861275
	DD	0
	DD	1071661056
	DD	1547061248
	DD	1072017216
	DD	2886781435
	DD	1026423395
	DD	0
	DD	1071652864
	DD	2854492160
	DD	1072033410
	DD	215631550
	DD	1025638968
	DD	0
	DD	1071644672
	DD	4277811200
	DD	1072049730
	DD	2479318832
	DD	1026487127
	DD	4277811200
	DD	1072049730
	DD	2479318832
	DD	1026487127
	DD	64
	DD	1120927744
	DD	0
	DD	1094713344
	DD	0
	DD	1065615360
	DD	0
	DD	1135607808
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	3219128320
	DD	1431655955
	DD	1070945621
	DD	610
	DD	3218079744
	DD	2545118337
	DD	1070176665
	DD	1378399119
	DD	3217380693
	DD	612435357
	DD	1069697472
	DD	94536557
	DD	3217031348
_2il0floatpacket_87	DD	000000000H,080000000H,000000000H,000000000H
_2il0floatpacket_86	DD	000000000H,03ff00000H
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
