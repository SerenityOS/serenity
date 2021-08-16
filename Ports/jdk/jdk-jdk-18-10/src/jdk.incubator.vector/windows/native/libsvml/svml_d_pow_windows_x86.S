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
	PUBLIC __svml_pow2_ha_e9

__svml_pow2_ha_e9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 440
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [400+rsp], xmm15
        vmovups   XMMWORD PTR [256+rsp], xmm14
        vmovups   XMMWORD PTR [272+rsp], xmm13
        vmovups   XMMWORD PTR [288+rsp], xmm12
        vmovups   XMMWORD PTR [320+rsp], xmm11
        vmovups   XMMWORD PTR [304+rsp], xmm10
        vmovups   XMMWORD PTR [336+rsp], xmm9
        vmovups   XMMWORD PTR [352+rsp], xmm8
        vmovups   XMMWORD PTR [368+rsp], xmm7
        vmovups   XMMWORD PTR [384+rsp], xmm6
        vmovapd   xmm6, xmm1
        mov       QWORD PTR [416+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vpshufd   xmm5, xmm0, 221
        and       r13, -64
        vmovq     xmm12, QWORD PTR [__svml_dpow_ha_data_internal+28672]
        vmovq     xmm11, QWORD PTR [__svml_dpow_ha_data_internal+28736]
        vpand     xmm15, xmm5, xmm12
        vandpd    xmm14, xmm0, XMMWORD PTR [__svml_dpow_ha_data_internal+27520]
        vpaddd    xmm10, xmm15, xmm11
        vorpd     xmm13, xmm14, XMMWORD PTR [__svml_dpow_ha_data_internal+27648]
        vpsrld    xmm14, xmm10, 10
        vpslld    xmm12, xmm14, 3
        vpslld    xmm9, xmm14, 4
        vmovd     edx, xmm12
        vmovd     r8d, xmm9
        vmovq     xmm14, QWORD PTR [__svml_dpow_ha_data_internal+28800]
        vmovq     xmm4, QWORD PTR [__svml_dpow_ha_data_internal+28352]
        movsxd    rdx, edx
        vpaddd    xmm4, xmm5, xmm4
        vpextrd   ecx, xmm12, 1
        vpextrd   r9d, xmm9, 1
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movsxd    r9, r9d
        vmovsd    xmm15, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+rdx]
        vmovhpd   xmm7, xmm15, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+rcx]
        vpsubd    xmm15, xmm5, xmm14
        vmovupd   xmm8, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rax+r8]
        vpsrad    xmm9, xmm15, 20
        vmovupd   xmm10, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rax+r9]
        vunpcklpd xmm11, xmm8, xmm10
        vunpckhpd xmm12, xmm8, xmm10
        vmovq     xmm8, QWORD PTR [__svml_dpow_ha_data_internal+28864]
        vpaddd    xmm10, xmm9, xmm8
        vpshufd   xmm14, xmm10, 80
        vandpd    xmm15, xmm14, XMMWORD PTR [__svml_dpow_ha_data_internal+27712]
        vsubpd    xmm10, xmm15, XMMWORD PTR [__svml_dpow_ha_data_internal+27776]
        vmulpd    xmm14, xmm13, xmm7
        vaddpd    xmm11, xmm11, xmm10
        vmovupd   xmm15, XMMWORD PTR [__svml_dpow_ha_data_internal+27840]
        vandpd    xmm8, xmm13, xmm15
        vsubpd    xmm9, xmm13, xmm8
        vaddpd    xmm13, xmm14, XMMWORD PTR [__svml_dpow_ha_data_internal+27904]
        vmulpd    xmm8, xmm7, xmm8
        vmulpd    xmm7, xmm7, xmm9
        vsubpd    xmm14, xmm8, xmm14
        vaddpd    xmm9, xmm13, xmm11
        vaddpd    xmm8, xmm14, xmm7
        vsubpd    xmm14, xmm11, xmm9
        vaddpd    xmm7, xmm8, xmm9
        vaddpd    xmm11, xmm13, xmm14
        vsubpd    xmm10, xmm9, xmm7
        vaddpd    xmm14, xmm8, xmm10
        vaddpd    xmm9, xmm11, xmm14
        vaddpd    xmm14, xmm13, xmm8
        vaddpd    xmm9, xmm12, xmm9
        vmulpd    xmm13, xmm14, XMMWORD PTR [__svml_dpow_ha_data_internal+26816]
        vaddpd    xmm12, xmm13, XMMWORD PTR [__svml_dpow_ha_data_internal+26880]
        vmulpd    xmm13, xmm14, xmm12
        vaddpd    xmm8, xmm13, XMMWORD PTR [__svml_dpow_ha_data_internal+26944]
        vmulpd    xmm10, xmm14, xmm8
        vaddpd    xmm11, xmm10, XMMWORD PTR [__svml_dpow_ha_data_internal+27008]
        vmulpd    xmm12, xmm14, xmm11
        vaddpd    xmm13, xmm12, XMMWORD PTR [__svml_dpow_ha_data_internal+27072]
        vmulpd    xmm8, xmm14, xmm13
        vaddpd    xmm10, xmm8, XMMWORD PTR [__svml_dpow_ha_data_internal+27136]
        vmulpd    xmm14, xmm14, xmm10
        vaddpd    xmm9, xmm9, xmm14
        vaddpd    xmm13, xmm7, xmm9
        vsubpd    xmm7, xmm13, xmm7
        vsubpd    xmm14, xmm9, xmm7
        vandpd    xmm12, xmm13, xmm15
        vandpd    xmm7, xmm6, xmm15
        vmulpd    xmm15, xmm12, xmm7
        vsubpd    xmm8, xmm13, xmm12
        vsubpd    xmm9, xmm6, xmm7
        vmulpd    xmm7, xmm7, xmm8
        vmulpd    xmm12, xmm12, xmm9
        vmovq     xmm2, QWORD PTR [__svml_dpow_ha_data_internal+28416]
        vmovq     xmm1, QWORD PTR [__svml_dpow_ha_data_internal+28480]
        vpshufd   xmm3, xmm6, 221
        vpshufd   xmm11, xmm15, 221
        vpand     xmm3, xmm3, xmm1
        vpand     xmm11, xmm11, xmm1
        vpcmpgtd  xmm1, xmm2, xmm4
        vmovq     xmm2, QWORD PTR [__svml_dpow_ha_data_internal+28544]
        vpcmpgtd  xmm5, xmm3, xmm2
        vpcmpeqd  xmm4, xmm3, xmm2
        vpor      xmm2, xmm5, xmm4
        vmulpd    xmm4, xmm8, xmm9
        vmovq     xmm10, QWORD PTR [__svml_dpow_ha_data_internal+28608]
        vpor      xmm13, xmm1, xmm2
        vpcmpgtd  xmm3, xmm11, xmm10
        vpcmpeqd  xmm5, xmm11, xmm10
        vpor      xmm10, xmm3, xmm5
        vpor      xmm11, xmm13, xmm10
        vmovupd   xmm13, XMMWORD PTR [__svml_dpow_ha_data_internal+28032]
        vaddpd    xmm1, xmm13, xmm15
        vaddpd    xmm8, xmm4, xmm7
        vmulpd    xmm5, xmm6, xmm14
        vaddpd    xmm12, xmm8, xmm12
        vmovmskps edx, xmm11
        vpshufd   xmm4, xmm1, 136
        vmovq     xmm14, QWORD PTR [__svml_dpow_ha_data_internal+28992]
        vpslld    xmm2, xmm4, 13
        vpand     xmm9, xmm4, xmm14
        vpslld    xmm8, xmm9, 4
        vmovd     r10d, xmm8
        vmovq     xmm14, QWORD PTR [__svml_dpow_ha_data_internal+28928]
        vpaddd    xmm3, xmm2, xmm14
        vpextrd   r11d, xmm8, 1
        vsubpd    xmm1, xmm1, xmm13
        movsxd    r10, r10d
        movsxd    r11, r11d
        vpshufd   xmm7, xmm3, 80
        vsubpd    xmm15, xmm15, xmm1
        vmovupd   xmm14, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r10]
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r11]
        vunpcklpd xmm2, xmm14, xmm3
        vunpckhpd xmm3, xmm14, xmm3
        vaddpd    xmm14, xmm12, xmm5
        vaddpd    xmm14, xmm15, xmm14
        vmulpd    xmm10, xmm14, xmm14
        vmulpd    xmm1, xmm14, XMMWORD PTR [__svml_dpow_ha_data_internal+27200]
        vmulpd    xmm5, xmm10, xmm10
        vandpd    xmm4, xmm7, XMMWORD PTR [__svml_dpow_ha_data_internal+27968]
        vaddpd    xmm7, xmm1, XMMWORD PTR [__svml_dpow_ha_data_internal+27264]
        vmulpd    xmm8, xmm5, xmm7
        vmulpd    xmm9, xmm2, xmm8
        vaddpd    xmm13, xmm3, xmm9
        vmulpd    xmm3, xmm14, XMMWORD PTR [__svml_dpow_ha_data_internal+27328]
        vmulpd    xmm14, xmm14, XMMWORD PTR [__svml_dpow_ha_data_internal+27456]
        vaddpd    xmm11, xmm3, XMMWORD PTR [__svml_dpow_ha_data_internal+27392]
        vmulpd    xmm3, xmm2, xmm14
        vmulpd    xmm12, xmm10, xmm11
        vmulpd    xmm15, xmm2, xmm12
        vaddpd    xmm1, xmm13, xmm15
        vaddpd    xmm5, xmm1, xmm3
        vaddpd    xmm2, xmm2, xmm5
        mov       QWORD PTR [424+rsp], r13
        vmulpd    xmm1, xmm4, xmm2
        and       edx, 3
        jne       _B1_3

_B1_2::

        vmovups   xmm6, XMMWORD PTR [384+rsp]
        vmovapd   xmm0, xmm1
        vmovups   xmm7, XMMWORD PTR [368+rsp]
        vmovups   xmm8, XMMWORD PTR [352+rsp]
        vmovups   xmm9, XMMWORD PTR [336+rsp]
        vmovups   xmm10, XMMWORD PTR [304+rsp]
        vmovups   xmm11, XMMWORD PTR [320+rsp]
        vmovups   xmm12, XMMWORD PTR [288+rsp]
        vmovups   xmm13, XMMWORD PTR [272+rsp]
        vmovups   xmm14, XMMWORD PTR [256+rsp]
        vmovups   xmm15, XMMWORD PTR [400+rsp]
        mov       r13, QWORD PTR [416+rsp]
        add       rsp, 440
        ret

_B1_3::

        vmovupd   XMMWORD PTR [r13], xmm0
        vmovupd   XMMWORD PTR [64+r13], xmm6
        vmovupd   XMMWORD PTR [128+r13], xmm1
        je        _B1_2

_B1_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B1_7::

        bt        esi, ebx
        jc        _B1_10

_B1_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B1_7

_B1_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm1, XMMWORD PTR [128+r13]
        jmp       _B1_2

_B1_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]
        lea       r8, QWORD PTR [128+r13+rbx*8]

        call      __svml_dpow_ha_cout_rare_internal
        jmp       _B1_8
        ALIGN     16

_B1_11::

__svml_pow2_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow2_ha_e9_B1_B3:
	DD	1603585
	DD	3462264
	DD	1599596
	DD	1538147
	DD	1476698
	DD	1415249
	DD	1288264
	DD	1357887
	DD	1230902
	DD	1169453
	DD	1108004
	DD	1701915
	DD	3604747

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_pow2_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow2_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_pow2_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_6
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_pow2_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_pow8_ha_z0

__svml_pow8_ha_z0	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L28::

        sub       rsp, 1400
        vgetmantpd zmm27, zmm0, 10 {sae}
        vgetexppd zmm24, zmm0 {sae}
        vfpclasspd k0, zmm0, 223
        vrcp14pd  zmm28, zmm27
        kmovw     r8d, k0
        vrndscalepd zmm30, zmm28, 88 {sae}
        mov       QWORD PTR [1384+rsp], r13
        lea       r13, QWORD PTR [1183+rsp]
        vmovups   zmm25, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+768]
        and       r13, -64
        vmovups   zmm31, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+832]
        vmovups   zmm5, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512]
        vmovups   zmm2, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+128]
        vmovups   zmm4, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+256]
        vmovups   zmm3, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+384]
        vmovups   zmm28, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1152]
        vmulpd    zmm29, zmm27, zmm25 {rn-sae}
        vcmppd    k2, zmm30, zmm31, 17 {sae}
        vmovups   zmm27, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1024]
        vmovups   zmm31, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1536]
        vpsrlq    zmm22, zmm30, 47
        vfmsub231pd zmm25, zmm29, zmm30 {rn-sae}
        vmovups   zmm30, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1408]
        vpsllq    zmm23, zmm22, 59
        vpermt2pd zmm5, zmm22, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+64]
        vpermt2pd zmm2, zmm22, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+192]
        vpmovq2m  k3, zmm23
        vpermt2pd zmm4, zmm22, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+320]
        vpermt2pd zmm3, zmm22, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+448]
        vblendmpd zmm29{k3}, zmm5, zmm2
        vmulpd    zmm22, zmm25, zmm25 {rn-sae}
        vmovups   zmm23, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1088]
        vmovups   zmm2, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+960]
        vblendmpd zmm5{k3}, zmm4, zmm3
        vmovups   zmm4, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1344]
        vfmadd231pd zmm23, zmm27, zmm25 {rn-sae}
        vmovups   zmm27, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1600]
        vmovaps   zmm26, zmm1
        vmovups   zmm1, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+896]
        vfmadd231pd zmm27, zmm31, zmm25 {rn-sae}
        vmovups   zmm31, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1920]
        vaddpd    zmm24{k2}, zmm24, zmm1 {rn-sae}
        vfpclasspd k1, zmm26, 153
        vmovups   zmm1, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1216]
        vaddpd    zmm3, zmm29, zmm24 {rn-sae}
        vmovups   zmm24, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1280]
        vfmadd231pd zmm1, zmm28, zmm25 {rn-sae}
        kmovw     eax, k1
        vfmadd231pd zmm4, zmm24, zmm25 {rn-sae}
        vmovups   zmm24, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1472]
        vfmadd213pd zmm23, zmm22, zmm1 {rn-sae}
        vmulpd    zmm1, zmm22, zmm22 {rn-sae}
        vfmadd231pd zmm24, zmm30, zmm25 {rn-sae}
        vmovups   zmm30, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1792]
        vfmadd213pd zmm4, zmm22, zmm24 {rn-sae}
        vmovaps   zmm28, zmm3
        or        r8d, eax
        vfmadd231pd zmm28, zmm2, zmm25 {rn-sae}
        vfmadd213pd zmm23, zmm1, zmm4 {rn-sae}
        vsubpd    zmm3, zmm28, zmm3 {rn-sae}
        vfmadd213pd zmm23, zmm22, zmm27 {rn-sae}
        vfmsub213pd zmm2, zmm25, zmm3 {rn-sae}
        vfmadd213pd zmm23, zmm25, zmm5 {rn-sae}
        vfnmadd231pd zmm2, zmm2, zmm25 {rn-sae}
        vmovaps   zmm4, zmm3
        vfnmadd213pd zmm4, zmm25, zmm28 {rn-sae}
        vsubpd    zmm22, zmm28, zmm4 {rn-sae}
        vfmsub213pd zmm25, zmm3, zmm22 {rn-sae}
        vmovups   zmm22, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1664]
        vsubpd    zmm25, zmm2, zmm25 {rn-sae}
        vaddpd    zmm1, zmm23, zmm25 {rn-sae}
        vaddpd    zmm3, zmm4, zmm1 {rn-sae}
        vmulpd    zmm5, zmm3, zmm26 {rz-sae}
        vsubpd    zmm2, zmm3, zmm4 {rn-sae}
        vmovups   zmm4, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+512]
        vfmsub213pd zmm3, zmm26, zmm5 {rz-sae}
        vsubpd    zmm23, zmm1, zmm2 {rn-sae}
        vfmadd213pd zmm23, zmm26, zmm3 {rz-sae}
        vmovups   zmm3, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+640]
        vaddpd    zmm1, zmm5, zmm23 {rz-sae}
        vsubpd    zmm24, zmm1, zmm5 {rn-sae}
        vaddpd    zmm28, zmm1, zmm22 {rd-sae}
        vreducepd zmm25, zmm1, 65 {sae}
        vmovups   zmm5, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1856]
        vsubpd    zmm27, zmm23, zmm24 {rn-sae}
        vpermt2pd zmm4, zmm28, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+576]
        vpermt2pd zmm3, zmm28, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+704]
        vaddpd    zmm29, zmm25, zmm27 {rn-sae}
        vandpd    zmm2, zmm29, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1728]
        vmovups   zmm29, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+2112]
        vmulpd    zmm28, zmm2, zmm2 {rn-sae}
        vfmadd231pd zmm5, zmm30, zmm2 {rn-sae}
        vmovups   zmm30, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+1984]
        vfmadd231pd zmm30, zmm31, zmm2 {rn-sae}
        vmovups   zmm31, ZMMWORD PTR [__svml_dpow_ha_data_internal_avx512+2048]
        vfmadd213pd zmm5, zmm28, zmm30 {rn-sae}
        vfmadd231pd zmm29, zmm31, zmm2 {rn-sae}
        vfmadd213pd zmm5, zmm28, zmm29 {rn-sae}
        vfmadd213pd zmm5, zmm2, zmm3 {rn-sae}
        vfmadd213pd zmm5, zmm4, zmm4 {rn-sae}
        mov       QWORD PTR [1392+rsp], r13
        vscalefpd zmm1, zmm5, zmm1 {rn-sae}
        jne       _B2_3

_B2_2::

        mov       r13, QWORD PTR [1384+rsp]
        vmovaps   zmm0, zmm1
        add       rsp, 1400
        ret

_B2_3::

        vstmxcsr  DWORD PTR [1376+rsp]

_B2_4::

        movzx     edx, WORD PTR [1376+rsp]
        mov       eax, edx
        or        eax, 8064
        cmp       edx, eax
        je        _B2_6

_B2_5::

        mov       DWORD PTR [1376+rsp], eax
        vldmxcsr  DWORD PTR [1376+rsp]

_B2_6::

        vmovups   ZMMWORD PTR [r13], zmm0
        vmovups   ZMMWORD PTR [64+r13], zmm26
        vmovups   ZMMWORD PTR [128+r13], zmm1
        test      r8d, r8d
        jne       _B2_11

_B2_7::

        cmp       edx, eax
        je        _B2_2

_B2_8::

        vstmxcsr  DWORD PTR [1376+rsp]
        mov       eax, DWORD PTR [1376+rsp]

_B2_9::

        and       eax, -8065
        or        eax, edx
        mov       DWORD PTR [1376+rsp], eax
        vldmxcsr  DWORD PTR [1376+rsp]
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
        cmp       ebx, 8
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
        vmovups   zmm1, ZMMWORD PTR [128+r13]
        mov       rbx, QWORD PTR [1104+rsp]
        mov       edx, esi
        mov       rsi, QWORD PTR [1096+rsp]
        mov       rdi, QWORD PTR [1088+rsp]
        mov       rbp, QWORD PTR [1112+rsp]
        jmp       _B2_7

_B2_15::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]
        lea       r8, QWORD PTR [128+r13+rbx*8]

        call      __svml_dpow_ha_cout_rare_internal
        jmp       _B2_13
        ALIGN     16

_B2_16::

__svml_pow8_ha_z0 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow8_ha_z0_B1_B9:
	DD	276481
	DD	11392056
	DD	11469067

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_pow8_ha_z0_B1_B9

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow8_ha_z0_B11_B15:
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

	DD	imagerel _B2_11
	DD	imagerel _B2_16
	DD	imagerel _unwind___svml_pow8_ha_z0_B11_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_pow1_ha_ex

__svml_pow1_ha_ex	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L79::

        sub       rsp, 376
        mov       r8d, 1048064
        mov       r9d, 512
        mov       edx, 2147483647
        movups    XMMWORD PTR [288+rsp], xmm12
        mov       ecx, 2139095040
        movups    XMMWORD PTR [320+rsp], xmm10
        lea       rax, QWORD PTR [__ImageBase]
        pshufd    xmm10, xmm0, 85
        movd      xmm12, r8d
        pand      xmm12, xmm10
        movd      xmm5, r9d
        paddd     xmm12, xmm5
        movd      xmm2, ecx
        movups    XMMWORD PTR [256+rsp], xmm14
        movaps    xmm14, xmm1
        movd      xmm1, edx
        mov       edx, 1072168448
        movups    XMMWORD PTR [240+rsp], xmm15
        psrld     xmm12, 10
        movups    XMMWORD PTR [272+rsp], xmm13
        movdqa    xmm3, xmm12
        movups    XMMWORD PTR [304+rsp], xmm11
        mov       ecx, 1094189056
        movups    XMMWORD PTR [336+rsp], xmm9
        pslld     xmm3, 3
        movups    XMMWORD PTR [352+rsp], xmm8
        movd      xmm15, edx
        mov       QWORD PTR [232+rsp], r13
        pslld     xmm12, 4
        movsd     xmm9, QWORD PTR [__svml_dpow_ha_data_internal+27520]
        movd      xmm13, ecx
        movsd     xmm4, QWORD PTR [__svml_dpow_ha_data_internal+27648]
        andps     xmm9, xmm0
        movd      r10d, xmm3
        orps      xmm9, xmm4
        movdqa    xmm4, xmm10
        psubd     xmm4, xmm15
        movaps    xmm15, xmm9
        psrad     xmm4, 20
        mov       r9d, 1048576
        movsxd    r10, r10d
        paddd     xmm4, xmm13
        movsd     xmm8, QWORD PTR [__svml_dpow_ha_data_internal+27712]
        mov       edx, 2097152
        pshufd    xmm13, xmm4, 0
        mov       r8d, 1083129855
        movsd     xmm4, QWORD PTR [__svml_dpow_ha_data_internal+27840]
        andps     xmm13, xmm8
        movd      r11d, xmm12
        movaps    xmm5, xmm4
        andps     xmm5, xmm9
        subsd     xmm13, QWORD PTR [__svml_dpow_ha_data_internal+27776]
        mulsd     xmm9, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+r10]
        subsd     xmm15, xmm5
        mulsd     xmm5, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+r10]
        mulsd     xmm15, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+r10]
        subsd     xmm5, xmm9
        movsxd    r11, r11d
        movaps    xmm8, xmm9
        movsd     xmm9, QWORD PTR [__svml_dpow_ha_data_internal+26816]
        mov       ecx, 1072693248
        pshufd    xmm11, xmm14, 85
        lea       r13, QWORD PTR [95+rsp]
        pand      xmm11, xmm1
        and       r13, -64
        mov       QWORD PTR [368+rsp], r13
        addsd     xmm15, xmm5
        addsd     xmm13, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rax+r11]
        addsd     xmm8, QWORD PTR [__svml_dpow_ha_data_internal+27904]
        movaps    xmm3, xmm13
        movaps    xmm5, xmm4
        andps     xmm4, xmm14
        addsd     xmm3, xmm8
        movaps    xmm12, xmm3
        subsd     xmm13, xmm3
        addsd     xmm12, xmm15
        addsd     xmm13, xmm8
        addsd     xmm8, xmm15
        subsd     xmm3, xmm12
        mulsd     xmm9, xmm8
        addsd     xmm3, xmm15
        addsd     xmm9, QWORD PTR [__svml_dpow_ha_data_internal+26880]
        addsd     xmm3, xmm13
        mulsd     xmm9, xmm8
        addsd     xmm3, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8264+rax+r11]
        addsd     xmm9, QWORD PTR [__svml_dpow_ha_data_internal+26944]
        mulsd     xmm9, xmm8
        movd      xmm15, r8d
        mov       r8d, 127
        addsd     xmm9, QWORD PTR [__svml_dpow_ha_data_internal+27008]
        mulsd     xmm9, xmm8
        addsd     xmm9, QWORD PTR [__svml_dpow_ha_data_internal+27072]
        mulsd     xmm9, xmm8
        addsd     xmm9, QWORD PTR [__svml_dpow_ha_data_internal+27136]
        mulsd     xmm9, xmm8
        movaps    xmm8, xmm12
        addsd     xmm9, xmm3
        addsd     xmm8, xmm9
        movaps    xmm13, xmm8
        andps     xmm5, xmm8
        subsd     xmm13, xmm12
        movaps    xmm12, xmm14
        subsd     xmm12, xmm4
        subsd     xmm8, xmm5
        subsd     xmm9, xmm13
        movaps    xmm13, xmm4
        mulsd     xmm13, xmm5
        mulsd     xmm4, xmm8
        pshufd    xmm3, xmm13, 85
        pand      xmm3, xmm1
        movd      xmm1, r9d
        paddd     xmm10, xmm1
        movd      xmm1, edx
        pcmpgtd   xmm1, xmm10
        movdqa    xmm10, xmm11
        pcmpgtd   xmm10, xmm2
        pcmpeqd   xmm11, xmm2
        movdqa    xmm2, xmm3
        pcmpeqd   xmm3, xmm15
        pcmpgtd   xmm2, xmm15
        movaps    xmm15, xmm12
        mulsd     xmm15, xmm8
        movaps    xmm8, xmm13
        mulsd     xmm12, xmm5
        addsd     xmm4, xmm15
        movsd     xmm5, QWORD PTR [__svml_dpow_ha_data_internal+28032]
        por       xmm10, xmm11
        por       xmm1, xmm10
        movaps    xmm10, xmm14
        mulsd     xmm10, xmm9
        addsd     xmm8, xmm5
        addsd     xmm12, xmm4
        pshufd    xmm4, xmm8, 0
        por       xmm2, xmm3
        por       xmm1, xmm2
        movd      xmm3, r8d
        movmskps  edx, xmm1
        movdqa    xmm1, xmm4
        pand      xmm4, xmm3
        pslld     xmm1, 13
        movsd     xmm3, QWORD PTR [__svml_dpow_ha_data_internal+27200]
        movd      xmm9, ecx
        paddd     xmm1, xmm9
        pslld     xmm4, 4
        pshufd    xmm15, xmm1, 0
        and       edx, 1
        movsd     xmm1, QWORD PTR [__svml_dpow_ha_data_internal+27328]
        subsd     xmm8, xmm5
        addsd     xmm12, xmm10
        movd      r9d, xmm4
        subsd     xmm13, xmm8
        movsxd    r9, r9d
        addsd     xmm12, xmm13
        movaps    xmm13, xmm12
        mulsd     xmm13, xmm12
        mulsd     xmm3, xmm12
        mulsd     xmm1, xmm12
        addsd     xmm3, QWORD PTR [__svml_dpow_ha_data_internal+27264]
        addsd     xmm1, QWORD PTR [__svml_dpow_ha_data_internal+27392]
        movaps    xmm11, xmm13
        mulsd     xmm11, xmm13
        mulsd     xmm1, xmm13
        mulsd     xmm3, xmm11
        mulsd     xmm1, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r9]
        mulsd     xmm3, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r9]
        movsd     xmm2, QWORD PTR [__svml_dpow_ha_data_internal+27968]
        andps     xmm15, xmm2
        addsd     xmm3, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24712+rax+r9]
        movsd     xmm2, QWORD PTR [__svml_dpow_ha_data_internal+27456]
        addsd     xmm3, xmm1
        mulsd     xmm2, xmm12
        mulsd     xmm2, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r9]
        addsd     xmm3, xmm2
        addsd     xmm3, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r9]
        mulsd     xmm3, xmm15
        jne       _B3_3

_B3_2::

        movups    xmm8, XMMWORD PTR [352+rsp]
        movaps    xmm0, xmm3
        movups    xmm9, XMMWORD PTR [336+rsp]
        movups    xmm10, XMMWORD PTR [320+rsp]
        movups    xmm11, XMMWORD PTR [304+rsp]
        movups    xmm12, XMMWORD PTR [288+rsp]
        movups    xmm13, XMMWORD PTR [272+rsp]
        movups    xmm14, XMMWORD PTR [256+rsp]
        movups    xmm15, XMMWORD PTR [240+rsp]
        mov       r13, QWORD PTR [232+rsp]
        add       rsp, 376
        ret

_B3_3::

        movsd     QWORD PTR [r13], xmm0
        movsd     QWORD PTR [64+r13], xmm14
        movsd     QWORD PTR [128+r13], xmm3
        jne       _B3_6

_B3_4::

        movsd     xmm3, QWORD PTR [128+r13]
        jmp       _B3_2

_B3_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]
        lea       r8, QWORD PTR [128+r13]

        call      __svml_dpow_ha_cout_rare_internal
        jmp       _B3_4
        ALIGN     16

_B3_7::

__svml_pow1_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow1_ha_ex_B1_B6:
	DD	1359105
	DD	1955005
	DD	1476784
	DD	1415330
	DD	1292436
	DD	1169542
	DD	1046647
	DD	1108065
	DD	1353779
	DD	1230885
	DD	3080459

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_7
	DD	imagerel _unwind___svml_pow1_ha_ex_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_pow2_ha_ex

__svml_pow2_ha_ex	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L98::

        sub       rsp, 440
        lea       rax, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [272+rsp], xmm15
        movups    XMMWORD PTR [288+rsp], xmm14
        movups    XMMWORD PTR [304+rsp], xmm13
        movups    XMMWORD PTR [320+rsp], xmm12
        movups    XMMWORD PTR [336+rsp], xmm11
        movups    XMMWORD PTR [352+rsp], xmm10
        movaps    xmm10, xmm1
        movups    XMMWORD PTR [368+rsp], xmm9
        movups    XMMWORD PTR [384+rsp], xmm8
        movups    XMMWORD PTR [400+rsp], xmm7
        movups    XMMWORD PTR [256+rsp], xmm6
        mov       QWORD PTR [416+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        pshufd    xmm1, xmm0, 221
        and       r13, -64
        movq      xmm13, QWORD PTR [__svml_dpow_ha_data_internal+28672]
        movq      xmm11, QWORD PTR [__svml_dpow_ha_data_internal+28736]
        pand      xmm13, xmm1
        paddd     xmm13, xmm11
        psrld     xmm13, 10
        movdqa    xmm9, xmm13
        pslld     xmm13, 4
        movd      r8d, xmm13
        pslld     xmm9, 3
        pshufd    xmm7, xmm13, 1
        pshufd    xmm12, xmm9, 1
        movd      edx, xmm9
        movd      r9d, xmm7
        movups    xmm8, XMMWORD PTR [__svml_dpow_ha_data_internal+27520]
        movups    XMMWORD PTR [32+rsp], xmm0
        andps     xmm8, xmm0
        movd      ecx, xmm12
        movdqa    xmm12, xmm1
        movq      xmm0, QWORD PTR [__svml_dpow_ha_data_internal+28800]
        psubd     xmm12, xmm0
        movsxd    r8, r8d
        psrad     xmm12, 20
        movsxd    rdx, edx
        movsxd    r9, r9d
        movsxd    rcx, ecx
        movq      xmm11, QWORD PTR [__svml_dpow_ha_data_internal+28864]
        movups    xmm9, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rax+r8]
        paddd     xmm12, xmm11
        movups    xmm7, XMMWORD PTR [__svml_dpow_ha_data_internal+27840]
        movaps    xmm13, xmm9
        orps      xmm8, XMMWORD PTR [__svml_dpow_ha_data_internal+27648]
        movsd     xmm14, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+rdx]
        movups    xmm15, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rax+r9]
        pshufd    xmm0, xmm12, 80
        movaps    xmm12, xmm7
        movhpd    xmm14, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+rcx]
        andps     xmm12, xmm8
        unpcklpd  xmm13, xmm15
        unpckhpd  xmm9, xmm15
        movaps    xmm15, xmm8
        subpd     xmm15, xmm12
        mulpd     xmm8, xmm14
        mulpd     xmm12, xmm14
        mulpd     xmm14, xmm15
        subpd     xmm12, xmm8
        andps     xmm0, XMMWORD PTR [__svml_dpow_ha_data_internal+27712]
        movaps    xmm15, xmm7
        subpd     xmm0, XMMWORD PTR [__svml_dpow_ha_data_internal+27776]
        addpd     xmm12, xmm14
        addpd     xmm13, xmm0
        movups    xmm11, XMMWORD PTR [__svml_dpow_ha_data_internal+27904]
        andps     xmm7, xmm10
        addpd     xmm11, xmm8
        movaps    xmm14, xmm11
        movaps    xmm8, xmm12
        addpd     xmm14, xmm13
        subpd     xmm13, xmm14
        addpd     xmm8, xmm14
        addpd     xmm13, xmm11
        addpd     xmm11, xmm12
        subpd     xmm14, xmm8
        movups    xmm0, XMMWORD PTR [__svml_dpow_ha_data_internal+26816]
        mulpd     xmm0, xmm11
        addpd     xmm14, xmm12
        addpd     xmm0, XMMWORD PTR [__svml_dpow_ha_data_internal+26880]
        addpd     xmm13, xmm14
        mulpd     xmm0, xmm11
        addpd     xmm9, xmm13
        addpd     xmm0, XMMWORD PTR [__svml_dpow_ha_data_internal+26944]
        mulpd     xmm0, xmm11
        addpd     xmm0, XMMWORD PTR [__svml_dpow_ha_data_internal+27008]
        mulpd     xmm0, xmm11
        addpd     xmm0, XMMWORD PTR [__svml_dpow_ha_data_internal+27072]
        mulpd     xmm0, xmm11
        addpd     xmm0, XMMWORD PTR [__svml_dpow_ha_data_internal+27136]
        mulpd     xmm11, xmm0
        addpd     xmm9, xmm11
        movaps    xmm0, xmm8
        movaps    xmm14, xmm10
        addpd     xmm0, xmm9
        subpd     xmm14, xmm7
        movaps    xmm11, xmm0
        andps     xmm15, xmm0
        subpd     xmm11, xmm8
        subpd     xmm0, xmm15
        subpd     xmm9, xmm11
        movaps    xmm8, xmm15
        movaps    xmm12, xmm0
        mulpd     xmm8, xmm7
        mulpd     xmm12, xmm14
        mulpd     xmm7, xmm0
        mulpd     xmm15, xmm14
        addpd     xmm12, xmm7
        mulpd     xmm9, xmm10
        addpd     xmm12, xmm15
        movq      xmm5, QWORD PTR [__svml_dpow_ha_data_internal+28480]
        pshufd    xmm3, xmm10, 221
        movq      xmm2, QWORD PTR [__svml_dpow_ha_data_internal+28352]
        pand      xmm3, xmm5
        movq      xmm6, QWORD PTR [__svml_dpow_ha_data_internal+28544]
        paddd     xmm1, xmm2
        pshufd    xmm13, xmm8, 221
        movdqa    xmm2, xmm3
        pand      xmm13, xmm5
        pcmpgtd   xmm2, xmm6
        pcmpeqd   xmm3, xmm6
        movq      xmm5, QWORD PTR [__svml_dpow_ha_data_internal+28608]
        por       xmm2, xmm3
        movq      xmm4, QWORD PTR [__svml_dpow_ha_data_internal+28416]
        movdqa    xmm3, xmm13
        pcmpgtd   xmm4, xmm1
        pcmpgtd   xmm3, xmm5
        pcmpeqd   xmm13, xmm5
        por       xmm4, xmm2
        por       xmm3, xmm13
        por       xmm4, xmm3
        movups    xmm3, XMMWORD PTR [__svml_dpow_ha_data_internal+28032]
        movaps    xmm5, xmm3
        addpd     xmm5, xmm8
        addpd     xmm12, xmm9
        movmskps  edx, xmm4
        pshufd    xmm0, xmm5, 136
        subpd     xmm5, xmm3
        subpd     xmm8, xmm5
        addpd     xmm8, xmm12
        movaps    xmm7, xmm8
        and       edx, 3
        mulpd     xmm7, xmm8
        movups    xmm9, XMMWORD PTR [__svml_dpow_ha_data_internal+27200]
        movaps    xmm12, xmm7
        mulpd     xmm9, xmm8
        mulpd     xmm12, xmm7
        addpd     xmm9, XMMWORD PTR [__svml_dpow_ha_data_internal+27264]
        movups    xmm6, XMMWORD PTR [__svml_dpow_ha_data_internal+27328]
        mulpd     xmm6, xmm8
        mulpd     xmm12, xmm9
        addpd     xmm6, XMMWORD PTR [__svml_dpow_ha_data_internal+27392]
        movq      xmm1, QWORD PTR [__svml_dpow_ha_data_internal+28992]
        pand      xmm1, xmm0
        pslld     xmm0, 13
        movq      xmm4, QWORD PTR [__svml_dpow_ha_data_internal+28928]
        pslld     xmm1, 4
        movd      r10d, xmm1
        paddd     xmm0, xmm4
        pshufd    xmm4, xmm1, 1
        movups    xmm13, XMMWORD PTR [__svml_dpow_ha_data_internal+27456]
        movd      r11d, xmm4
        pshufd    xmm0, xmm0, 80
        andps     xmm0, XMMWORD PTR [__svml_dpow_ha_data_internal+27968]
        movsxd    r10, r10d
        movsxd    r11, r11d
        mulpd     xmm7, xmm6
        mulpd     xmm13, xmm8
        movups    xmm11, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r10]
        movups    xmm2, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r11]
        movaps    xmm14, xmm11
        unpcklpd  xmm14, xmm2
        mulpd     xmm12, xmm14
        mulpd     xmm7, xmm14
        mulpd     xmm13, xmm14
        unpckhpd  xmm11, xmm2
        addpd     xmm11, xmm12
        addpd     xmm11, xmm7
        addpd     xmm11, xmm13
        addpd     xmm14, xmm11
        mov       QWORD PTR [424+rsp], r13
        mulpd     xmm0, xmm14
        jne       _B4_3

_B4_2::

        movups    xmm6, XMMWORD PTR [256+rsp]
        movups    xmm7, XMMWORD PTR [400+rsp]
        movups    xmm8, XMMWORD PTR [384+rsp]
        movups    xmm9, XMMWORD PTR [368+rsp]
        movups    xmm10, XMMWORD PTR [352+rsp]
        movups    xmm11, XMMWORD PTR [336+rsp]
        movups    xmm12, XMMWORD PTR [320+rsp]
        movups    xmm13, XMMWORD PTR [304+rsp]
        movups    xmm14, XMMWORD PTR [288+rsp]
        movups    xmm15, XMMWORD PTR [272+rsp]
        mov       r13, QWORD PTR [416+rsp]
        add       rsp, 440
        ret

_B4_3::

        movups    xmm1, XMMWORD PTR [32+rsp]
        movups    XMMWORD PTR [r13], xmm1
        movups    XMMWORD PTR [64+r13], xmm10
        movups    XMMWORD PTR [128+r13], xmm0
        je        _B4_2

_B4_6::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B4_7::

        mov       ecx, ebx
        mov       eax, 1
        shl       eax, cl
        test      esi, eax
        jne       _B4_10

_B4_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B4_7

_B4_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [128+r13]
        jmp       _B4_2

_B4_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]
        lea       r8, QWORD PTR [128+r13+rbx*8]

        call      __svml_dpow_ha_cout_rare_internal
        jmp       _B4_8
        ALIGN     16

_B4_11::

