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
	PUBLIC __svml_atan2_ha_e9

__svml_atan2_ha_e9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 328
        vmovapd   xmm5, xmm0
        vmovups   XMMWORD PTR [224+rsp], xmm15
        lea       r8, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [240+rsp], xmm14
        vmovups   XMMWORD PTR [256+rsp], xmm13
        vmovups   XMMWORD PTR [272+rsp], xmm12
        vmovups   XMMWORD PTR [288+rsp], xmm10
        vmovups   XMMWORD PTR [192+rsp], xmm7
        vmovups   XMMWORD PTR [208+rsp], xmm6
        mov       QWORD PTR [304+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vandpd    xmm3, xmm5, XMMWORD PTR [__svml_datan_ha_data_internal+1152]
        and       r13, -64
        vmovupd   xmm2, XMMWORD PTR [__svml_datan_ha_data_internal+1344]
        vxorpd    xmm4, xmm5, xmm3
        vpshufd   xmm6, xmm3, 221
        vandpd    xmm0, xmm3, xmm2
        vmovq     xmm15, QWORD PTR [__svml_datan_ha_data_internal+2560]
        vsubpd    xmm1, xmm3, xmm0
        vmovq     xmm3, QWORD PTR [__svml_datan_ha_data_internal+2624]
        vpsubd    xmm12, xmm6, xmm15
        vpcmpgtd  xmm10, xmm12, xmm3
        vpcmpeqd  xmm7, xmm12, xmm3
        vpor      xmm13, xmm10, xmm7
        vmovmskps edx, xmm13
        vmovq     xmm14, QWORD PTR [__svml_datan_ha_data_internal+1408]
        vmovq     xmm13, QWORD PTR [__svml_datan_ha_data_internal+1472]
        vpsubd    xmm12, xmm14, xmm6
        vmovq     xmm7, QWORD PTR [__svml_datan_ha_data_internal+1536]
        vpsubd    xmm13, xmm13, xmm6
        vmovq     xmm15, QWORD PTR [__svml_datan_ha_data_internal+1600]
        vpsrad    xmm12, xmm12, 31
        vpsubd    xmm15, xmm15, xmm6
        vpsubd    xmm6, xmm7, xmm6
        vpsrad    xmm14, xmm15, 31
        vpsrad    xmm13, xmm13, 31
        vpsrad    xmm6, xmm6, 31
        vpaddd    xmm15, xmm12, xmm14
        vpaddd    xmm12, xmm13, xmm6
        vmovq     xmm10, QWORD PTR [__svml_datan_ha_data_internal+1664]
        vpaddd    xmm7, xmm15, xmm12
        vpaddd    xmm10, xmm7, xmm10
        vpslld    xmm13, xmm10, 5
        vmovd     eax, xmm13
        vmovupd   xmm3, XMMWORD PTR [__svml_datan_ha_data_internal+1728]
        vpextrd   ecx, xmm13, 1
        vmovq     xmm14, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+r8+rax]
        vmovq     xmm6, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+r8+rax]
        vmovhpd   xmm15, xmm14, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+r8+rcx]
        vmovhpd   xmm14, xmm6, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+r8+rcx]
        vandpd    xmm7, xmm14, xmm0
        vsubpd    xmm7, xmm7, xmm15
        vmulpd    xmm0, xmm0, xmm15
        vmovq     xmm12, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+r8+rax]
        vmovq     xmm10, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+r8+rax]
        vmovhpd   xmm6, xmm12, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+r8+rcx]
        vmovhpd   xmm12, xmm10, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+r8+rcx]
        vandpd    xmm10, xmm14, xmm1
        vsubpd    xmm13, xmm7, xmm10
        vmulpd    xmm1, xmm15, xmm1
        vsubpd    xmm7, xmm13, xmm7
        vsubpd    xmm7, xmm10, xmm7
        vandpd    xmm10, xmm13, xmm2
        vandpd    xmm14, xmm14, xmm3
        vsubpd    xmm13, xmm13, xmm10
        vaddpd    xmm13, xmm7, xmm13
        vaddpd    xmm7, xmm14, xmm0
        vaddpd    xmm0, xmm7, xmm1
        vsubpd    xmm15, xmm7, xmm0
        vaddpd    xmm15, xmm1, xmm15
        vandpd    xmm1, xmm0, xmm2
        vsubpd    xmm2, xmm0, xmm1
        vcvtpd2ps xmm0, xmm1
        vaddpd    xmm15, xmm15, xmm2
        vmovlhps  xmm2, xmm0, xmm0
        vrcpps    xmm7, xmm2
        vcvtps2pd xmm7, xmm7
        vmulpd    xmm1, xmm1, xmm7
        vmulpd    xmm15, xmm7, xmm15
        vsubpd    xmm3, xmm1, xmm3
        vaddpd    xmm14, xmm3, xmm15
        vmulpd    xmm15, xmm14, xmm14
        vsubpd    xmm1, xmm15, xmm14
        vmulpd    xmm0, xmm14, xmm1
        vaddpd    xmm2, xmm14, xmm0
        vmulpd    xmm3, xmm14, xmm2
        vsubpd    xmm15, xmm3, xmm14
        vmulpd    xmm1, xmm14, xmm15
        vaddpd    xmm0, xmm14, xmm1
        vmulpd    xmm2, xmm14, xmm0
        vsubpd    xmm14, xmm2, xmm14
        vmulpd    xmm15, xmm7, xmm14
        vmulpd    xmm1, xmm10, xmm15
        vmulpd    xmm0, xmm13, xmm15
        vmulpd    xmm13, xmm7, xmm13
        vaddpd    xmm2, xmm1, xmm0
        vmulpd    xmm15, xmm10, xmm7
        vaddpd    xmm0, xmm2, xmm13
        vaddpd    xmm1, xmm0, xmm15
        vaddpd    xmm12, xmm12, xmm0
        vmulpd    xmm2, xmm1, xmm1
        vmulpd    xmm3, xmm2, xmm2
        vmulpd    xmm10, xmm3, XMMWORD PTR [__svml_datan_ha_data_internal+1792]
        vmulpd    xmm7, xmm3, XMMWORD PTR [__svml_datan_ha_data_internal+1856]
        vaddpd    xmm10, xmm10, XMMWORD PTR [__svml_datan_ha_data_internal+1920]
        vaddpd    xmm14, xmm7, XMMWORD PTR [__svml_datan_ha_data_internal+1984]
        vmulpd    xmm13, xmm3, xmm10
        vmulpd    xmm14, xmm3, xmm14
        vaddpd    xmm7, xmm13, XMMWORD PTR [__svml_datan_ha_data_internal+2048]
        vaddpd    xmm13, xmm14, XMMWORD PTR [__svml_datan_ha_data_internal+2112]
        vmulpd    xmm10, xmm3, xmm7
        vmulpd    xmm13, xmm3, xmm13
        vaddpd    xmm7, xmm10, XMMWORD PTR [__svml_datan_ha_data_internal+2176]
        vaddpd    xmm14, xmm13, XMMWORD PTR [__svml_datan_ha_data_internal+2240]
        vmulpd    xmm10, xmm3, xmm7
        vmulpd    xmm14, xmm3, xmm14
        vaddpd    xmm7, xmm10, XMMWORD PTR [__svml_datan_ha_data_internal+2304]
        vaddpd    xmm13, xmm14, XMMWORD PTR [__svml_datan_ha_data_internal+2368]
        vmulpd    xmm10, xmm3, xmm7
        vmulpd    xmm3, xmm3, xmm13
        vaddpd    xmm7, xmm10, XMMWORD PTR [__svml_datan_ha_data_internal+2432]
        vaddpd    xmm3, xmm3, XMMWORD PTR [__svml_datan_ha_data_internal+2496]
        vmulpd    xmm10, xmm2, xmm7
        vaddpd    xmm13, xmm3, xmm10
        vmulpd    xmm2, xmm2, xmm13
        vmulpd    xmm0, xmm1, xmm2
        vaddpd    xmm1, xmm12, xmm0
        vaddpd    xmm2, xmm15, xmm1
        vaddpd    xmm6, xmm6, xmm2
        mov       QWORD PTR [312+rsp], r13
        vorpd     xmm0, xmm6, xmm4
        and       edx, 3
        jne       _B1_3

_B1_2::

        vmovups   xmm6, XMMWORD PTR [208+rsp]
        vmovups   xmm7, XMMWORD PTR [192+rsp]
        vmovups   xmm10, XMMWORD PTR [288+rsp]
        vmovups   xmm12, XMMWORD PTR [272+rsp]
        vmovups   xmm13, XMMWORD PTR [256+rsp]
        vmovups   xmm14, XMMWORD PTR [240+rsp]
        vmovups   xmm15, XMMWORD PTR [224+rsp]
        mov       r13, QWORD PTR [304+rsp]
        add       rsp, 328
        ret

_B1_3::

        vmovupd   XMMWORD PTR [r13], xmm5
        vmovupd   XMMWORD PTR [64+r13], xmm0
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
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B1_2

_B1_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_datan_ha_cout_rare_internal
        jmp       _B1_8
        ALIGN     16

_B1_11::

__svml_atan2_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_atan2_ha_e9_B1_B3:
	DD	1203457
	DD	2544733
	DD	878677
	DD	817228
	DD	1222723
	DD	1165370
	DD	1103921
	DD	1042472
	DD	981016
	DD	2687243

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_atan2_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_atan2_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_atan2_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_6
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_atan2_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_atan1_ha_e9

__svml_atan1_ha_e9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L22::

        sub       rsp, 264
        mov       eax, -2144337920
        vmovups   XMMWORD PTR [176+rsp], xmm12
        mov       edx, -36700160
        vmovups   XMMWORD PTR [192+rsp], xmm11
        vmovapd   xmm12, xmm0
        vmovups   XMMWORD PTR [208+rsp], xmm10
        mov       r8d, 1072037888
        vmovups   XMMWORD PTR [224+rsp], xmm9
        vmovd     xmm9, eax
        vmovups   XMMWORD PTR [240+rsp], xmm8
        mov       ecx, 1071382528
        mov       QWORD PTR [168+rsp], r13
        mov       r9d, 1072889856
        vmovsd    xmm11, QWORD PTR [__svml_datan_ha_data_internal+1152]
        vmovd     xmm4, edx
        vandpd    xmm8, xmm12, xmm11
        mov       r10d, 1073971200
        vmovsd    xmm5, QWORD PTR [__svml_datan_ha_data_internal+1344]
        vxorpd    xmm11, xmm12, xmm8
        vpshufd   xmm3, xmm8, 85
        vandpd    xmm1, xmm8, xmm5
        vpsubd    xmm10, xmm3, xmm9
        mov       r11d, 4
        vpcmpgtd  xmm0, xmm10, xmm4
        lea       r13, QWORD PTR [95+rsp]
        and       r13, -64
        vsubsd    xmm2, xmm8, xmm1
        vpcmpeqd  xmm8, xmm10, xmm4
        vmovd     xmm10, ecx
        vpor      xmm9, xmm0, xmm8
        vmovd     xmm0, r8d
        vmovmskps eax, xmm9
        vpsubd    xmm9, xmm0, xmm3
        vmovd     xmm0, r9d
        vpsubd    xmm4, xmm10, xmm3
        vpsrad    xmm10, xmm9, 31
        vpsubd    xmm9, xmm0, xmm3
        vmovd     xmm0, r10d
        vpsrad    xmm4, xmm4, 31
        vpsubd    xmm3, xmm0, xmm3
        vpsrad    xmm9, xmm9, 31
        vpsrad    xmm3, xmm3, 31
        vpaddd    xmm10, xmm4, xmm10
        vpaddd    xmm9, xmm9, xmm3
        vmovd     xmm0, r11d
        vpaddd    xmm4, xmm10, xmm9
        lea       rcx, QWORD PTR [__ImageBase]
        vpaddd    xmm9, xmm4, xmm0
        vpslld    xmm10, xmm9, 5
        vmovd     edx, xmm10
        vmovsd    xmm8, QWORD PTR [__svml_datan_ha_data_internal+1728]
        vmovq     xmm3, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+rcx+rdx]
        vmovq     xmm4, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+rcx+rdx]
        vandpd    xmm9, xmm3, xmm1
        vandpd    xmm10, xmm3, xmm2
        vandpd    xmm3, xmm3, xmm8
        vmulsd    xmm1, xmm4, xmm1
        vsubsd    xmm9, xmm9, xmm4
        vmulsd    xmm4, xmm4, xmm2
        vsubsd    xmm0, xmm9, xmm10
        vaddsd    xmm1, xmm1, xmm3
        vsubsd    xmm9, xmm0, xmm9
        vaddsd    xmm2, xmm1, xmm4
        vsubsd    xmm9, xmm10, xmm9
        vandpd    xmm10, xmm0, xmm5
        mov       QWORD PTR [256+rsp], r13
        vsubsd    xmm0, xmm0, xmm10
        vaddsd    xmm9, xmm0, xmm9
        vsubsd    xmm0, xmm1, xmm2
        vaddsd    xmm1, xmm4, xmm0
        vandpd    xmm0, xmm2, xmm5
        vsubsd    xmm5, xmm2, xmm0
        vaddsd    xmm3, xmm1, xmm5
        vcvtpd2ps xmm5, xmm0
        vmovlhps  xmm2, xmm5, xmm5
        vrcpps    xmm1, xmm2
        vcvtps2pd xmm1, xmm1
        vmulsd    xmm4, xmm0, xmm1
        vsubsd    xmm0, xmm4, xmm8
        vmulsd    xmm8, xmm3, xmm1
        vaddsd    xmm4, xmm8, xmm0
        vmulsd    xmm3, xmm4, xmm4
        vsubsd    xmm5, xmm3, xmm4
        vmulsd    xmm8, xmm5, xmm4
        vaddsd    xmm2, xmm8, xmm4
        vmulsd    xmm0, xmm2, xmm4
        vsubsd    xmm3, xmm0, xmm4
        vmulsd    xmm5, xmm3, xmm4
        vaddsd    xmm8, xmm5, xmm4
        vmulsd    xmm2, xmm8, xmm4
        vsubsd    xmm4, xmm2, xmm4
        vmulsd    xmm5, xmm4, xmm1
        vmulsd    xmm2, xmm5, xmm10
        vmulsd    xmm8, xmm5, xmm9
        vmulsd    xmm9, xmm1, xmm9
        vmulsd    xmm5, xmm1, xmm10
        vaddsd    xmm0, xmm8, xmm2
        vmovsd    xmm1, QWORD PTR [__svml_datan_ha_data_internal+1792]
        vaddsd    xmm2, xmm9, xmm0
        vmovsd    xmm0, QWORD PTR [__svml_datan_ha_data_internal+1856]
        vaddsd    xmm8, xmm5, xmm2
        vaddsd    xmm2, xmm2, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+rcx+rdx]
        vmulsd    xmm9, xmm8, xmm8
        vmulsd    xmm10, xmm9, xmm9
        vmulsd    xmm1, xmm1, xmm10
        vmulsd    xmm3, xmm0, xmm10
        vaddsd    xmm4, xmm1, QWORD PTR [__svml_datan_ha_data_internal+1920]
        vaddsd    xmm0, xmm3, QWORD PTR [__svml_datan_ha_data_internal+1984]
        vmulsd    xmm4, xmm4, xmm10
        vmulsd    xmm1, xmm0, xmm10
        vaddsd    xmm3, xmm4, QWORD PTR [__svml_datan_ha_data_internal+2048]
        vaddsd    xmm0, xmm1, QWORD PTR [__svml_datan_ha_data_internal+2112]
        vmulsd    xmm3, xmm3, xmm10
        vmulsd    xmm1, xmm0, xmm10
        vaddsd    xmm4, xmm3, QWORD PTR [__svml_datan_ha_data_internal+2176]
        vaddsd    xmm0, xmm1, QWORD PTR [__svml_datan_ha_data_internal+2240]
        vmulsd    xmm4, xmm4, xmm10
        vmulsd    xmm1, xmm0, xmm10
        vaddsd    xmm3, xmm4, QWORD PTR [__svml_datan_ha_data_internal+2304]
        vaddsd    xmm0, xmm1, QWORD PTR [__svml_datan_ha_data_internal+2368]
        vmulsd    xmm3, xmm3, xmm10
        vmulsd    xmm10, xmm0, xmm10
        vaddsd    xmm1, xmm3, QWORD PTR [__svml_datan_ha_data_internal+2432]
        vaddsd    xmm3, xmm10, QWORD PTR [__svml_datan_ha_data_internal+2496]
        vmulsd    xmm0, xmm1, xmm9
        vaddsd    xmm4, xmm0, xmm3
        vmulsd    xmm9, xmm4, xmm9
        vmulsd    xmm8, xmm9, xmm8
        vaddsd    xmm10, xmm8, xmm2
        vaddsd    xmm5, xmm5, xmm10
        vaddsd    xmm0, xmm5, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+rcx+rdx]
        vorpd     xmm0, xmm0, xmm11
        and       eax, 1
        jne       _B2_3

