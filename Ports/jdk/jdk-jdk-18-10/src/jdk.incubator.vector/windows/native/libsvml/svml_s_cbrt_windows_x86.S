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
	PUBLIC __svml_cbrtf4_ha_l9

__svml_cbrtf4_ha_l9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 248
        vmovaps   xmm4, xmm0
        vmovups   XMMWORD PTR [192+rsp], xmm9
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [208+rsp], xmm8
        vpsrld    xmm8, xmm4, 16
        mov       QWORD PTR [224+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vpand     xmm0, xmm8, XMMWORD PTR [__svml_scbrt_ha_data_internal+1408]
        and       r13, -64
        vmovd     eax, xmm0
        vpextrd   r8d, xmm0, 2
        movsxd    rax, eax
        vpextrd   ecx, xmm0, 1
        movsxd    r8, r8d
        vpextrd   r9d, xmm0, 3
        movsxd    rcx, ecx
        movsxd    r9, r9d
        vmovd     xmm1, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rax]
        vmovd     xmm2, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r8]
        vpinsrd   xmm5, xmm1, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rcx], 1
        vpinsrd   xmm1, xmm2, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r9], 1
        vandps    xmm2, xmm4, XMMWORD PTR [__svml_scbrt_ha_data_internal+1792]
        vpsubd    xmm9, xmm2, XMMWORD PTR [__svml_scbrt_ha_data_internal+1856]
        vandps    xmm2, xmm4, XMMWORD PTR [__svml_scbrt_ha_data_internal+1152]
        vorps     xmm2, xmm2, XMMWORD PTR [__svml_scbrt_ha_data_internal+1280]
        vpunpcklqdq xmm3, xmm5, xmm1
        vpsrld    xmm5, xmm8, 7
        vpcmpgtd  xmm1, xmm9, XMMWORD PTR [__svml_scbrt_ha_data_internal+1920]
        vandps    xmm9, xmm4, XMMWORD PTR [__svml_scbrt_ha_data_internal+1216]
        vorps     xmm9, xmm9, XMMWORD PTR [__svml_scbrt_ha_data_internal+1344]
        vpand     xmm8, xmm5, XMMWORD PTR [__svml_scbrt_ha_data_internal+1472]
        vsubps    xmm2, xmm2, xmm9
        vmovmskps eax, xmm1
        vpsubd    xmm9, xmm8, XMMWORD PTR [__svml_scbrt_ha_data_internal+1664]
        vmulps    xmm2, xmm3, xmm2
        vpmulld   xmm3, xmm8, XMMWORD PTR [__svml_scbrt_ha_data_internal+1728]
        vpsrld    xmm3, xmm3, 12
        vpand     xmm5, xmm5, XMMWORD PTR [__svml_scbrt_ha_data_internal+1536]
        vpaddd    xmm8, xmm3, XMMWORD PTR [__svml_scbrt_ha_data_internal+1600]
        vpor      xmm5, xmm8, xmm5
        vpslld    xmm8, xmm5, 23
        vpsubd    xmm5, xmm9, xmm3
        vpsubd    xmm9, xmm5, xmm3
        vpsubd    xmm3, xmm9, xmm3
        vpslld    xmm3, xmm3, 7
        vpaddd    xmm0, xmm0, xmm3
        vpandn    xmm1, xmm1, xmm0
        vpslld    xmm5, xmm1, 1
        vmovd     r10d, xmm5
        vpextrd   r11d, xmm5, 1
        vpextrd   ecx, xmm5, 2
        vpextrd   r8d, xmm5, 3
        movsxd    r10, r10d
        movsxd    r11, r11d
        movsxd    rcx, ecx
        movsxd    r8, r8d
        vmovq     xmm9, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r10]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r11]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+rcx]
        vmovq     xmm3, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r8]
        vunpcklps xmm5, xmm9, xmm1
        vunpcklps xmm0, xmm0, xmm3
        vmovlhps  xmm1, xmm5, xmm0
        vshufps   xmm0, xmm5, xmm0, 238
        vmulps    xmm1, xmm8, xmm1
        vmulps    xmm0, xmm8, xmm0
        vmovups   xmm8, XMMWORD PTR [__svml_scbrt_ha_data_internal+896]
        vfmadd213ps xmm8, xmm2, XMMWORD PTR [__svml_scbrt_ha_data_internal+960]
        vfmadd213ps xmm8, xmm2, XMMWORD PTR [__svml_scbrt_ha_data_internal+1024]
        vfmadd213ps xmm8, xmm2, XMMWORD PTR [__svml_scbrt_ha_data_internal+1088]
        vmulps    xmm2, xmm2, xmm1
        vmulps    xmm2, xmm8, xmm2
        mov       QWORD PTR [232+rsp], r13
        vaddps    xmm3, xmm0, xmm2
        vaddps    xmm0, xmm1, xmm3
        test      eax, eax
        jne       _B1_3

_B1_2::

        vmovups   xmm8, XMMWORD PTR [208+rsp]
        vmovups   xmm9, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [224+rsp]
        add       rsp, 248
        ret

_B1_3::

        vmovups   XMMWORD PTR [r13], xmm4
        vmovups   XMMWORD PTR [64+r13], xmm0

_B1_6::

        xor       edx, edx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, edx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

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

        call      __svml_scbrt_ha_cout_rare_internal
        jmp       _B1_8
        ALIGN     16

_B1_11::

__svml_cbrtf4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrtf4_ha_l9_B1_B3:
	DD	537857
	DD	1889333
	DD	886824
	DD	825368
	DD	2031883

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_cbrtf4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrtf4_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_cbrtf4_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_6
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_cbrtf4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_cbrtf4_ha_ex

