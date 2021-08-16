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
	PUBLIC __svml_hypot2_ha_e9

__svml_hypot2_ha_e9	PROC	EXPORT

_B1_1::

        DB        243
        DB        15
        DB        30
        DB        250
L1::

        sub       rsp, 344
        vmovups   XMMWORD PTR [256+rsp], xmm15
        vmovups   XMMWORD PTR [272+rsp], xmm14
        vmovups   XMMWORD PTR [304+rsp], xmm11
        vmovups   XMMWORD PTR [288+rsp], xmm10
        vmovapd   xmm10, xmm1
        mov       QWORD PTR [320+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovupd   xmm4, XMMWORD PTR [__svml_dhypot_ha_data_internal+64]
        and       r13, -64
        vandpd    xmm5, xmm0, xmm4
        vandpd    xmm3, xmm10, xmm4
        vmaxpd    xmm1, xmm5, xmm3
        vminpd    xmm2, xmm3, xmm5
        vpshufd   xmm14, xmm1, 221
        vpshufd   xmm15, xmm2, 221
        vmovq     xmm5, QWORD PTR [__svml_dhypot_ha_data_internal+512]
        vpsubd    xmm14, xmm14, xmm15
        vpcmpgtd  xmm5, xmm14, xmm5
        vmovupd   xmm11, XMMWORD PTR [__svml_dhypot_ha_data_internal]
        vpshufd   xmm5, xmm5, 80
        vandpd    xmm3, xmm1, xmm11
        vandnpd   xmm11, xmm5, xmm11
        vandpd    xmm15, xmm2, xmm11
        vsubpd    xmm4, xmm1, xmm3
        vsubpd    xmm14, xmm2, xmm15
        vaddpd    xmm1, xmm1, xmm3
        vmulpd    xmm5, xmm3, xmm3
        vmulpd    xmm3, xmm4, xmm1
        vmulpd    xmm2, xmm2, xmm14
        vmulpd    xmm11, xmm15, xmm15
        vmulpd    xmm14, xmm15, xmm14
        vaddpd    xmm4, xmm3, xmm2
        vaddpd    xmm5, xmm5, xmm11
        vaddpd    xmm15, xmm4, xmm14
        vaddpd    xmm2, xmm5, xmm15
        vmovq     xmm3, QWORD PTR [__svml_dhypot_ha_data_internal+640]
        vpshufd   xmm1, xmm2, 221
        vpcmpgtd  xmm14, xmm1, xmm3
        vcvtpd2ps xmm3, xmm2
        vmovq     xmm11, QWORD PTR [__svml_dhypot_ha_data_internal+576]
        vpcmpgtd  xmm4, xmm11, xmm1
        vpor      xmm11, xmm4, xmm14
        vmovlhps  xmm4, xmm3, xmm3
        vrsqrtps  xmm14, xmm4
        vcvtps2pd xmm14, xmm14
        vpshufd   xmm1, xmm11, 80
        vmovmskpd edx, xmm1
        vmulpd    xmm1, xmm14, xmm14
        vmulpd    xmm11, xmm5, xmm1
        vmulpd    xmm4, xmm15, xmm1
        vsubpd    xmm3, xmm11, XMMWORD PTR [__svml_dhypot_ha_data_internal+128]
        vmulpd    xmm15, xmm14, xmm15
        vaddpd    xmm11, xmm3, xmm4
        vmulpd    xmm5, xmm14, xmm5
        vmulpd    xmm1, xmm11, XMMWORD PTR [__svml_dhypot_ha_data_internal+192]
        vaddpd    xmm3, xmm1, XMMWORD PTR [__svml_dhypot_ha_data_internal+256]
        vmulpd    xmm4, xmm11, xmm3
        vaddpd    xmm1, xmm4, XMMWORD PTR [__svml_dhypot_ha_data_internal+320]
        vmulpd    xmm3, xmm11, xmm1
        vaddpd    xmm4, xmm3, XMMWORD PTR [__svml_dhypot_ha_data_internal+384]
        vmulpd    xmm1, xmm11, xmm4
        vaddpd    xmm3, xmm1, XMMWORD PTR [__svml_dhypot_ha_data_internal+448]
        vmulpd    xmm11, xmm11, xmm3
        vmulpd    xmm1, xmm14, xmm11
        vmulpd    xmm2, xmm2, xmm1
        vaddpd    xmm1, xmm2, xmm15
        mov       QWORD PTR [328+rsp], r13
        vaddpd    xmm1, xmm1, xmm5
        test      edx, edx
        jne       _B1_3

_B1_2::

        vmovups   xmm10, XMMWORD PTR [288+rsp]
        vmovapd   xmm0, xmm1
        vmovups   xmm11, XMMWORD PTR [304+rsp]
        vmovups   xmm14, XMMWORD PTR [272+rsp]
        vmovups   xmm15, XMMWORD PTR [256+rsp]
        mov       r13, QWORD PTR [320+rsp]
        add       rsp, 344
        ret

_B1_3::

        vmovupd   XMMWORD PTR [r13], xmm0
        vmovupd   XMMWORD PTR [64+r13], xmm10
        vmovupd   XMMWORD PTR [128+r13], xmm1

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

        call      __svml_dhypot_ha_cout_rare_internal
        jmp       _B1_8
        ALIGN     16

_B1_11::

__svml_hypot2_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot2_ha_e9_B1_B3:
	DD	801537
	DD	2675771
	DD	1222703
	DD	1292326
	DD	1173533
	DD	1112084
	DD	2818315

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_hypot2_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot2_ha_e9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B1_1
	DD	imagerel _B1_6
	DD	imagerel _unwind___svml_hypot2_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B1_6
	DD	imagerel _B1_11
	DD	imagerel _unwind___svml_hypot2_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST1:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_hypot4_ha_e9

__svml_hypot4_ha_e9	PROC	EXPORT

_B2_1::

        DB        243
        DB        15
        DB        30
        DB        250
L16::

        sub       rsp, 616
        vmovups   YMMWORD PTR [464+rsp], ymm15
        vmovups   YMMWORD PTR [496+rsp], ymm14
        vmovups   YMMWORD PTR [560+rsp], ymm12
        vmovups   YMMWORD PTR [528+rsp], ymm6
        mov       QWORD PTR [592+rsp], r13
        lea       r13, QWORD PTR [303+rsp]
        vmovupd   ymm2, YMMWORD PTR [__svml_dhypot_ha_data_internal+64]
        and       r13, -64
        vmovapd   ymm14, ymm1
        vmovupd   ymm1, YMMWORD PTR [__svml_dhypot_ha_data_internal]
        vandpd    ymm4, ymm0, ymm2
        vandpd    ymm12, ymm14, ymm2
        vmaxpd    ymm6, ymm4, ymm12
        vminpd    ymm4, ymm12, ymm4
        vandpd    ymm5, ymm6, ymm1
        vsubpd    ymm3, ymm6, ymm5
        mov       QWORD PTR [600+rsp], r13
        vextractf128 xmm12, ymm6, 1
        vextractf128 xmm2, ymm4, 1
        vshufps   xmm12, xmm6, xmm12, 221
        vshufps   xmm15, xmm4, xmm2, 221
        vpsubd    xmm2, xmm12, xmm15
        vpcmpgtd  xmm15, xmm2, XMMWORD PTR [__svml_dhypot_ha_data_internal+512]
        vpshufd   xmm12, xmm15, 80
        vpshufd   xmm2, xmm15, 250
        vinsertf128 ymm12, ymm12, xmm2, 1
        vandnpd   ymm1, ymm12, ymm1
        vandpd    ymm1, ymm4, ymm1
        vsubpd    ymm15, ymm4, ymm1
        vmulpd    ymm2, ymm5, ymm5
        vmulpd    ymm12, ymm1, ymm1
        vmulpd    ymm4, ymm4, ymm15
        vaddpd    ymm5, ymm6, ymm5
        vaddpd    ymm12, ymm2, ymm12
        vmulpd    ymm3, ymm3, ymm5
        vaddpd    ymm5, ymm3, ymm4
        vmulpd    ymm3, ymm1, ymm15
        vmovups   xmm1, XMMWORD PTR [__svml_dhypot_ha_data_internal+576]
        vaddpd    ymm5, ymm5, ymm3
        vaddpd    ymm3, ymm12, ymm5
        vextractf128 xmm15, ymm3, 1
        vshufps   xmm6, xmm3, xmm15, 221
        vpcmpgtd  xmm15, xmm6, XMMWORD PTR [__svml_dhypot_ha_data_internal+640]
        vpcmpgtd  xmm4, xmm1, xmm6
        vpor      xmm1, xmm4, xmm15
        vcvtpd2ps xmm15, ymm3
        vpshufd   xmm2, xmm1, 80
        vpshufd   xmm6, xmm1, 250
        vshufps   xmm4, xmm2, xmm6, 221
        vrsqrtps  xmm1, xmm15
        vmovmskps edx, xmm4
        vcvtps2pd ymm4, xmm1
        vmulpd    ymm6, ymm4, ymm4
        vmulpd    ymm2, ymm12, ymm6
        vmulpd    ymm1, ymm5, ymm6
        vmulpd    ymm5, ymm4, ymm5
        vmulpd    ymm12, ymm4, ymm12
        vsubpd    ymm15, ymm2, YMMWORD PTR [__svml_dhypot_ha_data_internal+128]
        vaddpd    ymm15, ymm15, ymm1
        vmulpd    ymm2, ymm15, YMMWORD PTR [__svml_dhypot_ha_data_internal+192]
        vaddpd    ymm6, ymm2, YMMWORD PTR [__svml_dhypot_ha_data_internal+256]
        vmulpd    ymm1, ymm15, ymm6
        vaddpd    ymm2, ymm1, YMMWORD PTR [__svml_dhypot_ha_data_internal+320]
        vmulpd    ymm6, ymm15, ymm2
        vaddpd    ymm1, ymm6, YMMWORD PTR [__svml_dhypot_ha_data_internal+384]
        vmulpd    ymm2, ymm15, ymm1
        vaddpd    ymm6, ymm2, YMMWORD PTR [__svml_dhypot_ha_data_internal+448]
        vmulpd    ymm15, ymm15, ymm6
        vmulpd    ymm1, ymm4, ymm15
        vmulpd    ymm3, ymm3, ymm1
        vaddpd    ymm1, ymm3, ymm5
        vaddpd    ymm1, ymm1, ymm12
        test      edx, edx
        jne       _B2_3

_B2_2::

        vmovups   ymm6, YMMWORD PTR [528+rsp]
        vmovups   ymm12, YMMWORD PTR [560+rsp]
        vmovups   ymm14, YMMWORD PTR [496+rsp]
        vmovups   ymm15, YMMWORD PTR [464+rsp]
        mov       r13, QWORD PTR [592+rsp]
        vmovapd   ymm0, ymm1
        add       rsp, 616
        ret

_B2_3::

        vmovupd   YMMWORD PTR [r13], ymm0
        vmovupd   YMMWORD PTR [64+r13], ymm14
        vmovupd   YMMWORD PTR [128+r13], ymm1

_B2_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [192+rsp], ymm7
        vmovups   YMMWORD PTR [160+rsp], ymm8
        vmovups   YMMWORD PTR [128+rsp], ymm9
        vmovups   YMMWORD PTR [96+rsp], ymm10
        vmovups   YMMWORD PTR [64+rsp], ymm11
        vmovups   YMMWORD PTR [32+rsp], ymm13
        mov       QWORD PTR [232+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [224+rsp], rsi
        mov       esi, edx

_B2_7::

        bt        esi, ebx
        jc        _B2_10

_B2_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B2_7

_B2_9::

        vmovups   ymm7, YMMWORD PTR [192+rsp]
        vmovups   ymm8, YMMWORD PTR [160+rsp]
        vmovups   ymm9, YMMWORD PTR [128+rsp]
        vmovups   ymm10, YMMWORD PTR [96+rsp]
        vmovups   ymm11, YMMWORD PTR [64+rsp]
        vmovups   ymm13, YMMWORD PTR [32+rsp]
        vmovupd   ymm1, YMMWORD PTR [128+r13]
        mov       rbx, QWORD PTR [232+rsp]
        mov       rsi, QWORD PTR [224+rsp]
        jmp       _B2_2

_B2_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]
        lea       r8, QWORD PTR [128+r13+rbx*8]

        call      __svml_dhypot_ha_cout_rare_internal
        jmp       _B2_8
        ALIGN     16

_B2_11::

__svml_hypot4_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot4_ha_e9_B1_B3:
	DD	800513
	DD	4903991
	DD	2189359
	DD	2344998
	DD	2091037
	DD	1964052
	DD	5046539

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_hypot4_ha_e9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot4_ha_e9_B6_B10:
	DD	1065249
	DD	1860673
	DD	1913911
	DD	186415
	DD	309289
	DD	436259
	DD	563229
	DD	690196
	DD	817163
	DD	imagerel _B2_1
	DD	imagerel _B2_6
	DD	imagerel _unwind___svml_hypot4_ha_e9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B2_6
	DD	imagerel _B2_11
	DD	imagerel _unwind___svml_hypot4_ha_e9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST2:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_hypot1_ha_l9

__svml_hypot1_ha_l9	PROC	EXPORT

_B3_1::

        DB        243
        DB        15
        DB        30
        DB        250
L43::

        sub       rsp, 328
        vmovapd   xmm4, xmm1
        vmovups   XMMWORD PTR [288+rsp], xmm15
        vmovapd   xmm2, xmm0
        vmovups   XMMWORD PTR [256+rsp], xmm10
        mov       eax, 6291456
        vmovups   XMMWORD PTR [272+rsp], xmm9
        mov       ecx, 1141899264
        vmovups   XMMWORD PTR [304+rsp], xmm7
        mov       edx, 1002438656
        vmovups   XMMWORD PTR [240+rsp], xmm6
        mov       QWORD PTR [232+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        vmovsd    xmm1, QWORD PTR [__svml_dhypot_ha_data_internal+64]
        and       r13, -64
        vandpd    xmm5, xmm2, xmm1
        vandpd    xmm6, xmm4, xmm1
        vmovddup  xmm7, xmm5
        vmovddup  xmm15, xmm6
        vmaxpd    xmm5, xmm7, xmm15
        vminpd    xmm15, xmm15, xmm7
        vpshufd   xmm1, xmm5, 85
        vpshufd   xmm6, xmm15, 85
        vmovsd    xmm0, QWORD PTR [__svml_dhypot_ha_data_internal]
        vpsubd    xmm7, xmm1, xmm6
        vmovd     xmm1, eax
        vandpd    xmm9, xmm5, xmm0
        vpcmpgtd  xmm6, xmm7, xmm1
        vsubsd    xmm10, xmm5, xmm9
        vpshufd   xmm7, xmm6, 0
        vmulsd    xmm6, xmm9, xmm9
        vaddsd    xmm9, xmm5, xmm9
        vandnpd   xmm0, xmm7, xmm0
        vandpd    xmm7, xmm15, xmm0
        vmulsd    xmm10, xmm9, xmm10
        vsubsd    xmm1, xmm15, xmm7
        vmovapd   xmm5, xmm1
        vmovapd   xmm0, xmm7
        vfmadd213sd xmm5, xmm15, xmm10
        vmovd     xmm10, ecx
        vfmadd213sd xmm0, xmm7, xmm6
        vmovsd    xmm3, QWORD PTR [__svml_dhypot_ha_data_internal+192]
        vfmadd213sd xmm1, xmm7, xmm5
        vmovd     xmm7, edx
        mov       QWORD PTR [320+rsp], r13
        vaddsd    xmm6, xmm0, xmm1
        vpshufd   xmm9, xmm6, 85
        vpcmpgtd  xmm5, xmm9, xmm10
        vpcmpgtd  xmm15, xmm7, xmm9
        vcvtpd2ps xmm10, xmm6
        vpor      xmm7, xmm15, xmm5
        vbroadcastss xmm15, xmm10
        vmovapd   xmm10, xmm1
        vpshufd   xmm9, xmm7, 0
        vmovmskpd eax, xmm9
        vmovapd   xmm9, xmm0
        vrsqrtps  xmm5, xmm15
        vcvtps2pd xmm5, xmm5
        vmulsd    xmm7, xmm5, xmm5
        vfmsub213sd xmm9, xmm7, QWORD PTR [__svml_dhypot_ha_data_internal+128]
        vfmadd213sd xmm10, xmm7, xmm9
        vfmadd213sd xmm3, xmm10, QWORD PTR [__svml_dhypot_ha_data_internal+256]
        vfmadd213sd xmm3, xmm10, QWORD PTR [__svml_dhypot_ha_data_internal+320]
        vfmadd213sd xmm3, xmm10, QWORD PTR [__svml_dhypot_ha_data_internal+384]
        vfmadd213sd xmm3, xmm10, QWORD PTR [__svml_dhypot_ha_data_internal+448]
        vmulsd    xmm3, xmm3, xmm10
        vmulsd    xmm3, xmm3, xmm5
        vmulsd    xmm6, xmm3, xmm6
        vfmadd213sd xmm1, xmm5, xmm6
        vfmadd213sd xmm0, xmm5, xmm1
        and       eax, 1
        jne       _B3_3

_B3_2::

        vmovups   xmm6, XMMWORD PTR [240+rsp]
        vmovups   xmm7, XMMWORD PTR [304+rsp]
        vmovups   xmm9, XMMWORD PTR [272+rsp]
        vmovups   xmm10, XMMWORD PTR [256+rsp]
        vmovups   xmm15, XMMWORD PTR [288+rsp]
        mov       r13, QWORD PTR [232+rsp]
        add       rsp, 328
        ret

_B3_3::

        vmovsd    QWORD PTR [r13], xmm2
        vmovsd    QWORD PTR [64+r13], xmm4
        vmovsd    QWORD PTR [128+r13], xmm0
        jne       _B3_6

_B3_4::

        vmovsd    xmm0, QWORD PTR [128+r13]
        jmp       _B3_2

_B3_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]
        lea       r8, QWORD PTR [128+r13]

        call      __svml_dhypot_ha_cout_rare_internal
        jmp       _B3_4
        ALIGN     16

_B3_7::

__svml_hypot1_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot1_ha_l9_B1_B6:
	DD	939777
	DD	1954903
	DD	1009743
	DD	1275969
	DD	1153075
	DD	1091621
	DD	1243160
	DD	2687243

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B3_1
	DD	imagerel _B3_7
	DD	imagerel _unwind___svml_hypot1_ha_l9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST3:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_hypot2_ha_l9

__svml_hypot2_ha_l9	PROC	EXPORT

_B4_1::

        DB        243
        DB        15
        DB        30
        DB        250
L56::

        sub       rsp, 376
        vmovups   XMMWORD PTR [288+rsp], xmm14
        vmovups   XMMWORD PTR [272+rsp], xmm11
        vmovups   XMMWORD PTR [336+rsp], xmm10
        vmovups   XMMWORD PTR [304+rsp], xmm9
        vmovups   XMMWORD PTR [320+rsp], xmm8
        vmovapd   xmm8, xmm1
        vmovups   XMMWORD PTR [256+rsp], xmm6
        mov       QWORD PTR [352+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        vmovupd   xmm2, XMMWORD PTR [__svml_dhypot_ha_data_internal+64]
        and       r13, -64
        vandpd    xmm3, xmm0, xmm2
        vandpd    xmm4, xmm8, xmm2
        vmaxpd    xmm1, xmm3, xmm4
        vminpd    xmm2, xmm4, xmm3
        vmovupd   xmm11, XMMWORD PTR [__svml_dhypot_ha_data_internal]
        vandpd    xmm3, xmm1, xmm11
        vsubpd    xmm4, xmm1, xmm3
        vpshufd   xmm9, xmm1, 221
        vaddpd    xmm1, xmm1, xmm3
        vpshufd   xmm10, xmm2, 221
        vmovq     xmm14, QWORD PTR [__svml_dhypot_ha_data_internal+512]
        vpsubd    xmm9, xmm9, xmm10
        vpcmpgtd  xmm14, xmm9, xmm14
        vpshufd   xmm14, xmm14, 80
        vmulpd    xmm4, xmm4, xmm1
        vandnpd   xmm11, xmm14, xmm11
        vandpd    xmm10, xmm2, xmm11
        vsubpd    xmm11, xmm2, xmm10
        vmulpd    xmm14, xmm3, xmm3
        vfmadd231pd xmm4, xmm2, xmm11
        vfmadd231pd xmm14, xmm10, xmm10
        vmovq     xmm2, QWORD PTR [__svml_dhypot_ha_data_internal+576]
        vmovq     xmm1, QWORD PTR [__svml_dhypot_ha_data_internal+640]
        vmovupd   xmm6, XMMWORD PTR [__svml_dhypot_ha_data_internal+128]
        vmovupd   xmm5, XMMWORD PTR [__svml_dhypot_ha_data_internal+192]
        vfmadd213pd xmm11, xmm10, xmm4
        vaddpd    xmm9, xmm14, xmm11
        vpshufd   xmm3, xmm9, 221
        vpcmpgtd  xmm2, xmm2, xmm3
        vpcmpgtd  xmm3, xmm3, xmm1
        vcvtpd2ps xmm1, xmm9
        vpor      xmm4, xmm2, xmm3
        vmovlhps  xmm2, xmm1, xmm1
        vrsqrtps  xmm3, xmm2
        vpshufd   xmm10, xmm4, 80
        vmovmskpd edx, xmm10
        vcvtps2pd xmm10, xmm3
        vmulpd    xmm4, xmm10, xmm10
        vfmsub231pd xmm6, xmm4, xmm14
        vfmadd231pd xmm6, xmm4, xmm11
        vfmadd213pd xmm5, xmm6, XMMWORD PTR [__svml_dhypot_ha_data_internal+256]
        vfmadd213pd xmm5, xmm6, XMMWORD PTR [__svml_dhypot_ha_data_internal+320]
        vfmadd213pd xmm5, xmm6, XMMWORD PTR [__svml_dhypot_ha_data_internal+384]
        vfmadd213pd xmm5, xmm6, XMMWORD PTR [__svml_dhypot_ha_data_internal+448]
        vmulpd    xmm5, xmm6, xmm5
        vmulpd    xmm4, xmm10, xmm5
        vmulpd    xmm5, xmm9, xmm4
        vfmadd213pd xmm11, xmm10, xmm5
        mov       QWORD PTR [360+rsp], r13
        vfmadd213pd xmm14, xmm10, xmm11
        test      edx, edx
        jne       _B4_3

_B4_2::

        vmovups   xmm6, XMMWORD PTR [256+rsp]
        vmovapd   xmm0, xmm14
        vmovups   xmm8, XMMWORD PTR [320+rsp]
        vmovups   xmm9, XMMWORD PTR [304+rsp]
        vmovups   xmm10, XMMWORD PTR [336+rsp]
        vmovups   xmm11, XMMWORD PTR [272+rsp]
        vmovups   xmm14, XMMWORD PTR [288+rsp]
        mov       r13, QWORD PTR [352+rsp]
        add       rsp, 376
        ret

_B4_3::

        vmovupd   XMMWORD PTR [r13], xmm0
        vmovupd   XMMWORD PTR [64+r13], xmm8
        vmovupd   XMMWORD PTR [128+r13], xmm14

_B4_6::

        xor       eax, eax
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, edx

_B4_7::

        bt        esi, ebx
        jc        _B4_10

_B4_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B4_7

_B4_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        vmovupd   xmm14, XMMWORD PTR [128+r13]
        jmp       _B4_2

_B4_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]
        lea       r8, QWORD PTR [128+r13+rbx*8]

        call      __svml_dhypot_ha_cout_rare_internal
        jmp       _B4_8
        ALIGN     16

_B4_11::

__svml_hypot2_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot2_ha_l9_B1_B3:
	DD	1068289
	DD	2937933
	DD	1075269
	DD	1345592
	DD	1284143
	DD	1419302
	DD	1161245
	DD	1239060
	DD	3080459

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_hypot2_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot2_ha_l9_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B4_1
	DD	imagerel _B4_6
	DD	imagerel _unwind___svml_hypot2_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B4_6
	DD	imagerel _B4_11
	DD	imagerel _unwind___svml_hypot2_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST4:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_hypot4_ha_l9

__svml_hypot4_ha_l9	PROC	EXPORT

_B5_1::

        DB        243
        DB        15
        DB        30
        DB        250
L75::

        sub       rsp, 616
        vmovups   YMMWORD PTR [496+rsp], ymm14
        vmovups   YMMWORD PTR [464+rsp], ymm12
        vmovups   YMMWORD PTR [528+rsp], ymm10
        vmovups   YMMWORD PTR [432+rsp], ymm8
        vmovups   YMMWORD PTR [560+rsp], ymm7
        mov       QWORD PTR [592+rsp], r13
        lea       r13, QWORD PTR [271+rsp]
        vmovupd   ymm5, YMMWORD PTR [__svml_dhypot_ha_data_internal+64]
        and       r13, -64
        vmovupd   ymm2, YMMWORD PTR [__svml_dhypot_ha_data_internal]
        vmovupd   ymm12, YMMWORD PTR [__svml_dhypot_ha_data_internal+128]
        vmovupd   ymm14, YMMWORD PTR [__svml_dhypot_ha_data_internal+192]
        vmovdqa   ymm10, ymm0
        vandpd    ymm8, ymm10, ymm5
        vandpd    ymm0, ymm1, ymm5
        vmaxpd    ymm7, ymm8, ymm0
        vminpd    ymm4, ymm0, ymm8
        vandpd    ymm5, ymm7, ymm2
        vsubpd    ymm3, ymm7, ymm5
        mov       QWORD PTR [600+rsp], r13
        vextracti128 xmm0, ymm7, 1
        vextracti128 xmm8, ymm4, 1
        vshufps   xmm0, xmm7, xmm0, 221
        vshufps   xmm8, xmm4, xmm8, 221
        vpsubd    xmm0, xmm0, xmm8
        vpcmpgtd  xmm8, xmm0, XMMWORD PTR [__svml_dhypot_ha_data_internal+512]
        vpshufd   xmm0, xmm8, 80
        vpshufd   xmm8, xmm8, 250
        vinserti128 ymm0, ymm0, xmm8, 1
        vandnpd   ymm2, ymm0, ymm2
        vmulpd    ymm0, ymm5, ymm5
        vaddpd    ymm5, ymm7, ymm5
        vmulpd    ymm3, ymm3, ymm5
        vandpd    ymm2, ymm4, ymm2
        vsubpd    ymm8, ymm4, ymm2
        vfmadd231pd ymm0, ymm2, ymm2
        vfmadd231pd ymm3, ymm4, ymm8
        vfmadd213pd ymm8, ymm2, ymm3
        vmovups   xmm2, XMMWORD PTR [__svml_dhypot_ha_data_internal+576]
        vaddpd    ymm3, ymm0, ymm8
        vextracti128 xmm4, ymm3, 1
        vshufps   xmm5, xmm3, xmm4, 221
        vpcmpgtd  xmm4, xmm5, XMMWORD PTR [__svml_dhypot_ha_data_internal+640]
        vpcmpgtd  xmm7, xmm2, xmm5
        vpor      xmm5, xmm7, xmm4
        vpshufd   xmm2, xmm5, 80
        vpshufd   xmm7, xmm5, 250
        vinserti128 ymm4, ymm2, xmm7, 1
        vcvtpd2ps xmm2, ymm3
        vrsqrtps  xmm5, xmm2
        vmovmskpd edx, ymm4
        vcvtps2pd ymm4, xmm5
        test      edx, edx
        vmulpd    ymm7, ymm4, ymm4
        vfmsub231pd ymm12, ymm7, ymm0
        vfmadd231pd ymm12, ymm7, ymm8
        vfmadd213pd ymm14, ymm12, YMMWORD PTR [__svml_dhypot_ha_data_internal+256]
        vfmadd213pd ymm14, ymm12, YMMWORD PTR [__svml_dhypot_ha_data_internal+320]
        vfmadd213pd ymm14, ymm12, YMMWORD PTR [__svml_dhypot_ha_data_internal+384]
        vfmadd213pd ymm14, ymm12, YMMWORD PTR [__svml_dhypot_ha_data_internal+448]
        vmulpd    ymm12, ymm12, ymm14
        vmulpd    ymm14, ymm4, ymm12
        vmulpd    ymm2, ymm3, ymm14
        vfmadd213pd ymm8, ymm4, ymm2
        vfmadd213pd ymm0, ymm4, ymm8
        jne       _B5_3

_B5_2::

        vmovups   ymm7, YMMWORD PTR [560+rsp]
        vmovups   ymm8, YMMWORD PTR [432+rsp]
        vmovups   ymm10, YMMWORD PTR [528+rsp]
        vmovups   ymm12, YMMWORD PTR [464+rsp]
        vmovups   ymm14, YMMWORD PTR [496+rsp]
        mov       r13, QWORD PTR [592+rsp]
        add       rsp, 616
        ret

_B5_3::

        vmovupd   YMMWORD PTR [r13], ymm10
        vmovupd   YMMWORD PTR [64+r13], ymm1
        vmovupd   YMMWORD PTR [128+r13], ymm0

_B5_6::

        xor       eax, eax
        vmovups   YMMWORD PTR [160+rsp], ymm6
        vmovups   YMMWORD PTR [128+rsp], ymm9
        vmovups   YMMWORD PTR [96+rsp], ymm11
        vmovups   YMMWORD PTR [64+rsp], ymm13
        vmovups   YMMWORD PTR [32+rsp], ymm15
        mov       QWORD PTR [200+rsp], rbx
        mov       ebx, eax
        mov       QWORD PTR [192+rsp], rsi
        mov       esi, edx

_B5_7::

        bt        esi, ebx
        jc        _B5_10

_B5_8::

        inc       ebx
        cmp       ebx, 4
        jl        _B5_7

_B5_9::

        vmovups   ymm6, YMMWORD PTR [160+rsp]
        vmovups   ymm9, YMMWORD PTR [128+rsp]
        vmovups   ymm11, YMMWORD PTR [96+rsp]
        vmovups   ymm13, YMMWORD PTR [64+rsp]
        vmovups   ymm15, YMMWORD PTR [32+rsp]
        vmovupd   ymm0, YMMWORD PTR [128+r13]
        mov       rbx, QWORD PTR [200+rsp]
        mov       rsi, QWORD PTR [192+rsp]
        jmp       _B5_2

_B5_10::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]
        lea       r8, QWORD PTR [128+r13+rbx*8]

        call      __svml_dhypot_ha_cout_rare_internal
        jmp       _B5_8
        ALIGN     16

