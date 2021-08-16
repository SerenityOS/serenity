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
	.file "svml_d_cosh.c"
	.text
..TXTST0:
.L_2__routine_start___svml_cosh1_ha_e9_0:
# -- Begin  __svml_cosh1_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosh1_ha_e9
# --- __svml_cosh1_ha_e9(__m128d)
__svml_cosh1_ha_e9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #285.1
        .byte     15                                            #359.546
        .byte     30                                            #359.546
        .byte     250                                           #359.546
	.cfi_startproc
..___tag_value___svml_cosh1_ha_e9.1:
..L2:
                                                          #285.1
        pushq     %rbp                                          #285.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #285.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #285.1
        subq      $128, %rsp                                    #285.1
        movl      $127, %edx                                    #327.23
        movsd     4672+__svml_dcosh_ha_data_internal(%rip), %xmm1 #319.16
        movl      $1082531225, %eax                             #323.27
        andnps    %xmm0, %xmm1                                  #319.16
        lea       912+__svml_dcosh_ha_data_internal(%rip), %rsi #328.642
        movaps    %xmm1, %xmm12                                 #320.25
        mulsd     3968+__svml_dcosh_ha_data_internal(%rip), %xmm12 #320.25
        movd      %edx, %xmm4                                   #327.23
        movsd     4160+__svml_dcosh_ha_data_internal(%rip), %xmm7 #309.20
        movd      %eax, %xmm2                                   #323.27
        movsd     4032+__svml_dcosh_ha_data_internal(%rip), %xmm8 #332.35
        addsd     %xmm7, %xmm12                                 #320.13
        movaps    %xmm12, %xmm9                                 #331.13
        movsd     4096+__svml_dcosh_ha_data_internal(%rip), %xmm10 #333.32
        subsd     %xmm7, %xmm9                                  #331.13
        mulsd     %xmm9, %xmm8                                  #332.35
        mulsd     %xmm9, %xmm10                                 #333.32
        pshufd    $85, %xmm1, %xmm3                             #322.22
        subsd     %xmm8, %xmm1                                  #332.13
        pshufd    $0, %xmm12, %xmm5                             #326.23
        pcmpgtd   %xmm2, %xmm3                                  #323.27
        pand      %xmm4, %xmm5                                  #327.23
        psllq     $45, %xmm12                                   #334.19
        movsd     4544+__svml_dcosh_ha_data_internal(%rip), %xmm4 #349.25
        movdqa    %xmm5, %xmm6                                  #328.242
        pslld     $1, %xmm6                                     #328.242
        subsd     %xmm10, %xmm1                                 #333.13
        movmskps  %xmm3, %eax                                   #324.44
        movaps    %xmm1, %xmm13                                 #348.14
        paddd     %xmm5, %xmm6                                  #328.389
        mulsd     %xmm1, %xmm13                                 #348.14
        pslld     $3, %xmm6                                     #328.389
        movd      %xmm6, %ecx                                   #328.463
        movsd     4480+__svml_dcosh_ha_data_internal(%rip), %xmm8 #350.27
        mulsd     %xmm13, %xmm4                                 #349.25
        mulsd     %xmm13, %xmm8                                 #350.27
        addsd     4416+__svml_dcosh_ha_data_internal(%rip), %xmm4 #349.13
        addsd     4352+__svml_dcosh_ha_data_internal(%rip), %xmm8 #350.15
        mulsd     %xmm13, %xmm4                                 #351.13
        mulsd     %xmm13, %xmm8                                 #352.15
        mulsd     %xmm1, %xmm4                                  #354.25
        movq      4608+__svml_dcosh_ha_data_internal(%rip), %xmm11 #335.25
        addsd     %xmm1, %xmm4                                  #354.13
        movq      -16(%rcx,%rsi), %xmm3                         #328.642
        pand      %xmm11, %xmm12                                #336.19
        movq      (%rsi,%rcx), %xmm2                            #330.639
        paddq     %xmm12, %xmm3                                 #338.19
        psubq     %xmm12, %xmm2                                 #344.19
        movdqa    %xmm3, %xmm14                                 #347.16
        movdqa    %xmm3, %xmm7                                  #346.16
        movq      -8(%rsi,%rcx), %xmm15                         #329.642
        addsd     %xmm2, %xmm14                                 #347.16
        subsd     %xmm2, %xmm7                                  #346.16
        paddq     %xmm12, %xmm15                                #341.19
        mulsd     %xmm14, %xmm8                                 #353.27
        mulsd     %xmm7, %xmm4                                  #355.27
        addsd     %xmm15, %xmm8                                 #353.15
        addsd     %xmm8, %xmm4                                  #355.15
        addsd     %xmm2, %xmm4                                  #356.15
        addsd     %xmm3, %xmm4                                  #357.14
        andl      $1, %eax                                      #324.98
        jne       ..B1.3        # Prob 5%                       #359.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm4
..B1.2:                         # Preds ..B1.4 ..B1.1
                                # Execution count [1.00e+00]
        movaps    %xmm4, %xmm0                                  #362.12
        movq      %rbp, %rsp                                    #362.12
        popq      %rbp                                          #362.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #362.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm0, (%rsp)                                 #359.160
        movsd     %xmm4, 64(%rsp)                               #359.233
        jne       ..B1.6        # Prob 5%                       #359.374
                                # LOE rbx r12 r13 r14 r15
..B1.4:                         # Preds ..B1.6 ..B1.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm4                               #359.626
        jmp       ..B1.2        # Prob 100%                     #359.626
                                # LOE rbx r12 r13 r14 r15 xmm4
..B1.6:                         # Preds ..B1.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #359.546
        lea       64(%rsp), %rsi                                #359.546
..___tag_value___svml_cosh1_ha_e9.10:
#       __svml_dcosh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcosh_ha_cout_rare_internal            #359.546
..___tag_value___svml_cosh1_ha_e9.11:
        jmp       ..B1.4        # Prob 100%                     #359.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_cosh1_ha_e9,@function
	.size	__svml_cosh1_ha_e9,.-__svml_cosh1_ha_e9
..LN__svml_cosh1_ha_e9.0:
	.data
# -- End  __svml_cosh1_ha_e9
	.text
.L_2__routine_start___svml_cosh4_ha_l9_1:
# -- Begin  __svml_cosh4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosh4_ha_l9
# --- __svml_cosh4_ha_l9(__m256d)
__svml_cosh4_ha_l9:
# parameter 1: %ymm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #367.1
        .byte     15                                            #441.552
        .byte     30                                            #441.552
        .byte     250                                           #441.552
	.cfi_startproc
..___tag_value___svml_cosh4_ha_l9.13:
..L14:
                                                         #367.1
        pushq     %rbp                                          #367.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #367.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #367.1
        subq      $192, %rsp                                    #367.1
        lea       912+__svml_dcosh_ha_data_internal(%rip), %r8  #410.706
        vmovupd   4672+__svml_dcosh_ha_data_internal(%rip), %ymm7 #397.51
        vmovupd   4160+__svml_dcosh_ha_data_internal(%rip), %ymm4 #391.54
        vmovupd   3968+__svml_dcosh_ha_data_internal(%rip), %ymm3 #402.13
        vmovupd   4032+__svml_dcosh_ha_data_internal(%rip), %ymm5 #399.53
        vmovapd   %ymm0, %ymm6                                  #367.1
        vandnpd   %ymm6, %ymm7, %ymm2                           #401.16
        vfmadd213pd %ymm4, %ymm2, %ymm3                         #402.13
        vextractf128 $1, %ymm3, %xmm11                          #408.126
        vsubpd    %ymm4, %ymm3, %ymm4                           #413.13
        vshufps   $136, %xmm11, %xmm3, %xmm12                   #408.37
        vpand     4224+__svml_dcosh_ha_data_internal(%rip), %xmm12, %xmm14 #409.19
        vpslld    $1, %xmm14, %xmm13                            #410.238
        vpaddd    %xmm14, %xmm13, %xmm15                        #410.365
        vpslld    $3, %xmm15, %xmm7                             #410.365
        vpsllq    $45, %ymm3, %ymm3                             #416.15
        vmovd     %xmm7, %edx                                   #410.435
        vpextrd   $2, %xmm7, %esi                               #410.566
        vpextrd   $1, %xmm7, %ecx                               #410.499
        vpextrd   $3, %xmm7, %edi                               #410.633
        vmovq     -16(%rdx,%r8), %xmm0                          #410.1203
        vmovq     (%rdx,%r8), %xmm14                            #412.1200
        vmovq     -8(%rsi,%r8), %xmm12                          #411.1403
        vmovq     (%rsi,%r8), %xmm7                             #412.1400
        vmovhpd   -16(%rcx,%r8), %xmm0, %xmm1                   #410.1171
        vmovhpd   (%rcx,%r8), %xmm14, %xmm15                    #412.1168
        vmovhpd   -8(%rdi,%r8), %xmm12, %xmm13                  #411.1371
        vextractf128 $1, %ymm2, %xmm8                           #404.125
        vshufps   $221, %xmm8, %xmm2, %xmm9                     #404.36
        vfnmadd231pd %ymm5, %ymm4, %ymm2                        #414.13
        vpcmpgtd  4736+__svml_dcosh_ha_data_internal(%rip), %xmm9, %xmm10 #405.23
        vpand     4608+__svml_dcosh_ha_data_internal(%rip), %ymm3, %ymm5 #418.15
        vmovq     -16(%rsi,%r8), %xmm8                          #410.1403
        vmovmskps %xmm10, %eax                                  #406.44
        vmovq     -8(%rdx,%r8), %xmm10                          #411.1203
        vmovhpd   -16(%rdi,%r8), %xmm8, %xmm9                   #410.1371
        vfnmadd231pd 4096+__svml_dcosh_ha_data_internal(%rip), %ymm4, %ymm2 #415.13
        vmovhpd   -8(%rcx,%r8), %xmm10, %xmm11                  #411.1171
        vmovhpd   (%rdi,%r8), %xmm7, %xmm8                      #412.1368
        vinsertf128 $1, %xmm9, %ymm1, %ymm1                     #410.1125
        vinsertf128 $1, %xmm13, %ymm11, %ymm0                   #411.1125
        vinsertf128 $1, %xmm8, %ymm15, %ymm9                    #412.1122
        vpaddq    %ymm5, %ymm1, %ymm11                          #420.15
        vpaddq    %ymm5, %ymm0, %ymm3                           #423.15
        vpsubq    %ymm5, %ymm9, %ymm8                           #426.15
        vmulpd    %ymm2, %ymm2, %ymm5                           #430.14
        vmovupd   4544+__svml_dcosh_ha_data_internal(%rip), %ymm9 #431.13
        vmovupd   4480+__svml_dcosh_ha_data_internal(%rip), %ymm0 #432.15
        vaddpd    %ymm8, %ymm11, %ymm1                          #429.16
        vsubpd    %ymm8, %ymm11, %ymm4                          #428.16
        vfmadd213pd 4416+__svml_dcosh_ha_data_internal(%rip), %ymm5, %ymm9 #431.13
        vfmadd213pd 4352+__svml_dcosh_ha_data_internal(%rip), %ymm5, %ymm0 #432.15
        vmulpd    %ymm9, %ymm5, %ymm10                          #433.13
        vmulpd    %ymm0, %ymm5, %ymm7                           #434.15
        vfmadd213pd %ymm2, %ymm2, %ymm10                        #436.13
        vfmadd213pd %ymm3, %ymm1, %ymm7                         #435.15
        vfmadd213pd %ymm7, %ymm4, %ymm10                        #437.15
        vaddpd    %ymm10, %ymm8, %ymm2                          #438.15
        vaddpd    %ymm2, %ymm11, %ymm0                          #439.14
        testl     %eax, %eax                                    #441.52
        jne       ..B2.3        # Prob 5%                       #441.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm6
..B2.2:                         # Preds ..B2.3 ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #444.12
        popq      %rbp                                          #444.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #444.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm6, 64(%rsp)                               #441.200
        vmovupd   %ymm0, 128(%rsp)                              #441.276
        je        ..B2.2        # Prob 95%                      #441.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #441.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B2.13:                        # Preds ..B2.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #441.523
        jc        ..B2.10       # Prob 5%                       #441.523
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #441.476
        cmpl      $4, %r12d                                     #441.471
        jl        ..B2.7        # Prob 82%                      #441.471
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #441.683
        jmp       ..B2.2        # Prob 100%                     #441.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #441.552
        lea       128(%rsp,%r12,8), %rsi                        #441.552
..___tag_value___svml_cosh4_ha_l9.31:
#       __svml_dcosh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcosh_ha_cout_rare_internal            #441.552
..___tag_value___svml_cosh4_ha_l9.32:
        jmp       ..B2.8        # Prob 100%                     #441.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cosh4_ha_l9,@function
	.size	__svml_cosh4_ha_l9,.-__svml_cosh4_ha_l9
..LN__svml_cosh4_ha_l9.1:
	.data
# -- End  __svml_cosh4_ha_l9
	.text
