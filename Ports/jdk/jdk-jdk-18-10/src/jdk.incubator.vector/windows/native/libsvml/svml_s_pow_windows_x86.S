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
	PUBLIC __svml_powf16_ha_z0

__svml_powf16_ha_z0	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 1400
        vgetmantps zmm5, zmm0, 10 {sae}
        vgetexpps zmm31, zmm0 {sae}
        vfpclassps k0, zmm0, 223
        vrcp14ps  zmm23, zmm5
        kmovw     r8d, k0
        vrndscaleps zmm24, zmm23, 88 {sae}
        mov       QWORD PTR [1384+rsp], r13
        lea       r13, QWORD PTR [1183+rsp]
        vmovups   zmm26, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+576]
        and       r13, -64
        vmovups   zmm25, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+512]
        vmovups   zmm28, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+640]
        vmovups   zmm29, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+704]
        vmovups   zmm30, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512]
        vfmsub213ps zmm5, zmm24, zmm26 {rn-sae}
        vmovups   zmm23, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+832]
        vmovups   zmm4, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+128]
        vcmpps    k2, zmm24, zmm25, 17 {sae}
        vmulps    zmm3, zmm5, zmm5 {rn-sae}
        vfmadd231ps zmm29, zmm28, zmm5 {rn-sae}
        vmovups   zmm25, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+896]
        vmovups   zmm28, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+1024]
        vpsrld    zmm27, zmm24, 18
        vaddps    zmm31{k2}, zmm31, zmm26 {rn-sae}
        vmovups   zmm26, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+960]
        vpermt2ps zmm30, zmm27, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+64]
        vpermt2ps zmm4, zmm27, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+192]
        vaddps    zmm24, zmm30, zmm31 {rn-sae}
        vmovaps   zmm22, zmm1
        vmovups   zmm1, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+768]
        vfpclassps k1, zmm22, 153
        vfmadd231ps zmm1, zmm29, zmm5 {rn-sae}
        kmovw     eax, k1
        vfmadd231ps zmm23, zmm1, zmm5 {rn-sae}
        vmovaps   zmm2, zmm5
        or        r8d, eax
        vfmsub213ps zmm2, zmm5, zmm3 {rn-sae}
        vfmadd231ps zmm25, zmm23, zmm5 {rn-sae}
        vfmadd213ps zmm2, zmm28, zmm4 {rn-sae}
        vmovaps   zmm27, zmm24
        vfmadd231ps zmm27, zmm26, zmm5 {rn-sae}
        vfmadd231ps zmm2, zmm25, zmm5 {rn-sae}
        vmovups   zmm25, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+1216]
        vsubps    zmm4, zmm27, zmm24 {rn-sae}
        vmovaps   zmm30, zmm27
        vfmadd231ps zmm30, zmm28, zmm3 {rn-sae}
        vfmsub213ps zmm5, zmm26, zmm4 {rn-sae}
        vmovups   zmm26, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+1344]
        vaddps    zmm1, zmm30, zmm2 {rn-sae}
        vsubps    zmm29, zmm30, zmm27 {rn-sae}
        vmulps    zmm4, zmm1, zmm22 {rz-sae}
        vfmsub213ps zmm3, zmm28, zmm29 {rn-sae}
        vmovups   zmm28, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+384]
        vmovups   zmm29, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+256]
        vsubps    zmm31, zmm1, zmm30 {rn-sae}
        vaddps    zmm3, zmm5, zmm3 {rn-sae}
        vfmsub213ps zmm1, zmm22, zmm4 {rz-sae}
        vmovups   zmm5, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+1088]
        vmovups   zmm30, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+1280]
        vsubps    zmm2, zmm2, zmm31 {rn-sae}
        vaddps    zmm2, zmm2, zmm3 {rn-sae}
        vfmadd213ps zmm2, zmm22, zmm1 {rz-sae}
        vaddps    zmm1, zmm4, zmm2 {rz-sae}
        vsubps    zmm4, zmm1, zmm4 {rn-sae}
        vaddps    zmm23, zmm1, zmm5 {rd-sae}
        vreduceps zmm3, zmm1, 81 {sae}
        vsubps    zmm5, zmm2, zmm4 {rn-sae}
        vpermt2ps zmm28, zmm23, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+448]
        vpermt2ps zmm29, zmm23, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+320]
        vaddps    zmm24, zmm3, zmm5 {rn-sae}
        vandps    zmm27, zmm24, ZMMWORD PTR [__svml_spow_ha_data_internal_avx512+1152]
        vfmadd231ps zmm30, zmm25, zmm27 {rn-sae}
        vfmadd213ps zmm30, zmm27, zmm26 {rn-sae}
        vfmadd213ps zmm30, zmm27, zmm28 {rn-sae}
        vfmadd213ps zmm30, zmm29, zmm29 {rn-sae}
        mov       QWORD PTR [1392+rsp], r13
        vscalefps zmm1, zmm30, zmm1 {rn-sae}
        jne       _B1_3

_B1_2::

        mov       r13, QWORD PTR [1384+rsp]
        vmovaps   zmm0, zmm1
        add       rsp, 1400
        ret

_B1_3::

        vstmxcsr  DWORD PTR [1376+rsp]

_B1_4::

        movzx     edx, WORD PTR [1376+rsp]
        mov       eax, edx
        or        eax, 8064
        cmp       edx, eax
        je        _B1_6

_B1_5::

        mov       DWORD PTR [1376+rsp], eax
        vldmxcsr  DWORD PTR [1376+rsp]

_B1_6::

        vmovups   ZMMWORD PTR [r13], zmm0
        vmovups   ZMMWORD PTR [64+r13], zmm22
        vmovups   ZMMWORD PTR [128+r13], zmm1
        test      r8d, r8d
        jne       _B1_11

_B1_7::

        cmp       edx, eax
        je        _B1_2

_B1_8::

        vstmxcsr  DWORD PTR [1376+rsp]
        mov       eax, DWORD PTR [1376+rsp]

_B1_9::

        and       eax, -8065
        or        eax, edx
        mov       DWORD PTR [1376+rsp], eax
        vldmxcsr  DWORD PTR [1376+rsp]
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
        vmovups   zmm1, ZMMWORD PTR [128+r13]
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
        lea       r8, QWORD PTR [128+r13+rbx*4]

        call      __svml_spow_ha_cout_rare_internal
        jmp       _B1_13
        ALIGN     16

_B1_16::

__svml_powf16_ha_z0 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_powf16_ha_z0_B1_B9:
	DD	276481
	DD	11392056
	DD	11469067

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_powf16_ha_z0_B1_B9

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_powf16_ha_z0_B11_B15:
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

	DD	imagerel _B1_11
	DD	imagerel _B1_16
	DD	imagerel _unwind___svml_powf16_ha_z0_B11_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_powf8_ha_l9

__svml_powf8_ha_l9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L52::

        sub       rsp, 616
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [304+rsp], ymm15
        vmovups   YMMWORD PTR [336+rsp], ymm14
        vmovups   YMMWORD PTR [368+rsp], ymm13
        vmovups   YMMWORD PTR [400+rsp], ymm12
        vmovups   YMMWORD PTR [432+rsp], ymm11
        vmovups   YMMWORD PTR [496+rsp], ymm10
        vmovups   YMMWORD PTR [528+rsp], ymm9
        vmovups   YMMWORD PTR [560+rsp], ymm8
        vmovups   YMMWORD PTR [464+rsp], ymm7
        vmovups   YMMWORD PTR [272+rsp], ymm6
        mov       QWORD PTR [592+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovupd   ymm4, YMMWORD PTR [__svml_spow_ha_data_internal+12992]
        and       r13, -64
        vmovupd   ymm5, YMMWORD PTR [__svml_spow_ha_data_internal+13056]
        vmovups   ymm14, YMMWORD PTR [__VPACK_ODD_ind_2742_0_4]
        vmovaps   ymm13, ymm1
        mov       QWORD PTR [600+rsp], r13
        vcvtps2pd ymm7, xmm0
        vpsubd    ymm10, ymm0, YMMWORD PTR [__svml_spow_ha_data_internal]
        vextractf128 xmm9, ymm0, 1
        vcvtps2pd ymm2, xmm9
        vandpd    ymm8, ymm7, ymm4
        vorpd     ymm8, ymm8, ymm5
        vcvtpd2ps xmm3, ymm8
        vpsrlq    ymm7, ymm7, 20
        vrcpps    xmm1, xmm3
        vandpd    ymm4, ymm2, ymm4
        vorpd     ymm4, ymm4, ymm5
        vcvtpd2ps xmm6, ymm4
        vcvtps2pd ymm3, xmm1
        vpsrlq    ymm2, ymm2, 20
        vrcpps    xmm15, xmm6
        vroundpd  ymm1, ymm3, 0
        vcvtps2pd ymm5, xmm15
        vpsrlq    ymm15, ymm1, 40
        vpermps   ymm6, ymm14, ymm7
        vpermps   ymm7, ymm14, ymm2
        vroundpd  ymm14, ymm5, 0
        vmovupd   ymm5, YMMWORD PTR [__svml_spow_ha_data_internal+13312]
        vfmsub213pd ymm8, ymm1, ymm5
        vfmsub213pd ymm4, ymm14, ymm5
        vcvtdq2pd ymm2, xmm6
        vpsrlq    ymm6, ymm14, 40
        vmovd     eax, xmm15
        movsxd    rax, eax
        vpextrd   ecx, xmm15, 2
        movsxd    rcx, ecx
        vcvtdq2pd ymm3, xmm7
        vmovsd    xmm5, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rax]
        vextracti128 xmm7, ymm15, 1
        vmovhpd   xmm5, xmm5, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rcx]
        vmovd     r8d, xmm7
        movsxd    r8, r8d
        vpextrd   r9d, xmm7, 2
        movsxd    r9, r9d
        vmovd     r10d, xmm6
        vmovsd    xmm15, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r8]
        vmovhpd   xmm7, xmm15, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r9]
        vpextrd   r11d, xmm6, 2
        vextracti128 xmm6, ymm6, 1
        vmovd     eax, xmm6
        movsxd    r10, r10d
        movsxd    r11, r11d
        movsxd    rax, eax
        vpextrd   ecx, xmm6, 2
        movsxd    rcx, ecx
        vmovsd    xmm15, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r10]
        vinsertf128 ymm5, ymm5, xmm7, 1
        vmovhpd   xmm7, xmm15, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r11]
        vmovsd    xmm15, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rax]
        vmovhpd   xmm6, xmm15, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rcx]
        vmovupd   ymm15, YMMWORD PTR [__svml_spow_ha_data_internal+13504]
        vcmplt_oqpd ymm1, ymm15, ymm1
        vcmplt_oqpd ymm14, ymm15, ymm14
        vmovupd   ymm15, YMMWORD PTR [__svml_spow_ha_data_internal+13568]
        vandpd    ymm1, ymm1, ymm15
        vandpd    ymm14, ymm14, ymm15
        vmovupd   ymm15, YMMWORD PTR [__svml_spow_ha_data_internal+13632]
        vorpd     ymm1, ymm1, ymm15
        vorpd     ymm14, ymm14, ymm15
        vsubpd    ymm2, ymm2, ymm1
        vsubpd    ymm3, ymm3, ymm14
        vmovupd   ymm15, YMMWORD PTR [__svml_spow_ha_data_internal+12928]
        vmovapd   ymm14, ymm15
        vinsertf128 ymm6, ymm7, xmm6, 1
        vmovupd   ymm7, YMMWORD PTR [__svml_spow_ha_data_internal+13696]
        vmulpd    ymm1, ymm7, ymm2
        vmulpd    ymm2, ymm7, ymm3
        vmovupd   ymm3, YMMWORD PTR [__svml_spow_ha_data_internal+12864]
        vmulpd    ymm7, ymm8, ymm8
        vfmadd231pd ymm14, ymm3, ymm8
        vfmadd231pd ymm15, ymm3, ymm4
        vmulpd    ymm3, ymm4, ymm4
        vmulpd    ymm14, ymm14, ymm7
        vmulpd    ymm15, ymm15, ymm3
        vmovupd   ymm3, YMMWORD PTR [__svml_spow_ha_data_internal+30464]
        vaddpd    ymm8, ymm8, ymm14
        vaddpd    ymm4, ymm4, ymm15
        vmovups   ymm14, YMMWORD PTR [__svml_spow_ha_data_internal+64]
        vaddpd    ymm8, ymm5, ymm8
        vaddpd    ymm4, ymm6, ymm4
        vaddpd    ymm1, ymm1, ymm8
        vaddpd    ymm8, ymm2, ymm4
        vpcmpgtd  ymm15, ymm10, ymm14
        vpcmpeqd  ymm10, ymm10, ymm14
        vextractf128 xmm11, ymm13, 1
        vcvtps2pd ymm9, xmm13
        vcvtps2pd ymm12, xmm11
        vpand     ymm11, ymm13, YMMWORD PTR [__svml_spow_ha_data_internal+192]
        vmulpd    ymm7, ymm9, ymm1
        vmovups   ymm1, YMMWORD PTR [__VPACK_ODD_ind_2743_0_4]
        vmulpd    ymm8, ymm12, ymm8
        vmovupd   ymm12, YMMWORD PTR [__svml_spow_ha_data_internal+30400]
        vmovupd   ymm9, YMMWORD PTR [__svml_spow_ha_data_internal+30528]
        vmulpd    ymm5, ymm12, ymm7
        vmulpd    ymm4, ymm12, ymm8
        vsubpd    ymm12, ymm5, ymm9
        vsubpd    ymm9, ymm4, ymm9
        vaddpd    ymm12, ymm3, ymm12
        vaddpd    ymm9, ymm3, ymm9
        vsubpd    ymm2, ymm12, ymm3
        vsubpd    ymm3, ymm9, ymm3
        vsubpd    ymm5, ymm5, ymm2
        vsubpd    ymm2, ymm4, ymm3
        vmovupd   ymm4, YMMWORD PTR [__svml_spow_ha_data_internal+30592]
        vpermps   ymm6, ymm1, ymm7
        vpermps   ymm7, ymm1, ymm8
        vmovups   ymm8, YMMWORD PTR [__svml_spow_ha_data_internal+128]
        vpor      ymm1, ymm15, ymm10
        vpcmpgtd  ymm10, ymm11, ymm8
        vpcmpeqd  ymm11, ymm11, ymm8
        vmovupd   ymm8, YMMWORD PTR [__svml_spow_ha_data_internal+30656]
        vinsertf128 ymm7, ymm6, xmm7, 1
        vpor      ymm6, ymm10, ymm11
        vpor      ymm14, ymm1, ymm6
        vandps    ymm7, ymm7, YMMWORD PTR [__svml_spow_ha_data_internal+30720]
        vpcmpgtd  ymm15, ymm7, YMMWORD PTR [__svml_spow_ha_data_internal+30784]
        vpor      ymm11, ymm14, ymm15
        vmovmskps eax, ymm11
        vandps    ymm11, ymm12, ymm8
        vandps    ymm10, ymm9, ymm8
        vmulpd    ymm8, ymm4, ymm5
        vmulpd    ymm4, ymm4, ymm2
        vpsrlq    ymm12, ymm12, 11
        vpsrlq    ymm9, ymm9, 11
        vpsllq    ymm9, ymm9, 52
        vextracti128 xmm1, ymm11, 1
        vmovd     r8d, xmm11
        vmovd     r10d, xmm1
        shl       r8d, 3
        vmovq     xmm3, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r8]
        vpextrd   r9d, xmm11, 2
        shl       r10d, 3
        vpextrd   ecx, xmm1, 2
        shl       r9d, 3
        shl       ecx, 3
        vmovq     xmm2, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r10]
        vmovhpd   xmm11, xmm3, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r9]
        vmovhpd   xmm3, xmm2, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+rcx]
        vextracti128 xmm5, ymm10, 1
        vmovd     r8d, xmm10
        vmovd     r11d, xmm5
        shl       r8d, 3
        vpextrd   r9d, xmm10, 2
        shl       r11d, 3
        vpextrd   ecx, xmm5, 2
        shl       r9d, 3
        vmovq     xmm6, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r8]
        shl       ecx, 3
        vmovq     xmm10, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r11]
        vmovhpd   xmm7, xmm6, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r9]
        vmovhpd   xmm5, xmm10, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+rcx]
        vinsertf128 ymm11, ymm11, xmm3, 1
        vfmadd213pd ymm11, ymm8, ymm11
        vinsertf128 ymm14, ymm7, xmm5, 1
        vfmadd213pd ymm14, ymm4, ymm14
        vpsllq    ymm4, ymm12, 52
        vpaddq    ymm3, ymm14, ymm9
        vpaddq    ymm1, ymm11, ymm4
        vcvtpd2ps xmm4, ymm3
        vcvtpd2ps xmm2, ymm1
        vinsertf128 ymm1, ymm2, xmm4, 1
        test      eax, eax
        jne       _B2_3

_B2_2::

        vmovups   ymm6, YMMWORD PTR [272+rsp]
        vmovups   ymm7, YMMWORD PTR [464+rsp]
        vmovups   ymm8, YMMWORD PTR [560+rsp]
        vmovups   ymm9, YMMWORD PTR [528+rsp]
        vmovups   ymm10, YMMWORD PTR [496+rsp]
        vmovups   ymm11, YMMWORD PTR [432+rsp]
        vmovups   ymm12, YMMWORD PTR [400+rsp]
        vmovups   ymm13, YMMWORD PTR [368+rsp]
        vmovups   ymm14, YMMWORD PTR [336+rsp]
        vmovups   ymm15, YMMWORD PTR [304+rsp]
        mov       r13, QWORD PTR [592+rsp]
        vmovdqa   ymm0, ymm1
        add       rsp, 616
        ret

_B2_3::

        vmovups   YMMWORD PTR [r13], ymm0
        vmovups   YMMWORD PTR [64+r13], ymm13
        vmovups   YMMWORD PTR [128+r13], ymm1

_B2_6::

        xor       edx, edx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, edx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

_B2_7::

        bt        esi, ebx
        jc        _B2_10

_B2_8::

        inc       ebx
        cmp       ebx, 8
        jl        _B2_7

_B2_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovups   ymm1, YMMWORD PTR [128+r13]
        jmp       _B2_2

_B2_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]
        lea       r8, QWORD PTR [128+r13+rbx*4]

        call      __svml_spow_ha_cout_rare_internal
        jmp       _B2_8
        ALIGN     16

_B2_11::

__svml_powf8_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_powf8_ha_l9_B1_B3:
	DD	1602561
	DD	4904052
	DD	1140844
	DD	1931363
	DD	2328666
	DD	2201681
	DD	2074696
	DD	1816639
	DD	1689654
	DD	1562669
	DD	1435684
	DD	1308699
	DD	5046539

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_powf8_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_powf8_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_powf8_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_6
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_powf8_ha_l9_B6_B10

.pdata	ENDS
_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
__VPACK_ODD_ind_2742_0_4	DD	1
	DD	3
	DD	5
	DD	7
	DD	0
	DD	0
	DD	0
	DD	0
	DD 8 DUP (0H)	
__VPACK_ODD_ind_2743_0_4	DD	1
	DD	3
	DD	5
	DD	7
	DD	0
	DD	0
	DD	0
	DD	0
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_powf4_ha_e9

__svml_powf4_ha_e9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L79::

        sub       rsp, 392
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [352+rsp], xmm10
        vcvtps2pd ymm10, xmm0
        vmovups   XMMWORD PTR [272+rsp], xmm13
        vmovups   XMMWORD PTR [320+rsp], xmm11
        vmovups   XMMWORD PTR [288+rsp], xmm9
        vmovaps   xmm9, xmm1
        vmovups   XMMWORD PTR [304+rsp], xmm8
        vmovups   XMMWORD PTR [256+rsp], xmm7
        vmovups   XMMWORD PTR [336+rsp], xmm6
        mov       QWORD PTR [368+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vandpd    ymm2, ymm10, YMMWORD PTR [__svml_spow_ha_data_internal+12992]
        and       r13, -64
        vorpd     ymm3, ymm2, YMMWORD PTR [__svml_spow_ha_data_internal+13056]
        vcvtpd2ps xmm13, ymm3
        vcvtps2pd ymm4, xmm9
        vmovups   xmm6, XMMWORD PTR [__svml_spow_ha_data_internal+64]
        vmovups   xmm5, XMMWORD PTR [__svml_spow_ha_data_internal+128]
        vpand     xmm8, xmm9, XMMWORD PTR [__svml_spow_ha_data_internal+192]
        vrcpps    xmm11, xmm13
        vcvtps2pd ymm1, xmm11
        vpsubd    xmm7, xmm0, XMMWORD PTR [__svml_spow_ha_data_internal]
        mov       QWORD PTR [376+rsp], r13
        vpsrlq    xmm13, xmm10, 20
        vextractf128 xmm2, ymm10, 1
        vpsrlq    xmm11, xmm2, 20
        vroundpd  ymm2, ymm1, 0
        vshufps   xmm13, xmm13, xmm11, 221
        vcvtdq2pd ymm1, xmm13
        vmulpd    ymm3, ymm3, ymm2
        vsubpd    ymm3, ymm3, YMMWORD PTR [__svml_spow_ha_data_internal+13312]
        vpsrlq    xmm13, xmm2, 40
        vextractf128 xmm11, ymm2, 1
        vmovd     eax, xmm13
        vpsrlq    xmm10, xmm11, 40
        vmovd     r8d, xmm10
        movsxd    rax, eax
        vpextrd   ecx, xmm13, 2
        movsxd    r8, r8d
        vpextrd   r9d, xmm10, 2
        movsxd    rcx, ecx
        movsxd    r9, r9d
        vmovsd    xmm11, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rax]
        vmovsd    xmm13, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r8]
        vmovhpd   xmm11, xmm11, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rcx]
        vmovhpd   xmm10, xmm13, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r9]
        vinsertf128 ymm13, ymm11, xmm10, 1
        vcmpgt_oqpd ymm11, ymm2, YMMWORD PTR [__svml_spow_ha_data_internal+13504]
        vandpd    ymm10, ymm11, YMMWORD PTR [__svml_spow_ha_data_internal+13568]
        vmulpd    ymm11, ymm3, YMMWORD PTR [__svml_spow_ha_data_internal+12864]
        vorpd     ymm2, ymm10, YMMWORD PTR [__svml_spow_ha_data_internal+13632]
        vsubpd    ymm1, ymm1, ymm2
        vmulpd    ymm2, ymm3, ymm3
        vmulpd    ymm10, ymm1, YMMWORD PTR [__svml_spow_ha_data_internal+13696]
        vaddpd    ymm1, ymm11, YMMWORD PTR [__svml_spow_ha_data_internal+12928]
        vmulpd    ymm11, ymm1, ymm2
        vpcmpgtd  xmm2, xmm7, xmm6
        vpcmpeqd  xmm6, xmm7, xmm6
        vpcmpgtd  xmm7, xmm8, xmm5
        vpcmpeqd  xmm5, xmm8, xmm5
        vaddpd    ymm3, ymm3, ymm11
        vaddpd    ymm13, ymm13, ymm3
        vaddpd    ymm10, ymm10, ymm13
        vmulpd    ymm13, ymm4, ymm10
        vmovupd   ymm10, YMMWORD PTR [__svml_spow_ha_data_internal+30464]
        vmulpd    ymm3, ymm13, YMMWORD PTR [__svml_spow_ha_data_internal+30400]
        vsubpd    ymm4, ymm3, YMMWORD PTR [__svml_spow_ha_data_internal+30528]
        vaddpd    ymm11, ymm10, ymm4
        vsubpd    ymm4, ymm11, ymm10
        vpor      xmm10, xmm2, xmm6
        vsubpd    ymm3, ymm3, ymm4
        vextractf128 xmm1, ymm13, 1
        vshufps   xmm13, xmm13, xmm1, 221
        vpor      xmm1, xmm7, xmm5
        vandps    xmm13, xmm13, XMMWORD PTR [__svml_spow_ha_data_internal+30720]
        vpor      xmm2, xmm10, xmm1
        vpcmpgtd  xmm5, xmm13, XMMWORD PTR [__svml_spow_ha_data_internal+30784]
        vmovupd   xmm7, XMMWORD PTR [__svml_spow_ha_data_internal+30656]
        vpor      xmm6, xmm2, xmm5
        vmulpd    ymm1, ymm3, YMMWORD PTR [__svml_spow_ha_data_internal+30592]
        vmovmskps eax, xmm6
        vandps    xmm8, xmm11, xmm7
        vextractf128 xmm13, ymm11, 1
        vpsrlq    xmm11, xmm11, 11
        vmovd     r10d, xmm8
        vpsllq    xmm11, xmm11, 52
        vandps    xmm10, xmm13, xmm7
        vpsrlq    xmm13, xmm13, 11
        vmovd     ecx, xmm10
        shl       r10d, 3
        vpextrd   r11d, xmm8, 2
        shl       ecx, 3
        vpextrd   r8d, xmm10, 2
        shl       r11d, 3
        vmovq     xmm4, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r10]
        shl       r8d, 3
        vmovq     xmm8, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+rcx]
        vmovhpd   xmm4, xmm4, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r11]
        vmovhpd   xmm10, xmm8, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r8]
        vinsertf128 ymm2, ymm4, xmm10, 1
        vmulpd    ymm3, ymm2, ymm1
        vaddpd    ymm5, ymm2, ymm3
        vpsllq    xmm2, xmm13, 52
        vextractf128 xmm6, ymm5, 1
        vpaddq    xmm1, xmm5, xmm11
        vpaddq    xmm3, xmm6, xmm2
        vinsertf128 ymm4, ymm1, xmm3, 1
        vcvtpd2ps xmm1, ymm4
        test      eax, eax
        jne       _B3_3

_B3_2::

        vmovaps   xmm0, xmm1
        mov       r13, QWORD PTR [368+rsp]
        vzeroupper
        vmovups   xmm6, XMMWORD PTR [336+rsp]
        vmovups   xmm7, XMMWORD PTR [256+rsp]
        vmovups   xmm8, XMMWORD PTR [304+rsp]
        vmovups   xmm9, XMMWORD PTR [288+rsp]
        vmovups   xmm10, XMMWORD PTR [352+rsp]
        vmovups   xmm11, XMMWORD PTR [320+rsp]
        vmovups   xmm13, XMMWORD PTR [272+rsp]
        add       rsp, 392
        ret

_B3_3::

        vmovups   XMMWORD PTR [r13], xmm0
        vmovups   XMMWORD PTR [64+r13], xmm9
        vmovups   XMMWORD PTR [128+r13], xmm1

_B3_6::

        xor       edx, edx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, edx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

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
        vmovups   xmm1, XMMWORD PTR [128+r13]
        jmp       _B3_2

_B3_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]
        lea       r8, QWORD PTR [128+r13+rbx*4]

        call      __svml_spow_ha_cout_rare_internal
        jmp       _B3_8
        ALIGN     16

_B3_11::

__svml_powf4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_powf4_ha_e9_B1_B3:
	DD	1204481
	DD	3069025
	DD	1402969
	DD	1079376
	DD	1280071
	DD	1218618
	DD	1357873
	DD	1169448
	DD	1484827
	DD	3211531

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_powf4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_powf4_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_powf4_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_6
	DD	imagerel _B3_11
	DD	imagerel _unwind___svml_powf4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_powf8_ha_e9

