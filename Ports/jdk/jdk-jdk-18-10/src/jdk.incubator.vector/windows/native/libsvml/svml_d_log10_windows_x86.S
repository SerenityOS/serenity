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
	PUBLIC __svml_log102_ha_ex

__svml_log102_ha_ex	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 264
        movaps    xmm1, xmm0
        movups    XMMWORD PTR [224+rsp], xmm15
        lea       r8, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [192+rsp], xmm8
        movaps    xmm8, xmm0
        movups    XMMWORD PTR [208+rsp], xmm7
        movaps    xmm7, xmm0
        mov       QWORD PTR [240+rsp], r13
        psrlq     xmm7, 20
        movups    xmm2, XMMWORD PTR [__svml_dlog10_ha_data_internal+9024]
        lea       r13, QWORD PTR [111+rsp]
        andps     xmm2, xmm0
        and       r13, -64
        orps      xmm2, XMMWORD PTR [__svml_dlog10_ha_data_internal+9088]
        cvtpd2ps  xmm4, xmm2
        cmpltpd   xmm1, XMMWORD PTR [__svml_dlog10_ha_data_internal+9152]
        cmpnlepd  xmm8, XMMWORD PTR [__svml_dlog10_ha_data_internal+9216]
        movlhps   xmm4, xmm4
        orps      xmm1, xmm8
        rcpps     xmm5, xmm4
        movmskpd  eax, xmm1
        movups    xmm15, XMMWORD PTR [__svml_dlog10_ha_data_internal+9344]
        movups    xmm4, XMMWORD PTR [_2il0floatpacket_28]
        cvtps2pd  xmm3, xmm5
        mulpd     xmm3, xmm15
        addpd     xmm3, xmm4
        subpd     xmm3, xmm4
        movups    xmm1, XMMWORD PTR [__svml_dlog10_ha_data_internal+9280]
        andps     xmm1, xmm2
        subpd     xmm2, xmm1
        mulpd     xmm1, xmm3
        mulpd     xmm2, xmm3
        subpd     xmm1, xmm15
        pshufd    xmm5, xmm7, 221
        psrlq     xmm3, 40
        cvtdq2pd  xmm4, xmm5
        addpd     xmm2, xmm1
        movd      edx, xmm3
        pshufd    xmm8, xmm3, 2
        movaps    xmm5, xmm2
        movups    xmm15, XMMWORD PTR [__svml_dlog10_ha_data_internal+9472]
        movd      ecx, xmm8
        movups    xmm7, XMMWORD PTR [__svml_dlog10_ha_data_internal+8576]
        mulpd     xmm15, xmm4
        mulpd     xmm5, xmm2
        mulpd     xmm4, XMMWORD PTR [__svml_dlog10_ha_data_internal+9536]
        mulpd     xmm7, xmm2
        movsxd    rdx, edx
        movsxd    rcx, ecx
        addpd     xmm7, XMMWORD PTR [__svml_dlog10_ha_data_internal+8640]
        movups    xmm1, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+r8+rdx]
        movups    xmm3, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+r8+rcx]
        movaps    xmm8, xmm1
        unpcklpd  xmm8, xmm3
        addpd     xmm8, xmm15
        movups    xmm15, XMMWORD PTR [__svml_dlog10_ha_data_internal+8320]
        mulpd     xmm15, xmm2
        unpckhpd  xmm1, xmm3
        movups    xmm3, XMMWORD PTR [__svml_dlog10_ha_data_internal+8448]
        addpd     xmm15, XMMWORD PTR [__svml_dlog10_ha_data_internal+8384]
        addpd     xmm1, xmm4
        mulpd     xmm3, xmm2
        mulpd     xmm15, xmm5
        addpd     xmm3, XMMWORD PTR [__svml_dlog10_ha_data_internal+8512]
        addpd     xmm3, xmm15
        mulpd     xmm5, xmm3
        movaps    xmm4, xmm2
        addpd     xmm4, xmm8
        addpd     xmm7, xmm5
        movaps    xmm15, xmm4
        subpd     xmm15, xmm8
        mulpd     xmm7, xmm2
        subpd     xmm2, xmm15
        addpd     xmm1, xmm2
        addpd     xmm7, xmm1
        mov       QWORD PTR [248+rsp], r13
        addpd     xmm4, xmm7
        test      eax, eax
        jne       _B1_3

_B1_2::

        movups    xmm7, XMMWORD PTR [208+rsp]
        movaps    xmm0, xmm4
        movups    xmm8, XMMWORD PTR [192+rsp]
        movups    xmm15, XMMWORD PTR [224+rsp]
        mov       r13, QWORD PTR [240+rsp]
        add       rsp, 264
        ret

_B1_3::

        movups    XMMWORD PTR [r13], xmm0
        movups    XMMWORD PTR [64+r13], xmm4

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
        cmp       ebx, 2
        jl        _B1_7

_B1_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm4, XMMWORD PTR [64+r13]
        jmp       _B1_2

_B1_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dlog10_ha_cout_rare_internal
        jmp       _B1_8
        ALIGN     16

_B1_11::

__svml_log102_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log102_ha_ex_B1_B3:
	DD	671233
	DD	2020414
	DD	882739
	DD	821287
	DD	981015
	DD	2162955

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_log102_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log102_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_log102_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_6
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_log102_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log101_ha_l9

__svml_log101_ha_l9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L14::

        sub       rsp, 232
        vmovapd   xmm1, xmm0
        vmovups   XMMWORD PTR [176+rsp], xmm14
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [208+rsp], xmm13
        vmovups   XMMWORD PTR [192+rsp], xmm10
        mov       QWORD PTR [168+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        vmovsd    xmm2, QWORD PTR [__svml_dlog10_ha_data_internal+9024]
        and       r13, -64
        vmovsd    xmm0, QWORD PTR [__svml_dlog10_ha_data_internal+9088]
        vandpd    xmm5, xmm1, xmm2
        vorpd     xmm0, xmm5, xmm0
        vcmpnlesd xmm5, xmm1, QWORD PTR [__svml_dlog10_ha_data_internal+9216]
        vcvtpd2ps xmm2, xmm0
        vmovlhps  xmm3, xmm2, xmm2
        vpsrlq    xmm2, xmm1, 20
        vrcpps    xmm4, xmm3
        vcvtps2pd xmm10, xmm4
        vcmpltsd  xmm4, xmm1, QWORD PTR [__svml_dlog10_ha_data_internal+9152]
        vmovsd    xmm3, QWORD PTR [__svml_dlog10_ha_data_internal+9344]
        vorpd     xmm5, xmm4, xmm5
        vmulsd    xmm13, xmm10, xmm3
        vmovmskpd ecx, xmm5
        vpshufd   xmm10, xmm2, 85
        vroundsd  xmm14, xmm13, xmm13, 0
        vfmsub213sd xmm0, xmm14, xmm3
        vpsrlq    xmm14, xmm14, 40
        vpshufd   xmm13, xmm10, 0
        vmovsd    xmm5, QWORD PTR [__svml_dlog10_ha_data_internal+9472]
        vmovd     eax, xmm14
        vmovsd    xmm10, QWORD PTR [__svml_dlog10_ha_data_internal+8320]
        vmovsd    xmm14, QWORD PTR [__svml_dlog10_ha_data_internal+8448]
        vcvtdq2pd xmm2, xmm13
        vfmadd213sd xmm10, xmm0, QWORD PTR [__svml_dlog10_ha_data_internal+8384]
        vfmadd213sd xmm14, xmm0, QWORD PTR [__svml_dlog10_ha_data_internal+8512]
        vmovapd   xmm13, xmm2
        vmovsd    xmm4, QWORD PTR [__svml_dlog10_ha_data_internal+8576]
        movsxd    rax, eax
        vfmadd213sd xmm4, xmm0, QWORD PTR [__svml_dlog10_ha_data_internal+8640]
        vmovddup  xmm3, QWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221880+rdx+rax]
        vfmadd213sd xmm13, xmm5, QWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+rdx+rax]
        vfmadd132sd xmm2, xmm3, QWORD PTR [__svml_dlog10_ha_data_internal+9536]
        vmulsd    xmm3, xmm0, xmm0
        vaddsd    xmm5, xmm13, xmm0
        vfmadd213sd xmm10, xmm3, xmm14
        vsubsd    xmm13, xmm5, xmm13
        vfmadd213sd xmm10, xmm3, xmm4
        vmulsd    xmm4, xmm10, xmm0
        vsubsd    xmm0, xmm0, xmm13
        mov       QWORD PTR [224+rsp], r13
        vaddsd    xmm2, xmm2, xmm0
        vaddsd    xmm10, xmm2, xmm4
        vaddsd    xmm0, xmm10, xmm5
        and       ecx, 1
        jne       _B2_3

_B2_2::

        vmovups   xmm10, XMMWORD PTR [192+rsp]
        vmovups   xmm13, XMMWORD PTR [208+rsp]
        vmovups   xmm14, XMMWORD PTR [176+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 232
        ret

_B2_3::

        vmovsd    QWORD PTR [r13], xmm1
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B2_6

_B2_4::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B2_2

_B2_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dlog10_ha_cout_rare_internal
        jmp       _B2_4
        ALIGN     16

_B2_7::

__svml_log101_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log101_ha_l9_B1_B6:
	DD	669953
	DD	1430585
	DD	829489
	DD	907304
	DD	780312
	DD	1900811

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_7
	DD	imagerel _unwind___svml_log101_ha_l9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log101_ha_e9

__svml_log101_ha_e9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L23::

        sub       rsp, 232
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [176+rsp], xmm13
        vmovapd   xmm13, xmm0
        vmovups   XMMWORD PTR [208+rsp], xmm11
        vmovups   XMMWORD PTR [192+rsp], xmm7
        mov       QWORD PTR [168+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        vmovsd    xmm1, QWORD PTR [__svml_dlog10_ha_data_internal+9024]
        and       r13, -64
        vmovsd    xmm5, QWORD PTR [__svml_dlog10_ha_data_internal+9088]
        vandpd    xmm11, xmm13, xmm1
        vorpd     xmm2, xmm11, xmm5
        vpsrlq    xmm5, xmm13, 20
        vcvtpd2ps xmm7, xmm2
        vcmpltsd  xmm3, xmm13, QWORD PTR [__svml_dlog10_ha_data_internal+9152]
        vmovlhps  xmm0, xmm7, xmm7
        vcmpnlesd xmm7, xmm13, QWORD PTR [__svml_dlog10_ha_data_internal+9216]
        vrcpps    xmm4, xmm0
        vcvtps2pd xmm1, xmm4
        vmovsd    xmm4, QWORD PTR [__svml_dlog10_ha_data_internal+9344]
        vorpd     xmm3, xmm3, xmm7
        vmulsd    xmm11, xmm1, xmm4
        vmovmskpd ecx, xmm3
        vpshufd   xmm1, xmm5, 85
        vroundsd  xmm0, xmm11, xmm11, 0
        vpshufd   xmm11, xmm1, 0
        vcvtdq2pd xmm5, xmm11
        vmovsd    xmm11, QWORD PTR [__svml_dlog10_ha_data_internal+9280]
        vandpd    xmm7, xmm2, xmm11
        vmulsd    xmm11, xmm7, xmm0
        vsubsd    xmm2, xmm2, xmm7
        vmulsd    xmm3, xmm2, xmm0
        vsubsd    xmm7, xmm11, xmm4
        vmulsd    xmm4, xmm5, QWORD PTR [__svml_dlog10_ha_data_internal+9472]
        vmulsd    xmm5, xmm5, QWORD PTR [__svml_dlog10_ha_data_internal+9536]
        vaddsd    xmm7, xmm7, xmm3
        vpsrlq    xmm0, xmm0, 40
        vmovd     eax, xmm0
        vmovsd    xmm2, QWORD PTR [__svml_dlog10_ha_data_internal+8320]
        vmovsd    xmm11, QWORD PTR [__svml_dlog10_ha_data_internal+8576]
        movsxd    rax, eax
        vmulsd    xmm1, xmm2, xmm7
        vmulsd    xmm11, xmm11, xmm7
        vaddsd    xmm0, xmm4, QWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+rdx+rax]
        vaddsd    xmm5, xmm5, QWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221880+rdx+rax]
        vaddsd    xmm1, xmm1, QWORD PTR [__svml_dlog10_ha_data_internal+8384]
        vmovsd    xmm4, QWORD PTR [__svml_dlog10_ha_data_internal+8448]
        vmulsd    xmm3, xmm4, xmm7
        vaddsd    xmm4, xmm11, QWORD PTR [__svml_dlog10_ha_data_internal+8640]
        vaddsd    xmm11, xmm0, xmm7
        vaddsd    xmm2, xmm3, QWORD PTR [__svml_dlog10_ha_data_internal+8512]
        vmulsd    xmm3, xmm7, xmm7
        vsubsd    xmm0, xmm11, xmm0
        vmulsd    xmm1, xmm1, xmm3
        vsubsd    xmm0, xmm7, xmm0
        vaddsd    xmm2, xmm1, xmm2
        vaddsd    xmm5, xmm5, xmm0
        vmulsd    xmm1, xmm2, xmm3
        mov       QWORD PTR [224+rsp], r13
        vaddsd    xmm2, xmm1, xmm4
        vmulsd    xmm3, xmm2, xmm7
        vaddsd    xmm7, xmm5, xmm3
        vaddsd    xmm0, xmm7, xmm11
        and       ecx, 1
        jne       _B3_3