__svml_cbrtf4_ha_ex	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L12::

        sub       rsp, 264
        movaps    xmm5, xmm0
        movups    XMMWORD PTR [192+rsp], xmm15
        movaps    xmm2, xmm5
        movups    XMMWORD PTR [208+rsp], xmm10
        psrld     xmm2, 16
        movups    XMMWORD PTR [224+rsp], xmm9
        lea       rdx, QWORD PTR [__ImageBase]
        mov       QWORD PTR [240+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movdqu    xmm1, XMMWORD PTR [__svml_scbrt_ha_data_internal+1408]
        and       r13, -64
        pand      xmm1, xmm2
        psrld     xmm2, 7
        pshufd    xmm10, xmm1, 1
        movd      eax, xmm1
        pshufd    xmm0, xmm1, 2
        movups    xmm3, XMMWORD PTR [__svml_scbrt_ha_data_internal+1152]
        movd      ecx, xmm10
        andps     xmm3, xmm5
        pshufd    xmm10, xmm1, 3
        movups    xmm15, XMMWORD PTR [__svml_scbrt_ha_data_internal+1216]
        movd      r8d, xmm0
        andps     xmm15, xmm5
        movd      r9d, xmm10
        orps      xmm3, XMMWORD PTR [__svml_scbrt_ha_data_internal+1280]
        orps      xmm15, XMMWORD PTR [__svml_scbrt_ha_data_internal+1344]
        movsxd    rax, eax
        subps     xmm3, xmm15
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movsxd    r9, r9d
        movd      xmm4, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rax]
        movd      xmm9, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rcx]
        punpckldq xmm4, xmm9
        movd      xmm0, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r8]
        movd      xmm9, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r9]
        punpckldq xmm0, xmm9
        punpcklqdq xmm4, xmm0
        movdqu    xmm0, XMMWORD PTR [__svml_scbrt_ha_data_internal+1472]
        mulps     xmm4, xmm3
        pand      xmm0, xmm2
        movdqu    xmm3, XMMWORD PTR [__svml_scbrt_ha_data_internal+1728]
        movdqa    xmm15, xmm0
        movdqa    xmm10, xmm3
        psrlq     xmm3, 32
        pmuludq   xmm10, xmm0
        psrlq     xmm0, 32
        pmuludq   xmm0, xmm3
        pand      xmm10, XMMWORD PTR [_2il0floatpacket_46]
        psllq     xmm0, 32
        por       xmm10, xmm0
        psubd     xmm15, XMMWORD PTR [__svml_scbrt_ha_data_internal+1664]
        psrld     xmm10, 12
        psubd     xmm15, xmm10
        movdqu    xmm9, XMMWORD PTR [__svml_scbrt_ha_data_internal+1792]
        psubd     xmm15, xmm10
        pand      xmm9, xmm5
        psubd     xmm15, xmm10
        psubd     xmm9, XMMWORD PTR [__svml_scbrt_ha_data_internal+1856]
        pslld     xmm15, 7
        pcmpgtd   xmm9, XMMWORD PTR [__svml_scbrt_ha_data_internal+1920]
        paddd     xmm1, xmm15
        movmskps  eax, xmm9
        pandn     xmm9, xmm1
        movdqu    xmm3, XMMWORD PTR [__svml_scbrt_ha_data_internal+1600]
        pslld     xmm9, 1
        pand      xmm2, XMMWORD PTR [__svml_scbrt_ha_data_internal+1536]
        paddd     xmm3, xmm10
        por       xmm3, xmm2
        pshufd    xmm2, xmm9, 1
        pslld     xmm3, 23
        movd      r10d, xmm9
        pshufd    xmm10, xmm9, 2
        pshufd    xmm0, xmm9, 3
        movd      r11d, xmm2
        movd      ecx, xmm10
        movd      r8d, xmm0
        movsxd    r10, r10d
        movsxd    r11, r11d
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movq      xmm1, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r10]
        movq      xmm15, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r11]
        movq      xmm10, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+rcx]
        movq      xmm2, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r8]
        unpcklps  xmm1, xmm15
        unpcklps  xmm10, xmm2
        movaps    xmm0, xmm1
        movlhps   xmm0, xmm10
        shufps    xmm1, xmm10, 238
        mulps     xmm0, xmm3
        mulps     xmm3, xmm1
        movups    xmm1, XMMWORD PTR [__svml_scbrt_ha_data_internal+896]
        mulps     xmm1, xmm4
        mov       QWORD PTR [248+rsp], r13
        addps     xmm1, XMMWORD PTR [__svml_scbrt_ha_data_internal+960]
        mulps     xmm1, xmm4
        addps     xmm1, XMMWORD PTR [__svml_scbrt_ha_data_internal+1024]
        mulps     xmm1, xmm4
        mulps     xmm4, xmm0
        addps     xmm1, XMMWORD PTR [__svml_scbrt_ha_data_internal+1088]
        mulps     xmm1, xmm4
        addps     xmm3, xmm1
        addps     xmm0, xmm3
        test      eax, eax
        jne       _B2_3

_B2_2::

        movups    xmm9, XMMWORD PTR [224+rsp]
        movups    xmm10, XMMWORD PTR [208+rsp]
        movups    xmm15, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [240+rsp]
        add       rsp, 264
        ret

_B2_3::

        movups    XMMWORD PTR [r13], xmm5
        movups    XMMWORD PTR [64+r13], xmm0

_B2_6::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

_B2_7::

        mov       ecx, ebx
        mov       edx, 1
        shl       edx, cl
        test      esi, edx
        jne       _B2_10

_B2_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B2_7

_B2_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [64+r13]
        jmp       _B2_2

_B2_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_scbrt_ha_cout_rare_internal
        jmp       _B2_8
        ALIGN     16

_B2_11::

__svml_cbrtf4_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrtf4_ha_ex_B1_B3:
	DD	671745
	DD	2020416
	DD	956465
	DD	895011
	DD	849943
	DD	2162955

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_cbrtf4_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrtf4_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_cbrtf4_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_6
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_cbrtf4_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_cbrtf4_ha_e9

