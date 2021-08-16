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
	.file "svml_d_expm1.c"
	.text
..TXTST0:
.L_2__routine_start___svml_expm14_ha_l9_0:
# -- Begin  __svml_expm14_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm14_ha_l9
# --- __svml_expm14_ha_l9(__m256d)
__svml_expm14_ha_l9:
# parameter 1: %ymm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #332.1
        .byte     15                                            #443.552
        .byte     30                                            #443.552
        .byte     250                                           #443.552
	.cfi_startproc
..___tag_value___svml_expm14_ha_l9.1:
..L2:
                                                          #332.1
        pushq     %rbp                                          #332.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #332.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #332.1
        subq      $192, %rsp                                    #332.1
        lea       __svml_dexpm1_ha_data_internal(%rip), %r8     #405.68
        vmovapd   %ymm0, %ymm2                                  #332.1
        vmulpd    2304+__svml_dexpm1_ha_data_internal(%rip), %ymm2, %ymm4 #385.12
        vmovupd   2816+__svml_dexpm1_ha_data_internal(%rip), %ymm3 #393.52
        vmovupd   2368+__svml_dexpm1_ha_data_internal(%rip), %ymm0 #389.14
        vroundpd  $0, %ymm4, %ymm8                              #386.12
        vaddpd    2496+__svml_dexpm1_ha_data_internal(%rip), %ymm8, %ymm15 #401.16
        vmulpd    2432+__svml_dexpm1_ha_data_internal(%rip), %ymm8, %ymm1 #390.14
        vfnmadd213pd %ymm2, %ymm8, %ymm0                        #389.14
        vmovupd   2048+__svml_dexpm1_ha_data_internal(%rip), %ymm8 #416.14
        vsubpd    %ymm1, %ymm0, %ymm5                           #391.12
        vcmpeqpd  %ymm2, %ymm0, %ymm6                           #392.20
        vfmadd213pd 2112+__svml_dexpm1_ha_data_internal(%rip), %ymm5, %ymm8 #416.14
        vandps    2560+__svml_dexpm1_ha_data_internal(%rip), %ymm15, %ymm10 #404.18
        vandpd    %ymm3, %ymm2, %ymm9                           #395.13
        vcmpnle_uqpd 2880+__svml_dexpm1_ha_data_internal(%rip), %ymm9, %ymm7 #397.18
        vmovmskpd %ymm7, %eax                                   #399.44
        vandps    2624+__svml_dexpm1_ha_data_internal(%rip), %ymm15, %ymm15 #407.17
        vorpd     2688+__svml_dexpm1_ha_data_internal(%rip), %ymm6, %ymm6 #420.19
        vandpd    %ymm6, %ymm0, %ymm6                           #421.13
        vandnpd   %ymm2, %ymm3, %ymm4                           #396.14
        vextractf128 $1, %ymm10, %xmm11                         #405.546
        vmovd     %xmm10, %edx                                  #405.138
        vmovd     %xmm11, %esi                                  #405.527
        vpextrd   $2, %xmm10, %ecx                              #405.331
        vpextrd   $2, %xmm11, %edi                              #405.725
        movslq    %edx, %rdx                                    #405.68
        movslq    %ecx, %rcx                                    #405.261
        movslq    %esi, %rsi                                    #405.457
        movslq    %edi, %rdi                                    #405.655
        vmovupd   (%r8,%rdx), %xmm14                            #405.68
        vmovupd   (%r8,%rcx), %xmm13                            #405.261
        vmovupd   (%r8,%rsi), %xmm12                            #405.457
        vmovupd   (%r8,%rdi), %xmm11                            #405.655
        vunpcklpd %xmm13, %xmm14, %xmm9                         #405.853
        vunpcklpd %xmm11, %xmm12, %xmm7                         #405.886
        vunpckhpd %xmm11, %xmm12, %xmm12                        #405.1013
        vmulpd    %ymm5, %ymm5, %ymm11                          #418.13
        vunpckhpd %xmm13, %xmm14, %xmm13                        #405.980
        vinsertf128 $1, %xmm7, %ymm9, %ymm10                    #405.807
        vpsllq    $41, %ymm15, %ymm7                            #408.17
        vorpd     %ymm7, %ymm10, %ymm9                          #410.13
        vmovupd   2176+__svml_dexpm1_ha_data_internal(%rip), %ymm10 #417.14
        vfmadd213pd 2240+__svml_dexpm1_ha_data_internal(%rip), %ymm5, %ymm10 #417.14
        vsubpd    %ymm6, %ymm0, %ymm5                           #422.13
        vfmadd213pd %ymm10, %ymm11, %ymm8                       #423.12
        vsubpd    %ymm1, %ymm5, %ymm1                           #424.13
        vmulpd    %ymm6, %ymm9, %ymm5                           #425.15
        vfmadd213pd %ymm1, %ymm11, %ymm8                        #426.12
        vmovupd   2752+__svml_dexpm1_ha_data_internal(%rip), %ymm1 #427.49
        vaddpd    %ymm8, %ymm6, %ymm6                           #437.13
        vandpd    %ymm3, %ymm9, %ymm0                           #428.163
        vandpd    %ymm3, %ymm1, %ymm3                           #428.211
        vcmple_oqpd %ymm3, %ymm0, %ymm0                         #428.262
        vblendvpd %ymm0, %ymm1, %ymm9, %ymm3                    #428.319
        vblendvpd %ymm0, %ymm9, %ymm1, %ymm10                   #428.374
        vinsertf128 $1, %xmm12, %ymm13, %ymm14                  #405.934
        vaddpd    %ymm1, %ymm9, %ymm13                          #429.14
        vmulpd    %ymm7, %ymm14, %ymm7                          #411.13
        vsubpd    %ymm13, %ymm3, %ymm11                         #430.14
        vaddpd    %ymm13, %ymm5, %ymm0                          #433.15
        vaddpd    %ymm11, %ymm10, %ymm12                        #431.14
        vsubpd    %ymm0, %ymm13, %ymm14                         #434.14
        vaddpd    %ymm12, %ymm7, %ymm15                         #432.14
        vaddpd    %ymm14, %ymm5, %ymm5                          #435.17
        vfmadd213pd %ymm15, %ymm6, %ymm7                        #438.14
        vfmadd213pd %ymm5, %ymm8, %ymm9                         #436.14
        vaddpd    %ymm7, %ymm9, %ymm7                           #439.14
        vaddpd    %ymm7, %ymm0, %ymm8                           #440.15
        vorpd     %ymm4, %ymm8, %ymm0                           #441.14
        testl     %eax, %eax                                    #443.52
        jne       ..B1.3        # Prob 5%                       #443.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm2
..B1.2:                         # Preds ..B1.3 ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #446.12
        popq      %rbp                                          #446.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #446.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm2, 64(%rsp)                               #443.200
        vmovupd   %ymm0, 128(%rsp)                              #443.276
        je        ..B1.2        # Prob 95%                      #443.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #443.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B1.13:                        # Preds ..B1.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #443.523
        jc        ..B1.10       # Prob 5%                       #443.523
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #443.476
        cmpl      $4, %r12d                                     #443.471
        jl        ..B1.7        # Prob 82%                      #443.471
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #443.684
        jmp       ..B1.2        # Prob 100%                     #443.684
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #443.552
        lea       128(%rsp,%r12,8), %rsi                        #443.552
..___tag_value___svml_expm14_ha_l9.19:
#       __svml_dexpm1_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexpm1_ha_cout_rare_internal           #443.552
..___tag_value___svml_expm14_ha_l9.20:
        jmp       ..B1.8        # Prob 100%                     #443.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expm14_ha_l9,@function
	.size	__svml_expm14_ha_l9,.-__svml_expm14_ha_l9
..LN__svml_expm14_ha_l9.0:
	.data
# -- End  __svml_expm14_ha_l9
	.text
.L_2__routine_start___svml_expm12_ha_e9_1:
# -- Begin  __svml_expm12_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm12_ha_e9
# --- __svml_expm12_ha_e9(__m128d)
__svml_expm12_ha_e9:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #451.1
        .byte     15                                            #562.546
        .byte     30                                            #562.546
        .byte     250                                           #562.546
	.cfi_startproc
..___tag_value___svml_expm12_ha_e9.22:
..L23:
                                                         #451.1
        pushq     %rbp                                          #451.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #451.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #451.1
        subq      $192, %rsp                                    #451.1
        movaps    %xmm0, %xmm10                                 #451.1
        movups    2304+__svml_dexpm1_ha_data_internal(%rip), %xmm11 #503.47
        movaps    %xmm10, %xmm9                                 #508.14
        mulpd     %xmm10, %xmm11                                #504.12
        movups    2368+__svml_dexpm1_ha_data_internal(%rip), %xmm12 #506.45
        lea       __svml_dexpm1_ha_data_internal(%rip), %rsi    #524.67
        roundpd   $0, %xmm11, %xmm11                            #505.12
        mulpd     %xmm11, %xmm12                                #508.34
        movups    2432+__svml_dexpm1_ha_data_internal(%rip), %xmm3 #509.14
        mulpd     %xmm11, %xmm3                                 #509.14
        subpd     %xmm12, %xmm9                                 #508.14
        movaps    %xmm9, %xmm6                                  #510.12
        movaps    %xmm9, %xmm7                                  #511.20
        subpd     %xmm3, %xmm6                                  #510.12
        cmpeqpd   %xmm10, %xmm7                                 #511.20
        movups    2816+__svml_dexpm1_ha_data_internal(%rip), %xmm5 #512.49
        movaps    %xmm5, %xmm13                                 #514.13
        movaps    %xmm5, %xmm8                                  #515.14
        andps     %xmm10, %xmm13                                #514.13
        andnps    %xmm10, %xmm8                                 #515.14
        cmpnlepd  2880+__svml_dexpm1_ha_data_internal(%rip), %xmm13 #516.18
        movups    2048+__svml_dexpm1_ha_data_internal(%rip), %xmm12 #535.26
        mulpd     %xmm6, %xmm12                                 #535.26
        movmskpd  %xmm13, %edx                                  #518.44
        movups    2176+__svml_dexpm1_ha_data_internal(%rip), %xmm13 #536.26
        movups    2496+__svml_dexpm1_ha_data_internal(%rip), %xmm14 #519.53
        mulpd     %xmm6, %xmm13                                 #536.26
        mulpd     %xmm6, %xmm6                                  #537.13
        addpd     2112+__svml_dexpm1_ha_data_internal(%rip), %xmm12 #535.14
        addpd     %xmm14, %xmm11                                #520.16
        addpd     2240+__svml_dexpm1_ha_data_internal(%rip), %xmm13 #536.14
        mulpd     %xmm6, %xmm12                                 #542.24
        movups    2560+__svml_dexpm1_ha_data_internal(%rip), %xmm15 #523.22
        andps     %xmm11, %xmm15                                #523.22
        movd      %xmm15, %eax                                  #524.137
        orps      2688+__svml_dexpm1_ha_data_internal(%rip), %xmm7 #539.19
        andps     %xmm9, %xmm7                                  #540.13
        movslq    %eax, %rax                                    #524.67
        pextrd    $2, %xmm15, %ecx                              #524.301
        addpd     %xmm12, %xmm13                                #542.12
        subpd     %xmm7, %xmm9                                  #541.13
        mulpd     %xmm13, %xmm6                                 #545.24
        subpd     %xmm3, %xmm9                                  #543.13
        movslq    %ecx, %rcx                                    #524.231
        movdqu    2624+__svml_dexpm1_ha_data_internal(%rip), %xmm0 #526.21
        movups    (%rsi,%rax), %xmm4                            #524.67
        pand      %xmm0, %xmm11                                 #526.21
        movaps    %xmm4, %xmm2                                  #524.350
        psllq     $41, %xmm11                                   #527.21
        movups    (%rsi,%rcx), %xmm1                            #524.231
        movaps    %xmm5, %xmm0                                  #547.163
        unpcklpd  %xmm1, %xmm2                                  #524.350
        movups    2752+__svml_dexpm1_ha_data_internal(%rip), %xmm3 #546.46
        orps      %xmm11, %xmm2                                 #529.13
        andps     %xmm2, %xmm0                                  #547.163
        andps     %xmm3, %xmm5                                  #547.208
        cmplepd   %xmm5, %xmm0                                  #547.256
        addpd     %xmm6, %xmm9                                  #545.12
        unpckhpd  %xmm1, %xmm4                                  #524.391
        movaps    %xmm2, %xmm1                                  #544.15
        movaps    %xmm2, %xmm6                                  #547.300
        movaps    %xmm3, %xmm14                                 #547.352
        mulpd     %xmm7, %xmm1                                  #544.15
        mulpd     %xmm11, %xmm4                                 #530.13
        addpd     %xmm9, %xmm7                                  #556.13
        blendvpd  %xmm0, %xmm3, %xmm6                           #547.300
        addpd     %xmm2, %xmm3                                  #548.14
        subpd     %xmm3, %xmm6                                  #549.14
        blendvpd  %xmm0, %xmm2, %xmm14                          #547.352
        movaps    %xmm3, %xmm0                                  #552.15
        addpd     %xmm1, %xmm0                                  #552.15
        addpd     %xmm6, %xmm14                                 #550.14
        mulpd     %xmm9, %xmm2                                  #555.26
        subpd     %xmm0, %xmm3                                  #553.14
        addpd     %xmm4, %xmm14                                 #551.14
        mulpd     %xmm7, %xmm4                                  #557.26
        addpd     %xmm3, %xmm1                                  #554.17
        addpd     %xmm4, %xmm14                                 #557.14
        addpd     %xmm2, %xmm1                                  #555.14
        addpd     %xmm14, %xmm1                                 #558.14
        addpd     %xmm1, %xmm0                                  #559.15
        orps      %xmm8, %xmm0                                  #560.14
        testl     %edx, %edx                                    #562.52
        jne       ..B2.3        # Prob 5%                       #562.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm10
