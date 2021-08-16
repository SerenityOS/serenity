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
	.file "svml_s_pow.c"
	.text
..TXTST0:
.L_2__routine_start___svml_powf16_ha_z0_0:
# -- Begin  __svml_powf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_powf16_ha_z0
# --- __svml_powf16_ha_z0(__m512, __m512)
__svml_powf16_ha_z0:
# parameter 1: %zmm0
# parameter 2: %zmm1
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #3421.1
        .byte     15                                            #3556.878
        .byte     30                                            #3556.878
        .byte     250                                           #3556.878
	.cfi_startproc
..___tag_value___svml_powf16_ha_z0.1:
..L2:
                                                          #3421.1
        pushq     %rbp                                          #3421.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #3421.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #3421.1
        subq      $256, %rsp                                    #3421.1
        vgetmantps $10, {sae}, %zmm0, %zmm5                     #3487.28
        vmovups   576+__svml_spow_ha_data_internal_avx512(%rip), %zmm10 #3497.53
        vmovups   512+__svml_spow_ha_data_internal_avx512(%rip), %zmm9 #3495.53
        vmovups   640+__svml_spow_ha_data_internal_avx512(%rip), %zmm12 #3505.61
        vmovups   704+__svml_spow_ha_data_internal_avx512(%rip), %zmm13 #3506.61
        vgetexpps {sae}, %zmm0, %zmm15                          #3488.25
        vmovups   __svml_spow_ha_data_internal_avx512(%rip), %zmm14 #3503.287
        vmovups   128+__svml_spow_ha_data_internal_avx512(%rip), %zmm4 #3504.287
        vrcp14ps  %zmm5, %zmm7                                  #3491.24
        vfpclassps $223, %zmm0, %k0                             #3489.27
        vrndscaleps $88, {sae}, %zmm7, %zmm8                    #3492.24
        vmovups   832+__svml_spow_ha_data_internal_avx512(%rip), %zmm7 #3513.22
        kmovw     %k0, %esi                                     #3489.27
        vfmsub213ps {rn-sae}, %zmm10, %zmm8, %zmm5              #3498.19
        vcmpps    $17, {sae}, %zmm9, %zmm8, %k2                 #3496.26
        vfmadd231ps {rn-sae}, %zmm5, %zmm12, %zmm13             #3507.22
        vmovups   896+__svml_spow_ha_data_internal_avx512(%rip), %zmm9 #3518.22
        vmovups   1024+__svml_spow_ha_data_internal_avx512(%rip), %zmm12 #3519.58
        vmulps    {rn-sae}, %zmm5, %zmm5, %zmm3                 #3508.22
        vpsrld    $18, %zmm8, %zmm11                            #3499.45
        vaddps    {rn-sae}, %zmm10, %zmm15, %zmm15{%k2}         #3501.25
        vmovups   960+__svml_spow_ha_data_internal_avx512(%rip), %zmm10 #3515.58
        vpermt2ps 64+__svml_spow_ha_data_internal_avx512(%rip), %zmm11, %zmm14 #3503.287
        vpermt2ps 192+__svml_spow_ha_data_internal_avx512(%rip), %zmm11, %zmm4 #3504.287
        vaddps    {rn-sae}, %zmm15, %zmm14, %zmm8               #3511.22
        vmovaps   %zmm1, %zmm6                                  #3421.1
        vmovups   768+__svml_spow_ha_data_internal_avx512(%rip), %zmm1 #3509.61
        vfpclassps $153, %zmm6, %k1                             #3490.28
        vfmadd231ps {rn-sae}, %zmm5, %zmm13, %zmm1              #3510.22
        kmovw     %k1, %eax                                     #3490.28
        vfmadd231ps {rn-sae}, %zmm5, %zmm1, %zmm7               #3513.22
        vmovaps   %zmm5, %zmm2                                  #3514.21
        orl       %eax, %esi                                    #3494.48
        vfmsub213ps {rn-sae}, %zmm3, %zmm5, %zmm2               #3514.21
        vfmadd231ps {rn-sae}, %zmm5, %zmm7, %zmm9               #3518.22
        vfmadd213ps {rn-sae}, %zmm4, %zmm12, %zmm2              #3520.20
        vmovaps   %zmm8, %zmm11                                 #3516.23
        vfmadd231ps {rn-sae}, %zmm5, %zmm10, %zmm11             #3516.23
        vfmadd231ps {rn-sae}, %zmm5, %zmm9, %zmm2               #3523.20
        vmovups   1216+__svml_spow_ha_data_internal_avx512(%rip), %zmm9 #3547.63
        vsubps    {rn-sae}, %zmm8, %zmm11, %zmm4                #3521.26
        vmovaps   %zmm11, %zmm14                                #3522.23
        vfmadd231ps {rn-sae}, %zmm3, %zmm12, %zmm14             #3522.23
        vfmsub213ps {rn-sae}, %zmm4, %zmm10, %zmm5              #3524.24
        vmovups   1344+__svml_spow_ha_data_internal_avx512(%rip), %zmm10 #3550.63
        vaddps    {rn-sae}, %zmm2, %zmm14, %zmm1                #3526.24
        vsubps    {rn-sae}, %zmm11, %zmm14, %zmm13              #3525.27
        vmulps    {rz-sae}, %zmm6, %zmm1, %zmm4                 #3529.21
        vfmsub213ps {rn-sae}, %zmm13, %zmm12, %zmm3             #3527.25
        vmovups   384+__svml_spow_ha_data_internal_avx512(%rip), %zmm12 #3544.288
        vmovups   256+__svml_spow_ha_data_internal_avx512(%rip), %zmm13 #3543.288
        vsubps    {rn-sae}, %zmm14, %zmm1, %zmm15               #3528.23
        vaddps    {rn-sae}, %zmm3, %zmm5, %zmm3                 #3530.26
        vfmsub213ps {rz-sae}, %zmm4, %zmm6, %zmm1               #3532.20
        vmovups   1088+__svml_spow_ha_data_internal_avx512(%rip), %zmm5 #3538.57
        vmovups   1280+__svml_spow_ha_data_internal_avx512(%rip), %zmm14 #3548.63
        vsubps    {rn-sae}, %zmm15, %zmm2, %zmm2                #3531.22
        vaddps    {rn-sae}, %zmm3, %zmm2, %zmm2                 #3533.22
        vfmadd213ps {rz-sae}, %zmm1, %zmm6, %zmm2               #3534.20
        vaddps    {rz-sae}, %zmm2, %zmm4, %zmm1                 #3535.22
        vsubps    {rn-sae}, %zmm4, %zmm1, %zmm4                 #3537.23
        vaddps    {rd-sae}, %zmm5, %zmm1, %zmm7                 #3539.28
        vreduceps $81, {sae}, %zmm1, %zmm3                      #3536.20
        vsubps    {rn-sae}, %zmm4, %zmm2, %zmm5                 #3540.22
        vpermt2ps 448+__svml_spow_ha_data_internal_avx512(%rip), %zmm7, %zmm12 #3544.288
        vpermt2ps 320+__svml_spow_ha_data_internal_avx512(%rip), %zmm7, %zmm13 #3543.288
        vaddps    {rn-sae}, %zmm5, %zmm3, %zmm8                 #3541.22
        vandps    1152+__svml_spow_ha_data_internal_avx512(%rip), %zmm8, %zmm11 #3546.20
        vfmadd231ps {rn-sae}, %zmm11, %zmm9, %zmm14             #3549.20
        vfmadd213ps {rn-sae}, %zmm10, %zmm11, %zmm14            #3551.20
        vfmadd213ps {rn-sae}, %zmm12, %zmm11, %zmm14            #3552.20
        vfmadd213ps {rn-sae}, %zmm13, %zmm13, %zmm14            #3553.20
        vscalefps {rn-sae}, %zmm1, %zmm14, %zmm1                #3554.21
        jne       ..B1.3        # Prob 5%                       #3556.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm1 zmm6
..B1.2:                         # Preds ..B1.9 ..B1.7 ..B1.1
                                # Execution count [1.00e+00]
        vmovaps   %zmm1, %zmm0                                  #3559.12
        movq      %rbp, %rsp                                    #3559.12
        popq      %rbp                                          #3559.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #3559.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #3556.293
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm1 zmm6
..B1.4:                         # Preds ..B1.3
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #3556.293
        movl      %edx, %eax                                    #3556.347
        orl       $8064, %eax                                   #3556.347
        cmpl      %eax, %edx                                    #3556.376
        je        ..B1.6        # Prob 78%                      #3556.376
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm1 zmm6
..B1.5:                         # Preds ..B1.4
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #3556.388
        vldmxcsr  32(%rsp)                                      #3556.388
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm1 zmm6
..B1.6:                         # Preds ..B1.5 ..B1.4
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm0, 64(%rsp)                               #3556.448
        vmovups   %zmm6, 128(%rsp)                              #3556.522
        vmovups   %zmm1, 192(%rsp)                              #3556.596
        testl     %esi, %esi                                    #3556.704
        jne       ..B1.11       # Prob 5%                       #3556.704
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm1
..B1.7:                         # Preds ..B1.14 ..B1.6
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #3556.1062
        je        ..B1.2        # Prob 78%                      #3556.1062
                                # LOE rbx r12 r13 r14 r15 edx zmm1
..B1.8:                         # Preds ..B1.7
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #3556.1087
        movl      32(%rsp), %eax                                #3556.1087
                                # LOE rbx r12 r13 r14 r15 eax edx zmm1
..B1.9:                         # Preds ..B1.8
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #3556.1074
        orl       %edx, %eax                                    #3556.1074
        movl      %eax, 32(%rsp)                                #3556.1074
        vldmxcsr  32(%rsp)                                      #3556.1074
        jmp       ..B1.2        # Prob 100%                     #3556.1074
                                # LOE rbx r12 r13 r14 r15 zmm1
..B1.11:                        # Preds ..B1.6
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #3556.786
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B1.18:                        # Preds ..B1.11
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 16(%rsp)                                #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x10, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
        movq      %r14, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %esi, %r14d                                   #
        movq      %rbx, 24(%rsp)                                #[spill]
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x18, 0xff, 0xff, 0xff, 0x22
        movl      %ecx, %ebx                                    #
                                # LOE rbx r15 r12d r13d r14d
..B1.12:                        # Preds ..B1.13 ..B1.18
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #3556.849
        jc        ..B1.15       # Prob 5%                       #3556.849
                                # LOE rbx r15 r12d r13d r14d
..B1.13:                        # Preds ..B1.15 ..B1.12
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #3556.802
        cmpl      $16, %ebx                                     #3556.797
        jl        ..B1.12       # Prob 82%                      #3556.797
                                # LOE rbx r15 r12d r13d r14d
..B1.14:                        # Preds ..B1.13
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
        vmovups   192(%rsp), %zmm1                              #3556.1017
        jmp       ..B1.7        # Prob 100%                     #3556.1017
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x18, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x10, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm1
..B1.15:                        # Preds ..B1.12
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,4), %rdi                         #3556.878
        lea       128(%rsp,%rbx,4), %rsi                        #3556.878
        lea       192(%rsp,%rbx,4), %rdx                        #3556.878
..___tag_value___svml_powf16_ha_z0.29:
#       __svml_spow_ha_cout_rare_internal(const float *, float *, float *)
        call      __svml_spow_ha_cout_rare_internal             #3556.878
..___tag_value___svml_powf16_ha_z0.30:
        jmp       ..B1.13       # Prob 100%                     #3556.878
        .align    16,0x90
                                # LOE r15 ebx r12d r13d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_powf16_ha_z0,@function
	.size	__svml_powf16_ha_z0,.-__svml_powf16_ha_z0
..LN__svml_powf16_ha_z0.0:
	.data
# -- End  __svml_powf16_ha_z0
	.text
.L_2__routine_start___svml_powf8_ha_l9_1:
# -- Begin  __svml_powf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_powf8_ha_l9
# --- __svml_powf8_ha_l9(__m256, __m256)
__svml_powf8_ha_l9:
# parameter 1: %ymm0
# parameter 2: %ymm1
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #4288.1
        .byte     15                                            #4438.663
        .byte     30                                            #4438.663
        .byte     250                                           #4438.663
	.cfi_startproc
..___tag_value___svml_powf8_ha_l9.32:
..L33:
                                                         #4288.1
        pushq     %rbp                                          #4288.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #4288.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #4288.1
        subq      $256, %rsp                                    #4288.1
        lea       13952+__svml_spow_ha_data_internal(%rip), %rdx #4390.78
        vmovupd   12992+__svml_spow_ha_data_internal(%rip), %ymm8 #4374.54
        vmovupd   13056+__svml_spow_ha_data_internal(%rip), %ymm14 #4375.52
        vmovaps   %ymm1, %ymm13                                 #4288.1
        vextractf128 $1, %ymm0, %xmm9                           #4363.102
        vcvtps2pd %xmm0, %ymm7                                  #4363.19
        vpsubd    __svml_spow_ha_data_internal(%rip), %ymm0, %ymm10 #4369.19
        vcvtps2pd %xmm9, %ymm2                                  #4363.85
        vandpd    %ymm8, %ymm7, %ymm4                           #4376.21
        vandpd    %ymm8, %ymm2, %ymm6                           #4376.77
        vorpd     %ymm14, %ymm4, %ymm8                          #4377.21
        vcvtpd2ps %ymm8, %xmm15                                 #4380.46
        vpsrlq    $20, %ymm2, %ymm2                             #4382.77
        vpsrlq    $20, %ymm7, %ymm7                             #4382.20
        vrcpps    %xmm15, %xmm5                                 #4380.35
        vorpd     %ymm14, %ymm6, %ymm4                          #4377.80
        vcvtpd2ps %ymm4, %xmm1                                  #4380.121
        vcvtps2pd %xmm5, %ymm3                                  #4380.19
        vmovups   __VPACK_ODD_ind.1470.0.4(%rip), %ymm14        #4383.263
        vrcpps    %xmm1, %xmm6                                  #4380.110
        vroundpd  $0, %ymm3, %ymm1                              #4384.19
        vcvtps2pd %xmm6, %ymm5                                  #4380.94
        vpermps   %ymm2, %ymm14, %ymm15                         #4383.309
        vpermps   %ymm7, %ymm14, %ymm6                          #4383.189
        vroundpd  $0, %ymm5, %ymm14                             #4384.91
        vmovupd   13312+__svml_spow_ha_data_internal(%rip), %ymm5 #4386.50
        vfmsub213pd %ymm5, %ymm1, %ymm8                         #4387.15
        vfmsub213pd %ymm5, %ymm14, %ymm4                        #4387.82
        vcvtdq2pd %xmm15, %ymm3                                 #4385.104
        vpsrlq    $40, %ymm1, %ymm15                            #4389.20
        vmovd     %xmm15, %eax                                  #4390.176
        movslq    %eax, %rax                                    #4390.78
        vpextrd   $2, %xmm15, %ecx                              #4390.338
        movslq    %ecx, %rcx                                    #4390.240
        vcvtdq2pd %xmm6, %ymm2                                  #4385.24
        vpsrlq    $40, %ymm14, %ymm6                            #4389.78
        vmovsd    -4232448(%rdx,%rax), %xmm7                    #4390.78
        vmovhpd   -4232448(%rdx,%rcx), %xmm7, %xmm5             #4390.61
        vextractf128 $1, %ymm15, %xmm7                          #4390.541
        vmovd     %xmm7, %esi                                   #4390.522
        movslq    %esi, %rsi                                    #4390.424
        vpextrd   $2, %xmm7, %edi                               #4390.689
        movslq    %edi, %rdi                                    #4390.591
        vmovd     %xmm6, %r8d                                   #4390.937
        vmovsd    -4232448(%rdx,%rsi), %xmm15                   #4390.424
        vmovhpd   -4232448(%rdx,%rdi), %xmm15, %xmm7            #4390.407
        vpextrd   $2, %xmm6, %r9d                               #4390.1099
        vextractf128 $1, %ymm6, %xmm6                           #4390.1302
        vmovd     %xmm6, %r10d                                  #4390.1283
        movslq    %r8d, %r8                                     #4390.839
        movslq    %r9d, %r9                                     #4390.1001
        movslq    %r10d, %r10                                   #4390.1185
        vpextrd   $2, %xmm6, %r11d                              #4390.1450
        movslq    %r11d, %r11                                   #4390.1352
        vmovsd    -4232448(%rdx,%r8), %xmm15                    #4390.839
        vinsertf128 $1, %xmm7, %ymm5, %ymm5                     #4390.15
        vmovhpd   -4232448(%rdx,%r9), %xmm15, %xmm7             #4390.822
        vmovsd    -4232448(%rdx,%r10), %xmm15                   #4390.1185
        vmovhpd   -4232448(%rdx,%r11), %xmm15, %xmm6            #4390.1168
        vmovupd   13504+__svml_spow_ha_data_internal(%rip), %ymm15 #4391.56
        vcmplt_oqpd %ymm1, %ymm15, %ymm1                        #4395.18
        vcmplt_oqpd %ymm14, %ymm15, %ymm14                      #4395.89
        vmovupd   13568+__svml_spow_ha_data_internal(%rip), %ymm15 #4396.18
        vandpd    %ymm15, %ymm1, %ymm1                          #4396.18
        vandpd    %ymm15, %ymm14, %ymm14                        #4396.71
        vmovupd   13632+__svml_spow_ha_data_internal(%rip), %ymm15 #4396.125
        vorpd     %ymm15, %ymm1, %ymm1                          #4396.125
        vorpd     %ymm15, %ymm14, %ymm14                        #4396.178
        vsubpd    %ymm1, %ymm2, %ymm2                           #4397.20
        vsubpd    %ymm14, %ymm3, %ymm3                          #4397.78
        vmovupd   12928+__svml_spow_ha_data_internal(%rip), %ymm14 #4400.60
        vmovapd   %ymm14, %ymm15                                #4401.16
        vinsertf128 $1, %xmm6, %ymm7, %ymm6                     #4390.776
        vmovupd   13696+__svml_spow_ha_data_internal(%rip), %ymm7 #4394.49
        vmulpd    %ymm2, %ymm7, %ymm1                           #4398.15
        vmulpd    %ymm3, %ymm7, %ymm2                           #4398.65
        vmovupd   12864+__svml_spow_ha_data_internal(%rip), %ymm3 #4399.60
        vmulpd    %ymm8, %ymm8, %ymm7                           #4402.15
        vfmadd231pd %ymm8, %ymm3, %ymm15                        #4401.16
        vfmadd231pd %ymm4, %ymm3, %ymm14                        #4401.95
        vmulpd    %ymm4, %ymm4, %ymm3                           #4402.60
        vmulpd    %ymm7, %ymm15, %ymm15                         #4403.15
        vmulpd    %ymm3, %ymm14, %ymm3                          #4403.61
        vmovups   64+__svml_spow_ha_data_internal(%rip), %ymm14 #4370.36
        vaddpd    %ymm15, %ymm8, %ymm8                          #4404.15
        vaddpd    %ymm3, %ymm4, %ymm4                           #4404.60
        vaddpd    %ymm8, %ymm5, %ymm8                           #4405.15
        vaddpd    %ymm4, %ymm6, %ymm4                           #4405.60
        vaddpd    %ymm8, %ymm1, %ymm5                           #4406.15
        vaddpd    %ymm4, %ymm2, %ymm3                           #4406.60
        vmovupd   30464+__svml_spow_ha_data_internal(%rip), %ymm1 #4413.56
        vpcmpgtd  %ymm14, %ymm10, %ymm15                        #4370.36
        vpcmpeqd  %ymm14, %ymm10, %ymm10                        #4370.86
        vextractf128 $1, %ymm13, %xmm11                         #4364.102
        vcvtps2pd %xmm13, %ymm9                                 #4364.19
        vcvtps2pd %xmm11, %ymm12                                #4364.85
        vpand     192+__svml_spow_ha_data_internal(%rip), %ymm13, %ymm11 #4371.19
        vmulpd    %ymm5, %ymm9, %ymm7                           #4407.15
        vmovupd   30528+__svml_spow_ha_data_internal(%rip), %ymm9 #4410.53
        vmulpd    %ymm3, %ymm12, %ymm8                          #4407.60
        vmovupd   30400+__svml_spow_ha_data_internal(%rip), %ymm12 #4408.57
        vmulpd    %ymm7, %ymm12, %ymm5                          #4409.17
        vmulpd    %ymm8, %ymm12, %ymm4                          #4409.72
        vsubpd    %ymm9, %ymm5, %ymm12                          #4411.15
        vsubpd    %ymm9, %ymm4, %ymm9                           #4411.66
        vaddpd    %ymm12, %ymm1, %ymm12                         #4414.15
        vaddpd    %ymm9, %ymm1, %ymm9                           #4414.67
        vsubpd    %ymm1, %ymm12, %ymm2                          #4415.15
        vsubpd    %ymm1, %ymm9, %ymm3                           #4415.67
        vmovups   __VPACK_ODD_ind.1471.0.4(%rip), %ymm1         #4416.259
        vsubpd    %ymm2, %ymm5, %ymm5                           #4426.15
        vsubpd    %ymm3, %ymm4, %ymm2                           #4426.62
        vmovupd   30592+__svml_spow_ha_data_internal(%rip), %ymm4 #4427.51
        vpermps   %ymm7, %ymm1, %ymm6                           #4416.188
        vpermps   %ymm8, %ymm1, %ymm7                           #4416.305
        vmovups   128+__svml_spow_ha_data_internal(%rip), %ymm8 #4372.36
        vpor      %ymm10, %ymm15, %ymm1                         #4370.19
        vpcmpgtd  %ymm8, %ymm11, %ymm10                         #4372.36
        vpcmpeqd  %ymm8, %ymm11, %ymm11                         #4372.82
        vmovupd   30656+__svml_spow_ha_data_internal(%rip), %ymm8 #4424.24
        vinsertf128 $1, %xmm7, %ymm6, %ymm7                     #4416.118
        vpor      %ymm11, %ymm10, %ymm6                         #4372.19
        vpor      %ymm6, %ymm1, %ymm14                          #4373.23
        vandps    30720+__svml_spow_ha_data_internal(%rip), %ymm7, %ymm7 #4418.18
        vpcmpgtd  30784+__svml_spow_ha_data_internal(%rip), %ymm7, %ymm15 #4420.25
        vpor      %ymm15, %ymm14, %ymm11                        #4421.23
        vmovmskps %ymm11, %eax                                  #4422.45
        vandps    %ymm8, %ymm12, %ymm11                         #4425.17
        vandps    %ymm8, %ymm9, %ymm10                          #4425.78
        vmulpd    %ymm5, %ymm4, %ymm8                           #4428.15
        vmulpd    %ymm2, %ymm4, %ymm4                           #4428.62
        vpsrlq    $11, %ymm12, %ymm12                           #4429.17
        vpsrlq    $11, %ymm9, %ymm9                             #4429.65
        vpsllq    $52, %ymm9, %ymm9                             #4431.72
        vmovd     %xmm11, %ecx                                  #4430.128
        vextractf128 $1, %ymm11, %xmm1                          #4430.318
        shll      $3, %ecx                                      #4430.2219
        vmovd     %xmm1, %edi                                   #4430.299
        vmovq     (%rdx,%rcx), %xmm3                            #4430.2219
        vpextrd   $2, %xmm11, %esi                              #4430.212
        shll      $3, %esi                                      #4430.2187
        shll      $3, %edi                                      #4430.2423
        vpextrd   $2, %xmm1, %r8d                               #4430.388
        vmovhpd   (%rdx,%rsi), %xmm3, %xmm11                    #4430.2187
        shll      $3, %r8d                                      #4430.2391
        vmovq     (%rdx,%rdi), %xmm2                            #4430.2423
        vmovhpd   (%rdx,%r8), %xmm2, %xmm3                      #4430.2391
        vextractf128 $1, %ymm10, %xmm5                          #4430.670
        vmovd     %xmm10, %r9d                                  #4430.480
        vmovd     %xmm5, %ecx                                   #4430.651
        shll      $3, %r9d                                      #4430.2686
        vpextrd   $2, %xmm10, %r10d                             #4430.564
        shll      $3, %ecx                                      #4430.2890
        vpextrd   $2, %xmm5, %esi                               #4430.740
        shll      $3, %r10d                                     #4430.2654
        vmovq     (%rdx,%r9), %xmm6                             #4430.2686
        shll      $3, %esi                                      #4430.2858
        vmovq     (%rdx,%rcx), %xmm10                           #4430.2890
        vmovhpd   (%rdx,%r10), %xmm6, %xmm7                     #4430.2654
        vmovhpd   (%rdx,%rsi), %xmm10, %xmm5                    #4430.2858
        vinsertf128 $1, %xmm3, %ymm11, %ymm11                   #4430.2141
        vfmadd213pd %ymm11, %ymm8, %ymm11                       #4432.17
        vinsertf128 $1, %xmm5, %ymm7, %ymm14                    #4430.2608
        vfmadd213pd %ymm14, %ymm4, %ymm14                       #4432.75
        vpsllq    $52, %ymm12, %ymm4                            #4431.17
        vpaddq    %ymm9, %ymm14, %ymm3                          #4434.71
        vpaddq    %ymm4, %ymm11, %ymm1                          #4434.17
        vcvtpd2ps %ymm3, %xmm4                                  #4436.94
        vcvtpd2ps %ymm1, %xmm2                                  #4436.60
        vinsertf128 $1, %xmm4, %ymm2, %ymm1                     #4436.14
        testl     %eax, %eax                                    #4438.52
        jne       ..B2.3        # Prob 5%                       #4438.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm1 ymm13
..B2.2:                         # Preds ..B2.3 ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        vmovaps   %ymm1, %ymm0                                  #4441.12
        movq      %rbp, %rsp                                    #4441.12
        popq      %rbp                                          #4441.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #4441.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm0, 64(%rsp)                               #4438.239
        vmovups   %ymm13, 128(%rsp)                             #4438.313
        vmovups   %ymm1, 192(%rsp)                              #4438.387
        je        ..B2.2        # Prob 95%                      #4438.491
                                # LOE rbx r12 r13 r14 r15 eax ymm1
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #4438.571
                                # LOE rbx r12 r13 r14 r15 eax edx
..B2.13:                        # Preds ..B2.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #4438.634
        jc        ..B2.10       # Prob 5%                       #4438.634
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #4438.587
        cmpl      $8, %r12d                                     #4438.582
        jl        ..B2.7        # Prob 82%                      #4438.582
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   192(%rsp), %ymm1                              #4438.804
        jmp       ..B2.2        # Prob 100%                     #4438.804
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm1
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #4438.663
        lea       128(%rsp,%r12,4), %rsi                        #4438.663
        lea       192(%rsp,%r12,4), %rdx                        #4438.663
..___tag_value___svml_powf8_ha_l9.50:
#       __svml_spow_ha_cout_rare_internal(const float *, float *, float *)
        call      __svml_spow_ha_cout_rare_internal             #4438.663
..___tag_value___svml_powf8_ha_l9.51:
        jmp       ..B2.8        # Prob 100%                     #4438.663
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_powf8_ha_l9,@function
	.size	__svml_powf8_ha_l9,.-__svml_powf8_ha_l9
..LN__svml_powf8_ha_l9.1:
	.section .rodata, "a"
	.align 64
	.align 64
__VPACK_ODD_ind.1470.0.4:
	.long	1
	.long	3
	.long	5
	.long	7
	.long	0
	.long	0
	.long	0
	.long	0
	.space 32, 0x00 	# pad
	.align 64
__VPACK_ODD_ind.1471.0.4:
	.long	1
	.long	3
	.long	5
	.long	7
	.long	0
	.long	0
	.long	0
	.long	0
	.data
# -- End  __svml_powf8_ha_l9
	.text
.L_2__routine_start___svml_powf4_ha_e9_2:
# -- Begin  __svml_powf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_powf4_ha_e9
# --- __svml_powf4_ha_e9(__m128, __m128)
__svml_powf4_ha_e9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #4446.1
        .byte     15                                            #4596.654
        .byte     30                                            #4596.654
        .byte     250                                           #4596.654
	.cfi_startproc
..___tag_value___svml_powf4_ha_e9.53:
..L54:
                                                         #4446.1
        pushq     %rbp                                          #4446.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #4446.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #4446.1
        subq      $256, %rsp                                    #4446.1
        lea       13952+__svml_spow_ha_data_internal(%rip), %rdx #4548.76
        vcvtps2pd %xmm0, %ymm4                                  #4521.19
        vcvtps2pd %xmm1, %ymm12                                 #4522.19
        vmovups   64+__svml_spow_ha_data_internal(%rip), %xmm13 #4524.25
        vmovups   128+__svml_spow_ha_data_internal(%rip), %xmm14 #4525.21
        vpand     192+__svml_spow_ha_data_internal(%rip), %xmm1, %xmm3 #4529.23
        vandpd    12992+__svml_spow_ha_data_internal(%rip), %ymm4, %ymm2 #4534.19
        vorpd     13056+__svml_spow_ha_data_internal(%rip), %ymm2, %ymm11 #4535.19
        vcvtpd2ps %ymm11, %xmm6                                 #4538.44
        vpsubd    __svml_spow_ha_data_internal(%rip), %xmm0, %xmm15 #4527.23
        vrcpps    %xmm6, %xmm5                                  #4538.33
        vcvtps2pd %xmm5, %ymm10                                 #4538.17
        vroundpd  $0, %ymm10, %ymm10                            #4542.17
        vmulpd    %ymm10, %ymm11, %ymm11                        #4545.28
        vpsrlq    $20, %xmm4, %xmm8                             #4540.22
        vextractf128 $1, %ymm4, %xmm7                           #4539.110
        vpsrlq    $20, %xmm7, %xmm9                             #4540.80
        vshufps   $221, %xmm9, %xmm8, %xmm2                     #4541.41
        vcvtdq2pd %xmm2, %ymm9                                  #4543.24
        vsubpd    13312+__svml_spow_ha_data_internal(%rip), %ymm11, %ymm8 #4545.13
        vpsrlq    $40, %xmm10, %xmm6                            #4547.22
        vextractf128 $1, %ymm10, %xmm2                          #4546.114
        vmovd     %xmm6, %eax                                   #4548.174
        vpsrlq    $40, %xmm2, %xmm7                             #4547.81
        vmovd     %xmm7, %esi                                   #4548.472
        movslq    %eax, %rax                                    #4548.76
        vpextrd   $2, %xmm6, %ecx                               #4548.312
        movslq    %esi, %rsi                                    #4548.374
        vpextrd   $2, %xmm7, %edi                               #4548.610
        movslq    %ecx, %rcx                                    #4548.214
        movslq    %edi, %rdi                                    #4548.512
        vmovupd   13504+__svml_spow_ha_data_internal(%rip), %ymm6 #4549.54
        vmovsd    -4232448(%rdx,%rax), %xmm5                    #4548.76
        vmovsd    -4232448(%rdx,%rsi), %xmm11                   #4548.374
        vmovhpd   -4232448(%rdx,%rcx), %xmm5, %xmm4             #4548.59
        vmovhpd   -4232448(%rdx,%rdi), %xmm11, %xmm2            #4548.357
        vcmplt_oqpd %ymm10, %ymm6, %ymm5                        #4553.16
        vandpd    13568+__svml_spow_ha_data_internal(%rip), %ymm5, %ymm10 #4554.16
        vmulpd    %ymm8, %ymm8, %ymm5                           #4560.13
        vorpd     13632+__svml_spow_ha_data_internal(%rip), %ymm10, %ymm11 #4554.64
        vsubpd    %ymm11, %ymm9, %ymm9                          #4555.18
        vmovupd   30656+__svml_spow_ha_data_internal(%rip), %xmm11 #4582.26
        vmulpd    13696+__svml_spow_ha_data_internal(%rip), %ymm9, %ymm9 #4556.13
        vinsertf128 $1, %xmm2, %ymm4, %ymm7                     #4548.13
        vmulpd    12864+__svml_spow_ha_data_internal(%rip), %ymm8, %ymm2 #4559.29
        vaddpd    12928+__svml_spow_ha_data_internal(%rip), %ymm2, %ymm6 #4559.14
        vmulpd    %ymm5, %ymm6, %ymm4                           #4561.13
        vmovupd   30464+__svml_spow_ha_data_internal(%rip), %ymm5 #4571.54
        vaddpd    %ymm4, %ymm8, %ymm8                           #4562.13
        vaddpd    %ymm8, %ymm7, %ymm2                           #4563.13
        vpcmpgtd  %xmm13, %xmm15, %xmm7                         #4528.37
        vpcmpeqd  %xmm13, %xmm15, %xmm13                        #4528.92
        vpcmpgtd  %xmm14, %xmm3, %xmm15                         #4530.37
        vpcmpeqd  %xmm14, %xmm3, %xmm14                         #4530.88
        vpor      %xmm13, %xmm7, %xmm7                          #4528.23
        vaddpd    %ymm2, %ymm9, %ymm6                           #4564.13
        vpor      %xmm14, %xmm15, %xmm3                         #4530.23
        vpor      %xmm3, %xmm7, %xmm8                           #4531.27
        vmulpd    %ymm6, %ymm12, %ymm12                         #4565.13
        vmulpd    30400+__svml_spow_ha_data_internal(%rip), %ymm12, %ymm6 #4567.15
        vsubpd    30528+__svml_spow_ha_data_internal(%rip), %ymm6, %ymm2 #4569.13
        vaddpd    %ymm2, %ymm5, %ymm2                           #4572.13
        vsubpd    %ymm5, %ymm2, %ymm5                           #4573.13
        vsubpd    %ymm5, %ymm6, %ymm5                           #4584.13
        vextractf128 $1, %ymm12, %xmm4                          #4570.104
        vshufps   $221, %xmm4, %xmm12, %xmm4                    #4574.40
        vandps    30720+__svml_spow_ha_data_internal(%rip), %xmm4, %xmm4 #4576.22
        vpcmpgtd  30784+__svml_spow_ha_data_internal(%rip), %xmm4, %xmm9 #4578.29
        vmulpd    30592+__svml_spow_ha_data_internal(%rip), %ymm5, %ymm4 #4586.13
        vpor      %xmm9, %xmm8, %xmm10                          #4579.27
        vmovmskps %xmm10, %eax                                  #4580.44
        vandps    %xmm11, %xmm2, %xmm12                         #4583.19
        vextractf128 $1, %ymm2, %xmm3                           #4581.104
        vpsrlq    $11, %xmm2, %xmm2                             #4587.19
        vmovd     %xmm12, %r8d                                  #4588.128
        vpsllq    $52, %xmm2, %xmm2                             #4589.19
        vandps    %xmm11, %xmm3, %xmm14                         #4583.83
        vpsrlq    $11, %xmm3, %xmm3                             #4587.68
        vmovd     %xmm14, %r10d                                 #4588.247
        shll      $3, %r8d                                      #4588.1086
        vpextrd   $2, %xmm12, %r9d                              #4588.186
        shll      $3, %r10d                                     #4588.1286
        vpextrd   $2, %xmm14, %r11d                             #4588.304
        shll      $3, %r9d                                      #4588.1054
        vmovq     (%rdx,%r8), %xmm6                             #4588.1086
        shll      $3, %r11d                                     #4588.1254
        vmovq     (%rdx,%r10), %xmm15                           #4588.1286
        vmovhpd   (%rdx,%r9), %xmm6, %xmm13                     #4588.1054
        vmovhpd   (%rdx,%r11), %xmm15, %xmm6                    #4588.1254
        vinsertf128 $1, %xmm6, %ymm13, %ymm5                    #4588.1008
        vmulpd    %ymm4, %ymm5, %ymm7                           #4590.30
        vaddpd    %ymm7, %ymm5, %ymm8                           #4590.15
        vpsllq    $52, %xmm3, %xmm5                             #4589.75
        vextractf128 $1, %ymm8, %xmm9                           #4591.106
        vpaddq    %xmm2, %xmm8, %xmm4                           #4592.19
        vpaddq    %xmm5, %xmm9, %xmm6                           #4592.76
        vinsertf128 $1, %xmm6, %ymm4, %ymm7                     #4593.13
        vcvtpd2ps %ymm7, %xmm2                                  #4594.14
        testl     %eax, %eax                                    #4596.52
        jne       ..B3.3        # Prob 5%                       #4596.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1 xmm2
..B3.13:                        # Preds ..B3.3 ..B3.1
                                # Execution count [9.50e-01]
        vzeroupper                                              #
                                # LOE rbx r12 r13 r14 r15 xmm2
..B3.2:                         # Preds ..B3.9 ..B3.13
                                # Execution count [1.00e+00]
        movaps    %xmm2, %xmm0                                  #4599.12
        movq      %rbp, %rsp                                    #4599.12
        popq      %rbp                                          #4599.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #4599.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm0, 64(%rsp)                               #4596.236
        vmovups   %xmm1, 128(%rsp)                              #4596.307
        vmovups   %xmm2, 192(%rsp)                              #4596.378
        je        ..B3.13       # Prob 95%                      #4596.482
                                # LOE rbx r12 r13 r14 r15 eax xmm2
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #4596.562
                                # LOE rbx r12 r13 r14 r15 eax edx
..B3.15:                        # Preds ..B3.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B3.7:                         # Preds ..B3.8 ..B3.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #4596.625
        jc        ..B3.10       # Prob 5%                       #4596.625
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #4596.578
        cmpl      $4, %r12d                                     #4596.573
        jl        ..B3.7        # Prob 82%                      #4596.573
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm2                              #4596.792
        jmp       ..B3.2        # Prob 100%                     #4596.792
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm2
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #4596.654
        lea       128(%rsp,%r12,4), %rsi                        #4596.654
        lea       192(%rsp,%r12,4), %rdx                        #4596.654
..___tag_value___svml_powf4_ha_e9.71:
#       __svml_spow_ha_cout_rare_internal(const float *, float *, float *)
        call      __svml_spow_ha_cout_rare_internal             #4596.654
