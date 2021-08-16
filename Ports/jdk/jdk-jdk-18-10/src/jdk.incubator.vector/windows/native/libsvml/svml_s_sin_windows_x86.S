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
	PUBLIC __svml_sinf16_ha_z0

__svml_sinf16_ha_z0	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 1336
        mov       QWORD PTR [1320+rsp], r13
        lea       r13, QWORD PTR [1183+rsp]
        vmovups   zmm4, ZMMWORD PTR [__svml_ssin_ha_data_internal+4096]
        and       r13, -64
        vmovups   zmm24, ZMMWORD PTR [__svml_ssin_ha_data_internal+5056]
        vmovups   zmm25, ZMMWORD PTR [__svml_ssin_ha_data_internal+4544]
        vmovups   zmm28, ZMMWORD PTR [__svml_ssin_ha_data_internal+5120]
        vmovups   zmm30, ZMMWORD PTR [__svml_ssin_ha_data_internal+5184]
        vmovaps   zmm22, zmm0
        vandps    zmm5, zmm22, zmm4
        vandnps   zmm1, zmm4, zmm22
        vmovups   zmm4, ZMMWORD PTR [__svml_ssin_ha_data_internal+5440]
        mov       QWORD PTR [1328+rsp], r13
        vfmadd213ps zmm24, zmm5, zmm25 {rn-sae}
        vcvtps2pd zmm29, ymm5 {sae}
        vextractf32x8 ymm23, zmm5, 1
        vsubps    zmm26, zmm24, zmm25 {rn-sae}
        vpslld    zmm3, zmm24, 31
        vcvtps2pd zmm31, ymm23 {sae}
        vmovups   zmm23, ZMMWORD PTR [__svml_ssin_ha_data_internal+5312]
        vmovups   zmm24, ZMMWORD PTR [__svml_ssin_ha_data_internal+5248]
        vextractf32x8 ymm27, zmm26, 1
        vcvtps2pd zmm2, ymm26 {sae}
        vcvtps2pd zmm0, ymm27 {sae}
        vmovups   zmm27, ZMMWORD PTR [__svml_ssin_ha_data_internal+5376]
        vfnmadd231pd zmm29, zmm28, zmm2 {rn-sae}
        vfnmadd231pd zmm31, zmm28, zmm0 {rn-sae}
        vfnmadd213pd zmm2, zmm30, zmm29 {rn-sae}
        vfnmadd213pd zmm0, zmm30, zmm31 {rn-sae}
        vmulpd    zmm26, zmm2, zmm2 {rn-sae}
        vmulpd    zmm28, zmm0, zmm0 {rn-sae}
        vmovaps   zmm25, zmm27
        vfmadd231pd zmm27, zmm4, zmm28 {rn-sae}
        vfmadd231pd zmm25, zmm4, zmm26 {rn-sae}
        vfmadd213pd zmm27, zmm28, zmm23 {rn-sae}
        vfmadd213pd zmm25, zmm26, zmm23 {rn-sae}
        vfmadd213pd zmm27, zmm28, zmm24 {rn-sae}
        vfmadd213pd zmm25, zmm26, zmm24 {rn-sae}
        vmulpd    zmm30, zmm27, zmm28 {rn-sae}
        vmulpd    zmm29, zmm25, zmm26 {rn-sae}
        vfmadd213pd zmm30, zmm0, zmm0 {rn-sae}
        vfmadd213pd zmm29, zmm2, zmm2 {rn-sae}
        vcvtpd2ps ymm0, zmm30 {rn-sae}
        vcvtpd2ps ymm2, zmm29 {rn-sae}
        vinsertf32x8 zmm0, zmm2, ymm0, 1
        vpternlogd zmm0, zmm3, zmm1, 150
        vpternlogd zmm3, zmm3, zmm3, 255
        vmovups   zmm1, ZMMWORD PTR [__svml_ssin_ha_data_internal+4160]
        vcmpps    k1, zmm5, zmm1, 18 {sae}
        vpandnd   zmm3{k1}, zmm5, zmm5
        vptestmd  k0, zmm3, zmm3
        kmovw     r8d, k0
        test      r8d, r8d
        jne       _B1_3

_B1_2::

        mov       r13, QWORD PTR [1320+rsp]
        add       rsp, 1336
        ret

_B1_3::

        vstmxcsr  DWORD PTR [1312+rsp]

_B1_4::

        movzx     edx, WORD PTR [1312+rsp]
        mov       eax, edx
        or        eax, 8064
        cmp       edx, eax
        je        _B1_6

_B1_5::

        mov       DWORD PTR [1312+rsp], eax
        vldmxcsr  DWORD PTR [1312+rsp]

_B1_6::

        vmovups   ZMMWORD PTR [r13], zmm22
        vmovups   ZMMWORD PTR [64+r13], zmm0
        test      r8d, r8d
        jne       _B1_11

_B1_7::

        cmp       edx, eax
        je        _B1_2

_B1_8::

        vstmxcsr  DWORD PTR [1312+rsp]
        mov       eax, DWORD PTR [1312+rsp]

_B1_9::

        and       eax, -8065
        or        eax, edx
        mov       DWORD PTR [1312+rsp], eax
        vldmxcsr  DWORD PTR [1312+rsp]
        jmp       _B1_2

_B1_11::

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

_B1_12::

        bt        edi, ebx
        jc        _B1_15

_B1_13::

        inc       ebx
        cmp       ebx, 16
        jl        _B1_12

_B1_14::

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
        jmp       _B1_7

_B1_15::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_ssin_ha_cout_rare_internal
        jmp       _B1_13
        ALIGN     16

_B1_16::

__svml_sinf16_ha_z0 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinf16_ha_z0_B1_B9:
	DD	267009
	DD	10867731
	DD	10944779

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_sinf16_ha_z0_B1_B9

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinf16_ha_z0_B11_B15:
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

	DD	imagerel _B1_11
	DD	imagerel _B1_16
	DD	imagerel _unwind___svml_sinf16_ha_z0_B11_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinf8_ha_l9

__svml_sinf8_ha_l9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L52::

        sub       rsp, 552
        vmovups   YMMWORD PTR [496+rsp], ymm15
        vmovups   YMMWORD PTR [368+rsp], ymm10
        vmovups   YMMWORD PTR [464+rsp], ymm9
        vmovups   YMMWORD PTR [400+rsp], ymm8
        vmovups   YMMWORD PTR [432+rsp], ymm6
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [271+rsp]
        vmovups   ymm5, YMMWORD PTR [__svml_ssin_ha_data_internal+4096]
        and       r13, -64
        vmovups   ymm1, YMMWORD PTR [__svml_ssin_ha_data_internal+5056]
        vmovupd   ymm8, YMMWORD PTR [__svml_ssin_ha_data_internal+5120]
        vmovupd   ymm9, YMMWORD PTR [__svml_ssin_ha_data_internal+5184]
        vmovdqa   ymm4, ymm0
        vmovups   ymm0, YMMWORD PTR [__svml_ssin_ha_data_internal+4544]
        vandps    ymm3, ymm4, ymm5
        mov       QWORD PTR [536+rsp], r13
        vfmadd213ps ymm1, ymm3, ymm0
        vcvtps2pd ymm6, xmm3
        vextractf128 xmm2, ymm3, 1
        vcmpnle_uqps ymm3, ymm3, YMMWORD PTR [__svml_ssin_ha_data_internal+4160]
        vcvtps2pd ymm15, xmm2
        vpslld    ymm2, ymm1, 31
        vsubps    ymm1, ymm1, ymm0
        vmovmskps edx, ymm3
        test      edx, edx
        vextractf128 xmm10, ymm1, 1
        vcvtps2pd ymm0, xmm1
        vcvtps2pd ymm1, xmm10
        vfnmadd231pd ymm6, ymm8, ymm0
        vfnmadd231pd ymm15, ymm8, ymm1
        vfnmadd213pd ymm0, ymm9, ymm6
        vfnmadd213pd ymm1, ymm9, ymm15
        vmovupd   ymm15, YMMWORD PTR [__svml_ssin_ha_data_internal+5440]
        vmulpd    ymm9, ymm0, ymm0
        vmulpd    ymm10, ymm1, ymm1
        vandnps   ymm6, ymm5, ymm4
        vmovupd   ymm5, YMMWORD PTR [__svml_ssin_ha_data_internal+5376]
        vmovdqa   ymm8, ymm5
        vfmadd231pd ymm8, ymm15, ymm9
        vfmadd231pd ymm5, ymm15, ymm10
        vmovupd   ymm15, YMMWORD PTR [__svml_ssin_ha_data_internal+5312]
        vfmadd213pd ymm8, ymm9, ymm15
        vfmadd213pd ymm5, ymm10, ymm15
        vmovupd   ymm15, YMMWORD PTR [__svml_ssin_ha_data_internal+5248]
        vfmadd213pd ymm8, ymm9, ymm15
        vfmadd213pd ymm5, ymm10, ymm15
        vmulpd    ymm8, ymm9, ymm8
        vmulpd    ymm5, ymm10, ymm5
        vfmadd213pd ymm8, ymm0, ymm0
        vfmadd213pd ymm5, ymm1, ymm1
        vxorps    ymm0, ymm2, ymm6
        vcvtpd2ps xmm2, ymm8
        vcvtpd2ps xmm6, ymm5
        vinsertf128 ymm2, ymm2, xmm6, 1
        vxorps    ymm0, ymm2, ymm0
        jne       _B2_3

