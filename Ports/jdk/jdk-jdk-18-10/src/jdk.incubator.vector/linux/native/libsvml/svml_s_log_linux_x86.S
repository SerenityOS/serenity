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
	.file "svml_s_log.c"
	.text
..TXTST0:
.L_2__routine_start___svml_logf4_ha_ex_0:
# -- Begin  __svml_logf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_logf4_ha_ex
# --- __svml_logf4_ha_ex(__m128)
__svml_logf4_ha_ex:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #223.1
        .byte     15                                            #283.540
        .byte     30                                            #283.540
        .byte     250                                           #283.540
	.cfi_startproc
..___tag_value___svml_logf4_ha_ex.1:
..L2:
                                                          #223.1
        pushq     %rbp                                          #223.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #223.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #223.1
        subq      $192, %rsp                                    #223.1
        movaps    %xmm0, %xmm2                                  #250.9
        movdqu    576+__svml_slog_ha_data_internal(%rip), %xmm1 #247.17
        movdqu    640+__svml_slog_ha_data_internal(%rip), %xmm4 #247.17
        paddd     %xmm0, %xmm1                                  #247.17
        movdqu    704+__svml_slog_ha_data_internal(%rip), %xmm3 #248.51
        pcmpgtd   %xmm1, %xmm4                                  #247.17
        movdqu    768+__svml_slog_ha_data_internal(%rip), %xmm1 #251.9
        psubd     %xmm3, %xmm2                                  #250.9
        pand      %xmm2, %xmm1                                  #251.9
        psrad     $23, %xmm2                                    #252.9
        paddd     %xmm3, %xmm1                                  #255.9
        movups    512+__svml_slog_ha_data_internal(%rip), %xmm5 #259.49
        cvtdq2ps  %xmm2, %xmm8                                  #254.9
        subps     832+__svml_slog_ha_data_internal(%rip), %xmm1 #258.9
        movmskps  %xmm4, %edx                                   #256.40
        movups    960+__svml_slog_ha_data_internal(%rip), %xmm6 #279.47
        mulps     %xmm1, %xmm5                                  #261.25
        mulps     %xmm8, %xmm6                                  #280.26
        addps     448+__svml_slog_ha_data_internal(%rip), %xmm5 #261.13
        mulps     %xmm1, %xmm5                                  #263.25
        movups    896+__svml_slog_ha_data_internal(%rip), %xmm7 #278.47
        mulps     %xmm7, %xmm8                                  #281.26
        addps     384+__svml_slog_ha_data_internal(%rip), %xmm5 #263.13
        mulps     %xmm1, %xmm5                                  #265.25
        addps     320+__svml_slog_ha_data_internal(%rip), %xmm5 #265.13
        mulps     %xmm1, %xmm5                                  #267.25
        addps     256+__svml_slog_ha_data_internal(%rip), %xmm5 #267.13
        mulps     %xmm1, %xmm5                                  #269.25
        addps     192+__svml_slog_ha_data_internal(%rip), %xmm5 #269.13
        mulps     %xmm1, %xmm5                                  #271.25
        addps     128+__svml_slog_ha_data_internal(%rip), %xmm5 #271.13
        mulps     %xmm1, %xmm5                                  #273.25
        addps     64+__svml_slog_ha_data_internal(%rip), %xmm5  #273.13
        mulps     %xmm1, %xmm5                                  #275.25
        addps     __svml_slog_ha_data_internal(%rip), %xmm5     #275.13
        mulps     %xmm1, %xmm5                                  #276.9
        mulps     %xmm1, %xmm5                                  #277.21
        addps     %xmm5, %xmm1                                  #277.9
        addps     %xmm6, %xmm1                                  #280.14
        addps     %xmm8, %xmm1                                  #281.14
        testl     %edx, %edx                                    #283.52
        jne       ..B1.3        # Prob 5%                       #283.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm1
..B1.2:                         # Preds ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movaps    %xmm1, %xmm0                                  #286.12
        movq      %rbp, %rsp                                    #286.12
        popq      %rbp                                          #286.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #286.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #283.193
        movups    %xmm1, 128(%rsp)                              #283.264
                                # LOE rbx r12 r13 r14 r15 edx
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #283.448
        movq      %r12, 8(%rsp)                                 #283.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #283.448
        movq      %r13, (%rsp)                                  #283.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #283.448
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #283.511
        jc        ..B1.10       # Prob 5%                       #283.511
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #283.464
        cmpl      $4, %r12d                                     #283.459
        jl        ..B1.7        # Prob 82%                      #283.459
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm1                              #283.665
        jmp       ..B1.2        # Prob 100%                     #283.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm1
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #283.540
        lea       128(%rsp,%r12,4), %rsi                        #283.540
..___tag_value___svml_logf4_ha_ex.19:
#       __svml_slog_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog_ha_cout_rare_internal             #283.540
..___tag_value___svml_logf4_ha_ex.20:
        jmp       ..B1.8        # Prob 100%                     #283.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_logf4_ha_ex,@function
	.size	__svml_logf4_ha_ex,.-__svml_logf4_ha_ex
..LN__svml_logf4_ha_ex.0:
	.data
# -- End  __svml_logf4_ha_ex
	.text
.L_2__routine_start___svml_logf4_ha_e9_1:
# -- Begin  __svml_logf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_logf4_ha_e9
# --- __svml_logf4_ha_e9(__m128)
__svml_logf4_ha_e9:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #291.1
        .byte     15                                            #351.540
        .byte     30                                            #351.540
        .byte     250                                           #351.540
	.cfi_startproc