_B3_2::

        vmovups   xmm7, XMMWORD PTR [192+rsp]
        vmovups   xmm11, XMMWORD PTR [208+rsp]
        vmovups   xmm13, XMMWORD PTR [176+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 232
        ret

_B3_3::

        vmovsd    QWORD PTR [r13], xmm13
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B3_6

_B3_4::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B3_2

_B3_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dlog10_ha_cout_rare_internal
        jmp       _B3_4
        ALIGN     16

_B3_7::

__svml_log101_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log101_ha_e9_B1_B6:
	DD	669953
	DD	1430585
	DD	817201
	DD	899112
	DD	776219
	DD	1900811

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_7
	DD	imagerel _unwind___svml_log101_ha_e9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log104_ha_e9

__svml_log104_ha_e9	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L32::

        sub       rsp, 552
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [464+rsp], ymm15
        vmovups   YMMWORD PTR [496+rsp], ymm13
        vmovups   YMMWORD PTR [432+rsp], ymm10
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [335+rsp]
        vmovapd   ymm13, ymm0
        and       r13, -64
        vandpd    ymm1, ymm13, YMMWORD PTR [__svml_dlog10_ha_data_internal+9024]
        vorpd     ymm4, ymm1, YMMWORD PTR [__svml_dlog10_ha_data_internal+9088]
        vcvtpd2ps xmm3, ymm4
        vrcpps    xmm5, xmm3
        vcvtps2pd ymm5, xmm5
        mov       QWORD PTR [536+rsp], r13
        vpsrlq    xmm10, xmm13, 20
        vextractf128 xmm2, ymm13, 1
        vpsrlq    xmm15, xmm2, 20
        vshufps   xmm3, xmm10, xmm15, 221
        vmovupd   ymm15, YMMWORD PTR [__svml_dlog10_ha_data_internal+9344]
        vcvtdq2pd ymm0, xmm3
        vcmplt_oqpd ymm10, ymm13, YMMWORD PTR [__svml_dlog10_ha_data_internal+9152]
        vcmpnle_uqpd ymm2, ymm13, YMMWORD PTR [__svml_dlog10_ha_data_internal+9216]
        vmulpd    ymm1, ymm5, ymm15
        vorpd     ymm2, ymm10, ymm2
        vandpd    ymm3, ymm4, YMMWORD PTR [__svml_dlog10_ha_data_internal+9280]
        vroundpd  ymm5, ymm1, 0
        vsubpd    ymm4, ymm4, ymm3
        vextractf128 xmm10, ymm2, 1
        vshufps   xmm1, xmm2, xmm10, 221
        vmulpd    ymm2, ymm5, ymm3
        vmulpd    ymm10, ymm5, ymm4
        vmovmskps edx, xmm1
        vsubpd    ymm15, ymm2, ymm15
        vaddpd    ymm10, ymm10, ymm15
        vpsrlq    xmm1, xmm5, 40
        vextractf128 xmm4, ymm5, 1
        vmovd     ecx, xmm1
        vpsrlq    xmm5, xmm4, 40
        vmovd     r9d, xmm5
        vpextrd   r8d, xmm1, 2
        vpextrd   r10d, xmm5, 2
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movsxd    r9, r9d
        movsxd    r10, r10d
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+rax+rcx]
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+rax+r8]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+rax+r9]
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+rax+r10]
        vunpcklpd xmm15, xmm3, xmm4
        vunpcklpd xmm5, xmm1, xmm2
        vunpckhpd xmm2, xmm1, xmm2
        vunpckhpd xmm3, xmm3, xmm4
        vinsertf128 ymm5, ymm15, xmm5, 1
        vmulpd    ymm15, ymm0, YMMWORD PTR [__svml_dlog10_ha_data_internal+9472]
        vmulpd    ymm0, ymm0, YMMWORD PTR [__svml_dlog10_ha_data_internal+9536]
        vaddpd    ymm1, ymm5, ymm15
        vmulpd    ymm5, ymm10, YMMWORD PTR [__svml_dlog10_ha_data_internal+8320]
        vinsertf128 ymm4, ymm3, xmm2, 1
        vmulpd    ymm3, ymm10, ymm10
        vaddpd    ymm2, ymm4, ymm0
        vaddpd    ymm0, ymm5, YMMWORD PTR [__svml_dlog10_ha_data_internal+8384]
        vmulpd    ymm5, ymm10, YMMWORD PTR [__svml_dlog10_ha_data_internal+8448]
        vmulpd    ymm4, ymm10, YMMWORD PTR [__svml_dlog10_ha_data_internal+8576]
        vmulpd    ymm0, ymm0, ymm3
        vaddpd    ymm15, ymm5, YMMWORD PTR [__svml_dlog10_ha_data_internal+8512]
        vaddpd    ymm5, ymm10, ymm1
        vaddpd    ymm4, ymm4, YMMWORD PTR [__svml_dlog10_ha_data_internal+8640]
        vaddpd    ymm0, ymm15, ymm0
        vsubpd    ymm15, ymm5, ymm1
        vmulpd    ymm1, ymm3, ymm0
        vaddpd    ymm3, ymm4, ymm1
        vmulpd    ymm0, ymm10, ymm3
        vsubpd    ymm10, ymm10, ymm15
        vaddpd    ymm2, ymm2, ymm10
        vaddpd    ymm15, ymm0, ymm2
        vaddpd    ymm0, ymm5, ymm15
        test      edx, edx
        jne       _B4_3

_B4_2::

        vmovups   ymm10, YMMWORD PTR [432+rsp]
        vmovups   ymm13, YMMWORD PTR [496+rsp]
        vmovups   ymm15, YMMWORD PTR [464+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B4_3::

        vmovupd   YMMWORD PTR [r13], ymm13
        vmovupd   YMMWORD PTR [64+r13], ymm0

_B4_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [224+rsp], ymm6
        vmovups   YMMWORD PTR [192+rsp], ymm7
        vmovups   YMMWORD PTR [160+rsp], ymm8
        vmovups   YMMWORD PTR [128+rsp], ymm9
        vmovups   YMMWORD PTR [96+rsp], ymm11
        vmovups   YMMWORD PTR [64+rsp], ymm12
        vmovups   YMMWORD PTR [32+rsp], ymm14
        mov       QWORD PTR [264+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [256+rsp], rsi
        mov       esi, edx

_B4_7::

        bt        esi, ebx
        jc        _B4_10

_B4_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B4_7

_B4_9::

        vmovups   ymm6, YMMWORD PTR [224+rsp]
        vmovups   ymm7, YMMWORD PTR [192+rsp]
        vmovups   ymm8, YMMWORD PTR [160+rsp]
        vmovups   ymm9, YMMWORD PTR [128+rsp]
        vmovups   ymm11, YMMWORD PTR [96+rsp]
        vmovups   ymm12, YMMWORD PTR [64+rsp]
        vmovups   ymm14, YMMWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [264+rsp]
        mov       rsi, QWORD PTR [256+rsp]
        jmp       _B4_2

_B4_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dlog10_ha_cout_rare_internal
        jmp       _B4_8
        ALIGN     16

_B4_11::

__svml_log104_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log104_ha_e9_B1_B3:
	DD	668929
	DD	4379701
	DD	1812525
	DD	2086948
	DD	1964059
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_log104_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log104_ha_e9_B6_B10:
	DD	1198625
	DD	2122826
	DD	2176064
	DD	190520
	DD	313394
	DD	440364
	DD	563238
	DD	690205
	DD	817172
	DD	944139
	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_log104_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_6
	DD	imagerel _B4_11
	DD	imagerel _unwind___svml_log104_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log108_ha_z0

__svml_log108_ha_z0	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L59::

        vmovaps   zmm5, zmm0
        vgetmantpd zmm4, zmm5, 8 {sae}
        vgetexppd zmm3, zmm5 {sae}
        vfpclasspd k2, zmm5, 94
        vrcp14pd  zmm22, zmm4
        vrndscalepd zmm23, zmm22, 88 {sae}
        vmovups   zmm25, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+256]
        vmovups   zmm24, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+320]
        vmovups   zmm2, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512]
        vmovups   zmm29, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+1024]
        vmovups   zmm27, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+384]
        vfmsub213pd zmm4, zmm23, zmm25 {rn-sae}
        vmovups   zmm1, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+448]
        vmovups   zmm28, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+512]
        vmovups   zmm22, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+576]
        vmovups   zmm31, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+128]
        vmovups   zmm30, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+1088]
        vfmadd231pd zmm1, zmm27, zmm4 {rn-sae}
        vmovups   zmm27, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+640]
        vfmadd231pd zmm22, zmm28, zmm4 {rn-sae}
        vmovups   zmm28, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+768]
        vmovups   zmm5, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+896]
        vcmppd    k1, zmm23, zmm24, 17 {sae}
        vmulpd    zmm0, zmm4, zmm4 {rn-sae}
        vpsrlq    zmm26, zmm23, 48
        vmovups   zmm23, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+832]
        vmulpd    zmm24, zmm0, zmm0 {rn-sae}
        vfmadd231pd zmm23, zmm28, zmm4 {rn-sae}
        vfmadd213pd zmm1, zmm0, zmm22 {rn-sae}
        vaddpd    zmm3{k1}, zmm3, zmm25 {rn-sae}
        vpermt2pd zmm2, zmm26, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+64]
        vpermt2pd zmm31, zmm26, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+192]
        vmovups   zmm25, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+704]
        vfmadd231pd zmm2, zmm29, zmm3 {rn-sae}
        vmovups   zmm26, ZMMWORD PTR [__svml_dlog10_ha_data_internal_avx512+960]
        vfmadd213pd zmm3, zmm30, zmm31 {rn-sae}
        vfmadd231pd zmm25, zmm27, zmm4 {rn-sae}
        vmovaps   zmm29, zmm2
        vfmadd231pd zmm29, zmm4, zmm26 {rn-sae}
        vfmadd213pd zmm25, zmm0, zmm23 {rn-sae}
        vsubpd    zmm0, zmm29, zmm2 {rn-sae}
        vfmadd213pd zmm1, zmm24, zmm25 {rn-sae}
        vxorpd    zmm0{k2}, zmm0, zmm0
        vfmadd213pd zmm1, zmm4, zmm5 {rn-sae}
        vfmsub231pd zmm0, zmm4, zmm26 {rn-sae}
        vaddpd    zmm2, zmm3, zmm0 {rn-sae}
        vfmadd213pd zmm1, zmm4, zmm2 {rn-sae}
        vaddpd    zmm0, zmm29, zmm1 {rn-sae}
        ret
        ALIGN     16

_B5_2::

__svml_log108_ha_z0 ENDP

_TEXT	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log101_ha_ex

__svml_log101_ha_ex	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L60::

        sub       rsp, 232
        movaps    xmm2, xmm0
        movups    XMMWORD PTR [192+rsp], xmm9
        movaps    xmm9, xmm2
        movups    XMMWORD PTR [176+rsp], xmm8
        lea       rdx, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [208+rsp], xmm7
        movaps    xmm7, xmm2
        mov       QWORD PTR [168+rsp], r13
        psrlq     xmm7, 20
        movsd     xmm5, QWORD PTR [__svml_dlog10_ha_data_internal+9024]
        lea       r13, QWORD PTR [95+rsp]
        movsd     xmm1, QWORD PTR [__svml_dlog10_ha_data_internal+9088]
        andps     xmm5, xmm2
        orps      xmm5, xmm1
        movaps    xmm1, xmm2
        cvtpd2ps  xmm3, xmm5
        cmpltsd   xmm1, QWORD PTR [__svml_dlog10_ha_data_internal+9152]
        cmpnlesd  xmm9, QWORD PTR [__svml_dlog10_ha_data_internal+9216]
        movlhps   xmm3, xmm3
        orps      xmm1, xmm9
        rcpps     xmm4, xmm3
        movmskpd  ecx, xmm1
        movsd     xmm3, QWORD PTR [__svml_dlog10_ha_data_internal+9344]
        movups    xmm0, XMMWORD PTR [_2il0floatpacket_28]
        cvtps2pd  xmm4, xmm4
        mulsd     xmm4, xmm3
        and       r13, -64
        addpd     xmm4, xmm0
        subpd     xmm4, xmm0
        movsd     xmm1, QWORD PTR [__svml_dlog10_ha_data_internal+9280]
        and       ecx, 1
        andps     xmm1, xmm5
        pshufd    xmm8, xmm7, 85
        subsd     xmm5, xmm1
        mulsd     xmm1, xmm4
        mulsd     xmm5, xmm4
        subsd     xmm1, xmm3
        pshufd    xmm0, xmm8, 0
        psrlq     xmm4, 40
        movsd     xmm8, QWORD PTR [__svml_dlog10_ha_data_internal+8320]
        addsd     xmm1, xmm5
        cvtdq2pd  xmm0, xmm0
        mulsd     xmm8, xmm1
        movd      eax, xmm4
        movsd     xmm9, QWORD PTR [__svml_dlog10_ha_data_internal+8448]
        movaps    xmm4, xmm1
        mulsd     xmm9, xmm1
        addsd     xmm8, QWORD PTR [__svml_dlog10_ha_data_internal+8384]
        mulsd     xmm4, xmm1
        addsd     xmm9, QWORD PTR [__svml_dlog10_ha_data_internal+8512]
        mulsd     xmm8, xmm4
        movaps    xmm3, xmm0
        addsd     xmm8, xmm9
        mulsd     xmm3, QWORD PTR [__svml_dlog10_ha_data_internal+9472]
        mulsd     xmm0, QWORD PTR [__svml_dlog10_ha_data_internal+9536]
        mulsd     xmm8, xmm4
        movsxd    rax, eax
        movsd     xmm5, QWORD PTR [__svml_dlog10_ha_data_internal+8576]
        mulsd     xmm5, xmm1
        addsd     xmm3, QWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+rdx+rax]
        addsd     xmm0, QWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221880+rdx+rax]
        addsd     xmm5, QWORD PTR [__svml_dlog10_ha_data_internal+8640]
        movaps    xmm7, xmm3
        addsd     xmm8, xmm5
        addsd     xmm7, xmm1
        mulsd     xmm8, xmm1
        movaps    xmm9, xmm7
        mov       QWORD PTR [224+rsp], r13
        subsd     xmm9, xmm3
        subsd     xmm1, xmm9
        addsd     xmm0, xmm1
        addsd     xmm0, xmm8
        addsd     xmm0, xmm7
        jne       _B6_3

_B6_2::

        movups    xmm7, XMMWORD PTR [208+rsp]
        movups    xmm8, XMMWORD PTR [176+rsp]
        movups    xmm9, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 232
        ret

_B6_3::

        movsd     QWORD PTR [r13], xmm2
        movsd     QWORD PTR [64+r13], xmm0
        jne       _B6_6

_B6_4::

        movsd     xmm0, QWORD PTR [64+r13]
        jmp       _B6_2

_B6_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dlog10_ha_cout_rare_internal
        jmp       _B6_4
        ALIGN     16

_B6_7::

__svml_log101_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log101_ha_ex_B1_B6:
	DD	671233
	DD	1430590
	DD	882739
	DD	755748
	DD	825367
	DD	1900811

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_log101_ha_ex_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log102_ha_e9