__svml_pow2_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow2_ha_ex_B1_B3:
	DD	1603073
	DD	3462262
	DD	1075310
	DD	1669222
	DD	1607774
	DD	1546325
	DD	1484872
	DD	1423423
	DD	1361974
	DD	1300525
	DD	1239076
	DD	1177627
	DD	3604747

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_pow2_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow2_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_pow2_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_6
	DD	imagerel _B4_11
	DD	imagerel _unwind___svml_pow2_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_pow4_ha_e9

__svml_pow4_ha_e9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L127::

        sub       rsp, 616
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [464+rsp], ymm6
        vmovups   YMMWORD PTR [400+rsp], ymm15
        vmovups   YMMWORD PTR [432+rsp], ymm14
        vmovups   YMMWORD PTR [528+rsp], ymm13
        vmovups   YMMWORD PTR [560+rsp], ymm12
        vmovups   YMMWORD PTR [304+rsp], ymm11
        vmovups   YMMWORD PTR [496+rsp], ymm9
        vmovups   YMMWORD PTR [368+rsp], ymm8
        vmovups   YMMWORD PTR [336+rsp], ymm7
        vmovapd   ymm6, ymm1
        mov       QWORD PTR [592+rsp], r13
        lea       r13, QWORD PTR [143+rsp]
        vmovups   xmm4, XMMWORD PTR [__svml_dpow_ha_data_internal+28416]
        and       r13, -64
        vmovups   xmm1, XMMWORD PTR [__svml_dpow_ha_data_internal+28544]
        mov       QWORD PTR [600+rsp], r13
        vextractf128 xmm5, ymm0, 1
        vshufps   xmm15, xmm0, xmm5, 221
        vmovups   xmm5, XMMWORD PTR [__svml_dpow_ha_data_internal+28480]
        vandpd    ymm2, ymm0, YMMWORD PTR [__svml_dpow_ha_data_internal+27520]
        vorpd     ymm7, ymm2, YMMWORD PTR [__svml_dpow_ha_data_internal+27648]
        vextractf128 xmm3, ymm6, 1
        vshufps   xmm8, xmm6, xmm3, 221
        vpaddd    xmm3, xmm15, XMMWORD PTR [__svml_dpow_ha_data_internal+28352]
        vpcmpgtd  xmm4, xmm4, xmm3
        vpand     xmm3, xmm8, xmm5
        vandps    xmm8, xmm15, XMMWORD PTR [__svml_dpow_ha_data_internal+28672]
        vpaddd    xmm13, xmm8, XMMWORD PTR [__svml_dpow_ha_data_internal+28736]
        vpsrld    xmm2, xmm13, 10
        vpslld    xmm14, xmm2, 3
        vpslld    xmm2, xmm2, 4
        vmovd     eax, xmm14
        vmovd     r10d, xmm2
        vpsubd    xmm15, xmm15, XMMWORD PTR [__svml_dpow_ha_data_internal+28800]
        movsxd    rax, eax
        vpextrd   ecx, xmm14, 1
        movsxd    rcx, ecx
        vpextrd   r8d, xmm14, 2
        vpextrd   r11d, xmm2, 1
        vmovsd    xmm9, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rdx+rax]
        movsxd    r8, r8d
        vpextrd   r9d, xmm14, 3
        movsxd    r10, r10d
        movsxd    r11, r11d
        vmovhpd   xmm12, xmm9, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rdx+rcx]
        movsxd    r9, r9d
        vpextrd   eax, xmm2, 2
        vpextrd   ecx, xmm2, 3
        movsxd    rax, eax
        movsxd    rcx, ecx
        vmovsd    xmm11, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rdx+r8]
        vmovupd   xmm13, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rdx+r10]
        vmovupd   xmm14, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rdx+r11]
        vmovhpd   xmm8, xmm11, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rdx+r9]
        vinsertf128 ymm9, ymm12, xmm8, 1
        vpsrad    xmm8, xmm15, 20
        vinsertf128 ymm2, ymm13, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rdx+rax], 1
        vinsertf128 ymm11, ymm14, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rdx+rcx], 1
        vunpcklpd ymm12, ymm2, ymm11
        vunpckhpd ymm14, ymm2, ymm11
        vpaddd    xmm2, xmm8, XMMWORD PTR [__svml_dpow_ha_data_internal+28864]
        vpshufd   xmm13, xmm2, 80
        vpshufd   xmm15, xmm2, 250
        vmovupd   ymm2, YMMWORD PTR [__svml_dpow_ha_data_internal+27840]
        vinsertf128 ymm11, ymm13, xmm15, 1
        vandpd    ymm8, ymm11, YMMWORD PTR [__svml_dpow_ha_data_internal+27712]
        vandpd    ymm11, ymm7, ymm2
        vsubpd    ymm13, ymm8, YMMWORD PTR [__svml_dpow_ha_data_internal+27776]
        vsubpd    ymm8, ymm7, ymm11
        vmulpd    ymm7, ymm7, ymm9
        vmulpd    ymm11, ymm9, ymm11
        vmulpd    ymm9, ymm9, ymm8
        vaddpd    ymm15, ymm7, YMMWORD PTR [__svml_dpow_ha_data_internal+27904]
        vsubpd    ymm7, ymm11, ymm7
        vaddpd    ymm13, ymm12, ymm13
        vaddpd    ymm8, ymm7, ymm9
        vaddpd    ymm12, ymm15, ymm13
        vaddpd    ymm7, ymm8, ymm12
        vsubpd    ymm9, ymm13, ymm12
        vsubpd    ymm11, ymm12, ymm7
        vaddpd    ymm13, ymm15, ymm9
        vaddpd    ymm9, ymm8, ymm11
        vaddpd    ymm12, ymm13, ymm9
        vaddpd    ymm11, ymm14, ymm12
        vaddpd    ymm14, ymm15, ymm8
        vmulpd    ymm8, ymm14, YMMWORD PTR [__svml_dpow_ha_data_internal+26816]
        vaddpd    ymm15, ymm8, YMMWORD PTR [__svml_dpow_ha_data_internal+26880]
        vmulpd    ymm8, ymm14, ymm15
        vaddpd    ymm13, ymm8, YMMWORD PTR [__svml_dpow_ha_data_internal+26944]
        vmulpd    ymm9, ymm14, ymm13
        vaddpd    ymm12, ymm9, YMMWORD PTR [__svml_dpow_ha_data_internal+27008]
        vmulpd    ymm15, ymm14, ymm12
        vaddpd    ymm8, ymm15, YMMWORD PTR [__svml_dpow_ha_data_internal+27072]
        vmulpd    ymm13, ymm14, ymm8
        vaddpd    ymm9, ymm13, YMMWORD PTR [__svml_dpow_ha_data_internal+27136]
        vmulpd    ymm14, ymm14, ymm9
        vaddpd    ymm8, ymm11, ymm14
        vaddpd    ymm13, ymm7, ymm8
        vandpd    ymm12, ymm13, ymm2
        vsubpd    ymm7, ymm13, ymm7
        vsubpd    ymm9, ymm13, ymm12
        vsubpd    ymm15, ymm8, ymm7
        vandpd    ymm13, ymm6, ymm2
        vmulpd    ymm2, ymm12, ymm13
        vsubpd    ymm8, ymm6, ymm13
        vmulpd    ymm13, ymm13, ymm9
        vextractf128 xmm14, ymm2, 1
        vshufps   xmm7, xmm2, xmm14, 221
        vmulpd    ymm14, ymm9, ymm8
        vpand     xmm11, xmm7, xmm5
        vmulpd    ymm8, ymm12, ymm8
        vpcmpgtd  xmm5, xmm3, xmm1
        vpcmpeqd  xmm1, xmm3, xmm1
        vmovups   xmm7, XMMWORD PTR [__svml_dpow_ha_data_internal+28608]
        vpor      xmm5, xmm5, xmm1
        vpor      xmm3, xmm4, xmm5
        vpcmpgtd  xmm4, xmm11, xmm7
        vpcmpeqd  xmm5, xmm11, xmm7
        vaddpd    ymm9, ymm14, ymm13
        vpor      xmm4, xmm4, xmm5
        vpor      xmm1, xmm3, xmm4
        vmulpd    ymm13, ymm6, ymm15
        vaddpd    ymm8, ymm9, ymm8
        vmovmskps eax, xmm1
        vmovupd   ymm9, YMMWORD PTR [__svml_dpow_ha_data_internal+28032]
        vaddpd    ymm12, ymm9, ymm2
        vextractf128 xmm7, ymm12, 1
        vsubpd    ymm9, ymm12, ymm9
        vshufps   xmm15, xmm12, xmm7, 136
        vandps    xmm11, xmm15, XMMWORD PTR [__svml_dpow_ha_data_internal+28992]
        vpslld    xmm5, xmm15, 13
        vpslld    xmm14, xmm11, 4
        vmovd     r8d, xmm14
        vpaddd    xmm4, xmm5, XMMWORD PTR [__svml_dpow_ha_data_internal+28928]
        vpshufd   xmm3, xmm4, 80
        vpextrd   r9d, xmm14, 1
        movsxd    r8, r8d
        movsxd    r9, r9d
        vpextrd   r10d, xmm14, 2
        vpextrd   ecx, xmm14, 3
        movsxd    r10, r10d
        movsxd    rcx, ecx
        vmovupd   xmm11, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rdx+r8]
        vmovupd   xmm15, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rdx+r9]
        vpshufd   xmm1, xmm4, 250
        vsubpd    ymm2, ymm2, ymm9
        vinsertf128 ymm14, ymm11, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rdx+r10], 1
        vinsertf128 ymm4, ymm15, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rdx+rcx], 1
        vinsertf128 ymm7, ymm3, xmm1, 1
        vunpcklpd ymm3, ymm14, ymm4
        vunpckhpd ymm1, ymm14, ymm4
        vaddpd    ymm4, ymm8, ymm13
        vaddpd    ymm4, ymm2, ymm4
        vmulpd    ymm12, ymm4, ymm4
        vmulpd    ymm2, ymm4, YMMWORD PTR [__svml_dpow_ha_data_internal+27200]
        vandpd    ymm5, ymm7, YMMWORD PTR [__svml_dpow_ha_data_internal+27968]
        vmulpd    ymm7, ymm12, ymm12
        vaddpd    ymm8, ymm2, YMMWORD PTR [__svml_dpow_ha_data_internal+27264]
        vmulpd    ymm9, ymm7, ymm8
        vmulpd    ymm11, ymm3, ymm9
        vaddpd    ymm15, ymm1, ymm11
        vmulpd    ymm1, ymm4, YMMWORD PTR [__svml_dpow_ha_data_internal+27328]
        vmulpd    ymm4, ymm4, YMMWORD PTR [__svml_dpow_ha_data_internal+27456]
        vaddpd    ymm13, ymm1, YMMWORD PTR [__svml_dpow_ha_data_internal+27392]
        vmulpd    ymm7, ymm3, ymm4
        vmulpd    ymm14, ymm12, ymm13
        vmulpd    ymm1, ymm3, ymm14
        vaddpd    ymm2, ymm15, ymm1
        vaddpd    ymm8, ymm2, ymm7
        vaddpd    ymm3, ymm3, ymm8
        vmulpd    ymm1, ymm5, ymm3
        test      eax, eax
        jne       _B5_3

_B5_2::

        vmovups   ymm6, YMMWORD PTR [464+rsp]
        vmovups   ymm7, YMMWORD PTR [336+rsp]
        vmovups   ymm8, YMMWORD PTR [368+rsp]
        vmovups   ymm9, YMMWORD PTR [496+rsp]
        vmovups   ymm11, YMMWORD PTR [304+rsp]
        vmovups   ymm12, YMMWORD PTR [560+rsp]
        vmovups   ymm13, YMMWORD PTR [528+rsp]
        vmovups   ymm14, YMMWORD PTR [432+rsp]
        vmovups   ymm15, YMMWORD PTR [400+rsp]
        mov       r13, QWORD PTR [592+rsp]
        vmovapd   ymm0, ymm1
        add       rsp, 616
        ret

_B5_3::

        vmovupd   YMMWORD PTR [r13], ymm0
        vmovupd   YMMWORD PTR [64+r13], ymm6
        vmovupd   YMMWORD PTR [128+r13], ymm1

_B5_6::

        xor       edx, edx
        vmovups   YMMWORD PTR [32+rsp], ymm10
        mov       QWORD PTR [72+rsp], rbx
        mov       ebx, edx
        mov       QWORD PTR [64+rsp], rsi
        mov       esi, eax

_B5_7::

        bt        esi, ebx
        jc        _B5_10

_B5_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B5_7

_B5_9::

        vmovups   ymm10, YMMWORD PTR [32+rsp]
        vmovupd   ymm1, YMMWORD PTR [128+r13]
        mov       rbx, QWORD PTR [72+rsp]
        mov       rsi, QWORD PTR [64+rsp]
        jmp       _B5_2

_B5_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]
        lea       r8, QWORD PTR [128+r13+rbx*8]

        call      __svml_dpow_ha_cout_rare_internal
        jmp       _B5_8
        ALIGN     16

_B5_11::

__svml_pow4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow4_ha_e9_B1_B3:
	DD	1470209
	DD	4904047
	DD	1407075
	DD	1542234
	DD	2070609
	DD	1292360
	DD	2345023
	DD	2218038
	DD	1828909
	DD	1701924
	DD	1927195
	DD	5046539

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_pow4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow4_ha_e9_B6_B10:
	DD	398369
	DD	549908
	DD	603149
	DD	174088
	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_pow4_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_6
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_pow4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_pow1_ha_e9

__svml_pow1_ha_e9	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L154::

        sub       rsp, 376
        mov       edx, 2147483647
        mov       r8d, 1048064
        mov       r9d, 512
        mov       ecx, 2139095040
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [272+rsp], xmm9
        vmovd     xmm9, edx
        mov       edx, 1072168448
        vmovups   XMMWORD PTR [336+rsp], xmm8
        vmovd     xmm8, r8d
        vpshufd   xmm3, xmm0, 85
        mov       r8d, 1083129855
        vmovups   XMMWORD PTR [288+rsp], xmm15
        vmovups   XMMWORD PTR [304+rsp], xmm14
        vpand     xmm14, xmm3, xmm8
        vmovups   XMMWORD PTR [320+rsp], xmm13
        vmovd     xmm8, edx
        vmovups   XMMWORD PTR [352+rsp], xmm12
        vmovd     xmm12, r9d
        vmovups   XMMWORD PTR [256+rsp], xmm7
        vmovapd   xmm7, xmm1
        vmovups   XMMWORD PTR [240+rsp], xmm6
        vmovd     xmm1, ecx
        mov       QWORD PTR [232+rsp], r13
        mov       ecx, 1094189056
        vmovsd    xmm15, QWORD PTR [__svml_dpow_ha_data_internal+27520]
        vpaddd    xmm4, xmm14, xmm12
        vmovsd    xmm13, QWORD PTR [__svml_dpow_ha_data_internal+27648]
        vandpd    xmm5, xmm0, xmm15
        vpsrld    xmm6, xmm4, 10
        vorpd     xmm15, xmm5, xmm13
        vpslld    xmm5, xmm6, 3
        vpsubd    xmm14, xmm3, xmm8
        vmovd     r10d, xmm5
        vpsrad    xmm12, xmm14, 20
        vmovd     xmm4, ecx
        vpslld    xmm13, xmm6, 4
        vpaddd    xmm5, xmm12, xmm4
        vmovd     r11d, xmm13
        mov       r9d, 1048576
        vmovsd    xmm13, QWORD PTR [__svml_dpow_ha_data_internal+27712]
        vpshufd   xmm6, xmm5, 0
        movsxd    r10, r10d
        vandpd    xmm8, xmm6, xmm13
        movsxd    r11, r11d
        mov       edx, 2097152
        vpshufd   xmm2, xmm7, 85
        mov       ecx, 1072693248
        vpand     xmm2, xmm2, xmm9
        lea       r13, QWORD PTR [95+rsp]
        vmulsd    xmm6, xmm15, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+r10]
        and       r13, -64
        vsubsd    xmm12, xmm8, QWORD PTR [__svml_dpow_ha_data_internal+27776]
        vaddsd    xmm4, xmm6, QWORD PTR [__svml_dpow_ha_data_internal+27904]
        vmovsd    xmm8, QWORD PTR [__svml_dpow_ha_data_internal+27840]
        vandpd    xmm5, xmm15, xmm8
        mov       QWORD PTR [368+rsp], r13
        vsubsd    xmm14, xmm15, xmm5
        vmulsd    xmm15, xmm5, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+r10]
        vsubsd    xmm5, xmm15, xmm6
        vmulsd    xmm15, xmm14, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+r10]
        vaddsd    xmm14, xmm12, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rax+r11]
        vaddsd    xmm13, xmm15, xmm5
        vaddsd    xmm15, xmm14, xmm4
        vaddsd    xmm6, xmm15, xmm13
        vsubsd    xmm12, xmm14, xmm15
        vsubsd    xmm5, xmm15, xmm6
        vaddsd    xmm12, xmm12, xmm4
        vaddsd    xmm14, xmm5, xmm13
        vaddsd    xmm15, xmm14, xmm12
        vaddsd    xmm5, xmm15, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8264+rax+r11]
        vaddsd    xmm15, xmm4, xmm13
        vmovsd    xmm4, QWORD PTR [__svml_dpow_ha_data_internal+26816]
        vmulsd    xmm13, xmm4, xmm15
        vaddsd    xmm14, xmm13, QWORD PTR [__svml_dpow_ha_data_internal+26880]
        vmulsd    xmm12, xmm14, xmm15
        vaddsd    xmm4, xmm12, QWORD PTR [__svml_dpow_ha_data_internal+26944]
        vmulsd    xmm13, xmm4, xmm15
        vaddsd    xmm14, xmm13, QWORD PTR [__svml_dpow_ha_data_internal+27008]
        vmulsd    xmm12, xmm14, xmm15
        vaddsd    xmm4, xmm12, QWORD PTR [__svml_dpow_ha_data_internal+27072]
        vmulsd    xmm13, xmm4, xmm15
        vaddsd    xmm14, xmm13, QWORD PTR [__svml_dpow_ha_data_internal+27136]
        vmulsd    xmm15, xmm14, xmm15
        vandpd    xmm14, xmm7, xmm8
        vaddsd    xmm5, xmm15, xmm5
        vaddsd    xmm15, xmm6, xmm5
        vsubsd    xmm6, xmm15, xmm6
        vsubsd    xmm6, xmm5, xmm6
        vandpd    xmm5, xmm15, xmm8
        vmulsd    xmm8, xmm14, xmm5
        vsubsd    xmm4, xmm15, xmm5
        vsubsd    xmm15, xmm7, xmm14
        vmulsd    xmm14, xmm14, xmm4
        vpshufd   xmm13, xmm8, 85
        vpand     xmm12, xmm13, xmm9
        vmovd     xmm9, r9d
        vpaddd    xmm9, xmm3, xmm9
        vmovd     xmm3, edx
        vpcmpgtd  xmm9, xmm3, xmm9
        vpcmpgtd  xmm3, xmm2, xmm1
        vpcmpeqd  xmm1, xmm2, xmm1
        vmovd     xmm13, r8d
        vpor      xmm2, xmm3, xmm1
        vpcmpgtd  xmm1, xmm12, xmm13
        vpor      xmm2, xmm9, xmm2
        vpcmpeqd  xmm13, xmm12, xmm13
        vmulsd    xmm9, xmm15, xmm4
        vpor      xmm12, xmm1, xmm13
        vmulsd    xmm15, xmm15, xmm5
        vmulsd    xmm13, xmm7, xmm6
        vaddsd    xmm4, xmm14, xmm9
        vpor      xmm3, xmm2, xmm12
        vmovd     xmm6, ecx
        vmovsd    xmm12, QWORD PTR [__svml_dpow_ha_data_internal+28032]
        mov       r8d, 127
        vmovmskps edx, xmm3
        vaddsd    xmm14, xmm8, xmm12
        vaddsd    xmm1, xmm15, xmm4
        vsubsd    xmm9, xmm14, xmm12
        vpshufd   xmm4, xmm14, 0
        vmovd     xmm2, r8d
        vpslld    xmm5, xmm4, 13
        vpand     xmm3, xmm4, xmm2
        vpaddd    xmm15, xmm5, xmm6
        vpslld    xmm4, xmm3, 4
        vmovsd    xmm6, QWORD PTR [__svml_dpow_ha_data_internal+27968]
        vpshufd   xmm5, xmm15, 0
        vandpd    xmm15, xmm5, xmm6
        vsubsd    xmm5, xmm8, xmm9
        vaddsd    xmm8, xmm1, xmm13
        vmovd     r9d, xmm4
        vmovsd    xmm1, QWORD PTR [__svml_dpow_ha_data_internal+27200]
        vaddsd    xmm5, xmm8, xmm5
        vmulsd    xmm6, xmm5, xmm5
        vmulsd    xmm2, xmm1, xmm5
        vmulsd    xmm4, xmm6, xmm6
        vaddsd    xmm3, xmm2, QWORD PTR [__svml_dpow_ha_data_internal+27264]
        vmovsd    xmm12, QWORD PTR [__svml_dpow_ha_data_internal+27328]
        vmulsd    xmm13, xmm12, xmm5
        vmulsd    xmm8, xmm3, xmm4
        vaddsd    xmm14, xmm13, QWORD PTR [__svml_dpow_ha_data_internal+27392]
        movsxd    r9, r9d
        vmulsd    xmm6, xmm14, xmm6
        vmulsd    xmm9, xmm8, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r9]
        vmulsd    xmm2, xmm6, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r9]
        vaddsd    xmm1, xmm9, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24712+rax+r9]
        vmovsd    xmm3, QWORD PTR [__svml_dpow_ha_data_internal+27456]
        vaddsd    xmm4, xmm1, xmm2
        vmulsd    xmm5, xmm3, xmm5
        vmulsd    xmm6, xmm5, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r9]
        vaddsd    xmm8, xmm4, xmm6
        vaddsd    xmm9, xmm8, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r9]
        vmulsd    xmm1, xmm9, xmm15
        and       edx, 1
        jne       _B6_3

_B6_2::

        vmovups   xmm6, XMMWORD PTR [240+rsp]
        vmovapd   xmm0, xmm1
        vmovups   xmm7, XMMWORD PTR [256+rsp]
        vmovups   xmm8, XMMWORD PTR [336+rsp]
        vmovups   xmm9, XMMWORD PTR [272+rsp]
        vmovups   xmm12, XMMWORD PTR [352+rsp]
        vmovups   xmm13, XMMWORD PTR [320+rsp]
        vmovups   xmm14, XMMWORD PTR [304+rsp]
        vmovups   xmm15, XMMWORD PTR [288+rsp]
        mov       r13, QWORD PTR [232+rsp]
        add       rsp, 376
        ret

_B6_3::

        vmovsd    QWORD PTR [r13], xmm0
        vmovsd    QWORD PTR [64+r13], xmm7
        vmovsd    QWORD PTR [128+r13], xmm1
        jne       _B6_6

_B6_4::

        vmovsd    xmm1, QWORD PTR [128+r13]
        jmp       _B6_2

_B6_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]
        lea       r8, QWORD PTR [128+r13]

        call      __svml_dpow_ha_cout_rare_internal
        jmp       _B6_4
        ALIGN     16

_B6_7::

__svml_pow1_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow1_ha_e9_B1_B6:
	DD	1353473
	DD	1954983
	DD	1009819
	DD	1079438
	DD	1493120
	DD	1366131
	DD	1304677
	DD	1243228
	DD	1411139
	DD	1153073
	DD	3080459

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_pow1_ha_e9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_pow4_ha_l9

__svml_pow4_ha_l9	PROC	EXPORT

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L173::

        sub       rsp, 616
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [304+rsp], ymm15
        vmovups   YMMWORD PTR [336+rsp], ymm14
        vmovups   YMMWORD PTR [464+rsp], ymm13
        vmovups   YMMWORD PTR [496+rsp], ymm12
        vmovups   YMMWORD PTR [560+rsp], ymm10
        vmovups   YMMWORD PTR [528+rsp], ymm9
        vmovups   YMMWORD PTR [368+rsp], ymm8
        vmovups   YMMWORD PTR [400+rsp], ymm7
        vmovups   YMMWORD PTR [432+rsp], ymm6
        mov       QWORD PTR [592+rsp], r13
        lea       r13, QWORD PTR [143+rsp]
        vmovups   xmm4, XMMWORD PTR [__svml_dpow_ha_data_internal+28416]
        and       r13, -64
        vmovapd   ymm14, ymm1
        vmovups   xmm7, XMMWORD PTR [__svml_dpow_ha_data_internal+28480]
        vmovups   xmm8, XMMWORD PTR [__svml_dpow_ha_data_internal+28544]
        mov       QWORD PTR [600+rsp], r13
        vextracti128 xmm5, ymm0, 1
        vshufps   xmm6, xmm0, xmm5, 221
        vandps    xmm12, xmm6, XMMWORD PTR [__svml_dpow_ha_data_internal+28672]
        vpaddd    xmm13, xmm12, XMMWORD PTR [__svml_dpow_ha_data_internal+28736]
        vpaddd    xmm3, xmm6, XMMWORD PTR [__svml_dpow_ha_data_internal+28352]
        vpsrld    xmm5, xmm13, 10
        vpcmpgtd  xmm10, xmm4, xmm3
        vpslld    xmm3, xmm5, 3
        vmovd     eax, xmm3
        vpslld    xmm12, xmm5, 4
        vmovd     r10d, xmm12
        vpsubd    xmm6, xmm6, XMMWORD PTR [__svml_dpow_ha_data_internal+28800]
        movsxd    rax, eax
        vpextrd   ecx, xmm3, 1
        movsxd    rcx, ecx
        vpextrd   r8d, xmm3, 2
        vpextrd   r11d, xmm12, 1
        movsxd    r8, r8d
        vpextrd   r9d, xmm3, 3
        movsxd    r10, r10d
        movsxd    r11, r11d
        movsxd    r9, r9d
        vandpd    ymm15, ymm0, YMMWORD PTR [__svml_dpow_ha_data_internal+27520]
        vorpd     ymm1, ymm15, YMMWORD PTR [__svml_dpow_ha_data_internal+27648]
        vmovsd    xmm15, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rdx+r8]
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rdx+r10]
        vmovhpd   xmm13, xmm15, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rdx+r9]
        vextracti128 xmm2, ymm14, 1
        vshufps   xmm9, xmm14, xmm2, 221
        vmovsd    xmm2, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rdx+rax]
        vpand     xmm9, xmm9, xmm7
        vmovhpd   xmm4, xmm2, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rdx+rcx]
        vpextrd   eax, xmm12, 2
        vpextrd   ecx, xmm12, 3
        movsxd    rax, eax
        movsxd    rcx, ecx
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rdx+r11]
        vinsertf128 ymm12, ymm5, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rdx+rax], 1
        vinsertf128 ymm15, ymm2, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rdx+rcx], 1
        vunpcklpd ymm2, ymm12, ymm15
        vinsertf128 ymm3, ymm4, xmm13, 1
        vunpckhpd ymm4, ymm12, ymm15
        vpsrad    xmm15, xmm6, 20
        vpaddd    xmm6, xmm15, XMMWORD PTR [__svml_dpow_ha_data_internal+28864]
        vpshufd   xmm12, xmm6, 80
        vpshufd   xmm13, xmm6, 250
        vmovupd   ymm6, YMMWORD PTR [__svml_dpow_ha_data_internal+27840]
        vinserti128 ymm5, ymm12, xmm13, 1
        vmulpd    ymm12, ymm1, ymm3
        vandpd    ymm15, ymm5, YMMWORD PTR [__svml_dpow_ha_data_internal+27712]
        vsubpd    ymm13, ymm15, YMMWORD PTR [__svml_dpow_ha_data_internal+27776]
        vandpd    ymm15, ymm1, ymm6
        vsubpd    ymm5, ymm1, ymm15
        vaddpd    ymm1, ymm12, YMMWORD PTR [__svml_dpow_ha_data_internal+27904]
        vfmsub213pd ymm15, ymm3, ymm12
        vfmadd213pd ymm5, ymm3, ymm15
        vaddpd    ymm3, ymm2, ymm13
        vaddpd    ymm13, ymm1, ymm3
        vaddpd    ymm12, ymm5, ymm13
        vsubpd    ymm15, ymm3, ymm13
        vsubpd    ymm2, ymm13, ymm12
        vaddpd    ymm3, ymm1, ymm15
        vaddpd    ymm1, ymm1, ymm5
        vaddpd    ymm15, ymm5, ymm2
        vaddpd    ymm13, ymm3, ymm15
        vmovupd   ymm15, YMMWORD PTR [__svml_dpow_ha_data_internal+26816]
        vaddpd    ymm4, ymm4, ymm13
        vfmadd213pd ymm15, ymm1, YMMWORD PTR [__svml_dpow_ha_data_internal+26880]
        vfmadd213pd ymm15, ymm1, YMMWORD PTR [__svml_dpow_ha_data_internal+26944]
        vfmadd213pd ymm15, ymm1, YMMWORD PTR [__svml_dpow_ha_data_internal+27008]
        vfmadd213pd ymm15, ymm1, YMMWORD PTR [__svml_dpow_ha_data_internal+27072]
        vfmadd213pd ymm15, ymm1, YMMWORD PTR [__svml_dpow_ha_data_internal+27136]
        vfmadd213pd ymm15, ymm1, ymm4
        vaddpd    ymm13, ymm12, ymm15
        vandpd    ymm5, ymm13, ymm6
        vandpd    ymm2, ymm14, ymm6
        vsubpd    ymm12, ymm13, ymm12
        vsubpd    ymm1, ymm13, ymm5
        vmulpd    ymm13, ymm5, ymm2
        vsubpd    ymm15, ymm15, ymm12
        vsubpd    ymm12, ymm14, ymm2
        vextracti128 xmm6, ymm13, 1
        vshufps   xmm3, xmm13, xmm6, 221
        vpand     xmm4, xmm3, xmm7
        vpcmpgtd  xmm7, xmm9, xmm8
        vpcmpeqd  xmm8, xmm9, xmm8
        vpor      xmm9, xmm7, xmm8
        vmulpd    ymm7, ymm1, ymm12
        vpor      xmm8, xmm10, xmm9
        vmovups   xmm3, XMMWORD PTR [__svml_dpow_ha_data_internal+28608]
        vpcmpgtd  xmm10, xmm4, xmm3
        vpcmpeqd  xmm9, xmm4, xmm3
        vpor      xmm10, xmm10, xmm9
        vpor      xmm6, xmm8, xmm10
        vfmadd213pd ymm2, ymm1, ymm7
        vmovmskps eax, xmm6
        vmulpd    ymm6, ymm14, ymm15
        vfmadd213pd ymm12, ymm5, ymm2
        vmovupd   ymm5, YMMWORD PTR [__svml_dpow_ha_data_internal+28032]
        vaddpd    ymm12, ymm12, ymm6
        vaddpd    ymm7, ymm5, ymm13
        vmovupd   ymm6, YMMWORD PTR [__svml_dpow_ha_data_internal+27328]
        vextracti128 xmm15, ymm7, 1
        vsubpd    ymm5, ymm7, ymm5
        vshufps   xmm2, xmm7, xmm15, 136
        vandps    xmm1, xmm2, XMMWORD PTR [__svml_dpow_ha_data_internal+28992]
        vpslld    xmm3, xmm2, 13
        vpslld    xmm15, xmm1, 4
        vmovd     r8d, xmm15
        vpaddd    xmm9, xmm3, XMMWORD PTR [__svml_dpow_ha_data_internal+28928]
        vpshufd   xmm4, xmm9, 80
        vpextrd   r9d, xmm15, 1
        movsxd    r8, r8d
        movsxd    r9, r9d
        vpextrd   r10d, xmm15, 2
        vpextrd   ecx, xmm15, 3
        movsxd    r10, r10d
        movsxd    rcx, ecx
        vsubpd    ymm13, ymm13, ymm5
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rdx+r8]
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rdx+r9]
        vpshufd   xmm8, xmm9, 250
        vinserti128 ymm10, ymm4, xmm8, 1
        vinsertf128 ymm15, ymm1, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rdx+r10], 1
        vinsertf128 ymm3, ymm2, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rdx+rcx], 1
        vandpd    ymm9, ymm10, YMMWORD PTR [__svml_dpow_ha_data_internal+27968]
        vunpcklpd ymm8, ymm15, ymm3
        vunpckhpd ymm10, ymm15, ymm3
        vaddpd    ymm15, ymm13, ymm12
        vmovupd   ymm2, YMMWORD PTR [__svml_dpow_ha_data_internal+27200]
        vmulpd    ymm5, ymm15, ymm15
        vfmadd213pd ymm6, ymm15, YMMWORD PTR [__svml_dpow_ha_data_internal+27392]
        vfmadd213pd ymm2, ymm15, YMMWORD PTR [__svml_dpow_ha_data_internal+27264]
        vmulpd    ymm15, ymm15, YMMWORD PTR [__svml_dpow_ha_data_internal+27456]
        vmulpd    ymm1, ymm5, ymm5
        vmulpd    ymm7, ymm5, ymm6
        vmulpd    ymm3, ymm1, ymm2
        vmulpd    ymm12, ymm8, ymm7
        vmulpd    ymm1, ymm8, ymm15
        vmulpd    ymm4, ymm8, ymm3
        vaddpd    ymm10, ymm10, ymm4
        vaddpd    ymm13, ymm10, ymm12
        vaddpd    ymm2, ymm13, ymm1
        vaddpd    ymm8, ymm8, ymm2
        vmulpd    ymm1, ymm9, ymm8
        test      eax, eax
        jne       _B7_3

_B7_2::

        vmovups   ymm6, YMMWORD PTR [432+rsp]
        vmovups   ymm7, YMMWORD PTR [400+rsp]
        vmovups   ymm8, YMMWORD PTR [368+rsp]
        vmovups   ymm9, YMMWORD PTR [528+rsp]
        vmovups   ymm10, YMMWORD PTR [560+rsp]
        vmovups   ymm12, YMMWORD PTR [496+rsp]
        vmovups   ymm13, YMMWORD PTR [464+rsp]
        vmovups   ymm14, YMMWORD PTR [336+rsp]
        vmovups   ymm15, YMMWORD PTR [304+rsp]
        mov       r13, QWORD PTR [592+rsp]
        vmovdqa   ymm0, ymm1
        add       rsp, 616
        ret

_B7_3::

        vmovupd   YMMWORD PTR [r13], ymm0
        vmovupd   YMMWORD PTR [64+r13], ymm14
        vmovupd   YMMWORD PTR [128+r13], ymm1

_B7_6::

        xor       edx, edx
        vmovups   YMMWORD PTR [32+rsp], ymm11
        mov       QWORD PTR [72+rsp], rbx
        mov       ebx, edx
        mov       QWORD PTR [64+rsp], rsi
        mov       esi, eax

_B7_7::

        bt        esi, ebx
        jc        _B7_10

_B7_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B7_7

_B7_9::

        vmovups   ymm11, YMMWORD PTR [32+rsp]
        vmovupd   ymm1, YMMWORD PTR [128+r13]
        mov       rbx, QWORD PTR [72+rsp]
        mov       rsi, QWORD PTR [64+rsp]
        jmp       _B7_2

_B7_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]
        lea       r8, QWORD PTR [128+r13+rbx*8]

        call      __svml_dpow_ha_cout_rare_internal
        jmp       _B7_8
        ALIGN     16

_B7_11::

__svml_pow4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow4_ha_l9_B1_B3:
	DD	1469185
	DD	4904043
	DD	1796195
	DD	1669210
	DD	1542225
	DD	2201672
	DD	2336831
	DD	2082870
	DD	1955885
	DD	1435684
	DD	1308699
	DD	5046539

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_6
	DD	imagerel _unwind___svml_pow4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow4_ha_l9_B6_B10:
	DD	398369
	DD	549908
	DD	603149
	DD	178184
	DD	imagerel _B7_1
	DD	imagerel _B7_6
	DD	imagerel _unwind___svml_pow4_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_6
	DD	imagerel _B7_11
	DD	imagerel _unwind___svml_pow4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST7:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_pow2_ha_l9

__svml_pow2_ha_l9	PROC	EXPORT

_B8_1::

        DB        243
        DB        15
        DB        30
        DB        250
