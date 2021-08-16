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
	PUBLIC __svml_exp1_ha_e9

__svml_exp1_ha_e9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 184
        vmovapd   xmm3, xmm0
        mov       QWORD PTR [168+rsp], r13
        mov       eax, 2147483647
        vmulsd    xmm1, xmm3, QWORD PTR [__svml_dexp_ha_data_internal+1024]
        mov       edx, 1082532650
        vmovsd    xmm2, QWORD PTR [__svml_dexp_ha_data_internal+1088]
        vmovd     xmm5, eax
        vmovsd    xmm0, QWORD PTR [__svml_dexp_ha_data_internal+1536]
        lea       r8, QWORD PTR [__ImageBase]
        vaddsd    xmm1, xmm1, xmm2
        vsubsd    xmm4, xmm1, xmm2
        vpshufd   xmm2, xmm3, 85
        vpand     xmm2, xmm2, xmm5
        vmovd     xmm5, edx
        vpcmpgtd  xmm2, xmm2, xmm5
        lea       r13, QWORD PTR [95+rsp]
        vmovmskps eax, xmm2
        and       r13, -64
        vmovq     xmm2, QWORD PTR [__svml_dexp_ha_data_internal+1600]
        vpand     xmm5, xmm1, xmm2
        vpandn    xmm1, xmm2, xmm1
        vpshufd   xmm5, xmm5, 0
        vpslld    xmm5, xmm5, 4
        vmovd     ecx, xmm5
        vmulsd    xmm5, xmm4, QWORD PTR [__svml_dexp_ha_data_internal+1216]
        vmulsd    xmm4, xmm4, QWORD PTR [__svml_dexp_ha_data_internal+1280]
        vsubsd    xmm5, xmm3, xmm5
        movsxd    rcx, ecx
        vsubsd    xmm5, xmm5, xmm4
        vmulsd    xmm0, xmm0, xmm5
        mov       QWORD PTR [176+rsp], r13
        vaddsd    xmm4, xmm0, QWORD PTR [__svml_dexp_ha_data_internal+1472]
        vmulsd    xmm0, xmm4, xmm5
        vmulsd    xmm4, xmm5, xmm5
        vaddsd    xmm0, xmm0, QWORD PTR [__svml_dexp_ha_data_internal+1408]
        vmulsd    xmm0, xmm0, xmm5
        vaddsd    xmm0, xmm0, QWORD PTR [__svml_dexp_ha_data_internal+1344]
        vmulsd    xmm4, xmm0, xmm4
        vaddsd    xmm5, xmm4, xmm5
        vmulsd    xmm0, xmm5, QWORD PTR [imagerel(__svml_dexp_ha_data_internal)+r8+rcx]
        vaddsd    xmm4, xmm0, QWORD PTR [imagerel(__svml_dexp_ha_data_internal)+8+r8+rcx]
        vpsllq    xmm0, xmm1, 46
        vaddsd    xmm5, xmm4, QWORD PTR [imagerel(__svml_dexp_ha_data_internal)+r8+rcx]
        vpaddq    xmm0, xmm5, xmm0
        test      al, 1
        jne       _B1_3

_B1_2::

        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 184
        ret

_B1_3::

        vcmpgtsd  xmm1, xmm3, QWORD PTR [__svml_dexp_ha_data_internal+1984]
        vcmpltsd  xmm2, xmm3, QWORD PTR [__svml_dexp_ha_data_internal+2048]
        vblendvpd xmm0, xmm0, XMMWORD PTR [_2il0floatpacket_33], xmm1
        vorpd     xmm4, xmm1, xmm2
        vmovmskpd edx, xmm4
        vandnpd   xmm0, xmm2, xmm0
        not       edx
        and       edx, eax
        and       edx, 1
        je        _B1_2

_B1_4::

        vmovsd    QWORD PTR [r13], xmm3
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B1_7

_B1_5::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B1_2

_B1_7::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dexp_ha_cout_rare_internal
        jmp       _B1_5
        ALIGN     16

_B1_8::

__svml_exp1_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_exp1_ha_e9_B1_B7:
	DD	268033
	DD	1430551
	DD	1507595

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_8
	DD	imagerel _unwind___svml_exp1_ha_e9_B1_B7

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_exp8_ha_z0

__svml_exp8_ha_z0	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L4::

        vmovups   zmm4, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+256]
        vmovups   zmm1, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+320]
        vmovups   zmm2, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+384]
        vmovups   zmm3, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+448]
        vmovups   zmm5, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+576]
        vmovups   zmm27, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+640]
        vfmadd213pd zmm4, zmm0, zmm1 {rz-sae}
        vmovups   zmm22, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+704]
        vmovups   zmm24, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+768]
        vmovups   zmm23, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+832]
        vmovups   zmm26, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+896]
        vmovups   zmm28, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+128]
        vmovups   zmm29, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512]
        vsubpd    zmm31, zmm4, zmm1 {rn-sae}
        vpermt2pd zmm28, zmm4, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+192]
        vpermt2pd zmm29, zmm4, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+64]
        vfnmadd231pd zmm0, zmm2, zmm31 {rn-sae}
        vfnmadd231pd zmm0, zmm3, zmm31 {rn-sae}
        vandpd    zmm30, zmm0, ZMMWORD PTR [__svml_dexp_ha_data_internal_avx512+512]
        vmulpd    zmm25, zmm30, zmm30 {rn-sae}
        vfmadd231pd zmm27, zmm5, zmm30 {rn-sae}
        vfmadd231pd zmm24, zmm22, zmm30 {rn-sae}
        vfmadd231pd zmm26, zmm23, zmm30 {rn-sae}
        vfmadd213pd zmm27, zmm25, zmm24 {rn-sae}
        vfmadd213pd zmm27, zmm25, zmm26 {rn-sae}
        vfmadd213pd zmm30, zmm27, zmm28 {rn-sae}
        vfmadd213pd zmm30, zmm29, zmm29 {rn-sae}
        vscalefpd zmm0, zmm30, zmm31 {rn-sae}
        ret
        ALIGN     16

_B2_2::

__svml_exp8_ha_z0 ENDP

_TEXT	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_exp1_ha_l9

__svml_exp1_ha_l9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L5::

        sub       rsp, 200
        vmovapd   xmm4, xmm0
        vmovups   XMMWORD PTR [176+rsp], xmm8
        vmovapd   xmm1, xmm4
        mov       QWORD PTR [168+rsp], r13
        lea       rdx, QWORD PTR [__ImageBase]
        vmovsd    xmm2, QWORD PTR [__svml_dexp_ha_data_internal+1088]
        lea       r13, QWORD PTR [95+rsp]
        vfmadd132sd xmm1, xmm2, QWORD PTR [__svml_dexp_ha_data_internal+1024]
        and       r13, -64
        vmovsd    xmm8, QWORD PTR [__svml_dexp_ha_data_internal+1728]
        vmovsd    xmm5, QWORD PTR [__svml_dexp_ha_data_internal+1536]
        mov       QWORD PTR [192+rsp], r13
        vsubsd    xmm3, xmm1, xmm2
        vandpd    xmm1, xmm4, xmm8
        vmovapd   xmm8, xmm3
        vfnmadd132sd xmm8, xmm4, QWORD PTR [__svml_dexp_ha_data_internal+1216]
        vcmpnlesd xmm2, xmm1, QWORD PTR [__svml_dexp_ha_data_internal+1792]
        vaddsd    xmm1, xmm3, QWORD PTR [__svml_dexp_ha_data_internal+1152]
        vfnmadd132sd xmm3, xmm8, QWORD PTR [__svml_dexp_ha_data_internal+1280]
        vmovmskpd ecx, xmm2
        vmovq     xmm2, QWORD PTR [__svml_dexp_ha_data_internal+1664]
        vpand     xmm0, xmm1, xmm2
        vfmadd213sd xmm5, xmm3, QWORD PTR [__svml_dexp_ha_data_internal+1472]
        vmulsd    xmm8, xmm3, xmm3
        vmovd     eax, xmm0
        vfmadd213sd xmm5, xmm3, QWORD PTR [__svml_dexp_ha_data_internal+1408]
        vfmadd213sd xmm5, xmm3, QWORD PTR [__svml_dexp_ha_data_internal+1344]
        movsxd    rax, eax
        vfmadd213sd xmm5, xmm8, xmm3
        vpandn    xmm3, xmm2, xmm1
        vmovddup  xmm0, QWORD PTR [imagerel(__svml_dexp_ha_data_internal)+8+rdx+rax]
        vfmadd132sd xmm5, xmm0, QWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rdx+rax]
        vpsllq    xmm0, xmm3, 42
        vaddsd    xmm5, xmm5, QWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rdx+rax]
        vpaddq    xmm0, xmm5, xmm0
        test      cl, 1
        jne       _B3_3

