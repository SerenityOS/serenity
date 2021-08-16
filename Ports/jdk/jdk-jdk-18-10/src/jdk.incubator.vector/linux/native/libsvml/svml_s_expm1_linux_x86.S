/*
 * Copyright (c) 2018, 2021, Intel Corporation.
 * Intel Short Vector Math Library (SVML) Source Code
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */
#include "globals_vectorApiSupport_linux.S.inc"
#ifdef __VECTOR_API_MATH_INTRINSICS_LINUX
# -- Machine type EM64t
	.file "svml_s_expm1.c"
	.text
..TXTST0:
.L_2__routine_start___svml_expm1f4_ha_e9_0:
# -- Begin  __svml_expm1f4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm1f4_ha_e9
# --- __svml_expm1f4_ha_e9(__m128)
__svml_expm1f4_ha_e9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #530.1
        .byte     15                                            #636.540
        .byte     30                                            #636.540
        .byte     250                                           #636.540
	.cfi_startproc
..___tag_value___svml_expm1f4_ha_e9.1:
..L2:
                                                          #530.1
        pushq     %rbp                                          #530.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #530.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #530.1
        subq      $192, %rsp                                    #530.1
        movaps    %xmm0, %xmm11                                 #530.1
        movups    768+__svml_sexpm1_ha_data_internal(%rip), %xmm5 #581.46
        movaps    %xmm11, %xmm10                                #586.14
        mulps     %xmm11, %xmm5                                 #582.12
        lea       __svml_sexpm1_ha_data_internal(%rip), %r8     #602.61
        movups    832+__svml_sexpm1_ha_data_internal(%rip), %xmm12 #584.44
        roundps   $0, %xmm5, %xmm5                              #583.12
        mulps     %xmm5, %xmm12                                 #586.34
        movups    896+__svml_sexpm1_ha_data_internal(%rip), %xmm4 #587.14
        subps     %xmm12, %xmm10                                #586.14
        mulps     %xmm5, %xmm4                                  #587.14
        movups    960+__svml_sexpm1_ha_data_internal(%rip), %xmm14 #597.52
        movaps    %xmm10, %xmm8                                 #589.20
        movups    1024+__svml_sexpm1_ha_data_internal(%rip), %xmm15 #601.22
        movaps    %xmm10, %xmm7                                 #588.12
        cmpeqps   %xmm11, %xmm8                                 #589.20
        addps     %xmm14, %xmm5                                 #598.16
        subps     %xmm4, %xmm7                                  #588.12
        andps     %xmm5, %xmm15                                 #601.22
        movd      %xmm15, %edx                                  #602.175
        movups    1088+__svml_sexpm1_ha_data_internal(%rip), %xmm12 #603.24
        orps      1152+__svml_sexpm1_ha_data_internal(%rip), %xmm8 #614.19
        andps     %xmm12, %xmm5                                 #604.21
        pextrd    $1, %xmm15, %ecx                              #602.380
        andps     %xmm10, %xmm8                                 #615.13
        movslq    %edx, %rdx                                    #602.61
        pslld     $14, %xmm5                                    #605.21
        movslq    %ecx, %rcx                                    #602.266
        subps     %xmm8, %xmm10                                 #616.13
        pextrd    $2, %xmm15, %esi                              #602.588
        subps     %xmm4, %xmm10                                 #617.13
        pextrd    $3, %xmm15, %edi                              #602.796
        movslq    %esi, %rsi                                    #602.474
        movslq    %edi, %rdi                                    #602.682
        movq      (%r8,%rdx), %xmm3                             #602.61
        movq      (%r8,%rcx), %xmm2                             #602.266
        movq      (%r8,%rsi), %xmm0                             #602.474
        movq      (%r8,%rdi), %xmm1                             #602.682
        unpcklps  %xmm2, %xmm3                                  #602.843
        unpcklps  %xmm1, %xmm0                                  #602.878
        movaps    %xmm3, %xmm2                                  #602.918
        movlhps   %xmm0, %xmm2                                  #602.918
        shufps    $238, %xmm0, %xmm3                            #602.962
        orps      %xmm5, %xmm2                                  #607.13
        mulps     %xmm5, %xmm3                                  #608.13
        movaps    %xmm2, %xmm1                                  #618.15
        mulps     %xmm8, %xmm1                                  #618.15
        movups    512+__svml_sexpm1_ha_data_internal(%rip), %xmm5 #611.24
        mulps     %xmm7, %xmm5                                  #611.24
        mulps     %xmm7, %xmm7                                  #612.13
        addps     576+__svml_sexpm1_ha_data_internal(%rip), %xmm5 #611.12
        movups    1280+__svml_sexpm1_ha_data_internal(%rip), %xmm6 #590.48
        movups    1216+__svml_sexpm1_ha_data_internal(%rip), %xmm4 #620.45
        movaps    %xmm6, %xmm13                                 #592.13
        movaps    %xmm6, %xmm0                                  #621.160
        andps     %xmm11, %xmm13                                #592.13
        movaps    %xmm6, %xmm9                                  #593.14
        andps     %xmm2, %xmm0                                  #621.160
        andps     %xmm4, %xmm6                                  #621.205
        andnps    %xmm11, %xmm9                                 #593.14
        cmpnleps  1344+__svml_sexpm1_ha_data_internal(%rip), %xmm13 #594.18
        cmpleps   %xmm6, %xmm0                                  #621.253
        mulps     %xmm7, %xmm5                                  #619.24
        movmskps  %xmm13, %eax                                  #596.44
        movaps    %xmm2, %xmm7                                  #621.297
        movaps    %xmm4, %xmm13                                 #621.349
        blendvps  %xmm0, %xmm4, %xmm7                           #621.297
        addps     %xmm2, %xmm4                                  #622.14
        addps     %xmm5, %xmm10                                 #619.12
        subps     %xmm4, %xmm7                                  #623.14
        addps     %xmm10, %xmm8                                 #630.13
        blendvps  %xmm0, %xmm2, %xmm13                          #621.349
        movaps    %xmm4, %xmm0                                  #626.15
        mulps     %xmm10, %xmm2                                 #629.26
        addps     %xmm1, %xmm0                                  #626.15
        addps     %xmm7, %xmm13                                 #624.14
        subps     %xmm0, %xmm4                                  #627.14
        addps     %xmm3, %xmm13                                 #625.14
        mulps     %xmm8, %xmm3                                  #631.26
        addps     %xmm4, %xmm1                                  #628.17
        addps     %xmm3, %xmm13                                 #631.14
        addps     %xmm2, %xmm1                                  #629.14
        addps     %xmm13, %xmm1                                 #632.14
        addps     %xmm1, %xmm0                                  #633.14
        orps      %xmm9, %xmm0                                  #634.14
        testl     %eax, %eax                                    #636.52
        jne       ..B1.3        # Prob 5%                       #636.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm11
..B1.2:                         # Preds ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #639.12
        popq      %rbp                                          #639.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #639.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm11, 64(%rsp)                              #636.193
        movups    %xmm0, 128(%rsp)                              #636.264
                                # LOE rbx r12 r13 r14 r15 eax
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #636.448
        movq      %r12, 8(%rsp)                                 #636.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #636.448
        movq      %r13, (%rsp)                                  #636.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #636.448
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #636.511
        jc        ..B1.10       # Prob 5%                       #636.511
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #636.464
        cmpl      $4, %r12d                                     #636.459
        jl        ..B1.7        # Prob 82%                      #636.459
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #636.667
        jmp       ..B1.2        # Prob 100%                     #636.667
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #636.540
        lea       128(%rsp,%r12,4), %rsi                        #636.540
..___tag_value___svml_expm1f4_ha_e9.19:
#       __svml_sexpm1_ha_cout_rare_internal(const float *, float *)
        call      __svml_sexpm1_ha_cout_rare_internal           #636.540
..___tag_value___svml_expm1f4_ha_e9.20:
        jmp       ..B1.8        # Prob 100%                     #636.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expm1f4_ha_e9,@function
	.size	__svml_expm1f4_ha_e9,.-__svml_expm1f4_ha_e9
..LN__svml_expm1f4_ha_e9.0:
	.data
# -- End  __svml_expm1f4_ha_e9
	.text
.L_2__routine_start___svml_expm1f4_ha_ex_1:
# -- Begin  __svml_expm1f4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm1f4_ha_ex
# --- __svml_expm1f4_ha_ex(__m128)
__svml_expm1f4_ha_ex:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #644.1
        .byte     15                                            #750.540
        .byte     30                                            #750.540
        .byte     250                                           #750.540
	.cfi_startproc