..___tag_value___svml_logf4_ha_e9.22:
..L23:
                                                         #291.1
        pushq     %rbp                                          #291.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #291.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #291.1
        subq      $192, %rsp                                    #291.1
        movaps    %xmm0, %xmm10                                 #318.15
        movups    704+__svml_slog_ha_data_internal(%rip), %xmm3 #316.22
        movups    768+__svml_slog_ha_data_internal(%rip), %xmm2 #317.25
        psubd     %xmm3, %xmm10                                 #318.15
        movdqa    %xmm10, %xmm1                                 #320.15
        pand      %xmm2, %xmm10                                 #319.15
        paddd     %xmm3, %xmm10                                 #323.9
        psrad     $23, %xmm1                                    #320.15
        movups    512+__svml_slog_ha_data_internal(%rip), %xmm6 #327.49
        cvtdq2ps  %xmm1, %xmm9                                  #322.9
        subps     832+__svml_slog_ha_data_internal(%rip), %xmm10 #326.9
        mulps     %xmm10, %xmm6                                 #329.25
        movups    960+__svml_slog_ha_data_internal(%rip), %xmm7 #347.47
        mulps     %xmm9, %xmm7                                  #348.26
        addps     448+__svml_slog_ha_data_internal(%rip), %xmm6 #329.13
        mulps     %xmm10, %xmm6                                 #331.25
        movups    576+__svml_slog_ha_data_internal(%rip), %xmm4 #315.23
        movups    896+__svml_slog_ha_data_internal(%rip), %xmm8 #346.47
        paddd     %xmm0, %xmm4                                  #315.23
        movups    640+__svml_slog_ha_data_internal(%rip), %xmm5 #315.23
        mulps     %xmm8, %xmm9                                  #349.26
        addps     384+__svml_slog_ha_data_internal(%rip), %xmm6 #331.13
        mulps     %xmm10, %xmm6                                 #333.25
        pcmpgtd   %xmm4, %xmm5                                  #315.23
        movmskps  %xmm5, %edx                                   #324.40
        addps     320+__svml_slog_ha_data_internal(%rip), %xmm6 #333.13
        mulps     %xmm10, %xmm6                                 #335.25
        addps     256+__svml_slog_ha_data_internal(%rip), %xmm6 #335.13
        mulps     %xmm10, %xmm6                                 #337.25
        addps     192+__svml_slog_ha_data_internal(%rip), %xmm6 #337.13
        mulps     %xmm10, %xmm6                                 #339.25
        addps     128+__svml_slog_ha_data_internal(%rip), %xmm6 #339.13
        mulps     %xmm10, %xmm6                                 #341.25
        addps     64+__svml_slog_ha_data_internal(%rip), %xmm6  #341.13
        mulps     %xmm10, %xmm6                                 #343.25
        addps     __svml_slog_ha_data_internal(%rip), %xmm6     #343.13
        mulps     %xmm10, %xmm6                                 #344.9
        mulps     %xmm10, %xmm6                                 #345.21
        addps     %xmm6, %xmm10                                 #345.9
        addps     %xmm7, %xmm10                                 #348.14
        addps     %xmm9, %xmm10                                 #349.14
        testl     %edx, %edx                                    #351.52
        jne       ..B2.3        # Prob 5%                       #351.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm10
..B2.2:                         # Preds ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        movaps    %xmm10, %xmm0                                 #354.12
        movq      %rbp, %rsp                                    #354.12
        popq      %rbp                                          #354.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #354.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #351.193
        movups    %xmm10, 128(%rsp)                             #351.264
                                # LOE rbx r12 r13 r14 r15 edx
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #351.448
        movq      %r12, 8(%rsp)                                 #351.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #351.448
        movq      %r13, (%rsp)                                  #351.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #351.448
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #351.511
        jc        ..B2.10       # Prob 5%                       #351.511
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #351.464
        cmpl      $4, %r12d                                     #351.459
        jl        ..B2.7        # Prob 82%                      #351.459
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm10                             #351.665
        jmp       ..B2.2        # Prob 100%                     #351.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm10
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #351.540
        lea       128(%rsp,%r12,4), %rsi                        #351.540
..___tag_value___svml_logf4_ha_e9.40:
#       __svml_slog_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog_ha_cout_rare_internal             #351.540
..___tag_value___svml_logf4_ha_e9.41:
        jmp       ..B2.8        # Prob 100%                     #351.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_logf4_ha_e9,@function
	.size	__svml_logf4_ha_e9,.-__svml_logf4_ha_e9
..LN__svml_logf4_ha_e9.1:
	.data
# -- End  __svml_logf4_ha_e9
	.text
.L_2__routine_start___svml_logf16_ha_z0_2:
# -- Begin  __svml_logf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_logf16_ha_z0
# --- __svml_logf16_ha_z0(__m512)
__svml_logf16_ha_z0:
# parameter 1: %zmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #359.1
        .byte     15                                            #463.28
        .byte     30                                            #463.28
        .byte     250                                           #463.28
	.cfi_startproc
..___tag_value___svml_logf16_ha_z0.43:
..L44:
                                                         #359.1
        vmovaps   %zmm0, %zmm6                                  #359.1
        vrcp14ps  %zmm6, %zmm1                                  #397.21
        vmovups   128+__svml_slog_ha_data_internal_avx512(%rip), %zmm2 #409.288
        vmovups   256+__svml_slog_ha_data_internal_avx512(%rip), %zmm3 #410.288
        vmovups   768+__svml_slog_ha_data_internal_avx512(%rip), %zmm11 #419.16
        vpaddd    __svml_slog_ha_data_internal_avx512(%rip), %zmm1, %zmm4 #401.19
        vmovups   640+__svml_slog_ha_data_internal_avx512(%rip), %zmm1 #406.50
        vpandd    64+__svml_slog_ha_data_internal_avx512(%rip), %zmm4, %zmm7 #402.19
        vpsrld    $18, %zmm7, %zmm5                             #403.27
        vgetexpps {sae}, %zmm7, %zmm10                          #405.22
        vmovups   960+__svml_slog_ha_data_internal_avx512(%rip), %zmm4 #413.50
        vpermt2ps 192+__svml_slog_ha_data_internal_avx512(%rip), %zmm5, %zmm2 #409.288
        vpermt2ps 320+__svml_slog_ha_data_internal_avx512(%rip), %zmm5, %zmm3 #410.288
        vfpclassps $255, %zmm7, %k2                             #407.30
        vmovups   896+__svml_slog_ha_data_internal_avx512(%rip), %zmm5 #411.50
        vfmsub213ps {rn-sae}, %zmm1, %zmm6, %zmm7               #408.16
        kmovw     %k2, %eax                                     #415.61
        vfnmadd231ps {rn-sae}, %zmm10, %zmm5, %zmm2             #412.17
        vfnmadd213ps {rn-sae}, %zmm3, %zmm4, %zmm10             #414.17
        vmovups   704+__svml_slog_ha_data_internal_avx512(%rip), %zmm3 #417.58
        vmulps    {rn-sae}, %zmm7, %zmm7, %zmm9                 #420.19
        vfmadd231ps {rn-sae}, %zmm7, %zmm3, %zmm11              #419.16
        vaddps    {rn-sae}, %zmm7, %zmm2, %zmm13                #416.21
        vsubps    {rn-sae}, %zmm2, %zmm13, %zmm8                #421.19
        vmovups   832+__svml_slog_ha_data_internal_avx512(%rip), %zmm2 #422.58
        vsubps    {rn-sae}, %zmm8, %zmm7, %zmm12                #424.18
        vfmadd213ps {rn-sae}, %zmm2, %zmm7, %zmm11              #423.16
        vfmadd213ps {rn-sae}, %zmm10, %zmm9, %zmm11             #425.16
        vaddps    {rn-sae}, %zmm12, %zmm11, %zmm14              #427.18
        vaddps    {rn-sae}, %zmm14, %zmm13, %zmm0               #428.20
        testl     %eax, %eax                                    #429.61
        jne       ..B3.3        # Prob 5%                       #429.61
                                # LOE rbx rbp r12 r13 r14 r15 zmm0 zmm1 zmm2 zmm3 zmm4 zmm5 zmm6 k2