_B2_2::

        vmovups   xmm8, XMMWORD PTR [240+rsp]
        vmovups   xmm9, XMMWORD PTR [224+rsp]
        vmovups   xmm10, XMMWORD PTR [208+rsp]
        vmovups   xmm11, XMMWORD PTR [192+rsp]
        vmovups   xmm12, XMMWORD PTR [176+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 264
        ret

_B2_3::

        vmovsd    QWORD PTR [r13], xmm12
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B2_6

_B2_4::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B2_2

_B2_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_datan_ha_cout_rare_internal
        jmp       _B2_4
        ALIGN     16

_B2_7::

__svml_atan1_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_atan1_ha_e9_B1_B6:
	DD	941313
	DD	1430621
	DD	1017936
	DD	956483
	DD	895028
	DD	833575
	DD	772121
	DD	2162955

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_7
	DD	imagerel _unwind___svml_atan1_ha_e9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_atan4_ha_e9

__svml_atan4_ha_e9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L35::

        sub       rsp, 552
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [304+rsp], ymm15
        vmovups   YMMWORD PTR [336+rsp], ymm14
        vmovups   YMMWORD PTR [432+rsp], ymm13
        vmovups   YMMWORD PTR [464+rsp], ymm12
        vmovups   YMMWORD PTR [496+rsp], ymm10
        vmovups   YMMWORD PTR [368+rsp], ymm7
        vmovups   YMMWORD PTR [400+rsp], ymm6
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [207+rsp]
        vmovupd   ymm5, YMMWORD PTR [__svml_datan_ha_data_internal+1344]
        and       r13, -64
        vmovups   xmm6, XMMWORD PTR [__svml_datan_ha_data_internal+2624]
        vmovapd   ymm15, ymm0
        vandpd    ymm3, ymm15, YMMWORD PTR [__svml_datan_ha_data_internal+1152]
        vandpd    ymm1, ymm3, ymm5
        vxorpd    ymm14, ymm15, ymm3
        vsubpd    ymm2, ymm3, ymm1
        mov       QWORD PTR [536+rsp], r13
        vextractf128 xmm4, ymm3, 1
        vshufps   xmm10, xmm3, xmm4, 221
        vpsubd    xmm13, xmm10, XMMWORD PTR [__svml_datan_ha_data_internal+2560]
        vmovups   xmm4, XMMWORD PTR [__svml_datan_ha_data_internal+1408]
        vpcmpgtd  xmm12, xmm13, xmm6
        vpcmpeqd  xmm7, xmm13, xmm6
        vpsubd    xmm0, xmm4, xmm10
        vpor      xmm3, xmm12, xmm7
        vpsrad    xmm7, xmm0, 31
        vmovmskps r8d, xmm3
        vmovups   xmm3, XMMWORD PTR [__svml_datan_ha_data_internal+1472]
        vmovups   xmm0, XMMWORD PTR [__svml_datan_ha_data_internal+1536]
        vpsubd    xmm4, xmm3, xmm10
        vmovups   xmm13, XMMWORD PTR [__svml_datan_ha_data_internal+1600]
        vpsubd    xmm12, xmm13, xmm10
        vpsubd    xmm10, xmm0, xmm10
        vpsrad    xmm12, xmm12, 31
        vpsrad    xmm13, xmm4, 31
        vpsrad    xmm10, xmm10, 31
        vpaddd    xmm12, xmm7, xmm12
        vpaddd    xmm13, xmm13, xmm10
        vpaddd    xmm7, xmm12, xmm13
        vpaddd    xmm3, xmm7, XMMWORD PTR [__svml_datan_ha_data_internal+1664]
        vpslld    xmm13, xmm3, 5
        vmovd     ecx, xmm13
        vmovupd   ymm6, YMMWORD PTR [__svml_datan_ha_data_internal+1728]
        vmovq     xmm4, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+rdx+rcx]
        vmovq     xmm10, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+rdx+rcx]
        vpextrd   r9d, xmm13, 2
        vpextrd   eax, xmm13, 1
        vpextrd   r10d, xmm13, 3
        vmovq     xmm12, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+rdx+r9]
        vmovhpd   xmm0, xmm4, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+rdx+rax]
        vmovhpd   xmm7, xmm12, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+rdx+r10]
        vmovq     xmm13, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+rdx+r9]
        vmovhpd   xmm3, xmm10, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+rdx+rax]
        vmovhpd   xmm12, xmm13, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+rdx+r10]
        vinsertf128 ymm4, ymm0, xmm7, 1
        vmovq     xmm7, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+rdx+rcx]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+rdx+r9]
        vmovhpd   xmm10, xmm7, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+rdx+rax]
        vmovhpd   xmm13, xmm0, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+rdx+r10]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+rdx+r9]
        vinsertf128 ymm3, ymm3, xmm12, 1
        vmovq     xmm12, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+rdx+rcx]
        vmovhpd   xmm7, xmm12, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+rdx+rax]
        vmovhpd   xmm12, xmm0, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+rdx+r10]
        vinsertf128 ymm13, ymm10, xmm13, 1
        vandpd    ymm10, ymm3, ymm1
        vmulpd    ymm1, ymm1, ymm4
        vinsertf128 ymm12, ymm7, xmm12, 1
        vsubpd    ymm7, ymm10, ymm4
        vmulpd    ymm4, ymm4, ymm2
        vandpd    ymm10, ymm3, ymm2
        vsubpd    ymm0, ymm7, ymm10
        vandpd    ymm3, ymm3, ymm6
        vaddpd    ymm3, ymm3, ymm1
        vsubpd    ymm7, ymm0, ymm7
        vaddpd    ymm2, ymm3, ymm4
        vsubpd    ymm7, ymm10, ymm7
        vsubpd    ymm1, ymm3, ymm2
        vandpd    ymm10, ymm0, ymm5
        vsubpd    ymm0, ymm0, ymm10
        vaddpd    ymm3, ymm4, ymm1
        vaddpd    ymm7, ymm7, ymm0
        vandpd    ymm0, ymm2, ymm5
        vsubpd    ymm5, ymm2, ymm0
        vaddpd    ymm1, ymm3, ymm5
        vcvtpd2ps xmm5, ymm0
        vrcpps    xmm2, xmm5
        vcvtps2pd ymm3, xmm2
        vmulpd    ymm4, ymm0, ymm3
        vmulpd    ymm0, ymm3, ymm1
        vsubpd    ymm6, ymm4, ymm6
        vaddpd    ymm4, ymm6, ymm0
        vmulpd    ymm1, ymm4, ymm4
        vsubpd    ymm5, ymm1, ymm4
        vmulpd    ymm6, ymm4, ymm5
        vaddpd    ymm2, ymm4, ymm6
        vmulpd    ymm0, ymm4, ymm2
        vsubpd    ymm1, ymm0, ymm4
        vmulpd    ymm5, ymm4, ymm1
        vaddpd    ymm6, ymm4, ymm5
        vmulpd    ymm2, ymm4, ymm6
        vsubpd    ymm4, ymm2, ymm4
        vmulpd    ymm5, ymm3, ymm4
        vmulpd    ymm6, ymm10, ymm5
        vmulpd    ymm2, ymm7, ymm5
        vmulpd    ymm7, ymm3, ymm7
        vmulpd    ymm5, ymm10, ymm3
        vaddpd    ymm0, ymm6, ymm2
        vaddpd    ymm2, ymm0, ymm7
        vaddpd    ymm6, ymm2, ymm5
        vaddpd    ymm12, ymm12, ymm2
        vmulpd    ymm7, ymm6, ymm6
        vmulpd    ymm10, ymm7, ymm7
        vmulpd    ymm3, ymm10, YMMWORD PTR [__svml_datan_ha_data_internal+1792]
        vmulpd    ymm0, ymm10, YMMWORD PTR [__svml_datan_ha_data_internal+1856]
        vaddpd    ymm1, ymm3, YMMWORD PTR [__svml_datan_ha_data_internal+1920]
        vaddpd    ymm4, ymm0, YMMWORD PTR [__svml_datan_ha_data_internal+1984]
        vmulpd    ymm3, ymm10, ymm1
        vmulpd    ymm4, ymm10, ymm4
        vaddpd    ymm0, ymm3, YMMWORD PTR [__svml_datan_ha_data_internal+2048]
        vaddpd    ymm3, ymm4, YMMWORD PTR [__svml_datan_ha_data_internal+2112]
        vmulpd    ymm1, ymm10, ymm0
        vmulpd    ymm3, ymm10, ymm3
        vaddpd    ymm0, ymm1, YMMWORD PTR [__svml_datan_ha_data_internal+2176]
        vaddpd    ymm4, ymm3, YMMWORD PTR [__svml_datan_ha_data_internal+2240]
        vmulpd    ymm1, ymm10, ymm0
        vmulpd    ymm4, ymm10, ymm4
        vaddpd    ymm0, ymm1, YMMWORD PTR [__svml_datan_ha_data_internal+2304]
        vaddpd    ymm3, ymm4, YMMWORD PTR [__svml_datan_ha_data_internal+2368]
        vmulpd    ymm1, ymm10, ymm0
        vmulpd    ymm10, ymm10, ymm3
        vaddpd    ymm0, ymm1, YMMWORD PTR [__svml_datan_ha_data_internal+2432]
        vaddpd    ymm1, ymm10, YMMWORD PTR [__svml_datan_ha_data_internal+2496]
        vmulpd    ymm3, ymm7, ymm0
        vaddpd    ymm4, ymm1, ymm3
        vmulpd    ymm7, ymm7, ymm4
        vmulpd    ymm0, ymm6, ymm7
        vaddpd    ymm1, ymm12, ymm0
        vaddpd    ymm2, ymm5, ymm1
        vaddpd    ymm13, ymm13, ymm2
        vorpd     ymm0, ymm13, ymm14
        test      r8d, r8d
        jne       _B3_3