__svml_cbrtf4_ha_e9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L25::

        sub       rsp, 248
        vmovaps   xmm4, xmm0
        vmovups   XMMWORD PTR [192+rsp], xmm8
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [208+rsp], xmm7
        vpsrld    xmm7, xmm4, 16
        mov       QWORD PTR [224+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vpand     xmm0, xmm7, XMMWORD PTR [__svml_scbrt_ha_data_internal+1408]
        and       r13, -64
        vmovd     eax, xmm0
        vpextrd   r8d, xmm0, 2
        movsxd    rax, eax
        vpextrd   ecx, xmm0, 1
        movsxd    r8, r8d
        vpextrd   r9d, xmm0, 3
        movsxd    rcx, ecx
        movsxd    r9, r9d
        vmovd     xmm1, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rax]
        vmovd     xmm2, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r8]
        vpinsrd   xmm5, xmm1, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rcx], 1
        vpinsrd   xmm1, xmm2, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r9], 1
        vandps    xmm2, xmm4, XMMWORD PTR [__svml_scbrt_ha_data_internal+1792]
        vpsubd    xmm8, xmm2, XMMWORD PTR [__svml_scbrt_ha_data_internal+1856]
        vandps    xmm2, xmm4, XMMWORD PTR [__svml_scbrt_ha_data_internal+1152]
        vorps     xmm2, xmm2, XMMWORD PTR [__svml_scbrt_ha_data_internal+1280]
        vpunpcklqdq xmm3, xmm5, xmm1
        vpsrld    xmm5, xmm7, 7
        vpcmpgtd  xmm1, xmm8, XMMWORD PTR [__svml_scbrt_ha_data_internal+1920]
        vandps    xmm8, xmm4, XMMWORD PTR [__svml_scbrt_ha_data_internal+1216]
        vorps     xmm8, xmm8, XMMWORD PTR [__svml_scbrt_ha_data_internal+1344]
        vpand     xmm7, xmm5, XMMWORD PTR [__svml_scbrt_ha_data_internal+1472]
        vsubps    xmm2, xmm2, xmm8
        vmovmskps eax, xmm1
        vpsubd    xmm8, xmm7, XMMWORD PTR [__svml_scbrt_ha_data_internal+1664]
        vmulps    xmm2, xmm3, xmm2
        vpmulld   xmm3, xmm7, XMMWORD PTR [__svml_scbrt_ha_data_internal+1728]
        vpsrld    xmm3, xmm3, 12
        vpand     xmm5, xmm5, XMMWORD PTR [__svml_scbrt_ha_data_internal+1536]
        vpsubd    xmm8, xmm8, xmm3
        vpaddd    xmm7, xmm3, XMMWORD PTR [__svml_scbrt_ha_data_internal+1600]
        vpor      xmm5, xmm7, xmm5
        vpslld    xmm7, xmm5, 23
        vpsubd    xmm5, xmm8, xmm3
        vpsubd    xmm3, xmm5, xmm3
        vpslld    xmm8, xmm3, 7
        vpaddd    xmm0, xmm0, xmm8
        vpandn    xmm0, xmm1, xmm0
        vpslld    xmm1, xmm0, 1
        vmovd     r10d, xmm1
        vpextrd   r11d, xmm1, 1
        vpextrd   ecx, xmm1, 2
        vpextrd   r8d, xmm1, 3
        movsxd    r10, r10d
        movsxd    r11, r11d
        movsxd    rcx, ecx
        movsxd    r8, r8d
        vmovq     xmm3, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r10]
        vmovq     xmm5, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r11]
        vmovq     xmm8, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+rcx]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r8]
        vunpcklps xmm1, xmm3, xmm5
        vunpcklps xmm8, xmm8, xmm0
        vmovlhps  xmm0, xmm1, xmm8
        vshufps   xmm3, xmm1, xmm8, 238
        vmulps    xmm0, xmm7, xmm0
        vmulps    xmm1, xmm7, xmm3
        vmulps    xmm7, xmm2, XMMWORD PTR [__svml_scbrt_ha_data_internal+896]
        mov       QWORD PTR [232+rsp], r13
        vaddps    xmm3, xmm7, XMMWORD PTR [__svml_scbrt_ha_data_internal+960]
        vmulps    xmm5, xmm2, xmm3
        vaddps    xmm7, xmm5, XMMWORD PTR [__svml_scbrt_ha_data_internal+1024]
        vmulps    xmm8, xmm2, xmm7
        vmulps    xmm2, xmm2, xmm0
        vaddps    xmm3, xmm8, XMMWORD PTR [__svml_scbrt_ha_data_internal+1088]
        vmulps    xmm2, xmm3, xmm2
        vaddps    xmm1, xmm1, xmm2
        vaddps    xmm0, xmm0, xmm1
        test      eax, eax
        jne       _B3_3

_B3_2::

        vmovups   xmm7, XMMWORD PTR [208+rsp]
        vmovups   xmm8, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [224+rsp]
        add       rsp, 248
        ret

_B3_3::

        vmovups   XMMWORD PTR [r13], xmm4
        vmovups   XMMWORD PTR [64+r13], xmm0

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
        vmovups   xmm0, XMMWORD PTR [64+r13]
        jmp       _B3_2

_B3_10::

        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_scbrt_ha_cout_rare_internal
        jmp       _B3_8
        ALIGN     16

_B3_11::

__svml_cbrtf4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrtf4_ha_e9_B1_B3:
	DD	537857
	DD	1889333
	DD	882728
	DD	821272
	DD	2031883

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_cbrtf4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrtf4_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B3_1
	DD	imagerel _B3_6
	DD	imagerel _unwind___svml_cbrtf4_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_6
	DD	imagerel _B3_11
	DD	imagerel _unwind___svml_cbrtf4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_cbrtf16_ha_z0

__svml_cbrtf16_ha_z0	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L36::

        vgetexpps zmm4, zmm0 {sae}
        vmovups   zmm5, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+384]
        vgetmantps zmm27, zmm0, 0 {sae}
        vmovups   zmm22, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+512]
        vmovups   zmm23, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+576]
        vmovups   zmm24, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+640]
        vmovups   zmm31, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+256]
        vmovups   zmm30, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+768]
        vrcp14ps  zmm26, zmm27
        vaddps    zmm25, zmm4, zmm5 {rn-sae}
        vmovups   zmm4, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+704]
        vrndscaleps zmm28, zmm26, 88 {sae}
        vfmsub231ps zmm23, zmm22, zmm25 {rn-sae}
        vmovups   zmm5, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+832]
        vfmsub231ps zmm4, zmm27, zmm28 {rn-sae}
        vrndscaleps zmm3, zmm23, 9 {sae}
        vmovups   zmm27, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+128]
        vpsrld    zmm29, zmm28, 19
        vfmadd231ps zmm5, zmm30, zmm4 {rn-sae}
        vfnmadd231ps zmm25, zmm24, zmm3 {rn-sae}
        vmovups   zmm30, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+896]
        vpermt2ps zmm27, zmm29, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+192]
        vpermps   zmm1, zmm25, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512]
        vpermt2ps zmm31, zmm29, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+320]
        vpermps   zmm2, zmm25, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+64]
        vmulps    zmm22, zmm1, zmm27 {rn-sae}
        vfmadd213ps zmm5, zmm4, zmm30 {rn-sae}
        vmovaps   zmm29, zmm1
        vfmsub213ps zmm29, zmm27, zmm22 {rn-sae}
        vfmadd213ps zmm1, zmm31, zmm29 {rn-sae}
        vfmadd213ps zmm2, zmm27, zmm1 {rn-sae}
        vmulps    zmm1, zmm22, zmm4 {rn-sae}
        vfmadd213ps zmm1, zmm5, zmm2 {rn-sae}
        vaddps    zmm2, zmm1, zmm22 {rn-sae}
        vscalefps zmm3, zmm2, zmm3 {rn-sae}
        vpternlogd zmm0, zmm3, ZMMWORD PTR [__svml_scbrt_ha_data_internal_avx512+448], 236
        ret
        ALIGN     16

_B4_2::

__svml_cbrtf16_ha_z0 ENDP

_TEXT	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_cbrtf8_ha_l9