_B3_2::

        vmovups   xmm8, XMMWORD PTR [176+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 200
        ret

_B3_3::

        vcmpgtsd  xmm1, xmm4, QWORD PTR [__svml_dexp_ha_data_internal+1984]
        vcmpltsd  xmm2, xmm4, QWORD PTR [__svml_dexp_ha_data_internal+2048]
        vblendvpd xmm0, xmm0, XMMWORD PTR [_2il0floatpacket_33], xmm1
        vorpd     xmm3, xmm1, xmm2
        vmovmskpd eax, xmm3
        vandnpd   xmm0, xmm2, xmm0
        andn      eax, eax, ecx
        and       eax, 1
        je        _B3_2

_B3_4::

        vmovsd    QWORD PTR [r13], xmm4
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B3_7

_B3_5::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B3_2

_B3_7::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dexp_ha_cout_rare_internal
        jmp       _B3_5
        ALIGN     16

_B3_8::

__svml_exp1_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_exp1_ha_l9_B1_B7:
	DD	402433
	DD	1430564
	DD	755736
	DD	1638667

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_8
	DD	imagerel _unwind___svml_exp1_ha_l9_B1_B7

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_exp2_ha_ex

__svml_exp2_ha_ex	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L10::

        sub       rsp, 280
        movaps    xmm4, xmm0
        movups    XMMWORD PTR [224+rsp], xmm14
        lea       rcx, QWORD PTR [__ImageBase]
        movups    XMMWORD PTR [240+rsp], xmm13
        movups    XMMWORD PTR [192+rsp], xmm8
        movups    XMMWORD PTR [208+rsp], xmm7
        mov       QWORD PTR [256+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movups    xmm13, XMMWORD PTR [__svml_dexp_ha_data_internal+1024]
        and       r13, -64
        mulpd     xmm13, xmm4
        movups    xmm3, XMMWORD PTR [__svml_dexp_ha_data_internal+1088]
        addpd     xmm13, xmm3
        movaps    xmm14, xmm13
        subpd     xmm14, xmm3
        movq      xmm2, QWORD PTR [__svml_dexp_ha_data_internal+1856]
        pshufd    xmm1, xmm4, 221
        movdqu    xmm3, XMMWORD PTR [__svml_dexp_ha_data_internal+1600]
        pand      xmm1, xmm2
        movdqa    xmm2, xmm3
        pandn     xmm3, xmm13
        movq      xmm0, QWORD PTR [__svml_dexp_ha_data_internal+1920]
        pand      xmm2, xmm13
        pcmpgtd   xmm1, xmm0
        psllq     xmm3, 46
        pshufd    xmm0, xmm2, 136
        pslld     xmm0, 4
        movmskps  r8d, xmm1
        pshufd    xmm1, xmm0, 1
        movups    xmm5, XMMWORD PTR [__svml_dexp_ha_data_internal+1216]
        movd      eax, xmm0
        movd      edx, xmm1
        movups    xmm7, XMMWORD PTR [__svml_dexp_ha_data_internal+1280]
        movups    xmm8, XMMWORD PTR [__svml_dexp_ha_data_internal+1536]
        mulpd     xmm5, xmm14
        mulpd     xmm7, xmm14
        movsxd    rax, eax
        movsxd    rdx, edx
        mov       QWORD PTR [264+rsp], r13
        movups    xmm2, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rcx+rax]
        movups    xmm1, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rcx+rdx]
        movaps    xmm0, xmm2
        unpcklpd  xmm0, xmm1
        unpckhpd  xmm2, xmm1
        movaps    xmm1, xmm4
        subpd     xmm1, xmm5
        subpd     xmm1, xmm7
        mulpd     xmm8, xmm1
        addpd     xmm8, XMMWORD PTR [__svml_dexp_ha_data_internal+1472]
        mulpd     xmm8, xmm1
        addpd     xmm8, XMMWORD PTR [__svml_dexp_ha_data_internal+1408]
        mulpd     xmm8, xmm1
        movaps    xmm5, xmm1
        mulpd     xmm5, xmm1
        addpd     xmm8, XMMWORD PTR [__svml_dexp_ha_data_internal+1344]
        mulpd     xmm5, xmm8
        addpd     xmm1, xmm5
        mulpd     xmm1, xmm0
        addpd     xmm2, xmm1
        addpd     xmm0, xmm2
        paddq     xmm0, xmm3
        test      r8d, 3
        jne       _B4_3

_B4_2::

        movups    xmm7, XMMWORD PTR [208+rsp]
        movups    xmm8, XMMWORD PTR [192+rsp]
        movups    xmm13, XMMWORD PTR [240+rsp]
        movups    xmm14, XMMWORD PTR [224+rsp]
        mov       r13, QWORD PTR [256+rsp]
        add       rsp, 280
        ret

_B4_3::

        movups    xmm3, XMMWORD PTR [__svml_dexp_ha_data_internal+1984]
        movaps    xmm2, xmm4
        cmpltpd   xmm3, xmm4
        cmpltpd   xmm2, XMMWORD PTR [__svml_dexp_ha_data_internal+2048]
        movaps    xmm1, xmm3
        andnps    xmm1, xmm0
        movups    xmm0, XMMWORD PTR [_2il0floatpacket_33]
        andps     xmm0, xmm3
        orps      xmm3, xmm2
        movmskpd  eax, xmm3
        orps      xmm1, xmm0
        movaps    xmm0, xmm2
        andnps    xmm0, xmm1
        not       eax
        and       eax, r8d
        and       eax, 3
        je        _B4_2

_B4_4::

        movups    XMMWORD PTR [r13], xmm4
        movups    XMMWORD PTR [64+r13], xmm0
        je        _B4_2

_B4_7::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

_B4_8::

        mov       ecx, ebx
        mov       edx, 1
        shl       edx, cl
        test      esi, edx
        jne       _B4_11

_B4_9::

        inc       ebx
        cmp       ebx, 2
        jl        _B4_8

_B4_10::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [64+r13]
        jmp       _B4_2

_B4_11::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dexp_ha_cout_rare_internal
        jmp       _B4_9
        ALIGN     16

_B4_12::

__svml_exp2_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_exp2_ha_ex_B1_B4:
	DD	802817
	DD	2151488
	DD	882744
	DD	821296
	DD	1038375
	DD	976919
	DD	2294027

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_7
	DD	imagerel _unwind___svml_exp2_ha_ex_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_exp2_ha_ex_B7_B11:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B4_1
	DD	imagerel _B4_7
	DD	imagerel _unwind___svml_exp2_ha_ex_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_7
	DD	imagerel _B4_12
	DD	imagerel _unwind___svml_exp2_ha_ex_B7_B11

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_exp4_ha_e9