..B2.2:                         # Preds ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #565.12
        popq      %rbp                                          #565.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #565.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm10, 64(%rsp)                              #562.197
        movups    %xmm0, 128(%rsp)                              #562.270
                                # LOE rbx r12 r13 r14 r15 edx
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #562.454
        movq      %r12, 8(%rsp)                                 #562.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #562.454
        movq      %r13, (%rsp)                                  #562.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #562.454
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #562.517
        jc        ..B2.10       # Prob 5%                       #562.517
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #562.470
        cmpl      $2, %r12d                                     #562.465
        jl        ..B2.7        # Prob 82%                      #562.465
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #562.675
        jmp       ..B2.2        # Prob 100%                     #562.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #562.546
        lea       128(%rsp,%r12,8), %rsi                        #562.546
..___tag_value___svml_expm12_ha_e9.40:
#       __svml_dexpm1_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexpm1_ha_cout_rare_internal           #562.546
..___tag_value___svml_expm12_ha_e9.41:
        jmp       ..B2.8        # Prob 100%                     #562.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expm12_ha_e9,@function
	.size	__svml_expm12_ha_e9,.-__svml_expm12_ha_e9
..LN__svml_expm12_ha_e9.1:
	.data
# -- End  __svml_expm12_ha_e9
	.text
.L_2__routine_start___svml_expm12_ha_l9_2:
# -- Begin  __svml_expm12_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm12_ha_l9
# --- __svml_expm12_ha_l9(__m128d)
__svml_expm12_ha_l9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #829.1
        .byte     15                                            #940.546
        .byte     30                                            #940.546
        .byte     250                                           #940.546
	.cfi_startproc
..___tag_value___svml_expm12_ha_l9.43:
..L44:
                                                         #829.1
        pushq     %rbp                                          #829.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #829.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #829.1
        subq      $192, %rsp                                    #829.1
        vmovapd   %xmm0, %xmm12                                 #829.1
        vmulpd    2304+__svml_dexpm1_ha_data_internal(%rip), %xmm12, %xmm13 #882.12
        vroundpd  $0, %xmm13, %xmm13                            #883.12
        lea       __svml_dexpm1_ha_data_internal(%rip), %rsi    #902.67
        vaddpd    2496+__svml_dexpm1_ha_data_internal(%rip), %xmm13, %xmm2 #898.16
        vmulpd    2432+__svml_dexpm1_ha_data_internal(%rip), %xmm13, %xmm8 #887.14
        vmovupd   2368+__svml_dexpm1_ha_data_internal(%rip), %xmm9 #886.14
        vfnmadd213pd %xmm12, %xmm13, %xmm9                      #886.14
        vandps    2560+__svml_dexpm1_ha_data_internal(%rip), %xmm2, %xmm5 #901.18
        vmovupd   2816+__svml_dexpm1_ha_data_internal(%rip), %xmm10 #890.49
        vmovd     %xmm5, %eax                                   #902.137
        vandpd    %xmm10, %xmm12, %xmm14                        #892.13
        vpand     2624+__svml_dexpm1_ha_data_internal(%rip), %xmm2, %xmm1 #904.17
        vandnpd   %xmm12, %xmm10, %xmm11                        #893.14
        vcmpnlepd 2880+__svml_dexpm1_ha_data_internal(%rip), %xmm14, %xmm15 #894.18
        vsubpd    %xmm8, %xmm9, %xmm7                           #888.12
        vcmpeqpd  %xmm12, %xmm9, %xmm6                          #889.20
        vmovmskpd %xmm15, %edx                                  #896.44
        vpsllq    $41, %xmm1, %xmm15                            #905.17
        vpextrd   $2, %xmm5, %ecx                               #902.301
        movslq    %eax, %rax                                    #902.67
        movslq    %ecx, %rcx                                    #902.231
        vmovupd   2176+__svml_dexpm1_ha_data_internal(%rip), %xmm1 #914.14
        vfmadd213pd 2240+__svml_dexpm1_ha_data_internal(%rip), %xmm7, %xmm1 #914.14
        vmovupd   (%rsi,%rax), %xmm3                            #902.67
        vmovupd   (%rsi,%rcx), %xmm4                            #902.231
        vunpcklpd %xmm4, %xmm3, %xmm0                           #902.350
        vunpckhpd %xmm4, %xmm3, %xmm14                          #902.391
        vorpd     %xmm15, %xmm0, %xmm5                          #907.13
        vmovupd   2048+__svml_dexpm1_ha_data_internal(%rip), %xmm4 #913.14
        vandpd    %xmm10, %xmm5, %xmm0                          #925.163
        vfmadd213pd 2112+__svml_dexpm1_ha_data_internal(%rip), %xmm7, %xmm4 #913.14
        vorpd     2688+__svml_dexpm1_ha_data_internal(%rip), %xmm6, %xmm6 #917.19
        vandpd    %xmm6, %xmm9, %xmm2                           #918.13
        vmovupd   2752+__svml_dexpm1_ha_data_internal(%rip), %xmm6 #924.46
        vandpd    %xmm10, %xmm6, %xmm10                         #925.208
        vmulpd    %xmm7, %xmm7, %xmm7                           #915.13
        vsubpd    %xmm2, %xmm9, %xmm9                           #919.13
        vcmplepd  %xmm10, %xmm0, %xmm0                          #925.256
        vaddpd    %xmm6, %xmm5, %xmm13                          #926.14
        vmulpd    %xmm15, %xmm14, %xmm3                         #908.13
        vfmadd213pd %xmm1, %xmm7, %xmm4                         #920.12
        vmulpd    %xmm2, %xmm5, %xmm1                           #922.15
        vsubpd    %xmm8, %xmm9, %xmm8                           #921.13
        vfmadd213pd %xmm8, %xmm7, %xmm4                         #923.12
        vmovapd   %xmm5, %xmm7                                  #925.300
        vmovapd   %xmm6, %xmm8                                  #925.352
        blendvpd  %xmm0, %xmm6, %xmm7                           #925.300
        blendvpd  %xmm0, %xmm5, %xmm8                           #925.352
        vsubpd    %xmm13, %xmm7, %xmm9                          #927.14
        vaddpd    %xmm13, %xmm1, %xmm0                          #930.15
        vaddpd    %xmm4, %xmm2, %xmm2                           #934.13
        vaddpd    %xmm9, %xmm8, %xmm10                          #928.14
        vsubpd    %xmm0, %xmm13, %xmm14                         #931.14
        vaddpd    %xmm10, %xmm3, %xmm15                         #929.14
        vaddpd    %xmm14, %xmm1, %xmm1                          #932.17
        vfmadd213pd %xmm15, %xmm2, %xmm3                        #935.14
        vfmadd213pd %xmm1, %xmm4, %xmm5                         #933.14
        vaddpd    %xmm3, %xmm5, %xmm3                           #936.14
        vaddpd    %xmm3, %xmm0, %xmm4                           #937.15
        vorpd     %xmm11, %xmm4, %xmm0                          #938.14
        testl     %edx, %edx                                    #940.52
        jne       ..B3.3        # Prob 5%                       #940.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm12
..B3.2:                         # Preds ..B3.3 ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #943.12
        popq      %rbp                                          #943.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #943.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm12, 64(%rsp)                              #940.197
        vmovupd   %xmm0, 128(%rsp)                              #940.270
        je        ..B3.2        # Prob 95%                      #940.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #940.454
        movq      %r12, 8(%rsp)                                 #940.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #940.454
        movq      %r13, (%rsp)                                  #940.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #940.454
                                # LOE rbx r12 r14 r15 r13d
..B3.7:                         # Preds ..B3.8 ..B3.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #940.517
        jc        ..B3.10       # Prob 5%                       #940.517
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #940.470
        cmpl      $2, %r12d                                     #940.465
        jl        ..B3.7        # Prob 82%                      #940.465
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #940.675
        jmp       ..B3.2        # Prob 100%                     #940.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #940.546
        lea       128(%rsp,%r12,8), %rsi                        #940.546
..___tag_value___svml_expm12_ha_l9.61:
#       __svml_dexpm1_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexpm1_ha_cout_rare_internal           #940.546
..___tag_value___svml_expm12_ha_l9.62:
        jmp       ..B3.8        # Prob 100%                     #940.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expm12_ha_l9,@function
	.size	__svml_expm12_ha_l9,.-__svml_expm12_ha_l9
..LN__svml_expm12_ha_l9.2:
	.data
# -- End  __svml_expm12_ha_l9
	.text
.L_2__routine_start___svml_expm11_ha_ex_3:
# -- Begin  __svml_expm11_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm11_ha_ex
# --- __svml_expm11_ha_ex(__m128d)
__svml_expm11_ha_ex:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #948.1
        .byte     15                                            #1059.546
        .byte     30                                            #1059.546
        .byte     250                                           #1059.546
	.cfi_startproc
..___tag_value___svml_expm11_ha_ex.64:
..L65:
                                                         #948.1
        pushq     %rbp                                          #948.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #948.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #948.1
        subq      $128, %rsp                                    #948.1
        movaps    %xmm0, %xmm9                                  #948.1
        movaps    %xmm9, %xmm15                                 #1001.12
        movaps    %xmm9, %xmm8                                  #1005.14
        mulsd     2304+__svml_dexpm1_ha_data_internal(%rip), %xmm15 #1001.12
        lea       __svml_dexpm1_ha_data_internal(%rip), %rdx    #1021.17
        movups    .L_2il0floatpacket.33(%rip), %xmm10           #1002.107
        addpd     %xmm10, %xmm15                                #1002.297
        subpd     %xmm10, %xmm15                                #1002.342
        movaps    %xmm15, %xmm11                                #1005.34
        movaps    %xmm15, %xmm10                                #1006.14
        mulsd     2368+__svml_dexpm1_ha_data_internal(%rip), %xmm11 #1005.34
        addsd     2496+__svml_dexpm1_ha_data_internal(%rip), %xmm15 #1017.16
        mulsd     2432+__svml_dexpm1_ha_data_internal(%rip), %xmm10 #1006.14
        subsd     %xmm11, %xmm8                                 #1005.14
        movaps    %xmm8, %xmm7                                  #1007.12
        movaps    %xmm8, %xmm1                                  #1008.20
        movsd     2816+__svml_dexpm1_ha_data_internal(%rip), %xmm0 #1009.18
        subsd     %xmm10, %xmm7                                 #1007.12
        cmpeqsd   %xmm9, %xmm1                                  #1008.20
        movaps    %xmm0, %xmm12                                 #1011.13
        movaps    %xmm0, %xmm6                                  #1012.14
        andps     %xmm9, %xmm12                                 #1011.13
        andnps    %xmm9, %xmm6                                  #1012.14
        movsd     2048+__svml_dexpm1_ha_data_internal(%rip), %xmm3 #1032.26
        mulsd     %xmm7, %xmm3                                  #1032.26
        cmpnlesd  2880+__svml_dexpm1_ha_data_internal(%rip), %xmm12 #1013.18
        movmskpd  %xmm12, %ecx                                  #1015.44
        addsd     2112+__svml_dexpm1_ha_data_internal(%rip), %xmm3 #1032.14
        movsd     2176+__svml_dexpm1_ha_data_internal(%rip), %xmm12 #1033.26
        mulsd     %xmm7, %xmm12                                 #1033.26
        mulsd     %xmm7, %xmm7                                  #1034.13
        addsd     2240+__svml_dexpm1_ha_data_internal(%rip), %xmm12 #1033.14
        mulsd     %xmm7, %xmm3                                  #1039.24
        movsd     2688+__svml_dexpm1_ha_data_internal(%rip), %xmm2 #1036.19
        addsd     %xmm12, %xmm3                                 #1039.12
        orps      %xmm1, %xmm2                                  #1036.19
        andps     %xmm8, %xmm2                                  #1037.13
        movq      2560+__svml_dexpm1_ha_data_internal(%rip), %xmm13 #1020.16
        subsd     %xmm2, %xmm8                                  #1038.13
        mulsd     %xmm7, %xmm3                                  #1042.24
        subsd     %xmm10, %xmm8                                 #1040.13
        pand      %xmm15, %xmm13                                #1020.16
        addsd     %xmm8, %xmm3                                  #1042.12
        movd      %xmm13, %eax                                  #1021.110
        movq      2624+__svml_dexpm1_ha_data_internal(%rip), %xmm14 #1022.18
        movaps    %xmm0, %xmm8                                  #1044.163
        movslq    %eax, %rax                                    #1021.58
        pand      %xmm14, %xmm15                                #1023.15
        psllq     $41, %xmm15                                   #1024.15
        movsd     2752+__svml_dexpm1_ha_data_internal(%rip), %xmm11 #1043.15
        andps     %xmm11, %xmm0                                 #1044.208
        movaps    %xmm11, %xmm7                                 #1044.346
        movups    (%rdx,%rax), %xmm4                            #1021.58
        orps      %xmm15, %xmm4                                 #1026.13
        andps     %xmm4, %xmm8                                  #1044.163
        movaps    %xmm4, %xmm14                                 #1045.14
        movaps    %xmm4, %xmm1                                  #1041.15
        cmplesd   %xmm0, %xmm8                                  #1044.256
        mulsd     %xmm2, %xmm1                                  #1041.15
        addsd     %xmm11, %xmm14                                #1045.14
        movaps    %xmm8, %xmm10                                 #1044.311
        andps     %xmm8, %xmm7                                  #1044.346
        andnps    %xmm4, %xmm10                                 #1044.311
        movaps    %xmm14, %xmm0                                 #1049.15
        orps      %xmm7, %xmm10                                 #1044.300
        movaps    %xmm8, %xmm13                                 #1044.400
        movsd     8(%rdx,%rax), %xmm5                           #1021.58
        andnps    %xmm11, %xmm13                                #1044.400
        andps     %xmm4, %xmm8                                  #1044.437
        subsd     %xmm14, %xmm10                                #1046.14
        mulsd     %xmm3, %xmm4                                  #1052.26
        addsd     %xmm1, %xmm0                                  #1049.15
        addsd     %xmm2, %xmm3                                  #1053.13
        subsd     %xmm0, %xmm14                                 #1050.14
        unpcklpd  %xmm5, %xmm5                                  #1021.153
        orps      %xmm8, %xmm13                                 #1044.389
        mulsd     %xmm15, %xmm5                                 #1027.13
        addsd     %xmm13, %xmm10                                #1047.14
        addsd     %xmm14, %xmm1                                 #1051.17
        addsd     %xmm5, %xmm10                                 #1048.14
        mulsd     %xmm3, %xmm5                                  #1054.26
        addsd     %xmm1, %xmm4                                  #1052.14
        addsd     %xmm10, %xmm5                                 #1054.14
        addsd     %xmm5, %xmm4                                  #1055.14
        addsd     %xmm4, %xmm0                                  #1056.15
        orps      %xmm6, %xmm0                                  #1057.14
        andl      $1, %ecx                                      #1015.94
        jne       ..B4.3        # Prob 5%                       #1059.52
                                # LOE rbx r12 r13 r14 r15 ecx xmm0 xmm9