__svml_cbrtf8_ha_l9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L37::

        sub       rsp, 552
        lea       rdx, QWORD PTR [__ImageBase]
        vmovaps   ymm4, ymm0
        vpsrld    ymm1, ymm4, 16
        vmovups   YMMWORD PTR [368+rsp], ymm15
        vmovups   YMMWORD PTR [400+rsp], ymm14
        vmovups   YMMWORD PTR [432+rsp], ymm13
        vmovups   YMMWORD PTR [464+rsp], ymm12
        vmovups   YMMWORD PTR [496+rsp], ymm11
        vpsrld    ymm0, ymm1, 7
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [271+rsp]
        vpand     ymm5, ymm1, YMMWORD PTR [__svml_scbrt_ha_data_internal+1408]
        and       r13, -64
        vandps    ymm1, ymm4, YMMWORD PTR [__svml_scbrt_ha_data_internal+1792]
        mov       QWORD PTR [536+rsp], r13
        vmovd     eax, xmm5
        movsxd    rax, eax
        vpextrd   ecx, xmm5, 1
        movsxd    rcx, ecx
        vpextrd   r8d, xmm5, 2
        movsxd    r8, r8d
        vpextrd   r9d, xmm5, 3
        vmovd     xmm2, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rax]
        movsxd    r9, r9d
        vpinsrd   xmm14, xmm2, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rcx], 1
        vmovd     xmm3, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r8]
        vpinsrd   xmm15, xmm3, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r9], 1
        vextracti128 xmm2, ymm5, 1
        vpunpcklqdq xmm13, xmm14, xmm15
        vmovd     r10d, xmm2
        vpextrd   eax, xmm2, 2
        movsxd    r10, r10d
        vpextrd   r11d, xmm2, 1
        movsxd    rax, eax
        vpextrd   ecx, xmm2, 3
        movsxd    r11, r11d
        movsxd    rcx, ecx
        vmovd     xmm12, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r10]
        vmovd     xmm3, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rax]
        vpinsrd   xmm14, xmm12, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r11], 1
        vpinsrd   xmm15, xmm3, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rcx], 1
        vpand     ymm3, ymm0, YMMWORD PTR [__svml_scbrt_ha_data_internal+1472]
        vpand     ymm0, ymm0, YMMWORD PTR [__svml_scbrt_ha_data_internal+1536]
        vpunpcklqdq xmm11, xmm14, xmm15
        vpsubd    ymm14, ymm1, YMMWORD PTR [__svml_scbrt_ha_data_internal+1856]
        vandps    ymm15, ymm4, YMMWORD PTR [__svml_scbrt_ha_data_internal+1152]
        vorps     ymm1, ymm15, YMMWORD PTR [__svml_scbrt_ha_data_internal+1280]
        vpmulld   ymm15, ymm3, YMMWORD PTR [__svml_scbrt_ha_data_internal+1728]
        vinsertf128 ymm2, ymm13, xmm11, 1
        vandps    ymm11, ymm4, YMMWORD PTR [__svml_scbrt_ha_data_internal+1216]
        vorps     ymm12, ymm11, YMMWORD PTR [__svml_scbrt_ha_data_internal+1344]
        vpcmpgtd  ymm13, ymm14, YMMWORD PTR [__svml_scbrt_ha_data_internal+1920]
        vsubps    ymm14, ymm1, ymm12
        vpsrld    ymm11, ymm15, 12
        vmulps    ymm2, ymm2, ymm14
        vpsubd    ymm14, ymm3, YMMWORD PTR [__svml_scbrt_ha_data_internal+1664]
        vpaddd    ymm3, ymm11, YMMWORD PTR [__svml_scbrt_ha_data_internal+1600]
        vpsubd    ymm15, ymm14, ymm11
        vpor      ymm3, ymm3, ymm0
        vpsubd    ymm0, ymm15, ymm11
        vpslld    ymm3, ymm3, 23
        vpsubd    ymm11, ymm0, ymm11
        vpslld    ymm1, ymm11, 7
        vpaddd    ymm5, ymm5, ymm1
        vmovmskps eax, ymm13
        vpandn    ymm13, ymm13, ymm5
        vpslld    ymm11, ymm13, 1
        vmovd     r8d, xmm11
        vextracti128 xmm13, ymm11, 1
        movsxd    r8, r8d
        vpextrd   r9d, xmm11, 1
        vpextrd   ecx, xmm11, 3
        movsxd    r9, r9d
        movsxd    rcx, ecx
        vmovq     xmm14, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r8]
        vmovd     r8d, xmm13
        vmovq     xmm0, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r9]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+rcx]
        vpextrd   r10d, xmm11, 2
        vpextrd   r9d, xmm13, 1
        vpextrd   r11d, xmm13, 2
        vpextrd   ecx, xmm13, 3
        movsxd    r10, r10d
        movsxd    r8, r8d
        movsxd    r9, r9d
        movsxd    r11, r11d
        movsxd    rcx, ecx
        vmovq     xmm15, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r10]
        vmovq     xmm5, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r8]
        vmovq     xmm11, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r9]
        vmovq     xmm12, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r11]
        vmovq     xmm13, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+rcx]
        vunpcklps xmm15, xmm14, xmm15
        vunpcklps xmm14, xmm0, xmm1
        vunpcklps xmm0, xmm5, xmm12
        vunpcklps xmm1, xmm11, xmm13
        vinsertf128 ymm5, ymm15, xmm0, 1
        vinsertf128 ymm12, ymm14, xmm1, 1
        vunpcklps ymm0, ymm5, ymm12
        vunpckhps ymm1, ymm5, ymm12
        vmulps    ymm5, ymm3, ymm0
        vmulps    ymm1, ymm3, ymm1
        vmovups   ymm3, YMMWORD PTR [__svml_scbrt_ha_data_internal+896]
        vfmadd213ps ymm3, ymm2, YMMWORD PTR [__svml_scbrt_ha_data_internal+960]
        vfmadd213ps ymm3, ymm2, YMMWORD PTR [__svml_scbrt_ha_data_internal+1024]
        vfmadd213ps ymm3, ymm2, YMMWORD PTR [__svml_scbrt_ha_data_internal+1088]
        vmulps    ymm2, ymm2, ymm5
        vmulps    ymm2, ymm3, ymm2
        vaddps    ymm0, ymm1, ymm2
        vaddps    ymm0, ymm5, ymm0
        test      eax, eax
        jne       _B5_3