__svml_exp4_ha_e9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L25::

        sub       rsp, 552
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [464+rsp], ymm9
        vmovups   YMMWORD PTR [496+rsp], ymm7
        vmovups   YMMWORD PTR [432+rsp], ymm6
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [335+rsp]
        vmovapd   ymm5, ymm0
        and       r13, -64
        vmovupd   ymm4, YMMWORD PTR [__svml_dexp_ha_data_internal+1088]
        vmulpd    ymm2, ymm5, YMMWORD PTR [__svml_dexp_ha_data_internal+1024]
        vaddpd    ymm3, ymm4, ymm2
        vsubpd    ymm0, ymm3, ymm4
        mov       QWORD PTR [536+rsp], r13
        vextractf128 xmm6, ymm5, 1
        vshufps   xmm2, xmm5, xmm6, 221
        vandps    xmm4, xmm2, XMMWORD PTR [__svml_dexp_ha_data_internal+1856]
        vpcmpgtd  xmm1, xmm4, XMMWORD PTR [__svml_dexp_ha_data_internal+1920]
        vmovupd   xmm4, XMMWORD PTR [__svml_dexp_ha_data_internal+1600]
        vmovmskps edx, xmm1
        vandps    xmm9, xmm3, xmm4
        vextractf128 xmm2, ymm3, 1
        vpandn    xmm3, xmm4, xmm3
        vandps    xmm7, xmm2, xmm4
        vpandn    xmm2, xmm4, xmm2
        vshufps   xmm6, xmm9, xmm7, 136
        vpsllq    xmm4, xmm2, 46
        vpslld    xmm9, xmm6, 4
        vmovd     ecx, xmm9
        vpextrd   r8d, xmm9, 1
        movsxd    rcx, ecx
        movsxd    r8, r8d
        vpextrd   r9d, xmm9, 2
        vpextrd   r10d, xmm9, 3
        movsxd    r9, r9d
        movsxd    r10, r10d
        vmovupd   xmm7, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rax+rcx]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rax+r8]
        vinsertf128 ymm9, ymm7, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rax+r9], 1
        vinsertf128 ymm6, ymm1, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rax+r10], 1
        vunpcklpd ymm1, ymm9, ymm6
        vunpckhpd ymm6, ymm9, ymm6
        vmulpd    ymm9, ymm0, YMMWORD PTR [__svml_dexp_ha_data_internal+1216]
        vmulpd    ymm0, ymm0, YMMWORD PTR [__svml_dexp_ha_data_internal+1280]
        vsubpd    ymm9, ymm5, ymm9
        vsubpd    ymm9, ymm9, ymm0
        vmulpd    ymm7, ymm9, YMMWORD PTR [__svml_dexp_ha_data_internal+1536]
        vaddpd    ymm0, ymm7, YMMWORD PTR [__svml_dexp_ha_data_internal+1472]
        vmulpd    ymm7, ymm9, ymm0
        vaddpd    ymm0, ymm7, YMMWORD PTR [__svml_dexp_ha_data_internal+1408]
        vmulpd    ymm7, ymm9, ymm9
        vmulpd    ymm0, ymm9, ymm0
        vaddpd    ymm0, ymm0, YMMWORD PTR [__svml_dexp_ha_data_internal+1344]
        vmulpd    ymm7, ymm7, ymm0
        vaddpd    ymm9, ymm9, ymm7
        vmulpd    ymm0, ymm1, ymm9
        vaddpd    ymm6, ymm6, ymm0
        vpsllq    xmm0, xmm3, 46
        vaddpd    ymm1, ymm1, ymm6
        vextractf128 xmm6, ymm1, 1
        vpaddq    xmm3, xmm1, xmm0
        vpaddq    xmm6, xmm6, xmm4
        vinsertf128 ymm0, ymm3, xmm6, 1
        test      edx, edx
        jne       _B5_3

