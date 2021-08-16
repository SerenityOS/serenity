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
	PUBLIC __svml_tanhf4_ha_l9

__svml_tanhf4_ha_l9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 312
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [192+rsp], xmm15
        vmovdqa   xmm15, xmm0
        vmovups   XMMWORD PTR [208+rsp], xmm14
        vpxor     xmm3, xmm3, xmm3
        vmovups   XMMWORD PTR [224+rsp], xmm13
        vmovups   XMMWORD PTR [240+rsp], xmm12
        vmovups   XMMWORD PTR [256+rsp], xmm11
        vmovups   XMMWORD PTR [272+rsp], xmm10
        mov       QWORD PTR [288+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vpand     xmm4, xmm15, XMMWORD PTR [__svml_stanh_ha_data_internal+3200]
        and       r13, -64
        vpsubd    xmm2, xmm4, XMMWORD PTR [__svml_stanh_ha_data_internal+3328]
        vmovups   xmm11, XMMWORD PTR [__svml_stanh_ha_data_internal+3392]
        vpcmpgtd  xmm1, xmm2, xmm3
        vpcmpgtd  xmm5, xmm4, XMMWORD PTR [__svml_stanh_ha_data_internal+3264]
        vpand     xmm12, xmm2, xmm1
        vpcmpgtd  xmm10, xmm2, xmm11
        vmovmskps r8d, xmm5
        vandps    xmm13, xmm15, XMMWORD PTR [__svml_stanh_ha_data_internal+3136]
        vblendvps xmm5, xmm12, xmm11, xmm10
        vandps    xmm14, xmm15, XMMWORD PTR [__svml_stanh_ha_data_internal+3072]
        vpsrld    xmm4, xmm5, 15
        test      r8d, r8d
        vmovd     ecx, xmm4
        vcvtps2pd ymm13, xmm13
        vpextrd   eax, xmm4, 1
        movsxd    rcx, ecx
        movsxd    rax, eax
        vpextrd   r9d, xmm4, 2
        vpextrd   r10d, xmm4, 3
        movsxd    r9, r9d
        movsxd    r10, r10d
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+rdx+rcx]
        vmovupd   xmm0, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+rdx+rax]
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+rdx+rax]
        vmovupd   xmm10, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+rdx+rcx]
        mov       QWORD PTR [296+rsp], r13
        vinsertf128 ymm1, ymm3, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+rdx+r9], 1
        vinsertf128 ymm2, ymm0, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+rdx+r10], 1
        vunpcklpd ymm12, ymm1, ymm2
        vunpckhpd ymm11, ymm1, ymm2
        vmovupd   xmm0, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+rdx+rcx]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+rdx+rax]
        vinsertf128 ymm4, ymm10, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+rdx+r9], 1
        vinsertf128 ymm3, ymm5, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+rdx+r10], 1
        vunpcklpd ymm10, ymm4, ymm3
        vunpckhpd ymm5, ymm4, ymm3
        vinsertf128 ymm2, ymm0, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+rdx+r9], 1
        vinsertf128 ymm3, ymm1, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+rdx+r10], 1
        vunpcklpd ymm4, ymm2, ymm3
        vunpckhpd ymm3, ymm2, ymm3
        vmovupd   xmm0, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+rdx+rcx]
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+rdx+rax]
        vinsertf128 ymm1, ymm0, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+rdx+r9], 1
        vinsertf128 ymm2, ymm2, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+rdx+r10], 1
        vunpcklpd ymm0, ymm1, ymm2
        vunpckhpd ymm1, ymm1, ymm2
        vfmadd213pd ymm1, ymm13, ymm0
        vfmadd213pd ymm1, ymm13, ymm3
        vfmadd213pd ymm1, ymm13, ymm4
        vfmadd213pd ymm1, ymm13, ymm5
        vfmadd213pd ymm1, ymm13, ymm10
        vfmadd213pd ymm1, ymm13, ymm11
        vfmadd213pd ymm1, ymm13, ymm12
        vcvtpd2ps xmm0, ymm1
        vorps     xmm0, xmm0, xmm14
        jne       _B1_3

_B1_2::

        vzeroupper
        vmovups   xmm10, XMMWORD PTR [272+rsp]
        vmovups   xmm11, XMMWORD PTR [256+rsp]
        vmovups   xmm12, XMMWORD PTR [240+rsp]
        vmovups   xmm13, XMMWORD PTR [224+rsp]
        vmovups   xmm14, XMMWORD PTR [208+rsp]
        vmovups   xmm15, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [288+rsp]
        add       rsp, 312
        ret

_B1_3::

        vmovups   XMMWORD PTR [r13], xmm15
        vmovups   XMMWORD PTR [64+r13], xmm0

_B1_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, r8d

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

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_stanh_ha_cout_rare_internal
        jmp       _B1_8
        ALIGN     16

_B1_11::

__svml_tanhf4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanhf4_ha_l9_B1_B3:
	DD	1071105
	DD	2413656
	DD	1157200
	DD	1095751
	DD	1034302
	DD	972853
	DD	911400
	DD	849947
	DD	2556171

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_tanhf4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanhf4_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_tanhf4_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_6
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_tanhf4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanhf4_ha_e9

__svml_tanhf4_ha_e9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L20::

        sub       rsp, 312
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [256+rsp], xmm14
        vpxor     xmm4, xmm4, xmm4
        vmovups   XMMWORD PTR [192+rsp], xmm13
        vmovups   XMMWORD PTR [208+rsp], xmm12
        vmovups   XMMWORD PTR [224+rsp], xmm11
        vmovups   XMMWORD PTR [240+rsp], xmm10
        vmovups   XMMWORD PTR [272+rsp], xmm9
        mov       QWORD PTR [288+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vpand     xmm3, xmm0, XMMWORD PTR [__svml_stanh_ha_data_internal+3200]
        and       r13, -64
        vpcmpgtd  xmm2, xmm3, XMMWORD PTR [__svml_stanh_ha_data_internal+3264]
        vmovmskps r8d, xmm2
        vpsubd    xmm2, xmm3, XMMWORD PTR [__svml_stanh_ha_data_internal+3328]
        vmovups   xmm9, XMMWORD PTR [__svml_stanh_ha_data_internal+3392]
        vpcmpgtd  xmm5, xmm2, xmm4
        vpand     xmm3, xmm2, xmm5
        vpcmpgtd  xmm4, xmm2, xmm9
        vblendvps xmm5, xmm3, xmm9, xmm4
        vandps    xmm11, xmm0, XMMWORD PTR [__svml_stanh_ha_data_internal+3136]
        vpsrld    xmm10, xmm5, 15
        vmovd     ecx, xmm10
        vandps    xmm1, xmm0, XMMWORD PTR [__svml_stanh_ha_data_internal+3072]
        vcvtps2pd ymm11, xmm11
        vpextrd   eax, xmm10, 1
        movsxd    rcx, ecx
        movsxd    rax, eax
        vpextrd   r9d, xmm10, 2
        vpextrd   r10d, xmm10, 3
        movsxd    r9, r9d
        movsxd    r10, r10d
        vmovupd   xmm12, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+rdx+rcx]
        vmovupd   xmm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+rdx+rax]
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+rdx+rax]
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+rdx+rcx]
        mov       QWORD PTR [296+rsp], r13
        vinsertf128 ymm14, ymm12, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+rdx+r9], 1
        vinsertf128 ymm3, ymm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+rdx+r10], 1
        vmovupd   xmm12, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+rdx+rcx]
        vmovupd   xmm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+rdx+rax]
        vunpcklpd ymm2, ymm14, ymm3
        vunpckhpd ymm3, ymm14, ymm3
        vinsertf128 ymm9, ymm4, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+rdx+r9], 1
        vinsertf128 ymm10, ymm5, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+rdx+r10], 1
        vunpcklpd ymm4, ymm9, ymm10
        vunpckhpd ymm5, ymm9, ymm10
        vinsertf128 ymm14, ymm12, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+rdx+r9], 1
        vinsertf128 ymm10, ymm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+rdx+r10], 1
        vunpcklpd ymm9, ymm14, ymm10
        vunpckhpd ymm10, ymm14, ymm10
        vmovupd   xmm12, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+rdx+rcx]
        vmovupd   xmm14, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+rdx+rax]
        vinsertf128 ymm13, ymm12, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+rdx+r9], 1
        vinsertf128 ymm14, ymm14, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+rdx+r10], 1
        vunpcklpd ymm12, ymm13, ymm14
        vunpckhpd ymm13, ymm13, ymm14
        vmulpd    ymm13, ymm11, ymm13
        vaddpd    ymm12, ymm13, ymm12
        vmulpd    ymm14, ymm11, ymm12
        vaddpd    ymm10, ymm14, ymm10
        vmulpd    ymm10, ymm11, ymm10
        vaddpd    ymm9, ymm10, ymm9
        vmulpd    ymm9, ymm11, ymm9
        vaddpd    ymm5, ymm9, ymm5
        vmulpd    ymm5, ymm11, ymm5
        vaddpd    ymm4, ymm5, ymm4
        vmulpd    ymm4, ymm11, ymm4
        vaddpd    ymm3, ymm4, ymm3
        vmulpd    ymm11, ymm11, ymm3
        vaddpd    ymm2, ymm11, ymm2
        vcvtpd2ps xmm3, ymm2
        vorps     xmm1, xmm3, xmm1
        test      r8d, r8d
        jne       _B2_3