.L_2__routine_start___svml_cosh8_ha_z0_2:
# -- Begin  __svml_cosh8_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosh8_ha_z0
# --- __svml_cosh8_ha_z0(__m512d)
__svml_cosh8_ha_z0:
# parameter 1: %zmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #449.1
        .byte     15                                            #527.759
        .byte     30                                            #527.759
        .byte     250                                           #527.759
	.cfi_startproc
..___tag_value___svml_cosh8_ha_z0.34:
..L35:
                                                         #449.1
        pushq     %rbp                                          #449.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #449.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #449.1
        subq      $192, %rsp                                    #449.1
        vmovups   4672+__svml_dcosh_ha_data_internal(%rip), %zmm1 #484.49
        vmovups   384+__svml_dcosh_ha_data_internal(%rip), %zmm8 #477.52
        vmovups   3968+__svml_dcosh_ha_data_internal(%rip), %zmm7 #487.13
        vmovups   4032+__svml_dcosh_ha_data_internal(%rip), %zmm9 #475.51
        vmovups   4096+__svml_dcosh_ha_data_internal(%rip), %zmm6 #476.51
        vmovups   128+__svml_dcosh_ha_data_internal(%rip), %zmm4 #495.275
        vmovups   256+__svml_dcosh_ha_data_internal(%rip), %zmm5 #496.268
        vmovups   832+__svml_dcosh_ha_data_internal(%rip), %zmm11 #483.49
        vmovups   768+__svml_dcosh_ha_data_internal(%rip), %zmm13 #482.49
        vmovups   512+__svml_dcosh_ha_data_internal(%rip), %zmm14 #478.49
        vmovups   576+__svml_dcosh_ha_data_internal(%rip), %zmm12 #479.49
        vmovaps   %zmm0, %zmm15                                 #449.1
        vandnpd   %zmm15, %zmm1, %zmm10                         #486.16
        vfmadd213pd {rn-sae}, %zmm8, %zmm10, %zmm7              #487.13
        vpsrlq    $32, %zmm10, %zmm0                            #489.38
        vsubpd    {rn-sae}, %zmm8, %zmm7, %zmm8                 #497.15
        vpmovqd   %zmm0, %ymm2                                  #489.16
        vpermt2pd 192+__svml_dcosh_ha_data_internal(%rip), %zmm7, %zmm4 #495.275
        vpermt2pd 320+__svml_dcosh_ha_data_internal(%rip), %zmm7, %zmm5 #496.268
        vfnmadd231pd {rn-sae}, %zmm9, %zmm8, %zmm10             #498.13
        vfnmadd231pd {rn-sae}, %zmm6, %zmm8, %zmm10             #499.13
        vpsllq    $48, %zmm7, %zmm6                             #501.15
        vpcmpgtd  4736+__svml_dcosh_ha_data_internal(%rip), %ymm2, %ymm3 #490.23
        vmulpd    {rn-sae}, %zmm10, %zmm10, %zmm2               #500.16
        vmovmskps %ymm3, %esi                                   #491.44
        vmovups   __svml_dcosh_ha_data_internal(%rip), %zmm3    #494.271
        vpermt2pd 64+__svml_dcosh_ha_data_internal(%rip), %zmm7, %zmm3 #494.271
        vpandq    4608+__svml_dcosh_ha_data_internal(%rip), %zmm6, %zmm7 #503.13
        vpaddq    %zmm7, %zmm4, %zmm0                           #508.13
        vpaddq    %zmm7, %zmm3, %zmm1                           #505.13
        vpsubq    %zmm7, %zmm5, %zmm3                           #511.13
        vmovups   704+__svml_dcosh_ha_data_internal(%rip), %zmm4 #515.17
        vaddpd    {rn-sae}, %zmm3, %zmm1, %zmm5                 #514.18
        vsubpd    {rn-sae}, %zmm3, %zmm1, %zmm9                 #513.18
        vfmadd231pd {rn-sae}, %zmm2, %zmm11, %zmm4              #515.17
        vmovups   640+__svml_dcosh_ha_data_internal(%rip), %zmm11 #516.19
        vfmadd213pd {rn-sae}, %zmm12, %zmm2, %zmm4              #517.17
        vfmadd231pd {rn-sae}, %zmm2, %zmm13, %zmm11             #516.19
        vmulpd    {rn-sae}, %zmm2, %zmm4, %zmm13                #519.15
        vfmadd213pd {rn-sae}, %zmm14, %zmm2, %zmm11             #518.19
        vfmadd213pd {rn-sae}, %zmm10, %zmm10, %zmm13            #521.13
        vmulpd    {rn-sae}, %zmm2, %zmm11, %zmm12               #520.17
        vfmadd213pd {rn-sae}, %zmm0, %zmm5, %zmm12              #522.19
        vfmadd213pd {rn-sae}, %zmm12, %zmm9, %zmm13             #523.19
        vaddpd    {rn-sae}, %zmm3, %zmm13, %zmm10               #524.21
        vaddpd    {rn-sae}, %zmm1, %zmm10, %zmm0                #525.20
        testl     %esi, %esi                                    #527.52
        jne       ..B3.3        # Prob 5%                       #527.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm15
..B3.2:                         # Preds ..B3.9 ..B3.7 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #530.12
        popq      %rbp                                          #530.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #530.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #527.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm15
..B3.4:                         # Preds ..B3.3
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #527.249
        movl      %edx, %eax                                    #527.303
        orl       $8064, %eax                                   #527.303
        cmpl      %eax, %edx                                    #527.332
        je        ..B3.6        # Prob 78%                      #527.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm15
..B3.5:                         # Preds ..B3.4
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #527.344
        vldmxcsr  32(%rsp)                                      #527.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm15
..B3.6:                         # Preds ..B3.5 ..B3.4
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm15, 64(%rsp)                              #527.406
        vmovups   %zmm0, 128(%rsp)                              #527.481
        testl     %esi, %esi                                    #527.587
        jne       ..B3.11       # Prob 5%                       #527.587
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B3.7:                         # Preds ..B3.14 ..B3.6
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #527.931
        je        ..B3.2        # Prob 78%                      #527.931
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B3.8:                         # Preds ..B3.7
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #527.956
        movl      32(%rsp), %eax                                #527.956
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B3.9:                         # Preds ..B3.8
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #527.943
        orl       %edx, %eax                                    #527.943
        movl      %eax, 32(%rsp)                                #527.943
        vldmxcsr  32(%rsp)                                      #527.943
        jmp       ..B3.2        # Prob 100%                     #527.943
                                # LOE rbx r12 r13 r14 r15 zmm0
..B3.11:                        # Preds ..B3.6
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #527.667
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B3.18:                        # Preds ..B3.11
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
..B3.12:                        # Preds ..B3.13 ..B3.18
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #527.730
        jc        ..B3.15       # Prob 5%                       #527.730
                                # LOE rbx r15 r12d r13d r14d
..B3.13:                        # Preds ..B3.15 ..B3.12
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #527.683
        cmpl      $8, %ebx                                      #527.678
        jl        ..B3.12       # Prob 82%                      #527.678
                                # LOE rbx r15 r12d r13d r14d
..B3.14:                        # Preds ..B3.13
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
        vmovups   128(%rsp), %zmm0                              #527.887
        jmp       ..B3.7        # Prob 100%                     #527.887
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B3.15:                        # Preds ..B3.12
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,8), %rdi                         #527.759
        lea       128(%rsp,%rbx,8), %rsi                        #527.759
..___tag_value___svml_cosh8_ha_z0.62:
#       __svml_dcosh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcosh_ha_cout_rare_internal            #527.759
..___tag_value___svml_cosh8_ha_z0.63:
        jmp       ..B3.13       # Prob 100%                     #527.759
        .align    16,0x90
                                # LOE r15 ebx r12d r13d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_cosh8_ha_z0,@function
	.size	__svml_cosh8_ha_z0,.-__svml_cosh8_ha_z0
..LN__svml_cosh8_ha_z0.2:
	.data
# -- End  __svml_cosh8_ha_z0
	.text
.L_2__routine_start___svml_cosh2_ha_l9_3:
# -- Begin  __svml_cosh2_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosh2_ha_l9
# --- __svml_cosh2_ha_l9(__m128d)
__svml_cosh2_ha_l9:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #535.1
        .byte     15                                            #609.546
        .byte     30                                            #609.546
        .byte     250                                           #609.546
	.cfi_startproc
..___tag_value___svml_cosh2_ha_l9.65:
..L66:
                                                         #535.1
        pushq     %rbp                                          #535.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #535.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #535.1
        subq      $192, %rsp                                    #535.1
        vmovapd   %xmm0, %xmm4                                  #535.1
        vmovupd   4672+__svml_dcosh_ha_data_internal(%rip), %xmm5 #565.48
        lea       912+__svml_dcosh_ha_data_internal(%rip), %rsi #578.570
        vmovupd   4160+__svml_dcosh_ha_data_internal(%rip), %xmm2 #559.51
        vandnpd   %xmm4, %xmm5, %xmm0                           #569.16
        vmovupd   3968+__svml_dcosh_ha_data_internal(%rip), %xmm1 #570.13
        vfmadd213pd %xmm2, %xmm0, %xmm1                         #570.13
        vmovupd   4032+__svml_dcosh_ha_data_internal(%rip), %xmm3 #567.50
        vpshufd   $221, %xmm0, %xmm6                            #572.18
        vmovq     4224+__svml_dcosh_ha_data_internal(%rip), %xmm10 #560.23
        vmovq     4736+__svml_dcosh_ha_data_internal(%rip), %xmm7 #566.25
        vsubpd    %xmm2, %xmm1, %xmm2                           #581.13
        vfnmadd231pd %xmm3, %xmm2, %xmm0                        #582.13
        vpcmpgtd  %xmm7, %xmm6, %xmm8                           #573.23
        vpshufd   $136, %xmm1, %xmm9                            #576.19
        vpsllq    $45, %xmm1, %xmm1                             #584.15
        vpand     %xmm10, %xmm9, %xmm12                         #577.19
        vpslld    $1, %xmm12, %xmm11                            #578.238
        vpaddd    %xmm12, %xmm11, %xmm13                        #578.365
        vfnmadd231pd 4096+__svml_dcosh_ha_data_internal(%rip), %xmm2, %xmm0 #583.13
        vpslld    $3, %xmm13, %xmm14                            #578.365
        vmovupd   4544+__svml_dcosh_ha_data_internal(%rip), %xmm10 #599.13
        vmovupd   4480+__svml_dcosh_ha_data_internal(%rip), %xmm12 #600.15
        vmovd     %xmm14, %eax                                  #578.435
        vpand     4608+__svml_dcosh_ha_data_internal(%rip), %xmm1, %xmm3 #586.15
        vmulpd    %xmm0, %xmm0, %xmm11                          #598.14
        vmovmskps %xmm8, %edx                                   #574.44
        vmovq     -8(%rax,%rsi), %xmm5                          #579.799
        vmovq     -16(%rax,%rsi), %xmm15                        #578.799
        vfmadd213pd 4416+__svml_dcosh_ha_data_internal(%rip), %xmm11, %xmm10 #599.13
        vfmadd213pd 4352+__svml_dcosh_ha_data_internal(%rip), %xmm11, %xmm12 #600.15
        vmovq     (%rax,%rsi), %xmm6                            #580.796
        vpextrd   $1, %xmm14, %ecx                              #578.499
        vmovhpd   -8(%rcx,%rsi), %xmm5, %xmm8                   #579.767
        vmovhpd   -16(%rcx,%rsi), %xmm15, %xmm7                 #578.767
        vmovhpd   (%rcx,%rsi), %xmm6, %xmm9                     #580.764
        vpaddq    %xmm3, %xmm8, %xmm14                          #591.15
        vpaddq    %xmm3, %xmm7, %xmm1                           #588.15
        vpsubq    %xmm3, %xmm9, %xmm3                           #594.15
        vmulpd    %xmm10, %xmm11, %xmm8                         #601.13
        vmulpd    %xmm12, %xmm11, %xmm11                        #602.15
        vaddpd    %xmm3, %xmm1, %xmm13                          #597.16
        vsubpd    %xmm3, %xmm1, %xmm15                          #596.16
        vfmadd213pd %xmm0, %xmm0, %xmm8                         #604.13
        vfmadd213pd %xmm14, %xmm13, %xmm11                      #603.15
        vfmadd213pd %xmm11, %xmm15, %xmm8                       #605.15
        vaddpd    %xmm8, %xmm3, %xmm0                           #606.15
        vaddpd    %xmm0, %xmm1, %xmm0                           #607.14
        andl      $3, %edx                                      #574.98
        jne       ..B4.3        # Prob 5%                       #609.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm4
..B4.2:                         # Preds ..B4.3 ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #612.12
        popq      %rbp                                          #612.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #612.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm4, 64(%rsp)                               #609.197
        vmovupd   %xmm0, 128(%rsp)                              #609.270
        je        ..B4.2        # Prob 95%                      #609.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #609.454
        movq      %r12, 8(%rsp)                                 #609.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #609.454
        movq      %r13, (%rsp)                                  #609.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #609.454
                                # LOE rbx r12 r14 r15 r13d