_B2_2::

        vmovups   ymm6, YMMWORD PTR [432+rsp]
        vmovups   ymm8, YMMWORD PTR [400+rsp]
        vmovups   ymm9, YMMWORD PTR [464+rsp]
        vmovups   ymm10, YMMWORD PTR [368+rsp]
        vmovups   ymm15, YMMWORD PTR [496+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B2_3::

        vmovups   YMMWORD PTR [r13], ymm4
        vmovups   YMMWORD PTR [64+r13], ymm0

_B2_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [160+rsp], ymm7
        vmovups   YMMWORD PTR [128+rsp], ymm11
        vmovups   YMMWORD PTR [96+rsp], ymm12
        vmovups   YMMWORD PTR [64+rsp], ymm13
        vmovups   YMMWORD PTR [32+rsp], ymm14
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

        vmovups   ymm7, YMMWORD PTR [160+rsp]
        vmovups   ymm11, YMMWORD PTR [128+rsp]
        vmovups   ymm12, YMMWORD PTR [96+rsp]
        vmovups   ymm13, YMMWORD PTR [64+rsp]
        vmovups   ymm14, YMMWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [200+rsp]
        mov       rsi, QWORD PTR [192+rsp]
        jmp       _B2_2

_B2_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_ssin_ha_cout_rare_internal
        jmp       _B2_8
        ALIGN     16

_B2_11::

__svml_sinf8_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinf8_ha_l9_B1_B3:
	DD	933889
	DD	4379712
	DD	1796152
	DD	1673263
	DD	1939494
	DD	1550365
	DD	2095124
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_sinf8_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinf8_ha_l9_B6_B10:
	DD	931873
	DD	1598520
	DD	1651758
	DD	190502
	DD	317472
	DD	444442
	DD	571412
	DD	686091
	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_sinf8_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_6
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_sinf8_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinf4_ha_e9

__svml_sinf4_ha_e9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L79::

        sub       rsp, 232
        vmovaps   xmm3, xmm0
        vmovups   XMMWORD PTR [192+rsp], xmm14
        mov       QWORD PTR [208+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovups   xmm0, XMMWORD PTR [__svml_ssin_ha_data_internal+4096]
        and       r13, -64
        vandps    xmm2, xmm3, xmm0
        vmulps    xmm1, xmm2, XMMWORD PTR [__svml_ssin_ha_data_internal+5056]
        vcvtps2pd ymm14, xmm2
        vcmpnleps xmm2, xmm2, XMMWORD PTR [__svml_ssin_ha_data_internal+4160]
        vmovups   xmm5, XMMWORD PTR [__svml_ssin_ha_data_internal+4544]
        vmovmskps edx, xmm2
        vaddps    xmm4, xmm5, xmm1
        vpslld    xmm1, xmm4, 31
        vsubps    xmm5, xmm4, xmm5
        vcvtps2pd ymm4, xmm5
        vmulpd    ymm5, ymm4, YMMWORD PTR [__svml_ssin_ha_data_internal+5120]
        vmulpd    ymm4, ymm4, YMMWORD PTR [__svml_ssin_ha_data_internal+5184]
        vsubpd    ymm14, ymm14, ymm5
        vsubpd    ymm5, ymm14, ymm4
        vandnps   xmm4, xmm0, xmm3
        vmulpd    ymm14, ymm5, ymm5
        vmulpd    ymm0, ymm14, YMMWORD PTR [__svml_ssin_ha_data_internal+5440]
        vaddpd    ymm0, ymm0, YMMWORD PTR [__svml_ssin_ha_data_internal+5376]
        vmulpd    ymm0, ymm14, ymm0
        vaddpd    ymm0, ymm0, YMMWORD PTR [__svml_ssin_ha_data_internal+5312]
        vmulpd    ymm0, ymm14, ymm0
        vaddpd    ymm0, ymm0, YMMWORD PTR [__svml_ssin_ha_data_internal+5248]
        vmulpd    ymm14, ymm14, ymm0
        vmulpd    ymm0, ymm5, ymm14
        vaddpd    ymm5, ymm5, ymm0
        vxorps    xmm0, xmm1, xmm4
        vcvtpd2ps xmm1, ymm5
        mov       QWORD PTR [216+rsp], r13
        vxorps    xmm0, xmm1, xmm0
        test      edx, edx
        jne       _B3_3

_B3_2::

        vzeroupper
        vmovups   xmm14, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [208+rsp]
        add       rsp, 232
        ret

_B3_3::

        vmovups   XMMWORD PTR [r13], xmm3
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

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_ssin_ha_cout_rare_internal
        jmp       _B3_8
        ALIGN     16

_B3_11::

__svml_sinf4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinf4_ha_e9_B1_B3:
	DD	401409
	DD	1758240
	DD	845848
	DD	1900811

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_sinf4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinf4_ha_e9_B6_B10:
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
	DD	imagerel _unwind___svml_sinf4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinf8_ha_e9

__svml_sinf8_ha_e9	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L88::

        sub       rsp, 552
        vmovups   YMMWORD PTR [400+rsp], ymm15
        vmovups   YMMWORD PTR [368+rsp], ymm14
        vmovups   YMMWORD PTR [432+rsp], ymm13
        vmovups   YMMWORD PTR [496+rsp], ymm10
        vmovups   YMMWORD PTR [464+rsp], ymm9
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [271+rsp]
        vmovups   ymm1, YMMWORD PTR [__svml_ssin_ha_data_internal+4096]
        and       r13, -64
        vmovups   ymm10, YMMWORD PTR [__svml_ssin_ha_data_internal+4544]
        vmovaps   ymm4, ymm0
        vandps    ymm5, ymm4, ymm1
        vandnps   ymm1, ymm1, ymm4
        mov       QWORD PTR [536+rsp], r13
        vmulps    ymm13, ymm5, YMMWORD PTR [__svml_ssin_ha_data_internal+5056]
        vcvtps2pd ymm0, xmm5
        vextractf128 xmm9, ymm5, 1
        vaddps    ymm13, ymm10, ymm13
        vcmpnle_uqps ymm5, ymm5, YMMWORD PTR [__svml_ssin_ha_data_internal+4160]
        vcvtps2pd ymm2, xmm9
        vpslld    xmm15, xmm13, 31
        vextractf128 xmm14, ymm13, 1
        vsubps    ymm13, ymm13, ymm10
        vpslld    xmm3, xmm14, 31
        vcvtps2pd ymm14, xmm13
        vextractf128 xmm10, ymm13, 1
        vmovupd   ymm13, YMMWORD PTR [__svml_ssin_ha_data_internal+5120]
        vinsertf128 ymm9, ymm15, xmm3, 1
        vmulpd    ymm3, ymm14, ymm13
        vcvtps2pd ymm15, xmm10
        vsubpd    ymm10, ymm0, ymm3
        vmulpd    ymm0, ymm15, ymm13
        vmovupd   ymm3, YMMWORD PTR [__svml_ssin_ha_data_internal+5440]
        vsubpd    ymm13, ymm2, ymm0
        vmovupd   ymm2, YMMWORD PTR [__svml_ssin_ha_data_internal+5184]
        vmulpd    ymm14, ymm14, ymm2
        vmulpd    ymm15, ymm15, ymm2
        vmovupd   ymm2, YMMWORD PTR [__svml_ssin_ha_data_internal+5376]
        vsubpd    ymm10, ymm10, ymm14
        vsubpd    ymm13, ymm13, ymm15
        vmulpd    ymm14, ymm10, ymm10
        vmulpd    ymm15, ymm13, ymm13
        vmulpd    ymm0, ymm3, ymm14
        vmulpd    ymm3, ymm3, ymm15
        vaddpd    ymm0, ymm2, ymm0
        vaddpd    ymm2, ymm2, ymm3
        vmovupd   ymm3, YMMWORD PTR [__svml_ssin_ha_data_internal+5312]
        vmulpd    ymm0, ymm14, ymm0
        vmulpd    ymm2, ymm15, ymm2
        vaddpd    ymm0, ymm3, ymm0
        vaddpd    ymm3, ymm3, ymm2
        vmovupd   ymm2, YMMWORD PTR [__svml_ssin_ha_data_internal+5248]
        vmulpd    ymm0, ymm14, ymm0
        vmulpd    ymm3, ymm15, ymm3
        vaddpd    ymm0, ymm2, ymm0
        vaddpd    ymm2, ymm2, ymm3
        vmulpd    ymm14, ymm14, ymm0
        vmulpd    ymm0, ymm15, ymm2
        vmulpd    ymm15, ymm10, ymm14
        vaddpd    ymm2, ymm10, ymm15
        vmulpd    ymm10, ymm13, ymm0
        vaddpd    ymm13, ymm13, ymm10
        vxorps    ymm3, ymm9, ymm1
        vcvtpd2ps xmm1, ymm2
        vcvtpd2ps xmm2, ymm13
        vinsertf128 ymm0, ymm1, xmm2, 1
        vextractf128 xmm1, ymm5, 1
        vxorps    ymm0, ymm0, ymm3
        vpackssdw xmm2, xmm5, xmm1
        vpxor     xmm3, xmm3, xmm3
        vpacksswb xmm9, xmm2, xmm3
        vpmovmskb edx, xmm9
        test      dl, dl
        jne       _B4_3

_B4_2::

        vmovups   ymm9, YMMWORD PTR [464+rsp]
        vmovups   ymm10, YMMWORD PTR [496+rsp]
        vmovups   ymm13, YMMWORD PTR [432+rsp]
        vmovups   ymm14, YMMWORD PTR [368+rsp]
        vmovups   ymm15, YMMWORD PTR [400+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B4_3::

        vmovups   YMMWORD PTR [r13], ymm4
        vmovups   YMMWORD PTR [64+r13], ymm0
        test      edx, edx
        je        _B4_2

_B4_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [160+rsp], ymm6
        vmovups   YMMWORD PTR [128+rsp], ymm7
        vmovups   YMMWORD PTR [96+rsp], ymm8
        vmovups   YMMWORD PTR [64+rsp], ymm11
        vmovups   YMMWORD PTR [32+rsp], ymm12
        mov       QWORD PTR [200+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [192+rsp], rsi
        mov       esi, edx

_B4_7::

        bt        esi, ebx
        jc        _B4_10

_B4_8::

        inc       ebx
        cmp       ebx, 8
        jl        _B4_7

_B4_9::

        vmovups   ymm6, YMMWORD PTR [160+rsp]
        vmovups   ymm7, YMMWORD PTR [128+rsp]
        vmovups   ymm8, YMMWORD PTR [96+rsp]
        vmovups   ymm11, YMMWORD PTR [64+rsp]
        vmovups   ymm12, YMMWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [200+rsp]
        mov       rsi, QWORD PTR [192+rsp]
        jmp       _B4_2

_B4_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_ssin_ha_cout_rare_internal
        jmp       _B4_8
        ALIGN     16

_B4_11::

__svml_sinf8_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinf8_ha_e9_B1_B3:
	DD	933889
	DD	4379712
	DD	1939512
	DD	2074671
	DD	1824806
	DD	1566749
	DD	1701908
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_sinf8_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinf8_ha_e9_B6_B10:
	DD	931873
	DD	1598520
	DD	1651758
	DD	182310
	DD	309280
	DD	428058
	DD	555028
	DD	681995
	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_sinf8_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_6
	DD	imagerel _B4_11
	DD	imagerel _unwind___svml_sinf8_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinf4_ha_l9

__svml_sinf4_ha_l9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L115::

        sub       rsp, 232
        vmovups   XMMWORD PTR [192+rsp], xmm12
        mov       QWORD PTR [208+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovups   xmm4, XMMWORD PTR [__svml_ssin_ha_data_internal+4096]
        and       r13, -64
        vmovups   xmm3, XMMWORD PTR [__svml_ssin_ha_data_internal+5056]
        vandps    xmm1, xmm0, xmm4
        vmovups   xmm5, XMMWORD PTR [__svml_ssin_ha_data_internal+4544]
        vandnps   xmm4, xmm4, xmm0
        vfmadd213ps xmm3, xmm1, xmm5
        vcvtps2pd ymm12, xmm1
        vcmpnleps xmm1, xmm1, XMMWORD PTR [__svml_ssin_ha_data_internal+4160]
        vpslld    xmm2, xmm3, 31
        vsubps    xmm3, xmm3, xmm5
        vmovmskps edx, xmm1
        vcvtps2pd ymm3, xmm3
        vfnmadd231pd ymm12, ymm3, YMMWORD PTR [__svml_ssin_ha_data_internal+5120]
        vfnmadd132pd ymm3, ymm12, YMMWORD PTR [__svml_ssin_ha_data_internal+5184]
        vmovupd   ymm12, YMMWORD PTR [__svml_ssin_ha_data_internal+5440]
        vmulpd    ymm5, ymm3, ymm3
        vfmadd213pd ymm12, ymm5, YMMWORD PTR [__svml_ssin_ha_data_internal+5376]
        vfmadd213pd ymm12, ymm5, YMMWORD PTR [__svml_ssin_ha_data_internal+5312]
        vfmadd213pd ymm12, ymm5, YMMWORD PTR [__svml_ssin_ha_data_internal+5248]
        vmulpd    ymm5, ymm5, ymm12
        vfmadd213pd ymm5, ymm3, ymm3
        vxorps    xmm3, xmm2, xmm4
        vcvtpd2ps xmm2, ymm5
        mov       QWORD PTR [216+rsp], r13
        vxorps    xmm2, xmm2, xmm3
        test      edx, edx
        jne       _B5_3

_B5_2::

        vmovdqa   xmm0, xmm2
        mov       r13, QWORD PTR [208+rsp]
        vzeroupper
        vmovups   xmm12, XMMWORD PTR [192+rsp]
        add       rsp, 232
        ret

_B5_3::

        vmovups   XMMWORD PTR [r13], xmm0
        vmovups   XMMWORD PTR [64+r13], xmm2

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
        cmp       ebx, 4
        jl        _B5_7

_B5_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovups   xmm2, XMMWORD PTR [64+r13]
        jmp       _B5_2

_B5_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_ssin_ha_cout_rare_internal
        jmp       _B5_8
        ALIGN     16

_B5_11::

__svml_sinf4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinf4_ha_l9_B1_B3:
	DD	400385
	DD	1758236
	DD	837652
	DD	1900811

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_sinf4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinf4_ha_l9_B6_B10:
	DD	658945
	DD	287758
	DD	340999
	DD	837632
	DD	1758208
	DD	1900800

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_6
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_sinf4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinf4_ha_ex

__svml_sinf4_ha_ex	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L124::

        sub       rsp, 296
        movaps    xmm5, xmm0
        movups    XMMWORD PTR [224+rsp], xmm14
        movaps    xmm4, xmm5
        movups    XMMWORD PTR [256+rsp], xmm13
        movups    XMMWORD PTR [240+rsp], xmm12
        movups    XMMWORD PTR [192+rsp], xmm8
        movups    XMMWORD PTR [208+rsp], xmm7
        mov       QWORD PTR [272+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movups    xmm3, XMMWORD PTR [__svml_ssin_ha_data_internal+4096]
        and       r13, -64
        movups    xmm12, XMMWORD PTR [__svml_ssin_ha_data_internal+5056]
        andps     xmm4, xmm3
        mulps     xmm12, xmm4
        movaps    xmm2, xmm4
        cvtps2pd  xmm0, xmm4
        movups    xmm8, XMMWORD PTR [__svml_ssin_ha_data_internal+4544]
        andnps    xmm3, xmm5
        movhlps   xmm2, xmm4
        addps     xmm12, xmm8
        cvtps2pd  xmm1, xmm2
        cmpnleps  xmm4, XMMWORD PTR [__svml_ssin_ha_data_internal+4160]
        movaps    xmm2, xmm12
        subps     xmm12, xmm8
        movmskps  eax, xmm4
        movups    xmm14, XMMWORD PTR [__svml_ssin_ha_data_internal+5120]
        pslld     xmm2, 31
        cvtps2pd  xmm8, xmm12
        movhlps   xmm12, xmm12
        movaps    xmm13, xmm14
        cvtps2pd  xmm12, xmm12
        mulpd     xmm13, xmm8
        mulpd     xmm14, xmm12
        subpd     xmm0, xmm13
        subpd     xmm1, xmm14
        movups    xmm7, XMMWORD PTR [__svml_ssin_ha_data_internal+5184]
        pxor      xmm2, xmm3
        mulpd     xmm8, xmm7
        mulpd     xmm12, xmm7
        subpd     xmm0, xmm8
        subpd     xmm1, xmm12
        movaps    xmm12, xmm0
        movaps    xmm13, xmm1
        mulpd     xmm12, xmm0
        mulpd     xmm13, xmm1
        movups    xmm7, XMMWORD PTR [__svml_ssin_ha_data_internal+5440]
        movaps    xmm8, xmm7
        mulpd     xmm8, xmm12
        mulpd     xmm7, xmm13
        movups    xmm14, XMMWORD PTR [__svml_ssin_ha_data_internal+5376]
        addpd     xmm8, xmm14
        addpd     xmm14, xmm7
        mulpd     xmm8, xmm12
        mulpd     xmm14, xmm13
        movups    xmm7, XMMWORD PTR [__svml_ssin_ha_data_internal+5312]
        addpd     xmm8, xmm7
        addpd     xmm7, xmm14
        mulpd     xmm8, xmm12
        mulpd     xmm7, xmm13
        movups    xmm14, XMMWORD PTR [__svml_ssin_ha_data_internal+5248]
        addpd     xmm8, xmm14
        addpd     xmm14, xmm7
        mulpd     xmm12, xmm8
        mulpd     xmm13, xmm14
        mulpd     xmm12, xmm0
        mulpd     xmm13, xmm1
        addpd     xmm0, xmm12
        addpd     xmm1, xmm13
        cvtpd2ps  xmm0, xmm0
        cvtpd2ps  xmm1, xmm1
        movlhps   xmm0, xmm1
        mov       QWORD PTR [280+rsp], r13
        pxor      xmm0, xmm2
        test      eax, eax
        jne       _B6_3

_B6_2::

        movups    xmm7, XMMWORD PTR [208+rsp]
        movups    xmm8, XMMWORD PTR [192+rsp]
        movups    xmm12, XMMWORD PTR [240+rsp]
        movups    xmm13, XMMWORD PTR [256+rsp]
        movups    xmm14, XMMWORD PTR [224+rsp]
        mov       r13, QWORD PTR [272+rsp]
        add       rsp, 296
        ret

_B6_3::

        movups    XMMWORD PTR [r13], xmm5
        movups    XMMWORD PTR [64+r13], xmm0

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
        cmp       ebx, 4
        jl        _B6_7

_B6_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [64+r13]
        jmp       _B6_2

_B6_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_ssin_ha_cout_rare_internal
        jmp       _B6_8
        ALIGN     16

_B6_11::

__svml_sinf4_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinf4_ha_ex_B1_B3:
	DD	935169
	DD	2282565
	DD	882749
	DD	821301
	DD	1034284
	DD	1103907
	DD	976919
	DD	2425099

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_sinf4_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinf4_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_sinf4_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_6
	DD	imagerel _B6_11
	DD	imagerel _unwind___svml_sinf4_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_ssin_ha_cout_rare_internal

__svml_ssin_ha_cout_rare_internal	PROC

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L141::

        sub       rsp, 152
        mov       r9, rdx
        movups    XMMWORD PTR [96+rsp], xmm7
        pxor      xmm7, xmm7
        movss     xmm0, DWORD PTR [rcx]
        lea       r8, QWORD PTR [__ImageBase]
        cvtss2sd  xmm7, xmm0
        movsd     QWORD PTR [40+rsp], xmm7
        movzx     eax, WORD PTR [2+rcx]
        and       eax, 32640
        mov       dl, BYTE PTR [47+rsp]
        and       dl, 127
        movups    XMMWORD PTR [112+rsp], xmm15
        movups    XMMWORD PTR [128+rsp], xmm13
        movups    XMMWORD PTR [80+rsp], xmm6
        cmp       eax, 32640
        je        _B7_17

_B7_2::

        mov       BYTE PTR [47+rsp], dl
        movzx     eax, WORD PTR [46+rsp]
        and       eax, 32752
        shr       eax, 4
        cmp       eax, 771
        jl        _B7_13

_B7_3::

        cmp       eax, 1040
        jge       _B7_5

_B7_4::

        movsd     xmm0, QWORD PTR [_vmldSinHATab+2176]
        xor       r10d, r10d
        jmp       _B7_12

_B7_5::

        movsd     QWORD PTR [64+rsp], xmm7
        movzx     eax, BYTE PTR [71+rsp]
        mov       r10d, eax
        and       eax, 127
        and       r10d, 128
        mov       BYTE PTR [71+rsp], al
        movzx     ecx, WORD PTR [70+rsp]
        mov       eax, ecx
        and       eax, 32752
        shr       eax, 4
        shr       r10d, 7
        cmp       eax, 1053
        jge       _B7_10

_B7_6::

        movsd     xmm3, QWORD PTR [64+rsp]
        pxor      xmm2, xmm2
        movsd     xmm0, QWORD PTR [_vmlReductionTab+64]
        mulsd     xmm0, xmm3
        movsd     QWORD PTR [56+rsp], xmm0
        movzx     ecx, WORD PTR [62+rsp]
        mov       edx, DWORD PTR [60+rsp]
        shr       ecx, 4
        or        edx, -1048576
        mov       r11d, DWORD PTR [56+rsp]
        neg       ecx
        shl       edx, 11
        add       ecx, 30
        shr       r11d, 21
        or        edx, r11d
        shr       edx, cl
        mov       r11d, edx
        and       r11d, 1
        add       r11d, edx
        cvtsi2sd  xmm2, r11d
        cmp       eax, 1046
        jge       _B7_8

_B7_7::

        movsd     xmm1, QWORD PTR [_vmlReductionTab+80]
        mulsd     xmm1, xmm2
        movsd     xmm4, QWORD PTR [_vmlReductionTab+88]
        subsd     xmm3, xmm1
        mulsd     xmm4, xmm2
        movsd     QWORD PTR [48+rsp], xmm3
        movsd     xmm1, QWORD PTR [48+rsp]
        movsd     xmm0, QWORD PTR [48+rsp]
        movsd     xmm7, QWORD PTR [_vmlReductionTab+96]
        subsd     xmm0, xmm4
        mulsd     xmm7, xmm2
        movsd     QWORD PTR [48+rsp], xmm0
        movsd     xmm3, QWORD PTR [48+rsp]
        movsd     xmm13, QWORD PTR [48+rsp]
        subsd     xmm1, xmm3
        movsd     xmm5, QWORD PTR [48+rsp]
        subsd     xmm1, xmm4
        subsd     xmm5, xmm7
        movsd     QWORD PTR [48+rsp], xmm5
        movsd     xmm6, QWORD PTR [48+rsp]
        movsd     xmm15, QWORD PTR [_vmlReductionTab+104]
        subsd     xmm13, xmm6
        mulsd     xmm2, xmm15
        subsd     xmm13, xmm7
        addsd     xmm1, xmm13
        subsd     xmm1, xmm2
        jmp       _B7_9

_B7_8::

        movsd     xmm1, QWORD PTR [_vmlReductionTab+112]
        mulsd     xmm1, xmm2
        movsd     xmm5, QWORD PTR [_vmlReductionTab+120]
        subsd     xmm3, xmm1
        mulsd     xmm5, xmm2
        movsd     QWORD PTR [48+rsp], xmm3
        movsd     xmm0, QWORD PTR [48+rsp]
        movsd     xmm3, QWORD PTR [48+rsp]
        movsd     xmm13, QWORD PTR [_vmlReductionTab+128]
        subsd     xmm3, xmm5
        mulsd     xmm13, xmm2
        movsd     QWORD PTR [48+rsp], xmm3
        movsd     xmm4, QWORD PTR [48+rsp]
        movsd     xmm15, QWORD PTR [48+rsp]
        subsd     xmm0, xmm4
        movsd     xmm6, QWORD PTR [48+rsp]
        subsd     xmm0, xmm5
        subsd     xmm6, xmm13
        movsd     QWORD PTR [48+rsp], xmm6
        movsd     xmm7, QWORD PTR [48+rsp]
        movsd     xmm1, QWORD PTR [48+rsp]
        subsd     xmm15, xmm7
        movsd     xmm3, QWORD PTR [48+rsp]
        subsd     xmm15, xmm13
        movsd     xmm6, QWORD PTR [_vmlReductionTab+136]
        addsd     xmm0, xmm15
        mulsd     xmm6, xmm2
        addsd     xmm3, xmm0
        movsd     QWORD PTR [48+rsp], xmm3
        movsd     xmm4, QWORD PTR [48+rsp]
        movsd     xmm7, QWORD PTR [48+rsp]
        subsd     xmm1, xmm4
        movsd     xmm13, QWORD PTR [_vmlReductionTab+144]
        addsd     xmm1, xmm0
        mulsd     xmm2, xmm13
        movsd     xmm0, QWORD PTR [48+rsp]
        subsd     xmm0, xmm6
        movsd     QWORD PTR [48+rsp], xmm0
        movsd     xmm5, QWORD PTR [48+rsp]
        subsd     xmm7, xmm5
        subsd     xmm7, xmm6
        addsd     xmm1, xmm7
        subsd     xmm1, xmm2

_B7_9::

        mov       r10d, r10d
        inc       edx
        movsd     xmm0, QWORD PTR [48+rsp]
        shr       edx, 1
        movsd     xmm2, QWORD PTR [imagerel(_vmlReductionTab)+16+r8+r10*8]
        add       r10d, r10d
        mulsd     xmm0, xmm2
        neg       r10d
        mulsd     xmm2, xmm1
        movsd     QWORD PTR [48+rsp], xmm0
        inc       r10d
        movsd     xmm0, QWORD PTR [48+rsp]
        movaps    xmm7, xmm0
        imul      r10d, edx
        addsd     xmm7, xmm2
        subsd     xmm0, xmm7
        addsd     xmm0, xmm2
        jmp       _B7_12

_B7_10::

        and       ecx, -32753
        lea       edx, DWORD PTR [-200+rax]
        and       edx, 2047
        lea       r11d, DWORD PTR [-1052+rax]
        shl       edx, 4
        or        ecx, edx
        imul      edx, r11d, 83886
        mov       WORD PTR [70+rsp], cx
        movsd     xmm1, QWORD PTR [64+rsp]
        mov       ecx, DWORD PTR [64+rsp]
        movsd     QWORD PTR [40+rsp], xmm1
        and       ecx, -134217728
        sar       edx, 21
        mov       DWORD PTR [40+rsp], ecx
        movsxd    rdx, edx
        subsd     xmm1, QWORD PTR [40+rsp]
        imul      ecx, edx, -25
        movsd     xmm0, QWORD PTR [imagerel(_vmlReductionTab)+160+r8+rdx*8]
        mulsd     xmm0, xmm1
        lea       ecx, DWORD PTR [-1052+rcx+rax]
        movsd     QWORD PTR [48+rsp], xmm0
        neg       ecx
        movsd     xmm0, QWORD PTR [40+rsp]
        add       ecx, 38
        movaps    xmm2, xmm0
        mulsd     xmm2, QWORD PTR [imagerel(_vmlReductionTab)+168+r8+rdx*8]
        movsd     xmm7, QWORD PTR [48+rsp]
        movsd     xmm13, QWORD PTR [48+rsp]
        mov       rax, QWORD PTR [_vmlReductionTab+152]
        addsd     xmm13, xmm2
        movsd     QWORD PTR [48+rsp], xmm13
        movsd     xmm15, QWORD PTR [48+rsp]
        movsd     xmm3, QWORD PTR [48+rsp]
        subsd     xmm7, xmm15
        shl       rax, cl
        addsd     xmm7, xmm2
        movsd     xmm4, QWORD PTR [48+rsp]
        addsd     xmm3, xmm7
        movd      r11, xmm3
        movsd     xmm2, QWORD PTR [imagerel(_vmlReductionTab)+168+r8+rdx*8]
        movsd     xmm3, QWORD PTR [imagerel(_vmlReductionTab)+176+r8+rdx*8]
        and       r11, rax
        mov       QWORD PTR [56+rsp], r11
        mulsd     xmm2, xmm1
        subsd     xmm4, QWORD PTR [56+rsp]
        mulsd     xmm3, xmm1
        movsd     QWORD PTR [48+rsp], xmm4
        movaps    xmm4, xmm0
        movsd     xmm6, QWORD PTR [48+rsp]
        movsd     xmm5, QWORD PTR [48+rsp]
        mulsd     xmm4, QWORD PTR [imagerel(_vmlReductionTab)+184+r8+rdx*8]
        addsd     xmm5, xmm2
        movsd     QWORD PTR [48+rsp], xmm5
        movsd     xmm13, QWORD PTR [48+rsp]
        movsd     xmm5, QWORD PTR [48+rsp]
        subsd     xmm6, xmm13
        addsd     xmm6, xmm2
        movaps    xmm2, xmm0
        addsd     xmm6, xmm7
        mulsd     xmm2, QWORD PTR [imagerel(_vmlReductionTab)+176+r8+rdx*8]
        movsd     xmm7, QWORD PTR [48+rsp]
        addsd     xmm7, xmm2
        movsd     QWORD PTR [48+rsp], xmm7
        movsd     xmm15, QWORD PTR [48+rsp]
        subsd     xmm5, xmm15
        addsd     xmm5, xmm2
        addsd     xmm5, xmm6
        movsd     xmm6, QWORD PTR [48+rsp]
        movsd     xmm2, QWORD PTR [48+rsp]
        addsd     xmm6, xmm5
        addsd     xmm6, xmm3
        addsd     xmm6, xmm4
        movsd     QWORD PTR [56+rsp], xmm6
        movzx     ecx, WORD PTR [62+rsp]
        shr       ecx, 4
        movd      rax, xmm6
        neg       ecx
        add       ecx, 51
        shr       rax, cl
        mov       r11d, eax
        shl       rax, cl
        mov       QWORD PTR [56+rsp], rax
        mov       eax, r11d
        and       eax, 1
        inc       r11d
        shr       r11d, 1
        subsd     xmm2, QWORD PTR [56+rsp]
        movsd     QWORD PTR [48+rsp], xmm2
        movsd     xmm2, QWORD PTR [48+rsp]
        movsd     xmm7, QWORD PTR [48+rsp]
        addsd     xmm7, xmm5
        movsd     QWORD PTR [48+rsp], xmm7
        movsd     xmm6, QWORD PTR [48+rsp]
        subsd     xmm2, xmm6
        addsd     xmm2, xmm5
        movsd     xmm5, QWORD PTR [48+rsp]
        addsd     xmm5, QWORD PTR [imagerel(_vmlReductionTab)+r8+rax*8]
        movsd     QWORD PTR [48+rsp], xmm5
        movsd     xmm15, QWORD PTR [48+rsp]
        movsd     xmm13, QWORD PTR [48+rsp]
        addsd     xmm13, xmm3
        movsd     QWORD PTR [48+rsp], xmm13
        movsd     xmm5, QWORD PTR [48+rsp]
        movsd     xmm13, QWORD PTR [48+rsp]
        subsd     xmm15, xmm5
        movaps    xmm5, xmm0
        addsd     xmm15, xmm3
        mulsd     xmm5, QWORD PTR [imagerel(_vmlReductionTab)+192+r8+rdx*8]
        addsd     xmm15, xmm2
        movsd     xmm3, QWORD PTR [48+rsp]
        addsd     xmm3, xmm4
        movsd     QWORD PTR [48+rsp], xmm3
        movsd     xmm2, QWORD PTR [48+rsp]
        movsd     xmm7, QWORD PTR [48+rsp]
        subsd     xmm13, xmm2
        movsd     xmm2, QWORD PTR [imagerel(_vmlReductionTab)+184+r8+rdx*8]
        addsd     xmm13, xmm4
        mulsd     xmm2, xmm1
        addsd     xmm13, xmm15
        movsd     xmm4, QWORD PTR [48+rsp]
        addsd     xmm4, xmm2
        movsd     QWORD PTR [48+rsp], xmm4
        movsd     xmm15, QWORD PTR [48+rsp]
        movsd     xmm6, QWORD PTR [48+rsp]
        subsd     xmm7, xmm15
        movsd     xmm3, QWORD PTR [48+rsp]
        addsd     xmm7, xmm2
        addsd     xmm3, xmm5
        addsd     xmm7, xmm13
        movsd     QWORD PTR [48+rsp], xmm3
        movaps    xmm3, xmm0
        movsd     xmm4, QWORD PTR [48+rsp]
        movsd     xmm2, QWORD PTR [imagerel(_vmlReductionTab)+192+r8+rdx*8]
        subsd     xmm6, xmm4
        mulsd     xmm2, xmm1
        mulsd     xmm3, QWORD PTR [imagerel(_vmlReductionTab)+208+r8+rdx*8]
        addsd     xmm6, xmm5
        movsd     xmm5, QWORD PTR [48+rsp]
        addsd     xmm6, xmm7
        movsd     xmm7, QWORD PTR [48+rsp]
        addsd     xmm7, xmm2
        movsd     QWORD PTR [48+rsp], xmm7
        movsd     xmm13, QWORD PTR [48+rsp]
        movsd     xmm4, QWORD PTR [48+rsp]
        subsd     xmm5, xmm13
        movsd     xmm7, QWORD PTR [imagerel(_vmlReductionTab)+200+r8+rdx*8]
        addsd     xmm5, xmm2
        mulsd     xmm7, xmm1
        addsd     xmm5, xmm6
        addsd     xmm7, xmm3
        movaps    xmm2, xmm0
        mulsd     xmm2, QWORD PTR [imagerel(_vmlReductionTab)+200+r8+rdx*8]
        movsd     xmm6, QWORD PTR [48+rsp]
        addsd     xmm6, xmm2
        movsd     QWORD PTR [48+rsp], xmm6
        movsd     xmm15, QWORD PTR [48+rsp]
        movsd     xmm3, QWORD PTR [48+rsp]
        subsd     xmm4, xmm15
        addsd     xmm4, xmm2
        addsd     xmm4, xmm5
        movsd     xmm5, QWORD PTR [48+rsp]
        addsd     xmm5, xmm7
        movsd     QWORD PTR [48+rsp], xmm5
        movsd     xmm6, QWORD PTR [48+rsp]
        movsd     xmm5, QWORD PTR [imagerel(_vmlReductionTab)+208+r8+rdx*8]
        subsd     xmm3, xmm6
        mulsd     xmm5, xmm1
        addsd     xmm3, xmm7
        movsd     xmm2, QWORD PTR [48+rsp]
        addsd     xmm3, xmm4
        movaps    xmm4, xmm0
        mulsd     xmm4, QWORD PTR [imagerel(_vmlReductionTab)+216+r8+rdx*8]
        mulsd     xmm0, QWORD PTR [imagerel(_vmlReductionTab)+224+r8+rdx*8]
        addsd     xmm5, xmm4
        movsd     xmm13, QWORD PTR [48+rsp]
        addsd     xmm13, xmm5
        movsd     QWORD PTR [48+rsp], xmm13
        movsd     xmm15, QWORD PTR [48+rsp]
        movsd     xmm6, QWORD PTR [48+rsp]
        subsd     xmm2, xmm15
        addsd     xmm2, xmm5
        addsd     xmm2, xmm3
        movsd     xmm3, QWORD PTR [imagerel(_vmlReductionTab)+216+r8+rdx*8]
        mulsd     xmm1, xmm3
        addsd     xmm1, xmm0
        movsd     xmm0, QWORD PTR [48+rsp]
        addsd     xmm0, xmm1
        movsd     QWORD PTR [48+rsp], xmm0
        movsd     xmm4, QWORD PTR [48+rsp]
        mulsd     xmm4, QWORD PTR [_vmlReductionTab+32]
        movsd     xmm3, QWORD PTR [48+rsp]
        addsd     xmm3, xmm4
        movsd     QWORD PTR [32+rsp], xmm3
        movsd     xmm5, QWORD PTR [32+rsp]
        movsd     xmm3, QWORD PTR [_vmlReductionTab+48]
        subsd     xmm5, xmm4
        movsd     QWORD PTR [48+rsp], xmm5
        movsd     xmm0, QWORD PTR [48+rsp]
        subsd     xmm6, xmm0
        addsd     xmm6, xmm1
        movsd     xmm1, QWORD PTR [48+rsp]
        movsd     xmm0, QWORD PTR [48+rsp]
        addsd     xmm6, xmm2
        mulsd     xmm0, QWORD PTR [_vmlReductionTab+48]
        mulsd     xmm3, xmm6
        addsd     xmm6, xmm1
        mulsd     xmm6, QWORD PTR [_vmlReductionTab+56]
        movsd     QWORD PTR [48+rsp], xmm0
        addsd     xmm3, xmm6
        movsd     xmm0, QWORD PTR [48+rsp]
        movsd     xmm2, QWORD PTR [48+rsp]
        addsd     xmm2, xmm3
        movsd     QWORD PTR [48+rsp], xmm2
        movsd     xmm1, QWORD PTR [48+rsp]
        movsd     xmm7, QWORD PTR [48+rsp]
        subsd     xmm0, xmm1
        mulsd     xmm7, QWORD PTR [imagerel(_vmlReductionTab)+16+r8+r10*8]
        addsd     xmm0, xmm3
        mulsd     xmm0, QWORD PTR [imagerel(_vmlReductionTab)+16+r8+r10*8]
        add       r10d, r10d
        neg       r10d
        inc       r10d
        imul      r10d, r11d

_B7_12::

        movsd     xmm13, QWORD PTR [_vmldSinHATab+2048]
        xor       eax, eax
        mulsd     xmm13, xmm7
        movsd     xmm15, QWORD PTR [_vmldSinHATab+2064]
        movsd     xmm1, QWORD PTR [_vmldSinHATab+2072]
        shl       r10d, 4
        addsd     xmm13, QWORD PTR [_vmldSinHATab+2056]
        movsd     QWORD PTR [72+rsp], xmm13
        movsd     xmm2, QWORD PTR [72+rsp]
        add       r10d, DWORD PTR [72+rsp]
        and       r10d, 63
        subsd     xmm2, QWORD PTR [_vmldSinHATab+2056]
        mulsd     xmm15, xmm2
        mulsd     xmm1, xmm2
        mulsd     xmm2, QWORD PTR [_vmldSinHATab+2080]
        subsd     xmm7, xmm15
        movaps    xmm6, xmm7
        shl       r10d, 2
        subsd     xmm6, xmm1
        movaps    xmm13, xmm6
        subsd     xmm7, xmm6
        mulsd     xmm13, xmm6
        subsd     xmm7, xmm1
        movsd     xmm1, QWORD PTR [_vmldSinHATab+2112]
        subsd     xmm7, xmm2
        mulsd     xmm1, xmm13
        addsd     xmm7, xmm0
        addsd     xmm1, QWORD PTR [_vmldSinHATab+2104]
        movsd     xmm2, QWORD PTR [_vmldSinHATab+2144]
        mulsd     xmm1, xmm13
        mulsd     xmm2, xmm13
        addsd     xmm1, QWORD PTR [_vmldSinHATab+2096]
        addsd     xmm2, QWORD PTR [_vmldSinHATab+2136]
        mulsd     xmm1, xmm13
        mulsd     xmm2, xmm13
        addsd     xmm1, QWORD PTR [_vmldSinHATab+2088]
        addsd     xmm2, QWORD PTR [_vmldSinHATab+2128]
        mulsd     xmm1, xmm13
        mulsd     xmm2, xmm13
        mulsd     xmm1, xmm6
        addsd     xmm2, QWORD PTR [_vmldSinHATab+2120]
        movsd     xmm5, QWORD PTR [imagerel(_vmldSinHATab)+r8+r10*8]
        movsd     xmm0, QWORD PTR [imagerel(_vmldSinHATab)+24+r8+r10*8]
        movaps    xmm3, xmm5
        movsd     xmm4, QWORD PTR [imagerel(_vmldSinHATab)+8+r8+r10*8]
        addsd     xmm3, xmm0
        mulsd     xmm0, xmm6
        mulsd     xmm5, xmm6
        mulsd     xmm6, xmm4
        mulsd     xmm1, xmm3
        mulsd     xmm2, xmm13
        subsd     xmm3, xmm6
        mulsd     xmm2, xmm4
        mulsd     xmm7, xmm3
        movaps    xmm15, xmm4
        addsd     xmm7, QWORD PTR [imagerel(_vmldSinHATab)+16+r8+r10*8]
        addsd     xmm15, xmm0
        movaps    xmm13, xmm15
        subsd     xmm4, xmm15
        addsd     xmm13, xmm5
        addsd     xmm4, xmm0
        subsd     xmm15, xmm13
        addsd     xmm7, xmm4
        addsd     xmm5, xmm15
        movups    xmm6, XMMWORD PTR [80+rsp]
        addsd     xmm7, xmm5
        movups    xmm15, XMMWORD PTR [112+rsp]
        addsd     xmm7, xmm1
        addsd     xmm7, xmm2
        addsd     xmm13, xmm7
        movups    xmm7, XMMWORD PTR [96+rsp]
        cvtsd2ss  xmm13, xmm13
        movss     DWORD PTR [r9], xmm13
        movups    xmm13, XMMWORD PTR [128+rsp]
        add       rsp, 152
        ret

_B7_13::

        ucomisd   xmm7, QWORD PTR [_vmldSinHATab+2176]
        jp        _B7_14
        je        _B7_16

_B7_14::

        movsd     xmm0, QWORD PTR [_vmldSinHATab+2152]
        mulsd     xmm0, xmm7
        subsd     xmm0, xmm7
        cvtsd2ss  xmm0, xmm0
        cvtss2sd  xmm0, xmm0
        mulsd     xmm0, QWORD PTR [_vmldSinHATab+2160]
        cvtsd2ss  xmm0, xmm0

_B7_16::

        xor       eax, eax
        movups    xmm7, XMMWORD PTR [96+rsp]
        movss     DWORD PTR [r9], xmm0
        add       rsp, 152
        ret

_B7_17::

        cmp       DWORD PTR [40+rsp], 0
        jne       _B7_20

_B7_18::

        mov       BYTE PTR [47+rsp], dl
        cmp       DWORD PTR [44+rsp], 2146435072
        jne       _B7_20

_B7_19::

        mulsd     xmm7, QWORD PTR [_vmldSinHATab+2176]
        mov       eax, 1
        cvtsd2ss  xmm7, xmm7
        movss     DWORD PTR [r9], xmm7
        movups    xmm7, XMMWORD PTR [96+rsp]
        add       rsp, 152
        ret

_B7_20::

        mulss     xmm0, xmm0
        xor       eax, eax
        movups    xmm7, XMMWORD PTR [96+rsp]
        movss     DWORD PTR [r9], xmm0
        add       rsp, 152
        ret
        ALIGN     16

_B7_21::

__svml_ssin_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_ssin_ha_cout_rare_internal_B1_B20:
	DD	675841
	DD	354384
	DD	579659
	DD	522306
	DD	423955
	DD	1245451

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_21
	DD	imagerel _unwind___svml_ssin_ha_cout_rare_internal_B1_B20

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_ssin_ha_data_internal
__svml_ssin_ha_data_internal	DD	0
	DD	0
	DD	0
	DD	1065353216
	DD	3114133471
	DD	1019808432
	DD	2953169304
	DD	1065353216
	DD	3130909128
	DD	1028193072
	DD	2968461951
	DD	1065353216
	DD	3140588184
	DD	1033283845
	DD	2975014497
	DD	1065353216
	DD	3147680113
	DD	1036565814
	DD	2960495349
	DD	1065353216
	DD	3153489468
	DD	1039839859
	DD	2970970319
	DD	1065353216
	DD	3157349634
	DD	1041645699
	DD	837346836
	DD	1065353216
	DD	3161536011
	DD	1043271842
	DD	823224313
	DD	1065353216
	DD	3164432432
	DD	1044891074
	DD	2967836285
	DD	1065353216
	DD	3167161428
	DD	1046502419
	DD	833086710
	DD	1065353216
	DD	3170205956
	DD	1048104908
	DD	2971391005
	DD	1065353216
	DD	3172229004
	DD	1049136787
	DD	824999326
	DD	1065353216
	DD	3174063957
	DD	1049927729
	DD	846027248
	DD	1065353216
	DD	3176053642
	DD	1050712805
	DD	2990442912
	DD	1065353216
	DD	3178196862
	DD	1051491540
	DD	2988789250
	DD	1065353216
	DD	3179887378
	DD	1052263466
	DD	2993707942
	DD	1065353216
	DD	3181110540
	DD	1053028117
	DD	836097324
	DD	1065353216
	DD	3182408396
	DD	1053785034
	DD	829045603
	DD	1065353216
	DD	3183780163
	DD	1054533760
	DD	840832460
	DD	1065353216
	DD	3185225016
	DD	1055273845
	DD	2983839604
	DD	1065353216
	DD	3186742084
	DD	1056004842
	DD	2986287417
	DD	1065353216
	DD	3188000746
	DD	1056726311
	DD	2978016425
	DD	1065353216
	DD	3188830103
	DD	1057201213
	DD	2992349186
	DD	1065353216
	DD	3189694133
	DD	1057551771
	DD	2998815566
	DD	1065353216
	DD	3190592315
	DD	1057896922
	DD	2991207143
	DD	1065353216
	DD	3191524108
	DD	1058236458
	DD	852349230
	DD	1065353216
	DD	3192488951
	DD	1058570176
	DD	2982650867
	DD	1065353216
	DD	3193486263
	DD	1058897873
	DD	848430348
	DD	1065353216
	DD	3194515443
	DD	1059219353
	DD	841032635
	DD	1065353216
	DD	3195575871
	DD	1059534422
	DD	2986574659
	DD	1065353216
	DD	3196363278
	DD	1059842890
	DD	2998350134
	DD	1065353216
	DD	3196923773
	DD	1060144571
	DD	2997759282
	DD	1065353216
	DD	3197498906
	DD	1060439283
	DD	844097402
	DD	1065353216
	DD	1044518635
	DD	1060726850
	DD	2994798599
	DD	1056964608
	DD	1043311911
	DD	1061007097
	DD	832220140
	DD	1056964608
	DD	1042078039
	DD	1061279856
	DD	851442039
	DD	1056964608
	DD	1040817765
	DD	1061544963
	DD	850481524
	DD	1056964608
	DD	1038876298
	DD	1061802258
	DD	848897600
	DD	1056964608
	DD	1036254719
	DD	1062051586
	DD	847147240
	DD	1056964608
	DD	1033584979
	DD	1062292797
	DD	806113028
	DD	1056964608
	DD	1029938589
	DD	1062525745
	DD	848357914
	DD	1056964608
	DD	1024416170
	DD	1062750291
	DD	2994560960
	DD	1056964608
	DD	1013387058
	DD	1062966298
	DD	841166280
	DD	1056964608
	DD	3152590408
	DD	1063173637
	DD	851900755
	DD	1056964608
	DD	3169472868
	DD	1063372184
	DD	3001545765
	DD	1056964608
	DD	3176031322
	DD	1063561817
	DD	823789818
	DD	1056964608
	DD	3180617215
	DD	1063742424
	DD	2998678409
	DD	1056964608
	DD	3183612120
	DD	1063913895
	DD	3001754476
	DD	1056964608
	DD	3186639787
	DD	1064076126
	DD	854796500
	DD	1056964608
	DD	3188684717
	DD	1064229022
	DD	2995991516
	DD	1056964608
	DD	1035072335
	DD	1064372488
	DD	840880349
	DD	1048576000
	DD	1031957395
	DD	1064506439
	DD	851742225
	DD	1048576000
	DD	1025835404
	DD	1064630795
	DD	2996018466
	DD	1048576000
	DD	1015605553
	DD	1064745479
	DD	846006572
	DD	1048576000
	DD	3152414341
	DD	1064850424
	DD	2987244005
	DD	1048576000
	DD	3170705253
	DD	1064945565
	DD	851856985
	DD	1048576000
	DD	3177244920
	DD	1065030846
	DD	855602635
	DD	1048576000
	DD	1027359369
	DD	1065106216
	DD	2989610635
	DD	1040187392
	DD	1018299420
	DD	1065171628
	DD	2969000681
	DD	1040187392
	DD	3140071849
	DD	1065227044
	DD	3002197507
	DD	1040187392
	DD	3168602920
	DD	1065272429
	DD	838093129
	DD	1040187392
	DD	1010124837
	DD	1065307757
	DD	852498564
	DD	1031798784
	DD	3160150850
	DD	1065333007
	DD	836655967
	DD	1031798784
	DD	3151746369
	DD	1065348163
	DD	814009613
	DD	1023410176
	DD	0
	DD	1065353216
	DD	0
	DD	0
	DD	1004262721
	DD	1065348163
	DD	814009613
	DD	3170893824
	DD	1012667202
	DD	1065333007
	DD	836655967
	DD	3179282432
	DD	3157608485
	DD	1065307757
	DD	852498564
	DD	3179282432
	DD	1021119272
	DD	1065272429
	DD	838093129
	DD	3187671040
	DD	992588201
	DD	1065227044
	DD	3002197507
	DD	3187671040
	DD	3165783068
	DD	1065171628
	DD	2969000681
	DD	3187671040
	DD	3174843017
	DD	1065106216
	DD	2989610635
	DD	3187671040
	DD	1029761272
	DD	1065030846
	DD	855602635
	DD	3196059648
	DD	1023221605
	DD	1064945565
	DD	851856985
	DD	3196059648
	DD	1004930693
	DD	1064850424
	DD	2987244005
	DD	3196059648
	DD	3163089201
	DD	1064745479
	DD	846006572
	DD	3196059648
	DD	3173319052
	DD	1064630795
	DD	2996018466
	DD	3196059648
	DD	3179441043
	DD	1064506439
	DD	851742225
	DD	3196059648
	DD	3182555983
	DD	1064372488
	DD	840880349
	DD	3196059648
	DD	1041201069
	DD	1064229022
	DD	2995991516
	DD	3204448256
	DD	1039156139
	DD	1064076126
	DD	854796500
	DD	3204448256
	DD	1036128472
	DD	1063913895
	DD	3001754476
	DD	3204448256
	DD	1033133567
	DD	1063742424
	DD	2998678409
	DD	3204448256
	DD	1028547674
	DD	1063561817
	DD	823789818
	DD	3204448256
	DD	1021989220
	DD	1063372184
	DD	3001545765
	DD	3204448256
	DD	1005106760
	DD	1063173637
	DD	851900755
	DD	3204448256
	DD	3160870706
	DD	1062966298
	DD	841166280
	DD	3204448256
	DD	3171899818
	DD	1062750291
	DD	2994560960
	DD	3204448256
	DD	3177422237
	DD	1062525745
	DD	848357914
	DD	3204448256
	DD	3181068627
	DD	1062292797
	DD	806113028
	DD	3204448256
	DD	3183738367
	DD	1062051586
	DD	847147240
	DD	3204448256
	DD	3186359946
	DD	1061802258
	DD	848897600
	DD	3204448256
	DD	3188301413
	DD	1061544963
	DD	850481524
	DD	3204448256
	DD	3189561687
	DD	1061279856
	DD	851442039
	DD	3204448256
	DD	3190795559
	DD	1061007097
	DD	832220140
	DD	3204448256
	DD	3192002283
	DD	1060726850
	DD	2994798599
	DD	3204448256
	DD	1050015258
	DD	1060439283
	DD	844097402
	DD	3212836864
	DD	1049440125
	DD	1060144571
	DD	2997759282
	DD	3212836864
	DD	1048879630
	DD	1059842890
	DD	2998350134
	DD	3212836864
	DD	1048092223
	DD	1059534422
	DD	2986574659
	DD	3212836864
	DD	1047031795
	DD	1059219353
	DD	841032635
	DD	3212836864
	DD	1046002615
	DD	1058897873
	DD	848430348
	DD	3212836864
	DD	1045005303
	DD	1058570176
	DD	2982650867
	DD	3212836864
	DD	1044040460
	DD	1058236458
	DD	852349230
	DD	3212836864
	DD	1043108667
	DD	1057896922
	DD	2991207143
	DD	3212836864
	DD	1042210485
	DD	1057551771
	DD	2998815566
	DD	3212836864
	DD	1041346455
	DD	1057201213
	DD	2992349186
	DD	3212836864
	DD	1040517098
	DD	1056726311
	DD	2978016425
	DD	3212836864
	DD	1039258436
	DD	1056004842
	DD	2986287417
	DD	3212836864
	DD	1037741368
	DD	1055273845
	DD	2983839604
	DD	3212836864
	DD	1036296515
	DD	1054533760
	DD	840832460
	DD	3212836864
	DD	1034924748
	DD	1053785034
	DD	829045603
	DD	3212836864
	DD	1033626892
	DD	1053028117
	DD	836097324
	DD	3212836864
	DD	1032403730
	DD	1052263466
	DD	2993707942
	DD	3212836864
	DD	1030713214
	DD	1051491540
	DD	2988789250
	DD	3212836864
	DD	1028569994
	DD	1050712805
	DD	2990442912
	DD	3212836864
	DD	1026580309
	DD	1049927729
	DD	846027248
	DD	3212836864
	DD	1024745356
	DD	1049136787
	DD	824999326
	DD	3212836864
	DD	1022722308
	DD	1048104908
	DD	2971391005
	DD	3212836864
	DD	1019677780
	DD	1046502419
	DD	833086710
	DD	3212836864
	DD	1016948784
	DD	1044891074
	DD	2967836285
	DD	3212836864
	DD	1014052363
	DD	1043271842
	DD	823224313
	DD	3212836864
	DD	1009865986
	DD	1041645699
	DD	837346836
	DD	3212836864
	DD	1006005820
	DD	1039839859
	DD	2970970319
	DD	3212836864
	DD	1000196465
	DD	1036565814
	DD	2960495349
	DD	3212836864
	DD	993104536
	DD	1033283845
	DD	2975014497
	DD	3212836864
	DD	983425480
	DD	1028193072
	DD	2968461951
	DD	3212836864
	DD	966649823
	DD	1019808432
	DD	2953169304
	DD	3212836864
	DD	0
	DD	0
	DD	0
	DD	3212836864
	DD	966649823
	DD	3167292080
	DD	805685656
	DD	3212836864
	DD	983425480
	DD	3175676720
	DD	820978303
	DD	3212836864
	DD	993104536
	DD	3180767493
	DD	827530849
	DD	3212836864
	DD	1000196465
	DD	3184049462
	DD	813011701
	DD	3212836864
	DD	1006005820
	DD	3187323507
	DD	823486671
	DD	3212836864
	DD	1009865986
	DD	3189129347
	DD	2984830484
	DD	3212836864
	DD	1014052363
	DD	3190755490
	DD	2970707961
	DD	3212836864
	DD	1016948784
	DD	3192374722
	DD	820352637
	DD	3212836864
	DD	1019677780
	DD	3193986067
	DD	2980570358
	DD	3212836864
	DD	1022722308
	DD	3195588556
	DD	823907357
	DD	3212836864
	DD	1024745356
	DD	3196620435
	DD	2972482974
	DD	3212836864
	DD	1026580309
	DD	3197411377
	DD	2993510896
	DD	3212836864
	DD	1028569994
	DD	3198196453
	DD	842959264
	DD	3212836864
	DD	1030713214
	DD	3198975188
	DD	841305602
	DD	3212836864
	DD	1032403730
	DD	3199747114
	DD	846224294
	DD	3212836864
	DD	1033626892
	DD	3200511765
	DD	2983580972
	DD	3212836864
	DD	1034924748
	DD	3201268682
	DD	2976529251
	DD	3212836864
	DD	1036296515
	DD	3202017408
	DD	2988316108
	DD	3212836864
	DD	1037741368
	DD	3202757493
	DD	836355956
	DD	3212836864
	DD	1039258436
	DD	3203488490
	DD	838803769
	DD	3212836864
	DD	1040517098
	DD	3204209959
	DD	830532777
	DD	3212836864
	DD	1041346455
	DD	3204684861
	DD	844865538
	DD	3212836864
	DD	1042210485
	DD	3205035419
	DD	851331918
	DD	3212836864
	DD	1043108667
	DD	3205380570
	DD	843723495
	DD	3212836864
	DD	1044040460
	DD	3205720106
	DD	2999832878
	DD	3212836864
	DD	1045005303
	DD	3206053824
	DD	835167219
	DD	3212836864
	DD	1046002615
	DD	3206381521
	DD	2995913996
	DD	3212836864
	DD	1047031795
	DD	3206703001
	DD	2988516283
	DD	3212836864
	DD	1048092223
	DD	3207018070
	DD	839091011
	DD	3212836864
	DD	1048879630
	DD	3207326538
	DD	850866486
	DD	3212836864
	DD	1049440125
	DD	3207628219
	DD	850275634
	DD	3212836864
	DD	1050015258
	DD	3207922931
	DD	2991581050
	DD	3212836864
	DD	3192002283
	DD	3208210498
	DD	847314951
	DD	3204448256
	DD	3190795559
	DD	3208490745
	DD	2979703788
	DD	3204448256
	DD	3189561687
	DD	3208763504
	DD	2998925687
	DD	3204448256
	DD	3188301413
	DD	3209028611
	DD	2997965172
	DD	3204448256
	DD	3186359946
	DD	3209285906
	DD	2996381248
	DD	3204448256
	DD	3183738367
	DD	3209535234
	DD	2994630888
	DD	3204448256
	DD	3181068627
	DD	3209776445
	DD	2953596676
	DD	3204448256
	DD	3177422237
	DD	3210009393
	DD	2995841562
	DD	3204448256
	DD	3171899818
	DD	3210233939
	DD	847077312
	DD	3204448256
	DD	3160870706
	DD	3210449946
	DD	2988649928
	DD	3204448256
	DD	1005106760
	DD	3210657285
	DD	2999384403
	DD	3204448256
	DD	1021989220
	DD	3210855832
	DD	854062117
	DD	3204448256
	DD	1028547674
	DD	3211045465
	DD	2971273466
	DD	3204448256
	DD	1033133567
	DD	3211226072
	DD	851194761
	DD	3204448256
	DD	1036128472
	DD	3211397543
	DD	854270828
	DD	3204448256
	DD	1039156139
	DD	3211559774
	DD	3002280148
	DD	3204448256
	DD	1041201069
	DD	3211712670
	DD	848507868
	DD	3204448256
	DD	3182555983
	DD	3211856136
	DD	2988363997
	DD	3196059648
	DD	3179441043
	DD	3211990087
	DD	2999225873
	DD	3196059648
	DD	3173319052
	DD	3212114443
	DD	848534818
	DD	3196059648
	DD	3163089201
	DD	3212229127
	DD	2993490220
	DD	3196059648
	DD	1004930693
	DD	3212334072
	DD	839760357
	DD	3196059648
	DD	1023221605
	DD	3212429213
	DD	2999340633
	DD	3196059648
	DD	1029761272
	DD	3212514494
	DD	3003086283
	DD	3196059648
	DD	3174843017
	DD	3212589864
	DD	842126987
	DD	3187671040
	DD	3165783068
	DD	3212655276
	DD	821517033
	DD	3187671040
	DD	992588201
	DD	3212710692
	DD	854713859
	DD	3187671040
	DD	1021119272
	DD	3212756077
	DD	2985576777
	DD	3187671040
	DD	3157608485
	DD	3212791405
	DD	2999982212
	DD	3179282432
	DD	1012667202
	DD	3212816655
	DD	2984139615
	DD	3179282432
	DD	1004262721
	DD	3212831811
	DD	2961493261
	DD	3170893824
	DD	0
	DD	3212836864
	DD	0
	DD	0
	DD	3151746369
	DD	3212831811
	DD	2961493261
	DD	1023410176
	DD	3160150850
	DD	3212816655
	DD	2984139615
	DD	1031798784
	DD	1010124837
	DD	3212791405
	DD	2999982212
	DD	1031798784
	DD	3168602920
	DD	3212756077
	DD	2985576777
	DD	1040187392
	DD	3140071849
	DD	3212710692
	DD	854713859
	DD	1040187392
	DD	1018299420
	DD	3212655276
	DD	821517033
	DD	1040187392
	DD	1027359369
	DD	3212589864
	DD	842126987
	DD	1040187392
	DD	3177244920
	DD	3212514494
	DD	3003086283
	DD	1048576000
	DD	3170705253
	DD	3212429213
	DD	2999340633
	DD	1048576000
	DD	3152414341
	DD	3212334072
	DD	839760357
	DD	1048576000
	DD	1015605553
	DD	3212229127
	DD	2993490220
	DD	1048576000
	DD	1025835404
	DD	3212114443
	DD	848534818
	DD	1048576000
	DD	1031957395
	DD	3211990087
	DD	2999225873
	DD	1048576000
	DD	1035072335
	DD	3211856136
	DD	2988363997
	DD	1048576000
	DD	3188684717
	DD	3211712670
	DD	848507868
	DD	1056964608
	DD	3186639787
	DD	3211559774
	DD	3002280148
	DD	1056964608
	DD	3183612120
	DD	3211397543
	DD	854270828
	DD	1056964608
	DD	3180617215
	DD	3211226072
	DD	851194761
	DD	1056964608
	DD	3176031322
	DD	3211045465
	DD	2971273466
	DD	1056964608
	DD	3169472868
	DD	3210855832
	DD	854062117
	DD	1056964608
	DD	3152590408
	DD	3210657285
	DD	2999384403
	DD	1056964608
	DD	1013387058
	DD	3210449946
	DD	2988649928
	DD	1056964608
	DD	1024416170
	DD	3210233939
	DD	847077312
	DD	1056964608
	DD	1029938589
	DD	3210009393
	DD	2995841562
	DD	1056964608
	DD	1033584979
	DD	3209776445
	DD	2953596676
	DD	1056964608
	DD	1036254719
	DD	3209535234
	DD	2994630888
	DD	1056964608
	DD	1038876298
	DD	3209285906
	DD	2996381248
	DD	1056964608
	DD	1040817765
	DD	3209028611
	DD	2997965172
	DD	1056964608
	DD	1042078039
	DD	3208763504
	DD	2998925687
	DD	1056964608
	DD	1043311911
	DD	3208490745
	DD	2979703788
	DD	1056964608
	DD	1044518635
	DD	3208210498
	DD	847314951
	DD	1056964608
	DD	3197498906
	DD	3207922931
	DD	2991581050
	DD	1065353216
	DD	3196923773
	DD	3207628219
	DD	850275634
	DD	1065353216
	DD	3196363278
	DD	3207326538
	DD	850866486
	DD	1065353216
	DD	3195575871
	DD	3207018070
	DD	839091011
	DD	1065353216
	DD	3194515443
	DD	3206703001
	DD	2988516283
	DD	1065353216
	DD	3193486263
	DD	3206381521
	DD	2995913996
	DD	1065353216
	DD	3192488951
	DD	3206053824
	DD	835167219
	DD	1065353216
	DD	3191524108
	DD	3205720106
	DD	2999832878
	DD	1065353216
	DD	3190592315
	DD	3205380570
	DD	843723495
	DD	1065353216
	DD	3189694133
	DD	3205035419
	DD	851331918
	DD	1065353216
	DD	3188830103
	DD	3204684861
	DD	844865538
	DD	1065353216
	DD	3188000746
	DD	3204209959
	DD	830532777
	DD	1065353216
	DD	3186742084
	DD	3203488490
	DD	838803769
	DD	1065353216
	DD	3185225016
	DD	3202757493
	DD	836355956
	DD	1065353216
	DD	3183780163
	DD	3202017408
	DD	2988316108
	DD	1065353216
	DD	3182408396
	DD	3201268682
	DD	2976529251
	DD	1065353216
	DD	3181110540
	DD	3200511765
	DD	2983580972
	DD	1065353216
	DD	3179887378
	DD	3199747114
	DD	846224294
	DD	1065353216
	DD	3178196862
	DD	3198975188
	DD	841305602
	DD	1065353216
	DD	3176053642
	DD	3198196453
	DD	842959264
	DD	1065353216
	DD	3174063957
	DD	3197411377
	DD	2993510896
	DD	1065353216
	DD	3172229004
	DD	3196620435
	DD	2972482974
	DD	1065353216
	DD	3170205956
	DD	3195588556
	DD	823907357
	DD	1065353216
	DD	3167161428
	DD	3193986067
	DD	2980570358
	DD	1065353216
	DD	3164432432
	DD	3192374722
	DD	820352637
	DD	1065353216
	DD	3161536011
	DD	3190755490
	DD	2970707961
	DD	1065353216
	DD	3157349634
	DD	3189129347
	DD	2984830484
	DD	1065353216
	DD	3153489468
	DD	3187323507
	DD	823486671
	DD	1065353216
	DD	3147680113
	DD	3184049462
	DD	813011701
	DD	1065353216
	DD	3140588184
	DD	3180767493
	DD	827530849
	DD	1065353216
	DD	3130909128
	DD	3175676720
	DD	820978303
	DD	1065353216
	DD	3114133471
	DD	3167292080
	DD	805685656
	DD	1065353216
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
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	3190467243
	DD	1007192156
	DD	1007192156
	DD	1007192156
	DD	1007192156
	DD	1007192156
	DD	1007192156
	DD	1007192156
	DD	1007192156
	DD	1007192156
	DD	1007192156
	DD	1007192156
	DD	1007192156
	DD	1007192156
	DD	1007192156
	DD	1007192156
	DD	1007192156
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
	DD	1026206332
	DD	1026206332
	DD	1026206332
	DD	1026206332
	DD	1026206332
	DD	1026206332
	DD	1026206332
	DD	1026206332
	DD	1026206332
	DD	1026206332
	DD	1026206332
	DD	1026206332
	DD	1026206332
	DD	1026206332
	DD	1026206332
	DD	1026206332
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
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	1109588355
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	3167293403
	DD	809222144
	DD	809222144
	DD	809222144
	DD	809222144
	DD	809222144
	DD	809222144
	DD	809222144
	DD	809222144
	DD	809222144
	DD	809222144
	DD	809222144
	DD	809222144
	DD	809222144
	DD	809222144
	DD	809222144
	DD	809222144
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2838781952
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	2710384946
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1073741824
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1050868099
	DD	1413742592
	DD	1074340347
	DD	1413742592
	DD	1074340347
	DD	1413742592
	DD	1074340347
	DD	1413742592
	DD	1074340347
	DD	1413742592
	DD	1074340347
	DD	1413742592
	DD	1074340347
	DD	1413742592
	DD	1074340347
	DD	1413742592
	DD	1074340347
	DD	1280075305
	DD	1033276451
	DD	1280075305
	DD	1033276451
	DD	1280075305
	DD	1033276451
	DD	1280075305
	DD	1033276451
	DD	1280075305
	DD	1033276451
	DD	1280075305
	DD	1033276451
	DD	1280075305
	DD	1033276451
	DD	1280075305
	DD	1033276451
	DD	3162727815
	DD	3217380692
	DD	3162727815
	DD	3217380692
	DD	3162727815
	DD	3217380692
	DD	3162727815
	DD	3217380692
	DD	3162727815
	DD	3217380692
	DD	3162727815
	DD	3217380692
	DD	3162727815
	DD	3217380692
	DD	3162727815
	DD	3217380692
	DD	939838102
	DD	1065423085
	DD	939838102
	DD	1065423085
	DD	939838102
	DD	1065423085
	DD	939838102
	DD	1065423085
	DD	939838102
	DD	1065423085
	DD	939838102
	DD	1065423085
	DD	939838102
	DD	1065423085
	DD	939838102
	DD	1065423085
	DD	4003935331
	DD	3207198463
	DD	4003935331
	DD	3207198463
	DD	4003935331
	DD	3207198463
	DD	4003935331
	DD	3207198463
	DD	4003935331
	DD	3207198463
	DD	4003935331
	DD	3207198463
	DD	4003935331
	DD	3207198463
	DD	4003935331
	DD	3207198463
	DD	239893998
	DD	1053154271
	DD	239893998
	DD	1053154271
	DD	239893998
	DD	1053154271
	DD	239893998
	DD	1053154271
	DD	239893998
	DD	1053154271
	DD	239893998
	DD	1053154271
	DD	239893998
	DD	1053154271
	DD	239893998
	DD	1053154271
_vmldSinHATab	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	393047345
	DD	3212032302
	DD	3156849708
	DD	1069094822
	DD	3758096384
	DD	3158189848
	DD	0
	DD	1072693248
	DD	18115067
	DD	3214126342
	DD	1013556747
	DD	1070135480
	DD	3221225472
	DD	3160567065
	DD	0
	DD	1072693248
	DD	2476548698
	DD	3215330282
	DD	785751814
	DD	1070765062
	DD	2684354560
	DD	3161838221
	DD	0
	DD	1072693248
	DD	2255197647
	DD	3216211105
	DD	2796464483
	DD	1071152610
	DD	3758096384
	DD	3160878317
	DD	0
	DD	1072693248
	DD	1945768569
	DD	3216915048
	DD	939980347
	DD	1071524701
	DD	536870912
	DD	1012796809
	DD	0
	DD	1072693248
	DD	1539668340
	DD	3217396327
	DD	967731400
	DD	1071761211
	DD	536870912
	DD	1015752157
	DD	0
	DD	1072693248
	DD	1403757309
	DD	3217886718
	DD	621354454
	DD	1071926515
	DD	536870912
	DD	1013450602
	DD	0
	DD	1072693248
	DD	2583490354
	DD	1070236281
	DD	1719614413
	DD	1072079006
	DD	536870912
	DD	3163282740
	DD	0
	DD	1071644672
	DD	2485417816
	DD	1069626316
	DD	1796544321
	DD	1072217216
	DD	536870912
	DD	3162686945
	DD	0
	DD	1071644672
	DD	2598800519
	DD	1068266419
	DD	688824739
	DD	1072339814
	DD	3758096384
	DD	1010431536
	DD	0
	DD	1071644672
	DD	2140183630
	DD	3214756396
	DD	4051746225
	DD	1072445618
	DD	2147483648
	DD	3161907377
	DD	0
	DD	1071644672
	DD	1699043957
	DD	3216902261
	DD	3476196678
	DD	1072533611
	DD	536870912
	DD	1014257638
	DD	0
	DD	1071644672
	DD	1991047213
	DD	1067753521
	DD	1455828442
	DD	1072602945
	DD	3758096384
	DD	1015505073
	DD	0
	DD	1070596096
	DD	240740309
	DD	3215727903
	DD	3489094832
	DD	1072652951
	DD	536870912
	DD	1014325783
	DD	0
	DD	1070596096
	DD	257503056
	DD	3214647653
	DD	2748392742
	DD	1072683149
	DD	1073741824
	DD	3163061750
	DD	0
	DD	1069547520
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	2147483648
	DD	257503056
	DD	1067164005
	DD	2748392742
	DD	1072683149
	DD	1073741824
	DD	3163061750
	DD	0
	DD	3217031168
	DD	240740309
	DD	1068244255
	DD	3489094832
	DD	1072652951
	DD	536870912
	DD	1014325783
	DD	0
	DD	3218079744
	DD	1991047213
	DD	3215237169
	DD	1455828442
	DD	1072602945
	DD	3758096384
	DD	1015505073
	DD	0
	DD	3218079744
	DD	1699043957
	DD	1069418613
	DD	3476196678
	DD	1072533611
	DD	536870912
	DD	1014257638
	DD	0
	DD	3219128320
	DD	2140183630
	DD	1067272748
	DD	4051746225
	DD	1072445618
	DD	2147483648
	DD	3161907377
	DD	0
	DD	3219128320
	DD	2598800519
	DD	3215750067
	DD	688824739
	DD	1072339814
	DD	3758096384
	DD	1010431536
	DD	0
	DD	3219128320
	DD	2485417816
	DD	3217109964
	DD	1796544321
	DD	1072217216
	DD	536870912
	DD	3162686945
	DD	0
	DD	3219128320
	DD	2583490354
	DD	3217719929
	DD	1719614413
	DD	1072079006
	DD	536870912
	DD	3163282740
	DD	0
	DD	3219128320
	DD	1403757309
	DD	1070403070
	DD	621354454
	DD	1071926515
	DD	536870912
	DD	1013450602
	DD	0
	DD	3220176896
	DD	1539668340
	DD	1069912679
	DD	967731400
	DD	1071761211
	DD	536870912
	DD	1015752157
	DD	0
	DD	3220176896
	DD	1945768569
	DD	1069431400
	DD	939980347
	DD	1071524701
	DD	536870912
	DD	1012796809
	DD	0
	DD	3220176896
	DD	2255197647
	DD	1068727457
	DD	2796464483
	DD	1071152610
	DD	3758096384
	DD	3160878317
	DD	0
	DD	3220176896
	DD	2476548698
	DD	1067846634
	DD	785751814
	DD	1070765062
	DD	2684354560
	DD	3161838221
	DD	0
	DD	3220176896
	DD	18115067
	DD	1066642694
	DD	1013556747
	DD	1070135480
	DD	3221225472
	DD	3160567065
	DD	0
	DD	3220176896
	DD	393047345
	DD	1064548654
	DD	3156849708
	DD	1069094822
	DD	3758096384
	DD	3158189848
	DD	0
	DD	3220176896
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3220176896
	DD	393047345
	DD	1064548654
	DD	3156849708
	DD	3216578470
	DD	3758096384
	DD	1010706200
	DD	0
	DD	3220176896
	DD	18115067
	DD	1066642694
	DD	1013556747
	DD	3217619128
	DD	3221225472
	DD	1013083417
	DD	0
	DD	3220176896
	DD	2476548698
	DD	1067846634
	DD	785751814
	DD	3218248710
	DD	2684354560
	DD	1014354573
	DD	0
	DD	3220176896
	DD	2255197647
	DD	1068727457
	DD	2796464483
	DD	3218636258
	DD	3758096384
	DD	1013394669
	DD	0
	DD	3220176896
	DD	1945768569
	DD	1069431400
	DD	939980347
	DD	3219008349
	DD	536870912
	DD	3160280457
	DD	0
	DD	3220176896
	DD	1539668340
	DD	1069912679
	DD	967731400
	DD	3219244859
	DD	536870912
	DD	3163235805
	DD	0
	DD	3220176896
	DD	1403757309
	DD	1070403070
	DD	621354454
	DD	3219410163
	DD	536870912
	DD	3160934250
	DD	0
	DD	3220176896
	DD	2583490354
	DD	3217719929
	DD	1719614413
	DD	3219562654
	DD	536870912
	DD	1015799092
	DD	0
	DD	3219128320
	DD	2485417816
	DD	3217109964
	DD	1796544321
	DD	3219700864
	DD	536870912
	DD	1015203297
	DD	0
	DD	3219128320
	DD	2598800519
	DD	3215750067
	DD	688824739
	DD	3219823462
	DD	3758096384
	DD	3157915184
	DD	0
	DD	3219128320
	DD	2140183630
	DD	1067272748
	DD	4051746225
	DD	3219929266
	DD	2147483648
	DD	1014423729
	DD	0
	DD	3219128320
	DD	1699043957
	DD	1069418613
	DD	3476196678
	DD	3220017259
	DD	536870912
	DD	3161741286
	DD	0
	DD	3219128320
	DD	1991047213
	DD	3215237169
	DD	1455828442
	DD	3220086593
	DD	3758096384
	DD	3162988721
	DD	0
	DD	3218079744
	DD	240740309
	DD	1068244255
	DD	3489094832
	DD	3220136599
	DD	536870912
	DD	3161809431
	DD	0
	DD	3218079744
	DD	257503056
	DD	1067164005
	DD	2748392742
	DD	3220166797
	DD	1073741824
	DD	1015578102
	DD	0
	DD	3217031168
	DD	0
	DD	2147483648
	DD	0
	DD	3220176896
	DD	0
	DD	0
	DD	0
	DD	2147483648
	DD	257503056
	DD	3214647653
	DD	2748392742
	DD	3220166797
	DD	1073741824
	DD	1015578102
	DD	0
	DD	1069547520
	DD	240740309
	DD	3215727903
	DD	3489094832
	DD	3220136599
	DD	536870912
	DD	3161809431
	DD	0
	DD	1070596096
	DD	1991047213
	DD	1067753521
	DD	1455828442
	DD	3220086593
	DD	3758096384
	DD	3162988721
	DD	0
	DD	1070596096
	DD	1699043957
	DD	3216902261
	DD	3476196678
	DD	3220017259
	DD	536870912
	DD	3161741286
	DD	0
	DD	1071644672
	DD	2140183630
	DD	3214756396
	DD	4051746225
	DD	3219929266
	DD	2147483648
	DD	1014423729
	DD	0
	DD	1071644672
	DD	2598800519
	DD	1068266419
	DD	688824739
	DD	3219823462
	DD	3758096384
	DD	3157915184
	DD	0
	DD	1071644672
	DD	2485417816
	DD	1069626316
	DD	1796544321
	DD	3219700864
	DD	536870912
	DD	1015203297
	DD	0
	DD	1071644672
	DD	2583490354
	DD	1070236281
	DD	1719614413
	DD	3219562654
	DD	536870912
	DD	1015799092
	DD	0
	DD	1071644672
	DD	1403757309
	DD	3217886718
	DD	621354454
	DD	3219410163
	DD	536870912
	DD	3160934250
	DD	0
	DD	1072693248
	DD	1539668340
	DD	3217396327
	DD	967731400
	DD	3219244859
	DD	536870912
	DD	3163235805
	DD	0
	DD	1072693248
	DD	1945768569
	DD	3216915048
	DD	939980347
	DD	3219008349
	DD	536870912
	DD	3160280457
	DD	0
	DD	1072693248
	DD	2255197647
	DD	3216211105
	DD	2796464483
	DD	3218636258
	DD	3758096384
	DD	1013394669
	DD	0
	DD	1072693248
	DD	2476548698
	DD	3215330282
	DD	785751814
	DD	3218248710
	DD	2684354560
	DD	1014354573
	DD	0
	DD	1072693248
	DD	18115067
	DD	3214126342
	DD	1013556747
	DD	3217619128
	DD	3221225472
	DD	1013083417
	DD	0
	DD	1072693248
	DD	393047345
	DD	3212032302
	DD	3156849708
	DD	3216578470
	DD	3758096384
	DD	1010706200
	DD	0
	DD	1072693248
	DD	1841940611
	DD	1076125488
	DD	0
	DD	1127743488
	DD	1413480448
	DD	1069097467
	DD	442499072
	DD	1032893537
	DD	771977331
	DD	996350346
	DD	1431655765
	DD	3217380693
	DD	286331153
	DD	1065423121
	DD	436314138
	DD	3207201184
	DD	2773927732
	DD	1053236707
	DD	0
	DD	3219128320
	DD	1431655765
	DD	1067799893
	DD	381774871
	DD	3210133868
	DD	436314138
	DD	1056571808
	DD	0
	DD	1130364928
	DD	0
	DD	1015021568
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	2146435072
	DD 4 DUP (0H)	
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
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