..B4.2:                         # Preds ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1062.12
        popq      %rbp                                          #1062.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1062.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm9, (%rsp)                                 #1059.160
        movsd     %xmm0, 64(%rsp)                               #1059.233
        jne       ..B4.6        # Prob 5%                       #1059.374
                                # LOE rbx r12 r13 r14 r15
..B4.4:                         # Preds ..B4.6 ..B4.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #1059.627
        movq      %rbp, %rsp                                    #1059.627
        popq      %rbp                                          #1059.627
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1059.627
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1059.546
        lea       64(%rsp), %rsi                                #1059.546
..___tag_value___svml_expm11_ha_ex.77:
#       __svml_dexpm1_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexpm1_ha_cout_rare_internal           #1059.546
..___tag_value___svml_expm11_ha_ex.78:
        jmp       ..B4.4        # Prob 100%                     #1059.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_expm11_ha_ex,@function
	.size	__svml_expm11_ha_ex,.-__svml_expm11_ha_ex
..LN__svml_expm11_ha_ex.3:
	.data
# -- End  __svml_expm11_ha_ex
	.text
.L_2__routine_start___svml_expm12_ha_ex_4:
# -- Begin  __svml_expm12_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm12_ha_ex
# --- __svml_expm12_ha_ex(__m128d)
__svml_expm12_ha_ex:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1067.1
        .byte     15                                            #1178.546
        .byte     30                                            #1178.546
        .byte     250                                           #1178.546
	.cfi_startproc
..___tag_value___svml_expm12_ha_ex.80:
..L81:
                                                         #1067.1
        pushq     %rbp                                          #1067.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1067.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1067.1
        subq      $192, %rsp                                    #1067.1
        movaps    %xmm0, %xmm9                                  #1067.1
        movups    2304+__svml_dexpm1_ha_data_internal(%rip), %xmm13 #1120.12
        movaps    %xmm9, %xmm8                                  #1124.14
        mulpd     %xmm9, %xmm13                                 #1120.12
        movups    .L_2il0floatpacket.33(%rip), %xmm10           #1121.107
        lea       __svml_dexpm1_ha_data_internal(%rip), %rsi    #1140.67
        addpd     %xmm10, %xmm13                                #1121.297
        subpd     %xmm10, %xmm13                                #1121.342
        movups    2368+__svml_dexpm1_ha_data_internal(%rip), %xmm11 #1122.45
        mulpd     %xmm13, %xmm11                                #1124.34
        movups    2432+__svml_dexpm1_ha_data_internal(%rip), %xmm0 #1125.14
        mulpd     %xmm13, %xmm0                                 #1125.14
        subpd     %xmm11, %xmm8                                 #1124.14
        movaps    %xmm8, %xmm4                                  #1126.12
        movaps    %xmm8, %xmm6                                  #1127.20
        subpd     %xmm0, %xmm4                                  #1126.12
        cmpeqpd   %xmm9, %xmm6                                  #1127.20
        movups    2816+__svml_dexpm1_ha_data_internal(%rip), %xmm5 #1128.49
        movaps    %xmm5, %xmm12                                 #1130.13
        movaps    %xmm5, %xmm7                                  #1131.14
        andps     %xmm9, %xmm12                                 #1130.13
        andnps    %xmm9, %xmm7                                  #1131.14
        cmpnlepd  2880+__svml_dexpm1_ha_data_internal(%rip), %xmm12 #1132.18
        movups    2048+__svml_dexpm1_ha_data_internal(%rip), %xmm11 #1151.26
        mulpd     %xmm4, %xmm11                                 #1151.26
        movmskpd  %xmm12, %edx                                  #1134.44
        movups    2176+__svml_dexpm1_ha_data_internal(%rip), %xmm12 #1152.26
        movups    2496+__svml_dexpm1_ha_data_internal(%rip), %xmm10 #1136.16
        mulpd     %xmm4, %xmm12                                 #1152.26
        mulpd     %xmm4, %xmm4                                  #1153.13
        addpd     2112+__svml_dexpm1_ha_data_internal(%rip), %xmm11 #1151.14
        addpd     2240+__svml_dexpm1_ha_data_internal(%rip), %xmm12 #1152.14
        addpd     %xmm13, %xmm10                                #1136.16
        mulpd     %xmm4, %xmm11                                 #1158.24
        addpd     %xmm11, %xmm12                                #1158.12
        orps      2688+__svml_dexpm1_ha_data_internal(%rip), %xmm6 #1155.19
        movdqu    2560+__svml_dexpm1_ha_data_internal(%rip), %xmm14 #1139.16
        andps     %xmm8, %xmm6                                  #1156.13
        subpd     %xmm6, %xmm8                                  #1157.13
        mulpd     %xmm12, %xmm4                                 #1161.24
        subpd     %xmm0, %xmm8                                  #1159.13
        pand      %xmm10, %xmm14                                #1139.16
        movd      %xmm14, %eax                                  #1140.137
        pshufd    $2, %xmm14, %xmm15                            #1140.317
        movdqu    2624+__svml_dexpm1_ha_data_internal(%rip), %xmm13 #1142.15
        movd      %xmm15, %ecx                                  #1140.299
        pand      %xmm13, %xmm10                                #1142.15
        psllq     $41, %xmm10                                   #1143.15
        movups    2752+__svml_dexpm1_ha_data_internal(%rip), %xmm14 #1162.46
        movslq    %eax, %rax                                    #1140.67
        movaps    %xmm14, %xmm0                                 #1163.346
        movslq    %ecx, %rcx                                    #1140.229
        addpd     %xmm4, %xmm8                                  #1161.12
        movups    (%rsi,%rax), %xmm3                            #1140.67
        movaps    %xmm5, %xmm4                                  #1163.163
        movups    (%rsi,%rcx), %xmm1                            #1140.229
        movaps    %xmm3, %xmm2                                  #1140.383
        unpcklpd  %xmm1, %xmm2                                  #1140.383
        andps     %xmm14, %xmm5                                 #1163.208
        orps      %xmm10, %xmm2                                 #1145.13
        andps     %xmm2, %xmm4                                  #1163.163
        cmplepd   %xmm5, %xmm4                                  #1163.256
        unpckhpd  %xmm1, %xmm3                                  #1140.424
        movaps    %xmm2, %xmm1                                  #1160.15
        mulpd     %xmm10, %xmm3                                 #1146.13
        mulpd     %xmm6, %xmm1                                  #1160.15
        addpd     %xmm8, %xmm6                                  #1172.13
        movaps    %xmm4, %xmm10                                 #1163.400
        movaps    %xmm4, %xmm5                                  #1163.311
        andnps    %xmm14, %xmm10                                #1163.400
        andnps    %xmm2, %xmm5                                  #1163.311
        addpd     %xmm2, %xmm14                                 #1164.14
        andps     %xmm4, %xmm0                                  #1163.346
        andps     %xmm2, %xmm4                                  #1163.437
        orps      %xmm0, %xmm5                                  #1163.300
        movaps    %xmm14, %xmm0                                 #1168.15
        subpd     %xmm14, %xmm5                                 #1165.14
        addpd     %xmm1, %xmm0                                  #1168.15
        mulpd     %xmm8, %xmm2                                  #1171.26
        subpd     %xmm0, %xmm14                                 #1169.14
        orps      %xmm4, %xmm10                                 #1163.389
        addpd     %xmm5, %xmm10                                 #1166.14
        addpd     %xmm14, %xmm1                                 #1170.17
        addpd     %xmm3, %xmm10                                 #1167.14
        addpd     %xmm2, %xmm1                                  #1171.14
        mulpd     %xmm6, %xmm3                                  #1173.26
        addpd     %xmm3, %xmm10                                 #1173.14
        addpd     %xmm10, %xmm1                                 #1174.14
        addpd     %xmm1, %xmm0                                  #1175.15
        orps      %xmm7, %xmm0                                  #1176.14
        testl     %edx, %edx                                    #1178.52
        jne       ..B5.3        # Prob 5%                       #1178.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm9
..B5.2:                         # Preds ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1181.12
        popq      %rbp                                          #1181.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1181.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm9, 64(%rsp)                               #1178.197
        movups    %xmm0, 128(%rsp)                              #1178.270
                                # LOE rbx r12 r13 r14 r15 edx
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1178.454
        movq      %r12, 8(%rsp)                                 #1178.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1178.454
        movq      %r13, (%rsp)                                  #1178.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1178.454
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1178.517
        jc        ..B5.10       # Prob 5%                       #1178.517
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1178.470
        cmpl      $2, %r12d                                     #1178.465
        jl        ..B5.7        # Prob 82%                      #1178.465
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1178.675
        jmp       ..B5.2        # Prob 100%                     #1178.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1178.546
        lea       128(%rsp,%r12,8), %rsi                        #1178.546
..___tag_value___svml_expm12_ha_ex.98:
#       __svml_dexpm1_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexpm1_ha_cout_rare_internal           #1178.546
..___tag_value___svml_expm12_ha_ex.99:
        jmp       ..B5.8        # Prob 100%                     #1178.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expm12_ha_ex,@function
	.size	__svml_expm12_ha_ex,.-__svml_expm12_ha_ex
..LN__svml_expm12_ha_ex.4:
	.data
# -- End  __svml_expm12_ha_ex
	.text
.L_2__routine_start___svml_expm11_ha_l9_5:
# -- Begin  __svml_expm11_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm11_ha_l9
# --- __svml_expm11_ha_l9(__m128d)
__svml_expm11_ha_l9:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1186.1
        .byte     15                                            #1297.546
        .byte     30                                            #1297.546
        .byte     250                                           #1297.546
	.cfi_startproc
