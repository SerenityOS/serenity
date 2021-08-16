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
	PUBLIC __svml_sinhf4_ha_l9

__svml_sinhf4_ha_l9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 296
        lea       r10, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [192+rsp], xmm15
        vmovups   XMMWORD PTR [208+rsp], xmm14
        vmovups   XMMWORD PTR [224+rsp], xmm13
        vmovaps   xmm13, xmm0
        vmovups   XMMWORD PTR [256+rsp], xmm12
        vmovups   XMMWORD PTR [240+rsp], xmm6
        mov       QWORD PTR [272+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vandps    xmm12, xmm13, XMMWORD PTR [__svml_ssinh_ha_data_internal+1152]
        and       r13, -64
        vmovups   xmm3, XMMWORD PTR [__svml_ssinh_ha_data_internal+1280]
        vxorps    xmm0, xmm12, xmm13
        vmovups   xmm2, XMMWORD PTR [__svml_ssinh_ha_data_internal+960]
        vfmadd213ps xmm2, xmm0, xmm3
        vmovups   xmm5, XMMWORD PTR [__svml_ssinh_ha_data_internal+1344]
        vpcmpgtd  xmm4, xmm0, XMMWORD PTR [__svml_ssinh_ha_data_internal+1408]
        vmovmskps ecx, xmm4
        vmovups   xmm1, XMMWORD PTR [__svml_ssinh_ha_data_internal+1024]
        vxorps    xmm6, xmm2, xmm3
        vsubps    xmm3, xmm2, xmm3
        vpsubd    xmm15, xmm5, xmm6
        vpsrld    xmm4, xmm15, 28
        vpslld    xmm14, xmm4, 4
        vpor      xmm15, xmm14, xmm6
        vpand     xmm5, xmm15, xmm5
        vfnmadd213ps xmm1, xmm3, xmm0
        vpslld    xmm2, xmm5, 4
        vmovd     edx, xmm2
        vfnmadd231ps xmm1, xmm3, XMMWORD PTR [__svml_ssinh_ha_data_internal+1088]
        vmovd     xmm3, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+r10+rdx]
        vpextrd   r8d, xmm2, 2
        vpextrd   eax, xmm2, 1
        vpextrd   r9d, xmm2, 3
        vmovd     xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+r10+r8]
        vpinsrd   xmm2, xmm3, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+r10+rax], 1
        vpinsrd   xmm4, xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+r10+r9], 1
        vmovd     xmm3, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+r10+rdx]
        vmovd     xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+r10+r8]
        vunpcklpd xmm14, xmm2, xmm4
        vpinsrd   xmm2, xmm3, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+r10+rax], 1
        vpinsrd   xmm4, xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+r10+r9], 1
        vmovups   xmm15, XMMWORD PTR [__svml_ssinh_ha_data_internal+5568]
        vpunpcklqdq xmm3, xmm2, xmm4
        vpcmpgtd  xmm2, xmm15, xmm5
        vfmadd213ps xmm3, xmm1, xmm14
        vaddps    xmm4, xmm14, xmm14
        vmovd     xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+r10+rdx]
        vpsubd    xmm5, xmm6, xmm5
        vpinsrd   xmm15, xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+r10+rax], 1
        vmovd     xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+r10+r8]
        vpinsrd   xmm14, xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+r10+r9], 1
        vpunpcklqdq xmm14, xmm15, xmm14
        vandps    xmm15, xmm0, xmm2
        vmovd     xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+r10+rdx]
        vandnps   xmm2, xmm2, xmm4
        vfmadd213ps xmm3, xmm1, xmm14
        vpinsrd   xmm14, xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+r10+rax], 1
        vmovd     xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+r10+r8]
        vpinsrd   xmm0, xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+r10+r9], 1
        vpunpcklqdq xmm14, xmm14, xmm0
        vfmadd213ps xmm3, xmm1, xmm14
        vorps     xmm1, xmm2, xmm15
        mov       QWORD PTR [280+rsp], r13
        vaddps    xmm0, xmm1, xmm3
        vpslld    xmm1, xmm5, 19
        vpaddd    xmm2, xmm0, xmm1
        vorps     xmm0, xmm12, xmm2
        test      ecx, ecx
        jne       _B1_3

_B1_2::

        vmovups   xmm6, XMMWORD PTR [240+rsp]
        vmovups   xmm12, XMMWORD PTR [256+rsp]
        vmovups   xmm13, XMMWORD PTR [224+rsp]
        vmovups   xmm14, XMMWORD PTR [208+rsp]
        vmovups   xmm15, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [272+rsp]
        add       rsp, 296
        ret

_B1_3::

        vmovups   XMMWORD PTR [r13], xmm13
        vmovups   XMMWORD PTR [64+r13], xmm0

_B1_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, ecx

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

        call      __svml_ssinh_ha_cout_rare_internal
        jmp       _B1_8
        ALIGN     16

_B1_11::

__svml_sinhf4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinhf4_ha_l9_B1_B3:
	DD	936705
	DD	2282571
	DD	1009731
	DD	1099834
	DD	972845
	DD	911396
	DD	849947
	DD	2425099

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_sinhf4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinhf4_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_sinhf4_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_6
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_sinhf4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinhf16_ha_z0

__svml_sinhf16_ha_z0	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L18::

        sub       rsp, 1336
        mov       QWORD PTR [1320+rsp], r13
        lea       r13, QWORD PTR [1183+rsp]
        vmovups   zmm26, ZMMWORD PTR [__svml_ssinh_ha_data_internal+1152]
        and       r13, -64
        vmovups   zmm25, ZMMWORD PTR [__svml_ssinh_ha_data_internal+960]
        vmovups   zmm5, ZMMWORD PTR [__svml_ssinh_ha_data_internal+576]
        vmovups   zmm1, ZMMWORD PTR [__svml_ssinh_ha_data_internal+1024]
        vmovups   zmm24, ZMMWORD PTR [__svml_ssinh_ha_data_internal+1088]
        vmovups   zmm28, ZMMWORD PTR [__svml_ssinh_ha_data_internal+896]
        vmovups   zmm29, ZMMWORD PTR [__svml_ssinh_ha_data_internal+832]
        vpternlogd zmm4, zmm4, zmm4, 255
        vmovaps   zmm31, zmm0
        vandps    zmm30, zmm26, zmm31
        vmovups   zmm0, ZMMWORD PTR [__svml_ssinh_ha_data_internal+512]
        vxorps    zmm27, zmm30, zmm31
        vfmadd213ps zmm25, zmm27, zmm0 {rn-sae}
        vpcmpd    k1, zmm27, ZMMWORD PTR [__svml_ssinh_ha_data_internal+704], 2
        vcmpps    k2, zmm5, zmm25, 0 {sae}
        vmovups   zmm5, ZMMWORD PTR [__svml_ssinh_ha_data_internal+256]
        vblendmps zmm22{k2}, zmm25, zmm0
        vsubps    zmm3, zmm22, zmm0 {rn-sae}
        vmovups   zmm0, ZMMWORD PTR [__svml_ssinh_ha_data_internal+128]
        mov       QWORD PTR [1328+rsp], r13
        vpandnd   zmm4{k1}, zmm27, zmm27
        vfnmadd231ps zmm27, zmm3, zmm1 {rn-sae}
        vptestmd  k0, zmm4, zmm4
        vmovups   zmm4, ZMMWORD PTR [__svml_ssinh_ha_data_internal]
        vfnmadd231ps zmm27, zmm3, zmm24 {rn-sae}
        kmovw     r8d, k0
        vmulps    zmm25, zmm27, zmm27 {rn-sae}
        vmulps    zmm28, zmm28, zmm25 {rn-sae}
        vfmadd213ps zmm28, zmm27, zmm27 {rn-sae}
        vmulps    zmm27, zmm29, zmm25 {rn-sae}
        vpandd    zmm23, zmm22, ZMMWORD PTR [__svml_ssinh_ha_data_internal+640]
        vpxord    zmm2, zmm22, zmm23
        vpslld    zmm24, zmm2, 18
        vmovups   zmm2, ZMMWORD PTR [__svml_ssinh_ha_data_internal+384]
        vpermt2ps zmm5, zmm23, ZMMWORD PTR [__svml_ssinh_ha_data_internal+320]
        vpermt2ps zmm4, zmm23, ZMMWORD PTR [__svml_ssinh_ha_data_internal+64]
        vpermt2ps zmm2, zmm23, ZMMWORD PTR [__svml_ssinh_ha_data_internal+448]
        vpsubd    zmm3, zmm5, zmm24
        vpaddd    zmm22, zmm4, zmm24
        vpermt2ps zmm0, zmm23, ZMMWORD PTR [__svml_ssinh_ha_data_internal+192]
        vandnps   zmm23, zmm26, zmm2
        vsubps    zmm5, zmm22, zmm3 {rn-sae}
        vpaddd    zmm1, zmm0, zmm24
        vpsubd    zmm0, zmm2, zmm24
        vsubps    zmm26, zmm5, zmm22 {rn-sae}
        vaddps    zmm4, zmm22, zmm3 {rn-sae}
        vcmpps    k3, zmm23, zmm24, 17 {sae}
        vaddps    zmm2, zmm3, zmm26 {rn-sae}
        vxorps    zmm0{k3}, zmm0, zmm0
        vsubps    zmm1, zmm1, zmm2 {rn-sae}
        vsubps    zmm0, zmm1, zmm0 {rn-sae}
        vfmadd213ps zmm27, zmm5, zmm0 {rn-sae}
        vfmadd213ps zmm4, zmm28, zmm27 {rn-sae}
        vaddps    zmm29, zmm4, zmm5 {rn-sae}
        vorps     zmm0, zmm30, zmm29
        test      r8d, r8d
        jne       _B2_3

_B2_2::

        mov       r13, QWORD PTR [1320+rsp]
        add       rsp, 1336
        ret

_B2_3::

        vstmxcsr  DWORD PTR [1312+rsp]

_B2_4::

        movzx     edx, WORD PTR [1312+rsp]
        mov       eax, edx
        or        eax, 8064
        cmp       edx, eax
        je        _B2_6

_B2_5::

        mov       DWORD PTR [1312+rsp], eax
        vldmxcsr  DWORD PTR [1312+rsp]

_B2_6::

        vmovups   ZMMWORD PTR [r13], zmm31
        vmovups   ZMMWORD PTR [64+r13], zmm0
        test      r8d, r8d
        jne       _B2_11