L200::

        sub       rsp, 440
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [400+rsp], xmm15
        vmovups   XMMWORD PTR [256+rsp], xmm14
        vmovups   XMMWORD PTR [272+rsp], xmm13
        vmovups   XMMWORD PTR [288+rsp], xmm12
        vmovups   XMMWORD PTR [304+rsp], xmm11
        vmovapd   xmm11, xmm1
        vmovups   XMMWORD PTR [320+rsp], xmm10
        vmovups   XMMWORD PTR [336+rsp], xmm9
        vmovups   XMMWORD PTR [352+rsp], xmm8
        vmovups   XMMWORD PTR [368+rsp], xmm7
        vmovups   XMMWORD PTR [384+rsp], xmm6
        mov       QWORD PTR [416+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vpshufd   xmm6, xmm0, 221
        and       r13, -64
        vmovq     xmm13, QWORD PTR [__svml_dpow_ha_data_internal+28672]
        vmovq     xmm9, QWORD PTR [__svml_dpow_ha_data_internal+28736]
        vpand     xmm12, xmm6, xmm13
        vpaddd    xmm10, xmm12, xmm9
        vpsrld    xmm2, xmm10, 10
        vandpd    xmm14, xmm0, XMMWORD PTR [__svml_dpow_ha_data_internal+27520]
        vpslld    xmm1, xmm2, 3
        vorpd     xmm15, xmm14, XMMWORD PTR [__svml_dpow_ha_data_internal+27648]
        vpslld    xmm14, xmm2, 4
        vmovd     edx, xmm1
        vmovd     r8d, xmm14
        vmovq     xmm4, QWORD PTR [__svml_dpow_ha_data_internal+28480]
        vpshufd   xmm3, xmm11, 221
        movsxd    rdx, edx
        vpand     xmm7, xmm3, xmm4
        vpextrd   ecx, xmm1, 1
        vpextrd   r9d, xmm14, 1
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movsxd    r9, r9d
        vmovsd    xmm3, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+rdx]
        vmovq     xmm2, QWORD PTR [__svml_dpow_ha_data_internal+28800]
        vmovhpd   xmm9, xmm3, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+rcx]
        vpsubd    xmm3, xmm6, xmm2
        vmovupd   xmm13, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rax+r8]
        vpsrad    xmm14, xmm3, 20
        vmovupd   xmm10, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rax+r9]
        vunpcklpd xmm12, xmm13, xmm10
        vunpckhpd xmm1, xmm13, xmm10
        vmovq     xmm13, QWORD PTR [__svml_dpow_ha_data_internal+28864]
        vpaddd    xmm10, xmm14, xmm13
        vpshufd   xmm2, xmm10, 80
        vandpd    xmm3, xmm2, XMMWORD PTR [__svml_dpow_ha_data_internal+27712]
        vsubpd    xmm10, xmm3, XMMWORD PTR [__svml_dpow_ha_data_internal+27776]
        vmovupd   xmm3, XMMWORD PTR [__svml_dpow_ha_data_internal+27840]
        vandpd    xmm14, xmm15, xmm3
        vsubpd    xmm2, xmm15, xmm14
        vmulpd    xmm15, xmm15, xmm9
        vfmsub213pd xmm14, xmm9, xmm15
        vmovq     xmm5, QWORD PTR [__svml_dpow_ha_data_internal+28352]
        vmovq     xmm8, QWORD PTR [__svml_dpow_ha_data_internal+28416]
        vaddpd    xmm13, xmm15, XMMWORD PTR [__svml_dpow_ha_data_internal+27904]
        vfmadd213pd xmm2, xmm9, xmm14
        vaddpd    xmm14, xmm12, xmm10
        vaddpd    xmm15, xmm13, xmm14
        vaddpd    xmm9, xmm2, xmm15
        vsubpd    xmm12, xmm14, xmm15
        vsubpd    xmm10, xmm15, xmm9
        vaddpd    xmm14, xmm13, xmm12
        vaddpd    xmm13, xmm13, xmm2
        vaddpd    xmm12, xmm2, xmm10
        vmovupd   xmm2, XMMWORD PTR [__svml_dpow_ha_data_internal+26816]
        vfmadd213pd xmm2, xmm13, XMMWORD PTR [__svml_dpow_ha_data_internal+26880]
        vaddpd    xmm15, xmm14, xmm12
        vfmadd213pd xmm2, xmm13, XMMWORD PTR [__svml_dpow_ha_data_internal+26944]
        vandpd    xmm12, xmm11, xmm3
        vaddpd    xmm1, xmm1, xmm15
        vfmadd213pd xmm2, xmm13, XMMWORD PTR [__svml_dpow_ha_data_internal+27008]
        vfmadd213pd xmm2, xmm13, XMMWORD PTR [__svml_dpow_ha_data_internal+27072]
        vfmadd213pd xmm2, xmm13, XMMWORD PTR [__svml_dpow_ha_data_internal+27136]
        vfmadd213pd xmm2, xmm13, xmm1
        vmovq     xmm13, QWORD PTR [__svml_dpow_ha_data_internal+28608]
        vaddpd    xmm14, xmm9, xmm2
        vandpd    xmm1, xmm14, xmm3
        vmulpd    xmm10, xmm1, xmm12
        vsubpd    xmm9, xmm14, xmm9
        vsubpd    xmm15, xmm14, xmm1
        vsubpd    xmm2, xmm2, xmm9
        vsubpd    xmm9, xmm11, xmm12
        vpshufd   xmm3, xmm10, 221
        vpand     xmm14, xmm3, xmm4
        vpaddd    xmm4, xmm6, xmm5
        vmovq     xmm5, QWORD PTR [__svml_dpow_ha_data_internal+28544]
        vpcmpgtd  xmm3, xmm8, xmm4
        vpcmpgtd  xmm8, xmm7, xmm5
        vpcmpeqd  xmm7, xmm7, xmm5
        vpor      xmm4, xmm8, xmm7
        vpcmpgtd  xmm5, xmm14, xmm13
        vpcmpeqd  xmm14, xmm14, xmm13
        vpor      xmm6, xmm3, xmm4
        vpor      xmm13, xmm5, xmm14
        vpor      xmm14, xmm6, xmm13
        vmulpd    xmm13, xmm15, xmm9
        vmovmskps edx, xmm14
        vmovupd   xmm8, XMMWORD PTR [__svml_dpow_ha_data_internal+28032]
        vaddpd    xmm7, xmm8, xmm10
        vfmadd213pd xmm12, xmm15, xmm13
        vmulpd    xmm15, xmm11, xmm2
        vsubpd    xmm5, xmm7, xmm8
        vfmadd213pd xmm9, xmm1, xmm12
        vpshufd   xmm2, xmm7, 136
        vmovq     xmm12, QWORD PTR [__svml_dpow_ha_data_internal+28992]
        vpslld    xmm14, xmm2, 13
        vsubpd    xmm10, xmm10, xmm5
        vaddpd    xmm9, xmm9, xmm15
        vpand     xmm1, xmm2, xmm12
        vpslld    xmm1, xmm1, 4
        vmovd     r10d, xmm1
        vmovq     xmm13, QWORD PTR [__svml_dpow_ha_data_internal+28928]
        vpaddd    xmm12, xmm14, xmm13
        vpextrd   r11d, xmm1, 1
        vaddpd    xmm7, xmm10, xmm9
        movsxd    r10, r10d
        movsxd    r11, r11d
        vpshufd   xmm2, xmm12, 80
        vmovupd   xmm1, XMMWORD PTR [__svml_dpow_ha_data_internal+27200]
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r10]
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r11]
        vunpcklpd xmm13, xmm3, xmm4
        vunpckhpd xmm12, xmm3, xmm4
        vmulpd    xmm3, xmm7, xmm7
        vfmadd213pd xmm1, xmm7, XMMWORD PTR [__svml_dpow_ha_data_internal+27264]
        vandpd    xmm14, xmm2, XMMWORD PTR [__svml_dpow_ha_data_internal+27968]
        vmulpd    xmm15, xmm3, xmm3
        vmulpd    xmm8, xmm7, XMMWORD PTR [__svml_dpow_ha_data_internal+27456]
        vmulpd    xmm15, xmm15, xmm1
        vmulpd    xmm10, xmm13, xmm8
        vmulpd    xmm2, xmm13, xmm15
        vaddpd    xmm5, xmm12, xmm2
        vmovupd   xmm12, XMMWORD PTR [__svml_dpow_ha_data_internal+27328]
        vfmadd213pd xmm12, xmm7, XMMWORD PTR [__svml_dpow_ha_data_internal+27392]
        vmulpd    xmm4, xmm3, xmm12
        vmulpd    xmm6, xmm13, xmm4
        vaddpd    xmm9, xmm5, xmm6
        vaddpd    xmm1, xmm9, xmm10
        vaddpd    xmm13, xmm13, xmm1
        mov       QWORD PTR [424+rsp], r13
        vmulpd    xmm1, xmm14, xmm13
        and       edx, 3
        jne       _B8_3

_B8_2::

        vmovups   xmm6, XMMWORD PTR [384+rsp]
        vmovapd   xmm0, xmm1
        vmovups   xmm7, XMMWORD PTR [368+rsp]
        vmovups   xmm8, XMMWORD PTR [352+rsp]
        vmovups   xmm9, XMMWORD PTR [336+rsp]
        vmovups   xmm10, XMMWORD PTR [320+rsp]
        vmovups   xmm11, XMMWORD PTR [304+rsp]
        vmovups   xmm12, XMMWORD PTR [288+rsp]
        vmovups   xmm13, XMMWORD PTR [272+rsp]
        vmovups   xmm14, XMMWORD PTR [256+rsp]
        vmovups   xmm15, XMMWORD PTR [400+rsp]
        mov       r13, QWORD PTR [416+rsp]
        add       rsp, 440
        ret

_B8_3::

        vmovupd   XMMWORD PTR [r13], xmm0
        vmovupd   XMMWORD PTR [64+r13], xmm11
        vmovupd   XMMWORD PTR [128+r13], xmm1
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
        vmovupd   xmm1, XMMWORD PTR [128+r13]
        jmp       _B8_2

_B8_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]
        lea       r8, QWORD PTR [128+r13+rbx*8]

        call      __svml_dpow_ha_cout_rare_internal
        jmp       _B8_8
        ALIGN     16

_B8_11::

__svml_pow2_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow2_ha_l9_B1_B3:
	DD	1603585
	DD	3462264
	DD	1599600
	DD	1538151
	DD	1476702
	DD	1415253
	DD	1353804
	DD	1292351
	DD	1230902
	DD	1169453
	DD	1108004
	DD	1701915
	DD	3604747

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_1
	DD	imagerel _B8_6
	DD	imagerel _unwind___svml_pow2_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow2_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B8_1
	DD	imagerel _B8_6
	DD	imagerel _unwind___svml_pow2_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_6
	DD	imagerel _B8_11
	DD	imagerel _unwind___svml_pow2_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST8:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_pow1_ha_l9

__svml_pow1_ha_l9	PROC	EXPORT

_B9_1::

        DB        243
        DB        15
        DB        30
        DB        250
L227::

        sub       rsp, 376
        mov       r8d, 1048064
        mov       edx, 2147483647
        mov       r9d, 512
        mov       ecx, 2139095040
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [288+rsp], xmm15
        vmovups   XMMWORD PTR [320+rsp], xmm14
        vmovd     xmm14, r8d
        vmovups   XMMWORD PTR [336+rsp], xmm13
        vmovd     xmm2, edx
        vmovups   XMMWORD PTR [304+rsp], xmm10
        mov       edx, 1072168448
        vmovups   XMMWORD PTR [352+rsp], xmm9
        vmovd     xmm9, r9d
        vmovups   XMMWORD PTR [256+rsp], xmm8
        mov       r9d, 1048576
        vmovups   XMMWORD PTR [272+rsp], xmm7
        vmovd     xmm7, ecx
        vmovups   XMMWORD PTR [240+rsp], xmm6
        mov       ecx, 1094189056
        mov       QWORD PTR [232+rsp], r13
        mov       r8d, 1083129855
        vpshufd   xmm3, xmm0, 85
        lea       r13, QWORD PTR [95+rsp]
        vpshufd   xmm10, xmm1, 85
        vpand     xmm8, xmm3, xmm14
        vmovsd    xmm5, QWORD PTR [__svml_dpow_ha_data_internal+27520]
        vpand     xmm6, xmm10, xmm2
        vmovsd    xmm15, QWORD PTR [__svml_dpow_ha_data_internal+27648]
        vandpd    xmm10, xmm0, xmm5
        vpaddd    xmm13, xmm8, xmm9
        vorpd     xmm4, xmm10, xmm15
        vpsrld    xmm10, xmm13, 10
        vmovd     xmm14, edx
        vpslld    xmm5, xmm10, 3
        vpsubd    xmm8, xmm3, xmm14
        vmovd     r10d, xmm5
        vpsrad    xmm9, xmm8, 20
        vmovd     xmm13, ecx
        vpslld    xmm15, xmm10, 4
        vpaddd    xmm5, xmm9, xmm13
        movsxd    r10, r10d
        mov       edx, 2097152
        vmovd     r11d, xmm15
        mov       ecx, 1072693248
        vmovsd    xmm15, QWORD PTR [__svml_dpow_ha_data_internal+27712]
        vpshufd   xmm10, xmm5, 0
        vandpd    xmm14, xmm10, xmm15
        and       r13, -64
        vmovsd    xmm15, QWORD PTR [__svml_dpow_ha_data_internal+27840]
        vandpd    xmm8, xmm4, xmm15
        vsubsd    xmm9, xmm14, QWORD PTR [__svml_dpow_ha_data_internal+27776]
        vsubsd    xmm10, xmm4, xmm8
        vmulsd    xmm4, xmm4, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+r10]
        movsxd    r11, r11d
        vaddsd    xmm5, xmm4, QWORD PTR [__svml_dpow_ha_data_internal+27904]
        vfmsub132sd xmm8, xmm4, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+r10]
        vaddsd    xmm4, xmm9, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8256+rax+r11]
        vfmadd132sd xmm10, xmm8, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+rax+r10]
        vaddsd    xmm13, xmm4, xmm5
        mov       QWORD PTR [368+rsp], r13
        vsubsd    xmm14, xmm4, xmm13
        vaddsd    xmm9, xmm14, xmm5
        vaddsd    xmm14, xmm13, xmm10
        vsubsd    xmm4, xmm13, xmm14
        vaddsd    xmm8, xmm4, xmm10
        vaddsd    xmm10, xmm5, xmm10
        vaddsd    xmm13, xmm8, xmm9
        vmovsd    xmm5, QWORD PTR [__svml_dpow_ha_data_internal+26816]
        vandpd    xmm9, xmm1, xmm15
        vfmadd213sd xmm5, xmm10, QWORD PTR [__svml_dpow_ha_data_internal+26880]
        vaddsd    xmm4, xmm13, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+8264+rax+r11]
        vfmadd213sd xmm5, xmm10, QWORD PTR [__svml_dpow_ha_data_internal+26944]
        vfmadd213sd xmm5, xmm10, QWORD PTR [__svml_dpow_ha_data_internal+27008]
        vfmadd213sd xmm5, xmm10, QWORD PTR [__svml_dpow_ha_data_internal+27072]
        vfmadd213sd xmm5, xmm10, QWORD PTR [__svml_dpow_ha_data_internal+27136]
        vfmadd213sd xmm5, xmm10, xmm4
        vaddsd    xmm4, xmm14, xmm5
        vsubsd    xmm10, xmm4, xmm14
        vsubsd    xmm13, xmm5, xmm10
        vandpd    xmm10, xmm4, xmm15
        vmulsd    xmm14, xmm9, xmm10
        vsubsd    xmm8, xmm4, xmm10
        vsubsd    xmm15, xmm1, xmm9
        vpshufd   xmm5, xmm14, 85
        vpand     xmm4, xmm5, xmm2
        vmovd     xmm2, r9d
        vpaddd    xmm2, xmm3, xmm2
        vmovd     xmm3, edx
        vpcmpgtd  xmm2, xmm3, xmm2
        vpcmpgtd  xmm3, xmm6, xmm7
        vpcmpeqd  xmm6, xmm6, xmm7
        vmovd     xmm5, r8d
        vpor      xmm7, xmm3, xmm6
        mov       r8d, 127
        vpor      xmm3, xmm2, xmm7
        vpcmpgtd  xmm2, xmm4, xmm5
        vmulsd    xmm7, xmm15, xmm8
        vpcmpeqd  xmm4, xmm4, xmm5
        vfmadd213sd xmm9, xmm8, xmm7
        vpor      xmm5, xmm2, xmm4
        vmulsd    xmm4, xmm1, xmm13
        vmovd     xmm2, ecx
        vfmadd213sd xmm15, xmm10, xmm9
        vmovsd    xmm9, QWORD PTR [__svml_dpow_ha_data_internal+28032]
        vpor      xmm6, xmm3, xmm5
        vmovsd    xmm5, QWORD PTR [__svml_dpow_ha_data_internal+27968]
        vaddsd    xmm8, xmm14, xmm9
        vmovmskps edx, xmm6
        vaddsd    xmm15, xmm15, xmm4
        vpshufd   xmm13, xmm8, 0
        vmovd     xmm6, r8d
        vpslld    xmm10, xmm13, 13
        vpand     xmm13, xmm13, xmm6
        vpaddd    xmm10, xmm10, xmm2
        vpslld    xmm2, xmm13, 4
        vpshufd   xmm3, xmm10, 0
        vsubsd    xmm8, xmm8, xmm9
        vmovd     r9d, xmm2
        vandpd    xmm10, xmm3, xmm5
        vsubsd    xmm9, xmm14, xmm8
        vmovsd    xmm14, QWORD PTR [__svml_dpow_ha_data_internal+27200]
        vaddsd    xmm15, xmm15, xmm9
        vmulsd    xmm5, xmm15, xmm15
        vfmadd213sd xmm14, xmm15, QWORD PTR [__svml_dpow_ha_data_internal+27264]
        vmovsd    xmm4, QWORD PTR [__svml_dpow_ha_data_internal+27328]
        vmulsd    xmm2, xmm5, xmm5
        vfmadd213sd xmm4, xmm15, QWORD PTR [__svml_dpow_ha_data_internal+27392]
        vmovsd    xmm9, QWORD PTR [__svml_dpow_ha_data_internal+27456]
        vmulsd    xmm14, xmm14, xmm2
        vmulsd    xmm6, xmm4, xmm5
        vmulsd    xmm15, xmm9, xmm15
        movsxd    r9, r9d
        vmulsd    xmm3, xmm14, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r9]
        vmulsd    xmm8, xmm6, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r9]
        vmulsd    xmm2, xmm15, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r9]
        vaddsd    xmm7, xmm3, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24712+rax+r9]
        vaddsd    xmm13, xmm7, xmm8
        vaddsd    xmm3, xmm13, xmm2
        vaddsd    xmm4, xmm3, QWORD PTR [imagerel(__svml_dpow_ha_data_internal)+24704+rax+r9]
        vmulsd    xmm2, xmm4, xmm10
        and       edx, 1
        jne       _B9_3

_B9_2::

        vmovups   xmm6, XMMWORD PTR [240+rsp]
        vmovapd   xmm0, xmm2
        vmovups   xmm7, XMMWORD PTR [272+rsp]
        vmovups   xmm8, XMMWORD PTR [256+rsp]
        vmovups   xmm9, XMMWORD PTR [352+rsp]
        vmovups   xmm10, XMMWORD PTR [304+rsp]
        vmovups   xmm13, XMMWORD PTR [336+rsp]
        vmovups   xmm14, XMMWORD PTR [320+rsp]
        vmovups   xmm15, XMMWORD PTR [288+rsp]
        mov       r13, QWORD PTR [232+rsp]
        add       rsp, 376
        ret

_B9_3::

        vmovsd    QWORD PTR [r13], xmm0
        vmovsd    QWORD PTR [64+r13], xmm1
        vmovsd    QWORD PTR [128+r13], xmm2
        jne       _B9_6

_B9_4::

        vmovsd    xmm2, QWORD PTR [128+r13]
        jmp       _B9_2

_B9_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]
        lea       r8, QWORD PTR [128+r13]

        call      __svml_dpow_ha_cout_rare_internal
        jmp       _B9_4
        ALIGN     16

_B9_7::

__svml_pow1_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_pow1_ha_l9_B1_B6:
	DD	1350145
	DD	1954970
	DD	1009805
	DD	1144960
	DD	1083505
	DD	1480803
	DD	1288277
	DD	1431624
	DD	1370170
	DD	1243185
	DD	3080459

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_1
	DD	imagerel _B9_7
	DD	imagerel _unwind___svml_pow1_ha_l9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST9:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_dpow_ha_cout_rare_internal

__svml_dpow_ha_cout_rare_internal	PROC

_B10_1::

        DB        243
        DB        15
        DB        30
        DB        250
L246::

        push      rbx
        push      rsi
        push      rdi
        push      r12
        push      r15
        push      rbp
        sub       rsp, 200
        xor       eax, eax
        movsd     xmm0, QWORD PTR [_vmldPowHATab+6824]
        movsd     xmm2, QWORD PTR [rcx]
        movsd     xmm3, QWORD PTR [rdx]
        mulsd     xmm2, xmm0
        mulsd     xmm3, xmm0
        movsd     QWORD PTR [48+rsp], xmm2
        movsd     QWORD PTR [104+rsp], xmm3
        movzx     r12d, WORD PTR [54+rsp]
        movzx     edx, WORD PTR [110+rsp]
        and       r12d, 32752
        movzx     r11d, BYTE PTR [55+rsp]
        and       edx, 32752
        mov       bl, BYTE PTR [111+rsp]
        and       r11d, 128
        and       bl, -128
        shr       r12d, 4
        shr       edx, 4
        shr       r11d, 7
        shr       bl, 7
        movups    XMMWORD PTR [144+rsp], xmm15
        movups    XMMWORD PTR [160+rsp], xmm14
        movups    XMMWORD PTR [176+rsp], xmm8
        movups    XMMWORD PTR [112+rsp], xmm7
        movups    XMMWORD PTR [128+rsp], xmm6
        test      DWORD PTR [52+rsp], 1048575
        jne       _B10_4

_B10_2::

        cmp       DWORD PTR [48+rsp], 0
        jne       _B10_4

_B10_3::

        mov       r10b, 1
        jmp       _B10_5

_B10_4::

        xor       r10b, r10b

_B10_5::

        mov       ecx, DWORD PTR [108+rsp]
        mov       esi, DWORD PTR [104+rsp]
        and       ecx, 1048575
        jne       _B10_8

_B10_6::

        test      esi, esi
        jne       _B10_8

_B10_7::

        mov       r9d, 1
        jmp       _B10_9

_B10_8::

        xor       r9d, r9d

_B10_9::

        mov       edi, edx
        xor       ebp, ebp
        cmp       edx, 2047
        setne     bpl
        shl       edi, 20
        or        edi, ecx
        mov       ecx, esi
        or        ecx, edi
        je        _B10_24

_B10_10::

        lea       ecx, DWORD PTR [-1023+rdx]
        cmp       edx, 1023
        jl        _B10_23

_B10_11::

        test      ebp, ebp
        je        _B10_23

_B10_12::

        cmp       ecx, 20
        jg        _B10_16

_B10_13::

        mov       r15d, edi
        shl       r15d, cl
        shl       r15d, 12
        or        r15d, esi
        je        _B10_15

_B10_14::

        xor       edi, edi
        jmp       _B10_21

_B10_15::

        lea       ecx, DWORD PTR [-1012+rdx]
        shl       edi, cl
        not       edi
        shr       edi, 31
        inc       edi
        jmp       _B10_21

_B10_16::

        cmp       ecx, 53
        jge       _B10_20

_B10_17::

        lea       ecx, DWORD PTR [-1012+rdx]
        shl       esi, cl
        test      esi, 2147483647
        jne       _B10_14

_B10_19::

        not       esi
        shr       esi, 31
        lea       edi, DWORD PTR [1+rsi]
        jmp       _B10_21

_B10_20::

        mov       edi, 2

_B10_21::

        test      r11d, r11d
        jne       _B10_29

_B10_22::

        cmp       r12d, 1023
        jne       _B10_29
        jmp       _B10_57

_B10_23::

        xor       edi, edi
        jmp       _B10_25

_B10_24::

        mov       edi, 2

_B10_25::

        test      r11d, r11d
        jne       _B10_27

_B10_26::

        cmp       r12d, 1023
        je        _B10_74

_B10_27::

        test      edx, edx
        jne       _B10_29

_B10_28::

        test      r9d, r9d
        jne       _B10_55

_B10_29::

        cmp       r12d, 2047
        je        _B10_31

_B10_30::

        mov       cl, 1
        jmp       _B10_32

_B10_31::

        xor       cl, cl

_B10_32::

        or        r10b, cl
        je        _B10_54

_B10_33::

        or        r9d, ebp
        je        _B10_54

_B10_34::

        movsd     xmm1, QWORD PTR [_vmldPowHATab+6816]
        ucomisd   xmm2, xmm1
        jp        _B10_35
        je        _B10_71

_B10_35::

        ucomisd   xmm2, QWORD PTR [_vmldPowHATab+6832]
        jp        _B10_36
        je        _B10_68

_B10_36::

        test      cl, cl
        je        _B10_47

_B10_37::

        test      ebp, ebp
        je        _B10_47

_B10_38::

        comisd    xmm2, xmm1
        ja        _B10_40

_B10_39::

        test      edi, edi
        je        _B10_46

_B10_40::

        lea       rcx, QWORD PTR [__ImageBase]
        and       edi, r11d
        movsd     QWORD PTR [48+rsp], xmm2
        and       BYTE PTR [55+rsp], 127
        movsd     xmm1, QWORD PTR [imagerel(_vmldPowHATab)+6824+rcx+rdi*8]
        xor       ecx, ecx
        test      r12d, r12d
        jne       _B10_42

_B10_41::

        movsd     xmm2, QWORD PTR [48+rsp]
        mov       ecx, -200
        mulsd     xmm2, QWORD PTR [_vmldPowHATab+6864]
        movsd     QWORD PTR [48+rsp], xmm2
        jmp       _B10_43

_B10_42::

        movsd     xmm2, QWORD PTR [48+rsp]

_B10_43::

        movsd     QWORD PTR [64+rsp], xmm2
        lea       r10, QWORD PTR [__ImageBase]
        movzx     edi, WORD PTR [70+rsp]
        mov       r11, r10
        and       edi, -32753
        mov       r15, r10
        add       edi, 16368
        pxor      xmm6, xmm6
        mov       WORD PTR [70+rsp], di
        mov       r12, r10
        mov       ebp, DWORD PTR [68+rsp]
        and       ebp, 1032192
        add       ebp, 16384
        shr       ebp, 15
        movsd     xmm5, QWORD PTR [imagerel(_vmldPowHATab)+r10+rbp*8]
        movzx     esi, WORD PTR [54+rsp]
        movaps    xmm8, xmm5
        and       esi, 32752
        add       ebp, ebp
        movsd     xmm2, QWORD PTR [64+rsp]
        shr       esi, 4
        mulsd     xmm8, xmm2
        mov       r9d, DWORD PTR [52+rsp]
        shl       esi, 20
        and       r9d, 1048575
        or        esi, r9d
        mov       r9, r10
        add       esi, -1072152576
        sar       esi, 20
        movsd     QWORD PTR [72+rsp], xmm8
        add       esi, ecx
        mov       ecx, DWORD PTR [76+rsp]
        and       ecx, 64512
        add       ecx, 1024
        shr       ecx, 11
        movsd     xmm15, QWORD PTR [imagerel(_vmldPowHATab)+792+r11+rcx*8]
        mulsd     xmm8, xmm15
        add       ecx, ecx
        cvtsi2sd  xmm6, esi
        mulsd     xmm5, xmm15
        movsd     QWORD PTR [80+rsp], xmm8
        mov       rsi, r10
        movsd     xmm4, QWORD PTR [_vmldPowHATab+6856]
        mov       edi, DWORD PTR [84+rsp]
        mulsd     xmm4, xmm2
        and       edi, 4080
        addsd     xmm6, QWORD PTR [imagerel(_vmldPowHATab)+264+rsi+rbp*8]
        add       edi, 16
        addsd     xmm6, QWORD PTR [imagerel(_vmldPowHATab)+1056+r9+rcx*8]
        shr       edi, 5
        movsd     xmm3, QWORD PTR [imagerel(_vmldPowHATab)+1584+r15+rdi*8]
        movsd     QWORD PTR [88+rsp], xmm4
        add       edi, edi
        mulsd     xmm8, xmm3
        mulsd     xmm5, xmm3
        movsd     xmm3, QWORD PTR [88+rsp]
        movaps    xmm14, xmm8
        movsd     xmm7, QWORD PTR [imagerel(_vmldPowHATab)+1064+r12+rcx*8]
        mov       rcx, r10
        subsd     xmm3, QWORD PTR [64+rsp]
        subsd     xmm14, QWORD PTR [_vmldPowHATab+6848]
        addsd     xmm7, QWORD PTR [imagerel(_vmldPowHATab)+2624+r10+rdi*8]
        addsd     xmm6, QWORD PTR [imagerel(_vmldPowHATab)+2616+rcx+rdi*8]
        addsd     xmm7, QWORD PTR [imagerel(_vmldPowHATab)+272+r11+rbp*8]
        movsd     QWORD PTR [96+rsp], xmm3
        movsd     xmm3, QWORD PTR [88+rsp]
        movsd     xmm15, QWORD PTR [96+rsp]
        subsd     xmm3, xmm15
        movsd     QWORD PTR [88+rsp], xmm3
        movsd     xmm4, QWORD PTR [88+rsp]
        subsd     xmm2, xmm4
        movsd     QWORD PTR [96+rsp], xmm2
        movsd     xmm2, QWORD PTR [_vmldPowHATab+6856]
        mulsd     xmm2, xmm5
        movsd     xmm3, QWORD PTR [88+rsp]
        movsd     xmm4, QWORD PTR [96+rsp]
        movsd     QWORD PTR [88+rsp], xmm2
        movsd     xmm15, QWORD PTR [88+rsp]
        subsd     xmm15, xmm5
        movsd     QWORD PTR [96+rsp], xmm15
        movsd     xmm15, QWORD PTR [88+rsp]
        movsd     xmm2, QWORD PTR [96+rsp]
        subsd     xmm15, xmm2
        movsd     QWORD PTR [88+rsp], xmm15
        movsd     xmm2, QWORD PTR [88+rsp]
        subsd     xmm5, xmm2
        movaps    xmm2, xmm3
        movsd     QWORD PTR [96+rsp], xmm5
        movsd     xmm5, QWORD PTR [88+rsp]
        mulsd     xmm2, xmm5
        mulsd     xmm5, xmm4
        subsd     xmm2, xmm8
        movaps    xmm8, xmm6
        addsd     xmm2, xmm5
        addsd     xmm8, xmm14
        movsd     xmm15, QWORD PTR [96+rsp]
        mulsd     xmm3, xmm15
        mulsd     xmm4, xmm15
        addsd     xmm2, xmm3
        movsd     QWORD PTR [88+rsp], xmm8
        addsd     xmm2, xmm4
        movsd     xmm3, QWORD PTR [88+rsp]
        subsd     xmm6, xmm3
        addsd     xmm6, xmm14
        movsd     QWORD PTR [96+rsp], xmm6
        movsd     xmm4, QWORD PTR [88+rsp]
        movsd     QWORD PTR [56+rsp], xmm4
        movzx     ecx, WORD PTR [62+rsp]
        and       ecx, 32752
        shr       ecx, 4
        add       ecx, edx
        movsd     xmm3, QWORD PTR [96+rsp]
        cmp       ecx, 2057
        jge       _B10_67

_B10_44::

        cmp       ecx, 1984
        jg        _B10_58

_B10_45::

        movsd     QWORD PTR [88+rsp], xmm0
        movsd     xmm0, QWORD PTR [88+rsp]
        addsd     xmm0, QWORD PTR [_vmldPowHATab+6808]
        movsd     QWORD PTR [88+rsp], xmm0
        movsd     xmm2, QWORD PTR [88+rsp]
        mulsd     xmm1, xmm2
        jmp       _B10_56

_B10_46::

        movsd     QWORD PTR [88+rsp], xmm1
        mov       eax, 1
        movsd     xmm1, QWORD PTR [88+rsp]
        movsd     xmm0, QWORD PTR [88+rsp]
        divsd     xmm1, xmm0
        movsd     QWORD PTR [88+rsp], xmm1
        movsd     xmm1, QWORD PTR [88+rsp]
        jmp       _B10_56

_B10_47::

        cmp       r12d, 1023
        jge       _B10_51

_B10_48::

        test      bl, bl
        je        _B10_56

_B10_49::

        movaps    xmm1, xmm3
        mulsd     xmm1, xmm3
        jmp       _B10_56

_B10_51::

        test      bl, bl
        je        _B10_53

_B10_52::

        lea       rdx, QWORD PTR [__ImageBase]
        and       edi, r11d
        mulsd     xmm1, QWORD PTR [imagerel(_vmldPowHATab)+6824+rdx+rdi*8]
        jmp       _B10_56

_B10_53::

        mulsd     xmm2, xmm2
        lea       rdx, QWORD PTR [__ImageBase]
        mulsd     xmm2, xmm3
        and       edi, r11d
        movaps    xmm1, xmm2
        mulsd     xmm1, QWORD PTR [imagerel(_vmldPowHATab)+6824+rdx+rdi*8]
        jmp       _B10_56

_B10_54::

        movaps    xmm1, xmm2
        addsd     xmm1, xmm3
        jmp       _B10_56

_B10_55::

        mov       rdx, QWORD PTR [_vmldPowHATab+6824]
        addsd     xmm2, xmm3
        movsd     QWORD PTR [88+rsp], xmm2
        mov       QWORD PTR [96+rsp], rdx
        mov       cl, BYTE PTR [95+rsp]
        mov       bl, BYTE PTR [103+rsp]
        and       cl, -128
        and       bl, 127
        or        bl, cl
        mov       BYTE PTR [103+rsp], bl
        movsd     xmm1, QWORD PTR [96+rsp]
        movsd     xmm0, QWORD PTR [96+rsp]
        mulsd     xmm1, xmm0

_B10_56::

        movups    xmm6, XMMWORD PTR [128+rsp]
        movups    xmm7, XMMWORD PTR [112+rsp]
        movups    xmm8, XMMWORD PTR [176+rsp]
        movups    xmm14, XMMWORD PTR [160+rsp]
        movups    xmm15, XMMWORD PTR [144+rsp]
        movsd     QWORD PTR [r8], xmm1
        add       rsp, 200
        pop       rbp
        pop       r15
        pop       r12
        pop       rdi
        pop       rsi
        pop       rbx
        ret

_B10_57::

        test      r10b, r10b
        je        _B10_30
        jmp       _B10_55

_B10_58::

        lea       rdx, QWORD PTR [_vmldPowHATab+6752]
        movsd     xmm8, QWORD PTR [rdx]
        lea       rcx, QWORD PTR [__ImageBase]
        movsd     xmm15, QWORD PTR [56+rsp]
        mov       rbx, rcx
        addsd     xmm14, xmm2
        addsd     xmm2, xmm3
        mulsd     xmm8, xmm14
        addsd     xmm7, xmm2
        addsd     xmm8, QWORD PTR [-8+rdx]
        mulsd     xmm8, xmm14
        movaps    xmm6, xmm7
        addsd     xmm8, QWORD PTR [-16+rdx]
        mulsd     xmm8, xmm14
        addsd     xmm8, QWORD PTR [-24+rdx]
        mulsd     xmm8, xmm14
        addsd     xmm4, xmm8
        lea       rbp, QWORD PTR [88+rsp]
        movsd     QWORD PTR [rbp], xmm4
        movsd     xmm14, QWORD PTR [rbp]
        subsd     xmm15, xmm14
        addsd     xmm15, xmm8
        lea       rsi, QWORD PTR [96+rsp]
        movsd     QWORD PTR [rsi], xmm15
        movsd     xmm3, QWORD PTR [rbp]
        movsd     xmm4, QWORD PTR [rsi]
        addsd     xmm6, xmm3
        movsd     QWORD PTR [rbp], xmm6
        movsd     xmm2, QWORD PTR [rbp]
        subsd     xmm3, xmm2
        addsd     xmm3, xmm7
        movsd     xmm7, QWORD PTR [104+rdx]
        movsd     QWORD PTR [rsi], xmm3
        movsd     xmm8, QWORD PTR [rbp]
        mulsd     xmm7, xmm8
        movsd     xmm6, QWORD PTR [rsi]
        movsd     QWORD PTR [rbp], xmm7
        addsd     xmm6, xmm4
        movsd     xmm2, QWORD PTR [rbp]
        lea       rdi, QWORD PTR [104+rsp]
        mulsd     xmm6, QWORD PTR [rdi]
        subsd     xmm2, xmm8
        movsd     QWORD PTR [rsi], xmm2
        movsd     xmm4, QWORD PTR [rbp]
        movsd     xmm3, QWORD PTR [rsi]
        subsd     xmm4, xmm3
        movsd     QWORD PTR [rbp], xmm4
        movsd     xmm5, QWORD PTR [rbp]
        subsd     xmm8, xmm5
        movsd     QWORD PTR [rsi], xmm8
        movsd     xmm8, QWORD PTR [rdi]
        movaps    xmm14, xmm8
        mulsd     xmm14, QWORD PTR [104+rdx]
        movsd     xmm4, QWORD PTR [rbp]
        movsd     xmm5, QWORD PTR [rsi]
        movsd     QWORD PTR [rbp], xmm14
        movsd     xmm15, QWORD PTR [rbp]
        subsd     xmm15, QWORD PTR [rdi]
        movsd     QWORD PTR [rsi], xmm15
        movsd     xmm2, QWORD PTR [rbp]
        movsd     xmm7, QWORD PTR [rsi]
        subsd     xmm2, xmm7
        movsd     QWORD PTR [rbp], xmm2
        movsd     xmm3, QWORD PTR [rbp]
        subsd     xmm8, xmm3
        movsd     QWORD PTR [rsi], xmm8
        movsd     xmm14, QWORD PTR [rbp]
        movsd     xmm7, QWORD PTR [rsi]
        movaps    xmm2, xmm14
        movaps    xmm3, xmm7
        mulsd     xmm3, xmm5
        mulsd     xmm14, xmm5
        mulsd     xmm2, xmm4
        mulsd     xmm4, xmm7
        addsd     xmm3, xmm14
        addsd     xmm3, xmm4
        movsd     xmm4, QWORD PTR [88+rdx]
        addsd     xmm4, xmm2
        addsd     xmm6, xmm3
        movsd     QWORD PTR [rbp], xmm4
        movsd     xmm5, QWORD PTR [rbp]
        movsd     xmm3, QWORD PTR [40+rdx]
        mov       edx, DWORD PTR [rbp]
        mov       esi, edx
        and       edx, 127
        subsd     xmm5, QWORD PTR [_vmldPowHATab+6840]
        movsd     QWORD PTR [32+rsp], xmm5
        add       edx, edx
        movsd     xmm15, QWORD PTR [32+rsp]
        movsd     xmm4, QWORD PTR [imagerel(_vmldPowHATab)+4680+rcx+rdx*8]
        subsd     xmm2, xmm15
        movsd     QWORD PTR [40+rsp], xmm2
        movsd     xmm2, QWORD PTR [40+rsp]
        sar       esi, 7
        addsd     xmm6, xmm2
        mulsd     xmm3, xmm6
        lea       rcx, QWORD PTR [_vmldPowHATab+6784]
        addsd     xmm3, QWORD PTR [rcx]
        mulsd     xmm3, xmm6
        addsd     xmm3, QWORD PTR [-8+rcx]
        mulsd     xmm3, xmm6
        addsd     xmm3, QWORD PTR [-16+rcx]
        mulsd     xmm3, xmm6
        addsd     xmm3, QWORD PTR [-24+rcx]
        mulsd     xmm3, xmm6
        mulsd     xmm3, xmm4
        addsd     xmm3, QWORD PTR [imagerel(_vmldPowHATab)+4688+rbx+rdx*8]
        movaps    xmm5, xmm3
        addsd     xmm5, xmm4
        movsd     QWORD PTR [48+rsp], xmm5
        movzx     ecx, WORD PTR [54+rsp]
        mov       ebp, ecx
        and       ebp, 32752
        shr       ebp, 4
        lea       edx, DWORD PTR [-1023+rsi+rbp]
        cmp       edx, 1024
        jge       _B10_66

_B10_59::

        cmp       edx, -1022
        jl        _B10_61

_B10_60::

        and       ecx, -32753
        lea       edx, DWORD PTR [1023+rdx]
        and       edx, 2047
        shl       edx, 4
        or        ecx, edx
        mov       WORD PTR [54+rsp], cx
        movsd     xmm0, QWORD PTR [48+rsp]
        mulsd     xmm1, xmm0
        movsd     QWORD PTR [48+rsp], xmm1
        jmp       _B10_56

_B10_61::

        cmp       edx, -1032
        jl        _B10_63