..___tag_value___svml_expm11_ha_l9.101:
..L102:
                                                        #1186.1
        pushq     %rbp                                          #1186.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1186.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1186.1
        subq      $128, %rsp                                    #1186.1
        vmovapd   %xmm0, %xmm13                                 #1186.1
        vmulsd    2304+__svml_dexpm1_ha_data_internal(%rip), %xmm13, %xmm14 #1239.12
        lea       __svml_dexpm1_ha_data_internal(%rip), %rdx    #1259.17
        vroundsd  $0, %xmm14, %xmm14, %xmm4                     #1240.12
        vmovq     2560+__svml_dexpm1_ha_data_internal(%rip), %xmm5 #1257.22
        vmovapd   %xmm4, %xmm9                                  #1243.14
        vfnmadd132sd 2368+__svml_dexpm1_ha_data_internal(%rip), %xmm13, %xmm9 #1243.14
        vmulsd    2432+__svml_dexpm1_ha_data_internal(%rip), %xmm4, %xmm10 #1244.14
        vaddsd    2496+__svml_dexpm1_ha_data_internal(%rip), %xmm4, %xmm0 #1255.16
        vcmpeqsd  %xmm13, %xmm9, %xmm8                          #1246.20
        vsubsd    %xmm10, %xmm9, %xmm7                          #1245.12
        vpand     %xmm5, %xmm0, %xmm2                           #1258.18
        vmovd     %xmm2, %eax                                   #1259.110
        vmovsd    2816+__svml_dexpm1_ha_data_internal(%rip), %xmm11 #1247.18
        vandpd    %xmm11, %xmm13, %xmm15                        #1249.13
        vandnpd   %xmm13, %xmm11, %xmm12                        #1250.14
        movslq    %eax, %rax                                    #1259.58
        vcmpnlesd 2880+__svml_dexpm1_ha_data_internal(%rip), %xmm15, %xmm6 #1251.18
        vmovsd    8(%rdx,%rax), %xmm1                           #1259.58
        vunpcklpd %xmm1, %xmm1, %xmm15                          #1259.155
        vmovsd    2048+__svml_dexpm1_ha_data_internal(%rip), %xmm5 #1270.14
        vmovsd    2176+__svml_dexpm1_ha_data_internal(%rip), %xmm1 #1271.14
        vmovq     2624+__svml_dexpm1_ha_data_internal(%rip), %xmm3 #1260.20
        vfmadd213sd 2112+__svml_dexpm1_ha_data_internal(%rip), %xmm7, %xmm5 #1270.14
        vpand     %xmm3, %xmm0, %xmm14                          #1261.17
        vfmadd213sd 2240+__svml_dexpm1_ha_data_internal(%rip), %xmm7, %xmm1 #1271.14
        vpsllq    $41, %xmm14, %xmm4                            #1262.17
        vmulsd    %xmm7, %xmm7, %xmm3                           #1272.13
        vmovmskpd %xmm6, %ecx                                   #1253.44
        vmovsd    2688+__svml_dexpm1_ha_data_internal(%rip), %xmm7 #1273.19
        vorpd     %xmm8, %xmm7, %xmm8                           #1274.19
        vandpd    %xmm8, %xmm9, %xmm2                           #1275.13
        vfmadd213sd %xmm1, %xmm3, %xmm5                         #1277.12
        vsubsd    %xmm2, %xmm9, %xmm9                           #1276.13
        vorpd     (%rdx,%rax), %xmm4, %xmm6                     #1264.13
        vsubsd    %xmm10, %xmm9, %xmm10                         #1278.13
        vmulsd    %xmm2, %xmm6, %xmm1                           #1279.15
        vmulsd    %xmm4, %xmm15, %xmm4                          #1265.13
        vfmadd213sd %xmm10, %xmm3, %xmm5                        #1280.12
        vmovsd    2752+__svml_dexpm1_ha_data_internal(%rip), %xmm3 #1281.15
        vandpd    %xmm11, %xmm6, %xmm0                          #1282.163
        vandpd    %xmm11, %xmm3, %xmm11                         #1282.208
        vmovapd   %xmm6, %xmm7                                  #1282.300
        vmovapd   %xmm3, %xmm9                                  #1282.352
        vcmplesd  %xmm11, %xmm0, %xmm0                          #1282.256
        vaddsd    %xmm3, %xmm6, %xmm11                          #1283.14
        vaddsd    %xmm2, %xmm5, %xmm2                           #1291.13
        blendvpd  %xmm0, %xmm3, %xmm7                           #1282.300
        blendvpd  %xmm0, %xmm6, %xmm9                           #1282.352
        vsubsd    %xmm11, %xmm7, %xmm8                          #1284.14
        vaddsd    %xmm1, %xmm11, %xmm0                          #1287.15
        vaddsd    %xmm9, %xmm8, %xmm10                          #1285.14
        vsubsd    %xmm0, %xmm11, %xmm14                         #1288.14
        vaddsd    %xmm4, %xmm10, %xmm15                         #1286.14
        vaddsd    %xmm14, %xmm1, %xmm1                          #1289.17
        vfmadd213sd %xmm15, %xmm2, %xmm4                        #1292.14
        vfmadd213sd %xmm1, %xmm5, %xmm6                         #1290.14
        vaddsd    %xmm4, %xmm6, %xmm4                           #1293.14
        vaddsd    %xmm4, %xmm0, %xmm5                           #1294.15
        vorpd     %xmm12, %xmm5, %xmm0                          #1295.14
        andl      $1, %ecx                                      #1253.94
        jne       ..B6.3        # Prob 5%                       #1297.52
                                # LOE rbx r12 r13 r14 r15 ecx xmm0 xmm13
..B6.2:                         # Preds ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1300.12
        popq      %rbp                                          #1300.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1300.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm13, (%rsp)                                #1297.160
        vmovsd    %xmm0, 64(%rsp)                               #1297.233
        jne       ..B6.5        # Prob 5%                       #1297.374
                                # LOE rbx r12 r13 r14 r15 ecx
..B6.4:                         # Preds ..B6.6 ..B6.5 ..B6.3
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm0                               #1297.627
        movq      %rbp, %rsp                                    #1297.627
        popq      %rbp                                          #1297.627
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1297.627
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B6.5:                         # Preds ..B6.3
                                # Execution count [2.50e-03]: Infreq
        je        ..B6.4        # Prob 95%                      #1297.517
                                # LOE rbx r12 r13 r14 r15
..B6.6:                         # Preds ..B6.5
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1297.546
        lea       64(%rsp), %rsi                                #1297.546
..___tag_value___svml_expm11_ha_l9.114:
#       __svml_dexpm1_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexpm1_ha_cout_rare_internal           #1297.546
..___tag_value___svml_expm11_ha_l9.115:
        jmp       ..B6.4        # Prob 100%                     #1297.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_expm11_ha_l9,@function
	.size	__svml_expm11_ha_l9,.-__svml_expm11_ha_l9
..LN__svml_expm11_ha_l9.5:
	.data
# -- End  __svml_expm11_ha_l9
	.text
.L_2__routine_start___svml_expm14_ha_e9_6:
# -- Begin  __svml_expm14_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm14_ha_e9
# --- __svml_expm14_ha_e9(__m256d)
__svml_expm14_ha_e9:
# parameter 1: %ymm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1305.1
        .byte     15                                            #1416.552
        .byte     30                                            #1416.552
        .byte     250                                           #1416.552
	.cfi_startproc
..___tag_value___svml_expm14_ha_e9.117:
..L118:
                                                        #1305.1
        pushq     %rbp                                          #1305.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1305.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1305.1
        subq      $192, %rsp                                    #1305.1
        lea       __svml_dexpm1_ha_data_internal(%rip), %r8     #1378.68
        vmovapd   %ymm0, %ymm6                                  #1305.1
        vmulpd    2304+__svml_dexpm1_ha_data_internal(%rip), %ymm6, %ymm5 #1358.12
        vmovupd   2560+__svml_dexpm1_ha_data_internal(%rip), %xmm11 #1376.26
        vroundpd  $0, %ymm5, %ymm12                             #1359.12
        vmulpd    2368+__svml_dexpm1_ha_data_internal(%rip), %ymm12, %ymm4 #1362.37
        vmulpd    2432+__svml_dexpm1_ha_data_internal(%rip), %ymm12, %ymm2 #1363.14
        vaddpd    2496+__svml_dexpm1_ha_data_internal(%rip), %ymm12, %ymm9 #1374.16
        vsubpd    %ymm4, %ymm6, %ymm3                           #1362.14
        vmovupd   2816+__svml_dexpm1_ha_data_internal(%rip), %ymm4 #1366.52
        vsubpd    %ymm2, %ymm3, %ymm0                           #1364.12
        vcmpeqpd  %ymm6, %ymm3, %ymm1                           #1365.20
        vandpd    %ymm4, %ymm6, %ymm10                          #1368.13
        vcmpnle_uqpd 2880+__svml_dexpm1_ha_data_internal(%rip), %ymm10, %ymm15 #1370.18
        vorpd     2688+__svml_dexpm1_ha_data_internal(%rip), %ymm1, %ymm1 #1393.19
        vandnpd   %ymm6, %ymm4, %ymm5                           #1369.14
        vandps    %xmm11, %xmm9, %xmm10                         #1377.22
        vextractf128 $1, %ymm15, %xmm14                         #1371.117
        vextractf128 $1, %ymm9, %xmm13                          #1375.115
        vmovd     %xmm10, %edx                                  #1378.138
        vshufps   $221, %xmm14, %xmm15, %xmm7                   #1372.61
        vandps    %xmm11, %xmm13, %xmm15                        #1377.93
        vmovd     %xmm15, %esi                                  #1378.483
        vmovmskps %xmm7, %eax                                   #1372.44
        vpextrd   $2, %xmm10, %ecx                              #1378.309
        vpextrd   $2, %xmm15, %edi                              #1378.654
        movslq    %edx, %rdx                                    #1378.68
        movslq    %ecx, %rcx                                    #1378.239
        movslq    %esi, %rsi                                    #1378.413
        movslq    %edi, %rdi                                    #1378.584
        vmovupd   (%r8,%rdx), %xmm8                             #1378.68
        vmovupd   (%r8,%rcx), %xmm12                            #1378.239
        vmovupd   (%r8,%rsi), %xmm11                            #1378.413
        vmovupd   (%r8,%rdi), %xmm7                             #1378.584
        vunpcklpd %xmm12, %xmm8, %xmm14                         #1378.755
        vunpcklpd %xmm7, %xmm11, %xmm10                         #1378.788
        vunpckhpd %xmm12, %xmm8, %xmm8                          #1378.882
        vunpckhpd %xmm7, %xmm11, %xmm7                          #1378.915
        vinsertf128 $1, %xmm10, %ymm14, %ymm15                  #1378.709
        vmovupd   2624+__svml_dexpm1_ha_data_internal(%rip), %xmm10 #1379.24
        vandps    %xmm10, %xmm9, %xmm12                         #1380.21
        vandps    %xmm10, %xmm13, %xmm13                        #1380.89
        vpsllq    $41, %xmm12, %xmm11                           #1381.21
        vmulpd    %ymm0, %ymm0, %ymm10                          #1391.13
        vinsertf128 $1, %xmm7, %ymm8, %ymm14                    #1378.836
        vpsllq    $41, %xmm13, %xmm7                            #1381.77
        vinsertf128 $1, %xmm7, %ymm11, %ymm9                    #1382.16
        vmulpd    %ymm9, %ymm14, %ymm12                         #1384.13
        vmulpd    2048+__svml_dexpm1_ha_data_internal(%rip), %ymm0, %ymm14 #1389.29
        vaddpd    2112+__svml_dexpm1_ha_data_internal(%rip), %ymm14, %ymm7 #1389.14
        vorpd     %ymm9, %ymm15, %ymm13                         #1383.13
        vmulpd    2176+__svml_dexpm1_ha_data_internal(%rip), %ymm0, %ymm15 #1390.29
        vandpd    %ymm1, %ymm3, %ymm11                          #1394.13
        vsubpd    %ymm11, %ymm3, %ymm9                          #1395.13
        vaddpd    2240+__svml_dexpm1_ha_data_internal(%rip), %ymm15, %ymm8 #1390.14
        vmulpd    %ymm10, %ymm7, %ymm3                          #1396.27
        vsubpd    %ymm2, %ymm9, %ymm2                           #1397.13
        vmulpd    %ymm11, %ymm13, %ymm9                         #1398.15
        vaddpd    %ymm3, %ymm8, %ymm7                           #1396.12
        vmulpd    %ymm7, %ymm10, %ymm7                          #1399.27
        vaddpd    %ymm7, %ymm2, %ymm8                           #1399.12
        vmovupd   2752+__svml_dexpm1_ha_data_internal(%rip), %ymm2 #1400.49
        vandpd    %ymm4, %ymm13, %ymm0                          #1401.163
        vandpd    %ymm4, %ymm2, %ymm4                           #1401.211
        vcmple_oqpd %ymm4, %ymm0, %ymm1                         #1401.262
        vaddpd    %ymm2, %ymm13, %ymm0                          #1402.14
        vblendvpd %ymm1, %ymm2, %ymm13, %ymm3                   #1401.319
        vblendvpd %ymm1, %ymm13, %ymm2, %ymm10                  #1401.374
        vsubpd    %ymm0, %ymm3, %ymm14                          #1403.14
        vaddpd    %ymm0, %ymm9, %ymm7                           #1406.15
        vmulpd    %ymm8, %ymm13, %ymm13                         #1409.29
        vaddpd    %ymm8, %ymm11, %ymm8                          #1410.13
        vaddpd    %ymm14, %ymm10, %ymm15                        #1404.14
        vsubpd    %ymm7, %ymm0, %ymm1                           #1407.14
        vmulpd    %ymm8, %ymm12, %ymm11                         #1411.29
        vaddpd    %ymm15, %ymm12, %ymm2                         #1405.14
        vaddpd    %ymm1, %ymm9, %ymm9                           #1408.17
        vaddpd    %ymm11, %ymm2, %ymm12                         #1411.14
        vaddpd    %ymm13, %ymm9, %ymm0                          #1409.14
        vaddpd    %ymm12, %ymm0, %ymm0                          #1412.14
        vaddpd    %ymm0, %ymm7, %ymm1                           #1413.15
        vorpd     %ymm5, %ymm1, %ymm0                           #1414.14
        testl     %eax, %eax                                    #1416.52
        jne       ..B7.3        # Prob 5%                       #1416.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm6