..___tag_value___svml_expm1f4_ha_ex.22:
..L23:
                                                         #644.1
        pushq     %rbp                                          #644.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #644.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #644.1
        subq      $192, %rsp                                    #644.1
        movaps    %xmm0, %xmm10                                 #644.1
        movups    768+__svml_sexpm1_ha_data_internal(%rip), %xmm14 #696.12
        movaps    %xmm10, %xmm9                                 #700.14
        mulps     %xmm10, %xmm14                                #696.12
        lea       __svml_sexpm1_ha_data_internal(%rip), %r8     #716.61
        movups    .L_2il0floatpacket.29(%rip), %xmm11           #697.105
        movups    960+__svml_sexpm1_ha_data_internal(%rip), %xmm0 #712.16
        addps     %xmm11, %xmm14                                #697.162
        movups    832+__svml_sexpm1_ha_data_internal(%rip), %xmm12 #698.44
        subps     %xmm11, %xmm14                                #697.207
        movdqu    1024+__svml_sexpm1_ha_data_internal(%rip), %xmm1 #715.16
        addps     %xmm14, %xmm0                                 #712.16
        mulps     %xmm14, %xmm12                                #700.34
        pand      %xmm0, %xmm1                                  #715.16
        subps     %xmm12, %xmm9                                 #700.14
        movd      %xmm1, %edx                                   #716.175
        movups    1280+__svml_sexpm1_ha_data_internal(%rip), %xmm6 #704.48
        movaps    %xmm9, %xmm7                                  #702.12
        pshufd    $1, %xmm1, %xmm15                             #716.396
        movaps    %xmm6, %xmm13                                 #706.13
        movd      %xmm15, %ecx                                  #716.378
        andps     %xmm10, %xmm13                                #706.13
        pshufd    $2, %xmm1, %xmm2                              #716.639
        movaps    %xmm9, %xmm5                                  #703.20
        pshufd    $3, %xmm1, %xmm11                             #716.882
        movaps    %xmm6, %xmm8                                  #707.14
        movd      %xmm2, %esi                                   #716.621
        andnps    %xmm10, %xmm8                                 #707.14
        movd      %xmm11, %edi                                  #716.864
        movups    896+__svml_sexpm1_ha_data_internal(%rip), %xmm4 #701.14
        movdqu    1088+__svml_sexpm1_ha_data_internal(%rip), %xmm15 #718.15
        cmpnleps  1344+__svml_sexpm1_ha_data_internal(%rip), %xmm13 #708.18
        cmpeqps   %xmm10, %xmm5                                 #703.20
        mulps     %xmm14, %xmm4                                 #701.14
        movmskps  %xmm13, %eax                                  #710.44
        pand      %xmm15, %xmm0                                 #718.15
        subps     %xmm4, %xmm7                                  #702.12
        movslq    %edx, %rdx                                    #716.61
        pslld     $14, %xmm0                                    #719.15
        movslq    %ecx, %rcx                                    #716.264
        movslq    %esi, %rsi                                    #716.507
        movslq    %edi, %rdi                                    #716.750
        movq      (%r8,%rdx), %xmm3                             #716.61
        movq      (%r8,%rcx), %xmm12                            #716.264
        movq      (%r8,%rsi), %xmm14                            #716.507
        movq      (%r8,%rdi), %xmm13                            #716.750
        unpcklps  %xmm12, %xmm3                                 #716.946
        unpcklps  %xmm13, %xmm14                                #716.981
        movaps    %xmm3, %xmm2                                  #716.1021
        movlhps   %xmm14, %xmm2                                 #716.1021
        shufps    $238, %xmm14, %xmm3                           #716.1065
        orps      %xmm0, %xmm2                                  #721.13
        mulps     %xmm0, %xmm3                                  #722.13
        movaps    %xmm2, %xmm1                                  #732.15
        movups    512+__svml_sexpm1_ha_data_internal(%rip), %xmm0 #725.24
        mulps     %xmm7, %xmm0                                  #725.24
        mulps     %xmm7, %xmm7                                  #726.13
        addps     576+__svml_sexpm1_ha_data_internal(%rip), %xmm0 #725.12
        orps      1152+__svml_sexpm1_ha_data_internal(%rip), %xmm5 #728.19
        andps     %xmm9, %xmm5                                  #729.13
        movups    1216+__svml_sexpm1_ha_data_internal(%rip), %xmm11 #734.45
        subps     %xmm5, %xmm9                                  #730.13
        mulps     %xmm7, %xmm0                                  #733.24
        mulps     %xmm5, %xmm1                                  #732.15
        subps     %xmm4, %xmm9                                  #731.13
        movaps    %xmm6, %xmm7                                  #735.160
        andps     %xmm11, %xmm6                                 #735.205
        andps     %xmm2, %xmm7                                  #735.160
        addps     %xmm0, %xmm9                                  #733.12
        cmpleps   %xmm6, %xmm7                                  #735.253
        addps     %xmm9, %xmm5                                  #744.13
        movaps    %xmm7, %xmm0                                  #735.308
        movaps    %xmm7, %xmm4                                  #735.343
        andnps    %xmm2, %xmm0                                  #735.308
        andps     %xmm11, %xmm4                                 #735.343
        orps      %xmm4, %xmm0                                  #735.297
        movaps    %xmm7, %xmm4                                  #735.397
        andnps    %xmm11, %xmm4                                 #735.397
        andps     %xmm2, %xmm7                                  #735.434
        orps      %xmm7, %xmm4                                  #735.386
        addps     %xmm2, %xmm11                                 #736.14
        mulps     %xmm9, %xmm2                                  #743.26
        subps     %xmm11, %xmm0                                 #737.14
        addps     %xmm0, %xmm4                                  #738.14
        movaps    %xmm11, %xmm0                                 #740.15
        addps     %xmm1, %xmm0                                  #740.15
        addps     %xmm3, %xmm4                                  #739.14
        mulps     %xmm5, %xmm3                                  #745.26
        subps     %xmm0, %xmm11                                 #741.14
        addps     %xmm3, %xmm4                                  #745.14
        addps     %xmm11, %xmm1                                 #742.17
        addps     %xmm2, %xmm1                                  #743.14
        addps     %xmm4, %xmm1                                  #746.14
        addps     %xmm1, %xmm0                                  #747.14
        orps      %xmm8, %xmm0                                  #748.14
        testl     %eax, %eax                                    #750.52
        jne       ..B2.3        # Prob 5%                       #750.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm10
..B2.2:                         # Preds ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #753.12
        popq      %rbp                                          #753.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #753.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm10, 64(%rsp)                              #750.193
        movups    %xmm0, 128(%rsp)                              #750.264
                                # LOE rbx r12 r13 r14 r15 eax
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #750.448
        movq      %r12, 8(%rsp)                                 #750.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #750.448
        movq      %r13, (%rsp)                                  #750.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #750.448
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #750.511
        jc        ..B2.10       # Prob 5%                       #750.511
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #750.464
        cmpl      $4, %r12d                                     #750.459
        jl        ..B2.7        # Prob 82%                      #750.459
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #750.667
        jmp       ..B2.2        # Prob 100%                     #750.667
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #750.540
        lea       128(%rsp,%r12,4), %rsi                        #750.540
..___tag_value___svml_expm1f4_ha_ex.40:
#       __svml_sexpm1_ha_cout_rare_internal(const float *, float *)
        call      __svml_sexpm1_ha_cout_rare_internal           #750.540
..___tag_value___svml_expm1f4_ha_ex.41:
        jmp       ..B2.8        # Prob 100%                     #750.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expm1f4_ha_ex,@function
	.size	__svml_expm1f4_ha_ex,.-__svml_expm1f4_ha_ex
..LN__svml_expm1f4_ha_ex.1:
	.data
# -- End  __svml_expm1f4_ha_ex
	.text
.L_2__routine_start___svml_expm1f8_ha_l9_2:
# -- Begin  __svml_expm1f8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm1f8_ha_l9
# --- __svml_expm1f8_ha_l9(__m256)
__svml_expm1f8_ha_l9:
# parameter 1: %ymm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #758.1
        .byte     15                                            #864.546
        .byte     30                                            #864.546
        .byte     250                                           #864.546
	.cfi_startproc