_B10_62::

        lea       rcx, QWORD PTR [88+rsp]
        movsd     QWORD PTR [rcx], xmm5
        add       esi, 1223
        movsd     xmm2, QWORD PTR [rcx]
        and       esi, 2047
        lea       rbx, QWORD PTR [_vmldPowHATab+6824]
        mov       rdx, QWORD PTR [rbx]
        subsd     xmm4, xmm2
        mov       QWORD PTR [56+rsp], rdx
        addsd     xmm4, xmm3
        lea       rbp, QWORD PTR [96+rsp]
        movsd     QWORD PTR [rbp], xmm4
        movsd     xmm8, QWORD PTR [rcx]
        movsd     xmm3, QWORD PTR [32+rbx]
        mulsd     xmm3, xmm8
        movsd     xmm0, QWORD PTR [rbp]
        movsd     QWORD PTR [rcx], xmm3
        movsd     xmm4, QWORD PTR [rcx]
        shr       rdx, 48
        subsd     xmm4, xmm8
        movsd     QWORD PTR [rbp], xmm4
        and       edx, -32753
        movsd     xmm6, QWORD PTR [rcx]
        movsd     xmm5, QWORD PTR [rbp]
        shl       esi, 4
        subsd     xmm6, xmm5
        movsd     QWORD PTR [rcx], xmm6
        or        edx, esi
        lea       rsi, QWORD PTR [88+rsp]
        movsd     xmm7, QWORD PTR [rsi]
        mov       WORD PTR [62+rsp], dx
        subsd     xmm8, xmm7
        movsd     QWORD PTR [rbp], xmm8
        movsd     xmm2, QWORD PTR [rsi]
        movsd     xmm14, QWORD PTR [rbp]
        movsd     xmm15, QWORD PTR [48+rbx]
        addsd     xmm0, xmm14
        mulsd     xmm1, xmm15
        movsd     xmm14, QWORD PTR [56+rsp]
        mulsd     xmm2, xmm14
        mulsd     xmm0, xmm14
        mov       rcx, QWORD PTR [-16+rbx]
        addsd     xmm2, xmm0
        mov       QWORD PTR [rsi], rcx
        lea       rcx, QWORD PTR [88+rsp]
        movsd     xmm3, QWORD PTR [rcx]
        movsd     xmm0, QWORD PTR [rcx]
        mulsd     xmm3, xmm0
        mulsd     xmm1, xmm2
        movsd     QWORD PTR [rcx], xmm3
        movsd     xmm4, QWORD PTR [rcx]
        addsd     xmm1, xmm4
        movsd     QWORD PTR [48+rsp], xmm1
        jmp       _B10_56

_B10_63::

        cmp       edx, -1084
        jl        _B10_65

_B10_64::

        add       esi, 1223
        and       esi, 2047
        lea       rcx, QWORD PTR [_vmldPowHATab+6830]
        movzx     edx, WORD PTR [rcx]
        shl       esi, 4
        and       edx, -32753
        movsd     QWORD PTR [56+rsp], xmm0
        or        edx, esi
        mov       WORD PTR [62+rsp], dx
        movsd     xmm2, QWORD PTR [42+rcx]
        movsd     xmm0, QWORD PTR [56+rsp]
        mov       rcx, QWORD PTR [-22+rcx]
        mulsd     xmm1, xmm2
        mulsd     xmm5, xmm0
        mov       QWORD PTR [88+rsp], rcx
        lea       rcx, QWORD PTR [88+rsp]
        movsd     xmm4, QWORD PTR [rcx]
        movsd     xmm3, QWORD PTR [rcx]
        mulsd     xmm4, xmm3
        mulsd     xmm1, xmm5
        movsd     QWORD PTR [rcx], xmm4
        movsd     xmm5, QWORD PTR [rcx]
        addsd     xmm1, xmm5
        movsd     QWORD PTR [48+rsp], xmm1
        jmp       _B10_56

_B10_65::

        mov       rdx, QWORD PTR [_vmldPowHATab+6808]
        mov       QWORD PTR [88+rsp], rdx
        lea       rdx, QWORD PTR [88+rsp]
        movsd     xmm2, QWORD PTR [rdx]
        movsd     xmm0, QWORD PTR [rdx]
        mulsd     xmm2, xmm0
        movsd     QWORD PTR [rdx], xmm2
        movsd     xmm3, QWORD PTR [rdx]
        mulsd     xmm1, xmm3
        movsd     QWORD PTR [48+rsp], xmm1
        jmp       _B10_56

_B10_66::

        mov       rdx, QWORD PTR [_vmldPowHATab+6800]
        mov       QWORD PTR [88+rsp], rdx
        lea       rdx, QWORD PTR [88+rsp]
        movsd     xmm2, QWORD PTR [rdx]
        movsd     xmm0, QWORD PTR [rdx]
        mulsd     xmm2, xmm0
        movsd     QWORD PTR [rdx], xmm2
        movsd     xmm3, QWORD PTR [rdx]
        mulsd     xmm1, xmm3
        movsd     QWORD PTR [48+rsp], xmm1
        jmp       _B10_56

_B10_67::

        mov       dl, BYTE PTR [63+rsp]
        lea       rcx, QWORD PTR [__ImageBase]
        and       dl, -128
        shr       dl, 7
        xor       bl, dl
        movzx     ebx, bl
        movsd     xmm0, QWORD PTR [imagerel(_vmldPowHATab)+6800+rcx+rbx*8]
        mulsd     xmm0, xmm0
        mulsd     xmm0, xmm1
        movaps    xmm1, xmm0
        jmp       _B10_56

_B10_68::

        test      edi, edi
        jne       _B10_70

_B10_69::

        test      ebp, ebp
        jne       _B10_36

_B10_70::

        lea       rdx, QWORD PTR [__ImageBase]
        and       edi, 1
        movsd     xmm1, QWORD PTR [imagerel(_vmldPowHATab)+6824+rdx+rdi*8]
        jmp       _B10_56

_B10_71::

        mulsd     xmm2, xmm2
        test      bl, bl
        je        _B10_73

_B10_72::

        lea       rax, QWORD PTR [__ImageBase]
        and       edi, r11d
        movsd     xmm1, QWORD PTR [imagerel(_vmldPowHATab)+6824+rax+rdi*8]
        mov       eax, 1
        divsd     xmm1, xmm2
        jmp       _B10_56

_B10_73::

        lea       rdx, QWORD PTR [__ImageBase]
        and       edi, r11d
        movsd     xmm1, QWORD PTR [imagerel(_vmldPowHATab)+6824+rdx+rdi*8]
        mulsd     xmm1, xmm2
        jmp       _B10_56

_B10_74::

        test      r10b, r10b
        jne       _B10_55

_B10_75::

        test      edx, edx
        jne       _B10_30

_B10_76::

        test      r9d, r9d
        je        _B10_30
        jmp       _B10_55
        ALIGN     16

_B10_77::

__svml_dpow_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_dpow_ha_cout_rare_internal_B1_B76:
	DD	1219329
	DD	551067
	DD	489619
	DD	755854
	DD	714885
	DD	653436
	DD	1638675
	DD	4027273228
	DD	1879556105
	DD	805658630

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B10_1
	DD	imagerel _B10_77
	DD	imagerel _unwind___svml_dpow_ha_cout_rare_internal_B1_B76

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_dpow_ha_data_internal_avx512
__svml_dpow_ha_data_internal_avx512	DD	0
	DD	0
	DD	1972240384
	DD	3215375059
	DD	4207476736
	DD	3216401398
	DD	2363129856
	DD	3217067096
	DD	972816384
	DD	3217408026
	DD	766836736
	DD	3217739614
	DD	3103948800
	DD	3218062358
	DD	2869821440
	DD	3218228231
	DD	1271726080
	DD	3218381432
	DD	3449618432
	DD	3218530849
	DD	2300510208
	DD	3218676666
	DD	4147675136
	DD	3218819051
	DD	3761438720
	DD	3218958163
	DD	1758134272
	DD	3219094149
	DD	14041088
	DD	3219177733
	DD	513138688
	DD	3219242801
	DD	1904279552
	DD	1071288313
	DD	180338688
	DD	1071163544
	DD	1751498752
	DD	1071041295
	DD	2999894016
	DD	1070921467
	DD	3815833600
	DD	1070803966
	DD	1399062528
	DD	1070688704
	DD	2835742720
	DD	1070555096
	DD	2818572288
	DD	1070333031
	DD	180322304
	DD	1070114968
	DD	704610304
	DD	1069900764
	DD	3265986560
	DD	1069690285
	DD	3908239360
	DD	1069419290
	DD	2530213888
	DD	1069012484
	DD	2785017856
	DD	1068612402
	DD	3386507264
	DD	1067938708
	DD	2250244096
	DD	1066877934
	DD	0
	DD	0
	DD	650173971
	DD	3177165030
	DD	3428024929
	DD	3174241916
	DD	1628324029
	DD	1026060711
	DD	804943611
	DD	1028963376
	DD	518075456
	DD	1027828752
	DD	1462134616
	DD	1028126172
	DD	384118417
	DD	3174884873
	DD	1227618047
	DD	3176893182
	DD	446961290
	DD	3175726255
	DD	2998207852
	DD	3176597684
	DD	2742536172
	DD	3173319968
	DD	3242321520
	DD	1029042433
	DD	1690697745
	DD	3174775608
	DD	4137858450
	DD	1027958429
	DD	2514005062
	DD	1029694520
	DD	804943611
	DD	1027914800
	DD	2871266960
	DD	3173412044
	DD	3679462403
	DD	1027724294
	DD	2476829589
	DD	1026974179
	DD	1572243234
	DD	3176241050
	DD	2514550597
	DD	3175960347
	DD	1207415416
	DD	1029642824
	DD	531120703
	DD	3174459378
	DD	894287639
	DD	1029609779
	DD	1133539114
	DD	1029069062
	DD	1763539348
	DD	1029327721
	DD	1658032750
	DD	3171241178
	DD	825146242
	DD	3176213734
	DD	831162967
	DD	1028990787
	DD	1128763360
	DD	3176457556
	DD	896504796
	DD	3175699769
	DD	0
	DD	1072693248
	DD	1828292879
	DD	1072739672
	DD	1014845819
	DD	1072788152
	DD	1853186616
	DD	1072838778
	DD	171030293
	DD	1072891646
	DD	1276261410
	DD	1072946854
	DD	3577096743
	DD	1073004506
	DD	3712504873
	DD	1073064711
	DD	1719614413
	DD	1073127582
	DD	1944781191
	DD	1073193236
	DD	1110089947
	DD	1073261797
	DD	2191782032
	DD	1073333393
	DD	2572866477
	DD	1073408159
	DD	3716502172
	DD	1073486235
	DD	3707479175
	DD	1073567768
	DD	2728693978
	DD	1073652911
	DD	0
	DD	0
	DD	1568897901
	DD	1016568486
	DD	3936719688
	DD	3162512149
	DD	3819481236
	DD	1016499965
	DD	1303423926
	DD	1015238005
	DD	2804567149
	DD	1015390024
	DD	3145379760
	DD	1014403278
	DD	3793507337
	DD	1016095713
	DD	3210617384
	DD	3163796463
	DD	3108873501
	DD	3162190556
	DD	3253791412
	DD	1015920431
	DD	730975783
	DD	1014083580
	DD	2462790535
	DD	1015814775
	DD	816778419
	DD	1014197934
	DD	2789017511
	DD	1014276997
	DD	2413007344
	DD	3163551506
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
	DD	1073217536
	DD	0
	DD	1073217536
	DD	0
	DD	1073217536
	DD	0
	DD	1073217536
	DD	0
	DD	1073217536
	DD	0
	DD	1073217536
	DD	0
	DD	1073217536
	DD	0
	DD	1073217536
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
	DD	1697350398
	DD	1074206023
	DD	1697350398
	DD	1074206023
	DD	1697350398
	DD	1074206023
	DD	1697350398
	DD	1074206023
	DD	1697350398
	DD	1074206023
	DD	1697350398
	DD	1074206023
	DD	1697350398
	DD	1074206023
	DD	1697350398
	DD	1074206023
	DD	1132869487
	DD	3227679289
	DD	1132869487
	DD	3227679289
	DD	1132869487
	DD	3227679289
	DD	1132869487
	DD	3227679289
	DD	1132869487
	DD	3227679289
	DD	1132869487
	DD	3227679289
	DD	1132869487
	DD	3227679289
	DD	1132869487
	DD	3227679289
	DD	4168171985
	DD	1079281468
	DD	4168171985
	DD	1079281468
	DD	4168171985
	DD	1079281468
	DD	4168171985
	DD	1079281468
	DD	4168171985
	DD	1079281468
	DD	4168171985
	DD	1079281468
	DD	4168171985
	DD	1079281468
	DD	4168171985
	DD	1079281468
	DD	891875265
	DD	3225883975
	DD	891875265
	DD	3225883975
	DD	891875265
	DD	3225883975
	DD	891875265
	DD	3225883975
	DD	891875265
	DD	3225883975
	DD	891875265
	DD	3225883975
	DD	891875265
	DD	3225883975
	DD	891875265
	DD	3225883975
	DD	132470805
	DD	1077567862
	DD	132470805
	DD	1077567862
	DD	132470805
	DD	1077567862
	DD	132470805
	DD	1077567862
	DD	132470805
	DD	1077567862
	DD	132470805
	DD	1077567862
	DD	132470805
	DD	1077567862
	DD	132470805
	DD	1077567862
	DD	3694883233
	DD	3224291081
	DD	3694883233
	DD	3224291081
	DD	3694883233
	DD	3224291081
	DD	3694883233
	DD	3224291081
	DD	3694883233
	DD	3224291081
	DD	3694883233
	DD	3224291081
	DD	3694883233
	DD	3224291081
	DD	3694883233
	DD	3224291081
	DD	1357904862
	DD	1076000620
	DD	1357904862
	DD	1076000620
	DD	1357904862
	DD	1076000620
	DD	1357904862
	DD	1076000620
	DD	1357904862
	DD	1076000620
	DD	1357904862
	DD	1076000620
	DD	1357904862
	DD	1076000620
	DD	1357904862
	DD	1076000620
	DD	1697350396
	DD	3222738247
	DD	1697350396
	DD	3222738247
	DD	1697350396
	DD	3222738247
	DD	1697350396
	DD	3222738247
	DD	1697350396
	DD	3222738247
	DD	1697350396
	DD	3222738247
	DD	1697350396
	DD	3222738247
	DD	1697350396
	DD	3222738247
	DD	3694789629
	DD	1074710281
	DD	3694789629
	DD	1074710281
	DD	3694789629
	DD	1074710281
	DD	3694789629
	DD	1074710281
	DD	3694789629
	DD	1074710281
	DD	3694789629
	DD	1074710281
	DD	3694789629
	DD	1074710281
	DD	3694789629
	DD	1074710281
	DD	3340305463
	DD	3162994905
	DD	3340305463
	DD	3162994905
	DD	3340305463
	DD	3162994905
	DD	3340305463
	DD	3162994905
	DD	3340305463
	DD	3162994905
	DD	3340305463
	DD	3162994905
	DD	3340305463
	DD	3162994905
	DD	3340305463
	DD	3162994905
	DD	1891065104
	DD	1015511007
	DD	1891065104
	DD	1015511007
	DD	1891065104
	DD	1015511007
	DD	1891065104
	DD	1015511007
	DD	1891065104
	DD	1015511007
	DD	1891065104
	DD	1015511007
	DD	1891065104
	DD	1015511007
	DD	1891065104
	DD	1015511007
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	16368
	DD	1123549184
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4294967295
	DD	3221225471
	DD	4119604569
	DD	1059365335
	DD	4119604569
	DD	1059365335
	DD	4119604569
	DD	1059365335
	DD	4119604569
	DD	1059365335
	DD	4119604569
	DD	1059365335
	DD	4119604569
	DD	1059365335
	DD	4119604569
	DD	1059365335
	DD	4119604569
	DD	1059365335
	DD	662950521
	DD	1062590279
	DD	662950521
	DD	1062590279
	DD	662950521
	DD	1062590279
	DD	662950521
	DD	1062590279
	DD	662950521
	DD	1062590279
	DD	662950521
	DD	1062590279
	DD	662950521
	DD	1062590279
	DD	662950521
	DD	1062590279
	DD	454355882
	DD	1065595565
	DD	454355882
	DD	1065595565
	DD	454355882
	DD	1065595565
	DD	454355882
	DD	1065595565
	DD	454355882
	DD	1065595565
	DD	454355882
	DD	1065595565
	DD	454355882
	DD	1065595565
	DD	454355882
	DD	1065595565
	DD	3568144057
	DD	1068264200
	DD	3568144057
	DD	1068264200
	DD	3568144057
	DD	1068264200
	DD	3568144057
	DD	1068264200
	DD	3568144057
	DD	1068264200
	DD	3568144057
	DD	1068264200
	DD	3568144057
	DD	1068264200
	DD	3568144057
	DD	1068264200
	DD	4286862669
	DD	1070514109
	DD	4286862669
	DD	1070514109
	DD	4286862669
	DD	1070514109
	DD	4286862669
	DD	1070514109
	DD	4286862669
	DD	1070514109
	DD	4286862669
	DD	1070514109
	DD	4286862669
	DD	1070514109
	DD	4286862669
	DD	1070514109
	DD	4277811595
	DD	1072049730
	DD	4277811595
	DD	1072049730
	DD	4277811595
	DD	1072049730
	DD	4277811595
	DD	1072049730
	DD	4277811595
	DD	1072049730
	DD	4277811595
	DD	1072049730
	DD	4277811595
	DD	1072049730
	DD	4277811595
	DD	1072049730
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
	DD	248
	DD	0
	DD	248
	DD	0
	DD	248
	DD	0
	DD	248
	DD	0
	DD	248
	DD	0
	DD	248
	DD	0
	DD	248
	DD	0
	DD	248
	DD	0
	DD	120
	DD	0
	DD	120
	DD	0
	DD	120
	DD	0
	DD	120
	DD	0
	DD	120
	DD	0
	DD	120
	DD	0
	DD	120
	DD	0
	DD	120
	DD	0
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
	DD	0
	DD	1083173888
	DD	0
	DD	1083173888
	DD	0
	DD	1083173888
	DD	0
	DD	1083173888
	DD	0
	DD	1083173888
	DD	0
	DD	1083173888
	DD	0
	DD	1083173888
	DD	0
	DD	1083173888
	DD	0
	DD	2146435072
	DD	0
	DD	2146435072
	DD	0
	DD	2146435072
	DD	0
	DD	2146435072
	DD	0
	DD	2146435072
	DD	0
	DD	2146435072
	DD	0
	DD	2146435072
	DD	0
	DD	2146435072
	PUBLIC __svml_dpow_ha_data_internal