..B7.2:                         # Preds ..B7.3 ..B7.9 ..B7.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1419.12
        popq      %rbp                                          #1419.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1419.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.3:                         # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm6, 64(%rsp)                               #1416.200
        vmovupd   %ymm0, 128(%rsp)                              #1416.276
        je        ..B7.2        # Prob 95%                      #1416.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B7.6:                         # Preds ..B7.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1416.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B7.13:                        # Preds ..B7.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B7.7:                         # Preds ..B7.8 ..B7.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1416.523
        jc        ..B7.10       # Prob 5%                       #1416.523
                                # LOE rbx r12 r14 r15 r13d
..B7.8:                         # Preds ..B7.10 ..B7.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1416.476
        cmpl      $4, %r12d                                     #1416.471
        jl        ..B7.7        # Prob 82%                      #1416.471
                                # LOE rbx r12 r14 r15 r13d
..B7.9:                         # Preds ..B7.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1416.684
        jmp       ..B7.2        # Prob 100%                     #1416.684
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B7.10:                        # Preds ..B7.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1416.552
        lea       128(%rsp,%r12,8), %rsi                        #1416.552
..___tag_value___svml_expm14_ha_e9.135:
#       __svml_dexpm1_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexpm1_ha_cout_rare_internal           #1416.552
..___tag_value___svml_expm14_ha_e9.136:
        jmp       ..B7.8        # Prob 100%                     #1416.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expm14_ha_e9,@function
	.size	__svml_expm14_ha_e9,.-__svml_expm14_ha_e9
..LN__svml_expm14_ha_e9.6:
	.data
# -- End  __svml_expm14_ha_e9
	.text
.L_2__routine_start___svml_expm18_ha_z0_7:
# -- Begin  __svml_expm18_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm18_ha_z0
# --- __svml_expm18_ha_z0(__m512d)
__svml_expm18_ha_z0:
# parameter 1: %zmm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1424.1
        .byte     15                                            #1536.759
        .byte     30                                            #1536.759
        .byte     250                                           #1536.759
	.cfi_startproc
..___tag_value___svml_expm18_ha_z0.138:
..L139:
                                                        #1424.1
        pushq     %rbp                                          #1424.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1424.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1424.1
        subq      $192, %rsp                                    #1424.1
        vmovups   256+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm9 #1479.53
        vmovups   320+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm8 #1480.57
        vmovups   512+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm10 #1488.53
        vmovups   576+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm11 #1489.53
        vmovups   640+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm13 #1497.56
        vmovups   384+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm7 #1482.59
        vmovups   __svml_dexpm1_ha_data_internal_avx512(%rip), %zmm14 #1493.296
        vmovups   128+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm4 #1494.297
        vmovaps   %zmm0, %zmm6                                  #1424.1
        vfmadd213pd {rn-sae}, %zmm8, %zmm6, %zmm9               #1481.24
        vmovups   704+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm0 #1500.55
        vcmppd    $21, {sae}, %zmm7, %zmm6, %k0                 #1483.27
        vmovups   768+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm7 #1503.61
        vsubpd    {rn-sae}, %zmm8, %zmm9, %zmm12                #1487.22
        vpermt2pd 64+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm9, %zmm14 #1493.296
        vpermt2pd 192+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm9, %zmm4 #1494.297
        vandpd    448+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm6, %zmm5 #1486.21
        vfnmadd213pd {rn-sae}, %zmm6, %zmm12, %zmm10            #1490.21
        vmovups   896+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm8 #1505.61
        vmovups   1024+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm9 #1507.61
        vmaxpd    {sae}, %zmm13, %zmm12, %zmm15                 #1498.22
        kmovw     %k0, %esi                                     #1484.48
        vscalefpd {rn-sae}, %zmm15, %zmm14, %zmm3               #1499.20
        vmovups   1216+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm15 #1519.53
        vmovaps   %zmm11, %zmm2                                 #1491.19
        vfnmadd213pd {rn-sae}, %zmm10, %zmm12, %zmm2            #1491.19
        vcmppd    $17, {sae}, %zmm15, %zmm3, %k1                #1521.25
        vsubpd    {rn-sae}, %zmm2, %zmm10, %zmm1                #1495.23
        vandpd    %zmm0, %zmm2, %zmm2                           #1501.19
        vmovups   960+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm10 #1511.21
        vfnmadd231pd {rn-sae}, %zmm12, %zmm11, %zmm1            #1496.20
        vmovups   1088+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm11 #1513.21
        vmovups   1152+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm12 #1509.61
        vfmadd231pd {rn-sae}, %zmm2, %zmm8, %zmm10              #1511.21
        vfmadd231pd {rn-sae}, %zmm2, %zmm9, %zmm11              #1513.21
        vandpd    %zmm0, %zmm1, %zmm13                          #1502.20
        vmulpd    {rn-sae}, %zmm2, %zmm2, %zmm0                 #1512.22
        vmovups   832+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm1 #1510.21
        vaddpd    {rn-sae}, %zmm4, %zmm13, %zmm14               #1517.22
        vfmadd231pd {rn-sae}, %zmm2, %zmm7, %zmm1               #1510.21
        vmovups   1280+__svml_dexpm1_ha_data_internal_avx512(%rip), %zmm7 #1520.54
        vfmadd213pd {rn-sae}, %zmm14, %zmm2, %zmm4              #1518.20
        vfmadd213pd {rn-sae}, %zmm10, %zmm0, %zmm1              #1514.22
        vblendmpd %zmm7, %zmm3, %zmm8{%k1}                      #1522.19
        vblendmpd %zmm3, %zmm7, %zmm10{%k1}                     #1523.19
        vfmadd213pd {rn-sae}, %zmm11, %zmm0, %zmm1              #1515.22
        vaddpd    {rn-sae}, %zmm7, %zmm3, %zmm11                #1524.23
        vfmadd213pd {rn-sae}, %zmm12, %zmm2, %zmm1              #1516.22
        vsubpd    {rn-sae}, %zmm11, %zmm8, %zmm9                #1525.23
        vfmadd213pd {rn-sae}, %zmm4, %zmm0, %zmm1               #1527.22
        vaddpd    {rn-sae}, %zmm10, %zmm9, %zmm12               #1526.23
        vmovaps   %zmm11, %zmm0                                 #1528.22
        vfmadd231pd {rn-sae}, %zmm3, %zmm2, %zmm0               #1528.22
        vsubpd    {rn-sae}, %zmm11, %zmm0, %zmm4                #1529.24
        vfmsub231pd {rn-sae}, %zmm3, %zmm2, %zmm4               #1530.22
        vfmadd213pd {rn-sae}, %zmm12, %zmm1, %zmm3              #1531.21
        vaddpd    {rn-sae}, %zmm4, %zmm3, %zmm3                 #1532.23
        vaddpd    {rn-sae}, %zmm3, %zmm0, %zmm0                 #1533.24
        vorpd     %zmm5, %zmm0, %zmm0                           #1534.21
        testl     %esi, %esi                                    #1536.52
        jne       ..B8.3        # Prob 5%                       #1536.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm6
..B8.2:                         # Preds ..B8.9 ..B8.7 ..B8.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1539.12
        popq      %rbp                                          #1539.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1539.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.3:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #1536.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm6
..B8.4:                         # Preds ..B8.3
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #1536.249
        movl      %edx, %eax                                    #1536.303
        orl       $8064, %eax                                   #1536.303
        cmpl      %eax, %edx                                    #1536.332
        je        ..B8.6        # Prob 78%                      #1536.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm6
..B8.5:                         # Preds ..B8.4
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #1536.344
        vldmxcsr  32(%rsp)                                      #1536.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm6
..B8.6:                         # Preds ..B8.5 ..B8.4
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm6, 64(%rsp)                               #1536.406
        vmovups   %zmm0, 128(%rsp)                              #1536.481
        testl     %esi, %esi                                    #1536.587
        jne       ..B8.11       # Prob 5%                       #1536.587
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B8.7:                         # Preds ..B8.14 ..B8.6
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #1536.932
        je        ..B8.2        # Prob 78%                      #1536.932
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B8.8:                         # Preds ..B8.7
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #1536.957
        movl      32(%rsp), %eax                                #1536.957
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B8.9:                         # Preds ..B8.8
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #1536.944
        orl       %edx, %eax                                    #1536.944
        movl      %eax, 32(%rsp)                                #1536.944
        vldmxcsr  32(%rsp)                                      #1536.944
        jmp       ..B8.2        # Prob 100%                     #1536.944
                                # LOE rbx r12 r13 r14 r15 zmm0
..B8.11:                        # Preds ..B8.6
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #1536.667
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B8.18:                        # Preds ..B8.11
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
..B8.12:                        # Preds ..B8.13 ..B8.18
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #1536.730
        jc        ..B8.15       # Prob 5%                       #1536.730
                                # LOE rbx r15 r12d r13d r14d
..B8.13:                        # Preds ..B8.15 ..B8.12
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #1536.683
        cmpl      $8, %ebx                                      #1536.678
        jl        ..B8.12       # Prob 82%                      #1536.678
                                # LOE rbx r15 r12d r13d r14d
..B8.14:                        # Preds ..B8.13
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
        vmovups   128(%rsp), %zmm0                              #1536.888
        jmp       ..B8.7        # Prob 100%                     #1536.888
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B8.15:                        # Preds ..B8.12
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,8), %rdi                         #1536.759
        lea       128(%rsp,%rbx,8), %rsi                        #1536.759
..___tag_value___svml_expm18_ha_z0.166:
#       __svml_dexpm1_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexpm1_ha_cout_rare_internal           #1536.759
..___tag_value___svml_expm18_ha_z0.167:
        jmp       ..B8.13       # Prob 100%                     #1536.759
        .align    16,0x90
                                # LOE r15 ebx r12d r13d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_expm18_ha_z0,@function
	.size	__svml_expm18_ha_z0,.-__svml_expm18_ha_z0
..LN__svml_expm18_ha_z0.7:
	.data
# -- End  __svml_expm18_ha_z0
	.text
.L_2__routine_start___svml_expm11_ha_e9_8:
# -- Begin  __svml_expm11_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expm11_ha_e9
# --- __svml_expm11_ha_e9(__m128d)
__svml_expm11_ha_e9:
# parameter 1: %xmm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1544.1
        .byte     15                                            #1655.546
        .byte     30                                            #1655.546
        .byte     250                                           #1655.546
	.cfi_startproc