..___tag_value___svml_expm1f8_ha_l9.43:
..L44:
                                                         #758.1
        pushq     %rbp                                          #758.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #758.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #758.1
        subq      $192, %rsp                                    #758.1
        lea       __svml_sexpm1_ha_data_internal(%rip), %rax    #830.64
        vmovaps   %ymm0, %ymm13                                 #758.1
        vmulps    768+__svml_sexpm1_ha_data_internal(%rip), %ymm13, %ymm12 #810.12
        vmovups   1280+__svml_sexpm1_ha_data_internal(%rip), %ymm11 #818.51
        vmovups   832+__svml_sexpm1_ha_data_internal(%rip), %ymm10 #814.14
        vroundps  $0, %ymm12, %ymm5                             #811.12
        vaddps    960+__svml_sexpm1_ha_data_internal(%rip), %ymm5, %ymm2 #826.16
        vfnmadd213ps %ymm13, %ymm5, %ymm10                      #814.14
        vmulps    896+__svml_sexpm1_ha_data_internal(%rip), %ymm5, %ymm9 #815.14
        vcmpeqps  %ymm13, %ymm10, %ymm7                         #817.20
        vsubps    %ymm9, %ymm10, %ymm8                          #816.12
        vandps    1024+__svml_sexpm1_ha_data_internal(%rip), %ymm2, %ymm6 #829.18
        vandps    %ymm11, %ymm13, %ymm1                         #820.13
        vcmpnle_uqps 1344+__svml_sexpm1_ha_data_internal(%rip), %ymm1, %ymm3 #822.18
        vandps    1088+__svml_sexpm1_ha_data_internal(%rip), %ymm2, %ymm2 #832.17
        vmovmskps %ymm3, %edx                                   #824.45
        vpslld    $14, %ymm2, %ymm2                             #833.17
        vorps     1152+__svml_sexpm1_ha_data_internal(%rip), %ymm7, %ymm7 #842.19
        vandnps   %ymm13, %ymm11, %ymm12                        #821.14
        vmovd     %xmm6, %ecx                                   #830.178
        vpextrd   $1, %xmm6, %esi                               #830.414
        vpextrd   $2, %xmm6, %edi                               #830.653
        vpextrd   $3, %xmm6, %r8d                               #830.892
        vextractf128 $1, %ymm6, %xmm6                           #830.1150
        movslq    %ecx, %rcx                                    #830.64
        vmovd     %xmm6, %r9d                                   #830.1131
        vmovq     (%rax,%rcx), %xmm15                           #830.64
        vpextrd   $1, %xmm6, %r10d                              #830.1372
        vpextrd   $2, %xmm6, %r11d                              #830.1616
        vpextrd   $3, %xmm6, %ecx                               #830.1860
        movslq    %esi, %rsi                                    #830.300
        movslq    %edi, %rdi                                    #830.539
        movslq    %r8d, %r8                                     #830.778
        movslq    %r9d, %r9                                     #830.1017
        movslq    %r10d, %r10                                   #830.1258
        movslq    %r11d, %r11                                   #830.1502
        movslq    %ecx, %rcx                                    #830.1746
        vmovq     (%rax,%rsi), %xmm0                            #830.300
        vmovq     (%rax,%rdi), %xmm14                           #830.539
        vmovq     (%rax,%r8), %xmm1                             #830.778
        vmovq     (%rax,%r9), %xmm3                             #830.1017
        vmovq     (%rax,%r10), %xmm4                            #830.1258
        vmovq     (%rax,%r11), %xmm5                            #830.1502
        vmovq     (%rax,%rcx), %xmm6                            #830.1746
        vunpcklps %xmm14, %xmm15, %xmm14                        #830.1954
        vunpcklps %xmm1, %xmm0, %xmm15                          #830.2009
        vunpcklps %xmm5, %xmm3, %xmm0                           #830.2064
        vunpcklps %xmm6, %xmm4, %xmm4                           #830.2119
        vinsertf128 $1, %xmm0, %ymm14, %ymm1                    #830.2182
        vinsertf128 $1, %xmm4, %ymm15, %ymm3                    #830.2290
        vunpcklps %ymm3, %ymm1, %ymm5                           #830.2381
        vunpckhps %ymm3, %ymm1, %ymm4                           #830.2455
        vorps     %ymm2, %ymm5, %ymm6                           #835.13
        vmulps    %ymm2, %ymm4, %ymm4                           #836.13
        vmovups   512+__svml_sexpm1_ha_data_internal(%rip), %ymm2 #839.12
        vmovups   1216+__svml_sexpm1_ha_data_internal(%rip), %ymm1 #848.48
        vfmadd213ps 576+__svml_sexpm1_ha_data_internal(%rip), %ymm8, %ymm2 #839.12
        vmulps    %ymm8, %ymm8, %ymm8                           #840.13
        vandps    %ymm7, %ymm10, %ymm15                         #843.13
        vsubps    %ymm15, %ymm10, %ymm10                        #844.13
        vmulps    %ymm15, %ymm6, %ymm14                         #846.15
        vsubps    %ymm9, %ymm10, %ymm9                          #845.13
        vandps    %ymm11, %ymm6, %ymm0                          #849.160
        vandps    %ymm11, %ymm1, %ymm11                         #849.208
        vfmadd213ps %ymm9, %ymm8, %ymm2                         #847.12
        vcmple_oqps %ymm11, %ymm0, %ymm0                        #849.259
        vaddps    %ymm1, %ymm6, %ymm9                           #850.14
        vblendvps %ymm0, %ymm1, %ymm6, %ymm3                    #849.316
        vblendvps %ymm0, %ymm6, %ymm1, %ymm5                    #849.371
        vaddps    %ymm9, %ymm14, %ymm0                          #854.15
        vsubps    %ymm9, %ymm3, %ymm7                           #851.14
        vsubps    %ymm0, %ymm9, %ymm10                          #855.14
        vaddps    %ymm7, %ymm5, %ymm8                           #852.14
        vaddps    %ymm10, %ymm14, %ymm14                        #856.17
        vaddps    %ymm8, %ymm4, %ymm11                          #853.14
        vfmadd213ps %ymm14, %ymm2, %ymm6                        #857.14
        vaddps    %ymm2, %ymm15, %ymm2                          #858.13
        vfmadd213ps %ymm11, %ymm2, %ymm4                        #859.14
        vaddps    %ymm4, %ymm6, %ymm4                           #860.14
        vaddps    %ymm4, %ymm0, %ymm6                           #861.14
        vorps     %ymm12, %ymm6, %ymm0                          #862.14
        testl     %edx, %edx                                    #864.52
        jne       ..B3.3        # Prob 5%                       #864.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm13
..B3.2:                         # Preds ..B3.3 ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #867.12
        popq      %rbp                                          #867.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #867.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm13, 64(%rsp)                              #864.196
        vmovups   %ymm0, 128(%rsp)                              #864.270
        je        ..B3.2        # Prob 95%                      #864.374
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #864.454
                                # LOE rbx r12 r13 r14 r15 eax edx
..B3.13:                        # Preds ..B3.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B3.7:                         # Preds ..B3.8 ..B3.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #864.517
        jc        ..B3.10       # Prob 5%                       #864.517
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #864.470
        cmpl      $8, %r12d                                     #864.465
        jl        ..B3.7        # Prob 82%                      #864.465
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #864.676
        jmp       ..B3.2        # Prob 100%                     #864.676
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #864.546
        lea       128(%rsp,%r12,4), %rsi                        #864.546
..___tag_value___svml_expm1f8_ha_l9.61:
#       __svml_sexpm1_ha_cout_rare_internal(const float *, float *)
        call      __svml_sexpm1_ha_cout_rare_internal           #864.546
..___tag_value___svml_expm1f8_ha_l9.62:
        jmp       ..B3.8        # Prob 100%                     #864.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expm1f8_ha_l9,@function
	.size	__svml_expm1f8_ha_l9,.-__svml_expm1f8_ha_l9
..LN__svml_expm1f8_ha_l9.2:
	.data
# -- End  __svml_expm1f8_ha_l9
	.text
.L_2__routine_start___svml_expm1f16_ha_z0_3:
# -- Begin  __svml_expm1f16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm1f16_ha_z0
# --- __svml_expm1f16_ha_z0(__m512)
__svml_expm1f16_ha_z0:
# parameter 1: %zmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #872.1
        .byte     15                                            #969.760
        .byte     30                                            #969.760
        .byte     250                                           #969.760
	.cfi_startproc
..___tag_value___svml_expm1f16_ha_z0.64:
..L65:
                                                         #872.1
        pushq     %rbp                                          #872.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #872.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #872.1
        subq      $192, %rsp                                    #872.1
        vmovups   128+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm9 #920.53
        vmovups   192+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm8 #921.57
        vmovups   384+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm10 #929.53
        vmovups   448+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm11 #930.53
        vmovups   512+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm13 #938.56
        vmovups   256+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm7 #923.59
        vmovaps   %zmm0, %zmm6                                  #872.1
        vfmadd213ps {rn-sae}, %zmm8, %zmm6, %zmm9               #922.24
        vmovups   576+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm0 #941.55
        vcmpps    $21, {sae}, %zmm7, %zmm6, %k0                 #924.27
        vmovups   640+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm7 #944.61
        vsubps    {rn-sae}, %zmm8, %zmm9, %zmm12                #928.22
        vpermps   __svml_sexpm1_ha_data_internal_avx512(%rip), %zmm9, %zmm14 #934.154
        vpermps   64+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm9, %zmm4 #935.155
        vandps    320+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm6, %zmm5 #927.21
        vfnmadd213ps {rn-sae}, %zmm6, %zmm12, %zmm10            #931.21
        vmovups   768+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm8 #946.61
        vmaxps    {sae}, %zmm13, %zmm12, %zmm15                 #939.22
        kmovw     %k0, %esi                                     #925.48
        vscalefps {rn-sae}, %zmm15, %zmm14, %zmm3               #940.20
        vmovaps   %zmm11, %zmm2                                 #932.19
        vfnmadd213ps {rn-sae}, %zmm10, %zmm12, %zmm2            #932.19
        vsubps    {rn-sae}, %zmm2, %zmm10, %zmm1                #936.23
        vandps    %zmm0, %zmm2, %zmm2                           #942.19
        vfnmadd231ps {rn-sae}, %zmm12, %zmm11, %zmm1            #937.20
        vmovups   832+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm11 #952.53
        vmovups   896+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm12 #953.54
        vandps    %zmm0, %zmm1, %zmm9                           #943.20
        vmovups   704+__svml_sexpm1_ha_data_internal_avx512(%rip), %zmm1 #947.21
        vmulps    {rn-sae}, %zmm2, %zmm2, %zmm0                 #948.22
        vcmpps    $17, {sae}, %zmm11, %zmm3, %k1                #954.25
        vfmadd231ps {rn-sae}, %zmm2, %zmm7, %zmm1               #947.21
        vaddps    {rn-sae}, %zmm4, %zmm9, %zmm10                #950.22
        vaddps    {rn-sae}, %zmm12, %zmm3, %zmm7                #957.23
        vfmadd213ps {rn-sae}, %zmm8, %zmm2, %zmm1               #949.22
        vfmadd213ps {rn-sae}, %zmm10, %zmm2, %zmm4              #951.20
        vblendmps %zmm12, %zmm3, %zmm13{%k1}                    #955.19
        vblendmps %zmm3, %zmm12, %zmm15{%k1}                    #956.19
        vfmadd213ps {rn-sae}, %zmm4, %zmm0, %zmm1               #960.22
        vsubps    {rn-sae}, %zmm7, %zmm13, %zmm14               #958.23
        vmovaps   %zmm7, %zmm0                                  #961.22
        vfmadd231ps {rn-sae}, %zmm3, %zmm2, %zmm0               #961.22
        vaddps    {rn-sae}, %zmm15, %zmm14, %zmm8               #959.23
        vsubps    {rn-sae}, %zmm7, %zmm0, %zmm4                 #962.24
        vfmsub231ps {rn-sae}, %zmm3, %zmm2, %zmm4               #963.22
        vfmadd213ps {rn-sae}, %zmm8, %zmm1, %zmm3               #964.21
        vaddps    {rn-sae}, %zmm4, %zmm3, %zmm3                 #965.23
        vaddps    {rn-sae}, %zmm3, %zmm0, %zmm0                 #966.24
        vorps     %zmm5, %zmm0, %zmm0                           #967.21
        testl     %esi, %esi                                    #969.52
        jne       ..B4.3        # Prob 5%                       #969.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm6