..___tag_value___svml_powf4_ha_e9.72:
        jmp       ..B3.8        # Prob 100%                     #4596.654
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_powf4_ha_e9,@function
	.size	__svml_powf4_ha_e9,.-__svml_powf4_ha_e9
..LN__svml_powf4_ha_e9.2:
	.data
# -- End  __svml_powf4_ha_e9
	.text
.L_2__routine_start___svml_powf8_ha_e9_3:
# -- Begin  __svml_powf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_powf8_ha_e9
# --- __svml_powf8_ha_e9(__m256, __m256)
__svml_powf8_ha_e9:
# parameter 1: %ymm0
# parameter 2: %ymm1
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #4604.1
        .byte     15                                            #4754.663
        .byte     30                                            #4754.663
        .byte     250                                           #4754.663
	.cfi_startproc
..___tag_value___svml_powf8_ha_e9.74:
..L75:
                                                         #4604.1
        pushq     %rbp                                          #4604.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #4604.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #4604.1
        subq      $256, %rsp                                    #4604.1
        lea       13952+__svml_spow_ha_data_internal(%rip), %rdx #4706.78
        vmovups   %ymm0, (%rsp)                                 #4604.1[spill]
        vmovupd   12992+__svml_spow_ha_data_internal(%rip), %ymm9 #4690.54
        vmovupd   13056+__svml_spow_ha_data_internal(%rip), %ymm7 #4691.52
        vmovups   __svml_spow_ha_data_internal(%rip), %xmm2     #4681.26
        vmovups   192+__svml_spow_ha_data_internal(%rip), %xmm14 #4684.25
        vextractf128 $1, %ymm1, %xmm11                          #4678.105
        vmovups   %ymm1, 32(%rsp)                               #4604.1[spill]
        vpand     %xmm14, %xmm11, %xmm8                         #4687.90
        vmovdqu   %xmm8, 64(%rsp)                               #4687.90[spill]
        vcvtps2pd %xmm11, %ymm3                                 #4680.85
        vextractf128 $1, %ymm0, %xmm5                           #4677.105
        vcvtps2pd %xmm0, %ymm12                                 #4679.19
        vpsubd    %xmm2, %xmm0, %xmm0                           #4685.23
        vcvtps2pd %xmm5, %ymm13                                 #4679.85
        vandpd    %ymm9, %ymm13, %ymm6                          #4692.77
        vandpd    %ymm9, %ymm12, %ymm10                         #4692.21
        vorpd     %ymm7, %ymm6, %ymm6                           #4693.80
        vmovaps   %xmm1, %xmm4                                  #4678.37
        vpsubd    %xmm2, %xmm5, %xmm1                           #4685.91
        vorpd     %ymm7, %ymm10, %ymm5                          #4693.21
        vpand     %xmm14, %xmm4, %xmm2                          #4687.23
        vcvtpd2ps %ymm6, %xmm14                                 #4696.121
        vcvtps2pd %xmm4, %ymm15                                 #4680.19
        vcvtpd2ps %ymm5, %xmm4                                  #4696.46
        vrcpps    %xmm14, %xmm8                                 #4696.110
        vrcpps    %xmm4, %xmm11                                 #4696.35
        vcvtps2pd %xmm8, %ymm10                                 #4696.94
        vcvtps2pd %xmm11, %ymm7                                 #4696.19
        vpsrlq    $20, %xmm12, %xmm11                           #4698.24
        vextractf128 $1, %ymm12, %xmm4                          #4697.116
        vpsrlq    $20, %xmm4, %xmm14                            #4698.86
        vshufps   $221, %xmm14, %xmm11, %xmm11                  #4699.41
        vroundpd  $0, %ymm7, %ymm14                             #4700.19
        vmovupd   13312+__svml_spow_ha_data_internal(%rip), %ymm7 #4702.50
        vcvtdq2pd %xmm11, %ymm11                                #4701.24
        vmulpd    %ymm14, %ymm5, %ymm5                          #4703.30
        vpsrlq    $20, %xmm13, %xmm8                            #4698.148
        vextractf128 $1, %ymm13, %xmm9                          #4697.266
        vroundpd  $0, %ymm10, %ymm13                            #4700.91
        vsubpd    %ymm7, %ymm5, %ymm10                          #4703.15
        vpsrlq    $20, %xmm9, %xmm9                             #4698.210
        vshufps   $221, %xmm9, %xmm8, %xmm12                    #4699.191
        vmulpd    %ymm13, %ymm6, %ymm4                          #4703.112
        vcvtdq2pd %xmm12, %ymm12                                #4701.82
        vsubpd    %ymm7, %ymm4, %ymm9                           #4703.97
        vpsrlq    $40, %xmm14, %xmm6                            #4705.24
        vextractf128 $1, %ymm14, %xmm8                          #4704.120
        vmovd     %xmm6, %eax                                   #4706.176
        vpsrlq    $40, %xmm8, %xmm7                             #4705.87
        vmovd     %xmm7, %esi                                   #4706.478
        movslq    %eax, %rax                                    #4706.78
        vpextrd   $2, %xmm6, %ecx                               #4706.316
        movslq    %esi, %rsi                                    #4706.380
        vpextrd   $2, %xmm7, %edi                               #4706.618
        movslq    %ecx, %rcx                                    #4706.218
        movslq    %edi, %rdi                                    #4706.520
        vmovsd    -4232448(%rdx,%rax), %xmm8                    #4706.78
        vmovsd    -4232448(%rdx,%rsi), %xmm6                    #4706.380
        vmovhpd   -4232448(%rdx,%rcx), %xmm8, %xmm8             #4706.61
        vmovhpd   -4232448(%rdx,%rdi), %xmm6, %xmm6             #4706.363
        vpsrlq    $40, %xmm13, %xmm5                            #4705.150
        vextractf128 $1, %ymm13, %xmm4                          #4704.278
        vmovd     %xmm5, %r8d                                   #4706.839
        vpsrlq    $40, %xmm4, %xmm4                             #4705.213
        vmovd     %xmm4, %r10d                                  #4706.1141
        movslq    %r10d, %r10                                   #4706.1043
        vpextrd   $2, %xmm4, %r11d                              #4706.1281
        movslq    %r11d, %r11                                   #4706.1183
        movslq    %r8d, %r8                                     #4706.741
        vpextrd   $2, %xmm5, %r9d                               #4706.979
        movslq    %r9d, %r9                                     #4706.881
        vinsertf128 $1, %xmm6, %ymm8, %ymm8                     #4706.15
        vmovsd    -4232448(%rdx,%r10), %xmm6                    #4706.1043
        vmovhpd   -4232448(%rdx,%r11), %xmm6, %xmm4             #4706.1026
        vmovupd   13504+__svml_spow_ha_data_internal(%rip), %ymm6 #4707.56
        vmovsd    -4232448(%rdx,%r8), %xmm7                     #4706.741
        vmovhpd   -4232448(%rdx,%r9), %xmm7, %xmm5              #4706.724
        vmovupd   13568+__svml_spow_ha_data_internal(%rip), %ymm7 #4708.51
        vcmplt_oqpd %ymm14, %ymm6, %ymm14                       #4711.18
        vcmplt_oqpd %ymm13, %ymm6, %ymm6                        #4711.89
        vandpd    %ymm7, %ymm14, %ymm13                         #4712.18
        vandpd    %ymm7, %ymm6, %ymm14                          #4712.71
        vmovupd   13632+__svml_spow_ha_data_internal(%rip), %ymm6 #4712.125
        vorpd     %ymm6, %ymm13, %ymm13                         #4712.125
        vorpd     %ymm6, %ymm14, %ymm14                         #4712.178
        vsubpd    %ymm13, %ymm11, %ymm11                        #4713.20
        vsubpd    %ymm14, %ymm12, %ymm12                        #4713.78
        vmovupd   12928+__svml_spow_ha_data_internal(%rip), %ymm14 #4716.60
        vinsertf128 $1, %xmm4, %ymm5, %ymm5                     #4706.678
        vmovupd   13696+__svml_spow_ha_data_internal(%rip), %ymm4 #4710.49
        vmulpd    %ymm11, %ymm4, %ymm7                          #4714.15
        vmulpd    %ymm12, %ymm4, %ymm6                          #4714.65
        vmovupd   12864+__svml_spow_ha_data_internal(%rip), %ymm4 #4715.60
        vmulpd    %ymm10, %ymm4, %ymm13                         #4717.31
        vmulpd    %ymm9, %ymm4, %ymm12                          #4717.125
        vaddpd    %ymm13, %ymm14, %ymm11                        #4717.16
        vaddpd    %ymm12, %ymm14, %ymm4                         #4717.110
        vmulpd    %ymm10, %ymm10, %ymm13                        #4718.15
        vmulpd    %ymm9, %ymm9, %ymm14                          #4718.60
        vmulpd    %ymm13, %ymm11, %ymm11                        #4719.15
        vmulpd    %ymm14, %ymm4, %ymm13                         #4719.61
        vaddpd    %ymm11, %ymm10, %ymm10                        #4720.15
        vaddpd    %ymm13, %ymm9, %ymm13                         #4720.60
        vaddpd    %ymm10, %ymm8, %ymm4                          #4721.15
        vaddpd    %ymm13, %ymm5, %ymm9                          #4721.60
        vaddpd    %ymm4, %ymm7, %ymm13                          #4722.15
        vaddpd    %ymm9, %ymm6, %ymm4                           #4722.60
        vmovupd   30464+__svml_spow_ha_data_internal(%rip), %ymm9 #4729.56
        vmulpd    %ymm13, %ymm15, %ymm14                        #4723.15
        vmulpd    %ymm4, %ymm3, %ymm13                          #4723.60
        vmovupd   30400+__svml_spow_ha_data_internal(%rip), %ymm3 #4724.57
        vmovupd   30528+__svml_spow_ha_data_internal(%rip), %ymm15 #4726.53
        vmulpd    %ymm14, %ymm3, %ymm6                          #4725.17
        vmulpd    %ymm13, %ymm3, %ymm3                          #4725.72
        vsubpd    %ymm15, %ymm6, %ymm11                         #4727.15
        vsubpd    %ymm15, %ymm3, %ymm8                          #4727.66
        vaddpd    %ymm11, %ymm9, %ymm11                         #4730.15
        vaddpd    %ymm8, %ymm9, %ymm4                           #4730.67
        vmovups   30720+__svml_spow_ha_data_internal(%rip), %xmm8 #4733.25
        vsubpd    %ymm9, %ymm11, %ymm7                          #4731.15
        vsubpd    %ymm9, %ymm4, %ymm5                           #4731.67
        vmovups   30784+__svml_spow_ha_data_internal(%rip), %xmm9 #4735.29
        vsubpd    %ymm7, %ymm6, %ymm6                           #4742.15
        vsubpd    %ymm5, %ymm3, %ymm5                           #4742.62
        vmovupd   30592+__svml_spow_ha_data_internal(%rip), %ymm3 #4743.51
        vextractf128 $1, %ymm13, %xmm12                         #4728.254
        vshufps   $221, %xmm12, %xmm13, %xmm10                  #4732.183
        vmovups   64+__svml_spow_ha_data_internal(%rip), %xmm12 #4686.37
        vandps    %xmm8, %xmm10, %xmm10                         #4734.91
        vextractf128 $1, %ymm14, %xmm15                         #4728.110
        vshufps   $221, %xmm15, %xmm14, %xmm15                  #4732.40
        vpcmpgtd  %xmm12, %xmm0, %xmm14                         #4686.37
        vpcmpeqd  %xmm12, %xmm0, %xmm0                          #4686.92
        vandps    %xmm8, %xmm15, %xmm15                         #4734.22
        vpor      %xmm0, %xmm14, %xmm14                         #4686.23
        vmovups   128+__svml_spow_ha_data_internal(%rip), %xmm0 #4688.37
        vpcmpgtd  %xmm0, %xmm2, %xmm13                          #4688.37
        vpcmpeqd  %xmm0, %xmm2, %xmm2                           #4688.88
        vpor      %xmm2, %xmm13, %xmm13                         #4688.23
        vpor      %xmm13, %xmm14, %xmm2                         #4689.27
        vpcmpgtd  %xmm9, %xmm15, %xmm13                         #4736.29
        vpcmpgtd  %xmm12, %xmm1, %xmm15                         #4686.180
        vpcmpeqd  %xmm12, %xmm1, %xmm1                          #4686.235
        vmovdqu   64(%rsp), %xmm12                              #4688.172[spill]
        vpor      %xmm13, %xmm2, %xmm14                         #4737.27
        vpcmpgtd  %xmm0, %xmm12, %xmm13                         #4688.172
        vpcmpeqd  %xmm0, %xmm12, %xmm0                          #4688.223
        vpor      %xmm1, %xmm15, %xmm2                          #4686.166
        vpor      %xmm0, %xmm13, %xmm1                          #4688.158
        vpor      %xmm1, %xmm2, %xmm12                          #4689.99
        vpcmpgtd  %xmm9, %xmm10, %xmm13                         #4736.111
        vpor      %xmm13, %xmm12, %xmm0                         #4737.109
        vmovupd   30656+__svml_spow_ha_data_internal(%rip), %xmm15 #4740.28
        vpxor     %xmm1, %xmm1, %xmm1                           #4738.62
        vpackssdw %xmm0, %xmm14, %xmm2                          #4738.79
        vpacksswb %xmm1, %xmm2, %xmm14                          #4738.62
        vmulpd    %ymm6, %ymm3, %ymm13                          #4744.15
        vpmovmskb %xmm14, %eax                                  #4738.43
        vmulpd    %ymm5, %ymm3, %ymm14                          #4744.62
        vandps    %xmm15, %xmm11, %xmm10                        #4741.21
        vextractf128 $1, %ymm11, %xmm1                          #4739.110
        vpsrlq    $11, %xmm11, %xmm11                           #4745.21
        vmovd     %xmm10, %ecx                                  #4746.128
        vpsllq    $52, %xmm11, %xmm11                           #4747.21
        vandps    %xmm15, %xmm1, %xmm9                          #4741.91
        vpsrlq    $11, %xmm1, %xmm1                             #4745.74
        shll      $3, %ecx                                      #4746.1988
        vpsllq    $52, %xmm1, %xmm1                             #4747.81
        vmovd     %xmm9, %edi                                   #4746.246
        vmovq     (%rdx,%rcx), %xmm7                            #4746.1988
        vpextrd   $2, %xmm10, %esi                              #4746.185
        shll      $3, %esi                                      #4746.1956
        shll      $3, %edi                                      #4746.2192
        vpextrd   $2, %xmm9, %r8d                               #4746.303
        vmovhpd   (%rdx,%rsi), %xmm7, %xmm3                     #4746.1956
        shll      $3, %r8d                                      #4746.2160
        vmovq     (%rdx,%rdi), %xmm5                            #4746.2192
        vmovhpd   (%rdx,%r8), %xmm5, %xmm6                      #4746.2160
        vandps    %xmm15, %xmm4, %xmm8                          #4741.161
        vextractf128 $1, %ymm4, %xmm0                           #4739.254
        vpsrlq    $11, %xmm4, %xmm4                             #4745.127
        vmovd     %xmm8, %r9d                                   #4746.364
        vpsllq    $52, %xmm4, %xmm4                             #4747.141
        vandps    %xmm15, %xmm0, %xmm2                          #4741.231
        vpsrlq    $11, %xmm0, %xmm0                             #4745.180
        vmovd     %xmm2, %ecx                                   #4746.483
        shll      $3, %r9d                                      #4746.2455
        vpextrd   $2, %xmm8, %r10d                              #4746.422
        shll      $3, %ecx                                      #4746.2659
        vpextrd   $2, %xmm2, %esi                               #4746.540
        shll      $3, %r10d                                     #4746.2423
        vmovq     (%rdx,%r9), %xmm7                             #4746.2455
        shll      $3, %esi                                      #4746.2627
        vmovq     (%rdx,%rcx), %xmm9                            #4746.2659
        vmovhpd   (%rdx,%r10), %xmm7, %xmm8                     #4746.2423
        vmovhpd   (%rdx,%rsi), %xmm9, %xmm2                     #4746.2627
        vinsertf128 $1, %xmm6, %ymm3, %ymm10                    #4746.1910
        vmulpd    %ymm13, %ymm10, %ymm13                        #4748.32
        vaddpd    %ymm13, %ymm10, %ymm15                        #4748.17
        vinsertf128 $1, %xmm2, %ymm8, %ymm12                    #4746.2377
        vmulpd    %ymm14, %ymm12, %ymm14                        #4748.105
        vaddpd    %ymm14, %ymm12, %ymm2                         #4748.90
        vextractf128 $1, %ymm15, %xmm5                          #4749.112
        vpaddq    %xmm11, %xmm15, %xmm3                         #4750.21
        vpaddq    %xmm1, %xmm5, %xmm6                           #4750.84
        vpsllq    $52, %xmm0, %xmm1                             #4747.201
        vextractf128 $1, %ymm2, %xmm13                          #4749.260
        vpaddq    %xmm4, %xmm2, %xmm2                           #4750.147
        vpaddq    %xmm1, %xmm13, %xmm13                         #4750.210
        vinsertf128 $1, %xmm6, %ymm3, %ymm7                     #4751.15
        vcvtpd2ps %ymm7, %xmm3                                  #4752.60
        vinsertf128 $1, %xmm13, %ymm2, %ymm5                    #4751.148
        vcvtpd2ps %ymm5, %xmm6                                  #4752.94
        vinsertf128 $1, %xmm6, %ymm3, %ymm0                     #4752.14
        testb     %al, %al                                      #4754.52
        jne       ..B4.3        # Prob 5%                       #4754.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B4.2:                         # Preds ..B4.3 ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #4757.12
        popq      %rbp                                          #4757.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #4757.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   (%rsp), %ymm1                                 #4754.239[spill]
        vmovups   32(%rsp), %ymm2                               #4754.313[spill]
        vmovups   %ymm0, 192(%rsp)                              #4754.387
        vmovups   %ymm1, 64(%rsp)                               #4754.239
        vmovups   %ymm2, 128(%rsp)                              #4754.313
        testl     %eax, %eax                                    #4754.491
        je        ..B4.2        # Prob 95%                      #4754.491
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #4754.571
                                # LOE rbx r12 r13 r14 r15 eax edx
..B4.13:                        # Preds ..B4.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B4.7:                         # Preds ..B4.8 ..B4.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #4754.634
        jc        ..B4.10       # Prob 5%                       #4754.634
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #4754.587
        cmpl      $8, %r12d                                     #4754.582
        jl        ..B4.7        # Prob 82%                      #4754.582
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   192(%rsp), %ymm0                              #4754.804
        jmp       ..B4.2        # Prob 100%                     #4754.804
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #4754.663
        lea       128(%rsp,%r12,4), %rsi                        #4754.663
        lea       192(%rsp,%r12,4), %rdx                        #4754.663
..___tag_value___svml_powf8_ha_e9.98:
#       __svml_spow_ha_cout_rare_internal(const float *, float *, float *)
        call      __svml_spow_ha_cout_rare_internal             #4754.663
..___tag_value___svml_powf8_ha_e9.99:
        jmp       ..B4.8        # Prob 100%                     #4754.663
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_powf8_ha_e9,@function
	.size	__svml_powf8_ha_e9,.-__svml_powf8_ha_e9
..LN__svml_powf8_ha_e9.3:
	.data
# -- End  __svml_powf8_ha_e9
	.text
.L_2__routine_start___svml_powf4_ha_l9_4:
# -- Begin  __svml_powf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_powf4_ha_l9
# --- __svml_powf4_ha_l9(__m128, __m128)
__svml_powf4_ha_l9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #4762.1
        .byte     15                                            #4912.654
        .byte     30                                            #4912.654
        .byte     250                                           #4912.654
	.cfi_startproc
..___tag_value___svml_powf4_ha_l9.101:
..L102:
                                                        #4762.1
        pushq     %rbp                                          #4762.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #4762.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #4762.1
        subq      $256, %rsp                                    #4762.1
        vmovaps   %xmm1, %xmm14                                 #4762.1
        vcvtps2pd %xmm0, %ymm2                                  #4837.19
        lea       13952+__svml_spow_ha_data_internal(%rip), %rdx #4864.76
        vcvtps2pd %xmm14, %ymm9                                 #4838.19
        vmovups   64+__svml_spow_ha_data_internal(%rip), %xmm10 #4840.21
        vpsubd    __svml_spow_ha_data_internal(%rip), %xmm0, %xmm13 #4843.19
        vmovups   128+__svml_spow_ha_data_internal(%rip), %xmm11 #4841.17
        vandpd    12992+__svml_spow_ha_data_internal(%rip), %ymm2, %ymm15 #4850.19
        vorpd     13056+__svml_spow_ha_data_internal(%rip), %ymm15, %ymm8 #4851.19
        vcvtpd2ps %ymm8, %xmm3                                  #4854.44
        vpsrlq    $20, %ymm2, %ymm1                             #4856.18
        vpand     192+__svml_spow_ha_data_internal(%rip), %xmm14, %xmm12 #4845.19
        vrcpps    %xmm3, %xmm4                                  #4854.33
        vmovupd   13312+__svml_spow_ha_data_internal(%rip), %ymm3 #4860.48
        vcvtps2pd %xmm4, %ymm6                                  #4854.17
        vroundpd  $0, %ymm6, %ymm7                              #4858.17
        vpsrlq    $40, %ymm7, %ymm4                             #4863.18
        vfmsub213pd %ymm3, %ymm7, %ymm8                         #4861.13
        vextractf128 $1, %ymm1, %xmm5                           #4857.129
        vshufps   $221, %xmm5, %xmm1, %xmm15                    #4857.37
        vcvtdq2pd %xmm15, %ymm6                                 #4859.24
        vextractf128 $1, %ymm4, %xmm5                           #4864.535
        vmovd     %xmm4, %eax                                   #4864.174
        vmovd     %xmm5, %esi                                   #4864.516
        movslq    %eax, %rax                                    #4864.76
        vpextrd   $2, %xmm4, %ecx                               #4864.334
        movslq    %esi, %rsi                                    #4864.418
        vpextrd   $2, %xmm5, %edi                               #4864.681
        movslq    %ecx, %rcx                                    #4864.236
        vmovupd   13504+__svml_spow_ha_data_internal(%rip), %ymm4 #4865.54
        vmovsd    -4232448(%rdx,%rax), %xmm2                    #4864.76
        vmovsd    -4232448(%rdx,%rsi), %xmm15                   #4864.418
        vmovhpd   -4232448(%rdx,%rcx), %xmm2, %xmm1             #4864.59
        movslq    %edi, %rdi                                    #4864.583
        vcmplt_oqpd %ymm7, %ymm4, %ymm7                         #4869.16
        vmovhpd   -4232448(%rdx,%rdi), %xmm15, %xmm3            #4864.401
        vinsertf128 $1, %xmm3, %ymm1, %ymm5                     #4864.13
        vmovupd   12864+__svml_spow_ha_data_internal(%rip), %ymm1 #4873.58
        vandpd    13568+__svml_spow_ha_data_internal(%rip), %ymm7, %ymm3 #4870.16
        vorpd     13632+__svml_spow_ha_data_internal(%rip), %ymm3, %ymm4 #4870.64
        vsubpd    %ymm4, %ymm6, %ymm2                           #4871.18
        vfmadd213pd 12928+__svml_spow_ha_data_internal(%rip), %ymm8, %ymm1 #4875.14
        vmulpd    %ymm8, %ymm8, %ymm6                           #4876.13
        vmulpd    13696+__svml_spow_ha_data_internal(%rip), %ymm2, %ymm15 #4872.13
        vmovupd   30464+__svml_spow_ha_data_internal(%rip), %ymm2 #4887.54
        vmulpd    %ymm6, %ymm1, %ymm7                           #4877.13
        vpcmpgtd  %xmm10, %xmm13, %xmm6                         #4844.33
        vpcmpeqd  %xmm10, %xmm13, %xmm10                        #4844.80
        vpcmpgtd  %xmm11, %xmm12, %xmm13                        #4846.33
        vpcmpeqd  %xmm11, %xmm12, %xmm11                        #4846.76
        vpor      %xmm11, %xmm13, %xmm12                        #4846.19
        vaddpd    %ymm7, %ymm8, %ymm8                           #4878.13
        vaddpd    %ymm8, %ymm5, %ymm3                           #4879.13
        vaddpd    %ymm3, %ymm15, %ymm4                          #4880.13
        vmulpd    %ymm4, %ymm9, %ymm1                           #4881.13
        vmulpd    30400+__svml_spow_ha_data_internal(%rip), %ymm1, %ymm4 #4883.15
        vsubpd    30528+__svml_spow_ha_data_internal(%rip), %ymm4, %ymm9 #4885.13
        vaddpd    %ymm9, %ymm2, %ymm3                           #4888.13
        vandps    30656+__svml_spow_ha_data_internal(%rip), %ymm3, %ymm8 #4899.15
        vsubpd    %ymm2, %ymm3, %ymm2                           #4889.13
        vpsrlq    $11, %ymm3, %ymm3                             #4903.15
        vsubpd    %ymm2, %ymm4, %ymm2                           #4900.13
        vmulpd    30592+__svml_spow_ha_data_internal(%rip), %ymm2, %ymm13 #4902.13
        vextractf128 $1, %ymm1, %xmm5                           #4890.125
        vshufps   $221, %xmm5, %xmm1, %xmm1                     #4890.36
        vpor      %xmm10, %xmm6, %xmm5                          #4844.19
        vpor      %xmm12, %xmm5, %xmm5                          #4847.23
        vandps    30720+__svml_spow_ha_data_internal(%rip), %xmm1, %xmm1 #4892.18
        vpcmpgtd  30784+__svml_spow_ha_data_internal(%rip), %xmm1, %xmm6 #4894.25
        vpsllq    $52, %ymm3, %ymm1                             #4905.15
        vpor      %xmm6, %xmm5, %xmm7                           #4895.23
        vmovmskps %xmm7, %eax                                   #4896.44
        vextractf128 $1, %ymm8, %xmm10                          #4904.318
        vmovd     %xmm8, %r8d                                   #4904.128
        vmovd     %xmm10, %r10d                                 #4904.299
        shll      $3, %r8d                                      #4904.1201
        vpextrd   $2, %xmm8, %r9d                               #4904.212
        shll      $3, %r10d                                     #4904.1401
        vpextrd   $2, %xmm10, %r11d                             #4904.388
        shll      $3, %r9d                                      #4904.1169
        vmovq     (%rdx,%r8), %xmm4                             #4904.1201
        shll      $3, %r11d                                     #4904.1369
        vmovq     (%rdx,%r10), %xmm11                           #4904.1401
        vmovhpd   (%rdx,%r9), %xmm4, %xmm9                      #4904.1169
        vmovhpd   (%rdx,%r11), %xmm11, %xmm12                   #4904.1369
        vinsertf128 $1, %xmm12, %ymm9, %ymm15                   #4904.1123
        vfmadd213pd %ymm15, %ymm13, %ymm15                      #4906.15
        vpaddq    %ymm1, %ymm15, %ymm2                          #4908.15
        vcvtpd2ps %ymm2, %xmm1                                  #4910.14
        testl     %eax, %eax                                    #4912.52
        jne       ..B5.3        # Prob 5%                       #4912.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1 xmm14
..B5.13:                        # Preds ..B5.3 ..B5.1
                                # Execution count [9.50e-01]
        vzeroupper                                              #
                                # LOE rbx r12 r13 r14 r15 xmm1
..B5.2:                         # Preds ..B5.9 ..B5.13
                                # Execution count [1.00e+00]
        movaps    %xmm1, %xmm0                                  #4915.12
        movq      %rbp, %rsp                                    #4915.12
        popq      %rbp                                          #4915.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #4915.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm0, 64(%rsp)                               #4912.236
        vmovups   %xmm14, 128(%rsp)                             #4912.307
        vmovups   %xmm1, 192(%rsp)                              #4912.378
        je        ..B5.13       # Prob 95%                      #4912.482
                                # LOE rbx r12 r13 r14 r15 eax xmm1
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #4912.562
                                # LOE rbx r12 r13 r14 r15 eax edx
..B5.15:                        # Preds ..B5.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #4912.625
        jc        ..B5.10       # Prob 5%                       #4912.625
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #4912.578
        cmpl      $4, %r12d                                     #4912.573
        jl        ..B5.7        # Prob 82%                      #4912.573
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm1                              #4912.792
        jmp       ..B5.2        # Prob 100%                     #4912.792
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm1
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #4912.654
        lea       128(%rsp,%r12,4), %rsi                        #4912.654
        lea       192(%rsp,%r12,4), %rdx                        #4912.654
..___tag_value___svml_powf4_ha_l9.119:
#       __svml_spow_ha_cout_rare_internal(const float *, float *, float *)
        call      __svml_spow_ha_cout_rare_internal             #4912.654
..___tag_value___svml_powf4_ha_l9.120:
        jmp       ..B5.8        # Prob 100%                     #4912.654
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_powf4_ha_l9,@function
	.size	__svml_powf4_ha_l9,.-__svml_powf4_ha_l9
..LN__svml_powf4_ha_l9.4:
	.data
# -- End  __svml_powf4_ha_l9
	.text
.L_2__routine_start___svml_powf4_ha_ex_5:
# -- Begin  __svml_powf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_powf4_ha_ex
# --- __svml_powf4_ha_ex(__m128, __m128)
__svml_powf4_ha_ex:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #4920.1
        .byte     15                                            #5070.654
        .byte     30                                            #5070.654
        .byte     250                                           #5070.654
	.cfi_startproc
..___tag_value___svml_powf4_ha_ex.122:
..L123:
                                                        #4920.1
        pushq     %rbp                                          #4920.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #4920.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #4920.1
        subq      $256, %rsp                                    #4920.1
        movaps    %xmm0, %xmm12                                 #4995.71
        movhlps   %xmm0, %xmm12                                 #4995.71
        movaps    %xmm1, %xmm13                                 #4920.1
        cvtps2pd  %xmm0, %xmm6                                  #4995.19
        cvtps2pd  %xmm12, %xmm7                                 #4995.57
        cvtps2pd  %xmm13, %xmm12                                #4996.19
        movups    12992+__svml_spow_ha_data_internal(%rip), %xmm5 #5006.51
        lea       13952+__svml_spow_ha_data_internal(%rip), %rdx #5022.32
        movaps    %xmm5, %xmm4                                  #5008.21
        andps     %xmm7, %xmm5                                  #5008.74
        movups    13056+__svml_spow_ha_data_internal(%rip), %xmm8 #5007.49
        andps     %xmm6, %xmm4                                  #5008.21
        orps      %xmm8, %xmm4                                  #5009.21
        orps      %xmm8, %xmm5                                  #5009.77
        cvtpd2ps  %xmm4, %xmm14                                 #5012.59
        cvtpd2ps  %xmm5, %xmm3                                  #5012.197
        movlhps   %xmm14, %xmm14                                #5012.43
        psrlq     $20, %xmm6                                    #5014.18
        movlhps   %xmm3, %xmm3                                  #5012.181
        psrlq     $20, %xmm7                                    #5014.68
        rcpps     %xmm14, %xmm15                                #5012.32
        rcpps     %xmm3, %xmm2                                  #5012.170
        cvtps2pd  %xmm15, %xmm1                                 #5012.19
        cvtps2pd  %xmm2, %xmm2                                  #5012.157
        shufps    $221, %xmm7, %xmm6                            #5015.35
        movaps    %xmm13, %xmm11                                #4996.71
        movups    .L_2il0floatpacket.40(%rip), %xmm7            #5016.107
        movaps    %xmm0, %xmm10                                 #5001.17
        addpd     %xmm7, %xmm1                                  #5016.297
        addpd     %xmm7, %xmm2                                  #5016.705
        cvtdq2pd  %xmm6, %xmm8                                  #5017.24
        subpd     %xmm7, %xmm1                                  #5016.349
        subpd     %xmm7, %xmm2                                  #5016.757
        mulpd     %xmm1, %xmm4                                  #5019.27
        mulpd     %xmm2, %xmm5                                  #5019.103
        movups    13312+__svml_spow_ha_data_internal(%rip), %xmm14 #5018.47
        movaps    %xmm1, %xmm15                                 #5021.18
        subpd     %xmm14, %xmm4                                 #5019.15
        subpd     %xmm14, %xmm5                                 #5019.91
        movaps    %xmm2, %xmm14                                 #5021.69
        psrlq     $40, %xmm15                                   #5021.18
        psrlq     $40, %xmm14                                   #5021.69
        movd      %xmm14, %esi                                  #5022.461
        pshufd    $2, %xmm14, %xmm14                            #5022.611
        pshufd    $2, %xmm15, %xmm7                             #5022.280
        movd      %xmm14, %edi                                  #5022.593
        movups    13504+__svml_spow_ha_data_internal(%rip), %xmm14 #5023.53
        pshufd    $238, %xmm6, %xmm6                            #5017.90
        movd      %xmm15, %eax                                  #5022.130
        movaps    %xmm14, %xmm15                                #5027.18
        movd      %xmm7, %ecx                                   #5022.262
        movhlps   %xmm13, %xmm11                                #4996.71
        psubd     __svml_spow_ha_data_internal(%rip), %xmm10    #5001.17
        cmpltpd   %xmm1, %xmm15                                 #5027.18
        cmpltpd   %xmm2, %xmm14                                 #5027.76
        cvtdq2pd  %xmm6, %xmm3                                  #5017.73
        cvtps2pd  %xmm11, %xmm9                                 #4996.57
        movups    13568+__svml_spow_ha_data_internal(%rip), %xmm1 #5028.18
        movups    13632+__svml_spow_ha_data_internal(%rip), %xmm2 #5028.119
        andps     %xmm1, %xmm15                                 #5028.18
        andps     %xmm1, %xmm14                                 #5028.68
        orps      %xmm2, %xmm15                                 #5028.119
        orps      %xmm2, %xmm14                                 #5028.169
        movaps    %xmm4, %xmm1                                  #5034.15
        subpd     %xmm15, %xmm8                                 #5029.20
        subpd     %xmm14, %xmm3                                 #5029.75
        mulpd     %xmm4, %xmm1                                  #5034.15
        movups    13696+__svml_spow_ha_data_internal(%rip), %xmm2 #5030.15
        movups    12864+__svml_spow_ha_data_internal(%rip), %xmm15 #5031.57
        mulpd     %xmm2, %xmm8                                  #5030.15
        mulpd     %xmm3, %xmm2                                  #5030.62
        movaps    %xmm15, %xmm3                                 #5033.28
        mulpd     %xmm4, %xmm3                                  #5033.28
        mulpd     %xmm5, %xmm15                                 #5033.116
        movups    12928+__svml_spow_ha_data_internal(%rip), %xmm14 #5032.57
        addpd     %xmm14, %xmm3                                 #5033.16
        addpd     %xmm15, %xmm14                                #5033.104
        mulpd     %xmm1, %xmm3                                  #5035.15
        movaps    %xmm5, %xmm15                                 #5034.57
        movdqa    %xmm10, %xmm1                                 #5002.31
        mulpd     %xmm5, %xmm15                                 #5034.57
        addpd     %xmm3, %xmm4                                  #5036.15
        mulpd     %xmm15, %xmm14                                #5035.58
        movslq    %eax, %rax                                    #5022.32
        movslq    %esi, %rsi                                    #5022.363
        addpd     %xmm14, %xmm5                                 #5036.57
        movslq    %ecx, %rcx                                    #5022.164
        movslq    %edi, %rdi                                    #5022.495
        movsd     -4232448(%rdx,%rax), %xmm6                    #5022.32
        movsd     -4232448(%rdx,%rsi), %xmm7                    #5022.363
        movhpd    -4232448(%rdx,%rcx), %xmm6                    #5022.15
        movhpd    -4232448(%rdx,%rdi), %xmm7                    #5022.346
        addpd     %xmm4, %xmm6                                  #5037.15
        addpd     %xmm5, %xmm7                                  #5037.57
        addpd     %xmm6, %xmm8                                  #5038.15
        addpd     %xmm7, %xmm2                                  #5038.57
        mulpd     %xmm8, %xmm12                                 #5039.15
        mulpd     %xmm2, %xmm9                                  #5039.57
        movups    30400+__svml_spow_ha_data_internal(%rip), %xmm4 #5040.54
        movaps    %xmm4, %xmm6                                  #5041.17
        mulpd     %xmm12, %xmm6                                 #5041.17
        mulpd     %xmm9, %xmm4                                  #5041.69
        movups    30528+__svml_spow_ha_data_internal(%rip), %xmm3 #5042.50
        movaps    %xmm6, %xmm5                                  #5043.15
        movaps    %xmm4, %xmm8                                  #5043.63
        subpd     %xmm3, %xmm5                                  #5043.15
        subpd     %xmm3, %xmm8                                  #5043.63
        movups    30464+__svml_spow_ha_data_internal(%rip), %xmm7 #5045.53
        addpd     %xmm7, %xmm5                                  #5046.15
        addpd     %xmm7, %xmm8                                  #5046.64
        movaps    %xmm5, %xmm15                                 #5047.15
        movaps    %xmm8, %xmm14                                 #5047.64
        subpd     %xmm7, %xmm15                                 #5047.15
        subpd     %xmm7, %xmm14                                 #5047.64
        subpd     %xmm15, %xmm6                                 #5058.15
        subpd     %xmm14, %xmm4                                 #5058.59
        movups    30656+__svml_spow_ha_data_internal(%rip), %xmm2 #5056.57
        movups    30592+__svml_spow_ha_data_internal(%rip), %xmm14 #5059.48
        movaps    %xmm2, %xmm3                                  #5057.15
        mulpd     %xmm14, %xmm6                                 #5060.15
        mulpd     %xmm4, %xmm14                                 #5060.59
        andps     %xmm5, %xmm3                                  #5057.15
        andps     %xmm8, %xmm2                                  #5057.67
        movd      %xmm3, %r8d                                   #5062.128
        psrlq     $11, %xmm5                                    #5061.15
        movd      %xmm2, %r10d                                  #5062.238
        psrlq     $11, %xmm8                                    #5061.56
        movdqu    192+__svml_spow_ha_data_internal(%rip), %xmm11 #5003.17
        psllq     $52, %xmm5                                    #5063.15
        shufps    $221, %xmm9, %xmm12                           #5048.34
        pand      %xmm13, %xmm11                                #5003.17
        pextrw    $4, %xmm3, %r9d                               #5062.182
        pextrw    $4, %xmm2, %r11d                              #5062.294
        shll      $3, %r8d                                      #5062.1030
        psllq     $52, %xmm8                                    #5063.63
        shll      $3, %r10d                                     #5062.1230
        shll      $3, %r9d                                      #5062.998
        shll      $3, %r11d                                     #5062.1198
        movq      (%rdx,%r8), %xmm4                             #5062.1030
        movq      (%rdx,%r10), %xmm7                            #5062.1230
        movhpd    (%rdx,%r9), %xmm4                             #5062.998
        movhpd    (%rdx,%r11), %xmm7                            #5062.1198
        mulpd     %xmm4, %xmm6                                  #5064.29
        mulpd     %xmm7, %xmm14                                 #5064.96
        addpd     %xmm6, %xmm4                                  #5064.17
        addpd     %xmm14, %xmm7                                 #5064.84
        paddq     %xmm5, %xmm4                                  #5066.15
        paddq     %xmm8, %xmm7                                  #5066.60
        movdqu    64+__svml_spow_ha_data_internal(%rip), %xmm9  #5002.31
        pcmpgtd   %xmm9, %xmm1                                  #5002.31
        pcmpeqd   %xmm9, %xmm10                                 #5002.74
        por       %xmm10, %xmm1                                 #5002.17
        movdqa    %xmm11, %xmm9                                 #5004.31
        movdqu    128+__svml_spow_ha_data_internal(%rip), %xmm10 #5004.31
        pcmpgtd   %xmm10, %xmm9                                 #5004.31
        pcmpeqd   %xmm10, %xmm11                                #5004.70
        pand      30720+__svml_spow_ha_data_internal(%rip), %xmm12 #5050.16
        por       %xmm11, %xmm9                                 #5004.17
        pcmpgtd   30784+__svml_spow_ha_data_internal(%rip), %xmm12 #5052.23
        por       %xmm9, %xmm1                                  #5005.21
        por       %xmm12, %xmm1                                 #5053.21
        movmskps  %xmm1, %eax                                   #5054.44
        cvtpd2ps  %xmm4, %xmm1                                  #5068.30
        cvtpd2ps  %xmm7, %xmm5                                  #5068.59
        movlhps   %xmm5, %xmm1                                  #5068.14
        testl     %eax, %eax                                    #5070.52
        jne       ..B6.3        # Prob 5%                       #5070.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1 xmm13