_B5_2::

        vmovups   ymm11, YMMWORD PTR [496+rsp]
        vmovups   ymm12, YMMWORD PTR [464+rsp]
        vmovups   ymm13, YMMWORD PTR [432+rsp]
        vmovups   ymm14, YMMWORD PTR [400+rsp]
        vmovups   ymm15, YMMWORD PTR [368+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B5_3::

        vmovups   YMMWORD PTR [r13], ymm4
        vmovups   YMMWORD PTR [64+r13], ymm0

_B5_6::

        xor       edx, edx
        vmovups   YMMWORD PTR [160+rsp], ymm6
        vmovups   YMMWORD PTR [128+rsp], ymm7
        vmovups   YMMWORD PTR [96+rsp], ymm8
        vmovups   YMMWORD PTR [64+rsp], ymm9
        vmovups   YMMWORD PTR [32+rsp], ymm10
        mov       QWORD PTR [200+rsp], rbx
        mov       ebx, edx
        mov       QWORD PTR [192+rsp], rsi
        mov       esi, eax

_B5_7::

        bt        esi, ebx
        jc        _B5_10

_B5_8::

        inc       ebx
        cmp       ebx, 8
        jl        _B5_7

_B5_9::

        vmovups   ymm6, YMMWORD PTR [160+rsp]
        vmovups   ymm7, YMMWORD PTR [128+rsp]
        vmovups   ymm8, YMMWORD PTR [96+rsp]
        vmovups   ymm9, YMMWORD PTR [64+rsp]
        vmovups   ymm10, YMMWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [200+rsp]
        mov       rsi, QWORD PTR [192+rsp]
        jmp       _B5_2

_B5_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_scbrt_ha_cout_rare_internal
        jmp       _B5_8
        ALIGN     16

_B5_11::

__svml_cbrtf8_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrtf8_ha_l9_B1_B3:
	DD	939265
	DD	4379733
	DD	2078792
	DD	1951807
	DD	1824822
	DD	1697837
	DD	1570852
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_cbrtf8_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrtf8_ha_l9_B6_B10:
	DD	931873
	DD	1598520
	DD	1651758
	DD	174118
	DD	301088
	DD	428058
	DD	555028
	DD	681995
	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_cbrtf8_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_6
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_cbrtf8_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_cbrtf8_ha_e9

__svml_cbrtf8_ha_e9	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L64::

        sub       rsp, 552
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [272+rsp], ymm15
        vmovups   YMMWORD PTR [304+rsp], ymm14
        vmovups   YMMWORD PTR [432+rsp], ymm13
        vmovups   YMMWORD PTR [208+rsp], ymm12
        vmovups   YMMWORD PTR [240+rsp], ymm11
        vmovups   YMMWORD PTR [336+rsp], ymm10
        vmovups   YMMWORD PTR [368+rsp], ymm9
        vmovups   YMMWORD PTR [400+rsp], ymm8
        vmovups   YMMWORD PTR [464+rsp], ymm7
        vmovups   YMMWORD PTR [496+rsp], ymm6
        vmovaps   ymm15, ymm0
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovups   xmm11, XMMWORD PTR [__svml_scbrt_ha_data_internal+1408]
        and       r13, -64
        vmovups   xmm4, XMMWORD PTR [__svml_scbrt_ha_data_internal+1792]
        vmovups   xmm5, XMMWORD PTR [__svml_scbrt_ha_data_internal+1856]
        vmovups   xmm6, XMMWORD PTR [__svml_scbrt_ha_data_internal+1920]
        vmovups   xmm0, XMMWORD PTR [__svml_scbrt_ha_data_internal+1472]
        vmovups   xmm1, XMMWORD PTR [__svml_scbrt_ha_data_internal+1536]
        vmovups   xmm3, XMMWORD PTR [__svml_scbrt_ha_data_internal+1600]
        vmovups   xmm2, XMMWORD PTR [__svml_scbrt_ha_data_internal+1664]
        mov       QWORD PTR [536+rsp], r13
        vpsrld    xmm8, xmm15, 16
        vpand     xmm10, xmm8, xmm11
        vpsrld    xmm8, xmm8, 7
        vmovd     eax, xmm10
        vextractf128 xmm7, ymm15, 1
        vpextrd   r8d, xmm10, 2
        vpsrld    xmm9, xmm7, 16
        movsxd    rax, eax
        vpand     xmm11, xmm9, xmm11
        vpextrd   ecx, xmm10, 1
        vpsrld    xmm9, xmm9, 7
        movsxd    r8, r8d
        vpextrd   r9d, xmm10, 3
        movsxd    rcx, ecx
        movsxd    r9, r9d
        vmovd     xmm14, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rax]
        vmovd     xmm13, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r8]
        vpinsrd   xmm14, xmm14, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rcx], 1
        vpinsrd   xmm12, xmm13, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r9], 1
        vmovd     r10d, xmm11
        vpunpcklqdq xmm12, xmm14, xmm12
        movsxd    r10, r10d
        vpextrd   r11d, xmm11, 1
        vpextrd   eax, xmm11, 2
        movsxd    r11, r11d
        movsxd    rax, eax
        vpextrd   ecx, xmm11, 3
        movsxd    rcx, ecx
        vmovd     xmm14, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r10]
        vpinsrd   xmm13, xmm14, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+r11], 1
        vmovd     xmm14, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rax]
        vpinsrd   xmm14, xmm14, DWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+rdx+rcx], 1
        vpunpcklqdq xmm13, xmm13, xmm14
        vpand     xmm14, xmm15, xmm4
        vandps    xmm4, xmm7, xmm4
        vpsubd    xmm14, xmm14, xmm5
        vpsubd    xmm5, xmm4, xmm5
        vpcmpgtd  xmm14, xmm14, xmm6
        vpcmpgtd  xmm7, xmm5, xmm6
        vpackssdw xmm4, xmm14, xmm7
        vpxor     xmm5, xmm5, xmm5
        vpacksswb xmm6, xmm4, xmm5
        vandps    ymm4, ymm15, YMMWORD PTR [__svml_scbrt_ha_data_internal+1152]
        vandps    ymm5, ymm15, YMMWORD PTR [__svml_scbrt_ha_data_internal+1216]
        vpmovmskb eax, xmm6
        vorps     ymm6, ymm4, YMMWORD PTR [__svml_scbrt_ha_data_internal+1280]
        vorps     ymm4, ymm5, YMMWORD PTR [__svml_scbrt_ha_data_internal+1344]
        vsubps    ymm5, ymm6, ymm4
        vinsertf128 ymm12, ymm12, xmm13, 1
        vpand     xmm13, xmm8, xmm0
        vmulps    ymm4, ymm12, ymm5
        vpand     xmm0, xmm9, xmm0
        vmovups   xmm5, XMMWORD PTR [__svml_scbrt_ha_data_internal+1728]
        vpand     xmm9, xmm9, xmm1
        vpmulld   xmm12, xmm13, xmm5
        vpsrld    xmm6, xmm12, 12
        vpmulld   xmm12, xmm0, xmm5
        vpsrld    xmm5, xmm12, 12
        vpand     xmm12, xmm8, xmm1
        vpaddd    xmm8, xmm6, xmm3
        vpaddd    xmm1, xmm3, xmm5
        vpor      xmm8, xmm8, xmm12
        vpor      xmm3, xmm1, xmm9
        vpslld    xmm8, xmm8, 23
        vpslld    xmm1, xmm3, 23
        vinsertf128 ymm3, ymm8, xmm1, 1
        vpsubd    xmm8, xmm13, xmm2
        vpsubd    xmm12, xmm8, xmm6
        vpsubd    xmm2, xmm0, xmm2
        vpsubd    xmm13, xmm12, xmm6
        vpsubd    xmm0, xmm2, xmm5
        vpsubd    xmm6, xmm13, xmm6
        vpslld    xmm1, xmm6, 7
        vpaddd    xmm10, xmm10, xmm1
        vpsubd    xmm1, xmm0, xmm5
        vpandn    xmm14, xmm14, xmm10
        vpsubd    xmm2, xmm1, xmm5
        vpslld    xmm10, xmm14, 1
        vpslld    xmm5, xmm2, 7
        vmovd     r8d, xmm10
        vpaddd    xmm6, xmm11, xmm5
        vpandn    xmm7, xmm7, xmm6
        vpslld    xmm8, xmm7, 1
        movsxd    r8, r8d
        vpextrd   r9d, xmm10, 1
        vpextrd   ecx, xmm10, 3
        movsxd    r9, r9d
        movsxd    rcx, ecx
        vmovq     xmm9, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r8]
        vmovd     r8d, xmm8
        vmovq     xmm12, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r9]
        vpextrd   r10d, xmm10, 2
        vmovq     xmm10, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+rcx]
        vpextrd   r9d, xmm8, 1
        vpextrd   r11d, xmm8, 2
        vpextrd   ecx, xmm8, 3
        movsxd    r10, r10d
        movsxd    r8, r8d
        movsxd    r9, r9d
        movsxd    r11, r11d
        movsxd    rcx, ecx
        vmovq     xmm11, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r10]
        vmovq     xmm13, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r8]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r9]
        vmovq     xmm14, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+r11]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_scbrt_ha_data_internal)+128+rdx+rcx]
        vunpcklps xmm2, xmm9, xmm11
        vunpcklps xmm5, xmm12, xmm10
        vunpcklps xmm13, xmm13, xmm14
        vunpcklps xmm6, xmm0, xmm1
        vinsertf128 ymm7, ymm2, xmm13, 1
        vinsertf128 ymm8, ymm5, xmm6, 1
        vunpcklps ymm9, ymm7, ymm8
        vunpckhps ymm10, ymm7, ymm8
        vmulps    ymm11, ymm3, ymm9
        vmulps    ymm5, ymm3, ymm10
        vmulps    ymm3, ymm4, YMMWORD PTR [__svml_scbrt_ha_data_internal+896]
        vaddps    ymm3, ymm3, YMMWORD PTR [__svml_scbrt_ha_data_internal+960]
        vmulps    ymm10, ymm4, ymm3
        vaddps    ymm0, ymm10, YMMWORD PTR [__svml_scbrt_ha_data_internal+1024]
        vmulps    ymm1, ymm4, ymm0
        vmulps    ymm4, ymm4, ymm11
        vaddps    ymm2, ymm1, YMMWORD PTR [__svml_scbrt_ha_data_internal+1088]
        vmulps    ymm0, ymm2, ymm4
        vaddps    ymm1, ymm5, ymm0
        vaddps    ymm0, ymm11, ymm1
        test      al, al
        jne       _B6_3

