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
	PUBLIC __svml_cbrt1_ha_e9

__svml_cbrt1_ha_e9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 200
        mov       eax, 2147483647
        mov       edx, -2146435072
        mov       ecx, -2097153
        vmovapd   xmm4, xmm0
        mov       r8d, 1015808
        vpshufd   xmm0, xmm4, 85
        mov       r11d, 2048
        vmovd     xmm3, eax
        vmovups   XMMWORD PTR [176+rsp], xmm13
        vpand     xmm1, xmm0, xmm3
        vmovd     xmm13, edx
        lea       rdx, QWORD PTR [__ImageBase]
        vpsubd    xmm3, xmm1, xmm13
        vmovd     xmm1, ecx
        mov       QWORD PTR [168+rsp], r13
        vpcmpgtd  xmm13, xmm3, xmm1
        vmovd     xmm3, r8d
        lea       r13, QWORD PTR [95+rsp]
        vmovq     xmm2, QWORD PTR [__svml_dcbrt_ha_data_internal+2624]
        vpand     xmm1, xmm0, xmm3
        vmovmskps eax, xmm13
        vpsrlq    xmm13, xmm4, 52
        vmovq     xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+2688]
        vpsrld    xmm3, xmm1, 12
        vpand     xmm1, xmm13, xmm2
        vpsrld    xmm0, xmm0, 20
        vpmuludq  xmm2, xmm1, xmm5
        and       r13, -64
        vmovd     r9d, xmm3
        vpshufd   xmm5, xmm2, 0
        vpshufd   xmm1, xmm1, 0
        vpsrld    xmm13, xmm5, 14
        vpsubd    xmm2, xmm1, xmm13
        vpaddd    xmm5, xmm13, xmm13
        vpsubd    xmm1, xmm2, xmm5
        vpslld    xmm2, xmm1, 8
        vpaddd    xmm3, xmm3, xmm2
        vpslld    xmm5, xmm3, 1
        vmovd     r10d, xmm5
        vmovd     xmm5, r11d
        vpand     xmm0, xmm0, xmm5
        movsxd    r10, r10d
        movsxd    r9, r9d
        mov       QWORD PTR [192+rsp], r13
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rdx+r10]
        vmovsd    xmm3, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+264+rdx+r10]
        mov       r10d, 682
        vmovd     xmm2, r10d
        vpor      xmm5, xmm0, xmm2
        vpaddd    xmm13, xmm5, xmm13
        vmovsd    xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+2496]
        vpslld    xmm13, xmm13, 20
        vpxor     xmm5, xmm5, xmm5
        vpunpckldq xmm2, xmm5, xmm13
        vandpd    xmm5, xmm4, xmm0
        vmovsd    xmm13, QWORD PTR [__svml_dcbrt_ha_data_internal+2432]
        vmovsd    xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+2368]
        vorpd     xmm5, xmm5, xmm13
        vandpd    xmm13, xmm4, xmm0
        vmovsd    xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+2304]
        vorpd     xmm13, xmm13, xmm0
        vmulsd    xmm3, xmm3, xmm2
        vsubsd    xmm5, xmm5, xmm13
        vmulsd    xmm0, xmm5, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rdx+r9]
        vmovsd    xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+1792]
        vmulsd    xmm13, xmm5, xmm0
        vaddsd    xmm5, xmm13, QWORD PTR [__svml_dcbrt_ha_data_internal+1856]
        vmulsd    xmm13, xmm5, xmm0
        vaddsd    xmm5, xmm13, QWORD PTR [__svml_dcbrt_ha_data_internal+1920]
        vmulsd    xmm13, xmm5, xmm0
        vaddsd    xmm5, xmm13, QWORD PTR [__svml_dcbrt_ha_data_internal+1984]
        vmulsd    xmm13, xmm5, xmm0
        vaddsd    xmm5, xmm13, QWORD PTR [__svml_dcbrt_ha_data_internal+2048]
        vmulsd    xmm13, xmm5, xmm0
        vaddsd    xmm5, xmm13, QWORD PTR [__svml_dcbrt_ha_data_internal+2112]
        vmulsd    xmm13, xmm5, xmm0
        vaddsd    xmm5, xmm13, QWORD PTR [__svml_dcbrt_ha_data_internal+2176]
        vmulsd    xmm13, xmm5, xmm0
        vmulsd    xmm5, xmm1, xmm2
        vaddsd    xmm13, xmm13, QWORD PTR [__svml_dcbrt_ha_data_internal+2240]
        vmulsd    xmm1, xmm5, xmm0
        vmulsd    xmm2, xmm13, xmm1
        vaddsd    xmm0, xmm2, xmm3
        vaddsd    xmm0, xmm0, xmm5
        and       eax, 1
        jne       _B1_3

_B1_2::

        vmovups   xmm13, XMMWORD PTR [176+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 200
        ret

_B1_3::

        vmovsd    QWORD PTR [r13], xmm4
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B1_6

_B1_4::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B1_2

_B1_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dcbrt_ha_cout_rare_internal
        jmp       _B1_4
        ALIGN     16

_B1_7::

__svml_cbrt1_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrt1_ha_e9_B1_B6:
	DD	416769
	DD	1430620
	DD	776252
	DD	1638667

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_7
	DD	imagerel _unwind___svml_cbrt1_ha_e9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_cbrt4_ha_l9

__svml_cbrt4_ha_l9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L6::

        sub       rsp, 552
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [496+rsp], ymm15
        vmovapd   ymm15, ymm0
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [399+rsp]
        and       r13, -64
        mov       QWORD PTR [536+rsp], r13
        vextracti128 xmm2, ymm15, 1
        vshufps   xmm1, xmm15, xmm2, 221
        vandps    xmm5, xmm1, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2752]
        vpsrld    xmm5, xmm5, 12
        vmovd     ecx, xmm5
        vandps    xmm4, xmm1, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2816]
        vpsrld    xmm1, xmm1, 20
        vpsubd    xmm0, xmm4, XMMWORD PTR [__svml_dcbrt_ha_data_internal+3008]
        movsxd    rcx, ecx
        vpextrd   r8d, xmm5, 1
        movsxd    r8, r8d
        vpextrd   r9d, xmm5, 2
        movsxd    r9, r9d
        vpextrd   r10d, xmm5, 3
        vmovsd    xmm3, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+rcx]
        movsxd    r10, r10d
        vmovhpd   xmm4, xmm3, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+r8]
        vpsrlq    ymm3, ymm15, 52
        vpcmpgtd  xmm2, xmm0, XMMWORD PTR [__svml_dcbrt_ha_data_internal+3072]
        vmovsd    xmm0, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+r9]
        vmovmskps edx, xmm2
        vmovhpd   xmm2, xmm0, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+r10]
        vinsertf128 ymm2, ymm4, xmm2, 1
        vpand     ymm4, ymm3, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2624]
        vpmuludq  ymm0, ymm4, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2688]
        vextracti128 xmm3, ymm4, 1
        vshufps   xmm4, xmm4, xmm3, 136
        vextracti128 xmm3, ymm0, 1
        vshufps   xmm0, xmm0, xmm3, 136
        vpsrld    xmm0, xmm0, 14
        vpsubd    xmm4, xmm4, xmm0
        vpaddd    xmm3, xmm0, xmm0
        vpsubd    xmm4, xmm4, xmm3
        vpslld    xmm4, xmm4, 8
        vpaddd    xmm5, xmm5, xmm4
        vpslld    xmm4, xmm5, 1
        vmovd     r11d, xmm4
        vpextrd   ecx, xmm4, 1
        movsxd    r11, r11d
        movsxd    rcx, ecx
        vpextrd   r8d, xmm4, 2
        vpextrd   r9d, xmm4, 3
        movsxd    r8, r8d
        movsxd    r9, r9d
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+r11]
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+rcx]
        vinsertf128 ymm5, ymm5, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+r8], 1
        vinsertf128 ymm3, ymm3, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+r9], 1
        vunpcklpd ymm4, ymm5, ymm3
        vunpckhpd ymm5, ymm5, ymm3
        vpand     xmm3, xmm1, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2880]
        vpor      xmm1, xmm3, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2944]
        vmovups   ymm3, YMMWORD PTR [__VUNPACK_ODD_ind1_2671_0_2]
        vpaddd    xmm0, xmm1, xmm0
        vpslld    xmm0, xmm0, 20
        vpermps   ymm1, ymm3, ymm0
        vandpd    ymm0, ymm15, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2496]
        vandps    ymm3, ymm1, YMMWORD PTR [__VUNPACK_ODD_mask_2671_0_2]
        vorpd     ymm1, ymm0, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2432]
        vandpd    ymm0, ymm15, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2368]
        vorpd     ymm0, ymm0, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2304]
        vsubpd    ymm1, ymm1, ymm0
        vmovupd   ymm0, YMMWORD PTR [__svml_dcbrt_ha_data_internal+1792]
        vmulpd    ymm4, ymm4, ymm3
        vmulpd    ymm2, ymm2, ymm1
        vmulpd    ymm5, ymm5, ymm3
        vfmadd213pd ymm0, ymm2, YMMWORD PTR [__svml_dcbrt_ha_data_internal+1856]
        vfmadd213pd ymm0, ymm2, YMMWORD PTR [__svml_dcbrt_ha_data_internal+1920]
        vfmadd213pd ymm0, ymm2, YMMWORD PTR [__svml_dcbrt_ha_data_internal+1984]
        vfmadd213pd ymm0, ymm2, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2048]
        vfmadd213pd ymm0, ymm2, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2112]
        vfmadd213pd ymm0, ymm2, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2176]
        vfmadd213pd ymm0, ymm2, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2240]
        vmulpd    ymm2, ymm2, ymm4
        vmulpd    ymm0, ymm0, ymm2
        vaddpd    ymm1, ymm5, ymm0
        vaddpd    ymm0, ymm4, ymm1
        test      edx, edx
        jne       _B2_3