_B2_2::

        vmovaps   xmm0, xmm1
        mov       r13, QWORD PTR [288+rsp]
        vzeroupper
        vmovups   xmm9, XMMWORD PTR [272+rsp]
        vmovups   xmm10, XMMWORD PTR [240+rsp]
        vmovups   xmm11, XMMWORD PTR [224+rsp]
        vmovups   xmm12, XMMWORD PTR [208+rsp]
        vmovups   xmm13, XMMWORD PTR [192+rsp]
        vmovups   xmm14, XMMWORD PTR [256+rsp]
        add       rsp, 312
        ret

_B2_3::

        vmovups   XMMWORD PTR [r13], xmm0
        vmovups   XMMWORD PTR [64+r13], xmm1

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
        vmovups   xmm1, XMMWORD PTR [64+r13]
        jmp       _B2_2

_B2_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_stanh_ha_cout_rare_internal
        jmp       _B2_8
        ALIGN     16

_B2_11::

__svml_tanhf4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanhf4_ha_e9_B1_B3:
	DD	1070081
	DD	2413652
	DD	1153100
	DD	1026115
	DD	964666
	DD	903217
	DD	841768
	DD	1107995
	DD	2556171

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_tanhf4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanhf4_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_tanhf4_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_6
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_tanhf4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanhf16_ha_z0

__svml_tanhf16_ha_z0	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L39::

        sub       rsp, 1336
        mov       QWORD PTR [1320+rsp], r13
        lea       r13, QWORD PTR [1183+rsp]
        vmovups   zmm24, ZMMWORD PTR [__svml_stanh_ha_data_internal]
        and       r13, -64
        vmovups   zmm25, ZMMWORD PTR [__svml_stanh_ha_data_internal+896]
        vmovups   zmm30, ZMMWORD PTR [__svml_stanh_ha_data_internal+1024]
        vmovups   zmm26, ZMMWORD PTR [__svml_stanh_ha_data_internal+768]
        vmovups   zmm27, ZMMWORD PTR [__svml_stanh_ha_data_internal+640]
        vmovups   zmm28, ZMMWORD PTR [__svml_stanh_ha_data_internal+512]
        vmovups   zmm31, ZMMWORD PTR [__svml_stanh_ha_data_internal+256]
        vandps    zmm23, zmm0, ZMMWORD PTR [__svml_stanh_ha_data_internal+3136]
        vpternlogd zmm1, zmm1, zmm1, 255
        mov       QWORD PTR [1328+rsp], r13
        vpandd    zmm2, zmm0, ZMMWORD PTR [__svml_stanh_ha_data_internal+1152]
        vpsubd    zmm3, zmm2, ZMMWORD PTR [__svml_stanh_ha_data_internal+1216]
        vpcmpd    k1, zmm2, ZMMWORD PTR [__svml_stanh_ha_data_internal+3264], 2
        vpxord    zmm4, zmm4, zmm4
        vpmaxsd   zmm5, zmm3, zmm4
        vpminsd   zmm22, zmm5, ZMMWORD PTR [__svml_stanh_ha_data_internal+1280]
        vpsrld    zmm29, zmm22, 21
        vmovups   zmm3, ZMMWORD PTR [__svml_stanh_ha_data_internal+128]
        vpermt2ps zmm24, zmm29, ZMMWORD PTR [__svml_stanh_ha_data_internal+64]
        vpermt2ps zmm25, zmm29, ZMMWORD PTR [__svml_stanh_ha_data_internal+960]
        vpermt2ps zmm30, zmm29, ZMMWORD PTR [__svml_stanh_ha_data_internal+1088]
        vpermt2ps zmm26, zmm29, ZMMWORD PTR [__svml_stanh_ha_data_internal+832]
        vpermt2ps zmm27, zmm29, ZMMWORD PTR [__svml_stanh_ha_data_internal+704]
        vpermt2ps zmm28, zmm29, ZMMWORD PTR [__svml_stanh_ha_data_internal+576]
        vpermt2ps zmm31, zmm29, ZMMWORD PTR [__svml_stanh_ha_data_internal+320]
        vpermt2ps zmm3, zmm29, ZMMWORD PTR [__svml_stanh_ha_data_internal+192]
        vpandnd   zmm1{k1}, zmm2, zmm2
        vsubps    zmm2, zmm23, zmm24 {rn-sae}
        vptestmd  k0, zmm1, zmm1
        vmovups   zmm1, ZMMWORD PTR [__svml_stanh_ha_data_internal+384]
        vfmadd213ps zmm30, zmm2, zmm25 {rn-sae}
        kmovw     r8d, k0
        vfmadd213ps zmm30, zmm2, zmm26 {rn-sae}
        vpermt2ps zmm1, zmm29, ZMMWORD PTR [__svml_stanh_ha_data_internal+448]
        vfmadd213ps zmm30, zmm2, zmm27 {rn-sae}
        vfmadd213ps zmm30, zmm2, zmm28 {rn-sae}
        vmulps    zmm25, zmm30, zmm2 {rn-sae}
        vfmadd213ps zmm25, zmm2, zmm31 {rn-sae}
        vfmadd213ps zmm2, zmm1, zmm25 {rn-sae}
        vaddps    zmm1, zmm2, zmm3 {rn-sae}
        vpternlogd zmm1, zmm0, ZMMWORD PTR [__svml_stanh_ha_data_internal+3072], 248
        test      r8d, r8d
        jne       _B3_3