_B5_2::

        vmovups   ymm6, YMMWORD PTR [432+rsp]
        vmovups   ymm7, YMMWORD PTR [496+rsp]
        vmovups   ymm9, YMMWORD PTR [464+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B5_3::

        vcmpgt_oqpd ymm1, ymm5, YMMWORD PTR [__svml_dexp_ha_data_internal+1984]
        vcmplt_oqpd ymm2, ymm5, YMMWORD PTR [__svml_dexp_ha_data_internal+2048]
        vblendvpd ymm0, ymm0, YMMWORD PTR [_2il0floatpacket_34], ymm1
        vorpd     ymm3, ymm1, ymm2
        vandnpd   ymm0, ymm2, ymm0
        vextractf128 xmm4, ymm3, 1
        vshufps   xmm6, xmm3, xmm4, 221
        vmovmskps ecx, xmm6
        not       ecx
        and       ecx, edx
        je        _B5_2

_B5_4::

        vmovupd   YMMWORD PTR [r13], ymm5
        vmovupd   YMMWORD PTR [64+r13], ymm0
        je        _B5_2

_B5_7::

        xor       eax, eax
        vmovups   YMMWORD PTR [224+rsp], ymm8
        vmovups   YMMWORD PTR [192+rsp], ymm10
        vmovups   YMMWORD PTR [160+rsp], ymm11
        vmovups   YMMWORD PTR [128+rsp], ymm12
        vmovups   YMMWORD PTR [96+rsp], ymm13
        vmovups   YMMWORD PTR [64+rsp], ymm14
        vmovups   YMMWORD PTR [32+rsp], ymm15
        mov       QWORD PTR [264+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [256+rsp], rsi
        mov       esi, ecx

_B5_8::

        bt        esi, ebx
        jc        _B5_11

_B5_9::

        inc       ebx
        cmp       ebx, 4
        jl        _B5_8

_B5_10::

        vmovups   ymm8, YMMWORD PTR [224+rsp]
        vmovups   ymm10, YMMWORD PTR [192+rsp]
        vmovups   ymm11, YMMWORD PTR [160+rsp]
        vmovups   ymm12, YMMWORD PTR [128+rsp]
        vmovups   ymm13, YMMWORD PTR [96+rsp]
        vmovups   ymm14, YMMWORD PTR [64+rsp]
        vmovups   ymm15, YMMWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [264+rsp]
        mov       rsi, QWORD PTR [256+rsp]
        jmp       _B5_2

_B5_11::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dexp_ha_cout_rare_internal
        jmp       _B5_9
        ALIGN     16

_B5_12::

__svml_exp4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_exp4_ha_e9_B1_B4:
	DD	668929
	DD	4379701
	DD	1796141
	DD	2062372
	DD	1939483
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_7
	DD	imagerel _unwind___svml_exp4_ha_e9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_exp4_ha_e9_B7_B11:
	DD	1198625
	DD	2122826
	DD	2176064
	DD	194616
	DD	321586
	DD	448556
	DD	575526
	DD	702493
	DD	829460
	DD	952331
	DD	imagerel _B5_1
	DD	imagerel _B5_7
	DD	imagerel _unwind___svml_exp4_ha_e9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_7
	DD	imagerel _B5_12
	DD	imagerel _unwind___svml_exp4_ha_e9_B7_B11

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_exp2_ha_l9

__svml_exp2_ha_l9	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L52::

        sub       rsp, 264
        vmovapd   xmm5, xmm0
        vmovups   XMMWORD PTR [192+rsp], xmm10
        lea       rcx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [208+rsp], xmm9
        vmovups   XMMWORD PTR [224+rsp], xmm8
        mov       QWORD PTR [240+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovupd   xmm2, XMMWORD PTR [__svml_dexp_ha_data_internal+1024]
        and       r13, -64
        vmovupd   xmm3, XMMWORD PTR [__svml_dexp_ha_data_internal+1088]
        vfmadd213pd xmm2, xmm5, xmm3
        vmovupd   xmm8, XMMWORD PTR [__svml_dexp_ha_data_internal+1216]
        vmovupd   xmm9, XMMWORD PTR [__svml_dexp_ha_data_internal+1536]
        vandpd    xmm1, xmm5, XMMWORD PTR [__svml_dexp_ha_data_internal+1728]
        vcmpnlepd xmm10, xmm1, XMMWORD PTR [__svml_dexp_ha_data_internal+1792]
        vsubpd    xmm4, xmm2, xmm3
        vmovmskpd r8d, xmm10
        vmovdqu   xmm3, XMMWORD PTR [__svml_dexp_ha_data_internal+1664]
        vfnmadd213pd xmm8, xmm4, xmm5
        vaddpd    xmm2, xmm4, XMMWORD PTR [__svml_dexp_ha_data_internal+1152]
        vfnmadd132pd xmm4, xmm8, XMMWORD PTR [__svml_dexp_ha_data_internal+1280]
        vpand     xmm0, xmm2, xmm3
        vmovd     eax, xmm0
        vfmadd213pd xmm9, xmm4, XMMWORD PTR [__svml_dexp_ha_data_internal+1472]
        vmulpd    xmm8, xmm4, xmm4
        vfmadd213pd xmm9, xmm4, XMMWORD PTR [__svml_dexp_ha_data_internal+1408]
        vfmadd213pd xmm9, xmm4, XMMWORD PTR [__svml_dexp_ha_data_internal+1344]
        vpextrd   edx, xmm0, 2
        movsxd    rax, eax
        movsxd    rdx, edx
        vfmadd213pd xmm9, xmm8, xmm4
        vpandn    xmm4, xmm3, xmm2
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rcx+rax]
        vmovupd   xmm10, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rcx+rdx]
        vunpcklpd xmm0, xmm1, xmm10
        vunpckhpd xmm1, xmm1, xmm10
        vfmadd213pd xmm9, xmm0, xmm1
        vpsllq    xmm1, xmm4, 42
        vaddpd    xmm0, xmm0, xmm9
        mov       QWORD PTR [248+rsp], r13
        vpaddq    xmm0, xmm0, xmm1
        test      r8d, r8d
        jne       _B6_3

_B6_2::

        vmovups   xmm8, XMMWORD PTR [224+rsp]
        vmovups   xmm9, XMMWORD PTR [208+rsp]
        vmovups   xmm10, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [240+rsp]
        add       rsp, 264
        ret

_B6_3::

        vcmpgtpd  xmm1, xmm5, XMMWORD PTR [__svml_dexp_ha_data_internal+1984]
        vcmpltpd  xmm2, xmm5, XMMWORD PTR [__svml_dexp_ha_data_internal+2048]
        vblendvpd xmm0, xmm0, XMMWORD PTR [_2il0floatpacket_33], xmm1
        vorpd     xmm3, xmm1, xmm2
        vmovmskpd eax, xmm3
        vandnpd   xmm0, xmm2, xmm0
        andn      edx, eax, r8d
        je        _B6_2

_B6_4::

        vmovupd   XMMWORD PTR [r13], xmm5
        vmovupd   XMMWORD PTR [64+r13], xmm0
        je        _B6_2

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
        jmp       _B6_2

_B6_11::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dexp_ha_cout_rare_internal
        jmp       _B6_9
        ALIGN     16

_B6_12::

__svml_exp2_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_exp2_ha_l9_B1_B4:
	DD	669953
	DD	2020409
	DD	952369
	DD	890920
	DD	829464
	DD	2162955

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_exp2_ha_l9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_exp2_ha_l9_B7_B11:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_exp2_ha_l9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_7
	DD	imagerel _B6_12
	DD	imagerel _unwind___svml_exp2_ha_l9_B7_B11

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_exp1_ha_ex

__svml_exp1_ha_ex	PROC	EXPORT

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L65::

        sub       rsp, 200
        movaps    xmm5, xmm0
        movups    XMMWORD PTR [176+rsp], xmm13
        movaps    xmm3, xmm5
        mov       QWORD PTR [168+rsp], r13
        mov       eax, 2147483647
        mulsd     xmm3, QWORD PTR [__svml_dexp_ha_data_internal+1024]
        mov       edx, 1082532650
        movsd     xmm2, QWORD PTR [__svml_dexp_ha_data_internal+1088]
        movd      xmm1, eax
        movd      xmm13, edx
        lea       r8, QWORD PTR [__ImageBase]
        movsd     xmm0, QWORD PTR [__svml_dexp_ha_data_internal+1536]
        lea       r13, QWORD PTR [95+rsp]
        and       r13, -64
        addsd     xmm3, xmm2
        movaps    xmm4, xmm3
        mov       QWORD PTR [192+rsp], r13
        subsd     xmm4, xmm2
        pshufd    xmm2, xmm5, 85
        pand      xmm2, xmm1
        pcmpgtd   xmm2, xmm13
        movmskps  eax, xmm2
        movq      xmm2, QWORD PTR [__svml_dexp_ha_data_internal+1600]
        movdqa    xmm1, xmm2
        pandn     xmm2, xmm3
        pand      xmm1, xmm3
        psllq     xmm2, 46
        pshufd    xmm13, xmm1, 0
        movaps    xmm1, xmm5
        pslld     xmm13, 4
        movd      ecx, xmm13
        movaps    xmm13, xmm4
        mulsd     xmm13, QWORD PTR [__svml_dexp_ha_data_internal+1216]
        mulsd     xmm4, QWORD PTR [__svml_dexp_ha_data_internal+1280]
        subsd     xmm1, xmm13
        movsxd    rcx, ecx
        subsd     xmm1, xmm4
        mulsd     xmm0, xmm1
        movaps    xmm4, xmm1
        mulsd     xmm4, xmm1
        addsd     xmm0, QWORD PTR [__svml_dexp_ha_data_internal+1472]
        mulsd     xmm0, xmm1
        addsd     xmm0, QWORD PTR [__svml_dexp_ha_data_internal+1408]
        mulsd     xmm0, xmm1
        addsd     xmm0, QWORD PTR [__svml_dexp_ha_data_internal+1344]
        mulsd     xmm0, xmm4
        addsd     xmm0, xmm1
        mulsd     xmm0, QWORD PTR [imagerel(__svml_dexp_ha_data_internal)+r8+rcx]
        addsd     xmm0, QWORD PTR [imagerel(__svml_dexp_ha_data_internal)+8+r8+rcx]
        addsd     xmm0, QWORD PTR [imagerel(__svml_dexp_ha_data_internal)+r8+rcx]
        paddq     xmm0, xmm2
        test      al, 1
        jne       _B7_3

_B7_2::

        movups    xmm13, XMMWORD PTR [176+rsp]
        mov       r13, QWORD PTR [168+rsp]
        add       rsp, 200
        ret

_B7_3::

        movsd     xmm1, QWORD PTR [__svml_dexp_ha_data_internal+1984]
        movaps    xmm3, xmm5
        movaps    xmm4, xmm5
        cmpltsd   xmm1, xmm5
        cmpltsd   xmm3, QWORD PTR [__svml_dexp_ha_data_internal+2048]
        movsd     xmm4, xmm1
        movaps    xmm2, xmm4
        andnps    xmm2, xmm0
        movups    xmm0, XMMWORD PTR [_2il0floatpacket_33]
        andps     xmm0, xmm4
        orps      xmm4, xmm3
        movmskpd  edx, xmm4
        orps      xmm2, xmm0
        movaps    xmm0, xmm3
        andnps    xmm0, xmm2
        not       edx
        and       edx, eax
        and       edx, 1
        je        _B7_2

_B7_4::

        movsd     QWORD PTR [r13], xmm5
        movsd     QWORD PTR [64+r13], xmm0
        jne       _B7_7

_B7_5::

        movsd     xmm0, QWORD PTR [64+r13]
        jmp       _B7_2

_B7_7::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dexp_ha_cout_rare_internal
        jmp       _B7_5
        ALIGN     16

_B7_8::

__svml_exp1_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_exp1_ha_ex_B1_B7:
	DD	401921
	DD	1430562
	DD	776215
	DD	1638667

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_8
	DD	imagerel _unwind___svml_exp1_ha_ex_B1_B7

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST7:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_exp4_ha_l9

__svml_exp4_ha_l9	PROC	EXPORT

_B8_1::

        DB        243
        DB        15
        DB        30
        DB        250
L70::

        sub       rsp, 552
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [368+rsp], ymm15
        vmovups   YMMWORD PTR [336+rsp], ymm10
        vmovups   YMMWORD PTR [400+rsp], ymm9
        vmovups   YMMWORD PTR [432+rsp], ymm8
        vmovups   YMMWORD PTR [464+rsp], ymm7
        vmovups   YMMWORD PTR [496+rsp], ymm6
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [239+rsp]
        vmovupd   ymm4, YMMWORD PTR [__svml_dexp_ha_data_internal+1024]
        and       r13, -64
        vmovupd   ymm5, YMMWORD PTR [__svml_dexp_ha_data_internal+1088]
        vmovupd   ymm9, YMMWORD PTR [__svml_dexp_ha_data_internal+1216]
        vmovupd   ymm10, YMMWORD PTR [__svml_dexp_ha_data_internal+1536]
        vmovdqa   ymm8, ymm0
        vfmadd213pd ymm4, ymm8, ymm5
        vsubpd    ymm7, ymm4, ymm5
        vmovupd   ymm5, YMMWORD PTR [__svml_dexp_ha_data_internal+1664]
        vfnmadd213pd ymm9, ymm7, ymm8
        vaddpd    ymm4, ymm7, YMMWORD PTR [__svml_dexp_ha_data_internal+1152]
        vfnmadd132pd ymm7, ymm9, YMMWORD PTR [__svml_dexp_ha_data_internal+1280]
        vfmadd213pd ymm10, ymm7, YMMWORD PTR [__svml_dexp_ha_data_internal+1472]
        vmulpd    ymm9, ymm7, ymm7
        vfmadd213pd ymm10, ymm7, YMMWORD PTR [__svml_dexp_ha_data_internal+1408]
        vandps    ymm15, ymm4, ymm5
        vfmadd213pd ymm10, ymm7, YMMWORD PTR [__svml_dexp_ha_data_internal+1344]
        vpandn    ymm4, ymm5, ymm4
        vfmadd213pd ymm10, ymm9, ymm7
        vpsllq    ymm5, ymm4, 42
        vandpd    ymm2, ymm8, YMMWORD PTR [__svml_dexp_ha_data_internal+1728]
        vcmpnle_uqpd ymm1, ymm2, YMMWORD PTR [__svml_dexp_ha_data_internal+1792]
        vmovmskpd edx, ymm1
        mov       QWORD PTR [536+rsp], r13
        test      edx, edx
        vextracti128 xmm0, ymm15, 1
        vmovd     ecx, xmm15
        vmovd     r9d, xmm0
        vpextrd   r8d, xmm15, 2
        vpextrd   r10d, xmm0, 2
        movsxd    rcx, ecx
        movsxd    r8, r8d
        movsxd    r9, r9d
        movsxd    r10, r10d
        vmovupd   xmm6, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rax+rcx]
        vmovupd   xmm3, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rax+r8]
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rax+r9]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rax+r10]
        vunpcklpd xmm0, xmm6, xmm3
        vunpckhpd xmm3, xmm6, xmm3
        vunpcklpd xmm15, xmm2, xmm1
        vunpckhpd xmm1, xmm2, xmm1
        vinsertf128 ymm0, ymm0, xmm15, 1
        vinsertf128 ymm2, ymm3, xmm1, 1
        vfmadd213pd ymm10, ymm0, ymm2
        vaddpd    ymm0, ymm0, ymm10
        vpaddq    ymm0, ymm0, ymm5
        jne       _B8_3