_B2_7::

        cmp       edx, eax
        je        _B2_2

_B2_8::

        vstmxcsr  DWORD PTR [1312+rsp]
        mov       eax, DWORD PTR [1312+rsp]

_B2_9::

        and       eax, -8065
        or        eax, edx
        mov       DWORD PTR [1312+rsp], eax
        vldmxcsr  DWORD PTR [1312+rsp]
        jmp       _B2_2

_B2_11::

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

_B2_12::

        bt        edi, ebx
        jc        _B2_15

_B2_13::

        inc       ebx
        cmp       ebx, 16
        jl        _B2_12

_B2_14::

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
        jmp       _B2_7

_B2_15::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_ssinh_ha_cout_rare_internal
        jmp       _B2_13
        ALIGN     16

_B2_16::

__svml_sinhf16_ha_z0 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinhf16_ha_z0_B1_B9:
	DD	267009
	DD	10867731
	DD	10944779

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_sinhf16_ha_z0_B1_B9

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinhf16_ha_z0_B11_B15:
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

	DD	imagerel _B2_11
	DD	imagerel _B2_16
	DD	imagerel _unwind___svml_sinhf16_ha_z0_B11_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinhf4_ha_e9

__svml_sinhf4_ha_e9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L69::

        sub       rsp, 296
        lea       r10, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [192+rsp], xmm15
        vmovups   XMMWORD PTR [208+rsp], xmm13
        vmovups   XMMWORD PTR [240+rsp], xmm12
        vmovaps   xmm12, xmm0
        vmovups   XMMWORD PTR [256+rsp], xmm11
        vmovups   XMMWORD PTR [224+rsp], xmm6
        mov       QWORD PTR [272+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vandps    xmm11, xmm12, XMMWORD PTR [__svml_ssinh_ha_data_internal+1152]
        and       r13, -64
        vxorps    xmm0, xmm11, xmm12
        vmulps    xmm6, xmm0, XMMWORD PTR [__svml_ssinh_ha_data_internal+960]
        vmovups   xmm3, XMMWORD PTR [__svml_ssinh_ha_data_internal+1280]
        vmovups   xmm5, XMMWORD PTR [__svml_ssinh_ha_data_internal+1344]
        vaddps    xmm4, xmm3, xmm6
        vxorps    xmm6, xmm4, xmm3
        vsubps    xmm3, xmm4, xmm3
        vpsubd    xmm15, xmm5, xmm6
        vpsrld    xmm13, xmm15, 28
        vmulps    xmm4, xmm3, XMMWORD PTR [__svml_ssinh_ha_data_internal+1024]
        vpslld    xmm1, xmm13, 4
        vmulps    xmm15, xmm3, XMMWORD PTR [__svml_ssinh_ha_data_internal+1088]
        vpcmpgtd  xmm2, xmm0, XMMWORD PTR [__svml_ssinh_ha_data_internal+1408]
        vmovmskps ecx, xmm2
        vpor      xmm2, xmm1, xmm6
        vpand     xmm5, xmm2, xmm5
        vsubps    xmm2, xmm0, xmm4
        vpslld    xmm3, xmm5, 4
        vsubps    xmm4, xmm2, xmm15
        vmovd     edx, xmm3
        vpextrd   r8d, xmm3, 2
        vpextrd   eax, xmm3, 1
        vpextrd   r9d, xmm3, 3
        vmovd     xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+r10+rdx]
        vmovd     xmm1, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+r10+r8]
        vpinsrd   xmm2, xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+r10+rax], 1
        vpinsrd   xmm15, xmm1, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+r10+r9], 1
        vmovd     xmm3, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+r10+rdx]
        vmovd     xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+r10+r8]
        vunpcklpd xmm1, xmm2, xmm15
        vpinsrd   xmm2, xmm3, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+r10+rax], 1
        vpinsrd   xmm15, xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+r10+r9], 1
        vaddps    xmm13, xmm1, xmm1
        vmovups   xmm3, XMMWORD PTR [__svml_ssinh_ha_data_internal+5568]
        vpcmpgtd  xmm3, xmm3, xmm5
        vpsubd    xmm5, xmm6, xmm5
        vpunpcklqdq xmm2, xmm2, xmm15
        vmulps    xmm15, xmm2, xmm4
        vmovd     xmm2, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+r10+rdx]
        vaddps    xmm1, xmm1, xmm15
        vpinsrd   xmm2, xmm2, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+r10+rax], 1
        vmovd     xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+r10+r8]
        vpinsrd   xmm15, xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+r10+r9], 1
        vpunpcklqdq xmm15, xmm2, xmm15
        vandps    xmm2, xmm0, xmm3
        vmulps    xmm0, xmm4, xmm1
        vandnps   xmm3, xmm3, xmm13
        vmovd     xmm1, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+r10+rdx]
        vorps     xmm2, xmm3, xmm2
        mov       QWORD PTR [280+rsp], r13
        vaddps    xmm0, xmm15, xmm0
        vpinsrd   xmm15, xmm1, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+r10+rax], 1
        vmovd     xmm1, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+r10+r8]
        vpinsrd   xmm1, xmm1, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+r10+r9], 1
        vmulps    xmm4, xmm4, xmm0
        vpunpcklqdq xmm15, xmm15, xmm1
        vaddps    xmm0, xmm15, xmm4
        vaddps    xmm1, xmm2, xmm0
        vpslld    xmm0, xmm5, 19
        vpaddd    xmm2, xmm1, xmm0
        vorps     xmm0, xmm11, xmm2
        test      ecx, ecx
        jne       _B3_3

_B3_2::

        vmovups   xmm6, XMMWORD PTR [224+rsp]
        vmovups   xmm11, XMMWORD PTR [256+rsp]
        vmovups   xmm12, XMMWORD PTR [240+rsp]
        vmovups   xmm13, XMMWORD PTR [208+rsp]
        vmovups   xmm15, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [272+rsp]
        add       rsp, 296
        ret

_B3_3::

        vmovups   XMMWORD PTR [r13], xmm12
        vmovups   XMMWORD PTR [64+r13], xmm0

_B3_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, ecx

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

        call      __svml_ssinh_ha_cout_rare_internal
        jmp       _B3_8
        ALIGN     16

_B3_11::

__svml_sinhf4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinhf4_ha_e9_B1_B3:
	DD	936705
	DD	2282571
	DD	944195
	DD	1095738
	DD	1034285
	DD	907300
	DD	849947
	DD	2425099

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_sinhf4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinhf4_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_sinhf4_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_6
	DD	imagerel _B3_11
	DD	imagerel _unwind___svml_sinhf4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinhf8_ha_e9

__svml_sinhf8_ha_e9	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L86::

        push      rsi
        push      rdi
        push      r12
        sub       rsp, 528
        lea       rdi, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [320+rsp], ymm15
        vmovups   YMMWORD PTR [384+rsp], ymm14
        vmovups   YMMWORD PTR [352+rsp], ymm13
        vpxor     xmm14, xmm14, xmm14
        vmovups   YMMWORD PTR [416+rsp], ymm12
        vmovups   YMMWORD PTR [448+rsp], ymm11
        vmovups   YMMWORD PTR [480+rsp], ymm10
        vmovups   YMMWORD PTR [288+rsp], ymm7
        vmovups   YMMWORD PTR [256+rsp], ymm6
        mov       QWORD PTR [512+rsp], r13
        lea       r13, QWORD PTR [159+rsp]
        vmovaps   ymm12, ymm0
        and       r13, -64
        vandps    ymm11, ymm12, YMMWORD PTR [__svml_ssinh_ha_data_internal+1152]
        vxorps    ymm1, ymm11, ymm12
        vmovups   ymm15, YMMWORD PTR [__svml_ssinh_ha_data_internal+1280]
        vmulps    ymm5, ymm1, YMMWORD PTR [__svml_ssinh_ha_data_internal+960]
        vmovups   xmm7, XMMWORD PTR [__svml_ssinh_ha_data_internal+1408]
        vmovups   xmm6, XMMWORD PTR [__svml_ssinh_ha_data_internal+1344]
        vmovups   xmm0, XMMWORD PTR [__svml_ssinh_ha_data_internal+5568]
        vaddps    ymm4, ymm15, ymm5
        mov       QWORD PTR [520+rsp], r13
        vpcmpgtd  xmm5, xmm1, xmm7
        vextractf128 xmm10, ymm1, 1
        vpcmpgtd  xmm2, xmm10, xmm7
        vxorps    ymm7, ymm4, ymm15
        vpackssdw xmm3, xmm5, xmm2
        vsubps    ymm15, ymm4, ymm15
        vpacksswb xmm13, xmm3, xmm14
        vpmovmskb esi, xmm13
        vmulps    ymm4, ymm15, YMMWORD PTR [__svml_ssinh_ha_data_internal+1024]
        vsubps    ymm4, ymm1, ymm4
        vpsubd    xmm5, xmm6, xmm7
        vextractf128 xmm10, ymm7, 1
        vpsrld    xmm2, xmm5, 28
        vpslld    xmm3, xmm2, 4
        vpor      xmm14, xmm3, xmm7
        vpsubd    xmm2, xmm6, xmm10
        vpand     xmm5, xmm14, xmm6
        vpsrld    xmm3, xmm2, 28
        vpslld    xmm13, xmm3, 4
        vpslld    xmm3, xmm5, 4
        vpor      xmm2, xmm13, xmm10
        vpand     xmm6, xmm2, xmm6
        vmulps    ymm2, ymm15, YMMWORD PTR [__svml_ssinh_ha_data_internal+1088]
        vpslld    xmm15, xmm6, 4
        vmovd     r11d, xmm3
        vmovd     ecx, xmm15
        vsubps    ymm4, ymm4, ymm2
        vmovd     xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rdi+r11]
        vpextrd   r9d, xmm3, 2
        vpextrd   r10d, xmm3, 1
        vpextrd   r8d, xmm3, 3
        vmovd     xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rdi+r9]
        vpinsrd   xmm2, xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rdi+r10], 1
        vpinsrd   xmm3, xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rdi+r8], 1
        vpextrd   edx, xmm15, 1
        vpextrd   eax, xmm15, 2
        vpextrd   r12d, xmm15, 3
        vmovd     xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rdi+rcx]
        vpunpcklqdq xmm3, xmm2, xmm3
        vpinsrd   xmm2, xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rdi+rdx], 1
        vmovd     xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rdi+rax]
        vpinsrd   xmm14, xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rdi+r12], 1
        vmovd     xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rdi+r9]
        vpunpcklqdq xmm13, xmm2, xmm14
        vinsertf128 ymm14, ymm3, xmm13, 1
        vmovd     xmm3, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rdi+r11]
        vpinsrd   xmm2, xmm3, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rdi+r10], 1
        vpinsrd   xmm3, xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rdi+r8], 1
        vmovd     xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rdi+rcx]
        vmovd     xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rdi+rax]
        vpunpcklqdq xmm2, xmm2, xmm3
        vpinsrd   xmm3, xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rdi+rdx], 1
        vpinsrd   xmm13, xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rdi+r12], 1
        vpunpcklqdq xmm15, xmm3, xmm13
        vinsertf128 ymm15, ymm2, xmm15, 1
        vpcmpgtd  xmm2, xmm0, xmm5
        vmulps    ymm15, ymm15, ymm4
        vpcmpgtd  xmm0, xmm0, xmm6
        vpsubd    xmm5, xmm7, xmm5
        vpsubd    xmm6, xmm10, xmm6
        vpslld    xmm7, xmm5, 19
        vpslld    xmm6, xmm6, 19
        vaddps    ymm13, ymm14, ymm15
        vmulps    ymm13, ymm4, ymm13
        vinsertf128 ymm3, ymm2, xmm0, 1
        vaddps    ymm2, ymm14, ymm14
        vmovd     xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rdi+r11]
        vmovd     xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rdi+r9]
        vpinsrd   xmm15, xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rdi+r10], 1
        vpinsrd   xmm14, xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rdi+r8], 1
        vmovd     xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rdi+rcx]
        vpunpcklqdq xmm14, xmm15, xmm14
        vpinsrd   xmm15, xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rdi+rdx], 1
        vmovd     xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rdi+rax]
        vpinsrd   xmm0, xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rdi+r12], 1
        vpunpcklqdq xmm15, xmm15, xmm0
        vandps    ymm1, ymm1, ymm3
        vandnps   ymm2, ymm3, ymm2
        vorps     ymm1, ymm2, ymm1
        vinsertf128 ymm14, ymm14, xmm15, 1
        vaddps    ymm15, ymm14, ymm13
        vmovd     xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rdi+r11]
        vmovd     xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rdi+r9]
        vpinsrd   xmm0, xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rdi+r10], 1
        vpinsrd   xmm14, xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rdi+r8], 1
        vmovd     xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rdi+rcx]
        vpinsrd   xmm13, xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rdi+rdx], 1
        vmulps    ymm4, ymm4, ymm15
        vpunpcklqdq xmm14, xmm0, xmm14
        vmovd     xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rdi+rax]
        vpinsrd   xmm0, xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rdi+r12], 1
        vpunpcklqdq xmm13, xmm13, xmm0
        vinsertf128 ymm14, ymm14, xmm13, 1
        vaddps    ymm3, ymm14, ymm4
        vaddps    ymm0, ymm1, ymm3
        vextractf128 xmm1, ymm0, 1
        vpaddd    xmm0, xmm0, xmm7
        vpaddd    xmm2, xmm1, xmm6
        vinsertf128 ymm3, ymm0, xmm2, 1
        vorps     ymm0, ymm11, ymm3
        test      sil, sil
        jne       _B4_3