..B4.7:                         # Preds ..B4.8 ..B4.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #609.517
        jc        ..B4.10       # Prob 5%                       #609.517
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #609.470
        cmpl      $2, %r12d                                     #609.465
        jl        ..B4.7        # Prob 82%                      #609.465
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #609.674
        jmp       ..B4.2        # Prob 100%                     #609.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #609.546
        lea       128(%rsp,%r12,8), %rsi                        #609.546
..___tag_value___svml_cosh2_ha_l9.83:
#       __svml_dcosh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcosh_ha_cout_rare_internal            #609.546
..___tag_value___svml_cosh2_ha_l9.84:
        jmp       ..B4.8        # Prob 100%                     #609.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cosh2_ha_l9,@function
	.size	__svml_cosh2_ha_l9,.-__svml_cosh2_ha_l9
..LN__svml_cosh2_ha_l9.3:
	.data
# -- End  __svml_cosh2_ha_l9
	.text
.L_2__routine_start___svml_cosh1_ha_l9_4:
# -- Begin  __svml_cosh1_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosh1_ha_l9
# --- __svml_cosh1_ha_l9(__m128d)
__svml_cosh1_ha_l9:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #617.1
        .byte     15                                            #691.546
        .byte     30                                            #691.546
        .byte     250                                           #691.546
	.cfi_startproc
..___tag_value___svml_cosh1_ha_l9.86:
..L87:
                                                         #617.1
        pushq     %rbp                                          #617.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #617.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #617.1
        subq      $128, %rsp                                    #617.1
        vmovapd   %xmm0, %xmm4                                  #617.1
        vmovsd    4672+__svml_dcosh_ha_data_internal(%rip), %xmm5 #647.17
        movl      $127, %edx                                    #659.19
        vandnpd   %xmm4, %xmm5, %xmm2                           #651.16
        movl      $1082531225, %eax                             #655.23
        vmovsd    4160+__svml_dcosh_ha_data_internal(%rip), %xmm1 #641.20
        vmovapd   %xmm2, %xmm3                                  #652.13
        vfmadd132sd 3968+__svml_dcosh_ha_data_internal(%rip), %xmm1, %xmm3 #652.13
        lea       912+__svml_dcosh_ha_data_internal(%rip), %rsi #660.610
        vmovsd    4096+__svml_dcosh_ha_data_internal(%rip), %xmm0 #665.13
        vmovd     %edx, %xmm10                                  #659.19
        vpshufd   $85, %xmm2, %xmm6                             #654.18
        vmovd     %eax, %xmm7                                   #655.23
        vpcmpgtd  %xmm7, %xmm6, %xmm8                           #655.23
        vmovmskps %xmm8, %eax                                   #656.44
        vpshufd   $0, %xmm3, %xmm9                              #658.19
        vsubsd    %xmm1, %xmm3, %xmm15                          #663.13
        vmovsd    4032+__svml_dcosh_ha_data_internal(%rip), %xmm1 #664.13
        vpand     %xmm10, %xmm9, %xmm12                         #659.19
        vfnmadd213sd %xmm2, %xmm15, %xmm1                       #664.13
        vpslld    $1, %xmm12, %xmm11                            #660.238
        vpaddd    %xmm12, %xmm11, %xmm13                        #660.365
        vpsllq    $45, %xmm3, %xmm3                             #666.15
        vpslld    $3, %xmm13, %xmm14                            #660.365
        vmovd     %xmm14, %ecx                                  #660.435
        vmovsd    4544+__svml_dcosh_ha_data_internal(%rip), %xmm9 #681.13
        vmovsd    4480+__svml_dcosh_ha_data_internal(%rip), %xmm10 #682.15
        vfnmadd213sd %xmm1, %xmm15, %xmm0                       #665.13
        vmovq     4608+__svml_dcosh_ha_data_internal(%rip), %xmm2 #667.21
        vmovq     -16(%rcx,%rsi), %xmm5                         #660.610
        vpand     %xmm2, %xmm3, %xmm8                           #668.15
        vmovq     (%rsi,%rcx), %xmm7                            #662.607
        vpaddq    %xmm8, %xmm5, %xmm1                           #670.15
        vpsubq    %xmm8, %xmm7, %xmm15                          #676.15
        vmulsd    %xmm0, %xmm0, %xmm11                          #680.14
        vaddsd    %xmm15, %xmm1, %xmm12                         #679.16
        vsubsd    %xmm15, %xmm1, %xmm14                         #678.16
        vfmadd213sd 4416+__svml_dcosh_ha_data_internal(%rip), %xmm11, %xmm9 #681.13
        vfmadd213sd 4352+__svml_dcosh_ha_data_internal(%rip), %xmm11, %xmm10 #682.15
        vmovq     -8(%rsi,%rcx), %xmm6                          #661.610
        vpaddq    %xmm8, %xmm6, %xmm13                          #673.15
        vmulsd    %xmm11, %xmm9, %xmm9                          #683.13
        vmulsd    %xmm11, %xmm10, %xmm10                        #684.15
        vfmadd213sd %xmm0, %xmm0, %xmm9                         #686.13
        vfmadd213sd %xmm13, %xmm12, %xmm10                      #685.15
        vfmadd213sd %xmm10, %xmm14, %xmm9                       #687.15
        vaddsd    %xmm15, %xmm9, %xmm0                          #688.15
        vaddsd    %xmm1, %xmm0, %xmm0                           #689.14
        andl      $1, %eax                                      #656.98
        jne       ..B5.3        # Prob 5%                       #691.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm4
..B5.2:                         # Preds ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #694.12
        popq      %rbp                                          #694.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #694.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm4, (%rsp)                                 #691.160
        vmovsd    %xmm0, 64(%rsp)                               #691.233
        jne       ..B5.5        # Prob 5%                       #691.374
                                # LOE rbx r12 r13 r14 r15 eax
..B5.4:                         # Preds ..B5.6 ..B5.5 ..B5.3
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm0                               #691.626
        movq      %rbp, %rsp                                    #691.626
        popq      %rbp                                          #691.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #691.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B5.5:                         # Preds ..B5.3
                                # Execution count [2.50e-03]: Infreq
        je        ..B5.4        # Prob 95%                      #691.517
                                # LOE rbx r12 r13 r14 r15
..B5.6:                         # Preds ..B5.5
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #691.546
        lea       64(%rsp), %rsi                                #691.546
..___tag_value___svml_cosh1_ha_l9.99:
#       __svml_dcosh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcosh_ha_cout_rare_internal            #691.546
..___tag_value___svml_cosh1_ha_l9.100:
        jmp       ..B5.4        # Prob 100%                     #691.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_cosh1_ha_l9,@function
	.size	__svml_cosh1_ha_l9,.-__svml_cosh1_ha_l9
..LN__svml_cosh1_ha_l9.4:
	.data
# -- End  __svml_cosh1_ha_l9
	.text
.L_2__routine_start___svml_cosh4_ha_e9_5:
# -- Begin  __svml_cosh4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosh4_ha_e9
# --- __svml_cosh4_ha_e9(__m256d)
__svml_cosh4_ha_e9:
# parameter 1: %ymm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #954.1
        .byte     15                                            #1028.552
        .byte     30                                            #1028.552
        .byte     250                                           #1028.552
	.cfi_startproc
..___tag_value___svml_cosh4_ha_e9.102:
..L103:
                                                        #954.1
        pushq     %rbp                                          #954.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #954.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #954.1
        subq      $192, %rsp                                    #954.1
        lea       912+__svml_dcosh_ha_data_internal(%rip), %r8  #997.750
        vmovupd   4672+__svml_dcosh_ha_data_internal(%rip), %ymm3 #984.51
        vmovupd   4160+__svml_dcosh_ha_data_internal(%rip), %ymm4 #978.54
        vmovapd   %ymm0, %ymm2                                  #954.1
        vandnpd   %ymm2, %ymm3, %ymm6                           #988.16
        vmulpd    3968+__svml_dcosh_ha_data_internal(%rip), %ymm6, %ymm9 #989.28
        vaddpd    %ymm9, %ymm4, %ymm14                          #989.13
        vextractf128 $1, %ymm14, %xmm15                         #994.104
        vshufps   $136, %xmm15, %xmm14, %xmm8                   #995.41
        vpsllq    $45, %xmm15, %xmm15                           #1003.74
        vpand     4224+__svml_dcosh_ha_data_internal(%rip), %xmm8, %xmm12 #996.23
        vpslld    $1, %xmm12, %xmm10                            #997.242
        vpaddd    %xmm12, %xmm10, %xmm11                        #997.389
        vpslld    $3, %xmm11, %xmm3                             #997.389
        vmovd     %xmm3, %edx                                   #997.463
        vpextrd   $1, %xmm3, %ecx                               #997.531
        vpextrd   $2, %xmm3, %esi                               #997.602
        vpextrd   $3, %xmm3, %edi                               #997.673
        vsubpd    %ymm4, %ymm14, %ymm3                          #1000.13
        vpsllq    $45, %xmm14, %xmm14                           #1003.19
        vmovq     -16(%rsi,%r8), %xmm9                          #997.1447
        vmovhpd   -16(%rdi,%r8), %xmm9, %xmm11                  #997.1415
        vmulpd    4032+__svml_dcosh_ha_data_internal(%rip), %ymm3, %ymm4 #1001.38
        vmovq     (%rsi,%r8), %xmm8                             #999.1444
        vmovq     -16(%rdx,%r8), %xmm13                         #997.1247
        vmovhpd   -16(%rcx,%r8), %xmm13, %xmm13                 #997.1215
        vextractf128 $1, %ymm6, %xmm5                           #990.107
        vshufps   $221, %xmm5, %xmm6, %xmm1                     #991.40
        vsubpd    %ymm4, %ymm6, %ymm6                           #1001.13
        vpcmpgtd  4736+__svml_dcosh_ha_data_internal(%rip), %xmm1, %xmm7 #992.27
        vmovq     -8(%rsi,%r8), %xmm1                           #998.1447
        vmulpd    4096+__svml_dcosh_ha_data_internal(%rip), %ymm3, %ymm4 #1002.35
        vmovq     -8(%rdx,%r8), %xmm5                           #998.1247
        vmovhpd   -8(%rdi,%r8), %xmm1, %xmm10                   #998.1415
        vmovhpd   (%rdi,%r8), %xmm8, %xmm1                      #999.1412
        vmovhpd   -8(%rcx,%r8), %xmm5, %xmm12                   #998.1215
        vsubpd    %ymm4, %ymm6, %ymm9                           #1002.13
        vmovupd   4608+__svml_dcosh_ha_data_internal(%rip), %xmm6 #1004.25
        vpand     %xmm6, %xmm14, %xmm8                          #1005.19
        vpaddq    %xmm8, %xmm12, %xmm5                          #1010.19
        vpaddq    %xmm8, %xmm13, %xmm4                          #1007.19
        vmulpd    %ymm9, %ymm9, %ymm12                          #1017.14
        vmovmskps %xmm7, %eax                                   #993.44
        vmovq     (%rdx,%r8), %xmm7                             #999.1244
        vmovhpd   (%rcx,%r8), %xmm7, %xmm0                      #999.1212
        vpand     %xmm6, %xmm15, %xmm7                          #1005.82
        vpaddq    %xmm7, %xmm11, %xmm3                          #1007.76
        vpaddq    %xmm7, %xmm10, %xmm10                         #1010.76
        vpsubq    %xmm7, %xmm1, %xmm1                           #1013.76
        vpsubq    %xmm8, %xmm0, %xmm0                           #1013.19
        vmulpd    4544+__svml_dcosh_ha_data_internal(%rip), %ymm12, %ymm7 #1018.28
        vmulpd    4480+__svml_dcosh_ha_data_internal(%rip), %ymm12, %ymm8 #1019.30
        vaddpd    4416+__svml_dcosh_ha_data_internal(%rip), %ymm7, %ymm11 #1018.13
        vaddpd    4352+__svml_dcosh_ha_data_internal(%rip), %ymm8, %ymm13 #1019.15
        vmulpd    %ymm13, %ymm12, %ymm15                        #1021.15
        vinsertf128 $1, %xmm3, %ymm4, %ymm6                     #1008.17
        vinsertf128 $1, %xmm1, %ymm0, %ymm4                     #1014.14
        vmulpd    %ymm11, %ymm12, %ymm0                         #1020.13
        vaddpd    %ymm4, %ymm6, %ymm14                          #1016.16
        vsubpd    %ymm4, %ymm6, %ymm3                           #1015.16
        vmulpd    %ymm0, %ymm9, %ymm1                           #1023.28
        vmulpd    %ymm15, %ymm14, %ymm12                        #1022.30
        vaddpd    %ymm1, %ymm9, %ymm9                           #1023.13
        vmulpd    %ymm9, %ymm3, %ymm0                           #1024.30
        vinsertf128 $1, %xmm10, %ymm5, %ymm5                    #1011.17
        vaddpd    %ymm12, %ymm5, %ymm5                          #1022.15
        vaddpd    %ymm0, %ymm5, %ymm1                           #1024.15
        vaddpd    %ymm1, %ymm4, %ymm3                           #1025.15
        vaddpd    %ymm3, %ymm6, %ymm0                           #1026.14
        testl     %eax, %eax                                    #1028.52
        jne       ..B6.3        # Prob 5%                       #1028.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm2