_B2_2::

        vmovups   ymm15, YMMWORD PTR [496+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B2_3::

        vmovupd   YMMWORD PTR [r13], ymm15
        vmovupd   YMMWORD PTR [64+r13], ymm0

_B2_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [288+rsp], ymm6
        vmovups   YMMWORD PTR [256+rsp], ymm7
        vmovups   YMMWORD PTR [224+rsp], ymm8
        vmovups   YMMWORD PTR [192+rsp], ymm9
        vmovups   YMMWORD PTR [160+rsp], ymm10
        vmovups   YMMWORD PTR [128+rsp], ymm11
        vmovups   YMMWORD PTR [96+rsp], ymm12
        vmovups   YMMWORD PTR [64+rsp], ymm13
        vmovups   YMMWORD PTR [32+rsp], ymm14
        mov       QWORD PTR [328+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [320+rsp], rsi
        mov       esi, edx

_B2_7::

        bt        esi, ebx
        jc        _B2_10

_B2_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B2_7

_B2_9::

        vmovups   ymm6, YMMWORD PTR [288+rsp]
        vmovups   ymm7, YMMWORD PTR [256+rsp]
        vmovups   ymm8, YMMWORD PTR [224+rsp]
        vmovups   ymm9, YMMWORD PTR [192+rsp]
        vmovups   ymm10, YMMWORD PTR [160+rsp]
        vmovups   ymm11, YMMWORD PTR [128+rsp]
        vmovups   ymm12, YMMWORD PTR [96+rsp]
        vmovups   ymm13, YMMWORD PTR [64+rsp]
        vmovups   ymm14, YMMWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [328+rsp]
        mov       rsi, QWORD PTR [320+rsp]
        jmp       _B2_2

_B2_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dcbrt_ha_cout_rare_internal
        jmp       _B2_8
        ALIGN     16

_B2_11::

__svml_cbrt4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrt4_ha_l9_B1_B3:
	DD	403201
	DD	4379687
	DD	2095131
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_cbrt4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrt4_ha_l9_B6_B10:
	DD	1858561
	DD	2647132
	DD	2700370
	DD	190538
	DD	317508
	DD	444478
	DD	571448
	DD	698415
	DD	825382
	DD	952349
	DD	1079316
	DD	1206283
	DD	2095104
	DD	4379648
	DD	4522240

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_6
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_cbrt4_ha_l9_B6_B10

.pdata	ENDS
_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
__VUNPACK_ODD_ind1_2671_0_2	DD	0
	DD	0
	DD	0
	DD	1
	DD	0
	DD	2
	DD	0
	DD	3
	DD 8 DUP (0H)	
__VUNPACK_ODD_mask_2671_0_2	DD	0
	DD	-1
	DD	0
	DD	-1
	DD	0
	DD	-1
	DD	0
	DD	-1
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_cbrt8_ha_z0

__svml_cbrt8_ha_z0	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L33::

        vgetexppd zmm25, zmm0 {sae}
        vmovups   zmm26, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+384]
        vgetmantpd zmm3, zmm0, 0 {sae}
        vmovups   zmm27, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+512]
        vmovups   zmm28, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+576]
        vmovups   zmm29, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+640]
        vmovups   zmm2, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+128]
        vmovups   zmm5, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+704]
        vrcp14pd  zmm31, zmm3
        vaddpd    zmm30, zmm25, zmm26 {rn-sae}
        vmovups   zmm25, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+768]
        vrndscalepd zmm4, zmm31, 72 {sae}
        vfmsub231pd zmm28, zmm27, zmm30 {rn-sae}
        vmovups   zmm27, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+256]
        vmovups   zmm26, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+896]
        vpsrlq    zmm1, zmm4, 49
        vrndscalepd zmm24, zmm28, 9 {sae}
        vfmsub231pd zmm5, zmm3, zmm4 {rn-sae}
        vmovups   zmm4, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+832]
        vpermt2pd zmm2, zmm1, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+192]
        vpermt2pd zmm27, zmm1, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+320]
        vfnmadd231pd zmm30, zmm29, zmm24 {rn-sae}
        vfmadd231pd zmm4, zmm25, zmm5 {rn-sae}
        vmovups   zmm29, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+1024]
        vmovups   zmm25, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+1088]
        vmulpd    zmm1, zmm5, zmm5 {rn-sae}
        vfmadd231pd zmm25, zmm29, zmm5 {rn-sae}
        vpermpd   zmm23, zmm30, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512]
        vpermpd   zmm22, zmm30, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+64]
        vmovups   zmm30, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+960]
        vmulpd    zmm3, zmm23, zmm2 {rn-sae}
        vfmadd231pd zmm30, zmm26, zmm5 {rn-sae}
        vmovups   zmm26, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+1216]
        vfmadd213pd zmm4, zmm1, zmm30 {rn-sae}
        vmovaps   zmm28, zmm23
        vfmsub213pd zmm28, zmm2, zmm3 {rn-sae}
        vfmadd213pd zmm4, zmm1, zmm25 {rn-sae}
        vfmadd213pd zmm23, zmm27, zmm28 {rn-sae}
        vfmadd213pd zmm22, zmm2, zmm23 {rn-sae}
        vmovups   zmm2, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+1152]
        vmovups   zmm23, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+1280]
        vfmadd231pd zmm26, zmm2, zmm5 {rn-sae}
        vmovups   zmm2, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+1344]
        vfmadd213pd zmm4, zmm1, zmm26 {rn-sae}
        vfmadd231pd zmm2, zmm23, zmm5 {rn-sae}
        vmulpd    zmm5, zmm3, zmm5 {rn-sae}
        vfmadd213pd zmm4, zmm1, zmm2 {rn-sae}
        vfmadd213pd zmm5, zmm4, zmm22 {rn-sae}
        vaddpd    zmm22, zmm5, zmm3 {rn-sae}
        vscalefpd zmm24, zmm22, zmm24 {rn-sae}
        vpternlogq zmm0, zmm24, ZMMWORD PTR [__svml_dcbrt_ha_data_internal_avx512+448], 236
        ret
        ALIGN     16

_B3_2::

__svml_cbrt8_ha_z0 ENDP

_TEXT	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_cbrt1_ha_l9

__svml_cbrt1_ha_l9	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L34::

        sub       rsp, 200
        mov       eax, 2147483647
        mov       edx, -2146435072
        mov       ecx, -2097153
        vmovapd   xmm4, xmm0
        mov       r8d, 1015808
        vpshufd   xmm0, xmm4, 85
        mov       r11d, 2048
        vmovd     xmm5, eax
        vpand     xmm3, xmm0, xmm5
        vmovd     xmm1, edx
        vpsubd    xmm5, xmm3, xmm1
        vmovd     xmm3, ecx
        vmovups   XMMWORD PTR [176+rsp], xmm14
        vpcmpgtd  xmm1, xmm5, xmm3
        mov       QWORD PTR [168+rsp], r13
        vmovd     xmm5, r8d
        vmovq     xmm2, QWORD PTR [__svml_dcbrt_ha_data_internal+2624]
        vpand     xmm3, xmm0, xmm5
        vmovmskps eax, xmm1
        vpsrlq    xmm1, xmm4, 52
        vmovq     xmm14, QWORD PTR [__svml_dcbrt_ha_data_internal+2688]
        vpand     xmm1, xmm1, xmm2
        vpsrld    xmm3, xmm3, 12
        vpsrld    xmm0, xmm0, 20
        vpmuludq  xmm2, xmm1, xmm14
        lea       rdx, QWORD PTR [__ImageBase]
        vmovd     r9d, xmm3
        vpshufd   xmm14, xmm2, 0
        vpshufd   xmm1, xmm1, 0
        vpsrld    xmm5, xmm14, 14
        vpsubd    xmm2, xmm1, xmm5
        vpaddd    xmm14, xmm5, xmm5
        vpsubd    xmm1, xmm2, xmm14
        lea       r13, QWORD PTR [95+rsp]
        vpslld    xmm2, xmm1, 8
        and       r13, -64
        vpaddd    xmm3, xmm3, xmm2
        vpslld    xmm14, xmm3, 1
        vmovd     r10d, xmm14
        vmovd     xmm14, r11d
        vpand     xmm0, xmm0, xmm14
        movsxd    r10, r10d
        movsxd    r9, r9d
        mov       QWORD PTR [192+rsp], r13
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rdx+r10]
        vmovsd    xmm3, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+264+rdx+r10]
        mov       r10d, 682
        vmovd     xmm2, r10d
        vpor      xmm14, xmm0, xmm2
        vpaddd    xmm5, xmm14, xmm5
        vpslld    xmm0, xmm5, 20
        vpxor     xmm14, xmm14, xmm14
        vmovsd    xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+2496]
        vpunpckldq xmm2, xmm14, xmm0
        vandpd    xmm14, xmm4, xmm5
        vmovsd    xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+2432]
        vmovsd    xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+2368]
        vorpd     xmm14, xmm14, xmm0
        vandpd    xmm0, xmm4, xmm5
        vmovsd    xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+2304]
        vorpd     xmm0, xmm0, xmm5
        vmulsd    xmm3, xmm3, xmm2
        vsubsd    xmm14, xmm14, xmm0
        vmulsd    xmm5, xmm14, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rdx+r9]
        vmulsd    xmm14, xmm1, xmm2
        vmovsd    xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+1792]
        vfmadd213sd xmm0, xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+1856]
        vmulsd    xmm1, xmm14, xmm5
        vfmadd213sd xmm0, xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+1920]
        vfmadd213sd xmm0, xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+1984]
        vfmadd213sd xmm0, xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+2048]
        vfmadd213sd xmm0, xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+2112]
        vfmadd213sd xmm0, xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+2176]
        vfmadd213sd xmm0, xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+2240]
        vmulsd    xmm2, xmm0, xmm1
        vaddsd    xmm0, xmm2, xmm3
        vaddsd    xmm0, xmm0, xmm14
        and       eax, 1
        jne       _B4_3