_B4_2::

        vmovups   ymm6, YMMWORD PTR [256+rsp]
        vmovups   ymm7, YMMWORD PTR [288+rsp]
        vmovups   ymm10, YMMWORD PTR [480+rsp]
        vmovups   ymm11, YMMWORD PTR [448+rsp]
        vmovups   ymm12, YMMWORD PTR [416+rsp]
        vmovups   ymm13, YMMWORD PTR [352+rsp]
        vmovups   ymm14, YMMWORD PTR [384+rsp]
        vmovups   ymm15, YMMWORD PTR [320+rsp]
        mov       r13, QWORD PTR [512+rsp]
        add       rsp, 528
        pop       r12
        pop       rdi
        pop       rsi
        ret

_B4_3::

        vmovups   YMMWORD PTR [r13], ymm12
        vmovups   YMMWORD PTR [64+r13], ymm0
        test      esi, esi
        je        _B4_2

_B4_6::

        vmovups   YMMWORD PTR [64+rsp], ymm8
        xor       edi, edi
        vmovups   YMMWORD PTR [32+rsp], ymm9

_B4_7::

        bt        esi, edi
        jc        _B4_10

_B4_8::

        inc       edi
        cmp       edi, 8
        jl        _B4_7

_B4_9::

        vmovups   ymm8, YMMWORD PTR [64+rsp]
        vmovups   ymm9, YMMWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [64+r13]
        jmp       _B4_2

_B4_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rdi*4]
        lea       rdx, QWORD PTR [64+r13+rdi*4]

        call      __svml_ssinh_ha_cout_rare_internal
        jmp       _B4_8
        ALIGN     16

_B4_11::

__svml_sinhf8_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinhf8_ha_e9_B1_B3:
	DD	1534721
	DD	4248683
	DD	1075299
	DD	1210458
	DD	2009169
	DD	1882184
	DD	1755199
	DD	1497137
	DD	1632296
	DD	1374239
	DD	4325647
	DD	1879490568
	DD	24581

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_sinhf8_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinhf8_ha_e9_B6_B10:
	DD	265761
	DD	169998
	DD	296966
	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_sinhf8_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_6
	DD	imagerel _B4_11
	DD	imagerel _unwind___svml_sinhf8_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinhf8_ha_l9

__svml_sinhf8_ha_l9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L109::

        push      rbx
        push      rsi
        push      rbp
        sub       rsp, 528
        lea       rbp, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [352+rsp], ymm15
        vmovups   YMMWORD PTR [384+rsp], ymm14
        vmovups   YMMWORD PTR [416+rsp], ymm13
        vmovups   YMMWORD PTR [448+rsp], ymm12
        vmovups   YMMWORD PTR [480+rsp], ymm11
        vmovups   YMMWORD PTR [320+rsp], ymm9
        mov       QWORD PTR [512+rsp], r13
        lea       r13, QWORD PTR [223+rsp]
        vmovups   ymm3, YMMWORD PTR [__svml_ssinh_ha_data_internal+1280]
        and       r13, -64
        vmovups   ymm15, YMMWORD PTR [__svml_ssinh_ha_data_internal+960]
        vmovups   ymm5, YMMWORD PTR [__svml_ssinh_ha_data_internal+1344]
        vmovups   ymm1, YMMWORD PTR [__svml_ssinh_ha_data_internal+1024]
        vmovaps   ymm12, ymm0
        vandps    ymm11, ymm12, YMMWORD PTR [__svml_ssinh_ha_data_internal+1152]
        vxorps    ymm0, ymm11, ymm12
        vfmadd213ps ymm15, ymm0, ymm3
        vpcmpgtd  ymm9, ymm0, YMMWORD PTR [__svml_ssinh_ha_data_internal+1408]
        vmovmskps ebx, ymm9
        vxorps    ymm9, ymm15, ymm3
        vpsubd    ymm2, ymm5, ymm9
        vsubps    ymm3, ymm15, ymm3
        vpsrld    ymm4, ymm2, 28
        vfnmadd213ps ymm1, ymm3, ymm0
        vpslld    ymm2, ymm4, 4
        vfnmadd231ps ymm1, ymm3, YMMWORD PTR [__svml_ssinh_ha_data_internal+1088]
        vpor      ymm4, ymm2, ymm9
        vpand     ymm5, ymm4, ymm5
        vpslld    ymm15, ymm5, 4
        mov       QWORD PTR [520+rsp], r13
        vmovd     r10d, xmm15
        vextracti128 xmm4, ymm15, 1
        vmovd     xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rbp+r10]
        vpextrd   r8d, xmm15, 2
        vpextrd   r9d, xmm15, 1
        vpextrd   esi, xmm15, 3
        vmovd     xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rbp+r8]
        vpinsrd   xmm3, xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rbp+r9], 1
        vpinsrd   xmm15, xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rbp+rsi], 1
        vmovd     ecx, xmm4
        vpunpcklqdq xmm2, xmm3, xmm15
        vpextrd   eax, xmm4, 2
        vpextrd   edx, xmm4, 1
        vpextrd   r11d, xmm4, 3
        vmovd     xmm4, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rbp+rcx]
        vmovd     xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rbp+rax]
        vpinsrd   xmm13, xmm4, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rbp+rdx], 1
        vpinsrd   xmm3, xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+rbp+r11], 1
        vmovd     xmm4, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rbp+r8]
        vpunpcklqdq xmm15, xmm13, xmm3
        vpinsrd   xmm3, xmm4, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rbp+rsi], 1
        vinsertf128 ymm13, ymm2, xmm15, 1
        vmovd     xmm2, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rbp+r10]
        vpinsrd   xmm14, xmm2, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rbp+r9], 1
        vmovd     xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rbp+rcx]
        vmovd     xmm2, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rbp+rax]
        vpinsrd   xmm4, xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rbp+rdx], 1
        vpunpcklqdq xmm3, xmm14, xmm3
        vpinsrd   xmm14, xmm2, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+rbp+r11], 1
        vmovups   ymm2, YMMWORD PTR [__svml_ssinh_ha_data_internal+5568]
        vpunpcklqdq xmm15, xmm4, xmm14
        vaddps    ymm4, ymm13, ymm13
        vpcmpgtd  ymm2, ymm2, ymm5
        vpsubd    ymm5, ymm9, ymm5
        vinsertf128 ymm3, ymm3, xmm15, 1
        vfmadd213ps ymm3, ymm1, ymm13
        vmovd     xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rbp+r10]
        vmovd     xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rbp+r8]
        vpinsrd   xmm14, xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rbp+r9], 1
        vpinsrd   xmm13, xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rbp+rsi], 1
        vmovd     xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rbp+rcx]
        vpinsrd   xmm15, xmm15, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rbp+rdx], 1
        vpunpcklqdq xmm14, xmm14, xmm13
        vmovd     xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rbp+rax]
        vpinsrd   xmm13, xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+rbp+r11], 1
        vpunpcklqdq xmm15, xmm15, xmm13
        vinsertf128 ymm14, ymm14, xmm15, 1
        vandps    ymm15, ymm0, ymm2
        vmovd     xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rbp+r10]
        vfmadd213ps ymm3, ymm1, ymm14
        vmovd     xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rbp+r8]
        vpinsrd   xmm13, xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rbp+r9], 1
        vpinsrd   xmm0, xmm14, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rbp+rsi], 1
        vpunpcklqdq xmm14, xmm13, xmm0
        vmovd     xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rbp+rcx]
        vmovd     xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rbp+rax]
        vpinsrd   xmm13, xmm13, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rbp+rdx], 1
        vpinsrd   xmm0, xmm0, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+rbp+r11], 1
        vpunpcklqdq xmm13, xmm13, xmm0
        vandnps   ymm2, ymm2, ymm4
        vinsertf128 ymm14, ymm14, xmm13, 1
        vfmadd213ps ymm3, ymm1, ymm14
        vorps     ymm1, ymm2, ymm15
        vaddps    ymm0, ymm1, ymm3
        vpslld    ymm1, ymm5, 19
        vpaddd    ymm2, ymm0, ymm1
        vorps     ymm0, ymm11, ymm2
        test      ebx, ebx
        jne       _B5_3