_B5_11::

__svml_hypot4_ha_l9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot4_ha_l9_B1_B3:
	DD	933889
	DD	4904000
	DD	2324536
	DD	1804335
	DD	2205734
	DD	1951773
	DD	2091028
	DD	5046539

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_hypot4_ha_l9_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot4_ha_l9_B6_B10:
	DD	931873
	DD	1598520
	DD	1651758
	DD	194598
	DD	317472
	DD	440346
	DD	563220
	DD	681995
	DD	imagerel _B5_1
	DD	imagerel _B5_6
	DD	imagerel _unwind___svml_hypot4_ha_l9_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B5_6
	DD	imagerel _B5_11
	DD	imagerel _unwind___svml_hypot4_ha_l9_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST5:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_hypot2_ha_ex

__svml_hypot2_ha_ex	PROC	EXPORT

_B6_1::

        DB        243
        DB        15
        DB        30
        DB        250
L102::

        sub       rsp, 360
        movups    XMMWORD PTR [288+rsp], xmm14
        movaps    xmm14, xmm0
        movups    XMMWORD PTR [304+rsp], xmm13
        movups    XMMWORD PTR [256+rsp], xmm10
        movups    XMMWORD PTR [320+rsp], xmm9
        movups    XMMWORD PTR [272+rsp], xmm6
        mov       QWORD PTR [336+rsp], r13
        lea       r13, QWORD PTR [111+rsp]
        movups    xmm9, XMMWORD PTR [__svml_dhypot_ha_data_internal+64]
        and       r13, -64
        movaps    xmm3, xmm9
        andps     xmm9, xmm1
        andps     xmm3, xmm14
        movaps    xmm10, xmm3
        maxpd     xmm10, xmm9
        minpd     xmm9, xmm3
        pshufd    xmm3, xmm10, 221
        movaps    xmm6, xmm10
        pshufd    xmm0, xmm9, 221
        movq      xmm2, QWORD PTR [__svml_dhypot_ha_data_internal+512]
        psubd     xmm3, xmm0
        pcmpgtd   xmm3, xmm2
        movaps    xmm0, xmm9
        movups    xmm5, XMMWORD PTR [__svml_dhypot_ha_data_internal]
        pshufd    xmm2, xmm3, 80
        movaps    xmm4, xmm5
        andnps    xmm2, xmm5
        andps     xmm4, xmm10
        andps     xmm2, xmm9
        movaps    xmm3, xmm4
        subpd     xmm6, xmm4
        subpd     xmm0, xmm2
        addpd     xmm10, xmm4
        mulpd     xmm3, xmm4
        mulpd     xmm6, xmm10
        mulpd     xmm9, xmm0
        movaps    xmm5, xmm2
        mulpd     xmm5, xmm2
        mulpd     xmm2, xmm0
        addpd     xmm6, xmm9
        addpd     xmm3, xmm5
        addpd     xmm6, xmm2
        movaps    xmm0, xmm3
        addpd     xmm0, xmm6
        cvtpd2ps  xmm9, xmm0
        movlhps   xmm9, xmm9
        rsqrtps   xmm10, xmm9
        movq      xmm5, QWORD PTR [__svml_dhypot_ha_data_internal+576]
        movq      xmm4, QWORD PTR [__svml_dhypot_ha_data_internal+640]
        pshufd    xmm2, xmm0, 221
        pcmpgtd   xmm5, xmm2
        pcmpgtd   xmm2, xmm4
        por       xmm5, xmm2
        pshufd    xmm4, xmm5, 80
        cvtps2pd  xmm5, xmm10
        movmskpd  eax, xmm4
        movaps    xmm2, xmm5
        movaps    xmm4, xmm3
        mulpd     xmm2, xmm5
        mulpd     xmm4, xmm2
        mulpd     xmm2, xmm6
        subpd     xmm4, XMMWORD PTR [__svml_dhypot_ha_data_internal+128]
        mulpd     xmm6, xmm5
        addpd     xmm4, xmm2
        movups    xmm13, XMMWORD PTR [__svml_dhypot_ha_data_internal+192]
        mulpd     xmm13, xmm4
        addpd     xmm13, XMMWORD PTR [__svml_dhypot_ha_data_internal+256]
        mulpd     xmm13, xmm4
        addpd     xmm13, XMMWORD PTR [__svml_dhypot_ha_data_internal+320]
        mulpd     xmm13, xmm4
        addpd     xmm13, XMMWORD PTR [__svml_dhypot_ha_data_internal+384]
        mulpd     xmm13, xmm4
        addpd     xmm13, XMMWORD PTR [__svml_dhypot_ha_data_internal+448]
        mulpd     xmm4, xmm13
        mulpd     xmm4, xmm5
        mulpd     xmm5, xmm3
        mulpd     xmm0, xmm4
        addpd     xmm0, xmm6
        mov       QWORD PTR [344+rsp], r13
        addpd     xmm0, xmm5
        test      eax, eax
        jne       _B6_3