_B4_2::

        vmovups   xmm14, XMMWORD PTR [176+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 200
        ret

_B4_3::

        vmovsd    QWORD PTR [r13], xmm4
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B4_6

_B4_4::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B4_2

_B4_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dcbrt_ha_cout_rare_internal
        jmp       _B4_4
        ALIGN     16

_B4_7::

__svml_cbrt1_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrt1_ha_l9_B1_B6:
	DD	415745
	DD	1430616
	DD	780364
	DD	1638667

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_7
	DD	imagerel _unwind___svml_cbrt1_ha_l9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_cbrt2_ha_l9

__svml_cbrt2_ha_l9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L39::

        sub       rsp, 264
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [224+rsp], xmm12
        vmovups   XMMWORD PTR [192+rsp], xmm11
        vmovapd   xmm11, xmm0
        vmovups   XMMWORD PTR [208+rsp], xmm10
        mov       QWORD PTR [240+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovq     xmm4, QWORD PTR [__svml_dcbrt_ha_data_internal+2752]
        and       r13, -64
        vmovq     xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+2816]
        vpshufd   xmm2, xmm11, 221
        vmovq     xmm10, QWORD PTR [__svml_dcbrt_ha_data_internal+3008]
        vpand     xmm3, xmm2, xmm5
        vpand     xmm4, xmm2, xmm4
        vpsubd    xmm5, xmm3, xmm10
        vpsrld    xmm10, xmm4, 12
        vpsrlq    xmm3, xmm11, 52
        vmovd     ecx, xmm10
        vpsrld    xmm2, xmm2, 20
        vmovq     xmm12, QWORD PTR [__svml_dcbrt_ha_data_internal+3072]
        vpcmpgtd  xmm12, xmm5, xmm12
        movsxd    rcx, ecx
        vpextrd   r8d, xmm10, 1
        movsxd    r8, r8d
        vmovmskps edx, xmm12
        vmovsd    xmm12, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+rcx]
        vpand     xmm5, xmm3, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2624]
        vmovhpd   xmm4, xmm12, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+r8]
        vpmuludq  xmm12, xmm5, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2688]
        vpshufd   xmm3, xmm12, 136
        vpshufd   xmm5, xmm5, 136
        vpsrld    xmm3, xmm3, 14
        vpsubd    xmm12, xmm5, xmm3
        vpaddd    xmm5, xmm3, xmm3
        vpsubd    xmm12, xmm12, xmm5
        vpslld    xmm5, xmm12, 8
        vpaddd    xmm10, xmm10, xmm5
        vpslld    xmm5, xmm10, 1
        vmovd     r9d, xmm5
        vmovq     xmm1, QWORD PTR [__svml_dcbrt_ha_data_internal+2880]
        vmovq     xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+2944]
        vpand     xmm1, xmm2, xmm1
        vpextrd   r10d, xmm5, 1
        movsxd    r9, r9d
        movsxd    r10, r10d
        vandpd    xmm2, xmm11, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2496]
        mov       QWORD PTR [248+rsp], r13
        vmovupd   xmm12, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+r9]
        vmovupd   xmm10, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+r10]
        vunpcklpd xmm5, xmm12, xmm10
        vunpckhpd xmm10, xmm12, xmm10
        vpor      xmm12, xmm1, xmm0
        vpaddd    xmm3, xmm12, xmm3
        vandpd    xmm12, xmm11, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2368]
        vpslld    xmm1, xmm3, 20
        vpxor     xmm0, xmm0, xmm0
        vpunpckldq xmm3, xmm0, xmm1
        vorpd     xmm0, xmm2, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2432]
        vorpd     xmm1, xmm12, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2304]
        vsubpd    xmm2, xmm0, xmm1
        vmulpd    xmm12, xmm5, xmm3
        vmulpd    xmm4, xmm4, xmm2
        vmulpd    xmm10, xmm10, xmm3
        vmulpd    xmm5, xmm4, xmm12
        vmovupd   xmm0, XMMWORD PTR [__svml_dcbrt_ha_data_internal+1792]
        vfmadd213pd xmm0, xmm4, XMMWORD PTR [__svml_dcbrt_ha_data_internal+1856]
        vfmadd213pd xmm0, xmm4, XMMWORD PTR [__svml_dcbrt_ha_data_internal+1920]
        vfmadd213pd xmm0, xmm4, XMMWORD PTR [__svml_dcbrt_ha_data_internal+1984]
        vfmadd213pd xmm0, xmm4, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2048]
        vfmadd213pd xmm0, xmm4, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2112]
        vfmadd213pd xmm0, xmm4, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2176]
        vfmadd213pd xmm0, xmm4, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2240]
        vmulpd    xmm0, xmm0, xmm5
        vaddpd    xmm1, xmm10, xmm0
        vaddpd    xmm0, xmm12, xmm1
        and       edx, 3
        jne       _B5_3

_B5_2::

        vmovups   xmm10, XMMWORD PTR [208+rsp]
        vmovups   xmm11, XMMWORD PTR [192+rsp]
        vmovups   xmm12, XMMWORD PTR [224+rsp]
        mov       r13, QWORD PTR [240+rsp]
        add       rsp, 264
        ret

_B5_3::

        vmovupd   XMMWORD PTR [r13], xmm11
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

        call      __svml_dcbrt_ha_cout_rare_internal
        jmp       _B5_8
        ALIGN     16

_B5_11::

__svml_cbrt2_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrt2_ha_l9_B1_B3:
	DD	669953
	DD	2020409
	DD	895025
	DD	833572
	DD	968731
	DD	2162955

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_cbrt2_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrt2_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_cbrt2_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_6
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_cbrt2_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_cbrt2_ha_e9

__svml_cbrt2_ha_e9	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L52::

        sub       rsp, 264
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [208+rsp], xmm9
        vmovups   XMMWORD PTR [224+rsp], xmm8
        vmovapd   xmm8, xmm0
        vmovups   XMMWORD PTR [192+rsp], xmm6
        mov       QWORD PTR [240+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovq     xmm9, QWORD PTR [__svml_dcbrt_ha_data_internal+2752]
        and       r13, -64
        vmovq     xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+2816]
        vpshufd   xmm2, xmm8, 221
        vmovq     xmm6, QWORD PTR [__svml_dcbrt_ha_data_internal+3008]
        vpand     xmm3, xmm2, xmm5
        vpand     xmm9, xmm2, xmm9
        vpsubd    xmm5, xmm3, xmm6
        vpsrld    xmm6, xmm9, 12
        vpsrlq    xmm9, xmm8, 52
        vmovd     ecx, xmm6
        vpsrld    xmm2, xmm2, 20
        vmovq     xmm4, QWORD PTR [__svml_dcbrt_ha_data_internal+3072]
        vpcmpgtd  xmm4, xmm5, xmm4
        movsxd    rcx, ecx
        vpextrd   r8d, xmm6, 1
        movsxd    r8, r8d
        vpand     xmm5, xmm9, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2624]
        vmovsd    xmm3, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+rcx]
        vmovmskps edx, xmm4
        vmovhpd   xmm4, xmm3, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+r8]
        vmovq     xmm1, QWORD PTR [__svml_dcbrt_ha_data_internal+2880]
        vpmuludq  xmm3, xmm5, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2688]
        vpand     xmm1, xmm2, xmm1
        vpshufd   xmm9, xmm3, 136
        vpshufd   xmm5, xmm5, 136
        vpsrld    xmm3, xmm9, 14
        vpsubd    xmm9, xmm5, xmm3
        vpaddd    xmm5, xmm3, xmm3
        vpsubd    xmm9, xmm9, xmm5
        vpslld    xmm5, xmm9, 8
        vpaddd    xmm6, xmm6, xmm5
        vpslld    xmm5, xmm6, 1
        vmovd     r9d, xmm5
        vmovq     xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+2944]
        vandpd    xmm2, xmm8, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2496]
        vpextrd   r10d, xmm5, 1
        movsxd    r9, r9d
        movsxd    r10, r10d
        mov       QWORD PTR [248+rsp], r13
        vmovupd   xmm9, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+r9]
        vmovupd   xmm6, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+r10]
        vunpcklpd xmm5, xmm9, xmm6
        vunpckhpd xmm6, xmm9, xmm6
        vpor      xmm9, xmm1, xmm0
        vpaddd    xmm3, xmm9, xmm3
        vandpd    xmm9, xmm8, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2368]
        vpslld    xmm1, xmm3, 20
        vpxor     xmm0, xmm0, xmm0
        vpunpckldq xmm3, xmm0, xmm1
        vorpd     xmm0, xmm2, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2432]
        vorpd     xmm1, xmm9, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2304]
        vsubpd    xmm2, xmm0, xmm1
        vmulpd    xmm4, xmm4, xmm2
        vmulpd    xmm9, xmm4, XMMWORD PTR [__svml_dcbrt_ha_data_internal+1792]
        vaddpd    xmm0, xmm9, XMMWORD PTR [__svml_dcbrt_ha_data_internal+1856]
        vmulpd    xmm1, xmm4, xmm0
        vaddpd    xmm2, xmm1, XMMWORD PTR [__svml_dcbrt_ha_data_internal+1920]
        vmulpd    xmm9, xmm4, xmm2
        vaddpd    xmm0, xmm9, XMMWORD PTR [__svml_dcbrt_ha_data_internal+1984]
        vmulpd    xmm1, xmm4, xmm0
        vaddpd    xmm2, xmm1, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2048]
        vmulpd    xmm9, xmm4, xmm2
        vaddpd    xmm0, xmm9, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2112]
        vmulpd    xmm1, xmm4, xmm0
        vaddpd    xmm2, xmm1, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2176]
        vmulpd    xmm9, xmm4, xmm2
        vaddpd    xmm0, xmm9, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2240]
        vmulpd    xmm9, xmm5, xmm3
        vmulpd    xmm5, xmm6, xmm3
        vmulpd    xmm4, xmm4, xmm9
        vmulpd    xmm6, xmm0, xmm4
        vaddpd    xmm0, xmm5, xmm6
        vaddpd    xmm0, xmm9, xmm0
        and       edx, 3
        jne       _B6_3

_B6_2::

        vmovups   xmm6, XMMWORD PTR [192+rsp]
        vmovups   xmm8, XMMWORD PTR [224+rsp]
        vmovups   xmm9, XMMWORD PTR [208+rsp]
        mov       r13, QWORD PTR [240+rsp]
        add       rsp, 264
        ret

_B6_3::

        vmovupd   XMMWORD PTR [r13], xmm8
        vmovupd   XMMWORD PTR [64+r13], xmm0
        je        _B6_2

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
        cmp       ebx, 2
        jl        _B6_7

_B6_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B6_2

_B6_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dcbrt_ha_cout_rare_internal
        jmp       _B6_8
        ALIGN     16

_B6_11::

__svml_cbrt2_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrt2_ha_e9_B1_B3:
	DD	669953
	DD	2020409
	DD	813105
	DD	952356
	DD	890907
	DD	2162955

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_cbrt2_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrt2_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_cbrt2_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_6
	DD	imagerel _B6_11
	DD	imagerel _unwind___svml_cbrt2_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_cbrt1_ha_ex