_B5_2::

        vmovups   ymm9, YMMWORD PTR [320+rsp]
        vmovups   ymm11, YMMWORD PTR [480+rsp]
        vmovups   ymm12, YMMWORD PTR [448+rsp]
        vmovups   ymm13, YMMWORD PTR [416+rsp]
        vmovups   ymm14, YMMWORD PTR [384+rsp]
        vmovups   ymm15, YMMWORD PTR [352+rsp]
        mov       r13, QWORD PTR [512+rsp]
        add       rsp, 528
        pop       rbp
        pop       rsi
        pop       rbx
        ret

_B5_3::

        vmovups   YMMWORD PTR [r13], ymm12
        vmovups   YMMWORD PTR [64+r13], ymm0

_B5_6::

        vmovups   YMMWORD PTR [128+rsp], ymm6
        xor       ebp, ebp
        vmovups   YMMWORD PTR [96+rsp], ymm7
        vmovups   YMMWORD PTR [64+rsp], ymm8
        vmovups   YMMWORD PTR [32+rsp], ymm10

_B5_7::

        bt        ebx, ebp
        jc        _B5_10

_B5_8::

        inc       ebp
        cmp       ebp, 8
        jl        _B5_7

_B5_9::

        vmovups   ymm6, YMMWORD PTR [128+rsp]
        vmovups   ymm7, YMMWORD PTR [96+rsp]
        vmovups   ymm8, YMMWORD PTR [64+rsp]
        vmovups   ymm10, YMMWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [64+r13]
        jmp       _B5_2

_B5_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbp*4]
        lea       rdx, QWORD PTR [64+r13+rbp*4]

        call      __svml_ssinh_ha_cout_rare_internal
        jmp       _B5_8
        ALIGN     16

_B5_11::

__svml_sinhf8_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinhf8_ha_l9_B1_B3:
	DD	1266433
	DD	4248659
	DD	1349707
	DD	2013250
	DD	1886265
	DD	1759280
	DD	1632295
	DD	1505310
	DD	4325646
	DD	1611026439
	DD	12293

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_sinhf8_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinhf8_ha_l9_B6_B10:
	DD	531745
	DD	174109
	DD	296983
	DD	423953
	DD	550921
	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_sinhf8_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_6
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_sinhf8_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sinhf4_ha_ex

__svml_sinhf4_ha_ex	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L132::

        sub       rsp, 312
        lea       r10, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [256+rsp], xmm12
        movaps    xmm12, xmm0
        movups    XMMWORD PTR [240+rsp], xmm10
        movups    XMMWORD PTR [272+rsp], xmm9
        movups    XMMWORD PTR [192+rsp], xmm8
        movups    XMMWORD PTR [208+rsp], xmm7
        movups    XMMWORD PTR [224+rsp], xmm6
        mov       QWORD PTR [288+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movups    xmm0, XMMWORD PTR [__svml_ssinh_ha_data_internal+1152]
        and       r13, -64
        andps     xmm0, xmm12
        movaps    xmm10, xmm0
        movups    xmm5, XMMWORD PTR [__svml_ssinh_ha_data_internal+960]
        pxor      xmm10, xmm12
        mulps     xmm5, xmm10
        movaps    xmm2, xmm10
        movups    xmm4, XMMWORD PTR [__svml_ssinh_ha_data_internal+1280]
        movaps    xmm7, xmm10
        movdqu    xmm3, XMMWORD PTR [__svml_ssinh_ha_data_internal+1344]
        addps     xmm5, xmm4
        movaps    xmm8, xmm5
        movdqa    xmm9, xmm3
        pxor      xmm8, xmm4
        subps     xmm5, xmm4
        psubd     xmm9, xmm8
        psrld     xmm9, 28
        movups    xmm1, XMMWORD PTR [__svml_ssinh_ha_data_internal+1024]
        pslld     xmm9, 4
        mulps     xmm1, xmm5
        por       xmm9, xmm8
        pand      xmm9, xmm3
        subps     xmm7, xmm1
        movdqa    xmm4, xmm9
        psubd     xmm8, xmm9
        movups    xmm6, XMMWORD PTR [__svml_ssinh_ha_data_internal+1088]
        pslld     xmm4, 4
        pcmpgtd   xmm2, XMMWORD PTR [__svml_ssinh_ha_data_internal+1408]
        pslld     xmm8, 19
        mulps     xmm6, xmm5
        movmskps  ecx, xmm2
        movd      edx, xmm4
        pshufd    xmm2, xmm4, 1
        subps     xmm7, xmm6
        movd      eax, xmm2
        pshufd    xmm3, xmm4, 2
        pshufd    xmm1, xmm4, 3
        movd      r8d, xmm3
        movd      r9d, xmm1
        movd      xmm6, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+r10+rdx]
        movd      xmm5, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+r10+rax]
        movd      xmm3, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+r10+r8]
        movd      xmm2, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1480+r10+r9]
        movd      xmm1, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+r10+r9]
        punpckldq xmm6, xmm5
        punpckldq xmm3, xmm2
        movd      xmm5, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+r10+rdx]
        movd      xmm4, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+r10+rax]
        movd      xmm2, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1484+r10+r8]
        punpckldq xmm5, xmm4
        punpckldq xmm2, xmm1
        punpcklqdq xmm5, xmm2
        mulps     xmm5, xmm7
        unpcklpd  xmm6, xmm3
        movaps    xmm3, xmm6
        movd      xmm2, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+r10+rdx]
        addps     xmm3, xmm6
        addps     xmm6, xmm5
        mulps     xmm6, xmm7
        movd      xmm5, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+r10+rax]
        punpckldq xmm2, xmm5
        movd      xmm5, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+r10+r8]
        movd      xmm1, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1476+r10+r9]
        punpckldq xmm5, xmm1
        punpcklqdq xmm2, xmm5
        movd      xmm1, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+r10+rdx]
        addps     xmm2, xmm6
        mulps     xmm7, xmm2
        movd      xmm6, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+r10+rax]
        punpckldq xmm1, xmm6
        movd      xmm6, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+r10+r8]
        movd      xmm5, DWORD PTR [imagerel(__svml_ssinh_ha_data_internal)+1472+r10+r9]
        punpckldq xmm6, xmm5
        punpcklqdq xmm1, xmm6
        movdqu    xmm4, XMMWORD PTR [__svml_ssinh_ha_data_internal+5568]
        addps     xmm1, xmm7
        pcmpgtd   xmm4, xmm9
        andps     xmm10, xmm4
        andnps    xmm4, xmm3
        orps      xmm4, xmm10
        mov       QWORD PTR [296+rsp], r13
        addps     xmm4, xmm1
        paddd     xmm4, xmm8
        orps      xmm0, xmm4
        test      ecx, ecx
        jne       _B6_3

_B6_2::

        movups    xmm6, XMMWORD PTR [224+rsp]
        movups    xmm7, XMMWORD PTR [208+rsp]
        movups    xmm8, XMMWORD PTR [192+rsp]
        movups    xmm9, XMMWORD PTR [272+rsp]
        movups    xmm10, XMMWORD PTR [240+rsp]
        movups    xmm12, XMMWORD PTR [256+rsp]
        mov       r13, QWORD PTR [288+rsp]
        add       rsp, 312
        ret

_B6_3::

        movups    XMMWORD PTR [r13], xmm12
        movups    XMMWORD PTR [64+r13], xmm0

_B6_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, ecx

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

        call      __svml_ssinh_ha_cout_rare_internal
        jmp       _B6_8
        ALIGN     16

_B6_11::

__svml_sinhf4_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinhf4_ha_ex_B1_B3:
	DD	1069569
	DD	2413650
	DD	944202
	DD	882754
	DD	821306
	DD	1153073
	DD	1026088
	DD	1099803
	DD	2556171

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_sinhf4_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sinhf4_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_sinhf4_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_6
	DD	imagerel _B6_11
	DD	imagerel _unwind___svml_sinhf4_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_ssinh_ha_cout_rare_internal

__svml_ssinh_ha_cout_rare_internal	PROC

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L151::

        sub       rsp, 216
        mov       r9, rcx
        movups    XMMWORD PTR [80+rsp], xmm15
        xor       eax, eax
        movups    XMMWORD PTR [96+rsp], xmm14
        movups    XMMWORD PTR [112+rsp], xmm13
        movzx     r8d, WORD PTR [2+r9]
        and       r8d, 32640
        movss     xmm2, DWORD PTR [r9]
        movups    XMMWORD PTR [128+rsp], xmm12
        movups    XMMWORD PTR [144+rsp], xmm11
        movups    XMMWORD PTR [160+rsp], xmm10
        movups    XMMWORD PTR [176+rsp], xmm9
        movups    XMMWORD PTR [192+rsp], xmm8
        cmp       r8d, 32640
        je        _B7_17

_B7_2::

        cvtss2sd  xmm2, xmm2
        movsd     QWORD PTR [208+rsp], xmm2
        movzx     ecx, WORD PTR [214+rsp]
        and       ecx, 32752
        movsd     QWORD PTR [56+rsp], xmm2
        shr       ecx, 4
        and       BYTE PTR [63+rsp], 127
        test      ecx, ecx
        jle       _B7_16

_B7_3::

        cmp       ecx, 969
        jle       _B7_14

_B7_4::

        movsd     xmm0, QWORD PTR [56+rsp]
        movsd     xmm1, QWORD PTR [_vmldSinhHATab+1136]
        comisd    xmm1, xmm0
        jbe       _B7_13