..B4.2:                         # Preds ..B4.9 ..B4.7 ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #972.12
        popq      %rbp                                          #972.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #972.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #969.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm6
..B4.4:                         # Preds ..B4.3
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #969.249
        movl      %edx, %eax                                    #969.303
        orl       $8064, %eax                                   #969.303
        cmpl      %eax, %edx                                    #969.332
        je        ..B4.6        # Prob 78%                      #969.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm6
..B4.5:                         # Preds ..B4.4
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #969.344
        vldmxcsr  32(%rsp)                                      #969.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm6
..B4.6:                         # Preds ..B4.5 ..B4.4
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm6, 64(%rsp)                               #969.404
        vmovups   %zmm0, 128(%rsp)                              #969.478
        testl     %esi, %esi                                    #969.586
        jne       ..B4.11       # Prob 5%                       #969.586
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B4.7:                         # Preds ..B4.14 ..B4.6
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #969.933
        je        ..B4.2        # Prob 78%                      #969.933
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B4.8:                         # Preds ..B4.7
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #969.958
        movl      32(%rsp), %eax                                #969.958
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B4.9:                         # Preds ..B4.8
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #969.945
        orl       %edx, %eax                                    #969.945
        movl      %eax, 32(%rsp)                                #969.945
        vldmxcsr  32(%rsp)                                      #969.945
        jmp       ..B4.2        # Prob 100%                     #969.945
                                # LOE rbx r12 r13 r14 r15 zmm0
..B4.11:                        # Preds ..B4.6
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #969.668
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B4.18:                        # Preds ..B4.11
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 16(%rsp)                                #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
        movq      %r14, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %esi, %r14d                                   #
        movq      %rbx, 24(%rsp)                                #[spill]
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
        movl      %ecx, %ebx                                    #
                                # LOE rbx r15 r12d r13d r14d
..B4.12:                        # Preds ..B4.13 ..B4.18
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #969.731
        jc        ..B4.15       # Prob 5%                       #969.731
                                # LOE rbx r15 r12d r13d r14d
..B4.13:                        # Preds ..B4.15 ..B4.12
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #969.684
        cmpl      $16, %ebx                                     #969.679
        jl        ..B4.12       # Prob 82%                      #969.679
                                # LOE rbx r15 r12d r13d r14d
..B4.14:                        # Preds ..B4.13
                                # Execution count [2.25e-03]: Infreq
        movq      (%rsp), %r14                                  #[spill]
	.cfi_restore 14
        movl      %r12d, %eax                                   #
        movq      16(%rsp), %r12                                #[spill]
	.cfi_restore 12
        movl      %r13d, %edx                                   #
        movq      8(%rsp), %r13                                 #[spill]
	.cfi_restore 13
        movq      24(%rsp), %rbx                                #[spill]
	.cfi_restore 3
        vmovups   128(%rsp), %zmm0                              #969.888
        jmp       ..B4.7        # Prob 100%                     #969.888
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B4.15:                        # Preds ..B4.12
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,4), %rdi                         #969.760
        lea       128(%rsp,%rbx,4), %rsi                        #969.760
..___tag_value___svml_expm1f16_ha_z0.92:
#       __svml_sexpm1_ha_cout_rare_internal(const float *, float *)
        call      __svml_sexpm1_ha_cout_rare_internal           #969.760
..___tag_value___svml_expm1f16_ha_z0.93:
        jmp       ..B4.13       # Prob 100%                     #969.760
        .align    16,0x90
                                # LOE r15 ebx r12d r13d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_expm1f16_ha_z0,@function
	.size	__svml_expm1f16_ha_z0,.-__svml_expm1f16_ha_z0
..LN__svml_expm1f16_ha_z0.3:
	.data
# -- End  __svml_expm1f16_ha_z0
	.text
.L_2__routine_start___svml_expm1f4_ha_l9_4:
# -- Begin  __svml_expm1f4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm1f4_ha_l9
# --- __svml_expm1f4_ha_l9(__m128)
__svml_expm1f4_ha_l9:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #977.1
        .byte     15                                            #1083.540
        .byte     30                                            #1083.540
        .byte     250                                           #1083.540
	.cfi_startproc
..___tag_value___svml_expm1f4_ha_l9.95:
..L96:
                                                         #977.1
        pushq     %rbp                                          #977.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #977.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #977.1
        subq      $192, %rsp                                    #977.1
        vmovaps   %xmm0, %xmm13                                 #977.1
        vmulps    768+__svml_sexpm1_ha_data_internal(%rip), %xmm13, %xmm14 #1029.12
        lea       __svml_sexpm1_ha_data_internal(%rip), %r8     #1049.61
        vroundps  $0, %xmm14, %xmm14                            #1030.12
        vmovups   832+__svml_sexpm1_ha_data_internal(%rip), %xmm10 #1033.14
        vfnmadd213ps %xmm13, %xmm14, %xmm10                     #1033.14
        vmovups   1280+__svml_sexpm1_ha_data_internal(%rip), %xmm11 #1037.48
        vandps    %xmm11, %xmm13, %xmm15                        #1039.13
        vandnps   %xmm13, %xmm11, %xmm12                        #1040.14
        vcmpnleps 1344+__svml_sexpm1_ha_data_internal(%rip), %xmm15, %xmm5 #1041.18
        vmulps    896+__svml_sexpm1_ha_data_internal(%rip), %xmm14, %xmm9 #1034.14
        vaddps    960+__svml_sexpm1_ha_data_internal(%rip), %xmm14, %xmm6 #1045.16
        vcmpeqps  %xmm13, %xmm10, %xmm7                         #1036.20
        vsubps    %xmm9, %xmm10, %xmm8                          #1035.12
        vmovmskps %xmm5, %eax                                   #1043.44
        vandps    1024+__svml_sexpm1_ha_data_internal(%rip), %xmm6, %xmm3 #1048.18
        vmovd     %xmm3, %edx                                   #1049.175
        vandps    1088+__svml_sexpm1_ha_data_internal(%rip), %xmm6, %xmm6 #1051.17
        vorps     1152+__svml_sexpm1_ha_data_internal(%rip), %xmm7, %xmm7 #1061.19
        vpextrd   $1, %xmm3, %ecx                               #1049.380
        vpextrd   $2, %xmm3, %esi                               #1049.588
        vpextrd   $3, %xmm3, %edi                               #1049.796
        movslq    %edx, %rdx                                    #1049.61
        movslq    %ecx, %rcx                                    #1049.266
        movslq    %esi, %rsi                                    #1049.474
        movslq    %edi, %rdi                                    #1049.682
        vmovq     (%r8,%rdx), %xmm4                             #1049.61
        vmovq     (%r8,%rcx), %xmm2                             #1049.266
        vmovq     (%r8,%rsi), %xmm1                             #1049.474
        vmovq     (%r8,%rdi), %xmm0                             #1049.682
        vunpcklps %xmm2, %xmm4, %xmm15                          #1049.843
        vpslld    $14, %xmm6, %xmm2                             #1052.17
        vunpcklps %xmm0, %xmm1, %xmm5                           #1049.878
        vmovlhps  %xmm5, %xmm15, %xmm3                          #1049.918
        vshufps   $238, %xmm5, %xmm15, %xmm4                    #1049.962
        vorps     %xmm2, %xmm3, %xmm5                           #1054.13
        vmulps    %xmm2, %xmm4, %xmm3                           #1055.13
        vandps    %xmm7, %xmm10, %xmm2                          #1062.13
        vsubps    %xmm2, %xmm10, %xmm10                         #1063.13
        vmulps    %xmm2, %xmm5, %xmm1                           #1065.15
        vsubps    %xmm9, %xmm10, %xmm9                          #1064.13
        vmovups   1216+__svml_sexpm1_ha_data_internal(%rip), %xmm6 #1067.45
        vandps    %xmm11, %xmm5, %xmm0                          #1068.160
        vandps    %xmm11, %xmm6, %xmm11                         #1068.205
        vmovaps   %xmm5, %xmm7                                  #1068.297
        vmovups   512+__svml_sexpm1_ha_data_internal(%rip), %xmm4 #1058.12
        vcmpleps  %xmm11, %xmm0, %xmm0                          #1068.253
        vfmadd213ps 576+__svml_sexpm1_ha_data_internal(%rip), %xmm8, %xmm4 #1058.12
        vaddps    %xmm6, %xmm5, %xmm11                          #1069.14
        vmulps    %xmm8, %xmm8, %xmm8                           #1059.13
        vfmadd213ps %xmm9, %xmm8, %xmm4                         #1066.12
        vmovaps   %xmm6, %xmm8                                  #1068.349
        blendvps  %xmm0, %xmm6, %xmm7                           #1068.297
        vaddps    %xmm4, %xmm2, %xmm2                           #1077.13
        vsubps    %xmm11, %xmm7, %xmm9                          #1070.14
        blendvps  %xmm0, %xmm5, %xmm8                           #1068.349
        vaddps    %xmm11, %xmm1, %xmm0                          #1073.15
        vaddps    %xmm9, %xmm8, %xmm10                          #1071.14
        vsubps    %xmm0, %xmm11, %xmm14                         #1074.14
        vaddps    %xmm10, %xmm3, %xmm15                         #1072.14
        vaddps    %xmm14, %xmm1, %xmm1                          #1075.17
        vfmadd213ps %xmm15, %xmm2, %xmm3                        #1078.14
        vfmadd213ps %xmm1, %xmm4, %xmm5                         #1076.14
        vaddps    %xmm3, %xmm5, %xmm3                           #1079.14
        vaddps    %xmm3, %xmm0, %xmm4                           #1080.14
        vorps     %xmm12, %xmm4, %xmm0                          #1081.14
        testl     %eax, %eax                                    #1083.52
        jne       ..B5.3        # Prob 5%                       #1083.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm13