__svml_cbrt1_ha_ex	PROC	EXPORT

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L65::

        sub       rsp, 200
        mov       eax, 2147483647
        mov       edx, -2146435072
        movaps    xmm1, xmm0
        movups    XMMWORD PTR [176+rsp], xmm11
        mov       ecx, -2097153
        pshufd    xmm0, xmm1, 85
        mov       r8d, 1015808
        movd      xmm11, eax
        mov       r11d, 2048
        pand      xmm11, xmm0
        movd      xmm3, edx
        mov       QWORD PTR [168+rsp], r13
        psubd     xmm11, xmm3
        movaps    xmm3, xmm1
        movd      xmm2, ecx
        movq      xmm5, QWORD PTR [__svml_dcbrt_ha_data_internal+2624]
        psrlq     xmm3, 52
        movq      xmm4, QWORD PTR [__svml_dcbrt_ha_data_internal+2688]
        pand      xmm3, xmm5
        pmuludq   xmm4, xmm3
        pcmpgtd   xmm11, xmm2
        movmskps  eax, xmm11
        pshufd    xmm2, xmm4, 0
        movd      xmm11, r8d
        psrld     xmm2, 14
        pand      xmm11, xmm0
        pshufd    xmm5, xmm3, 0
        movdqa    xmm4, xmm2
        psubd     xmm5, xmm2
        paddd     xmm4, xmm2
        psubd     xmm5, xmm4
        psrld     xmm11, 12
        pslld     xmm5, 8
        lea       rdx, QWORD PTR [__ImageBase]
        movd      r9d, xmm11
        paddd     xmm11, xmm5
        pslld     xmm11, 1
        psrld     xmm0, 20
        movd      xmm3, r11d
        movd      r10d, xmm11
        pand      xmm0, xmm3
        pxor      xmm3, xmm3
        movsxd    r10, r10d
        lea       r13, QWORD PTR [95+rsp]
        movsxd    r9, r9d
        and       r13, -64
        mov       QWORD PTR [192+rsp], r13
        and       eax, 1
        movups    xmm5, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rdx+r10]
        movsd     xmm4, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+264+rdx+r10]
        mov       r10d, 682
        movd      xmm11, r10d
        por       xmm0, xmm11
        paddd     xmm0, xmm2
        movsd     xmm2, QWORD PTR [__svml_dcbrt_ha_data_internal+2496]
        pslld     xmm0, 20
        punpckldq xmm3, xmm0
        andps     xmm2, xmm1
        movsd     xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+2432]
        movsd     xmm11, QWORD PTR [__svml_dcbrt_ha_data_internal+2368]
        orps      xmm2, xmm0
        movsd     xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+2304]
        andps     xmm11, xmm1
        orps      xmm11, xmm0
        movsd     xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+1792]
        subsd     xmm2, xmm11
        mulsd     xmm5, xmm3
        mulsd     xmm4, xmm3
        mulsd     xmm2, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rdx+r9]
        mulsd     xmm0, xmm2
        movaps    xmm3, xmm5
        mulsd     xmm3, xmm2
        addsd     xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+1856]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+1920]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+1984]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+2048]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+2112]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+2176]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+2240]
        mulsd     xmm0, xmm3
        addsd     xmm0, xmm4
        addsd     xmm0, xmm5
        jne       _B7_3

_B7_2::

        movups    xmm11, XMMWORD PTR [176+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 200
        ret

_B7_3::

        movsd     QWORD PTR [r13], xmm1
        movsd     QWORD PTR [64+r13], xmm0
        jne       _B7_6

_B7_4::

        movsd     xmm0, QWORD PTR [64+r13]
        jmp       _B7_2

_B7_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dcbrt_ha_cout_rare_internal
        jmp       _B7_4
        ALIGN     16

_B7_7::

__svml_cbrt1_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrt1_ha_ex_B1_B6:
	DD	412929
	DD	1430605
	DD	768033
	DD	1638667

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_7
	DD	imagerel _unwind___svml_cbrt1_ha_ex_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST7:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_cbrt4_ha_e9

__svml_cbrt4_ha_e9	PROC	EXPORT

_B8_1::

        DB        243
        DB        15
        DB        30
        DB        250
L70::

        sub       rsp, 552
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [464+rsp], ymm15
        vmovups   YMMWORD PTR [496+rsp], ymm8
        vmovapd   ymm15, ymm0
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [367+rsp]
        vmovupd   xmm0, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2624]
        and       r13, -64
        vmovupd   xmm1, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2688]
        mov       QWORD PTR [536+rsp], r13
        vextractf128 xmm2, ymm15, 1
        vshufps   xmm3, xmm15, xmm2, 221
        vpsrlq    xmm2, xmm2, 52
        vandps    xmm5, xmm3, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2816]
        vpand     xmm2, xmm2, xmm0
        vpsubd    xmm4, xmm5, XMMWORD PTR [__svml_dcbrt_ha_data_internal+3008]
        vpcmpgtd  xmm5, xmm4, XMMWORD PTR [__svml_dcbrt_ha_data_internal+3072]
        vandps    xmm4, xmm3, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2752]
        vpsrld    xmm3, xmm3, 20
        vpsrld    xmm4, xmm4, 12
        vmovd     ecx, xmm4
        vmovmskps edx, xmm5
        vpand     xmm3, xmm3, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2880]
        movsxd    rcx, ecx
        vpextrd   r8d, xmm4, 1
        vpextrd   r9d, xmm4, 2
        movsxd    r8, r8d
        movsxd    r9, r9d
        vpextrd   r10d, xmm4, 3
        movsxd    r10, r10d
        vmovsd    xmm8, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+rcx]
        vmovhpd   xmm5, xmm8, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+r8]
        vmovsd    xmm8, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+r9]
        vmovhpd   xmm8, xmm8, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+r10]
        vinsertf128 ymm5, ymm5, xmm8, 1
        vpsrlq    xmm8, xmm15, 52
        vpand     xmm8, xmm8, xmm0
        vshufps   xmm0, xmm8, xmm2, 136
        vpmuludq  xmm8, xmm8, xmm1
        vpmuludq  xmm1, xmm2, xmm1
        vshufps   xmm1, xmm8, xmm1, 136
        vpsrld    xmm1, xmm1, 14
        vpsubd    xmm0, xmm0, xmm1
        vpaddd    xmm2, xmm1, xmm1
        vpsubd    xmm8, xmm0, xmm2
        vpslld    xmm0, xmm8, 8
        vpaddd    xmm4, xmm4, xmm0
        vpslld    xmm2, xmm4, 1
        vmovd     r11d, xmm2
        vpextrd   ecx, xmm2, 1
        movsxd    r11, r11d
        movsxd    rcx, ecx
        vpextrd   r8d, xmm2, 2
        vpextrd   r9d, xmm2, 3
        movsxd    r8, r8d
        movsxd    r9, r9d
        vpor      xmm2, xmm3, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2944]
        vmovupd   xmm8, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+r11]
        vpaddd    xmm1, xmm2, xmm1
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+rcx]
        vpslld    xmm1, xmm1, 20
        vpxor     xmm3, xmm3, xmm3
        vpunpckhdq xmm2, xmm3, xmm1
        vinsertf128 ymm8, ymm8, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+r8], 1
        vinsertf128 ymm0, ymm4, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+r9], 1
        vunpcklpd ymm4, ymm8, ymm0
        vunpckhpd ymm0, ymm8, ymm0
        vpunpckldq xmm8, xmm3, xmm1
        vandpd    ymm3, ymm15, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2496]
        vandpd    ymm1, ymm15, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2368]
        vinsertf128 ymm2, ymm8, xmm2, 1
        vorpd     ymm8, ymm3, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2432]
        vorpd     ymm3, ymm1, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2304]
        vsubpd    ymm1, ymm8, ymm3
        vmulpd    ymm1, ymm5, ymm1
        vmulpd    ymm5, ymm1, YMMWORD PTR [__svml_dcbrt_ha_data_internal+1792]
        vaddpd    ymm3, ymm5, YMMWORD PTR [__svml_dcbrt_ha_data_internal+1856]
        vmulpd    ymm5, ymm1, ymm3
        vaddpd    ymm8, ymm5, YMMWORD PTR [__svml_dcbrt_ha_data_internal+1920]
        vmulpd    ymm3, ymm1, ymm8
        vaddpd    ymm5, ymm3, YMMWORD PTR [__svml_dcbrt_ha_data_internal+1984]
        vmulpd    ymm8, ymm1, ymm5
        vaddpd    ymm3, ymm8, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2048]
        vmulpd    ymm5, ymm1, ymm3
        vaddpd    ymm8, ymm5, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2112]
        vmulpd    ymm3, ymm1, ymm8
        vaddpd    ymm5, ymm3, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2176]
        vmulpd    ymm3, ymm4, ymm2
        vmulpd    ymm4, ymm0, ymm2
        vmulpd    ymm8, ymm1, ymm5
        vmulpd    ymm0, ymm1, ymm3
        vaddpd    ymm5, ymm8, YMMWORD PTR [__svml_dcbrt_ha_data_internal+2240]
        vmulpd    ymm1, ymm5, ymm0
        vaddpd    ymm2, ymm4, ymm1
        vaddpd    ymm0, ymm3, ymm2
        test      edx, edx
        jne       _B8_3