..B3.2:                         # Preds ..B3.1
                                # Execution count [1.00e+00]
        ret                                                     #469.12
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vgetmantps $10, {sae}, %zmm6, %zmm8                     #433.33
        vgetexpps {sae}, %zmm6, %zmm11                          #434.30
        vmovups   384+__svml_slog_ha_data_internal_avx512(%rip), %zmm14 #443.294
        vmovups   512+__svml_slog_ha_data_internal_avx512(%rip), %zmm13 #444.294
        vrcp14ps  %zmm8, %zmm7                                  #436.29
        vfpclassps $223, %zmm6, %k1                             #435.36
        vrndscaleps $88, {sae}, %zmm7, %zmm9                    #437.29
        vgetexpps {sae}, %zmm9, %zmm12                          #438.31
        vfmsub231ps {rn-sae}, %zmm9, %zmm8, %zmm1               #440.24
        vpsrld    $18, %zmm9, %zmm10                            #441.50
        vsubps    {rn-sae}, %zmm12, %zmm11, %zmm15              #445.30
        vpermt2ps 448+__svml_slog_ha_data_internal_avx512(%rip), %zmm10, %zmm14 #443.294
        vpermt2ps 576+__svml_slog_ha_data_internal_avx512(%rip), %zmm10, %zmm13 #444.294
        vfmadd231ps {rn-sae}, %zmm15, %zmm5, %zmm14             #447.25
        vmovups   768+__svml_slog_ha_data_internal_avx512(%rip), %zmm5 #450.24
        vfmadd213ps {rn-sae}, %zmm13, %zmm4, %zmm15             #456.25
        vfmadd231ps {rn-sae}, %zmm1, %zmm3, %zmm5               #450.24
        vmulps    {rn-sae}, %zmm1, %zmm1, %zmm3                 #451.27
        vfmadd213ps {rn-sae}, %zmm2, %zmm1, %zmm5               #453.24
        vaddps    {rn-sae}, %zmm1, %zmm14, %zmm2                #454.29
        vfmadd213ps {rn-sae}, %zmm15, %zmm3, %zmm5              #459.24
        vsubps    {rn-sae}, %zmm14, %zmm2, %zmm4                #457.27
        vxorps    %zmm4, %zmm4, %zmm4{%k1}                      #458.25
        vsubps    {rn-sae}, %zmm4, %zmm1, %zmm6                 #460.26
        vaddps    {rn-sae}, %zmm6, %zmm5, %zmm1                 #462.26
        vaddps    {rn-sae}, %zmm1, %zmm2, %zmm0{%k2}            #463.28
        ret                                                     #463.28
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 zmm0
	.cfi_endproc
# mark_end;
	.type	__svml_logf16_ha_z0,@function
	.size	__svml_logf16_ha_z0,.-__svml_logf16_ha_z0
..LN__svml_logf16_ha_z0.2:
	.data
# -- End  __svml_logf16_ha_z0
	.text
.L_2__routine_start___svml_logf4_ha_l9_3:
# -- Begin  __svml_logf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_logf4_ha_l9
# --- __svml_logf4_ha_l9(__m128)
__svml_logf4_ha_l9:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #777.1
        .byte     15                                            #837.540
        .byte     30                                            #837.540
        .byte     250                                           #837.540
	.cfi_startproc
..___tag_value___svml_logf4_ha_l9.46:
..L47:
                                                         #777.1
        pushq     %rbp                                          #777.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #777.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #777.1
        subq      $192, %rsp                                    #777.1
        vmovups   704+__svml_slog_ha_data_internal(%rip), %xmm4 #802.18
        vpsubd    %xmm4, %xmm0, %xmm2                           #804.11
        vpand     768+__svml_slog_ha_data_internal(%rip), %xmm2, %xmm3 #805.11
        vpsrad    $23, %xmm2, %xmm1                             #806.11
        vpaddd    %xmm4, %xmm3, %xmm8                           #809.9
        vmovups   512+__svml_slog_ha_data_internal(%rip), %xmm9 #813.49
        vcvtdq2ps %xmm1, %xmm1                                  #808.9
        vsubps    832+__svml_slog_ha_data_internal(%rip), %xmm8, %xmm10 #812.9
        vfmadd213ps 448+__svml_slog_ha_data_internal(%rip), %xmm10, %xmm9 #815.13
        vmovups   640+__svml_slog_ha_data_internal(%rip), %xmm5 #798.17
        vpaddd    576+__svml_slog_ha_data_internal(%rip), %xmm0, %xmm6 #801.19
        vpcmpgtd  %xmm6, %xmm5, %xmm7                           #801.19
        vmovmskps %xmm7, %edx                                   #810.40
        vfmadd213ps 384+__svml_slog_ha_data_internal(%rip), %xmm10, %xmm9 #817.13
        vfmadd213ps 320+__svml_slog_ha_data_internal(%rip), %xmm10, %xmm9 #819.13
        vfmadd213ps 256+__svml_slog_ha_data_internal(%rip), %xmm10, %xmm9 #821.13
        vfmadd213ps 192+__svml_slog_ha_data_internal(%rip), %xmm10, %xmm9 #823.13
        vfmadd213ps 128+__svml_slog_ha_data_internal(%rip), %xmm10, %xmm9 #825.13
        vfmadd213ps 64+__svml_slog_ha_data_internal(%rip), %xmm10, %xmm9 #827.13
        vfmadd213ps __svml_slog_ha_data_internal(%rip), %xmm10, %xmm9 #829.13
        vmulps    %xmm9, %xmm10, %xmm11                         #830.9
        vfmadd213ps %xmm10, %xmm10, %xmm11                      #831.9
        vfmadd231ps 960+__svml_slog_ha_data_internal(%rip), %xmm1, %xmm11 #834.14
        vfmadd132ps 896+__svml_slog_ha_data_internal(%rip), %xmm11, %xmm1 #835.14
        testl     %edx, %edx                                    #837.52
        jne       ..B4.3        # Prob 5%                       #837.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm1
..B4.2:                         # Preds ..B4.3 ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        movaps    %xmm1, %xmm0                                  #840.12
        movq      %rbp, %rsp                                    #840.12
        popq      %rbp                                          #840.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #840.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm0, 64(%rsp)                               #837.193
        vmovups   %xmm1, 128(%rsp)                              #837.264
        je        ..B4.2        # Prob 95%                      #837.368
                                # LOE rbx r12 r13 r14 r15 edx xmm1
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #837.448
        movq      %r12, 8(%rsp)                                 #837.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #837.448
        movq      %r13, (%rsp)                                  #837.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #837.448
                                # LOE rbx r12 r14 r15 r13d
..B4.7:                         # Preds ..B4.8 ..B4.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #837.511
        jc        ..B4.10       # Prob 5%                       #837.511
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #837.464
        cmpl      $4, %r12d                                     #837.459
        jl        ..B4.7        # Prob 82%                      #837.459
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm1                              #837.665
        jmp       ..B4.2        # Prob 100%                     #837.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm1
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #837.540
        lea       128(%rsp,%r12,4), %rsi                        #837.540
..___tag_value___svml_logf4_ha_l9.64:
#       __svml_slog_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog_ha_cout_rare_internal             #837.540
..___tag_value___svml_logf4_ha_l9.65:
        jmp       ..B4.8        # Prob 100%                     #837.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_logf4_ha_l9,@function
	.size	__svml_logf4_ha_l9,.-__svml_logf4_ha_l9
..LN__svml_logf4_ha_l9.3:
	.data
# -- End  __svml_logf4_ha_l9
	.text