_B3_2::

        vmovaps   zmm0, zmm1
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

        vmovups   ZMMWORD PTR [r13], zmm0
        vmovups   ZMMWORD PTR [64+r13], zmm1
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
        vmovups   zmm1, ZMMWORD PTR [64+r13]
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

        call      __svml_stanh_ha_cout_rare_internal
        jmp       _B3_13
        ALIGN     16

_B3_16::

__svml_tanhf16_ha_z0 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanhf16_ha_z0_B1_B9:
	DD	267009
	DD	10867731
	DD	10944779

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_11
	DD	imagerel _unwind___svml_tanhf16_ha_z0_B1_B9

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanhf16_ha_z0_B11_B15:
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
	DD	imagerel _unwind___svml_tanhf16_ha_z0_B11_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanhf8_ha_l9

__svml_tanhf8_ha_l9	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L90::

        push      rbx
        push      rsi
        push      rdi
        sub       rsp, 560
        lea       r9, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [224+rsp], ymm15
        vmovups   YMMWORD PTR [256+rsp], ymm14
        vmovups   YMMWORD PTR [288+rsp], ymm13
        vmovups   YMMWORD PTR [320+rsp], ymm12
        vmovups   YMMWORD PTR [352+rsp], ymm11
        vmovups   YMMWORD PTR [384+rsp], ymm10
        vmovups   YMMWORD PTR [416+rsp], ymm9
        vmovups   YMMWORD PTR [448+rsp], ymm8
        vmovups   YMMWORD PTR [480+rsp], ymm7
        vmovups   YMMWORD PTR [512+rsp], ymm6
        vpxor     ymm10, ymm10, ymm10
        mov       QWORD PTR [544+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        and       r13, -64
        vmovups   YMMWORD PTR [32+r13], ymm0
        vmovups   ymm9, YMMWORD PTR [__svml_stanh_ha_data_internal+3392]
        vandps    ymm12, ymm0, YMMWORD PTR [__svml_stanh_ha_data_internal+3136]
        vandps    ymm5, ymm0, YMMWORD PTR [__svml_stanh_ha_data_internal+3072]
        vpand     ymm0, ymm0, YMMWORD PTR [__svml_stanh_ha_data_internal+3200]
        vmovups   YMMWORD PTR [r13], ymm5
        vpsubd    ymm8, ymm0, YMMWORD PTR [__svml_stanh_ha_data_internal+3328]
        vpcmpgtd  ymm11, ymm8, ymm10
        vpand     ymm6, ymm8, ymm11
        vpcmpgtd  ymm7, ymm8, ymm9
        vblendvps ymm2, ymm6, ymm9, ymm7
        vpsrld    ymm4, ymm2, 15
        vpcmpgtd  ymm3, ymm0, YMMWORD PTR [__svml_stanh_ha_data_internal+3264]
        vmovmskps ebx, ymm3
        mov       QWORD PTR [552+rsp], r13
        test      ebx, ebx
        vextracti128 xmm5, ymm4, 1
        vmovd     r10d, xmm4
        vmovd     ecx, xmm5
        vpextrd   r8d, xmm4, 1
        movsxd    r10, r10d
        movsxd    r8, r8d
        vpextrd   edi, xmm4, 2
        vpextrd   esi, xmm4, 3
        vpextrd   edx, xmm5, 1
        movsxd    rdi, edi
        movsxd    rsi, esi
        movsxd    rcx, ecx
        movsxd    rdx, edx
        vpextrd   eax, xmm5, 2
        vpextrd   r11d, xmm5, 3
        movsxd    rax, eax
        movsxd    r11, r11d
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+r10]
        vmovupd   xmm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+r8]
        vmovupd   xmm14, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+rcx]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+rdx]
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+rdx]
        vmovupd   xmm9, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+r10]
        vmovupd   xmm7, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+r8]
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+rcx]
        vinsertf128 ymm0, ymm3, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+rdi], 1
        vinsertf128 ymm10, ymm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+rsi], 1
        vunpcklpd ymm15, ymm0, ymm10
        vmovupd   YMMWORD PTR [64+r13], ymm15
        vmovupd   xmm15, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+r8]
        vunpckhpd ymm10, ymm0, ymm10
        vmovupd   xmm0, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+rcx]
        vinsertf128 ymm8, ymm14, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+rax], 1
        vinsertf128 ymm6, ymm1, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+r11], 1
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+r10]
        vunpcklpd ymm11, ymm8, ymm6
        vmovupd   YMMWORD PTR [96+r13], ymm11
        vunpckhpd ymm11, ymm8, ymm6
        vinsertf128 ymm14, ymm4, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+r11], 1
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+rdx]
        vinsertf128 ymm5, ymm9, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+rdi], 1
        vinsertf128 ymm3, ymm7, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+rsi], 1
        vinsertf128 ymm13, ymm2, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+rax], 1
        vunpcklpd ymm8, ymm5, ymm3
        vunpckhpd ymm6, ymm5, ymm3
        vunpcklpd ymm9, ymm13, ymm14
        vunpckhpd ymm7, ymm13, ymm14
        vinsertf128 ymm2, ymm1, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+rdi], 1
        vinsertf128 ymm5, ymm15, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+rsi], 1
        vinsertf128 ymm3, ymm0, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+rax], 1
        vinsertf128 ymm13, ymm4, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+r11], 1
        vmovupd   xmm0, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+r8]
        vunpcklpd ymm4, ymm2, ymm5
        vunpckhpd ymm2, ymm2, ymm5
        vunpcklpd ymm5, ymm3, ymm13
        vunpckhpd ymm3, ymm3, ymm13
        vmovupd   xmm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+r10]
        vmovupd   xmm15, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+rcx]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+rdx]
        vinsertf128 ymm14, ymm0, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+rsi], 1
        vinsertf128 ymm13, ymm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+rdi], 1
        vinsertf128 ymm15, ymm15, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+rax], 1
        vinsertf128 ymm0, ymm1, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+r11], 1
        vunpcklpd ymm1, ymm13, ymm14
        vunpckhpd ymm14, ymm13, ymm14
        vunpcklpd ymm13, ymm15, ymm0
        vunpckhpd ymm0, ymm15, ymm0
        vcvtps2pd ymm15, xmm12
        vextractf128 xmm12, ymm12, 1
        vfmadd213pd ymm14, ymm15, ymm1
        vcvtps2pd ymm12, xmm12
        vfmadd213pd ymm14, ymm15, ymm2
        vfmadd213pd ymm0, ymm12, ymm13
        vfmadd213pd ymm14, ymm15, ymm4
        vfmadd213pd ymm0, ymm12, ymm3
        vfmadd213pd ymm14, ymm15, ymm6
        vfmadd213pd ymm0, ymm12, ymm5
        vfmadd213pd ymm14, ymm15, ymm8
        vfmadd213pd ymm0, ymm12, ymm7
        vfmadd213pd ymm14, ymm15, ymm10
        vfmadd213pd ymm0, ymm12, ymm9
        vfmadd213pd ymm14, ymm15, YMMWORD PTR [64+r13]
        vfmadd213pd ymm0, ymm12, ymm11
        vcvtpd2ps xmm1, ymm14
        vfmadd213pd ymm0, ymm12, YMMWORD PTR [96+r13]
        vcvtpd2ps xmm0, ymm0
        vinsertf128 ymm2, ymm1, xmm0, 1
        vorps     ymm0, ymm2, YMMWORD PTR [r13]
        jne       _B4_3