..B6.2:                         # Preds ..B6.3 ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1031.12
        popq      %rbp                                          #1031.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1031.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm2, 64(%rsp)                               #1028.200
        vmovupd   %ymm0, 128(%rsp)                              #1028.276
        je        ..B6.2        # Prob 95%                      #1028.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1028.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B6.13:                        # Preds ..B6.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1028.523
        jc        ..B6.10       # Prob 5%                       #1028.523
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1028.476
        cmpl      $4, %r12d                                     #1028.471
        jl        ..B6.7        # Prob 82%                      #1028.471
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1028.683
        jmp       ..B6.2        # Prob 100%                     #1028.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1028.552
        lea       128(%rsp,%r12,8), %rsi                        #1028.552
..___tag_value___svml_cosh4_ha_e9.120:
#       __svml_dcosh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcosh_ha_cout_rare_internal            #1028.552
..___tag_value___svml_cosh4_ha_e9.121:
        jmp       ..B6.8        # Prob 100%                     #1028.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cosh4_ha_e9,@function
	.size	__svml_cosh4_ha_e9,.-__svml_cosh4_ha_e9
..LN__svml_cosh4_ha_e9.5:
	.data
# -- End  __svml_cosh4_ha_e9
	.text
.L_2__routine_start___svml_cosh1_ha_ex_6:
# -- Begin  __svml_cosh1_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosh1_ha_ex
# --- __svml_cosh1_ha_ex(__m128d)
__svml_cosh1_ha_ex:
# parameter 1: %xmm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1036.1
        .byte     15                                            #1110.546
        .byte     30                                            #1110.546
        .byte     250                                           #1110.546
	.cfi_startproc
..___tag_value___svml_cosh1_ha_ex.123:
..L124:
                                                        #1036.1
        pushq     %rbp                                          #1036.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1036.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1036.1
        subq      $128, %rsp                                    #1036.1
        movl      $127, %edx                                    #1078.17
        movsd     4672+__svml_dcosh_ha_data_internal(%rip), %xmm1 #1070.16
        movl      $1082531225, %eax                             #1074.21
        andnps    %xmm0, %xmm1                                  #1070.16
        lea       912+__svml_dcosh_ha_data_internal(%rip), %rsi #1079.594
        movaps    %xmm1, %xmm12                                 #1071.25
        mulsd     3968+__svml_dcosh_ha_data_internal(%rip), %xmm12 #1071.25
        movd      %edx, %xmm4                                   #1078.17
        movsd     4160+__svml_dcosh_ha_data_internal(%rip), %xmm7 #1060.20
        movd      %eax, %xmm2                                   #1074.21
        movsd     4032+__svml_dcosh_ha_data_internal(%rip), %xmm8 #1083.35
        addsd     %xmm7, %xmm12                                 #1071.13
        movaps    %xmm12, %xmm9                                 #1082.13
        movsd     4096+__svml_dcosh_ha_data_internal(%rip), %xmm10 #1084.32
        subsd     %xmm7, %xmm9                                  #1082.13
        mulsd     %xmm9, %xmm8                                  #1083.35
        mulsd     %xmm9, %xmm10                                 #1084.32
        pshufd    $85, %xmm1, %xmm3                             #1073.16
        subsd     %xmm8, %xmm1                                  #1083.13
        pshufd    $0, %xmm12, %xmm5                             #1077.17
        pcmpgtd   %xmm2, %xmm3                                  #1074.21
        pand      %xmm4, %xmm5                                  #1078.17
        psllq     $45, %xmm12                                   #1085.13
        movsd     4544+__svml_dcosh_ha_data_internal(%rip), %xmm4 #1100.25
        movdqa    %xmm5, %xmm6                                  #1079.236
        pslld     $1, %xmm6                                     #1079.236
        subsd     %xmm10, %xmm1                                 #1084.13
        movmskps  %xmm3, %eax                                   #1075.44
        movaps    %xmm1, %xmm13                                 #1099.14
        paddd     %xmm5, %xmm6                                  #1079.353
        mulsd     %xmm1, %xmm13                                 #1099.14
        pslld     $3, %xmm6                                     #1079.353
        movd      %xmm6, %ecx                                   #1079.421
        movsd     4480+__svml_dcosh_ha_data_internal(%rip), %xmm8 #1101.27
        mulsd     %xmm13, %xmm4                                 #1100.25
        mulsd     %xmm13, %xmm8                                 #1101.27
        addsd     4416+__svml_dcosh_ha_data_internal(%rip), %xmm4 #1100.13
        addsd     4352+__svml_dcosh_ha_data_internal(%rip), %xmm8 #1101.15
        mulsd     %xmm13, %xmm4                                 #1102.13
        mulsd     %xmm13, %xmm8                                 #1103.15
        mulsd     %xmm1, %xmm4                                  #1105.25
        movq      4608+__svml_dcosh_ha_data_internal(%rip), %xmm11 #1086.19
        addsd     %xmm1, %xmm4                                  #1105.13
        movq      -16(%rcx,%rsi), %xmm3                         #1079.594
        pand      %xmm11, %xmm12                                #1087.13
        movq      (%rsi,%rcx), %xmm2                            #1081.591
        paddq     %xmm12, %xmm3                                 #1089.13
        psubq     %xmm12, %xmm2                                 #1095.13
        movdqa    %xmm3, %xmm14                                 #1098.16
        movdqa    %xmm3, %xmm7                                  #1097.16
        movq      -8(%rsi,%rcx), %xmm15                         #1080.594
        addsd     %xmm2, %xmm14                                 #1098.16
        subsd     %xmm2, %xmm7                                  #1097.16
        paddq     %xmm12, %xmm15                                #1092.13
        mulsd     %xmm14, %xmm8                                 #1104.27
        mulsd     %xmm7, %xmm4                                  #1106.27
        addsd     %xmm15, %xmm8                                 #1104.15
        addsd     %xmm8, %xmm4                                  #1106.15
        addsd     %xmm2, %xmm4                                  #1107.15
        addsd     %xmm3, %xmm4                                  #1108.14
        andl      $1, %eax                                      #1075.98
        jne       ..B7.3        # Prob 5%                       #1110.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm4
..B7.2:                         # Preds ..B7.4 ..B7.1
                                # Execution count [1.00e+00]
        movaps    %xmm4, %xmm0                                  #1113.12
        movq      %rbp, %rsp                                    #1113.12
        popq      %rbp                                          #1113.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1113.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.3:                         # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm0, (%rsp)                                 #1110.160
        movsd     %xmm4, 64(%rsp)                               #1110.233
        jne       ..B7.6        # Prob 5%                       #1110.374
                                # LOE rbx r12 r13 r14 r15
..B7.4:                         # Preds ..B7.6 ..B7.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm4                               #1110.626
        jmp       ..B7.2        # Prob 100%                     #1110.626
                                # LOE rbx r12 r13 r14 r15 xmm4
..B7.6:                         # Preds ..B7.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1110.546
        lea       64(%rsp), %rsi                                #1110.546
..___tag_value___svml_cosh1_ha_ex.132:
#       __svml_dcosh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcosh_ha_cout_rare_internal            #1110.546
..___tag_value___svml_cosh1_ha_ex.133:
        jmp       ..B7.4        # Prob 100%                     #1110.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_cosh1_ha_ex,@function
	.size	__svml_cosh1_ha_ex,.-__svml_cosh1_ha_ex
..LN__svml_cosh1_ha_ex.6:
	.data
# -- End  __svml_cosh1_ha_ex
	.text
.L_2__routine_start___svml_cosh2_ha_ex_7:
# -- Begin  __svml_cosh2_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosh2_ha_ex
# --- __svml_cosh2_ha_ex(__m128d)
__svml_cosh2_ha_ex:
# parameter 1: %xmm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1118.1
        .byte     15                                            #1192.546
        .byte     30                                            #1192.546
        .byte     250                                           #1192.546
	.cfi_startproc
..___tag_value___svml_cosh2_ha_ex.135:
..L136:
                                                        #1118.1
        pushq     %rbp                                          #1118.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1118.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1118.1
        subq      $192, %rsp                                    #1118.1
        movaps    %xmm0, %xmm2                                  #1118.1
        movups    4672+__svml_dcosh_ha_data_internal(%rip), %xmm1 #1152.16
        lea       912+__svml_dcosh_ha_data_internal(%rip), %rsi #1161.591
        movups    3968+__svml_dcosh_ha_data_internal(%rip), %xmm13 #1153.25
        andnps    %xmm2, %xmm1                                  #1152.16
        mulpd     %xmm1, %xmm13                                 #1153.25
        movups    4160+__svml_dcosh_ha_data_internal(%rip), %xmm9 #1142.51
        addpd     %xmm9, %xmm13                                 #1153.13
        movaps    %xmm13, %xmm11                                #1164.13
        subpd     %xmm9, %xmm11                                 #1164.13
        movups    4032+__svml_dcosh_ha_data_internal(%rip), %xmm10 #1165.35
        mulpd     %xmm11, %xmm10                                #1165.35
        movups    4096+__svml_dcosh_ha_data_internal(%rip), %xmm12 #1166.32
        mulpd     %xmm11, %xmm12                                #1166.32
        pshufd    $221, %xmm1, %xmm4                            #1155.16
        subpd     %xmm10, %xmm1                                 #1165.13
        subpd     %xmm12, %xmm1                                 #1166.13
        movaps    %xmm1, %xmm15                                 #1181.14
        mulpd     %xmm1, %xmm15                                 #1181.14
        movups    4544+__svml_dcosh_ha_data_internal(%rip), %xmm10 #1182.25
        mulpd     %xmm15, %xmm10                                #1182.25
        movups    4480+__svml_dcosh_ha_data_internal(%rip), %xmm14 #1183.27
        addpd     4416+__svml_dcosh_ha_data_internal(%rip), %xmm10 #1182.13
        mulpd     %xmm15, %xmm14                                #1183.27
        mulpd     %xmm15, %xmm10                                #1184.13
        addpd     4352+__svml_dcosh_ha_data_internal(%rip), %xmm14 #1183.15
        mulpd     %xmm1, %xmm10                                 #1187.25
        mulpd     %xmm14, %xmm15                                #1185.15
        addpd     %xmm10, %xmm1                                 #1187.13
        movq      4224+__svml_dcosh_ha_data_internal(%rip), %xmm5 #1143.21
        pshufd    $136, %xmm13, %xmm6                           #1159.17
        psllq     $45, %xmm13                                   #1167.13
        pand      %xmm5, %xmm6                                  #1160.17
        movdqa    %xmm6, %xmm7                                  #1161.236
        pslld     $1, %xmm7                                     #1161.236
        paddd     %xmm6, %xmm7                                  #1161.353
        pslld     $3, %xmm7                                     #1161.353
        movd      %xmm7, %eax                                   #1161.421
        pshufd    $1, %xmm7, %xmm8                              #1161.502
        movq      4736+__svml_dcosh_ha_data_internal(%rip), %xmm3 #1149.23
        movd      %xmm8, %ecx                                   #1161.483
        pcmpgtd   %xmm3, %xmm4                                  #1156.21
        movq      -16(%rax,%rsi), %xmm0                         #1161.820
        pand      4608+__svml_dcosh_ha_data_internal(%rip), %xmm13 #1169.13
        movmskps  %xmm4, %edx                                   #1157.44
        movhpd    -16(%rcx,%rsi), %xmm0                         #1161.788
        movq      (%rax,%rsi), %xmm4                            #1163.817
        movhpd    (%rcx,%rsi), %xmm4                            #1163.785
        paddq     %xmm13, %xmm0                                 #1171.13
        psubq     %xmm13, %xmm4                                 #1177.13
        movaps    %xmm0, %xmm9                                  #1180.16
        movaps    %xmm0, %xmm11                                 #1179.16
        addpd     %xmm4, %xmm9                                  #1180.16
        subpd     %xmm4, %xmm11                                 #1179.16
        mulpd     %xmm15, %xmm9                                 #1186.27
        mulpd     %xmm1, %xmm11                                 #1188.27
        movq      -8(%rax,%rsi), %xmm3                          #1162.820
        movhpd    -8(%rcx,%rsi), %xmm3                          #1162.788
        paddq     %xmm13, %xmm3                                 #1174.13
        addpd     %xmm9, %xmm3                                  #1186.15
        addpd     %xmm11, %xmm3                                 #1188.15
        addpd     %xmm3, %xmm4                                  #1189.15
        addpd     %xmm4, %xmm0                                  #1190.14
        andl      $3, %edx                                      #1157.98
        jne       ..B8.3        # Prob 5%                       #1192.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm2