..B5.2:                         # Preds ..B5.3 ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1086.12
        popq      %rbp                                          #1086.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1086.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm13, 64(%rsp)                              #1083.193
        vmovups   %xmm0, 128(%rsp)                              #1083.264
        je        ..B5.2        # Prob 95%                      #1083.368
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1083.448
        movq      %r12, 8(%rsp)                                 #1083.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #1083.448
        movq      %r13, (%rsp)                                  #1083.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #1083.448
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1083.511
        jc        ..B5.10       # Prob 5%                       #1083.511
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1083.464
        cmpl      $4, %r12d                                     #1083.459
        jl        ..B5.7        # Prob 82%                      #1083.459
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1083.667
        jmp       ..B5.2        # Prob 100%                     #1083.667
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1083.540
        lea       128(%rsp,%r12,4), %rsi                        #1083.540
..___tag_value___svml_expm1f4_ha_l9.113:
#       __svml_sexpm1_ha_cout_rare_internal(const float *, float *)
        call      __svml_sexpm1_ha_cout_rare_internal           #1083.540
..___tag_value___svml_expm1f4_ha_l9.114:
        jmp       ..B5.8        # Prob 100%                     #1083.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expm1f4_ha_l9,@function
	.size	__svml_expm1f4_ha_l9,.-__svml_expm1f4_ha_l9
..LN__svml_expm1f4_ha_l9.4:
	.data
# -- End  __svml_expm1f4_ha_l9
	.text
.L_2__routine_start___svml_expm1f8_ha_e9_5:
# -- Begin  __svml_expm1f8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm1f8_ha_e9
# --- __svml_expm1f8_ha_e9(__m256)
__svml_expm1f8_ha_e9:
# parameter 1: %ymm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1091.1
        .byte     15                                            #1197.546
        .byte     30                                            #1197.546
        .byte     250                                           #1197.546
	.cfi_startproc
..___tag_value___svml_expm1f8_ha_e9.116:
..L117:
                                                        #1091.1
        pushq     %rbp                                          #1091.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1091.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1091.1
        subq      $192, %rsp                                    #1091.1
        lea       __svml_sexpm1_ha_data_internal(%rip), %rax    #1163.42
        vmovups   1280+__svml_sexpm1_ha_data_internal(%rip), %ymm14 #1151.51
        vpxor     %xmm9, %xmm9, %xmm9                           #1157.62
        vmulps    768+__svml_sexpm1_ha_data_internal(%rip), %ymm0, %ymm15 #1143.12
        vmovups   1024+__svml_sexpm1_ha_data_internal(%rip), %xmm7 #1161.26
        vmovups   %ymm0, (%rsp)                                 #1091.1[spill]
        vroundps  $0, %ymm15, %ymm6                             #1144.12
        vandps    %ymm14, %ymm0, %ymm1                          #1153.13
        vcmpnle_uqps 1344+__svml_sexpm1_ha_data_internal(%rip), %ymm1, %ymm3 #1155.18
        vmulps    832+__svml_sexpm1_ha_data_internal(%rip), %ymm6, %ymm11 #1147.37
        vmulps    896+__svml_sexpm1_ha_data_internal(%rip), %ymm6, %ymm13 #1148.14
        vsubps    %ymm11, %ymm0, %ymm12                         #1147.14
        vcmpeqps  %ymm0, %ymm12, %ymm10                         #1150.20
        vsubps    %ymm13, %ymm12, %ymm11                        #1149.12
        vandnps   %ymm0, %ymm14, %ymm15                         #1154.14
        vorps     1152+__svml_sexpm1_ha_data_internal(%rip), %ymm10, %ymm10 #1175.19
        vandps    %ymm10, %ymm12, %ymm10                        #1176.13
        vsubps    %ymm10, %ymm12, %ymm12                        #1177.13
        vsubps    %ymm13, %ymm12, %ymm13                        #1178.13
        vextractf128 $1, %ymm3, %xmm4                           #1156.117
        vpackssdw %xmm4, %xmm3, %xmm5                           #1157.79
        vpacksswb %xmm9, %xmm5, %xmm8                           #1157.62
        vpmovmskb %xmm8, %edx                                   #1157.43
        vaddps    960+__svml_sexpm1_ha_data_internal(%rip), %ymm6, %ymm8 #1159.16
        vandps    %xmm7, %xmm8, %xmm3                           #1162.22
        vmovd     %xmm3, %ecx                                   #1163.156
        vextractf128 $1, %ymm8, %xmm9                           #1160.115
        movslq    %ecx, %rcx                                    #1163.42
        vandps    %xmm7, %xmm9, %xmm6                           #1162.93
        vmovd     %xmm6, %r9d                                   #1163.933
        vpextrd   $1, %xmm3, %esi                               #1163.348
        vpextrd   $2, %xmm3, %edi                               #1163.543
        vpextrd   $3, %xmm3, %r8d                               #1163.738
        vmovq     (%rax,%rcx), %xmm0                            #1163.42
        vpextrd   $1, %xmm6, %r10d                              #1163.1125
        vpextrd   $2, %xmm6, %r11d                              #1163.1320
        vpextrd   $3, %xmm6, %ecx                               #1163.1515
        movslq    %esi, %rsi                                    #1163.234
        movslq    %edi, %rdi                                    #1163.429
        movslq    %r8d, %r8                                     #1163.624
        movslq    %r9d, %r9                                     #1163.819
        movslq    %r10d, %r10                                   #1163.1011
        movslq    %r11d, %r11                                   #1163.1206
        movslq    %ecx, %rcx                                    #1163.1401
        vmovq     (%rax,%rsi), %xmm2                            #1163.234
        vmovq     (%rax,%rdi), %xmm1                            #1163.429
        vmovq     (%rax,%r8), %xmm3                             #1163.624
        vmovq     (%rax,%r9), %xmm4                             #1163.819
        vmovq     (%rax,%r10), %xmm7                            #1163.1011
        vmovq     (%rax,%r11), %xmm5                            #1163.1206
        vmovq     (%rax,%rcx), %xmm6                            #1163.1401
        vunpcklps %xmm1, %xmm0, %xmm0                           #1163.1582
        vunpcklps %xmm3, %xmm2, %xmm2                           #1163.1637
        vunpcklps %xmm5, %xmm4, %xmm1                           #1163.1692
        vunpcklps %xmm6, %xmm7, %xmm4                           #1163.1747
        vinsertf128 $1, %xmm1, %ymm0, %ymm5                     #1163.1810
        vinsertf128 $1, %xmm4, %ymm2, %ymm7                     #1163.1918
        vmovups   1088+__svml_sexpm1_ha_data_internal(%rip), %xmm1 #1164.24
        vmulps    512+__svml_sexpm1_ha_data_internal(%rip), %ymm11, %ymm4 #1172.27
        vandps    %xmm1, %xmm8, %xmm8                           #1165.21
        vandps    %xmm1, %xmm9, %xmm9                           #1165.89
        vpslld    $14, %xmm8, %xmm0                             #1166.21
        vpslld    $14, %xmm9, %xmm2                             #1166.76
        vmulps    %ymm11, %ymm11, %ymm11                        #1173.13
        vunpcklps %ymm7, %ymm5, %ymm6                           #1163.2009
        vunpckhps %ymm7, %ymm5, %ymm7                           #1163.2083
        vaddps    576+__svml_sexpm1_ha_data_internal(%rip), %ymm4, %ymm5 #1172.12
        vinsertf128 $1, %xmm2, %ymm0, %ymm3                     #1167.16
        vmovups   1216+__svml_sexpm1_ha_data_internal(%rip), %ymm2 #1181.48
        vmulps    %ymm11, %ymm5, %ymm0                          #1180.27
        vmulps    %ymm3, %ymm7, %ymm9                           #1169.13
        vaddps    %ymm0, %ymm13, %ymm13                         #1180.12
        vorps     %ymm3, %ymm6, %ymm8                           #1168.13
        vandps    %ymm14, %ymm8, %ymm1                          #1182.160
        vandps    %ymm14, %ymm2, %ymm14                         #1182.208
        vcmple_oqps %ymm14, %ymm1, %ymm14                       #1182.259
        vaddps    %ymm2, %ymm8, %ymm5                           #1183.14
        vmulps    %ymm10, %ymm8, %ymm12                         #1179.15
        vaddps    %ymm13, %ymm10, %ymm10                        #1191.13
        vblendvps %ymm14, %ymm2, %ymm8, %ymm0                   #1182.316
        vblendvps %ymm14, %ymm8, %ymm2, %ymm1                   #1182.371
        vsubps    %ymm5, %ymm0, %ymm3                           #1184.14
        vaddps    %ymm5, %ymm12, %ymm14                         #1187.15
        vmulps    %ymm13, %ymm8, %ymm8                          #1190.29
        vaddps    %ymm3, %ymm1, %ymm4                           #1185.14
        vsubps    %ymm14, %ymm5, %ymm6                          #1188.14
        vaddps    %ymm4, %ymm9, %ymm7                           #1186.14
        vaddps    %ymm6, %ymm12, %ymm12                         #1189.17
        vmulps    %ymm10, %ymm9, %ymm9                          #1192.29
        vaddps    %ymm8, %ymm12, %ymm11                         #1190.14
        vaddps    %ymm9, %ymm7, %ymm13                          #1192.14
        vaddps    %ymm13, %ymm11, %ymm0                         #1193.14
        vaddps    %ymm0, %ymm14, %ymm1                          #1194.14
        vorps     %ymm15, %ymm1, %ymm0                          #1195.14
        vmovups   (%rsp), %ymm1                                 #1197.52[spill]
        testb     %dl, %dl                                      #1197.52
        jne       ..B6.3        # Prob 5%                       #1197.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm1