_B6_2::

        vmovups   ymm6, YMMWORD PTR [496+rsp]
        vmovups   ymm7, YMMWORD PTR [464+rsp]
        vmovups   ymm8, YMMWORD PTR [400+rsp]
        vmovups   ymm9, YMMWORD PTR [368+rsp]
        vmovups   ymm10, YMMWORD PTR [336+rsp]
        vmovups   ymm11, YMMWORD PTR [240+rsp]
        vmovups   ymm12, YMMWORD PTR [208+rsp]
        vmovups   ymm13, YMMWORD PTR [432+rsp]
        vmovups   ymm14, YMMWORD PTR [304+rsp]
        vmovups   ymm15, YMMWORD PTR [272+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B6_3::

        vmovups   YMMWORD PTR [r13], ymm15
        vmovups   YMMWORD PTR [64+r13], ymm0
        test      eax, eax
        je        _B6_2

_B6_6::

        xor       edx, edx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, edx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

_B6_7::

        bt        esi, ebx
        jc        _B6_10

_B6_8::

        inc       ebx
        cmp       ebx, 8
        jl        _B6_7

_B6_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovups   ymm0, YMMWORD PTR [64+r13]
        jmp       _B6_2

_B6_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*4]
        lea       rdx, QWORD PTR [64+r13+rbx*4]

        call      __svml_scbrt_ha_cout_rare_internal
        jmp       _B6_8
        ALIGN     16

_B6_11::

__svml_cbrtf8_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrtf8_ha_e9_B1_B3:
	DD	1603585
	DD	4379768
	DD	2058348
	DD	1931363
	DD	1673306
	DD	1546321
	DD	1419336
	DD	1030207
	DD	903222
	DD	1824813
	DD	1304612
	DD	1177627
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_cbrtf8_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_cbrtf8_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_cbrtf8_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_6
	DD	imagerel _B6_11
	DD	imagerel _unwind___svml_cbrtf8_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_scbrt_ha_cout_rare_internal

__svml_scbrt_ha_cout_rare_internal	PROC

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L91::

        push      rbx
        push      rbp
        sub       rsp, 56
        mov       r9, rdx
        movss     xmm0, DWORD PTR [rcx]
        lea       r8, QWORD PTR [__ImageBase]
        movss     xmm2, DWORD PTR [ione]
        mulss     xmm2, xmm0
        movss     DWORD PTR [52+rsp], xmm2
        movzx     eax, WORD PTR [54+rsp]
        and       eax, 32640
        shr       eax, 7
        cmp       eax, 255
        je        _B7_9

_B7_2::

        pxor      xmm0, xmm0
        cvtss2sd  xmm0, xmm2
        pxor      xmm1, xmm1
        ucomisd   xmm0, xmm1
        jp        _B7_3
        je        _B7_8

_B7_3::

        test      eax, eax
        jne       _B7_5

_B7_4::

        mov       DWORD PTR [32+rsp], 2122317824
        mov       DWORD PTR [36+rsp], 713031680
        jmp       _B7_6

_B7_5::

        mov       eax, 1065353216
        mov       DWORD PTR [32+rsp], eax
        mov       DWORD PTR [36+rsp], eax

_B7_6::

        movss     xmm0, DWORD PTR [32+rsp]
        mulss     xmm2, xmm0
        movd      ecx, xmm2
        movss     DWORD PTR [52+rsp], xmm2
        mov       r10d, ecx
        mov       ebx, ecx
        and       r10d, 8388607
        mov       r11d, ecx
        shr       ebx, 23
        and       r11d, 8257536
        or        r10d, -1082130432
        or        r11d, -1081999360
        mov       DWORD PTR [40+rsp], r10d
        mov       edx, ecx
        movzx     ebp, bl
        and       ecx, 2147483647
        mov       DWORD PTR [44+rsp], r11d
        and       ebx, -256
        movss     xmm2, DWORD PTR [40+rsp]
        add       ecx, 2139095040
        shr       edx, 16
        subss     xmm2, DWORD PTR [44+rsp]
        and       edx, 124
        lea       r10d, DWORD PTR [rbp+rbp*4]
        mulss     xmm2, DWORD PTR [imagerel(vscbrt_ha_cout_data)+r8+rdx]
        lea       r11d, DWORD PTR [r10+r10]
        movss     xmm5, DWORD PTR [_2il0floatpacket_51]
        lea       eax, DWORD PTR [r11+r11]
        add       eax, eax
        lea       r10d, DWORD PTR [r10+r11*8]
        add       eax, eax
        dec       ebp
        mulss     xmm5, xmm2
        shl       ebp, 7
        lea       r11d, DWORD PTR [r10+rax*8]
        lea       r10d, DWORD PTR [r11+rax*8]
        shr       r10d, 12
        addss     xmm5, DWORD PTR [_2il0floatpacket_52]
        mulss     xmm5, xmm2
        lea       eax, DWORD PTR [85+r10]
        or        eax, ebx
        xor       ebx, ebx
        cmp       ecx, -16777217
        addss     xmm5, DWORD PTR [_2il0floatpacket_53]
        setg      bl
        shl       r10d, 7
        neg       ebx
        sub       ebp, r10d
        add       r10d, r10d
        sub       ebp, r10d
        not       ebx
        add       edx, ebp
        and       ebx, edx
        shl       eax, 23
        add       ebx, ebx
        mov       DWORD PTR [48+rsp], eax
        movss     xmm4, DWORD PTR [imagerel(vscbrt_ha_cout_data)+128+r8+rbx]
        movss     xmm1, DWORD PTR [48+rsp]
        mulss     xmm5, xmm2
        mulss     xmm4, xmm1
        addss     xmm5, DWORD PTR [_2il0floatpacket_54]
        mulss     xmm2, xmm4
        movss     xmm3, DWORD PTR [imagerel(vscbrt_ha_cout_data)+132+r8+rbx]
        mulss     xmm3, xmm1
        mulss     xmm5, xmm2
        addss     xmm5, xmm3
        addss     xmm5, xmm4
        mulss     xmm5, DWORD PTR [36+rsp]
        movss     DWORD PTR [r9], xmm5