..B6.2:                         # Preds ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movaps    %xmm1, %xmm0                                  #5073.12
        movq      %rbp, %rsp                                    #5073.12
        popq      %rbp                                          #5073.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #5073.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #5070.236
        movups    %xmm13, 128(%rsp)                             #5070.307
        movups    %xmm1, 192(%rsp)                              #5070.378
                                # LOE rbx r12 r13 r14 r15 eax
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #5070.562
        movq      %r12, 8(%rsp)                                 #5070.562[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #5070.562
        movq      %r13, (%rsp)                                  #5070.562[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #5070.562
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #5070.625
        jc        ..B6.10       # Prob 5%                       #5070.625
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #5070.578
        cmpl      $4, %r12d                                     #5070.573
        jl        ..B6.7        # Prob 82%                      #5070.573
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm1                              #5070.792
        jmp       ..B6.2        # Prob 100%                     #5070.792
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm1
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #5070.654
        lea       128(%rsp,%r12,4), %rsi                        #5070.654
        lea       192(%rsp,%r12,4), %rdx                        #5070.654
..___tag_value___svml_powf4_ha_ex.140:
#       __svml_spow_ha_cout_rare_internal(const float *, float *, float *)
        call      __svml_spow_ha_cout_rare_internal             #5070.654
..___tag_value___svml_powf4_ha_ex.141:
        jmp       ..B6.8        # Prob 100%                     #5070.654
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_powf4_ha_ex,@function
	.size	__svml_powf4_ha_ex,.-__svml_powf4_ha_ex
..LN__svml_powf4_ha_ex.5:
	.data
# -- End  __svml_powf4_ha_ex
	.text
.L_2__routine_start__TestInt_6:
# -- Begin  _TestInt
	.text
# mark_begin;
       .align    16,0x90
# --- _TestInt(float)
_TestInt:
# parameter 1: %xmm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
	.cfi_startproc
..___tag_value__TestInt.143:
..L144:
                                                        #3945.1
        movd      %xmm0, %edx                                   #3946.22
        andl      $2147483647, %edx                             #3946.27
        lea       -1065353216(%rdx), %eax                       #3948.13
        cmpl      $1073741824, %eax                             #3948.13
        jae       ..B7.5        # Prob 28%                      #3948.13
                                # LOE rbx rbp r12 r13 r14 r15 edx
..B7.2:                         # Preds ..B7.1
                                # Execution count [7.20e-01]
        cmpl      $1266679808, %edx                             #3952.13
        jge       ..B7.7        # Prob 19%                      #3952.13
                                # LOE rbx rbp r12 r13 r14 r15 edx
..B7.3:                         # Preds ..B7.2
                                # Execution count [5.81e-01]
        movl      %edx, %ecx                                    #3956.13
        andl      $-8388608, %ecx                               #3956.13
        addl      $8388608, %ecx                                #3956.25
        shrl      $23, %ecx                                     #3956.38
        shll      %cl, %edx                                     #3957.12
        testl     $8388607, %edx                                #3958.14
        jne       ..B7.5        # Prob 28%                      #3958.18
                                # LOE rbx rbp r12 r13 r14 r15 edx
..B7.4:                         # Preds ..B7.3
                                # Execution count [4.19e-01]
        andl      $16777215, %edx                               #3962.14
        movl      $1, %ecx                                      #3964.16
        movl      $2, %eax                                      #3964.16
        cmpl      $8388608, %edx                                #3964.16
        cmove     %ecx, %eax                                    #3964.16
        ret                                                     #3964.16
                                # LOE
..B7.5:                         # Preds ..B7.1 ..B7.3
                                # Execution count [1.63e-01]
        xorl      %eax, %eax                                    #3960.16
        ret                                                     #3960.16
                                # LOE
..B7.7:                         # Preds ..B7.2
                                # Execution count [1.39e-01]: Infreq
        movl      $2, %eax                                      #3954.16
        ret                                                     #3954.16
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	_TestInt,@function
	.size	_TestInt,.-_TestInt
..LN_TestInt.6:
	.data
# -- End  _TestInt
	.text
.L_2__routine_start___svml_spow_ha_cout_rare_internal_7:
# -- Begin  __svml_spow_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_spow_ha_cout_rare_internal
	.globl __svml_spow_ha_cout_rare_internal
# --- __svml_spow_ha_cout_rare_internal(const float *, float *, float *)
__svml_spow_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
# parameter 3: %rdx
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #3969.1
        .byte     15                                            #3996.48
        .byte     30                                            #3996.48
        .byte     250                                           #3996.48
	.cfi_startproc
..___tag_value___svml_spow_ha_cout_rare_internal.146:
..L147:
                                                        #3969.1
        pushq     %r12                                          #3969.1
	.cfi_def_cfa_offset 16
	.cfi_offset 12, -16
        pushq     %r13                                          #3969.1
	.cfi_def_cfa_offset 24
	.cfi_offset 13, -24
        pushq     %r14                                          #3969.1
	.cfi_def_cfa_offset 32
	.cfi_offset 14, -32
        pushq     %r15                                          #3969.1
	.cfi_def_cfa_offset 40
	.cfi_offset 15, -40
        pushq     %rbx                                          #3969.1
	.cfi_def_cfa_offset 48
	.cfi_offset 3, -48
        pushq     %rbp                                          #3969.1
	.cfi_def_cfa_offset 56
	.cfi_offset 6, -56
        subq      $88, %rsp                                     #3969.1
	.cfi_def_cfa_offset 144
        movq      %rdx, %r15                                    #3969.1
        movss     (%rdi), %xmm4                                 #3981.17
        pxor      %xmm1, %xmm1                                  #3983.9
        movss     (%rsi), %xmm3                                 #3982.17
        movaps    %xmm4, %xmm2                                  #3981.9
        movl      $0, 64(%rsp)                                  #3970.14[spill]
        movaps    %xmm3, %xmm0                                  #3982.9
        ucomiss   %xmm1, %xmm4                                  #3983.19
        jp        ..B8.2        # Prob 0%                       #3983.19
        je        ..B8.3        # Prob 23%                      #3983.19
                                # LOE r15 xmm0 xmm2 xmm3 xmm4
..B8.2:                         # Preds ..B8.1
                                # Execution count [7.63e-01]
        movss     %xmm4, 8(%rsp)                                #3981.9
        jmp       ..B8.4        # Prob 100%                     #3981.9
                                # LOE r15 xmm0 xmm2 xmm3 xmm4
..B8.3:                         # Preds ..B8.1
                                # Execution count [2.37e-01]
        movaps    %xmm4, %xmm2                                  #3983.36
        addss     %xmm4, %xmm2                                  #3983.36
        movss     %xmm2, 8(%rsp)                                #3983.22
                                # LOE r15 xmm0 xmm2 xmm3 xmm4
..B8.4:                         # Preds ..B8.3 ..B8.2
                                # Execution count [1.00e+00]
        pxor      %xmm1, %xmm1                                  #3984.19
        ucomiss   %xmm1, %xmm3                                  #3984.19
        jp        ..B8.5        # Prob 0%                       #3984.19
        je        ..B8.6        # Prob 23%                      #3984.19
                                # LOE r15 xmm0 xmm2 xmm3 xmm4
..B8.5:                         # Preds ..B8.4
                                # Execution count [7.63e-01]
        movss     %xmm3, 28(%rsp)                               #3982.9
        jmp       ..B8.7        # Prob 100%                     #3982.9
                                # LOE r15 xmm0 xmm2 xmm3 xmm4
..B8.6:                         # Preds ..B8.4
                                # Execution count [2.37e-01]
        movaps    %xmm3, %xmm0                                  #3984.36
        addss     %xmm3, %xmm0                                  #3984.36
        movss     %xmm0, 28(%rsp)                               #3984.22
                                # LOE r15 xmm0 xmm2 xmm3 xmm4
..B8.7:                         # Preds ..B8.6 ..B8.5
                                # Execution count [1.00e+00]
        movb      31(%rsp), %al                                 #3990.39
        andb      $-128, %al                                    #3990.39
        xorl      %ebx, %ebx                                    #3991.45
        shrb      $7, %al                                       #3990.39
        xorl      %ebp, %ebp                                    #3992.45
        testl     $8388607, 8(%rsp)                             #3991.45
        movzwl    30(%rsp), %r13d                               #3986.37
        sete      %bl                                           #3991.9
        andl      $32640, %r13d                                 #3986.37
        testl     $8388607, 28(%rsp)                            #3992.45
        movzwl    10(%rsp), %r14d                               #3985.37
        movzbl    11(%rsp), %r12d                               #3989.39
        sete      %bpl                                          #3992.9
        andl      $32640, %r14d                                 #3985.37
        andl      $128, %r12d                                   #3989.39
        shrl      $7, %r13d                                     #3986.37
        shrl      $7, %r14d                                     #3985.37
        shrl      $7, %r12d                                     #3989.39
        movb      %al, 72(%rsp)                                 #3990.39[spill]
        cmpl      $255, %r13d                                   #3993.69
        je        ..B8.59       # Prob 16%                      #3993.69
                                # LOE r15 ebx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.8:                         # Preds ..B8.7
                                # Execution count [8.40e-01]
        movl      $1, 8(%rsp)                                   #3993.9[spill]
        movss     %xmm2, 32(%rsp)                               #3995.19[spill]
        movss     %xmm0, 40(%rsp)                               #3995.19[spill]
        movss     %xmm3, 48(%rsp)                               #3995.19[spill]
        movss     %xmm4, 56(%rsp)                               #3995.19[spill]
..___tag_value___svml_spow_ha_cout_rare_internal.167:
#       _TestInt(float)
        call      _TestInt                                      #3995.19
..___tag_value___svml_spow_ha_cout_rare_internal.168:
                                # LOE r15 eax ebx ebp r12d r13d r14d
..B8.64:                        # Preds ..B8.8
                                # Execution count [8.40e-01]
        movss     56(%rsp), %xmm4                               #[spill]
        movl      %eax, %edx                                    #3995.19
        movss     48(%rsp), %xmm3                               #[spill]
        movss     40(%rsp), %xmm0                               #[spill]
        movss     32(%rsp), %xmm2                               #[spill]
        testl     %r12d, %r12d                                  #3996.26
        jne       ..B8.10       # Prob 50%                      #3996.26
                                # LOE r15 edx ebx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.9:                         # Preds ..B8.64
                                # Execution count [4.20e-01]
        cmpl      $127, %r14d                                   #3996.39
        je        ..B8.56       # Prob 16%                      #3996.39
                                # LOE r15 edx ebx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.10:                        # Preds ..B8.64 ..B8.9
                                # Execution count [7.73e-01]
        testl     %r13d, %r13d                                  #3997.19
        jne       ..B8.12       # Prob 50%                      #3997.19
                                # LOE r15 edx ebx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.11:                        # Preds ..B8.57 ..B8.10
                                # Execution count [4.60e-01]
        testl     %ebp, %ebp                                    #3997.25
        jne       ..B8.38       # Prob 50%                      #3997.25
                                # LOE r15 edx ebx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.12:                        # Preds ..B8.60 ..B8.65 ..B8.10 ..B8.11
                                # Execution count [7.00e-01]
        cmpl      $255, %r14d                                   #4000.73
        je        ..B8.14       # Prob 16%                      #4000.73
                                # LOE r15 edx ebx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.13:                        # Preds ..B8.61 ..B8.12 ..B8.57
                                # Execution count [6.05e-01]
        movl      $1, %eax                                      #4000.13
        jmp       ..B8.15       # Prob 100%                     #4000.13
                                # LOE r15 eax edx ebx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.14:                        # Preds ..B8.12
                                # Execution count [1.15e-01]
        xorl      %eax, %eax                                    #4000.13
                                # LOE r15 eax edx ebx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.15:                        # Preds ..B8.13 ..B8.14
                                # Execution count [7.20e-01]
        orl       %eax, %ebx                                    #4001.19
        je        ..B8.37       # Prob 50%                      #4001.19
                                # LOE r15 eax edx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.16:                        # Preds ..B8.15
                                # Execution count [3.60e-01]
        orl       8(%rsp), %ebp                                 #4002.19[spill]
        je        ..B8.37       # Prob 50%                      #4002.19
                                # LOE r15 eax edx r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.17:                        # Preds ..B8.16
                                # Execution count [1.80e-01]
        pxor      %xmm1, %xmm1                                  #4005.46
        ucomiss   %xmm1, %xmm2                                  #4005.46
        jp        ..B8.18       # Prob 0%                       #4005.46
        je        ..B8.53       # Prob 16%                      #4005.46
                                # LOE r15 eax edx r12d r13d r14d xmm0 xmm2
..B8.18:                        # Preds ..B8.17
                                # Execution count [1.51e-01]
        ucomiss   .L_2il0floatpacket.143(%rip), %xmm2           #4008.52
        jp        ..B8.19       # Prob 0%                       #4008.52
        je        ..B8.50       # Prob 16%                      #4008.52
                                # LOE r15 eax edx r12d r13d r14d xmm0 xmm2
..B8.19:                        # Preds ..B8.51 ..B8.18
                                # Execution count [1.33e-01]
        testl     %eax, %eax                                    #4011.30
        je        ..B8.30       # Prob 50%                      #4011.30
                                # LOE r15 edx r12d r13d r14d xmm0 xmm2
..B8.20:                        # Preds ..B8.19
                                # Execution count [6.65e-02]
        cmpl      $0, 8(%rsp)                                   #4011.44[spill]
        je        ..B8.30       # Prob 50%                      #4011.44
                                # LOE r15 edx r12d r13d r14d xmm0 xmm2
..B8.21:                        # Preds ..B8.20
                                # Execution count [3.33e-02]
        pxor      %xmm0, %xmm0                                  #4014.56
        comiss    %xmm0, %xmm2                                  #4014.56
        ja        ..B8.23       # Prob 50%                      #4014.56
                                # LOE r15 edx r12d r13d r14d xmm2
..B8.22:                        # Preds ..B8.21
                                # Execution count [1.66e-02]
        testl     %edx, %edx                                    #4014.80
        je        ..B8.29       # Prob 50%                      #4014.80
                                # LOE r15 edx r12d r13d r14d xmm2
..B8.23:                        # Preds ..B8.22 ..B8.21
                                # Execution count [2.49e-02]
        lea       1484+_vmlsPowHATab(%rip), %rax                #4017.61
        andl      %r12d, %edx                                   #4017.90
        movss     %xmm2, 12(%rsp)                               #4019.33
        xorl      %ecx, %ecx                                    #4018.33
        andb      $127, 15(%rsp)                                #4020.54
        movss     (%rax,%rdx,4), %xmm12                         #4017.61
        testl     %r14d, %r14d                                  #4021.46
        jne       ..B8.25       # Prob 50%                      #4021.46
                                # LOE r15 ecx r13d xmm12
..B8.24:                        # Preds ..B8.23
                                # Execution count [1.25e-02]
        movss     12(%rsp), %xmm0                               #4023.44
        movl      $-64, %ecx                                    #4024.37
        mulss     .L_2il0floatpacket.144(%rip), %xmm0           #4023.67
        movss     %xmm0, 12(%rsp)                               #4023.37
        jmp       ..B8.26       # Prob 100%                     #4023.37
                                # LOE r15 ecx r13d xmm0 xmm12
..B8.25:                        # Preds ..B8.23
                                # Execution count [1.25e-02]
        movss     12(%rsp), %xmm0                               #4026.40
                                # LOE r15 ecx r13d xmm0 xmm12
..B8.26:                        # Preds ..B8.24 ..B8.25
                                # Execution count [2.49e-02]
        movzwl    14(%rsp), %edi                                #4028.61
        lea       _vmlsPowHATab(%rip), %rsi                     #4038.58
        andl      $32640, %edi                                  #4028.61
        pxor      %xmm1, %xmm1                                  #4057.46
        shrl      $7, %edi                                      #4028.61
        movl      12(%rsp), %edx                                #4030.68
        shll      $23, %edi                                     #4029.48
        andl      $8388607, %edx                                #4030.68
        movss     %xmm0, 16(%rsp)                               #4026.33
        orl       %edx, %edi                                    #4030.68
        movzwl    18(%rsp), %eax                                #4027.54
        addl      $-1060634624, %edi                            #4031.44
        andl      $-32641, %eax                                 #4027.54
        addl      $16256, %eax                                  #4027.54
        movw      %ax, 18(%rsp)                                 #4027.54
        sarl      $23, %edi                                     #4032.42
        addl      %ecx, %edi                                    #4033.41
        movl      16(%rsp), %ecx                                #4034.59
        andl      $7864320, %ecx                                #4035.43
        addl      $524288, %ecx                                 #4036.43
        cvtsi2ss  %edi, %xmm1                                   #4057.46
        shrl      $20, %ecx                                     #4037.44
        movss     (%rsi,%rcx,4), %xmm4                          #4038.58
        addl      %ecx, %ecx                                    #4039.77
        movss     36(%rsi,%rcx,4), %xmm13                       #4039.58
        movaps    %xmm13, %xmm7                                 #4058.60
        movss     16(%rsp), %xmm0                               #4041.40
        addss     %xmm1, %xmm7                                  #4058.60
        movaps    %xmm0, %xmm6                                  #4041.45
        mulss     %xmm4, %xmm6                                  #4041.45
        movss     %xmm7, 20(%rsp)                               #4058.33
        movss     20(%rsp), %xmm3                               #4058.98
        movd      %xmm6, %ebx                                   #4042.59
        subss     %xmm3, %xmm1                                  #4058.98
        andl      $1966080, %ebx                                #4043.43
        addss     %xmm1, %xmm13                                 #4058.137
        addl      $131072, %ebx                                 #4044.43
        shrl      $18, %ebx                                     #4045.44
        movss     108(%rsi,%rbx,4), %xmm11                      #4046.58
        addl      %ebx, %ebx                                    #4047.78
        movss     144(%rsi,%rbx,4), %xmm5                       #4047.58
        movss     %xmm13, 24(%rsp)                              #4058.109
        movaps    %xmm5, %xmm14                                 #4059.60
        movss     20(%rsp), %xmm1                               #4058.155
        addss     %xmm1, %xmm14                                 #4059.60
        mulss     %xmm11, %xmm6                                 #4049.54
        mulss     %xmm11, %xmm4                                 #4068.60
        movd      %xmm6, %ebp                                   #4050.59
        movss     24(%rsp), %xmm7                               #4058.175
        movss     %xmm14, 20(%rsp)                              #4059.33
        movss     20(%rsp), %xmm3                               #4059.98
        andl      $507904, %ebp                                 #4051.43
        addl      $16384, %ebp                                  #4052.43
        subss     %xmm3, %xmm1                                  #4059.98
        shrl      $15, %ebp                                     #4053.44
        addss     %xmm1, %xmm5                                  #4059.137
        movss     216(%rsi,%rbp,4), %xmm15                      #4054.58
        addl      %ebp, %ebp                                    #4055.78
        movss     284(%rsi,%rbp,4), %xmm2                       #4055.58
        movss     %xmm5, 24(%rsp)                               #4059.109
        movaps    %xmm2, %xmm13                                 #4060.60
        movss     20(%rsp), %xmm1                               #4059.155
        movss     24(%rsp), %xmm5                               #4059.175
        addss     %xmm1, %xmm13                                 #4060.60
        mulss     %xmm15, %xmm4                                 #4069.60
        movss     %xmm13, 20(%rsp)                              #4060.33
        movss     20(%rsp), %xmm3                               #4060.98
        movss     .L_2il0floatpacket.146(%rip), %xmm11          #4070.78
        subss     %xmm3, %xmm1                                  #4060.98
        addss     %xmm1, %xmm2                                  #4060.137
        mulss     %xmm15, %xmm6                                 #4066.56
        movaps    %xmm11, %xmm15                                #4070.78
        movaps    %xmm6, %xmm3                                  #4067.72
        mulss     %xmm0, %xmm15                                 #4070.78
        subss     .L_2il0floatpacket.145(%rip), %xmm3           #4067.72
        movss     %xmm2, 24(%rsp)                               #4060.109
        movss     20(%rsp), %xmm1                               #4060.155
        movss     24(%rsp), %xmm2                               #4060.175
        movss     %xmm15, 20(%rsp)                              #4070.33
        movss     20(%rsp), %xmm13                              #4070.121
        movss     40(%rsi,%rcx,4), %xmm9                        #4040.58
        movss     148(%rsi,%rbx,4), %xmm8                       #4048.58
        movss     288(%rsi,%rbp,4), %xmm10                      #4056.58
        subss     16(%rsp), %xmm13                              #4070.131
        movss     %xmm13, 24(%rsp)                              #4070.102
        movss     20(%rsp), %xmm13                              #4070.160
        movss     24(%rsp), %xmm14                              #4070.170
        subss     %xmm14, %xmm13                                #4070.170
        movss     %xmm13, 20(%rsp)                              #4070.141
        movss     20(%rsp), %xmm15                              #4070.209
        subss     %xmm15, %xmm0                                 #4070.209
        movss     %xmm0, 24(%rsp)                               #4070.181
        movaps    %xmm4, %xmm0                                  #4071.80
        mulss     %xmm11, %xmm0                                 #4071.80
        movss     20(%rsp), %xmm13                              #4070.229
        movss     24(%rsp), %xmm14                              #4070.247
        movss     %xmm0, 20(%rsp)                               #4071.33
        movss     20(%rsp), %xmm15                              #4071.123
        subss     %xmm4, %xmm15                                 #4071.133
        movss     %xmm15, 24(%rsp)                              #4071.104
        movss     20(%rsp), %xmm15                              #4071.164
        movss     24(%rsp), %xmm0                               #4071.174
        subss     %xmm0, %xmm15                                 #4071.174
        movss     %xmm15, 20(%rsp)                              #4071.145
        movss     20(%rsp), %xmm0                               #4071.215
        subss     %xmm0, %xmm4                                  #4071.215
        movaps    %xmm13, %xmm0                                 #4072.60
        movss     %xmm4, 24(%rsp)                               #4071.185
        movss     20(%rsp), %xmm4                               #4071.237
        mulss     %xmm4, %xmm0                                  #4072.60
        mulss     %xmm14, %xmm4                                 #4074.60
        subss     %xmm6, %xmm0                                  #4073.60
        movaps    %xmm3, %xmm6                                  #4080.60
        addss     %xmm4, %xmm0                                  #4075.57
        addss     %xmm1, %xmm6                                  #4080.60
        movss     24(%rsp), %xmm15                              #4071.257
        movss     %xmm6, 20(%rsp)                               #4080.33
        movss     20(%rsp), %xmm4                               #4080.96
        mulss     %xmm15, %xmm13                                #4076.60
        subss     %xmm4, %xmm1                                  #4080.96
        mulss     %xmm15, %xmm14                                #4078.60
        addss     %xmm13, %xmm0                                 #4077.57
        addss     %xmm3, %xmm1                                  #4080.135
        addss     %xmm14, %xmm0                                 #4079.57
        movss     %xmm1, 24(%rsp)                               #4080.107
        movss     20(%rsp), %xmm6                               #4080.156
        movss     %xmm6, 8(%rsp)                                #4080.145
        movzwl    10(%rsp), %eax                                #4081.64
        andl      $32640, %eax                                  #4081.64
        shrl      $7, %eax                                      #4081.64
        addl      %r13d, %eax                                   #4082.48
        movss     24(%rsp), %xmm4                               #4080.174
        cmpl      $265, %eax                                    #4082.55
        jge       ..B8.49       # Prob 0%                       #4082.55
                                # LOE r15 eax xmm0 xmm2 xmm3 xmm4 xmm5 xmm6 xmm7 xmm8 xmm9 xmm10 xmm11 xmm12
..B8.27:                        # Preds ..B8.26
                                # Execution count [2.49e-02]
        cmpl      $192, %eax                                    #4085.59
        jg        ..B8.40       # Prob 0%                       #4085.59
                                # LOE r15 xmm0 xmm2 xmm3 xmm4 xmm5 xmm6 xmm7 xmm8 xmm9 xmm10 xmm11 xmm12
..B8.28:                        # Preds ..B8.27
                                # Execution count [2.49e-02]
        movl      $1065353216, 20(%rsp)                         #4185.41
        movss     20(%rsp), %xmm0                               #4186.60
        addss     .L_2il0floatpacket.155(%rip), %xmm0           #4186.86
        movss     %xmm0, 20(%rsp)                               #4186.41
        movss     20(%rsp), %xmm1                               #4187.55
        mulss     %xmm12, %xmm1                                 #4187.65
        movss     %xmm1, (%r15)                                 #4187.42
        jmp       ..B8.39       # Prob 100%                     #4187.42
                                # LOE
..B8.29:                        # Preds ..B8.22
                                # Execution count [8.32e-03]
        movl      $1, 64(%rsp)                                  #4206.33[spill]
        pxor      %xmm0, %xmm0                                  #4203.33
        movss     %xmm0, 20(%rsp)                               #4203.33
        movss     20(%rsp), %xmm2                               #4204.52
        movss     20(%rsp), %xmm1                               #4204.62
        divss     %xmm1, %xmm2                                  #4204.62
        movss     %xmm2, 20(%rsp)                               #4204.33
        movl      20(%rsp), %eax                                #4205.38
        movl      %eax, (%r15)                                  #4205.34
        jmp       ..B8.39       # Prob 100%                     #4205.34
                                # LOE
..B8.30:                        # Preds ..B8.20 ..B8.19
                                # Execution count [9.98e-02]
        cmpl      $127, %r14d                                   #4212.41
        jge       ..B8.34       # Prob 50%                      #4212.41
                                # LOE r15 edx r12d xmm0 xmm2
..B8.31:                        # Preds ..B8.30
                                # Execution count [4.99e-02]
        movb      72(%rsp), %al                                 #4215.38[spill]
        testb     %al, %al                                      #4215.38
        je        ..B8.33       # Prob 50%                      #4215.38
                                # LOE r15 xmm0
..B8.32:                        # Preds ..B8.31
                                # Execution count [2.49e-02]
        mulss     %xmm0, %xmm0                                  #4218.58
        movss     %xmm0, (%r15)                                 #4218.38
        jmp       ..B8.39       # Prob 100%                     #4218.38
                                # LOE
..B8.33:                        # Preds ..B8.31
                                # Execution count [2.49e-02]
        pxor      %xmm0, %xmm0                                  #4223.38
        movss     %xmm0, (%r15)                                 #4223.38
        jmp       ..B8.39       # Prob 100%                     #4223.38
                                # LOE
..B8.34:                        # Preds ..B8.30
                                # Execution count [4.99e-02]
        movb      72(%rsp), %al                                 #4229.38[spill]
        testb     %al, %al                                      #4229.38
        je        ..B8.36       # Prob 50%                      #4229.38
                                # LOE r15 edx r12d xmm0 xmm2
..B8.35:                        # Preds ..B8.34
                                # Execution count [2.49e-02]
        lea       1512+_vmlsPowHATab(%rip), %rax                #4232.61
        andl      %r12d, %edx                                   #4232.91
        movl      (%rax,%rdx,4), %ecx                           #4232.61
        movl      %ecx, 12(%rsp)                                #4232.37
        movl      %ecx, (%r15)                                  #4233.38
        jmp       ..B8.39       # Prob 100%                     #4233.38
                                # LOE
..B8.36:                        # Preds ..B8.34
                                # Execution count [2.49e-02]
        mulss     %xmm2, %xmm2                                  #4238.62
        lea       1484+_vmlsPowHATab(%rip), %rax                #4240.67
        mulss     %xmm0, %xmm2                                  #4239.64
        andl      %r12d, %edx                                   #4240.97
        mulss     (%rax,%rdx,4), %xmm2                          #4240.67
        movss     %xmm2, (%r15)                                 #4240.38
        jmp       ..B8.39       # Prob 100%                     #4240.38
                                # LOE
..B8.37:                        # Preds ..B8.16 ..B8.15
                                # Execution count [5.40e-01]
        addss     %xmm3, %xmm4                                  #4271.28
        movss     %xmm4, (%r15)                                 #4271.18
        jmp       ..B8.39       # Prob 100%                     #4271.18
                                # LOE
..B8.38:                        # Preds ..B8.61 ..B8.56 ..B8.11
                                # Execution count [2.80e-01]
        addss     %xmm0, %xmm2                                  #4277.30
        movss     %xmm2, 20(%rsp)                               #4277.13
        movl      $1065353216, 24(%rsp)                         #4279.13
        movb      23(%rsp), %al                                 #4278.42
        movb      27(%rsp), %dl                                 #4280.34
        andb      $-128, %al                                    #4280.34
        andb      $127, %dl                                     #4280.34
        orb       %al, %dl                                      #4280.34
        movb      %dl, 27(%rsp)                                 #4280.34
        movss     24(%rsp), %xmm1                               #4281.18
        movss     24(%rsp), %xmm0                               #4281.28
        mulss     %xmm0, %xmm1                                  #4281.28
        movss     %xmm1, (%r15)                                 #4281.14
                                # LOE
..B8.39:                        # Preds ..B8.42 ..B8.44 ..B8.46 ..B8.47 ..B8.48
                                #       ..B8.28 ..B8.49 ..B8.29 ..B8.32 ..B8.33
                                #       ..B8.35 ..B8.36 ..B8.52 ..B8.54 ..B8.55
                                #       ..B8.37 ..B8.38
                                # Execution count [1.00e+00]
        movl      64(%rsp), %eax                                #4283.12[spill]
        addq      $88, %rsp                                     #4283.12
	.cfi_def_cfa_offset 56
	.cfi_restore 6
        popq      %rbp                                          #4283.12
	.cfi_def_cfa_offset 48
	.cfi_restore 3
        popq      %rbx                                          #4283.12
	.cfi_def_cfa_offset 40
	.cfi_restore 15
        popq      %r15                                          #4283.12
	.cfi_def_cfa_offset 32
	.cfi_restore 14
        popq      %r14                                          #4283.12
	.cfi_def_cfa_offset 24
	.cfi_restore 13
        popq      %r13                                          #4283.12
	.cfi_def_cfa_offset 16
	.cfi_restore 12
        popq      %r12                                          #4283.12
	.cfi_def_cfa_offset 8
        ret                                                     #4283.12
	.cfi_def_cfa_offset 144
	.cfi_offset 3, -48
	.cfi_offset 6, -56
	.cfi_offset 12, -16
	.cfi_offset 13, -24
	.cfi_offset 14, -32
	.cfi_offset 15, -40
                                # LOE
..B8.40:                        # Preds ..B8.27
                                # Execution count [0.00e+00]: Infreq
        movss     .L_2il0floatpacket.150(%rip), %xmm1           #4089.106
        lea       424+_vmlsPowHATab(%rip), %rdx                 #4109.93
        addss     %xmm0, %xmm3                                  #4088.63
        addss     %xmm5, %xmm7                                  #4061.59
        addss     %xmm4, %xmm0                                  #4090.72
        mulss     %xmm3, %xmm1                                  #4089.128
        addss     %xmm7, %xmm2                                  #4062.54
        addss     .L_2il0floatpacket.149(%rip), %xmm1           #4089.150
        addss     %xmm2, %xmm9                                  #4063.54
        mulss     %xmm3, %xmm1                                  #4089.172
        addss     %xmm9, %xmm8                                  #4064.54
        addss     .L_2il0floatpacket.148(%rip), %xmm1           #4089.194
        addss     %xmm8, %xmm10                                 #4065.54
        mulss     %xmm3, %xmm1                                  #4089.216
        addss     %xmm0, %xmm10                                 #4091.69
        addss     .L_2il0floatpacket.147(%rip), %xmm1           #4089.238
        mulss     %xmm3, %xmm1                                  #4089.260
        movaps    %xmm10, %xmm5                                 #4093.69
        movss     8(%rsp), %xmm4                                #4092.107
        movaps    %xmm11, %xmm8                                 #4095.86
        addss     %xmm1, %xmm6                                  #4092.73
        lea       20(%rsp), %rax                                #4092.41
        movss     %xmm6, (%rax)                                 #4092.41
        movss     (%rax), %xmm0                                 #4092.120
        subss     %xmm0, %xmm4                                  #4092.120
        movaps    %xmm11, %xmm0                                 #4096.86
        addss     %xmm1, %xmm4                                  #4092.159
        lea       24(%rsp), %rcx                                #4092.131
        movss     %xmm4, (%rcx)                                 #4092.131
        movss     (%rax), %xmm7                                 #4092.182
        movss     (%rcx), %xmm3                                 #4092.198
        addss     %xmm7, %xmm5                                  #4093.69
        movss     %xmm5, (%rax)                                 #4093.41
        movss     (%rax), %xmm6                                 #4093.112
        subss     %xmm6, %xmm7                                  #4093.112
        addss     %xmm10, %xmm7                                 #4093.151
        movss     %xmm7, (%rcx)                                 #4093.123
        movss     (%rax), %xmm10                                #4093.174
        mulss     %xmm10, %xmm8                                 #4095.86
        movss     (%rcx), %xmm2                                 #4093.191
        movss     %xmm8, (%rax)                                 #4095.41
        addss     %xmm3, %xmm2                                  #4094.66
        movss     (%rax), %xmm9                                 #4095.129
        lea       28(%rsp), %rbx                                #4096.60
        movss     (%rbx), %xmm7                                 #4096.60
        subss     %xmm10, %xmm9                                 #4095.139
        mulss     %xmm7, %xmm0                                  #4096.86
        mulss     (%rbx), %xmm2                                 #4098.65
        movss     %xmm9, (%rcx)                                 #4095.110
        movss     (%rax), %xmm14                                #4095.168
        movss     (%rcx), %xmm13                                #4095.178
        movss     .L_2il0floatpacket.151(%rip), %xmm9           #4099.83
        subss     %xmm13, %xmm14                                #4095.178
        movss     %xmm14, (%rax)                                #4095.149
        movss     (%rax), %xmm15                                #4095.217
        subss     %xmm15, %xmm10                                #4095.217
        movss     %xmm10, (%rcx)                                #4095.189
        movss     (%rax), %xmm8                                 #4095.235
        movss     (%rcx), %xmm10                                #4095.251
        movss     %xmm0, (%rax)                                 #4096.41
        movss     (%rax), %xmm1                                 #4096.129
        subss     (%rbx), %xmm1                                 #4096.139
        movss     %xmm1, (%rcx)                                 #4096.110
        movss     (%rax), %xmm5                                 #4096.168
        movss     (%rcx), %xmm4                                 #4096.178
        subss     %xmm4, %xmm5                                  #4096.178
        movss     %xmm5, (%rax)                                 #4096.149
        movss     (%rax), %xmm6                                 #4096.217
        subss     %xmm6, %xmm7                                  #4096.217
        movss     %xmm7, (%rcx)                                 #4096.189
        movss     (%rax), %xmm1                                 #4096.236
        movss     (%rcx), %xmm15                                #4096.253
        movaps    %xmm1, %xmm0                                  #4097.67
        mulss     %xmm8, %xmm0                                  #4097.67
        mulss     %xmm10, %xmm1                                 #4097.110
        mulss     %xmm15, %xmm8                                 #4097.136
        mulss     %xmm15, %xmm10                                #4097.265
        addss     %xmm8, %xmm1                                  #4097.136
        movaps    %xmm0, %xmm8                                  #4097.173
        addss     %xmm1, %xmm8                                  #4097.173
        movaps    %xmm8, %xmm3                                  #4099.83
        subss     %xmm8, %xmm0                                  #4097.226
        addss     %xmm9, %xmm3                                  #4099.83
        addss     %xmm0, %xmm1                                  #4097.237
        movss     %xmm3, (%rax)                                 #4099.41
        movaps    %xmm8, %xmm3                                  #4105.66
        movss     (%rax), %xmm13                                #4100.59
        addss     %xmm1, %xmm10                                 #4097.265
        subss     %xmm9, %xmm13                                 #4100.85
        addss     %xmm2, %xmm10                                 #4106.64
        movss     %xmm13, (%rsp)                                #4100.41
        movss     (%rsp), %xmm14                                #4105.66
        movss     .L_2il0floatpacket.154(%rip), %xmm2           #4108.97
        subss     %xmm14, %xmm3                                 #4105.66
        movss     %xmm3, 4(%rsp)                                #4105.41
        movss     4(%rsp), %xmm4                                #4107.62
        movl      (%rax), %eax                                  #4101.67
        movl      %eax, %ebx                                    #4103.52
        andl      $127, %eax                                    #4102.50
        addss     %xmm10, %xmm4                                 #4107.62
        mulss     %xmm4, %xmm2                                  #4108.119
        addl      %eax, %eax                                    #4109.114
        movss     -4(%rdx,%rax,4), %xmm1                        #4109.93
        shll      $10, %ebx                                     #4103.52
        addss     .L_2il0floatpacket.153(%rip), %xmm2           #4108.141
        mulss     %xmm4, %xmm2                                  #4108.163
        sarl      $17, %ebx                                     #4104.52
        addss     .L_2il0floatpacket.152(%rip), %xmm2           #4108.185
        mulss     %xmm4, %xmm2                                  #4108.207
        mulss     %xmm1, %xmm2                                  #4109.93
        addss     (%rdx,%rax,4), %xmm2                          #4110.90
        movaps    %xmm2, %xmm5                                  #4112.68
        addss     %xmm1, %xmm5                                  #4112.68
        movss     %xmm5, 12(%rsp)                               #4112.41
        movzwl    14(%rsp), %edx                                #4113.70
        movl      %edx, %ecx                                    #4113.70
        andl      $32640, %ecx                                  #4113.70
        shrl      $7, %ecx                                      #4113.70
        lea       -127(%rbx,%rcx), %eax                         #4115.66
        cmpl      $128, %eax                                    #4116.54
        jge       ..B8.48       # Prob 0%                       #4116.54
                                # LOE r15 eax edx ebx xmm1 xmm2 xmm5 xmm11 xmm12
