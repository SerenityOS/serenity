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
	PUBLIC __svml_sin2_ha_e9

__svml_sin2_ha_e9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 488
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [304+rsp], xmm15
        xor       edx, edx
        vmovups   XMMWORD PTR [320+rsp], xmm14
        vmovups   XMMWORD PTR [336+rsp], xmm13
        vmovups   XMMWORD PTR [352+rsp], xmm12
        vmovups   XMMWORD PTR [368+rsp], xmm11
        vmovups   XMMWORD PTR [384+rsp], xmm10
        vmovups   XMMWORD PTR [400+rsp], xmm9
        vmovups   XMMWORD PTR [416+rsp], xmm8
        vmovapd   xmm8, xmm0
        vmovups   XMMWORD PTR [432+rsp], xmm7
        vmovups   XMMWORD PTR [448+rsp], xmm6
        mov       QWORD PTR [464+rsp], r13
        lea       r13, QWORD PTR [223+rsp]
        vmovupd   xmm1, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16384]
        and       r13, -64
        vandnpd   xmm7, xmm1, xmm8
        vandpd    xmm1, xmm8, xmm1
        vmulpd    xmm2, xmm1, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16576]
        vmovupd   xmm9, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16640]
        vaddpd    xmm10, xmm9, xmm2
        vsubpd    xmm4, xmm10, xmm9
        vmulpd    xmm2, xmm4, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16768]
        vmulpd    xmm13, xmm4, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16832]
        vsubpd    xmm14, xmm1, xmm2
        vmulpd    xmm5, xmm4, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16896]
        vsubpd    xmm6, xmm14, xmm13
        vandps    xmm3, xmm10, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16704]
        vsubpd    xmm15, xmm14, xmm6
        vmovd     ecx, xmm3
        vsubpd    xmm11, xmm15, xmm13
        shl       ecx, 5
        vpextrd   r8d, xmm3, 2
        shl       r8d, 5
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        vmovhpd   xmm15, xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r8]
        vmulpd    xmm9, xmm15, xmm6
        vsubpd    xmm5, xmm11, xmm5
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+rcx]
        vmovhpd   xmm13, xmm12, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r8]
        vmulpd    xmm10, xmm13, xmm6
        vmovq     xmm2, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+rcx]
        vmovhpd   xmm3, xmm2, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r8]
        vaddpd    xmm11, xmm3, xmm9
        vaddpd    xmm4, xmm10, xmm11
        vsubpd    xmm14, xmm3, xmm11
        vsubpd    xmm12, xmm11, xmm4
        vaddpd    xmm0, xmm9, xmm14
        vaddpd    xmm2, xmm10, xmm12
        vaddpd    xmm14, xmm0, xmm2
        vmulpd    xmm0, xmm6, xmm6
        vmovupd   xmm2, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+17024]
        vmulpd    xmm11, xmm2, xmm0
        vmovupd   xmm9, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16960]
        vaddpd    xmm10, xmm9, xmm11
        vmulpd    xmm12, xmm0, xmm10
        vaddpd    xmm10, xmm13, xmm15
        vmulpd    xmm11, xmm6, xmm12
        vmulpd    xmm15, xmm11, xmm10
        vaddpd    xmm12, xmm14, xmm15
        vmovupd   xmm15, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+17216]
        vmulpd    xmm13, xmm15, xmm0
        vmovupd   xmm14, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+17152]
        vaddpd    xmm11, xmm14, xmm13
        vmulpd    xmm11, xmm0, xmm11
        vmovupd   xmm13, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+17088]
        vaddpd    xmm11, xmm13, xmm11
        vmulpd    xmm0, xmm0, xmm11
        vmulpd    xmm11, xmm3, xmm0
        vmulpd    xmm3, xmm3, xmm6
        vaddpd    xmm11, xmm12, xmm11
        vsubpd    xmm0, xmm10, xmm3
        vmulpd    xmm5, xmm5, xmm0
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+rcx]
        vmovhpd   xmm3, xmm6, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r8]
        vaddpd    xmm0, xmm3, xmm5
        vaddpd    xmm3, xmm11, xmm0
        vaddpd    xmm4, xmm4, xmm3
        vcmpnlepd xmm3, xmm1, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16448]
        vmovmskpd r9d, xmm3
        vxorpd    xmm0, xmm4, xmm7
        mov       QWORD PTR [472+rsp], r13
        test      r9d, r9d
        jne       _B1_12

_B1_2::

        test      edx, edx
        jne       _B1_4

_B1_3::

        vmovups   xmm6, XMMWORD PTR [448+rsp]
        vmovups   xmm7, XMMWORD PTR [432+rsp]
        vmovups   xmm8, XMMWORD PTR [416+rsp]
        vmovups   xmm9, XMMWORD PTR [400+rsp]
        vmovups   xmm10, XMMWORD PTR [384+rsp]
        vmovups   xmm11, XMMWORD PTR [368+rsp]
        vmovups   xmm12, XMMWORD PTR [352+rsp]
        vmovups   xmm13, XMMWORD PTR [336+rsp]
        vmovups   xmm14, XMMWORD PTR [320+rsp]
        vmovups   xmm15, XMMWORD PTR [304+rsp]
        mov       r13, QWORD PTR [464+rsp]
        add       rsp, 488
        ret

_B1_4::

        vmovupd   XMMWORD PTR [r13], xmm8
        vmovupd   XMMWORD PTR [64+r13], xmm0
        je        _B1_3

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
        cmp       ebx, 2
        jl        _B1_8

_B1_10::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B1_3

_B1_11::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dsin_ha_cout_rare_internal
        jmp       _B1_9

_B1_12::

        vpand     xmm12, xmm1, XMMWORD PTR [_2il0floatpacket_36]
        vpsrlq    xmm11, xmm12, 52
        vmovupd   XMMWORD PTR [32+rsp], xmm9
        vmovupd   xmm9, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16512]
        vmovd     ecx, xmm11
        vandpd    xmm10, xmm9, xmm1
        vmovupd   XMMWORD PTR [64+rsp], xmm14
        vpand     xmm4, xmm1, XMMWORD PTR [_2il0floatpacket_37]
        vpextrd   r9d, xmm11, 2
        vcmpeqpd  xmm14, xmm10, xmm9
        vmovupd   XMMWORD PTR [96+rsp], xmm7
        lea       r8d, DWORD PTR [rcx+rcx*2]
        shl       r8d, 3
        lea       r10d, DWORD PTR [r9+r9*2]
        shl       r10d, 3
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r8]
        vpaddq    xmm7, xmm4, XMMWORD PTR [_2il0floatpacket_38]
        vmovmskpd edx, xmm14
        vmovupd   XMMWORD PTR [80+rsp], xmm13
        vpsrlq    xmm11, xmm7, 32
        vmovupd   XMMWORD PTR [144+rsp], xmm8
        vmovq     xmm8, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r8]
        vmovhpd   xmm13, xmm6, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r10]
        vmovdqu   xmm14, XMMWORD PTR [_2il0floatpacket_39]
        vmovq     xmm5, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r8]
        vmovupd   XMMWORD PTR [128+rsp], xmm0
        vpsrlq    xmm0, xmm13, 32
        vmovhpd   xmm12, xmm8, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r10]
        vpand     xmm8, xmm7, xmm14
        vpand     xmm13, xmm13, xmm14
        vpand     xmm9, xmm12, xmm14
        vmovhpd   xmm10, xmm5, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r10]
        vpsrlq    xmm12, xmm12, 32
        vmovupd   XMMWORD PTR [48+rsp], xmm15
        vpand     xmm4, xmm10, xmm14
        vpmuludq  xmm15, xmm11, xmm13
        vpsrlq    xmm6, xmm10, 32
        vpmuludq  xmm13, xmm11, xmm0
        vpmuludq  xmm0, xmm8, xmm0
        vpmuludq  xmm5, xmm8, xmm6
        vpmuludq  xmm6, xmm11, xmm6
        vpmuludq  xmm10, xmm8, xmm9
        vpmuludq  xmm9, xmm11, xmm9
        vmovupd   XMMWORD PTR [112+rsp], xmm3
        vpsrlq    xmm0, xmm0, 32
        vpmuludq  xmm3, xmm8, xmm4
        vpsrlq    xmm15, xmm15, 32
        vpmuludq  xmm4, xmm11, xmm4
        vpmuludq  xmm8, xmm8, xmm12
        vpaddq    xmm13, xmm13, xmm0
        vpand     xmm7, xmm3, xmm14
        vpsrlq    xmm3, xmm3, 32
        vpaddq    xmm7, xmm7, xmm13
        vpaddq    xmm13, xmm15, xmm7
        vpaddq    xmm15, xmm4, xmm3
        vpand     xmm7, xmm5, xmm14
        vpsrlq    xmm5, xmm5, 32
        vpaddq    xmm7, xmm7, xmm15
        vpaddq    xmm5, xmm6, xmm5
        vpsrlq    xmm0, xmm13, 32
        vpand     xmm15, xmm10, xmm14
        vpaddq    xmm7, xmm0, xmm7
        vpaddq    xmm15, xmm15, xmm5
        vpsrlq    xmm11, xmm10, 32
        vpsrlq    xmm3, xmm7, 32
        vpaddq    xmm10, xmm9, xmm11
        vpaddq    xmm0, xmm3, xmm15
        vpand     xmm15, xmm8, xmm14
        vpsrlq    xmm4, xmm0, 32
        vpaddq    xmm9, xmm15, xmm10
        vpaddq    xmm12, xmm4, xmm9
        vpsllq    xmm11, xmm12, 32
        vpand     xmm8, xmm0, xmm14
        vpaddq    xmm3, xmm11, xmm8
        vpand     xmm6, xmm1, XMMWORD PTR [_2il0floatpacket_40]
        vpsllq    xmm7, xmm7, 32
        vpxor     xmm10, xmm6, XMMWORD PTR [_2il0floatpacket_41]
        vpand     xmm14, xmm13, xmm14
        vpsrlq    xmm13, xmm3, 12
        vpaddq    xmm5, xmm7, xmm14
        vmovupd   xmm9, XMMWORD PTR [_2il0floatpacket_42]
        vpor      xmm14, xmm13, xmm10
        vaddpd    xmm15, xmm14, xmm9
        vsubpd    xmm7, xmm15, xmm9
        vpand     xmm11, xmm5, XMMWORD PTR [_2il0floatpacket_45]
        vpsrlq    xmm5, xmm5, 24
        vpxor     xmm13, xmm6, XMMWORD PTR [_2il0floatpacket_44]
        vpsllq    xmm0, xmm11, 28
        vpand     xmm3, xmm3, XMMWORD PTR [_2il0floatpacket_47]
        vpor      xmm4, xmm0, xmm13
        vpxor     xmm9, xmm6, XMMWORD PTR [_2il0floatpacket_46]
        vpsllq    xmm6, xmm3, 40
        vsubpd    xmm11, xmm4, xmm13
        vsubpd    xmm8, xmm14, xmm7
        vpor      xmm13, xmm6, xmm5
        vpor      xmm10, xmm13, xmm9
        vsubpd    xmm14, xmm10, xmm9
        vaddpd    xmm7, xmm8, xmm14
        vsubpd    xmm8, xmm8, xmm7
        vaddpd    xmm13, xmm14, xmm8
        vandpd    xmm9, xmm7, XMMWORD PTR [_2il0floatpacket_51]
        vaddpd    xmm14, xmm11, xmm13
        vsubpd    xmm3, xmm7, xmm9
        vmulpd    xmm6, xmm14, XMMWORD PTR [_2il0floatpacket_48]
        vmovupd   xmm10, XMMWORD PTR [_2il0floatpacket_49]
        vmovupd   xmm0, XMMWORD PTR [_2il0floatpacket_50]
        vmulpd    xmm4, xmm10, xmm3
        vmulpd    xmm5, xmm0, xmm9
        vmulpd    xmm8, xmm0, xmm3
        vaddpd    xmm11, xmm4, xmm5
        vaddpd    xmm13, xmm6, xmm8
        vandps    xmm12, xmm15, XMMWORD PTR [_2il0floatpacket_43]
        vmulpd    xmm15, xmm10, xmm9
        vaddpd    xmm10, xmm11, xmm13
        vmovd     r11d, xmm12
        vandpd    xmm13, xmm1, XMMWORD PTR [_2il0floatpacket_52]
        vaddpd    xmm14, xmm15, xmm10
        vsubpd    xmm15, xmm15, xmm14
        vaddpd    xmm15, xmm10, xmm15
        vmovupd   xmm10, XMMWORD PTR [_2il0floatpacket_53]
        vcmpgtpd  xmm7, xmm13, xmm10
        vcmplepd  xmm9, xmm13, xmm10
        shl       r11d, 5
        vandpd    xmm1, xmm9, xmm1
        vpextrd   ecx, xmm12, 2
        vandpd    xmm13, xmm7, xmm14
        shl       ecx, 5
        vorpd     xmm9, xmm1, xmm13
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r11]
        vandpd    xmm10, xmm7, xmm15
        vmovhpd   xmm15, xmm1, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        vmulpd    xmm3, xmm9, xmm15
        vmulpd    xmm1, xmm9, xmm9
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r11]
        vmovhpd   xmm7, xmm14, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+rcx]
        vmulpd    xmm6, xmm9, xmm7
        vmulpd    xmm2, xmm2, xmm1
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r11]
        vmovhpd   xmm14, xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+rcx]
        vaddpd    xmm5, xmm14, xmm3
        vaddpd    xmm2, xmm2, XMMWORD PTR [32+rsp]
        vaddpd    xmm13, xmm6, xmm5
        vsubpd    xmm4, xmm14, xmm5
        vmulpd    xmm0, xmm1, xmm2
        vsubpd    xmm8, xmm5, xmm13
        vaddpd    xmm2, xmm7, xmm15
        vaddpd    xmm11, xmm3, xmm4
        vmulpd    xmm3, xmm9, xmm0
        vaddpd    xmm12, xmm6, xmm8
        vmulpd    xmm6, xmm1, XMMWORD PTR [48+rsp]
        vmulpd    xmm9, xmm9, xmm14
        vaddpd    xmm7, xmm6, XMMWORD PTR [64+rsp]
        vaddpd    xmm4, xmm11, xmm12
        vmulpd    xmm5, xmm3, xmm2
        vmulpd    xmm8, xmm1, xmm7
        vsubpd    xmm2, xmm2, xmm9
        vaddpd    xmm11, xmm8, XMMWORD PTR [80+rsp]
        vaddpd    xmm12, xmm4, xmm5
        vmulpd    xmm10, xmm10, xmm2
        vmulpd    xmm1, xmm1, xmm11
        vmulpd    xmm15, xmm14, xmm1
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r11]
        vmovhpd   xmm9, xmm14, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+rcx]
        vaddpd    xmm0, xmm12, xmm15
        vaddpd    xmm1, xmm9, xmm10
        vaddpd    xmm2, xmm0, xmm1
        vaddpd    xmm13, xmm13, xmm2
        vxorpd    xmm1, xmm13, XMMWORD PTR [96+rsp]
        vmovupd   xmm0, XMMWORD PTR [128+rsp]
        vmovupd   xmm2, XMMWORD PTR [112+rsp]
        vmovupd   xmm8, XMMWORD PTR [144+rsp]
        vblendvpd xmm0, xmm0, xmm1, xmm2
        jmp       _B1_2
        ALIGN     16

_B1_13::

__svml_sin2_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin2_ha_e9_B1_B4:
	DD	1604097
	DD	3855482
	DD	1861746
	DD	1800297
	DD	1738844
	DD	1677395
	DD	1615946
	DD	1554497
	DD	1493048
	DD	1431599
	DD	1370150
	DD	1308699
	DD	3997963

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_7
	DD	imagerel _unwind___svml_sin2_ha_e9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin2_ha_e9_B7_B11:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B1_1
	DD	imagerel _B1_7
	DD	imagerel _unwind___svml_sin2_ha_e9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_7
	DD	imagerel _B1_12
	DD	imagerel _unwind___svml_sin2_ha_e9_B7_B11

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin2_ha_e9_B12_B12:
	DD	33
	DD	imagerel _B1_1
	DD	imagerel _B1_7
	DD	imagerel _unwind___svml_sin2_ha_e9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_12
	DD	imagerel _B1_13
	DD	imagerel _unwind___svml_sin2_ha_e9_B12_B12

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sin4_ha_l9

__svml_sin4_ha_l9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L44::

        sub       rsp, 712
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [368+rsp], ymm15
        xor       edx, edx
        vmovups   YMMWORD PTR [400+rsp], ymm14
        vmovups   YMMWORD PTR [432+rsp], ymm13
        vmovups   YMMWORD PTR [464+rsp], ymm12
        vmovups   YMMWORD PTR [496+rsp], ymm11
        vmovups   YMMWORD PTR [528+rsp], ymm10
        vmovups   YMMWORD PTR [560+rsp], ymm9
        vmovups   YMMWORD PTR [592+rsp], ymm8
        vmovups   YMMWORD PTR [624+rsp], ymm7
        vmovups   YMMWORD PTR [656+rsp], ymm6
        mov       QWORD PTR [688+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovupd   ymm2, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16384]
        and       r13, -64
        vmovupd   ymm13, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16576]
        vmovupd   ymm1, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16640]
        vmovupd   ymm15, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16768]
        vmovupd   ymm14, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16832]
        vmovdqa   ymm8, ymm0
        vandnpd   ymm7, ymm2, ymm8
        vandpd    ymm2, ymm8, ymm2
        vfmadd213pd ymm13, ymm2, ymm1
        vsubpd    ymm5, ymm13, ymm1
        vfnmadd213pd ymm15, ymm5, ymm2
        vmovdqa   ymm3, ymm14
        vandps    ymm1, ymm13, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16704]
        vfnmadd213pd ymm3, ymm5, ymm15
        vsubpd    ymm9, ymm15, ymm3
        vfnmadd231pd ymm9, ymm14, ymm5
        vfnmadd132pd ymm5, ymm9, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16896]
        mov       QWORD PTR [696+rsp], r13
        vextracti128 xmm6, ymm1, 1
        vmovd     r8d, xmm1
        vmovd     r9d, xmm6
        shl       r8d, 5
        vpextrd   ecx, xmm1, 2
        shl       r9d, 5
        vpextrd   r10d, xmm6, 2
        shl       ecx, 5
        vmovq     xmm4, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r8]
        shl       r10d, 5
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r9]
        vmovhpd   xmm12, xmm4, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+rcx]
        vmovhpd   xmm10, xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r10]
        vmovq     xmm13, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r8]
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r8]
        vmovhpd   xmm4, xmm13, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+rcx]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r9]
        vmovhpd   xmm14, xmm11, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        vmovhpd   xmm9, xmm1, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r10]
        vinsertf128 ymm15, ymm12, xmm10, 1
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r9]
        vmovhpd   xmm6, xmm12, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r10]
        vinsertf128 ymm6, ymm4, xmm6, 1
        vmovdqa   ymm0, ymm6
        vmovdqa   ymm4, ymm15
        vinsertf128 ymm14, ymm14, xmm9, 1
        vfmadd231pd ymm0, ymm14, ymm3
        vmovupd   ymm9, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16960]
        vfmadd213pd ymm4, ymm3, ymm0
        vsubpd    ymm10, ymm6, ymm0
        vsubpd    ymm1, ymm0, ymm4
        vfmadd231pd ymm10, ymm14, ymm3
        vmulpd    ymm0, ymm3, ymm3
        vfmadd231pd ymm1, ymm3, ymm15
        vaddpd    ymm13, ymm10, ymm1
        vmovupd   ymm1, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+17024]
        vmovdqa   ymm12, ymm9
        vfmadd231pd ymm12, ymm1, ymm0
        vmulpd    ymm11, ymm0, ymm12
        vmulpd    ymm10, ymm3, ymm11
        vaddpd    ymm11, ymm15, ymm14
        vmovupd   ymm14, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+17152]
        vmovupd   ymm15, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+17216]
        vfmadd213pd ymm10, ymm11, ymm13
        vmovupd   ymm13, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+17088]
        vfnmadd213pd ymm3, ymm6, ymm11
        vmovdqa   ymm12, ymm14
        vfmadd231pd ymm12, ymm15, ymm0
        vfmadd213pd ymm12, ymm0, ymm13
        vmulpd    ymm12, ymm0, ymm12
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r8]
        vfmadd213pd ymm12, ymm6, ymm10
        vmovq     xmm10, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r9]
        vmovhpd   xmm6, xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+rcx]
        vmovhpd   xmm11, xmm10, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r10]
        vinsertf128 ymm0, ymm6, xmm11, 1
        vfmadd213pd ymm5, ymm3, ymm0
        vaddpd    ymm3, ymm12, ymm5
        vaddpd    ymm5, ymm4, ymm3
        vcmpnle_uqpd ymm3, ymm2, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16448]
        vmovmskpd r11d, ymm3
        vxorpd    ymm0, ymm5, ymm7
        test      r11d, r11d
        jne       _B2_12

_B2_2::

        test      edx, edx
        jne       _B2_4

_B2_3::

        vmovups   ymm6, YMMWORD PTR [656+rsp]
        vmovups   ymm7, YMMWORD PTR [624+rsp]
        vmovups   ymm8, YMMWORD PTR [592+rsp]
        vmovups   ymm9, YMMWORD PTR [560+rsp]
        vmovups   ymm10, YMMWORD PTR [528+rsp]
        vmovups   ymm11, YMMWORD PTR [496+rsp]
        vmovups   ymm12, YMMWORD PTR [464+rsp]
        vmovups   ymm13, YMMWORD PTR [432+rsp]
        vmovups   ymm14, YMMWORD PTR [400+rsp]
        vmovups   ymm15, YMMWORD PTR [368+rsp]
        mov       r13, QWORD PTR [688+rsp]
        add       rsp, 712
        ret

_B2_4::

        vmovupd   YMMWORD PTR [r13], ymm8
        vmovupd   YMMWORD PTR [64+r13], ymm0
        je        _B2_3

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
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        jmp       _B2_3

_B2_11::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dsin_ha_cout_rare_internal
        jmp       _B2_9

_B2_12::

        vmovupd   YMMWORD PTR [r13], ymm9
        vmovupd   YMMWORD PTR [224+r13], ymm8
        vmovupd   YMMWORD PTR [160+r13], ymm3
        vmovupd   YMMWORD PTR [64+r13], ymm14
        vmovupd   ymm14, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16512]
        vmovupd   YMMWORD PTR [96+r13], ymm13
        vmovupd   YMMWORD PTR [192+r13], ymm0
        vmovupd   YMMWORD PTR [32+r13], ymm15
        vmovupd   YMMWORD PTR [128+r13], ymm7
        vpand     ymm9, ymm2, YMMWORD PTR [_2il0floatpacket_54]
        vpsrlq    ymm8, ymm9, 52
        vandpd    ymm10, ymm14, ymm2
        vcmpeqpd  ymm11, ymm10, ymm14
        vmovmskpd edx, ymm11
        vextracti128 xmm3, ymm8, 1
        vmovd     r8d, xmm8
        vmovd     r9d, xmm3
        vpextrd   ecx, xmm8, 2
        lea       r11d, DWORD PTR [r8+r8*2]
        vpextrd   r8d, xmm3, 2
        lea       r9d, DWORD PTR [r9+r9*2]
        shl       r11d, 3
        shl       r9d, 3
        vpand     ymm8, ymm2, YMMWORD PTR [_2il0floatpacket_55]
        lea       r10d, DWORD PTR [rcx+rcx*2]
        vmovq     xmm13, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r11]
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r9]
        vmovq     xmm5, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r11]
        shl       r10d, 3
        lea       ecx, DWORD PTR [r8+r8*2]
        shl       ecx, 3
        vmovhpd   xmm14, xmm13, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r10]
        vmovhpd   xmm9, xmm11, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+rcx]
        vpaddq    ymm3, ymm8, YMMWORD PTR [_2il0floatpacket_56]
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r9]
        vmovhpd   xmm12, xmm5, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r10]
        vmovhpd   xmm0, xmm6, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+rcx]
        vpsrlq    ymm11, ymm3, 32
        vmovq     xmm4, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r11]
        vmovq     xmm10, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r9]
        vmovhpd   xmm15, xmm4, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r10]
        vmovhpd   xmm7, xmm10, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+rcx]
        vinsertf128 ymm13, ymm14, xmm9, 1
        vmovdqu   ymm14, YMMWORD PTR [_2il0floatpacket_57]
        vpand     ymm8, ymm3, ymm14
        vinsertf128 ymm12, ymm12, xmm0, 1
        vpsrlq    ymm0, ymm13, 32
        vpand     ymm9, ymm12, ymm14
        vpsrlq    ymm12, ymm12, 32
        vpmuludq  ymm10, ymm8, ymm9
        vinsertf128 ymm5, ymm15, xmm7, 1
        vpand     ymm4, ymm5, ymm14
        vpand     ymm15, ymm13, ymm14
        vpsrlq    ymm6, ymm5, 32
        vpmuludq  ymm13, ymm11, ymm0
        vpmuludq  ymm0, ymm8, ymm0
        vpsrlq    ymm0, ymm0, 32
        vpmuludq  ymm3, ymm8, ymm4
        vpmuludq  ymm7, ymm11, ymm15
        vpand     ymm15, ymm3, ymm14
        vpaddq    ymm13, ymm13, ymm0
        vpsrlq    ymm7, ymm7, 32
        vpsrlq    ymm3, ymm3, 32
        vpaddq    ymm15, ymm15, ymm13
        vpmuludq  ymm5, ymm8, ymm6
        vpmuludq  ymm4, ymm11, ymm4
        vpaddq    ymm13, ymm7, ymm15
        vpand     ymm15, ymm5, ymm14
        vpaddq    ymm0, ymm4, ymm3
        vpand     ymm4, ymm10, ymm14
        vpsrlq    ymm7, ymm13, 32
        vpsrlq    ymm10, ymm10, 32
        vpaddq    ymm15, ymm15, ymm0
        vpand     ymm13, ymm13, ymm14
        vpaddq    ymm7, ymm7, ymm15
        vpsrlq    ymm15, ymm5, 32
        vpsrlq    ymm0, ymm7, 32
        vpsllq    ymm7, ymm7, 32
        vpmuludq  ymm6, ymm11, ymm6
        vpaddq    ymm5, ymm6, ymm15
        vpaddq    ymm3, ymm4, ymm5
        vpmuludq  ymm15, ymm8, ymm12
        vpmuludq  ymm11, ymm11, ymm9
        vpaddq    ymm3, ymm0, ymm3
        vpand     ymm8, ymm15, ymm14
        vpaddq    ymm9, ymm11, ymm10
        vpsrlq    ymm6, ymm3, 32
        vpand     ymm3, ymm3, ymm14
        vpaddq    ymm5, ymm8, ymm9
        vpaddq    ymm8, ymm7, ymm13
        vpaddq    ymm12, ymm6, ymm5
        vpand     ymm13, ymm8, YMMWORD PTR [_2il0floatpacket_63]
        vpsrlq    ymm8, ymm8, 24
        vmovupd   ymm6, YMMWORD PTR [_2il0floatpacket_60]
        vpsllq    ymm0, ymm12, 32
        vpand     ymm12, ymm2, YMMWORD PTR [_2il0floatpacket_58]
        vpaddq    ymm5, ymm0, ymm3
        vpxor     ymm4, ymm12, YMMWORD PTR [_2il0floatpacket_59]
        vpxor     ymm3, ymm12, YMMWORD PTR [_2il0floatpacket_62]
        vpsrlq    ymm14, ymm5, 12
        vpand     ymm5, ymm5, YMMWORD PTR [_2il0floatpacket_65]
        vpor      ymm15, ymm14, ymm4
        vpsllq    ymm4, ymm5, 40
        vpsllq    ymm14, ymm13, 28
        vaddpd    ymm7, ymm15, ymm6
        vpor      ymm0, ymm14, ymm3
        vpand     ymm11, ymm7, YMMWORD PTR [_2il0floatpacket_61]
        vsubpd    ymm10, ymm7, ymm6
        vpor      ymm6, ymm4, ymm8
        vsubpd    ymm9, ymm15, ymm10
        vsubpd    ymm15, ymm0, ymm3
        vpxor     ymm10, ymm12, YMMWORD PTR [_2il0floatpacket_64]
        vmovupd   ymm0, YMMWORD PTR [_2il0floatpacket_69]
        vpor      ymm12, ymm6, ymm10
        vsubpd    ymm7, ymm12, ymm10
        vaddpd    ymm13, ymm9, ymm7
        vsubpd    ymm9, ymm9, ymm13
        vaddpd    ymm10, ymm7, ymm9
        vmovupd   ymm7, YMMWORD PTR [_2il0floatpacket_66]
        vaddpd    ymm6, ymm15, ymm10
        vmulpd    ymm4, ymm7, ymm13
        vmovapd   ymm15, ymm7
        vfmsub213pd ymm15, ymm13, ymm4
        vandpd    ymm14, ymm2, YMMWORD PTR [_2il0floatpacket_68]
        vcmpgt_oqpd ymm5, ymm14, ymm0
        vcmple_oqpd ymm3, ymm14, ymm0
        vfmadd132pd ymm13, ymm15, YMMWORD PTR [_2il0floatpacket_67]
        vfmadd213pd ymm6, ymm7, ymm13
        vandpd    ymm2, ymm3, ymm2
        vandpd    ymm15, ymm5, ymm4
        vorpd     ymm10, ymm2, ymm15
        vandpd    ymm7, ymm5, ymm6
        vmovapd   ymm5, ymm10
        vextracti128 xmm14, ymm11, 1
        vmovd     r8d, xmm11
        vmovd     r10d, xmm14
        shl       r8d, 5
        vpextrd   ecx, xmm11, 2
        shl       r10d, 5
        vpextrd   r11d, xmm14, 2
        shl       ecx, 5
        shl       r11d, 5
        vmovq     xmm2, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r8]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r10]
        vmovhpd   xmm13, xmm2, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+rcx]
        vmovhpd   xmm3, xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r11]
        vmovq     xmm4, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r8]
        vmovq     xmm15, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r8]
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r10]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r10]
        vmovhpd   xmm8, xmm4, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        vmovhpd   xmm2, xmm15, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+rcx]
        vmovhpd   xmm12, xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r11]
        vinsertf128 ymm13, ymm13, xmm3, 1
        vmovhpd   xmm3, xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r11]
        vmovaps   ymm15, ymm13
        vinsertf128 ymm14, ymm8, xmm12, 1
        vinsertf128 ymm2, ymm2, xmm3, 1
        vfmadd213pd ymm5, ymm14, ymm2
        vmovupd   ymm3, YMMWORD PTR [r13]
        vfmadd213pd ymm15, ymm10, ymm5
        vsubpd    ymm6, ymm2, ymm5
        vsubpd    ymm0, ymm5, ymm15
        vmovq     xmm5, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r10]
        vfmadd231pd ymm6, ymm14, ymm10
        vfmadd231pd ymm0, ymm10, ymm13
        vaddpd    ymm11, ymm6, ymm0
        vmovhpd   xmm6, xmm5, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r11]
        vmulpd    ymm0, ymm10, ymm10
        vfmadd231pd ymm3, ymm1, ymm0
        vmulpd    ymm1, ymm0, ymm3
        vmulpd    ymm3, ymm10, ymm1
        vaddpd    ymm1, ymm13, ymm14
        vmovupd   ymm13, YMMWORD PTR [64+r13]
        vmovupd   ymm14, YMMWORD PTR [160+r13]
        vfmadd213pd ymm3, ymm1, ymm11
        vfnmadd213pd ymm10, ymm2, ymm1
        vfmadd231pd ymm13, ymm0, YMMWORD PTR [32+r13]
        vfmadd213pd ymm13, ymm0, YMMWORD PTR [96+r13]
        vmulpd    ymm9, ymm0, ymm13
        vmovupd   ymm0, YMMWORD PTR [192+r13]
        vfmadd213pd ymm9, ymm2, ymm3
        vmovq     xmm2, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r8]
        vmovhpd   xmm4, xmm2, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+rcx]
        vinsertf128 ymm8, ymm4, xmm6, 1
        vfmadd213pd ymm7, ymm10, ymm8
        vmovupd   ymm8, YMMWORD PTR [224+r13]
        vaddpd    ymm7, ymm9, ymm7
        vaddpd    ymm10, ymm15, ymm7
        vxorpd    ymm12, ymm10, YMMWORD PTR [128+r13]
        vblendvpd ymm0, ymm0, ymm12, ymm14
        jmp       _B2_2
        ALIGN     16

_B2_13::

__svml_sin4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin4_ha_l9_B1_B4:
	DD	1603073
	DD	5690486
	DD	2713710
	DD	2586725
	DD	2459740
	DD	2332755
	DD	2205770
	DD	2078785
	DD	1951800
	DD	1824815
	DD	1697830
	DD	1570843
	DD	5832971

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_7
	DD	imagerel _unwind___svml_sin4_ha_l9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin4_ha_l9_B7_B11:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B2_1
	DD	imagerel _B2_7
	DD	imagerel _unwind___svml_sin4_ha_l9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_7
	DD	imagerel _B2_12
	DD	imagerel _unwind___svml_sin4_ha_l9_B7_B11

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin4_ha_l9_B12_B12:
	DD	33
	DD	imagerel _B2_1
	DD	imagerel _B2_7
	DD	imagerel _unwind___svml_sin4_ha_l9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_12
	DD	imagerel _B2_13
	DD	imagerel _unwind___svml_sin4_ha_l9_B12_B12

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sin1_ha_e9

__svml_sin1_ha_e9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L87::

        sub       rsp, 472
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [304+rsp], xmm15
        xor       edx, edx
        vmovups   XMMWORD PTR [320+rsp], xmm14
        vmovups   XMMWORD PTR [336+rsp], xmm13
        vmovups   XMMWORD PTR [352+rsp], xmm12
        vmovups   XMMWORD PTR [368+rsp], xmm11
        vmovups   XMMWORD PTR [384+rsp], xmm10
        vmovups   XMMWORD PTR [400+rsp], xmm9
        vmovups   XMMWORD PTR [416+rsp], xmm8
        vmovapd   xmm8, xmm0
        vmovups   XMMWORD PTR [432+rsp], xmm7
        vmovups   XMMWORD PTR [448+rsp], xmm6
        mov       QWORD PTR [296+rsp], r13
        lea       r13, QWORD PTR [223+rsp]
        vmovsd    xmm1, QWORD PTR [__svml_dsin_ha_data_internal_ha+16384]
        and       r13, -64
        vandnpd   xmm7, xmm1, xmm8
        vandpd    xmm1, xmm8, xmm1
        vmulsd    xmm2, xmm1, QWORD PTR [__svml_dsin_ha_data_internal_ha+16576]
        vmovsd    xmm9, QWORD PTR [__svml_dsin_ha_data_internal_ha+16640]
        vmovq     xmm5, QWORD PTR [__svml_dsin_ha_data_internal_ha+16704]
        vaddsd    xmm10, xmm2, xmm9
        vpand     xmm12, xmm10, xmm5
        vsubsd    xmm3, xmm10, xmm9
        vmovd     ecx, xmm12
        vmulsd    xmm2, xmm3, QWORD PTR [__svml_dsin_ha_data_internal_ha+16768]
        vmulsd    xmm13, xmm3, QWORD PTR [__svml_dsin_ha_data_internal_ha+16832]
        vmulsd    xmm6, xmm3, QWORD PTR [__svml_dsin_ha_data_internal_ha+16896]
        vsubsd    xmm14, xmm1, xmm2
        shl       ecx, 5
        vsubsd    xmm4, xmm14, xmm13
        vmulsd    xmm2, xmm4, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        vsubsd    xmm15, xmm14, xmm4
        vmovq     xmm5, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+rcx]
        vsubsd    xmm11, xmm15, xmm13
        vaddsd    xmm9, xmm5, xmm2
        vsubsd    xmm6, xmm11, xmm6
        vsubsd    xmm0, xmm5, xmm9
        vmovq     xmm15, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+rcx]
        vaddsd    xmm10, xmm0, xmm2
        vmulsd    xmm13, xmm15, xmm4
        vmulsd    xmm0, xmm4, xmm4
        vaddsd    xmm3, xmm13, xmm9
        vmovsd    xmm2, QWORD PTR [__svml_dsin_ha_data_internal_ha+17024]
        vsubsd    xmm14, xmm9, xmm3
        vmovsd    xmm9, QWORD PTR [__svml_dsin_ha_data_internal_ha+16960]
        vaddsd    xmm11, xmm14, xmm13
        vmulsd    xmm13, xmm2, xmm0
        vaddsd    xmm14, xmm11, xmm10
        vaddsd    xmm12, xmm13, xmm9
        vaddsd    xmm10, xmm15, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        vmulsd    xmm13, xmm12, xmm0
        vmulsd    xmm11, xmm13, xmm4
        vmulsd    xmm4, xmm4, xmm5
        vmulsd    xmm15, xmm11, xmm10
        mov       QWORD PTR [464+rsp], r13
        vaddsd    xmm12, xmm15, xmm14
        vmovsd    xmm15, QWORD PTR [__svml_dsin_ha_data_internal_ha+17216]
        vmulsd    xmm13, xmm15, xmm0
        vmovsd    xmm14, QWORD PTR [__svml_dsin_ha_data_internal_ha+17152]
        vaddsd    xmm11, xmm13, xmm14
        vmovsd    xmm13, QWORD PTR [__svml_dsin_ha_data_internal_ha+17088]
        vmulsd    xmm11, xmm11, xmm0
        vaddsd    xmm11, xmm11, xmm13
        vmulsd    xmm0, xmm11, xmm0
        vmulsd    xmm11, xmm0, xmm5
        vsubsd    xmm0, xmm10, xmm4
        vmulsd    xmm4, xmm6, xmm0
        vaddsd    xmm11, xmm11, xmm12
        vaddsd    xmm5, xmm4, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+rcx]
        vaddsd    xmm6, xmm11, xmm5
        vaddsd    xmm3, xmm3, xmm6
        vxorpd    xmm0, xmm3, xmm7
        vcmpnlesd xmm3, xmm1, QWORD PTR [__svml_dsin_ha_data_internal_ha+16448]
        vmovmskpd r8d, xmm3
        test      r8d, 1
        jne       _B3_8

_B3_2::

        test      edx, edx
        jne       _B3_4

_B3_3::

        vmovups   xmm6, XMMWORD PTR [448+rsp]
        vmovups   xmm7, XMMWORD PTR [432+rsp]
        vmovups   xmm8, XMMWORD PTR [416+rsp]
        vmovups   xmm9, XMMWORD PTR [400+rsp]
        vmovups   xmm10, XMMWORD PTR [384+rsp]
        vmovups   xmm11, XMMWORD PTR [368+rsp]
        vmovups   xmm12, XMMWORD PTR [352+rsp]
        vmovups   xmm13, XMMWORD PTR [336+rsp]
        vmovups   xmm14, XMMWORD PTR [320+rsp]
        vmovups   xmm15, XMMWORD PTR [304+rsp]
        mov       r13, QWORD PTR [296+rsp]
        add       rsp, 472
        ret

_B3_4::

        vmovsd    QWORD PTR [r13], xmm8
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B3_6

_B3_5::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B3_3

_B3_6::

        je        _B3_5

_B3_7::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dsin_ha_cout_rare_internal
        jmp       _B3_5

_B3_8::

        vmovupd   XMMWORD PTR [32+rsp], xmm9
        vpand     xmm9, xmm1, XMMWORD PTR [_2il0floatpacket_36]
        vpsrlq    xmm11, xmm9, 52
        vmovd     ecx, xmm11
        vmovupd   XMMWORD PTR [64+rsp], xmm14
        vmovupd   XMMWORD PTR [144+rsp], xmm8
        vmovsd    xmm14, QWORD PTR [__svml_dsin_ha_data_internal_ha+16512]
        vpand     xmm8, xmm1, XMMWORD PTR [_2il0floatpacket_37]
        lea       r8d, DWORD PTR [rcx+rcx*2]
        shl       r8d, 3
        vandpd    xmm10, xmm14, xmm1
        vmovupd   XMMWORD PTR [128+rsp], xmm3
        vcmpeqsd  xmm5, xmm10, xmm14
        vpaddq    xmm3, xmm8, XMMWORD PTR [_2il0floatpacket_38]
        vmovmskpd edx, xmm5
        vmovupd   XMMWORD PTR [80+rsp], xmm13
        vpsrlq    xmm11, xmm3, 32
        vmovq     xmm13, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r8]
        vmovdqu   xmm14, XMMWORD PTR [_2il0floatpacket_39]
        and       edx, 1
        vmovupd   XMMWORD PTR [112+rsp], xmm0
        vpsrlq    xmm0, xmm13, 32
        vpand     xmm8, xmm3, xmm14
        vpand     xmm13, xmm13, xmm14
        vmovq     xmm4, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r8]
        vmovupd   XMMWORD PTR [96+rsp], xmm7
        vpsrlq    xmm6, xmm4, 32
        vpmuludq  xmm7, xmm11, xmm13
        vpand     xmm4, xmm4, xmm14
        vpmuludq  xmm13, xmm11, xmm0
        vpmuludq  xmm0, xmm8, xmm0
        vpmuludq  xmm3, xmm8, xmm4
        vpmuludq  xmm5, xmm8, xmm6
        vpmuludq  xmm6, xmm11, xmm6
        vpsrlq    xmm0, xmm0, 32
        vpaddq    xmm13, xmm13, xmm0
        vmovupd   XMMWORD PTR [48+rsp], xmm15
        vpsrlq    xmm15, xmm7, 32
        vpand     xmm7, xmm3, xmm14
        vpaddq    xmm7, xmm7, xmm13
        vpaddq    xmm13, xmm15, xmm7
        vpmuludq  xmm15, xmm11, xmm4
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r8]
        vpsrlq    xmm4, xmm3, 32
        vpand     xmm9, xmm12, xmm14
        vpand     xmm7, xmm5, xmm14
        vpmuludq  xmm10, xmm8, xmm9
        vpsrlq    xmm5, xmm5, 32
        vpaddq    xmm3, xmm15, xmm4
        vpaddq    xmm6, xmm6, xmm5
        vpaddq    xmm7, xmm7, xmm3
        vpmuludq  xmm5, xmm11, xmm9
        vpsrlq    xmm0, xmm13, 32
        vpsrlq    xmm12, xmm12, 32
        vpaddq    xmm7, xmm0, xmm7
        vpand     xmm0, xmm10, xmm14
        vpsrlq    xmm15, xmm7, 32
        vpaddq    xmm4, xmm0, xmm6
        vpaddq    xmm4, xmm15, xmm4
        vpmuludq  xmm15, xmm8, xmm12
        vpsrlq    xmm10, xmm10, 32
        vpand     xmm8, xmm15, xmm14
        vpaddq    xmm9, xmm5, xmm10
        vpaddq    xmm11, xmm8, xmm9
        vpsrlq    xmm3, xmm4, 32
        vpand     xmm0, xmm4, xmm14
        vpaddq    xmm6, xmm3, xmm11
        vpsllq    xmm12, xmm6, 32
        vpsllq    xmm7, xmm7, 32
        vpaddq    xmm6, xmm12, xmm0
        vpand     xmm12, xmm1, XMMWORD PTR [_2il0floatpacket_40]
        vpand     xmm13, xmm13, xmm14
        vpaddq    xmm8, xmm7, xmm13
        vpxor     xmm13, xmm12, XMMWORD PTR [_2il0floatpacket_41]
        vpsrlq    xmm14, xmm6, 12
        vmovupd   xmm7, XMMWORD PTR [_2il0floatpacket_42]
        vpor      xmm10, xmm14, xmm13
        vpand     xmm4, xmm6, XMMWORD PTR [_2il0floatpacket_47]
        vaddsd    xmm5, xmm7, xmm10
        vpand     xmm15, xmm8, XMMWORD PTR [_2il0floatpacket_45]
        vpsllq    xmm13, xmm4, 40
        vpxor     xmm3, xmm12, XMMWORD PTR [_2il0floatpacket_44]
        vpsllq    xmm9, xmm15, 28
        vpxor     xmm15, xmm12, XMMWORD PTR [_2il0floatpacket_46]
        vpor      xmm0, xmm9, xmm3
        vmovupd   xmm6, XMMWORD PTR [_2il0floatpacket_49]
        vsubsd    xmm14, xmm5, xmm7
        vsubsd    xmm9, xmm0, xmm3
        vsubsd    xmm11, xmm10, xmm14
        vpsrlq    xmm7, xmm8, 24
        vpor      xmm10, xmm13, xmm7
        vpor      xmm14, xmm10, xmm15
        vmovupd   xmm13, XMMWORD PTR [_2il0floatpacket_50]
        vsubsd    xmm0, xmm14, xmm15
        vaddsd    xmm4, xmm11, xmm0
        vandpd    xmm8, xmm4, XMMWORD PTR [_2il0floatpacket_51]
        vsubsd    xmm11, xmm11, xmm4
        vmulsd    xmm14, xmm13, xmm8
        vsubsd    xmm7, xmm4, xmm8
        vmulsd    xmm15, xmm6, xmm8
        vaddsd    xmm3, xmm0, xmm11
        vmulsd    xmm10, xmm6, xmm7
        vaddsd    xmm12, xmm3, xmm9
        vmulsd    xmm3, xmm13, xmm7
        vaddsd    xmm4, xmm10, xmm14
        vmovupd   xmm9, XMMWORD PTR [_2il0floatpacket_48]
        vmulsd    xmm0, xmm9, xmm12
        vmovupd   xmm11, XMMWORD PTR [_2il0floatpacket_53]
        vaddsd    xmm6, xmm0, xmm3
        vandpd    xmm9, xmm1, XMMWORD PTR [_2il0floatpacket_52]
        vaddsd    xmm8, xmm4, xmm6
        vcmpgtsd  xmm7, xmm9, xmm11
        vcmplesd  xmm12, xmm9, xmm11
        vaddsd    xmm13, xmm15, xmm8
        vandpd    xmm1, xmm12, xmm1
        vsubsd    xmm15, xmm15, xmm13
        vandpd    xmm13, xmm7, xmm13
        vaddsd    xmm14, xmm15, xmm8
        vorpd     xmm10, xmm1, xmm13
        vandpd    xmm7, xmm7, xmm14
        vandps    xmm1, xmm5, XMMWORD PTR [_2il0floatpacket_43]
        vmovd     r9d, xmm1
        vmulsd    xmm1, xmm10, xmm10
        shl       r9d, 5
        vmulsd    xmm3, xmm10, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r9]
        vmulsd    xmm2, xmm2, xmm1
        vmovq     xmm15, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r9]
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r9]
        vmulsd    xmm6, xmm15, xmm10
        vaddsd    xmm4, xmm14, xmm3
        vaddsd    xmm2, xmm2, QWORD PTR [32+rsp]
        vaddsd    xmm13, xmm6, xmm4
        vsubsd    xmm0, xmm14, xmm4
        vsubsd    xmm5, xmm4, xmm13
        vaddsd    xmm9, xmm0, xmm3
        vmulsd    xmm0, xmm2, xmm1
        vaddsd    xmm2, xmm15, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r9]
        vaddsd    xmm8, xmm5, xmm6
        vmulsd    xmm3, xmm0, xmm10
        vmulsd    xmm10, xmm10, xmm14
        vaddsd    xmm4, xmm8, xmm9
        vmulsd    xmm15, xmm3, xmm2
        vmovupd   xmm5, XMMWORD PTR [48+rsp]
        vaddsd    xmm0, xmm15, xmm4
        vmulsd    xmm11, xmm5, xmm1
        vaddsd    xmm12, xmm11, QWORD PTR [64+rsp]
        vmulsd    xmm5, xmm12, xmm1
        vaddsd    xmm11, xmm5, QWORD PTR [80+rsp]
        vmulsd    xmm1, xmm11, xmm1
        vmulsd    xmm12, xmm1, xmm14
        vsubsd    xmm14, xmm2, xmm10
        vmulsd    xmm7, xmm7, xmm14
        vaddsd    xmm3, xmm12, xmm0
        vmovupd   xmm1, XMMWORD PTR [128+rsp]
        vmovupd   xmm0, XMMWORD PTR [112+rsp]
        vaddsd    xmm6, xmm7, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r9]
        vaddsd    xmm8, xmm3, xmm6
        vaddsd    xmm9, xmm13, xmm8
        vmovupd   xmm8, XMMWORD PTR [144+rsp]
        vxorpd    xmm13, xmm9, XMMWORD PTR [96+rsp]
        vblendvpd xmm0, xmm0, xmm13, xmm1
        jmp       _B3_2
        ALIGN     16

_B3_9::

__svml_sin1_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin1_ha_e9_B1_B8:
	DD	1604097
	DD	2479226
	DD	1861746
	DD	1800297
	DD	1738844
	DD	1677395
	DD	1615946
	DD	1554497
	DD	1493048
	DD	1431599
	DD	1370150
	DD	1308699
	DD	3866891

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_9
	DD	imagerel _unwind___svml_sin1_ha_e9_B1_B8

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sin4_ha_e9

__svml_sin4_ha_e9	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L126::

        push      rbp
        sub       rsp, 928
        lea       rbp, QWORD PTR [32+rsp]
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [560+rbp], ymm15
        xor       edx, edx
        vmovups   YMMWORD PTR [592+rbp], ymm14
        vmovups   YMMWORD PTR [624+rbp], ymm13
        vmovups   YMMWORD PTR [656+rbp], ymm12
        vmovups   YMMWORD PTR [688+rbp], ymm11
        vmovups   YMMWORD PTR [720+rbp], ymm10
        vmovups   YMMWORD PTR [752+rbp], ymm9
        vmovups   YMMWORD PTR [784+rbp], ymm8
        vmovups   YMMWORD PTR [816+rbp], ymm7
        vmovups   YMMWORD PTR [848+rbp], ymm6
        mov       QWORD PTR [880+rbp], r13
        lea       r13, QWORD PTR [79+rbp]
        vmovupd   ymm8, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16384]
        and       r13, -64
        vmovupd   ymm7, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16640]
        vmovapd   ymm10, ymm0
        vandnpd   ymm9, ymm8, ymm10
        vandpd    ymm8, ymm10, ymm8
        vmulpd    ymm6, ymm8, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16576]
        vaddpd    ymm3, ymm7, ymm6
        vmovupd   xmm6, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16704]
        vsubpd    ymm15, ymm3, ymm7
        vmulpd    ymm11, ymm15, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16768]
        vmulpd    ymm5, ymm15, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16832]
        vmulpd    ymm14, ymm15, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16896]
        vsubpd    ymm12, ymm8, ymm11
        vsubpd    ymm2, ymm12, ymm5
        vsubpd    ymm13, ymm12, ymm2
        vsubpd    ymm4, ymm13, ymm5
        vsubpd    ymm4, ymm4, ymm14
        mov       QWORD PTR [888+rbp], r13
        vandps    xmm7, xmm3, xmm6
        vextractf128 xmm1, ymm3, 1
        vmovd     r8d, xmm7
        vandps    xmm13, xmm1, xmm6
        vmovd     r9d, xmm13
        shl       r8d, 5
        vpextrd   ecx, xmm7, 2
        shl       r9d, 5
        vpextrd   r10d, xmm13, 2
        shl       ecx, 5
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r8]
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r8]
        shl       r10d, 5
        vmovq     xmm5, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r9]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r9]
        vmovhpd   xmm12, xmm11, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+rcx]
        vmovhpd   xmm3, xmm14, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        vmovhpd   xmm15, xmm5, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r10]
        vmovhpd   xmm0, xmm1, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r10]
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r8]
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r9]
        vmovhpd   xmm7, xmm6, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+rcx]
        vmovhpd   xmm5, xmm11, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r10]
        vinsertf128 ymm13, ymm12, xmm15, 1
        vinsertf128 ymm12, ymm3, xmm0, 1
        vmulpd    ymm15, ymm12, ymm2
        vmulpd    ymm0, ymm13, ymm2
        vinsertf128 ymm1, ymm7, xmm5, 1
        vaddpd    ymm3, ymm1, ymm15
        vaddpd    ymm5, ymm0, ymm3
        vsubpd    ymm14, ymm1, ymm3
        vsubpd    ymm6, ymm3, ymm5
        vaddpd    ymm7, ymm15, ymm14
        vaddpd    ymm3, ymm13, ymm12
        vaddpd    ymm11, ymm0, ymm6
        vmulpd    ymm0, ymm2, ymm2
        vmovupd   ymm6, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16960]
        vaddpd    ymm11, ymm7, ymm11
        vmovupd   ymm7, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+17024]
        vmulpd    ymm15, ymm7, ymm0
        vaddpd    ymm14, ymm6, ymm15
        vmulpd    ymm15, ymm0, ymm14
        vmulpd    ymm14, ymm2, ymm15
        vmulpd    ymm2, ymm1, ymm2
        vmulpd    ymm12, ymm14, ymm3
        vaddpd    ymm15, ymm11, ymm12
        vmovupd   ymm11, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+17216]
        vmovupd   ymm12, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+17152]
        vmulpd    ymm13, ymm11, ymm0
        vaddpd    ymm14, ymm12, ymm13
        vmovupd   ymm13, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+17088]
        vmulpd    ymm14, ymm0, ymm14
        vaddpd    ymm14, ymm13, ymm14
        vmulpd    ymm0, ymm0, ymm14
        vmulpd    ymm14, ymm1, ymm0
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r8]
        vmovhpd   xmm1, xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+rcx]
        vaddpd    ymm15, ymm15, ymm14
        vsubpd    ymm14, ymm3, ymm2
        vmovq     xmm2, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r9]
        vmovhpd   xmm3, xmm2, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r10]
        vmulpd    ymm4, ymm4, ymm14
        vinsertf128 ymm0, ymm1, xmm3, 1
        vcmpnle_uqpd ymm1, ymm8, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16448]
        vaddpd    ymm14, ymm0, ymm4
        vaddpd    ymm15, ymm15, ymm14
        vaddpd    ymm5, ymm5, ymm15
        vxorpd    ymm0, ymm5, ymm9
        vextractf128 xmm2, ymm1, 1
        vshufps   xmm3, xmm1, xmm2, 221
        vmovmskps r11d, xmm3
        test      r11d, r11d
        jne       _B4_12

_B4_2::

        test      edx, edx
        jne       _B4_4

_B4_3::

        vmovups   ymm6, YMMWORD PTR [848+rbp]
        vmovups   ymm7, YMMWORD PTR [816+rbp]
        vmovups   ymm8, YMMWORD PTR [784+rbp]
        vmovups   ymm9, YMMWORD PTR [752+rbp]
        vmovups   ymm10, YMMWORD PTR [720+rbp]
        vmovups   ymm11, YMMWORD PTR [688+rbp]
        vmovups   ymm12, YMMWORD PTR [656+rbp]
        vmovups   ymm13, YMMWORD PTR [624+rbp]
        vmovups   ymm14, YMMWORD PTR [592+rbp]
        vmovups   ymm15, YMMWORD PTR [560+rbp]
        mov       r13, QWORD PTR [880+rbp]
        lea       rsp, QWORD PTR [896+rbp]
        pop       rbp
        ret

_B4_4::

        vmovupd   YMMWORD PTR [r13], ymm10
        vmovupd   YMMWORD PTR [64+r13], ymm0
        je        _B4_3

_B4_7::

        xor       eax, eax
        mov       QWORD PTR [8+rbp], rbx
        mov       ebx, eax
        mov       QWORD PTR [rbp], rsi
        mov       esi, edx

_B4_8::

        bt        esi, ebx
        jc        _B4_11

_B4_9::

        inc       ebx
        cmp       ebx, 4
        jl        _B4_8

_B4_10::

        mov       rbx, QWORD PTR [8+rbp]
        mov       rsi, QWORD PTR [rbp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        jmp       _B4_3

_B4_11::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dsin_ha_cout_rare_internal
        jmp       _B4_9

_B4_12::

        vmovupd   ymm14, YMMWORD PTR [__svml_dsin_ha_data_internal_ha+16512]
        vmovupd   YMMWORD PTR [32+r13], ymm7
        vmovdqu   xmm7, XMMWORD PTR [_2il0floatpacket_36]
        vmovupd   YMMWORD PTR [r13], ymm8
        vmovupd   YMMWORD PTR [96+r13], ymm11
        vmovupd   YMMWORD PTR [160+r13], ymm13
        vmovupd   YMMWORD PTR [256+r13], ymm0
        vmovupd   YMMWORD PTR [192+r13], ymm9
        vmovupd   YMMWORD PTR [288+r13], ymm10
        vmovupd   YMMWORD PTR [64+r13], ymm6
        vmovupd   YMMWORD PTR [224+r13], ymm1
        vmovupd   YMMWORD PTR [128+r13], ymm12
        vandpd    ymm11, ymm14, ymm8
        vcmpeqpd  ymm9, ymm11, ymm14
        vpand     xmm15, xmm7, xmm8
        vextractf128 xmm13, ymm8, 1
        vpsrlq    xmm3, xmm15, 52
        vmovd     r8d, xmm3
        vmovdqu   xmm15, XMMWORD PTR [_2il0floatpacket_37]
        vpand     xmm4, xmm7, xmm13
        vpsrlq    xmm0, xmm4, 52
        vmovd     r9d, xmm0
        lea       r11d, DWORD PTR [r8+r8*2]
        vmovups   XMMWORD PTR [rbp], xmm8
        vpand     xmm8, xmm15, xmm8
        vmovups   XMMWORD PTR [400+rbp], xmm13
        vpand     xmm13, xmm15, xmm13
        vpextrd   ecx, xmm3, 2
        vpextrd   r8d, xmm0, 2
        lea       r9d, DWORD PTR [r9+r9*2]
        shl       r9d, 3
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r9]
        vmovdqu   xmm7, XMMWORD PTR [_2il0floatpacket_38]
        lea       r10d, DWORD PTR [rcx+rcx*2]
        shl       r11d, 3
        lea       ecx, DWORD PTR [r8+r8*2]
        shl       ecx, 3
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r9]
        shl       r10d, 3
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r11]
        vmovq     xmm5, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r11]
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r11]
        vmovhpd   xmm4, xmm5, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r10]
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r9]
        vmovhpd   xmm3, xmm12, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+rcx]
        vmovupd   XMMWORD PTR [416+rbp], xmm4
        vextractf128 xmm2, ymm9, 1
        vshufps   xmm10, xmm9, xmm2, 221
        vmovmskps edx, xmm10
        vmovhpd   xmm10, xmm11, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+rcx]
        vpaddq    xmm11, xmm8, xmm7
        vpaddq    xmm7, xmm13, xmm7
        vmovhpd   xmm9, xmm6, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+rcx]
        vmovdqu   xmm13, XMMWORD PTR [_2il0floatpacket_39]
        vpsrlq    xmm5, xmm9, 32
        vmovhpd   xmm2, xmm1, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r10]
        vpand     xmm0, xmm9, xmm13
        vmovhpd   xmm1, xmm14, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r10]
        vpand     xmm14, xmm10, xmm13
        vpsrlq    xmm9, xmm11, 32
        vpand     xmm8, xmm11, xmm13
        vpand     xmm11, xmm7, xmm13
        vpsrlq    xmm12, xmm2, 32
        vmovdqu   XMMWORD PTR [464+rbp], xmm14
        vpand     xmm6, xmm2, xmm13
        vpmuludq  xmm14, xmm11, xmm14
        vpsrlq    xmm2, xmm1, 32
        vmovupd   XMMWORD PTR [432+rbp], xmm10
        vpsrlq    xmm10, xmm7, 32
        vmovdqu   XMMWORD PTR [512+rbp], xmm0
        vpand     xmm15, xmm4, xmm13
        vpmuludq  xmm7, xmm11, xmm0
        vpand     xmm0, xmm1, xmm13
        vpmuludq  xmm1, xmm9, xmm0
        vpmuludq  xmm0, xmm9, xmm2
        vpmuludq  xmm2, xmm8, xmm2
        vmovdqu   XMMWORD PTR [496+rbp], xmm5
        vpsrlq    xmm2, xmm2, 32
        vmovdqu   XMMWORD PTR [544+rbp], xmm14
        vpsrlq    xmm4, xmm3, 32
        vpmuludq  xmm14, xmm11, xmm5
        vpand     xmm3, xmm3, xmm13
        vpmuludq  xmm5, xmm8, xmm6
        vpmuludq  xmm6, xmm9, xmm6
        vpaddq    xmm0, xmm0, xmm2
        vmovdqu   XMMWORD PTR [448+rbp], xmm15
        vpmuludq  xmm15, xmm8, xmm15
        vmovdqu   XMMWORD PTR [528+rbp], xmm15
        vpsrlq    xmm15, xmm1, 32
        vpand     xmm1, xmm5, xmm13
        vpsrlq    xmm5, xmm5, 32
        vpaddq    xmm1, xmm1, xmm0
        vpaddq    xmm5, xmm6, xmm5
        vpaddq    xmm2, xmm15, xmm1
        vpmuludq  xmm0, xmm10, xmm4
        vpmuludq  xmm4, xmm11, xmm4
        vpmuludq  xmm15, xmm10, xmm3
        vpsrlq    xmm4, xmm4, 32
        vpand     xmm1, xmm7, xmm13
        vpaddq    xmm3, xmm0, xmm4
        vpaddq    xmm0, xmm1, xmm3
        vmovdqu   XMMWORD PTR [480+rbp], xmm12
        vpsrlq    xmm15, xmm15, 32
        vpmuludq  xmm12, xmm8, xmm12
        vpsrlq    xmm7, xmm7, 32
        vpaddq    xmm4, xmm15, xmm0
        vpmuludq  xmm0, xmm10, XMMWORD PTR [512+rbp]
        vpand     xmm15, xmm12, xmm13
        vpand     xmm1, xmm14, xmm13
        vpaddq    xmm15, xmm15, xmm5
        vpaddq    xmm6, xmm0, xmm7
        vpmuludq  xmm5, xmm9, XMMWORD PTR [480+rbp]
        vpmuludq  xmm9, xmm9, XMMWORD PTR [448+rbp]
        vpaddq    xmm7, xmm1, xmm6
        vpmuludq  xmm1, xmm10, XMMWORD PTR [496+rbp]
        vpmuludq  xmm10, xmm10, XMMWORD PTR [464+rbp]
        vpsrlq    xmm3, xmm2, 32
        vpsrlq    xmm12, xmm12, 32
        vpaddq    xmm15, xmm3, xmm15
        vpaddq    xmm5, xmm5, xmm12
        vpsrlq    xmm3, xmm4, 32
        vpsrlq    xmm6, xmm15, 32
        vpaddq    xmm7, xmm3, xmm7
        vmovdqu   xmm3, XMMWORD PTR [528+rbp]
        vpsrlq    xmm14, xmm14, 32
        vpand     xmm0, xmm3, xmm13
        vpsrlq    xmm3, xmm3, 32
        vpaddq    xmm0, xmm0, xmm5
        vpaddq    xmm1, xmm1, xmm14
        vpaddq    xmm6, xmm6, xmm0
        vpaddq    xmm9, xmm9, xmm3
        vmovdqu   xmm0, XMMWORD PTR [544+rbp]
        vpsrlq    xmm5, xmm7, 32
        vpand     xmm12, xmm0, xmm13
        vpsllq    xmm15, xmm15, 32
        vpaddq    xmm12, xmm12, xmm1
        vmovupd   xmm14, XMMWORD PTR [416+rbp]
        vpand     xmm2, xmm2, xmm13
        vpaddq    xmm1, xmm5, xmm12
        vpsrlq    xmm12, xmm14, 32
        vpsrlq    xmm5, xmm6, 32
        vpmuludq  xmm8, xmm8, xmm12
        vpand     xmm6, xmm6, xmm13
        vpand     xmm8, xmm8, xmm13
        vpsllq    xmm7, xmm7, 32
        vpaddq    xmm8, xmm8, xmm9
        vmovupd   xmm12, XMMWORD PTR [432+rbp]
        vpsrlq    xmm9, xmm1, 32
        vpaddq    xmm5, xmm5, xmm8
        vpsrlq    xmm8, xmm12, 32
        vpsllq    xmm14, xmm5, 32
        vpmuludq  xmm11, xmm11, xmm8
        vpsrlq    xmm8, xmm0, 32
        vpaddq    xmm10, xmm10, xmm8
        vpaddq    xmm6, xmm14, xmm6
        vpand     xmm3, xmm11, xmm13
        vpand     xmm11, xmm1, xmm13
        vpaddq    xmm5, xmm3, xmm10
        vmovupd   ymm1, YMMWORD PTR [_2il0floatpacket_60]
        vpaddq    xmm9, xmm9, xmm5
        vpsllq    xmm0, xmm9, 32
        vpand     xmm13, xmm4, xmm13
        vpaddq    xmm9, xmm15, xmm2
        vpaddq    xmm8, xmm0, xmm11
        vpaddq    xmm7, xmm7, xmm13
        vmovdqu   xmm2, XMMWORD PTR [_2il0floatpacket_40]
        vpsrlq    xmm10, xmm6, 12
        vpand     xmm15, xmm2, XMMWORD PTR [rbp]
        vpsrlq    xmm5, xmm8, 12
        vmovdqu   xmm3, XMMWORD PTR [_2il0floatpacket_41]
        vpand     xmm2, xmm2, XMMWORD PTR [400+rbp]
        vpxor     xmm13, xmm15, xmm3
        vpxor     xmm0, xmm2, xmm3
        vpor      xmm4, xmm10, xmm13
        vpor      xmm11, xmm5, xmm0
        vmovups   xmm3, XMMWORD PTR [_2il0floatpacket_43]
        vmovdqu   xmm0, XMMWORD PTR [_2il0floatpacket_44]
        vinsertf128 ymm14, ymm4, xmm11, 1
        vaddpd    ymm10, ymm14, ymm1
        vsubpd    ymm12, ymm10, ymm1
        vsubpd    ymm5, ymm14, ymm12
        vpxor     xmm12, xmm15, xmm0
        vpxor     xmm14, xmm2, xmm0
        vandps    xmm13, xmm10, xmm3
        vextractf128 xmm4, ymm10, 1
        vmovd     r8d, xmm13
        vandps    xmm10, xmm4, xmm3
        vmovdqu   xmm3, XMMWORD PTR [_2il0floatpacket_45]
        vpand     xmm11, xmm3, xmm9
        vpand     xmm0, xmm3, xmm7
        vpsllq    xmm1, xmm11, 28
        vpsllq    xmm11, xmm0, 28
        vpor      xmm4, xmm1, xmm12
        vpor      xmm1, xmm11, xmm14
        vpsrlq    xmm9, xmm9, 24
        vpsrlq    xmm7, xmm7, 24
        vmovd     r10d, xmm10
        shl       r8d, 5
        vpextrd   ecx, xmm13, 2
        shl       r10d, 5
        vpextrd   r11d, xmm10, 2
        shl       ecx, 5
        shl       r11d, 5
        vinsertf128 ymm4, ymm4, xmm1, 1
        vinsertf128 ymm14, ymm12, xmm14, 1
        vsubpd    ymm3, ymm4, ymm14
        vmovdqu   xmm4, XMMWORD PTR [_2il0floatpacket_46]
        vmovdqu   xmm1, XMMWORD PTR [_2il0floatpacket_47]
        vpxor     xmm0, xmm2, xmm4
        vpand     xmm2, xmm1, xmm6
        vpand     xmm8, xmm1, xmm8
        vpxor     xmm11, xmm15, xmm4
        vpsllq    xmm15, xmm2, 40
        vpsllq    xmm2, xmm8, 40
        vpor      xmm6, xmm15, xmm9
        vpor      xmm2, xmm2, xmm7
        vpor      xmm9, xmm6, xmm11
        vpor      xmm15, xmm2, xmm0
        vinsertf128 ymm8, ymm9, xmm15, 1
        vinsertf128 ymm4, ymm11, xmm0, 1
        vsubpd    ymm0, ymm8, ymm4
        vmovupd   ymm9, YMMWORD PTR [_2il0floatpacket_71]
        vmovupd   ymm11, YMMWORD PTR [_2il0floatpacket_69]
        vaddpd    ymm6, ymm5, ymm0
        vsubpd    ymm5, ymm5, ymm6
        vaddpd    ymm1, ymm0, ymm5
        vaddpd    ymm14, ymm3, ymm1
        vmovupd   ymm3, YMMWORD PTR [_2il0floatpacket_70]
        vmulpd    ymm7, ymm14, YMMWORD PTR [_2il0floatpacket_66]
        vandpd    ymm12, ymm6, YMMWORD PTR [_2il0floatpacket_72]
        vsubpd    ymm2, ymm6, ymm12
        vmulpd    ymm8, ymm9, ymm12
        vmulpd    ymm0, ymm3, ymm12
        vmulpd    ymm15, ymm3, ymm2
        vmulpd    ymm4, ymm9, ymm2
        vmovupd   ymm9, YMMWORD PTR [r13]
        vaddpd    ymm3, ymm15, ymm8
        vaddpd    ymm12, ymm7, ymm4
        vaddpd    ymm1, ymm3, ymm12
        vmovq     xmm3, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r10]
        vaddpd    ymm2, ymm0, ymm1
        vandpd    ymm6, ymm9, YMMWORD PTR [_2il0floatpacket_68]
        vsubpd    ymm5, ymm0, ymm2
        vcmple_oqpd ymm14, ymm6, ymm11
        vcmpgt_oqpd ymm7, ymm6, ymm11
        vaddpd    ymm4, ymm1, ymm5
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r8]
        vmovhpd   xmm13, xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+rcx]
        vandpd    ymm15, ymm14, ymm9
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r10]
        vandpd    ymm8, ymm7, ymm2
        vmovhpd   xmm10, xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r11]
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r8]
        vorpd     ymm15, ymm15, ymm8
        vmovq     xmm8, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r10]
        vandpd    ymm2, ymm7, ymm4
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r8]
        vmovhpd   xmm1, xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+rcx]
        vmovhpd   xmm5, xmm3, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r11]
        vinsertf128 ymm7, ymm13, xmm10, 1
        vmovhpd   xmm10, xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        vmovhpd   xmm13, xmm8, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r11]
        vmulpd    ymm14, ymm15, ymm7
        vinsertf128 ymm4, ymm10, xmm13, 1
        vmulpd    ymm6, ymm15, ymm4
        vinsertf128 ymm8, ymm1, xmm5, 1
        vaddpd    ymm12, ymm8, ymm6
        vaddpd    ymm9, ymm14, ymm12
        vsubpd    ymm11, ymm8, ymm12
        vsubpd    ymm10, ymm12, ymm9
        vaddpd    ymm13, ymm6, ymm11
        vaddpd    ymm0, ymm14, ymm10
        vaddpd    ymm10, ymm7, ymm4
        vaddpd    ymm11, ymm13, ymm0
        vmulpd    ymm13, ymm15, ymm15
        vmulpd    ymm1, ymm13, YMMWORD PTR [32+r13]
        vmulpd    ymm7, ymm13, YMMWORD PTR [96+r13]
        vaddpd    ymm3, ymm1, YMMWORD PTR [64+r13]
        vaddpd    ymm12, ymm7, YMMWORD PTR [128+r13]
        vmulpd    ymm5, ymm13, ymm3
        vmulpd    ymm14, ymm13, ymm12
        vmulpd    ymm6, ymm15, ymm5
        vmulpd    ymm15, ymm15, ymm8
        vmovupd   ymm5, YMMWORD PTR [224+r13]
        vaddpd    ymm7, ymm14, YMMWORD PTR [160+r13]
        vmulpd    ymm4, ymm6, ymm10
        vmulpd    ymm13, ymm13, ymm7
        vaddpd    ymm0, ymm11, ymm4
        vmulpd    ymm1, ymm8, ymm13
        vmovq     xmm8, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r8]
        vaddpd    ymm4, ymm0, ymm1
        vsubpd    ymm1, ymm10, ymm15
        vmovq     xmm15, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r10]
        vmovhpd   xmm10, xmm8, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+rcx]
        vmulpd    ymm2, ymm2, ymm1
        vmovhpd   xmm0, xmm15, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r11]
        vinsertf128 ymm3, ymm10, xmm0, 1
        vaddpd    ymm0, ymm3, ymm2
        vmovupd   ymm10, YMMWORD PTR [288+r13]
        vaddpd    ymm1, ymm4, ymm0
        vmovupd   ymm0, YMMWORD PTR [256+r13]
        vaddpd    ymm2, ymm9, ymm1
        vxorpd    ymm3, ymm2, YMMWORD PTR [192+r13]
        vblendvpd ymm0, ymm0, ymm3, ymm5
        jmp       _B4_2
        ALIGN     16

_B4_13::

__svml_sin4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin4_ha_e9_B1_B4:
	DD	622489857
	DD	7525489
	DD	3631210
	DD	3504226
	DD	3377242
	DD	3250258
	DD	3123274
	DD	2996290
	DD	2869306
	DD	2742322
	DD	2615338
	DD	2488352
	DD	17564433
	DD	1342505076

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_7
	DD	imagerel _unwind___svml_sin4_ha_e9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin4_ha_e9_B7_B11:
	DD	621022241
	DD	287756
	DD	340998
	DD	imagerel _B4_1
	DD	imagerel _B4_7
	DD	imagerel _unwind___svml_sin4_ha_e9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_7
	DD	imagerel _B4_12
	DD	imagerel _unwind___svml_sin4_ha_e9_B7_B11

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin4_ha_e9_B12_B12:
	DD	620757025
	DD	imagerel _B4_1
	DD	imagerel _B4_7
	DD	imagerel _unwind___svml_sin4_ha_e9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_12
	DD	imagerel _B4_13
	DD	imagerel _unwind___svml_sin4_ha_e9_B12_B12

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sin1_ha_ex

__svml_sin1_ha_ex	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L195::

        sub       rsp, 472
        lea       rax, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [352+rsp], xmm15
        xor       edx, edx
        movups    XMMWORD PTR [368+rsp], xmm14
        movaps    xmm14, xmm0
        movups    XMMWORD PTR [384+rsp], xmm13
        movups    XMMWORD PTR [400+rsp], xmm12
        movups    XMMWORD PTR [432+rsp], xmm11
        movups    XMMWORD PTR [448+rsp], xmm10
        movups    XMMWORD PTR [336+rsp], xmm9
        movups    XMMWORD PTR [416+rsp], xmm8
        movups    XMMWORD PTR [320+rsp], xmm7
        movups    XMMWORD PTR [304+rsp], xmm6
        mov       QWORD PTR [296+rsp], r13
        lea       r13, QWORD PTR [223+rsp]
        movsd     xmm2, QWORD PTR [__svml_dsin_ha_data_internal_ha+16384]
        and       r13, -64
        movaps    xmm13, xmm2
        andps     xmm2, xmm14
        movaps    xmm9, xmm2
        movaps    xmm12, xmm2
        mulsd     xmm9, QWORD PTR [__svml_dsin_ha_data_internal_ha+16576]
        andnps    xmm13, xmm14
        movsd     xmm1, QWORD PTR [__svml_dsin_ha_data_internal_ha+16640]
        movq      xmm0, QWORD PTR [__svml_dsin_ha_data_internal_ha+16704]
        addsd     xmm9, xmm1
        movaps    xmm5, xmm9
        pand      xmm9, xmm0
        movd      ecx, xmm9
        subsd     xmm5, xmm1
        movaps    xmm3, xmm5
        movaps    xmm4, xmm5
        mulsd     xmm3, QWORD PTR [__svml_dsin_ha_data_internal_ha+16768]
        mulsd     xmm4, QWORD PTR [__svml_dsin_ha_data_internal_ha+16832]
        mulsd     xmm5, QWORD PTR [__svml_dsin_ha_data_internal_ha+16896]
        subsd     xmm12, xmm3
        movaps    xmm11, xmm12
        shl       ecx, 5
        subsd     xmm11, xmm4
        movaps    xmm1, xmm11
        subsd     xmm12, xmm11
        mulsd     xmm1, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        subsd     xmm12, xmm4
        movq      xmm10, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+rcx]
        subsd     xmm12, xmm5
        movq      xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+rcx]
        movdqa    xmm6, xmm10
        mulsd     xmm6, xmm11
        movdqa    xmm3, xmm9
        addsd     xmm3, xmm1
        addsd     xmm10, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        movaps    xmm0, xmm6
        movdqa    xmm7, xmm9
        movsd     xmm8, QWORD PTR [__svml_dsin_ha_data_internal_ha+16960]
        addsd     xmm0, xmm3
        subsd     xmm7, xmm3
        subsd     xmm3, xmm0
        addsd     xmm7, xmm1
        addsd     xmm3, xmm6
        movaps    xmm6, xmm11
        addsd     xmm3, xmm7
        mulsd     xmm6, xmm11
        movsd     xmm1, QWORD PTR [__svml_dsin_ha_data_internal_ha+17024]
        movaps    xmm7, xmm1
        mulsd     xmm7, xmm6
        movsd     xmm5, QWORD PTR [__svml_dsin_ha_data_internal_ha+17216]
        addsd     xmm7, xmm8
        mulsd     xmm7, xmm6
        movaps    xmm15, xmm5
        mulsd     xmm15, xmm6
        mulsd     xmm7, xmm11
        mulsd     xmm11, xmm9
        mulsd     xmm7, xmm10
        subsd     xmm10, xmm11
        addsd     xmm7, xmm3
        mulsd     xmm12, xmm10
        movsd     xmm4, QWORD PTR [__svml_dsin_ha_data_internal_ha+17152]
        movsd     xmm3, QWORD PTR [__svml_dsin_ha_data_internal_ha+17088]
        addsd     xmm15, xmm4
        addsd     xmm12, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+rcx]
        mulsd     xmm15, xmm6
        mov       QWORD PTR [464+rsp], r13
        addsd     xmm15, xmm3
        mulsd     xmm15, xmm6
        movaps    xmm6, xmm2
        cmpnlesd  xmm6, QWORD PTR [__svml_dsin_ha_data_internal_ha+16448]
        mulsd     xmm15, xmm9
        movmskpd  r8d, xmm6
        addsd     xmm15, xmm7
        addsd     xmm15, xmm12
        addsd     xmm0, xmm15
        pxor      xmm0, xmm13
        test      r8d, 1
        jne       _B5_8

_B5_2::

        test      edx, edx
        jne       _B5_4

_B5_3::

        movups    xmm6, XMMWORD PTR [304+rsp]
        movups    xmm7, XMMWORD PTR [320+rsp]
        movups    xmm8, XMMWORD PTR [416+rsp]
        movups    xmm9, XMMWORD PTR [336+rsp]
        movups    xmm10, XMMWORD PTR [448+rsp]
        movups    xmm11, XMMWORD PTR [432+rsp]
        movups    xmm12, XMMWORD PTR [400+rsp]
        movups    xmm13, XMMWORD PTR [384+rsp]
        movups    xmm14, XMMWORD PTR [368+rsp]
        movups    xmm15, XMMWORD PTR [352+rsp]
        mov       r13, QWORD PTR [296+rsp]
        add       rsp, 472
        ret

_B5_4::

        movsd     QWORD PTR [r13], xmm14
        movsd     QWORD PTR [64+r13], xmm0
        jne       _B5_6

_B5_5::

        movsd     xmm0, QWORD PTR [64+r13]
        jmp       _B5_3

_B5_6::

        je        _B5_5

_B5_7::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dsin_ha_cout_rare_internal
        jmp       _B5_5

_B5_8::

        movups    XMMWORD PTR [32+rsp], xmm8
        movdqu    xmm8, XMMWORD PTR [_2il0floatpacket_36]
        pand      xmm8, xmm2
        psrlq     xmm8, 52
        movd      ecx, xmm8
        movups    XMMWORD PTR [128+rsp], xmm0
        movups    XMMWORD PTR [112+rsp], xmm6
        movaps    xmm6, xmm2
        movsd     xmm0, QWORD PTR [__svml_dsin_ha_data_internal_ha+16512]
        movups    XMMWORD PTR [48+rsp], xmm5
        andps     xmm6, xmm0
        movdqu    xmm5, XMMWORD PTR [_2il0floatpacket_37]
        lea       r8d, DWORD PTR [rcx+rcx*2]
        pand      xmm5, xmm2
        cmpeqsd   xmm6, xmm0
        paddq     xmm5, XMMWORD PTR [_2il0floatpacket_38]
        movmskpd  edx, xmm6
        movups    XMMWORD PTR [96+rsp], xmm13
        movups    XMMWORD PTR [64+rsp], xmm4
        movdqa    xmm4, xmm5
        shl       r8d, 3
        psrlq     xmm4, 32
        movq      xmm13, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r8]
        movdqa    xmm6, xmm4
        movups    XMMWORD PTR [144+rsp], xmm14
        movdqa    xmm0, xmm13
        movdqu    xmm14, XMMWORD PTR [_2il0floatpacket_39]
        psrlq     xmm0, 32
        pand      xmm5, xmm14
        pand      xmm13, xmm14
        movq      xmm11, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r8]
        movdqa    xmm9, xmm5
        pmuludq   xmm6, xmm0
        movdqa    xmm12, xmm11
        pmuludq   xmm0, xmm5
        pmuludq   xmm13, xmm4
        pand      xmm11, xmm14
        psrlq     xmm0, 32
        pmuludq   xmm9, xmm11
        psrlq     xmm12, 32
        pmuludq   xmm11, xmm4
        paddq     xmm6, xmm0
        movdqa    xmm10, xmm5
        movdqa    xmm8, xmm14
        pmuludq   xmm10, xmm12
        pand      xmm8, xmm9
        pmuludq   xmm12, xmm4
        paddq     xmm8, xmm6
        movq      xmm7, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r8]
        movdqa    xmm15, xmm14
        psrlq     xmm13, 32
        psrlq     xmm9, 32
        movups    XMMWORD PTR [80+rsp], xmm3
        pand      xmm15, xmm7
        movdqa    xmm3, xmm5
        movdqa    xmm6, xmm14
        paddq     xmm13, xmm8
        paddq     xmm11, xmm9
        pmuludq   xmm3, xmm15
        pmuludq   xmm4, xmm15
        pand      xmm6, xmm10
        movdqa    xmm0, xmm13
        paddq     xmm6, xmm11
        psrlq     xmm0, 32
        psrlq     xmm10, 32
        psrlq     xmm7, 32
        movdqa    xmm9, xmm14
        paddq     xmm0, xmm6
        paddq     xmm12, xmm10
        pmuludq   xmm5, xmm7
        pand      xmm9, xmm3
        movdqa    xmm11, xmm0
        paddq     xmm9, xmm12
        psrlq     xmm11, 32
        psrlq     xmm3, 32
        paddq     xmm11, xmm9
        paddq     xmm4, xmm3
        pand      xmm5, xmm14
        movdqa    xmm8, xmm11
        paddq     xmm5, xmm4
        psrlq     xmm8, 32
        pand      xmm11, xmm14
        paddq     xmm8, xmm5
        psllq     xmm8, 32
        psllq     xmm0, 32
        paddq     xmm8, xmm11
        movdqu    xmm3, XMMWORD PTR [_2il0floatpacket_40]
        movdqa    xmm4, xmm8
        movdqu    xmm10, XMMWORD PTR [_2il0floatpacket_41]
        pand      xmm3, xmm2
        movups    xmm12, XMMWORD PTR [_2il0floatpacket_42]
        psrlq     xmm4, 12
        pxor      xmm10, xmm3
        movaps    xmm7, xmm12
        por       xmm4, xmm10
        pand      xmm13, xmm14
        paddq     xmm0, xmm13
        addsd     xmm7, xmm4
        movaps    xmm13, xmm7
        and       edx, 1
        movdqu    xmm6, XMMWORD PTR [_2il0floatpacket_47]
        subsd     xmm13, xmm12
        movdqu    xmm5, XMMWORD PTR [_2il0floatpacket_45]
        pand      xmm6, xmm8
        movdqu    xmm14, XMMWORD PTR [_2il0floatpacket_44]
        pand      xmm5, xmm0
        psllq     xmm6, 40
        psrlq     xmm0, 24
        pxor      xmm14, xmm3
        por       xmm6, xmm0
        pxor      xmm3, XMMWORD PTR [_2il0floatpacket_46]
        psllq     xmm5, 28
        por       xmm6, xmm3
        por       xmm5, xmm14
        movups    xmm0, XMMWORD PTR [_2il0floatpacket_51]
        subsd     xmm4, xmm13
        subsd     xmm6, xmm3
        subsd     xmm5, xmm14
        movaps    xmm8, xmm4
        movups    xmm15, XMMWORD PTR [_2il0floatpacket_49]
        addsd     xmm8, xmm6
        andps     xmm0, xmm8
        subsd     xmm4, xmm8
        subsd     xmm8, xmm0
        addsd     xmm6, xmm4
        movups    xmm4, XMMWORD PTR [_2il0floatpacket_50]
        addsd     xmm6, xmm5
        movups    xmm10, XMMWORD PTR [_2il0floatpacket_48]
        movaps    xmm9, xmm4
        mulsd     xmm9, xmm0
        movaps    xmm5, xmm15
        mulsd     xmm15, xmm8
        mulsd     xmm10, xmm6
        mulsd     xmm4, xmm8
        addsd     xmm15, xmm9
        mulsd     xmm5, xmm0
        addsd     xmm10, xmm4
        movups    xmm11, XMMWORD PTR [_2il0floatpacket_53]
        movaps    xmm12, xmm5
        movups    xmm3, XMMWORD PTR [_2il0floatpacket_52]
        movaps    xmm6, xmm11
        andps     xmm3, xmm2
        addsd     xmm15, xmm10
        cmpltsd   xmm6, xmm3
        addsd     xmm12, xmm15
        movaps    xmm4, xmm3
        cmplesd   xmm3, xmm11
        subsd     xmm5, xmm12
        andps     xmm7, XMMWORD PTR [_2il0floatpacket_43]
        movsd     xmm4, xmm6
        movd      r9d, xmm7
        andps     xmm3, xmm2
        andps     xmm12, xmm4
        addsd     xmm5, xmm15
        orps      xmm3, xmm12
        andps     xmm4, xmm5
        shl       r9d, 5
        movaps    xmm2, xmm3
        movaps    xmm7, xmm3
        mulsd     xmm2, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r9]
        mulsd     xmm7, xmm3
        movq      xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r9]
        movq      xmm8, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r9]
        movdqa    xmm0, xmm9
        mulsd     xmm0, xmm3
        movdqa    xmm6, xmm8
        addsd     xmm6, xmm2
        mulsd     xmm1, xmm7
        addsd     xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r9]
        movups    xmm10, XMMWORD PTR [48+rsp]
        movaps    xmm11, xmm0
        mulsd     xmm10, xmm7
        addsd     xmm1, QWORD PTR [32+rsp]
        addsd     xmm11, xmm6
        mulsd     xmm1, xmm7
        addsd     xmm10, QWORD PTR [64+rsp]
        movdqa    xmm5, xmm8
        mulsd     xmm10, xmm7
        subsd     xmm5, xmm6
        subsd     xmm6, xmm11
        mulsd     xmm1, xmm3
        mulsd     xmm3, xmm8
        addsd     xmm10, QWORD PTR [80+rsp]
        addsd     xmm5, xmm2
        mulsd     xmm1, xmm9
        addsd     xmm6, xmm0
        subsd     xmm9, xmm3
        mulsd     xmm10, xmm7
        addsd     xmm6, xmm5
        mulsd     xmm4, xmm9
        mulsd     xmm10, xmm8
        addsd     xmm1, xmm6
        addsd     xmm4, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r9]
        addsd     xmm10, xmm1
        movups    xmm1, XMMWORD PTR [112+rsp]
        addsd     xmm10, xmm4
        movaps    xmm0, xmm1
        addsd     xmm11, xmm10
        xorps     xmm11, XMMWORD PTR [96+rsp]
        andnps    xmm0, XMMWORD PTR [128+rsp]
        andps     xmm11, xmm1
        movups    xmm14, XMMWORD PTR [144+rsp]
        orps      xmm0, xmm11
        jmp       _B5_2
        ALIGN     16

_B5_9::

__svml_sin1_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin1_ha_ex_B1_B8:
	DD	1603585
	DD	2479224
	DD	1271920
	DD	1341544
	DD	1738848
	DD	1415255
	DD	1878094
	DD	1816645
	DD	1689660
	DD	1628211
	DD	1566758
	DD	1505307
	DD	3866891

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_9
	DD	imagerel _unwind___svml_sin1_ha_ex_B1_B8

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sin2_ha_l9

__svml_sin2_ha_l9	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L234::

        sub       rsp, 488
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [304+rsp], xmm15
        xor       edx, edx
        vmovups   XMMWORD PTR [320+rsp], xmm14
        vmovups   XMMWORD PTR [336+rsp], xmm13
        vmovups   XMMWORD PTR [352+rsp], xmm12
        vmovups   XMMWORD PTR [368+rsp], xmm11
        vmovups   XMMWORD PTR [384+rsp], xmm10
        vmovups   XMMWORD PTR [400+rsp], xmm9
        vmovups   XMMWORD PTR [416+rsp], xmm8
        vmovapd   xmm8, xmm0
        vmovups   XMMWORD PTR [432+rsp], xmm7
        vmovups   XMMWORD PTR [448+rsp], xmm6
        mov       QWORD PTR [464+rsp], r13
        lea       r13, QWORD PTR [223+rsp]
        vmovupd   xmm1, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16384]
        and       r13, -64
        vmovupd   xmm13, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16576]
        vandnpd   xmm7, xmm1, xmm8
        vmovupd   xmm2, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16640]
        vandpd    xmm1, xmm8, xmm1
        vfmadd213pd xmm13, xmm1, xmm2
        vmovupd   xmm15, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16768]
        vmovupd   xmm14, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16832]
        vsubpd    xmm6, xmm13, xmm2
        vfnmadd213pd xmm15, xmm6, xmm1
        vmovapd   xmm3, xmm6
        vandps    xmm2, xmm13, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16704]
        vmovd     ecx, xmm2
        vfnmadd213pd xmm3, xmm14, xmm15
        vsubpd    xmm9, xmm15, xmm3
        shl       ecx, 5
        vmovapd   xmm0, xmm3
        vpextrd   r8d, xmm2, 2
        shl       r8d, 5
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        vmovq     xmm5, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+rcx]
        vfnmadd231pd xmm9, xmm14, xmm6
        vmovhpd   xmm14, xmm12, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r8]
        vmovhpd   xmm5, xmm5, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r8]
        vmovq     xmm4, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+rcx]
        vmovhpd   xmm15, xmm4, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r8]
        vfmadd213pd xmm0, xmm14, xmm5
        vfnmadd132pd xmm6, xmm9, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16896]
        vmovupd   xmm9, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16960]
        vmovupd   xmm2, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+17024]
        vmovapd   xmm4, xmm0
        vfmadd231pd xmm4, xmm3, xmm15
        vsubpd    xmm10, xmm5, xmm0
        vsubpd    xmm11, xmm0, xmm4
        vmulpd    xmm0, xmm3, xmm3
        vfmadd231pd xmm10, xmm14, xmm3
        vmovapd   xmm12, xmm0
        vfmadd231pd xmm11, xmm3, xmm15
        vfmadd213pd xmm12, xmm2, xmm9
        vaddpd    xmm13, xmm10, xmm11
        vaddpd    xmm11, xmm15, xmm14
        vmulpd    xmm10, xmm0, xmm12
        vmulpd    xmm10, xmm3, xmm10
        vfnmadd213pd xmm3, xmm5, xmm11
        vmovupd   xmm15, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+17216]
        vmovapd   xmm12, xmm0
        vmovupd   xmm14, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+17152]
        vfmadd213pd xmm12, xmm15, xmm14
        vfmadd213pd xmm10, xmm11, xmm13
        vmovupd   xmm13, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+17088]
        vfmadd213pd xmm12, xmm0, xmm13
        vmulpd    xmm12, xmm0, xmm12
        vfmadd213pd xmm12, xmm5, xmm10
        vmovq     xmm5, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+rcx]
        vmovhpd   xmm0, xmm5, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r8]
        vfmadd213pd xmm6, xmm3, xmm0
        vaddpd    xmm3, xmm12, xmm6
        vaddpd    xmm0, xmm4, xmm3
        vcmpnlepd xmm3, xmm1, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16448]
        vmovmskpd r9d, xmm3
        vxorpd    xmm0, xmm0, xmm7
        mov       QWORD PTR [472+rsp], r13
        test      r9d, r9d
        jne       _B6_12

_B6_2::

        test      edx, edx
        jne       _B6_4

_B6_3::

        vmovups   xmm6, XMMWORD PTR [448+rsp]
        vmovups   xmm7, XMMWORD PTR [432+rsp]
        vmovups   xmm8, XMMWORD PTR [416+rsp]
        vmovups   xmm9, XMMWORD PTR [400+rsp]
        vmovups   xmm10, XMMWORD PTR [384+rsp]
        vmovups   xmm11, XMMWORD PTR [368+rsp]
        vmovups   xmm12, XMMWORD PTR [352+rsp]
        vmovups   xmm13, XMMWORD PTR [336+rsp]
        vmovups   xmm14, XMMWORD PTR [320+rsp]
        vmovups   xmm15, XMMWORD PTR [304+rsp]
        mov       r13, QWORD PTR [464+rsp]
        add       rsp, 488
        ret

_B6_4::

        vmovupd   XMMWORD PTR [r13], xmm8
        vmovupd   XMMWORD PTR [64+r13], xmm0
        je        _B6_3

_B6_7::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B6_8::

        bt        esi, ebx
        jc        _B6_11

_B6_9::

        inc       ebx
        cmp       ebx, 2
        jl        _B6_8

_B6_10::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B6_3

_B6_11::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dsin_ha_cout_rare_internal
        jmp       _B6_9

_B6_12::

        vpand     xmm11, xmm1, XMMWORD PTR [_2il0floatpacket_36]
        vpsrlq    xmm12, xmm11, 52
        vmovd     ecx, xmm12
        vmovupd   XMMWORD PTR [32+rsp], xmm9
        vmovupd   xmm9, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16512]
        vpextrd   r9d, xmm12, 2
        vpand     xmm4, xmm1, XMMWORD PTR [_2il0floatpacket_37]
        lea       r8d, DWORD PTR [rcx+rcx*2]
        shl       r8d, 3
        vmovupd   XMMWORD PTR [48+rsp], xmm15
        vmovupd   XMMWORD PTR [64+rsp], xmm14
        lea       r10d, DWORD PTR [r9+r9*2]
        shl       r10d, 3
        vandpd    xmm14, xmm9, xmm1
        vmovq     xmm5, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r8]
        vpaddq    xmm15, xmm4, XMMWORD PTR [_2il0floatpacket_38]
        vcmpeqpd  xmm10, xmm14, xmm9
        vmovupd   XMMWORD PTR [96+rsp], xmm7
        vpsrlq    xmm11, xmm15, 32
        vmovupd   XMMWORD PTR [144+rsp], xmm8
        vmovq     xmm8, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r8]
        vmovhpd   xmm7, xmm5, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r10]
        vmovdqu   xmm14, XMMWORD PTR [_2il0floatpacket_39]
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r8]
        vmovupd   XMMWORD PTR [128+rsp], xmm0
        vpsrlq    xmm0, xmm7, 32
        vmovhpd   xmm12, xmm8, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r10]
        vpand     xmm8, xmm15, xmm14
        vmovupd   XMMWORD PTR [112+rsp], xmm3
        vpand     xmm9, xmm12, xmm14
        vmovhpd   xmm3, xmm6, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r10]
        vpsrlq    xmm12, xmm12, 32
        vmovupd   XMMWORD PTR [80+rsp], xmm13
        vpand     xmm13, xmm7, xmm14
        vpmuludq  xmm7, xmm11, xmm0
        vpand     xmm4, xmm3, xmm14
        vpmuludq  xmm0, xmm8, xmm0
        vpmuludq  xmm15, xmm11, xmm13
        vmovmskpd edx, xmm10
        vpmuludq  xmm10, xmm8, xmm9
        vpmuludq  xmm9, xmm11, xmm9
        vpsrlq    xmm6, xmm3, 32
        vpsrlq    xmm0, xmm0, 32
        vpmuludq  xmm3, xmm8, xmm4
        vpsrlq    xmm15, xmm15, 32
        vpmuludq  xmm4, xmm11, xmm4
        vpmuludq  xmm5, xmm8, xmm6
        vpmuludq  xmm8, xmm8, xmm12
        vpaddq    xmm7, xmm7, xmm0
        vpand     xmm13, xmm3, xmm14
        vpsrlq    xmm3, xmm3, 32
        vpaddq    xmm13, xmm13, xmm7
        vpaddq    xmm7, xmm4, xmm3
        vpaddq    xmm13, xmm15, xmm13
        vpand     xmm0, xmm5, xmm14
        vpsrlq    xmm15, xmm13, 32
        vpaddq    xmm4, xmm0, xmm7
        vpaddq    xmm7, xmm15, xmm4
        vpmuludq  xmm15, xmm11, xmm6
        vpsrlq    xmm6, xmm5, 32
        vpand     xmm3, xmm10, xmm14
        vpaddq    xmm5, xmm15, xmm6
        vpaddq    xmm15, xmm3, xmm5
        vpsrlq    xmm10, xmm10, 32
        vpsrlq    xmm4, xmm7, 32
        vpaddq    xmm11, xmm9, xmm10
        vpaddq    xmm6, xmm4, xmm15
        vpand     xmm15, xmm8, xmm14
        vpsrlq    xmm5, xmm6, 32
        vpaddq    xmm15, xmm15, xmm11
        vpaddq    xmm9, xmm5, xmm15
        vpsllq    xmm10, xmm9, 32
        vpand     xmm11, xmm6, xmm14
        vpaddq    xmm12, xmm10, xmm11
        vpand     xmm8, xmm1, XMMWORD PTR [_2il0floatpacket_40]
        vpsllq    xmm7, xmm7, 32
        vpand     xmm14, xmm13, xmm14
        vpsrlq    xmm15, xmm12, 12
        vpaddq    xmm11, xmm7, xmm14
        vpxor     xmm7, xmm8, XMMWORD PTR [_2il0floatpacket_41]
        vmovupd   xmm13, XMMWORD PTR [_2il0floatpacket_42]
        vpor      xmm14, xmm15, xmm7
        vaddpd    xmm0, xmm14, xmm13
        vsubpd    xmm9, xmm0, xmm13
        vpand     xmm12, xmm12, XMMWORD PTR [_2il0floatpacket_47]
        vsubpd    xmm10, xmm14, xmm9
        vpxor     xmm6, xmm8, XMMWORD PTR [_2il0floatpacket_44]
        vpand     xmm3, xmm11, XMMWORD PTR [_2il0floatpacket_45]
        vpsrlq    xmm11, xmm11, 24
        vpxor     xmm14, xmm8, XMMWORD PTR [_2il0floatpacket_46]
        vpsllq    xmm8, xmm12, 40
        vpor      xmm7, xmm8, xmm11
        vpsllq    xmm4, xmm3, 28
        vpor      xmm13, xmm7, xmm14
        vpor      xmm5, xmm4, xmm6
        vandps    xmm9, xmm0, XMMWORD PTR [_2il0floatpacket_43]
        vsubpd    xmm0, xmm13, xmm14
        vsubpd    xmm15, xmm5, xmm6
        vmovd     r11d, xmm9
        vaddpd    xmm3, xmm10, xmm0
        vsubpd    xmm10, xmm10, xmm3
        vmovupd   xmm13, XMMWORD PTR [_2il0floatpacket_48]
        vmulpd    xmm5, xmm13, xmm3
        vaddpd    xmm7, xmm0, xmm10
        vaddpd    xmm8, xmm15, xmm7
        vmovapd   xmm15, xmm13
        vfmsub213pd xmm15, xmm3, xmm5
        vmovupd   xmm0, XMMWORD PTR [_2il0floatpacket_53]
        vandpd    xmm14, xmm1, XMMWORD PTR [_2il0floatpacket_52]
        vmovupd   xmm11, XMMWORD PTR [112+rsp]
        vcmpgtpd  xmm6, xmm14, xmm0
        vcmplepd  xmm4, xmm14, xmm0
        vfmadd132pd xmm3, xmm15, XMMWORD PTR [_2il0floatpacket_73]
        vandpd    xmm1, xmm4, xmm1
        vandpd    xmm15, xmm6, xmm5
        shl       r11d, 5
        vorpd     xmm7, xmm1, xmm15
        vpextrd   ecx, xmm9, 2
        vmovapd   xmm9, xmm7
        shl       ecx, 5
        vmovapd   xmm15, xmm7
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r11]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r11]
        vfmadd213pd xmm8, xmm13, xmm3
        vmovq     xmm3, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r11]
        vmovhpd   xmm14, xmm1, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+rcx]
        vmovhpd   xmm0, xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        vmovhpd   xmm1, xmm3, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+rcx]
        vfmadd213pd xmm9, xmm0, xmm1
        vandpd    xmm13, xmm6, xmm8
        vmulpd    xmm5, xmm7, xmm7
        vfmadd132pd xmm15, xmm9, xmm14
        vsubpd    xmm12, xmm1, xmm9
        vsubpd    xmm3, xmm9, xmm15
        vfmadd231pd xmm12, xmm0, xmm7
        vmovupd   xmm9, XMMWORD PTR [32+rsp]
        vfmadd231pd xmm9, xmm2, xmm5
        vfmadd231pd xmm3, xmm7, xmm14
        vmulpd    xmm2, xmm5, xmm9
        vaddpd    xmm4, xmm12, xmm3
        vmulpd    xmm3, xmm7, xmm2
        vaddpd    xmm2, xmm14, xmm0
        vmovupd   xmm14, XMMWORD PTR [64+rsp]
        vfmadd231pd xmm14, xmm5, XMMWORD PTR [48+rsp]
        vfmadd213pd xmm3, xmm2, xmm4
        vfnmadd213pd xmm7, xmm1, xmm2
        vmovupd   xmm0, XMMWORD PTR [128+rsp]
        vfmadd213pd xmm14, xmm5, XMMWORD PTR [80+rsp]
        vmulpd    xmm8, xmm5, xmm14
        vfmadd213pd xmm8, xmm1, xmm3
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r11]
        vmovhpd   xmm6, xmm1, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+rcx]
        vfmadd213pd xmm13, xmm7, xmm6
        vaddpd    xmm7, xmm8, xmm13
        vaddpd    xmm9, xmm15, xmm7
        vxorpd    xmm10, xmm9, XMMWORD PTR [96+rsp]
        vmovupd   xmm8, XMMWORD PTR [144+rsp]
        vblendvpd xmm0, xmm0, xmm10, xmm11
        jmp       _B6_2
        ALIGN     16

_B6_13::

__svml_sin2_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin2_ha_l9_B1_B4:
	DD	1604097
	DD	3855482
	DD	1861746
	DD	1800297
	DD	1738844
	DD	1677395
	DD	1615946
	DD	1554497
	DD	1493048
	DD	1431599
	DD	1370150
	DD	1308699
	DD	3997963

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_sin2_ha_l9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin2_ha_l9_B7_B11:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_sin2_ha_l9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_7
	DD	imagerel _B6_12
	DD	imagerel _unwind___svml_sin2_ha_l9_B7_B11

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin2_ha_l9_B12_B12:
	DD	33
	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_sin2_ha_l9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_12
	DD	imagerel _B6_13
	DD	imagerel _unwind___svml_sin2_ha_l9_B12_B12

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sin1_ha_l9

__svml_sin1_ha_l9	PROC	EXPORT

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L277::

        sub       rsp, 472
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [304+rsp], xmm15
        xor       edx, edx
        vmovups   XMMWORD PTR [320+rsp], xmm14
        vmovups   XMMWORD PTR [336+rsp], xmm13
        vmovups   XMMWORD PTR [352+rsp], xmm12
        vmovups   XMMWORD PTR [368+rsp], xmm11
        vmovapd   xmm11, xmm0
        vmovups   XMMWORD PTR [384+rsp], xmm10
        vmovups   XMMWORD PTR [400+rsp], xmm9
        vmovups   XMMWORD PTR [416+rsp], xmm8
        vmovups   XMMWORD PTR [432+rsp], xmm7
        vmovups   XMMWORD PTR [448+rsp], xmm6
        mov       QWORD PTR [296+rsp], r13
        lea       r13, QWORD PTR [223+rsp]
        vmovsd    xmm2, QWORD PTR [__svml_dsin_ha_data_internal_ha+16384]
        and       r13, -64
        vandpd    xmm13, xmm11, xmm2
        vandnpd   xmm12, xmm2, xmm11
        vmovsd    xmm3, QWORD PTR [__svml_dsin_ha_data_internal_ha+16640]
        vmovapd   xmm8, xmm13
        vfmadd132sd xmm8, xmm3, QWORD PTR [__svml_dsin_ha_data_internal_ha+16576]
        vmovsd    xmm6, QWORD PTR [__svml_dsin_ha_data_internal_ha+16832]
        mov       QWORD PTR [464+rsp], r13
        vsubsd    xmm9, xmm8, xmm3
        vmovapd   xmm1, xmm9
        vmovapd   xmm10, xmm9
        vfnmadd132sd xmm1, xmm13, QWORD PTR [__svml_dsin_ha_data_internal_ha+16768]
        vmovapd   xmm2, xmm9
        vmovq     xmm3, QWORD PTR [__svml_dsin_ha_data_internal_ha+16704]
        vpand     xmm7, xmm8, xmm3
        vmovd     ecx, xmm7
        vfnmadd213sd xmm10, xmm6, xmm1
        shl       ecx, 5
        vmovapd   xmm4, xmm10
        vmovq     xmm7, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+rcx]
        vsubsd    xmm14, xmm1, xmm10
        vfmadd132sd xmm4, xmm7, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+rcx]
        vmovdqa   xmm8, xmm1
        vmovdqa   xmm15, xmm1
        vfmadd213sd xmm8, xmm10, xmm4
        vsubsd    xmm0, xmm7, xmm4
        vfnmadd213sd xmm2, xmm6, xmm14
        vmovapd   xmm6, xmm10
        vsubsd    xmm5, xmm4, xmm8
        vfmadd132sd xmm6, xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        vfnmadd132sd xmm9, xmm2, QWORD PTR [__svml_dsin_ha_data_internal_ha+16896]
        vaddsd    xmm0, xmm1, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        vfmadd213sd xmm15, xmm10, xmm5
        vmulsd    xmm5, xmm10, xmm10
        vaddsd    xmm15, xmm15, xmm6
        vmovsd    xmm14, QWORD PTR [__svml_dsin_ha_data_internal_ha+17024]
        vmovsd    xmm6, QWORD PTR [__svml_dsin_ha_data_internal_ha+16960]
        vmovapd   xmm2, xmm14
        vfmadd213sd xmm2, xmm5, xmm6
        vmovsd    xmm1, QWORD PTR [__svml_dsin_ha_data_internal_ha+17088]
        vmulsd    xmm3, xmm2, xmm5
        vmulsd    xmm4, xmm3, xmm10
        vfnmadd213sd xmm10, xmm7, xmm0
        vmovsd    xmm3, QWORD PTR [__svml_dsin_ha_data_internal_ha+17216]
        vmovsd    xmm2, QWORD PTR [__svml_dsin_ha_data_internal_ha+17152]
        vfmadd213sd xmm4, xmm0, xmm15
        vmovapd   xmm15, xmm3
        vfmadd213sd xmm15, xmm5, xmm2
        vfmadd213sd xmm9, xmm10, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+rcx]
        vfmadd213sd xmm15, xmm5, xmm1
        vmulsd    xmm5, xmm15, xmm5
        vfmadd213sd xmm5, xmm7, xmm4
        vaddsd    xmm0, xmm5, xmm9
        vaddsd    xmm4, xmm8, xmm0
        vxorpd    xmm0, xmm4, xmm12
        vcmpnlesd xmm4, xmm13, QWORD PTR [__svml_dsin_ha_data_internal_ha+16448]
        vmovmskpd r8d, xmm4
        test      r8d, 1
        jne       _B7_8

_B7_2::

        test      edx, edx
        jne       _B7_4

_B7_3::

        vmovups   xmm6, XMMWORD PTR [448+rsp]
        vmovups   xmm7, XMMWORD PTR [432+rsp]
        vmovups   xmm8, XMMWORD PTR [416+rsp]
        vmovups   xmm9, XMMWORD PTR [400+rsp]
        vmovups   xmm10, XMMWORD PTR [384+rsp]
        vmovups   xmm11, XMMWORD PTR [368+rsp]
        vmovups   xmm12, XMMWORD PTR [352+rsp]
        vmovups   xmm13, XMMWORD PTR [336+rsp]
        vmovups   xmm14, XMMWORD PTR [320+rsp]
        vmovups   xmm15, XMMWORD PTR [304+rsp]
        mov       r13, QWORD PTR [296+rsp]
        add       rsp, 472
        ret

_B7_4::

        vmovsd    QWORD PTR [r13], xmm11
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B7_6

_B7_5::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B7_3

_B7_6::

        je        _B7_5

_B7_7::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dsin_ha_cout_rare_internal
        jmp       _B7_5

_B7_8::

        vmovupd   XMMWORD PTR [96+rsp], xmm12
        vpand     xmm12, xmm13, XMMWORD PTR [_2il0floatpacket_36]
        vpsrlq    xmm9, xmm12, 52
        vmovsd    xmm15, QWORD PTR [__svml_dsin_ha_data_internal_ha+16512]
        vmovd     ecx, xmm9
        vandpd    xmm10, xmm15, xmm13
        vmovupd   XMMWORD PTR [144+rsp], xmm11
        vcmpeqsd  xmm11, xmm10, xmm15
        vpand     xmm8, xmm13, XMMWORD PTR [_2il0floatpacket_37]
        lea       r8d, DWORD PTR [rcx+rcx*2]
        shl       r8d, 3
        vmovupd   XMMWORD PTR [48+rsp], xmm3
        vpaddq    xmm3, xmm8, XMMWORD PTR [_2il0floatpacket_38]
        vmovmskpd edx, xmm11
        vmovupd   XMMWORD PTR [32+rsp], xmm6
        vpsrlq    xmm9, xmm3, 32
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r8]
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r8]
        and       edx, 1
        vmovdqu   xmm12, XMMWORD PTR [_2il0floatpacket_39]
        vmovupd   XMMWORD PTR [64+rsp], xmm2
        vpand     xmm2, xmm6, xmm12
        vmovupd   XMMWORD PTR [112+rsp], xmm0
        vpsrlq    xmm0, xmm11, 32
        vmovupd   XMMWORD PTR [128+rsp], xmm4
        vpsrlq    xmm4, xmm6, 32
        vpand     xmm6, xmm3, xmm12
        vpand     xmm5, xmm11, xmm12
        vpmuludq  xmm11, xmm9, xmm0
        vpmuludq  xmm0, xmm6, xmm0
        vpmuludq  xmm15, xmm9, xmm5
        vpmuludq  xmm3, xmm6, xmm4
        vpmuludq  xmm4, xmm9, xmm4
        vmovupd   XMMWORD PTR [80+rsp], xmm1
        vpsrlq    xmm0, xmm0, 32
        vpmuludq  xmm1, xmm6, xmm2
        vpsrlq    xmm15, xmm15, 32
        vpmuludq  xmm2, xmm9, xmm2
        vpaddq    xmm11, xmm11, xmm0
        vpand     xmm5, xmm1, xmm12
        vpsrlq    xmm1, xmm1, 32
        vpaddq    xmm5, xmm5, xmm11
        vpaddq    xmm11, xmm15, xmm5
        vpaddq    xmm5, xmm2, xmm1
        vpand     xmm0, xmm3, xmm12
        vpsrlq    xmm15, xmm11, 32
        vmovq     xmm10, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r8]
        vpand     xmm11, xmm11, xmm12
        vpaddq    xmm2, xmm0, xmm5
        vpand     xmm7, xmm10, xmm12
        vpsrlq    xmm10, xmm10, 32
        vpmuludq  xmm8, xmm6, xmm7
        vpmuludq  xmm7, xmm9, xmm7
        vpaddq    xmm5, xmm15, xmm2
        vpsrlq    xmm15, xmm3, 32
        vpand     xmm1, xmm8, xmm12
        vpaddq    xmm4, xmm4, xmm15
        vpmuludq  xmm15, xmm6, xmm10
        vpaddq    xmm3, xmm1, xmm4
        vpand     xmm6, xmm15, xmm12
        vpsrlq    xmm15, xmm8, 32
        vpsrlq    xmm2, xmm5, 32
        vpsllq    xmm5, xmm5, 32
        vpaddq    xmm9, xmm7, xmm15
        vpaddq    xmm4, xmm2, xmm3
        vpaddq    xmm8, xmm6, xmm9
        vpsrlq    xmm3, xmm4, 32
        vpand     xmm1, xmm4, xmm12
        vpaddq    xmm7, xmm3, xmm8
        vpaddq    xmm8, xmm5, xmm11
        vpsllq    xmm0, xmm7, 32
        vpaddq    xmm6, xmm0, xmm1
        vpand     xmm7, xmm13, XMMWORD PTR [_2il0floatpacket_40]
        vpsrlq    xmm12, xmm6, 12
        vpxor     xmm2, xmm7, XMMWORD PTR [_2il0floatpacket_41]
        vmovupd   xmm3, XMMWORD PTR [_2il0floatpacket_42]
        vpor      xmm4, xmm12, xmm2
        vpand     xmm6, xmm6, XMMWORD PTR [_2il0floatpacket_47]
        vaddsd    xmm9, xmm3, xmm4
        vpand     xmm15, xmm8, XMMWORD PTR [_2il0floatpacket_45]
        vpsllq    xmm1, xmm6, 40
        vpsrlq    xmm8, xmm8, 24
        vpsllq    xmm11, xmm15, 28
        vpor      xmm2, xmm1, xmm8
        vsubsd    xmm10, xmm9, xmm3
        vpxor     xmm0, xmm7, XMMWORD PTR [_2il0floatpacket_44]
        vsubsd    xmm5, xmm4, xmm10
        vpxor     xmm4, xmm7, XMMWORD PTR [_2il0floatpacket_46]
        vpor      xmm12, xmm11, xmm0
        vpor      xmm3, xmm2, xmm4
        vsubsd    xmm15, xmm12, xmm0
        vsubsd    xmm7, xmm3, xmm4
        vmovupd   xmm11, XMMWORD PTR [_2il0floatpacket_48]
        vaddsd    xmm12, xmm5, xmm7
        vmulsd    xmm3, xmm12, xmm11
        vsubsd    xmm5, xmm5, xmm12
        vmovupd   xmm1, XMMWORD PTR [_2il0floatpacket_53]
        vaddsd    xmm10, xmm7, xmm5
        vandpd    xmm0, xmm13, XMMWORD PTR [_2il0floatpacket_52]
        vaddsd    xmm6, xmm10, xmm15
        vcmpgtsd  xmm4, xmm0, xmm1
        vcmplesd  xmm2, xmm0, xmm1
        vmovapd   xmm15, xmm12
        vandpd    xmm13, xmm2, xmm13
        vfmsub213sd xmm15, xmm11, xmm3
        vmovupd   xmm10, XMMWORD PTR [128+rsp]
        vfmadd132sd xmm12, xmm15, QWORD PTR [_2il0floatpacket_73]
        vandpd    xmm15, xmm4, xmm3
        vfmadd213sd xmm6, xmm11, xmm12
        vorpd     xmm11, xmm13, xmm15
        vandps    xmm13, xmm9, XMMWORD PTR [_2il0floatpacket_43]
        vmovapd   xmm0, xmm11
        vmovapd   xmm2, xmm11
        vmovd     r9d, xmm13
        vmulsd    xmm5, xmm11, xmm11
        shl       r9d, 5
        vandpd    xmm12, xmm4, xmm6
        vmovq     xmm13, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r9]
        vfmadd132sd xmm0, xmm13, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r9]
        vmovq     xmm3, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r9]
        vmovdqa   xmm15, xmm3
        vmovdqa   xmm1, xmm3
        vfmadd213sd xmm14, xmm5, QWORD PTR [32+rsp]
        vfmadd213sd xmm15, xmm11, xmm0
        vsubsd    xmm9, xmm13, xmm0
        vmulsd    xmm14, xmm14, xmm5
        vfmadd132sd xmm2, xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r9]
        vsubsd    xmm9, xmm0, xmm15
        vmovupd   xmm0, XMMWORD PTR [48+rsp]
        vfmadd213sd xmm0, xmm5, QWORD PTR [64+rsp]
        vfmadd213sd xmm1, xmm11, xmm9
        vfmadd213sd xmm0, xmm5, QWORD PTR [80+rsp]
        vaddsd    xmm4, xmm1, xmm2
        vmulsd    xmm1, xmm14, xmm11
        vaddsd    xmm2, xmm3, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r9]
        vmulsd    xmm6, xmm0, xmm5
        vfmadd213sd xmm1, xmm2, xmm4
        vfnmadd213sd xmm11, xmm13, xmm2
        vmovupd   xmm0, XMMWORD PTR [112+rsp]
        vfmadd213sd xmm6, xmm13, xmm1
        vfmadd213sd xmm12, xmm11, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r9]
        vmovupd   xmm11, XMMWORD PTR [144+rsp]
        vaddsd    xmm7, xmm6, xmm12
        vaddsd    xmm8, xmm15, xmm7
        vxorpd    xmm9, xmm8, XMMWORD PTR [96+rsp]
        vblendvpd xmm0, xmm0, xmm9, xmm10
        jmp       _B7_2
        ALIGN     16

_B7_9::

__svml_sin1_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin1_ha_l9_B1_B8:
	DD	1604097
	DD	2479226
	DD	1861746
	DD	1800297
	DD	1738848
	DD	1677399
	DD	1615950
	DD	1554497
	DD	1493048
	DD	1431599
	DD	1370150
	DD	1308699
	DD	3866891

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_9
	DD	imagerel _unwind___svml_sin1_ha_l9_B1_B8

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST7:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sin2_ha_ex

__svml_sin2_ha_ex	PROC	EXPORT

_B8_1::

        DB        243
        DB        15
        DB        30
        DB        250
L316::

        sub       rsp, 488
        lea       rax, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [304+rsp], xmm15
        xor       edx, edx
        movups    XMMWORD PTR [320+rsp], xmm14
        movaps    xmm14, xmm0
        movups    XMMWORD PTR [336+rsp], xmm13
        movups    XMMWORD PTR [352+rsp], xmm12
        movups    XMMWORD PTR [368+rsp], xmm11
        movups    XMMWORD PTR [384+rsp], xmm10
        movups    XMMWORD PTR [400+rsp], xmm9
        movups    XMMWORD PTR [416+rsp], xmm8
        movups    XMMWORD PTR [432+rsp], xmm7
        movups    XMMWORD PTR [448+rsp], xmm6
        mov       QWORD PTR [464+rsp], r13
        lea       r13, QWORD PTR [223+rsp]
        movups    xmm1, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16384]
        and       r13, -64
        movups    xmm3, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16576]
        movaps    xmm13, xmm1
        andps     xmm1, xmm14
        andnps    xmm13, xmm14
        mulpd     xmm3, xmm1
        movups    xmm2, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16640]
        movaps    xmm12, xmm1
        addpd     xmm3, xmm2
        movaps    xmm10, xmm3
        subpd     xmm10, xmm2
        movups    xmm4, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16768]
        mulpd     xmm4, xmm10
        movups    xmm5, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16832]
        mulpd     xmm5, xmm10
        subpd     xmm12, xmm4
        movaps    xmm11, xmm12
        andps     xmm3, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16704]
        subpd     xmm11, xmm5
        pextrw    r8d, xmm3, 4
        movd      ecx, xmm3
        subpd     xmm12, xmm11
        shl       ecx, 5
        movaps    xmm8, xmm11
        shl       r8d, 5
        movaps    xmm4, xmm11
        movq      xmm3, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        movaps    xmm6, xmm11
        movhpd    xmm3, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r8]
        mulpd     xmm8, xmm3
        subpd     xmm12, xmm5
        mulpd     xmm6, xmm11
        movq      xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+rcx]
        movups    xmm0, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16896]
        movhpd    xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r8]
        mulpd     xmm0, xmm10
        movq      xmm10, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+rcx]
        movaps    xmm7, xmm9
        movhpd    xmm10, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r8]
        movaps    xmm2, xmm9
        addpd     xmm7, xmm8
        subpd     xmm12, xmm0
        mulpd     xmm4, xmm10
        subpd     xmm2, xmm7
        addpd     xmm10, xmm3
        addpd     xmm8, xmm2
        movaps    xmm0, xmm7
        addpd     xmm0, xmm4
        movups    xmm2, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+17024]
        movaps    xmm15, xmm2
        mulpd     xmm15, xmm6
        subpd     xmm7, xmm0
        addpd     xmm4, xmm7
        movups    xmm7, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16960]
        addpd     xmm15, xmm7
        addpd     xmm8, xmm4
        mulpd     xmm15, xmm6
        mulpd     xmm15, xmm11
        mulpd     xmm15, xmm10
        movups    xmm5, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+17216]
        addpd     xmm8, xmm15
        movaps    xmm15, xmm5
        mulpd     xmm15, xmm6
        movups    xmm4, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+17152]
        addpd     xmm15, xmm4
        mulpd     xmm15, xmm6
        movups    xmm3, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+17088]
        addpd     xmm15, xmm3
        mulpd     xmm6, xmm15
        mulpd     xmm6, xmm9
        mulpd     xmm9, xmm11
        addpd     xmm8, xmm6
        subpd     xmm10, xmm9
        mulpd     xmm12, xmm10
        movq      xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+rcx]
        movaps    xmm6, xmm1
        movhpd    xmm9, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r8]
        addpd     xmm9, xmm12
        cmpnlepd  xmm6, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16448]
        addpd     xmm8, xmm9
        movmskpd  r9d, xmm6
        addpd     xmm0, xmm8
        mov       QWORD PTR [472+rsp], r13
        pxor      xmm0, xmm13
        test      r9d, r9d
        jne       _B8_12

_B8_2::

        test      edx, edx
        jne       _B8_4

_B8_3::

        movups    xmm6, XMMWORD PTR [448+rsp]
        movups    xmm7, XMMWORD PTR [432+rsp]
        movups    xmm8, XMMWORD PTR [416+rsp]
        movups    xmm9, XMMWORD PTR [400+rsp]
        movups    xmm10, XMMWORD PTR [384+rsp]
        movups    xmm11, XMMWORD PTR [368+rsp]
        movups    xmm12, XMMWORD PTR [352+rsp]
        movups    xmm13, XMMWORD PTR [336+rsp]
        movups    xmm14, XMMWORD PTR [320+rsp]
        movups    xmm15, XMMWORD PTR [304+rsp]
        mov       r13, QWORD PTR [464+rsp]
        add       rsp, 488
        ret

_B8_4::

        movups    XMMWORD PTR [r13], xmm14
        movups    XMMWORD PTR [64+r13], xmm0
        je        _B8_3

_B8_7::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B8_8::

        mov       ecx, ebx
        mov       eax, 1
        shl       eax, cl
        test      esi, eax
        jne       _B8_11

_B8_9::

        inc       ebx
        cmp       ebx, 2
        jl        _B8_8

_B8_10::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [64+r13]
        jmp       _B8_3

_B8_11::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dsin_ha_cout_rare_internal
        jmp       _B8_9

_B8_12::

        movdqu    xmm9, XMMWORD PTR [_2il0floatpacket_36]
        pand      xmm9, xmm1
        psrlq     xmm9, 52
        movd      ecx, xmm9
        movups    XMMWORD PTR [32+rsp], xmm7
        movups    xmm7, XMMWORD PTR [__svml_dsin_ha_data_internal_ha+16512]
        pextrw    r9d, xmm9, 4
        movups    XMMWORD PTR [128+rsp], xmm0
        movaps    xmm0, xmm7
        movups    XMMWORD PTR [112+rsp], xmm6
        andps     xmm0, xmm1
        movdqu    xmm6, XMMWORD PTR [_2il0floatpacket_37]
        lea       r8d, DWORD PTR [rcx+rcx*2]
        cmpeqpd   xmm0, xmm7
        pand      xmm6, xmm1
        lea       r10d, DWORD PTR [r9+r9*2]
        shl       r8d, 3
        paddq     xmm6, XMMWORD PTR [_2il0floatpacket_38]
        movmskpd  edx, xmm0
        movups    XMMWORD PTR [144+rsp], xmm14
        movq      xmm14, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r8]
        shl       r10d, 3
        movups    XMMWORD PTR [64+rsp], xmm4
        movdqa    xmm4, xmm6
        movhpd    xmm14, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+rax+r10]
        psrlq     xmm4, 32
        movdqu    xmm15, XMMWORD PTR [_2il0floatpacket_39]
        movaps    xmm7, xmm14
        movq      xmm12, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r8]
        psrlq     xmm7, 32
        pand      xmm6, xmm15
        movdqa    xmm0, xmm4
        movhpd    xmm12, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rax+r10]
        movdqa    xmm10, xmm6
        pmuludq   xmm0, xmm7
        pand      xmm14, xmm15
        pmuludq   xmm7, xmm6
        pmuludq   xmm14, xmm4
        movups    XMMWORD PTR [96+rsp], xmm13
        movaps    xmm13, xmm12
        pand      xmm12, xmm15
        psrlq     xmm7, 32
        pmuludq   xmm10, xmm12
        psrlq     xmm13, 32
        pmuludq   xmm12, xmm4
        paddq     xmm0, xmm7
        movdqa    xmm11, xmm6
        movdqa    xmm9, xmm15
        pmuludq   xmm11, xmm13
        pand      xmm9, xmm10
        pmuludq   xmm13, xmm4
        paddq     xmm9, xmm0
        movq      xmm8, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r8]
        psrlq     xmm14, 32
        movups    XMMWORD PTR [80+rsp], xmm3
        movdqa    xmm3, xmm15
        movhpd    xmm8, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rax+r10]
        psrlq     xmm10, 32
        movups    XMMWORD PTR [48+rsp], xmm5
        pand      xmm3, xmm8
        movdqa    xmm5, xmm6
        movdqa    xmm7, xmm15
        paddq     xmm14, xmm9
        paddq     xmm12, xmm10
        pmuludq   xmm5, xmm3
        pmuludq   xmm4, xmm3
        pand      xmm7, xmm11
        movaps    xmm0, xmm14
        paddq     xmm7, xmm12
        psrlq     xmm0, 32
        psrlq     xmm11, 32
        psrlq     xmm8, 32
        movdqa    xmm10, xmm15
        paddq     xmm0, xmm7
        paddq     xmm13, xmm11
        pmuludq   xmm6, xmm8
        pand      xmm10, xmm5
        movdqa    xmm12, xmm0
        paddq     xmm10, xmm13
        psrlq     xmm12, 32
        psrlq     xmm5, 32
        paddq     xmm12, xmm10
        paddq     xmm4, xmm5
        pand      xmm6, xmm15
        movdqa    xmm9, xmm12
        paddq     xmm6, xmm4
        psrlq     xmm9, 32
        pand      xmm12, xmm15
        paddq     xmm9, xmm6
        psllq     xmm9, 32
        psllq     xmm0, 32
        paddq     xmm9, xmm12
        movdqu    xmm5, XMMWORD PTR [_2il0floatpacket_40]
        movdqa    xmm4, xmm9
        movdqu    xmm11, XMMWORD PTR [_2il0floatpacket_41]
        pand      xmm5, xmm1
        movups    xmm13, XMMWORD PTR [_2il0floatpacket_42]
        psrlq     xmm4, 12
        pxor      xmm11, xmm5
        movaps    xmm8, xmm13
        por       xmm4, xmm11
        pand      xmm14, xmm15
        addpd     xmm8, xmm4
        paddq     xmm0, xmm14
        movaps    xmm14, xmm8
        subpd     xmm14, xmm13
        movdqu    xmm6, XMMWORD PTR [_2il0floatpacket_47]
        movdqu    xmm7, XMMWORD PTR [_2il0floatpacket_45]
        pand      xmm6, xmm9
        movdqu    xmm3, XMMWORD PTR [_2il0floatpacket_44]
        pand      xmm7, xmm0
        psllq     xmm6, 40
        psrlq     xmm0, 24
        pxor      xmm3, xmm5
        por       xmm6, xmm0
        pxor      xmm5, XMMWORD PTR [_2il0floatpacket_46]
        psllq     xmm7, 28
        subpd     xmm4, xmm14
        por       xmm6, xmm5
        movaps    xmm0, xmm4
        subpd     xmm6, xmm5
        addpd     xmm0, xmm6
        subpd     xmm4, xmm0
        por       xmm7, xmm3
        subpd     xmm7, xmm3
        addpd     xmm6, xmm4
        movups    xmm9, XMMWORD PTR [_2il0floatpacket_51]
        andps     xmm9, xmm0
        addpd     xmm7, xmm6
        subpd     xmm0, xmm9
        mulpd     xmm7, XMMWORD PTR [_2il0floatpacket_48]
        movups    xmm15, XMMWORD PTR [_2il0floatpacket_49]
        movups    xmm3, XMMWORD PTR [_2il0floatpacket_50]
        movaps    xmm10, xmm15
        mulpd     xmm10, xmm9
        mulpd     xmm15, xmm0
        mulpd     xmm9, xmm3
        mulpd     xmm3, xmm0
        addpd     xmm15, xmm9
        addpd     xmm7, xmm3
        addpd     xmm15, xmm7
        movups    xmm4, XMMWORD PTR [_2il0floatpacket_53]
        movaps    xmm7, xmm15
        movups    xmm6, XMMWORD PTR [_2il0floatpacket_52]
        movaps    xmm5, xmm4
        andps     xmm6, xmm1
        addpd     xmm7, xmm10
        cmpltpd   xmm5, xmm6
        cmplepd   xmm6, xmm4
        subpd     xmm10, xmm7
        andps     xmm8, XMMWORD PTR [_2il0floatpacket_43]
        andps     xmm6, xmm1
        movd      r11d, xmm8
        andps     xmm7, xmm5
        orps      xmm6, xmm7
        movaps    xmm1, xmm6
        movaps    xmm12, xmm6
        pextrw    ecx, xmm8, 4
        addpd     xmm15, xmm10
        shl       r11d, 5
        andps     xmm5, xmm15
        shl       ecx, 5
        movq      xmm13, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+r11]
        movhpd    xmm13, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+rax+rcx]
        mulpd     xmm1, xmm13
        movq      xmm4, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+r11]
        movq      xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+r11]
        movhpd    xmm4, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+rax+rcx]
        movhpd    xmm0, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+rax+rcx]
        mulpd     xmm12, xmm4
        addpd     xmm4, xmm13
        movaps    xmm11, xmm0
        movaps    xmm8, xmm0
        addpd     xmm11, xmm1
        movaps    xmm3, xmm11
        addpd     xmm3, xmm12
        subpd     xmm8, xmm11
        subpd     xmm11, xmm3
        addpd     xmm1, xmm8
        addpd     xmm12, xmm11
        movaps    xmm11, xmm6
        mulpd     xmm11, xmm6
        addpd     xmm1, xmm12
        mulpd     xmm2, xmm11
        movups    xmm8, XMMWORD PTR [32+rsp]
        addpd     xmm8, xmm2
        mulpd     xmm8, xmm11
        mulpd     xmm8, xmm6
        mulpd     xmm6, xmm0
        mulpd     xmm8, xmm4
        subpd     xmm4, xmm6
        addpd     xmm1, xmm8
        mulpd     xmm5, xmm4
        movups    xmm2, XMMWORD PTR [48+rsp]
        mulpd     xmm2, xmm11
        movups    xmm8, XMMWORD PTR [64+rsp]
        addpd     xmm8, xmm2
        mulpd     xmm8, xmm11
        movups    xmm13, XMMWORD PTR [80+rsp]
        addpd     xmm13, xmm8
        mulpd     xmm11, xmm13
        mulpd     xmm11, xmm0
        movq      xmm6, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+r11]
        movhpd    xmm6, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rax+rcx]
        addpd     xmm1, xmm11
        addpd     xmm6, xmm5
        addpd     xmm1, xmm6
        addpd     xmm3, xmm1
        movups    xmm1, XMMWORD PTR [112+rsp]
        xorps     xmm3, XMMWORD PTR [96+rsp]
        movaps    xmm0, xmm1
        andnps    xmm0, XMMWORD PTR [128+rsp]
        andps     xmm3, xmm1
        movups    xmm14, XMMWORD PTR [144+rsp]
        orps      xmm0, xmm3
        jmp       _B8_2
        ALIGN     16

_B8_13::

__svml_sin2_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin2_ha_ex_B1_B4:
	DD	1603585
	DD	3855480
	DD	1861744
	DD	1800296
	DD	1738848
	DD	1677399
	DD	1615950
	DD	1554501
	DD	1493052
	DD	1431603
	DD	1370150
	DD	1308699
	DD	3997963

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_1
	DD	imagerel _B8_7
	DD	imagerel _unwind___svml_sin2_ha_ex_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin2_ha_ex_B7_B11:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B8_1
	DD	imagerel _B8_7
	DD	imagerel _unwind___svml_sin2_ha_ex_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_7
	DD	imagerel _B8_12
	DD	imagerel _unwind___svml_sin2_ha_ex_B7_B11

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin2_ha_ex_B12_B12:
	DD	33
	DD	imagerel _B8_1
	DD	imagerel _B8_7
	DD	imagerel _unwind___svml_sin2_ha_ex_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_12
	DD	imagerel _B8_13
	DD	imagerel _unwind___svml_sin2_ha_ex_B12_B12

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST8:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_sin8_ha_z0

__svml_sin8_ha_z0	PROC	EXPORT

_B9_1::

        DB        243
        DB        15
        DB        30
        DB        250
L359::

        sub       rsp, 1336
        xor       eax, eax
        vmovups   ZMMWORD PTR [1264+rsp], zmm21
        vmovups   ZMMWORD PTR [1200+rsp], zmm8
        mov       QWORD PTR [1192+rsp], r13
        lea       r13, QWORD PTR [1055+rsp]
        vmovups   zmm28, ZMMWORD PTR [__svml_dsin_ha_data_internal+128]
        and       r13, -64
        vmovups   zmm30, ZMMWORD PTR [__svml_dsin_ha_data_internal+192]
        vmovups   zmm31, ZMMWORD PTR [__svml_dsin_ha_data_internal+256]
        vmovups   zmm2, ZMMWORD PTR [__svml_dsin_ha_data_internal+320]
        vmovups   zmm1, ZMMWORD PTR [__svml_dsin_ha_data_internal+384]
        vmovups   zmm29, ZMMWORD PTR [__svml_dsin_ha_data_internal+64]
        vmovups   zmm24, ZMMWORD PTR [__svml_dsin_ha_data_internal+640]
        vmovups   zmm22, ZMMWORD PTR [__svml_dsin_ha_data_internal+512]
        vmovups   zmm3, ZMMWORD PTR [__svml_dsin_ha_data_internal+1088]
        vmovups   zmm25, ZMMWORD PTR [__svml_dsin_ha_data_internal+768]
        vmovaps   zmm27, zmm0
        vfmadd213pd zmm28, zmm27, zmm30 {rn-sae}
        vmovups   zmm0, ZMMWORD PTR [__svml_dsin_ha_data_internal+1024]
        vpcmpeqq  k2, zmm27, ZMMWORD PTR [__svml_dsin_ha_data_internal+896]
        vsubpd    zmm8, zmm28, zmm30 {rn-sae}
        vandpd    zmm26, zmm27, ZMMWORD PTR [__svml_dsin_ha_data_internal]
        vpermt2pd zmm24, zmm28, ZMMWORD PTR [__svml_dsin_ha_data_internal+704]
        vpermt2pd zmm22, zmm28, ZMMWORD PTR [__svml_dsin_ha_data_internal+576]
        vpermt2pd zmm25, zmm28, ZMMWORD PTR [__svml_dsin_ha_data_internal+832]
        vxorpd    zmm28{k2}, zmm28, ZMMWORD PTR [__svml_dsin_ha_data_internal+960]
        vfnmadd213pd zmm31, zmm8, zmm27 {rn-sae}
        vmovups   zmm30, ZMMWORD PTR [__svml_dsin_ha_data_internal+1152]
        vcmppd    k1, zmm26, zmm29, 22 {sae}
        vpsrlq    zmm28, zmm28, 4
        kortestw  k1, k1
        vmovaps   zmm4, zmm8
        vfnmadd213pd zmm4, zmm2, zmm31 {rn-sae}
        vmovaps   zmm23, zmm8
        vfnmadd213pd zmm23, zmm1, zmm4 {rn-sae}
        vsubpd    zmm5, zmm31, zmm4 {rn-sae}
        vmovups   zmm31, ZMMWORD PTR [__svml_dsin_ha_data_internal+1280]
        vmulpd    zmm21, zmm23, zmm23 {rn-sae}
        vfnmadd231pd zmm5, zmm2, zmm8 {rn-sae}
        vmovups   zmm2, ZMMWORD PTR [__svml_dsin_ha_data_internal+1216]
        vsubpd    zmm29, zmm23, zmm4 {rn-sae}
        vfmadd231pd zmm3, zmm0, zmm21 {rn-sae}
        vfmadd231pd zmm2, zmm30, zmm21 {rn-sae}
        vfmadd213pd zmm8, zmm1, zmm29 {rn-sae}
        vfmadd213pd zmm3, zmm21, zmm31 {rn-sae}
        vsubpd    zmm0, zmm5, zmm8 {rn-sae}
        vmovups   zmm8, ZMMWORD PTR [__svml_dsin_ha_data_internal+1408]
        vmovups   zmm5, ZMMWORD PTR [__svml_dsin_ha_data_internal+1344]
        vfmadd213pd zmm3, zmm21, zmm8 {rn-sae}
        vfmadd213pd zmm2, zmm21, zmm5 {rn-sae}
        vmovups   zmm8, ZMMWORD PTR [__svml_dsin_ha_data_internal+1472]
        vpsllq    zmm5, zmm25, 32
        vmulpd    zmm3, zmm22, zmm3 {rn-sae}
        vfmadd213pd zmm2, zmm21, zmm8 {rn-sae}
        vmovaps   zmm4, zmm24
        vfmadd213pd zmm4, zmm23, zmm22 {rn-sae}
        vsubpd    zmm1, zmm4, zmm22 {rn-sae}
        vfmsub231pd zmm1, zmm23, zmm24 {rn-sae}
        vmovaps   zmm29, zmm22
        vfnmadd213pd zmm29, zmm23, zmm24 {rn-sae}
        vfmadd213pd zmm25, zmm23, zmm1 {rn-sae}
        vmulpd    zmm23, zmm24, zmm23 {rn-sae}
        vfmadd213pd zmm29, zmm0, zmm5 {rn-sae}
        vpsllq    zmm24, zmm28, 63
        vfmadd213pd zmm2, zmm23, zmm3 {rn-sae}
        vaddpd    zmm22, zmm25, zmm29 {rn-sae}
        vfmadd213pd zmm2, zmm21, zmm22 {rn-sae}
        vaddpd    zmm21, zmm4, zmm2 {rn-sae}
        mov       QWORD PTR [1328+rsp], r13
        vxorpd    zmm0, zmm21, zmm24
        jne       _B9_17

_B9_2::

        test      eax, eax
        jne       _B9_4

_B9_3::

        vmovups   zmm8, ZMMWORD PTR [1200+rsp]
        vmovups   zmm21, ZMMWORD PTR [1264+rsp]
        mov       r13, QWORD PTR [1192+rsp]
        add       rsp, 1336
        ret

_B9_4::

        vstmxcsr  DWORD PTR [1184+rsp]

_B9_5::

        movzx     ecx, WORD PTR [1184+rsp]
        mov       edx, ecx
        or        edx, 8064
        cmp       ecx, edx
        je        _B9_7

_B9_6::

        mov       DWORD PTR [1184+rsp], edx
        vldmxcsr  DWORD PTR [1184+rsp]

_B9_7::

        vmovups   ZMMWORD PTR [r13], zmm27
        vmovups   ZMMWORD PTR [64+r13], zmm0
        test      eax, eax
        jne       _B9_12

_B9_8::

        cmp       ecx, edx
        je        _B9_3

_B9_9::

        vstmxcsr  DWORD PTR [1184+rsp]
        mov       eax, DWORD PTR [1184+rsp]

_B9_10::

        and       eax, -8065
        or        eax, ecx
        mov       DWORD PTR [1184+rsp], eax
        vldmxcsr  DWORD PTR [1184+rsp]
        jmp       _B9_3

_B9_12::

        xor       r8d, r8d
        kmovw     WORD PTR [952+rsp], k4
        kmovw     WORD PTR [944+rsp], k5
        kmovw     WORD PTR [936+rsp], k6
        kmovw     WORD PTR [928+rsp], k7
        vmovups   ZMMWORD PTR [864+rsp], zmm6
        vmovups   ZMMWORD PTR [800+rsp], zmm7
        vmovups   ZMMWORD PTR [736+rsp], zmm9
        vmovups   ZMMWORD PTR [672+rsp], zmm10
        vmovups   ZMMWORD PTR [608+rsp], zmm11
        vmovups   ZMMWORD PTR [544+rsp], zmm12
        vmovups   ZMMWORD PTR [480+rsp], zmm13
        vmovups   ZMMWORD PTR [416+rsp], zmm14
        vmovups   ZMMWORD PTR [352+rsp], zmm15
        vmovups   ZMMWORD PTR [288+rsp], zmm16
        vmovups   ZMMWORD PTR [224+rsp], zmm17
        vmovups   ZMMWORD PTR [160+rsp], zmm18
        vmovups   ZMMWORD PTR [96+rsp], zmm19
        vmovups   ZMMWORD PTR [32+rsp], zmm20
        mov       QWORD PTR [976+rsp], rbx
        mov       ebx, r8d
        mov       QWORD PTR [968+rsp], rsi
        mov       esi, ecx
        mov       QWORD PTR [960+rsp], rdi
        mov       edi, eax
        mov       QWORD PTR [984+rsp], rbp
        mov       ebp, edx

_B9_13::

        bt        edi, ebx
        jc        _B9_16

_B9_14::

        inc       ebx
        cmp       ebx, 8
        jl        _B9_13

_B9_15::

        kmovw     k4, WORD PTR [952+rsp]
        mov       edx, ebp
        kmovw     k5, WORD PTR [944+rsp]
        kmovw     k6, WORD PTR [936+rsp]
        kmovw     k7, WORD PTR [928+rsp]
        vmovups   zmm6, ZMMWORD PTR [864+rsp]
        vmovups   zmm7, ZMMWORD PTR [800+rsp]
        vmovups   zmm9, ZMMWORD PTR [736+rsp]
        vmovups   zmm10, ZMMWORD PTR [672+rsp]
        vmovups   zmm11, ZMMWORD PTR [608+rsp]
        vmovups   zmm12, ZMMWORD PTR [544+rsp]
        vmovups   zmm13, ZMMWORD PTR [480+rsp]
        vmovups   zmm14, ZMMWORD PTR [416+rsp]
        vmovups   zmm15, ZMMWORD PTR [352+rsp]
        vmovups   zmm16, ZMMWORD PTR [288+rsp]
        vmovups   zmm17, ZMMWORD PTR [224+rsp]
        vmovups   zmm18, ZMMWORD PTR [160+rsp]
        vmovups   zmm19, ZMMWORD PTR [96+rsp]
        vmovups   zmm20, ZMMWORD PTR [32+rsp]
        vmovups   zmm0, ZMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [976+rsp]
        mov       ecx, esi
        mov       rsi, QWORD PTR [968+rsp]
        mov       rdi, QWORD PTR [960+rsp]
        mov       rbp, QWORD PTR [984+rsp]
        jmp       _B9_8

_B9_16::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dsin_ha_cout_rare_internal
        jmp       _B9_14

_B9_17::

        vpandq    zmm23, zmm26, QWORD BCST [_2il0floatpacket_75]
        lea       rdx, QWORD PTR [__ImageBase]
        vpsrlq    zmm4, zmm23, 52
        mov       rcx, rdx
        vmovups   zmm5, ZMMWORD PTR [__svml_dsin_ha_data_internal_ha+16512]
        vpcmpeqd  k3, zmm0, zmm0
        vpsllq    zmm21, zmm4, 1
        vandpd    zmm8, zmm5, zmm26
        vpbroadcastq zmm25, QWORD PTR [_2il0floatpacket_74]
        vpaddq    zmm30, zmm21, zmm4
        vpbroadcastq zmm21, QWORD PTR [_2il0floatpacket_78]
        vcmppd    k2, zmm8, zmm5, 4 {sae}
        vpsllq    zmm2, zmm30, 3
        vpmovqd   ymm28, zmm2
        mov       r8, rdx
        vmovaps   zmm22, zmm25
        mov       r9, rdx
        mov       r10, rdx
        mov       r11, rdx
        vpxord    zmm5, zmm5, zmm5
        vgatherdpd zmm5{k3}, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+rdx+ymm28]
        vpcmpeqd  k3, zmm0, zmm0
        vpandnq   zmm22{k2}, zmm8, zmm8
        vpcmpeqd  k2, zmm0, zmm0
        vcmppd    k0, zmm22, zmm22, 3 {sae}
        kmovw     eax, k0
        vpxord    zmm8, zmm8, zmm8
        vpxord    zmm22, zmm22, zmm22
        vpandq    zmm24, zmm26, QWORD BCST [_2il0floatpacket_76]
        vpaddq    zmm23, zmm24, QWORD BCST [_2il0floatpacket_77]
        vpsrlq    zmm24, zmm5, 32
        vgatherdpd zmm8{k2}, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+8+rcx+ymm28]
        vgatherdpd zmm22{k3}, QWORD PTR [imagerel(__svml_dsin_ha_reduction_data_internal)+16+r8+ymm28]
        vpsrlq    zmm31, zmm8, 32
        vpsrlq    zmm29, zmm22, 32
        vpandq    zmm28, zmm8, zmm21
        vpsrlq    zmm8, zmm23, 32
        vpmullq   zmm4, zmm8, zmm31
        vpmullq   zmm3, zmm8, zmm28
        vpmullq   zmm2, zmm8, zmm29
        vpandq    zmm1, zmm5, zmm21
        vpandq    zmm22, zmm22, zmm21
        vpandq    zmm30, zmm23, zmm21
        vpmullq   zmm5, zmm8, zmm1
        vpmullq   zmm23, zmm8, zmm22
        vpmullq   zmm8, zmm30, zmm1
        vpmullq   zmm1, zmm30, zmm28
        vpmullq   zmm22, zmm30, zmm24
        vpmullq   zmm24, zmm30, zmm31
        vpmullq   zmm31, zmm30, zmm29
        vpsrlq    zmm29, zmm1, 32
        vpsrlq    zmm28, zmm8, 32
        vpsrlq    zmm30, zmm31, 32
        vpsrlq    zmm31, zmm24, 32
        vpaddq    zmm3, zmm3, zmm29
        vpaddq    zmm2, zmm2, zmm30
        vpaddq    zmm31, zmm4, zmm31
        vpaddq    zmm30, zmm5, zmm28
        vpandq    zmm4, zmm24, zmm21
        vpaddq    zmm5, zmm4, zmm3
        vpsrlq    zmm3, zmm23, 32
        vpandq    zmm1, zmm1, zmm21
        vpaddq    zmm2, zmm1, zmm2
        vpaddq    zmm23, zmm3, zmm2
        vpandq    zmm29, zmm8, zmm21
        vpandq    zmm24, zmm22, zmm21
        vpaddq    zmm8, zmm29, zmm31
        vpaddq    zmm31, zmm24, zmm30
        vpsrlq    zmm24, zmm23, 32
        vpaddq    zmm29, zmm24, zmm5
        vpsrlq    zmm1, zmm29, 32
        vpsllq    zmm5, zmm29, 32
        vpaddq    zmm4, zmm1, zmm8
        vpbroadcastq zmm1, QWORD PTR [_2il0floatpacket_81]
        vpsrlq    zmm22, zmm4, 32
        vpaddq    zmm3, zmm22, zmm31
        vpandq    zmm2, zmm23, zmm21
        vpandq    zmm24, zmm4, zmm21
        vpsllq    zmm21, zmm3, 32
        vpaddq    zmm3, zmm5, zmm2
        vandpd    zmm2, zmm26, QWORD BCST [_2il0floatpacket_89]
        vpaddq    zmm29, zmm21, zmm24
        vpsrlq    zmm8, zmm29, 12
        vpandq    zmm5, zmm26, QWORD BCST [_2il0floatpacket_79]
        vpternlogq zmm8, zmm5, QWORD BCST [_2il0floatpacket_80], 246
        vaddpd    zmm23, zmm1, zmm8 {rn-sae}
        vsubpd    zmm22, zmm23, zmm1 {rn-sae}
        vsubpd    zmm4, zmm8, zmm22 {rn-sae}
        vpbroadcastq zmm22, QWORD PTR [_2il0floatpacket_87]
        vpandq    zmm21, zmm3, QWORD BCST [_2il0floatpacket_84]
        vpsllq    zmm28, zmm21, 28
        vpsrlq    zmm3, zmm3, 24
        vpbroadcastq zmm21, QWORD PTR [_2il0floatpacket_90]
        vcmppd    k2, zmm2, zmm21, 26 {sae}
        vcmppd    k3, zmm2, zmm21, 22 {sae}
        vpxorq    zmm24, zmm5, QWORD BCST [_2il0floatpacket_83]
        vporq     zmm30, zmm28, zmm24
        vpandq    zmm29, zmm29, QWORD BCST [_2il0floatpacket_86]
        vsubpd    zmm8, zmm30, zmm24 {rn-sae}
        vpsllq    zmm24, zmm29, 40
        vmovups   zmm30, ZMMWORD PTR [__svml_dsin_ha_data_internal_ha+17024]
        vmovaps   zmm28, zmm25
        vpxorq    zmm1, zmm5, QWORD BCST [_2il0floatpacket_85]
        vpternlogq zmm3, zmm24, zmm1, 254
        vpbroadcastq zmm5, QWORD PTR [_2il0floatpacket_88]
        vsubpd    zmm24, zmm3, zmm1 {rn-sae}
        vmovaps   zmm1, zmm22
        vpandq    zmm31, zmm23, QWORD BCST [_2il0floatpacket_82]
        vaddpd    zmm23, zmm4, zmm24 {rn-sae}
        vmulpd    zmm3, zmm23, zmm22 {rn-sae}
        vsubpd    zmm4, zmm4, zmm23 {rn-sae}
        vfmsub213pd zmm1, zmm23, zmm3 {rn-sae}
        vaddpd    zmm24, zmm24, zmm4 {rn-sae}
        vmovups   zmm4, ZMMWORD PTR [__svml_dsin_ha_data_internal_ha+17152]
        vfmadd213pd zmm23, zmm5, zmm1 {rn-sae}
        vaddpd    zmm29, zmm24, zmm8 {rn-sae}
        vpsllq    zmm24, zmm31, 5
        vfmadd213pd zmm29, zmm22, zmm23 {rn-sae}
        vpmovqd   ymm8, zmm24
        vpandnq   zmm28{k2}, zmm2, zmm2
        vpcmpeqd  k2, zmm0, zmm0
        vandpd    zmm5, zmm28, zmm3
        vandpd    zmm1, zmm28, zmm29
        vmovups   zmm3, ZMMWORD PTR [__svml_dsin_ha_data_internal_ha+16960]
        vpandnq   zmm25{k3}, zmm2, zmm2
        vpternlogq zmm5, zmm25, zmm26, 248
        vpcmpeqd  k3, zmm0, zmm0
        vmovaps   zmm31, zmm5
        vmovaps   zmm24, zmm5
        vpxord    zmm25, zmm25, zmm25
        vgatherdpd zmm25{k2}, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+r9+ymm8]
        vpcmpeqd  k2, zmm0, zmm0
        vpxord    zmm21, zmm21, zmm21
        vpxord    zmm22, zmm22, zmm22
        vgatherdpd zmm21{k3}, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+8+r10+ymm8]
        vgatherdpd zmm22{k2}, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+16+r11+ymm8]
        vpcmpeqd  k3, zmm0, zmm0
        vfmadd213pd zmm31, zmm21, zmm22 {rn-sae}
        vfmadd132pd zmm24, zmm31, zmm25 {rn-sae}
        vsubpd    zmm2, zmm22, zmm31 {rn-sae}
        vsubpd    zmm23, zmm31, zmm24 {rn-sae}
        vfmadd231pd zmm2, zmm21, zmm5 {rn-sae}
        vfmadd231pd zmm23, zmm5, zmm25 {rn-sae}
        vaddpd    zmm2, zmm23, zmm2 {rn-sae}
        vmulpd    zmm23, zmm5, zmm5 {rn-sae}
        vfmadd231pd zmm3, zmm30, zmm23 {rn-sae}
        vmulpd    zmm30, zmm3, zmm23 {rn-sae}
        vaddpd    zmm3, zmm25, zmm21 {rn-sae}
        vmovups   zmm21, ZMMWORD PTR [__svml_dsin_ha_data_internal_ha+17216]
        vmovups   zmm25, ZMMWORD PTR [__svml_dsin_ha_data_internal_ha+17088]
        vmulpd    zmm28, zmm30, zmm5 {rn-sae}
        vfmadd231pd zmm4, zmm21, zmm23 {rn-sae}
        vfnmadd213pd zmm5, zmm22, zmm3 {rn-sae}
        vfmadd213pd zmm28, zmm3, zmm2 {rn-sae}
        vfmadd213pd zmm4, zmm23, zmm25 {rn-sae}
        vmulpd    zmm2, zmm4, zmm23 {rn-sae}
        vfmadd213pd zmm2, zmm22, zmm28 {rn-sae}
        vpxord    zmm22, zmm22, zmm22
        vgatherdpd zmm22{k3}, QWORD PTR [imagerel(__svml_dsin_ha_data_internal_ha)+24+rdx+ymm8]
        vfmadd213pd zmm1, zmm5, zmm22 {rn-sae}
        vaddpd    zmm1, zmm2, zmm1 {rn-sae}
        vaddpd    zmm5, zmm24, zmm1 {rn-sae}
        vpternlogq zmm5, zmm26, zmm27, 150
        vblendmpd zmm0{k1}, zmm0, zmm5
        jmp       _B9_2
        ALIGN     16

_B9_18::

__svml_sin8_ha_z0 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin8_ha_z0_B1_B10:
	DD	535297
	DD	9819179
	DD	4950051
	DD	5199896
	DD	10944779

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_1
	DD	imagerel _B9_12
	DD	imagerel _unwind___svml_sin8_ha_z0_B1_B10

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin8_ha_z0_B12_B16:
	DD	2943009
	DD	8082664
	DD	7894238
	DD	7955668
	DD	8008905
	DD	149697
	DD	407734
	DD	665771
	DD	923808
	DD	1181845
	DD	1505418
	DD	1763455
	DD	2021492
	DD	2279529
	DD	2537566
	DD	2795603
	DD	3053640
	DD	3307581
	DD	3565618
	DD	7633703
	DD	7695134
	DD	7756565
	DD	7817996
	DD	imagerel _B9_1
	DD	imagerel _B9_12
	DD	imagerel _unwind___svml_sin8_ha_z0_B1_B10

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_12
	DD	imagerel _B9_17
	DD	imagerel _unwind___svml_sin8_ha_z0_B12_B16

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_sin8_ha_z0_B17_B17:
	DD	33
	DD	imagerel _B9_1
	DD	imagerel _B9_12
	DD	imagerel _unwind___svml_sin8_ha_z0_B1_B10

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_17
	DD	imagerel _B9_18
	DD	imagerel _unwind___svml_sin8_ha_z0_B17_B17

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST9:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_dsin_ha_cout_rare_internal

__svml_dsin_ha_cout_rare_internal	PROC

_B10_1::

        DB        243
        DB        15
        DB        30
        DB        250
L410::

        sub       rsp, 40
        mov       r8, rdx
        movzx     eax, WORD PTR [6+rcx]
        and       eax, 32752
        movsd     xmm1, QWORD PTR [rcx]
        mov       dl, BYTE PTR [7+rcx]
        and       dl, 127
        movsd     QWORD PTR [32+rsp], xmm1
        cmp       eax, 32752
        jne       _B10_6

_B10_2::

        cmp       DWORD PTR [32+rsp], 0
        jne       _B10_5

_B10_3::

        mov       BYTE PTR [39+rsp], dl
        cmp       DWORD PTR [36+rsp], 2146435072
        jne       _B10_5

_B10_4::

        movsd     xmm0, QWORD PTR [_vmldSinHATab]
        mov       eax, 1
        mulsd     xmm1, xmm0
        movsd     QWORD PTR [r8], xmm1
        add       rsp, 40
        ret

_B10_5::

        mulsd     xmm1, xmm1
        xor       eax, eax
        movsd     QWORD PTR [r8], xmm1
        add       rsp, 40
        ret

_B10_6::

        xor       eax, eax
        add       rsp, 40
        ret
        ALIGN     16

_B10_7::

__svml_dsin_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_dsin_ha_cout_rare_internal_B1_B6:
	DD	67585
	DD	16904

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B10_1
	DD	imagerel _B10_7
	DD	imagerel _unwind___svml_dsin_ha_cout_rare_internal_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_dsin_ha_reduction_data_internal
__svml_dsin_ha_reduction_data_internal	DD	0
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
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	5
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	10
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	20
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	40
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	81
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	162
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	325
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	651
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1303
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2607
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	5215
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	10430
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	20860
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	41721
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	83443
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	166886
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	333772
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	667544
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1335088
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2670176
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	5340353
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	10680707
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	21361414
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	42722829
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	85445659
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	170891318
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	341782637
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	683565275
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1367130551
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2734261102
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1173554908
	DD	1
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2347109817
	DD	2
	DD	0
	DD	0
	DD	0
	DD	0
	DD	399252338
	DD	5
	DD	0
	DD	0
	DD	0
	DD	0
	DD	798504676
	DD	10
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1597009353
	DD	20
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3194018707
	DD	40
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2093070119
	DD	81
	DD	0
	DD	0
	DD	0
	DD	0
	DD	4186140238
	DD	162
	DD	0
	DD	0
	DD	0
	DD	0
	DD	4077313180
	DD	325
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3859659065
	DD	651
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3424350834
	DD	1303
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2553734372
	DD	2607
	DD	0
	DD	0
	DD	0
	DD	0
	DD	812501448
	DD	5215
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1625002897
	DD	10430
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3250005794
	DD	20860
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2205044292
	DD	41721
	DD	0
	DD	0
	DD	0
	DD	0
	DD	115121288
	DD	83443
	DD	0
	DD	0
	DD	0
	DD	0
	DD	230242576
	DD	166886
	DD	0
	DD	0
	DD	0
	DD	0
	DD	460485152
	DD	333772
	DD	0
	DD	0
	DD	0
	DD	0
	DD	920970305
	DD	667544
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1841940610
	DD	1335088
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3683881221
	DD	2670176
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3072795146
	DD	5340353
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1850622997
	DD	10680707
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3701245994
	DD	21361414
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3107524692
	DD	42722829
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1920082089
	DD	85445659
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3840164178
	DD	170891318
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3385361061
	DD	341782637
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2475754826
	DD	683565275
	DD	0
	DD	0
	DD	0
	DD	0
	DD	656542356
	DD	1367130551
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1313084713
	DD	2734261102
	DD	0
	DD	0
	DD	1
	DD	0
	DD	2626169427
	DD	1173554908
	DD	0
	DD	0
	DD	2
	DD	0
	DD	957371559
	DD	2347109817
	DD	0
	DD	0
	DD	5
	DD	0
	DD	1914743119
	DD	399252338
	DD	0
	DD	0
	DD	10
	DD	0
	DD	3829486239
	DD	798504676
	DD	0
	DD	0
	DD	20
	DD	0
	DD	3364005183
	DD	1597009353
	DD	0
	DD	0
	DD	40
	DD	0
	DD	2433043071
	DD	3194018707
	DD	0
	DD	0
	DD	81
	DD	0
	DD	571118846
	DD	2093070119
	DD	0
	DD	0
	DD	162
	DD	0
	DD	1142237692
	DD	4186140238
	DD	0
	DD	0
	DD	325
	DD	0
	DD	2284475384
	DD	4077313180
	DD	0
	DD	0
	DD	651
	DD	0
	DD	273983472
	DD	3859659065
	DD	0
	DD	0
	DD	1303
	DD	0
	DD	547966945
	DD	3424350834
	DD	0
	DD	0
	DD	2607
	DD	0
	DD	1095933890
	DD	2553734372
	DD	0
	DD	0
	DD	5215
	DD	0
	DD	2191867780
	DD	812501448
	DD	0
	DD	0
	DD	10430
	DD	0
	DD	88768265
	DD	1625002897
	DD	0
	DD	0
	DD	20860
	DD	0
	DD	177536531
	DD	3250005794
	DD	0
	DD	0
	DD	41721
	DD	0
	DD	355073063
	DD	2205044292
	DD	0
	DD	0
	DD	83443
	DD	0
	DD	710146126
	DD	115121288
	DD	0
	DD	0
	DD	166886
	DD	0
	DD	1420292253
	DD	230242576
	DD	0
	DD	0
	DD	333772
	DD	0
	DD	2840584506
	DD	460485152
	DD	0
	DD	0
	DD	667544
	DD	0
	DD	1386201717
	DD	920970305
	DD	0
	DD	0
	DD	1335088
	DD	0
	DD	2772403434
	DD	1841940610
	DD	0
	DD	0
	DD	2670176
	DD	0
	DD	1249839573
	DD	3683881221
	DD	0
	DD	0
	DD	5340353
	DD	0
	DD	2499679147
	DD	3072795146
	DD	0
	DD	0
	DD	10680707
	DD	0
	DD	704390999
	DD	1850622997
	DD	0
	DD	0
	DD	21361414
	DD	0
	DD	1408781999
	DD	3701245994
	DD	0
	DD	0
	DD	42722829
	DD	0
	DD	2817563999
	DD	3107524692
	DD	0
	DD	0
	DD	85445659
	DD	0
	DD	1340160702
	DD	1920082089
	DD	0
	DD	0
	DD	170891318
	DD	0
	DD	2680321405
	DD	3840164178
	DD	0
	DD	0
	DD	341782637
	DD	0
	DD	1065675514
	DD	3385361061
	DD	0
	DD	0
	DD	683565275
	DD	0
	DD	2131351028
	DD	2475754826
	DD	0
	DD	0
	DD	1367130551
	DD	0
	DD	4262702056
	DD	656542356
	DD	0
	DD	0
	DD	2734261102
	DD	0
	DD	4230436817
	DD	1313084713
	DD	0
	DD	0
	DD	1173554908
	DD	1
	DD	4165906339
	DD	2626169427
	DD	0
	DD	0
	DD	2347109817
	DD	2
	DD	4036845383
	DD	957371559
	DD	0
	DD	0
	DD	399252338
	DD	5
	DD	3778723471
	DD	1914743119
	DD	0
	DD	0
	DD	798504676
	DD	10
	DD	3262479647
	DD	3829486239
	DD	0
	DD	0
	DD	1597009353
	DD	20
	DD	2229991998
	DD	3364005183
	DD	0
	DD	0
	DD	3194018707
	DD	40
	DD	165016701
	DD	2433043071
	DD	0
	DD	0
	DD	2093070119
	DD	81
	DD	330033402
	DD	571118846
	DD	0
	DD	0
	DD	4186140238
	DD	162
	DD	660066805
	DD	1142237692
	DD	0
	DD	0
	DD	4077313180
	DD	325
	DD	1320133610
	DD	2284475384
	DD	0
	DD	0
	DD	3859659065
	DD	651
	DD	2640267220
	DD	273983472
	DD	0
	DD	0
	DD	3424350834
	DD	1303
	DD	985567145
	DD	547966945
	DD	0
	DD	0
	DD	2553734372
	DD	2607
	DD	1971134291
	DD	1095933890
	DD	0
	DD	0
	DD	812501448
	DD	5215
	DD	3942268582
	DD	2191867780
	DD	0
	DD	0
	DD	1625002897
	DD	10430
	DD	3589569869
	DD	88768265
	DD	0
	DD	0
	DD	3250005794
	DD	20860
	DD	2884172442
	DD	177536531
	DD	0
	DD	0
	DD	2205044292
	DD	41721
	DD	1473377588
	DD	355073063
	DD	0
	DD	0
	DD	115121288
	DD	83443
	DD	2946755177
	DD	710146126
	DD	0
	DD	0
	DD	230242576
	DD	166886
	DD	1598543059
	DD	1420292253
	DD	0
	DD	0
	DD	460485152
	DD	333772
	DD	3197086118
	DD	2840584506
	DD	0
	DD	0
	DD	920970305
	DD	667544
	DD	2099204941
	DD	1386201717
	DD	0
	DD	0
	DD	1841940610
	DD	1335088
	DD	4198409883
	DD	2772403434
	DD	0
	DD	0
	DD	3683881221
	DD	2670176
	DD	4101852471
	DD	1249839573
	DD	0
	DD	0
	DD	3072795146
	DD	5340353
	DD	3908737646
	DD	2499679147
	DD	0
	DD	0
	DD	1850622997
	DD	10680707
	DD	3522507997
	DD	704390999
	DD	0
	DD	0
	DD	3701245994
	DD	21361414
	DD	2750048699
	DD	1408781999
	DD	0
	DD	0
	DD	3107524692
	DD	42722829
	DD	1205130103
	DD	2817563999
	DD	0
	DD	0
	DD	1920082089
	DD	85445659
	DD	2410260206
	DD	1340160702
	DD	0
	DD	0
	DD	3840164178
	DD	170891318
	DD	525553116
	DD	2680321405
	DD	0
	DD	0
	DD	3385361061
	DD	341782637
	DD	1051106232
	DD	1065675514
	DD	0
	DD	0
	DD	2475754826
	DD	683565275
	DD	2102212464
	DD	2131351028
	DD	0
	DD	0
	DD	656542356
	DD	1367130551
	DD	4204424928
	DD	4262702056
	DD	0
	DD	0
	DD	1313084713
	DD	2734261102
	DD	4113882560
	DD	4230436817
	DD	1
	DD	0
	DD	2626169427
	DD	1173554908
	DD	3932797825
	DD	4165906339
	DD	2
	DD	0
	DD	957371559
	DD	2347109817
	DD	3570628355
	DD	4036845383
	DD	5
	DD	0
	DD	1914743119
	DD	399252338
	DD	2846289414
	DD	3778723471
	DD	10
	DD	0
	DD	3829486239
	DD	798504676
	DD	1397611533
	DD	3262479647
	DD	20
	DD	0
	DD	3364005183
	DD	1597009353
	DD	2795223067
	DD	2229991998
	DD	40
	DD	0
	DD	2433043071
	DD	3194018707
	DD	1295478838
	DD	165016701
	DD	81
	DD	0
	DD	571118846
	DD	2093070119
	DD	2590957677
	DD	330033402
	DD	162
	DD	0
	DD	1142237692
	DD	4186140238
	DD	886948059
	DD	660066805
	DD	325
	DD	0
	DD	2284475384
	DD	4077313180
	DD	1773896118
	DD	1320133610
	DD	651
	DD	0
	DD	273983472
	DD	3859659065
	DD	3547792237
	DD	2640267220
	DD	1303
	DD	0
	DD	547966945
	DD	3424350834
	DD	2800617179
	DD	985567145
	DD	2607
	DD	0
	DD	1095933890
	DD	2553734372
	DD	1306267062
	DD	1971134291
	DD	5215
	DD	0
	DD	2191867780
	DD	812501448
	DD	2612534124
	DD	3942268582
	DD	10430
	DD	0
	DD	88768265
	DD	1625002897
	DD	930100952
	DD	3589569869
	DD	20860
	DD	0
	DD	177536531
	DD	3250005794
	DD	1860201905
	DD	2884172442
	DD	41721
	DD	0
	DD	355073063
	DD	2205044292
	DD	3720403810
	DD	1473377588
	DD	83443
	DD	0
	DD	710146126
	DD	115121288
	DD	3145840325
	DD	2946755177
	DD	166886
	DD	0
	DD	1420292253
	DD	230242576
	DD	1996713354
	DD	1598543059
	DD	333772
	DD	0
	DD	2840584506
	DD	460485152
	DD	3993426708
	DD	3197086118
	DD	667544
	DD	0
	DD	1386201717
	DD	920970305
	DD	3691886121
	DD	2099204941
	DD	1335088
	DD	0
	DD	2772403434
	DD	1841940610
	DD	3088804946
	DD	4198409883
	DD	2670176
	DD	0
	DD	1249839573
	DD	3683881221
	DD	1882642597
	DD	4101852471
	DD	5340353
	DD	0
	DD	2499679147
	DD	3072795146
	DD	3765285194
	DD	3908737646
	DD	10680707
	DD	0
	DD	704390999
	DD	1850622997
	DD	3235603093
	DD	3522507997
	DD	21361414
	DD	0
	DD	1408781999
	DD	3701245994
	DD	2176238891
	DD	2750048699
	DD	42722829
	DD	0
	DD	2817563999
	DD	3107524692
	DD	57510486
	DD	1205130103
	DD	85445659
	DD	0
	DD	1340160702
	DD	1920082089
	DD	115020972
	DD	2410260206
	DD	170891318
	DD	0
	DD	2680321405
	DD	3840164178
	DD	230041945
	DD	525553116
	DD	341782637
	DD	0
	DD	1065675514
	DD	3385361061
	DD	460083891
	DD	1051106232
	DD	683565275
	DD	0
	DD	2131351028
	DD	2475754826
	DD	920167782
	DD	2102212464
	DD	1367130551
	DD	0
	DD	4262702056
	DD	656542356
	DD	1840335564
	DD	4204424928
	DD	2734261102
	DD	0
	DD	4230436817
	DD	1313084713
	DD	3680671129
	DD	4113882560
	DD	1173554908
	DD	1
	DD	4165906339
	DD	2626169427
	DD	3066374962
	DD	3932797825
	DD	2347109817
	DD	2
	DD	4036845383
	DD	957371559
	DD	1837782628
	DD	3570628355
	DD	399252338
	DD	5
	DD	3778723471
	DD	1914743119
	DD	3675565257
	DD	2846289414
	DD	798504676
	DD	10
	DD	3262479647
	DD	3829486239
	DD	3056163219
	DD	1397611533
	DD	1597009353
	DD	20
	DD	2229991998
	DD	3364005183
	DD	1817359143
	DD	2795223067
	DD	3194018707
	DD	40
	DD	165016701
	DD	2433043071
	DD	3634718287
	DD	1295478838
	DD	2093070119
	DD	81
	DD	330033402
	DD	571118846
	DD	2974469278
	DD	2590957677
	DD	4186140238
	DD	162
	DD	660066805
	DD	1142237692
	DD	1653971260
	DD	886948059
	DD	4077313180
	DD	325
	DD	1320133610
	DD	2284475384
	DD	3307942520
	DD	1773896118
	DD	3859659065
	DD	651
	DD	2640267220
	DD	273983472
	DD	2320917745
	DD	3547792237
	DD	3424350834
	DD	1303
	DD	985567145
	DD	547966945
	DD	346868194
	DD	2800617179
	DD	2553734372
	DD	2607
	DD	1971134291
	DD	1095933890
	DD	693736388
	DD	1306267062
	DD	812501448
	DD	5215
	DD	3942268582
	DD	2191867780
	DD	1387472776
	DD	2612534124
	DD	1625002897
	DD	10430
	DD	3589569869
	DD	88768265
	DD	2774945552
	DD	930100952
	DD	3250005794
	DD	20860
	DD	2884172442
	DD	177536531
	DD	1254923809
	DD	1860201905
	DD	2205044292
	DD	41721
	DD	1473377588
	DD	355073063
	DD	2509847619
	DD	3720403810
	DD	115121288
	DD	83443
	DD	2946755177
	DD	710146126
	DD	724727943
	DD	3145840325
	DD	230242576
	DD	166886
	DD	1598543059
	DD	1420292253
	DD	1449455886
	DD	1996713354
	DD	460485152
	DD	333772
	DD	3197086118
	DD	2840584506
	DD	2898911772
	DD	3993426708
	DD	920970305
	DD	667544
	DD	2099204941
	DD	1386201717
	DD	1502856249
	DD	3691886121
	DD	1841940610
	DD	1335088
	DD	4198409883
	DD	2772403434
	DD	3005712498
	DD	3088804946
	DD	3683881221
	DD	2670176
	DD	4101852471
	DD	1249839573
	DD	1716457700
	DD	1882642597
	DD	3072795146
	DD	5340353
	DD	3908737646
	DD	2499679147
	DD	3432915400
	DD	3765285194
	DD	1850622997
	DD	10680707
	DD	3522507997
	DD	704390999
	DD	2570863504
	DD	3235603093
	DD	3701245994
	DD	21361414
	DD	2750048699
	DD	1408781999
	DD	846759712
	DD	2176238891
	DD	3107524692
	DD	42722829
	DD	1205130103
	DD	2817563999
	DD	1693519425
	DD	57510486
	DD	1920082089
	DD	85445659
	DD	2410260206
	DD	1340160702
	DD	3387038850
	DD	115020972
	DD	3840164178
	DD	170891318
	DD	525553116
	DD	2680321405
	DD	2479110404
	DD	230041945
	DD	3385361061
	DD	341782637
	DD	1051106232
	DD	1065675514
	DD	663253512
	DD	460083891
	DD	2475754826
	DD	683565275
	DD	2102212464
	DD	2131351028
	DD	1326507024
	DD	920167782
	DD	656542356
	DD	1367130551
	DD	4204424928
	DD	4262702056
	DD	2653014048
	DD	1840335564
	DD	1313084713
	DD	2734261102
	DD	4113882560
	DD	4230436817
	DD	1011060801
	DD	3680671129
	DD	2626169427
	DD	1173554908
	DD	3932797825
	DD	4165906339
	DD	2022121603
	DD	3066374962
	DD	957371559
	DD	2347109817
	DD	3570628355
	DD	4036845383
	DD	4044243207
	DD	1837782628
	DD	1914743119
	DD	399252338
	DD	2846289414
	DD	3778723471
	DD	3793519119
	DD	3675565257
	DD	3829486239
	DD	798504676
	DD	1397611533
	DD	3262479647
	DD	3292070943
	DD	3056163219
	DD	3364005183
	DD	1597009353
	DD	2795223067
	DD	2229991998
	DD	2289174591
	DD	1817359143
	DD	2433043071
	DD	3194018707
	DD	1295478838
	DD	165016701
	DD	283381887
	DD	3634718287
	DD	571118846
	DD	2093070119
	DD	2590957677
	DD	330033402
	DD	566763775
	DD	2974469278
	DD	1142237692
	DD	4186140238
	DD	886948059
	DD	660066805
	DD	1133527550
	DD	1653971260
	DD	2284475384
	DD	4077313180
	DD	1773896118
	DD	1320133610
	DD	2267055100
	DD	3307942520
	DD	273983472
	DD	3859659065
	DD	3547792237
	DD	2640267220
	DD	239142905
	DD	2320917745
	DD	547966945
	DD	3424350834
	DD	2800617179
	DD	985567145
	DD	478285810
	DD	346868194
	DD	1095933890
	DD	2553734372
	DD	1306267062
	DD	1971134291
	DD	956571621
	DD	693736388
	DD	2191867780
	DD	812501448
	DD	2612534124
	DD	3942268582
	DD	1913143242
	DD	1387472776
	DD	88768265
	DD	1625002897
	DD	930100952
	DD	3589569869
	DD	3826286484
	DD	2774945552
	DD	177536531
	DD	3250005794
	DD	1860201905
	DD	2884172442
	DD	3357605672
	DD	1254923809
	DD	355073063
	DD	2205044292
	DD	3720403810
	DD	1473377588
	DD	2420244049
	DD	2509847619
	DD	710146126
	DD	115121288
	DD	3145840325
	DD	2946755177
	DD	545520802
	DD	724727943
	DD	1420292253
	DD	230242576
	DD	1996713354
	DD	1598543059
	DD	1091041605
	DD	1449455886
	DD	2840584506
	DD	460485152
	DD	3993426708
	DD	3197086118
	DD	2182083211
	DD	2898911772
	DD	1386201717
	DD	920970305
	DD	3691886121
	DD	2099204941
	DD	69199126
	DD	1502856249
	DD	2772403434
	DD	1841940610
	DD	3088804946
	DD	4198409883
	DD	138398252
	DD	3005712498
	DD	1249839573
	DD	3683881221
	DD	1882642597
	DD	4101852471
	DD	276796504
	DD	1716457700
	DD	2499679147
	DD	3072795146
	DD	3765285194
	DD	3908737646
	DD	553593009
	DD	3432915400
	DD	704390999
	DD	1850622997
	DD	3235603093
	DD	3522507997
	DD	1107186019
	DD	2570863504
	DD	1408781999
	DD	3701245994
	DD	2176238891
	DD	2750048699
	DD	2214372039
	DD	846759712
	DD	2817563999
	DD	3107524692
	DD	57510486
	DD	1205130103
	DD	133776782
	DD	1693519425
	DD	1340160702
	DD	1920082089
	DD	115020972
	DD	2410260206
	DD	267553565
	DD	3387038850
	DD	2680321405
	DD	3840164178
	DD	230041945
	DD	525553116
	DD	535107130
	DD	2479110404
	DD	1065675514
	DD	3385361061
	DD	460083891
	DD	1051106232
	DD	1070214261
	DD	663253512
	DD	2131351028
	DD	2475754826
	DD	920167782
	DD	2102212464
	DD	2140428522
	DD	1326507024
	DD	4262702056
	DD	656542356
	DD	1840335564
	DD	4204424928
	DD	4280857045
	DD	2653014048
	DD	4230436817
	DD	1313084713
	DD	3680671129
	DD	4113882560
	DD	4266746795
	DD	1011060801
	DD	4165906339
	DD	2626169427
	DD	3066374962
	DD	3932797825
	DD	4238526295
	DD	2022121603
	DD	4036845383
	DD	957371559
	DD	1837782628
	DD	3570628355
	DD	4182085295
	DD	4044243207
	DD	3778723471
	DD	1914743119
	DD	3675565257
	DD	2846289414
	DD	4069203294
	DD	3793519119
	DD	3262479647
	DD	3829486239
	DD	3056163219
	DD	1397611533
	DD	3843439293
	DD	3292070943
	DD	2229991998
	DD	3364005183
	DD	1817359143
	DD	2795223067
	DD	3391911291
	DD	2289174591
	DD	165016701
	DD	2433043071
	DD	3634718287
	DD	1295478838
	DD	2488855287
	DD	283381887
	DD	330033402
	DD	571118846
	DD	2974469278
	DD	2590957677
	DD	682743279
	DD	566763775
	DD	660066805
	DD	1142237692
	DD	1653971260
	DD	886948059
	DD	1365486558
	DD	1133527550
	DD	1320133610
	DD	2284475384
	DD	3307942520
	DD	1773896118
	DD	2730973117
	DD	2267055100
	DD	2640267220
	DD	273983472
	DD	2320917745
	DD	3547792237
	DD	1166978938
	DD	239142905
	DD	985567145
	DD	547966945
	DD	346868194
	DD	2800617179
	DD	2333957877
	DD	478285810
	DD	1971134291
	DD	1095933890
	DD	693736388
	DD	1306267062
	DD	372948459
	DD	956571621
	DD	3942268582
	DD	2191867780
	DD	1387472776
	DD	2612534124
	DD	745896919
	DD	1913143242
	DD	3589569869
	DD	88768265
	DD	2774945552
	DD	930100952
	DD	1491793838
	DD	3826286484
	DD	2884172442
	DD	177536531
	DD	1254923809
	DD	1860201905
	DD	2983587677
	DD	3357605672
	DD	1473377588
	DD	355073063
	DD	2509847619
	DD	3720403810
	DD	1672208059
	DD	2420244049
	DD	2946755177
	DD	710146126
	DD	724727943
	DD	3145840325
	DD	3344416119
	DD	545520802
	DD	1598543059
	DD	1420292253
	DD	1449455886
	DD	1996713354
	DD	2393864943
	DD	1091041605
	DD	3197086118
	DD	2840584506
	DD	2898911772
	DD	3993426708
	DD	492762590
	DD	2182083211
	DD	2099204941
	DD	1386201717
	DD	1502856249
	DD	3691886121
	DD	985525180
	DD	69199126
	DD	4198409883
	DD	2772403434
	DD	3005712498
	DD	3088804946
	DD	1971050360
	DD	138398252
	DD	4101852471
	DD	1249839573
	DD	1716457700
	DD	1882642597
	DD	3942100721
	DD	276796504
	DD	3908737646
	DD	2499679147
	DD	3432915400
	DD	3765285194
	DD	3589234146
	DD	553593009
	DD	3522507997
	DD	704390999
	DD	2570863504
	DD	3235603093
	DD	2883500997
	DD	1107186019
	DD	2750048699
	DD	1408781999
	DD	846759712
	DD	2176238891
	DD	1472034698
	DD	2214372039
	DD	1205130103
	DD	2817563999
	DD	1693519425
	DD	57510486
	DD	2944069397
	DD	133776782
	DD	2410260206
	DD	1340160702
	DD	3387038850
	DD	115020972
	DD	1593171499
	DD	267553565
	DD	525553116
	DD	2680321405
	DD	2479110404
	DD	230041945
	DD	3186342998
	DD	535107130
	DD	1051106232
	DD	1065675514
	DD	663253512
	DD	460083891
	DD	2077718700
	DD	1070214261
	DD	2102212464
	DD	2131351028
	DD	1326507024
	DD	920167782
	DD	4155437400
	DD	2140428522
	DD	4204424928
	DD	4262702056
	DD	2653014048
	DD	1840335564
	DD	4015907504
	DD	4280857045
	DD	4113882560
	DD	4230436817
	DD	1011060801
	DD	3680671129
	DD	3736847713
	DD	4266746795
	DD	3932797825
	DD	4165906339
	DD	2022121603
	DD	3066374962
	DD	3178728131
	DD	4238526295
	DD	3570628355
	DD	4036845383
	DD	4044243207
	DD	1837782628
	DD	2062488966
	DD	4182085295
	DD	2846289414
	DD	3778723471
	DD	3793519119
	DD	3675565257
	DD	4124977933
	DD	4069203294
	DD	1397611533
	DD	3262479647
	DD	3292070943
	DD	3056163219
	DD	3954988571
	DD	3843439293
	DD	2795223067
	DD	2229991998
	DD	2289174591
	DD	1817359143
	DD	3615009846
	DD	3391911291
	DD	1295478838
	DD	165016701
	DD	283381887
	DD	3634718287
	DD	2935052397
	DD	2488855287
	DD	2590957677
	DD	330033402
	DD	566763775
	DD	2974469278
	DD	1575137499
	DD	682743279
	DD	886948059
	DD	660066805
	DD	1133527550
	DD	1653971260
	DD	3150274999
	DD	1365486558
	DD	1773896118
	DD	1320133610
	DD	2267055100
	DD	3307942520
	DD	2005582702
	DD	2730973117
	DD	3547792237
	DD	2640267220
	DD	239142905
	DD	2320917745
	DD	4011165404
	DD	1166978938
	DD	2800617179
	DD	985567145
	DD	478285810
	DD	346868194
	DD	3727363513
	DD	2333957877
	DD	1306267062
	DD	1971134291
	DD	956571621
	DD	693736388
	DD	3159759730
	DD	372948459
	DD	2612534124
	DD	3942268582
	DD	1913143242
	DD	1387472776
	DD	2024552164
	DD	745896919
	DD	930100952
	DD	3589569869
	DD	3826286484
	DD	2774945552
	DD	4049104329
	DD	1491793838
	DD	1860201905
	DD	2884172442
	DD	3357605672
	DD	1254923809
	DD	3803241362
	DD	2983587677
	DD	3720403810
	DD	1473377588
	DD	2420244049
	DD	2509847619
	DD	3311515428
	DD	1672208059
	DD	3145840325
	DD	2946755177
	DD	545520802
	DD	724727943
	DD	2328063560
	DD	3344416119
	DD	1996713354
	DD	1598543059
	DD	1091041605
	DD	1449455886
	DD	361159825
	DD	2393864943
	DD	3993426708
	DD	3197086118
	DD	2182083211
	DD	2898911772
	DD	722319651
	DD	492762590
	DD	3691886121
	DD	2099204941
	DD	69199126
	DD	1502856249
	DD	1444639302
	DD	985525180
	DD	3088804946
	DD	4198409883
	DD	138398252
	DD	3005712498
	DD	2889278605
	DD	1971050360
	DD	1882642597
	DD	4101852471
	DD	276796504
	DD	1716457700
	DD	1483589915
	DD	3942100721
	DD	3765285194
	DD	3908737646
	DD	553593009
	DD	3432915400
	DD	2967179831
	DD	3589234146
	DD	3235603093
	DD	3522507997
	DD	1107186019
	DD	2570863504
	DD	1639392366
	DD	2883500997
	DD	2176238891
	DD	2750048699
	DD	2214372039
	DD	846759712
	DD	3278784732
	DD	1472034698
	DD	57510486
	DD	1205130103
	DD	133776782
	DD	1693519425
	DD	2262602168
	DD	2944069397
	DD	115020972
	DD	2410260206
	DD	267553565
	DD	3387038850
	DD	230237041
	DD	1593171499
	DD	230041945
	DD	525553116
	DD	535107130
	DD	2479110404
	DD	460474083
	DD	3186342998
	DD	460083891
	DD	1051106232
	DD	1070214261
	DD	663253512
	DD	920948167
	DD	2077718700
	DD	920167782
	DD	2102212464
	DD	2140428522
	DD	1326507024
	DD	1841896334
	DD	4155437400
	DD	1840335564
	DD	4204424928
	DD	4280857045
	DD	2653014048
	DD	3683792669
	DD	4015907504
	DD	3680671129
	DD	4113882560
	DD	4266746795
	DD	1011060801
	DD	3072618042
	DD	3736847713
	DD	3066374962
	DD	3932797825
	DD	4238526295
	DD	2022121603
	DD	1850268788
	DD	3178728131
	DD	1837782628
	DD	3570628355
	DD	4182085295
	DD	4044243207
	DD	3700537577
	DD	2062488966
	DD	3675565257
	DD	2846289414
	DD	4069203294
	DD	3793519119
	DD	3106107858
	DD	4124977933
	DD	3056163219
	DD	1397611533
	DD	3843439293
	DD	3292070943
	DD	1917248420
	DD	3954988571
	DD	1817359143
	DD	2795223067
	DD	3391911291
	DD	2289174591
	DD	3834496840
	DD	3615009846
	DD	3634718287
	DD	1295478838
	DD	2488855287
	DD	283381887
	DD	3374026384
	DD	2935052397
	DD	2974469278
	DD	2590957677
	DD	682743279
	DD	566763775
	DD	2453085473
	DD	1575137499
	DD	1653971260
	DD	886948059
	DD	1365486558
	DD	1133527550
	DD	611203650
	DD	3150274999
	DD	3307942520
	DD	1773896118
	DD	2730973117
	DD	2267055100
	DD	1222407300
	DD	2005582702
	DD	2320917745
	DD	3547792237
	DD	1166978938
	DD	239142905
	DD	2444814601
	DD	4011165404
	DD	346868194
	DD	2800617179
	DD	2333957877
	DD	478285810
	DD	594661906
	DD	3727363513
	DD	693736388
	DD	1306267062
	DD	372948459
	DD	956571621
	DD	1189323812
	DD	3159759730
	DD	1387472776
	DD	2612534124
	DD	745896919
	DD	1913143242
	DD	2378647625
	DD	2024552164
	DD	2774945552
	DD	930100952
	DD	1491793838
	DD	3826286484
	DD	462327955
	DD	4049104329
	DD	1254923809
	DD	1860201905
	DD	2983587677
	DD	3357605672
	DD	924655910
	DD	3803241362
	DD	2509847619
	DD	3720403810
	DD	1672208059
	DD	2420244049
	DD	1849311821
	DD	3311515428
	DD	724727943
	DD	3145840325
	DD	3344416119
	DD	545520802
	DD	3698623643
	DD	2328063560
	DD	1449455886
	DD	1996713354
	DD	2393864943
	DD	1091041605
	DD	3102279991
	DD	361159825
	DD	2898911772
	DD	3993426708
	DD	492762590
	DD	2182083211
	DD	1909592686
	DD	722319651
	DD	1502856249
	DD	3691886121
	DD	985525180
	DD	69199126
	DD	3819185373
	DD	1444639302
	DD	3005712498
	DD	3088804946
	DD	1971050360
	DD	138398252
	DD	3343403450
	DD	2889278605
	DD	1716457700
	DD	1882642597
	DD	3942100721
	DD	276796504
	DD	2391839604
	DD	1483589915
	DD	3432915400
	DD	3765285194
	DD	3589234146
	DD	553593009
	DD	488711913
	DD	2967179831
	DD	2570863504
	DD	3235603093
	DD	2883500997
	DD	1107186019
	DD	977423826
	DD	1639392366
	DD	846759712
	DD	2176238891
	DD	1472034698
	DD	2214372039
	DD	1954847653
	DD	3278784732
	DD	1693519425
	DD	57510486
	DD	2944069397
	DD	133776782
	DD	3909695307
	DD	2262602168
	DD	3387038850
	DD	115020972
	DD	1593171499
	DD	267553565
	DD	3524423319
	DD	230237041
	DD	2479110404
	DD	230041945
	DD	3186342998
	DD	535107130
	DD	2753879342
	DD	460474083
	DD	663253512
	DD	460083891
	DD	2077718700
	DD	1070214261
	DD	1212791388
	DD	920948167
	DD	1326507024
	DD	920167782
	DD	4155437400
	DD	2140428522
	DD	2425582776
	DD	1841896334
	DD	2653014048
	DD	1840335564
	DD	4015907504
	DD	4280857045
	DD	556198256
	DD	3683792669
	DD	1011060801
	DD	3680671129
	DD	3736847713
	DD	4266746795
	DD	1112396512
	DD	3072618042
	DD	2022121603
	DD	3066374962
	DD	3178728131
	DD	4238526295
	DD	2224793024
	DD	1850268788
	DD	4044243207
	DD	1837782628
	DD	2062488966
	DD	4182085295
	DD	154618752
	DD	3700537577
	DD	3793519119
	DD	3675565257
	DD	4124977933
	DD	4069203294
	DD	309237504
	DD	3106107858
	DD	3292070943
	DD	3056163219
	DD	3954988571
	DD	3843439293
	DD	618475008
	DD	1917248420
	DD	2289174591
	DD	1817359143
	DD	3615009846
	DD	3391911291
	DD	1236950016
	DD	3834496840
	DD	283381887
	DD	3634718287
	DD	2935052397
	DD	2488855287
	DD	2473900033
	DD	3374026384
	DD	566763775
	DD	2974469278
	DD	1575137499
	DD	682743279
	DD	652832771
	DD	2453085473
	DD	1133527550
	DD	1653971260
	DD	3150274999
	DD	1365486558
	DD	1305665542
	DD	611203650
	DD	2267055100
	DD	3307942520
	DD	2005582702
	DD	2730973117
	DD	2611331084
	DD	1222407300
	DD	239142905
	DD	2320917745
	DD	4011165404
	DD	1166978938
	DD	927694873
	DD	2444814601
	DD	478285810
	DD	346868194
	DD	3727363513
	DD	2333957877
	DD	1855389746
	DD	594661906
	DD	956571621
	DD	693736388
	DD	3159759730
	DD	372948459
	DD	3710779492
	DD	1189323812
	DD	1913143242
	DD	1387472776
	DD	2024552164
	DD	745896919
	DD	3126591689
	DD	2378647625
	DD	3826286484
	DD	2774945552
	DD	4049104329
	DD	1491793838
	DD	1958216082
	DD	462327955
	DD	3357605672
	DD	1254923809
	DD	3803241362
	DD	2983587677
	DD	3916432164
	DD	924655910
	DD	2420244049
	DD	2509847619
	DD	3311515428
	DD	1672208059
	DD	3537897033
	DD	1849311821
	DD	545520802
	DD	724727943
	DD	2328063560
	DD	3344416119
	DD	2780826770
	DD	3698623643
	DD	1091041605
	DD	1449455886
	DD	361159825
	DD	2393864943
	DD	1266686244
	DD	3102279991
	DD	2182083211
	DD	2898911772
	DD	722319651
	DD	492762590
	DD	2533372489
	DD	1909592686
	DD	69199126
	DD	1502856249
	DD	1444639302
	DD	985525180
	DD	771777682
	DD	3819185373
	DD	138398252
	DD	3005712498
	DD	2889278605
	DD	1971050360
	DD	1543555365
	DD	3343403450
	DD	276796504
	DD	1716457700
	DD	1483589915
	DD	3942100721
	DD	3087110731
	DD	2391839604
	DD	553593009
	DD	3432915400
	DD	2967179831
	DD	3589234146
	DD	1879254167
	DD	488711913
	DD	1107186019
	DD	2570863504
	DD	1639392366
	DD	2883500997
	DD	3758508334
	DD	977423826
	DD	2214372039
	DD	846759712
	DD	3278784732
	DD	1472034698
	DD	3222049373
	DD	1954847653
	DD	133776782
	DD	1693519425
	DD	2262602168
	DD	2944069397
	DD	2149131451
	DD	3909695307
	DD	267553565
	DD	3387038850
	DD	230237041
	DD	1593171499
	DD	3295607
	DD	3524423319
	DD	535107130
	DD	2479110404
	DD	460474083
	DD	3186342998
	DD	6591214
	DD	2753879342
	DD	1070214261
	DD	663253512
	DD	920948167
	DD	2077718700
	DD	13182429
	DD	1212791388
	DD	2140428522
	DD	1326507024
	DD	1841896334
	DD	4155437400
	DD	26364858
	DD	2425582776
	DD	4280857045
	DD	2653014048
	DD	3683792669
	DD	4015907504
	DD	52729717
	DD	556198256
	DD	4266746795
	DD	1011060801
	DD	3072618042
	DD	3736847713
	DD	105459434
	DD	1112396512
	DD	4238526295
	DD	2022121603
	DD	1850268788
	DD	3178728131
	DD	210918868
	DD	2224793024
	DD	4182085295
	DD	4044243207
	DD	3700537577
	DD	2062488966
	DD	421837736
	DD	154618752
	DD	4069203294
	DD	3793519119
	DD	3106107858
	DD	4124977933
	DD	843675472
	DD	309237504
	DD	3843439293
	DD	3292070943
	DD	1917248420
	DD	3954988571
	DD	1687350944
	DD	618475008
	DD	3391911291
	DD	2289174591
	DD	3834496840
	DD	3615009846
	DD	3374701889
	DD	1236950016
	DD	2488855287
	DD	283381887
	DD	3374026384
	DD	2935052397
	DD	2454436482
	DD	2473900033
	DD	682743279
	DD	566763775
	DD	2453085473
	DD	1575137499
	DD	613905668
	DD	652832771
	DD	1365486558
	DD	1133527550
	DD	611203650
	DD	3150274999
	DD	1227811337
	DD	1305665542
	DD	2730973117
	DD	2267055100
	DD	1222407300
	DD	2005582702
	DD	2455622675
	DD	2611331084
	DD	1166978938
	DD	239142905
	DD	2444814601
	DD	4011165404
	DD	616278055
	DD	927694873
	DD	2333957877
	DD	478285810
	DD	594661906
	DD	3727363513
	DD	1232556110
	DD	1855389746
	DD	372948459
	DD	956571621
	DD	1189323812
	DD	3159759730
	DD	2465112221
	DD	3710779492
	DD	745896919
	DD	1913143242
	DD	2378647625
	DD	2024552164
	DD	635257146
	DD	3126591689
	DD	1491793838
	DD	3826286484
	DD	462327955
	DD	4049104329
	DD	1270514292
	DD	1958216082
	DD	2983587677
	DD	3357605672
	DD	924655910
	DD	3803241362
	DD	2541028584
	DD	3916432164
	DD	1672208059
	DD	2420244049
	DD	1849311821
	DD	3311515428
	DD	787089873
	DD	3537897033
	DD	3344416119
	DD	545520802
	DD	3698623643
	DD	2328063560
	DD	1574179747
	DD	2780826770
	DD	2393864943
	DD	1091041605
	DD	3102279991
	DD	361159825
	DD	3148359494
	DD	1266686244
	DD	492762590
	DD	2182083211
	DD	1909592686
	DD	722319651
	DD	2001751692
	DD	2533372489
	DD	985525180
	DD	69199126
	DD	3819185373
	DD	1444639302
	DD	4003503385
	DD	771777682
	DD	1971050360
	DD	138398252
	DD	3343403450
	DD	2889278605
	DD	3712039474
	DD	1543555365
	DD	3942100721
	DD	276796504
	DD	2391839604
	DD	1483589915
	DD	3129111652
	DD	3087110731
	DD	3589234146
	DD	553593009
	DD	488711913
	DD	2967179831
	DD	1963256009
	DD	1879254167
	DD	2883500997
	DD	1107186019
	DD	977423826
	DD	1639392366
	DD	3926512018
	DD	3758508334
	DD	1472034698
	DD	2214372039
	DD	1954847653
	DD	3278784732
	DD	3558056740
	DD	3222049373
	DD	2944069397
	DD	133776782
	DD	3909695307
	DD	2262602168
	DD	2821146184
	DD	2149131451
	DD	1593171499
	DD	267553565
	DD	3524423319
	DD	230237041
	DD	1347325072
	DD	3295607
	DD	3186342998
	DD	535107130
	DD	2753879342
	DD	460474083
	DD	2694650145
	DD	6591214
	DD	2077718700
	DD	1070214261
	DD	1212791388
	DD	920948167
	DD	1094332995
	DD	13182429
	DD	4155437400
	DD	2140428522
	DD	2425582776
	DD	1841896334
	DD	2188665991
	DD	26364858
	DD	4015907504
	DD	4280857045
	DD	556198256
	DD	3683792669
	DD	82364686
	DD	52729717
	DD	3736847713
	DD	4266746795
	DD	1112396512
	DD	3072618042
	DD	164729372
	DD	105459434
	DD	3178728131
	DD	4238526295
	DD	2224793024
	DD	1850268788
	DD	329458745
	DD	210918868
	DD	2062488966
	DD	4182085295
	DD	154618752
	DD	3700537577
	DD	658917491
	DD	421837736
	DD	4124977933
	DD	4069203294
	DD	309237504
	DD	3106107858
	DD	1317834983
	DD	843675472
	DD	3954988571
	DD	3843439293
	DD	618475008
	DD	1917248420
	DD	2635669967
	DD	1687350944
	DD	3615009846
	DD	3391911291
	DD	1236950016
	DD	3834496840
	DD	976372639
	DD	3374701889
	DD	2935052397
	DD	2488855287
	DD	2473900033
	DD	3374026384
	DD	1952745279
	DD	2454436482
	DD	1575137499
	DD	682743279
	DD	652832771
	DD	2453085473
	DD	3905490559
	DD	613905668
	DD	3150274999
	DD	1365486558
	DD	1305665542
	DD	611203650
	DD	3516013822
	DD	1227811337
	DD	2005582702
	DD	2730973117
	DD	2611331084
	DD	1222407300
	DD	2737060348
	DD	2455622675
	DD	4011165404
	DD	1166978938
	DD	927694873
	DD	2444814601
	DD	1179153400
	DD	616278055
	DD	3727363513
	DD	2333957877
	DD	1855389746
	DD	594661906
	DD	2358306800
	DD	1232556110
	DD	3159759730
	DD	372948459
	DD	3710779492
	DD	1189323812
	DD	421646305
	DD	2465112221
	DD	2024552164
	DD	745896919
	DD	3126591689
	DD	2378647625
	DD	843292611
	DD	635257146
	DD	4049104329
	DD	1491793838
	DD	1958216082
	DD	462327955
	DD	1686585223
	DD	1270514292
	DD	3803241362
	DD	2983587677
	DD	3916432164
	DD	924655910
	DD	3373170446
	DD	2541028584
	DD	3311515428
	DD	1672208059
	DD	3537897033
	DD	1849311821
	DD	2451373597
	DD	787089873
	DD	2328063560
	DD	3344416119
	DD	2780826770
	DD	3698623643
	DD	607779899
	DD	1574179747
	DD	361159825
	DD	2393864943
	DD	1266686244
	DD	3102279991
	DD	1215559799
	DD	3148359494
	DD	722319651
	DD	492762590
	DD	2533372489
	DD	1909592686
	DD	2431119599
	DD	2001751692
	DD	1444639302
	DD	985525180
	DD	771777682
	DD	3819185373
	DD	567271902
	DD	4003503385
	DD	2889278605
	DD	1971050360
	DD	1543555365
	DD	3343403450
	DD	1134543805
	DD	3712039474
	DD	1483589915
	DD	3942100721
	DD	3087110731
	DD	2391839604
	DD	2269087610
	DD	3129111652
	DD	2967179831
	DD	3589234146
	DD	1879254167
	DD	488711913
	DD	243207925
	DD	1963256009
	DD	1639392366
	DD	2883500997
	DD	3758508334
	DD	977423826
	DD	486415851
	DD	3926512018
	DD	3278784732
	DD	1472034698
	DD	3222049373
	DD	1954847653
	DD	972831702
	DD	3558056740
	DD	2262602168
	DD	2944069397
	DD	2149131451
	DD	3909695307
	DD	1945663404
	DD	2821146184
	DD	230237041
	DD	1593171499
	DD	3295607
	DD	3524423319
	DD	3891326808
	DD	1347325072
	DD	460474083
	DD	3186342998
	DD	6591214
	DD	2753879342
	DD	3487686321
	DD	2694650145
	DD	920948167
	DD	2077718700
	DD	13182429
	DD	1212791388
	DD	2680405347
	DD	1094332995
	DD	1841896334
	DD	4155437400
	DD	26364858
	DD	2425582776
	DD	1065843399
	DD	2188665991
	DD	3683792669
	DD	4015907504
	DD	52729717
	DD	556198256
	DD	2131686798
	DD	82364686
	DD	3072618042
	DD	3736847713
	DD	105459434
	DD	1112396512
	DD	4263373596
	DD	164729372
	DD	1850268788
	DD	3178728131
	DD	210918868
	DD	2224793024
	DD	4231779897
	DD	329458745
	DD	3700537577
	DD	2062488966
	DD	421837736
	DD	154618752
	DD	4168592498
	DD	658917491
	DD	3106107858
	DD	4124977933
	DD	843675472
	DD	309237504
	DD	4042217701
	DD	1317834983
	DD	1917248420
	DD	3954988571
	DD	1687350944
	DD	618475008
	DD	3789468107
	DD	2635669967
	DD	3834496840
	DD	3615009846
	DD	3374701889
	DD	1236950016
	DD	3283968918
	DD	976372639
	DD	3374026384
	DD	2935052397
	DD	2454436482
	DD	2473900033
	DD	2272970540
	DD	1952745279
	DD	2453085473
	DD	1575137499
	DD	613905668
	DD	652832771
	DD	250973784
	DD	3905490559
	DD	611203650
	DD	3150274999
	DD	1227811337
	DD	1305665542
	DD	501947569
	DD	3516013822
	DD	1222407300
	DD	2005582702
	DD	2455622675
	DD	2611331084
	DD	1003895138
	DD	2737060348
	DD	2444814601
	DD	4011165404
	DD	616278055
	DD	927694873
	DD	2007790276
	DD	1179153400
	DD	594661906
	DD	3727363513
	DD	1232556110
	DD	1855389746
	DD	4015580553
	DD	2358306800
	DD	1189323812
	DD	3159759730
	DD	2465112221
	DD	3710779492
	DD	3736193810
	DD	421646305
	DD	2378647625
	DD	2024552164
	DD	635257146
	DD	3126591689
	DD	3177420325
	DD	843292611
	DD	462327955
	DD	4049104329
	DD	1270514292
	DD	1958216082
	DD	2059873354
	DD	1686585223
	DD	924655910
	DD	3803241362
	DD	2541028584
	DD	3916432164
	DD	4119746708
	DD	3373170446
	DD	1849311821
	DD	3311515428
	DD	787089873
	DD	3537897033
	DD	3944526121
	DD	2451373597
	DD	3698623643
	DD	2328063560
	DD	1574179747
	DD	2780826770
	DD	3594084947
	DD	607779899
	DD	3102279991
	DD	361159825
	DD	3148359494
	DD	1266686244
	DD	2893202598
	DD	1215559799
	DD	1909592686
	DD	722319651
	DD	2001751692
	DD	2533372489
	DD	1491437901
	DD	2431119599
	DD	3819185373
	DD	1444639302
	DD	4003503385
	DD	771777682
	DD	2982875802
	DD	567271902
	DD	3343403450
	DD	2889278605
	DD	3712039474
	DD	1543555365
	DD	1670784308
	DD	1134543805
	DD	2391839604
	DD	1483589915
	DD	3129111652
	DD	3087110731
	DD	3341568617
	DD	2269087610
	DD	488711913
	DD	2967179831
	DD	1963256009
	DD	1879254167
	DD	2388169939
	DD	243207925
	DD	977423826
	DD	1639392366
	DD	3926512018
	DD	3758508334
	DD	481372583
	DD	486415851
	DD	1954847653
	DD	3278784732
	DD	3558056740
	DD	3222049373
	DD	962745166
	DD	972831702
	DD	3909695307
	DD	2262602168
	DD	2821146184
	DD	2149131451
	DD	1925490332
	DD	1945663404
	DD	3524423319
	DD	230237041
	DD	1347325072
	DD	3295607
	DD	3850980665
	DD	3891326808
	DD	2753879342
	DD	460474083
	DD	2694650145
	DD	6591214
	DD	3406994035
	DD	3487686321
	DD	1212791388
	DD	920948167
	DD	1094332995
	DD	13182429
	DD	2519020775
	DD	2680405347
	DD	2425582776
	DD	1841896334
	DD	2188665991
	DD	26364858
	DD	743074255
	DD	1065843399
	DD	556198256
	DD	3683792669
	DD	82364686
	DD	52729717
	DD	1486148511
	DD	2131686798
	DD	1112396512
	DD	3072618042
	DD	164729372
	DD	105459434
	DD	2972297022
	DD	4263373596
	DD	2224793024
	DD	1850268788
	DD	329458745
	DD	210918868
	DD	1649626749
	DD	4231779897
	DD	154618752
	DD	3700537577
	DD	658917491
	DD	421837736
	DD	3299253499
	DD	4168592498
	DD	309237504
	DD	3106107858
	DD	1317834983
	DD	843675472
	DD	2303539703
	DD	4042217701
	DD	618475008
	DD	1917248420
	DD	2635669967
	DD	1687350944
	DD	312112110
	DD	3789468107
	DD	1236950016
	DD	3834496840
	DD	976372639
	DD	3374701889
	DD	624224221
	DD	3283968918
	DD	2473900033
	DD	3374026384
	DD	1952745279
	DD	2454436482
	DD	1248448442
	DD	2272970540
	DD	652832771
	DD	2453085473
	DD	3905490559
	DD	613905668
	DD	2496896884
	DD	250973784
	DD	1305665542
	DD	611203650
	DD	3516013822
	DD	1227811337
	DD	698826472
	DD	501947569
	DD	2611331084
	DD	1222407300
	DD	2737060348
	DD	2455622675
	DD	1397652945
	DD	1003895138
	DD	927694873
	DD	2444814601
	DD	1179153400
	DD	616278055
	DD	2795305890
	DD	2007790276
	DD	1855389746
	DD	594661906
	DD	2358306800
	DD	1232556110
	DD	1295644484
	DD	4015580553
	DD	3710779492
	DD	1189323812
	DD	421646305
	DD	2465112221
	DD	2591288968
	DD	3736193810
	DD	3126591689
	DD	2378647625
	DD	843292611
	DD	635257146
	DD	887610640
	DD	3177420325
	DD	1958216082
	DD	462327955
	DD	1686585223
	DD	1270514292
	DD	1775221280
	DD	2059873354
	DD	3916432164
	DD	924655910
	DD	3373170446
	DD	2541028584
	DD	3550442561
	DD	4119746708
	DD	3537897033
	DD	1849311821
	DD	2451373597
	DD	787089873
	DD	2805917826
	DD	3944526121
	DD	2780826770
	DD	3698623643
	DD	607779899
	DD	1574179747
	DD	1316868356
	DD	3594084947
	DD	1266686244
	DD	3102279991
	DD	1215559799
	DD	3148359494
	DD	2633736712
	DD	2893202598
	DD	2533372489
	DD	1909592686
	DD	2431119599
	DD	2001751692
	DD	972506129
	DD	1491437901
	DD	771777682
	DD	3819185373
	DD	567271902
	DD	4003503385
	DD	1945012259
	DD	2982875802
	DD	1543555365
	DD	3343403450
	DD	1134543805
	DD	3712039474
	DD	3890024518
	DD	1670784308
	DD	3087110731
	DD	2391839604
	DD	2269087610
	DD	3129111652
	DD	3485081741
	DD	3341568617
	DD	1879254167
	DD	488711913
	DD	243207925
	DD	1963256009
	DD	2675196186
	DD	2388169939
	DD	3758508334
	DD	977423826
	DD	486415851
	DD	3926512018
	DD	1055425077
	DD	481372583
	DD	3222049373
	DD	1954847653
	DD	972831702
	DD	3558056740
	DD	2110850155
	DD	962745166
	DD	2149131451
	DD	3909695307
	DD	1945663404
	DD	2821146184
	DD	4221700311
	DD	1925490332
	DD	3295607
	DD	3524423319
	DD	3891326808
	DD	1347325072
	DD	4148433327
	DD	3850980665
	DD	6591214
	DD	2753879342
	DD	3487686321
	DD	2694650145
	DD	4001899359
	DD	3406994035
	DD	13182429
	DD	1212791388
	DD	2680405347
	DD	1094332995
	DD	3708831422
	DD	2519020775
	DD	26364858
	DD	2425582776
	DD	1065843399
	DD	2188665991
	DD	3122695549
	DD	743074255
	DD	52729717
	DD	556198256
	DD	2131686798
	DD	82364686
	DD	1950423802
	DD	1486148511
	DD	105459434
	DD	1112396512
	DD	4263373596
	DD	164729372
	DD	3900847605
	DD	2972297022
	DD	210918868
	DD	2224793024
	DD	4231779897
	DD	329458745
	DD	3506727914
	DD	1649626749
	DD	421837736
	DD	154618752
	DD	4168592498
	DD	658917491
	DD	2718488532
	DD	3299253499
	DD	843675472
	DD	309237504
	DD	4042217701
	DD	1317834983
	DD	1142009769
	DD	2303539703
	DD	1687350944
	DD	618475008
	DD	3789468107
	DD	2635669967
	DD	2284019538
	DD	312112110
	DD	3374701889
	DD	1236950016
	DD	3283968918
	DD	976372639
	DD	273071781
	DD	624224221
	DD	2454436482
	DD	2473900033
	DD	2272970540
	DD	1952745279
	DD	546143563
	DD	1248448442
	DD	613905668
	DD	652832771
	DD	250973784
	DD	3905490559
	DD	1092287127
	DD	2496896884
	DD	1227811337
	DD	1305665542
	DD	501947569
	DD	3516013822
	DD	2184574254
	DD	698826472
	DD	2455622675
	DD	2611331084
	DD	1003895138
	DD	2737060348
	DD	74181213
	DD	1397652945
	DD	616278055
	DD	927694873
	DD	2007790276
	DD	1179153400
	DD	148362426
	DD	2795305890
	DD	1232556110
	DD	1855389746
	DD	4015580553
	DD	2358306800
	DD	296724853
	DD	1295644484
	DD	2465112221
	DD	3710779492
	DD	3736193810
	DD	421646305
	DD	593449707
	DD	2591288968
	DD	635257146
	DD	3126591689
	DD	3177420325
	DD	843292611
	DD	1186899415
	DD	887610640
	DD	1270514292
	DD	1958216082
	DD	2059873354
	DD	1686585223
	DD	2373798830
	DD	1775221280
	DD	2541028584
	DD	3916432164
	DD	4119746708
	DD	3373170446
	DD	452630365
	DD	3550442561
	DD	787089873
	DD	3537897033
	DD	3944526121
	DD	2451373597
	DD	905260731
	DD	2805917826
	DD	1574179747
	DD	2780826770
	DD	3594084947
	DD	607779899
	DD	1810521462
	DD	1316868356
	DD	3148359494
	DD	1266686244
	DD	2893202598
	DD	1215559799
	DD	3621042925
	DD	2633736712
	DD	2001751692
	DD	2533372489
	DD	1491437901
	DD	2431119599
	DD	2947118554
	DD	972506129
	DD	4003503385
	DD	771777682
	DD	2982875802
	DD	567271902
	DD	1599269812
	DD	1945012259
	DD	3712039474
	DD	1543555365
	DD	1670784308
	DD	1134543805
	DD	3198539624
	DD	3890024518
	DD	3129111652
	DD	3087110731
	DD	3341568617
	DD	2269087610
	DD	2102111953
	DD	3485081741
	DD	1963256009
	DD	1879254167
	DD	2388169939
	DD	243207925
	DD	4204223906
	DD	2675196186
	DD	3926512018
	DD	3758508334
	DD	481372583
	DD	486415851
	DD	4113480516
	DD	1055425077
	DD	3558056740
	DD	3222049373
	DD	962745166
	DD	972831702
	DD	3931993737
	DD	2110850155
	DD	2821146184
	DD	2149131451
	DD	1925490332
	DD	1945663404
	DD	3569020178
	DD	4221700311
	DD	1347325072
	DD	3295607
	DD	3850980665
	DD	3891326808
	DD	2843073060
	DD	4148433327
	DD	2694650145
	DD	6591214
	DD	3406994035
	DD	3487686321
	DD	1391178824
	DD	4001899359
	DD	1094332995
	DD	13182429
	DD	2519020775
	DD	2680405347
	DD	2782357648
	DD	3708831422
	DD	2188665991
	DD	26364858
	DD	743074255
	DD	1065843399
	DD	1269748001
	DD	3122695549
	DD	82364686
	DD	52729717
	DD	1486148511
	DD	2131686798
	DD	2539496002
	DD	1950423802
	DD	164729372
	DD	105459434
	DD	2972297022
	DD	4263373596
	DD	784024708
	DD	3900847605
	DD	329458745
	DD	210918868
	DD	1649626749
	DD	4231779897
	DD	1568049417
	DD	3506727914
	DD	658917491
	DD	421837736
	DD	3299253499
	DD	4168592498
	DD	3136098835
	DD	2718488532
	DD	1317834983
	DD	843675472
	DD	2303539703
	DD	4042217701
	DD	1977230375
	DD	1142009769
	DD	2635669967
	DD	1687350944
	DD	312112110
	DD	3789468107
	DD	3954460750
	DD	2284019538
	DD	976372639
	DD	3374701889
	DD	624224221
	DD	3283968918
	DD	3613954205
	DD	273071781
	DD	1952745279
	DD	2454436482
	DD	1248448442
	DD	2272970540
	DD	2932941114
	DD	546143563
	DD	3905490559
	DD	613905668
	DD	2496896884
	DD	250973784
	DD	1570914932
	DD	1092287127
	DD	3516013822
	DD	1227811337
	DD	698826472
	DD	501947569
	DD	3141829865
	DD	2184574254
	DD	2737060348
	DD	2455622675
	DD	1397652945
	DD	1003895138
	DD	1988692435
	DD	74181213
	DD	1179153400
	DD	616278055
	DD	2795305890
	DD	2007790276
	DD	3977384870
	DD	148362426
	DD	2358306800
	DD	1232556110
	DD	1295644484
	DD	4015580553
	DD	3659802444
	DD	296724853
	DD	421646305
	DD	2465112221
	DD	2591288968
	DD	3736193810
	DD	3024637593
	DD	593449707
	DD	843292611
	DD	635257146
	DD	887610640
	DD	3177420325
	DD	1754307891
	DD	1186899415
	DD	1686585223
	DD	1270514292
	DD	1775221280
	DD	2059873354
	DD	3508615783
	DD	2373798830
	DD	3373170446
	DD	2541028584
	DD	3550442561
	DD	4119746708
	DD	2722264270
	DD	452630365
	DD	2451373597
	DD	787089873
	DD	2805917826
	DD	3944526121
	DD	1149561244
	DD	905260731
	DD	607779899
	DD	1574179747
	DD	1316868356
	DD	3594084947
	DD	2299122488
	DD	1810521462
	DD	1215559799
	DD	3148359494
	DD	2633736712
	DD	2893202598
	DD	303277681
	DD	3621042925
	DD	2431119599
	DD	2001751692
	DD	972506129
	DD	1491437901
	DD	606555363
	DD	2947118554
	DD	567271902
	DD	4003503385
	DD	1945012259
	DD	2982875802
	DD	1213110727
	DD	1599269812
	DD	1134543805
	DD	3712039474
	DD	3890024518
	DD	1670784308
	DD	2426221454
	DD	3198539624
	DD	2269087610
	DD	3129111652
	DD	3485081741
	DD	3341568617
	DD	557475612
	DD	2102111953
	DD	243207925
	DD	1963256009
	DD	2675196186
	DD	2388169939
	DD	1114951224
	DD	4204223906
	DD	486415851
	DD	3926512018
	DD	1055425077
	DD	481372583
	DD	2229902448
	DD	4113480516
	DD	972831702
	DD	3558056740
	DD	2110850155
	DD	962745166
	DD	164837600
	DD	3931993737
	DD	1945663404
	DD	2821146184
	DD	4221700311
	DD	1925490332
	DD	329675200
	DD	3569020178
	DD	3891326808
	DD	1347325072
	DD	4148433327
	DD	3850980665
	DD	659350401
	DD	2843073060
	DD	3487686321
	DD	2694650145
	DD	4001899359
	DD	3406994035
	DD	1318700802
	DD	1391178824
	DD	2680405347
	DD	1094332995
	DD	3708831422
	DD	2519020775
	DD	2637401604
	DD	2782357648
	DD	1065843399
	DD	2188665991
	DD	3122695549
	DD	743074255
	DD	979835913
	DD	1269748001
	DD	2131686798
	DD	82364686
	DD	1950423802
	DD	1486148511
	DD	1959671827
	DD	2539496002
	DD	4263373596
	DD	164729372
	DD	3900847605
	DD	2972297022
	DD	3919343654
	DD	784024708
	DD	4231779897
	DD	329458745
	DD	3506727914
	DD	1649626749
	DD	3543720013
	DD	1568049417
	DD	4168592498
	DD	658917491
	DD	2718488532
	DD	3299253499
	DD	2792472730
	DD	3136098835
	DD	4042217701
	DD	1317834983
	DD	1142009769
	DD	2303539703
	DD	1289978165
	DD	1977230375
	DD	3789468107
	DD	2635669967
	DD	2284019538
	DD	312112110
	DD	2579956331
	DD	3954460750
	DD	3283968918
	DD	976372639
	DD	273071781
	DD	624224221
	DD	864945366
	DD	3613954205
	DD	2272970540
	DD	1952745279
	DD	546143563
	DD	1248448442
	DD	1729890733
	DD	2932941114
	DD	250973784
	DD	3905490559
	DD	1092287127
	DD	2496896884
	DD	3459781466
	DD	1570914932
	DD	501947569
	DD	3516013822
	DD	2184574254
	DD	698826472
	DD	2624595636
	DD	3141829865
	DD	1003895138
	DD	2737060348
	DD	74181213
	DD	1397652945
	DD	954223976
	DD	1988692435
	DD	2007790276
	DD	1179153400
	DD	148362426
	DD	2795305890
	DD	1908447953
	DD	3977384870
	DD	4015580553
	DD	2358306800
	DD	296724853
	DD	1295644484
	DD	3816895906
	DD	3659802444
	DD	3736193810
	DD	421646305
	DD	593449707
	DD	2591288968
	DD	3338824517
	DD	3024637593
	DD	3177420325
	DD	843292611
	DD	1186899415
	DD	887610640
	DD	2382681739
	DD	1754307891
	DD	2059873354
	DD	1686585223
	DD	2373798830
	DD	1775221280
	DD	470396183
	DD	3508615783
	DD	4119746708
	DD	3373170446
	DD	452630365
	DD	3550442561
	DD	940792367
	DD	2722264270
	DD	3944526121
	DD	2451373597
	DD	905260731
	DD	2805917826
	DD	1881584735
	DD	1149561244
	DD	3594084947
	DD	607779899
	DD	1810521462
	DD	1316868356
	DD	3763169470
	DD	2299122488
	DD	2893202598
	DD	1215559799
	DD	3621042925
	DD	2633736712
	DD	3231371645
	DD	303277681
	DD	1491437901
	DD	2431119599
	DD	2947118554
	DD	972506129
	DD	2167775995
	DD	606555363
	DD	2982875802
	DD	567271902
	DD	1599269812
	DD	1945012259
	DD	40584695
	DD	1213110727
	DD	1670784308
	DD	1134543805
	DD	3198539624
	DD	3890024518
	DD	81169391
	DD	2426221454
	DD	3341568617
	DD	2269087610
	DD	2102111953
	DD	3485081741
	DD	162338783
	DD	557475612
	DD	2388169939
	DD	243207925
	DD	4204223906
	DD	2675196186
	DD	324677567
	DD	1114951224
	DD	481372583
	DD	486415851
	DD	4113480516
	DD	1055425077
	DD	649355134
	DD	2229902448
	DD	962745166
	DD	972831702
	DD	3931993737
	DD	2110850155
	DD	1298710268
	DD	164837600
	DD	1925490332
	DD	1945663404
	DD	3569020178
	DD	4221700311
	DD	2597420537
	DD	329675200
	DD	3850980665
	DD	3891326808
	DD	2843073060
	DD	4148433327
	DD	899873778
	DD	659350401
	DD	3406994035
	DD	3487686321
	DD	1391178824
	DD	4001899359
	DD	1799747556
	DD	1318700802
	DD	2519020775
	DD	2680405347
	DD	2782357648
	DD	3708831422
	DD	3599495112
	DD	2637401604
	DD	743074255
	DD	1065843399
	DD	1269748001
	DD	3122695549
	DD	2904022928
	DD	979835913
	DD	1486148511
	DD	2131686798
	DD	2539496002
	DD	1950423802
	DD	1513078560
	DD	1959671827
	DD	2972297022
	DD	4263373596
	DD	784024708
	DD	3900847605
	DD	3026157121
	DD	3919343654
	DD	1649626749
	DD	4231779897
	DD	1568049417
	DD	3506727914
	DD	1757346946
	DD	3543720013
	DD	3299253499
	DD	4168592498
	DD	3136098835
	DD	2718488532
	DD	3514693892
	DD	2792472730
	DD	2303539703
	DD	4042217701
	DD	1977230375
	DD	1142009769
	DD	2734420489
	DD	1289978165
	DD	312112110
	DD	3789468107
	DD	3954460750
	DD	2284019538
	DD	1173873683
	DD	2579956331
	DD	624224221
	DD	3283968918
	DD	3613954205
	DD	273071781
	DD	2347747367
	DD	864945366
	DD	1248448442
	DD	2272970540
	DD	2932941114
	DD	546143563
	DD	400527438
	DD	1729890733
	DD	2496896884
	DD	250973784
	DD	1570914932
	DD	1092287127
	DD	801054876
	DD	3459781466
	DD	698826472
	DD	501947569
	DD	3141829865
	DD	2184574254
	DD	1602109753
	DD	2624595636
	DD	1397652945
	DD	1003895138
	DD	1988692435
	DD	74181213
	DD	3204219507
	DD	954223976
	DD	2795305890
	DD	2007790276
	DD	3977384870
	DD	148362426
	DD	2113471718
	DD	1908447953
	DD	1295644484
	DD	4015580553
	DD	3659802444
	DD	296724853
	DD	4226943436
	DD	3816895906
	DD	2591288968
	DD	3736193810
	DD	3024637593
	DD	593449707
	DD	4158919577
	DD	3338824517
	DD	887610640
	DD	3177420325
	DD	1754307891
	DD	1186899415
	DD	4022871858
	DD	2382681739
	DD	1775221280
	DD	2059873354
	DD	3508615783
	DD	2373798830
	DD	3750776420
	DD	470396183
	DD	3550442561
	DD	4119746708
	DD	2722264270
	DD	452630365
	DD	3206585544
	DD	940792367
	DD	2805917826
	DD	3944526121
	DD	1149561244
	DD	905260731
	DD	2118203793
	DD	1881584735
	DD	1316868356
	DD	3594084947
	DD	2299122488
	DD	1810521462
	DD	4236407587
	DD	3763169470
	DD	2633736712
	DD	2893202598
	DD	303277681
	DD	3621042925
	DD	4177847879
	DD	3231371645
	DD	972506129
	DD	1491437901
	DD	606555363
	DD	2947118554
	DD	4060728462
	DD	2167775995
	DD	1945012259
	DD	2982875802
	DD	1213110727
	DD	1599269812
	DD	3826489629
	DD	40584695
	DD	3890024518
	DD	1670784308
	DD	2426221454
	DD	3198539624
	DD	3358011962
	DD	81169391
	DD	3485081741
	DD	3341568617
	DD	557475612
	DD	2102111953
	DD	2421056629
	DD	162338783
	DD	2675196186
	DD	2388169939
	DD	1114951224
	DD	4204223906
	DD	547145963
	DD	324677567
	DD	1055425077
	DD	481372583
	DD	2229902448
	DD	4113480516
	DD	1094291926
	DD	649355134
	DD	2110850155
	DD	962745166
	DD	164837600
	DD	3931993737
	DD	2188583852
	DD	1298710268
	DD	4221700311
	DD	1925490332
	DD	329675200
	DD	3569020178
	DD	82200408
	DD	2597420537
	DD	4148433327
	DD	3850980665
	DD	659350401
	DD	2843073060
	DD	164400817
	DD	899873778
	DD	4001899359
	DD	3406994035
	DD	1318700802
	DD	1391178824
	DD	328801635
	DD	1799747556
	DD	3708831422
	DD	2519020775
	DD	2637401604
	DD	2782357648
	DD	657603271
	DD	3599495112
	DD	3122695549
	DD	743074255
	DD	979835913
	DD	1269748001
	DD	1315206542
	DD	2904022928
	DD	1950423802
	DD	1486148511
	DD	1959671827
	DD	2539496002
	DD	2630413084
	DD	1513078560
	DD	3900847605
	DD	2972297022
	DD	3919343654
	DD	784024708
	DD	965858873
	DD	3026157121
	DD	3506727914
	DD	1649626749
	DD	3543720013
	DD	1568049417
	DD	1931717747
	DD	1757346946
	DD	2718488532
	DD	3299253499
	DD	2792472730
	DD	3136098835
	DD	3863435494
	DD	3514693892
	DD	1142009769
	DD	2303539703
	DD	1289978165
	DD	1977230375
	DD	3431903692
	DD	2734420489
	DD	2284019538
	DD	312112110
	DD	2579956331
	DD	3954460750
	DD	2568840088
	DD	1173873683
	DD	273071781
	DD	624224221
	DD	864945366
	DD	3613954205
	DD	842712880
	DD	2347747367
	DD	546143563
	DD	1248448442
	DD	1729890733
	DD	2932941114
	DD	1685425760
	DD	400527438
	DD	1092287127
	DD	2496896884
	DD	3459781466
	DD	1570914932
	DD	3370851521
	DD	801054876
	DD	2184574254
	DD	698826472
	DD	2624595636
	DD	3141829865
	DD	2446735747
	DD	1602109753
	DD	74181213
	DD	1397652945
	DD	954223976
	DD	1988692435
	DD	598504198
	DD	3204219507
	DD	148362426
	DD	2795305890
	DD	1908447953
	DD	3977384870
	DD	1197008397
	DD	2113471718
	DD	296724853
	DD	1295644484
	DD	3816895906
	DD	3659802444
	DD	2394016794
	DD	4226943436
	DD	593449707
	DD	2591288968
	DD	3338824517
	DD	3024637593
	DD	493066293
	DD	4158919577
	DD	1186899415
	DD	887610640
	DD	2382681739
	DD	1754307891
	DD	986132586
	DD	4022871858
	DD	2373798830
	DD	1775221280
	DD	470396183
	DD	3508615783
	DD	1972265172
	DD	3750776420
	DD	452630365
	DD	3550442561
	DD	940792367
	DD	2722264270
	DD	3944530345
	DD	3206585544
	DD	905260731
	DD	2805917826
	DD	1881584735
	DD	1149561244
	DD	3594093395
	DD	2118203793
	DD	1810521462
	DD	1316868356
	DD	3763169470
	DD	2299122488
	DD	2893219494
	DD	4236407587
	DD	3621042925
	DD	2633736712
	DD	3231371645
	DD	303277681
	DD	1491471692
	DD	4177847879
	DD	2947118554
	DD	972506129
	DD	2167775995
	DD	606555363
	DD	2982943385
	DD	4060728462
	DD	1599269812
	DD	1945012259
	DD	40584695
	DD	1213110727
	DD	1670919475
	DD	3826489629
	DD	3198539624
	DD	3890024518
	DD	81169391
	DD	2426221454
	DD	3341838951
	DD	3358011962
	DD	2102111953
	DD	3485081741
	DD	162338783
	DD	557475612
	DD	2388710606
	DD	2421056629
	DD	4204223906
	DD	2675196186
	DD	324677567
	DD	1114951224
	DD	482453916
	DD	547145963
	DD	4113480516
	DD	1055425077
	DD	649355134
	DD	2229902448
	DD	964907833
	DD	1094291926
	DD	3931993737
	DD	2110850155
	DD	1298710268
	DD	164837600
	DD	1929815667
	DD	2188583852
	DD	3569020178
	DD	4221700311
	DD	2597420537
	DD	329675200
	DD	3859631335
	DD	82200408
	DD	2843073060
	DD	4148433327
	DD	899873778
	DD	659350401
	DD	3424295375
	DD	164400817
	DD	1391178824
	DD	4001899359
	DD	1799747556
	DD	1318700802
	DD	2553623455
	DD	328801635
	DD	2782357648
	DD	3708831422
	DD	3599495112
	DD	2637401604
	DD	812279614
	DD	657603271
	DD	1269748001
	DD	3122695549
	DD	2904022928
	DD	979835913
	DD	1624559229
	DD	1315206542
	DD	2539496002
	DD	1950423802
	DD	1513078560
	DD	1959671827
	DD	3249118458
	DD	2630413084
	DD	784024708
	DD	3900847605
	DD	3026157121
	DD	3919343654
	DD	2203269620
	DD	965858873
	DD	1568049417
	DD	3506727914
	DD	1757346946
	DD	3543720013
	DD	111571945
	DD	1931717747
	DD	3136098835
	DD	2718488532
	DD	3514693892
	DD	2792472730
	DD	223143890
	DD	3863435494
	DD	1977230375
	DD	1142009769
	DD	2734420489
	DD	1289978165
	DD	446287780
	DD	3431903692
	DD	3954460750
	DD	2284019538
	DD	1173873683
	DD	2579956331
	DD	892575561
	DD	2568840088
	DD	3613954205
	DD	273071781
	DD	2347747367
	DD	864945366
	DD	1785151123
	DD	842712880
	DD	2932941114
	DD	546143563
	DD	400527438
	DD	1729890733
	DD	3570302247
	DD	1685425760
	DD	1570914932
	DD	1092287127
	DD	801054876
	DD	3459781466
	DD	2845637198
	DD	3370851521
	DD	3141829865
	DD	2184574254
	DD	1602109753
	DD	2624595636
	DD	1396307100
	DD	2446735747
	DD	1988692435
	DD	74181213
	DD	3204219507
	DD	954223976
	DD	2792614201
	DD	598504198
	DD	3977384870
	DD	148362426
	DD	2113471718
	DD	1908447953
	DD	1290261106
	DD	1197008397
	DD	3659802444
	DD	296724853
	DD	4226943436
	DD	3816895906
	DD	2580522212
	DD	2394016794
	DD	3024637593
	DD	593449707
	DD	4158919577
	DD	3338824517
	DD	866077128
	DD	493066293
	DD	1754307891
	DD	1186899415
	DD	4022871858
	DD	2382681739
	DD	1732154256
	DD	986132586
	DD	3508615783
	DD	2373798830
	DD	3750776420
	DD	470396183
	DD	3464308513
	DD	1972265172
	DD	2722264270
	DD	452630365
	DD	3206585544
	DD	940792367
	DD	2633649730
	DD	3944530345
	DD	1149561244
	DD	905260731
	DD	2118203793
	DD	1881584735
	DD	972332164
	DD	3594093395
	DD	2299122488
	DD	1810521462
	DD	4236407587
	DD	3763169470
	DD	1944664328
	DD	2893219494
	DD	303277681
	DD	3621042925
	DD	4177847879
	DD	3231371645
	DD	3889328657
	DD	1491471692
	DD	606555363
	DD	2947118554
	DD	4060728462
	DD	2167775995
	DD	3483690018
	DD	2982943385
	DD	1213110727
	DD	1599269812
	DD	3826489629
	DD	40584695
	DD	2672412741
	DD	1670919475
	DD	2426221454
	DD	3198539624
	DD	3358011962
	DD	81169391
	DD	1049858187
	DD	3341838951
	DD	557475612
	DD	2102111953
	DD	2421056629
	DD	162338783
	DD	2099716375
	DD	2388710606
	DD	1114951224
	DD	4204223906
	DD	547145963
	DD	324677567
	DD	4199432751
	DD	482453916
	DD	2229902448
	DD	4113480516
	DD	1094291926
	DD	649355134
	DD	4103898207
	DD	964907833
	DD	164837600
	DD	3931993737
	DD	2188583852
	DD	1298710268
	DD	3912829119
	DD	1929815667
	DD	329675200
	DD	3569020178
	DD	82200408
	DD	2597420537
	DD	3530690942
	DD	3859631335
	DD	659350401
	DD	2843073060
	DD	164400817
	DD	899873778
	DD	2766414588
	DD	3424295375
	DD	1318700802
	DD	1391178824
	DD	328801635
	DD	1799747556
	DD	1237861880
	DD	2553623455
	DD	2637401604
	DD	2782357648
	DD	657603271
	DD	3599495112
	DD	2475723761
	DD	812279614
	DD	979835913
	DD	1269748001
	DD	1315206542
	DD	2904022928
	DD	656480226
	DD	1624559229
	DD	1959671827
	DD	2539496002
	DD	2630413084
	DD	1513078560
	DD	1312960453
	DD	3249118458
	DD	3919343654
	DD	784024708
	DD	965858873
	DD	3026157121
	DD	2625920907
	DD	2203269620
	DD	3543720013
	DD	1568049417
	DD	1931717747
	DD	1757346946
	DD	956874519
	DD	111571945
	DD	2792472730
	DD	3136098835
	DD	3863435494
	DD	3514693892
	DD	1913749038
	DD	223143890
	DD	1289978165
	DD	1977230375
	DD	3431903692
	DD	2734420489
	DD	3827498077
	DD	446287780
	DD	2579956331
	DD	3954460750
	DD	2568840088
	DD	1173873683
	DD	3360028859
	DD	892575561
	DD	864945366
	DD	3613954205
	DD	842712880
	DD	2347747367
	DD	2425090423
	DD	1785151123
	DD	1729890733
	DD	2932941114
	DD	1685425760
	DD	400527438
	DD	555213551
	DD	3570302247
	DD	3459781466
	DD	1570914932
	DD	3370851521
	DD	801054876
	DD	1110427102
	DD	2845637198
	DD	2624595636
	DD	3141829865
	DD	2446735747
	DD	1602109753
	DD	2220854205
	DD	1396307100
	DD	954223976
	DD	1988692435
	DD	598504198
	DD	3204219507
	DD	146741115
	DD	2792614201
	DD	1908447953
	DD	3977384870
	DD	1197008397
	DD	2113471718
	DD	293482231
	DD	1290261106
	DD	3816895906
	DD	3659802444
	DD	2394016794
	DD	4226943436
	DD	586964463
	DD	2580522212
	DD	3338824517
	DD	3024637593
	DD	493066293
	DD	4158919577
	DD	1173928927
	DD	866077128
	DD	2382681739
	DD	1754307891
	DD	986132586
	DD	4022871858
	DD	2347857855
	DD	1732154256
	DD	470396183
	DD	3508615783
	DD	1972265172
	DD	3750776420
	DD	400748414
	DD	3464308513
	DD	940792367
	DD	2722264270
	DD	3944530345
	DD	3206585544
	DD	801496828
	DD	2633649730
	DD	1881584735
	DD	1149561244
	DD	3594093395
	DD	2118203793
	DD	1602993657
	DD	972332164
	DD	3763169470
	DD	2299122488
	DD	2893219494
	DD	4236407587
	DD	3205987314
	DD	1944664328
	DD	3231371645
	DD	303277681
	DD	1491471692
	DD	4177847879
	DD	2117007332
	DD	3889328657
	DD	2167775995
	DD	606555363
	DD	2982943385
	DD	4060728462
	DD	4234014665
	DD	3483690018
	DD	40584695
	DD	1213110727
	DD	1670919475
	DD	3826489629
	DD	4173062034
	DD	2672412741
	DD	81169391
	DD	2426221454
	DD	3341838951
	DD	3358011962
	DD	4051156773
	DD	1049858187
	DD	162338783
	DD	557475612
	DD	2388710606
	DD	2421056629
	DD	3807346250
	DD	2099716375
	DD	324677567
	DD	1114951224
	DD	482453916
	DD	547145963
	DD	3319725204
	DD	4199432751
	DD	649355134
	DD	2229902448
	DD	964907833
	DD	1094291926
	DD	2344483112
	DD	4103898207
	DD	1298710268
	DD	164837600
	DD	1929815667
	DD	2188583852
	DD	393998928
	DD	3912829119
	DD	2597420537
	DD	329675200
	DD	3859631335
	DD	82200408
	DD	787997856
	DD	3530690942
	DD	899873778
	DD	659350401
	DD	3424295375
	DD	164400817
	DD	1575995713
	DD	2766414588
	DD	1799747556
	DD	1318700802
	DD	2553623455
	DD	328801635
	DD	3151991427
	DD	1237861880
	DD	3599495112
	DD	2637401604
	DD	812279614
	DD	657603271
	DD	2009015559
	DD	2475723761
	DD	2904022928
	DD	979835913
	DD	1624559229
	DD	1315206542
	DD	4018031118
	DD	656480226
	DD	1513078560
	DD	1959671827
	DD	3249118458
	DD	2630413084
	DD	3741094941
	DD	1312960453
	DD	3026157121
	DD	3919343654
	DD	2203269620
	DD	965858873
	DD	3187222587
	DD	2625920907
	DD	1757346946
	DD	3543720013
	DD	111571945
	DD	1931717747
	DD	2079477878
	DD	956874519
	DD	3514693892
	DD	2792472730
	DD	223143890
	DD	3863435494
	DD	4158955756
	DD	1913749038
	DD	2734420489
	DD	1289978165
	DD	446287780
	DD	3431903692
	DD	4022944216
	DD	3827498077
	DD	1173873683
	DD	2579956331
	DD	892575561
	DD	2568840088
	DD	3750921137
	DD	3360028859
	DD	2347747367
	DD	864945366
	DD	1785151123
	DD	842712880
	DD	3206874979
	DD	2425090423
	DD	400527438
	DD	1729890733
	DD	3570302247
	DD	1685425760
	DD	2118782663
	DD	555213551
	DD	801054876
	DD	3459781466
	DD	2845637198
	DD	3370851521
	DD	4237565327
	DD	1110427102
	DD	1602109753
	DD	2624595636
	DD	1396307100
	DD	2446735747
	DD	4180163359
	DD	2220854205
	DD	3204219507
	DD	954223976
	DD	2792614201
	DD	598504198
	DD	4065359423
	DD	146741115
	DD	2113471718
	DD	1908447953
	DD	1290261106
	DD	1197008397
	DD	3835751551
	DD	293482231
	DD	4226943436
	DD	3816895906
	DD	2580522212
	DD	2394016794
	DD	3376535807
	DD	586964463
	DD	4158919577
	DD	3338824517
	DD	866077128
	DD	493066293
	DD	2458104319
	DD	1173928927
	DD	4022871858
	DD	2382681739
	DD	1732154256
	DD	986132586
	DD	621241343
	DD	2347857855
	DD	3750776420
	DD	470396183
	DD	3464308513
	DD	1972265172
	DD	1242482686
	DD	400748414
	DD	3206585544
	DD	940792367
	DD	2633649730
	DD	3944530345
	DD	2484965372
	DD	801496828
	DD	2118203793
	DD	1881584735
	DD	972332164
	DD	3594093395
	DD	674963448
	DD	1602993657
	DD	4236407587
	DD	3763169470
	DD	1944664328
	DD	2893219494
	DD	1349926897
	DD	3205987314
	DD	4177847879
	DD	3231371645
	DD	3889328657
	DD	1491471692
	DD	2699853794
	DD	2117007332
	DD	4060728462
	DD	2167775995
	DD	3483690018
	DD	2982943385
	DD	1104740292
	DD	4234014665
	DD	3826489629
	DD	40584695
	DD	2672412741
	DD	1670919475
	DD	2209480585
	DD	4173062034
	DD	3358011962
	DD	81169391
	DD	1049858187
	DD	3341838951
	DD	123993874
	DD	4051156773
	DD	2421056629
	DD	162338783
	DD	2099716375
	DD	2388710606
	DD	247987749
	DD	3807346250
	DD	547145963
	DD	324677567
	DD	4199432751
	DD	482453916
	DD	495975499
	DD	3319725204
	DD	1094291926
	DD	649355134
	DD	4103898207
	DD	964907833
	DD	991950999
	DD	2344483112
	DD	2188583852
	DD	1298710268
	DD	3912829119
	DD	1929815667
	DD	1983901999
	DD	393998928
	DD	82200408
	DD	2597420537
	DD	3530690942
	DD	3859631335
	DD	3967803999
	DD	787997856
	DD	164400817
	DD	899873778
	DD	2766414588
	DD	3424295375
	DD	3640640703
	DD	1575995713
	DD	328801635
	DD	1799747556
	DD	1237861880
	DD	2553623455
	DD	2986314111
	DD	3151991427
	DD	657603271
	DD	3599495112
	DD	2475723761
	DD	812279614
	DD	1677660927
	DD	2009015559
	DD	1315206542
	DD	2904022928
	DD	656480226
	DD	1624559229
	DD	3355321855
	DD	4018031118
	DD	2630413084
	DD	1513078560
	DD	1312960453
	DD	3249118458
	DD	2415676415
	DD	3741094941
	DD	965858873
	DD	3026157121
	DD	2625920907
	DD	2203269620
	DD	536385535
	DD	3187222587
	DD	1931717747
	DD	1757346946
	DD	956874519
	DD	111571945
	DD	1072771071
	DD	2079477878
	DD	3863435494
	DD	3514693892
	DD	1913749038
	DD	223143890
	DD	2145542143
	DD	4158955756
	DD	3431903692
	DD	2734420489
	DD	3827498077
	DD	446287780
	DD	4291084286
	DD	4022944216
	DD	2568840088
	DD	1173873683
	DD	3360028859
	DD	892575561
	DD	4287201277
	DD	3750921137
	DD	842712880
	DD	2347747367
	DD	2425090423
	DD	1785151123
	DD	4279435259
	DD	3206874979
	DD	1685425760
	DD	400527438
	DD	555213551
	DD	3570302247
	DD	4263903223
	DD	2118782663
	DD	3370851521
	DD	801054876
	DD	1110427102
	DD	2845637198
	DD	4232839151
	DD	4237565327
	DD	2446735747
	DD	1602109753
	DD	2220854205
	DD	1396307100
	DD	4170711006
	DD	4180163359
	DD	598504198
	DD	3204219507
	DD	146741115
	DD	2792614201
	DD	4046454716
	DD	4065359423
	DD	1197008397
	DD	2113471718
	DD	293482231
	DD	1290261106
	DD	3797942136
	DD	3835751551
	DD	2394016794
	DD	4226943436
	DD	586964463
	DD	2580522212
	DD	3300916976
	DD	3376535807
	DD	493066293
	DD	4158919577
	DD	1173928927
	DD	866077128
	DD	2306866656
	DD	2458104319
	DD	986132586
	DD	4022871858
	DD	2347857855
	DD	1732154256
	DD	318766016
	DD	621241343
	DD	1972265172
	DD	3750776420
	DD	400748414
	DD	3464308513
	DD	637532033
	DD	1242482686
	DD	3944530345
	DD	3206585544
	DD	801496828
	DD	2633649730
	DD	1275064066
	DD	2484965372
	DD	3594093395
	DD	2118203793
	DD	1602993657
	DD	972332164
	DD	2550128133
	DD	674963448
	DD	2893219494
	DD	4236407587
	DD	3205987314
	DD	1944664328
	DD	805288971
	DD	1349926897
	DD	1491471692
	DD	4177847879
	DD	2117007332
	DD	3889328657
	DD	1610577942
	DD	2699853794
	DD	2982943385
	DD	4060728462
	DD	4234014665
	DD	3483690018
	DD	3221155884
	DD	1104740292
	DD	1670919475
	DD	3826489629
	DD	4173062034
	DD	2672412741
	DD	2147344473
	DD	2209480585
	DD	3341838951
	DD	3358011962
	DD	4051156773
	DD	1049858187
	DD	4294688947
	DD	123993874
	DD	2388710606
	DD	2421056629
	DD	3807346250
	DD	2099716375
	DD	4294410598
	DD	247987749
	DD	482453916
	DD	547145963
	DD	3319725204
	DD	4199432751
	DD	4293853900
	DD	495975499
	DD	964907833
	DD	1094291926
	DD	2344483112
	DD	4103898207
	DD	4292740504
	DD	991950999
	DD	1929815667
	DD	2188583852
	DD	393998928
	DD	3912829119
	DD	4290513712
	DD	1983901999
	DD	3859631335
	DD	82200408
	DD	787997856
	DD	3530690942
	DD	4286060128
	DD	3967803999
	DD	3424295375
	DD	164400817
	DD	1575995713
	DD	2766414588
	DD	4277152960
	DD	3640640703
	DD	2553623455
	DD	328801635
	DD	3151991427
	DD	1237861880
	DD	4259338624
	DD	2986314111
	DD	812279614
	DD	657603271
	DD	2009015559
	DD	2475723761
	DD	4223709953
	DD	1677660927
	DD	1624559229
	DD	1315206542
	DD	4018031118
	DD	656480226
	DD	4152452611
	DD	3355321855
	DD	3249118458
	DD	2630413084
	DD	3741094941
	DD	1312960453
	DD	4009937927
	DD	2415676415
	DD	2203269620
	DD	965858873
	DD	3187222587
	DD	2625920907
	DD	3724908559
	DD	536385535
	DD	111571945
	DD	1931717747
	DD	2079477878
	DD	956874519
	DD	3154849823
	DD	1072771071
	DD	223143890
	DD	3863435494
	DD	4158955756
	DD	1913749038
	DD	2014732351
	DD	2145542143
	DD	446287780
	DD	3431903692
	DD	4022944216
	DD	3827498077
	DD	4029464703
	DD	4291084286
	DD	892575561
	DD	2568840088
	DD	3750921137
	DD	3360028859
	DD	3763962110
	DD	4287201277
	DD	1785151123
	DD	842712880
	DD	3206874979
	DD	2425090423
	DD	3232956925
	DD	4279435259
	DD	3570302247
	DD	1685425760
	DD	2118782663
	DD	555213551
	DD	2170946555
	DD	4263903223
	DD	2845637198
	DD	3370851521
	DD	4237565327
	DD	1110427102
	DD	46925815
	DD	4232839151
	DD	1396307100
	DD	2446735747
	DD	4180163359
	DD	2220854205
	DD	93851631
	DD	4170711006
	DD	2792614201
	DD	598504198
	DD	4065359423
	DD	146741115
	DD	187703262
	DD	4046454716
	DD	1290261106
	DD	1197008397
	DD	3835751551
	DD	293482231
	DD	375406524
	DD	3797942136
	DD	2580522212
	DD	2394016794
	DD	3376535807
	DD	586964463
	DD	750813049
	DD	3300916976
	DD	866077128
	DD	493066293
	DD	2458104319
	DD	1173928927
	DD	1501626098
	DD	2306866656
	DD	1732154256
	DD	986132586
	DD	621241343
	DD	2347857855
	DD	3003252197
	DD	318766016
	DD	3464308513
	DD	1972265172
	DD	1242482686
	DD	400748414
	DD	1711537099
	DD	637532033
	DD	2633649730
	DD	3944530345
	DD	2484965372
	DD	801496828
	DD	3423074199
	DD	1275064066
	DD	972332164
	DD	3594093395
	DD	674963448
	DD	1602993657
	DD	2551181103
	DD	2550128133
	DD	1944664328
	DD	2893219494
	DD	1349926897
	DD	3205987314
	DD	807394910
	DD	805288971
	DD	3889328657
	DD	1491471692
	DD	2699853794
	DD	2117007332
	DD	1614789820
	DD	1610577942
	DD	3483690018
	DD	2982943385
	DD	1104740292
	DD	4234014665
	DD	3229579640
	DD	3221155884
	DD	2672412741
	DD	1670919475
	DD	2209480585
	DD	4173062034
	DD	2164191985
	DD	2147344473
	DD	1049858187
	DD	3341838951
	DD	123993874
	DD	4051156773
	DD	33416674
	DD	4294688947
	DD	2099716375
	DD	2388710606
	DD	247987749
	DD	3807346250
	DD	66833348
	DD	4294410598
	DD	4199432751
	DD	482453916
	DD	495975499
	DD	3319725204
	DD	133666696
	DD	4293853900
	DD	4103898207
	DD	964907833
	DD	991950999
	DD	2344483112
	DD	267333393
	DD	4292740504
	DD	3912829119
	DD	1929815667
	DD	1983901999
	DD	393998928
	DD	534666787
	DD	4290513712
	DD	3530690942
	DD	3859631335
	DD	3967803999
	DD	787997856
	DD	1069333574
	DD	4286060128
	DD	2766414588
	DD	3424295375
	DD	3640640703
	DD	1575995713
	DD	2138667148
	DD	4277152960
	DD	1237861880
	DD	2553623455
	DD	2986314111
	DD	3151991427
	DD	4277334296
	DD	4259338624
	DD	2475723761
	DD	812279614
	DD	1677660927
	DD	2009015559
	DD	4259701297
	DD	4223709953
	DD	656480226
	DD	1624559229
	DD	3355321855
	DD	4018031118
	DD	4224435298
	DD	4152452611
	DD	1312960453
	DD	3249118458
	DD	2415676415
	DD	3741094941
	DD	4153903301
	DD	4009937927
	DD	2625920907
	DD	2203269620
	DD	536385535
	DD	3187222587
	DD	4012839307
	DD	3724908559
	DD	956874519
	DD	111571945
	DD	1072771071
	DD	2079477878
	DD	3730711318
	DD	3154849823
	DD	1913749038
	DD	223143890
	DD	2145542143
	DD	4158955756
	DD	3166455341
	DD	2014732351
	DD	3827498077
	DD	446287780
	DD	4291084286
	DD	4022944216
	DD	2037943386
	DD	4029464703
	DD	3360028859
	DD	892575561
	DD	4287201277
	DD	3750921137
	DD	4075886773
	DD	3763962110
	DD	2425090423
	DD	1785151123
	DD	4279435259
	DD	3206874979
	DD	3856806251
	DD	3232956925
	DD	555213551
	DD	3570302247
	DD	4263903223
	DD	2118782663
	DD	3418645206
	DD	2170946555
	DD	1110427102
	DD	2845637198
	DD	4232839151
	DD	4237565327
	DD	2542323117
	DD	46925815
	DD	2220854205
	DD	1396307100
	DD	4170711006
	DD	4180163359
	DD	789678938
	DD	93851631
	DD	146741115
	DD	2792614201
	DD	4046454716
	DD	4065359423
	DD	1579357876
	DD	187703262
	DD	293482231
	DD	1290261106
	DD	3797942136
	DD	3835751551
	DD	3158715752
	DD	375406524
	DD	586964463
	DD	2580522212
	DD	3300916976
	DD	3376535807
	DD	2022464208
	DD	750813049
	DD	1173928927
	DD	866077128
	DD	2306866656
	DD	2458104319
	DD	4044928416
	DD	1501626098
	DD	2347857855
	DD	1732154256
	DD	318766016
	DD	621241343
	DD	3794889537
	DD	3003252197
	DD	400748414
	DD	3464308513
	DD	637532033
	DD	1242482686
	DD	3294811778
	DD	1711537099
	DD	801496828
	DD	2633649730
	DD	1275064066
	DD	2484965372
	DD	2294656261
	DD	3423074199
	DD	1602993657
	DD	972332164
	DD	2550128133
	DD	674963448
	DD	294345226
	DD	2551181103
	DD	3205987314
	DD	1944664328
	DD	805288971
	DD	1349926897
	DD	588690452
	DD	807394910
	DD	2117007332
	DD	3889328657
	DD	1610577942
	DD	2699853794
	DD	1177380905
	DD	1614789820
	DD	4234014665
	DD	3483690018
	DD	3221155884
	DD	1104740292
	DD	2354761811
	DD	3229579640
	DD	4173062034
	DD	2672412741
	DD	2147344473
	DD	2209480585
	DD	414556326
	DD	2164191985
	DD	4051156773
	DD	1049858187
	DD	4294688947
	DD	123993874
	DD	829112653
	DD	33416674
	DD	3807346250
	DD	2099716375
	DD	4294410598
	DD	247987749
	DD	1658225307
	DD	66833348
	DD	3319725204
	DD	4199432751
	DD	4293853900
	DD	495975499
	DD	3316450614
	DD	133666696
	DD	2344483112
	DD	4103898207
	DD	4292740504
	DD	991950999
	DD	2337933933
	DD	267333393
	DD	393998928
	DD	3912829119
	DD	4290513712
	DD	1983901999
	DD	380900570
	DD	534666787
	DD	787997856
	DD	3530690942
	DD	4286060128
	DD	3967803999
	DD	761801140
	DD	1069333574
	DD	1575995713
	DD	2766414588
	DD	4277152960
	DD	3640640703
	DD	1523602280
	DD	2138667148
	DD	3151991427
	DD	1237861880
	DD	4259338624
	DD	2986314111
	DD	3047204561
	DD	4277334296
	DD	2009015559
	DD	2475723761
	DD	4223709953
	DD	1677660927
	DD	1799441827
	DD	4259701297
	DD	4018031118
	DD	656480226
	DD	4152452611
	DD	3355321855
	DD	3598883655
	DD	4224435298
	DD	3741094941
	DD	1312960453
	DD	4009937927
	DD	2415676415
	DD	2902800015
	DD	4153903301
	DD	3187222587
	DD	2625920907
	DD	3724908559
	DD	536385535
	DD	1510632735
	DD	4012839307
	DD	2079477878
	DD	956874519
	DD	3154849823
	DD	1072771071
	DD	3021265470
	DD	3730711318
	DD	4158955756
	DD	1913749038
	DD	2014732351
	DD	2145542143
	DD	1747563645
	DD	3166455341
	DD	4022944216
	DD	3827498077
	DD	4029464703
	DD	4291084286
	DD	3495127291
	DD	2037943386
	DD	3750921137
	DD	3360028859
	DD	3763962110
	DD	4287201277
	DD	2695287286
	DD	4075886773
	DD	3206874979
	DD	2425090423
	DD	3232956925
	DD	4279435259
	DD	1095607277
	DD	3856806251
	DD	2118782663
	DD	555213551
	DD	2170946555
	DD	4263903223
	DD	2191214555
	DD	3418645206
	DD	4237565327
	DD	1110427102
	DD	46925815
	DD	4232839151
	DD	87461814
	DD	2542323117
	DD	4180163359
	DD	2220854205
	DD	93851631
	DD	4170711006
	DD	174923629
	DD	789678938
	DD	4065359423
	DD	146741115
	DD	187703262
	DD	4046454716
	DD	349847258
	DD	1579357876
	DD	3835751551
	DD	293482231
	DD	375406524
	DD	3797942136
	DD	699694516
	DD	3158715752
	DD	3376535807
	DD	586964463
	DD	750813049
	DD	3300916976
	DD	1399389033
	DD	2022464208
	DD	2458104319
	DD	1173928927
	DD	1501626098
	DD	2306866656
	DD	2798778067
	DD	4044928416
	DD	621241343
	DD	2347857855
	DD	3003252197
	DD	318766016
	DD	1302588838
	DD	3794889537
	DD	1242482686
	DD	400748414
	DD	1711537099
	DD	637532033
	DD	2605177677
	DD	3294811778
	DD	2484965372
	DD	801496828
	DD	3423074199
	DD	1275064066
	DD	915388059
	DD	2294656261
	DD	674963448
	DD	1602993657
	DD	2551181103
	DD	2550128133
	DD	1830776118
	DD	294345226
	DD	1349926897
	DD	3205987314
	DD	807394910
	DD	805288971
	DD	3661552236
	DD	588690452
	DD	2699853794
	DD	2117007332
	DD	1614789820
	DD	1610577942
	DD	3028137177
	DD	1177380905
	DD	1104740292
	DD	4234014665
	DD	3229579640
	DD	3221155884
	DD	1761307059
	DD	2354761811
	DD	2209480585
	DD	4173062034
	DD	2164191985
	DD	2147344473
	DD	3522614119
	DD	414556326
	DD	123993874
	DD	4051156773
	DD	33416674
	DD	4294688947
	DD	2750260943
	DD	829112653
	DD	247987749
	DD	3807346250
	DD	66833348
	DD	4294410598
	DD	1205554591
	DD	1658225307
	DD	495975499
	DD	3319725204
	DD	133666696
	DD	4293853900
	DD	2411109183
	DD	3316450614
	DD	991950999
	DD	2344483112
	DD	267333393
	DD	4292740504
	DD	527251070
	DD	2337933933
	DD	1983901999
	DD	393998928
	DD	534666787
	DD	4290513712
	DD	1054502141
	DD	380900570
	DD	3967803999
	DD	787997856
	DD	1069333574
	DD	4286060128
	DD	2109004283
	DD	761801140
	DD	3640640703
	DD	1575995713
	DD	2138667148
	DD	4277152960
	DD	4218008566
	DD	1523602280
	DD	2986314111
	DD	3151991427
	DD	4277334296
	DD	4259338624
	DD	4141049836
	DD	3047204561
	DD	1677660927
	DD	2009015559
	DD	4259701297
	DD	4223709953
	DD	3987132377
	DD	1799441827
	DD	3355321855
	DD	4018031118
	DD	4224435298
	DD	4152452611
	DD	3679297459
	DD	3598883655
	DD	2415676415
	DD	3741094941
	DD	4153903301
	DD	4009937927
	DD	3063627623
	DD	2902800015
	DD	536385535
	DD	3187222587
	DD	4012839307
	DD	3724908559
	DD	1832287951
	DD	1510632735
	DD	1072771071
	DD	2079477878
	DD	3730711318
	DD	3154849823
	DD	3664575902
	DD	3021265470
	DD	2145542143
	DD	4158955756
	DD	3166455341
	DD	2014732351
	DD	3034184508
	DD	1747563645
	DD	4291084286
	DD	4022944216
	DD	2037943386
	DD	4029464703
	DD	1773401721
	DD	3495127291
	DD	4287201277
	DD	3750921137
	DD	4075886773
	DD	3763962110
	DD	3546803442
	DD	2695287286
	DD	4279435259
	DD	3206874979
	DD	3856806251
	DD	3232956925
	DD	2798639588
	DD	1095607277
	DD	4263903223
	DD	2118782663
	DD	3418645206
	DD	2170946555
	DD	1302311881
	DD	2191214555
	DD	4232839151
	DD	4237565327
	DD	2542323117
	DD	46925815
	DD	2604623763
	DD	87461814
	DD	4170711006
	DD	4180163359
	DD	789678938
	DD	93851631
	DD	914280231
	DD	174923629
	DD	4046454716
	DD	4065359423
	DD	1579357876
	DD	187703262
	DD	1828560463
	DD	349847258
	DD	3797942136
	DD	3835751551
	DD	3158715752
	DD	375406524
	DD	3657120927
	DD	699694516
	DD	3300916976
	DD	3376535807
	DD	2022464208
	DD	750813049
	DD	3019274558
	DD	1399389033
	DD	2306866656
	DD	2458104319
	DD	4044928416
	DD	1501626098
	DD	1743581820
	DD	2798778067
	DD	318766016
	DD	621241343
	DD	3794889537
	DD	3003252197
	DD	3487163641
	DD	1302588838
	DD	637532033
	DD	1242482686
	DD	3294811778
	DD	1711537099
	DD	2679359986
	DD	2605177677
	DD	1275064066
	DD	2484965372
	DD	2294656261
	DD	3423074199
	DD	1063752677
	DD	915388059
	DD	2550128133
	DD	674963448
	DD	294345226
	DD	2551181103
	DD	2127505355
	DD	1830776118
	DD	805288971
	DD	1349926897
	DD	588690452
	DD	807394910
	DD	4255010710
	DD	3661552236
	DD	1610577942
	DD	2699853794
	DD	1177380905
	DD	1614789820
	DD	4215054124
	DD	3028137177
	DD	3221155884
	DD	1104740292
	DD	2354761811
	DD	3229579640
	DD	4135140952
	DD	1761307059
	DD	2147344473
	DD	2209480585
	DD	414556326
	DD	2164191985
	DD	3975314608
	DD	3522614119
	DD	4294688947
	DD	123993874
	DD	829112653
	DD	33416674
	DD	3655661921
	DD	2750260943
	DD	4294410598
	DD	247987749
	DD	1658225307
	DD	66833348
	DD	3016356546
	DD	1205554591
	DD	4293853900
	DD	495975499
	DD	3316450614
	DD	133666696
	DD	1737745796
	DD	2411109183
	DD	4292740504
	DD	991950999
	DD	2337933933
	DD	267333393
	DD	3475491593
	DD	527251070
	DD	4290513712
	DD	1983901999
	DD	380900570
	DD	534666787
	DD	2656015891
	DD	1054502141
	DD	4286060128
	DD	3967803999
	DD	761801140
	DD	1069333574
	DD	1017064486
	DD	2109004283
	DD	4277152960
	DD	3640640703
	DD	1523602280
	DD	2138667148
	DD	2034128973
	DD	4218008566
	DD	4259338624
	DD	2986314111
	DD	3047204561
	DD	4277334296
	DD	4068257947
	DD	4141049836
	DD	4223709953
	DD	1677660927
	DD	1799441827
	DD	4259701297
	DD	3841548598
	DD	3987132377
	DD	4152452611
	DD	3355321855
	DD	3598883655
	DD	4224435298
	DD	3388129901
	DD	3679297459
	DD	4009937927
	DD	2415676415
	DD	2902800015
	DD	4153903301
	DD	2481292507
	DD	3063627623
	DD	3724908559
	DD	536385535
	DD	1510632735
	DD	4012839307
	DD	667617719
	DD	1832287951
	DD	3154849823
	DD	1072771071
	DD	3021265470
	DD	3730711318
	DD	1335235438
	DD	3664575902
	DD	2014732351
	DD	2145542143
	DD	1747563645
	DD	3166455341
	DD	2670470877
	DD	3034184508
	DD	4029464703
	DD	4291084286
	DD	3495127291
	DD	2037943386
	DD	1045974458
	DD	1773401721
	DD	3763962110
	DD	4287201277
	DD	2695287286
	DD	4075886773
	DD	2091948916
	DD	3546803442
	DD	3232956925
	DD	4279435259
	DD	1095607277
	DD	3856806251
	DD	4183897833
	DD	2798639588
	DD	2170946555
	DD	4263903223
	DD	2191214555
	DD	3418645206
	DD	4072828371
	DD	1302311881
	DD	46925815
	DD	4232839151
	DD	87461814
	DD	2542323117
	DD	3850689447
	DD	2604623763
	DD	93851631
	DD	4170711006
	DD	174923629
	DD	789678938
	DD	3406411599
	DD	914280231
	DD	187703262
	DD	4046454716
	DD	349847258
	DD	1579357876
	DD	2517855902
	DD	1828560463
	DD	375406524
	DD	3797942136
	DD	699694516
	DD	3158715752
	DD	740744509
	DD	3657120927
	DD	750813049
	DD	3300916976
	DD	1399389033
	DD	2022464208
	DD	1481489018
	DD	3019274558
	DD	1501626098
	DD	2306866656
	DD	2798778067
	DD	4044928416
	DD	2962978036
	DD	1743581820
	DD	3003252197
	DD	318766016
	DD	1302588838
	DD	3794889537
	DD	1630988776
	DD	3487163641
	DD	1711537099
	DD	637532033
	DD	2605177677
	DD	3294811778
	DD	3261977553
	DD	2679359986
	DD	3423074199
	DD	1275064066
	DD	915388059
	DD	2294656261
	DD	2228987811
	DD	1063752677
	DD	2551181103
	DD	2550128133
	DD	1830776118
	DD	294345226
	DD	163008326
	DD	2127505355
	DD	807394910
	DD	805288971
	DD	3661552236
	DD	588690452
	DD	326016652
	DD	4255010710
	DD	1614789820
	DD	1610577942
	DD	3028137177
	DD	1177380905
	DD	652033304
	DD	4215054124
	DD	3229579640
	DD	3221155884
	DD	1761307059
	DD	2354761811
	DD	1304066609
	DD	4135140952
	DD	2164191985
	DD	2147344473
	DD	3522614119
	DD	414556326
	DD	2608133219
	DD	3975314608
	DD	33416674
	DD	4294688947
	DD	2750260943
	DD	829112653
	DD	921299143
	DD	3655661921
	DD	66833348
	DD	4294410598
	DD	1205554591
	DD	1658225307
	DD	1842598287
	DD	3016356546
	DD	133666696
	DD	4293853900
	DD	2411109183
	DD	3316450614
	DD	3685196575
	DD	1737745796
	DD	267333393
	DD	4292740504
	DD	527251070
	DD	2337933933
	DD	3075425855
	DD	3475491593
	DD	534666787
	DD	4290513712
	DD	1054502141
	DD	380900570
	DD	1855884414
	DD	2656015891
	DD	1069333574
	DD	4286060128
	DD	2109004283
	DD	761801140
	DD	3711768829
	DD	1017064486
	DD	2138667148
	DD	4277152960
	DD	4218008566
	DD	1523602280
	DD	3128570363
	DD	2034128973
	DD	4277334296
	DD	4259338624
	DD	4141049836
	DD	3047204561
	DD	1962173430
	DD	4068257947
	DD	4259701297
	DD	4223709953
	DD	3987132377
	DD	1799441827
	DD	3924346860
	DD	3841548598
	DD	4224435298
	DD	4152452611
	DD	3679297459
	DD	3598883655
	DD	3553726425
	DD	3388129901
	DD	4153903301
	DD	4009937927
	DD	3063627623
	DD	2902800015
	DD	2812485555
	DD	2481292507
	DD	4012839307
	DD	3724908559
	DD	1832287951
	DD	1510632735
	DD	1330003814
	DD	667617719
	DD	3730711318
	DD	3154849823
	DD	3664575902
	DD	3021265470
	DD	2660007629
	DD	1335235438
	DD	3166455341
	DD	2014732351
	DD	3034184508
	DD	1747563645
	DD	1025047962
	DD	2670470877
	DD	2037943386
	DD	4029464703
	DD	1773401721
	DD	3495127291
	DD	2050095924
	DD	1045974458
	DD	4075886773
	DD	3763962110
	DD	3546803442
	DD	2695287286
	DD	4100191849
	DD	2091948916
	DD	3856806251
	DD	3232956925
	DD	2798639588
	DD	1095607277
	DD	3905416403
	DD	4183897833
	DD	3418645206
	DD	2170946555
	DD	1302311881
	DD	2191214555
	DD	3515865511
	DD	4072828371
	DD	2542323117
	DD	46925815
	DD	2604623763
	DD	87461814
	DD	2736763727
	DD	3850689447
	DD	789678938
	DD	93851631
	DD	914280231
	DD	174923629
	DD	1178560158
	DD	3406411599
	DD	1579357876
	DD	187703262
	DD	1828560463
	DD	349847258
	DD	2357120316
	DD	2517855902
	DD	3158715752
	DD	375406524
	DD	3657120927
	DD	699694516
	DD	419273337
	DD	740744509
	DD	2022464208
	DD	750813049
	DD	3019274558
	DD	1399389033
	DD	838546674
	DD	1481489018
	DD	4044928416
	DD	1501626098
	DD	1743581820
	DD	2798778067
	DD	1677093349
	DD	2962978036
	DD	3794889537
	DD	3003252197
	DD	3487163641
	DD	1302588838
	DD	3354186699
	DD	1630988776
	DD	3294811778
	DD	1711537099
	DD	2679359986
	DD	2605177677
	DD	2413406103
	DD	3261977553
	DD	2294656261
	DD	3423074199
	DD	1063752677
	DD	915388059
	DD	531844911
	DD	2228987811
	DD	294345226
	DD	2551181103
	DD	2127505355
	DD	1830776118
	DD	1063689823
	DD	163008326
	DD	588690452
	DD	807394910
	DD	4255010710
	DD	3661552236
	DD	2127379647
	DD	326016652
	DD	1177380905
	DD	1614789820
	DD	4215054124
	DD	3028137177
	DD	4254759295
	DD	652033304
	DD	2354761811
	DD	3229579640
	DD	4135140952
	DD	1761307059
	DD	4214551295
	DD	1304066609
	DD	414556326
	DD	2164191985
	DD	3975314608
	DD	3522614119
	DD	4134135294
	DD	2608133219
	DD	829112653
	DD	33416674
	DD	3655661921
	DD	2750260943
	DD	3973303293
	DD	921299143
	DD	1658225307
	DD	66833348
	DD	3016356546
	DD	1205554591
	DD	3651639290
	DD	1842598287
	DD	3316450614
	DD	133666696
	DD	1737745796
	DD	2411109183
	DD	3008311285
	DD	3685196575
	DD	2337933933
	DD	267333393
	DD	3475491593
	DD	527251070
	DD	1721655274
	DD	3075425855
	DD	380900570
	DD	534666787
	DD	2656015891
	DD	1054502141
	DD	3443310548
	DD	1855884414
	DD	761801140
	DD	1069333574
	DD	1017064486
	DD	2109004283
	DD	2591653800
	DD	3711768829
	DD	1523602280
	DD	2138667148
	DD	2034128973
	DD	4218008566
	DD	888340305
	DD	3128570363
	DD	3047204561
	DD	4277334296
	DD	4068257947
	DD	4141049836
	DD	1776680610
	DD	1962173430
	DD	1799441827
	DD	4259701297
	DD	3841548598
	DD	3987132377
	DD	3553361221
	DD	3924346860
	DD	3598883655
	DD	4224435298
	DD	3388129901
	DD	3679297459
	DD	2811755147
	DD	3553726425
	DD	2902800015
	DD	4153903301
	DD	2481292507
	DD	3063627623
	DD	1328542998
	DD	2812485555
	DD	1510632735
	DD	4012839307
	DD	667617719
	DD	1832287951
	DD	2657085997
	DD	1330003814
	DD	3021265470
	DD	3730711318
	DD	1335235438
	DD	3664575902
	DD	1019204698
	DD	2660007629
	DD	1747563645
	DD	3166455341
	DD	2670470877
	DD	3034184508
	DD	2038409397
	DD	1025047962
	DD	3495127291
	DD	2037943386
	DD	1045974458
	DD	1773401721
	DD	4076818795
	DD	2050095924
	DD	2695287286
	DD	4075886773
	DD	2091948916
	DD	3546803442
	DD	3858670295
	DD	4100191849
	DD	1095607277
	DD	3856806251
	DD	4183897833
	DD	2798639588
	DD	3422373294
	DD	3905416403
	DD	2191214555
	DD	3418645206
	DD	4072828371
	DD	1302311881
	DD	2549779293
	DD	3515865511
	DD	87461814
	DD	2542323117
	DD	3850689447
	DD	2604623763
	DD	804591290
	DD	2736763727
	DD	174923629
	DD	789678938
	DD	3406411599
	DD	914280231
	DD	1609182581
	DD	1178560158
	DD	349847258
	DD	1579357876
	DD	2517855902
	DD	1828560463
	DD	3218365162
	DD	2357120316
	DD	699694516
	DD	3158715752
	DD	740744509
	DD	3657120927
	DD	2141763028
	DD	419273337
	DD	1399389033
	DD	2022464208
	DD	1481489018
	DD	3019274558
	DD	4283526057
	DD	838546674
	DD	2798778067
	DD	4044928416
	DD	2962978036
	DD	1743581820
	DD	4272084818
	DD	1677093349
	DD	1302588838
	DD	3794889537
	DD	1630988776
	DD	3487163641
	DD	4249202340
	DD	3354186699
	DD	2605177677
	DD	3294811778
	DD	3261977553
	DD	2679359986
	DD	4203437385
	DD	2413406103
	DD	915388059
	DD	2294656261
	DD	2228987811
	DD	1063752677
	DD	4111907475
	DD	531844911
	DD	1830776118
	DD	294345226
	DD	163008326
	DD	2127505355
	DD	3928847655
	DD	1063689823
	DD	3661552236
	DD	588690452
	DD	326016652
	DD	4255010710
	DD	3562728015
	DD	2127379647
	DD	3028137177
	DD	1177380905
	DD	652033304
	DD	4215054124
	DD	2830488734
	DD	4254759295
	DD	1761307059
	DD	2354761811
	DD	1304066609
	DD	4135140952
	DD	1366010173
	DD	4214551295
	DD	3522614119
	DD	414556326
	DD	2608133219
	DD	3975314608
	DD	2732020347
	DD	4134135294
	DD	2750260943
	DD	829112653
	DD	921299143
	DD	3655661921
	DD	1169073399
	DD	3973303293
	DD	1205554591
	DD	1658225307
	DD	1842598287
	DD	3016356546
	DD	2338146798
	DD	3651639290
	DD	2411109183
	DD	3316450614
	DD	3685196575
	DD	1737745796
	DD	381326301
	DD	3008311285
	DD	527251070
	DD	2337933933
	DD	3075425855
	DD	3475491593
	DD	762652602
	DD	1721655274
	DD	1054502141
	DD	380900570
	DD	1855884414
	DD	2656015891
	DD	1525305205
	DD	3443310548
	DD	2109004283
	DD	761801140
	DD	3711768829
	DD	1017064486
	DD	3050610411
	DD	2591653800
	DD	4218008566
	DD	1523602280
	DD	3128570363
	DD	2034128973
	DD	1806253526
	DD	888340305
	DD	4141049836
	DD	3047204561
	DD	1962173430
	DD	4068257947
	DD	3612507052
	DD	1776680610
	DD	3987132377
	DD	1799441827
	DD	3924346860
	DD	3841548598
	DD	2930046808
	DD	3553361221
	DD	3679297459
	DD	3598883655
	DD	3553726425
	DD	3388129901
	DD	1565126321
	DD	2811755147
	DD	3063627623
	DD	2902800015
	DD	2812485555
	DD	2481292507
	DD	3130252643
	DD	1328542998
	DD	1832287951
	DD	1510632735
	DD	1330003814
	DD	667617719
	DD	1965537991
	DD	2657085997
	DD	3664575902
	DD	3021265470
	DD	2660007629
	DD	1335235438
	DD	3931075983
	DD	1019204698
	DD	3034184508
	DD	1747563645
	DD	1025047962
	DD	2670470877
	DD	3567184671
	DD	2038409397
	DD	1773401721
	DD	3495127291
	DD	2050095924
	DD	1045974458
	DD	2839402047
	DD	4076818795
	DD	3546803442
	DD	2695287286
	DD	4100191849
	DD	2091948916
	DD	1383836798
	DD	3858670295
	DD	2798639588
	DD	1095607277
	DD	3905416403
	DD	4183897833
	DD	2767673597
	DD	3422373294
	DD	1302311881
	DD	2191214555
	DD	3515865511
	DD	4072828371
	DD	1240379898
	DD	2549779293
	DD	2604623763
	DD	87461814
	DD	2736763727
	DD	3850689447
	DD	2480759797
	DD	804591290
	DD	914280231
	DD	174923629
	DD	1178560158
	DD	3406411599
	DD	666552299
	DD	1609182581
	DD	1828560463
	DD	349847258
	DD	2357120316
	DD	2517855902
	DD	1333104599
	DD	3218365162
	DD	3657120927
	DD	699694516
	DD	419273337
	DD	740744509
	DD	2666209199
	DD	2141763028
	DD	3019274558
	DD	1399389033
	DD	838546674
	DD	1481489018
	DD	1037451103
	DD	4283526057
	DD	1743581820
	DD	2798778067
	DD	1677093349
	DD	2962978036
	DD	2074902206
	DD	4272084818
	DD	3487163641
	DD	1302588838
	DD	3354186699
	DD	1630988776
	DD	4149804412
	DD	4249202340
	DD	2679359986
	DD	2605177677
	DD	2413406103
	DD	3261977553
	DD	4004641529
	DD	4203437385
	DD	1063752677
	DD	915388059
	DD	531844911
	DD	2228987811
	DD	3714315762
	DD	4111907475
	DD	2127505355
	DD	1830776118
	DD	1063689823
	DD	163008326
	DD	3133664229
	DD	3928847655
	DD	4255010710
	DD	3661552236
	DD	2127379647
	DD	326016652
	DD	1972361163
	DD	3562728015
	DD	4215054124
	DD	3028137177
	DD	4254759295
	DD	652033304
	DD	3944722327
	DD	2830488734
	DD	4135140952
	DD	1761307059
	DD	4214551295
	DD	1304066609
	DD	3594477359
	DD	1366010173
	DD	3975314608
	DD	3522614119
	DD	4134135294
	DD	2608133219
	DD	2893987423
	DD	2732020347
	DD	3655661921
	DD	2750260943
	DD	3973303293
	DD	921299143
	DD	1493007550
	DD	1169073399
	DD	3016356546
	DD	1205554591
	DD	3651639290
	DD	1842598287
	DD	2986015100
	DD	2338146798
	DD	1737745796
	DD	2411109183
	DD	3008311285
	DD	3685196575
	DD	1677062904
	DD	381326301
	DD	3475491593
	DD	527251070
	DD	1721655274
	DD	3075425855
	DD	3354125809
	DD	762652602
	DD	2656015891
	DD	1054502141
	DD	3443310548
	DD	1855884414
	DD	2413284322
	DD	1525305205
	DD	1017064486
	DD	2109004283
	DD	2591653800
	DD	3711768829
	DD	531601349
	DD	3050610411
	DD	2034128973
	DD	4218008566
	DD	888340305
	DD	3128570363
	DD	1063202699
	DD	1806253526
	DD	4068257947
	DD	4141049836
	DD	1776680610
	DD	1962173430
	DD	2126405399
	DD	3612507052
	DD	3841548598
	DD	3987132377
	DD	3553361221
	DD	3924346860
	DD	4252810799
	DD	2930046808
	DD	3388129901
	DD	3679297459
	DD	2811755147
	DD	3553726425
	DD	4210654302
	DD	1565126321
	DD	2481292507
	DD	3063627623
	DD	1328542998
	DD	2812485555
	DD	4126341309
	DD	3130252643
	DD	667617719
	DD	1832287951
	DD	2657085997
	DD	1330003814
	DD	3957715323
	DD	1965537991
	DD	1335235438
	DD	3664575902
	DD	1019204698
	DD	2660007629
	DD	3620463350
	DD	3931075983
	DD	2670470877
	DD	3034184508
	DD	2038409397
	DD	1025047962
	DD	2945959404
	DD	3567184671
	DD	1045974458
	DD	1773401721
	DD	4076818795
	DD	2050095924
	DD	1596951513
	DD	2839402047
	DD	2091948916
	DD	3546803442
	DD	3858670295
	DD	4100191849
	DD	3193903027
	DD	1383836798
	DD	4183897833
	DD	2798639588
	DD	3422373294
	DD	3905416403
	DD	2092838759
	DD	2767673597
	DD	4072828371
	DD	1302311881
	DD	2549779293
	DD	3515865511
	DD	4185677519
	DD	1240379898
	DD	3850689447
	DD	2604623763
	DD	804591290
	DD	2736763727
	DD	4076387742
	DD	2480759797
	DD	3406411599
	DD	914280231
	DD	1609182581
	DD	1178560158
	DD	3857808189
	DD	666552299
	DD	2517855902
	DD	1828560463
	DD	3218365162
	DD	2357120316
	DD	3420649082
	DD	1333104599
	DD	740744509
	DD	3657120927
	DD	2141763028
	DD	419273337
	DD	2546330868
	DD	2666209199
	DD	1481489018
	DD	3019274558
	DD	4283526057
	DD	838546674
	DD	797694440
	DD	1037451103
	DD	2962978036
	DD	1743581820
	DD	4272084818
	DD	1677093349
	DD	1595388880
	DD	2074902206
	DD	1630988776
	DD	3487163641
	DD	4249202340
	DD	3354186699
	DD	3190777760
	DD	4149804412
	DD	3261977553
	DD	2679359986
	DD	4203437385
	DD	2413406103
	DD	2086588225
	DD	4004641529
	DD	2228987811
	DD	1063752677
	DD	4111907475
	DD	531844911
	DD	4173176451
	DD	3714315762
	DD	163008326
	DD	2127505355
	DD	3928847655
	DD	1063689823
	DD	4051385607
	DD	3133664229
	DD	326016652
	DD	4255010710
	DD	3562728015
	DD	2127379647
	DD	3807803918
	DD	1972361163
	DD	652033304
	DD	4215054124
	DD	2830488734
	DD	4254759295
	DD	3320640540
	DD	3944722327
	DD	1304066609
	DD	4135140952
	DD	1366010173
	DD	4214551295
	DD	2346313785
	DD	3594477359
	DD	2608133219
	DD	3975314608
	DD	2732020347
	DD	4134135294
	DD	397660275
	DD	2893987423
	DD	921299143
	DD	3655661921
	DD	1169073399
	DD	3973303293
	DD	795320551
	DD	1493007550
	DD	1842598287
	DD	3016356546
	DD	2338146798
	DD	3651639290
	DD	1590641102
	DD	2986015100
	DD	3685196575
	DD	1737745796
	DD	381326301
	DD	3008311285
	DD	3181282204
	DD	1677062904
	DD	3075425855
	DD	3475491593
	DD	762652602
	DD	1721655274
	DD	2067597113
	DD	3354125809
	DD	1855884414
	DD	2656015891
	DD	1525305205
	DD	3443310548
	DD	4135194227
	DD	2413284322
	DD	3711768829
	DD	1017064486
	DD	3050610411
	DD	2591653800
	DD	3975421159
	DD	531601349
	DD	3128570363
	DD	2034128973
	DD	1806253526
	DD	888340305
	DD	3655875023
	DD	1063202699
	DD	1962173430
	DD	4068257947
	DD	3612507052
	DD	1776680610
	DD	3016782751
	DD	2126405399
	DD	3924346860
	DD	3841548598
	DD	2930046808
	DD	3553361221
	DD	1738598206
	DD	4252810799
	DD	3553726425
	DD	3388129901
	DD	1565126321
	DD	2811755147
	DD	3477196413
	DD	4210654302
	DD	2812485555
	DD	2481292507
	DD	3130252643
	DD	1328542998
	DD	2659425531
	DD	4126341309
	DD	1330003814
	DD	667617719
	DD	1965537991
	DD	2657085997
	DD	1023883767
	DD	3957715323
	DD	2660007629
	DD	1335235438
	DD	3931075983
	DD	1019204698
	DD	2047767535
	DD	3620463350
	DD	1025047962
	DD	2670470877
	DD	3567184671
	DD	2038409397
	DD	4095535070
	DD	2945959404
	DD	2050095924
	DD	1045974458
	DD	2839402047
	DD	4076818795
	DD	3896102844
	DD	1596951513
	DD	4100191849
	DD	2091948916
	DD	1383836798
	DD	3858670295
	DD	3497238392
	DD	3193903027
	DD	3905416403
	DD	4183897833
	DD	2767673597
	DD	3422373294
	DD	2699509489
	DD	2092838759
	DD	3515865511
	DD	4072828371
	DD	1240379898
	DD	2549779293
	DD	1104051682
	DD	4185677519
	DD	2736763727
	DD	3850689447
	DD	2480759797
	DD	804591290
	DD	2208103365
	DD	4076387742
	DD	1178560158
	DD	3406411599
	DD	666552299
	DD	1609182581
	DD	121239434
	DD	3857808189
	DD	2357120316
	DD	2517855902
	DD	1333104599
	DD	3218365162
	DD	242478868
	DD	3420649082
	DD	419273337
	DD	740744509
	DD	2666209199
	DD	2141763028
	DD	484957737
	DD	2546330868
	DD	838546674
	DD	1481489018
	DD	1037451103
	DD	4283526057
	DD	969915474
	DD	797694440
	DD	1677093349
	DD	2962978036
	DD	2074902206
	DD	4272084818
	DD	1939830949
	DD	1595388880
	DD	3354186699
	DD	1630988776
	DD	4149804412
	DD	4249202340
	DD	3879661898
	DD	3190777760
	DD	2413406103
	DD	3261977553
	DD	4004641529
	DD	4203437385
	DD	3464356500
	DD	2086588225
	DD	531844911
	DD	2228987811
	DD	3714315762
	DD	4111907475
	DD	2633745705
	DD	4173176451
	DD	1063689823
	DD	163008326
	DD	3133664229
	DD	3928847655
	DD	972524114
	DD	4051385607
	DD	2127379647
	DD	326016652
	DD	1972361163
	DD	3562728015
	DD	1945048229
	DD	3807803918
	DD	4254759295
	DD	652033304
	DD	3944722327
	DD	2830488734
	DD	3890096458
	DD	3320640540
	DD	4214551295
	DD	1304066609
	DD	3594477359
	DD	1366010173
	DD	3485225620
	DD	2346313785
	DD	4134135294
	DD	2608133219
	DD	2893987423
	DD	2732020347
	DD	2675483945
	DD	397660275
	DD	3973303293
	DD	921299143
	DD	1493007550
	DD	1169073399
	DD	1056000594
	DD	795320551
	DD	3651639290
	DD	1842598287
	DD	2986015100
	DD	2338146798
	DD	2112001188
	DD	1590641102
	DD	3008311285
	DD	3685196575
	DD	1677062904
	DD	381326301
	DD	4224002377
	DD	3181282204
	DD	1721655274
	DD	3075425855
	DD	3354125809
	DD	762652602
	DD	4153037458
	DD	2067597113
	DD	3443310548
	DD	1855884414
	DD	2413284322
	DD	1525305205
	DD	4011107621
	DD	4135194227
	DD	2591653800
	DD	3711768829
	DD	531601349
	DD	3050610411
	DD	3727247947
	DD	3975421159
	DD	888340305
	DD	3128570363
	DD	1063202699
	DD	1806253526
	DD	3159528599
	DD	3655875023
	DD	1776680610
	DD	1962173430
	DD	2126405399
	DD	3612507052
	DD	2024089902
	DD	3016782751
	DD	3553361221
	DD	3924346860
	DD	4252810799
	DD	2930046808
	DD	4048179805
	DD	1738598206
	DD	2811755147
	DD	3553726425
	DD	4210654302
	DD	1565126321
	DD	3801392314
	DD	3477196413
	DD	1328542998
	DD	2812485555
	DD	4126341309
	DD	3130252643
	DD	3307817333
	DD	2659425531
	DD	2657085997
	DD	1330003814
	DD	3957715323
	DD	1965537991
	DD	2320667370
	DD	1023883767
	DD	1019204698
	DD	2660007629
	DD	3620463350
	DD	3931075983
	DD	346367444
	DD	2047767535
	DD	2038409397
	DD	1025047962
	DD	2945959404
	DD	3567184671
	DD	692734889
	DD	4095535070
	DD	4076818795
	DD	2050095924
	DD	1596951513
	DD	2839402047
	DD	1385469779
	DD	3896102844
	DD	3858670295
	DD	4100191849
	DD	3193903027
	DD	1383836798
	DD	2770939558
	DD	3497238392
	DD	3422373294
	DD	3905416403
	DD	2092838759
	DD	2767673597
	DD	1246911821
	DD	2699509489
	DD	2549779293
	DD	3515865511
	DD	4185677519
	DD	1240379898
	DD	2493823642
	DD	1104051682
	DD	804591290
	DD	2736763727
	DD	4076387742
	DD	2480759797
	DD	692679989
	DD	2208103365
	DD	1609182581
	DD	1178560158
	DD	3857808189
	DD	666552299
	DD	1385359979
	DD	121239434
	DD	3218365162
	DD	2357120316
	DD	3420649082
	DD	1333104599
	DD	2770719959
	DD	242478868
	DD	2141763028
	DD	419273337
	DD	2546330868
	DD	2666209199
	DD	1246472623
	DD	484957737
	DD	4283526057
	DD	838546674
	DD	797694440
	DD	1037451103
	DD	2492945247
	DD	969915474
	DD	4272084818
	DD	1677093349
	DD	1595388880
	DD	2074902206
	DD	690923199
	DD	1939830949
	DD	4249202340
	DD	3354186699
	DD	3190777760
	DD	4149804412
	DD	1381846399
	DD	3879661898
	DD	4203437385
	DD	2413406103
	DD	2086588225
	DD	4004641529
	DD	2763692798
	DD	3464356500
	DD	4111907475
	DD	531844911
	DD	4173176451
	DD	3714315762
	DD	1232418301
	DD	2633745705
	DD	3928847655
	DD	1063689823
	DD	4051385607
	DD	3133664229
	DD	2464836603
	DD	972524114
	DD	3562728015
	DD	2127379647
	DD	3807803918
	DD	1972361163
	DD	634705910
	DD	1945048229
	DD	2830488734
	DD	4254759295
	DD	3320640540
	DD	3944722327
	DD	1269411821
	DD	3890096458
	DD	1366010173
	DD	4214551295
	DD	2346313785
	DD	3594477359
	DD	2538823642
	DD	3485225620
	DD	2732020347
	DD	4134135294
	DD	397660275
	DD	2893987423
	DD	782679989
	DD	2675483945
	DD	1169073399
	DD	3973303293
	DD	795320551
	DD	1493007550
	DD	1565359979
	DD	1056000594
	DD	2338146798
	DD	3651639290
	DD	1590641102
	DD	2986015100
	DD	3130719959
	DD	2112001188
	DD	381326301
	DD	3008311285
	DD	3181282204
	DD	1677062904
	DD	1966472623
	DD	4224002377
	DD	762652602
	DD	1721655274
	DD	2067597113
	DD	3354125809
	DD	3932945247
	DD	4153037458
	DD	1525305205
	DD	3443310548
	DD	4135194227
	DD	2413284322
	DD	3570923199
	DD	4011107621
	DD	3050610411
	DD	2591653800
	DD	3975421159
	DD	531601349
	DD	2846879102
	DD	3727247947
	DD	1806253526
	DD	888340305
	DD	3655875023
	DD	1063202699
	DD	1398790909
	DD	3159528599
	DD	3612507052
	DD	1776680610
	DD	3016782751
	DD	2126405399
	DD	2797581819
	DD	2024089902
	DD	2930046808
	DD	3553361221
	DD	1738598206
	DD	4252810799
	DD	1300196342
	DD	4048179805
	DD	1565126321
	DD	2811755147
	DD	3477196413
	DD	4210654302
	DD	2600392684
	DD	3801392314
	DD	3130252643
	DD	1328542998
	DD	2659425531
	DD	4126341309
	DD	905818072
	DD	3307817333
	DD	1965537991
	DD	2657085997
	DD	1023883767
	DD	3957715323
	DD	1811636145
	DD	2320667370
	DD	3931075983
	DD	1019204698
	DD	2047767535
	DD	3620463350
	DD	3623272290
	DD	346367444
	DD	3567184671
	DD	2038409397
	DD	4095535070
	DD	2945959404
	DD	2951577284
	DD	692734889
	DD	2839402047
	DD	4076818795
	DD	3896102844
	DD	1596951513
	DD	1608187272
	DD	1385469779
	DD	1383836798
	DD	3858670295
	DD	3497238392
	DD	3193903027
	DD	3216374545
	DD	2770939558
	DD	2767673597
	DD	3422373294
	DD	2699509489
	DD	2092838759
	DD	2137781795
	DD	1246911821
	DD	1240379898
	DD	2549779293
	DD	1104051682
	DD	4185677519
	DD	4275563591
	DD	2493823642
	DD	2480759797
	DD	804591290
	DD	2208103365
	DD	4076387742
	DD	4256159887
	DD	692679989
	DD	666552299
	DD	1609182581
	DD	121239434
	DD	3857808189
	DD	4217352479
	DD	1385359979
	DD	1333104599
	DD	3218365162
	DD	242478868
	DD	3420649082
	DD	4139737663
	DD	2770719959
	DD	2666209199
	DD	2141763028
	DD	484957737
	DD	2546330868
	DD	3984508030
	DD	1246472623
	DD	1037451103
	DD	4283526057
	DD	969915474
	DD	797694440
	DD	3674048764
	DD	2492945247
	DD	2074902206
	DD	4272084818
	DD	1939830949
	DD	1595388880
	DD	3053130232
	DD	690923199
	DD	4149804412
	DD	4249202340
	DD	3879661898
	DD	3190777760
	DD	1811293169
	DD	1381846399
	DD	4004641529
	DD	4203437385
	DD	3464356500
	DD	2086588225
	DD	3622586339
	DD	2763692798
	DD	3714315762
	DD	4111907475
	DD	2633745705
	DD	4173176451
	DD	2950205382
	DD	1232418301
	DD	3133664229
	DD	3928847655
	DD	972524114
	DD	4051385607
	DD	1605443469
	DD	2464836603
	DD	1972361163
	DD	3562728015
	DD	1945048229
	DD	3807803918
	DD	3210886938
	DD	634705910
	DD	3944722327
	DD	2830488734
	DD	3890096458
	DD	3320640540
	DD	2126806581
	DD	1269411821
	DD	3594477359
	DD	1366010173
	DD	3485225620
	DD	2346313785
	DD	4253613162
	DD	2538823642
	DD	2893987423
	DD	2732020347
	DD	2675483945
	DD	397660275
	DD	4212259029
	DD	782679989
	DD	1493007550
	DD	1169073399
	DD	1056000594
	DD	795320551
	DD	4129550763
	DD	1565359979
	DD	2986015100
	DD	2338146798
	DD	2112001188
	DD	1590641102
	DD	3964134231
	DD	3130719959
	DD	1677062904
	DD	381326301
	DD	4224002377
	DD	3181282204
	DD	3633301166
	DD	1966472623
	DD	3354125809
	DD	762652602
	DD	4153037458
	DD	2067597113
	DD	2971635037
	DD	3932945247
	DD	2413284322
	DD	1525305205
	DD	4011107621
	DD	4135194227
	DD	1648302778
	DD	3570923199
	DD	531601349
	DD	3050610411
	DD	3727247947
	DD	3975421159
	DD	3296605556
	DD	2846879102
	DD	1063202699
	DD	1806253526
	DD	3159528599
	DD	3655875023
	DD	2298243816
	DD	1398790909
	DD	2126405399
	DD	3612507052
	DD	2024089902
	DD	3016782751
	DD	301520336
	DD	2797581819
	DD	4252810799
	DD	2930046808
	DD	4048179805
	DD	1738598206
	DD	603040673
	DD	1300196342
	DD	4210654302
	DD	1565126321
	DD	3801392314
	DD	3477196413
	DD	1206081346
	DD	2600392684
	DD	4126341309
	DD	3130252643
	DD	3307817333
	DD	2659425531
	DD	2412162692
	DD	905818072
	DD	3957715323
	DD	1965537991
	DD	2320667370
	DD	1023883767
	DD	529358088
	DD	1811636145
	DD	3620463350
	DD	3931075983
	DD	346367444
	DD	2047767535
	DD	1058716176
	DD	3623272290
	DD	2945959404
	DD	3567184671
	DD	692734889
	DD	4095535070
	DD	2117432353
	DD	2951577284
	DD	1596951513
	DD	2839402047
	DD	1385469779
	DD	3896102844
	DD	4234864706
	DD	1608187272
	DD	3193903027
	DD	1383836798
	DD	2770939558
	DD	3497238392
	DD	4174762117
	DD	3216374545
	DD	2092838759
	DD	2767673597
	DD	1246911821
	DD	2699509489
	DD	4054556938
	DD	2137781795
	DD	4185677519
	DD	1240379898
	DD	2493823642
	DD	1104051682
	DD	3814146581
	DD	4275563591
	DD	4076387742
	DD	2480759797
	DD	692679989
	DD	2208103365
	DD	3333325867
	DD	4256159887
	DD	3857808189
	DD	666552299
	DD	1385359979
	DD	121239434
	DD	2371684438
	DD	4217352479
	DD	3420649082
	DD	1333104599
	DD	2770719959
	DD	242478868
	DD	448401580
	DD	4139737663
	DD	2546330868
	DD	2666209199
	DD	1246472623
	DD	484957737
	DD	896803160
	DD	3984508030
	DD 128 DUP (0H)	
	PUBLIC __svml_dsin_ha_data_internal_ha
__svml_dsin_ha_data_internal_ha	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2376373521
	DD	3205741868
	DD	0
	DD	1072693248
	DD	4242458500
	DD	1065951697
	DD	2684354560
	DD	1009354638
	DD	3650591271
	DD	3207838971
	DD	0
	DD	1072693248
	DD	4154680958
	DD	1067000149
	DD	0
	DD	3153796451
	DD	69373323
	DD	3209049152
	DD	0
	DD	1072693248
	DD	1972655565
	DD	1067636837
	DD	1073741824
	DD	1009157878
	DD	220529721
	DD	3209935929
	DD	0
	DD	1072693248
	DD	4044216340
	DD	1068048229
	DD	0
	DD	3156808381
	DD	3810305407
	DD	3210663220
	DD	0
	DD	1072693248
	DD	2046304480
	DD	1068459374
	DD	3758096384
	DD	3156402667
	DD	4269262315
	DD	3211145810
	DD	0
	DD	1072693248
	DD	2462980598
	DD	1068684576
	DD	2684354560
	DD	3158941832
	DD	1118107366
	DD	3211670787
	DD	0
	DD	1072693248
	DD	177906713
	DD	1068889808
	DD	3221225472
	DD	3159310370
	DD	393047345
	DD	3212032302
	DD	0
	DD	1072693248
	DD	3156849708
	DD	1069094822
	DD	3221225472
	DD	3158189848
	DD	1227670166
	DD	3212375297
	DD	0
	DD	1072693248
	DD	3349070549
	DD	1069299589
	DD	3758096384
	DD	3156538269
	DD	2402293340
	DD	3212758471
	DD	0
	DD	1072693248
	DD	1453945614
	DD	1069504078
	DD	1610612736
	DD	1011492612
	DD	2592747447
	DD	3213009315
	DD	0
	DD	1072693248
	DD	3470456183
	DD	1069627888
	DD	1073741824
	DD	3160744771
	DD	1037482584
	DD	3213240992
	DD	0
	DD	1072693248
	DD	1854824762
	DD	1069729808
	DD	2684354560
	DD	1011953664
	DD	3451754846
	DD	3213492662
	DD	0
	DD	1072693248
	DD	2972270170
	DD	1069831542
	DD	1073741824
	DD	3159524770
	DD	1672287667
	DD	3213764289
	DD	0
	DD	1072693248
	DD	1149976518
	DD	1069933076
	DD	1610612736
	DD	1013264895
	DD	2346447124
	DD	3213970635
	DD	0
	DD	1072693248
	DD	3729833777
	DD	1070034393
	DD	3758096384
	DD	1013044718
	DD	18115067
	DD	3214126342
	DD	0
	DD	1072693248
	DD	1013556747
	DD	1070135480
	DD	2684354560
	DD	3160567065
	DD	2660899430
	DD	3214291960
	DD	0
	DD	1072693248
	DD	632292433
	DD	1070236320
	DD	3758096384
	DD	3160963333
	DD	1935888103
	DD	3214467466
	DD	0
	DD	1072693248
	DD	1786662755
	DD	1070336898
	DD	2147483648
	DD	3155882307
	DD	288924873
	DD	3214652833
	DD	0
	DD	1072693248
	DD	3846521617
	DD	1070437199
	DD	536870912
	DD	1013406610
	DD	2377770267
	DD	3214848032
	DD	0
	DD	1072693248
	DD	2065784603
	DD	1070537209
	DD	3758096384
	DD	3160681966
	DD	3250111450
	DD	3214993525
	DD	0
	DD	1072693248
	DD	238550446
	DD	1070616504
	DD	2684354560
	DD	3154383565
	DD	2306566604
	DD	3215100913
	DD	0
	DD	1072693248
	DD	1655830135
	DD	1070666194
	DD	0
	DD	1014506690
	DD	1913604284
	DD	3215213171
	DD	0
	DD	1072693248
	DD	1062001470
	DD	1070715716
	DD	0
	DD	3160875220
	DD	2476548698
	DD	3215330282
	DD	0
	DD	1072693248
	DD	785751814
	DD	1070765062
	DD	2684354560
	DD	3161838221
	DD	1261629236
	DD	3215452229
	DD	0
	DD	1072693248
	DD	3269558610
	DD	1070814224
	DD	536870912
	DD	3161705216
	DD	997322465
	DD	3215578993
	DD	0
	DD	1072693248
	DD	2484561409
	DD	1070863196
	DD	1073741824
	DD	1014024572
	DD	1296295961
	DD	3215710555
	DD	0
	DD	1072693248
	DD	1115219818
	DD	1070911970
	DD	0
	DD	1013072242
	DD	2962723252
	DD	3215846895
	DD	0
	DD	1072693248
	DD	1974149085
	DD	1070960538
	DD	3221225472
	DD	3156147648
	DD	4002549419
	DD	3215985292
	DD	0
	DD	1072693248
	DD	3711903686
	DD	1071008893
	DD	1073741824
	DD	1011944747
	DD	1234824733
	DD	3216058210
	DD	0
	DD	1072693248
	DD	821708191
	DD	1071057029
	DD	3758096384
	DD	3161738009
	DD	3996842794
	DD	3216133484
	DD	0
	DD	1072693248
	DD	824036583
	DD	1071104937
	DD	0
	DD	1008313330
	DD	2255197647
	DD	3216211105
	DD	0
	DD	1072693248
	DD	2796464483
	DD	1071152610
	DD	3221225472
	DD	3160878317
	DD	1638976426
	DD	3216291060
	DD	0
	DD	1072693248
	DD	1673302999
	DD	1071200042
	DD	2147483648
	DD	1013351473
	DD	1972483635
	DD	3216373337
	DD	0
	DD	1072693248
	DD	1135144330
	DD	1071247225
	DD	536870912
	DD	1013373569
	DD	1577996576
	DD	3216457924
	DD	0
	DD	1072693248
	DD	728580042
	DD	1071294152
	DD	1610612736
	DD	3162332944
	DD	1578746984
	DD	3216544808
	DD	0
	DD	1072693248
	DD	165764288
	DD	1071340816
	DD	3221225472
	DD	1013298018
	DD	1612193054
	DD	3216633976
	DD	0
	DD	1072693248
	DD	3623952103
	DD	1071387209
	DD	2684354560
	DD	3161985962
	DD	4133449816
	DD	3216725414
	DD	0
	DD	1072693248
	DD	2570175582
	DD	1071433326
	DD	1073741824
	DD	1012303118
	DD	1539072807
	DD	3216819110
	DD	0
	DD	1072693248
	DD	1535566729
	DD	1071479159
	DD	2147483648
	DD	1014321388
	DD	1945768569
	DD	3216915048
	DD	0
	DD	1072693248
	DD	939980347
	DD	1071524701
	DD	0
	DD	1012796809
	DD	3429717432
	DD	3217013214
	DD	0
	DD	1072693248
	DD	1391425750
	DD	1071569945
	DD	3221225472
	DD	3162362103
	DD	1312926554
	DD	3217072381
	DD	0
	DD	1072693248
	DD	3690502842
	DD	1071614884
	DD	3221225472
	DD	3157453855
	DD	1663679671
	DD	3217123670
	DD	0
	DD	1072693248
	DD	2269920951
	DD	1071652092
	DD	3758096384
	DD	3161672245
	DD	3952756490
	DD	3217176050
	DD	0
	DD	1072693248
	DD	2569207790
	DD	1071674247
	DD	1073741824
	DD	3163183617
	DD	69903290
	DD	3217229515
	DD	0
	DD	1072693248
	DD	1297975695
	DD	1071696240
	DD	3221225472
	DD	3163083735
	DD	2678847490
	DD	3217284054
	DD	0
	DD	1072693248
	DD	1411074851
	DD	1071718067
	DD	1610612736
	DD	3163484731
	DD	2272832445
	DD	3217339661
	DD	0
	DD	1072693248
	DD	1675549513
	DD	1071739725
	DD	3221225472
	DD	3163025138
	DD	1539668340
	DD	3217396327
	DD	0
	DD	1072693248
	DD	967731400
	DD	1071761211
	DD	536870912
	DD	1015752157
	DD	2482246777
	DD	3217454043
	DD	0
	DD	1072693248
	DD	2570316633
	DD	1071782521
	DD	1610612736
	DD	3162744753
	DD	2129093113
	DD	3217512801
	DD	0
	DD	1072693248
	DD	1289556703
	DD	1071803653
	DD	536870912
	DD	3162888820
	DD	1129922991
	DD	3217572592
	DD	0
	DD	1072693248
	DD	637203434
	DD	1071824603
	DD	3221225472
	DD	3159443841
	DD	3761365757
	DD	3217633406
	DD	0
	DD	1072693248
	DD	4242632757
	DD	1071845367
	DD	536870912
	DD	3161390278
	DD	752919289
	DD	3217695236
	DD	0
	DD	1072693248
	DD	2969983475
	DD	1071865944
	DD	2684354560
	DD	3162635443
	DD	3652612115
	DD	3217758070
	DD	0
	DD	1072693248
	DD	689983673
	DD	1071886330
	DD	3758096384
	DD	1014906405
	DD	1883353036
	DD	3217821901
	DD	0
	DD	1072693248
	DD	1397053140
	DD	1071906521
	DD	1610612736
	DD	1015245078
	DD	1403757309
	DD	3217886718
	DD	0
	DD	1072693248
	DD	621354454
	DD	1071926515
	DD	536870912
	DD	1013450602
	DD	3239527685
	DD	3217952511
	DD	0
	DD	1072693248
	DD	2610628921
	DD	1071946308
	DD	1610612736
	DD	3162668769
	DD	3489768009
	DD	3218019271
	DD	0
	DD	1072693248
	DD	3152274921
	DD	1071965898
	DD	3758096384
	DD	1013170835
	DD	2371640770
	DD	1070588851
	DD	0
	DD	1071644672
	DD	2460177814
	DD	1071985282
	DD	2147483648
	DD	3162116843
	DD	2309668539
	DD	1070520188
	DD	0
	DD	1071644672
	DD	881650848
	DD	1072004457
	DD	2147483648
	DD	3162986272
	DD	2081278341
	DD	1070450589
	DD	0
	DD	1071644672
	DD	3194290572
	DD	1072023419
	DD	2147483648
	DD	1015048682
	DD	3753901657
	DD	1070380064
	DD	0
	DD	1071644672
	DD	1427975391
	DD	1072042167
	DD	1073741824
	DD	3161608627
	DD	1403691706
	DD	1070308625
	DD	0
	DD	1071644672
	DD	636516413
	DD	1072060697
	DD	3758096384
	DD	1015823716
	DD	2583490354
	DD	1070236281
	DD	0
	DD	1071644672
	DD	1719614413
	DD	1072079006
	DD	0
	DD	3163282740
	DD	2546065654
	DD	1070163044
	DD	0
	DD	1071644672
	DD	1424664751
	DD	1072097092
	DD	3221225472
	DD	3163405315
	DD	1416934267
	DD	1070088925
	DD	0
	DD	1071644672
	DD	938475414
	DD	1072114952
	DD	0
	DD	1015076079
	DD	4187227697
	DD	1070013934
	DD	0
	DD	1071644672
	DD	1594061409
	DD	1072132583
	DD	2147483648
	DD	1014353870
	DD	3526603391
	DD	1069938084
	DD	0
	DD	1071644672
	DD	577417135
	DD	1072149983
	DD	2684354560
	DD	3161519415
	DD	1250776663
	DD	1069861386
	DD	0
	DD	1071644672
	DD	3814135665
	DD	1072167148
	DD	3221225472
	DD	3163146456
	DD	4019165092
	DD	1069783850
	DD	0
	DD	1071644672
	DD	201299822
	DD	1072184078
	DD	3758096384
	DD	3161912352
	DD	1852581091
	DD	1069705490
	DD	0
	DD	1071644672
	DD	263859903
	DD	1072200768
	DD	0
	DD	3162692284
	DD	2485417816
	DD	1069626316
	DD	0
	DD	1071644672
	DD	1796544321
	DD	1072217216
	DD	536870912
	DD	3162686945
	DD	2586490530
	DD	1069545162
	DD	0
	DD	1071644672
	DD	2750387213
	DD	1072233420
	DD	2147483648
	DD	3160858537
	DD	1224902090
	DD	1069383633
	DD	0
	DD	1071644672
	DD	1234330619
	DD	1072249378
	DD	2684354560
	DD	1015272473
	DD	2285031318
	DD	1069220549
	DD	0
	DD	1071644672
	DD	4106737474
	DD	1072265086
	DD	2684354560
	DD	3162592377
	DD	3876290983
	DD	1069055935
	DD	0
	DD	1071644672
	DD	1207142209
	DD	1072280544
	DD	2147483648
	DD	3162705634
	DD	802660176
	DD	1068889817
	DD	0
	DD	1071644672
	DD	4012486548
	DD	1072295747
	DD	2684354560
	DD	1014701564
	DD	1726519029
	DD	1068722218
	DD	0
	DD	1071644672
	DD	2688952194
	DD	1072310695
	DD	0
	DD	3161532603
	DD	3382663878
	DD	1068553164
	DD	0
	DD	1071644672
	DD	453180130
	DD	1072325385
	DD	1073741824
	DD	3162411281
	DD	2598800519
	DD	1068266419
	DD	0
	DD	1071644672
	DD	688824739
	DD	1072339814
	DD	3758096384
	DD	1010431536
	DD	1038988426
	DD	1067922646
	DD	0
	DD	1071644672
	DD	2653017361
	DD	1072353980
	DD	0
	DD	3162800062
	DD	1102361128
	DD	1067576117
	DD	0
	DD	1071644672
	DD	1477771776
	DD	1072367882
	DD	536870912
	DD	3163104986
	DD	2880516564
	DD	1067003401
	DD	0
	DD	1071644672
	DD	1056266002
	DD	1072381517
	DD	2684354560
	DD	3163469496
	DD	1220541286
	DD	1066197478
	DD	0
	DD	1071644672
	DD	1159294526
	DD	1072394883
	DD	536870912
	DD	3163276959
	DD	1279178457
	DD	1064107143
	DD	0
	DD	1071644672
	DD	1731563730
	DD	1072407978
	DD	536870912
	DD	1015540302
	DD	31019393
	DD	3212646089
	DD	0
	DD	1071644672
	DD	2892993834
	DD	1072420800
	DD	536870912
	DD	3163039143
	DD	3506991783
	DD	3214032490
	DD	0
	DD	1071644672
	DD	645027145
	DD	1072433348
	DD	3221225472
	DD	1012602239
	DD	2140183630
	DD	3214756396
	DD	0
	DD	1071644672
	DD	4051746225
	DD	1072445618
	DD	1610612736
	DD	3161907377
	DD	600368053
	DD	3215209541
	DD	0
	DD	1071644672
	DD	881357723
	DD	1072457611
	DD	3221225472
	DD	1013810890
	DD	913431823
	DD	3215576203
	DD	0
	DD	1071644672
	DD	557060597
	DD	1072469323
	DD	0
	DD	3159554934
	DD	1073154251
	DD	3215945137
	DD	0
	DD	1071644672
	DD	4093443164
	DD	1072480752
	DD	536870912
	DD	1014912865
	DD	3631910143
	DD	3216149439
	DD	0
	DD	1071644672
	DD	4097623923
	DD	1072491898
	DD	2684354560
	DD	3160308587
	DD	1380647130
	DD	3216336095
	DD	0
	DD	1071644672
	DD	1950234076
	DD	1072502759
	DD	536870912
	DD	3159425498
	DD	4049357271
	DD	3216523802
	DD	0
	DD	1071644672
	DD	3511535930
	DD	1072513332
	DD	2147483648
	DD	3162828626
	DD	1896722595
	DD	3216712534
	DD	0
	DD	1071644672
	DD	1942611595
	DD	1072523617
	DD	3221225472
	DD	3162410081
	DD	1699043957
	DD	3216902261
	DD	0
	DD	1071644672
	DD	3476196678
	DD	1072533611
	DD	0
	DD	1014257638
	DD	3296146332
	DD	1069485732
	DD	0
	DD	1070596096
	DD	1647878299
	DD	1072543314
	DD	3758096384
	DD	3162169063
	DD	3286832763
	DD	1069294100
	DD	0
	DD	1070596096
	DD	3066872380
	DD	1072552723
	DD	3221225472
	DD	1015561271
	DD	1962712856
	DD	1069101559
	DD	0
	DD	1070596096
	DD	1647164971
	DD	1072561838
	DD	2147483648
	DD	3162838250
	DD	3601629044
	DD	1068908137
	DD	0
	DD	1070596096
	DD	83265253
	DD	1072570657
	DD	1610612736
	DD	3162510535
	DD	165911977
	DD	1068713865
	DD	0
	DD	1070596096
	DD	1261158782
	DD	1072579178
	DD	2147483648
	DD	1015711544
	DD	1348311045
	DD	1068518770
	DD	0
	DD	1070596096
	DD	3964199186
	DD	1072587400
	DD	3758096384
	DD	3160501078
	DD	386665102
	DD	1068146822
	DD	0
	DD	1070596096
	DD	2873938189
	DD	1072595323
	DD	1610612736
	DD	3162311422
	DD	1991047213
	DD	1067753521
	DD	0
	DD	1070596096
	DD	1455828442
	DD	1072602945
	DD	3758096384
	DD	1015505073
	DD	4107121629
	DD	1067267139
	DD	0
	DD	1070596096
	DD	3370060660
	DD	1072610264
	DD	1610612736
	DD	1014787212
	DD	723478390
	DD	1066474790
	DD	0
	DD	1070596096
	DD	3882371427
	DD	1072617280
	DD	2147483648
	DD	1014923975
	DD	2382326656
	DD	1064562179
	DD	0
	DD	1070596096
	DD	2749658050
	DD	1072623992
	DD	1610612736
	DD	1015427338
	DD	2614369450
	DD	3212624080
	DD	0
	DD	1070596096
	DD	4220661975
	DD	1072630398
	DD	3221225472
	DD	1014312058
	DD	3587565429
	DD	3214108032
	DD	0
	DD	1070596096
	DD	4151720666
	DD	1072636498
	DD	0
	DD	3162639544
	DD	2886567144
	DD	3214910444
	DD	0
	DD	1070596096
	DD	2892293513
	DD	1072642291
	DD	2684354560
	DD	3162340271
	DD	1765797566
	DD	3215324543
	DD	0
	DD	1070596096
	DD	990588717
	DD	1072647776
	DD	3758096384
	DD	3162041857
	DD	240740309
	DD	3215727903
	DD	0
	DD	1070596096
	DD	3489094832
	DD	1072652951
	DD	536870912
	DD	1014325783
	DD	2034433221
	DD	1068349287
	DD	0
	DD	1069547520
	DD	2745245815
	DD	1072657817
	DD	3221225472
	DD	1015566129
	DD	304938777
	DD	1067944017
	DD	0
	DD	1069547520
	DD	4201728937
	DD	1072662372
	DD	1610612736
	DD	1014674006
	DD	3299146087
	DD	1067537882
	DD	0
	DD	1069547520
	DD	617154971
	DD	1072666617
	DD	3758096384
	DD	3163433696
	DD	1953696209
	DD	1066811523
	DD	0
	DD	1069547520
	DD	2131272983
	DD	1072670549
	DD	3221225472
	DD	3163005096
	DD	3987691371
	DD	1065590540
	DD	0
	DD	1069547520
	DD	1905645377
	DD	1072674169
	DD	1610612736
	DD	1014087596
	DD	718380601
	DD	3211081269
	DD	0
	DD	1069547520
	DD	1893834707
	DD	1072677476
	DD	1610612736
	DD	1015147654
	DD	3272206680
	DD	3213771729
	DD	0
	DD	1069547520
	DD	4251821691
	DD	1072680469
	DD	2147483648
	DD	3161841007
	DD	257503056
	DD	3214647653
	DD	0
	DD	1069547520
	DD	2748392742
	DD	1072683149
	DD	536870912
	DD	3163061750
	DD	711626851
	DD	1066916672
	DD	0
	DD	1068498944
	DD	4240268041
	DD	1072685514
	DD	536870912
	DD	1013588963
	DD	2523975598
	DD	1065789700
	DD	0
	DD	1068498944
	DD	2902558778
	DD	1072687565
	DD	3221225472
	DD	1013260780
	DD	3237733378
	DD	3209908784
	DD	0
	DD	1068498944
	DD	1703834029
	DD	1072689301
	DD	3758096384
	DD	1015028364
	DD	1003003824
	DD	3213591144
	DD	0
	DD	1068498944
	DD	3816385918
	DD	1072690721
	DD	536870912
	DD	3161216956
	DD	2896342633
	DD	1064747819
	DD	0
	DD	1067450368
	DD	4026464093
	DD	1072691826
	DD	3221225472
	DD	3163075789
	DD	561145352
	DD	3212540584
	DD	0
	DD	1067450368
	DD	1619315981
	DD	1072692616
	DD	1073741824
	DD	3162584397
	DD	210035182
	DD	3211491512
	DD	0
	DD	1066401792
	DD	379294427
	DD	1072693090
	DD	3221225472
	DD	1015405987
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	210035182
	DD	1064007864
	DD	0
	DD	3213885440
	DD	379294427
	DD	1072693090
	DD	3221225472
	DD	1015405987
	DD	561145352
	DD	1065056936
	DD	0
	DD	3214934016
	DD	1619315981
	DD	1072692616
	DD	1073741824
	DD	3162584397
	DD	2896342633
	DD	3212231467
	DD	0
	DD	3214934016
	DD	4026464093
	DD	1072691826
	DD	3221225472
	DD	3163075789
	DD	1003003824
	DD	1066107496
	DD	0
	DD	3215982592
	DD	3816385918
	DD	1072690721
	DD	536870912
	DD	3161216956
	DD	3237733378
	DD	1062425136
	DD	0
	DD	3215982592
	DD	1703834029
	DD	1072689301
	DD	3758096384
	DD	1015028364
	DD	2523975598
	DD	3213273348
	DD	0
	DD	3215982592
	DD	2902558778
	DD	1072687565
	DD	3221225472
	DD	1013260780
	DD	711626851
	DD	3214400320
	DD	0
	DD	3215982592
	DD	4240268041
	DD	1072685514
	DD	536870912
	DD	1013588963
	DD	257503056
	DD	1067164005
	DD	0
	DD	3217031168
	DD	2748392742
	DD	1072683149
	DD	536870912
	DD	3163061750
	DD	3272206680
	DD	1066288081
	DD	0
	DD	3217031168
	DD	4251821691
	DD	1072680469
	DD	2147483648
	DD	3161841007
	DD	718380601
	DD	1063597621
	DD	0
	DD	3217031168
	DD	1893834707
	DD	1072677476
	DD	1610612736
	DD	1015147654
	DD	3987691371
	DD	3213074188
	DD	0
	DD	3217031168
	DD	1905645377
	DD	1072674169
	DD	1610612736
	DD	1014087596
	DD	1953696209
	DD	3214295171
	DD	0
	DD	3217031168
	DD	2131272983
	DD	1072670549
	DD	3221225472
	DD	3163005096
	DD	3299146087
	DD	3215021530
	DD	0
	DD	3217031168
	DD	617154971
	DD	1072666617
	DD	3758096384
	DD	3163433696
	DD	304938777
	DD	3215427665
	DD	0
	DD	3217031168
	DD	4201728937
	DD	1072662372
	DD	1610612736
	DD	1014674006
	DD	2034433221
	DD	3215832935
	DD	0
	DD	3217031168
	DD	2745245815
	DD	1072657817
	DD	3221225472
	DD	1015566129
	DD	240740309
	DD	1068244255
	DD	0
	DD	3218079744
	DD	3489094832
	DD	1072652951
	DD	536870912
	DD	1014325783
	DD	1765797566
	DD	1067840895
	DD	0
	DD	3218079744
	DD	990588717
	DD	1072647776
	DD	3758096384
	DD	3162041857
	DD	2886567144
	DD	1067426796
	DD	0
	DD	3218079744
	DD	2892293513
	DD	1072642291
	DD	2684354560
	DD	3162340271
	DD	3587565429
	DD	1066624384
	DD	0
	DD	3218079744
	DD	4151720666
	DD	1072636498
	DD	0
	DD	3162639544
	DD	2614369450
	DD	1065140432
	DD	0
	DD	3218079744
	DD	4220661975
	DD	1072630398
	DD	3221225472
	DD	1014312058
	DD	2382326656
	DD	3212045827
	DD	0
	DD	3218079744
	DD	2749658050
	DD	1072623992
	DD	1610612736
	DD	1015427338
	DD	723478390
	DD	3213958438
	DD	0
	DD	3218079744
	DD	3882371427
	DD	1072617280
	DD	2147483648
	DD	1014923975
	DD	4107121629
	DD	3214750787
	DD	0
	DD	3218079744
	DD	3370060660
	DD	1072610264
	DD	1610612736
	DD	1014787212
	DD	1991047213
	DD	3215237169
	DD	0
	DD	3218079744
	DD	1455828442
	DD	1072602945
	DD	3758096384
	DD	1015505073
	DD	386665102
	DD	3215630470
	DD	0
	DD	3218079744
	DD	2873938189
	DD	1072595323
	DD	1610612736
	DD	3162311422
	DD	1348311045
	DD	3216002418
	DD	0
	DD	3218079744
	DD	3964199186
	DD	1072587400
	DD	3758096384
	DD	3160501078
	DD	165911977
	DD	3216197513
	DD	0
	DD	3218079744
	DD	1261158782
	DD	1072579178
	DD	2147483648
	DD	1015711544
	DD	3601629044
	DD	3216391785
	DD	0
	DD	3218079744
	DD	83265253
	DD	1072570657
	DD	1610612736
	DD	3162510535
	DD	1962712856
	DD	3216585207
	DD	0
	DD	3218079744
	DD	1647164971
	DD	1072561838
	DD	2147483648
	DD	3162838250
	DD	3286832763
	DD	3216777748
	DD	0
	DD	3218079744
	DD	3066872380
	DD	1072552723
	DD	3221225472
	DD	1015561271
	DD	3296146332
	DD	3216969380
	DD	0
	DD	3218079744
	DD	1647878299
	DD	1072543314
	DD	3758096384
	DD	3162169063
	DD	1699043957
	DD	1069418613
	DD	0
	DD	3219128320
	DD	3476196678
	DD	1072533611
	DD	0
	DD	1014257638
	DD	1896722595
	DD	1069228886
	DD	0
	DD	3219128320
	DD	1942611595
	DD	1072523617
	DD	3221225472
	DD	3162410081
	DD	4049357271
	DD	1069040154
	DD	0
	DD	3219128320
	DD	3511535930
	DD	1072513332
	DD	2147483648
	DD	3162828626
	DD	1380647130
	DD	1068852447
	DD	0
	DD	3219128320
	DD	1950234076
	DD	1072502759
	DD	536870912
	DD	3159425498
	DD	3631910143
	DD	1068665791
	DD	0
	DD	3219128320
	DD	4097623923
	DD	1072491898
	DD	2684354560
	DD	3160308587
	DD	1073154251
	DD	1068461489
	DD	0
	DD	3219128320
	DD	4093443164
	DD	1072480752
	DD	536870912
	DD	1014912865
	DD	913431823
	DD	1068092555
	DD	0
	DD	3219128320
	DD	557060597
	DD	1072469323
	DD	0
	DD	3159554934
	DD	600368053
	DD	1067725893
	DD	0
	DD	3219128320
	DD	881357723
	DD	1072457611
	DD	3221225472
	DD	1013810890
	DD	2140183630
	DD	1067272748
	DD	0
	DD	3219128320
	DD	4051746225
	DD	1072445618
	DD	1610612736
	DD	3161907377
	DD	3506991783
	DD	1066548842
	DD	0
	DD	3219128320
	DD	645027145
	DD	1072433348
	DD	3221225472
	DD	1012602239
	DD	31019393
	DD	1065162441
	DD	0
	DD	3219128320
	DD	2892993834
	DD	1072420800
	DD	536870912
	DD	3163039143
	DD	1279178457
	DD	3211590791
	DD	0
	DD	3219128320
	DD	1731563730
	DD	1072407978
	DD	536870912
	DD	1015540302
	DD	1220541286
	DD	3213681126
	DD	0
	DD	3219128320
	DD	1159294526
	DD	1072394883
	DD	536870912
	DD	3163276959
	DD	2880516564
	DD	3214487049
	DD	0
	DD	3219128320
	DD	1056266002
	DD	1072381517
	DD	2684354560
	DD	3163469496
	DD	1102361128
	DD	3215059765
	DD	0
	DD	3219128320
	DD	1477771776
	DD	1072367882
	DD	536870912
	DD	3163104986
	DD	1038988426
	DD	3215406294
	DD	0
	DD	3219128320
	DD	2653017361
	DD	1072353980
	DD	0
	DD	3162800062
	DD	2598800519
	DD	3215750067
	DD	0
	DD	3219128320
	DD	688824739
	DD	1072339814
	DD	3758096384
	DD	1010431536
	DD	3382663878
	DD	3216036812
	DD	0
	DD	3219128320
	DD	453180130
	DD	1072325385
	DD	1073741824
	DD	3162411281
	DD	1726519029
	DD	3216205866
	DD	0
	DD	3219128320
	DD	2688952194
	DD	1072310695
	DD	0
	DD	3161532603
	DD	802660176
	DD	3216373465
	DD	0
	DD	3219128320
	DD	4012486548
	DD	1072295747
	DD	2684354560
	DD	1014701564
	DD	3876290983
	DD	3216539583
	DD	0
	DD	3219128320
	DD	1207142209
	DD	1072280544
	DD	2147483648
	DD	3162705634
	DD	2285031318
	DD	3216704197
	DD	0
	DD	3219128320
	DD	4106737474
	DD	1072265086
	DD	2684354560
	DD	3162592377
	DD	1224902090
	DD	3216867281
	DD	0
	DD	3219128320
	DD	1234330619
	DD	1072249378
	DD	2684354560
	DD	1015272473
	DD	2586490530
	DD	3217028810
	DD	0
	DD	3219128320
	DD	2750387213
	DD	1072233420
	DD	2147483648
	DD	3160858537
	DD	2485417816
	DD	3217109964
	DD	0
	DD	3219128320
	DD	1796544321
	DD	1072217216
	DD	536870912
	DD	3162686945
	DD	1852581091
	DD	3217189138
	DD	0
	DD	3219128320
	DD	263859903
	DD	1072200768
	DD	0
	DD	3162692284
	DD	4019165092
	DD	3217267498
	DD	0
	DD	3219128320
	DD	201299822
	DD	1072184078
	DD	3758096384
	DD	3161912352
	DD	1250776663
	DD	3217345034
	DD	0
	DD	3219128320
	DD	3814135665
	DD	1072167148
	DD	3221225472
	DD	3163146456
	DD	3526603391
	DD	3217421732
	DD	0
	DD	3219128320
	DD	577417135
	DD	1072149983
	DD	2684354560
	DD	3161519415
	DD	4187227697
	DD	3217497582
	DD	0
	DD	3219128320
	DD	1594061409
	DD	1072132583
	DD	2147483648
	DD	1014353870
	DD	1416934267
	DD	3217572573
	DD	0
	DD	3219128320
	DD	938475414
	DD	1072114952
	DD	0
	DD	1015076079
	DD	2546065654
	DD	3217646692
	DD	0
	DD	3219128320
	DD	1424664751
	DD	1072097092
	DD	3221225472
	DD	3163405315
	DD	2583490354
	DD	3217719929
	DD	0
	DD	3219128320
	DD	1719614413
	DD	1072079006
	DD	0
	DD	3163282740
	DD	1403691706
	DD	3217792273
	DD	0
	DD	3219128320
	DD	636516413
	DD	1072060697
	DD	3758096384
	DD	1015823716
	DD	3753901657
	DD	3217863712
	DD	0
	DD	3219128320
	DD	1427975391
	DD	1072042167
	DD	1073741824
	DD	3161608627
	DD	2081278341
	DD	3217934237
	DD	0
	DD	3219128320
	DD	3194290572
	DD	1072023419
	DD	2147483648
	DD	1015048682
	DD	2309668539
	DD	3218003836
	DD	0
	DD	3219128320
	DD	881650848
	DD	1072004457
	DD	2147483648
	DD	3162986272
	DD	2371640770
	DD	3218072499
	DD	0
	DD	3219128320
	DD	2460177814
	DD	1071985282
	DD	2147483648
	DD	3162116843
	DD	3489768009
	DD	1070535623
	DD	0
	DD	3220176896
	DD	3152274921
	DD	1071965898
	DD	3758096384
	DD	1013170835
	DD	3239527685
	DD	1070468863
	DD	0
	DD	3220176896
	DD	2610628921
	DD	1071946308
	DD	1610612736
	DD	3162668769
	DD	1403757309
	DD	1070403070
	DD	0
	DD	3220176896
	DD	621354454
	DD	1071926515
	DD	536870912
	DD	1013450602
	DD	1883353036
	DD	1070338253
	DD	0
	DD	3220176896
	DD	1397053140
	DD	1071906521
	DD	1610612736
	DD	1015245078
	DD	3652612115
	DD	1070274422
	DD	0
	DD	3220176896
	DD	689983673
	DD	1071886330
	DD	3758096384
	DD	1014906405
	DD	752919289
	DD	1070211588
	DD	0
	DD	3220176896
	DD	2969983475
	DD	1071865944
	DD	2684354560
	DD	3162635443
	DD	3761365757
	DD	1070149758
	DD	0
	DD	3220176896
	DD	4242632757
	DD	1071845367
	DD	536870912
	DD	3161390278
	DD	1129922991
	DD	1070088944
	DD	0
	DD	3220176896
	DD	637203434
	DD	1071824603
	DD	3221225472
	DD	3159443841
	DD	2129093113
	DD	1070029153
	DD	0
	DD	3220176896
	DD	1289556703
	DD	1071803653
	DD	536870912
	DD	3162888820
	DD	2482246777
	DD	1069970395
	DD	0
	DD	3220176896
	DD	2570316633
	DD	1071782521
	DD	1610612736
	DD	3162744753
	DD	1539668340
	DD	1069912679
	DD	0
	DD	3220176896
	DD	967731400
	DD	1071761211
	DD	536870912
	DD	1015752157
	DD	2272832445
	DD	1069856013
	DD	0
	DD	3220176896
	DD	1675549513
	DD	1071739725
	DD	3221225472
	DD	3163025138
	DD	2678847490
	DD	1069800406
	DD	0
	DD	3220176896
	DD	1411074851
	DD	1071718067
	DD	1610612736
	DD	3163484731
	DD	69903290
	DD	1069745867
	DD	0
	DD	3220176896
	DD	1297975695
	DD	1071696240
	DD	3221225472
	DD	3163083735
	DD	3952756490
	DD	1069692402
	DD	0
	DD	3220176896
	DD	2569207790
	DD	1071674247
	DD	1073741824
	DD	3163183617
	DD	1663679671
	DD	1069640022
	DD	0
	DD	3220176896
	DD	2269920951
	DD	1071652092
	DD	3758096384
	DD	3161672245
	DD	1312926554
	DD	1069588733
	DD	0
	DD	3220176896
	DD	3690502842
	DD	1071614884
	DD	3221225472
	DD	3157453855
	DD	3429717432
	DD	1069529566
	DD	0
	DD	3220176896
	DD	1391425750
	DD	1071569945
	DD	3221225472
	DD	3162362103
	DD	1945768569
	DD	1069431400
	DD	0
	DD	3220176896
	DD	939980347
	DD	1071524701
	DD	0
	DD	1012796809
	DD	1539072807
	DD	1069335462
	DD	0
	DD	3220176896
	DD	1535566729
	DD	1071479159
	DD	2147483648
	DD	1014321388
	DD	4133449816
	DD	1069241766
	DD	0
	DD	3220176896
	DD	2570175582
	DD	1071433326
	DD	1073741824
	DD	1012303118
	DD	1612193054
	DD	1069150328
	DD	0
	DD	3220176896
	DD	3623952103
	DD	1071387209
	DD	2684354560
	DD	3161985962
	DD	1578746984
	DD	1069061160
	DD	0
	DD	3220176896
	DD	165764288
	DD	1071340816
	DD	3221225472
	DD	1013298018
	DD	1577996576
	DD	1068974276
	DD	0
	DD	3220176896
	DD	728580042
	DD	1071294152
	DD	1610612736
	DD	3162332944
	DD	1972483635
	DD	1068889689
	DD	0
	DD	3220176896
	DD	1135144330
	DD	1071247225
	DD	536870912
	DD	1013373569
	DD	1638976426
	DD	1068807412
	DD	0
	DD	3220176896
	DD	1673302999
	DD	1071200042
	DD	2147483648
	DD	1013351473
	DD	2255197647
	DD	1068727457
	DD	0
	DD	3220176896
	DD	2796464483
	DD	1071152610
	DD	3221225472
	DD	3160878317
	DD	3996842794
	DD	1068649836
	DD	0
	DD	3220176896
	DD	824036583
	DD	1071104937
	DD	0
	DD	1008313330
	DD	1234824733
	DD	1068574562
	DD	0
	DD	3220176896
	DD	821708191
	DD	1071057029
	DD	3758096384
	DD	3161738009
	DD	4002549419
	DD	1068501644
	DD	0
	DD	3220176896
	DD	3711903686
	DD	1071008893
	DD	1073741824
	DD	1011944747
	DD	2962723252
	DD	1068363247
	DD	0
	DD	3220176896
	DD	1974149085
	DD	1070960538
	DD	3221225472
	DD	3156147648
	DD	1296295961
	DD	1068226907
	DD	0
	DD	3220176896
	DD	1115219818
	DD	1070911970
	DD	0
	DD	1013072242
	DD	997322465
	DD	1068095345
	DD	0
	DD	3220176896
	DD	2484561409
	DD	1070863196
	DD	1073741824
	DD	1014024572
	DD	1261629236
	DD	1067968581
	DD	0
	DD	3220176896
	DD	3269558610
	DD	1070814224
	DD	536870912
	DD	3161705216
	DD	2476548698
	DD	1067846634
	DD	0
	DD	3220176896
	DD	785751814
	DD	1070765062
	DD	2684354560
	DD	3161838221
	DD	1913604284
	DD	1067729523
	DD	0
	DD	3220176896
	DD	1062001470
	DD	1070715716
	DD	0
	DD	3160875220
	DD	2306566604
	DD	1067617265
	DD	0
	DD	3220176896
	DD	1655830135
	DD	1070666194
	DD	0
	DD	1014506690
	DD	3250111450
	DD	1067509877
	DD	0
	DD	3220176896
	DD	238550446
	DD	1070616504
	DD	2684354560
	DD	3154383565
	DD	2377770267
	DD	1067364384
	DD	0
	DD	3220176896
	DD	2065784603
	DD	1070537209
	DD	3758096384
	DD	3160681966
	DD	288924873
	DD	1067169185
	DD	0
	DD	3220176896
	DD	3846521617
	DD	1070437199
	DD	536870912
	DD	1013406610
	DD	1935888103
	DD	1066983818
	DD	0
	DD	3220176896
	DD	1786662755
	DD	1070336898
	DD	2147483648
	DD	3155882307
	DD	2660899430
	DD	1066808312
	DD	0
	DD	3220176896
	DD	632292433
	DD	1070236320
	DD	3758096384
	DD	3160963333
	DD	18115067
	DD	1066642694
	DD	0
	DD	3220176896
	DD	1013556747
	DD	1070135480
	DD	2684354560
	DD	3160567065
	DD	2346447124
	DD	1066486987
	DD	0
	DD	3220176896
	DD	3729833777
	DD	1070034393
	DD	3758096384
	DD	1013044718
	DD	1672287667
	DD	1066280641
	DD	0
	DD	3220176896
	DD	1149976518
	DD	1069933076
	DD	1610612736
	DD	1013264895
	DD	3451754846
	DD	1066009014
	DD	0
	DD	3220176896
	DD	2972270170
	DD	1069831542
	DD	1073741824
	DD	3159524770
	DD	1037482584
	DD	1065757344
	DD	0
	DD	3220176896
	DD	1854824762
	DD	1069729808
	DD	2684354560
	DD	1011953664
	DD	2592747447
	DD	1065525667
	DD	0
	DD	3220176896
	DD	3470456183
	DD	1069627888
	DD	1073741824
	DD	3160744771
	DD	2402293340
	DD	1065274823
	DD	0
	DD	3220176896
	DD	1453945614
	DD	1069504078
	DD	1610612736
	DD	1011492612
	DD	1227670166
	DD	1064891649
	DD	0
	DD	3220176896
	DD	3349070549
	DD	1069299589
	DD	3758096384
	DD	3156538269
	DD	393047345
	DD	1064548654
	DD	0
	DD	3220176896
	DD	3156849708
	DD	1069094822
	DD	3221225472
	DD	3158189848
	DD	1118107366
	DD	1064187139
	DD	0
	DD	3220176896
	DD	177906713
	DD	1068889808
	DD	3221225472
	DD	3159310370
	DD	4269262315
	DD	1063662162
	DD	0
	DD	3220176896
	DD	2462980598
	DD	1068684576
	DD	2684354560
	DD	3158941832
	DD	3810305407
	DD	1063179572
	DD	0
	DD	3220176896
	DD	2046304480
	DD	1068459374
	DD	3758096384
	DD	3156402667
	DD	220529721
	DD	1062452281
	DD	0
	DD	3220176896
	DD	4044216340
	DD	1068048229
	DD	0
	DD	3156808381
	DD	69373323
	DD	1061565504
	DD	0
	DD	3220176896
	DD	1972655565
	DD	1067636837
	DD	1073741824
	DD	1009157878
	DD	3650591271
	DD	1060355323
	DD	0
	DD	3220176896
	DD	4154680958
	DD	1067000149
	DD	0
	DD	3153796451
	DD	2376373521
	DD	1058258220
	DD	0
	DD	3220176896
	DD	4242458500
	DD	1065951697
	DD	2684354560
	DD	1009354638
	DD	0
	DD	0
	DD	0
	DD	3220176896
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2376373521
	DD	1058258220
	DD	0
	DD	3220176896
	DD	4242458500
	DD	3213435345
	DD	2684354560
	DD	3156838286
	DD	3650591271
	DD	1060355323
	DD	0
	DD	3220176896
	DD	4154680958
	DD	3214483797
	DD	0
	DD	1006312803
	DD	69373323
	DD	1061565504
	DD	0
	DD	3220176896
	DD	1972655565
	DD	3215120485
	DD	1073741824
	DD	3156641526
	DD	220529721
	DD	1062452281
	DD	0
	DD	3220176896
	DD	4044216340
	DD	3215531877
	DD	0
	DD	1009324733
	DD	3810305407
	DD	1063179572
	DD	0
	DD	3220176896
	DD	2046304480
	DD	3215943022
	DD	3758096384
	DD	1008919019
	DD	4269262315
	DD	1063662162
	DD	0
	DD	3220176896
	DD	2462980598
	DD	3216168224
	DD	2684354560
	DD	1011458184
	DD	1118107366
	DD	1064187139
	DD	0
	DD	3220176896
	DD	177906713
	DD	3216373456
	DD	3221225472
	DD	1011826722
	DD	393047345
	DD	1064548654
	DD	0
	DD	3220176896
	DD	3156849708
	DD	3216578470
	DD	3221225472
	DD	1010706200
	DD	1227670166
	DD	1064891649
	DD	0
	DD	3220176896
	DD	3349070549
	DD	3216783237
	DD	3758096384
	DD	1009054621
	DD	2402293340
	DD	1065274823
	DD	0
	DD	3220176896
	DD	1453945614
	DD	3216987726
	DD	1610612736
	DD	3158976260
	DD	2592747447
	DD	1065525667
	DD	0
	DD	3220176896
	DD	3470456183
	DD	3217111536
	DD	1073741824
	DD	1013261123
	DD	1037482584
	DD	1065757344
	DD	0
	DD	3220176896
	DD	1854824762
	DD	3217213456
	DD	2684354560
	DD	3159437312
	DD	3451754846
	DD	1066009014
	DD	0
	DD	3220176896
	DD	2972270170
	DD	3217315190
	DD	1073741824
	DD	1012041122
	DD	1672287667
	DD	1066280641
	DD	0
	DD	3220176896
	DD	1149976518
	DD	3217416724
	DD	1610612736
	DD	3160748543
	DD	2346447124
	DD	1066486987
	DD	0
	DD	3220176896
	DD	3729833777
	DD	3217518041
	DD	3758096384
	DD	3160528366
	DD	18115067
	DD	1066642694
	DD	0
	DD	3220176896
	DD	1013556747
	DD	3217619128
	DD	2684354560
	DD	1013083417
	DD	2660899430
	DD	1066808312
	DD	0
	DD	3220176896
	DD	632292433
	DD	3217719968
	DD	3758096384
	DD	1013479685
	DD	1935888103
	DD	1066983818
	DD	0
	DD	3220176896
	DD	1786662755
	DD	3217820546
	DD	2147483648
	DD	1008398659
	DD	288924873
	DD	1067169185
	DD	0
	DD	3220176896
	DD	3846521617
	DD	3217920847
	DD	536870912
	DD	3160890258
	DD	2377770267
	DD	1067364384
	DD	0
	DD	3220176896
	DD	2065784603
	DD	3218020857
	DD	3758096384
	DD	1013198318
	DD	3250111450
	DD	1067509877
	DD	0
	DD	3220176896
	DD	238550446
	DD	3218100152
	DD	2684354560
	DD	1006899917
	DD	2306566604
	DD	1067617265
	DD	0
	DD	3220176896
	DD	1655830135
	DD	3218149842
	DD	0
	DD	3161990338
	DD	1913604284
	DD	1067729523
	DD	0
	DD	3220176896
	DD	1062001470
	DD	3218199364
	DD	0
	DD	1013391572
	DD	2476548698
	DD	1067846634
	DD	0
	DD	3220176896
	DD	785751814
	DD	3218248710
	DD	2684354560
	DD	1014354573
	DD	1261629236
	DD	1067968581
	DD	0
	DD	3220176896
	DD	3269558610
	DD	3218297872
	DD	536870912
	DD	1014221568
	DD	997322465
	DD	1068095345
	DD	0
	DD	3220176896
	DD	2484561409
	DD	3218346844
	DD	1073741824
	DD	3161508220
	DD	1296295961
	DD	1068226907
	DD	0
	DD	3220176896
	DD	1115219818
	DD	3218395618
	DD	0
	DD	3160555890
	DD	2962723252
	DD	1068363247
	DD	0
	DD	3220176896
	DD	1974149085
	DD	3218444186
	DD	3221225472
	DD	1008664000
	DD	4002549419
	DD	1068501644
	DD	0
	DD	3220176896
	DD	3711903686
	DD	3218492541
	DD	1073741824
	DD	3159428395
	DD	1234824733
	DD	1068574562
	DD	0
	DD	3220176896
	DD	821708191
	DD	3218540677
	DD	3758096384
	DD	1014254361
	DD	3996842794
	DD	1068649836
	DD	0
	DD	3220176896
	DD	824036583
	DD	3218588585
	DD	0
	DD	3155796978
	DD	2255197647
	DD	1068727457
	DD	0
	DD	3220176896
	DD	2796464483
	DD	3218636258
	DD	3221225472
	DD	1013394669
	DD	1638976426
	DD	1068807412
	DD	0
	DD	3220176896
	DD	1673302999
	DD	3218683690
	DD	2147483648
	DD	3160835121
	DD	1972483635
	DD	1068889689
	DD	0
	DD	3220176896
	DD	1135144330
	DD	3218730873
	DD	536870912
	DD	3160857217
	DD	1577996576
	DD	1068974276
	DD	0
	DD	3220176896
	DD	728580042
	DD	3218777800
	DD	1610612736
	DD	1014849296
	DD	1578746984
	DD	1069061160
	DD	0
	DD	3220176896
	DD	165764288
	DD	3218824464
	DD	3221225472
	DD	3160781666
	DD	1612193054
	DD	1069150328
	DD	0
	DD	3220176896
	DD	3623952103
	DD	3218870857
	DD	2684354560
	DD	1014502314
	DD	4133449816
	DD	1069241766
	DD	0
	DD	3220176896
	DD	2570175582
	DD	3218916974
	DD	1073741824
	DD	3159786766
	DD	1539072807
	DD	1069335462
	DD	0
	DD	3220176896
	DD	1535566729
	DD	3218962807
	DD	2147483648
	DD	3161805036
	DD	1945768569
	DD	1069431400
	DD	0
	DD	3220176896
	DD	939980347
	DD	3219008349
	DD	0
	DD	3160280457
	DD	3429717432
	DD	1069529566
	DD	0
	DD	3220176896
	DD	1391425750
	DD	3219053593
	DD	3221225472
	DD	1014878455
	DD	1312926554
	DD	1069588733
	DD	0
	DD	3220176896
	DD	3690502842
	DD	3219098532
	DD	3221225472
	DD	1009970207
	DD	1663679671
	DD	1069640022
	DD	0
	DD	3220176896
	DD	2269920951
	DD	3219135740
	DD	3758096384
	DD	1014188597
	DD	3952756490
	DD	1069692402
	DD	0
	DD	3220176896
	DD	2569207790
	DD	3219157895
	DD	1073741824
	DD	1015699969
	DD	69903290
	DD	1069745867
	DD	0
	DD	3220176896
	DD	1297975695
	DD	3219179888
	DD	3221225472
	DD	1015600087
	DD	2678847490
	DD	1069800406
	DD	0
	DD	3220176896
	DD	1411074851
	DD	3219201715
	DD	1610612736
	DD	1016001083
	DD	2272832445
	DD	1069856013
	DD	0
	DD	3220176896
	DD	1675549513
	DD	3219223373
	DD	3221225472
	DD	1015541490
	DD	1539668340
	DD	1069912679
	DD	0
	DD	3220176896
	DD	967731400
	DD	3219244859
	DD	536870912
	DD	3163235805
	DD	2482246777
	DD	1069970395
	DD	0
	DD	3220176896
	DD	2570316633
	DD	3219266169
	DD	1610612736
	DD	1015261105
	DD	2129093113
	DD	1070029153
	DD	0
	DD	3220176896
	DD	1289556703
	DD	3219287301
	DD	536870912
	DD	1015405172
	DD	1129922991
	DD	1070088944
	DD	0
	DD	3220176896
	DD	637203434
	DD	3219308251
	DD	3221225472
	DD	1011960193
	DD	3761365757
	DD	1070149758
	DD	0
	DD	3220176896
	DD	4242632757
	DD	3219329015
	DD	536870912
	DD	1013906630
	DD	752919289
	DD	1070211588
	DD	0
	DD	3220176896
	DD	2969983475
	DD	3219349592
	DD	2684354560
	DD	1015151795
	DD	3652612115
	DD	1070274422
	DD	0
	DD	3220176896
	DD	689983673
	DD	3219369978
	DD	3758096384
	DD	3162390053
	DD	1883353036
	DD	1070338253
	DD	0
	DD	3220176896
	DD	1397053140
	DD	3219390169
	DD	1610612736
	DD	3162728726
	DD	1403757309
	DD	1070403070
	DD	0
	DD	3220176896
	DD	621354454
	DD	3219410163
	DD	536870912
	DD	3160934250
	DD	3239527685
	DD	1070468863
	DD	0
	DD	3220176896
	DD	2610628921
	DD	3219429956
	DD	1610612736
	DD	1015185121
	DD	3489768009
	DD	1070535623
	DD	0
	DD	3220176896
	DD	3152274921
	DD	3219449546
	DD	3758096384
	DD	3160654483
	DD	2371640770
	DD	3218072499
	DD	0
	DD	3219128320
	DD	2460177814
	DD	3219468930
	DD	2147483648
	DD	1014633195
	DD	2309668539
	DD	3218003836
	DD	0
	DD	3219128320
	DD	881650848
	DD	3219488105
	DD	2147483648
	DD	1015502624
	DD	2081278341
	DD	3217934237
	DD	0
	DD	3219128320
	DD	3194290572
	DD	3219507067
	DD	2147483648
	DD	3162532330
	DD	3753901657
	DD	3217863712
	DD	0
	DD	3219128320
	DD	1427975391
	DD	3219525815
	DD	1073741824
	DD	1014124979
	DD	1403691706
	DD	3217792273
	DD	0
	DD	3219128320
	DD	636516413
	DD	3219544345
	DD	3758096384
	DD	3163307364
	DD	2583490354
	DD	3217719929
	DD	0
	DD	3219128320
	DD	1719614413
	DD	3219562654
	DD	0
	DD	1015799092
	DD	2546065654
	DD	3217646692
	DD	0
	DD	3219128320
	DD	1424664751
	DD	3219580740
	DD	3221225472
	DD	1015921667
	DD	1416934267
	DD	3217572573
	DD	0
	DD	3219128320
	DD	938475414
	DD	3219598600
	DD	0
	DD	3162559727
	DD	4187227697
	DD	3217497582
	DD	0
	DD	3219128320
	DD	1594061409
	DD	3219616231
	DD	2147483648
	DD	3161837518
	DD	3526603391
	DD	3217421732
	DD	0
	DD	3219128320
	DD	577417135
	DD	3219633631
	DD	2684354560
	DD	1014035767
	DD	1250776663
	DD	3217345034
	DD	0
	DD	3219128320
	DD	3814135665
	DD	3219650796
	DD	3221225472
	DD	1015662808
	DD	4019165092
	DD	3217267498
	DD	0
	DD	3219128320
	DD	201299822
	DD	3219667726
	DD	3758096384
	DD	1014428704
	DD	1852581091
	DD	3217189138
	DD	0
	DD	3219128320
	DD	263859903
	DD	3219684416
	DD	0
	DD	1015208636
	DD	2485417816
	DD	3217109964
	DD	0
	DD	3219128320
	DD	1796544321
	DD	3219700864
	DD	536870912
	DD	1015203297
	DD	2586490530
	DD	3217028810
	DD	0
	DD	3219128320
	DD	2750387213
	DD	3219717068
	DD	2147483648
	DD	1013374889
	DD	1224902090
	DD	3216867281
	DD	0
	DD	3219128320
	DD	1234330619
	DD	3219733026
	DD	2684354560
	DD	3162756121
	DD	2285031318
	DD	3216704197
	DD	0
	DD	3219128320
	DD	4106737474
	DD	3219748734
	DD	2684354560
	DD	1015108729
	DD	3876290983
	DD	3216539583
	DD	0
	DD	3219128320
	DD	1207142209
	DD	3219764192
	DD	2147483648
	DD	1015221986
	DD	802660176
	DD	3216373465
	DD	0
	DD	3219128320
	DD	4012486548
	DD	3219779395
	DD	2684354560
	DD	3162185212
	DD	1726519029
	DD	3216205866
	DD	0
	DD	3219128320
	DD	2688952194
	DD	3219794343
	DD	0
	DD	1014048955
	DD	3382663878
	DD	3216036812
	DD	0
	DD	3219128320
	DD	453180130
	DD	3219809033
	DD	1073741824
	DD	1014927633
	DD	2598800519
	DD	3215750067
	DD	0
	DD	3219128320
	DD	688824739
	DD	3219823462
	DD	3758096384
	DD	3157915184
	DD	1038988426
	DD	3215406294
	DD	0
	DD	3219128320
	DD	2653017361
	DD	3219837628
	DD	0
	DD	1015316414
	DD	1102361128
	DD	3215059765
	DD	0
	DD	3219128320
	DD	1477771776
	DD	3219851530
	DD	536870912
	DD	1015621338
	DD	2880516564
	DD	3214487049
	DD	0
	DD	3219128320
	DD	1056266002
	DD	3219865165
	DD	2684354560
	DD	1015985848
	DD	1220541286
	DD	3213681126
	DD	0
	DD	3219128320
	DD	1159294526
	DD	3219878531
	DD	536870912
	DD	1015793311
	DD	1279178457
	DD	3211590791
	DD	0
	DD	3219128320
	DD	1731563730
	DD	3219891626
	DD	536870912
	DD	3163023950
	DD	31019393
	DD	1065162441
	DD	0
	DD	3219128320
	DD	2892993834
	DD	3219904448
	DD	536870912
	DD	1015555495
	DD	3506991783
	DD	1066548842
	DD	0
	DD	3219128320
	DD	645027145
	DD	3219916996
	DD	3221225472
	DD	3160085887
	DD	2140183630
	DD	1067272748
	DD	0
	DD	3219128320
	DD	4051746225
	DD	3219929266
	DD	1610612736
	DD	1014423729
	DD	600368053
	DD	1067725893
	DD	0
	DD	3219128320
	DD	881357723
	DD	3219941259
	DD	3221225472
	DD	3161294538
	DD	913431823
	DD	1068092555
	DD	0
	DD	3219128320
	DD	557060597
	DD	3219952971
	DD	0
	DD	1012071286
	DD	1073154251
	DD	1068461489
	DD	0
	DD	3219128320
	DD	4093443164
	DD	3219964400
	DD	536870912
	DD	3162396513
	DD	3631910143
	DD	1068665791
	DD	0
	DD	3219128320
	DD	4097623923
	DD	3219975546
	DD	2684354560
	DD	1012824939
	DD	1380647130
	DD	1068852447
	DD	0
	DD	3219128320
	DD	1950234076
	DD	3219986407
	DD	536870912
	DD	1011941850
	DD	4049357271
	DD	1069040154
	DD	0
	DD	3219128320
	DD	3511535930
	DD	3219996980
	DD	2147483648
	DD	1015344978
	DD	1896722595
	DD	1069228886
	DD	0
	DD	3219128320
	DD	1942611595
	DD	3220007265
	DD	3221225472
	DD	1014926433
	DD	1699043957
	DD	1069418613
	DD	0
	DD	3219128320
	DD	3476196678
	DD	3220017259
	DD	0
	DD	3161741286
	DD	3296146332
	DD	3216969380
	DD	0
	DD	3218079744
	DD	1647878299
	DD	3220026962
	DD	3758096384
	DD	1014685415
	DD	3286832763
	DD	3216777748
	DD	0
	DD	3218079744
	DD	3066872380
	DD	3220036371
	DD	3221225472
	DD	3163044919
	DD	1962712856
	DD	3216585207
	DD	0
	DD	3218079744
	DD	1647164971
	DD	3220045486
	DD	2147483648
	DD	1015354602
	DD	3601629044
	DD	3216391785
	DD	0
	DD	3218079744
	DD	83265253
	DD	3220054305
	DD	1610612736
	DD	1015026887
	DD	165911977
	DD	3216197513
	DD	0
	DD	3218079744
	DD	1261158782
	DD	3220062826
	DD	2147483648
	DD	3163195192
	DD	1348311045
	DD	3216002418
	DD	0
	DD	3218079744
	DD	3964199186
	DD	3220071048
	DD	3758096384
	DD	1013017430
	DD	386665102
	DD	3215630470
	DD	0
	DD	3218079744
	DD	2873938189
	DD	3220078971
	DD	1610612736
	DD	1014827774
	DD	1991047213
	DD	3215237169
	DD	0
	DD	3218079744
	DD	1455828442
	DD	3220086593
	DD	3758096384
	DD	3162988721
	DD	4107121629
	DD	3214750787
	DD	0
	DD	3218079744
	DD	3370060660
	DD	3220093912
	DD	1610612736
	DD	3162270860
	DD	723478390
	DD	3213958438
	DD	0
	DD	3218079744
	DD	3882371427
	DD	3220100928
	DD	2147483648
	DD	3162407623
	DD	2382326656
	DD	3212045827
	DD	0
	DD	3218079744
	DD	2749658050
	DD	3220107640
	DD	1610612736
	DD	3162910986
	DD	2614369450
	DD	1065140432
	DD	0
	DD	3218079744
	DD	4220661975
	DD	3220114046
	DD	3221225472
	DD	3161795706
	DD	3587565429
	DD	1066624384
	DD	0
	DD	3218079744
	DD	4151720666
	DD	3220120146
	DD	0
	DD	1015155896
	DD	2886567144
	DD	1067426796
	DD	0
	DD	3218079744
	DD	2892293513
	DD	3220125939
	DD	2684354560
	DD	1014856623
	DD	1765797566
	DD	1067840895
	DD	0
	DD	3218079744
	DD	990588717
	DD	3220131424
	DD	3758096384
	DD	1014558209
	DD	240740309
	DD	1068244255
	DD	0
	DD	3218079744
	DD	3489094832
	DD	3220136599
	DD	536870912
	DD	3161809431
	DD	2034433221
	DD	3215832935
	DD	0
	DD	3217031168
	DD	2745245815
	DD	3220141465
	DD	3221225472
	DD	3163049777
	DD	304938777
	DD	3215427665
	DD	0
	DD	3217031168
	DD	4201728937
	DD	3220146020
	DD	1610612736
	DD	3162157654
	DD	3299146087
	DD	3215021530
	DD	0
	DD	3217031168
	DD	617154971
	DD	3220150265
	DD	3758096384
	DD	1015950048
	DD	1953696209
	DD	3214295171
	DD	0
	DD	3217031168
	DD	2131272983
	DD	3220154197
	DD	3221225472
	DD	1015521448
	DD	3987691371
	DD	3213074188
	DD	0
	DD	3217031168
	DD	1905645377
	DD	3220157817
	DD	1610612736
	DD	3161571244
	DD	718380601
	DD	1063597621
	DD	0
	DD	3217031168
	DD	1893834707
	DD	3220161124
	DD	1610612736
	DD	3162631302
	DD	3272206680
	DD	1066288081
	DD	0
	DD	3217031168
	DD	4251821691
	DD	3220164117
	DD	2147483648
	DD	1014357359
	DD	257503056
	DD	1067164005
	DD	0
	DD	3217031168
	DD	2748392742
	DD	3220166797
	DD	536870912
	DD	1015578102
	DD	711626851
	DD	3214400320
	DD	0
	DD	3215982592
	DD	4240268041
	DD	3220169162
	DD	536870912
	DD	3161072611
	DD	2523975598
	DD	3213273348
	DD	0
	DD	3215982592
	DD	2902558778
	DD	3220171213
	DD	3221225472
	DD	3160744428
	DD	3237733378
	DD	1062425136
	DD	0
	DD	3215982592
	DD	1703834029
	DD	3220172949
	DD	3758096384
	DD	3162512012
	DD	1003003824
	DD	1066107496
	DD	0
	DD	3215982592
	DD	3816385918
	DD	3220174369
	DD	536870912
	DD	1013733308
	DD	2896342633
	DD	3212231467
	DD	0
	DD	3214934016
	DD	4026464093
	DD	3220175474
	DD	3221225472
	DD	1015592141
	DD	561145352
	DD	1065056936
	DD	0
	DD	3214934016
	DD	1619315981
	DD	3220176264
	DD	1073741824
	DD	1015100749
	DD	210035182
	DD	1064007864
	DD	0
	DD	3213885440
	DD	379294427
	DD	3220176738
	DD	3221225472
	DD	3162889635
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	3220176896
	DD	0
	DD	0
	DD	210035182
	DD	3211491512
	DD	0
	DD	1066401792
	DD	379294427
	DD	3220176738
	DD	3221225472
	DD	3162889635
	DD	561145352
	DD	3212540584
	DD	0
	DD	1067450368
	DD	1619315981
	DD	3220176264
	DD	1073741824
	DD	1015100749
	DD	2896342633
	DD	1064747819
	DD	0
	DD	1067450368
	DD	4026464093
	DD	3220175474
	DD	3221225472
	DD	1015592141
	DD	1003003824
	DD	3213591144
	DD	0
	DD	1068498944
	DD	3816385918
	DD	3220174369
	DD	536870912
	DD	1013733308
	DD	3237733378
	DD	3209908784
	DD	0
	DD	1068498944
	DD	1703834029
	DD	3220172949
	DD	3758096384
	DD	3162512012
	DD	2523975598
	DD	1065789700
	DD	0
	DD	1068498944
	DD	2902558778
	DD	3220171213
	DD	3221225472
	DD	3160744428
	DD	711626851
	DD	1066916672
	DD	0
	DD	1068498944
	DD	4240268041
	DD	3220169162
	DD	536870912
	DD	3161072611
	DD	257503056
	DD	3214647653
	DD	0
	DD	1069547520
	DD	2748392742
	DD	3220166797
	DD	536870912
	DD	1015578102
	DD	3272206680
	DD	3213771729
	DD	0
	DD	1069547520
	DD	4251821691
	DD	3220164117
	DD	2147483648
	DD	1014357359
	DD	718380601
	DD	3211081269
	DD	0
	DD	1069547520
	DD	1893834707
	DD	3220161124
	DD	1610612736
	DD	3162631302
	DD	3987691371
	DD	1065590540
	DD	0
	DD	1069547520
	DD	1905645377
	DD	3220157817
	DD	1610612736
	DD	3161571244
	DD	1953696209
	DD	1066811523
	DD	0
	DD	1069547520
	DD	2131272983
	DD	3220154197
	DD	3221225472
	DD	1015521448
	DD	3299146087
	DD	1067537882
	DD	0
	DD	1069547520
	DD	617154971
	DD	3220150265
	DD	3758096384
	DD	1015950048
	DD	304938777
	DD	1067944017
	DD	0
	DD	1069547520
	DD	4201728937
	DD	3220146020
	DD	1610612736
	DD	3162157654
	DD	2034433221
	DD	1068349287
	DD	0
	DD	1069547520
	DD	2745245815
	DD	3220141465
	DD	3221225472
	DD	3163049777
	DD	240740309
	DD	3215727903
	DD	0
	DD	1070596096
	DD	3489094832
	DD	3220136599
	DD	536870912
	DD	3161809431
	DD	1765797566
	DD	3215324543
	DD	0
	DD	1070596096
	DD	990588717
	DD	3220131424
	DD	3758096384
	DD	1014558209
	DD	2886567144
	DD	3214910444
	DD	0
	DD	1070596096
	DD	2892293513
	DD	3220125939
	DD	2684354560
	DD	1014856623
	DD	3587565429
	DD	3214108032
	DD	0
	DD	1070596096
	DD	4151720666
	DD	3220120146
	DD	0
	DD	1015155896
	DD	2614369450
	DD	3212624080
	DD	0
	DD	1070596096
	DD	4220661975
	DD	3220114046
	DD	3221225472
	DD	3161795706
	DD	2382326656
	DD	1064562179
	DD	0
	DD	1070596096
	DD	2749658050
	DD	3220107640
	DD	1610612736
	DD	3162910986
	DD	723478390
	DD	1066474790
	DD	0
	DD	1070596096
	DD	3882371427
	DD	3220100928
	DD	2147483648
	DD	3162407623
	DD	4107121629
	DD	1067267139
	DD	0
	DD	1070596096
	DD	3370060660
	DD	3220093912
	DD	1610612736
	DD	3162270860
	DD	1991047213
	DD	1067753521
	DD	0
	DD	1070596096
	DD	1455828442
	DD	3220086593
	DD	3758096384
	DD	3162988721
	DD	386665102
	DD	1068146822
	DD	0
	DD	1070596096
	DD	2873938189
	DD	3220078971
	DD	1610612736
	DD	1014827774
	DD	1348311045
	DD	1068518770
	DD	0
	DD	1070596096
	DD	3964199186
	DD	3220071048
	DD	3758096384
	DD	1013017430
	DD	165911977
	DD	1068713865
	DD	0
	DD	1070596096
	DD	1261158782
	DD	3220062826
	DD	2147483648
	DD	3163195192
	DD	3601629044
	DD	1068908137
	DD	0
	DD	1070596096
	DD	83265253
	DD	3220054305
	DD	1610612736
	DD	1015026887
	DD	1962712856
	DD	1069101559
	DD	0
	DD	1070596096
	DD	1647164971
	DD	3220045486
	DD	2147483648
	DD	1015354602
	DD	3286832763
	DD	1069294100
	DD	0
	DD	1070596096
	DD	3066872380
	DD	3220036371
	DD	3221225472
	DD	3163044919
	DD	3296146332
	DD	1069485732
	DD	0
	DD	1070596096
	DD	1647878299
	DD	3220026962
	DD	3758096384
	DD	1014685415
	DD	1699043957
	DD	3216902261
	DD	0
	DD	1071644672
	DD	3476196678
	DD	3220017259
	DD	0
	DD	3161741286
	DD	1896722595
	DD	3216712534
	DD	0
	DD	1071644672
	DD	1942611595
	DD	3220007265
	DD	3221225472
	DD	1014926433
	DD	4049357271
	DD	3216523802
	DD	0
	DD	1071644672
	DD	3511535930
	DD	3219996980
	DD	2147483648
	DD	1015344978
	DD	1380647130
	DD	3216336095
	DD	0
	DD	1071644672
	DD	1950234076
	DD	3219986407
	DD	536870912
	DD	1011941850
	DD	3631910143
	DD	3216149439
	DD	0
	DD	1071644672
	DD	4097623923
	DD	3219975546
	DD	2684354560
	DD	1012824939
	DD	1073154251
	DD	3215945137
	DD	0
	DD	1071644672
	DD	4093443164
	DD	3219964400
	DD	536870912
	DD	3162396513
	DD	913431823
	DD	3215576203
	DD	0
	DD	1071644672
	DD	557060597
	DD	3219952971
	DD	0
	DD	1012071286
	DD	600368053
	DD	3215209541
	DD	0
	DD	1071644672
	DD	881357723
	DD	3219941259
	DD	3221225472
	DD	3161294538
	DD	2140183630
	DD	3214756396
	DD	0
	DD	1071644672
	DD	4051746225
	DD	3219929266
	DD	1610612736
	DD	1014423729
	DD	3506991783
	DD	3214032490
	DD	0
	DD	1071644672
	DD	645027145
	DD	3219916996
	DD	3221225472
	DD	3160085887
	DD	31019393
	DD	3212646089
	DD	0
	DD	1071644672
	DD	2892993834
	DD	3219904448
	DD	536870912
	DD	1015555495
	DD	1279178457
	DD	1064107143
	DD	0
	DD	1071644672
	DD	1731563730
	DD	3219891626
	DD	536870912
	DD	3163023950
	DD	1220541286
	DD	1066197478
	DD	0
	DD	1071644672
	DD	1159294526
	DD	3219878531
	DD	536870912
	DD	1015793311
	DD	2880516564
	DD	1067003401
	DD	0
	DD	1071644672
	DD	1056266002
	DD	3219865165
	DD	2684354560
	DD	1015985848
	DD	1102361128
	DD	1067576117
	DD	0
	DD	1071644672
	DD	1477771776
	DD	3219851530
	DD	536870912
	DD	1015621338
	DD	1038988426
	DD	1067922646
	DD	0
	DD	1071644672
	DD	2653017361
	DD	3219837628
	DD	0
	DD	1015316414
	DD	2598800519
	DD	1068266419
	DD	0
	DD	1071644672
	DD	688824739
	DD	3219823462
	DD	3758096384
	DD	3157915184
	DD	3382663878
	DD	1068553164
	DD	0
	DD	1071644672
	DD	453180130
	DD	3219809033
	DD	1073741824
	DD	1014927633
	DD	1726519029
	DD	1068722218
	DD	0
	DD	1071644672
	DD	2688952194
	DD	3219794343
	DD	0
	DD	1014048955
	DD	802660176
	DD	1068889817
	DD	0
	DD	1071644672
	DD	4012486548
	DD	3219779395
	DD	2684354560
	DD	3162185212
	DD	3876290983
	DD	1069055935
	DD	0
	DD	1071644672
	DD	1207142209
	DD	3219764192
	DD	2147483648
	DD	1015221986
	DD	2285031318
	DD	1069220549
	DD	0
	DD	1071644672
	DD	4106737474
	DD	3219748734
	DD	2684354560
	DD	1015108729
	DD	1224902090
	DD	1069383633
	DD	0
	DD	1071644672
	DD	1234330619
	DD	3219733026
	DD	2684354560
	DD	3162756121
	DD	2586490530
	DD	1069545162
	DD	0
	DD	1071644672
	DD	2750387213
	DD	3219717068
	DD	2147483648
	DD	1013374889
	DD	2485417816
	DD	1069626316
	DD	0
	DD	1071644672
	DD	1796544321
	DD	3219700864
	DD	536870912
	DD	1015203297
	DD	1852581091
	DD	1069705490
	DD	0
	DD	1071644672
	DD	263859903
	DD	3219684416
	DD	0
	DD	1015208636
	DD	4019165092
	DD	1069783850
	DD	0
	DD	1071644672
	DD	201299822
	DD	3219667726
	DD	3758096384
	DD	1014428704
	DD	1250776663
	DD	1069861386
	DD	0
	DD	1071644672
	DD	3814135665
	DD	3219650796
	DD	3221225472
	DD	1015662808
	DD	3526603391
	DD	1069938084
	DD	0
	DD	1071644672
	DD	577417135
	DD	3219633631
	DD	2684354560
	DD	1014035767
	DD	4187227697
	DD	1070013934
	DD	0
	DD	1071644672
	DD	1594061409
	DD	3219616231
	DD	2147483648
	DD	3161837518
	DD	1416934267
	DD	1070088925
	DD	0
	DD	1071644672
	DD	938475414
	DD	3219598600
	DD	0
	DD	3162559727
	DD	2546065654
	DD	1070163044
	DD	0
	DD	1071644672
	DD	1424664751
	DD	3219580740
	DD	3221225472
	DD	1015921667
	DD	2583490354
	DD	1070236281
	DD	0
	DD	1071644672
	DD	1719614413
	DD	3219562654
	DD	0
	DD	1015799092
	DD	1403691706
	DD	1070308625
	DD	0
	DD	1071644672
	DD	636516413
	DD	3219544345
	DD	3758096384
	DD	3163307364
	DD	3753901657
	DD	1070380064
	DD	0
	DD	1071644672
	DD	1427975391
	DD	3219525815
	DD	1073741824
	DD	1014124979
	DD	2081278341
	DD	1070450589
	DD	0
	DD	1071644672
	DD	3194290572
	DD	3219507067
	DD	2147483648
	DD	3162532330
	DD	2309668539
	DD	1070520188
	DD	0
	DD	1071644672
	DD	881650848
	DD	3219488105
	DD	2147483648
	DD	1015502624
	DD	2371640770
	DD	1070588851
	DD	0
	DD	1071644672
	DD	2460177814
	DD	3219468930
	DD	2147483648
	DD	1014633195
	DD	3489768009
	DD	3218019271
	DD	0
	DD	1072693248
	DD	3152274921
	DD	3219449546
	DD	3758096384
	DD	3160654483
	DD	3239527685
	DD	3217952511
	DD	0
	DD	1072693248
	DD	2610628921
	DD	3219429956
	DD	1610612736
	DD	1015185121
	DD	1403757309
	DD	3217886718
	DD	0
	DD	1072693248
	DD	621354454
	DD	3219410163
	DD	536870912
	DD	3160934250
	DD	1883353036
	DD	3217821901
	DD	0
	DD	1072693248
	DD	1397053140
	DD	3219390169
	DD	1610612736
	DD	3162728726
	DD	3652612115
	DD	3217758070
	DD	0
	DD	1072693248
	DD	689983673
	DD	3219369978
	DD	3758096384
	DD	3162390053
	DD	752919289
	DD	3217695236
	DD	0
	DD	1072693248
	DD	2969983475
	DD	3219349592
	DD	2684354560
	DD	1015151795
	DD	3761365757
	DD	3217633406
	DD	0
	DD	1072693248
	DD	4242632757
	DD	3219329015
	DD	536870912
	DD	1013906630
	DD	1129922991
	DD	3217572592
	DD	0
	DD	1072693248
	DD	637203434
	DD	3219308251
	DD	3221225472
	DD	1011960193
	DD	2129093113
	DD	3217512801
	DD	0
	DD	1072693248
	DD	1289556703
	DD	3219287301
	DD	536870912
	DD	1015405172
	DD	2482246777
	DD	3217454043
	DD	0
	DD	1072693248
	DD	2570316633
	DD	3219266169
	DD	1610612736
	DD	1015261105
	DD	1539668340
	DD	3217396327
	DD	0
	DD	1072693248
	DD	967731400
	DD	3219244859
	DD	536870912
	DD	3163235805
	DD	2272832445
	DD	3217339661
	DD	0
	DD	1072693248
	DD	1675549513
	DD	3219223373
	DD	3221225472
	DD	1015541490
	DD	2678847490
	DD	3217284054
	DD	0
	DD	1072693248
	DD	1411074851
	DD	3219201715
	DD	1610612736
	DD	1016001083
	DD	69903290
	DD	3217229515
	DD	0
	DD	1072693248
	DD	1297975695
	DD	3219179888
	DD	3221225472
	DD	1015600087
	DD	3952756490
	DD	3217176050
	DD	0
	DD	1072693248
	DD	2569207790
	DD	3219157895
	DD	1073741824
	DD	1015699969
	DD	1663679671
	DD	3217123670
	DD	0
	DD	1072693248
	DD	2269920951
	DD	3219135740
	DD	3758096384
	DD	1014188597
	DD	1312926554
	DD	3217072381
	DD	0
	DD	1072693248
	DD	3690502842
	DD	3219098532
	DD	3221225472
	DD	1009970207
	DD	3429717432
	DD	3217013214
	DD	0
	DD	1072693248
	DD	1391425750
	DD	3219053593
	DD	3221225472
	DD	1014878455
	DD	1945768569
	DD	3216915048
	DD	0
	DD	1072693248
	DD	939980347
	DD	3219008349
	DD	0
	DD	3160280457
	DD	1539072807
	DD	3216819110
	DD	0
	DD	1072693248
	DD	1535566729
	DD	3218962807
	DD	2147483648
	DD	3161805036
	DD	4133449816
	DD	3216725414
	DD	0
	DD	1072693248
	DD	2570175582
	DD	3218916974
	DD	1073741824
	DD	3159786766
	DD	1612193054
	DD	3216633976
	DD	0
	DD	1072693248
	DD	3623952103
	DD	3218870857
	DD	2684354560
	DD	1014502314
	DD	1578746984
	DD	3216544808
	DD	0
	DD	1072693248
	DD	165764288
	DD	3218824464
	DD	3221225472
	DD	3160781666
	DD	1577996576
	DD	3216457924
	DD	0
	DD	1072693248
	DD	728580042
	DD	3218777800
	DD	1610612736
	DD	1014849296
	DD	1972483635
	DD	3216373337
	DD	0
	DD	1072693248
	DD	1135144330
	DD	3218730873
	DD	536870912
	DD	3160857217
	DD	1638976426
	DD	3216291060
	DD	0
	DD	1072693248
	DD	1673302999
	DD	3218683690
	DD	2147483648
	DD	3160835121
	DD	2255197647
	DD	3216211105
	DD	0
	DD	1072693248
	DD	2796464483
	DD	3218636258
	DD	3221225472
	DD	1013394669
	DD	3996842794
	DD	3216133484
	DD	0
	DD	1072693248
	DD	824036583
	DD	3218588585
	DD	0
	DD	3155796978
	DD	1234824733
	DD	3216058210
	DD	0
	DD	1072693248
	DD	821708191
	DD	3218540677
	DD	3758096384
	DD	1014254361
	DD	4002549419
	DD	3215985292
	DD	0
	DD	1072693248
	DD	3711903686
	DD	3218492541
	DD	1073741824
	DD	3159428395
	DD	2962723252
	DD	3215846895
	DD	0
	DD	1072693248
	DD	1974149085
	DD	3218444186
	DD	3221225472
	DD	1008664000
	DD	1296295961
	DD	3215710555
	DD	0
	DD	1072693248
	DD	1115219818
	DD	3218395618
	DD	0
	DD	3160555890
	DD	997322465
	DD	3215578993
	DD	0
	DD	1072693248
	DD	2484561409
	DD	3218346844
	DD	1073741824
	DD	3161508220
	DD	1261629236
	DD	3215452229
	DD	0
	DD	1072693248
	DD	3269558610
	DD	3218297872
	DD	536870912
	DD	1014221568
	DD	2476548698
	DD	3215330282
	DD	0
	DD	1072693248
	DD	785751814
	DD	3218248710
	DD	2684354560
	DD	1014354573
	DD	1913604284
	DD	3215213171
	DD	0
	DD	1072693248
	DD	1062001470
	DD	3218199364
	DD	0
	DD	1013391572
	DD	2306566604
	DD	3215100913
	DD	0
	DD	1072693248
	DD	1655830135
	DD	3218149842
	DD	0
	DD	3161990338
	DD	3250111450
	DD	3214993525
	DD	0
	DD	1072693248
	DD	238550446
	DD	3218100152
	DD	2684354560
	DD	1006899917
	DD	2377770267
	DD	3214848032
	DD	0
	DD	1072693248
	DD	2065784603
	DD	3218020857
	DD	3758096384
	DD	1013198318
	DD	288924873
	DD	3214652833
	DD	0
	DD	1072693248
	DD	3846521617
	DD	3217920847
	DD	536870912
	DD	3160890258
	DD	1935888103
	DD	3214467466
	DD	0
	DD	1072693248
	DD	1786662755
	DD	3217820546
	DD	2147483648
	DD	1008398659
	DD	2660899430
	DD	3214291960
	DD	0
	DD	1072693248
	DD	632292433
	DD	3217719968
	DD	3758096384
	DD	1013479685
	DD	18115067
	DD	3214126342
	DD	0
	DD	1072693248
	DD	1013556747
	DD	3217619128
	DD	2684354560
	DD	1013083417
	DD	2346447124
	DD	3213970635
	DD	0
	DD	1072693248
	DD	3729833777
	DD	3217518041
	DD	3758096384
	DD	3160528366
	DD	1672287667
	DD	3213764289
	DD	0
	DD	1072693248
	DD	1149976518
	DD	3217416724
	DD	1610612736
	DD	3160748543
	DD	3451754846
	DD	3213492662
	DD	0
	DD	1072693248
	DD	2972270170
	DD	3217315190
	DD	1073741824
	DD	1012041122
	DD	1037482584
	DD	3213240992
	DD	0
	DD	1072693248
	DD	1854824762
	DD	3217213456
	DD	2684354560
	DD	3159437312
	DD	2592747447
	DD	3213009315
	DD	0
	DD	1072693248
	DD	3470456183
	DD	3217111536
	DD	1073741824
	DD	1013261123
	DD	2402293340
	DD	3212758471
	DD	0
	DD	1072693248
	DD	1453945614
	DD	3216987726
	DD	1610612736
	DD	3158976260
	DD	1227670166
	DD	3212375297
	DD	0
	DD	1072693248
	DD	3349070549
	DD	3216783237
	DD	3758096384
	DD	1009054621
	DD	393047345
	DD	3212032302
	DD	0
	DD	1072693248
	DD	3156849708
	DD	3216578470
	DD	3221225472
	DD	1010706200
	DD	1118107366
	DD	3211670787
	DD	0
	DD	1072693248
	DD	177906713
	DD	3216373456
	DD	3221225472
	DD	1011826722
	DD	4269262315
	DD	3211145810
	DD	0
	DD	1072693248
	DD	2462980598
	DD	3216168224
	DD	2684354560
	DD	1011458184
	DD	3810305407
	DD	3210663220
	DD	0
	DD	1072693248
	DD	2046304480
	DD	3215943022
	DD	3758096384
	DD	1008919019
	DD	220529721
	DD	3209935929
	DD	0
	DD	1072693248
	DD	4044216340
	DD	3215531877
	DD	0
	DD	1009324733
	DD	69373323
	DD	3209049152
	DD	0
	DD	1072693248
	DD	1972655565
	DD	3215120485
	DD	1073741824
	DD	3156641526
	DD	3650591271
	DD	3207838971
	DD	0
	DD	1072693248
	DD	4154680958
	DD	3214483797
	DD	0
	DD	1006312803
	DD	2376373521
	DD	3205741868
	DD	0
	DD	1072693248
	DD	4242458500
	DD	3213435345
	DD	2684354560
	DD	3156838286
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
	DD	239972940
	DD	1089470464
	DD	239972940
	DD	1089470464
	DD	239972940
	DD	1089470464
	DD	239972940
	DD	1089470464
	DD	239972940
	DD	1089470464
	DD	239972940
	DD	1089470464
	DD	239972940
	DD	1089470464
	DD	239972940
	DD	1089470464
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
	DD	1841940611
	DD	1079271216
	DD	1841940611
	DD	1079271216
	DD	1841940611
	DD	1079271216
	DD	1841940611
	DD	1079271216
	DD	1841940611
	DD	1079271216
	DD	1841940611
	DD	1079271216
	DD	1841940611
	DD	1079271216
	DD	1841940611
	DD	1079271216
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
	DD	511
	DD	0
	DD	511
	DD	0
	DD	511
	DD	0
	DD	511
	DD	0
	DD	511
	DD	0
	DD	511
	DD	0
	DD	511
	DD	0
	DD	511
	DD	0
	DD	1413480448
	DD	1065951739
	DD	1413480448
	DD	1065951739
	DD	1413480448
	DD	1065951739
	DD	1413480448
	DD	1065951739
	DD	1413480448
	DD	1065951739
	DD	1413480448
	DD	1065951739
	DD	1413480448
	DD	1065951739
	DD	1413480448
	DD	1065951739
	DD	442499072
	DD	1029747809
	DD	442499072
	DD	1029747809
	DD	442499072
	DD	1029747809
	DD	442499072
	DD	1029747809
	DD	442499072
	DD	1029747809
	DD	442499072
	DD	1029747809
	DD	442499072
	DD	1029747809
	DD	442499072
	DD	1029747809
	DD	771977331
	DD	993204618
	DD	771977331
	DD	993204618
	DD	771977331
	DD	993204618
	DD	771977331
	DD	993204618
	DD	771977331
	DD	993204618
	DD	771977331
	DD	993204618
	DD	771977331
	DD	993204618
	DD	771977331
	DD	993204618
	DD	1431651400
	DD	3217380693
	DD	1431651400
	DD	3217380693
	DD	1431651400
	DD	3217380693
	DD	1431651400
	DD	3217380693
	DD	1431651400
	DD	3217380693
	DD	1431651400
	DD	3217380693
	DD	1431651400
	DD	3217380693
	DD	1431651400
	DD	3217380693
	DD	2880656668
	DD	1065423119
	DD	2880656668
	DD	1065423119
	DD	2880656668
	DD	1065423119
	DD	2880656668
	DD	1065423119
	DD	2880656668
	DD	1065423119
	DD	2880656668
	DD	1065423119
	DD	2880656668
	DD	1065423119
	DD	2880656668
	DD	1065423119
	DD	0
	DD	3219128320
	DD	0
	DD	3219128320
	DD	0
	DD	3219128320
	DD	0
	DD	3219128320
	DD	0
	DD	3219128320
	DD	0
	DD	3219128320
	DD	0
	DD	3219128320
	DD	0
	DD	3219128320
	DD	1431653583
	DD	1067799893
	DD	1431653583
	DD	1067799893
	DD	1431653583
	DD	1067799893
	DD	1431653583
	DD	1067799893
	DD	1431653583
	DD	1067799893
	DD	1431653583
	DD	1067799893
	DD	1431653583
	DD	1067799893
	DD	1431653583
	DD	1067799893
	DD	2976101079
	DD	3210133866
	DD	2976101079
	DD	3210133866
	DD	2976101079
	DD	3210133866
	DD	2976101079
	DD	3210133866
	DD	2976101079
	DD	3210133866
	DD	2976101079
	DD	3210133866
	DD	2976101079
	DD	3210133866
	DD	2976101079
	DD	3210133866
	PUBLIC __svml_dsin_ha_data_internal
__svml_dsin_ha_data_internal	DD	4294967295
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
	DD	1097859072
	DD	0
	DD	1097859072
	DD	0
	DD	1097859072
	DD	0
	DD	1097859072
	DD	0
	DD	1097859072
	DD	0
	DD	1097859072
	DD	0
	DD	1097859072
	DD	0
	DD	1097859072
	DD	1841940611
	DD	1075076912
	DD	1841940611
	DD	1075076912
	DD	1841940611
	DD	1075076912
	DD	1841940611
	DD	1075076912
	DD	1841940611
	DD	1075076912
	DD	1841940611
	DD	1075076912
	DD	1841940611
	DD	1075076912
	DD	1841940611
	DD	1075076912
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
	DD	1413754136
	DD	1070146043
	DD	1413754136
	DD	1070146043
	DD	1413754136
	DD	1070146043
	DD	1413754136
	DD	1070146043
	DD	1413754136
	DD	1070146043
	DD	1413754136
	DD	1070146043
	DD	1413754136
	DD	1070146043
	DD	1413754136
	DD	1070146043
	DD	855638016
	DD	1013032486
	DD	855638016
	DD	1013032486
	DD	855638016
	DD	1013032486
	DD	855638016
	DD	1013032486
	DD	855638016
	DD	1013032486
	DD	855638016
	DD	1013032486
	DD	855638016
	DD	1013032486
	DD	855638016
	DD	1013032486
	DD	3773204808
	DD	979655686
	DD	3773204808
	DD	979655686
	DD	3773204808
	DD	979655686
	DD	3773204808
	DD	979655686
	DD	3773204808
	DD	979655686
	DD	3773204808
	DD	979655686
	DD	3773204808
	DD	979655686
	DD	3773204808
	DD	979655686
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
	DD	0
	DD	0
	DD	1013556747
	DD	1070135480
	DD	2796464483
	DD	1071152610
	DD	967731400
	DD	1071761211
	DD	1719614413
	DD	1072079006
	DD	688824739
	DD	1072339814
	DD	3476196678
	DD	1072533611
	DD	3489094832
	DD	1072652951
	DD	0
	DD	1072693248
	DD	3489094832
	DD	1072652951
	DD	3476196678
	DD	1072533611
	DD	688824739
	DD	1072339814
	DD	1719614413
	DD	1072079006
	DD	967731400
	DD	1071761211
	DD	2796464483
	DD	1071152610
	DD	1013556747
	DD	1070135480
	DD	0
	DD	1072693248
	DD	3489094832
	DD	1072652951
	DD	3476196678
	DD	1072533611
	DD	688824739
	DD	1072339814
	DD	1719614413
	DD	1072079006
	DD	967731400
	DD	1071761211
	DD	2796464483
	DD	1071152610
	DD	1013556747
	DD	1070135480
	DD	0
	DD	0
	DD	1013556747
	DD	3217619128
	DD	2796464483
	DD	3218636258
	DD	967731400
	DD	3219244859
	DD	1719614413
	DD	3219562654
	DD	688824739
	DD	3219823462
	DD	3476196678
	DD	3220017259
	DD	3489094832
	DD	3220136599
	DD	0
	DD	0
	DD	3160567065
	DD	1014325783
	DD	3160878317
	DD	1014257638
	DD	1015752157
	DD	1010431536
	DD	3163282740
	DD	3163282740
	DD	1010431536
	DD	1015752157
	DD	1014257638
	DD	3160878317
	DD	1014325783
	DD	3160567065
	DD	0
	DD	0
	DD	1014325783
	DD	1013083417
	DD	1014257638
	DD	1013394669
	DD	1010431536
	DD	3163235805
	DD	3163282740
	DD	1015799092
	DD	1015752157
	DD	3157915184
	DD	3160878317
	DD	3161741286
	DD	3160567065
	DD	3161809431
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
	DD	16
	DD	0
	DD	16
	DD	0
	DD	16
	DD	0
	DD	16
	DD	0
	DD	16
	DD	0
	DD	16
	DD	0
	DD	16
	DD	0
	DD	16
	DD	0
	DD	3654764988
	DD	1056571351
	DD	3654764988
	DD	1056571351
	DD	3654764988
	DD	1056571351
	DD	3654764988
	DD	1056571351
	DD	3654764988
	DD	1056571351
	DD	3654764988
	DD	1056571351
	DD	3654764988
	DD	1056571351
	DD	3654764988
	DD	1056571351
	DD	123565048
	DD	3210133868
	DD	123565048
	DD	3210133868
	DD	123565048
	DD	3210133868
	DD	123565048
	DD	3210133868
	DD	123565048
	DD	3210133868
	DD	123565048
	DD	3210133868
	DD	123565048
	DD	3210133868
	DD	123565048
	DD	3210133868
	DD	3525997533
	DD	1053236375
	DD	3525997533
	DD	1053236375
	DD	3525997533
	DD	1053236375
	DD	3525997533
	DD	1053236375
	DD	3525997533
	DD	1053236375
	DD	3525997533
	DD	1053236375
	DD	3525997533
	DD	1053236375
	DD	3525997533
	DD	1053236375
	DD	248520414
	DD	3207201184
	DD	248520414
	DD	3207201184
	DD	248520414
	DD	3207201184
	DD	248520414
	DD	3207201184
	DD	248520414
	DD	3207201184
	DD	248520414
	DD	3207201184
	DD	248520414
	DD	3207201184
	DD	248520414
	DD	3207201184
	DD	1431627990
	DD	1067799893
	DD	1431627990
	DD	1067799893
	DD	1431627990
	DD	1067799893
	DD	1431627990
	DD	1067799893
	DD	1431627990
	DD	1067799893
	DD	1431627990
	DD	1067799893
	DD	1431627990
	DD	1067799893
	DD	1431627990
	DD	1067799893
	DD	286321053
	DD	1065423121
	DD	286321053
	DD	1065423121
	DD	286321053
	DD	1065423121
	DD	286321053
	DD	1065423121
	DD	286321053
	DD	1065423121
	DD	286321053
	DD	1065423121
	DD	286321053
	DD	1065423121
	DD	286321053
	DD	1065423121
	DD	4294967292
	DD	3219128319
	DD	4294967292
	DD	3219128319
	DD	4294967292
	DD	3219128319
	DD	4294967292
	DD	3219128319
	DD	4294967292
	DD	3219128319
	DD	4294967292
	DD	3219128319
	DD	4294967292
	DD	3219128319
	DD	4294967292
	DD	3219128319
	DD	1431655765
	DD	3217380693
	DD	1431655765
	DD	3217380693
	DD	1431655765
	DD	3217380693
	DD	1431655765
	DD	3217380693
	DD	1431655765
	DD	3217380693
	DD	1431655765
	DD	3217380693
	DD	1431655765
	DD	3217380693
	DD	1431655765
	DD	3217380693
_2il0floatpacket_54	DD	000000000H,07ff00000H,000000000H,07ff00000H,000000000H,07ff00000H,000000000H,07ff00000H
_2il0floatpacket_55	DD	0ffffffffH,0000fffffH,0ffffffffH,0000fffffH,0ffffffffH,0000fffffH,0ffffffffH,0000fffffH
_2il0floatpacket_56	DD	000000000H,000100000H,000000000H,000100000H,000000000H,000100000H,000000000H,000100000H
_2il0floatpacket_57	DD	0ffffffffH,000000000H,0ffffffffH,000000000H,0ffffffffH,000000000H,0ffffffffH,000000000H
_2il0floatpacket_58	DD	000000000H,080000000H,000000000H,080000000H,000000000H,080000000H,000000000H,080000000H
_2il0floatpacket_59	DD	000000000H,03ff00000H,000000000H,03ff00000H,000000000H,03ff00000H,000000000H,03ff00000H
_2il0floatpacket_60	DD	000000000H,042a80000H,000000000H,042a80000H,000000000H,042a80000H,000000000H,042a80000H
_2il0floatpacket_61	DD	0000001ffH,000000000H,0000001ffH,000000000H,0000001ffH,000000000H,0000001ffH,000000000H
_2il0floatpacket_62	DD	000000000H,039700000H,000000000H,039700000H,000000000H,039700000H,000000000H,039700000H
_2il0floatpacket_63	DD	000ffffffH,000000000H,000ffffffH,000000000H,000ffffffH,000000000H,000ffffffH,000000000H
_2il0floatpacket_64	DD	000000000H,03cb00000H,000000000H,03cb00000H,000000000H,03cb00000H,000000000H,03cb00000H
_2il0floatpacket_65	DD	000000fffH,000000000H,000000fffH,000000000H,000000fffH,000000000H,000000fffH,000000000H
_2il0floatpacket_66	DD	054442d18H,0401921fbH,054442d18H,0401921fbH,054442d18H,0401921fbH,054442d18H,0401921fbH
_2il0floatpacket_67	DD	033145c07H,03cb1a626H,033145c07H,03cb1a626H,033145c07H,03cb1a626H,033145c07H,03cb1a626H
_2il0floatpacket_68	DD	0ffffffffH,07fffffffH,0ffffffffH,07fffffffH,0ffffffffH,07fffffffH,0ffffffffH,07fffffffH
_2il0floatpacket_69	DD	000000000H,03eb00000H,000000000H,03eb00000H,000000000H,03eb00000H,000000000H,03eb00000H
_2il0floatpacket_70	DD	054000000H,0401921fbH,054000000H,0401921fbH,054000000H,0401921fbH,054000000H,0401921fbH
_2il0floatpacket_71	DD	011a62633H,03e310b46H,011a62633H,03e310b46H,011a62633H,03e310b46H,011a62633H,03e310b46H
_2il0floatpacket_72	DD	0f8000000H,0ffffffffH,0f8000000H,0ffffffffH,0f8000000H,0ffffffffH,0f8000000H,0ffffffffH
_2il0floatpacket_36	DD	000000000H,07ff00000H,000000000H,07ff00000H
_2il0floatpacket_37	DD	0ffffffffH,0000fffffH,0ffffffffH,0000fffffH
_2il0floatpacket_38	DD	000000000H,000100000H,000000000H,000100000H
_2il0floatpacket_39	DD	0ffffffffH,000000000H,0ffffffffH,000000000H
_2il0floatpacket_40	DD	000000000H,080000000H,000000000H,080000000H
_2il0floatpacket_41	DD	000000000H,03ff00000H,000000000H,03ff00000H
_2il0floatpacket_42	DD	000000000H,042a80000H,000000000H,042a80000H
_2il0floatpacket_43	DD	0000001ffH,000000000H,0000001ffH,000000000H
_2il0floatpacket_44	DD	000000000H,039700000H,000000000H,039700000H
_2il0floatpacket_45	DD	000ffffffH,000000000H,000ffffffH,000000000H
_2il0floatpacket_46	DD	000000000H,03cb00000H,000000000H,03cb00000H
_2il0floatpacket_47	DD	000000fffH,000000000H,000000fffH,000000000H
_2il0floatpacket_48	DD	054442d18H,0401921fbH,054442d18H,0401921fbH
_2il0floatpacket_49	DD	054000000H,0401921fbH,054000000H,0401921fbH
_2il0floatpacket_50	DD	011a62633H,03e310b46H,011a62633H,03e310b46H
_2il0floatpacket_51	DD	0f8000000H,0ffffffffH,0f8000000H,0ffffffffH
_2il0floatpacket_52	DD	0ffffffffH,07fffffffH,0ffffffffH,07fffffffH
_2il0floatpacket_53	DD	000000000H,03eb00000H,000000000H,03eb00000H
_2il0floatpacket_73	DD	033145c07H,03cb1a626H,033145c07H,03cb1a626H
_2il0floatpacket_74	DD	0ffffffffH,0ffffffffH
_2il0floatpacket_75	DD	000000000H,07ff00000H
_2il0floatpacket_76	DD	0ffffffffH,0000fffffH
_2il0floatpacket_77	DD	000000000H,000100000H
_2il0floatpacket_78	DD	0ffffffffH,000000000H
_2il0floatpacket_79	DD	000000000H,080000000H
_2il0floatpacket_80	DD	000000000H,03ff00000H
_2il0floatpacket_81	DD	000000000H,042a80000H
_2il0floatpacket_82	DD	0000001ffH,000000000H
_2il0floatpacket_83	DD	000000000H,039700000H
_2il0floatpacket_84	DD	000ffffffH,000000000H
_2il0floatpacket_85	DD	000000000H,03cb00000H
_2il0floatpacket_86	DD	000000fffH,000000000H
_2il0floatpacket_87	DD	054442d18H,0401921fbH
_2il0floatpacket_88	DD	033145c07H,03cb1a626H
_2il0floatpacket_89	DD	0ffffffffH,07fffffffH
_2il0floatpacket_90	DD	000000000H,03eb00000H
_vmldSinHATab	DD	0
	DD	0
	DD	0
	DD	2146435072
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