_B3_2::

        vmovups   ymm6, YMMWORD PTR [400+rsp]
        vmovups   ymm7, YMMWORD PTR [368+rsp]
        vmovups   ymm10, YMMWORD PTR [496+rsp]
        vmovups   ymm12, YMMWORD PTR [464+rsp]
        vmovups   ymm13, YMMWORD PTR [432+rsp]
        vmovups   ymm14, YMMWORD PTR [336+rsp]
        vmovups   ymm15, YMMWORD PTR [304+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B3_3::

        vmovupd   YMMWORD PTR [r13], ymm15
        vmovupd   YMMWORD PTR [64+r13], ymm0

_B3_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [96+rsp], ymm8
        vmovups   YMMWORD PTR [64+rsp], ymm9
        vmovups   YMMWORD PTR [32+rsp], ymm11
        mov       QWORD PTR [136+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [128+rsp], rsi
        mov       esi, r8d

_B3_7::

        bt        esi, ebx
        jc        _B3_10

_B3_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B3_7

_B3_9::

        vmovups   ymm8, YMMWORD PTR [96+rsp]
        vmovups   ymm9, YMMWORD PTR [64+rsp]
        vmovups   ymm11, YMMWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [136+rsp]
        mov       rsi, QWORD PTR [128+rsp]
        jmp       _B3_2

_B3_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_datan_ha_cout_rare_internal
        jmp       _B3_8
        ALIGN     16

_B3_11::

__svml_atan4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_atan4_ha_e9_B1_B3:
	DD	1202433
	DD	4379737
	DD	1665105
	DD	1538120
	DD	2074687
	DD	1951798
	DD	1824813
	DD	1435684
	DD	1308699
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_atan4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_atan4_ha_e9_B6_B10:
	DD	665121
	DD	1074214
	DD	1127452
	DD	178196
	DD	301070
	DD	428040
	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_atan4_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_6
	DD	imagerel _B3_11
	DD	imagerel _unwind___svml_atan4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_atan8_ha_z0

__svml_atan8_ha_z0	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L62::

        vmovups   zmm26, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+128]
        vmovups   zmm27, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+320]
        vmovups   zmm28, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+64]
        vmovups   zmm5, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+256]
        vmovups   zmm1, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+192]
        vandpd    zmm25, zmm0, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512]
        vcmppd    k1, zmm25, zmm26, 17 {sae}
        vminpd    zmm24, zmm27, zmm25 {sae}
        vmovups   zmm26, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+960]
        vmovups   zmm27, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+448]
        vreducepd zmm1{k1}, zmm25, 40 {sae}
        vaddpd    zmm23, zmm25, zmm28 {rn-sae}
        knotw     k3, k1
        vsubpd    zmm29, zmm23, zmm28 {rn-sae}
        vpermt2pd zmm27, zmm23, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+512]
        vmovups   zmm28, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+576]
        vfmadd213pd zmm24{k1}, zmm29, zmm5 {rn-sae}
        vcmppd    k2, zmm23, zmm26, 29 {sae}
        vmovups   zmm26, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+1344]
        vrcp14pd  zmm4, zmm24
        vsubpd    zmm2, zmm24, zmm5 {rn-sae}
        vpermt2pd zmm28, zmm23, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+640]
        vfmsub231pd zmm2, zmm29, zmm25 {rn-sae}
        vmovups   zmm29, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+704]
        vmovaps   zmm30, zmm24
        vfnmadd213pd zmm30, zmm4, zmm5 {rn-sae}
        vpermt2pd zmm29, zmm23, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+768]
        vmulpd    zmm31, zmm30, zmm30 {rn-sae}
        vfmadd213pd zmm4, zmm30, zmm4 {rn-sae}
        vmovups   zmm30, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+832]
        vfmadd213pd zmm4, zmm31, zmm4 {rn-sae}
        vpermt2pd zmm30, zmm23, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+896]
        vblendmpd zmm23{k2}, zmm27, zmm28
        vmulpd    zmm22, zmm4, zmm1 {rn-sae}
        vmulpd    zmm3, zmm2, zmm4 {rn-sae}
        vfnmadd213pd zmm24, zmm4, zmm5 {rn-sae}
        vmovups   zmm2, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+1152]
        vmovups   zmm5, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+1216]
        vfmsub213pd zmm1, zmm4, zmm22 {rn-sae}
        vmulpd    zmm4, zmm22, zmm22 {rn-sae}
        vfmadd213pd zmm24, zmm22, zmm1 {rn-sae}
        vmovups   zmm1, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+1280]
        vblendmpd zmm31{k2}, zmm29, zmm30
        vfmadd231pd zmm5, zmm2, zmm4 {rn-sae}
        vfnmadd231pd zmm24{k1}, zmm3, zmm22 {rn-sae}
        vfmadd231pd zmm26, zmm1, zmm4 {rn-sae}
        vmulpd    zmm1, zmm4, zmm4 {rn-sae}
        vmulpd    zmm28, zmm4, zmm22 {rn-sae}
        vblendmpd zmm3{k3}, zmm23, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+1024]
        vblendmpd zmm23{k3}, zmm31, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+1088]
        vfmadd213pd zmm5, zmm1, zmm26 {rn-sae}
        vaddpd    zmm27, zmm24, zmm23 {rn-sae}
        vaddpd    zmm2, zmm3, zmm22 {rn-sae}
        vmovups   zmm24, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+1408]
        vmovups   zmm23, ZMMWORD PTR [__svml_datan_ha_data_internal_avx512+1472]
        vsubpd    zmm3, zmm2, zmm3 {rn-sae}
        vfmadd213pd zmm4, zmm24, zmm23 {rn-sae}
        vsubpd    zmm22, zmm22, zmm3 {rn-sae}
        vfmadd213pd zmm5, zmm1, zmm4 {rn-sae}
        vaddpd    zmm1, zmm27, zmm22 {rn-sae}
        vfmadd213pd zmm5, zmm28, zmm1 {rn-sae}
        vaddpd    zmm2, zmm5, zmm2 {rn-sae}
        vpternlogq zmm0, zmm25, zmm2, 150
        ret
        ALIGN     16

_B4_2::

__svml_atan8_ha_z0 ENDP

_TEXT	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_atan2_ha_ex

__svml_atan2_ha_ex	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L63::

        sub       rsp, 344
        lea       r8, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [288+rsp], xmm15
        movups    XMMWORD PTR [192+rsp], xmm12
        movups    XMMWORD PTR [208+rsp], xmm11
        movups    XMMWORD PTR [304+rsp], xmm10
        movups    XMMWORD PTR [224+rsp], xmm9
        movups    XMMWORD PTR [240+rsp], xmm8
        movaps    xmm8, xmm0
        movups    XMMWORD PTR [256+rsp], xmm7
        movups    XMMWORD PTR [272+rsp], xmm6
        mov       QWORD PTR [320+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movups    xmm4, XMMWORD PTR [__svml_datan_ha_data_internal+1152]
        and       r13, -64
        andps     xmm4, xmm8
        pshufd    xmm0, xmm4, 221
        movaps    xmm9, xmm4
        movq      xmm1, QWORD PTR [__svml_datan_ha_data_internal+2560]
        movdqa    xmm3, xmm0
        psubd     xmm3, xmm1
        movaps    xmm12, xmm4
        movq      xmm2, QWORD PTR [__svml_datan_ha_data_internal+2624]
        movdqa    xmm5, xmm3
        movups    xmm10, XMMWORD PTR [__svml_datan_ha_data_internal+1344]
        pcmpgtd   xmm5, xmm2
        movaps    xmm11, xmm10
        pcmpeqd   xmm3, xmm2
        movq      xmm2, QWORD PTR [__svml_datan_ha_data_internal+1472]
        andps     xmm11, xmm4
        movq      xmm1, QWORD PTR [__svml_datan_ha_data_internal+1536]
        por       xmm5, xmm3
        movq      xmm4, QWORD PTR [__svml_datan_ha_data_internal+1600]
        psubd     xmm2, xmm0
        movq      xmm3, QWORD PTR [__svml_datan_ha_data_internal+1408]
        psubd     xmm4, xmm0
        psubd     xmm3, xmm0
        psubd     xmm1, xmm0
        psrad     xmm3, 31
        psrad     xmm4, 31
        psrad     xmm2, 31
        psrad     xmm1, 31
        paddd     xmm3, xmm4
        paddd     xmm2, xmm1
        movq      xmm15, QWORD PTR [__svml_datan_ha_data_internal+1664]
        paddd     xmm3, xmm2
        paddd     xmm3, xmm15
        movaps    xmm1, xmm11
        pslld     xmm3, 5
        pxor      xmm9, xmm8
        movd      edx, xmm3
        pshufd    xmm0, xmm3, 1
        movups    xmm7, XMMWORD PTR [__svml_datan_ha_data_internal+1728]
        movd      ecx, xmm0
        movq      xmm6, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+r8+rdx]
        movq      xmm3, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+r8+rdx]
        movhpd    xmm6, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+r8+rcx]
        subpd     xmm12, xmm11
        mulpd     xmm11, xmm6
        movmskps  eax, xmm5
        movhpd    xmm3, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+r8+rcx]
        movaps    xmm2, xmm12
        andps     xmm1, xmm3
        andps     xmm2, xmm3
        subpd     xmm1, xmm6
        mulpd     xmm6, xmm12
        andps     xmm3, xmm7
        movaps    xmm5, xmm1
        addpd     xmm3, xmm11
        subpd     xmm5, xmm2
        movaps    xmm11, xmm3
        movaps    xmm15, xmm5
        addpd     xmm11, xmm6
        subpd     xmm15, xmm1
        subpd     xmm3, xmm11
        subpd     xmm2, xmm15
        addpd     xmm6, xmm3
        movaps    xmm1, xmm10
        andps     xmm10, xmm11
        cvtpd2ps  xmm12, xmm10
        subpd     xmm11, xmm10
        andps     xmm1, xmm5
        and       eax, 3
        subpd     xmm5, xmm1
        addpd     xmm6, xmm11
        addpd     xmm2, xmm5
        movlhps   xmm12, xmm12
        rcpps     xmm5, xmm12
        cvtps2pd  xmm3, xmm5
        mulpd     xmm10, xmm3
        mulpd     xmm6, xmm3
        subpd     xmm10, xmm7
        addpd     xmm10, xmm6
        movaps    xmm6, xmm10
        movaps    xmm5, xmm1
        mulpd     xmm6, xmm10
        mulpd     xmm1, xmm3
        subpd     xmm6, xmm10
        mulpd     xmm6, xmm10
        addpd     xmm6, xmm10
        mulpd     xmm6, xmm10
        subpd     xmm6, xmm10
        mulpd     xmm6, xmm10
        addpd     xmm6, xmm10
        mulpd     xmm6, xmm10
        subpd     xmm6, xmm10
        mulpd     xmm6, xmm3
        mulpd     xmm5, xmm6
        mulpd     xmm6, xmm2
        mulpd     xmm2, xmm3
        addpd     xmm5, xmm6
        addpd     xmm5, xmm2
        movaps    xmm6, xmm5
        addpd     xmm6, xmm1
        movaps    xmm3, xmm6
        mulpd     xmm3, xmm6
        movaps    xmm2, xmm3
        mulpd     xmm2, xmm3
        movups    xmm10, XMMWORD PTR [__svml_datan_ha_data_internal+1792]
        mulpd     xmm10, xmm2
        movups    xmm7, XMMWORD PTR [__svml_datan_ha_data_internal+1856]
        addpd     xmm10, XMMWORD PTR [__svml_datan_ha_data_internal+1920]
        mulpd     xmm7, xmm2
        mulpd     xmm10, xmm2
        addpd     xmm7, XMMWORD PTR [__svml_datan_ha_data_internal+1984]
        addpd     xmm10, XMMWORD PTR [__svml_datan_ha_data_internal+2048]
        mulpd     xmm7, xmm2
        mulpd     xmm10, xmm2
        addpd     xmm7, XMMWORD PTR [__svml_datan_ha_data_internal+2112]
        addpd     xmm10, XMMWORD PTR [__svml_datan_ha_data_internal+2176]
        mulpd     xmm7, xmm2
        mulpd     xmm10, xmm2
        addpd     xmm7, XMMWORD PTR [__svml_datan_ha_data_internal+2240]
        addpd     xmm10, XMMWORD PTR [__svml_datan_ha_data_internal+2304]
        mulpd     xmm7, xmm2
        mulpd     xmm10, xmm2
        addpd     xmm7, XMMWORD PTR [__svml_datan_ha_data_internal+2368]
        addpd     xmm10, XMMWORD PTR [__svml_datan_ha_data_internal+2432]
        mulpd     xmm2, xmm7
        mulpd     xmm10, xmm3
        addpd     xmm2, XMMWORD PTR [__svml_datan_ha_data_internal+2496]
        addpd     xmm2, xmm10
        mulpd     xmm3, xmm2
        mulpd     xmm6, xmm3
        movq      xmm4, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+r8+rdx]
        movhpd    xmm4, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+r8+rcx]
        addpd     xmm4, xmm5
        addpd     xmm4, xmm6
        addpd     xmm1, xmm4
        movq      xmm0, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+r8+rdx]
        movhpd    xmm0, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+r8+rcx]
        addpd     xmm0, xmm1
        mov       QWORD PTR [328+rsp], r13
        orps      xmm0, xmm9
        jne       _B5_3

_B5_2::

        movups    xmm6, XMMWORD PTR [272+rsp]
        movups    xmm7, XMMWORD PTR [256+rsp]
        movups    xmm8, XMMWORD PTR [240+rsp]
        movups    xmm9, XMMWORD PTR [224+rsp]
        movups    xmm10, XMMWORD PTR [304+rsp]
        movups    xmm11, XMMWORD PTR [208+rsp]
        movups    xmm12, XMMWORD PTR [192+rsp]
        movups    xmm15, XMMWORD PTR [288+rsp]
        mov       r13, QWORD PTR [320+rsp]
        add       rsp, 344
        ret

_B5_3::

        movups    XMMWORD PTR [r13], xmm8
        movups    XMMWORD PTR [64+r13], xmm0
        je        _B5_2

_B5_6::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

_B5_7::

        mov       ecx, ebx
        mov       edx, 1
        shl       edx, cl
        test      esi, edx
        jne       _B5_10

_B5_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B5_7

_B5_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [64+r13]
        jmp       _B5_2

_B5_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_datan_ha_cout_rare_internal
        jmp       _B5_8
        ALIGN     16

_B5_11::

__svml_atan2_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_atan2_ha_ex_B1_B3:
	DD	1336321
	DD	2675812
	DD	1140828
	DD	1079380
	DD	1017928
	DD	956479
	DD	1288246
	DD	899117
	DD	837668
	DD	1243163
	DD	2818315

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_atan2_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_atan2_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_atan2_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_6
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_atan2_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_atan1_ha_l9