..B8.41:                        # Preds ..B8.40
                                # Execution count [0.00e+00]: Infreq
        cmpl      $-126, %eax                                   #4119.59
        jl        ..B8.43       # Prob 50%                      #4119.59
                                # LOE r15 eax edx ebx xmm1 xmm2 xmm5 xmm11 xmm12
..B8.42:                        # Preds ..B8.41
                                # Execution count [0.00e+00]: Infreq
        andl      $-32641, %edx                                 #4122.70
        lea       127(%rax), %eax                               #4122.102
        movzbl    %al, %eax                                     #4122.70
        shll      $7, %eax                                      #4122.70
        orl       %eax, %edx                                    #4122.70
        movw      %dx, 14(%rsp)                                 #4122.70
        movss     12(%rsp), %xmm0                               #4123.66
        mulss     %xmm12, %xmm0                                 #4123.74
        movss     %xmm0, 12(%rsp)                               #4123.49
        movss     %xmm0, (%r15)                                 #4124.50
        jmp       ..B8.39       # Prob 100%                     #4124.50
                                # LOE
..B8.43:                        # Preds ..B8.41
                                # Execution count [0.00e+00]: Infreq
        cmpl      $-136, %eax                                   #4128.63
        jl        ..B8.45       # Prob 50%                      #4128.63
                                # LOE r15 eax ebx xmm1 xmm2 xmm5 xmm11 xmm12
..B8.44:                        # Preds ..B8.43
                                # Execution count [0.00e+00]: Infreq
        lea       20(%rsp), %rdx                                #4131.53
        movss     %xmm5, (%rdx)                                 #4131.53
        movl      $1065353216, %eax                             #4135.53
        movss     (%rdx), %xmm0                                 #4131.127
        addl      $191, %ebx                                    #4136.107
        movl      %eax, 8(%rsp)                                 #4135.53
        subss     %xmm0, %xmm1                                  #4131.127
        shrl      $16, %eax                                     #4136.74
        addss     %xmm1, %xmm2                                  #4131.166
        movss     %xmm2, 24(%rsp)                               #4131.138
        movss     (%rdx), %xmm5                                 #4131.189
        mulss     %xmm5, %xmm11                                 #4132.101
        movss     24(%rsp), %xmm6                               #4131.208
        movss     %xmm11, (%rdx)                                #4132.53
        movss     (%rdx), %xmm1                                 #4132.144
        movzwl    %ax, %edx                                     #4136.74
        subss     %xmm5, %xmm1                                  #4132.154
        lea       24(%rsp), %rax                                #4132.125
        movss     %xmm1, (%rax)                                 #4132.125
        andl      $-32641, %edx                                 #4136.74
        lea       20(%rsp), %rcx                                #4132.186
        movss     (%rcx), %xmm3                                 #4132.186
        movss     (%rax), %xmm2                                 #4132.196
        movzbl    %bl, %ebx                                     #4136.74
        subss     %xmm2, %xmm3                                  #4132.196
        movss     %xmm3, (%rcx)                                 #4132.167
        movss     (%rcx), %xmm4                                 #4132.238
        shll      $7, %ebx                                      #4136.74
        subss     %xmm4, %xmm5                                  #4132.238
        movss     %xmm5, (%rax)                                 #4132.207
        orl       %ebx, %edx                                    #4136.74
        movss     (%rcx), %xmm8                                 #4132.259
        movss     (%rax), %xmm14                                #4132.277
        movw      %dx, 10(%rsp)                                 #4136.74
        addss     %xmm6, %xmm14                                 #4133.82
        movss     8(%rsp), %xmm7                                #4138.80
        mulss     %xmm7, %xmm14                                 #4138.80
        mulss     %xmm7, %xmm8                                  #4137.80
        lea       20(%rsp), %rdx                                #4141.53
        movl      $8388608, (%rdx)                              #4141.53
        addss     %xmm8, %xmm14                                 #4139.80
        movss     (%rdx), %xmm10                                #4142.72
        movss     (%rdx), %xmm9                                 #4142.82
        mulss     %xmm9, %xmm10                                 #4142.82
        mulss     .L_2il0floatpacket.157(%rip), %xmm14          #4140.92
        movss     %xmm10, (%rdx)                                #4142.53
        movss     (%rdx), %xmm13                                #4143.78
        addss     %xmm13, %xmm14                                #4143.78
        mulss     %xmm14, %xmm12                                #4144.66
        movss     %xmm14, 12(%rsp)                              #4143.53
        movss     %xmm12, (%r15)                                #4144.54
        jmp       ..B8.39       # Prob 100%                     #4144.54
                                # LOE
..B8.45:                        # Preds ..B8.43
                                # Execution count [0.00e+00]: Infreq
        cmpl      $-159, %eax                                   #4149.67
        jl        ..B8.47       # Prob 50%                      #4149.67
                                # LOE r15 ebx xmm5 xmm12
..B8.46:                        # Preds ..B8.45
                                # Execution count [0.00e+00]: Infreq
        movl      $1065353216, %eax                             #4153.57
        addl      $191, %ebx                                    #4154.111
        movl      %eax, 8(%rsp)                                 #4153.57
        shrl      $16, %eax                                     #4154.78
        movzwl    %ax, %edx                                     #4154.78
        movzbl    %bl, %ebx                                     #4154.78
        andl      $-32641, %edx                                 #4154.78
        shll      $7, %ebx                                      #4154.78
        orl       %ebx, %edx                                    #4154.78
        movw      %dx, 10(%rsp)                                 #4154.78
        movss     8(%rsp), %xmm0                                #4155.80
        movss     .L_2il0floatpacket.157(%rip), %xmm1           #4151.84
        mulss     %xmm0, %xmm5                                  #4155.80
        mulss     %xmm1, %xmm12                                 #4151.57
        lea       20(%rsp), %rdx                                #4157.57
        movl      $8388608, (%rdx)                              #4157.57
        movss     (%rdx), %xmm3                                 #4158.68
        movss     (%rdx), %xmm2                                 #4158.57
        mulss     %xmm2, %xmm3                                  #4158.57
        mulss     %xmm12, %xmm5                                 #4156.80
        movss     %xmm3, (%rdx)                                 #4158.57
        movss     (%rdx), %xmm4                                 #4159.82
        addss     %xmm4, %xmm5                                  #4159.82
        movss     %xmm5, 12(%rsp)                               #4159.57
        movss     %xmm5, (%r15)                                 #4160.58
        jmp       ..B8.39       # Prob 100%                     #4160.58
                                # LOE
..B8.47:                        # Preds ..B8.45
                                # Execution count [0.00e+00]: Infreq
        lea       20(%rsp), %rax                                #4165.57
        movl      $8388608, (%rax)                              #4165.57
        movss     (%rax), %xmm1                                 #4166.68
        movss     (%rax), %xmm0                                 #4166.57
        mulss     %xmm0, %xmm1                                  #4166.57
        movss     %xmm1, (%rax)                                 #4166.57
        movss     (%rax), %xmm2                                 #4167.72
        mulss     %xmm2, %xmm12                                 #4167.80
        movss     %xmm12, 12(%rsp)                              #4167.57
        movss     %xmm12, (%r15)                                #4168.58
        jmp       ..B8.39       # Prob 100%                     #4168.58
                                # LOE
..B8.48:                        # Preds ..B8.40
                                # Execution count [0.00e+00]: Infreq
        lea       20(%rsp), %rax                                #4176.45
        movl      $2130706432, (%rax)                           #4176.45
        movss     (%rax), %xmm1                                 #4177.64
        movss     (%rax), %xmm0                                 #4177.74
        mulss     %xmm0, %xmm1                                  #4177.74
        movss     %xmm1, (%rax)                                 #4177.45
        movss     (%rax), %xmm2                                 #4178.62
        mulss     %xmm2, %xmm12                                 #4178.72
        movss     %xmm12, 12(%rsp)                              #4178.45
        movss     %xmm12, (%r15)                                #4179.46
        jmp       ..B8.39       # Prob 100%                     #4179.46
                                # LOE
..B8.49:                        # Preds ..B8.26
                                # Execution count [0.00e+00]: Infreq
        movb      11(%rsp), %al                                 #4193.75
        lea       1472+_vmlsPowHATab(%rip), %rcx                #4194.62
        andb      $-128, %al                                    #4193.75
        movb      72(%rsp), %dl                                 #4193.75[spill]
        shrb      $7, %al                                       #4193.75
        xorb      %al, %dl                                      #4193.75
        movzbl    %dl, %ebx                                     #4193.37
        movss     (%rcx,%rbx,4), %xmm0                          #4194.62
        mulss     %xmm0, %xmm0                                  #4195.64
        mulss     %xmm12, %xmm0                                 #4196.64
        movss     %xmm0, (%r15)                                 #4197.38
        jmp       ..B8.39       # Prob 100%                     #4197.38
                                # LOE
..B8.50:                        # Preds ..B8.18
                                # Execution count [2.42e-02]: Infreq
        testl     %edx, %edx                                    #4008.77
        jne       ..B8.52       # Prob 50%                      #4008.77
                                # LOE r15 eax edx r12d r13d r14d xmm0 xmm2
..B8.51:                        # Preds ..B8.50
                                # Execution count [1.21e-02]: Infreq
        cmpl      $0, 8(%rsp)                                   #4008.89[spill]
        jne       ..B8.19       # Prob 50%                      #4008.89
                                # LOE r15 eax edx r12d r13d r14d xmm0 xmm2
..B8.52:                        # Preds ..B8.51 ..B8.50
                                # Execution count [1.81e-02]: Infreq
        lea       1484+_vmlsPowHATab(%rip), %rax                #4248.46
        andl      $1, %edx                                      #4248.76
        movl      (%rax,%rdx,4), %ecx                           #4248.46
        movl      %ecx, (%r15)                                  #4248.26
        jmp       ..B8.39       # Prob 100%                     #4248.26
                                # LOE
..B8.53:                        # Preds ..B8.17
                                # Execution count [2.88e-02]: Infreq
        movb      72(%rsp), %al                                 #4255.26[spill]
        mulss     %xmm2, %xmm2                                  #4254.37
        testb     %al, %al                                      #4255.26
        je        ..B8.55       # Prob 50%                      #4255.26
                                # LOE r15 edx r12d xmm2
..B8.54:                        # Preds ..B8.53
                                # Execution count [1.44e-02]: Infreq
        lea       1484+_vmlsPowHATab(%rip), %rax                #4258.46
        andl      %r12d, %edx                                   #4258.76
        movl      $1, 64(%rsp)                                  #4259.25[spill]
        movss     (%rax,%rdx,4), %xmm0                          #4258.46
        divss     %xmm2, %xmm0                                  #4258.87
        movss     %xmm0, (%r15)                                 #4258.26
        jmp       ..B8.39       # Prob 100%                     #4258.26
                                # LOE
..B8.55:                        # Preds ..B8.53
                                # Execution count [1.44e-02]: Infreq
        lea       1484+_vmlsPowHATab(%rip), %rax                #4264.46
        andl      %r12d, %edx                                   #4264.76
        movss     (%rax,%rdx,4), %xmm0                          #4264.46
        mulss     %xmm2, %xmm0                                  #4264.87
        movss     %xmm0, (%r15)                                 #4264.26
        jmp       ..B8.39       # Prob 100%                     #4264.26
                                # LOE
..B8.56:                        # Preds ..B8.9
                                # Execution count [6.72e-02]: Infreq
        testl     %ebx, %ebx                                    #3996.48
        jne       ..B8.38       # Prob 50%                      #3996.48
                                # LOE r15 edx ebx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.57:                        # Preds ..B8.56
                                # Execution count [3.36e-02]: Infreq
        testl     %r13d, %r13d                                  #3997.19
        jne       ..B8.13       # Prob 50%                      #3997.19
        jmp       ..B8.11       # Prob 100%                     #3997.19
                                # LOE r15 edx ebx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.59:                        # Preds ..B8.7
                                # Execution count [1.60e-01]: Infreq
        movl      $0, 8(%rsp)                                   #3993.9[spill]
        movss     %xmm2, 32(%rsp)                               #3995.19[spill]
        movss     %xmm0, 40(%rsp)                               #3995.19[spill]
        movss     %xmm3, 48(%rsp)                               #3995.19[spill]
        movss     %xmm4, 56(%rsp)                               #3995.19[spill]
..___tag_value___svml_spow_ha_cout_rare_internal.208:
#       _TestInt(float)
        call      _TestInt                                      #3995.19
..___tag_value___svml_spow_ha_cout_rare_internal.209:
                                # LOE r15 eax ebx ebp r12d r13d r14d
..B8.65:                        # Preds ..B8.59
                                # Execution count [1.60e-01]: Infreq
        movss     56(%rsp), %xmm4                               #[spill]
        movl      %eax, %edx                                    #3995.19
        movss     48(%rsp), %xmm3                               #[spill]
        movss     40(%rsp), %xmm0                               #[spill]
        movss     32(%rsp), %xmm2                               #[spill]
        testl     %r12d, %r12d                                  #3996.26
        jne       ..B8.12       # Prob 50%                      #3996.26
                                # LOE r15 edx ebx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.60:                        # Preds ..B8.65
                                # Execution count [8.00e-02]: Infreq
        cmpl      $127, %r14d                                   #3996.39
        jne       ..B8.12       # Prob 84%                      #3996.39
                                # LOE r15 edx ebx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
..B8.61:                        # Preds ..B8.60
                                # Execution count [1.28e-02]: Infreq
        testl     %ebx, %ebx                                    #3996.48
        je        ..B8.13       # Prob 50%                      #3996.48
        jmp       ..B8.38       # Prob 100%                     #3996.48
        .align    16,0x90
                                # LOE r15 edx ebx ebp r12d r13d r14d xmm0 xmm2 xmm3 xmm4
	.cfi_endproc
# mark_end;
	.type	__svml_spow_ha_cout_rare_internal,@function
	.size	__svml_spow_ha_cout_rare_internal,.-__svml_spow_ha_cout_rare_internal
..LN__svml_spow_ha_cout_rare_internal.7:
	.data
# -- End  __svml_spow_ha_cout_rare_internal
	.section .rodata, "a"
	.space 32, 0x00 	# pad
	.align 64
	.hidden __svml_spow_ha_data_internal_avx512
	.globl __svml_spow_ha_data_internal_avx512
__svml_spow_ha_data_internal_avx512:
	.long	0
	.long	3174420480
	.long	3182632960
	.long	3187958784
	.long	3190685696
	.long	3193338880
	.long	3195920384
	.long	3197247488
	.long	3198473216
	.long	3199668736
	.long	3200835072
	.long	3201974272
	.long	3203086848
	.long	3204174848
	.long	3204843520
	.long	3205364224
	.long	1054113792
	.long	1053115392
	.long	1052137472
	.long	1051179008
	.long	1050238976
	.long	1049316864
	.long	1048248320
	.long	1046471680
	.long	1044726784
	.long	1043013632
	.long	1041329152
	.long	1039161344
	.long	1035907072
	.long	1032706048
	.long	1027317760
	.long	1018830848
	.long	0
	.long	3067311503
	.long	890262383
	.long	916311190
	.long	3058814943
	.long	914835756
	.long	3056977939
	.long	3052757441
	.long	905348701
	.long	921801496
	.long	900652061
	.long	916473404
	.long	3063873943
	.long	3048020321
	.long	3055557319
	.long	921573027
	.long	3050426335
	.long	918574590
	.long	913737309
	.long	3045697063
	.long	3029223305
	.long	866568163
	.long	3063765991
	.long	3057827840
	.long	910185982
	.long	3062847489
	.long	917965485
	.long	903301016
	.long	882039287
	.long	910858241
	.long	3059117133
	.long	3029061382
	.long	1065353216
	.long	1065536903
	.long	1065724611
	.long	1065916431
	.long	1066112450
	.long	1066312762
	.long	1066517459
	.long	1066726640
	.long	1066940400
	.long	1067158842
	.long	1067382066
	.long	1067610179
	.long	1067843287
	.long	1068081499
	.long	1068324927
	.long	1068573686
	.long	1068827891
	.long	1069087663
	.long	1069353124
	.long	1069624397
	.long	1069901610
	.long	1070184894
	.long	1070474380
	.long	1070770206
	.long	1071072509
	.long	1071381432
	.long	1071697119
	.long	1072019719
	.long	1072349383
	.long	1072686266
	.long	1073030525
	.long	1073382323
	.long	0
	.long	3007986186
	.long	860277610
	.long	3010384254
	.long	2991457809
	.long	3008462297
	.long	860562562
	.long	3004532446
	.long	856238081
	.long	3001480295
	.long	857441778
	.long	815380209
	.long	3003456168
	.long	3001196762
	.long	2986372182
	.long	3006683458
	.long	848495278
	.long	851809756
	.long	3003311522
	.long	2995654817
	.long	833868005
	.long	3004843819
	.long	835836658
	.long	3003498340
	.long	2994528642
	.long	3002229827
	.long	2981408986
	.long	2983889551
	.long	2983366846
	.long	3000350873
	.long	833659207
	.long	2987748092
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
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
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	1049872133
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	3199775725
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	1056323663
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	2981005319
	.long	849703116
	.long	849703116
	.long	849703116
	.long	849703116
	.long	849703116
	.long	849703116
	.long	849703116
	.long	849703116
	.long	849703116
	.long	849703116
	.long	849703116
	.long	849703116
	.long	849703116
	.long	849703116
	.long	849703116
	.long	849703116
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
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	3208161851
	.long	1220546528
	.long	1220546528
	.long	1220546528
	.long	1220546528
	.long	1220546528
	.long	1220546528
	.long	1220546528
	.long	1220546528
	.long	1220546528
	.long	1220546528
	.long	1220546528
	.long	1220546528
	.long	1220546528
	.long	1220546528
	.long	1220546528
	.long	1220546528
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
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1030083214
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1047919318
	.long	1060205081
	.long	1060205081
	.long	1060205081
	.long	1060205081
	.long	1060205081
	.long	1060205081
	.long	1060205081
	.long	1060205081
	.long	1060205081
	.long	1060205081
	.long	1060205081
	.long	1060205081
	.long	1060205081
	.long	1060205081
	.long	1060205081
	.long	1060205081
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
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	1123745792
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.type	__svml_spow_ha_data_internal_avx512,@object
	.size	__svml_spow_ha_data_internal_avx512,1792
	.align 64
	.hidden __svml_spow_ha_data_internal
	.globl __svml_spow_ha_data_internal