..___tag_value___svml_expm11_ha_e9.169:
..L170:
                                                        #1544.1
        pushq     %rbp                                          #1544.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1544.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1544.1
        subq      $128, %rsp                                    #1544.1
        movaps    %xmm0, %xmm10                                 #1544.1
        movaps    %xmm10, %xmm15                                #1597.12
        movaps    %xmm10, %xmm9                                 #1601.14
        mulsd     2304+__svml_dexpm1_ha_data_internal(%rip), %xmm15 #1597.12
        lea       __svml_dexpm1_ha_data_internal(%rip), %rdx    #1617.17
        roundsd   $0, %xmm15, %xmm15                            #1598.12
        movaps    %xmm15, %xmm11                                #1601.34
        mulsd     2368+__svml_dexpm1_ha_data_internal(%rip), %xmm11 #1601.34
        movsd     2048+__svml_dexpm1_ha_data_internal(%rip), %xmm3 #1628.26
        subsd     %xmm11, %xmm9                                 #1601.14
        movaps    %xmm15, %xmm11                                #1602.14
        movaps    %xmm9, %xmm7                                  #1603.12
        mulsd     2432+__svml_dexpm1_ha_data_internal(%rip), %xmm11 #1602.14
        movaps    %xmm9, %xmm1                                  #1604.20
        addsd     2496+__svml_dexpm1_ha_data_internal(%rip), %xmm15 #1613.16
        cmpeqsd   %xmm10, %xmm1                                 #1604.20
        subsd     %xmm11, %xmm7                                 #1603.12
        mulsd     %xmm7, %xmm3                                  #1628.26
        movsd     2176+__svml_dexpm1_ha_data_internal(%rip), %xmm0 #1629.26
        mulsd     %xmm7, %xmm0                                  #1629.26
        addsd     2112+__svml_dexpm1_ha_data_internal(%rip), %xmm3 #1628.14
        mulsd     %xmm7, %xmm7                                  #1630.13
        addsd     2240+__svml_dexpm1_ha_data_internal(%rip), %xmm0 #1629.14
        mulsd     %xmm7, %xmm3                                  #1635.24
        movq      2560+__svml_dexpm1_ha_data_internal(%rip), %xmm13 #1616.22
        addsd     %xmm0, %xmm3                                  #1635.12
        pand      %xmm15, %xmm13                                #1616.22
        movd      %xmm13, %eax                                  #1617.110
        movsd     2816+__svml_dexpm1_ha_data_internal(%rip), %xmm6 #1605.18
        movsd     2688+__svml_dexpm1_ha_data_internal(%rip), %xmm2 #1632.19
        movaps    %xmm6, %xmm12                                 #1607.13
        orps      %xmm1, %xmm2                                  #1632.19
        andps     %xmm10, %xmm12                                #1607.13
        movslq    %eax, %rax                                    #1617.58
        andps     %xmm9, %xmm2                                  #1633.13
        movq      2624+__svml_dexpm1_ha_data_internal(%rip), %xmm14 #1618.24
        movaps    %xmm6, %xmm0                                  #1640.163
        pand      %xmm14, %xmm15                                #1619.21
        movaps    %xmm6, %xmm8                                  #1608.14
        psllq     $41, %xmm15                                   #1620.21
        andnps    %xmm10, %xmm8                                 #1608.14
        movups    (%rdx,%rax), %xmm5                            #1617.58
        subsd     %xmm2, %xmm9                                  #1634.13
        mulsd     %xmm7, %xmm3                                  #1638.24
        cmpnlesd  2880+__svml_dexpm1_ha_data_internal(%rip), %xmm12 #1609.18
        subsd     %xmm11, %xmm9                                 #1636.13
        movmskpd  %xmm12, %ecx                                  #1611.44
        movsd     2752+__svml_dexpm1_ha_data_internal(%rip), %xmm7 #1639.15
        orps      %xmm15, %xmm5                                 #1622.13
        andps     %xmm5, %xmm0                                  #1640.163
        andps     %xmm7, %xmm6                                  #1640.208
        movaps    %xmm5, %xmm12                                 #1641.14
        movaps    %xmm5, %xmm1                                  #1637.15
        mulsd     %xmm2, %xmm1                                  #1637.15
        cmplesd   %xmm6, %xmm0                                  #1640.256
        addsd     %xmm7, %xmm12                                 #1641.14
        addsd     %xmm9, %xmm3                                  #1638.12
        movaps    %xmm5, %xmm13                                 #1640.300
        movaps    %xmm7, %xmm9                                  #1640.352
        blendvpd  %xmm0, %xmm7, %xmm13                          #1640.300
        blendvpd  %xmm0, %xmm5, %xmm9                           #1640.352
        movaps    %xmm12, %xmm0                                 #1645.15
        movsd     8(%rdx,%rax), %xmm4                           #1617.58
        subsd     %xmm12, %xmm13                                #1642.14
        mulsd     %xmm3, %xmm5                                  #1648.26
        addsd     %xmm1, %xmm0                                  #1645.15
        addsd     %xmm2, %xmm3                                  #1649.13
        addsd     %xmm9, %xmm13                                 #1643.14
        subsd     %xmm0, %xmm12                                 #1646.14
        unpcklpd  %xmm4, %xmm4                                  #1617.155
        addsd     %xmm12, %xmm1                                 #1647.17
        mulsd     %xmm15, %xmm4                                 #1623.13
        addsd     %xmm1, %xmm5                                  #1648.14
        addsd     %xmm4, %xmm13                                 #1644.14
        mulsd     %xmm3, %xmm4                                  #1650.26
        addsd     %xmm13, %xmm4                                 #1650.14
        addsd     %xmm4, %xmm5                                  #1651.14
        addsd     %xmm5, %xmm0                                  #1652.15
        orps      %xmm8, %xmm0                                  #1653.14
        andl      $1, %ecx                                      #1611.94
        jne       ..B9.3        # Prob 5%                       #1655.52
                                # LOE rbx r12 r13 r14 r15 ecx xmm0 xmm10
..B9.2:                         # Preds ..B9.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1658.12
        popq      %rbp                                          #1658.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1658.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.3:                         # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm10, (%rsp)                                #1655.160
        movsd     %xmm0, 64(%rsp)                               #1655.233
        jne       ..B9.6        # Prob 5%                       #1655.374
                                # LOE rbx r12 r13 r14 r15
..B9.4:                         # Preds ..B9.6 ..B9.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #1655.627
        movq      %rbp, %rsp                                    #1655.627
        popq      %rbp                                          #1655.627
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1655.627
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B9.6:                         # Preds ..B9.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1655.546
        lea       64(%rsp), %rsi                                #1655.546
..___tag_value___svml_expm11_ha_e9.182:
#       __svml_dexpm1_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexpm1_ha_cout_rare_internal           #1655.546
..___tag_value___svml_expm11_ha_e9.183:
        jmp       ..B9.4        # Prob 100%                     #1655.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_expm11_ha_e9,@function
	.size	__svml_expm11_ha_e9,.-__svml_expm11_ha_e9
..LN__svml_expm11_ha_e9.8:
	.data
# -- End  __svml_expm11_ha_e9
	.text
.L_2__routine_start___svml_dexpm1_ha_cout_rare_internal_9:
# -- Begin  __svml_dexpm1_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dexpm1_ha_cout_rare_internal
	.globl __svml_dexpm1_ha_cout_rare_internal
# --- __svml_dexpm1_ha_cout_rare_internal(const double *, double *)
__svml_dexpm1_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #718.1
        .byte     15                                            #817.15
        .byte     30                                            #817.15
        .byte     250                                           #817.15
	.cfi_startproc
..___tag_value___svml_dexpm1_ha_cout_rare_internal.185:
..L186:
                                                        #718.1
        xorl      %eax, %eax                                    #719.14
        movsd     (%rdi), %xmm6                                 #726.10
        pxor      %xmm0, %xmm0                                  #726.5
        movzwl    6(%rdi), %edx                                 #726.10
        comisd    %xmm6, %xmm0                                  #726.13
        ja        ..B10.18      # Prob 28%                      #726.13
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax edx xmm6
..B10.2:                        # Preds ..B10.1
                                # Execution count [7.20e-01]
        andl      $32752, %edx                                  #729.31
        shrl      $4, %edx                                      #729.31
        movsd     %xmm6, -8(%rsp)                               #728.5
        cmpl      $2047, %edx                                   #729.56
        je        ..B10.19      # Prob 16%                      #729.56
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax edx xmm6
..B10.3:                        # Preds ..B10.2
                                # Execution count [6.05e-01]
        cmpl      $970, %edx                                    #731.58
        jle       ..B10.16      # Prob 50%                      #731.58
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm6
..B10.4:                        # Preds ..B10.3
                                # Execution count [3.02e-01]
        movsd     1080+_imldExpHATab(%rip), %xmm0               #733.42
        comisd    %xmm6, %xmm0                                  #733.42
        jb        ..B10.15      # Prob 50%                      #733.42
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm6
..B10.5:                        # Preds ..B10.4
                                # Execution count [1.51e-01]
        comisd    1096+_imldExpHATab(%rip), %xmm6               #735.46
        jb        ..B10.14      # Prob 50%                      #735.46
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm6
..B10.6:                        # Preds ..B10.5
                                # Execution count [7.56e-02]
        movsd     1024+_imldExpHATab(%rip), %xmm0               #737.49
        movaps    %xmm6, %xmm5                                  #742.41
        mulsd     %xmm6, %xmm0                                  #737.49
        lea       _imldExpHATab(%rip), %r10                     #750.98
        movsd     %xmm0, -24(%rsp)                              #737.21
        movsd     -24(%rsp), %xmm1                              #738.35
        movq      1136+_imldExpHATab(%rip), %rdx                #727.5
        movq      %rdx, -8(%rsp)                                #727.5
        addsd     1032+_imldExpHATab(%rip), %xmm1               #738.56
        movsd     %xmm1, -16(%rsp)                              #738.21
        movsd     -16(%rsp), %xmm2                              #739.34
        movl      -16(%rsp), %r8d                               #746.46
        movl      %r8d, %ecx                                    #748.30
        andl      $63, %r8d                                     #747.29
        subsd     1032+_imldExpHATab(%rip), %xmm2               #739.56
        movsd     %xmm2, -24(%rsp)                              #739.21
        lea       1(%r8,%r8), %r9d                              #750.74
        movsd     -24(%rsp), %xmm3                              #740.36
        lea       (%r8,%r8), %edi                               #750.72
        mulsd     1104+_imldExpHATab(%rip), %xmm3               #740.57
        movsd     -24(%rsp), %xmm4                              #741.36
        subsd     %xmm3, %xmm5                                  #742.41
        mulsd     1112+_imldExpHATab(%rip), %xmm4               #741.57
        movsd     1072+_imldExpHATab(%rip), %xmm2               #744.55
        subsd     %xmm4, %xmm5                                  #743.38
        mulsd     %xmm5, %xmm2                                  #744.77
        shrl      $6, %ecx                                      #748.30
        addsd     1064+_imldExpHATab(%rip), %xmm2               #744.98
        comisd    1088+_imldExpHATab(%rip), %xmm6               #751.50
        mulsd     %xmm5, %xmm2                                  #744.121
        movsd     (%r10,%rdi,8), %xmm0                          #750.98
        lea       1023(%rcx), %edx                              #749.29
        addsd     1056+_imldExpHATab(%rip), %xmm2               #744.142
        mulsd     %xmm5, %xmm2                                  #744.165
        addsd     1048+_imldExpHATab(%rip), %xmm2               #744.186
        mulsd     %xmm5, %xmm2                                  #744.209
        addsd     1040+_imldExpHATab(%rip), %xmm2               #744.230
        mulsd     %xmm5, %xmm2                                  #744.253
        mulsd     %xmm5, %xmm2                                  #745.47
        addsd     %xmm5, %xmm2                                  #745.53
        addsd     (%r10,%r9,8), %xmm2                           #750.55
        mulsd     %xmm0, %xmm2                                  #750.98
        jb        ..B10.10      # Prob 50%                      #751.50
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax edx ecx xmm0 xmm2
..B10.7:                        # Preds ..B10.6
                                # Execution count [3.78e-02]
        andl      $2047, %edx                                   #754.33
        addsd     %xmm0, %xmm2                                  #753.59
        cmpl      $2046, %edx                                   #755.34
        ja        ..B10.9       # Prob 50%                      #755.34
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax edx xmm2
..B10.8:                        # Preds ..B10.7
                                # Execution count [1.89e-02]
        movq      1136+_imldExpHATab(%rip), %rcx                #757.50
        shrq      $48, %rcx                                     #757.50
        shll      $4, %edx                                      #757.50
        andl      $-32753, %ecx                                 #757.50
        orl       %edx, %ecx                                    #757.50
        movw      %cx, -2(%rsp)                                 #757.50
        movsd     -8(%rsp), %xmm0                               #758.45
        mulsd     %xmm0, %xmm2                                  #758.53
        movsd     %xmm2, (%rsi)                                 #758.31
        ret                                                     #758.31
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.9:                        # Preds ..B10.7
                                # Execution count [1.89e-02]
        decl      %edx                                          #762.78
        andl      $2047, %edx                                   #762.50
        movzwl    -2(%rsp), %ecx                                #762.50
        shll      $4, %edx                                      #762.50
        andl      $-32753, %ecx                                 #762.50
        orl       %edx, %ecx                                    #762.50
        movw      %cx, -2(%rsp)                                 #762.50
        movsd     -8(%rsp), %xmm0                               #763.46
        mulsd     %xmm0, %xmm2                                  #763.46
        mulsd     1152+_imldExpHATab(%rip), %xmm2               #764.84
        movsd     %xmm2, (%rsi)                                 #764.31
        ret                                                     #764.31
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.10:                       # Preds ..B10.6
                                # Execution count [3.78e-02]
        addl      $1083, %ecx                                   #769.42
        andl      $2047, %ecx                                   #770.33
        movl      %ecx, %eax                                    #771.46
        movzwl    -2(%rsp), %edx                                #771.46
        shll      $4, %eax                                      #771.46
        andl      $-32753, %edx                                 #771.46
        orl       %eax, %edx                                    #771.46
        movw      %dx, -2(%rsp)                                 #771.46
        movsd     -8(%rsp), %xmm1                               #772.30
        mulsd     %xmm1, %xmm2                                  #772.25
        mulsd     %xmm0, %xmm1                                  #773.66
        movaps    %xmm1, %xmm0                                  #776.74
        addsd     %xmm2, %xmm0                                  #776.74
        cmpl      $50, %ecx                                     #774.34
        ja        ..B10.12      # Prob 50%                      #774.34
                                # LOE rbx rbp rsi r12 r13 r14 r15 xmm0 xmm1 xmm2
..B10.11:                       # Preds ..B10.10
                                # Execution count [1.89e-02]
        mulsd     1160+_imldExpHATab(%rip), %xmm0               #777.94
        movsd     %xmm0, (%rsi)                                 #777.59
        jmp       ..B10.13      # Prob 100%                     #777.59
                                # LOE rbx rbp r12 r13 r14 r15