_B4_2::

        vmovups   ymm6, YMMWORD PTR [512+rsp]
        vmovups   ymm7, YMMWORD PTR [480+rsp]
        vmovups   ymm8, YMMWORD PTR [448+rsp]
        vmovups   ymm9, YMMWORD PTR [416+rsp]
        vmovups   ymm10, YMMWORD PTR [384+rsp]
        vmovups   ymm11, YMMWORD PTR [352+rsp]
        vmovups   ymm12, YMMWORD PTR [320+rsp]
        vmovups   ymm13, YMMWORD PTR [288+rsp]
        vmovups   ymm14, YMMWORD PTR [256+rsp]
        vmovups   ymm15, YMMWORD PTR [224+rsp]
        mov       r13, QWORD PTR [544+rsp]
        add       rsp, 560
        pop       rdi
        pop       rsi
        pop       rbx
        ret

_B4_3::

        vmovups   ymm1, YMMWORD PTR [32+r13]
        vmovups   YMMWORD PTR [64+r13], ymm0
        vmovups   YMMWORD PTR [r13], ymm1

_B4_6::

        xor       esi, esi

_B4_7::

        bt        ebx, esi
        jc        _B4_10

_B4_8::

        inc       esi
        cmp       esi, 8
        jl        _B4_7

_B4_9::

        vmovups   ymm0, YMMWORD PTR [64+r13]
        jmp       _B4_2

_B4_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rsi*4]
        lea       rdx, QWORD PTR [64+r13+rsi*4]

        call      __svml_stanh_ha_cout_rare_internal
        jmp       _B4_8
        ALIGN     16

_B4_11::

__svml_tanhf8_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanhf8_ha_l9_B1_B10:
	DD	1801217
	DD	4510844
	DD	2123887
	DD	1996902
	DD	1869917
	DD	1742932
	DD	1615947
	DD	1488962
	DD	1361977
	DD	1234992
	DD	1108007
	DD	981022
	DD	4587790
	DD	1611034631
	DD	12293

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_11
	DD	imagerel _unwind___svml_tanhf8_ha_l9_B1_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanhf8_ha_e9

__svml_tanhf8_ha_e9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L121::

        push      rbx
        push      rsi
        push      rdi
        sub       rsp, 560
        lea       r9, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [224+rsp], ymm15
        vmovups   YMMWORD PTR [384+rsp], ymm14
        vmovups   YMMWORD PTR [512+rsp], ymm13
        vmovups   YMMWORD PTR [256+rsp], ymm12
        vmovups   YMMWORD PTR [288+rsp], ymm11
        vpxor     xmm12, xmm12, xmm12
        vmovups   YMMWORD PTR [320+rsp], ymm10
        vmovups   YMMWORD PTR [352+rsp], ymm9
        vmovups   YMMWORD PTR [416+rsp], ymm8
        vmovups   YMMWORD PTR [448+rsp], ymm7
        vmovups   YMMWORD PTR [480+rsp], ymm6
        mov       QWORD PTR [544+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        and       r13, -64
        vmovups   YMMWORD PTR [32+r13], ymm0
        vmovups   xmm3, XMMWORD PTR [__svml_stanh_ha_data_internal+3200]
        vmovups   xmm14, XMMWORD PTR [__svml_stanh_ha_data_internal+3264]
        vmovups   xmm11, XMMWORD PTR [__svml_stanh_ha_data_internal+3328]
        vmovups   xmm10, XMMWORD PTR [__svml_stanh_ha_data_internal+3392]
        vandps    ymm15, ymm0, YMMWORD PTR [__svml_stanh_ha_data_internal+3136]
        vandps    ymm5, ymm0, YMMWORD PTR [__svml_stanh_ha_data_internal+3072]
        vmovups   YMMWORD PTR [r13], ymm5
        mov       QWORD PTR [552+rsp], r13
        vpand     xmm5, xmm0, xmm3
        vextractf128 xmm4, ymm0, 1
        vpcmpgtd  xmm9, xmm5, xmm14
        vpand     xmm4, xmm4, xmm3
        vpsubd    xmm3, xmm5, xmm11
        vpcmpgtd  xmm8, xmm4, xmm14
        vpsubd    xmm13, xmm4, xmm11
        vpackssdw xmm7, xmm9, xmm8
        vpcmpgtd  xmm11, xmm3, xmm12
        vpacksswb xmm6, xmm7, xmm12
        vpcmpgtd  xmm12, xmm13, xmm12
        vpand     xmm1, xmm3, xmm11
        vpcmpgtd  xmm2, xmm3, xmm10
        vpand     xmm0, xmm13, xmm12
        vpcmpgtd  xmm14, xmm13, xmm10
        vblendvps xmm11, xmm1, xmm10, xmm2
        vblendvps xmm10, xmm0, xmm10, xmm14
        vpsrld    xmm14, xmm11, 15
        vpsrld    xmm7, xmm10, 15
        vmovd     r10d, xmm14
        vmovd     ecx, xmm7
        vpmovmskb ebx, xmm6
        vpextrd   r8d, xmm14, 1
        vpextrd   edx, xmm7, 1
        movsxd    r10, r10d
        movsxd    r8, r8d
        movsxd    rcx, ecx
        movsxd    rdx, edx
        vpextrd   edi, xmm14, 2
        vpextrd   esi, xmm14, 3
        vpextrd   eax, xmm7, 2
        vpextrd   r11d, xmm7, 3
        movsxd    rdi, edi
        movsxd    rsi, esi
        movsxd    rax, eax
        movsxd    r11, r11d
        vmovupd   xmm12, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+r10]
        vmovupd   xmm11, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+r8]
        vmovupd   xmm6, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+rcx]
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+rdx]
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+r10]
        vmovupd   xmm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+r8]
        vinsertf128 ymm9, ymm12, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+rdi], 1
        vinsertf128 ymm8, ymm11, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+rsi], 1
        vinsertf128 ymm3, ymm6, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+rax], 1
        vinsertf128 ymm1, ymm5, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+r9+r11], 1
        vunpcklpd ymm10, ymm9, ymm8
        vunpcklpd ymm4, ymm3, ymm1
        vmovupd   YMMWORD PTR [64+r13], ymm10
        vmovupd   YMMWORD PTR [96+r13], ymm4
        vmovupd   xmm10, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+rcx]
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+r10]
        vunpckhpd ymm11, ymm9, ymm8
        vunpckhpd ymm12, ymm3, ymm1
        vmovupd   xmm8, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+rdx]
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+r8]
        vinsertf128 ymm0, ymm2, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+rdi], 1
        vinsertf128 ymm14, ymm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+rsi], 1
        vunpcklpd ymm9, ymm0, ymm14
        vunpckhpd ymm7, ymm0, ymm14
        vmovupd   xmm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+rcx]
        vmovupd   xmm0, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+rdx]
        vinsertf128 ymm6, ymm10, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+rax], 1
        vinsertf128 ymm5, ymm8, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+r9+r11], 1
        vinsertf128 ymm1, ymm4, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+rdi], 1
        vinsertf128 ymm2, ymm3, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+rsi], 1
        vunpcklpd ymm10, ymm6, ymm5
        vunpckhpd ymm8, ymm6, ymm5
        vunpcklpd ymm5, ymm1, ymm2
        vunpckhpd ymm3, ymm1, ymm2
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+r10]
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+r8]
        vinsertf128 ymm14, ymm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+rax], 1
        vinsertf128 ymm4, ymm0, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+r9+r11], 1
        vunpcklpd ymm6, ymm14, ymm4
        vunpckhpd ymm4, ymm14, ymm4
        vinsertf128 ymm14, ymm1, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+rdi], 1
        vinsertf128 ymm13, ymm2, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+rsi], 1
        vunpcklpd ymm1, ymm14, ymm13
        vunpckhpd ymm0, ymm14, ymm13
        vmovupd   xmm14, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+rcx]
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+rdx]
        vinsertf128 ymm14, ymm14, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+rax], 1
        vinsertf128 ymm13, ymm2, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+r9+r11], 1
        vunpcklpd ymm2, ymm14, ymm13
        vunpckhpd ymm13, ymm14, ymm13
        vcvtps2pd ymm14, xmm15
        vextractf128 xmm15, ymm15, 1
        vmulpd    ymm0, ymm14, ymm0
        vcvtps2pd ymm15, xmm15
        vaddpd    ymm1, ymm0, ymm1
        vmulpd    ymm0, ymm15, ymm13
        vmulpd    ymm1, ymm14, ymm1
        vaddpd    ymm2, ymm0, ymm2
        vaddpd    ymm0, ymm1, ymm3
        vmulpd    ymm3, ymm15, ymm2
        vmulpd    ymm1, ymm14, ymm0
        vaddpd    ymm2, ymm3, ymm4
        vaddpd    ymm0, ymm1, ymm5
        vmulpd    ymm5, ymm15, ymm2
        vaddpd    ymm1, ymm5, ymm6
        vmulpd    ymm6, ymm14, ymm0
        vaddpd    ymm0, ymm6, ymm7
        vmulpd    ymm7, ymm15, ymm1
        vmulpd    ymm1, ymm14, ymm0
        vaddpd    ymm2, ymm7, ymm8
        vaddpd    ymm0, ymm1, ymm9
        vmulpd    ymm9, ymm15, ymm2
        vmulpd    ymm1, ymm14, ymm0
        vaddpd    ymm2, ymm9, ymm10
        vaddpd    ymm0, ymm1, ymm11
        vmulpd    ymm11, ymm15, ymm2
        vaddpd    ymm1, ymm11, ymm12
        vmulpd    ymm12, ymm14, ymm0
        vmulpd    ymm4, ymm15, ymm1
        vaddpd    ymm0, ymm12, YMMWORD PTR [64+r13]
        vaddpd    ymm10, ymm4, YMMWORD PTR [96+r13]
        vcvtpd2ps xmm8, ymm0
        vcvtpd2ps xmm13, ymm10
        vinsertf128 ymm14, ymm8, xmm13, 1
        vorps     ymm0, ymm14, YMMWORD PTR [r13]
        test      bl, bl
        jne       _B5_3