..B6.2:                         # Preds ..B6.3 ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1200.12
        popq      %rbp                                          #1200.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1200.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm1, 64(%rsp)                               #1197.196
        vmovups   %ymm0, 128(%rsp)                              #1197.270
        testl     %edx, %edx                                    #1197.374
        je        ..B6.2        # Prob 95%                      #1197.374
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1197.454
                                # LOE rbx r12 r13 r14 r15 eax edx
..B6.13:                        # Preds ..B6.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1197.517
        jc        ..B6.10       # Prob 5%                       #1197.517
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1197.470
        cmpl      $8, %r12d                                     #1197.465
        jl        ..B6.7        # Prob 82%                      #1197.465
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #1197.676
        jmp       ..B6.2        # Prob 100%                     #1197.676
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1197.546
        lea       128(%rsp,%r12,4), %rsi                        #1197.546
..___tag_value___svml_expm1f8_ha_e9.136:
#       __svml_sexpm1_ha_cout_rare_internal(const float *, float *)
        call      __svml_sexpm1_ha_cout_rare_internal           #1197.546
..___tag_value___svml_expm1f8_ha_e9.137:
        jmp       ..B6.8        # Prob 100%                     #1197.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expm1f8_ha_e9,@function
	.size	__svml_expm1f8_ha_e9,.-__svml_expm1f8_ha_e9
..LN__svml_expm1f8_ha_e9.5:
	.data
# -- End  __svml_expm1f8_ha_e9
	.text
.L_2__routine_start___svml_sexpm1_ha_cout_rare_internal_6:
# -- Begin  __svml_sexpm1_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_sexpm1_ha_cout_rare_internal
	.globl __svml_sexpm1_ha_cout_rare_internal
# --- __svml_sexpm1_ha_cout_rare_internal(const float *, float *)
__svml_sexpm1_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #394.1
        .byte     15                                            #503.19
        .byte     30                                            #503.19
        .byte     250                                           #503.19
	.cfi_startproc
..___tag_value___svml_sexpm1_ha_cout_rare_internal.139:
..L140:
                                                        #394.1
        xorl      %eax, %eax                                    #402.14
        movss     (%rdi), %xmm7                                 #403.10
        pxor      %xmm0, %xmm0                                  #403.5
        comiss    %xmm7, %xmm0                                  #403.13
        ja        ..B7.18       # Prob 28%                      #403.13
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm7
..B7.2:                         # Preds ..B7.1
                                # Execution count [7.20e-01]
        movzwl    2(%rdi), %edx                                 #405.35
        andl      $32640, %edx                                  #405.35
        shrl      $7, %edx                                      #405.35
        cmpl      $255, %edx                                    #405.61
        je        ..B7.19       # Prob 16%                      #405.61
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax edx xmm7
..B7.3:                         # Preds ..B7.2
                                # Execution count [6.05e-01]
        cvtss2sd  %xmm7, %xmm7                                  #407.28
        cmpl      $74, %edx                                     #408.63
        jle       ..B7.16       # Prob 50%                      #408.63
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm7
..B7.4:                         # Preds ..B7.3
                                # Execution count [3.02e-01]
        movsd     1080+_vmldExpHATab(%rip), %xmm0               #410.45
        comisd    %xmm7, %xmm0                                  #410.45
        jb        ..B7.15       # Prob 50%                      #410.45
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm7
..B7.5:                         # Preds ..B7.4
                                # Execution count [1.51e-01]
        comisd    1096+_vmldExpHATab(%rip), %xmm7               #412.49
        jb        ..B7.14       # Prob 50%                      #412.49
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm7
..B7.6:                         # Preds ..B7.5
                                # Execution count [7.56e-02]
        movsd     1024+_vmldExpHATab(%rip), %xmm1               #414.51
        movaps    %xmm7, %xmm6                                  #420.33
        mulsd     %xmm7, %xmm1                                  #414.51
        lea       _vmldExpHATab(%rip), %r10                     #429.47
        movsd     %xmm1, -16(%rsp)                              #414.25
        movsd     -16(%rsp), %xmm2                              #415.30
        movsd     1072+_vmldExpHATab(%rip), %xmm1               #421.50
        movsd     1136+_vmldExpHATab(%rip), %xmm0               #404.5
        movsd     %xmm0, -24(%rsp)                              #404.5
        addsd     1032+_vmldExpHATab(%rip), %xmm2               #415.51
        movsd     %xmm2, -8(%rsp)                               #415.25
        movsd     -8(%rsp), %xmm3                               #416.29
        movl      -8(%rsp), %r8d                                #424.50
        movl      %r8d, %ecx                                    #426.34
        andl      $63, %r8d                                     #425.33
        subsd     1032+_vmldExpHATab(%rip), %xmm3               #416.51
        movsd     %xmm3, -16(%rsp)                              #416.25
        lea       1(%r8,%r8), %r9d                              #428.71
        movsd     -16(%rsp), %xmm5                              #417.31
        lea       (%r8,%r8), %edi                               #428.69
        movsd     -16(%rsp), %xmm4                              #418.31
        mulsd     1112+_vmldExpHATab(%rip), %xmm4               #418.52
        mulsd     1104+_vmldExpHATab(%rip), %xmm5               #417.52
        subsd     %xmm4, %xmm6                                  #420.33
        shrl      $6, %ecx                                      #426.34
        subsd     %xmm5, %xmm6                                  #419.34
        comisd    1088+_vmldExpHATab(%rip), %xmm7               #430.53
        mulsd     %xmm6, %xmm1                                  #421.72
        movsd     (%r10,%rdi,8), %xmm0                          #429.47
        lea       1023(%rcx), %edx                              #427.33
        addsd     1064+_vmldExpHATab(%rip), %xmm1               #421.93
        mulsd     %xmm6, %xmm1                                  #421.116
        addsd     1056+_vmldExpHATab(%rip), %xmm1               #421.137
        mulsd     %xmm6, %xmm1                                  #421.160
        addsd     1048+_vmldExpHATab(%rip), %xmm1               #422.46
        mulsd     %xmm6, %xmm1                                  #422.69
        addsd     1040+_vmldExpHATab(%rip), %xmm1               #422.90
        mulsd     %xmm6, %xmm1                                  #422.113
        mulsd     %xmm6, %xmm1                                  #423.33
        addsd     %xmm6, %xmm1                                  #423.37
        addsd     (%r10,%r9,8), %xmm1                           #428.52
        mulsd     %xmm0, %xmm1                                  #429.25
        jb        ..B7.10       # Prob 50%                      #430.53
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax edx ecx xmm0 xmm1
..B7.7:                         # Preds ..B7.6
                                # Execution count [3.78e-02]
        andl      $2047, %edx                                   #433.37
        addsd     %xmm0, %xmm1                                  #432.54
        cmpl      $2046, %edx                                   #434.39
        ja        ..B7.9        # Prob 50%                      #434.39
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax edx xmm1
..B7.8:                         # Preds ..B7.7
                                # Execution count [1.89e-02]
        movzwl    1142+_vmldExpHATab(%rip), %ecx                #436.54
        shll      $4, %edx                                      #436.54
        andl      $-32753, %ecx                                 #436.54
        orl       %edx, %ecx                                    #436.54
        movw      %cx, -18(%rsp)                                #436.54
        movsd     -24(%rsp), %xmm0                              #437.38
        mulsd     %xmm0, %xmm1                                  #437.33
        cvtsd2ss  %xmm1, %xmm1                                  #438.35
        movss     %xmm1, (%rsi)                                 #438.35
        ret                                                     #438.35
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.9:                         # Preds ..B7.7
                                # Execution count [1.89e-02]
        decl      %edx                                          #442.82
        andl      $2047, %edx                                   #442.54
        movzwl    -18(%rsp), %ecx                               #442.54
        shll      $4, %edx                                      #442.54
        andl      $-32753, %ecx                                 #442.54
        orl       %edx, %ecx                                    #442.54
        movw      %cx, -18(%rsp)                                #442.54
        movsd     -24(%rsp), %xmm0                              #443.41
        mulsd     %xmm0, %xmm1                                  #443.41
        mulsd     1152+_vmldExpHATab(%rip), %xmm1               #444.76
        cvtsd2ss  %xmm1, %xmm1                                  #445.35
        movss     %xmm1, (%rsi)                                 #445.35
        ret                                                     #445.35
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.10:                        # Preds ..B7.6
                                # Execution count [3.78e-02]
        addl      $1083, %ecx                                   #450.37
        andl      $2047, %ecx                                   #451.37
        movl      %ecx, %eax                                    #452.50
        movzwl    -18(%rsp), %edx                               #452.50
        shll      $4, %eax                                      #452.50
        andl      $-32753, %edx                                 #452.50
        orl       %eax, %edx                                    #452.50
        movw      %dx, -18(%rsp)                                #452.50
        movsd     -24(%rsp), %xmm3                              #453.34
        mulsd     %xmm3, %xmm1                                  #453.29
        mulsd     %xmm0, %xmm3                                  #454.61
        movaps    %xmm1, %xmm2                                  #457.41
        addsd     %xmm3, %xmm2                                  #457.41
        cmpl      $50, %ecx                                     #455.39
        ja        ..B7.12       # Prob 50%                      #455.39
                                # LOE rbx rbp rsi r12 r13 r14 r15 xmm1 xmm2 xmm3