..B8.2:                         # Preds ..B8.3 ..B8.9 ..B8.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1195.12
        popq      %rbp                                          #1195.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1195.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.3:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm2, 64(%rsp)                               #1192.197
        movups    %xmm0, 128(%rsp)                              #1192.270
        je        ..B8.2        # Prob 95%                      #1192.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B8.6:                         # Preds ..B8.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1192.454
        movq      %r12, 8(%rsp)                                 #1192.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1192.454
        movq      %r13, (%rsp)                                  #1192.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1192.454
                                # LOE rbx r12 r14 r15 r13d
..B8.7:                         # Preds ..B8.8 ..B8.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1192.517
        jc        ..B8.10       # Prob 5%                       #1192.517
                                # LOE rbx r12 r14 r15 r13d
..B8.8:                         # Preds ..B8.10 ..B8.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1192.470
        cmpl      $2, %r12d                                     #1192.465
        jl        ..B8.7        # Prob 82%                      #1192.465
                                # LOE rbx r12 r14 r15 r13d
..B8.9:                         # Preds ..B8.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1192.674
        jmp       ..B8.2        # Prob 100%                     #1192.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B8.10:                        # Preds ..B8.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1192.546
        lea       128(%rsp,%r12,8), %rsi                        #1192.546
..___tag_value___svml_cosh2_ha_ex.153:
#       __svml_dcosh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcosh_ha_cout_rare_internal            #1192.546
..___tag_value___svml_cosh2_ha_ex.154:
        jmp       ..B8.8        # Prob 100%                     #1192.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cosh2_ha_ex,@function
	.size	__svml_cosh2_ha_ex,.-__svml_cosh2_ha_ex
..LN__svml_cosh2_ha_ex.7:
	.data
# -- End  __svml_cosh2_ha_ex
	.text
.L_2__routine_start___svml_cosh2_ha_e9_8:
# -- Begin  __svml_cosh2_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosh2_ha_e9
# --- __svml_cosh2_ha_e9(__m128d)
__svml_cosh2_ha_e9:
# parameter 1: %xmm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1200.1
        .byte     15                                            #1274.546
        .byte     30                                            #1274.546
        .byte     250                                           #1274.546
	.cfi_startproc
..___tag_value___svml_cosh2_ha_e9.156:
..L157:
                                                        #1200.1
        pushq     %rbp                                          #1200.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1200.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1200.1
        subq      $192, %rsp                                    #1200.1
        lea       912+__svml_dcosh_ha_data_internal(%rip), %rsi #1243.606
        movups    4672+__svml_dcosh_ha_data_internal(%rip), %xmm1 #1234.16
        movups    3968+__svml_dcosh_ha_data_internal(%rip), %xmm11 #1235.25
        andnps    %xmm0, %xmm1                                  #1234.16
        mulpd     %xmm1, %xmm11                                 #1235.25
        movups    4160+__svml_dcosh_ha_data_internal(%rip), %xmm7 #1224.51
        addpd     %xmm7, %xmm11                                 #1235.13
        movaps    %xmm11, %xmm9                                 #1246.13
        subpd     %xmm7, %xmm9                                  #1246.13
        movups    4032+__svml_dcosh_ha_data_internal(%rip), %xmm8 #1247.35
        mulpd     %xmm9, %xmm8                                  #1247.35
        movups    4096+__svml_dcosh_ha_data_internal(%rip), %xmm10 #1248.32
        mulpd     %xmm9, %xmm10                                 #1248.32
        pshufd    $221, %xmm1, %xmm3                            #1237.22
        subpd     %xmm8, %xmm1                                  #1247.13
        subpd     %xmm10, %xmm1                                 #1248.13
        movaps    %xmm1, %xmm13                                 #1263.14
        mulpd     %xmm1, %xmm13                                 #1263.14
        movups    4544+__svml_dcosh_ha_data_internal(%rip), %xmm15 #1264.25
        mulpd     %xmm13, %xmm15                                #1264.25
        movups    4480+__svml_dcosh_ha_data_internal(%rip), %xmm12 #1265.27
        addpd     4416+__svml_dcosh_ha_data_internal(%rip), %xmm15 #1264.13
        mulpd     %xmm13, %xmm12                                #1265.27
        mulpd     %xmm13, %xmm15                                #1266.13
        addpd     4352+__svml_dcosh_ha_data_internal(%rip), %xmm12 #1265.15
        mulpd     %xmm1, %xmm15                                 #1269.25
        mulpd     %xmm12, %xmm13                                #1267.15
        addpd     %xmm15, %xmm1                                 #1269.13
        movq      4224+__svml_dcosh_ha_data_internal(%rip), %xmm4 #1225.27
        pshufd    $136, %xmm11, %xmm5                           #1241.23
        psllq     $45, %xmm11                                   #1249.19
        pand      %xmm4, %xmm5                                  #1242.23
        movdqa    %xmm5, %xmm6                                  #1243.242
        pslld     $1, %xmm6                                     #1243.242
        paddd     %xmm5, %xmm6                                  #1243.389
        pslld     $3, %xmm6                                     #1243.389
        movd      %xmm6, %eax                                   #1243.463
        movq      4736+__svml_dcosh_ha_data_internal(%rip), %xmm2 #1231.29
        pcmpgtd   %xmm2, %xmm3                                  #1238.27
        pextrd    $1, %xmm6, %ecx                               #1243.531
        movq      -16(%rax,%rsi), %xmm4                         #1243.835
        movmskps  %xmm3, %edx                                   #1239.44
        movq      (%rax,%rsi), %xmm3                            #1245.832
        movhpd    -16(%rcx,%rsi), %xmm4                         #1243.803
        pand      4608+__svml_dcosh_ha_data_internal(%rip), %xmm11 #1251.19
        movhpd    (%rcx,%rsi), %xmm3                            #1245.800
        paddq     %xmm11, %xmm4                                 #1253.19
        psubq     %xmm11, %xmm3                                 #1259.19
        movaps    %xmm4, %xmm14                                 #1262.16
        movaps    %xmm4, %xmm7                                  #1261.16
        addpd     %xmm3, %xmm14                                 #1262.16
        subpd     %xmm3, %xmm7                                  #1261.16
        mulpd     %xmm13, %xmm14                                #1268.27
        mulpd     %xmm1, %xmm7                                  #1270.27
        movq      -8(%rax,%rsi), %xmm2                          #1244.835
        movhpd    -8(%rcx,%rsi), %xmm2                          #1244.803
        paddq     %xmm11, %xmm2                                 #1256.19
        addpd     %xmm14, %xmm2                                 #1268.15
        addpd     %xmm7, %xmm2                                  #1270.15
        addpd     %xmm2, %xmm3                                  #1271.15
        addpd     %xmm3, %xmm4                                  #1272.14
        andl      $3, %edx                                      #1239.98
        jne       ..B9.3        # Prob 5%                       #1274.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm4
..B9.2:                         # Preds ..B9.3 ..B9.9 ..B9.1
                                # Execution count [1.00e+00]
        movaps    %xmm4, %xmm0                                  #1277.12
        movq      %rbp, %rsp                                    #1277.12
        popq      %rbp                                          #1277.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1277.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.3:                         # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #1274.197
        movups    %xmm4, 128(%rsp)                              #1274.270
        je        ..B9.2        # Prob 95%                      #1274.374
                                # LOE rbx r12 r13 r14 r15 edx xmm4
..B9.6:                         # Preds ..B9.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1274.454
        movq      %r12, 8(%rsp)                                 #1274.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1274.454
        movq      %r13, (%rsp)                                  #1274.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1274.454
                                # LOE rbx r12 r14 r15 r13d
..B9.7:                         # Preds ..B9.8 ..B9.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1274.517
        jc        ..B9.10       # Prob 5%                       #1274.517
                                # LOE rbx r12 r14 r15 r13d
..B9.8:                         # Preds ..B9.10 ..B9.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1274.470
        cmpl      $2, %r12d                                     #1274.465
        jl        ..B9.7        # Prob 82%                      #1274.465
                                # LOE rbx r12 r14 r15 r13d
..B9.9:                         # Preds ..B9.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm4                              #1274.674
        jmp       ..B9.2        # Prob 100%                     #1274.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm4
..B9.10:                        # Preds ..B9.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1274.546
        lea       128(%rsp,%r12,8), %rsi                        #1274.546
..___tag_value___svml_cosh2_ha_e9.174:
#       __svml_dcosh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcosh_ha_cout_rare_internal            #1274.546
..___tag_value___svml_cosh2_ha_e9.175:
        jmp       ..B9.8        # Prob 100%                     #1274.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cosh2_ha_e9,@function
	.size	__svml_cosh2_ha_e9,.-__svml_cosh2_ha_e9
..LN__svml_cosh2_ha_e9.8:
	.data
# -- End  __svml_cosh2_ha_e9
	.text
.L_2__routine_start___svml_dcosh_ha_cout_rare_internal_9:
# -- Begin  __svml_dcosh_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dcosh_ha_cout_rare_internal
	.globl __svml_dcosh_ha_cout_rare_internal
# --- __svml_dcosh_ha_cout_rare_internal(const double *, double *)
__svml_dcosh_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #844.1
        .byte     15                                            #947.11
        .byte     30                                            #947.11
        .byte     250                                           #947.11
	.cfi_startproc
..___tag_value___svml_dcosh_ha_cout_rare_internal.177:
..L178:
                                                        #844.1
        movq      %rsi, %r8                                     #844.1
        movzwl    6(%rdi), %edx                                 #857.31
        xorl      %eax, %eax                                    #855.14
        andl      $32752, %edx                                  #857.31
        cmpl      $32752, %edx                                  #857.57
        je        ..B10.12      # Prob 16%                      #857.57
                                # LOE rbx rbp rdi r8 r12 r13 r14 r15 eax