__svml_log102_ha_e9	PROC	EXPORT

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L69::

        sub       rsp, 264
        vmovapd   xmm3, xmm0
        vmovups   XMMWORD PTR [208+rsp], xmm15
        lea       r8, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [192+rsp], xmm11
        vpsrlq    xmm11, xmm3, 20
        vmovups   XMMWORD PTR [224+rsp], xmm9
        mov       QWORD PTR [240+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vandpd    xmm1, xmm3, XMMWORD PTR [__svml_dlog10_ha_data_internal+9024]
        and       r13, -64
        vorpd     xmm4, xmm1, XMMWORD PTR [__svml_dlog10_ha_data_internal+9088]
        vcvtpd2ps xmm2, xmm4
        vcmpnlepd xmm0, xmm3, XMMWORD PTR [__svml_dlog10_ha_data_internal+9216]
        vmovlhps  xmm5, xmm2, xmm2
        vrcpps    xmm15, xmm5
        vcmpltpd  xmm5, xmm3, XMMWORD PTR [__svml_dlog10_ha_data_internal+9152]
        vcvtps2pd xmm9, xmm15
        vmovupd   xmm2, XMMWORD PTR [__svml_dlog10_ha_data_internal+9344]
        vorpd     xmm5, xmm5, xmm0
        vmulpd    xmm1, xmm9, xmm2
        vmovmskpd edx, xmm5
        vandpd    xmm0, xmm4, XMMWORD PTR [__svml_dlog10_ha_data_internal+9280]
        vpshufd   xmm15, xmm11, 221
        vroundpd  xmm1, xmm1, 0
        vsubpd    xmm4, xmm4, xmm0
        vmulpd    xmm11, xmm1, xmm0
        vcvtdq2pd xmm9, xmm15
        vsubpd    xmm2, xmm11, xmm2
        vmulpd    xmm5, xmm1, xmm4
        vpsrlq    xmm1, xmm1, 40
        vmovd     eax, xmm1
        vaddpd    xmm2, xmm5, xmm2
        vpextrd   ecx, xmm1, 2
        vmulpd    xmm1, xmm9, XMMWORD PTR [__svml_dlog10_ha_data_internal+9472]
        vmulpd    xmm9, xmm9, XMMWORD PTR [__svml_dlog10_ha_data_internal+9536]
        vmulpd    xmm5, xmm2, XMMWORD PTR [__svml_dlog10_ha_data_internal+8448]
        movsxd    rax, eax
        movsxd    rcx, ecx
        mov       QWORD PTR [248+rsp], r13
        vmovupd   xmm0, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+r8+rax]
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+r8+rcx]
        vunpcklpd xmm15, xmm0, xmm4
        vunpckhpd xmm11, xmm0, xmm4
        vaddpd    xmm0, xmm15, xmm1
        vaddpd    xmm1, xmm11, xmm9
        vaddpd    xmm15, xmm5, XMMWORD PTR [__svml_dlog10_ha_data_internal+8512]
        vmulpd    xmm9, xmm2, XMMWORD PTR [__svml_dlog10_ha_data_internal+8320]
        vmulpd    xmm4, xmm2, xmm2
        vaddpd    xmm11, xmm9, XMMWORD PTR [__svml_dlog10_ha_data_internal+8384]
        vmulpd    xmm9, xmm2, XMMWORD PTR [__svml_dlog10_ha_data_internal+8576]
        vmulpd    xmm11, xmm11, xmm4
        vaddpd    xmm5, xmm9, XMMWORD PTR [__svml_dlog10_ha_data_internal+8640]
        vaddpd    xmm9, xmm2, xmm0
        vaddpd    xmm15, xmm15, xmm11
        vsubpd    xmm11, xmm9, xmm0
        vmulpd    xmm0, xmm4, xmm15
        vaddpd    xmm4, xmm5, xmm0
        vmulpd    xmm0, xmm2, xmm4
        vsubpd    xmm2, xmm2, xmm11
        vaddpd    xmm1, xmm1, xmm2
        vaddpd    xmm2, xmm0, xmm1
        vaddpd    xmm0, xmm9, xmm2
        test      edx, edx
        jne       _B7_3

_B7_2::

        vmovups   xmm9, XMMWORD PTR [224+rsp]
        vmovups   xmm11, XMMWORD PTR [192+rsp]
        vmovups   xmm15, XMMWORD PTR [208+rsp]
        mov       r13, QWORD PTR [240+rsp]
        add       rsp, 264
        ret

_B7_3::

        vmovupd   XMMWORD PTR [r13], xmm3
        vmovupd   XMMWORD PTR [64+r13], xmm0

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

        call      __svml_dlog10_ha_cout_rare_internal
        jmp       _B7_8
        ALIGN     16

_B7_11::

__svml_log102_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log102_ha_e9_B1_B3:
	DD	671233
	DD	2020414
	DD	956470
	DD	833576
	DD	915480
	DD	2162955

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_6
	DD	imagerel _unwind___svml_log102_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log102_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B7_1
	DD	imagerel _B7_6
	DD	imagerel _unwind___svml_log102_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_6
	DD	imagerel _B7_11
	DD	imagerel _unwind___svml_log102_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST7:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log102_ha_l9

__svml_log102_ha_l9	PROC	EXPORT

_B8_1::

        DB        243
        DB        15
        DB        30
        DB        250
L82::

        sub       rsp, 264
        vmovapd   xmm1, xmm0
        vmovups   XMMWORD PTR [192+rsp], xmm15
        lea       r8, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [208+rsp], xmm12
        vpsrlq    xmm12, xmm1, 20
        vmovups   XMMWORD PTR [224+rsp], xmm10
        mov       QWORD PTR [240+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vandpd    xmm2, xmm1, XMMWORD PTR [__svml_dlog10_ha_data_internal+9024]
        and       r13, -64
        vorpd     xmm0, xmm2, XMMWORD PTR [__svml_dlog10_ha_data_internal+9088]
        vcvtpd2ps xmm4, xmm0
        vcmpnlepd xmm5, xmm1, XMMWORD PTR [__svml_dlog10_ha_data_internal+9216]
        vmovlhps  xmm2, xmm4, xmm4
        vrcpps    xmm3, xmm2
        vcvtps2pd xmm10, xmm3
        vcmpltpd  xmm3, xmm1, XMMWORD PTR [__svml_dlog10_ha_data_internal+9152]
        vmovupd   xmm4, XMMWORD PTR [__svml_dlog10_ha_data_internal+9344]
        vorpd     xmm5, xmm3, xmm5
        vmulpd    xmm15, xmm10, xmm4
        vmovmskpd edx, xmm5
        vpshufd   xmm2, xmm12, 221
        vcvtdq2pd xmm2, xmm2
        vroundpd  xmm15, xmm15, 0
        vfmsub213pd xmm0, xmm15, xmm4
        vpsrlq    xmm4, xmm15, 40
        vmovupd   xmm15, XMMWORD PTR [__svml_dlog10_ha_data_internal+8448]
        vmovd     eax, xmm4
        vfmadd213pd xmm15, xmm0, XMMWORD PTR [__svml_dlog10_ha_data_internal+8512]
        vpextrd   ecx, xmm4, 2
        movsxd    rax, eax
        movsxd    rcx, ecx
        mov       QWORD PTR [248+rsp], r13
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+r8+rax]
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+r8+rcx]
        vunpcklpd xmm12, xmm3, xmm5
        vunpckhpd xmm10, xmm3, xmm5
        vmulpd    xmm5, xmm0, xmm0
        vfmadd231pd xmm12, xmm2, XMMWORD PTR [__svml_dlog10_ha_data_internal+9472]
        vfmadd132pd xmm2, xmm10, XMMWORD PTR [__svml_dlog10_ha_data_internal+9536]
        vmovupd   xmm10, XMMWORD PTR [__svml_dlog10_ha_data_internal+8320]
        vmovupd   xmm3, XMMWORD PTR [__svml_dlog10_ha_data_internal+8576]
        vfmadd213pd xmm10, xmm0, XMMWORD PTR [__svml_dlog10_ha_data_internal+8384]
        vfmadd213pd xmm3, xmm0, XMMWORD PTR [__svml_dlog10_ha_data_internal+8640]
        vaddpd    xmm4, xmm0, xmm12
        vfmadd213pd xmm10, xmm5, xmm15
        vsubpd    xmm12, xmm4, xmm12
        vfmadd213pd xmm10, xmm5, xmm3
        vmulpd    xmm3, xmm0, xmm10
        vsubpd    xmm0, xmm0, xmm12
        vaddpd    xmm0, xmm2, xmm0
        vaddpd    xmm2, xmm3, xmm0
        vaddpd    xmm0, xmm4, xmm2
        test      edx, edx
        jne       _B8_3

_B8_2::

        vmovups   xmm10, XMMWORD PTR [224+rsp]
        vmovups   xmm12, XMMWORD PTR [208+rsp]
        vmovups   xmm15, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [240+rsp]
        add       rsp, 264
        ret

_B8_3::

        vmovupd   XMMWORD PTR [r13], xmm1
        vmovupd   XMMWORD PTR [64+r13], xmm0

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
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B8_2

_B8_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dlog10_ha_cout_rare_internal
        jmp       _B8_8
        ALIGN     16

_B8_11::

__svml_log102_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log102_ha_l9_B1_B3:
	DD	671233
	DD	2020414
	DD	960566
	DD	903208
	DD	849944
	DD	2162955

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_1
	DD	imagerel _B8_6
	DD	imagerel _unwind___svml_log102_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log102_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B8_1
	DD	imagerel _B8_6
	DD	imagerel _unwind___svml_log102_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_6
	DD	imagerel _B8_11
	DD	imagerel _unwind___svml_log102_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST8:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_log104_ha_l9

__svml_log104_ha_l9	PROC	EXPORT

_B9_1::

        DB        243
        DB        15
        DB        30
        DB        250
L95::

        sub       rsp, 552
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [432+rsp], ymm14
        vmovups   YMMWORD PTR [464+rsp], ymm13
        vmovups   YMMWORD PTR [496+rsp], ymm12
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [335+rsp]
        vmovdqa   ymm2, ymm0
        and       r13, -64
        vandpd    ymm12, ymm2, YMMWORD PTR [__svml_dlog10_ha_data_internal+9024]
        vorpd     ymm0, ymm12, YMMWORD PTR [__svml_dlog10_ha_data_internal+9088]
        vcvtpd2ps xmm1, ymm0
        vpsrlq    ymm4, ymm2, 20
        vmovupd   ymm14, YMMWORD PTR [__svml_dlog10_ha_data_internal+9344]
        vrcpps    xmm5, xmm1
        vcmpnle_uqpd ymm13, ymm2, YMMWORD PTR [__svml_dlog10_ha_data_internal+9216]
        vcvtps2pd ymm1, xmm5
        vmulpd    ymm12, ymm1, ymm14
        vroundpd  ymm12, ymm12, 0
        vfmsub213pd ymm0, ymm12, ymm14
        mov       QWORD PTR [536+rsp], r13
        vextracti128 xmm3, ymm4, 1
        vshufps   xmm5, xmm4, xmm3, 221
        vcmplt_oqpd ymm3, ymm2, YMMWORD PTR [__svml_dlog10_ha_data_internal+9152]
        vcvtdq2pd ymm1, xmm5
        vorpd     ymm4, ymm3, ymm13
        vpsrlq    ymm13, ymm12, 40
        vmovmskpd edx, ymm4
        test      edx, edx
        vextracti128 xmm3, ymm13, 1
        vmovd     ecx, xmm13
        vmovd     r9d, xmm3
        vpextrd   r8d, xmm13, 2
        vpextrd   r10d, xmm3, 2
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movsxd    r9, r9d
        movsxd    r10, r10d
        vmovupd   xmm5, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+rax+rcx]
        vmovupd   xmm4, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+rax+r8]
        vmovupd   xmm12, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+rax+r9]
        vmovupd   xmm14, XMMWORD PTR [imagerel(__svml_dlog10_ha_data_internal)-4221888+rax+r10]
        vunpcklpd xmm13, xmm5, xmm4
        vunpckhpd xmm4, xmm5, xmm4
        vunpcklpd xmm3, xmm12, xmm14
        vunpckhpd xmm14, xmm12, xmm14
        vmovupd   ymm5, YMMWORD PTR [__svml_dlog10_ha_data_internal+8576]
        vfmadd213pd ymm5, ymm0, YMMWORD PTR [__svml_dlog10_ha_data_internal+8640]
        vinsertf128 ymm3, ymm13, xmm3, 1
        vinsertf128 ymm12, ymm4, xmm14, 1
        vmovupd   ymm13, YMMWORD PTR [__svml_dlog10_ha_data_internal+8320]
        vmovupd   ymm14, YMMWORD PTR [__svml_dlog10_ha_data_internal+8448]
        vmulpd    ymm4, ymm0, ymm0
        vfmadd231pd ymm3, ymm1, YMMWORD PTR [__svml_dlog10_ha_data_internal+9472]
        vfmadd132pd ymm1, ymm12, YMMWORD PTR [__svml_dlog10_ha_data_internal+9536]
        vfmadd213pd ymm13, ymm0, YMMWORD PTR [__svml_dlog10_ha_data_internal+8384]
        vfmadd213pd ymm14, ymm0, YMMWORD PTR [__svml_dlog10_ha_data_internal+8512]
        vaddpd    ymm12, ymm0, ymm3
        vfmadd213pd ymm13, ymm4, ymm14
        vsubpd    ymm3, ymm12, ymm3
        vfmadd213pd ymm13, ymm4, ymm5
        vmulpd    ymm4, ymm0, ymm13
        vsubpd    ymm0, ymm0, ymm3
        vaddpd    ymm0, ymm1, ymm0
        vaddpd    ymm1, ymm4, ymm0
        vaddpd    ymm0, ymm12, ymm1
        jne       _B9_3