.L_2__routine_start___svml_logf8_ha_e9_4:
# -- Begin  __svml_logf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_logf8_ha_e9
# --- __svml_logf8_ha_e9(__m256)
__svml_logf8_ha_e9:
# parameter 1: %ymm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #845.1
        .byte     15                                            #905.546
        .byte     30                                            #905.546
        .byte     250                                           #905.546
	.cfi_startproc
..___tag_value___svml_logf8_ha_e9.67:
..L68:
                                                         #845.1
        pushq     %rbp                                          #845.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #845.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #845.1
        subq      $192, %rsp                                    #845.1
        vmovaps   %ymm0, %ymm8                                  #845.1
        vextractf128 $1, %ymm8, %xmm5                           #867.101
        vmovups   704+__svml_slog_ha_data_internal(%rip), %xmm4 #870.22
        vmovups   768+__svml_slog_ha_data_internal(%rip), %xmm3 #871.25
        vpsubd    %xmm4, %xmm5, %xmm2                           #872.79
        vpsrad    $23, %xmm2, %xmm10                            #874.64
        vpand     %xmm3, %xmm2, %xmm15                          #873.82
        vpaddd    %xmm4, %xmm15, %xmm2                          #877.90
        vmovups   576+__svml_slog_ha_data_internal(%rip), %xmm7 #865.21
        vmovups   640+__svml_slog_ha_data_internal(%rip), %xmm6 #866.21
        vpaddd    %xmm5, %xmm7, %xmm5                           #869.100
        vmovaps   %xmm8, %xmm0                                  #867.33
        vpsubd    %xmm4, %xmm0, %xmm12                          #872.15
        vpsrad    $23, %xmm12, %xmm9                            #874.15
        vpand     %xmm3, %xmm12, %xmm13                         #873.15
        vpaddd    %xmm4, %xmm13, %xmm14                         #877.55
        vpaddd    %xmm7, %xmm0, %xmm3                           #869.23
        vpcmpgtd  %xmm3, %xmm6, %xmm4                           #869.23
        vpcmpgtd  %xmm5, %xmm6, %xmm6                           #869.100
        vpackssdw %xmm6, %xmm4, %xmm7                           #878.75
        vinsertf128 $1, %xmm10, %ymm9, %ymm11                   #876.29
        vcvtdq2ps %ymm11, %ymm1                                 #876.9
        vpxor     %xmm9, %xmm9, %xmm9                           #878.58
        vpacksswb %xmm9, %xmm7, %xmm10                          #878.58
        vpmovmskb %xmm10, %edx                                  #878.39
        vinsertf128 $1, %xmm2, %ymm14, %ymm11                   #877.9
        vsubps    832+__svml_slog_ha_data_internal(%rip), %ymm11, %ymm0 #880.9
        vmulps    512+__svml_slog_ha_data_internal(%rip), %ymm0, %ymm12 #883.28
        vaddps    448+__svml_slog_ha_data_internal(%rip), %ymm12, %ymm13 #883.13
        vmulps    %ymm13, %ymm0, %ymm15                         #885.28
        vaddps    384+__svml_slog_ha_data_internal(%rip), %ymm15, %ymm2 #885.13
        vmulps    %ymm2, %ymm0, %ymm3                           #887.28
        vaddps    320+__svml_slog_ha_data_internal(%rip), %ymm3, %ymm4 #887.13
        vmulps    %ymm4, %ymm0, %ymm5                           #889.28
        vaddps    256+__svml_slog_ha_data_internal(%rip), %ymm5, %ymm6 #889.13
        vmulps    %ymm6, %ymm0, %ymm7                           #891.28
        vaddps    192+__svml_slog_ha_data_internal(%rip), %ymm7, %ymm9 #891.13
        vmulps    %ymm9, %ymm0, %ymm10                          #893.28
        vaddps    128+__svml_slog_ha_data_internal(%rip), %ymm10, %ymm11 #893.13
        vmulps    %ymm11, %ymm0, %ymm12                         #895.28
        vaddps    64+__svml_slog_ha_data_internal(%rip), %ymm12, %ymm13 #895.13
        vmulps    %ymm13, %ymm0, %ymm14                         #897.28
        vaddps    __svml_slog_ha_data_internal(%rip), %ymm14, %ymm15 #897.13
        vmulps    %ymm15, %ymm0, %ymm2                          #898.9
        vmulps    %ymm2, %ymm0, %ymm3                           #899.24
        vaddps    %ymm3, %ymm0, %ymm2                           #899.9
        vmulps    960+__svml_slog_ha_data_internal(%rip), %ymm1, %ymm3 #902.29
        vmulps    896+__svml_slog_ha_data_internal(%rip), %ymm1, %ymm1 #903.29
        vaddps    %ymm3, %ymm2, %ymm2                           #902.14
        vaddps    %ymm1, %ymm2, %ymm0                           #903.14
        testb     %dl, %dl                                      #905.52
        jne       ..B5.3        # Prob 5%                       #905.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm8
..B5.2:                         # Preds ..B5.3 ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #908.12
        popq      %rbp                                          #908.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #908.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm8, 64(%rsp)                               #905.196
        vmovups   %ymm0, 128(%rsp)                              #905.270
        testl     %edx, %edx                                    #905.374
        je        ..B5.2        # Prob 95%                      #905.374
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #905.454
                                # LOE rbx r12 r13 r14 r15 eax edx
..B5.13:                        # Preds ..B5.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #905.517
        jc        ..B5.10       # Prob 5%                       #905.517
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #905.470
        cmpl      $8, %r12d                                     #905.465
        jl        ..B5.7        # Prob 82%                      #905.465
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #905.674
        jmp       ..B5.2        # Prob 100%                     #905.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #905.546
        lea       128(%rsp,%r12,4), %rsi                        #905.546
..___tag_value___svml_logf8_ha_e9.85:
#       __svml_slog_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog_ha_cout_rare_internal             #905.546
..___tag_value___svml_logf8_ha_e9.86:
        jmp       ..B5.8        # Prob 100%                     #905.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_logf8_ha_e9,@function
	.size	__svml_logf8_ha_e9,.-__svml_logf8_ha_e9
..LN__svml_logf8_ha_e9.4:
	.data
# -- End  __svml_logf8_ha_e9
	.text
.L_2__routine_start___svml_logf8_ha_l9_5:
# -- Begin  __svml_logf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_logf8_ha_l9
# --- __svml_logf8_ha_l9(__m256)
__svml_logf8_ha_l9:
# parameter 1: %ymm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #913.1
        .byte     15                                            #973.546
        .byte     30                                            #973.546
        .byte     250                                           #973.546
	.cfi_startproc