_B7_5::

        movsd     xmm1, QWORD PTR [_vmldSinhHATab+1184]
        comisd    xmm1, xmm0
        jbe       _B7_9

_B7_6::

        comisd    xmm0, QWORD PTR [_vmldSinhHATab+1176]
        jb        _B7_8

_B7_7::

        movsd     xmm8, QWORD PTR [_vmldSinhHATab+1112]
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
        xorps     xmm10, XMMWORD PTR [_2il0floatpacket_103]
        add       r8d, 1022
        mulsd     xmm10, QWORD PTR [_vmldSinhHATab+1152]
        subsd     xmm0, xmm5
        movaps    xmm3, xmm0
        movaps    xmm1, xmm0
        movsd     xmm5, QWORD PTR [_vmldSinhHATab+1128]
        and       r8d, 2047
        shl       r8d, 4
        lea       ecx, DWORD PTR [r11+r11]
        or        r9d, r8d
        lea       r8, QWORD PTR [__ImageBase]
        neg       r10d
        lea       r11d, DWORD PTR [1+r11+r11]
        add       r10d, -4
        addsd     xmm3, xmm10
        movsd     QWORD PTR [64+rsp], xmm3
        and       r10d, 2047
        movsd     xmm9, QWORD PTR [64+rsp]
        mov       WORD PTR [78+rsp], r9w
        and       r9d, -32753
        shl       r10d, 4
        subsd     xmm1, xmm9
        movsd     QWORD PTR [32+rsp], xmm1
        or        r9d, r10d
        movsd     xmm4, QWORD PTR [64+rsp]
        movsd     xmm2, QWORD PTR [32+rsp]
        addsd     xmm4, xmm2
        movsd     QWORD PTR [40+rsp], xmm4
        movsd     xmm13, QWORD PTR [32+rsp]
        addsd     xmm10, xmm13
        movsd     xmm13, QWORD PTR [72+rsp]
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
        movsd     xmm4, QWORD PTR [64+rsp]
        mulsd     xmm5, xmm4
        movaps    xmm1, xmm4
        movsd     xmm2, QWORD PTR [40+rsp]
        movaps    xmm14, xmm4
        movsd     QWORD PTR [64+rsp], xmm5
        movsd     xmm3, QWORD PTR [64+rsp]
        mulsd     xmm14, xmm4
        subsd     xmm3, xmm4
        movsd     QWORD PTR [32+rsp], xmm3
        movsd     xmm11, QWORD PTR [64+rsp]
        movsd     xmm12, QWORD PTR [32+rsp]
        movsd     xmm0, QWORD PTR [_vmldSinhHATab+1064]
        subsd     xmm11, xmm12
        mulsd     xmm0, xmm14
        movsd     QWORD PTR [64+rsp], xmm11
        movsd     xmm9, QWORD PTR [64+rsp]
        movsd     xmm12, QWORD PTR [imagerel(_vmldSinhHATab)+r8+r11*8]
        subsd     xmm1, xmm9
        mulsd     xmm12, xmm13
        addsd     xmm0, QWORD PTR [_vmldSinhHATab+1048]
        movsd     QWORD PTR [32+rsp], xmm1
        movsd     xmm1, QWORD PTR [_vmldSinhHATab+1072]
        mulsd     xmm1, xmm14
        mulsd     xmm0, xmm14
        addsd     xmm1, QWORD PTR [_vmldSinhHATab+1056]
        mulsd     xmm0, xmm4
        mulsd     xmm1, xmm14
        movsd     xmm5, QWORD PTR [64+rsp]
        movsd     xmm3, QWORD PTR [32+rsp]
        mov       r11b, BYTE PTR [215+rsp]
        and       r11b, -128
        addsd     xmm1, QWORD PTR [_vmldSinhHATab+1040]
        mulsd     xmm1, xmm14
        movsd     xmm14, QWORD PTR [imagerel(_vmldSinhHATab)+r8+rcx*8]
        neg       ecx
        mulsd     xmm14, xmm13
        movaps    xmm8, xmm14
        lea       r10d, DWORD PTR [128+rcx]
        movsd     xmm11, QWORD PTR [imagerel(_vmldSinhHATab)+r8+r10*8]
        add       ecx, 129
        mulsd     xmm11, xmm10
        movsd     xmm15, QWORD PTR [imagerel(_vmldSinhHATab)+r9+rcx*8]
        subsd     xmm8, xmm11
        mulsd     xmm15, xmm10
        movsd     QWORD PTR [64+rsp], xmm8
        movaps    xmm8, xmm14
        movsd     xmm9, QWORD PTR [64+rsp]
        subsd     xmm8, xmm9
        subsd     xmm8, xmm11
        movsd     QWORD PTR [32+rsp], xmm8
        movsd     xmm9, QWORD PTR [64+rsp]
        movsd     xmm8, QWORD PTR [32+rsp]
        movaps    xmm13, xmm9
        subsd     xmm8, xmm15
        addsd     xmm8, xmm12
        addsd     xmm12, xmm15
        addsd     xmm13, xmm8
        movsd     QWORD PTR [64+rsp], xmm13
        movaps    xmm13, xmm14
        movsd     xmm10, QWORD PTR [64+rsp]
        addsd     xmm13, xmm11
        subsd     xmm9, xmm10
        addsd     xmm9, xmm8
        movsd     QWORD PTR [32+rsp], xmm9
        movsd     xmm8, QWORD PTR [64+rsp]
        movsd     xmm9, QWORD PTR [32+rsp]
        movsd     QWORD PTR [64+rsp], xmm13
        movsd     xmm10, QWORD PTR [64+rsp]
        subsd     xmm14, xmm10
        addsd     xmm14, xmm11
        movsd     QWORD PTR [32+rsp], xmm14
        movsd     xmm14, QWORD PTR [64+rsp]
        movsd     xmm11, QWORD PTR [32+rsp]
        addsd     xmm11, xmm12
        movaps    xmm12, xmm14
        addsd     xmm12, xmm11
        movsd     QWORD PTR [64+rsp], xmm12
        movsd     xmm15, QWORD PTR [64+rsp]
        movsd     xmm12, QWORD PTR [_vmldSinhHATab+1128]
        subsd     xmm14, xmm15
        addsd     xmm14, xmm11
        movsd     QWORD PTR [32+rsp], xmm14
        movsd     xmm14, QWORD PTR [64+rsp]
        mulsd     xmm12, xmm14
        movsd     xmm13, QWORD PTR [32+rsp]
        movsd     QWORD PTR [64+rsp], xmm12
        movsd     xmm10, QWORD PTR [64+rsp]
        mulsd     xmm4, xmm13
        subsd     xmm10, xmm14
        movsd     QWORD PTR [32+rsp], xmm10
        movaps    xmm10, xmm2
        movsd     xmm11, QWORD PTR [64+rsp]
        movsd     xmm15, QWORD PTR [32+rsp]
        mulsd     xmm10, xmm13
        subsd     xmm11, xmm15
        mulsd     xmm2, xmm14
        movaps    xmm15, xmm0
        mulsd     xmm15, xmm13
        movsd     QWORD PTR [64+rsp], xmm11
        movaps    xmm11, xmm14
        mulsd     xmm14, xmm0
        addsd     xmm10, xmm15
        movaps    xmm15, xmm1
        mulsd     xmm15, xmm9
        mulsd     xmm1, xmm8
        addsd     xmm10, xmm15
        movsd     xmm12, QWORD PTR [64+rsp]
        addsd     xmm10, xmm4
        subsd     xmm11, xmm12
        addsd     xmm10, xmm2
        movaps    xmm2, xmm14
        movaps    xmm4, xmm3
        movsd     QWORD PTR [32+rsp], xmm11
        addsd     xmm2, xmm1
        addsd     xmm9, xmm10
        movsd     xmm12, QWORD PTR [64+rsp]
        movsd     xmm11, QWORD PTR [32+rsp]
        movsd     QWORD PTR [64+rsp], xmm2
        movaps    xmm2, xmm8
        movsd     xmm0, QWORD PTR [64+rsp]
        mulsd     xmm4, xmm11
        subsd     xmm14, xmm0
        mulsd     xmm11, xmm5
        mulsd     xmm3, xmm12
        addsd     xmm14, xmm1
        mulsd     xmm5, xmm12
        movsd     QWORD PTR [32+rsp], xmm14
        movsd     xmm0, QWORD PTR [64+rsp]
        movsd     xmm1, QWORD PTR [32+rsp]
        addsd     xmm1, xmm9
        addsd     xmm1, xmm4
        addsd     xmm1, xmm11
        addsd     xmm1, xmm3
        movaps    xmm3, xmm0
        addsd     xmm3, xmm5
        movsd     QWORD PTR [64+rsp], xmm3
        movsd     xmm4, QWORD PTR [64+rsp]
        subsd     xmm5, xmm4
        addsd     xmm5, xmm0
        movsd     QWORD PTR [32+rsp], xmm5
        movsd     xmm9, QWORD PTR [64+rsp]
        movsd     xmm10, QWORD PTR [32+rsp]
        addsd     xmm2, xmm9
        addsd     xmm10, xmm1
        movsd     QWORD PTR [64+rsp], xmm2
        movsd     xmm5, QWORD PTR [64+rsp]
        subsd     xmm8, xmm5
        addsd     xmm8, xmm9
        movsd     QWORD PTR [32+rsp], xmm8
        movsd     xmm8, QWORD PTR [64+rsp]
        movsd     xmm11, QWORD PTR [32+rsp]
        addsd     xmm11, xmm10
        addsd     xmm11, xmm8
        movsd     QWORD PTR [56+rsp], xmm11
        mov       cl, BYTE PTR [63+rsp]
        and       cl, 127
        or        cl, r11b
        mov       BYTE PTR [63+rsp], cl
        movsd     xmm12, QWORD PTR [56+rsp]
        cvtsd2ss  xmm12, xmm12
        movss     DWORD PTR [rdx], xmm12
        jmp       _B7_15

_B7_8::

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
        movsd     xmm0, QWORD PTR [56+rsp]
        cvtsd2ss  xmm0, xmm0
        movss     DWORD PTR [rdx], xmm0
        jmp       _B7_15

_B7_9::

        movsd     xmm1, QWORD PTR [_vmldSinhHATab+1112]
        lea       r9, QWORD PTR [__ImageBase]
        mulsd     xmm1, xmm0
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
        mov       r8, r9
        addsd     xmm1, QWORD PTR [imagerel(_vmldSinhHATab)+r8+r11*8]
        addsd     xmm1, xmm0
        cmp       ecx, 2046
        ja        _B7_11