__svml_atan1_ha_l9	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L86::

        sub       rsp, 232
        mov       eax, -2144337920
        vmovups   XMMWORD PTR [192+rsp], xmm11
        mov       edx, -36700160
        vmovups   XMMWORD PTR [176+rsp], xmm10
        vmovapd   xmm3, xmm0
        vmovups   XMMWORD PTR [208+rsp], xmm9
        mov       r8d, 1072037888
        mov       QWORD PTR [168+rsp], r13
        vmovd     xmm1, eax
        vmovsd    xmm2, QWORD PTR [__svml_datan_ha_data_internal+1152]
        mov       ecx, 1071382528
        vandpd    xmm0, xmm3, xmm2
        mov       r9d, 1072889856
        vpshufd   xmm9, xmm0, 85
        vmovd     xmm4, edx
        vpsubd    xmm5, xmm9, xmm1
        mov       r10d, 1073971200
        vpcmpgtd  xmm1, xmm5, xmm4
        vpcmpeqd  xmm5, xmm5, xmm4
        vpor      xmm10, xmm1, xmm5
        vmovd     xmm5, r8d
        vmovmskps eax, xmm10
        vmovd     xmm11, ecx
        vpsubd    xmm10, xmm5, xmm9
        vmovd     xmm5, r9d
        vpsubd    xmm4, xmm11, xmm9
        vpsrad    xmm11, xmm10, 31
        vpsubd    xmm10, xmm5, xmm9
        vmovd     xmm5, r10d
        mov       r11d, 4
        vpsubd    xmm9, xmm5, xmm9
        vpsrad    xmm4, xmm4, 31
        vpsrad    xmm10, xmm10, 31
        vpsrad    xmm9, xmm9, 31
        vpaddd    xmm4, xmm4, xmm11
        vpaddd    xmm5, xmm10, xmm9
        lea       rcx, QWORD PTR [__ImageBase]
        vpaddd    xmm10, xmm4, xmm5
        vmovd     xmm11, r11d
        vpaddd    xmm4, xmm10, xmm11
        vxorpd    xmm2, xmm3, xmm0
        vpslld    xmm9, xmm4, 5
        lea       r13, QWORD PTR [95+rsp]
        vmovd     edx, xmm9
        and       r13, -64
        vmovsd    xmm1, QWORD PTR [__svml_datan_ha_data_internal+1728]
        vmovq     xmm10, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+rcx+rdx]
        vmovq     xmm9, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+rcx+rdx]
        vandpd    xmm5, xmm10, xmm0
        vandpd    xmm11, xmm10, xmm1
        vsubsd    xmm5, xmm5, xmm9
        vfmadd213sd xmm9, xmm0, xmm11
        vcvtpd2ps xmm0, xmm9
        vmovlhps  xmm4, xmm0, xmm0
        vmovaps   xmm10, xmm9
        vrcpps    xmm0, xmm4
        vcvtps2pd xmm0, xmm0
        vfnmadd213sd xmm10, xmm0, xmm1
        vmovaps   xmm11, xmm9
        vfmadd213sd xmm10, xmm10, xmm10
        vfmadd213sd xmm0, xmm10, xmm0
        vfnmadd213sd xmm11, xmm0, xmm1
        vfmadd213sd xmm0, xmm11, xmm0
        vmulsd    xmm4, xmm0, xmm5
        vfnmadd213sd xmm9, xmm4, xmm5
        vmovsd    xmm5, QWORD PTR [__svml_datan_ha_data_internal+1856]
        vmulsd    xmm10, xmm9, xmm0
        vmulsd    xmm0, xmm4, xmm4
        vmulsd    xmm1, xmm0, xmm0
        vmovsd    xmm9, QWORD PTR [__svml_datan_ha_data_internal+1792]
        vfmadd213sd xmm9, xmm1, QWORD PTR [__svml_datan_ha_data_internal+1920]
        vfmadd213sd xmm5, xmm1, QWORD PTR [__svml_datan_ha_data_internal+1984]
        vfmadd213sd xmm9, xmm1, QWORD PTR [__svml_datan_ha_data_internal+2048]
        vfmadd213sd xmm5, xmm1, QWORD PTR [__svml_datan_ha_data_internal+2112]
        vfmadd213sd xmm9, xmm1, QWORD PTR [__svml_datan_ha_data_internal+2176]
        vfmadd213sd xmm5, xmm1, QWORD PTR [__svml_datan_ha_data_internal+2240]
        vfmadd213sd xmm9, xmm1, QWORD PTR [__svml_datan_ha_data_internal+2304]
        vfmadd213sd xmm5, xmm1, QWORD PTR [__svml_datan_ha_data_internal+2368]
        vfmadd213sd xmm9, xmm1, QWORD PTR [__svml_datan_ha_data_internal+2432]
        vfmadd213sd xmm5, xmm1, QWORD PTR [__svml_datan_ha_data_internal+2496]
        vaddsd    xmm1, xmm10, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+rcx+rdx]
        vfmadd213sd xmm9, xmm0, xmm5
        vmulsd    xmm5, xmm9, xmm0
        vfmadd213sd xmm5, xmm4, xmm1
        mov       QWORD PTR [224+rsp], r13
        vaddsd    xmm4, xmm4, xmm5
        vaddsd    xmm9, xmm4, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+rcx+rdx]
        vorpd     xmm0, xmm9, xmm2
        and       eax, 1
        jne       _B6_3

_B6_2::

        vmovups   xmm9, XMMWORD PTR [208+rsp]
        vmovups   xmm10, XMMWORD PTR [176+rsp]
        vmovups   xmm11, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 232
        ret

_B6_3::

        vmovsd    QWORD PTR [r13], xmm3
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B6_6

_B6_4::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B6_2

_B6_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_datan_ha_cout_rare_internal
        jmp       _B6_4
        ALIGN     16

_B6_7::

__svml_atan1_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_atan1_ha_l9_B1_B6:
	DD	672257
	DD	1430594
	DD	890932
	DD	763943
	DD	833561
	DD	1900811

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_atan1_ha_l9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_atan2_ha_l9

__svml_atan2_ha_l9	PROC	EXPORT

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L95::

        sub       rsp, 280
        vmovapd   xmm2, xmm0
        vmovups   XMMWORD PTR [192+rsp], xmm15
        lea       r8, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [208+rsp], xmm14
        vmovups   XMMWORD PTR [224+rsp], xmm13
        vmovups   XMMWORD PTR [240+rsp], xmm11
        mov       QWORD PTR [256+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vandpd    xmm0, xmm2, XMMWORD PTR [__svml_datan_ha_data_internal+1152]
        and       r13, -64
        vpshufd   xmm5, xmm0, 221
        vxorpd    xmm1, xmm2, xmm0
        vmovq     xmm3, QWORD PTR [__svml_datan_ha_data_internal+2560]
        vpsubd    xmm13, xmm5, xmm3
        vmovq     xmm3, QWORD PTR [__svml_datan_ha_data_internal+2624]
        vpcmpgtd  xmm11, xmm13, xmm3
        vpcmpeqd  xmm14, xmm13, xmm3
        vpor      xmm4, xmm11, xmm14
        vmovmskps edx, xmm4
        vmovq     xmm13, QWORD PTR [__svml_datan_ha_data_internal+1408]
        vmovq     xmm4, QWORD PTR [__svml_datan_ha_data_internal+1472]
        vpsubd    xmm13, xmm13, xmm5
        vmovq     xmm15, QWORD PTR [__svml_datan_ha_data_internal+1536]
        vpsubd    xmm4, xmm4, xmm5
        vmovq     xmm11, QWORD PTR [__svml_datan_ha_data_internal+1600]
        vpsrad    xmm13, xmm13, 31
        vpsubd    xmm11, xmm11, xmm5
        vpsubd    xmm5, xmm15, xmm5
        vpsrad    xmm11, xmm11, 31
        vpsrad    xmm4, xmm4, 31
        vpsrad    xmm5, xmm5, 31
        vpaddd    xmm13, xmm13, xmm11
        vpaddd    xmm11, xmm4, xmm5
        vmovq     xmm14, QWORD PTR [__svml_datan_ha_data_internal+1664]
        vpaddd    xmm4, xmm13, xmm11
        vpaddd    xmm14, xmm4, xmm14
        vpslld    xmm15, xmm14, 5
        vmovd     eax, xmm15
        vmovupd   xmm3, XMMWORD PTR [__svml_datan_ha_data_internal+1728]
        vpextrd   ecx, xmm15, 1
        vmovq     xmm11, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+r8+rax]
        vmovq     xmm13, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+r8+rax]
        vmovq     xmm5, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+r8+rax]
        vmovhpd   xmm15, xmm11, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+r8+rcx]
        vmovhpd   xmm4, xmm13, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+r8+rcx]
        vmovhpd   xmm13, xmm5, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+r8+rcx]
        vandpd    xmm5, xmm15, xmm0
        vmovq     xmm14, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+r8+rax]
        vmovhpd   xmm11, xmm14, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+r8+rcx]
        vsubpd    xmm14, xmm5, xmm4
        vandpd    xmm5, xmm15, xmm3
        vfmadd231pd xmm5, xmm0, xmm4
        vcvtpd2ps xmm0, xmm5
        vmovlhps  xmm0, xmm0, xmm0
        vrcpps    xmm4, xmm0
        vcvtps2pd xmm15, xmm4
        vmovapd   xmm0, xmm15
        vfnmadd213pd xmm0, xmm5, xmm3
        vfmadd213pd xmm0, xmm0, xmm0
        vfmadd213pd xmm15, xmm0, xmm15
        vfnmadd231pd xmm3, xmm15, xmm5
        vfmadd213pd xmm15, xmm3, xmm15
        vmulpd    xmm0, xmm14, xmm15
        vfnmadd213pd xmm5, xmm0, xmm14
        vmulpd    xmm14, xmm0, xmm0
        vmulpd    xmm3, xmm15, xmm5
        vmulpd    xmm4, xmm14, xmm14
        vaddpd    xmm3, xmm11, xmm3
        vmovupd   xmm15, XMMWORD PTR [__svml_datan_ha_data_internal+1792]
        vmovupd   xmm5, XMMWORD PTR [__svml_datan_ha_data_internal+1856]
        vfmadd213pd xmm15, xmm4, XMMWORD PTR [__svml_datan_ha_data_internal+1920]
        vfmadd213pd xmm5, xmm4, XMMWORD PTR [__svml_datan_ha_data_internal+1984]
        vfmadd213pd xmm15, xmm4, XMMWORD PTR [__svml_datan_ha_data_internal+2048]
        vfmadd213pd xmm5, xmm4, XMMWORD PTR [__svml_datan_ha_data_internal+2112]
        vfmadd213pd xmm15, xmm4, XMMWORD PTR [__svml_datan_ha_data_internal+2176]
        vfmadd213pd xmm5, xmm4, XMMWORD PTR [__svml_datan_ha_data_internal+2240]
        vfmadd213pd xmm15, xmm4, XMMWORD PTR [__svml_datan_ha_data_internal+2304]
        vfmadd213pd xmm5, xmm4, XMMWORD PTR [__svml_datan_ha_data_internal+2368]
        vfmadd213pd xmm15, xmm4, XMMWORD PTR [__svml_datan_ha_data_internal+2432]
        vfmadd213pd xmm5, xmm4, XMMWORD PTR [__svml_datan_ha_data_internal+2496]
        vfmadd213pd xmm15, xmm14, xmm5
        vmulpd    xmm4, xmm14, xmm15
        vfmadd213pd xmm4, xmm0, xmm3
        vaddpd    xmm0, xmm0, xmm4
        vaddpd    xmm3, xmm13, xmm0
        mov       QWORD PTR [264+rsp], r13
        vorpd     xmm0, xmm3, xmm1
        and       edx, 3
        jne       _B7_3

_B7_2::

        vmovups   xmm11, XMMWORD PTR [240+rsp]
        vmovups   xmm13, XMMWORD PTR [224+rsp]
        vmovups   xmm14, XMMWORD PTR [208+rsp]
        vmovups   xmm15, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [256+rsp]
        add       rsp, 280
        ret

_B7_3::

        vmovupd   XMMWORD PTR [r13], xmm2
        vmovupd   XMMWORD PTR [64+r13], xmm0
        je        _B7_2

_B7_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B7_7::

        bt        esi, ebx
        jc        _B7_10

_B7_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B7_7

_B7_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B7_2

_B7_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_datan_ha_cout_rare_internal
        jmp       _B7_8
        ALIGN     16

_B7_11::

__svml_atan2_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_atan2_ha_l9_B1_B3:
	DD	803329
	DD	2151490
	DD	1030202
	DD	972849
	DD	911400
	DD	849944
	DD	2294027

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_6
	DD	imagerel _unwind___svml_atan2_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_atan2_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B7_1
	DD	imagerel _B7_6
	DD	imagerel _unwind___svml_atan2_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_6
	DD	imagerel _B7_11
	DD	imagerel _unwind___svml_atan2_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST7:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_atan1_ha_ex

__svml_atan1_ha_ex	PROC	EXPORT

_B8_1::

        DB        243
        DB        15
        DB        30
        DB        250