..B10.2:                        # Preds ..B10.1
                                # Execution count [8.40e-01]
        movq      (%rdi), %rdx                                  #859.19
        movq      %rdx, -8(%rsp)                                #859.9
        shrq      $56, %rdx                                     #860.30
        andl      $127, %edx                                    #860.30
        movb      %dl, -1(%rsp)                                 #860.30
        movzwl    -2(%rsp), %ecx                                #861.35
        andl      $32752, %ecx                                  #861.35
        cmpl      $15504, %ecx                                  #861.60
        jle       ..B10.10      # Prob 50%                      #861.60
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax
..B10.3:                        # Preds ..B10.2
                                # Execution count [4.20e-01]
        movsd     -8(%rsp), %xmm0                               #863.19
        movsd     1096+_vmldCoshHATab(%rip), %xmm1              #863.44
        comisd    %xmm0, %xmm1                                  #863.44
        jbe       ..B10.9       # Prob 50%                      #863.44
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax xmm0
..B10.4:                        # Preds ..B10.3
                                # Execution count [2.10e-01]
        movq      1128+_vmldCoshHATab(%rip), %rdx               #856.5
        movq      %rdx, -8(%rsp)                                #856.5
        comisd    1144+_vmldCoshHATab(%rip), %xmm0              #865.50
        jb        ..B10.8       # Prob 50%                      #865.50
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax xmm0
..B10.5:                        # Preds ..B10.4
                                # Execution count [1.05e-01]
        movsd     1040+_vmldCoshHATab(%rip), %xmm1              #867.56
        lea       _vmldCoshHATab(%rip), %r9                     #882.59
        mulsd     %xmm0, %xmm1                                  #867.56
        addsd     1048+_vmldCoshHATab(%rip), %xmm1              #868.62
        movsd     %xmm1, -40(%rsp)                              #868.25
        movsd     -40(%rsp), %xmm2                              #869.38
        movsd     1088+_vmldCoshHATab(%rip), %xmm1              #874.59
        movl      -40(%rsp), %edx                               #876.50
        movl      %edx, %esi                                    #877.33
        andl      $63, %esi                                     #877.33
        subsd     1048+_vmldCoshHATab(%rip), %xmm2              #869.60
        movsd     %xmm2, -32(%rsp)                              #869.25
        lea       (%rsi,%rsi), %ecx                             #880.64
        movsd     -32(%rsp), %xmm3                              #870.40
        lea       1(%rsi,%rsi), %edi                            #881.83
        mulsd     1104+_vmldCoshHATab(%rip), %xmm3              #870.61
        movsd     -32(%rsp), %xmm4                              #871.40
        subsd     %xmm3, %xmm0                                  #872.46
        mulsd     1112+_vmldCoshHATab(%rip), %xmm4              #871.61
        shrl      $6, %edx                                      #878.43
        subsd     %xmm4, %xmm0                                  #873.42
        mulsd     %xmm0, %xmm1                                  #874.82
        addl      $1022, %edx                                   #883.43
        andl      $2047, %edx                                   #883.48
        addsd     1080+_vmldCoshHATab(%rip), %xmm1              #874.103
        mulsd     %xmm0, %xmm1                                  #874.127
        addsd     1072+_vmldCoshHATab(%rip), %xmm1              #874.148
        mulsd     %xmm0, %xmm1                                  #874.172
        addsd     1064+_vmldCoshHATab(%rip), %xmm1              #874.193
        mulsd     %xmm0, %xmm1                                  #874.217
        addsd     1056+_vmldCoshHATab(%rip), %xmm1              #874.238
        mulsd     %xmm0, %xmm1                                  #874.262
        mulsd     %xmm0, %xmm1                                  #875.42
        addsd     %xmm0, %xmm1                                  #875.46
        movsd     (%r9,%rcx,8), %xmm0                           #880.46
        mulsd     %xmm0, %xmm1                                  #880.71
        addsd     (%r9,%rdi,8), %xmm1                           #881.59
        addsd     %xmm0, %xmm1                                  #882.59
        cmpl      $2046, %edx                                   #884.35
        ja        ..B10.7       # Prob 50%                      #884.35
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax edx xmm1
..B10.6:                        # Preds ..B10.5
                                # Execution count [5.25e-02]
        movq      1128+_vmldCoshHATab(%rip), %rcx               #886.51
        shrq      $48, %rcx                                     #886.51
        shll      $4, %edx                                      #886.51
        andl      $-32753, %ecx                                 #886.51
        orl       %edx, %ecx                                    #886.51
        movw      %cx, -2(%rsp)                                 #886.51
        movsd     -8(%rsp), %xmm0                               #887.37
        mulsd     %xmm1, %xmm0                                  #887.45
        movsd     %xmm0, (%r8)                                  #887.32
        ret                                                     #887.32
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.7:                        # Preds ..B10.5
                                # Execution count [5.25e-02]
        decl      %edx                                          #891.79
        andl      $2047, %edx                                   #891.51
        movzwl    -2(%rsp), %ecx                                #891.51
        shll      $4, %edx                                      #891.51
        andl      $-32753, %ecx                                 #891.51
        orl       %edx, %ecx                                    #891.51
        movw      %cx, -2(%rsp)                                 #891.51
        movsd     -8(%rsp), %xmm0                               #892.47
        mulsd     %xmm0, %xmm1                                  #892.47
        mulsd     1024+_vmldCoshHATab(%rip), %xmm1              #893.77
        movsd     %xmm1, (%r8)                                  #893.32
        ret                                                     #893.32
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.8:                        # Preds ..B10.4
                                # Execution count [1.05e-01]
        movsd     1040+_vmldCoshHATab(%rip), %xmm1              #898.56
        lea       _vmldCoshHATab(%rip), %rcx                    #914.49
        movzwl    -2(%rsp), %esi                                #913.46
        andl      $-32753, %esi                                 #913.46
        movsd     1080+_vmldCoshHATab(%rip), %xmm14             #906.85
        mulsd     %xmm0, %xmm1                                  #898.56
        addsd     1048+_vmldCoshHATab(%rip), %xmm1              #899.62
        movsd     %xmm1, -40(%rsp)                              #899.25
        movsd     -40(%rsp), %xmm2                              #900.38
        movl      -40(%rsp), %r10d                              #908.50
        movl      %r10d, %r9d                                   #910.43
        shrl      $6, %r9d                                      #910.43
        subsd     1048+_vmldCoshHATab(%rip), %xmm2              #900.60
        movsd     %xmm2, -32(%rsp)                              #900.25
        lea       1023(%r9), %edi                               #911.42
        andl      $63, %r10d                                    #909.33
        addl      $1022, %r9d                                   #913.74
        movsd     -32(%rsp), %xmm3                              #901.31
        andl      $2047, %r9d                                   #913.46
        negl      %edi                                          #916.39
        shll      $4, %r9d                                      #913.46
        addl      $-4, %edi                                     #917.74
        mulsd     1104+_vmldCoshHATab(%rip), %xmm3              #901.52
        lea       (%r10,%r10), %edx                             #914.67
        movsd     (%rcx,%rdx,8), %xmm15                         #914.49
        negl      %edx                                          #918.71
        movsd     -32(%rsp), %xmm4                              #902.31
        orl       %r9d, %esi                                    #913.46
        andl      $2047, %edi                                   #917.46
        lea       1(%r10,%r10), %r11d                           #915.73
        mulsd     1112+_vmldCoshHATab(%rip), %xmm4              #902.52
        subsd     %xmm3, %xmm0                                  #903.46
        movw      %si, -2(%rsp)                                 #913.46
        andl      $-32753, %esi                                 #917.46
        shll      $4, %edi                                      #917.46
        subsd     %xmm4, %xmm0                                  #904.42
        movsd     -8(%rsp), %xmm6                               #914.74
        orl       %edi, %esi                                    #917.46
        movw      %si, -2(%rsp)                                 #917.46
        lea       128(%rdx), %esi                               #918.71
        mulsd     %xmm6, %xmm15                                 #914.74
        movaps    %xmm0, %xmm5                                  #905.44
        mulsd     %xmm0, %xmm5                                  #905.44
        movsd     -8(%rsp), %xmm7                               #918.79
        movaps    %xmm15, %xmm8                                 #920.51
        movsd     (%rcx,%rsi,8), %xmm11                         #918.49
        addl      $129, %edx                                    #919.71
        mulsd     %xmm7, %xmm11                                 #918.79
        movaps    %xmm15, %xmm10                                #920.85
        mulsd     %xmm5, %xmm14                                 #906.85
        addsd     %xmm11, %xmm8                                 #920.51
        subsd     %xmm11, %xmm15                                #921.47
        addsd     1064+_vmldCoshHATab(%rip), %xmm14             #906.108
        movsd     %xmm8, -24(%rsp)                              #920.25
        movsd     (%rcx,%r11,8), %xmm12                         #915.49
        movsd     (%rcx,%rdx,8), %xmm13                         #919.49
        movsd     -24(%rsp), %xmm9                              #920.85
        mulsd     %xmm6, %xmm12                                 #915.78
        subsd     %xmm9, %xmm10                                 #920.85
        mulsd     %xmm7, %xmm13                                 #919.83
        mulsd     %xmm5, %xmm14                                 #906.132
        addsd     %xmm11, %xmm10                                #920.117
        mulsd     %xmm0, %xmm14                                 #906.139
        movsd     1088+_vmldCoshHATab(%rip), %xmm1              #907.85
        movaps    %xmm12, %xmm11                                #924.37
        mulsd     %xmm5, %xmm1                                  #907.85
        subsd     %xmm13, %xmm12                                #922.51
        mulsd     %xmm15, %xmm14                                #926.46
        mulsd     %xmm0, %xmm12                                 #922.51
        addsd     1072+_vmldCoshHATab(%rip), %xmm1              #907.108
        mulsd     %xmm15, %xmm0                                 #928.43
        mulsd     %xmm5, %xmm1                                  #907.132
        addsd     %xmm12, %xmm11                                #923.25
        movsd     %xmm10, -16(%rsp)                             #920.94
        addsd     %xmm13, %xmm11                                #924.25
        addsd     1056+_vmldCoshHATab(%rip), %xmm1              #907.156
        addsd     %xmm14, %xmm11                                #925.25
        mulsd     %xmm5, %xmm1                                  #907.180
        addsd     %xmm0, %xmm11                                 #926.25
        movsd     -24(%rsp), %xmm3                              #920.133
        mulsd     %xmm3, %xmm1                                  #927.47
        movsd     -16(%rsp), %xmm2                              #920.146
        addsd     %xmm1, %xmm11                                 #927.25
        addsd     %xmm2, %xmm11                                 #928.25
        movsd     %xmm11, -24(%rsp)                             #929.25
        movsd     -24(%rsp), %xmm0                              #930.29
        addsd     %xmm0, %xmm3                                  #930.37
        movsd     %xmm3, (%r8)                                  #931.27
        ret                                                     #931.27
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.9:                        # Preds ..B10.3
                                # Execution count [2.10e-01]
        movsd     1120+_vmldCoshHATab(%rip), %xmm0              #936.83
        movl      $3, %eax                                      #937.21
        mulsd     %xmm0, %xmm0                                  #936.83
        movsd     %xmm0, (%r8)                                  #936.21
        ret                                                     #936.21
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.10:                       # Preds ..B10.2
                                # Execution count [4.20e-01]
        movsd     1136+_vmldCoshHATab(%rip), %xmm0              #942.38
        addsd     -8(%rsp), %xmm0                               #942.61
        movsd     %xmm0, (%r8)                                  #942.16
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.11:                       # Preds ..B10.10
                                # Execution count [1.00e+00]
        ret                                                     #949.13
                                # LOE
..B10.12:                       # Preds ..B10.1
                                # Execution count [1.60e-01]: Infreq
        movsd     (%rdi), %xmm0                                 #947.18
        mulsd     %xmm0, %xmm0                                  #947.25
        movsd     %xmm0, (%r8)                                  #947.11
        ret                                                     #947.11
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_dcosh_ha_cout_rare_internal,@function
	.size	__svml_dcosh_ha_cout_rare_internal,.-__svml_dcosh_ha_cout_rare_internal
..LN__svml_dcosh_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dcosh_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_dcosh_ha_data_internal
	.globl __svml_dcosh_ha_data_internal