__svml_powf8_ha_e9	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L100::

        sub       rsp, 616
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [400+rsp], ymm15
        vmovups   YMMWORD PTR [464+rsp], ymm14
        vmovups   YMMWORD PTR [496+rsp], ymm13
        vmovups   YMMWORD PTR [528+rsp], ymm12
        vmovups   YMMWORD PTR [560+rsp], ymm11
        vmovups   YMMWORD PTR [272+rsp], ymm10
        vmovups   YMMWORD PTR [304+rsp], ymm9
        vmovups   YMMWORD PTR [336+rsp], ymm8
        vmovups   YMMWORD PTR [368+rsp], ymm7
        vmovups   YMMWORD PTR [432+rsp], ymm6
        vextractf128 xmm11, ymm1, 1
        mov       QWORD PTR [592+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        and       r13, -64
        vmovups   YMMWORD PTR [32+r13], ymm0
        vmovupd   ymm9, YMMWORD PTR [__svml_spow_ha_data_internal+12992]
        vmovupd   ymm6, YMMWORD PTR [__svml_spow_ha_data_internal+13056]
        vmovups   xmm2, XMMWORD PTR [__svml_spow_ha_data_internal]
        vcvtps2pd ymm3, xmm11
        vmovups   xmm14, XMMWORD PTR [__svml_spow_ha_data_internal+192]
        vpand     xmm8, xmm11, xmm14
        vmovdqu   XMMWORD PTR [32+rsp], xmm8
        vmovups   YMMWORD PTR [96+r13], ymm1
        mov       QWORD PTR [600+rsp], r13
        vextractf128 xmm5, ymm0, 1
        vcvtps2pd ymm12, xmm0
        vpsubd    xmm0, xmm0, xmm2
        vcvtps2pd ymm13, xmm5
        vandpd    ymm10, ymm12, ymm9
        vmovaps   xmm4, xmm1
        vpsubd    xmm1, xmm5, xmm2
        vorpd     ymm5, ymm10, ymm6
        vpand     xmm2, xmm4, xmm14
        vcvtps2pd ymm15, xmm4
        vcvtpd2ps xmm7, ymm5
        vandpd    ymm4, ymm13, ymm9
        vorpd     ymm6, ymm4, ymm6
        vcvtpd2ps xmm11, ymm6
        vrcpps    xmm4, xmm7
        vrcpps    xmm14, xmm11
        vcvtps2pd ymm7, xmm4
        vcvtps2pd ymm10, xmm14
        vroundpd  ymm14, ymm7, 0
        vmulpd    ymm5, ymm5, ymm14
        vpsrlq    xmm11, xmm12, 20
        vextractf128 xmm4, ymm12, 1
        vpsrlq    xmm4, xmm4, 20
        vshufps   xmm11, xmm11, xmm4, 221
        vcvtdq2pd ymm11, xmm11
        vextractf128 xmm9, ymm13, 1
        vpsrlq    xmm13, xmm13, 20
        vpsrlq    xmm4, xmm9, 20
        vshufps   xmm8, xmm13, xmm4, 221
        vroundpd  ymm13, ymm10, 0
        vmovupd   ymm9, YMMWORD PTR [__svml_spow_ha_data_internal+13312]
        vcvtdq2pd ymm12, xmm8
        vmulpd    ymm4, ymm6, ymm13
        vsubpd    ymm10, ymm5, ymm9
        vsubpd    ymm9, ymm4, ymm9
        vpsrlq    xmm6, xmm14, 40
        vextractf128 xmm8, ymm14, 1
        vmovd     eax, xmm6
        vpsrlq    xmm7, xmm8, 40
        movsxd    rax, eax
        vmovd     r8d, xmm7
        vpextrd   ecx, xmm6, 2
        movsxd    rcx, ecx
        vmovsd    xmm8, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rax]
        movsxd    r8, r8d
        vpextrd   r9d, xmm7, 2
        movsxd    r9, r9d
        vmovhpd   xmm8, xmm8, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rcx]
        vmovsd    xmm6, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r8]
        vpsrlq    xmm5, xmm13, 40
        vextractf128 xmm4, ymm13, 1
        vmovd     r10d, xmm5
        vmovhpd   xmm7, xmm6, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r9]
        vpsrlq    xmm4, xmm4, 40
        vmovd     eax, xmm4
        movsxd    r10, r10d
        vpextrd   r11d, xmm5, 2
        movsxd    r11, r11d
        movsxd    rax, eax
        vpextrd   ecx, xmm4, 2
        movsxd    rcx, ecx
        vmovsd    xmm6, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r10]
        vmovhpd   xmm5, xmm6, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r11]
        vmovsd    xmm6, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rax]
        vmovhpd   xmm4, xmm6, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rcx]
        vmovupd   ymm6, YMMWORD PTR [__svml_spow_ha_data_internal+13504]
        vcmplt_oqpd ymm14, ymm6, ymm14
        vcmplt_oqpd ymm6, ymm6, ymm13
        vinsertf128 ymm8, ymm8, xmm7, 1
        vmovupd   ymm7, YMMWORD PTR [__svml_spow_ha_data_internal+13568]
        vandpd    ymm13, ymm14, ymm7
        vandpd    ymm14, ymm6, ymm7
        vmovupd   ymm7, YMMWORD PTR [__svml_spow_ha_data_internal+13632]
        vorpd     ymm13, ymm13, ymm7
        vorpd     ymm14, ymm14, ymm7
        vsubpd    ymm11, ymm11, ymm13
        vsubpd    ymm12, ymm12, ymm14
        vmovupd   ymm14, YMMWORD PTR [__svml_spow_ha_data_internal+12928]
        vinsertf128 ymm5, ymm5, xmm4, 1
        vmovupd   ymm4, YMMWORD PTR [__svml_spow_ha_data_internal+13696]
        vmulpd    ymm7, ymm4, ymm11
        vmulpd    ymm6, ymm4, ymm12
        vmovupd   ymm4, YMMWORD PTR [__svml_spow_ha_data_internal+12864]
        vmulpd    ymm13, ymm4, ymm10
        vmulpd    ymm12, ymm4, ymm9
        vaddpd    ymm11, ymm14, ymm13
        vaddpd    ymm4, ymm14, ymm12
        vmulpd    ymm13, ymm10, ymm10
        vmulpd    ymm14, ymm9, ymm9
        vmulpd    ymm11, ymm11, ymm13
        vmulpd    ymm13, ymm4, ymm14
        vaddpd    ymm10, ymm10, ymm11
        vaddpd    ymm13, ymm9, ymm13
        vaddpd    ymm4, ymm8, ymm10
        vaddpd    ymm11, ymm5, ymm13
        vmovupd   ymm5, YMMWORD PTR [__svml_spow_ha_data_internal+30464]
        vaddpd    ymm14, ymm7, ymm4
        vaddpd    ymm8, ymm6, ymm11
        vmulpd    ymm14, ymm15, ymm14
        vmulpd    ymm13, ymm3, ymm8
        vmovupd   ymm3, YMMWORD PTR [__svml_spow_ha_data_internal+30400]
        vmovupd   ymm15, YMMWORD PTR [__svml_spow_ha_data_internal+30528]
        vmovups   xmm8, XMMWORD PTR [__svml_spow_ha_data_internal+30720]
        vmulpd    ymm6, ymm3, ymm14
        vmulpd    ymm3, ymm3, ymm13
        vsubpd    ymm9, ymm6, ymm15
        vsubpd    ymm10, ymm3, ymm15
        vaddpd    ymm11, ymm5, ymm9
        vaddpd    ymm4, ymm5, ymm10
        vmovups   xmm9, XMMWORD PTR [__svml_spow_ha_data_internal+30784]
        vsubpd    ymm7, ymm11, ymm5
        vsubpd    ymm5, ymm4, ymm5
        vsubpd    ymm6, ymm6, ymm7
        vsubpd    ymm7, ymm3, ymm5
        vmovupd   ymm3, YMMWORD PTR [__svml_spow_ha_data_internal+30592]
        vextractf128 xmm12, ymm13, 1
        vshufps   xmm10, xmm13, xmm12, 221
        vmovups   xmm12, XMMWORD PTR [__svml_spow_ha_data_internal+64]
        vpcmpgtd  xmm13, xmm0, xmm12
        vpcmpeqd  xmm0, xmm0, xmm12
        vextractf128 xmm15, ymm14, 1
        vshufps   xmm15, xmm14, xmm15, 221
        vpor      xmm14, xmm13, xmm0
        vmovups   xmm0, XMMWORD PTR [__svml_spow_ha_data_internal+128]
        vpcmpgtd  xmm13, xmm2, xmm0
        vpcmpeqd  xmm2, xmm2, xmm0
        vpor      xmm13, xmm13, xmm2
        vandps    xmm2, xmm15, xmm8
        vpor      xmm14, xmm14, xmm13
        vpcmpgtd  xmm13, xmm1, xmm12
        vpcmpeqd  xmm1, xmm1, xmm12
        vpcmpgtd  xmm15, xmm2, xmm9
        vmovdqu   xmm12, XMMWORD PTR [32+rsp]
        vpor      xmm2, xmm13, xmm1
        vpcmpgtd  xmm13, xmm12, xmm0
        vpcmpeqd  xmm0, xmm12, xmm0
        vpor      xmm1, xmm13, xmm0
        vandps    xmm8, xmm10, xmm8
        vpor      xmm12, xmm2, xmm1
        vpcmpgtd  xmm13, xmm8, xmm9
        vpor      xmm14, xmm14, xmm15
        vpor      xmm0, xmm12, xmm13
        vpackssdw xmm2, xmm14, xmm0
        vpxor     xmm1, xmm1, xmm1
        vmovupd   xmm15, XMMWORD PTR [__svml_spow_ha_data_internal+30656]
        vpacksswb xmm14, xmm2, xmm1
        vpmovmskb eax, xmm14
        vmulpd    ymm13, ymm3, ymm6
        vmulpd    ymm14, ymm3, ymm7
        vandps    xmm10, xmm11, xmm15
        vextractf128 xmm1, ymm11, 1
        vpsrlq    xmm11, xmm11, 11
        vmovd     r8d, xmm10
        vpsllq    xmm11, xmm11, 52
        vandps    xmm9, xmm1, xmm15
        vpsrlq    xmm1, xmm1, 11
        vmovd     r10d, xmm9
        shl       r8d, 3
        vmovq     xmm3, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r8]
        vpextrd   r9d, xmm10, 2
        shl       r10d, 3
        vpextrd   ecx, xmm9, 2
        shl       r9d, 3
        shl       ecx, 3
        vmovq     xmm6, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r10]
        vmovhpd   xmm5, xmm3, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r9]
        vmovhpd   xmm7, xmm6, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+rcx]
        vandps    xmm8, xmm4, xmm15
        vextractf128 xmm0, ymm4, 1
        vpsrlq    xmm4, xmm4, 11
        vmovd     r8d, xmm8
        vpsllq    xmm4, xmm4, 52
        vandps    xmm2, xmm0, xmm15
        vpsrlq    xmm0, xmm0, 11
        vmovd     r11d, xmm2
        shl       r8d, 3
        vpextrd   r9d, xmm8, 2
        shl       r11d, 3
        vpextrd   ecx, xmm2, 2
        shl       r9d, 3
        vmovq     xmm9, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r8]
        shl       ecx, 3
        vmovq     xmm10, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r11]
        vmovhpd   xmm8, xmm9, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r9]
        vmovhpd   xmm2, xmm10, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+rcx]
        vinsertf128 ymm12, ymm5, xmm7, 1
        vpsllq    xmm7, xmm1, 52
        vmulpd    ymm13, ymm12, ymm13
        vpsllq    xmm1, xmm0, 52
        vaddpd    ymm3, ymm12, ymm13
        vinsertf128 ymm15, ymm8, xmm2, 1
        vmulpd    ymm14, ymm15, ymm14
        vaddpd    ymm2, ymm15, ymm14
        vextractf128 xmm6, ymm3, 1
        vpaddq    xmm5, xmm3, xmm11
        vpaddq    xmm8, xmm6, xmm7
        vextractf128 xmm13, ymm2, 1
        vpaddq    xmm2, xmm2, xmm4
        vpaddq    xmm13, xmm13, xmm1
        vinsertf128 ymm9, ymm5, xmm8, 1
        vcvtpd2ps xmm3, ymm9
        vinsertf128 ymm5, ymm2, xmm13, 1
        vcvtpd2ps xmm6, ymm5
        vinsertf128 ymm0, ymm3, xmm6, 1
        test      al, al
        jne       _B4_3

_B4_2::

        vmovups   ymm6, YMMWORD PTR [432+rsp]
        vmovups   ymm7, YMMWORD PTR [368+rsp]
        vmovups   ymm8, YMMWORD PTR [336+rsp]
        vmovups   ymm9, YMMWORD PTR [304+rsp]
        vmovups   ymm10, YMMWORD PTR [272+rsp]
        vmovups   ymm11, YMMWORD PTR [560+rsp]
        vmovups   ymm12, YMMWORD PTR [528+rsp]
        vmovups   ymm13, YMMWORD PTR [496+rsp]
        vmovups   ymm14, YMMWORD PTR [464+rsp]
        vmovups   ymm15, YMMWORD PTR [400+rsp]
        mov       r13, QWORD PTR [592+rsp]
        add       rsp, 616
        ret

_B4_3::

        vmovups   ymm1, YMMWORD PTR [32+r13]
        vmovups   ymm2, YMMWORD PTR [96+r13]
        vmovups   YMMWORD PTR [128+r13], ymm0
        vmovups   YMMWORD PTR [r13], ymm1
        vmovups   YMMWORD PTR [64+r13], ymm2
        test      eax, eax
        je        _B4_2

_B4_6::

        xor       edx, edx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, edx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

_B4_7::

        bt        esi, ebx
        jc        _B4_10

_B4_8::

        inc       ebx
        cmp       ebx, 8
        jl        _B4_7

_B4_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [128+r13]
        jmp       _B4_2

_B4_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]
        lea       r8, QWORD PTR [128+r13+rbx*4]

        call      __svml_spow_ha_cout_rare_internal
        jmp       _B4_8
        ALIGN     16

_B4_11::

__svml_powf8_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_powf8_ha_e9_B1_B3:
	DD	1604097
	DD	4904058
	DD	1796204
	DD	1538147
	DD	1411162
	DD	1284177
	DD	1157192
	DD	2340927
	DD	2213942
	DD	2086957
	DD	1959972
	DD	1701915
	DD	5046539

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_powf8_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_powf8_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_powf8_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_6
	DD	imagerel _B4_11
	DD	imagerel _unwind___svml_powf8_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_powf4_ha_l9

__svml_powf4_ha_l9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L133::

        sub       rsp, 392
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [256+rsp], xmm9
        vcvtps2pd ymm9, xmm0
        vmovups   XMMWORD PTR [320+rsp], xmm15
        vmovups   XMMWORD PTR [352+rsp], xmm14
        vmovups   XMMWORD PTR [272+rsp], xmm11
        vmovups   XMMWORD PTR [288+rsp], xmm10
        vmovups   XMMWORD PTR [336+rsp], xmm8
        vmovaps   xmm8, xmm1
        vmovups   XMMWORD PTR [304+rsp], xmm7
        mov       QWORD PTR [368+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vandpd    ymm11, ymm9, YMMWORD PTR [__svml_spow_ha_data_internal+12992]
        and       r13, -64
        vorpd     ymm1, ymm11, YMMWORD PTR [__svml_spow_ha_data_internal+13056]
        vcvtpd2ps xmm14, ymm1
        vpsrlq    ymm11, ymm9, 20
        vcvtps2pd ymm2, xmm8
        vmovups   xmm4, XMMWORD PTR [__svml_spow_ha_data_internal+128]
        vpand     xmm5, xmm8, XMMWORD PTR [__svml_spow_ha_data_internal+192]
        vmovups   xmm3, XMMWORD PTR [__svml_spow_ha_data_internal+64]
        vrcpps    xmm10, xmm14
        vcvtps2pd ymm15, xmm10
        vpsubd    xmm7, xmm0, XMMWORD PTR [__svml_spow_ha_data_internal]
        vroundpd  ymm15, ymm15, 0
        vfmsub213pd ymm1, ymm15, YMMWORD PTR [__svml_spow_ha_data_internal+13312]
        mov       QWORD PTR [376+rsp], r13
        vextracti128 xmm14, ymm11, 1
        vshufps   xmm10, xmm11, xmm14, 221
        vcvtdq2pd ymm9, xmm10
        vpsrlq    ymm10, ymm15, 40
        vcmpgt_oqpd ymm15, ymm15, YMMWORD PTR [__svml_spow_ha_data_internal+13504]
        vmovd     eax, xmm10
        movsxd    rax, eax
        vpextrd   ecx, xmm10, 2
        movsxd    rcx, ecx
        vmovsd    xmm11, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rax]
        vmovhpd   xmm14, xmm11, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rcx]
        vextracti128 xmm11, ymm10, 1
        vmovd     r8d, xmm11
        movsxd    r8, r8d
        vpextrd   r9d, xmm11, 2
        movsxd    r9, r9d
        vmovsd    xmm10, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r8]
        vmovhpd   xmm11, xmm10, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r9]
        vinsertf128 ymm11, ymm14, xmm11, 1
        vandpd    ymm14, ymm15, YMMWORD PTR [__svml_spow_ha_data_internal+13568]
        vorpd     ymm10, ymm14, YMMWORD PTR [__svml_spow_ha_data_internal+13632]
        vmovupd   ymm14, YMMWORD PTR [__svml_spow_ha_data_internal+12864]
        vsubpd    ymm9, ymm9, ymm10
        vmulpd    ymm10, ymm1, ymm1
        vfmadd213pd ymm14, ymm1, YMMWORD PTR [__svml_spow_ha_data_internal+12928]
        vmulpd    ymm15, ymm9, YMMWORD PTR [__svml_spow_ha_data_internal+13696]
        vmulpd    ymm9, ymm14, ymm10
        vmovupd   ymm10, YMMWORD PTR [__svml_spow_ha_data_internal+30464]
        vaddpd    ymm1, ymm1, ymm9
        vaddpd    ymm11, ymm11, ymm1
        vaddpd    ymm14, ymm15, ymm11
        vmulpd    ymm9, ymm2, ymm14
        vmulpd    ymm14, ymm9, YMMWORD PTR [__svml_spow_ha_data_internal+30400]
        vsubpd    ymm2, ymm14, YMMWORD PTR [__svml_spow_ha_data_internal+30528]
        vaddpd    ymm11, ymm10, ymm2
        vpcmpgtd  xmm2, xmm7, xmm3
        vpcmpeqd  xmm3, xmm7, xmm3
        vpor      xmm2, xmm2, xmm3
        vsubpd    ymm10, ymm11, ymm10
        vsubpd    ymm10, ymm14, ymm10
        vextracti128 xmm1, ymm9, 1
        vshufps   xmm9, xmm9, xmm1, 221
        vpcmpgtd  xmm1, xmm5, xmm4
        vpcmpeqd  xmm4, xmm5, xmm4
        vpor      xmm5, xmm1, xmm4
        vandps    ymm4, ymm11, YMMWORD PTR [__svml_spow_ha_data_internal+30656]
        vpor      xmm1, xmm2, xmm5
        vandps    xmm9, xmm9, XMMWORD PTR [__svml_spow_ha_data_internal+30720]
        vpcmpgtd  xmm2, xmm9, XMMWORD PTR [__svml_spow_ha_data_internal+30784]
        vpor      xmm3, xmm1, xmm2
        vmulpd    ymm1, ymm10, YMMWORD PTR [__svml_spow_ha_data_internal+30592]
        vpsrlq    ymm11, ymm11, 11
        vmovmskps eax, xmm3
        vextracti128 xmm14, ymm4, 1
        vmovd     r10d, xmm4
        vmovd     ecx, xmm14
        shl       r10d, 3
        vpextrd   r11d, xmm4, 2
        shl       ecx, 3
        vpextrd   r8d, xmm14, 2
        shl       r11d, 3
        vmovq     xmm5, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r10]
        shl       r8d, 3
        vmovq     xmm15, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+rcx]
        vmovhpd   xmm7, xmm5, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r11]
        vmovhpd   xmm4, xmm15, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r8]
        vinsertf128 ymm2, ymm7, xmm4, 1
        vfmadd213pd ymm2, ymm1, ymm2
        vpsllq    ymm1, ymm11, 52
        vpaddq    ymm3, ymm2, ymm1
        vcvtpd2ps xmm1, ymm3
        test      eax, eax
        jne       _B5_3

_B5_2::

        vmovaps   xmm0, xmm1
        mov       r13, QWORD PTR [368+rsp]
        vzeroupper
        vmovups   xmm7, XMMWORD PTR [304+rsp]
        vmovups   xmm8, XMMWORD PTR [336+rsp]
        vmovups   xmm9, XMMWORD PTR [256+rsp]
        vmovups   xmm10, XMMWORD PTR [288+rsp]
        vmovups   xmm11, XMMWORD PTR [272+rsp]
        vmovups   xmm14, XMMWORD PTR [352+rsp]
        vmovups   xmm15, XMMWORD PTR [320+rsp]
        add       rsp, 392
        ret

_B5_3::

        vmovups   XMMWORD PTR [r13], xmm0
        vmovups   XMMWORD PTR [64+r13], xmm8
        vmovups   XMMWORD PTR [128+r13], xmm1

_B5_6::

        xor       edx, edx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, edx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

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
        vmovups   xmm1, XMMWORD PTR [128+r13]
        jmp       _B5_2

_B5_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]
        lea       r8, QWORD PTR [128+r13+rbx*4]

        call      __svml_spow_ha_cout_rare_internal
        jmp       _B5_8
        ALIGN     16

_B5_11::

__svml_powf4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_powf4_ha_l9_B1_B3:
	DD	1204481
	DD	3069025
	DD	1275993
	DD	1411148
	DD	1222723
	DD	1161274
	DD	1501233
	DD	1374248
	DD	1087515
	DD	3211531

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_powf4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_powf4_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_powf4_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_6
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_powf4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_powf4_ha_ex

__svml_powf4_ha_ex	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L154::

        sub       rsp, 456
        movaps    xmm3, xmm0
        movups    XMMWORD PTR [384+rsp], xmm12
        lea       rdx, QWORD PTR [__ImageBase]
        cvtps2pd  xmm12, xmm0
        movhlps   xmm3, xmm0
        movups    XMMWORD PTR [320+rsp], xmm9
        movaps    xmm9, xmm1
        cvtps2pd  xmm1, xmm3
        movaps    xmm5, xmm9
        movups    XMMWORD PTR [256+rsp], xmm15
        movups    XMMWORD PTR [272+rsp], xmm14
        movups    XMMWORD PTR [288+rsp], xmm13
        movups    XMMWORD PTR [400+rsp], xmm11
        movups    XMMWORD PTR [304+rsp], xmm10
        movups    XMMWORD PTR [336+rsp], xmm8
        movups    XMMWORD PTR [352+rsp], xmm7
        movups    XMMWORD PTR [368+rsp], xmm6
        mov       QWORD PTR [432+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movhlps   xmm5, xmm9
        and       r13, -64
        cvtps2pd  xmm7, xmm5
        cvtps2pd  xmm8, xmm9
        movups    xmm5, XMMWORD PTR [__svml_spow_ha_data_internal+12992]
        movaps    xmm3, xmm5
        andps     xmm5, xmm1
        movups    xmm4, XMMWORD PTR [__svml_spow_ha_data_internal+13056]
        andps     xmm3, xmm12
        orps      xmm3, xmm4
        orps      xmm5, xmm4
        cvtpd2ps  xmm2, xmm3
        cvtpd2ps  xmm14, xmm5
        movlhps   xmm2, xmm2
        psrlq     xmm12, 20
        rcpps     xmm15, xmm2
        movlhps   xmm14, xmm14
        psrlq     xmm1, 20
        rcpps     xmm13, xmm14
        cvtps2pd  xmm4, xmm15
        cvtps2pd  xmm2, xmm13
        movups    xmm11, XMMWORD PTR [_2il0floatpacket_40]
        addpd     xmm4, xmm11
        addpd     xmm2, xmm11
        subpd     xmm4, xmm11
        subpd     xmm2, xmm11
        mulpd     xmm3, xmm4
        mulpd     xmm5, xmm2
        shufps    xmm12, xmm1, 221
        movaps    xmm14, xmm4
        cvtdq2pd  xmm1, xmm12
        pshufd    xmm12, xmm12, 238
        psrlq     xmm14, 40
        movaps    xmm13, xmm2
        cvtdq2pd  xmm15, xmm12
        movd      eax, xmm14
        pshufd    xmm12, xmm14, 2
        psrlq     xmm13, 40
        movd      ecx, xmm12
        pshufd    xmm12, xmm13, 2
        movups    xmm14, XMMWORD PTR [__svml_spow_ha_data_internal+13568]
        movd      r8d, xmm13
        movups    xmm13, XMMWORD PTR [__svml_spow_ha_data_internal+13504]
        movups    XMMWORD PTR [416+rsp], xmm14
        movaps    xmm14, xmm13
        cmpltpd   xmm14, xmm4
        cmpltpd   xmm13, xmm2
        movd      r9d, xmm12
        movups    xmm10, XMMWORD PTR [__svml_spow_ha_data_internal+13312]
        subpd     xmm3, xmm10
        subpd     xmm5, xmm10
        movups    xmm2, XMMWORD PTR [416+rsp]
        movups    xmm4, XMMWORD PTR [__svml_spow_ha_data_internal+13632]
        andps     xmm14, xmm2
        andps     xmm13, xmm2
        orps      xmm14, xmm4
        subpd     xmm1, xmm14
        movups    xmm14, XMMWORD PTR [__svml_spow_ha_data_internal+12864]
        orps      xmm13, xmm4
        subpd     xmm15, xmm13
        movaps    xmm2, xmm14
        movaps    xmm13, xmm5
        mulpd     xmm2, xmm3
        mulpd     xmm14, xmm5
        mulpd     xmm13, xmm5
        movups    xmm12, XMMWORD PTR [__svml_spow_ha_data_internal+13696]
        mulpd     xmm1, xmm12
        mulpd     xmm12, xmm15
        movaps    xmm15, xmm3
        mulpd     xmm15, xmm3
        movups    xmm4, XMMWORD PTR [__svml_spow_ha_data_internal+12928]
        addpd     xmm2, xmm4
        addpd     xmm4, xmm14
        mulpd     xmm2, xmm15
        mulpd     xmm4, xmm13
        addpd     xmm3, xmm2
        addpd     xmm5, xmm4
        movsxd    rax, eax
        movsxd    r8, r8d
        movsxd    rcx, ecx
        movsxd    r9, r9d
        movsd     xmm11, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rax]
        movsd     xmm10, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r8]
        movhpd    xmm11, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+rcx]
        movhpd    xmm10, QWORD PTR [imagerel(__svml_spow_ha_data_internal)-4218496+rdx+r9]
        addpd     xmm11, xmm3
        addpd     xmm10, xmm5
        addpd     xmm1, xmm11
        addpd     xmm12, xmm10
        mulpd     xmm8, xmm1
        mulpd     xmm7, xmm12
        movups    xmm13, XMMWORD PTR [__svml_spow_ha_data_internal+30400]
        movaps    xmm12, xmm13
        mulpd     xmm12, xmm8
        mulpd     xmm13, xmm7
        movups    xmm1, XMMWORD PTR [__svml_spow_ha_data_internal+30528]
        movaps    xmm14, xmm12
        movaps    xmm15, xmm13
        subpd     xmm14, xmm1
        subpd     xmm15, xmm1
        movups    xmm3, XMMWORD PTR [__svml_spow_ha_data_internal+30464]
        addpd     xmm14, xmm3
        addpd     xmm15, xmm3
        movaps    xmm10, xmm14
        movaps    xmm11, xmm15
        subpd     xmm10, xmm3
        subpd     xmm11, xmm3
        subpd     xmm12, xmm10
        subpd     xmm13, xmm11
        movups    XMMWORD PTR [32+rsp], xmm0
        psubd     xmm0, XMMWORD PTR [__svml_spow_ha_data_internal]
        movdqu    xmm5, XMMWORD PTR [__svml_spow_ha_data_internal+64]
        movdqa    xmm1, xmm0
        movdqu    xmm6, XMMWORD PTR [__svml_spow_ha_data_internal+192]
        pcmpgtd   xmm1, xmm5
        pand      xmm6, xmm9
        pcmpeqd   xmm0, xmm5
        shufps    xmm8, xmm7, 221
        por       xmm1, xmm0
        movdqu    xmm0, XMMWORD PTR [__svml_spow_ha_data_internal+128]
        movdqa    xmm7, xmm6
        pcmpgtd   xmm7, xmm0
        pcmpeqd   xmm6, xmm0
        pand      xmm8, XMMWORD PTR [__svml_spow_ha_data_internal+30720]
        por       xmm7, xmm6
        pcmpgtd   xmm8, XMMWORD PTR [__svml_spow_ha_data_internal+30784]
        por       xmm1, xmm7
        movups    xmm0, XMMWORD PTR [__svml_spow_ha_data_internal+30592]
        por       xmm1, xmm8
        movups    xmm8, XMMWORD PTR [__svml_spow_ha_data_internal+30656]
        movaps    xmm6, xmm14
        mulpd     xmm12, xmm0
        mulpd     xmm0, xmm13
        movmskps  eax, xmm1
        andps     xmm6, xmm8
        andps     xmm8, xmm15
        psrlq     xmm14, 11
        psrlq     xmm15, 11
        movd      r10d, xmm6
        psllq     xmm14, 52
        movd      ecx, xmm8
        psllq     xmm15, 52
        pextrw    r11d, xmm6, 4
        pextrw    r8d, xmm8, 4
        shl       r10d, 3
        shl       ecx, 3
        shl       r11d, 3
        shl       r8d, 3
        movq      xmm1, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r10]
        movq      xmm2, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+rcx]
        movhpd    xmm1, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r11]
        movhpd    xmm2, QWORD PTR [imagerel(__svml_spow_ha_data_internal)+13952+rdx+r8]
        mulpd     xmm12, xmm1
        mulpd     xmm0, xmm2
        addpd     xmm1, xmm12
        addpd     xmm2, xmm0
        paddq     xmm1, xmm14
        paddq     xmm2, xmm15
        cvtpd2ps  xmm0, xmm1
        cvtpd2ps  xmm3, xmm2
        mov       QWORD PTR [440+rsp], r13
        movlhps   xmm0, xmm3
        test      eax, eax
        jne       _B6_3

_B6_2::

        movups    xmm6, XMMWORD PTR [368+rsp]
        movups    xmm7, XMMWORD PTR [352+rsp]
        movups    xmm8, XMMWORD PTR [336+rsp]
        movups    xmm9, XMMWORD PTR [320+rsp]
        movups    xmm10, XMMWORD PTR [304+rsp]
        movups    xmm11, XMMWORD PTR [400+rsp]
        movups    xmm12, XMMWORD PTR [384+rsp]
        movups    xmm13, XMMWORD PTR [288+rsp]
        movups    xmm14, XMMWORD PTR [272+rsp]
        movups    xmm15, XMMWORD PTR [256+rsp]
        mov       r13, QWORD PTR [432+rsp]
        add       rsp, 456
        ret

_B6_3::

        movups    xmm1, XMMWORD PTR [32+rsp]
        movups    XMMWORD PTR [r13], xmm1
        movups    XMMWORD PTR [64+r13], xmm9
        movups    XMMWORD PTR [128+r13], xmm0

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
        movups    xmm0, XMMWORD PTR [128+r13]
        jmp       _B6_2

_B6_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]
        lea       r8, QWORD PTR [128+r13+rbx*4]

        call      __svml_spow_ha_cout_rare_internal
        jmp       _B6_8
        ALIGN     16

_B6_11::

__svml_powf4_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_powf4_ha_ex_B1_B3:
	DD	1607425
	DD	3593351
	DD	1534079
	DD	1472631
	DD	1411183
	DD	1288294
	DD	1685597
	DD	1235028
	DD	1173579
	DD	1112130
	DD	1349678
	DD	1624087
	DD	3735819

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_powf4_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_powf4_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_powf4_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_6
	DD	imagerel _B6_11
	DD	imagerel _unwind___svml_powf4_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_spow_ha_cout_rare_internal

__svml_spow_ha_cout_rare_internal	PROC

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L185::

        push      rbx
        push      rsi
        push      r14
        push      rbp
        sub       rsp, 152
        xor       eax, eax
        movss     xmm4, DWORD PTR [rcx]
        pxor      xmm0, xmm0
        movss     xmm3, DWORD PTR [rdx]
        movaps    xmm1, xmm4
        movups    XMMWORD PTR [112+rsp], xmm15
        movaps    xmm2, xmm3
        movups    XMMWORD PTR [128+rsp], xmm14
        movups    XMMWORD PTR [96+rsp], xmm8
        movups    XMMWORD PTR [64+rsp], xmm7
        movups    XMMWORD PTR [80+rsp], xmm6
        ucomiss   xmm4, xmm0
        jp        _B7_2
        je        _B7_3

_B7_2::

        movss     DWORD PTR [40+rsp], xmm4
        jmp       _B7_4

_B7_3::

        movaps    xmm1, xmm4
        addss     xmm1, xmm4
        movss     DWORD PTR [40+rsp], xmm1

_B7_4::

        ucomiss   xmm3, xmm0
        jp        _B7_5
        je        _B7_6

_B7_5::

        movss     DWORD PTR [60+rsp], xmm3
        jmp       _B7_7

_B7_6::

        movaps    xmm2, xmm3
        addss     xmm2, xmm3
        movss     DWORD PTR [60+rsp], xmm2

_B7_7::

        movd      ebp, xmm2
        xor       r10d, r10d
        test      DWORD PTR [40+rsp], 8388607
        movzx     ebx, WORD PTR [62+rsp]
        sete      r10b
        xor       r9d, r9d
        xor       esi, esi
        and       ebp, 2147483647
        and       ebx, 32640
        test      DWORD PTR [60+rsp], 8388607
        movzx     r14d, WORD PTR [42+rsp]
        sete      r9b
        and       r14d, 32640
        shr       ebx, 7
        lea       ecx, DWORD PTR [-1065353216+rbp]
        cmp       ebx, 255
        movzx     r11d, BYTE PTR [43+rsp]
        mov       dl, BYTE PTR [63+rsp]
        setne     sil
        and       r11d, 128
        and       dl, -128
        shr       r14d, 7
        shr       r11d, 7
        shr       dl, 7
        cmp       ecx, 1073741824
        jae       _B7_12

_B7_8::

        cmp       ebp, 1266679808
        jge       _B7_45

_B7_9::

        mov       ecx, ebp
        and       ecx, -8388608
        add       ecx, 8388608
        shr       ecx, 23
        shl       ebp, cl
        test      ebp, 8388607
        jne       _B7_12

_B7_10::

        and       ebp, 16777215
        cmp       ebp, 8388608
        mov       ebp, 0
        setne     bpl
        inc       ebp
        jmp       _B7_13

_B7_12::

        xor       ebp, ebp

_B7_13::

        test      r11d, r11d
        jne       _B7_15

_B7_14::

        cmp       r14d, 127
        je        _B7_62

_B7_15::

        test      ebx, ebx
        jne       _B7_17

_B7_16::

        test      r9d, r9d
        jne       _B7_43

_B7_17::

        cmp       r14d, 255
        je        _B7_19

_B7_18::

        mov       ecx, 1
        jmp       _B7_20

_B7_19::

        xor       ecx, ecx

_B7_20::

        or        r10d, ecx
        je        _B7_42

_B7_21::

        or        r9d, esi
        je        _B7_42

_B7_22::

        ucomiss   xmm1, xmm0
        jp        _B7_23
        je        _B7_59

_B7_23::

        ucomiss   xmm1, DWORD PTR [_2il0floatpacket_143]
        jp        _B7_24
        je        _B7_56

_B7_24::

        test      ecx, ecx
        je        _B7_35

_B7_25::

        test      esi, esi
        je        _B7_35

_B7_26::

        comiss    xmm1, xmm0
        ja        _B7_28

_B7_27::

        test      ebp, ebp
        je        _B7_34

_B7_28::

        lea       rcx, QWORD PTR [__ImageBase]
        and       ebp, r11d
        movss     DWORD PTR [44+rsp], xmm1
        and       BYTE PTR [47+rsp], 127
        movss     xmm14, DWORD PTR [imagerel(_vmlsPowHATab)+1484+rcx+rbp*4]
        xor       ecx, ecx
        test      r14d, r14d
        jne       _B7_30

_B7_29::

        movss     xmm0, DWORD PTR [44+rsp]
        mov       ecx, -64
        mulss     xmm0, DWORD PTR [_2il0floatpacket_144]
        movss     DWORD PTR [44+rsp], xmm0
        jmp       _B7_31

_B7_30::

        movss     xmm0, DWORD PTR [44+rsp]