L110::

        sub       rsp, 280
        mov       eax, -2144337920
        movups    XMMWORD PTR [208+rsp], xmm15
        mov       edx, -36700160
        movups    XMMWORD PTR [224+rsp], xmm14
        mov       ecx, 1071382528
        movups    XMMWORD PTR [240+rsp], xmm13
        mov       r8d, 1072037888
        movups    XMMWORD PTR [256+rsp], xmm12
        movd      xmm5, eax
        movups    XMMWORD PTR [176+rsp], xmm7
        mov       r9d, 1072889856
        movups    XMMWORD PTR [192+rsp], xmm6
        mov       r10d, 1073971200
        mov       QWORD PTR [168+rsp], r13
        movd      xmm3, edx
        movsd     xmm4, QWORD PTR [__svml_datan_ha_data_internal+1152]
        movd      xmm7, r8d
        movsd     xmm1, QWORD PTR [__svml_datan_ha_data_internal+1344]
        andps     xmm4, xmm0
        pshufd    xmm13, xmm4, 85
        movaps    xmm14, xmm1
        movaps    xmm2, xmm4
        andps     xmm14, xmm4
        movaps    xmm15, xmm4
        movdqa    xmm4, xmm13
        psubd     xmm4, xmm5
        movd      xmm6, r10d
        movdqa    xmm12, xmm4
        pcmpeqd   xmm4, xmm3
        pcmpgtd   xmm12, xmm3
        mov       r11d, 4
        por       xmm12, xmm4
        movd      xmm4, r9d
        movmskps  eax, xmm12
        movd      xmm12, ecx
        psubd     xmm12, xmm13
        psubd     xmm7, xmm13
        psubd     xmm4, xmm13
        psubd     xmm6, xmm13
        psrad     xmm12, 31
        psrad     xmm7, 31
        psrad     xmm4, 31
        psrad     xmm6, 31
        paddd     xmm12, xmm7
        paddd     xmm4, xmm6
        paddd     xmm12, xmm4
        movd      xmm5, r11d
        paddd     xmm12, xmm5
        lea       rcx, QWORD PTR [__ImageBase]
        pslld     xmm12, 5
        lea       r13, QWORD PTR [95+rsp]
        movd      edx, xmm12
        pxor      xmm2, xmm0
        movsd     xmm3, QWORD PTR [__svml_datan_ha_data_internal+1728]
        subsd     xmm15, xmm14
        movq      xmm6, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+rcx+rdx]
        and       r13, -64
        movdqa    xmm13, xmm6
        movdqa    xmm4, xmm6
        movq      xmm7, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+rcx+rdx]
        andps     xmm13, xmm14
        andps     xmm4, xmm15
        andps     xmm6, xmm3
        mov       QWORD PTR [272+rsp], r13
        and       eax, 1
        subsd     xmm13, xmm7
        movaps    xmm12, xmm13
        subsd     xmm12, xmm4
        movaps    xmm5, xmm12
        subsd     xmm5, xmm13
        subsd     xmm4, xmm5
        movaps    xmm5, xmm1
        andps     xmm5, xmm12
        subsd     xmm12, xmm5
        addsd     xmm12, xmm4
        movdqa    xmm4, xmm7
        mulsd     xmm4, xmm14
        mulsd     xmm7, xmm15
        addsd     xmm4, xmm6
        movaps    xmm14, xmm4
        addsd     xmm14, xmm7
        andps     xmm1, xmm14
        subsd     xmm4, xmm14
        cvtpd2ps  xmm15, xmm1
        addsd     xmm7, xmm4
        subsd     xmm14, xmm1
        movlhps   xmm15, xmm15
        addsd     xmm7, xmm14
        rcpps     xmm4, xmm15
        cvtps2pd  xmm4, xmm4
        mulsd     xmm1, xmm4
        movaps    xmm13, xmm4
        mulsd     xmm7, xmm4
        mulsd     xmm13, xmm12
        subsd     xmm1, xmm3
        movsd     xmm14, QWORD PTR [__svml_datan_ha_data_internal+1792]
        addsd     xmm7, xmm1
        movaps    xmm3, xmm7
        mulsd     xmm3, xmm7
        subsd     xmm3, xmm7
        mulsd     xmm3, xmm7
        addsd     xmm3, xmm7
        mulsd     xmm3, xmm7
        subsd     xmm3, xmm7
        mulsd     xmm3, xmm7
        addsd     xmm3, xmm7
        mulsd     xmm3, xmm7
        subsd     xmm3, xmm7
        mulsd     xmm3, xmm4
        mulsd     xmm4, xmm5
        movaps    xmm1, xmm3
        movaps    xmm7, xmm4
        mulsd     xmm1, xmm5
        mulsd     xmm3, xmm12
        movsd     xmm12, QWORD PTR [__svml_datan_ha_data_internal+1856]
        addsd     xmm3, xmm1
        addsd     xmm13, xmm3
        addsd     xmm7, xmm13
        addsd     xmm13, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+rcx+rdx]
        movaps    xmm6, xmm7
        mulsd     xmm6, xmm7
        movaps    xmm5, xmm6
        mulsd     xmm5, xmm6
        mulsd     xmm14, xmm5
        mulsd     xmm12, xmm5
        addsd     xmm14, QWORD PTR [__svml_datan_ha_data_internal+1920]
        addsd     xmm12, QWORD PTR [__svml_datan_ha_data_internal+1984]
        mulsd     xmm14, xmm5
        mulsd     xmm12, xmm5
        addsd     xmm14, QWORD PTR [__svml_datan_ha_data_internal+2048]
        addsd     xmm12, QWORD PTR [__svml_datan_ha_data_internal+2112]
        mulsd     xmm14, xmm5
        mulsd     xmm12, xmm5
        addsd     xmm14, QWORD PTR [__svml_datan_ha_data_internal+2176]
        addsd     xmm12, QWORD PTR [__svml_datan_ha_data_internal+2240]
        mulsd     xmm14, xmm5
        mulsd     xmm12, xmm5
        addsd     xmm14, QWORD PTR [__svml_datan_ha_data_internal+2304]
        addsd     xmm12, QWORD PTR [__svml_datan_ha_data_internal+2368]
        mulsd     xmm14, xmm5
        mulsd     xmm12, xmm5
        addsd     xmm14, QWORD PTR [__svml_datan_ha_data_internal+2432]
        addsd     xmm12, QWORD PTR [__svml_datan_ha_data_internal+2496]
        mulsd     xmm14, xmm6
        addsd     xmm14, xmm12
        mulsd     xmm14, xmm6
        mulsd     xmm14, xmm7
        addsd     xmm14, xmm13
        addsd     xmm4, xmm14
        addsd     xmm4, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+rcx+rdx]
        orps      xmm4, xmm2
        jne       _B8_3

_B8_2::

        movups    xmm6, XMMWORD PTR [192+rsp]
        movaps    xmm0, xmm4
        movups    xmm7, XMMWORD PTR [176+rsp]
        movups    xmm12, XMMWORD PTR [256+rsp]
        movups    xmm13, XMMWORD PTR [240+rsp]
        movups    xmm14, XMMWORD PTR [224+rsp]
        movups    xmm15, XMMWORD PTR [208+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 280
        ret

_B8_3::

        movsd     QWORD PTR [r13], xmm0
        movsd     QWORD PTR [64+r13], xmm4
        jne       _B8_6

_B8_4::

        movsd     xmm4, QWORD PTR [64+r13]
        jmp       _B8_2

_B8_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_datan_ha_cout_rare_internal
        jmp       _B8_4
        ALIGN     16

_B8_7::

__svml_atan1_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_atan1_ha_ex_B1_B6:
	DD	1076225
	DD	1430636
	DD	813150
	DD	751696
	DD	1099844
	DD	1038389
	DD	976935
	DD	915481
	DD	2294027

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_1
	DD	imagerel _B8_7
	DD	imagerel _unwind___svml_atan1_ha_ex_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST8:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_atan4_ha_l9

__svml_atan4_ha_l9	PROC	EXPORT

_B9_1::

        DB        243
        DB        15
        DB        30
        DB        250
L125::

        sub       rsp, 552
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [400+rsp], ymm15
        vmovups   YMMWORD PTR [432+rsp], ymm14
        vmovups   YMMWORD PTR [464+rsp], ymm12
        vmovups   YMMWORD PTR [496+rsp], ymm9
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [303+rsp]
        vmovapd   ymm5, ymm0
        and       r13, -64
        vandpd    ymm1, ymm5, YMMWORD PTR [__svml_datan_ha_data_internal+1152]
        vxorpd    ymm4, ymm5, ymm1
        vmovups   xmm9, XMMWORD PTR [__svml_datan_ha_data_internal+2624]
        mov       QWORD PTR [536+rsp], r13
        vextracti128 xmm3, ymm1, 1
        vshufps   xmm14, xmm1, xmm3, 221
        vpsubd    xmm12, xmm14, XMMWORD PTR [__svml_datan_ha_data_internal+2560]
        vpcmpgtd  xmm2, xmm12, xmm9
        vpcmpeqd  xmm0, xmm12, xmm9
        vmovups   xmm3, XMMWORD PTR [__svml_datan_ha_data_internal+1408]
        vpor      xmm15, xmm2, xmm0
        vmovups   xmm9, XMMWORD PTR [__svml_datan_ha_data_internal+1600]
        vpsubd    xmm12, xmm3, xmm14
        vmovmskps r8d, xmm15
        vpsubd    xmm15, xmm9, xmm14
        vmovups   xmm3, XMMWORD PTR [__svml_datan_ha_data_internal+1472]
        vpsrad    xmm2, xmm12, 31
        vpsrad    xmm12, xmm15, 31
        vpsubd    xmm9, xmm3, xmm14
        vmovups   xmm15, XMMWORD PTR [__svml_datan_ha_data_internal+1536]
        vpsrad    xmm3, xmm9, 31
        vpsubd    xmm14, xmm15, xmm14
        vpaddd    xmm2, xmm2, xmm12
        vpsrad    xmm9, xmm14, 31
        vpaddd    xmm3, xmm3, xmm9
        vpaddd    xmm12, xmm2, xmm3
        vpaddd    xmm9, xmm12, XMMWORD PTR [__svml_datan_ha_data_internal+1664]
        vpslld    xmm15, xmm9, 5
        vmovd     ecx, xmm15
        vmovupd   ymm0, YMMWORD PTR [__svml_datan_ha_data_internal+1728]
        vmovq     xmm2, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+rdx+rcx]
        vpextrd   r9d, xmm15, 2
        vpextrd   eax, xmm15, 1
        vpextrd   r10d, xmm15, 3
        vmovq     xmm3, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+rdx+r9]
        vmovhpd   xmm14, xmm2, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+rdx+rax]
        vmovhpd   xmm12, xmm3, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+rdx+r10]
        vmovq     xmm2, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+rdx+rcx]
        vmovq     xmm3, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+rdx+r9]
        vmovhpd   xmm15, xmm2, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+rdx+rax]
        vmovq     xmm2, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+rdx+rcx]
        vinsertf128 ymm9, ymm14, xmm12, 1
        vmovhpd   xmm12, xmm3, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+8+rdx+r10]
        vmovq     xmm3, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+rdx+r9]
        vmovhpd   xmm14, xmm2, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+rdx+rax]
        vmovq     xmm2, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+rdx+rcx]
        vinsertf128 ymm15, ymm15, xmm12, 1
        vmovhpd   xmm12, xmm3, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+16+rdx+r10]
        vinsertf128 ymm3, ymm14, xmm12, 1
        vmovhpd   xmm12, xmm2, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+rdx+rax]
        vmovq     xmm2, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+rdx+r9]
        vmovhpd   xmm14, xmm2, QWORD PTR [imagerel(__svml_datan_ha_data_internal)+24+rdx+r10]
        vinsertf128 ymm2, ymm12, xmm14, 1
        vandpd    ymm12, ymm15, ymm1
        vandpd    ymm15, ymm15, ymm0
        vsubpd    ymm14, ymm12, ymm9
        vfmadd213pd ymm9, ymm1, ymm15
        vcvtpd2ps xmm1, ymm9
        vrcpps    xmm12, xmm1
        vcvtps2pd ymm1, xmm12
        vmovapd   ymm15, ymm0
        vfnmadd231pd ymm15, ymm1, ymm9
        vfmadd213pd ymm15, ymm15, ymm15
        vfmadd213pd ymm1, ymm15, ymm1
        vmovupd   ymm15, YMMWORD PTR [__svml_datan_ha_data_internal+1792]
        vfnmadd231pd ymm0, ymm1, ymm9
        vfmadd213pd ymm1, ymm0, ymm1
        vmulpd    ymm12, ymm14, ymm1
        vfnmadd213pd ymm9, ymm12, ymm14
        vmulpd    ymm14, ymm12, ymm12
        vmulpd    ymm9, ymm1, ymm9
        vmovupd   ymm1, YMMWORD PTR [__svml_datan_ha_data_internal+1856]
        vmulpd    ymm0, ymm14, ymm14
        vaddpd    ymm2, ymm2, ymm9
        vfmadd213pd ymm15, ymm0, YMMWORD PTR [__svml_datan_ha_data_internal+1920]
        vfmadd213pd ymm1, ymm0, YMMWORD PTR [__svml_datan_ha_data_internal+1984]
        vfmadd213pd ymm15, ymm0, YMMWORD PTR [__svml_datan_ha_data_internal+2048]
        vfmadd213pd ymm1, ymm0, YMMWORD PTR [__svml_datan_ha_data_internal+2112]
        vfmadd213pd ymm15, ymm0, YMMWORD PTR [__svml_datan_ha_data_internal+2176]
        vfmadd213pd ymm1, ymm0, YMMWORD PTR [__svml_datan_ha_data_internal+2240]
        vfmadd213pd ymm15, ymm0, YMMWORD PTR [__svml_datan_ha_data_internal+2304]
        vfmadd213pd ymm1, ymm0, YMMWORD PTR [__svml_datan_ha_data_internal+2368]
        vfmadd213pd ymm15, ymm0, YMMWORD PTR [__svml_datan_ha_data_internal+2432]
        vfmadd213pd ymm1, ymm0, YMMWORD PTR [__svml_datan_ha_data_internal+2496]
        vfmadd213pd ymm15, ymm14, ymm1
        vmulpd    ymm0, ymm14, ymm15
        vfmadd213pd ymm0, ymm12, ymm2
        vaddpd    ymm9, ymm12, ymm0
        vaddpd    ymm3, ymm3, ymm9
        vorpd     ymm0, ymm3, ymm4
        test      r8d, r8d
        jne       _B9_3