..___tag_value___svml_logf8_ha_l9.88:
..L89:
                                                         #913.1
        pushq     %rbp                                          #913.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #913.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #913.1
        subq      $192, %rsp                                    #913.1
        vmovups   704+__svml_slog_ha_data_internal(%rip), %ymm4 #938.18
        vmovups   512+__svml_slog_ha_data_internal(%rip), %ymm9 #949.52
        vmovups   640+__svml_slog_ha_data_internal(%rip), %ymm5 #934.17
        vpaddd    576+__svml_slog_ha_data_internal(%rip), %ymm0, %ymm6 #936.15
        vpsubd    %ymm4, %ymm0, %ymm2                           #940.11
        vpand     768+__svml_slog_ha_data_internal(%rip), %ymm2, %ymm3 #941.11
        vpsrad    $23, %ymm2, %ymm1                             #942.11
        vpaddd    %ymm4, %ymm3, %ymm8                           #943.11
        vcvtdq2ps %ymm1, %ymm1                                  #944.9
        vsubps    832+__svml_slog_ha_data_internal(%rip), %ymm8, %ymm10 #948.9
        vfmadd213ps 448+__svml_slog_ha_data_internal(%rip), %ymm10, %ymm9 #951.13
        vfmadd213ps 384+__svml_slog_ha_data_internal(%rip), %ymm10, %ymm9 #953.13
        vfmadd213ps 320+__svml_slog_ha_data_internal(%rip), %ymm10, %ymm9 #955.13
        vfmadd213ps 256+__svml_slog_ha_data_internal(%rip), %ymm10, %ymm9 #957.13
        vfmadd213ps 192+__svml_slog_ha_data_internal(%rip), %ymm10, %ymm9 #959.13
        vfmadd213ps 128+__svml_slog_ha_data_internal(%rip), %ymm10, %ymm9 #961.13
        vfmadd213ps 64+__svml_slog_ha_data_internal(%rip), %ymm10, %ymm9 #963.13
        vfmadd213ps __svml_slog_ha_data_internal(%rip), %ymm10, %ymm9 #965.13
        vmulps    %ymm9, %ymm10, %ymm11                         #966.9
        vfmadd213ps %ymm10, %ymm10, %ymm11                      #967.9
        vfmadd231ps 960+__svml_slog_ha_data_internal(%rip), %ymm1, %ymm11 #970.14
        vpcmpgtd  %ymm6, %ymm5, %ymm7                           #937.19
        vmovmskps %ymm7, %edx                                   #946.41
        vfmadd132ps 896+__svml_slog_ha_data_internal(%rip), %ymm11, %ymm1 #971.14
        testl     %edx, %edx                                    #973.52
        jne       ..B6.3        # Prob 5%                       #973.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm1
..B6.2:                         # Preds ..B6.3 ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        vmovaps   %ymm1, %ymm0                                  #976.12
        movq      %rbp, %rsp                                    #976.12
        popq      %rbp                                          #976.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #976.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm0, 64(%rsp)                               #973.196
        vmovups   %ymm1, 128(%rsp)                              #973.270
        je        ..B6.2        # Prob 95%                      #973.374
                                # LOE rbx r12 r13 r14 r15 edx ymm1
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #973.454
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
        btl       %r12d, %r13d                                  #973.517
        jc        ..B6.10       # Prob 5%                       #973.517
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #973.470
        cmpl      $8, %r12d                                     #973.465
        jl        ..B6.7        # Prob 82%                      #973.465
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm1                              #973.674
        jmp       ..B6.2        # Prob 100%                     #973.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm1
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #973.546
        lea       128(%rsp,%r12,4), %rsi                        #973.546
..___tag_value___svml_logf8_ha_l9.106:
#       __svml_slog_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog_ha_cout_rare_internal             #973.546
..___tag_value___svml_logf8_ha_l9.107:
        jmp       ..B6.8        # Prob 100%                     #973.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_logf8_ha_l9,@function
	.size	__svml_logf8_ha_l9,.-__svml_logf8_ha_l9
..LN__svml_logf8_ha_l9.5:
	.data
# -- End  __svml_logf8_ha_l9
	.text
.L_2__routine_start___svml_slog_ha_cout_rare_internal_6:
# -- Begin  __svml_slog_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_slog_ha_cout_rare_internal
	.globl __svml_slog_ha_cout_rare_internal
# --- __svml_slog_ha_cout_rare_internal(const float *, float *)
__svml_slog_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #685.1
        .byte     15                                            #764.13
        .byte     30                                            #764.13
        .byte     250                                           #764.13
	.cfi_startproc