_B5_2::

        vmovups   ymm6, YMMWORD PTR [480+rsp]
        vmovups   ymm7, YMMWORD PTR [448+rsp]
        vmovups   ymm8, YMMWORD PTR [416+rsp]
        vmovups   ymm9, YMMWORD PTR [352+rsp]
        vmovups   ymm10, YMMWORD PTR [320+rsp]
        vmovups   ymm11, YMMWORD PTR [288+rsp]
        vmovups   ymm12, YMMWORD PTR [256+rsp]
        vmovups   ymm13, YMMWORD PTR [512+rsp]
        vmovups   ymm14, YMMWORD PTR [384+rsp]
        vmovups   ymm15, YMMWORD PTR [224+rsp]
        mov       r13, QWORD PTR [544+rsp]
        add       rsp, 560
        pop       rdi
        pop       rsi
        pop       rbx
        ret

_B5_3::

        vmovups   ymm1, YMMWORD PTR [32+r13]
        vmovups   YMMWORD PTR [64+r13], ymm0
        vmovups   YMMWORD PTR [r13], ymm1
        test      ebx, ebx
        je        _B5_2

_B5_6::

        xor       esi, esi

_B5_7::

        bt        ebx, esi
        jc        _B5_10

_B5_8::

        inc       esi
        cmp       esi, 8
        jl        _B5_7

_B5_9::

        vmovups   ymm0, YMMWORD PTR [64+r13]
        jmp       _B5_2

_B5_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rsi*4]
        lea       rdx, QWORD PTR [64+r13+rsi*4]

        call      __svml_stanh_ha_cout_rare_internal
        jmp       _B5_8
        ALIGN     16

_B5_11::

__svml_tanhf8_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanhf8_ha_e9_B1_B10:
	DD	1801217
	DD	4510844
	DD	1992820
	DD	1865835
	DD	1738850
	DD	1480793
	DD	1353808
	DD	1226818
	DD	1099833
	DD	2152496
	DD	1632295
	DD	981022
	DD	4587790
	DD	1611034631
	DD	12293

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_tanhf8_ha_e9_B1_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tanhf4_ha_ex