_B9_2::

        vmovups   ymm9, YMMWORD PTR [496+rsp]
        vmovups   ymm12, YMMWORD PTR [464+rsp]
        vmovups   ymm14, YMMWORD PTR [432+rsp]
        vmovups   ymm15, YMMWORD PTR [400+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B9_3::

        vmovupd   YMMWORD PTR [r13], ymm5
        vmovupd   YMMWORD PTR [64+r13], ymm0

_B9_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [192+rsp], ymm6
        vmovups   YMMWORD PTR [160+rsp], ymm7
        vmovups   YMMWORD PTR [128+rsp], ymm8
        vmovups   YMMWORD PTR [96+rsp], ymm10
        vmovups   YMMWORD PTR [64+rsp], ymm11
        vmovups   YMMWORD PTR [32+rsp], ymm13
        mov       QWORD PTR [232+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [224+rsp], rsi
        mov       esi, r8d

_B9_7::

        bt        esi, ebx
        jc        _B9_10

_B9_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B9_7

_B9_9::

        vmovups   ymm6, YMMWORD PTR [192+rsp]
        vmovups   ymm7, YMMWORD PTR [160+rsp]
        vmovups   ymm8, YMMWORD PTR [128+rsp]
        vmovups   ymm10, YMMWORD PTR [96+rsp]
        vmovups   ymm11, YMMWORD PTR [64+rsp]
        vmovups   ymm13, YMMWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [232+rsp]
        mov       rsi, QWORD PTR [224+rsp]
        jmp       _B9_2

_B9_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_datan_ha_cout_rare_internal
        jmp       _B9_8
        ALIGN     16

_B9_11::

__svml_atan4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_atan4_ha_l9_B1_B3:
	DD	802305
	DD	4379710
	DD	2070582
	DD	1951789
	DD	1828900
	DD	1701915
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_1
	DD	imagerel _B9_6
	DD	imagerel _unwind___svml_atan4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_atan4_ha_l9_B6_B10:
	DD	1065249
	DD	1860673
	DD	1913911
	DD	186415
	DD	309289
	DD	436259
	DD	559133
	DD	686100
	DD	813067
	DD	imagerel _B9_1
	DD	imagerel _B9_6
	DD	imagerel _unwind___svml_atan4_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_6
	DD	imagerel _B9_11
	DD	imagerel _unwind___svml_atan4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST9:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_datan_ha_cout_rare_internal

__svml_datan_ha_cout_rare_internal	PROC

_B10_1::

        DB        243
        DB        15
        DB        30
        DB        250
L152::

        sub       rsp, 88
        mov       r8, rdx
        movzx     eax, WORD PTR [6+rcx]
        and       eax, 32752
        shr       eax, 4
        cmp       eax, 2047
        je        _B10_12

_B10_2::

        mov       r9, QWORD PTR [rcx]
        mov       QWORD PTR [72+rsp], r9
        shr       r9, 56
        mov       dl, BYTE PTR [7+rcx]
        and       r9d, 127
        mov       BYTE PTR [79+rsp], r9b
        movsd     xmm0, QWORD PTR [72+rsp]
        shr       dl, 7
        comisd    xmm0, QWORD PTR [_vmldAtanHATab+1888]
        mov       r9d, DWORD PTR [76+rsp]
        jb        _B10_6

_B10_3::

        movsd     xmm1, QWORD PTR [_vmldAtanHATab+1896]
        comisd    xmm1, xmm0
        jbe       _B10_5

_B10_4::

        mov       r10d, r9d
        and       r9d, -524288
        add       r9d, 262144
        and       r10d, -1048576
        and       r9d, 1048575
        movaps    xmm3, xmm0
        movsd     QWORD PTR [32+rsp], xmm0
        or        r10d, r9d
        mov       DWORD PTR [32+rsp], 0
        lea       r9, QWORD PTR [__ImageBase]
        mov       DWORD PTR [36+rsp], r10d
        mov       r11, r9
        movsd     xmm4, QWORD PTR [_vmldAtanHATab+1928]
        mov       ecx, DWORD PTR [4+rcx]
        shl       eax, 20
        and       ecx, 1048575
        or        eax, ecx
        subsd     xmm3, QWORD PTR [32+rsp]
        shl       dl, 7
        mulsd     xmm4, xmm3
        movsd     QWORD PTR [40+rsp], xmm4
        add       eax, -1069547520
        movsd     xmm2, QWORD PTR [40+rsp]
        sar       eax, 18
        subsd     xmm2, xmm3
        movsd     QWORD PTR [48+rsp], xmm2
        movaps    xmm2, xmm0
        movsd     xmm1, QWORD PTR [40+rsp]
        and       eax, -2
        movsd     xmm4, QWORD PTR [48+rsp]
        mulsd     xmm2, QWORD PTR [_vmldAtanHATab+1928]
        subsd     xmm1, xmm4
        movsd     QWORD PTR [40+rsp], xmm1
        movsd     xmm5, QWORD PTR [40+rsp]
        movsxd    rax, eax
        subsd     xmm3, xmm5
        movsd     QWORD PTR [48+rsp], xmm3
        movsd     xmm4, QWORD PTR [40+rsp]
        movsd     xmm3, QWORD PTR [48+rsp]
        movsd     QWORD PTR [40+rsp], xmm2
        movsd     xmm1, QWORD PTR [40+rsp]
        subsd     xmm1, QWORD PTR [72+rsp]
        movsd     QWORD PTR [48+rsp], xmm1
        movsd     xmm2, QWORD PTR [40+rsp]
        movsd     xmm5, QWORD PTR [48+rsp]
        subsd     xmm2, xmm5
        movsd     QWORD PTR [40+rsp], xmm2
        movsd     xmm1, QWORD PTR [40+rsp]
        subsd     xmm0, xmm1
        movsd     QWORD PTR [48+rsp], xmm0
        movsd     xmm2, QWORD PTR [40+rsp]
        movsd     xmm0, QWORD PTR [32+rsp]
        movsd     xmm5, QWORD PTR [48+rsp]
        mulsd     xmm2, xmm0
        mulsd     xmm5, xmm0
        movaps    xmm1, xmm2
        addsd     xmm1, xmm5
        movsd     QWORD PTR [40+rsp], xmm1
        movsd     xmm0, QWORD PTR [40+rsp]
        subsd     xmm2, xmm0
        addsd     xmm5, xmm2
        movsd     QWORD PTR [48+rsp], xmm5
        movsd     xmm1, QWORD PTR [40+rsp]
        movsd     xmm5, QWORD PTR [_vmldAtanHATab+1904]
        movsd     xmm0, QWORD PTR [48+rsp]
        addsd     xmm5, xmm1
        movsd     QWORD PTR [40+rsp], xmm5
        movsd     xmm2, QWORD PTR [40+rsp]
        movsd     xmm5, QWORD PTR [_vmldAtanHATab+1904]
        subsd     xmm5, xmm2
        movsd     QWORD PTR [48+rsp], xmm5
        movsd     xmm5, QWORD PTR [40+rsp]
        movsd     xmm2, QWORD PTR [48+rsp]
        addsd     xmm5, xmm2
        movsd     QWORD PTR [56+rsp], xmm5
        movsd     xmm2, QWORD PTR [48+rsp]
        addsd     xmm1, xmm2
        movsd     xmm2, QWORD PTR [_vmldAtanHATab+1904]
        movsd     QWORD PTR [48+rsp], xmm1
        movsd     xmm1, QWORD PTR [56+rsp]
        subsd     xmm2, xmm1
        movsd     QWORD PTR [56+rsp], xmm2
        movsd     xmm5, QWORD PTR [48+rsp]
        movsd     xmm1, QWORD PTR [56+rsp]
        addsd     xmm5, xmm1
        movsd     QWORD PTR [56+rsp], xmm5
        movsd     xmm2, QWORD PTR [40+rsp]
        movsd     xmm5, QWORD PTR [_vmldAtanHATab+1928]
        mulsd     xmm5, xmm2
        movsd     xmm1, QWORD PTR [56+rsp]
        movsd     QWORD PTR [40+rsp], xmm5
        addsd     xmm0, xmm1
        movsd     xmm1, QWORD PTR [40+rsp]
        subsd     xmm1, xmm2
        movsd     QWORD PTR [48+rsp], xmm1
        movsd     xmm1, QWORD PTR [40+rsp]
        movsd     xmm5, QWORD PTR [48+rsp]
        subsd     xmm1, xmm5
        movsd     QWORD PTR [40+rsp], xmm1
        movsd     xmm5, QWORD PTR [40+rsp]
        subsd     xmm2, xmm5
        movsd     xmm5, QWORD PTR [_vmldAtanHATab+1928]
        movsd     QWORD PTR [48+rsp], xmm2
        movsd     xmm1, QWORD PTR [40+rsp]
        movsd     xmm2, QWORD PTR [48+rsp]
        addsd     xmm2, xmm0
        movsd     xmm0, QWORD PTR [_vmldAtanHATab+1904]
        divsd     xmm0, xmm1
        mulsd     xmm5, xmm0
        movsd     QWORD PTR [48+rsp], xmm5
        movsd     xmm5, QWORD PTR [48+rsp]
        subsd     xmm5, xmm0
        movsd     QWORD PTR [56+rsp], xmm5
        movsd     xmm5, QWORD PTR [48+rsp]
        movsd     xmm0, QWORD PTR [56+rsp]
        subsd     xmm5, xmm0
        movsd     QWORD PTR [56+rsp], xmm5
        movsd     xmm0, QWORD PTR [56+rsp]
        mulsd     xmm1, xmm0
        movsd     xmm5, QWORD PTR [_vmldAtanHATab+1904]
        subsd     xmm5, xmm1
        movsd     xmm1, QWORD PTR [56+rsp]
        mulsd     xmm2, xmm1
        movsd     QWORD PTR [48+rsp], xmm2
        movsd     xmm2, QWORD PTR [48+rsp]
        subsd     xmm5, xmm2
        movsd     QWORD PTR [48+rsp], xmm5
        movsd     xmm2, QWORD PTR [48+rsp]
        movsd     xmm1, QWORD PTR [56+rsp]
        movsd     xmm5, QWORD PTR [48+rsp]
        movsd     xmm0, QWORD PTR [56+rsp]
        addsd     xmm2, QWORD PTR [_vmldAtanHATab+1904]
        mulsd     xmm2, xmm5
        mulsd     xmm2, xmm0
        movaps    xmm0, xmm1
        mulsd     xmm1, xmm3
        mulsd     xmm0, xmm4
        mulsd     xmm4, xmm2
        movaps    xmm5, xmm2
        mulsd     xmm5, xmm3
        movaps    xmm3, xmm0
        addsd     xmm5, xmm1
        addsd     xmm5, xmm4
        movsd     QWORD PTR [40+rsp], xmm5
        movsd     xmm1, QWORD PTR [40+rsp]
        movsd     xmm5, QWORD PTR [imagerel(_vmldAtanHATab)+r9+rax*8]
        addsd     xmm3, xmm1
        movsd     QWORD PTR [40+rsp], xmm3
        movsd     xmm3, QWORD PTR [40+rsp]
        subsd     xmm0, xmm3
        addsd     xmm0, xmm1
        movsd     xmm1, QWORD PTR [_vmldAtanHATab+1872]
        movsd     QWORD PTR [48+rsp], xmm0
        movaps    xmm0, xmm5
        movsd     xmm3, QWORD PTR [40+rsp]
        movaps    xmm2, xmm3
        addsd     xmm0, xmm3
        mulsd     xmm2, xmm3
        mulsd     xmm1, xmm2
        movsd     xmm4, QWORD PTR [48+rsp]
        movsd     QWORD PTR [40+rsp], xmm0
        movsd     xmm0, QWORD PTR [40+rsp]
        addsd     xmm1, QWORD PTR [_vmldAtanHATab+1864]
        subsd     xmm5, xmm0
        mulsd     xmm1, xmm2
        addsd     xmm5, xmm3
        addsd     xmm1, QWORD PTR [_vmldAtanHATab+1856]
        mulsd     xmm1, xmm2
        movsd     QWORD PTR [48+rsp], xmm5
        movsd     xmm0, QWORD PTR [40+rsp]
        movsd     xmm5, QWORD PTR [48+rsp]
        addsd     xmm1, QWORD PTR [_vmldAtanHATab+1848]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_vmldAtanHATab+1840]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_vmldAtanHATab+1832]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_vmldAtanHATab+1824]
        mulsd     xmm2, xmm1
        mulsd     xmm3, xmm2
        addsd     xmm4, xmm3
        addsd     xmm4, QWORD PTR [imagerel(_vmldAtanHATab)+8+r11+rax*8]
        addsd     xmm5, xmm4
        addsd     xmm0, xmm5
        movsd     QWORD PTR [64+rsp], xmm0
        mov       cl, BYTE PTR [71+rsp]
        and       cl, 127
        or        cl, dl
        mov       BYTE PTR [71+rsp], cl
        mov       rdx, QWORD PTR [64+rsp]
        mov       QWORD PTR [r8], rdx
        jmp       _B10_11

_B10_5::

        movsd     xmm0, QWORD PTR [_vmldAtanHATab+1912]
        shl       dl, 7
        addsd     xmm0, QWORD PTR [_vmldAtanHATab+1920]
        movsd     QWORD PTR [64+rsp], xmm0
        mov       al, BYTE PTR [71+rsp]
        and       al, 127
        or        al, dl
        mov       BYTE PTR [71+rsp], al
        mov       rdx, QWORD PTR [64+rsp]
        mov       QWORD PTR [r8], rdx
        jmp       _B10_11

_B10_6::

        comisd    xmm0, QWORD PTR [_vmldAtanHATab+1880]
        jb        _B10_8

_B10_7::

        movaps    xmm2, xmm0
        mulsd     xmm2, xmm0
        shl       dl, 7
        movsd     xmm1, QWORD PTR [_vmldAtanHATab+1872]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_vmldAtanHATab+1864]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_vmldAtanHATab+1856]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_vmldAtanHATab+1848]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_vmldAtanHATab+1840]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_vmldAtanHATab+1832]
        mulsd     xmm1, xmm2
        addsd     xmm1, QWORD PTR [_vmldAtanHATab+1824]
        mulsd     xmm2, xmm1
        mulsd     xmm2, xmm0
        addsd     xmm2, xmm0
        movsd     QWORD PTR [64+rsp], xmm2
        mov       al, BYTE PTR [71+rsp]
        and       al, 127
        or        al, dl
        mov       BYTE PTR [71+rsp], al
        mov       rdx, QWORD PTR [64+rsp]
        mov       QWORD PTR [r8], rdx
        jmp       _B10_11

_B10_8::

        movzx     eax, WORD PTR [78+rsp]
        test      eax, 32752
        je        _B10_10

_B10_9::

        movsd     xmm1, QWORD PTR [_vmldAtanHATab+1904]
        shl       dl, 7
        addsd     xmm1, xmm0
        movsd     QWORD PTR [40+rsp], xmm1
        movsd     xmm0, QWORD PTR [40+rsp]
        mulsd     xmm0, QWORD PTR [72+rsp]
        movsd     QWORD PTR [64+rsp], xmm0
        mov       al, BYTE PTR [71+rsp]
        and       al, 127
        or        al, dl
        mov       BYTE PTR [71+rsp], al
        mov       rdx, QWORD PTR [64+rsp]
        mov       QWORD PTR [r8], rdx
        jmp       _B10_11