__svml_dcosh_ha_data_internal:
	.long	0
	.long	1071644672
	.long	1828292879
	.long	1071691096
	.long	1014845819
	.long	1071739576
	.long	1853186616
	.long	1071790202
	.long	171030293
	.long	1071843070
	.long	1276261410
	.long	1071898278
	.long	3577096743
	.long	1071955930
	.long	3712504873
	.long	1072016135
	.long	1719614413
	.long	1072079006
	.long	1944781191
	.long	1072144660
	.long	1110089947
	.long	1072213221
	.long	2191782032
	.long	1072284817
	.long	2572866477
	.long	1072359583
	.long	3716502172
	.long	1072437659
	.long	3707479175
	.long	1072519192
	.long	2728693978
	.long	1072604335
	.long	0
	.long	0
	.long	1255956747
	.long	1015588398
	.long	3117910646
	.long	3161559105
	.long	3066496371
	.long	1015656574
	.long	3526460132
	.long	1014428778
	.long	300981948
	.long	1014684169
	.long	2951496418
	.long	1013793687
	.long	88491949
	.long	1015427660
	.long	330458198
	.long	3163282740
	.long	3993278767
	.long	3161724279
	.long	1451641639
	.long	1015474673
	.long	2960257726
	.long	1013742662
	.long	878562433
	.long	1015521741
	.long	2303740125
	.long	1014042725
	.long	3613079303
	.long	1014164738
	.long	396109971
	.long	3163462691
	.long	0
	.long	1071644672
	.long	2728693978
	.long	1071555759
	.long	3707479175
	.long	1071470616
	.long	3716502172
	.long	1071389083
	.long	2572866477
	.long	1071311007
	.long	2191782032
	.long	1071236241
	.long	1110089947
	.long	1071164645
	.long	1944781191
	.long	1071096084
	.long	1719614413
	.long	1071030430
	.long	3712504873
	.long	1070967559
	.long	3577096743
	.long	1070907354
	.long	1276261410
	.long	1070849702
	.long	171030293
	.long	1070794494
	.long	1853186616
	.long	1070741626
	.long	1014845819
	.long	1070691000
	.long	1828292879
	.long	1070642520
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	15
	.long	0
	.long	15
	.long	0
	.long	15
	.long	0
	.long	15
	.long	0
	.long	15
	.long	0
	.long	15
	.long	0
	.long	15
	.long	0
	.long	15
	.long	0
	.long	4
	.long	1071644672
	.long	4
	.long	1071644672
	.long	4
	.long	1071644672
	.long	4
	.long	1071644672
	.long	4
	.long	1071644672
	.long	4
	.long	1071644672
	.long	4
	.long	1071644672
	.long	4
	.long	1071644672
	.long	1431655747
	.long	1069897045
	.long	1431655747
	.long	1069897045
	.long	1431655747
	.long	1069897045
	.long	1431655747
	.long	1069897045
	.long	1431655747
	.long	1069897045
	.long	1431655747
	.long	1069897045
	.long	1431655747
	.long	1069897045
	.long	1431655747
	.long	1069897045
	.long	1430802231
	.long	1067799893
	.long	1430802231
	.long	1067799893
	.long	1430802231
	.long	1067799893
	.long	1430802231
	.long	1067799893
	.long	1430802231
	.long	1067799893
	.long	1430802231
	.long	1067799893
	.long	1430802231
	.long	1067799893
	.long	1430802231
	.long	1067799893
	.long	287861260
	.long	1065423121
	.long	287861260
	.long	1065423121
	.long	287861260
	.long	1065423121
	.long	287861260
	.long	1065423121
	.long	287861260
	.long	1065423121
	.long	287861260
	.long	1065423121
	.long	287861260
	.long	1065423121
	.long	287861260
	.long	1065423121
	.long	3658019094
	.long	1062650243
	.long	3658019094
	.long	1062650243
	.long	3658019094
	.long	1062650243
	.long	3658019094
	.long	1062650243
	.long	3658019094
	.long	1062650243
	.long	3658019094
	.long	1062650243
	.long	3658019094
	.long	1062650243
	.long	3658019094
	.long	1062650243
	.long	1993999322
	.long	1059717517
	.long	1993999322
	.long	1059717517
	.long	1993999322
	.long	1059717517
	.long	1993999322
	.long	1059717517
	.long	1993999322
	.long	1059717517
	.long	1993999322
	.long	1059717517
	.long	1993999322
	.long	1059717517
	.long	1993999322
	.long	1059717517
	.long	0
	.long	1071644672
	.long	0
	.long	0
	.long	0
	.long	1071644672
	.long	2851812149
	.long	1071650365
	.long	2595802551
	.long	1015767337
	.long	730821105
	.long	1071633346
	.long	1048019041
	.long	1071656090
	.long	1398474845
	.long	3160510595
	.long	2174652632
	.long	1071622081
	.long	3899555717
	.long	1071661845
	.long	427280750
	.long	3162546972
	.long	2912730644
	.long	1071610877
	.long	3541402996
	.long	1071667632
	.long	2759177317
	.long	1014854626
	.long	1533953344
	.long	1071599734
	.long	702412510
	.long	1071673451
	.long	3803266087
	.long	3162280415
	.long	929806999
	.long	1071588651
	.long	410360776
	.long	1071679301
	.long	1269990655
	.long	1011975870
	.long	3999357479
	.long	1071577627
	.long	3402036099
	.long	1071685182
	.long	405889334
	.long	1015105656
	.long	764307441
	.long	1071566664
	.long	1828292879
	.long	1071691096
	.long	1255956747
	.long	1015588398
	.long	2728693978
	.long	1071555759
	.long	728909815
	.long	1071697042
	.long	383930225
	.long	1015029468
	.long	4224142467
	.long	1071544913
	.long	852742562
	.long	1071703020
	.long	667253587
	.long	1009793559
	.long	3884662774
	.long	1071534126
	.long	2952712987
	.long	1071709030
	.long	3293494651
	.long	3160120301
	.long	351641897
	.long	1071523398
	.long	3490863953
	.long	1071715073
	.long	960797498
	.long	3162948880
	.long	863738719
	.long	1071512727
	.long	3228316108
	.long	1071721149
	.long	3010241991
	.long	3158422804
	.long	4076975200
	.long	1071502113
	.long	2930322912
	.long	1071727258
	.long	2599499422
	.long	3162714047
	.long	64696965
	.long	1071491558
	.long	3366293073
	.long	1071733400
	.long	3119426314
	.long	1014120554
	.long	382305176
	.long	1071481059
	.long	1014845819
	.long	1071739576
	.long	3117910646
	.long	3161559105
	.long	3707479175
	.long	1071470616
	.long	948735466
	.long	1071745785
	.long	3516338028
	.long	3162574883
	.long	135105010
	.long	1071460231
	.long	3949972341
	.long	1071752027
	.long	2068408548
	.long	1014913868
	.long	1242007932
	.long	1071449901
	.long	2214878420
	.long	1071758304
	.long	892270087
	.long	3163116422
	.long	1432208378
	.long	1071439627
	.long	828946858
	.long	1071764615
	.long	10642492
	.long	1015939438
	.long	3706687593
	.long	1071429408
	.long	586995997
	.long	1071770960
	.long	41662348
	.long	3162627992
	.long	2483480501
	.long	1071419245
	.long	2288159958
	.long	1071777339
	.long	2169144469
	.long	1014876021
	.long	777507147
	.long	1071409137
	.long	2440944790
	.long	1071783753
	.long	2492769774
	.long	1014147454
	.long	1610600570
	.long	1071399083
	.long	1853186616
	.long	1071790202
	.long	3066496371
	.long	1015656574
	.long	3716502172
	.long	1071389083
	.long	1337108031
	.long	1071796686
	.long	3203724452
	.long	1014677845
	.long	1540824585
	.long	1071379138
	.long	1709341917
	.long	1071803205
	.long	2571168217
	.long	1014152499
	.long	2420883922
	.long	1071369246
	.long	3790955393
	.long	1071809759
	.long	2352942462
	.long	3163180090
	.long	815859274
	.long	1071359408
	.long	4112506593
	.long	1071816349
	.long	2947355221
	.long	1014371048
	.long	4076559943
	.long	1071349622
	.long	3504003472
	.long	1071822975
	.long	3594001060
	.long	3157330652
	.long	2380618042
	.long	1071339890
	.long	2799960843
	.long	1071829637
	.long	1423655381
	.long	1015022151
	.long	3092190715
	.long	1071330210
	.long	2839424854
	.long	1071836335
	.long	1171596163
	.long	1013041679
	.long	697153126
	.long	1071320583
	.long	171030293
	.long	1071843070
	.long	3526460132
	.long	1014428778
	.long	2572866477
	.long	1071311007
	.long	4232894513
	.long	1071849840
	.long	2383938684
	.long	1014668519
	.long	3218338682
	.long	1071301483
	.long	2992903935
	.long	1071856648
	.long	2218154406
	.long	1015228193
	.long	1434058175
	.long	1071292011
	.long	1603444721
	.long	1071863493
	.long	1548633640
	.long	3162201326
	.long	321958744
	.long	1071282590
	.long	926591435
	.long	1071870375
	.long	3208833762
	.long	3162913514
	.long	2990417245
	.long	1071273219
	.long	1829099622
	.long	1071877294
	.long	1016661181
	.long	3163461005
	.long	3964284211
	.long	1071263899
	.long	887463927
	.long	1071884251
	.long	3596744163
	.long	3160794166
	.long	2069751141
	.long	1071254630
	.long	3272845541
	.long	1071891245
	.long	928852419
	.long	3163488248
	.long	434316067
	.long	1071245411
	.long	1276261410
	.long	1071898278
	.long	300981948
	.long	1014684169
	.long	2191782032
	.long	1071236241
	.long	78413852
	.long	1071905349
	.long	4183226867
	.long	3163017251
	.long	1892288442
	.long	1071227121
	.long	569847338
	.long	1071912458
	.long	472945272
	.long	3159290729
	.long	2682146384
	.long	1071218050
	.long	3645941911
	.long	1071919605
	.long	3814685081
	.long	3161573341
	.long	3418903055
	.long	1071209028
	.long	1617004845
	.long	1071926792
	.long	82804944
	.long	1010342778
	.long	2966275557
	.long	1071200055
	.long	3978100823
	.long	1071934017
	.long	3513027190
	.long	1015845963
	.long	194117574
	.long	1071191131
	.long	3049340112
	.long	1071941282
	.long	3062915824
	.long	1013170595
	.long	2568320822
	.long	1071182254
	.long	4040676318
	.long	1071948586
	.long	4090609238
	.long	1015663458
	.long	380978316
	.long	1071173426
	.long	3577096743
	.long	1071955930
	.long	2951496418
	.long	1013793687
	.long	1110089947
	.long	1071164645
	.long	2583551245
	.long	1071963314
	.long	3161094195
	.long	1015606491
	.long	3649726105
	.long	1071155911
	.long	1990012071
	.long	1071970738
	.long	3529070563
	.long	3162813193
	.long	2604962541
	.long	1071147225
	.long	2731501122
	.long	1071978202
	.long	1774031855
	.long	3162470021
	.long	1176749997
	.long	1071138586
	.long	1453150082
	.long	1071985707
	.long	498154669
	.long	3161488062
	.long	2571947539
	.long	1071129993
	.long	3395129871
	.long	1071993252
	.long	4025345435
	.long	3162335388
	.long	1413356050
	.long	1071121447
	.long	917841882
	.long	1072000839
	.long	18715565
	.long	1015659308
	.long	919555682
	.long	1071112947
	.long	3566716925
	.long	1072008466
	.long	1536826856
	.long	1014142433
	.long	19972402
	.long	1071104493
	.long	3712504873
	.long	1072016135
	.long	88491949
	.long	1015427660
	.long	1944781191
	.long	1071096084
	.long	2321106615
	.long	1072023846
	.long	2171176610
	.long	1009535771
	.long	1339972927
	.long	1071087721
	.long	363667784
	.long	1072031599
	.long	813753950
	.long	1015785209
	.long	1447192521
	.long	1071079403
	.long	3111574537
	.long	1072039393
	.long	2606161479
	.long	3162759746
	.long	1218806132
	.long	1071071130
	.long	2956612997
	.long	1072047230
	.long	2118169751
	.long	3162735553
	.long	3907805044
	.long	1071062901
	.long	885834528
	.long	1072055110
	.long	1973258547
	.long	3162261564
	.long	4182873220
	.long	1071054717
	.long	2186617381
	.long	1072063032
	.long	2270764084
	.long	3163272713
	.long	1013258799
	.long	1071046578
	.long	3561793907
	.long	1072070997
	.long	1157054053
	.long	1011890350
	.long	1963711167
	.long	1071038482
	.long	1719614413
	.long	1072079006
	.long	330458198
	.long	3163282740
	.long	1719614413
	.long	1071030430
	.long	1963711167
	.long	1072087058
	.long	1744767757
	.long	3160574294
	.long	3561793907
	.long	1071022421
	.long	1013258799
	.long	1072095154
	.long	1748797611
	.long	3160129082
	.long	2186617381
	.long	1071014456
	.long	4182873220
	.long	1072103293
	.long	629542646
	.long	3161996303
	.long	885834528
	.long	1071006534
	.long	3907805044
	.long	1072111477
	.long	2257091225
	.long	3161550407
	.long	2956612997
	.long	1070998654
	.long	1218806132
	.long	1072119706
	.long	1818613052
	.long	3162548441
	.long	3111574537
	.long	1070990817
	.long	1447192521
	.long	1072127979
	.long	1462857171
	.long	3162514521
	.long	363667784
	.long	1070983023
	.long	1339972927
	.long	1072136297
	.long	167908909
	.long	1015572152
	.long	2321106615
	.long	1070975270
	.long	1944781191
	.long	1072144660
	.long	3993278767
	.long	3161724279
	.long	3712504873
	.long	1070967559
	.long	19972402
	.long	1072153069
	.long	3507899862
	.long	1016009292
	.long	3566716925
	.long	1070959890
	.long	919555682
	.long	1072161523
	.long	3121969534
	.long	1012948226
	.long	917841882
	.long	1070952263
	.long	1413356050
	.long	1072170023
	.long	1651349291
	.long	3162668166
	.long	3395129871
	.long	1070944676
	.long	2571947539
	.long	1072178569
	.long	3558159064
	.long	3163376669
	.long	1453150082
	.long	1070937131
	.long	1176749997
	.long	1072187162
	.long	2738998779
	.long	3162035844
	.long	2731501122
	.long	1070929626
	.long	2604962541
	.long	1072195801
	.long	2614425274
	.long	3163539192
	.long	1990012071
	.long	1070922162
	.long	3649726105
	.long	1072204487
	.long	4085036346
	.long	1015649474
	.long	2583551245
	.long	1070914738
	.long	1110089947
	.long	1072213221
	.long	1451641639
	.long	1015474673
	.long	3577096743
	.long	1070907354
	.long	380978316
	.long	1072222002
	.long	854188970
	.long	3160462686
	.long	4040676318
	.long	1070900010
	.long	2568320822
	.long	1072230830
	.long	2732824428
	.long	1014352915
	.long	3049340112
	.long	1070892706
	.long	194117574
	.long	1072239707
	.long	777528612
	.long	3163412089
	.long	3978100823
	.long	1070885441
	.long	2966275557
	.long	1072248631
	.long	2176155324
	.long	3159842759
	.long	1617004845
	.long	1070878216
	.long	3418903055
	.long	1072257604
	.long	2527457337
	.long	3160820604
	.long	3645941911
	.long	1070871029
	.long	2682146384
	.long	1072266626
	.long	2082178513
	.long	3163363419
	.long	569847338
	.long	1070863882
	.long	1892288442
	.long	1072275697
	.long	2446255666
	.long	3162600381
	.long	78413852
	.long	1070856773
	.long	2191782032
	.long	1072284817
	.long	2960257726
	.long	1013742662
	.long	1276261410
	.long	1070849702
	.long	434316067
	.long	1072293987
	.long	2028358766
	.long	1013458122
	.long	3272845541
	.long	1070842669
	.long	2069751141
	.long	1072303206
	.long	1562170675
	.long	3162724681
	.long	887463927
	.long	1070835675
	.long	3964284211
	.long	1072312475
	.long	2111583915
	.long	1015427164
	.long	1829099622
	.long	1070828718
	.long	2990417245
	.long	1072321795
	.long	3683467745
	.long	3163369326
	.long	926591435
	.long	1070821799
	.long	321958744
	.long	1072331166
	.long	3401933767
	.long	1015794558
	.long	1603444721
	.long	1070814917
	.long	1434058175
	.long	1072340587
	.long	251133233
	.long	1015085769
	.long	2992903935
	.long	1070808072
	.long	3218338682
	.long	1072350059
	.long	3404164304
	.long	3162477108
	.long	4232894513
	.long	1070801264
	.long	2572866477
	.long	1072359583
	.long	878562433
	.long	1015521741
	.long	171030293
	.long	1070794494
	.long	697153126
	.long	1072369159
	.long	1283515429
	.long	3163283189
	.long	2839424854
	.long	1070787759
	.long	3092190715
	.long	1072378786
	.long	814012168
	.long	3159523422
	.long	2799960843
	.long	1070781061
	.long	2380618042
	.long	1072388466
	.long	3149557219
	.long	3163320799
	.long	3504003472
	.long	1070774399
	.long	4076559943
	.long	1072398198
	.long	2119478331
	.long	3160758351
	.long	4112506593
	.long	1070767773
	.long	815859274
	.long	1072407984
	.long	240396590
	.long	3163487443
	.long	3790955393
	.long	1070761183
	.long	2420883922
	.long	1072417822
	.long	2049810052
	.long	1014119888
	.long	1709341917
	.long	1070754629
	.long	1540824585
	.long	1072427714
	.long	1064017011
	.long	3163487690
	.long	1337108031
	.long	1070748110
	.long	3716502172
	.long	1072437659
	.long	2303740125
	.long	1014042725
	.long	1853186616
	.long	1070741626
	.long	1610600570
	.long	1072447659
	.long	3766732298
	.long	1015760183
	.long	2440944790
	.long	1070735177
	.long	777507147
	.long	1072457713
	.long	4282924205
	.long	1015187533
	.long	2288159958
	.long	1070728763
	.long	2483480501
	.long	1072467821
	.long	1216371780
	.long	1013034172
	.long	586995997
	.long	1070722384
	.long	3706687593
	.long	1072477984
	.long	3521726939
	.long	1013253067
	.long	828946858
	.long	1070716039
	.long	1432208378
	.long	1072488203
	.long	1401068914
	.long	3162363963
	.long	2214878420
	.long	1070709728
	.long	1242007932
	.long	1072498477
	.long	1132034716
	.long	3163339831
	.long	3949972341
	.long	1070703451
	.long	135105010
	.long	1072508807
	.long	1906148728
	.long	3163375739
	.long	948735466
	.long	1070697209
	.long	3707479175
	.long	1072519192
	.long	3613079303
	.long	1014164738
	.long	1014845819
	.long	1070691000
	.long	382305176
	.long	1072529635
	.long	2347622376
	.long	3162578625
	.long	3366293073
	.long	1070684824
	.long	64696965
	.long	1072540134
	.long	1768797490
	.long	1015816960
	.long	2930322912
	.long	1070678682
	.long	4076975200
	.long	1072550689
	.long	2029000899
	.long	1015208535
	.long	3228316108
	.long	1070672573
	.long	863738719
	.long	1072561303
	.long	1326992220
	.long	3162613197
	.long	3490863953
	.long	1070666497
	.long	351641897
	.long	1072571974
	.long	2172261526
	.long	3163010599
	.long	2952712987
	.long	1070660454
	.long	3884662774
	.long	1072582702
	.long	2158611599
	.long	1014210185
	.long	852742562
	.long	1070654444
	.long	4224142467
	.long	1072593489
	.long	3389820386
	.long	1015207202
	.long	728909815
	.long	1070648466
	.long	2728693978
	.long	1072604335
	.long	396109971
	.long	3163462691
	.long	1828292879
	.long	1070642520
	.long	764307441
	.long	1072615240
	.long	3021057420
	.long	3163329523
	.long	3402036099
	.long	1070636606
	.long	3999357479
	.long	1072626203
	.long	2258941616
	.long	1015924724
	.long	410360776
	.long	1070630725
	.long	929806999
	.long	1072637227
	.long	3205336643
	.long	1015259557
	.long	702412510
	.long	1070624875
	.long	1533953344
	.long	1072648310
	.long	769171851
	.long	1015665633
	.long	3541402996
	.long	1070619056
	.long	2912730644
	.long	1072659453
	.long	3490067722
	.long	3163405074
	.long	3899555717
	.long	1070613269
	.long	2174652632
	.long	1072670657
	.long	4087714590
	.long	1014450259
	.long	1048019041
	.long	1070607514
	.long	730821105
	.long	1072681922
	.long	2523232743
	.long	1012067188
	.long	2851812149
	.long	1070601789
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
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	3164486458
	.long	1031600026
	.long	3164486458
	.long	1031600026
	.long	3164486458
	.long	1031600026
	.long	3164486458
	.long	1031600026
	.long	3164486458
	.long	1031600026
	.long	3164486458
	.long	1031600026
	.long	3164486458
	.long	1031600026
	.long	3164486458
	.long	1031600026
	.long	0
	.long	1120403456
	.long	0
	.long	1120403456
	.long	0
	.long	1120403456
	.long	0
	.long	1120403456
	.long	0
	.long	1120403456
	.long	0
	.long	1120403456
	.long	0
	.long	1120403456
	.long	0
	.long	1120403456
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
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
	.long	4294967128
	.long	1071644671
	.long	4294967128
	.long	1071644671
	.long	4294967128
	.long	1071644671
	.long	4294967128
	.long	1071644671
	.long	4294967128
	.long	1071644671
	.long	4294967128
	.long	1071644671
	.long	4294967128
	.long	1071644671
	.long	4294967128
	.long	1071644671
	.long	1431655910
	.long	1069897045
	.long	1431655910
	.long	1069897045
	.long	1431655910
	.long	1069897045
	.long	1431655910
	.long	1069897045
	.long	1431655910
	.long	1069897045
	.long	1431655910
	.long	1069897045
	.long	1431655910
	.long	1069897045
	.long	1431655910
	.long	1069897045
	.long	2898925341
	.long	1067799893
	.long	2898925341
	.long	1067799893
	.long	2898925341
	.long	1067799893
	.long	2898925341
	.long	1067799893
	.long	2898925341
	.long	1067799893
	.long	2898925341
	.long	1067799893
	.long	2898925341
	.long	1067799893
	.long	2898925341
	.long	1067799893
	.long	564252519
	.long	1065423121
	.long	564252519
	.long	1065423121
	.long	564252519
	.long	1065423121
	.long	564252519
	.long	1065423121
	.long	564252519
	.long	1065423121
	.long	564252519
	.long	1065423121
	.long	564252519
	.long	1065423121
	.long	564252519
	.long	1065423121
	.long	0
	.long	2146435072
	.long	0
	.long	2146435072
	.long	0
	.long	2146435072
	.long	0
	.long	2146435072
	.long	0
	.long	2146435072
	.long	0
	.long	2146435072
	.long	0
	.long	2146435072
	.long	0
	.long	2146435072
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
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.long	1082531225
	.type	__svml_dcosh_ha_data_internal,@object
	.size	__svml_dcosh_ha_data_internal,4800
	.align 32