..___tag_value___svml_slog_ha_cout_rare_internal.109:
..L110:
                                                        #685.1
        xorl      %eax, %eax                                    #695.14
        movzwl    2(%rdi), %edx                                 #696.32
        andl      $32640, %edx                                  #696.32
        cmpl      $32640, %edx                                  #696.57
        je        ..B7.12       # Prob 16%                      #696.57
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B7.2:                         # Preds ..B7.1
                                # Execution count [8.40e-01]
        pxor      %xmm2, %xmm2                                  #698.13
        xorl      %ecx, %ecx                                    #699.9
        cvtss2sd  (%rdi), %xmm2                                 #698.13
        movsd     %xmm2, -8(%rsp)                               #698.9
        movzwl    -2(%rsp), %edx                                #700.35
        testl     $32752, %edx                                  #700.35
        jne       ..B7.4        # Prob 50%                      #700.57
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm2
..B7.3:                         # Preds ..B7.2
                                # Execution count [4.20e-01]
        mulsd     1600+_imlsLnHATab(%rip), %xmm2                #702.13
        movl      $-60, %ecx                                    #703.13
        movsd     %xmm2, -8(%rsp)                               #702.13
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm2
..B7.4:                         # Preds ..B7.3 ..B7.2
                                # Execution count [8.40e-01]
        movsd     1608+_imlsLnHATab(%rip), %xmm0                #705.35
        comisd    %xmm0, %xmm2                                  #705.35
        jbe       ..B7.8        # Prob 50%                      #705.35
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm0 xmm2
..B7.5:                         # Preds ..B7.4
                                # Execution count [4.20e-01]
        movsd     .L_2il0floatpacket.85(%rip), %xmm3            #707.13
        movaps    %xmm2, %xmm1                                  #707.21
        subsd     %xmm3, %xmm1                                  #707.21
        movsd     %xmm1, -16(%rsp)                              #708.13
        andb      $127, -9(%rsp)                                #709.34
        movsd     -16(%rsp), %xmm0                              #710.18
        comisd    1592+_imlsLnHATab(%rip), %xmm0                #710.44
        jbe       ..B7.7        # Prob 50%                      #710.44
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm1 xmm2 xmm3
..B7.6:                         # Preds ..B7.5
                                # Execution count [2.10e-01]
        movsd     %xmm2, -16(%rsp)                              #716.17
        pxor      %xmm6, %xmm6                                  #713.31
        movzwl    -10(%rsp), %edi                               #717.38
        lea       _imlsLnHATab(%rip), %r10                      #720.44
        andl      $-32753, %edi                                 #717.38
        addl      $16368, %edi                                  #717.38
        movw      %di, -10(%rsp)                                #717.38
        movsd     -16(%rsp), %xmm4                              #718.25
        movaps    %xmm4, %xmm1                                  #718.46
        movaps    %xmm4, %xmm2                                  #725.46
        movsd     1672+_imlsLnHATab(%rip), %xmm9                #731.45
        movzwl    -2(%rsp), %edx                                #712.44
        andl      $32752, %edx                                  #712.44
        addsd     1576+_imlsLnHATab(%rip), %xmm1                #718.46
        addsd     1584+_imlsLnHATab(%rip), %xmm2                #725.46
        movsd     %xmm1, -24(%rsp)                              #718.17
        movl      -24(%rsp), %r8d                               #719.42
        movsd     %xmm2, -24(%rsp)                              #725.17
        andl      $127, %r8d                                    #719.73
        movsd     -24(%rsp), %xmm7                              #726.25
        movsd     1560+_imlsLnHATab(%rip), %xmm5                #714.51
        movsd     1568+_imlsLnHATab(%rip), %xmm0                #715.51
        shrl      $4, %edx                                      #712.44
        subsd     1584+_imlsLnHATab(%rip), %xmm7                #726.50
        lea       (%r8,%r8,2), %r9d                             #720.60
        movsd     (%r10,%r9,8), %xmm8                           #720.44
        lea       -1023(%rcx,%rdx), %ecx                        #712.17
        cvtsi2sd  %ecx, %xmm6                                   #713.31
        subsd     %xmm7, %xmm4                                  #727.29
        mulsd     %xmm8, %xmm7                                  #728.35
        mulsd     %xmm6, %xmm5                                  #714.51
        subsd     %xmm3, %xmm7                                  #728.43
        mulsd     %xmm4, %xmm8                                  #729.35
        mulsd     %xmm0, %xmm6                                  #715.51
        addsd     8(%r10,%r9,8), %xmm5                          #723.36
        addsd     16(%r10,%r9,8), %xmm6                         #724.36
        movaps    %xmm7, %xmm3                                  #730.29
        addsd     %xmm8, %xmm3                                  #730.29
        mulsd     %xmm3, %xmm9                                  #731.64
        addsd     1664+_imlsLnHATab(%rip), %xmm9                #731.83
        mulsd     %xmm3, %xmm9                                  #731.103
        addsd     1656+_imlsLnHATab(%rip), %xmm9                #731.124
        mulsd     %xmm3, %xmm9                                  #731.144
        addsd     1648+_imlsLnHATab(%rip), %xmm9                #731.165
        mulsd     %xmm3, %xmm9                                  #731.185
        addsd     1640+_imlsLnHATab(%rip), %xmm9                #731.206
        mulsd     %xmm3, %xmm9                                  #731.226
        addsd     1632+_imlsLnHATab(%rip), %xmm9                #731.247
        mulsd     %xmm3, %xmm9                                  #731.267
        mulsd     %xmm3, %xmm3                                  #732.26
        addsd     1624+_imlsLnHATab(%rip), %xmm9                #731.288
        mulsd     %xmm3, %xmm9                                  #732.17
        addsd     %xmm5, %xmm9                                  #733.35
        addsd     %xmm6, %xmm9                                  #735.35
        addsd     %xmm7, %xmm9                                  #734.36
        addsd     %xmm8, %xmm9                                  #734.45
        cvtsd2ss  %xmm9, %xmm9                                  #735.17
        movss     %xmm9, (%rsi)                                 #735.17
        ret                                                     #735.17
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.7:                         # Preds ..B7.5
                                # Execution count [2.10e-01]
        movsd     1672+_imlsLnHATab(%rip), %xmm2                #739.45
        movaps    %xmm1, %xmm0                                  #741.26
        mulsd     %xmm1, %xmm2                                  #739.64
        mulsd     %xmm1, %xmm0                                  #741.26
        addsd     1664+_imlsLnHATab(%rip), %xmm2                #739.85
        mulsd     %xmm1, %xmm2                                  #739.105
        addsd     1656+_imlsLnHATab(%rip), %xmm2                #739.126
        mulsd     %xmm1, %xmm2                                  #739.146
        addsd     1648+_imlsLnHATab(%rip), %xmm2                #739.167
        mulsd     %xmm1, %xmm2                                  #739.187
        addsd     1640+_imlsLnHATab(%rip), %xmm2                #739.208
        mulsd     %xmm1, %xmm2                                  #739.228
        addsd     1632+_imlsLnHATab(%rip), %xmm2                #740.40
        mulsd     %xmm1, %xmm2                                  #740.60
        addsd     1624+_imlsLnHATab(%rip), %xmm2                #740.81
        mulsd     %xmm0, %xmm2                                  #741.17
        addsd     %xmm1, %xmm2                                  #742.17
        cvtsd2ss  %xmm2, %xmm2                                  #743.17
        movss     %xmm2, (%rsi)                                 #743.17
        ret                                                     #743.17
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.8:                         # Preds ..B7.4
                                # Execution count [4.20e-01]
        ucomisd   %xmm0, %xmm2                                  #748.40
        jp        ..B7.9        # Prob 0%                       #748.40
        je        ..B7.11       # Prob 16%                      #748.40
                                # LOE rbx rbp rsi r12 r13 r14 r15 xmm0
..B7.9:                         # Preds ..B7.8
                                # Execution count [3.53e-01]
        divsd     %xmm0, %xmm0                                  #755.79
        cvtsd2ss  %xmm0, %xmm0                                  #755.17
        movss     %xmm0, (%rsi)                                 #755.17
        movl      $1, %eax                                      #756.17
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.10:                        # Preds ..B7.9
                                # Execution count [1.00e+00]
        ret                                                     #772.12
                                # LOE
..B7.11:                        # Preds ..B7.8
                                # Execution count [6.72e-02]: Infreq
        movsd     1616+_imlsLnHATab(%rip), %xmm1                #750.42
        movl      $2, %eax                                      #751.17
        xorps     .L_2il0floatpacket.84(%rip), %xmm1            #750.42
        divsd     %xmm0, %xmm1                                  #750.80
        cvtsd2ss  %xmm1, %xmm1                                  #750.17
        movss     %xmm1, (%rsi)                                 #750.17
        ret                                                     #750.17
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.12:                        # Preds ..B7.1
                                # Execution count [1.60e-01]: Infreq
        movb      3(%rdi), %dl                                  #762.36
        andb      $-128, %dl                                    #762.36
        cmpb      $-128, %dl                                    #762.57
        je        ..B7.14       # Prob 16%                      #762.57
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B7.13:                        # Preds ..B7.14 ..B7.12
                                # Execution count [1.47e-01]: Infreq
        movss     (%rdi), %xmm0                                 #769.20
        mulss     %xmm0, %xmm0                                  #769.27
        movss     %xmm0, (%rsi)                                 #769.13
        ret                                                     #769.13
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.14:                        # Preds ..B7.12
                                # Execution count [2.56e-02]: Infreq
        testl     $8388607, (%rdi)                              #762.85
        jne       ..B7.13       # Prob 50%                      #762.113
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B7.15:                        # Preds ..B7.14
                                # Execution count [1.28e-02]: Infreq
        movsd     1608+_imlsLnHATab(%rip), %xmm0                #764.75
        movl      $1, %eax                                      #765.13
        divsd     %xmm0, %xmm0                                  #764.75
        cvtsd2ss  %xmm0, %xmm0                                  #764.13
        movss     %xmm0, (%rsi)                                 #764.13
        ret                                                     #764.13
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_slog_ha_cout_rare_internal,@function
	.size	__svml_slog_ha_cout_rare_internal,.-__svml_slog_ha_cout_rare_internal