__svml_tanhf4_ha_ex	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L152::

        sub       rsp, 424
        movaps    xmm4, xmm0
        movups    XMMWORD PTR [256+rsp], xmm15
        lea       rdx, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [272+rsp], xmm14
        pxor      xmm1, xmm1
        movups    XMMWORD PTR [288+rsp], xmm13
        movups    XMMWORD PTR [304+rsp], xmm12
        movups    XMMWORD PTR [320+rsp], xmm11
        movups    XMMWORD PTR [336+rsp], xmm10
        movups    XMMWORD PTR [192+rsp], xmm9
        movups    XMMWORD PTR [240+rsp], xmm8
        movups    XMMWORD PTR [208+rsp], xmm7
        movups    XMMWORD PTR [224+rsp], xmm6
        mov       QWORD PTR [400+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movdqu    xmm11, XMMWORD PTR [__svml_stanh_ha_data_internal+3200]
        and       r13, -64
        pand      xmm11, xmm4
        movdqa    xmm5, xmm11
        psubd     xmm11, XMMWORD PTR [__svml_stanh_ha_data_internal+3328]
        movdqu    xmm7, XMMWORD PTR [__svml_stanh_ha_data_internal+3392]
        movdqa    xmm12, xmm11
        movdqa    xmm2, xmm11
        pcmpgtd   xmm12, xmm7
        pcmpgtd   xmm2, xmm1
        movdqa    xmm10, xmm12
        pand      xmm11, xmm2
        andps     xmm7, xmm12
        andnps    xmm10, xmm11
        orps      xmm10, xmm7
        psrld     xmm10, 15
        movups    xmm3, XMMWORD PTR [__svml_stanh_ha_data_internal+3072]
        movd      ecx, xmm10
        andps     xmm3, xmm4
        pshufd    xmm8, xmm10, 1
        pshufd    xmm9, xmm10, 2
        movd      eax, xmm8
        movups    XMMWORD PTR [352+rsp], xmm3
        pshufd    xmm3, xmm10, 3
        movd      r9d, xmm9
        movd      r10d, xmm3
        movups    xmm0, XMMWORD PTR [__svml_stanh_ha_data_internal+3136]
        pcmpgtd   xmm5, XMMWORD PTR [__svml_stanh_ha_data_internal+3264]
        andps     xmm0, xmm4
        movsxd    rcx, ecx
        movsxd    rax, eax
        movsxd    r9, r9d
        movsxd    r10, r10d
        movups    xmm11, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+rdx+rcx]
        movups    XMMWORD PTR [32+rsp], xmm4
        movaps    xmm4, xmm11
        movmskps  r8d, xmm5
        movups    xmm5, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+rdx+rax]
        movups    xmm12, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+rdx+r9]
        movups    xmm7, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+rdx+rcx]
        movaps    xmm6, xmm12
        unpcklpd  xmm4, xmm5
        movaps    xmm9, xmm7
        movups    xmm1, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1344+rdx+r10]
        movups    XMMWORD PTR [368+rsp], xmm4
        movups    xmm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+rdx+rax]
        movups    xmm8, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+rdx+r9]
        movups    xmm4, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+rdx+r9]
        movaps    xmm10, xmm8
        unpcklpd  xmm6, xmm1
        movups    XMMWORD PTR [384+rsp], xmm6
        movaps    xmm6, xmm4
        movups    xmm14, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1360+rdx+r10]
        movups    xmm2, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+rdx+r10]
        unpcklpd  xmm9, xmm13
        unpckhpd  xmm7, xmm13
        movups    xmm3, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+rdx+rcx]
        movups    xmm13, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+rdx+rcx]
        unpckhpd  xmm11, xmm5
        movaps    xmm5, xmm3
        unpckhpd  xmm12, xmm1
        movaps    xmm1, xmm13
        unpcklpd  xmm10, xmm14
        unpckhpd  xmm8, xmm14
        movups    xmm15, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1376+rdx+rax]
        unpcklpd  xmm6, xmm2
        unpckhpd  xmm4, xmm2
        movups    xmm2, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+rdx+rax]
        movups    xmm14, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+rdx+r9]
        unpcklpd  xmm5, xmm15
        unpckhpd  xmm3, xmm15
        movups    xmm15, XMMWORD PTR [imagerel(__svml_stanh_ha_data_internal)+1392+rdx+r10]
        unpcklpd  xmm1, xmm2
        unpckhpd  xmm13, xmm2
        movaps    xmm2, xmm14
        unpcklpd  xmm2, xmm15
        unpckhpd  xmm14, xmm15
        cvtps2pd  xmm15, xmm0
        movhlps   xmm0, xmm0
        cvtps2pd  xmm0, xmm0
        mulpd     xmm13, xmm15
        mulpd     xmm14, xmm0
        addpd     xmm13, xmm1
        addpd     xmm14, xmm2
        mulpd     xmm13, xmm15
        mulpd     xmm14, xmm0
        addpd     xmm13, xmm3
        addpd     xmm14, xmm4
        mulpd     xmm13, xmm15
        mulpd     xmm14, xmm0
        addpd     xmm13, xmm5
        addpd     xmm14, xmm6
        mulpd     xmm13, xmm15
        mulpd     xmm14, xmm0
        addpd     xmm13, xmm7
        addpd     xmm14, xmm8
        mulpd     xmm13, xmm15
        mulpd     xmm14, xmm0
        addpd     xmm13, xmm9
        addpd     xmm14, xmm10
        mulpd     xmm13, xmm15
        mulpd     xmm14, xmm0
        addpd     xmm13, xmm11
        addpd     xmm14, xmm12
        mulpd     xmm15, xmm13
        mulpd     xmm0, xmm14
        addpd     xmm15, XMMWORD PTR [368+rsp]
        addpd     xmm0, XMMWORD PTR [384+rsp]
        cvtpd2ps  xmm2, xmm15
        cvtpd2ps  xmm1, xmm0
        movlhps   xmm2, xmm1
        mov       QWORD PTR [408+rsp], r13
        orps      xmm2, XMMWORD PTR [352+rsp]
        test      r8d, r8d
        jne       _B6_3

_B6_2::

        movups    xmm6, XMMWORD PTR [224+rsp]
        movaps    xmm0, xmm2
        movups    xmm7, XMMWORD PTR [208+rsp]
        movups    xmm8, XMMWORD PTR [240+rsp]
        movups    xmm9, XMMWORD PTR [192+rsp]
        movups    xmm10, XMMWORD PTR [336+rsp]
        movups    xmm11, XMMWORD PTR [320+rsp]
        movups    xmm12, XMMWORD PTR [304+rsp]
        movups    xmm13, XMMWORD PTR [288+rsp]
        movups    xmm14, XMMWORD PTR [272+rsp]
        movups    xmm15, XMMWORD PTR [256+rsp]
        mov       r13, QWORD PTR [400+rsp]
        add       rsp, 424
        ret

_B6_3::

        movups    xmm1, XMMWORD PTR [32+rsp]
        movups    XMMWORD PTR [r13], xmm1
        movups    XMMWORD PTR [64+r13], xmm2

_B6_6::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, r8d

_B6_7::

        mov       ecx, ebx
        mov       eax, 1
        shl       eax, cl
        test      esi, eax
        jne       _B6_10

_B6_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B6_7

_B6_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm2, XMMWORD PTR [64+r13]
        jmp       _B6_2

_B6_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_stanh_ha_cout_rare_internal
        jmp       _B6_8
        ALIGN     16

_B6_11::

__svml_tanhf4_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanhf4_ha_ex_B1_B3:
	DD	1603841
	DD	3331193
	DD	944241
	DD	882793
	DD	1017953
	DD	825432
	DD	1419343
	DD	1357894
	DD	1296445
	DD	1234996
	DD	1173543
	DD	1112087
	DD	3473675

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_tanhf4_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tanhf4_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_tanhf4_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_6
	DD	imagerel _B6_11
	DD	imagerel _unwind___svml_tanhf4_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_stanh_ha_cout_rare_internal

__svml_stanh_ha_cout_rare_internal	PROC

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L187::

        lea       r8, QWORD PTR [__ImageBase]
        mov       al, BYTE PTR [3+rcx]
        and       al, -128
        shr       al, 7
        movzx     r9d, al
        movzx     r10d, WORD PTR [2+rcx]
        and       r10d, 32640
        mov       eax, DWORD PTR [imagerel(_imlsTanhHATab)+r8+r9*4]
        cmp       r10d, 32640
        je        _B7_4

_B7_2::

        mov       DWORD PTR [rdx], eax

_B7_3::

        xor       eax, eax
        ret

_B7_4::

        test      DWORD PTR [rcx], 8388607
        je        _B7_2

_B7_5::

        movss     xmm0, DWORD PTR [rcx]
        addss     xmm0, xmm0
        movss     DWORD PTR [rdx], xmm0
        jmp       _B7_3
        ALIGN     16

_B7_6::

__svml_stanh_ha_cout_rare_internal ENDP