_B7_31::

        movzx     r9d, WORD PTR [46+rsp]
        lea       r14, QWORD PTR [__ImageBase]
        and       r9d, 32640
        pxor      xmm8, xmm8
        shr       r9d, 7
        mov       r11d, DWORD PTR [44+rsp]
        movss     DWORD PTR [48+rsp], xmm0
        and       r11d, 8388607
        shl       r9d, 23
        movzx     r10d, WORD PTR [50+rsp]
        or        r9d, r11d
        and       r10d, -32641
        add       r9d, -1060634624
        add       r10d, 16256
        mov       r11, r14
        mov       WORD PTR [50+rsp], r10w
        mov       r10, r14
        sar       r9d, 23
        mov       esi, DWORD PTR [48+rsp]
        add       r9d, ecx
        and       esi, 7864320
        mov       rcx, r14
        add       esi, 524288
        cvtsi2ss  xmm8, r9d
        shr       esi, 20
        mov       r9, r14
        movss     xmm7, DWORD PTR [imagerel(_vmlsPowHATab)+r14+rsi*4]
        add       esi, esi
        movss     xmm4, DWORD PTR [imagerel(_vmlsPowHATab)+36+rcx+rsi*4]
        movaps    xmm15, xmm4
        movss     xmm5, DWORD PTR [48+rsp]
        addss     xmm15, xmm8
        movss     DWORD PTR [52+rsp], xmm15
        movaps    xmm6, xmm5
        movss     xmm15, DWORD PTR [52+rsp]
        mulss     xmm6, xmm7
        subss     xmm8, xmm15
        movd      ebp, xmm6
        addss     xmm4, xmm8
        and       ebp, 1966080
        add       ebp, 131072
        shr       ebp, 18
        movss     DWORD PTR [56+rsp], xmm4
        movss     xmm3, DWORD PTR [imagerel(_vmlsPowHATab)+108+r10+rbp*4]
        add       ebp, ebp
        movss     xmm15, DWORD PTR [52+rsp]
        movss     xmm0, DWORD PTR [imagerel(_vmlsPowHATab)+144+r11+rbp*4]
        movaps    xmm4, xmm15
        movss     xmm8, DWORD PTR [56+rsp]
        addss     xmm4, xmm0
        mulss     xmm6, xmm3
        mulss     xmm7, xmm3
        movd      ecx, xmm6
        movss     DWORD PTR [52+rsp], xmm4
        movss     xmm4, DWORD PTR [52+rsp]
        and       ecx, 507904
        subss     xmm15, xmm4
        add       ecx, 16384
        addss     xmm15, xmm0
        shr       ecx, 15
        movss     DWORD PTR [56+rsp], xmm15
        movss     xmm2, DWORD PTR [imagerel(_vmlsPowHATab)+216+r14+rcx*4]
        add       ecx, ecx
        movss     xmm4, DWORD PTR [52+rsp]
        movss     xmm1, DWORD PTR [imagerel(_vmlsPowHATab)+284+r10+rcx*4]
        movaps    xmm15, xmm4
        movss     xmm0, DWORD PTR [56+rsp]
        addss     xmm15, xmm1
        addss     xmm8, xmm0
        mulss     xmm7, xmm2
        movss     DWORD PTR [52+rsp], xmm15
        movss     xmm15, DWORD PTR [52+rsp]
        subss     xmm4, xmm15
        mulss     xmm6, xmm2
        addss     xmm4, xmm1
        movss     xmm2, DWORD PTR [_2il0floatpacket_146]
        movaps    xmm0, xmm6
        movaps    xmm3, xmm2
        subss     xmm0, DWORD PTR [_2il0floatpacket_145]
        mulss     xmm3, xmm5
        movss     DWORD PTR [56+rsp], xmm4
        movss     xmm4, DWORD PTR [52+rsp]
        movss     xmm1, DWORD PTR [56+rsp]
        movss     DWORD PTR [52+rsp], xmm3
        addss     xmm8, xmm1
        movss     xmm1, DWORD PTR [52+rsp]
        subss     xmm1, DWORD PTR [48+rsp]
        addss     xmm8, DWORD PTR [imagerel(_vmlsPowHATab)+40+r9+rsi*4]
        movss     DWORD PTR [56+rsp], xmm1
        mov       rsi, r14
        movss     xmm1, DWORD PTR [52+rsp]
        movss     xmm15, DWORD PTR [56+rsp]
        subss     xmm1, xmm15
        addss     xmm8, DWORD PTR [imagerel(_vmlsPowHATab)+148+rsi+rbp*4]
        movss     DWORD PTR [52+rsp], xmm1
        mov       rbp, r14
        movss     xmm3, DWORD PTR [52+rsp]
        subss     xmm5, xmm3
        addss     xmm8, DWORD PTR [imagerel(_vmlsPowHATab)+288+rbp+rcx*4]
        movss     DWORD PTR [56+rsp], xmm5
        movaps    xmm5, xmm7
        mulss     xmm5, xmm2
        movss     xmm15, DWORD PTR [52+rsp]
        movss     xmm1, DWORD PTR [56+rsp]
        movss     DWORD PTR [52+rsp], xmm5
        movss     xmm2, DWORD PTR [52+rsp]
        subss     xmm2, xmm7
        movss     DWORD PTR [56+rsp], xmm2
        movss     xmm2, DWORD PTR [52+rsp]
        movss     xmm3, DWORD PTR [56+rsp]
        subss     xmm2, xmm3
        movaps    xmm3, xmm15
        movss     DWORD PTR [52+rsp], xmm2
        movss     xmm5, DWORD PTR [52+rsp]
        subss     xmm7, xmm5
        movss     DWORD PTR [56+rsp], xmm7
        movss     xmm7, DWORD PTR [52+rsp]
        mulss     xmm3, xmm7
        mulss     xmm7, xmm1
        subss     xmm3, xmm6
        movaps    xmm6, xmm0
        addss     xmm3, xmm7
        addss     xmm6, xmm4
        movss     xmm2, DWORD PTR [56+rsp]
        movss     DWORD PTR [52+rsp], xmm6
        mulss     xmm15, xmm2
        mulss     xmm1, xmm2
        addss     xmm3, xmm15
        movss     xmm2, DWORD PTR [52+rsp]
        addss     xmm1, xmm3
        subss     xmm4, xmm2
        addss     xmm4, xmm0
        movss     DWORD PTR [56+rsp], xmm4
        movss     xmm3, DWORD PTR [52+rsp]
        movss     DWORD PTR [40+rsp], xmm3
        movzx     ecx, WORD PTR [42+rsp]
        and       ecx, 32640
        shr       ecx, 7
        add       ecx, ebx
        movss     xmm2, DWORD PTR [56+rsp]
        cmp       ecx, 265
        jge       _B7_55

_B7_32::

        cmp       ecx, 192
        jg        _B7_46

_B7_33::

        mov       DWORD PTR [52+rsp], 1065353216
        movss     xmm0, DWORD PTR [52+rsp]
        addss     xmm0, DWORD PTR [_2il0floatpacket_155]
        movss     DWORD PTR [52+rsp], xmm0
        movss     xmm1, DWORD PTR [52+rsp]
        mulss     xmm14, xmm1
        movss     DWORD PTR [r8], xmm14
        jmp       _B7_44

_B7_34::

        movss     DWORD PTR [52+rsp], xmm0
        movss     xmm1, DWORD PTR [52+rsp]
        movss     xmm0, DWORD PTR [52+rsp]
        divss     xmm1, xmm0
        movss     DWORD PTR [52+rsp], xmm1
        mov       eax, DWORD PTR [52+rsp]
        mov       DWORD PTR [r8], eax
        mov       eax, 1
        jmp       _B7_44

_B7_35::

        cmp       r14d, 127
        jge       _B7_39

_B7_36::

        test      dl, dl
        je        _B7_38

_B7_37::

        mulss     xmm2, xmm2
        movss     DWORD PTR [r8], xmm2
        jmp       _B7_44

_B7_38::

        movss     DWORD PTR [r8], xmm0
        jmp       _B7_44

_B7_39::

        test      dl, dl
        je        _B7_41

_B7_40::

        lea       rdx, QWORD PTR [__ImageBase]
        and       ebp, r11d
        mov       ecx, DWORD PTR [imagerel(_vmlsPowHATab)+1512+rdx+rbp*4]
        mov       DWORD PTR [44+rsp], ecx
        mov       DWORD PTR [r8], ecx
        jmp       _B7_44

_B7_41::

        mulss     xmm1, xmm1
        lea       rdx, QWORD PTR [__ImageBase]
        mulss     xmm2, xmm1
        and       ebp, r11d
        mulss     xmm2, DWORD PTR [imagerel(_vmlsPowHATab)+1484+rdx+rbp*4]
        movss     DWORD PTR [r8], xmm2
        jmp       _B7_44

_B7_42::

        addss     xmm4, xmm3
        movss     DWORD PTR [r8], xmm4
        jmp       _B7_44

_B7_43::

        addss     xmm1, xmm2
        movss     DWORD PTR [52+rsp], xmm1
        mov       DWORD PTR [56+rsp], 1065353216
        mov       dl, BYTE PTR [55+rsp]
        mov       cl, BYTE PTR [59+rsp]
        and       dl, -128
        and       cl, 127
        or        cl, dl
        mov       BYTE PTR [59+rsp], cl
        movss     xmm1, DWORD PTR [56+rsp]
        movss     xmm0, DWORD PTR [56+rsp]
        mulss     xmm1, xmm0
        movss     DWORD PTR [r8], xmm1

_B7_44::

        movups    xmm6, XMMWORD PTR [80+rsp]
        movups    xmm7, XMMWORD PTR [64+rsp]
        movups    xmm8, XMMWORD PTR [96+rsp]
        movups    xmm14, XMMWORD PTR [128+rsp]
        movups    xmm15, XMMWORD PTR [112+rsp]
        add       rsp, 152
        pop       rbp
        pop       r14
        pop       rsi
        pop       rbx
        ret

_B7_45::

        mov       ebp, 2
        jmp       _B7_13

_B7_46::

        movss     xmm4, DWORD PTR [_2il0floatpacket_147]
        lea       rbx, QWORD PTR [__ImageBase]
        movss     xmm6, DWORD PTR [40+rsp]
        mov       rbp, rbx
        addss     xmm0, xmm1
        addss     xmm1, xmm2
        mulss     xmm4, xmm0
        addss     xmm8, xmm1
        addss     xmm4, DWORD PTR [_2il0floatpacket_148]
        mulss     xmm4, xmm0
        movaps    xmm7, xmm8
        addss     xmm4, DWORD PTR [_2il0floatpacket_149]
        mulss     xmm4, xmm0
        addss     xmm4, DWORD PTR [_2il0floatpacket_150]
        mulss     xmm4, xmm0
        movss     xmm0, DWORD PTR [_2il0floatpacket_146]
        addss     xmm3, xmm4
        lea       rdx, QWORD PTR [52+rsp]
        movss     DWORD PTR [rdx], xmm3
        movss     xmm5, DWORD PTR [rdx]
        subss     xmm6, xmm5
        addss     xmm6, xmm4
        lea       rcx, QWORD PTR [56+rsp]
        movss     DWORD PTR [rcx], xmm6
        movss     xmm3, DWORD PTR [rdx]
        movss     xmm1, DWORD PTR [rcx]
        addss     xmm7, xmm3
        movss     DWORD PTR [rdx], xmm7
        movss     xmm15, DWORD PTR [rdx]
        subss     xmm3, xmm15
        addss     xmm3, xmm8
        movaps    xmm8, xmm0
        movss     DWORD PTR [rcx], xmm3
        movss     xmm7, DWORD PTR [rdx]
        mulss     xmm8, xmm7
        movss     xmm3, DWORD PTR [rcx]
        movss     DWORD PTR [rdx], xmm8
        addss     xmm3, xmm1
        movss     xmm2, DWORD PTR [rdx]
        lea       rsi, QWORD PTR [60+rsp]
        mulss     xmm3, DWORD PTR [rsi]
        subss     xmm2, xmm7
        movss     DWORD PTR [rcx], xmm2
        movss     xmm5, DWORD PTR [rdx]
        movss     xmm4, DWORD PTR [rcx]
        subss     xmm5, xmm4
        movss     DWORD PTR [rdx], xmm5
        movss     xmm6, DWORD PTR [rdx]
        movss     xmm5, DWORD PTR [rsi]
        subss     xmm7, xmm6
        mulss     xmm0, xmm5
        movss     DWORD PTR [rcx], xmm7
        movss     xmm2, DWORD PTR [rdx]
        movss     xmm1, DWORD PTR [rcx]
        movss     DWORD PTR [rdx], xmm0
        movss     xmm8, DWORD PTR [rdx]
        subss     xmm8, DWORD PTR [rsi]
        movss     DWORD PTR [rcx], xmm8
        movss     xmm4, DWORD PTR [rdx]
        movss     xmm15, DWORD PTR [rcx]
        subss     xmm4, xmm15
        movss     DWORD PTR [rdx], xmm4
        movss     xmm8, DWORD PTR [rdx]
        subss     xmm5, xmm8
        movss     DWORD PTR [rcx], xmm5
        movss     xmm6, DWORD PTR [rdx]
        movss     xmm0, DWORD PTR [rcx]
        movaps    xmm8, xmm6
        mulss     xmm8, xmm2
        mulss     xmm6, xmm1
        mulss     xmm2, xmm0
        mulss     xmm1, xmm0
        addss     xmm6, xmm2
        movaps    xmm4, xmm8
        movss     xmm2, DWORD PTR [_2il0floatpacket_151]
        addss     xmm4, xmm6
        subss     xmm8, xmm4
        addss     xmm8, xmm6
        addss     xmm8, xmm1
        movaps    xmm1, xmm2
        addss     xmm1, xmm4
        addss     xmm3, xmm8
        movss     DWORD PTR [rdx], xmm1
        movss     xmm7, DWORD PTR [rdx]
        movss     xmm1, DWORD PTR [_2il0floatpacket_152]
        subss     xmm7, xmm2
        movss     DWORD PTR [32+rsp], xmm7
        movss     xmm15, DWORD PTR [32+rsp]
        mov       ecx, DWORD PTR [rdx]
        mov       edx, ecx
        and       ecx, 127
        subss     xmm4, xmm15
        movss     DWORD PTR [36+rsp], xmm4
        add       ecx, ecx
        movss     xmm0, DWORD PTR [36+rsp]
        shl       edx, 10
        addss     xmm3, xmm0
        mulss     xmm1, xmm3
        movss     xmm0, DWORD PTR [imagerel(_vmlsPowHATab)+420+rbx+rcx*4]
        sar       edx, 17
        addss     xmm1, DWORD PTR [_2il0floatpacket_153]
        mulss     xmm1, xmm3
        addss     xmm1, DWORD PTR [_2il0floatpacket_154]
        mulss     xmm1, xmm3
        mulss     xmm1, xmm0
        addss     xmm1, DWORD PTR [imagerel(_vmlsPowHATab)+424+rbp+rcx*4]
        movaps    xmm2, xmm1
        addss     xmm2, xmm0
        movss     DWORD PTR [44+rsp], xmm2
        movzx     ecx, WORD PTR [46+rsp]
        mov       esi, ecx
        and       esi, 32640
        shr       esi, 7
        lea       ebx, DWORD PTR [-127+rdx+rsi]
        cmp       ebx, 128
        jge       _B7_54

_B7_47::

        cmp       ebx, -126
        jl        _B7_49

_B7_48::

        and       ecx, -32641
        lea       edx, DWORD PTR [127+rbx]
        movzx     edx, dl
        shl       edx, 7
        or        ecx, edx
        mov       WORD PTR [46+rsp], cx
        movss     xmm0, DWORD PTR [44+rsp]
        mulss     xmm0, xmm14
        movss     DWORD PTR [44+rsp], xmm0
        movss     DWORD PTR [r8], xmm0
        jmp       _B7_44

_B7_49::

        cmp       ebx, -136
        jl        _B7_51

_B7_50::

        lea       rbx, QWORD PTR [52+rsp]
        movss     DWORD PTR [rbx], xmm2
        mov       ecx, 1065353216
        movss     xmm2, DWORD PTR [rbx]
        add       edx, 191
        mov       DWORD PTR [40+rsp], ecx
        subss     xmm0, xmm2
        shr       ecx, 16
        addss     xmm0, xmm1
        movss     DWORD PTR [56+rsp], xmm0
        movss     xmm7, DWORD PTR [rbx]
        movss     xmm1, DWORD PTR [_2il0floatpacket_146]
        mulss     xmm1, xmm7
        movss     xmm0, DWORD PTR [56+rsp]
        movss     DWORD PTR [rbx], xmm1
        movss     xmm3, DWORD PTR [rbx]
        movzx     ebx, cx
        subss     xmm3, xmm7
        lea       rcx, QWORD PTR [56+rsp]
        movss     DWORD PTR [rcx], xmm3
        and       ebx, -32641
        lea       rsi, QWORD PTR [52+rsp]
        movss     xmm5, DWORD PTR [rsi]
        movss     xmm4, DWORD PTR [rcx]
        movzx     edx, dl
        subss     xmm5, xmm4
        movss     DWORD PTR [rsi], xmm5
        movss     xmm6, DWORD PTR [rsi]
        shl       edx, 7
        subss     xmm7, xmm6
        movss     DWORD PTR [rcx], xmm7
        or        ebx, edx
        movss     xmm1, DWORD PTR [rsi]
        movss     xmm8, DWORD PTR [rcx]
        mov       WORD PTR [42+rsp], bx
        addss     xmm0, xmm8
        movss     xmm15, DWORD PTR [40+rsp]
        mulss     xmm1, xmm15
        mulss     xmm0, xmm15
        lea       rbx, QWORD PTR [52+rsp]
        mov       DWORD PTR [rbx], 8388608
        addss     xmm1, xmm0
        movss     xmm8, DWORD PTR [rbx]
        movss     xmm0, DWORD PTR [rbx]
        mulss     xmm8, xmm0
        mulss     xmm1, DWORD PTR [_2il0floatpacket_157]
        movss     DWORD PTR [rbx], xmm8
        movss     xmm15, DWORD PTR [rbx]
        addss     xmm1, xmm15
        mulss     xmm14, xmm1
        movss     DWORD PTR [44+rsp], xmm1
        movss     DWORD PTR [r8], xmm14
        jmp       _B7_44

_B7_51::

        cmp       ebx, -159
        jl        _B7_53

_B7_52::

        mov       ecx, 1065353216
        add       edx, 191
        mov       DWORD PTR [40+rsp], ecx
        shr       ecx, 16
        movzx     ebx, cx
        movzx     edx, dl
        and       ebx, -32641
        shl       edx, 7
        or        ebx, edx
        mov       WORD PTR [42+rsp], bx
        movss     xmm0, DWORD PTR [_2il0floatpacket_157]
        movss     xmm1, DWORD PTR [40+rsp]
        mulss     xmm14, xmm0
        mulss     xmm2, xmm1
        lea       rbx, QWORD PTR [52+rsp]
        mov       DWORD PTR [rbx], 8388608
        movss     xmm3, DWORD PTR [rbx]
        mulss     xmm14, xmm2
        movss     xmm2, DWORD PTR [rbx]
        mulss     xmm3, xmm2
        movss     DWORD PTR [rbx], xmm3
        movss     xmm4, DWORD PTR [rbx]
        addss     xmm14, xmm4
        movss     DWORD PTR [44+rsp], xmm14
        movss     DWORD PTR [r8], xmm14
        jmp       _B7_44

_B7_53::

        lea       rdx, QWORD PTR [52+rsp]
        mov       DWORD PTR [rdx], 8388608
        movss     xmm1, DWORD PTR [rdx]
        movss     xmm0, DWORD PTR [rdx]
        mulss     xmm1, xmm0
        movss     DWORD PTR [rdx], xmm1
        movss     xmm2, DWORD PTR [rdx]
        mulss     xmm14, xmm2
        movss     DWORD PTR [44+rsp], xmm14
        movss     DWORD PTR [r8], xmm14
        jmp       _B7_44

_B7_54::

        lea       rdx, QWORD PTR [52+rsp]
        mov       DWORD PTR [rdx], 2130706432
        movss     xmm1, DWORD PTR [rdx]
        movss     xmm0, DWORD PTR [rdx]
        mulss     xmm1, xmm0
        movss     DWORD PTR [rdx], xmm1
        movss     xmm2, DWORD PTR [rdx]
        mulss     xmm14, xmm2
        movss     DWORD PTR [44+rsp], xmm14
        movss     DWORD PTR [r8], xmm14
        jmp       _B7_44

_B7_55::

        mov       cl, BYTE PTR [43+rsp]
        and       cl, -128
        shr       cl, 7
        xor       dl, cl
        movzx     ebx, dl
        lea       rdx, QWORD PTR [__ImageBase]
        movss     xmm0, DWORD PTR [imagerel(_vmlsPowHATab)+1472+rdx+rbx*4]
        mulss     xmm0, xmm0
        mulss     xmm0, xmm14
        movss     DWORD PTR [r8], xmm0
        jmp       _B7_44

_B7_56::

        test      ebp, ebp
        jne       _B7_58

_B7_57::

        test      esi, esi
        jne       _B7_24

_B7_58::

        lea       rdx, QWORD PTR [__ImageBase]
        and       ebp, 1
        mov       ecx, DWORD PTR [imagerel(_vmlsPowHATab)+1484+rdx+rbp*4]
        mov       DWORD PTR [r8], ecx
        jmp       _B7_44

_B7_59::

        mulss     xmm1, xmm1
        test      dl, dl
        je        _B7_61

_B7_60::

        lea       rax, QWORD PTR [__ImageBase]
        and       ebp, r11d
        movss     xmm0, DWORD PTR [imagerel(_vmlsPowHATab)+1484+rax+rbp*4]
        mov       eax, 1
        divss     xmm0, xmm1
        movss     DWORD PTR [r8], xmm0
        jmp       _B7_44

_B7_61::

        lea       rdx, QWORD PTR [__ImageBase]
        and       ebp, r11d
        movss     xmm0, DWORD PTR [imagerel(_vmlsPowHATab)+1484+rdx+rbp*4]
        mulss     xmm0, xmm1
        movss     DWORD PTR [r8], xmm0
        jmp       _B7_44

_B7_62::

        test      r10d, r10d
        jne       _B7_43

_B7_63::

        test      ebx, ebx
        jne       _B7_18

_B7_64::

        test      r9d, r9d
        je        _B7_18
        jmp       _B7_43
        ALIGN     16

_B7_65::

__svml_spow_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_spow_ha_cout_rare_internal_B1_B64:
	DD	1065729
	DD	354371
	DD	292926
	DD	428089
	DD	583731
	DD	522279
	DD	1245456
	DD	3758641161
	DD	805658630

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_65
	DD	imagerel _unwind___svml_spow_ha_cout_rare_internal_B1_B64

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ  'DATA'
	DD 8 DUP (0H)	
	PUBLIC __svml_spow_ha_data_internal_avx512
__svml_spow_ha_data_internal_avx512	DD	0
	DD	3174420480
	DD	3182632960
	DD	3187958784
	DD	3190685696
	DD	3193338880
	DD	3195920384
	DD	3197247488
	DD	3198473216
	DD	3199668736
	DD	3200835072
	DD	3201974272
	DD	3203086848
	DD	3204174848
	DD	3204843520
	DD	3205364224
	DD	1054113792
	DD	1053115392
	DD	1052137472
	DD	1051179008
	DD	1050238976
	DD	1049316864
	DD	1048248320
	DD	1046471680
	DD	1044726784
	DD	1043013632
	DD	1041329152
	DD	1039161344
	DD	1035907072
	DD	1032706048
	DD	1027317760
	DD	1018830848
	DD	0
	DD	3067311503
	DD	890262383
	DD	916311190
	DD	3058814943
	DD	914835756
	DD	3056977939
	DD	3052757441
	DD	905348701
	DD	921801496
	DD	900652061
	DD	916473404
	DD	3063873943
	DD	3048020321
	DD	3055557319
	DD	921573027
	DD	3050426335
	DD	918574590
	DD	913737309
	DD	3045697063
	DD	3029223305
	DD	866568163
	DD	3063765991
	DD	3057827840
	DD	910185982
	DD	3062847489
	DD	917965485
	DD	903301016
	DD	882039287
	DD	910858241
	DD	3059117133
	DD	3029061382
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
	DD	0
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
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	1049872133
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	3199775725
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	1056323663
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	2981005319
	DD	849703116
	DD	849703116
	DD	849703116
	DD	849703116
	DD	849703116
	DD	849703116
	DD	849703116
	DD	849703116
	DD	849703116
	DD	849703116
	DD	849703116
	DD	849703116
	DD	849703116
	DD	849703116
	DD	849703116
	DD	849703116
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
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	3208161851
	DD	1220546528
	DD	1220546528
	DD	1220546528
	DD	1220546528
	DD	1220546528
	DD	1220546528
	DD	1220546528
	DD	1220546528
	DD	1220546528
	DD	1220546528
	DD	1220546528
	DD	1220546528
	DD	1220546528
	DD	1220546528
	DD	1220546528
	DD	1220546528
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
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1030083214
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1047919318
	DD	1060205081
	DD	1060205081
	DD	1060205081
	DD	1060205081
	DD	1060205081
	DD	1060205081
	DD	1060205081
	DD	1060205081
	DD	1060205081
	DD	1060205081
	DD	1060205081
	DD	1060205081
	DD	1060205081
	DD	1060205081
	DD	1060205081
	DD	1060205081
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
	DD	1123745792
	DD	1123745792
	DD	1123745792
	DD	1123745792
	DD	1123745792
	DD	1123745792
	DD	1123745792
	DD	1123745792
	DD	1123745792
	DD	1123745792
	DD	1123745792
	DD	1123745792
	DD	1123745792
	DD	1123745792
	DD	1123745792
	DD	1123745792
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
	PUBLIC __svml_spow_ha_data_internal