_B6_2::

        movups    xmm6, XMMWORD PTR [272+rsp]
        movups    xmm9, XMMWORD PTR [320+rsp]
        movups    xmm10, XMMWORD PTR [256+rsp]
        movups    xmm13, XMMWORD PTR [304+rsp]
        movups    xmm14, XMMWORD PTR [288+rsp]
        mov       r13, QWORD PTR [336+rsp]
        add       rsp, 360
        ret

_B6_3::

        movups    XMMWORD PTR [r13], xmm14
        movups    XMMWORD PTR [64+r13], xmm1
        movups    XMMWORD PTR [128+r13], xmm0

_B6_6::

        xor       ecx, ecx
        mov       QWORD PTR [40+rsp], rbx
        mov       ebx, ecx
        mov       QWORD PTR [32+rsp], rsi
        mov       esi, eax

_B6_7::

        mov       ecx, ebx
        mov       edx, 1
        shl       edx, cl
        test      esi, edx
        jne       _B6_10

_B6_8::

        inc       ebx
        cmp       ebx, 2
        jl        _B6_7

_B6_9::

        mov       rbx, QWORD PTR [40+rsp]
        mov       rsi, QWORD PTR [32+rsp]
        movups    xmm0, XMMWORD PTR [128+r13]
        jmp       _B6_2