_vmldCoshHATab:
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	1048019041
	.long	1072704666
	.long	1398474845
	.long	3161559171
	.long	3541402996
	.long	1072716208
	.long	2759177317
	.long	1015903202
	.long	410360776
	.long	1072727877
	.long	1269990655
	.long	1013024446
	.long	1828292879
	.long	1072739672
	.long	1255956747
	.long	1016636974
	.long	852742562
	.long	1072751596
	.long	667253587
	.long	1010842135
	.long	3490863953
	.long	1072763649
	.long	960797498
	.long	3163997456
	.long	2930322912
	.long	1072775834
	.long	2599499422
	.long	3163762623
	.long	1014845819
	.long	1072788152
	.long	3117910646
	.long	3162607681
	.long	3949972341
	.long	1072800603
	.long	2068408548
	.long	1015962444
	.long	828946858
	.long	1072813191
	.long	10642492
	.long	1016988014
	.long	2288159958
	.long	1072825915
	.long	2169144469
	.long	1015924597
	.long	1853186616
	.long	1072838778
	.long	3066496371
	.long	1016705150
	.long	1709341917
	.long	1072851781
	.long	2571168217
	.long	1015201075
	.long	4112506593
	.long	1072864925
	.long	2947355221
	.long	1015419624
	.long	2799960843
	.long	1072878213
	.long	1423655381
	.long	1016070727
	.long	171030293
	.long	1072891646
	.long	3526460132
	.long	1015477354
	.long	2992903935
	.long	1072905224
	.long	2218154406
	.long	1016276769
	.long	926591435
	.long	1072918951
	.long	3208833762
	.long	3163962090
	.long	887463927
	.long	1072932827
	.long	3596744163
	.long	3161842742
	.long	1276261410
	.long	1072946854
	.long	300981948
	.long	1015732745
	.long	569847338
	.long	1072961034
	.long	472945272
	.long	3160339305
	.long	1617004845
	.long	1072975368
	.long	82804944
	.long	1011391354
	.long	3049340112
	.long	1072989858
	.long	3062915824
	.long	1014219171
	.long	3577096743
	.long	1073004506
	.long	2951496418
	.long	1014842263
	.long	1990012071
	.long	1073019314
	.long	3529070563
	.long	3163861769
	.long	1453150082
	.long	1073034283
	.long	498154669
	.long	3162536638
	.long	917841882
	.long	1073049415
	.long	18715565
	.long	1016707884
	.long	3712504873
	.long	1073064711
	.long	88491949
	.long	1016476236
	.long	363667784
	.long	1073080175
	.long	813753950
	.long	1016833785
	.long	2956612997
	.long	1073095806
	.long	2118169751
	.long	3163784129
	.long	2186617381
	.long	1073111608
	.long	2270764084
	.long	3164321289
	.long	1719614413
	.long	1073127582
	.long	330458198
	.long	3164331316
	.long	1013258799
	.long	1073143730
	.long	1748797611
	.long	3161177658
	.long	3907805044
	.long	1073160053
	.long	2257091225
	.long	3162598983
	.long	1447192521
	.long	1073176555
	.long	1462857171
	.long	3163563097
	.long	1944781191
	.long	1073193236
	.long	3993278767
	.long	3162772855
	.long	919555682
	.long	1073210099
	.long	3121969534
	.long	1013996802
	.long	2571947539
	.long	1073227145
	.long	3558159064
	.long	3164425245
	.long	2604962541
	.long	1073244377
	.long	2614425274
	.long	3164587768
	.long	1110089947
	.long	1073261797
	.long	1451641639
	.long	1016523249
	.long	2568320822
	.long	1073279406
	.long	2732824428
	.long	1015401491
	.long	2966275557
	.long	1073297207
	.long	2176155324
	.long	3160891335
	.long	2682146384
	.long	1073315202
	.long	2082178513
	.long	3164411995
	.long	2191782032
	.long	1073333393
	.long	2960257726
	.long	1014791238
	.long	2069751141
	.long	1073351782
	.long	1562170675
	.long	3163773257
	.long	2990417245
	.long	1073370371
	.long	3683467745
	.long	3164417902
	.long	1434058175
	.long	1073389163
	.long	251133233
	.long	1016134345
	.long	2572866477
	.long	1073408159
	.long	878562433
	.long	1016570317
	.long	3092190715
	.long	1073427362
	.long	814012168
	.long	3160571998
	.long	4076559943
	.long	1073446774
	.long	2119478331
	.long	3161806927
	.long	2420883922
	.long	1073466398
	.long	2049810052
	.long	1015168464
	.long	3716502172
	.long	1073486235
	.long	2303740125
	.long	1015091301
	.long	777507147
	.long	1073506289
	.long	4282924205
	.long	1016236109
	.long	3706687593
	.long	1073526560
	.long	3521726939
	.long	1014301643
	.long	1242007932
	.long	1073547053
	.long	1132034716
	.long	3164388407
	.long	3707479175
	.long	1073567768
	.long	3613079303
	.long	1015213314
	.long	64696965
	.long	1073588710
	.long	1768797490
	.long	1016865536
	.long	863738719
	.long	1073609879
	.long	1326992220
	.long	3163661773
	.long	3884662774
	.long	1073631278
	.long	2158611599
	.long	1015258761
	.long	2728693978
	.long	1073652911
	.long	396109971
	.long	3164511267
	.long	3999357479
	.long	1073674779
	.long	2258941616
	.long	1016973300
	.long	1533953344
	.long	1073696886
	.long	769171851
	.long	1016714209
	.long	2174652632
	.long	1073719233
	.long	4087714590
	.long	1015498835
	.long	0
	.long	1073741824
	.long	0
	.long	0
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
	.long	2411329662
	.long	1082536910
	.long	4277796864
	.long	1065758274
	.long	3164486458
	.long	1025308570
	.long	4294967295
	.long	2146435071
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	3875694624
	.long	1077247184
	.type	_vmldCoshHATab,@object
	.size	_vmldCoshHATab,1152
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