__svml_dpow_ha_data_internal	DD	1073741824
	DD	1073157447
	DD	1073741824
	DD	1073155971
	DD	1073741824
	DD	1073154498
	DD	1073741824
	DD	1073153028
	DD	0
	DD	1073151561
	DD	2147483648
	DD	1073150096
	DD	0
	DD	1073148635
	DD	1073741824
	DD	1073147176
	DD	1073741824
	DD	1073145720
	DD	1073741824
	DD	1073144267
	DD	0
	DD	1073142817
	DD	2147483648
	DD	1073141369
	DD	3221225472
	DD	1073139924
	DD	3221225472
	DD	1073138482
	DD	3221225472
	DD	1073137043
	DD	2147483648
	DD	1073135607
	DD	3221225472
	DD	1073134173
	DD	0
	DD	1073132743
	DD	3221225472
	DD	1073131314
	DD	2147483648
	DD	1073129889
	DD	0
	DD	1073128467
	DD	0
	DD	1073127047
	DD	3221225472
	DD	1073125629
	DD	1073741824
	DD	1073124215
	DD	2147483648
	DD	1073122803
	DD	2147483648
	DD	1073121394
	DD	0
	DD	1073119988
	DD	1073741824
	DD	1073118584
	DD	1073741824
	DD	1073117183
	DD	3221225472
	DD	1073115784
	DD	1073741824
	DD	1073114389
	DD	0
	DD	1073112996
	DD	2147483648
	DD	1073111605
	DD	3221225472
	DD	1073110217
	DD	2147483648
	DD	1073108832
	DD	0
	DD	1073107450
	DD	0
	DD	1073106070
	DD	2147483648
	DD	1073104692
	DD	3221225472
	DD	1073103317
	DD	2147483648
	DD	1073101945
	DD	0
	DD	1073100576
	DD	0
	DD	1073099209
	DD	2147483648
	DD	1073097844
	DD	2147483648
	DD	1073096482
	DD	1073741824
	DD	1073095123
	DD	1073741824
	DD	1073093766
	DD	0
	DD	1073092412
	DD	1073741824
	DD	1073091060
	DD	0
	DD	1073089711
	DD	1073741824
	DD	1073088364
	DD	0
	DD	1073087020
	DD	2147483648
	DD	1073085678
	DD	1073741824
	DD	1073084339
	DD	2147483648
	DD	1073083002
	DD	1073741824
	DD	1073081668
	DD	1073741824
	DD	1073080336
	DD	0
	DD	1073079007
	DD	1073741824
	DD	1073077680
	DD	3221225472
	DD	1073076355
	DD	3221225472
	DD	1073075033
	DD	1073741824
	DD	1073073714
	DD	1073741824
	DD	1073072397
	DD	2147483648
	DD	1073071082
	DD	1073741824
	DD	1073069770
	DD	2147483648
	DD	1073068460
	DD	0
	DD	1073067153
	DD	0
	DD	1073065848
	DD	2147483648
	DD	1073064545
	DD	1073741824
	DD	1073063245
	DD	1073741824
	DD	1073061947
	DD	3221225472
	DD	1073060651
	DD	3221225472
	DD	1073059358
	DD	3221225472
	DD	1073058067
	DD	2147483648
	DD	1073056779
	DD	1073741824
	DD	1073055493
	DD	3221225472
	DD	1073054209
	DD	1073741824
	DD	1073052928
	DD	1073741824
	DD	1073051649
	DD	2147483648
	DD	1073050372
	DD	0
	DD	1073049098
	DD	3221225472
	DD	1073047825
	DD	0
	DD	1073046556
	DD	2147483648
	DD	1073045288
	DD	1073741824
	DD	1073044023
	DD	1073741824
	DD	1073042760
	DD	3221225472
	DD	1073041499
	DD	1073741824
	DD	1073040241
	DD	0
	DD	1073038985
	DD	1073741824
	DD	1073037731
	DD	2147483648
	DD	1073036479
	DD	1073741824
	DD	1073035230
	DD	0
	DD	1073033983
	DD	1073741824
	DD	1073032738
	DD	2147483648
	DD	1073031495
	DD	0
	DD	1073030255
	DD	3221225472
	DD	1073029016
	DD	3221225472
	DD	1073027780
	DD	0
	DD	1073026547
	DD	1073741824
	DD	1073025315
	DD	0
	DD	1073024086
	DD	3221225472
	DD	1073022858
	DD	3221225472
	DD	1073021633
	DD	3221225472
	DD	1073020410
	DD	0
	DD	1073019190
	DD	2147483648
	DD	1073017971
	DD	1073741824
	DD	1073016755
	DD	0
	DD	1073015541
	DD	3221225472
	DD	1073014328
	DD	3221225472
	DD	1073013118
	DD	0
	DD	1073011911
	DD	2147483648
	DD	1073010705
	DD	3221225472
	DD	1073009501
	DD	2147483648
	DD	1073008300
	DD	0
	DD	1073007101
	DD	3221225472
	DD	1073005903
	DD	3221225472
	DD	1073004708
	DD	3221225472
	DD	1073003515
	DD	3221225472
	DD	1073002324
	DD	0
	DD	1073001136
	DD	1073741824
	DD	1072999949
	DD	2147483648
	DD	1072998764
	DD	0
	DD	1072997582
	DD	1073741824
	DD	1072996401
	DD	0
	DD	1072995223
	DD	2147483648
	DD	1072994046
	DD	0
	DD	1072992872
	DD	3221225472
	DD	1072991699
	DD	2147483648
	DD	1072990529
	DD	0
	DD	1072989361
	DD	3221225472
	DD	1072988194
	DD	3221225472
	DD	1072987030
	DD	2147483648
	DD	1072985868
	DD	1073741824
	DD	1072984708
	DD	0
	DD	1072983550
	DD	3221225472
	DD	1072982393
	DD	2147483648
	DD	1072981239
	DD	2147483648
	DD	1072980087
	DD	1073741824
	DD	1072978937
	DD	0
	DD	1072977789
	DD	3221225472
	DD	1072976642
	DD	1073741824
	DD	1072975498
	DD	0
	DD	1072974356
	DD	2147483648
	DD	1072973215
	DD	1073741824
	DD	1072972077
	DD	3221225472
	DD	1072970940
	DD	1073741824
	DD	1072969806
	DD	2147483648
	DD	1072968673
	DD	0
	DD	1072967543
	DD	1073741824
	DD	1072966414
	DD	2147483648
	DD	1072965287
	DD	2147483648
	DD	1072964162
	DD	2147483648
	DD	1072963039
	DD	2147483648
	DD	1072961918
	DD	1073741824
	DD	1072960799
	DD	0
	DD	1072959682
	DD	3221225472
	DD	1072958566
	DD	1073741824
	DD	1072957453
	DD	3221225472
	DD	1072956341
	DD	0
	DD	1072955232
	DD	0
	DD	1072954124
	DD	1073741824
	DD	1072953018
	DD	0
	DD	1072951914
	DD	3221225472
	DD	1072950811
	DD	2147483648
	DD	1072949711
	DD	0
	DD	1072948613
	DD	1073741824
	DD	1072947516
	DD	2147483648
	DD	1072946421
	DD	2147483648
	DD	1072945328
	DD	1073741824
	DD	1072944237
	DD	0
	DD	1072943148
	DD	2147483648
	DD	1072942060
	DD	3221225472
	DD	1072940974
	DD	0
	DD	1072939891
	DD	0
	DD	1072938809
	DD	3221225472
	DD	1072937728
	DD	1073741824
	DD	1072936650
	DD	2147483648
	DD	1072935573
	DD	3221225472
	DD	1072934498
	DD	2147483648
	DD	1072933425
	DD	1073741824
	DD	1072932354
	DD	3221225472
	DD	1072931284
	DD	0
	DD	1072930217
	DD	1073741824
	DD	1072929151
	DD	0
	DD	1072928087
	DD	2147483648
	DD	1072927024
	DD	3221225472
	DD	1072925963
	DD	0
	DD	1072924905
	DD	3221225472
	DD	1072923847
	DD	1073741824
	DD	1072922792
	DD	3221225472
	DD	1072921738
	DD	3221225472
	DD	1072920686
	DD	2147483648
	DD	1072919636
	DD	0
	DD	1072918588
	DD	1073741824
	DD	1072917541
	DD	1073741824
	DD	1072916496
	DD	3221225472
	DD	1072915452
	DD	1073741824
	DD	1072914411
	DD	1073741824
	DD	1072913371
	DD	0
	DD	1072912333
	DD	2147483648
	DD	1072911296
	DD	3221225472
	DD	1072910261
	DD	2147483648
	DD	1072909228
	DD	1073741824
	DD	1072908197
	DD	2147483648
	DD	1072907167
	DD	1073741824
	DD	1072906139
	DD	0
	DD	1072905113
	DD	1073741824
	DD	1072904088
	DD	0
	DD	1072903065
	DD	3221225472
	DD	1072902043
	DD	3221225472
	DD	1072901023
	DD	3221225472
	DD	1072900005
	DD	1073741824
	DD	1072898989
	DD	2147483648
	DD	1072897974
	DD	1073741824
	DD	1072896961
	DD	3221225472
	DD	1072895949
	DD	3221225472
	DD	1072894939
	DD	2147483648
	DD	1072893931
	DD	0
	DD	1072892925
	DD	3221225472
	DD	1072891919
	DD	2147483648
	DD	1072890916
	DD	3221225472
	DD	1072889914
	DD	2147483648
	DD	1072888914
	DD	3221225472
	DD	1072887915
	DD	0
	DD	1072886919
	DD	2147483648
	DD	1072885923
	DD	3221225472
	DD	1072884929
	DD	2147483648
	DD	1072883937
	DD	3221225472
	DD	1072882946
	DD	3221225472
	DD	1072881957
	DD	1073741824
	DD	1072880970
	DD	2147483648
	DD	1072879984
	DD	1073741824
	DD	1072879000
	DD	2147483648
	DD	1072878017
	DD	1073741824
	DD	1072877036
	DD	2147483648
	DD	1072876056
	DD	2147483648
	DD	1072875078
	DD	0
	DD	1072874102
	DD	0
	DD	1072873127
	DD	3221225472
	DD	1072872153
	DD	3221225472
	DD	1072871181
	DD	2147483648
	DD	1072870211
	DD	3221225472
	DD	1072869242
	DD	1073741824
	DD	1072868275
	DD	2147483648
	DD	1072867309
	DD	2147483648
	DD	1072866345
	DD	3221225472
	DD	1072865382
	DD	2147483648
	DD	1072864421
	DD	3221225472
	DD	1072863461
	DD	3221225472
	DD	1072862503
	DD	0
	DD	1072861547
	DD	3221225472
	DD	1072860591
	DD	1073741824
	DD	1072859638
	DD	0
	DD	1072858686
	DD	1073741824
	DD	1072857735
	DD	1073741824
	DD	1072856786
	DD	2147483648
	DD	1072855838
	DD	1073741824
	DD	1072854892
	DD	2147483648
	DD	1072853947
	DD	1073741824
	DD	1072853004
	DD	2147483648
	DD	1072852062
	DD	0
	DD	1072851122
	DD	1073741824
	DD	1072850183
	DD	3221225472
	DD	1072849245
	DD	3221225472
	DD	1072848309
	DD	1073741824
	DD	1072847375
	DD	1073741824
	DD	1072846442
	DD	3221225472
	DD	1072845510
	DD	2147483648
	DD	1072844580
	DD	3221225472
	DD	1072843651
	DD	2147483648
	DD	1072842724
	DD	3221225472
	DD	1072841798
	DD	1073741824
	DD	1072840874
	DD	1073741824
	DD	1072839951
	DD	3221225472
	DD	1072839029
	DD	2147483648
	DD	1072838109
	DD	3221225472
	DD	1072837190
	DD	2147483648
	DD	1072836273
	DD	2147483648
	DD	1072835357
	DD	0
	DD	1072834443
	DD	0
	DD	1072833530
	DD	1073741824
	DD	1072832618
	DD	0
	DD	1072831708
	DD	0
	DD	1072830799
	DD	2147483648
	DD	1072829891
	DD	1073741824
	DD	1072828985
	DD	2147483648
	DD	1072828080
	DD	1073741824
	DD	1072827177
	DD	1073741824
	DD	1072826275
	DD	3221225472
	DD	1072825374
	DD	2147483648
	DD	1072824475
	DD	2147483648
	DD	1072823577
	DD	0
	DD	1072822681
	DD	3221225472
	DD	1072821785
	DD	0
	DD	1072820892
	DD	3221225472
	DD	1072819999
	DD	2147483648
	DD	1072819108
	DD	3221225472
	DD	1072818218
	DD	2147483648
	DD	1072817330
	DD	2147483648
	DD	1072816443
	DD	3221225472
	DD	1072815557
	DD	2147483648
	DD	1072814673
	DD	2147483648
	DD	1072813790
	DD	3221225472
	DD	1072812908
	DD	1073741824
	DD	1072812028
	DD	1073741824
	DD	1072811149
	DD	2147483648
	DD	1072810271
	DD	1073741824
	DD	1072809395
	DD	1073741824
	DD	1072808520
	DD	1073741824
	DD	1072807646
	DD	0
	DD	1072806774
	DD	3221225472
	DD	1072805902
	DD	0
	DD	1072805033
	DD	2147483648
	DD	1072804164
	DD	1073741824
	DD	1072803297
	DD	1073741824
	DD	1072802431
	DD	2147483648
	DD	1072801566
	DD	1073741824
	DD	1072800703
	DD	1073741824
	DD	1072799841
	DD	2147483648
	DD	1072798980
	DD	0
	DD	1072798121
	DD	3221225472
	DD	1072797262
	DD	3221225472
	DD	1072796405
	DD	0
	DD	1072795550
	DD	3221225472
	DD	1072794695
	DD	2147483648
	DD	1072793842
	DD	3221225472
	DD	1072792990
	DD	0
	DD	1072792140
	DD	3221225472
	DD	1072791290
	DD	2147483648
	DD	1072790442
	DD	3221225472
	DD	1072789595
	DD	1073741824
	DD	1072788750
	DD	3221225472
	DD	1072787905
	DD	3221225472
	DD	1072787062
	DD	0
	DD	1072786221
	DD	1073741824
	DD	1072785380
	DD	0
	DD	1072784541
	DD	3221225472
	DD	1072783702
	DD	3221225472
	DD	1072782865
	DD	1073741824
	DD	1072782030
	DD	3221225472
	DD	1072781195
	DD	2147483648
	DD	1072780362
	DD	2147483648
	DD	1072779530
	DD	3221225472
	DD	1072778699
	DD	1073741824
	DD	1072777870
	DD	3221225472
	DD	1072777041
	DD	3221225472
	DD	1072776214
	DD	3221225472
	DD	1072775388
	DD	0
	DD	1072774564
	DD	2147483648
	DD	1072773740
	DD	0
	DD	1072772918
	DD	0
	DD	1072772097
	DD	0
	DD	1072771277
	DD	1073741824
	DD	1072770458
	DD	3221225472
	DD	1072769640
	DD	1073741824
	DD	1072768824
	DD	0
	DD	1072768009
	DD	0
	DD	1072767195
	DD	1073741824
	DD	1072766382
	DD	2147483648
	DD	1072765570
	DD	0
	DD	1072764760
	DD	3221225472
	DD	1072763950
	DD	3221225472
	DD	1072763142
	DD	3221225472
	DD	1072762335
	DD	3221225472
	DD	1072761529
	DD	1073741824
	DD	1072760725
	DD	3221225472
	DD	1072759921
	DD	2147483648
	DD	1072759119
	DD	1073741824
	DD	1072758318
	DD	1073741824
	DD	1072757518
	DD	1073741824
	DD	1072756719
	DD	2147483648
	DD	1072755921
	DD	0
	DD	1072755125
	DD	2147483648
	DD	1072754329
	DD	1073741824
	DD	1072753535
	DD	0
	DD	1072752742
	DD	0
	DD	1072751950
	DD	0
	DD	1072751159
	DD	1073741824
	DD	1072750369
	DD	2147483648
	DD	1072749580
	DD	0
	DD	1072748793
	DD	3221225472
	DD	1072748006
	DD	1073741824
	DD	1072747221
	DD	1073741824
	DD	1072746437
	DD	1073741824
	DD	1072745654
	DD	1073741824
	DD	1072744872
	DD	1073741824
	DD	1072744091
	DD	2147483648
	DD	1072743311
	DD	0
	DD	1072742533
	DD	2147483648
	DD	1072741755
	DD	0
	DD	1072740979
	DD	3221225472
	DD	1072740203
	DD	2147483648
	DD	1072739429
	DD	1073741824
	DD	1072738656
	DD	1073741824
	DD	1072737884
	DD	1073741824
	DD	1072737113
	DD	2147483648
	DD	1072736343
	DD	3221225472
	DD	1072735574
	DD	0
	DD	1072734807
	DD	2147483648
	DD	1072734040
	DD	3221225472
	DD	1072733274
	DD	2147483648
	DD	1072732510
	DD	0
	DD	1072731747
	DD	3221225472
	DD	1072730984
	DD	2147483648
	DD	1072730223
	DD	1073741824
	DD	1072729463
	DD	0
	DD	1072728704
	DD	0
	DD	1072727946
	DD	0
	DD	1072727189
	DD	0
	DD	1072726433
	DD	1073741824
	DD	1072725678
	DD	1073741824
	DD	1072724924
	DD	2147483648
	DD	1072724171
	DD	3221225472
	DD	1072723419
	DD	0
	DD	1072722669
	DD	2147483648
	DD	1072721919
	DD	3221225472
	DD	1072721170
	DD	1073741824
	DD	1072720423
	DD	3221225472
	DD	1072719676
	DD	1073741824
	DD	1072718931
	DD	3221225472
	DD	1072718186
	DD	1073741824
	DD	1072717443
	DD	0
	DD	1072716701
	DD	2147483648
	DD	1072715959
	DD	1073741824
	DD	1072715219
	DD	0
	DD	1072714480
	DD	2147483648
	DD	1072713741
	DD	1073741824
	DD	1072713004
	DD	0
	DD	1072712268
	DD	3221225472
	DD	1072711532
	DD	2147483648
	DD	1072710798
	DD	1073741824
	DD	1072710065
	DD	0
	DD	1072709333
	DD	3221225472
	DD	1072708601
	DD	3221225472
	DD	1072707871
	DD	2147483648
	DD	1072707142
	DD	1073741824
	DD	1072706414
	DD	0
	DD	1072705687
	DD	3221225472
	DD	1072704960
	DD	2147483648
	DD	1072704235
	DD	1073741824
	DD	1072703511
	DD	0
	DD	1072702788
	DD	3221225472
	DD	1072702065
	DD	2147483648
	DD	1072701344
	DD	1073741824
	DD	1072700624
	DD	3221225472
	DD	1072699904
	DD	2147483648
	DD	1072699186
	DD	1073741824
	DD	1072698469
	DD	3221225472
	DD	1072697752
	DD	2147483648
	DD	1072697037
	DD	0
	DD	1072696323
	DD	2147483648
	DD	1072695609
	DD	0
	DD	1072694897
	DD	2147483648
	DD	1072694185
	DD	0
	DD	1072693475
	DD	2147483648
	DD	1072692282
	DD	1073741824
	DD	1072690865
	DD	3221225472
	DD	1072689449
	DD	2147483648
	DD	1072688036
	DD	3221225472
	DD	1072686624
	DD	1073741824
	DD	1072685215
	DD	2147483648
	DD	1072683807
	DD	2147483648
	DD	1072682401
	DD	2147483648
	DD	1072680997
	DD	2147483648
	DD	1072679595
	DD	1073741824
	DD	1072678195
	DD	0
	DD	1072676797
	DD	2147483648
	DD	1072675400
	DD	0
	DD	1072674006
	DD	1073741824
	DD	1072672613
	DD	2147483648
	DD	1072671222
	DD	2147483648
	DD	1072669833
	DD	1073741824
	DD	1072668446
	DD	0
	DD	1072667061
	DD	3221225472
	DD	1072665677
	DD	0
	DD	1072664296
	DD	1073741824
	DD	1072662916
	DD	2147483648
	DD	1072661538
	DD	2147483648
	DD	1072660162
	DD	1073741824
	DD	1072658788
	DD	3221225472
	DD	1072657415
	DD	1073741824
	DD	1072656045
	DD	2147483648
	DD	1072654676
	DD	2147483648
	DD	1072653309
	DD	2147483648
	DD	1072651944
	DD	1073741824
	DD	1072650581
	DD	3221225472
	DD	1072649219
	DD	0
	DD	1072647860
	DD	1073741824
	DD	1072646502
	DD	0
	DD	1072645146
	DD	3221225472
	DD	1072643791
	DD	1073741824
	DD	1072642439
	DD	2147483648
	DD	1072641088
	DD	2147483648
	DD	1072639739
	DD	2147483648
	DD	1072638392
	DD	0
	DD	1072637047
	DD	2147483648
	DD	1072635703
	DD	2147483648
	DD	1072634361
	DD	2147483648
	DD	1072633021
	DD	1073741824
	DD	1072631683
	DD	3221225472
	DD	1072630346
	DD	3221225472
	DD	1072629011
	DD	3221225472
	DD	1072627678
	DD	2147483648
	DD	1072626347
	DD	0
	DD	1072625018
	DD	0
	DD	1072623690
	DD	0
	DD	1072622364
	DD	2147483648
	DD	1072621039
	DD	0
	DD	1072619717
	DD	0
	DD	1072618396
	DD	0
	DD	1072617077
	DD	2147483648
	DD	1072615759
	DD	3221225472
	DD	1072614443
	DD	3221225472
	DD	1072613129
	DD	1073741824
	DD	1072611817
	DD	3221225472
	DD	1072610506
	DD	3221225472
	DD	1072609197
	DD	2147483648
	DD	1072607890
	DD	0
	DD	1072606585
	DD	1073741824
	DD	1072605281
	DD	1073741824
	DD	1072603979
	DD	3221225472
	DD	1072602678
	DD	0
	DD	1072601380
	DD	0
	DD	1072600083
	DD	2147483648
	DD	1072598787
	DD	3221225472
	DD	1072597493
	DD	3221225472
	DD	1072596201
	DD	1073741824
	DD	1072594911
	DD	2147483648
	DD	1072593622
	DD	2147483648
	DD	1072592335
	DD	0
	DD	1072591050
	DD	1073741824
	DD	1072589766
	DD	1073741824
	DD	1072588484
	DD	3221225472
	DD	1072587203
	DD	0
	DD	1072585925
	DD	3221225472
	DD	1072584647
	DD	1073741824
	DD	1072583372
	DD	2147483648
	DD	1072582098
	DD	0
	DD	1072580826
	DD	2147483648
	DD	1072579555
	DD	2147483648
	DD	1072578286
	DD	0
	DD	1072577019
	DD	1073741824
	DD	1072575753
	DD	1073741824
	DD	1072574489
	DD	3221225472
	DD	1072573226
	DD	3221225472
	DD	1072571965
	DD	2147483648
	DD	1072570706
	DD	3221225472
	DD	1072569448
	DD	2147483648
	DD	1072568192
	DD	0
	DD	1072566938
	DD	1073741824
	DD	1072565685
	DD	3221225472
	DD	1072564433
	DD	0
	DD	1072563184
	DD	0
	DD	1072561936
	DD	1073741824
	DD	1072560689
	DD	1073741824
	DD	1072559444
	DD	0
	DD	1072558201
	DD	0
	DD	1072556959
	DD	3221225472
	DD	1072555718
	DD	1073741824
	DD	1072554480
	DD	0
	DD	1072553243
	DD	2147483648
	DD	1072552007
	DD	2147483648
	DD	1072550773
	DD	0
	DD	1072549541
	DD	0
	DD	1072548310
	DD	3221225472
	DD	1072547080
	DD	0
	DD	1072545853
	DD	3221225472
	DD	1072544626
	DD	0
	DD	1072543402
	DD	3221225472
	DD	1072542178
	DD	0
	DD	1072540957
	DD	0
	DD	1072539737
	DD	2147483648
	DD	1072538518
	DD	1073741824
	DD	1072537301
	DD	3221225472
	DD	1072536085
	DD	3221225472
	DD	1072534871
	DD	1073741824
	DD	1072533659
	DD	1073741824
	DD	1072532448
	DD	3221225472
	DD	1072531238
	DD	0
	DD	1072530031
	DD	2147483648
	DD	1072528824
	DD	2147483648
	DD	1072527619
	DD	0
	DD	1072526416
	DD	1073741824
	DD	1072525214
	DD	3221225472
	DD	1072524013
	DD	3221225472
	DD	1072522814
	DD	1073741824
	DD	1072521617
	DD	1073741824
	DD	1072520421
	DD	3221225472
	DD	1072519226
	DD	3221225472
	DD	1072518033
	DD	1073741824
	DD	1072516842
	DD	1073741824
	DD	1072515652
	DD	2147483648
	DD	1072514463
	DD	2147483648
	DD	1072513276
	DD	3221225472
	DD	1072512090
	DD	3221225472
	DD	1072510906
	DD	0
	DD	1072509724
	DD	3221225472
	DD	1072508542
	DD	0
	DD	1072507363
	DD	2147483648
	DD	1072506184
	DD	3221225472
	DD	1072505007
	DD	1073741824
	DD	1072503832
	DD	1073741824
	DD	1072502658
	DD	3221225472
	DD	1072501485
	DD	3221225472
	DD	1072500314
	DD	0
	DD	1072499145
	DD	3221225472
	DD	1072497976
	DD	0
	DD	1072496810
	DD	2147483648
	DD	1072495644
	DD	3221225472
	DD	1072494480
	DD	0
	DD	1072493318
	DD	0
	DD	1072492157
	DD	1073741824
	DD	1072490997
	DD	0
	DD	1072489839
	DD	1073741824
	DD	1072488682
	DD	3221225472
	DD	1072487526
	DD	3221225472
	DD	1072486372
	DD	1073741824
	DD	1072485220
	DD	0
	DD	1072484069
	DD	1073741824
	DD	1072482919
	DD	3221225472
	DD	1072481770
	DD	3221225472
	DD	1072480623
	DD	1073741824
	DD	1072479478
	DD	0
	DD	1072478334
	DD	1073741824
	DD	1072477191
	DD	3221225472
	DD	1072476049
	DD	3221225472
	DD	1072474909
	DD	0
	DD	1072473771
	DD	3221225472
	DD	1072472633
	DD	3221225472
	DD	1072471497
	DD	1073741824
	DD	1072470363
	DD	0
	DD	1072469230
	DD	1073741824
	DD	1072468098
	DD	3221225472
	DD	1072466967
	DD	3221225472
	DD	1072465838
	DD	0
	DD	1072464711
	DD	3221225472
	DD	1072463584
	DD	3221225472
	DD	1072462459
	DD	0
	DD	1072461336
	DD	3221225472
	DD	1072460213
	DD	3221225472
	DD	1072459092
	DD	1073741824
	DD	1072457973
	DD	0
	DD	1072456855
	DD	0
	DD	1072455738
	DD	2147483648
	DD	1072454622
	DD	1073741824
	DD	1072453508
	DD	2147483648
	DD	1072452395
	DD	3221225472
	DD	1072451283
	DD	3221225472
	DD	1072450173
	DD	3221225472
	DD	1072449064
	DD	1073741824
	DD	1072447957
	DD	0
	DD	1072446851
	DD	0
	DD	1072445746
	DD	1073741824
	DD	1072444642
	DD	0
	DD	1072443540
	DD	0
	DD	1072442439
	DD	2147483648
	DD	1072441339
	DD	0
	DD	1072440241
	DD	0
	DD	1072439144
	DD	1073741824
	DD	1072438048
	DD	3221225472
	DD	1072436953
	DD	3221225472
	DD	1072435860
	DD	3221225472
	DD	1072434768
	DD	1073741824
	DD	1072433678
	DD	0
	DD	1072432589
	DD	0
	DD	1072431501
	DD	1073741824
	DD	1072430414
	DD	0
	DD	1072429329
	DD	3221225472
	DD	1072428244
	DD	0
	DD	1072427162
	DD	2147483648
	DD	1072426080
	DD	1073741824
	DD	1072425000
	DD	1073741824
	DD	1072423921
	DD	2147483648
	DD	1072422843
	DD	0
	DD	1072421767
	DD	3221225472
	DD	1072420691
	DD	0
	DD	1072419618
	DD	1073741824
	DD	1072418545
	DD	3221225472
	DD	1072417473
	DD	3221225472
	DD	1072416403
	DD	3221225472
	DD	1072415334
	DD	1073741824
	DD	1072414267
	DD	3221225472
	DD	1072413200
	DD	3221225472
	DD	1072412135
	DD	3221225472
	DD	1072411071
	DD	1073741824
	DD	1072410009
	DD	3221225472
	DD	1072408947
	DD	3221225472
	DD	1072407887
	DD	3221225472
	DD	1072406828
	DD	0
	DD	1072405771
	DD	3221225472
	DD	1072404714
	DD	2147483648
	DD	1072403659
	DD	2147483648
	DD	1072402605
	DD	3221225472
	DD	1072401552
	DD	1073741824
	DD	1072400501
	DD	0
	DD	1072399451
	DD	3221225472
	DD	1072398401
	DD	0
	DD	1072397354
	DD	1073741824
	DD	1072396307
	DD	0
	DD	1072395262
	DD	3221225472
	DD	1072394217
	DD	3221225472
	DD	1072393174
	DD	0
	DD	1072392133
	DD	1073741824
	DD	1072391092
	DD	0
	DD	1072390053
	DD	3221225472
	DD	1072389014
	DD	3221225472
	DD	1072387977
	DD	0
	DD	1072386942
	DD	2147483648
	DD	1072385907
	DD	0
	DD	1072384874
	DD	0
	DD	1072383842
	DD	0
	DD	1072382811
	DD	1073741824
	DD	1072381781
	DD	2147483648
	DD	1072380752
	DD	0
	DD	1072379725
	DD	3221225472
	DD	1072378698
	DD	3221225472
	DD	1072377673
	DD	3221225472
	DD	1072376649
	DD	1073741824
	DD	1072375627
	DD	2147483648
	DD	1072374605
	DD	1073741824
	DD	1072373585
	DD	0
	DD	1072372566
	DD	0
	DD	1072371548
	DD	0
	DD	1072370531
	DD	2147483648
	DD	1072369515
	DD	3221225472
	DD	1072368500
	DD	2147483648
	DD	1072367487
	DD	1073741824
	DD	1072366475
	DD	1073741824
	DD	1072365464
	DD	1073741824
	DD	1072364454
	DD	2147483648
	DD	1072363445
	DD	0
	DD	1072362438
	DD	2147483648
	DD	1072361431
	DD	1073741824
	DD	1072360426
	DD	0
	DD	1072359422
	DD	0
	DD	1072358419
	DD	0
	DD	1072357417
	DD	1073741824
	DD	1072356416
	DD	3221225472
	DD	1072355416
	DD	1073741824
	DD	1072354418
	DD	3221225472
	DD	1072353420
	DD	3221225472
	DD	1072352424
	DD	2147483648
	DD	1072351429
	DD	2147483648
	DD	1072350435
	DD	3221225472
	DD	1072349442
	DD	0
	DD	1072348451
	DD	2147483648
	DD	1072347460
	DD	0
	DD	1072346471
	DD	2147483648
	DD	1072345482
	DD	2147483648
	DD	1072344495
	DD	1073741824
	DD	1072343509
	DD	1073741824
	DD	1072342524
	DD	1073741824
	DD	1072341540
	DD	2147483648
	DD	1072340557
	DD	3221225472
	DD	1072339575
	DD	1073741824
	DD	1072338595
	DD	3221225472
	DD	1072337615
	DD	2147483648
	DD	1072336637
	DD	1073741824
	DD	1072335660
	DD	0
	DD	1072334684
	DD	3221225472
	DD	1072333708
	DD	3221225472
	DD	1072332734
	DD	0
	DD	1072331762
	DD	1073741824
	DD	1072330790
	DD	2147483648
	DD	1072329819
	DD	3221225472
	DD	1072328849
	DD	1073741824
	DD	1072327881
	DD	3221225472
	DD	1072326913
	DD	1073741824
	DD	1072325947
	DD	0
	DD	1072324982
	DD	3221225472
	DD	1072324017
	DD	2147483648
	DD	1072323054
	DD	2147483648
	DD	1072322092
	DD	2147483648
	DD	1072321131
	DD	2147483648
	DD	1072320171
	DD	3221225472
	DD	1072319212
	DD	3221225472
	DD	1072318254
	DD	0
	DD	1072317298
	DD	1073741824
	DD	1072316342
	DD	3221225472
	DD	1072315387
	DD	1073741824
	DD	1072314434
	DD	3221225472
	DD	1072313481
	DD	1073741824
	DD	1072312530
	DD	3221225472
	DD	1072311579
	DD	2147483648
	DD	1072310630
	DD	0
	DD	1072309682
	DD	3221225472
	DD	1072308734
	DD	3221225472
	DD	1072307788
	DD	2147483648
	DD	1072306843
	DD	1073741824
	DD	1072305899
	DD	1073741824
	DD	1072304956
	DD	1073741824
	DD	1072304014
	DD	1073741824
	DD	1072303073
	DD	1073741824
	DD	1072302133
	DD	1073741824
	DD	1072301194
	DD	2147483648
	DD	1072300256
	DD	2147483648
	DD	1072299319
	DD	3221225472
	DD	1072298383
	DD	0
	DD	1072297449
	DD	1073741824
	DD	1072296515
	DD	2147483648
	DD	1072295582
	DD	3221225472
	DD	1072294650
	DD	0
	DD	1072293720
	DD	1073741824
	DD	1072292790
	DD	2147483648
	DD	1072291861
	DD	0
	DD	1072290934
	DD	1073741824
	DD	1072290007
	DD	3221225472
	DD	1072289081
	DD	0
	DD	1072288157
	DD	2147483648
	DD	1072287233
	DD	3221225472
	DD	1072286310
	DD	1073741824
	DD	1072285389
	DD	2147483648
	DD	1072284468
	DD	0
	DD	1072283549
	DD	2147483648
	DD	1072282630
	DD	3221225472
	DD	1072281712
	DD	1073741824
	DD	1072280796
	DD	3221225472
	DD	1072279880
	DD	0
	DD	1072278966
	DD	2147483648
	DD	1072278052
	DD	3221225472
	DD	1072277139
	DD	1073741824
	DD	1072276228
	DD	2147483648
	DD	1072275317
	DD	3221225472
	DD	1072274407
	DD	1073741824
	DD	1072273499
	DD	2147483648
	DD	1072272591
	DD	3221225472
	DD	1072271684
	DD	0
	DD	1072270779
	DD	1073741824
	DD	1072269874
	DD	2147483648
	DD	1072268970
	DD	3221225472
	DD	1072268067
	DD	0
	DD	1072267166
	DD	0
	DD	1072266265
	DD	1073741824
	DD	1072265365
	DD	1073741824
	DD	1072264466
	DD	2147483648
	DD	1072263568
	DD	2147483648
	DD	1072262671
	DD	2147483648
	DD	1072261775
	DD	1073741824
	DD	1072260880
	DD	1073741824
	DD	1072259986
	DD	1073741824
	DD	1072259093
	DD	0
	DD	1072258201
	DD	3221225472
	DD	1072257309
	DD	2147483648
	DD	1072256419
	DD	1073741824
	DD	1072255530
	DD	0
	DD	1072254642
	DD	2147483648
	DD	1072253754
	DD	0
	DD	1072252868
	DD	2147483648
	DD	1072251982
	DD	0
	DD	1072251098
	DD	1073741824
	DD	1072250214
	DD	3221225472
	DD	1072249331
	DD	0
	DD	1072248450
	DD	1073741824
	DD	1072247569
	DD	1073741824
	DD	1072246689
	DD	2147483648
	DD	1072245810
	DD	2147483648
	DD	1072244932
	DD	2147483648
	DD	1072244055
	DD	1073741824
	DD	1072243179
	DD	1073741824
	DD	1072242304
	DD	0
	DD	1072241430
	DD	2147483648
	DD	1072240556
	DD	1073741824
	DD	1072239684
	DD	3221225472
	DD	1072238812
	DD	1073741824
	DD	1072237942
	DD	2147483648
	DD	1072237072
	DD	3221225472
	DD	1072236203
	DD	0
	DD	1072235336
	DD	1073741824
	DD	1072234469
	DD	1073741824
	DD	1072233603
	DD	1073741824
	DD	1072232738
	DD	0
	DD	1072231874
	DD	0
	DD	1072231011
	DD	2147483648
	DD	1072230148
	DD	1073741824
	DD	1072229287
	DD	3221225472
	DD	1072228426
	DD	1073741824
	DD	1072227567
	DD	2147483648
	DD	1072226708
	DD	3221225472
	DD	1072225850
	DD	3221225472
	DD	1072224993
	DD	0
	DD	1072224138
	DD	3221225472
	DD	1072223282
	DD	3221225472
	DD	1072222428
	DD	2147483648
	DD	1072221575
	DD	0
	DD	1072220723
	DD	2147483648
	DD	1072219871
	DD	0
	DD	1072219021
	DD	1073741824
	DD	1072218171
	DD	2147483648
	DD	1072217322
	DD	2147483648
	DD	1072216474
	DD	2147483648
	DD	1072215627
	DD	1073741824
	DD	1072214781
	DD	0
	DD	1072213936
	DD	3221225472
	DD	1072213091
	DD	1073741824
	DD	1072212248
	DD	2147483648
	DD	1072211405
	DD	3221225472
	DD	1072210563
	DD	0
	DD	1072209723
	DD	0
	DD	1072208883
	DD	3221225472
	DD	1072208043
	DD	2147483648
	DD	1072207205
	DD	1073741824
	DD	1072206368
	DD	3221225472
	DD	1072205531
	DD	0
	DD	1072204696
	DD	1073741824
	DD	1072203861
	DD	2147483648
	DD	1072203027
	DD	1073741824
	DD	1072202194
	DD	1073741824
	DD	1072201362
	DD	3221225472
	DD	1072200530
	DD	2147483648
	DD	1072199700
	DD	3221225472
	DD	1072198870
	DD	0
	DD	1072198042
	DD	1073741824
	DD	1072197214
	DD	1073741824
	DD	1072196387
	DD	0
	DD	1072195561
	DD	3221225472
	DD	1072194735
	DD	1073741824
	DD	1072193911
	DD	2147483648
	DD	1072193087
	DD	3221225472
	DD	1072192264
	DD	0
	DD	1072191443
	DD	3221225472
	DD	1072190621
	DD	2147483648
	DD	1072189801
	DD	1073741824
	DD	1072188982
	DD	3221225472
	DD	1072188163
	DD	0
	DD	1072187346
	DD	0
	DD	1072186529
	DD	0
	DD	1072185713
	DD	0
	DD	1072184898
	DD	2147483648
	DD	1072184083
	DD	0
	DD	1072183270
	DD	1073741824
	DD	1072182457
	DD	2147483648
	DD	1072181645
	DD	2147483648
	DD	1072180834
	DD	1073741824
	DD	1072180024
	DD	0
	DD	1072179215
	DD	1073741824
	DD	1072178406
	DD	3221225472
	DD	1072177598
	DD	3221225472
	DD	1072176791
	DD	3221225472
	DD	1072175985
	DD	2147483648
	DD	1072175180
	DD	0
	DD	1072174376
	DD	2147483648
	DD	1072173572
	DD	3221225472
	DD	1072172769
	DD	3221225472
	DD	1072171967
	DD	2147483648
	DD	1072171166
	DD	1073741824
	DD	1072170366
	DD	3221225472
	DD	1072169566
	DD	0
	DD	1072168768
	DD	0
	DD	1072167970
	DD	0
	DD	1072167173
	DD	3221225472
	DD	1072166376
	DD	1073741824
	DD	1072165581
	DD	2147483648
	DD	1072164786
	DD	2147483648
	DD	1072163992
	DD	2147483648
	DD	1072163199
	DD	1073741824
	DD	1072162407
	DD	3221225472
	DD	1072161615
	DD	1073741824
	DD	1072160825
	DD	1073741824
	DD	1072160035
	DD	1073741824
	DD	1072159246
	DD	0
	DD	1072158458
	DD	2147483648
	DD	1072157670
	DD	3221225472
	DD	1072156883
	DD	3221225472
	DD	1072156097
	DD	3221225472
	DD	1072155312
	DD	2147483648
	DD	1072154528
	DD	0
	DD	1072153745
	DD	1073741824
	DD	1072152962
	DD	1073741824
	DD	1072152180
	DD	0
	DD	1072151399
	DD	3221225472
	DD	1072150618
	DD	0
	DD	1072149839
	DD	1073741824
	DD	1072149060
	DD	1073741824
	DD	1072148282
	DD	0
	DD	1072147505
	DD	2147483648
	DD	1072146728
	DD	3221225472
	DD	1072145952
	DD	3221225472
	DD	1072145177
	DD	3221225472
	DD	1072144403
	DD	1073741824
	DD	1072143630
	DD	3221225472
	DD	1072142857
	DD	0
	DD	1072142086
	DD	3221225472
	DD	1072141314
	DD	2147483648
	DD	1072140544
	DD	0
	DD	1072139775
	DD	1073741824
	DD	1072139006
	DD	1073741824
	DD	1072138238
	DD	0
	DD	1072137471
	DD	2147483648
	DD	1072136704
	DD	3221225472
	DD	1072135938
	DD	3221225472
	DD	1072135173
	DD	3221225472
	DD	1072134409
	DD	1073741824
	DD	1072133646
	DD	2147483648
	DD	1072132883
	DD	3221225472
	DD	1072132121
	DD	2147483648
	DD	1072131360
	DD	0
	DD	1072130600
	DD	2147483648
	DD	1072129840
	DD	2147483648
	DD	1072129081
	DD	1073741824
	DD	1072128323
	DD	0
	DD	1072127566
	DD	1073741824
	DD	1072126809
	DD	2147483648
	DD	1072126053
	DD	1073741824
	DD	1072125298
	DD	3221225472
	DD	1072124543
	DD	1073741824
	DD	1072123790
	DD	1073741824
	DD	1072123037
	DD	0
	DD	1072122285
	DD	2147483648
	DD	1072121533
	DD	3221225472
	DD	1072120782
	DD	0
	DD	1072120033
	DD	3221225472
	DD	1072119283
	DD	1073741824
	DD	1072118535
	DD	2147483648
	DD	1072117787
	DD	2147483648
	DD	1072117040
	DD	0
	DD	1072116294
	DD	2147483648
	DD	1072115548
	DD	3221225472
	DD	1072114803
	DD	2147483648
	DD	1072114059
	DD	1073741824
	DD	1072113316
	DD	2147483648
	DD	1072112573
	DD	3221225472
	DD	1072111831
	DD	2147483648
	DD	1072111090
	DD	0
	DD	1072110350
	DD	1073741824
	DD	1072109610
	DD	1073741824
	DD	1072108871
	DD	0
	DD	0
	DB 0
	ORG $+46
	DB	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1062671073
	DD	796678535
	DD	1041164450
	DD	0
	DD	1063718849
	DD	1140684073
	DD	1041752416
	DD	0
	DD	1064388983
	DD	2255163804
	DD	1042521851
	DD	0
	DD	1064765883
	DD	4003036608
	DD	1041981095
	DD	0
	DD	1065142442
	DD	1447370381
	DD	1043266257
	DD	0
	DD	1065435906
	DD	3214237416
	DD	1042303349
	DD	0
	DD	1065623811
	DD	1395867546
	DD	1043223139
	DD	0
	DD	1065811544
	DD	3650643237
	DD	1043184581
	DD	0
	DD	1065999072
	DD	668168440
	DD	1042356961
	DD	0
	DD	1066186426
	DD	2317418103
	DD	1043002446
	DD	0
	DD	1066373606
	DD	484047815
	DD	1042249661
	DD	0
	DD	1066481201
	DD	2023523235
	DD	1044444463
	DD	0
	DD	1066574616
	DD	3255192356
	DD	1041703688
	DD	0
	DD	1066667926
	DD	2662903533
	DD	1043280941
	DD	0
	DD	1066761147
	DD	340289207
	DD	1045085082
	DD	0
	DD	1066854296
	DD	3341484544
	DD	1044953178
	DD	0
	DD	1066947340
	DD	4224212358
	DD	1043144193
	DD	0
	DD	1067040310
	DD	13476061
	DD	1043845743
	DD	0
	DD	1067133174
	DD	2391019286
	DD	1043608353
	DD	0
	DD	1067225947
	DD	133925943
	DD	1045385377
	DD	0
	DD	1067318647
	DD	162130248
	DD	1043727590
	DD	0
	DD	1067411255
	DD	3844935960
	DD	1044946701
	DD	0
	DD	1067477070
	DD	3514481986
	DD	1045238395
	DD	0
	DD	1067523283
	DD	1322147927
	DD	1044023707
	DD	0
	DD	1067569449
	DD	2575666426
	DD	1046416655
	DD	0
	DD	1067615578
	DD	2312730532
	DD	1046001998
	DD	0
	DD	1067661661
	DD	1811376022
	DD	1044392575
	DD	0
	DD	1067707697
	DD	2749409974
	DD	1044263981
	DD	0
	DD	1067753694
	DD	1289046379
	DD	1046006505
	DD	0
	DD	1067799637
	DD	3634184242
	DD	1044372580
	DD	0
	DD	1067845549
	DD	253051172
	DD	1043849171
	DD	0
	DD	1067891413
	DD	1415622883
	DD	1046393312
	DD	0
	DD	1067937231
	DD	348590237
	DD	1044975277
	DD	0
	DD	1067983009
	DD	4043773411
	DD	1045584106
	DD	0
	DD	1068028740
	DD	3797923024
	DD	1042557480
	DD	0
	DD	1068074431
	DD	3242650639
	DD	1039813251
	DD	0
	DD	1068120082
	DD	1609062134
	DD	1044151780
	DD	0
	DD	1068165685
	DD	4036053544
	DD	1044415064
	DD	0
	DD	1068211248
	DD	3324567520
	DD	1044354441
	DD	0
	DD	1068256762
	DD	1809751839
	DD	1045897557
	DD	0
	DD	1068302236
	DD	4161743430
	DD	1046120551
	DD	0
	DD	1068347670
	DD	2543475086
	DD	1045059644
	DD	0
	DD	1068393063
	DD	2148275205
	DD	1044658247
	DD	0
	DD	1068438407
	DD	1755383432
	DD	1041654945
	DD	0
	DD	1068483718
	DD	1931750958
	DD	1043248273
	DD	0
	DD	1068513961
	DD	3743230105
	DD	1047206800
	DD	0
	DD	1068536571
	DD	3706253206
	DD	1047480360
	DD	0
	DD	1068559161
	DD	1081208188
	DD	1045948584
	DD	0
	DD	1068581729
	DD	1190940899
	DD	1047437934
	DD	0
	DD	1068604277
	DD	1379366228
	DD	1046795244
	DD	0
	DD	1068626800
	DD	2164889869
	DD	1044817266
	DD	0
	DD	1068649305
	DD	283192480
	DD	1047216747
	DD	0
	DD	1068671790
	DD	2082162375
	DD	1046068154
	DD	0
	DD	1068694253
	DD	947408815
	DD	1047001494
	DD	0
	DD	1068716700
	DD	2525331809
	DD	1042174705
	DD	0
	DD	1068739120
	DD	1844386278
	DD	1046979838
	DD	0
	DD	1068761520
	DD	652649436
	DD	1040384675
	DD	0
	DD	1068783901
	DD	776167241
	DD	1047503387
	DD	0
	DD	1068806262
	DD	1365404007
	DD	1045971294
	DD	0
	DD	1068828601
	DD	1817788868
	DD	1043989309
	DD	0
	DD	1068850918
	DD	2085890411
	DD	1043580998
	DD	0
	DD	1068873217
	DD	2704270941
	DD	1046348533
	DD	0
	DD	1068895494
	DD	1839126285
	DD	1047449544
	DD	0
	DD	1068917750
	DD	2544072376
	DD	1046393412
	DD	0
	DD	1068939988
	DD	1927459855
	DD	1044957055
	DD	0
	DD	1068962203
	DD	1582487710
	DD	1047063933
	DD	0
	DD	1068984397
	DD	2849208731
	DD	1044642430
	DD	0
	DD	1069006572
	DD	351760603
	DD	1046501140
	DD	0
	DD	1069028729
	DD	2133946083
	DD	1047054288
	DD	0
	DD	1069050864
	DD	549944300
	DD	1046700276
	DD	0
	DD	1069072976
	DD	1205145505
	DD	1047368137
	DD	0
	DD	1069095075
	DD	2839269716
	DD	1045111579
	DD	0
	DD	1069117146
	DD	2157575645
	DD	1046598540
	DD	0
	DD	1069139203
	DD	2600563884
	DD	1046970091
	DD	0
	DD	1069161233
	DD	3491103052
	DD	1047123677
	DD	0
	DD	1069183249
	DD	2217441161
	DD	1046636176
	DD	0
	DD	1069205242
	DD	815121713
	DD	1045967033
	DD	0
	DD	1069227216
	DD	1368263980
	DD	1046227865
	DD	0
	DD	1069249171
	DD	2441229856
	DD	1046882203
	DD	0
	DD	1069271108
	DD	3372580556
	DD	1043377583
	DD	0
	DD	1069293021
	DD	1382639658
	DD	1044782308
	DD	0
	DD	1069314915
	DD	1596973686
	DD	1045447919
	DD	0
	DD	1069336790
	DD	3575860455
	DD	1045345243
	DD	0
	DD	1069358646
	DD	1852981273
	DD	1043248906
	DD	0
	DD	1069380478
	DD	2541460049
	DD	1045693349
	DD	0
	DD	1069402295
	DD	2995285043
	DD	1046664003
	DD	0
	DD	1069424093
	DD	4225069737
	DD	1046430780
	DD	0
	DD	1069445867
	DD	2693529773
	DD	1046687732
	DD	0
	DD	1069467626
	DD	1331463947
	DD	1046551955
	DD	0
	DD	1069489361
	DD	418170877
	DD	1046547196
	DD	0
	DD	1069511081
	DD	3560455957
	DD	1045192121
	DD	0
	DD	1069532776
	DD	3856463114
	DD	1047159135
	DD	0
	DD	1069550988
	DD	3092400973
	DD	1047496324
	DD	0
	DD	1069561818
	DD	741957782
	DD	1047818725
	DD	0
	DD	1069572638
	DD	2496886154
	DD	1048189613
	DD	0
	DD	1069583448
	DD	1463603613
	DD	1048550223
	DD	0
	DD	1069594249
	DD	631868157
	DD	1045334249
	DD	0
	DD	1069605041
	DD	3505304627
	DD	1046966802
	DD	0
	DD	1069615821
	DD	3831046799
	DD	1046740808
	DD	0
	DD	1069626593
	DD	1769659443
	DD	1046716993
	DD	0
	DD	1069637355
	DD	3894843808
	DD	1045383802
	DD	0
	DD	1069648108
	DD	3791552918
	DD	1048413090
	DD	0
	DD	1069658852
	DD	1388688759
	DD	1047426652
	DD	0
	DD	1069669585
	DD	2013501886
	DD	1048046245
	DD	0
	DD	1069680308
	DD	4183470588
	DD	1047948382
	DD	0
	DD	1069691023
	DD	3587924100
	DD	1047542354
	DD	0
	DD	1069701730
	DD	2105734897
	DD	1045526637
	DD	0
	DD	1069712426
	DD	770531512
	DD	1046888938
	DD	0
	DD	1069723112
	DD	3936842487
	DD	1045093883
	DD	0
	DD	1069733787
	DD	3154517508
	DD	1046990331
	DD	0
	DD	1069744456
	DD	1973897243
	DD	1047624407
	DD	0
	DD	1069755112
	DD	2792602356
	DD	1048465173
	DD	0
	DD	1069765763
	DD	2168432948
	DD	1046015137
	DD	0
	DD	1069776402
	DD	942577403
	DD	1048196642
	DD	0
	DD	1069787031
	DD	707586570
	DD	1048123067
	DD	0
	DD	1069797652
	DD	2326401483
	DD	1046603426
	DD	0
	DD	1069808264
	DD	3395877152
	DD	1046586318
	DD	0
	DD	1069818865
	DD	3368745197
	DD	1047549989
	DD	0
	DD	1069829458
	DD	1911861303
	DD	1045900655
	DD	0
	DD	1069840042
	DD	4090149705
	DD	1046503677
	DD	0
	DD	1069850615
	DD	3297666986
	DD	1047394389
	DD	0
	DD	1069861182
	DD	1741176201
	DD	1046512967
	DD	0
	DD	1069871735
	DD	2918857969
	DD	1048324050
	DD	0
	DD	1069882283
	DD	2007611647
	DD	1042409839
	DD	0
	DD	1069892821
	DD	1725563175
	DD	1047115920
	DD	0
	DD	1069903348
	DD	104793657
	DD	1048064427
	DD	0
	DD	1069913867
	DD	2549366830
	DD	1046651903
	DD	0
	DD	1069924379
	DD	1789268516
	DD	1046281708
	DD	0
	DD	1069934880
	DD	2738975643
	DD	1043382169
	DD	0
	DD	1069945369
	DD	1543936759
	DD	1047890178
	DD	0
	DD	1069955852
	DD	230850934
	DD	1047783367
	DD	0
	DD	1069966326
	DD	1381363090
	DD	1047730466
	DD	0
	DD	1069976791
	DD	2660427302
	DD	1047666073
	DD	0
	DD	1069987247
	DD	1007754378
	DD	1047521978
	DD	0
	DD	1069997694
	DD	4221728480
	DD	1046954127
	DD	0
	DD	1070008129
	DD	852168656
	DD	1048288736
	DD	0
	DD	1070018558
	DD	2381379470
	DD	1047509681
	DD	0
	DD	1070028977
	DD	1414317450
	DD	1048505743
	DD	0
	DD	1070039388
	DD	798566997
	DD	1046426680
	DD	0
	DD	1070049791
	DD	4065745044
	DD	1047673355
	DD	0
	DD	1070060183
	DD	643874660
	DD	1046971607
	DD	0
	DD	1070070568
	DD	4070576090
	DD	1045679865
	DD	0
	DD	1070080941
	DD	2121491982
	DD	1046886018
	DD	0
	DD	1070091307
	DD	3465567839
	DD	1047399360
	DD	0
	DD	1070101664
	DD	748821327
	DD	1046167763
	DD	0
	DD	1070112013
	DD	1343374980
	DD	1048221318
	DD	0
	DD	1070122351
	DD	4106571202
	DD	1047995593
	DD	0
	DD	1070132682
	DD	3018043708
	DD	1047235914
	DD	0
	DD	1070143003
	DD	3487235678
	DD	1047811947
	DD	0
	DD	1070153317
	DD	2352442431
	DD	1047736864
	DD	0
	DD	1070163622
	DD	2210934251
	DD	1039234528
	DD	0
	DD	1070173916
	DD	296385840
	DD	1048178638
	DD	0
	DD	1070184204
	DD	1085495685
	DD	1046654584
	DD	0
	DD	1070194482
	DD	136140764
	DD	1045335059
	DD	0
	DD	1070204750
	DD	3210807675
	DD	1046523944
	DD	0
	DD	1070215010
	DD	103829135
	DD	1048552471
	DD	0
	DD	1070225261
	DD	2486662291
	DD	1048514143
	DD	0
	DD	1070235505
	DD	3203312410
	DD	1047453418
	DD	0
	DD	1070245741
	DD	4039797773
	DD	1047519010
	DD	0
	DD	1070255965
	DD	4001968028
	DD	1046965562
	DD	0
	DD	1070266183
	DD	4078049975
	DD	1048558647
	DD	0
	DD	1070276392
	DD	2388514226
	DD	1047992239
	DD	0
	DD	1070286591
	DD	2018340210
	DD	1047576582
	DD	0
	DD	1070296782
	DD	2884086000
	DD	1047915506
	DD	0
	DD	1070306966
	DD	3786043284
	DD	1045890930
	DD	0
	DD	1070317139
	DD	1566631364
	DD	1047857120
	DD	0
	DD	1070327305
	DD	477504213
	DD	1047152540
	DD	0
	DD	1070337463
	DD	3403037290
	DD	1047164499
	DD	0
	DD	1070347611
	DD	1791004279
	DD	1046726231
	DD	0
	DD	1070357751
	DD	4288050343
	DD	1047093633
	DD	0
	DD	1070367883
	DD	2295007444
	DD	1047840907
	DD	0
	DD	1070378005
	DD	2750776851
	DD	1047957217
	DD	0
	DD	1070388119
	DD	240357404
	DD	1048299472
	DD	0
	DD	1070398226
	DD	340799861
	DD	1045215427
	DD	0
	DD	1070408324
	DD	1770263195
	DD	1047150692
	DD	0
	DD	1070418414
	DD	1220750140
	DD	1048020555
	DD	0
	DD	1070428494
	DD	253948398
	DD	1047977031
	DD	0
	DD	1070438569
	DD	3094480300
	DD	1041576003
	DD	0
	DD	1070448632
	DD	1868279505
	DD	1048371721
	DD	0
	DD	1070458688
	DD	990785184
	DD	1047967273
	DD	0
	DD	1070468736
	DD	848865488
	DD	1047127488
	DD	0
	DD	1070478773
	DD	2916681836
	DD	1047746980
	DD	0
	DD	1070488804
	DD	3828484875
	DD	1048558502
	DD	0
	DD	1070498828
	DD	1734393623
	DD	1045931829
	DD	0
	DD	1070508843
	DD	1282528426
	DD	1045697245
	DD	0
	DD	1070518847
	DD	3554561892
	DD	1047563451
	DD	0
	DD	1070528845
	DD	1874456463
	DD	1048498668
	DD	0
	DD	1070538836
	DD	494811099
	DD	1045110929
	DD	0
	DD	1070548815
	DD	3880431493
	DD	1047630510
	DD	0
	DD	1070558789
	DD	2502237429
	DD	1038745459
	DD	0
	DD	1070568754
	DD	1165037507
	DD	1044484047
	DD	0
	DD	1070578710
	DD	21263643
	DD	1047994445
	DD	0
	DD	1070588658
	DD	3033898382
	DD	1048442466
	DD	0
	DD	1070597347
	DD	397450529
	DD	1047908959
	DD	0
	DD	1070602314
	DD	2219334821
	DD	1046742082
	DD	0
	DD	1070607275
	DD	3341994796
	DD	1048619424
	DD	0
	DD	1070612233
	DD	3511223861
	DD	1049209226
	DD	0
	DD	1070617187
	DD	1445626495
	DD	1049196374
	DD	0
	DD	1070622137
	DD	3469555560
	DD	1048527569
	DD	0
	DD	1070627082
	DD	26583152
	DD	1049343494
	DD	0
	DD	1070632025
	DD	975950645
	DD	1046239867
	DD	0
	DD	1070636961
	DD	4137995552
	DD	1049241411
	DD	0
	DD	1070641895
	DD	4186973616
	DD	1048010627
	DD	0
	DD	1070646825
	DD	1679926900
	DD	1049086597
	DD	0
	DD	1070651750
	DD	820974333
	DD	1048661288
	DD	0
	DD	1070656672
	DD	3398496800
	DD	1046623374
	DD	0
	DD	1070661590
	DD	1976666915
	DD	1048653775
	DD	0
	DD	1070666503
	DD	3214908883
	DD	1047770306
	DD	0
	DD	1070671414
	DD	7382280
	DD	1045012211
	DD	0
	DD	1070676319
	DD	3655916187
	DD	1046605232
	DD	0
	DD	1070681220
	DD	827507300
	DD	1049568172
	DD	0
	DD	1070686118
	DD	867635885
	DD	1047867074
	DD	0
	DD	1070691012
	DD	2259138636
	DD	1048188473
	DD	0
	DD	1070695901
	DD	3661956722
	DD	1049588559
	DD	0
	DD	1070700788
	DD	739636523
	DD	1047537814
	DD	0
	DD	1070705669
	DD	557802518
	DD	1049612862
	DD	0
	DD	1070710547
	DD	376189753
	DD	1047061731
	DD	0
	DD	1070715422
	DD	3889840108
	DD	1047587413
	DD	0
	DD	1070720291
	DD	1077801053
	DD	1048689123
	DD	0
	DD	1070725157
	DD	1738542922
	DD	1048475171
	DD	0
	DD	1070730020
	DD	3718088452
	DD	1043564724
	DD	0
	DD	1070734879
	DD	3350684644
	DD	1046873019
	DD	0
	DD	1070739732
	DD	4150780361
	DD	1048524985
	DD	0
	DD	1070744583
	DD	3827576939
	DD	1048797632
	DD	0
	DD	1070749430
	DD	123987445
	DD	1045684570
	DD	0
	DD	1070754273
	DD	257245867
	DD	1046230126
	DD	0
	DD	1070759112
	DD	3237406949
	DD	1048978362
	DD	0
	DD	1070763947
	DD	3931183460
	DD	1048761956
	DD	0
	DD	1070768779
	DD	1964394977
	DD	1042565159
	DD	0
	DD	1070773605
	DD	1932165925
	DD	1049036386
	DD	0
	DD	1070778428
	DD	4030295045
	DD	1049475200
	DD	0
	DD	1070783248
	DD	1227093456
	DD	1048840627
	DD	0
	DD	1070788064
	DD	512005862
	DD	1049205778
	DD	0
	DD	1070792877
	DD	584191776
	DD	1048322863
	DD	0
	DD	1070797685
	DD	1817758221
	DD	1047750717
	DD	0
	DD	1070802489
	DD	337142479
	DD	1048800613
	DD	0
	DD	1070807290
	DD	4022531585
	DD	1047903762
	DD	0
	DD	1070812086
	DD	1777579942
	DD	1047557400
	DD	0
	DD	1070816879
	DD	1136393361
	DD	1049229648
	DD	0
	DD	1070821668
	DD	2996794901
	DD	1048630425
	DD	0
	DD	1070826453
	DD	1306863874
	DD	1048829327
	DD	0
	DD	1070831236
	DD	4202579682
	DD	1047825011
	DD	0
	DD	1070836014
	DD	739123541
	DD	1046630929
	DD	0
	DD	1070840787
	DD	2199804631
	DD	1045602780
	DD	0
	DD	1070845557
	DD	333939332
	DD	1048730896
	DD	0
	DD	1070850324
	DD	4176308913
	DD	1048145703
	DD	0
	DD	1070855087
	DD	1309699946
	DD	1048642620
	DD	0
	DD	1070859845
	DD	2054458693
	DD	1049043808
	DD	0
	DD	1070864601
	DD	4089887121
	DD	1048337014
	DD	0
	DD	1070869353
	DD	1265557010
	DD	1048318933
	DD	0
	DD	1070874100
	DD	1127420846
	DD	1048380124
	DD	0
	DD	1070878844
	DD	4141971308
	DD	1049549737
	DD	0
	DD	1070883585
	DD	3206355626
	DD	1049028708
	DD	0
	DD	1070888321
	DD	1985657905
	DD	1048358544
	DD	0
	DD	1070893054
	DD	910615973
	DD	1048879074
	DD	0
	DD	1070897784
	DD	1950082077
	DD	1044879926
	DD	0
	DD	1070902509
	DD	848934976
	DD	1048871904
	DD	0
	DD	1070907231
	DD	1153872427
	DD	1048230817
	DD	0
	DD	1070911949
	DD	4258918971
	DD	1048005920
	DD	0
	DD	1070916664
	DD	2057600513
	DD	1049042207
	DD	0
	DD	1070921374
	DD	3124444597
	DD	1049569376
	DD	0
	DD	1070926082
	DD	386505726
	DD	1048817436
	DD	0
	DD	1070930786
	DD	3732185524
	DD	1048076281
	DD	0
	DD	1070935486
	DD	3484395535
	DD	1047565093
	DD	0
	DD	1070940182
	DD	3438755194
	DD	1047423303
	DD	0
	DD	1070944874
	DD	3587939031
	DD	1047757092
	DD	0
	DD	1070949563
	DD	476230562
	DD	1049014929
	DD	0
	DD	1070954249
	DD	1973348497
	DD	1046944188
	DD	0
	DD	1070958930
	DD	764637372
	DD	1049008690
	DD	0
	DD	1070963608
	DD	2839757026
	DD	1047631753
	DD	0
	DD	1070968283
	DD	2020219930
	DD	1046594521
	DD	0
	DD	1070972954
	DD	169948185
	DD	1043853145
	DD	0
	DD	1070977620
	DD	285414159
	DD	1049449707
	DD	0
	DD	1070982285
	DD	2629906384
	DD	1045624362
	DD	0
	DD	1070986945
	DD	1148732993
	DD	1047259570
	DD	0
	DD	1070991601
	DD	293909111
	DD	1047890514
	DD	0
	DD	1070996254
	DD	1932670065
	DD	1048970280
	DD	0
	DD	1071000903
	DD	2159311823
	DD	1049607293
	DD	0
	DD	1071005549
	DD	125740886
	DD	1047407746
	DD	0
	DD	1071010191
	DD	1811376116
	DD	1048956143
	DD	0
	DD	1071014830
	DD	502800015
	DD	1044835032
	DD	0
	DD	1071019465
	DD	4044150405
	DD	1048763651
	DD	0
	DD	1071024097
	DD	1841045260
	DD	1044287532
	DD	0
	DD	1071028725
	DD	3133785266
	DD	1048850842
	DD	0
	DD	1071033350
	DD	1724009237
	DD	1046152529
	DD	0
	DD	1071037970
	DD	2068805762
	DD	1048329184
	DD	0
	DD	1071042587
	DD	3716447837
	DD	1049427347
	DD	0
	DD	1071047201
	DD	861464791
	DD	1047187078
	DD	0
	DD	1071051811
	DD	1648541673
	DD	1048830988
	DD	0
	DD	1071056419
	DD	2823265117
	DD	1046547126
	DD	0
	DD	1071061022
	DD	3170737398
	DD	1048368777
	DD	0
	DD	1071065622
	DD	1648779980
	DD	1049303416
	DD	0
	DD	1071070219
	DD	2592129555
	DD	1044060813
	DD	0
	DD	1071074810
	DD	1654631877
	DD	1049576652
	DD	0
	DD	1071079401
	DD	1647796229
	DD	1047581045
	DD	0
	DD	1071083987
	DD	647692424
	DD	1047732159
	DD	0
	DD	1071088569
	DD	4029101878
	DD	1045605139
	DD	0
	DD	1071093147
	DD	1404118998
	DD	1049338962
	DD	0
	DD	1071097723
	DD	2017086973
	DD	1048915876
	DD	0
	DD	1071102295
	DD	658705748
	DD	1046816981
	DD	0
	DD	1071106863
	DD	2831500312
	DD	1048901320
	DD	0
	DD	1071111429
	DD	2771960115
	DD	1046429771
	DD	0
	DD	1071115991
	DD	138818297
	DD	1048694922
	DD	0
	DD	1071120549
	DD	2606206358
	DD	1048896195
	DD	0
	DD	1071125104
	DD	2402036482
	DD	1048985879
	DD	0
	DD	1071129655
	DD	686723486
	DD	1048058780
	DD	0
	DD	1071134202
	DD	3067443036
	DD	1049590551
	DD	0
	DD	1071138748
	DD	1630477102
	DD	1049221900
	DD	0
	DD	1071143288
	DD	3804998927
	DD	1049493418
	DD	0
	DD	1071147827
	DD	3494982215
	DD	1047590326
	DD	0
	DD	1071152361
	DD	3502517450
	DD	1046911410
	DD	0
	DD	1071156891
	DD	908559027
	DD	1049549846
	DD	0
	DD	1071161419
	DD	491387723
	DD	1048825207
	DD	0
	DD	1071165944
	DD	292847180
	DD	1046523995
	DD	0
	DD	1071170465
	DD	2171450049
	DD	1048531657
	DD	0
	DD	1071174982
	DD	3577861825
	DD	1048063344
	DD	0
	DD	1071179496
	DD	3423944158
	DD	1045932689
	DD	0
	DD	1071184006
	DD	2382475260
	DD	1048835674
	DD	0
	DD	1071188513
	DD	1197041375
	DD	1049545442
	DD	0
	DD	1071193018
	DD	2554909289
	DD	1046028054
	DD	0
	DD	1071197519
	DD	2486865187
	DD	1043744711
	DD	0
	DD	1071202016
	DD	1035200280
	DD	1049044671
	DD	0
	DD	1071206509
	DD	4210692309
	DD	1049369112
	DD	0
	DD	1071211001
	DD	3248026144
	DD	1046161216
	DD	0
	DD	1071215487
	DD	1398948536
	DD	1049132977
	DD	0
	DD	1071219972
	DD	431431486
	DD	1048495558
	DD	0
	DD	1071224452
	DD	3943587108
	DD	1048798931
	DD	0
	DD	1071228930
	DD	2404975021
	DD	1049142212
	DD	0
	DD	1071233404
	DD	762316107
	DD	1047831483
	DD	0
	DD	1071237874
	DD	3883613913
	DD	1048729905
	DD	0
	DD	1071242342
	DD	1924884768
	DD	1049280957
	DD	0
	DD	1071246806
	DD	2004582630
	DD	1048389321
	DD	0
	DD	1071251266
	DD	2953051716
	DD	1049087478
	DD	0
	DD	1071255725
	DD	1599231962
	DD	1043473772
	DD	0
	DD	1071260178
	DD	4279527120
	DD	1048818851
	DD	0
	DD	1071264630
	DD	1221405487
	DD	1046953904
	DD	0
	DD	1071269078
	DD	2816097299
	DD	1048069360
	DD	0
	DD	1071273521
	DD	3663411169
	DD	1049349030
	DD	0
	DD	1071277963
	DD	1849981741
	DD	1047733633
	DD	0
	DD	1071282401
	DD	3271252640
	DD	1047870959
	DD	0
	DD	1071286835
	DD	2250253358
	DD	1049534312
	DD	0
	DD	1071291267
	DD	1664859842
	DD	1046809699
	DD	0
	DD	1071295694
	DD	653971165
	DD	1049508089
	DD	0
	DD	1071300120
	DD	4055663594
	DD	1048834019
	DD	0
	DD	1071304541
	DD	2211260648
	DD	1048616605
	DD	0
	DD	1071308960
	DD	4276771626
	DD	1047721897
	DD	0
	DD	1071313375
	DD	2007222528
	DD	1048802851
	DD	0
	DD	1071317787
	DD	4024343562
	DD	1048344600
	DD	0
	DD	1071322197
	DD	3459883422
	DD	1046581240
	DD	0
	DD	1071326601
	DD	3019951557
	DD	1049613392
	DD	0
	DD	1071331004
	DD	2456463112
	DD	1048974151
	DD	0
	DD	1071335403
	DD	1336124288
	DD	1049356048
	DD	0
	DD	1071339799
	DD	1536224909
	DD	1048640963
	DD	0
	DD	1071344193
	DD	647928540
	DD	1040540693
	DD	0
	DD	1071348582
	DD	1496506993
	DD	1049484256
	DD	0
	DD	1071352969
	DD	3746556472
	DD	1047789089
	DD	0
	DD	1071357352
	DD	3624742118
	DD	1046524646
	DD	0
	DD	1071361732
	DD	3840005276
	DD	1049051785
	DD	0
	DD	1071366109
	DD	84584192
	DD	1049087515
	DD	0
	DD	1071370483
	DD	1497662264
	DD	1046868936
	DD	0
	DD	1071374852
	DD	2036828729
	DD	1049623679
	DD	0
	DD	1071379220
	DD	3384702102
	DD	1048726046
	DD	0
	DD	1071383585
	DD	3959484802
	DD	1049373462
	DD	0
	DD	1071387946
	DD	1321767383
	DD	1047171685
	DD	0
	DD	1071392304
	DD	2341521400
	DD	1047541640
	DD	0
	DD	1071396658
	DD	2420218824
	DD	1048896928
	DD	0
	DD	1071401010
	DD	3764844109
	DD	1049110306
	DD	0
	DD	1071405359
	DD	1896541964
	DD	1047041197
	DD	0
	DD	1071409705
	DD	111479065
	DD	1047800076
	DD	0
	DD	1071414047
	DD	1091608136
	DD	1049111018
	DD	0
	DD	1071418386
	DD	1792476776
	DD	1048580603
	DD	0
	DD	1071422722
	DD	4090167448
	DD	1049434440
	DD	0
	DD	1071427055
	DD	3239566861
	DD	1048796237
	DD	0
	DD	1071431385
	DD	209019828
	DD	1049507697
	DD	0
	DD	1071435712
	DD	3088651057
	DD	1048688389
	DD	0
	DD	1071440036
	DD	486420980
	DD	1049184075
	DD	0
	DD	1071444357
	DD	1860382011
	DD	1047644376
	DD	0
	DD	1071448675
	DD	2568472625
	DD	1048057089
	DD	0
	DD	1071452989
	DD	490775408
	DD	1049011596
	DD	0
	DD	1071457300
	DD	2176612802
	DD	1047577340
	DD	0
	DD	1071461609
	DD	95270247
	DD	1049147956
	DD	0
	DD	1071465914
	DD	3975310265
	DD	1046338473
	DD	0
	DD	1071470216
	DD	3897578511
	DD	1044193017
	DD	0
	DD	1071474515
	DD	885657038
	DD	1048577629
	DD	0
	DD	1071478812
	DD	3761049041
	DD	1048631091
	DD	0
	DD	1071483105
	DD	1527353244
	DD	1049038582
	DD	0
	DD	1071487395
	DD	48390192
	DD	1045612641
	DD	0
	DD	1071491681
	DD	4014768595
	DD	1049526473
	DD	0
	DD	1071495966
	DD	2628957221
	DD	1047958241
	DD	0
	DD	1071500246
	DD	1683100999
	DD	1049380398
	DD	0
	DD	1071504524
	DD	3557884766
	DD	1049451037
	DD	0
	DD	1071508800
	DD	4101672850
	DD	1048350861
	DD	0
	DD	1071513072
	DD	2134944413
	DD	1045716209
	DD	0
	DD	1071517341
	DD	717499477
	DD	1047034903
	DD	0
	DD	1071521606
	DD	2472744143
	DD	1048107672
	DD	0
	DD	1071525869
	DD	2890826887
	DD	1044910820
	DD	0
	DD	1071530129
	DD	1395796230
	DD	1046771028
	DD	0
	DD	1071534385
	DD	775877882
	DD	1047865518
	DD	0
	DD	1071538640
	DD	1827978896
	DD	1047855473
	DD	0
	DD	1071542889
	DD	2341181545
	DD	1049574634
	DD	0
	DD	1071547138
	DD	672439089
	DD	1048307352
	DD	0
	DD	1071551382
	DD	933700661
	DD	1049452073
	DD	0
	DD	1071555624
	DD	3798565185
	DD	1049214907
	DD	0
	DD	1071559864
	DD	2355967464
	DD	1045877083
	DD	0
	DD	1071564100
	DD	155949587
	DD	1049128845
	DD	0
	DD	1071568333
	DD	3701796848
	DD	1048257477
	DD	0
	DD	1071572563
	DD	4028727335
	DD	1048397371
	DD	0
	DD	1071576790
	DD	1615913045
	DD	1049322780
	DD	0
	DD	1071581014
	DD	2478368294
	DD	1047217831
	DD	0
	DD	1071585236
	DD	917577567
	DD	1047815426
	DD	0
	DD	1071589454
	DD	2565841069
	DD	1048011410
	DD	0
	DD	1071593669
	DD	3200038571
	DD	1049089944
	DD	0
	DD	1071597882
	DD	3539257847
	DD	1048366566
	DD	0
	DD	1071602091
	DD	710932936
	DD	1045530199
	DD	0
	DD	1071606298
	DD	1704052999
	DD	1048157440
	DD	0
	DD	1071610501
	DD	3879651705
	DD	1048844640
	DD	0
	DD	1071614702
	DD	1377073661
	DD	1046678184
	DD	0
	DD	1071618899
	DD	2621611412
	DD	1049529222
	DD	0
	DD	1071623095
	DD	633905467
	DD	1044599036
	DD	0
	DD	1071627287
	DD	1861604490
	DD	1048117402
	DD	0
	DD	1071631475
	DD	2592784367
	DD	1048774196
	DD	0
	DD	1071635662
	DD	2668018718
	DD	1048420182
	DD	0
	DD	1071639845
	DD	3251710233
	DD	1046812174
	DD	0
	DD	1071644025
	DD	626369684
	DD	1046119423
	DD	0
	DD	1071646437
	DD	910482982
	DD	1050344616
	DD	0
	DD	1071648525
	DD	2423903336
	DD	1045677213
	DD	0
	DD	1071650610
	DD	3882059822
	DD	1050339179
	DD	0
	DD	1071652695
	DD	2246204545
	DD	1048541490
	DD	0
	DD	1071654778
	DD	1670915822
	DD	1048576579
	DD	0
	DD	1071656859
	DD	854771881
	DD	1050357784
	DD	0
	DD	1071658940
	DD	1415031676
	DD	1045738827
	DD	0
	DD	1071661018
	DD	1151313830
	DD	1050292979
	DD	0
	DD	1071663095
	DD	2446597541
	DD	1049495054
	DD	0
	DD	1071665171
	DD	715253803
	DD	1049247544
	DD	0
	DD	1071667245
	DD	831266258
	DD	1050519732
	DD	0
	DD	1071669319
	DD	2692487078
	DD	1044353652
	DD	0
	DD	1071671390
	DD	3320905325
	DD	1049985102
	DD	0
	DD	1071673460
	DD	2020409618
	DD	1050447532
	DD	0
	DD	1071675529
	DD	4217782635
	DD	1049987395
	DD	0
	DD	1071677597
	DD	3470676448
	DD	1043594114
	DD	0
	DD	1071679662
	DD	357969972
	DD	1050460795
	DD	0
	DD	1071681727
	DD	2091070605
	DD	1048936806
	DD	0
	DD	1071683790
	DD	3495427066
	DD	1048859820
	DD	0
	DD	1071685852
	DD	1549005693
	DD	1049743132
	DD	0
	DD	1071687912
	DD	603226576
	DD	1049866932
	DD	0
	DD	1071689971
	DD	3820364916
	DD	1048216340
	DD	0
	DD	1071692029
	DD	2718487701
	DD	1045909227
	DD	0
	DD	1071694084
	DD	1297799786
	DD	1050024647
	DD	0
	DD	1071696139
	DD	3886708913
	DD	1049783773
	DD	0
	DD	1071698192
	DD	496006409
	DD	1050628537
	DD	0
	DD	1071700244
	DD	2050750519
	DD	1050453463
	DD	0
	DD	1071702295
	DD	4185851790
	DD	1048875870
	DD	0
	DD	1071704344
	DD	1028934102
	DD	1048590687
	DD	0
	DD	1071706392
	DD	3012402897
	DD	1049242748
	DD	0
	DD	1071708438
	DD	22203316
	DD	1050048442
	DD	0
	DD	1071710483
	DD	665764605
	DD	1050360441
	DD	0
	DD	1071712527
	DD	2313729961
	DD	1047559976
	DD	0
	DD	1071714569
	DD	3122133791
	DD	1049869555
	DD	0
	DD	1071716610
	DD	2193666532
	DD	1048374481
	DD	0
	DD	1071718649
	DD	4213671346
	DD	1049994178
	DD	0
	DD	1071720687
	DD	1784900113
	DD	1050598350
	DD	0
	DD	1071722724
	DD	2847421139
	DD	1050094717
	DD	0
	DD	1071724759
	DD	3949406723
	DD	1050572015
	DD	0
	DD	1071726794
	DD	4139294257
	DD	1042947668
	DD	0
	DD	1071728826
	DD	1880751396
	DD	1049668979
	DD	0
	DD	1071730857
	DD	679290916
	DD	1050370972
	DD	0
	DD	1071732887
	DD	3711818429
	DD	1049925046
	DD	0
	DD	1071734916
	DD	4264670672
	DD	1048521455
	DD	0
	DD	1071736943
	DD	1548930207
	DD	1048738616
	DD	0
	DD	1071738969
	DD	3549708326
	DD	1047651585
	DD	0
	DD	1071740993
	DD	739124738
	DD	1050239626
	DD	0
	DD	1071743016
	DD	4049183935
	DD	1050422866
	DD	0
	DD	1071745038
	DD	2968772573
	DD	1049196249
	DD	0
	DD	1071747059
	DD	3439618396
	DD	1046643987
	DD	0
	DD	1071749078
	DD	1616633336
	DD	1047900890
	DD	0
	DD	1071751095
	DD	2187587096
	DD	1050013284
	DD	0
	DD	1071753111
	DD	449385046
	DD	1050635872
	DD	0
	DD	1071755127
	DD	395022541
	DD	1046287706
	DD	0
	DD	1071757140
	DD	3731258177
	DD	1050475497
	DD	0
	DD	1071759153
	DD	260677565
	DD	1047808744
	DD	0
	DD	1071761164
	DD	4056447433
	DD	1047976303
	DD	0
	DD	1071763173
	DD	738668208
	DD	1050594142
	DD	0
	DD	1071765182
	DD	3189618712
	DD	1047642397
	DD	0
	DD	1071767189
	DD	2505691407
	DD	1046633659
	DD	0
	DD	1071769194
	DD	1635171806
	DD	1050165702
	DD	0
	DD	1071771199
	DD	1016578577
	DD	1048108308
	DD	0
	DD	1071773202
	DD	4043642296
	DD	1043373880
	DD	0
	DD	1071775203
	DD	729467733
	DD	1050534352
	DD	0
	DD	1071777204
	DD	1862705055
	DD	1048407654
	DD	0
	DD	1071779203
	DD	3045759592
	DD	1048626265
	DD	0
	DD	1071781201
	DD	3292670787
	DD	1045117638
	DD	0
	DD	1071783197
	DD	3186981907
	DD	1049667290
	DD	0
	DD	1071785192
	DD	1580099252
	DD	1049077488
	DD	0
	DD	1071787186
	DD	2863105791
	DD	1048832970
	DD	0
	DD	1071789178
	DD	2007906494
	DD	1049836625
	DD	0
	DD	1071791169
	DD	43748125
	DD	1050625253
	DD	0
	DD	1071793159
	DD	3902070576
	DD	1050031552
	DD	0
	DD	1071795148
	DD	4059257100
	DD	1047431161
	DD	0
	DD	1071797135
	DD	2258875433
	DD	1048570571
	DD	0
	DD	1071799121
	DD	1309713434
	DD	1049427407
	DD	0
	DD	1071801105
	DD	534636097
	DD	1050637218
	DD	0
	DD	1071803089
	DD	299199803
	DD	1048218496
	DD	0
	DD	1071805071
	DD	2103129296
	DD	1047495871
	DD	0
	DD	1071807052
	DD	479138224
	DD	1047102924
	DD	0
	DD	1071809031
	DD	1778567171
	DD	1049154138
	DD	0
	DD	1071811009
	DD	3275964022
	DD	1050107860
	DD	0
	DD	1071812986
	DD	1199690434
	DD	1049109781
	DD	0
	DD	1071814962
	DD	2514881829
	DD	1046640917
	DD	0
	DD	1071816936
	DD	1104503643
	DD	1046502220
	DD	0
	DD	1071818909
	DD	5407894
	DD	1047062825
	DD	0
	DD	1071820880
	DD	1576444545
	DD	1050243092
	DD	0
	DD	3218771961
	DD	1440681443
	DD	3196465185
	DD	0
	DD	3218768022
	DD	1635187408
	DD	3196922673
	DD	0
	DD	3218764087
	DD	4138527638
	DD	3196210610
	DD	0
	DD	3218760154
	DD	2152571444
	DD	3194425722
	DD	0
	DD	3218756223
	DD	267747451
	DD	3196033014
	DD	0
	DD	3218752295
	DD	1152231976
	DD	3196099932
	DD	0
	DD	3218748370
	DD	3734685755
	DD	3195069127
	DD	0
	DD	3218744447
	DD	424868672
	DD	3196551425
	DD	0
	DD	3218740527
	DD	3987709718
	DD	3194459328
	DD	0
	DD	3218736609
	DD	3918093064
	DD	3195526646
	DD	0
	DD	3218732694
	DD	1437482408
	DD	3195252468
	DD	0
	DD	3218728781
	DD	746301886
	DD	3194924868
	DD	0
	DD	3218724870
	DD	1139884144
	DD	3196963122
	DD	0
	DD	3218720963
	DD	2035740717
	DD	3195589050
	DD	0
	DD	3218717057
	DD	2015757072
	DD	3196816414
	DD	0
	DD	3218713155
	DD	3386053521
	DD	3193758335
	DD	0
	DD	3218709255
	DD	1442056314
	DD	3194178403
	DD	0
	DD	3218705357
	DD	1462759235
	DD	3194786996
	DD	0
	DD	3218701462
	DD	501826151
	DD	3193456242
	DD	0
	DD	3218697569
	DD	3453886314
	DD	3196630858
	DD	0
	DD	3218693679
	DD	66499200
	DD	3196066366
	DD	0
	DD	3218689792
	DD	1417806911
	DD	3192599817
	DD	0
	DD	3218685906
	DD	3652930140
	DD	3196412983
	DD	0
	DD	3218682024
	DD	2293262949
	DD	3193812403
	DD	0
	DD	3218678144
	DD	1757472212
	DD	3195652407
	DD	0
	DD	3218674266
	DD	3500865402
	DD	3190288617
	DD	0
	DD	3218670391
	DD	2086224625
	DD	3194882143
	DD	0
	DD	3218666518
	DD	4126438761
	DD	3196172685
	DD	0
	DD	3218662647
	DD	2890784492
	DD	3197056040
	DD	0
	DD	3218658780
	DD	1620206862
	DD	3195086831
	DD	0
	DD	3218654915
	DD	1818676664
	DD	3195807278
	DD	0
	DD	3218651052
	DD	4294225685
	DD	3196546991
	DD	0
	DD	3218647192
	DD	2912876962
	DD	3194183646
	DD	0
	DD	3218643334
	DD	165960939
	DD	3195927649
	DD	0
	DD	3218639478
	DD	1097746209
	DD	3196949673
	DD	0
	DD	3218635625
	DD	892481449
	DD	3196111293
	DD	0
	DD	3218631775
	DD	348717007
	DD	3192241479
	DD	0
	DD	3218627927
	DD	2881114496
	DD	3195996870
	DD	0
	DD	3218624081
	DD	4235642082
	DD	3195314644
	DD	0
	DD	3218620238
	DD	3560874073
	DD	3193489339
	DD	0
	DD	3218616397
	DD	75584058
	DD	3196721188
	DD	0
	DD	3218612558
	DD	3773330185
	DD	3196993813
	DD	0
	DD	3218608723
	DD	3385148500
	DD	3191891964
	DD	0
	DD	3218604890
	DD	1080079306
	DD	3193248004
	DD	0
	DD	3218601058
	DD	3132354469
	DD	3196187074
	DD	0
	DD	3218597230
	DD	380186399
	DD	3190771585
	DD	0
	DD	3218593404
	DD	2943702034
	DD	3195863275
	DD	0
	DD	3218589580
	DD	850041604
	DD	3195370675
	DD	0
	DD	3218585759
	DD	2032443937
	DD	3194709909
	DD	0
	DD	3218581940
	DD	4069079423
	DD	3195311169
	DD	0
	DD	3218578123
	DD	2000269891
	DD	3196403232
	DD	0
	DD	3218574309
	DD	3062399245
	DD	3194983439
	DD	0
	DD	3218570497
	DD	2904519755
	DD	3196796407
	DD	0
	DD	3218566688
	DD	2902993865
	DD	3196497971
	DD	0
	DD	3218562881
	DD	303596762
	DD	3196742396
	DD	0
	DD	3218559077
	DD	3028031620
	DD	3194648195
	DD	0
	DD	3218555274
	DD	878141398
	DD	3196811134
	DD	0
	DD	3218551474
	DD	3166301789
	DD	3196659707
	DD	0
	DD	3218547677
	DD	1888507499
	DD	3196634765
	DD	0
	DD	3218543883
	DD	705606033
	DD	3192531754
	DD	0
	DD	3218540089
	DD	2380675131
	DD	3196779510
	DD	0
	DD	3218536299
	DD	2466537128
	DD	3196509680
	DD	0
	DD	3218532511
	DD	1556116846
	DD	3196881987
	DD	0
	DD	3218528726
	DD	3908658719
	DD	3195563322
	DD	0
	DD	3218524943
	DD	1064148360
	DD	3194499812
	DD	0
	DD	3218521162
	DD	325158783
	DD	3195305144
	DD	0
	DD	3218517384
	DD	150061299
	DD	3196191576
	DD	0
	DD	3218513608
	DD	2581293346
	DD	3195469587
	DD	0
	DD	3218509834
	DD	830881235
	DD	3196046772
	DD	0
	DD	3218506062
	DD	335263961
	DD	3197069629
	DD	0
	DD	3218502294
	DD	2419522259
	DD	3196276402
	DD	0
	DD	3218498527
	DD	1779617326
	DD	3196689221
	DD	0
	DD	3218494763
	DD	2768486793
	DD	3195479576
	DD	0
	DD	3218491001
	DD	2457191843
	DD	3195194450
	DD	0
	DD	3218487241
	DD	2979669267
	DD	3196273865
	DD	0
	DD	3218483484
	DD	2520598456
	DD	3195190860
	DD	0
	DD	3218479729
	DD	1404472806
	DD	3195521798
	DD	0
	DD	3218475976
	DD	2933674193
	DD	3196782639
	DD	0
	DD	3218472226
	DD	2155935676
	DD	3196516230
	DD	0
	DD	3218468478
	DD	1776786016
	DD	3194658741
	DD	0
	DD	3218464732
	DD	3208985092
	DD	3196894677
	DD	0
	DD	3218460989
	DD	1414628316
	DD	3194730222
	DD	0
	DD	3218457248
	DD	3264913953
	DD	3196578854
	DD	0
	DD	3218453509
	DD	2212744173
	DD	3196935593
	DD	0
	DD	3218449773
	DD	3219516420
	DD	3196106465
	DD	0
	DD	3218446039
	DD	1174975199
	DD	3196201080
	DD	0
	DD	3218442307
	DD	2518811262
	DD	3195082305
	DD	0
	DD	3218438578
	DD	955422098
	DD	3194678890
	DD	0
	DD	3218434850
	DD	403509013
	DD	3196729007
	DD	0
	DD	3218431125
	DD	1925863372
	DD	3196880179
	DD	0
	DD	3218427403
	DD	3762587332
	DD	3195777038
	DD	0
	DD	3218423683
	DD	2299930842
	DD	3195846699
	DD	0
	DD	3218419965
	DD	3913056099
	DD	3194190159
	DD	0
	DD	3218416249
	DD	1309532869
	DD	3195393925
	DD	0
	DD	3218412536
	DD	1427758304
	DD	3191287204
	DD	0
	DD	3218408824
	DD	3664935924
	DD	3195664594
	DD	0
	DD	3218405116
	DD	56148989
	DD	3193360070
	DD	0
	DD	3218401408
	DD	1546977600
	DD	3196453606
	DD	0
	DD	3218397704
	DD	657959344
	DD	3196266697
	DD	0
	DD	3218394002
	DD	2271720885
	DD	3194864391
	DD	0
	DD	3218390302
	DD	667751116
	DD	3195609145
	DD	0
	DD	3218386605
	DD	2712697666
	DD	3193291133
	DD	0
	DD	3218382909
	DD	634117544
	DD	3196093020
	DD	0
	DD	3218379216
	DD	2714468923
	DD	3196018715
	DD	0
	DD	3218375526
	DD	108477979
	DD	3190599358
	DD	0
	DD	3218371837
	DD	2267170825
	DD	3195187144
	DD	0
	DD	3218368151
	DD	3669671918
	DD	3194008660
	DD	0
	DD	3218364466
	DD	2196723791
	DD	3196549091
	DD	0
	DD	3218360784
	DD	2791195639
	DD	3196915183
	DD	0
	DD	3218357105
	DD	1710231845
	DD	3196395748
	DD	0
	DD	3218353428
	DD	812053421
	DD	3194530267
	DD	0
	DD	3218349753
	DD	677148646
	DD	3195279565
	DD	0
	DD	3218346080
	DD	1770376871
	DD	3194806774
	DD	0
	DD	3218342409
	DD	1232334729
	DD	3196557456
	DD	0
	DD	3218338741
	DD	2877444568
	DD	3192481746
	DD	0
	DD	3218335074
	DD	1231961352
	DD	3197051949
	DD	0
	DD	3218331410
	DD	3148440772
	DD	3196494739
	DD	0
	DD	3218327749
	DD	3402561471
	DD	3192943984
	DD	0
	DD	3218324089
	DD	2767624854
	DD	3194945039
	DD	0
	DD	3218320432
	DD	1944101510
	DD	3192619635
	DD	0
	DD	3218316776
	DD	3808117721
	DD	3196471987
	DD	0
	DD	3218313123
	DD	1320358680
	DD	3197075792
	DD	0
	DD	3218309473
	DD	4220614032
	DD	3193778532
	DD	0
	DD	3218305824
	DD	1798479743
	DD	3196898179
	DD	0
	DD	3218302178
	DD	223350984
	DD	3196138393
	DD	0
	DD	3218298534
	DD	323093589
	DD	3191884737
	DD	0
	DD	3218294892
	DD	3473561564
	DD	3194582292
	DD	0
	DD	3218291252
	DD	1989807800
	DD	3194867291
	DD	0
	DD	3218287614
	DD	3491286165
	DD	3197005341
	DD	0
	DD	3218283979
	DD	1595992051
	DD	3196156752
	DD	0
	DD	3218280346
	DD	2249824945
	DD	3191354847
	DD	0
	DD	3218276715
	DD	3614031323
	DD	3194605328
	DD	0
	DD	3218273086
	DD	710982524
	DD	3195103304
	DD	0
	DD	3218269460
	DD	4122801604
	DD	3192641229
	DD	0
	DD	3218265834
	DD	872411268
	DD	3196949062
	DD	0
	DD	3218262213
	DD	2584080015
	DD	3195355844
	DD	0
	DD	3218258592
	DD	1091729883
	DD	3196766588
	DD	0
	DD	3218254975
	DD	3918597111
	DD	3193137868
	DD	0
	DD	3218251359
	DD	1227822836
	DD	3194904357
	DD	0
	DD	3218247745
	DD	1207867124
	DD	3195330376
	DD	0
	DD	3218244133
	DD	4096823531
	DD	3195418520
	DD	0
	DD	3218240524
	DD	2905773477
	DD	3193944437
	DD	0
	DD	3218236916
	DD	1699117459
	DD	3196779712
	DD	0
	DD	3218233312
	DD	1245991678
	DD	3195346690
	DD	0
	DD	3218229708
	DD	2686753329
	DD	3197002361
	DD	0
	DD	3218226108
	DD	2345509379
	DD	3195416953
	DD	0
	DD	3218222509
	DD	2415203021
	DD	3196493158
	DD	0
	DD	3218218913
	DD	3921604109
	DD	3191962058
	DD	0
	DD	3218215319
	DD	277792045
	DD	3194223162
	DD	0
	DD	3218211726
	DD	3606809116
	DD	3195779816
	DD	0
	DD	3218208136
	DD	3837979753
	DD	3196092304
	DD	0
	DD	3218204548
	DD	1537990937
	DD	3196238895
	DD	0
	DD	3218200962
	DD	3639025436
	DD	3196369572
	DD	0
	DD	3218197378
	DD	3338799915
	DD	3196494415
	DD	0
	DD	3218193797
	DD	771634997
	DD	3196256920
	DD	0
	DD	3218190217
	DD	3459385850
	DD	3196401390
	DD	0
	DD	3218186640
	DD	2411804193
	DD	3196205774
	DD	0
	DD	3218183065
	DD	293719170
	DD	3196033910
	DD	0
	DD	3218179492
	DD	2916290495
	DD	3195809410
	DD	0
	DD	3218175921
	DD	3024635388
	DD	3195698671
	DD	0
	DD	3218172352
	DD	1593723743
	DD	3195721980
	DD	0
	DD	3218168785
	DD	1479963450
	DD	3195899642
	DD	0
	DD	3218165220
	DD	3624935352
	DD	3196155814
	DD	0
	DD	3218161658
	DD	2595462099
	DD	3196072118
	DD	0
	DD	3218158097
	DD	2245272707
	DD	3196454512
	DD	0
	DD	3218154538
	DD	967038611
	DD	3196954786
	DD	0
	DD	3218150983
	DD	1080468995
	DD	3192746537
	DD	0
	DD	3218147428
	DD	3695483798
	DD	3195739041
	DD	0
	DD	3218143876
	DD	1918755805
	DD	3196463254
	DD	0
	DD	3218140326
	DD	2316428375
	DD	3194637714
	DD	0
	DD	3218136778
	DD	3117766005
	DD	3196336694
	DD	0
	DD	3218133232
	DD	2367290997
	DD	3195269664
	DD	0
	DD	3218129688
	DD	2174960832
	DD	3196931070
	DD	0
	DD	3218126146
	DD	4059456029
	DD	3196649238
	DD	0
	DD	3218122607
	DD	2989164202
	DD	3196231288
	DD	0
	DD	3218119069
	DD	912256154
	DD	3196380747
	DD	0
	DD	3218115533
	DD	966295295
	DD	3196760767
	DD	0
	DD	3218112000
	DD	1865482816
	DD	3197038606
	DD	0
	DD	3218108469
	DD	3711231051
	DD	3195569397
	DD	0
	DD	3218104939
	DD	253015153
	DD	3196949072
	DD	0
	DD	3218101412
	DD	3711619112
	DD	3196258273
	DD	0
	DD	3218097887
	DD	1664363626
	DD	3195639578
	DD	0
	DD	3218094364
	DD	1441323873
	DD	3195407215
	DD	0
	DD	3218090842
	DD	2313568823
	DD	3196256975
	DD	0
	DD	3218087323
	DD	1056740186
	DD	3196755788
	DD	0
	DD	3218083806
	DD	2378505439
	DD	3195572058
	DD	0
	DD	3218080291
	DD	2380553244
	DD	3196971131
	DD	0
	DD	3218073813
	DD	3937810052
	DD	3195255951
	DD	0
	DD	3218066791
	DD	223549577
	DD	3195431769
	DD	0
	DD	3218059774
	DD	1159462682
	DD	3192961630
	DD	0
	DD	3218052759
	DD	2767985889
	DD	3193806469
	DD	0
	DD	3218045750
	DD	2871627184
	DD	3194519485
	DD	0
	DD	3218038744
	DD	2938115425
	DD	3193733918
	DD	0
	DD	3218031742
	DD	681625989
	DD	3194787788
	DD	0
	DD	3218024745
	DD	2230411464
	DD	3191903911
	DD	0
	DD	3218017752
	DD	3828352395
	DD	3192273408
	DD	0
	DD	3218010761
	DD	182419510
	DD	3195706084
	DD	0
	DD	3218003777
	DD	1938408115
	DD	3195410319
	DD	0
	DD	3217996796
	DD	2321559771
	DD	3194129984
	DD	0
	DD	3217989819
	DD	4238424546
	DD	3194304268
	DD	0
	DD	3217982845
	DD	2621704586
	DD	3192953609
	DD	0
	DD	3217975875
	DD	4076531572
	DD	3194541514
	DD	0
	DD	3217968910
	DD	57348911
	DD	3192770703
	DD	0
	DD	3217961949
	DD	3409110112
	DD	3194277108
	DD	0
	DD	3217954991
	DD	743327329
	DD	3194275012
	DD	0
	DD	3217948039
	DD	3763808734
	DD	3195058205
	DD	0
	DD	3217941088
	DD	1990262286
	DD	3195699729
	DD	0
	DD	3217934144
	DD	1681663171
	DD	3194393958
	DD	0
	DD	3217927202
	DD	865478140
	DD	3195436168
	DD	0
	DD	3217920265
	DD	900361350
	DD	3195197991
	DD	0
	DD	3217913331
	DD	1247763158
	DD	3194244205
	DD	0
	DD	3217906403
	DD	570303764
	DD	3194197725
	DD	0
	DD	3217899476
	DD	3848949711
	DD	3194792234
	DD	0
	DD	3217892555
	DD	1846465414
	DD	3195788420
	DD	0
	DD	3217885638
	DD	3819865690
	DD	3193288843
	DD	0
	DD	3217878725
	DD	1913743408
	DD	3191850656
	DD	0
	DD	3217871814
	DD	341191762
	DD	3195738722
	DD	0
	DD	3217864909
	DD	3378919128
	DD	3193908369
	DD	0
	DD	3217858008
	DD	2253508503
	DD	3194012596
	DD	0
	DD	3217851110
	DD	896953819
	DD	3193530098
	DD	0
	DD	3217844216
	DD	3940932776
	DD	3195588116
	DD	0
	DD	3217837326
	DD	893082698
	DD	3194115193
	DD	0
	DD	3217830440
	DD	2066242242
	DD	3194827578
	DD	0
	DD	3217823557
	DD	4267520666
	DD	3195172783
	DD	0
	DD	3217816679
	DD	310595324
	DD	3194486781
	DD	0
	DD	3217809805
	DD	592985039
	DD	3195766306
	DD	0
	DD	3217802935
	DD	1179103730
	DD	3194264455
	DD	0
	DD	3217796067
	DD	4056442023
	DD	3195572891
	DD	0
	DD	3217789206
	DD	2525983703
	DD	3195326580
	DD	0
	DD	3217782346
	DD	462639888
	DD	3195665189
	DD	0
	DD	3217775491
	DD	1452163513
	DD	3195442838
	DD	0
	DD	3217768641
	DD	3085668832
	DD	3194350976
	DD	0
	DD	3217761794
	DD	959619812
	DD	3188184364
	DD	0
	DD	3217754951
	DD	3618217958
	DD	3194718935
	DD	0
	DD	3217748111
	DD	2438313840
	DD	3195845675
	DD	0
	DD	3217741275
	DD	622109995
	DD	3194658879
	DD	0
	DD	3217734445
	DD	1380674991
	DD	3194813213
	DD	0
	DD	3217727616
	DD	532356761
	DD	3195697357
	DD	0
	DD	3217720793
	DD	3816638055
	DD	3188822039
	DD	0
	DD	3217713972
	DD	2543132486
	DD	3194062008
	DD	0
	DD	3217707156
	DD	3781500864
	DD	3194442569
	DD	0
	DD	3217700343
	DD	3271981533
	DD	3195127980
	DD	0
	DD	3217693535
	DD	2193594423
	DD	3195202582
	DD	0
	DD	3217686730
	DD	908278928
	DD	3195523219
	DD	0
	DD	3217679930
	DD	1565596
	DD	3195556580
	DD	0
	DD	3217673133
	DD	2150610967
	DD	3195871192
	DD	0
	DD	3217666340
	DD	2927195196
	DD	3193557311
	DD	0
	DD	3217659549
	DD	1577220047
	DD	3195309368
	DD	0
	DD	3217652763
	DD	2822277736
	DD	3196020623
	DD	0
	DD	3217645983
	DD	3902046444
	DD	3193790856
	DD	0
	DD	3217639205
	DD	328998549
	DD	3195331787
	DD	0
	DD	3217632431
	DD	2753795905
	DD	3194000361
	DD	0
	DD	3217625660
	DD	2232384424
	DD	3191624077
	DD	0
	DD	3217618893
	DD	3362724876
	DD	3195632511
	DD	0
	DD	3217612130
	DD	2685500669
	DD	3195523712
	DD	0
	DD	3217605370
	DD	1959170703
	DD	3195864961
	DD	0
	DD	3217598616
	DD	1691145527
	DD	3191177082
	DD	0
	DD	3217591864
	DD	523145001
	DD	3194626765
	DD	0
	DD	3217585116
	DD	696251972
	DD	3189528621
	DD	0
	DD	3217578372
	DD	3458529112
	DD	3195260295
	DD	0
	DD	3217571632
	DD	3133416995
	DD	3194926870
	DD	0
	DD	3217564895
	DD	255919104
	DD	3195230506
	DD	0
	DD	3217558163
	DD	1955031973
	DD	3195548721
	DD	0
	DD	3217551433
	DD	3106947854
	DD	3194725625
	DD	0
	DD	3217544708
	DD	990604043
	DD	3193137117
	DD	0
	DD	3217537986
	DD	2715581070
	DD	3193266789
	DD	0
	DD	3217531269
	DD	2743222724
	DD	3193926060
	DD	0
	DD	3217524555
	DD	4186399851
	DD	3195298689
	DD	0
	DD	3217517845
	DD	3154468969
	DD	3194378244
	DD	0
	DD	3217511138
	DD	1498790854
	DD	3194546238
	DD	0
	DD	3217504434
	DD	2289901185
	DD	3195566617
	DD	0
	DD	3217497736
	DD	1299060885
	DD	3193950469
	DD	0
	DD	3217491039
	DD	1695279173
	DD	3194253799
	DD	0
	DD	3217484347
	DD	2459318143
	DD	3195043485
	DD	0
	DD	3217477660
	DD	1318896562
	DD	3195739065
	DD	0
	DD	3217470975
	DD	3399155144
	DD	3195614926
	DD	0
	DD	3217464294
	DD	2175532535
	DD	3192697768
	DD	0
	DD	3217457617
	DD	2821221487
	DD	3195216209
	DD	0
	DD	3217450944
	DD	1050471715
	DD	3194944558
	DD	0
	DD	3217444274
	DD	2896097874
	DD	3195595654
	DD	0
	DD	3217437608
	DD	448663497
	DD	3194966239
	DD	0
	DD	3217430945
	DD	4271049871
	DD	3195270062
	DD	0
	DD	3217424287
	DD	3223000093
	DD	3195984944
	DD	0
	DD	3217417631
	DD	838735400
	DD	3195996397
	DD	0
	DD	3217410981
	DD	1894172303
	DD	3193537457
	DD	0
	DD	3217404333
	DD	3720049521
	DD	3195818818
	DD	0
	DD	3217397690
	DD	2664115622
	DD	3190944059
	DD	0
	DD	3217391049
	DD	3358571556
	DD	3195339663
	DD	0
	DD	3217384412
	DD	2902988675
	DD	3195551733
	DD	0
	DD	3217377779
	DD	2792446069
	DD	3194365731
	DD	0
	DD	3217371149
	DD	3383719562
	DD	3194715004
	DD	0
	DD	3217364523
	DD	371933453
	DD	3189733585
	DD	0
	DD	3217357901
	DD	2033346561
	DD	3195843446
	DD	0
	DD	3217351282
	DD	2490390795
	DD	3195037178
	DD	0
	DD	3217344668
	DD	871211063
	DD	3194763778
	DD	0
	DD	3217338056
	DD	1435072769
	DD	3192783168
	DD	0
	DD	3217331449
	DD	63108017
	DD	3192895712
	DD	0
	DD	3217324843
	DD	429747946
	DD	3195832726
	DD	0
	DD	3217318244
	DD	3357428628
	DD	3188698891
	DD	0
	DD	3217311645
	DD	4059192451
	DD	3195846727
	DD	0
	DD	3217305053
	DD	1365972095
	DD	3193163994
	DD	0
	DD	3217298463
	DD	181083177
	DD	3196046105
	DD	0
	DD	3217291876
	DD	207665873
	DD	3195278572
	DD	0
	DD	3217285294
	DD	2555026016
	DD	3195333050
	DD	0
	DD	3217278716
	DD	3435214381
	DD	3194110775
	DD	0
	DD	3217272139
	DD	3423519400
	DD	3195499700
	DD	0
	DD	3217265568
	DD	192871548
	DD	3195218744
	DD	0
	DD	3217258999
	DD	834855312
	DD	3194149577
	DD	0
	DD	3217252435
	DD	2755511593
	DD	3194701583
	DD	0
	DD	3217245873
	DD	371981362
	DD	3194596342
	DD	0
	DD	3217239315
	DD	882911353
	DD	3190531251
	DD	0
	DD	3217232762
	DD	2548239619
	DD	3192753122
	DD	0
	DD	3217226211
	DD	2707704866
	DD	3191954429
	DD	0
	DD	3217219663
	DD	2492144320
	DD	3195419582
	DD	0
	DD	3217213120
	DD	1118888178
	DD	3187763775
	DD	0
	DD	3217206580
	DD	3635693166
	DD	3187717376
	DD	0
	DD	3217200043
	DD	2866004879
	DD	3195435060
	DD	0
	DD	3217193511
	DD	3738567713
	DD	3192602551
	DD	0
	DD	3217186982
	DD	2403061065
	DD	3193751041
	DD	0
	DD	3217180455
	DD	3155708000
	DD	3193977689
	DD	0
	DD	3217173933
	DD	2516361431
	DD	3195688294
	DD	0
	DD	3217167414
	DD	1993682103
	DD	3194241861
	DD	0
	DD	3217160900
	DD	931047246
	DD	3194459850
	DD	0
	DD	3217154388
	DD	2357548445
	DD	3194419065
	DD	0
	DD	3217147880
	DD	2522835332
	DD	3192643124
	DD	0
	DD	3217141373
	DD	1879285990
	DD	3195659435
	DD	0
	DD	3217134873
	DD	3818568364
	DD	3192519953
	DD	0
	DD	3217128376
	DD	2341847387
	DD	3193354164
	DD	0
	DD	3217121881
	DD	1044171365
	DD	3193750381
	DD	0
	DD	3217115390
	DD	1466037916
	DD	3191983688
	DD	0
	DD	3217108902
	DD	87123985
	DD	3195472415
	DD	0
	DD	3217102419
	DD	3692756628
	DD	3193408841
	DD	0
	DD	3217095939
	DD	835558996
	DD	3194883530
	DD	0
	DD	3217089461
	DD	235548629
	DD	3195555429
	DD	0
	DD	3217082987
	DD	1473895097
	DD	3195818861
	DD	0
	DD	3217076517
	DD	1468858877
	DD	3195754405
	DD	0
	DD	3217070051
	DD	3476923690
	DD	3195378880
	DD	0
	DD	3217063587
	DD	2083957431
	DD	3195067519
	DD	0
	DD	3217057129
	DD	653174521
	DD	3192063351
	DD	0
	DD	3217050672
	DD	330347861
	DD	3195360039
	DD	0
	DD	3217044220
	DD	4163060375
	DD	3193042742
	DD	0
	DD	3217037769
	DD	655523368
	DD	3195370132
	DD	0
	DD	3217031324
	DD	165033185
	DD	3195917177
	DD	0
	DD	3217018596
	DD	2451734327
	DD	3194021131
	DD	0
	DD	3217005719
	DD	733830196
	DD	3188749387
	DD	0
	DD	3216992845
	DD	448162519
	DD	3194320635
	DD	0
	DD	3216979983
	DD	3541733870
	DD	3194704756
	DD	0
	DD	3216967126
	DD	4007770139
	DD	3192763920
	DD	0
	DD	3216954272
	DD	2542701496
	DD	3194624439
	DD	0
	DD	3216941430
	DD	3210146190
	DD	3194965174
	DD	0
	DD	3216928593
	DD	198424127
	DD	3193435774
	DD	0
	DD	3216915763
	DD	3601349996
	DD	3194382887
	DD	0
	DD	3216902938
	DD	210106919
	DD	3193060543
	DD	0
	DD	3216890120
	DD	2166324646
	DD	3194649837
	DD	0
	DD	3216877311
	DD	3176464770
	DD	3193312737
	DD	0
	DD	3216864509
	DD	1131568387
	DD	3194703264
	DD	0
	DD	3216851712
	DD	2855127652
	DD	3194325577
	DD	0
	DD	3216838923
	DD	987254513
	DD	3193988692
	DD	0
	DD	3216826138
	DD	1880006425
	DD	3194357565
	DD	0
	DD	3216813365
	DD	3612923737
	DD	3194200732
	DD	0
	DD	3216800593
	DD	2104212637
	DD	3192502391
	DD	0
	DD	3216787832
	DD	4098392036
	DD	3194109831
	DD	0
	DD	3216775076
	DD	3032901152
	DD	3193248108
	DD	0
	DD	3216762328
	DD	1510152099
	DD	3192483448
	DD	0
	DD	3216749584
	DD	26573632
	DD	3193889941
	DD	0
	DD	3216736848
	DD	1212773515
	DD	3194812127
	DD	0
	DD	3216724117
	DD	1272384655
	DD	3194523542
	DD	0
	DD	3216711398
	DD	391224244
	DD	3193991875
	DD	0
	DD	3216698680
	DD	174468048
	DD	3188044585
	DD	0
	DD	3216685973
	DD	3495439369
	DD	3193644138
	DD	0
	DD	3216673271
	DD	562110310
	DD	3193255719
	DD	0
	DD	3216660573
	DD	4270374115
	DD	3194472853
	DD	0
	DD	3216647884
	DD	3781289832
	DD	3193529861
	DD	0
	DD	3216635203
	DD	2832059071
	DD	3193159022
	DD	0
	DD	3216622526
	DD	1217340318
	DD	3194516390
	DD	0
	DD	3216609858
	DD	624551246
	DD	3193977051
	DD	0
	DD	3216597194
	DD	4041951210
	DD	3194631542
	DD	0
	DD	3216584539
	DD	2028438232
	DD	3193741669
	DD	0
	DD	3216571888
	DD	93099012
	DD	3194319325
	DD	0
	DD	3216559246
	DD	2456500498
	DD	3192815986
	DD	0
	DD	3216546612
	DD	2761563865
	DD	3192229560
	DD	0
	DD	3216533982
	DD	872643390
	DD	3194450788
	DD	0
	DD	3216521357
	DD	3070664325
	DD	3194890024
	DD	0
	DD	3216508741
	DD	3585410270
	DD	3194078908
	DD	0
	DD	3216496133
	DD	2449281783
	DD	3194149367
	DD	0
	DD	3216483530
	DD	1198491839
	DD	3193155979
	DD	0
	DD	3216470931
	DD	3608918574
	DD	3194428202
	DD	0
	DD	3216458341
	DD	733833448
	DD	3194169666
	DD	0
	DD	3216445759
	DD	803531950
	DD	3194915644
	DD	0
	DD	3216433183
	DD	3589101801
	DD	3190265325
	DD	0
	DD	3216420610
	DD	1148851194
	DD	3194768391
	DD	0
	DD	3216408047
	DD	2202069022
	DD	3192860325
	DD	0
	DD	3216395492
	DD	2324692388
	DD	3191997241
	DD	0
	DD	3216382938
	DD	2903658154
	DD	3190636871
	DD	0
	DD	3216370396
	DD	2203818087
	DD	3193150566
	DD	0
	DD	3216357855
	DD	3512375392
	DD	3194165235
	DD	0
	DD	3216345327
	DD	273359950
	DD	3192927348
	DD	0
	DD	3216332799
	DD	3770494612
	DD	3194953210
	DD	0
	DD	3216320281
	DD	4010399688
	DD	3193182345
	DD	0
	DD	3216307771
	DD	3564491985
	DD	3193101430
	DD	0
	DD	3216295266
	DD	1997150005
	DD	3192075352
	DD	0
	DD	3216282765
	DD	3049829531
	DD	3194782243
	DD	0
	DD	3216270274
	DD	3794804802
	DD	3193179537
	DD	0
	DD	3216257787
	DD	105748591
	DD	3194270847
	DD	0
	DD	3216245305
	DD	650649155
	DD	3194807509
	DD	0
	DD	3216232832
	DD	1242111302
	DD	3194765681
	DD	0
	DD	3216220368
	DD	3100116739
	DD	3194180621
	DD	0
	DD	3216207905
	DD	2823135836
	DD	3193933041
	DD	0
	DD	3216195451
	DD	3349309551
	DD	3192064613
	DD	0
	DD	3216183005
	DD	1914005084
	DD	3194147434
	DD	0
	DD	3216170565
	DD	3820665233
	DD	3189505879
	DD	0
	DD	3216158129
	DD	3243698221
	DD	3193866975
	DD	0
	DD	3216145698
	DD	475994629
	DD	3194903812
	DD	0
	DD	3216133277
	DD	2514433100
	DD	3192955408
	DD	0
	DD	3216120864
	DD	403000866
	DD	3193657282
	DD	0
	DD	3216108452
	DD	4103637128
	DD	3194689542
	DD	0
	DD	3216096050
	DD	3697321750
	DD	3192061993
	DD	0
	DD	3216083652
	DD	3533458410
	DD	3194035854
	DD	0
	DD	3216071263
	DD	505206761
	DD	3194662478
	DD	0
	DD	3216058880
	DD	376084181
	DD	3192858709
	DD	0
	DD	3216046501
	DD	1130964551
	DD	3194539207
	DD	0
	DD	3216034132
	DD	4287884960
	DD	3192963768
	DD	0
	DD	3216021764
	DD	812141044
	DD	3190986496
	DD	0
	DD	3216009408
	DD	2548015311
	DD	3194362997
	DD	0
	DD	3215997054
	DD	3248094678
	DD	3194288464
	DD	0
	DD	3215984709
	DD	2774143461
	DD	3194238141
	DD	0
	DD	3215962147
	DD	3685172673
	DD	3193140277
	DD	0
	DD	3215937478
	DD	34195044
	DD	3192361842
	DD	0
	DD	3215912827
	DD	2634118068
	DD	3191768156
	DD	0
	DD	3215888186
	DD	3176380607
	DD	3193074056
	DD	0
	DD	3215863556
	DD	4073721017
	DD	3192513761
	DD	0
	DD	3215838936
	DD	1630699674
	DD	3193428827
	DD	0
	DD	3215814335
	DD	2883540767
	DD	3192113147
	DD	0
	DD	3215789744
	DD	1945195295
	DD	3192653928
	DD	0
	DD	3215765164
	DD	1680941704
	DD	3191066642
	DD	0
	DD	3215740594
	DD	454148743
	DD	3192837946
	DD	0
	DD	3215716043
	DD	1642744018
	DD	3190984023
	DD	0
	DD	3215691502
	DD	1469759626
	DD	3192792921
	DD	0
	DD	3215666972
	DD	2072896110
	DD	3192842898
	DD	0
	DD	3215642453
	DD	1128375468
	DD	3191473132
	DD	0
	DD	3215617944
	DD	1700889023
	DD	3193237433
	DD	0
	DD	3215593454
	DD	2152031588
	DD	3193084582
	DD	0
	DD	3215568975
	DD	3153088533
	DD	3192043023
	DD	0
	DD	3215544506
	DD	948524716
	DD	3193563725
	DD	0
	DD	3215520056
	DD	2379930838
	DD	3193733647
	DD	0
	DD	3215495610
	DD	860641527
	DD	3190284378
	DD	0
	DD	3215471181
	DD	2998535030
	DD	3193778729
	DD	0
	DD	3215446764
	DD	3230587212
	DD	3193200247
	DD	0
	DD	3215422358
	DD	3069793617
	DD	3192026710
	DD	0
	DD	3215397962
	DD	2480079877
	DD	3193733793
	DD	0
	DD	3215373578
	DD	524036170
	DD	3192774333
	DD	0
	DD	3215349212
	DD	2174341080
	DD	3193543696
	DD	0
	DD	3215324858
	DD	942207192
	DD	3191115295
	DD	0
	DD	3215300514
	DD	3101644490
	DD	3193061566
	DD	0
	DD	3215276182
	DD	923504793
	DD	3189435749
	DD	0
	DD	3215251860
	DD	1829313629
	DD	3193186582
	DD	0
	DD	3215227550
	DD	112678546
	DD	3192182235
	DD	0
	DD	3215203258
	DD	3866114170
	DD	3193851890
	DD	0
	DD	3215178970
	DD	1852268242
	DD	3193861087
	DD	0
	DD	3215154701
	DD	1257683952
	DD	3193827993
	DD	0
	DD	3215130444
	DD	1707073265
	DD	3190923423
	DD	0
	DD	3215106197
	DD	4268388449
	DD	3193082042
	DD	0
	DD	3215081962
	DD	3292217729
	DD	3192017233
	DD	0
	DD	3215057738
	DD	3227804721
	DD	3191986395
	DD	0
	DD	3215033525
	DD	2004651165
	DD	3193084076
	DD	0
	DD	3215009332
	DD	3622306650
	DD	3188761337
	DD	0
	DD	3214985142
	DD	1712312456
	DD	3188440140
	DD	0
	DD	3214960971
	DD	3079998739
	DD	3191653236
	DD	0
	DD	3214936811
	DD	2900099685
	DD	3193610328
	DD	0
	DD	3214891296
	DD	2120086208
	DD	3189021517
	DD	0
	DD	3214843023
	DD	3884702627
	DD	3188762535
	DD	0
	DD	3214794773
	DD	2542646001
	DD	3185584669
	DD	0
	DD	3214746545
	DD	2726758030
	DD	3192820263
	DD	0
	DD	3214698341
	DD	157783032
	DD	3192816993
	DD	0
	DD	3214650161
	DD	571973410
	DD	3188287713
	DD	0
	DD	3214602003
	DD	3235619407
	DD	3190981733
	DD	0
	DD	3214553868
	DD	836818699
	DD	3192325406
	DD	0
	DD	3214505757
	DD	1979660954
	DD	3191388370
	DD	0
	DD	3214457685
	DD	2307406117
	DD	3188861915
	DD	0
	DD	3214409620
	DD	1565589195
	DD	3191274488
	DD	0
	DD	3214361579
	DD	2395778116
	DD	3187178682
	DD	0
	DD	3214313576
	DD	2148275312
	DD	3192640893
	DD	0
	DD	3214265582
	DD	945215022
	DD	3191595667
	DD	0
	DD	3214217611
	DD	3773939323
	DD	3191903776
	DD	0
	DD	3214169679
	DD	3479353361
	DD	3192800037
	DD	0
	DD	3214121756
	DD	2942969280
	DD	3190480617
	DD	0
	DD	3214073856
	DD	3532612553
	DD	3189820998
	DD	0
	DD	3214025995
	DD	2679569931
	DD	3191994026
	DD	0
	DD	3213978142
	DD	2572640283
	DD	3192799552
	DD	0
	DD	3213930329
	DD	2788579501
	DD	3192804325
	DD	0
	DD	3213879609
	DD	1298308477
	DD	3191791314
	DD	0
	DD	3213784047
	DD	3934074411
	DD	3191482834
	DD	0
	DD	3213688565
	DD	4266305381
	DD	3189649017
	DD	0
	DD	3213593098
	DD	41558379
	DD	3191768965
	DD	0
	DD	3213497680
	DD	3584605677
	DD	3191316852
	DD	0
	DD	3213402310
	DD	2402932686
	DD	3191201256
	DD	0
	DD	3213306988
	DD	1061463941
	DD	3191620074
	DD	0
	DD	3213211747
	DD	4292342829
	DD	3189329001
	DD	0
	DD	3213116522
	DD	220507961
	DD	3189399118
	DD	0
	DD	3213021345
	DD	3949738008
	DD	3191346705
	DD	0
	DD	3212926217
	DD	2630220167
	DD	3191723482
	DD	0
	DD	3212825413
	DD	3769653518
	DD	3190144384
	DD	0
	DD	3212635289
	DD	2968429840
	DD	3189564539
	DD	0
	DD	3212445326
	DD	8877429
	DD	3190217697
	DD	0
	DD	3212255462
	DD	347726252
	DD	3181028598
	DD	0
	DD	3212065631
	DD	2984998978
	DD	3190233750
	DD	0
	DD	3211875963
	DD	356621
	DD	3189971190
	DD	0
	DD	3211584372
	DD	304813922
	DD	3188488782
	DD	0
	DD	3211205430
	DD	3822195921
	DD	3189540305
	DD	0
	DD	3210826559
	DD	1638539092
	DD	3188619579
	DD	0
	DD	3210156060
	DD	1163243384
	DD	3187747178
	DD	0
	DD	3209107086
	DD	887880220
	DD	3186822511
	DD	0
	DD	0
	DD	0
	DD	0
	DB 0
	ORG $+46
	DB	0
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	2851812149
	DD	1072698941
	DD	2595802551
	DD	1016815913
	DD	1048019041
	DD	1072704666
	DD	1398474845
	DD	3161559171
	DD	3899555717
	DD	1072710421
	DD	427280750
	DD	3163595548
	DD	3541402996
	DD	1072716208
	DD	2759177317
	DD	1015903202
	DD	702412510
	DD	1072722027
	DD	3803266086
	DD	3163328991
	DD	410360776
	DD	1072727877
	DD	1269990655
	DD	1013024446
	DD	3402036099
	DD	1072733758
	DD	405889333
	DD	1016154232
	DD	1828292879
	DD	1072739672
	DD	1255956746
	DD	1016636974
	DD	728909815
	DD	1072745618
	DD	383930225
	DD	1016078044
	DD	852742562
	DD	1072751596
	DD	667253586
	DD	1010842135
	DD	2952712987
	DD	1072757606
	DD	3293494651
	DD	3161168877
	DD	3490863953
	DD	1072763649
	DD	960797497
	DD	3163997456
	DD	3228316108
	DD	1072769725
	DD	3010241991
	DD	3159471380
	DD	2930322912
	DD	1072775834
	DD	2599499422
	DD	3163762623
	DD	3366293073
	DD	1072781976
	DD	3119426313
	DD	1015169130
	DD	1014845819
	DD	1072788152
	DD	3117910645
	DD	3162607681
	DD	948735466
	DD	1072794361
	DD	3516338027
	DD	3163623459
	DD	3949972341
	DD	1072800603
	DD	2068408548
	DD	1015962444
	DD	2214878420
	DD	1072806880
	DD	892270087
	DD	3164164998
	DD	828946858
	DD	1072813191
	DD	10642492
	DD	1016988014
	DD	586995997
	DD	1072819536
	DD	41662347
	DD	3163676568
	DD	2288159958
	DD	1072825915
	DD	2169144468
	DD	1015924597
	DD	2440944790
	DD	1072832329
	DD	2492769773
	DD	1015196030
	DD	1853186616
	DD	1072838778
	DD	3066496370
	DD	1016705150
	DD	1337108031
	DD	1072845262
	DD	3203724452
	DD	1015726421
	DD	1709341917
	DD	1072851781
	DD	2571168217
	DD	1015201075
	DD	3790955393
	DD	1072858335
	DD	2352942461
	DD	3164228666
	DD	4112506593
	DD	1072864925
	DD	2947355221
	DD	1015419624
	DD	3504003472
	DD	1072871551
	DD	3594001059
	DD	3158379228
	DD	2799960843
	DD	1072878213
	DD	1423655380
	DD	1016070727
	DD	2839424854
	DD	1072884911
	DD	1171596163
	DD	1014090255
	DD	171030293
	DD	1072891646
	DD	3526460132
	DD	1015477354
	DD	4232894513
	DD	1072898416
	DD	2383938684
	DD	1015717095
	DD	2992903935
	DD	1072905224
	DD	2218154405
	DD	1016276769
	DD	1603444721
	DD	1072912069
	DD	1548633640
	DD	3163249902
	DD	926591435
	DD	1072918951
	DD	3208833761
	DD	3163962090
	DD	1829099622
	DD	1072925870
	DD	1016661180
	DD	3164509581
	DD	887463927
	DD	1072932827
	DD	3596744162
	DD	3161842742
	DD	3272845541
	DD	1072939821
	DD	928852419
	DD	3164536824
	DD	1276261410
	DD	1072946854
	DD	300981947
	DD	1015732745
	DD	78413852
	DD	1072953925
	DD	4183226867
	DD	3164065827
	DD	569847338
	DD	1072961034
	DD	472945272
	DD	3160339305
	DD	3645941911
	DD	1072968181
	DD	3814685080
	DD	3162621917
	DD	1617004845
	DD	1072975368
	DD	82804943
	DD	1011391354
	DD	3978100823
	DD	1072982593
	DD	3513027190
	DD	1016894539
	DD	3049340112
	DD	1072989858
	DD	3062915824
	DD	1014219171
	DD	4040676318
	DD	1072997162
	DD	4090609238
	DD	1016712034
	DD	3577096743
	DD	1073004506
	DD	2951496418
	DD	1014842263
	DD	2583551245
	DD	1073011890
	DD	3161094195
	DD	1016655067
	DD	1990012071
	DD	1073019314
	DD	3529070563
	DD	3163861769
	DD	2731501122
	DD	1073026778
	DD	1774031854
	DD	3163518597
	DD	1453150082
	DD	1073034283
	DD	498154668
	DD	3162536638
	DD	3395129871
	DD	1073041828
	DD	4025345434
	DD	3163383964
	DD	917841882
	DD	1073049415
	DD	18715564
	DD	1016707884
	DD	3566716925
	DD	1073057042
	DD	1536826855
	DD	1015191009
	DD	3712504873
	DD	1073064711
	DD	88491948
	DD	1016476236
	DD	2321106615
	DD	1073072422
	DD	2171176610
	DD	1010584347
	DD	363667784
	DD	1073080175
	DD	813753949
	DD	1016833785
	DD	3111574537
	DD	1073087969
	DD	2606161479
	DD	3163808322
	DD	2956612997
	DD	1073095806
	DD	2118169750
	DD	3163784129
	DD	885834528
	DD	1073103686
	DD	1973258546
	DD	3163310140
	DD	2186617381
	DD	1073111608
	DD	2270764083
	DD	3164321289
	DD	3561793907
	DD	1073119573
	DD	1157054052
	DD	1012938926
	DD	1719614413
	DD	1073127582
	DD	330458197
	DD	3164331316
	DD	1963711167
	DD	1073135634
	DD	1744767756
	DD	3161622870
	DD	1013258799
	DD	1073143730
	DD	1748797610
	DD	3161177658
	DD	4182873220
	DD	1073151869
	DD	629542646
	DD	3163044879
	DD	3907805044
	DD	1073160053
	DD	2257091225
	DD	3162598983
	DD	1218806132
	DD	1073168282
	DD	1818613051
	DD	3163597017
	DD	1447192521
	DD	1073176555
	DD	1462857171
	DD	3163563097
	DD	1339972927
	DD	1073184873
	DD	167908908
	DD	1016620728
	DD	1944781191
	DD	1073193236
	DD	3993278767
	DD	3162772855
	DD	19972402
	DD	1073201645
	DD	3507899861
	DD	1017057868
	DD	919555682
	DD	1073210099
	DD	3121969534
	DD	1013996802
	DD	1413356050
	DD	1073218599
	DD	1651349290
	DD	3163716742
	DD	2571947539
	DD	1073227145
	DD	3558159063
	DD	3164425245
	DD	1176749997
	DD	1073235738
	DD	2738998779
	DD	3163084420
	DD	2604962541
	DD	1073244377
	DD	2614425274
	DD	3164587768
	DD	3649726105
	DD	1073253063
	DD	4085036346
	DD	1016698050
	DD	1110089947
	DD	1073261797
	DD	1451641638
	DD	1016523249
	DD	380978316
	DD	1073270578
	DD	854188970
	DD	3161511262
	DD	2568320822
	DD	1073279406
	DD	2732824428
	DD	1015401491
	DD	194117574
	DD	1073288283
	DD	777528611
	DD	3164460665
	DD	2966275557
	DD	1073297207
	DD	2176155323
	DD	3160891335
	DD	3418903055
	DD	1073306180
	DD	2527457337
	DD	3161869180
	DD	2682146384
	DD	1073315202
	DD	2082178512
	DD	3164411995
	DD	1892288442
	DD	1073324273
	DD	2446255666
	DD	3163648957
	DD	2191782032
	DD	1073333393
	DD	2960257726
	DD	1014791238
	DD	434316067
	DD	1073342563
	DD	2028358766
	DD	1014506698
	DD	2069751141
	DD	1073351782
	DD	1562170674
	DD	3163773257
	DD	3964284211
	DD	1073361051
	DD	2111583915
	DD	1016475740
	DD	2990417245
	DD	1073370371
	DD	3683467745
	DD	3164417902
	DD	321958744
	DD	1073379742
	DD	3401933766
	DD	1016843134
	DD	1434058175
	DD	1073389163
	DD	251133233
	DD	1016134345
	DD	3218338682
	DD	1073398635
	DD	3404164304
	DD	3163525684
	DD	2572866477
	DD	1073408159
	DD	878562433
	DD	1016570317
	DD	697153126
	DD	1073417735
	DD	1283515428
	DD	3164331765
	DD	3092190715
	DD	1073427362
	DD	814012167
	DD	3160571998
	DD	2380618042
	DD	1073437042
	DD	3149557219
	DD	3164369375
	DD	4076559943
	DD	1073446774
	DD	2119478330
	DD	3161806927
	DD	815859274
	DD	1073456560
	DD	240396590
	DD	3164536019
	DD	2420883922
	DD	1073466398
	DD	2049810052
	DD	1015168464
	DD	1540824585
	DD	1073476290
	DD	1064017010
	DD	3164536266
	DD	3716502172
	DD	1073486235
	DD	2303740125
	DD	1015091301
	DD	1610600570
	DD	1073496235
	DD	3766732298
	DD	1016808759
	DD	777507147
	DD	1073506289
	DD	4282924204
	DD	1016236109
	DD	2483480501
	DD	1073516397
	DD	1216371780
	DD	1014082748
	DD	3706687593
	DD	1073526560
	DD	3521726939
	DD	1014301643
	DD	1432208378
	DD	1073536779
	DD	1401068914
	DD	3163412539
	DD	1242007932
	DD	1073547053
	DD	1132034716
	DD	3164388407
	DD	135105010
	DD	1073557383
	DD	1906148727
	DD	3164424315
	DD	3707479175
	DD	1073567768
	DD	3613079302
	DD	1015213314
	DD	382305176
	DD	1073578211
	DD	2347622376
	DD	3163627201
	DD	64696965
	DD	1073588710
	DD	1768797490
	DD	1016865536
	DD	4076975200
	DD	1073599265
	DD	2029000898
	DD	1016257111
	DD	863738719
	DD	1073609879
	DD	1326992219
	DD	3163661773
	DD	351641897
	DD	1073620550
	DD	2172261526
	DD	3164059175
	DD	3884662774
	DD	1073631278
	DD	2158611599
	DD	1015258761
	DD	4224142467
	DD	1073642065
	DD	3389820385
	DD	1016255778
	DD	2728693978
	DD	1073652911
	DD	396109971
	DD	3164511267
	DD	764307441
	DD	1073663816
	DD	3021057420
	DD	3164378099
	DD	3999357479
	DD	1073674779
	DD	2258941616
	DD	1016973300
	DD	929806999
	DD	1073685803
	DD	3205336643
	DD	1016308133
	DD	1533953344
	DD	1073696886
	DD	769171850
	DD	1016714209
	DD	2912730644
	DD	1073708029
	DD	3490067721
	DD	3164453650
	DD	2174652632
	DD	1073719233
	DD	4087714590
	DD	1015498835
	DD	730821105
	DD	1073730498
	DD	2523232743
	DD	1013115764
	DD	3478756438
	DD	1066416464
	DD	3478756438
	DD	1066416464
	DD	3478756438
	DD	1066416464
	DD	3478756438
	DD	1066416464
	DD	3478756438
	DD	1066416464
	DD	3478756438
	DD	1066416464
	DD	3478756438
	DD	1066416464
	DD	3478756438
	DD	1066416464
	DD	3913391954
	DD	3214626464
	DD	3913391954
	DD	3214626464
	DD	3913391954
	DD	3214626464
	DD	3913391954
	DD	3214626464
	DD	3913391954
	DD	3214626464
	DD	3913391954
	DD	3214626464
	DD	3913391954
	DD	3214626464
	DD	3913391954
	DD	3214626464
	DD	3724320646
	DD	1067950900
	DD	3724320646
	DD	1067950900
	DD	3724320646
	DD	1067950900
	DD	3724320646
	DD	1067950900
	DD	3724320646
	DD	1067950900
	DD	3724320646
	DD	1067950900
	DD	3724320646
	DD	1067950900
	DD	3724320646
	DD	1067950900
	DD	713798755
	DD	3216330823
	DD	713798755
	DD	3216330823
	DD	713798755
	DD	3216330823
	DD	713798755
	DD	3216330823
	DD	713798755
	DD	3216330823
	DD	713798755
	DD	3216330823
	DD	713798755
	DD	3216330823
	DD	713798755
	DD	3216330823
	DD	1655945238
	DD	1069842388
	DD	1655945238
	DD	1069842388
	DD	1655945238
	DD	1069842388
	DD	1655945238
	DD	1069842388
	DD	1655945238
	DD	1069842388
	DD	1655945238
	DD	1069842388
	DD	1655945238
	DD	1069842388
	DD	1655945238
	DD	1069842388
	DD	1181303047
	DD	3218484803
	DD	1181303047
	DD	3218484803
	DD	1181303047
	DD	3218484803
	DD	1181303047
	DD	3218484803
	DD	1181303047
	DD	3218484803
	DD	1181303047
	DD	3218484803
	DD	1181303047
	DD	3218484803
	DD	1181303047
	DD	3218484803
	DD	2523158510
	DD	1048167334
	DD	2523158510
	DD	1048167334
	DD	2523158510
	DD	1048167334
	DD	2523158510
	DD	1048167334
	DD	2523158510
	DD	1048167334
	DD	2523158510
	DD	1048167334
	DD	2523158510
	DD	1048167334
	DD	2523158510
	DD	1048167334
	DD	3884607281
	DD	1062590591
	DD	3884607281
	DD	1062590591
	DD	3884607281
	DD	1062590591
	DD	3884607281
	DD	1062590591
	DD	3884607281
	DD	1062590591
	DD	3884607281
	DD	1062590591
	DD	3884607281
	DD	1062590591
	DD	3884607281
	DD	1062590591
	DD	1874480759
	DD	1065595563
	DD	1874480759
	DD	1065595563
	DD	1874480759
	DD	1065595563
	DD	1874480759
	DD	1065595563
	DD	1874480759
	DD	1065595563
	DD	1874480759
	DD	1065595563
	DD	1874480759
	DD	1065595563
	DD	1874480759
	DD	1065595563
	DD	3607404735
	DD	1068264200
	DD	3607404735
	DD	1068264200
	DD	3607404735
	DD	1068264200
	DD	3607404735
	DD	1068264200
	DD	3607404735
	DD	1068264200
	DD	3607404735
	DD	1068264200
	DD	3607404735
	DD	1068264200
	DD	3607404735
	DD	1068264200
	DD	4286760334
	DD	1070514109
	DD	4286760334
	DD	1070514109
	DD	4286760334
	DD	1070514109
	DD	4286760334
	DD	1070514109
	DD	4286760334
	DD	1070514109
	DD	4286760334
	DD	1070514109
	DD	4286760334
	DD	1070514109
	DD	4286760334
	DD	1070514109
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
	DD	1072168448
	DD	0
	DD	1072168448
	DD	0
	DD	1072168448
	DD	0
	DD	1072168448
	DD	0
	DD	1072168448
	DD	0
	DD	1072168448
	DD	0
	DD	1072168448
	DD	0
	DD	1072168448
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
	DD	1094189056
	DD	0
	DD	1094189056
	DD	0
	DD	1094189056
	DD	0
	DD	1094189056
	DD	0
	DD	1094189056
	DD	0
	DD	1094189056
	DD	0
	DD	1094189056
	DD	0
	DD	1094189056
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	4160749568
	DD	4294967295
	DD	1073741824
	DD	3220641095
	DD	1073741824
	DD	3220641095
	DD	1073741824
	DD	3220641095
	DD	1073741824
	DD	3220641095
	DD	1073741824
	DD	3220641095
	DD	1073741824
	DD	3220641095
	DD	1073741824
	DD	3220641095
	DD	1073741824
	DD	3220641095
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
	DD	0
	DD	1120403456
	DD	0
	DD	1120403456
	DD	0
	DD	1120403456
	DD	0
	DD	1120403456
	DD	0
	DD	1120403456
	DD	0
	DD	1120403456
	DD	0
	DD	1120403456
	DD	0
	DD	1120403456
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
	DD	1127219200
	DD	0
	DD	1127219200
	DD	0
	DD	1127219200
	DD	0
	DD	1127219200
	DD	0
	DD	1127219200
	DD	0
	DD	1127219200
	DD	0
	DD	1127219200
	DD	0
	DD	1127219200
	DD	0
	DD	956301312
	DD	0
	DD	956301312
	DD	0
	DD	956301312
	DD	0
	DD	956301312
	DD	0
	DD	956301312
	DD	0
	DD	956301312
	DD	0
	DD	956301312
	DD	0
	DD	956301312
	DD	1048576
	DD	1048576
	DD	1048576
	DD	1048576
	DD	1048576
	DD	1048576
	DD	1048576
	DD	1048576
	DD	1048576
	DD	1048576
	DD	1048576
	DD	1048576
	DD	1048576
	DD	1048576
	DD	1048576
	DD	1048576
	DD	2097152
	DD	2097152
	DD	2097152
	DD	2097152
	DD	2097152
	DD	2097152
	DD	2097152
	DD	2097152
	DD	2097152
	DD	2097152
	DD	2097152
	DD	2097152
	DD	2097152
	DD	2097152
	DD	2097152
	DD	2097152
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
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1083129855
	DD	1048064
	DD	1048064
	DD	1048064
	DD	1048064
	DD	1048064
	DD	1048064
	DD	1048064
	DD	1048064
	DD	1048064
	DD	1048064
	DD	1048064
	DD	1048064
	DD	1048064
	DD	1048064
	DD	1048064
	DD	1048064
	DD	512
	DD	512
	DD	512
	DD	512
	DD	512
	DD	512
	DD	512
	DD	512
	DD	512
	DD	512
	DD	512
	DD	512
	DD	512
	DD	512
	DD	512
	DD	512
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1072168448
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1094189056
	DD	1072693248
	DD	1072693248
	DD	1072693248
	DD	1072693248
	DD	1072693248
	DD	1072693248
	DD	1072693248
	DD	1072693248
	DD	1072693248
	DD	1072693248
	DD	1072693248
	DD	1072693248
	DD	1072693248
	DD	1072693248
	DD	1072693248
	DD	1072693248
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
	DD 32 DUP (0H)	