_B7_7::

        xor       eax, eax
        add       rsp, 56
        pop       rbp
        pop       rbx
        ret

_B7_8::

        movss     DWORD PTR [r9], xmm2
        jmp       _B7_7

_B7_9::

        addss     xmm0, xmm0
        movss     DWORD PTR [r9], xmm0
        jmp       _B7_7
        ALIGN     16

_B7_10::

__svml_scbrt_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_scbrt_ha_cout_rare_internal_B1_B9:
	DD	199169
	DD	1342595594
	DD	12293

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_10
	DD	imagerel _unwind___svml_scbrt_ha_cout_rare_internal_B1_B9

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_DATA	SEGMENT      'DATA'
	ALIGN 004H
ione	DD	1065353216
	DD	-1082130432
_DATA	ENDS
_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
vscbrt_ha_cout_data	DD	3212578753
	DD	3212085645
	DD	3211621124
	DD	3211182772
	DD	3210768440
	DD	3210376206
	DD	3210004347
	DD	3209651317
	DD	3209315720
	DD	3208996296
	DD	3208691905
	DD	3208401508
	DD	3208124163
	DD	3207859009
	DD	3207605259
	DD	3207362194
	DD	3207129151
	DD	3206905525
	DD	3206690755
	DD	3206484326
	DD	3206285761
	DD	3206094618
	DD	3205910490
	DD	3205732998
	DD	3205561788
	DD	3205396533
	DD	3205236929
	DD	3205082689
	DD	3204933547
	DD	3204789256
	DD	3204649583
	DD	3204514308
	DD	1065396681
	DD	839340838
	DD	1065482291
	DD	867750258
	DD	1065566215
	DD	851786446
	DD	1065648532
	DD	853949398
	DD	1065729317
	DD	864938789
	DD	1065808640
	DD	864102364
	DD	1065886565
	DD	864209792
	DD	1065963152
	DD	865422805
	DD	1066038457
	DD	867593594
	DD	1066112533
	DD	854482593
	DD	1066185428
	DD	848298042
	DD	1066257188
	DD	860064854
	DD	1066327857
	DD	844792593
	DD	1066397474
	DD	870701309
	DD	1066466079
	DD	872023170
	DD	1066533708
	DD	860255342
	DD	1066600394
	DD	849966899
	DD	1066666169
	DD	863561479
	DD	1066731064
	DD	869115319
	DD	1066795108
	DD	871961375
	DD	1066858329
	DD	859537336
	DD	1066920751
	DD	871954398
	DD	1066982401
	DD	863817578
	DD	1067043301
	DD	861687921
	DD	1067103474
	DD	849594757
	DD	1067162941
	DD	816486846
	DD	1067221722
	DD	858183533
	DD	1067279837
	DD	864500406
	DD	1067337305
	DD	850523240
	DD	1067394143
	DD	808125243
	DD	1067450368
	DD	0
	DD	1067505996
	DD	861173761
	DD	1067588354
	DD	859000219
	DD	1067696217
	DD	823158129
	DD	1067801953
	DD	871826232
	DD	1067905666
	DD	871183196
	DD	1068007450
	DD	839030530
	DD	1068107390
	DD	867690638
	DD	1068205570
	DD	840440923
	DD	1068302063
	DD	868033274
	DD	1068396942
	DD	855856030
	DD	1068490271
	DD	865094453
	DD	1068582113
	DD	860418487
	DD	1068672525
	DD	866225006
	DD	1068761562
	DD	866458226
	DD	1068849275
	DD	865124659
	DD	1068935712
	DD	864837702
	DD	1069020919
	DD	811742505
	DD	1069104937
	DD	869432099
	DD	1069187809
	DD	864584201
	DD	1069269572
	DD	864183978
	DD	1069350263
	DD	844810573
	DD	1069429915
	DD	869245699
	DD	1069508563
	DD	859556409
	DD	1069586236
	DD	870675446
	DD	1069662966
	DD	814190139
	DD	1069738778
	DD	870686941
	DD	1069813702
	DD	861800510
	DD	1069887762
	DD	855649163
	DD	1069960982
	DD	869347119
	DD	1070033387
	DD	864252033
	DD	1070104998
	DD	867276215
	DD	1070175837
	DD	868189817
	DD	1070245925
	DD	849541095
	DD	1070349689
	DD	866633177
	DD	1070485588
	DD	843967686
	DD	1070618808
	DD	857522493
	DD	1070749478
	DD	862339487
	DD	1070877717
	DD	850054662
	DD	1071003634
	DD	864048556
	DD	1071127332
	DD	868027089
	DD	1071248907
	DD	848093931
	DD	1071368446
	DD	865355299
	DD	1071486034
	DD	848111485
	DD	1071601747
	DD	865557362
	DD	1071715659
	DD	870297525
	DD	1071827839
	DD	863416216
	DD	1071938350
	DD	869675693
	DD	1072047254
	DD	865888071
	DD	1072154608
	DD	825332584
	DD	1072260465
	DD	843309506
	DD	1072364876
	DD	870885636
	DD	1072467891
	DD	869119784
	DD	1072569555
	DD	865466648
	DD	1072669911
	DD	867459244
	DD	1072769001
	DD	861192764
	DD	1072866863
	DD	871247716
	DD	1072963536
	DD	864927982
	DD	1073059054
	DD	869195129
	DD	1073153452
	DD	864849564
	DD	1073246762
	DD	840005936
	DD	1073339014
	DD	852579258
	DD	1073430238
	DD	860852782
	DD	1073520462
	DD	869711141
	DD	1073609714
	DD	862506141
	DD	1073698019
	DD	837959274
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
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
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
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
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
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
	PUBLIC __svml_scbrt_ha_data_internal_avx512