__svml_spow_ha_data_internal:
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
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
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	1118738944
	.long	3715793664
	.long	3230016299
	.long	4013928704
	.long	3189565726
	.long	3699038248
	.long	3230016303
	.long	4255595370
	.long	3189567439
	.long	3648859040
	.long	3230016307
	.long	1732833791
	.long	3189567296
	.long	3565385952
	.long	3230016311
	.long	2485752410
	.long	3189567274
	.long	3448748152
	.long	3230016315
	.long	3514744523
	.long	3189567800
	.long	3299074072
	.long	3230016319
	.long	3686511262
	.long	3189567474
	.long	3116491376
	.long	3230016323
	.long	716063611
	.long	3189567908
	.long	2901127016
	.long	3230016327
	.long	354437295
	.long	3189567291
	.long	2653107184
	.long	3230016331
	.long	3777462366
	.long	3189567264
	.long	2372557360
	.long	3230016335
	.long	3590663412
	.long	3189567498
	.long	2059602304
	.long	3230016339
	.long	1680795031
	.long	3189567428
	.long	1714366048
	.long	3230016343
	.long	2010344982
	.long	3189567986
	.long	1336971936
	.long	3230016347
	.long	166254822
	.long	3189567184
	.long	927542568
	.long	3230016351
	.long	59552610
	.long	3189567972
	.long	486199888
	.long	3230016355
	.long	2268824919
	.long	3189567664
	.long	13065112
	.long	3230016359
	.long	3005381643
	.long	3189567835
	.long	3803226080
	.long	3230016362
	.long	4018973165
	.long	3189567825
	.long	3266868056
	.long	3230016366
	.long	3693925101
	.long	3189567457
	.long	2699077504
	.long	3230016370
	.long	4005905743
	.long	3189567697
	.long	2099972936
	.long	3230016374
	.long	3580935118
	.long	3189567215
	.long	1469672176
	.long	3230016378
	.long	2928784295
	.long	3189567129
	.long	808292392
	.long	3230016382
	.long	674298833
	.long	3189567553
	.long	115950104
	.long	3230016386
	.long	2555938320
	.long	3189567208
	.long	3687728456
	.long	3230016389
	.long	532458218
	.long	3189567130
	.long	2933808064
	.long	3230016393
	.long	3888913110
	.long	3189567192
	.long	2149270784
	.long	3230016397
	.long	2365955494
	.long	3189567756
	.long	1334230552
	.long	3230016401
	.long	3325722981
	.long	3189567206
	.long	488800640
	.long	3230016405
	.long	1496133617
	.long	3189567683
	.long	3908061016
	.long	3230016408
	.long	1824836379
	.long	3189567490
	.long	3002189112
	.long	3230016412
	.long	1222174320
	.long	3189567837
	.long	2066263648
	.long	3230016416
	.long	2472769660
	.long	3189567285
	.long	1100395408
	.long	3230016420
	.long	447932460
	.long	3189567455
	.long	104694584
	.long	3230016424
	.long	1006807684
	.long	3189567460
	.long	3374238048
	.long	3230016427
	.long	4121834935
	.long	3189567556
	.long	2319200184
	.long	3230016431
	.long	3019906587
	.long	3189567664
	.long	1234656664
	.long	3230016435
	.long	3882744783
	.long	3189567930
	.long	120715280
	.long	3230016439
	.long	1927363170
	.long	3189567242
	.long	3272450504
	.long	3230016442
	.long	8203115
	.long	3189567876
	.long	2100034368
	.long	3230016446
	.long	1260625560
	.long	3189567855
	.long	898540192
	.long	3230016450
	.long	1791679111
	.long	3189567639
	.long	3963040720
	.long	3230016453
	.long	831133439
	.long	3189567590
	.long	2703706240
	.long	3230016457
	.long	2803140504
	.long	3189567482
	.long	1415608352
	.long	3230016461
	.long	2955844462
	.long	3189568058
	.long	98850816
	.long	3230016465
	.long	4017967070
	.long	3189567455
	.long	3048504096
	.long	3230016468
	.long	3974385451
	.long	3189567871
	.long	1674736240
	.long	3230016472
	.long	2424114556
	.long	3189567887
	.long	272616624
	.long	3230016476
	.long	4261883032
	.long	3189567073
	.long	3137214056
	.long	3230016479
	.long	3716750895
	.long	3189567522
	.long	1678694944
	.long	3230016483
	.long	498025304
	.long	3189567180
	.long	192127016
	.long	3230016487
	.long	1011852160
	.long	3189567511
	.long	2972577480
	.long	3230016490
	.long	558940916
	.long	3189567850
	.long	1430211120
	.long	3230016494
	.long	2550865739
	.long	3189567935
	.long	4155061376
	.long	3230016497
	.long	1930847170
	.long	3189567371
	.long	2557291968
	.long	3230016501
	.long	1716354804
	.long	3189567130
	.long	931967984
	.long	3230016505
	.long	2729403392
	.long	3189567981
	.long	3574154016
	.long	3230016508
	.long	1067280599
	.long	3189567890
	.long	1894012232
	.long	3230016512
	.long	2094260282
	.long	3189567604
	.long	186606176
	.long	3230016516
	.long	3897933165
	.long	3189568046
	.long	2746998904
	.long	3230016519
	.long	1935720459
	.long	3189567701
	.long	985351064
	.long	3230016523
	.long	2170344821
	.long	3189567164
	.long	3491691984
	.long	3230016526
	.long	4220979537
	.long	3189567547
	.long	1676181328
	.long	3230016530
	.long	1953924393
	.long	3189567854
	.long	4128847448
	.long	3230016533
	.long	1689829067
	.long	3189567468
	.long	2259849008
	.long	3230016537
	.long	2367753130
	.long	3189567619
	.long	364246088
	.long	3230016541
	.long	2263309073
	.long	3189567722
	.long	2737098280
	.long	3230016544
	.long	2366756869
	.long	3189567856
	.long	788562816
	.long	3230016548
	.long	2294653975
	.long	3189567145
	.long	3108665616
	.long	3230016551
	.long	1786494117
	.long	3189567254
	.long	1107562960
	.long	3230016555
	.long	3655887416
	.long	3189567689
	.long	3375279848
	.long	3230016558
	.long	1178948625
	.long	3189567241
	.long	1321971608
	.long	3230016562
	.long	1620080441
	.long	3189567466
	.long	3537662304
	.long	3230016565
	.long	154455618
	.long	3189567981
	.long	1432506360
	.long	3230016569
	.long	3128192125
	.long	3189567889
	.long	3596526920
	.long	3230016572
	.long	2026842750
	.long	3189567212
	.long	1439877480
	.long	3230016576
	.long	1384431897
	.long	3189567284
	.long	3552580280
	.long	3230016579
	.long	1999983129
	.long	3189568079
	.long	1344787944
	.long	3230016583
	.long	4035436820
	.long	3189567579
	.long	3406521808
	.long	3230016586
	.long	77799817
	.long	3189567285
	.long	1147933592
	.long	3230016590
	.long	1261060207
	.long	3189567477
	.long	3159043768
	.long	3230016593
	.long	2102717775
	.long	3189567605
	.long	850003184
	.long	3230016597
	.long	161472469
	.long	3189567688
	.long	2810831440
	.long	3230016600
	.long	1937336881
	.long	3189567665
	.long	451678520
	.long	3230016604
	.long	2268353549
	.long	3189567773
	.long	2362563168
	.long	3230016607
	.long	1785871595
	.long	3189567889
	.long	4248601816
	.long	3230016610
	.long	2434669991
	.long	3189567898
	.long	1814910472
	.long	3230016614
	.long	2666999375
	.long	3189568058
	.long	3651506624
	.long	3230016617
	.long	1033139811
	.long	3189567312
	.long	1168538136
	.long	3230016621
	.long	4004618418
	.long	3189567740
	.long	2956021672
	.long	3230016624
	.long	3799928764
	.long	3189567751
	.long	424104288
	.long	3230016628
	.long	3650233137
	.long	3189567549
	.long	2162801816
	.long	3230016631
	.long	437313833
	.long	3189567438
	.long	3877227800
	.long	3230016634
	.long	2138293494
	.long	3189567140
	.long	1272495376
	.long	3230016638
	.long	2891370752
	.long	3189567168
	.long	2938619176
	.long	3230016641
	.long	1704142730
	.long	3189567114
	.long	285744240
	.long	3230016645
	.long	500817607
	.long	3189568035
	.long	1903884424
	.long	3230016648
	.long	3102571944
	.long	3189567668
	.long	3498151272
	.long	3230016651
	.long	2374193154
	.long	3189567933
	.long	773655960
	.long	3230016655
	.long	2898331636
	.long	3189568089
	.long	2320411168
	.long	3230016658
	.long	1983295500
	.long	3189567113
	.long	3843527280
	.long	3230016661
	.long	2132853542
	.long	3189567093
	.long	1048114320
	.long	3230016665
	.long	3551155364
	.long	3189567407
	.long	2524183824
	.long	3230016668
	.long	2564000663
	.long	3189567090
	.long	3976845048
	.long	3230016671
	.long	361581803
	.long	3189567196
	.long	1111206880
	.long	3230016675
	.long	696765230
	.long	3189568040
	.long	2517279744
	.long	3230016678
	.long	941542475
	.long	3189567463
	.long	3900171776
	.long	3230016681
	.long	2377895552
	.long	3189567282
	.long	964990760
	.long	3230016685
	.long	1173605489
	.long	3189567454
	.long	2301746008
	.long	3230016688
	.long	3096720313
	.long	3189567400
	.long	3615544576
	.long	3230016691
	.long	1942008121
	.long	3189567335
	.long	611493160
	.long	3230016695
	.long	3375481119
	.long	3189567537
	.long	1879599992
	.long	3230016698
	.long	4103239143
	.long	3189567624
	.long	3124971056
	.long	3230016701
	.long	3773474925
	.long	3189567869
	.long	52711992
	.long	3230016705
	.long	2465315445
	.long	3189567444
	.long	1252829960
	.long	3230016708
	.long	1041581158
	.long	3189567781
	.long	2430429904
	.long	3230016711
	.long	166620501
	.long	3189567739
	.long	3585583704
	.long	3230016714
	.long	2379237630
	.long	3189567984
	.long	423395616
	.long	3230016718
	.long	134834559
	.long	3189567178
	.long	1533871416
	.long	3230016721
	.long	1449318760
	.long	3189567373
	.long	2622114680
	.long	3230016724
	.long	3042372984
	.long	3189567127
	.long	3688195920
	.long	3230016727
	.long	1265290101
	.long	3189567921
	.long	437218048
	.long	3230016731
	.long	2248549362
	.long	3189567262
	.long	1459185496
	.long	3230016734
	.long	1293250365
	.long	3189567151
	.long	2459200496
	.long	3230016737
	.long	313786605
	.long	3189567180
	.long	3437332240
	.long	3230016740
	.long	3289151823
	.long	3189567872
	.long	98682312
	.long	3230016744
	.long	2693758821
	.long	3189567877
	.long	1033253848
	.long	3230016747
	.long	4237443192
	.long	3189567301
	.long	1946147760
	.long	3230016750
	.long	4099404586
	.long	3189567972
	.long	2837431968
	.long	3230016753
	.long	343640666
	.long	3189567552
	.long	3707174040
	.long	3230016756
	.long	4238401458
	.long	3189567983
	.long	260473960
	.long	3230016760
	.long	21423548
	.long	3189567559
	.long	1087333264
	.long	3230016763
	.long	3515278593
	.long	3189567309
	.long	1892851296
	.long	3230016766
	.long	629106375
	.long	3189567166
	.long	2677094384
	.long	3230016769
	.long	2048670044
	.long	3189567227
	.long	3440128544
	.long	3230016772
	.long	3739217292
	.long	3189568009
	.long	4182019504
	.long	3230016775
	.long	3662039444
	.long	3189567618
	.long	607865368
	.long	3230016779
	.long	1766486991
	.long	3189567112
	.long	1307665824
	.long	3230016782
	.long	512689457
	.long	3189567670
	.long	1986518392
	.long	3230016785
	.long	1704577378
	.long	3189567760
	.long	2644487576
	.long	3230016788
	.long	4001935135
	.long	3189567473
	.long	3281637576
	.long	3230016791
	.long	1184452342
	.long	3189567735
	.long	3898032312
	.long	3230016794
	.long	2714793956
	.long	3189567464
	.long	198768096
	.long	3230016798
	.long	3657654418
	.long	3189567905
	.long	773842856
	.long	3230016801
	.long	2809688013
	.long	3189567736
	.long	1328352328
	.long	3230016804
	.long	390727591
	.long	3189567393
	.long	1862359256
	.long	3230016807
	.long	1135237925
	.long	3189567248
	.long	2375926096
	.long	3230016810
	.long	1377572509
	.long	3189567837
	.long	2869115032
	.long	3230016813
	.long	3148383775
	.long	3189568032
	.long	3341987960
	.long	3230016816
	.long	3809536738
	.long	3189567313
	.long	3794606472
	.long	3230016819
	.long	4243445934
	.long	3189568034
	.long	4227031920
	.long	3230016822
	.long	1302642088
	.long	3189567451
	.long	344358048
	.long	3230016826
	.long	3239125361
	.long	3189567151
	.long	736580224
	.long	3230016829
	.long	3753161736
	.long	3189567129
	.long	1108791648
	.long	3230016832
	.long	624357483
	.long	3189568045
	.long	1461052568
	.long	3230016835
	.long	3153257064
	.long	3189567338
	.long	1793422920
	.long	3230016838
	.long	2705820149
	.long	3189567635
	.long	2105962400
	.long	3230016841
	.long	911564860
	.long	3189567737
	.long	2398730424
	.long	3230016844
	.long	1086958332
	.long	3189567948
	.long	2671786152
	.long	3230016847
	.long	1598984535
	.long	3189567210
	.long	2925188456
	.long	3230016850
	.long	4113385879
	.long	3189567401
	.long	3158995968
	.long	3230016853
	.long	2426812671
	.long	3189567396
	.long	3373267048
	.long	3230016856
	.long	3480845679
	.long	3189567359
	.long	3568059792
	.long	3230016859
	.long	3306230729
	.long	3189567924
	.long	3743432056
	.long	3230016862
	.long	3853821277
	.long	3189567317
	.long	3899441408
	.long	3230016865
	.long	2049309148
	.long	3189567699
	.long	4036145192
	.long	3230016868
	.long	2667321817
	.long	3189567166
	.long	4153600472
	.long	3230016871
	.long	3540670389
	.long	3189567114
	.long	4251864072
	.long	3230016874
	.long	404207883
	.long	3189567310
	.long	36025272
	.long	3230016878
	.long	2837617046
	.long	3189567103
	.long	96074976
	.long	3230016881
	.long	1231280453
	.long	3189567669
	.long	137101976
	.long	3230016884
	.long	792492437
	.long	3189567071
	.long	159162080
	.long	3230016887
	.long	2385525292
	.long	3189567618
	.long	162310880
	.long	3230016890
	.long	3272051015
	.long	3189567858
	.long	146603712
	.long	3230016893
	.long	623573538
	.long	3189567881
	.long	112095672
	.long	3230016896
	.long	2151835939
	.long	3189567451
	.long	58841608
	.long	3230016899
	.long	958006130
	.long	3189567217
	.long	4281863424
	.long	3230016901
	.long	549948691
	.long	3189567863
	.long	4191280920
	.long	3230016904
	.long	4177689987
	.long	3189567217
	.long	4082115512
	.long	3230016907
	.long	2895765193
	.long	3189567599
	.long	3954421112
	.long	3230016910
	.long	1055606208
	.long	3189567799
	.long	3808251392
	.long	3230016913
	.long	1966521217
	.long	3189567351
	.long	3643659776
	.long	3230016916
	.long	1017382758
	.long	3189567732
	.long	3460699480
	.long	3230016919
	.long	3485890155
	.long	3189567410
	.long	3259423472
	.long	3230016922
	.long	2657419170
	.long	3189567140
	.long	3039884488
	.long	3230016925
	.long	4198858220
	.long	3189568080
	.long	2802135064
	.long	3230016928
	.long	2343740494
	.long	3189567869
	.long	2546227480
	.long	3230016931
	.long	2423084215
	.long	3189567957
	.long	2272213808
	.long	3230016934
	.long	3887273856
	.long	3189567631
	.long	1980145888
	.long	3230016937
	.long	3992755473
	.long	3189567251
	.long	1670075336
	.long	3230016940
	.long	722498073
	.long	3189567389
	.long	1342053552
	.long	3230016943
	.long	3573506400
	.long	3189567965
	.long	996131728
	.long	3230016946
	.long	714695829
	.long	3189567392
	.long	632360808
	.long	3230016949
	.long	2816704229
	.long	3189567846
	.long	250791552
	.long	3230016952
	.long	386951333
	.long	3189567243
	.long	4146441768
	.long	3230016954
	.long	2155319939
	.long	3189567580
	.long	3729427184
	.long	3230016957
	.long	4227840137
	.long	3189567931
	.long	3294765200
	.long	3230016960
	.long	1718354212
	.long	3189567672
	.long	2842505696
	.long	3230016963
	.long	4243880582
	.long	3189567656
	.long	2372698352
	.long	3230016966
	.long	3784093842
	.long	3189567302
	.long	1885392624
	.long	3230016969
	.long	2318733051
	.long	3189567813
	.long	1380637784
	.long	3230016972
	.long	497929746
	.long	3189567210
	.long	858482864
	.long	3230016975
	.long	3218883783
	.long	3189567649
	.long	318976720
	.long	3230016978
	.long	3492652862
	.long	3189567382
	.long	4057135272
	.long	3230016980
	.long	473724673
	.long	3189568069
	.long	3483072376
	.long	3230016983
	.long	150485010
	.long	3189567757
	.long	2891803552
	.long	3230016986
	.long	3028900958
	.long	3189567173
	.long	2283376816
	.long	3230016989
	.long	321242596
	.long	3189567822
	.long	1657840008
	.long	3230016992
	.long	151305746
	.long	3189568029
	.long	1015240760
	.long	3230016995
	.long	2208026401
	.long	3189567184
	.long	355626480
	.long	3230016998
	.long	2487781842
	.long	3189567931
	.long	3974011720
	.long	3230017000
	.long	2391142094
	.long	3189567091
	.long	3280508896
	.long	3230017003
	.long	734457618
	.long	3189568092
	.long	2570132168
	.long	3230017006
	.long	3070902701
	.long	3189567794
	.long	1842928168
	.long	3230017009
	.long	1110877923
	.long	3189567896
	.long	1098943352
	.long	3230017012
	.long	3880360185
	.long	3189567898
	.long	338223984
	.long	3230017015
	.long	391995809
	.long	3189567294
	.long	3855783416
	.long	3230017017
	.long	4186345433
	.long	3189567745
	.long	3061732952
	.long	3230017020
	.long	3682852050
	.long	3189567081
	.long	2251085560
	.long	3230017023
	.long	3057037137
	.long	3189567617
	.long	1423886760
	.long	3230017026
	.long	3352664894
	.long	3189567074
	.long	580181848
	.long	3230017029
	.long	1170568982
	.long	3189567927
	.long	4014983272
	.long	3230017031
	.long	671148579
	.long	3189567293
	.long	3138401368
	.long	3230017034
	.long	267496658
	.long	3189567304
	.long	2245448192
	.long	3230017037
	.long	1285167087
	.long	3189568066
	.long	1336168336
	.long	3230017040
	.long	4259909589
	.long	3189567788
	.long	410606184
	.long	3230017043
	.long	1494078902
	.long	3189568031
	.long	3763773256
	.long	3230017045
	.long	261935754
	.long	3189567685
	.long	2805778992
	.long	3230017048
	.long	3679585605
	.long	3189567195
	.long	1831634536
	.long	3230017051
	.long	809066513
	.long	3189567664
	.long	841383576
	.long	3230017054
	.long	385945238
	.long	3189567897
	.long	4130036912
	.long	3230017056
	.long	825579909
	.long	3189567603
	.long	3107703272
	.long	3230017059
	.long	3911295427
	.long	3189567514
	.long	2069393104
	.long	3230017062
	.long	3473784588
	.long	3189567461
	.long	1015149384
	.long	3230017065
	.long	629861200
	.long	3189567515
	.long	4239982216
	.long	3230017067
	.long	2659142760
	.long	3189567083
	.long	3153999632
	.long	3230017070
	.long	1251680713
	.long	3189567080
	.long	2052211384
	.long	3230017073
	.long	3168705163
	.long	3189567992
	.long	934659776
	.long	3230017076
	.long	1674797624
	.long	3189567952
	.long	4096354216
	.long	3230017078
	.long	437847902
	.long	3189567971
	.long	2947402064
	.long	3230017081
	.long	4024401533
	.long	3189567960
	.long	1782812400
	.long	3230017084
	.long	786902458
	.long	3189567873
	.long	602626840
	.long	3230017087
	.long	2593792633
	.long	3189567812
	.long	3701854136
	.long	3230017089
	.long	3989721221
	.long	3189567126
	.long	2490600968
	.long	3230017092
	.long	2545746866
	.long	3189567591
	.long	1263875768
	.long	3230017095
	.long	3115843521
	.long	3189567377
	.long	21719488
	.long	3230017098
	.long	1730226063
	.long	3189567307
	.long	3059140216
	.long	3230017100
	.long	690350922
	.long	3189567892
	.long	1786244008
	.long	3230017103
	.long	3350998585
	.long	3189567394
	.long	498038616
	.long	3230017106
	.long	1499123379
	.long	3189568082
	.long	3489531672
	.long	3230017108
	.long	2598665922
	.long	3189567118
	.long	2170828712
	.long	3230017111
	.long	2436376826
	.long	3189567935
	.long	836937048
	.long	3230017114
	.long	1977004902
	.long	3189568052
	.long	3782863816
	.long	3230017116
	.long	4027224619
	.long	3189567373
	.long	2418714096
	.long	3230017119
	.long	2616869856
	.long	3189567271
	.long	1039494712
	.long	3230017122
	.long	3775628133
	.long	3189567616
	.long	3940212336
	.long	3230017124
	.long	419018052
	.long	3189567909
	.long	2530971600
	.long	3230017127
	.long	150919657
	.long	3189567379
	.long	1106778856
	.long	3230017130
	.long	2926254537
	.long	3189567139
	.long	3962640312
	.long	3230017132
	.long	1610863810
	.long	3189567216
	.long	2508660136
	.long	3230017135
	.long	1166015244
	.long	3189567672
	.long	1039844240
	.long	3230017138
	.long	645159638
	.long	3189567685
	.long	3851198376
	.long	3230017140
	.long	2449439581
	.long	3189567695
	.long	2352826264
	.long	3230017143
	.long	1619104640
	.long	3189567458
	.long	839733360
	.long	3230017146
	.long	2427070371
	.long	3189567165
	.long	3606924968
	.long	3230017148
	.long	2166480890
	.long	3189567544
	.long	2064504368
	.long	3230017151
	.long	2283025905
	.long	3189567907
	.long	507476584
	.long	3230017154
	.long	3923356754
	.long	3189567272
	.long	3230846472
	.long	3230017156
	.long	805331677
	.long	3189567533
	.long	1644716880
	.long	3230017159
	.long	3790469685
	.long	3189567406
	.long	44092384
	.long	3230017162
	.long	2689788214
	.long	3189567653
	.long	2723977432
	.long	3230017164
	.long	2762250221
	.long	3189567072
	.long	1094474416
	.long	3230017167
	.long	2781925193
	.long	3189567716
	.long	3745554800
	.long	3230017169
	.long	593120878
	.long	3189567817
	.long	2087320704
	.long	3230017172
	.long	403414988
	.long	3189568021
	.long	414776008
	.long	3230017175
	.long	359307328
	.long	3189567387
	.long	3022924432
	.long	3230017177
	.long	3373547966
	.long	3189567572
	.long	1321867688
	.long	3230017180
	.long	3978711340
	.long	3189567805
	.long	3901576528
	.long	3230017182
	.long	2765663238
	.long	3189568046
	.long	2172152384
	.long	3230017185
	.long	1181150766
	.long	3189568053
	.long	428598440
	.long	3230017188
	.long	2473022131
	.long	3189567471
	.long	2965917728
	.long	3230017190
	.long	1621744488
	.long	3189567971
	.long	1194211288
	.long	3230017193
	.long	2931299013
	.long	3189567190
	.long	3703449168
	.long	3230017195
	.long	873154950
	.long	3189568039
	.long	1903732144
	.long	3230017198
	.long	1510784525
	.long	3189567521
	.long	90062704
	.long	3230017201
	.long	2918231590
	.long	3189568086
	.long	2557443240
	.long	3230017203
	.long	3329944838
	.long	3189567475
	.long	715974088
	.long	3230017206
	.long	2679715410
	.long	3189568021
	.long	3155624672
	.long	3230017208
	.long	1841110143
	.long	3189567518
	.long	1286495072
	.long	3230017211
	.long	3881638924
	.long	3189567497
	.long	3698554440
	.long	3230017213
	.long	167906931
	.long	3189567169
	.long	1801902600
	.long	3230017216
	.long	1468264167
	.long	3189567597
	.long	4186508448
	.long	3230017218
	.long	2400624638
	.long	3189567691
	.long	2262471560
	.long	3230017221
	.long	3796182064
	.long	3189567357
	.long	324793264
	.long	3230017224
	.long	2801238958
	.long	3189567581
	.long	2668474776
	.long	3230017226
	.long	2273142813
	.long	3189567436
	.long	703615288
	.long	3230017229
	.long	831867571
	.long	3189567237
	.long	3020183048
	.long	3230017231
	.long	2238048829
	.long	3189567595
	.long	1028277008
	.long	3230017234
	.long	1163763070
	.long	3189567452
	.long	3317865168
	.long	3230017236
	.long	1206450788
	.long	3189567228
	.long	1299046216
	.long	3230017239
	.long	737162226
	.long	3189567879
	.long	3561787920
	.long	3230017241
	.long	2433253277
	.long	3189567901
	.long	1516188728
	.long	3230017244
	.long	3561277099
	.long	3189567506
	.long	3752216144
	.long	3230017246
	.long	1385095507
	.long	3189567674
	.long	1679968384
	.long	3230017249
	.long	829462408
	.long	3189567156
	.long	3889412704
	.long	3230017251
	.long	2939266725
	.long	3189567673
	.long	1790647080
	.long	3230017254
	.long	4214100827
	.long	3189567848
	.long	3973638544
	.long	3230017256
	.long	2537954451
	.long	3189567376
	.long	1848484816
	.long	3230017259
	.long	1128740312
	.long	3189567098
	.long	4005152680
	.long	3230017261
	.long	1932232434
	.long	3189568053
	.long	1853739648
	.long	3230017264
	.long	1359710694
	.long	3189567457
	.long	3984212256
	.long	3230017266
	.long	1268482287
	.long	3189567990
	.long	1806667776
	.long	3230017269
	.long	839689017
	.long	3189567631
	.long	3911072520
	.long	3230017271
	.long	3727165601
	.long	3189567921
	.long	1707523520
	.long	3230017274
	.long	1836550952
	.long	3189567895
	.long	3785986872
	.long	3230017276
	.long	172951407
	.long	3189567220
	.long	1556559360
	.long	3230017279
	.long	1166449891
	.long	3189567294
	.long	3609206856
	.long	3230017281
	.long	28625366
	.long	3189567222
	.long	1354025920
	.long	3230017284
	.long	4038426582
	.long	3189567978
	.long	3380982208
	.long	3230017286
	.long	2756284659
	.long	3189567362
	.long	1100172048
	.long	3230017289
	.long	848830252
	.long	3189567230
	.long	3101560856
	.long	3230017291
	.long	1868170309
	.long	3189567423
	.long	795244752
	.long	3230017294
	.long	3128260908
	.long	3189567885
	.long	2771188936
	.long	3230017296
	.long	1691261279
	.long	3189567706
	.long	439489304
	.long	3230017299
	.long	2886711298
	.long	3189567236
	.long	2390110824
	.long	3230017301
	.long	1311431101
	.long	3189567133
	.long	33149176
	.long	3230017304
	.long	3042183093
	.long	3189567401
	.long	1958569120
	.long	3230017306
	.long	3174208880
	.long	3189567462
	.long	3871433416
	.long	3230017308
	.long	302374021
	.long	3189567243
	.long	1476804712
	.long	3230017311
	.long	712531519
	.long	3189567241
	.long	3364647440
	.long	3230017313
	.long	3705318448
	.long	3189567568
	.long	945056744
	.long	3230017316
	.long	2424171915
	.long	3189568016
	.long	2807996856
	.long	3230017318
	.long	199511961
	.long	3189567094
	.long	363562688
	.long	3230017321
	.long	3766234060
	.long	3189567214
	.long	2201718256
	.long	3230017323
	.long	1956195829
	.long	3189567592
	.long	4027525584
	.long	3230017325
	.long	2891794585
	.long	3189567375
	.long	1546046576
	.long	3230017328
	.long	843173017
	.long	3189567760
	.long	3347244936
	.long	3230017330
	.long	1609532414
	.long	3189567955
	.long	841215080
	.long	3230017333
	.long	3356704285
	.long	3189567294
	.long	2617920488
	.long	3230017335
	.long	1641729240
	.long	3189567346
	.long	87455368
	.long	3230017338
	.long	1455753222
	.long	3189567878
	.long	1839783016
	.long	3230017340
	.long	1936589978
	.long	3189567944
	.long	3579964728
	.long	3230017342
	.long	1006586129
	.long	3189567993
	.long	1013061704
	.long	3230017345
	.long	2766825484
	.long	3189567880
	.long	2729036928
	.long	3230017347
	.long	1888685184
	.long	3189567954
	.long	137984104
	.long	3230017350
	.long	848046306
	.long	3189568090
	.long	1829866024
	.long	3230017352
	.long	4178224056
	.long	3189567750
	.long	3509743488
	.long	3230017354
	.long	2637072474
	.long	3189567070
	.long	882677184
	.long	3230017357
	.long	3133305384
	.long	3189567936
	.long	2538629624
	.long	3230017359
	.long	1343200028
	.long	3189567905
	.long	4182661312
	.long	3230017361
	.long	107195460
	.long	3189567427
	.long	1519832648
	.long	3230017364
	.long	602154914
	.long	3189567857
	.long	3140105848
	.long	3230017366
	.long	659160740
	.long	3189567417
	.long	453573824
	.long	3230017369
	.long	236889930
	.long	3189567396
	.long	2050198592
	.long	3230017371
	.long	1175634921
	.long	3189567087
	.long	3635040168
	.long	3230017373
	.long	1241855527
	.long	3189567941
	.long	913158504
	.long	3230017376
	.long	3947904414
	.long	3189567477
	.long	2474515312
	.long	3230017378
	.long	1206979822
	.long	3189567534
	.long	4024170344
	.long	3230017380
	.long	1847397805
	.long	3189567152
	.long	1267183240
	.long	3230017383
	.long	2916539301
	.long	3189567754
	.long	2793515456
	.long	3230017385
	.long	2568213263
	.long	3189568078
	.long	13259168
	.long	3230017388
	.long	3003245330
	.long	3189567303
	.long	1516375624
	.long	3230017390
	.long	3472633477
	.long	3189567153
	.long	3007924104
	.long	3230017392
	.long	532406289
	.long	3189567855
	.long	192963816
	.long	3230017395
	.long	4149310663
	.long	3189567165
	.long	1661455728
	.long	3230017397
	.long	946970842
	.long	3189567598
	.long	3118458864
	.long	3230017399
	.long	2040517972
	.long	3189567254
	.long	269032128
	.long	3230017402
	.long	3669971831
	.long	3189568072
	.long	1703136256
	.long	3230017404
	.long	2375197389
	.long	3189567687
	.long	3125829976
	.long	3230017406
	.long	296757744
	.long	3189567673
	.long	242171944
	.long	3230017409
	.long	611424102
	.long	3189567452
	.long	1642122608
	.long	3230017411
	.long	1700968423
	.long	3189567419
	.long	3030740440
	.long	3230017413
	.long	2947762668
	.long	3189567968
	.long	113083840
	.long	3230017416
	.long	690901756
	.long	3189567496
	.long	1479112984
	.long	3230017418
	.long	121139903
	.long	3189567571
	.long	2833886088
	.long	3230017420
	.long	173386055
	.long	3189567866
	.long	4177428584
	.long	3230017422
	.long	2964718132
	.long	3189567231
	.long	1214798496
	.long	3230017425
	.long	541562024
	.long	3189567842
	.long	2535955680
	.long	3230017427
	.long	3368765612
	.long	3189568076
	.long	3845958008
	.long	3230017429
	.long	3194541507
	.long	3189567689
	.long	849863256
	.long	3230017432
	.long	3443356552
	.long	3189567858
	.long	2137631024
	.long	3230017434
	.long	1180679397
	.long	3189567138
	.long	3414318912
	.long	3230017436
	.long	2261929999
	.long	3189567651
	.long	384984464
	.long	3230017439
	.long	954057098
	.long	3189567971
	.long	1639587024
	.long	3230017441
	.long	1346995839
	.long	3189567263
	.long	2883183944
	.long	3230017443
	.long	3253373727
	.long	3189567382
	.long	4115799808
	.long	3230017445
	.long	4143791142
	.long	3189567800
	.long	1042491824
	.long	3230017448
	.long	4240432954
	.long	3189567700
	.long	2253218992
	.long	3230017450
	.long	1891708033
	.long	3189568072
	.long	3453038360
	.long	3230017452
	.long	710292621
	.long	3189567662
	.long	347006880
	.long	3230017455
	.long	2712254611
	.long	3189567116
	.long	1525083304
	.long	3230017457
	.long	349583280
	.long	3189568004
	.long	2692324448
	.long	3230017459
	.long	1803227864
	.long	3189567740
	.long	3848754312
	.long	3230017461
	.long	3644606976
	.long	3189567826
	.long	699429536
	.long	3230017464
	.long	1067153346
	.long	3189567751
	.long	1834308568
	.long	3230017466
	.long	1335350692
	.long	3189567081
	.long	2958447872
	.long	3230017468
	.long	1803696599
	.long	3189567557
	.long	4071871152
	.long	3230017470
	.long	1232152547
	.long	3189568017
	.long	879634736
	.long	3230017473
	.long	1455437901
	.long	3189567512
	.long	1971696744
	.long	3230017475
	.long	3708110750
	.long	3189567399
	.long	3053113344
	.long	3230017477
	.long	3907324819
	.long	3189567290
	.long	4123907920
	.long	3230017479
	.long	4080000314
	.long	3189567141
	.long	889136480
	.long	3230017482
	.long	1286519001
	.long	3189567298
	.long	1938756848
	.long	3230017484
	.long	1292347423
	.long	3189567513
	.long	2977824880
	.long	3230017486
	.long	1159725249
	.long	3189568015
	.long	4006363664
	.long	3230017488
	.long	895621348
	.long	3189567502
	.long	729428896
	.long	3230017491
	.long	1212894155
	.long	3189567280
	.long	1736978096
	.long	3230017493
	.long	296117275
	.long	3189567210
	.long	2734066816
	.long	3230017495
	.long	2463481211
	.long	3189567796
	.long	3720717848
	.long	3230017497
	.long	76524950
	.long	3189567159
	.long	401986584
	.long	3230017500
	.long	1718551671
	.long	3189567216
	.long	1367830248
	.long	3230017502
	.long	3568874946
	.long	3189567584
	.long	2323304104
	.long	3230017504
	.long	2763456396
	.long	3189567666
	.long	3268430632
	.long	3230017506
	.long	1208592501
	.long	3189567715
	.long	4203232240
	.long	3230017508
	.long	4148835618
	.long	3189567851
	.long	832763976
	.long	3230017511
	.long	610418007
	.long	3189567082
	.long	1746982680
	.long	3230017513
	.long	3035714190
	.long	3189567431
	.long	2650943256
	.long	3230017515
	.long	85931336
	.long	3189567843
	.long	3544667832
	.long	3230017517
	.long	3006685957
	.long	3189567286
	.long	133211144
	.long	3230017520
	.long	878859600
	.long	3189567873
	.long	1006529776
	.long	3230017522
	.long	854333969
	.long	3189567724
	.long	1869678336
	.long	3230017524
	.long	3058646748
	.long	3189567155
	.long	2722678648
	.long	3230017526
	.long	360733640
	.long	3189567693
	.long	3565552488
	.long	3230017528
	.long	3015564990
	.long	3189568013
	.long	103354256
	.long	3230017531
	.long	2286095909
	.long	3189568081
	.long	926040176
	.long	3230017533
	.long	1899270393
	.long	3189567137
	.long	1738664488
	.long	3230017535
	.long	1377641292
	.long	3189567833
	.long	2541248696
	.long	3230017537
	.long	4266686523
	.long	3189568077
	.long	3333814224
	.long	3230017539
	.long	4018775563
	.long	3189567218
	.long	4116382408
	.long	3230017541
	.long	3373913644
	.long	3189567103
	.long	594007232
	.long	3230017544
	.long	2818265402
	.long	3189568021
	.long	1356644520
	.long	3230017546
	.long	1075601108
	.long	3189567716
	.long	2109348112
	.long	3230017548
	.long	3342127702
	.long	3189567566
	.long	2852139088
	.long	3230017550
	.long	2910355434
	.long	3189567504
	.long	3585038464
	.long	3230017552
	.long	611858749
	.long	3189567074
	.long	13099880
	.long	3230017555
	.long	1954698831
	.long	3189567516
	.long	726278816
	.long	3230017557
	.long	550712630
	.long	3189567710
	.long	1429628792
	.long	3230017559
	.long	3672474493
	.long	3189567278
	.long	2123170544
	.long	3230017561
	.long	1160875209
	.long	3189567651
	.long	2806924760
	.long	3230017563
	.long	2242524344
	.long	3189568000
	.long	3480912064
	.long	3230017565
	.long	2823138691
	.long	3189567327
	.long	4145152992
	.long	3230017567
	.long	3521079748
	.long	3189567568
	.long	504700744
	.long	3230017570
	.long	1136670137
	.long	3189567486
	.long	1149510328
	.long	3230017572
	.long	806933442
	.long	3189567823
	.long	1784634816
	.long	3230017574
	.long	1606084669
	.long	3189567242
	.long	2410094488
	.long	3230017576
	.long	2841342625
	.long	3189567480
	.long	3025909584
	.long	3230017578
	.long	984188059
	.long	3189567239
	.long	3632100264
	.long	3230017580
	.long	3011730434
	.long	3189567337
	.long	4228686632
	.long	3230017582
	.long	328432041
	.long	3189567676
	.long	520721440
	.long	3230017585
	.long	2057225622
	.long	3189567268
	.long	1098159248
	.long	3230017587
	.long	3150727306
	.long	3189567348
	.long	1666052680
	.long	3230017589
	.long	3291380622
	.long	3189567307
	.long	2224421584
	.long	3230017591
	.long	3570771984
	.long	3189567798
	.long	2773285760
	.long	3230017593
	.long	2427604440
	.long	3189567698
	.long	3312664936
	.long	3230017595
	.long	3733848165
	.long	3189567211
	.long	3842578768
	.long	3230017597
	.long	2673912350
	.long	3189567904
	.long	68079584
	.long	3230017600
	.long	960995892
	.long	3189567616
	.long	579121512
	.long	3230017602
	.long	4150493479
	.long	3189567659
	.long	1080756744
	.long	3230017604
	.long	1054941272
	.long	3189567735
	.long	1573004704
	.long	3230017606
	.long	2574365292
	.long	3189568007
	.long	2055884768
	.long	3230017608
	.long	2507498673
	.long	3189567092
	.long	2529416224
	.long	3230017610
	.long	2082176323
	.long	3189567207
	.long	2993618320
	.long	3230017612
	.long	3719982601
	.long	3189568058
	.long	3448510256
	.long	3230017614
	.long	4104247904
	.long	3189567897
	.long	3894111152
	.long	3230017616
	.long	3735557011
	.long	3189567647
	.long	35472776
	.long	3230017619
	.long	2914260983
	.long	3189567862
	.long	462548736
	.long	3230017621
	.long	1334094042
	.long	3189567734
	.long	880390688
	.long	3230017623
	.long	1291097379
	.long	3189567171
	.long	1289017512
	.long	3230017625
	.long	1627119803
	.long	3189567853
	.long	1688448064
	.long	3230017627
	.long	3886873723
	.long	3189567117
	.long	2078701104
	.long	3230017629
	.long	627887757
	.long	3189567206
	.long	2459795360
	.long	3230017631
	.long	1722013861
	.long	3189567101
	.long	2831749488
	.long	3230017633
	.long	4163262354
	.long	3189567678
	.long	3194582104
	.long	3230017635
	.long	4170237688
	.long	3189567639
	.long	3548311752
	.long	3230017637
	.long	47563772
	.long	3189567637
	.long	3892956928
	.long	3230017639
	.long	2990136007
	.long	3189567231
	.long	4228536056
	.long	3230017641
	.long	4294532248
	.long	3189567995
	.long	260100240
	.long	3230017644
	.long	2060709623
	.long	3189567397
	.long	577602376
	.long	3230017646
	.long	1488525373
	.long	3189568044
	.long	886093456
	.long	3230017648
	.long	1657840200
	.long	3189567474
	.long	1185591680
	.long	3230017650
	.long	3155771782
	.long	3189567399
	.long	1476115208
	.long	3230017652
	.long	2195162329
	.long	3189567591
	.long	1757682144
	.long	3230017654
	.long	1292806663
	.long	3189567956
	.long	2030310544
	.long	3230017656
	.long	2151451208
	.long	3189567540
	.long	2294018392
	.long	3230017658
	.long	2224122735
	.long	3189567630
	.long	2548823632
	.long	3230017660
	.long	3554417607
	.long	3189567684
	.long	2794744152
	.long	3230017662
	.long	3831650627
	.long	3189567409
	.long	3031797776
	.long	3230017664
	.long	4024246224
	.long	3189567811
	.long	3260002296
	.long	3230017666
	.long	2530219738
	.long	3189567128
	.long	3479375416
	.long	3230017668
	.long	1618145996
	.long	3189568024
	.long	3689934832
	.long	3230017670
	.long	392379700
	.long	3189567378
	.long	3891698144
	.long	3230017672
	.long	55874694
	.long	3189567574
	.long	4084682928
	.long	3230017674
	.long	4294251475
	.long	3189567312
	.long	4268906688
	.long	3230017676
	.long	1899705040
	.long	3189567783
	.long	149419600
	.long	3230017679
	.long	357699389
	.long	3189567568
	.long	316173656
	.long	3230017681
	.long	3025711727
	.long	3189567790
	.long	474218920
	.long	3230017683
	.long	2921891821
	.long	3189568046
	.long	623572704
	.long	3230017685
	.long	1716968749
	.long	3189567453
	.long	764252248
	.long	3230017687
	.long	2393171526
	.long	3189567748
	.long	896274768
	.long	3230017689
	.long	1343317993
	.long	3189567172
	.long	1019657400
	.long	3230017691
	.long	1388235839
	.long	3189567661
	.long	1134417256
	.long	3230017693
	.long	2766147125
	.long	3189567706
	.long	1240571384
	.long	3230017695
	.long	2162068842
	.long	3189567499
	.long	1338136776
	.long	3230017697
	.long	4075455588
	.long	3189567932
	.long	1427130392
	.long	3230017699
	.long	1579647664
	.long	3189567555
	.long	1507569120
	.long	3230017701
	.long	3785749868
	.long	3189567715
	.long	1579469816
	.long	3230017703
	.long	2639950365
	.long	3189567469
	.long	1642849272
	.long	3230017705
	.long	3611501026
	.long	3189567698
	.long	1697724240
	.long	3230017707
	.long	15422953
	.long	3189568066
	.long	1744111424
	.long	3230017709
	.long	2642213241
	.long	3189568062
	.long	1782027472
	.long	3230017711
	.long	143606300
	.long	3189568058
	.long	1811488992
	.long	3230017713
	.long	501328690
	.long	3189567276
	.long	1832512520
	.long	3230017715
	.long	3258054578
	.long	3189567915
	.long	1845114576
	.long	3230017717
	.long	1297851139
	.long	3189568007
	.long	1849311616
	.long	3230017719
	.long	3589200000
	.long	3189567580
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
	.long	2846560486
	.long	3210737666
	.long	2729511872
	.long	3211784202
	.long	3956813460
	.long	3212307985
	.long	1796237193
	.long	3212828714
	.long	1562748889
	.long	3213092649
	.long	1602292343
	.long	3213352007
	.long	745814092
	.long	3213610865
	.long	2969306084
	.long	3213869224
	.long	3953060839
	.long	3214006263
	.long	1128703349
	.long	3214134948
	.long	1371067273
	.long	3214263386
	.long	131445019
	.long	3214391579
	.long	1427948820
	.long	3214519527
	.long	665817496
	.long	3214647232
	.long	1817459908
	.long	3214774694
	.long	242758400
	.long	3214901915
	.long	2082038134
	.long	3214981455
	.long	2300828032
	.long	3215044825
	.long	571795571
	.long	3215108076
	.long	3120479584
	.long	3215171207
	.long	3276640540
	.long	3215234220
	.long	2949178581
	.long	3215297115
	.long	4036279704
	.long	3215359892
	.long	4130528508
	.long	3215422552
	.long	813954767
	.long	3215485096
	.long	248046571
	.long	3215547523
	.long	4288926261
	.long	3215609833
	.long	1897492927
	.long	3215672029
	.long	3499237165
	.long	3215734109
	.long	2329611090
	.long	3215796075
	.long	203907094
	.long	3215857927
	.long	3222364838
	.long	3215919664
	.long	295408387
	.long	3215981289
	.long	899212401
	.long	3216012696
	.long	2606049022
	.long	3216043395
	.long	4003691233
	.long	3216074038
	.long	1675303236
	.long	3216104626
	.long	789189655
	.long	3216135158
	.long	2213928479
	.long	3216165634
	.long	2518405585
	.long	3216196055
	.long	2561783534
	.long	3216226421
	.long	3198568176
	.long	3216256732
	.long	983674944
	.long	3216286989
	.long	1057364031
	.long	3216317191
	.long	4260371496
	.long	3216347338
	.long	2838974660
	.long	3216377432
	.long	1919860988
	.long	3216407472
	.long	2330291020
	.long	3216437458
	.long	598130195
	.long	3216467391
	.long	1836782285
	.long	3216497270
	.long	2565351466
	.long	3216527096
	.long	3593575201
	.long	3216556869
	.long	1431920350
	.long	3216586590
	.long	1176515501
	.long	3216616258
	.long	3624279252
	.long	3216645873
	.long	977982819
	.long	3216675437
	.long	2616083454
	.long	3216704948
	.long	733015457
	.long	3216734408
	.long	403990373
	.long	3216763816
	.long	2405156675
	.long	3216793172
	.long	3213628385
	.long	3216822477
	.long	3597448029
	.long	3216851731
	.long	25680167
	.long	3216880935
	.long	1848308454
	.long	3216910087
	.long	1231492199
	.long	3216939189
	.long	3222364838
	.long	3216968240
	.long	4274224607
	.long	3216997241
	.long	836496059
	.long	3217026193
	.long	1119829323
	.long	3217043131
	.long	315251054
	.long	3217057557
	.long	518461891
	.long	3217071958
	.long	2094042713
	.long	3217086334
	.long	1109735857
	.long	3217100686
	.long	2221359788
	.long	3217115013
	.long	1492952593
	.long	3217129316
	.long	3576653728
	.long	3217143594
	.long	237880003
	.long	3217157849
	.long	420109003
	.long	3217172079
	.long	180120274
	.long	3217186285
	.long	4162843939
	.long	3217200466
	.long	4126536273
	.long	3217214624
	.long	417628115
	.long	3217228759
	.long	1970736719
	.long	3217242869
	.long	538873716
	.long	3217256956
	.long	758227830
	.long	3217271019
	.long	2968307242
	.long	3217285058
	.long	3211951040
	.long	3217299074
	.long	1825275166
	.long	3217313067
	.long	3437683677
	.long	3217327036
	.long	4086978021
	.long	3217340982
	.long	4104270002
	.long	3217354905
	.long	3819025464
	.long	3217368805
	.long	3559075183
	.long	3217382682
	.long	3650625670
	.long	3217396536
	.long	123302585
	.long	3217410368
	.long	1890030548
	.long	3217424176
	.long	682272600
	.long	3217437962
	.long	1114811729
	.long	3217451725
	.long	3505892050
	.long	3217465465
	.long	3877229085
	.long	3217479183
	.long	2543954549
	.long	3217492879
	.long	4114626469
	.long	3217506552
	.long	311370023
	.long	3217520204
	.long	34658569
	.long	3217533833
	.long	3593519732
	.long	3217547439
	.long	2705545195
	.long	3217561024
	.long	1971736889
	.long	3217574587
	.long	1696647441
	.long	3217588128
	.long	2183357019
	.long	3217601647
	.long	3733482817
	.long	3217615144
	.long	2352221150
	.long	3217628620
	.long	2633258676
	.long	3217642074
	.long	578912458
	.long	3217655507
	.long	780008323
	.long	3217668918
	.long	3530988084
	.long	3217682307
	.long	534951267
	.long	3217695676
	.long	673467853
	.long	3217709023
	.long	4236783385
	.long	3217722348
	.long	2923827788
	.long	3217735653
	.long	1317060595
	.long	3217748937
	.long	3997577740
	.long	3217762199
	.long	2660218276
	.long	3217775441
	.long	1883376699
	.long	3217788662
	.long	1949142230
	.long	3217801862
	.long	3138274530
	.long	3217815041
	.long	1435244737
	.long	3217828200
	.long	1413145637
	.long	3217841338
	.long	3348797986
	.long	3217854455
	.long	3222758658
	.long	3217867552
	.long	1309263324
	.long	3217880629
	.long	2176234471
	.long	3217893685
	.long	1800387473
	.long	3217906721
	.long	452140382
	.long	3217919737
	.long	2695621757
	.long	3217932732
	.long	208809263
	.long	3217945708
	.long	1848308454
	.long	3217958663
	.long	3584589359
	.long	3217971598
	.long	1386895968
	.long	3217984514
	.long	4108155659
	.long	3217997409
	.long	3420215606
	.long	3218010285
	.long	3878621277
	.long	3218023141
	.long	1447787319
	.long	3218035978
	.long	680874053
	.long	3218048795
	.long	1834892843
	.long	3218061592
	.long	870713292
	.long	3218074370
	.long	1168986141
	.long	3218083436
	.long	3245089583
	.long	3218089805
	.long	2494877761
	.long	3218096165
	.long	3338922950
	.long	3218102515
	.long	1607293197
	.long	3218108856
	.long	1719424943
	.long	3218115187
	.long	3799224547
	.long	3218121508
	.long	3675071677
	.long	3218127820
	.long	1469757259
	.long	3218134123
	.long	1600486820
	.long	3218140416
	.long	4188949205
	.long	3218146699
	.long	766352567
	.long	3218152974
	.long	43231408
	.long	3218159239
	.long	2139646042
	.long	3218165494
	.long	2880153102
	.long	3218171740
	.long	2383743325
	.long	3218177977
	.long	768877423
	.long	3218184205
	.long	2448456521
	.long	3218190423
	.long	3244923387
	.long	3218196632
	.long	3275167422
	.long	3218202832
	.long	2655560435
	.long	3218209023
	.long	1501959692
	.long	3218215205
	.long	4224678246
	.long	3218221377
	.long	2348618756
	.long	3218227541
	.long	283080250
	.long	3218233696
	.long	2436859197
	.long	3218239841
	.long	333350563
	.long	3218245978
	.long	2675321801
	.long	3218252105
	.long	985177381
	.long	3218258224
	.long	3964700035
	.long	3218264333
	.long	3135315249
	.long	3218270434
	.long	2902865164
	.long	3218276526
	.long	3377742213
	.long	3218282609
	.long	374891912
	.long	3218288684
	.long	2593684821
	.long	3218294749
	.long	1553148222
	.long	3218300806
	.long	1656739931
	.long	3218306854
	.long	3012481825
	.long	3218312893
	.long	1432962538
	.long	3218318924
	.long	1320242026
	.long	3218324946
	.long	2780952336
	.long	3218330959
	.long	1626300240
	.long	3218336964
	.long	2256971748
	.long	3218342960
	.long	483265518
	.long	3218348948
	.long	704964628
	.long	3218354927
	.long	3026437244
	.long	3218360897
	.long	3256639170
	.long	3218366859
	.long	1499050969
	.long	3218372813
	.long	2151680465
	.long	3218378758
	.long	1022163351
	.long	3218384695
	.long	2507634849
	.long	3218390623
	.long	2414862975
	.long	3218396543
	.long	845152874
	.long	3218402455
	.long	2194349235
	.long	3218408358
	.long	2267936813
	.long	3218414253
	.long	1165944702
	.long	3218420140
	.long	3282948707
	.long	3218426018
	.long	128204514
	.long	3218431889
	.long	390421100
	.long	3218437751
	.long	4167959282
	.long	3218443604
	.long	2968834018
	.long	3218449450
	.long	1185553177
	.long	3218455288
	.long	3210217930
	.long	3218461117
	.long	549623114
	.long	3218466939
	.long	1890030548
	.long	3218472752
	.long	3032400188
	.long	3218478557
	.long	4072261525
	.long	3218484354
	.long	3450874911
	.long	1070995813
	.long	2331021804
	.long	1070990032
	.long	1029453230
	.long	1070984259
	.long	3747176240
	.long	1070978493
	.long	1800682930
	.long	1070972736
	.long	3686719393
	.long	1070966986
	.long	722545241
	.long	1070961245
	.long	1405669883
	.long	1070955511
	.long	1349079372
	.long	1070949785
	.long	461103529
	.long	1070944067
	.long	2945413886
	.long	1070938356
	.long	121152472
	.long	1070932654
	.long	487700860
	.long	1070926959
	.long	3954874384
	.long	1070921271
	.long	1842920138
	.long	1070915592
	.long	2652318780
	.long	1070909920
	.long	1999011482
	.long	1070904256
	.long	4089234463
	.long	1070898599
	.long	244680560
	.long	1070892951
	.long	3262235675
	.long	1070887309
	.long	169336595
	.long	1070881676
	.long	3763576649
	.long	1070876049
	.long	1073096258
	.long	1070870431
	.long	601221319
	.long	1070864820
	.long	2261690268
	.long	1070859216
	.long	1673619518
	.long	1070853620
	.long	3046403514
	.long	1070848031
	.long	1999843716
	.long	1070842450
	.long	2744015967
	.long	1070836876
	.long	899399508
	.long	1070831310
	.long	676744370
	.long	1070825751
	.long	1992167703
	.long	1070820199
	.long	467152011
	.long	1070814655
	.long	313445281
	.long	1070809118
	.long	1448157352
	.long	1070803588
	.long	3788725476
	.long	1070798065
	.long	2957945300
	.long	1070792550
	.long	3168871046
	.long	1070787042
	.long	44944622
	.long	1070781542
	.long	2094830420
	.long	1070776048
	.long	647642559
	.long	1070770562
	.long	4212681596
	.long	1070765082
	.long	4119694493
	.long	1070759610
	.long	288676754
	.long	1070754146
	.long	1229870791
	.long	1070748688
	.long	2568927817
	.long	1070743237
	.long	4226775419
	.long	1070737793
	.long	1829681364
	.long	1070732357
	.long	3889121191
	.long	1070726927
	.long	1737005556
	.long	1070721505
	.long	3885417029
	.long	1070716089
	.long	1666870165
	.long	1070710681
	.long	3594048319
	.long	1070705279
	.long	1000063738
	.long	1070699885
	.long	2398194399
	.long	1070694497
	.long	3417111411
	.long	1070689116
	.long	3980746691
	.long	1070683742
	.long	4013324163
	.long	1070678375
	.long	3439358267
	.long	1070673015
	.long	2183652476
	.long	1070667662
	.long	171297820
	.long	1070662316
	.long	1622638721
	.long	1070656976
	.long	2168369647
	.long	1070651643
	.long	1734435555
	.long	1070646317
	.long	247063161
	.long	1070640998
	.long	1927726804
	.long	1070635685
	.long	2408245143
	.long	1070630379
	.long	1615681634
	.long	1070625080
	.long	3772343130
	.long	1070619787
	.long	215909304
	.long	1070614502
	.long	3759169634
	.long	1070609222
	.long	1445381777
	.long	1070603950
	.long	1792910451
	.long	1070598684
	.long	870975437
	.long	1070590754
	.long	3194096222
	.long	1070580248
	.long	1823716217
	.long	1070569756
	.long	913800688
	.long	1070559277
	.long	323875369
	.long	1070548811
	.long	4208958430
	.long	1070538357
	.long	3839688678
	.long	1070527917
	.long	3372126730
	.long	1070517490
	.long	2667883253
	.long	1070507076
	.long	1589083690
	.long	1070496675
	.long	4293333010
	.long	1070486286
	.long	2053843986
	.long	1070475911
	.long	3324205749
	.long	1070465548
	.long	3673610217
	.long	1070455198
	.long	2966718784
	.long	1070444861
	.long	1068692560
	.long	1070434537
	.long	2140157205
	.long	1070424225
	.long	1752298597
	.long	1070413926
	.long	4066729595
	.long	1070403639
	.long	360651144
	.long	1070393366
	.long	3386588242
	.long	1070383104
	.long	127747308
	.long	1070372856
	.long	3337621361
	.long	1070362619
	.long	380113
	.long	1070352396
	.long	2870475188
	.long	1070342184
	.long	3227997535
	.long	1070331985
	.long	943446189
	.long	1070321799
	.long	182758681
	.long	1070311625
	.long	817374162
	.long	1070301463
	.long	2719198427
	.long	1070291313
	.long	1465634365
	.long	1070281176
	.long	1224481606
	.long	1070271051
	.long	1869032404
	.long	1070260938
	.long	3273036717
	.long	1070250837
	.long	1015732707
	.long	1070240749
	.long	3561713742
	.long	1070230672
	.long	2196155141
	.long	1070220608
	.long	1089583089
	.long	1070210556
	.long	118003300
	.long	1070200516
	.long	3452833475
	.long	1070190487
	.long	2381031992
	.long	1070180471
	.long	1074899573
	.long	1070170467
	.long	3707175295
	.long	1070160474
	.long	1566132617
	.long	1070150494
	.long	3120348377
	.long	1070140525
	.long	3953929663
	.long	1070130568
	.long	3946380940
	.long	1070120623
	.long	2977634725
	.long	1070110690
	.long	928049556
	.long	1070100769
	.long	1973375279
	.long	1070090859
	.long	1699849154
	.long	1070080961
	.long	4284063055
	.long	1070071074
	.long	1018125008
	.long	1070061200
	.long	374428300
	.long	1070051337
	.long	2235845747
	.long	1070041485
	.long	2190695046
	.long	1070031645
	.long	122671437
	.long	1070021817
	.long	210845780
	.long	1070012000
	.long	2339728060
	.long	1070002194
	.long	2099265479
	.long	1069992400
	.long	3669742467
	.long	1069982617
	.long	2641909617
	.long	1069972846
	.long	3196851002
	.long	1069963086
	.long	926113142
	.long	1069953338
	.long	11572342
	.long	1069943601
	.long	340530969
	.long	1069933875
	.long	1800682930
	.long	1069924160
	.long	4280111857
	.long	1069914456
	.long	3372322013
	.long	1069904764
	.long	3261138386
	.long	1069895083
	.long	3835803021
	.long	1069885413
	.long	690973253
	.long	1069875755
	.long	2306589128
	.long	1069866107
	.long	4278067876
	.long	1069856470
	.long	2201204065
	.long	1069846845
	.long	262102455
	.long	1069837231
	.long	2647241692
	.long	1069827627
	.long	658570708
	.long	1069818035
	.long	2778278092
	.long	1069808453
	.long	309052031
	.long	1069798883
	.long	1733817001
	.long	1069789323
	.long	2650961017
	.long	1069779774
	.long	2954203158
	.long	1069770236
	.long	2537624622
	.long	1069760709
	.long	1295667077
	.long	1069751193
	.long	3418098326
	.long	1069741687
	.long	210141498
	.long	1069732193
	.long	157244505
	.long	1069722709
	.long	3155274661
	.long	1069713235
	.long	510517079
	.long	1069703773
	.long	709476866
	.long	1069694321
	.long	3649073763
	.long	1069684879
	.long	636640577
	.long	1069675449
	.long	159725394
	.long	1069666029
	.long	2116286250
	.long	1069656619
	.long	2109656884
	.long	1069647220
	.long	38479793
	.long	1069637832
	.long	96704707
	.long	1069628454
	.long	2183652476
	.long	1069619086
	.long	1904013563
	.long	1069609729
	.long	3452748429
	.long	1069600382
	.long	2435216853
	.long	1069591046
	.long	3047045639
	.long	1069581720
	.long	894257944
	.long	1069572405
	.long	173141005
	.long	1069563100
	.long	785342784
	.long	1069553805
	.long	970708326
	.long	1069541521
	.long	2645916898
	.long	1069522972
	.long	2106590683
	.long	1069504444
	.long	3453588195
	.long	1069485936
	.long	2198475040
	.long	1069467449
	.long	2443390270
	.long	1069448982
	.long	3996141691
	.long	1069430535
	.long	2370203065
	.long	1069412109
	.long	1669613218
	.long	1069393703
	.long	1704071394
	.long	1069375317
	.long	2283901796
	.long	1069356951
	.long	3220050856
	.long	1069338605
	.long	29117219
	.long	1069320280
	.long	1113218225
	.long	1069301974
	.long	1990183441
	.long	1069283688
	.long	2473421172
	.long	1069265422
	.long	2376948508
	.long	1069247176
	.long	1515388672
	.long	1069228950
	.long	3998935692
	.long	1069210743
	.long	1053482595
	.long	1069192557
	.long	1085389880
	.long	1069174390
	.long	3911679148
	.long	1069156242
	.long	760030536
	.long	1069138115
	.long	38583924
	.long	1069120007
	.long	1566132617
	.long	1069101918
	.long	867088104
	.long	1069083849
	.long	2056379433
	.long	1069065799
	.long	659581512
	.long	1069047769
	.long	792781808
	.long	1069029758
	.long	2277675977
	.long	1069011766
	.long	641565395
	.long	1068993794
	.long	2256601
	.long	1068975841
	.long	183156961
	.long	1068957907
	.long	1008239542
	.long	1068939992
	.long	2302040698
	.long	1068922096
	.long	3889657667
	.long	1068904219
	.long	1301778891
	.long	1068886362
	.long	2954550821
	.long	1068868523
	.long	84804484
	.long	1068850704
	.long	1109791498
	.long	1068832903
	.long	1562410668
	.long	1068815121
	.long	1271074840
	.long	1068797358
	.long	64741293
	.long	1068779614
	.long	2067876735
	.long	1068761888
	.long	2815553124
	.long	1068744181
	.long	2138347284
	.long	1068726493
	.long	4162338635
	.long	1068708823
	.long	129237761
	.long	1068691173
	.long	2756122533
	.long	1068673540
	.long	3285762921
	.long	1068655926
	.long	1551389854
	.long	1068638331
	.long	1681725707
	.long	1068620754
	.long	3511047527
	.long	1068603195
	.long	2579184841
	.long	1068585655
	.long	3016419377
	.long	1068568133
	.long	363613719
	.long	1068550630
	.long	3047045639
	.long	1068533144
	.long	2313634884
	.long	1068515677
	.long	296456961
	.long	1068497513
	.long	1367328035
	.long	1068462651
	.long	3221665916
	.long	1068427825
	.long	1242258971
	.long	1068393036
	.long	3697798734
	.long	1068358282
	.long	1678104687
	.long	1068323565
	.long	3453858508
	.long	1068288883
	.long	116861598
	.long	1068254238
	.long	4234736672
	.long	1068219627
	.long	2606283437
	.long	1068185053
	.long	3506114812
	.long	1068150514
	.long	2324914545
	.long	1068116011
	.long	3044269692
	.long	1068081543
	.long	1056797460
	.long	1068047111
	.long	346010435
	.long	1068012714
	.long	601410756
	.long	1067978352
	.long	1513453498
	.long	1067944025
	.long	2773542775
	.long	1067909733
	.long	4074027864
	.long	1067875476
	.long	813232056
	.long	1067841255
	.long	1275317999
	.long	1067807068
	.long	860480111
	.long	1067772916
	.long	3559777258
	.long	1067738798
	.long	480292498
	.long	1067704716
	.long	4204867691
	.long	1067670667
	.long	1547459502
	.long	1067636654
	.long	797775935
	.long	1067602675
	.long	1656501562
	.long	1067568730
	.long	3825228429
	.long	1067534819
	.long	2711485098
	.long	1067500943
	.long	2313634884
	.long	1067467101
	.long	376973398
	.long	1067416219
	.long	672386641
	.long	1067348671
	.long	630691842
	.long	1067281191
	.long	3960777458
	.long	1067213778
	.long	1488394470
	.long	1067146434
	.long	1220920383
	.long	1067079157
	.long	2577548405
	.long	1067011947
	.long	684247743
	.long	1066944805
	.long	3553625811
	.long	1066877729
	.long	2020150230
	.long	1066810721
	.long	4099880293
	.long	1066743779
	.long	630721729
	.long	1066676905
	.long	3927125538
	.long	1066610096
	.long	535440936
	.long	1066543355
	.long	2773516144
	.long	1066476679
	.long	1486051399
	.long	1066410070
	.long	808661260
	.long	1066285262
	.long	2227658502
	.long	1066152307
	.long	1813447778
	.long	1066019484
	.long	2743640746
	.long	1065886792
	.long	3904192454
	.long	1065754231
	.long	4184355568
	.long	1065621801
	.long	2476667369
	.long	1065489502
	.long	1971904113
	.long	1065357333
	.long	3138267131
	.long	1065097372
	.long	341338964
	.long	1064833554
	.long	1954052322
	.long	1064569994
	.long	1498975029
	.long	1064306693
	.long	2190050752
	.long	1063782660
	.long	1440057502
	.long	1063257089
	.long	1432705161
	.long	1062208000
	.long	0
	.long	0
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
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
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
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
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
	.long	0
	.long	2146435072
	.long	0
	.long	4293918720
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
	.long	1072693248
	.long	0
	.long	3220176896
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
	.long	0
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
	.long	4230471070
	.long	1072693247
	.long	4054058109
	.long	1072693602
	.long	66486866
	.long	1072693958
	.long	890114735
	.long	1072694313
	.long	2230149170
	.long	1072694668
	.long	4086764978
	.long	1072695023
	.long	2165169729
	.long	1072695379
	.long	760505647
	.long	1072695735
	.long	4167915012
	.long	1072696090
	.long	3797638278
	.long	1072696446
	.long	3944817843
	.long	1072696802
	.long	314661576
	.long	1072697159
	.long	1497279289
	.long	1072697515
	.long	3197878970
	.long	1072697871
	.long	1121668664
	.long	1072698228
	.long	3858758362
	.long	1072698584
	.long	2819388932
	.long	1072698941
	.long	2298703190
	.long	1072699298
	.long	2296876715
	.long	1072699655
	.long	2814085144
	.long	1072700012
	.long	3850504175
	.long	1072700369
	.long	1111342268
	.long	1072700727
	.long	3186709833
	.long	1072701084
	.long	1486848153
	.long	1072701442
	.long	306900460
	.long	1072701800
	.long	3942010044
	.long	1072702157
	.long	3802418369
	.long	1072702515
	.long	4183268845
	.long	1072702873
	.long	789770349
	.long	1072703232
	.long	2212033708
	.long	1072703590
	.long	4155267917
	.long	1072703948
	.long	2324682034
	.long	1072704307
	.long	1015419769
	.long	1072704666
	.long	227657592
	.long	1072705025
	.long	4256539332
	.long	1072705383
	.long	217339693
	.long	1072705743
	.long	995137212
	.long	1072706102
	.long	2295141308
	.long	1072706461
	.long	4117528750
	.long	1072706820
	.long	2167509075
	.long	1072707180
	.long	740226466
	.long	1072707540
	.long	4130825173
	.long	1072707899
	.long	3749547615
	.long	1072708259
	.long	3891538155
	.long	1072708619
	.long	262006630
	.long	1072708980
	.long	1451064823
	.long	1072709340
	.long	3163922686
	.long	1072709700
	.long	1105790237
	.long	1072710061
	.long	3866779434
	.long	1072710421
	.long	2857133118
	.long	1072710782
	.long	2371996076
	.long	1072711143
	.long	2411545858
	.long	1072711504
	.long	2975960075
	.long	1072711865
	.long	4065416397
	.long	1072712226
	.long	1385125259
	.long	1072712588
	.long	3525199044
	.long	1072712949
	.long	1895881013
	.long	1072713311
	.long	792316372
	.long	1072713673
	.long	214683093
	.long	1072714035
	.long	163159208
	.long	1072714397
	.long	637922811
	.long	1072714759
	.long	1639152051
	.long	1072715121
	.long	3167025144
	.long	1072715483
	.long	926753066
	.long	1072715846
	.long	3508448741
	.long	1072716208
	.long	2322355974
	.long	1072716571
	.long	1663620514
	.long	1072716934
	.long	1532420874
	.long	1072717297
	.long	1928935631
	.long	1072717660
	.long	2853343421
	.long	1072718023
	.long	10855643
	.long	1072718387
	.long	1991585646
	.long	1072718750
	.long	205777654
	.long	1072719114
	.long	3243545138
	.long	1072719477
	.long	2515132442
	.long	1072719841
	.long	2315685863
	.long	1072720205
	.long	2645384460
	.long	1072720569
	.long	3504407352
	.long	1072720933
	.long	597966424
	.long	1072721298
	.long	2516175509
	.long	1072721662
	.long	669279317
	.long	1072722027
	.long	3647391802
	.long	1072722391
	.long	2860757796
	.long	1072722756
	.long	2604524079
	.long	1072723121
	.long	2878870196
	.long	1072723486
	.long	3683975751
	.long	1072723851
	.long	725053116
	.long	1072724217
	.long	2592216609
	.long	1072724582
	.long	695711428
	.long	1072724948
	.long	3625652011
	.long	1072725313
	.long	2792283678
	.long	1072725679
	.long	2490753696
	.long	1072726045
	.long	2721242094
	.long	1072726411
	.long	3483928965
	.long	1072726777
	.long	484027169
	.long	1072727144
	.long	2311651510
	.long	1072727510
	.long	377047673
	.long	1072727877
	.long	3270330585
	.long	1072728243
	.long	2401746054
	.long	1072728610
	.long	2066441831
	.long	1072728977
	.long	2264598438
	.long	1072729344
	.long	2996396456
	.long	1072729711
	.long	4262016525
	.long	1072730078
	.long	1766672054
	.long	1072730446
	.long	4100478400
	.long	1072730813
	.long	2673681794
	.long	1072731181
	.long	1781430419
	.long	1072731549
	.long	1423905224
	.long	1072731917
	.long	1601287218
	.long	1072732285
	.long	2313757470
	.long	1072732653
	.long	3561497114
	.long	1072733021
	.long	1049720044
	.long	1072733390
	.long	3368542109
	.long	1072733758
	.long	1928210032
	.long	1072734127
	.long	1023872485
	.long	1072734496
	.long	655710909
	.long	1072734865
	.long	823906800
	.long	1072735234
	.long	1528641723
	.long	1072735603
	.long	2770097299
	.long	1072735972
	.long	253487916
	.long	1072736342
	.long	2568929912
	.long	1072736711
	.long	1126670503
	.long	1072737081
	.long	221858853
	.long	1072737451
	.long	4149644189
	.long	1072737820
	.long	25306615
	.long	1072738191
	.long	733930073
	.long	1072738561
	.long	1980729381
	.long	1072738931
	.long	3765886716
	.long	1072739301
	.long	1794617020
	.long	1072739672
	.long	362069892
	.long	1072740043
	.long	3763394987
	.long	1072740413
	.long	3408840138
	.long	1072740784
	.long	3593555127
	.long	1072741155
	.long	22755203
	.long	1072741527
	.long	1286557569
	.long	1072741898
	.long	3090177599
	.long	1072742269
	.long	1138830730
	.long	1072742641
	.long	4022634348
	.long	1072743012
	.long	3151836718
	.long	1072743384
	.long	2821588053
	.long	1072743756
	.long	3032071334
	.long	1072744128
	.long	3783469604
	.long	1072744500
	.long	780998670
	.long	1072744873
	.long	2614776289
	.long	1072745245
	.long	695051099
	.long	1072745618
	.long	3611940982
	.long	1072745990
	.long	2775694698
	.long	1072746363
	.long	2481462955
	.long	1072746736
	.long	2729429233
	.long	1072747109
	.long	3519777067
	.long	1072747482
	.long	557722764
	.long	1072747856
	.long	2433384577
	.long	1072748229
	.long	557011640
	.long	1072748603
	.long	3518722331
	.long	1072748976
	.long	2728765908
	.long	1072749350
	.long	2482293578
	.long	1072749724
	.long	2779489315
	.long	1072750098
	.long	3620537156
	.long	1072750472
	.long	710653901
	.long	1072750847
	.long	2639958305
	.long	1072751221
	.long	818699997
	.long	1072751596
	.long	3836997855
	.long	1072751970
	.long	3105101637
	.long	1072752345
	.long	2918163045
	.long	1072752720
	.long	3276366555
	.long	1072753095
	.long	4179896701
	.long	1072753470
	.long	1333970782
	.long	1072753846
	.long	3328708054
	.long	1072754221
	.long	1574358645
	.long	1072754597
	.long	366074636
	.long	1072754973
	.long	3999008171
	.long	1072755348
	.long	3883409570
	.long	1072755724
	.long	19463802
	.long	1072756101
	.long	997290496
	.long	1072756477
	.long	2522107455
	.long	1072756853
	.long	299132543
	.long	1072757230
	.long	2918485574
	.long	1072757606
	.long	1790417244
	.long	1072757983
	.long	1210080196
	.long	1072758360
	.long	1177659842
	.long	1072758737
	.long	1693341657
	.long	1072759114
	.long	2757311177
	.long	1072759491
	.long	74786706
	.long	1072759869
	.long	2235888499
	.long	1072760246
	.long	650867689
	.long	1072760624
	.long	3909844658
	.long	1072761001
	.long	3423070662
	.long	1072761379
	.long	3485698915
	.long	1072761757
	.long	4097915390
	.long	1072762135
	.long	964938835
	.long	1072762514
	.long	2676889941
	.long	1072762892
	.long	644020286
	.long	1072763271
	.long	3456450689
	.long	1072763649
	.long	2524432850
	.long	1072764028
	.long	2143120419
	.long	1072764407
	.long	2312699817
	.long	1072764786
	.long	3033357524
	.long	1072765165
	.long	10312788
	.long	1072765545
	.long	1833686810
	.long	1072765924
	.long	4208698963
	.long	1072766303
	.long	2840568686
	.long	1072766683
	.long	2024450071
	.long	1072767063
	.long	1760529979
	.long	1072767443
	.long	2048995336
	.long	1072767823
	.long	2890033125
	.long	1072768203
	.long	4283830399
	.long	1072768583
	.long	1935606973
	.long	1072768964
	.long	140517323
	.long	1072769345
	.long	3193715983
	.long	1072769725
	.long	2505455666
	.long	1072770106
	.long	2370891033
	.long	1072770487
	.long	2790209517
	.long	1072770868
	.long	3763598611
	.long	1072771249
	.long	996278574
	.long	1072771631
	.long	3078371623
	.long	1072772012
	.long	1420130847
	.long	1072772394
	.long	316711292
	.long	1072772776
	.long	4063268064
	.long	1072773157
	.long	4070054446
	.long	1072773539
	.long	337258378
	.long	1072773922
	.long	1455002454
	.long	1072774304
	.long	3128507443
	.long	1072774686
	.long	1062994179
	.long	1072775069
	.long	3848585447
	.long	1072775451
	.long	2895534912
	.long	1072775834
	.long	2498998190
	.long	1072776217
	.long	2659163667
	.long	1072776600
	.long	3376219790
	.long	1072776983
	.long	355387776
	.long	1072777367
	.long	2186790791
	.long	1072777750
	.long	280682885
	.long	1072778134
	.long	3227187351
	.long	1072778517
	.long	2436558366
	.long	1072778901
	.long	2203952056
	.long	1072779285
	.long	2529557318
	.long	1072779669
	.long	3413563109
	.long	1072780053
	.long	561191158
	.long	1072780438
	.long	2562565143
	.long	1072780822
	.long	827939623
	.long	1072781207
	.long	3947438405
	.long	1072781591
	.long	3331316177
	.long	1072781976
	.long	3274729579
	.long	1072782361
	.long	3777868015
	.long	1072782746
	.long	545953663
	.long	1072783132
	.long	2169110651
	.long	1072783517
	.long	57593985
	.long	1072783903
	.long	2801527921
	.long	1072784288
	.long	1811167597
	.long	1072784674
	.long	1381670099
	.long	1072785060
	.long	1513225284
	.long	1072785446
	.long	2206023075
	.long	1072785832
	.long	3460253456
	.long	1072786218
	.long	981139179
	.long	1072786605
	.long	3358804952
	.long	1072786991
	.long	2003506360
	.long	1072787378
	.long	1210400941
	.long	1072787765
	.long	979679002
	.long	1072788152
	.long	1311530916
	.long	1072788539
	.long	2206147117
	.long	1072788926
	.long	3663718106
	.long	1072789313
	.long	1389467151
	.long	1072789701
	.long	3973519475
	.long	1072790088
	.long	2826131177
	.long	1072790476
	.long	2242460315
	.long	1072790864
	.long	2222697708
	.long	1072791252
	.long	2767034247
	.long	1072791640
	.long	3875660881
	.long	1072792028
	.long	1253801333
	.long	1072792417
	.long	3491581275
	.long	1072792805
	.long	1999257261
	.long	1072793194
	.long	1071987800
	.long	1072793583
	.long	709964165
	.long	1072793972
	.long	913377697
	.long	1072794361
	.long	1682419801
	.long	1072794750
	.long	3017281948
	.long	1072795139
	.long	623188375
	.long	1072795529
	.long	3090265274
	.long	1072795918
	.long	1828769718
	.long	1072796308
	.long	1133860732
	.long	1072796698
	.long	1005730110
	.long	1072797088
	.long	1444569709
	.long	1072797478
	.long	2450571457
	.long	1072797868
	.long	4023927340
	.long	1072798258
	.long	1869862117
	.long	1072798649
	.long	283535203
	.long	1072799040
	.long	3560106076
	.long	1072799430
	.long	3109832396
	.long	1072799821
	.long	3227873773
	.long	1072800212
	.long	3914422583
	.long	1072800603
	.long	874703977
	.long	1072800995
	.long	2698845057
	.long	1072801386
	.long	797103804
	.long	1072801778
	.long	3759607450
	.long	1072802169
	.long	2996614109
	.long	1072802561
	.long	2803283846
	.long	1072802953
	.long	3179809498
	.long	1072803345
	.long	4126383965
	.long	1072803737
	.long	1348232917
	.long	1072804130
	.long	3435483979
	.long	1072804522
	.long	1798395653
	.long	1072804915
	.long	732128398
	.long	1072805308
	.long	236875443
	.long	1072805701
	.long	312830080
	.long	1072806094
	.long	960185668
	.long	1072806487
	.long	2179135630
	.long	1072806880
	.long	3969873455
	.long	1072807273
	.long	2037625403
	.long	1072807667
	.long	677552389
	.long	1072808061
	.long	4184815396
	.long	1072808454
	.long	3969673583
	.long	1072808848
	.long	32320766
	.long	1072809243
	.long	962885422
	.long	1072809637
	.long	2466594201
	.long	1072810031
	.long	248673822
	.long	1072810426
	.long	2899252956
	.long	1072810820
	.long	1828591156
	.long	1072811215
	.long	1331849931
	.long	1072811610
	.long	1409223555
	.long	1072812005
	.long	2060906373
	.long	1072812400
	.long	3287092791
	.long	1072812795
	.long	793009988
	.long	1072813191
	.long	3168787097
	.long	1072813586
	.long	1824684131
	.long	1072813982
	.long	1055863057
	.long	1072814378
	.long	862518614
	.long	1072814774
	.long	1244845604
	.long	1072815170
	.long	2203038896
	.long	1072815566
	.long	3737293427
	.long	1072815962
	.long	1552836903
	.long	1072816359
	.long	4239798981
	.long	1072816755
	.long	3208440203
	.long	1072817152
	.long	2753923066
	.long	1072817549
	.long	2876442833
	.long	1072817946
	.long	3576194839
	.long	1072818343
	.long	558407182
	.long	1072818741
	.long	2413209922
	.long	1072819138
	.long	550863994
	.long	1072819536
	.long	3561499587
	.long	1072819933
	.long	2855377771
	.long	1072820331
	.long	2727661572
	.long	1072820729
	.long	3178546784
	.long	1072821127
	.long	4208229269
	.long	1072821525
	.long	1521937658
	.long	1072821924
	.long	3709802537
	.long	1072822322
	.long	2182085374
	.long	1072822721
	.long	1233949591
	.long	1072823120
	.long	865591383
	.long	1072823519
	.long	1077207007
	.long	1072823918
	.long	1868992789
	.long	1072824317
	.long	3241145124
	.long	1072824716
	.long	898893172
	.long	1072825116
	.long	3432368052
	.long	1072825515
	.long	2251831762
	.long	1072825915
	.long	1652448257
	.long	1072826315
	.long	1634414263
	.long	1072826715
	.long	2197926570
	.long	1072827115
	.long	3343182037
	.long	1072827515
	.long	775410292
	.long	1072827916
	.long	3084742920
	.long	1072828316
	.long	1681442385
	.long	1072828717
	.long	860673109
	.long	1072829118
	.long	622632283
	.long	1072829519
	.long	967517166
	.long	1072829920
	.long	1895525083
	.long	1072830321
	.long	3406853425
	.long	1072830722
	.long	1206732355
	.long	1072831124
	.long	3885293991
	.long	1072831525
	.long	2852801332
	.long	1072831927
	.long	2404419335
	.long	1072832329
	.long	2540345726
	.long	1072832731
	.long	3260778297
	.long	1072833133
	.long	270947612
	.long	1072833536
	.long	2160986190
	.long	1072833938
	.long	341157435
	.long	1072834341
	.long	3401593998
	.long	1072834743
	.long	2752559416
	.long	1072835146
	.long	2689219179
	.long	1072835549
	.long	3211771552
	.long	1072835952
	.long	25447565
	.long	1072836356
	.long	1720380209
	.long	1072836759
	.long	4001800650
	.long	1072837162
	.long	2574940123
	.long	1072837566
	.long	1734964523
	.long	1072837970
	.long	1482072516
	.long	1072838374
	.long	1816462831
	.long	1072838778
	.long	2738334272
	.long	1072839182
	.long	4247885705
	.long	1072839586
	.long	2050348766
	.long	1072839991
	.long	440889755
	.long	1072840396
	.long	3714675036
	.long	1072840800
	.long	3281969154
	.long	1072841205
	.long	3437938610
	.long	1072841610
	.long	4182782673
	.long	1072842015
	.long	1221733388
	.long	1072842421
	.long	3144924752
	.long	1072842826
	.long	1362621647
	.long	1072843232
	.long	169990911
	.long	1072843638
	.long	3862199448
	.long	1072844043
	.long	3849512343
	.long	1072844449
	.long	132129338
	.long	1072844856
	.long	1300184840
	.long	1072845262
	.long	3058911430
	.long	1072845668
	.long	1113541760
	.long	1072846075
	.long	4054210436
	.long	1072846481
	.long	3291182947
	.long	1072846888
	.long	3119626743
	.long	1072847295
	.long	3539742037
	.long	1072847702
	.long	256761821
	.long	1072848110
	.long	1860821041
	.long	1072848517
	.long	4057152821
	.long	1072848924
	.long	2550990355
	.long	1072849332
	.long	1637501494
	.long	1072849740
	.long	1316886866
	.long	1072850148
	.long	1589347160
	.long	1072850556
	.long	2455083139
	.long	1072850964
	.long	3914295631
	.long	1072851372
	.long	1672218237
	.long	1072851781
	.long	24019217
	.long	1072852190
	.long	3264866899
	.long	1072852598
	.long	2805027793
	.long	1072853007
	.long	2939670363
	.long	1072853416
	.long	3668995847
	.long	1072853825
	.long	698238252
	.long	1072854235
	.long	2617533544
	.long	1072854644
	.long	837148572
	.long	1072855054
	.long	3947219439
	.long	1072855463
	.long	3358013128
	.long	1072855873
	.long	3364698584
	.long	1072856283
	.long	3967477519
	.long	1072856693
	.long	871584420
	.long	1072857104
	.long	2667155729
	.long	1072857514
	.long	764458774
	.long	1072857925
	.long	3753630135
	.long	1072858335
	.long	3044937274
	.long	1072858746
	.long	2933549613
	.long	1072859157
	.long	3419669343
	.long	1072859568
	.long	208531430
	.long	1072859980
	.long	1890272795
	.long	1072860391
	.long	4170128542
	.long	1072860802
	.long	2753333840
	.long	1072861214
	.long	1935058519
	.long	1072861626
	.long	1715505184
	.long	1072862038
	.long	2094876506
	.long	1072862450
	.long	3073375228
	.long	1072862862
	.long	356236861
	.long	1072863275
	.long	2533598878
	.long	1072863687
	.long	1015729630
	.long	1072864100
	.long	97799432
	.long	1072864513
	.long	4074978662
	.long	1072864925
	.long	62568587
	.long	1072865339
	.long	945674314
	.long	1072865752
	.long	2429531839
	.long	1072866165
	.long	219377225
	.long	1072866579
	.long	2905348493
	.long	1072866992
	.long	1897714548
	.long	1072867406
	.long	1491646249
	.long	1072867820
	.long	1687347236
	.long	1072868234
	.long	2485021208
	.long	1072868648
	.long	3884871940
	.long	1072869062
	.long	1592135977
	.long	1072869477
	.long	4196951822
	.long	1072869891
	.long	3109588863
	.long	1072870306
	.long	2625218444
	.long	1072870721
	.long	2744044683
	.long	1072871136
	.long	3466271768
	.long	1072871551
	.long	497136658
	.long	1072871967
	.long	2426778270
	.long	1072872382
	.long	665466407
	.long	1072872798
	.long	3803340124
	.long	1072873213
	.long	3250669363
	.long	1072873629
	.long	3302626020
	.long	1072874045
	.long	3959414768
	.long	1072874461
	.long	926273050
	.long	1072874878
	.long	2793340269
	.long	1072875294
	.long	970886712
	.long	1072875711
	.long	4049051919
	.long	1072876127
	.long	3438106317
	.long	1072876544
	.long	3433222287
	.long	1072876961
	.long	4034604989
	.long	1072877378
	.long	947492350
	.long	1072877796
	.long	2762024260
	.long	1072878213
	.long	888471491
	.long	1072878631
	.long	3916974070
	.long	1072879048
	.long	3257802909
	.long	1072879466
	.long	3206130877
	.long	1072879884
	.long	3762163619
	.long	1072880302
	.long	631139552
	.long	1072880721
	.long	2403199048
	.long	1072881139
	.long	488613369
	.long	1072881558
	.long	3477523031
	.long	1072881976
	.long	2780199430
	.long	1072882395
	.long	2691815924
	.long	1072882814
	.long	3212578647
	.long	1072883233
	.long	47726500
	.long	1072883653
	.long	1787400350
	.long	1072884072
	.long	4136839237
	.long	1072884491
	.long	2801282279
	.long	1072884911
	.long	2075903250
	.long	1072885331
	.long	1960908701
	.long	1072885751
	.long	2456505253
	.long	1072886171
	.long	3562899594
	.long	1072886591
	.long	985331189
	.long	1072887012
	.long	3313941461
	.long	1072887432
	.long	1959002716
	.long	1072887853
	.long	1215689221
	.long	1072888274
	.long	1084208016
	.long	1072888695
	.long	1564766211
	.long	1072889116
	.long	2657570987
	.long	1072889537
	.long	67862297
	.long	1072889959
	.long	2385782055
	.long	1072890380
	.long	1021603060
	.long	1072890802
	.long	270500067
	.long	1072891224
	.long	132680608
	.long	1072891646
	.long	608352285
	.long	1072892068
	.long	1697722769
	.long	1072892490
	.long	3400999802
	.long	1072892912
	.long	1423423901
	.long	1072893335
	.long	60170245
	.long	1072893758
	.long	3606414082
	.long	1072894180
	.long	3472428843
	.long	1072894603
	.long	3953389919
	.long	1072895026
	.long	754538178
	.long	1072895450
	.long	2466016447
	.long	1072895873
	.long	498098438
	.long	1072896297
	.long	3440927120
	.long	1072896720
	.long	2704776347
	.long	1072897144
	.long	2584821931
	.long	1072897568
	.long	3081272460
	.long	1072897992
	.long	4194336593
	.long	1072898416
	.long	1629255761
	.long	1072898841
	.long	3976173357
	.long	1072899265
	.long	2645363658
	.long	1072899690
	.long	1932002900
	.long	1072900115
	.long	1836300097
	.long	1072900540
	.long	2358464328
	.long	1072900965
	.long	3498704748
	.long	1072901390
	.long	962263284
	.long	1072901816
	.long	3339283823
	.long	1072902241
	.long	2040041138
	.long	1072902667
	.long	1359711962
	.long	1072903093
	.long	1298505802
	.long	1072903519
	.long	1856632235
	.long	1072903945
	.long	3034300912
	.long	1072904371
	.long	536754255
	.long	1072904798
	.long	2954136649
	.long	1072905224
	.long	1696723363
	.long	1072905651
	.long	1059691625
	.long	1072906078
	.long	1043251442
	.long	1072906505
	.long	1647612887
	.long	1072906932
	.long	2872986106
	.long	1072907359
	.long	424614023
	.long	1072907787
	.long	2892641516
	.long	1072908214
	.long	1687344354
	.long	1072908642
	.long	1103900263
	.long	1072909070
	.long	1142519748
	.long	1072909498
	.long	1803413380
	.long	1072909926
	.long	3086791805
	.long	1072910354
	.long	697898441
	.long	1072910783
	.long	3226878671
	.long	1072911211
	.long	2084008759
	.long	1072911640
	.long	1564466932
	.long	1072912069
	.long	1668464192
	.long	1072912498
	.long	2396211611
	.long	1072912927
	.long	3747920335
	.long	1072913356
	.long	1428834282
	.long	1072913786
	.long	4029099333
	.long	1072914215
	.long	2958992255
	.long	1072914645
	.long	2513691773
	.long	1072915075
	.long	2693409391
	.long	1072915505
	.long	3498356681
	.long	1072915935
	.long	633777992
	.long	1072916366
	.long	2689819635
	.long	1072916796
	.long	1076758805
	.long	1072917227
	.long	89774659
	.long	1072917658
	.long	4024046424
	.long	1072918088
	.long	4289851512
	.long	1072918519
	.long	887402001
	.long	1072918951
	.long	2406844627
	.long	1072919382
	.long	258457017
	.long	1072919814
	.long	3032386058
	.long	1072920245
	.long	2138909516
	.long	1072920677
	.long	1873207124
	.long	1072921109
	.long	2235491389
	.long	1072921541
	.long	3225974889
	.long	1072921973
	.long	549902978
	.long	1072922406
	.long	2797422972
	.long	1072922838
	.long	1378813070
	.long	1072923271
	.long	589253439
	.long	1072923704
	.long	428957015
	.long	1072924137
	.long	898136809
	.long	1072924570
	.long	1997005904
	.long	1072925003
	.long	3725777455
	.long	1072925436
	.long	1789697393
	.long	1072925870
	.long	483946312
	.long	1072926304
	.long	4103704882
	.long	1072926737
	.long	4059251953
	.long	1072927171
	.long	350801040
	.long	1072927606
	.long	1568500325
	.long	1072928040
	.long	3417596172
	.long	1072928474
	.long	1603335018
	.long	1072928909
	.long	420897964
	.long	1072929344
	.long	4165466183
	.long	1072929778
	.long	4247319032
	.long	1072930213
	.long	666670536
	.long	1072930649
	.long	2013669380
	.long	1072931084
	.long	3993562437
	.long	1072931519
	.long	2311596648
	.long	1072931955
	.long	1262953624
	.long	1072932391
	.long	847847748
	.long	1072932827
	.long	1066493478
	.long	1072933263
	.long	1919105345
	.long	1072933699
	.long	3405897950
	.long	1072934135
	.long	1232118674
	.long	1072934572
	.long	3987916855
	.long	1072935008
	.long	3083572723
	.long	1072935445
	.long	2814268468
	.long	1072935882
	.long	3180219054
	.long	1072936319
	.long	4181639523
	.long	1072936756
	.long	1523777687
	.long	1072937194
	.long	3796783325
	.long	1072937631
	.long	2410937101
	.long	1072938069
	.long	1661421642
	.long	1072938507
	.long	1548452351
	.long	1072938945
	.long	2072244704
	.long	1072939383
	.long	3233014250
	.long	1072939821
	.long	736009314
	.long	1072940260
	.long	3171380184
	.long	1072940698
	.long	1949408036
	.long	1072941137
	.long	1365276005
	.long	1072941576
	.long	1419200007
	.long	1072942015
	.long	2111396029
	.long	1072942454
	.long	3442080132
	.long	1072942893
	.long	1116501150
	.long	1072943333
	.long	3724809886
	.long	1072943772
	.long	2677288023
	.long	1072944212
	.long	2269119214
	.long	1072944652
	.long	2500519883
	.long	1072945092
	.long	3371706530
	.long	1072945532
	.long	587928431
	.long	1072945973
	.long	2739336825
	.long	1072946413
	.long	1236213839
	.long	1072946854
	.long	373743562
	.long	1072947295
	.long	152142860
	.long	1072947736
	.long	571628671
	.long	1072948177
	.long	1632418011
	.long	1072948618
	.long	3334727963
	.long	1072949059
	.long	1383808394
	.long	1072949501
	.long	74843831
	.long	1072949943
	.long	3703018878
	.long	1072950384
	.long	3678616324
	.long	1072950826
	.long	1853621
	.long	1072951269
	.long	1262882892
	.long	1072951711
	.long	3166954440
	.long	1072952153
	.long	1419318643
	.long	1072952596
	.long	315160549
	.long	1072953039
	.long	4149665274
	.long	1072953481
	.long	38148827
	.long	1072953925
	.long	865731065
	.long	1072954368
	.long	2337662736
	.long	1072954811
	.long	159194662
	.long	1072955255
	.long	2920479626
	.long	1072955698
	.long	2031801301
	.long	1072956142
	.long	1788345322
	.long	1072956586
	.long	2190330102
	.long	1072957030
	.long	3237974129
	.long	1072957474
	.long	636528667
	.long	1072957919
	.long	2976146942
	.long	1072958363
	.long	1667113072
	.long	1072958808
	.long	1004613135
	.long	1072959253
	.long	988865989
	.long	1072959698
	.long	1620090564
	.long	1072960143
	.long	2898505866
	.long	1072960588
	.long	529363677
	.long	1072961034
	.long	3102817744
	.long	1072961479
	.long	2029152702
	.long	1072961925
	.long	1603555148
	.long	1072962371
	.long	1826244460
	.long	1072962817
	.long	2697440087
	.long	1072963263
	.long	4217361553
	.long	1072963709
	.long	2091261162
	.long	1072964156
	.long	614325883
	.long	1072964603
	.long	4081742759
	.long	1072965049
	.long	3903797021
	.long	1072965496
	.long	80708565
	.long	1072965944
	.long	1202631953
	.long	1072966391
	.long	2974819937
	.long	1072966838
	.long	1102525337
	.long	1072967286
	.long	4175902942
	.long	1072967733
	.long	3605238428
	.long	1072968181
	.long	3685719435
	.long	1072968629
	.long	122599084
	.long	1072969078
	.long	1506032460
	.long	1072969526
	.long	3541272834
	.long	1072969974
	.long	1933573553
	.long	1072970423
	.long	978122628
	.long	1072970872
	.long	675140853
	.long	1072971321
	.long	1024849091
	.long	1072971770
	.long	2027468284
	.long	1072972219
	.long	3683219449
	.long	1072972668
	.long	1697356377
	.long	1072973118
	.long	365067534
	.long	1072973568
	.long	3981541451
	.long	1072974017
	.long	3957064855
	.long	1072974467
	.long	291859135
	.long	1072974918
	.long	1576080346
	.long	1072975368
	.long	3514982734
	.long	1072975818
	.long	1813820617
	.long	1072976269
	.long	767782981
	.long	1072976720
	.long	377091592
	.long	1072977171
	.long	641968288
	.long	1072977622
	.long	1562634985
	.long	1072978073
	.long	3139313673
	.long	1072978524
	.long	1077259122
	.long	1072978976
	.long	3966628063
	.long	1072979427
	.long	3217708121
	.long	1072979879
	.long	3125688884
	.long	1072980331
	.long	3690792716
	.long	1072980783
	.long	618274764
	.long	1072981236
	.long	2498292134
	.long	1072981688
	.long	741132830
	.long	1072982141
	.long	3936954108
	.long	1072982593
	.long	3496044122
	.long	1072983046
	.long	3713592984
	.long	1072983499
	.long	294856292
	.long	1072983953
	.long	1829991607
	.long	1072984406
	.long	4024254678
	.long	1072984859
	.long	2582901329
	.long	1072985313
	.long	1801122051
	.long	1072985767
	.long	1679140116
	.long	1072986221
	.long	2217178870
	.long	1072986675
	.long	3415461735
	.long	1072987129
	.long	979244914
	.long	1072987584
	.long	3498686573
	.long	1072988038
	.long	2384075768
	.long	1072988493
	.long	1930603520
	.long	1072988948
	.long	2138493630
	.long	1072989403
	.long	3007969975
	.long	1072989858
	.long	244289211
	.long	1072990314
	.long	2437609958
	.long	1072990769
	.long	998221727
	.long	1072991225
	.long	221315995
	.long	1072991681
	.long	107117018
	.long	1072992137
	.long	655849126
	.long	1072992593
	.long	1867736728
	.long	1072993049
	.long	3743004307
	.long	1072993505
	.long	1986909126
	.long	1072993962
	.long	894643117
	.long	1072994419
	.long	466430991
	.long	1072994876
	.long	702497536
	.long	1072995333
	.long	1603067616
	.long	1072995790
	.long	3168366170
	.long	1072996247
	.long	1103650918
	.long	1072996705
	.long	3999081545
	.long	1072997162
	.long	3264948627
	.long	1072997620
	.long	3196444704
	.long	1072998078
	.long	3793795097
	.long	1072998536
	.long	762257908
	.long	1072998995
	.long	2691993199
	.long	1072999453
	.long	993291932
	.long	1072999912
	.long	4256314324
	.long	1073000370
	.long	3891351483
	.long	1073000829
	.long	4193596486
	.long	1073001288
	.long	868307891
	.long	1073001748
	.long	2505646223
	.long	1073002207
	.long	515902897
	.long	1073002667
	.long	3489238590
	.long	1073003126
	.long	2835944871
	.long	1073003586
	.long	2851215274
	.long	1073004046
	.long	3535276114
	.long	1073004506
	.long	593386486
	.long	1073004967
	.long	2615707447
	.long	1073005427
	.long	1012530953
	.long	1073005888
	.long	79050918
	.long	1073006349
	.long	4110461337
	.long	1073006809
	.long	222087097
	.long	1073007271
	.long	1299056934
	.long	1073007732
	.long	3046630484
	.long	1073008193
	.long	1170067454
	.long	1073008655
	.long	4259529517
	.long	1073009116
	.long	3725309241
	.long	1073009578
	.long	3862601154
	.long	1073010040
	.long	376665277
	.long	1073010503
	.long	1857663588
	.long	1073010965
	.long	4010856258
	.long	1073011427
	.long	2541503535
	.long	1073011890
	.long	1744800334
	.long	1073012353
	.long	1620974353
	.long	1073012816
	.long	2170253366
	.long	1073013279
	.long	3392865225
	.long	1073013742
	.long	994070562
	.long	1073014206
	.long	3564031974
	.long	1073014669
	.long	2513042955
	.long	1073015133
	.long	2136298958
	.long	1073015597
	.long	2434028221
	.long	1073016061
	.long	3406459059
	.long	1073016525
	.long	758852568
	.long	1073016990
	.long	3081371809
	.long	1073017454
	.long	1784310738
	.long	1073017919
	.long	1162865273
	.long	1073018384
	.long	1217264116
	.long	1073018849
	.long	1947736046
	.long	1073019314
	.long	3354509918
	.long	1073019779
	.long	1142847371
	.long	1073020245
	.long	3902912008
	.long	1073020710
	.long	3044998326
	.long	1073021176
	.long	2864302785
	.long	1073021642
	.long	3361054631
	.long	1073022108
	.long	240515889
	.long	1073022575
	.long	2092850551
	.long	1073023041
	.long	328353499
	.long	1073023508
	.long	3537188882
	.long	1073023974
	.long	3129651738
	.long	1073024441
	.long	3400939074
	.long	1073024908
	.long	56313381
	.long	1073025376
	.long	1685939117
	.long	1073025843
	.long	3995078930
	.long	1073026310
	.long	2688995542
	.long	1073026778
	.long	2062886350
	.long	1073027246
	.long	2116981530
	.long	1073027714
	.long	2851511338
	.long	1073028182
	.long	4266706105
	.long	1073028650
	.long	2067828946
	.long	1073029119
	.long	550077646
	.long	1073029588
	.long	4008650067
	.long	1073030056
	.long	3853842263
	.long	1073030525
	.long	85884954
	.long	1073030995
	.long	1294943536
	.long	1073031464
	.long	3186281590
	.long	1073031933
	.long	1465162777
	.long	1073032403
	.long	426785429
	.long	1073032873
	.long	71380660
	.long	1073033343
	.long	399179661
	.long	1073033813
	.long	1410413701
	.long	1073034283
	.long	3105314129
	.long	1073034753
	.long	1189145077
	.long	1073035224
	.long	4252072640
	.long	1073035694
	.long	3704393811
	.long	1073036165
	.long	3841307546
	.long	1073036636
	.long	368078290
	.long	1073037108
	.long	1874872453
	.long	1073037579
	.long	4066954636
	.long	1073038050
	.long	2649589519
	.long	1073038522
	.long	1917976452
	.long	1073038994
	.long	1872347569
	.long	1073039466
	.long	2512935078
	.long	1073039938
	.long	3839971271
	.long	1073040410
	.long	1558721219
	.long	1073040883
	.long	4259351963
	.long	1073041355
	.long	3352161436
	.long	1073041828
	.long	3132349536
	.long	1073042301
	.long	3600148950
	.long	1073042774
	.long	460825142
	.long	1073043248
	.long	2304545545
	.long	1073043721
	.long	541608488
	.long	1073044195
	.long	3762181559
	.long	1073044668
	.long	3376563243
	.long	1073045142
	.long	3679953995
	.long	1073045616
	.long	377619753
	.long	1073046091
	.long	2059728421
	.long	1073046565
	.long	136578804
	.long	1073047040
	.long	3198338962
	.long	1073047514
	.long	2655307857
	.long	1073047989
	.long	2802686414
	.long	1073048464
	.long	3640708341
	.long	1073048939
	.long	874640132
	.long	1073049415
	.long	3094650247
	.long	1073049890
	.long	1711038040
	.long	1073050366
	.long	1019004832
	.long	1073050842
	.long	1018784728
	.long	1073051318
	.long	1710611914
	.long	1073051794
	.long	3094720652
	.long	1073052270
	.long	876377990
	.long	1073052747
	.long	3645752941
	.long	1073053223
	.long	2813145416
	.long	1073053700
	.long	2673757290
	.long	1073054177
	.long	3227823225
	.long	1073054654
	.long	180610665
	.long	1073055132
	.long	2122289020
	.long	1073055609
	.long	463158596
	.long	1073056087
	.long	3793388967
	.long	1073056564
	.long	3523280594
	.long	1073057042
	.long	3948035914
	.long	1073057520
	.long	772922846
	.long	1073057999
	.long	2588111279
	.long	1073058477
	.long	803901998
	.long	1073058956
	.long	4010465051
	.long	1073059434
	.long	3618101379
	.long	1073059913
	.long	3922013897
	.long	1073060392
	.long	627471001
	.long	1073060872
	.long	2324643060
	.long	1073061351
	.long	423831335
	.long	1073061831
	.long	3515206355
	.long	1073062310
	.long	3009069540
	.long	1073062790
	.long	3200624281
	.long	1073063270
	.long	4090106753
	.long	1073063750
	.long	1382785912
	.long	1073064231
	.long	3668832687
	.long	1073064711
	.long	2358548899
	.long	1073065192
	.long	1747138337
	.long	1073065673
	.long	1834837577
	.long	1073066154
	.long	2621883273
	.long	1073066635
	.long	4108512157
	.long	1073067116
	.long	1999993751
	.long	1073067598
	.long	591532243
	.long	1073068080
	.long	4178331906
	.long	1073068561
	.long	4170695201
	.long	1073069043
	.long	568859265
	.long	1073069526
	.long	1962995905
	.long	1073070008
	.long	4058375122
	.long	1073070490
	.long	2560266994
	.long	1073070973
	.long	1763876276
	.long	1073071456
	.long	1669440503
	.long	1073071939
	.long	2277197293
	.long	1073072422
	.long	3587384346
	.long	1073072905
	.long	1305272140
	.long	1073073389
	.long	4021033130
	.long	1073073872
	.long	3144970662
	.long	1073074356
	.long	2972290050
	.long	1073074840
	.long	3503229396
	.long	1073075324
	.long	443059585
	.long	1073075809
	.long	2381953471
	.long	1073076293
	.long	730214803
	.long	1073076778
	.long	4078016597
	.long	1073077262
	.long	3835662765
	.long	1073077747
	.long	3391891
	.long	1073078233
	.long	1171377231
	.long	1073078718
	.long	3044890237
	.long	1073079203
	.long	1329202438
	.long	1073079689
	.long	319520039
	.long	1073080175
	.long	16082029
	.long	1073080661
	.long	419127475
	.long	1073081147
	.long	1528895530
	.long	1073081633
	.long	3345625422
	.long	1073082119
	.long	1574589172
	.long	1073082606
	.long	510993465
	.long	1073083093
	.long	155077776
	.long	1073083580
	.long	507081663
	.long	1073084067
	.long	1567244761
	.long	1073084554
	.long	3335806787
	.long	1073085041
	.long	1518040244
	.long	1073085529
	.long	409152307
	.long	1073086017
	.long	9382939
	.long	1073086505
	.long	318972182
	.long	1073086993
	.long	1338160159
	.long	1073087481
	.long	3067187074
	.long	1073087969
	.long	1211325919
	.long	1073088458
	.long	65784356
	.long	1073088947
	.long	3925770131
	.long	1073089435
	.long	4201589183
	.long	1073089924
	.long	893482122
	.long	1073090414
	.long	2591624233
	.long	1073090903
	.long	706321701
	.long	1073091393
	.long	3827749971
	.long	1073091882
	.long	3366215391
	.long	1073092372
	.long	3616926276
	.long	1073092862
	.long	285156430
	.long	1073093353
	.long	1961081627
	.long	1073093843
	.long	55008540
	.long	1073094334
	.long	3157113108
	.long	1073094824
	.long	2677702164
	.long	1073095315
	.long	2911984515
	.long	1073095806
	.long	3860201750
	.long	1073096297
	.long	1227628247
	.long	1073096789
	.long	3604440353
	.long	1073097280
	.long	2400945312
	.long	1073097772
	.long	1912352338
	.long	1073098264
	.long	2138903430
	.long	1073098756
	.long	3080840673
	.long	1073099248
	.long	443438931
	.long	1073099741
	.long	2816875045
	.long	1073100233
	.long	1611456749
	.long	1073100726
	.long	1122393751
	.long	1073101219
	.long	1349928543
	.long	1073101712
	.long	2294303696
	.long	1073102205
	.long	3955761869
	.long	1073102698
	.long	2039578504
	.long	1073103192
	.long	840963717
	.long	1073103686
	.long	360160411
	.long	1073104180
	.long	597411571
	.long	1073104674
	.long	1552960265
	.long	1073105168
	.long	3227049641
	.long	1073105662
	.long	1324955635
	.long	1073106157
	.long	141888859
	.long	1073106652
	.long	3973060003
	.long	1073107146
	.long	4228777955
	.long	1073107641
	.long	909286277
	.long	1073108137
	.long	2604763204
	.long	1073108632
	.long	725517870
	.long	1073109128
	.long	3861728676
	.long	1073109623
	.long	3423704921
	.long	1073110119
	.long	3706657876
	.long	1073110615
	.long	415864301
	.long	1073111112
	.long	2141502926
	.long	1073111608
	.long	293883382
	.long	1073112105
	.long	3463184564
	.long	1073112601
	.long	3059716268
	.long	1073113098
	.long	3378690259
	.long	1073113595
	.long	125383795
	.long	1073114093
	.long	1889976102
	.long	1073114590
	.long	82777307
	.long	1073115088
	.long	3293966802
	.long	1073115585
	.long	2933854879
	.long	1073116083
	.long	3297653802
	.long	1073116581
	.long	90641322
	.long	1073117080
	.long	1902997167
	.long	1073117578
	.long	145031958
	.long	1073118077
	.long	3406925586
	.long	1073118575
	.long	3098988842
	.long	1073119074
	.long	3516434485
	.long	1073119573
	.long	364540769
	.long	1073120073
	.long	2233487914
	.long	1073120572
	.long	533587044
	.long	1073121072
	.long	3855018547
	.long	1073121571
	.long	3608093714
	.long	1073122071
	.long	4088025801
	.long	1073122571
	.long	1000093561
	.long	1073123072
	.long	2934477717
	.long	1073123572
	.long	1301489888
	.long	1073124073
	.long	396343670
	.long	1073124574
	.long	219285440
	.long	1073125075
	.long	770561665
	.long	1073125576
	.long	2050418889
	.long	1073126077
	.long	4059103743
	.long	1073126578
	.long	2501895648
	.long	1073127080
	.long	1674008694
	.long	1073127582
	.long	1575689763
	.long	1073128084
	.long	2207185821
	.long	1073128586
	.long	3568743916
	.long	1073129088
	.long	1365643882
	.long	1073129591
	.long	4188067528
	.long	1073130093
	.long	3446327562
	.long	1073130596
	.long	3435638662
	.long	1073131099
	.long	4156248294
	.long	1073131602
	.long	1313436713
	.long	1073132106
	.long	3497386145
	.long	1073132609
	.long	2118409717
	.long	1073133113
	.long	1471722526
	.long	1073133617
	.long	1557572458
	.long	1073134121
	.long	2376207482
	.long	1073134625
	.long	3927875654
	.long	1073135129
	.long	1917857814
	.long	1073135634
	.long	641369479
	.long	1073136139
	.long	98658955
	.long	1073136644
	.long	289974633
	.long	1073137149
	.long	1215564985
	.long	1073137654
	.long	2875678570
	.long	1073138159
	.long	975596734
	.long	1073138665
	.long	4105502795
	.long	1073139170
	.long	3675710971
	.long	1073139676
	.long	3981437453
	.long	1073140182
	.long	727963924
	.long	1073140689
	.long	2505474039
	.long	1073141195
	.long	724282353
	.long	1073141702
	.long	3974572691
	.long	1073142208
	.long	3666659776
	.long	1073142715
	.long	4095760304
	.long	1073143222
	.long	967156467
	.long	1073143730
	.long	2871032423
	.long	1073144237
	.long	1217703235
	.long	1073144745
	.long	302385939
	.long	1073145253
	.long	125330356
	.long	1073145761
	.long	686786394
	.long	1073146269
	.long	1987004045
	.long	1073146777
	.long	4026233386
	.long	1073147285
	.long	2509757284
	.long	1073147794
	.long	1732793279
	.long	1073148303
	.long	1695591702
	.long	1073148812
	.long	2398402970
	.long	1073149321
	.long	3841477581
	.long	1073149830
	.long	1730098826
	.long	1073150340
	.long	359484670
	.long	1073150850
	.long	4024853164
	.long	1073151359
	.long	4136520556
	.long	1073151869
	.long	694737771
	.long	1073152380
	.long	2289690409
	.long	1073152890
	.long	331694973
	.long	1073153401
	.long	3410937236
	.long	1073153911
	.long	2937733868
	.long	1073154422
	.long	3207303516
	.long	1073154933
	.long	4219897616
	.long	1073155444
	.long	1680800389
	.long	1073155956
	.long	4180198033
	.long	1073156467
	.long	3128407647
	.long	1073156979
	.long	2820648301
	.long	1073157491
	.long	3257171856
	.long	1073158003
	.long	143262962
	.long	1073158516
	.long	2069108242
	.long	1073159028
	.long	445025220
	.long	1073159541
	.long	3861200690
	.long	1073160053
	.long	3727952347
	.long	1073160566
	.long	45532563
	.long	1073161080
	.long	1404128388
	.long	1073161593
	.long	3509025071
	.long	1073162106
	.long	2065507943
	.long	1073162620
	.long	1368797014
	.long	1073163134
	.long	1419145086
	.long	1073163648
	.long	2216805043
	.long	1073164162
	.long	3762029856
	.long	1073164676
	.long	1760105286
	.long	1073165191
	.long	506251772
	.long	1073165706
	.long	722541
	.long	1073166221
	.long	243770908
	.long	1073166736
	.long	1235650271
	.long	1073167251
	.long	2976614117
	.long	1073167766
	.long	1171948721
	.long	1073168282
	.long	116875034
	.long	1073168798
	.long	4106614097
	.long	1073169313
	.long	256517850
	.long	1073169830
	.long	1451742095
	.long	1073170346
	.long	3397573537
	.long	1073170862
	.long	1799298968
	.long	1073171379
	.long	952139854
	.long	1073171896
	.long	856350455
	.long	1073172413
	.long	1512185116
	.long	1073172930
	.long	2919898267
	.long	1073173447
	.long	784777130
	.long	1073173965
	.long	3697010899
	.long	1073174482
	.long	3066919672
	.long	1073175000
	.long	3189725520
	.long	1073175518
	.long	4065683305
	.long	1073176036
	.long	1400080679
	.long	1073176555
	.long	3783107268
	.long	1073177073
	.long	2625083600
	.long	1073177592
	.long	2221232180
	.long	1073178111
	.long	2571808300
	.long	1073178630
	.long	3677067340
	.long	1073179149
	.long	1242297469
	.long	1073179669
	.long	3857688835
	.long	1073180188
	.long	2933562482
	.long	1073180708
	.long	2765141434
	.long	1073181228
	.long	3352681500
	.long	1073181748
	.long	401471288
	.long	1073182269
	.long	2501701371
	.long	1073182789
	.long	1063693232
	.long	1073183310
	.long	382670324
	.long	1073183831
	.long	458888893
	.long	1073184352
	.long	1292605272
	.long	1073184873
	.long	2884075881
	.long	1073185394
	.long	938589928
	.long	1073185916
	.long	4046338599
	.long	1073186437
	.long	3617643980
	.long	1073186959
	.long	3947730136
	.long	1073187481
	.long	741886623
	.long	1073188004
	.long	2590304972
	.long	1073188526
	.long	903307621
	.long	1073189049
	.long	4271086275
	.long	1073189571
	.long	4103963542
	.long	1073190094
	.long	402196713
	.long	1073190618
	.long	1755977754
	.long	1073191141
	.long	3870596832
	.long	1073191664
	.long	2451344201
	.long	1073192188
	.long	1793444794
	.long	1073192712
	.long	1897156335
	.long	1073193236
	.long	2762736634
	.long	1073193760
	.long	95476296
	.long	1073194285
	.long	2485567899
	.long	1073194809
	.long	1343334923
	.long	1073195334
	.long	964002825
	.long	1073195859
	.long	1347829853
	.long	1073196384
	.long	2495074343
	.long	1073196909
	.long	111027422
	.long	1073197435
	.long	2785882192
	.long	1073197960
	.long	1929962659
	.long	1073198486
	.long	1838494805
	.long	1073199012
	.long	2511737402
	.long	1073199538
	.long	3949949312
	.long	1073200064
	.long	1858422186
	.long	1073200591
	.long	532382357
	.long	1073201118
	.long	4267056243
	.long	1073201644
	.long	177801170
	.long	1073202172
	.long	1149778323
	.long	1073202699
	.long	2888279793
	.long	1073203226
	.long	1098597760
	.long	1073203754
	.long	75959080
	.long	1073204282
	.long	4115590701
	.long	1073204809
	.long	332850474
	.long	1073205338
	.long	1612900112
	.long	1073205866
	.long	3661032234
	.long	1073206394
	.long	2182539545
	.long	1073206923
	.long	1472649431
	.long	1073207452
	.long	1531622072
	.long	1073207981
	.long	2359717732
	.long	1073208510
	.long	3957196766
	.long	1073209039
	.long	2029352322
	.long	1073209569
	.long	871412225
	.long	1073210099
	.long	483637094
	.long	1073210629
	.long	866287636
	.long	1073211159
	.long	2019624647
	.long	1073211689
	.long	3943909011
	.long	1073212219
	.long	2344434403
	.long	1073212750
	.long	1516429178
	.long	1073213281
	.long	1460154487
	.long	1073213812
	.long	2175871566
	.long	1073214343
	.long	3663841740
	.long	1073214874
	.long	1629359127
	.long	1073215406
	.long	367652526
	.long	1073215938
	.long	4173950822
	.long	1073216469
	.long	163613810
	.long	1073217002
	.long	1221805143
	.long	1073217534
	.long	3053819383
	.long	1073218066
	.long	1364951179
	.long	1073218599
	.long	450429860
	.long	1073219132
	.long	310517550
	.long	1073219665
	.long	945476461
	.long	1073220198
	.long	2355568891
	.long	1073220731
	.long	246089934
	.long	1073221265
	.long	3207236660
	.long	1073221798
	.long	2649337043
	.long	1073222332
	.long	2867621035
	.long	1073222866
	.long	3862351381
	.long	1073223400
	.long	1338823616
	.long	1073223935
	.long	3887235257
	.long	1073224469
	.long	2917914723
	.long	1073225004
	.long	2726092409
	.long	1073225539
	.long	3312031505
	.long	1073226074
	.long	381027993
	.long	1073226610
	.long	2523279834
	.long	1073227145
	.long	1149115891
	.long	1073227681
	.long	553767008
	.long	1073228217
	.long	737496817
	.long	1073228753
	.long	1700569046
	.long	1073229289
	.long	3443247507
	.long	1073229825
	.long	1670828807
	.long	1073230362
	.long	678544234
	.long	1073230899
	.long	466657870
	.long	1073231436
	.long	1035433887
	.long	1073231973
	.long	2385136545
	.long	1073232510
	.long	221062897
	.long	1073233048
	.long	3133411975
	.long	1073233585
	.long	2532513716
	.long	1073234123
	.long	2713600035
	.long	1073234661
	.long	3676935639
	.long	1073235199
	.long	1127818031
	.long	1073235738
	.long	3656446689
	.long	1073236276
	.long	2673151999
	.long	1073236815
	.long	2473166324
	.long	1073237354
	.long	3056754818
	.long	1073237893
	.long	129215434
	.long	1073238433
	.long	2280748099
	.long	1073238972
	.long	921683645
	.long	1073239512
	.long	347254887
	.long	1073240052
	.long	557727428
	.long	1073240592
	.long	1553366965
	.long	1073241132
	.long	3334439281
	.long	1073241672
	.long	1606242957
	.long	1073242213
	.long	664011255
	.long	1073242754
	.long	508010229
	.long	1073243295
	.long	1138506026
	.long	1073243836
	.long	2555764879
	.long	1073244377
	.long	465085820
	.long	1073244919
	.long	3456669854
	.long	1073245460
	.long	2940848899
	.long	1073246002
	.long	3212856843
	.long	1073246544
	.long	4272960376
	.long	1073247086
	.long	1826458977
	.long	1073247629
	.long	168586808
	.long	1073248172
	.long	3594578124
	.long	1073248714
	.long	3514765380
	.long	1073249257
	.long	4224383010
	.long	1073249800
	.long	1428730948
	.long	1073250344
	.long	3718011103
	.long	1073250887
	.long	2502556294
	.long	1073251431
	.long	2077601315
	.long	1073251975
	.long	2443413759
	.long	1073252519
	.long	3600261305
	.long	1073253063
	.long	1253444431
	.long	1073253608
	.long	3993165589
	.long	1073254152
	.long	3229758142
	.long	1073254697
	.long	3258457428
	.long	1073255242
	.long	4079531583
	.long	1073255787
	.long	1398281537
	.long	1073256333
	.long	3804910195
	.long	1073256878
	.long	2709751374
	.long	1073257424
	.long	2408040869
	.long	1073257970
	.long	2900047266
	.long	1073258516
	.long	4186039245
	.long	1073259062
	.long	1971318283
	.long	1073259609
	.long	551120534
	.long	1073260156
	.long	4220682249
	.long	1073260702
	.long	95370583
	.long	1073261250
	.long	1060356558
	.long	1073261797
	.long	2820942105
	.long	1073262344
	.long	1082429243
	.long	1073262892
	.long	140054677
	.long	1073263440
	.long	4289055202
	.long	1073263987
	.long	644798521
	.long	1073264536
	.long	2092456205
	.long	1073265084
	.long	42363431
	.long	1073265633
	.long	3084724658
	.long	1073266181
	.long	2629875250
	.long	1073266730
	.long	2973052550
	.long	1073267279
	.long	4114526696
	.long	1073267828
	.long	1759600621
	.long	1073268378
	.long	203511943
	.long	1073268928
	.long	3741498372
	.long	1073269477
	.long	3783895818
	.long	1073270027
	.long	330974880
	.long	1073270578
	.long	1972940835
	.long	1073271128
	.long	120129872
	.long	1073271679
	.long	3362747453
	.long	1073272229
	.long	3111129949
	.long	1073272780
	.long	3660515711
	.long	1073273331
	.long	716208586
	.long	1073273883
	.long	2868414407
	.long	1073274434
	.long	1527469912
	.long	1073274986
	.long	988613815
	.long	1073275538
	.long	1252117633
	.long	1073276090
	.long	2318252969
	.long	1073276642
	.long	4187291521
	.long	1073277194
	.long	2564537783
	.long	1073277747
	.long	1745230932
	.long	1073278300
	.long	1729642940
	.long	1073278853
	.long	2518045872
	.long	1073279406
	.long	4110711887
	.long	1073279959
	.long	2212945938
	.long	1073280513
	.long	1119987663
	.long	1073281067
	.long	832109496
	.long	1073281621
	.long	1349583962
	.long	1073282175
	.long	2672683681
	.long	1073282729
	.long	506714065
	.long	1073283284
	.long	3441882511
	.long	1073283838
	.long	2888527322
	.long	1073284393
	.long	3141888782
	.long	1073284948
	.long	4202239970
	.long	1073285503
	.long	1774886764
	.long	1073286059
	.long	155069723
	.long	1073286615
	.long	3638029501
	.long	1073287170
	.long	3634104958
	.long	1073287726
	.long	143569636
	.long	1073288283
	.long	1756631760
	.long	1073288839
	.long	4178597763
	.long	1073289395
	.long	3114774171
	.long	1073289952
	.long	2860402192
	.long	1073290509
	.long	3415755831
	.long	1073291066
	.long	486141891
	.long	1073291624
	.long	2661769156
	.long	1073292181
	.long	1352977319
	.long	1073292739
	.long	855008051
	.long	1073293297
	.long	1168135824
	.long	1073293855
	.long	2292635199
	.long	1073294413
	.long	4228780835
	.long	1073294971
	.long	2681880180
	.long	1073295530
	.long	1947175377
	.long	1073296089
	.long	2024941357
	.long	1073296648
	.long	2915453149
	.long	1073297207
	.long	324018578
	.long	1073297767
	.long	2840847451
	.long	1073298326
	.long	1876280482
	.long	1073298886
	.long	1725560368
	.long	1073299446
	.long	2388962603
	.long	1073300006
	.long	3866762774
	.long	1073300566
	.long	1864269266
	.long	1073301127
	.long	676725147
	.long	1073301688
	.long	304406285
	.long	1073302249
	.long	747588638
	.long	1073302810
	.long	2006548263
	.long	1073303371
	.long	4081561306
	.long	1073303932
	.long	2677936710
	.long	1073304494
	.long	2090918109
	.long	1073305056
	.long	2320781926
	.long	1073305618
	.long	3367804686
	.long	1073306180
	.long	937295706
	.long	1073306743
	.long	3619466287
	.long	1073307305
	.long	2824658639
	.long	1073307868
	.long	2848116954
	.long	1073308431
	.long	3690118222
	.long	1073308994
	.long	1055972230
	.long	1073309558
	.long	3535890746
	.long	1073310121
	.long	2540216452
	.long	1073310685
	.long	2364194007
	.long	1073311249
	.long	3008100869
	.long	1073311813
	.long	177247295
	.long	1073312378
	.long	2461845526
	.long	1073312942
	.long	1272238707
	.long	1073313507
	.long	903671971
	.long	1073314072
	.long	1356423246
	.long	1073314637
	.long	2630770556
	.long	1073315202
	.long	432024719
	.long	1073315768
	.long	3350398541
	.long	1073316333
	.long	2796235733
	.long	1073316899
	.long	3064781991
	.long	1073317465
	.long	4156315810
	.long	1073318031
	.long	1776148481
	.long	1073318598
	.long	219525981
	.long	1073319165
	.long	3781694384
	.long	1073319731
	.long	3872997969
	.long	1073320298
	.long	493715700
	.long	1073320866
	.long	2234061230
	.long	1073321433
	.long	504379122
	.long	1073322001
	.long	3894883214
	.long	1073322568
	.long	3815918261
	.long	1073323136
	.long	267763697
	.long	1073323705
	.long	1840633648
	.long	1073324273
	.long	4239840446
	.long	1073324841
	.long	3170696514
	.long	1073325410
	.long	2928448968
	.long	1073325979
	.long	3513377716
	.long	1073326548
	.long	630795469
	.long	1073327118
	.long	2870916921
	.long	1073327687
	.long	1644087673
	.long	1073328257
	.long	1245555313
	.long	1073328827
	.long	1675600225
	.long	1073329397
	.long	2934502889
	.long	1073329967
	.long	727576585
	.long	1073330538
	.long	3645036575
	.long	1073331108
	.long	3097229033
	.long	1073331679
	.long	3379402112
	.long	1073332250
	.long	196869472
	.long	1073332822
	.long	2139846758
	.long	1073333393
	.long	618680520
	.long	1073333965
	.long	4223586593
	.long	1073334536
	.long	69944423
	.long	1073335109
	.long	1042937330
	.long	1073335681
	.long	2847879545
	.long	1073336253
	.long	1190085390
	.long	1073336826
	.long	364803882
	.long	1073337399
	.long	372316832
	.long	1073337972
	.long	1212906146
	.long	1073338545
	.long	2886853829
	.long	1073339118
	.long	1099474683
	.long	1073339692
	.long	146018196
	.long	1073340266
	.long	26766661
	.long	1073340840
	.long	742002461
	.long	1073341414
	.long	2292008077
	.long	1073341988
	.long	382098789
	.long	1073342563
	.long	3602491862
	.long	1073343137
	.long	3363535471
	.long	1073343712
	.long	3960479776
	.long	1073344287
	.long	1098640438
	.long	1073344863
	.long	3368235106
	.long	1073345438
	.long	2179612338
	.long	1073346014
	.long	1828022677
	.long	1073346590
	.long	2313749465
	.long	1073347166
	.long	3637076140
	.long	1073347742
	.long	1503318941
	.long	1073348319
	.long	207728794
	.long	1073348896
	.long	4045556722
	.long	1073349472
	.long	132184659
	.long	1073350050
	.long	1352798410
	.long	1073350627
	.long	3412714695
	.long	1073351204
	.long	2017250329
	.long	1073351782
	.long	1461656813
	.long	1073352360
	.long	1746218452
	.long	1073352938
	.long	2871219644
	.long	1073353516
	.long	541977589
	.long	1073354095
	.long	3348711472
	.long	1073354673
	.long	2701771387
	.long	1073355252
	.long	2896409416
	.long	1073355831
	.long	3932910439
	.long	1073356410
	.long	1516592138
	.long	1073356990
	.long	4237674180
	.long	1073357569
	.long	3506507142
	.long	1073358149
	.long	3618343587
	.long	1073358729
	.long	278501583
	.long	1073359310
	.long	2077201180
	.long	1073359890
	.long	424793346
	.long	1073360471
	.long	3911498325
	.long	1073361051
	.long	3947667274
	.long	1073361632
	.long	533586041
	.long	1073362214
	.long	2259475161
	.long	1073362795
	.long	535686082
	.long	1073363377
	.long	3952439535
	.long	1073363958
	.long	3920087160
	.long	1073364540
	.long	438915288
	.long	1073365123
	.long	2099144940
	.long	1073365705
	.long	311128049
	.long	1073366288
	.long	3665085827
	.long	1073366870
	.long	3571370402
	.long	1073367453
	.long	30268589
	.long	1073368037
	.long	1632001894
	.long	1073368620
	.long	4081890032
	.long	1073369203
	.long	3085252812
	.long	1073369787
	.long	2937344736
	.long	1073370371
	.long	3638453105
	.long	1073370955
	.long	893898021
	.long	1073371540
	.long	3293901575
	.long	1073372124
	.long	2248816765
	.long	1073372709
	.long	2053898580
	.long	1073373294
	.long	2709434808
	.long	1073373879
	.long	4215713335
	.long	1073374464
	.long	2278054847
	.long	1073375050
	.long	1191714722
	.long	1073375636
	.long	956981137
	.long	1073376222
	.long	1574142367
	.long	1073376808
	.long	3043486787
	.long	1073377394
	.long	1070335571
	.long	1073377981
	.long	4244911880
	.long	1073378567
	.long	3977569787
	.long	1073379154
	.long	268598057
	.long	1073379742
	.long	1708220144
	.long	1073380329
	.long	4001757711
	.long	1073380916
	.long	2854532518
	.long	1073381504
	.long	2561801018
	.long	1073382092
	.long	3123852464
	.long	1073382680
	.long	246008909
	.long	1073383269
	.long	2518494395
	.long	1073383857
	.long	1351663878
	.long	1073384446
	.long	1040774297
	.long	1073385035
	.long	1586115396
	.long	1073385624
	.long	2987977015
	.long	1073386213
	.long	951681799
	.long	1073386803
	.long	4067454374
	.long	1073387392
	.long	3745650286
	.long	1073387982
	.long	4281527065
	.long	1073388572
	.long	1380407745
	.long	1073389163
	.long	3632517350
	.long	1073389753
	.long	2448211814
	.long	1073390344
	.long	2122749062
	.long	1073390935
	.long	2656419818
	.long	1073391526
	.long	4049514907
	.long	1073392117
	.long	2007357954
	.long	1073392709
	.long	825207276
	.long	1073393301
	.long	503353991
	.long	1073393893
	.long	1042089317
	.long	1073394485
	.long	2441704572
	.long	1073395077
	.long	407523872
	.long	1073395670
	.long	3529773324
	.long	1073396262
	.long	3218809949
	.long	1073396855
	.long	3769892752
	.long	1073397448
	.long	888346247
	.long	1073398042
	.long	3164396935
	.long	1073398635
	.long	2008402230
	.long	1073399229
	.long	1715621535
	.long	1073399823
	.long	2286347056
	.long	1073400417
	.long	3720871095
	.long	1073401011
	.long	1724518759
	.long	1073401606
	.long	592549846
	.long	1073402201
	.long	325256956
	.long	1073402796
	.long	922932790
	.long	1073403391
	.long	2385870147
	.long	1073403986
	.long	419394629
	.long	1073404582
	.long	3613733823
	.long	1073405177
	.long	3379246235
	.long	1073405773
	.long	4011192357
	.long	1073406369
	.long	1214898188
	.long	1073406966
	.long	3580591712
	.long	1073407562
	.long	2518631832
	.long	1073408159
	.long	2324279437
	.long	1073408756
	.long	2997828218
	.long	1073409353
	.long	244604671
	.long	1073409951
	.long	2654837279
	.long	1073410548
	.long	1638885440
	.long	1073411146
	.long	1492010541
	.long	1073411744
	.long	2214506772
	.long	1073412342
	.long	3806668421
	.long	1073412940
	.long	1973822580
	.long	1073413539
	.long	1011231034
	.long	1073414138
	.long	919188372
	.long	1073414737
	.long	1697989282
	.long	1073415336
	.long	3347928550
	.long	1073415935
	.long	1574333768
	.long	1073416535
	.long	672467219
	.long	1073417135
	.long	642623992
	.long	1073417735
	.long	1485099270
	.long	1073418335
	.long	3200188344
	.long	1073418935
	.long	1493219302
	.long	1073419536
	.long	659454927
	.long	1073420137
	.long	699190808
	.long	1073420738
	.long	1612722630
	.long	1073421339
	.long	3400346180
	.long	1073421940
	.long	1767390049
	.long	1073422542
	.long	1009117521
	.long	1073423144
	.long	1125824683
	.long	1073423746
	.long	2117807723
	.long	1073424348
	.long	3985362929
	.long	1073424950
	.long	2433819392
	.long	1073425553
	.long	1758440898
	.long	1073426156
	.long	1959524035
	.long	1073426759
	.long	3037365493
	.long	1073427362
	.long	697294765
	.long	1073427966
	.long	3529543335
	.long	1073428569
	.long	2944473600
	.long	1073429173
	.long	3237349947
	.long	1073429777
	.long	113502272
	.long	1073430382
	.long	2163162461
	.long	1073430986
	.long	796693315
	.long	1073431591
	.long	309359621
	.long	1073432196
	.long	701458976
	.long	1073432801
	.long	1973289073
	.long	1073433406
	.long	4125147710
	.long	1073434011
	.long	2862365488
	.long	1073434617
	.long	2480207697
	.long	1073435223
	.long	2978972441
	.long	1073435829
	.long	63990619
	.long	1073436436
	.long	2325495125
	.long	1073437042
	.long	1173849767
	.long	1073437649
	.long	904320345
	.long	1073438256
	.long	1517205462
	.long	1073438863
	.long	3012803822
	.long	1073439470
	.long	1096446934
	.long	1073440078
	.long	63401002
	.long	1073440686
	.long	4208932328
	.long	1073441293
	.long	648438135
	.long	1073441902
	.long	2267119521
	.long	1073442510
	.long	475341206
	.long	1073443119
	.long	3863337193
	.long	1073443727
	.long	3841472405
	.long	1073444336
	.long	410046459
	.long	1073444946
	.long	2159293663
	.long	1073445555
	.long	499579244
	.long	1073446165
	.long	4021137714
	.long	1073446774
	.long	4134334502
	.long	1073447384
	.long	839469732
	.long	1073447995
	.long	2726778220
	.long	1073448605
	.long	1206625701
	.long	1073449216
	.long	574279898
	.long	1073449827
	.long	830041342
	.long	1073450438
	.long	1974210664
	.long	1073451049
	.long	4007088598
	.long	1073451660
	.long	2634008681
	.long	1073452272
	.long	2150239147
	.long	1073452884
	.long	2556081034
	.long	1073453496
	.long	3851835485
	.long	1073454108
	.long	1742836444
	.long	1073454721
	.long	524352553
	.long	1073455334
	.long	196685258
	.long	1073455947
	.long	760136107
	.long	1073456560
	.long	2215006752
	.long	1073457173
	.long	266631649
	.long	1073457787
	.long	3505247244
	.long	1073458400
	.long	3341220903
	.long	1073459014
	.long	4069821983
	.long	1073459628
	.long	1396385347
	.long	1073460243
	.long	3911147852
	.long	1073460857
	.long	3024477272
	.long	1073461472
	.long	3031643371
	.long	1073462087
	.long	3932948720
	.long	1073462702
	.long	1433728699
	.long	1073463318
	.long	4124220672
	.long	1073463933
	.long	3414792927
	.long	1073464549
	.long	3600715740
	.long	1073465165
	.long	387324901
	.long	1073465782
	.long	2364858184
	.long	1073466398
	.long	943684289
	.long	1073467015
	.long	419073901
	.long	1073467632
	.long	791330515
	.long	1073468249
	.long	2060757728
	.long	1073468866
	.long	4227659239
	.long	1073469483
	.long	2997371554
	.long	1073470101
	.long	2665165874
	.long	1073470719
	.long	3231346208
	.long	1073471337
	.long	401249370
	.long	1073471956
	.long	2765114166
	.long	1073472574
	.long	1733310322
	.long	1073473193
	.long	1601109552
	.long	1073473812
	.long	2368816380
	.long	1073474431
	.long	4036735431
	.long	1073475050
	.long	2310204140
	.long	1073475670
	.long	1484494633
	.long	1073476290
	.long	1559911846
	.long	1073476910
	.long	2536760819
	.long	1073477530
	.long	120379396
	.long	1073478151
	.long	2901007417
	.long	1073478771
	.long	2289015635
	.long	1073479392
	.long	2579676800
	.long	1073480013
	.long	3773296467
	.long	1073480634
	.long	1575212999
	.long	1073481256
	.long	280699453
	.long	1073481878
	.long	4185028992
	.long	1073482499
	.long	403605697
	.long	1073483122
	.long	1821637528
	.long	1073483744
	.long	4144463366
	.long	1073484366
	.long	3077422196
	.long	1073484989
	.long	2915787696
	.long	1073485612
	.long	3659866354
	.long	1073486235
	.long	1014997465
	.long	1073486859
	.long	3571422318
	.long	1073487482
	.long	2739513116
	.long	1073488106
	.long	2814544062
	.long	1073488730
	.long	3796822160
	.long	1073489354
	.long	1391687223
	.long	1073489979
	.long	4189381061
	.long	1073490603
	.long	3600276399
	.long	1073491228
	.long	3919647954
	.long	1073491853
	.long	852835958
	.long	1073492479
	.long	2990082633
	.long	1073493104
	.long	1741761122
	.long	1073493730
	.long	1403146560
	.long	1073494356
	.long	1974546891
	.long	1073494982
	.long	3456270160
	.long	1073495608
	.long	1553657225
	.long	1073496235
	.long	561983637
	.long	1073496862
	.long	481557757
	.long	1073497489
	.long	1312688048
	.long	1073498116
	.long	3055683081
	.long	1073498743
	.long	1415884232
	.long	1073499371
	.long	688567575
	.long	1073499999
	.long	874041994
	.long	1073500627
	.long	1972616476
	.long	1073501255
	.long	3984600112
	.long	1073501883
	.long	2615334802
	.long	1073502512
	.long	2160097144
	.long	1073503141
	.long	2619196543
	.long	1073503770
	.long	3992942510
	.long	1073504399
	.long	1986677364
	.long	1073505029
	.long	895678121
	.long	1073505659
	.long	720254605
	.long	1073506289
	.long	1460716748
	.long	1073506919
	.long	3117374583
	.long	1073507549
	.long	1395570953
	.long	1073508180
	.long	590583400
	.long	1073508811
	.long	702722273
	.long	1073509442
	.long	1732298027
	.long	1073510073
	.long	3679621221
	.long	1073510704
	.long	2250035226
	.long	1073511336
	.long	1738818105
	.long	1073511968
	.long	2146280735
	.long	1073512600
	.long	3472734097
	.long	1073513232
	.long	1423521980
	.long	1073513865
	.long	293922871
	.long	1073514498
	.long	84248066
	.long	1073515131
	.long	794808969
	.long	1073515764
	.long	2425917084
	.long	1073516397
	.long	682916729
	.long	1073517031
	.long	4156054215
	.long	1073517664
	.long	4255706773
	.long	1073518298
	.long	982186331
	.long	1073518933
	.long	2925739518
	.long	1073519567
	.long	1496743882
	.long	1073520202
	.long	990478963
	.long	1073520837
	.long	1407257115
	.long	1073521472
	.long	2747390795
	.long	1073522107
	.long	716225270
	.long	1073522743
	.long	3904007800
	.long	1073523378
	.long	3721116570
	.long	1073524014
	.long	167864460
	.long	1073524651
	.long	1834499047
	.long	1073525287
	.long	131398835
	.long	1073525924
	.long	3648811612
	.long	1073526560
	.long	3797116091
	.long	1073527197
	.long	576625684
	.long	1073527835
	.long	2577588500
	.long	1073528472
	.long	1210383568
	.long	1073529110
	.long	770291914
	.long	1073529748
	.long	1257627374
	.long	1073530386
	.long	2672703888
	.long	1073531024
	.long	720868210
	.long	1073531663
	.long	3992369085
	.long	1073532301
	.long	3897586180
	.long	1073532940
	.long	436833864
	.long	1073533580
	.long	2200361202
	.long	1073534219
	.long	598548181
	.long	1073534859
	.long	4221644080
	.long	1073535498
	.long	185061802
	.long	1073536139
	.long	1374018136
	.long	1073536779
	.long	3493860791
	.long	1073537419
	.long	2249937585
	.long	1073538060
	.long	1937531030
	.long	1073538701
	.long	2556956456
	.long	1073539342
	.long	4108529292
	.long	1073539983
	.long	2297597784
	.long	1073540625
	.long	1419444873
	.long	1073541267
	.long	1474386312
	.long	1073541909
	.long	2462737961
	.long	1073542551
	.long	89848491
	.long	1073543194
	.long	2945968568
	.long	1073543836
	.long	2441479782
	.long	1073544479
	.long	2871665714
	.long	1073545122
	.long	4236842761
	.long	1073545765
	.long	2242360129
	.long	1073546409
	.long	1183501723
	.long	1073547053
	.long	1060584258
	.long	1073547697
	.long	1873924558
	.long	1073548341
	.long	3623839555
	.long	1073548985
	.long	2015678991
	.long	1073549630
	.long	1344727307
	.long	1073550275
	.long	1611301755
	.long	1073550920
	.long	2815719695
	.long	1073551565
	.long	663331300
	.long	1073552211
	.long	3744388737
	.long	1073552856
	.long	3469275096
	.long	1073553502
	.long	4133275462
	.long	1073554148
	.long	1441740438
	.long	1073554795
	.long	3984922621
	.long	1073555441
	.long	3173205530
	.long	1073556088
	.long	3301874686
	.long	1073556735
	.long	76281118
	.long	1073557383
	.long	2086677854
	.long	1073558030
	.long	743448846
	.long	1073558678
	.long	341880045
	.long	1073559326
	.long	882290206
	.long	1073559974
	.long	2364998199
	.long	1073560622
	.long	495355702
	.long	1073561271
	.long	3863616392
	.long	1073561919
	.long	3880164866
	.long	1073562568
	.long	545320427
	.long	1073563218
	.long	2449337072
	.long	1073563867
	.long	1002599724
	.long	1073564517
	.long	500395305
	.long	1073565167
	.long	943043546
	.long	1073565817
	.long	2330864287
	.long	1073566467
	.long	369210180
	.long	1073567118
	.long	3648335875
	.long	1073567768
	.long	3578626945
	.long	1073568419
	.long	160403663
	.long	1073569071
	.long	1983921003
	.long	1073569722
	.long	459564863
	.long	1073570374
	.long	4177590436
	.long	1073571025
	.long	253416538
	.long	1073571678
	.long	1572265876
	.long	1073572330
	.long	3839492077
	.long	1073572982
	.long	2760448878
	.long	1073573635
	.long	2630424719
	.long	1073574288
	.long	3449740848
	.long	1073574941
	.long	923751329
	.long	1073575595
	.long	3642712223
	.long	1073576248
	.long	3017010515
	.long	1073576902
	.long	3341935185
	.long	1073577556
	.long	322840734
	.long	1073578211
	.long	2549983656
	.long	1073578865
	.long	1433751372
	.long	1073579520
	.long	1269433299
	.long	1073580175
	.long	2057351669
	.long	1073580830
	.long	3797828821
	.long	1073581485
	.long	2196219907
	.long	1073582141
	.long	1547814783
	.long	1073582797
	.long	1852936114
	.long	1073583453
	.long	3111906678
	.long	1073584109
	.long	1030082064
	.long	1073584766
	.long	4197719859
	.long	1073585422
	.long	4025208575
	.long	1073586079
	.long	512871426
	.long	1073586737
	.long	2250966326
	.long	1073587394
	.long	649882117
	.long	1073588052
	.long	4909637
	.long	1073588710
	.long	316372536
	.long	1073589368
	.long	1584594577
	.long	1073590026
	.long	3809899630
	.long	1073590684
	.long	2697644378
	.long	1073591343
	.long	2543120208
	.long	1073592002
	.long	3346651320
	.long	1073592661
	.long	813594727
	.long	1073593321
	.long	3534209441
	.long	1073593980
	.long	2918885397
	.long	1073594640
	.long	3262914532
	.long	1073595300
	.long	271654298
	.long	1073595961
	.long	2535364145
	.long	1073596621
	.long	1464434451
	.long	1073597282
	.long	1354157591
	.long	1073597943
	.long	2204858751
	.long	1073598604
	.long	4016863232
	.long	1073599265
	.long	2495529148
	.long	1073599927
	.long	1936149311
	.long	1073600589
	.long	2339049353
	.long	1073601251
	.long	3704555011
	.long	1073601913
	.long	1738024841
	.long	1073602576
	.long	734752097
	.long	1073603239
	.long	695062851
	.long	1073603902
	.long	1619283284
	.long	1073604565
	.long	3507739686
	.long	1073605228
	.long	2065791163
	.long	1073605892
	.long	1588731523
	.long	1073606556
	.long	2076887390
	.long	1073607220
	.long	3530585494
	.long	1073607884
	.long	1655185387
	.long	1073608549
	.long	745981317
	.long	1073609214
	.long	803300349
	.long	1073609879
	.long	1827469659
	.long	1073610544
	.long	3818816536
	.long	1073611209
	.long	2482701077
	.long	1073611875
	.long	2114418090
	.long	1073612541
	.long	2714295193
	.long	1073613207
	.long	4282660114
	.long	1073613873
	.long	2524873401
	.long	1073614540
	.long	1736230300
	.long	1073615207
	.long	1917058875
	.long	1073615874
	.long	3067687299
	.long	1073616541
	.long	893476561
	.long	1073617209
	.long	3984689650
	.long	1073617876
	.long	3751720481
	.long	1073618544
	.long	194897673
	.long	1073619213
	.long	1904484547
	.long	1073619881
	.long	290875353
	.long	1073620550
	.long	3944333634
	.long	1073621218
	.long	4275253862
	.long	1073621887
	.long	1283965214
	.long	1073622557
	.long	3560731566
	.long	1073623226
	.long	2515947727
	.long	1073623896
	.long	2444910500
	.long	1073624566
	.long	3347949506
	.long	1073625236
	.long	930427183
	.long	1073625907
	.long	3782607966
	.long	1073626577
	.long	3314887221
	.long	1073627248
	.long	3822562310
	.long	1073627919
	.long	1010996116
	.long	1073628591
	.long	3470453524
	.long	1073629262
	.long	2611330342
	.long	1073629934
	.long	2728924383
	.long	1073630606
	.long	3823566274
	.long	1073631278
	.long	1600619456
	.long	1073631951
	.long	355382076
	.long	1073632624
	.long	88185097
	.long	1073633297
	.long	799359593
	.long	1073633970
	.long	2489236751
	.long	1073634643
	.long	863180574
	.long	1073635317
	.long	216489768
	.long	1073635991
	.long	549495856
	.long	1073636665
	.long	1862530475
	.long	1073637339
	.long	4155925370
	.long	1073638013
	.long	3135045107
	.long	1073638688
	.long	3095188953
	.long	1073639363
	.long	4036688994
	.long	1073640038
	.long	1664910130
	.long	1073640714
	.long	275151965
	.long	1073641390
	.long	4162714219
	.long	1073642065
	.long	443027538
	.long	1073642742
	.long	2001326456
	.long	1073643418
	.long	248009142
	.long	1073644095
	.long	3773343060
	.long	1073644771
	.long	3987726602
	.long	1073645448
	.long	891492865
	.long	1073646126
	.long	3074909654
	.long	1073646803
	.long	1948375699
	.long	1073647481
	.long	1807191730
	.long	1073648159
	.long	2651691300
	.long	1073648837
	.long	187240772
	.long	1073649516
	.long	3004108512
	.long	1073650194
	.long	2512693819
	.long	1073650873
	.long	3008297986
	.long	1073651552
	.long	196287834
	.long	1073652232
	.long	2666932180
	.long	1073652911
	.long	1830630773
	.long	1073653591
	.long	1982685362
	.long	1073654271
	.long	3123430513
	.long	1073654951
	.long	958233609
	.long	1073655632
	.long	4077364038
	.long	1073656312
	.long	3891222112
	.long	1073656993
	.long	400142851
	.long	1073657675
	.long	2194395979
	.long	1073658356
	.long	684382154
	.long	1073659038
	.long	165404028
	.long	1073659720
	.long	637797077
	.long	1073660402
	.long	2101896888
	.long	1073661084
	.long	263071865
	.long	1073661767
	.long	3711592415
	.long	1073662449
	.long	3857859873
	.long	1073663132
	.long	702210283
	.long	1073663816
	.long	2834914390
	.long	1073664499
	.long	1666373871
	.long	1073665183
	.long	1491892407
	.long	1073665867
	.long	2311806496
	.long	1073666551
	.long	4126452746
	.long	1073667235
	.long	2641200587
	.long	1073667920
	.long	2151354154
	.long	1073668605
	.long	2657250399
	.long	1073669290
	.long	4159226391
	.long	1073669975
	.long	2362652014
	.long	1073670661
	.long	1562831858
	.long	1073671347
	.long	1760103333
	.long	1073672033
	.long	2954803963
	.long	1073672719
	.long	852304090
	.long	1073673406
	.long	4042876058
	.long	1073674092
	.long	3936923140
	.long	1073674779
	.long	534783318
	.long	1073675467
	.long	2426729279
	.long	1073676154
	.long	1023164641
	.long	1073676842
	.long	619395024
	.long	1073677530
	.long	1215758867
	.long	1073678218
	.long	2812594723
	.long	1073678906
	.long	1115273962
	.long	1073679595
	.long	419102666
	.long	1073680284
	.long	724419729
	.long	1073680973
	.long	2031564164
	.long	1073681662
	.long	45907802
	.long	1073682352
	.long	3357724476
	.long	1073683041
	.long	3377418951
	.long	1073683731
	.long	105330697
	.long	1073684422
	.long	2131733893
	.long	1073685112
	.long	867033647
	.long	1073685803
	.long	606537073
	.long	1073686494
	.long	1350584099
	.long	1073687185
	.long	3099514774
	.long	1073687876
	.long	1558701962
	.long	1073688568
	.long	1023453234
	.long	1073689260
	.long	1494108984
	.long	1073689952
	.long	2971009717
	.long	1073690644
	.long	1159528760
	.long	1073691337
	.long	354974146
	.long	1073692030
	.long	557686728
	.long	1073692723
	.long	1768007473
	.long	1073693416
	.long	3986277466
	.long	1073694109
	.long	2917870609
	.long	1073694803
	.long	2858095512
	.long	1073695497
	.long	3807293606
	.long	1073696191
	.long	1470839139
	.long	1073696886
	.long	144041070
	.long	1073697581
	.long	4122208472
	.long	1073698275
	.long	520781347
	.long	1073698971
	.long	2225003594
	.long	1073699666
	.long	645282744
	.long	1073700362
	.long	76928333
	.long	1073701058
	.long	520282716
	.long	1073701754
	.long	1975688363
	.long	1073702450
	.long	148520570
	.long	1073703147
	.long	3629056629
	.long	1073703843
	.long	3827704769
	.long	1073704540
	.long	744807923
	.long	1073705238
	.long	2970643739
	.long	1073705935
	.long	1915620789
	.long	1073706633
	.long	1875049654
	.long	1073707331
	.long	2849273735
	.long	1073708029
	.long	543669252
	.long	1073708728
	.long	3548514428
	.long	1073709426
	.long	3274218421
	.long	1073710125
	.long	4016092394
	.long	1073710824
	.long	1479513031
	.long	1073711524
	.long	4254759022
	.long	1073712223
	.long	3752239991
	.long	1073712923
	.long	4267267563
	.long	1073713623
	.long	1505218891
	.long	1073714324
	.long	4056373132
	.long	1073715024
	.long	3331140374
	.long	1073715725
	.long	3624832712
	.long	1073716426
	.long	642827762
	.long	1073717128
	.long	2975405148
	.long	1073717829
	.long	2032975426
	.long	1073718531
	.long	2110851158
	.long	1073719233
	.long	3209377725
	.long	1073719935
	.long	1033933329
	.long	1073720638
	.long	4174798177
	.long	1073721340
	.long	4042383409
	.long	1073722043
	.long	637034877
	.long	1073722747
	.long	2549033138
	.long	1073723450
	.long	1188789684
	.long	1073724154
	.long	851618011
	.long	1073724858
	.long	1537864438
	.long	1073725562
	.long	3247875400
	.long	1073726266
	.long	1687030154
	.long	1073726971
	.long	1150642665
	.long	1073727676
	.long	1639059720
	.long	1073728381
	.long	3152628225
	.long	1073729086
	.long	1396727905
	.long	1073729792
	.long	666673196
	.long	1073730498
	.long	962811354
	.long	1073731204
	.long	2285489754
	.long	1073731910
	.long	340088594
	.long	1073732617
	.long	3716890072
	.long	1073733323
	.long	3826307327
	.long	1073734030
	.long	668688203
	.long	1073734738
	.long	2834315254
	.long	1073735445
	.long	1733601969
	.long	1073736153
	.long	1661863844
	.long	1073736861
	.long	2619449193
	.long	1073737569
	.long	311739156
	.long	1073738278
	.long	3329016876
	.long	1073738986
	.long	3081696432
	.long	1073739695
	.long	3865093909
	.long	1073740404
	.long	1384590916
	.long	1073741114
	.long	4230471070
	.long	1073741823
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
	.long	1697350398
	.long	1084691783
	.long	1697350398
	.long	1084691783
	.long	1697350398
	.long	1084691783
	.long	1697350398
	.long	1084691783
	.long	1697350398
	.long	1084691783
	.long	1697350398
	.long	1084691783
	.long	1697350398
	.long	1084691783
	.long	1697350398
	.long	1084691783
	.long	0
	.long	1127743488
	.long	0
	.long	1127743488
	.long	0
	.long	1127743488
	.long	0
	.long	1127743488
	.long	0
	.long	1127743488
	.long	0
	.long	1127743488
	.long	0
	.long	1127743488
	.long	0
	.long	1127743488
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
	.long	67444641
	.long	1060515641
	.long	67444641
	.long	1060515641
	.long	67444641
	.long	1060515641
	.long	67444641
	.long	1060515641
	.long	67444641
	.long	1060515641
	.long	67444641
	.long	1060515641
	.long	67444641
	.long	1060515641
	.long	67444641
	.long	1060515641
	.long	2047
	.long	0
	.long	2047
	.long	0
	.long	2047
	.long	0
	.long	2047
	.long	0
	.long	2047
	.long	0
	.long	2047
	.long	0
	.long	2047
	.long	0
	.long	2047
	.long	0
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
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.long	1079639606
	.type	__svml_spow_ha_data_internal,@object
	.size	__svml_spow_ha_data_internal,30848
	.align 32