..LN__svml_slog_ha_cout_rare_internal.6:
	.data
# -- End  __svml_slog_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_slog_ha_data_internal_avx512
	.globl __svml_slog_ha_data_internal_avx512
__svml_slog_ha_data_internal_avx512:
	.long	131072
	.long	131072
	.long	131072
	.long	131072
	.long	131072
	.long	131072
	.long	131072
	.long	131072
	.long	131072
	.long	131072
	.long	131072
	.long	131072
	.long	131072
	.long	131072
	.long	131072
	.long	131072
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	4294705152
	.long	0
	.long	3170631680
	.long	3178782720
	.long	3182919680
	.long	3186704384
	.long	3189024768
	.long	3190814720
	.long	3192557568
	.long	3194257408
	.long	3195914240
	.long	3196795904
	.long	3197585408
	.long	3198356480
	.long	3199110144
	.long	3199848448
	.long	3200569344
	.long	3201275904
	.long	3201968128
	.long	3202646016
	.long	3203310592
	.long	3203961856
	.long	3204524544
	.long	3204838400
	.long	3205146112
	.long	3205448192
	.long	3205745152
	.long	3206036992
	.long	3206324224
	.long	3206605824
	.long	3206883328
	.long	3207156224
	.long	3207424512
	.long	0
	.long	3072770974
	.long	929538039
	.long	3075640037
	.long	930648533
	.long	3072716864
	.long	3066151582
	.long	3073718761
	.long	897812054
	.long	3057871602
	.long	923619665
	.long	921315575
	.long	3057394118
	.long	3078028640
	.long	924853521
	.long	3075349253
	.long	3071259390
	.long	906511159
	.long	906200662
	.long	917494258
	.long	3061185264
	.long	3071618351
	.long	919715245
	.long	3052715317
	.long	3078288258
	.long	3077608526
	.long	3074005625
	.long	930702671
	.long	3070133351
	.long	913590776
	.long	924241186
	.long	3021499198
	.long	1060205056
	.long	1059688960
	.long	1059187712
	.long	1058701824
	.long	1058229248
	.long	1057769472
	.long	1057321984
	.long	1056807936
	.long	1055958016
	.long	1055129600
	.long	1054320640
	.long	1053531136
	.long	1052760064
	.long	1052006400
	.long	1051268096
	.long	1050547200
	.long	1049840640
	.long	1049148416
	.long	1048365056
	.long	1047035904
	.long	1045733376
	.long	1044455424
	.long	1043200000
	.long	1041969152
	.long	1040760832
	.long	1038958592
	.long	1036623872
	.long	1034330112
	.long	1032073216
	.long	1027907584
	.long	1023541248
	.long	1015087104
	.long	901758606
	.long	3071200204
	.long	931108809
	.long	3074069268
	.long	3077535321
	.long	3071146094
	.long	3063010043
	.long	3072147991
	.long	908173938
	.long	3049723733
	.long	925190435
	.long	923601997
	.long	3048768765
	.long	3076457870
	.long	926424291
	.long	3073778483
	.long	3069146713
	.long	912794238
	.long	912483742
	.long	920635797
	.long	3054902185
	.long	3069864633
	.long	922801832
	.long	3033791132
	.long	3076717488
	.long	3076037756
	.long	3072434855
	.long	3077481184
	.long	3066991812
	.long	917116064
	.long	925811956
	.long	900509991
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
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
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
	.long	262144
	.long	262144
	.long	262144
	.long	262144
	.long	262144
	.long	262144
	.long	262144
	.long	262144
	.long	262144
	.long	262144
	.long	262144
	.long	262144
	.long	262144
	.long	262144
	.long	262144
	.long	262144
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	4294443008
	.long	0
	.long	3178782720
	.long	3186704384
	.long	3190814720
	.long	3194257408
	.long	3196795904
	.long	3198356480
	.long	3199848448
	.long	3201275904
	.long	3202646016
	.long	3203961856
	.long	3204838400
	.long	3205448192
	.long	3206036992
	.long	3206605824
	.long	3207156224
	.long	0
	.long	929538039
	.long	930648533
	.long	3066151582
	.long	897812054
	.long	923619665
	.long	3057394118
	.long	924853521
	.long	3071259390
	.long	906200662
	.long	3061185264
	.long	919715245
	.long	3078288258
	.long	3074005625
	.long	3070133351
	.long	924241186
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
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	1045236958
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	3196066480
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	1051372199
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.long	3204448254
	.type	__svml_slog_ha_data_internal_avx512,@object
	.size	__svml_slog_ha_data_internal_avx512,1920
	.align 64
	.hidden __svml_slog_ha_data_internal
	.globl __svml_slog_ha_data_internal
__svml_slog_ha_data_internal:
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	3204448256
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	1051372163
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	3196059512
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	1045227540
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	3190476518
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	1041197964
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	3187247262
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	1041183581
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	3187933896
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
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
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	2139095040
	.long	4286578688
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.long	1065353216
	.long	3212836864
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.long	0
	.long	2147483648
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.type	__svml_slog_ha_data_internal,@object
	.size	__svml_slog_ha_data_internal,1216
	.align 32