__svml_scbrt_ha_data_internal_avx512	DD	1065353216
	DD	1067533592
	DD	1070280693
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2999865775
	DD	849849800
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1067533592
	DD	1067322155
	DD	1067126683
	DD	1066945178
	DD	1066775983
	DD	1066617708
	DD	1066469175
	DD	1066329382
	DD	1066197466
	DD	1066072682
	DD	1065954382
	DD	1065841998
	DD	1065735031
	DD	1065633040
	DD	1065535634
	DD	1065442463
	DD	1065353216
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	2999865775
	DD	849353281
	DD	2992093760
	DD	858369405
	DD	861891413
	DD	3001900484
	DD	2988845984
	DD	3009185201
	DD	3001209163
	DD	847824101
	DD	839380496
	DD	845124191
	DD	851391835
	DD	856440803
	DD	2989578734
	DD	852890174
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
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
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1249902592
	DD	1077936128
	DD	1077936128
	DD	1077936128
	DD	1077936128
	DD	1077936128
	DD	1077936128
	DD	1077936128
	DD	1077936128
	DD	1077936128
	DD	1077936128
	DD	1077936128
	DD	1077936128
	DD	1077936128
	DD	1077936128
	DD	1077936128
	DD	1077936128
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
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	1031603580
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	3185812323
	DD	1051372202
	DD	1051372202
	DD	1051372202
	DD	1051372202
	DD	1051372202
	DD	1051372202
	DD	1051372202
	DD	1051372202
	DD	1051372202
	DD	1051372202
	DD	1051372202
	DD	1051372202
	DD	1051372202
	DD	1051372202
	DD	1051372202
	DD	1051372202
	PUBLIC __svml_scbrt_ha_data_internal
__svml_scbrt_ha_data_internal	DD	3212578753
	DD	3212085645
	DD	3211621124
	DD	3211182772
	DD	3210768440
	DD	3210376206
	DD	3210004347
	DD	3209651317
	DD	3209315720
	DD	3208996296
	DD	3208691905
	DD	3208401508
	DD	3208124163
	DD	3207859009
	DD	3207605259
	DD	3207362194
	DD	3207129151
	DD	3206905525
	DD	3206690755
	DD	3206484326
	DD	3206285761
	DD	3206094618
	DD	3205910490
	DD	3205732998
	DD	3205561788
	DD	3205396533
	DD	3205236929
	DD	3205082689
	DD	3204933547
	DD	3204789256
	DD	3204649583
	DD	3204514308
	DD	1065396681
	DD	839340838
	DD	1065482291
	DD	867750258
	DD	1065566215
	DD	851786446
	DD	1065648532
	DD	853949398
	DD	1065729317
	DD	864938789
	DD	1065808640
	DD	864102364
	DD	1065886565
	DD	864209792
	DD	1065963152
	DD	865422805
	DD	1066038457
	DD	867593594
	DD	1066112533
	DD	854482593
	DD	1066185428
	DD	848298042
	DD	1066257188
	DD	860064854
	DD	1066327857
	DD	844792593
	DD	1066397474
	DD	870701309
	DD	1066466079
	DD	872023170
	DD	1066533708
	DD	860255342
	DD	1066600394
	DD	849966899
	DD	1066666169
	DD	863561479
	DD	1066731064
	DD	869115319
	DD	1066795108
	DD	871961375
	DD	1066858329
	DD	859537336
	DD	1066920751
	DD	871954398
	DD	1066982401
	DD	863817578
	DD	1067043301
	DD	861687921
	DD	1067103474
	DD	849594757
	DD	1067162941
	DD	816486846
	DD	1067221722
	DD	858183533
	DD	1067279837
	DD	864500406
	DD	1067337305
	DD	850523240
	DD	1067394143
	DD	808125243
	DD	1067450368
	DD	0
	DD	1067505996
	DD	861173761
	DD	1067588354
	DD	859000219
	DD	1067696217
	DD	823158129
	DD	1067801953
	DD	871826232
	DD	1067905666
	DD	871183196
	DD	1068007450
	DD	839030530
	DD	1068107390
	DD	867690638
	DD	1068205570
	DD	840440923
	DD	1068302063
	DD	868033274
	DD	1068396942
	DD	855856030
	DD	1068490271
	DD	865094453
	DD	1068582113
	DD	860418487
	DD	1068672525
	DD	866225006
	DD	1068761562
	DD	866458226
	DD	1068849275
	DD	865124659
	DD	1068935712
	DD	864837702
	DD	1069020919
	DD	811742505
	DD	1069104937
	DD	869432099
	DD	1069187809
	DD	864584201
	DD	1069269572
	DD	864183978
	DD	1069350263
	DD	844810573
	DD	1069429915
	DD	869245699
	DD	1069508563
	DD	859556409
	DD	1069586236
	DD	870675446
	DD	1069662966
	DD	814190139
	DD	1069738778
	DD	870686941
	DD	1069813702
	DD	861800510
	DD	1069887762
	DD	855649163
	DD	1069960982
	DD	869347119
	DD	1070033387
	DD	864252033
	DD	1070104998
	DD	867276215
	DD	1070175837
	DD	868189817
	DD	1070245925
	DD	849541095
	DD	1070349689
	DD	866633177
	DD	1070485588
	DD	843967686
	DD	1070618808
	DD	857522493
	DD	1070749478
	DD	862339487
	DD	1070877717
	DD	850054662
	DD	1071003634
	DD	864048556
	DD	1071127332
	DD	868027089
	DD	1071248907
	DD	848093931
	DD	1071368446
	DD	865355299
	DD	1071486034
	DD	848111485
	DD	1071601747
	DD	865557362
	DD	1071715659
	DD	870297525
	DD	1071827839
	DD	863416216
	DD	1071938350
	DD	869675693
	DD	1072047254
	DD	865888071
	DD	1072154608
	DD	825332584
	DD	1072260465
	DD	843309506
	DD	1072364876
	DD	870885636
	DD	1072467891
	DD	869119784
	DD	1072569555
	DD	865466648
	DD	1072669911
	DD	867459244
	DD	1072769001
	DD	861192764
	DD	1072866863
	DD	871247716
	DD	1072963536
	DD	864927982
	DD	1073059054
	DD	869195129
	DD	1073153452
	DD	864849564
	DD	1073246762
	DD	840005936
	DD	1073339014
	DD	852579258
	DD	1073430238
	DD	860852782
	DD	1073520462
	DD	869711141
	DD	1073609714
	DD	862506141
	DD	1073698019
	DD	837959274
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	3173551943
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	1031591658
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	3185806905
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
	DD	1051372203
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
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	8257536
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212836864
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
	DD	3212967936
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
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	256
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	85
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1
	DD	1365
	DD	1365
	DD	1365
	DD	1365
	DD	1365
	DD	1365
	DD	1365
	DD	1365
	DD	1365
	DD	1365
	DD	1365
	DD	1365
	DD	1365
	DD	1365
	DD	1365
	DD	1365
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
_2il0floatpacket_46	DD	0ffffffffH,000000000H,0ffffffffH,000000000H
_2il0floatpacket_47	DD	0007fffffH
_2il0floatpacket_48	DD	0007e0000H
_2il0floatpacket_49	DD	0bf800000H
_2il0floatpacket_50	DD	0bf820000H
_2il0floatpacket_51	DD	0bd288f47H
_2il0floatpacket_52	DD	03d7cd6eaH
_2il0floatpacket_53	DD	0bde38e39H
_2il0floatpacket_54	DD	03eaaaaabH
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