_B8_2::

        vmovups   ymm8, YMMWORD PTR [496+rsp]
        vmovups   ymm15, YMMWORD PTR [464+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B8_3::

        vmovupd   YMMWORD PTR [r13], ymm15
        vmovupd   YMMWORD PTR [64+r13], ymm0

_B8_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [256+rsp], ymm6
        vmovups   YMMWORD PTR [224+rsp], ymm7
        vmovups   YMMWORD PTR [192+rsp], ymm9
        vmovups   YMMWORD PTR [160+rsp], ymm10
        vmovups   YMMWORD PTR [128+rsp], ymm11
        vmovups   YMMWORD PTR [96+rsp], ymm12
        vmovups   YMMWORD PTR [64+rsp], ymm13
        vmovups   YMMWORD PTR [32+rsp], ymm14
        mov       QWORD PTR [296+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [288+rsp], rsi
        mov       esi, edx

_B8_7::

        bt        esi, ebx
        jc        _B8_10

_B8_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B8_7

_B8_9::

        vmovups   ymm6, YMMWORD PTR [256+rsp]
        vmovups   ymm7, YMMWORD PTR [224+rsp]
        vmovups   ymm9, YMMWORD PTR [192+rsp]
        vmovups   ymm10, YMMWORD PTR [160+rsp]
        vmovups   ymm11, YMMWORD PTR [128+rsp]
        vmovups   ymm12, YMMWORD PTR [96+rsp]
        vmovups   ymm13, YMMWORD PTR [64+rsp]
        vmovups   ymm14, YMMWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [296+rsp]
        mov       rsi, QWORD PTR [288+rsp]
        jmp       _B8_2

_B8_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dcbrt_ha_cout_rare_internal
        jmp       _B8_8
        ALIGN     16

_B8_11::

__svml_cbrt4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrt4_ha_e9_B1_B3:
	DD	536577
	DD	4379696
	DD	2066468
	DD	1964059
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_1
	DD	imagerel _B8_6
	DD	imagerel _unwind___svml_cbrt4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrt4_ha_e9_B6_B10:
	DD	1332001
	DD	2384979
	DD	2438217
	DD	190529
	DD	317499
	DD	444469
	DD	571439
	DD	698406
	DD	825373
	DD	948244
	DD	1075211
	DD	imagerel _B8_1
	DD	imagerel _B8_6
	DD	imagerel _unwind___svml_cbrt4_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_6
	DD	imagerel _B8_11
	DD	imagerel _unwind___svml_cbrt4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST8:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_cbrt2_ha_ex

__svml_cbrt2_ha_ex	PROC	EXPORT

_B9_1::

        DB        243
        DB        15
        DB        30
        DB        250
L97::

        sub       rsp, 264
        movaps    xmm5, xmm0
        movups    XMMWORD PTR [224+rsp], xmm9
        lea       rax, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [192+rsp], xmm7
        movups    XMMWORD PTR [208+rsp], xmm6
        mov       QWORD PTR [240+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movq      xmm0, QWORD PTR [__svml_dcbrt_ha_data_internal+2752]
        and       r13, -64
        pshufd    xmm7, xmm5, 221
        pand      xmm0, xmm7
        psrld     xmm0, 12
        movq      xmm2, QWORD PTR [__svml_dcbrt_ha_data_internal+2816]
        pshufd    xmm9, xmm0, 1
        pand      xmm2, xmm7
        movq      xmm3, QWORD PTR [__svml_dcbrt_ha_data_internal+3008]
        psrld     xmm7, 20
        movd      r8d, xmm9
        movaps    xmm9, xmm5
        movq      xmm1, QWORD PTR [__svml_dcbrt_ha_data_internal+3072]
        psubd     xmm2, xmm3
        psrlq     xmm9, 52
        pcmpgtd   xmm2, xmm1
        pand      xmm9, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2624]
        movdqu    xmm1, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2688]
        pmuludq   xmm1, xmm9
        movmskps  edx, xmm2
        movd      ecx, xmm0
        movq      xmm4, QWORD PTR [__svml_dcbrt_ha_data_internal+2880]
        pshufd    xmm2, xmm1, 136
        pand      xmm7, xmm4
        movq      xmm6, QWORD PTR [__svml_dcbrt_ha_data_internal+2944]
        psrld     xmm2, 14
        por       xmm7, xmm6
        movdqa    xmm1, xmm2
        pshufd    xmm9, xmm9, 136
        paddd     xmm7, xmm2
        psubd     xmm9, xmm2
        paddd     xmm1, xmm2
        pslld     xmm7, 20
        psubd     xmm9, xmm1
        pxor      xmm2, xmm2
        pslld     xmm9, 8
        punpckldq xmm2, xmm7
        paddd     xmm0, xmm9
        movups    xmm7, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2496]
        pslld     xmm0, 1
        movups    xmm4, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2368]
        andps     xmm7, xmm5
        andps     xmm4, xmm5
        and       edx, 3
        orps      xmm7, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2432]
        orps      xmm4, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2304]
        movsxd    rcx, ecx
        subpd     xmm7, xmm4
        movd      r9d, xmm0
        movsd     xmm3, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+rcx]
        movsxd    r8, r8d
        movups    xmm6, XMMWORD PTR [__svml_dcbrt_ha_data_internal+1792]
        pshufd    xmm0, xmm0, 1
        movd      r10d, xmm0
        movhpd    xmm3, QWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+rax+r8]
        mulpd     xmm3, xmm7
        mulpd     xmm6, xmm3
        addpd     xmm6, XMMWORD PTR [__svml_dcbrt_ha_data_internal+1856]
        mulpd     xmm6, xmm3
        addpd     xmm6, XMMWORD PTR [__svml_dcbrt_ha_data_internal+1920]
        mulpd     xmm6, xmm3
        addpd     xmm6, XMMWORD PTR [__svml_dcbrt_ha_data_internal+1984]
        mulpd     xmm6, xmm3
        addpd     xmm6, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2048]
        mulpd     xmm6, xmm3
        addpd     xmm6, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2112]
        mulpd     xmm6, xmm3
        movsxd    r9, r9d
        movsxd    r10, r10d
        addpd     xmm6, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2176]
        movups    xmm1, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+r9]
        movups    xmm9, XMMWORD PTR [imagerel(__svml_dcbrt_ha_data_internal)+256+rax+r10]
        movaps    xmm0, xmm1
        unpcklpd  xmm0, xmm9
        mulpd     xmm0, xmm2
        mulpd     xmm6, xmm3
        mulpd     xmm3, xmm0
        addpd     xmm6, XMMWORD PTR [__svml_dcbrt_ha_data_internal+2240]
        unpckhpd  xmm1, xmm9
        mulpd     xmm1, xmm2
        mulpd     xmm6, xmm3
        addpd     xmm1, xmm6
        mov       QWORD PTR [248+rsp], r13
        addpd     xmm0, xmm1
        jne       _B9_3

_B9_2::

        movups    xmm6, XMMWORD PTR [208+rsp]
        movups    xmm7, XMMWORD PTR [192+rsp]
        movups    xmm9, XMMWORD PTR [224+rsp]
        mov       r13, QWORD PTR [240+rsp]
        add       rsp, 264
        ret

_B9_3::

        movups    XMMWORD PTR [r13], xmm5
        movups    XMMWORD PTR [64+r13], xmm0
        je        _B9_2

_B9_6::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B9_7::

        mov       ecx, ebx
        mov       eax, 1
        shl       eax, cl
        test      esi, eax
        jne       _B9_10

_B9_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B9_7

_B9_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [64+r13]
        jmp       _B9_2

_B9_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dcbrt_ha_cout_rare_internal
        jmp       _B9_8
        ALIGN     16

_B9_11::

__svml_cbrt2_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrt2_ha_ex_B1_B3:
	DD	669185
	DD	2020406
	DD	878638
	DD	817190
	DD	956439
	DD	2162955

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_1
	DD	imagerel _B9_6
	DD	imagerel _unwind___svml_cbrt2_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrt2_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B9_1
	DD	imagerel _B9_6
	DD	imagerel _unwind___svml_cbrt2_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_6
	DD	imagerel _B9_11
	DD	imagerel _unwind___svml_cbrt2_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST9:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_dcbrt_ha_cout_rare_internal

__svml_dcbrt_ha_cout_rare_internal	PROC

_B10_1::

        DB        243
        DB        15
        DB        30
        DB        250
L110::

        push      r12
        sub       rsp, 128
        mov       r8, rdx
        movzx     r10d, WORD PTR [6+rcx]
        lea       r12, QWORD PTR [__ImageBase]
        and       r10d, 32752
        shr       r10d, 4
        mov       r9b, BYTE PTR [7+rcx]
        movsd     xmm1, QWORD PTR [rcx]
        movups    XMMWORD PTR [64+rsp], xmm13
        movups    XMMWORD PTR [48+rsp], xmm12
        movups    XMMWORD PTR [32+rsp], xmm11
        cmp       r10d, 2047
        je        _B10_9

_B10_2::

        ucomisd   xmm1, QWORD PTR [_vmldCbrtHATab+432]
        jp        _B10_3
        je        _B10_8

_B10_3::

        mov       al, r9b
        and       r9b, 127
        and       al, -128
        xor       r11d, r11d
        shr       al, 7
        movsd     QWORD PTR [80+rsp], xmm1
        movzx     edx, al
        mov       BYTE PTR [87+rsp], r9b
        movsd     xmm4, QWORD PTR [imagerel(_vmldCbrtHATab)+440+r12+rdx*8]
        test      r10d, r10d
        jne       _B10_5

_B10_4::

        movsd     xmm0, QWORD PTR [80+rsp]
        mov       r11d, 100
        mulsd     xmm0, QWORD PTR [_vmldCbrtHATab+360]
        movsd     QWORD PTR [80+rsp], xmm0
        jmp       _B10_6

_B10_5::

        movsd     xmm0, QWORD PTR [80+rsp]