_B6_10::

        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]
        lea       r8, QWORD PTR [128+r13+rbx*8]

        call      __svml_dhypot_ha_cout_rare_internal
        jmp       _B6_8
        ALIGN     16

_B6_11::

__svml_hypot2_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot2_ha_ex_B1_B3:
	DD	934657
	DD	2806851
	DD	1140795
	DD	1349683
	DD	1091626
	DD	1300513
	DD	1239060
	DD	2949387

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_hypot2_ha_ex_B1_B3

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot2_ha_ex_B6_B10:
	DD	265761
	DD	287758
	DD	340999
	DD	imagerel _B6_1
	DD	imagerel _B6_6
	DD	imagerel _unwind___svml_hypot2_ha_ex_B1_B3

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B6_6
	DD	imagerel _B6_11
	DD	imagerel _unwind___svml_hypot2_ha_ex_B6_B10

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST6:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_hypot1_ha_ex

__svml_hypot1_ha_ex	PROC	EXPORT

_B7_1::

        DB        243
        DB        15
        DB        30
        DB        250
L119::

        sub       rsp, 328
        movaps    xmm2, xmm1
        movups    XMMWORD PTR [288+rsp], xmm13
        mov       eax, 6291456
        movups    XMMWORD PTR [240+rsp], xmm12
        mov       edx, 1002438656
        movups    XMMWORD PTR [272+rsp], xmm11
        mov       ecx, 1141899264
        movups    XMMWORD PTR [256+rsp], xmm9
        movups    XMMWORD PTR [304+rsp], xmm8
        movd      xmm4, eax
        mov       QWORD PTR [232+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        movsd     xmm12, QWORD PTR [__svml_dhypot_ha_data_internal+64]
        and       r13, -64
        movaps    xmm5, xmm12
        andps     xmm12, xmm2
        andps     xmm5, xmm0
        unpcklpd  xmm5, xmm5
        unpcklpd  xmm12, xmm12
        movaps    xmm13, xmm5
        maxpd     xmm13, xmm12
        minpd     xmm12, xmm5
        pshufd    xmm5, xmm13, 85
        movaps    xmm11, xmm13
        pshufd    xmm3, xmm12, 85
        psubd     xmm5, xmm3
        pcmpgtd   xmm5, xmm4
        movsd     xmm8, QWORD PTR [__svml_dhypot_ha_data_internal]
        pshufd    xmm4, xmm5, 0
        movaps    xmm9, xmm8
        andnps    xmm4, xmm8
        movaps    xmm5, xmm12
        andps     xmm4, xmm12
        andps     xmm9, xmm13
        movaps    xmm3, xmm9
        movaps    xmm8, xmm4
        mulsd     xmm3, xmm9
        subsd     xmm5, xmm4
        subsd     xmm11, xmm9
        mulsd     xmm8, xmm4
        addsd     xmm13, xmm9
        movaps    xmm9, xmm5
        addsd     xmm8, xmm3
        mulsd     xmm13, xmm11
        mulsd     xmm9, xmm12
        mulsd     xmm5, xmm4
        addsd     xmm9, xmm13
        movaps    xmm3, xmm8
        movd      xmm12, edx
        movd      xmm4, ecx
        addsd     xmm5, xmm9
        movsd     xmm1, QWORD PTR [__svml_dhypot_ha_data_internal+192]
        addsd     xmm3, xmm5
        pshufd    xmm11, xmm3, 85
        pcmpgtd   xmm12, xmm11
        pcmpgtd   xmm11, xmm4
        cvtpd2ps  xmm4, xmm3
        shufps    xmm4, xmm4, 0
        por       xmm12, xmm11
        rsqrtps   xmm9, xmm4
        pshufd    xmm13, xmm12, 0
        movaps    xmm12, xmm8
        movmskpd  eax, xmm13
        movaps    xmm4, xmm5
        cvtps2pd  xmm13, xmm9
        movaps    xmm11, xmm13
        and       eax, 1
        mulsd     xmm11, xmm13
        mulsd     xmm5, xmm13
        mulsd     xmm8, xmm13
        mulsd     xmm12, xmm11
        mulsd     xmm4, xmm11
        subsd     xmm12, QWORD PTR [__svml_dhypot_ha_data_internal+128]
        mov       QWORD PTR [320+rsp], r13
        addsd     xmm4, xmm12
        mulsd     xmm1, xmm4
        addsd     xmm1, QWORD PTR [__svml_dhypot_ha_data_internal+256]
        mulsd     xmm1, xmm4
        addsd     xmm1, QWORD PTR [__svml_dhypot_ha_data_internal+320]
        mulsd     xmm1, xmm4
        addsd     xmm1, QWORD PTR [__svml_dhypot_ha_data_internal+384]
        mulsd     xmm1, xmm4
        addsd     xmm1, QWORD PTR [__svml_dhypot_ha_data_internal+448]
        mulsd     xmm1, xmm4
        mulsd     xmm1, xmm13
        mulsd     xmm1, xmm3
        addsd     xmm5, xmm1
        addsd     xmm8, xmm5
        jne       _B7_3

_B7_2::

        movups    xmm9, XMMWORD PTR [256+rsp]
        movaps    xmm0, xmm8
        movups    xmm8, XMMWORD PTR [304+rsp]
        movups    xmm11, XMMWORD PTR [272+rsp]
        movups    xmm12, XMMWORD PTR [240+rsp]
        movups    xmm13, XMMWORD PTR [288+rsp]
        mov       r13, QWORD PTR [232+rsp]
        add       rsp, 328
        ret

_B7_3::

        movsd     QWORD PTR [r13], xmm0
        movsd     QWORD PTR [64+r13], xmm2
        movsd     QWORD PTR [128+r13], xmm8
        jne       _B7_6

_B7_4::

        movsd     xmm8, QWORD PTR [128+r13]
        jmp       _B7_2

_B7_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]
        lea       r8, QWORD PTR [128+r13]

        call      __svml_dhypot_ha_cout_rare_internal
        jmp       _B7_4
        ALIGN     16

_B7_7::

__svml_hypot1_ha_ex ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot1_ha_ex_B1_B6:
	DD	939521
	DD	1954902
	DD	1280074
	DD	1087553
	DD	1161267
	DD	1034277
	DD	1234967
	DD	2687243

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B7_1
	DD	imagerel _B7_7
	DD	imagerel _unwind___svml_hypot1_ha_ex_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST7:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_hypot8_ha_z0

__svml_hypot8_ha_z0	PROC	EXPORT

_B8_1::

        DB        243
        DB        15
        DB        30
        DB        250