_B7_10::

        mov       r8, QWORD PTR [_vmldSinhHATab+8]
        shr       r8, 48
        shl       ecx, 4
        and       r8d, -32753
        or        r8d, ecx
        mov       WORD PTR [78+rsp], r8w
        movsd     xmm0, QWORD PTR [72+rsp]
        mulsd     xmm0, xmm1
        movsd     QWORD PTR [56+rsp], xmm0
        jmp       _B7_12

_B7_11::

        dec       ecx
        and       ecx, 2047
        movzx     r8d, WORD PTR [78+rsp]
        shl       ecx, 4
        and       r8d, -32753
        or        r8d, ecx
        mov       WORD PTR [78+rsp], r8w
        movsd     xmm0, QWORD PTR [72+rsp]
        mulsd     xmm0, xmm1
        mulsd     xmm0, QWORD PTR [_vmldSinhHATab+1024]
        movsd     QWORD PTR [56+rsp], xmm0

_B7_12::

        mov       r8b, BYTE PTR [63+rsp]
        mov       cl, BYTE PTR [215+rsp]
        and       r8b, 127
        and       cl, -128
        or        r8b, cl
        mov       BYTE PTR [63+rsp], r8b
        movsd     xmm0, QWORD PTR [56+rsp]
        cvtsd2ss  xmm0, xmm0
        movss     DWORD PTR [rdx], xmm0
        jmp       _B7_15

_B7_13::

        movsd     xmm0, QWORD PTR [_vmldSinhHATab+1168]
        mov       eax, 3
        mulsd     xmm0, xmm2
        cvtsd2ss  xmm0, xmm0
        movss     DWORD PTR [rdx], xmm0
        jmp       _B7_15

_B7_14::

        movsd     xmm0, QWORD PTR [_vmldSinhHATab]
        addsd     xmm0, QWORD PTR [_vmldSinhHATab+1160]
        mulsd     xmm0, xmm2
        cvtsd2ss  xmm0, xmm0
        movss     DWORD PTR [rdx], xmm0

_B7_15::

        movups    xmm8, XMMWORD PTR [192+rsp]
        movups    xmm9, XMMWORD PTR [176+rsp]
        movups    xmm10, XMMWORD PTR [160+rsp]
        movups    xmm11, XMMWORD PTR [144+rsp]
        movups    xmm12, XMMWORD PTR [128+rsp]
        movups    xmm13, XMMWORD PTR [112+rsp]
        movups    xmm14, XMMWORD PTR [96+rsp]
        movups    xmm15, XMMWORD PTR [80+rsp]
        add       rsp, 216
        ret

_B7_16::

        movsd     xmm0, QWORD PTR [_vmldSinhHATab+1160]
        mulsd     xmm2, xmm0
        movsd     QWORD PTR [64+rsp], xmm2
        pxor      xmm2, xmm2
        cvtss2sd  xmm2, DWORD PTR [r9]
        movsd     xmm1, QWORD PTR [64+rsp]
        mov       rcx, QWORD PTR [_vmldSinhHATab+8]
        addsd     xmm2, xmm1
        cvtsd2ss  xmm2, xmm2
        mov       QWORD PTR [72+rsp], rcx
        movss     DWORD PTR [rdx], xmm2
        jmp       _B7_15

_B7_17::

        addss     xmm2, xmm2
        movss     DWORD PTR [rdx], xmm2
        jmp       _B7_15
        ALIGN     16

_B7_18::