_vmlsPowHATab:
	.long	1065353216
	.long	1063518208
	.long	1061945344
	.long	1060765696
	.long	1059717120
	.long	1058930688
	.long	1058144256
	.long	1057488896
	.long	1056964608
	.long	0
	.long	0
	.long	1043013120
	.long	912347133
	.long	1051178752
	.long	920491638
	.long	1055650560
	.long	910207939
	.long	3201407744
	.long	3065009147
	.long	3197864192
	.long	3046757530
	.long	3192020992
	.long	3064938684
	.long	3182631936
	.long	3069048210
	.long	0
	.long	0
	.long	1065353216
	.long	1064828928
	.long	1064304640
	.long	1066008576
	.long	1065877504
	.long	1065746432
	.long	1065615360
	.long	1065484288
	.long	1065353216
	.long	0
	.long	0
	.long	1027315712
	.long	915720665
	.long	1035907072
	.long	882039287
	.long	3185459200
	.long	3062137179
	.long	3182631936
	.long	3069048210
	.long	3179763712
	.long	3059908234
	.long	3174420480
	.long	3067311503
	.long	3166121984
	.long	3066774241
	.long	0
	.long	0
	.long	1069056000
	.long	1069006848
	.long	1068957696
	.long	1068908544
	.long	1068875776
	.long	1069318144
	.long	1069301760
	.long	1069268992
	.long	1069252608
	.long	1069219840
	.long	1069203456
	.long	1069170688
	.long	1069154304
	.long	1069121536
	.long	1069105152
	.long	1069072384
	.long	1069056000
	.long	0
	.long	0
	.long	1002471424
	.long	906080490
	.long	1010884608
	.long	916991201
	.long	1016135680
	.long	905081895
	.long	1018261504
	.long	918286540
	.long	3170725888
	.long	3067774196
	.long	3169697792
	.long	3068476631
	.long	3167637504
	.long	3069858259
	.long	3166609408
	.long	3020376912
	.long	3164540928
	.long	3039629966
	.long	3163504640
	.long	3043319364
	.long	3160350720
	.long	3046704089
	.long	3158269952
	.long	3047249178
	.long	3154083840
	.long	3046609959
	.long	3149905920
	.long	3045301896
	.long	3137339392
	.long	3034784887
	.long	0
	.long	0
	.long	1065353216
	.long	0
	.long	1065398766
	.long	3014665830
	.long	1065444562
	.long	2982428624
	.long	1065490608
	.long	3015478276
	.long	1065536902
	.long	865643564
	.long	1065583450
	.long	3014725705
	.long	1065630248
	.long	868461790
	.long	1065677302
	.long	858550072
	.long	1065724612
	.long	3013096376
	.long	1065772178
	.long	3013897664
	.long	1065820002
	.long	3008545510
	.long	1065868086
	.long	3011512679
	.long	1065916430
	.long	864064219
	.long	1065965038
	.long	819487640
	.long	1066013910
	.long	3012212369
	.long	1066063046
	.long	856316133
	.long	1066112450
	.long	2992679841
	.long	1066162122
	.long	3001970243
	.long	1066212064
	.long	3013902756
	.long	1066262276
	.long	847285146
	.long	1066312762
	.long	3010032741
	.long	1066363522
	.long	3018332471
	.long	1066414556
	.long	856041677
	.long	1066465868
	.long	864808677
	.long	1066517460
	.long	3012318446
	.long	1066569330
	.long	863709796
	.long	1066621484
	.long	3016813593
	.long	1066673920
	.long	3018872036
	.long	1066726640
	.long	3006136850
	.long	1066779646
	.long	864474828
	.long	1066832942
	.long	3016286184
	.long	1066886526
	.long	3015052933
	.long	1066940400
	.long	857938801
	.long	1066994568
	.long	2993474036
	.long	1067049030
	.long	3009003152
	.long	1067103786
	.long	872191232
	.long	1067158842
	.long	3003929955
	.long	1067214196
	.long	3013071165
	.long	1067269850
	.long	3006375425
	.long	1067325806
	.long	843377209
	.long	1067382066
	.long	859906882
	.long	1067438632
	.long	848662531
	.long	1067495506
	.long	3018868367
	.long	1067552686
	.long	868910405
	.long	1067610180
	.long	3019699127
	.long	1067667984
	.long	3013023741
	.long	1067726102
	.long	3005475891
	.long	1067784536
	.long	3010626242
	.long	1067843286
	.long	866758993
	.long	1067902356
	.long	869265128
	.long	1067961748
	.long	3004575030
	.long	1068021462
	.long	3018425550
	.long	1068081498
	.long	867494524
	.long	1068141862
	.long	858118433
	.long	1068202554
	.long	3004476802
	.long	1068263574
	.long	866434624
	.long	1068324926
	.long	870990497
	.long	1068386612
	.long	858100843
	.long	1068448632
	.long	867002634
	.long	1068510990
	.long	3000050815
	.long	1068573686
	.long	3011271336
	.long	1068636722
	.long	3006477262
	.long	1068700100
	.long	840255625
	.long	1068763822
	.long	866280780
	.long	1068827892
	.long	3016492578
	.long	1068892308
	.long	3006218836
	.long	1068957074
	.long	2993076596
	.long	1069022192
	.long	3000356208
	.long	1069087664
	.long	3015220484
	.long	1069153490
	.long	856315927
	.long	1069219674
	.long	867308350
	.long	1069286218
	.long	863888852
	.long	1069353124
	.long	3007401960
	.long	1069420392
	.long	832069785
	.long	1069488026
	.long	3004369690
	.long	1069556026
	.long	866250961
	.long	1069624396
	.long	868902513
	.long	1069693138
	.long	851736822
	.long	1069762252
	.long	869934231
	.long	1069831742
	.long	869028661
	.long	1069901610
	.long	839559223
	.long	1069971856
	.long	867543588
	.long	1070042484
	.long	868789178
	.long	1070113496
	.long	859381756
	.long	1070184894
	.long	3010667426
	.long	1070256678
	.long	859604257
	.long	1070328852
	.long	872346226
	.long	1070401420
	.long	3010682756
	.long	1070474380
	.long	841546788
	.long	1070547736
	.long	869210393
	.long	1070621492
	.long	2996061011
	.long	1070695648
	.long	3013455510
	.long	1070770206
	.long	3009158570
	.long	1070845168
	.long	865699227
	.long	1070920538
	.long	866897902
	.long	1070996318
	.long	2955948569
	.long	1071072508
	.long	868931229
	.long	1071149114
	.long	3014890061
	.long	1071226134
	.long	3002473793
	.long	1071303572
	.long	861820308
	.long	1071381432
	.long	3008383516
	.long	1071459714
	.long	3010850715
	.long	1071538420
	.long	864181775
	.long	1071617554
	.long	870234352
	.long	1071697118
	.long	871115413
	.long	1071777114
	.long	872414852
	.long	1071857546
	.long	3012378998
	.long	1071938412
	.long	866137918
	.long	1072019718
	.long	870808707
	.long	1072101466
	.long	866840096
	.long	1072183658
	.long	857766040
	.long	1072266296
	.long	855693471
	.long	1072349382
	.long	870833444
	.long	1072432920
	.long	867585053
	.long	1072516912
	.long	846646433
	.long	1072601360
	.long	3008357562
	.long	1072686266
	.long	3007858250
	.long	1072771632
	.long	866626825
	.long	1072857464
	.long	3015943680
	.long	1072943760
	.long	2995197552
	.long	1073030526
	.long	3018513273
	.long	1073117762
	.long	3012791488
	.long	1073205472
	.long	3012359471
	.long	1073293658
	.long	3003728983
	.long	1073382322
	.long	870019626
	.long	1073471470
	.long	3012762127
	.long	1073561100
	.long	835668076
	.long	1073651218
	.long	3013837936
	.long	980050793
	.long	3199320925
	.long	1042575209
	.long	3182108321
	.long	1060205080
	.long	1047920112
	.long	1029920839
	.long	2130706432
	.long	8388608
	.long	0
	.long	1065353216
	.long	3212836864
	.long	1203765248
	.long	1069056000
	.long	1166018560
	.long	1602224128
	.long	528482304
	.long	0
	.long	2147483648
	.type	_vmlsPowHATab,@object
	.size	_vmlsPowHATab,1520
	.align 16