..B7.11:                        # Preds ..B7.10
                                # Execution count [1.89e-02]
        movsd     1160+_vmldExpHATab(%rip), %xmm0               #458.58
        mulsd     %xmm2, %xmm0                                  #458.58
        cvtsd2ss  %xmm0, %xmm0                                  #459.35
        movss     %xmm0, (%rsi)                                 #459.35
        jmp       ..B7.13       # Prob 100%                     #459.35
                                # LOE rbx rbp r12 r13 r14 r15
..B7.12:                        # Preds ..B7.10
                                # Execution count [1.89e-02]
        movsd     %xmm2, -72(%rsp)                              #463.34
        movsd     -72(%rsp), %xmm0                              #464.58
        subsd     %xmm0, %xmm3                                  #464.58
        movsd     %xmm3, -64(%rsp)                              #464.34
        movsd     -64(%rsp), %xmm2                              #465.34
        addsd     %xmm1, %xmm2                                  #465.34
        movsd     %xmm2, -64(%rsp)                              #465.34
        movsd     -72(%rsp), %xmm1                              #466.80
        mulsd     1168+_vmldExpHATab(%rip), %xmm1               #466.80
        movsd     %xmm1, -56(%rsp)                              #466.34
        movsd     -72(%rsp), %xmm4                              #467.41
        movsd     -56(%rsp), %xmm3                              #467.58
        addsd     %xmm3, %xmm4                                  #467.58
        movsd     %xmm4, -48(%rsp)                              #467.34
        movsd     -48(%rsp), %xmm6                              #468.45
        movsd     -56(%rsp), %xmm5                              #468.52
        subsd     %xmm5, %xmm6                                  #468.52
        movsd     %xmm6, -40(%rsp)                              #468.34
        movsd     -72(%rsp), %xmm8                              #469.45
        movsd     -40(%rsp), %xmm7                              #469.62
        subsd     %xmm7, %xmm8                                  #469.62
        movsd     %xmm8, -32(%rsp)                              #469.34
        movsd     -64(%rsp), %xmm10                             #470.46
        movsd     -32(%rsp), %xmm9                              #470.34
        addsd     %xmm9, %xmm10                                 #470.34
        movsd     %xmm10, -32(%rsp)                             #470.34
        movsd     -40(%rsp), %xmm11                             #471.45
        mulsd     1160+_vmldExpHATab(%rip), %xmm11              #471.73
        movsd     %xmm11, -40(%rsp)                             #471.34
        movsd     -32(%rsp), %xmm12                             #472.45
        mulsd     1160+_vmldExpHATab(%rip), %xmm12              #472.73
        movsd     %xmm12, -32(%rsp)                             #472.34
        movsd     -40(%rsp), %xmm14                             #473.38
        movsd     -32(%rsp), %xmm13                             #473.49
        addsd     %xmm13, %xmm14                                #473.49
        cvtsd2ss  %xmm14, %xmm14                                #474.36
        movss     %xmm14, (%rsi)                                #474.36
                                # LOE rbx rbp r12 r13 r14 r15
..B7.13:                        # Preds ..B7.11 ..B7.12
                                # Execution count [3.78e-02]
        movl      $4, %eax                                      #476.25
        ret                                                     #476.25
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.14:                        # Preds ..B7.5
                                # Execution count [7.56e-02]
        movsd     1120+_vmldExpHATab(%rip), %xmm0               #481.85
        movl      $4, %eax                                      #483.25
        mulsd     %xmm0, %xmm0                                  #481.85
        cvtsd2ss  %xmm0, %xmm0                                  #482.27
        movss     %xmm0, (%rsi)                                 #482.27
        ret                                                     #482.27
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.15:                        # Preds ..B7.4
                                # Execution count [1.51e-01]
        movsd     1128+_vmldExpHATab(%rip), %xmm0               #488.81
        movl      $3, %eax                                      #490.21
        mulsd     %xmm0, %xmm0                                  #488.81
        cvtsd2ss  %xmm0, %xmm0                                  #489.23
        movss     %xmm0, (%rsi)                                 #489.23
        ret                                                     #489.23
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.16:                        # Preds ..B7.3
                                # Execution count [3.02e-01]
        movsd     1144+_vmldExpHATab(%rip), %xmm0               #495.38
        addsd     %xmm7, %xmm0                                  #495.60
        cvtsd2ss  %xmm0, %xmm0                                  #496.19
        movss     %xmm0, (%rsi)                                 #496.19
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.17:                        # Preds ..B7.16
                                # Execution count [7.20e-01]
        ret                                                     #510.12
                                # LOE
..B7.18:                        # Preds ..B7.1
                                # Execution count [2.80e-01]
        movl      $-1082130432, (%rsi)                          #403.19
        ret                                                     #403.35
                                # LOE
..B7.19:                        # Preds ..B7.2
                                # Execution count [1.15e-01]: Infreq
        movb      3(%rdi), %dl                                  #501.39
        andb      $-128, %dl                                    #501.39
        cmpb      $-128, %dl                                    #501.61
        je        ..B7.21       # Prob 16%                      #501.61
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm7
..B7.20:                        # Preds ..B7.21 ..B7.19
                                # Execution count [1.06e-01]: Infreq
        mulss     %xmm7, %xmm7                                  #507.33
        movss     %xmm7, (%rsi)                                 #507.19
        ret                                                     #507.19
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.21:                        # Preds ..B7.19
                                # Execution count [1.84e-02]: Infreq
        testl     $8388607, (%rdi)                              #501.88
        jne       ..B7.20       # Prob 50%                      #501.117
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm7
..B7.22:                        # Preds ..B7.21
                                # Execution count [9.22e-03]: Infreq
        movsd     1136+_vmldExpHATab(%rip), %xmm0               #503.19
        cvtsd2ss  %xmm0, %xmm0                                  #503.19
        movss     %xmm0, (%rsi)                                 #503.19
        ret                                                     #503.19
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_sexpm1_ha_cout_rare_internal,@function
	.size	__svml_sexpm1_ha_cout_rare_internal,.-__svml_sexpm1_ha_cout_rare_internal
..LN__svml_sexpm1_ha_cout_rare_internal.6:
	.data
# -- End  __svml_sexpm1_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_sexpm1_ha_data_internal_avx512
	.globl __svml_sexpm1_ha_data_internal_avx512
__svml_sexpm1_ha_data_internal_avx512:
	.long	1065353216
	.long	1065724611
	.long	1066112450
	.long	1066517459
	.long	1066940400
	.long	1067382066
	.long	1067843287
	.long	1068324927
	.long	1068827891
	.long	1069353124
	.long	1069901610
	.long	1070474380
	.long	1071072509
	.long	1071697119
	.long	1072349383
	.long	1073030525
	.long	0
	.long	860277610
	.long	2991457809
	.long	860562562
	.long	856238081
	.long	857441778
	.long	3003456168
	.long	2986372182
	.long	848495278
	.long	3003311522
	.long	833868005
	.long	835836658
	.long	2994528642
	.long	2981408986
	.long	2983366846
	.long	833659207
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	2147483648
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3263168512
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1026207428
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1042984644
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1056964608
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	1065353216
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.type	__svml_sexpm1_ha_data_internal_avx512,@object
	.size	__svml_sexpm1_ha_data_internal_avx512,960
	.align 64
	.hidden __svml_sexpm1_ha_data_internal
	.globl __svml_sexpm1_ha_data_internal