__svml_spow_ha_data_internal	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	2155872256
	DD	4278190079
	DD	4278190079
	DD	4278190079
	DD	4278190079
	DD	4278190079
	DD	4278190079
	DD	4278190079
	DD	4278190079
	DD	4278190079
	DD	4278190079
	DD	4278190079
	DD	4278190079
	DD	4278190079
	DD	4278190079
	DD	4278190079
	DD	4278190079
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
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	1118738944
	DD	3715793664
	DD	3230016299
	DD	4013928704
	DD	3189565726
	DD	3699038248
	DD	3230016303
	DD	4255595370
	DD	3189567439
	DD	3648859040
	DD	3230016307
	DD	1732833791
	DD	3189567296
	DD	3565385952
	DD	3230016311
	DD	2485752410
	DD	3189567274
	DD	3448748152
	DD	3230016315
	DD	3514744523
	DD	3189567800
	DD	3299074072
	DD	3230016319
	DD	3686511262
	DD	3189567474
	DD	3116491376
	DD	3230016323
	DD	716063611
	DD	3189567908
	DD	2901127016
	DD	3230016327
	DD	354437295
	DD	3189567291
	DD	2653107184
	DD	3230016331
	DD	3777462366
	DD	3189567264
	DD	2372557360
	DD	3230016335
	DD	3590663412
	DD	3189567498
	DD	2059602304
	DD	3230016339
	DD	1680795031
	DD	3189567428
	DD	1714366048
	DD	3230016343
	DD	2010344982
	DD	3189567986
	DD	1336971936
	DD	3230016347
	DD	166254822
	DD	3189567184
	DD	927542568
	DD	3230016351
	DD	59552610
	DD	3189567972
	DD	486199888
	DD	3230016355
	DD	2268824919
	DD	3189567664
	DD	13065112
	DD	3230016359
	DD	3005381643
	DD	3189567835
	DD	3803226080
	DD	3230016362
	DD	4018973165
	DD	3189567825
	DD	3266868056
	DD	3230016366
	DD	3693925101
	DD	3189567457
	DD	2699077504
	DD	3230016370
	DD	4005905743
	DD	3189567697
	DD	2099972936
	DD	3230016374
	DD	3580935118
	DD	3189567215
	DD	1469672176
	DD	3230016378
	DD	2928784295
	DD	3189567129
	DD	808292392
	DD	3230016382
	DD	674298833
	DD	3189567553
	DD	115950104
	DD	3230016386
	DD	2555938320
	DD	3189567208
	DD	3687728456
	DD	3230016389
	DD	532458218
	DD	3189567130
	DD	2933808064
	DD	3230016393
	DD	3888913110
	DD	3189567192
	DD	2149270784
	DD	3230016397
	DD	2365955494
	DD	3189567756
	DD	1334230552
	DD	3230016401
	DD	3325722981
	DD	3189567206
	DD	488800640
	DD	3230016405
	DD	1496133617
	DD	3189567683
	DD	3908061016
	DD	3230016408
	DD	1824836379
	DD	3189567490
	DD	3002189112
	DD	3230016412
	DD	1222174320
	DD	3189567837
	DD	2066263648
	DD	3230016416
	DD	2472769660
	DD	3189567285
	DD	1100395408
	DD	3230016420
	DD	447932460
	DD	3189567455
	DD	104694584
	DD	3230016424
	DD	1006807684
	DD	3189567460
	DD	3374238048
	DD	3230016427
	DD	4121834935
	DD	3189567556
	DD	2319200184
	DD	3230016431
	DD	3019906587
	DD	3189567664
	DD	1234656664
	DD	3230016435
	DD	3882744783
	DD	3189567930
	DD	120715280
	DD	3230016439
	DD	1927363170
	DD	3189567242
	DD	3272450504
	DD	3230016442
	DD	8203115
	DD	3189567876
	DD	2100034368
	DD	3230016446
	DD	1260625560
	DD	3189567855
	DD	898540192
	DD	3230016450
	DD	1791679111
	DD	3189567639
	DD	3963040720
	DD	3230016453
	DD	831133439
	DD	3189567590
	DD	2703706240
	DD	3230016457
	DD	2803140504
	DD	3189567482
	DD	1415608352
	DD	3230016461
	DD	2955844462
	DD	3189568058
	DD	98850816
	DD	3230016465
	DD	4017967070
	DD	3189567455
	DD	3048504096
	DD	3230016468
	DD	3974385451
	DD	3189567871
	DD	1674736240
	DD	3230016472
	DD	2424114556
	DD	3189567887
	DD	272616624
	DD	3230016476
	DD	4261883032
	DD	3189567073
	DD	3137214056
	DD	3230016479
	DD	3716750895
	DD	3189567522
	DD	1678694944
	DD	3230016483
	DD	498025304
	DD	3189567180
	DD	192127016
	DD	3230016487
	DD	1011852160
	DD	3189567511
	DD	2972577480
	DD	3230016490
	DD	558940916
	DD	3189567850
	DD	1430211120
	DD	3230016494
	DD	2550865739
	DD	3189567935
	DD	4155061376
	DD	3230016497
	DD	1930847170
	DD	3189567371
	DD	2557291968
	DD	3230016501
	DD	1716354804
	DD	3189567130
	DD	931967984
	DD	3230016505
	DD	2729403392
	DD	3189567981
	DD	3574154016
	DD	3230016508
	DD	1067280599
	DD	3189567890
	DD	1894012232
	DD	3230016512
	DD	2094260282
	DD	3189567604
	DD	186606176
	DD	3230016516
	DD	3897933165
	DD	3189568046
	DD	2746998904
	DD	3230016519
	DD	1935720459
	DD	3189567701
	DD	985351064
	DD	3230016523
	DD	2170344821
	DD	3189567164
	DD	3491691984
	DD	3230016526
	DD	4220979537
	DD	3189567547
	DD	1676181328
	DD	3230016530
	DD	1953924393
	DD	3189567854
	DD	4128847448
	DD	3230016533
	DD	1689829067
	DD	3189567468
	DD	2259849008
	DD	3230016537
	DD	2367753130
	DD	3189567619
	DD	364246088
	DD	3230016541
	DD	2263309073
	DD	3189567722
	DD	2737098280
	DD	3230016544
	DD	2366756869
	DD	3189567856
	DD	788562816
	DD	3230016548
	DD	2294653975
	DD	3189567145
	DD	3108665616
	DD	3230016551
	DD	1786494117
	DD	3189567254
	DD	1107562960
	DD	3230016555
	DD	3655887416
	DD	3189567689
	DD	3375279848
	DD	3230016558
	DD	1178948625
	DD	3189567241
	DD	1321971608
	DD	3230016562
	DD	1620080441
	DD	3189567466
	DD	3537662304
	DD	3230016565
	DD	154455618
	DD	3189567981
	DD	1432506360
	DD	3230016569
	DD	3128192125
	DD	3189567889
	DD	3596526920
	DD	3230016572
	DD	2026842750
	DD	3189567212
	DD	1439877480
	DD	3230016576
	DD	1384431897
	DD	3189567284
	DD	3552580280
	DD	3230016579
	DD	1999983129
	DD	3189568079
	DD	1344787944
	DD	3230016583
	DD	4035436820
	DD	3189567579
	DD	3406521808
	DD	3230016586
	DD	77799817
	DD	3189567285
	DD	1147933592
	DD	3230016590
	DD	1261060207
	DD	3189567477
	DD	3159043768
	DD	3230016593
	DD	2102717775
	DD	3189567605
	DD	850003184
	DD	3230016597
	DD	161472469
	DD	3189567688
	DD	2810831440
	DD	3230016600
	DD	1937336881
	DD	3189567665
	DD	451678520
	DD	3230016604
	DD	2268353549
	DD	3189567773
	DD	2362563168
	DD	3230016607
	DD	1785871595
	DD	3189567889
	DD	4248601816
	DD	3230016610
	DD	2434669991
	DD	3189567898
	DD	1814910472
	DD	3230016614
	DD	2666999375
	DD	3189568058
	DD	3651506624
	DD	3230016617
	DD	1033139811
	DD	3189567312
	DD	1168538136
	DD	3230016621
	DD	4004618418
	DD	3189567740
	DD	2956021672
	DD	3230016624
	DD	3799928764
	DD	3189567751
	DD	424104288
	DD	3230016628
	DD	3650233137
	DD	3189567549
	DD	2162801816
	DD	3230016631
	DD	437313833
	DD	3189567438
	DD	3877227800
	DD	3230016634
	DD	2138293494
	DD	3189567140
	DD	1272495376
	DD	3230016638
	DD	2891370752
	DD	3189567168
	DD	2938619176
	DD	3230016641
	DD	1704142730
	DD	3189567114
	DD	285744240
	DD	3230016645
	DD	500817607
	DD	3189568035
	DD	1903884424
	DD	3230016648
	DD	3102571944
	DD	3189567668
	DD	3498151272
	DD	3230016651
	DD	2374193154
	DD	3189567933
	DD	773655960
	DD	3230016655
	DD	2898331636
	DD	3189568089
	DD	2320411168
	DD	3230016658
	DD	1983295500
	DD	3189567113
	DD	3843527280
	DD	3230016661
	DD	2132853542
	DD	3189567093
	DD	1048114320
	DD	3230016665
	DD	3551155364
	DD	3189567407
	DD	2524183824
	DD	3230016668
	DD	2564000663
	DD	3189567090
	DD	3976845048
	DD	3230016671
	DD	361581803
	DD	3189567196
	DD	1111206880
	DD	3230016675
	DD	696765230
	DD	3189568040
	DD	2517279744
	DD	3230016678
	DD	941542475
	DD	3189567463
	DD	3900171776
	DD	3230016681
	DD	2377895552
	DD	3189567282
	DD	964990760
	DD	3230016685
	DD	1173605489
	DD	3189567454
	DD	2301746008
	DD	3230016688
	DD	3096720313
	DD	3189567400
	DD	3615544576
	DD	3230016691
	DD	1942008121
	DD	3189567335
	DD	611493160
	DD	3230016695
	DD	3375481119
	DD	3189567537
	DD	1879599992
	DD	3230016698
	DD	4103239143
	DD	3189567624
	DD	3124971056
	DD	3230016701
	DD	3773474925
	DD	3189567869
	DD	52711992
	DD	3230016705
	DD	2465315445
	DD	3189567444
	DD	1252829960
	DD	3230016708
	DD	1041581158
	DD	3189567781
	DD	2430429904
	DD	3230016711
	DD	166620501
	DD	3189567739
	DD	3585583704
	DD	3230016714
	DD	2379237630
	DD	3189567984
	DD	423395616
	DD	3230016718
	DD	134834559
	DD	3189567178
	DD	1533871416
	DD	3230016721
	DD	1449318760
	DD	3189567373
	DD	2622114680
	DD	3230016724
	DD	3042372984
	DD	3189567127
	DD	3688195920
	DD	3230016727
	DD	1265290101
	DD	3189567921
	DD	437218048
	DD	3230016731
	DD	2248549362
	DD	3189567262
	DD	1459185496
	DD	3230016734
	DD	1293250365
	DD	3189567151
	DD	2459200496
	DD	3230016737
	DD	313786605
	DD	3189567180
	DD	3437332240
	DD	3230016740
	DD	3289151823
	DD	3189567872
	DD	98682312
	DD	3230016744
	DD	2693758821
	DD	3189567877
	DD	1033253848
	DD	3230016747
	DD	4237443192
	DD	3189567301
	DD	1946147760
	DD	3230016750
	DD	4099404586
	DD	3189567972
	DD	2837431968
	DD	3230016753
	DD	343640666
	DD	3189567552
	DD	3707174040
	DD	3230016756
	DD	4238401458
	DD	3189567983
	DD	260473960
	DD	3230016760
	DD	21423548
	DD	3189567559
	DD	1087333264
	DD	3230016763
	DD	3515278593
	DD	3189567309
	DD	1892851296
	DD	3230016766
	DD	629106375
	DD	3189567166
	DD	2677094384
	DD	3230016769
	DD	2048670044
	DD	3189567227
	DD	3440128544
	DD	3230016772
	DD	3739217292
	DD	3189568009
	DD	4182019504
	DD	3230016775
	DD	3662039444
	DD	3189567618
	DD	607865368
	DD	3230016779
	DD	1766486991
	DD	3189567112
	DD	1307665824
	DD	3230016782
	DD	512689457
	DD	3189567670
	DD	1986518392
	DD	3230016785
	DD	1704577378
	DD	3189567760
	DD	2644487576
	DD	3230016788
	DD	4001935135
	DD	3189567473
	DD	3281637576
	DD	3230016791
	DD	1184452342
	DD	3189567735
	DD	3898032312
	DD	3230016794
	DD	2714793956
	DD	3189567464
	DD	198768096
	DD	3230016798
	DD	3657654418
	DD	3189567905
	DD	773842856
	DD	3230016801
	DD	2809688013
	DD	3189567736
	DD	1328352328
	DD	3230016804
	DD	390727591
	DD	3189567393
	DD	1862359256
	DD	3230016807
	DD	1135237925
	DD	3189567248
	DD	2375926096
	DD	3230016810
	DD	1377572509
	DD	3189567837
	DD	2869115032
	DD	3230016813
	DD	3148383775
	DD	3189568032
	DD	3341987960
	DD	3230016816
	DD	3809536738
	DD	3189567313
	DD	3794606472
	DD	3230016819
	DD	4243445934
	DD	3189568034
	DD	4227031920
	DD	3230016822
	DD	1302642088
	DD	3189567451
	DD	344358048
	DD	3230016826
	DD	3239125361
	DD	3189567151
	DD	736580224
	DD	3230016829
	DD	3753161736
	DD	3189567129
	DD	1108791648
	DD	3230016832
	DD	624357483
	DD	3189568045
	DD	1461052568
	DD	3230016835
	DD	3153257064
	DD	3189567338
	DD	1793422920
	DD	3230016838
	DD	2705820149
	DD	3189567635
	DD	2105962400
	DD	3230016841
	DD	911564860
	DD	3189567737
	DD	2398730424
	DD	3230016844
	DD	1086958332
	DD	3189567948
	DD	2671786152
	DD	3230016847
	DD	1598984535
	DD	3189567210
	DD	2925188456
	DD	3230016850
	DD	4113385879
	DD	3189567401
	DD	3158995968
	DD	3230016853
	DD	2426812671
	DD	3189567396
	DD	3373267048
	DD	3230016856
	DD	3480845679
	DD	3189567359
	DD	3568059792
	DD	3230016859
	DD	3306230729
	DD	3189567924
	DD	3743432056
	DD	3230016862
	DD	3853821277
	DD	3189567317
	DD	3899441408
	DD	3230016865
	DD	2049309148
	DD	3189567699
	DD	4036145192
	DD	3230016868
	DD	2667321817
	DD	3189567166
	DD	4153600472
	DD	3230016871
	DD	3540670389
	DD	3189567114
	DD	4251864072
	DD	3230016874
	DD	404207883
	DD	3189567310
	DD	36025272
	DD	3230016878
	DD	2837617046
	DD	3189567103
	DD	96074976
	DD	3230016881
	DD	1231280453
	DD	3189567669
	DD	137101976
	DD	3230016884
	DD	792492437
	DD	3189567071
	DD	159162080
	DD	3230016887
	DD	2385525292
	DD	3189567618
	DD	162310880
	DD	3230016890
	DD	3272051015
	DD	3189567858
	DD	146603712
	DD	3230016893
	DD	623573538
	DD	3189567881
	DD	112095672
	DD	3230016896
	DD	2151835939
	DD	3189567451
	DD	58841608
	DD	3230016899
	DD	958006130
	DD	3189567217
	DD	4281863424
	DD	3230016901
	DD	549948691
	DD	3189567863
	DD	4191280920
	DD	3230016904
	DD	4177689987
	DD	3189567217
	DD	4082115512
	DD	3230016907
	DD	2895765193
	DD	3189567599
	DD	3954421112
	DD	3230016910
	DD	1055606208
	DD	3189567799
	DD	3808251392
	DD	3230016913
	DD	1966521217
	DD	3189567351
	DD	3643659776
	DD	3230016916
	DD	1017382758
	DD	3189567732
	DD	3460699480
	DD	3230016919
	DD	3485890155
	DD	3189567410
	DD	3259423472
	DD	3230016922
	DD	2657419170
	DD	3189567140
	DD	3039884488
	DD	3230016925
	DD	4198858220
	DD	3189568080
	DD	2802135064
	DD	3230016928
	DD	2343740494
	DD	3189567869
	DD	2546227480
	DD	3230016931
	DD	2423084215
	DD	3189567957
	DD	2272213808
	DD	3230016934
	DD	3887273856
	DD	3189567631
	DD	1980145888
	DD	3230016937
	DD	3992755473
	DD	3189567251
	DD	1670075336
	DD	3230016940
	DD	722498073
	DD	3189567389
	DD	1342053552
	DD	3230016943
	DD	3573506400
	DD	3189567965
	DD	996131728
	DD	3230016946
	DD	714695829
	DD	3189567392
	DD	632360808
	DD	3230016949
	DD	2816704229
	DD	3189567846
	DD	250791552
	DD	3230016952
	DD	386951333
	DD	3189567243
	DD	4146441768
	DD	3230016954
	DD	2155319939
	DD	3189567580
	DD	3729427184
	DD	3230016957
	DD	4227840137
	DD	3189567931
	DD	3294765200
	DD	3230016960
	DD	1718354212
	DD	3189567672
	DD	2842505696
	DD	3230016963
	DD	4243880582
	DD	3189567656
	DD	2372698352
	DD	3230016966
	DD	3784093842
	DD	3189567302
	DD	1885392624
	DD	3230016969
	DD	2318733051
	DD	3189567813
	DD	1380637784
	DD	3230016972
	DD	497929746
	DD	3189567210
	DD	858482864
	DD	3230016975
	DD	3218883783
	DD	3189567649
	DD	318976720
	DD	3230016978
	DD	3492652862
	DD	3189567382
	DD	4057135272
	DD	3230016980
	DD	473724673
	DD	3189568069
	DD	3483072376
	DD	3230016983
	DD	150485010
	DD	3189567757
	DD	2891803552
	DD	3230016986
	DD	3028900958
	DD	3189567173
	DD	2283376816
	DD	3230016989
	DD	321242596
	DD	3189567822
	DD	1657840008
	DD	3230016992
	DD	151305746
	DD	3189568029
	DD	1015240760
	DD	3230016995
	DD	2208026401
	DD	3189567184
	DD	355626480
	DD	3230016998
	DD	2487781842
	DD	3189567931
	DD	3974011720
	DD	3230017000
	DD	2391142094
	DD	3189567091
	DD	3280508896
	DD	3230017003
	DD	734457618
	DD	3189568092
	DD	2570132168
	DD	3230017006
	DD	3070902701
	DD	3189567794
	DD	1842928168
	DD	3230017009
	DD	1110877923
	DD	3189567896
	DD	1098943352
	DD	3230017012
	DD	3880360185
	DD	3189567898
	DD	338223984
	DD	3230017015
	DD	391995809
	DD	3189567294
	DD	3855783416
	DD	3230017017
	DD	4186345433
	DD	3189567745
	DD	3061732952
	DD	3230017020
	DD	3682852050
	DD	3189567081
	DD	2251085560
	DD	3230017023
	DD	3057037137
	DD	3189567617
	DD	1423886760
	DD	3230017026
	DD	3352664894
	DD	3189567074
	DD	580181848
	DD	3230017029
	DD	1170568982
	DD	3189567927
	DD	4014983272
	DD	3230017031
	DD	671148579
	DD	3189567293
	DD	3138401368
	DD	3230017034
	DD	267496658
	DD	3189567304
	DD	2245448192
	DD	3230017037
	DD	1285167087
	DD	3189568066
	DD	1336168336
	DD	3230017040
	DD	4259909589
	DD	3189567788
	DD	410606184
	DD	3230017043
	DD	1494078902
	DD	3189568031
	DD	3763773256
	DD	3230017045
	DD	261935754
	DD	3189567685
	DD	2805778992
	DD	3230017048
	DD	3679585605
	DD	3189567195
	DD	1831634536
	DD	3230017051
	DD	809066513
	DD	3189567664
	DD	841383576
	DD	3230017054
	DD	385945238
	DD	3189567897
	DD	4130036912
	DD	3230017056
	DD	825579909
	DD	3189567603
	DD	3107703272
	DD	3230017059
	DD	3911295427
	DD	3189567514
	DD	2069393104
	DD	3230017062
	DD	3473784588
	DD	3189567461
	DD	1015149384
	DD	3230017065
	DD	629861200
	DD	3189567515
	DD	4239982216
	DD	3230017067
	DD	2659142760
	DD	3189567083
	DD	3153999632
	DD	3230017070
	DD	1251680713
	DD	3189567080
	DD	2052211384
	DD	3230017073
	DD	3168705163
	DD	3189567992
	DD	934659776
	DD	3230017076
	DD	1674797624
	DD	3189567952
	DD	4096354216
	DD	3230017078
	DD	437847902
	DD	3189567971
	DD	2947402064
	DD	3230017081
	DD	4024401533
	DD	3189567960
	DD	1782812400
	DD	3230017084
	DD	786902458
	DD	3189567873
	DD	602626840
	DD	3230017087
	DD	2593792633
	DD	3189567812
	DD	3701854136
	DD	3230017089
	DD	3989721221
	DD	3189567126
	DD	2490600968
	DD	3230017092
	DD	2545746866
	DD	3189567591
	DD	1263875768
	DD	3230017095
	DD	3115843521
	DD	3189567377
	DD	21719488
	DD	3230017098
	DD	1730226063
	DD	3189567307
	DD	3059140216
	DD	3230017100
	DD	690350922
	DD	3189567892
	DD	1786244008
	DD	3230017103
	DD	3350998585
	DD	3189567394
	DD	498038616
	DD	3230017106
	DD	1499123379
	DD	3189568082
	DD	3489531672
	DD	3230017108
	DD	2598665922
	DD	3189567118
	DD	2170828712
	DD	3230017111
	DD	2436376826
	DD	3189567935
	DD	836937048
	DD	3230017114
	DD	1977004902
	DD	3189568052
	DD	3782863816
	DD	3230017116
	DD	4027224619
	DD	3189567373
	DD	2418714096
	DD	3230017119
	DD	2616869856
	DD	3189567271
	DD	1039494712
	DD	3230017122
	DD	3775628133
	DD	3189567616
	DD	3940212336
	DD	3230017124
	DD	419018052
	DD	3189567909
	DD	2530971600
	DD	3230017127
	DD	150919657
	DD	3189567379
	DD	1106778856
	DD	3230017130
	DD	2926254537
	DD	3189567139
	DD	3962640312
	DD	3230017132
	DD	1610863810
	DD	3189567216
	DD	2508660136
	DD	3230017135
	DD	1166015244
	DD	3189567672
	DD	1039844240
	DD	3230017138
	DD	645159638
	DD	3189567685
	DD	3851198376
	DD	3230017140
	DD	2449439581
	DD	3189567695
	DD	2352826264
	DD	3230017143
	DD	1619104640
	DD	3189567458
	DD	839733360
	DD	3230017146
	DD	2427070371
	DD	3189567165
	DD	3606924968
	DD	3230017148
	DD	2166480890
	DD	3189567544
	DD	2064504368
	DD	3230017151
	DD	2283025905
	DD	3189567907
	DD	507476584
	DD	3230017154
	DD	3923356754
	DD	3189567272
	DD	3230846472
	DD	3230017156
	DD	805331677
	DD	3189567533
	DD	1644716880
	DD	3230017159
	DD	3790469685
	DD	3189567406
	DD	44092384
	DD	3230017162
	DD	2689788214
	DD	3189567653
	DD	2723977432
	DD	3230017164
	DD	2762250221
	DD	3189567072
	DD	1094474416
	DD	3230017167
	DD	2781925193
	DD	3189567716
	DD	3745554800
	DD	3230017169
	DD	593120878
	DD	3189567817
	DD	2087320704
	DD	3230017172
	DD	403414988
	DD	3189568021
	DD	414776008
	DD	3230017175
	DD	359307328
	DD	3189567387
	DD	3022924432
	DD	3230017177
	DD	3373547966
	DD	3189567572
	DD	1321867688
	DD	3230017180
	DD	3978711340
	DD	3189567805
	DD	3901576528
	DD	3230017182
	DD	2765663238
	DD	3189568046
	DD	2172152384
	DD	3230017185
	DD	1181150766
	DD	3189568053
	DD	428598440
	DD	3230017188
	DD	2473022131
	DD	3189567471
	DD	2965917728
	DD	3230017190
	DD	1621744488
	DD	3189567971
	DD	1194211288
	DD	3230017193
	DD	2931299013
	DD	3189567190
	DD	3703449168
	DD	3230017195
	DD	873154950
	DD	3189568039
	DD	1903732144
	DD	3230017198
	DD	1510784525
	DD	3189567521
	DD	90062704
	DD	3230017201
	DD	2918231590
	DD	3189568086
	DD	2557443240
	DD	3230017203
	DD	3329944838
	DD	3189567475
	DD	715974088
	DD	3230017206
	DD	2679715410
	DD	3189568021
	DD	3155624672
	DD	3230017208
	DD	1841110143
	DD	3189567518
	DD	1286495072
	DD	3230017211
	DD	3881638924
	DD	3189567497
	DD	3698554440
	DD	3230017213
	DD	167906931
	DD	3189567169
	DD	1801902600
	DD	3230017216
	DD	1468264167
	DD	3189567597
	DD	4186508448
	DD	3230017218
	DD	2400624638
	DD	3189567691
	DD	2262471560
	DD	3230017221
	DD	3796182064
	DD	3189567357
	DD	324793264
	DD	3230017224
	DD	2801238958
	DD	3189567581
	DD	2668474776
	DD	3230017226
	DD	2273142813
	DD	3189567436
	DD	703615288
	DD	3230017229
	DD	831867571
	DD	3189567237
	DD	3020183048
	DD	3230017231
	DD	2238048829
	DD	3189567595
	DD	1028277008
	DD	3230017234
	DD	1163763070
	DD	3189567452
	DD	3317865168
	DD	3230017236
	DD	1206450788
	DD	3189567228
	DD	1299046216
	DD	3230017239
	DD	737162226
	DD	3189567879
	DD	3561787920
	DD	3230017241
	DD	2433253277
	DD	3189567901
	DD	1516188728
	DD	3230017244
	DD	3561277099
	DD	3189567506
	DD	3752216144
	DD	3230017246
	DD	1385095507
	DD	3189567674
	DD	1679968384
	DD	3230017249
	DD	829462408
	DD	3189567156
	DD	3889412704
	DD	3230017251
	DD	2939266725
	DD	3189567673
	DD	1790647080
	DD	3230017254
	DD	4214100827
	DD	3189567848
	DD	3973638544
	DD	3230017256
	DD	2537954451
	DD	3189567376
	DD	1848484816
	DD	3230017259
	DD	1128740312
	DD	3189567098
	DD	4005152680
	DD	3230017261
	DD	1932232434
	DD	3189568053
	DD	1853739648
	DD	3230017264
	DD	1359710694
	DD	3189567457
	DD	3984212256
	DD	3230017266
	DD	1268482287
	DD	3189567990
	DD	1806667776
	DD	3230017269
	DD	839689017
	DD	3189567631
	DD	3911072520
	DD	3230017271
	DD	3727165601
	DD	3189567921
	DD	1707523520
	DD	3230017274
	DD	1836550952
	DD	3189567895
	DD	3785986872
	DD	3230017276
	DD	172951407
	DD	3189567220
	DD	1556559360
	DD	3230017279
	DD	1166449891
	DD	3189567294
	DD	3609206856
	DD	3230017281
	DD	28625366
	DD	3189567222
	DD	1354025920
	DD	3230017284
	DD	4038426582
	DD	3189567978
	DD	3380982208
	DD	3230017286
	DD	2756284659
	DD	3189567362
	DD	1100172048
	DD	3230017289
	DD	848830252
	DD	3189567230
	DD	3101560856
	DD	3230017291
	DD	1868170309
	DD	3189567423
	DD	795244752
	DD	3230017294
	DD	3128260908
	DD	3189567885
	DD	2771188936
	DD	3230017296
	DD	1691261279
	DD	3189567706
	DD	439489304
	DD	3230017299
	DD	2886711298
	DD	3189567236
	DD	2390110824
	DD	3230017301
	DD	1311431101
	DD	3189567133
	DD	33149176
	DD	3230017304
	DD	3042183093
	DD	3189567401
	DD	1958569120
	DD	3230017306
	DD	3174208880
	DD	3189567462
	DD	3871433416
	DD	3230017308
	DD	302374021
	DD	3189567243
	DD	1476804712
	DD	3230017311
	DD	712531519
	DD	3189567241
	DD	3364647440
	DD	3230017313
	DD	3705318448
	DD	3189567568
	DD	945056744
	DD	3230017316
	DD	2424171915
	DD	3189568016
	DD	2807996856
	DD	3230017318
	DD	199511961
	DD	3189567094
	DD	363562688
	DD	3230017321
	DD	3766234060
	DD	3189567214
	DD	2201718256
	DD	3230017323
	DD	1956195829
	DD	3189567592
	DD	4027525584
	DD	3230017325
	DD	2891794585
	DD	3189567375
	DD	1546046576
	DD	3230017328
	DD	843173017
	DD	3189567760
	DD	3347244936
	DD	3230017330
	DD	1609532414
	DD	3189567955
	DD	841215080
	DD	3230017333
	DD	3356704285
	DD	3189567294
	DD	2617920488
	DD	3230017335
	DD	1641729240
	DD	3189567346
	DD	87455368
	DD	3230017338
	DD	1455753222
	DD	3189567878
	DD	1839783016
	DD	3230017340
	DD	1936589978
	DD	3189567944
	DD	3579964728
	DD	3230017342
	DD	1006586129
	DD	3189567993
	DD	1013061704
	DD	3230017345
	DD	2766825484
	DD	3189567880
	DD	2729036928
	DD	3230017347
	DD	1888685184
	DD	3189567954
	DD	137984104
	DD	3230017350
	DD	848046306
	DD	3189568090
	DD	1829866024
	DD	3230017352
	DD	4178224056
	DD	3189567750
	DD	3509743488
	DD	3230017354
	DD	2637072474
	DD	3189567070
	DD	882677184
	DD	3230017357
	DD	3133305384
	DD	3189567936
	DD	2538629624
	DD	3230017359
	DD	1343200028
	DD	3189567905
	DD	4182661312
	DD	3230017361
	DD	107195460
	DD	3189567427
	DD	1519832648
	DD	3230017364
	DD	602154914
	DD	3189567857
	DD	3140105848
	DD	3230017366
	DD	659160740
	DD	3189567417
	DD	453573824
	DD	3230017369
	DD	236889930
	DD	3189567396
	DD	2050198592
	DD	3230017371
	DD	1175634921
	DD	3189567087
	DD	3635040168
	DD	3230017373
	DD	1241855527
	DD	3189567941
	DD	913158504
	DD	3230017376
	DD	3947904414
	DD	3189567477
	DD	2474515312
	DD	3230017378
	DD	1206979822
	DD	3189567534
	DD	4024170344
	DD	3230017380
	DD	1847397805
	DD	3189567152
	DD	1267183240
	DD	3230017383
	DD	2916539301
	DD	3189567754
	DD	2793515456
	DD	3230017385
	DD	2568213263
	DD	3189568078
	DD	13259168
	DD	3230017388
	DD	3003245330
	DD	3189567303
	DD	1516375624
	DD	3230017390
	DD	3472633477
	DD	3189567153
	DD	3007924104
	DD	3230017392
	DD	532406289
	DD	3189567855
	DD	192963816
	DD	3230017395
	DD	4149310663
	DD	3189567165
	DD	1661455728
	DD	3230017397
	DD	946970842
	DD	3189567598
	DD	3118458864
	DD	3230017399
	DD	2040517972
	DD	3189567254
	DD	269032128
	DD	3230017402
	DD	3669971831
	DD	3189568072
	DD	1703136256
	DD	3230017404
	DD	2375197389
	DD	3189567687
	DD	3125829976
	DD	3230017406
	DD	296757744
	DD	3189567673
	DD	242171944
	DD	3230017409
	DD	611424102
	DD	3189567452
	DD	1642122608
	DD	3230017411
	DD	1700968423
	DD	3189567419
	DD	3030740440
	DD	3230017413
	DD	2947762668
	DD	3189567968
	DD	113083840
	DD	3230017416
	DD	690901756
	DD	3189567496
	DD	1479112984
	DD	3230017418
	DD	121139903
	DD	3189567571
	DD	2833886088
	DD	3230017420
	DD	173386055
	DD	3189567866
	DD	4177428584
	DD	3230017422
	DD	2964718132
	DD	3189567231
	DD	1214798496
	DD	3230017425
	DD	541562024
	DD	3189567842
	DD	2535955680
	DD	3230017427
	DD	3368765612
	DD	3189568076
	DD	3845958008
	DD	3230017429
	DD	3194541507
	DD	3189567689
	DD	849863256
	DD	3230017432
	DD	3443356552
	DD	3189567858
	DD	2137631024
	DD	3230017434
	DD	1180679397
	DD	3189567138
	DD	3414318912
	DD	3230017436
	DD	2261929999
	DD	3189567651
	DD	384984464
	DD	3230017439
	DD	954057098
	DD	3189567971
	DD	1639587024
	DD	3230017441
	DD	1346995839
	DD	3189567263
	DD	2883183944
	DD	3230017443
	DD	3253373727
	DD	3189567382
	DD	4115799808
	DD	3230017445
	DD	4143791142
	DD	3189567800
	DD	1042491824
	DD	3230017448
	DD	4240432954
	DD	3189567700
	DD	2253218992
	DD	3230017450
	DD	1891708033
	DD	3189568072
	DD	3453038360
	DD	3230017452
	DD	710292621
	DD	3189567662
	DD	347006880
	DD	3230017455
	DD	2712254611
	DD	3189567116
	DD	1525083304
	DD	3230017457
	DD	349583280
	DD	3189568004
	DD	2692324448
	DD	3230017459
	DD	1803227864
	DD	3189567740
	DD	3848754312
	DD	3230017461
	DD	3644606976
	DD	3189567826
	DD	699429536
	DD	3230017464
	DD	1067153346
	DD	3189567751
	DD	1834308568
	DD	3230017466
	DD	1335350692
	DD	3189567081
	DD	2958447872
	DD	3230017468
	DD	1803696599
	DD	3189567557
	DD	4071871152
	DD	3230017470
	DD	1232152547
	DD	3189568017
	DD	879634736
	DD	3230017473
	DD	1455437901
	DD	3189567512
	DD	1971696744
	DD	3230017475
	DD	3708110750
	DD	3189567399
	DD	3053113344
	DD	3230017477
	DD	3907324819
	DD	3189567290
	DD	4123907920
	DD	3230017479
	DD	4080000314
	DD	3189567141
	DD	889136480
	DD	3230017482
	DD	1286519001
	DD	3189567298
	DD	1938756848
	DD	3230017484
	DD	1292347423
	DD	3189567513
	DD	2977824880
	DD	3230017486
	DD	1159725249
	DD	3189568015
	DD	4006363664
	DD	3230017488
	DD	895621348
	DD	3189567502
	DD	729428896
	DD	3230017491
	DD	1212894155
	DD	3189567280
	DD	1736978096
	DD	3230017493
	DD	296117275
	DD	3189567210
	DD	2734066816
	DD	3230017495
	DD	2463481211
	DD	3189567796
	DD	3720717848
	DD	3230017497
	DD	76524950
	DD	3189567159
	DD	401986584
	DD	3230017500
	DD	1718551671
	DD	3189567216
	DD	1367830248
	DD	3230017502
	DD	3568874946
	DD	3189567584
	DD	2323304104
	DD	3230017504
	DD	2763456396
	DD	3189567666
	DD	3268430632
	DD	3230017506
	DD	1208592501
	DD	3189567715
	DD	4203232240
	DD	3230017508
	DD	4148835618
	DD	3189567851
	DD	832763976
	DD	3230017511
	DD	610418007
	DD	3189567082
	DD	1746982680
	DD	3230017513
	DD	3035714190
	DD	3189567431
	DD	2650943256
	DD	3230017515
	DD	85931336
	DD	3189567843
	DD	3544667832
	DD	3230017517
	DD	3006685957
	DD	3189567286
	DD	133211144
	DD	3230017520
	DD	878859600
	DD	3189567873
	DD	1006529776
	DD	3230017522
	DD	854333969
	DD	3189567724
	DD	1869678336
	DD	3230017524
	DD	3058646748
	DD	3189567155
	DD	2722678648
	DD	3230017526
	DD	360733640
	DD	3189567693
	DD	3565552488
	DD	3230017528
	DD	3015564990
	DD	3189568013
	DD	103354256
	DD	3230017531
	DD	2286095909
	DD	3189568081
	DD	926040176
	DD	3230017533
	DD	1899270393
	DD	3189567137
	DD	1738664488
	DD	3230017535
	DD	1377641292
	DD	3189567833
	DD	2541248696
	DD	3230017537
	DD	4266686523
	DD	3189568077
	DD	3333814224
	DD	3230017539
	DD	4018775563
	DD	3189567218
	DD	4116382408
	DD	3230017541
	DD	3373913644
	DD	3189567103
	DD	594007232
	DD	3230017544
	DD	2818265402
	DD	3189568021
	DD	1356644520
	DD	3230017546
	DD	1075601108
	DD	3189567716
	DD	2109348112
	DD	3230017548
	DD	3342127702
	DD	3189567566
	DD	2852139088
	DD	3230017550
	DD	2910355434
	DD	3189567504
	DD	3585038464
	DD	3230017552
	DD	611858749
	DD	3189567074
	DD	13099880
	DD	3230017555
	DD	1954698831
	DD	3189567516
	DD	726278816
	DD	3230017557
	DD	550712630
	DD	3189567710
	DD	1429628792
	DD	3230017559
	DD	3672474493
	DD	3189567278
	DD	2123170544
	DD	3230017561
	DD	1160875209
	DD	3189567651
	DD	2806924760
	DD	3230017563
	DD	2242524344
	DD	3189568000
	DD	3480912064
	DD	3230017565
	DD	2823138691
	DD	3189567327
	DD	4145152992
	DD	3230017567
	DD	3521079748
	DD	3189567568
	DD	504700744
	DD	3230017570
	DD	1136670137
	DD	3189567486
	DD	1149510328
	DD	3230017572
	DD	806933442
	DD	3189567823
	DD	1784634816
	DD	3230017574
	DD	1606084669
	DD	3189567242
	DD	2410094488
	DD	3230017576
	DD	2841342625
	DD	3189567480
	DD	3025909584
	DD	3230017578
	DD	984188059
	DD	3189567239
	DD	3632100264
	DD	3230017580
	DD	3011730434
	DD	3189567337
	DD	4228686632
	DD	3230017582
	DD	328432041
	DD	3189567676
	DD	520721440
	DD	3230017585
	DD	2057225622
	DD	3189567268
	DD	1098159248
	DD	3230017587
	DD	3150727306
	DD	3189567348
	DD	1666052680
	DD	3230017589
	DD	3291380622
	DD	3189567307
	DD	2224421584
	DD	3230017591
	DD	3570771984
	DD	3189567798
	DD	2773285760
	DD	3230017593
	DD	2427604440
	DD	3189567698
	DD	3312664936
	DD	3230017595
	DD	3733848165
	DD	3189567211
	DD	3842578768
	DD	3230017597
	DD	2673912350
	DD	3189567904
	DD	68079584
	DD	3230017600
	DD	960995892
	DD	3189567616
	DD	579121512
	DD	3230017602
	DD	4150493479
	DD	3189567659
	DD	1080756744
	DD	3230017604
	DD	1054941272
	DD	3189567735
	DD	1573004704
	DD	3230017606
	DD	2574365292
	DD	3189568007
	DD	2055884768
	DD	3230017608
	DD	2507498673
	DD	3189567092
	DD	2529416224
	DD	3230017610
	DD	2082176323
	DD	3189567207
	DD	2993618320
	DD	3230017612
	DD	3719982601
	DD	3189568058
	DD	3448510256
	DD	3230017614
	DD	4104247904
	DD	3189567897
	DD	3894111152
	DD	3230017616
	DD	3735557011
	DD	3189567647
	DD	35472776
	DD	3230017619
	DD	2914260983
	DD	3189567862
	DD	462548736
	DD	3230017621
	DD	1334094042
	DD	3189567734
	DD	880390688
	DD	3230017623
	DD	1291097379
	DD	3189567171
	DD	1289017512
	DD	3230017625
	DD	1627119803
	DD	3189567853
	DD	1688448064
	DD	3230017627
	DD	3886873723
	DD	3189567117
	DD	2078701104
	DD	3230017629
	DD	627887757
	DD	3189567206
	DD	2459795360
	DD	3230017631
	DD	1722013861
	DD	3189567101
	DD	2831749488
	DD	3230017633
	DD	4163262354
	DD	3189567678
	DD	3194582104
	DD	3230017635
	DD	4170237688
	DD	3189567639
	DD	3548311752
	DD	3230017637
	DD	47563772
	DD	3189567637
	DD	3892956928
	DD	3230017639
	DD	2990136007
	DD	3189567231
	DD	4228536056
	DD	3230017641
	DD	4294532248
	DD	3189567995
	DD	260100240
	DD	3230017644
	DD	2060709623
	DD	3189567397
	DD	577602376
	DD	3230017646
	DD	1488525373
	DD	3189568044
	DD	886093456
	DD	3230017648
	DD	1657840200
	DD	3189567474
	DD	1185591680
	DD	3230017650
	DD	3155771782
	DD	3189567399
	DD	1476115208
	DD	3230017652
	DD	2195162329
	DD	3189567591
	DD	1757682144
	DD	3230017654
	DD	1292806663
	DD	3189567956
	DD	2030310544
	DD	3230017656
	DD	2151451208
	DD	3189567540
	DD	2294018392
	DD	3230017658
	DD	2224122735
	DD	3189567630
	DD	2548823632
	DD	3230017660
	DD	3554417607
	DD	3189567684
	DD	2794744152
	DD	3230017662
	DD	3831650627
	DD	3189567409
	DD	3031797776
	DD	3230017664
	DD	4024246224
	DD	3189567811
	DD	3260002296
	DD	3230017666
	DD	2530219738
	DD	3189567128
	DD	3479375416
	DD	3230017668
	DD	1618145996
	DD	3189568024
	DD	3689934832
	DD	3230017670
	DD	392379700
	DD	3189567378
	DD	3891698144
	DD	3230017672
	DD	55874694
	DD	3189567574
	DD	4084682928
	DD	3230017674
	DD	4294251475
	DD	3189567312
	DD	4268906688
	DD	3230017676
	DD	1899705040
	DD	3189567783
	DD	149419600
	DD	3230017679
	DD	357699389
	DD	3189567568
	DD	316173656
	DD	3230017681
	DD	3025711727
	DD	3189567790
	DD	474218920
	DD	3230017683
	DD	2921891821
	DD	3189568046
	DD	623572704
	DD	3230017685
	DD	1716968749
	DD	3189567453
	DD	764252248
	DD	3230017687
	DD	2393171526
	DD	3189567748
	DD	896274768
	DD	3230017689
	DD	1343317993
	DD	3189567172
	DD	1019657400
	DD	3230017691
	DD	1388235839
	DD	3189567661
	DD	1134417256
	DD	3230017693
	DD	2766147125
	DD	3189567706
	DD	1240571384
	DD	3230017695
	DD	2162068842
	DD	3189567499
	DD	1338136776
	DD	3230017697
	DD	4075455588
	DD	3189567932
	DD	1427130392
	DD	3230017699
	DD	1579647664
	DD	3189567555
	DD	1507569120
	DD	3230017701
	DD	3785749868
	DD	3189567715
	DD	1579469816
	DD	3230017703
	DD	2639950365
	DD	3189567469
	DD	1642849272
	DD	3230017705
	DD	3611501026
	DD	3189567698
	DD	1697724240
	DD	3230017707
	DD	15422953
	DD	3189568066
	DD	1744111424
	DD	3230017709
	DD	2642213241
	DD	3189568062
	DD	1782027472
	DD	3230017711
	DD	143606300
	DD	3189568058
	DD	1811488992
	DD	3230017713
	DD	501328690
	DD	3189567276
	DD	1832512520
	DD	3230017715
	DD	3258054578
	DD	3189567915
	DD	1845114576
	DD	3230017717
	DD	1297851139
	DD	3189568007
	DD	1849311616
	DD	3230017719
	DD	3589200000
	DD	3189567580
	DB 0
	ORG $+46
	DB	0
	DD	0
	DD	2147483648
	DD	2846560486
	DD	3210737666
	DD	2729511872
	DD	3211784202
	DD	3956813460
	DD	3212307985
	DD	1796237193
	DD	3212828714
	DD	1562748889
	DD	3213092649
	DD	1602292343
	DD	3213352007
	DD	745814092
	DD	3213610865
	DD	2969306084
	DD	3213869224
	DD	3953060839
	DD	3214006263
	DD	1128703349
	DD	3214134948
	DD	1371067273
	DD	3214263386
	DD	131445019
	DD	3214391579
	DD	1427948820
	DD	3214519527
	DD	665817496
	DD	3214647232
	DD	1817459908
	DD	3214774694
	DD	242758400
	DD	3214901915
	DD	2082038134
	DD	3214981455
	DD	2300828032
	DD	3215044825
	DD	571795571
	DD	3215108076
	DD	3120479584
	DD	3215171207
	DD	3276640540
	DD	3215234220
	DD	2949178581
	DD	3215297115
	DD	4036279704
	DD	3215359892
	DD	4130528508
	DD	3215422552
	DD	813954767
	DD	3215485096
	DD	248046571
	DD	3215547523
	DD	4288926261
	DD	3215609833
	DD	1897492927
	DD	3215672029
	DD	3499237165
	DD	3215734109
	DD	2329611090
	DD	3215796075
	DD	203907094
	DD	3215857927
	DD	3222364838
	DD	3215919664
	DD	295408387
	DD	3215981289
	DD	899212401
	DD	3216012696
	DD	2606049022
	DD	3216043395
	DD	4003691233
	DD	3216074038
	DD	1675303236
	DD	3216104626
	DD	789189655
	DD	3216135158
	DD	2213928479
	DD	3216165634
	DD	2518405585
	DD	3216196055
	DD	2561783534
	DD	3216226421
	DD	3198568176
	DD	3216256732
	DD	983674944
	DD	3216286989
	DD	1057364031
	DD	3216317191
	DD	4260371496
	DD	3216347338
	DD	2838974660
	DD	3216377432
	DD	1919860988
	DD	3216407472
	DD	2330291020
	DD	3216437458
	DD	598130195
	DD	3216467391
	DD	1836782285
	DD	3216497270
	DD	2565351466
	DD	3216527096
	DD	3593575201
	DD	3216556869
	DD	1431920350
	DD	3216586590
	DD	1176515501
	DD	3216616258
	DD	3624279252
	DD	3216645873
	DD	977982819
	DD	3216675437
	DD	2616083454
	DD	3216704948
	DD	733015457
	DD	3216734408
	DD	403990373
	DD	3216763816
	DD	2405156675
	DD	3216793172
	DD	3213628385
	DD	3216822477
	DD	3597448029
	DD	3216851731
	DD	25680167
	DD	3216880935
	DD	1848308454
	DD	3216910087
	DD	1231492199
	DD	3216939189
	DD	3222364838
	DD	3216968240
	DD	4274224607
	DD	3216997241
	DD	836496059
	DD	3217026193
	DD	1119829323
	DD	3217043131
	DD	315251054
	DD	3217057557
	DD	518461891
	DD	3217071958
	DD	2094042713
	DD	3217086334
	DD	1109735857
	DD	3217100686
	DD	2221359788
	DD	3217115013
	DD	1492952593
	DD	3217129316
	DD	3576653728
	DD	3217143594
	DD	237880003
	DD	3217157849
	DD	420109003
	DD	3217172079
	DD	180120274
	DD	3217186285
	DD	4162843939
	DD	3217200466
	DD	4126536273
	DD	3217214624
	DD	417628115
	DD	3217228759
	DD	1970736719
	DD	3217242869
	DD	538873716
	DD	3217256956
	DD	758227830
	DD	3217271019
	DD	2968307242
	DD	3217285058
	DD	3211951040
	DD	3217299074
	DD	1825275166
	DD	3217313067
	DD	3437683677
	DD	3217327036
	DD	4086978021
	DD	3217340982
	DD	4104270002
	DD	3217354905
	DD	3819025464
	DD	3217368805
	DD	3559075183
	DD	3217382682
	DD	3650625670
	DD	3217396536
	DD	123302585
	DD	3217410368
	DD	1890030548
	DD	3217424176
	DD	682272600
	DD	3217437962
	DD	1114811729
	DD	3217451725
	DD	3505892050
	DD	3217465465
	DD	3877229085
	DD	3217479183
	DD	2543954549
	DD	3217492879
	DD	4114626469
	DD	3217506552
	DD	311370023
	DD	3217520204
	DD	34658569
	DD	3217533833
	DD	3593519732
	DD	3217547439
	DD	2705545195
	DD	3217561024
	DD	1971736889
	DD	3217574587
	DD	1696647441
	DD	3217588128
	DD	2183357019
	DD	3217601647
	DD	3733482817
	DD	3217615144
	DD	2352221150
	DD	3217628620
	DD	2633258676
	DD	3217642074
	DD	578912458
	DD	3217655507
	DD	780008323
	DD	3217668918
	DD	3530988084
	DD	3217682307
	DD	534951267
	DD	3217695676
	DD	673467853
	DD	3217709023
	DD	4236783385
	DD	3217722348
	DD	2923827788
	DD	3217735653
	DD	1317060595
	DD	3217748937
	DD	3997577740
	DD	3217762199
	DD	2660218276
	DD	3217775441
	DD	1883376699
	DD	3217788662
	DD	1949142230
	DD	3217801862
	DD	3138274530
	DD	3217815041
	DD	1435244737
	DD	3217828200
	DD	1413145637
	DD	3217841338
	DD	3348797986
	DD	3217854455
	DD	3222758658
	DD	3217867552
	DD	1309263324
	DD	3217880629
	DD	2176234471
	DD	3217893685
	DD	1800387473
	DD	3217906721
	DD	452140382
	DD	3217919737
	DD	2695621757
	DD	3217932732
	DD	208809263
	DD	3217945708
	DD	1848308454
	DD	3217958663
	DD	3584589359
	DD	3217971598
	DD	1386895968
	DD	3217984514
	DD	4108155659
	DD	3217997409
	DD	3420215606
	DD	3218010285
	DD	3878621277
	DD	3218023141
	DD	1447787319
	DD	3218035978
	DD	680874053
	DD	3218048795
	DD	1834892843
	DD	3218061592
	DD	870713292
	DD	3218074370
	DD	1168986141
	DD	3218083436
	DD	3245089583
	DD	3218089805
	DD	2494877761
	DD	3218096165
	DD	3338922950
	DD	3218102515
	DD	1607293197
	DD	3218108856
	DD	1719424943
	DD	3218115187
	DD	3799224547
	DD	3218121508
	DD	3675071677
	DD	3218127820
	DD	1469757259
	DD	3218134123
	DD	1600486820
	DD	3218140416
	DD	4188949205
	DD	3218146699
	DD	766352567
	DD	3218152974
	DD	43231408
	DD	3218159239
	DD	2139646042
	DD	3218165494
	DD	2880153102
	DD	3218171740
	DD	2383743325
	DD	3218177977
	DD	768877423
	DD	3218184205
	DD	2448456521
	DD	3218190423
	DD	3244923387
	DD	3218196632
	DD	3275167422
	DD	3218202832
	DD	2655560435
	DD	3218209023
	DD	1501959692
	DD	3218215205
	DD	4224678246
	DD	3218221377
	DD	2348618756
	DD	3218227541
	DD	283080250
	DD	3218233696
	DD	2436859197
	DD	3218239841
	DD	333350563
	DD	3218245978
	DD	2675321801
	DD	3218252105
	DD	985177381
	DD	3218258224
	DD	3964700035
	DD	3218264333
	DD	3135315249
	DD	3218270434
	DD	2902865164
	DD	3218276526
	DD	3377742213
	DD	3218282609
	DD	374891912
	DD	3218288684
	DD	2593684821
	DD	3218294749
	DD	1553148222
	DD	3218300806
	DD	1656739931
	DD	3218306854
	DD	3012481825
	DD	3218312893
	DD	1432962538
	DD	3218318924
	DD	1320242026
	DD	3218324946
	DD	2780952336
	DD	3218330959
	DD	1626300240
	DD	3218336964
	DD	2256971748
	DD	3218342960
	DD	483265518
	DD	3218348948
	DD	704964628
	DD	3218354927
	DD	3026437244
	DD	3218360897
	DD	3256639170
	DD	3218366859
	DD	1499050969
	DD	3218372813
	DD	2151680465
	DD	3218378758
	DD	1022163351
	DD	3218384695
	DD	2507634849
	DD	3218390623
	DD	2414862975
	DD	3218396543
	DD	845152874
	DD	3218402455
	DD	2194349235
	DD	3218408358
	DD	2267936813
	DD	3218414253
	DD	1165944702
	DD	3218420140
	DD	3282948707
	DD	3218426018
	DD	128204514
	DD	3218431889
	DD	390421100
	DD	3218437751
	DD	4167959282
	DD	3218443604
	DD	2968834018
	DD	3218449450
	DD	1185553177
	DD	3218455288
	DD	3210217930
	DD	3218461117
	DD	549623114
	DD	3218466939
	DD	1890030548
	DD	3218472752
	DD	3032400188
	DD	3218478557
	DD	4072261525
	DD	3218484354
	DD	3450874911
	DD	1070995813
	DD	2331021804
	DD	1070990032
	DD	1029453230
	DD	1070984259
	DD	3747176240
	DD	1070978493
	DD	1800682930
	DD	1070972736
	DD	3686719393
	DD	1070966986
	DD	722545241
	DD	1070961245
	DD	1405669883
	DD	1070955511
	DD	1349079372
	DD	1070949785
	DD	461103529
	DD	1070944067
	DD	2945413886
	DD	1070938356
	DD	121152472
	DD	1070932654
	DD	487700860
	DD	1070926959
	DD	3954874384
	DD	1070921271
	DD	1842920138
	DD	1070915592
	DD	2652318780
	DD	1070909920
	DD	1999011482
	DD	1070904256
	DD	4089234463
	DD	1070898599
	DD	244680560
	DD	1070892951
	DD	3262235675
	DD	1070887309
	DD	169336595
	DD	1070881676
	DD	3763576649
	DD	1070876049
	DD	1073096258
	DD	1070870431
	DD	601221319
	DD	1070864820
	DD	2261690268
	DD	1070859216
	DD	1673619518
	DD	1070853620
	DD	3046403514
	DD	1070848031
	DD	1999843716
	DD	1070842450
	DD	2744015967
	DD	1070836876
	DD	899399508
	DD	1070831310
	DD	676744370
	DD	1070825751
	DD	1992167703
	DD	1070820199
	DD	467152011
	DD	1070814655
	DD	313445281
	DD	1070809118
	DD	1448157352
	DD	1070803588
	DD	3788725476
	DD	1070798065
	DD	2957945300
	DD	1070792550
	DD	3168871046
	DD	1070787042
	DD	44944622
	DD	1070781542
	DD	2094830420
	DD	1070776048
	DD	647642559
	DD	1070770562
	DD	4212681596
	DD	1070765082
	DD	4119694493
	DD	1070759610
	DD	288676754
	DD	1070754146
	DD	1229870791
	DD	1070748688
	DD	2568927817
	DD	1070743237
	DD	4226775419
	DD	1070737793
	DD	1829681364
	DD	1070732357
	DD	3889121191
	DD	1070726927
	DD	1737005556
	DD	1070721505
	DD	3885417029
	DD	1070716089
	DD	1666870165
	DD	1070710681
	DD	3594048319
	DD	1070705279
	DD	1000063738
	DD	1070699885
	DD	2398194399
	DD	1070694497
	DD	3417111411
	DD	1070689116
	DD	3980746691
	DD	1070683742
	DD	4013324163
	DD	1070678375
	DD	3439358267
	DD	1070673015
	DD	2183652476
	DD	1070667662
	DD	171297820
	DD	1070662316
	DD	1622638721
	DD	1070656976
	DD	2168369647
	DD	1070651643
	DD	1734435555
	DD	1070646317
	DD	247063161
	DD	1070640998
	DD	1927726804
	DD	1070635685
	DD	2408245143
	DD	1070630379
	DD	1615681634
	DD	1070625080
	DD	3772343130
	DD	1070619787
	DD	215909304
	DD	1070614502
	DD	3759169634
	DD	1070609222
	DD	1445381777
	DD	1070603950
	DD	1792910451
	DD	1070598684
	DD	870975437
	DD	1070590754
	DD	3194096222
	DD	1070580248
	DD	1823716217
	DD	1070569756
	DD	913800688
	DD	1070559277
	DD	323875369
	DD	1070548811
	DD	4208958430
	DD	1070538357
	DD	3839688678
	DD	1070527917
	DD	3372126730
	DD	1070517490
	DD	2667883253
	DD	1070507076
	DD	1589083690
	DD	1070496675
	DD	4293333010
	DD	1070486286
	DD	2053843986
	DD	1070475911
	DD	3324205749
	DD	1070465548
	DD	3673610217
	DD	1070455198
	DD	2966718784
	DD	1070444861
	DD	1068692560
	DD	1070434537
	DD	2140157205
	DD	1070424225
	DD	1752298597
	DD	1070413926
	DD	4066729595
	DD	1070403639
	DD	360651144
	DD	1070393366
	DD	3386588242
	DD	1070383104
	DD	127747308
	DD	1070372856
	DD	3337621361
	DD	1070362619
	DD	380113
	DD	1070352396
	DD	2870475188
	DD	1070342184
	DD	3227997535
	DD	1070331985
	DD	943446189
	DD	1070321799
	DD	182758681
	DD	1070311625
	DD	817374162
	DD	1070301463
	DD	2719198427
	DD	1070291313
	DD	1465634365
	DD	1070281176
	DD	1224481606
	DD	1070271051
	DD	1869032404
	DD	1070260938
	DD	3273036717
	DD	1070250837
	DD	1015732707
	DD	1070240749
	DD	3561713742
	DD	1070230672
	DD	2196155141
	DD	1070220608
	DD	1089583089
	DD	1070210556
	DD	118003300
	DD	1070200516
	DD	3452833475
	DD	1070190487
	DD	2381031992
	DD	1070180471
	DD	1074899573
	DD	1070170467
	DD	3707175295
	DD	1070160474
	DD	1566132617
	DD	1070150494
	DD	3120348377
	DD	1070140525
	DD	3953929663
	DD	1070130568
	DD	3946380940
	DD	1070120623
	DD	2977634725
	DD	1070110690
	DD	928049556
	DD	1070100769
	DD	1973375279
	DD	1070090859
	DD	1699849154
	DD	1070080961
	DD	4284063055
	DD	1070071074
	DD	1018125008
	DD	1070061200
	DD	374428300
	DD	1070051337
	DD	2235845747
	DD	1070041485
	DD	2190695046
	DD	1070031645
	DD	122671437
	DD	1070021817
	DD	210845780
	DD	1070012000
	DD	2339728060
	DD	1070002194
	DD	2099265479
	DD	1069992400
	DD	3669742467
	DD	1069982617
	DD	2641909617
	DD	1069972846
	DD	3196851002
	DD	1069963086
	DD	926113142
	DD	1069953338
	DD	11572342
	DD	1069943601
	DD	340530969
	DD	1069933875
	DD	1800682930
	DD	1069924160
	DD	4280111857
	DD	1069914456
	DD	3372322013
	DD	1069904764
	DD	3261138386
	DD	1069895083
	DD	3835803021
	DD	1069885413
	DD	690973253
	DD	1069875755
	DD	2306589128
	DD	1069866107
	DD	4278067876
	DD	1069856470
	DD	2201204065
	DD	1069846845
	DD	262102455
	DD	1069837231
	DD	2647241692
	DD	1069827627
	DD	658570708
	DD	1069818035
	DD	2778278092
	DD	1069808453
	DD	309052031
	DD	1069798883
	DD	1733817001
	DD	1069789323
	DD	2650961017
	DD	1069779774
	DD	2954203158
	DD	1069770236
	DD	2537624622
	DD	1069760709
	DD	1295667077
	DD	1069751193
	DD	3418098326
	DD	1069741687
	DD	210141498
	DD	1069732193
	DD	157244505
	DD	1069722709
	DD	3155274661
	DD	1069713235
	DD	510517079
	DD	1069703773
	DD	709476866
	DD	1069694321
	DD	3649073763
	DD	1069684879
	DD	636640577
	DD	1069675449
	DD	159725394
	DD	1069666029
	DD	2116286250
	DD	1069656619
	DD	2109656884
	DD	1069647220
	DD	38479793
	DD	1069637832
	DD	96704707
	DD	1069628454
	DD	2183652476
	DD	1069619086
	DD	1904013563
	DD	1069609729
	DD	3452748429
	DD	1069600382
	DD	2435216853
	DD	1069591046
	DD	3047045639
	DD	1069581720
	DD	894257944
	DD	1069572405
	DD	173141005
	DD	1069563100
	DD	785342784
	DD	1069553805
	DD	970708326
	DD	1069541521
	DD	2645916898
	DD	1069522972
	DD	2106590683
	DD	1069504444
	DD	3453588195
	DD	1069485936
	DD	2198475040
	DD	1069467449
	DD	2443390270
	DD	1069448982
	DD	3996141691
	DD	1069430535
	DD	2370203065
	DD	1069412109
	DD	1669613218
	DD	1069393703
	DD	1704071394
	DD	1069375317
	DD	2283901796
	DD	1069356951
	DD	3220050856
	DD	1069338605
	DD	29117219
	DD	1069320280
	DD	1113218225
	DD	1069301974
	DD	1990183441
	DD	1069283688
	DD	2473421172
	DD	1069265422
	DD	2376948508
	DD	1069247176
	DD	1515388672
	DD	1069228950
	DD	3998935692
	DD	1069210743
	DD	1053482595
	DD	1069192557
	DD	1085389880
	DD	1069174390
	DD	3911679148
	DD	1069156242
	DD	760030536
	DD	1069138115
	DD	38583924
	DD	1069120007
	DD	1566132617
	DD	1069101918
	DD	867088104
	DD	1069083849
	DD	2056379433
	DD	1069065799
	DD	659581512
	DD	1069047769
	DD	792781808
	DD	1069029758
	DD	2277675977
	DD	1069011766
	DD	641565395
	DD	1068993794
	DD	2256601
	DD	1068975841
	DD	183156961
	DD	1068957907
	DD	1008239542
	DD	1068939992
	DD	2302040698
	DD	1068922096
	DD	3889657667
	DD	1068904219
	DD	1301778891
	DD	1068886362
	DD	2954550821
	DD	1068868523
	DD	84804484
	DD	1068850704
	DD	1109791498
	DD	1068832903
	DD	1562410668
	DD	1068815121
	DD	1271074840
	DD	1068797358
	DD	64741293
	DD	1068779614
	DD	2067876735
	DD	1068761888
	DD	2815553124
	DD	1068744181
	DD	2138347284
	DD	1068726493
	DD	4162338635
	DD	1068708823
	DD	129237761
	DD	1068691173
	DD	2756122533
	DD	1068673540
	DD	3285762921
	DD	1068655926
	DD	1551389854
	DD	1068638331
	DD	1681725707
	DD	1068620754
	DD	3511047527
	DD	1068603195
	DD	2579184841
	DD	1068585655
	DD	3016419377
	DD	1068568133
	DD	363613719
	DD	1068550630
	DD	3047045639
	DD	1068533144
	DD	2313634884
	DD	1068515677
	DD	296456961
	DD	1068497513
	DD	1367328035
	DD	1068462651
	DD	3221665916
	DD	1068427825
	DD	1242258971
	DD	1068393036
	DD	3697798734
	DD	1068358282
	DD	1678104687
	DD	1068323565
	DD	3453858508
	DD	1068288883
	DD	116861598
	DD	1068254238
	DD	4234736672
	DD	1068219627
	DD	2606283437
	DD	1068185053
	DD	3506114812
	DD	1068150514
	DD	2324914545
	DD	1068116011
	DD	3044269692
	DD	1068081543
	DD	1056797460
	DD	1068047111
	DD	346010435
	DD	1068012714
	DD	601410756
	DD	1067978352
	DD	1513453498
	DD	1067944025
	DD	2773542775
	DD	1067909733
	DD	4074027864
	DD	1067875476
	DD	813232056
	DD	1067841255
	DD	1275317999
	DD	1067807068
	DD	860480111
	DD	1067772916
	DD	3559777258
	DD	1067738798
	DD	480292498
	DD	1067704716
	DD	4204867691
	DD	1067670667
	DD	1547459502
	DD	1067636654
	DD	797775935
	DD	1067602675
	DD	1656501562
	DD	1067568730
	DD	3825228429
	DD	1067534819
	DD	2711485098
	DD	1067500943
	DD	2313634884
	DD	1067467101
	DD	376973398
	DD	1067416219
	DD	672386641
	DD	1067348671
	DD	630691842
	DD	1067281191
	DD	3960777458
	DD	1067213778
	DD	1488394470
	DD	1067146434
	DD	1220920383
	DD	1067079157
	DD	2577548405
	DD	1067011947
	DD	684247743
	DD	1066944805
	DD	3553625811
	DD	1066877729
	DD	2020150230
	DD	1066810721
	DD	4099880293
	DD	1066743779
	DD	630721729
	DD	1066676905
	DD	3927125538
	DD	1066610096
	DD	535440936
	DD	1066543355
	DD	2773516144
	DD	1066476679
	DD	1486051399
	DD	1066410070
	DD	808661260
	DD	1066285262
	DD	2227658502
	DD	1066152307
	DD	1813447778
	DD	1066019484
	DD	2743640746
	DD	1065886792
	DD	3904192454
	DD	1065754231
	DD	4184355568
	DD	1065621801
	DD	2476667369
	DD	1065489502
	DD	1971904113
	DD	1065357333
	DD	3138267131
	DD	1065097372
	DD	341338964
	DD	1064833554
	DD	1954052322
	DD	1064569994
	DD	1498975029
	DD	1064306693
	DD	2190050752
	DD	1063782660
	DD	1440057502
	DD	1063257089
	DD	1432705161
	DD	1062208000
	DD	0
	DD	0
	DB 0
	ORG $+54
	DB	0
	DD	2900053258
	DD	1070176668
	DD	2900053258
	DD	1070176668
	DD	2900053258
	DD	1070176668
	DD	2900053258
	DD	1070176668
	DD	2900053258
	DD	1070176668
	DD	2900053258
	DD	1070176668
	DD	2900053258
	DD	1070176668
	DD	2900053258
	DD	1070176668
	DD	1208323809
	DD	3218079745
	DD	1208323809
	DD	3218079745
	DD	1208323809
	DD	3218079745
	DD	1208323809
	DD	3218079745
	DD	1208323809
	DD	3218079745
	DD	1208323809
	DD	3218079745
	DD	1208323809
	DD	3218079745
	DD	1208323809
	DD	3218079745
	DD	1431651269
	DD	1070945621
	DD	1431651269
	DD	1070945621
	DD	1431651269
	DD	1070945621
	DD	1431651269
	DD	1070945621
	DD	1431651269
	DD	1070945621
	DD	1431651269
	DD	1070945621
	DD	1431651269
	DD	1070945621
	DD	1431651269
	DD	1070945621
	DD	4294965279
	DD	3219128319
	DD	4294965279
	DD	3219128319
	DD	4294965279
	DD	3219128319
	DD	4294965279
	DD	3219128319
	DD	4294965279
	DD	3219128319
	DD	4294965279
	DD	3219128319
	DD	4294965279
	DD	3219128319
	DD	4294965279
	DD	3219128319
	DD	4294967295
	DD	1048575
	DD	4294967295
	DD	1048575
	DD	4294967295
	DD	1048575
	DD	4294967295
	DD	1048575
	DD	4294967295
	DD	1048575
	DD	4294967295
	DD	1048575
	DD	4294967295
	DD	1048575
	DD	4294967295
	DD	1048575
	DD	0
	DD	1062207488
	DD	0
	DD	1062207488
	DD	0
	DD	1062207488
	DD	0
	DD	1062207488
	DD	0
	DD	1062207488
	DD	0
	DD	1062207488
	DD	0
	DD	1062207488
	DD	0
	DD	1062207488
	DD	0
	DD	1048576
	DD	0
	DD	1048576
	DD	0
	DD	1048576
	DD	0
	DD	1048576
	DD	0
	DD	1048576
	DD	0
	DD	1048576
	DD	0
	DD	1048576
	DD	0
	DD	1048576
	DD	4294967295
	DD	2146435071
	DD	4294967295
	DD	2146435071
	DD	4294967295
	DD	2146435071
	DD	4294967295
	DD	2146435071
	DD	4294967295
	DD	2146435071
	DD	4294967295
	DD	2146435071
	DD	4294967295
	DD	2146435071
	DD	4294967295
	DD	2146435071
	DD	4227858432
	DD	4294967295
	DD	4227858432
	DD	4294967295
	DD	4227858432
	DD	4294967295
	DD	4227858432
	DD	4294967295
	DD	4227858432
	DD	4294967295
	DD	4227858432
	DD	4294967295
	DD	4227858432
	DD	4294967295
	DD	4227858432
	DD	4294967295
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
	DD	3164471296
	DD	1031600026
	DD	3164471296
	DD	1031600026
	DD	3164471296
	DD	1031600026
	DD	3164471296
	DD	1031600026
	DD	3164471296
	DD	1031600026
	DD	3164471296
	DD	1031600026
	DD	3164471296
	DD	1031600026
	DD	3164471296
	DD	1031600026
	DD	0
	DD	1082564608
	DD	0
	DD	1082564608
	DD	0
	DD	1082564608
	DD	0
	DD	1082564608
	DD	0
	DD	1082564608
	DD	0
	DD	1082564608
	DD	0
	DD	1082564608
	DD	0
	DD	1082564608
	DD	0
	DD	1083176960
	DD	0
	DD	1083176960
	DD	0
	DD	1083176960
	DD	0
	DD	1083176960
	DD	0
	DD	1083176960
	DD	0
	DD	1083176960
	DD	0
	DD	1083176960
	DD	0
	DD	1083176960
	DD	0
	DD	1083174912
	DD	0
	DD	1083174912
	DD	0
	DD	1083174912
	DD	0
	DD	1083174912
	DD	0
	DD	1083174912
	DD	0
	DD	1083174912
	DD	0
	DD	1083174912
	DD	0
	DD	1083174912
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
	DD	0
	DD	2146435072
	DD	0
	DD	4293918720
	DB 0
	ORG $+46
	DB	0
	DD	0
	DD	1072693248
	DD	0
	DD	3220176896
	DB 0
	ORG $+46
	DB	0
	DD	0
	DD	0
	DD	0
	DD	2147483648
	DB 0
	ORG $+46
	DB	0
	DD	4230471070
	DD	1072693247
	DD	4054058109
	DD	1072693602
	DD	66486866
	DD	1072693958
	DD	890114735
	DD	1072694313
	DD	2230149170
	DD	1072694668
	DD	4086764978
	DD	1072695023
	DD	2165169729
	DD	1072695379
	DD	760505647
	DD	1072695735
	DD	4167915012
	DD	1072696090
	DD	3797638278
	DD	1072696446
	DD	3944817843
	DD	1072696802
	DD	314661576
	DD	1072697159
	DD	1497279289
	DD	1072697515
	DD	3197878970
	DD	1072697871
	DD	1121668664
	DD	1072698228
	DD	3858758362
	DD	1072698584
	DD	2819388932
	DD	1072698941
	DD	2298703190
	DD	1072699298
	DD	2296876715
	DD	1072699655
	DD	2814085144
	DD	1072700012
	DD	3850504175
	DD	1072700369
	DD	1111342268
	DD	1072700727
	DD	3186709833
	DD	1072701084
	DD	1486848153
	DD	1072701442
	DD	306900460
	DD	1072701800
	DD	3942010044
	DD	1072702157
	DD	3802418369
	DD	1072702515
	DD	4183268845
	DD	1072702873
	DD	789770349
	DD	1072703232
	DD	2212033708
	DD	1072703590
	DD	4155267917
	DD	1072703948
	DD	2324682034
	DD	1072704307
	DD	1015419769
	DD	1072704666
	DD	227657592
	DD	1072705025
	DD	4256539332
	DD	1072705383
	DD	217339693
	DD	1072705743
	DD	995137212
	DD	1072706102
	DD	2295141308
	DD	1072706461
	DD	4117528750
	DD	1072706820
	DD	2167509075
	DD	1072707180
	DD	740226466
	DD	1072707540
	DD	4130825173
	DD	1072707899
	DD	3749547615
	DD	1072708259
	DD	3891538155
	DD	1072708619
	DD	262006630
	DD	1072708980
	DD	1451064823
	DD	1072709340
	DD	3163922686
	DD	1072709700
	DD	1105790237
	DD	1072710061
	DD	3866779434
	DD	1072710421
	DD	2857133118
	DD	1072710782
	DD	2371996076
	DD	1072711143
	DD	2411545858
	DD	1072711504
	DD	2975960075
	DD	1072711865
	DD	4065416397
	DD	1072712226
	DD	1385125259
	DD	1072712588
	DD	3525199044
	DD	1072712949
	DD	1895881013
	DD	1072713311
	DD	792316372
	DD	1072713673
	DD	214683093
	DD	1072714035
	DD	163159208
	DD	1072714397
	DD	637922811
	DD	1072714759
	DD	1639152051
	DD	1072715121
	DD	3167025144
	DD	1072715483
	DD	926753066
	DD	1072715846
	DD	3508448741
	DD	1072716208
	DD	2322355974
	DD	1072716571
	DD	1663620514
	DD	1072716934
	DD	1532420874
	DD	1072717297
	DD	1928935631
	DD	1072717660
	DD	2853343421
	DD	1072718023
	DD	10855643
	DD	1072718387
	DD	1991585646
	DD	1072718750
	DD	205777654
	DD	1072719114
	DD	3243545138
	DD	1072719477
	DD	2515132442
	DD	1072719841
	DD	2315685863
	DD	1072720205
	DD	2645384460
	DD	1072720569
	DD	3504407352
	DD	1072720933
	DD	597966424
	DD	1072721298
	DD	2516175509
	DD	1072721662
	DD	669279317
	DD	1072722027
	DD	3647391802
	DD	1072722391
	DD	2860757796
	DD	1072722756
	DD	2604524079
	DD	1072723121
	DD	2878870196
	DD	1072723486
	DD	3683975751
	DD	1072723851
	DD	725053116
	DD	1072724217
	DD	2592216609
	DD	1072724582
	DD	695711428
	DD	1072724948
	DD	3625652011
	DD	1072725313
	DD	2792283678
	DD	1072725679
	DD	2490753696
	DD	1072726045
	DD	2721242094
	DD	1072726411
	DD	3483928965
	DD	1072726777
	DD	484027169
	DD	1072727144
	DD	2311651510
	DD	1072727510
	DD	377047673
	DD	1072727877
	DD	3270330585
	DD	1072728243
	DD	2401746054
	DD	1072728610
	DD	2066441831
	DD	1072728977
	DD	2264598438
	DD	1072729344
	DD	2996396456
	DD	1072729711
	DD	4262016525
	DD	1072730078
	DD	1766672054
	DD	1072730446
	DD	4100478400
	DD	1072730813
	DD	2673681794
	DD	1072731181
	DD	1781430419
	DD	1072731549
	DD	1423905224
	DD	1072731917
	DD	1601287218
	DD	1072732285
	DD	2313757470
	DD	1072732653
	DD	3561497114
	DD	1072733021
	DD	1049720044
	DD	1072733390
	DD	3368542109
	DD	1072733758
	DD	1928210032
	DD	1072734127
	DD	1023872485
	DD	1072734496
	DD	655710909
	DD	1072734865
	DD	823906800
	DD	1072735234
	DD	1528641723
	DD	1072735603
	DD	2770097299
	DD	1072735972
	DD	253487916
	DD	1072736342
	DD	2568929912
	DD	1072736711
	DD	1126670503
	DD	1072737081
	DD	221858853
	DD	1072737451
	DD	4149644189
	DD	1072737820
	DD	25306615
	DD	1072738191
	DD	733930073
	DD	1072738561
	DD	1980729381
	DD	1072738931
	DD	3765886716
	DD	1072739301
	DD	1794617020
	DD	1072739672
	DD	362069892
	DD	1072740043
	DD	3763394987
	DD	1072740413
	DD	3408840138
	DD	1072740784
	DD	3593555127
	DD	1072741155
	DD	22755203
	DD	1072741527
	DD	1286557569
	DD	1072741898
	DD	3090177599
	DD	1072742269
	DD	1138830730
	DD	1072742641
	DD	4022634348
	DD	1072743012
	DD	3151836718
	DD	1072743384
	DD	2821588053
	DD	1072743756
	DD	3032071334
	DD	1072744128
	DD	3783469604
	DD	1072744500
	DD	780998670
	DD	1072744873
	DD	2614776289
	DD	1072745245
	DD	695051099
	DD	1072745618
	DD	3611940982
	DD	1072745990
	DD	2775694698
	DD	1072746363
	DD	2481462955
	DD	1072746736
	DD	2729429233
	DD	1072747109
	DD	3519777067
	DD	1072747482
	DD	557722764
	DD	1072747856
	DD	2433384577
	DD	1072748229
	DD	557011640
	DD	1072748603
	DD	3518722331
	DD	1072748976
	DD	2728765908
	DD	1072749350
	DD	2482293578
	DD	1072749724
	DD	2779489315
	DD	1072750098
	DD	3620537156
	DD	1072750472
	DD	710653901
	DD	1072750847
	DD	2639958305
	DD	1072751221
	DD	818699997
	DD	1072751596
	DD	3836997855
	DD	1072751970
	DD	3105101637
	DD	1072752345
	DD	2918163045
	DD	1072752720
	DD	3276366555
	DD	1072753095
	DD	4179896701
	DD	1072753470
	DD	1333970782
	DD	1072753846
	DD	3328708054
	DD	1072754221
	DD	1574358645
	DD	1072754597
	DD	366074636
	DD	1072754973
	DD	3999008171
	DD	1072755348
	DD	3883409570
	DD	1072755724
	DD	19463802
	DD	1072756101
	DD	997290496
	DD	1072756477
	DD	2522107455
	DD	1072756853
	DD	299132543
	DD	1072757230
	DD	2918485574
	DD	1072757606
	DD	1790417244
	DD	1072757983
	DD	1210080196
	DD	1072758360
	DD	1177659842
	DD	1072758737
	DD	1693341657
	DD	1072759114
	DD	2757311177
	DD	1072759491
	DD	74786706
	DD	1072759869
	DD	2235888499
	DD	1072760246
	DD	650867689
	DD	1072760624
	DD	3909844658
	DD	1072761001
	DD	3423070662
	DD	1072761379
	DD	3485698915
	DD	1072761757
	DD	4097915390
	DD	1072762135
	DD	964938835
	DD	1072762514
	DD	2676889941
	DD	1072762892
	DD	644020286
	DD	1072763271
	DD	3456450689
	DD	1072763649
	DD	2524432850
	DD	1072764028
	DD	2143120419
	DD	1072764407
	DD	2312699817
	DD	1072764786
	DD	3033357524
	DD	1072765165
	DD	10312788
	DD	1072765545
	DD	1833686810
	DD	1072765924
	DD	4208698963
	DD	1072766303
	DD	2840568686
	DD	1072766683
	DD	2024450071
	DD	1072767063
	DD	1760529979
	DD	1072767443
	DD	2048995336
	DD	1072767823
	DD	2890033125
	DD	1072768203
	DD	4283830399
	DD	1072768583
	DD	1935606973
	DD	1072768964
	DD	140517323
	DD	1072769345
	DD	3193715983
	DD	1072769725
	DD	2505455666
	DD	1072770106
	DD	2370891033
	DD	1072770487
	DD	2790209517
	DD	1072770868
	DD	3763598611
	DD	1072771249
	DD	996278574
	DD	1072771631
	DD	3078371623
	DD	1072772012
	DD	1420130847
	DD	1072772394
	DD	316711292
	DD	1072772776
	DD	4063268064
	DD	1072773157
	DD	4070054446
	DD	1072773539
	DD	337258378
	DD	1072773922
	DD	1455002454
	DD	1072774304
	DD	3128507443
	DD	1072774686
	DD	1062994179
	DD	1072775069
	DD	3848585447
	DD	1072775451
	DD	2895534912
	DD	1072775834
	DD	2498998190
	DD	1072776217
	DD	2659163667
	DD	1072776600
	DD	3376219790
	DD	1072776983
	DD	355387776
	DD	1072777367
	DD	2186790791
	DD	1072777750
	DD	280682885
	DD	1072778134
	DD	3227187351
	DD	1072778517
	DD	2436558366
	DD	1072778901
	DD	2203952056
	DD	1072779285
	DD	2529557318
	DD	1072779669
	DD	3413563109
	DD	1072780053
	DD	561191158
	DD	1072780438
	DD	2562565143
	DD	1072780822
	DD	827939623
	DD	1072781207
	DD	3947438405
	DD	1072781591
	DD	3331316177
	DD	1072781976
	DD	3274729579
	DD	1072782361
	DD	3777868015
	DD	1072782746
	DD	545953663
	DD	1072783132
	DD	2169110651
	DD	1072783517
	DD	57593985
	DD	1072783903
	DD	2801527921
	DD	1072784288
	DD	1811167597
	DD	1072784674
	DD	1381670099
	DD	1072785060
	DD	1513225284
	DD	1072785446
	DD	2206023075
	DD	1072785832
	DD	3460253456
	DD	1072786218
	DD	981139179
	DD	1072786605
	DD	3358804952
	DD	1072786991
	DD	2003506360
	DD	1072787378
	DD	1210400941
	DD	1072787765
	DD	979679002
	DD	1072788152
	DD	1311530916
	DD	1072788539
	DD	2206147117
	DD	1072788926
	DD	3663718106
	DD	1072789313
	DD	1389467151
	DD	1072789701
	DD	3973519475
	DD	1072790088
	DD	2826131177
	DD	1072790476
	DD	2242460315
	DD	1072790864
	DD	2222697708
	DD	1072791252
	DD	2767034247
	DD	1072791640
	DD	3875660881
	DD	1072792028
	DD	1253801333
	DD	1072792417
	DD	3491581275
	DD	1072792805
	DD	1999257261
	DD	1072793194
	DD	1071987800
	DD	1072793583
	DD	709964165
	DD	1072793972
	DD	913377697
	DD	1072794361
	DD	1682419801
	DD	1072794750
	DD	3017281948
	DD	1072795139
	DD	623188375
	DD	1072795529
	DD	3090265274
	DD	1072795918
	DD	1828769718
	DD	1072796308
	DD	1133860732
	DD	1072796698
	DD	1005730110
	DD	1072797088
	DD	1444569709
	DD	1072797478
	DD	2450571457
	DD	1072797868
	DD	4023927340
	DD	1072798258
	DD	1869862117
	DD	1072798649
	DD	283535203
	DD	1072799040
	DD	3560106076
	DD	1072799430
	DD	3109832396
	DD	1072799821
	DD	3227873773
	DD	1072800212
	DD	3914422583
	DD	1072800603
	DD	874703977
	DD	1072800995
	DD	2698845057
	DD	1072801386
	DD	797103804
	DD	1072801778
	DD	3759607450
	DD	1072802169
	DD	2996614109
	DD	1072802561
	DD	2803283846
	DD	1072802953
	DD	3179809498
	DD	1072803345
	DD	4126383965
	DD	1072803737
	DD	1348232917
	DD	1072804130
	DD	3435483979
	DD	1072804522
	DD	1798395653
	DD	1072804915
	DD	732128398
	DD	1072805308
	DD	236875443
	DD	1072805701
	DD	312830080
	DD	1072806094
	DD	960185668
	DD	1072806487
	DD	2179135630
	DD	1072806880
	DD	3969873455
	DD	1072807273
	DD	2037625403
	DD	1072807667
	DD	677552389
	DD	1072808061
	DD	4184815396
	DD	1072808454
	DD	3969673583
	DD	1072808848
	DD	32320766
	DD	1072809243
	DD	962885422
	DD	1072809637
	DD	2466594201
	DD	1072810031
	DD	248673822
	DD	1072810426
	DD	2899252956
	DD	1072810820
	DD	1828591156
	DD	1072811215
	DD	1331849931
	DD	1072811610
	DD	1409223555
	DD	1072812005
	DD	2060906373
	DD	1072812400
	DD	3287092791
	DD	1072812795
	DD	793009988
	DD	1072813191
	DD	3168787097
	DD	1072813586
	DD	1824684131
	DD	1072813982
	DD	1055863057
	DD	1072814378
	DD	862518614
	DD	1072814774
	DD	1244845604
	DD	1072815170
	DD	2203038896
	DD	1072815566
	DD	3737293427
	DD	1072815962
	DD	1552836903
	DD	1072816359
	DD	4239798981
	DD	1072816755
	DD	3208440203
	DD	1072817152
	DD	2753923066
	DD	1072817549
	DD	2876442833
	DD	1072817946
	DD	3576194839
	DD	1072818343
	DD	558407182
	DD	1072818741
	DD	2413209922
	DD	1072819138
	DD	550863994
	DD	1072819536
	DD	3561499587
	DD	1072819933
	DD	2855377771
	DD	1072820331
	DD	2727661572
	DD	1072820729
	DD	3178546784
	DD	1072821127
	DD	4208229269
	DD	1072821525
	DD	1521937658
	DD	1072821924
	DD	3709802537
	DD	1072822322
	DD	2182085374
	DD	1072822721
	DD	1233949591
	DD	1072823120
	DD	865591383
	DD	1072823519
	DD	1077207007
	DD	1072823918
	DD	1868992789
	DD	1072824317
	DD	3241145124
	DD	1072824716
	DD	898893172
	DD	1072825116
	DD	3432368052
	DD	1072825515
	DD	2251831762
	DD	1072825915
	DD	1652448257
	DD	1072826315
	DD	1634414263
	DD	1072826715
	DD	2197926570
	DD	1072827115
	DD	3343182037
	DD	1072827515
	DD	775410292
	DD	1072827916
	DD	3084742920
	DD	1072828316
	DD	1681442385
	DD	1072828717
	DD	860673109
	DD	1072829118
	DD	622632283
	DD	1072829519
	DD	967517166
	DD	1072829920
	DD	1895525083
	DD	1072830321
	DD	3406853425
	DD	1072830722
	DD	1206732355
	DD	1072831124
	DD	3885293991
	DD	1072831525
	DD	2852801332
	DD	1072831927
	DD	2404419335
	DD	1072832329
	DD	2540345726
	DD	1072832731
	DD	3260778297
	DD	1072833133
	DD	270947612
	DD	1072833536
	DD	2160986190
	DD	1072833938
	DD	341157435
	DD	1072834341
	DD	3401593998
	DD	1072834743
	DD	2752559416
	DD	1072835146
	DD	2689219179
	DD	1072835549
	DD	3211771552
	DD	1072835952
	DD	25447565
	DD	1072836356
	DD	1720380209
	DD	1072836759
	DD	4001800650
	DD	1072837162
	DD	2574940123
	DD	1072837566
	DD	1734964523
	DD	1072837970
	DD	1482072516
	DD	1072838374
	DD	1816462831
	DD	1072838778
	DD	2738334272
	DD	1072839182
	DD	4247885705
	DD	1072839586
	DD	2050348766
	DD	1072839991
	DD	440889755
	DD	1072840396
	DD	3714675036
	DD	1072840800
	DD	3281969154
	DD	1072841205
	DD	3437938610
	DD	1072841610
	DD	4182782673
	DD	1072842015
	DD	1221733388
	DD	1072842421
	DD	3144924752
	DD	1072842826
	DD	1362621647
	DD	1072843232
	DD	169990911
	DD	1072843638
	DD	3862199448
	DD	1072844043
	DD	3849512343
	DD	1072844449
	DD	132129338
	DD	1072844856
	DD	1300184840
	DD	1072845262
	DD	3058911430
	DD	1072845668
	DD	1113541760
	DD	1072846075
	DD	4054210436
	DD	1072846481
	DD	3291182947
	DD	1072846888
	DD	3119626743
	DD	1072847295
	DD	3539742037
	DD	1072847702
	DD	256761821
	DD	1072848110
	DD	1860821041
	DD	1072848517
	DD	4057152821
	DD	1072848924
	DD	2550990355
	DD	1072849332
	DD	1637501494
	DD	1072849740
	DD	1316886866
	DD	1072850148
	DD	1589347160
	DD	1072850556
	DD	2455083139
	DD	1072850964
	DD	3914295631
	DD	1072851372
	DD	1672218237
	DD	1072851781
	DD	24019217
	DD	1072852190
	DD	3264866899
	DD	1072852598
	DD	2805027793
	DD	1072853007
	DD	2939670363
	DD	1072853416
	DD	3668995847
	DD	1072853825
	DD	698238252
	DD	1072854235
	DD	2617533544
	DD	1072854644
	DD	837148572
	DD	1072855054
	DD	3947219439
	DD	1072855463
	DD	3358013128
	DD	1072855873
	DD	3364698584
	DD	1072856283
	DD	3967477519
	DD	1072856693
	DD	871584420
	DD	1072857104
	DD	2667155729
	DD	1072857514
	DD	764458774
	DD	1072857925
	DD	3753630135
	DD	1072858335
	DD	3044937274
	DD	1072858746
	DD	2933549613
	DD	1072859157
	DD	3419669343
	DD	1072859568
	DD	208531430
	DD	1072859980
	DD	1890272795
	DD	1072860391
	DD	4170128542
	DD	1072860802
	DD	2753333840
	DD	1072861214
	DD	1935058519
	DD	1072861626
	DD	1715505184
	DD	1072862038
	DD	2094876506
	DD	1072862450
	DD	3073375228
	DD	1072862862
	DD	356236861
	DD	1072863275
	DD	2533598878
	DD	1072863687
	DD	1015729630
	DD	1072864100
	DD	97799432
	DD	1072864513
	DD	4074978662
	DD	1072864925
	DD	62568587
	DD	1072865339
	DD	945674314
	DD	1072865752
	DD	2429531839
	DD	1072866165
	DD	219377225
	DD	1072866579
	DD	2905348493
	DD	1072866992
	DD	1897714548
	DD	1072867406
	DD	1491646249
	DD	1072867820
	DD	1687347236
	DD	1072868234
	DD	2485021208
	DD	1072868648
	DD	3884871940
	DD	1072869062
	DD	1592135977
	DD	1072869477
	DD	4196951822
	DD	1072869891
	DD	3109588863
	DD	1072870306
	DD	2625218444
	DD	1072870721
	DD	2744044683
	DD	1072871136
	DD	3466271768
	DD	1072871551
	DD	497136658
	DD	1072871967
	DD	2426778270
	DD	1072872382
	DD	665466407
	DD	1072872798
	DD	3803340124
	DD	1072873213
	DD	3250669363
	DD	1072873629
	DD	3302626020
	DD	1072874045
	DD	3959414768
	DD	1072874461
	DD	926273050
	DD	1072874878
	DD	2793340269
	DD	1072875294
	DD	970886712
	DD	1072875711
	DD	4049051919
	DD	1072876127
	DD	3438106317
	DD	1072876544
	DD	3433222287
	DD	1072876961
	DD	4034604989
	DD	1072877378
	DD	947492350
	DD	1072877796
	DD	2762024260
	DD	1072878213
	DD	888471491
	DD	1072878631
	DD	3916974070
	DD	1072879048
	DD	3257802909
	DD	1072879466
	DD	3206130877
	DD	1072879884
	DD	3762163619
	DD	1072880302
	DD	631139552
	DD	1072880721
	DD	2403199048
	DD	1072881139
	DD	488613369
	DD	1072881558
	DD	3477523031
	DD	1072881976
	DD	2780199430
	DD	1072882395
	DD	2691815924
	DD	1072882814
	DD	3212578647
	DD	1072883233
	DD	47726500
	DD	1072883653
	DD	1787400350
	DD	1072884072
	DD	4136839237
	DD	1072884491
	DD	2801282279
	DD	1072884911
	DD	2075903250
	DD	1072885331
	DD	1960908701
	DD	1072885751
	DD	2456505253
	DD	1072886171
	DD	3562899594
	DD	1072886591
	DD	985331189
	DD	1072887012
	DD	3313941461
	DD	1072887432
	DD	1959002716
	DD	1072887853
	DD	1215689221
	DD	1072888274
	DD	1084208016
	DD	1072888695
	DD	1564766211
	DD	1072889116
	DD	2657570987
	DD	1072889537
	DD	67862297
	DD	1072889959
	DD	2385782055
	DD	1072890380
	DD	1021603060
	DD	1072890802
	DD	270500067
	DD	1072891224
	DD	132680608
	DD	1072891646
	DD	608352285
	DD	1072892068
	DD	1697722769
	DD	1072892490
	DD	3400999802
	DD	1072892912
	DD	1423423901
	DD	1072893335
	DD	60170245
	DD	1072893758
	DD	3606414082
	DD	1072894180
	DD	3472428843
	DD	1072894603
	DD	3953389919
	DD	1072895026
	DD	754538178
	DD	1072895450
	DD	2466016447
	DD	1072895873
	DD	498098438
	DD	1072896297
	DD	3440927120
	DD	1072896720
	DD	2704776347
	DD	1072897144
	DD	2584821931
	DD	1072897568
	DD	3081272460
	DD	1072897992
	DD	4194336593
	DD	1072898416
	DD	1629255761
	DD	1072898841
	DD	3976173357
	DD	1072899265
	DD	2645363658
	DD	1072899690
	DD	1932002900
	DD	1072900115
	DD	1836300097
	DD	1072900540
	DD	2358464328
	DD	1072900965
	DD	3498704748
	DD	1072901390
	DD	962263284
	DD	1072901816
	DD	3339283823
	DD	1072902241
	DD	2040041138
	DD	1072902667
	DD	1359711962
	DD	1072903093
	DD	1298505802
	DD	1072903519
	DD	1856632235
	DD	1072903945
	DD	3034300912
	DD	1072904371
	DD	536754255
	DD	1072904798
	DD	2954136649
	DD	1072905224
	DD	1696723363
	DD	1072905651
	DD	1059691625
	DD	1072906078
	DD	1043251442
	DD	1072906505
	DD	1647612887
	DD	1072906932
	DD	2872986106
	DD	1072907359
	DD	424614023
	DD	1072907787
	DD	2892641516
	DD	1072908214
	DD	1687344354
	DD	1072908642
	DD	1103900263
	DD	1072909070
	DD	1142519748
	DD	1072909498
	DD	1803413380
	DD	1072909926
	DD	3086791805
	DD	1072910354
	DD	697898441
	DD	1072910783
	DD	3226878671
	DD	1072911211
	DD	2084008759
	DD	1072911640
	DD	1564466932
	DD	1072912069
	DD	1668464192
	DD	1072912498
	DD	2396211611
	DD	1072912927
	DD	3747920335
	DD	1072913356
	DD	1428834282
	DD	1072913786
	DD	4029099333
	DD	1072914215
	DD	2958992255
	DD	1072914645
	DD	2513691773
	DD	1072915075
	DD	2693409391
	DD	1072915505
	DD	3498356681
	DD	1072915935
	DD	633777992
	DD	1072916366
	DD	2689819635
	DD	1072916796
	DD	1076758805
	DD	1072917227
	DD	89774659
	DD	1072917658
	DD	4024046424
	DD	1072918088
	DD	4289851512
	DD	1072918519
	DD	887402001
	DD	1072918951
	DD	2406844627
	DD	1072919382
	DD	258457017
	DD	1072919814
	DD	3032386058
	DD	1072920245
	DD	2138909516
	DD	1072920677
	DD	1873207124
	DD	1072921109
	DD	2235491389
	DD	1072921541
	DD	3225974889
	DD	1072921973
	DD	549902978
	DD	1072922406
	DD	2797422972
	DD	1072922838
	DD	1378813070
	DD	1072923271
	DD	589253439
	DD	1072923704
	DD	428957015
	DD	1072924137
	DD	898136809
	DD	1072924570
	DD	1997005904
	DD	1072925003
	DD	3725777455
	DD	1072925436
	DD	1789697393
	DD	1072925870
	DD	483946312
	DD	1072926304
	DD	4103704882
	DD	1072926737
	DD	4059251953
	DD	1072927171
	DD	350801040
	DD	1072927606
	DD	1568500325
	DD	1072928040
	DD	3417596172
	DD	1072928474
	DD	1603335018
	DD	1072928909
	DD	420897964
	DD	1072929344
	DD	4165466183
	DD	1072929778
	DD	4247319032
	DD	1072930213
	DD	666670536
	DD	1072930649
	DD	2013669380
	DD	1072931084
	DD	3993562437
	DD	1072931519
	DD	2311596648
	DD	1072931955
	DD	1262953624
	DD	1072932391
	DD	847847748
	DD	1072932827
	DD	1066493478
	DD	1072933263
	DD	1919105345
	DD	1072933699
	DD	3405897950
	DD	1072934135
	DD	1232118674
	DD	1072934572
	DD	3987916855
	DD	1072935008
	DD	3083572723
	DD	1072935445
	DD	2814268468
	DD	1072935882
	DD	3180219054
	DD	1072936319
	DD	4181639523
	DD	1072936756
	DD	1523777687
	DD	1072937194
	DD	3796783325
	DD	1072937631
	DD	2410937101
	DD	1072938069
	DD	1661421642
	DD	1072938507
	DD	1548452351
	DD	1072938945
	DD	2072244704
	DD	1072939383
	DD	3233014250
	DD	1072939821
	DD	736009314
	DD	1072940260
	DD	3171380184
	DD	1072940698
	DD	1949408036
	DD	1072941137
	DD	1365276005
	DD	1072941576
	DD	1419200007
	DD	1072942015
	DD	2111396029
	DD	1072942454
	DD	3442080132
	DD	1072942893
	DD	1116501150
	DD	1072943333
	DD	3724809886
	DD	1072943772
	DD	2677288023
	DD	1072944212
	DD	2269119214
	DD	1072944652
	DD	2500519883
	DD	1072945092
	DD	3371706530
	DD	1072945532
	DD	587928431
	DD	1072945973
	DD	2739336825
	DD	1072946413
	DD	1236213839
	DD	1072946854
	DD	373743562
	DD	1072947295
	DD	152142860
	DD	1072947736
	DD	571628671
	DD	1072948177
	DD	1632418011
	DD	1072948618
	DD	3334727963
	DD	1072949059
	DD	1383808394
	DD	1072949501
	DD	74843831
	DD	1072949943
	DD	3703018878
	DD	1072950384
	DD	3678616324
	DD	1072950826
	DD	1853621
	DD	1072951269
	DD	1262882892
	DD	1072951711
	DD	3166954440
	DD	1072952153
	DD	1419318643
	DD	1072952596
	DD	315160549
	DD	1072953039
	DD	4149665274
	DD	1072953481
	DD	38148827
	DD	1072953925
	DD	865731065
	DD	1072954368
	DD	2337662736
	DD	1072954811
	DD	159194662
	DD	1072955255
	DD	2920479626
	DD	1072955698
	DD	2031801301
	DD	1072956142
	DD	1788345322
	DD	1072956586
	DD	2190330102
	DD	1072957030
	DD	3237974129
	DD	1072957474
	DD	636528667
	DD	1072957919
	DD	2976146942
	DD	1072958363
	DD	1667113072
	DD	1072958808
	DD	1004613135
	DD	1072959253
	DD	988865989
	DD	1072959698
	DD	1620090564
	DD	1072960143
	DD	2898505866
	DD	1072960588
	DD	529363677
	DD	1072961034
	DD	3102817744
	DD	1072961479
	DD	2029152702
	DD	1072961925
	DD	1603555148
	DD	1072962371
	DD	1826244460
	DD	1072962817
	DD	2697440087
	DD	1072963263
	DD	4217361553
	DD	1072963709
	DD	2091261162
	DD	1072964156
	DD	614325883
	DD	1072964603
	DD	4081742759
	DD	1072965049
	DD	3903797021
	DD	1072965496
	DD	80708565
	DD	1072965944
	DD	1202631953
	DD	1072966391
	DD	2974819937
	DD	1072966838
	DD	1102525337
	DD	1072967286
	DD	4175902942
	DD	1072967733
	DD	3605238428
	DD	1072968181
	DD	3685719435
	DD	1072968629
	DD	122599084
	DD	1072969078
	DD	1506032460
	DD	1072969526
	DD	3541272834
	DD	1072969974
	DD	1933573553
	DD	1072970423
	DD	978122628
	DD	1072970872
	DD	675140853
	DD	1072971321
	DD	1024849091
	DD	1072971770
	DD	2027468284
	DD	1072972219
	DD	3683219449
	DD	1072972668
	DD	1697356377
	DD	1072973118
	DD	365067534
	DD	1072973568
	DD	3981541451
	DD	1072974017
	DD	3957064855
	DD	1072974467
	DD	291859135
	DD	1072974918
	DD	1576080346
	DD	1072975368
	DD	3514982734
	DD	1072975818
	DD	1813820617
	DD	1072976269
	DD	767782981
	DD	1072976720
	DD	377091592
	DD	1072977171
	DD	641968288
	DD	1072977622
	DD	1562634985
	DD	1072978073
	DD	3139313673
	DD	1072978524
	DD	1077259122
	DD	1072978976
	DD	3966628063
	DD	1072979427
	DD	3217708121
	DD	1072979879
	DD	3125688884
	DD	1072980331
	DD	3690792716
	DD	1072980783
	DD	618274764
	DD	1072981236
	DD	2498292134
	DD	1072981688
	DD	741132830
	DD	1072982141
	DD	3936954108
	DD	1072982593
	DD	3496044122
	DD	1072983046
	DD	3713592984
	DD	1072983499
	DD	294856292
	DD	1072983953
	DD	1829991607
	DD	1072984406
	DD	4024254678
	DD	1072984859
	DD	2582901329
	DD	1072985313
	DD	1801122051
	DD	1072985767
	DD	1679140116
	DD	1072986221
	DD	2217178870
	DD	1072986675
	DD	3415461735
	DD	1072987129
	DD	979244914
	DD	1072987584
	DD	3498686573
	DD	1072988038
	DD	2384075768
	DD	1072988493
	DD	1930603520
	DD	1072988948
	DD	2138493630
	DD	1072989403
	DD	3007969975
	DD	1072989858
	DD	244289211
	DD	1072990314
	DD	2437609958
	DD	1072990769
	DD	998221727
	DD	1072991225
	DD	221315995
	DD	1072991681
	DD	107117018
	DD	1072992137
	DD	655849126
	DD	1072992593
	DD	1867736728
	DD	1072993049
	DD	3743004307
	DD	1072993505
	DD	1986909126
	DD	1072993962
	DD	894643117
	DD	1072994419
	DD	466430991
	DD	1072994876
	DD	702497536
	DD	1072995333
	DD	1603067616
	DD	1072995790
	DD	3168366170
	DD	1072996247
	DD	1103650918
	DD	1072996705
	DD	3999081545
	DD	1072997162
	DD	3264948627
	DD	1072997620
	DD	3196444704
	DD	1072998078
	DD	3793795097
	DD	1072998536
	DD	762257908
	DD	1072998995
	DD	2691993199
	DD	1072999453
	DD	993291932
	DD	1072999912
	DD	4256314324
	DD	1073000370
	DD	3891351483
	DD	1073000829
	DD	4193596486
	DD	1073001288
	DD	868307891
	DD	1073001748
	DD	2505646223
	DD	1073002207
	DD	515902897
	DD	1073002667
	DD	3489238590
	DD	1073003126
	DD	2835944871
	DD	1073003586
	DD	2851215274
	DD	1073004046
	DD	3535276114
	DD	1073004506
	DD	593386486
	DD	1073004967
	DD	2615707447
	DD	1073005427
	DD	1012530953
	DD	1073005888
	DD	79050918
	DD	1073006349
	DD	4110461337
	DD	1073006809
	DD	222087097
	DD	1073007271
	DD	1299056934
	DD	1073007732
	DD	3046630484
	DD	1073008193
	DD	1170067454
	DD	1073008655
	DD	4259529517
	DD	1073009116
	DD	3725309241
	DD	1073009578
	DD	3862601154
	DD	1073010040
	DD	376665277
	DD	1073010503
	DD	1857663588
	DD	1073010965
	DD	4010856258
	DD	1073011427
	DD	2541503535
	DD	1073011890
	DD	1744800334
	DD	1073012353
	DD	1620974353
	DD	1073012816
	DD	2170253366
	DD	1073013279
	DD	3392865225
	DD	1073013742
	DD	994070562
	DD	1073014206
	DD	3564031974
	DD	1073014669
	DD	2513042955
	DD	1073015133
	DD	2136298958
	DD	1073015597
	DD	2434028221
	DD	1073016061
	DD	3406459059
	DD	1073016525
	DD	758852568
	DD	1073016990
	DD	3081371809
	DD	1073017454
	DD	1784310738
	DD	1073017919
	DD	1162865273
	DD	1073018384
	DD	1217264116
	DD	1073018849
	DD	1947736046
	DD	1073019314
	DD	3354509918
	DD	1073019779
	DD	1142847371
	DD	1073020245
	DD	3902912008
	DD	1073020710
	DD	3044998326
	DD	1073021176
	DD	2864302785
	DD	1073021642
	DD	3361054631
	DD	1073022108
	DD	240515889
	DD	1073022575
	DD	2092850551
	DD	1073023041
	DD	328353499
	DD	1073023508
	DD	3537188882
	DD	1073023974
	DD	3129651738
	DD	1073024441
	DD	3400939074
	DD	1073024908
	DD	56313381
	DD	1073025376
	DD	1685939117
	DD	1073025843
	DD	3995078930
	DD	1073026310
	DD	2688995542
	DD	1073026778
	DD	2062886350
	DD	1073027246
	DD	2116981530
	DD	1073027714
	DD	2851511338
	DD	1073028182
	DD	4266706105
	DD	1073028650
	DD	2067828946
	DD	1073029119
	DD	550077646
	DD	1073029588
	DD	4008650067
	DD	1073030056
	DD	3853842263
	DD	1073030525
	DD	85884954
	DD	1073030995
	DD	1294943536
	DD	1073031464
	DD	3186281590
	DD	1073031933
	DD	1465162777
	DD	1073032403
	DD	426785429
	DD	1073032873
	DD	71380660
	DD	1073033343
	DD	399179661
	DD	1073033813
	DD	1410413701
	DD	1073034283
	DD	3105314129
	DD	1073034753
	DD	1189145077
	DD	1073035224
	DD	4252072640
	DD	1073035694
	DD	3704393811
	DD	1073036165
	DD	3841307546
	DD	1073036636
	DD	368078290
	DD	1073037108
	DD	1874872453
	DD	1073037579
	DD	4066954636
	DD	1073038050
	DD	2649589519
	DD	1073038522
	DD	1917976452
	DD	1073038994
	DD	1872347569
	DD	1073039466
	DD	2512935078
	DD	1073039938
	DD	3839971271
	DD	1073040410
	DD	1558721219
	DD	1073040883
	DD	4259351963
	DD	1073041355
	DD	3352161436
	DD	1073041828
	DD	3132349536
	DD	1073042301
	DD	3600148950
	DD	1073042774
	DD	460825142
	DD	1073043248
	DD	2304545545
	DD	1073043721
	DD	541608488
	DD	1073044195
	DD	3762181559
	DD	1073044668
	DD	3376563243
	DD	1073045142
	DD	3679953995
	DD	1073045616
	DD	377619753
	DD	1073046091
	DD	2059728421
	DD	1073046565
	DD	136578804
	DD	1073047040
	DD	3198338962
	DD	1073047514
	DD	2655307857
	DD	1073047989
	DD	2802686414
	DD	1073048464
	DD	3640708341
	DD	1073048939
	DD	874640132
	DD	1073049415
	DD	3094650247
	DD	1073049890
	DD	1711038040
	DD	1073050366
	DD	1019004832
	DD	1073050842
	DD	1018784728
	DD	1073051318
	DD	1710611914
	DD	1073051794
	DD	3094720652
	DD	1073052270
	DD	876377990
	DD	1073052747
	DD	3645752941
	DD	1073053223
	DD	2813145416
	DD	1073053700
	DD	2673757290
	DD	1073054177
	DD	3227823225
	DD	1073054654
	DD	180610665
	DD	1073055132
	DD	2122289020
	DD	1073055609
	DD	463158596
	DD	1073056087
	DD	3793388967
	DD	1073056564
	DD	3523280594
	DD	1073057042
	DD	3948035914
	DD	1073057520
	DD	772922846
	DD	1073057999
	DD	2588111279
	DD	1073058477
	DD	803901998
	DD	1073058956
	DD	4010465051
	DD	1073059434
	DD	3618101379
	DD	1073059913
	DD	3922013897
	DD	1073060392
	DD	627471001
	DD	1073060872
	DD	2324643060
	DD	1073061351
	DD	423831335
	DD	1073061831
	DD	3515206355
	DD	1073062310
	DD	3009069540
	DD	1073062790
	DD	3200624281
	DD	1073063270
	DD	4090106753
	DD	1073063750
	DD	1382785912
	DD	1073064231
	DD	3668832687
	DD	1073064711
	DD	2358548899
	DD	1073065192
	DD	1747138337
	DD	1073065673
	DD	1834837577
	DD	1073066154
	DD	2621883273
	DD	1073066635
	DD	4108512157
	DD	1073067116
	DD	1999993751
	DD	1073067598
	DD	591532243
	DD	1073068080
	DD	4178331906
	DD	1073068561
	DD	4170695201
	DD	1073069043
	DD	568859265
	DD	1073069526
	DD	1962995905
	DD	1073070008
	DD	4058375122
	DD	1073070490
	DD	2560266994
	DD	1073070973
	DD	1763876276
	DD	1073071456
	DD	1669440503
	DD	1073071939
	DD	2277197293
	DD	1073072422
	DD	3587384346
	DD	1073072905
	DD	1305272140
	DD	1073073389
	DD	4021033130
	DD	1073073872
	DD	3144970662
	DD	1073074356
	DD	2972290050
	DD	1073074840
	DD	3503229396
	DD	1073075324
	DD	443059585
	DD	1073075809
	DD	2381953471
	DD	1073076293
	DD	730214803
	DD	1073076778
	DD	4078016597
	DD	1073077262
	DD	3835662765
	DD	1073077747
	DD	3391891
	DD	1073078233
	DD	1171377231
	DD	1073078718
	DD	3044890237
	DD	1073079203
	DD	1329202438
	DD	1073079689
	DD	319520039
	DD	1073080175
	DD	16082029
	DD	1073080661
	DD	419127475
	DD	1073081147
	DD	1528895530
	DD	1073081633
	DD	3345625422
	DD	1073082119
	DD	1574589172
	DD	1073082606
	DD	510993465
	DD	1073083093
	DD	155077776
	DD	1073083580
	DD	507081663
	DD	1073084067
	DD	1567244761
	DD	1073084554
	DD	3335806787
	DD	1073085041
	DD	1518040244
	DD	1073085529
	DD	409152307
	DD	1073086017
	DD	9382939
	DD	1073086505
	DD	318972182
	DD	1073086993
	DD	1338160159
	DD	1073087481
	DD	3067187074
	DD	1073087969
	DD	1211325919
	DD	1073088458
	DD	65784356
	DD	1073088947
	DD	3925770131
	DD	1073089435
	DD	4201589183
	DD	1073089924
	DD	893482122
	DD	1073090414
	DD	2591624233
	DD	1073090903
	DD	706321701
	DD	1073091393
	DD	3827749971
	DD	1073091882
	DD	3366215391
	DD	1073092372
	DD	3616926276
	DD	1073092862
	DD	285156430
	DD	1073093353
	DD	1961081627
	DD	1073093843
	DD	55008540
	DD	1073094334
	DD	3157113108
	DD	1073094824
	DD	2677702164
	DD	1073095315
	DD	2911984515
	DD	1073095806
	DD	3860201750
	DD	1073096297
	DD	1227628247
	DD	1073096789
	DD	3604440353
	DD	1073097280
	DD	2400945312
	DD	1073097772
	DD	1912352338
	DD	1073098264
	DD	2138903430
	DD	1073098756
	DD	3080840673
	DD	1073099248
	DD	443438931
	DD	1073099741
	DD	2816875045
	DD	1073100233
	DD	1611456749
	DD	1073100726
	DD	1122393751
	DD	1073101219
	DD	1349928543
	DD	1073101712
	DD	2294303696
	DD	1073102205
	DD	3955761869
	DD	1073102698
	DD	2039578504
	DD	1073103192
	DD	840963717
	DD	1073103686
	DD	360160411
	DD	1073104180
	DD	597411571
	DD	1073104674
	DD	1552960265
	DD	1073105168
	DD	3227049641
	DD	1073105662
	DD	1324955635
	DD	1073106157
	DD	141888859
	DD	1073106652
	DD	3973060003
	DD	1073107146
	DD	4228777955
	DD	1073107641
	DD	909286277
	DD	1073108137
	DD	2604763204
	DD	1073108632
	DD	725517870
	DD	1073109128
	DD	3861728676
	DD	1073109623
	DD	3423704921
	DD	1073110119
	DD	3706657876
	DD	1073110615
	DD	415864301
	DD	1073111112
	DD	2141502926
	DD	1073111608
	DD	293883382
	DD	1073112105
	DD	3463184564
	DD	1073112601
	DD	3059716268
	DD	1073113098
	DD	3378690259
	DD	1073113595
	DD	125383795
	DD	1073114093
	DD	1889976102
	DD	1073114590
	DD	82777307
	DD	1073115088
	DD	3293966802
	DD	1073115585
	DD	2933854879
	DD	1073116083
	DD	3297653802
	DD	1073116581
	DD	90641322
	DD	1073117080
	DD	1902997167
	DD	1073117578
	DD	145031958
	DD	1073118077
	DD	3406925586
	DD	1073118575
	DD	3098988842
	DD	1073119074
	DD	3516434485
	DD	1073119573
	DD	364540769
	DD	1073120073
	DD	2233487914
	DD	1073120572
	DD	533587044
	DD	1073121072
	DD	3855018547
	DD	1073121571
	DD	3608093714
	DD	1073122071
	DD	4088025801
	DD	1073122571
	DD	1000093561
	DD	1073123072
	DD	2934477717
	DD	1073123572
	DD	1301489888
	DD	1073124073
	DD	396343670
	DD	1073124574
	DD	219285440
	DD	1073125075
	DD	770561665
	DD	1073125576
	DD	2050418889
	DD	1073126077
	DD	4059103743
	DD	1073126578
	DD	2501895648
	DD	1073127080
	DD	1674008694
	DD	1073127582
	DD	1575689763
	DD	1073128084
	DD	2207185821
	DD	1073128586
	DD	3568743916
	DD	1073129088
	DD	1365643882
	DD	1073129591
	DD	4188067528
	DD	1073130093
	DD	3446327562
	DD	1073130596
	DD	3435638662
	DD	1073131099
	DD	4156248294
	DD	1073131602
	DD	1313436713
	DD	1073132106
	DD	3497386145
	DD	1073132609
	DD	2118409717
	DD	1073133113
	DD	1471722526
	DD	1073133617
	DD	1557572458
	DD	1073134121
	DD	2376207482
	DD	1073134625
	DD	3927875654
	DD	1073135129
	DD	1917857814
	DD	1073135634
	DD	641369479
	DD	1073136139
	DD	98658955
	DD	1073136644
	DD	289974633
	DD	1073137149
	DD	1215564985
	DD	1073137654
	DD	2875678570
	DD	1073138159
	DD	975596734
	DD	1073138665
	DD	4105502795
	DD	1073139170
	DD	3675710971
	DD	1073139676
	DD	3981437453
	DD	1073140182
	DD	727963924
	DD	1073140689
	DD	2505474039
	DD	1073141195
	DD	724282353
	DD	1073141702
	DD	3974572691
	DD	1073142208
	DD	3666659776
	DD	1073142715
	DD	4095760304
	DD	1073143222
	DD	967156467
	DD	1073143730
	DD	2871032423
	DD	1073144237
	DD	1217703235
	DD	1073144745
	DD	302385939
	DD	1073145253
	DD	125330356
	DD	1073145761
	DD	686786394
	DD	1073146269
	DD	1987004045
	DD	1073146777
	DD	4026233386
	DD	1073147285
	DD	2509757284
	DD	1073147794
	DD	1732793279
	DD	1073148303
	DD	1695591702
	DD	1073148812
	DD	2398402970
	DD	1073149321
	DD	3841477581
	DD	1073149830
	DD	1730098826
	DD	1073150340
	DD	359484670
	DD	1073150850
	DD	4024853164
	DD	1073151359
	DD	4136520556
	DD	1073151869
	DD	694737771
	DD	1073152380
	DD	2289690409
	DD	1073152890
	DD	331694973
	DD	1073153401
	DD	3410937236
	DD	1073153911
	DD	2937733868
	DD	1073154422
	DD	3207303516
	DD	1073154933
	DD	4219897616
	DD	1073155444
	DD	1680800389
	DD	1073155956
	DD	4180198033
	DD	1073156467
	DD	3128407647
	DD	1073156979
	DD	2820648301
	DD	1073157491
	DD	3257171856
	DD	1073158003
	DD	143262962
	DD	1073158516
	DD	2069108242
	DD	1073159028
	DD	445025220
	DD	1073159541
	DD	3861200690
	DD	1073160053
	DD	3727952347
	DD	1073160566
	DD	45532563
	DD	1073161080
	DD	1404128388
	DD	1073161593
	DD	3509025071
	DD	1073162106
	DD	2065507943
	DD	1073162620
	DD	1368797014
	DD	1073163134
	DD	1419145086
	DD	1073163648
	DD	2216805043
	DD	1073164162
	DD	3762029856
	DD	1073164676
	DD	1760105286
	DD	1073165191
	DD	506251772
	DD	1073165706
	DD	722541
	DD	1073166221
	DD	243770908
	DD	1073166736
	DD	1235650271
	DD	1073167251
	DD	2976614117
	DD	1073167766
	DD	1171948721
	DD	1073168282
	DD	116875034
	DD	1073168798
	DD	4106614097
	DD	1073169313
	DD	256517850
	DD	1073169830
	DD	1451742095
	DD	1073170346
	DD	3397573537
	DD	1073170862
	DD	1799298968
	DD	1073171379
	DD	952139854
	DD	1073171896
	DD	856350455
	DD	1073172413
	DD	1512185116
	DD	1073172930
	DD	2919898267
	DD	1073173447
	DD	784777130
	DD	1073173965
	DD	3697010899
	DD	1073174482
	DD	3066919672
	DD	1073175000
	DD	3189725520
	DD	1073175518
	DD	4065683305
	DD	1073176036
	DD	1400080679
	DD	1073176555
	DD	3783107268
	DD	1073177073
	DD	2625083600
	DD	1073177592
	DD	2221232180
	DD	1073178111
	DD	2571808300
	DD	1073178630
	DD	3677067340
	DD	1073179149
	DD	1242297469
	DD	1073179669
	DD	3857688835
	DD	1073180188
	DD	2933562482
	DD	1073180708
	DD	2765141434
	DD	1073181228
	DD	3352681500
	DD	1073181748
	DD	401471288
	DD	1073182269
	DD	2501701371
	DD	1073182789
	DD	1063693232
	DD	1073183310
	DD	382670324
	DD	1073183831
	DD	458888893
	DD	1073184352
	DD	1292605272
	DD	1073184873
	DD	2884075881
	DD	1073185394
	DD	938589928
	DD	1073185916
	DD	4046338599
	DD	1073186437
	DD	3617643980
	DD	1073186959
	DD	3947730136
	DD	1073187481
	DD	741886623
	DD	1073188004
	DD	2590304972
	DD	1073188526
	DD	903307621
	DD	1073189049
	DD	4271086275
	DD	1073189571
	DD	4103963542
	DD	1073190094
	DD	402196713
	DD	1073190618
	DD	1755977754
	DD	1073191141
	DD	3870596832
	DD	1073191664
	DD	2451344201
	DD	1073192188
	DD	1793444794
	DD	1073192712
	DD	1897156335
	DD	1073193236
	DD	2762736634
	DD	1073193760
	DD	95476296
	DD	1073194285
	DD	2485567899
	DD	1073194809
	DD	1343334923
	DD	1073195334
	DD	964002825
	DD	1073195859
	DD	1347829853
	DD	1073196384
	DD	2495074343
	DD	1073196909
	DD	111027422
	DD	1073197435
	DD	2785882192
	DD	1073197960
	DD	1929962659
	DD	1073198486
	DD	1838494805
	DD	1073199012
	DD	2511737402
	DD	1073199538
	DD	3949949312
	DD	1073200064
	DD	1858422186
	DD	1073200591
	DD	532382357
	DD	1073201118
	DD	4267056243
	DD	1073201644
	DD	177801170
	DD	1073202172
	DD	1149778323
	DD	1073202699
	DD	2888279793
	DD	1073203226
	DD	1098597760
	DD	1073203754
	DD	75959080
	DD	1073204282
	DD	4115590701
	DD	1073204809
	DD	332850474
	DD	1073205338
	DD	1612900112
	DD	1073205866
	DD	3661032234
	DD	1073206394
	DD	2182539545
	DD	1073206923
	DD	1472649431
	DD	1073207452
	DD	1531622072
	DD	1073207981
	DD	2359717732
	DD	1073208510
	DD	3957196766
	DD	1073209039
	DD	2029352322
	DD	1073209569
	DD	871412225
	DD	1073210099
	DD	483637094
	DD	1073210629
	DD	866287636
	DD	1073211159
	DD	2019624647
	DD	1073211689
	DD	3943909011
	DD	1073212219
	DD	2344434403
	DD	1073212750
	DD	1516429178
	DD	1073213281
	DD	1460154487
	DD	1073213812
	DD	2175871566
	DD	1073214343
	DD	3663841740
	DD	1073214874
	DD	1629359127
	DD	1073215406
	DD	367652526
	DD	1073215938
	DD	4173950822
	DD	1073216469
	DD	163613810
	DD	1073217002
	DD	1221805143
	DD	1073217534
	DD	3053819383
	DD	1073218066
	DD	1364951179
	DD	1073218599
	DD	450429860
	DD	1073219132
	DD	310517550
	DD	1073219665
	DD	945476461
	DD	1073220198
	DD	2355568891
	DD	1073220731
	DD	246089934
	DD	1073221265
	DD	3207236660
	DD	1073221798
	DD	2649337043
	DD	1073222332
	DD	2867621035
	DD	1073222866
	DD	3862351381
	DD	1073223400
	DD	1338823616
	DD	1073223935
	DD	3887235257
	DD	1073224469
	DD	2917914723
	DD	1073225004
	DD	2726092409
	DD	1073225539
	DD	3312031505
	DD	1073226074
	DD	381027993
	DD	1073226610
	DD	2523279834
	DD	1073227145
	DD	1149115891
	DD	1073227681
	DD	553767008
	DD	1073228217
	DD	737496817
	DD	1073228753
	DD	1700569046
	DD	1073229289
	DD	3443247507
	DD	1073229825
	DD	1670828807
	DD	1073230362
	DD	678544234
	DD	1073230899
	DD	466657870
	DD	1073231436
	DD	1035433887
	DD	1073231973
	DD	2385136545
	DD	1073232510
	DD	221062897
	DD	1073233048
	DD	3133411975
	DD	1073233585
	DD	2532513716
	DD	1073234123
	DD	2713600035
	DD	1073234661
	DD	3676935639
	DD	1073235199
	DD	1127818031
	DD	1073235738
	DD	3656446689
	DD	1073236276
	DD	2673151999
	DD	1073236815
	DD	2473166324
	DD	1073237354
	DD	3056754818
	DD	1073237893
	DD	129215434
	DD	1073238433
	DD	2280748099
	DD	1073238972
	DD	921683645
	DD	1073239512
	DD	347254887
	DD	1073240052
	DD	557727428
	DD	1073240592
	DD	1553366965
	DD	1073241132
	DD	3334439281
	DD	1073241672
	DD	1606242957
	DD	1073242213
	DD	664011255
	DD	1073242754
	DD	508010229
	DD	1073243295
	DD	1138506026
	DD	1073243836
	DD	2555764879
	DD	1073244377
	DD	465085820
	DD	1073244919
	DD	3456669854
	DD	1073245460
	DD	2940848899
	DD	1073246002
	DD	3212856843
	DD	1073246544
	DD	4272960376
	DD	1073247086
	DD	1826458977
	DD	1073247629
	DD	168586808
	DD	1073248172
	DD	3594578124
	DD	1073248714
	DD	3514765380
	DD	1073249257
	DD	4224383010
	DD	1073249800
	DD	1428730948
	DD	1073250344
	DD	3718011103
	DD	1073250887
	DD	2502556294
	DD	1073251431
	DD	2077601315
	DD	1073251975
	DD	2443413759
	DD	1073252519
	DD	3600261305
	DD	1073253063
	DD	1253444431
	DD	1073253608
	DD	3993165589
	DD	1073254152
	DD	3229758142
	DD	1073254697
	DD	3258457428
	DD	1073255242
	DD	4079531583
	DD	1073255787
	DD	1398281537
	DD	1073256333
	DD	3804910195
	DD	1073256878
	DD	2709751374
	DD	1073257424
	DD	2408040869
	DD	1073257970
	DD	2900047266
	DD	1073258516
	DD	4186039245
	DD	1073259062
	DD	1971318283
	DD	1073259609
	DD	551120534
	DD	1073260156
	DD	4220682249
	DD	1073260702
	DD	95370583
	DD	1073261250
	DD	1060356558
	DD	1073261797
	DD	2820942105
	DD	1073262344
	DD	1082429243
	DD	1073262892
	DD	140054677
	DD	1073263440
	DD	4289055202
	DD	1073263987
	DD	644798521
	DD	1073264536
	DD	2092456205
	DD	1073265084
	DD	42363431
	DD	1073265633
	DD	3084724658
	DD	1073266181
	DD	2629875250
	DD	1073266730
	DD	2973052550
	DD	1073267279
	DD	4114526696
	DD	1073267828
	DD	1759600621
	DD	1073268378
	DD	203511943
	DD	1073268928
	DD	3741498372
	DD	1073269477
	DD	3783895818
	DD	1073270027
	DD	330974880
	DD	1073270578
	DD	1972940835
	DD	1073271128
	DD	120129872
	DD	1073271679
	DD	3362747453
	DD	1073272229
	DD	3111129949
	DD	1073272780
	DD	3660515711
	DD	1073273331
	DD	716208586
	DD	1073273883
	DD	2868414407
	DD	1073274434
	DD	1527469912
	DD	1073274986
	DD	988613815
	DD	1073275538
	DD	1252117633
	DD	1073276090
	DD	2318252969
	DD	1073276642
	DD	4187291521
	DD	1073277194
	DD	2564537783
	DD	1073277747
	DD	1745230932
	DD	1073278300
	DD	1729642940
	DD	1073278853
	DD	2518045872
	DD	1073279406
	DD	4110711887
	DD	1073279959
	DD	2212945938
	DD	1073280513
	DD	1119987663
	DD	1073281067
	DD	832109496
	DD	1073281621
	DD	1349583962
	DD	1073282175
	DD	2672683681
	DD	1073282729
	DD	506714065
	DD	1073283284
	DD	3441882511
	DD	1073283838
	DD	2888527322
	DD	1073284393
	DD	3141888782
	DD	1073284948
	DD	4202239970
	DD	1073285503
	DD	1774886764
	DD	1073286059
	DD	155069723
	DD	1073286615
	DD	3638029501
	DD	1073287170
	DD	3634104958
	DD	1073287726
	DD	143569636
	DD	1073288283
	DD	1756631760
	DD	1073288839
	DD	4178597763
	DD	1073289395
	DD	3114774171
	DD	1073289952
	DD	2860402192
	DD	1073290509
	DD	3415755831
	DD	1073291066
	DD	486141891
	DD	1073291624
	DD	2661769156
	DD	1073292181
	DD	1352977319
	DD	1073292739
	DD	855008051
	DD	1073293297
	DD	1168135824
	DD	1073293855
	DD	2292635199
	DD	1073294413
	DD	4228780835
	DD	1073294971
	DD	2681880180
	DD	1073295530
	DD	1947175377
	DD	1073296089
	DD	2024941357
	DD	1073296648
	DD	2915453149
	DD	1073297207
	DD	324018578
	DD	1073297767
	DD	2840847451
	DD	1073298326
	DD	1876280482
	DD	1073298886
	DD	1725560368
	DD	1073299446
	DD	2388962603
	DD	1073300006
	DD	3866762774
	DD	1073300566
	DD	1864269266
	DD	1073301127
	DD	676725147
	DD	1073301688
	DD	304406285
	DD	1073302249
	DD	747588638
	DD	1073302810
	DD	2006548263
	DD	1073303371
	DD	4081561306
	DD	1073303932
	DD	2677936710
	DD	1073304494
	DD	2090918109
	DD	1073305056
	DD	2320781926
	DD	1073305618
	DD	3367804686
	DD	1073306180
	DD	937295706
	DD	1073306743
	DD	3619466287
	DD	1073307305
	DD	2824658639
	DD	1073307868
	DD	2848116954
	DD	1073308431
	DD	3690118222
	DD	1073308994
	DD	1055972230
	DD	1073309558
	DD	3535890746
	DD	1073310121
	DD	2540216452
	DD	1073310685
	DD	2364194007
	DD	1073311249
	DD	3008100869
	DD	1073311813
	DD	177247295
	DD	1073312378
	DD	2461845526
	DD	1073312942
	DD	1272238707
	DD	1073313507
	DD	903671971
	DD	1073314072
	DD	1356423246
	DD	1073314637
	DD	2630770556
	DD	1073315202
	DD	432024719
	DD	1073315768
	DD	3350398541
	DD	1073316333
	DD	2796235733
	DD	1073316899
	DD	3064781991
	DD	1073317465
	DD	4156315810
	DD	1073318031
	DD	1776148481
	DD	1073318598
	DD	219525981
	DD	1073319165
	DD	3781694384
	DD	1073319731
	DD	3872997969
	DD	1073320298
	DD	493715700
	DD	1073320866
	DD	2234061230
	DD	1073321433
	DD	504379122
	DD	1073322001
	DD	3894883214
	DD	1073322568
	DD	3815918261
	DD	1073323136
	DD	267763697
	DD	1073323705
	DD	1840633648
	DD	1073324273
	DD	4239840446
	DD	1073324841
	DD	3170696514
	DD	1073325410
	DD	2928448968
	DD	1073325979
	DD	3513377716
	DD	1073326548
	DD	630795469
	DD	1073327118
	DD	2870916921
	DD	1073327687
	DD	1644087673
	DD	1073328257
	DD	1245555313
	DD	1073328827
	DD	1675600225
	DD	1073329397
	DD	2934502889
	DD	1073329967
	DD	727576585
	DD	1073330538
	DD	3645036575
	DD	1073331108
	DD	3097229033
	DD	1073331679
	DD	3379402112
	DD	1073332250
	DD	196869472
	DD	1073332822
	DD	2139846758
	DD	1073333393
	DD	618680520
	DD	1073333965
	DD	4223586593
	DD	1073334536
	DD	69944423
	DD	1073335109
	DD	1042937330
	DD	1073335681
	DD	2847879545
	DD	1073336253
	DD	1190085390
	DD	1073336826
	DD	364803882
	DD	1073337399
	DD	372316832
	DD	1073337972
	DD	1212906146
	DD	1073338545
	DD	2886853829
	DD	1073339118
	DD	1099474683
	DD	1073339692
	DD	146018196
	DD	1073340266
	DD	26766661
	DD	1073340840
	DD	742002461
	DD	1073341414
	DD	2292008077
	DD	1073341988
	DD	382098789
	DD	1073342563
	DD	3602491862
	DD	1073343137
	DD	3363535471
	DD	1073343712
	DD	3960479776
	DD	1073344287
	DD	1098640438
	DD	1073344863
	DD	3368235106
	DD	1073345438
	DD	2179612338
	DD	1073346014
	DD	1828022677
	DD	1073346590
	DD	2313749465
	DD	1073347166
	DD	3637076140
	DD	1073347742
	DD	1503318941
	DD	1073348319
	DD	207728794
	DD	1073348896
	DD	4045556722
	DD	1073349472
	DD	132184659
	DD	1073350050
	DD	1352798410
	DD	1073350627
	DD	3412714695
	DD	1073351204
	DD	2017250329
	DD	1073351782
	DD	1461656813
	DD	1073352360
	DD	1746218452
	DD	1073352938
	DD	2871219644
	DD	1073353516
	DD	541977589
	DD	1073354095
	DD	3348711472
	DD	1073354673
	DD	2701771387
	DD	1073355252
	DD	2896409416
	DD	1073355831
	DD	3932910439
	DD	1073356410
	DD	1516592138
	DD	1073356990
	DD	4237674180
	DD	1073357569
	DD	3506507142
	DD	1073358149
	DD	3618343587
	DD	1073358729
	DD	278501583
	DD	1073359310
	DD	2077201180
	DD	1073359890
	DD	424793346
	DD	1073360471
	DD	3911498325
	DD	1073361051
	DD	3947667274
	DD	1073361632
	DD	533586041
	DD	1073362214
	DD	2259475161
	DD	1073362795
	DD	535686082
	DD	1073363377
	DD	3952439535
	DD	1073363958
	DD	3920087160
	DD	1073364540
	DD	438915288
	DD	1073365123
	DD	2099144940
	DD	1073365705
	DD	311128049
	DD	1073366288
	DD	3665085827
	DD	1073366870
	DD	3571370402
	DD	1073367453
	DD	30268589
	DD	1073368037
	DD	1632001894
	DD	1073368620
	DD	4081890032
	DD	1073369203
	DD	3085252812
	DD	1073369787
	DD	2937344736
	DD	1073370371
	DD	3638453105
	DD	1073370955
	DD	893898021
	DD	1073371540
	DD	3293901575
	DD	1073372124
	DD	2248816765
	DD	1073372709
	DD	2053898580
	DD	1073373294
	DD	2709434808
	DD	1073373879
	DD	4215713335
	DD	1073374464
	DD	2278054847
	DD	1073375050
	DD	1191714722
	DD	1073375636
	DD	956981137
	DD	1073376222
	DD	1574142367
	DD	1073376808
	DD	3043486787
	DD	1073377394
	DD	1070335571
	DD	1073377981
	DD	4244911880
	DD	1073378567
	DD	3977569787
	DD	1073379154
	DD	268598057
	DD	1073379742
	DD	1708220144
	DD	1073380329
	DD	4001757711
	DD	1073380916
	DD	2854532518
	DD	1073381504
	DD	2561801018
	DD	1073382092
	DD	3123852464
	DD	1073382680
	DD	246008909
	DD	1073383269
	DD	2518494395
	DD	1073383857
	DD	1351663878
	DD	1073384446
	DD	1040774297
	DD	1073385035
	DD	1586115396
	DD	1073385624
	DD	2987977015
	DD	1073386213
	DD	951681799
	DD	1073386803
	DD	4067454374
	DD	1073387392
	DD	3745650286
	DD	1073387982
	DD	4281527065
	DD	1073388572
	DD	1380407745
	DD	1073389163
	DD	3632517350
	DD	1073389753
	DD	2448211814
	DD	1073390344
	DD	2122749062
	DD	1073390935
	DD	2656419818
	DD	1073391526
	DD	4049514907
	DD	1073392117
	DD	2007357954
	DD	1073392709
	DD	825207276
	DD	1073393301
	DD	503353991
	DD	1073393893
	DD	1042089317
	DD	1073394485
	DD	2441704572
	DD	1073395077
	DD	407523872
	DD	1073395670
	DD	3529773324
	DD	1073396262
	DD	3218809949
	DD	1073396855
	DD	3769892752
	DD	1073397448
	DD	888346247
	DD	1073398042
	DD	3164396935
	DD	1073398635
	DD	2008402230
	DD	1073399229
	DD	1715621535
	DD	1073399823
	DD	2286347056
	DD	1073400417
	DD	3720871095
	DD	1073401011
	DD	1724518759
	DD	1073401606
	DD	592549846
	DD	1073402201
	DD	325256956
	DD	1073402796
	DD	922932790
	DD	1073403391
	DD	2385870147
	DD	1073403986
	DD	419394629
	DD	1073404582
	DD	3613733823
	DD	1073405177
	DD	3379246235
	DD	1073405773
	DD	4011192357
	DD	1073406369
	DD	1214898188
	DD	1073406966
	DD	3580591712
	DD	1073407562
	DD	2518631832
	DD	1073408159
	DD	2324279437
	DD	1073408756
	DD	2997828218
	DD	1073409353
	DD	244604671
	DD	1073409951
	DD	2654837279
	DD	1073410548
	DD	1638885440
	DD	1073411146
	DD	1492010541
	DD	1073411744
	DD	2214506772
	DD	1073412342
	DD	3806668421
	DD	1073412940
	DD	1973822580
	DD	1073413539
	DD	1011231034
	DD	1073414138
	DD	919188372
	DD	1073414737
	DD	1697989282
	DD	1073415336
	DD	3347928550
	DD	1073415935
	DD	1574333768
	DD	1073416535
	DD	672467219
	DD	1073417135
	DD	642623992
	DD	1073417735
	DD	1485099270
	DD	1073418335
	DD	3200188344
	DD	1073418935
	DD	1493219302
	DD	1073419536
	DD	659454927
	DD	1073420137
	DD	699190808
	DD	1073420738
	DD	1612722630
	DD	1073421339
	DD	3400346180
	DD	1073421940
	DD	1767390049
	DD	1073422542
	DD	1009117521
	DD	1073423144
	DD	1125824683
	DD	1073423746
	DD	2117807723
	DD	1073424348
	DD	3985362929
	DD	1073424950
	DD	2433819392
	DD	1073425553
	DD	1758440898
	DD	1073426156
	DD	1959524035
	DD	1073426759
	DD	3037365493
	DD	1073427362
	DD	697294765
	DD	1073427966
	DD	3529543335
	DD	1073428569
	DD	2944473600
	DD	1073429173
	DD	3237349947
	DD	1073429777
	DD	113502272
	DD	1073430382
	DD	2163162461
	DD	1073430986
	DD	796693315
	DD	1073431591
	DD	309359621
	DD	1073432196
	DD	701458976
	DD	1073432801
	DD	1973289073
	DD	1073433406
	DD	4125147710
	DD	1073434011
	DD	2862365488
	DD	1073434617
	DD	2480207697
	DD	1073435223
	DD	2978972441
	DD	1073435829
	DD	63990619
	DD	1073436436
	DD	2325495125
	DD	1073437042
	DD	1173849767
	DD	1073437649
	DD	904320345
	DD	1073438256
	DD	1517205462
	DD	1073438863
	DD	3012803822
	DD	1073439470
	DD	1096446934
	DD	1073440078
	DD	63401002
	DD	1073440686
	DD	4208932328
	DD	1073441293
	DD	648438135
	DD	1073441902
	DD	2267119521
	DD	1073442510
	DD	475341206
	DD	1073443119
	DD	3863337193
	DD	1073443727
	DD	3841472405
	DD	1073444336
	DD	410046459
	DD	1073444946
	DD	2159293663
	DD	1073445555
	DD	499579244
	DD	1073446165
	DD	4021137714
	DD	1073446774
	DD	4134334502
	DD	1073447384
	DD	839469732
	DD	1073447995
	DD	2726778220
	DD	1073448605
	DD	1206625701
	DD	1073449216
	DD	574279898
	DD	1073449827
	DD	830041342
	DD	1073450438
	DD	1974210664
	DD	1073451049
	DD	4007088598
	DD	1073451660
	DD	2634008681
	DD	1073452272
	DD	2150239147
	DD	1073452884
	DD	2556081034
	DD	1073453496
	DD	3851835485
	DD	1073454108
	DD	1742836444
	DD	1073454721
	DD	524352553
	DD	1073455334
	DD	196685258
	DD	1073455947
	DD	760136107
	DD	1073456560
	DD	2215006752
	DD	1073457173
	DD	266631649
	DD	1073457787
	DD	3505247244
	DD	1073458400
	DD	3341220903
	DD	1073459014
	DD	4069821983
	DD	1073459628
	DD	1396385347
	DD	1073460243
	DD	3911147852
	DD	1073460857
	DD	3024477272
	DD	1073461472
	DD	3031643371
	DD	1073462087
	DD	3932948720
	DD	1073462702
	DD	1433728699
	DD	1073463318
	DD	4124220672
	DD	1073463933
	DD	3414792927
	DD	1073464549
	DD	3600715740
	DD	1073465165
	DD	387324901
	DD	1073465782
	DD	2364858184
	DD	1073466398
	DD	943684289
	DD	1073467015
	DD	419073901
	DD	1073467632
	DD	791330515
	DD	1073468249
	DD	2060757728
	DD	1073468866
	DD	4227659239
	DD	1073469483
	DD	2997371554
	DD	1073470101
	DD	2665165874
	DD	1073470719
	DD	3231346208
	DD	1073471337
	DD	401249370
	DD	1073471956
	DD	2765114166
	DD	1073472574
	DD	1733310322
	DD	1073473193
	DD	1601109552
	DD	1073473812
	DD	2368816380
	DD	1073474431
	DD	4036735431
	DD	1073475050
	DD	2310204140
	DD	1073475670
	DD	1484494633
	DD	1073476290
	DD	1559911846
	DD	1073476910
	DD	2536760819
	DD	1073477530
	DD	120379396
	DD	1073478151
	DD	2901007417
	DD	1073478771
	DD	2289015635
	DD	1073479392
	DD	2579676800
	DD	1073480013
	DD	3773296467
	DD	1073480634
	DD	1575212999
	DD	1073481256
	DD	280699453
	DD	1073481878
	DD	4185028992
	DD	1073482499
	DD	403605697
	DD	1073483122
	DD	1821637528
	DD	1073483744
	DD	4144463366
	DD	1073484366
	DD	3077422196
	DD	1073484989
	DD	2915787696
	DD	1073485612
	DD	3659866354
	DD	1073486235
	DD	1014997465
	DD	1073486859
	DD	3571422318
	DD	1073487482
	DD	2739513116
	DD	1073488106
	DD	2814544062
	DD	1073488730
	DD	3796822160
	DD	1073489354
	DD	1391687223
	DD	1073489979
	DD	4189381061
	DD	1073490603
	DD	3600276399
	DD	1073491228
	DD	3919647954
	DD	1073491853
	DD	852835958
	DD	1073492479
	DD	2990082633
	DD	1073493104
	DD	1741761122
	DD	1073493730
	DD	1403146560
	DD	1073494356
	DD	1974546891
	DD	1073494982
	DD	3456270160
	DD	1073495608
	DD	1553657225
	DD	1073496235
	DD	561983637
	DD	1073496862
	DD	481557757
	DD	1073497489
	DD	1312688048
	DD	1073498116
	DD	3055683081
	DD	1073498743
	DD	1415884232
	DD	1073499371
	DD	688567575
	DD	1073499999
	DD	874041994
	DD	1073500627
	DD	1972616476
	DD	1073501255
	DD	3984600112
	DD	1073501883
	DD	2615334802
	DD	1073502512
	DD	2160097144
	DD	1073503141
	DD	2619196543
	DD	1073503770
	DD	3992942510
	DD	1073504399
	DD	1986677364
	DD	1073505029
	DD	895678121
	DD	1073505659
	DD	720254605
	DD	1073506289
	DD	1460716748
	DD	1073506919
	DD	3117374583
	DD	1073507549
	DD	1395570953
	DD	1073508180
	DD	590583400
	DD	1073508811
	DD	702722273
	DD	1073509442
	DD	1732298027
	DD	1073510073
	DD	3679621221
	DD	1073510704
	DD	2250035226
	DD	1073511336
	DD	1738818105
	DD	1073511968
	DD	2146280735
	DD	1073512600
	DD	3472734097
	DD	1073513232
	DD	1423521980
	DD	1073513865
	DD	293922871
	DD	1073514498
	DD	84248066
	DD	1073515131
	DD	794808969
	DD	1073515764
	DD	2425917084
	DD	1073516397
	DD	682916729
	DD	1073517031
	DD	4156054215
	DD	1073517664
	DD	4255706773
	DD	1073518298
	DD	982186331
	DD	1073518933
	DD	2925739518
	DD	1073519567
	DD	1496743882
	DD	1073520202
	DD	990478963
	DD	1073520837
	DD	1407257115
	DD	1073521472
	DD	2747390795
	DD	1073522107
	DD	716225270
	DD	1073522743
	DD	3904007800
	DD	1073523378
	DD	3721116570
	DD	1073524014
	DD	167864460
	DD	1073524651
	DD	1834499047
	DD	1073525287
	DD	131398835
	DD	1073525924
	DD	3648811612
	DD	1073526560
	DD	3797116091
	DD	1073527197
	DD	576625684
	DD	1073527835
	DD	2577588500
	DD	1073528472
	DD	1210383568
	DD	1073529110
	DD	770291914
	DD	1073529748
	DD	1257627374
	DD	1073530386
	DD	2672703888
	DD	1073531024
	DD	720868210
	DD	1073531663
	DD	3992369085
	DD	1073532301
	DD	3897586180
	DD	1073532940
	DD	436833864
	DD	1073533580
	DD	2200361202
	DD	1073534219
	DD	598548181
	DD	1073534859
	DD	4221644080
	DD	1073535498
	DD	185061802
	DD	1073536139
	DD	1374018136
	DD	1073536779
	DD	3493860791
	DD	1073537419
	DD	2249937585
	DD	1073538060
	DD	1937531030
	DD	1073538701
	DD	2556956456
	DD	1073539342
	DD	4108529292
	DD	1073539983
	DD	2297597784
	DD	1073540625
	DD	1419444873
	DD	1073541267
	DD	1474386312
	DD	1073541909
	DD	2462737961
	DD	1073542551
	DD	89848491
	DD	1073543194
	DD	2945968568
	DD	1073543836
	DD	2441479782
	DD	1073544479
	DD	2871665714
	DD	1073545122
	DD	4236842761
	DD	1073545765
	DD	2242360129
	DD	1073546409
	DD	1183501723
	DD	1073547053
	DD	1060584258
	DD	1073547697
	DD	1873924558
	DD	1073548341
	DD	3623839555
	DD	1073548985
	DD	2015678991
	DD	1073549630
	DD	1344727307
	DD	1073550275
	DD	1611301755
	DD	1073550920
	DD	2815719695
	DD	1073551565
	DD	663331300
	DD	1073552211
	DD	3744388737
	DD	1073552856
	DD	3469275096
	DD	1073553502
	DD	4133275462
	DD	1073554148
	DD	1441740438
	DD	1073554795
	DD	3984922621
	DD	1073555441
	DD	3173205530
	DD	1073556088
	DD	3301874686
	DD	1073556735
	DD	76281118
	DD	1073557383
	DD	2086677854
	DD	1073558030
	DD	743448846
	DD	1073558678
	DD	341880045
	DD	1073559326
	DD	882290206
	DD	1073559974
	DD	2364998199
	DD	1073560622
	DD	495355702
	DD	1073561271
	DD	3863616392
	DD	1073561919
	DD	3880164866
	DD	1073562568
	DD	545320427
	DD	1073563218
	DD	2449337072
	DD	1073563867
	DD	1002599724
	DD	1073564517
	DD	500395305
	DD	1073565167
	DD	943043546
	DD	1073565817
	DD	2330864287
	DD	1073566467
	DD	369210180
	DD	1073567118
	DD	3648335875
	DD	1073567768
	DD	3578626945
	DD	1073568419
	DD	160403663
	DD	1073569071
	DD	1983921003
	DD	1073569722
	DD	459564863
	DD	1073570374
	DD	4177590436
	DD	1073571025
	DD	253416538
	DD	1073571678
	DD	1572265876
	DD	1073572330
	DD	3839492077
	DD	1073572982
	DD	2760448878
	DD	1073573635
	DD	2630424719
	DD	1073574288
	DD	3449740848
	DD	1073574941
	DD	923751329
	DD	1073575595
	DD	3642712223
	DD	1073576248
	DD	3017010515
	DD	1073576902
	DD	3341935185
	DD	1073577556
	DD	322840734
	DD	1073578211
	DD	2549983656
	DD	1073578865
	DD	1433751372
	DD	1073579520
	DD	1269433299
	DD	1073580175
	DD	2057351669
	DD	1073580830
	DD	3797828821
	DD	1073581485
	DD	2196219907
	DD	1073582141
	DD	1547814783
	DD	1073582797
	DD	1852936114
	DD	1073583453
	DD	3111906678
	DD	1073584109
	DD	1030082064
	DD	1073584766
	DD	4197719859
	DD	1073585422
	DD	4025208575
	DD	1073586079
	DD	512871426
	DD	1073586737
	DD	2250966326
	DD	1073587394
	DD	649882117
	DD	1073588052
	DD	4909637
	DD	1073588710
	DD	316372536
	DD	1073589368
	DD	1584594577
	DD	1073590026
	DD	3809899630
	DD	1073590684
	DD	2697644378
	DD	1073591343
	DD	2543120208
	DD	1073592002
	DD	3346651320
	DD	1073592661
	DD	813594727
	DD	1073593321
	DD	3534209441
	DD	1073593980
	DD	2918885397
	DD	1073594640
	DD	3262914532
	DD	1073595300
	DD	271654298
	DD	1073595961
	DD	2535364145
	DD	1073596621
	DD	1464434451
	DD	1073597282
	DD	1354157591
	DD	1073597943
	DD	2204858751
	DD	1073598604
	DD	4016863232
	DD	1073599265
	DD	2495529148
	DD	1073599927
	DD	1936149311
	DD	1073600589
	DD	2339049353
	DD	1073601251
	DD	3704555011
	DD	1073601913
	DD	1738024841
	DD	1073602576
	DD	734752097
	DD	1073603239
	DD	695062851
	DD	1073603902
	DD	1619283284
	DD	1073604565
	DD	3507739686
	DD	1073605228
	DD	2065791163
	DD	1073605892
	DD	1588731523
	DD	1073606556
	DD	2076887390
	DD	1073607220
	DD	3530585494
	DD	1073607884
	DD	1655185387
	DD	1073608549
	DD	745981317
	DD	1073609214
	DD	803300349
	DD	1073609879
	DD	1827469659
	DD	1073610544
	DD	3818816536
	DD	1073611209
	DD	2482701077
	DD	1073611875
	DD	2114418090
	DD	1073612541
	DD	2714295193
	DD	1073613207
	DD	4282660114
	DD	1073613873
	DD	2524873401
	DD	1073614540
	DD	1736230300
	DD	1073615207
	DD	1917058875
	DD	1073615874
	DD	3067687299
	DD	1073616541
	DD	893476561
	DD	1073617209
	DD	3984689650
	DD	1073617876
	DD	3751720481
	DD	1073618544
	DD	194897673
	DD	1073619213
	DD	1904484547
	DD	1073619881
	DD	290875353
	DD	1073620550
	DD	3944333634
	DD	1073621218
	DD	4275253862
	DD	1073621887
	DD	1283965214
	DD	1073622557
	DD	3560731566
	DD	1073623226
	DD	2515947727
	DD	1073623896
	DD	2444910500
	DD	1073624566
	DD	3347949506
	DD	1073625236
	DD	930427183
	DD	1073625907
	DD	3782607966
	DD	1073626577
	DD	3314887221
	DD	1073627248
	DD	3822562310
	DD	1073627919
	DD	1010996116
	DD	1073628591
	DD	3470453524
	DD	1073629262
	DD	2611330342
	DD	1073629934
	DD	2728924383
	DD	1073630606
	DD	3823566274
	DD	1073631278
	DD	1600619456
	DD	1073631951
	DD	355382076
	DD	1073632624
	DD	88185097
	DD	1073633297
	DD	799359593
	DD	1073633970
	DD	2489236751
	DD	1073634643
	DD	863180574
	DD	1073635317
	DD	216489768
	DD	1073635991
	DD	549495856
	DD	1073636665
	DD	1862530475
	DD	1073637339
	DD	4155925370
	DD	1073638013
	DD	3135045107
	DD	1073638688
	DD	3095188953
	DD	1073639363
	DD	4036688994
	DD	1073640038
	DD	1664910130
	DD	1073640714
	DD	275151965
	DD	1073641390
	DD	4162714219
	DD	1073642065
	DD	443027538
	DD	1073642742
	DD	2001326456
	DD	1073643418
	DD	248009142
	DD	1073644095
	DD	3773343060
	DD	1073644771
	DD	3987726602
	DD	1073645448
	DD	891492865
	DD	1073646126
	DD	3074909654
	DD	1073646803
	DD	1948375699
	DD	1073647481
	DD	1807191730
	DD	1073648159
	DD	2651691300
	DD	1073648837
	DD	187240772
	DD	1073649516
	DD	3004108512
	DD	1073650194
	DD	2512693819
	DD	1073650873
	DD	3008297986
	DD	1073651552
	DD	196287834
	DD	1073652232
	DD	2666932180
	DD	1073652911
	DD	1830630773
	DD	1073653591
	DD	1982685362
	DD	1073654271
	DD	3123430513
	DD	1073654951
	DD	958233609
	DD	1073655632
	DD	4077364038
	DD	1073656312
	DD	3891222112
	DD	1073656993
	DD	400142851
	DD	1073657675
	DD	2194395979
	DD	1073658356
	DD	684382154
	DD	1073659038
	DD	165404028
	DD	1073659720
	DD	637797077
	DD	1073660402
	DD	2101896888
	DD	1073661084
	DD	263071865
	DD	1073661767
	DD	3711592415
	DD	1073662449
	DD	3857859873
	DD	1073663132
	DD	702210283
	DD	1073663816
	DD	2834914390
	DD	1073664499
	DD	1666373871
	DD	1073665183
	DD	1491892407
	DD	1073665867
	DD	2311806496
	DD	1073666551
	DD	4126452746
	DD	1073667235
	DD	2641200587
	DD	1073667920
	DD	2151354154
	DD	1073668605
	DD	2657250399
	DD	1073669290
	DD	4159226391
	DD	1073669975
	DD	2362652014
	DD	1073670661
	DD	1562831858
	DD	1073671347
	DD	1760103333
	DD	1073672033
	DD	2954803963
	DD	1073672719
	DD	852304090
	DD	1073673406
	DD	4042876058
	DD	1073674092
	DD	3936923140
	DD	1073674779
	DD	534783318
	DD	1073675467
	DD	2426729279
	DD	1073676154
	DD	1023164641
	DD	1073676842
	DD	619395024
	DD	1073677530
	DD	1215758867
	DD	1073678218
	DD	2812594723
	DD	1073678906
	DD	1115273962
	DD	1073679595
	DD	419102666
	DD	1073680284
	DD	724419729
	DD	1073680973
	DD	2031564164
	DD	1073681662
	DD	45907802
	DD	1073682352
	DD	3357724476
	DD	1073683041
	DD	3377418951
	DD	1073683731
	DD	105330697
	DD	1073684422
	DD	2131733893
	DD	1073685112
	DD	867033647
	DD	1073685803
	DD	606537073
	DD	1073686494
	DD	1350584099
	DD	1073687185
	DD	3099514774
	DD	1073687876
	DD	1558701962
	DD	1073688568
	DD	1023453234
	DD	1073689260
	DD	1494108984
	DD	1073689952
	DD	2971009717
	DD	1073690644
	DD	1159528760
	DD	1073691337
	DD	354974146
	DD	1073692030
	DD	557686728
	DD	1073692723
	DD	1768007473
	DD	1073693416
	DD	3986277466
	DD	1073694109
	DD	2917870609
	DD	1073694803
	DD	2858095512
	DD	1073695497
	DD	3807293606
	DD	1073696191
	DD	1470839139
	DD	1073696886
	DD	144041070
	DD	1073697581
	DD	4122208472
	DD	1073698275
	DD	520781347
	DD	1073698971
	DD	2225003594
	DD	1073699666
	DD	645282744
	DD	1073700362
	DD	76928333
	DD	1073701058
	DD	520282716
	DD	1073701754
	DD	1975688363
	DD	1073702450
	DD	148520570
	DD	1073703147
	DD	3629056629
	DD	1073703843
	DD	3827704769
	DD	1073704540
	DD	744807923
	DD	1073705238
	DD	2970643739
	DD	1073705935
	DD	1915620789
	DD	1073706633
	DD	1875049654
	DD	1073707331
	DD	2849273735
	DD	1073708029
	DD	543669252
	DD	1073708728
	DD	3548514428
	DD	1073709426
	DD	3274218421
	DD	1073710125
	DD	4016092394
	DD	1073710824
	DD	1479513031
	DD	1073711524
	DD	4254759022
	DD	1073712223
	DD	3752239991
	DD	1073712923
	DD	4267267563
	DD	1073713623
	DD	1505218891
	DD	1073714324
	DD	4056373132
	DD	1073715024
	DD	3331140374
	DD	1073715725
	DD	3624832712
	DD	1073716426
	DD	642827762
	DD	1073717128
	DD	2975405148
	DD	1073717829
	DD	2032975426
	DD	1073718531
	DD	2110851158
	DD	1073719233
	DD	3209377725
	DD	1073719935
	DD	1033933329
	DD	1073720638
	DD	4174798177
	DD	1073721340
	DD	4042383409
	DD	1073722043
	DD	637034877
	DD	1073722747
	DD	2549033138
	DD	1073723450
	DD	1188789684
	DD	1073724154
	DD	851618011
	DD	1073724858
	DD	1537864438
	DD	1073725562
	DD	3247875400
	DD	1073726266
	DD	1687030154
	DD	1073726971
	DD	1150642665
	DD	1073727676
	DD	1639059720
	DD	1073728381
	DD	3152628225
	DD	1073729086
	DD	1396727905
	DD	1073729792
	DD	666673196
	DD	1073730498
	DD	962811354
	DD	1073731204
	DD	2285489754
	DD	1073731910
	DD	340088594
	DD	1073732617
	DD	3716890072
	DD	1073733323
	DD	3826307327
	DD	1073734030
	DD	668688203
	DD	1073734738
	DD	2834315254
	DD	1073735445
	DD	1733601969
	DD	1073736153
	DD	1661863844
	DD	1073736861
	DD	2619449193
	DD	1073737569
	DD	311739156
	DD	1073738278
	DD	3329016876
	DD	1073738986
	DD	3081696432
	DD	1073739695
	DD	3865093909
	DD	1073740404
	DD	1384590916
	DD	1073741114
	DD	4230471070
	DD	1073741823
	DB 0
	ORG $+54
	DB	0
	DD	1697350398
	DD	1084691783
	DD	1697350398
	DD	1084691783
	DD	1697350398
	DD	1084691783
	DD	1697350398
	DD	1084691783
	DD	1697350398
	DD	1084691783
	DD	1697350398
	DD	1084691783
	DD	1697350398
	DD	1084691783
	DD	1697350398
	DD	1084691783
	DD	0
	DD	1127743488
	DD	0
	DD	1127743488
	DD	0
	DD	1127743488
	DD	0
	DD	1127743488
	DD	0
	DD	1127743488
	DD	0
	DD	1127743488
	DD	0
	DD	1127743488
	DD	0
	DD	1127743488
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
	DD	67444641
	DD	1060515641
	DD	67444641
	DD	1060515641
	DD	67444641
	DD	1060515641
	DD	67444641
	DD	1060515641
	DD	67444641
	DD	1060515641
	DD	67444641
	DD	1060515641
	DD	67444641
	DD	1060515641
	DD	67444641
	DD	1060515641
	DD	2047
	DD	0
	DD	2047
	DD	0
	DD	2047
	DD	0
	DD	2047
	DD	0
	DD	2047
	DD	0
	DD	2047
	DD	0
	DD	2047
	DD	0
	DD	2047
	DD	0
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
	DD	1079639606
	DD	1079639606
	DD	1079639606
	DD	1079639606
	DD	1079639606
	DD	1079639606
	DD	1079639606
	DD	1079639606
	DD	1079639606
	DD	1079639606
	DD	1079639606
	DD	1079639606
	DD	1079639606
	DD	1079639606
	DD	1079639606
	DD	1079639606