.L_2il0floatpacket.40:
	.long	0x00000000,0x43380000,0x00000000,0x43380000
	.type	.L_2il0floatpacket.40,@object
	.size	.L_2il0floatpacket.40,16
	.align 4
.L_2il0floatpacket.143:
	.long	0xbf800000
	.type	.L_2il0floatpacket.143,@object
	.size	.L_2il0floatpacket.143,4
	.align 4
.L_2il0floatpacket.144:
	.long	0x5f800000
	.type	.L_2il0floatpacket.144,@object
	.size	.L_2il0floatpacket.144,4
	.align 4
.L_2il0floatpacket.145:
	.long	0x3fb88000
	.type	.L_2il0floatpacket.145,@object
	.size	.L_2il0floatpacket.145,4
	.align 4
.L_2il0floatpacket.146:
	.long	0x45800800
	.type	.L_2il0floatpacket.146,@object
	.size	.L_2il0floatpacket.146,4
	.align 4
.L_2il0floatpacket.147:
	.long	0x3a6a6369
	.type	.L_2il0floatpacket.147,@object
	.size	.L_2il0floatpacket.147,4
	.align 4
.L_2il0floatpacket.148:
	.long	0xbeb1c35d
	.type	.L_2il0floatpacket.148,@object
	.size	.L_2il0floatpacket.148,4
	.align 4