_B8_2::

        vmovups   ymm6, YMMWORD PTR [496+rsp]
        vmovups   ymm7, YMMWORD PTR [464+rsp]
        vmovups   ymm8, YMMWORD PTR [432+rsp]
        vmovups   ymm9, YMMWORD PTR [400+rsp]
        vmovups   ymm10, YMMWORD PTR [336+rsp]
        vmovups   ymm15, YMMWORD PTR [368+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B8_3::

        vcmpgt_oqpd ymm1, ymm8, YMMWORD PTR [__svml_dexp_ha_data_internal+1984]
        vcmplt_oqpd ymm2, ymm8, YMMWORD PTR [__svml_dexp_ha_data_internal+2048]
        vblendvpd ymm0, ymm0, YMMWORD PTR [_2il0floatpacket_34], ymm1
        vorpd     ymm3, ymm1, ymm2
        vmovmskpd eax, ymm3
        vandnpd   ymm0, ymm2, ymm0
        andn      edx, eax, edx
        je        _B8_2

_B8_4::

        vmovupd   YMMWORD PTR [r13], ymm8
        vmovupd   YMMWORD PTR [64+r13], ymm0
        je        _B8_2

_B8_7::

        xor       eax, eax
        vmovups   YMMWORD PTR [128+rsp], ymm11
        vmovups   YMMWORD PTR [96+rsp], ymm12
        vmovups   YMMWORD PTR [64+rsp], ymm13
        vmovups   YMMWORD PTR [32+rsp], ymm14
        mov       QWORD PTR [168+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [160+rsp], rsi
        mov       esi, edx

_B8_8::

        bt        esi, ebx
        jc        _B8_11

_B8_9::

        inc       ebx
        cmp       ebx, 4
        jl        _B8_8

_B8_10::

        vmovups   ymm11, YMMWORD PTR [128+rsp]
        vmovups   ymm12, YMMWORD PTR [96+rsp]
        vmovups   ymm13, YMMWORD PTR [64+rsp]
        vmovups   ymm14, YMMWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [168+rsp]
        mov       rsi, QWORD PTR [160+rsp]
        jmp       _B8_2

_B8_11::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dexp_ha_cout_rare_internal
        jmp       _B8_9
        ALIGN     16

_B8_12::

__svml_exp4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_exp4_ha_l9_B1_B4:
	DD	1069057
	DD	4379728
	DD	2058312
	DD	1931327
	DD	1804342
	DD	1677357
	DD	1419300
	DD	1570843
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_1
	DD	imagerel _B8_7
	DD	imagerel _unwind___svml_exp4_ha_l9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_exp4_ha_l9_B7_B11:
	DD	798497
	DD	1336367
	DD	1389605
	DD	190493
	DD	317463
	DD	444433
	DD	571403
	DD	imagerel _B8_1
	DD	imagerel _B8_7
	DD	imagerel _unwind___svml_exp4_ha_l9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_7
	DD	imagerel _B8_12
	DD	imagerel _unwind___svml_exp4_ha_l9_B7_B11

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST8:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_exp2_ha_e9

__svml_exp2_ha_e9	PROC	EXPORT

_B9_1::

        DB        243
        DB        15
        DB        30
        DB        250
L97::

        sub       rsp, 248
        lea       rcx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [192+rsp], xmm7
        vmovapd   xmm7, xmm0
        vmovups   XMMWORD PTR [208+rsp], xmm6
        mov       QWORD PTR [224+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmulpd    xmm4, xmm7, XMMWORD PTR [__svml_dexp_ha_data_internal+1024]
        vmovupd   xmm3, XMMWORD PTR [__svml_dexp_ha_data_internal+1088]
        and       r13, -64
        vaddpd    xmm6, xmm3, xmm4
        vmovq     xmm1, QWORD PTR [__svml_dexp_ha_data_internal+1856]
        vpshufd   xmm2, xmm7, 221
        vsubpd    xmm5, xmm6, xmm3
        vmovq     xmm3, QWORD PTR [__svml_dexp_ha_data_internal+1920]
        vpand     xmm4, xmm2, xmm1
        vpcmpgtd  xmm2, xmm4, xmm3
        vmovdqu   xmm4, XMMWORD PTR [__svml_dexp_ha_data_internal+1600]
        vpand     xmm1, xmm6, xmm4
        vpandn    xmm4, xmm4, xmm6
        vpshufd   xmm0, xmm1, 136
        vpsllq    xmm6, xmm4, 46
        vpslld    xmm3, xmm0, 4
        vmulpd    xmm0, xmm5, XMMWORD PTR [__svml_dexp_ha_data_internal+1216]
        vmovd     eax, xmm3
        vmovmskps r8d, xmm2
        vmulpd    xmm5, xmm5, XMMWORD PTR [__svml_dexp_ha_data_internal+1280]
        vpextrd   edx, xmm3, 1
        movsxd    rax, eax
        movsxd    rdx, edx
        mov       QWORD PTR [232+rsp], r13
        vmovupd   xmm2, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rcx+rax]
        vmovupd   xmm1, XMMWORD PTR [imagerel(__svml_dexp_ha_data_internal)+rcx+rdx]
        vunpcklpd xmm3, xmm2, xmm1
        vunpckhpd xmm2, xmm2, xmm1
        vsubpd    xmm1, xmm7, xmm0
        vsubpd    xmm1, xmm1, xmm5
        vmulpd    xmm0, xmm1, XMMWORD PTR [__svml_dexp_ha_data_internal+1536]
        vaddpd    xmm5, xmm0, XMMWORD PTR [__svml_dexp_ha_data_internal+1472]
        vmulpd    xmm0, xmm1, xmm5
        vaddpd    xmm5, xmm0, XMMWORD PTR [__svml_dexp_ha_data_internal+1408]
        vmulpd    xmm0, xmm1, xmm1
        vmulpd    xmm5, xmm1, xmm5
        vaddpd    xmm5, xmm5, XMMWORD PTR [__svml_dexp_ha_data_internal+1344]
        vmulpd    xmm0, xmm0, xmm5
        vaddpd    xmm1, xmm1, xmm0
        vmulpd    xmm0, xmm3, xmm1
        vaddpd    xmm2, xmm2, xmm0
        vaddpd    xmm3, xmm3, xmm2
        vpaddq    xmm0, xmm3, xmm6
        test      r8d, 3
        jne       _B9_3

_B9_2::

        vmovups   xmm6, XMMWORD PTR [208+rsp]
        vmovups   xmm7, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [224+rsp]
        add       rsp, 248
        ret

_B9_3::

        vcmpgtpd  xmm1, xmm7, XMMWORD PTR [__svml_dexp_ha_data_internal+1984]
        vcmpltpd  xmm2, xmm7, XMMWORD PTR [__svml_dexp_ha_data_internal+2048]
        vblendvpd xmm0, xmm0, XMMWORD PTR [_2il0floatpacket_33], xmm1
        vorpd     xmm3, xmm1, xmm2
        vmovmskpd edx, xmm3
        vandnpd   xmm0, xmm2, xmm0
        not       edx
        and       edx, r8d
        and       edx, 3
        je        _B9_2

_B9_4::

        vmovupd   XMMWORD PTR [r13], xmm7
        vmovupd   XMMWORD PTR [64+r13], xmm0
        je        _B9_2

_B9_7::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B9_8::

        bt        esi, ebx
        jc        _B9_11

_B9_9::

        inc       ebx
        cmp       ebx, 2
        jl        _B9_8

_B9_10::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B9_2

_B9_11::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dexp_ha_cout_rare_internal
        jmp       _B9_9
        ALIGN     16

_B9_12::

__svml_exp2_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_exp2_ha_e9_B1_B4:
	DD	536577
	DD	1889328
	DD	878632
	DD	817179
	DD	2031883

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_1
	DD	imagerel _B9_7
	DD	imagerel _unwind___svml_exp2_ha_e9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_exp2_ha_e9_B7_B11:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B9_1
	DD	imagerel _B9_7
	DD	imagerel _unwind___svml_exp2_ha_e9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_7
	DD	imagerel _B9_12
	DD	imagerel _unwind___svml_exp2_ha_e9_B7_B11

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST9:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_dexp_ha_cout_rare_internal

__svml_dexp_ha_cout_rare_internal	PROC

_B10_1::

        DB        243
        DB        15
        DB        30
        DB        250
L108::

        sub       rsp, 104
        mov       r9, rdx
        movzx     edx, WORD PTR [6+rcx]
        xor       eax, eax
        and       edx, 32752
        shr       edx, 4
        movsd     xmm1, QWORD PTR [rcx]
        movsd     QWORD PTR [96+rsp], xmm1
        cmp       edx, 2047
        je        _B10_17

_B10_2::

        cmp       edx, 970
        jle       _B10_15

_B10_3::

        movsd     xmm0, QWORD PTR [_imldExpHATab+1080]
        comisd    xmm0, xmm1
        jb        _B10_14

_B10_4::

        comisd    xmm1, QWORD PTR [_imldExpHATab+1096]
        jb        _B10_13

_B10_5::

        movsd     xmm0, QWORD PTR [_imldExpHATab+1024]
        lea       r11, QWORD PTR [__ImageBase]
        mulsd     xmm0, xmm1
        movsd     QWORD PTR [80+rsp], xmm0
        movsd     xmm2, QWORD PTR [80+rsp]
        movsd     xmm0, QWORD PTR [_imldExpHATab+1072]
        mov       rcx, QWORD PTR [_imldExpHATab+1136]
        mov       QWORD PTR [96+rsp], rcx
        addsd     xmm2, QWORD PTR [_imldExpHATab+1032]
        movsd     QWORD PTR [88+rsp], xmm2
        movaps    xmm2, xmm1
        movsd     xmm3, QWORD PTR [88+rsp]
        mov       edx, DWORD PTR [88+rsp]
        mov       r8d, edx
        and       edx, 63
        subsd     xmm3, QWORD PTR [_imldExpHATab+1032]
        movsd     QWORD PTR [80+rsp], xmm3
        lea       r10d, DWORD PTR [rdx+rdx]
        movsd     xmm4, QWORD PTR [80+rsp]
        lea       edx, DWORD PTR [1+rdx+rdx]
        mulsd     xmm4, QWORD PTR [_imldExpHATab+1104]
        movsd     xmm5, QWORD PTR [80+rsp]
        subsd     xmm2, xmm4
        mulsd     xmm5, QWORD PTR [_imldExpHATab+1112]
        shr       r8d, 6
        subsd     xmm2, xmm5
        comisd    xmm1, QWORD PTR [_imldExpHATab+1088]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [_imldExpHATab+1064]
        mulsd     xmm0, xmm2
        lea       ecx, DWORD PTR [1023+r8]
        addsd     xmm0, QWORD PTR [_imldExpHATab+1056]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [_imldExpHATab+1048]
        mulsd     xmm0, xmm2
        addsd     xmm0, QWORD PTR [_imldExpHATab+1040]
        mulsd     xmm0, xmm2
        mulsd     xmm0, xmm2
        addsd     xmm0, xmm2
        movsd     xmm2, QWORD PTR [imagerel(_imldExpHATab)+r11+r10*8]
        addsd     xmm0, QWORD PTR [imagerel(_imldExpHATab)+r11+rdx*8]
        mulsd     xmm0, xmm2
        jb        _B10_9

_B10_6::

        and       ecx, 2047
        addsd     xmm0, xmm2
        cmp       ecx, 2046
        ja        _B10_8

_B10_7::

        mov       rdx, QWORD PTR [_imldExpHATab+1136]
        shr       rdx, 48
        shl       ecx, 4
        and       edx, -32753
        or        edx, ecx
        mov       WORD PTR [102+rsp], dx
        movsd     xmm1, QWORD PTR [96+rsp]
        mulsd     xmm0, xmm1
        movsd     QWORD PTR [r9], xmm0
        add       rsp, 104
        ret

_B10_8::

        dec       ecx
        and       ecx, 2047
        movzx     edx, WORD PTR [102+rsp]
        shl       ecx, 4
        and       edx, -32753
        or        edx, ecx
        mov       WORD PTR [102+rsp], dx
        movsd     xmm1, QWORD PTR [96+rsp]
        mulsd     xmm0, xmm1
        mulsd     xmm0, QWORD PTR [_imldExpHATab+1152]
        movsd     QWORD PTR [r9], xmm0
        add       rsp, 104
        ret

_B10_9::

        add       r8d, 1083
        and       r8d, 2047
        mov       eax, r8d
        movzx     edx, WORD PTR [102+rsp]
        shl       eax, 4
        and       edx, -32753
        or        edx, eax
        mov       WORD PTR [102+rsp], dx
        movsd     xmm3, QWORD PTR [96+rsp]
        mulsd     xmm0, xmm3
        mulsd     xmm3, xmm2
        movaps    xmm1, xmm3
        addsd     xmm1, xmm0
        cmp       r8d, 50
        ja        _B10_11

_B10_10::

        mulsd     xmm1, QWORD PTR [_imldExpHATab+1160]
        movsd     QWORD PTR [r9], xmm1
        jmp       _B10_12

_B10_11::

        movsd     QWORD PTR [32+rsp], xmm1
        movsd     xmm1, QWORD PTR [32+rsp]
        subsd     xmm3, xmm1
        movsd     QWORD PTR [40+rsp], xmm3
        movsd     xmm2, QWORD PTR [40+rsp]
        addsd     xmm2, xmm0
        movsd     QWORD PTR [40+rsp], xmm2
        movsd     xmm0, QWORD PTR [32+rsp]
        mulsd     xmm0, QWORD PTR [_imldExpHATab+1168]
        movsd     QWORD PTR [48+rsp], xmm0
        movsd     xmm4, QWORD PTR [32+rsp]
        movsd     xmm3, QWORD PTR [48+rsp]
        addsd     xmm4, xmm3
        movsd     QWORD PTR [56+rsp], xmm4
        movsd     xmm0, QWORD PTR [56+rsp]
        movsd     xmm5, QWORD PTR [48+rsp]
        subsd     xmm0, xmm5
        movsd     QWORD PTR [64+rsp], xmm0
        movsd     xmm2, QWORD PTR [32+rsp]
        movsd     xmm1, QWORD PTR [64+rsp]
        subsd     xmm2, xmm1
        movsd     QWORD PTR [72+rsp], xmm2
        movsd     xmm4, QWORD PTR [40+rsp]
        movsd     xmm3, QWORD PTR [72+rsp]
        addsd     xmm4, xmm3
        movsd     QWORD PTR [72+rsp], xmm4
        movsd     xmm0, QWORD PTR [64+rsp]
        mulsd     xmm0, QWORD PTR [_imldExpHATab+1160]
        movsd     QWORD PTR [64+rsp], xmm0
        movsd     xmm1, QWORD PTR [72+rsp]
        mulsd     xmm1, QWORD PTR [_imldExpHATab+1160]
        movsd     QWORD PTR [72+rsp], xmm1
        movsd     xmm2, QWORD PTR [64+rsp]
        movsd     xmm5, QWORD PTR [72+rsp]
        addsd     xmm2, xmm5
        movsd     QWORD PTR [r9], xmm2

_B10_12::

        mov       eax, 4
        add       rsp, 104
        ret

_B10_13::

        movsd     xmm0, QWORD PTR [_imldExpHATab+1120]
        mov       eax, 4
        mulsd     xmm0, xmm0
        movsd     QWORD PTR [r9], xmm0
        add       rsp, 104
        ret

_B10_14::

        movsd     xmm0, QWORD PTR [_imldExpHATab+1128]
        mov       eax, 3
        mulsd     xmm0, xmm0
        movsd     QWORD PTR [r9], xmm0
        add       rsp, 104
        ret

_B10_15::

        movsd     xmm0, QWORD PTR [_imldExpHATab+1144]
        addsd     xmm0, xmm1
        movsd     QWORD PTR [r9], xmm0

_B10_16::

        add       rsp, 104
        ret

_B10_17::

        mov       dl, BYTE PTR [103+rsp]
        and       dl, -128
        cmp       dl, -128
        je        _B10_19

_B10_18::

        mulsd     xmm1, xmm1
        movsd     QWORD PTR [r9], xmm1
        add       rsp, 104
        ret

_B10_19::

        test      DWORD PTR [100+rsp], 1048575
        jne       _B10_18

_B10_20::

        cmp       DWORD PTR [96+rsp], 0
        jne       _B10_18

_B10_21::

        mov       rdx, QWORD PTR [_imldExpHATab+1136]
        mov       QWORD PTR [r9], rdx
        add       rsp, 104
        ret
        ALIGN     16

_B10_22::

__svml_dexp_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_dexp_ha_cout_rare_internal_B1_B21:
	DD	67585
	DD	49672

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B10_1
	DD	imagerel _B10_22
	DD	imagerel _unwind___svml_dexp_ha_cout_rare_internal_B1_B21

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_dexp_ha_data_internal_avx512
__svml_dexp_ha_data_internal_avx512	DD	0
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
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
	DD	1697350398
	DD	1073157447
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
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
	DD	993624127
	DD	1014676638
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
	DD	915672068
	DD	1062682904
	DD	915672068
	DD	1062682904
	DD	915672068
	DD	1062682904
	DD	915672068
	DD	1062682904
	DD	915672068
	DD	1062682904
	DD	915672068
	DD	1062682904
	DD	915672068
	DD	1062682904
	DD	915672068
	DD	1062682904
	DD	3150079424
	DD	1065422876
	DD	3150079424
	DD	1065422876
	DD	3150079424
	DD	1065422876
	DD	3150079424
	DD	1065422876
	DD	3150079424
	DD	1065422876
	DD	3150079424
	DD	1065422876
	DD	3150079424
	DD	1065422876
	DD	3150079424
	DD	1065422876
	DD	606955774
	DD	1067799895
	DD	606955774
	DD	1067799895
	DD	606955774
	DD	1067799895
	DD	606955774
	DD	1067799895
	DD	606955774
	DD	1067799895
	DD	606955774
	DD	1067799895
	DD	606955774
	DD	1067799895
	DD	606955774
	DD	1067799895
	DD	1402181426
	DD	1069897045
	DD	1402181426
	DD	1069897045
	DD	1402181426
	DD	1069897045
	DD	1402181426
	DD	1069897045
	DD	1402181426
	DD	1069897045
	DD	1402181426
	DD	1069897045
	DD	1402181426
	DD	1069897045
	DD	1402181426
	DD	1069897045
	DD	53256
	DD	1071644672
	DD	53256
	DD	1071644672
	DD	53256
	DD	1071644672
	DD	53256
	DD	1071644672
	DD	53256
	DD	1071644672
	DD	53256
	DD	1071644672
	DD	53256
	DD	1071644672
	DD	53256
	DD	1071644672
	DD	4294967152
	DD	1072693247
	DD	4294967152
	DD	1072693247
	DD	4294967152
	DD	1072693247
	DD	4294967152
	DD	1072693247
	DD	4294967152
	DD	1072693247
	DD	4294967152
	DD	1072693247
	DD	4294967152
	DD	1072693247
	DD	4294967152
	DD	1072693247
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
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	1287323204
	DD	1082531232
	DD	0
	DD	1005584384
	DD	0
	DD	1005584384
	DD	0
	DD	1005584384
	DD	0
	DD	1005584384
	DD	0
	DD	1005584384
	DD	0
	DD	1005584384
	DD	0
	DD	1005584384
	DD	0
	DD	1005584384
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
	DD	32752
	DD	0
	DD	32752
	DD	0
	DD	32752
	DD	0
	DD	32752
	DD	0
	DD	32752
	DD	0
	DD	32752
	DD	0
	DD	32752
	DD	0
	DD	32752
	DD	0
	PUBLIC __svml_dexp_ha_data_internal
__svml_dexp_ha_data_internal	DD	0
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
	DD	1697350398
	DD	1079448903
	DD	1697350398
	DD	1079448903
	DD	1697350398
	DD	1079448903
	DD	1697350398
	DD	1079448903
	DD	1697350398
	DD	1079448903
	DD	1697350398
	DD	1079448903
	DD	1697350398
	DD	1079448903
	DD	1697350398
	DD	1079448903
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
	DD	1123549184
	DD	0
	DD	1123549184
	DD	0
	DD	1123549184
	DD	0
	DD	1123549184
	DD	0
	DD	1123549184
	DD	0
	DD	1123549184
	DD	0
	DD	1123549184
	DD	0
	DD	1123549184
	DD	4277796864
	DD	1065758274
	DD	4277796864
	DD	1065758274
	DD	4277796864
	DD	1065758274
	DD	4277796864
	DD	1065758274
	DD	4277796864
	DD	1065758274
	DD	4277796864
	DD	1065758274
	DD	4277796864
	DD	1065758274
	DD	4277796864
	DD	1065758274
	DD	3164486458
	DD	1025308570
	DD	3164486458
	DD	1025308570
	DD	3164486458
	DD	1025308570
	DD	3164486458
	DD	1025308570
	DD	3164486458
	DD	1025308570
	DD	3164486458
	DD	1025308570
	DD	3164486458
	DD	1025308570
	DD	3164486458
	DD	1025308570
	DD	4294957883
	DD	1071644671
	DD	4294957883
	DD	1071644671
	DD	4294957883
	DD	1071644671
	DD	4294957883
	DD	1071644671
	DD	4294957883
	DD	1071644671
	DD	4294957883
	DD	1071644671
	DD	4294957883
	DD	1071644671
	DD	4294957883
	DD	1071644671
	DD	1431659838
	DD	1069897045
	DD	1431659838
	DD	1069897045
	DD	1431659838
	DD	1069897045
	DD	1431659838
	DD	1069897045
	DD	1431659838
	DD	1069897045
	DD	1431659838
	DD	1069897045
	DD	1431659838
	DD	1069897045
	DD	1431659838
	DD	1069897045
	DD	1059163027
	DD	1067799895
	DD	1059163027
	DD	1067799895
	DD	1059163027
	DD	1067799895
	DD	1059163027
	DD	1067799895
	DD	1059163027
	DD	1067799895
	DD	1059163027
	DD	1067799895
	DD	1059163027
	DD	1067799895
	DD	1059163027
	DD	1067799895
	DD	765416603
	DD	1065423121
	DD	765416603
	DD	1065423121
	DD	765416603
	DD	1065423121
	DD	765416603
	DD	1065423121
	DD	765416603
	DD	1065423121
	DD	765416603
	DD	1065423121
	DD	765416603
	DD	1065423121
	DD	765416603
	DD	1065423121
	DD	63
	DD	0
	DD	63
	DD	0
	DD	63
	DD	0
	DD	63
	DD	0
	DD	63
	DD	0
	DD	63
	DD	0
	DD	63
	DD	0
	DD	63
	DD	0
	DD	1023
	DD	0
	DD	1023
	DD	0
	DD	1023
	DD	0
	DD	1023
	DD	0
	DD	1023
	DD	0
	DD	1023
	DD	0
	DD	1023
	DD	0
	DD	1023
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
	DD	4294967295
	DD	1082532650
	DD	4294967295
	DD	1082532650
	DD	4294967295
	DD	1082532650
	DD	4294967295
	DD	1082532650
	DD	4294967295
	DD	1082532650
	DD	4294967295
	DD	1082532650
	DD	4294967295
	DD	1082532650
	DD	4294967295
	DD	1082532650
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
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	1082532650
	DD	4277811695
	DD	1082535490
	DD	4277811695
	DD	1082535490
	DD	4277811695
	DD	1082535490
	DD	4277811695
	DD	1082535490
	DD	4277811695
	DD	1082535490
	DD	4277811695
	DD	1082535490
	DD	4277811695
	DD	1082535490
	DD	4277811695
	DD	1082535490
	DD	3576508497
	DD	3230091536
	DD	3576508497
	DD	3230091536
	DD	3576508497
	DD	3230091536
	DD	3576508497
	DD	3230091536
	DD	3576508497
	DD	3230091536
	DD	3576508497
	DD	3230091536
	DD	3576508497
	DD	3230091536
	DD	3576508497
	DD	3230091536
_2il0floatpacket_34	DD	000000000H,07ff00000H,000000000H,07ff00000H,000000000H,07ff00000H,000000000H,07ff00000H
_imldExpHATab	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	1048019041
	DD	1072704666
	DD	2631457885
	DD	3161546771
	DD	3541402996
	DD	1072716208
	DD	896005651
	DD	1015861842
	DD	410360776
	DD	1072727877
	DD	1642514529
	DD	1012987726
	DD	1828292879
	DD	1072739672
	DD	1568897901
	DD	1016568486
	DD	852742562
	DD	1072751596
	DD	1882168529
	DD	1010744893
	DD	3490863953
	DD	1072763649
	DD	707771662
	DD	3163903570
	DD	2930322912
	DD	1072775834
	DD	3117806614
	DD	3163670819
	DD	1014845819
	DD	1072788152
	DD	3936719688
	DD	3162512149
	DD	3949972341
	DD	1072800603
	DD	1058231231
	DD	1015777676
	DD	828946858
	DD	1072813191
	DD	1044000608
	DD	1016786167
	DD	2288159958
	DD	1072825915
	DD	1151779725
	DD	1015705409
	DD	1853186616
	DD	1072838778
	DD	3819481236
	DD	1016499965
	DD	1709341917
	DD	1072851781
	DD	2552227826
	DD	1015039787
	DD	4112506593
	DD	1072864925
	DD	1829350193
	DD	1015216097
	DD	2799960843
	DD	1072878213
	DD	1913391796
	DD	1015756674
	DD	171030293
	DD	1072891646
	DD	1303423926
	DD	1015238005
	DD	2992903935
	DD	1072905224
	DD	1574172746
	DD	1016061241
	DD	926591435
	DD	1072918951
	DD	3427487848
	DD	3163704045
	DD	887463927
	DD	1072932827
	DD	1049900754
	DD	3161575912
	DD	1276261410
	DD	1072946854
	DD	2804567149
	DD	1015390024
	DD	569847338
	DD	1072961034
	DD	1209502043
	DD	3159926671
	DD	1617004845
	DD	1072975368
	DD	1623370769
	DD	1011049453
	DD	3049340112
	DD	1072989858
	DD	3667985273
	DD	1013894369
	DD	3577096743
	DD	1073004506
	DD	3145379760
	DD	1014403278
	DD	1990012071
	DD	1073019314
	DD	7447438
	DD	3163526196
	DD	1453150082
	DD	1073034283
	DD	3171891295
	DD	3162037958
	DD	917841882
	DD	1073049415
	DD	419288974
	DD	1016280325
	DD	3712504873
	DD	1073064711
	DD	3793507337
	DD	1016095713
	DD	363667784
	DD	1073080175
	DD	728023093
	DD	1016345318
	DD	2956612997
	DD	1073095806
	DD	1005538728
	DD	3163304901
	DD	2186617381
	DD	1073111608
	DD	2018924632
	DD	3163803357
	DD	1719614413
	DD	1073127582
	DD	3210617384
	DD	3163796463
	DD	1013258799
	DD	1073143730
	DD	3094194670
	DD	3160631279
	DD	3907805044
	DD	1073160053
	DD	2119843535
	DD	3161988964
	DD	1447192521
	DD	1073176555
	DD	508946058
	DD	3162904882
	DD	1944781191
	DD	1073193236
	DD	3108873501
	DD	3162190556
	DD	919555682
	DD	1073210099
	DD	2882956373
	DD	1013312481
	DD	2571947539
	DD	1073227145
	DD	4047189812
	DD	3163777462
	DD	2604962541
	DD	1073244377
	DD	3631372142
	DD	3163870288
	DD	1110089947
	DD	1073261797
	DD	3253791412
	DD	1015920431
	DD	2568320822
	DD	1073279406
	DD	1509121860
	DD	1014756995
	DD	2966275557
	DD	1073297207
	DD	2339118633
	DD	3160254904
	DD	2682146384
	DD	1073315202
	DD	586480042
	DD	3163702083
	DD	2191782032
	DD	1073333393
	DD	730975783
	DD	1014083580
	DD	2069751141
	DD	1073351782
	DD	576856675
	DD	3163014404
	DD	2990417245
	DD	1073370371
	DD	3552361237
	DD	3163667409
	DD	1434058175
	DD	1073389163
	DD	1853053619
	DD	1015310724
	DD	2572866477
	DD	1073408159
	DD	2462790535
	DD	1015814775
	DD	3092190715
	DD	1073427362
	DD	1457303226
	DD	3159737305
	DD	4076559943
	DD	1073446774
	DD	950899508
	DD	3160987380
	DD	2420883922
	DD	1073466398
	DD	174054861
	DD	1014300631
	DD	3716502172
	DD	1073486235
	DD	816778419
	DD	1014197934
	DD	777507147
	DD	1073506289
	DD	3507050924
	DD	1015341199
	DD	3706687593
	DD	1073526560
	DD	1821514088
	DD	1013410604
	DD	1242007932
	DD	1073547053
	DD	1073740399
	DD	3163532637
	DD	3707479175
	DD	1073567768
	DD	2789017511
	DD	1014276997
	DD	64696965
	DD	1073588710
	DD	3586233004
	DD	1015962192
	DD	863738719
	DD	1073609879
	DD	129252895
	DD	3162690849
	DD	3884662774
	DD	1073631278
	DD	1614448851
	DD	1014281732
	DD	2728693978
	DD	1073652911
	DD	2413007344
	DD	3163551506
	DD	3999357479
	DD	1073674779
	DD	1101668360
	DD	1015989180
	DD	1533953344
	DD	1073696886
	DD	835814894
	DD	1015702697
	DD	2174652632
	DD	1073719233
	DD	1301400989
	DD	1014466875
	DD	1697350398
	DD	1079448903
	DD	0
	DD	1127743488
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
	DD	4277811695
	DD	1082535490
	DD	3715808466
	DD	3230016299
	DD	3576508497
	DD	3230091536
	DD	4277796864
	DD	1065758274
	DD	3164486458
	DD	1025308570
	DD	1
	DD	1048576
	DD	4294967295
	DD	2146435071
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	1073741824
	DD	0
	DD	1009778688
	DD	0
	DD	1106771968
	DD 2 DUP (0H)	
_2il0floatpacket_33	DD	000000000H,07ff00000H,000000000H,07ff00000H
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