__svml_sexpm1_ha_data_internal:
	.long	0
	.long	0
	.long	90112
	.long	958021240
	.long	184320
	.long	3097385429
	.long	278528
	.long	3107645322
	.long	372736
	.long	3106378502
	.long	466944
	.long	3080612193
	.long	565248
	.long	3112054766
	.long	659456
	.long	958049961
	.long	757760
	.long	959986813
	.long	860160
	.long	3106149520
	.long	958464
	.long	956772713
	.long	1060864
	.long	946741642
	.long	1163264
	.long	955571435
	.long	1269760
	.long	3107625502
	.long	1372160
	.long	958264608
	.long	1478656
	.long	956671716
	.long	1585152
	.long	964561458
	.long	1695744
	.long	923477569
	.long	1806336
	.long	3098644875
	.long	1916928
	.long	3088264298
	.long	2027520
	.long	958811154
	.long	2142208
	.long	924983155
	.long	2256896
	.long	923141657
	.long	2371584
	.long	958576065
	.long	2490368
	.long	3088362279
	.long	2609152
	.long	3097170630
	.long	2727936
	.long	942496373
	.long	2850816
	.long	3107506507
	.long	2973696
	.long	3111658168
	.long	3096576
	.long	3104893523
	.long	3219456
	.long	956129397
	.long	3346432
	.long	945949013
	.long	3473408
	.long	958293631
	.long	3604480
	.long	3097200438
	.long	3735552
	.long	3104446228
	.long	3866624
	.long	3081063917
	.long	4001792
	.long	3110833173
	.long	4132864
	.long	963851983
	.long	4272128
	.long	3102526822
	.long	4407296
	.long	962173765
	.long	4546560
	.long	962937387
	.long	4689920
	.long	3097677269
	.long	4833280
	.long	3108523826
	.long	4976640
	.long	3103440963
	.long	5120000
	.long	957448868
	.long	5267456
	.long	952956604
	.long	5419008
	.long	3111931330
	.long	5566464
	.long	953592564
	.long	5718016
	.long	958372187
	.long	5873664
	.long	3099234146
	.long	6029312
	.long	3104378117
	.long	6184960
	.long	938771038
	.long	6344704
	.long	3100132597
	.long	6504448
	.long	3077380739
	.long	6668288
	.long	3110019856
	.long	6832128
	.long	3109205496
	.long	6995968
	.long	935782365
	.long	7163904
	.long	3083854118
	.long	7331840
	.long	957821820
	.long	7503872
	.long	943431213
	.long	7675904
	.long	959423141
	.long	7852032
	.long	937390859
	.long	8028160
	.long	955037405
	.long	8208384
	.long	3095001478
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1042983615
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	1056964623
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1119398459
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	1009876992
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	3059646595
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	1240530432
	.long	504
	.long	504
	.long	504
	.long	504
	.long	504
	.long	504
	.long	504
	.long	504
	.long	504
	.long	504
	.long	504
	.long	504
	.long	504
	.long	504
	.long	504
	.long	504
	.long	130560
	.long	130560
	.long	130560
	.long	130560
	.long	130560
	.long	130560
	.long	130560
	.long	130560
	.long	130560
	.long	130560
	.long	130560
	.long	130560
	.long	130560
	.long	130560
	.long	130560
	.long	130560
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	3212836864
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	2147483647
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1118652779
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1111009851
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1018262040
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	1245724544
	.long	124
	.long	124
	.long	124
	.long	124
	.long	124
	.long	124
	.long	124
	.long	124
	.long	124
	.long	124
	.long	124
	.long	124
	.long	124
	.long	124
	.long	124
	.long	124
	.long	32640
	.long	32640
	.long	32640
	.long	32640
	.long	32640
	.long	32640
	.long	32640
	.long	32640
	.long	32640
	.long	32640
	.long	32640
	.long	32640
	.long	32640
	.long	32640
	.long	32640
	.long	32640
	.type	__svml_sexpm1_ha_data_internal,@object
	.size	__svml_sexpm1_ha_data_internal,1728
	.align 32
_vmldExpHATab:
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	1048019041
	.long	1072704666
	.long	2631457885
	.long	3161546771
	.long	3541402996
	.long	1072716208
	.long	896005651
	.long	1015861842
	.long	410360776
	.long	1072727877
	.long	1642514529
	.long	1012987726
	.long	1828292879
	.long	1072739672
	.long	1568897901
	.long	1016568486
	.long	852742562
	.long	1072751596
	.long	1882168529
	.long	1010744893
	.long	3490863953
	.long	1072763649
	.long	707771662
	.long	3163903570
	.long	2930322912
	.long	1072775834
	.long	3117806614
	.long	3163670819
	.long	1014845819
	.long	1072788152
	.long	3936719688
	.long	3162512149
	.long	3949972341
	.long	1072800603
	.long	1058231231
	.long	1015777676
	.long	828946858
	.long	1072813191
	.long	1044000608
	.long	1016786167
	.long	2288159958
	.long	1072825915
	.long	1151779725
	.long	1015705409
	.long	1853186616
	.long	1072838778
	.long	3819481236
	.long	1016499965
	.long	1709341917
	.long	1072851781
	.long	2552227826
	.long	1015039787
	.long	4112506593
	.long	1072864925
	.long	1829350193
	.long	1015216097
	.long	2799960843
	.long	1072878213
	.long	1913391796
	.long	1015756674
	.long	171030293
	.long	1072891646
	.long	1303423926
	.long	1015238005
	.long	2992903935
	.long	1072905224
	.long	1574172746
	.long	1016061241
	.long	926591435
	.long	1072918951
	.long	3427487848
	.long	3163704045
	.long	887463927
	.long	1072932827
	.long	1049900754
	.long	3161575912
	.long	1276261410
	.long	1072946854
	.long	2804567149
	.long	1015390024
	.long	569847338
	.long	1072961034
	.long	1209502043
	.long	3159926671
	.long	1617004845
	.long	1072975368
	.long	1623370769
	.long	1011049453
	.long	3049340112
	.long	1072989858
	.long	3667985273
	.long	1013894369
	.long	3577096743
	.long	1073004506
	.long	3145379760
	.long	1014403278
	.long	1990012071
	.long	1073019314
	.long	7447438
	.long	3163526196
	.long	1453150082
	.long	1073034283
	.long	3171891295
	.long	3162037958
	.long	917841882
	.long	1073049415
	.long	419288974
	.long	1016280325
	.long	3712504873
	.long	1073064711
	.long	3793507337
	.long	1016095713
	.long	363667784
	.long	1073080175
	.long	728023093
	.long	1016345318
	.long	2956612997
	.long	1073095806
	.long	1005538728
	.long	3163304901
	.long	2186617381
	.long	1073111608
	.long	2018924632
	.long	3163803357
	.long	1719614413
	.long	1073127582
	.long	3210617384
	.long	3163796463
	.long	1013258799
	.long	1073143730
	.long	3094194670
	.long	3160631279
	.long	3907805044
	.long	1073160053
	.long	2119843535
	.long	3161988964
	.long	1447192521
	.long	1073176555
	.long	508946058
	.long	3162904882
	.long	1944781191
	.long	1073193236
	.long	3108873501
	.long	3162190556
	.long	919555682
	.long	1073210099
	.long	2882956373
	.long	1013312481
	.long	2571947539
	.long	1073227145
	.long	4047189812
	.long	3163777462
	.long	2604962541
	.long	1073244377
	.long	3631372142
	.long	3163870288
	.long	1110089947
	.long	1073261797
	.long	3253791412
	.long	1015920431
	.long	2568320822
	.long	1073279406
	.long	1509121860
	.long	1014756995
	.long	2966275557
	.long	1073297207
	.long	2339118633
	.long	3160254904
	.long	2682146384
	.long	1073315202
	.long	586480042
	.long	3163702083
	.long	2191782032
	.long	1073333393
	.long	730975783
	.long	1014083580
	.long	2069751141
	.long	1073351782
	.long	576856675
	.long	3163014404
	.long	2990417245
	.long	1073370371
	.long	3552361237
	.long	3163667409
	.long	1434058175
	.long	1073389163
	.long	1853053619
	.long	1015310724
	.long	2572866477
	.long	1073408159
	.long	2462790535
	.long	1015814775
	.long	3092190715
	.long	1073427362
	.long	1457303226
	.long	3159737305
	.long	4076559943
	.long	1073446774
	.long	950899508
	.long	3160987380
	.long	2420883922
	.long	1073466398
	.long	174054861
	.long	1014300631
	.long	3716502172
	.long	1073486235
	.long	816778419
	.long	1014197934
	.long	777507147
	.long	1073506289
	.long	3507050924
	.long	1015341199
	.long	3706687593
	.long	1073526560
	.long	1821514088
	.long	1013410604
	.long	1242007932
	.long	1073547053
	.long	1073740399
	.long	3163532637
	.long	3707479175
	.long	1073567768
	.long	2789017511
	.long	1014276997
	.long	64696965
	.long	1073588710
	.long	3586233004
	.long	1015962192
	.long	863738719
	.long	1073609879
	.long	129252895
	.long	3162690849
	.long	3884662774
	.long	1073631278
	.long	1614448851
	.long	1014281732
	.long	2728693978
	.long	1073652911
	.long	2413007344
	.long	3163551506
	.long	3999357479
	.long	1073674779
	.long	1101668360
	.long	1015989180
	.long	1533953344
	.long	1073696886
	.long	835814894
	.long	1015702697
	.long	2174652632
	.long	1073719233
	.long	1301400989
	.long	1014466875
	.long	1697350398
	.long	1079448903
	.long	0
	.long	1127743488
	.long	0
	.long	1071644672
	.long	1431652600
	.long	1069897045
	.long	1431670732
	.long	1067799893
	.long	984555731
	.long	1065423122
	.long	472530941
	.long	1062650218
	.long	3758096384
	.long	1079389762
	.long	3758096384
	.long	3226850697
	.long	2147483648
	.long	3227123254
	.long	4277796864
	.long	1065758274
	.long	3164486458
	.long	1025308570
	.long	1
	.long	1048576
	.long	4294967295
	.long	2146435071
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	0
	.long	1073741824
	.long	0
	.long	1009778688
	.long	0
	.long	1106771968
	.type	_vmldExpHATab,@object
	.size	_vmldExpHATab,1176
	.space 8, 0x00 	# pad
	.align 16
.L_2il0floatpacket.29:
	.long	0x4b400000,0x4b400000,0x4b400000,0x4b400000
	.type	.L_2il0floatpacket.29,@object
	.size	.L_2il0floatpacket.29,16
	.align 4
.L_2il0floatpacket.88:
	.long	0xbf800000
	.type	.L_2il0floatpacket.88,@object
	.size	.L_2il0floatpacket.88,4
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