_TEXT	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_stanh_ha_data_internal
__svml_stanh_ha_data_internal	DD	0
	DD	1030750208
	DD	1032847360
	DD	1034944512
	DD	1037041664
	DD	1039138816
	DD	1041235968
	DD	1043333120
	DD	1045430272
	DD	1047527424
	DD	1049624576
	DD	1051721728
	DD	1053818880
	DD	1055916032
	DD	1058013184
	DD	1060110336
	DD	1062207488
	DD	1064304640
	DD	1066401792
	DD	1068498944
	DD	1070596096
	DD	1072693248
	DD	1074790400
	DD	1076887552
	DD	1078984704
	DD	1081081856
	DD	1083179008
	DD	1085276160
	DD	1087373312
	DD	1089470464
	DD	1091567616
	DD	0
	DD	0
	DD	1030732233
	DD	1032831839
	DD	1034916201
	DD	1036994987
	DD	1039067209
	DD	1041174248
	DD	1043220868
	DD	1045245838
	DD	1047245614
	DD	1049383373
	DD	1051287907
	DD	1053115377
	DD	1054857013
	DD	1057129528
	DD	1058581488
	DD	1059832960
	DD	1060891676
	DD	1062153819
	DD	1063337043
	DD	1064100733
	DD	1064582223
	DD	1064984555
	DD	1065216645
	DD	1065302845
	DD	1065334668
	DD	1065349076
	DD	1065352656
	DD	1065353140
	DD	1065353206
	DD	1065353215
	DD	1065353216
	DD	0
	DD	2963361822
	DD	2971470750
	DD	2945658640
	DD	821708412
	DD	824483568
	DD	824941280
	DD	2984085072
	DD	2957298688
	DD	838449816
	DD	2966046080
	DD	2988320324
	DD	2989804564
	DD	842626356
	DD	3000013710
	DD	2972725824
	DD	3002017674
	DD	853753500
	DD	2987104448
	DD	3000350914
	DD	855535800
	DD	852410906
	DD	851608946
	DD	2988641656
	DD	2997011000
	DD	2989576736
	DD	3000884068
	DD	2999984336
	DD	840950056
	DD	2995215280
	DD	855269702
	DD	0
	DD	1065353216
	DD	1065295748
	DD	1065270545
	DD	1065229919
	DD	1065181343
	DD	1065124909
	DD	1065025765
	DD	1064867200
	DD	1064679597
	DD	1064464345
	DD	1064093083
	DD	1063517074
	DD	1062862743
	DD	1062146519
	DD	1060992371
	DD	1059386208
	DD	1057800167
	DD	1055660649
	DD	1051764737
	DD	1046959010
	DD	1041444634
	DD	1035462611
	DD	1026689093
	DD	1015337940
	DD	1002731447
	DD	990958554
	DD	973168670
	DD	948705851
	DD	924299482
	DD	899955662
	DD	864224966
	DD	0
	DD	2956213371
	DD	3178161821
	DD	3180268967
	DD	3182315389
	DD	3184339487
	DD	3186337805
	DD	3188474939
	DD	3190373619
	DD	3192189570
	DD	3193910865
	DD	3196176320
	DD	3197556682
	DD	3198679950
	DD	3199536798
	DD	3200331518
	DD	3200564882
	DD	3200049264
	DD	3199029518
	DD	3197040598
	DD	3192620804
	DD	3188208183
	DD	3182392393
	DD	3173916356
	DD	3162750726
	DD	3150176437
	DD	3138431708
	DD	3120650203
	DD	3096189170
	DD	3071783062
	DD	3047439278
	DD	3011707180
	DD	0
	DD	3198855845
	DD	3198879250
	DD	3198677023
	DD	3198476576
	DD	3198388151
	DD	3198245218
	DD	3197982711
	DD	3197594458
	DD	3197117197
	DD	3196587519
	DD	3195304371
	DD	3192667528
	DD	3189843074
	DD	3186330810
	DD	3177085101
	DD	1013669486
	DD	1032032579
	DD	1036132065
	DD	1038305199
	DD	1036774550
	DD	1033498413
	DD	1028927137
	DD	1021175553
	DD	1009568359
	DD	998361895
	DD	985691041
	DD	967585842
	DD	943363289
	DD	919210013
	DD	895139148
	DD	858471606
	DD	0
	DD	3077428921
	DD	3189516141
	DD	1008586543
	DD	1036101517
	DD	1033304453
	DD	1034073627
	DD	1036071831
	DD	1037235824
	DD	1039436298
	DD	1040631208
	DD	1041906362
	DD	1042793477
	DD	1043232976
	DD	1043086916
	DD	1042100375
	DD	1039444212
	DD	1034126600
	DD	1026638186
	DD	995501655
	DD	3165579977
	DD	3167654937
	DD	3165317828
	DD	3158960080
	DD	3148291549
	DD	3137354510
	DD	3124730373
	DD	3106670759
	DD	3082457650
	DD	3058305807
	DD	3034235241
	DD	2997581996
	DD	0
	DD	1040781545
	DD	1131811139
	DD	1097198812
	DD	3247503190
	DD	3230402941
	DD	3224086547
	DD	3212798938
	DD	1059790272
	DD	1053691997
	DD	1061317268
	DD	3134918084
	DD	1034173207
	DD	3176246152
	DD	3165561405
	DD	3174788493
	DD	3178015405
	DD	3178847213
	DD	3177176538
	DD	3171127099
	DD	3155996003
	DD	985352038
	DD	999682315
	DD	998398067
	DD	989522534
	DD	977926264
	DD	966355955
	DD	948911724
	DD	924561635
	DD	900244966
	DD	875993879
	DD	841254832
	DD	0
	DD	3155046246
	DD	1175181842
	DD	1138112751
	DD	3286309950
	DD	3267011817
	DD	3259619885
	DD	3246758786
	DD	1088248663
	DD	1078543936
	DD	1086795944
	DD	3205436942
	DD	1043392367
	DD	3198686087
	DD	3182586396
	DD	3174374999
	DD	3142320544
	DD	1008565243
	DD	1014115537
	DD	1016545052
	DD	1010017051
	DD	998649588
	DD	975680464
	DD	3124451591
	DD	3121544226
	DD	3112148751
	DD	3100159824
	DD	3082673659
	DD	3058641232
	DD	3034613169
	DD	3010665978
	DD	2975473412
	DD	0
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	1027604480
	DD	65011712
	DD	65011712
	DD	65011712
	DD	65011712
	DD	65011712
	DD	65011712
	DD	65011712
	DD	65011712
	DD	65011712
	DD	65011712
	DD	65011712
	DD	65011712
	DD	65011712
	DD	65011712
	DD	65011712
	DD	65011712
	DD	0
	DD	0
	DD	434973
	DD	1072693248
	DD	2462381979
	DD	3194875870
	DD	1922363613
	DD	3218429247
	DD	3436033793
	DD	3207031997
	DD	1164049177
	DD	1069640674
	DD	2766840751
	DD	3214171076
	DD	0
	DD	0
	DD	1640345390
	DD	3194496887
	DD	1662417413
	DD	1072693250
	DD	3716526453
	DD	3205098446
	DD	1853512117
	DD	3218426760
	DD	3156517427
	DD	3211911078
	DD	3184519871
	DD	1069779119
	DD	3280598482
	DD	3215502338
	DD	0
	DD	0
	DD	1962245523
	DD	3196442075
	DD	366239963
	DD	1072693255
	DD	3337913224
	DD	3206521562
	DD	1328356923
	DD	3218424220
	DD	1470245799
	DD	3212690354
	DD	922782103
	DD	1069841533
	DD	705136934
	DD	3215769608
	DD	0
	DD	0
	DD	917120191
	DD	3198018856
	DD	2270262052
	DD	1072693264
	DD	21785897
	DD	3207561752
	DD	3853373788
	DD	3218420654
	DD	3959915849
	DD	3213220134
	DD	839274685
	DD	1069902529
	DD	3478609944
	DD	3215984949
	DD	0
	DD	0
	DD	321264669
	DD	3199232231
	DD	3507921756
	DD	1072693279
	DD	855596455
	DD	3208292995
	DD	4197403487
	DD	3218416395
	DD	1260192796
	DD	3213688235
	DD	509545499
	DD	1069956190
	DD	4001843557
	DD	3216067072
	DD	0
	DD	0
	DD	2572895834
	DD	3200373196
	DD	4238319527
	DD	1072693307
	DD	1589084946
	DD	3209032256
	DD	323547252
	DD	3218410632
	DD	129829396
	DD	3214058556
	DD	2665301683
	DD	1070009663
	DD	3805267410
	DD	3216137363
	DD	0
	DD	0
	DD	1373918637
	DD	3199925337
	DD	2391440540
	DD	1072693299
	DD	3494583150
	DD	3208925835
	DD	2192964039
	DD	3218411256
	DD	579095213
	DD	3214044622
	DD	3432431090
	DD	1070009041
	DD	3870858437
	DD	3216138421
	DD	0
	DD	0
	DD	3062447777
	DD	1055926683
	DD	3334650904
	DD	1072692790
	DD	3497776375
	DD	1062371871
	DD	4014660983
	DD	3218436927
	DD	1708666466
	DD	3212333537
	DD	648260668
	DD	1069902577
	DD	1156520282
	DD	3216044909
	DD	0
	DD	0
	DD	4186264729
	DD	1058462985
	DD	3883474621
	DD	1072690745
	DD	4001630278
	DD	1065042042
	DD	484659484
	DD	3218507007
	DD	301873053
	DD	1066864880
	DD	2426783364
	DD	1069685380
	DD	3518509994
	DD	3215777524
	DD	0
	DD	0
	DD	1324317639
	DD	1061009204
	DD	1677646538
	DD	1072681882
	DD	781584286
	DD	1067165904
	DD	3649499968
	DD	3218726741
	DD	2264952365
	DD	1069102871
	DD	2344790854
	DD	1068835622
	DD	4047770869
	DD	3215138580
	DD	0
	DD	0
	DD	70848422
	DD	1063287485
	DD	1930391614
	DD	1072650795
	DD	586495590
	DD	1068891644
	DD	2415479819
	DD	3219189888
	DD	2049892606
	DD	1070582148
	DD	1783689851
	DD	3213584996
	DD	2396151379
	DD	3213355995
	DD	0
	DD	0
	DD	2764829776
	DD	1064683280
	DD	95861817
	DD	1072595436
	DD	350241294
	DD	1069957747
	DD	1429983818
	DD	3219518543
	DD	2046078110
	DD	1071248730
	DD	2818409407
	DD	3216573116
	DD	351621961
	DD	1065184929
	DD	0
	DD	0
	DD	818345493
	DD	1065579544
	DD	47166764
	DD	1072535009
	DD	2931635641
	DD	1070624305
	DD	2472163867
	DD	3219785146
	DD	898647657
	DD	1071677167
	DD	2840881315
	DD	3217227676
	DD	1213275070
	DD	1066490976
	DD	0
	DD	0
	DD	3770339094
	DD	1065664250
	DD	4021094867
	DD	1072525054
	DD	3250137669
	DD	1070683759
	DD	3067647579
	DD	3219831010
	DD	706412794
	DD	1071716084
	DD	3457985438
	DD	3217296958
	DD	693681995
	DD	1066592455
	DD	0
	DD	0
	DD	794345931
	DD	3214229553
	DD	674007974
	DD	1072761769
	DD	1339296402
	DD	3213866766
	DD	2063412275
	DD	3219199437
	DD	3042293216
	DD	1071038746
	DD	1218111703
	DD	3216613854
	DD	1828949834
	DD	1065778789
	DD	0
	DD	0
	DD	3709362262
	DD	3216572138
	DD	1704472411
	DD	1073083731
	DD	334125080
	DD	3219185499
	DD	3643953259
	DD	3216245823
	DD	972935809
	DD	1069563300
	DD	4262764539
	DD	3215188513
	DD	3947124972
	DD	1064363655
	DD	0
	DD	0
	DD	684725320
	DD	3217602215
	DD	2059930851
	DD	1073428282
	DD	6923247
	DD	3220175349
	DD	1962536238
	DD	1070738118
	DD	2626892535
	DD	3214818472
	DD	1541908021
	DD	3211168932
	DD	1264782098
	DD	1061514036
	DD	0
	DD	0
	DD	4193183898
	DD	3218211722
	DD	2527318106
	DD	1073704783
	DD	1779267795
	DD	3220520390
	DD	2178062862
	DD	1071649373
	DD	2371270354
	DD	3216802466
	DD	214503718
	DD	1066134183
	DD	2527651537
	DD	3209129722
	DD	0
	DD	0
	DD	1145099230
	DD	3217848868
	DD	1219675578
	DD	1073564173
	DD	3377824792
	DD	3220387400
	DD	1294161399
	DD	1071386209
	DD	535756989
	DD	3216499614
	DD	3414431292
	DD	1065769858
	DD	3872552752
	DD	3208765586
	DD	0
	DD	0
	DD	3432152680
	DD	3212471108
	DD	3481247728
	DD	1073111648
	DD	2087872556
	DD	3219843286
	DD	1539630695
	DD	1070713931
	DD	2045031161
	DD	3215666774
	DD	1438917333
	DD	1064738520
	DD	2997200424
	DD	3207590169
	DD	0
	DD	0
	DD	157024952
	DD	1070614475
	DD	1896115811
	DD	1072588717
	DD	1533634146
	DD	3219167457
	DD	3479089950
	DD	1069795336
	DD	294041664
	DD	3214609167
	DD	3323703207
	DD	1063520882
	DD	1200470279
	DD	3206092743
	DD	0
	DD	0
	DD	780145450
	DD	1071804775
	DD	3436973384
	DD	1071541223
	DD	1373298557
	DD	3217881162
	DD	616458359
	DD	1068360186
	DD	1012488256
	DD	3212939359
	DD	3381328826
	DD	1061569412
	DD	3619594050
	DD	3203906531
	DD	0
	DD	0
	DD	3555024088
	DD	1072352823
	DD	703965661
	DD	1069801815
	DD	68876051
	DD	3215985072
	DD	4285546012
	DD	1066131701
	DD	1692571309
	DD	3210444434
	DD	2250664999
	DD	1058874117
	DD	2757518980
	DD	3200902424
	DD	0
	DD	0
	DD	4088530245
	DD	1072580854
	DD	2571880719
	DD	1067895848
	DD	4091013897
	DD	3213873796
	DD	4246435429
	DD	1063770948
	DD	92905889
	DD	3207872058
	DD	248987709
	DD	1056074614
	DD	2369951583
	DD	3197898922
	DD	0
	DD	0
	DD	3580076556
	DD	1072660066
	DD	1353576036
	DD	1065860878
	DD	2410885661
	DD	3211602990
	DD	2989427096
	DD	1061369430
	DD	3886685439
	DD	3205273864
	DD	529712074
	DD	1053215589
	DD	3764845364
	DD	3194905549
	DD	0
	DD	0
	DD	660908647
	DD	1072688177
	DD	2675542798
	DD	1062777930
	DD	772498083
	DD	3208233517
	DD	377295306
	DD	1057798793
	DD	162648032
	DD	3201438006
	DD	623489458
	DD	1049119366
	DD	3651746243
	DD	3190506519
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
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
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
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2145386496
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	2130706432
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	1038090240
	DD	54525952
	DD	54525952
	DD	54525952
	DD	54525952
	DD	54525952
	DD	54525952
	DD	54525952
	DD	54525952
	DD	54525952
	DD	54525952
	DD	54525952
	DD	54525952
	DD	54525952
	DD	54525952
	DD	54525952
	DD	54525952
_imlsTanhHATab	DD	1065353216
	DD	3212836864
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