L132::

        sub       rsp, 1400
        vmovaps   zmm2, zmm1
        vmulpd    zmm23, zmm0, zmm0 {rn-sae}
        vmulpd    zmm24, zmm2, zmm2 {rn-sae}
        vgetexppd zmm3, zmm0 {sae}
        vgetexppd zmm4, zmm2 {sae}
        vaddpd    zmm1, zmm23, zmm24 {rn-sae}
        vmaxpd    zmm25, zmm23, zmm24 {sae}
        vmaxpd    zmm5, zmm3, zmm4 {sae}
        vrsqrt14pd zmm3, zmm1
        vminpd    zmm26, zmm23, zmm24 {sae}
        vsubpd    zmm27, zmm1, zmm25 {rn-sae}
        vmulpd    zmm4, zmm1, zmm3 {rn-sae}
        vsubpd    zmm30, zmm26, zmm27 {rn-sae}
        mov       QWORD PTR [1384+rsp], r13
        lea       r13, QWORD PTR [1183+rsp]
        vmovaps   zmm28, zmm0
        and       r13, -64
        vfmsub213pd zmm28, zmm0, zmm23 {rn-sae}
        vmovups   zmm23, ZMMWORD PTR [__svml_dhypot_ha_data_internal+832]
        vandpd    zmm22, zmm5, ZMMWORD PTR [__svml_dhypot_ha_data_internal+64]
        vpcmpq    k0, zmm22, ZMMWORD PTR [__svml_dhypot_ha_data_internal+704], 5
        vmulpd    zmm22, zmm23, zmm3 {rn-sae}
        kmovw     r8d, k0
        vfnmadd231pd zmm23, zmm22, zmm4 {rn-sae}
        vmovaps   zmm29, zmm2
        vfmsub213pd zmm29, zmm2, zmm24 {rn-sae}
        vfmadd231pd zmm22, zmm22, zmm23 {rn-sae}
        vfmadd213pd zmm23, zmm4, zmm4 {rn-sae}
        vaddpd    zmm31, zmm28, zmm29 {rn-sae}
        vfnmadd231pd zmm1, zmm23, zmm23 {rn-sae}
        vaddpd    zmm5, zmm30, zmm31 {rn-sae}
        vaddpd    zmm1, zmm1, zmm5 {rn-sae}
        mov       QWORD PTR [1392+rsp], r13
        vfmadd213pd zmm1, zmm22, zmm23 {rn-sae}
        test      r8d, r8d
        jne       _B8_3

_B8_2::

        vmovaps   zmm0, zmm1
        mov       r13, QWORD PTR [1384+rsp]
        add       rsp, 1400
        ret

_B8_3::

        vstmxcsr  DWORD PTR [1376+rsp]

_B8_4::

        movzx     edx, WORD PTR [1376+rsp]
        mov       eax, edx
        or        eax, 8064
        cmp       edx, eax
        je        _B8_6

_B8_5::

        mov       DWORD PTR [1376+rsp], eax
        vldmxcsr  DWORD PTR [1376+rsp]

_B8_6::

        vmovups   ZMMWORD PTR [r13], zmm0
        vmovups   ZMMWORD PTR [64+r13], zmm2
        vmovups   ZMMWORD PTR [128+r13], zmm1
        test      r8d, r8d
        jne       _B8_11

_B8_7::

        cmp       edx, eax
        je        _B8_2

_B8_8::

        vstmxcsr  DWORD PTR [1376+rsp]
        mov       eax, DWORD PTR [1376+rsp]

_B8_9::

        and       eax, -8065
        or        eax, edx
        mov       DWORD PTR [1376+rsp], eax
        vldmxcsr  DWORD PTR [1376+rsp]
        jmp       _B8_2

_B8_11::

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

_B8_12::

        bt        edi, ebx
        jc        _B8_15

_B8_13::

        inc       ebx
        cmp       ebx, 8
        jl        _B8_12

_B8_14::

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
        jmp       _B8_7

_B8_15::

        vzeroupper
        lea       rcx, QWORD PTR [r13+rbx*8]
        lea       rdx, QWORD PTR [64+r13+rbx*8]
        lea       r8, QWORD PTR [128+r13+rbx*8]

        call      __svml_dhypot_ha_cout_rare_internal
        jmp       _B8_13
        ALIGN     16

_B8_16::

__svml_hypot8_ha_z0 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot8_ha_z0_B1_B9:
	DD	286977
	DD	11392097
	DD	11469067

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B8_1
	DD	imagerel _B8_11
	DD	imagerel _unwind___svml_hypot8_ha_z0_B1_B9

.pdata	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot8_ha_z0_B11_B15:
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

	DD	imagerel _B8_11
	DD	imagerel _B8_16
	DD	imagerel _unwind___svml_hypot8_ha_z0_B11_B15

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST8:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_hypot1_ha_e9

__svml_hypot1_ha_e9	PROC	EXPORT

_B9_1::

        DB        243
        DB        15
        DB        30
        DB        250
L183::

        sub       rsp, 328
        vmovapd   xmm3, xmm1
        vmovups   XMMWORD PTR [256+rsp], xmm15
        mov       eax, 6291456
        vmovups   XMMWORD PTR [240+rsp], xmm14
        mov       ecx, 1141899264
        vmovups   XMMWORD PTR [272+rsp], xmm13
        mov       edx, 1002438656
        vmovups   XMMWORD PTR [288+rsp], xmm11
        vmovups   XMMWORD PTR [304+rsp], xmm10
        mov       QWORD PTR [232+rsp], r13
        lea       r13, QWORD PTR [95+rsp]
        vmovsd    xmm4, QWORD PTR [__svml_dhypot_ha_data_internal+64]
        and       r13, -64
        vandpd    xmm5, xmm0, xmm4
        vandpd    xmm11, xmm3, xmm4
        vmovddup  xmm13, xmm5
        vmovddup  xmm4, xmm11
        vmaxpd    xmm10, xmm13, xmm4
        vminpd    xmm15, xmm4, xmm13
        vpshufd   xmm14, xmm10, 85
        vpshufd   xmm4, xmm15, 85
        vpsubd    xmm13, xmm14, xmm4
        vmovd     xmm14, eax
        vpcmpgtd  xmm4, xmm13, xmm14
        vmovsd    xmm2, QWORD PTR [__svml_dhypot_ha_data_internal]
        vpshufd   xmm13, xmm4, 0
        vandpd    xmm11, xmm10, xmm2
        vandnpd   xmm2, xmm13, xmm2
        vsubsd    xmm5, xmm10, xmm11
        vmulsd    xmm4, xmm11, xmm11
        vaddsd    xmm10, xmm10, xmm11
        vandpd    xmm13, xmm15, xmm2
        vmulsd    xmm5, xmm10, xmm5
        vsubsd    xmm14, xmm15, xmm13
        vmulsd    xmm2, xmm13, xmm13
        vmulsd    xmm15, xmm14, xmm15
        vmulsd    xmm11, xmm14, xmm13
        vaddsd    xmm2, xmm2, xmm4
        vaddsd    xmm10, xmm15, xmm5
        vmovd     xmm14, ecx
        vmovd     xmm4, edx
        vmovsd    xmm1, QWORD PTR [__svml_dhypot_ha_data_internal+192]
        vaddsd    xmm11, xmm11, xmm10
        mov       QWORD PTR [320+rsp], r13
        vaddsd    xmm10, xmm2, xmm11
        vpshufd   xmm13, xmm10, 85
        vpcmpgtd  xmm5, xmm13, xmm14
        vpcmpgtd  xmm15, xmm4, xmm13
        vcvtpd2ps xmm14, xmm10
        vpor      xmm4, xmm15, xmm5
        vshufps   xmm15, xmm14, xmm14, 0
        vrsqrtps  xmm5, xmm15
        vcvtps2pd xmm5, xmm5
        vpshufd   xmm13, xmm4, 0
        vmovmskpd eax, xmm13
        vmulsd    xmm13, xmm5, xmm5
        vmulsd    xmm4, xmm2, xmm13
        vmulsd    xmm14, xmm11, xmm13
        vmulsd    xmm11, xmm11, xmm5
        vsubsd    xmm15, xmm4, QWORD PTR [__svml_dhypot_ha_data_internal+128]
        vmulsd    xmm2, xmm2, xmm5
        vaddsd    xmm4, xmm14, xmm15
        vmulsd    xmm1, xmm1, xmm4
        vaddsd    xmm1, xmm1, QWORD PTR [__svml_dhypot_ha_data_internal+256]
        vmulsd    xmm13, xmm1, xmm4
        vaddsd    xmm14, xmm13, QWORD PTR [__svml_dhypot_ha_data_internal+320]
        vmulsd    xmm15, xmm14, xmm4
        vaddsd    xmm1, xmm15, QWORD PTR [__svml_dhypot_ha_data_internal+384]
        vmulsd    xmm13, xmm1, xmm4
        vaddsd    xmm14, xmm13, QWORD PTR [__svml_dhypot_ha_data_internal+448]
        vmulsd    xmm4, xmm14, xmm4
        vmulsd    xmm1, xmm4, xmm5
        vmulsd    xmm10, xmm1, xmm10
        vaddsd    xmm1, xmm11, xmm10
        vaddsd    xmm1, xmm2, xmm1
        and       eax, 1
        jne       _B9_3

_B9_2::

        vmovups   xmm10, XMMWORD PTR [304+rsp]
        vmovapd   xmm0, xmm1
        vmovups   xmm11, XMMWORD PTR [288+rsp]
        vmovups   xmm13, XMMWORD PTR [272+rsp]
        vmovups   xmm14, XMMWORD PTR [240+rsp]
        vmovups   xmm15, XMMWORD PTR [256+rsp]
        mov       r13, QWORD PTR [232+rsp]
        add       rsp, 328
        ret

_B9_3::

        vmovsd    QWORD PTR [r13], xmm0
        vmovsd    QWORD PTR [64+r13], xmm3
        vmovsd    QWORD PTR [128+r13], xmm1
        jne       _B9_6

_B9_4::

        vmovsd    xmm1, QWORD PTR [128+r13]
        jmp       _B9_2

_B9_6::

        lea       rcx, QWORD PTR [r13]
        lea       rdx, QWORD PTR [64+r13]
        lea       r8, QWORD PTR [128+r13]

        call      __svml_dhypot_ha_cout_rare_internal
        jmp       _B9_4
        ALIGN     16

_B9_7::

__svml_hypot1_ha_e9 ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_hypot1_ha_e9_B1_B6:
	DD	938753
	DD	1954899
	DD	1288267
	DD	1226818
	DD	1169460
	DD	1042470
	DD	1112088
	DD	2687243

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B9_1
	DD	imagerel _B9_7
	DD	imagerel _unwind___svml_hypot1_ha_e9_B1_B6

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_TEXT	SEGMENT      'CODE'

TXTST9:

_TEXT	ENDS
_TEXT	SEGMENT      'CODE'

       ALIGN     16
	PUBLIC __svml_dhypot_ha_cout_rare_internal

__svml_dhypot_ha_cout_rare_internal	PROC

_B10_1::

        DB        243
        DB        15
        DB        30
        DB        250
L196::

        push      r12
        sub       rsp, 128
        lea       r10, QWORD PTR [__ImageBase]
        movzx     eax, WORD PTR [6+rcx]
        and       eax, 32752
        movups    XMMWORD PTR [32+rsp], xmm15
        movups    XMMWORD PTR [48+rsp], xmm14
        cmp       eax, 32752
        je        _B10_14

_B10_2::

        movzx     eax, WORD PTR [6+rdx]
        and       eax, 32752
        cmp       eax, 32752
        je        _B10_13

_B10_3::

        movsd     xmm1, QWORD PTR [rcx]
        movsd     xmm4, QWORD PTR [_vmldHypotHATab+4096]
        mov       r9b, BYTE PTR [7+rcx]
        mov       al, BYTE PTR [7+rdx]
        movsd     xmm0, QWORD PTR [rdx]
        ucomisd   xmm1, xmm4
        jp        _B10_4
        je        _B10_11