..B10.12:                       # Preds ..B10.10
                                # Execution count [1.89e-02]
        movsd     %xmm0, -72(%rsp)                              #781.29
        movsd     -72(%rsp), %xmm0                              #782.62
        subsd     %xmm0, %xmm1                                  #782.62
        movsd     %xmm1, -64(%rsp)                              #782.29
        movsd     -64(%rsp), %xmm1                              #783.29
        addsd     %xmm2, %xmm1                                  #783.29
        movsd     %xmm1, -64(%rsp)                              #783.29
        movsd     -72(%rsp), %xmm2                              #784.112
        mulsd     1168+_imldExpHATab(%rip), %xmm2               #784.112
        movsd     %xmm2, -56(%rsp)                              #784.57
        movsd     -72(%rsp), %xmm4                              #785.45
        movsd     -56(%rsp), %xmm3                              #785.62
        addsd     %xmm3, %xmm4                                  #785.62
        movsd     %xmm4, -48(%rsp)                              #785.29
        movsd     -48(%rsp), %xmm6                              #786.49
        movsd     -56(%rsp), %xmm5                              #786.56
        subsd     %xmm5, %xmm6                                  #786.56
        movsd     %xmm6, -40(%rsp)                              #786.29
        movsd     -72(%rsp), %xmm8                              #787.49
        movsd     -40(%rsp), %xmm7                              #787.66
        subsd     %xmm7, %xmm8                                  #787.66
        movsd     %xmm8, -32(%rsp)                              #787.29
        movsd     -64(%rsp), %xmm10                             #788.41
        movsd     -32(%rsp), %xmm9                              #788.29
        addsd     %xmm9, %xmm10                                 #788.29
        movsd     %xmm10, -32(%rsp)                             #788.29
        movsd     -40(%rsp), %xmm11                             #789.49
        mulsd     1160+_imldExpHATab(%rip), %xmm11              #789.77
        movsd     %xmm11, -40(%rsp)                             #789.29
        movsd     -32(%rsp), %xmm12                             #790.49
        mulsd     1160+_imldExpHATab(%rip), %xmm12              #790.77
        movsd     %xmm12, -32(%rsp)                             #790.29
        movsd     -40(%rsp), %xmm14                             #791.45
        movsd     -32(%rsp), %xmm13                             #791.56
        addsd     %xmm13, %xmm14                                #791.56
        movsd     %xmm14, (%rsi)                                #791.31
                                # LOE rbx rbp r12 r13 r14 r15
..B10.13:                       # Preds ..B10.11 ..B10.12
                                # Execution count [3.78e-02]
        movl      $4, %eax                                      #793.25
        ret                                                     #793.25
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.14:                       # Preds ..B10.5
                                # Execution count [7.56e-02]
        movsd     1120+_imldExpHATab(%rip), %xmm0               #798.84
        movl      $4, %eax                                      #799.21
        mulsd     %xmm0, %xmm0                                  #798.84
        movsd     %xmm0, (%rsi)                                 #798.23
        ret                                                     #798.23
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.15:                       # Preds ..B10.4
                                # Execution count [1.51e-01]
        movsd     1128+_imldExpHATab(%rip), %xmm0               #804.80
        movl      $3, %eax                                      #805.17
        mulsd     %xmm0, %xmm0                                  #804.80
        movsd     %xmm0, (%rsi)                                 #804.19
        ret                                                     #804.19
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.16:                       # Preds ..B10.3
                                # Execution count [3.02e-01]
        movsd     1144+_imldExpHATab(%rip), %xmm0               #810.37
        addsd     %xmm6, %xmm0                                  #810.59
        movsd     %xmm0, (%rsi)                                 #810.15
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.17:                       # Preds ..B10.16
                                # Execution count [7.20e-01]
        ret                                                     #824.12
                                # LOE
..B10.18:                       # Preds ..B10.1
                                # Execution count [2.80e-01]
        movq      $0xbff0000000000000, %rax                     #726.21
        movq      %rax, (%rsi)                                  #726.19
        xorl      %eax, %eax                                    #726.35
        ret                                                     #726.35
                                # LOE
..B10.19:                       # Preds ..B10.2
                                # Execution count [1.15e-01]: Infreq
        movb      -1(%rsp), %dl                                 #815.36
        andb      $-128, %dl                                    #815.36
        cmpb      $-128, %dl                                    #815.57
        je        ..B10.21      # Prob 16%                      #815.57
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm6
..B10.20:                       # Preds ..B10.22 ..B10.19 ..B10.21
                                # Execution count [1.11e-01]: Infreq
        mulsd     %xmm6, %xmm6                                  #821.27
        movsd     %xmm6, (%rsi)                                 #821.15
        ret                                                     #821.15
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.21:                       # Preds ..B10.19
                                # Execution count [1.84e-02]: Infreq
        testl     $1048575, -4(%rsp)                            #815.86
        jne       ..B10.20      # Prob 50%                      #815.117
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm6
..B10.22:                       # Preds ..B10.21
                                # Execution count [9.22e-03]: Infreq
        cmpl      $0, -8(%rsp)                                  #815.176
        jne       ..B10.20      # Prob 50%                      #815.176
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm6
..B10.23:                       # Preds ..B10.22
                                # Execution count [4.61e-03]: Infreq
        movq      1136+_imldExpHATab(%rip), %rdx                #817.15
        movq      %rdx, (%rsi)                                  #817.15
        ret                                                     #817.15
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_dexpm1_ha_cout_rare_internal,@function
	.size	__svml_dexpm1_ha_cout_rare_internal,.-__svml_dexpm1_ha_cout_rare_internal
..LN__svml_dexpm1_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dexpm1_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_dexpm1_ha_data_internal_avx512
	.globl __svml_dexpm1_ha_data_internal_avx512
__svml_dexpm1_ha_data_internal_avx512:
	.long	0
	.long	1072693248
	.long	1828292879
	.long	1072739672
	.long	1014845819
	.long	1072788152
	.long	1853186616
	.long	1072838778
	.long	171030293
	.long	1072891646
	.long	1276261410
	.long	1072946854
	.long	3577096743
	.long	1073004506
	.long	3712504873
	.long	1073064711
	.long	1719614413
	.long	1073127582
	.long	1944781191
	.long	1073193236
	.long	1110089947
	.long	1073261797
	.long	2191782032
	.long	1073333393
	.long	2572866477
	.long	1073408159
	.long	3716502172
	.long	1073486235
	.long	3707479175
	.long	1073567768
	.long	2728693978
	.long	1073652911
	.long	0
	.long	0
	.long	1568897901
	.long	1016568486
	.long	3936719688
	.long	3162512149
	.long	3819481236
	.long	1016499965
	.long	1303423926
	.long	1015238005
	.long	2804567149
	.long	1015390024
	.long	3145379760
	.long	1014403278
	.long	3793507337
	.long	1016095713
	.long	3210617384
	.long	3163796463
	.long	3108873501
	.long	3162190556
	.long	3253791412
	.long	1015920431
	.long	730975783
	.long	1014083580
	.long	2462790535
	.long	1015814775
	.long	816778419
	.long	1014197934
	.long	2789017511
	.long	1014276997
	.long	2413007344
	.long	3163551506
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	0
	.long	2147483648
	.long	0
	.long	2147483648
	.long	0
	.long	2147483648
	.long	0
	.long	2147483648
	.long	0
	.long	2147483648
	.long	0
	.long	2147483648
	.long	0
	.long	2147483648
	.long	0
	.long	2147483648
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	0
	.long	3227516928
	.long	0
	.long	3227516928
	.long	0
	.long	3227516928
	.long	0
	.long	3227516928
	.long	0
	.long	3227516928
	.long	0
	.long	3227516928
	.long	0
	.long	3227516928
	.long	0
	.long	3227516928
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	4106095538
	.long	1056571896
	.long	4106095538
	.long	1056571896
	.long	4106095538
	.long	1056571896
	.long	4106095538
	.long	1056571896
	.long	4106095538
	.long	1056571896
	.long	4106095538
	.long	1056571896
	.long	4106095538
	.long	1056571896
	.long	4106095538
	.long	1056571896
	.long	4105251267
	.long	1059717624
	.long	4105251267
	.long	1059717624
	.long	4105251267
	.long	1059717624
	.long	4105251267
	.long	1059717624
	.long	4105251267
	.long	1059717624
	.long	4105251267
	.long	1059717624
	.long	4105251267
	.long	1059717624
	.long	4105251267
	.long	1059717624
	.long	372262401
	.long	1062650220
	.long	372262401
	.long	1062650220
	.long	372262401
	.long	1062650220
	.long	372262401
	.long	1062650220
	.long	372262401
	.long	1062650220
	.long	372262401
	.long	1062650220
	.long	372262401
	.long	1062650220
	.long	372262401
	.long	1062650220
	.long	279336721
	.long	1065423121
	.long	279336721
	.long	1065423121
	.long	279336721
	.long	1065423121
	.long	279336721
	.long	1065423121
	.long	279336721
	.long	1065423121
	.long	279336721
	.long	1065423121
	.long	279336721
	.long	1065423121
	.long	279336721
	.long	1065423121
	.long	1431655946
	.long	1067799893
	.long	1431655946
	.long	1067799893
	.long	1431655946
	.long	1067799893
	.long	1431655946
	.long	1067799893
	.long	1431655946
	.long	1067799893
	.long	1431655946
	.long	1067799893
	.long	1431655946
	.long	1067799893
	.long	1431655946
	.long	1067799893
	.long	1431655929
	.long	1069897045
	.long	1431655929
	.long	1069897045
	.long	1431655929
	.long	1069897045
	.long	1431655929
	.long	1069897045
	.long	1431655929
	.long	1069897045
	.long	1431655929
	.long	1069897045
	.long	1431655929
	.long	1069897045
	.long	1431655929
	.long	1069897045
	.long	0
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	1072693248
	.long	0
	.long	1072693248
	.long	0
	.long	1072693248
	.long	0
	.long	1072693248
	.long	0
	.long	1072693248
	.long	0
	.long	1072693248
	.long	0
	.long	1072693248
	.long	0
	.long	1072693248
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.type	__svml_dexpm1_ha_data_internal_avx512,@object
	.size	__svml_dexpm1_ha_data_internal_avx512,1344
	.align 64
	.hidden __svml_dexpm1_ha_data_internal
	.globl __svml_dexpm1_ha_data_internal