_B10_6::

        movzx     r10d, WORD PTR [86+rsp]
        mov       eax, 1431655766
        and       r10d, 32752
        shr       r10d, 4
        imul      r10d
        mov       eax, 1431655766
        lea       r9d, DWORD PTR [rdx+rdx*2]
        neg       r9d
        add       r9d, r10d
        sub       r10d, r9d
        add       r9d, r9d
        add       r10d, -1023
        imul      r10d
        sar       r10d, 31
        sub       edx, r10d
        movsd     QWORD PTR [96+rsp], xmm0
        add       edx, 1023
        sub       edx, r11d
        movzx     r11d, WORD PTR [102+rsp]
        and       edx, 2047
        and       r11d, -32753
        add       r11d, 16368
        mov       WORD PTR [102+rsp], r11w
        movsd     xmm13, QWORD PTR [96+rsp]
        movaps    xmm3, xmm13
        mulsd     xmm3, QWORD PTR [_vmldCbrtHATab+376]
        movsd     QWORD PTR [104+rsp], xmm3
        movsd     xmm2, QWORD PTR [104+rsp]
        mov       eax, DWORD PTR [100+rsp]
        and       eax, 1048575
        subsd     xmm2, QWORD PTR [96+rsp]
        movsd     QWORD PTR [112+rsp], xmm2
        movsd     xmm12, QWORD PTR [104+rsp]
        movsd     xmm3, QWORD PTR [112+rsp]
        shr       eax, 15
        subsd     xmm12, xmm3
        movsd     QWORD PTR [104+rsp], xmm12
        movsd     xmm11, QWORD PTR [104+rsp]
        movsd     xmm5, QWORD PTR [imagerel(_vmldCbrtHATab)+r12+rax*8]
        subsd     xmm13, xmm11
        movaps    xmm0, xmm5
        mulsd     xmm0, xmm5
        movsd     QWORD PTR [112+rsp], xmm13
        movsd     xmm2, QWORD PTR [104+rsp]
        mulsd     xmm2, xmm0
        movsd     xmm3, QWORD PTR [112+rsp]
        mulsd     xmm3, xmm0
        movaps    xmm0, xmm5
        mulsd     xmm0, xmm2
        mulsd     xmm5, xmm3
        movsd     xmm1, QWORD PTR [_vmldCbrtHATab+440]
        movsd     xmm12, QWORD PTR [_vmldCbrtHATab+368]
        subsd     xmm1, xmm0
        mulsd     xmm12, xmm1
        movsd     QWORD PTR [104+rsp], xmm12
        movsd     xmm11, QWORD PTR [104+rsp]
        xorps     xmm5, XMMWORD PTR [_2il0floatpacket_104]
        subsd     xmm11, xmm1
        movsd     QWORD PTR [112+rsp], xmm11
        movsd     xmm0, QWORD PTR [104+rsp]
        movsd     xmm13, QWORD PTR [112+rsp]
        movsd     xmm11, QWORD PTR [_vmldCbrtHATab+256]
        subsd     xmm0, xmm13
        movsd     QWORD PTR [104+rsp], xmm0
        movsd     xmm12, QWORD PTR [104+rsp]
        movsd     xmm13, QWORD PTR [_vmldCbrtHATab+256]
        subsd     xmm1, xmm12
        movsd     QWORD PTR [112+rsp], xmm1
        movsd     xmm0, QWORD PTR [104+rsp]
        movsd     xmm1, QWORD PTR [112+rsp]
        movsd     xmm12, QWORD PTR [_vmldCbrtHATab+352]
        addsd     xmm1, xmm5
        movaps    xmm5, xmm0
        movsxd    r9, r9d
        addsd     xmm5, xmm1
        mulsd     xmm12, xmm5
        mov       rcx, QWORD PTR [_vmldCbrtHATab+440]
        mov       QWORD PTR [88+rsp], rcx
        shr       rcx, 48
        addsd     xmm12, QWORD PTR [_vmldCbrtHATab+344]
        mulsd     xmm12, xmm5
        and       ecx, -32753
        shl       edx, 4
        addsd     xmm12, QWORD PTR [_vmldCbrtHATab+336]
        mulsd     xmm12, xmm5
        or        ecx, edx
        mov       WORD PTR [94+rsp], cx
        addsd     xmm12, QWORD PTR [_vmldCbrtHATab+328]
        mulsd     xmm12, xmm5
        addsd     xmm12, QWORD PTR [_vmldCbrtHATab+320]
        mulsd     xmm12, xmm5
        addsd     xmm12, QWORD PTR [_vmldCbrtHATab+312]
        mulsd     xmm12, xmm5
        addsd     xmm12, QWORD PTR [_vmldCbrtHATab+304]
        mulsd     xmm12, xmm5
        addsd     xmm12, QWORD PTR [_vmldCbrtHATab+296]
        mulsd     xmm12, xmm5
        addsd     xmm12, QWORD PTR [_vmldCbrtHATab+288]
        mulsd     xmm12, xmm5
        addsd     xmm12, QWORD PTR [_vmldCbrtHATab+280]
        mulsd     xmm12, xmm5
        addsd     xmm12, QWORD PTR [_vmldCbrtHATab+272]
        mulsd     xmm12, xmm5
        addsd     xmm11, xmm12
        movsd     QWORD PTR [104+rsp], xmm11
        movsd     xmm5, QWORD PTR [104+rsp]
        subsd     xmm13, xmm5
        movsd     QWORD PTR [112+rsp], xmm13
        movsd     xmm11, QWORD PTR [104+rsp]
        movsd     xmm5, QWORD PTR [112+rsp]
        addsd     xmm11, xmm5
        movsd     xmm5, QWORD PTR [_vmldCbrtHATab+256]
        movsd     QWORD PTR [120+rsp], xmm11
        movsd     xmm13, QWORD PTR [112+rsp]
        addsd     xmm12, xmm13
        movsd     QWORD PTR [112+rsp], xmm12
        movsd     xmm12, QWORD PTR [120+rsp]
        subsd     xmm5, xmm12
        movsd     QWORD PTR [120+rsp], xmm5
        movsd     xmm11, QWORD PTR [112+rsp]
        movsd     xmm12, QWORD PTR [120+rsp]
        addsd     xmm11, xmm12
        movsd     xmm12, QWORD PTR [_vmldCbrtHATab+368]
        movsd     QWORD PTR [120+rsp], xmm11
        movsd     xmm5, QWORD PTR [104+rsp]
        mulsd     xmm12, xmm5
        movsd     xmm13, QWORD PTR [120+rsp]
        movsd     QWORD PTR [104+rsp], xmm12
        movsd     xmm11, QWORD PTR [104+rsp]
        subsd     xmm11, xmm5
        addsd     xmm13, QWORD PTR [_vmldCbrtHATab+264]
        movsd     QWORD PTR [112+rsp], xmm11
        movsd     xmm11, QWORD PTR [104+rsp]
        movsd     xmm12, QWORD PTR [112+rsp]
        subsd     xmm11, xmm12
        movsd     QWORD PTR [104+rsp], xmm11
        movsd     xmm12, QWORD PTR [104+rsp]
        subsd     xmm5, xmm12
        movsd     QWORD PTR [112+rsp], xmm5
        movaps    xmm5, xmm1
        movsd     xmm12, QWORD PTR [104+rsp]
        movsd     xmm11, QWORD PTR [112+rsp]
        mulsd     xmm1, xmm12
        addsd     xmm11, xmm13
        mulsd     xmm5, xmm11
        movaps    xmm13, xmm0
        mulsd     xmm0, xmm11
        mulsd     xmm13, xmm12
        addsd     xmm5, xmm1
        movsd     xmm1, QWORD PTR [_vmldCbrtHATab+368]
        addsd     xmm5, xmm0
        mulsd     xmm1, xmm13
        movsd     QWORD PTR [104+rsp], xmm5
        movsd     xmm5, QWORD PTR [104+rsp]
        movsd     QWORD PTR [104+rsp], xmm1
        movsd     xmm0, QWORD PTR [104+rsp]
        subsd     xmm0, xmm13
        movsd     QWORD PTR [112+rsp], xmm0
        movsd     xmm11, QWORD PTR [104+rsp]
        movsd     xmm12, QWORD PTR [112+rsp]
        subsd     xmm11, xmm12
        movsd     QWORD PTR [104+rsp], xmm11
        movsd     xmm1, QWORD PTR [104+rsp]
        subsd     xmm13, xmm1
        movsd     QWORD PTR [112+rsp], xmm13
        movsd     xmm0, QWORD PTR [104+rsp]
        movsd     xmm13, QWORD PTR [112+rsp]
        addsd     xmm5, xmm13
        movaps    xmm13, xmm0
        mulsd     xmm0, xmm3
        mulsd     xmm13, xmm2
        movaps    xmm12, xmm5
        mulsd     xmm12, xmm3
        mulsd     xmm5, xmm2
        addsd     xmm12, xmm0
        addsd     xmm12, xmm5
        movaps    xmm5, xmm13
        addsd     xmm5, xmm2
        movsd     QWORD PTR [104+rsp], xmm12
        movaps    xmm12, xmm2
        movsd     xmm0, QWORD PTR [104+rsp]
        movsd     QWORD PTR [104+rsp], xmm5
        movsd     xmm5, QWORD PTR [104+rsp]
        subsd     xmm12, xmm5
        movsd     QWORD PTR [112+rsp], xmm12
        movsd     xmm1, QWORD PTR [104+rsp]
        movsd     xmm11, QWORD PTR [112+rsp]
        addsd     xmm1, xmm11
        movsd     QWORD PTR [120+rsp], xmm1
        movsd     xmm5, QWORD PTR [112+rsp]
        movsd     xmm1, QWORD PTR [_vmldCbrtHATab+368]
        addsd     xmm13, xmm5
        movsd     QWORD PTR [112+rsp], xmm13
        movsd     xmm13, QWORD PTR [120+rsp]
        subsd     xmm2, xmm13
        movsd     QWORD PTR [120+rsp], xmm2
        movsd     xmm5, QWORD PTR [112+rsp]
        movsd     xmm2, QWORD PTR [120+rsp]
        addsd     xmm5, xmm2
        movsd     QWORD PTR [120+rsp], xmm5
        movsd     xmm11, QWORD PTR [104+rsp]
        mulsd     xmm1, xmm11
        movsd     xmm12, QWORD PTR [120+rsp]
        movsd     QWORD PTR [104+rsp], xmm1
        movsd     xmm13, QWORD PTR [104+rsp]
        subsd     xmm13, xmm11
        movsd     QWORD PTR [112+rsp], xmm13
        movsd     xmm5, QWORD PTR [104+rsp]
        movsd     xmm2, QWORD PTR [112+rsp]
        subsd     xmm5, xmm2
        movsd     QWORD PTR [104+rsp], xmm5
        movsd     xmm1, QWORD PTR [104+rsp]
        subsd     xmm11, xmm1
        movsd     QWORD PTR [112+rsp], xmm11
        movsd     xmm5, QWORD PTR [104+rsp]
        movsd     xmm11, QWORD PTR [112+rsp]
        movaps    xmm2, xmm5
        addsd     xmm12, xmm11
        addsd     xmm0, xmm12
        movsd     xmm12, QWORD PTR [imagerel(_vmldCbrtHATab)+384+r12+r9*8]
        mulsd     xmm2, xmm12
        addsd     xmm0, xmm3
        movsd     xmm3, QWORD PTR [imagerel(_vmldCbrtHATab)+392+r12+r9*8]
        movaps    xmm1, xmm3
        mulsd     xmm1, xmm0
        mulsd     xmm3, xmm5
        mulsd     xmm0, xmm12
        addsd     xmm1, xmm3
        addsd     xmm1, xmm0
        movsd     QWORD PTR [104+rsp], xmm1
        movsd     xmm0, QWORD PTR [104+rsp]
        addsd     xmm0, xmm2
        mulsd     xmm0, QWORD PTR [88+rsp]
        mulsd     xmm4, xmm0
        movsd     QWORD PTR [r8], xmm4

_B10_7::

        movups    xmm11, XMMWORD PTR [32+rsp]
        xor       eax, eax
        movups    xmm12, XMMWORD PTR [48+rsp]
        movups    xmm13, XMMWORD PTR [64+rsp]
        add       rsp, 128
        pop       r12
        ret

_B10_8::

        movsd     xmm0, QWORD PTR [_vmldCbrtHATab+440]
        mulsd     xmm1, xmm0
        movsd     QWORD PTR [r8], xmm1
        jmp       _B10_7

_B10_9::

        addsd     xmm1, xmm1
        movsd     QWORD PTR [r8], xmm1
        jmp       _B10_7
        ALIGN     16

_B10_10::

__svml_dcbrt_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_dcbrt_ha_cout_rare_internal_B1_B9:
	DD	540929
	DD	178241
	DD	247867
	DD	317493
	DD	3221680653

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B10_1
	DD	imagerel _B10_10
	DD	imagerel _unwind___svml_dcbrt_ha_cout_rare_internal_B1_B9

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ  'DATA'
	DD 8 DUP (0H)	
	PUBLIC __svml_dcbrt_ha_data_internal_avx512
