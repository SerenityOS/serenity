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
	PUBLIC __svml_tan1_ha_ex

__svml_tan1_ha_ex	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 392
        xor       edx, edx
        movups    XMMWORD PTR [240+rsp], xmm15
        movups    XMMWORD PTR [256+rsp], xmm14
        movups    XMMWORD PTR [272+rsp], xmm13
        movups    XMMWORD PTR [288+rsp], xmm12
        movups    XMMWORD PTR [304+rsp], xmm11
        movups    XMMWORD PTR [320+rsp], xmm10
        movups    XMMWORD PTR [336+rsp], xmm9
        movups    XMMWORD PTR [224+rsp], xmm8
        movups    XMMWORD PTR [192+rsp], xmm7
        movups    XMMWORD PTR [208+rsp], xmm6
        mov       QWORD PTR [184+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movsd     xmm9, QWORD PTR [__svml_dtan_ha_data_internal+1024]
        and       r13, -64
        movaps    xmm12, xmm9
        andnps    xmm9, xmm0
        andps     xmm12, xmm0
        movaps    xmm6, xmm12
        movaps    xmm2, xmm12
        mulsd     xmm6, QWORD PTR [__svml_dtan_ha_data_internal+1472]
        movaps    xmm1, xmm12
        cmpnlesd  xmm1, QWORD PTR [__svml_dtan_ha_data_internal+28160]
        movsd     xmm4, QWORD PTR [__svml_dtan_ha_data_internal+1152]
        movups    XMMWORD PTR [352+rsp], xmm0
        addsd     xmm6, xmm4
        movmskpd  r8d, xmm1
        movaps    xmm0, xmm6
        movups    xmm15, XMMWORD PTR [_2il0floatpacket_27]
        subsd     xmm0, xmm4
        movaps    xmm3, xmm0
        movaps    xmm5, xmm0
        mulsd     xmm3, QWORD PTR [__svml_dtan_ha_data_internal+1216]
        mulsd     xmm5, QWORD PTR [__svml_dtan_ha_data_internal+1280]
        mulsd     xmm0, QWORD PTR [__svml_dtan_ha_data_internal+1344]
        subsd     xmm2, xmm3
        movaps    xmm8, xmm2
        movq      xmm3, QWORD PTR [__svml_dtan_ha_data_internal+1408]
        subsd     xmm8, xmm5
        pand      xmm6, xmm3
        movaps    xmm4, xmm8
        pand      xmm6, XMMWORD PTR [_2il0floatpacket_26]
        subsd     xmm2, xmm8
        movd      eax, xmm6
        movups    XMMWORD PTR [368+rsp], xmm9
        subsd     xmm2, xmm5
        imul      ecx, eax, 104
        subsd     xmm2, xmm0
        movaps    xmm0, xmm15
        lea       rax, QWORD PTR [__ImageBase]
        addsd     xmm4, xmm2
        movq      xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rax+rcx]
        subsd     xmm8, xmm4
        movdqa    xmm14, xmm10
        addsd     xmm2, xmm8
        subsd     xmm14, xmm4
        andps     xmm0, xmm14
        subsd     xmm10, xmm14
        cvtpd2ps  xmm13, xmm0
        subsd     xmm10, xmm4
        subsd     xmm14, xmm0
        subsd     xmm10, xmm2
        addsd     xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rax+rcx]
        movlhps   xmm13, xmm13
        addsd     xmm10, xmm14
        rcpps     xmm11, xmm13
        cvtps2pd  xmm11, xmm11
        andps     xmm11, xmm15
        mulsd     xmm0, xmm11
        movups    xmm13, XMMWORD PTR [_2il0floatpacket_28]
        movaps    xmm14, xmm13
        movq      xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rax+rcx]
        subsd     xmm14, xmm0
        movaps    xmm0, xmm11
        movaps    xmm15, xmm14
        mulsd     xmm0, xmm14
        mulsd     xmm15, xmm14
        addsd     xmm0, xmm11
        mulsd     xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+rcx]
        addsd     xmm15, xmm13
        mulsd     xmm0, xmm15
        mulsd     xmm10, xmm0
        movaps    xmm13, xmm10
        mulsd     xmm10, xmm10
        subsd     xmm13, xmm14
        movdqa    xmm14, xmm8
        subsd     xmm10, xmm13
        mulsd     xmm14, xmm4
        addsd     xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+rcx]
        mulsd     xmm10, xmm0
        mulsd     xmm8, xmm2
        mulsd     xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+rcx]
        movq      xmm5, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rax+rcx]
        movdqa    xmm13, xmm5
        movq      xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rax+rcx]
        addsd     xmm13, xmm14
        mulsd     xmm6, xmm4
        subsd     xmm5, xmm13
        addsd     xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rax+rcx]
        addsd     xmm5, xmm14
        movaps    xmm0, xmm13
        addsd     xmm5, xmm10
        addsd     xmm0, xmm11
        movq      xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rax+rcx]
        movaps    xmm10, xmm4
        mulsd     xmm7, xmm4
        subsd     xmm11, xmm0
        mulsd     xmm10, xmm4
        addsd     xmm11, xmm13
        addsd     xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rax+rcx]
        mulsd     xmm6, xmm10
        addsd     xmm5, xmm11
        movq      xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rax+rcx]
        addsd     xmm8, xmm5
        mulsd     xmm3, xmm4
        movq      xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rax+rcx]
        mulsd     xmm11, xmm10
        addsd     xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+rcx]
        mulsd     xmm10, xmm10
        addsd     xmm11, xmm7
        addsd     xmm6, xmm3
        mulsd     xmm11, xmm10
        movq      xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rax+rcx]
        addsd     xmm11, xmm6
        addsd     xmm2, xmm8
        mulsd     xmm11, xmm4
        mov       QWORD PTR [384+rsp], r13
        addsd     xmm11, xmm2
        addsd     xmm0, xmm11
        pxor      xmm0, xmm9
        test      r8d, 1
        jne       _B1_8

_B1_2::

        test      edx, edx
        jne       _B1_4

_B1_3::

        movups    xmm6, XMMWORD PTR [208+rsp]
        movups    xmm7, XMMWORD PTR [192+rsp]
        movups    xmm8, XMMWORD PTR [224+rsp]
        movups    xmm9, XMMWORD PTR [336+rsp]
        movups    xmm10, XMMWORD PTR [320+rsp]
        movups    xmm11, XMMWORD PTR [304+rsp]
        movups    xmm12, XMMWORD PTR [288+rsp]
        movups    xmm13, XMMWORD PTR [272+rsp]
        movups    xmm14, XMMWORD PTR [256+rsp]
        movups    xmm15, XMMWORD PTR [240+rsp]
        mov       r13, QWORD PTR [184+rsp]
        add       rsp, 392
        ret

_B1_4::

        movups    xmm1, XMMWORD PTR [352+rsp]
        movsd     QWORD PTR [r13], xmm1
        movsd     QWORD PTR [64+r13], xmm0
        jne       _B1_6

_B1_5::

        movsd     xmm0, QWORD PTR [64+r13]
        jmp       _B1_3

_B1_6::

        je        _B1_5

_B1_7::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dtan_ha_cout_rare_internal
        jmp       _B1_5

_B1_8::

        movdqu    xmm6, XMMWORD PTR [_2il0floatpacket_29]
        movaps    xmm13, xmm12
        pand      xmm6, xmm12
        psrlq     xmm6, 52
        movd      ecx, xmm6
        movsd     xmm15, QWORD PTR [__svml_dtan_ha_data_internal+1088]
        movdqu    xmm2, XMMWORD PTR [_2il0floatpacket_30]
        andps     xmm13, xmm15
        pand      xmm2, xmm12
        cmpeqsd   xmm13, xmm15
        paddq     xmm2, XMMWORD PTR [_2il0floatpacket_31]
        movmskpd  edx, xmm13
        movdqa    xmm3, xmm2
        movups    XMMWORD PTR [32+rsp], xmm0
        psrlq     xmm3, 32
        movdqu    xmm10, XMMWORD PTR [_2il0floatpacket_32]
        lea       r8d, DWORD PTR [rcx+rcx*2]
        shl       r8d, 3
        pand      xmm2, xmm10
        movq      xmm9, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rax+r8]
        movdqa    xmm13, xmm3
        movdqa    xmm0, xmm9
        movdqa    xmm5, xmm2
        psrlq     xmm0, 32
        pand      xmm9, xmm10
        movq      xmm7, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rax+r8]
        movdqa    xmm6, xmm2
        pmuludq   xmm13, xmm0
        movdqa    xmm8, xmm7
        pmuludq   xmm0, xmm2
        pmuludq   xmm9, xmm3
        pand      xmm7, xmm10
        psrlq     xmm0, 32
        pmuludq   xmm5, xmm7
        psrlq     xmm8, 32
        pmuludq   xmm7, xmm3
        pmuludq   xmm6, xmm8
        pmuludq   xmm8, xmm3
        paddq     xmm13, xmm0
        movdqa    xmm15, xmm10
        movdqa    xmm14, xmm10
        pand      xmm15, xmm5
        psrlq     xmm9, 32
        paddq     xmm15, xmm13
        movq      xmm11, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rax+r8]
        psrlq     xmm5, 32
        pand      xmm14, xmm11
        movdqa    xmm4, xmm2
        paddq     xmm9, xmm15
        paddq     xmm7, xmm5
        pmuludq   xmm4, xmm14
        pmuludq   xmm3, xmm14
        movdqa    xmm13, xmm10
        movdqa    xmm0, xmm9
        pand      xmm13, xmm6
        psrlq     xmm0, 32
        paddq     xmm13, xmm7
        psrlq     xmm6, 32
        psrlq     xmm11, 32
        paddq     xmm0, xmm13
        paddq     xmm8, xmm6
        pmuludq   xmm2, xmm11
        movdqa    xmm5, xmm10
        movdqa    xmm7, xmm0
        pand      xmm5, xmm4
        psrlq     xmm7, 32
        paddq     xmm5, xmm8
        psrlq     xmm4, 32
        pand      xmm2, xmm10
        paddq     xmm7, xmm5
        paddq     xmm3, xmm4
        movdqa    xmm13, xmm7
        pand      xmm7, xmm10
        paddq     xmm2, xmm3
        psrlq     xmm13, 32
        psllq     xmm0, 32
        paddq     xmm13, xmm2
        psllq     xmm13, 32
        pand      xmm9, xmm10
        paddq     xmm13, xmm7
        paddq     xmm0, xmm9
        movdqu    xmm3, XMMWORD PTR [_2il0floatpacket_33]
        movdqa    xmm5, xmm13
        movdqu    xmm8, XMMWORD PTR [_2il0floatpacket_34]
        pand      xmm3, xmm12
        movups    xmm14, XMMWORD PTR [_2il0floatpacket_35]
        psrlq     xmm5, 12
        pxor      xmm8, xmm3
        movaps    xmm4, xmm14
        por       xmm5, xmm8
        and       edx, 1
        movdqu    xmm11, XMMWORD PTR [_2il0floatpacket_39]
        addsd     xmm4, xmm5
        movaps    xmm9, xmm4
        pand      xmm11, xmm13
        movdqu    xmm6, XMMWORD PTR [_2il0floatpacket_37]
        psllq     xmm11, 40
        pand      xmm6, xmm0
        psrlq     xmm0, 24
        por       xmm11, xmm0
        psllq     xmm6, 28
        movups    xmm2, XMMWORD PTR [_2il0floatpacket_27]
        subsd     xmm9, xmm14
        movdqu    xmm14, XMMWORD PTR [_2il0floatpacket_36]
        movaps    xmm13, xmm2
        pxor      xmm14, xmm3
        subsd     xmm5, xmm9
        pxor      xmm3, XMMWORD PTR [_2il0floatpacket_38]
        movaps    xmm7, xmm5
        por       xmm11, xmm3
        por       xmm6, xmm14
        movups    xmm10, XMMWORD PTR [_2il0floatpacket_41]
        subsd     xmm11, xmm3
        subsd     xmm6, xmm14
        addsd     xmm7, xmm11
        andps     xmm13, xmm7
        movaps    xmm3, xmm10
        movups    xmm14, XMMWORD PTR [_2il0floatpacket_42]
        subsd     xmm5, xmm7
        mulsd     xmm3, xmm13
        subsd     xmm7, xmm13
        addsd     xmm11, xmm5
        mulsd     xmm10, xmm7
        addsd     xmm11, xmm6
        movups    xmm15, XMMWORD PTR [_2il0floatpacket_40]
        movaps    xmm8, xmm14
        mulsd     xmm8, xmm13
        mulsd     xmm15, xmm11
        mulsd     xmm14, xmm7
        addsd     xmm10, xmm8
        addsd     xmm15, xmm14
        movaps    xmm14, xmm3
        addsd     xmm10, xmm15
        pand      xmm4, XMMWORD PTR [_2il0floatpacket_45]
        addsd     xmm14, xmm10
        pand      xmm4, XMMWORD PTR [_2il0floatpacket_26]
        subsd     xmm3, xmm14
        movd      r9d, xmm4
        movups    xmm5, XMMWORD PTR [_2il0floatpacket_44]
        addsd     xmm3, xmm10
        imul      r10d, r9d, 104
        movups    xmm13, XMMWORD PTR [_2il0floatpacket_43]
        movaps    xmm0, xmm5
        andps     xmm13, xmm12
        movaps    xmm11, xmm13
        cmpltsd   xmm0, xmm13
        cmplesd   xmm13, xmm5
        movsd     xmm11, xmm0
        andps     xmm13, xmm12
        movq      xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rax+r10]
        andps     xmm14, xmm11
        orps      xmm13, xmm14
        movdqa    xmm12, xmm10
        movaps    xmm0, xmm2
        andps     xmm11, xmm3
        movq      xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rax+r10]
        subsd     xmm12, xmm13
        andps     xmm0, xmm12
        subsd     xmm10, xmm12
        subsd     xmm12, xmm0
        cvtpd2ps  xmm14, xmm0
        subsd     xmm10, xmm13
        addsd     xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rax+r10]
        subsd     xmm10, xmm11
        movlhps   xmm14, xmm14
        addsd     xmm10, xmm12
        rcpps     xmm12, xmm14
        cvtps2pd  xmm12, xmm12
        andps     xmm12, xmm2
        mulsd     xmm0, xmm12
        movaps    xmm15, xmm12
        movups    xmm14, XMMWORD PTR [_2il0floatpacket_28]
        movaps    xmm2, xmm14
        movq      xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rax+r10]
        subsd     xmm2, xmm0
        movaps    xmm0, xmm2
        mulsd     xmm15, xmm2
        mulsd     xmm0, xmm2
        addsd     xmm15, xmm12
        mulsd     xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+r10]
        addsd     xmm0, xmm14
        mulsd     xmm15, xmm0
        movdqa    xmm0, xmm6
        mulsd     xmm10, xmm15
        movaps    xmm14, xmm10
        mulsd     xmm10, xmm10
        subsd     xmm14, xmm2
        movdqa    xmm2, xmm9
        subsd     xmm10, xmm14
        mulsd     xmm2, xmm13
        addsd     xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+r10]
        mulsd     xmm10, xmm15
        addsd     xmm0, xmm2
        mulsd     xmm9, xmm11
        mulsd     xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+r10]
        subsd     xmm6, xmm0
        movq      xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rax+r10]
        movaps    xmm14, xmm0
        movq      xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rax+r10]
        addsd     xmm6, xmm2
        mulsd     xmm3, xmm13
        addsd     xmm14, xmm12
        mulsd     xmm7, xmm13
        addsd     xmm6, xmm10
        addsd     xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rax+r10]
        subsd     xmm12, xmm14
        addsd     xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rax+r10]
        addsd     xmm12, xmm0
        movaps    xmm10, xmm13
        movaps    xmm0, xmm1
        mulsd     xmm10, xmm13
        addsd     xmm6, xmm12
        movq      xmm4, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rax+r10]
        addsd     xmm9, xmm6
        mulsd     xmm4, xmm13
        mulsd     xmm3, xmm10
        addsd     xmm4, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+r10]
        movq      xmm5, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rax+r10]
        addsd     xmm3, xmm4
        mulsd     xmm5, xmm10
        mulsd     xmm10, xmm10
        addsd     xmm5, xmm7
        mulsd     xmm5, xmm10
        movq      xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rax+r10]
        addsd     xmm5, xmm3
        addsd     xmm8, xmm9
        mulsd     xmm5, xmm13
        andnps    xmm0, XMMWORD PTR [32+rsp]
        addsd     xmm5, xmm8
        addsd     xmm14, xmm5
        xorps     xmm14, XMMWORD PTR [368+rsp]
        andps     xmm14, xmm1
        orps      xmm0, xmm14
        jmp       _B1_2
        ALIGN     16

_B1_9::

__svml_tan1_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan1_ha_ex_B1_B8:
	DD	1600769
	DD	1561709
	DD	878693
	DD	817245
	DD	952405
	DD	1415244
	DD	1353795
	DD	1292346
	DD	1230897
	DD	1169448
	DD	1107999
	DD	1046550
	DD	3211531

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_9
	DD	imagerel _unwind___svml_tan1_ha_ex_B1_B8

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tan1_ha_e9

__svml_tan1_ha_e9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L30::

        sub       rsp, 392
        xor       edx, edx
        vmovups   XMMWORD PTR [192+rsp], xmm15
        vmovups   XMMWORD PTR [208+rsp], xmm14
        vmovups   XMMWORD PTR [224+rsp], xmm13
        vmovups   XMMWORD PTR [240+rsp], xmm12
        vmovups   XMMWORD PTR [256+rsp], xmm11
        vmovups   XMMWORD PTR [272+rsp], xmm10
        vmovups   XMMWORD PTR [288+rsp], xmm9
        vmovups   XMMWORD PTR [304+rsp], xmm8
        vmovups   XMMWORD PTR [320+rsp], xmm7
        vmovups   XMMWORD PTR [336+rsp], xmm6
        mov       QWORD PTR [184+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovsd    xmm2, QWORD PTR [__svml_dtan_ha_data_internal+1024]
        and       r13, -64
        vandpd    xmm1, xmm0, xmm2
        vandnpd   xmm12, xmm2, xmm0
        vmulsd    xmm3, xmm1, QWORD PTR [__svml_dtan_ha_data_internal+1472]
        vcmpnlesd xmm2, xmm1, QWORD PTR [__svml_dtan_ha_data_internal+28160]
        vmovsd    xmm8, QWORD PTR [__svml_dtan_ha_data_internal+1152]
        vmovupd   XMMWORD PTR [352+rsp], xmm0
        vaddsd    xmm9, xmm3, xmm8
        vmovmskpd r8d, xmm2
        vmovq     xmm3, QWORD PTR [__svml_dtan_ha_data_internal+1408]
        vsubsd    xmm14, xmm9, xmm8
        vmulsd    xmm11, xmm14, QWORD PTR [__svml_dtan_ha_data_internal+1216]
        vpand     xmm9, xmm9, xmm3
        vmulsd    xmm10, xmm14, QWORD PTR [__svml_dtan_ha_data_internal+1280]
        vmulsd    xmm5, xmm14, QWORD PTR [__svml_dtan_ha_data_internal+1344]
        vsubsd    xmm13, xmm1, xmm11
        vpand     xmm8, xmm9, XMMWORD PTR [_2il0floatpacket_26]
        vsubsd    xmm6, xmm13, xmm10
        vmovd     eax, xmm8
        vmovupd   xmm8, XMMWORD PTR [_2il0floatpacket_27]
        vsubsd    xmm4, xmm13, xmm6
        imul      ecx, eax, 104
        vmovupd   XMMWORD PTR [368+rsp], xmm12
        vsubsd    xmm15, xmm4, xmm10
        lea       rax, QWORD PTR [__ImageBase]
        vsubsd    xmm7, xmm15, xmm5
        vmovq     xmm13, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rax+rcx]
        vaddsd    xmm11, xmm6, xmm7
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rax+rcx]
        vsubsd    xmm0, xmm6, xmm11
        vmovq     xmm4, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rax+rcx]
        vaddsd    xmm10, xmm7, xmm0
        vsubsd    xmm0, xmm13, xmm11
        vandpd    xmm15, xmm0, xmm8
        vsubsd    xmm14, xmm13, xmm0
        vsubsd    xmm0, xmm0, xmm15
        vsubsd    xmm3, xmm14, xmm11
        vaddsd    xmm14, xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rax+rcx]
        vsubsd    xmm13, xmm3, xmm10
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rax+rcx]
        vaddsd    xmm3, xmm13, xmm14
        vmulsd    xmm6, xmm6, xmm11
        vcvtpd2ps xmm13, xmm15
        vaddsd    xmm6, xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rax+rcx]
        vmovlhps  xmm14, xmm13, xmm13
        vrcpps    xmm0, xmm14
        vcvtps2pd xmm13, xmm0
        vandpd    xmm0, xmm13, xmm8
        vmulsd    xmm15, xmm15, xmm0
        vmovupd   xmm14, XMMWORD PTR [_2il0floatpacket_28]
        vmovq     xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rax+rcx]
        vsubsd    xmm13, xmm14, xmm15
        vmulsd    xmm7, xmm7, xmm11
        vmulsd    xmm8, xmm0, xmm13
        vmulsd    xmm15, xmm13, xmm13
        vaddsd    xmm8, xmm8, xmm0
        vaddsd    xmm14, xmm15, xmm14
        vmulsd    xmm15, xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+rcx]
        vmulsd    xmm0, xmm9, xmm11
        vmulsd    xmm8, xmm8, xmm14
        vmulsd    xmm3, xmm3, xmm8
        vmovq     xmm5, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rax+rcx]
        vsubsd    xmm14, xmm3, xmm13
        vmulsd    xmm13, xmm3, xmm3
        vmulsd    xmm5, xmm5, xmm11
        vsubsd    xmm13, xmm13, xmm14
        vaddsd    xmm14, xmm4, xmm0
        vaddsd    xmm5, xmm5, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+rcx]
        vmulsd    xmm8, xmm13, xmm8
        vsubsd    xmm4, xmm4, xmm14
        vmulsd    xmm13, xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+rcx]
        vaddsd    xmm8, xmm14, xmm15
        vaddsd    xmm4, xmm4, xmm0
        vaddsd    xmm0, xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rax+rcx]
        vsubsd    xmm15, xmm15, xmm8
        vaddsd    xmm13, xmm4, xmm13
        vaddsd    xmm14, xmm15, xmm14
        vmulsd    xmm15, xmm11, xmm11
        vmulsd    xmm3, xmm6, xmm15
        vaddsd    xmm4, xmm13, xmm14
        vaddsd    xmm14, xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+rcx]
        vaddsd    xmm5, xmm3, xmm5
        vmulsd    xmm10, xmm14, xmm10
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rax+rcx]
        vaddsd    xmm3, xmm10, xmm4
        vmulsd    xmm13, xmm9, xmm15
        vmulsd    xmm15, xmm15, xmm15
        vaddsd    xmm13, xmm13, xmm0
        vmulsd    xmm13, xmm13, xmm15
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rax+rcx]
        vaddsd    xmm4, xmm13, xmm5
        vaddsd    xmm6, xmm0, xmm3
        vmulsd    xmm11, xmm4, xmm11
        mov       QWORD PTR [384+rsp], r13
        vaddsd    xmm0, xmm11, xmm6
        vaddsd    xmm3, xmm8, xmm0
        vxorpd    xmm0, xmm3, xmm12
        test      r8d, 1
        jne       _B2_8

_B2_2::

        test      edx, edx
        jne       _B2_4

_B2_3::

        vmovups   xmm6, XMMWORD PTR [336+rsp]
        vmovups   xmm7, XMMWORD PTR [320+rsp]
        vmovups   xmm8, XMMWORD PTR [304+rsp]
        vmovups   xmm9, XMMWORD PTR [288+rsp]
        vmovups   xmm10, XMMWORD PTR [272+rsp]
        vmovups   xmm11, XMMWORD PTR [256+rsp]
        vmovups   xmm12, XMMWORD PTR [240+rsp]
        vmovups   xmm13, XMMWORD PTR [224+rsp]
        vmovups   xmm14, XMMWORD PTR [208+rsp]
        vmovups   xmm15, XMMWORD PTR [192+rsp]
        mov       r13, QWORD PTR [184+rsp]
        add       rsp, 392
        ret

_B2_4::

        vmovupd   xmm1, XMMWORD PTR [352+rsp]
        vmovsd    QWORD PTR [r13], xmm1
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B2_6

_B2_5::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B2_3

_B2_6::

        je        _B2_5

_B2_7::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dtan_ha_cout_rare_internal
        jmp       _B2_5

_B2_8::

        vpand     xmm13, xmm1, XMMWORD PTR [_2il0floatpacket_29]
        vpsrlq    xmm8, xmm13, 52
        vmovd     ecx, xmm8
        vmovsd    xmm5, QWORD PTR [__svml_dtan_ha_data_internal+1088]
        vandpd    xmm14, xmm5, xmm1
        vpand     xmm3, xmm1, XMMWORD PTR [_2il0floatpacket_30]
        vcmpeqsd  xmm6, xmm14, xmm5
        vpaddq    xmm10, xmm3, XMMWORD PTR [_2il0floatpacket_31]
        vmovmskpd edx, xmm6
        vmovdqu   xmm14, XMMWORD PTR [_2il0floatpacket_32]
        vpsrlq    xmm11, xmm10, 32
        vmovupd   XMMWORD PTR [32+rsp], xmm0
        vpand     xmm8, xmm10, xmm14
        and       edx, 1
        lea       r8d, DWORD PTR [rcx+rcx*2]
        shl       r8d, 3
        vmovq     xmm13, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rax+r8]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rax+r8]
        vpand     xmm7, xmm13, xmm14
        vpsrlq    xmm6, xmm0, 32
        vpand     xmm4, xmm0, xmm14
        vpsrlq    xmm0, xmm13, 32
        vpmuludq  xmm13, xmm11, xmm7
        vpmuludq  xmm3, xmm8, xmm4
        vpmuludq  xmm4, xmm11, xmm4
        vpmuludq  xmm5, xmm8, xmm6
        vpmuludq  xmm6, xmm11, xmm6
        vpsrlq    xmm15, xmm13, 32
        vpand     xmm7, xmm3, xmm14
        vpmuludq  xmm13, xmm11, xmm0
        vpsrlq    xmm3, xmm3, 32
        vpmuludq  xmm0, xmm8, xmm0
        vpsrlq    xmm0, xmm0, 32
        vpaddq    xmm13, xmm13, xmm0
        vpaddq    xmm0, xmm4, xmm3
        vpaddq    xmm7, xmm7, xmm13
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rax+r8]
        vpand     xmm9, xmm12, xmm14
        vpsrlq    xmm12, xmm12, 32
        vpmuludq  xmm10, xmm8, xmm9
        vpmuludq  xmm8, xmm8, xmm12
        vpmuludq  xmm11, xmm11, xmm9
        vpaddq    xmm13, xmm15, xmm7
        vpand     xmm7, xmm5, xmm14
        vpsrlq    xmm5, xmm5, 32
        vpaddq    xmm4, xmm7, xmm0
        vpaddq    xmm5, xmm6, xmm5
        vpsrlq    xmm15, xmm13, 32
        vpand     xmm3, xmm10, xmm14
        vpaddq    xmm7, xmm15, xmm4
        vpaddq    xmm6, xmm3, xmm5
        vpsrlq    xmm9, xmm10, 32
        vpsrlq    xmm15, xmm7, 32
        vpand     xmm3, xmm8, xmm14
        vpsllq    xmm4, xmm7, 32
        vpaddq    xmm8, xmm11, xmm9
        vpaddq    xmm6, xmm15, xmm6
        vpaddq    xmm15, xmm3, xmm8
        vpsrlq    xmm5, xmm6, 32
        vpand     xmm6, xmm6, xmm14
        vpaddq    xmm5, xmm5, xmm15
        vpsllq    xmm0, xmm5, 32
        vpand     xmm14, xmm13, xmm14
        vpaddq    xmm10, xmm0, xmm6
        vpaddq    xmm11, xmm4, xmm14
        vpand     xmm12, xmm1, XMMWORD PTR [_2il0floatpacket_33]
        vpsrlq    xmm5, xmm10, 12
        vpxor     xmm6, xmm12, XMMWORD PTR [_2il0floatpacket_34]
        vmovupd   xmm13, XMMWORD PTR [_2il0floatpacket_35]
        vpor      xmm8, xmm5, xmm6
        vpxor     xmm7, xmm12, XMMWORD PTR [_2il0floatpacket_36]
        vaddsd    xmm9, xmm13, xmm8
        vpand     xmm15, xmm11, XMMWORD PTR [_2il0floatpacket_37]
        vpsrlq    xmm11, xmm11, 24
        vpsllq    xmm0, xmm15, 28
        vsubsd    xmm3, xmm9, xmm13
        vpxor     xmm13, xmm12, XMMWORD PTR [_2il0floatpacket_38]
        vpor      xmm4, xmm0, xmm7
        vpand     xmm12, xmm10, XMMWORD PTR [_2il0floatpacket_39]
        vsubsd    xmm8, xmm8, xmm3
        vsubsd    xmm6, xmm4, xmm7
        vpsllq    xmm10, xmm12, 40
        vpor      xmm14, xmm10, xmm11
        vpor      xmm5, xmm14, xmm13
        vmovupd   xmm11, XMMWORD PTR [_2il0floatpacket_40]
        vsubsd    xmm3, xmm5, xmm13
        vmovupd   xmm10, XMMWORD PTR [_2il0floatpacket_42]
        vaddsd    xmm0, xmm8, xmm3
        vsubsd    xmm8, xmm8, xmm0
        vaddsd    xmm15, xmm3, xmm8
        vaddsd    xmm12, xmm15, xmm6
        vmovupd   xmm15, XMMWORD PTR [_2il0floatpacket_27]
        vandpd    xmm4, xmm0, xmm15
        vmovupd   xmm6, XMMWORD PTR [_2il0floatpacket_41]
        vsubsd    xmm7, xmm0, xmm4
        vmulsd    xmm5, xmm6, xmm4
        vmulsd    xmm13, xmm11, xmm12
        vmulsd    xmm14, xmm6, xmm7
        vmulsd    xmm6, xmm10, xmm4
        vmulsd    xmm8, xmm10, xmm7
        vaddsd    xmm3, xmm14, xmm6
        vaddsd    xmm0, xmm13, xmm8
        vmovupd   xmm12, XMMWORD PTR [_2il0floatpacket_44]
        vaddsd    xmm4, xmm3, xmm0
        vandpd    xmm11, xmm1, XMMWORD PTR [_2il0floatpacket_43]
        vaddsd    xmm7, xmm5, xmm4
        vcmpgtsd  xmm14, xmm11, xmm12
        vcmplesd  xmm10, xmm11, xmm12
        vsubsd    xmm5, xmm5, xmm7
        vandpd    xmm1, xmm10, xmm1
        vaddsd    xmm6, xmm5, xmm4
        vandpd    xmm5, xmm14, xmm7
        vandpd    xmm8, xmm14, xmm6
        vorpd     xmm5, xmm1, xmm5
        vpand     xmm1, xmm9, XMMWORD PTR [_2il0floatpacket_45]
        vpand     xmm13, xmm1, XMMWORD PTR [_2il0floatpacket_26]
        vmovd     r9d, xmm13
        imul      r10d, r9d, 104
        vmovq     xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rax+r10]
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rax+r10]
        vsubsd    xmm1, xmm3, xmm5
        vandpd    xmm4, xmm1, xmm15
        vsubsd    xmm0, xmm3, xmm1
        vsubsd    xmm1, xmm1, xmm4
        vsubsd    xmm14, xmm0, xmm5
        vaddsd    xmm0, xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rax+r10]
        vcvtpd2ps xmm1, xmm4
        vsubsd    xmm3, xmm14, xmm8
        vmovq     xmm13, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rax+r10]
        vaddsd    xmm14, xmm3, xmm0
        vmovlhps  xmm3, xmm1, xmm1
        vrcpps    xmm0, xmm3
        vcvtps2pd xmm1, xmm0
        vandpd    xmm1, xmm1, xmm15
        vmulsd    xmm4, xmm4, xmm1
        vmovupd   xmm15, XMMWORD PTR [_2il0floatpacket_28]
        vmovq     xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rax+r10]
        vsubsd    xmm0, xmm15, xmm4
        vmulsd    xmm7, xmm7, xmm5
        vmulsd    xmm3, xmm1, xmm0
        vmulsd    xmm4, xmm0, xmm0
        vaddsd    xmm3, xmm3, xmm1
        vaddsd    xmm15, xmm4, xmm15
        vmulsd    xmm4, xmm3, xmm15
        vmulsd    xmm3, xmm14, xmm4
        vmulsd    xmm15, xmm3, xmm3
        vsubsd    xmm0, xmm3, xmm0
        vmovq     xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rax+r10]
        vsubsd    xmm14, xmm15, xmm0
        vmulsd    xmm0, xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+r10]
        vmulsd    xmm10, xmm10, xmm5
        vmulsd    xmm4, xmm14, xmm4
        vmulsd    xmm14, xmm11, xmm5
        vmulsd    xmm3, xmm4, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+r10]
        vaddsd    xmm4, xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rax+r10]
        vaddsd    xmm15, xmm13, xmm14
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rax+r10]
        vsubsd    xmm13, xmm13, xmm15
        vmulsd    xmm9, xmm9, xmm5
        vaddsd    xmm1, xmm15, xmm0
        vaddsd    xmm13, xmm13, xmm14
        vsubsd    xmm0, xmm0, xmm1
        vaddsd    xmm3, xmm13, xmm3
        vaddsd    xmm15, xmm0, xmm15
        vaddsd    xmm0, xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+r10]
        vaddsd    xmm7, xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rax+r10]
        vaddsd    xmm13, xmm3, xmm15
        vmulsd    xmm15, xmm5, xmm5
        vaddsd    xmm3, xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+r10]
        vmulsd    xmm4, xmm4, xmm15
        vmulsd    xmm3, xmm3, xmm8
        vaddsd    xmm0, xmm4, xmm0
        vaddsd    xmm10, xmm3, xmm13
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rax+r10]
        vmulsd    xmm11, xmm12, xmm15
        vmulsd    xmm15, xmm15, xmm15
        vaddsd    xmm9, xmm11, xmm7
        vmulsd    xmm8, xmm9, xmm15
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rax+r10]
        vaddsd    xmm12, xmm8, xmm0
        vaddsd    xmm6, xmm6, xmm10
        vmulsd    xmm5, xmm12, xmm5
        vaddsd    xmm13, xmm5, xmm6
        vaddsd    xmm0, xmm1, xmm13
        vxorpd    xmm1, xmm0, XMMWORD PTR [368+rsp]
        vmovupd   xmm0, XMMWORD PTR [32+rsp]
        vblendvpd xmm0, xmm0, xmm1, xmm2
        jmp       _B2_2
        ALIGN     16

_B2_9::

__svml_tan1_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan1_ha_e9_B1_B8:
	DD	1601281
	DD	1561711
	DD	1402983
	DD	1341534
	DD	1280085
	DD	1218636
	DD	1157187
	DD	1095738
	DD	1034289
	DD	972840
	DD	911391
	DD	849942
	DD	3211531

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_9
	DD	imagerel _unwind___svml_tan1_ha_e9_B1_B8

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tan4_ha_e9

__svml_tan4_ha_e9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L59::

        push      rbp
        sub       rsp, 864
        lea       rbp, QWORD PTR [32+rsp]
        xor       eax, eax
        vmovups   YMMWORD PTR [496+rbp], ymm15
        vmovups   YMMWORD PTR [528+rbp], ymm14
        vmovups   YMMWORD PTR [560+rbp], ymm13
        vmovups   YMMWORD PTR [592+rbp], ymm12
        vmovups   YMMWORD PTR [624+rbp], ymm11
        vmovups   YMMWORD PTR [656+rbp], ymm10
        vmovups   YMMWORD PTR [688+rbp], ymm9
        vmovups   YMMWORD PTR [720+rbp], ymm8
        vmovups   YMMWORD PTR [752+rbp], ymm7
        vmovups   YMMWORD PTR [784+rbp], ymm6
        vmovdqu   xmm6, XMMWORD PTR [_2il0floatpacket_26]
        mov       QWORD PTR [816+rbp], r13
        lea       r13, QWORD PTR [239+rbp]
        vmovupd   ymm5, YMMWORD PTR [__svml_dtan_ha_data_internal+1024]
        and       r13, -64
        vmovupd   ymm10, YMMWORD PTR [__svml_dtan_ha_data_internal+1152]
        vmovupd   YMMWORD PTR [128+r13], ymm0
        vandpd    ymm9, ymm0, ymm5
        vmulpd    ymm14, ymm9, YMMWORD PTR [__svml_dtan_ha_data_internal+1472]
        vmovupd   YMMWORD PTR [224+r13], ymm9
        vcmpnle_uqpd ymm13, ymm9, YMMWORD PTR [__svml_dtan_ha_data_internal+28160]
        vaddpd    ymm8, ymm10, ymm14
        vmovupd   YMMWORD PTR [160+r13], ymm13
        vandnpd   ymm15, ymm5, ymm0
        vsubpd    ymm5, ymm8, ymm10
        vmovupd   YMMWORD PTR [192+r13], ymm15
        vmulpd    ymm4, ymm5, YMMWORD PTR [__svml_dtan_ha_data_internal+1216]
        vmulpd    ymm1, ymm5, YMMWORD PTR [__svml_dtan_ha_data_internal+1280]
        vmulpd    ymm7, ymm5, YMMWORD PTR [__svml_dtan_ha_data_internal+1344]
        vsubpd    ymm3, ymm9, ymm4
        vmovupd   xmm9, XMMWORD PTR [__svml_dtan_ha_data_internal+1408]
        mov       QWORD PTR [824+rbp], r13
        vextractf128 xmm12, ymm13, 1
        vshufps   xmm11, xmm13, xmm12, 221
        vsubpd    ymm13, ymm3, ymm1
        vmovups   XMMWORD PTR [rbp], xmm11
        vsubpd    ymm2, ymm3, ymm13
        vsubpd    ymm15, ymm2, ymm1
        vsubpd    ymm12, ymm15, ymm7
        vaddpd    ymm15, ymm13, ymm12
        vsubpd    ymm11, ymm13, ymm15
        vaddpd    ymm5, ymm12, ymm11
        vmovupd   YMMWORD PTR [r13], ymm5
        vandps    xmm14, xmm8, xmm9
        vextractf128 xmm10, ymm8, 1
        vpand     xmm3, xmm14, xmm6
        vmovd     edx, xmm3
        vandps    xmm4, xmm10, xmm9
        vpand     xmm0, xmm4, xmm6
        vmovd     r11d, xmm0
        imul      r9d, edx, 104
        imul      ecx, r11d, 104
        vpextrd   r10d, xmm3, 2
        lea       rdx, QWORD PTR [__ImageBase]
        imul      r8d, r10d, 104
        vmovq     xmm13, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rdx+r9]
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rdx+rcx]
        vpextrd   r10d, xmm0, 2
        imul      r11d, r10d, 104
        vmovhpd   xmm12, xmm13, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rdx+r8]
        vmovq     xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rdx+r9]
        vmovq     xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rdx+r9]
        vmovq     xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rdx+rcx]
        vmovhpd   xmm14, xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rdx+r11]
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rdx+rcx]
        vmovhpd   xmm1, xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rdx+r8]
        vmovhpd   xmm9, xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rdx+r8]
        vmovhpd   xmm8, xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rdx+r11]
        vmovhpd   xmm3, xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rdx+r11]
        vmovq     xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rdx+r9]
        vmovhpd   xmm7, xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rdx+r8]
        vinsertf128 ymm0, ymm12, xmm14, 1
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rdx+r9]
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rdx+rcx]
        vmovhpd   xmm11, xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rdx+r8]
        vmovhpd   xmm10, xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rdx+r11]
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rdx+r9]
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rdx+rcx]
        vinsertf128 ymm4, ymm1, xmm8, 1
        vmovq     xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rdx+rcx]
        vinsertf128 ymm6, ymm9, xmm3, 1
        vinsertf128 ymm9, ymm11, xmm10, 1
        vmovhpd   xmm11, xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rdx+r8]
        vmovq     xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rdx+r9]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rdx+rcx]
        vmovhpd   xmm10, xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rdx+r11]
        vmovhpd   xmm13, xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rdx+r11]
        vmovups   YMMWORD PTR [32+r13], ymm9
        vmovhpd   xmm2, xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rdx+r8]
        vmovhpd   xmm8, xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rdx+r11]
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rdx+r9]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rdx+rcx]
        vmovhpd   xmm3, xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rdx+r8]
        vinsertf128 ymm12, ymm11, xmm10, 1
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rdx+r9]
        vmovq     xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rdx+rcx]
        vmovhpd   xmm14, xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rdx+r8]
        vmovhpd   xmm9, xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rdx+r11]
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rdx+rcx]
        vinsertf128 ymm7, ymm7, xmm13, 1
        vinsertf128 ymm13, ymm2, xmm8, 1
        vmovhpd   xmm8, xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rdx+r11]
        vmovq     xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rdx+r9]
        vmovhpd   xmm1, xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rdx+r8]
        vinsertf128 ymm8, ymm3, xmm8, 1
        vinsertf128 ymm10, ymm14, xmm9, 1
        vmovhpd   xmm14, xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rdx+r11]
        vmovq     xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rdx+r9]
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rdx+rcx]
        vmovhpd   xmm2, xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rdx+r8]
        vmovq     xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rdx+r9]
        vmulpd    ymm8, ymm8, ymm15
        vaddpd    ymm8, ymm12, ymm8
        vinsertf128 ymm9, ymm1, xmm14, 1
        vmovhpd   xmm14, xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rdx+r11]
        vmovhpd   xmm1, xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rdx+r8]
        vmulpd    ymm9, ymm9, ymm15
        vaddpd    ymm10, ymm10, ymm9
        vinsertf128 ymm11, ymm2, xmm14, 1
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rdx+rcx]
        vmovhpd   xmm3, xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rdx+r11]
        vmovq     xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rdx+r9]
        vinsertf128 ymm14, ymm1, xmm3, 1
        vmovhpd   xmm3, xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rdx+r8]
        vmovq     xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rdx+rcx]
        vmovhpd   xmm1, xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rdx+r11]
        vmovupd   ymm2, YMMWORD PTR [_2il0floatpacket_46]
        vmulpd    ymm14, ymm14, ymm15
        vaddpd    ymm11, ymm11, ymm14
        vinsertf128 ymm3, ymm3, xmm1, 1
        vsubpd    ymm1, ymm4, ymm15
        vmovups   YMMWORD PTR [64+r13], ymm3
        vsubpd    ymm4, ymm4, ymm1
        vsubpd    ymm4, ymm4, ymm15
        vandpd    ymm3, ymm1, ymm2
        vsubpd    ymm5, ymm4, ymm5
        vsubpd    ymm1, ymm1, ymm3
        vcvtpd2ps xmm4, ymm3
        vaddpd    ymm0, ymm0, ymm1
        vrcpps    xmm1, xmm4
        vaddpd    ymm5, ymm5, ymm0
        vcvtps2pd ymm0, xmm1
        vandpd    ymm4, ymm0, ymm2
        vmovupd   ymm2, YMMWORD PTR [_2il0floatpacket_47]
        vmulpd    ymm3, ymm3, ymm4
        vsubpd    ymm1, ymm2, ymm3
        vmulpd    ymm3, ymm4, ymm1
        vaddpd    ymm0, ymm4, ymm3
        vmulpd    ymm3, ymm1, ymm1
        vaddpd    ymm2, ymm2, ymm3
        vmulpd    ymm2, ymm0, ymm2
        vmulpd    ymm5, ymm5, ymm2
        vsubpd    ymm3, ymm5, ymm1
        vmulpd    ymm5, ymm5, ymm5
        vsubpd    ymm0, ymm5, ymm3
        vmulpd    ymm3, ymm6, ymm4
        vmulpd    ymm1, ymm2, ymm0
        vmulpd    ymm2, ymm13, ymm15
        vmulpd    ymm6, ymm6, ymm1
        vaddpd    ymm4, ymm7, ymm2
        vaddpd    ymm1, ymm13, ymm12
        vsubpd    ymm7, ymm7, ymm4
        vaddpd    ymm5, ymm3, ymm4
        vaddpd    ymm7, ymm2, ymm7
        vsubpd    ymm0, ymm3, ymm5
        vaddpd    ymm6, ymm6, ymm7
        vaddpd    ymm7, ymm4, ymm0
        vmulpd    ymm0, ymm15, ymm15
        vaddpd    ymm7, ymm6, ymm7
        vmulpd    ymm12, ymm0, YMMWORD PTR [64+r13]
        vmulpd    ymm3, ymm0, ymm0
        vmulpd    ymm13, ymm10, ymm0
        vaddpd    ymm2, ymm11, ymm12
        vaddpd    ymm4, ymm8, ymm13
        vmulpd    ymm6, ymm2, ymm3
        vmulpd    ymm8, ymm1, YMMWORD PTR [r13]
        vmovups   xmm2, XMMWORD PTR [rbp]
        vmovmskps r8d, xmm2
        vaddpd    ymm9, ymm4, ymm6
        vaddpd    ymm7, ymm7, ymm8
        vmulpd    ymm15, ymm15, ymm9
        vaddpd    ymm10, ymm7, YMMWORD PTR [32+r13]
        vmovupd   ymm7, YMMWORD PTR [224+r13]
        vaddpd    ymm0, ymm10, ymm15
        vaddpd    ymm1, ymm5, ymm0
        vxorpd    ymm0, ymm1, YMMWORD PTR [192+r13]
        test      r8d, r8d
        jne       _B3_12

_B3_2::

        test      eax, eax
        jne       _B3_4

_B3_3::

        vmovups   ymm6, YMMWORD PTR [784+rbp]
        vmovups   ymm7, YMMWORD PTR [752+rbp]
        vmovups   ymm8, YMMWORD PTR [720+rbp]
        vmovups   ymm9, YMMWORD PTR [688+rbp]
        vmovups   ymm10, YMMWORD PTR [656+rbp]
        vmovups   ymm11, YMMWORD PTR [624+rbp]
        vmovups   ymm12, YMMWORD PTR [592+rbp]
        vmovups   ymm13, YMMWORD PTR [560+rbp]
        vmovups   ymm14, YMMWORD PTR [528+rbp]
        vmovups   ymm15, YMMWORD PTR [496+rbp]
        mov       r13, QWORD PTR [816+rbp]
        lea       rsp, QWORD PTR [832+rbp]
        pop       rbp
        ret

_B3_4::

        vmovupd   ymm1, YMMWORD PTR [128+r13]
        vmovupd   YMMWORD PTR [64+r13], ymm0
        vmovupd   YMMWORD PTR [r13], ymm1
        je        _B3_3

_B3_7::

        xor       edx, edx
        mov       QWORD PTR [8+rbp], rbx
        mov       ebx, edx
        mov       QWORD PTR [rbp], rsi
        mov       esi, eax

_B3_8::

        bt        esi, ebx
        jc        _B3_11

_B3_9::

        inc       ebx
        cmp       ebx, 4
        jl        _B3_8

_B3_10::

        mov       rbx, QWORD PTR [8+rbp]
        mov       rsi, QWORD PTR [rbp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        jmp       _B3_3

_B3_11::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dtan_ha_cout_rare_internal
        jmp       _B3_9

_B3_12::

        vmovupd   ymm14, YMMWORD PTR [__svml_dtan_ha_data_internal+1088]
        vmovdqu   xmm6, XMMWORD PTR [_2il0floatpacket_29]
        vmovupd   YMMWORD PTR [224+r13], ymm7
        vmovupd   YMMWORD PTR [r13], ymm0
        vandpd    ymm12, ymm14, ymm7
        vcmpeqpd  ymm8, ymm12, ymm14
        vpand     xmm15, xmm6, xmm7
        vextractf128 xmm11, ymm7, 1
        vpsrlq    xmm0, xmm15, 52
        vmovd     r8d, xmm0
        vmovups   XMMWORD PTR [rbp], xmm7
        vpand     xmm3, xmm6, xmm11
        vpsrlq    xmm1, xmm3, 52
        vmovd     r10d, xmm1
        lea       r9d, DWORD PTR [r8+r8*2]
        vmovups   XMMWORD PTR [16+rbp], xmm11
        vmovdqu   xmm15, XMMWORD PTR [_2il0floatpacket_31]
        vpextrd   ecx, xmm0, 2
        vpextrd   r8d, xmm1, 2
        lea       r10d, DWORD PTR [r10+r10*2]
        shl       r9d, 3
        shl       r10d, 3
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rdx+r9]
        lea       r11d, DWORD PTR [rcx+rcx*2]
        shl       r11d, 3
        lea       ecx, DWORD PTR [r8+r8*2]
        shl       ecx, 3
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rdx+r9]
        vmovq     xmm4, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rdx+r10]
        vmovhpd   xmm1, xmm14, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rdx+r11]
        vmovhpd   xmm9, xmm9, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rdx+r11]
        vmovq     xmm5, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rdx+r9]
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rdx+r10]
        vmovhpd   xmm2, xmm5, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rdx+r11]
        vmovupd   XMMWORD PTR [32+rbp], xmm2
        vextractf128 xmm13, ymm8, 1
        vshufps   xmm10, xmm8, xmm13, 221
        vmovmskps eax, xmm10
        vmovdqu   xmm10, XMMWORD PTR [_2il0floatpacket_30]
        vpand     xmm7, xmm10, xmm7
        vpand     xmm11, xmm10, xmm11
        vpaddq    xmm14, xmm7, xmm15
        vpaddq    xmm6, xmm11, xmm15
        vmovhpd   xmm13, xmm4, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rdx+rcx]
        vpsrlq    xmm10, xmm14, 32
        vmovq     xmm8, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rdx+r10]
        vmovdqu   xmm15, XMMWORD PTR [_2il0floatpacket_32]
        vmovhpd   xmm4, xmm12, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rdx+rcx]
        vpand     xmm12, xmm13, xmm15
        vmovhpd   xmm3, xmm8, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rdx+rcx]
        vpsrlq    xmm8, xmm9, 32
        vpand     xmm5, xmm9, xmm15
        vpand     xmm9, xmm14, xmm15
        vpand     xmm14, xmm6, xmm15
        vpand     xmm11, xmm2, xmm15
        vmovdqu   XMMWORD PTR [80+rbp], xmm12
        vpsrlq    xmm7, xmm4, 32
        vpmuludq  xmm12, xmm14, xmm12
        vpand     xmm0, xmm4, xmm15
        vmovdqu   XMMWORD PTR [64+rbp], xmm11
        vpsrlq    xmm2, xmm1, 32
        vpmuludq  xmm11, xmm9, xmm11
        vpsrlq    xmm4, xmm3, 32
        vmovdqu   XMMWORD PTR [112+rbp], xmm7
        vpand     xmm3, xmm3, xmm15
        vmovdqu   XMMWORD PTR [160+rbp], xmm12
        vpmuludq  xmm12, xmm14, xmm7
        vpand     xmm7, xmm1, xmm15
        vmovdqu   XMMWORD PTR [96+rbp], xmm8
        vmovdqu   XMMWORD PTR [128+rbp], xmm0
        vmovdqu   XMMWORD PTR [144+rbp], xmm11
        vpmuludq  xmm11, xmm9, xmm8
        vpmuludq  xmm8, xmm14, xmm0
        vpmuludq  xmm0, xmm10, xmm7
        vpmuludq  xmm7, xmm10, xmm2
        vpmuludq  xmm2, xmm9, xmm2
        vmovupd   XMMWORD PTR [48+rbp], xmm13
        vpsrlq    xmm13, xmm6, 32
        vpmuludq  xmm6, xmm9, xmm5
        vpsrlq    xmm2, xmm2, 32
        vpmuludq  xmm5, xmm10, xmm5
        vpaddq    xmm7, xmm7, xmm2
        vpmuludq  xmm2, xmm13, xmm4
        vpmuludq  xmm4, xmm14, xmm4
        vpsrlq    xmm1, xmm0, 32
        vpand     xmm0, xmm6, xmm15
        vpaddq    xmm0, xmm0, xmm7
        vpaddq    xmm7, xmm1, xmm0
        vpmuludq  xmm1, xmm13, xmm3
        vpsrlq    xmm4, xmm4, 32
        vpand     xmm3, xmm8, xmm15
        vpaddq    xmm2, xmm2, xmm4
        vpaddq    xmm3, xmm3, xmm2
        vpsrlq    xmm0, xmm1, 32
        vpsrlq    xmm6, xmm6, 32
        vpaddq    xmm4, xmm0, xmm3
        vpaddq    xmm2, xmm5, xmm6
        vpmuludq  xmm3, xmm13, XMMWORD PTR [128+rbp]
        vpand     xmm0, xmm11, xmm15
        vpsrlq    xmm1, xmm7, 32
        vpaddq    xmm6, xmm0, xmm2
        vpsrlq    xmm8, xmm8, 32
        vpsrlq    xmm5, xmm4, 32
        vpaddq    xmm0, xmm1, xmm6
        vpaddq    xmm2, xmm3, xmm8
        vpmuludq  xmm6, xmm10, XMMWORD PTR [96+rbp]
        vpmuludq  xmm10, xmm10, XMMWORD PTR [64+rbp]
        vpand     xmm1, xmm12, xmm15
        vpsrlq    xmm11, xmm11, 32
        vpaddq    xmm8, xmm1, xmm2
        vpaddq    xmm2, xmm6, xmm11
        vpaddq    xmm1, xmm5, xmm8
        vmovdqu   xmm5, XMMWORD PTR [144+rbp]
        vpsrlq    xmm3, xmm0, 32
        vpand     xmm8, xmm5, xmm15
        vpsrlq    xmm12, xmm12, 32
        vpaddq    xmm11, xmm8, xmm2
        vpaddq    xmm3, xmm3, xmm11
        vpmuludq  xmm11, xmm13, XMMWORD PTR [112+rbp]
        vpmuludq  xmm13, xmm13, XMMWORD PTR [80+rbp]
        vpaddq    xmm11, xmm11, xmm12
        vmovdqu   xmm8, XMMWORD PTR [160+rbp]
        vpsrlq    xmm6, xmm1, 32
        vpand     xmm2, xmm8, xmm15
        vpsrlq    xmm12, xmm3, 32
        vpaddq    xmm2, xmm2, xmm11
        vpaddq    xmm11, xmm6, xmm2
        vmovupd   xmm6, XMMWORD PTR [32+rbp]
        vpand     xmm3, xmm3, xmm15
        vpsrlq    xmm2, xmm6, 32
        vpand     xmm7, xmm7, xmm15
        vpmuludq  xmm9, xmm9, xmm2
        vpand     xmm4, xmm4, xmm15
        vpand     xmm2, xmm9, xmm15
        vpsrlq    xmm9, xmm5, 32
        vmovupd   xmm5, XMMWORD PTR [48+rbp]
        vpaddq    xmm10, xmm10, xmm9
        vpsrlq    xmm9, xmm5, 32
        vpmuludq  xmm14, xmm14, xmm9
        vpsrlq    xmm9, xmm8, 32
        vpaddq    xmm2, xmm2, xmm10
        vpaddq    xmm12, xmm12, xmm2
        vpsllq    xmm6, xmm12, 32
        vpand     xmm12, xmm14, xmm15
        vpaddq    xmm14, xmm13, xmm9
        vpaddq    xmm2, xmm6, xmm3
        vpaddq    xmm8, xmm12, xmm14
        vpsrlq    xmm10, xmm11, 32
        vpand     xmm11, xmm11, xmm15
        vpaddq    xmm13, xmm10, xmm8
        vpsllq    xmm10, xmm13, 32
        vpsllq    xmm3, xmm1, 32
        vmovdqu   xmm15, XMMWORD PTR [_2il0floatpacket_33]
        vpsllq    xmm6, xmm0, 32
        vpaddq    xmm11, xmm10, xmm11
        vpaddq    xmm13, xmm3, xmm4
        vpaddq    xmm9, xmm6, xmm7
        vmovupd   ymm0, YMMWORD PTR [_2il0floatpacket_48]
        vpand     xmm3, xmm15, XMMWORD PTR [rbp]
        vpsrlq    xmm4, xmm2, 12
        vmovdqu   xmm14, XMMWORD PTR [_2il0floatpacket_34]
        vpsrlq    xmm8, xmm11, 12
        vpand     xmm6, xmm15, XMMWORD PTR [16+rbp]
        vpxor     xmm7, xmm3, xmm14
        vpxor     xmm10, xmm6, xmm14
        vpor      xmm12, xmm4, xmm7
        vpor      xmm15, xmm8, xmm10
        vmovdqu   xmm7, XMMWORD PTR [_2il0floatpacket_36]
        vpxor     xmm8, xmm3, xmm7
        vpxor     xmm14, xmm6, xmm7
        vinsertf128 ymm5, ymm12, xmm15, 1
        vaddpd    ymm1, ymm5, ymm0
        vmovdqu   xmm12, XMMWORD PTR [_2il0floatpacket_37]
        vsubpd    ymm4, ymm1, ymm0
        vmovdqu   xmm0, XMMWORD PTR [_2il0floatpacket_45]
        vsubpd    ymm10, ymm5, ymm4
        vpand     xmm5, xmm12, xmm9
        vpsllq    xmm4, xmm5, 28
        vpand     xmm12, xmm12, xmm13
        vpor      xmm7, xmm4, xmm8
        vpsllq    xmm4, xmm12, 28
        vpor      xmm12, xmm4, xmm14
        vpsrlq    xmm9, xmm9, 24
        vinsertf128 ymm7, ymm7, xmm12, 1
        vinsertf128 ymm14, ymm8, xmm14, 1
        vmovdqu   xmm12, XMMWORD PTR [_2il0floatpacket_39]
        vsubpd    ymm4, ymm7, ymm14
        vpand     xmm2, xmm12, xmm2
        vmovdqu   xmm7, XMMWORD PTR [_2il0floatpacket_38]
        vpand     xmm11, xmm12, xmm11
        vpxor     xmm14, xmm6, xmm7
        vpsllq    xmm6, xmm2, 40
        vpxor     xmm8, xmm3, xmm7
        vpor      xmm2, xmm6, xmm9
        vpor      xmm7, xmm2, xmm8
        vpsllq    xmm2, xmm11, 40
        vpsrlq    xmm12, xmm13, 24
        vpor      xmm13, xmm2, xmm12
        vpor      xmm3, xmm13, xmm14
        vmovupd   ymm9, YMMWORD PTR [_2il0floatpacket_50]
        vinsertf128 ymm5, ymm7, xmm3, 1
        vinsertf128 ymm14, ymm8, xmm14, 1
        vsubpd    ymm2, ymm5, ymm14
        vmovupd   ymm3, YMMWORD PTR [_2il0floatpacket_46]
        vmovupd   ymm14, YMMWORD PTR [_2il0floatpacket_51]
        vaddpd    ymm7, ymm10, ymm2
        vsubpd    ymm10, ymm10, ymm7
        vaddpd    ymm2, ymm2, ymm10
        vandpd    ymm11, ymm7, ymm3
        vaddpd    ymm12, ymm4, ymm2
        vsubpd    ymm8, ymm7, ymm11
        vmulpd    ymm10, ymm14, ymm11
        vmulpd    ymm6, ymm12, YMMWORD PTR [_2il0floatpacket_49]
        vmulpd    ymm13, ymm9, ymm8
        vmulpd    ymm5, ymm14, ymm8
        vmulpd    ymm4, ymm9, ymm11
        vaddpd    ymm2, ymm13, ymm10
        vaddpd    ymm7, ymm6, ymm5
        vaddpd    ymm9, ymm2, ymm7
        vaddpd    ymm11, ymm4, ymm9
        vsubpd    ymm4, ymm4, ymm11
        vaddpd    ymm13, ymm9, ymm4
        vmovupd   ymm9, YMMWORD PTR [224+r13]
        vmovupd   ymm4, YMMWORD PTR [_2il0floatpacket_53]
        vandpd    ymm2, ymm9, YMMWORD PTR [_2il0floatpacket_52]
        vcmple_oqpd ymm7, ymm2, ymm4
        vcmpgt_oqpd ymm8, ymm2, ymm4
        vmovdqu   xmm2, XMMWORD PTR [_2il0floatpacket_26]
        vandpd    ymm12, ymm7, ymm9
        vandpd    ymm14, ymm8, ymm11
        vorpd     ymm4, ymm12, ymm14
        vandpd    ymm5, ymm8, ymm13
        vmovupd   YMMWORD PTR [32+r13], ymm5
        vextractf128 xmm15, ymm1, 1
        vpand     xmm1, xmm1, xmm0
        vpand     xmm7, xmm1, xmm2
        vmovd     r9d, xmm7
        vpand     xmm15, xmm15, xmm0
        vpand     xmm9, xmm15, xmm2
        vmovd     ecx, xmm9
        imul      r9d, r9d, 104
        imul      ecx, ecx, 104
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rdx+r9]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rdx+r9]
        vpextrd   r11d, xmm7, 2
        vpextrd   r10d, xmm9, 2
        imul      r8d, r11d, 104
        imul      r11d, r10d, 104
        vmovq     xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rdx+rcx]
        vmovq     xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rdx+rcx]
        vmovq     xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rdx+r9]
        vmovhpd   xmm14, xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rdx+r8]
        vmovhpd   xmm13, xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rdx+r11]
        vmovhpd   xmm2, xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rdx+r8]
        vmovhpd   xmm11, xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rdx+r11]
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rdx+rcx]
        vmovhpd   xmm15, xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rdx+r8]
        vmovq     xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rdx+r9]
        vmovhpd   xmm0, xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rdx+r11]
        vmovhpd   xmm9, xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rdx+r8]
        vinsertf128 ymm1, ymm14, xmm13, 1
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rdx+r9]
        vmovq     xmm13, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rdx+rcx]
        vmovhpd   xmm8, xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rdx+r8]
        vmovhpd   xmm10, xmm13, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rdx+r11]
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rdx+r9]
        vmovq     xmm13, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rdx+rcx]
        vinsertf128 ymm2, ymm2, xmm11, 1
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rdx+rcx]
        vmovhpd   xmm12, xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rdx+r11]
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rdx+rcx]
        vinsertf128 ymm6, ymm15, xmm0, 1
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rdx+r9]
        vinsertf128 ymm15, ymm8, xmm10, 1
        vinsertf128 ymm7, ymm9, xmm12, 1
        vmovhpd   xmm9, xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rdx+r8]
        vmovhpd   xmm12, xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rdx+r11]
        vmovups   YMMWORD PTR [64+r13], ymm15
        vmovhpd   xmm8, xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rdx+r8]
        vmovq     xmm15, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rdx+r9]
        vmovhpd   xmm10, xmm13, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rdx+r11]
        vmovhpd   xmm0, xmm15, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rdx+r8]
        vinsertf128 ymm11, ymm9, xmm12, 1
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rdx+rcx]
        vmovhpd   xmm14, xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rdx+r11]
        vinsertf128 ymm12, ymm8, xmm10, 1
        vmovq     xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rdx+r9]
        vmovq     xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rdx+rcx]
        vmovhpd   xmm13, xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rdx+r8]
        vmovhpd   xmm15, xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rdx+r11]
        vmovq     xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rdx+rcx]
        vinsertf128 ymm9, ymm0, xmm14, 1
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rdx+r9]
        vmovhpd   xmm8, xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rdx+r8]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rdx+r9]
        vmovhpd   xmm14, xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rdx+r8]
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rdx+r9]
        vmulpd    ymm9, ymm4, ymm9
        vaddpd    ymm9, ymm12, ymm9
        vinsertf128 ymm13, ymm13, xmm15, 1
        vmovhpd   xmm15, xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rdx+r11]
        vinsertf128 ymm10, ymm8, xmm15, 1
        vmovq     xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rdx+rcx]
        vmovhpd   xmm15, xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rdx+r11]
        vmovhpd   xmm8, xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rdx+r8]
        vinsertf128 ymm14, ymm14, xmm15, 1
        vmovq     xmm15, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rdx+rcx]
        vmovhpd   xmm0, xmm15, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rdx+r11]
        vmovq     xmm15, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rdx+r9]
        vmovhpd   xmm15, xmm15, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rdx+r8]
        vinsertf128 ymm8, ymm8, xmm0, 1
        vmovq     xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rdx+rcx]
        vmovhpd   xmm0, xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rdx+r11]
        vmulpd    ymm8, ymm4, ymm8
        vaddpd    ymm14, ymm14, ymm8
        vinsertf128 ymm15, ymm15, xmm0, 1
        vsubpd    ymm0, ymm2, ymm4
        vmovups   YMMWORD PTR [96+r13], ymm15
        vsubpd    ymm2, ymm2, ymm0
        vsubpd    ymm2, ymm2, ymm4
        vandpd    ymm15, ymm0, ymm3
        vsubpd    ymm5, ymm2, ymm5
        vsubpd    ymm0, ymm0, ymm15
        vcvtpd2ps xmm2, ymm15
        vaddpd    ymm1, ymm1, ymm0
        vrcpps    xmm0, xmm2
        vaddpd    ymm5, ymm5, ymm1
        vcvtps2pd ymm1, xmm0
        vandpd    ymm3, ymm1, ymm3
        vmulpd    ymm15, ymm15, ymm3
        vmovupd   ymm1, YMMWORD PTR [_2il0floatpacket_47]
        vsubpd    ymm0, ymm1, ymm15
        vmulpd    ymm2, ymm3, ymm0
        vaddpd    ymm15, ymm3, ymm2
        vmulpd    ymm2, ymm0, ymm0
        vmulpd    ymm3, ymm6, ymm3
        vaddpd    ymm1, ymm1, ymm2
        vmulpd    ymm2, ymm15, ymm1
        vmulpd    ymm5, ymm5, ymm2
        vmulpd    ymm15, ymm5, ymm5
        vsubpd    ymm0, ymm5, ymm0
        vmulpd    ymm5, ymm4, ymm11
        vsubpd    ymm1, ymm15, ymm0
        vaddpd    ymm15, ymm7, ymm5
        vmulpd    ymm2, ymm2, ymm1
        vmulpd    ymm1, ymm4, ymm10
        vsubpd    ymm7, ymm7, ymm15
        vmulpd    ymm6, ymm6, ymm2
        vaddpd    ymm7, ymm5, ymm7
        vaddpd    ymm2, ymm3, ymm15
        vmulpd    ymm5, ymm4, ymm4
        vsubpd    ymm3, ymm3, ymm2
        vaddpd    ymm7, ymm6, ymm7
        vaddpd    ymm6, ymm11, ymm12
        vaddpd    ymm0, ymm15, ymm3
        vaddpd    ymm3, ymm13, ymm1
        vmulpd    ymm11, ymm5, YMMWORD PTR [96+r13]
        vmulpd    ymm15, ymm5, ymm5
        vmulpd    ymm12, ymm3, ymm5
        vmulpd    ymm10, ymm6, YMMWORD PTR [32+r13]
        vaddpd    ymm13, ymm14, ymm11
        vaddpd    ymm7, ymm7, ymm0
        vaddpd    ymm0, ymm9, ymm12
        vmulpd    ymm13, ymm13, ymm15
        vaddpd    ymm1, ymm7, ymm10
        vaddpd    ymm3, ymm0, ymm13
        vaddpd    ymm5, ymm1, YMMWORD PTR [64+r13]
        vmulpd    ymm4, ymm4, ymm3
        vmovupd   ymm3, YMMWORD PTR [160+r13]
        vaddpd    ymm0, ymm5, ymm4
        vaddpd    ymm1, ymm2, ymm0
        vmovupd   ymm0, YMMWORD PTR [r13]
        vxorpd    ymm2, ymm1, YMMWORD PTR [192+r13]
        vblendvpd ymm0, ymm0, ymm2, ymm3
        jmp       _B3_2
        ALIGN     16

_B3_13::

__svml_tan4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan4_ha_e9_B1_B4:
	DD	622490113
	DD	7001202
	DD	3369059
	DD	3242075
	DD	3115091
	DD	2988107
	DD	2861123
	DD	2734139
	DD	2607155
	DD	2480171
	DD	2353187
	DD	2226203
	DD	17564433
	DD	1342505068

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_7
	DD	imagerel _unwind___svml_tan4_ha_e9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan4_ha_e9_B7_B11:
	DD	621022241
	DD	287756
	DD	340998
	DD	imagerel _B3_1
	DD	imagerel _B3_7
	DD	imagerel _unwind___svml_tan4_ha_e9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_7
	DD	imagerel _B3_12
	DD	imagerel _unwind___svml_tan4_ha_e9_B7_B11

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan4_ha_e9_B12_B12:
	DD	620757025
	DD	imagerel _B3_1
	DD	imagerel _B3_7
	DD	imagerel _unwind___svml_tan4_ha_e9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_12
	DD	imagerel _B3_13
	DD	imagerel _unwind___svml_tan4_ha_e9_B12_B12

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tan1_ha_l9

__svml_tan1_ha_l9	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L135::

        sub       rsp, 392
        lea       rdx, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [224+rsp], xmm15
        xor       eax, eax
        vmovups   XMMWORD PTR [240+rsp], xmm14
        vmovapd   xmm14, xmm0
        vmovups   XMMWORD PTR [256+rsp], xmm13
        vmovups   XMMWORD PTR [272+rsp], xmm12
        vmovups   XMMWORD PTR [288+rsp], xmm11
        vmovups   XMMWORD PTR [304+rsp], xmm10
        vmovups   XMMWORD PTR [320+rsp], xmm9
        vmovups   XMMWORD PTR [336+rsp], xmm8
        vmovups   XMMWORD PTR [352+rsp], xmm7
        vmovups   XMMWORD PTR [368+rsp], xmm6
        mov       QWORD PTR [216+rsp], r13
        lea       r13, QWORD PTR [143+rsp]
        vmovsd    xmm2, QWORD PTR [__svml_dtan_ha_data_internal+1152]
        and       r13, -64
        vfmadd132sd xmm0, xmm2, QWORD PTR [__svml_dtan_ha_data_internal]
        vmovsd    xmm5, QWORD PTR [__svml_dtan_ha_data_internal+256]
        vmovsd    xmm1, QWORD PTR [__svml_dtan_ha_data_internal+1024]
        vandpd    xmm8, xmm14, xmm1
        vcmpnlesd xmm13, xmm8, QWORD PTR [__svml_dtan_ha_data_internal+28160]
        vsubsd    xmm7, xmm0, xmm2
        vmovmskpd ecx, xmm13
        vmovapd   xmm3, xmm7
        vmovapd   xmm4, xmm7
        vmovsd    xmm2, QWORD PTR [__svml_dtan_ha_data_internal+320]
        vmovapd   xmm1, xmm7
        vfnmadd132sd xmm3, xmm14, QWORD PTR [__svml_dtan_ha_data_internal+64]
        vmovapd   xmm6, xmm7
        vfnmadd213sd xmm4, xmm5, xmm3
        vfnmadd213sd xmm1, xmm2, xmm4
        vsubsd    xmm3, xmm3, xmm4
        vfnmadd213sd xmm6, xmm5, xmm3
        vsubsd    xmm4, xmm1, xmm4
        vfmadd213sd xmm7, xmm2, xmm4
        mov       QWORD PTR [384+rsp], r13
        vsubsd    xmm10, xmm6, xmm7
        test      cl, 1
        jne       _B4_8

_B4_2::

        vmulsd    xmm7, xmm1, xmm1
        vpsllq    xmm4, xmm0, 3
        vmovsd    xmm6, QWORD PTR [__svml_dtan_ha_data_internal+896]
        vfmadd213sd xmm6, xmm7, QWORD PTR [__svml_dtan_ha_data_internal+832]
        vpand     xmm5, xmm4, XMMWORD PTR [_2il0floatpacket_58]
        vmovd     ecx, xmm5
        vmovsd    xmm5, QWORD PTR [__svml_dtan_ha_data_internal+960]
        vfmadd213sd xmm6, xmm7, QWORD PTR [__svml_dtan_ha_data_internal+768]
        vfmadd213sd xmm6, xmm7, QWORD PTR [__svml_dtan_ha_data_internal+704]
        vfmadd213sd xmm6, xmm7, QWORD PTR [__svml_dtan_ha_data_internal+640]
        vmulsd    xmm9, xmm6, xmm1
        vfnmsub213sd xmm9, xmm7, xmm10
        movsxd    rcx, ecx
        vsubsd    xmm0, xmm1, xmm9
        vmovsd    xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+384+rdx+rcx]
        vsubsd    xmm8, xmm1, xmm0
        vaddsd    xmm1, xmm0, xmm2
        vsubsd    xmm15, xmm8, xmm9
        vsubsd    xmm10, xmm1, xmm2
        vmovapd   xmm6, xmm2
        vmovapd   xmm4, xmm2
        vfnmadd213sd xmm6, xmm0, xmm5
        vsubsd    xmm11, xmm0, xmm10
        vdivsd    xmm8, xmm5, xmm6
        vmovsd    xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+512+rdx+rcx]
        vsubsd    xmm13, xmm6, xmm5
        vaddsd    xmm12, xmm11, xmm3
        vfmadd213sd xmm4, xmm0, xmm13
        vaddsd    xmm7, xmm12, xmm15
        vfmadd213sd xmm2, xmm15, xmm4
        vfmadd213sd xmm3, xmm0, xmm2
        vmovapd   xmm0, xmm8
        vfnmadd213sd xmm0, xmm6, xmm5
        vfmadd213sd xmm8, xmm0, xmm8
        vmovapd   xmm2, xmm8
        vmovapd   xmm13, xmm8
        vfnmadd213sd xmm2, xmm6, xmm5
        vfmadd213sd xmm13, xmm3, xmm2
        vfmadd213sd xmm8, xmm13, xmm8
        vmulsd    xmm9, xmm8, xmm1
        vfmsub213sd xmm6, xmm9, xmm1
        vfnmadd213sd xmm3, xmm9, xmm6
        vsubsd    xmm0, xmm3, xmm7
        vfnmadd213sd xmm0, xmm8, xmm9
        test      eax, eax
        jne       _B4_4

_B4_3::

        vmovups   xmm6, XMMWORD PTR [368+rsp]
        vmovups   xmm7, XMMWORD PTR [352+rsp]
        vmovups   xmm8, XMMWORD PTR [336+rsp]
        vmovups   xmm9, XMMWORD PTR [320+rsp]
        vmovups   xmm10, XMMWORD PTR [304+rsp]
        vmovups   xmm11, XMMWORD PTR [288+rsp]
        vmovups   xmm12, XMMWORD PTR [272+rsp]
        vmovups   xmm13, XMMWORD PTR [256+rsp]
        vmovups   xmm14, XMMWORD PTR [240+rsp]
        vmovups   xmm15, XMMWORD PTR [224+rsp]
        mov       r13, QWORD PTR [216+rsp]
        add       rsp, 392
        ret

_B4_4::

        vmovsd    QWORD PTR [r13], xmm14
        vmovsd    QWORD PTR [64+r13], xmm0
        jne       _B4_6

_B4_5::

        vmovsd    xmm0, QWORD PTR [64+r13]
        jmp       _B4_3

_B4_6::

        je        _B4_5

_B4_7::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]

        call      __svml_dtan_ha_cout_rare_internal
        jmp       _B4_5

_B4_8::

        vpand     xmm15, xmm14, XMMWORD PTR [_2il0floatpacket_29]
        vmovupd   XMMWORD PTR [48+rsp], xmm1
        vpsrlq    xmm1, xmm15, 52
        vmovd     ecx, xmm1
        vmovupd   XMMWORD PTR [64+rsp], xmm10
        vmovsd    xmm10, QWORD PTR [__svml_dtan_ha_data_internal+1088]
        vandpd    xmm6, xmm10, xmm8
        vmovupd   XMMWORD PTR [32+rsp], xmm0
        lea       r8d, DWORD PTR [rcx+rcx*2]
        vpand     xmm12, xmm14, XMMWORD PTR [_2il0floatpacket_30]
        vcmpeqsd  xmm0, xmm6, xmm10
        vpaddq    xmm8, xmm12, XMMWORD PTR [_2il0floatpacket_31]
        vmovmskpd eax, xmm0
        vmovdqu   xmm12, XMMWORD PTR [_2il0floatpacket_32]
        vpsrlq    xmm9, xmm8, 32
        vpand     xmm6, xmm8, xmm12
        shl       r8d, 3
        and       eax, 1
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rdx+r8]
        vpand     xmm15, xmm11, xmm12
        vpsrlq    xmm0, xmm11, 32
        vpmuludq  xmm11, xmm9, xmm15
        vmovq     xmm5, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rdx+r8]
        vpsrlq    xmm15, xmm11, 32
        vpmuludq  xmm11, xmm9, xmm0
        vpand     xmm2, xmm5, xmm12
        vpmuludq  xmm0, xmm6, xmm0
        vpmuludq  xmm1, xmm6, xmm2
        vpmuludq  xmm2, xmm9, xmm2
        vpsrlq    xmm0, xmm0, 32
        vpsrlq    xmm4, xmm5, 32
        vpaddq    xmm11, xmm11, xmm0
        vpmuludq  xmm3, xmm6, xmm4
        vpmuludq  xmm4, xmm9, xmm4
        vpand     xmm5, xmm1, xmm12
        vpsrlq    xmm1, xmm1, 32
        vmovq     xmm10, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rdx+r8]
        vpand     xmm0, xmm3, xmm12
        vpaddq    xmm5, xmm5, xmm11
        vpand     xmm7, xmm10, xmm12
        vpsrlq    xmm3, xmm3, 32
        vpaddq    xmm11, xmm15, xmm5
        vpaddq    xmm5, xmm2, xmm1
        vpmuludq  xmm8, xmm6, xmm7
        vpmuludq  xmm7, xmm9, xmm7
        vpaddq    xmm0, xmm0, xmm5
        vpsrlq    xmm15, xmm11, 32
        vpsrlq    xmm10, xmm10, 32
        vpaddq    xmm5, xmm15, xmm0
        vpaddq    xmm0, xmm4, xmm3
        vpmuludq  xmm6, xmm6, xmm10
        vpand     xmm1, xmm8, xmm12
        vpsrlq    xmm2, xmm5, 32
        vpaddq    xmm15, xmm1, xmm0
        vpsrlq    xmm9, xmm8, 32
        vpand     xmm0, xmm6, xmm12
        vpaddq    xmm4, xmm2, xmm15
        vpaddq    xmm15, xmm7, xmm9
        vpaddq    xmm1, xmm0, xmm15
        vpsrlq    xmm3, xmm4, 32
        vpand     xmm8, xmm4, xmm12
        vpaddq    xmm7, xmm3, xmm1
        vpsllq    xmm9, xmm7, 32
        vpand     xmm12, xmm11, xmm12
        vpaddq    xmm9, xmm9, xmm8
        vpand     xmm8, xmm14, XMMWORD PTR [_2il0floatpacket_33]
        vpsrlq    xmm11, xmm9, 12
        vpxor     xmm2, xmm8, XMMWORD PTR [_2il0floatpacket_34]
        vpsllq    xmm5, xmm5, 32
        vmovupd   xmm3, XMMWORD PTR [_2il0floatpacket_35]
        vpor      xmm4, xmm11, xmm2
        vpaddq    xmm5, xmm5, xmm12
        vaddsd    xmm7, xmm3, xmm4
        vpand     xmm12, xmm9, XMMWORD PTR [_2il0floatpacket_39]
        vsubsd    xmm6, xmm7, xmm3
        vpand     xmm10, xmm5, XMMWORD PTR [_2il0floatpacket_37]
        vpsllq    xmm9, xmm12, 40
        vpsrlq    xmm5, xmm5, 24
        vpsllq    xmm0, xmm10, 28
        vpor      xmm2, xmm9, xmm5
        vsubsd    xmm11, xmm4, xmm6
        vpxor     xmm4, xmm8, XMMWORD PTR [_2il0floatpacket_38]
        vpor      xmm3, xmm2, xmm4
        vpxor     xmm1, xmm8, XMMWORD PTR [_2il0floatpacket_36]
        vsubsd    xmm6, xmm3, xmm4
        vpor      xmm15, xmm0, xmm1
        vmovupd   xmm10, XMMWORD PTR [_2il0floatpacket_40]
        vsubsd    xmm15, xmm15, xmm1
        vaddsd    xmm1, xmm11, xmm6
        vmulsd    xmm2, xmm1, xmm10
        vsubsd    xmm11, xmm11, xmm1
        vmovapd   xmm0, xmm1
        vaddsd    xmm8, xmm6, xmm11
        vfmsub213sd xmm0, xmm10, xmm2
        vpand     xmm7, xmm7, XMMWORD PTR [_2il0floatpacket_45]
        vaddsd    xmm5, xmm8, xmm15
        vfmadd132sd xmm1, xmm0, QWORD PTR [_2il0floatpacket_54]
        vmovupd   xmm15, XMMWORD PTR [_2il0floatpacket_44]
        vandpd    xmm0, xmm14, XMMWORD PTR [_2il0floatpacket_43]
        vfmadd213sd xmm5, xmm10, xmm1
        vcmpgtsd  xmm4, xmm0, xmm15
        vcmplesd  xmm12, xmm0, xmm15
        vandpd    xmm10, xmm12, xmm14
        vandpd    xmm3, xmm4, xmm2
        vorpd     xmm12, xmm10, xmm3
        vandpd    xmm0, xmm4, xmm5
        vpshufd   xmm15, xmm12, 85
        vmovdqu   xmm1, XMMWORD PTR [_2il0floatpacket_55]
        vpsrld    xmm2, xmm15, 31
        vpshufd   xmm7, xmm7, 0
        vpsubd    xmm3, xmm1, xmm2
        vpaddd    xmm4, xmm7, xmm3
        vpxor     xmm5, xmm5, xmm5
        vpsrld    xmm6, xmm4, 4
        vpslld    xmm8, xmm6, 4
        vpsubd    xmm9, xmm7, xmm8
        vpshufd   xmm10, xmm9, 0
        vcvtdq2pd xmm2, xmm10
        vmovupd   xmm11, XMMWORD PTR [_2il0floatpacket_56]
        vmovapd   xmm3, xmm2
        vfmadd213sd xmm3, xmm11, xmm12
        vmovapd   xmm1, xmm2
        vpunpckldq xmm15, xmm6, xmm5
        vmovupd   xmm10, XMMWORD PTR [64+rsp]
        vfnmadd213sd xmm1, xmm11, xmm3
        vsubsd    xmm12, xmm12, xmm1
        vmovupd   xmm1, XMMWORD PTR [48+rsp]
        vblendvpd xmm1, xmm1, xmm3, xmm13
        vfmadd132sd xmm2, xmm12, QWORD PTR [_2il0floatpacket_57]
        vaddsd    xmm4, xmm2, xmm0
        vmovupd   xmm0, XMMWORD PTR [32+rsp]
        vblendvpd xmm0, xmm0, xmm15, xmm13
        vblendvpd xmm10, xmm10, xmm4, xmm13
        jmp       _B4_2
        ALIGN     16

_B4_9::

__svml_tan1_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan1_ha_l9_B1_B8:
	DD	1604097
	DD	1823866
	DD	1534066
	DD	1472617
	DD	1411168
	DD	1349719
	DD	1288270
	DD	1226821
	DD	1165372
	DD	1103923
	DD	1042470
	DD	981019
	DD	3211531

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_9
	DD	imagerel _unwind___svml_tan1_ha_l9_B1_B8

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tan8_ha_z0

__svml_tan8_ha_z0	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L164::

        sub       rsp, 1336
        xor       r8d, r8d
        vmovups   ZMMWORD PTR [1136+rsp], zmm15
        vmovups   ZMMWORD PTR [1200+rsp], zmm14
        vmovups   ZMMWORD PTR [1264+rsp], zmm6
        mov       QWORD PTR [1128+rsp], r13
        lea       r13, QWORD PTR [991+rsp]
        vmovaps   zmm4, zmm0
        and       r13, -64
        vmovups   zmm0, ZMMWORD PTR [__svml_dtan_ha_data_internal]
        vmovups   zmm1, ZMMWORD PTR [__svml_dtan_ha_data_internal+1152]
        vmovups   zmm2, ZMMWORD PTR [__svml_dtan_ha_data_internal+64]
        vmovups   zmm22, ZMMWORD PTR [__svml_dtan_ha_data_internal+256]
        vmovups   zmm3, ZMMWORD PTR [__svml_dtan_ha_data_internal+28160]
        vmovups   zmm23, ZMMWORD PTR [__svml_dtan_ha_data_internal+320]
        vfmadd213pd zmm0, zmm4, zmm1 {rn-sae}
        vandpd    zmm6, zmm4, ZMMWORD PTR [__svml_dtan_ha_data_internal+1024]
        vsubpd    zmm26, zmm0, zmm1 {rn-sae}
        vcmppd    k1, zmm6, zmm3, 22 {sae}
        vfnmadd213pd zmm2, zmm26, zmm4 {rn-sae}
        kortestw  k1, k1
        vmovaps   zmm5, zmm26
        vfnmadd213pd zmm5, zmm22, zmm2 {rn-sae}
        vmovaps   zmm3, zmm26
        vfnmadd213pd zmm3, zmm23, zmm5 {rn-sae}
        vsubpd    zmm25, zmm2, zmm5 {rn-sae}
        vsubpd    zmm24, zmm3, zmm5 {rn-sae}
        vfnmadd231pd zmm25, zmm22, zmm26 {rn-sae}
        vfmadd213pd zmm26, zmm23, zmm24 {rn-sae}
        mov       QWORD PTR [1328+rsp], r13
        vsubpd    zmm2, zmm25, zmm26 {rn-sae}
        jne       _B5_17

_B5_2::

        vmulpd    zmm23, zmm3, zmm3 {rn-sae}
        vmovups   zmm5, ZMMWORD PTR [__svml_dtan_ha_data_internal+384]
        vmovups   zmm1, ZMMWORD PTR [__svml_dtan_ha_data_internal+512]
        vmovups   zmm22, ZMMWORD PTR [__svml_dtan_ha_data_internal+832]
        vmovups   zmm6, ZMMWORD PTR [__svml_dtan_ha_data_internal+768]
        vmovups   zmm14, ZMMWORD PTR [__svml_dtan_ha_data_internal+704]
        vmovups   zmm15, ZMMWORD PTR [__svml_dtan_ha_data_internal+640]
        vmovups   zmm30, ZMMWORD PTR [__svml_dtan_ha_data_internal+960]
        vpermt2pd zmm5, zmm0, ZMMWORD PTR [__svml_dtan_ha_data_internal+448]
        vpermt2pd zmm1, zmm0, ZMMWORD PTR [__svml_dtan_ha_data_internal+576]
        vmovups   zmm0, ZMMWORD PTR [__svml_dtan_ha_data_internal+896]
        vfmadd231pd zmm22, zmm0, zmm23 {rn-sae}
        vfmadd213pd zmm22, zmm23, zmm6 {rn-sae}
        vfmadd213pd zmm22, zmm23, zmm14 {rn-sae}
        vfmadd213pd zmm22, zmm23, zmm15 {rn-sae}
        vmulpd    zmm24, zmm22, zmm3 {rn-sae}
        vfnmsub213pd zmm24, zmm23, zmm2 {rn-sae}
        vsubpd    zmm29, zmm3, zmm24 {rn-sae}
        vmovaps   zmm0, zmm30
        vfnmadd231pd zmm0, zmm29, zmm5 {rn-sae}
        vsubpd    zmm3, zmm3, zmm29 {rn-sae}
        vaddpd    zmm31, zmm29, zmm5 {rn-sae}
        vsubpd    zmm28, zmm0, zmm30 {rn-sae}
        vsubpd    zmm27, zmm3, zmm24 {rn-sae}
        vsubpd    zmm2, zmm31, zmm5 {rn-sae}
        vfmadd231pd zmm28, zmm29, zmm5 {rn-sae}
        vrcp14pd  zmm3, zmm0
        vfmadd213pd zmm5, zmm27, zmm28 {rn-sae}
        vsubpd    zmm25, zmm29, zmm2 {rn-sae}
        vfnmadd231pd zmm30, zmm0, zmm3 {rn-sae}
        vaddpd    zmm26, zmm25, zmm1 {rn-sae}
        vfmadd213pd zmm1, zmm29, zmm5 {rn-sae}
        vaddpd    zmm2, zmm26, zmm27 {rn-sae}
        vfmadd231pd zmm30, zmm1, zmm3 {rn-sae}
        vfmadd213pd zmm3, zmm30, zmm3 {rn-sae}
        vmulpd    zmm5, zmm3, zmm31 {rn-sae}
        vfmsub213pd zmm0, zmm5, zmm31 {rn-sae}
        vfnmadd213pd zmm1, zmm5, zmm0 {rn-sae}
        vsubpd    zmm0, zmm1, zmm2 {rn-sae}
        vfnmadd213pd zmm0, zmm3, zmm5 {rn-sae}
        test      r8d, r8d
        jne       _B5_4

_B5_3::

        vmovups   zmm6, ZMMWORD PTR [1264+rsp]
        vmovups   zmm14, ZMMWORD PTR [1200+rsp]
        vmovups   zmm15, ZMMWORD PTR [1136+rsp]
        mov       r13, QWORD PTR [1128+rsp]
        add       rsp, 1336
        ret

_B5_4::

        vstmxcsr  DWORD PTR [1120+rsp]

_B5_5::

        movzx     edx, WORD PTR [1120+rsp]
        mov       eax, edx
        or        eax, 8064
        cmp       edx, eax
        je        _B5_7

_B5_6::

        mov       DWORD PTR [1120+rsp], eax
        vldmxcsr  DWORD PTR [1120+rsp]

_B5_7::

        vmovups   ZMMWORD PTR [r13], zmm4
        vmovups   ZMMWORD PTR [64+r13], zmm0
        test      r8d, r8d
        jne       _B5_12

_B5_8::

        cmp       edx, eax
        je        _B5_3

_B5_9::

        vstmxcsr  DWORD PTR [1120+rsp]
        mov       eax, DWORD PTR [1120+rsp]

_B5_10::

        and       eax, -8065
        or        eax, edx
        mov       DWORD PTR [1120+rsp], eax
        vldmxcsr  DWORD PTR [1120+rsp]
        jmp       _B5_3

_B5_12::

        xor       ecx, ecx
        kmovw     WORD PTR [888+rsp], k4
        kmovw     WORD PTR [880+rsp], k5
        kmovw     WORD PTR [872+rsp], k6
        kmovw     WORD PTR [864+rsp], k7
        vmovups   ZMMWORD PTR [800+rsp], zmm7
        vmovups   ZMMWORD PTR [736+rsp], zmm8
        vmovups   ZMMWORD PTR [672+rsp], zmm9
        vmovups   ZMMWORD PTR [608+rsp], zmm10
        vmovups   ZMMWORD PTR [544+rsp], zmm11
        vmovups   ZMMWORD PTR [480+rsp], zmm12
        vmovups   ZMMWORD PTR [416+rsp], zmm13
        vmovups   ZMMWORD PTR [352+rsp], zmm16
        vmovups   ZMMWORD PTR [288+rsp], zmm17
        vmovups   ZMMWORD PTR [224+rsp], zmm18
        vmovups   ZMMWORD PTR [160+rsp], zmm19
        vmovups   ZMMWORD PTR [96+rsp], zmm20
        vmovups   ZMMWORD PTR [32+rsp], zmm21
        mov       QWORD PTR [912+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [904+rsp], rsi
        mov       esi, edx
        mov       QWORD PTR [896+rsp], rdi
        mov       edi, r8d
        mov       QWORD PTR [920+rsp], rbp
        mov       ebp, eax

_B5_13::

        bt        edi, ebx
        jc        _B5_16

_B5_14::

        inc       ebx
        cmp       ebx, 8
        jl        _B5_13

_B5_15::

        kmovw     k4, WORD PTR [888+rsp]
        mov       eax, ebp
        kmovw     k5, WORD PTR [880+rsp]
        kmovw     k6, WORD PTR [872+rsp]
        kmovw     k7, WORD PTR [864+rsp]
        vmovups   zmm7, ZMMWORD PTR [800+rsp]
        vmovups   zmm8, ZMMWORD PTR [736+rsp]
        vmovups   zmm9, ZMMWORD PTR [672+rsp]
        vmovups   zmm10, ZMMWORD PTR [608+rsp]
        vmovups   zmm11, ZMMWORD PTR [544+rsp]
        vmovups   zmm12, ZMMWORD PTR [480+rsp]
        vmovups   zmm13, ZMMWORD PTR [416+rsp]
        vmovups   zmm16, ZMMWORD PTR [352+rsp]
        vmovups   zmm17, ZMMWORD PTR [288+rsp]
        vmovups   zmm18, ZMMWORD PTR [224+rsp]
        vmovups   zmm19, ZMMWORD PTR [160+rsp]
        vmovups   zmm20, ZMMWORD PTR [96+rsp]
        vmovups   zmm21, ZMMWORD PTR [32+rsp]
        vmovups   zmm0, ZMMWORD PTR [64+r13]
        mov       rbx, QWORD PTR [912+rsp]
        mov       edx, esi
        mov       rsi, QWORD PTR [904+rsp]
        mov       rdi, QWORD PTR [896+rsp]
        mov       rbp, QWORD PTR [920+rsp]
        jmp       _B5_8

_B5_16::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dtan_ha_cout_rare_internal
        jmp       _B5_14

_B5_17::

        vpandq    zmm29, zmm4, QWORD BCST [_2il0floatpacket_60]
        lea       rax, QWORD PTR [__ImageBase]
        vpsrlq    zmm27, zmm29, 52
        mov       rdx, rax
        vmovups   zmm14, ZMMWORD PTR [__svml_dtan_ha_data_internal+1088]
        vpcmpeqd  k3, zmm0, zmm0
        vpsllq    zmm30, zmm27, 1
        vandpd    zmm1, zmm14, zmm6
        vpbroadcastq zmm31, QWORD PTR [_2il0floatpacket_59]
        vpaddq    zmm22, zmm30, zmm27
        vcmppd    k2, zmm1, zmm14, 4 {sae}
        vpsllq    zmm26, zmm22, 3
        vpmovqd   ymm25, zmm26
        mov       rcx, rax
        vmovaps   zmm28, zmm31
        vpxord    zmm23, zmm23, zmm23
        vpandnq   zmm28{k2}, zmm1, zmm1
        vpcmpeqd  k2, zmm0, zmm0
        vcmppd    k0, zmm28, zmm28, 3 {sae}
        vgatherdpd zmm23{k3}, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rax+ymm25]
        vpcmpeqd  k3, zmm0, zmm0
        vpbroadcastq zmm28, QWORD PTR [_2il0floatpacket_63]
        vpsrlq    zmm1, zmm23, 32
        kmovw     r8d, k0
        vpxord    zmm15, zmm15, zmm15
        vgatherdpd zmm15{k2}, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rdx+ymm25]
        vpsrlq    zmm22, zmm15, 32
        vpxord    zmm29, zmm29, zmm29
        vpandq    zmm24, zmm4, QWORD BCST [_2il0floatpacket_61]
        vpaddq    zmm14, zmm24, QWORD BCST [_2il0floatpacket_62]
        vgatherdpd zmm29{k3}, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rcx+ymm25]
        vpsrlq    zmm30, zmm14, 32
        vpsrlq    zmm6, zmm29, 32
        vpmullq   zmm25, zmm30, zmm22
        vpandq    zmm5, zmm15, zmm28
        vpandq    zmm14, zmm14, zmm28
        vpandq    zmm15, zmm29, zmm28
        vpmullq   zmm24, zmm30, zmm5
        vpmullq   zmm29, zmm14, zmm1
        vpmullq   zmm1, zmm14, zmm22
        vpmullq   zmm22, zmm14, zmm5
        vpmullq   zmm5, zmm14, zmm6
        vpandq    zmm27, zmm23, zmm28
        vpmullq   zmm26, zmm30, zmm27
        vpmullq   zmm27, zmm14, zmm27
        vpmullq   zmm23, zmm30, zmm6
        vpmullq   zmm30, zmm30, zmm15
        vpsrlq    zmm14, zmm5, 32
        vpsrlq    zmm6, zmm22, 32
        vpsrlq    zmm5, zmm27, 32
        vpsrlq    zmm30, zmm30, 32
        vpsrlq    zmm15, zmm1, 32
        vpaddq    zmm23, zmm23, zmm14
        vpaddq    zmm24, zmm24, zmm6
        vpaddq    zmm26, zmm26, zmm5
        vpaddq    zmm25, zmm25, zmm15
        vpandq    zmm6, zmm22, zmm28
        vpaddq    zmm22, zmm6, zmm23
        vpaddq    zmm6, zmm30, zmm22
        vpandq    zmm29, zmm29, zmm28
        vpaddq    zmm5, zmm29, zmm26
        vpsrlq    zmm26, zmm6, 32
        vpandq    zmm1, zmm1, zmm28
        vpaddq    zmm23, zmm1, zmm24
        vpaddq    zmm29, zmm26, zmm23
        vpsrlq    zmm24, zmm29, 32
        vpandq    zmm27, zmm27, zmm28
        vpaddq    zmm25, zmm27, zmm25
        vpaddq    zmm14, zmm24, zmm25
        vpsrlq    zmm15, zmm14, 32
        vpaddq    zmm1, zmm15, zmm5
        vpandq    zmm27, zmm6, zmm28
        vpandq    zmm30, zmm14, zmm28
        vpsllq    zmm28, zmm1, 32
        vpsllq    zmm1, zmm29, 32
        vpaddq    zmm22, zmm28, zmm30
        vpaddq    zmm27, zmm1, zmm27
        vpbroadcastq zmm28, QWORD PTR [_2il0floatpacket_66]
        vpsrlq    zmm29, zmm22, 12
        vpsrlq    zmm1, zmm27, 24
        vpandq    zmm6, zmm4, QWORD BCST [_2il0floatpacket_64]
        vpternlogq zmm29, zmm6, QWORD BCST [_2il0floatpacket_65], 246
        vaddpd    zmm24, zmm28, zmm29 {rn-sae}
        vsubpd    zmm23, zmm24, zmm28 {rn-sae}
        vsubpd    zmm30, zmm29, zmm23 {rn-sae}
        vpandq    zmm22, zmm22, QWORD BCST [_2il0floatpacket_71]
        vpsllq    zmm14, zmm22, 40
        vpxorq    zmm28, zmm6, QWORD BCST [_2il0floatpacket_70]
        vpternlogq zmm1, zmm14, zmm28, 254
        vsubpd    zmm22, zmm1, zmm28 {rn-sae}
        vmovaps   zmm1, zmm31
        vpandq    zmm25, zmm27, QWORD BCST [_2il0floatpacket_69]
        vpsllq    zmm26, zmm25, 28
        vpxorq    zmm5, zmm6, QWORD BCST [_2il0floatpacket_68]
        vporq     zmm15, zmm26, zmm5
        vsubpd    zmm23, zmm15, zmm5 {rn-sae}
        vaddpd    zmm15, zmm30, zmm22 {rn-sae}
        vpbroadcastq zmm26, QWORD PTR [_2il0floatpacket_72]
        vpbroadcastq zmm5, QWORD PTR [_2il0floatpacket_75]
        vandpd    zmm6, zmm4, QWORD BCST [_2il0floatpacket_74]
        vsubpd    zmm30, zmm30, zmm15 {rn-sae}
        vmulpd    zmm14, zmm15, zmm26 {rn-sae}
        vcmppd    k2, zmm6, zmm5, 26 {sae}
        vcmppd    k3, zmm6, zmm5, 22 {sae}
        vaddpd    zmm27, zmm22, zmm30 {rn-sae}
        vmovaps   zmm25, zmm26
        vfmsub213pd zmm25, zmm15, zmm14 {rn-sae}
        vaddpd    zmm22, zmm27, zmm23 {rn-sae}
        vpbroadcastq zmm23, QWORD PTR [_2il0floatpacket_78]
        vpandq    zmm29, zmm24, QWORD BCST [_2il0floatpacket_67]
        vpbroadcastq zmm24, QWORD PTR [_2il0floatpacket_73]
        vfmadd213pd zmm15, zmm24, zmm25 {rn-sae}
        vfmadd213pd zmm22, zmm26, zmm15 {rn-sae}
        vpmovqd   ymm15, zmm29
        vpandnq   zmm1{k2}, zmm6, zmm6
        vandpd    zmm28, zmm1, zmm14
        vpandnq   zmm31{k3}, zmm6, zmm6
        vpternlogq zmm28, zmm31, zmm4, 248
        vandpd    zmm31, zmm1, zmm22
        vpbroadcastq zmm22, QWORD PTR [_2il0floatpacket_77]
        vpsrlq    zmm29, zmm28, 32
        vmovdqu   ymm6, YMMWORD PTR [_2il0floatpacket_76]
        vpmovqd   ymm5, zmm29
        vpsrld    ymm14, ymm5, 31
        vpsubd    ymm1, ymm6, ymm14
        vpaddd    ymm5, ymm15, ymm1
        vpsrld    ymm1, ymm5, 4
        vpslld    ymm6, ymm1, 4
        vpmovzxdq zmm0{k1}, ymm1
        vpsubd    ymm15, ymm15, ymm6
        vmovaps   zmm24, zmm28
        vcvtdq2pd zmm25, ymm15
        vfmadd231pd zmm24, zmm22, zmm25 {rn-sae}
        vfnmadd213pd zmm22, zmm25, zmm24 {rn-sae}
        vblendmpd zmm3{k1}, zmm3, zmm24
        vsubpd    zmm28, zmm28, zmm22 {rn-sae}
        vfmadd213pd zmm25, zmm23, zmm28 {rn-sae}
        vaddpd    zmm2{k1}, zmm25, zmm31 {rn-sae}
        jmp       _B5_2
        ALIGN     16

_B5_18::

__svml_tan8_ha_z0 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan8_ha_z0_B1_B10:
	DD	669441
	DD	9294903
	DD	5204015
	DD	4974628
	DD	4716569
	DD	10944779

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_12
	DD	imagerel _unwind___svml_tan8_ha_z0_B1_B10

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan8_ha_z0_B12_B16:
	DD	2808865
	DD	7558364
	DD	7369937
	DD	7431367
	DD	7484605
	DD	153781
	DD	411818
	DD	669855
	DD	927892
	DD	1185929
	DD	1443966
	DD	1759347
	DD	2017384
	DD	2275421
	DD	2533458
	DD	2791495
	DD	3049532
	DD	3307569
	DD	7109414
	DD	7170845
	DD	7232276
	DD	7293707
	DD	imagerel _B5_1
	DD	imagerel _B5_12
	DD	imagerel _unwind___svml_tan8_ha_z0_B1_B10

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_12
	DD	imagerel _B5_17
	DD	imagerel _unwind___svml_tan8_ha_z0_B12_B16

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan8_ha_z0_B17_B17:
	DD	33
	DD	imagerel _B5_1
	DD	imagerel _B5_12
	DD	imagerel _unwind___svml_tan8_ha_z0_B1_B10

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_17
	DD	imagerel _B5_18
	DD	imagerel _unwind___svml_tan8_ha_z0_B17_B17

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tan2_ha_ex

__svml_tan2_ha_ex	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L215::

        sub       rsp, 488
        xor       edx, edx
        movups    XMMWORD PTR [240+rsp], xmm15
        movups    XMMWORD PTR [256+rsp], xmm14
        movups    XMMWORD PTR [272+rsp], xmm13
        movups    XMMWORD PTR [288+rsp], xmm12
        movups    XMMWORD PTR [304+rsp], xmm11
        movups    XMMWORD PTR [320+rsp], xmm10
        movups    XMMWORD PTR [336+rsp], xmm9
        movups    XMMWORD PTR [352+rsp], xmm8
        movups    XMMWORD PTR [368+rsp], xmm7
        movups    XMMWORD PTR [384+rsp], xmm6
        mov       QWORD PTR [464+rsp], r13
        lea       r13, QWORD PTR [159+rsp]
        movups    xmm3, XMMWORD PTR [__svml_dtan_ha_data_internal+1024]
        and       r13, -64
        movaps    xmm14, xmm3
        andnps    xmm3, xmm0
        andps     xmm14, xmm0
        movaps    xmm5, xmm14
        cmpnlepd  xmm5, XMMWORD PTR [__svml_dtan_ha_data_internal+28160]
        movups    XMMWORD PTR [448+rsp], xmm5
        movups    xmm5, XMMWORD PTR [__svml_dtan_ha_data_internal+1472]
        mulpd     xmm5, xmm14
        movups    xmm4, XMMWORD PTR [__svml_dtan_ha_data_internal+1152]
        addpd     xmm5, xmm4
        movups    XMMWORD PTR [416+rsp], xmm3
        movaps    xmm3, xmm5
        subpd     xmm3, xmm4
        movups    xmm1, XMMWORD PTR [__svml_dtan_ha_data_internal+1216]
        mulpd     xmm1, xmm3
        movups    xmm8, XMMWORD PTR [__svml_dtan_ha_data_internal+1280]
        mulpd     xmm8, xmm3
        movups    XMMWORD PTR [432+rsp], xmm14
        subpd     xmm14, xmm1
        movups    XMMWORD PTR [400+rsp], xmm0
        movaps    xmm0, xmm14
        subpd     xmm0, xmm8
        subpd     xmm14, xmm0
        movups    xmm7, XMMWORD PTR [__svml_dtan_ha_data_internal+1344]
        mulpd     xmm7, xmm3
        subpd     xmm14, xmm8
        pand      xmm5, XMMWORD PTR [__svml_dtan_ha_data_internal+1408]
        movaps    xmm8, xmm0
        pand      xmm5, XMMWORD PTR [_2il0floatpacket_26]
        subpd     xmm14, xmm7
        pextrw    r8d, xmm5, 4
        movd      eax, xmm5
        addpd     xmm8, xmm14
        imul      ecx, eax, 104
        imul      r9d, r8d, 104
        subpd     xmm0, xmm8
        lea       rax, QWORD PTR [__ImageBase]
        addpd     xmm14, xmm0
        movq      xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rax+rcx]
        movhpd    xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rax+r9]
        movaps    xmm15, xmm1
        subpd     xmm15, xmm8
        movq      xmm13, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rax+rcx]
        subpd     xmm1, xmm15
        movhpd    xmm13, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rax+r9]
        movups    XMMWORD PTR [64+rsp], xmm13
        movups    xmm13, XMMWORD PTR [_2il0floatpacket_27]
        movaps    xmm0, xmm13
        andps     xmm0, xmm15
        subpd     xmm1, xmm8
        subpd     xmm15, xmm0
        subpd     xmm1, xmm14
        movq      xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rax+rcx]
        movhpd    xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rax+r9]
        addpd     xmm2, xmm15
        addpd     xmm1, xmm2
        cvtpd2ps  xmm2, xmm0
        movlhps   xmm2, xmm2
        rcpps     xmm2, xmm2
        cvtps2pd  xmm2, xmm2
        andps     xmm2, xmm13
        mulpd     xmm0, xmm2
        movups    XMMWORD PTR [32+rsp], xmm14
        movaps    xmm13, xmm2
        movups    xmm14, XMMWORD PTR [_2il0floatpacket_28]
        movaps    xmm15, xmm14
        subpd     xmm15, xmm0
        movaps    xmm0, xmm15
        mulpd     xmm13, xmm15
        mulpd     xmm0, xmm15
        addpd     xmm13, xmm2
        addpd     xmm0, xmm14
        mulpd     xmm13, xmm0
        mulpd     xmm1, xmm13
        movaps    xmm14, xmm1
        mulpd     xmm1, xmm1
        subpd     xmm14, xmm15
        movq      xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rax+rcx]
        subpd     xmm1, xmm14
        movhpd    xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rax+r9]
        mulpd     xmm6, xmm8
        mulpd     xmm13, xmm1
        movq      xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rax+rcx]
        movaps    xmm1, xmm8
        movhpd    xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rax+r9]
        movups    XMMWORD PTR [48+rsp], xmm9
        movq      xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rax+rcx]
        movhpd    xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rax+r9]
        mulpd     xmm1, xmm9
        movq      xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rax+rcx]
        movq      xmm4, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rax+rcx]
        movhpd    xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rax+r9]
        movhpd    xmm4, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rax+r9]
        mulpd     xmm7, xmm8
        addpd     xmm4, xmm6
        movaps    xmm6, xmm8
        mulpd     xmm6, xmm8
        movq      xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+rcx]
        movq      xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rax+rcx]
        movq      xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rax+rcx]
        movhpd    xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+r9]
        movhpd    xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rax+r9]
        movhpd    xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rax+r9]
        mulpd     xmm2, xmm3
        mulpd     xmm10, xmm8
        mulpd     xmm3, xmm13
        mulpd     xmm4, xmm6
        movaps    xmm13, xmm12
        addpd     xmm13, xmm1
        movq      xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+rcx]
        movaps    xmm0, xmm13
        movhpd    xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+r9]
        addpd     xmm7, xmm11
        addpd     xmm9, xmm11
        subpd     xmm12, xmm13
        addpd     xmm0, xmm2
        addpd     xmm7, xmm4
        addpd     xmm1, xmm12
        subpd     xmm2, xmm0
        addpd     xmm3, xmm1
        addpd     xmm13, xmm2
        movups    xmm11, XMMWORD PTR [64+rsp]
        mulpd     xmm11, xmm6
        mulpd     xmm6, xmm6
        addpd     xmm3, xmm13
        movq      xmm5, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rax+rcx]
        movhpd    xmm5, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rax+r9]
        addpd     xmm5, xmm10
        addpd     xmm5, xmm11
        mulpd     xmm5, xmm6
        movups    xmm4, XMMWORD PTR [32+rsp]
        mulpd     xmm4, xmm9
        addpd     xmm7, xmm5
        addpd     xmm3, xmm4
        mulpd     xmm8, xmm7
        movups    xmm10, XMMWORD PTR [48+rsp]
        addpd     xmm10, xmm3
        addpd     xmm10, xmm8
        addpd     xmm0, xmm10
        movups    xmm1, XMMWORD PTR [448+rsp]
        movmskpd  r10d, xmm1
        xorps     xmm0, XMMWORD PTR [416+rsp]
        mov       QWORD PTR [472+rsp], r13
        test      r10d, r10d
        jne       _B6_12

_B6_2::

        test      edx, edx
        jne       _B6_4

_B6_3::

        movups    xmm6, XMMWORD PTR [384+rsp]
        movups    xmm7, XMMWORD PTR [368+rsp]
        movups    xmm8, XMMWORD PTR [352+rsp]
        movups    xmm9, XMMWORD PTR [336+rsp]
        movups    xmm10, XMMWORD PTR [320+rsp]
        movups    xmm11, XMMWORD PTR [304+rsp]
        movups    xmm12, XMMWORD PTR [288+rsp]
        movups    xmm13, XMMWORD PTR [272+rsp]
        movups    xmm14, XMMWORD PTR [256+rsp]
        movups    xmm15, XMMWORD PTR [240+rsp]
        mov       r13, QWORD PTR [464+rsp]
        add       rsp, 488
        ret

_B6_4::

        movups    xmm1, XMMWORD PTR [400+rsp]
        movups    XMMWORD PTR [r13], xmm1
        movups    XMMWORD PTR [64+r13], xmm0
        je        _B6_3

_B6_7::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B6_8::

        mov       ecx, ebx
        mov       eax, 1
        shl       eax, cl
        test      esi, eax
        jne       _B6_11

_B6_9::

        inc       ebx
        cmp       ebx, 2
        jl        _B6_8

_B6_10::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [64+r13]
        jmp       _B6_3

_B6_11::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dtan_ha_cout_rare_internal
        jmp       _B6_9

_B6_12::

        movups    xmm12, XMMWORD PTR [432+rsp]
        movdqu    xmm7, XMMWORD PTR [_2il0floatpacket_29]
        pand      xmm7, xmm12
        psrlq     xmm7, 52
        movd      ecx, xmm7
        movups    xmm6, XMMWORD PTR [__svml_dtan_ha_data_internal+1088]
        movdqu    xmm13, XMMWORD PTR [_2il0floatpacket_30]
        movaps    xmm4, xmm6
        pextrw    r9d, xmm7, 4
        pand      xmm13, xmm12
        lea       r8d, DWORD PTR [rcx+rcx*2]
        shl       r8d, 3
        lea       r10d, DWORD PTR [r9+r9*2]
        andps     xmm4, xmm12
        paddq     xmm13, XMMWORD PTR [_2il0floatpacket_31]
        cmpeqpd   xmm4, xmm6
        shl       r10d, 3
        movdqa    xmm11, xmm13
        movq      xmm5, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rax+r8]
        psrlq     xmm11, 32
        movhpd    xmm5, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rax+r10]
        movdqu    xmm8, XMMWORD PTR [_2il0floatpacket_32]
        movaps    xmm3, xmm5
        movq      xmm7, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rax+r8]
        psrlq     xmm3, 32
        movups    XMMWORD PTR [32+rsp], xmm0
        pand      xmm13, xmm8
        movdqa    xmm0, xmm11
        pand      xmm5, xmm8
        movhpd    xmm7, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rax+r10]
        movdqa    xmm9, xmm13
        pmuludq   xmm0, xmm3
        movaps    xmm2, xmm7
        pmuludq   xmm3, xmm13
        pmuludq   xmm5, xmm11
        movmskpd  edx, xmm4
        pand      xmm7, xmm8
        movdqa    xmm4, xmm13
        pmuludq   xmm4, xmm7
        psrlq     xmm3, 32
        pmuludq   xmm7, xmm11
        paddq     xmm0, xmm3
        psrlq     xmm2, 32
        movdqa    xmm15, xmm8
        pmuludq   xmm9, xmm2
        pand      xmm15, xmm4
        pmuludq   xmm2, xmm11
        paddq     xmm15, xmm0
        movq      xmm14, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rax+r8]
        psrlq     xmm5, 32
        movups    XMMWORD PTR [448+rsp], xmm1
        movdqa    xmm1, xmm8
        movhpd    xmm14, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rax+r10]
        psrlq     xmm4, 32
        pand      xmm1, xmm14
        movdqa    xmm10, xmm13
        paddq     xmm5, xmm15
        paddq     xmm7, xmm4
        pmuludq   xmm10, xmm1
        pmuludq   xmm11, xmm1
        movdqa    xmm6, xmm8
        movaps    xmm3, xmm5
        pand      xmm6, xmm9
        psrlq     xmm3, 32
        paddq     xmm6, xmm7
        psrlq     xmm9, 32
        psrlq     xmm14, 32
        paddq     xmm3, xmm6
        paddq     xmm2, xmm9
        pmuludq   xmm13, xmm14
        movdqa    xmm4, xmm8
        movdqa    xmm7, xmm3
        pand      xmm4, xmm10
        psrlq     xmm7, 32
        paddq     xmm4, xmm2
        psrlq     xmm10, 32
        pand      xmm13, xmm8
        paddq     xmm7, xmm4
        paddq     xmm11, xmm10
        movdqa    xmm6, xmm7
        pand      xmm7, xmm8
        paddq     xmm13, xmm11
        psrlq     xmm6, 32
        psllq     xmm3, 32
        paddq     xmm6, xmm13
        psllq     xmm6, 32
        pand      xmm5, xmm8
        paddq     xmm6, xmm7
        paddq     xmm3, xmm5
        movdqu    xmm4, XMMWORD PTR [_2il0floatpacket_33]
        movdqa    xmm7, xmm6
        movdqu    xmm9, XMMWORD PTR [_2il0floatpacket_34]
        pand      xmm4, xmm12
        movups    xmm2, XMMWORD PTR [_2il0floatpacket_35]
        psrlq     xmm7, 12
        pxor      xmm9, xmm4
        movaps    xmm10, xmm2
        por       xmm7, xmm9
        addpd     xmm10, xmm7
        movaps    xmm5, xmm10
        subpd     xmm5, xmm2
        movdqu    xmm9, XMMWORD PTR [_2il0floatpacket_39]
        movdqu    xmm11, XMMWORD PTR [_2il0floatpacket_37]
        pand      xmm9, xmm6
        movdqu    xmm1, XMMWORD PTR [_2il0floatpacket_36]
        pand      xmm11, xmm3
        psllq     xmm9, 40
        psrlq     xmm3, 24
        pxor      xmm1, xmm4
        por       xmm9, xmm3
        pxor      xmm4, XMMWORD PTR [_2il0floatpacket_38]
        psllq     xmm11, 28
        subpd     xmm7, xmm5
        por       xmm9, xmm4
        movaps    xmm6, xmm7
        subpd     xmm9, xmm4
        addpd     xmm6, xmm9
        subpd     xmm7, xmm6
        por       xmm11, xmm1
        subpd     xmm11, xmm1
        addpd     xmm9, xmm7
        movups    xmm14, XMMWORD PTR [_2il0floatpacket_27]
        movaps    xmm3, xmm14
        andps     xmm3, xmm6
        addpd     xmm11, xmm9
        subpd     xmm6, xmm3
        mulpd     xmm11, XMMWORD PTR [_2il0floatpacket_40]
        movups    xmm8, XMMWORD PTR [_2il0floatpacket_41]
        movups    xmm1, XMMWORD PTR [_2il0floatpacket_42]
        movaps    xmm2, xmm8
        mulpd     xmm2, xmm3
        mulpd     xmm8, xmm6
        mulpd     xmm3, xmm1
        mulpd     xmm1, xmm6
        addpd     xmm8, xmm3
        addpd     xmm11, xmm1
        addpd     xmm8, xmm11
        pand      xmm10, XMMWORD PTR [_2il0floatpacket_45]
        movaps    xmm0, xmm8
        pand      xmm10, XMMWORD PTR [_2il0floatpacket_26]
        movups    xmm4, XMMWORD PTR [_2il0floatpacket_44]
        movups    xmm11, XMMWORD PTR [_2il0floatpacket_43]
        movaps    xmm13, xmm4
        movd      r11d, xmm10
        andps     xmm11, xmm12
        pextrw    r8d, xmm10, 4
        cmpltpd   xmm13, xmm11
        cmplepd   xmm11, xmm4
        addpd     xmm0, xmm2
        imul      ecx, r11d, 104
        imul      r9d, r8d, 104
        andps     xmm11, xmm12
        movq      xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rax+rcx]
        movq      xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rax+rcx]
        subpd     xmm2, xmm0
        movhpd    xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rax+r9]
        andps     xmm0, xmm13
        movhpd    xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rax+r9]
        orps      xmm11, xmm0
        movups    XMMWORD PTR [64+rsp], xmm12
        movaps    xmm12, xmm1
        subpd     xmm12, xmm11
        addpd     xmm8, xmm2
        subpd     xmm1, xmm12
        movq      xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rax+rcx]
        andps     xmm13, xmm8
        movhpd    xmm0, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rax+r9]
        movups    XMMWORD PTR [80+rsp], xmm0
        movaps    xmm0, xmm14
        subpd     xmm1, xmm11
        andps     xmm0, xmm12
        subpd     xmm12, xmm0
        subpd     xmm1, xmm13
        movq      xmm15, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rax+rcx]
        movups    XMMWORD PTR [48+rsp], xmm13
        cvtpd2ps  xmm13, xmm0
        movhpd    xmm15, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rax+r9]
        addpd     xmm15, xmm12
        movlhps   xmm13, xmm13
        addpd     xmm1, xmm15
        rcpps     xmm15, xmm13
        cvtps2pd  xmm13, xmm15
        andps     xmm13, xmm14
        mulpd     xmm0, xmm13
        movups    xmm15, XMMWORD PTR [_2il0floatpacket_28]
        movaps    xmm12, xmm13
        movaps    xmm14, xmm15
        subpd     xmm14, xmm0
        movaps    xmm0, xmm14
        mulpd     xmm12, xmm14
        mulpd     xmm0, xmm14
        addpd     xmm12, xmm13
        addpd     xmm15, xmm0
        mulpd     xmm12, xmm15
        mulpd     xmm1, xmm12
        movq      xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rax+rcx]
        movaps    xmm0, xmm1
        movhpd    xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rax+r9]
        subpd     xmm0, xmm14
        mulpd     xmm1, xmm1
        movaps    xmm14, xmm11
        mulpd     xmm14, xmm7
        subpd     xmm1, xmm0
        movq      xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rax+rcx]
        movhpd    xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rax+r9]
        movaps    xmm0, xmm2
        addpd     xmm0, xmm14
        mulpd     xmm12, xmm1
        subpd     xmm2, xmm0
        movq      xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rax+rcx]
        movaps    xmm1, xmm0
        movq      xmm5, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rax+rcx]
        movhpd    xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rax+r9]
        movhpd    xmm5, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rax+r9]
        addpd     xmm14, xmm2
        mulpd     xmm6, xmm11
        mulpd     xmm5, xmm11
        movaps    xmm2, xmm11
        mulpd     xmm2, xmm11
        movq      xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+rcx]
        movq      xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rax+rcx]
        movhpd    xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+r9]
        movhpd    xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rax+r9]
        mulpd     xmm13, xmm10
        mulpd     xmm8, xmm11
        addpd     xmm1, xmm13
        mulpd     xmm10, xmm12
        subpd     xmm13, xmm1
        addpd     xmm10, xmm14
        addpd     xmm0, xmm13
        movq      xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+rcx]
        movq      xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rax+rcx]
        movhpd    xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+r9]
        movhpd    xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rax+r9]
        addpd     xmm6, xmm9
        addpd     xmm7, xmm9
        addpd     xmm3, xmm5
        addpd     xmm10, xmm0
        mulpd     xmm3, xmm2
        movups    xmm9, XMMWORD PTR [80+rsp]
        mulpd     xmm9, xmm2
        mulpd     xmm2, xmm2
        addpd     xmm6, xmm3
        movq      xmm4, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rax+rcx]
        movhpd    xmm4, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rax+r9]
        addpd     xmm4, xmm8
        addpd     xmm4, xmm9
        mulpd     xmm4, xmm2
        movups    xmm3, XMMWORD PTR [48+rsp]
        mulpd     xmm3, xmm7
        addpd     xmm6, xmm4
        addpd     xmm10, xmm3
        mulpd     xmm11, xmm6
        movups    xmm4, XMMWORD PTR [64+rsp]
        addpd     xmm4, xmm10
        addpd     xmm4, xmm11
        addpd     xmm1, xmm4
        movups    xmm5, XMMWORD PTR [448+rsp]
        xorps     xmm1, XMMWORD PTR [416+rsp]
        movaps    xmm0, xmm5
        andnps    xmm0, XMMWORD PTR [32+rsp]
        andps     xmm1, xmm5
        orps      xmm0, xmm1
        jmp       _B6_2
        ALIGN     16

_B6_13::

__svml_tan2_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan2_ha_ex_B1_B4:
	DD	1600769
	DD	3855469
	DD	1599589
	DD	1538141
	DD	1476693
	DD	1415244
	DD	1353795
	DD	1292346
	DD	1230897
	DD	1169448
	DD	1107999
	DD	1046550
	DD	3997963

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_tan2_ha_ex_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan2_ha_ex_B7_B11:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_tan2_ha_ex_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_7
	DD	imagerel _B6_12
	DD	imagerel _unwind___svml_tan2_ha_ex_B7_B11

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan2_ha_ex_B12_B12:
	DD	33
	DD	imagerel _B6_1
	DD	imagerel _B6_7
	DD	imagerel _unwind___svml_tan2_ha_ex_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_12
	DD	imagerel _B6_13
	DD	imagerel _unwind___svml_tan2_ha_ex_B12_B12

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tan2_ha_l9

__svml_tan2_ha_l9	PROC	EXPORT

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L267::

        sub       rsp, 408
        lea       rax, QWORD PTR [__ImageBase]
        vmovups   XMMWORD PTR [224+rsp], xmm15
        xor       edx, edx
        vmovups   XMMWORD PTR [240+rsp], xmm14
        vmovapd   xmm14, xmm0
        vmovups   XMMWORD PTR [256+rsp], xmm13
        vmovups   XMMWORD PTR [272+rsp], xmm12
        vmovups   XMMWORD PTR [288+rsp], xmm11
        vmovups   XMMWORD PTR [304+rsp], xmm10
        vmovups   XMMWORD PTR [320+rsp], xmm9
        vmovups   XMMWORD PTR [336+rsp], xmm8
        vmovups   XMMWORD PTR [352+rsp], xmm7
        vmovups   XMMWORD PTR [368+rsp], xmm6
        mov       QWORD PTR [384+rsp], r13
        lea       r13, QWORD PTR [143+rsp]
        vmovupd   xmm0, XMMWORD PTR [__svml_dtan_ha_data_internal]
        and       r13, -64
        vmovupd   xmm3, XMMWORD PTR [__svml_dtan_ha_data_internal+1152]
        vfmadd213pd xmm0, xmm14, xmm3
        vmovupd   xmm1, XMMWORD PTR [__svml_dtan_ha_data_internal+64]
        vmovupd   xmm4, XMMWORD PTR [__svml_dtan_ha_data_internal+256]
        vmovupd   xmm5, XMMWORD PTR [__svml_dtan_ha_data_internal+320]
        vandpd    xmm7, xmm14, XMMWORD PTR [__svml_dtan_ha_data_internal+1024]
        vsubpd    xmm6, xmm0, xmm3
        vcmpnlepd xmm13, xmm7, XMMWORD PTR [__svml_dtan_ha_data_internal+28160]
        vfnmadd213pd xmm1, xmm6, xmm14
        vmovapd   xmm2, xmm6
        vmovapd   xmm3, xmm6
        vmovmskpd ecx, xmm13
        vfnmadd213pd xmm2, xmm4, xmm1
        vfnmadd213pd xmm3, xmm5, xmm2
        vsubpd    xmm1, xmm1, xmm2
        vsubpd    xmm2, xmm3, xmm2
        vfnmadd231pd xmm1, xmm4, xmm6
        vfmadd213pd xmm6, xmm5, xmm2
        mov       QWORD PTR [392+rsp], r13
        vsubpd    xmm11, xmm1, xmm6
        test      ecx, ecx
        jne       _B7_12

_B7_2::

        vmulpd    xmm9, xmm3, xmm3
        vmovupd   xmm8, XMMWORD PTR [__svml_dtan_ha_data_internal+896]
        vpsllq    xmm4, xmm0, 3
        vfmadd213pd xmm8, xmm9, XMMWORD PTR [__svml_dtan_ha_data_internal+832]
        vpand     xmm5, xmm4, XMMWORD PTR [_2il0floatpacket_58]
        vmovd     ecx, xmm5
        vfmadd213pd xmm8, xmm9, XMMWORD PTR [__svml_dtan_ha_data_internal+768]
        vfmadd213pd xmm8, xmm9, XMMWORD PTR [__svml_dtan_ha_data_internal+704]
        vfmadd213pd xmm8, xmm9, XMMWORD PTR [__svml_dtan_ha_data_internal+640]
        vmulpd    xmm10, xmm3, xmm8
        vfnmsub213pd xmm10, xmm9, xmm11
        movsxd    rcx, ecx
        vpextrd   r8d, xmm5, 2
        vsubpd    xmm4, xmm3, xmm10
        movsxd    r8, r8d
        vmovsd    xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+384+rax+rcx]
        vmovupd   xmm5, XMMWORD PTR [__svml_dtan_ha_data_internal+960]
        vsubpd    xmm3, xmm3, xmm4
        vmovhpd   xmm1, xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+384+rax+r8]
        vmovapd   xmm6, xmm5
        vfnmadd231pd xmm6, xmm4, xmm1
        vmovsd    xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+512+rax+rcx]
        vmovhpd   xmm2, xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+512+rax+r8]
        vaddpd    xmm0, xmm1, xmm4
        vsubpd    xmm15, xmm3, xmm10
        vsubpd    xmm3, xmm6, xmm5
        vdivpd    xmm8, xmm5, xmm6
        vsubpd    xmm11, xmm0, xmm1
        vfmadd231pd xmm3, xmm4, xmm1
        vsubpd    xmm12, xmm4, xmm11
        vfmadd213pd xmm1, xmm15, xmm3
        vaddpd    xmm13, xmm2, xmm12
        vfmadd213pd xmm2, xmm4, xmm1
        vmovapd   xmm1, xmm5
        vfnmadd231pd xmm1, xmm6, xmm8
        vaddpd    xmm7, xmm15, xmm13
        vfmadd213pd xmm8, xmm1, xmm8
        vfnmadd231pd xmm5, xmm6, xmm8
        vfmadd231pd xmm5, xmm2, xmm8
        vfmadd213pd xmm8, xmm5, xmm8
        vmulpd    xmm9, xmm0, xmm8
        vfmsub213pd xmm6, xmm9, xmm0
        vfnmadd213pd xmm2, xmm9, xmm6
        vsubpd    xmm0, xmm2, xmm7
        vfnmadd213pd xmm0, xmm8, xmm9
        test      edx, edx
        jne       _B7_4

_B7_3::

        vmovups   xmm6, XMMWORD PTR [368+rsp]
        vmovups   xmm7, XMMWORD PTR [352+rsp]
        vmovups   xmm8, XMMWORD PTR [336+rsp]
        vmovups   xmm9, XMMWORD PTR [320+rsp]
        vmovups   xmm10, XMMWORD PTR [304+rsp]
        vmovups   xmm11, XMMWORD PTR [288+rsp]
        vmovups   xmm12, XMMWORD PTR [272+rsp]
        vmovups   xmm13, XMMWORD PTR [256+rsp]
        vmovups   xmm14, XMMWORD PTR [240+rsp]
        vmovups   xmm15, XMMWORD PTR [224+rsp]
        mov       r13, QWORD PTR [384+rsp]
        add       rsp, 408
        ret

_B7_4::

        vmovupd   XMMWORD PTR [r13], xmm14
        vmovupd   XMMWORD PTR [64+r13], xmm0
        je        _B7_3

_B7_7::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B7_8::

        bt        esi, ebx
        jc        _B7_11

_B7_9::

        inc       ebx
        cmp       ebx, 2
        jl        _B7_8

_B7_10::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B7_3

_B7_11::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dtan_ha_cout_rare_internal
        jmp       _B7_9

_B7_12::

        vmovupd   XMMWORD PTR [32+rsp], xmm0
        vpand     xmm0, xmm14, XMMWORD PTR [_2il0floatpacket_29]
        vmovupd   XMMWORD PTR [48+rsp], xmm3
        vpsrlq    xmm3, xmm0, 52
        vmovd     ecx, xmm3
        vmovupd   xmm6, XMMWORD PTR [__svml_dtan_ha_data_internal+1088]
        vandpd    xmm9, xmm6, xmm7
        vpextrd   r9d, xmm3, 2
        vcmpeqpd  xmm10, xmm9, xmm6
        vpand     xmm7, xmm14, XMMWORD PTR [_2il0floatpacket_30]
        lea       r8d, DWORD PTR [rcx+rcx*2]
        shl       r8d, 3
        lea       r10d, DWORD PTR [r9+r9*2]
        shl       r10d, 3
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rax+r8]
        vpaddq    xmm6, xmm7, XMMWORD PTR [_2il0floatpacket_31]
        vmovmskpd edx, xmm10
        vmovupd   XMMWORD PTR [64+rsp], xmm11
        vpsrlq    xmm9, xmm6, 32
        vmovhpd   xmm11, xmm12, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rax+r10]
        vmovq     xmm15, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rax+r8]
        vpsrlq    xmm0, xmm11, 32
        vmovdqu   xmm12, XMMWORD PTR [_2il0floatpacket_32]
        vmovhpd   xmm10, xmm15, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rax+r10]
        vpand     xmm15, xmm11, xmm12
        vpmuludq  xmm11, xmm9, xmm15
        vpand     xmm6, xmm6, xmm12
        vmovq     xmm8, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rax+r8]
        vpsrlq    xmm15, xmm11, 32
        vmovhpd   xmm5, xmm8, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rax+r10]
        vpand     xmm7, xmm10, xmm12
        vpmuludq  xmm11, xmm9, xmm0
        vpand     xmm2, xmm5, xmm12
        vpmuludq  xmm0, xmm6, xmm0
        vpmuludq  xmm1, xmm6, xmm2
        vpmuludq  xmm2, xmm9, xmm2
        vpmuludq  xmm8, xmm6, xmm7
        vpsrlq    xmm0, xmm0, 32
        vpsrlq    xmm4, xmm5, 32
        vpaddq    xmm11, xmm11, xmm0
        vpmuludq  xmm3, xmm6, xmm4
        vpmuludq  xmm4, xmm9, xmm4
        vpmuludq  xmm9, xmm9, xmm7
        vpand     xmm5, xmm1, xmm12
        vpsrlq    xmm1, xmm1, 32
        vpaddq    xmm5, xmm5, xmm11
        vpaddq    xmm11, xmm15, xmm5
        vpaddq    xmm5, xmm2, xmm1
        vpand     xmm0, xmm3, xmm12
        vpsrlq    xmm15, xmm11, 32
        vpaddq    xmm0, xmm0, xmm5
        vpaddq    xmm5, xmm15, xmm0
        vpsrlq    xmm0, xmm3, 32
        vpsrlq    xmm10, xmm10, 32
        vpaddq    xmm3, xmm4, xmm0
        vpmuludq  xmm0, xmm6, xmm10
        vpand     xmm1, xmm8, xmm12
        vpsrlq    xmm2, xmm5, 32
        vpaddq    xmm15, xmm1, xmm3
        vpaddq    xmm4, xmm2, xmm15
        vpand     xmm15, xmm0, xmm12
        vpsrlq    xmm0, xmm8, 32
        vpaddq    xmm8, xmm9, xmm0
        vpaddq    xmm7, xmm15, xmm8
        vpsrlq    xmm3, xmm4, 32
        vpand     xmm2, xmm4, xmm12
        vpaddq    xmm3, xmm3, xmm7
        vpsllq    xmm1, xmm3, 32
        vpsllq    xmm5, xmm5, 32
        vpand     xmm12, xmm11, xmm12
        vpaddq    xmm7, xmm1, xmm2
        vpaddq    xmm11, xmm5, xmm12
        vpand     xmm5, xmm14, XMMWORD PTR [_2il0floatpacket_33]
        vpsrlq    xmm4, xmm7, 12
        vpxor     xmm6, xmm5, XMMWORD PTR [_2il0floatpacket_34]
        vmovupd   xmm9, XMMWORD PTR [_2il0floatpacket_35]
        vpor      xmm10, xmm4, xmm6
        vaddpd    xmm8, xmm10, xmm9
        vsubpd    xmm0, xmm8, xmm9
        vpand     xmm7, xmm7, XMMWORD PTR [_2il0floatpacket_39]
        vpand     xmm3, xmm11, XMMWORD PTR [_2il0floatpacket_37]
        vpsllq    xmm4, xmm7, 40
        vpsrlq    xmm11, xmm11, 24
        vpsllq    xmm15, xmm3, 28
        vpxor     xmm2, xmm5, XMMWORD PTR [_2il0floatpacket_36]
        vsubpd    xmm12, xmm10, xmm0
        vpxor     xmm5, xmm5, XMMWORD PTR [_2il0floatpacket_38]
        vpor      xmm0, xmm4, xmm11
        vpor      xmm1, xmm15, xmm2
        vpor      xmm3, xmm0, xmm5
        vsubpd    xmm15, xmm1, xmm2
        vsubpd    xmm1, xmm3, xmm5
        vaddpd    xmm6, xmm12, xmm1
        vsubpd    xmm12, xmm12, xmm6
        vmovupd   xmm4, XMMWORD PTR [_2il0floatpacket_40]
        vmulpd    xmm11, xmm4, xmm6
        vaddpd    xmm2, xmm1, xmm12
        vaddpd    xmm1, xmm15, xmm2
        vmovapd   xmm15, xmm4
        vfmsub213pd xmm15, xmm6, xmm11
        vmovupd   xmm9, XMMWORD PTR [_2il0floatpacket_44]
        vandpd    xmm7, xmm14, XMMWORD PTR [_2il0floatpacket_43]
        vpand     xmm8, xmm8, XMMWORD PTR [_2il0floatpacket_45]
        vpshufd   xmm8, xmm8, 136
        vfmadd132pd xmm6, xmm15, XMMWORD PTR [_2il0floatpacket_54]
        vcmpgtpd  xmm15, xmm7, xmm9
        vcmplepd  xmm10, xmm7, xmm9
        vfmadd213pd xmm1, xmm4, xmm6
        vandpd    xmm0, xmm10, xmm14
        vandpd    xmm3, xmm15, xmm11
        vorpd     xmm0, xmm0, xmm3
        vpxor     xmm6, xmm6, xmm6
        vpshufd   xmm3, xmm0, 221
        vandpd    xmm15, xmm15, xmm1
        vmovdqu   xmm1, XMMWORD PTR [_2il0floatpacket_55]
        vpsrld    xmm2, xmm3, 31
        vpsubd    xmm4, xmm1, xmm2
        vmovapd   xmm1, xmm0
        vpaddd    xmm5, xmm8, xmm4
        vpsrld    xmm7, xmm5, 4
        vpslld    xmm9, xmm7, 4
        vpsubd    xmm10, xmm8, xmm9
        vcvtdq2pd xmm12, xmm10
        vmovupd   xmm11, XMMWORD PTR [_2il0floatpacket_56]
        vfmadd231pd xmm1, xmm11, xmm12
        vpunpckldq xmm3, xmm7, xmm6
        vfnmadd213pd xmm11, xmm12, xmm1
        vsubpd    xmm0, xmm0, xmm11
        vfmadd132pd xmm12, xmm0, XMMWORD PTR [_2il0floatpacket_57]
        vmovupd   xmm0, XMMWORD PTR [32+rsp]
        vmovupd   xmm11, XMMWORD PTR [64+rsp]
        vblendvpd xmm0, xmm0, xmm3, xmm13
        vmovupd   xmm3, XMMWORD PTR [48+rsp]
        vaddpd    xmm15, xmm15, xmm12
        vblendvpd xmm3, xmm3, xmm1, xmm13
        vblendvpd xmm11, xmm11, xmm15, xmm13
        jmp       _B7_2
        ALIGN     16

_B7_13::

__svml_tan2_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan2_ha_l9_B1_B4:
	DD	1604097
	DD	3200122
	DD	1534066
	DD	1472617
	DD	1411168
	DD	1349719
	DD	1288270
	DD	1226821
	DD	1165372
	DD	1103923
	DD	1042470
	DD	981019
	DD	3342603

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_7
	DD	imagerel _unwind___svml_tan2_ha_l9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan2_ha_l9_B7_B11:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B7_1
	DD	imagerel _B7_7
	DD	imagerel _unwind___svml_tan2_ha_l9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_7
	DD	imagerel _B7_12
	DD	imagerel _unwind___svml_tan2_ha_l9_B7_B11

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan2_ha_l9_B12_B12:
	DD	33
	DD	imagerel _B7_1
	DD	imagerel _B7_7
	DD	imagerel _unwind___svml_tan2_ha_l9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_12
	DD	imagerel _B7_13
	DD	imagerel _unwind___svml_tan2_ha_l9_B12_B12

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST7:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tan2_ha_e9

__svml_tan2_ha_e9	PROC	EXPORT

_B8_1::

        DB        243
        DB        15
        DB        30
        DB        250
L300::

        sub       rsp, 472
        xor       edx, edx
        vmovups   XMMWORD PTR [240+rsp], xmm15
        vmovups   XMMWORD PTR [256+rsp], xmm14
        vmovups   XMMWORD PTR [272+rsp], xmm13
        vmovups   XMMWORD PTR [288+rsp], xmm12
        vmovups   XMMWORD PTR [304+rsp], xmm11
        vmovups   XMMWORD PTR [320+rsp], xmm10
        vmovups   XMMWORD PTR [336+rsp], xmm9
        vmovups   XMMWORD PTR [352+rsp], xmm8
        vmovups   XMMWORD PTR [368+rsp], xmm7
        vmovups   XMMWORD PTR [384+rsp], xmm6
        mov       QWORD PTR [448+rsp], r13
        lea       r13, QWORD PTR [159+rsp]
        vmovupd   xmm5, XMMWORD PTR [__svml_dtan_ha_data_internal+1024]
        and       r13, -64
        vandpd    xmm14, xmm0, xmm5
        vandnpd   xmm15, xmm5, xmm0
        vmulpd    xmm12, xmm14, XMMWORD PTR [__svml_dtan_ha_data_internal+1472]
        vcmpnlepd xmm13, xmm14, XMMWORD PTR [__svml_dtan_ha_data_internal+28160]
        vmovupd   xmm11, XMMWORD PTR [__svml_dtan_ha_data_internal+1152]
        vmovupd   XMMWORD PTR [416+rsp], xmm15
        vaddpd    xmm15, xmm11, xmm12
        vsubpd    xmm2, xmm15, xmm11
        vmulpd    xmm10, xmm2, XMMWORD PTR [__svml_dtan_ha_data_internal+1216]
        vmulpd    xmm3, xmm2, XMMWORD PTR [__svml_dtan_ha_data_internal+1280]
        vsubpd    xmm9, xmm14, xmm10
        vmulpd    xmm5, xmm2, XMMWORD PTR [__svml_dtan_ha_data_internal+1344]
        vsubpd    xmm7, xmm9, xmm3
        vsubpd    xmm4, xmm9, xmm7
        vsubpd    xmm1, xmm4, xmm3
        vsubpd    xmm8, xmm1, xmm5
        vpand     xmm12, xmm15, XMMWORD PTR [__svml_dtan_ha_data_internal+1408]
        vaddpd    xmm15, xmm7, xmm8
        vpand     xmm11, xmm12, XMMWORD PTR [_2il0floatpacket_26]
        vmovd     eax, xmm11
        vmovupd   XMMWORD PTR [432+rsp], xmm13
        vmovupd   XMMWORD PTR [32+rsp], xmm14
        imul      ecx, eax, 104
        lea       rax, QWORD PTR [__ImageBase]
        vmovupd   XMMWORD PTR [400+rsp], xmm0
        vsubpd    xmm13, xmm7, xmm15
        vpextrd   r8d, xmm11, 2
        imul      r9d, r8d, 104
        vmovq     xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rax+rcx]
        vmovq     xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rax+rcx]
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rax+rcx]
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rax+rcx]
        vmovhpd   xmm1, xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rax+r9]
        vaddpd    xmm5, xmm8, xmm13
        vmovq     xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rax+rcx]
        vmovhpd   xmm7, xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rax+r9]
        vmovupd   XMMWORD PTR [64+rsp], xmm1
        vmovq     xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rax+rcx]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rax+rcx]
        vmovhpd   xmm4, xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rax+r9]
        vmovhpd   xmm13, xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rax+r9]
        vmovhpd   xmm8, xmm11, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rax+r9]
        vmovhpd   xmm11, xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rax+r9]
        vmovhpd   xmm3, xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rax+r9]
        vsubpd    xmm1, xmm4, xmm15
        vmulpd    xmm8, xmm8, xmm15
        vsubpd    xmm4, xmm4, xmm1
        vmovq     xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rax+rcx]
        vmovq     xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rax+rcx]
        vmovq     xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rax+rcx]
        vmovhpd   xmm0, xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rax+r9]
        vmovhpd   xmm10, xmm14, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rax+r9]
        vmovhpd   xmm14, xmm2, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rax+r9]
        vmovupd   xmm2, XMMWORD PTR [_2il0floatpacket_27]
        vsubpd    xmm4, xmm4, xmm15
        vmulpd    xmm14, xmm14, xmm15
        vmovupd   XMMWORD PTR [80+rsp], xmm3
        vandpd    xmm3, xmm1, xmm2
        vsubpd    xmm1, xmm1, xmm3
        vaddpd    xmm11, xmm11, xmm14
        vaddpd    xmm0, xmm0, xmm1
        vmovupd   XMMWORD PTR [48+rsp], xmm5
        vsubpd    xmm5, xmm4, xmm5
        vcvtpd2ps xmm4, xmm3
        vaddpd    xmm5, xmm5, xmm0
        vmovlhps  xmm1, xmm4, xmm4
        vrcpps    xmm0, xmm1
        vcvtps2pd xmm4, xmm0
        vandpd    xmm4, xmm4, xmm2
        vmulpd    xmm3, xmm3, xmm4
        vmovupd   xmm2, XMMWORD PTR [_2il0floatpacket_28]
        vsubpd    xmm1, xmm2, xmm3
        vmulpd    xmm3, xmm4, xmm1
        vaddpd    xmm0, xmm4, xmm3
        vmulpd    xmm3, xmm1, xmm1
        vaddpd    xmm2, xmm2, xmm3
        vmulpd    xmm2, xmm0, xmm2
        vmulpd    xmm5, xmm5, xmm2
        vsubpd    xmm3, xmm5, xmm1
        vmulpd    xmm5, xmm5, xmm5
        vsubpd    xmm0, xmm5, xmm3
        vmulpd    xmm1, xmm2, xmm0
        vmulpd    xmm2, xmm13, xmm15
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+rcx]
        vmovhpd   xmm6, xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+r9]
        vmulpd    xmm3, xmm6, xmm4
        vaddpd    xmm4, xmm7, xmm2
        vmulpd    xmm6, xmm6, xmm1
        vsubpd    xmm7, xmm7, xmm4
        vaddpd    xmm5, xmm3, xmm4
        vaddpd    xmm7, xmm2, xmm7
        vsubpd    xmm0, xmm3, xmm5
        vmulpd    xmm2, xmm15, xmm15
        vaddpd    xmm6, xmm6, xmm7
        vaddpd    xmm7, xmm4, xmm0
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rax+rcx]
        vmovhpd   xmm9, xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rax+r9]
        vmulpd    xmm0, xmm9, xmm15
        vmulpd    xmm9, xmm2, xmm2
        vaddpd    xmm1, xmm10, xmm0
        vaddpd    xmm7, xmm6, xmm7
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+rcx]
        vmovhpd   xmm12, xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+r9]
        vaddpd    xmm8, xmm12, xmm8
        vaddpd    xmm3, xmm13, xmm12
        vmulpd    xmm12, xmm2, XMMWORD PTR [80+rsp]
        vmulpd    xmm13, xmm1, xmm2
        vaddpd    xmm6, xmm11, xmm12
        vaddpd    xmm10, xmm8, xmm13
        vmulpd    xmm4, xmm3, XMMWORD PTR [48+rsp]
        vmulpd    xmm6, xmm6, xmm9
        vaddpd    xmm0, xmm7, xmm4
        vaddpd    xmm1, xmm10, xmm6
        vaddpd    xmm2, xmm0, XMMWORD PTR [64+rsp]
        vmulpd    xmm15, xmm15, xmm1
        vaddpd    xmm0, xmm2, xmm15
        vaddpd    xmm5, xmm5, xmm0
        vmovupd   xmm2, XMMWORD PTR [432+rsp]
        vmovmskpd r10d, xmm2
        vxorpd    xmm0, xmm5, XMMWORD PTR [416+rsp]
        vmovupd   xmm1, XMMWORD PTR [32+rsp]
        mov       QWORD PTR [456+rsp], r13
        test      r10d, r10d
        jne       _B8_12

_B8_2::

        test      edx, edx
        jne       _B8_4

_B8_3::

        vmovups   xmm6, XMMWORD PTR [384+rsp]
        vmovups   xmm7, XMMWORD PTR [368+rsp]
        vmovups   xmm8, XMMWORD PTR [352+rsp]
        vmovups   xmm9, XMMWORD PTR [336+rsp]
        vmovups   xmm10, XMMWORD PTR [320+rsp]
        vmovups   xmm11, XMMWORD PTR [304+rsp]
        vmovups   xmm12, XMMWORD PTR [288+rsp]
        vmovups   xmm13, XMMWORD PTR [272+rsp]
        vmovups   xmm14, XMMWORD PTR [256+rsp]
        vmovups   xmm15, XMMWORD PTR [240+rsp]
        mov       r13, QWORD PTR [448+rsp]
        add       rsp, 472
        ret

_B8_4::

        vmovupd   xmm1, XMMWORD PTR [400+rsp]
        vmovupd   XMMWORD PTR [r13], xmm1
        vmovupd   XMMWORD PTR [64+r13], xmm0
        je        _B8_3

_B8_7::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B8_8::

        bt        esi, ebx
        jc        _B8_11

_B8_9::

        inc       ebx
        cmp       ebx, 2
        jl        _B8_8

_B8_10::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm0, XMMWORD PTR [64+r13]
        jmp       _B8_3

_B8_11::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dtan_ha_cout_rare_internal
        jmp       _B8_9

_B8_12::

        vpand     xmm10, xmm1, XMMWORD PTR [_2il0floatpacket_29]
        vmovupd   XMMWORD PTR [432+rsp], xmm2
        vpsrlq    xmm2, xmm10, 52
        vmovupd   xmm3, XMMWORD PTR [__svml_dtan_ha_data_internal+1088]
        vmovd     ecx, xmm2
        vandpd    xmm9, xmm3, xmm1
        vmovupd   XMMWORD PTR [32+rsp], xmm0
        vcmpeqpd  xmm0, xmm9, xmm3
        vpextrd   r9d, xmm2, 2
        lea       r8d, DWORD PTR [rcx+rcx*2]
        shl       r8d, 3
        vpand     xmm9, xmm1, XMMWORD PTR [_2il0floatpacket_30]
        vmovq     xmm7, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rax+r8]
        vpaddq    xmm11, xmm9, XMMWORD PTR [_2il0floatpacket_31]
        vmovmskpd edx, xmm0
        vmovq     xmm8, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rax+r8]
        vmovdqu   xmm3, XMMWORD PTR [_2il0floatpacket_32]
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rax+r8]
        lea       r10d, DWORD PTR [r9+r9*2]
        shl       r10d, 3
        vpand     xmm9, xmm11, xmm3
        vmovhpd   xmm0, xmm7, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+rax+r10]
        vpsrlq    xmm7, xmm11, 32
        vmovhpd   xmm5, xmm8, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+rax+r10]
        vpand     xmm2, xmm0, xmm3
        vpsrlq    xmm15, xmm5, 32
        vpand     xmm13, xmm5, xmm3
        vpmuludq  xmm5, xmm7, xmm2
        vpmuludq  xmm12, xmm9, xmm13
        vpmuludq  xmm13, xmm7, xmm13
        vpmuludq  xmm14, xmm9, xmm15
        vpmuludq  xmm15, xmm7, xmm15
        vmovhpd   xmm4, xmm6, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+rax+r10]
        vpsrlq    xmm6, xmm0, 32
        vpsrlq    xmm2, xmm5, 32
        vpand     xmm0, xmm12, xmm3
        vpmuludq  xmm5, xmm7, xmm6
        vpand     xmm10, xmm4, xmm3
        vpmuludq  xmm6, xmm9, xmm6
        vpmuludq  xmm8, xmm9, xmm10
        vpmuludq  xmm10, xmm7, xmm10
        vpsrlq    xmm11, xmm6, 32
        vpsrlq    xmm12, xmm12, 32
        vpaddq    xmm6, xmm5, xmm11
        vpaddq    xmm0, xmm0, xmm6
        vpaddq    xmm5, xmm2, xmm0
        vpaddq    xmm2, xmm13, xmm12
        vpand     xmm11, xmm14, xmm3
        vpsrlq    xmm14, xmm14, 32
        vpaddq    xmm6, xmm11, xmm2
        vpaddq    xmm11, xmm15, xmm14
        vpsrlq    xmm0, xmm5, 32
        vpsrlq    xmm4, xmm4, 32
        vpaddq    xmm6, xmm0, xmm6
        vpand     xmm0, xmm8, xmm3
        vpsrlq    xmm2, xmm6, 32
        vpaddq    xmm13, xmm0, xmm11
        vpaddq    xmm0, xmm2, xmm13
        vpmuludq  xmm2, xmm9, xmm4
        vpsrlq    xmm8, xmm8, 32
        vpand     xmm4, xmm2, xmm3
        vpaddq    xmm7, xmm10, xmm8
        vpaddq    xmm9, xmm4, xmm7
        vpsrlq    xmm11, xmm0, 32
        vpand     xmm0, xmm0, xmm3
        vpaddq    xmm11, xmm11, xmm9
        vpsllq    xmm13, xmm11, 32
        vpsllq    xmm6, xmm6, 32
        vpaddq    xmm4, xmm13, xmm0
        vpand     xmm14, xmm1, XMMWORD PTR [_2il0floatpacket_33]
        vpand     xmm3, xmm5, xmm3
        vpxor     xmm10, xmm14, XMMWORD PTR [_2il0floatpacket_34]
        vpsrlq    xmm0, xmm4, 12
        vpaddq    xmm8, xmm6, xmm3
        vmovupd   xmm2, XMMWORD PTR [_2il0floatpacket_35]
        vpor      xmm6, xmm0, xmm10
        vaddpd    xmm7, xmm6, xmm2
        vsubpd    xmm9, xmm7, xmm2
        vpand     xmm4, xmm4, XMMWORD PTR [_2il0floatpacket_39]
        vpand     xmm3, xmm8, XMMWORD PTR [_2il0floatpacket_37]
        vpsllq    xmm15, xmm4, 40
        vpsrlq    xmm8, xmm8, 24
        vpsllq    xmm5, xmm3, 28
        vpxor     xmm10, xmm14, XMMWORD PTR [_2il0floatpacket_38]
        vpor      xmm12, xmm15, xmm8
        vpor      xmm0, xmm12, xmm10
        vsubpd    xmm6, xmm6, xmm9
        vsubpd    xmm8, xmm0, xmm10
        vaddpd    xmm9, xmm6, xmm8
        vsubpd    xmm6, xmm6, xmm9
        vpxor     xmm13, xmm14, XMMWORD PTR [_2il0floatpacket_36]
        vpor      xmm11, xmm5, xmm13
        vsubpd    xmm2, xmm11, xmm13
        vaddpd    xmm4, xmm8, xmm6
        vmovupd   xmm0, XMMWORD PTR [_2il0floatpacket_27]
        vandpd    xmm3, xmm9, xmm0
        vaddpd    xmm5, xmm2, xmm4
        vsubpd    xmm13, xmm9, xmm3
        vmulpd    xmm12, xmm5, XMMWORD PTR [_2il0floatpacket_40]
        vmovupd   xmm2, XMMWORD PTR [_2il0floatpacket_41]
        vmovupd   xmm11, XMMWORD PTR [_2il0floatpacket_42]
        vmulpd    xmm10, xmm2, xmm3
        vmulpd    xmm14, xmm2, xmm13
        vmulpd    xmm15, xmm11, xmm3
        vmulpd    xmm2, xmm11, xmm13
        vaddpd    xmm6, xmm14, xmm15
        vaddpd    xmm8, xmm12, xmm2
        vaddpd    xmm4, xmm6, xmm8
        vaddpd    xmm11, xmm10, xmm4
        vsubpd    xmm10, xmm10, xmm11
        vpand     xmm2, xmm7, XMMWORD PTR [_2il0floatpacket_45]
        vpand     xmm6, xmm2, XMMWORD PTR [_2il0floatpacket_26]
        vmovd     r11d, xmm6
        vmovupd   xmm3, XMMWORD PTR [_2il0floatpacket_44]
        vandpd    xmm9, xmm1, XMMWORD PTR [_2il0floatpacket_43]
        vcmpgtpd  xmm13, xmm9, xmm3
        vcmplepd  xmm5, xmm9, xmm3
        vaddpd    xmm14, xmm4, xmm10
        imul      ecx, r11d, 104
        vandpd    xmm1, xmm5, xmm1
        vandpd    xmm10, xmm13, xmm11
        vpextrd   r8d, xmm6, 2
        vorpd     xmm10, xmm1, xmm10
        imul      r9d, r8d, 104
        vandpd    xmm14, xmm13, xmm14
        vmovq     xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rax+rcx]
        vmovq     xmm4, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+rcx]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rax+rcx]
        vmovq     xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rax+rcx]
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rax+rcx]
        vmovq     xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rax+rcx]
        vmovhpd   xmm13, xmm8, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1536+rax+r9]
        vmovhpd   xmm11, xmm4, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1552+rax+r9]
        vmovhpd   xmm8, xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1576+rax+r9]
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+rcx]
        vmovq     xmm4, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rax+rcx]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rax+rcx]
        vmovhpd   xmm15, xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1544+rax+r9]
        vmovhpd   xmm2, xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1560+rax+r9]
        vmovhpd   xmm5, xmm3, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1568+rax+r9]
        vmovhpd   xmm7, xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1584+rax+r9]
        vmovhpd   xmm3, xmm4, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1600+rax+r9]
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rax+rcx]
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rax+rcx]
        vmovhpd   xmm4, xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1616+rax+r9]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rax+rcx]
        vmovupd   XMMWORD PTR [64+rsp], xmm5
        vmovhpd   xmm5, xmm9, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1608+rax+r9]
        vmovhpd   xmm9, xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1624+rax+r9]
        vmovhpd   xmm12, xmm1, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1632+rax+r9]
        vmovupd   XMMWORD PTR [80+rsp], xmm12
        vsubpd    xmm12, xmm13, xmm10
        vmulpd    xmm9, xmm10, xmm9
        vsubpd    xmm13, xmm13, xmm12
        vaddpd    xmm4, xmm4, xmm9
        vsubpd    xmm13, xmm13, xmm10
        vandpd    xmm1, xmm12, xmm0
        vmovupd   XMMWORD PTR [48+rsp], xmm14
        vsubpd    xmm13, xmm13, xmm14
        vsubpd    xmm14, xmm12, xmm1
        vaddpd    xmm15, xmm15, xmm14
        vcvtpd2ps xmm14, xmm1
        vaddpd    xmm13, xmm13, xmm15
        vmovlhps  xmm15, xmm14, xmm14
        vrcpps    xmm12, xmm15
        vcvtps2pd xmm14, xmm12
        vandpd    xmm0, xmm14, xmm0
        vmulpd    xmm1, xmm1, xmm0
        vmovupd   xmm12, XMMWORD PTR [_2il0floatpacket_28]
        vsubpd    xmm14, xmm12, xmm1
        vmulpd    xmm1, xmm0, xmm14
        vaddpd    xmm15, xmm0, xmm1
        vmulpd    xmm1, xmm14, xmm14
        vaddpd    xmm12, xmm12, xmm1
        vmulpd    xmm1, xmm15, xmm12
        vmulpd    xmm13, xmm13, xmm1
        vsubpd    xmm12, xmm13, xmm14
        vmulpd    xmm13, xmm13, xmm13
        vsubpd    xmm14, xmm13, xmm12
        vmulpd    xmm12, xmm10, xmm8
        vmulpd    xmm13, xmm11, xmm0
        vmulpd    xmm15, xmm1, xmm14
        vaddpd    xmm1, xmm2, xmm12
        vmulpd    xmm11, xmm11, xmm15
        vsubpd    xmm2, xmm2, xmm1
        vaddpd    xmm0, xmm13, xmm1
        vaddpd    xmm2, xmm12, xmm2
        vsubpd    xmm12, xmm13, xmm0
        vaddpd    xmm11, xmm11, xmm2
        vaddpd    xmm2, xmm1, xmm12
        vmulpd    xmm1, xmm10, xmm5
        vmulpd    xmm5, xmm10, xmm10
        vaddpd    xmm2, xmm11, xmm2
        vaddpd    xmm11, xmm8, xmm7
        vaddpd    xmm3, xmm3, xmm1
        vmulpd    xmm14, xmm5, xmm5
        vmulpd    xmm8, xmm3, xmm5
        vmulpd    xmm12, xmm11, XMMWORD PTR [48+rsp]
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rax+rcx]
        vmovhpd   xmm6, xmm6, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+1592+rax+r9]
        vmulpd    xmm6, xmm10, xmm6
        vaddpd    xmm1, xmm2, xmm12
        vaddpd    xmm6, xmm7, xmm6
        vmulpd    xmm7, xmm5, XMMWORD PTR [80+rsp]
        vaddpd    xmm15, xmm6, xmm8
        vaddpd    xmm13, xmm4, xmm7
        vaddpd    xmm3, xmm1, XMMWORD PTR [64+rsp]
        vmulpd    xmm13, xmm13, xmm14
        vaddpd    xmm2, xmm15, xmm13
        vmulpd    xmm10, xmm10, xmm2
        vaddpd    xmm1, xmm3, xmm10
        vaddpd    xmm0, xmm0, xmm1
        vxorpd    xmm1, xmm0, XMMWORD PTR [416+rsp]
        vmovupd   xmm0, XMMWORD PTR [32+rsp]
        vmovupd   xmm2, XMMWORD PTR [432+rsp]
        vblendvpd xmm0, xmm0, xmm1, xmm2
        jmp       _B8_2
        ALIGN     16

_B8_13::

__svml_tan2_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan2_ha_e9_B1_B4:
	DD	1601281
	DD	3724399
	DD	1599591
	DD	1538142
	DD	1476693
	DD	1415244
	DD	1353795
	DD	1292346
	DD	1230897
	DD	1169448
	DD	1107999
	DD	1046550
	DD	3866891

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_1
	DD	imagerel _B8_7
	DD	imagerel _unwind___svml_tan2_ha_e9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan2_ha_e9_B7_B11:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B8_1
	DD	imagerel _B8_7
	DD	imagerel _unwind___svml_tan2_ha_e9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_7
	DD	imagerel _B8_12
	DD	imagerel _unwind___svml_tan2_ha_e9_B7_B11

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan2_ha_e9_B12_B12:
	DD	33
	DD	imagerel _B8_1
	DD	imagerel _B8_7
	DD	imagerel _unwind___svml_tan2_ha_e9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_12
	DD	imagerel _B8_13
	DD	imagerel _unwind___svml_tan2_ha_e9_B12_B12

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST8:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_tan4_ha_l9

__svml_tan4_ha_l9	PROC	EXPORT

_B9_1::

        DB        243
        DB        15
        DB        30
        DB        250
L352::

        sub       rsp, 552
        lea       r8, QWORD PTR [__ImageBase]
        vmovups   YMMWORD PTR [208+rsp], ymm15
        xor       r9d, r9d
        vmovups   YMMWORD PTR [240+rsp], ymm14
        vmovups   YMMWORD PTR [272+rsp], ymm13
        vmovups   YMMWORD PTR [304+rsp], ymm12
        vmovups   YMMWORD PTR [336+rsp], ymm11
        vmovups   YMMWORD PTR [368+rsp], ymm10
        vmovups   YMMWORD PTR [400+rsp], ymm9
        vmovups   YMMWORD PTR [432+rsp], ymm8
        vmovups   YMMWORD PTR [464+rsp], ymm7
        vmovups   YMMWORD PTR [496+rsp], ymm6
        mov       QWORD PTR [528+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovdqa   ymm14, ymm0
        and       r13, -64
        vmovupd   ymm0, YMMWORD PTR [__svml_dtan_ha_data_internal]
        vmovupd   ymm4, YMMWORD PTR [__svml_dtan_ha_data_internal+1152]
        vmovupd   ymm1, YMMWORD PTR [__svml_dtan_ha_data_internal+64]
        vmovupd   ymm3, YMMWORD PTR [__svml_dtan_ha_data_internal+256]
        vmovupd   ymm5, YMMWORD PTR [__svml_dtan_ha_data_internal+320]
        vfmadd213pd ymm0, ymm14, ymm4
        vsubpd    ymm6, ymm0, ymm4
        vfnmadd213pd ymm1, ymm6, ymm14
        vmovdqa   ymm2, ymm3
        vfnmadd213pd ymm2, ymm6, ymm1
        vmovdqa   ymm4, ymm5
        vfnmadd213pd ymm4, ymm6, ymm2
        vsubpd    ymm1, ymm1, ymm2
        vsubpd    ymm2, ymm4, ymm2
        vfnmadd231pd ymm1, ymm3, ymm6
        vfmadd213pd ymm6, ymm5, ymm2
        vandpd    ymm7, ymm14, YMMWORD PTR [__svml_dtan_ha_data_internal+1024]
        vcmpnle_uqpd ymm13, ymm7, YMMWORD PTR [__svml_dtan_ha_data_internal+28160]
        vsubpd    ymm3, ymm1, ymm6
        vmovmskpd eax, ymm13
        mov       QWORD PTR [536+rsp], r13
        test      eax, eax
        jne       _B9_12

_B9_2::

        vpsllq    ymm5, ymm0, 3
        vmovupd   ymm0, YMMWORD PTR [__svml_dtan_ha_data_internal+896]
        vpand     ymm6, ymm5, YMMWORD PTR [_2il0floatpacket_93]
        vmulpd    ymm5, ymm4, ymm4
        vfmadd213pd ymm0, ymm5, YMMWORD PTR [__svml_dtan_ha_data_internal+832]
        vfmadd213pd ymm0, ymm5, YMMWORD PTR [__svml_dtan_ha_data_internal+768]
        vfmadd213pd ymm0, ymm5, YMMWORD PTR [__svml_dtan_ha_data_internal+704]
        vfmadd213pd ymm0, ymm5, YMMWORD PTR [__svml_dtan_ha_data_internal+640]
        vextracti128 xmm9, ymm6, 1
        vmovd     edx, xmm6
        vmovd     ecx, xmm9
        vpextrd   eax, xmm6, 2
        vmulpd    ymm6, ymm4, ymm0
        movsxd    rdx, edx
        movsxd    rcx, ecx
        vpextrd   r10d, xmm9, 2
        movsxd    rax, eax
        movsxd    r10, r10d
        vmovsd    xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+384+r8+rdx]
        vmovsd    xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+384+r8+rcx]
        vfnmsub213pd ymm6, ymm5, ymm3
        vmovhpd   xmm8, xmm7, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+384+r8+rax]
        vmovhpd   xmm11, xmm10, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+384+r8+r10]
        vmovsd    xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+512+r8+rdx]
        vmovsd    xmm15, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+512+r8+rcx]
        vsubpd    ymm7, ymm4, ymm6
        vmovhpd   xmm13, xmm12, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+512+r8+rax]
        vmovhpd   xmm1, xmm15, QWORD PTR [imagerel(__svml_dtan_ha_data_internal)+512+r8+r10]
        vsubpd    ymm4, ymm4, ymm7
        vsubpd    ymm6, ymm4, ymm6
        vinsertf128 ymm2, ymm8, xmm11, 1
        vmovupd   ymm8, YMMWORD PTR [__svml_dtan_ha_data_internal+960]
        vaddpd    ymm0, ymm2, ymm7
        vmovapd   ymm9, ymm8
        vfnmadd231pd ymm9, ymm7, ymm2
        vsubpd    ymm3, ymm0, ymm2
        vdivpd    ymm11, ymm8, ymm9
        vsubpd    ymm5, ymm9, ymm8
        vsubpd    ymm3, ymm7, ymm3
        vfmadd231pd ymm5, ymm7, ymm2
        vfmadd213pd ymm2, ymm6, ymm5
        vinsertf128 ymm1, ymm13, xmm1, 1
        vaddpd    ymm4, ymm1, ymm3
        vfmadd213pd ymm1, ymm7, ymm2
        vaddpd    ymm10, ymm6, ymm4
        vmovapd   ymm2, ymm8
        vfnmadd231pd ymm2, ymm9, ymm11
        vfmadd213pd ymm11, ymm2, ymm11
        vfnmadd231pd ymm8, ymm9, ymm11
        vfmadd231pd ymm8, ymm1, ymm11
        vfmadd213pd ymm11, ymm8, ymm11
        vmulpd    ymm12, ymm0, ymm11
        vfmsub213pd ymm9, ymm12, ymm0
        vfnmadd213pd ymm1, ymm12, ymm9
        vsubpd    ymm0, ymm1, ymm10
        vfnmadd213pd ymm0, ymm11, ymm12
        test      r9d, r9d
        jne       _B9_4

_B9_3::

        vmovups   ymm6, YMMWORD PTR [496+rsp]
        vmovups   ymm7, YMMWORD PTR [464+rsp]
        vmovups   ymm8, YMMWORD PTR [432+rsp]
        vmovups   ymm9, YMMWORD PTR [400+rsp]
        vmovups   ymm10, YMMWORD PTR [368+rsp]
        vmovups   ymm11, YMMWORD PTR [336+rsp]
        vmovups   ymm12, YMMWORD PTR [304+rsp]
        vmovups   ymm13, YMMWORD PTR [272+rsp]
        vmovups   ymm14, YMMWORD PTR [240+rsp]
        vmovups   ymm15, YMMWORD PTR [208+rsp]
        mov       r13, QWORD PTR [528+rsp]
        add       rsp, 552
        ret

_B9_4::

        vmovupd   YMMWORD PTR [r13], ymm14
        vmovupd   YMMWORD PTR [64+r13], ymm0
        je        _B9_3

_B9_7::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, r9d

_B9_8::

        bt        esi, ebx
        jc        _B9_11

_B9_9::

        inc       ebx
        cmp       ebx, 4
        jl        _B9_8

_B9_10::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [64+r13]
        jmp       _B9_3

_B9_11::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]

        call      __svml_dtan_ha_cout_rare_internal
        jmp       _B9_9

_B9_12::

        vmovupd   YMMWORD PTR [32+r13], ymm4
        vmovupd   YMMWORD PTR [64+r13], ymm3
        vmovupd   ymm10, YMMWORD PTR [__svml_dtan_ha_data_internal+1088]
        vmovupd   YMMWORD PTR [r13], ymm0
        vpand     ymm4, ymm14, YMMWORD PTR [_2il0floatpacket_79]
        vpsrlq    ymm3, ymm4, 52
        vandpd    ymm6, ymm10, ymm7
        vcmpeqpd  ymm0, ymm6, ymm10
        vmovmskpd r9d, ymm0
        vmovd     r10d, xmm3
        vextracti128 xmm5, ymm3, 1
        vpextrd   r11d, xmm3, 2
        lea       ecx, DWORD PTR [r10+r10*2]
        vmovd     eax, xmm5
        shl       ecx, 3
        lea       edx, DWORD PTR [r11+r11*2]
        vpextrd   r10d, xmm5, 2
        lea       eax, DWORD PTR [rax+rax*2]
        shl       edx, 3
        vmovq     xmm12, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+r8+rcx]
        vmovhpd   xmm15, xmm12, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+r8+rdx]
        vpand     ymm12, ymm14, YMMWORD PTR [_2il0floatpacket_80]
        lea       r11d, DWORD PTR [r10+r10*2]
        vmovq     xmm1, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+r8+rcx]
        vmovhpd   xmm0, xmm1, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+r8+rdx]
        vmovq     xmm9, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+r8+rcx]
        shl       eax, 3
        shl       r11d, 3
        vmovq     xmm11, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+r8+rax]
        vmovhpd   xmm8, xmm11, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+r8+r11]
        vpaddq    ymm11, ymm12, YMMWORD PTR [_2il0floatpacket_81]
        vmovq     xmm4, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+r8+rax]
        vmovhpd   xmm3, xmm4, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+16+r8+r11]
        vmovq     xmm6, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+r8+rax]
        vmovdqu   ymm12, YMMWORD PTR [_2il0floatpacket_82]
        vmovhpd   xmm7, xmm9, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+r8+rdx]
        vmovhpd   xmm2, xmm6, QWORD PTR [imagerel(__svml_dtan_ha_reduction_data_internal)+8+r8+r11]
        vpsrlq    ymm9, ymm11, 32
        vpand     ymm6, ymm11, ymm12
        vinsertf128 ymm5, ymm0, xmm3, 1
        vpsrlq    ymm0, ymm5, 32
        vpand     ymm5, ymm5, ymm12
        vinsertf128 ymm10, ymm15, xmm8, 1
        vinsertf128 ymm15, ymm7, xmm2, 1
        vpsrlq    ymm4, ymm15, 32
        vpand     ymm2, ymm15, ymm12
        vpand     ymm7, ymm10, ymm12
        vpsrlq    ymm10, ymm10, 32
        vpmuludq  ymm15, ymm9, ymm5
        vpmuludq  ymm5, ymm9, ymm0
        vpmuludq  ymm0, ymm6, ymm0
        vpsrlq    ymm0, ymm0, 32
        vpsrlq    ymm15, ymm15, 32
        vpaddq    ymm5, ymm5, ymm0
        vpmuludq  ymm1, ymm6, ymm2
        vpand     ymm11, ymm1, ymm12
        vpsrlq    ymm1, ymm1, 32
        vpaddq    ymm11, ymm11, ymm5
        vpmuludq  ymm3, ymm6, ymm4
        vpmuludq  ymm2, ymm9, ymm2
        vpaddq    ymm11, ymm15, ymm11
        vpand     ymm0, ymm3, ymm12
        vpaddq    ymm5, ymm2, ymm1
        vpsrlq    ymm15, ymm11, 32
        vpaddq    ymm0, ymm0, ymm5
        vpaddq    ymm5, ymm15, ymm0
        vpsrlq    ymm0, ymm3, 32
        vpsrlq    ymm2, ymm5, 32
        vpsllq    ymm5, ymm5, 32
        vpmuludq  ymm8, ymm6, ymm7
        vpmuludq  ymm4, ymm9, ymm4
        vpand     ymm1, ymm8, ymm12
        vpaddq    ymm4, ymm4, ymm0
        vpsrlq    ymm8, ymm8, 32
        vpaddq    ymm3, ymm1, ymm4
        vpmuludq  ymm0, ymm6, ymm10
        vpmuludq  ymm15, ymm9, ymm7
        vpaddq    ymm4, ymm2, ymm3
        vpand     ymm1, ymm0, ymm12
        vpaddq    ymm9, ymm15, ymm8
        vmovupd   ymm10, YMMWORD PTR [_2il0floatpacket_48]
        vpsrlq    ymm3, ymm4, 32
        vpaddq    ymm7, ymm1, ymm9
        vpand     ymm4, ymm4, ymm12
        vpand     ymm12, ymm11, ymm12
        vpaddq    ymm3, ymm3, ymm7
        vpand     ymm7, ymm14, YMMWORD PTR [_2il0floatpacket_83]
        vpaddq    ymm8, ymm5, ymm12
        vpsllq    ymm2, ymm3, 32
        vpxor     ymm6, ymm7, YMMWORD PTR [_2il0floatpacket_84]
        vpand     ymm12, ymm8, YMMWORD PTR [_2il0floatpacket_87]
        vpsrlq    ymm8, ymm8, 24
        vpaddq    ymm9, ymm2, ymm4
        vpxor     ymm2, ymm7, YMMWORD PTR [_2il0floatpacket_86]
        vpsllq    ymm15, ymm12, 28
        vpsrlq    ymm11, ymm9, 12
        vpand     ymm9, ymm9, YMMWORD PTR [_2il0floatpacket_89]
        vpor      ymm1, ymm15, ymm2
        vpor      ymm0, ymm11, ymm6
        vpsllq    ymm6, ymm9, 40
        vsubpd    ymm15, ymm1, ymm2
        vaddpd    ymm3, ymm0, ymm10
        vmovupd   ymm1, YMMWORD PTR [_2il0floatpacket_53]
        vpand     ymm5, ymm3, YMMWORD PTR [_2il0floatpacket_85]
        vsubpd    ymm4, ymm3, ymm10
        vmovupd   ymm3, YMMWORD PTR [_2il0floatpacket_49]
        vsubpd    ymm11, ymm0, ymm4
        vpxor     ymm0, ymm7, YMMWORD PTR [_2il0floatpacket_88]
        vpor      ymm7, ymm6, ymm8
        vpor      ymm10, ymm7, ymm0
        vsubpd    ymm4, ymm10, ymm0
        vaddpd    ymm12, ymm11, ymm4
        vmulpd    ymm6, ymm3, ymm12
        vsubpd    ymm11, ymm11, ymm12
        vaddpd    ymm0, ymm4, ymm11
        vmovapd   ymm4, ymm3
        vfmsub213pd ymm4, ymm12, ymm6
        vaddpd    ymm10, ymm15, ymm0
        vfmadd132pd ymm12, ymm4, YMMWORD PTR [_2il0floatpacket_90]
        vandpd    ymm15, ymm14, YMMWORD PTR [_2il0floatpacket_52]
        vcmpgt_oqpd ymm9, ymm15, ymm1
        vcmple_oqpd ymm2, ymm15, ymm1
        vfmadd213pd ymm10, ymm3, ymm12
        vandpd    ymm7, ymm2, ymm14
        vandpd    ymm8, ymm9, ymm6
        vorpd     ymm12, ymm7, ymm8
        vmovdqu   xmm2, XMMWORD PTR [_2il0floatpacket_55]
        vmovups   ymm8, YMMWORD PTR [__VUNPACK_EVEN_ind1_3308_0_10]
        vandpd    ymm0, ymm9, ymm10
        vextracti128 xmm3, ymm12, 1
        vshufps   xmm1, xmm12, xmm3, 221
        vextracti128 xmm4, ymm5, 1
        vshufps   xmm15, xmm5, xmm4, 136
        vpsrld    xmm5, xmm1, 31
        vpsubd    xmm6, xmm2, xmm5
        vpaddd    xmm7, xmm15, xmm6
        vpsrld    xmm9, xmm7, 4
        vmovupd   ymm4, YMMWORD PTR [_2il0floatpacket_91]
        vmovapd   ymm2, ymm12
        vpslld    xmm11, xmm9, 4
        vpsubd    xmm15, xmm15, xmm11
        vpermps   ymm10, ymm8, ymm9
        vcvtdq2pd ymm9, xmm15
        vfmadd231pd ymm2, ymm4, ymm9
        vfnmadd213pd ymm4, ymm9, ymm2
        vsubpd    ymm3, ymm12, ymm4
        vmovupd   ymm4, YMMWORD PTR [32+r13]
        vfmadd132pd ymm9, ymm3, YMMWORD PTR [_2il0floatpacket_92]
        vmovupd   ymm3, YMMWORD PTR [64+r13]
        vblendvpd ymm4, ymm4, ymm2, ymm13
        vaddpd    ymm5, ymm0, ymm9
        vmovupd   ymm0, YMMWORD PTR [r13]
        vblendvpd ymm3, ymm3, ymm5, ymm13
        vandps    ymm1, ymm10, YMMWORD PTR [__VUNPACK_EVEN_mask_3308_0_10]
        vblendvpd ymm0, ymm0, ymm1, ymm13
        jmp       _B9_2
        ALIGN     16

_B9_13::

__svml_tan4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan4_ha_l9_B1_B4:
	DD	1603329
	DD	4379767
	DD	2058351
	DD	1931366
	DD	1804381
	DD	1677396
	DD	1550411
	DD	1423426
	DD	1296441
	DD	1169456
	DD	1042471
	DD	915483
	DD	4522251

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_1
	DD	imagerel _B9_7
	DD	imagerel _unwind___svml_tan4_ha_l9_B1_B4

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan4_ha_l9_B7_B11:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B9_1
	DD	imagerel _B9_7
	DD	imagerel _unwind___svml_tan4_ha_l9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_7
	DD	imagerel _B9_12
	DD	imagerel _unwind___svml_tan4_ha_l9_B7_B11

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_tan4_ha_l9_B12_B12:
	DD	33
	DD	imagerel _B9_1
	DD	imagerel _B9_7
	DD	imagerel _unwind___svml_tan4_ha_l9_B1_B4

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_12
	DD	imagerel _B9_13
	DD	imagerel _unwind___svml_tan4_ha_l9_B12_B12

.pdata	ENDS
_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
__VUNPACK_EVEN_ind1_3308_0_10	DD	0
	DD	0
	DD	1
	DD	0
	DD	2
	DD	0
	DD	3
	DD	0
	DD 8 DUP (0H)	
__VUNPACK_EVEN_mask_3308_0_10	DD	-1
	DD	0
	DD	-1
	DD	0
	DD	-1
	DD	0
	DD	-1
	DD	0
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST9:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_dtan_ha_cout_rare_internal

__svml_dtan_ha_cout_rare_internal	PROC

_B10_1::

        DB        243
        DB        15
        DB        30
        DB        250
L385::

        sub       rsp, 40
        xor       eax, eax
        movzx     r9d, WORD PTR [6+rcx]
        movsd     xmm1, QWORD PTR [rcx]
        and       r9d, 32752
        mov       r8b, BYTE PTR [7+rcx]
        and       r8b, 127
        movsd     QWORD PTR [32+rsp], xmm1
        mov       BYTE PTR [39+rsp], r8b
        cmp       r9d, 32752
        je        _B10_3

_B10_2::

        add       rsp, 40
        ret

_B10_3::

        cmp       DWORD PTR [32+rsp], 0
        jne       _B10_6

_B10_4::

        cmp       DWORD PTR [36+rsp], 2146435072
        jne       _B10_6

_B10_5::

        movsd     xmm0, QWORD PTR [_vmldTanHATab]
        mov       eax, 1
        mulsd     xmm1, xmm0
        movsd     QWORD PTR [rdx], xmm1
        add       rsp, 40
        ret

_B10_6::

        mulsd     xmm1, xmm1
        movsd     QWORD PTR [rdx], xmm1
        add       rsp, 40
        ret
        ALIGN     16

_B10_7::

__svml_dtan_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_dtan_ha_cout_rare_internal_B1_B6:
	DD	67585
	DD	16904

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B10_1
	DD	imagerel _B10_7
	DD	imagerel _unwind___svml_dtan_ha_cout_rare_internal_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ  'DATA'
	DD 8 DUP (0H)	
	PUBLIC __svml_dtan_ha_reduction_data_internal
__svml_dtan_ha_reduction_data_internal	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
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
	PUBLIC __svml_dtan_ha_data_internal
__svml_dtan_ha_data_internal	DD	1841940611
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
	DD	856972294
	DD	1013032486
	DD	856972294
	DD	1013032486
	DD	856972294
	DD	1013032486
	DD	856972294
	DD	1013032486
	DD	856972294
	DD	1013032486
	DD	856972294
	DD	1013032486
	DD	856972294
	DD	1013032486
	DD	856972294
	DD	1013032486
	DD	688016905
	DD	958143697
	DD	688016905
	DD	958143697
	DD	688016905
	DD	958143697
	DD	688016905
	DD	958143697
	DD	688016905
	DD	958143697
	DD	688016905
	DD	958143697
	DD	688016905
	DD	958143697
	DD	688016905
	DD	958143697
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
	DD	0
	DD	2147483648
	DD	3763679576
	DD	1070167541
	DD	2583490354
	DD	1071284857
	DD	716700048
	DD	1071997368
	DD	0
	DD	1072693248
	DD	3797578849
	DD	1073213976
	DD	859807206
	DD	1073958991
	DD	3795994481
	DD	1075059710
	DD	4294967295
	DD	4293918719
	DD	3795994481
	DD	3222543358
	DD	859807206
	DD	3221442639
	DD	3797578849
	DD	3220697624
	DD	0
	DD	3220176896
	DD	716700048
	DD	3219481016
	DD	2583490354
	DD	3218768505
	DD	3763679576
	DD	3217651189
	DD	0
	DD	2147483648
	DD	1732516166
	DD	1009710547
	DD	2973134505
	DD	1014008623
	DD	561165941
	DD	1015523525
	DD	0
	DD	0
	DD	1767150223
	DD	1016338938
	DD	4129738197
	DD	1017254245
	DD	4275273486
	DD	1015088902
	DD	0
	DD	4238344192
	DD	4275273486
	DD	3162572550
	DD	4129738197
	DD	3164737893
	DD	1767150223
	DD	3163822586
	DD	0
	DD	0
	DD	561165941
	DD	3163007173
	DD	2973134505
	DD	3161492271
	DD	1732516166
	DD	3157194195
	DD	1431655900
	DD	1070945621
	DD	1431655900
	DD	1070945621
	DD	1431655900
	DD	1070945621
	DD	1431655900
	DD	1070945621
	DD	1431655900
	DD	1070945621
	DD	1431655900
	DD	1070945621
	DD	1431655900
	DD	1070945621
	DD	1431655900
	DD	1070945621
	DD	285935618
	DD	1069617425
	DD	285935618
	DD	1069617425
	DD	285935618
	DD	1069617425
	DD	285935618
	DD	1069617425
	DD	285935618
	DD	1069617425
	DD	285935618
	DD	1069617425
	DD	285935618
	DD	1069617425
	DD	285935618
	DD	1069617425
	DD	1218258378
	DD	1068212666
	DD	1218258378
	DD	1068212666
	DD	1218258378
	DD	1068212666
	DD	1218258378
	DD	1068212666
	DD	1218258378
	DD	1068212666
	DD	1218258378
	DD	1068212666
	DD	1218258378
	DD	1068212666
	DD	1218258378
	DD	1068212666
	DD	1716452265
	DD	1066820779
	DD	1716452265
	DD	1066820779
	DD	1716452265
	DD	1066820779
	DD	1716452265
	DD	1066820779
	DD	1716452265
	DD	1066820779
	DD	1716452265
	DD	1066820779
	DD	1716452265
	DD	1066820779
	DD	1716452265
	DD	1066820779
	DD	3351902813
	DD	1065508044
	DD	3351902813
	DD	1065508044
	DD	3351902813
	DD	1065508044
	DD	3351902813
	DD	1065508044
	DD	3351902813
	DD	1065508044
	DD	3351902813
	DD	1065508044
	DD	3351902813
	DD	1065508044
	DD	3351902813
	DD	1065508044
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
	DD	1413754136
	DD	1073291771
	DD	856972295
	DD	1016178214
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1431655769
	DD	1070945621
	DD	0
	DD	0
	DD	285114973
	DD	1069617425
	DD	0
	DD	0
	DD	3223786787
	DD	1068212718
	DD	1570481342
	DD	1073278903
	DD	2595802427
	DD	1014369127
	DD	0
	DD	0
	DD	75380366
	DD	1065951822
	DD	2999349512
	DD	3155830414
	DD	0
	DD	1072693248
	DD	2911696896
	DD	1059306942
	DD	451505506
	DD	1065952070
	DD	3089290734
	DD	1070946463
	DD	3705470160
	DD	1065403174
	DD	2568117119
	DD	1069618857
	DD	643394266
	DD	1064501452
	DD	501385640
	DD	1068202863
	DD	1727208548
	DD	1073266035
	DD	2531251621
	DD	3162145225
	DD	0
	DD	0
	DD	612012528
	DD	1067000646
	DD	549540416
	DD	3153143513
	DD	0
	DD	1072693248
	DD	2064191488
	DD	1061404484
	DD	4042886527
	DD	1067001638
	DD	149923164
	DD	1070948993
	DD	104448276
	DD	1066453158
	DD	2663431480
	DD	1069623160
	DD	4100741303
	DD	1065552806
	DD	3253087105
	DD	1068220130
	DD	1883935754
	DD	1073253167
	DD	2988318241
	DD	3163734974
	DD	0
	DD	0
	DD	3919601072
	DD	1067637674
	DD	2391050108
	DD	3158536696
	DD	0
	DD	1072693248
	DD	1803946752
	DD	1062615560
	DD	2247013512
	DD	1067639351
	DD	3277043521
	DD	1070953215
	DD	578207594
	DD	1067006653
	DD	1619551617
	DD	1069630354
	DD	2647612950
	DD	1066183733
	DD	2691224773
	DD	1068193529
	DD	2040662960
	DD	1073240299
	DD	4270081753
	DD	3164530442
	DD	0
	DD	0
	DD	2319126882
	DD	1068050215
	DD	2052183488
	DD	1006305725
	DD	0
	DD	1072693248
	DD	2962101888
	DD	1063503197
	DD	3966050657
	DD	1068054193
	DD	3178024141
	DD	1070959142
	DD	1146831000
	DD	1067507381
	DD	1630003540
	DD	1069640468
	DD	2204487828
	DD	1066612393
	DD	783402153
	DD	1068243248
	DD	2197390165
	DD	1073227431
	DD	3038089327
	DD	1016395176
	DD	0
	DD	0
	DD	3930011523
	DD	1068463253
	DD	3574549514
	DD	3158343440
	DD	0
	DD	1072693248
	DD	930814464
	DD	1064233867
	DD	3218126771
	DD	1068471034
	DD	873740287
	DD	1070966789
	DD	1580060562
	DD	1067789097
	DD	1505280051
	DD	1069653548
	DD	2221338604
	DD	1066937592
	DD	2641117162
	DD	1068312400
	DD	2354117371
	DD	1073214563
	DD	3512651629
	DD	1015129272
	DD	0
	DD	0
	DD	3439278129
	DD	1068687929
	DD	1073016494
	DD	1012431341
	DD	0
	DD	1072693248
	DD	2934847296
	DD	1064716672
	DD	2625704508
	DD	1068694663
	DD	3692939630
	DD	1070976174
	DD	3072165553
	DD	1068074393
	DD	223733198
	DD	1069669653
	DD	1158528257
	DD	1067269817
	DD	3197805108
	DD	1068357327
	DD	2510844577
	DD	1073201695
	DD	498468876
	DD	3161098111
	DD	0
	DD	0
	DD	1972895212
	DD	1068895135
	DD	3208334872
	DD	1010638399
	DD	0
	DD	1072693248
	DD	4107853760
	DD	1065246718
	DD	2229347510
	DD	1068905849
	DD	4172638464
	DD	1070987323
	DD	212469442
	DD	1068364021
	DD	1989165656
	DD	1069688852
	DD	4131454312
	DD	1067530480
	DD	3453538591
	DD	1068469531
	DD	2667571783
	DD	1073188827
	DD	1614402419
	DD	3163482232
	DD	0
	DD	0
	DD	2354785698
	DD	1069102779
	DD	959196066
	DD	1012432163
	DD	0
	DD	1072693248
	DD	2828230112
	DD	1065606626
	DD	3693284264
	DD	1069118808
	DD	1996210616
	DD	1071000265
	DD	3053541878
	DD	1068578846
	DD	1133334569
	DD	1069711236
	DD	977677893
	DD	1067705915
	DD	347264115
	DD	1068436198
	DD	2824298989
	DD	1073175959
	DD	2088964722
	DD	3164313480
	DD	0
	DD	0
	DD	931121154
	DD	1069310926
	DD	1121692745
	DD	3159637885
	DD	0
	DD	1072693248
	DD	709357472
	DD	1065955296
	DD	29972165
	DD	1069333808
	DD	254360647
	DD	1071015033
	DD	3732687627
	DD	1068729157
	DD	3224370683
	DD	1069736900
	DD	202592779
	DD	1067887231
	DD	1420423314
	DD	1068491719
	DD	2981026194
	DD	1073163091
	DD	924239062
	DD	1016612139
	DD	0
	DD	0
	DD	982366697
	DD	1069519640
	DD	1160213218
	DD	3158385836
	DD	0
	DD	1072693248
	DD	404678272
	DD	1066346403
	DD	1419607756
	DD	1069549319
	DD	1800406832
	DD	1071031665
	DD	3144361055
	DD	1068882825
	DD	1744148040
	DD	1069765960
	DD	3211597674
	DD	1068075289
	DD	404952187
	DD	1068609434
	DD	3137753400
	DD	1073150223
	DD	3579918397
	DD	1015563197
	DD	0
	DD	0
	DD	2242453104
	DD	1069638253
	DD	1143545012
	DD	3159361608
	DD	0
	DD	1072693248
	DD	1016752608
	DD	1066591110
	DD	1270001991
	DD	1069659269
	DD	1410785275
	DD	1071050206
	DD	1103929722
	DD	1069040275
	DD	3268015215
	DD	1069798554
	DD	957665168
	DD	1068270980
	DD	1067662813
	DD	1068610012
	DD	3294480606
	DD	1073137355
	DD	1489607747
	DD	3151418907
	DD	0
	DD	0
	DD	1940260416
	DD	1069743276
	DD	4144715194
	DD	1012060053
	DD	0
	DD	1072693248
	DD	1135818128
	DD	1066829850
	DD	867512158
	DD	1069770656
	DD	3821819258
	DD	1071070704
	DD	2890864845
	DD	1069201948
	DD	918459419
	DD	1069834828
	DD	797472495
	DD	1068475275
	DD	4093958433
	DD	1068635569
	DD	3451207812
	DD	1073124487
	DD	1547135652
	DD	3163048307
	DD	0
	DD	0
	DD	1959743063
	DD	1069848682
	DD	607727474
	DD	1013558026
	DD	0
	DD	1072693248
	DD	225834432
	DD	1067090614
	DD	732340398
	DD	1069883626
	DD	3221152670
	DD	1071093215
	DD	2810588359
	DD	1069368307
	DD	612200458
	DD	1069874948
	DD	645427023
	DD	1068594071
	DD	1966294363
	DD	1068694214
	DD	3607935018
	DD	1073111619
	DD	4202814986
	DD	3164096517
	DD	0
	DD	0
	DD	2429422902
	DD	1069954505
	DD	3297515896
	DD	3160006592
	DD	0
	DD	1072693248
	DD	528565440
	DD	1067373727
	DD	2395999282
	DD	1069998329
	DD	1512953454
	DD	1071117799
	DD	2702142222
	DD	1069539835
	DD	3515899576
	DD	1069919100
	DD	2767905785
	DD	1068706391
	DD	3127207959
	DD	1068808800
	DD	3764662223
	DD	1073098751
	DD	3105356094
	DD	1016829101
	DD	0
	DD	0
	DD	1425758568
	DD	1070060780
	DD	1224256500
	DD	1011856982
	DD	0
	DD	1072693248
	DD	2086713664
	DD	1067564956
	DD	1688303979
	DD	1070114921
	DD	938619730
	DD	1071144522
	DD	526120622
	DD	1069632280
	DD	3819295667
	DD	1069967496
	DD	2059466202
	DD	1068824653
	DD	4035080204
	DD	1068903334
	DD	3921389429
	DD	1073085883
	DD	3647185164
	DD	1015997122
	DD	0
	DD	0
	DD	3763679576
	DD	1070167541
	DD	448465424
	DD	1009708707
	DD	0
	DD	1072693248
	DD	64931152
	DD	1067729411
	DD	3593250320
	DD	1070233561
	DD	1127364104
	DD	1071173457
	DD	21320967
	DD	1069723988
	DD	4036384112
	DD	1070020367
	DD	3394693835
	DD	1068949482
	DD	4171096556
	DD	1068939101
	DD	4078116635
	DD	1073073015
	DD	39665264
	DD	1013608617
	DD	0
	DD	0
	DD	4023957346
	DD	1070274825
	DD	2652053984
	DD	1012417284
	DD	0
	DD	1072693248
	DD	1548193280
	DD	1067905620
	DD	2849460080
	DD	1070354416
	DD	586099284
	DD	1071204684
	DD	2426038050
	DD	1069819083
	DD	1209882932
	DD	1070077962
	DD	204929667
	DD	1069081561
	DD	2951798640
	DD	1069077118
	DD	4234843841
	DD	1073060147
	DD	1479868884
	DD	3162614382
	DD	0
	DD	0
	DD	3947580658
	DD	1070382668
	DD	4153467944
	DD	1013228820
	DD	0
	DD	1072693248
	DD	1306970432
	DD	1068093810
	DD	3320658510
	DD	1070477657
	DD	2638377394
	DD	1071238289
	DD	1218095092
	DD	1069917865
	DD	506153462
	DD	1070140566
	DD	2096772755
	DD	1069221614
	DD	2058474785
	DD	1069148484
	DD	96603751
	DD	1073047280
	DD	2021697954
	DD	3163879555
	DD	0
	DD	0
	DD	4068253482
	DD	1070491107
	DD	3098804354
	DD	1013932712
	DD	0
	DD	1072693248
	DD	3555466384
	DD	1068294223
	DD	3714292293
	DD	1070599779
	DD	58004546
	DD	1071274368
	DD	2889916307
	DD	1070020649
	DD	2725223781
	DD	1070208483
	DD	1960889140
	DD	1069370437
	DD	2173066945
	DD	1069259864
	DD	253330956
	DD	1073034412
	DD	991505830
	DD	1017046064
	DD	0
	DD	0
	DD	1971725998
	DD	1070598138
	DD	2628060598
	DD	1013294571
	DD	0
	DD	1072693248
	DD	3850616508
	DD	1068503032
	DD	715485429
	DD	1070664058
	DD	2831870887
	DD	1071313021
	DD	276112813
	DD	1070127773
	DD	4071473622
	DD	1070282053
	DD	3678820497
	DD	1069528889
	DD	928174949
	DD	1069392578
	DD	410058162
	DD	1073021544
	DD	4004709614
	DD	1016250595
	DD	0
	DD	0
	DD	1197423420
	DD	1070653011
	DD	176404688
	DD	1013428940
	DD	0
	DD	1072693248
	DD	3614211808
	DD	1068615863
	DD	2638984897
	DD	1070729808
	DD	1695604536
	DD	1071354361
	DD	2372043566
	DD	1070239593
	DD	136909610
	DD	1070361652
	DD	3640039434
	DD	1069622710
	DD	4088329288
	DD	1069449448
	DD	566785368
	DD	1073008676
	DD	2301849815
	DD	1014658654
	DD	0
	DD	0
	DD	1026922473
	DD	1070708240
	DD	4275552176
	DD	1013759157
	DD	0
	DD	1072693248
	DD	865263972
	DD	1068735226
	DD	1189261043
	DD	1070797132
	DD	602895285
	DD	1071398507
	DD	457371263
	DD	1070356493
	DD	1846858239
	DD	1070447677
	DD	3060498109
	DD	1069713009
	DD	4267979072
	DD	1069596604
	DD	723512574
	DD	1072995808
	DD	2825204233
	DD	3161855698
	DD	0
	DD	0
	DD	2664093517
	DD	1070763845
	DD	3574550848
	DD	1009790086
	DD	0
	DD	1072693248
	DD	2465054196
	DD	1068861279
	DD	3495559184
	DD	1070866134
	DD	2180799467
	DD	1071445588
	DD	3760731789
	DD	1070478879
	DD	3108073741
	DD	1070540584
	DD	1955989611
	DD	1069809654
	DD	166740423
	DD	1069654305
	DD	880239780
	DD	1072982940
	DD	4135548218
	DD	3163662592
	DD	0
	DD	0
	DD	895247324
	DD	1070819848
	DD	3063999364
	DD	3161289029
	DD	0
	DD	1072693248
	DD	729983844
	DD	1068994194
	DD	3631919317
	DD	1070936926
	DD	2031343772
	DD	1071495745
	DD	1505593195
	DD	1070601643
	DD	519428998
	DD	1070618477
	DD	3710689648
	DD	1069913254
	DD	3156018952
	DD	1069741343
	DD	1036966986
	DD	1072970072
	DD	1122344434
	DD	3164458061
	DD	0
	DD	0
	DD	1412148370
	DD	1070876269
	DD	2689479793
	DD	3161477235
	DD	0
	DD	1072693248
	DD	1375255072
	DD	1069134151
	DD	1493266594
	DD	1071009624
	DD	487968719
	DD	1071549129
	DD	4010395236
	DD	1070668994
	DD	2875570533
	DD	1070672565
	DD	2596803931
	DD	1070024476
	DD	2843491938
	DD	1069843051
	DD	1193694191
	DD	1072957204
	DD	1890859349
	DD	1016467558
	DD	0
	DD	0
	DD	3782220567
	DD	1070933130
	DD	2488122455
	DD	1014753484
	DD	0
	DD	1072693248
	DD	91372748
	DD	1069281344
	DD	318776714
	DD	1071084349
	DD	1791786485
	DD	1071605902
	DD	245863368
	DD	1070739794
	DD	3306699314
	DD	1070730899
	DD	492155886
	DD	1070144051
	DD	504705110
	DD	1069961586
	DD	1350421397
	DD	1072944336
	DD	1218191675
	DD	1015274036
	DD	0
	DD	0
	DD	1606100959
	DD	1070990455
	DD	3385800912
	DD	1013630535
	DD	0
	DD	1072693248
	DD	2255302008
	DD	1069435977
	DD	54085333
	DD	1071161229
	DD	2504974906
	DD	1071655456
	DD	4169947836
	DD	1070814310
	DD	2891827400
	DD	1070793803
	DD	1030744926
	DD	1070272780
	DD	2378131433
	DD	1070036506
	DD	1507148603
	DD	1072931468
	DD	1086374101
	DD	3160519057
	DD	0
	DD	0
	DD	4158096969
	DD	1071048265
	DD	3224523546
	DD	1013419306
	DD	0
	DD	1072693248
	DD	4240313622
	DD	1069572894
	DD	3745046477
	DD	1071240398
	DD	1955120038
	DD	1071687503
	DD	2661530988
	DD	1070892836
	DD	2615081511
	DD	1070861626
	DD	3172686013
	DD	1070411548
	DD	3468537064
	DD	1070173373
	DD	1663875809
	DD	1072918600
	DD	3908862373
	DD	3163337468
	DD	0
	DD	0
	DD	2200768388
	DD	1071106586
	DD	302579070
	DD	3162432935
	DD	0
	DD	1072693248
	DD	2338248308
	DD	1069657986
	DD	2617403810
	DD	1071322000
	DD	60078074
	DD	1071721530
	DD	2753634668
	DD	1070975684
	DD	626020947
	DD	1070934758
	DD	25846105
	DD	1070561329
	DD	1123371528
	DD	1070307499
	DD	1820603015
	DD	1072905732
	DD	3236194699
	DD	3164241098
	DD	0
	DD	0
	DD	2528366626
	DD	1071165441
	DD	3808697588
	DD	3160880158
	DD	0
	DD	1072693248
	DD	1938261590
	DD	1069747146
	DD	4282399603
	DD	1071406183
	DD	1922568281
	DD	1071757646
	DD	2428166646
	DD	1071063193
	DD	717464826
	DD	1071013623
	DD	2205664064
	DD	1070659645
	DD	1525962144
	DD	1070438229
	DD	1977330220
	DD	1072892864
	DD	4071976381
	DD	1016684520
	DD	0
	DD	0
	DD	2390846448
	DD	1071224856
	DD	3510510824
	DD	1014225707
	DD	0
	DD	1072693248
	DD	1208409702
	DD	1069840505
	DD	1006199412
	DD	1071493107
	DD	2504246873
	DD	1071795971
	DD	1040653613
	DD	1071155729
	DD	1713268218
	DD	1071098686
	DD	462276291
	DD	1070747214
	DD	4239134769
	DD	1070603989
	DD	2134057426
	DD	1072879996
	DD	1285458442
	DD	1015707961
	DD	0
	DD	0
	DD	2583490354
	DD	1071284857
	DD	27154665
	DD	1014008337
	DD	0
	DD	1072693248
	DD	2550940470
	DD	1069938201
	DD	2616040293
	DD	1071582937
	DD	2529278955
	DD	1071836633
	DD	2244405849
	DD	1071253687
	DD	2064814125
	DD	1071190460
	DD	1527853242
	DD	1070842074
	DD	354304617
	DD	1070708112
	DD	2290784632
	DD	1072867128
	DD	2660353234
	DD	1011979525
	DD	0
	DD	0
	DD	3368427099
	DD	1071345471
	DD	2187847358
	DD	3161070788
	DD	0
	DD	1072693248
	DD	1708021028
	DD	1070040382
	DD	459972195
	DD	1071660262
	DD	3210514347
	DD	1071879770
	DD	955408873
	DD	1071357497
	DD	1911386086
	DD	1071289511
	DD	3563284025
	DD	1070944951
	DD	547392146
	DD	1070790031
	DD	2447511838
	DD	1072854260
	DD	3841595606
	DD	3162903543
	DD	0
	DD	0
	DD	408840118
	DD	1071406727
	DD	4048150272
	DD	1011870625
	DD	0
	DD	1072693248
	DD	2794370512
	DD	1070147203
	DD	1550993351
	DD	1071708355
	DD	1305093678
	DD	1071925532
	DD	1748211158
	DD	1071467623
	DD	3244212116
	DD	1071396455
	DD	2145665988
	DD	1071056651
	DD	1098394636
	DD	1070891096
	DD	2604239044
	DD	1072841392
	DD	1055077667
	DD	3164024136
	DD	0
	DD	0
	DD	2191706112
	DD	1071468652
	DD	333457838
	DD	1013847700
	DD	0
	DD	1072693248
	DD	3435408456
	DD	1070258830
	DD	487319719
	DD	1071758184
	DD	1713711059
	DD	1071974079
	DD	130753286
	DD	1071584571
	DD	909725129
	DD	1071511974
	DD	2762442355
	DD	1071178067
	DD	932699057
	DD	1071027067
	DD	2760966249
	DD	1072828524
	DD	1958126117
	DD	1016901483
	DD	0
	DD	0
	DD	220675706
	DD	1071531278
	DD	3158460941
	DD	1014240139
	DD	0
	DD	1072693248
	DD	1565030890
	DD	1070375439
	DD	335263687
	DD	1071809854
	DD	419816567
	DD	1072025586
	DD	2098048102
	DD	1071676780
	DD	257229334
	DD	1071636821
	DD	3464457175
	DD	1071310192
	DD	2745951745
	DD	1071158056
	DD	2917693455
	DD	1072815656
	DD	676362605
	DD	1016106015
	DD	0
	DD	0
	DD	3649427950
	DD	1071594634
	DD	4113913432
	DD	1011178545
	DD	0
	DD	1072693248
	DD	2424145432
	DD	1070497215
	DD	2715404890
	DD	1071863477
	DD	2238283673
	DD	1072080240
	DD	1186867211
	DD	1071742923
	DD	660318792
	DD	1071708248
	DD	2091427899
	DD	1071454132
	DD	2591062578
	DD	1071291190
	DD	3074420661
	DD	1072802788
	DD	1873363667
	DD	1014080331
	DD	0
	DD	0
	DD	2606398711
	DD	1071651713
	DD	2533078623
	DD	1015070425
	DD	0
	DD	1072693248
	DD	2151361100
	DD	1070610226
	DD	1532348096
	DD	1071919174
	DD	408604530
	DD	1072138246
	DD	1039538740
	DD	1071813375
	DD	1234598850
	DD	1071781284
	DD	692507560
	DD	1071611119
	DD	4090315823
	DD	1071451983
	DD	3231147867
	DD	1072789920
	DD	3253690381
	DD	3162434021
	DD	0
	DD	0
	DD	1505561933
	DD	1071684172
	DD	2813788960
	DD	3158646164
	DD	0
	DD	1072693248
	DD	2997217365
	DD	1070676584
	DD	2681788575
	DD	1071977071
	DD	3989126937
	DD	1072199822
	DD	1049115510
	DD	1071888491
	DD	511642057
	DD	1071860360
	DD	4034728042
	DD	1071713598
	DD	1788921063
	DD	1071639550
	DD	3387875073
	DD	1072777052
	DD	3168927931
	DD	3163807173
	DD	0
	DD	0
	DD	419968236
	DD	1071717047
	DD	131821922
	DD	3163445114
	DD	0
	DD	1072693248
	DD	2960267235
	DD	1070745841
	DD	1037049094
	DD	1072037305
	DD	1447436626
	DD	1072265209
	DD	3197232138
	DD	1071968658
	DD	4004685083
	DD	1071946034
	DD	1412816411
	DD	1071807280
	DD	3184767140
	DD	1071740610
	DD	3544602278
	DD	1072764184
	DD	4139243149
	DD	1017118445
	DD	0
	DD	0
	DD	4270735453
	DD	1071750355
	DD	3565353881
	DD	3162826177
	DD	0
	DD	1072693248
	DD	2915760230
	DD	1070818115
	DD	638580392
	DD	1072100020
	DD	2252948193
	DD	1072334663
	DD	1194731830
	DD	1072054301
	DD	1353002943
	DD	1072038927
	DD	422364777
	DD	1071909798
	DD	3669605864
	DD	1071842023
	DD	3701329484
	DD	1072751316
	DD	2857479637
	DD	1016322977
	DD	0
	DD	0
	DD	4010047525
	DD	1071784117
	DD	3339961616
	DD	1012589046
	DD	0
	DD	1072693248
	DD	966450668
	DD	1070893532
	DD	803989702
	DD	1072165371
	DD	1934453966
	DD	1072408465
	DD	1455077850
	DD	1072145882
	DD	2709264343
	DD	1072139721
	DD	1918064545
	DD	1072022114
	DD	1476733491
	DD	1071960574
	DD	3858056690
	DD	1072738448
	DD	2007897202
	DD	1014948181
	DD	0
	DD	0
	DD	2591355529
	DD	1071818352
	DD	2351346752
	DD	1014424505
	DD	0
	DD	1072693248
	DD	3206845590
	DD	1070972224
	DD	4023349050
	DD	1072233523
	DD	3838822434
	DD	1072486918
	DD	3122427157
	DD	1072243909
	DD	2261703628
	DD	1072249178
	DD	1764870085
	DD	1072145307
	DD	3201774398
	DD	1072093301
	DD	4014783896
	DD	1072725580
	DD	3119156846
	DD	3161566171
	DD	0
	DD	0
	DD	2298773501
	DD	1071853080
	DD	4027796943
	DD	3162574088
	DD	0
	DD	1072693248
	DD	312748411
	DD	1071054335
	DD	2723913091
	DD	1072304655
	DD	3207063211
	DD	1072570353
	DD	1845780128
	DD	1072348940
	DD	752659336
	DD	1072368142
	DD	1097207438
	DD	1072280589
	DD	407427378
	DD	1072246732
	DD	4171511102
	DD	1072712712
	DD	987810900
	DD	3163590211
	DD	0
	DD	0
	DD	681498355
	DD	1071888323
	DD	3542613740
	DD	3163311347
	DD	0
	DD	1072693248
	DD	168222332
	DD	1071140014
	DD	776736583
	DD	1072378956
	DD	2405401100
	DD	1072659128
	DD	430546584
	DD	1072461586
	DD	2974083787
	DD	1072497552
	DD	849442051
	DD	1072429322
	DD	2727422945
	DD	1072409680
	DD	33271012
	DD	1072699845
	DD	2269574412
	DD	3164385679
	DD	0
	DD	0
	DD	3684492763
	DD	1071924102
	DD	2905877726
	DD	3163193209
	DD	0
	DD	1072693248
	DD	585640690
	DD	1071229422
	DD	1068301595
	DD	1072456629
	DD	2953900489
	DD	1072723440
	DD	950237889
	DD	1072582519
	DD	109735426
	DD	1072638455
	DD	792610960
	DD	1072593041
	DD	2904161704
	DD	1072594405
	DD	379996435
	DD	1072680706
	DD	2807708551
	DD	3162614199
	DD	0
	DD	0
	DD	142288477
	DD	1071960443
	DD	1256321232
	DD	3162016428
	DD	0
	DD	1072693248
	DD	925074332
	DD	1071322730
	DD	2134728327
	DD	1072537893
	DD	1886408555
	DD	1072773770
	DD	101893267
	DD	1072702864
	DD	2020085224
	DD	1072742631
	DD	2955803851
	DD	1072733362
	DD	343310803
	DD	1072748161
	DD	693450846
	DD	1072654970
	DD	3218699017
	DD	1015418799
	DD	0
	DD	0
	DD	716700048
	DD	1071997368
	DD	1043856658
	DD	1015522810
	DD	0
	DD	1072693248
	DD	1097907398
	DD	1071420120
	DD	3912524876
	DD	1072622983
	DD	4107784306
	DD	1072827408
	DD	937056065
	DD	1072772766
	DD	4128945134
	DD	1072826390
	DD	1032262383
	DD	1072832917
	DD	1825923576
	DD	1072864423
	DD	1006905258
	DD	1072629234
	DD	3348558652
	DD	3159471942
	DD	0
	DD	0
	DD	3841229102
	DD	1072034903
	DD	1782837269
	DD	1016032992
	DD	0
	DD	1072693248
	DD	866896685
	DD	1071521786
	DD	2743435231
	DD	1072702700
	DD	3017984361
	DD	1072884608
	DD	1491919678
	DD	1072848040
	DD	1507535811
	DD	1072917857
	DD	1966124961
	DD	1072942914
	DD	381628626
	DD	1072992589
	DD	1320359670
	DD	1072603498
	DD	1908355032
	DD	3163192705
	DD	0
	DD	0
	DD	343219638
	DD	1072073077
	DD	2238302242
	DD	1014248160
	DD	0
	DD	1072693248
	DD	3768132253
	DD	1071627934
	DD	3895546569
	DD	1072749460
	DD	1613056473
	DD	1072945645
	DD	3409294908
	DD	1072929183
	DD	3138966349
	DD	1073017844
	DD	3799067273
	DD	1073064607
	DD	359470462
	DD	1073133087
	DD	1633814081
	DD	1072577762
	DD	3941137777
	DD	1014659019
	DD	0
	DD	0
	DD	3860971117
	DD	1072111915
	DD	2891637204
	DD	1015730508
	DD	0
	DD	1073741824
	DD	80822128
	DD	3219034205
	DD	2077635078
	DD	1072798543
	DD	1552870456
	DD	1073010820
	DD	3607700650
	DD	1073016746
	DD	249538440
	DD	1073127265
	DD	2233833066
	DD	1073199420
	DD	2436428201
	DD	1073293556
	DD	1947268493
	DD	1072552026
	DD	1185916272
	DD	3161855333
	DD	0
	DD	0
	DD	3978924239
	DD	1072151449
	DD	680748782
	DD	3163204529
	DD	0
	DD	1073741824
	DD	3528982175
	DD	3218918413
	DD	3018376188
	DD	1072850106
	DD	2093086652
	DD	1073080463
	DD	451029217
	DD	1073111339
	DD	1097121536
	DD	1073247142
	DD	2277789226
	DD	1073348969
	DD	149166906
	DD	1073478822
	DD	2260722904
	DD	1072526290
	DD	3285965784
	DD	1015852724
	DD	0
	DD	0
	DD	997287281
	DD	1072191710
	DD	3612277486
	DD	1015833408
	DD	0
	DD	1073741824
	DD	3037674984
	DD	3218797431
	DD	2624215131
	DD	1072904321
	DD	464779768
	DD	1073154936
	DD	3700763630
	DD	1073213636
	DD	1200926910
	DD	1073378628
	DD	2934576429
	DD	1073515096
	DD	3287041855
	DD	1073681673
	DD	2574177316
	DD	1072500554
	DD	2889755040
	DD	1013031024
	DD	0
	DD	0
	DD	2079874795
	DD	1072232729
	DD	1885505696
	DD	3161954077
	DD	0
	DD	1073741824
	DD	1656469901
	DD	3218670989
	DD	823761676
	DD	1072961373
	DD	2491445454
	DD	1073234634
	DD	1584341555
	DD	1073324391
	DD	1976688438
	DD	1073523018
	DD	3918313340
	DD	1073699902
	DD	3178717687
	DD	1073829019
	DD	2887631728
	DD	1072474818
	DD	1841088264
	DD	3162758780
	DD	0
	DD	0
	DD	258163604
	DD	1072274542
	DD	3299923807
	DD	1015128357
	DD	0
	DD	1073741824
	DD	2865277319
	DD	3218538798
	DD	3110066496
	DD	1073021461
	DD	723074889
	DD	1073319994
	DD	1618819009
	DD	1073444438
	DD	1176243936
	DD	1073681775
	DD	1223848987
	DD	1073823806
	DD	2369274010
	DD	1073957581
	DD	3201086139
	DD	1072449082
	DD	4185319304
	DD	1015274218
	DD	0
	DD	0
	DD	719595600
	DD	1072317184
	DD	114895218
	DD	3162143748
	DD	0
	DD	1073741824
	DD	3811788216
	DD	3218400550
	DD	1264738948
	DD	1073084804
	DD	1958933439
	DD	1073411493
	DD	4246310163
	DD	1073574708
	DD	1367842247
	DD	1073799186
	DD	4112596162
	DD	1073938662
	DD	2100954442
	DD	1074105254
	DD	3514540551
	DD	1072423346
	DD	2102765473
	DD	3160518326
	DD	0
	DD	0
	DD	4189730214
	DD	1072360693
	DD	873070368
	DD	1011168520
	DD	0
	DD	1073741824
	DD	760634709
	DD	3218255915
	DD	2067827628
	DD	1073151636
	DD	1386756408
	DD	1073509659
	DD	404552634
	DD	1073716242
	DD	1143062645
	DD	1073895515
	DD	555342321
	DD	1074067009
	DD	3470170865
	DD	1074271418
	DD	3827994963
	DD	1072397610
	DD	941734744
	DD	3163337286
	DD	0
	DD	0
	DD	3247184626
	DD	1072405111
	DD	1245412628
	DD	1014426137
	DD	0
	DD	1073741824
	DD	2381906911
	DD	3218104536
	DD	1010924947
	DD	1073222214
	DD	2481381005
	DD	1073615072
	DD	1356661294
	DD	1073806011
	DD	1527070582
	DD	1074001844
	DD	3061693388
	DD	1074210655
	DD	1993183195
	DD	1074463435
	DD	4141449374
	DD	1072371874
	DD	1579411055
	DD	1014369858
	DD	0
	DD	0
	DD	166164867
	DD	1072450480
	DD	2304058808
	DD	3159574198
	DD	0
	DD	1073741824
	DD	2798667120
	DD	3217812323
	DD	4203596829
	DD	1073296815
	DD	1473339147
	DD	1073728374
	DD	4243021652
	DD	1073889850
	DD	1638747017
	DD	1074119375
	DD	1512673096
	DD	1074371687
	DD	2338684787
	DD	1074678259
	DD	159936490
	DD	1072346139
	DD	3547642993
	DD	3162144494
	DD	0
	DD	0
	DD	3342355758
	DD	1072496843
	DD	1667048854
	DD	1014547708
	DD	0
	DD	1073741824
	DD	2271288998
	DD	3217480250
	DD	4111284847
	DD	1073375744
	DD	2796272966
	DD	1073796048
	DD	1730314448
	DD	1073981280
	DD	1813001552
	DD	1074249474
	DD	603251414
	DD	1074552509
	DD	3079042420
	DD	1074856525
	DD	473390901
	DD	1072320403
	DD	4252586071
	DD	1015708143
	DD	0
	DD	0
	DD	2308259791
	DD	1072544250
	DD	4116261952
	DD	1009282152
	DD	0
	DD	1073741824
	DD	411343470
	DD	3217132228
	DD	3760686807
	DD	1073459331
	DD	4171367426
	DD	1073861688
	DD	1878500541
	DD	1074081117
	DD	108767834
	DD	1074393698
	DD	2824899539
	DD	1074755901
	DD	3321235986
	DD	1074997439
	DD	786845313
	DD	1072294667
	DD	627570489
	DD	1011980987
	DD	0
	DD	0
	DD	2949191430
	DD	1072592750
	DD	3989207545
	DD	1015613028
	DD	0
	DD	1073741824
	DD	2909112164
	DD	3216503445
	DD	3682557528
	DD	1073547937
	DD	4184421718
	DD	1073932454
	DD	380803270
	DD	1074190282
	DD	3399744364
	DD	1074553823
	DD	3842864947
	DD	1074887742
	DD	748075417
	DD	1075157395
	DD	1100299725
	DD	1072268931
	DD	874467977
	DD	3162903361
	DD	0
	DD	0
	DD	2668510354
	DD	1072642397
	DD	1701265160
	DD	3159690220
	DD	0
	DD	1073741824
	DD	1404962112
	DD	3215492924
	DD	3327263454
	DD	1073641957
	DD	1987673213
	DD	1074008829
	DD	231222583
	DD	1074309811
	DD	1756671652
	DD	1074731884
	DD	3921518313
	DD	1075017100
	DD	2691193077
	DD	1075341426
	DD	1413754136
	DD	1072243195
	DD	856972295
	DD	1015129638
	DD	0
	DD	1072693248
	DD	3072795147
	DD	3218177217
	DD	2531204756
	DD	1014677625
	DD	0
	DD	1071644672
	DD	573338107
	DD	3216966390
	DD	1757081539
	DD	3216009405
	DD	1043378852
	DD	1067696161
	DD	2698850995
	DD	3213513876
	DD	495882814
	DD	1064911053
	DD	3842541443
	DD	3210988571
	DD	2725665262
	DD	1061135526
	DD	1727208548
	DD	1072217459
	DD	2531251621
	DD	3161096649
	DD	0
	DD	1072693248
	DD	963359913
	DD	3218157757
	DD	2464811394
	DD	1013284229
	DD	0
	DD	1071644672
	DD	1498613974
	DD	3216992493
	DD	4138103782
	DD	3215985771
	DD	2840947366
	DD	1067675293
	DD	209537977
	DD	3213463826
	DD	3590357530
	DD	1064864171
	DD	73959547
	DD	3210940132
	DD	1511606549
	DD	1062390487
	DD	2040662960
	DD	1072191723
	DD	4270081753
	DD	3163481866
	DD	0
	DD	1072693248
	DD	4002124759
	DD	3218138375
	DD	3453342566
	DD	3160907529
	DD	0
	DD	1071644672
	DD	774562305
	DD	3217018021
	DD	377617675
	DD	3215942442
	DD	2495478050
	DD	1067655031
	DD	1508050213
	DD	3213415186
	DD	1921336534
	DD	1064819067
	DD	975923672
	DD	3210893594
	DD	2289965811
	DD	1060505011
	DD	2354117371
	DD	1072165987
	DD	3512651629
	DD	1014080696
	DD	0
	DD	1072693248
	DD	373017919
	DD	3218119072
	DD	3647199073
	DD	3162144529
	DD	0
	DD	1070596096
	DD	933359764
	DD	1069535705
	DD	1391441088
	DD	3215896667
	DD	53106651
	DD	1067635358
	DD	4157835043
	DD	3213367904
	DD	4055868050
	DD	1064775611
	DD	1529330423
	DD	3210848884
	DD	325278885
	DD	1062325273
	DD	2667571783
	DD	1072140251
	DD	1614402419
	DD	3162433656
	DD	0
	DD	1072693248
	DD	4148643959
	DD	3218099843
	DD	2887822595
	DD	3161698583
	DD	0
	DD	1070596096
	DD	4101770849
	DD	1069511300
	DD	1167052501
	DD	3215851606
	DD	2569727718
	DD	1067616256
	DD	1681502493
	DD	3213321931
	DD	3685967947
	DD	1064733780
	DD	2973319847
	DD	3210805911
	DD	472683240
	DD	1062549181
	DD	2981026194
	DD	1072114515
	DD	924239062
	DD	1015563563
	DD	0
	DD	1072693248
	DD	3747221382
	DD	3218080689
	DD	1912303920
	DD	3161550059
	DD	0
	DD	1070596096
	DD	1727686788
	DD	1069487445
	DD	681802874
	DD	3215807238
	DD	2531789465
	DD	1067597711
	DD	331202638
	DD	3213277217
	DD	2627596737
	DD	1064693515
	DD	566979211
	DD	3210764596
	DD	2424396143
	DD	1061741710
	DD	3294480606
	DD	1072088779
	DD	1489607747
	DD	3150370331
	DD	0
	DD	1072693248
	DD	1167858038
	DD	3218043472
	DD	2180016084
	DD	3158357424
	DD	0
	DD	1070596096
	DD	772792549
	DD	1069464130
	DD	3356872770
	DD	3215763542
	DD	3524844777
	DD	1067579707
	DD	1521756467
	DD	3213233715
	DD	3931969740
	DD	1064654734
	DD	2908114613
	DD	3210710011
	DD	3523476919
	DD	1061374592
	DD	3607935018
	DD	1072063043
	DD	4202814986
	DD	3163047941
	DD	0
	DD	1072693248
	DD	955408364
	DD	3218005450
	DD	2659929740
	DD	1013295819
	DD	0
	DD	1070596096
	DD	659174847
	DD	1069441347
	DD	2081794994
	DD	3215720500
	DD	2956616392
	DD	1067562230
	DD	1461867065
	DD	3213191381
	DD	879427918
	DD	1064617384
	DD	209598599
	DD	3210633576
	DD	60948554
	DD	1061529630
	DD	3921389429
	DD	1072037307
	DD	3647185164
	DD	1014948546
	DD	0
	DD	1072693248
	DD	1526324265
	DD	3217967566
	DD	4056331296
	DD	1010311990
	DD	0
	DD	1070596096
	DD	1830915900
	DD	1069419088
	DD	602185706
	DD	3215678092
	DD	555210277
	DD	1067545266
	DD	3690136761
	DD	3213150171
	DD	4115917121
	DD	1064581405
	DD	589230607
	DD	3210560017
	DD	3446641745
	DD	1062382364
	DD	4234843841
	DD	1072011571
	DD	1479868884
	DD	3161565806
	DD	0
	DD	1072693248
	DD	2049842017
	DD	3217929817
	DD	2491067243
	DD	1013757722
	DD	0
	DD	1070596096
	DD	1431833764
	DD	1069397346
	DD	551048935
	DD	3215636299
	DD	2575917112
	DD	1067528800
	DD	1559777140
	DD	3213110045
	DD	743322344
	DD	1064546764
	DD	1974991822
	DD	3210489213
	DD	3456487690
	DD	1062460308
	DD	253330956
	DD	1071985836
	DD	991505830
	DD	1015997488
	DD	0
	DD	1072693248
	DD	1894214069
	DD	3217892200
	DD	2182661104
	DD	3159506151
	DD	0
	DD	1070596096
	DD	1869133310
	DD	1069376113
	DD	1368144043
	DD	3215595103
	DD	242783364
	DD	1067512821
	DD	3692616565
	DD	3213070961
	DD	2890683542
	DD	1064513419
	DD	2407947192
	DD	3210421028
	DD	3811448635
	DD	1060561671
	DD	566785368
	DD	1071960100
	DD	2301849815
	DD	1013610078
	DD	0
	DD	1072693248
	DD	620845047
	DD	3217854712
	DD	314567190
	DD	3160767875
	DD	0
	DD	1070596096
	DD	2198158867
	DD	1069355382
	DD	222370857
	DD	3215554487
	DD	4028822361
	DD	1067497314
	DD	3610397327
	DD	3213032882
	DD	171354848
	DD	1064481283
	DD	693369877
	DD	3210355362
	DD	164303632
	DD	1061632559
	DD	880239780
	DD	1071934364
	DD	4135548218
	DD	3162614016
	DD	0
	DD	1072693248
	DD	2273547475
	DD	3217817349
	DD	4162051772
	DD	3158862098
	DD	0
	DD	1070596096
	DD	2392983423
	DD	1069335146
	DD	526970473
	DD	3215514433
	DD	628322840
	DD	1067482270
	DD	924033682
	DD	3212995771
	DD	854746898
	DD	1064450346
	DD	4004963123
	DD	3210292086
	DD	1474471147
	DD	1060747995
	DD	1193694191
	DD	1071908628
	DD	1890859349
	DD	1015418982
	DD	0
	DD	1072693248
	DD	2488075121
	DD	3217780109
	DD	1856288899
	DD	1013767581
	DD	0
	DD	1070596096
	DD	3322930631
	DD	1069315398
	DD	3277607933
	DD	3215474924
	DD	130253043
	DD	1067467675
	DD	1057196725
	DD	3212959591
	DD	3278604843
	DD	1064420531
	DD	4197429676
	DD	3210231108
	DD	1627850508
	DD	1062399550
	DD	1507148603
	DD	1071882892
	DD	1086374101
	DD	3159470481
	DD	0
	DD	1072693248
	DD	1371601499
	DD	3217742989
	DD	3050542164
	DD	3159667536
	DD	0
	DD	1070596096
	DD	2434996648
	DD	1069296132
	DD	2688049294
	DD	3215435945
	DD	1586964757
	DD	1067453518
	DD	2102156419
	DD	3212924308
	DD	2974407249
	DD	1064391847
	DD	249545848
	DD	3210172310
	DD	118754132
	DD	1061128535
	DD	1820603015
	DD	1071857156
	DD	3236194699
	DD	3163192522
	DD	0
	DD	1072693248
	DD	3497432648
	DD	3217705985
	DD	3403982176
	DD	3159219350
	DD	0
	DD	1070596096
	DD	2322009458
	DD	1069277341
	DD	3303184211
	DD	3215397479
	DD	3079386980
	DD	1067429210
	DD	3158396776
	DD	3212889889
	DD	500122468
	DD	1064364204
	DD	3226667041
	DD	3210115604
	DD	1839377740
	DD	1062167069
	DD	2134057426
	DD	1071831420
	DD	1285458442
	DD	1014659385
	DD	0
	DD	1072693248
	DD	719981842
	DD	3217669096
	DD	3396799415
	DD	1013109014
	DD	0
	DD	1070596096
	DD	2111724540
	DD	1069259019
	DD	2754706540
	DD	3215359511
	DD	790037588
	DD	1067402587
	DD	4097286835
	DD	3212856302
	DD	2918356813
	DD	1064337614
	DD	980543884
	DD	3210060878
	DD	1759882364
	DD	3204685716
	DD	2447511838
	DD	1071805684
	DD	3841595606
	DD	3161854967
	DD	0
	DD	1072693248
	DD	2234510834
	DD	3217632317
	DD	1077830624
	DD	3156276711
	DD	0
	DD	1070596096
	DD	1741599179
	DD	1069241160
	DD	288887764
	DD	3215322026
	DD	438316229
	DD	1067376778
	DD	2074161178
	DD	3212810170
	DD	2132352512
	DD	1064311981
	DD	321000190
	DD	3210008077
	DD	2574541397
	DD	1061549566
	DD	2760966249
	DD	1071779948
	DD	1958126117
	DD	1015852907
	DD	0
	DD	1072693248
	DD	212483252
	DD	3217595647
	DD	3496985358
	DD	1013615952
	DD	0
	DD	1070596096
	DD	1939348811
	DD	1069223758
	DD	1001569645
	DD	3215285008
	DD	764537997
	DD	1067351764
	DD	2326690580
	DD	3212746141
	DD	60750538
	DD	1064270005
	DD	2745665118
	DD	3209957083
	DD	1586280401
	DD	1060807371
	DD	3074420661
	DD	1071754212
	DD	1873363667
	DD	1013031755
	DD	0
	DD	1072693248
	DD	4156511795
	DD	3217559081
	DD	1791782369
	DD	3160930544
	DD	0
	DD	1070596096
	DD	4204226438
	DD	1069206807
	DD	2895376501
	DD	3215248443
	DD	3600185398
	DD	1067327526
	DD	2087275384
	DD	3212683597
	DD	347703885
	DD	1064222543
	DD	301495371
	DD	3209907826
	DD	876688468
	DD	3206713553
	DD	3387875073
	DD	1071728476
	DD	3168927931
	DD	3162758597
	DD	0
	DD	1072693248
	DD	2240974125
	DD	3217522619
	DD	3786179077
	DD	1012998644
	DD	0
	DD	1070596096
	DD	2199065489
	DD	1069190303
	DD	1118792048
	DD	3215212318
	DD	875809397
	DD	1067304048
	DD	2927616918
	DD	3212622482
	DD	3494337727
	DD	1064176822
	DD	4219965681
	DD	3209860241
	DD	4249954841
	DD	1060574506
	DD	3701329484
	DD	1071702740
	DD	2857479637
	DD	1015274401
	DD	0
	DD	1072693248
	DD	4257115870
	DD	3217486256
	DD	39728399
	DD	3160493923
	DD	0
	DD	1070596096
	DD	3207767847
	DD	1069174239
	DD	797077063
	DD	3215176618
	DD	287980713
	DD	1067281311
	DD	1596666478
	DD	3212562743
	DD	2775364
	DD	1064132847
	DD	3823518214
	DD	3209814240
	DD	3101392498
	DD	1060290467
	DD	4014783896
	DD	1071677004
	DD	3119156846
	DD	3160517595
	DD	0
	DD	1072693248
	DD	2953879462
	DD	3217449991
	DD	651182982
	DD	3159737324
	DD	0
	DD	1070596096
	DD	2348802841
	DD	1069158611
	DD	4095204310
	DD	3215141329
	DD	3730066556
	DD	1067259298
	DD	2541564470
	DD	3212504327
	DD	1012137407
	DD	1064090541
	DD	3007493349
	DD	3209769752
	DD	304719606
	DD	1058732349
	DD	33271012
	DD	1071651269
	DD	2269574412
	DD	3163337103
	DD	0
	DD	1072693248
	DD	4098310206
	DD	3217413820
	DD	678867490
	DD	1013738598
	DD	0
	DD	1070596096
	DD	4033975882
	DD	1069143413
	DD	1577500332
	DD	3215106440
	DD	2904242360
	DD	1067237995
	DD	383406633
	DD	3212447185
	DD	511745182
	DD	1064049810
	DD	3472404602
	DD	3209726723
	DD	3493824410
	DD	1061932631
	DD	693450846
	DD	1071606394
	DD	3218699017
	DD	1014370223
	DD	0
	DD	1072693248
	DD	701484222
	DD	3217377742
	DD	1189134796
	DD	1013549080
	DD	0
	DD	1070596096
	DD	478132658
	DD	1069128642
	DD	2223654597
	DD	3215071936
	DD	706118045
	DD	1067217386
	DD	1066336370
	DD	3212391267
	DD	3310393795
	DD	1064010705
	DD	1920669086
	DD	3209678984
	DD	1209448644
	DD	3206743753
	DD	1320359670
	DD	1071554922
	DD	1908355032
	DD	3162144129
	DD	0
	DD	1072693248
	DD	3079104278
	DD	3217341752
	DD	2593237472
	DD	3159688198
	DD	0
	DD	1070596096
	DD	1749059456
	DD	1069114291
	DD	3316773722
	DD	3215037805
	DD	2555119580
	DD	1067197455
	DD	59306268
	DD	3212336527
	DD	1944001738
	DD	1063973040
	DD	3607966944
	DD	3209598336
	DD	1548002341
	DD	1061515569
	DD	1947268493
	DD	1071503450
	DD	1185916272
	DD	3160806757
	DD	0
	DD	1072693248
	DD	192708373
	DD	3217305850
	DD	1013564964
	DD	3160609117
	DD	0
	DD	1070596096
	DD	1393443609
	DD	1069100357
	DD	3282624502
	DD	3215004035
	DD	3423023493
	DD	1067178189
	DD	3539283610
	DD	3212282918
	DD	1358653293
	DD	1063936902
	DD	1383848795
	DD	3209520160
	DD	1170879540
	DD	3207439782
	DD	2574177316
	DD	1071451978
	DD	2889755040
	DD	1011982448
	DD	0
	DD	1072693248
	DD	2595342993
	DD	3217270031
	DD	1222182906
	DD	3159411259
	DD	0
	DD	1070596096
	DD	2192927435
	DD	1069086835
	DD	1645422722
	DD	3214970614
	DD	648269084
	DD	1067159575
	DD	2319737250
	DD	3212230398
	DD	1121494038
	DD	1063902126
	DD	897314833
	DD	3209444434
	DD	2659165416
	DD	1060259181
	DD	3201086139
	DD	1071400506
	DD	4185319304
	DD	1014225642
	DD	0
	DD	1072693248
	DD	3772940467
	DD	3217234294
	DD	3998164722
	DD	1013347606
	DD	0
	DD	1070596096
	DD	3265990537
	DD	1069073721
	DD	3279971426
	DD	3214937529
	DD	2447085124
	DD	1067141598
	DD	1649985592
	DD	3212178923
	DD	3334399491
	DD	1063868754
	DD	454991634
	DD	3209370985
	DD	1938913543
	DD	3204215126
	DD	3827994963
	DD	1071349034
	DD	941734744
	DD	3162288710
	DD	0
	DD	1072693248
	DD	1615317960
	DD	3217198637
	DD	1400808192
	DD	1007479474
	DD	0
	DD	1070596096
	DD	55251407
	DD	1069061012
	DD	2380564598
	DD	3214875524
	DD	2078095393
	DD	1067124247
	DD	3759475360
	DD	3212128451
	DD	800747756
	DD	1063836662
	DD	3361780736
	DD	3209299766
	DD	3643416391
	DD	1062249457
	DD	159936490
	DD	1071297563
	DD	3547642993
	DD	3161095918
	DD	0
	DD	1072693248
	DD	2707383656
	DD	3217163056
	DD	554608178
	DD	3160521551
	DD	0
	DD	1070596096
	DD	1495139838
	DD	1069048702
	DD	183291521
	DD	3214810633
	DD	3540584774
	DD	1067107509
	DD	3317896890
	DD	3212078943
	DD	3844035447
	DD	1063805952
	DD	3695033041
	DD	3209230570
	DD	3731872264
	DD	3207727795
	DD	786845313
	DD	1071246091
	DD	627570489
	DD	1010932411
	DD	0
	DD	1072693248
	DD	850612498
	DD	3217127550
	DD	3957837480
	DD	1012739461
	DD	0
	DD	1070596096
	DD	4230382869
	DD	1069036788
	DD	2678394493
	DD	3214746346
	DD	42048419
	DD	1067091374
	DD	61549345
	DD	3212030360
	DD	3326100968
	DD	1063776440
	DD	377176410
	DD	3209163411
	DD	1539171840
	DD	3206122484
	DD	1413754136
	DD	1071194619
	DD	856972295
	DD	1014081062
	DD	0
	DD	1072693248
	DD	2829232582
	DD	3217092115
	DD	1037840994
	DD	1012412582
	DD	0
	DD	1070596096
	DD	1194701220
	DD	1069025268
	DD	1485063562
	DD	3214682643
	DD	3996610205
	DD	1067075828
	DD	2865183303
	DD	3211982662
	DD	2185691626
	DD	1063748117
	DD	435906073
	DD	3209098183
	DD	1583986254
	DD	1061922957
	DD	2040662960
	DD	1071143147
	DD	4270081753
	DD	3162433290
	DD	0
	DD	1072693248
	DD	2636871548
	DD	3217056750
	DD	3635907996
	DD	3159234439
	DD	0
	DD	1070596096
	DD	3074865967
	DD	1069014136
	DD	2827594877
	DD	3214619501
	DD	4018616278
	DD	1067060863
	DD	3744630852
	DD	3211935814
	DD	967547233
	DD	1063721046
	DD	1523268333
	DD	3209034706
	DD	370739821
	DD	1057912410
	DD	2667571783
	DD	1071091675
	DD	1614402419
	DD	3161385080
	DD	0
	DD	1072693248
	DD	1600848691
	DD	3217011737
	DD	3799011534
	DD	3158825865
	DD	0
	DD	1070596096
	DD	3940625627
	DD	1069003390
	DD	1694054617
	DD	3214556900
	DD	3525212904
	DD	1067046468
	DD	3400170401
	DD	3211889780
	DD	2025409789
	DD	1063695095
	DD	1861695371
	DD	3208972985
	DD	2099431114
	DD	1058280175
	DD	3294480606
	DD	1071040203
	DD	1489607747
	DD	3149321755
	DD	0
	DD	1072693248
	DD	163325898
	DD	3216941271
	DD	272531312
	DD	3158872717
	DD	0
	DD	1070596096
	DD	2709640876
	DD	1068993027
	DD	3241569416
	DD	3214494818
	DD	3499770285
	DD	1067032633
	DD	2428916378
	DD	3211844525
	DD	796870242
	DD	1063670282
	DD	2401385551
	DD	3208912885
	DD	417337253
	DD	3207953615
	DD	3921389429
	DD	1070988731
	DD	3647185164
	DD	1013899970
	DD	0
	DD	1072693248
	DD	2357811071
	DD	3216870929
	DD	1815527340
	DD	3157476592
	DD	0
	DD	1070596096
	DD	3138003653
	DD	1068983043
	DD	1253835798
	DD	3214433236
	DD	2444914170
	DD	1067019349
	DD	1113961907
	DD	3211800015
	DD	1725681990
	DD	1063646503
	DD	2521427548
	DD	3208854407
	DD	2469316955
	DD	1061668474
	DD	253330956
	DD	1070937260
	DD	991505830
	DD	1014948912
	DD	0
	DD	1072693248
	DD	1152452440
	DD	3216800708
	DD	2988871182
	DD	1011946816
	DD	0
	DD	1070596096
	DD	926263936
	DD	1068973436
	DD	3845070713
	DD	3214372132
	DD	626397172
	DD	1067006607
	DD	2777085683
	DD	3211724145
	DD	2172777456
	DD	1063623822
	DD	3188732906
	DD	3208797386
	DD	250185439
	DD	1062033866
	DD	880239780
	DD	1070885788
	DD	4135548218
	DD	3161565440
	DD	0
	DD	1072693248
	DD	2555242223
	DD	3216730602
	DD	1132402578
	DD	1012350135
	DD	0
	DD	1070596096
	DD	890626350
	DD	1068964202
	DD	3331100648
	DD	3214311488
	DD	30508518
	DD	1066994398
	DD	397665884
	DD	3211637908
	DD	1048931291
	DD	1063602233
	DD	4152525626
	DD	3208741715
	DD	51864355
	DD	3208005790
	DD	1507148603
	DD	1070834316
	DD	1086374101
	DD	3158421905
	DD	0
	DD	1072693248
	DD	4132779814
	DD	3216660607
	DD	3895880616
	DD	1012226511
	DD	0
	DD	1070596096
	DD	4069764223
	DD	1068955338
	DD	1936654463
	DD	3214251284
	DD	27137259
	DD	1066982714
	DD	112447739
	DD	3211552967
	DD	3681308921
	DD	1063581561
	DD	3611917249
	DD	3208687484
	DD	2182292750
	DD	1061868473
	DD	2134057426
	DD	1070782844
	DD	1285458442
	DD	1013610809
	DD	0
	DD	1072693248
	DD	3593905368
	DD	3216590719
	DD	2989246530
	DD	1011901384
	DD	0
	DD	1070596096
	DD	3421947019
	DD	1068946843
	DD	3144465174
	DD	3214191500
	DD	1618007405
	DD	1066971547
	DD	584787122
	DD	3211469261
	DD	1465845707
	DD	1063561956
	DD	939979903
	DD	3208626315
	DD	1695262346
	DD	3204506663
	DD	2760966249
	DD	1070731372
	DD	1958126117
	DD	1014804331
	DD	0
	DD	1072693248
	DD	3078460826
	DD	3216520933
	DD	4285081068
	DD	3159473614
	DD	0
	DD	1070596096
	DD	997373752
	DD	1068938714
	DD	3045840714
	DD	3214132118
	DD	3110377520
	DD	1066960890
	DD	2323415564
	DD	3211386730
	DD	18309569
	DD	1063543250
	DD	551124926
	DD	3208522770
	DD	249182975
	DD	1061811122
	DD	3387875073
	DD	1070679900
	DD	3168927931
	DD	3161710021
	DD	0
	DD	1072693248
	DD	561239310
	DD	3216451245
	DD	210497392
	DD	3156201161
	DD	0
	DD	1070596096
	DD	3636030687
	DD	1068930947
	DD	3167463735
	DD	3214073119
	DD	73152643
	DD	1066950737
	DD	3108019154
	DD	3211305316
	DD	2100498319
	DD	1063525559
	DD	282708930
	DD	3208421353
	DD	2551775895
	DD	3207605450
	DD	4014783896
	DD	1070628428
	DD	3119156846
	DD	3159469019
	DD	0
	DD	1072693248
	DD	2730856872
	DD	3216381649
	DD	4254293712
	DD	1009711154
	DD	0
	DD	1070596096
	DD	1486032260
	DD	1068923542
	DD	1824674436
	DD	3214014485
	DD	2488677748
	DD	1066941079
	DD	1925770461
	DD	3211224962
	DD	2531500940
	DD	1063508710
	DD	3007508061
	DD	3208322267
	DD	994458799
	DD	1061806090
	DD	693450846
	DD	1070557818
	DD	3218699017
	DD	1013321647
	DD	0
	DD	1072693248
	DD	3508967486
	DD	3216312142
	DD	3021155724
	DD	1011969550
	DD	0
	DD	1070596096
	DD	2356881417
	DD	1068916495
	DD	2655870529
	DD	3213956197
	DD	352865498
	DD	1066931912
	DD	3008575053
	DD	3211145611
	DD	2622088932
	DD	1063492845
	DD	3834913422
	DD	3208225014
	DD	1497815285
	DD	3207511002
	DD	1947268493
	DD	1070454874
	DD	1185916272
	DD	3159758181
	DD	0
	DD	1072693248
	DD	1224261120
	DD	3216242720
	DD	3122470664
	DD	3159031070
	DD	0
	DD	1070596096
	DD	1648557593
	DD	1068909805
	DD	1683250226
	DD	3213898238
	DD	2306165388
	DD	1066923228
	DD	2600821843
	DD	3211067209
	DD	3406795398
	DD	1063477798
	DD	3582359304
	DD	3208129845
	DD	3199024961
	DD	1061038571
	DD	3201086139
	DD	1070351930
	DD	4185319304
	DD	1013177066
	DD	0
	DD	1072693248
	DD	2901633254
	DD	3216173377
	DD	2788221280
	DD	1009319756
	DD	0
	DD	1070596096
	DD	3820542338
	DD	1068903469
	DD	288942435
	DD	3213795740
	DD	1237884341
	DD	1066915023
	DD	1606294444
	DD	3210989702
	DD	223992189
	DD	1063463656
	DD	1499707213
	DD	3208036378
	DD	3416758850
	DD	1060053511
	DD	159936490
	DD	1070248987
	DD	3547642993
	DD	3160047342
	DD	0
	DD	1072693248
	DD	2781619933
	DD	3216104110
	DD	3145354644
	DD	1011963772
	DD	0
	DD	1070596096
	DD	911495412
	DD	1068897487
	DD	290976546
	DD	3213681030
	DD	33241894
	DD	1066907291
	DD	977031819
	DD	3210913037
	DD	119357659
	DD	1063450345
	DD	1846248446
	DD	3207944648
	DD	4276662282
	DD	1061269448
	DD	1413754136
	DD	1070146043
	DD	856972295
	DD	1013032486
	DD	0
	DD	1072693248
	DD	3789571175
	DD	3216034914
	DD	2061677164
	DD	3158802525
	DD	0
	DD	1070596096
	DD	2598855658
	DD	1068891855
	DD	628750565
	DD	3213566872
	DD	2954249465
	DD	1066900026
	DD	1468236324
	DD	3210837162
	DD	3701994667
	DD	1063437912
	DD	310421843
	DD	3207854373
	DD	393637210
	DD	3207421006
	DD	2667571783
	DD	1070043099
	DD	1614402419
	DD	3160336504
	DD	0
	DD	1072693248
	DD	700360492
	DD	3215948980
	DD	4067889956
	DD	3158309941
	DD	0
	DD	1070596096
	DD	1834243786
	DD	1068886573
	DD	92010622
	DD	3213453232
	DD	2733460853
	DD	1066893225
	DD	3349627764
	DD	3210762026
	DD	2326014508
	DD	1063426279
	DD	2491557920
	DD	3207765637
	DD	2159546056
	DD	3208108974
	DD	3921389429
	DD	1069940155
	DD	3647185164
	DD	1012851394
	DD	0
	DD	1072693248
	DD	4008519973
	DD	3215810847
	DD	1156806192
	DD	3157002565
	DD	0
	DD	1070596096
	DD	608713188
	DD	1068881639
	DD	3200221055
	DD	3213340075
	DD	2024185290
	DD	1066886883
	DD	3737414605
	DD	3210635448
	DD	4102026093
	DD	1063415403
	DD	3274310324
	DD	3207678452
	DD	4280744879
	DD	1061910091
	DD	880239780
	DD	1069837212
	DD	4135548218
	DD	3160516864
	DD	0
	DD	1072693248
	DD	2381996637
	DD	3215672832
	DD	639270832
	DD	1008532593
	DD	0
	DD	1070596096
	DD	1358563144
	DD	1068877051
	DD	2925458633
	DD	3213227369
	DD	492833071
	DD	1066880996
	DD	4104628456
	DD	3210487835
	DD	3054991409
	DD	1063405423
	DD	2594255061
	DD	3207590366
	DD	2100320188
	DD	3208479772
	DD	2134057426
	DD	1069734268
	DD	1285458442
	DD	1012562233
	DD	0
	DD	1072693248
	DD	2319612510
	DD	3215534925
	DD	1111071278
	DD	3158888695
	DD	0
	DD	1070596096
	DD	2666415304
	DD	1068872808
	DD	2071510147
	DD	3213115080
	DD	3395467108
	DD	1066875559
	DD	2423946868
	DD	3210341405
	DD	80612299
	DD	1063396131
	DD	2656867111
	DD	3207420929
	DD	3004001956
	DD	1061836610
	DD	3387875073
	DD	1069631324
	DD	3168927931
	DD	3160661445
	DD	0
	DD	1072693248
	DD	1865079417
	DD	3215397118
	DD	2937833612
	DD	1011350737
	DD	0
	DD	1070596096
	DD	3552516355
	DD	1068868909
	DD	294617109
	DD	3213003175
	DD	76221622
	DD	1066870571
	DD	3376425735
	DD	3210196061
	DD	2723275475
	DD	1063387705
	DD	3315521969
	DD	3207252871
	DD	3590370444
	DD	3207679867
	DD	693450846
	DD	1069509242
	DD	3218699017
	DD	1012273071
	DD	0
	DD	1072693248
	DD	3481201957
	DD	3215259402
	DD	1636311970
	DD	3157709699
	DD	0
	DD	1070596096
	DD	3471360849
	DD	1068865353
	DD	2600335524
	DD	3212891620
	DD	2613792462
	DD	1066866026
	DD	3847698423
	DD	3210051709
	DD	663967578
	DD	1063380003
	DD	493409710
	DD	3207087119
	DD	1127477260
	DD	3207199264
	DD	3201086139
	DD	1069303354
	DD	4185319304
	DD	1012128490
	DD	0
	DD	1072693248
	DD	1154356116
	DD	3215121770
	DD	1840040117
	DD	3159097207
	DD	0
	DD	1070596096
	DD	2308597781
	DD	1068862139
	DD	4143524575
	DD	3212723904
	DD	2850915343
	DD	1066861923
	DD	1191668407
	DD	3209908256
	DD	2952843839
	DD	1063373033
	DD	3172237721
	DD	3206923432
	DD	2684115468
	DD	1061237060
	DD	1413754136
	DD	1069097467
	DD	856972295
	DD	1011983910
	DD	0
	DD	1072693248
	DD	1858778712
	DD	3214984212
	DD	552307712
	DD	1007653492
	DD	0
	DD	1070596096
	DD	378217452
	DD	1068859266
	DD	2507068679
	DD	3212502004
	DD	2446302718
	DD	1066858259
	DD	1361087111
	DD	3209765608
	DD	761263112
	DD	1063366881
	DD	2488333367
	DD	3206760542
	DD	3134826447
	DD	3208438003
	DD	3921389429
	DD	1068891579
	DD	3647185164
	DD	1011802818
	DD	0
	DD	1072693248
	DD	3847531748
	DD	3214759425
	DD	2484055616
	DD	1005679197
	DD	0
	DD	1070596096
	DD	2714982805
	DD	1068856732
	DD	2649074978
	DD	3212280611
	DD	4275478210
	DD	1066855031
	DD	2325700054
	DD	3209556212
	DD	3568629394
	DD	1063361434
	DD	1419097007
	DD	3206599479
	DD	3667282369
	DD	3208404843
	DD	2134057426
	DD	1068685692
	DD	1285458442
	DD	1011513657
	DD	0
	DD	1072693248
	DD	4111272116
	DD	3214484558
	DD	3215369808
	DD	3153450047
	DD	0
	DD	1070596096
	DD	1892298670
	DD	1068854538
	DD	4017792039
	DD	3212059661
	DD	3825916209
	DD	1066852238
	DD	1674419437
	DD	3209273590
	DD	2761556401
	DD	1063356717
	DD	4123887123
	DD	3206334117
	DD	39803485
	DD	3207166447
	DD	693450846
	DD	1068460666
	DD	3218699017
	DD	1011224495
	DD	0
	DD	1072693248
	DD	1769817487
	DD	3214209791
	DD	226900428
	DD	3157084142
	DD	0
	DD	1070596096
	DD	3790025843
	DD	1068852682
	DD	2975452701
	DD	3211839091
	DD	2074002222
	DD	1066849878
	DD	2835406423
	DD	3208992033
	DD	2160688582
	DD	1063352737
	DD	438342636
	DD	3206016666
	DD	598064701
	DD	3205441692
	DD	1413754136
	DD	1068048891
	DD	856972295
	DD	1010935334
	DD	0
	DD	1072693248
	DD	2796861084
	DD	3213935106
	DD	4160910146
	DD	1008936729
	DD	0
	DD	1070596096
	DD	1822957307
	DD	1068851165
	DD	2426092900
	DD	3211449386
	DD	1181651297
	DD	1066847949
	DD	11113416
	DD	3208711363
	DD	2350522541
	DD	1063349490
	DD	1110749705
	DD	3205700769
	DD	1529834450
	DD	3204806002
	DD	2134057426
	DD	1067637116
	DD	1285458442
	DD	1010465081
	DD	0
	DD	1072693248
	DD	766684177
	DD	3213435536
	DD	4183922317
	DD	3156428395
	DD	0
	DD	1070596096
	DD	2709169804
	DD	1068849985
	DD	2562652885
	DD	3211009382
	DD	194304310
	DD	1066846450
	DD	285423324
	DD	3208220240
	DD	4164698302
	DD	1063346958
	DD	490316583
	DD	3205276877
	DD	1161999017
	DD	1059948714
	DD	1413754136
	DD	1067000315
	DD	856972295
	DD	1009886758
	DD	0
	DD	1072693248
	DD	1373484687
	DD	3212886398
	DD	3463806808
	DD	3156987378
	DD	0
	DD	1070596096
	DD	699034059
	DD	1068849143
	DD	4196075177
	DD	3210399801
	DD	3624794075
	DD	1066845379
	DD	1212360239
	DD	3207661374
	DD	4225594870
	DD	1063345136
	DD	4142831282
	DD	3204654696
	DD	3419780486
	DD	1061860029
	DD	1413754136
	DD	1065951739
	DD	856972295
	DD	1008838182
	DD	0
	DD	1072693248
	DD	1048210722
	DD	3211837789
	DD	322286546
	DD	3154320084
	DD	0
	DD	1070596096
	DD	3344097458
	DD	1068848637
	DD	4078525744
	DD	3209350973
	DD	4268457311
	DD	1066844737
	DD	1752199129
	DD	3206612445
	DD	2933973771
	DD	1063344076
	DD	1552329397
	DD	3203604713
	DD	4286470131
	DD	1060555865
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1072693248
	DD	0
	DD	0
	DD	0
	DD	0
	DD	0
	DD	1070596096
	DD	1431655765
	DD	1068848469
	DD	0
	DD	0
	DD	381986942
	DD	1066844524
	DD	0
	DD	0
	DD	3364860459
	DD	1063343704
	DD	0
	DD	0
	DD	3669941704
	DD	1061551801
	DD	1413754136
	DD	3213435387
	DD	856972295
	DD	3156321830
	DD	0
	DD	1072693248
	DD	1048210722
	DD	1064354141
	DD	310222654
	DD	1006836436
	DD	0
	DD	1070596096
	DD	3344097458
	DD	1068848637
	DD	4078525744
	DD	1061867325
	DD	4268457311
	DD	1066844737
	DD	1752199185
	DD	1059128797
	DD	2933974085
	DD	1063344076
	DD	1541627965
	DD	1056121065
	DD	4213857106
	DD	1060555865
	DD	1413754136
	DD	3214483963
	DD	856972295
	DD	3157370406
	DD	0
	DD	1072693248
	DD	1373484687
	DD	1065402750
	DD	3463778643
	DD	1009503730
	DD	0
	DD	1070596096
	DD	699034059
	DD	1068849143
	DD	4196075177
	DD	1062916153
	DD	3624794075
	DD	1066845379
	DD	1212360238
	DD	1060177726
	DD	4225594899
	DD	1063345136
	DD	4143059094
	DD	1057171048
	DD	3416473992
	DD	1061860029
	DD	2134057426
	DD	3215120764
	DD	1285458442
	DD	3157948729
	DD	0
	DD	1072693248
	DD	766684177
	DD	1065951888
	DD	4184103028
	DD	1008944747
	DD	0
	DD	1070596096
	DD	2709169804
	DD	1068849985
	DD	2562652885
	DD	1063525734
	DD	194304310
	DD	1066846450
	DD	285423316
	DD	1060736592
	DD	4164698149
	DD	1063346958
	DD	492080240
	DD	1057793229
	DD	1233082996
	DD	1059948714
	DD	1413754136
	DD	3215532539
	DD	856972295
	DD	3158418982
	DD	0
	DD	1072693248
	DD	2796861084
	DD	1066451458
	DD	4163800696
	DD	3156420377
	DD	0
	DD	1070596096
	DD	1822957307
	DD	1068851165
	DD	2426092900
	DD	1063965738
	DD	1181651297
	DD	1066847949
	DD	11113467
	DD	1061227715
	DD	2350524377
	DD	1063349490
	DD	1099629520
	DD	1058217121
	DD	647421766
	DD	3204806003
	DD	693450846
	DD	3215944314
	DD	3218699017
	DD	3158708143
	DD	0
	DD	1072693248
	DD	1769817487
	DD	1066726143
	DD	226750770
	DD	1009600494
	DD	0
	DD	1070596096
	DD	3790025843
	DD	1068852682
	DD	2975452701
	DD	1064355443
	DD	2074002222
	DD	1066849878
	DD	2835406424
	DD	1061508385
	DD	2160688720
	DD	1063352737
	DD	438143169
	DD	1058533018
	DD	848462310
	DD	3205441692
	DD	2134057426
	DD	3216169340
	DD	1285458442
	DD	3158997305
	DD	0
	DD	1072693248
	DD	4111272116
	DD	1067000910
	DD	3213429568
	DD	1005966399
	DD	0
	DD	1070596096
	DD	1892298670
	DD	1068854538
	DD	4017792039
	DD	1064576013
	DD	3825916209
	DD	1066852238
	DD	1674419438
	DD	1061789942
	DD	2761556545
	DD	1063356717
	DD	4123711083
	DD	1058850469
	DD	104595699
	DD	3207166447
	DD	3921389429
	DD	3216375227
	DD	3647185164
	DD	3159286466
	DD	0
	DD	1072693248
	DD	3847531748
	DD	1067275777
	DD	2487971824
	DD	3153162845
	DD	0
	DD	1070596096
	DD	2714982805
	DD	1068856732
	DD	2649074978
	DD	1064796963
	DD	4275478210
	DD	1066855031
	DD	2325700056
	DD	1062072564
	DD	3568629551
	DD	1063361434
	DD	1418922081
	DD	1059115831
	DD	3702148884
	DD	3208404843
	DD	1413754136
	DD	3216581115
	DD	856972295
	DD	3159467558
	DD	0
	DD	1072693248
	DD	1858778712
	DD	1067500564
	DD	554450480
	DD	3155137140
	DD	0
	DD	1070596096
	DD	378217452
	DD	1068859266
	DD	2507068679
	DD	1065018356
	DD	2446302718
	DD	1066858259
	DD	1361087112
	DD	1062281960
	DD	761263211
	DD	1063366881
	DD	2488165471
	DD	1059276894
	DD	3156241770
	DD	3208438003
	DD	3201086139
	DD	3216787002
	DD	4185319304
	DD	3159612138
	DD	0
	DD	1072693248
	DD	1154356116
	DD	1067638122
	DD	1840130764
	DD	1011613559
	DD	0
	DD	1070596096
	DD	2308597781
	DD	1068862139
	DD	4143524575
	DD	1065240256
	DD	2850915343
	DD	1066861923
	DD	1191668406
	DD	1062424608
	DD	2952843813
	DD	1063373033
	DD	3172428186
	DD	1059439784
	DD	2686776959
	DD	1061237060
	DD	693450846
	DD	3216992890
	DD	3218699017
	DD	3159756719
	DD	0
	DD	1072693248
	DD	3481201957
	DD	1067775754
	DD	1635996196
	DD	1010226051
	DD	0
	DD	1070596096
	DD	3471360849
	DD	1068865353
	DD	2600335524
	DD	1065407972
	DD	2613792462
	DD	1066866026
	DD	3847698425
	DD	1062568061
	DD	663967590
	DD	1063380003
	DD	493016801
	DD	1059603471
	DD	1131084659
	DD	3207199264
	DD	3387875073
	DD	3217114972
	DD	3168927931
	DD	1013177797
	DD	0
	DD	1072693248
	DD	1865079417
	DD	1067913470
	DD	2938003329
	DD	3158834385
	DD	0
	DD	1070596096
	DD	3552516355
	DD	1068868909
	DD	294617109
	DD	1065519527
	DD	76221622
	DD	1066870571
	DD	3376425737
	DD	1062712413
	DD	2723275461
	DD	1063387705
	DD	3315136619
	DD	1059769223
	DD	3585984604
	DD	3207679867
	DD	2134057426
	DD	3217217916
	DD	1285458442
	DD	3160045881
	DD	0
	DD	1072693248
	DD	2319612510
	DD	1068051277
	DD	1111303258
	DD	1011405047
	DD	0
	DD	1070596096
	DD	2666415304
	DD	1068872808
	DD	2071510147
	DD	1065631432
	DD	3395467108
	DD	1066875559
	DD	2423946865
	DD	1062857757
	DD	80612341
	DD	1063396131
	DD	2657457026
	DD	1059937281
	DD	2998464421
	DD	1061836610
	DD	880239780
	DD	3217320860
	DD	4135548218
	DD	1013033216
	DD	0
	DD	1072693248
	DD	2381996637
	DD	1068189184
	DD	639224640
	DD	3156016241
	DD	0
	DD	1070596096
	DD	1358563144
	DD	1068877051
	DD	2925458633
	DD	1065743721
	DD	492833071
	DD	1066880996
	DD	4104628456
	DD	1063004187
	DD	3054991518
	DD	1063405423
	DD	2594252722
	DD	1060106718
	DD	2125241622
	DD	3208479772
	DD	3921389429
	DD	3217423803
	DD	3647185164
	DD	3160335042
	DD	0
	DD	1072693248
	DD	4008519973
	DD	1068327199
	DD	1156786808
	DD	1009518917
	DD	0
	DD	1070596096
	DD	608713188
	DD	1068881639
	DD	3200221055
	DD	1065856427
	DD	2024185290
	DD	1066886883
	DD	3737414605
	DD	1063151800
	DD	4102026005
	DD	1063415403
	DD	3274311301
	DD	1060194804
	DD	4290805031
	DD	1061910091
	DD	2667571783
	DD	3217526747
	DD	1614402419
	DD	1012852856
	DD	0
	DD	1072693248
	DD	700360492
	DD	1068465332
	DD	4067897658
	DD	1010826293
	DD	0
	DD	1070596096
	DD	1834243786
	DD	1068886573
	DD	92010622
	DD	1065969584
	DD	2733460853
	DD	1066893225
	DD	3349627764
	DD	1063278378
	DD	2326014613
	DD	1063426279
	DD	2491557128
	DD	1060281989
	DD	2183550903
	DD	3208108974
	DD	1413754136
	DD	3217629691
	DD	856972295
	DD	3160516134
	DD	0
	DD	1072693248
	DD	3789571175
	DD	1068551266
	DD	2061680020
	DD	1011318877
	DD	0
	DD	1070596096
	DD	2598855658
	DD	1068891855
	DD	628750565
	DD	1066083224
	DD	2954249465
	DD	1066900026
	DD	1468236324
	DD	1063353514
	DD	3701994745
	DD	1063437912
	DD	310421258
	DD	1060370725
	DD	429301190
	DD	3207421006
	DD	159936490
	DD	3217732635
	DD	3547642993
	DD	1012563694
	DD	0
	DD	1072693248
	DD	2781619933
	DD	1068620462
	DD	3145355490
	DD	3159447420
	DD	0
	DD	1070596096
	DD	911495412
	DD	1068897487
	DD	290976546
	DD	1066197382
	DD	33241894
	DD	1066907291
	DD	977031819
	DD	1063429389
	DD	119357612
	DD	1063450345
	DD	1846248792
	DD	1060461000
	DD	4282034711
	DD	1061269448
	DD	3201086139
	DD	3217835578
	DD	4185319304
	DD	3160660714
	DD	0
	DD	1072693248
	DD	2901633254
	DD	1068689729
	DD	2788220944
	DD	3156803404
	DD	0
	DD	1070596096
	DD	3820542338
	DD	1068903469
	DD	288942435
	DD	1066312092
	DD	1237884341
	DD	1066915023
	DD	1606294444
	DD	1063506054
	DD	223992191
	DD	1063463656
	DD	1499707196
	DD	1060552730
	DD	3415844307
	DD	1060053511
	DD	1947268493
	DD	3217938522
	DD	1185916272
	DD	1012274533
	DD	0
	DD	1072693248
	DD	1224261120
	DD	1068759072
	DD	3122469562
	DD	1011547422
	DD	0
	DD	1070596096
	DD	1648557593
	DD	1068909805
	DD	1683250226
	DD	1066414590
	DD	2306165388
	DD	1066923228
	DD	2600821843
	DD	1063583561
	DD	3406795366
	DD	1063477798
	DD	3582359528
	DD	1060646197
	DD	3206340638
	DD	1061038571
	DD	693450846
	DD	3218041466
	DD	3218699017
	DD	3160805295
	DD	0
	DD	1072693248
	DD	3508967486
	DD	1068828494
	DD	3021154406
	DD	3159453198
	DD	0
	DD	1070596096
	DD	2356881417
	DD	1068916495
	DD	2655870529
	DD	1066472549
	DD	352865498
	DD	1066931912
	DD	3008575053
	DD	1063661963
	DD	2622089007
	DD	1063492845
	DD	3834912888
	DD	1060741366
	DD	1532107502
	DD	3207511002
	DD	4014783896
	DD	3218112076
	DD	3119156846
	DD	1011985371
	DD	0
	DD	1072693248
	DD	2730856872
	DD	1068898001
	DD	4254304408
	DD	3157194802
	DD	0
	DD	1070596096
	DD	1486032260
	DD	1068923542
	DD	1824674436
	DD	1066530837
	DD	2488677748
	DD	1066941079
	DD	1925770461
	DD	1063741314
	DD	2531500863
	DD	1063508710
	DD	3007508602
	DD	1060838619
	DD	1003260451
	DD	1061806090
	DD	3387875073
	DD	3218163548
	DD	3168927931
	DD	1014226373
	DD	0
	DD	1072693248
	DD	561239310
	DD	1068967597
	DD	210518352
	DD	1008717513
	DD	0
	DD	1070596096
	DD	3636030687
	DD	1068930947
	DD	3167463735
	DD	1066589471
	DD	73152643
	DD	1066950737
	DD	3108019154
	DD	1063821668
	DD	2100498394
	DD	1063525559
	DD	282708402
	DD	1060937705
	DD	2568921764
	DD	3207605450
	DD	2760966249
	DD	3218215020
	DD	1958126117
	DD	3162287979
	DD	0
	DD	1072693248
	DD	3078460826
	DD	1069037285
	DD	4285079825
	DD	1011989966
	DD	0
	DD	1070596096
	DD	997373752
	DD	1068938714
	DD	3045840714
	DD	1066648470
	DD	3110377520
	DD	1066960890
	DD	2323415564
	DD	1063903082
	DD	18309497
	DD	1063543250
	DD	551125427
	DD	1061039122
	DD	257413032
	DD	1061811122
	DD	2134057426
	DD	3218266492
	DD	1285458442
	DD	3161094457
	DD	0
	DD	1072693248
	DD	3593905368
	DD	1069107071
	DD	2989245764
	DD	3159385032
	DD	0
	DD	1070596096
	DD	3421947019
	DD	1068946843
	DD	3144465174
	DD	1066707852
	DD	1618007405
	DD	1066971547
	DD	584787122
	DD	1063985613
	DD	1465845750
	DD	1063561956
	DD	939979596
	DD	1061142667
	DD	1773905530
	DD	3204506663
	DD	1507148603
	DD	3218317964
	DD	1086374101
	DD	1010938257
	DD	0
	DD	1072693248
	DD	4132779814
	DD	1069176959
	DD	3895881848
	DD	3159710159
	DD	0
	DD	1070596096
	DD	4069764223
	DD	1068955338
	DD	1936654463
	DD	1066767636
	DD	27137259
	DD	1066982714
	DD	112447739
	DD	1064069319
	DD	3681308850
	DD	1063581561
	DD	3611917496
	DD	1061203836
	DD	2190408434
	DD	1061868473
	DD	880239780
	DD	3218369436
	DD	4135548218
	DD	1014081792
	DD	0
	DD	1072693248
	DD	2555242223
	DD	1069246954
	DD	1132401098
	DD	3159833783
	DD	0
	DD	1070596096
	DD	890626350
	DD	1068964202
	DD	3331100648
	DD	1066827840
	DD	30508518
	DD	1066994398
	DD	397665884
	DD	1064154260
	DD	1048931375
	DD	1063602233
	DD	4152525330
	DD	1061258067
	DD	71067649
	DD	3208005790
	DD	253330956
	DD	3218420908
	DD	991505830
	DD	3162432560
	DD	0
	DD	1072693248
	DD	1152452440
	DD	1069317060
	DD	2988872515
	DD	3159430464
	DD	0
	DD	1070596096
	DD	926263936
	DD	1068973436
	DD	3845070713
	DD	1066888484
	DD	626397172
	DD	1067006607
	DD	2777085683
	DD	1064240497
	DD	2172777379
	DD	1063623822
	DD	3188733172
	DD	1061313738
	DD	258986845
	DD	1062033866
	DD	3921389429
	DD	3218472379
	DD	3647185164
	DD	3161383618
	DD	0
	DD	1072693248
	DD	2357811071
	DD	1069387281
	DD	1815523692
	DD	1009992944
	DD	0
	DD	1070596096
	DD	3138003653
	DD	1068983043
	DD	1253835798
	DD	1066949588
	DD	2444914170
	DD	1067019349
	DD	1113961907
	DD	1064316367
	DD	1725681937
	DD	1063646503
	DD	2521427730
	DD	1061370759
	DD	2475375033
	DD	1061668474
	DD	3294480606
	DD	3218523851
	DD	1489607747
	DD	1001838107
	DD	0
	DD	1072693248
	DD	163325898
	DD	1069457623
	DD	272534188
	DD	1011389069
	DD	0
	DD	1070596096
	DD	2709640876
	DD	1068993027
	DD	3241569416
	DD	1067011170
	DD	3499770285
	DD	1067032633
	DD	2428916378
	DD	1064360877
	DD	796870323
	DD	1063670282
	DD	2401385265
	DD	1061429237
	DD	435854613
	DD	3207953615
	DD	2667571783
	DD	3218575323
	DD	1614402419
	DD	1013901432
	DD	0
	DD	1072693248
	DD	1600848691
	DD	1069528089
	DD	3799012648
	DD	1011342217
	DD	0
	DD	1070596096
	DD	3940625627
	DD	1069003390
	DD	1694054617
	DD	1067073252
	DD	3525212904
	DD	1067046468
	DD	3400170401
	DD	1064406132
	DD	2025409820
	DD	1063695095
	DD	1861695260
	DD	1061489337
	DD	2071083481
	DD	1058280175
	DD	2040662960
	DD	3218626795
	DD	4270081753
	DD	1014949642
	DD	0
	DD	1072693248
	DD	2636871548
	DD	1069573102
	DD	3635909220
	DD	1011750791
	DD	0
	DD	1070596096
	DD	3074865967
	DD	1069014136
	DD	2827594877
	DD	1067135853
	DD	4018616278
	DD	1067060863
	DD	3744630852
	DD	1064452166
	DD	967547267
	DD	1063721046
	DD	1523268212
	DD	1061551058
	DD	308558327
	DD	1057912410
	DD	1413754136
	DD	3218678267
	DD	856972295
	DD	3161564710
	DD	0
	DD	1072693248
	DD	2829232582
	DD	1069608467
	DD	1037842028
	DD	3159896230
	DD	0
	DD	1070596096
	DD	1194701220
	DD	1069025268
	DD	1485063562
	DD	1067198995
	DD	3996610205
	DD	1067075828
	DD	2865183303
	DD	1064499014
	DD	2185691567
	DD	1063748117
	DD	435906278
	DD	1061614535
	DD	1590730193
	DD	1061922957
	DD	786845313
	DD	3218729739
	DD	627570489
	DD	3158416059
	DD	0
	DD	1072693248
	DD	850612498
	DD	1069643902
	DD	3957836518
	DD	3160223109
	DD	0
	DD	1070596096
	DD	4230382869
	DD	1069036788
	DD	2678394493
	DD	1067262698
	DD	42048419
	DD	1067091374
	DD	61549345
	DD	1064546712
	DD	3326101021
	DD	1063776440
	DD	377176219
	DD	1061679763
	DD	1587637306
	DD	3206122484
	DD	159936490
	DD	3218781211
	DD	3547642993
	DD	1013612270
	DD	0
	DD	1072693248
	DD	2707383656
	DD	1069679408
	DD	554608849
	DD	1013037903
	DD	0
	DD	1070596096
	DD	1495139838
	DD	1069048702
	DD	183291521
	DD	1067326985
	DD	3540584774
	DD	1067107509
	DD	3317896890
	DD	1064595295
	DD	3844035522
	DD	1063805952
	DD	3695032775
	DD	1061746922
	DD	3749017783
	DD	3207727795
	DD	3827994963
	DD	3218832682
	DD	941734744
	DD	1014805062
	DD	0
	DD	1072693248
	DD	1615317960
	DD	1069714989
	DD	1400849280
	DD	3154963122
	DD	0
	DD	1070596096
	DD	55251407
	DD	1069061012
	DD	2380564598
	DD	1067391876
	DD	2078095393
	DD	1067124247
	DD	3759475360
	DD	1064644803
	DD	800747683
	DD	1063836662
	DD	3361780990
	DD	1061816118
	DD	3647588455
	DD	1062249457
	DD	3201086139
	DD	3218884154
	DD	4185319304
	DD	3161709290
	DD	0
	DD	1072693248
	DD	3772940467
	DD	1069750646
	DD	3998164292
	DD	3160831254
	DD	0
	DD	1070596096
	DD	3265990537
	DD	1069073721
	DD	3279971426
	DD	1067453881
	DD	2447085124
	DD	1067141598
	DD	1649985592
	DD	1064695275
	DD	3334399538
	DD	1063868754
	DD	454991464
	DD	1061887337
	DD	2110827584
	DD	3204215126
	DD	2574177316
	DD	3218935626
	DD	2889755040
	DD	3159466096
	DD	0
	DD	1072693248
	DD	2595342993
	DD	1069786383
	DD	1222183174
	DD	1011927611
	DD	0
	DD	1070596096
	DD	2192927435
	DD	1069086835
	DD	1645422722
	DD	1067486966
	DD	648269084
	DD	1067159575
	DD	2319737250
	DD	1064746750
	DD	1121494052
	DD	1063902126
	DD	897314780
	DD	1061960786
	DD	2655964904
	DD	1060259181
	DD	1947268493
	DD	3218987098
	DD	1185916272
	DD	1013323109
	DD	0
	DD	1072693248
	DD	192708373
	DD	1069822202
	DD	1013565611
	DD	1013125469
	DD	0
	DD	1070596096
	DD	1393443609
	DD	1069100357
	DD	3282624502
	DD	1067520387
	DD	3423023493
	DD	1067178189
	DD	3539283610
	DD	1064799270
	DD	1358653364
	DD	1063936902
	DD	1383848540
	DD	1062036512
	DD	1203341716
	DD	3207439782
	DD	1320359670
	DD	3219038570
	DD	1908355032
	DD	1014660481
	DD	0
	DD	1072693248
	DD	3079104278
	DD	1069858104
	DD	2593237048
	DD	1012204550
	DD	0
	DD	1070596096
	DD	1749059456
	DD	1069114291
	DD	3316773722
	DD	1067554157
	DD	2555119580
	DD	1067197455
	DD	59306268
	DD	1064852879
	DD	1944001714
	DD	1063973040
	DD	3607967027
	DD	1062114688
	DD	1550745665
	DD	1061515569
	DD	693450846
	DD	3219090042
	DD	3218699017
	DD	3161853871
	DD	0
	DD	1072693248
	DD	701484222
	DD	1069894094
	DD	1189134214
	DD	3161032728
	DD	0
	DD	1070596096
	DD	478132658
	DD	1069128642
	DD	2223654597
	DD	1067588288
	DD	706118045
	DD	1067217386
	DD	1066336370
	DD	1064907619
	DD	3310393859
	DD	1064010705
	DD	1920668857
	DD	1062195336
	DD	1238710124
	DD	3206743753
	DD	33271012
	DD	3219134917
	DD	2269574412
	DD	1015853455
	DD	0
	DD	1072693248
	DD	4098310206
	DD	1069930172
	DD	678867859
	DD	3161222246
	DD	0
	DD	1070596096
	DD	4033975882
	DD	1069143413
	DD	1577500332
	DD	1067622792
	DD	2904242360
	DD	1067237995
	DD	383406633
	DD	1064963537
	DD	511745140
	DD	1064049810
	DD	3472404675
	DD	1062243075
	DD	3498625096
	DD	1061932631
	DD	4014783896
	DD	3219160652
	DD	3119156846
	DD	1013033947
	DD	0
	DD	1072693248
	DD	2953879462
	DD	1069966343
	DD	651183718
	DD	1012253676
	DD	0
	DD	1070596096
	DD	2348802841
	DD	1069158611
	DD	4095204310
	DD	1067657681
	DD	3730066556
	DD	1067259298
	DD	2541564470
	DD	1065020679
	DD	1012137447
	DD	1064090541
	DD	3007493277
	DD	1062286104
	DD	268143184
	DD	1058732349
	DD	3701329484
	DD	3219186388
	DD	2857479637
	DD	3162758049
	DD	0
	DD	1072693248
	DD	4257115870
	DD	1070002608
	DD	39728604
	DD	1013010275
	DD	0
	DD	1070596096
	DD	3207767847
	DD	1069174239
	DD	797077063
	DD	1067692970
	DD	287980713
	DD	1067281311
	DD	1596666478
	DD	1065079095
	DD	2775385
	DD	1064132847
	DD	3823518174
	DD	1062330592
	DD	3096591665
	DD	1060290467
	DD	3387875073
	DD	3219212124
	DD	3168927931
	DD	1015274949
	DD	0
	DD	1072693248
	DD	2240974125
	DD	1070038971
	DD	3786178918
	DD	3160482292
	DD	0
	DD	1070596096
	DD	2199065489
	DD	1069190303
	DD	1118792048
	DD	1067728670
	DD	875809397
	DD	1067304048
	DD	2927616918
	DD	1065138834
	DD	3494337743
	DD	1064176822
	DD	4219965650
	DD	1062376593
	DD	4246297104
	DD	1060574506
	DD	3074420661
	DD	3219237860
	DD	1873363667
	DD	3160515403
	DD	0
	DD	1072693248
	DD	4156511795
	DD	1070075433
	DD	1791783009
	DD	1013446896
	DD	0
	DD	1070596096
	DD	4204226438
	DD	1069206807
	DD	2895376501
	DD	1067764795
	DD	3600185398
	DD	1067327526
	DD	2087275384
	DD	1065199949
	DD	347703954
	DD	1064222543
	DD	301495246
	DD	1062424178
	DD	908235914
	DD	3206713553
	DD	2760966249
	DD	3219263596
	DD	1958126117
	DD	3163336555
	DD	0
	DD	1072693248
	DD	212483252
	DD	1070111999
	DD	3496985219
	DD	3161099600
	DD	0
	DD	1070596096
	DD	1939348811
	DD	1069223758
	DD	1001569645
	DD	1067801360
	DD	764537997
	DD	1067351764
	DD	2326690580
	DD	1065262493
	DD	60750552
	DD	1064270005
	DD	2745665091
	DD	1062473435
	DD	1583079917
	DD	1060807371
	DD	2447511838
	DD	3219289332
	DD	3841595606
	DD	1014371319
	DD	0
	DD	1072693248
	DD	2234510834
	DD	1070148669
	DD	1077829152
	DD	1008793063
	DD	0
	DD	1070596096
	DD	1741599179
	DD	1069241160
	DD	288887764
	DD	1067838378
	DD	438316229
	DD	1067376778
	DD	2074161178
	DD	1065326522
	DD	2132352506
	DD	1064311981
	DD	321000208
	DD	1062524429
	DD	2575912902
	DD	1061549566
	DD	2134057426
	DD	3219315068
	DD	1285458442
	DD	3162143033
	DD	0
	DD	1072693248
	DD	719981842
	DD	1070185448
	DD	3396798827
	DD	3160592662
	DD	0
	DD	1070596096
	DD	2111724540
	DD	1069259019
	DD	2754706540
	DD	1067875863
	DD	790037588
	DD	1067402587
	DD	4097286835
	DD	1065372654
	DD	2918356845
	DD	1064337614
	DD	980543769
	DD	1062577230
	DD	1876925371
	DD	3204685716
	DD	1820603015
	DD	3219340804
	DD	3236194699
	DD	1015708874
	DD	0
	DD	1072693248
	DD	3497432648
	DD	1070222337
	DD	3403980908
	DD	1011735702
	DD	0
	DD	1070596096
	DD	2322009458
	DD	1069277341
	DD	3303184211
	DD	1067913831
	DD	3079386980
	DD	1067429210
	DD	3158396776
	DD	1065406241
	DD	500122450
	DD	1064364204
	DD	3226667103
	DD	1062631956
	DD	1843492548
	DD	1062167069
	DD	1507148603
	DD	3219366540
	DD	1086374101
	DD	1011986833
	DD	0
	DD	1072693248
	DD	1371601499
	DD	1070259341
	DD	3050542448
	DD	1012183888
	DD	0
	DD	1070596096
	DD	2434996648
	DD	1069296132
	DD	2688049294
	DD	1067952297
	DD	1586964757
	DD	1067453518
	DD	2102156419
	DD	1065440660
	DD	2974407256
	DD	1064391847
	DD	249545820
	DD	1062688662
	DD	115553579
	DD	1061128535
	DD	1193694191
	DD	3219392276
	DD	1890859349
	DD	3162902630
	DD	0
	DD	1072693248
	DD	2488075121
	DD	1070296461
	DD	1856289345
	DD	3161251229
	DD	0
	DD	1070596096
	DD	3322930631
	DD	1069315398
	DD	3277607933
	DD	1067991276
	DD	130253043
	DD	1067467675
	DD	1057196725
	DD	1065475943
	DD	3278604818
	DD	1064420531
	DD	4197429763
	DD	1062747460
	DD	1630708049
	DD	1062399550
	DD	880239780
	DD	3219418012
	DD	4135548218
	DD	1015130368
	DD	0
	DD	1072693248
	DD	2273547475
	DD	1070333701
	DD	4162052844
	DD	1011378450
	DD	0
	DD	1070596096
	DD	2392983423
	DD	1069335146
	DD	526970473
	DD	1068030785
	DD	628322840
	DD	1067482270
	DD	924033682
	DD	1065512123
	DD	854746912
	DD	1064450346
	DD	4004963071
	DD	1062808438
	DD	1468070334
	DD	1060747995
	DD	566785368
	DD	3219443748
	DD	2301849815
	DD	3161093726
	DD	0
	DD	1072693248
	DD	620845047
	DD	1070371064
	DD	314567191
	DD	1013284227
	DD	0
	DD	1070596096
	DD	2198158867
	DD	1069355382
	DD	222370857
	DD	1068070839
	DD	4028822361
	DD	1067497314
	DD	3610397327
	DD	1065549234
	DD	171354847
	DD	1064481283
	DD	693369877
	DD	1062871714
	DD	164532117
	DD	1061632559
	DD	253330956
	DD	3219469484
	DD	991505830
	DD	3163481136
	DD	0
	DD	1072693248
	DD	1894214069
	DD	1070408552
	DD	2182661802
	DD	1012022503
	DD	0
	DD	1070596096
	DD	1869133310
	DD	1069376113
	DD	1368144043
	DD	1068111455
	DD	242783364
	DD	1067512821
	DD	3692616565
	DD	1065587313
	DD	2890683560
	DD	1064513419
	DD	2407947124
	DD	1062937380
	DD	3803218812
	DD	1060561671
	DD	4234843841
	DD	3219495219
	DD	1479868884
	DD	1014082158
	DD	0
	DD	1072693248
	DD	2049842017
	DD	1070446169
	DD	2491067638
	DD	3161241370
	DD	0
	DD	1070596096
	DD	1431833764
	DD	1069397346
	DD	551048935
	DD	1068152651
	DD	2575917112
	DD	1067528800
	DD	1559777140
	DD	1065626397
	DD	743322322
	DD	1064546764
	DD	1974991899
	DD	1063005565
	DD	3459002328
	DD	1062460308
	DD	3921389429
	DD	3219520955
	DD	3647185164
	DD	3162432194
	DD	0
	DD	1072693248
	DD	1526324265
	DD	1070483918
	DD	4056333712
	DD	3157795638
	DD	0
	DD	1070596096
	DD	1830915900
	DD	1069419088
	DD	602185706
	DD	1068194444
	DD	555210277
	DD	1067545266
	DD	3690136761
	DD	1065666523
	DD	4115917104
	DD	1064581405
	DD	589230666
	DD	1063076369
	DD	3448584873
	DD	1062382364
	DD	3607935018
	DD	3219546691
	DD	4202814986
	DD	1015564293
	DD	0
	DD	1072693248
	DD	955408364
	DD	1070521802
	DD	2659929592
	DD	3160779467
	DD	0
	DD	1070596096
	DD	659174847
	DD	1069441347
	DD	2081794994
	DD	1068236852
	DD	2956616392
	DD	1067562230
	DD	1461867065
	DD	1065707733
	DD	879427925
	DD	1064617384
	DD	209598570
	DD	1063149928
	DD	59348277
	DD	1061529630
	DD	3294480606
	DD	3219572427
	DD	1489607747
	DD	1002886683
	DD	0
	DD	1072693248
	DD	1167858038
	DD	1070559824
	DD	2180017056
	DD	1010873776
	DD	0
	DD	1070596096
	DD	772792549
	DD	1069464130
	DD	3356872770
	DD	1068279894
	DD	3524844777
	DD	1067579707
	DD	1521756467
	DD	1065750067
	DD	3931969752
	DD	1064654734
	DD	2908114566
	DD	1063226363
	DD	3520733649
	DD	1061374592
	DD	2981026194
	DD	3219598163
	DD	924239062
	DD	3163047211
	DD	0
	DD	1072693248
	DD	3747221382
	DD	1070597041
	DD	1912303979
	DD	1014066411
	DD	0
	DD	1070596096
	DD	1727686788
	DD	1069487445
	DD	681802874
	DD	1068323590
	DD	2531789465
	DD	1067597711
	DD	331202638
	DD	1065793569
	DD	2627596742
	DD	1064693515
	DD	566979200
	DD	1063280948
	DD	2423252998
	DD	1061741710
	DD	2667571783
	DD	3219623899
	DD	1614402419
	DD	1014950008
	DD	0
	DD	1072693248
	DD	4148643959
	DD	1070616195
	DD	2887822437
	DD	1014214935
	DD	0
	DD	1070596096
	DD	4101770849
	DD	1069511300
	DD	1167052501
	DD	1068367958
	DD	2569727718
	DD	1067616256
	DD	1681502493
	DD	1065838283
	DD	3685967929
	DD	1064733780
	DD	2973319878
	DD	1063322263
	DD	474740638
	DD	1062549181
	DD	2354117371
	DD	3219649635
	DD	3512651629
	DD	3161564344
	DD	0
	DD	1072693248
	DD	373017919
	DD	1070635424
	DD	3647199022
	DD	1014660881
	DD	0
	DD	1070596096
	DD	933359764
	DD	1069535705
	DD	1391441088
	DD	1068413019
	DD	53106651
	DD	1067635358
	DD	4157835043
	DD	1065884256
	DD	4055868043
	DD	1064775611
	DD	1529330433
	DD	1063365236
	DD	326078913
	DD	1062325273
	DD	2040662960
	DD	3219675371
	DD	4270081753
	DD	1015998218
	DD	0
	DD	1072693248
	DD	4002124759
	DD	1070654727
	DD	3453343154
	DD	1013423881
	DD	0
	DD	1071644672
	DD	774562305
	DD	3217018021
	DD	377617675
	DD	1068458794
	DD	2495478050
	DD	1067655031
	DD	1508050213
	DD	1065931538
	DD	1921336565
	DD	1064819067
	DD	975923615
	DD	1063409946
	DD	2275792800
	DD	1060505011
	DD	1727208548
	DD	3219701107
	DD	2531251621
	DD	1013613001
	DD	0
	DD	1072693248
	DD	963359913
	DD	1070674109
	DD	2464811476
	DD	3160767877
	DD	0
	DD	1071644672
	DD	1498613974
	DD	3216992493
	DD	4138103782
	DD	1068502123
	DD	2840947366
	DD	1067675293
	DD	209537977
	DD	1065980178
	DD	3590357525
	DD	1064864171
	DD	73959555
	DD	1063456484
	DD	1512178095
	DD	1062390487
	DD	1413754136
	DD	3219726843
	DD	856972295
	DD	3162613286
	DD	0
	DD	0
	DD	0
	DD	3220176896
	DD	1994327296
	DD	1006932657
	DD	0
	DD	1073741824
	DD	0
	DD	0
	DD	266
	DD	3221225472
	DD	1431655956
	DD	1074091349
	DD	2828070733
	DD	3221924522
	DD	268536947
	DD	1074860305
	DD	4220652043
	DD	3222647048
	DD	3597047153
	DD	1075552901
	DD	1100299725
	DD	3219752579
	DD	874467977
	DD	1015419713
	DD	0
	DD	0
	DD	2668510354
	DD	3220126045
	DD	1701998120
	DD	1012206572
	DD	0
	DD	1073741824
	DD	1404962112
	DD	3215492924
	DD	3327263454
	DD	3221125605
	DD	1987673213
	DD	1074008829
	DD	231222583
	DD	3221793459
	DD	1756671652
	DD	1074731884
	DD	3921518330
	DD	3222500748
	DD	2691193109
	DD	1075341426
	DD	786845313
	DD	3219778315
	DD	627570489
	DD	3159464635
	DD	0
	DD	0
	DD	2949191430
	DD	3220076398
	DD	3989126963
	DD	3163096676
	DD	0
	DD	1073741824
	DD	2909112164
	DD	3216503445
	DD	3682557528
	DD	3221031585
	DD	4184421718
	DD	1073932454
	DD	380803270
	DD	3221673930
	DD	3399744364
	DD	1074553823
	DD	3842864962
	DD	3222371390
	DD	748075446
	DD	1075157395
	DD	473390901
	DD	3219804051
	DD	4252586071
	DD	3163191791
	DD	0
	DD	0
	DD	2308259791
	DD	3220027898
	DD	4111771200
	DD	3156765800
	DD	0
	DD	1073741824
	DD	411343470
	DD	3217132228
	DD	3760686807
	DD	3220942979
	DD	4171367426
	DD	1073861688
	DD	1878500541
	DD	3221564765
	DD	108767834
	DD	1074393698
	DD	2824899565
	DD	3222239549
	DD	3321236009
	DD	1074997439
	DD	159936490
	DD	3219829787
	DD	3547642993
	DD	1014660846
	DD	0
	DD	0
	DD	3342355758
	DD	3219980491
	DD	1666924640
	DD	3162031356
	DD	0
	DD	1073741824
	DD	2271288998
	DD	3217480250
	DD	4111284847
	DD	3220859392
	DD	2796272966
	DD	1073796048
	DD	1730314448
	DD	3221464928
	DD	1813001552
	DD	1074249474
	DD	603251437
	DD	3222036157
	DD	3079042443
	DD	1074856525
	DD	4141449374
	DD	3219855522
	DD	1579411055
	DD	3161853506
	DD	0
	DD	0
	DD	166164867
	DD	3219934128
	DD	2304492376
	DD	1012090550
	DD	0
	DD	1073741824
	DD	2798667120
	DD	3217812323
	DD	4203596829
	DD	3220780463
	DD	1473339147
	DD	1073728374
	DD	4243021652
	DD	3221373498
	DD	1638747017
	DD	1074119375
	DD	1512673116
	DD	3221855335
	DD	2338684822
	DD	1074678259
	DD	3827994963
	DD	3219881258
	DD	941734744
	DD	1015853638
	DD	0
	DD	0
	DD	3247184626
	DD	3219888759
	DD	1245319390
	DD	3161909785
	DD	0
	DD	1073741824
	DD	2381906911
	DD	3218104536
	DD	1010924947
	DD	3220705862
	DD	2481381005
	DD	1073615072
	DD	1356661294
	DD	3221289659
	DD	1527070583
	DD	1074001844
	DD	3061693405
	DD	3221694303
	DD	1993154673
	DD	1074463435
	DD	3514540551
	DD	3219906994
	DD	2102765473
	DD	1013034678
	DD	0
	DD	0
	DD	4189730214
	DD	3219844341
	DD	872357632
	DD	3158652168
	DD	0
	DD	1073741824
	DD	760634709
	DD	3218255915
	DD	2067827628
	DD	3220635284
	DD	1386756408
	DD	1073509659
	DD	404552634
	DD	3221199890
	DD	1143062644
	DD	1073895515
	DD	555342338
	DD	3221550657
	DD	3470199450
	DD	1074271418
	DD	3201086139
	DD	3219932730
	DD	4185319304
	DD	3162757866
	DD	0
	DD	0
	DD	719595600
	DD	3219800832
	DD	114970824
	DD	1014660100
	DD	0
	DD	1073741824
	DD	3811788216
	DD	3218400550
	DD	1264738948
	DD	3220568452
	DD	1958933439
	DD	1073411493
	DD	4246310163
	DD	3221058356
	DD	1367842247
	DD	1073799186
	DD	4112596176
	DD	3221422310
	DD	2100954469
	DD	1074105254
	DD	2887631728
	DD	3219958466
	DD	1841088264
	DD	1015275132
	DD	0
	DD	0
	DD	258163604
	DD	3219758190
	DD	3299889334
	DD	3162612005
	DD	0
	DD	1073741824
	DD	2865277319
	DD	3218538798
	DD	3110066496
	DD	3220505109
	DD	723074889
	DD	1073319994
	DD	1618819009
	DD	3220928086
	DD	1176243935
	DD	1073681775
	DD	1223849000
	DD	3221307454
	DD	2369288310
	DD	1073957581
	DD	2574177316
	DD	3219984202
	DD	2889755040
	DD	3160514672
	DD	0
	DD	0
	DD	2079874795
	DD	3219716377
	DD	1885564844
	DD	1014470429
	DD	0
	DD	1073741824
	DD	1656469901
	DD	3218670989
	DD	823761676
	DD	3220445021
	DD	2491445454
	DD	1073234634
	DD	1584341555
	DD	3220808039
	DD	1976688438
	DD	1073523018
	DD	3918313362
	DD	3221183550
	DD	3178717701
	DD	1073829019
	DD	2260722904
	DD	3220009938
	DD	3285965784
	DD	3163336372
	DD	0
	DD	0
	DD	997287281
	DD	3219675358
	DD	3612251184
	DD	3163317056
	DD	0
	DD	1073741824
	DD	3037674984
	DD	3218797431
	DD	2624215131
	DD	3220387969
	DD	464779768
	DD	1073154936
	DD	3700763630
	DD	3220697284
	DD	1200926910
	DD	1073378628
	DD	2934576448
	DD	3220998744
	DD	3287041894
	DD	1073681673
	DD	1947268493
	DD	3220035674
	DD	1185916272
	DD	1014371685
	DD	0
	DD	0
	DD	3978924239
	DD	3219635097
	DD	680771999
	DD	1015720881
	DD	0
	DD	1073741824
	DD	3528982175
	DD	3218918413
	DD	3018376188
	DD	3220333754
	DD	2093086652
	DD	1073080463
	DD	451029217
	DD	3220594987
	DD	1097121537
	DD	1073247142
	DD	2277789243
	DD	3220832617
	DD	149138386
	DD	1073478822
	DD	1633814081
	DD	3220061410
	DD	3941137777
	DD	3162142667
	DD	0
	DD	0
	DD	3860971117
	DD	3219595563
	DD	2891614774
	DD	3163214156
	DD	0
	DD	1073741824
	DD	80822128
	DD	3219034205
	DD	2077635078
	DD	3220282191
	DD	1552870456
	DD	1073010820
	DD	3607700650
	DD	3220500394
	DD	249538439
	DD	1073127265
	DD	2233833083
	DD	3220683068
	DD	2436456793
	DD	1073293556
	DD	1320359670
	DD	3220087146
	DD	1908355032
	DD	1015709057
	DD	0
	DD	0
	DD	343219638
	DD	3219556725
	DD	2238262736
	DD	3161731808
	DD	0
	DD	1072693248
	DD	3768132253
	DD	1071627934
	DD	3895546569
	DD	3220233108
	DD	1613056473
	DD	1072945645
	DD	3409294908
	DD	3220412831
	DD	3138966349
	DD	1073017844
	DD	3799067288
	DD	3220548255
	DD	359470498
	DD	1073133087
	DD	1006905258
	DD	3220112882
	DD	3348558652
	DD	1011988294
	DD	0
	DD	0
	DD	3841229102
	DD	3219518551
	DD	1782820117
	DD	3163516640
	DD	0
	DD	1072693248
	DD	866896685
	DD	1071521786
	DD	2743435231
	DD	3220186348
	DD	3017984361
	DD	1072884608
	DD	1491919678
	DD	3220331688
	DD	1507535811
	DD	1072917857
	DD	1966124974
	DD	3220426562
	DD	381628641
	DD	1072992589
	DD	693450846
	DD	3220138618
	DD	3218699017
	DD	3162902447
	DD	0
	DD	0
	DD	716700048
	DD	3219481016
	DD	1043841701
	DD	3163006458
	DD	0
	DD	1072693248
	DD	1097907398
	DD	1071420120
	DD	3912524876
	DD	3220106631
	DD	4107784306
	DD	1072827408
	DD	937056065
	DD	3220256414
	DD	4128945134
	DD	1072826390
	DD	1032262394
	DD	3220316565
	DD	1825923585
	DD	1072864423
	DD	379996435
	DD	3220164354
	DD	2807708551
	DD	1015130551
	DD	0
	DD	0
	DD	142288477
	DD	3219444091
	DD	1256348500
	DD	1014532780
	DD	0
	DD	1072693248
	DD	925074332
	DD	1071322730
	DD	2134728327
	DD	3220021541
	DD	1886408555
	DD	1072773770
	DD	101893267
	DD	3220186512
	DD	2020085224
	DD	1072742631
	DD	2955803861
	DD	3220217010
	DD	343310816
	DD	1072748161
	DD	33271012
	DD	3220183493
	DD	2269574412
	DD	1016902031
	DD	0
	DD	0
	DD	3684492763
	DD	3219407750
	DD	2905890526
	DD	1015709561
	DD	0
	DD	1072693248
	DD	585640690
	DD	1071229422
	DD	1068301595
	DD	3219940277
	DD	2953900489
	DD	1072723440
	DD	950237889
	DD	3220066167
	DD	109735425
	DD	1072638455
	DD	792610979
	DD	3220076689
	DD	2904190292
	DD	1072594405
	DD	4171511102
	DD	3220196360
	DD	987810900
	DD	1016106563
	DD	0
	DD	0
	DD	681498355
	DD	3219371971
	DD	3542625299
	DD	1015827699
	DD	0
	DD	1072693248
	DD	168222332
	DD	1071140014
	DD	776736583
	DD	3219862604
	DD	2405401100
	DD	1072659128
	DD	430546584
	DD	3219945234
	DD	2974083786
	DD	1072497552
	DD	849442068
	DD	3219912970
	DD	2727451532
	DD	1072409680
	DD	4014783896
	DD	3220209228
	DD	3119156846
	DD	1014082523
	DD	0
	DD	0
	DD	2298773501
	DD	3219336728
	DD	4027807132
	DD	1015090440
	DD	0
	DD	1072693248
	DD	312748411
	DD	1071054335
	DD	2723913091
	DD	3219788303
	DD	3207063211
	DD	1072570353
	DD	1845780128
	DD	3219832588
	DD	752659336
	DD	1072368142
	DD	1097207453
	DD	3219764237
	DD	407427403
	DD	1072246732
	DD	3858056690
	DD	3220222096
	DD	2007897202
	DD	3162431829
	DD	0
	DD	0
	DD	2591355529
	DD	3219302000
	DD	2351326818
	DD	3161908153
	DD	0
	DD	1072693248
	DD	3206845590
	DD	1070972224
	DD	4023349050
	DD	3219717171
	DD	3838822434
	DD	1072486918
	DD	3122427157
	DD	3219727557
	DD	2261703627
	DD	1072249178
	DD	1764870100
	DD	3219628955
	DD	3201802997
	DD	1072093301
	DD	3701329484
	DD	3220234964
	DD	2857479637
	DD	3163806625
	DD	0
	DD	0
	DD	4010047525
	DD	3219267765
	DD	3339891512
	DD	3160072694
	DD	0
	DD	1072693248
	DD	966450668
	DD	1070893532
	DD	803989702
	DD	3219649019
	DD	1934453966
	DD	1072408465
	DD	1455077850
	DD	3219629530
	DD	2709264343
	DD	1072139721
	DD	1918064558
	DD	3219505762
	DD	1476733535
	DD	1071960574
	DD	3544602278
	DD	3220247832
	DD	4139243149
	DD	3164602093
	DD	0
	DD	0
	DD	4270735453
	DD	3219234003
	DD	3565361953
	DD	1015342529
	DD	0
	DD	1072693248
	DD	2915760230
	DD	1070818115
	DD	638580392
	DD	3219583668
	DD	2252948193
	DD	1072334663
	DD	1194731830
	DD	3219537949
	DD	1353002942
	DD	1072038927
	DD	422364789
	DD	3219393446
	DD	3669634446
	DD	1071842023
	DD	3387875073
	DD	3220260700
	DD	3168927931
	DD	1016323525
	DD	0
	DD	0
	DD	419968236
	DD	3219200695
	DD	131828542
	DD	1015961466
	DD	0
	DD	1072693248
	DD	2960267235
	DD	1070745841
	DD	1037049094
	DD	3219520953
	DD	1447436626
	DD	1072265209
	DD	3197232138
	DD	3219452306
	DD	4004685084
	DD	1071946034
	DD	1412816421
	DD	3219290928
	DD	3184738590
	DD	1071740610
	DD	3231147867
	DD	3220273568
	DD	3253690381
	DD	1014950373
	DD	0
	DD	0
	DD	1505561933
	DD	3219167820
	DD	2813885200
	DD	1011162516
	DD	0
	DD	1072693248
	DD	2997217365
	DD	1070676584
	DD	2681788575
	DD	3219460719
	DD	3989126937
	DD	1072199822
	DD	1049115510
	DD	3219372139
	DD	511642058
	DD	1071860360
	DD	4034728051
	DD	3219197246
	DD	1788863965
	DD	1071639550
	DD	3074420661
	DD	3220286436
	DD	1873363667
	DD	3161563979
	DD	0
	DD	0
	DD	2606398711
	DD	3219135361
	DD	2533072598
	DD	3162554073
	DD	0
	DD	1072693248
	DD	2151361100
	DD	1070610226
	DD	1532348096
	DD	3219402822
	DD	408604530
	DD	1072138246
	DD	1039538740
	DD	3219297023
	DD	1234598850
	DD	1071781284
	DD	692507578
	DD	3219094767
	DD	4090315877
	DD	1071451983
	DD	2917693455
	DD	3220299304
	DD	676362605
	DD	3163589663
	DD	0
	DD	0
	DD	3649427950
	DD	3219078282
	DD	4113825640
	DD	3158662193
	DD	0
	DD	1072693248
	DD	2424145432
	DD	1070497215
	DD	2715404890
	DD	3219347125
	DD	2238283673
	DD	1072080240
	DD	1186867211
	DD	3219226571
	DD	660318792
	DD	1071708248
	DD	2091427915
	DD	3218937780
	DD	2591062635
	DD	1071291190
	DD	2760966249
	DD	3220312172
	DD	1958126117
	DD	3164385131
	DD	0
	DD	0
	DD	220675706
	DD	3219014926
	DD	3158451653
	DD	3161723787
	DD	0
	DD	1072693248
	DD	1565030890
	DD	1070375439
	DD	335263687
	DD	3219293502
	DD	419816567
	DD	1072025586
	DD	2098048102
	DD	3219160428
	DD	257229335
	DD	1071636821
	DD	3464457189
	DD	3218793840
	DD	2745923191
	DD	1071158056
	DD	2604239044
	DD	3220325040
	DD	1055077667
	DD	1016540488
	DD	0
	DD	0
	DD	2191706112
	DD	3218952300
	DD	333441366
	DD	3161331348
	DD	0
	DD	1072693248
	DD	3435408456
	DD	1070258830
	DD	487319719
	DD	3219241832
	DD	1713711059
	DD	1071974079
	DD	130753286
	DD	3219068219
	DD	909725130
	DD	1071511974
	DD	2762442367
	DD	3218661715
	DD	932670499
	DD	1071027067
	DD	2447511838
	DD	3220337908
	DD	3841595606
	DD	1015419895
	DD	0
	DD	0
	DD	408840118
	DD	3218890375
	DD	4048072104
	DD	3159354273
	DD	0
	DD	1072693248
	DD	2794370512
	DD	1070147203
	DD	1550993351
	DD	3219192003
	DD	1305093678
	DD	1071925532
	DD	1748211158
	DD	3218951271
	DD	3244212114
	DD	1071396455
	DD	2145666003
	DD	3218540299
	DD	1098451822
	DD	1070891096
	DD	2290784632
	DD	3220350776
	DD	2660353234
	DD	3159463173
	DD	0
	DD	0
	DD	3368427099
	DD	3218829119
	DD	2187864186
	DD	1013587140
	DD	0
	DD	1072693248
	DD	1708021028
	DD	1070040382
	DD	459972195
	DD	3219143910
	DD	3210514347
	DD	1071879770
	DD	955408873
	DD	3218841145
	DD	1911386085
	DD	1071289511
	DD	3563284038
	DD	3218428599
	DD	547420745
	DD	1070790031
	DD	2134057426
	DD	3220363644
	DD	1285458442
	DD	3163191609
	DD	0
	DD	0
	DD	2583490354
	DD	3218768505
	DD	27148750
	DD	3161491985
	DD	0
	DD	1072693248
	DD	2550940470
	DD	1069938201
	DD	2616040293
	DD	3219066585
	DD	2529278955
	DD	1071836633
	DD	2244405849
	DD	3218737335
	DD	2064814127
	DD	1071190460
	DD	1527853251
	DD	3218325722
	DD	354247476
	DD	1070708112
	DD	1977330220
	DD	3220376512
	DD	4071976381
	DD	3164168168
	DD	0
	DD	0
	DD	2390846448
	DD	3218708504
	DD	3510503948
	DD	3161709355
	DD	0
	DD	1072693248
	DD	1208409702
	DD	1069840505
	DD	1006199412
	DD	3218976755
	DD	2504246873
	DD	1071795971
	DD	1040653613
	DD	3218639377
	DD	1713268217
	DD	1071098686
	DD	462276301
	DD	3218230862
	DD	4239163357
	DD	1070603989
	DD	1820603015
	DD	3220389380
	DD	3236194699
	DD	1016757450
	DD	0
	DD	0
	DD	2528366626
	DD	3218649089
	DD	3808712102
	DD	1013396510
	DD	0
	DD	1072693248
	DD	1938261590
	DD	1069747146
	DD	4282399603
	DD	3218889831
	DD	1922568281
	DD	1071757646
	DD	2428166646
	DD	3218546841
	DD	717464824
	DD	1071013623
	DD	2205664075
	DD	3218143293
	DD	1526076492
	DD	1070438229
	DD	1663875809
	DD	3220402248
	DD	3908862373
	DD	1015853820
	DD	0
	DD	0
	DD	2200768388
	DD	3218590234
	DD	302584884
	DD	1014949287
	DD	0
	DD	1072693248
	DD	2338248308
	DD	1069657986
	DD	2617403810
	DD	3218805648
	DD	60078074
	DD	1071721530
	DD	2753634668
	DD	3218459332
	DD	626020946
	DD	1070934758
	DD	25846122
	DD	3218044977
	DD	1123428672
	DD	1070307499
	DD	1507148603
	DD	3220415116
	DD	1086374101
	DD	1013035409
	DD	0
	DD	0
	DD	4158096969
	DD	3218531913
	DD	3224512900
	DD	3160902954
	DD	0
	DD	1072693248
	DD	4240313622
	DD	1069572894
	DD	3745046477
	DD	3218724046
	DD	1955120038
	DD	1071687503
	DD	2661530988
	DD	3218376484
	DD	2615081511
	DD	1070861626
	DD	3172686029
	DD	3217895196
	DD	3468537113
	DD	1070173373
	DD	1350421397
	DD	3220427984
	DD	1218191675
	DD	3162757684
	DD	0
	DD	0
	DD	1606100959
	DD	3218474103
	DD	3385789004
	DD	3161114183
	DD	0
	DD	1072693248
	DD	2255302008
	DD	1069435977
	DD	54085333
	DD	3218644877
	DD	2504974906
	DD	1071655456
	DD	4169947836
	DD	3218297958
	DD	2891827398
	DD	1070793803
	DD	1030744944
	DD	3217756428
	DD	2378245768
	DD	1070036506
	DD	1193694191
	DD	3220440852
	DD	1890859349
	DD	3163951206
	DD	0
	DD	0
	DD	3782220567
	DD	3218416778
	DD	2488119274
	DD	3162237132
	DD	0
	DD	1072693248
	DD	91372748
	DD	1069281344
	DD	318776714
	DD	3218567997
	DD	1791786485
	DD	1071605902
	DD	245863368
	DD	3218223442
	DD	3306699316
	DD	1070730899
	DD	492155895
	DD	3217627699
	DD	504590813
	DD	1069961586
	DD	1036966986
	DD	3220453720
	DD	1122344434
	DD	1016974413
	DD	0
	DD	0
	DD	1412148370
	DD	3218359917
	DD	2689483850
	DD	1013993587
	DD	0
	DD	1072693248
	DD	1375255072
	DD	1069134151
	DD	1493266594
	DD	3218493272
	DD	487968719
	DD	1071549129
	DD	4010395236
	DD	3218152642
	DD	2875570533
	DD	1070672565
	DD	2596803943
	DD	3217508124
	DD	2843491945
	DD	1069843051
	DD	880239780
	DD	3220466588
	DD	4135548218
	DD	1016178944
	DD	0
	DD	0
	DD	895247324
	DD	3218303496
	DD	3064008070
	DD	1013805381
	DD	0
	DD	1072693248
	DD	729983844
	DD	1068994194
	DD	3631919317
	DD	3218420574
	DD	2031343772
	DD	1071495745
	DD	1505593195
	DD	3218085291
	DD	519428997
	DD	1070618477
	DD	3710689661
	DD	3217396902
	DD	3156076115
	DD	1069741343
	DD	723512574
	DD	3220479456
	DD	2825204233
	DD	1014372050
	DD	0
	DD	0
	DD	2664093517
	DD	3218247493
	DD	3574482624
	DD	3157273734
	DD	0
	DD	1072693248
	DD	2465054196
	DD	1068861279
	DD	3495559184
	DD	3218349782
	DD	2180799467
	DD	1071445588
	DD	3760731789
	DD	3217962527
	DD	3108073739
	DD	1070540584
	DD	1955989624
	DD	3217293302
	DD	166797605
	DD	1069654305
	DD	566785368
	DD	3220492324
	DD	2301849815
	DD	3162142302
	DD	0
	DD	0
	DD	1026922473
	DD	3218191888
	DD	4275547180
	DD	3161242805
	DD	0
	DD	1072693248
	DD	865263972
	DD	1068735226
	DD	1189261043
	DD	3218280780
	DD	602895285
	DD	1071398507
	DD	457371263
	DD	3217840141
	DD	1846858242
	DD	1070447677
	DD	3060498116
	DD	3217196657
	DD	4267893365
	DD	1069596604
	DD	410058162
	DD	3220505192
	DD	4004709614
	DD	3163734243
	DD	0
	DD	0
	DD	1197423420
	DD	3218136659
	DD	176395860
	DD	3160912588
	DD	0
	DD	1072693248
	DD	3614211808
	DD	1068615863
	DD	2638984897
	DD	3218213456
	DD	1695604536
	DD	1071354361
	DD	2372043566
	DD	3217723241
	DD	136909605
	DD	1070361652
	DD	3640039447
	DD	3217106358
	DD	4088615056
	DD	1069449448
	DD	253330956
	DD	3220518060
	DD	991505830
	DD	3164529712
	DD	0
	DD	0
	DD	1971725998
	DD	3218081786
	DD	2628057466
	DD	3160778219
	DD	0
	DD	1072693248
	DD	3850616508
	DD	1068503032
	DD	715485429
	DD	3218147706
	DD	2831870887
	DD	1071313021
	DD	276112813
	DD	3217611421
	DD	4071473627
	DD	1070282053
	DD	3678820506
	DD	3217012537
	DD	927889203
	DD	1069392578
	DD	96603751
	DD	3220530928
	DD	2021697954
	DD	1016395907
	DD	0
	DD	0
	DD	4068253482
	DD	3217974755
	DD	3098800802
	DD	3161416360
	DD	0
	DD	1072693248
	DD	3555466384
	DD	1068294223
	DD	3714292293
	DD	3218083427
	DD	58004546
	DD	1071274368
	DD	2889916307
	DD	3217504297
	DD	2725223785
	DD	1070208483
	DD	1960889150
	DD	3216854085
	DD	2172838405
	DD	1069259864
	DD	4234843841
	DD	3220543795
	DD	1479868884
	DD	1015130734
	DD	0
	DD	0
	DD	3947580658
	DD	3217866316
	DD	4153464200
	DD	3160712468
	DD	0
	DD	1072693248
	DD	1306970432
	DD	1068093810
	DD	3320658510
	DD	3217961305
	DD	2638377394
	DD	1071238289
	DD	1218095092
	DD	3217401513
	DD	506153465
	DD	1070140566
	DD	2096772766
	DD	3216705262
	DD	2058303373
	DD	1069148484
	DD	4078116635
	DD	3220556663
	DD	39665264
	DD	3161092265
	DD	0
	DD	0
	DD	4023957346
	DD	3217758473
	DD	2652051644
	DD	3159900932
	DD	0
	DD	1072693248
	DD	1548193280
	DD	1067905620
	DD	2849460080
	DD	3217838064
	DD	586099284
	DD	1071204684
	DD	2426038050
	DD	3217302731
	DD	1209882939
	DD	1070077962
	DD	204929670
	DD	3216565209
	DD	2951398611
	DD	1069077118
	DD	3921389429
	DD	3220569531
	DD	3647185164
	DD	3163480770
	DD	0
	DD	0
	DD	3763679576
	DD	3217651189
	DD	448385104
	DD	3157192355
	DD	0
	DD	1072693248
	DD	64931152
	DD	1067729411
	DD	3593250320
	DD	3217717209
	DD	1127364104
	DD	1071173457
	DD	21320967
	DD	3217207636
	DD	4036384111
	DD	1070020367
	DD	3394693850
	DD	3216433130
	DD	4171153724
	DD	1068939101
	DD	3764662223
	DD	3220582399
	DD	3105356094
	DD	3164312749
	DD	0
	DD	0
	DD	1425758568
	DD	3217544428
	DD	1224254624
	DD	3159340630
	DD	0
	DD	1072693248
	DD	2086713664
	DD	1067564956
	DD	1688303979
	DD	3217598569
	DD	938619730
	DD	1071144522
	DD	526120622
	DD	3217115928
	DD	3819295674
	DD	1069967496
	DD	2059466203
	DD	3216308301
	DD	4034680199
	DD	1068903334
	DD	3607935018
	DD	3220595267
	DD	4202814986
	DD	1016612869
	DD	0
	DD	0
	DD	2429422902
	DD	3217438153
	DD	3297520416
	DD	1012522944
	DD	0
	DD	1072693248
	DD	528565440
	DD	1067373727
	DD	2395999282
	DD	3217481977
	DD	1512953454
	DD	1071117799
	DD	2702142222
	DD	3217023483
	DD	3515899579
	DD	1069919100
	DD	2767905791
	DD	3216190039
	DD	3127036520
	DD	1068808800
	DD	3451207812
	DD	3220608135
	DD	1547135652
	DD	1015564659
	DD	0
	DD	0
	DD	1959743063
	DD	3217332330
	DD	607720656
	DD	3161041674
	DD	0
	DD	1072693248
	DD	225834432
	DD	1067090614
	DD	732340398
	DD	3217367274
	DD	3221152670
	DD	1071093215
	DD	2810588359
	DD	3216851955
	DD	612200454
	DD	1069874948
	DD	645427044
	DD	3216077719
	DD	1966523063
	DD	1068694214
	DD	3294480606
	DD	3220621003
	DD	1489607747
	DD	1003935259
	DD	0
	DD	0
	DD	1940260416
	DD	3217226924
	DD	4144841612
	DD	3159543701
	DD	0
	DD	1072693248
	DD	1135818128
	DD	1066829850
	DD	867512158
	DD	3217254304
	DD	3821819258
	DD	1071070704
	DD	2890864845
	DD	3216685596
	DD	918459421
	DD	1069834828
	DD	797471869
	DD	3215958923
	DD	4093823457
	DD	1068635569
	DD	3137753400
	DD	3220633871
	DD	3579918397
	DD	3163046845
	DD	0
	DD	0
	DD	2242453104
	DD	3217121901
	DD	1143544320
	DD	1011877960
	DD	0
	DD	1072693248
	DD	1016752608
	DD	1066591110
	DD	1270001991
	DD	3217142917
	DD	1410785275
	DD	1071050206
	DD	1103929722
	DD	3216523923
	DD	3268015214
	DD	1069798554
	DD	957665123
	DD	3215754628
	DD	1067716675
	DD	1068610012
	DD	2981026194
	DD	3220646739
	DD	924239062
	DD	3164095787
	DD	0
	DD	0
	DD	982366697
	DD	3217003288
	DD	1160619554
	DD	1010902188
	DD	0
	DD	1072693248
	DD	404678272
	DD	1066346403
	DD	1419607756
	DD	3217032967
	DD	1800406832
	DD	1071031665
	DD	3144361055
	DD	3216366473
	DD	1744148039
	DD	1069765960
	DD	3211598528
	DD	3215558937
	DD	405031618
	DD	1068609434
	DD	2824298989
	DD	3220659607
	DD	2088964722
	DD	1016829832
	DD	0
	DD	0
	DD	931121154
	DD	3216794574
	DD	1121583044
	DD	1012154237
	DD	0
	DD	1072693248
	DD	709357472
	DD	1065955296
	DD	29972165
	DD	3216817456
	DD	254360647
	DD	1071015033
	DD	3732687627
	DD	3216212805
	DD	3224370681
	DD	1069736900
	DD	202592280
	DD	3215370879
	DD	1420621447
	DD	1068491719
	DD	2667571783
	DD	3220672475
	DD	1614402419
	DD	1015998584
	DD	0
	DD	0
	DD	2354785698
	DD	3216586427
	DD	959264268
	DD	3159915811
	DD	0
	DD	1072693248
	DD	2828230112
	DD	1065606626
	DD	3693284264
	DD	3216602456
	DD	1996210616
	DD	1071000265
	DD	3053541878
	DD	3216062494
	DD	1133334567
	DD	1069711236
	DD	977677585
	DD	3215189563
	DD	347473669
	DD	1068436198
	DD	2510844577
	DD	3220685343
	DD	498468876
	DD	1013614463
	DD	0
	DD	0
	DD	1972895212
	DD	3216378783
	DD	3207896256
	DD	3158122047
	DD	0
	DD	1072693248
	DD	4107853760
	DD	1065246718
	DD	2229347510
	DD	3216389497
	DD	4172638464
	DD	1070987323
	DD	212469442
	DD	3215847669
	DD	1989165661
	DD	1069688852
	DD	4131454738
	DD	3215014128
	DD	3452987325
	DD	1068469531
	DD	2354117371
	DD	3220698211
	DD	3512651629
	DD	3162612920
	DD	0
	DD	0
	DD	3439278129
	DD	3216171577
	DD	1073008748
	DD	3159914989
	DD	0
	DD	1072693248
	DD	2934847296
	DD	1064716672
	DD	2625704508
	DD	3216178311
	DD	3692939630
	DD	1070976174
	DD	3072165553
	DD	3215558041
	DD	223733198
	DD	1069669653
	DD	1158528288
	DD	3214753465
	DD	3197803998
	DD	1068357327
	DD	2197390165
	DD	3220711079
	DD	3038089327
	DD	3163878824
	DD	0
	DD	0
	DD	3930011523
	DD	3215946901
	DD	3574565577
	DD	1010859792
	DD	0
	DD	1072693248
	DD	930814464
	DD	1064233867
	DD	3218126771
	DD	3215954682
	DD	873740287
	DD	1070966789
	DD	1580060562
	DD	3215272745
	DD	1505280051
	DD	1069653548
	DD	2221338643
	DD	3214421240
	DD	2641116540
	DD	1068312400
	DD	2040662960
	DD	3220723947
	DD	4270081753
	DD	1017046794
	DD	0
	DD	0
	DD	2319126882
	DD	3215533863
	DD	2132624928
	DD	3153789373
	DD	0
	DD	1072693248
	DD	2962101888
	DD	1063503197
	DD	3966050657
	DD	3215537841
	DD	3178024141
	DD	1070959142
	DD	1146830997
	DD	3214991029
	DD	1630003424
	DD	1069640468
	DD	2204651989
	DD	3214096041
	DD	796867276
	DD	1068243248
	DD	1883935754
	DD	3220736815
	DD	2988318241
	DD	1016251326
	DD	0
	DD	0
	DD	3919601072
	DD	3215121322
	DD	2384023593
	DD	1011053048
	DD	0
	DD	1072693248
	DD	1803946752
	DD	1062615560
	DD	2247013512
	DD	3215122999
	DD	3277043521
	DD	1070953215
	DD	578207576
	DD	3214490301
	DD	1619551293
	DD	1069630354
	DD	2648603598
	DD	3213667381
	DD	2728840488
	DD	1068193529
	DD	1727208548
	DD	3220749683
	DD	2531251621
	DD	1014661577
	DD	0
	DD	0
	DD	612012528
	DD	3214484294
	DD	549859680
	DD	1005659865
	DD	0
	DD	1072693248
	DD	2064191488
	DD	1061404484
	DD	4042886527
	DD	3214485286
	DD	149923164
	DD	1070948993
	DD	104448276
	DD	3213936806
	DD	2663431480
	DD	1069623160
	DD	4100741337
	DD	3213036454
	DD	3253086979
	DD	1068220130
	DD	1570481342
	DD	3220762551
	DD	2595802427
	DD	3161852775
	DD	0
	DD	0
	DD	75380366
	DD	3213435470
	DD	3015416698
	DD	1008346766
	DD	0
	DD	1072693248
	DD	2911696896
	DD	1059306942
	DD	451505506
	DD	3213435718
	DD	3089290734
	DD	1070946463
	DD	3705470162
	DD	3212886822
	DD	2568117130
	DD	1069618857
	DD	643302148
	DD	3211985100
	DD	500114129
	DD	1068202863
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
	DD 240 DUP (0H)	
_2il0floatpacket_46	DD	0f8000000H,0ffffffffH,0f8000000H,0ffffffffH,0f8000000H,0ffffffffH,0f8000000H,0ffffffffH
_2il0floatpacket_47	DD	000000000H,03ff00000H,000000000H,03ff00000H,000000000H,03ff00000H,000000000H,03ff00000H
_2il0floatpacket_48	DD	000000000H,042a80000H,000000000H,042a80000H,000000000H,042a80000H,000000000H,042a80000H
_2il0floatpacket_49	DD	054442d18H,0401921fbH,054442d18H,0401921fbH,054442d18H,0401921fbH,054442d18H,0401921fbH
_2il0floatpacket_50	DD	054000000H,0401921fbH,054000000H,0401921fbH,054000000H,0401921fbH,054000000H,0401921fbH
_2il0floatpacket_51	DD	011a62633H,03e310b46H,011a62633H,03e310b46H,011a62633H,03e310b46H,011a62633H,03e310b46H
_2il0floatpacket_52	DD	0ffffffffH,07fffffffH,0ffffffffH,07fffffffH,0ffffffffH,07fffffffH,0ffffffffH,07fffffffH
_2il0floatpacket_53	DD	000000000H,03eb00000H,000000000H,03eb00000H,000000000H,03eb00000H,000000000H,03eb00000H
_2il0floatpacket_76	DD	000000008H,000000008H,000000008H,000000008H,000000008H,000000008H,000000008H,000000008H
_2il0floatpacket_79	DD	000000000H,07ff00000H,000000000H,07ff00000H,000000000H,07ff00000H,000000000H,07ff00000H
_2il0floatpacket_80	DD	0ffffffffH,0000fffffH,0ffffffffH,0000fffffH,0ffffffffH,0000fffffH,0ffffffffH,0000fffffH
_2il0floatpacket_81	DD	000000000H,000100000H,000000000H,000100000H,000000000H,000100000H,000000000H,000100000H
_2il0floatpacket_82	DD	0ffffffffH,000000000H,0ffffffffH,000000000H,0ffffffffH,000000000H,0ffffffffH,000000000H
_2il0floatpacket_83	DD	000000000H,080000000H,000000000H,080000000H,000000000H,080000000H,000000000H,080000000H
_2il0floatpacket_84	DD	000000000H,03ff00000H,000000000H,03ff00000H,000000000H,03ff00000H,000000000H,03ff00000H
_2il0floatpacket_85	DD	0000001ffH,000000000H,0000001ffH,000000000H,0000001ffH,000000000H,0000001ffH,000000000H
_2il0floatpacket_86	DD	000000000H,039700000H,000000000H,039700000H,000000000H,039700000H,000000000H,039700000H
_2il0floatpacket_87	DD	000ffffffH,000000000H,000ffffffH,000000000H,000ffffffH,000000000H,000ffffffH,000000000H
_2il0floatpacket_88	DD	000000000H,03cb00000H,000000000H,03cb00000H,000000000H,03cb00000H,000000000H,03cb00000H
_2il0floatpacket_89	DD	000000fffH,000000000H,000000fffH,000000000H,000000fffH,000000000H,000000fffH,000000000H
_2il0floatpacket_90	DD	033145c07H,03cb1a626H,033145c07H,03cb1a626H,033145c07H,03cb1a626H,033145c07H,03cb1a626H
_2il0floatpacket_91	DD	054442d18H,03f8921fbH,054442d18H,03f8921fbH,054442d18H,03f8921fbH,054442d18H,03f8921fbH
_2il0floatpacket_92	DD	033145c07H,03c21a626H,033145c07H,03c21a626H,033145c07H,03c21a626H,033145c07H,03c21a626H
_2il0floatpacket_93	DD	000000078H,000000000H,000000078H,000000000H,000000078H,000000000H,000000078H,000000000H
_2il0floatpacket_26	DD	0000000ffH,000000000H,0000000ffH,000000000H
_2il0floatpacket_27	DD	0f8000000H,0ffffffffH,0f8000000H,0ffffffffH
_2il0floatpacket_28	DD	000000000H,03ff00000H,000000000H,03ff00000H
_2il0floatpacket_29	DD	000000000H,07ff00000H,000000000H,07ff00000H
_2il0floatpacket_30	DD	0ffffffffH,0000fffffH,0ffffffffH,0000fffffH
_2il0floatpacket_31	DD	000000000H,000100000H,000000000H,000100000H
_2il0floatpacket_32	DD	0ffffffffH,000000000H,0ffffffffH,000000000H
_2il0floatpacket_33	DD	000000000H,080000000H,000000000H,080000000H
_2il0floatpacket_34	DD	000000000H,03ff00000H,000000000H,03ff00000H
_2il0floatpacket_35	DD	000000000H,042a80000H,000000000H,042a80000H
_2il0floatpacket_36	DD	000000000H,039700000H,000000000H,039700000H
_2il0floatpacket_37	DD	000ffffffH,000000000H,000ffffffH,000000000H
_2il0floatpacket_38	DD	000000000H,03cb00000H,000000000H,03cb00000H
_2il0floatpacket_39	DD	000000fffH,000000000H,000000fffH,000000000H
_2il0floatpacket_40	DD	054442d18H,0401921fbH,054442d18H,0401921fbH
_2il0floatpacket_41	DD	054000000H,0401921fbH,054000000H,0401921fbH
_2il0floatpacket_42	DD	011a62633H,03e310b46H,011a62633H,03e310b46H
_2il0floatpacket_43	DD	0ffffffffH,07fffffffH,0ffffffffH,07fffffffH
_2il0floatpacket_44	DD	000000000H,03eb00000H,000000000H,03eb00000H
_2il0floatpacket_45	DD	0000001ffH,000000000H,0000001ffH,000000000H
_2il0floatpacket_54	DD	033145c07H,03cb1a626H,033145c07H,03cb1a626H
_2il0floatpacket_55	DD	000000008H,000000008H,000000008H,000000008H
_2il0floatpacket_56	DD	054442d18H,03f8921fbH,054442d18H,03f8921fbH
_2il0floatpacket_57	DD	033145c07H,03c21a626H,033145c07H,03c21a626H
_2il0floatpacket_58	DD	000000078H,000000000H,000000078H,000000000H
_2il0floatpacket_59	DD	0ffffffffH,0ffffffffH
_2il0floatpacket_60	DD	000000000H,07ff00000H
_2il0floatpacket_61	DD	0ffffffffH,0000fffffH
_2il0floatpacket_62	DD	000000000H,000100000H
_2il0floatpacket_63	DD	0ffffffffH,000000000H
_2il0floatpacket_64	DD	000000000H,080000000H
_2il0floatpacket_65	DD	000000000H,03ff00000H
_2il0floatpacket_66	DD	000000000H,042a80000H
_2il0floatpacket_67	DD	0000001ffH,000000000H
_2il0floatpacket_68	DD	000000000H,039700000H
_2il0floatpacket_69	DD	000ffffffH,000000000H
_2il0floatpacket_70	DD	000000000H,03cb00000H
_2il0floatpacket_71	DD	000000fffH,000000000H
_2il0floatpacket_72	DD	054442d18H,0401921fbH
_2il0floatpacket_73	DD	033145c07H,03cb1a626H
_2il0floatpacket_74	DD	0ffffffffH,07fffffffH
_2il0floatpacket_75	DD	000000000H,03eb00000H
_2il0floatpacket_77	DD	054442d18H,03f8921fbH
_2il0floatpacket_78	DD	033145c07H,03c21a626H
_vmldTanHATab	DD	0
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