_B9_2::

        vmovups   ymm12, YMMWORD PTR [496+rsp]
        vmovups   ymm13, YMMWORD PTR [464+rsp]
        vmovups   ymm14, YMMWORD PTR [432+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B9_3::

        vmovupd   YMMWORD PTR [r13], ymm2
        vmovupd   YMMWORD PTR [64+r13], ymm0

_B9_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [224+rsp], ymm6
        vmovups   YMMWORD PTR [192+rsp], ymm7
        vmovups   YMMWORD PTR [160+rsp], ymm8
        vmovups   YMMWORD PTR [128+rsp], ymm9
        vmovups   YMMWORD PTR [96+rsp], ymm10
        vmovups   YMMWORD PTR [64+rsp], ymm11
        vmovups   YMMWORD PTR [32+rsp], ymm15
        mov       QWORD PTR [264+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [256+rsp], rsi
        mov       esi, edx

_B9_7::

        bt        esi, ebx
        jc        _B9_10

_B9_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B9_7

_B9_9::

        vmovups   ymm6, YMMWORD PTR [224+rsp]
        vmovups   ymm7, YMMWORD PTR [192+rsp]
        vmovups   ymm8, YMMWORD PTR [160+rsp]
        vmovups   ymm9, YMMWORD PTR [128+rsp]
        vmovups   ymm10, YMMWORD PTR [96+rsp]
        vmovups   ymm11, YMMWORD PTR [64+rsp]
        vmovups   ymm15, YMMWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [264+rsp]
        mov       rsi, QWORD PTR [256+rsp]
        jmp       _B9_2

_B9_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dlog10_ha_cout_rare_internal
        jmp       _B9_8
        ALIGN     16

_B9_11::

__svml_log104_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log104_ha_l9_B1_B3:
	DD	668929
	DD	4379701
	DD	2082861
	DD	1955876
	DD	1828891
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_1
	DD	imagerel _B9_6
	DD	imagerel _unwind___svml_log104_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_log104_ha_l9_B6_B10:
	DD	1198625
	DD	2122826
	DD	2176064
	DD	194616
	DD	309298
	DD	436268
	DD	563238
	DD	690205
	DD	817172
	DD	944139
	DD	imagerel _B9_1
	DD	imagerel _B9_6
	DD	imagerel _unwind___svml_log104_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_6
	DD	imagerel _B9_11
	DD	imagerel _unwind___svml_log104_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST9:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_dlog10_ha_cout_rare_internal

__svml_dlog10_ha_cout_rare_internal	PROC

_B10_1::

        DB        243
        DB        15
        DB        30
        DB        250
L122::

        sub       rsp, 56
        mov       r9, rcx
        xor       eax, eax
        movzx     ecx, WORD PTR [6+r9]
        mov       r8d, ecx
        and       r8d, 32752
        cmp       r8d, 32752
        je        _B10_12

_B10_2::

        movsd     xmm2, QWORD PTR [r9]
        xor       r8d, r8d
        movsd     QWORD PTR [48+rsp], xmm2
        test      ecx, 32752
        jne       _B10_4

_B10_3::

        movsd     xmm0, QWORD PTR [_vmldLgHATab+1600]
        mov       r8d, -60
        mulsd     xmm2, xmm0
        movsd     QWORD PTR [48+rsp], xmm2

_B10_4::

        movsd     xmm0, QWORD PTR [_vmldLgHATab+1608]
        comisd    xmm2, xmm0
        jbe       _B10_8

_B10_5::

        movaps    xmm3, xmm2
        subsd     xmm3, QWORD PTR [_2il0floatpacket_101]
        movsd     QWORD PTR [40+rsp], xmm3
        and       BYTE PTR [47+rsp], 127
        movsd     xmm0, QWORD PTR [40+rsp]
        comisd    xmm0, QWORD PTR [_vmldLgHATab+1592]
        jbe       _B10_7

_B10_6::

        movsd     QWORD PTR [40+rsp], xmm2
        pxor      xmm0, xmm0
        movzx     r9d, WORD PTR [46+rsp]
        and       r9d, -32753
        add       r9d, 16368
        mov       WORD PTR [46+rsp], r9w
        movsd     xmm4, QWORD PTR [40+rsp]
        movzx     ecx, WORD PTR [54+rsp]
        movaps    xmm2, xmm4
        and       ecx, 32752
        movaps    xmm3, xmm4
        shr       ecx, 4
        addsd     xmm2, QWORD PTR [_vmldLgHATab+1576]
        addsd     xmm3, QWORD PTR [_vmldLgHATab+1584]
        movsd     QWORD PTR [32+rsp], xmm2
        lea       r8d, DWORD PTR [-1023+r8+rcx]
        cvtsi2sd  xmm0, r8d
        mov       r10d, DWORD PTR [32+rsp]
        lea       rcx, QWORD PTR [__ImageBase]
        movsd     QWORD PTR [32+rsp], xmm3
        and       r10d, 127
        movsd     xmm3, QWORD PTR [32+rsp]
        mov       r9, rcx
        movsd     xmm1, QWORD PTR [_vmldLgHATab+1560]
        mov       r8, rcx
        movsd     xmm5, QWORD PTR [_vmldLgHATab+1568]
        movsd     xmm2, QWORD PTR [_vmldLgHATab+1688]
        lea       r11d, DWORD PTR [r10+r10*2]
        mulsd     xmm1, xmm0
        subsd     xmm3, QWORD PTR [_vmldLgHATab+1584]
        mulsd     xmm0, xmm5
        subsd     xmm4, xmm3
        addsd     xmm1, QWORD PTR [imagerel(_vmldLgHATab)+8+r8+r11*8]
        addsd     xmm0, QWORD PTR [imagerel(_vmldLgHATab)+16+r9+r11*8]
        movsd     xmm5, QWORD PTR [imagerel(_vmldLgHATab)+rcx+r11*8]
        mulsd     xmm3, xmm5
        mulsd     xmm5, xmm4
        subsd     xmm3, QWORD PTR [_vmldLgHATab+1624]
        movaps    xmm4, xmm3
        addsd     xmm1, xmm3
        addsd     xmm4, xmm5
        mulsd     xmm2, xmm4
        addsd     xmm2, QWORD PTR [_vmldLgHATab+1680]
        mulsd     xmm2, xmm4
        addsd     xmm2, QWORD PTR [_vmldLgHATab+1672]
        mulsd     xmm2, xmm4
        addsd     xmm2, QWORD PTR [_vmldLgHATab+1664]
        mulsd     xmm2, xmm4
        addsd     xmm2, QWORD PTR [_vmldLgHATab+1656]
        mulsd     xmm2, xmm4
        addsd     xmm2, QWORD PTR [_vmldLgHATab+1648]
        mulsd     xmm2, xmm4
        addsd     xmm2, QWORD PTR [_vmldLgHATab+1640]
        mulsd     xmm2, xmm4
        movaps    xmm4, xmm5
        addsd     xmm2, QWORD PTR [_vmldLgHATab+1632]
        mulsd     xmm4, xmm2
        mulsd     xmm3, xmm2
        addsd     xmm0, xmm4
        addsd     xmm5, xmm0
        addsd     xmm5, xmm3
        addsd     xmm1, xmm5
        movsd     QWORD PTR [rdx], xmm1
        add       rsp, 56
        ret

_B10_7::

        movsd     xmm0, QWORD PTR [_vmldLgHATab+1624]
        mulsd     xmm3, xmm0
        movsd     xmm1, QWORD PTR [_vmldLgHATab+1688]
        mulsd     xmm1, xmm3
        addsd     xmm1, QWORD PTR [_vmldLgHATab+1680]
        mulsd     xmm1, xmm3
        addsd     xmm1, QWORD PTR [_vmldLgHATab+1672]
        mulsd     xmm1, xmm3
        addsd     xmm1, QWORD PTR [_vmldLgHATab+1664]
        mulsd     xmm1, xmm3
        addsd     xmm1, QWORD PTR [_vmldLgHATab+1656]
        mulsd     xmm1, xmm3
        addsd     xmm1, QWORD PTR [_vmldLgHATab+1648]
        mulsd     xmm1, xmm3
        addsd     xmm1, QWORD PTR [_vmldLgHATab+1640]
        mulsd     xmm1, xmm3
        addsd     xmm1, QWORD PTR [_vmldLgHATab+1632]
        mulsd     xmm1, xmm3
        addsd     xmm3, xmm1
        movsd     QWORD PTR [rdx], xmm3
        add       rsp, 56
        ret

_B10_8::

        ucomisd   xmm2, xmm0
        jp        _B10_9
        je        _B10_11

_B10_9::

        divsd     xmm0, xmm0
        movsd     QWORD PTR [rdx], xmm0
        mov       eax, 1

_B10_10::

        add       rsp, 56
        ret

_B10_11::

        movsd     xmm1, QWORD PTR [_vmldLgHATab+1616]
        mov       eax, 2
        xorps     xmm1, XMMWORD PTR [_2il0floatpacket_102]
        divsd     xmm1, xmm0
        movsd     QWORD PTR [rdx], xmm1
        add       rsp, 56
        ret

_B10_12::

        mov       cl, BYTE PTR [7+r9]
        and       cl, -128
        cmp       cl, -128
        je        _B10_14

_B10_13::

        movsd     xmm0, QWORD PTR [r9]
        mulsd     xmm0, xmm0
        movsd     QWORD PTR [rdx], xmm0
        add       rsp, 56
        ret

_B10_14::

        test      DWORD PTR [4+r9], 1048575
        jne       _B10_13

_B10_15::

        cmp       DWORD PTR [r9], 0
        jne       _B10_13

_B10_16::

        movsd     xmm0, QWORD PTR [_vmldLgHATab+1608]
        mov       eax, 1
        divsd     xmm0, xmm0
        movsd     QWORD PTR [rdx], xmm0
        add       rsp, 56
        ret
        ALIGN     16

_B10_17::

__svml_dlog10_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_dlog10_ha_cout_rare_internal_B1_B16:
	DD	67585
	DD	25096

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B10_1
	DD	imagerel _B10_17
	DD	imagerel _unwind___svml_dlog10_ha_cout_rare_internal_B1_B16

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_dlog10_ha_data_internal_avx512
__svml_dlog10_ha_data_internal_avx512	DD	0
	DD	0
	DD	721420288
	DD	3214603769
	DD	3590979584
	DD	3215601833
	DD	1438908416
	DD	3216186160
	DD	948305920
	DD	3216559896
	DD	2869821440
	DD	3216915393
	DD	516521984
	DD	3217142759
	DD	2145648640
	DD	3217304702
	DD	733741056
	DD	1069546492
	DD	3513843712
	DD	1069249052
	DD	3459645440
	DD	1068963280
	DD	1085800448
	DD	1068688295
	DD	3613786112
	DD	1068347678
	DD	1803419648
	DD	1067836310
	DD	3436707840
	DD	1067234191
	DD	930611200
	DD	1066155272
	DD	0
	DD	0
	DD	3686878645
	DD	3175924212
	DD	2120733314
	DD	1025513021
	DD	1829252717
	DD	3176941056
	DD	3391913567
	DD	3176268582
	DD	1445614576
	DD	3174694461
	DD	577138096
	DD	1029194402
	DD	717064894
	DD	1025503698
	DD	1625232067
	DD	1029570781
	DD	2107125367
	DD	1029044389
	DD	1740576090
	DD	1029619435
	DD	4015256301
	DD	3177184346
	DD	1535607671
	DD	1029013126
	DD	2000266743
	DD	1028805283
	DD	761604295
	DD	1028127597
	DD	803304759
	DD	1025708071
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
	DD	1072168960
	DD	0
	DD	1072168960
	DD	0
	DD	1072168960
	DD	0
	DD	1072168960
	DD	0
	DD	1072168960
	DD	0
	DD	1072168960
	DD	0
	DD	1072168960
	DD	0
	DD	1072168960
	DD	675808112
	DD	1068024536
	DD	675808112
	DD	1068024536
	DD	675808112
	DD	1068024536
	DD	675808112
	DD	1068024536
	DD	675808112
	DD	1068024536
	DD	675808112
	DD	1068024536
	DD	675808112
	DD	1068024536
	DD	675808112
	DD	1068024536
	DD	2516752404
	DD	3215710221
	DD	2516752404
	DD	3215710221
	DD	2516752404
	DD	3215710221
	DD	2516752404
	DD	3215710221
	DD	2516752404
	DD	3215710221
	DD	2516752404
	DD	3215710221
	DD	2516752404
	DD	3215710221
	DD	2516752404
	DD	3215710221
	DD	4085995682
	DD	1068483574
	DD	4085995682
	DD	1068483574
	DD	4085995682
	DD	1068483574
	DD	4085995682
	DD	1068483574
	DD	4085995682
	DD	1068483574
	DD	4085995682
	DD	1068483574
	DD	4085995682
	DD	1068483574
	DD	4085995682
	DD	1068483574
	DD	879025280
	DD	3216148390
	DD	879025280
	DD	3216148390
	DD	879025280
	DD	3216148390
	DD	879025280
	DD	3216148390
	DD	879025280
	DD	3216148390
	DD	879025280
	DD	3216148390
	DD	879025280
	DD	3216148390
	DD	879025280
	DD	3216148390
	DD	2004821977
	DD	1068907618
	DD	2004821977
	DD	1068907618
	DD	2004821977
	DD	1068907618
	DD	2004821977
	DD	1068907618
	DD	2004821977
	DD	1068907618
	DD	2004821977
	DD	1068907618
	DD	2004821977
	DD	1068907618
	DD	2004821977
	DD	1068907618
	DD	356255395
	DD	3216755579
	DD	356255395
	DD	3216755579
	DD	356255395
	DD	3216755579
	DD	356255395
	DD	3216755579
	DD	356255395
	DD	3216755579
	DD	356255395
	DD	3216755579
	DD	356255395
	DD	3216755579
	DD	356255395
	DD	3216755579
	DD	1668235916
	DD	1069713319
	DD	1668235916
	DD	1069713319
	DD	1668235916
	DD	1069713319
	DD	1668235916
	DD	1069713319
	DD	1668235916
	DD	1069713319
	DD	1668235916
	DD	1069713319
	DD	1668235916
	DD	1069713319
	DD	1668235916
	DD	1069713319
	DD	354870491
	DD	3217804155
	DD	354870491
	DD	3217804155
	DD	354870491
	DD	3217804155
	DD	354870491
	DD	3217804155
	DD	354870491
	DD	3217804155
	DD	354870491
	DD	3217804155
	DD	354870491
	DD	3217804155
	DD	354870491
	DD	3217804155
	DD	2867927077
	DD	1013556733
	DD	2867927077
	DD	1013556733
	DD	2867927077
	DD	1013556733
	DD	2867927077
	DD	1013556733
	DD	2867927077
	DD	1013556733
	DD	2867927077
	DD	1013556733
	DD	2867927077
	DD	1013556733
	DD	2867927077
	DD	1013556733
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	3296479949
	DD	1031700412
	DD	3296479949
	DD	1031700412
	DD	3296479949
	DD	1031700412
	DD	3296479949
	DD	1031700412
	DD	3296479949
	DD	1031700412
	DD	3296479949
	DD	1031700412
	DD	3296479949
	DD	1031700412
	DD	3296479949
	DD	1031700412
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
	PUBLIC __svml_dlog10_ha_data_internal
__svml_dlog10_ha_data_internal	DD	1190572160
	DD	3228777073
	DD	3860447744
	DD	3189665916
	DD	1198987632
	DD	3228777077
	DD	3228397393
	DD	3189668294
	DD	1168821104
	DD	3228777081
	DD	637194908
	DD	3189668066
	DD	1100245360
	DD	3228777085
	DD	133785915
	DD	3189667542
	DD	993432048
	DD	3228777089
	DD	3017828443
	DD	3189667588
	DD	848551680
	DD	3228777093
	DD	560605178
	DD	3189668289
	DD	665773664
	DD	3228777097
	DD	3849538177
	DD	3189667548
	DD	445266208
	DD	3228777101
	DD	3804908986
	DD	3189667886
	DD	187196464
	DD	3228777105
	DD	3516966431
	DD	3189667858
	DD	4186697744
	DD	3228777108
	DD	3052190817
	DD	3189667809
	DD	3854000384
	DD	3228777112
	DD	1554781574
	DD	3189667427
	DD	3484235504
	DD	3228777116
	DD	4153839410
	DD	3189667384
	DD	3077565856
	DD	3228777120
	DD	4000768869
	DD	3189667904
	DD	2634153136
	DD	3228777124
	DD	4244738174
	DD	3189668339
	DD	2154157984
	DD	3228777128
	DD	3014247306
	DD	3189667771
	DD	1637739952
	DD	3228777132
	DD	1906045981
	DD	3189667650
	DD	1085057568
	DD	3228777136
	DD	3987700171
	DD	3189668284
	DD	496268368
	DD	3228777140
	DD	1674280096
	DD	3189667373
	DD	4166496080
	DD	3228777143
	DD	2078815235
	DD	3189667744
	DD	3505961600
	DD	3228777147
	DD	4286802640
	DD	3189667683
	DD	2809786672
	DD	3228777151
	DD	1913564473
	DD	3189667639
	DD	2078124752
	DD	3228777155
	DD	2693682663
	DD	3189667702
	DD	1311128304
	DD	3228777159
	DD	1012063542
	DD	3189668158
	DD	508948832
	DD	3228777163
	DD	2489929809
	DD	3189667978
	DD	3966704144
	DD	3228777166
	DD	2165614611
	DD	3189667408
	DD	3094609184
	DD	3228777170
	DD	2488783422
	DD	3189667472
	DD	2187779856
	DD	3228777174
	DD	2723126436
	DD	3189667428
	DD	1246363792
	DD	3228777178
	DD	3017034520
	DD	3189668359
	DD	270507744
	DD	3228777182
	DD	1972662349
	DD	3189667544
	DD	3555324736
	DD	3228777185
	DD	898998658
	DD	3189668182
	DD	2511025072
	DD	3228777189
	DD	2160314027
	DD	3189667608
	DD	1432719952
	DD	3228777193
	DD	1911887828
	DD	3189668033
	DD	320552432
	DD	3228777197
	DD	3280422502
	DD	3189667818
	DD	3469631920
	DD	3228777200
	DD	3027884278
	DD	3189668109
	DD	2290165072
	DD	3228777204
	DD	3517602960
	DD	3189668197
	DD	1077259536
	DD	3228777208
	DD	3013130823
	DD	3189668046
	DD	4126022080
	DD	3228777211
	DD	3235689898
	DD	3189667740
	DD	2846656704
	DD	3228777215
	DD	87643117
	DD	3189667953
	DD	1534268448
	DD	3228777219
	DD	754951962
	DD	3189668360
	DD	188994208
	DD	3228777223
	DD	3023581101
	DD	3189668100
	DD	3105937296
	DD	3228777226
	DD	1490072819
	DD	3189668276
	DD	1695298320
	DD	3228777230
	DD	3738440902
	DD	3189668304
	DD	252178944
	DD	3228777234
	DD	897194307
	DD	3189667388
	DD	3071679952
	DD	3228777237
	DD	195484330
	DD	3189668024
	DD	1563999488
	DD	3228777241
	DD	3795554301
	DD	3189668248
	DD	24236736
	DD	3228777245
	DD	2858806924
	DD	3189668212
	DD	2747490080
	DD	3228777248
	DD	4259795627
	DD	3189667532
	DD	1143955184
	DD	3228777252
	DD	4279885499
	DD	3189667764
	DD	3803696144
	DD	3228777255
	DD	3997664578
	DD	3189667698
	DD	2136907056
	DD	3228777259
	DD	2176158532
	DD	3189667869
	DD	438683136
	DD	3228777263
	DD	227964261
	DD	3189667893
	DD	3004118816
	DD	3228777266
	DD	2296676690
	DD	3189667896
	DD	1243405872
	DD	3228777270
	DD	2079834385
	DD	3189667894
	DD	3746604496
	DD	3228777273
	DD	3850151037
	DD	3189668185
	DD	1923904960
	DD	3228777277
	DD	574763554
	DD	3189667699
	DD	70398640
	DD	3228777281
	DD	3827982506
	DD	3189667452
	DD	2481176176
	DD	3228777284
	DD	1146808857
	DD	3189667869
	DD	566425600
	DD	3228777288
	DD	4238446104
	DD	3189668130
	DD	2916203392
	DD	3228777291
	DD	3553887815
	DD	3189667582
	DD	940696080
	DD	3228777295
	DD	1221192380
	DD	3189668153
	DD	3229958720
	DD	3228777298
	DD	2271570828
	DD	3189667556
	DD	1194176400
	DD	3228777302
	DD	3002536483
	DD	3189667850
	DD	3423402736
	DD	3228777305
	DD	2434838684
	DD	3189667608
	DD	1327821424
	DD	3228777309
	DD	2687777298
	DD	3189667396
	DD	3497484640
	DD	3228777312
	DD	2749657917
	DD	3189668082
	DD	1342574720
	DD	3228777316
	DD	3002546917
	DD	3189668115
	DD	3453142464
	DD	3228777319
	DD	3498283957
	DD	3189667970
	DD	1239368816
	DD	3228777323
	DD	455121273
	DD	3189667422
	DD	3291303200
	DD	3228777326
	DD	2621841620
	DD	3189667932
	DD	3012883008
	DD	3228777333
	DD	2362501462
	DD	3189667975
	DD	2618787376
	DD	3228777340
	DD	1091754251
	DD	3189667994
	DD	2109911280
	DD	3228777347
	DD	193599334
	DD	3189668101
	DD	1487139360
	DD	3228777354
	DD	3757852586
	DD	3189667728
	DD	751346048
	DD	3228777361
	DD	1167306858
	DD	3189667607
	DD	4198363056
	DD	3228777367
	DD	3727503052
	DD	3189667447
	DD	3239110320
	DD	3228777374
	DD	4101491224
	DD	3189667535
	DD	2169399936
	DD	3228777381
	DD	1682828411
	DD	3189668088
	DD	990067152
	DD	3228777388
	DD	325154957
	DD	3189667406
	DD	3996905008
	DD	3228777394
	DD	333920478
	DD	3189668041
	DD	2600795440
	DD	3228777401
	DD	1868291498
	DD	3189667430
	DD	1097513040
	DD	3228777408
	DD	2092796598
	DD	3189667816
	DD	3782823440
	DD	3228777414
	DD	2342635878
	DD	3189667569
	DD	2067581456
	DD	3228777421
	DD	2489432283
	DD	3189667708
	DD	247535056
	DD	3228777428
	DD	2233925548
	DD	3189668023
	DD	2618423584
	DD	3228777434
	DD	2884435678
	DD	3189668162
	DD	591076000
	DD	3228777441
	DD	2793733516
	DD	3189667544
	DD	2756182032
	DD	3228777447
	DD	726899401
	DD	3189668194
	DD	524554032
	DD	3228777454
	DD	3888914666
	DD	3189668216
	DD	2486865376
	DD	3228777460
	DD	2627594277
	DD	3189667420
	DD	53912208
	DD	3228777467
	DD	2333314786
	DD	3189667677
	DD	1816352000
	DD	3228777473
	DD	860249135
	DD	3189668031
	DD	3479932544
	DD	3228777479
	DD	1333576769
	DD	3189667862
	DD	750393936
	DD	3228777486
	DD	3863808803
	DD	3189667876
	DD	2218370608
	DD	3228777492
	DD	988274049
	DD	3189667895
	DD	3589587648
	DD	3228777498
	DD	1363020542
	DD	3189667588
	DD	569762768
	DD	3228777505
	DD	2860785679
	DD	3189668116
	DD	1749508368
	DD	3228777511
	DD	3454164214
	DD	3189667505
	DD	2834527744
	DD	3228777517
	DD	551781933
	DD	3189668193
	DD	3825484512
	DD	3228777523
	DD	2204991099
	DD	3189668076
	DD	428068000
	DD	3228777530
	DD	330816187
	DD	3189667835
	DD	1232862576
	DD	3228777536
	DD	1875584004
	DD	3189667900
	DD	1945543984
	DD	3228777542
	DD	3147029736
	DD	3189667368
	DD	2566748560
	DD	3228777548
	DD	2759544833
	DD	3189667927
	DD	3097106128
	DD	3228777554
	DD	2378335007
	DD	3189667440
	DD	3537239968
	DD	3228777560
	DD	59134449
	DD	3189667766
	DD	3887767008
	DD	3228777566
	DD	2237380018
	DD	3189668142
	DD	4149297872
	DD	3228777572
	DD	2899689733
	DD	3189667692
	DD	27469632
	DD	3228777579
	DD	2818642709
	DD	3189667832
	DD	112815152
	DD	3228777585
	DD	4092579806
	DD	3189667420
	DD	110959312
	DD	3228777591
	DD	936570796
	DD	3189668056
	DD	22488368
	DD	3228777597
	DD	365917996
	DD	3189668022
	DD	4142949952
	DD	3228777602
	DD	3149756965
	DD	3189667408
	DD	3882983968
	DD	3228777608
	DD	4081670258
	DD	3189668028
	DD	3538126528
	DD	3228777614
	DD	1719873156
	DD	3189668130
	DD	3108940752
	DD	3228777620
	DD	3284322898
	DD	3189668289
	DD	2595984192
	DD	3228777626
	DD	1642430589
	DD	3189668010
	DD	1999808880
	DD	3228777632
	DD	4232900950
	DD	3189667360
	DD	1320961392
	DD	3228777638
	DD	346567365
	DD	3189667513
	DD	559982960
	DD	3228777644
	DD	2138178062
	DD	3189668130
	DD	4012376832
	DD	3228777649
	DD	2536292360
	DD	3189667756
	DD	3088739088
	DD	3228777655
	DD	2310947316
	DD	3189668224
	DD	2084562592
	DD	3228777661
	DD	719337470
	DD	3189667732
	DD	1000367760
	DD	3228777667
	DD	2925814745
	DD	3189668182
	DD	4131637328
	DD	3228777672
	DD	2769058114
	DD	3189668120
	DD	2888947152
	DD	3228777678
	DD	2397746692
	DD	3189667888
	DD	1567770080
	DD	3228777684
	DD	1319666757
	DD	3189667549
	DD	168606816
	DD	3228777690
	DD	3249166601
	DD	3189667815
	DD	2986920608
	DD	3228777695
	DD	3117551188
	DD	3189667844
	DD	1433268080
	DD	3228777701
	DD	2392446108
	DD	3189668088
	DD	4098070400
	DD	3228777706
	DD	1952984424
	DD	3189667965
	DD	2391874944
	DD	3228777712
	DD	3260016359
	DD	3189667565
	DD	610126416
	DD	3228777718
	DD	1206683346
	DD	3189668275
	DD	3048265088
	DD	3228777723
	DD	3467821979
	DD	3189668220
	DD	1116824880
	DD	3228777729
	DD	3688655521
	DD	3189667898
	DD	3406204528
	DD	3228777734
	DD	3255713182
	DD	3189667556
	DD	1326929264
	DD	3228777740
	DD	200527713
	DD	3189667591
	DD	3469389248
	DD	3228777745
	DD	1129326388
	DD	3189667868
	DD	1244101248
	DD	3228777751
	DD	308865650
	DD	3189668017
	DD	3241447056
	DD	3228777756
	DD	3090939005
	DD	3189667668
	DD	871935152
	DD	3228777762
	DD	3265000937
	DD	3189667670
	DD	2725939152
	DD	3228777767
	DD	4229796659
	DD	3189668178
	DD	213959504
	DD	3228777773
	DD	1991139447
	DD	3189667724
	DD	1926361824
	DD	3228777778
	DD	2528808771
	DD	3189668376
	DD	3568606000
	DD	3228777783
	DD	4260639448
	DD	3189667546
	DD	846147968
	DD	3228777789
	DD	806895635
	DD	3189668179
	DD	2349341824
	DD	3228777794
	DD	3346322191
	DD	3189667388
	DD	3783635920
	DD	3228777799
	DD	175610890
	DD	3189667587
	DD	854474928
	DD	3228777805
	DD	2938776958
	DD	3189668104
	DD	2152201728
	DD	3228777810
	DD	2503702909
	DD	3189668036
	DD	3382253648
	DD	3228777815
	DD	990471545
	DD	3189668020
	DD	250064432
	DD	3228777821
	DD	2919909380
	DD	3189667876
	DD	1345966144
	DD	3228777826
	DD	3002840896
	DD	3189668306
	DD	2375385488
	DD	3228777831
	DD	3730416038
	DD	3189667426
	DD	3338712928
	DD	3228777836
	DD	1372953258
	DD	3189667506
	DD	4236335536
	DD	3228777841
	DD	3486643335
	DD	3189668352
	DD	773669728
	DD	3228777847
	DD	3527808638
	DD	3189667828
	DD	1541030400
	DD	3228777852
	DD	1323886332
	DD	3189667450
	DD	2243827264
	DD	3228777857
	DD	3473574637
	DD	3189667708
	DD	2882434080
	DD	3228777862
	DD	2116524762
	DD	3189667453
	DD	3457221328
	DD	3228777867
	DD	3472145296
	DD	3189668357
	DD	3968556352
	DD	3228777872
	DD	1399438939
	DD	3189668087
	DD	121835984
	DD	3228777878
	DD	1630440586
	DD	3189667769
	DD	507355824
	DD	3228777883
	DD	414821867
	DD	3189668165
	DD	830506528
	DD	3228777888
	DD	1651489076
	DD	3189667856
	DD	1091642960
	DD	3228777893
	DD	1914960493
	DD	3189667544
	DD	1291116960
	DD	3228777898
	DD	1585226677
	DD	3189668172
	DD	1429277424
	DD	3228777903
	DD	1458536815
	DD	3189667992
	DD	1506470256
	DD	3228777908
	DD	5177277
	DD	3189667794
	DD	1523038448
	DD	3228777913
	DD	2100423580
	DD	3189667910
	DD	1479322112
	DD	3228777918
	DD	2766296967
	DD	3189668266
	DD	1375658528
	DD	3228777923
	DD	3908110271
	DD	3189667373
	DD	1212382096
	DD	3228777928
	DD	1728734206
	DD	3189667440
	DD	989824464
	DD	3228777933
	DD	2721564364
	DD	3189668188
	DD	708314544
	DD	3228777938
	DD	1548992985
	DD	3189667813
	DD	368178464
	DD	3228777943
	DD	102841028
	DD	3189668008
	DD	4264706992
	DD	3228777947
	DD	975947285
	DD	3189667700
	DD	3808286304
	DD	3228777952
	DD	3025865597
	DD	3189668024
	DD	3294201856
	DD	3228777957
	DD	905114809
	DD	3189668054
	DD	2722769184
	DD	3228777962
	DD	3281428766
	DD	3189667672
	DD	2094301216
	DD	3228777967
	DD	2831959372
	DD	3189668375
	DD	1409108384
	DD	3228777972
	DD	2859726358
	DD	3189667900
	DD	667498528
	DD	3228777977
	DD	3483539044
	DD	3189668146
	DD	4164744336
	DD	3228777981
	DD	643592870
	DD	3189667709
	DD	3311214096
	DD	3228777986
	DD	3888135264
	DD	3189667701
	DD	2402175552
	DD	3228777991
	DD	1565708850
	DD	3189668342
	DD	1437926768
	DD	3228777996
	DD	3880143694
	DD	3189667559
	DD	418763344
	DD	3228778001
	DD	2177554551
	DD	3189667774
	DD	3639945856
	DD	3228778005
	DD	632683322
	DD	3189668317
	DD	2511830656
	DD	3228778010
	DD	1595152623
	DD	3189668068
	DD	1329673632
	DD	3228778015
	DD	1921601627
	DD	3189668075
	DD	93761104
	DD	3228778020
	DD	1757445293
	DD	3189668004
	DD	3099344416
	DD	3228778024
	DD	1435123721
	DD	3189667691
	DD	1756770768
	DD	3228778029
	DD	783914014
	DD	3189667649
	DD	361287040
	DD	3228778034
	DD	2235532382
	DD	3189667506
	DD	3208137904
	DD	3228778038
	DD	1904486204
	DD	3189667498
	DD	1707663968
	DD	3228778043
	DD	41908474
	DD	3189667890
	DD	155105584
	DD	3228778048
	DD	2107716085
	DD	3189668380
	DD	2845700992
	DD	3228778052
	DD	3156927676
	DD	3189667488
	DD	1189784368
	DD	3228778057
	DD	4047204189
	DD	3189668063
	DD	3777557072
	DD	3228778061
	DD	577628716
	DD	3189667418
	DD	2019349136
	DD	3228778066
	DD	2610467089
	DD	3189667920
	DD	210390496
	DD	3228778071
	DD	2766766141
	DD	3189668118
	DD	2645909040
	DD	3228778075
	DD	3243727052
	DD	3189668176
	DD	736228768
	DD	3228778080
	DD	3122320416
	DD	3189668130
	DD	3071540880
	DD	3228778084
	DD	3131821814
	DD	3189668172
	DD	1062165440
	DD	3228778089
	DD	3213377517
	DD	3189667895
	DD	3298289744
	DD	3228778093
	DD	2564668383
	DD	3189667569
	DD	1190229968
	DD	3228778098
	DD	3238426468
	DD	3189668376
	DD	3328169632
	DD	3228778102
	DD	1168702877
	DD	3189667512
	DD	1122421104
	DD	3228778107
	DD	19789062
	DD	3189667604
	DD	3163164128
	DD	3228778111
	DD	1630085837
	DD	3189667685
	DD	860707408
	DD	3228778116
	DD	4114762240
	DD	3189667464
	DD	2805226992
	DD	3228778120
	DD	18835043
	DD	3189667485
	DD	407027936
	DD	3228778125
	DD	1254882039
	DD	3189668241
	DD	2256282720
	DD	3228778129
	DD	1578671063
	DD	3189668264
	DD	4058260128
	DD	3228778133
	DD	2087871538
	DD	3189668330
	DD	1518227216
	DD	3228778138
	DD	1874562670
	DD	3189667466
	DD	3226351136
	DD	3228778142
	DD	1975306748
	DD	3189668156
	DD	592928208
	DD	3228778147
	DD	2027557844
	DD	3189668223
	DD	2208122192
	DD	3228778151
	DD	3385698680
	DD	3189668056
	DD	3777193280
	DD	3228778155
	DD	2370310143
	DD	3189667586
	DD	1005399968
	DD	3228778160
	DD	259325820
	DD	3189668365
	DD	2482901056
	DD	3228778164
	DD	780809723
	DD	3189667443
	DD	3914951728
	DD	3228778168
	DD	212773867
	DD	3189667618
	DD	1006805600
	DD	3228778173
	DD	951469551
	DD	3189668212
	DD	2348616576
	DD	3228778177
	DD	2086655720
	DD	3189668128
	DD	3645635056
	DD	3228778181
	DD	2113468742
	DD	3189667868
	DD	603109872
	DD	3228778186
	DD	1652875995
	DD	3189667443
	DD	1811190160
	DD	3228778190
	DD	2629878773
	DD	3189668368
	DD	2975121696
	DD	3228778194
	DD	346123600
	DD	3189667455
	DD	4095115920
	DD	3228778198
	DD	3526019824
	DD	3189667996
	DD	876415552
	DD	3228778203
	DD	1439312045
	DD	3189667395
	DD	1909163616
	DD	3228778207
	DD	3349683269
	DD	3189668324
	DD	2898599856
	DD	3228778211
	DD	649479362
	DD	3189667358
	DD	3844929728
	DD	3228778215
	DD	2676997587
	DD	3189668158
	DD	453390032
	DD	3228778220
	DD	848895008
	DD	3189668036
	DD	1314117952
	DD	3228778224
	DD	3492148440
	DD	3189668045
	DD	2132347376
	DD	3228778228
	DD	281429102
	DD	3189667715
	DD	2908278048
	DD	3228778232
	DD	2940038679
	DD	3189667953
	DD	3642108336
	DD	3228778236
	DD	3909547671
	DD	3189667808
	DD	39067888
	DD	3228778241
	DD	3920787602
	DD	3189668378
	DD	689286912
	DD	3228778245
	DD	1690865565
	DD	3189667500
	DD	1297992288
	DD	3228778249
	DD	2169866692
	DD	3189667738
	DD	1865376896
	DD	3228778253
	DD	967083596
	DD	3189667983
	DD	2391632256
	DD	3228778257
	DD	1361458469
	DD	3189668348
	DD	2876948576
	DD	3228778261
	DD	32611984
	DD	3189667893
	DD	3321514720
	DD	3228778265
	DD	2193827755
	DD	3189667454
	DD	3725518240
	DD	3228778269
	DD	2245419299
	DD	3189668375
	DD	4089145456
	DD	3228778273
	DD	3497133018
	DD	3189668150
	DD	117614048
	DD	3228778278
	DD	175818393
	DD	3189668353
	DD	401042352
	DD	3228778282
	DD	3839976210
	DD	3189668213
	DD	644645552
	DD	3228778286
	DD	3041010480
	DD	3189667412
	DD	848604848
	DD	3228778290
	DD	2760940853
	DD	3189667832
	DD	1013100256
	DD	3228778294
	DD	2385564198
	DD	3189668140
	DD	1138310560
	DD	3228778298
	DD	2892472831
	DD	3189667574
	DD	1224413296
	DD	3228778302
	DD	781676890
	DD	3189667676
	DD	1271584832
	DD	3228778306
	DD	3578454272
	DD	3189667868
	DB 0
	ORG $+46
	DB	0
	DD	0
	DD	0
	DD	2256045239
	DD	3209413770
	DD	2491592457
	DD	3210460574
	DD	1804910333
	DD	3211053321
	DD	1914826022
	DD	3211505620
	DD	3803025062
	DD	3211872665
	DD	1674748349
	DD	3212097941
	DD	1798374224
	DD	3212322782
	DD	2768032015
	DD	3212547190
	DD	3136444289
	DD	3212771167
	DD	2855105875
	DD	3212915789
	DD	2332612951
	DD	3213027349
	DD	1363424900
	DD	3213138696
	DD	3437629661
	DD	3213249830
	DD	3435456176
	DD	3213360753
	DD	512327647
	DD	3213471466
	DD	2393978768
	DD	3213581968
	DD	3901767770
	DD	3213692261
	DD	4132692718
	DD	3213802346
	DD	1082284973
	DD	3213898832
	DD	2823056504
	DD	3213953667
	DD	366152305
	DD	3214008400
	DD	3959556563
	DD	3214063029
	DD	2367110801
	DD	3214117557
	DD	1523289056
	DD	3214171983
	DD	3058430271
	DD	3214226307
	DD	3839215
	DD	3214280531
	DD	2561657861
	DD	3214334653
	DD	3745193976
	DD	3214388675
	DD	853823952
	DD	3214442598
	DD	4062993129
	DD	3214496420
	DD	2064542556
	DD	3214550144
	DD	721479184
	DD	3214603769
	DD	1593203319
	DD	3214657295
	DD	1935571978
	DD	3214710723
	DD	3290896245
	DD	3214764053
	DD	2898068879
	DD	3214817286
	DD	2282560537
	DD	3214870422
	DD	2961513548
	DD	3214923461
	DD	1074417563
	DD	3214955210
	DD	2820079616
	DD	3214981633
	DD	1021551109
	DD	3215008009
	DD	716013687
	DD	3215034337
	DD	2641666110
	DD	3215060617
	DD	3237753164
	DD	3215086850
	DD	3234528909
	DD	3215113036
	DD	3358317786
	DD	3215139175
	DD	36575458
	DD	3215165268
	DD	2577785893
	DD	3215191313
	DD	3106717932
	DD	3215217312
	DD	2334256463
	DD	3215243265
	DD	967494986
	DD	3215269172
	DD	4004729818
	DD	3215295032
	DD	3555617588
	DD	3215320847
	DD	316038151
	DD	3215346617
	DD	3568120799
	DD	3215372340
	DD	1115499161
	DD	3215398019
	DD	2233009907
	DD	3215423652
	DD	3306979906
	DD	3215449240
	DD	720153405
	DD	3215474784
	DD	3736618996
	DD	3215500282
	DD	142096100
	DD	3215525737
	DD	3488599868
	DD	3215551146
	DD	1554858050
	DD	3215576512
	DD	3590975466
	DD	3215601833
	DD	1663752343
	DD	3215627111
	DD	721479184
	DD	3215652345
	DD	1414091172
	DD	3215677535
	DD	93191556
	DD	3215702682
	DD	1696976726
	DD	3215727785
	DD	2570390009
	DD	3215752845
	DD	3350046349
	DD	3215777862
	DD	374320308
	DD	3215802837
	DD	2863237644
	DD	3215827768
	DD	2853726453
	DD	3215852657
	DD	969442960
	DD	3215877504
	DD	2125826056
	DD	3215902308
	DD	2645217056
	DD	3215927070
	DD	3141783046
	DD	3215951790
	DD	4226570871
	DD	3215976468
	DD	3253764118
	DD	3215991848
	DD	999795016
	DD	3216004146
	DD	4242218727
	DD	3216016422
	DD	395520918
	DD	3216028679
	DD	2642482782
	DD	3216040914
	DD	2689550894
	DD	3216053129
	DD	831618312
	DD	3216065324
	DD	1657067230
	DD	3216077498
	DD	1162876950
	DD	3216089652
	DD	3934502846
	DD	3216101785
	DD	1671049587
	DD	3216113899
	DD	3250051821
	DD	3216125992
	DD	367745354
	DD	3216138066
	DD	1898814972
	DD	3216150119
	DD	3831632753
	DD	3216162152
	DD	2153169263
	DD	3216174166
	DD	1438937368
	DD	3216186160
	DD	1968099508
	DD	3216198134
	DD	4018444062
	DD	3216210088
	DD	3571427061
	DD	3216222023
	DD	902115705
	DD	3216233939
	DD	579197221
	DD	3216245835
	DD	2875053055
	DD	3216257711
	DD	3765767585
	DD	3216269568
	DD	3521071355
	DD	3216281406
	DD	2409382351
	DD	3216293225
	DD	697814500
	DD	3216305025
	DD	2947153405
	DD	3216316805
	DD	831995525
	DD	3216328567
	DD	3205527541
	DD	3216340309
	DD	1739796226
	DD	3216352033
	DD	990487678
	DD	3216363738
	DD	1217066237
	DD	3216375424
	DD	2677749815
	DD	3216387091
	DD	1334550573
	DD	3216398740
	DD	1738184714
	DD	3216410370
	DD	4143178441
	DD	3216421981
	DD	212908442
	DD	3216433575
	DD	3084380679
	DD	3216445149
	DD	123597797
	DD	3216456706
	DD	170206973
	DD	3216468244
	DD	3472736381
	DD	3216479763
	DD	1688602678
	DD	3216491265
	DD	3653922191
	DD	3216502748
	DD	1023779914
	DD	3216514214
	DD	2631975180
	DD	3216525661
	DD	131290539
	DD	3216537091
	DD	2353237313
	DD	3216548502
	DD	948324365
	DD	3216559896
	DD	450836243
	DD	3216571272
	DD	1098971024
	DD	3216582630
	DD	3129814575
	DD	3216593970
	DD	2484380169
	DD	3216605293
	DD	3692517238
	DD	3216616598
	DD	2693048988
	DD	3216627886
	DD	4013648344
	DD	3216639156
	DD	3590975466
	DD	3216650409
	DD	1655586284
	DD	3216661645
	DD	2731939102
	DD	3216672863
	DD	2753499254
	DD	3216684064
	DD	1947647495
	DD	3216695248
	DD	540719148
	DD	3216706415
	DD	3052977802
	DD	3216717564
	DD	1118752477
	DD	3216728697
	DD	3551215000
	DD	3216739812
	DD	1982647887
	DD	3216750911
	DD	929221623
	DD	3216761993
	DD	611131637
	DD	3216773058
	DD	1247571711
	DD	3216784106
	DD	3056740043
	DD	3216795137
	DD	1960877976
	DD	3216806152
	DD	2471177857
	DD	3216817150
	DD	507919781
	DD	3216828132
	DD	580346667
	DD	3216839097
	DD	2901768203
	DD	3216850045
	DD	3389566649
	DD	3216860977
	DD	2255137180
	DD	3216871893
	DD	4003893595
	DD	3216882792
	DD	255404805
	DD	3216893676
	DD	4103138823
	DD	3216904542
	DD	2869828094
	DD	3216915393
	DD	1057147997
	DD	3216926228
	DD	3165853160
	DD	3216937046
	DD	810881035
	DD	3216947849
	DD	2786128388
	DD	3216958635
	DD	704718315
	DD	3216969406
	DD	3358743942
	DD	3216980160
	DD	2359535366
	DD	3216990899
	DD	2202435985
	DD	3217001622
	DD	3086938536
	DD	3217012329
	DD	916690285
	DD	3217023021
	DD	2239683676
	DD	3217032432
	DD	2690954374
	DD	3217037762
	DD	4057646264
	DD	3217043084
	DD	2142368519
	DD	3217048399
	DD	1337238495
	DD	3217053706
	DD	1738982326
	DD	3217059005
	DD	3443904681
	DD	3217064296
	DD	2252923910
	DD	3217069580
	DD	2556476367
	DD	3217074856
	DD	154649628
	DD	3217080125
	DD	3732021368
	DD	3217085385
	DD	497923366
	DD	3217090639
	DD	3431084121
	DD	3217095884
	DD	4034990935
	DD	3217101122
	DD	2402663310
	DD	3217106353
	DD	2921687961
	DD	3217111576
	DD	1389319216
	DD	3217116792
	DD	2192350480
	DD	3217122000
	DD	1127247302
	DD	3217127201
	DD	2580018809
	DD	3217132394
	DD	2346350743
	DD	3217137580
	DD	516509563
	DD	3217142759
	DD	1475344637
	DD	3217147930
	DD	1017388536
	DD	3217153094
	DD	3526728381
	DD	3217158250
	DD	502171511
	DD	3217163400
	DD	622018689
	DD	3217168542
	DD	3974262445
	DD	3217173676
	DD	2056589177
	DD	3217178804
	DD	3546185020
	DD	3217183924
	DD	4234966846
	DD	3217189037
	DD	4209453505
	DD	3217194143
	DD	3555800579
	DD	3217199242
	DD	2359802410
	DD	3217204334
	DD	706894115
	DD	3217209419
	DD	2977120889
	DD	3217214496
	DD	665270808
	DD	3217219567
	DD	2445647873
	DD	3217224630
	DD	4107302903
	DD	3217229686
	DD	1438937368
	DD	3217234736
	DD	3113807214
	DD	3217239778
	DD	624953709
	DD	3217244814
	DD	60313751
	DD	1069756916
	DD	2039124234
	DD	1069751894
	DD	3639817142
	DD	1069746879
	DD	485812347
	DD	1069741872
	DD	1085767695
	DD	1069736871
	DD	1063773387
	DD	1069731877
	DD	339219334
	DD	1069726890
	DD	3126793337
	DD	1069721909
	DD	756610100
	DD	1069716936
	DD	1738980508
	DD	1069711969
	DD	1699638774
	DD	1069707009
	DD	559609852
	DD	1069702056
	DD	2535207687
	DD	1069697109
	DD	3253131575
	DD	1069692169
	DD	2635366323
	DD	1069687236
	DD	604213229
	DD	1069682310
	DD	1377255668
	DD	1069677390
	DD	582455508
	DD	1069672477
	DD	2438020609
	DD	1069667570
	DD	2572533958
	DD	1069662670
	DD	909853896
	DD	1069657777
	DD	1669112469
	DD	1069652890
	DD	479811889
	DD	1069648010
	DD	1561692097
	DD	1069643136
	DD	544859953
	DD	1069638269
	DD	1649656813
	DD	1069633408
	DD	506787744
	DD	1069628554
	DD	1337189126
	DD	1069623706
	DD	4067125179
	DD	1069618864
	DD	33219107
	DD	1069614030
	DD	2047222611
	DD	1069609201
	DD	1446341380
	DD	1069604379
	DD	2452971930
	DD	1069599563
	DD	699863591
	DD	1069594754
	DD	409986179
	DD	1069589951
	DD	1511626604
	DD	1069585154
	DD	3933354665
	DD	1069580363
	DD	3309054273
	DD	1069575579
	DD	3862823860
	DD	1069570801
	DD	1229105731
	DD	1069566030
	DD	3927521083
	DD	1069561264
	DD	3298097489
	DD	1069556505
	DD	3566038531
	DD	1069551752
	DD	733771779
	DD	1069546492
	DD	147741522
	DD	1069537011
	DD	941119220
	DD	1069527542
	DD	2976542023
	DD	1069518085
	DD	1822213927
	DD	1069508641
	DD	1636804890
	DD	1069499209
	DD	2284546202
	DD	1069489789
	DD	3630195051
	DD	1069480381
	DD	1244064508
	DD	1069470986
	DD	3581890024
	DD	1069461602
	DD	1920055674
	DD	1069452231
	DD	420362572
	DD	1069442872
	DD	3245124339
	DD	1069433524
	DD	1672262586
	DD	1069424189
	DD	4160075380
	DD	1069414865
	DD	1987496281
	DD	1069405554
	DD	3613830132
	DD	1069396254
	DD	319012134
	DD	1069386967
	DD	563343667
	DD	1069377691
	DD	4217685989
	DD	1069368426
	DD	2563457725
	DD	1069359174
	DD	4062436141
	DD	1069349933
	DD	4291983603
	DD	1069340704
	DD	3124914285
	DD	1069331487
	DD	434524433
	DD	1069322282
	DD	389557226
	DD	1069313088
	DD	2864265763
	DD	1069303905
	DD	3438410662
	DD	1069294734
	DD	1987192267
	DD	1069285575
	DD	2681248272
	DD	1069276427
	DD	1101749478
	DD	1069267291
	DD	1420266631
	DD	1069258166
	DD	3513866211
	DD	1069249052
	DD	2965108111
	DD	1069239950
	DD	3946945232
	DD	1069230859
	DD	2042852013
	DD	1069221780
	DD	1426691339
	DD	1069212712
	DD	1977810406
	DD	1069203655
	DD	3576005764
	DD	1069194609
	DD	1806553800
	DD	1069185575
	DD	845110407
	DD	1069176552
	DD	572806897
	DD	1069167540
	DD	871215109
	DD	1069158539
	DD	1622345234
	DD	1069149549
	DD	2708643653
	DD	1069140570
	DD	4012990793
	DD	1069131602
	DD	1123731696
	DD	1069122646
	DD	2514543080
	DD	1069113700
	DD	3774627456
	DD	1069104765
	DD	493612919
	DD	1069095842
	DD	1146452959
	DD	1069086929
	DD	1323620609
	DD	1069078027
	DD	910975575
	DD	1069069136
	DD	4089762193
	DD	1069060255
	DD	2156738208
	DD	1069051386
	DD	3588943832
	DD	1069042527
	DD	3978928661
	DD	1069033679
	DD	3214618865
	DD	1069024842
	DD	1184347908
	DD	1069016016
	DD	2071821874
	DD	1069007200
	DD	1471215619
	DD	1068998395
	DD	3567040008
	DD	1068989600
	DD	3954270794
	DD	1068980816
	DD	2523248584
	DD	1068972043
	DD	3459676924
	DD	1068963280
	DD	2359718512
	DD	1068954528
	DD	3409862494
	DD	1068945786
	DD	2207053396
	DD	1068937055
	DD	2938558446
	DD	1068928334
	DD	1202096535
	DD	1068919624
	DD	1185705549
	DD	1068910924
	DD	2782838657
	DD	1068902234
	DD	1592362481
	DD	1068893555
	DD	1803457173
	DD	1068884886
	DD	3310712729
	DD	1068876227
	DD	1714127196
	DD	1068867579
	DD	1204006779
	DD	1068858941
	DD	1676062187
	DD	1068850313
	DD	3026374166
	DD	1068841695
	DD	856424459
	DD	1068833088
	DD	3652963247
	DD	1068824490
	DD	2723236352
	DD	1068815903
	DD	2259754591
	DD	1068807326
	DD	2160422882
	DD	1068798759
	DD	2323505847
	DD	1068790202
	DD	2647626118
	DD	1068781655
	DD	3031762665
	DD	1068773118
	DD	3375249122
	DD	1068764591
	DD	3577772136
	DD	1068756074
	DD	3539369713
	DD	1068747567
	DD	3160429578
	DD	1068739070
	DD	2341687551
	DD	1068730583
	DD	984225923
	DD	1068722106
	DD	3284439142
	DD	1068713638
	DD	554163028
	DD	1068705181
	DD	1285444256
	DD	1068696733
	DD	1085767695
	DD	1068688295
	DD	4152891319
	DD	1068679866
	DD	1800008162
	DD	1068671448
	DD	2520515836
	DD	1068663039
	DD	1923243906
	DD	1068654640
	DD	4207288842
	DD	1068646250
	DD	687176001
	DD	1068637871
	DD	4152596484
	DD	1068629500
	DD	1623765359
	DD	1068621140
	DD	1596060423
	DD	1068612789
	DD	3975249632
	DD	1068604447
	DD	77489616
	DD	1068596116
	DD	2694095274
	DD	1068587793
	DD	3141865345
	DD	1068579480
	DD	1327852022
	DD	1068571177
	DD	1454392201
	DD	1068562883
	DD	3429171450
	DD	1068554598
	DD	2865222567
	DD	1068546323
	DD	3965826047
	DD	1068538057
	DD	2344639476
	DD	1068529801
	DD	2205565304
	DD	1068521554
	DD	3457847553
	DD	1068513316
	DD	1716070423
	DD	1068505088
	DD	2370117590
	DD	1068494794
	DD	3549949915
	DD	1068478374
	DD	2497601776
	DD	1068461973
	DD	3329575662
	DD	1068445590
	DD	1573040697
	DD	1068429226
	DD	1345699131
	DD	1068412880
	DD	2470881766
	DD	1068396552
	DD	477545290
	DD	1068380243
	DD	3780138808
	DD	1068363951
	DD	3613830132
	DD	1068347678
	DD	4099274231
	DD	1068331423
	DD	767774141
	DD	1068315187
	DD	2036114853
	DD	1068298968
	DD	3436756955
	DD	1068282767
	DD	502735958
	DD	1068266585
	DD	1652561629
	DD	1068250420
	DD	2420411682
	DD	1068234273
	DD	2635998437
	DD	1068218144
	DD	2129599017
	DD	1068202033
	DD	732052851
	DD	1068185940
	DD	2569726489
	DD	1068169864
	DD	3179609248
	DD	1068153806
	DD	2394212644
	DD	1068137766
	DD	46600658
	DD	1068121744
	DD	265354603
	DD	1068105739
	DD	2884636119
	DD	1068089751
	DD	3444184771
	DD	1068073781
	DD	1779250256
	DD	1068057829
	DD	2020590027
	DD	1068041894
	DD	4004532340
	DD	1068025976
	DD	3272973907
	DD	1068010076
	DD	3958279445
	DD	1067994193
	DD	1603410172
	DD	1067978328
	DD	341790679
	DD	1067962480
	DD	12404745
	DD	1067946649
	DD	454760348
	DD	1067930835
	DD	1508887391
	DD	1067915038
	DD	3015335440
	DD	1067899258
	DD	520204185
	DD	1067883496
	DD	2455010382
	DD	1067867750
	DD	71914560
	DD	1067852022
	DD	1803457173
	DD	1067836310
	DD	3197785334
	DD	1067820615
	DD	4098519806
	DD	1067804937
	DD	54818232
	DD	1067789277
	DD	3796209456
	DD	1067773632
	DD	2282918403
	DD	1067758005
	DD	3950504196
	DD	1067742394
	DD	55152364
	DD	1067726801
	DD	3328378382
	DD	1067711223
	DD	732385311
	DD	1067695663
	DD	704701956
	DD	1067680119
	DD	3093409710
	DD	1067664591
	DD	3452107773
	DD	1067649080
	DD	1629845684
	DD	1067633586
	DD	1771121268
	DD	1067618108
	DD	3725944001
	DD	1067602646
	DD	3049832979
	DD	1067587201
	DD	3888716792
	DD	1067571772
	DD	1799062323
	DD	1067556360
	DD	927741940
	DD	1067540964
	DD	1127129614
	DD	1067525584
	DD	2250066244
	DD	1067510220
	DD	4149857685
	DD	1067494872
	DD	2385305499
	DD	1067479541
	DD	1105606892
	DD	1067464226
	DD	330901792
	DD	1067447486
	DD	3134934760
	DD	1067416919
	DD	1744581096
	DD	1067386385
	DD	166917835
	DD	1067355883
	DD	2409924962
	DD	1067325412
	DD	3892547049
	DD	1067294973
	DD	34624089
	DD	1067264567
	DD	3436756955
	DD	1067234191
	DD	930630721
	DD	1067203848
	DD	823651223
	DD	1067173536
	DD	2834170318
	DD	1067143255
	DD	2386449519
	DD	1067113006
	DD	3495558248
	DD	1067082788
	DD	1587501032
	DD	1067052602
	DD	679083091
	DD	1067022447
	DD	493004867
	DD	1066992323
	DD	752825758
	DD	1066962230
	DD	1182960573
	DD	1066932168
	DD	1508676010
	DD	1066902137
	DD	1456087143
	DD	1066872137
	DD	752153937
	DD	1066842168
	DD	3419645073
	DD	1066812229
	DD	597265304
	DD	1066782322
	DD	604423098
	DD	1066752445
	DD	3171423441
	DD	1066722598
	DD	3734431730
	DD	1066692782
	DD	2025404984
	DD	1066662997
	DD	2072088478
	DD	1066633242
	DD	3608077800
	DD	1066603517
	DD	2072815517
	DD	1066573823
	DD	1496489755
	DD	1066544159
	DD	1615129006
	DD	1066514525
	DD	2165566144
	DD	1066484921
	DD	2885435163
	DD	1066455347
	DD	3513167923
	DD	1066425803
	DD	3281014557
	DD	1066390787
	DD	2604876893
	DD	1066331819
	DD	184567847
	DD	1066272911
	DD	4093204381
	DD	1066214061
	DD	930630721
	DD	1066155272
	DD	3068052312
	DD	1066096541
	DD	1403389294
	DD	1066037870
	DD	4015975920
	DD	1065979257
	DD	1806815988
	DD	1065920704
	DD	2858315029
	DD	1065862209
	DD	2369503088
	DD	1065803773
	DD	4130865093
	DD	1065745395
	DD	3344465584
	DD	1065687076
	DD	3803811842
	DD	1065628815
	DD	713978687
	DD	1065570613
	DD	2166438958
	DD	1065512468
	DD	3369253777
	DD	1065454381
	DD	3826935798
	DD	1065396352
	DD	1795984718
	DD	1065323547
	DD	1064675693
	DD	1065207720
	DD	182837332
	DD	1065092008
	DD	2466218622
	DD	1064976410
	DD	2643543973
	DD	1064860927
	DD	4036370876
	DD	1064745558
	DD	1379209261
	DD	1064630304
	DD	2294346567
	DD	1064515163
	DD	1522032624
	DD	1064400136
	DD	495642370
	DD	1064265805
	DD	3615182787
	DD	1064036203
	DD	1900236729
	DD	1063806828
	DD	2033862474
	DD	1063577678
	DD	2114800432
	DD	1063348753
	DD	494771388
	DD	1062984042
	DD	1967954106
	DD	1062527089
	DD	1170198565
	DD	1061933680
	DD	417502738
	DD	1060884213
	DD	0
	DD	2147483648
	DB 0
	ORG $+54
	DB	0
	DD	368569247
	DD	3223701786
	DD	368569247
	DD	3223701786
	DD	368569247
	DD	3223701786
	DD	368569247
	DD	3223701786
	DD	368569247
	DD	3223701786
	DD	368569247
	DD	3223701786
	DD	368569247
	DD	3223701786
	DD	368569247
	DD	3223701786
	DD	1671522011
	DD	1075227560
	DD	1671522011
	DD	1075227560
	DD	1671522011
	DD	1075227560
	DD	1671522011
	DD	1075227560
	DD	1671522011
	DD	1075227560
	DD	1671522011
	DD	1075227560
	DD	1671522011
	DD	1075227560
	DD	1671522011
	DD	1075227560
	DD	1691548315
	DD	3221787401
	DD	1691548315
	DD	3221787401
	DD	1691548315
	DD	3221787401
	DD	1691548315
	DD	3221787401
	DD	1691548315
	DD	3221787401
	DD	1691548315
	DD	3221787401
	DD	1691548315
	DD	3221787401
	DD	1691548315
	DD	3221787401
	DD	3700771192
	DD	1073506818
	DD	3700771192
	DD	1073506818
	DD	3700771192
	DD	1073506818
	DD	3700771192
	DD	1073506818
	DD	3700771192
	DD	1073506818
	DD	3700771192
	DD	1073506818
	DD	3700771192
	DD	1073506818
	DD	3700771192
	DD	1073506818
	DD	3698831637
	DD	3220339442
	DD	3698831637
	DD	3220339442
	DD	3698831637
	DD	3220339442
	DD	3698831637
	DD	3220339442
	DD	3698831637
	DD	3220339442
	DD	3698831637
	DD	3220339442
	DD	3698831637
	DD	3220339442
	DD	3698831637
	DD	3220339442
	DD	3207479564
	DD	1062894188
	DD	3207479564
	DD	1062894188
	DD	3207479564
	DD	1062894188
	DD	3207479564
	DD	1062894188
	DD	3207479564
	DD	1062894188
	DD	3207479564
	DD	1062894188
	DD	3207479564
	DD	1062894188
	DD	3207479564
	DD	1062894188
	DD	589282582
	DD	1068907621
	DD	589282582
	DD	1068907621
	DD	589282582
	DD	1068907621
	DD	589282582
	DD	1068907621
	DD	589282582
	DD	1068907621
	DD	589282582
	DD	1068907621
	DD	589282582
	DD	1068907621
	DD	589282582
	DD	1068907621
	DD	1325131787
	DD	3216755581
	DD	1325131787
	DD	3216755581
	DD	1325131787
	DD	3216755581
	DD	1325131787
	DD	3216755581
	DD	1325131787
	DD	3216755581
	DD	1325131787
	DD	3216755581
	DD	1325131787
	DD	3216755581
	DD	1325131787
	DD	3216755581
	DD	1668232222
	DD	1069713319
	DD	1668232222
	DD	1069713319
	DD	1668232222
	DD	1069713319
	DD	1668232222
	DD	1069713319
	DD	1668232222
	DD	1069713319
	DD	1668232222
	DD	1069713319
	DD	1668232222
	DD	1069713319
	DD	1668232222
	DD	1069713319
	DD	354868790
	DD	3217804155
	DD	354868790
	DD	3217804155
	DD	354868790
	DD	3217804155
	DD	354868790
	DD	3217804155
	DD	354868790
	DD	3217804155
	DD	354868790
	DD	3217804155
	DD	354868790
	DD	3217804155
	DD	354868790
	DD	3217804155
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
	DD	354870542
	DD	1071369083
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
	DD	1071366144
	DD	0
	DD	1071366144
	DD	0
	DD	1071366144
	DD	0
	DD	1071366144
	DD	0
	DD	1071366144
	DD	0
	DD	1071366144
	DD	0
	DD	1071366144
	DD	0
	DD	1071366144
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
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	1352597504
	DD	1070810131
	DD	3296460800
	DD	1031700412
	DD	3296460800
	DD	1031700412
	DD	3296460800
	DD	1031700412
	DD	3296460800
	DD	1031700412
	DD	3296460800
	DD	1031700412
	DD	3296460800
	DD	1031700412
	DD	3296460800
	DD	1031700412
	DD	3296460800
	DD	1031700412
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
	DD	1352628735
	DD	1070810131
	DD	1352628735
	DD	1070810131
	DD	1352628735
	DD	1070810131
	DD	1352628735
	DD	1070810131
	DD	1352628735
	DD	1070810131
	DD	1352628735
	DD	1070810131
	DD	1352628735
	DD	1070810131
	DD	1352628735
	DD	1070810131
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
_vmldLgHATab	DD	0
	DD	1071366144
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1071337728
	DD	184549376
	DD	1065092008
	DD	2099961998
	DD	3178897324
	DD	0
	DD	1071309312
	DD	931135488
	DD	1066155272
	DD	2365712557
	DD	3178155773
	DD	0
	DD	1071280896
	DD	603979776
	DD	1066752445
	DD	709057215
	DD	1031474920
	DD	0
	DD	1071252480
	DD	3437232128
	DD	1067234191
	DD	1515412199
	DD	3179085970
	DD	0
	DD	1071238272
	DD	1105723392
	DD	1067464226
	DD	153915826
	DD	3178000698
	DD	0
	DD	1071209856
	DD	3328442368
	DD	1067711223
	DD	3899912278
	DD	3177135692
	DD	0
	DD	1071181440
	DD	341835776
	DD	1067962480
	DD	2509208190
	DD	3176531222
	DD	0
	DD	1071167232
	DD	2884632576
	DD	1068089751
	DD	1030636902
	DD	1025224143
	DD	0
	DD	1071138816
	DD	3613917184
	DD	1068347678
	DD	3527163461
	DD	3177529532
	DD	0
	DD	1071124608
	DD	3549954048
	DD	1068478374
	DD	3498894081
	DD	3173000425
	DD	0
	DD	1071096192
	DD	1623785472
	DD	1068621140
	DD	2889825554
	DD	3176375375
	DD	0
	DD	1071081984
	DD	1085800448
	DD	1068688295
	DD	4015256301
	DD	3177184346
	DD	0
	DD	1071053568
	DD	3652976640
	DD	1068824490
	DD	3600693529
	DD	3175753877
	DD	0
	DD	1071039360
	DD	1592393728
	DD	1068893555
	DD	231073830
	DD	3177087939
	DD	0
	DD	1071025152
	DD	3459645440
	DD	1068963280
	DD	1740576090
	DD	1029619435
	DD	0
	DD	1070996736
	DD	3774611456
	DD	1069104765
	DD	3858552785
	DD	1028603845
	DD	0
	DD	1070982528
	DD	845086720
	DD	1069176552
	DD	3138879731
	DD	1029120443
	DD	0
	DD	1070968320
	DD	3513843712
	DD	1069249052
	DD	2107125367
	DD	1029044389
	DD	0
	DD	1070954112
	DD	434503680
	DD	1069322282
	DD	3827602229
	DD	1028932700
	DD	0
	DD	1070939904
	DD	3613851648
	DD	1069396254
	DD	1223751955
	DD	3176465139
	DD	0
	DD	1070911488
	DD	733741056
	DD	1069546492
	DD	1625232067
	DD	1029570781
	DD	0
	DD	1070897280
	DD	1511620608
	DD	1069585154
	DD	3044605139
	DD	1028090775
	DD	0
	DD	1070883072
	DD	1337196544
	DD	1069623706
	DD	2602639001
	DD	3175938675
	DD	0
	DD	1070868864
	DD	2572533760
	DD	1069662670
	DD	3067107955
	DD	1022933137
	DD	0
	DD	1070854656
	DD	559611904
	DD	1069702056
	DD	764145786
	DD	3174041535
	DD	0
	DD	1070840448
	DD	485818368
	DD	1069741872
	DD	2037567072
	DD	3175580956
	DD	0
	DD	1070826240
	DD	259604480
	DD	1069782128
	DD	4012068429
	DD	1027865895
	DD	0
	DD	1070812032
	DD	3454042112
	DD	1069822833
	DD	2867680007
	DD	3174202478
	DD	0
	DD	1070797824
	DD	2188754944
	DD	1069863999
	DD	2538655286
	DD	3175840981
	DD	0
	DD	1070783616
	DD	2965241856
	DD	1069905635
	DD	1338936972
	DD	3176093950
	DD	0
	DD	1070769408
	DD	966279168
	DD	1069947753
	DD	1774547674
	DD	3175051484
	DD	0
	DD	1070755200
	DD	1604042752
	DD	1069990363
	DD	2557470738
	DD	3174667448
	DD	0
	DD	1070740992
	DD	3417833472
	DD	1070033477
	DD	2268255117
	DD	3175678264
	DD	0
	DD	1070740992
	DD	3417833472
	DD	1070033477
	DD	2268255117
	DD	3175678264
	DD	0
	DD	1070726784
	DD	2451292160
	DD	1070077108
	DD	3757728941
	DD	1027943275
	DD	0
	DD	1070712576
	DD	929644544
	DD	1070121268
	DD	899045708
	DD	1027944939
	DD	0
	DD	1070698368
	DD	3057254400
	DD	1070165969
	DD	3880649376
	DD	3172972504
	DD	0
	DD	1070684160
	DD	2231091200
	DD	1070211226
	DD	521319256
	DD	1027600177
	DD	0
	DD	1070684160
	DD	2231091200
	DD	1070211226
	DD	521319256
	DD	1027600177
	DD	0
	DD	1070669952
	DD	2620162048
	DD	1070257052
	DD	1385613369
	DD	3176104036
	DD	0
	DD	1070655744
	DD	2096726016
	DD	1070303462
	DD	3138305819
	DD	3173646777
	DD	0
	DD	1070641536
	DD	944717824
	DD	1070350471
	DD	1065120110
	DD	1027539054
	DD	0
	DD	1070641536
	DD	944717824
	DD	1070350471
	DD	1065120110
	DD	1027539054
	DD	0
	DD	1070627328
	DD	1985789952
	DD	1070398094
	DD	3635943864
	DD	3173136490
	DD	0
	DD	1070613120
	DD	2123825152
	DD	1070446348
	DD	1125219725
	DD	3175615738
	DD	0
	DD	1070598912
	DD	1078378496
	DD	1070495250
	DD	603852726
	DD	3174570526
	DD	0
	DD	1070598912
	DD	1078378496
	DD	1070495250
	DD	603852726
	DD	3174570526
	DD	0
	DD	1070573312
	DD	1537933312
	DD	1070544817
	DD	998069198
	DD	1026662908
	DD	0
	DD	1070544896
	DD	733773824
	DD	1070595068
	DD	4061058002
	DD	3174036009
	DD	0
	DD	1070544896
	DD	733773824
	DD	1070595068
	DD	4061058002
	DD	3174036009
	DD	0
	DD	1070516480
	DD	3897544704
	DD	1070621058
	DD	951856294
	DD	1026731877
	DD	0
	DD	1070516480
	DD	3897544704
	DD	1070621058
	DD	951856294
	DD	1026731877
	DD	0
	DD	1070488064
	DD	493535232
	DD	1070646897
	DD	3852369308
	DD	3173264746
	DD	0
	DD	1070459648
	DD	463249408
	DD	1070673107
	DD	2853152111
	DD	3174564937
	DD	0
	DD	1070459648
	DD	463249408
	DD	1070673107
	DD	2853152111
	DD	3174564937
	DD	0
	DD	1070431232
	DD	3186585600
	DD	1070699699
	DD	1874718356
	DD	3174139933
	DD	0
	DD	1070431232
	DD	3186585600
	DD	1070699699
	DD	1874718356
	DD	3174139933
	DD	0
	DD	1070402816
	DD	1525858304
	DD	1070726686
	DD	3039843523
	DD	1024724665
	DD	0
	DD	1070402816
	DD	1525858304
	DD	1070726686
	DD	3039843523
	DD	1024724665
	DD	0
	DD	1070374400
	DD	3425300480
	DD	1070754078
	DD	1303046649
	DD	1022401701
	DD	0
	DD	1070374400
	DD	3425300480
	DD	1070754078
	DD	1303046649
	DD	1022401701
	DD	0
	DD	1070345984
	DD	1980465152
	DD	1070781889
	DD	3188656319
	DD	1027271390
	DD	0
	DD	1070345984
	DD	1980465152
	DD	1070781889
	DD	3188656319
	DD	1027271390
	DD	0
	DD	1070317568
	DD	1352630272
	DD	1070810131
	DD	3090895658
	DD	3174564915
	DD	1352630272
	DD	1070810131
	DD	3090895658
	DD	3174564915
	DD	64
	DD	1120927744
	DD	0
	DD	1096810496
	DD	0
	DD	1064828928
	DD	0
	DD	1135607808
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	1071366144
	DD	3207479559
	DD	1062894188
	DD	3698831637
	DD	3220339442
	DD	3700832817
	DD	1073506818
	DD	1691624569
	DD	3221787401
	DD	2065628764
	DD	1075227551
	DD	1770847080
	DD	3223701774
	DD	3786517112
	DD	1077250450
	DD	1316351650
	DD	3225793313
_2il0floatpacket_28	DD	000000000H,043380000H,000000000H,043380000H
_2il0floatpacket_102	DD	000000000H,080000000H,000000000H,000000000H
_2il0floatpacket_101	DD	000000000H,03ff00000H
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