__svml_dcbrt_ha_data_internal_avx512	DD	0
	DD	1072693248
	DD	4186796683
	DD	1072965794
	DD	2772266557
	DD	1073309182
	DD	0
	DD	0
	DD	0
	DD	3220176896
	DD	4186796683
	DD	3220449442
	DD	2772266557
	DD	3220792830
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1418634270
	DD	3162364962
	DD	2576690953
	DD	3164558313
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1418634270
	DD	1014881314
	DD	2576690953
	DD	1017074665
	DD	0
	DD	0
	DD	4186796683
	DD	1072965794
	DD	1554061055
	DD	1072914931
	DD	3992368458
	DD	1072871093
	DD	3714535808
	DD	1072832742
	DD	954824104
	DD	1072798779
	DD	3256858690
	DD	1072768393
	DD	3858344660
	DD	1072740974
	DD	1027250248
	DD	1072716050
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
	DD	1418634270
	DD	3162364962
	DD	629721892
	DD	1016287007
	DD	1776620500
	DD	3163956186
	DD	648592220
	DD	1016269578
	DD	1295766103
	DD	3161896715
	DD	1348094586
	DD	3164476360
	DD	2407028709
	DD	1015925873
	DD	497428409
	DD	1014435402
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
	DD	1431655766
	DD	1070945621
	DD	1431655766
	DD	1070945621
	DD	1431655766
	DD	1070945621
	DD	1431655766
	DD	1070945621
	DD	1431655766
	DD	1070945621
	DD	1431655766
	DD	1070945621
	DD	1431655766
	DD	1070945621
	DD	1431655766
	DD	1070945621
	DD	0
	DD	1126170624
	DD	0
	DD	1126170624
	DD	0
	DD	1126170624
	DD	0
	DD	1126170624
	DD	0
	DD	1126170624
	DD	0
	DD	1126170624
	DD	0
	DD	1126170624
	DD	0
	DD	1126170624
	DD	0
	DD	1074266112
	DD	0
	DD	1074266112
	DD	0
	DD	1074266112
	DD	0
	DD	1074266112
	DD	0
	DD	1074266112
	DD	0
	DD	1074266112
	DD	0
	DD	1074266112
	DD	0
	DD	1074266112
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
	DD	1792985698
	DD	3213372987
	DD	1792985698
	DD	3213372987
	DD	1792985698
	DD	3213372987
	DD	1792985698
	DD	3213372987
	DD	1792985698
	DD	3213372987
	DD	1792985698
	DD	3213372987
	DD	1792985698
	DD	3213372987
	DD	1792985698
	DD	3213372987
	DD	3135539317
	DD	1066129956
	DD	3135539317
	DD	1066129956
	DD	3135539317
	DD	1066129956
	DD	3135539317
	DD	1066129956
	DD	3135539317
	DD	1066129956
	DD	3135539317
	DD	1066129956
	DD	3135539317
	DD	1066129956
	DD	3135539317
	DD	1066129956
	DD	2087834975
	DD	3213899448
	DD	2087834975
	DD	3213899448
	DD	2087834975
	DD	3213899448
	DD	2087834975
	DD	3213899448
	DD	2087834975
	DD	3213899448
	DD	2087834975
	DD	3213899448
	DD	2087834975
	DD	3213899448
	DD	2087834975
	DD	3213899448
	DD	2476259604
	DD	1066628333
	DD	2476259604
	DD	1066628333
	DD	2476259604
	DD	1066628333
	DD	2476259604
	DD	1066628333
	DD	2476259604
	DD	1066628333
	DD	2476259604
	DD	1066628333
	DD	2476259604
	DD	1066628333
	DD	2476259604
	DD	1066628333
	DD	2012366478
	DD	3214412045
	DD	2012366478
	DD	3214412045
	DD	2012366478
	DD	3214412045
	DD	2012366478
	DD	3214412045
	DD	2012366478
	DD	3214412045
	DD	2012366478
	DD	3214412045
	DD	2012366478
	DD	3214412045
	DD	2012366478
	DD	3214412045
	DD	1104999785
	DD	1067378449
	DD	1104999785
	DD	1067378449
	DD	1104999785
	DD	1067378449
	DD	1104999785
	DD	1067378449
	DD	1104999785
	DD	1067378449
	DD	1104999785
	DD	1067378449
	DD	1104999785
	DD	1067378449
	DD	1104999785
	DD	1067378449
	DD	3534763582
	DD	3215266280
	DD	3534763582
	DD	3215266280
	DD	3534763582
	DD	3215266280
	DD	3534763582
	DD	3215266280
	DD	3534763582
	DD	3215266280
	DD	3534763582
	DD	3215266280
	DD	3534763582
	DD	3215266280
	DD	3534763582
	DD	3215266280
	DD	1007386161
	DD	1068473053
	DD	1007386161
	DD	1068473053
	DD	1007386161
	DD	1068473053
	DD	1007386161
	DD	1068473053
	DD	1007386161
	DD	1068473053
	DD	1007386161
	DD	1068473053
	DD	1007386161
	DD	1068473053
	DD	1007386161
	DD	1068473053
	DD	477218625
	DD	3216798151
	DD	477218625
	DD	3216798151
	DD	477218625
	DD	3216798151
	DD	477218625
	DD	3216798151
	DD	477218625
	DD	3216798151
	DD	477218625
	DD	3216798151
	DD	477218625
	DD	3216798151
	DD	477218625
	DD	3216798151
	DD	1431655767
	DD	1070945621
	DD	1431655767
	DD	1070945621
	DD	1431655767
	DD	1070945621
	DD	1431655767
	DD	1070945621
	DD	1431655767
	DD	1070945621
	DD	1431655767
	DD	1070945621
	DD	1431655767
	DD	1070945621
	DD	1431655767
	DD	1070945621
	PUBLIC __svml_dcbrt_ha_data_internal