_vmlsPowHATab	DD	1065353216
	DD	1063518208
	DD	1061945344
	DD	1060765696
	DD	1059717120
	DD	1058930688
	DD	1058144256
	DD	1057488896
	DD	1056964608
	DD	0
	DD	0
	DD	1043013120
	DD	912347133
	DD	1051178752
	DD	920491638
	DD	1055650560
	DD	910207939
	DD	3201407744
	DD	3065009147
	DD	3197864192
	DD	3046757530
	DD	3192020992
	DD	3064938684
	DD	3182631936
	DD	3069048210
	DD	0
	DD	0
	DD	1065353216
	DD	1064828928
	DD	1064304640
	DD	1066008576
	DD	1065877504
	DD	1065746432
	DD	1065615360
	DD	1065484288
	DD	1065353216
	DD	0
	DD	0
	DD	1027315712
	DD	915720665
	DD	1035907072
	DD	882039287
	DD	3185459200
	DD	3062137179
	DD	3182631936
	DD	3069048210
	DD	3179763712
	DD	3059908234
	DD	3174420480
	DD	3067311503
	DD	3166121984
	DD	3066774241
	DD	0
	DD	0
	DD	1069056000
	DD	1069006848
	DD	1068957696
	DD	1068908544
	DD	1068875776
	DD	1069318144
	DD	1069301760
	DD	1069268992
	DD	1069252608
	DD	1069219840
	DD	1069203456
	DD	1069170688
	DD	1069154304
	DD	1069121536
	DD	1069105152
	DD	1069072384
	DD	1069056000
	DD	0
	DD	0
	DD	1002471424
	DD	906080490
	DD	1010884608
	DD	916991201
	DD	1016135680
	DD	905081895
	DD	1018261504
	DD	918286540
	DD	3170725888
	DD	3067774196
	DD	3169697792
	DD	3068476631
	DD	3167637504
	DD	3069858259
	DD	3166609408
	DD	3020376912
	DD	3164540928
	DD	3039629966
	DD	3163504640
	DD	3043319364
	DD	3160350720
	DD	3046704089
	DD	3158269952
	DD	3047249178
	DD	3154083840
	DD	3046609959
	DD	3149905920
	DD	3045301896
	DD	3137339392
	DD	3034784887
	DD	0
	DD	0
	DD	1065353216
	DD	0
	DD	1065398766
	DD	3014665830
	DD	1065444562
	DD	2982428624
	DD	1065490608
	DD	3015478276
	DD	1065536902
	DD	865643564
	DD	1065583450
	DD	3014725705
	DD	1065630248
	DD	868461790
	DD	1065677302
	DD	858550072
	DD	1065724612
	DD	3013096376
	DD	1065772178
	DD	3013897664
	DD	1065820002
	DD	3008545510
	DD	1065868086
	DD	3011512679
	DD	1065916430
	DD	864064219
	DD	1065965038
	DD	819487640
	DD	1066013910
	DD	3012212369
	DD	1066063046
	DD	856316133
	DD	1066112450
	DD	2992679841
	DD	1066162122
	DD	3001970243
	DD	1066212064
	DD	3013902756
	DD	1066262276
	DD	847285146
	DD	1066312762
	DD	3010032741
	DD	1066363522
	DD	3018332471
	DD	1066414556
	DD	856041677
	DD	1066465868
	DD	864808677
	DD	1066517460
	DD	3012318446
	DD	1066569330
	DD	863709796
	DD	1066621484
	DD	3016813593
	DD	1066673920
	DD	3018872036
	DD	1066726640
	DD	3006136850
	DD	1066779646
	DD	864474828
	DD	1066832942
	DD	3016286184
	DD	1066886526
	DD	3015052933
	DD	1066940400
	DD	857938801
	DD	1066994568
	DD	2993474036
	DD	1067049030
	DD	3009003152
	DD	1067103786
	DD	872191232
	DD	1067158842
	DD	3003929955
	DD	1067214196
	DD	3013071165
	DD	1067269850
	DD	3006375425
	DD	1067325806
	DD	843377209
	DD	1067382066
	DD	859906882
	DD	1067438632
	DD	848662531
	DD	1067495506
	DD	3018868367
	DD	1067552686
	DD	868910405
	DD	1067610180
	DD	3019699127
	DD	1067667984
	DD	3013023741
	DD	1067726102
	DD	3005475891
	DD	1067784536
	DD	3010626242
	DD	1067843286
	DD	866758993
	DD	1067902356
	DD	869265128
	DD	1067961748
	DD	3004575030
	DD	1068021462
	DD	3018425550
	DD	1068081498
	DD	867494524
	DD	1068141862
	DD	858118433
	DD	1068202554
	DD	3004476802
	DD	1068263574
	DD	866434624
	DD	1068324926
	DD	870990497
	DD	1068386612
	DD	858100843
	DD	1068448632
	DD	867002634
	DD	1068510990
	DD	3000050815
	DD	1068573686
	DD	3011271336
	DD	1068636722
	DD	3006477262
	DD	1068700100
	DD	840255625
	DD	1068763822
	DD	866280780
	DD	1068827892
	DD	3016492578
	DD	1068892308
	DD	3006218836
	DD	1068957074
	DD	2993076596
	DD	1069022192
	DD	3000356208
	DD	1069087664
	DD	3015220484
	DD	1069153490
	DD	856315927
	DD	1069219674
	DD	867308350
	DD	1069286218
	DD	863888852
	DD	1069353124
	DD	3007401960
	DD	1069420392
	DD	832069785
	DD	1069488026
	DD	3004369690
	DD	1069556026
	DD	866250961
	DD	1069624396
	DD	868902513
	DD	1069693138
	DD	851736822
	DD	1069762252
	DD	869934231
	DD	1069831742
	DD	869028661
	DD	1069901610
	DD	839559223
	DD	1069971856
	DD	867543588
	DD	1070042484
	DD	868789178
	DD	1070113496
	DD	859381756
	DD	1070184894
	DD	3010667426
	DD	1070256678
	DD	859604257
	DD	1070328852
	DD	872346226
	DD	1070401420
	DD	3010682756
	DD	1070474380
	DD	841546788
	DD	1070547736
	DD	869210393
	DD	1070621492
	DD	2996061011
	DD	1070695648
	DD	3013455510
	DD	1070770206
	DD	3009158570
	DD	1070845168
	DD	865699227
	DD	1070920538
	DD	866897902
	DD	1070996318
	DD	2955948569
	DD	1071072508
	DD	868931229
	DD	1071149114
	DD	3014890061
	DD	1071226134
	DD	3002473793
	DD	1071303572
	DD	861820308
	DD	1071381432
	DD	3008383516
	DD	1071459714
	DD	3010850715
	DD	1071538420
	DD	864181775
	DD	1071617554
	DD	870234352
	DD	1071697118
	DD	871115413
	DD	1071777114
	DD	872414852
	DD	1071857546
	DD	3012378998
	DD	1071938412
	DD	866137918
	DD	1072019718
	DD	870808707
	DD	1072101466
	DD	866840096
	DD	1072183658
	DD	857766040
	DD	1072266296
	DD	855693471
	DD	1072349382
	DD	870833444
	DD	1072432920
	DD	867585053
	DD	1072516912
	DD	846646433
	DD	1072601360
	DD	3008357562
	DD	1072686266
	DD	3007858250
	DD	1072771632
	DD	866626825
	DD	1072857464
	DD	3015943680
	DD	1072943760
	DD	2995197552
	DD	1073030526
	DD	3018513273
	DD	1073117762
	DD	3012791488
	DD	1073205472
	DD	3012359471
	DD	1073293658
	DD	3003728983
	DD	1073382322
	DD	870019626
	DD	1073471470
	DD	3012762127
	DD	1073561100
	DD	835668076
	DD	1073651218
	DD	3013837936
	DD	980050793
	DD	3199320925
	DD	1042575209
	DD	3182108321
	DD	1060205080
	DD	1047920112
	DD	1029920839
	DD	2130706432
	DD	8388608
	DD	0
	DD	1065353216
	DD	3212836864
	DD	1203765248
	DD	1069056000
	DD	1166018560
	DD	1602224128
	DD	528482304
	DD	0
	DD	2147483648
_2il0floatpacket_40	DD	000000000H,043380000H,000000000H,043380000H
_2il0floatpacket_143	DD	0bf800000H
_2il0floatpacket_144	DD	05f800000H
_2il0floatpacket_145	DD	03fb88000H
_2il0floatpacket_146	DD	045800800H
_2il0floatpacket_147	DD	0bdab1ea1H
_2il0floatpacket_148	DD	03e246f69H
_2il0floatpacket_149	DD	0beb1c35dH
_2il0floatpacket_150	DD	03a6a6369H
_2il0floatpacket_151	DD	047c00000H
_2il0floatpacket_152	DD	03d635847H
_2il0floatpacket_153	DD	03e75fdf0H
_2il0floatpacket_154	DD	03f317218H
_2il0floatpacket_155	DD	000800000H
_2il0floatpacket_156	DD	07f000000H
_2il0floatpacket_157	DD	01f800000H
_2il0floatpacket_158	DD	03f800000H
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