_B10_4::

        movsd     QWORD PTR [120+rsp], xmm1
        and       r9b, 127
        movsd     QWORD PTR [88+rsp], xmm0
        and       al, 127
        mov       BYTE PTR [127+rsp], r9b
        mov       BYTE PTR [95+rsp], al
        movsd     xmm0, QWORD PTR [120+rsp]
        movsd     xmm3, QWORD PTR [88+rsp]
        comisd    xmm3, xmm0
        jbe       _B10_6

_B10_5::

        movaps    xmm1, xmm0
        movaps    xmm0, xmm3
        movsd     QWORD PTR [120+rsp], xmm3
        movaps    xmm3, xmm1

_B10_6::

        movzx     edx, WORD PTR [126+rsp]
        and       edx, 32752
        shr       edx, 4
        neg       edx
        movzx     r11d, WORD PTR [_vmldHypotHATab+4102]
        and       r11d, -32753
        movsd     QWORD PTR [80+rsp], xmm4
        movsd     xmm2, QWORD PTR [_vmldHypotHATab+4128]
        lea       r9d, DWORD PTR [1025+rdx]
        neg       r9d
        add       r9d, 1000
        shr       r9d, 31
        imul      eax, r9d, -23
        lea       r9d, DWORD PTR [1025+rax+rdx]
        lea       ecx, DWORD PTR [1023+r9]
        and       ecx, 2047
        shl       ecx, 4
        or        r11d, ecx
        mov       WORD PTR [86+rsp], r11w
        movsd     xmm1, QWORD PTR [80+rsp]
        mulsd     xmm0, xmm1
        mulsd     xmm3, xmm1
        mulsd     xmm2, xmm0
        movsd     QWORD PTR [64+rsp], xmm2
        movsd     xmm5, QWORD PTR [64+rsp]
        movsd     QWORD PTR [120+rsp], xmm0
        subsd     xmm5, xmm0
        movsd     QWORD PTR [72+rsp], xmm5
        movsd     xmm15, QWORD PTR [64+rsp]
        movsd     xmm14, QWORD PTR [72+rsp]
        movsd     QWORD PTR [88+rsp], xmm3
        subsd     xmm15, xmm14
        movsd     QWORD PTR [64+rsp], xmm15
        movsd     xmm1, QWORD PTR [64+rsp]
        movzx     eax, WORD PTR [126+rsp]
        subsd     xmm0, xmm1
        movzx     r12d, WORD PTR [94+rsp]
        and       eax, 32752
        and       r12d, 32752
        shr       eax, 4
        shr       r12d, 4
        movsd     QWORD PTR [72+rsp], xmm0
        sub       eax, r12d
        movsd     xmm2, QWORD PTR [64+rsp]
        movsd     xmm0, QWORD PTR [72+rsp]
        cmp       eax, 6
        jle       _B10_8

_B10_7::

        movaps    xmm14, xmm3
        jmp       _B10_9

_B10_8::

        movsd     xmm3, QWORD PTR [88+rsp]
        movsd     xmm1, QWORD PTR [_vmldHypotHATab+4128]
        mulsd     xmm1, xmm3
        movsd     QWORD PTR [64+rsp], xmm1
        movaps    xmm1, xmm3
        movsd     xmm4, QWORD PTR [64+rsp]
        subsd     xmm4, QWORD PTR [88+rsp]
        movsd     QWORD PTR [72+rsp], xmm4
        movsd     xmm14, QWORD PTR [64+rsp]
        movsd     xmm5, QWORD PTR [72+rsp]
        subsd     xmm14, xmm5
        movsd     QWORD PTR [64+rsp], xmm14
        movsd     xmm15, QWORD PTR [64+rsp]
        subsd     xmm1, xmm15
        movsd     QWORD PTR [72+rsp], xmm1
        movsd     xmm4, QWORD PTR [64+rsp]
        movsd     xmm14, QWORD PTR [72+rsp]

_B10_9::

        movsd     xmm5, QWORD PTR [120+rsp]
        movaps    xmm1, xmm2
        mulsd     xmm3, xmm14
        addsd     xmm5, xmm2
        mulsd     xmm1, xmm2
        mulsd     xmm0, xmm5
        movaps    xmm15, xmm4
        neg       r9d
        mulsd     xmm15, xmm4
        addsd     xmm0, xmm3
        mulsd     xmm4, xmm14
        addsd     xmm1, xmm15
        addsd     xmm0, xmm4
        movaps    xmm14, xmm1
        add       r9d, 1023
        mov       rax, QWORD PTR [_vmldHypotHATab+4112]
        mov       r12, rax
        shr       r12, 48
        and       r9d, 2047
        shl       r9d, 4
        addsd     xmm14, xmm0
        movsd     QWORD PTR [80+rsp], xmm14
        and       r12d, -32753
        movzx     ecx, WORD PTR [86+rsp]
        and       ecx, 32752
        shr       ecx, 4
        add       ecx, -1023
        mov       edx, ecx
        and       edx, 1
        sub       ecx, edx
        shr       ecx, 1
        movsd     QWORD PTR [88+rsp], xmm14
        movzx     r11d, WORD PTR [94+rsp]
        and       r11d, -32753
        add       r11d, 16368
        mov       WORD PTR [94+rsp], r11w
        lea       r11d, DWORD PTR [1023+rcx]
        add       ecx, ecx
        and       r11d, 2047
        neg       ecx
        add       ecx, 1023
        and       ecx, 2047
        shl       r11d, 4
        shl       ecx, 4
        or        r11d, r12d
        movsd     xmm5, QWORD PTR [88+rsp]
        or        ecx, r12d
        or        r12d, r9d
        mov       r9d, DWORD PTR [92+rsp]
        mulsd     xmm5, QWORD PTR [imagerel(_vmldHypotHATab)+4112+r10+rdx*8]
        and       r9d, 1048575
        shr       r9d, 12
        shl       edx, 8
        add       r9d, edx
        movsd     xmm2, QWORD PTR [imagerel(_vmldHypotHATab)+r10+r9*8]
        movsd     xmm14, QWORD PTR [_vmldHypotHATab+4104]
        mulsd     xmm5, xmm2
        mulsd     xmm14, xmm2
        movaps    xmm3, xmm5
        mulsd     xmm3, xmm14
        movsd     xmm2, QWORD PTR [_vmldHypotHATab+4104]
        mov       QWORD PTR [104+rsp], rax
        subsd     xmm2, xmm3
        movaps    xmm4, xmm2
        mulsd     xmm2, xmm5
        mulsd     xmm4, xmm14
        addsd     xmm5, xmm2
        addsd     xmm14, xmm4
        movaps    xmm3, xmm5
        movaps    xmm15, xmm14
        mulsd     xmm3, xmm14
        movsd     xmm2, QWORD PTR [_vmldHypotHATab+4104]
        mov       WORD PTR [110+rsp], cx
        subsd     xmm2, xmm3
        mulsd     xmm15, xmm2
        mulsd     xmm2, xmm5
        addsd     xmm14, xmm15
        addsd     xmm5, xmm2
        movaps    xmm3, xmm5
        movaps    xmm4, xmm14
        mulsd     xmm3, xmm14
        movsd     xmm2, QWORD PTR [_vmldHypotHATab+4104]
        mov       QWORD PTR [96+rsp], rax
        subsd     xmm2, xmm3
        mulsd     xmm4, xmm2
        mulsd     xmm2, xmm5
        addsd     xmm14, xmm4
        addsd     xmm5, xmm2
        movsd     xmm3, QWORD PTR [_vmldHypotHATab+4128]
        mulsd     xmm3, xmm5
        movsd     QWORD PTR [64+rsp], xmm3
        movsd     xmm15, QWORD PTR [64+rsp]
        mov       WORD PTR [102+rsp], r11w
        subsd     xmm15, xmm5
        movsd     QWORD PTR [72+rsp], xmm15
        movsd     xmm3, QWORD PTR [64+rsp]
        movsd     xmm2, QWORD PTR [72+rsp]
        mov       QWORD PTR [112+rsp], rax
        subsd     xmm3, xmm2
        movsd     QWORD PTR [64+rsp], xmm3
        movaps    xmm2, xmm5
        movsd     xmm4, QWORD PTR [64+rsp]
        mov       WORD PTR [118+rsp], r12w
        subsd     xmm2, xmm4
        movsd     QWORD PTR [72+rsp], xmm2
        movsd     xmm2, QWORD PTR [64+rsp]
        movsd     xmm4, QWORD PTR [104+rsp]
        movaps    xmm15, xmm2
        mulsd     xmm1, xmm4
        mulsd     xmm15, xmm2
        mulsd     xmm0, xmm4
        subsd     xmm1, xmm15
        movsd     xmm15, QWORD PTR [_vmldHypotHATab+4120]
        mulsd     xmm15, xmm2
        movsd     xmm3, QWORD PTR [72+rsp]
        mulsd     xmm15, xmm3
        mulsd     xmm3, xmm3
        subsd     xmm1, xmm15
        subsd     xmm1, xmm3
        addsd     xmm1, xmm0
        mulsd     xmm14, xmm1
        addsd     xmm5, xmm14
        mulsd     xmm5, QWORD PTR [96+rsp]
        mulsd     xmm5, QWORD PTR [112+rsp]
        movsd     QWORD PTR [r8], xmm5

_B10_10::

        movups    xmm14, XMMWORD PTR [48+rsp]
        xor       eax, eax
        movups    xmm15, XMMWORD PTR [32+rsp]
        add       rsp, 128
        pop       r12
        ret

_B10_11::

        ucomisd   xmm0, xmm4
        jne       _B10_4
        jp        _B10_4

_B10_12::

        movsd     QWORD PTR [r8], xmm4
        jmp       _B10_10

_B10_13::

        movsd     xmm0, QWORD PTR [rdx]
        mulsd     xmm0, xmm0
        movsd     QWORD PTR [r8], xmm0
        jmp       _B10_10

_B10_14::

        movzx     eax, WORD PTR [6+rdx]
        and       eax, 32752
        cmp       eax, 32752
        je        _B10_16

_B10_15::

        movsd     xmm0, QWORD PTR [rcx]
        mulsd     xmm0, xmm0
        movsd     QWORD PTR [r8], xmm0
        jmp       _B10_10

_B10_16::

        mov       eax, DWORD PTR [4+rcx]
        and       eax, 1048575
        jne       _B10_18

_B10_17::

        cmp       DWORD PTR [rcx], 0
        je        _B10_22

_B10_18::

        test      DWORD PTR [4+rdx], 1048575
        jne       _B10_20

_B10_19::

        cmp       DWORD PTR [rdx], 0
        je        _B10_21

_B10_20::

        movsd     xmm0, QWORD PTR [rcx]
        mulsd     xmm0, QWORD PTR [rdx]
        movsd     QWORD PTR [r8], xmm0
        jmp       _B10_10

_B10_21::

        test      eax, eax
        jne       _B10_13

_B10_27::

        cmp       DWORD PTR [rcx], 0

_B10_22::

        je        _B10_15
        jmp       _B10_13
        ALIGN     16

_B10_25::

__svml_dhypot_ha_cout_rare_internal ENDP

_TEXT	ENDS
.xdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H
_unwind___svml_dhypot_ha_cout_rare_internal_B1_B22:
	DD	403713
	DD	256041
	DD	194595
	DD	3221680653