__svml_dexpm1_ha_data_internal:
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2818572288
	.long	5693
	.long	1457015399
	.long	1044362035
	.long	1073741824
	.long	11418
	.long	4044949557
	.long	3191375865
	.long	3892314112
	.long	17173
	.long	794616807
	.long	1041997793
	.long	3489660928
	.long	22960
	.long	2715237930
	.long	1044950059
	.long	671088640
	.long	28779
	.long	3698181124
	.long	1044242285
	.long	402653184
	.long	34629
	.long	36755401
	.long	1042114290
	.long	3355443200
	.long	40510
	.long	438898435
	.long	1044789148
	.long	1879048192
	.long	46424
	.long	2230008375
	.long	3192402871
	.long	671088640
	.long	52370
	.long	3120814979
	.long	1045140031
	.long	805306368
	.long	58348
	.long	269498903
	.long	1044815501
	.long	2952790016
	.long	64358
	.long	3705630242
	.long	3182612048
	.long	3489660928
	.long	70401
	.long	2766913307
	.long	1039293264
	.long	3221225472
	.long	76477
	.long	4276399797
	.long	1041960050
	.long	2952790016
	.long	82586
	.long	80474087
	.long	3191172386
	.long	3355443200
	.long	88728
	.long	613423790
	.long	1042592202
	.long	1073741824
	.long	94904
	.long	689505308
	.long	3192657268
	.long	939524096
	.long	101113
	.long	930606615
	.long	1042387389
	.long	3892314112
	.long	107355
	.long	2850403528
	.long	1045134939
	.long	2281701376
	.long	113632
	.long	1663725767
	.long	3192904985
	.long	805306368
	.long	119943
	.long	2810207104
	.long	1043762074
	.long	536870912
	.long	126288
	.long	3854830848
	.long	1044899528
	.long	2281701376
	.long	132667
	.long	2397289153
	.long	1041802037
	.long	2415919104
	.long	139081
	.long	1649749971
	.long	1043848649
	.long	1879048192
	.long	145530
	.long	2039734354
	.long	3191384540
	.long	1342177280
	.long	152014
	.long	849302817
	.long	3188938352
	.long	1744830464
	.long	158533
	.long	383003846
	.long	3191925785
	.long	3758096384
	.long	165087
	.long	158134621
	.long	1044338232
	.long	4160749568
	.long	171677
	.long	4137603445
	.long	3192324360
	.long	3489660928
	.long	178303
	.long	4290499725
	.long	1043028785
	.long	2818572288
	.long	184965
	.long	1275031083
	.long	3190931407
	.long	2818572288
	.long	191663
	.long	1629266164
	.long	1043587829
	.long	134217728
	.long	198398
	.long	2842642093
	.long	1044483512
	.long	0
	.long	205169
	.long	1985360263
	.long	3192756542
	.long	2952790016
	.long	211976
	.long	4200916017
	.long	1044586679
	.long	1610612736
	.long	218821
	.long	3450763054
	.long	3189463043
	.long	939524096
	.long	225703
	.long	2870834528
	.long	3190336198
	.long	1879048192
	.long	232622
	.long	3553800616
	.long	3192377660
	.long	939524096
	.long	239579
	.long	1219436983
	.long	3192443648
	.long	3221225472
	.long	246573
	.long	606077177
	.long	1044946247
	.long	1342177280
	.long	253606
	.long	3998375791
	.long	3192876638
	.long	134217728
	.long	260677
	.long	586810495
	.long	3192560639
	.long	536870912
	.long	267786
	.long	2676240988
	.long	1044345570
	.long	3623878656
	.long	274933
	.long	1841759300
	.long	1043663497
	.long	1610612736
	.long	282120
	.long	1086643152
	.long	1041785419
	.long	4026531840
	.long	289345
	.long	1148024454
	.long	3192330237
	.long	3087007744
	.long	296610
	.long	2137125602
	.long	3191993881
	.long	4026531840
	.long	303914
	.long	3437605242
	.long	1043004027
	.long	3623878656
	.long	311258
	.long	3340100419
	.long	3192278702
	.long	2550136832
	.long	318642
	.long	3594204911
	.long	1044372944
	.long	2013265920
	.long	326066
	.long	2502738549
	.long	3191221557
	.long	2684354560
	.long	333530
	.long	235444137
	.long	1044806450
	.long	1476395008
	.long	341035
	.long	3792656324
	.long	3191220999
	.long	3355443200
	.long	348580
	.long	1982428721
	.long	1044573328
	.long	939524096
	.long	356167
	.long	1502688512
	.long	3191123330
	.long	3623878656
	.long	363794
	.long	383164906
	.long	3192603072
	.long	3758096384
	.long	371463
	.long	3040458367
	.long	3192241502
	.long	2281701376
	.long	379174
	.long	3087934862
	.long	1044564533
	.long	402653184
	.long	386927
	.long	3163234522
	.long	3192035061
	.long	3087007744
	.long	394721
	.long	2332520281
	.long	1043819968
	.long	2952790016
	.long	402558
	.long	1492679939
	.long	1041050306
	.long	939524096
	.long	410438
	.long	29656007
	.long	3192494567
	.long	2147483648
	.long	418360
	.long	612974287
	.long	1044556049
	.long	3623878656
	.long	426325
	.long	1740578119
	.long	3192756916
	.long	1744830464
	.long	434334
	.long	922176773
	.long	3191344195
	.long	2013265920
	.long	442386
	.long	143936179
	.long	3192365354
	.long	1073741824
	.long	450482
	.long	2288974058
	.long	3192706862
	.long	4160749568
	.long	458621
	.long	1022918171
	.long	1043667272
	.long	3892314112
	.long	466805
	.long	2074373662
	.long	1043172334
	.long	1207959552
	.long	475034
	.long	2007733066
	.long	1042591790
	.long	1476395008
	.long	483307
	.long	1946752598
	.long	3191593347
	.long	1342177280
	.long	491625
	.long	1328713708
	.long	3187724640
	.long	1879048192
	.long	499988
	.long	918464641
	.long	1045387276
	.long	0
	.long	508397
	.long	667194164
	.long	1043532819
	.long	939524096
	.long	516851
	.long	3740938196
	.long	3191016217
	.long	1476395008
	.long	525351
	.long	1917817036
	.long	3192786735
	.long	2550136832
	.long	533897
	.long	682424459
	.long	1043647713
	.long	1207959552
	.long	542490
	.long	857395348
	.long	3191718789
	.long	2550136832
	.long	551129
	.long	1678188781
	.long	1045046423
	.long	3623878656
	.long	559815
	.long	2523214013
	.long	1043900009
	.long	1073741824
	.long	568549
	.long	3671932459
	.long	1044468998
	.long	402653184
	.long	577330
	.long	1091392995
	.long	3191122871
	.long	2550136832
	.long	586158
	.long	1656324724
	.long	1043421043
	.long	134217728
	.long	595035
	.long	742731994
	.long	1045204990
	.long	2952790016
	.long	603959
	.long	2659845000
	.long	1042921660
	.long	3355443200
	.long	612932
	.long	2001576987
	.long	1045316240
	.long	2684354560
	.long	621954
	.long	976271096
	.long	3187726552
	.long	1879048192
	.long	631025
	.long	927342903
	.long	1042890999
	.long	2147483648
	.long	640145
	.long	2162418230
	.long	1044717444
	.long	402653184
	.long	649315
	.long	830622888
	.long	1044263474
	.long	2013265920
	.long	658534
	.long	630511316
	.long	1045098283
	.long	4026531840
	.long	667803
	.long	1698296944
	.long	3192762006
	.long	2952790016
	.long	677123
	.long	3831108133
	.long	1044508970
	.long	268435456
	.long	686494
	.long	3279515609
	.long	1045005722
	.long	1476395008
	.long	695915
	.long	98608862
	.long	3192139794
	.long	3221225472
	.long	705387
	.long	529675467
	.long	3188065859
	.long	2550136832
	.long	714911
	.long	3588780877
	.long	1043705146
	.long	671088640
	.long	724487
	.long	1493713581
	.long	1043913574
	.long	3087007744
	.long	734114
	.long	3182425146
	.long	1041483134
	.long	2415919104
	.long	743794
	.long	864959479
	.long	3191919926
	.long	4026531840
	.long	753526
	.long	928333188
	.long	1044896498
	.long	805306368
	.long	763312
	.long	813799033
	.long	1042555081
	.long	2415919104
	.long	773150
	.long	2300504125
	.long	1041428596
	.long	1476395008
	.long	783042
	.long	1142965944
	.long	1045346544
	.long	3758096384
	.long	792987
	.long	518977959
	.long	3192116587
	.long	1610612736
	.long	802987
	.long	1972387576
	.long	3179791049
	.long	805306368
	.long	813041
	.long	1264446592
	.long	3191505643
	.long	2550136832
	.long	823149
	.long	1467128350
	.long	3192899778
	.long	3758096384
	.long	833312
	.long	3075989921
	.long	3192423292
	.long	1476395008
	.long	843531
	.long	836600757
	.long	3192197600
	.long	1207959552
	.long	853805
	.long	3697834264
	.long	1044397131
	.long	134217728
	.long	864135
	.long	364651635
	.long	1038816227
	.long	3758096384
	.long	874520
	.long	3335598035
	.long	3192398555
	.long	402653184
	.long	884963
	.long	2219290723
	.long	3191039942
	.long	0
	.long	895462
	.long	730095629
	.long	1045354900
	.long	4026531840
	.long	906017
	.long	39537391
	.long	1044909475
	.long	805306368
	.long	916631
	.long	4123739734
	.long	1045159130
	.long	402653184
	.long	927302
	.long	3136734448
	.long	3192410870
	.long	3892314112
	.long	938030
	.long	1982905152
	.long	3189583874
	.long	4160749568
	.long	948817
	.long	442147929
	.long	1045314148
	.long	2684354560
	.long	959663
	.long	3425467293
	.long	1044718726
	.long	805306368
	.long	970568
	.long	2073198199
	.long	3192097984
	.long	4026531840
	.long	981531
	.long	2291008222
	.long	3191466589
	.long	939524096
	.long	992555
	.long	372190496
	.long	3189934253
	.long	1476395008
	.long	1003638
	.long	54164518
	.long	1045131818
	.long	2952790016
	.long	1014781
	.long	1672962650
	.long	3192068623
	.long	2147483648
	.long	1025985
	.long	2196310654
	.long	1043982605
	.long	671088640
	.long	1037250
	.long	2286661074
	.long	1045199759
	.long	1753710392
	.long	1065423121
	.long	1753710392
	.long	1065423121
	.long	1753710392
	.long	1065423121
	.long	1753710392
	.long	1065423121
	.long	1753710392
	.long	1065423121
	.long	1753710392
	.long	1065423121
	.long	1753710392
	.long	1065423121
	.long	1753710392
	.long	1065423121
	.long	3265904883
	.long	1067799893
	.long	3265904883
	.long	1067799893
	.long	3265904883
	.long	1067799893
	.long	3265904883
	.long	1067799893
	.long	3265904883
	.long	1067799893
	.long	3265904883
	.long	1067799893
	.long	3265904883
	.long	1067799893
	.long	3265904883
	.long	1067799893
	.long	1431655453
	.long	1069897045
	.long	1431655453
	.long	1069897045
	.long	1431655453
	.long	1069897045
	.long	1431655453
	.long	1069897045
	.long	1431655453
	.long	1069897045
	.long	1431655453
	.long	1069897045
	.long	1431655453
	.long	1069897045
	.long	1431655453
	.long	1069897045
	.long	4294966876
	.long	1071644671
	.long	4294966876
	.long	1071644671
	.long	4294966876
	.long	1071644671
	.long	4294966876
	.long	1071644671
	.long	4294966876
	.long	1071644671
	.long	4294966876
	.long	1071644671
	.long	4294966876
	.long	1071644671
	.long	4294966876
	.long	1071644671
	.long	1697350398
	.long	1080497479
	.long	1697350398
	.long	1080497479
	.long	1697350398
	.long	1080497479
	.long	1697350398
	.long	1080497479
	.long	1697350398
	.long	1080497479
	.long	1697350398
	.long	1080497479
	.long	1697350398
	.long	1080497479
	.long	1697350398
	.long	1080497479
	.long	4277665792
	.long	1064709698
	.long	4277665792
	.long	1064709698
	.long	4277665792
	.long	1064709698
	.long	4277665792
	.long	1064709698
	.long	4277665792
	.long	1064709698
	.long	4277665792
	.long	1064709698
	.long	4277665792
	.long	1064709698
	.long	4277665792
	.long	1064709698
	.long	2882134964
	.long	1027723129
	.long	2882134964
	.long	1027723129
	.long	2882134964
	.long	1027723129
	.long	2882134964
	.long	1027723129
	.long	2882134964
	.long	1027723129
	.long	2882134964
	.long	1027723129
	.long	2882134964
	.long	1027723129
	.long	2882134964
	.long	1027723129
	.long	2095104
	.long	1123549184
	.long	2095104
	.long	1123549184
	.long	2095104
	.long	1123549184
	.long	2095104
	.long	1123549184
	.long	2095104
	.long	1123549184
	.long	2095104
	.long	1123549184
	.long	2095104
	.long	1123549184
	.long	2095104
	.long	1123549184
	.long	2032
	.long	0
	.long	2032
	.long	0
	.long	2032
	.long	0
	.long	2032
	.long	0
	.long	2032
	.long	0
	.long	2032
	.long	0
	.long	2032
	.long	0
	.long	2032
	.long	0
	.long	4192256
	.long	0
	.long	4192256
	.long	0
	.long	4192256
	.long	0
	.long	4192256
	.long	0
	.long	4192256
	.long	0
	.long	4192256
	.long	0
	.long	4192256
	.long	0
	.long	4192256
	.long	0
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.long	0
	.long	3220176896
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	1287323203
	.long	1082531232
	.long	1287323203
	.long	1082531232
	.long	1287323203
	.long	1082531232
	.long	1287323203
	.long	1082531232
	.long	1287323203
	.long	1082531232
	.long	1287323203
	.long	1082531232
	.long	1287323203
	.long	1082531232
	.long	1287323203
	.long	1082531232
	.long	4277811695
	.long	1064709698
	.long	4277811695
	.long	1064709698
	.long	4277811695
	.long	1064709698
	.long	4277811695
	.long	1064709698
	.long	4277811695
	.long	1064709698
	.long	4277811695
	.long	1064709698
	.long	4277811695
	.long	1064709698
	.long	4277811695
	.long	1064709698
	.long	1047552
	.long	1124597760
	.long	1047552
	.long	1124597760
	.long	1047552
	.long	1124597760
	.long	1047552
	.long	1124597760
	.long	1047552
	.long	1124597760
	.long	1047552
	.long	1124597760
	.long	1047552
	.long	1124597760
	.long	1047552
	.long	1124597760
	.long	1016
	.long	0
	.long	1016
	.long	0
	.long	1016
	.long	0
	.long	1016
	.long	0
	.long	1016
	.long	0
	.long	1016
	.long	0
	.long	1016
	.long	0
	.long	1016
	.long	0
	.long	2096128
	.long	0
	.long	2096128
	.long	0
	.long	2096128
	.long	0
	.long	2096128
	.long	0
	.long	2096128
	.long	0
	.long	2096128
	.long	0
	.long	2096128
	.long	0
	.long	2096128
	.long	0
	.type	__svml_dexpm1_ha_data_internal,@object
	.size	__svml_dexpm1_ha_data_internal,3200
	.align 32
_imldExpHATab:
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
	.long	4277811695
	.long	1082535490
	.long	3715808466
	.long	3230016299
	.long	3576508497
	.long	3230091536
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
	.type	_imldExpHATab,@object
	.size	_imldExpHATab,1176
	.space 8, 0x00 	# pad
	.align 16
.L_2il0floatpacket.33:
	.long	0x00000000,0x43380000,0x00000000,0x43380000
	.type	.L_2il0floatpacket.33,@object
	.size	.L_2il0floatpacket.33,16
	.align 8
.L_2il0floatpacket.92:
	.long	0x00000000,0xbff00000
	.type	.L_2il0floatpacket.92,@object
	.size	.L_2il0floatpacket.92,8
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