__svml_ssinh_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_ssinh_ha_cout_rare_internal_B1_B17:
	DD	1204225
	DD	821344
	DD	759895
	DD	698446
	DD	636997
	DD	575548
	DD	514082
	DD	452636
	DD	391188
	DD	1769739

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_18
	DD	imagerel _unwind___svml_ssinh_ha_cout_rare_internal_B1_B17

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_ssinh_ha_data_internal
__svml_ssinh_ha_data_internal	DD	1056964608
	DD	1057148295
	DD	1057336003
	DD	1057527823
	DD	1057723842
	DD	1057924154
	DD	1058128851
	DD	1058338032
	DD	1058551792
	DD	1058770234
	DD	1058993458
	DD	1059221571
	DD	1059454679
	DD	1059692891
	DD	1059936319
	DD	1060185078
	DD	1060439283
	DD	1060699055
	DD	1060964516
	DD	1061235789
	DD	1061513002
	DD	1061796286
	DD	1062085772
	DD	1062381598
	DD	1062683901
	DD	1062992824
	DD	1063308511
	DD	1063631111
	DD	1063960775
	DD	1064297658
	DD	1064641917
	DD	1064993715
	DD	0
	DD	2999887785
	DD	852465809
	DD	3003046475
	DD	2984291233
	DD	3001644133
	DD	854021668
	DD	2997748242
	DD	849550193
	DD	2995541347
	DD	851518274
	DD	809701978
	DD	2997656926
	DD	2996185864
	DD	2980965110
	DD	3002882728
	DD	844097402
	DD	848217591
	DD	2999013352
	DD	2992006718
	DD	831170615
	DD	3002278818
	DD	833158180
	DD	3000769962
	DD	2991891850
	DD	2999994908
	DD	2979965785
	DD	2982419430
	DD	2982221534
	DD	2999469642
	DD	833168438
	DD	2987538264
	DD	1056964608
	DD	1056605107
	DD	1056253309
	DD	1055909050
	DD	1055572167
	DD	1055242503
	DD	1054919903
	DD	1054604216
	DD	1054295293
	DD	1053992990
	DD	1053697164
	DD	1053407678
	DD	1053124394
	DD	1052847181
	DD	1052575908
	DD	1052310447
	DD	1052050675
	DD	1051796470
	DD	1051547711
	DD	1051304283
	DD	1051066071
	DD	1050832963
	DD	1050604850
	DD	1050381626
	DD	1050163184
	DD	1049949424
	DD	1049740243
	DD	1049535546
	DD	1049335234
	DD	1049139215
	DD	1048947395
	DD	1048759687
	DD	0
	DD	2979149656
	DD	824779830
	DD	2991081034
	DD	2973832926
	DD	2974030822
	DD	2971577177
	DD	2991606300
	DD	2983503242
	DD	2992381354
	DD	824769572
	DD	2993890210
	DD	822782007
	DD	2983618110
	DD	2990624744
	DD	839828983
	DD	835708794
	DD	2994494120
	DD	2972576502
	DD	2987797256
	DD	2989268318
	DD	801313370
	DD	843129666
	DD	2987152739
	DD	841161585
	DD	2989359634
	DD	845633060
	DD	2993255525
	DD	2975902625
	DD	2994657867
	DD	844077201
	DD	2991499177
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
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	1220542465
	DD	31
	DD	31
	DD	31
	DD	31
	DD	31
	DD	31
	DD	31
	DD	31
	DD	31
	DD	31
	DD	31
	DD	31
	DD	31
	DD	31
	DD	31
	DD	31
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
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1056964676
	DD	1042983605
	DD	1042983605
	DD	1042983605
	DD	1042983605
	DD	1042983605
	DD	1042983605
	DD	1042983605
	DD	1042983605
	DD	1042983605
	DD	1042983605
	DD	1042983605
	DD	1042983605
	DD	1042983605
	DD	1042983605
	DD	1042983605
	DD	1042983605
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
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	1060204544
	DD	939916788
	DD	939916788
	DD	939916788
	DD	939916788
	DD	939916788
	DD	939916788
	DD	939916788
	DD	939916788
	DD	939916788
	DD	939916788
	DD	939916788
	DD	939916788
	DD	939916788
	DD	939916788
	DD	939916788
	DD	939916788
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
	DD	1228931072
	DD	1228931072
	DD	1228931072
	DD	1228931072
	DD	1228931072
	DD	1228931072
	DD	1228931072
	DD	1228931072
	DD	1228931072
	DD	1228931072
	DD	1228931072
	DD	1228931072
	DD	1228931072
	DD	1228931072
	DD	1228931072
	DD	1228931072
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
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	1118922496
	DD	0
	DD	687887406
	DD	2915115070
	DD	1042983726
	DD	929258934
	DD	980813922
	DD	1018266026
	DD	1042992474
	DD	954428950
	DD	997598593
	DD	1026665631
	DD	1043023968
	DD	968883188
	DD	1007325380
	DD	1032156897
	DD	1043076692
	DD	979611573
	DD	1014406071
	DD	1035098005
	DD	1043150518
	DD	987662767
	DD	1019277074
	DD	1038061036
	DD	1043245588
	DD	994080931
	DD	1024140990
	DD	1040619474
	DD	1043361758
	DD	999883086
	DD	1027459341
	DD	1042131339
	DD	1043501068
	DD	1004844750
	DD	1031304911
	DD	1043662546
	DD	1043660792
	DD	1008932267
	DD	1033741849
	DD	1045216058
	DD	1043844218
	DD	1012931568
	DD	1036203181
	DD	1046794669
	DD	1044047389
	DD	1016426573
	DD	1038940459
	DD	1048401520
	DD	1044276784
	DD	1019375189
	DD	1041073109
	DD	1049307698
	DD	1044526186
	DD	1022871607
	DD	1042725668
	DD	1050143823
	DD	1044801107
	DD	1025188112
	DD	1044524705
	DD	1050998533
	DD	1045100274
	DD	1027560014
	DD	1046473595
	DD	1051873886
	DD	1045430084
	DD	1030282880
	DD	1048576000
	DD	1052770896
	DD	1045780079
	DD	1032591216
	DD	1049705931
	DD	1053691782
	DD	1046163095
	DD	1034344377
	DD	1050916716
	DD	1054637154
	DD	1046554568
	DD	1036314518
	DD	1052210623
	DD	1055610247
	DD	1046982038
	DD	1038516252
	DD	1053590081
	DD	1056612417
	DD	1047444104
	DD	1040576009
	DD	1055057680
	DD	1057305094
	DD	1047938203
	DD	1041931271
	DD	1056616175
	DD	1057838147
	DD	1048464140
	DD	1043425610
	DD	1057616551
	DD	1058387990
	DD	1048791818
	DD	1045067287
	DD	1058491172
	DD	1058956436
	DD	1049080411
	DD	1046864840
	DD	1059415895
	DD	1059545021
	DD	1049393183
	DD	1048701551
	DD	1060392458
	DD	1060153606
	DD	1049717660
	DD	1049769606
	DD	1061422692
	DD	1060784342
	DD	1050066322
	DD	1050929319
	DD	1062508534
	DD	1061437519
	DD	1050422447
	DD	1052185595
	DD	1063652019
	DD	1062114959
	DD	1050803760
	DD	1053543521
	DD	1064855295
	DD	1062817471
	DD	1051202252
	DD	1055008374
	DD	1065736918
	DD	1063547051
	DD	1051622601
	DD	3097084200
	DD	1074266112
	DD	1064305255
	DD	1052071435
	DD	3097592230
	DD	1074615279
	DD	1065092533
	DD	1052543428
	DD	3098127090
	DD	1074981832
	DD	1065632015
	DD	1053027915
	DD	3098657586
	DD	1075366458
	DD	1066057424
	DD	1053547140
	DD	3099216842
	DD	1075769880
	DD	1066499901
	DD	1054080955
	DD	3099820420
	DD	1076192855
	DD	1066960277
	DD	1054635449
	DD	3100431607
	DD	1076636176
	DD	1067439415
	DD	1055231108
	DD	3101072121
	DD	1077100676
	DD	1067938215
	DD	1055851490
	DD	3101734019
	DD	1077587227
	DD	1068457613
	DD	1056495628
	DD	3102420416
	DD	1078096742
	DD	1068998584
	DD	1057067604
	DD	3103151062
	DD	1078630177
	DD	1069562144
	DD	1057425150
	DD	3103842417
	DD	1079188534
	DD	1070149350
	DD	1057796175
	DD	3104239345
	DD	1079772860
	DD	1070761305
	DD	1058192335
	DD	3104632042
	DD	1080384254
	DD	1071399156
	DD	1058592040
	DD	3105065708
	DD	1081023861
	DD	1072064103
	DD	1059022895
	DD	3105522352
	DD	1081692883
	DD	1072757393
	DD	1059471212
	DD	3105980727
	DD	1082261504
	DD	1073480326
	DD	1059935747
	DD	3106458228
	DD	1082627342
	DD	1073988042
	DD	1060431367
	DD	3106985545
	DD	1083009859
	DD	1074381218
	DD	1060942660
	DD	3107497595
	DD	1083409773
	DD	1074791339
	DD	1061470753
	DD	3108033911
	DD	1083827834
	DD	1075219176
	DD	1062030223
	DD	3108625747
	DD	1084264827
	DD	1075665533
	DD	1062616535
	DD	3109213903
	DD	1084721573
	DD	1076131246
	DD	1063215716
	DD	3109826597
	DD	1085198928
	DD	1076617190
	DD	1063856328
	DD	3110492915
	DD	1085697789
	DD	1077124278
	DD	1064519640
	DD	3111153932
	DD	1086219092
	DD	1077653460
	DD	1065214942
	DD	3111866338
	DD	1086763816
	DD	1078205731
	DD	1065643458
	DD	3112375523
	DD	1087332983
	DD	1078782126
	DD	1066020158
	DD	3112765050
	DD	1087927661
	DD	1079383729
	DD	1066418599
	DD	3113168833
	DD	1088548967
	DD	1080011668
	DD	1066831834
	DD	3113609244
	DD	1089198066
	DD	1080667123
	DD	1067273229
	DD	3114032535
	DD	1089876179
	DD	1081351321
	DD	1067717011
	DD	3114484025
	DD	1090551808
	DD	1082065549
	DD	1068193280
	DD	3114970280
	DD	1090921814
	DD	1082470790
	DD	1068689694
	DD	3115467036
	DD	1091308322
	DD	1082859974
	DD	1069207685
	DD	3115974474
	DD	1091712058
	DD	1083266273
	DD	1069737995
	DD	3116537826
	DD	1092133779
	DD	1083690451
	DD	1070298768
	DD	3117085761
	DD	1092574277
	DD	1084133302
	DD	1070882802
	DD	3117709126
	DD	1093034378
	DD	1084595660
	DD	1071508439
	DD	3118314866
	DD	1093514947
	DD	1085078390
	DD	1072148994
	DD	3118933130
	DD	1094016886
	DD	1085582399
	DD	1072806866
	DD	3119628767
	DD	1094541136
	DD	1086108635
	DD	1073507255
	DD	3120312034
	DD	1095088682
	DD	1086658083
	DD	1073986932
	DD	3120816642
	DD	1095660551
	DD	1087231777
	DD	1074370169
	DD	3121187932
	DD	1096257817
	DD	1087830791
	DD	1074769178
	DD	3121594488
	DD	1096881601
	DD	1088456252
	DD	1075185795
	DD	3122020198
	DD	1097533074
	DD	1089109333
	DD	1075619595
	DD	3122451537
	DD	1098213459
	DD	1089791259
	DD	1076069917
	DD	3122905402
	DD	1098915840
	DD	1090503311
	DD	1076549119
	DD	3123389748
	DD	1099286888
	DD	1090882933
	DD	1077045731
	DD	3123878864
	DD	1099674394
	DD	1091271119
	DD	1077560283
	DD	3124401536
	DD	1100079085
	DD	1091676463
	DD	1078101378
	DD	3124930682
	DD	1100501721
	DD	1092099725
	DD	1078662472
	DD	3125516800
	DD	1100943095
	DD	1092541701
	DD	1079251056
	DD	3126075229
	DD	1101404036
	DD	1093003218
	DD	1079857728
	DD	3126728388
	DD	1101885408
	DD	1093485146
	DD	1080508502
	DD	3127359219
	DD	1102388116
	DD	1093988386
	DD	1081175245
	DD	3128014352
	DD	1102913103
	DD	1094513884
	DD	1081871787
	DD	3128747686
	DD	1103461354
	DD	1095062628
	DD	1082369373
	DD	3129206088
	DD	1104033899
	DD	1095635645
	DD	1082749126
	DD	3129593301
	DD	1104631812
	DD	1096234013
	DD	1083148279
	DD	3130008743
	DD	1105256215
	DD	1096858855
	DD	1083570858
	DD	3130406199
	DD	1105908282
	DD	1097511341
	DD	1083997642
	DD	3130855937
	DD	1106589234
	DD	1098192700
	DD	1084459829
	DD	3131310395
	DD	1107298304
	DD	1098904208
	DD	1084929536
	DD	3131761492
	DD	1107669613
	DD	1099277424
	DD	1085415965
	DD	3132265084
	DD	1108057368
	DD	1099665361
	DD	1085939887
	DD	3132783371
	DD	1108462298
	DD	1100070466
	DD	1086478564
	DD	3133369511
	DD	1108885162
	DD	1100493501
	DD	1087055088
	DD	3133891436
	DD	1109326756
	DD	1100935256
	DD	1087624344
	DD	3134507369
	DD	1109787906
	DD	1101396565
	DD	1088246740
	DD	3135123225
	DD	1110269479
	DD	1101878291
	DD	1088894950
	DD	3135765391
	DD	1110772379
	DD	1102381339
	DD	1089569026
	DD	3136459557
	DD	1111297550
	DD	1102906654
	DD	1090269725
	DD	3137139863
	DD	1111845978
	DD	1103455220
	DD	1090756438
	DD	3137594905
	DD	1112418692
	DD	1104028068
	DD	1091135322
	DD	3137977906
	DD	1113016767
	DD	1104626274
	DD	1091531952
	DD	3138391473
	DD	1113641325
	DD	1105250961
	DD	1091953464
	DD	3138794156
	DD	1114293540
	DD	1105903299
	DD	1092383610
	DD	3139244396
	DD	1114974634
	DD	1106584516
	DD	1092846205
	DD	3139699003
	DD	1115685376
	DD	1107295888
	DD	1093316096
	DD	3140154077
	DD	1116056750
	DD	1107667503
	DD	1093805095
	DD	3140669482
	DD	1116444567
	DD	1108055378
	DD	1094336475
	DD	3141178479
	DD	1116849557
	DD	1108460423
	DD	1094869431
	DD	3141737901
	DD	1117272479
	DD	1108883400
	DD	1095429351
	DD	3142284745
	DD	1117714127
	DD	1109325101
	DD	1096014237
	DD	3142915054
	DD	1118175329
	DD	1109786358
	DD	1096645678
	DD	3143505197
	DD	1118656953
	DD	1110268033
	DD	1097277902
	DD	3144150196
	DD	1119159901
	DD	1110771033
	DD	1097953811
	DD	3144845928
	DD	1119685118
	DD	1111296302
	DD	1098655549
	DD	3145529363
	DD	1120233590
	DD	1111844824
	DD	1099144661
	DD	3145987662
	DD	1120806346
	DD	1112417630
	DD	1099525884
	DD	3146377804
	DD	1121404461
	DD	1113015796
	DD	1099927000
	DD	3146786805
	DD	1122029058
	DD	1113640444
	DD	1100345687
	DD	3147190794
	DD	1122681310
	DD	1114292745
	DD	1100776673
	DD	3147632967
	DD	1123362440
	DD	1114973926
	DD	1101234255
	DD	3148087611
	DD	1124073600
	DD	1115685064
	DD	1101704192
	DD	3148551873
	DD	1124444990
	DD	1116056479
	DD	1102198949
	DD	3149053844
	DD	1124832823
	DD	1116444338
	DD	1102721963
	DD	3149560519
	DD	1125237828
	DD	1116849368
	DD	1103253489
	DD	3150129648
	DD	1125660764
	DD	1117272331
	DD	1103819489
	DD	3150699108
	DD	1126102425
	DD	1117714019
	DD	1104418512
	DD	3151300238
	DD	1126563641
	DD	1118175262
	DD	1105031754
	DD	3151908533
	DD	1127045277
	DD	1118656925
	DD	1105675327
	DD	3152521467
	DD	1127548238
	DD	1119159912
	DD	1106331233
	DD	3153233976
	DD	1128073466
	DD	1119685170
	DD	1107043461
	DD	3153918194
	DD	1128621949
	DD	1120233681
	DD	1107533172
	DD	3154369114
	DD	1129194716
	DD	1120806476
	DD	1107909865
	DD	3154761041
	DD	1129792841
	DD	1121404632
	DD	1108312102
	DD	3155164804
	DD	1130417448
	DD	1122029270
	DD	1108727526
	DD	3155573216
	DD	1131069709
	DD	1122681562
	DD	1109161279
	DD	3156013372
	DD	1131750848
	DD	1123362734
	DD	1109617608
	DD	3156476219
	DD	1132462112
	DD	1124073768
	DD	1110092672
	DD	3156942778
	DD	1132833506
	DD	1124445179
	DD	1110588868
	DD	3157441390
	DD	1133221343
	DD	1124833034
	DD	1111109791
	DD	3157939291
	DD	1133626352
	DD	1125238060
	DD	1111635844
	DD	3158527234
	DD	1134049291
	DD	1125661020
	DD	1112213594
	DD	3159077768
	DD	1134490956
	DD	1126102704
	DD	1112800807
	DD	3159687990
	DD	1134952175
	DD	1126563944
	DD	1113419729
	DD	3160268049
	DD	1135433815
	DD	1127045603
	DD	1114045678
	DD	3160913934
	DD	1135936778
	DD	1127548588
	DD	1114722160
	DD	3161622444
	DD	1136462009
	DD	1128073843
	DD	1115431895
	DD	3162298664
	DD	1137010495
	DD	1128622351
	DD	1115919199
	DD	3162764127
	DD	1137583264
	DD	1129195144
	DD	1116302432
	DD	3163148306
	DD	1138181392
	DD	1129793297
	DD	1116699834
	DD	3163558953
	DD	1138806001
	DD	1130417933
	DD	1117119556
	DD	3163972568
	DD	1139458264
	DD	1131070223
	DD	1117556560
	DD	3164399930
	DD	1140139406
	DD	1131751392
	DD	1118004903
	DD	3164864827
	DD	1140850696
	DD	1132462400
	DD	1118481248
	DD	3165331960
	DD	1141222091
	DD	1132833810
	DD	1118977804
	DD	3165829733
	DD	1141609929
	DD	1133221664
	DD	1119498204
	DD	3166325440
	DD	1142014939
	DD	1133626689
	DD	1120022889
	DD	3166909893
	DD	1142437879
	DD	1134049648
	DD	1120598461
	DD	3167455696
	DD	1142879545
	DD	1134491331
	DD	1121182721
	DD	3168059997
	DD	1143340765
	DD	1134952570
	DD	1121797948
	DD	3168665771
	DD	1143822405
	DD	1135434229
	DD	1122439952
	DD	3169303507
	DD	1144325369
	DD	1135937213
	DD	1123111348
	DD	3170002824
	DD	1144850601
	DD	1136462467
	DD	1123815345
	DD	3170701624
	DD	1145399087
	DD	1137010975
	DD	1124312276
	DD	3171154336
	DD	1145971857
	DD	1137583767
	DD	1124692030
	DD	3171532482
	DD	1146569986
	DD	1138181919
	DD	1125085665
	DD	3171936657
	DD	1147194596
	DD	1138806554
	DD	1125501347
	DD	3172359765
	DD	1147846859
	DD	1139458844
	DD	1125944278
	DD	3172796218
	DD	1148528001
	DD	1140140013
	DD	1126398298
	DD	3173253435
	DD	1149239298
	DD	1140851014
	DD	1126869848
	DD	3173728905
	DD	1149610693
	DD	1141222424
	DD	1127371609
	DD	3174201888
	DD	1149998532
	DD	1141610277
	DD	1127876532
	DD	3174738014
	DD	1150403541
	DD	1142015303
	DD	1128426452
	DD	3175297014
	DD	1150826482
	DD	1142438261
	DD	1128986134
	DD	3175849827
	DD	1151268148
	DD	1142879944
	DD	1129574771
	DD	3176460842
	DD	1151729368
	DD	1143341183
	DD	1130194189
	DD	3177073044
	DD	1152211008
	DD	1143822842
	DD	1130840207
	DD	3177684163
	DD	1152713973
	DD	1144325825
	DD	1131494986
	DD	3178389375
	DD	1153239205
	DD	1144851079
	DD	1132202663
	DD	3179093821
	DD	1153787691
	DD	1145399587
	DD	1132702002
	DD	3179547441
	DD	1154360461
	DD	1145972379
	DD	1133083443
	DD	3179928175
	DD	1154958590
	DD	1146570531
	DD	1133478694
	DD	3180334828
	DD	1155583200
	DD	1147195166
	DD	1133895924
	DD	3180743924
	DD	1156235464
	DD	1147847455
	DD	1134330106
	DD	3181182650
	DD	1156916606
	DD	1148528624
	DD	1134785545
	DD	3181625656
	DD	1157627905
	DD	1149239623
	DD	1135248223
	DD	3182103210
	DD	1157999300
	DD	1149611033
	DD	1135751286
	DD	3182610963
	DD	1158387138
	DD	1149998887
	DD	1136277916
	DD	3183116226
	DD	1158792148
	DD	1150403912
	DD	1136808568
	DD	3183677057
	DD	1159215089
	DD	1150826870
	DD	1137369393
	DD	3184231622
	DD	1159656755
	DD	1151268553
	DD	1137959124
	DD	3184844315
	DD	1160117975
	DD	1151729792
	DD	1138579590
	DD	3185458125
	DD	1160599615
	DD	1152211451
	DD	1139226612
	DD	3186070783
	DD	1161102580
	DD	1152714434
	DD	1139882351
	DD	3186777469
	DD	1161627812
	DD	1153239688
	DD	1140590948
	DD	3187483326
	DD	1162176298
	DD	1153788196
	DD	1141090889
	DD	3187937173
	DD	1162749068
	DD	1154360988
	DD	1141472752
	DD	3188318554
	DD	1163347197
	DD	1154959140
	DD	1141868407
	DD	3188725827
	DD	1163971807
	DD	1155583775
	DD	1142286024
	DD	3189135516
	DD	1164624071
	DD	1156236064
	DD	1142720577
	DD	3189574810
	DD	1165305213
	DD	1156917233
	DD	1143176370
	DD	3190034748
	DD	1166016512
	DD	1157628232
	DD	1143649619
	DD	3190480049
	DD	1166387908
	DD	1157999641
	DD	1144132546
	DD	3190988301
	DD	1166775746
	DD	1158387495
	DD	1144659488
	DD	3191494042
	DD	1167180756
	DD	1158792520
	DD	1145190438
	DD	3192088104
	DD	1167603696
	DD	1159215479
	DD	1145772009
	DD	3192610334
	DD	1168045363
	DD	1159657161
	DD	1146341553
	DD	3193223446
	DD	1168506583
	DD	1160118400
	DD	1146962281
	DD	3193837658
	DD	1168988223
	DD	1160600059
	DD	1147609554
	DD	3194483474
	DD	1169491187
	DD	1161103043
	DD	1148285994
	DD	3195157755
	DD	1170016420
	DD	1161628296
	DD	1148974361
	DD	3195863964
	DD	1170564906
	DD	1162176804
	DD	1149477009
	DD	3196321965
	DD	1171137676
	DD	1162749596
	DD	1149858978
	DD	3196703508
	DD	1171735805
	DD	1163347748
	DD	1150254734
	DD	3197110936
	DD	1172360415
	DD	1163972383
	DD	1150672447
	DD	3197520774
	DD	1173012679
	DD	1164624672
	DD	1151107093
	DD	3197960210
	DD	1173693821
	DD	1165305841
	DD	1151562975
	DD	3198420283
	DD	1174405120
	DD	1166016840
	DD	1152036309
	DD	3198898489
	DD	1174776515
	DD	1166388250
	DD	1152539778
	DD	3199374091
	DD	1175164354
	DD	1166776103
	DD	1153046337
	DD	3199879952
	DD	1175569364
	DD	1167181128
	DD	1153577361
	DD	3200474128
	DD	1175992304
	DD	1167604087
	DD	1154159004
	DD	3201029241
	DD	1176433970
	DD	1168045770
	DD	1154749077
	DD	3201609685
	DD	1176895191
	DD	1168507008
	DD	1155349410
	DD	3202223997
	DD	1177376831
	DD	1168988667
	DD	1155996745
	DD	3202869909
	DD	1177879795
	DD	1169491651
	DD	1156673246
	DD	3203544282
	DD	1178405028
	DD	1170016904
	DD	1157361670
	DD	3204250580
	DD	1178953514
	DD	1170565412
	DD	1157864996
	DD	3204709619
	DD	1179526284
	DD	1171138204
	DD	1158246990
	DD	3205091203
	DD	1180124413
	DD	1171736356
	DD	1158642772
	DD	3205498670
	DD	1180749023
	DD	1172360991
	DD	1159060509
	DD	3205908544
	DD	1181401287
	DD	1173013280
	DD	1159495178
	DD	3206348016
	DD	1182082429
	DD	1173694449
	DD	1159951082
	DD	3206808123
	DD	1182793728
	DD	1174405448
	DD	1160424437
	DD	3207286361
	DD	1183165123
	DD	1174776858
	DD	1160927927
	DD	3207761995
	DD	1183552962
	DD	1175164711
	DD	1161434505
	DD	3208300659
	DD	1183957971
	DD	1175569737
	DD	1161986009
	DD	3208862090
	DD	1184380912
	DD	1175992695
	DD	1162547209
	DD	3209417231
	DD	1184822578
	DD	1176434378
	DD	1163137299
	DD	3209997701
	DD	1185283799
	DD	1176895616
	DD	1163737648
	DD	3210612038
	DD	1185765439
	DD	1177377275
	DD	1164384999
	DD	3211257974
	DD	1186268403
	DD	1177880259
	DD	1165061514
	DD	3211932370
	DD	1186793636
	DD	1178405512
	DD	1165749953
	DD	3212638690
	DD	1187342122
	DD	1178954020
	DD	1166253448
	DD	3213097989
	DD	1187914892
	DD	1179526812
	DD	1166635449
	DD	3213479583
	DD	1188513021
	DD	1180124964
	DD	1167031237
	DD	3213887059
	DD	1189137631
	DD	1180749599
	DD	1167448981
	DD	3214296943
	DD	1189789895
	DD	1181401888
	DD	1167883655
	DD	3214736423
	DD	1190471037
	DD	1182083057
	DD	1168339565
	DD	32
	DD	32
	DD	32
	DD	32
	DD	32
	DD	32
	DD	32
	DD	32
	DD	32
	DD	32
	DD	32
	DD	32
	DD	32
	DD	32
	DD	32
	DD	32
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
	DD	2684354560
	DD	1079401119
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
_2il0floatpacket_103	DD	000000000H,080000000H,000000000H,000000000H
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