_B10_10::

        mulsd     xmm0, xmm0
        shl       dl, 7
        movsd     QWORD PTR [40+rsp], xmm0
        movsd     xmm0, QWORD PTR [40+rsp]
        addsd     xmm0, QWORD PTR [72+rsp]
        movsd     QWORD PTR [64+rsp], xmm0
        mov       al, BYTE PTR [71+rsp]
        and       al, 127
        or        al, dl
        mov       BYTE PTR [71+rsp], al
        mov       rdx, QWORD PTR [64+rsp]
        mov       QWORD PTR [r8], rdx

_B10_11::

        xor       eax, eax
        add       rsp, 88
        ret

_B10_12::

        test      DWORD PTR [4+rcx], 1048575
        jne       _B10_15

_B10_13::

        cmp       DWORD PTR [rcx], 0
        jne       _B10_15

_B10_14::

        movsd     xmm0, QWORD PTR [_vmldAtanHATab+1912]
        mov       cl, BYTE PTR [7+rcx]
        and       cl, -128
        addsd     xmm0, QWORD PTR [_vmldAtanHATab+1920]
        movsd     QWORD PTR [64+rsp], xmm0
        mov       al, BYTE PTR [71+rsp]
        and       al, 127
        or        al, cl
        mov       BYTE PTR [71+rsp], al
        mov       rdx, QWORD PTR [64+rsp]
        mov       QWORD PTR [r8], rdx
        jmp       _B10_11

_B10_15::

        movsd     xmm0, QWORD PTR [rcx]
        addsd     xmm0, xmm0
        movsd     QWORD PTR [r8], xmm0
        jmp       _B10_11
        ALIGN     16

_B10_16::

__svml_datan_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_datan_ha_cout_rare_internal_B1_B15:
	DD	67585
	DD	41480

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B10_1
	DD	imagerel _B10_16
	DD	imagerel _unwind___svml_datan_ha_cout_rare_internal_B1_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_datan_ha_data_internal_avx512
__svml_datan_ha_data_internal_avx512	DD	4294967295
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
	DD	1125646336
	DD	0
	DD	1125646336
	DD	0
	DD	1125646336
	DD	0
	DD	1125646336
	DD	0
	DD	1125646336
	DD	0
	DD	1125646336
	DD	0
	DD	1125646336
	DD	0
	DD	1125646336
	DD	0
	DD	1075806208
	DD	0
	DD	1075806208
	DD	0
	DD	1075806208
	DD	0
	DD	1075806208
	DD	0
	DD	1075806208
	DD	0
	DD	1075806208
	DD	0
	DD	1075806208
	DD	0
	DD	1075806208
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
	DD	1206910976
	DD	0
	DD	1206910976
	DD	0
	DD	1206910976
	DD	0
	DD	1206910976
	DD	0
	DD	1206910976
	DD	0
	DD	1206910976
	DD	0
	DD	1206910976
	DD	0
	DD	1206910976
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
	DD	0
	DD	4180443357
	DD	1070553973
	DD	90291023
	DD	1071492199
	DD	2737217249
	DD	1071945615
	DD	1413754136
	DD	1072243195
	DD	1468297118
	DD	1072475260
	DD	3531732635
	DD	1072657163
	DD	744202399
	DD	1072747407
	DD	2464923204
	DD	1072805601
	DD	1436891685
	DD	1072853231
	DD	2037009832
	DD	1072892781
	DD	1826698067
	DD	1072926058
	DD	1803191648
	DD	1072954391
	DD	2205372832
	DD	1072978772
	DD	4234512805
	DD	1072999952
	DD	3932628503
	DD	1073018509
	DD	2501811453
	DD	1073034892
	DD	866379431
	DD	1073049455
	DD	1376865888
	DD	1073062480
	DD	3290094269
	DD	1073074195
	DD	354764887
	DD	1073084787
	DD	3332975497
	DD	1073094406
	DD	1141460092
	DD	1073103181
	DD	745761286
	DD	1073111216
	DD	1673304509
	DD	1073118600
	DD	983388243
	DD	1073125409
	DD	3895509104
	DD	1073131706
	DD	2128523669
	DD	1073137548
	DD	2075485693
	DD	1073142981
	DD	121855980
	DD	1073148047
	DD	4181733783
	DD	1073152780
	DD	2887813284
	DD	1073157214
	DD	0
	DD	0
	DD	1022865341
	DD	1013492590
	DD	573531618
	DD	1014639487
	DD	2280825944
	DD	1014120858
	DD	856972295
	DD	1015129638
	DD	986810987
	DD	1015077601
	DD	2062601149
	DD	1013974920
	DD	589036912
	DD	3164328156
	DD	1787331214
	DD	1016798022
	DD	2942272763
	DD	3164235441
	DD	2956702105
	DD	1016472908
	DD	3903328092
	DD	3162582135
	DD	3175026820
	DD	3158589859
	DD	787328196
	DD	1014621351
	DD	2317874517
	DD	3163795518
	DD	4071621134
	DD	1016673529
	DD	2492111345
	DD	3164172103
	DD	3606178875
	DD	3162371821
	DD	3365790232
	DD	1014547152
	DD	2710887773
	DD	1017086651
	DD	2755350986
	DD	3162706257
	DD	198095269
	DD	3162802133
	DD	2791076759
	DD	3164364640
	DD	4214434319
	DD	3162164074
	DD	773754012
	DD	3164190653
	DD	139561443
	DD	3164313657
	DD	2197796619
	DD	3164066219
	DD	3592486882
	DD	1016669082
	DD	1148791015
	DD	3163724934
	DD	386789398
	DD	3163117479
	DD	2518816264
	DD	3162291736
	DD	2545101323
	DD	3164592727
	DD	16
	DD	1125646336
	DD	16
	DD	1125646336
	DD	16
	DD	1125646336
	DD	16
	DD	1125646336
	DD	16
	DD	1125646336
	DD	16
	DD	1125646336
	DD	16
	DD	1125646336
	DD	16
	DD	1125646336
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
	DD	4123328151
	DD	1068689849
	DD	4123328151
	DD	1068689849
	DD	4123328151
	DD	1068689849
	DD	4123328151
	DD	1068689849
	DD	4123328151
	DD	1068689849
	DD	4123328151
	DD	1068689849
	DD	4123328151
	DD	1068689849
	DD	4123328151
	DD	1068689849
	DD	3295121612
	DD	3216458327
	DD	3295121612
	DD	3216458327
	DD	3295121612
	DD	3216458327
	DD	3295121612
	DD	3216458327
	DD	3295121612
	DD	3216458327
	DD	3295121612
	DD	3216458327
	DD	3295121612
	DD	3216458327
	DD	3295121612
	DD	3216458327
	DD	4026078880
	DD	1069314495
	DD	4026078880
	DD	1069314495
	DD	4026078880
	DD	1069314495
	DD	4026078880
	DD	1069314495
	DD	4026078880
	DD	1069314495
	DD	4026078880
	DD	1069314495
	DD	4026078880
	DD	1069314495
	DD	4026078880
	DD	1069314495
	DD	2398029018
	DD	3217180964
	DD	2398029018
	DD	3217180964
	DD	2398029018
	DD	3217180964
	DD	2398029018
	DD	3217180964
	DD	2398029018
	DD	3217180964
	DD	2398029018
	DD	3217180964
	DD	2398029018
	DD	3217180964
	DD	2398029018
	DD	3217180964
	DD	2576905246
	DD	1070176665
	DD	2576905246
	DD	1070176665
	DD	2576905246
	DD	1070176665
	DD	2576905246
	DD	1070176665
	DD	2576905246
	DD	1070176665
	DD	2576905246
	DD	1070176665
	DD	2576905246
	DD	1070176665
	DD	2576905246
	DD	1070176665
	DD	1431655757
	DD	3218429269
	DD	1431655757
	DD	3218429269
	DD	1431655757
	DD	3218429269
	DD	1431655757
	DD	3218429269
	DD	1431655757
	DD	3218429269
	DD	1431655757
	DD	3218429269
	DD	1431655757
	DD	3218429269
	DD	1431655757
	DD	3218429269
	PUBLIC __svml_datan_ha_data_internal
__svml_datan_ha_data_internal	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	1413754136
	DD	1073291771
	DD	856972295
	DD	1016178214
	DD	0
	DD	1073217536
	DD	4294967295
	DD	4294967295
	DD	3531732635
	DD	1072657163
	DD	2062601149
	DD	1013974920
	DD	0
	DD	1072693248
	DD	4294967295
	DD	4294967295
	DD	1413754136
	DD	1072243195
	DD	856972295
	DD	1015129638
	DD	0
	DD	1071644672
	DD	4294967295
	DD	4294967295
	DD	90291023
	DD	1071492199
	DD	573531618
	DD	1014639487
	DD	0
	DD	0
	DD	4294967295
	DD	4294967295
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
	DD	1071382528
	DD	0
	DD	1071382528
	DD	0
	DD	1071382528
	DD	0
	DD	1071382528
	DD	0
	DD	1071382528
	DD	0
	DD	1071382528
	DD	0
	DD	1071382528
	DD	0
	DD	1071382528
	DD	0
	DD	1072889856
	DD	0
	DD	1072889856
	DD	0
	DD	1072889856
	DD	0
	DD	1072889856
	DD	0
	DD	1072889856
	DD	0
	DD	1072889856
	DD	0
	DD	1072889856
	DD	0
	DD	1072889856
	DD	0
	DD	1073971200
	DD	0
	DD	1073971200
	DD	0
	DD	1073971200
	DD	0
	DD	1073971200
	DD	0
	DD	1073971200
	DD	0
	DD	1073971200
	DD	0
	DD	1073971200
	DD	0
	DD	1073971200
	DD	0
	DD	1072037888
	DD	0
	DD	1072037888
	DD	0
	DD	1072037888
	DD	0
	DD	1072037888
	DD	0
	DD	1072037888
	DD	0
	DD	1072037888
	DD	0
	DD	1072037888
	DD	0
	DD	1072037888
	DD	90291023
	DD	1071492199
	DD	90291023
	DD	1071492199
	DD	90291023
	DD	1071492199
	DD	90291023
	DD	1071492199
	DD	90291023
	DD	1071492199
	DD	90291023
	DD	1071492199
	DD	90291023
	DD	1071492199
	DD	90291023
	DD	1071492199
	DD	573531618
	DD	1014639487
	DD	573531618
	DD	1014639487
	DD	573531618
	DD	1014639487
	DD	573531618
	DD	1014639487
	DD	573531618
	DD	1014639487
	DD	573531618
	DD	1014639487
	DD	573531618
	DD	1014639487
	DD	573531618
	DD	1014639487
	DD	1413754136
	DD	1072243195
	DD	1413754136
	DD	1072243195
	DD	1413754136
	DD	1072243195
	DD	1413754136
	DD	1072243195
	DD	1413754136
	DD	1072243195
	DD	1413754136
	DD	1072243195
	DD	1413754136
	DD	1072243195
	DD	1413754136
	DD	1072243195
	DD	856972295
	DD	1015129638
	DD	856972295
	DD	1015129638
	DD	856972295
	DD	1015129638
	DD	856972295
	DD	1015129638
	DD	856972295
	DD	1015129638
	DD	856972295
	DD	1015129638
	DD	856972295
	DD	1015129638
	DD	856972295
	DD	1015129638
	DD	3531732635
	DD	1072657163
	DD	3531732635
	DD	1072657163
	DD	3531732635
	DD	1072657163
	DD	3531732635
	DD	1072657163
	DD	3531732635
	DD	1072657163
	DD	3531732635
	DD	1072657163
	DD	3531732635
	DD	1072657163
	DD	3531732635
	DD	1072657163
	DD	2062601149
	DD	1013974920
	DD	2062601149
	DD	1013974920
	DD	2062601149
	DD	1013974920
	DD	2062601149
	DD	1013974920
	DD	2062601149
	DD	1013974920
	DD	2062601149
	DD	1013974920
	DD	2062601149
	DD	1013974920
	DD	2062601149
	DD	1013974920
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	1413754136
	DD	1073291771
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
	DD	856972295
	DD	1016178214
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
	DD	1413754136
	DD	1074340347
	DD	1413754136
	DD	1074340347
	DD	1413754136
	DD	1074340347
	DD	1413754136
	DD	1074340347
	DD	1413754136
	DD	1074340347
	DD	1413754136
	DD	1074340347
	DD	1413754136
	DD	1074340347
	DD	1413754136
	DD	1074340347
	DD	0
	DD	1017226816
	DD	0
	DD	1017226816
	DD	0
	DD	1017226816
	DD	0
	DD	1017226816
	DD	0
	DD	1017226816
	DD	0
	DD	1017226816
	DD	0
	DD	1017226816
	DD	0
	DD	1017226816
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
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1071382528
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1072889856
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1073971200
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	1072037888
	DD	4
	DD	4
	DD	4
	DD	4
	DD	4
	DD	4
	DD	4
	DD	4
	DD	4
	DD	4
	DD	4
	DD	4
	DD	4
	DD	4
	DD	4
	DD	4
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
	DD	3866310424
	DD	1066132731
	DD	3866310424
	DD	1066132731
	DD	3866310424
	DD	1066132731
	DD	3866310424
	DD	1066132731
	DD	3866310424
	DD	1066132731
	DD	3866310424
	DD	1066132731
	DD	3866310424
	DD	1066132731
	DD	3866310424
	DD	1066132731
	DD	529668190
	DD	3214953687
	DD	529668190
	DD	3214953687
	DD	529668190
	DD	3214953687
	DD	529668190
	DD	3214953687
	DD	529668190
	DD	3214953687
	DD	529668190
	DD	3214953687
	DD	529668190
	DD	3214953687
	DD	529668190
	DD	3214953687
	DD	1493047753
	DD	1067887957
	DD	1493047753
	DD	1067887957
	DD	1493047753
	DD	1067887957
	DD	1493047753
	DD	1067887957
	DD	1493047753
	DD	1067887957
	DD	1493047753
	DD	1067887957
	DD	1493047753
	DD	1067887957
	DD	1493047753
	DD	1067887957
	DD	1554070819
	DD	3215629941
	DD	1554070819
	DD	3215629941
	DD	1554070819
	DD	3215629941
	DD	1554070819
	DD	3215629941
	DD	1554070819
	DD	3215629941
	DD	1554070819
	DD	3215629941
	DD	1554070819
	DD	3215629941
	DD	1554070819
	DD	3215629941
	DD	3992437651
	DD	1068372721
	DD	3992437651
	DD	1068372721
	DD	3992437651
	DD	1068372721
	DD	3992437651
	DD	1068372721
	DD	3992437651
	DD	1068372721
	DD	3992437651
	DD	1068372721
	DD	3992437651
	DD	1068372721
	DD	3992437651
	DD	1068372721
	DD	845965549
	DD	3216052365
	DD	845965549
	DD	3216052365
	DD	845965549
	DD	3216052365
	DD	845965549
	DD	3216052365
	DD	845965549
	DD	3216052365
	DD	845965549
	DD	3216052365
	DD	845965549
	DD	3216052365
	DD	845965549
	DD	3216052365
	DD	3073500986
	DD	1068740914
	DD	3073500986
	DD	1068740914
	DD	3073500986
	DD	1068740914
	DD	3073500986
	DD	1068740914
	DD	3073500986
	DD	1068740914
	DD	3073500986
	DD	1068740914
	DD	3073500986
	DD	1068740914
	DD	3073500986
	DD	1068740914
	DD	426211919
	DD	3216459217
	DD	426211919
	DD	3216459217
	DD	426211919
	DD	3216459217
	DD	426211919
	DD	3216459217
	DD	426211919
	DD	3216459217
	DD	426211919
	DD	3216459217
	DD	426211919
	DD	3216459217
	DD	426211919
	DD	3216459217
	DD	435789718
	DD	1069314503
	DD	435789718
	DD	1069314503
	DD	435789718
	DD	1069314503
	DD	435789718
	DD	1069314503
	DD	435789718
	DD	1069314503
	DD	435789718
	DD	1069314503
	DD	435789718
	DD	1069314503
	DD	435789718
	DD	1069314503
	DD	2453936673
	DD	3217180964
	DD	2453936673
	DD	3217180964
	DD	2453936673
	DD	3217180964
	DD	2453936673
	DD	3217180964
	DD	2453936673
	DD	3217180964
	DD	2453936673
	DD	3217180964
	DD	2453936673
	DD	3217180964
	DD	2453936673
	DD	3217180964
	DD	2576977731
	DD	1070176665
	DD	2576977731
	DD	1070176665
	DD	2576977731
	DD	1070176665
	DD	2576977731
	DD	1070176665
	DD	2576977731
	DD	1070176665
	DD	2576977731
	DD	1070176665
	DD	2576977731
	DD	1070176665
	DD	2576977731
	DD	1070176665
	DD	1431655762
	DD	3218429269
	DD	1431655762
	DD	3218429269
	DD	1431655762
	DD	3218429269
	DD	1431655762
	DD	3218429269
	DD	1431655762
	DD	3218429269
	DD	1431655762
	DD	3218429269
	DD	1431655762
	DD	3218429269
	DD	1431655762
	DD	3218429269
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	2150629376
	DD	4258267136
	DD	4258267136
	DD	4258267136
	DD	4258267136
	DD	4258267136
	DD	4258267136
	DD	4258267136
	DD	4258267136
	DD	4258267136
	DD	4258267136
	DD	4258267136
	DD	4258267136
	DD	4258267136
	DD	4258267136
	DD	4258267136
	DD	4258267136