.xdata	ENDS
.pdata	SEGMENT  DWORD   READ  ''

	ALIGN 004H

	DD	imagerel _B10_1
	DD	imagerel _B10_25
	DD	imagerel _unwind___svml_dhypot_ha_cout_rare_internal_B1_B22

.pdata	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS

_RDATA	SEGMENT     READ PAGE   'DATA'
	ALIGN  32
	PUBLIC __svml_dhypot_ha_data_internal
__svml_dhypot_ha_data_internal	DD	0
	DD	4294950912
	DD	0
	DD	4294950912
	DD	0
	DD	4294950912
	DD	0
	DD	4294950912
	DD	0
	DD	4294950912
	DD	0
	DD	4294950912
	DD	0
	DD	4294950912
	DD	0
	DD	4294950912
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
	DD	3218046976
	DD	0
	DD	3218046976
	DD	0
	DD	3218046976
	DD	0
	DD	3218046976
	DD	0
	DD	3218046976
	DD	0
	DD	3218046976
	DD	0
	DD	3218046976
	DD	0
	DD	3218046976
	DD	0
	DD	1070694400
	DD	0
	DD	1070694400
	DD	0
	DD	1070694400
	DD	0
	DD	1070694400
	DD	0
	DD	1070694400
	DD	0
	DD	1070694400
	DD	0
	DD	1070694400
	DD	0
	DD	1070694400
	DD	0
	DD	3218341888
	DD	0
	DD	3218341888
	DD	0
	DD	3218341888
	DD	0
	DD	3218341888
	DD	0
	DD	3218341888
	DD	0
	DD	3218341888
	DD	0
	DD	3218341888
	DD	0
	DD	3218341888
	DD	0
	DD	1071120384
	DD	0
	DD	1071120384
	DD	0
	DD	1071120384
	DD	0
	DD	1071120384
	DD	0
	DD	1071120384
	DD	0
	DD	1071120384
	DD	0
	DD	1071120384
	DD	0
	DD	1071120384
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
	DD	6291456
	DD	6291456
	DD	6291456
	DD	6291456
	DD	6291456
	DD	6291456
	DD	6291456
	DD	6291456
	DD	6291456
	DD	6291456
	DD	6291456
	DD	6291456
	DD	6291456
	DD	6291456
	DD	6291456
	DD	6291456
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1002438656
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	1141899264
	DD	0
	DD	1082126336
	DD	0
	DD	1082126336
	DD	0
	DD	1082126336
	DD	0
	DD	1082126336
	DD	0
	DD	1082126336
	DD	0
	DD	1082126336
	DD	0
	DD	1082126336
	DD	0
	DD	1082126336
	DD	0
	DD	1078951936
	DD	0
	DD	1078951936
	DD	0
	DD	1078951936
	DD	0
	DD	1078951936
	DD	0
	DD	1078951936
	DD	0
	DD	1078951936
	DD	0
	DD	1078951936
	DD	0
	DD	1078951936
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
_vmldHypotHATab	DD	0
	DD	1072693248
	DD	0
	DD	1072689152
	DD	0
	DD	1072685056
	DD	0
	DD	1072680960
	DD	0
	DD	1072676864
	DD	0
	DD	1072672768
	DD	0
	DD	1072668672
	DD	0
	DD	1072665600
	DD	0
	DD	1072661504
	DD	0
	DD	1072657408
	DD	0
	DD	1072653312
	DD	0
	DD	1072649216
	DD	0
	DD	1072646144
	DD	0
	DD	1072642048
	DD	0
	DD	1072637952
	DD	0
	DD	1072634880
	DD	0
	DD	1072630784
	DD	0
	DD	1072626688
	DD	0
	DD	1072623616
	DD	0
	DD	1072619520
	DD	0
	DD	1072615424
	DD	0
	DD	1072612352
	DD	0
	DD	1072608256
	DD	0
	DD	1072605184
	DD	0
	DD	1072601088
	DD	0
	DD	1072598016
	DD	0
	DD	1072593920
	DD	0
	DD	1072590848
	DD	0
	DD	1072586752
	DD	0
	DD	1072583680
	DD	0
	DD	1072580608
	DD	0
	DD	1072576512
	DD	0
	DD	1072573440
	DD	0
	DD	1072570368
	DD	0
	DD	1072566272
	DD	0
	DD	1072563200
	DD	0
	DD	1072560128
	DD	0
	DD	1072556032
	DD	0
	DD	1072552960
	DD	0
	DD	1072549888
	DD	0
	DD	1072546816
	DD	0
	DD	1072542720
	DD	0
	DD	1072539648
	DD	0
	DD	1072536576
	DD	0
	DD	1072533504
	DD	0
	DD	1072530432
	DD	0
	DD	1072527360
	DD	0
	DD	1072523264
	DD	0
	DD	1072520192
	DD	0
	DD	1072517120
	DD	0
	DD	1072514048
	DD	0
	DD	1072510976
	DD	0
	DD	1072507904
	DD	0
	DD	1072504832
	DD	0
	DD	1072501760
	DD	0
	DD	1072498688
	DD	0
	DD	1072495616
	DD	0
	DD	1072492544
	DD	0
	DD	1072489472
	DD	0
	DD	1072486400
	DD	0
	DD	1072483328
	DD	0
	DD	1072480256
	DD	0
	DD	1072478208
	DD	0
	DD	1072475136
	DD	0
	DD	1072472064
	DD	0
	DD	1072468992
	DD	0
	DD	1072465920
	DD	0
	DD	1072462848
	DD	0
	DD	1072459776
	DD	0
	DD	1072457728
	DD	0
	DD	1072454656
	DD	0
	DD	1072451584
	DD	0
	DD	1072448512
	DD	0
	DD	1072446464
	DD	0
	DD	1072443392
	DD	0
	DD	1072440320
	DD	0
	DD	1072437248
	DD	0
	DD	1072435200
	DD	0
	DD	1072432128
	DD	0
	DD	1072429056
	DD	0
	DD	1072427008
	DD	0
	DD	1072423936
	DD	0
	DD	1072420864
	DD	0
	DD	1072418816
	DD	0
	DD	1072415744
	DD	0
	DD	1072412672
	DD	0
	DD	1072410624
	DD	0
	DD	1072407552
	DD	0
	DD	1072405504
	DD	0
	DD	1072402432
	DD	0
	DD	1072400384
	DD	0
	DD	1072397312
	DD	0
	DD	1072395264
	DD	0
	DD	1072392192
	DD	0
	DD	1072390144
	DD	0
	DD	1072387072
	DD	0
	DD	1072385024
	DD	0
	DD	1072381952
	DD	0
	DD	1072379904
	DD	0
	DD	1072376832
	DD	0
	DD	1072374784
	DD	0
	DD	1072371712
	DD	0
	DD	1072369664
	DD	0
	DD	1072366592
	DD	0
	DD	1072364544
	DD	0
	DD	1072362496
	DD	0
	DD	1072359424
	DD	0
	DD	1072357376
	DD	0
	DD	1072355328
	DD	0
	DD	1072352256
	DD	0
	DD	1072350208
	DD	0
	DD	1072347136
	DD	0
	DD	1072345088
	DD	0
	DD	1072343040
	DD	0
	DD	1072340992
	DD	0
	DD	1072337920
	DD	0
	DD	1072335872
	DD	0
	DD	1072333824
	DD	0
	DD	1072330752
	DD	0
	DD	1072328704
	DD	0
	DD	1072326656
	DD	0
	DD	1072324608
	DD	0
	DD	1072321536
	DD	0
	DD	1072319488
	DD	0
	DD	1072317440
	DD	0
	DD	1072315392
	DD	0
	DD	1072313344
	DD	0
	DD	1072310272
	DD	0
	DD	1072308224
	DD	0
	DD	1072306176
	DD	0
	DD	1072304128
	DD	0
	DD	1072302080
	DD	0
	DD	1072300032
	DD	0
	DD	1072296960
	DD	0
	DD	1072294912
	DD	0
	DD	1072292864
	DD	0
	DD	1072290816
	DD	0
	DD	1072288768
	DD	0
	DD	1072286720
	DD	0
	DD	1072284672
	DD	0
	DD	1072282624
	DD	0
	DD	1072280576
	DD	0
	DD	1072278528
	DD	0
	DD	1072275456
	DD	0
	DD	1072273408
	DD	0
	DD	1072271360
	DD	0
	DD	1072269312
	DD	0
	DD	1072267264
	DD	0
	DD	1072265216
	DD	0
	DD	1072263168
	DD	0
	DD	1072261120
	DD	0
	DD	1072259072
	DD	0
	DD	1072257024
	DD	0
	DD	1072254976
	DD	0
	DD	1072252928
	DD	0
	DD	1072250880
	DD	0
	DD	1072248832
	DD	0
	DD	1072246784
	DD	0
	DD	1072244736
	DD	0
	DD	1072243712
	DD	0
	DD	1072241664
	DD	0
	DD	1072239616
	DD	0
	DD	1072237568
	DD	0
	DD	1072235520
	DD	0
	DD	1072233472
	DD	0
	DD	1072231424
	DD	0
	DD	1072229376
	DD	0
	DD	1072227328
	DD	0
	DD	1072225280
	DD	0
	DD	1072223232
	DD	0
	DD	1072222208
	DD	0
	DD	1072220160
	DD	0
	DD	1072218112
	DD	0
	DD	1072216064
	DD	0
	DD	1072214016
	DD	0
	DD	1072211968
	DD	0
	DD	1072210944
	DD	0
	DD	1072208896
	DD	0
	DD	1072206848
	DD	0
	DD	1072204800
	DD	0
	DD	1072202752
	DD	0
	DD	1072201728
	DD	0
	DD	1072199680
	DD	0
	DD	1072197632
	DD	0
	DD	1072195584
	DD	0
	DD	1072193536
	DD	0
	DD	1072192512
	DD	0
	DD	1072190464
	DD	0
	DD	1072188416
	DD	0
	DD	1072186368
	DD	0
	DD	1072185344
	DD	0
	DD	1072183296
	DD	0
	DD	1072181248
	DD	0
	DD	1072179200
	DD	0
	DD	1072178176
	DD	0
	DD	1072176128
	DD	0
	DD	1072174080
	DD	0
	DD	1072173056
	DD	0
	DD	1072171008
	DD	0
	DD	1072168960
	DD	0
	DD	1072167936
	DD	0
	DD	1072165888
	DD	0
	DD	1072163840
	DD	0
	DD	1072161792
	DD	0
	DD	1072160768
	DD	0
	DD	1072158720
	DD	0
	DD	1072157696
	DD	0
	DD	1072155648
	DD	0
	DD	1072153600
	DD	0
	DD	1072152576
	DD	0
	DD	1072150528
	DD	0
	DD	1072148480
	DD	0
	DD	1072147456
	DD	0
	DD	1072145408
	DD	0
	DD	1072143360
	DD	0
	DD	1072142336
	DD	0
	DD	1072140288
	DD	0
	DD	1072139264
	DD	0
	DD	1072137216
	DD	0
	DD	1072135168
	DD	0
	DD	1072134144
	DD	0
	DD	1072132096
	DD	0
	DD	1072131072
	DD	0
	DD	1072129024
	DD	0
	DD	1072128000
	DD	0
	DD	1072125952
	DD	0
	DD	1072124928
	DD	0
	DD	1072122880
	DD	0
	DD	1072120832
	DD	0
	DD	1072119808
	DD	0
	DD	1072117760
	DD	0
	DD	1072116736
	DD	0
	DD	1072114688
	DD	0
	DD	1072113664
	DD	0
	DD	1072111616
	DD	0
	DD	1072110592
	DD	0
	DD	1072108544
	DD	0
	DD	1072107520
	DD	0
	DD	1072105472
	DD	0
	DD	1072104448
	DD	0
	DD	1072102400
	DD	0
	DD	1072101376
	DD	0
	DD	1072099328
	DD	0
	DD	1072098304
	DD	0
	DD	1072096256
	DD	0
	DD	1072095232
	DD	0
	DD	1072094208
	DD	0
	DD	1072092160
	DD	0
	DD	1072091136
	DD	0
	DD	1072089088
	DD	0
	DD	1072088064
	DD	0
	DD	1072086016
	DD	0
	DD	1072084992
	DD	0
	DD	1072082944
	DD	0
	DD	1072081920
	DD	0
	DD	1072080896
	DD	0
	DD	1072078848
	DD	0
	DD	1072075776
	DD	0
	DD	1072073728
	DD	0
	DD	1072070656
	DD	0
	DD	1072067584
	DD	0
	DD	1072064512
	DD	0
	DD	1072061440
	DD	0
	DD	1072059392
	DD	0
	DD	1072056320
	DD	0
	DD	1072053248
	DD	0
	DD	1072051200
	DD	0
	DD	1072048128
	DD	0
	DD	1072045056
	DD	0
	DD	1072043008
	DD	0
	DD	1072039936
	DD	0
	DD	1072037888
	DD	0
	DD	1072034816
	DD	0
	DD	1072031744
	DD	0
	DD	1072029696
	DD	0
	DD	1072026624
	DD	0
	DD	1072024576
	DD	0
	DD	1072021504
	DD	0
	DD	1072019456
	DD	0
	DD	1072016384
	DD	0
	DD	1072014336
	DD	0
	DD	1072011264
	DD	0
	DD	1072009216
	DD	0
	DD	1072006144
	DD	0
	DD	1072004096
	DD	0
	DD	1072002048
	DD	0
	DD	1071998976
	DD	0
	DD	1071996928
	DD	0
	DD	1071993856
	DD	0
	DD	1071991808
	DD	0
	DD	1071989760
	DD	0
	DD	1071986688
	DD	0
	DD	1071984640
	DD	0
	DD	1071982592
	DD	0
	DD	1071979520
	DD	0
	DD	1071977472
	DD	0
	DD	1071975424
	DD	0
	DD	1071972352
	DD	0
	DD	1071970304
	DD	0
	DD	1071968256
	DD	0
	DD	1071966208
	DD	0
	DD	1071964160
	DD	0
	DD	1071961088
	DD	0
	DD	1071959040
	DD	0
	DD	1071956992
	DD	0
	DD	1071954944
	DD	0
	DD	1071952896
	DD	0
	DD	1071949824
	DD	0
	DD	1071947776
	DD	0
	DD	1071945728
	DD	0
	DD	1071943680
	DD	0
	DD	1071941632
	DD	0
	DD	1071939584
	DD	0
	DD	1071937536
	DD	0
	DD	1071935488
	DD	0
	DD	1071933440
	DD	0
	DD	1071930368
	DD	0
	DD	1071928320
	DD	0
	DD	1071926272
	DD	0
	DD	1071924224
	DD	0
	DD	1071922176
	DD	0
	DD	1071920128
	DD	0
	DD	1071918080
	DD	0
	DD	1071916032
	DD	0
	DD	1071913984
	DD	0
	DD	1071911936
	DD	0
	DD	1071909888
	DD	0
	DD	1071907840
	DD	0
	DD	1071905792
	DD	0
	DD	1071903744
	DD	0
	DD	1071901696
	DD	0
	DD	1071900672
	DD	0
	DD	1071898624
	DD	0
	DD	1071896576
	DD	0
	DD	1071894528
	DD	0
	DD	1071892480
	DD	0
	DD	1071890432
	DD	0
	DD	1071888384
	DD	0
	DD	1071886336
	DD	0
	DD	1071884288
	DD	0
	DD	1071883264
	DD	0
	DD	1071881216
	DD	0
	DD	1071879168
	DD	0
	DD	1071877120
	DD	0
	DD	1071875072
	DD	0
	DD	1071873024
	DD	0
	DD	1071872000
	DD	0
	DD	1071869952
	DD	0
	DD	1071867904
	DD	0
	DD	1071865856
	DD	0
	DD	1071864832
	DD	0
	DD	1071862784
	DD	0
	DD	1071860736
	DD	0
	DD	1071858688
	DD	0
	DD	1071856640
	DD	0
	DD	1071855616
	DD	0
	DD	1071853568
	DD	0
	DD	1071851520
	DD	0
	DD	1071850496
	DD	0
	DD	1071848448
	DD	0
	DD	1071846400
	DD	0
	DD	1071844352
	DD	0
	DD	1071843328
	DD	0
	DD	1071841280
	DD	0
	DD	1071839232
	DD	0
	DD	1071838208
	DD	0
	DD	1071836160
	DD	0
	DD	1071834112
	DD	0
	DD	1071833088
	DD	0
	DD	1071831040
	DD	0
	DD	1071830016
	DD	0
	DD	1071827968
	DD	0
	DD	1071825920
	DD	0
	DD	1071824896
	DD	0
	DD	1071822848
	DD	0
	DD	1071821824
	DD	0
	DD	1071819776
	DD	0
	DD	1071817728
	DD	0
	DD	1071816704
	DD	0
	DD	1071814656
	DD	0
	DD	1071813632
	DD	0
	DD	1071811584
	DD	0
	DD	1071810560
	DD	0
	DD	1071808512
	DD	0
	DD	1071806464
	DD	0
	DD	1071805440
	DD	0
	DD	1071803392
	DD	0
	DD	1071802368
	DD	0
	DD	1071800320
	DD	0
	DD	1071799296
	DD	0
	DD	1071797248
	DD	0
	DD	1071796224
	DD	0
	DD	1071794176
	DD	0
	DD	1071793152
	DD	0
	DD	1071791104
	DD	0
	DD	1071790080
	DD	0
	DD	1071788032
	DD	0
	DD	1071787008
	DD	0
	DD	1071784960
	DD	0
	DD	1071783936
	DD	0
	DD	1071782912
	DD	0
	DD	1071780864
	DD	0
	DD	1071779840
	DD	0
	DD	1071777792
	DD	0
	DD	1071776768
	DD	0
	DD	1071774720
	DD	0
	DD	1071773696
	DD	0
	DD	1071772672
	DD	0
	DD	1071770624
	DD	0
	DD	1071769600
	DD	0
	DD	1071767552
	DD	0
	DD	1071766528
	DD	0
	DD	1071765504
	DD	0
	DD	1071763456
	DD	0
	DD	1071762432
	DD	0
	DD	1071760384
	DD	0
	DD	1071759360
	DD	0
	DD	1071758336
	DD	0
	DD	1071756288
	DD	0
	DD	1071755264
	DD	0
	DD	1071754240
	DD	0
	DD	1071752192
	DD	0
	DD	1071751168
	DD	0
	DD	1071750144
	DD	0
	DD	1071748096
	DD	0
	DD	1071747072
	DD	0
	DD	1071746048
	DD	0
	DD	1071744000
	DD	0
	DD	1071742976
	DD	0
	DD	1071741952
	DD	0
	DD	1071739904
	DD	0
	DD	1071738880
	DD	0
	DD	1071737856
	DD	0
	DD	1071736832
	DD	0
	DD	1071734784
	DD	0
	DD	1071733760
	DD	0
	DD	1071732736
	DD	0
	DD	1071730688
	DD	0
	DD	1071729664
	DD	0
	DD	1071728640
	DD	0
	DD	1071727616
	DD	0
	DD	1071725568
	DD	0
	DD	1071724544
	DD	0
	DD	1071723520
	DD	0
	DD	1071722496
	DD	0
	DD	1071720448
	DD	0
	DD	1071719424
	DD	0
	DD	1071718400
	DD	0
	DD	1071717376
	DD	0
	DD	1071715328
	DD	0
	DD	1071714304
	DD	0
	DD	1071713280
	DD	0
	DD	1071712256
	DD	0
	DD	1071711232
	DD	0
	DD	1071709184
	DD	0
	DD	1071708160
	DD	0
	DD	1071707136
	DD	0
	DD	1071706112
	DD	0
	DD	1071705088
	DD	0
	DD	1071704064
	DD	0
	DD	1071702016
	DD	0
	DD	1071700992
	DD	0
	DD	1071699968
	DD	0
	DD	1071698944
	DD	0
	DD	1071697920
	DD	0
	DD	1071696896
	DD	0
	DD	1071694848
	DD	0
	DD	1071693824
	DD	0
	DD	1071692800
	DD	0
	DD	1071691776
	DD	0
	DD	1071690752
	DD	0
	DD	1071689728
	DD	0
	DD	1071688704
	DD	0
	DD	1071686656
	DD	0
	DD	1071685632
	DD	0
	DD	1071684608
	DD	0
	DD	1071683584
	DD	0
	DD	1071682560
	DD	0
	DD	1071681536
	DD	0
	DD	1071680512
	DD	0
	DD	1071679488
	DD	0
	DD	1071677440
	DD	0
	DD	1071676416
	DD	0
	DD	1071675392
	DD	0
	DD	1071674368
	DD	0
	DD	1071673344
	DD	0
	DD	1071672320
	DD	0
	DD	1071671296
	DD	0
	DD	1071670272
	DD	0
	DD	1071669248
	DD	0
	DD	1071668224
	DD	0
	DD	1071667200
	DD	0
	DD	1071666176
	DD	0
	DD	1071665152
	DD	0
	DD	1071663104
	DD	0
	DD	1071662080
	DD	0
	DD	1071661056
	DD	0
	DD	1071660032
	DD	0
	DD	1071659008
	DD	0
	DD	1071657984
	DD	0
	DD	1071656960
	DD	0
	DD	1071655936
	DD	0
	DD	1071654912
	DD	0
	DD	1071653888
	DD	0
	DD	1071652864
	DD	0
	DD	1071651840
	DD	0
	DD	1071650816
	DD	0
	DD	1071649792
	DD	0
	DD	1071648768
	DD	0
	DD	1071647744
	DD	0
	DD	1071646720
	DD	0
	DD	1071645696
	DD	0
	DD	0
	DD	0
	DD	1071644672
	DD	0
	DD	1072693248
	DD	0
	DD	1073741824
	DD	33554432
	DD	1101004800
_RDATA	ENDS
_DATA	SEGMENT      'DATA'
_DATA	ENDS
EXTRN	__ImageBase:PROC
EXTRN	_fltused:BYTE
ENDIF
	END