__svml_dcbrt_ha_data_internal	DD	528611360
	DD	3220144632
	DD	2884679527
	DD	3220082993
	DD	1991868891
	DD	3220024928
	DD	2298714891
	DD	3219970134
	DD	58835168
	DD	3219918343
	DD	3035110223
	DD	3219869313
	DD	1617585086
	DD	3219822831
	DD	2500867033
	DD	3219778702
	DD	4241943008
	DD	3219736752
	DD	258732970
	DD	3219696825
	DD	404232216
	DD	3219658776
	DD	2172167368
	DD	3219622476
	DD	1544257904
	DD	3219587808
	DD	377579543
	DD	3219554664
	DD	1616385542
	DD	3219522945
	DD	813783277
	DD	3219492562
	DD	3940743189
	DD	3219463431
	DD	2689777499
	DD	3219435478
	DD	1700977147
	DD	3219408632
	DD	3169102082
	DD	3219382828
	DD	327235604
	DD	3219358008
	DD	1244336319
	DD	3219334115
	DD	1300311200
	DD	3219311099
	DD	3095471925
	DD	3219288912
	DD	2166487928
	DD	3219267511
	DD	2913108253
	DD	3219246854
	DD	293672978
	DD	3219226904
	DD	288737297
	DD	3219207624
	DD	1810275472
	DD	3219188981
	DD	174592167
	DD	3219170945
	DD	3539053052
	DD	3219153485
	DD	2164392968
	DD	3219136576
	DD	572345495
	DD	1072698681
	DD	831114197
	DD	1014426140
	DD	1998204467
	DD	1072709382
	DD	2676852344
	DD	1016444374
	DD	3861501553
	DD	1072719872
	DD	2448152898
	DD	1017271343
	DD	2268192434
	DD	1072730162
	DD	571097351
	DD	1016882671
	DD	2981979308
	DD	1072740260
	DD	4262088991
	DD	1013165739
	DD	270859143
	DD	1072750176
	DD	4129526850
	DD	1017541534
	DD	2958651392
	DD	1072759916
	DD	3359808165
	DD	1017217531
	DD	313113243
	DD	1072769490
	DD	483947492
	DD	1017266572
	DD	919449400
	DD	1072778903
	DD	3198298149
	DD	1017689293
	DD	2809328903
	DD	1072788162
	DD	427389337
	DD	1018030605
	DD	2222981587
	DD	1072797274
	DD	1956982634
	DD	1014089381
	DD	2352530781
	DD	1072806244
	DD	1432693183
	DD	1016413476
	DD	594152517
	DD	1072815078
	DD	4116689778
	DD	1016313239
	DD	1555767199
	DD	1072823780
	DD	4199324994
	DD	1014195323
	DD	4282421314
	DD	1072832355
	DD	1593346102
	DD	1017825413
	DD	2355578597
	DD	1072840809
	DD	1274262487
	DD	1016625594
	DD	1162590619
	DD	1072849145
	DD	334784993
	DD	1017096972
	DD	797864051
	DD	1072857367
	DD	568813162
	DD	1017168973
	DD	431273680
	DD	1072865479
	DD	3584554625
	DD	1017710626
	DD	2669831148
	DD	1072873484
	DD	3971702574
	DD	1017159584
	DD	733477752
	DD	1072881387
	DD	218128012
	DD	1018019915
	DD	4280220604
	DD	1072889189
	DD	75187235
	DD	1015924824
	DD	801961634
	DD	1072896896
	DD	3536670393
	DD	1016038493
	DD	2915370760
	DD	1072904508
	DD	839578387
	DD	1018086148
	DD	1159613482
	DD	1072912030
	DD	3759814061
	DD	1017352914
	DD	2689944798
	DD	1072919463
	DD	3143542556
	DD	1017717014
	DD	1248687822
	DD	1072926811
	DD	3006519911
	DD	1012519288
	DD	2967951030
	DD	1072934075
	DD	3521851764
	DD	1017830454
	DD	630170432
	DD	1072941259
	DD	1718375358
	DD	1017387255
	DD	3760898254
	DD	1072948363
	DD	85748770
	DD	1017604842
	DD	0
	DD	1072955392
	DD	0
	DD	0
	DD	2370273294
	DD	1072962345
	DD	1629859066
	DD	1013153035
	DD	1261754802
	DD	1072972640
	DD	113977743
	DD	1015673025
	DD	546334065
	DD	1072986123
	DD	1008348646
	DD	1016413877
	DD	1054893830
	DD	1072999340
	DD	2186484259
	DD	1018078778
	DD	1571187597
	DD	1073012304
	DD	1446107332
	DD	1015291210
	DD	1107975175
	DD	1073025027
	DD	69773316
	DD	1016481646
	DD	3606909377
	DD	1073037519
	DD	466887756
	DD	1017226688
	DD	1113616747
	DD	1073049792
	DD	3265289890
	DD	1017622569
	DD	4154744632
	DD	1073061853
	DD	2567589881
	DD	1015245005
	DD	3358931423
	DD	1073073713
	DD	343132434
	DD	1015771654
	DD	4060702372
	DD	1073085379
	DD	4013097658
	DD	1017904433
	DD	747576176
	DD	1073096860
	DD	1061470230
	DD	1014919738
	DD	3023138255
	DD	1073108161
	DD	1212724758
	DD	1018027762
	DD	1419988548
	DD	1073119291
	DD	3844345246
	DD	1014188976
	DD	1914185305
	DD	1073130255
	DD	303260851
	DD	1016023904
	DD	294389948
	DD	1073141060
	DD	3001102400
	DD	1016940338
	DD	3761802570
	DD	1073151710
	DD	41769798
	DD	1015538023
	DD	978281566
	DD	1073162213
	DD	1018481845
	DD	1017509529
	DD	823148820
	DD	1073172572
	DD	4070910954
	DD	1017208735
	DD	2420954441
	DD	1073182792
	DD	1209676399
	DD	1017337941
	DD	3815449908
	DD	1073192878
	DD	786936659
	DD	1017169506
	DD	2046058587
	DD	1073202835
	DD	3809068538
	DD	1017278211
	DD	1807524753
	DD	1073212666
	DD	2107909763
	DD	1017749298
	DD	2628681401
	DD	1073222375
	DD	638724993
	DD	1018143910
	DD	3225667357
	DD	1073231966
	DD	2846951590
	DD	1016668860
	DD	1555307421
	DD	1073241443
	DD	3233424021
	DD	1017889915
	DD	3454043099
	DD	1073250808
	DD	3181654301
	DD	1017329873
	DD	1208137896
	DD	1073260066
	DD	1842943805
	DD	1010293654
	DD	3659916772
	DD	1073269218
	DD	3049005729
	DD	1015940862
	DD	1886261264
	DD	1073278269
	DD	4113039774
	DD	1016737581
	DD	3593647839
	DD	1073287220
	DD	557134306
	DD	1017388941
	DD	3086012205
	DD	1073296075
	DD	2731128411
	DD	1017337242
	DD	2769796922
	DD	1073304836
	DD	3165647905
	DD	1017152170
	DD	888716057
	DD	1073317807
	DD	2912601025
	DD	1013105161
	DD	2201465623
	DD	1073334794
	DD	1739805577
	DD	1014678056
	DD	164369365
	DD	1073351447
	DD	563195703
	DD	1016553181
	DD	3462666733
	DD	1073367780
	DD	1021961283
	DD	1014782652
	DD	2773905457
	DD	1073383810
	DD	2309706734
	DD	1017182395
	DD	1342879088
	DD	1073399550
	DD	3777265738
	DD	1016157963
	DD	2543933975
	DD	1073415012
	DD	3047211052
	DD	1016886343
	DD	1684477781
	DD	1073430209
	DD	3956587805
	DD	1016191198
	DD	3532178543
	DD	1073445151
	DD	3446811632
	DD	1016964431
	DD	1147747300
	DD	1073459850
	DD	1052894694
	DD	1009560267
	DD	1928031793
	DD	1073474314
	DD	2233937521
	DD	1015025921
	DD	2079717015
	DD	1073488553
	DD	1170546599
	DD	1017990098
	DD	4016765315
	DD	1073502575
	DD	1501504581
	DD	1015834847
	DD	3670431139
	DD	1073516389
	DD	276476277
	DD	1015832488
	DD	3549227225
	DD	1073530002
	DD	3492674696
	DD	1017962706
	DD	11637607
	DD	1073543422
	DD	2597602399
	DD	1017405654
	DD	588220169
	DD	1073556654
	DD	2120795824
	DD	1017959346
	DD	2635407503
	DD	1073569705
	DD	229478739
	DD	1017912059
	DD	2042029317
	DD	1073582582
	DD	2247123382
	DD	1013256945
	DD	1925128962
	DD	1073595290
	DD	2868813801
	DD	1017582195
	DD	4136375664
	DD	1073607834
	DD	2715618536
	DD	1016959120
	DD	759964600
	DD	1073620221
	DD	3782520422
	DD	1017711264
	DD	4257606771
	DD	1073632453
	DD	3973324070
	DD	1015361972
	DD	297278907
	DD	1073644538
	DD	1732402144
	DD	1017675273
	DD	3655053093
	DD	1073656477
	DD	1558073476
	DD	1015468392
	DD	2442253172
	DD	1073668277
	DD	2080372965
	DD	1017517768
	DD	1111876799
	DD	1073679941
	DD	350080266
	DD	1017397321
	DD	3330973139
	DD	1073691472
	DD	2701328568
	DD	1016749585
	DD	3438879452
	DD	1073702875
	DD	2694623891
	DD	1017925557
	DD	3671565478
	DD	1073714153
	DD	2048173000
	DD	1016319877
	DD	1317849547
	DD	1073725310
	DD	180890
	DD	1017408398
	DD	1642364115
	DD	1073736348
	DD	1694598884
	DD	1017060392
	DD	1553778919
	DD	3213899486
	DD	1553778919
	DD	3213899486
	DD	1553778919
	DD	3213899486
	DD	1553778919
	DD	3213899486
	DD	1553778919
	DD	3213899486
	DD	1553778919
	DD	3213899486
	DD	1553778919
	DD	3213899486
	DD	1553778919
	DD	3213899486
	DD	3582521621
	DD	1066628362
	DD	3582521621
	DD	1066628362
	DD	3582521621
	DD	1066628362
	DD	3582521621
	DD	1066628362
	DD	3582521621
	DD	1066628362
	DD	3582521621
	DD	1066628362
	DD	3582521621
	DD	1066628362
	DD	3582521621
	DD	1066628362
	DD	1646371399
	DD	3214412045
	DD	1646371399
	DD	3214412045
	DD	1646371399
	DD	3214412045
	DD	1646371399
	DD	3214412045
	DD	1646371399
	DD	3214412045
	DD	1646371399
	DD	3214412045
	DD	1646371399
	DD	3214412045
	DD	1646371399
	DD	3214412045
	DD	889629714
	DD	1067378449
	DD	889629714
	DD	1067378449
	DD	889629714
	DD	1067378449
	DD	889629714
	DD	1067378449
	DD	889629714
	DD	1067378449
	DD	889629714
	DD	1067378449
	DD	889629714
	DD	1067378449
	DD	889629714
	DD	1067378449
	DD	3534952507
	DD	3215266280
	DD	3534952507
	DD	3215266280
	DD	3534952507
	DD	3215266280
	DD	3534952507
	DD	3215266280
	DD	3534952507
	DD	3215266280
	DD	3534952507
	DD	3215266280
	DD	3534952507
	DD	3215266280
	DD	3534952507
	DD	3215266280
	DD	1007461464
	DD	1068473053
	DD	1007461464
	DD	1068473053
	DD	1007461464
	DD	1068473053
	DD	1007461464
	DD	1068473053
	DD	1007461464
	DD	1068473053
	DD	1007461464
	DD	1068473053
	DD	1007461464
	DD	1068473053
	DD	1007461464
	DD	1068473053
	DD	477218588
	DD	3216798151
	DD	477218588
	DD	3216798151
	DD	477218588
	DD	3216798151
	DD	477218588
	DD	3216798151
	DD	477218588
	DD	3216798151
	DD	477218588
	DD	3216798151
	DD	477218588
	DD	3216798151
	DD	477218588
	DD	3216798151
	DD	1431655765
	DD	1070945621
	DD	1431655765
	DD	1070945621
	DD	1431655765
	DD	1070945621
	DD	1431655765
	DD	1070945621
	DD	1431655765
	DD	1070945621
	DD	1431655765
	DD	1070945621
	DD	1431655765
	DD	1070945621
	DD	1431655765
	DD	1070945621
	DD	0
	DD	3220193280
	DD	0
	DD	3220193280
	DD	0
	DD	3220193280
	DD	0
	DD	3220193280
	DD	0
	DD	3220193280
	DD	0
	DD	3220193280
	DD	0
	DD	3220193280
	DD	0
	DD	3220193280
	DD	0
	DD	1032192
	DD	0
	DD	1032192
	DD	0
	DD	1032192
	DD	0
	DD	1032192
	DD	0
	DD	1032192
	DD	0
	DD	1032192
	DD	0
	DD	1032192
	DD	0
	DD	1032192
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
	DD	4293918720
	DD	0
	DD	4293918720
	DD	0
	DD	4293918720
	DD	0
	DD	4293918720
	DD	0
	DD	4293918720
	DD	0
	DD	4293918720
	DD	0
	DD	4293918720
	DD	0
	DD	4293918720
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
	DD	5462
	DD	0
	DD	5462
	DD	0
	DD	5462
	DD	0
	DD	5462
	DD	0
	DD	5462
	DD	0
	DD	5462
	DD	0
	DD	5462
	DD	0
	DD	5462
	DD	0
	DD	1015808
	DD	1015808
	DD	1015808
	DD	1015808
	DD	1015808
	DD	1015808
	DD	1015808
	DD	1015808
	DD	1015808
	DD	1015808
	DD	1015808
	DD	1015808
	DD	1015808
	DD	1015808
	DD	1015808
	DD	1015808
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
	DD	2048
	DD	2048
	DD	2048
	DD	2048
	DD	2048
	DD	2048
	DD	2048
	DD	2048
	DD	2048
	DD	2048
	DD	2048
	DD	2048
	DD	2048
	DD	2048
	DD	2048
	DD	2048
	DD	682
	DD	682
	DD	682
	DD	682
	DD	682
	DD	682
	DD	682
	DD	682
	DD	682
	DD	682
	DD	682
	DD	682
	DD	682
	DD	682
	DD	682
	DD	682
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	2148532224
	DD	4292870143
	DD	4292870143
	DD	4292870143
	DD	4292870143
	DD	4292870143
	DD	4292870143
	DD	4292870143
	DD	4292870143
	DD	4292870143
	DD	4292870143
	DD	4292870143
	DD	4292870143
	DD	4292870143
	DD	4292870143
	DD	4292870143
	DD	4292870143
_vmldCbrtHATab	DD	0
	DD	1072693248
	DD	0
	DD	1072668672
	DD	0
	DD	1072644096
	DD	0
	DD	1072627712
	DD	0
	DD	1072611328
	DD	0
	DD	1072586752
	DD	0
	DD	1072570368
	DD	0
	DD	1072553984
	DD	0
	DD	1072537600
	DD	0
	DD	1072521216
	DD	0
	DD	1072504832
	DD	0
	DD	1072488448
	DD	0
	DD	1072480256
	DD	0
	DD	1072463872
	DD	0
	DD	1072447488
	DD	0
	DD	1072439296
	DD	0
	DD	1072422912
	DD	0
	DD	1072414720
	DD	0
	DD	1072398336
	DD	0
	DD	1072390144
	DD	0
	DD	1072373760
	DD	0
	DD	1072365568
	DD	0
	DD	1072357376
	DD	0
	DD	1072340992
	DD	0
	DD	1072332800
	DD	0
	DD	1072324608
	DD	0
	DD	1072308224
	DD	0
	DD	1072300032
	DD	0
	DD	1072291840
	DD	0
	DD	1072283648
	DD	0
	DD	1072275456
	DD	0
	DD	1072267264
	DD	1431655765
	DD	1071994197
	DD	1431655765
	DD	1015371093
	DD	1908874354
	DD	1071761180
	DD	1007461464
	DD	1071618781
	DD	565592401
	DD	1071446176
	DD	241555088
	DD	1071319599
	DD	943963244
	DD	1071221150
	DD	2330668378
	DD	1071141453
	DD	2770428108
	DD	1071075039
	DD	3622256836
	DD	1071018464
	DD	1497196870
	DD	1070969433
	DD	280472551
	DD	1070926345
	DD	1585032765
	DD	1070888044
	DD	0
	DD	1387266048
	DD	33554432
	DD	1101004800
	DD	512
	DD	1117782016
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	4160749568
	DD	1072965794
	DD	2921479643
	DD	1043912488
	DD	2684354560
	DD	1073309182
	DD	4060791142
	DD	1045755320
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	3220176896
	DD 2 DUP (0H)	
_2il0floatpacket_104	DD	000000000H,080000000H,000000000H,000000000H
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