_vmldAtanHATab	DD	3892314112
	DD	1069799150
	DD	2332892550
	DD	1039715405
	DD	1342177280
	DD	1070305495
	DD	270726690
	DD	1041535749
	DD	939524096
	DD	1070817911
	DD	2253973841
	DD	3188654726
	DD	3221225472
	DD	1071277294
	DD	3853927037
	DD	1043226911
	DD	2818572288
	DD	1071767563
	DD	2677759107
	DD	1044314101
	DD	3355443200
	DD	1072103591
	DD	1636578514
	DD	3191094734
	DD	1476395008
	DD	1072475260
	DD	1864703685
	DD	3188646936
	DD	805306368
	DD	1072747407
	DD	192551812
	DD	3192726267
	DD	2013265920
	DD	1072892781
	DD	2240369452
	DD	1043768538
	DD	0
	DD	1072999953
	DD	3665168337
	DD	3192705970
	DD	402653184
	DD	1073084787
	DD	1227953434
	DD	3192313277
	DD	2013265920
	DD	1073142981
	DD	3853283127
	DD	1045277487
	DD	805306368
	DD	1073187261
	DD	1676192264
	DD	3192868861
	DD	134217728
	DD	1073217000
	DD	4290763938
	DD	1042034855
	DD	671088640
	DD	1073239386
	DD	994303084
	DD	3189643768
	DD	402653184
	DD	1073254338
	DD	1878067156
	DD	1042652475
	DD	1610612736
	DD	1073265562
	DD	670314820
	DD	1045138554
	DD	3221225472
	DD	1073273048
	DD	691126919
	DD	3189987794
	DD	3489660928
	DD	1073278664
	DD	1618990832
	DD	3188194509
	DD	1207959552
	DD	1073282409
	DD	2198872939
	DD	1044806069
	DD	3489660928
	DD	1073285217
	DD	2633982383
	DD	1042307894
	DD	939524096
	DD	1073287090
	DD	1059367786
	DD	3189114230
	DD	2281701376
	DD	1073288494
	DD	3158525533
	DD	1044484961
	DD	3221225472
	DD	1073289430
	DD	286581777
	DD	1044893263
	DD	4026531840
	DD	1073290132
	DD	2000245215
	DD	3191647611
	DD	134217728
	DD	1073290601
	DD	4205071590
	DD	1045035927
	DD	536870912
	DD	1073290952
	DD	2334392229
	DD	1043447393
	DD	805306368
	DD	1073291186
	DD	2281458177
	DD	3188885569
	DD	3087007744
	DD	1073291361
	DD	691611507
	DD	1044733832
	DD	3221225472
	DD	1073291478
	DD	1816229550
	DD	1044363390
	DD	2281701376
	DD	1073291566
	DD	1993843750
	DD	3189837440
	DD	134217728
	DD	1073291625
	DD	3654754496
	DD	1044970837
	DD	4026531840
	DD	1073291668
	DD	3224300229
	DD	3191935390
	DD	805306368
	DD	1073291698
	DD	2988777976
	DD	3188950659
	DD	536870912
	DD	1073291720
	DD	1030371341
	DD	1043402665
	DD	3221225472
	DD	1073291734
	DD	1524463765
	DD	1044361356
	DD	3087007744
	DD	1073291745
	DD	2754295320
	DD	1044731036
	DD	134217728
	DD	1073291753
	DD	3099629057
	DD	1044970710
	DD	2281701376
	DD	1073291758
	DD	962914160
	DD	3189838838
	DD	805306368
	DD	1073291762
	DD	3543908206
	DD	3188950786
	DD	4026531840
	DD	1073291764
	DD	1849909620
	DD	3191935434
	DD	3221225472
	DD	1073291766
	DD	1641333636
	DD	1044361352
	DD	536870912
	DD	1073291768
	DD	1373968792
	DD	1043402654
	DD	134217728
	DD	1073291769
	DD	2033191599
	DD	1044970710
	DD	3087007744
	DD	1073291769
	DD	4117947437
	DD	1044731035
	DD	805306368
	DD	1073291770
	DD	315378368
	DD	3188950787
	DD	2281701376
	DD	1073291770
	DD	2428571750
	DD	3189838838
	DD	3221225472
	DD	1073291770
	DD	1608007466
	DD	1044361352
	DD	4026531840
	DD	1073291770
	DD	1895711420
	DD	3191935434
	DD	134217728
	DD	1073291771
	DD	2031108713
	DD	1044970710
	DD	536870912
	DD	1073291771
	DD	1362518342
	DD	1043402654
	DD	805306368
	DD	1073291771
	DD	317461253
	DD	3188950787
	DD	939524096
	DD	1073291771
	DD	4117231784
	DD	1044731035
	DD	1073741824
	DD	1073291771
	DD	1607942376
	DD	1044361352
	DD	1207959552
	DD	1073291771
	DD	2428929577
	DD	3189838838
	DD	1207959552
	DD	1073291771
	DD	2031104645
	DD	1044970710
	DD	1342177280
	DD	1073291771
	DD	1895722602
	DD	3191935434
	DD	1342177280
	DD	1073291771
	DD	317465322
	DD	3188950787
	DD	1342177280
	DD	1073291771
	DD	1362515546
	DD	1043402654
	DD	1342177280
	DD	1073291771
	DD	1607942248
	DD	1044361352
	DD	1342177280
	DD	1073291771
	DD	4117231610
	DD	1044731035
	DD	1342177280
	DD	1073291771
	DD	2031104637
	DD	1044970710
	DD	1342177280
	DD	1073291771
	DD	1540251232
	DD	1045150466
	DD	1342177280
	DD	1073291771
	DD	2644671394
	DD	1045270303
	DD	1342177280
	DD	1073291771
	DD	2399244691
	DD	1045360181
	DD	1342177280
	DD	1073291771
	DD	803971124
	DD	1045420100
	DD	1476395008
	DD	1073291771
	DD	3613709523
	DD	3192879152
	DD	1476395008
	DD	1073291771
	DD	2263862659
	DD	3192849193
	DD	1476395008
	DD	1073291771
	DD	177735686
	DD	3192826724
	DD	1476395008
	DD	1073291771
	DD	1650295902
	DD	3192811744
	DD	1476395008
	DD	1073291771
	DD	2754716064
	DD	3192800509
	DD	1476395008
	DD	1073291771
	DD	3490996172
	DD	3192793019
	DD	1476395008
	DD	1073291771
	DD	1895722605
	DD	3192787402
	DD	1476395008
	DD	1073291771
	DD	2263862659
	DD	3192783657
	DD	1476395008
	DD	1073291771
	DD	3613709523
	DD	3192780848
	DD	1476395008
	DD	1073291771
	DD	1650295902
	DD	3192778976
	DD	1476395008
	DD	1073291771
	DD	177735686
	DD	3192777572
	DD	1476395008
	DD	1073291771
	DD	3490996172
	DD	3192776635
	DD	1476395008
	DD	1073291771
	DD	2754716064
	DD	3192775933
	DD	1476395008
	DD	1073291771
	DD	2263862659
	DD	3192775465
	DD	1476395008
	DD	1073291771
	DD	1895722605
	DD	3192775114
	DD	1476395008
	DD	1073291771
	DD	1650295902
	DD	3192774880
	DD	1476395008
	DD	1073291771
	DD	3613709523
	DD	3192774704
	DD	1476395008
	DD	1073291771
	DD	3490996172
	DD	3192774587
	DD	1476395008
	DD	1073291771
	DD	177735686
	DD	3192774500
	DD	1476395008
	DD	1073291771
	DD	2263862659
	DD	3192774441
	DD	1476395008
	DD	1073291771
	DD	2754716064
	DD	3192774397
	DD	1476395008
	DD	1073291771
	DD	1650295902
	DD	3192774368
	DD	1476395008
	DD	1073291771
	DD	1895722605
	DD	3192774346
	DD	1476395008
	DD	1073291771
	DD	3490996172
	DD	3192774331
	DD	1476395008
	DD	1073291771
	DD	3613709523
	DD	3192774320
	DD	1476395008
	DD	1073291771
	DD	2263862659
	DD	3192774313
	DD	1476395008
	DD	1073291771
	DD	177735686
	DD	3192774308
	DD	1476395008
	DD	1073291771
	DD	1650295902
	DD	3192774304
	DD	1476395008
	DD	1073291771
	DD	2754716064
	DD	3192774301
	DD	1476395008
	DD	1073291771
	DD	3490996172
	DD	3192774299
	DD	1476395008
	DD	1073291771
	DD	1895722605
	DD	3192774298
	DD	1476395008
	DD	1073291771
	DD	2263862659
	DD	3192774297
	DD	1476395008
	DD	1073291771
	DD	3613709523
	DD	3192774296
	DD	1476395008
	DD	1073291771
	DD	1650295902
	DD	3192774296
	DD	1476395008
	DD	1073291771
	DD	177735686
	DD	3192774296
	DD	1476395008
	DD	1073291771
	DD	3490996172
	DD	3192774295
	DD	1476395008
	DD	1073291771
	DD	2754716064
	DD	3192774295
	DD	1476395008
	DD	1073291771
	DD	2263862659
	DD	3192774295
	DD	1476395008
	DD	1073291771
	DD	1895722605
	DD	3192774295
	DD	1476395008
	DD	1073291771
	DD	1650295902
	DD	3192774295
	DD	1476395008
	DD	1073291771
	DD	1466225875
	DD	3192774295
	DD	1476395008
	DD	1073291771
	DD	1343512524
	DD	3192774295
	DD	1476395008
	DD	1073291771
	DD	1251477510
	DD	3192774295
	DD	1476395008
	DD	1073291771
	DD	1190120835
	DD	3192774295
	DD	1476395008
	DD	1073291771
	DD	1144103328
	DD	3192774295
	DD	1476395008
	DD	1073291771
	DD	1113424990
	DD	3192774295
	DD	1476395008
	DD	1073291771
	DD	1090416237
	DD	3192774295
	DD	1476395008
	DD	1073291771
	DD	1075077068
	DD	3192774295
	DD	1431655765
	DD	3218429269
	DD	2576978363
	DD	1070176665
	DD	2453154343
	DD	3217180964
	DD	4189149139
	DD	1069314502
	DD	1775019125
	DD	3216459198
	DD	273199057
	DD	1068739452
	DD	874748308
	DD	3215993277
	DD	0
	DD	1017118720
	DD	0
	DD	1069547520
	DD	0
	DD	1129316352
	DD	0
	DD	1072693248
	DD	1413754136
	DD	1073291771
	DD	856972295
	DD	1016178214
	DD	33554432
	DD	1101004800
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