_imlsLnHATab:
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1072660480
	.long	1486880768
	.long	1066410070
	.long	1813744607
	.long	3179892593
	.long	0
	.long	1072629760
	.long	377487360
	.long	1067416219
	.long	919019713
	.long	3179241129
	.long	0
	.long	1072599040
	.long	1513619456
	.long	1067944025
	.long	874573033
	.long	3178512940
	.long	0
	.long	1072570368
	.long	3221749760
	.long	1068427825
	.long	4181665006
	.long	3177478212
	.long	0
	.long	1072541696
	.long	4162322432
	.long	1068708823
	.long	627020255
	.long	1028629941
	.long	0
	.long	1072513024
	.long	183107584
	.long	1068957907
	.long	2376703469
	.long	1030233118
	.long	0
	.long	1072486400
	.long	1053425664
	.long	1069192557
	.long	696277142
	.long	1030474863
	.long	0
	.long	1072459776
	.long	3996123136
	.long	1069430535
	.long	2630798680
	.long	1028792016
	.long	0
	.long	1072435200
	.long	3452764160
	.long	1069600382
	.long	624954044
	.long	3177101741
	.long	0
	.long	1072409600
	.long	207650816
	.long	1069717971
	.long	3272735636
	.long	3175176575
	.long	0
	.long	1072386048
	.long	2647228416
	.long	1069827627
	.long	3594228712
	.long	1029303785
	.long	0
	.long	1072362496
	.long	2712010752
	.long	1069938736
	.long	3653242769
	.long	3176839013
	.long	0
	.long	1072338944
	.long	374439936
	.long	1070051337
	.long	4072775574
	.long	3176577495
	.long	0
	.long	1072316416
	.long	3707174912
	.long	1070160474
	.long	1486946159
	.long	1023930920
	.long	0
	.long	1072294912
	.long	1443954688
	.long	1070265993
	.long	293532967
	.long	3176278277
	.long	0
	.long	1072273408
	.long	127762432
	.long	1070372856
	.long	3404145447
	.long	3177023955
	.long	0
	.long	1072252928
	.long	2053832704
	.long	1070475911
	.long	1575076358
	.long	1029048544
	.long	0
	.long	1072232448
	.long	3194093568
	.long	1070580248
	.long	1864169120
	.long	1026866084
	.long	0
	.long	1072212992
	.long	3917201408
	.long	1070638340
	.long	2362145246
	.long	3175606197
	.long	0
	.long	1072193536
	.long	3417112576
	.long	1070689116
	.long	70087871
	.long	3174183577
	.long	0
	.long	1072175104
	.long	4226777088
	.long	1070737793
	.long	1620410586
	.long	3174700065
	.long	0
	.long	1072156672
	.long	3168870400
	.long	1070787042
	.long	311238082
	.long	1025781772
	.long	0
	.long	1072139264
	.long	2150580224
	.long	1070834092
	.long	1664262457
	.long	3175299224
	.long	0
	.long	1072120832
	.long	4095672320
	.long	1070884491
	.long	1657121015
	.long	3174674199
	.long	0
	.long	1072104448
	.long	2595577856
	.long	1070929805
	.long	2014006823
	.long	3175423830
	.long	0
	.long	1072087040
	.long	3747176448
	.long	1070978493
	.long	144991708
	.long	3171552042
	.long	0
	.long	1072070656
	.long	1050435584
	.long	1071024840
	.long	3386227432
	.long	1027876916
	.long	0
	.long	1072055296
	.long	255516672
	.long	1071068760
	.long	2637594316
	.long	1028049573
	.long	0
	.long	1072038912
	.long	1640783872
	.long	1071116120
	.long	893247007
	.long	1028452162
	.long	0
	.long	1072023552
	.long	2940411904
	.long	1071161011
	.long	813240633
	.long	1027664048
	.long	0
	.long	1072009216
	.long	882917376
	.long	1071203348
	.long	2376597551
	.long	3175828767
	.long	0
	.long	1071993856
	.long	213966848
	.long	1071249188
	.long	2977204125
	.long	1028350609
	.long	0
	.long	1071979520
	.long	2921504768
	.long	1071292428
	.long	523218347
	.long	1028007004
	.long	0
	.long	1071965184
	.long	3186655232
	.long	1071336119
	.long	2352907891
	.long	1026967097
	.long	0
	.long	1071951872
	.long	2653364224
	.long	1071377101
	.long	2453418583
	.long	3174349512
	.long	0
	.long	1071938560
	.long	3759783936
	.long	1071418487
	.long	3685870403
	.long	3175415611
	.long	0
	.long	1071925248
	.long	2468364288
	.long	1071460286
	.long	1578908842
	.long	3175510517
	.long	0
	.long	1071911936
	.long	81903616
	.long	1071502506
	.long	770710269
	.long	1026742353
	.long	0
	.long	1071899648
	.long	2799321088
	.long	1071541858
	.long	3822266185
	.long	1028434427
	.long	0
	.long	1071886336
	.long	2142265344
	.long	1071584911
	.long	175901806
	.long	3173871540
	.long	0
	.long	1071874048
	.long	2944024576
	.long	1071625048
	.long	2747360403
	.long	1027672159
	.long	0
	.long	1071862784
	.long	3434301440
	.long	1071653426
	.long	4194662196
	.long	3173893003
	.long	0
	.long	1071850496
	.long	1547755520
	.long	1071673870
	.long	4248764681
	.long	3172759087
	.long	0
	.long	1071839232
	.long	4246986752
	.long	1071692786
	.long	2840205638
	.long	3174430911
	.long	0
	.long	1071826944
	.long	3418390528
	.long	1071713619
	.long	3041880823
	.long	1025440860
	.long	0
	.long	1071816704
	.long	4143093760
	.long	1071731139
	.long	2727587401
	.long	3173965207
	.long	0
	.long	1071805440
	.long	3121326080
	.long	1071750582
	.long	3173887692
	.long	3174190163
	.long	0
	.long	1071794176
	.long	1852893184
	.long	1071770207
	.long	3951060252
	.long	1027348295
	.long	0
	.long	1071783936
	.long	3636379648
	.long	1071788208
	.long	1684924001
	.long	3174777086
	.long	0
	.long	1071773696
	.long	516505600
	.long	1071806366
	.long	429181199
	.long	3173211033
	.long	0
	.long	1071763456
	.long	4186185728
	.long	1071824681
	.long	2044904577
	.long	3174967132
	.long	0
	.long	1071753216
	.long	877596672
	.long	1071843159
	.long	1396318105
	.long	3173959727
	.long	0
	.long	1071742976
	.long	2912784384
	.long	1071861800
	.long	448136789
	.long	3174814192
	.long	0
	.long	1071733760
	.long	3722825728
	.long	1071878720
	.long	714165913
	.long	3173439560
	.long	0
	.long	1071723520
	.long	2522374144
	.long	1071897682
	.long	3227240353
	.long	3173394323
	.long	0
	.long	1071714304
	.long	4165410816
	.long	1071914895
	.long	1365684961
	.long	3174365060
	.long	0
	.long	1071705088
	.long	3477135360
	.long	1071932251
	.long	368482985
	.long	3174140821
	.long	0
	.long	1071695872
	.long	2079455232
	.long	1071949752
	.long	1320576317
	.long	1026822714
	.long	0
	.long	1071687680
	.long	851795968
	.long	1071965432
	.long	3702467026
	.long	1025224125
	.long	0
	.long	1071678464
	.long	647743488
	.long	1071983213
	.long	772992109
	.long	3174038459
	.long	0
	.long	1071670272
	.long	26537984
	.long	1071999146
	.long	2360214276
	.long	3174861275
	.long	0
	.long	1071661056
	.long	1547061248
	.long	1072017216
	.long	2886781435
	.long	1026423395
	.long	0
	.long	1071652864
	.long	2854492160
	.long	1072033410
	.long	215631550
	.long	1025638968
	.long	0
	.long	1071644672
	.long	4277811200
	.long	1072049730
	.long	2479318832
	.long	1026487127
	.long	4277811200
	.long	1072049730
	.long	2479318832
	.long	1026487127
	.long	64
	.long	1120927744
	.long	0
	.long	1094713344
	.long	0
	.long	1065615360
	.long	0
	.long	1135607808
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	0
	.long	3219128320
	.long	1431655955
	.long	1070945621
	.long	610
	.long	3218079744
	.long	2545118337
	.long	1070176665
	.long	1378399119
	.long	3217380693
	.long	612435357
	.long	1069697472
	.long	94536557
	.long	3217031348
	.type	_imlsLnHATab,@object
	.size	_imlsLnHATab,1680
	.align 16
.L_2il0floatpacket.84:
	.long	0x00000000,0x80000000,0x00000000,0x00000000
	.type	.L_2il0floatpacket.84,@object
	.size	.L_2il0floatpacket.84,16
	.align 8
.L_2il0floatpacket.85:
	.long	0x00000000,0x3ff00000
	.type	.L_2il0floatpacket.85,@object
	.size	.L_2il0floatpacket.85,8
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