.L_2il0floatpacket.149:
	.long	0x3e246f69
	.type	.L_2il0floatpacket.149,@object
	.size	.L_2il0floatpacket.149,4
	.align 4
.L_2il0floatpacket.150:
	.long	0xbdab1ea1
	.type	.L_2il0floatpacket.150,@object
	.size	.L_2il0floatpacket.150,4
	.align 4
.L_2il0floatpacket.151:
	.long	0x47c00000
	.type	.L_2il0floatpacket.151,@object
	.size	.L_2il0floatpacket.151,4
	.align 4
.L_2il0floatpacket.152:
	.long	0x3f317218
	.type	.L_2il0floatpacket.152,@object
	.size	.L_2il0floatpacket.152,4
	.align 4
.L_2il0floatpacket.153:
	.long	0x3e75fdf0
	.type	.L_2il0floatpacket.153,@object
	.size	.L_2il0floatpacket.153,4
	.align 4
.L_2il0floatpacket.154:
	.long	0x3d635847
	.type	.L_2il0floatpacket.154,@object
	.size	.L_2il0floatpacket.154,4
	.align 4
.L_2il0floatpacket.155:
	.long	0x00800000
	.type	.L_2il0floatpacket.155,@object
	.size	.L_2il0floatpacket.155,4
	.align 4
.L_2il0floatpacket.156:
	.long	0x7f000000
	.type	.L_2il0floatpacket.156,@object
	.size	.L_2il0floatpacket.156,4
	.align 4
.L_2il0floatpacket.157:
	.long	0x1f800000
	.type	.L_2il0floatpacket.157,@object
	.size	.L_2il0floatpacket.157,4
	.align 4
.L_2il0floatpacket.158:
	.long	0x3f800000
	.type	.L_2il0floatpacket.158,@object
	.size	.L_2il0floatpacket.158,4
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