_vmldPowHATab	DD	0
	DD	1072693248
	DD	0
	DD	1072629696
	DD	0
	DD	1072569856
	DD	0
	DD	1072513472
	DD	0
	DD	1072460224
	DD	0
	DD	1072409856
	DD	0
	DD	1072362112
	DD	0
	DD	1072316864
	DD	0
	DD	1072273792
	DD	0
	DD	1072232896
	DD	0
	DD	1072193920
	DD	0
	DD	1072156736
	DD	0
	DD	1072121280
	DD	0
	DD	1072087424
	DD	0
	DD	1072054976
	DD	0
	DD	1072023936
	DD	0
	DD	1071994176
	DD	0
	DD	1071965696
	DD	0
	DD	1071938304
	DD	0
	DD	1071911936
	DD	0
	DD	1071886656
	DD	0
	DD	1071862272
	DD	0
	DD	1071838848
	DD	0
	DD	1071816256
	DD	0
	DD	1071794496
	DD	0
	DD	1071773440
	DD	0
	DD	1071753152
	DD	0
	DD	1071733504
	DD	0
	DD	1071714560
	DD	0
	DD	1071696256
	DD	0
	DD	1071678528
	DD	0
	DD	1071661312
	DD	0
	DD	1071644672
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2686386176
	DD	1067891457
	DD	1949948785
	DD	1027381598
	DD	1341652992
	DD	1068918120
	DD	2376679344
	DD	1026589938
	DD	2182004736
	DD	1069583575
	DD	297009671
	DD	1026900933
	DD	1687183360
	DD	1069924424
	DD	2120169064
	DD	1026082260
	DD	53207040
	DD	1070255920
	DD	3737096550
	DD	1026438963
	DD	3818315776
	DD	1070578756
	DD	677794872
	DD	1028109305
	DD	2429726720
	DD	1070744485
	DD	3907638365
	DD	1027382133
	DD	2702757888
	DD	1070897876
	DD	1929563302
	DD	1027984695
	DD	2465140736
	DD	1071047207
	DD	243175481
	DD	1026641700
	DD	2657701888
	DD	1071193041
	DD	3841377895
	DD	1028504382
	DD	658427904
	DD	1071335525
	DD	161357665
	DD	1028306250
	DD	539168768
	DD	1071474585
	DD	2531816708
	DD	1025043792
	DD	2658430976
	DD	1071610420
	DD	2178519328
	DD	1028288112
	DD	1355743232
	DD	1071694102
	DD	3943781029
	DD	1028003666
	DD	1854838784
	DD	1071759170
	DD	1812291414
	DD	1027042047
	DD	473251840
	DD	3218771869
	DD	1330616404
	DD	3175482613
	DD	2315530240
	DD	3218647330
	DD	3482179716
	DD	3175726112
	DD	3886694400
	DD	3218525081
	DD	3584491563
	DD	3175164762
	DD	1568866304
	DD	3218405023
	DD	3528175174
	DD	3174626157
	DD	4172640256
	DD	3218287637
	DD	3760034354
	DD	3171774178
	DD	3545214976
	DD	3218172213
	DD	881689765
	DD	3173077446
	DD	2121375744
	DD	3218038698
	DD	549802690
	DD	3174897014
	DD	492560384
	DD	3217816668
	DD	239252792
	DD	3173483664
	DD	155754496
	DD	3217598893
	DD	1693604438
	DD	3175909818
	DD	4285202432
	DD	3217384365
	DD	127148739
	DD	3175942199
	DD	41181184
	DD	3217174003
	DD	3260046653
	DD	3174058211
	DD	2465087488
	DD	3216902292
	DD	4241850247
	DD	3175110025
	DD	1101037568
	DD	3216495763
	DD	3170347605
	DD	3176066808
	DD	3478798336
	DD	3216096373
	DD	329155479
	DD	3175972274
	DD	3246555136
	DD	3215423741
	DD	4071576371
	DD	3174315914
	DD	830078976
	DD	3214361213
	DD	1258533012
	DD	3175547121
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	1072689152
	DD	0
	DD	1072685056
	DD	0
	DD	1072681024
	DD	0
	DD	1072676992
	DD	0
	DD	1072672960
	DD	0
	DD	1072668928
	DD	0
	DD	1072664960
	DD	0
	DD	1072660992
	DD	0
	DD	1072657024
	DD	0
	DD	1072653056
	DD	0
	DD	1072649152
	DD	0
	DD	1072645248
	DD	0
	DD	1072641344
	DD	0
	DD	1072637440
	DD	0
	DD	1072710976
	DD	0
	DD	1072709888
	DD	0
	DD	1072708864
	DD	0
	DD	1072707776
	DD	0
	DD	1072706752
	DD	0
	DD	1072705664
	DD	0
	DD	1072704640
	DD	0
	DD	1072703616
	DD	0
	DD	1072702528
	DD	0
	DD	1072701504
	DD	0
	DD	1072700480
	DD	0
	DD	1072699456
	DD	0
	DD	1072698368
	DD	0
	DD	1072697344
	DD	0
	DD	1072696320
	DD	0
	DD	1072695296
	DD	0
	DD	1072694272
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2754084864
	DD	1063721742
	DD	2557931335
	DD	1028226920
	DD	3228041216
	DD	1064771801
	DD	930662348
	DD	1027873525
	DD	2323251200
	DD	1065436614
	DD	2596299912
	DD	1027915217
	DD	1641152512
	DD	1065811444
	DD	1188689655
	DD	1027383036
	DD	895221760
	DD	1066187001
	DD	2918954073
	DD	1026717129
	DD	3962896384
	DD	1066482539
	DD	1338190555
	DD	1024402868
	DD	2071330816
	DD	1066668054
	DD	2834125591
	DD	1027573772
	DD	830078976
	DD	1066853925
	DD	1683363035
	DD	1027948302
	DD	1828782080
	DD	1067040153
	DD	874130859
	DD	1026348678
	DD	2395996160
	DD	1067226740
	DD	1724975876
	DD	1028585613
	DD	3558866944
	DD	1067410669
	DD	2189961434
	DD	1027936707
	DD	2542927872
	DD	1067522658
	DD	3621009110
	DD	1028493916
	DD	4208394240
	DD	1067614973
	DD	2777386350
	DD	1028255456
	DD	3217162240
	DD	1067707465
	DD	772669574
	DD	1028516547
	DD	824377344
	DD	3214460051
	DD	1593617402
	DD	3175722247
	DD	830078976
	DD	3214361213
	DD	1258533012
	DD	3175547121
	DD	4002480128
	DD	3214268096
	DD	1397883555
	DD	3175764245
	DD	2914385920
	DD	3214169062
	DD	3775067953
	DD	3175176772
	DD	1460142080
	DD	3214075761
	DD	1592372614
	DD	3175907032
	DD	219152384
	DD	3213976530
	DD	1716511551
	DD	3175540921
	DD	3419144192
	DD	3213880645
	DD	1128677462
	DD	3174560569
	DD	3320446976
	DD	3213693490
	DD	2965227743
	DD	3172454196
	DD	677904384
	DD	3213494440
	DD	4029390031
	DD	3174409513
	DD	1290797056
	DD	3213306911
	DD	1477436787
	DD	3173730612
	DD	2800877568
	DD	3213119200
	DD	4281418519
	DD	3173304523
	DD	3692822528
	DD	3212931307
	DD	751117103
	DD	3175382448
	DD	2547253248
	DD	3212626079
	DD	2419265147
	DD	3175328924
	DD	1836580864
	DD	3212249540
	DD	1456335141
	DD	3175441338
	DD	3438542848
	DD	3211872634
	DD	3721652080
	DD	3176073447
	DD	4278714368
	DD	3211202435
	DD	836003693
	DD	3174279974
	DD	926941184
	DD	3210154597
	DD	4249864733
	DD	3174015648
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1073741824
	DD	1073157447
	DD	0
	DD	1073157401
	DD	0
	DD	1073157355
	DD	3221225472
	DD	1073157308
	DD	2147483648
	DD	1073157262
	DD	2147483648
	DD	1073157216
	DD	1073741824
	DD	1073157170
	DD	1073741824
	DD	1073157124
	DD	0
	DD	1073157078
	DD	3221225472
	DD	1073157031
	DD	3221225472
	DD	1073156985
	DD	2147483648
	DD	1073156939
	DD	2147483648
	DD	1073156893
	DD	1073741824
	DD	1073156847
	DD	1073741824
	DD	1073156801
	DD	0
	DD	1073156755
	DD	0
	DD	1073156709
	DD	3221225472
	DD	1073156662
	DD	3221225472
	DD	1073156616
	DD	2147483648
	DD	1073156570
	DD	2147483648
	DD	1073156524
	DD	2147483648
	DD	1073156478
	DD	1073741824
	DD	1073156432
	DD	1073741824
	DD	1073156386
	DD	0
	DD	1073156340
	DD	0
	DD	1073156294
	DD	0
	DD	1073156248
	DD	3221225472
	DD	1073156201
	DD	3221225472
	DD	1073156155
	DD	2147483648
	DD	1073156109
	DD	2147483648
	DD	1073156063
	DD	2147483648
	DD	1073156017
	DD	1073741824
	DD	1073155971
	DD	1073741824
	DD	1073155925
	DD	1073741824
	DD	1073155879
	DD	1073741824
	DD	1073155833
	DD	0
	DD	1073155787
	DD	0
	DD	1073155741
	DD	0
	DD	1073155695
	DD	0
	DD	1073155649
	DD	3221225472
	DD	1073155602
	DD	3221225472
	DD	1073155556
	DD	3221225472
	DD	1073155510
	DD	3221225472
	DD	1073155464
	DD	3221225472
	DD	1073155418
	DD	2147483648
	DD	1073155372
	DD	2147483648
	DD	1073155326
	DD	2147483648
	DD	1073155280
	DD	2147483648
	DD	1073155234
	DD	2147483648
	DD	1073155188
	DD	2147483648
	DD	1073155142
	DD	2147483648
	DD	1073155096
	DD	2147483648
	DD	1073155050
	DD	2147483648
	DD	1073155004
	DD	1073741824
	DD	1073154958
	DD	1073741824
	DD	1073154912
	DD	1073741824
	DD	1073154866
	DD	1073741824
	DD	1073154820
	DD	1073741824
	DD	1073154774
	DD	1073741824
	DD	1073154728
	DD	1073741824
	DD	1073154682
	DD	2147483648
	DD	1073158995
	DD	1073741824
	DD	1073158972
	DD	1073741824
	DD	1073158949
	DD	0
	DD	1073158926
	DD	0
	DD	1073158903
	DD	3221225472
	DD	1073158879
	DD	3221225472
	DD	1073158856
	DD	2147483648
	DD	1073158833
	DD	2147483648
	DD	1073158810
	DD	1073741824
	DD	1073158787
	DD	1073741824
	DD	1073158764
	DD	0
	DD	1073158741
	DD	0
	DD	1073158718
	DD	3221225472
	DD	1073158694
	DD	3221225472
	DD	1073158671
	DD	2147483648
	DD	1073158648
	DD	2147483648
	DD	1073158625
	DD	1073741824
	DD	1073158602
	DD	1073741824
	DD	1073158579
	DD	0
	DD	1073158556
	DD	0
	DD	1073158533
	DD	3221225472
	DD	1073158509
	DD	3221225472
	DD	1073158486
	DD	2147483648
	DD	1073158463
	DD	2147483648
	DD	1073158440
	DD	1073741824
	DD	1073158417
	DD	1073741824
	DD	1073158394
	DD	1073741824
	DD	1073158371
	DD	0
	DD	1073158348
	DD	0
	DD	1073158325
	DD	3221225472
	DD	1073158301
	DD	3221225472
	DD	1073158278
	DD	2147483648
	DD	1073158255
	DD	2147483648
	DD	1073158232
	DD	2147483648
	DD	1073158209
	DD	1073741824
	DD	1073158186
	DD	1073741824
	DD	1073158163
	DD	0
	DD	1073158140
	DD	0
	DD	1073158117
	DD	3221225472
	DD	1073158093
	DD	3221225472
	DD	1073158070
	DD	3221225472
	DD	1073158047
	DD	2147483648
	DD	1073158024
	DD	2147483648
	DD	1073158001
	DD	1073741824
	DD	1073157978
	DD	1073741824
	DD	1073157955
	DD	1073741824
	DD	1073157932
	DD	0
	DD	1073157909
	DD	0
	DD	1073157886
	DD	3221225472
	DD	1073157862
	DD	3221225472
	DD	1073157839
	DD	3221225472
	DD	1073157816
	DD	2147483648
	DD	1073157793
	DD	2147483648
	DD	1073157770
	DD	2147483648
	DD	1073157747
	DD	1073741824
	DD	1073157724
	DD	1073741824
	DD	1073157701
	DD	0
	DD	1073157678
	DD	0
	DD	1073157655
	DD	0
	DD	1073157632
	DD	3221225472
	DD	1073157608
	DD	3221225472
	DD	1073157585
	DD	3221225472
	DD	1073157562
	DD	2147483648
	DD	1073157539
	DD	2147483648
	DD	1073157516
	DD	2147483648
	DD	1073157493
	DD	1073741824
	DD	1073157470
	DD	1073741824
	DD	1073157447
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1342177280
	DD	1057431575
	DD	1679773494
	DD	1024039205
	DD	989855744
	DD	1058476078
	DD	3244478756
	DD	1024589954
	DD	209715200
	DD	1059147828
	DD	152199156
	DD	1027874535
	DD	2449473536
	DD	1059526748
	DD	2343302255
	DD	1022283036
	DD	1560281088
	DD	1059903632
	DD	4038848719
	DD	1027337824
	DD	4282384384
	DD	1060196455
	DD	2325104861
	DD	1027595231
	DD	1665138688
	DD	1060384909
	DD	2934027888
	DD	1026982347
	DD	3263168512
	DD	1060574392
	DD	3208451390
	DD	1027670758
	DD	3980394496
	DD	1060763881
	DD	863587004
	DD	1026973426
	DD	2470445056
	DD	1060952352
	DD	1027097864
	DD	1028644619
	DD	1296039936
	DD	1061141853
	DD	2016162954
	DD	1025089894
	DD	3107979264
	DD	1061244623
	DD	970842239
	DD	1028172704
	DD	3722444800
	DD	1061339379
	DD	2640304163
	DD	1027825546
	DD	2959081472
	DD	1061433626
	DD	306547692
	DD	1028101690
	DD	2631925760
	DD	1061528388
	DD	747377661
	DD	1028120913
	DD	794820608
	DD	1061622641
	DD	3406550266
	DD	1028182206
	DD	3825205248
	DD	1061717408
	DD	3705775220
	DD	1027201825
	DD	916455424
	DD	1061811667
	DD	1432750358
	DD	1028165990
	DD	3011510272
	DD	1061906440
	DD	3361908688
	DD	1027438936
	DD	3330277376
	DD	1062000704
	DD	3560665332
	DD	1027805882
	DD	3082813440
	DD	1062094971
	DD	2539531329
	DD	1028011583
	DD	3747610624
	DD	1062189753
	DD	2232403651
	DD	1025658467
	DD	1218445312
	DD	1062245757
	DD	396499622
	DD	1025861782
	DD	1086324736
	DD	1062293151
	DD	2757240868
	DD	1026731615
	DD	2047868928
	DD	1062340290
	DD	2226191703
	DD	1027982328
	DD	580911104
	DD	1062387431
	DD	1252857417
	DD	1028280924
	DD	1887436800
	DD	1062434829
	DD	659583454
	DD	1025370904
	DD	4186963968
	DD	1062481972
	DD	3587661750
	DD	1028188900
	DD	738197504
	DD	1062529374
	DD	3240696709
	DD	1027025093
	DD	2511339520
	DD	1062576520
	DD	2884432087
	DD	1028614554
	DD	1859125248
	DD	1062623668
	DD	2402099113
	DD	1025699109
	DD	4148166656
	DD	1062671073
	DD	2335237504
	DD	1026835951
	DD	2970615808
	DD	1062718224
	DD	3698719430
	DD	1027808594
	DD	3662675968
	DD	1062765376
	DD	2704653673
	DD	1027603403
	DD	1929379840
	DD	1062812530
	DD	761521627
	DD	1027109120
	DD	3273654272
	DD	1062859941
	DD	470528098
	DD	1027977181
	DD	1019215872
	DD	1062907098
	DD	3704635566
	DD	1027707215
	DD	635437056
	DD	1062954256
	DD	3676592927
	DD	1027502983
	DD	2122317824
	DD	1063001415
	DD	1497197375
	DD	1028267547
	DD	2529165312
	DD	1063048832
	DD	3425827878
	DD	1022000476
	DD	3498049536
	DD	1063095994
	DD	1982476393
	DD	1026289596
	DD	2043674624
	DD	1063143158
	DD	2502680620
	DD	1028471295
	DD	2463105024
	DD	1063190323
	DD	991567028
	DD	1027421239
	DD	460324864
	DD	1063237490
	DD	1461814384
	DD	1026181618
	DD	920125440
	DD	1063270489
	DD	1613472693
	DD	1027845558
	DD	3956277248
	DD	1063294073
	DD	93449747
	DD	1028284502
	DD	1487405056
	DD	1063317659
	DD	1336931403
	DD	1026834156
	DD	2102919168
	DD	1063341245
	DD	319680825
	DD	1027392710
	DD	1508376576
	DD	1063364832
	DD	2474643583
	DD	1027776685
	DD	3999268864
	DD	1063388419
	DD	3104004650
	DD	1024627034
	DD	985137152
	DD	1063412008
	DD	550153379
	DD	1026678253
	DD	1056440320
	DD	1063435597
	DD	672168391
	DD	1027731310
	DD	4213702656
	DD	1063459186
	DD	1805142399
	DD	1026660459
	DD	2772434944
	DD	1063482905
	DD	2448602160
	DD	1028404887
	DD	3528458240
	DD	1063506496
	DD	3457943394
	DD	1027665063
	DD	3075473408
	DD	1063530088
	DD	121314862
	DD	1027996294
	DD	1414004736
	DD	1063553681
	DD	94774013
	DD	1028053481
	DD	2839019520
	DD	1063577274
	DD	1263902834
	DD	1028588748
	DD	3056074752
	DD	1063600868
	DD	369708558
	DD	1028257136
	DD	2065170432
	DD	1063624463
	DD	1634529849
	DD	1027810905
	DD	1769996288
	DD	3210227157
	DD	1054279927
	DD	3174741313
	DD	2442133504
	DD	3210203373
	DD	2067107398
	DD	3175167430
	DD	456130560
	DD	3210179845
	DD	4142755806
	DD	3170825152
	DD	2302672896
	DD	3210156060
	DD	1526169727
	DD	3175523413
	DD	1524629504
	DD	3210132531
	DD	2442955053
	DD	3175425591
	DD	251658240
	DD	3210108746
	DD	2154729168
	DD	3175535488
	DD	681574400
	DD	3210085216
	DD	4275862891
	DD	3176027230
	DD	584056832
	DD	3210061430
	DD	4255852476
	DD	3173565530
	DD	2221932544
	DD	3210037899
	DD	2498876736
	DD	3175149504
	DD	3297771520
	DD	3210014112
	DD	1851620949
	DD	3175688865
	DD	1849688064
	DD	3209990581
	DD	2923055509
	DD	3171310641
	DD	4099932160
	DD	3209966793
	DD	2427653201
	DD	3173037457
	DD	3858759680
	DD	3209943261
	DD	1550068012
	DD	3173027359
	DD	2987393024
	DD	3209919473
	DD	4127650534
	DD	3175851613
	DD	3954180096
	DD	3209895940
	DD	442055840
	DD	3174771669
	DD	4257218560
	DD	3209872151
	DD	4113960829
	DD	3175350854
	DD	2135949312
	DD	3209848618
	DD	2076166727
	DD	3175229825
	DD	3613392896
	DD	3209824828
	DD	3476091171
	DD	3171604778
	DD	2699034624
	DD	3209801294
	DD	1765290157
	DD	3173591669
	DD	1053818880
	DD	3209777504
	DD	3761837094
	DD	3175683182
	DD	1346371584
	DD	3209753969
	DD	1459626820
	DD	3176031561
	DD	875560960
	DD	3209730178
	DD	2402361097
	DD	3174909319
	DD	2375024640
	DD	3209706642
	DD	687754918
	DD	3174943382
	DD	1858076672
	DD	3209674565
	DD	252333183
	DD	3175531572
	DD	2975858688
	DD	3209627492
	DD	1334776821
	DD	3174591557
	DD	2430599168
	DD	3209579907
	DD	1326030186
	DD	3173486707
	DD	1665138688
	DD	3209532833
	DD	737674412
	DD	3174401557
	DD	2122317824
	DD	3209485758
	DD	3987168834
	DD	3175346908
	DD	815792128
	DD	3209438171
	DD	3526910672
	DD	3176068855
	DD	3686793216
	DD	3209391094
	DD	587265932
	DD	3174950865
	DD	429916160
	DD	3209343506
	DD	3143915816
	DD	3175955609
	DD	1417674752
	DD	3209296428
	DD	2918285701
	DD	3174860756
	DD	505413632
	DD	3209248838
	DD	436607152
	DD	3175743066
	DD	3904897024
	DD	3209201758
	DD	2867787430
	DD	3173594277
	DD	4229955584
	DD	3209154678
	DD	3971699810
	DD	3174682560
	DD	2556428288
	DD	3209107086
	DD	3215049067
	DD	3174495054
	DD	998244352
	DD	3209060005
	DD	2424883713
	DD	3173182748
	DD	1667235840
	DD	3209012411
	DD	762177973
	DD	3175232288
	DD	2518679552
	DD	3208965328
	DD	282609672
	DD	3175635057
	DD	1237319680
	DD	3208917733
	DD	1502777354
	DD	3174942228
	DD	203423744
	DD	3208870649
	DD	4128371954
	DD	3175884977
	DD	392167424
	DD	3208823564
	DD	306802084
	DD	3175724146
	DD	2642411520
	DD	3208775966
	DD	2960876517
	DD	3173143647
	DD	945815552
	DD	3208728880
	DD	1800251929
	DD	3170106484
	DD	1241513984
	DD	3208681281
	DD	2675524524
	DD	3173521837
	DD	3904897024
	DD	3208625826
	DD	83988225
	DD	3175795858
	DD	3477078016
	DD	3208531649
	DD	1575792028
	DD	3175657512
	DD	2537553920
	DD	3208436447
	DD	1662079495
	DD	3175916253
	DD	2634022912
	DD	3208342267
	DD	2818347875
	DD	3174383619
	DD	2080374784
	DD	3208247062
	DD	1081767985
	DD	3175779040
	DD	2696937472
	DD	3208152879
	DD	2443744157
	DD	3175275915
	DD	1459617792
	DD	3208058695
	DD	790904149
	DD	3174713637
	DD	3670016000
	DD	3207963485
	DD	581064731
	DD	3173466591
	DD	2952790016
	DD	3207869298
	DD	1008918738
	DD	3171724149
	DD	377487360
	DD	3207775110
	DD	1606538461
	DD	3175837201
	DD	1052770304
	DD	3207679896
	DD	2534546984
	DD	3175060122
	DD	2298478592
	DD	3207577425
	DD	2154814426
	DD	3172198942
	DD	117440512
	DD	3207386992
	DD	1374248651
	DD	3174502065
	DD	1342177280
	DD	3207198603
	DD	4280579335
	DD	3175188313
	DD	3154116608
	DD	3207010211
	DD	3334926656
	DD	3174829419
	DD	2189426688
	DD	3206819769
	DD	3100885346
	DD	3175936751
	DD	746586112
	DD	3206631372
	DD	315615614
	DD	3173018851
	DD	4043309056
	DD	3206340535
	DD	274116456
	DD	3175970612
	DD	268435456
	DD	3205959634
	DD	691182319
	DD	3173304996
	DD	603979776
	DD	3205582822
	DD	112661265
	DD	3170010307
	DD	4194304000
	DD	3204915176
	DD	3717748378
	DD	3174284044
	DD	2885681152
	DD	3203858420
	DD	192153543
	DD	3175961815
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	2851812149
	DD	1072698941
	DD	2595802551
	DD	1016815913
	DD	1048019041
	DD	1072704666
	DD	1398474845
	DD	3161559171
	DD	3899555717
	DD	1072710421
	DD	427280750
	DD	3163595548
	DD	3541402996
	DD	1072716208
	DD	2759177317
	DD	1015903202
	DD	702412510
	DD	1072722027
	DD	3803266086
	DD	3163328991
	DD	410360776
	DD	1072727877
	DD	1269990655
	DD	1013024446
	DD	3402036099
	DD	1072733758
	DD	405889333
	DD	1016154232
	DD	1828292879
	DD	1072739672
	DD	1255956746
	DD	1016636974
	DD	728909815
	DD	1072745618
	DD	383930225
	DD	1016078044
	DD	852742562
	DD	1072751596
	DD	667253586
	DD	1010842135
	DD	2952712987
	DD	1072757606
	DD	3293494651
	DD	3161168877
	DD	3490863953
	DD	1072763649
	DD	960797497
	DD	3163997456
	DD	3228316108
	DD	1072769725
	DD	3010241991
	DD	3159471380
	DD	2930322912
	DD	1072775834
	DD	2599499422
	DD	3163762623
	DD	3366293073
	DD	1072781976
	DD	3119426313
	DD	1015169130
	DD	1014845819
	DD	1072788152
	DD	3117910645
	DD	3162607681
	DD	948735466
	DD	1072794361
	DD	3516338027
	DD	3163623459
	DD	3949972341
	DD	1072800603
	DD	2068408548
	DD	1015962444
	DD	2214878420
	DD	1072806880
	DD	892270087
	DD	3164164998
	DD	828946858
	DD	1072813191
	DD	10642492
	DD	1016988014
	DD	586995997
	DD	1072819536
	DD	41662347
	DD	3163676568
	DD	2288159958
	DD	1072825915
	DD	2169144468
	DD	1015924597
	DD	2440944790
	DD	1072832329
	DD	2492769773
	DD	1015196030
	DD	1853186616
	DD	1072838778
	DD	3066496370
	DD	1016705150
	DD	1337108031
	DD	1072845262
	DD	3203724452
	DD	1015726421
	DD	1709341917
	DD	1072851781
	DD	2571168217
	DD	1015201075
	DD	3790955393
	DD	1072858335
	DD	2352942461
	DD	3164228666
	DD	4112506593
	DD	1072864925
	DD	2947355221
	DD	1015419624
	DD	3504003472
	DD	1072871551
	DD	3594001059
	DD	3158379228
	DD	2799960843
	DD	1072878213
	DD	1423655380
	DD	1016070727
	DD	2839424854
	DD	1072884911
	DD	1171596163
	DD	1014090255
	DD	171030293
	DD	1072891646
	DD	3526460132
	DD	1015477354
	DD	4232894513
	DD	1072898416
	DD	2383938684
	DD	1015717095
	DD	2992903935
	DD	1072905224
	DD	2218154405
	DD	1016276769
	DD	1603444721
	DD	1072912069
	DD	1548633640
	DD	3163249902
	DD	926591435
	DD	1072918951
	DD	3208833761
	DD	3163962090
	DD	1829099622
	DD	1072925870
	DD	1016661180
	DD	3164509581
	DD	887463927
	DD	1072932827
	DD	3596744162
	DD	3161842742
	DD	3272845541
	DD	1072939821
	DD	928852419
	DD	3164536824
	DD	1276261410
	DD	1072946854
	DD	300981947
	DD	1015732745
	DD	78413852
	DD	1072953925
	DD	4183226867
	DD	3164065827
	DD	569847338
	DD	1072961034
	DD	472945272
	DD	3160339305
	DD	3645941911
	DD	1072968181
	DD	3814685080
	DD	3162621917
	DD	1617004845
	DD	1072975368
	DD	82804943
	DD	1011391354
	DD	3978100823
	DD	1072982593
	DD	3513027190
	DD	1016894539
	DD	3049340112
	DD	1072989858
	DD	3062915824
	DD	1014219171
	DD	4040676318
	DD	1072997162
	DD	4090609238
	DD	1016712034
	DD	3577096743
	DD	1073004506
	DD	2951496418
	DD	1014842263
	DD	2583551245
	DD	1073011890
	DD	3161094195
	DD	1016655067
	DD	1990012071
	DD	1073019314
	DD	3529070563
	DD	3163861769
	DD	2731501122
	DD	1073026778
	DD	1774031854
	DD	3163518597
	DD	1453150082
	DD	1073034283
	DD	498154668
	DD	3162536638
	DD	3395129871
	DD	1073041828
	DD	4025345434
	DD	3163383964
	DD	917841882
	DD	1073049415
	DD	18715564
	DD	1016707884
	DD	3566716925
	DD	1073057042
	DD	1536826855
	DD	1015191009
	DD	3712504873
	DD	1073064711
	DD	88491948
	DD	1016476236
	DD	2321106615
	DD	1073072422
	DD	2171176610
	DD	1010584347
	DD	363667784
	DD	1073080175
	DD	813753949
	DD	1016833785
	DD	3111574537
	DD	1073087969
	DD	2606161479
	DD	3163808322
	DD	2956612997
	DD	1073095806
	DD	2118169750
	DD	3163784129
	DD	885834528
	DD	1073103686
	DD	1973258546
	DD	3163310140
	DD	2186617381
	DD	1073111608
	DD	2270764083
	DD	3164321289
	DD	3561793907
	DD	1073119573
	DD	1157054052
	DD	1012938926
	DD	1719614413
	DD	1073127582
	DD	330458197
	DD	3164331316
	DD	1963711167
	DD	1073135634
	DD	1744767756
	DD	3161622870
	DD	1013258799
	DD	1073143730
	DD	1748797610
	DD	3161177658
	DD	4182873220
	DD	1073151869
	DD	629542646
	DD	3163044879
	DD	3907805044
	DD	1073160053
	DD	2257091225
	DD	3162598983
	DD	1218806132
	DD	1073168282
	DD	1818613051
	DD	3163597017
	DD	1447192521
	DD	1073176555
	DD	1462857171
	DD	3163563097
	DD	1339972927
	DD	1073184873
	DD	167908908
	DD	1016620728
	DD	1944781191
	DD	1073193236
	DD	3993278767
	DD	3162772855
	DD	19972402
	DD	1073201645
	DD	3507899861
	DD	1017057868
	DD	919555682
	DD	1073210099
	DD	3121969534
	DD	1013996802
	DD	1413356050
	DD	1073218599
	DD	1651349290
	DD	3163716742
	DD	2571947539
	DD	1073227145
	DD	3558159063
	DD	3164425245
	DD	1176749997
	DD	1073235738
	DD	2738998779
	DD	3163084420
	DD	2604962541
	DD	1073244377
	DD	2614425274
	DD	3164587768
	DD	3649726105
	DD	1073253063
	DD	4085036346
	DD	1016698050
	DD	1110089947
	DD	1073261797
	DD	1451641638
	DD	1016523249
	DD	380978316
	DD	1073270578
	DD	854188970
	DD	3161511262
	DD	2568320822
	DD	1073279406
	DD	2732824428
	DD	1015401491
	DD	194117574
	DD	1073288283
	DD	777528611
	DD	3164460665
	DD	2966275557
	DD	1073297207
	DD	2176155323
	DD	3160891335
	DD	3418903055
	DD	1073306180
	DD	2527457337
	DD	3161869180
	DD	2682146384
	DD	1073315202
	DD	2082178512
	DD	3164411995
	DD	1892288442
	DD	1073324273
	DD	2446255666
	DD	3163648957
	DD	2191782032
	DD	1073333393
	DD	2960257726
	DD	1014791238
	DD	434316067
	DD	1073342563
	DD	2028358766
	DD	1014506698
	DD	2069751141
	DD	1073351782
	DD	1562170674
	DD	3163773257
	DD	3964284211
	DD	1073361051
	DD	2111583915
	DD	1016475740
	DD	2990417245
	DD	1073370371
	DD	3683467745
	DD	3164417902
	DD	321958744
	DD	1073379742
	DD	3401933766
	DD	1016843134
	DD	1434058175
	DD	1073389163
	DD	251133233
	DD	1016134345
	DD	3218338682
	DD	1073398635
	DD	3404164304
	DD	3163525684
	DD	2572866477
	DD	1073408159
	DD	878562433
	DD	1016570317
	DD	697153126
	DD	1073417735
	DD	1283515428
	DD	3164331765
	DD	3092190715
	DD	1073427362
	DD	814012167
	DD	3160571998
	DD	2380618042
	DD	1073437042
	DD	3149557219
	DD	3164369375
	DD	4076559943
	DD	1073446774
	DD	2119478330
	DD	3161806927
	DD	815859274
	DD	1073456560
	DD	240396590
	DD	3164536019
	DD	2420883922
	DD	1073466398
	DD	2049810052
	DD	1015168464
	DD	1540824585
	DD	1073476290
	DD	1064017010
	DD	3164536266
	DD	3716502172
	DD	1073486235
	DD	2303740125
	DD	1015091301
	DD	1610600570
	DD	1073496235
	DD	3766732298
	DD	1016808759
	DD	777507147
	DD	1073506289
	DD	4282924204
	DD	1016236109
	DD	2483480501
	DD	1073516397
	DD	1216371780
	DD	1014082748
	DD	3706687593
	DD	1073526560
	DD	3521726939
	DD	1014301643
	DD	1432208378
	DD	1073536779
	DD	1401068914
	DD	3163412539
	DD	1242007932
	DD	1073547053
	DD	1132034716
	DD	3164388407
	DD	135105010
	DD	1073557383
	DD	1906148727
	DD	3164424315
	DD	3707479175
	DD	1073567768
	DD	3613079302
	DD	1015213314
	DD	382305176
	DD	1073578211
	DD	2347622376
	DD	3163627201
	DD	64696965
	DD	1073588710
	DD	1768797490
	DD	1016865536
	DD	4076975200
	DD	1073599265
	DD	2029000898
	DD	1016257111
	DD	863738719
	DD	1073609879
	DD	1326992219
	DD	3163661773
	DD	351641897
	DD	1073620550
	DD	2172261526
	DD	3164059175
	DD	3884662774
	DD	1073631278
	DD	2158611599
	DD	1015258761
	DD	4224142467
	DD	1073642065
	DD	3389820385
	DD	1016255778
	DD	2728693978
	DD	1073652911
	DD	396109971
	DD	3164511267
	DD	764307441
	DD	1073663816
	DD	3021057420
	DD	3164378099
	DD	3999357479
	DD	1073674779
	DD	2258941616
	DD	1016973300
	DD	929806999
	DD	1073685803
	DD	3205336643
	DD	1016308133
	DD	1533953344
	DD	1073696886
	DD	769171850
	DD	1016714209
	DD	2912730644
	DD	1073708029
	DD	3490067721
	DD	3164453650
	DD	2174652632
	DD	1073719233
	DD	4087714590
	DD	1015498835
	DD	730821105
	DD	1073730498
	DD	2523232743
	DD	1013115764
	DD	2523158504
	DD	1048167334
	DD	1181303047
	DD	3218484803
	DD	1656151777
	DD	1069842388
	DD	714085080
	DD	3216330823
	DD	4277811695
	DD	1072049730
	DD	4286760335
	DD	1070514109
	DD	3607404736
	DD	1068264200
	DD	1874480759
	DD	1065595563
	DD	3884607281
	DD	1062590591
	DD	0
	DD	2145386496
	DD	0
	DD	1048576
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	3220176896
	DD	0
	DD	1120403456
	DD	1073741824
	DD	1073157447
	DD	33554432
	DD	1101004800
	DD	0
	DD	1282408448
	DD	0
	DD	862978048
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
