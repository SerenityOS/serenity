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
	.file "svml_s_sin.c"
	.text
..TXTST0:
.L_2__routine_start___svml_sinf16_ha_z0_0:
# -- Begin  __svml_sinf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinf16_ha_z0
# --- __svml_sinf16_ha_z0(__m512)
__svml_sinf16_ha_z0:
# parameter 1: %zmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #660.1
        .byte     15                                            #990.35
        .byte     30                                            #990.35
        .byte     250                                           #990.35
	.cfi_startproc
..___tag_value___svml_sinf16_ha_z0.1:
..L2:
                                                          #660.1
        pushq     %rbp                                          #660.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #660.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #660.1
        subq      $192, %rsp                                    #660.1
        xorl      %esi, %esi                                    #677.1
        vmovups   4096+__svml_ssin_ha_data_internal(%rip), %zmm3 #675.47
        vmovups   5056+__svml_ssin_ha_data_internal(%rip), %zmm6 #698.53
        vmovups   4544+__svml_ssin_ha_data_internal(%rip), %zmm7 #699.56
        vmovups   5120+__svml_ssin_ha_data_internal(%rip), %zmm10 #704.53
        vmovups   5184+__svml_ssin_ha_data_internal(%rip), %zmm13 #706.53
        vmovups   5440+__svml_ssin_ha_data_internal(%rip), %zmm15 #710.52
        vmovaps   %zmm0, %zmm11                                 #660.1
        vandps    %zmm3, %zmm11, %zmm4                          #676.12
        vfmadd213ps {rn-sae}, %zmm7, %zmm4, %zmm6               #700.17
        vcvtps2pd {sae}, %ymm4, %zmm12                          #697.23
        vextractf32x8 $1, %zmm4, %ymm5                          #697.139
        vsubps    {rn-sae}, %zmm7, %zmm6, %zmm8                 #702.19
        vpslld    $31, %zmm6, %zmm2                             #701.41
        vcvtps2pd {sae}, %ymm5, %zmm14                          #697.116
        vmovups   5376+__svml_ssin_ha_data_internal(%rip), %zmm7 #711.52
        vmovaps   %zmm7, %zmm5                                  #712.19
        vextractf32x8 $1, %zmm8, %ymm9                          #703.136
        vcvtps2pd {sae}, %ymm8, %zmm1                           #703.23
        vcvtps2pd {sae}, %ymm9, %zmm0                           #703.113
        vandnps   %zmm11, %zmm3, %zmm9                          #709.21
        vfnmadd231pd {rn-sae}, %zmm1, %zmm10, %zmm12            #705.19
        vfnmadd231pd {rn-sae}, %zmm0, %zmm10, %zmm14            #705.133
        vmovups   5248+__svml_ssin_ha_data_internal(%rip), %zmm3 #715.52
        vfnmadd213pd {rn-sae}, %zmm12, %zmm13, %zmm1            #707.19
        vfnmadd213pd {rn-sae}, %zmm14, %zmm13, %zmm0            #707.133
        vmovups   4160+__svml_ssin_ha_data_internal(%rip), %zmm14 #723.57
        vxorps    %zmm9, %zmm2, %zmm13                          #719.20
        vmulpd    {rn-sae}, %zmm1, %zmm1, %zmm6                 #708.22
        vmulpd    {rn-sae}, %zmm0, %zmm0, %zmm8                 #708.129
        vcmpps    $18, {sae}, %zmm14, %zmm4, %k1                #724.115
        vfmadd231pd {rn-sae}, %zmm6, %zmm15, %zmm5              #712.19
        vfmadd231pd {rn-sae}, %zmm8, %zmm15, %zmm7              #712.133
        vmovups   5312+__svml_ssin_ha_data_internal(%rip), %zmm15 #713.52
        vfmadd213pd {rn-sae}, %zmm15, %zmm6, %zmm5              #714.19
        vfmadd213pd {rn-sae}, %zmm15, %zmm8, %zmm7              #714.132
        vfmadd213pd {rn-sae}, %zmm3, %zmm6, %zmm5               #716.19
        vfmadd213pd {rn-sae}, %zmm3, %zmm8, %zmm7               #716.132
        vmulpd    {rn-sae}, %zmm6, %zmm5, %zmm10                #717.21
        vmulpd    {rn-sae}, %zmm8, %zmm7, %zmm12                #717.128
        vfmadd213pd {rn-sae}, %zmm1, %zmm1, %zmm10              #718.21
        vfmadd213pd {rn-sae}, %zmm0, %zmm0, %zmm12              #718.134
        vcvtpd2ps {rn-sae}, %zmm10, %ymm1                       #720.18
        vcvtpd2ps {rn-sae}, %zmm12, %ymm0                       #720.165
        vpternlogd $255, %zmm10, %zmm10, %zmm10                 #724.47
        vpandnd   %zmm4, %zmm4, %zmm10{%k1}                     #724.47
        vptestmd  %zmm10, %zmm10, %k0                           #726.63
        kmovw     %k0, %eax                                     #726.63
        vinsertf32x8 $1, %ymm0, %zmm1, %zmm2                    #720.116
        vxorps    %zmm13, %zmm2, %zmm0                          #721.18
        testl     %eax, %eax                                    #727.62
        jne       ..B1.17       # Prob 5%                       #727.62
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm4 zmm10 zmm11
..B1.2:                         # Preds ..B1.17 ..B1.1
                                # Execution count [1.00e+00]
        testl     %esi, %esi                                    #993.52
        jne       ..B1.4        # Prob 5%                       #993.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11
..B1.3:                         # Preds ..B1.10 ..B1.8 ..B1.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #996.12
        popq      %rbp                                          #996.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #996.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.4:                         # Preds ..B1.2
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #993.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11
..B1.5:                         # Preds ..B1.4
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #993.249
        movl      %edx, %eax                                    #993.303
        orl       $8064, %eax                                   #993.303
        cmpl      %eax, %edx                                    #993.332
        je        ..B1.7        # Prob 78%                      #993.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm11
..B1.6:                         # Preds ..B1.5
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #993.344
        vldmxcsr  32(%rsp)                                      #993.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm11
..B1.7:                         # Preds ..B1.6 ..B1.5
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm11, 64(%rsp)                              #993.404
        vmovups   %zmm0, 128(%rsp)                              #993.478
        testl     %esi, %esi                                    #993.586
        jne       ..B1.12       # Prob 5%                       #993.586
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B1.8:                         # Preds ..B1.15 ..B1.7
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #993.931
        je        ..B1.3        # Prob 78%                      #993.931
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B1.9:                         # Preds ..B1.8
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #993.956
        movl      32(%rsp), %eax                                #993.956
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B1.10:                        # Preds ..B1.9
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #993.943
        orl       %edx, %eax                                    #993.943
        movl      %eax, 32(%rsp)                                #993.943
        vldmxcsr  32(%rsp)                                      #993.943
        jmp       ..B1.3        # Prob 100%                     #993.943
                                # LOE rbx r12 r13 r14 r15 zmm0
..B1.12:                        # Preds ..B1.7
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #993.668
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B1.20:                        # Preds ..B1.12
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
..B1.13:                        # Preds ..B1.14 ..B1.20
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #993.731
        jc        ..B1.16       # Prob 5%                       #993.731
                                # LOE rbx r15 r12d r13d r14d
..B1.14:                        # Preds ..B1.16 ..B1.13
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #993.684
        cmpl      $16, %ebx                                     #993.679
        jl        ..B1.13       # Prob 82%                      #993.679
                                # LOE rbx r15 r12d r13d r14d
..B1.15:                        # Preds ..B1.14
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
        vmovups   128(%rsp), %zmm0                              #993.886
        jmp       ..B1.8        # Prob 100%                     #993.886
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B1.16:                        # Preds ..B1.13
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,4), %rdi                         #993.760
        lea       128(%rsp,%rbx,4), %rsi                        #993.760
..___tag_value___svml_sinf16_ha_z0.29:
#       __svml_ssin_ha_cout_rare_internal(const float *, float *)
        call      __svml_ssin_ha_cout_rare_internal             #993.760
..___tag_value___svml_sinf16_ha_z0.30:
        jmp       ..B1.14       # Prob 100%                     #993.760
	.cfi_restore 3
	.cfi_restore 12
	.cfi_restore 13
	.cfi_restore 14
                                # LOE r15 ebx r12d r13d r14d
..B1.17:                        # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   4224+__svml_ssin_ha_data_internal(%rip), %zmm8 #752.52
        lea       __svml_ssin_ha_reduction_data_internal(%rip), %rax #844.328
        vmovups   %zmm0, (%rsp)                                 #[spill]
        vpternlogd $255, %zmm1, %zmm1, %zmm1                    #754.42
        vandps    %zmm4, %zmm8, %zmm6                           #753.16
        vcmpps    $4, {sae}, %zmm8, %zmm6, %k1                  #754.110
        lea       __svml_ssin_ha_data_internal(%rip), %rdx      #963.125
        vpandd    .L_2il0floatpacket.20(%rip), %zmm11, %zmm12   #842.16
        vpsrld    $23, %zmm12, %zmm13                           #843.18
        vpslld    $1, %zmm13, %zmm4                             #844.140
        vpaddd    %zmm13, %zmm4, %zmm9                          #844.200
        vpslld    $2, %zmm9, %zmm5                              #844.268
        vpandnd   %zmm6, %zmm6, %zmm1{%k1}                      #754.42
        vptestmd  %zmm1, %zmm1, %k0                             #756.33
        vmovups   .L_2il0floatpacket.23(%rip), %zmm6            #854.15
        kxnorw    %k0, %k0, %k3                                 #845.329
        kxnorw    %k0, %k0, %k4                                 #846.328
        kxnorw    %k0, %k0, %k2                                 #844.328
        kxnorw    %k0, %k0, %k1                                 #964.125
        kxnorw    %k0, %k0, %k7                                 #963.125
        kmovw     %k0, %esi                                     #756.33
        vpxord    %zmm3, %zmm3, %zmm3                           #845.329
        vpxord    %zmm14, %zmm14, %zmm14                        #846.328
        vpxord    %zmm8, %zmm8, %zmm8                           #844.328
        vgatherdps 4(%rax,%zmm5), %zmm3{%k3}                    #845.329
        vgatherdps 8(%rax,%zmm5), %zmm14{%k4}                   #846.328
        vgatherdps (%rax,%zmm5), %zmm8{%k2}                     #844.328
        vpsrld    $16, %zmm3, %zmm15                            #857.12
        vpsrld    $16, %zmm14, %zmm13                           #859.12
        vpsrld    $16, %zmm8, %zmm7                             #855.12
        kxnorw    %k0, %k0, %k2                                 #965.127
        kxnorw    %k0, %k0, %k3                                 #983.125
        vpandd    .L_2il0floatpacket.21(%rip), %zmm11, %zmm2    #852.19
        vpaddd    .L_2il0floatpacket.22(%rip), %zmm2, %zmm1     #853.19
        vpsrld    $16, %zmm1, %zmm9                             #861.12
        vpandd    %zmm6, %zmm8, %zmm0                           #856.10
        vpandd    %zmm6, %zmm3, %zmm12                          #858.10
        vpandd    %zmm6, %zmm14, %zmm5                          #860.10
        vpandd    %zmm6, %zmm1, %zmm14                          #862.10
        vpmulld   %zmm0, %zmm9, %zmm4                           #864.13
        vpmulld   %zmm15, %zmm9, %zmm3                          #865.13
        vpmulld   %zmm12, %zmm9, %zmm2                          #866.13
        vpmulld   %zmm13, %zmm9, %zmm1                          #867.13
        vpmulld   %zmm5, %zmm9, %zmm8                           #868.13
        vpmulld   %zmm15, %zmm14, %zmm9                         #871.13
        vpmulld   %zmm13, %zmm14, %zmm15                        #873.13
        vpmulld   %zmm7, %zmm14, %zmm7                          #869.13
        vpmulld   %zmm0, %zmm14, %zmm5                          #870.13
        vpmulld   %zmm12, %zmm14, %zmm0                         #872.13
        vpsrld    $16, %zmm15, %zmm14                           #875.13
        vpsrld    $16, %zmm9, %zmm15                            #877.13
        vpsrld    $16, %zmm0, %zmm13                            #876.13
        vpsrld    $16, %zmm5, %zmm12                            #878.13
        vpaddd    %zmm15, %zmm3, %zmm3                          #881.11
        vpaddd    %zmm14, %zmm1, %zmm1                          #879.11
        vpaddd    %zmm13, %zmm2, %zmm14                         #880.11
        vpaddd    %zmm12, %zmm4, %zmm2                          #882.11
        vpandd    %zmm6, %zmm5, %zmm4                           #885.11
        vpaddd    %zmm3, %zmm4, %zmm3                           #890.11
        vpsrld    $16, %zmm8, %zmm4                             #893.13
        vpandd    %zmm6, %zmm0, %zmm0                           #883.11
        vpaddd    %zmm1, %zmm0, %zmm13                          #888.11
        vpandd    %zmm6, %zmm7, %zmm7                           #886.11
        vpaddd    %zmm2, %zmm7, %zmm0                           #891.11
        vpaddd    %zmm13, %zmm4, %zmm7                          #894.11
        vpandd    %zmm6, %zmm9, %zmm12                          #884.11
        vpsrld    $16, %zmm7, %zmm9                             #895.13
        vpaddd    %zmm14, %zmm12, %zmm5                         #889.11
        vpaddd    %zmm5, %zmm9, %zmm1                           #896.11
        vpsrld    $16, %zmm1, %zmm2                             #897.13
        vpslld    $16, %zmm1, %zmm13                            #904.19
        vpaddd    %zmm3, %zmm2, %zmm15                          #898.11
        vpsrld    $16, %zmm15, %zmm8                            #899.13
        vpaddd    %zmm0, %zmm8, %zmm14                          #900.11
        vpandd    %zmm6, %zmm7, %zmm12                          #901.11
        vpandd    %zmm6, %zmm15, %zmm4                          #902.11
        vpslld    $16, %zmm14, %zmm6                            #903.18
        vpaddd    %zmm12, %zmm13, %zmm13                        #906.17
        vpaddd    %zmm4, %zmm6, %zmm9                           #905.16
        vmovups   .L_2il0floatpacket.26(%rip), %zmm6            #914.15
        vpsrld    $9, %zmm9, %zmm2                              #911.20
        vpandd    .L_2il0floatpacket.29(%rip), %zmm13, %zmm15   #924.18
        vpslld    $5, %zmm15, %zmm1                             #925.20
        vpsrld    $18, %zmm13, %zmm13                           #935.19
        vpandd    .L_2il0floatpacket.24(%rip), %zmm11, %zmm5    #908.15
        vpxord    .L_2il0floatpacket.25(%rip), %zmm5, %zmm8     #910.20
        vpord     %zmm8, %zmm2, %zmm3                           #912.18
        vaddps    {rn-sae}, %zmm3, %zmm6, %zmm7                 #915.21
        vsubps    {rn-sae}, %zmm6, %zmm7, %zmm0                 #916.11
        vsubps    {rn-sae}, %zmm0, %zmm3, %zmm14                #917.19
        vandps    .L_2il0floatpacket.34(%rip), %zmm11, %zmm3    #953.11
        vpternlogd $255, %zmm0, %zmm0, %zmm0                    #955.41
        vpxord    .L_2il0floatpacket.28(%rip), %zmm5, %zmm8     #922.20
        vpord     %zmm8, %zmm1, %zmm2                           #926.18
        vpandd    .L_2il0floatpacket.31(%rip), %zmm9, %zmm9     #933.18
        vsubps    {rn-sae}, %zmm8, %zmm2, %zmm15                #929.19
        vpxord    .L_2il0floatpacket.30(%rip), %zmm5, %zmm2     #931.20
        vpslld    $14, %zmm9, %zmm5                             #934.20
        vpord     %zmm13, %zmm5, %zmm5                          #936.18
        vpord     %zmm2, %zmm5, %zmm9                           #937.18
        vsubps    {rn-sae}, %zmm2, %zmm9, %zmm8                 #940.20
        vmovups   .L_2il0floatpacket.32(%rip), %zmm9            #945.20
        vmovups   .L_2il0floatpacket.33(%rip), %zmm2            #946.21
        vaddps    {rn-sae}, %zmm8, %zmm14, %zmm6                #941.13
        vsubps    {rn-sae}, %zmm6, %zmm14, %zmm14               #942.19
        vmovaps   %zmm9, %zmm5                                  #948.17
        vaddps    {rn-sae}, %zmm14, %zmm8, %zmm13               #943.20
        vmovups   .L_2il0floatpacket.35(%rip), %zmm8            #952.16
        vaddps    {rn-sae}, %zmm15, %zmm13, %zmm14              #944.13
        vpternlogd $255, %zmm15, %zmm15, %zmm15                 #954.38
        vcmpps    $26, {sae}, %zmm8, %zmm3, %k5                 #954.106
        vcmpps    $22, {sae}, %zmm8, %zmm3, %k6                 #955.109
        vpandd    .L_2il0floatpacket.27(%rip), %zmm7, %zmm4     #920.13
        vmulps    {rn-sae}, %zmm9, %zmm6, %zmm7                 #947.19
        vfmsub213ps {rn-sae}, %zmm7, %zmm6, %zmm5               #948.17
        vfmadd213ps {rn-sae}, %zmm5, %zmm2, %zmm6               #949.17
        vfmadd213ps {rn-sae}, %zmm6, %zmm9, %zmm14              #950.17
        vpslld    $4, %zmm4, %zmm9                              #963.77
        vpandnd   %zmm3, %zmm3, %zmm15{%k5}                     #954.38
        vandps    %zmm7, %zmm15, %zmm12                         #957.25
        vandps    %zmm14, %zmm15, %zmm2                         #959.9
        vpxord    %zmm5, %zmm5, %zmm5                           #964.125
        vpxord    %zmm7, %zmm7, %zmm7                           #965.127
        vgatherdps 4(%rdx,%zmm9), %zmm5{%k1}                    #964.125
        vgatherdps 12(%rdx,%zmm9), %zmm7{%k2}                   #965.127
        vpandnd   %zmm3, %zmm3, %zmm0{%k6}                      #955.41
        vandps    %zmm11, %zmm0, %zmm1                          #956.24
        vorps     %zmm12, %zmm1, %zmm13                         #958.9
        vmovups   4352+__svml_ssin_ha_data_internal(%rip), %zmm12 #973.46
        vmulps    {rn-sae}, %zmm13, %zmm13, %zmm3               #962.17
        vmovaps   %zmm13, %zmm8                                 #966.22
        vfmadd213ps {rn-sae}, %zmm5, %zmm7, %zmm8               #966.22
        vmovaps   %zmm13, %zmm4                                 #967.21
        vsubps    {rn-sae}, %zmm8, %zmm5, %zmm1                 #968.20
        vfmadd231ps {rn-sae}, %zmm13, %zmm7, %zmm1              #969.18
        vpxord    %zmm0, %zmm0, %zmm0                           #963.125
        vgatherdps (%rdx,%zmm9), %zmm0{%k7}                     #963.125
        vfmadd132ps {rn-sae}, %zmm0, %zmm8, %zmm4               #967.21
        vsubps    {rn-sae}, %zmm4, %zmm8, %zmm6                 #970.20
        vfmadd231ps {rn-sae}, %zmm0, %zmm13, %zmm6              #971.18
        vaddps    {rn-sae}, %zmm1, %zmm6, %zmm8                 #972.24
        vaddps    {rn-sae}, %zmm7, %zmm0, %zmm1                 #978.15
        vmovups   4288+__svml_ssin_ha_data_internal(%rip), %zmm6 #974.46
        vmovups   4480+__svml_ssin_ha_data_internal(%rip), %zmm0 #979.46
        vmovups   4416+__svml_ssin_ha_data_internal(%rip), %zmm7 #980.46
        vfmadd231ps {rn-sae}, %zmm3, %zmm12, %zmm6              #975.14
        vfmadd231ps {rn-sae}, %zmm3, %zmm0, %zmm7               #981.14
        vmulps    {rn-sae}, %zmm3, %zmm6, %zmm14                #976.16
        vmulps    {rn-sae}, %zmm3, %zmm7, %zmm0                 #982.16
        vmulps    {rn-sae}, %zmm13, %zmm14, %zmm6               #977.16
        vfnmadd213ps {rn-sae}, %zmm1, %zmm5, %zmm13             #984.13
        vfmadd213ps {rn-sae}, %zmm8, %zmm13, %zmm6              #987.18
        vpxord    %zmm3, %zmm3, %zmm3                           #983.125
        vgatherdps 8(%rdx,%zmm9), %zmm3{%k3}                    #983.125
        vfmadd213ps {rn-sae}, %zmm3, %zmm13, %zmm2              #985.16
        vfmadd213ps {rn-sae}, %zmm2, %zmm5, %zmm0               #986.17
        vaddps    {rn-sae}, %zmm6, %zmm0, %zmm2                 #988.19
        vaddps    {rn-sae}, %zmm2, %zmm4, %zmm4                 #989.22
        vpandnd   (%rsp), %zmm10, %zmm0                         #990.52[spill]
        vpandd    %zmm10, %zmm4, %zmm10                         #990.153
        vpord     %zmm10, %zmm0, %zmm0                          #990.35
        jmp       ..B1.2        # Prob 100%                     #990.35
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11
	.cfi_endproc
# mark_end;
	.type	__svml_sinf16_ha_z0,@function
	.size	__svml_sinf16_ha_z0,.-__svml_sinf16_ha_z0
..LN__svml_sinf16_ha_z0.0:
	.data
# -- End  __svml_sinf16_ha_z0
	.text
.L_2__routine_start___svml_sinf8_ha_l9_1:
# -- Begin  __svml_sinf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinf8_ha_l9
# --- __svml_sinf8_ha_l9(__m256)
__svml_sinf8_ha_l9:
# parameter 1: %ymm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1034.1
        .byte     15                                            #1364.14
        .byte     30                                            #1364.14
        .byte     250                                           #1364.14
	.cfi_startproc
..___tag_value___svml_sinf8_ha_l9.38:
..L39:
                                                         #1034.1
        pushq     %rbp                                          #1034.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1034.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1034.1
        pushq     %r13                                          #1034.1
        subq      $184, %rsp                                    #1034.1
        xorl      %eax, %eax                                    #1051.1
        vmovups   4096+__svml_ssin_ha_data_internal(%rip), %ymm3 #1049.48
        vmovups   5056+__svml_ssin_ha_data_internal(%rip), %ymm6 #1072.54
        vmovups   4544+__svml_ssin_ha_data_internal(%rip), %ymm7 #1073.57
        vmovupd   5120+__svml_ssin_ha_data_internal(%rip), %ymm10 #1078.55
        vmovupd   5184+__svml_ssin_ha_data_internal(%rip), %ymm12 #1080.55
        vmovupd   5376+__svml_ssin_ha_data_internal(%rip), %ymm15 #1085.54
        vmovaps   %ymm0, %ymm14                                 #1034.1
        vandps    %ymm3, %ymm14, %ymm4                          #1050.12
        vfmadd213ps %ymm7, %ymm4, %ymm6                         #1074.17
        vcvtps2pd %xmm4, %ymm11                                 #1071.23
        vextractf128 $1, %ymm4, %xmm5                           #1071.108
        vsubps    %ymm7, %ymm6, %ymm8                           #1076.17
        vpslld    $31, %ymm6, %ymm2                             #1075.41
        vcvtps2pd %xmm5, %ymm13                                 #1071.91
        vmovupd   5312+__svml_ssin_ha_data_internal(%rip), %ymm5 #1087.54
        vmovupd   5248+__svml_ssin_ha_data_internal(%rip), %ymm6 #1089.54
        vextractf128 $1, %ymm8, %xmm9                           #1077.105
        vcvtps2pd %xmm8, %ymm1                                  #1077.23
        vcvtps2pd %xmm9, %ymm0                                  #1077.88
        vfnmadd231pd %ymm1, %ymm10, %ymm11                      #1079.19
        vfnmadd231pd %ymm0, %ymm10, %ymm13                      #1079.78
        vfnmadd213pd %ymm11, %ymm12, %ymm1                      #1081.19
        vfnmadd213pd %ymm13, %ymm12, %ymm0                      #1081.78
        vmulpd    %ymm1, %ymm1, %ymm7                           #1082.20
        vmulpd    %ymm0, %ymm0, %ymm9                           #1082.66
        vandnps   %ymm14, %ymm3, %ymm10                         #1083.21
        vmovupd   5440+__svml_ssin_ha_data_internal(%rip), %ymm3 #1084.54
        vmovapd   %ymm15, %ymm8                                 #1086.19
        vfmadd231pd %ymm7, %ymm3, %ymm8                         #1086.19
        vfmadd231pd %ymm9, %ymm3, %ymm15                        #1086.78
        vfmadd213pd %ymm5, %ymm7, %ymm8                         #1088.19
        vfmadd213pd %ymm5, %ymm9, %ymm15                        #1088.77
        vfmadd213pd %ymm6, %ymm7, %ymm8                         #1090.19
        vfmadd213pd %ymm6, %ymm9, %ymm15                        #1090.77
        vmulpd    %ymm8, %ymm7, %ymm11                          #1091.19
        vmulpd    %ymm15, %ymm9, %ymm12                         #1091.65
        vfmadd213pd %ymm1, %ymm1, %ymm11                        #1092.21
        vfmadd213pd %ymm0, %ymm0, %ymm12                        #1092.79
        vcvtpd2ps %ymm11, %xmm0                                 #1094.64
        vcvtpd2ps %ymm12, %xmm1                                 #1094.100
        vxorps    %ymm10, %ymm2, %ymm13                         #1093.20
        vinsertf128 $1, %xmm1, %ymm0, %ymm2                     #1094.18
        vxorps    %ymm13, %ymm2, %ymm0                          #1095.18
        vcmpnle_uqps 4160+__svml_ssin_ha_data_internal(%rip), %ymm4, %ymm13 #1098.26
        vmovmskps %ymm13, %edx                                  #1100.75
        testl     %edx, %edx                                    #1101.62
        jne       ..B2.12       # Prob 5%                       #1101.62
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r14 r15 eax ymm0 ymm4 ymm13 ymm14
..B2.2:                         # Preds ..B2.12 ..B2.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #1367.52
        jne       ..B2.4        # Prob 5%                       #1367.52
                                # LOE rbx r12 r14 r15 eax ymm0 ymm14
..B2.3:                         # Preds ..B2.4 ..B2.10 ..B2.2
                                # Execution count [1.00e+00]
        addq      $184, %rsp                                    #1370.12
	.cfi_restore 13
        popq      %r13                                          #1370.12
        movq      %rbp, %rsp                                    #1370.12
        popq      %rbp                                          #1370.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1370.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE
..B2.4:                         # Preds ..B2.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm14, 64(%rsp)                              #1367.196
        vmovups   %ymm0, 128(%rsp)                              #1367.270
        je        ..B2.3        # Prob 95%                      #1367.374
                                # LOE rbx r12 r14 r15 eax ymm0
..B2.7:                         # Preds ..B2.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %r13d, %r13d                                  #1367.454
                                # LOE rbx r12 r13 r14 r15 eax
..B2.15:                        # Preds ..B2.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
                                # LOE rbx r13 r14 r15 r12d
..B2.8:                         # Preds ..B2.9 ..B2.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r13d, %r12d                                  #1367.517
        jc        ..B2.11       # Prob 5%                       #1367.517
                                # LOE rbx r13 r14 r15 r12d
..B2.9:                         # Preds ..B2.11 ..B2.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r13d                                         #1367.470
        cmpl      $8, %r13d                                     #1367.465
        jl        ..B2.8        # Prob 82%                      #1367.465
                                # LOE rbx r13 r14 r15 r12d
..B2.10:                        # Preds ..B2.9
                                # Execution count [2.25e-03]: Infreq
        movq      (%rsp), %r12                                  #[spill]
	.cfi_restore 12
        vmovups   128(%rsp), %ymm0                              #1367.674
        jmp       ..B2.3        # Prob 100%                     #1367.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r14 r15 ymm0
..B2.11:                        # Preds ..B2.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r13,4), %rdi                         #1367.546
        lea       128(%rsp,%r13,4), %rsi                        #1367.546
..___tag_value___svml_sinf8_ha_l9.54:
#       __svml_ssin_ha_cout_rare_internal(const float *, float *)
        call      __svml_ssin_ha_cout_rare_internal             #1367.546
..___tag_value___svml_sinf8_ha_l9.55:
        jmp       ..B2.9        # Prob 100%                     #1367.546
	.cfi_restore 12
                                # LOE rbx r14 r15 r12d r13d
..B2.12:                        # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vpand     .L_2il0floatpacket.36(%rip), %ymm14, %ymm6    #1216.18
        lea       __svml_ssin_ha_reduction_data_internal(%rip), %rcx #1218.1212
        vmovups   4224+__svml_ssin_ha_data_internal(%rip), %ymm8 #1126.53
        vmovups   %ymm0, (%rsp)                                 #[spill]
        vpsrld    $23, %ymm6, %ymm3                             #1217.18
        vpslld    $1, %ymm3, %ymm7                              #1218.234
        vandps    %ymm4, %ymm8, %ymm10                          #1127.16
        vcmpeqps  %ymm8, %ymm10, %ymm12                         #1128.21
        vpaddd    %ymm3, %ymm7, %ymm10                          #1218.296
        vpslld    $2, %ymm10, %ymm9                             #1218.373
        vmovmskps %ymm12, %eax                                  #1130.45
        vextractf128 $1, %ymm9, %xmm1                           #1218.834
        vmovd     %xmm9, %esi                                   #1218.446
        vmovd     %xmm1, %r11d                                  #1218.815
        vmovd     (%rsi,%rcx), %xmm8                            #1218.2300
        vpextrd   $1, %xmm9, %edi                               #1218.536
        vpextrd   $2, %xmm9, %r8d                               #1218.629
        vpextrd   $3, %xmm9, %r9d                               #1218.722
        vpextrd   $1, %xmm1, %r10d                              #1218.910
        vpextrd   $2, %xmm1, %r13d                              #1218.1008
        vpextrd   $3, %xmm1, %edx                               #1218.1106
        vmovd     (%rdi,%rcx), %xmm5                            #1218.2389
        vmovd     (%r8,%rcx), %xmm11                            #1218.2499
        vmovd     (%r9,%rcx), %xmm12                            #1218.2588
        vmovd     (%r11,%rcx), %xmm2                            #1218.2741
        vmovd     (%r10,%rcx), %xmm0                            #1218.2830
        vmovd     (%r13,%rcx), %xmm7                            #1218.2940
        vmovd     (%rdx,%rcx), %xmm3                            #1218.3029
        vpunpcklqdq %xmm5, %xmm8, %xmm15                        #1218.2280
        vpunpcklqdq %xmm12, %xmm11, %xmm6                       #1218.2479
        vpunpcklqdq %xmm0, %xmm2, %xmm10                        #1218.2721
        vpunpcklqdq %xmm3, %xmm7, %xmm9                         #1218.2920
        vshufps   $136, %xmm6, %xmm15, %xmm4                    #1218.2260
        vmovd     4(%rsi,%rcx), %xmm5                           #1219.2301
        vmovd     4(%rdi,%rcx), %xmm11                          #1219.2390
        vmovd     4(%r8,%rcx), %xmm12                           #1219.2500
        vmovd     4(%r9,%rcx), %xmm15                           #1219.2589
        vshufps   $136, %xmm9, %xmm10, %xmm8                    #1218.2701
        vmovd     4(%r11,%rcx), %xmm0                           #1219.2742
        vmovd     4(%r10,%rcx), %xmm7                           #1219.2831
        vmovd     4(%r13,%rcx), %xmm3                           #1219.2941
        vmovd     4(%rdx,%rcx), %xmm9                           #1219.3030
        vpunpcklqdq %xmm11, %xmm5, %xmm6                        #1219.2281
        vpunpcklqdq %xmm15, %xmm12, %xmm1                       #1219.2480
        vpunpcklqdq %xmm9, %xmm3, %xmm5                         #1219.2921
        vshufps   $136, %xmm1, %xmm6, %xmm2                     #1219.2261
        vmovd     8(%r8,%rcx), %xmm6                            #1220.2499
        vmovd     8(%rsi,%rcx), %xmm12                          #1220.2300
        vmovd     8(%rdi,%rcx), %xmm15                          #1220.2389
        vmovd     8(%r11,%rcx), %xmm3                           #1220.2741
        lea       __svml_ssin_ha_data_internal(%rip), %r11      #1337.1007
        vpunpcklqdq %xmm15, %xmm12, %xmm1                       #1220.2280
        vinsertf128 $1, %xmm8, %ymm4, %ymm10                    #1218.2196
        vpunpcklqdq %xmm7, %xmm0, %xmm8                         #1219.2722
        vmovd     8(%r9,%rcx), %xmm4                            #1220.2588
        vshufps   $136, %xmm5, %xmm8, %xmm11                    #1219.2702
        vpunpcklqdq %xmm4, %xmm6, %xmm0                         #1220.2479
        vpand     .L_2il0floatpacket.37(%rip), %ymm14, %ymm4    #1226.21
        vmovd     8(%r10,%rcx), %xmm8                           #1220.2830
        vmovd     8(%r13,%rcx), %xmm5                           #1220.2940
        vpunpcklqdq %xmm8, %xmm3, %xmm12                        #1220.2721
        vshufps   $136, %xmm0, %xmm1, %xmm7                     #1220.2260
        vpaddd    .L_2il0floatpacket.38(%rip), %ymm4, %ymm1     #1227.21
        vinsertf128 $1, %xmm11, %ymm2, %ymm9                    #1219.2197
        vmovd     8(%rdx,%rcx), %xmm11                          #1220.3029
        vpunpcklqdq %xmm11, %xmm5, %xmm15                       #1220.2920
        vshufps   $136, %xmm15, %xmm12, %xmm6                   #1220.2701
        vmovdqu   .L_2il0floatpacket.39(%rip), %ymm12           #1228.17
        vpsrld    $16, %ymm9, %ymm4                             #1231.12
        vpand     %ymm12, %ymm9, %ymm2                          #1232.12
        vpsrld    $16, %ymm1, %ymm9                             #1235.12
        vinsertf128 $1, %xmm6, %ymm7, %ymm11                    #1220.2196
        vpand     %ymm12, %ymm11, %ymm5                         #1234.12
        vpsrld    $16, %ymm11, %ymm0                            #1233.12
        vpand     %ymm12, %ymm1, %ymm6                          #1236.12
        vpand     %ymm12, %ymm10, %ymm7                         #1230.12
        vpsrld    $16, %ymm10, %ymm10                           #1229.12
        vpmulld   %ymm5, %ymm9, %ymm11                          #1242.13
        vpsrld    $16, %ymm11, %ymm15                           #1267.13
        vpmulld   %ymm0, %ymm9, %ymm11                          #1241.13
        vpmulld   %ymm0, %ymm6, %ymm0                           #1247.13
        vpsrld    $16, %ymm0, %ymm0                             #1249.13
        vpmulld   %ymm2, %ymm6, %ymm1                           #1246.13
        vpand     %ymm12, %ymm1, %ymm5                          #1257.13
        vpaddd    %ymm0, %ymm11, %ymm11                         #1253.13
        vpaddd    %ymm11, %ymm5, %ymm5                          #1262.13
        vpaddd    %ymm5, %ymm15, %ymm11                         #1268.13
        vpsrld    $16, %ymm1, %ymm15                            #1250.13
        vpsrld    $16, %ymm11, %ymm0                            #1269.13
        vpand     %ymm12, %ymm11, %ymm11                        #1275.13
        vpmulld   %ymm4, %ymm6, %ymm3                           #1245.13
        vpmulld   %ymm2, %ymm9, %ymm2                           #1240.13
        vpand     %ymm12, %ymm3, %ymm5                          #1258.13
        vpaddd    %ymm15, %ymm2, %ymm1                          #1254.13
        vpsrld    $16, %ymm3, %ymm3                             #1251.13
        vpaddd    %ymm1, %ymm5, %ymm5                           #1263.13
        vpmulld   %ymm7, %ymm6, %ymm8                           #1244.13
        vpmulld   %ymm4, %ymm9, %ymm4                           #1239.13
        vpaddd    %ymm5, %ymm0, %ymm5                           #1270.13
        vpand     %ymm12, %ymm8, %ymm1                          #1259.13
        vpaddd    %ymm3, %ymm4, %ymm3                           #1255.13
        vpand     .L_2il0floatpacket.40(%rip), %ymm14, %ymm0    #1282.17
        vpsrld    $16, %ymm5, %ymm2                             #1271.13
        vpslld    $16, %ymm5, %ymm5                             #1278.19
        vpaddd    %ymm3, %ymm1, %ymm15                          #1264.13
        vpmulld   %ymm10, %ymm6, %ymm3                          #1243.13
        vpsrld    $16, %ymm8, %ymm10                            #1252.13
        vpaddd    %ymm15, %ymm2, %ymm4                          #1272.13
        vpand     %ymm12, %ymm3, %ymm6                          #1260.13
        vpaddd    %ymm11, %ymm5, %ymm2                          #1280.19
        vpsrld    $16, %ymm4, %ymm15                            #1273.13
        vpand     %ymm12, %ymm4, %ymm4                          #1276.13
        vpand     .L_2il0floatpacket.44(%rip), %ymm2, %ymm5     #1298.20
        vpsrld    $18, %ymm2, %ymm2                             #1309.19
        vpslld    $5, %ymm5, %ymm11                             #1299.20
        vpmulld   %ymm7, %ymm9, %ymm7                           #1238.13
        vpaddd    %ymm10, %ymm7, %ymm9                          #1256.13
        vpxor     .L_2il0floatpacket.41(%rip), %ymm0, %ymm7     #1284.22
        vmovups   .L_2il0floatpacket.42(%rip), %ymm10           #1288.15
        vpaddd    %ymm9, %ymm6, %ymm8                           #1265.13
        vpaddd    %ymm8, %ymm15, %ymm15                         #1274.13
        vpslld    $16, %ymm15, %ymm1                            #1277.18
        vpxor     .L_2il0floatpacket.43(%rip), %ymm0, %ymm15    #1296.22
        vpaddd    %ymm4, %ymm1, %ymm3                           #1279.18
        vpsrld    $9, %ymm3, %ymm12                             #1285.20
        vpor      %ymm7, %ymm12, %ymm9                          #1286.20
        vpor      %ymm15, %ymm11, %ymm12                        #1300.20
        vaddps    %ymm10, %ymm9, %ymm7                          #1289.19
        vsubps    %ymm15, %ymm12, %ymm4                         #1303.17
        vmovups   .L_2il0floatpacket.47(%rip), %ymm15           #1319.20
        vsubps    %ymm10, %ymm7, %ymm8                          #1290.9
        vpand     .L_2il0floatpacket.51(%rip), %ymm7, %ymm7     #1294.15
        vsubps    %ymm8, %ymm9, %ymm1                           #1291.17
        vpxor     .L_2il0floatpacket.45(%rip), %ymm0, %ymm9     #1305.22
        vpand     .L_2il0floatpacket.46(%rip), %ymm3, %ymm0     #1307.20
        vpslld    $4, %ymm7, %ymm7                              #1337.179
        vpslld    $14, %ymm0, %ymm3                             #1308.20
        vpor      %ymm2, %ymm3, %ymm3                           #1310.20
        vpor      %ymm9, %ymm3, %ymm10                          #1311.20
        vsubps    %ymm9, %ymm10, %ymm8                          #1314.18
        vaddps    %ymm8, %ymm1, %ymm6                           #1315.11
        vsubps    %ymm6, %ymm1, %ymm5                           #1316.17
        vmovups   .L_2il0floatpacket.50(%rip), %ymm1            #1326.16
        vaddps    %ymm5, %ymm8, %ymm11                          #1317.18
        vaddps    %ymm11, %ymm4, %ymm8                          #1318.11
        vmulps    %ymm6, %ymm15, %ymm4                          #1321.17
        vmovaps   %ymm15, %ymm12                                #1322.17
        vfmsub213ps %ymm4, %ymm6, %ymm12                        #1322.17
        vfmadd132ps .L_2il0floatpacket.48(%rip), %ymm12, %ymm6  #1323.17
        vandps    .L_2il0floatpacket.49(%rip), %ymm14, %ymm0    #1327.11
        vcmpgt_oqps %ymm1, %ymm0, %ymm9                         #1328.17
        vcmple_oqps %ymm1, %ymm0, %ymm2                         #1329.20
        vfmadd213ps %ymm6, %ymm15, %ymm8                        #1324.17
        vandps    %ymm14, %ymm2, %ymm3                          #1330.24
        vandps    %ymm4, %ymm9, %ymm10                          #1331.25
        vorps     %ymm10, %ymm3, %ymm3                          #1332.9
        vandps    %ymm8, %ymm9, %ymm10                          #1333.9
        vmulps    %ymm3, %ymm3, %ymm6                           #1336.15
        vextractf128 $1, %ymm7, %xmm1                           #1337.629
        vmovd     %xmm7, %r10d                                  #1337.241
        vmovd     %xmm1, %esi                                   #1337.610
        vmovd     (%r10,%r11), %xmm9                            #1337.1974
        vpextrd   $1, %xmm7, %r9d                               #1337.331
        vpextrd   $2, %xmm7, %r8d                               #1337.424
        vpextrd   $3, %xmm7, %edi                               #1337.517
        vpextrd   $1, %xmm1, %ecx                               #1337.705
        vpextrd   $2, %xmm1, %edx                               #1337.803
        vpextrd   $3, %xmm1, %r13d                              #1337.901
        vmovd     (%r9,%r11), %xmm8                             #1337.2063
        vmovd     (%r8,%r11), %xmm5                             #1337.2173
        vmovd     (%rdi,%r11), %xmm11                           #1337.2262
        vpunpcklqdq %xmm8, %xmm9, %xmm12                        #1337.1954
        vmovd     (%rsi,%r11), %xmm2                            #1337.2415
        vmovd     (%rcx,%r11), %xmm4                            #1337.2504
        vmovd     (%rdx,%r11), %xmm7                            #1337.2614
        vmovd     (%r13,%r11), %xmm9                            #1337.2703
        vpunpcklqdq %xmm11, %xmm5, %xmm15                       #1337.2153
        vpunpcklqdq %xmm4, %xmm2, %xmm8                         #1337.2395
        vpunpcklqdq %xmm9, %xmm7, %xmm5                         #1337.2594
        vshufps   $136, %xmm15, %xmm12, %xmm0                   #1337.1934
        vshufps   $136, %xmm5, %xmm8, %xmm11                    #1337.2375
        vmovd     4(%r10,%r11), %xmm12                          #1338.1974
        vmovd     4(%r9,%r11), %xmm1                            #1338.2063
        vmovd     4(%r8,%r11), %xmm2                            #1338.2173
        vmovd     4(%rdi,%r11), %xmm4                           #1338.2262
        vpunpcklqdq %xmm1, %xmm12, %xmm7                        #1338.1954
        vmovd     4(%rsi,%r11), %xmm5                           #1338.2415
        vmovd     4(%rdx,%r11), %xmm12                          #1338.2614
        vpunpcklqdq %xmm4, %xmm2, %xmm9                         #1338.2153
        vshufps   $136, %xmm9, %xmm7, %xmm8                     #1338.1934
        vmovd     12(%r10,%r11), %xmm7                          #1339.1976
        vinsertf128 $1, %xmm11, %ymm0, %ymm15                   #1337.1870
        vmovd     4(%rcx,%r11), %xmm11                          #1338.2504
        vmovd     4(%r13,%r11), %xmm0                           #1338.2703
        vpunpcklqdq %xmm11, %xmm5, %xmm1                        #1338.2395
        vpunpcklqdq %xmm0, %xmm12, %xmm2                        #1338.2594
        vshufps   $136, %xmm2, %xmm1, %xmm4                     #1338.2375
        vmovd     12(%r8,%r11), %xmm5                           #1339.2175
        vmovd     12(%rdi,%r11), %xmm11                         #1339.2264
        vmovd     12(%rsi,%r11), %xmm2                          #1339.2417
        vpunpcklqdq %xmm11, %xmm5, %xmm0                        #1339.2155
        vinsertf128 $1, %xmm4, %ymm8, %ymm9                     #1338.1870
        vmovd     12(%r9,%r11), %xmm8                           #1339.2065
        vpunpcklqdq %xmm8, %xmm7, %xmm12                        #1339.1956
        vmovd     12(%rcx,%r11), %xmm4                          #1339.2506
        vmovd     12(%rdx,%r11), %xmm7                          #1339.2616
        vmovd     12(%r13,%r11), %xmm8                          #1339.2705
        vpunpcklqdq %xmm4, %xmm2, %xmm5                         #1339.2397
        vpunpcklqdq %xmm8, %xmm7, %xmm11                        #1339.2596
        vshufps   $136, %xmm0, %xmm12, %xmm1                    #1339.1936
        vshufps   $136, %xmm11, %xmm5, %xmm12                   #1339.2377
        vmovaps   %ymm3, %ymm8                                  #1340.22
        vmovaps   %ymm15, %ymm7                                 #1341.21
        vmovups   4352+__svml_ssin_ha_data_internal(%rip), %ymm2 #1347.47
        vfmadd213ps 4288+__svml_ssin_ha_data_internal(%rip), %ymm6, %ymm2 #1349.14
        vmulps    %ymm2, %ymm6, %ymm4                           #1350.14
        vinsertf128 $1, %xmm12, %ymm1, %ymm11                   #1339.1872
        vfmadd213ps %ymm9, %ymm11, %ymm8                        #1340.22
        vmovd     8(%rdi,%r11), %xmm1                           #1357.2262
        vfmadd213ps %ymm8, %ymm3, %ymm7                         #1341.21
        vsubps    %ymm8, %ymm9, %ymm5                           #1342.18
        vsubps    %ymm7, %ymm8, %ymm0                           #1344.18
        vfmadd231ps %ymm3, %ymm11, %ymm5                        #1343.18
        vfmadd231ps %ymm15, %ymm3, %ymm0                        #1345.18
        vaddps    %ymm11, %ymm15, %ymm11                        #1352.13
        vmovups   4480+__svml_ssin_ha_data_internal(%rip), %ymm15 #1355.14
        vaddps    %ymm0, %ymm5, %ymm8                           #1346.22
        vmovd     8(%r8,%r11), %xmm0                            #1357.2173
        vfmadd213ps 4416+__svml_ssin_ha_data_internal(%rip), %ymm6, %ymm15 #1355.14
        vmulps    %ymm4, %ymm3, %ymm5                           #1351.14
        vpunpcklqdq %xmm1, %xmm0, %xmm4                         #1357.2153
        vmovd     8(%rcx,%r11), %xmm0                           #1357.2504
        vmovd     8(%rdx,%r11), %xmm1                           #1357.2614
        vmulps    %ymm15, %ymm6, %ymm12                         #1356.14
        vmovd     8(%r10,%r11), %xmm6                           #1357.1974
        vmovd     8(%r9,%r11), %xmm15                           #1357.2063
        vpunpcklqdq %xmm15, %xmm6, %xmm2                        #1357.1954
        vshufps   $136, %xmm4, %xmm2, %xmm15                    #1357.1934
        vfnmadd213ps %ymm11, %ymm9, %ymm3                       #1358.13
        vmovd     8(%rsi,%r11), %xmm6                           #1357.2415
        vmovd     8(%r13,%r11), %xmm2                           #1357.2703
        vpunpcklqdq %xmm0, %xmm6, %xmm4                         #1357.2395
        vpunpcklqdq %xmm2, %xmm1, %xmm6                         #1357.2594
        vshufps   $136, %xmm6, %xmm4, %xmm0                     #1357.2375
        vfmadd213ps %ymm8, %ymm3, %ymm5                         #1361.18
        vinsertf128 $1, %xmm0, %ymm15, %ymm15                   #1357.1870
        vfmadd213ps %ymm15, %ymm3, %ymm10                       #1359.16
        vmovups   (%rsp), %ymm0                                 #1364.14[spill]
        vfmadd213ps %ymm10, %ymm9, %ymm12                       #1360.17
        vaddps    %ymm5, %ymm12, %ymm3                          #1362.17
        vaddps    %ymm3, %ymm7, %ymm10                          #1363.20
        vblendvps %ymm13, %ymm10, %ymm0, %ymm0                  #1364.14
        jmp       ..B2.2        # Prob 100%                     #1364.14
        .align    16,0x90
                                # LOE rbx r12 r14 r15 eax ymm0 ymm14
	.cfi_endproc
# mark_end;
	.type	__svml_sinf8_ha_l9,@function
	.size	__svml_sinf8_ha_l9,.-__svml_sinf8_ha_l9
..LN__svml_sinf8_ha_l9.1:
	.data
# -- End  __svml_sinf8_ha_l9
	.text
.L_2__routine_start___svml_sinf4_ha_e9_2:
# -- Begin  __svml_sinf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinf4_ha_e9
# --- __svml_sinf4_ha_e9(__m128)
__svml_sinf4_ha_e9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1376.1
        .byte     15                                            #1719.14
        .byte     30                                            #1719.14
        .byte     250                                           #1719.14
	.cfi_startproc
..___tag_value___svml_sinf4_ha_e9.60:
..L61:
                                                         #1376.1
        pushq     %rbp                                          #1376.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1376.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1376.1
        subq      $192, %rsp                                    #1376.1
        vmovaps   %xmm0, %xmm14                                 #1376.1
        vmovups   4096+__svml_ssin_ha_data_internal(%rip), %xmm11 #1391.45
        xorl      %eax, %eax                                    #1393.1
        vandps    %xmm11, %xmm14, %xmm1                         #1392.12
        vmulps    5056+__svml_ssin_ha_data_internal(%rip), %xmm1, %xmm2 #1416.29
        vcvtps2pd %xmm1, %ymm6                                  #1413.23
        vmovups   4544+__svml_ssin_ha_data_internal(%rip), %xmm4 #1415.54
        vaddps    %xmm2, %xmm4, %xmm3                           #1416.17
        vpslld    $31, %xmm3, %xmm0                             #1417.38
        vsubps    %xmm4, %xmm3, %xmm5                           #1418.17
        vcvtps2pd %xmm5, %ymm8                                  #1419.23
        vandnps   %xmm14, %xmm11, %xmm5                         #1425.21
        vmulpd    5120+__svml_ssin_ha_data_internal(%rip), %ymm8, %ymm7 #1421.39
        vmulpd    5184+__svml_ssin_ha_data_internal(%rip), %ymm8, %ymm10 #1423.39
        vsubpd    %ymm7, %ymm6, %ymm9                           #1421.17
        vxorps    %xmm5, %xmm0, %xmm7                           #1435.20
        vsubpd    %ymm10, %ymm9, %ymm3                          #1423.17
        vmulpd    %ymm3, %ymm3, %ymm2                           #1424.18
        vmulpd    5440+__svml_ssin_ha_data_internal(%rip), %ymm2, %ymm12 #1428.32
        vaddpd    5376+__svml_ssin_ha_data_internal(%rip), %ymm12, %ymm13 #1428.17
        vmulpd    %ymm13, %ymm2, %ymm15                         #1430.32
        vaddpd    5312+__svml_ssin_ha_data_internal(%rip), %ymm15, %ymm12 #1430.17
        vmulpd    %ymm12, %ymm2, %ymm13                         #1432.32
        vaddpd    5248+__svml_ssin_ha_data_internal(%rip), %ymm13, %ymm15 #1432.17
        vmulpd    %ymm15, %ymm2, %ymm11                         #1433.17
        vmulpd    %ymm11, %ymm3, %ymm4                          #1434.34
        vaddpd    %ymm4, %ymm3, %ymm6                           #1434.19
        vcvtpd2ps %ymm6, %xmm0                                  #1436.18
        vxorps    %xmm7, %xmm0, %xmm2                           #1437.18
        vcmpnleps 4160+__svml_ssin_ha_data_internal(%rip), %xmm1, %xmm0 #1440.26
        vmovmskps %xmm0, %edx                                   #1442.74
        testl     %edx, %edx                                    #1443.62
        jne       ..B3.12       # Prob 5%                       #1443.62
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1 xmm2 xmm14
..B3.2:                         # Preds ..B3.12 ..B3.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #1722.52
        jne       ..B3.4        # Prob 5%                       #1722.52
                                # LOE rbx r12 r13 r14 r15 eax xmm2 xmm14
..B3.15:                        # Preds ..B3.4 ..B3.2
                                # Execution count [9.50e-01]
        vzeroupper                                              #
                                # LOE rbx r12 r13 r14 r15 xmm2
..B3.3:                         # Preds ..B3.10 ..B3.15
                                # Execution count [1.00e+00]
        movaps    %xmm2, %xmm0                                  #1725.12
        movq      %rbp, %rsp                                    #1725.12
        popq      %rbp                                          #1725.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1725.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.4:                         # Preds ..B3.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm14, 64(%rsp)                              #1722.193
        vmovups   %xmm2, 128(%rsp)                              #1722.264
        je        ..B3.15       # Prob 95%                      #1722.368
                                # LOE rbx r12 r13 r14 r15 eax xmm2
..B3.7:                         # Preds ..B3.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1722.448
                                # LOE rbx r12 r13 r14 r15 eax edx
..B3.17:                        # Preds ..B3.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.9 ..B3.17
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1722.511
        jc        ..B3.11       # Prob 5%                       #1722.511
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.11 ..B3.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1722.464
        cmpl      $4, %r12d                                     #1722.459
        jl        ..B3.8        # Prob 82%                      #1722.459
                                # LOE rbx r12 r14 r15 r13d
..B3.10:                        # Preds ..B3.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm2                              #1722.665
        jmp       ..B3.3        # Prob 100%                     #1722.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm2
..B3.11:                        # Preds ..B3.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1722.540
        lea       128(%rsp,%r12,4), %rsi                        #1722.540
..___tag_value___svml_sinf4_ha_e9.78:
#       __svml_ssin_ha_cout_rare_internal(const float *, float *)
        call      __svml_ssin_ha_cout_rare_internal             #1722.540
..___tag_value___svml_sinf4_ha_e9.79:
        jmp       ..B3.9        # Prob 100%                     #1722.540
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B3.12:                        # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        movl      $2139095040, %edx                             #1557.24
        lea       __svml_ssin_ha_reduction_data_internal(%rip), %r9 #1560.752
        vmovups   4224+__svml_ssin_ha_data_internal(%rip), %xmm13 #1468.50
        movl      $8388607, %r10d                               #1566.27
        vandps    %xmm1, %xmm13, %xmm11                         #1469.16
        movl      $8388608, %r11d                               #1567.26
        vcmpeqps  %xmm13, %xmm11, %xmm10                        #1470.21
        vmovd     %edx, %xmm5                                   #1557.24
        movl      $65535, %edx                                  #1570.23
        vpshufd   $0, %xmm5, %xmm9                              #1557.24
        vpand     %xmm14, %xmm9, %xmm6                          #1558.22
        vpsrld    $23, %xmm6, %xmm8                             #1559.22
        vpslld    $1, %xmm8, %xmm12                             #1560.238
        vpaddd    %xmm8, %xmm12, %xmm15                         #1560.391
        vpslld    $2, %xmm15, %xmm11                            #1560.391
        vmovd     %xmm11, %ecx                                  #1560.465
        vmovmskps %xmm10, %eax                                  #1472.44
        vmovups   %xmm2, (%rsp)                                 #[spill]
        vmovd     4(%rcx,%r9), %xmm3                            #1561.1281
        vpextrd   $1, %xmm11, %esi                              #1560.533
        vpextrd   $2, %xmm11, %edi                              #1560.604
        vpextrd   $3, %xmm11, %r8d                              #1560.675
        vmovd     4(%rsi,%r9), %xmm1                            #1561.1364
        vmovd     (%rsi,%r9), %xmm4                             #1560.1363
        vmovd     (%rcx,%r9), %xmm13                            #1560.1280
        vmovd     (%rdi,%r9), %xmm7                             #1560.1467
        vmovd     4(%rdi,%r9), %xmm9                            #1561.1468
        vmovd     (%r8,%r9), %xmm10                             #1560.1550
        vmovd     4(%r8,%r9), %xmm6                             #1561.1551
        vpunpcklqdq %xmm1, %xmm3, %xmm12                        #1561.1261
        vmovd     %r10d, %xmm3                                  #1566.27
        vpunpcklqdq %xmm4, %xmm13, %xmm5                        #1560.1260
        movl      $511, %r10d                                   #1648.26
        vpunpcklqdq %xmm10, %xmm7, %xmm2                        #1560.1447
        vmovd     8(%r8,%r9), %xmm10                            #1562.1550
        movl      $262143, %r8d                                 #1639.26
        vpunpcklqdq %xmm6, %xmm9, %xmm8                         #1561.1448
        vmovd     %r11d, %xmm9                                  #1567.26
        vmovd     8(%rcx,%r9), %xmm15                           #1562.1280
        movl      $-2147483648, %ecx                            #1623.23
        vmovd     8(%rsi,%r9), %xmm13                           #1562.1363
        movl      $1065353216, %esi                             #1625.28
        vmovd     8(%rdi,%r9), %xmm4                            #1562.1467
        movl      $679477248, %edi                              #1637.28
        vpshufd   $0, %xmm3, %xmm1                              #1566.27
        movl      $872415232, %r9d                              #1646.28
        vshufps   $136, %xmm2, %xmm5, %xmm11                    #1560.1240
        vpand     %xmm14, %xmm1, %xmm6                          #1568.25
        vshufps   $136, %xmm8, %xmm12, %xmm7                    #1561.1241
        movl      $255, %r11d                                   #1635.21
        vpunpcklqdq %xmm13, %xmm15, %xmm5                       #1562.1260
        vmovd     %edx, %xmm15                                  #1570.23
        vpunpcklqdq %xmm10, %xmm4, %xmm2                        #1562.1447
        vpshufd   $0, %xmm9, %xmm8                              #1567.26
        vshufps   $136, %xmm2, %xmm5, %xmm12                    #1562.1240
        vpaddd    %xmm8, %xmm6, %xmm4                           #1569.25
        vpshufd   $0, %xmm15, %xmm13                            #1570.23
        vpsrld    $16, %xmm7, %xmm5                             #1573.16
        vpand     %xmm13, %xmm7, %xmm3                          #1574.16
        vpsrld    $16, %xmm12, %xmm1                            #1575.16
        vpsrld    $16, %xmm4, %xmm10                            #1577.16
        vpand     %xmm13, %xmm4, %xmm7                          #1578.16
        vpand     %xmm13, %xmm12, %xmm12                        #1576.16
        vpand     %xmm13, %xmm11, %xmm8                         #1572.16
        vpmulld   %xmm12, %xmm10, %xmm6                         #1584.17
        vpsrld    $16, %xmm11, %xmm11                           #1571.16
        vpmulld   %xmm1, %xmm10, %xmm12                         #1583.17
        vpsrld    $16, %xmm6, %xmm15                            #1609.17
        vpmulld   %xmm1, %xmm7, %xmm1                           #1589.17
        vpmulld   %xmm3, %xmm7, %xmm2                           #1588.17
        vpsrld    $16, %xmm1, %xmm1                             #1591.17
        vpand     %xmm13, %xmm2, %xmm6                          #1599.17
        vpaddd    %xmm1, %xmm12, %xmm12                         #1595.17
        vpmulld   %xmm5, %xmm7, %xmm4                           #1587.17
        vpaddd    %xmm12, %xmm6, %xmm6                          #1604.17
        vpmulld   %xmm3, %xmm10, %xmm3                          #1582.17
        vpsrld    $16, %xmm2, %xmm2                             #1592.17
        vpaddd    %xmm6, %xmm15, %xmm12                         #1610.17
        vpand     %xmm13, %xmm4, %xmm1                          #1600.17
        vpaddd    %xmm2, %xmm3, %xmm15                          #1596.17
        vpsrld    $16, %xmm12, %xmm6                            #1611.17
        vpmulld   %xmm8, %xmm7, %xmm9                           #1586.17
        vpaddd    %xmm15, %xmm1, %xmm2                          #1605.17
        vpmulld   %xmm5, %xmm10, %xmm5                          #1581.17
        vpsrld    $16, %xmm4, %xmm15                            #1593.17
        vpaddd    %xmm2, %xmm6, %xmm6                           #1612.17
        vpand     %xmm13, %xmm9, %xmm2                          #1601.17
        vpaddd    %xmm15, %xmm5, %xmm4                          #1597.17
        vpsrld    $16, %xmm6, %xmm3                             #1613.17
        vpmulld   %xmm11, %xmm7, %xmm7                          #1585.17
        vpaddd    %xmm4, %xmm2, %xmm5                           #1606.17
        vpmulld   %xmm8, %xmm10, %xmm8                          #1580.17
        vpsrld    $16, %xmm9, %xmm9                             #1594.17
        vpaddd    %xmm5, %xmm3, %xmm4                           #1614.17
        vpand     %xmm13, %xmm7, %xmm11                         #1602.17
        vpaddd    %xmm9, %xmm8, %xmm7                           #1619.22
        vpsrld    $16, %xmm4, %xmm15                            #1615.17
        vpaddd    %xmm7, %xmm11, %xmm10                         #1619.22
        vpand     %xmm13, %xmm4, %xmm4                          #1618.17
        vpaddd    %xmm10, %xmm15, %xmm15                        #1619.22
        vpand     %xmm13, %xmm12, %xmm12                        #1617.17
        vmovd     %ecx, %xmm13                                  #1623.23
        vpslld    $16, %xmm15, %xmm5                            #1619.22
        vpshufd   $0, %xmm13, %xmm2                             #1623.23
        vmovd     %esi, %xmm3                                   #1625.28
        vpshufd   $0, %xmm3, %xmm9                              #1625.28
        vpaddd    %xmm4, %xmm5, %xmm8                           #1621.22
        vpslld    $16, %xmm6, %xmm6                             #1620.23
        vpand     %xmm14, %xmm2, %xmm1                          #1624.21
        vpaddd    %xmm12, %xmm6, %xmm6                          #1622.23
        vpsrld    $9, %xmm8, %xmm12                             #1627.24
        vpxor     %xmm9, %xmm1, %xmm15                          #1626.26
        vmovd     %edi, %xmm7                                   #1637.28
        vmovups   .L_2il0floatpacket.52(%rip), %xmm11           #1630.15
        vpor      %xmm15, %xmm12, %xmm13                        #1628.24
        vmovd     %r8d, %xmm5                                   #1639.26
        vaddps    %xmm11, %xmm13, %xmm9                         #1631.19
        vpshufd   $0, %xmm7, %xmm10                             #1637.28
        vsubps    %xmm11, %xmm9, %xmm4                          #1632.9
        vpshufd   $0, %xmm5, %xmm2                              #1639.26
        vsubps    %xmm4, %xmm13, %xmm3                          #1633.17
        vmovd     %r9d, %xmm4                                   #1646.28
        vpxor     %xmm10, %xmm1, %xmm13                         #1638.26
        vpshufd   $0, %xmm4, %xmm7                              #1646.28
        vpand     %xmm6, %xmm2, %xmm12                          #1640.24
        vpxor     %xmm7, %xmm1, %xmm4                           #1647.26
        vmovd     %r10d, %xmm1                                  #1648.26
        vpslld    $5, %xmm12, %xmm15                            #1641.24
        vpsrld    $18, %xmm6, %xmm6                             #1651.23
        vpshufd   $0, %xmm1, %xmm12                             #1648.26
        vpor      %xmm13, %xmm15, %xmm11                        #1642.24
        vpand     %xmm8, %xmm12, %xmm8                          #1649.24
        vsubps    %xmm13, %xmm11, %xmm2                         #1645.17
        vpslld    $14, %xmm8, %xmm15                            #1650.24
        lea       __svml_ssin_ha_data_internal(%rip), %r8       #1690.533
        vpor      %xmm6, %xmm15, %xmm11                         #1652.24
        vpor      %xmm4, %xmm11, %xmm13                         #1653.24
        vmovups   .L_2il0floatpacket.55(%rip), %xmm12           #1663.21
        vsubps    %xmm4, %xmm13, %xmm7                          #1656.18
        vaddps    %xmm7, %xmm3, %xmm1                           #1657.11
        vsubps    %xmm1, %xmm3, %xmm10                          #1658.17
        vandps    .L_2il0floatpacket.56(%rip), %xmm1, %xmm3     #1665.20
        vmulps    %xmm3, %xmm12, %xmm11                         #1669.16
        vsubps    %xmm3, %xmm1, %xmm8                           #1666.21
        vaddps    %xmm10, %xmm7, %xmm5                          #1659.18
        vmulps    %xmm8, %xmm12, %xmm4                          #1671.16
        vaddps    %xmm5, %xmm2, %xmm6                           #1660.11
        vmovups   .L_2il0floatpacket.54(%rip), %xmm2            #1662.20
        vmulps    %xmm8, %xmm2, %xmm15                          #1668.16
        vmulps    .L_2il0floatpacket.53(%rip), %xmm6, %xmm13    #1670.16
        vmulps    %xmm3, %xmm2, %xmm5                           #1667.20
        vaddps    %xmm11, %xmm15, %xmm7                         #1672.17
        vaddps    %xmm4, %xmm13, %xmm10                         #1673.17
        vmovups   .L_2il0floatpacket.58(%rip), %xmm3            #1679.16
        vaddps    %xmm10, %xmm7, %xmm1                          #1674.17
        vandps    .L_2il0floatpacket.57(%rip), %xmm14, %xmm2    #1680.11
        vaddps    %xmm1, %xmm5, %xmm12                          #1675.17
        vcmpleps  %xmm3, %xmm2, %xmm6                           #1682.20
        vcmpltps  %xmm2, %xmm3, %xmm11                          #1681.17
        vsubps    %xmm12, %xmm5, %xmm5                          #1676.20
        vandps    %xmm14, %xmm6, %xmm8                          #1683.24
        vmovd     %r11d, %xmm6                                  #1635.21
        vandps    %xmm12, %xmm11, %xmm15                        #1684.25
        vaddps    %xmm5, %xmm1, %xmm13                          #1677.17
        vpshufd   $0, %xmm6, %xmm12                             #1635.21
        vorps     %xmm15, %xmm8, %xmm4                          #1685.9
        vpand     %xmm12, %xmm9, %xmm9                          #1636.19
        vandps    %xmm13, %xmm11, %xmm11                        #1686.9
        vpslld    $4, %xmm9, %xmm9                              #1690.183
        vmovd     %xmm9, %edx                                   #1690.246
        vmulps    %xmm4, %xmm4, %xmm7                           #1689.15
        vpextrd   $1, %xmm9, %ecx                               #1690.314
        vpextrd   $2, %xmm9, %esi                               #1690.385
        vpextrd   $3, %xmm9, %edi                               #1690.456
        vmovd     (%rdx,%r8), %xmm6                             #1690.1000
        vmovd     (%rcx,%r8), %xmm12                            #1690.1083
        vmovd     (%rsi,%r8), %xmm8                             #1690.1187
        vmovd     (%rdi,%r8), %xmm15                            #1690.1270
        vpunpcklqdq %xmm12, %xmm6, %xmm13                       #1690.980
        vpunpcklqdq %xmm15, %xmm8, %xmm10                       #1690.1167
        vmovd     4(%rdx,%r8), %xmm1                            #1691.1000
        vmovd     4(%rcx,%r8), %xmm2                            #1691.1083
        vmovd     12(%rcx,%r8), %xmm15                          #1692.1085
        vshufps   $136, %xmm10, %xmm13, %xmm10                  #1690.960
        vpunpcklqdq %xmm2, %xmm1, %xmm9                         #1691.980
        vmovd     4(%rsi,%r8), %xmm3                            #1691.1187
        vmovd     12(%rsi,%r8), %xmm13                          #1692.1189
        vmovd     4(%rdi,%r8), %xmm5                            #1691.1270
        vmovd     12(%rdi,%r8), %xmm1                           #1692.1272
        vmovd     12(%rdx,%r8), %xmm12                          #1692.1002
        vpunpcklqdq %xmm5, %xmm3, %xmm6                         #1691.1167
        vpunpcklqdq %xmm15, %xmm12, %xmm2                       #1692.982
        vpunpcklqdq %xmm1, %xmm13, %xmm3                        #1692.1169
        vshufps   $136, %xmm3, %xmm2, %xmm15                    #1692.962
        vmulps    %xmm15, %xmm4, %xmm5                          #1694.22
        vmulps    %xmm10, %xmm4, %xmm13                         #1693.19
        vshufps   $136, %xmm6, %xmm9, %xmm8                     #1691.960
        vaddps    %xmm5, %xmm8, %xmm12                          #1695.22
        vsubps    %xmm12, %xmm8, %xmm6                          #1697.18
        vaddps    %xmm12, %xmm13, %xmm9                         #1696.21
        vaddps    %xmm6, %xmm5, %xmm2                           #1698.18
        vmulps    4352+__svml_ssin_ha_data_internal(%rip), %xmm7, %xmm5 #1704.26
        vsubps    %xmm9, %xmm12, %xmm1                          #1699.18
        vaddps    4288+__svml_ssin_ha_data_internal(%rip), %xmm5, %xmm12 #1704.14
        vaddps    %xmm1, %xmm13, %xmm3                          #1700.18
        vmulps    %xmm12, %xmm7, %xmm13                         #1705.14
        vaddps    %xmm3, %xmm2, %xmm6                           #1701.22
        vmulps    %xmm13, %xmm4, %xmm12                         #1706.14
        vaddps    %xmm15, %xmm10, %xmm13                        #1707.13
        vmulps    4480+__svml_ssin_ha_data_internal(%rip), %xmm7, %xmm10 #1710.26
        vmulps    %xmm8, %xmm4, %xmm4                           #1713.32
        vaddps    4416+__svml_ssin_ha_data_internal(%rip), %xmm10, %xmm15 #1710.14
        vmulps    %xmm15, %xmm7, %xmm15                         #1711.14
        vmovd     8(%rdx,%r8), %xmm7                            #1712.1000
        vmovd     8(%rcx,%r8), %xmm1                            #1712.1083
        vmovd     8(%rsi,%r8), %xmm2                            #1712.1187
        vmovd     8(%rdi,%r8), %xmm3                            #1712.1270
        vpunpcklqdq %xmm1, %xmm7, %xmm5                         #1712.980
        vpunpcklqdq %xmm3, %xmm2, %xmm7                         #1712.1167
        vsubps    %xmm4, %xmm13, %xmm2                          #1713.13
        vmulps    %xmm15, %xmm8, %xmm8                          #1715.29
        vmulps    %xmm2, %xmm11, %xmm11                         #1714.28
        vmulps    %xmm2, %xmm12, %xmm12                         #1716.30
        vshufps   $136, %xmm7, %xmm5, %xmm10                    #1712.960
        vaddps    %xmm12, %xmm6, %xmm4                          #1716.18
        vaddps    %xmm11, %xmm10, %xmm1                         #1714.16
        vmovups   (%rsp), %xmm2                                 #1719.14[spill]
        vaddps    %xmm8, %xmm1, %xmm3                           #1715.17
        vaddps    %xmm4, %xmm3, %xmm5                           #1717.17
        vaddps    %xmm5, %xmm9, %xmm6                           #1718.20
        blendvps  %xmm0, %xmm6, %xmm2                           #1719.14
        jmp       ..B3.2        # Prob 100%                     #1719.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm2 xmm14
	.cfi_endproc
# mark_end;
	.type	__svml_sinf4_ha_e9,@function
	.size	__svml_sinf4_ha_e9,.-__svml_sinf4_ha_e9
..LN__svml_sinf4_ha_e9.2:
	.data
# -- End  __svml_sinf4_ha_e9
	.text
.L_2__routine_start___svml_sinf8_ha_e9_3:
# -- Begin  __svml_sinf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinf8_ha_e9
# --- __svml_sinf8_ha_e9(__m256)
__svml_sinf8_ha_e9:
# parameter 1: %ymm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1731.1
        .byte     15                                            #2074.14
        .byte     30                                            #2074.14
        .byte     250                                           #2074.14
	.cfi_startproc
..___tag_value___svml_sinf8_ha_e9.85:
..L86:
                                                         #1731.1
        pushq     %rbp                                          #1731.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1731.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1731.1
        pushq     %rbx                                          #1731.1
        subq      $312, %rsp                                    #1731.1
        xorl      %eax, %eax                                    #1748.1
        vmovups   4096+__svml_ssin_ha_data_internal(%rip), %ymm7 #1746.48
        vmovups   4544+__svml_ssin_ha_data_internal(%rip), %ymm12 #1770.57
        vandps    %ymm7, %ymm0, %ymm8                           #1747.12
        vmulps    5056+__svml_ssin_ha_data_internal(%rip), %ymm8, %ymm10 #1771.32
        vcvtps2pd %xmm8, %ymm6                                  #1768.23
        vextractf128 $1, %ymm8, %xmm9                           #1768.108
        vaddps    %ymm10, %ymm12, %ymm11                        #1771.17
        vcvtps2pd %xmm9, %ymm5                                  #1768.91
        vmovupd   5120+__svml_ssin_ha_data_internal(%rip), %ymm9 #1775.55
        vpslld    $31, %xmm11, %xmm14                           #1772.84
        vsubps    %ymm12, %ymm11, %ymm1                         #1773.17
        vextractf128 $1, %ymm11, %xmm15                         #1772.215
        vmovupd   5184+__svml_ssin_ha_data_internal(%rip), %ymm12 #1777.55
        vpslld    $31, %xmm15, %xmm13                           #1772.181
        vcvtps2pd %xmm1, %ymm10                                 #1774.23
        vextractf128 $1, %ymm1, %xmm3                           #1774.105
        vmulpd    %ymm9, %ymm10, %ymm2                          #1776.43
        vsubpd    %ymm2, %ymm6, %ymm15                          #1776.19
        vinsertf128 $1, %xmm13, %ymm14, %ymm4                   #1772.20
        vcvtps2pd %xmm3, %ymm13                                 #1774.88
        vmovupd   5312+__svml_ssin_ha_data_internal(%rip), %ymm14 #1784.54
        vmulpd    %ymm9, %ymm13, %ymm6                          #1776.115
        vmulpd    %ymm12, %ymm13, %ymm2                         #1778.115
        vsubpd    %ymm6, %ymm5, %ymm1                           #1776.91
        vmulpd    %ymm12, %ymm10, %ymm5                         #1778.43
        vmovupd   5376+__svml_ssin_ha_data_internal(%rip), %ymm6 #1782.54
        vsubpd    %ymm2, %ymm1, %ymm1                           #1778.91
        vsubpd    %ymm5, %ymm15, %ymm3                          #1778.19
        vmovupd   5440+__svml_ssin_ha_data_internal(%rip), %ymm5 #1781.54
        vmulpd    %ymm1, %ymm1, %ymm2                           #1779.66
        vmulpd    %ymm3, %ymm3, %ymm12                          #1779.20
        vandnps   %ymm0, %ymm7, %ymm13                          #1780.21
        vmulpd    %ymm12, %ymm5, %ymm7                          #1783.34
        vaddpd    %ymm7, %ymm6, %ymm9                           #1783.19
        vmulpd    %ymm2, %ymm5, %ymm7                           #1783.108
        vmulpd    %ymm9, %ymm12, %ymm10                         #1785.34
        vmovupd   5248+__svml_ssin_ha_data_internal(%rip), %ymm9 #1786.54
        vaddpd    %ymm7, %ymm6, %ymm11                          #1783.93
        vaddpd    %ymm10, %ymm14, %ymm5                         #1785.19
        vmulpd    %ymm11, %ymm2, %ymm15                         #1785.107
        vmulpd    %ymm5, %ymm12, %ymm6                          #1787.34
        vaddpd    %ymm15, %ymm14, %ymm7                         #1785.92
        vaddpd    %ymm6, %ymm9, %ymm11                          #1787.19
        vpxor     %xmm6, %xmm6, %xmm6                           #1797.92
        vmulpd    %ymm7, %ymm2, %ymm10                          #1787.107
        vmulpd    %ymm11, %ymm12, %ymm12                        #1788.19
        vaddpd    %ymm10, %ymm9, %ymm14                         #1787.92
        vmulpd    %ymm14, %ymm2, %ymm5                          #1788.65
        vmulpd    %ymm12, %ymm3, %ymm2                          #1789.36
        vaddpd    %ymm2, %ymm3, %ymm12                          #1789.21
        vmulpd    %ymm5, %ymm1, %ymm3                           #1789.109
        vaddpd    %ymm3, %ymm1, %ymm1                           #1789.94
        vxorps    %ymm13, %ymm4, %ymm2                          #1790.20
        vcvtpd2ps %ymm12, %xmm4                                 #1791.64
        vcvtpd2ps %ymm1, %xmm13                                 #1791.100
        vinsertf128 $1, %xmm13, %ymm4, %ymm12                   #1791.18
        vxorps    %ymm2, %ymm12, %ymm13                         #1792.18
        vcmpnle_uqps 4160+__svml_ssin_ha_data_internal(%rip), %ymm8, %ymm12 #1795.26
        vextractf128 $1, %ymm12, %xmm3                          #1796.151
        vpackssdw %xmm3, %xmm12, %xmm5                          #1797.109
        vpacksswb %xmm6, %xmm5, %xmm7                           #1797.92
        vpmovmskb %xmm7, %edx                                   #1797.73
        testb     %dl, %dl                                      #1798.62
        jne       ..B4.12       # Prob 5%                       #1798.62
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE r12 r13 r14 r15 eax ymm0 ymm8 ymm12 ymm13
..B4.2:                         # Preds ..B4.16 ..B4.1
                                # Execution count [1.00e+00]
        testb     %al, %al                                      #2077.52
        jne       ..B4.4        # Prob 5%                       #2077.52
                                # LOE r12 r13 r14 r15 eax ymm0 ymm13
..B4.3:                         # Preds ..B4.4 ..B4.10 ..B4.2
                                # Execution count [1.00e+00]
        vmovaps   %ymm13, %ymm0                                 #2080.12
        addq      $312, %rsp                                    #2080.12
	.cfi_restore 3
        popq      %rbx                                          #2080.12
        movq      %rbp, %rsp                                    #2080.12
        popq      %rbp                                          #2080.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2080.12
	.cfi_def_cfa 6, 16
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
	.cfi_offset 6, -16
                                # LOE
..B4.4:                         # Preds ..B4.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm0, 64(%rsp)                               #2077.196
        vmovups   %ymm13, 128(%rsp)                             #2077.270
        testl     %eax, %eax                                    #2077.374
        je        ..B4.3        # Prob 95%                      #2077.374
                                # LOE r12 r13 r14 r15 eax ymm13
..B4.7:                         # Preds ..B4.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %ebx, %ebx                                    #2077.454
                                # LOE rbx r12 r13 r14 r15 eax
..B4.15:                        # Preds ..B4.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xc0, 0xfe, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
                                # LOE rbx r13 r14 r15 r12d
..B4.8:                         # Preds ..B4.9 ..B4.15
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r12d                                   #2077.517
        jc        ..B4.11       # Prob 5%                       #2077.517
                                # LOE rbx r13 r14 r15 r12d
..B4.9:                         # Preds ..B4.11 ..B4.8
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #2077.470
        cmpl      $8, %ebx                                      #2077.465
        jl        ..B4.8        # Prob 82%                      #2077.465
                                # LOE rbx r13 r14 r15 r12d
..B4.10:                        # Preds ..B4.9
                                # Execution count [2.25e-03]: Infreq
        movq      (%rsp), %r12                                  #[spill]
	.cfi_restore 12
        vmovups   128(%rsp), %ymm13                             #2077.674
        jmp       ..B4.3        # Prob 100%                     #2077.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xc0, 0xfe, 0xff, 0xff, 0x22
                                # LOE r12 r13 r14 r15 ymm13
..B4.11:                        # Preds ..B4.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,4), %rdi                         #2077.546
        lea       128(%rsp,%rbx,4), %rsi                        #2077.546
..___tag_value___svml_sinf8_ha_e9.101:
#       __svml_ssin_ha_cout_rare_internal(const float *, float *)
        call      __svml_ssin_ha_cout_rare_internal             #2077.546
..___tag_value___svml_sinf8_ha_e9.102:
        jmp       ..B4.9        # Prob 100%                     #2077.546
	.cfi_restore 12
                                # LOE r13 r14 r15 ebx r12d
..B4.12:                        # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   4224+__svml_ssin_ha_data_internal(%rip), %ymm1 #1823.53
        movl      $2139095040, %r11d                            #1912.24
        vmovups   %ymm0, (%rsp)                                 #[spill]
        vmovups   %ymm13, 64(%rsp)                              #[spill]
        vpxor     %xmm4, %xmm4, %xmm4                           #1827.62
        vmovups   %ymm12, 32(%rsp)                              #[spill]
        vandps    %ymm8, %ymm1, %ymm2                           #1824.16
        vmovd     %r11d, %xmm8                                  #1912.24
        vcmpeqps  %ymm1, %ymm2, %ymm11                          #1825.21
        lea       __svml_ssin_ha_reduction_data_internal(%rip), %rdx #1915.1256
        vpshufd   $0, %xmm8, %xmm13                             #1912.24
        vpand     %xmm0, %xmm13, %xmm6                          #1913.22
        vextractf128 $1, %ymm0, %xmm15                          #1911.109
        vpsrld    $23, %xmm6, %xmm5                             #1914.22
        vpslld    $1, %xmm5, %xmm12                             #1915.238
        vpaddd    %xmm5, %xmm12, %xmm14                         #1915.543
        vpand     %xmm15, %xmm13, %xmm9                         #1913.97
        vpsrld    $23, %xmm9, %xmm1                             #1914.85
        vpslld    $1, %xmm1, %xmm2                              #1915.304
        vmovups   %xmm0, 96(%rsp)                               #1911.37[spill]
        vmovups   %xmm15, 112(%rsp)                             #1911.109[spill]
        vextractf128 $1, %ymm11, %xmm3                          #1826.128
        vpackssdw %xmm3, %xmm11, %xmm10                         #1827.79
        vpaddd    %xmm1, %xmm2, %xmm11                          #1915.612
        vpslld    $2, %xmm14, %xmm3                             #1915.543
        vpslld    $2, %xmm11, %xmm9                             #1915.612
        vmovd     %xmm3, %ecx                                   #1915.686
        vmovd     %xmm9, %r8d                                   #1915.967
        vpacksswb %xmm4, %xmm10, %xmm7                          #1827.62
        vmovd     (%rcx,%rdx), %xmm10                           #1915.2344
        vpextrd   $1, %xmm3, %ebx                               #1915.754
        vpextrd   $2, %xmm3, %esi                               #1915.825
        vpextrd   $3, %xmm3, %edi                               #1915.896
        vpextrd   $1, %xmm9, %r10d                              #1915.1035
        vpextrd   $2, %xmm9, %r9d                               #1915.1106
        vpextrd   $3, %xmm9, %r11d                              #1915.1177
        vpmovmskb %xmm7, %eax                                   #1827.43
        vmovd     (%rbx,%rdx), %xmm4                            #1915.2433
        vmovd     (%rsi,%rdx), %xmm7                            #1915.2543
        vmovd     (%rdi,%rdx), %xmm8                            #1915.2632
        vmovd     (%r8,%rdx), %xmm12                            #1915.2785
        vmovd     (%r10,%rdx), %xmm5                            #1915.2874
        vmovd     (%r9,%rdx), %xmm14                            #1915.2984
        vmovd     (%r11,%rdx), %xmm2                            #1915.3073
        vpunpcklqdq %xmm4, %xmm10, %xmm6                        #1915.2324
        vpunpcklqdq %xmm8, %xmm7, %xmm13                        #1915.2523
        vpunpcklqdq %xmm5, %xmm12, %xmm1                        #1915.2765
        vpunpcklqdq %xmm2, %xmm14, %xmm3                        #1915.2964
        vmovd     4(%r11,%rdx), %xmm14                          #1916.3074
        vmovd     4(%rcx,%rdx), %xmm10                          #1916.2345
        vmovd     4(%rbx,%rdx), %xmm4                           #1916.2434
        vmovd     4(%rsi,%rdx), %xmm7                           #1916.2544
        vmovd     4(%rdi,%rdx), %xmm8                           #1916.2633
        vmovd     4(%r8,%rdx), %xmm9                            #1916.2786
        vmovd     4(%r10,%rdx), %xmm12                          #1916.2875
        vmovd     4(%r9,%rdx), %xmm5                            #1916.2985
        vshufps   $136, %xmm13, %xmm6, %xmm11                   #1915.2304
        vshufps   $136, %xmm3, %xmm1, %xmm2                     #1915.2745
        vpunpcklqdq %xmm4, %xmm10, %xmm6                        #1916.2325
        vpunpcklqdq %xmm8, %xmm7, %xmm13                        #1916.2524
        vpunpcklqdq %xmm12, %xmm9, %xmm1                        #1916.2766
        vpunpcklqdq %xmm14, %xmm5, %xmm3                        #1916.2965
        vmovd     8(%rcx,%rdx), %xmm4                           #1917.2344
        movl      $262143, %ecx                                 #1994.26
        vmovd     8(%rbx,%rdx), %xmm7                           #1917.2433
        movl      $679477248, %ebx                              #1992.28
        vshufps   $136, %xmm13, %xmm6, %xmm13                   #1916.2305
        vshufps   $136, %xmm3, %xmm1, %xmm10                    #1916.2746
        vpunpcklqdq %xmm7, %xmm4, %xmm9                         #1917.2324
        vmovd     8(%rsi,%rdx), %xmm8                           #1917.2543
        movl      $255, %esi                                    #1990.21
        vmovd     8(%rdi,%rdx), %xmm6                           #1917.2632
        movl      $1065353216, %edi                             #1980.28
        vmovd     8(%r8,%rdx), %xmm5                            #1917.2785
        movl      $-2147483648, %r8d                            #1978.23
        vmovd     8(%r10,%rdx), %xmm14                          #1917.2874
        movl      $65535, %r10d                                 #1925.23
        vmovd     8(%r9,%rdx), %xmm3                            #1917.2984
        movl      $8388608, %r9d                                #1922.26
        vmovd     8(%r11,%rdx), %xmm4                           #1917.3073
        movl      $8388607, %edx                                #1921.27
        vpunpcklqdq %xmm6, %xmm8, %xmm12                        #1917.2523
        movl      $511, %r11d                                   #2003.26
        vshufps   $136, %xmm12, %xmm9, %xmm1                    #1917.2304
        vmovd     %r9d, %xmm9                                   #1922.26
        lea       __svml_ssin_ha_data_internal(%rip), %r9       #2045.878
        vmovd     %edx, %xmm6                                   #1921.27
        movl      $872415232, %edx                              #2001.28
        vpshufd   $0, %xmm6, %xmm12                             #1921.27
        vpunpcklqdq %xmm14, %xmm5, %xmm7                        #1917.2765
        vpand     %xmm0, %xmm12, %xmm0                          #1923.25
        vpshufd   $0, %xmm9, %xmm5                              #1922.26
        vpand     %xmm15, %xmm12, %xmm15                        #1923.106
        vpaddd    %xmm5, %xmm0, %xmm14                          #1924.25
        vmovd     %r10d, %xmm0                                  #1925.23
        vpunpcklqdq %xmm4, %xmm3, %xmm8                         #1917.2964
        vpaddd    %xmm5, %xmm15, %xmm6                          #1924.111
        vpshufd   $0, %xmm0, %xmm15                             #1925.23
        vshufps   $136, %xmm8, %xmm7, %xmm3                     #1917.2745
        vpand     %xmm15, %xmm11, %xmm7                         #1927.16
        vpand     %xmm15, %xmm14, %xmm9                         #1933.16
        vpand     %xmm15, %xmm2, %xmm12                         #1927.83
        vmovups   %xmm2, 144(%rsp)                              #1915.2745[spill]
        vpsrld    $16, %xmm10, %xmm8                            #1928.70
        vpand     %xmm15, %xmm10, %xmm0                         #1929.84
        vpsrld    $16, %xmm1, %xmm2                             #1930.16
        vpsrld    $16, %xmm14, %xmm10                           #1932.16
        vpand     %xmm15, %xmm6, %xmm14                         #1933.90
        vmovdqu   %xmm7, 160(%rsp)                              #1927.16[spill]
        vpand     %xmm15, %xmm13, %xmm5                         #1929.16
        vpmulld   %xmm7, %xmm9, %xmm7                           #1941.17
        vpand     %xmm15, %xmm1, %xmm1                          #1931.16
        vmovdqu   %xmm7, 240(%rsp)                              #1941.17[spill]
        vpsrld    $16, %xmm3, %xmm4                             #1930.69
        vpmulld   %xmm2, %xmm10, %xmm7                          #1938.17
        vpand     %xmm15, %xmm3, %xmm3                          #1931.83
        vpmulld   %xmm2, %xmm9, %xmm2                           #1944.17
        vmovups   %xmm11, 128(%rsp)                             #1915.2304[spill]
        vpsrld    $16, %xmm13, %xmm11                           #1928.16
        vpsrld    $16, %xmm6, %xmm13                            #1932.76
        vpsrld    $16, %xmm2, %xmm2                             #1946.17
        vpmulld   %xmm12, %xmm14, %xmm6                         #1941.80
        vpaddd    %xmm2, %xmm7, %xmm7                           #1950.17
        vmovdqu   %xmm12, 176(%rsp)                             #1927.83[spill]
        vmovdqu   %xmm8, 208(%rsp)                              #1928.70[spill]
        vmovdqu   %xmm0, 224(%rsp)                              #1929.84[spill]
        vmovdqu   %xmm6, 256(%rsp)                              #1941.80[spill]
        vpmulld   %xmm8, %xmm14, %xmm12                         #1942.80
        vpmulld   %xmm5, %xmm9, %xmm6                           #1943.17
        vpmulld   %xmm0, %xmm14, %xmm8                          #1943.80
        vpmulld   %xmm1, %xmm10, %xmm0                          #1939.17
        vpsrld    $16, %xmm0, %xmm1                             #1964.17
        vpand     %xmm15, %xmm6, %xmm0                          #1954.17
        vpaddd    %xmm7, %xmm0, %xmm0                           #1959.17
        vpsrld    $16, %xmm6, %xmm6                             #1947.17
        vpaddd    %xmm0, %xmm1, %xmm7                           #1965.17
        vpmulld   %xmm4, %xmm13, %xmm1                          #1938.80
        vpmulld   %xmm4, %xmm14, %xmm4                          #1944.80
        vpsrld    $16, %xmm4, %xmm2                             #1946.70
        vpmulld   %xmm3, %xmm13, %xmm0                          #1939.80
        vpand     %xmm15, %xmm8, %xmm3                          #1954.84
        vpaddd    %xmm2, %xmm1, %xmm1                           #1950.80
        vpsrld    $16, %xmm0, %xmm0                             #1964.70
        vmovdqu   %xmm11, 192(%rsp)                             #1928.16[spill]
        vpaddd    %xmm1, %xmm3, %xmm2                           #1959.80
        vpmulld   %xmm11, %xmm9, %xmm11                         #1942.17
        vpaddd    %xmm2, %xmm0, %xmm4                           #1965.80
        vpmulld   %xmm5, %xmm10, %xmm5                          #1937.17
        vpand     %xmm15, %xmm11, %xmm0                         #1955.17
        vpaddd    %xmm6, %xmm5, %xmm1                           #1951.17
        vpsrld    $16, %xmm8, %xmm8                             #1947.70
        vpaddd    %xmm1, %xmm0, %xmm3                           #1960.17
        vpsrld    $16, %xmm7, %xmm2                             #1966.17
        vpmulld   224(%rsp), %xmm13, %xmm0                      #1937.80[spill]
        vpand     %xmm15, %xmm12, %xmm1                         #1955.84
        vpaddd    %xmm8, %xmm0, %xmm0                           #1951.80
        vpaddd    %xmm3, %xmm2, %xmm6                           #1967.17
        vpsrld    $16, %xmm4, %xmm2                             #1966.70
        vpaddd    %xmm0, %xmm1, %xmm3                           #1960.80
        vpmulld   192(%rsp), %xmm10, %xmm1                      #1936.17[spill]
        vpaddd    %xmm3, %xmm2, %xmm8                           #1967.80
        vmovdqu   240(%rsp), %xmm2                              #1956.17[spill]
        vpsrld    $16, %xmm11, %xmm11                           #1948.17
        vpand     %xmm15, %xmm2, %xmm0                          #1956.17
        vpaddd    %xmm11, %xmm1, %xmm1                          #1952.17
        vpsrld    $16, %xmm6, %xmm5                             #1968.17
        vpaddd    %xmm1, %xmm0, %xmm0                           #1961.17
        vpmulld   208(%rsp), %xmm13, %xmm1                      #1936.80[spill]
        vpaddd    %xmm0, %xmm5, %xmm0                           #1969.17
        vmovdqu   256(%rsp), %xmm5                              #1956.84[spill]
        vpsrld    $16, %xmm12, %xmm12                           #1948.70
        vpand     %xmm15, %xmm5, %xmm3                          #1956.84
        vpaddd    %xmm12, %xmm1, %xmm1                          #1952.80
        vmovups   128(%rsp), %xmm12                             #1926.16[spill]
        vpsrld    $16, %xmm8, %xmm11                            #1968.70
        vpaddd    %xmm1, %xmm3, %xmm3                           #1961.80
        vpsrld    $16, %xmm2, %xmm2                             #1949.17
        vpaddd    %xmm3, %xmm11, %xmm1                          #1969.80
        vpsrld    $16, %xmm12, %xmm3                            #1926.16
        vpmulld   160(%rsp), %xmm10, %xmm10                     #1935.17[spill]
        vpsrld    $16, %xmm0, %xmm11                            #1970.17
        vpmulld   %xmm3, %xmm9, %xmm9                           #1940.17
        vpaddd    %xmm2, %xmm10, %xmm3                          #1974.22
        vpand     %xmm15, %xmm9, %xmm9                          #1957.17
        vpand     %xmm15, %xmm0, %xmm0                          #1973.17
        vpaddd    %xmm3, %xmm9, %xmm10                          #1974.22
        vpsrld    $16, %xmm1, %xmm3                             #1970.70
        vmovups   144(%rsp), %xmm9                              #1926.69[spill]
        vpaddd    %xmm10, %xmm11, %xmm11                        #1974.22
        vpslld    $16, %xmm11, %xmm2                            #1974.22
        vpsrld    $16, %xmm9, %xmm12                            #1926.69
        vpmulld   176(%rsp), %xmm13, %xmm13                     #1935.80[spill]
        vpaddd    %xmm0, %xmm2, %xmm10                          #1976.22
        vpmulld   %xmm12, %xmm14, %xmm14                        #1940.80
        vpsrld    $16, %xmm5, %xmm0                             #1949.70
        vpand     %xmm15, %xmm14, %xmm2                         #1957.84
        vpaddd    %xmm0, %xmm13, %xmm11                         #1974.80
        vpaddd    %xmm11, %xmm2, %xmm13                         #1974.80
        vpand     %xmm15, %xmm1, %xmm1                          #1973.84
        vpaddd    %xmm13, %xmm3, %xmm3                          #1974.80
        vpand     %xmm15, %xmm7, %xmm7                          #1972.17
        vpand     %xmm15, %xmm4, %xmm15                         #1972.84
        vmovd     %r8d, %xmm4                                   #1978.23
        vpshufd   $0, %xmm4, %xmm12                             #1978.23
        vpslld    $16, %xmm3, %xmm9                             #1974.80
        vmovd     %edi, %xmm5                                   #1980.28
        vpaddd    %xmm1, %xmm9, %xmm3                           #1976.95
        vpand     96(%rsp), %xmm12, %xmm1                       #1979.21[spill]
        vpslld    $16, %xmm6, %xmm6                             #1975.23
        vpshufd   $0, %xmm5, %xmm4                              #1980.28
        vpslld    $16, %xmm8, %xmm8                             #1975.82
        vmovups   .L_2il0floatpacket.42(%rip), %ymm13           #1985.15
        vpsrld    $9, %xmm10, %xmm14                            #1982.24
        vpand     112(%rsp), %xmm12, %xmm2                      #1979.94[spill]
        vpxor     %xmm4, %xmm1, %xmm0                           #1981.26
        vpaddd    %xmm7, %xmm6, %xmm9                           #1977.23
        vpaddd    %xmm15, %xmm8, %xmm15                         #1977.98
        vpsrld    $9, %xmm3, %xmm7                              #1982.88
        vpxor     %xmm4, %xmm2, %xmm8                           #1981.111
        vpor      %xmm0, %xmm14, %xmm11                         #1983.24
        vpor      %xmm8, %xmm7, %xmm6                           #1983.109
        vmovd     %esi, %xmm14                                  #1990.21
        vmovd     %ebx, %xmm0                                   #1992.28
        vpshufd   $0, %xmm14, %xmm7                             #1990.21
        vinsertf128 $1, %xmm6, %ymm11, %ymm12                   #1984.17
        vmovd     %ecx, %xmm6                                   #1994.26
        vaddps    %ymm13, %ymm12, %ymm8                         #1986.19
        vpshufd   $0, %xmm6, %xmm14                             #1994.26
        vpshufd   $0, %xmm0, %xmm11                             #1992.28
        vpand     %xmm15, %xmm14, %xmm6                         #1995.107
        vsubps    %ymm13, %ymm8, %ymm5                          #1987.9
        vpxor     %xmm11, %xmm1, %xmm0                          #1993.26
        vpsrld    $18, %xmm15, %xmm15                           #2006.88
        vsubps    %ymm5, %ymm12, %ymm13                         #1988.17
        vpand     %xmm9, %xmm14, %xmm5                          #1995.24
        vpxor     %xmm11, %xmm2, %xmm12                         #1993.111
        vpslld    $5, %xmm5, %xmm11                             #1996.24
        vpslld    $5, %xmm6, %xmm14                             #1996.90
        vpor      %xmm0, %xmm11, %xmm5                          #1997.24
        vpor      %xmm12, %xmm14, %xmm11                        #1997.109
        vinsertf128 $1, %xmm12, %ymm0, %ymm12                   #1999.15
        vmovd     %edx, %xmm0                                   #2001.28
        vinsertf128 $1, %xmm11, %ymm5, %ymm6                    #1998.17
        vpshufd   $0, %xmm0, %xmm5                              #2001.28
        vpxor     %xmm5, %xmm1, %xmm11                          #2002.26
        vpxor     %xmm5, %xmm2, %xmm1                           #2002.111
        vmovd     %r11d, %xmm2                                  #2003.26
        vpshufd   $0, %xmm2, %xmm2                              #2003.26
        vpand     %xmm10, %xmm2, %xmm10                         #2004.24
        vpand     %xmm3, %xmm2, %xmm2                           #2004.106
        vpslld    $14, %xmm10, %xmm0                            #2005.24
        vpsrld    $18, %xmm9, %xmm10                            #2006.23
        vpslld    $14, %xmm2, %xmm3                             #2005.91
        vpor      %xmm10, %xmm0, %xmm5                          #2007.24
        vpor      %xmm15, %xmm3, %xmm0                          #2007.106
        vpor      %xmm11, %xmm5, %xmm9                          #2008.24
        vpor      %xmm1, %xmm0, %xmm2                           #2008.109
        vsubps    %ymm12, %ymm6, %ymm6                          #2000.17
        vinsertf128 $1, %xmm2, %ymm9, %ymm3                     #2009.18
        vinsertf128 $1, %xmm1, %ymm11, %ymm1                    #2010.15
        vsubps    %ymm1, %ymm3, %ymm0                           #2011.18
        vmovups   .L_2il0floatpacket.59(%rip), %ymm3            #2017.20
        vaddps    %ymm0, %ymm13, %ymm11                         #2012.11
        vsubps    %ymm11, %ymm13, %ymm2                         #2013.17
        vaddps    %ymm2, %ymm0, %ymm1                           #2014.18
        vaddps    %ymm1, %ymm6, %ymm5                           #2015.11
        vmovups   .L_2il0floatpacket.60(%rip), %ymm6            #2018.21
        vmulps    .L_2il0floatpacket.47(%rip), %ymm5, %ymm14    #2025.16
        vandps    .L_2il0floatpacket.61(%rip), %ymm11, %ymm10   #2020.20
        vsubps    %ymm10, %ymm11, %ymm9                         #2021.21
        vmulps    %ymm10, %ymm6, %ymm13                         #2024.16
        vmulps    %ymm10, %ymm3, %ymm0                          #2022.20
        vmulps    %ymm9, %ymm3, %ymm12                          #2023.16
        vmulps    %ymm9, %ymm6, %ymm15                          #2026.16
        vmovups   .L_2il0floatpacket.50(%rip), %ymm10           #2034.16
        vaddps    %ymm13, %ymm12, %ymm2                         #2027.17
        vaddps    %ymm15, %ymm14, %ymm1                         #2028.17
        vaddps    %ymm1, %ymm2, %ymm11                          #2029.17
        vaddps    %ymm11, %ymm0, %ymm6                          #2030.17
        vsubps    %ymm6, %ymm0, %ymm0                           #2031.20
        vaddps    %ymm0, %ymm11, %ymm14                         #2032.17
        vmovups   (%rsp), %ymm0                                 #2035.11[spill]
        vandps    .L_2il0floatpacket.49(%rip), %ymm0, %ymm3     #2035.11
        vcmpgt_oqps %ymm10, %ymm3, %ymm13                       #2036.17
        vcmple_oqps %ymm10, %ymm3, %ymm5                        #2037.20
        vandps    %ymm0, %ymm5, %ymm9                           #2038.24
        vandps    %ymm6, %ymm13, %ymm12                         #2039.25
        vorps     %ymm12, %ymm9, %ymm2                          #2040.9
        vandps    %ymm14, %ymm13, %ymm1                         #2041.9
        vmulps    %ymm2, %ymm2, %ymm11                          #2044.15
        vextractf128 $1, %ymm8, %xmm4                           #1989.118
        vpand     %xmm7, %xmm8, %xmm8                           #1991.19
        vpslld    $4, %xmm8, %xmm3                              #2045.183
        vmovd     %xmm3, %r10d                                  #2045.308
        vpand     %xmm7, %xmm4, %xmm4                           #1991.88
        vpslld    $4, %xmm4, %xmm8                              #2045.245
        vmovd     %xmm8, %ebx                                   #2045.589
        vmovd     (%r10,%r9), %xmm7                             #2045.1845
        vpextrd   $1, %xmm3, %r8d                               #2045.376
        vpextrd   $2, %xmm3, %edi                               #2045.447
        vpextrd   $3, %xmm3, %esi                               #2045.518
        vmovd     (%r8,%r9), %xmm3                              #2045.1934
        vpextrd   $1, %xmm8, %ecx                               #2045.657
        vpextrd   $2, %xmm8, %edx                               #2045.728
        vpextrd   $3, %xmm8, %r11d                              #2045.799
        vpunpcklqdq %xmm3, %xmm7, %xmm4                         #2045.1825
        vmovd     (%rdi,%r9), %xmm10                            #2045.2044
        vmovd     (%rsi,%r9), %xmm7                             #2045.2133
        vmovd     (%rbx,%r9), %xmm9                             #2045.2286
        vmovd     (%rcx,%r9), %xmm12                            #2045.2375
        vmovd     (%rdx,%r9), %xmm13                            #2045.2485
        vmovd     (%r11,%r9), %xmm14                            #2045.2574
        vpunpcklqdq %xmm7, %xmm10, %xmm5                        #2045.2024
        vpunpcklqdq %xmm12, %xmm9, %xmm15                       #2045.2266
        vpunpcklqdq %xmm14, %xmm13, %xmm3                       #2045.2465
        vshufps   $136, %xmm5, %xmm4, %xmm6                     #2045.1805
        vshufps   $136, %xmm3, %xmm15, %xmm10                   #2045.2246
        vmovd     4(%rbx,%r9), %xmm14                           #2046.2286
        vmovd     4(%rcx,%r9), %xmm15                           #2046.2375
        vmovd     4(%r10,%r9), %xmm4                            #2046.1845
        vmovd     4(%r8,%r9), %xmm5                             #2046.1934
        vmovd     4(%rdi,%r9), %xmm7                            #2046.2044
        vmovd     4(%rsi,%r9), %xmm8                            #2046.2133
        vpunpcklqdq %xmm15, %xmm14, %xmm15                      #2046.2266
        vmovd     4(%r11,%r9), %xmm14                           #2046.2574
        vpunpcklqdq %xmm5, %xmm4, %xmm9                         #2046.1825
        vpunpcklqdq %xmm8, %xmm7, %xmm12                        #2046.2024
        vshufps   $136, %xmm12, %xmm9, %xmm13                   #2046.1805
        vmovd     12(%r8,%r9), %xmm7                            #2047.1936
        vmovups   32(%rsp), %ymm12                              #2047.2135[spill]
        vinsertf128 $1, %xmm10, %ymm6, %ymm3                    #2045.1741
        vmovd     4(%rdx,%r9), %xmm10                           #2046.2485
        vpunpcklqdq %xmm14, %xmm10, %xmm4                       #2046.2465
        vshufps   $136, %xmm4, %xmm15, %xmm5                    #2046.2246
        vmovd     12(%r10,%r9), %xmm6                           #2047.1847
        vpunpcklqdq %xmm7, %xmm6, %xmm6                         #2047.1827
        vmovd     12(%rdi,%r9), %xmm4                           #2047.2046
        vinsertf128 $1, %xmm5, %ymm13, %ymm10                   #2046.1741
        vmovd     12(%rsi,%r9), %xmm5                           #2047.2135
        vmovups   64(%rsp), %ymm13                              #2047.2135[spill]
                                # LOE rdx rcx rbx rsi rdi r8 r9 r10 r11 r12 r13 r14 r15 eax xmm4 xmm5 xmm6 ymm0 ymm1 ymm2 ymm3 ymm10 ymm11 ymm12 ymm13
..B4.16:                        # Preds ..B4.12
                                # Execution count [5.00e-02]: Infreq
        vpunpcklqdq %xmm5, %xmm4, %xmm14                        #2047.2026
        vshufps   $136, %xmm14, %xmm6, %xmm15                   #2047.1807
        vmovd     12(%rbx,%r9), %xmm9                           #2047.2288
        vmovd     12(%rcx,%r9), %xmm7                           #2047.2377
        vmovd     12(%rdx,%r9), %xmm6                           #2047.2487
        vmovd     12(%r11,%r9), %xmm4                           #2047.2576
        vpunpcklqdq %xmm7, %xmm9, %xmm5                         #2047.2268
        vpunpcklqdq %xmm4, %xmm6, %xmm8                         #2047.2467
        vshufps   $136, %xmm8, %xmm5, %xmm14                    #2047.2248
        vmulps    %ymm3, %ymm2, %ymm5                           #2048.19
        vinsertf128 $1, %xmm14, %ymm15, %ymm8                   #2047.1743
        vmulps    %ymm8, %ymm2, %ymm7                           #2049.22
        vaddps    %ymm7, %ymm10, %ymm4                          #2050.22
        vsubps    %ymm4, %ymm10, %ymm6                          #2052.18
        vaddps    %ymm4, %ymm5, %ymm9                           #2051.21
        vaddps    %ymm6, %ymm7, %ymm15                          #2053.18
        vsubps    %ymm9, %ymm4, %ymm14                          #2054.18
        vmulps    4352+__svml_ssin_ha_data_internal(%rip), %ymm11, %ymm6 #2059.29
        vaddps    %ymm14, %ymm5, %ymm7                          #2055.18
        vaddps    4288+__svml_ssin_ha_data_internal(%rip), %ymm6, %ymm4 #2059.14
        vaddps    %ymm7, %ymm15, %ymm7                          #2056.22
        vmulps    %ymm4, %ymm11, %ymm5                          #2060.14
        vmulps    %ymm5, %ymm2, %ymm6                           #2061.14
        vmulps    %ymm10, %ymm2, %ymm2                          #2068.35
        vaddps    %ymm8, %ymm3, %ymm5                           #2062.13
        vmovd     8(%rdi,%r9), %xmm8                            #2067.2044
        vmulps    4480+__svml_ssin_ha_data_internal(%rip), %ymm11, %ymm3 #2065.29
        vsubps    %ymm2, %ymm5, %ymm2                           #2068.13
        vaddps    4416+__svml_ssin_ha_data_internal(%rip), %ymm3, %ymm4 #2065.14
        vmovd     8(%r8,%r9), %xmm3                             #2067.1934
        vmulps    %ymm2, %ymm1, %ymm1                           #2069.31
        vmulps    %ymm4, %ymm11, %ymm4                          #2066.14
        vmovd     8(%r10,%r9), %xmm11                           #2067.1845
        vpunpcklqdq %xmm3, %xmm11, %xmm14                       #2067.1825
        vmovd     8(%rsi,%r9), %xmm11                           #2067.2133
        vpunpcklqdq %xmm11, %xmm8, %xmm15                       #2067.2024
        vshufps   $136, %xmm15, %xmm14, %xmm3                   #2067.1805
        vmovd     8(%rbx,%r9), %xmm8                            #2067.2286
        vmovd     8(%rcx,%r9), %xmm11                           #2067.2375
        vmovd     8(%rdx,%r9), %xmm14                           #2067.2485
        vmovd     8(%r11,%r9), %xmm15                           #2067.2574
        vpunpcklqdq %xmm11, %xmm8, %xmm8                        #2067.2266
        vpunpcklqdq %xmm15, %xmm14, %xmm11                      #2067.2465
        vshufps   $136, %xmm11, %xmm8, %xmm14                   #2067.2246
        vmulps    %ymm4, %ymm10, %ymm10                         #2070.32
        vmulps    %ymm2, %ymm6, %ymm6                           #2071.33
        vinsertf128 $1, %xmm14, %ymm3, %ymm3                    #2067.1741
        vaddps    %ymm1, %ymm3, %ymm1                           #2069.16
        vaddps    %ymm6, %ymm7, %ymm3                           #2071.18
        vaddps    %ymm10, %ymm1, %ymm1                          #2070.17
        vaddps    %ymm3, %ymm1, %ymm4                           #2072.17
        vaddps    %ymm4, %ymm9, %ymm5                           #2073.20
        vblendvps %ymm12, %ymm5, %ymm13, %ymm13                 #2074.14
        jmp       ..B4.2        # Prob 100%                     #2074.14
        .align    16,0x90
                                # LOE r12 r13 r14 r15 eax ymm0 ymm13
	.cfi_endproc
# mark_end;
	.type	__svml_sinf8_ha_e9,@function
	.size	__svml_sinf8_ha_e9,.-__svml_sinf8_ha_e9
..LN__svml_sinf8_ha_e9.3:
	.data
# -- End  __svml_sinf8_ha_e9
	.text
.L_2__routine_start___svml_sinf4_ha_l9_4:
# -- Begin  __svml_sinf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinf4_ha_l9
# --- __svml_sinf4_ha_l9(__m128)
__svml_sinf4_ha_l9:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2086.1
        .byte     15                                            #2416.14
        .byte     30                                            #2416.14
        .byte     250                                           #2416.14
	.cfi_startproc
..___tag_value___svml_sinf4_ha_l9.133:
..L134:
                                                        #2086.1
        pushq     %rbp                                          #2086.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2086.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2086.1
        subq      $192, %rsp                                    #2086.1
        vmovaps   %xmm0, %xmm14                                 #2086.1
        vmovups   4096+__svml_ssin_ha_data_internal(%rip), %xmm6 #2101.45
        xorl      %eax, %eax                                    #2103.1
        vmovups   5056+__svml_ssin_ha_data_internal(%rip), %xmm2 #2124.51
        vandps    %xmm6, %xmm14, %xmm1                          #2102.12
        vmovups   4544+__svml_ssin_ha_data_internal(%rip), %xmm3 #2125.54
        vandnps   %xmm14, %xmm6, %xmm11                         #2135.21
        vfmadd213ps %xmm3, %xmm1, %xmm2                         #2126.17
        vmovupd   5440+__svml_ssin_ha_data_internal(%rip), %ymm8 #2136.52
        vcmpnleps 4160+__svml_ssin_ha_data_internal(%rip), %xmm1, %xmm0 #2150.26
        vcvtps2pd %xmm1, %ymm5                                  #2123.23
        vsubps    %xmm3, %xmm2, %xmm4                           #2128.17
        vmovmskps %xmm0, %edx                                   #2152.74
        vpslld    $31, %xmm2, %xmm10                            #2127.38
        vcvtps2pd %xmm4, %ymm9                                  #2129.23
        vxorps    %xmm11, %xmm10, %xmm15                        #2145.20
        vfnmadd231pd 5120+__svml_ssin_ha_data_internal(%rip), %ymm9, %ymm5 #2131.17
        vfnmadd132pd 5184+__svml_ssin_ha_data_internal(%rip), %ymm5, %ymm9 #2133.17
        vmulpd    %ymm9, %ymm9, %ymm7                           #2134.18
        vfmadd213pd 5376+__svml_ssin_ha_data_internal(%rip), %ymm7, %ymm8 #2138.17
        vfmadd213pd 5312+__svml_ssin_ha_data_internal(%rip), %ymm7, %ymm8 #2140.17
        vfmadd213pd 5248+__svml_ssin_ha_data_internal(%rip), %ymm7, %ymm8 #2142.17
        vmulpd    %ymm8, %ymm7, %ymm12                          #2143.17
        vfmadd213pd %ymm9, %ymm9, %ymm12                        #2144.19
        vcvtpd2ps %ymm12, %xmm13                                #2146.18
        vxorps    %xmm15, %xmm13, %xmm2                         #2147.18
        testl     %edx, %edx                                    #2153.62
        jne       ..B5.12       # Prob 5%                       #2153.62
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1 xmm2 xmm14
..B5.2:                         # Preds ..B5.12 ..B5.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #2419.52
        jne       ..B5.4        # Prob 5%                       #2419.52
                                # LOE rbx r12 r13 r14 r15 eax xmm2 xmm14
..B5.15:                        # Preds ..B5.4 ..B5.2
                                # Execution count [9.50e-01]
        vzeroupper                                              #
                                # LOE rbx r12 r13 r14 r15 xmm2
..B5.3:                         # Preds ..B5.10 ..B5.15
                                # Execution count [1.00e+00]
        movaps    %xmm2, %xmm0                                  #2422.12
        movq      %rbp, %rsp                                    #2422.12
        popq      %rbp                                          #2422.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2422.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.4:                         # Preds ..B5.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm14, 64(%rsp)                              #2419.193
        vmovups   %xmm2, 128(%rsp)                              #2419.264
        je        ..B5.15       # Prob 95%                      #2419.368
                                # LOE rbx r12 r13 r14 r15 eax xmm2
..B5.7:                         # Preds ..B5.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #2419.448
                                # LOE rbx r12 r13 r14 r15 eax edx
..B5.17:                        # Preds ..B5.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.9 ..B5.17
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #2419.511
        jc        ..B5.11       # Prob 5%                       #2419.511
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.11 ..B5.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #2419.464
        cmpl      $4, %r12d                                     #2419.459
        jl        ..B5.8        # Prob 82%                      #2419.459
                                # LOE rbx r12 r14 r15 r13d
..B5.10:                        # Preds ..B5.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm2                              #2419.665
        jmp       ..B5.3        # Prob 100%                     #2419.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm2
..B5.11:                        # Preds ..B5.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #2419.540
        lea       128(%rsp,%r12,4), %rsi                        #2419.540
..___tag_value___svml_sinf4_ha_l9.151:
#       __svml_ssin_ha_cout_rare_internal(const float *, float *)
        call      __svml_ssin_ha_cout_rare_internal             #2419.540
..___tag_value___svml_sinf4_ha_l9.152:
        jmp       ..B5.9        # Prob 100%                     #2419.540
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B5.12:                        # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vpand     .L_2il0floatpacket.62(%rip), %xmm14, %xmm13   #2268.18
        lea       __svml_ssin_ha_reduction_data_internal(%rip), %r8 #2270.708
        vmovups   4224+__svml_ssin_ha_data_internal(%rip), %xmm7 #2178.50
        vpsrld    $23, %xmm13, %xmm4                            #2269.18
        vandps    %xmm1, %xmm7, %xmm11                          #2179.16
        vpslld    $1, %xmm4, %xmm10                             #2270.234
        vpaddd    %xmm4, %xmm10, %xmm5                          #2270.367
        vcmpeqps  %xmm7, %xmm11, %xmm8                          #2180.21
        vpslld    $2, %xmm5, %xmm9                              #2270.367
        vmovd     %xmm9, %edx                                   #2270.437
        vmovmskps %xmm8, %eax                                   #2182.44
        vmovups   %xmm2, (%rsp)                                 #[spill]
        vmovd     (%rdx,%r8), %xmm11                            #2270.1236
        vpextrd   $1, %xmm9, %ecx                               #2270.501
        vpextrd   $2, %xmm9, %esi                               #2270.568
        vpextrd   $3, %xmm9, %edi                               #2270.635
        vmovd     (%rcx,%r8), %xmm7                             #2270.1319
        vmovd     (%rsi,%r8), %xmm8                             #2270.1423
        vmovd     (%rdi,%r8), %xmm6                             #2270.1506
        vpunpcklqdq %xmm7, %xmm11, %xmm12                       #2270.1216
        vpunpcklqdq %xmm6, %xmm8, %xmm13                        #2270.1403
        vmovd     8(%rdi,%r8), %xmm8                            #2272.1506
        vmovd     8(%rdx,%r8), %xmm3                            #2272.1236
        vmovd     8(%rcx,%r8), %xmm9                            #2272.1319
        vmovd     8(%rsi,%r8), %xmm7                            #2272.1423
        vmovd     4(%rsi,%r8), %xmm1                            #2271.1424
        vshufps   $136, %xmm13, %xmm12, %xmm11                  #2270.1196
        vmovd     4(%rdi,%r8), %xmm4                            #2271.1507
        vpand     .L_2il0floatpacket.63(%rip), %xmm14, %xmm13   #2278.21
        vpunpcklqdq %xmm9, %xmm3, %xmm6                         #2272.1216
        vpunpcklqdq %xmm8, %xmm7, %xmm12                        #2272.1403
        vpunpcklqdq %xmm4, %xmm1, %xmm2                         #2271.1404
        vpaddd    .L_2il0floatpacket.64(%rip), %xmm13, %xmm4    #2279.21
        vmovd     4(%rdx,%r8), %xmm15                           #2271.1237
        vmovd     4(%rcx,%r8), %xmm10                           #2271.1320
        lea       __svml_ssin_ha_data_internal(%rip), %rcx      #2389.509
        vshufps   $136, %xmm12, %xmm6, %xmm12                   #2272.1196
        vmovdqu   .L_2il0floatpacket.65(%rip), %xmm13           #2280.17
        vpsrld    $16, %xmm12, %xmm1                            #2285.12
        vpunpcklqdq %xmm10, %xmm15, %xmm5                       #2271.1217
        vpsrld    $16, %xmm4, %xmm10                            #2287.12
        vpand     %xmm13, %xmm12, %xmm6                         #2286.12
        vpand     %xmm13, %xmm4, %xmm7                          #2288.12
        vshufps   $136, %xmm2, %xmm5, %xmm15                    #2271.1197
        vpand     %xmm13, %xmm11, %xmm8                         #2282.12
        vpmulld   %xmm6, %xmm10, %xmm12                         #2294.13
        vpsrld    $16, %xmm15, %xmm5                            #2283.12
        vpand     %xmm13, %xmm15, %xmm3                         #2284.12
        vpsrld    $16, %xmm12, %xmm15                           #2319.13
        vpmulld   %xmm1, %xmm10, %xmm12                         #2293.13
        vpsrld    $16, %xmm11, %xmm11                           #2281.12
        vpmulld   %xmm1, %xmm7, %xmm1                           #2299.13
        vpmulld   %xmm3, %xmm7, %xmm2                           #2298.13
        vpsrld    $16, %xmm1, %xmm1                             #2301.13
        vpand     %xmm13, %xmm2, %xmm6                          #2309.13
        vpaddd    %xmm1, %xmm12, %xmm12                         #2305.13
        vpmulld   %xmm5, %xmm7, %xmm4                           #2297.13
        vpaddd    %xmm12, %xmm6, %xmm6                          #2314.13
        vpmulld   %xmm3, %xmm10, %xmm3                          #2292.13
        vpsrld    $16, %xmm2, %xmm2                             #2302.13
        vpaddd    %xmm6, %xmm15, %xmm12                         #2320.13
        vpand     %xmm13, %xmm4, %xmm6                          #2310.13
        vpaddd    %xmm2, %xmm3, %xmm1                           #2306.13
        vpsrld    $16, %xmm12, %xmm15                           #2321.13
        vpaddd    %xmm1, %xmm6, %xmm6                           #2315.13
        vpand     %xmm13, %xmm12, %xmm12                        #2327.13
        vpmulld   %xmm8, %xmm7, %xmm9                           #2296.13
        vpaddd    %xmm6, %xmm15, %xmm6                          #2322.13
        vpmulld   %xmm5, %xmm10, %xmm5                          #2291.13
        vpsrld    $16, %xmm4, %xmm15                            #2303.13
        vpand     %xmm13, %xmm9, %xmm2                          #2311.13
        vpaddd    %xmm15, %xmm5, %xmm1                          #2307.13
        vpmulld   %xmm11, %xmm7, %xmm11                         #2295.13
        vpsrld    $16, %xmm6, %xmm3                             #2323.13
        vpmulld   %xmm8, %xmm10, %xmm10                         #2290.13
        vpaddd    %xmm1, %xmm2, %xmm4                           #2316.13
        vpsrld    $16, %xmm9, %xmm9                             #2304.13
        vpaddd    %xmm4, %xmm3, %xmm1                           #2324.13
        vpand     %xmm13, %xmm11, %xmm7                         #2312.13
        vpaddd    %xmm9, %xmm10, %xmm11                         #2329.18
        vpsrld    $16, %xmm1, %xmm15                            #2325.13
        vpaddd    %xmm11, %xmm7, %xmm8                          #2329.18
        vpaddd    %xmm8, %xmm15, %xmm15                         #2329.18
        vpand     %xmm13, %xmm1, %xmm1                          #2328.13
        vpslld    $16, %xmm15, %xmm10                           #2329.18
        vpslld    $16, %xmm6, %xmm6                             #2330.19
        vpand     .L_2il0floatpacket.66(%rip), %xmm14, %xmm3    #2334.17
        vpaddd    %xmm1, %xmm10, %xmm9                          #2331.18
        vpxor     .L_2il0floatpacket.67(%rip), %xmm3, %xmm4     #2336.22
        vpsrld    $9, %xmm9, %xmm13                             #2337.20
        vmovups   .L_2il0floatpacket.52(%rip), %xmm5            #2340.15
        vpor      %xmm4, %xmm13, %xmm11                         #2338.20
        vpxor     .L_2il0floatpacket.68(%rip), %xmm3, %xmm13    #2348.22
        vpaddd    %xmm12, %xmm6, %xmm2                          #2332.19
        vpand     .L_2il0floatpacket.69(%rip), %xmm2, %xmm8     #2350.20
        vpsrld    $18, %xmm2, %xmm2                             #2361.19
        vpslld    $5, %xmm8, %xmm6                              #2351.20
        vaddps    %xmm5, %xmm11, %xmm1                          #2341.19
        vpor      %xmm13, %xmm6, %xmm12                         #2352.20
        vsubps    %xmm5, %xmm1, %xmm7                           #2342.9
        vsubps    %xmm13, %xmm12, %xmm4                         #2355.17
        vsubps    %xmm7, %xmm11, %xmm5                          #2343.17
        vpxor     .L_2il0floatpacket.70(%rip), %xmm3, %xmm7     #2357.22
        vpand     .L_2il0floatpacket.71(%rip), %xmm9, %xmm3     #2359.20
        vpslld    $14, %xmm3, %xmm9                             #2360.20
        vpor      %xmm2, %xmm9, %xmm9                           #2362.20
        vpor      %xmm7, %xmm9, %xmm11                          #2363.20
        vmovups   .L_2il0floatpacket.53(%rip), %xmm15           #2371.20
        vsubps    %xmm7, %xmm11, %xmm8                          #2366.18
        vmovaps   %xmm15, %xmm13                                #2374.17
        vaddps    %xmm8, %xmm5, %xmm10                          #2367.11
        vmovups   .L_2il0floatpacket.58(%rip), %xmm3            #2378.16
        vsubps    %xmm10, %xmm5, %xmm6                          #2368.17
        vmulps    %xmm10, %xmm15, %xmm5                         #2373.17
        vaddps    %xmm6, %xmm8, %xmm12                          #2369.18
        vfmsub213ps %xmm5, %xmm10, %xmm13                       #2374.17
        vandps    .L_2il0floatpacket.57(%rip), %xmm14, %xmm2    #2379.11
        vaddps    %xmm12, %xmm4, %xmm8                          #2370.11
        vfmadd132ps .L_2il0floatpacket.72(%rip), %xmm13, %xmm10 #2375.17
        vcmpltps  %xmm2, %xmm3, %xmm7                           #2380.17
        vcmpleps  %xmm3, %xmm2, %xmm4                           #2381.20
        vfmadd213ps %xmm10, %xmm15, %xmm8                       #2376.17
        vpand     .L_2il0floatpacket.73(%rip), %xmm1, %xmm1     #2346.15
        vandps    %xmm14, %xmm4, %xmm9                          #2382.24
        vandps    %xmm5, %xmm7, %xmm11                          #2383.25
        vorps     %xmm11, %xmm9, %xmm11                         #2384.9
        vpslld    $4, %xmm1, %xmm9                              #2389.179
        vmovd     %xmm9, %r9d                                   #2389.238
        vandps    %xmm8, %xmm7, %xmm7                           #2385.9
        vmulps    %xmm11, %xmm11, %xmm10                        #2388.15
        vpextrd   $1, %xmm9, %r10d                              #2389.302
        vpextrd   $2, %xmm9, %r11d                              #2389.369
        vpextrd   $3, %xmm9, %edx                               #2389.436
        vmovd     (%r9,%rcx), %xmm8                             #2389.976
        vmovd     (%r10,%rcx), %xmm6                            #2389.1059
        vmovd     (%r11,%rcx), %xmm12                           #2389.1163
        vmovd     (%rdx,%rcx), %xmm13                           #2389.1246
        vpunpcklqdq %xmm6, %xmm8, %xmm15                        #2389.956
        vpunpcklqdq %xmm13, %xmm12, %xmm1                       #2389.1143
        vmovd     4(%rdx,%rcx), %xmm5                           #2390.1246
        vshufps   $136, %xmm1, %xmm15, %xmm15                   #2389.936
        vmovd     12(%rdx,%rcx), %xmm1                          #2391.1248
        vmovd     4(%r9,%rcx), %xmm2                            #2390.976
        vmovd     12(%r9,%rcx), %xmm6                           #2391.978
        vmovd     4(%r10,%rcx), %xmm3                           #2390.1059
        vmovd     12(%r10,%rcx), %xmm12                         #2391.1061
        vmovd     4(%r11,%rcx), %xmm4                           #2390.1163
        vmovd     12(%r11,%rcx), %xmm13                         #2391.1165
        vpunpcklqdq %xmm3, %xmm2, %xmm9                         #2390.956
        vpunpcklqdq %xmm5, %xmm4, %xmm8                         #2390.1143
        vmovaps   %xmm11, %xmm4                                 #2392.22
        vpunpcklqdq %xmm12, %xmm6, %xmm2                        #2391.958
        vpunpcklqdq %xmm1, %xmm13, %xmm3                        #2391.1145
        vshufps   $136, %xmm8, %xmm9, %xmm8                     #2390.936
        vshufps   $136, %xmm3, %xmm2, %xmm1                     #2391.938
        vfmadd213ps %xmm8, %xmm1, %xmm4                         #2392.22
        vmovups   4352+__svml_ssin_ha_data_internal(%rip), %xmm13 #2399.44
        vmovd     8(%r11,%rcx), %xmm2                           #2409.1163
        vmovd     8(%rdx,%rcx), %xmm3                           #2409.1246
        vfmadd213ps 4288+__svml_ssin_ha_data_internal(%rip), %xmm10, %xmm13 #2401.14
        vmovaps   %xmm4, %xmm9                                  #2393.21
        vsubps    %xmm4, %xmm8, %xmm6                           #2394.18
        vfmadd231ps %xmm15, %xmm11, %xmm9                       #2393.21
        vmulps    %xmm13, %xmm10, %xmm5                         #2402.14
        vaddps    %xmm1, %xmm15, %xmm13                         #2404.13
        vfmadd231ps %xmm11, %xmm1, %xmm6                        #2395.18
        vsubps    %xmm9, %xmm4, %xmm12                          #2396.18
        vfmadd231ps %xmm15, %xmm11, %xmm12                      #2397.18
        vmovups   4480+__svml_ssin_ha_data_internal(%rip), %xmm15 #2407.14
        vmovd     8(%r10,%rcx), %xmm1                           #2409.1059
        vfmadd213ps 4416+__svml_ssin_ha_data_internal(%rip), %xmm10, %xmm15 #2407.14
        vaddps    %xmm12, %xmm6, %xmm6                          #2398.22
        vmulps    %xmm5, %xmm11, %xmm12                         #2403.14
        vfnmadd213ps %xmm13, %xmm8, %xmm11                      #2410.13
        vmulps    %xmm15, %xmm10, %xmm15                        #2408.14
        vfmadd213ps %xmm6, %xmm11, %xmm12                       #2413.18
        vmovd     8(%r9,%rcx), %xmm10                           #2409.976
        vpunpcklqdq %xmm1, %xmm10, %xmm4                        #2409.956
        vpunpcklqdq %xmm3, %xmm2, %xmm5                         #2409.1143
        vshufps   $136, %xmm5, %xmm4, %xmm10                    #2409.936
        vfmadd213ps %xmm10, %xmm11, %xmm7                       #2411.16
        vmovups   (%rsp), %xmm2                                 #2416.14[spill]
        vfmadd213ps %xmm7, %xmm8, %xmm15                        #2412.17
        vaddps    %xmm12, %xmm15, %xmm7                         #2414.17
        vaddps    %xmm7, %xmm9, %xmm11                          #2415.20
        blendvps  %xmm0, %xmm11, %xmm2                          #2416.14
        jmp       ..B5.2        # Prob 100%                     #2416.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm2 xmm14
	.cfi_endproc
# mark_end;
	.type	__svml_sinf4_ha_l9,@function
	.size	__svml_sinf4_ha_l9,.-__svml_sinf4_ha_l9
..LN__svml_sinf4_ha_l9.4:
	.data
# -- End  __svml_sinf4_ha_l9
	.text
.L_2__routine_start___svml_sinf4_ha_ex_5:
# -- Begin  __svml_sinf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinf4_ha_ex
# --- __svml_sinf4_ha_ex(__m128)
__svml_sinf4_ha_ex:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2428.1
        .byte     15                                            #2771.14
        .byte     30                                            #2771.14
        .byte     250                                           #2771.14
	.cfi_startproc
..___tag_value___svml_sinf4_ha_ex.158:
..L159:
                                                        #2428.1
        pushq     %rbp                                          #2428.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2428.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2428.1
        subq      $192, %rsp                                    #2428.1
        xorl      %eax, %eax                                    #2445.1
        movups    4096+__svml_ssin_ha_data_internal(%rip), %xmm2 #2443.45
        movaps    %xmm2, %xmm8                                  #2444.12
        andnps    %xmm0, %xmm2                                  #2477.21
        movups    5056+__svml_ssin_ha_data_internal(%rip), %xmm11 #2468.29
        andps     %xmm0, %xmm8                                  #2444.12
        mulps     %xmm8, %xmm11                                 #2468.29
        movaps    %xmm8, %xmm9                                  #2465.77
        cvtps2pd  %xmm8, %xmm7                                  #2465.23
        movups    4544+__svml_ssin_ha_data_internal(%rip), %xmm10 #2467.54
        movups    5120+__svml_ssin_ha_data_internal(%rip), %xmm13 #2472.52
        addps     %xmm10, %xmm11                                #2468.17
        movaps    %xmm11, %xmm6                                 #2469.38
        movaps    %xmm13, %xmm12                                #2473.40
        movhlps   %xmm8, %xmm9                                  #2465.77
        pslld     $31, %xmm6                                    #2469.38
        cvtps2pd  %xmm9, %xmm5                                  #2465.63
        subps     %xmm10, %xmm11                                #2470.17
        cvtps2pd  %xmm11, %xmm14                                #2471.23
        movhlps   %xmm11, %xmm11                                #2471.74
        pxor      %xmm2, %xmm6                                  #2487.20
        cvtps2pd  %xmm11, %xmm1                                 #2471.60
        mulpd     %xmm14, %xmm12                                #2473.40
        mulpd     %xmm1, %xmm13                                 #2473.106
        subpd     %xmm12, %xmm7                                 #2473.19
        subpd     %xmm13, %xmm5                                 #2473.85
        movups    5184+__svml_ssin_ha_data_internal(%rip), %xmm15 #2474.52
        mulpd     %xmm15, %xmm14                                #2475.40
        mulpd     %xmm15, %xmm1                                 #2475.106
        subpd     %xmm14, %xmm7                                 #2475.19
        subpd     %xmm1, %xmm5                                  #2475.85
        movaps    %xmm7, %xmm3                                  #2476.20
        movaps    %xmm5, %xmm4                                  #2476.63
        mulpd     %xmm7, %xmm3                                  #2476.20
        mulpd     %xmm5, %xmm4                                  #2476.63
        movups    5440+__svml_ssin_ha_data_internal(%rip), %xmm9 #2478.51
        movaps    %xmm9, %xmm1                                  #2480.31
        mulpd     %xmm3, %xmm1                                  #2480.31
        mulpd     %xmm4, %xmm9                                  #2480.99
        movups    5376+__svml_ssin_ha_data_internal(%rip), %xmm10 #2479.51
        addpd     %xmm10, %xmm1                                 #2480.19
        addpd     %xmm9, %xmm10                                 #2480.87
        mulpd     %xmm3, %xmm1                                  #2482.31
        mulpd     %xmm4, %xmm10                                 #2482.98
        movups    5312+__svml_ssin_ha_data_internal(%rip), %xmm11 #2481.51
        addpd     %xmm11, %xmm1                                 #2482.19
        addpd     %xmm10, %xmm11                                #2482.86
        mulpd     %xmm3, %xmm1                                  #2484.31
        mulpd     %xmm4, %xmm11                                 #2484.98
        movups    5248+__svml_ssin_ha_data_internal(%rip), %xmm12 #2483.51
        addpd     %xmm12, %xmm1                                 #2484.19
        addpd     %xmm11, %xmm12                                #2484.86
        mulpd     %xmm1, %xmm3                                  #2485.19
        mulpd     %xmm12, %xmm4                                 #2485.62
        mulpd     %xmm7, %xmm3                                  #2486.33
        mulpd     %xmm5, %xmm4                                  #2486.100
        addpd     %xmm3, %xmm7                                  #2486.21
        addpd     %xmm4, %xmm5                                  #2486.88
        cvtpd2ps  %xmm7, %xmm10                                 #2488.34
        cvtpd2ps  %xmm5, %xmm2                                  #2488.65
        movaps    %xmm8, %xmm1                                  #2492.26
        cmpnleps  4160+__svml_ssin_ha_data_internal(%rip), %xmm1 #2492.26
        movmskps  %xmm1, %edx                                   #2494.74
        movlhps   %xmm2, %xmm10                                 #2488.18
        pxor      %xmm6, %xmm10                                 #2489.18
        testl     %edx, %edx                                    #2495.62
        jne       ..B6.12       # Prob 5%                       #2495.62
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1 xmm8 xmm10
..B6.2:                         # Preds ..B6.12 ..B6.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #2774.52
        jne       ..B6.4        # Prob 5%                       #2774.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm10
..B6.3:                         # Preds ..B6.4 ..B6.10 ..B6.2
                                # Execution count [1.00e+00]
        movaps    %xmm10, %xmm0                                 #2777.12
        movq      %rbp, %rsp                                    #2777.12
        popq      %rbp                                          #2777.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2777.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.4:                         # Preds ..B6.2
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #2774.193
        movups    %xmm10, 128(%rsp)                             #2774.264
        je        ..B6.3        # Prob 95%                      #2774.368
                                # LOE rbx r12 r13 r14 r15 eax xmm10
..B6.7:                         # Preds ..B6.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #2774.448
        movq      %r12, 8(%rsp)                                 #2774.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #2774.448
        movq      %r13, (%rsp)                                  #2774.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #2774.448
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.9 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #2774.511
        jc        ..B6.11       # Prob 5%                       #2774.511
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.11 ..B6.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #2774.464
        cmpl      $4, %r12d                                     #2774.459
        jl        ..B6.8        # Prob 82%                      #2774.459
                                # LOE rbx r12 r14 r15 r13d
..B6.10:                        # Preds ..B6.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm10                             #2774.665
        jmp       ..B6.3        # Prob 100%                     #2774.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm10
..B6.11:                        # Preds ..B6.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #2774.540
        lea       128(%rsp,%r12,4), %rsi                        #2774.540
..___tag_value___svml_sinf4_ha_ex.176:
#       __svml_ssin_ha_cout_rare_internal(const float *, float *)
        call      __svml_ssin_ha_cout_rare_internal             #2774.540
..___tag_value___svml_sinf4_ha_ex.177:
        jmp       ..B6.9        # Prob 100%                     #2774.540
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B6.12:                        # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movl      $2139095040, %edx                             #2609.18
        movl      $8388607, %r10d                               #2618.21
        movups    4224+__svml_ssin_ha_data_internal(%rip), %xmm4 #2520.50
        lea       __svml_ssin_ha_reduction_data_internal(%rip), %r9 #2612.803
        andps     %xmm4, %xmm8                                  #2521.16
        movl      $8388608, %r11d                               #2619.20
        cmpeqps   %xmm4, %xmm8                                  #2522.21
        movd      %edx, %xmm13                                  #2609.18
        movl      $65535, %edx                                  #2622.17
        pshufd    $0, %xmm13, %xmm12                            #2609.18
        pand      %xmm0, %xmm12                                 #2610.16
        psrld     $23, %xmm12                                   #2611.16
        movdqa    %xmm12, %xmm7                                 #2612.232
        pslld     $1, %xmm7                                     #2612.232
        paddd     %xmm12, %xmm7                                 #2612.355
        pslld     $2, %xmm7                                     #2612.355
        movups    %xmm10, 32(%rsp)                              #[spill]
        pshufd    $1, %xmm7, %xmm11                             #2612.504
        pshufd    $2, %xmm7, %xmm10                             #2612.608
        pshufd    $3, %xmm7, %xmm14                             #2612.712
        movd      %xmm7, %ecx                                   #2612.423
        movd      %xmm11, %esi                                  #2612.485
        movd      %xmm10, %edi                                  #2612.589
        movd      %r10d, %xmm10                                 #2618.21
        movd      %xmm14, %r8d                                  #2612.693
        movd      (%rsi,%r9), %xmm15                            #2612.1413
        movd      (%rdi,%r9), %xmm6                             #2612.1517
        movmskps  %xmm8, %eax                                   #2524.44
        movl      $872415232, %r10d                             #2698.22
        movd      (%rcx,%r9), %xmm8                             #2612.1330
        movd      (%r8,%r9), %xmm2                              #2612.1600
        pshufd    $0, %xmm10, %xmm14                            #2618.21
        punpcklqdq %xmm15, %xmm8                                #2612.1311
        pand      %xmm0, %xmm14                                 #2620.19
        punpcklqdq %xmm2, %xmm6                                 #2612.1497
        movups    %xmm0, (%rsp)                                 #[spill]
        movd      %r11d, %xmm0                                  #2619.20
        movd      8(%rcx,%r9), %xmm4                            #2614.1330
        movl      $511, %r11d                                   #2700.20
        movd      8(%rsi,%r9), %xmm13                           #2614.1413
        shufps    $136, %xmm6, %xmm8                            #2612.1291
        movd      8(%rdi,%r9), %xmm11                           #2614.1517
        movd      8(%r8,%r9), %xmm12                            #2614.1600
        punpcklqdq %xmm13, %xmm4                                #2614.1311
        movd      %edx, %xmm13                                  #2622.17
        pshufd    $0, %xmm0, %xmm0                              #2619.20
        movl      $1086918619, %edx                             #2713.38
        punpcklqdq %xmm12, %xmm11                               #2614.1497
        movaps    %xmm8, %xmm12                                 #2623.10
        pshufd    $0, %xmm13, %xmm7                             #2622.17
        paddd     %xmm0, %xmm14                                 #2621.19
        psrld     $16, %xmm12                                   #2623.10
        pand      %xmm7, %xmm8                                  #2624.10
        movdqu    %xmm12, 48(%rsp)                              #2623.10[spill]
        movdqa    %xmm14, %xmm12                                #2629.10
        pand      %xmm7, %xmm14                                 #2630.10
        movaps    %xmm8, %xmm13                                 #2632.135
        movdqa    %xmm14, %xmm10                                #2637.93
        psrlq     $32, %xmm13                                   #2632.135
        psrlq     $32, %xmm10                                   #2637.93
        movdqa    %xmm14, %xmm15                                #2638.177
        movdqa    %xmm10, %xmm6                                 #2638.221
        psrld     $16, %xmm12                                   #2629.10
        movups    %xmm1, 16(%rsp)                               #[spill]
        movd      4(%rcx,%r9), %xmm9                            #2613.1331
        movl      $-2147483648, %ecx                            #2675.17
        movd      4(%rsi,%r9), %xmm5                            #2613.1414
        movl      $1065353216, %esi                             #2677.22
        movd      4(%rdi,%r9), %xmm1                            #2613.1518
        movl      $1195376640, %edi                             #2682.33
        movd      4(%r8,%r9), %xmm3                             #2613.1601
        movl      $262143, %r9d                                 #2691.20
        pmuludq   %xmm8, %xmm15                                 #2638.177
        movl      $679477248, %r8d                              #2689.22
        pmuludq   %xmm13, %xmm6                                 #2638.221
        punpcklqdq %xmm5, %xmm9                                 #2613.1312
        psllq     $32, %xmm6                                    #2638.283
        punpcklqdq %xmm3, %xmm1                                 #2613.1498
        shufps    $136, %xmm1, %xmm9                            #2613.1292
        movaps    %xmm9, %xmm0                                  #2625.10
        pand      %xmm7, %xmm9                                  #2626.10
        movups    %xmm8, 64(%rsp)                               #2624.10[spill]
        psrld     $16, %xmm0                                    #2625.10
        movdqu    .L_2il0floatpacket.74(%rip), %xmm8            #2638.251
        movdqa    %xmm0, %xmm1                                  #2633.135
        pand      %xmm8, %xmm15                                 #2638.251
        psrlq     $32, %xmm1                                    #2633.135
        por       %xmm6, %xmm15                                 #2638.318
        movaps    %xmm9, %xmm5                                  #2634.135
        movdqu    %xmm15, 128(%rsp)                             #2638.318[spill]
        movdqa    %xmm14, %xmm6                                 #2639.177
        movdqa    %xmm10, %xmm15                                #2639.221
        psrlq     $32, %xmm5                                    #2634.135
        movdqu    %xmm0, 80(%rsp)                               #2625.10[spill]
        movdqu    %xmm1, 112(%rsp)                              #2633.135[spill]
        pmuludq   %xmm0, %xmm6                                  #2639.177
        movdqa    %xmm10, %xmm0                                 #2640.221
        pmuludq   %xmm1, %xmm15                                 #2639.221
        pmuludq   %xmm5, %xmm0                                  #2640.221
        movdqa    %xmm14, %xmm1                                 #2640.177
        psllq     $32, %xmm0                                    #2640.283
        pmuludq   %xmm9, %xmm1                                  #2640.177
        pand      %xmm8, %xmm6                                  #2639.251
        pmuludq   %xmm12, %xmm9                                 #2634.177
        shufps    $136, %xmm11, %xmm4                           #2614.1291
        pand      %xmm8, %xmm1                                  #2640.251
        movaps    %xmm4, %xmm2                                  #2627.10
        pand      %xmm7, %xmm4                                  #2628.10
        movdqa    %xmm12, %xmm11                                #2632.93
        por       %xmm0, %xmm1                                  #2640.318
        movdqa    %xmm12, %xmm0                                 #2636.177
        psrlq     $32, %xmm11                                   #2632.93
        pmuludq   %xmm4, %xmm0                                  #2636.177
        psrlq     $32, %xmm4                                    #2636.135
        pmuludq   %xmm11, %xmm4                                 #2636.221
        pmuludq   %xmm11, %xmm5                                 #2634.221
        psrld     $16, %xmm2                                    #2627.10
        pand      %xmm8, %xmm0                                  #2636.251
        movdqa    %xmm2, %xmm3                                  #2635.135
        psllq     $32, %xmm4                                    #2636.283
        movdqu    %xmm13, 96(%rsp)                              #2632.135[spill]
        psrlq     $32, %xmm3                                    #2635.135
        por       %xmm4, %xmm0                                  #2636.318
        movdqa    %xmm12, %xmm13                                #2635.177
        movdqa    %xmm11, %xmm4                                 #2635.221
        psllq     $32, %xmm15                                   #2639.283
        pmuludq   %xmm2, %xmm13                                 #2635.177
        por       %xmm15, %xmm6                                 #2639.318
        pmuludq   %xmm3, %xmm4                                  #2635.221
        pmuludq   %xmm14, %xmm2                                 #2641.177
        pmuludq   %xmm10, %xmm3                                 #2641.221
        pand      %xmm8, %xmm2                                  #2641.251
        psllq     $32, %xmm3                                    #2641.283
        pand      %xmm8, %xmm13                                 #2635.251
        psllq     $32, %xmm4                                    #2635.283
        por       %xmm3, %xmm2                                  #2641.318
        movdqa    %xmm1, %xmm15                                 #2651.11
        por       %xmm4, %xmm13                                 #2635.318
        psrld     $16, %xmm2                                    #2643.11
        pand      %xmm8, %xmm9                                  #2634.251
        psllq     $32, %xmm5                                    #2634.283
        pand      %xmm7, %xmm15                                 #2651.11
        paddd     %xmm2, %xmm13                                 #2647.11
        movdqa    %xmm6, %xmm2                                  #2652.11
        por       %xmm5, %xmm9                                  #2634.318
        psrld     $16, %xmm1                                    #2644.11
        psrld     $16, %xmm0                                    #2661.11
        paddd     %xmm13, %xmm15                                #2656.11
        pand      %xmm7, %xmm2                                  #2652.11
        paddd     %xmm1, %xmm9                                  #2648.11
        paddd     %xmm15, %xmm0                                 #2662.11
        movdqu    80(%rsp), %xmm15                              #2633.177[spill]
        paddd     %xmm9, %xmm2                                  #2657.11
        movdqu    112(%rsp), %xmm9                              #2633.221[spill]
        psrld     $16, %xmm6                                    #2645.11
        pmuludq   %xmm12, %xmm15                                #2633.177
        movdqa    %xmm0, %xmm13                                 #2663.11
        pmuludq   %xmm11, %xmm9                                 #2633.221
        pmuludq   64(%rsp), %xmm12                              #2632.177[spill]
        pmuludq   96(%rsp), %xmm11                              #2632.221[spill]
        pand      %xmm8, %xmm15                                 #2633.251
        psllq     $32, %xmm9                                    #2633.283
        por       %xmm9, %xmm15                                 #2633.318
        psrld     $16, %xmm13                                   #2663.11
        paddd     %xmm6, %xmm15                                 #2649.11
        paddd     %xmm2, %xmm13                                 #2664.11
        movdqu    48(%rsp), %xmm6                               #2637.177[spill]
        movdqa    %xmm13, %xmm1                                 #2665.11
        pmuludq   %xmm6, %xmm14                                 #2637.177
        psrlq     $32, %xmm6                                    #2637.135
        pmuludq   %xmm6, %xmm10                                 #2637.221
        movdqu    128(%rsp), %xmm5                              #2653.11[spill]
        psrld     $16, %xmm1                                    #2665.11
        movdqa    %xmm5, %xmm2                                  #2653.11
        pand      %xmm8, %xmm14                                 #2637.251
        pand      %xmm7, %xmm2                                  #2653.11
        psllq     $32, %xmm10                                   #2637.283
        paddd     %xmm15, %xmm2                                 #2658.11
        pand      %xmm8, %xmm12                                 #2632.251
        psllq     $32, %xmm11                                   #2632.283
        paddd     %xmm2, %xmm1                                  #2666.11
        por       %xmm10, %xmm14                                #2637.318
        por       %xmm11, %xmm12                                #2632.318
        psrld     $16, %xmm5                                    #2646.11
        movdqa    %xmm1, %xmm3                                  #2667.11
        pand      %xmm7, %xmm14                                 #2654.11
        paddd     %xmm5, %xmm12                                 #2671.16
        psrld     $16, %xmm3                                    #2667.11
        paddd     %xmm12, %xmm14                                #2671.16
        paddd     %xmm14, %xmm3                                 #2671.16
        pand      %xmm7, %xmm1                                  #2670.11
        pslld     $16, %xmm3                                    #2671.16
        pslld     $16, %xmm13                                   #2672.17
        pand      %xmm7, %xmm0                                  #2669.11
        movd      %ecx, %xmm8                                   #2675.17
        pshufd    $0, %xmm8, %xmm9                              #2675.17
        paddd     %xmm1, %xmm3                                  #2673.16
        paddd     %xmm0, %xmm13                                 #2674.17
        movd      %esi, %xmm12                                  #2677.22
        movups    (%rsp), %xmm0                                 #2676.15[spill]
        movdqa    %xmm3, %xmm5                                  #2679.18
        pshufd    $0, %xmm12, %xmm11                            #2677.22
        pand      %xmm0, %xmm9                                  #2676.15
        psrld     $9, %xmm5                                     #2679.18
        pxor      %xmm9, %xmm11                                 #2678.20
        por       %xmm11, %xmm5                                 #2680.18
        movd      %edi, %xmm10                                  #2682.33
        pshufd    $0, %xmm10, %xmm7                             #2682.33
        movdqa    %xmm5, %xmm2                                  #2683.19
        movd      %r11d, %xmm11                                 #2700.20
        movd      %r9d, %xmm8                                   #2691.20
        pshufd    $0, %xmm11, %xmm10                            #2700.20
        movd      %r8d, %xmm15                                  #2689.22
        pshufd    $0, %xmm8, %xmm6                              #2691.20
        movd      %r10d, %xmm4                                  #2698.22
        pand      %xmm3, %xmm10                                 #2701.18
        pand      %xmm13, %xmm6                                 #2692.18
        pshufd    $0, %xmm15, %xmm1                             #2689.22
        pslld     $14, %xmm10                                   #2702.18
        pshufd    $0, %xmm4, %xmm12                             #2698.22
        psrld     $18, %xmm13                                   #2703.17
        pxor      %xmm9, %xmm1                                  #2690.20
        pxor      %xmm12, %xmm9                                 #2699.20
        por       %xmm13, %xmm10                                #2704.18
        pslld     $5, %xmm6                                     #2693.18
        por       %xmm9, %xmm10                                 #2705.18
        por       %xmm1, %xmm6                                  #2694.18
        movl      $-4096, %edi                                  #2716.38
        movd      %edx, %xmm13                                  #2713.38
        movl      $1086918656, %ecx                             #2714.38
        movl      $-1214941318, %esi                            #2715.39
        movl      $255, %r10d                                   #2687.15
        addps     %xmm7, %xmm2                                  #2683.19
        subps     %xmm9, %xmm10                                 #2708.18
        subps     %xmm1, %xmm6                                  #2697.17
        movaps    %xmm2, %xmm14                                 #2684.9
        movd      %edi, %xmm9                                   #2716.38
        movd      %esi, %xmm3                                   #2715.39
        subps     %xmm7, %xmm14                                 #2684.9
        pshufd    $0, %xmm3, %xmm11                             #2715.39
        subps     %xmm14, %xmm5                                 #2685.17
        movaps    %xmm5, %xmm12                                 #2709.11
        movl      $2147483647, %r8d                             #2730.33
        movl      $897581056, %r9d                              #2731.34
        addps     %xmm10, %xmm12                                #2709.11
        movd      %r8d, %xmm14                                  #2730.33
        movd      %r9d, %xmm15                                  #2731.34
        pshufd    $0, %xmm15, %xmm1                             #2731.34
        subps     %xmm12, %xmm5                                 #2710.17
        lea       __svml_ssin_ha_data_internal(%rip), %rdi      #2742.614
        addps     %xmm5, %xmm10                                 #2711.18
        movd      %ecx, %xmm5                                   #2714.38
        addps     %xmm10, %xmm6                                 #2712.11
        pshufd    $0, %xmm13, %xmm10                            #2713.38
        pshufd    $0, %xmm9, %xmm13                             #2716.38
        andps     %xmm12, %xmm13                                #2717.20
        pshufd    $0, %xmm5, %xmm8                              #2714.38
        subps     %xmm13, %xmm12                                #2718.21
        mulps     %xmm6, %xmm10                                 #2722.16
        movdqa    %xmm8, %xmm7                                  #2719.20
        movd      %r10d, %xmm6                                  #2687.15
        mulps     %xmm13, %xmm7                                 #2719.20
        mulps     %xmm12, %xmm8                                 #2720.16
        mulps     %xmm11, %xmm13                                #2721.16
        mulps     %xmm12, %xmm11                                #2723.16
        addps     %xmm13, %xmm8                                 #2724.17
        addps     %xmm11, %xmm10                                #2725.17
        pshufd    $0, %xmm6, %xmm13                             #2687.15
        movdqa    %xmm1, %xmm11                                 #2733.17
        pshufd    $0, %xmm14, %xmm12                            #2730.33
        pand      %xmm13, %xmm2                                 #2688.13
        andps     %xmm0, %xmm12                                 #2732.11
        pslld     $4, %xmm2                                     #2742.177
        cmpltps   %xmm12, %xmm11                                #2733.17
        cmpleps   %xmm1, %xmm12                                 #2734.20
        addps     %xmm10, %xmm8                                 #2726.17
        movd      %xmm2, %r11d                                  #2742.234
        movaps    %xmm8, %xmm4                                  #2727.17
        andps     %xmm0, %xmm12                                 #2735.24
        pshufd    $2, %xmm2, %xmm14                             #2742.419
        addps     %xmm7, %xmm4                                  #2727.17
        movd      %xmm14, %ecx                                  #2742.400
        subps     %xmm4, %xmm7                                  #2728.20
        andps     %xmm11, %xmm4                                 #2736.25
        movd      (%rcx,%rdi), %xmm15                           #2742.1267
        orps      %xmm4, %xmm12                                 #2737.9
        movd      12(%rcx,%rdi), %xmm6                          #2744.1269
        movaps    %xmm12, %xmm10                                #2741.15
        movd      4(%rcx,%rdi), %xmm3                           #2743.1267
        addps     %xmm7, %xmm8                                  #2729.17
        mulps     %xmm12, %xmm10                                #2741.15
        pshufd    $1, %xmm2, %xmm7                              #2742.315
        andps     %xmm8, %xmm11                                 #2738.9
        pshufd    $3, %xmm2, %xmm2                              #2742.523
        movd      %xmm2, %esi                                   #2742.504
        movd      %xmm7, %edx                                   #2742.296
        movd      (%r11,%rdi), %xmm7                            #2742.1080
        movd      (%rsi,%rdi), %xmm14                           #2742.1350
        punpcklqdq %xmm14, %xmm15                               #2742.1247
        movd      4(%r11,%rdi), %xmm14                          #2743.1080
        movd      4(%rdx,%rdi), %xmm1                           #2743.1163
        movd      (%rdx,%rdi), %xmm13                           #2742.1163
        movd      12(%rdx,%rdi), %xmm4                          #2744.1165
        punpcklqdq %xmm1, %xmm14                                #2743.1061
        movd      12(%r11,%rdi), %xmm1                          #2744.1082
        movd      12(%rsi,%rdi), %xmm5                          #2744.1352
        punpcklqdq %xmm13, %xmm7                                #2742.1061
        punpcklqdq %xmm4, %xmm1                                 #2744.1063
        movaps    %xmm12, %xmm4                                 #2745.19
        punpcklqdq %xmm5, %xmm6                                 #2744.1249
        shufps    $136, %xmm15, %xmm7                           #2742.1041
        movaps    %xmm12, %xmm15                                #2746.22
        shufps    $136, %xmm6, %xmm1                            #2744.1043
        mulps     %xmm1, %xmm15                                 #2746.22
        mulps     %xmm7, %xmm4                                  #2745.19
        addps     %xmm1, %xmm7                                  #2759.13
        movd      4(%rsi,%rdi), %xmm2                           #2743.1350
        punpcklqdq %xmm2, %xmm3                                 #2743.1247
        shufps    $136, %xmm3, %xmm14                           #2743.1041
        movaps    %xmm14, %xmm9                                 #2747.22
        movaps    %xmm14, %xmm8                                 #2749.18
        movups    4480+__svml_ssin_ha_data_internal(%rip), %xmm2 #2762.26
        addps     %xmm15, %xmm9                                 #2747.22
        mulps     %xmm10, %xmm2                                 #2762.26
        subps     %xmm9, %xmm8                                  #2749.18
        addps     4416+__svml_ssin_ha_data_internal(%rip), %xmm2 #2762.14
        addps     %xmm8, %xmm15                                 #2750.18
        movups    4352+__svml_ssin_ha_data_internal(%rip), %xmm8 #2756.26
        movaps    %xmm9, %xmm13                                 #2748.21
        mulps     %xmm10, %xmm8                                 #2756.26
        addps     %xmm4, %xmm13                                 #2748.21
        movd      8(%r11,%rdi), %xmm1                           #2764.1080
        subps     %xmm13, %xmm9                                 #2751.18
        addps     4288+__svml_ssin_ha_data_internal(%rip), %xmm8 #2756.14
        addps     %xmm9, %xmm4                                  #2752.18
        mulps     %xmm10, %xmm8                                 #2757.14
        mulps     %xmm2, %xmm10                                 #2763.14
        addps     %xmm4, %xmm15                                 #2753.22
        mulps     %xmm12, %xmm8                                 #2758.14
        mulps     %xmm14, %xmm12                                #2765.32
        mulps     %xmm10, %xmm14                                #2767.29
        subps     %xmm12, %xmm7                                 #2765.13
        mulps     %xmm7, %xmm11                                 #2766.28
        mulps     %xmm7, %xmm8                                  #2768.30
        movd      8(%rdx,%rdi), %xmm3                           #2764.1163
        addps     %xmm8, %xmm15                                 #2768.18
        movd      8(%rcx,%rdi), %xmm6                           #2764.1267
        movd      8(%rsi,%rdi), %xmm5                           #2764.1350
        punpcklqdq %xmm3, %xmm1                                 #2764.1061
        punpcklqdq %xmm5, %xmm6                                 #2764.1247
        shufps    $136, %xmm6, %xmm1                            #2764.1041
        movups    16(%rsp), %xmm7                               #2771.25[spill]
        addps     %xmm11, %xmm1                                 #2766.16
        movaps    %xmm7, %xmm10                                 #2771.25
        addps     %xmm14, %xmm1                                 #2767.17
        andnps    32(%rsp), %xmm10                              #2771.25[spill]
        addps     %xmm15, %xmm1                                 #2769.17
        addps     %xmm1, %xmm13                                 #2770.20
        andps     %xmm7, %xmm13                                 #2771.74
        orps      %xmm13, %xmm10                                #2771.14
        jmp       ..B6.2        # Prob 100%                     #2771.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm10
	.cfi_endproc
# mark_end;
	.type	__svml_sinf4_ha_ex,@function
	.size	__svml_sinf4_ha_ex,.-__svml_sinf4_ha_ex
..LN__svml_sinf4_ha_ex.5:
	.data
# -- End  __svml_sinf4_ha_ex
	.text
.L_2__routine_start___svml_ssin_ha_cout_rare_internal_6:
# -- Begin  __svml_ssin_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_ssin_ha_cout_rare_internal
	.globl __svml_ssin_ha_cout_rare_internal
# --- __svml_ssin_ha_cout_rare_internal(const float *, float *)
__svml_ssin_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1009.1
        .byte     15                                            #1028.12
        .byte     30                                            #1028.12
        .byte     250                                           #1028.12
	.cfi_startproc
..___tag_value___svml_ssin_ha_cout_rare_internal.199:
..L200:
                                                        #1009.1
        movl      (%rdi), %edx                                  #1012.22
        movzwl    2(%rdi), %eax                                 #1014.32
        movl      %edx, -8(%rsp)                                #1012.5
        andl      $32640, %eax                                  #1014.32
        shrl      $24, %edx                                     #1013.26
        andl      $127, %edx                                    #1013.26
        movss     (%rdi), %xmm1                                 #1011.33
        cmpl      $32640, %eax                                  #1014.56
        jne       ..B7.6        # Prob 67%                      #1014.56
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 edx xmm1
..B7.2:                         # Preds ..B7.1
                                # Execution count [3.29e-01]
        movb      %dl, -5(%rsp)                                 #1013.26
        cmpl      $2139095040, -8(%rsp)                         #1016.87
        jne       ..B7.4        # Prob 50%                      #1016.87
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 xmm1
..B7.3:                         # Preds ..B7.2
                                # Execution count [1.64e-01]
        movss     _vmlsSinHATab(%rip), %xmm0                    #1018.49
        movl      $1, %eax                                      #1020.22
        mulss     %xmm0, %xmm1                                  #1018.49
        movss     %xmm1, (%rsi)                                 #1018.15
        ret                                                     #1018.15
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.4:                         # Preds ..B7.2
                                # Execution count [1.64e-01]
        mulss     (%rdi), %xmm1                                 #1024.37
        xorl      %eax, %eax                                    #1025.22
        movss     %xmm1, (%rsi)                                 #1024.15
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.5:                         # Preds ..B7.4
                                # Execution count [3.29e-01]
        ret                                                     #1020.22
                                # LOE
..B7.6:                         # Preds ..B7.1
                                # Execution count [6.71e-01]
        xorl      %eax, %eax                                    #1028.12
        ret                                                     #1028.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_ssin_ha_cout_rare_internal,@function
	.size	__svml_ssin_ha_cout_rare_internal,.-__svml_ssin_ha_cout_rare_internal
..LN__svml_ssin_ha_cout_rare_internal.6:
	.data
# -- End  __svml_ssin_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
.L_2il0floatpacket.20:
	.long	0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000
	.type	.L_2il0floatpacket.20,@object
	.size	.L_2il0floatpacket.20,64
	.align 64
.L_2il0floatpacket.21:
	.long	0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff
	.type	.L_2il0floatpacket.21,@object
	.size	.L_2il0floatpacket.21,64
	.align 64
.L_2il0floatpacket.22:
	.long	0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000
	.type	.L_2il0floatpacket.22,@object
	.size	.L_2il0floatpacket.22,64
	.align 64
.L_2il0floatpacket.23:
	.long	0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff
	.type	.L_2il0floatpacket.23,@object
	.size	.L_2il0floatpacket.23,64
	.align 64
.L_2il0floatpacket.24:
	.long	0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000
	.type	.L_2il0floatpacket.24,@object
	.size	.L_2il0floatpacket.24,64
	.align 64
.L_2il0floatpacket.25:
	.long	0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000
	.type	.L_2il0floatpacket.25,@object
	.size	.L_2il0floatpacket.25,64
	.align 64
.L_2il0floatpacket.26:
	.long	0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000
	.type	.L_2il0floatpacket.26,@object
	.size	.L_2il0floatpacket.26,64
	.align 64
.L_2il0floatpacket.27:
	.long	0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff
	.type	.L_2il0floatpacket.27,@object
	.size	.L_2il0floatpacket.27,64
	.align 64
.L_2il0floatpacket.28:
	.long	0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000
	.type	.L_2il0floatpacket.28,@object
	.size	.L_2il0floatpacket.28,64
	.align 64
.L_2il0floatpacket.29:
	.long	0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff
	.type	.L_2il0floatpacket.29,@object
	.size	.L_2il0floatpacket.29,64
	.align 64
.L_2il0floatpacket.30:
	.long	0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000
	.type	.L_2il0floatpacket.30,@object
	.size	.L_2il0floatpacket.30,64
	.align 64
.L_2il0floatpacket.31:
	.long	0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff
	.type	.L_2il0floatpacket.31,@object
	.size	.L_2il0floatpacket.31,64
	.align 64
.L_2il0floatpacket.32:
	.long	0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb
	.type	.L_2il0floatpacket.32,@object
	.size	.L_2il0floatpacket.32,64
	.align 64
.L_2il0floatpacket.33:
	.long	0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e
	.type	.L_2il0floatpacket.33,@object
	.size	.L_2il0floatpacket.33,64
	.align 64
.L_2il0floatpacket.34:
	.long	0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff
	.type	.L_2il0floatpacket.34,@object
	.size	.L_2il0floatpacket.34,64
	.align 64
.L_2il0floatpacket.35:
	.long	0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000
	.type	.L_2il0floatpacket.35,@object
	.size	.L_2il0floatpacket.35,64
	.align 64
	.hidden __svml_ssin_ha_reduction_data_internal
	.globl __svml_ssin_ha_reduction_data_internal
__svml_ssin_ha_reduction_data_internal:
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
	.long	1
	.long	0
	.long	0
	.long	2
	.long	0
	.long	0
	.long	5
	.long	0
	.long	0
	.long	10
	.long	0
	.long	0
	.long	20
	.long	0
	.long	0
	.long	40
	.long	0
	.long	0
	.long	81
	.long	0
	.long	0
	.long	162
	.long	0
	.long	0
	.long	325
	.long	0
	.long	0
	.long	651
	.long	0
	.long	0
	.long	1303
	.long	0
	.long	0
	.long	2607
	.long	0
	.long	0
	.long	5215
	.long	0
	.long	0
	.long	10430
	.long	0
	.long	0
	.long	20860
	.long	0
	.long	0
	.long	41721
	.long	0
	.long	0
	.long	83443
	.long	0
	.long	0
	.long	166886
	.long	0
	.long	0
	.long	333772
	.long	0
	.long	0
	.long	667544
	.long	0
	.long	0
	.long	1335088
	.long	0
	.long	0
	.long	2670176
	.long	0
	.long	0
	.long	5340353
	.long	0
	.long	0
	.long	10680707
	.long	0
	.long	0
	.long	21361414
	.long	0
	.long	0
	.long	42722829
	.long	0
	.long	0
	.long	85445659
	.long	0
	.long	0
	.long	170891318
	.long	0
	.long	0
	.long	341782637
	.long	0
	.long	0
	.long	683565275
	.long	0
	.long	0
	.long	1367130551
	.long	0
	.long	0
	.long	2734261102
	.long	0
	.long	1
	.long	1173554908
	.long	0
	.long	2
	.long	2347109817
	.long	0
	.long	5
	.long	399252338
	.long	0
	.long	10
	.long	798504676
	.long	0
	.long	20
	.long	1597009353
	.long	0
	.long	40
	.long	3194018707
	.long	0
	.long	81
	.long	2093070119
	.long	0
	.long	162
	.long	4186140238
	.long	0
	.long	325
	.long	4077313180
	.long	0
	.long	651
	.long	3859659065
	.long	0
	.long	1303
	.long	3424350834
	.long	0
	.long	2607
	.long	2553734372
	.long	0
	.long	5215
	.long	812501448
	.long	0
	.long	10430
	.long	1625002897
	.long	0
	.long	20860
	.long	3250005794
	.long	0
	.long	41721
	.long	2205044292
	.long	0
	.long	83443
	.long	115121288
	.long	0
	.long	166886
	.long	230242576
	.long	0
	.long	333772
	.long	460485152
	.long	0
	.long	667544
	.long	920970305
	.long	0
	.long	1335088
	.long	1841940610
	.long	0
	.long	2670176
	.long	3683881221
	.long	0
	.long	5340353
	.long	3072795146
	.long	0
	.long	10680707
	.long	1850622997
	.long	0
	.long	21361414
	.long	3701245994
	.long	0
	.long	42722829
	.long	3107524692
	.long	0
	.long	85445659
	.long	1920082089
	.long	0
	.long	170891318
	.long	3840164178
	.long	0
	.long	341782637
	.long	3385361061
	.long	0
	.long	683565275
	.long	2475754826
	.long	0
	.long	1367130551
	.long	656542356
	.long	0
	.long	2734261102
	.long	1313084713
	.long	1
	.long	1173554908
	.long	2626169427
	.long	2
	.long	2347109817
	.long	957371559
	.long	5
	.long	399252338
	.long	1914743119
	.long	10
	.long	798504676
	.long	3829486239
	.long	20
	.long	1597009353
	.long	3364005183
	.long	40
	.long	3194018707
	.long	2433043071
	.long	81
	.long	2093070119
	.long	571118846
	.long	162
	.long	4186140238
	.long	1142237692
	.long	325
	.long	4077313180
	.long	2284475384
	.long	651
	.long	3859659065
	.long	273983472
	.long	1303
	.long	3424350834
	.long	547966945
	.long	2607
	.long	2553734372
	.long	1095933890
	.long	5215
	.long	812501448
	.long	2191867780
	.long	10430
	.long	1625002897
	.long	88768265
	.long	20860
	.long	3250005794
	.long	177536531
	.long	41721
	.long	2205044292
	.long	355073063
	.long	83443
	.long	115121288
	.long	710146126
	.long	166886
	.long	230242576
	.long	1420292253
	.long	333772
	.long	460485152
	.long	2840584506
	.long	667544
	.long	920970305
	.long	1386201717
	.long	1335088
	.long	1841940610
	.long	2772403434
	.long	2670176
	.long	3683881221
	.long	1249839573
	.long	5340353
	.long	3072795146
	.long	2499679147
	.long	10680707
	.long	1850622997
	.long	704390999
	.long	21361414
	.long	3701245994
	.long	1408781999
	.long	42722829
	.long	3107524692
	.long	2817563999
	.long	85445659
	.long	1920082089
	.long	1340160702
	.long	170891318
	.long	3840164178
	.long	2680321405
	.long	341782637
	.long	3385361061
	.long	1065675514
	.long	683565275
	.long	2475754826
	.long	2131351028
	.long	1367130551
	.long	656542356
	.long	4262702056
	.long	2734261102
	.long	1313084713
	.long	4230436817
	.long	1173554908
	.long	2626169427
	.long	4165906339
	.long	2347109817
	.long	957371559
	.long	4036845383
	.long	399252338
	.long	1914743119
	.long	3778723471
	.long	798504676
	.long	3829486239
	.long	3262479647
	.long	1597009353
	.long	3364005183
	.long	2229991998
	.long	3194018707
	.long	2433043071
	.long	165016701
	.long	2093070119
	.long	571118846
	.long	330033402
	.long	4186140238
	.long	1142237692
	.long	660066805
	.long	4077313180
	.long	2284475384
	.long	1320133610
	.long	3859659065
	.long	273983472
	.long	2640267220
	.long	3424350834
	.long	547966945
	.long	985567145
	.long	2553734372
	.long	1095933890
	.long	1971134291
	.long	812501448
	.long	2191867780
	.long	3942268582
	.long	1625002897
	.long	88768265
	.long	3589569869
	.long	3250005794
	.long	177536531
	.long	2884172442
	.long	2205044292
	.long	355073063
	.long	1473377588
	.long	115121288
	.long	710146126
	.long	2946755177
	.long	230242576
	.long	1420292253
	.long	1598543059
	.long	460485152
	.long	2840584506
	.long	3197086118
	.long	920970305
	.long	1386201717
	.long	2099204941
	.long	1841940610
	.long	2772403434
	.long	4198409883
	.long	3683881221
	.long	1249839573
	.long	4101852471
	.long	3072795146
	.long	2499679147
	.long	3908737646
	.long	1850622997
	.long	704390999
	.long	3522507997
	.long	3701245994
	.long	1408781999
	.long	2750048699
	.long	3107524692
	.long	2817563999
	.long	1205130103
	.long	1920082089
	.long	1340160702
	.long	2410260206
	.long	3840164178
	.long	2680321405
	.long	525553116
	.long	3385361061
	.long	1065675514
	.long	1051106232
	.long	2475754826
	.long	2131351028
	.long	2102212464
	.long	656542356
	.long	4262702056
	.long	4204424928
	.long	1313084713
	.long	4230436817
	.long	4113882560
	.long	2626169427
	.long	4165906339
	.long	3932797825
	.long	957371559
	.long	4036845383
	.long	3570628355
	.long	1914743119
	.long	3778723471
	.long	2846289414
	.long	3829486239
	.long	3262479647
	.long	1397611533
	.long	3364005183
	.long	2229991998
	.long	2795223067
	.long	2433043071
	.long	165016701
	.long	1295478838
	.long	571118846
	.long	330033402
	.long	2590957677
	.long	1142237692
	.long	660066805
	.long	886948059
	.long	2284475384
	.long	1320133610
	.long	1773896118
	.long	273983472
	.long	2640267220
	.long	3547792237
	.long	547966945
	.long	985567145
	.long	2800617179
	.long	1095933890
	.long	1971134291
	.long	1306267062
	.long	2191867780
	.long	3942268582
	.long	2612534124
	.long	88768265
	.long	3589569869
	.long	930100952
	.long	177536531
	.long	2884172442
	.long	1860201905
	.long	355073063
	.long	1473377588
	.long	3720403810
	.long	710146126
	.long	2946755177
	.long	3145840325
	.long	1420292253
	.long	1598543059
	.long	1996713354
	.long	2840584506
	.long	3197086118
	.long	3993426708
	.long	1386201717
	.long	2099204941
	.long	3691886121
	.long	2772403434
	.long	4198409883
	.long	3088804946
	.long	1249839573
	.long	4101852471
	.long	1882642597
	.long	2499679147
	.long	3908737646
	.long	3765285194
	.long	704390999
	.long	3522507997
	.long	3235603093
	.long	1408781999
	.long	2750048699
	.long	2176238891
	.long	2817563999
	.long	1205130103
	.long	57510486
	.long	1340160702
	.long	2410260206
	.long	115020972
	.long	2680321405
	.long	525553116
	.long	230041945
	.long	1065675514
	.long	1051106232
	.long	460083891
	.long	2131351028
	.long	2102212464
	.long	920167782
	.long	4262702056
	.long	4204424928
	.long	1840335564
	.long	4230436817
	.long	4113882560
	.long	3680671129
	.long	4165906339
	.long	3932797825
	.long	3066374962
	.long	4036845383
	.long	3570628355
	.long	1837782628
	.long	3778723471
	.long	2846289414
	.long	3675565257
	.long	3262479647
	.long	1397611533
	.long	3056163219
	.long	2229991998
	.long	2795223067
	.long	1817359143
	.long	165016701
	.long	1295478838
	.long	3634718287
	.long	330033402
	.long	2590957677
	.long	2974469278
	.long	660066805
	.long	886948059
	.long	1653971260
	.long	1320133610
	.long	1773896118
	.long	3307942520
	.long	2640267220
	.long	3547792237
	.long	2320917745
	.long	985567145
	.long	2800617179
	.long	346868194
	.long	1971134291
	.long	1306267062
	.long	693736388
	.long	3942268582
	.long	2612534124
	.long	1387472776
	.long	3589569869
	.long	930100952
	.long	2774945552
	.long	2884172442
	.long	1860201905
	.long	1254923809
	.long	1473377588
	.long	3720403810
	.long	2509847619
	.long	2946755177
	.long	3145840325
	.long	724727943
	.long	1598543059
	.long	1996713354
	.long	1449455886
	.long	3197086118
	.long	3993426708
	.long	2898911772
	.long	2099204941
	.long	3691886121
	.long	1502856249
	.long	4198409883
	.long	3088804946
	.long	3005712498
	.long	4101852471
	.long	1882642597
	.long	1716457700
	.long	3908737646
	.long	3765285194
	.long	3432915400
	.long	3522507997
	.long	3235603093
	.long	2570863504
	.long	2750048699
	.long	2176238891
	.long	846759712
	.long	1205130103
	.long	57510486
	.long	1693519425
	.long	2410260206
	.long	115020972
	.long	3387038850
	.long	525553116
	.long	230041945
	.long	2479110404
	.long	1051106232
	.long	460083891
	.long	663253512
	.long	2102212464
	.long	920167782
	.long	1326507024
	.long	4204424928
	.long	1840335564
	.long	2653014048
	.long	4113882560
	.long	3680671129
	.long	1011060801
	.long	3932797825
	.long	3066374962
	.long	2022121603
	.long	3570628355
	.long	1837782628
	.long	4044243207
	.long	2846289414
	.long	3675565257
	.long	3793519119
	.long	1397611533
	.long	3056163219
	.long	3292070943
	.long	2795223067
	.long	1817359143
	.long	2289174591
	.long	1295478838
	.long	3634718287
	.long	283381887
	.long	2590957677
	.long	2974469278
	.long	566763775
	.type	__svml_ssin_ha_reduction_data_internal,@object
	.size	__svml_ssin_ha_reduction_data_internal,3072
	.align 64
	.hidden __svml_ssin_ha_data_internal
	.globl __svml_ssin_ha_data_internal
__svml_ssin_ha_data_internal:
	.long	0
	.long	0
	.long	0
	.long	1065353216
	.long	3114133471
	.long	1019808432
	.long	2953169304
	.long	1065353216
	.long	3130909128
	.long	1028193072
	.long	2968461951
	.long	1065353216
	.long	3140588184
	.long	1033283845
	.long	2975014497
	.long	1065353216
	.long	3147680113
	.long	1036565814
	.long	2960495349
	.long	1065353216
	.long	3153489468
	.long	1039839859
	.long	2970970319
	.long	1065353216
	.long	3157349634
	.long	1041645699
	.long	837346836
	.long	1065353216
	.long	3161536011
	.long	1043271842
	.long	823224313
	.long	1065353216
	.long	3164432432
	.long	1044891074
	.long	2967836285
	.long	1065353216
	.long	3167161428
	.long	1046502419
	.long	833086710
	.long	1065353216
	.long	3170205956
	.long	1048104908
	.long	2971391005
	.long	1065353216
	.long	3172229004
	.long	1049136787
	.long	824999326
	.long	1065353216
	.long	3174063957
	.long	1049927729
	.long	846027248
	.long	1065353216
	.long	3176053642
	.long	1050712805
	.long	2990442912
	.long	1065353216
	.long	3178196862
	.long	1051491540
	.long	2988789250
	.long	1065353216
	.long	3179887378
	.long	1052263466
	.long	2993707942
	.long	1065353216
	.long	3181110540
	.long	1053028117
	.long	836097324
	.long	1065353216
	.long	3182408396
	.long	1053785034
	.long	829045603
	.long	1065353216
	.long	3183780163
	.long	1054533760
	.long	840832460
	.long	1065353216
	.long	3185225016
	.long	1055273845
	.long	2983839604
	.long	1065353216
	.long	3186742084
	.long	1056004842
	.long	2986287417
	.long	1065353216
	.long	3188000746
	.long	1056726311
	.long	2978016425
	.long	1065353216
	.long	3188830103
	.long	1057201213
	.long	2992349186
	.long	1065353216
	.long	3189694133
	.long	1057551771
	.long	2998815566
	.long	1065353216
	.long	3190592315
	.long	1057896922
	.long	2991207143
	.long	1065353216
	.long	3191524108
	.long	1058236458
	.long	852349230
	.long	1065353216
	.long	3192488951
	.long	1058570176
	.long	2982650867
	.long	1065353216
	.long	3193486263
	.long	1058897873
	.long	848430348
	.long	1065353216
	.long	3194515443
	.long	1059219353
	.long	841032635
	.long	1065353216
	.long	3195575871
	.long	1059534422
	.long	2986574659
	.long	1065353216
	.long	3196363278
	.long	1059842890
	.long	2998350134
	.long	1065353216
	.long	3196923773
	.long	1060144571
	.long	2997759282
	.long	1065353216
	.long	3197498906
	.long	1060439283
	.long	844097402
	.long	1065353216
	.long	1044518635
	.long	1060726850
	.long	2994798599
	.long	1056964608
	.long	1043311911
	.long	1061007097
	.long	832220140
	.long	1056964608
	.long	1042078039
	.long	1061279856
	.long	851442039
	.long	1056964608
	.long	1040817765
	.long	1061544963
	.long	850481524
	.long	1056964608
	.long	1038876298
	.long	1061802258
	.long	848897600
	.long	1056964608
	.long	1036254719
	.long	1062051586
	.long	847147240
	.long	1056964608
	.long	1033584979
	.long	1062292797
	.long	806113028
	.long	1056964608
	.long	1029938589
	.long	1062525745
	.long	848357914
	.long	1056964608
	.long	1024416170
	.long	1062750291
	.long	2994560960
	.long	1056964608
	.long	1013387058
	.long	1062966298
	.long	841166280
	.long	1056964608
	.long	3152590408
	.long	1063173637
	.long	851900755
	.long	1056964608
	.long	3169472868
	.long	1063372184
	.long	3001545765
	.long	1056964608
	.long	3176031322
	.long	1063561817
	.long	823789818
	.long	1056964608
	.long	3180617215
	.long	1063742424
	.long	2998678409
	.long	1056964608
	.long	3183612120
	.long	1063913895
	.long	3001754476
	.long	1056964608
	.long	3186639787
	.long	1064076126
	.long	854796500
	.long	1056964608
	.long	3188684717
	.long	1064229022
	.long	2995991516
	.long	1056964608
	.long	1035072335
	.long	1064372488
	.long	840880349
	.long	1048576000
	.long	1031957395
	.long	1064506439
	.long	851742225
	.long	1048576000
	.long	1025835404
	.long	1064630795
	.long	2996018466
	.long	1048576000
	.long	1015605553
	.long	1064745479
	.long	846006572
	.long	1048576000
	.long	3152414341
	.long	1064850424
	.long	2987244005
	.long	1048576000
	.long	3170705253
	.long	1064945565
	.long	851856985
	.long	1048576000
	.long	3177244920
	.long	1065030846
	.long	855602635
	.long	1048576000
	.long	1027359369
	.long	1065106216
	.long	2989610635
	.long	1040187392
	.long	1018299420
	.long	1065171628
	.long	2969000681
	.long	1040187392
	.long	3140071849
	.long	1065227044
	.long	3002197507
	.long	1040187392
	.long	3168602920
	.long	1065272429
	.long	838093129
	.long	1040187392
	.long	1010124837
	.long	1065307757
	.long	852498564
	.long	1031798784
	.long	3160150850
	.long	1065333007
	.long	836655967
	.long	1031798784
	.long	3151746369
	.long	1065348163
	.long	814009613
	.long	1023410176
	.long	0
	.long	1065353216
	.long	0
	.long	0
	.long	1004262721
	.long	1065348163
	.long	814009613
	.long	3170893824
	.long	1012667202
	.long	1065333007
	.long	836655967
	.long	3179282432
	.long	3157608485
	.long	1065307757
	.long	852498564
	.long	3179282432
	.long	1021119272
	.long	1065272429
	.long	838093129
	.long	3187671040
	.long	992588201
	.long	1065227044
	.long	3002197507
	.long	3187671040
	.long	3165783068
	.long	1065171628
	.long	2969000681
	.long	3187671040
	.long	3174843017
	.long	1065106216
	.long	2989610635
	.long	3187671040
	.long	1029761272
	.long	1065030846
	.long	855602635
	.long	3196059648
	.long	1023221605
	.long	1064945565
	.long	851856985
	.long	3196059648
	.long	1004930693
	.long	1064850424
	.long	2987244005
	.long	3196059648
	.long	3163089201
	.long	1064745479
	.long	846006572
	.long	3196059648
	.long	3173319052
	.long	1064630795
	.long	2996018466
	.long	3196059648
	.long	3179441043
	.long	1064506439
	.long	851742225
	.long	3196059648
	.long	3182555983
	.long	1064372488
	.long	840880349
	.long	3196059648
	.long	1041201069
	.long	1064229022
	.long	2995991516
	.long	3204448256
	.long	1039156139
	.long	1064076126
	.long	854796500
	.long	3204448256
	.long	1036128472
	.long	1063913895
	.long	3001754476
	.long	3204448256
	.long	1033133567
	.long	1063742424
	.long	2998678409
	.long	3204448256
	.long	1028547674
	.long	1063561817
	.long	823789818
	.long	3204448256
	.long	1021989220
	.long	1063372184
	.long	3001545765
	.long	3204448256
	.long	1005106760
	.long	1063173637
	.long	851900755
	.long	3204448256
	.long	3160870706
	.long	1062966298
	.long	841166280
	.long	3204448256
	.long	3171899818
	.long	1062750291
	.long	2994560960
	.long	3204448256
	.long	3177422237
	.long	1062525745
	.long	848357914
	.long	3204448256
	.long	3181068627
	.long	1062292797
	.long	806113028
	.long	3204448256
	.long	3183738367
	.long	1062051586
	.long	847147240
	.long	3204448256
	.long	3186359946
	.long	1061802258
	.long	848897600
	.long	3204448256
	.long	3188301413
	.long	1061544963
	.long	850481524
	.long	3204448256
	.long	3189561687
	.long	1061279856
	.long	851442039
	.long	3204448256
	.long	3190795559
	.long	1061007097
	.long	832220140
	.long	3204448256
	.long	3192002283
	.long	1060726850
	.long	2994798599
	.long	3204448256
	.long	1050015258
	.long	1060439283
	.long	844097402
	.long	3212836864
	.long	1049440125
	.long	1060144571
	.long	2997759282
	.long	3212836864
	.long	1048879630
	.long	1059842890
	.long	2998350134
	.long	3212836864
	.long	1048092223
	.long	1059534422
	.long	2986574659
	.long	3212836864
	.long	1047031795
	.long	1059219353
	.long	841032635
	.long	3212836864
	.long	1046002615
	.long	1058897873
	.long	848430348
	.long	3212836864
	.long	1045005303
	.long	1058570176
	.long	2982650867
	.long	3212836864
	.long	1044040460
	.long	1058236458
	.long	852349230
	.long	3212836864
	.long	1043108667
	.long	1057896922
	.long	2991207143
	.long	3212836864
	.long	1042210485
	.long	1057551771
	.long	2998815566
	.long	3212836864
	.long	1041346455
	.long	1057201213
	.long	2992349186
	.long	3212836864
	.long	1040517098
	.long	1056726311
	.long	2978016425
	.long	3212836864
	.long	1039258436
	.long	1056004842
	.long	2986287417
	.long	3212836864
	.long	1037741368
	.long	1055273845
	.long	2983839604
	.long	3212836864
	.long	1036296515
	.long	1054533760
	.long	840832460
	.long	3212836864
	.long	1034924748
	.long	1053785034
	.long	829045603
	.long	3212836864
	.long	1033626892
	.long	1053028117
	.long	836097324
	.long	3212836864
	.long	1032403730
	.long	1052263466
	.long	2993707942
	.long	3212836864
	.long	1030713214
	.long	1051491540
	.long	2988789250
	.long	3212836864
	.long	1028569994
	.long	1050712805
	.long	2990442912
	.long	3212836864
	.long	1026580309
	.long	1049927729
	.long	846027248
	.long	3212836864
	.long	1024745356
	.long	1049136787
	.long	824999326
	.long	3212836864
	.long	1022722308
	.long	1048104908
	.long	2971391005
	.long	3212836864
	.long	1019677780
	.long	1046502419
	.long	833086710
	.long	3212836864
	.long	1016948784
	.long	1044891074
	.long	2967836285
	.long	3212836864
	.long	1014052363
	.long	1043271842
	.long	823224313
	.long	3212836864
	.long	1009865986
	.long	1041645699
	.long	837346836
	.long	3212836864
	.long	1006005820
	.long	1039839859
	.long	2970970319
	.long	3212836864
	.long	1000196465
	.long	1036565814
	.long	2960495349
	.long	3212836864
	.long	993104536
	.long	1033283845
	.long	2975014497
	.long	3212836864
	.long	983425480
	.long	1028193072
	.long	2968461951
	.long	3212836864
	.long	966649823
	.long	1019808432
	.long	2953169304
	.long	3212836864
	.long	0
	.long	0
	.long	0
	.long	3212836864
	.long	966649823
	.long	3167292080
	.long	805685656
	.long	3212836864
	.long	983425480
	.long	3175676720
	.long	820978303
	.long	3212836864
	.long	993104536
	.long	3180767493
	.long	827530849
	.long	3212836864
	.long	1000196465
	.long	3184049462
	.long	813011701
	.long	3212836864
	.long	1006005820
	.long	3187323507
	.long	823486671
	.long	3212836864
	.long	1009865986
	.long	3189129347
	.long	2984830484
	.long	3212836864
	.long	1014052363
	.long	3190755490
	.long	2970707961
	.long	3212836864
	.long	1016948784
	.long	3192374722
	.long	820352637
	.long	3212836864
	.long	1019677780
	.long	3193986067
	.long	2980570358
	.long	3212836864
	.long	1022722308
	.long	3195588556
	.long	823907357
	.long	3212836864
	.long	1024745356
	.long	3196620435
	.long	2972482974
	.long	3212836864
	.long	1026580309
	.long	3197411377
	.long	2993510896
	.long	3212836864
	.long	1028569994
	.long	3198196453
	.long	842959264
	.long	3212836864
	.long	1030713214
	.long	3198975188
	.long	841305602
	.long	3212836864
	.long	1032403730
	.long	3199747114
	.long	846224294
	.long	3212836864
	.long	1033626892
	.long	3200511765
	.long	2983580972
	.long	3212836864
	.long	1034924748
	.long	3201268682
	.long	2976529251
	.long	3212836864
	.long	1036296515
	.long	3202017408
	.long	2988316108
	.long	3212836864
	.long	1037741368
	.long	3202757493
	.long	836355956
	.long	3212836864
	.long	1039258436
	.long	3203488490
	.long	838803769
	.long	3212836864
	.long	1040517098
	.long	3204209959
	.long	830532777
	.long	3212836864
	.long	1041346455
	.long	3204684861
	.long	844865538
	.long	3212836864
	.long	1042210485
	.long	3205035419
	.long	851331918
	.long	3212836864
	.long	1043108667
	.long	3205380570
	.long	843723495
	.long	3212836864
	.long	1044040460
	.long	3205720106
	.long	2999832878
	.long	3212836864
	.long	1045005303
	.long	3206053824
	.long	835167219
	.long	3212836864
	.long	1046002615
	.long	3206381521
	.long	2995913996
	.long	3212836864
	.long	1047031795
	.long	3206703001
	.long	2988516283
	.long	3212836864
	.long	1048092223
	.long	3207018070
	.long	839091011
	.long	3212836864
	.long	1048879630
	.long	3207326538
	.long	850866486
	.long	3212836864
	.long	1049440125
	.long	3207628219
	.long	850275634
	.long	3212836864
	.long	1050015258
	.long	3207922931
	.long	2991581050
	.long	3212836864
	.long	3192002283
	.long	3208210498
	.long	847314951
	.long	3204448256
	.long	3190795559
	.long	3208490745
	.long	2979703788
	.long	3204448256
	.long	3189561687
	.long	3208763504
	.long	2998925687
	.long	3204448256
	.long	3188301413
	.long	3209028611
	.long	2997965172
	.long	3204448256
	.long	3186359946
	.long	3209285906
	.long	2996381248
	.long	3204448256
	.long	3183738367
	.long	3209535234
	.long	2994630888
	.long	3204448256
	.long	3181068627
	.long	3209776445
	.long	2953596676
	.long	3204448256
	.long	3177422237
	.long	3210009393
	.long	2995841562
	.long	3204448256
	.long	3171899818
	.long	3210233939
	.long	847077312
	.long	3204448256
	.long	3160870706
	.long	3210449946
	.long	2988649928
	.long	3204448256
	.long	1005106760
	.long	3210657285
	.long	2999384403
	.long	3204448256
	.long	1021989220
	.long	3210855832
	.long	854062117
	.long	3204448256
	.long	1028547674
	.long	3211045465
	.long	2971273466
	.long	3204448256
	.long	1033133567
	.long	3211226072
	.long	851194761
	.long	3204448256
	.long	1036128472
	.long	3211397543
	.long	854270828
	.long	3204448256
	.long	1039156139
	.long	3211559774
	.long	3002280148
	.long	3204448256
	.long	1041201069
	.long	3211712670
	.long	848507868
	.long	3204448256
	.long	3182555983
	.long	3211856136
	.long	2988363997
	.long	3196059648
	.long	3179441043
	.long	3211990087
	.long	2999225873
	.long	3196059648
	.long	3173319052
	.long	3212114443
	.long	848534818
	.long	3196059648
	.long	3163089201
	.long	3212229127
	.long	2993490220
	.long	3196059648
	.long	1004930693
	.long	3212334072
	.long	839760357
	.long	3196059648
	.long	1023221605
	.long	3212429213
	.long	2999340633
	.long	3196059648
	.long	1029761272
	.long	3212514494
	.long	3003086283
	.long	3196059648
	.long	3174843017
	.long	3212589864
	.long	842126987
	.long	3187671040
	.long	3165783068
	.long	3212655276
	.long	821517033
	.long	3187671040
	.long	992588201
	.long	3212710692
	.long	854713859
	.long	3187671040
	.long	1021119272
	.long	3212756077
	.long	2985576777
	.long	3187671040
	.long	3157608485
	.long	3212791405
	.long	2999982212
	.long	3179282432
	.long	1012667202
	.long	3212816655
	.long	2984139615
	.long	3179282432
	.long	1004262721
	.long	3212831811
	.long	2961493261
	.long	3170893824
	.long	0
	.long	3212836864
	.long	0
	.long	0
	.long	3151746369
	.long	3212831811
	.long	2961493261
	.long	1023410176
	.long	3160150850
	.long	3212816655
	.long	2984139615
	.long	1031798784
	.long	1010124837
	.long	3212791405
	.long	2999982212
	.long	1031798784
	.long	3168602920
	.long	3212756077
	.long	2985576777
	.long	1040187392
	.long	3140071849
	.long	3212710692
	.long	854713859
	.long	1040187392
	.long	1018299420
	.long	3212655276
	.long	821517033
	.long	1040187392
	.long	1027359369
	.long	3212589864
	.long	842126987
	.long	1040187392
	.long	3177244920
	.long	3212514494
	.long	3003086283
	.long	1048576000
	.long	3170705253
	.long	3212429213
	.long	2999340633
	.long	1048576000
	.long	3152414341
	.long	3212334072
	.long	839760357
	.long	1048576000
	.long	1015605553
	.long	3212229127
	.long	2993490220
	.long	1048576000
	.long	1025835404
	.long	3212114443
	.long	848534818
	.long	1048576000
	.long	1031957395
	.long	3211990087
	.long	2999225873
	.long	1048576000
	.long	1035072335
	.long	3211856136
	.long	2988363997
	.long	1048576000
	.long	3188684717
	.long	3211712670
	.long	848507868
	.long	1056964608
	.long	3186639787
	.long	3211559774
	.long	3002280148
	.long	1056964608
	.long	3183612120
	.long	3211397543
	.long	854270828
	.long	1056964608
	.long	3180617215
	.long	3211226072
	.long	851194761
	.long	1056964608
	.long	3176031322
	.long	3211045465
	.long	2971273466
	.long	1056964608
	.long	3169472868
	.long	3210855832
	.long	854062117
	.long	1056964608
	.long	3152590408
	.long	3210657285
	.long	2999384403
	.long	1056964608
	.long	1013387058
	.long	3210449946
	.long	2988649928
	.long	1056964608
	.long	1024416170
	.long	3210233939
	.long	847077312
	.long	1056964608
	.long	1029938589
	.long	3210009393
	.long	2995841562
	.long	1056964608
	.long	1033584979
	.long	3209776445
	.long	2953596676
	.long	1056964608
	.long	1036254719
	.long	3209535234
	.long	2994630888
	.long	1056964608
	.long	1038876298
	.long	3209285906
	.long	2996381248
	.long	1056964608
	.long	1040817765
	.long	3209028611
	.long	2997965172
	.long	1056964608
	.long	1042078039
	.long	3208763504
	.long	2998925687
	.long	1056964608
	.long	1043311911
	.long	3208490745
	.long	2979703788
	.long	1056964608
	.long	1044518635
	.long	3208210498
	.long	847314951
	.long	1056964608
	.long	3197498906
	.long	3207922931
	.long	2991581050
	.long	1065353216
	.long	3196923773
	.long	3207628219
	.long	850275634
	.long	1065353216
	.long	3196363278
	.long	3207326538
	.long	850866486
	.long	1065353216
	.long	3195575871
	.long	3207018070
	.long	839091011
	.long	1065353216
	.long	3194515443
	.long	3206703001
	.long	2988516283
	.long	1065353216
	.long	3193486263
	.long	3206381521
	.long	2995913996
	.long	1065353216
	.long	3192488951
	.long	3206053824
	.long	835167219
	.long	1065353216
	.long	3191524108
	.long	3205720106
	.long	2999832878
	.long	1065353216
	.long	3190592315
	.long	3205380570
	.long	843723495
	.long	1065353216
	.long	3189694133
	.long	3205035419
	.long	851331918
	.long	1065353216
	.long	3188830103
	.long	3204684861
	.long	844865538
	.long	1065353216
	.long	3188000746
	.long	3204209959
	.long	830532777
	.long	1065353216
	.long	3186742084
	.long	3203488490
	.long	838803769
	.long	1065353216
	.long	3185225016
	.long	3202757493
	.long	836355956
	.long	1065353216
	.long	3183780163
	.long	3202017408
	.long	2988316108
	.long	1065353216
	.long	3182408396
	.long	3201268682
	.long	2976529251
	.long	1065353216
	.long	3181110540
	.long	3200511765
	.long	2983580972
	.long	1065353216
	.long	3179887378
	.long	3199747114
	.long	846224294
	.long	1065353216
	.long	3178196862
	.long	3198975188
	.long	841305602
	.long	1065353216
	.long	3176053642
	.long	3198196453
	.long	842959264
	.long	1065353216
	.long	3174063957
	.long	3197411377
	.long	2993510896
	.long	1065353216
	.long	3172229004
	.long	3196620435
	.long	2972482974
	.long	1065353216
	.long	3170205956
	.long	3195588556
	.long	823907357
	.long	1065353216
	.long	3167161428
	.long	3193986067
	.long	2980570358
	.long	1065353216
	.long	3164432432
	.long	3192374722
	.long	820352637
	.long	1065353216
	.long	3161536011
	.long	3190755490
	.long	2970707961
	.long	1065353216
	.long	3157349634
	.long	3189129347
	.long	2984830484
	.long	1065353216
	.long	3153489468
	.long	3187323507
	.long	823486671
	.long	1065353216
	.long	3147680113
	.long	3184049462
	.long	813011701
	.long	1065353216
	.long	3140588184
	.long	3180767493
	.long	827530849
	.long	1065353216
	.long	3130909128
	.long	3175676720
	.long	820978303
	.long	1065353216
	.long	3114133471
	.long	3167292080
	.long	805685656
	.long	1065353216
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
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
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
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	3190467243
	.long	1007192156
	.long	1007192156
	.long	1007192156
	.long	1007192156
	.long	1007192156
	.long	1007192156
	.long	1007192156
	.long	1007192156
	.long	1007192156
	.long	1007192156
	.long	1007192156
	.long	1007192156
	.long	1007192156
	.long	1007192156
	.long	1007192156
	.long	1007192156
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
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1026206332
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	255
	.long	255
	.long	255
	.long	255
	.long	255
	.long	255
	.long	255
	.long	255
	.long	255
	.long	255
	.long	255
	.long	255
	.long	255
	.long	255
	.long	255
	.long	255
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	1109588355
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	3167293403
	.long	809222144
	.long	809222144
	.long	809222144
	.long	809222144
	.long	809222144
	.long	809222144
	.long	809222144
	.long	809222144
	.long	809222144
	.long	809222144
	.long	809222144
	.long	809222144
	.long	809222144
	.long	809222144
	.long	809222144
	.long	809222144
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2838781952
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	2710384946
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1073741824
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1050868099
	.long	1413742592
	.long	1074340347
	.long	1413742592
	.long	1074340347
	.long	1413742592
	.long	1074340347
	.long	1413742592
	.long	1074340347
	.long	1413742592
	.long	1074340347
	.long	1413742592
	.long	1074340347
	.long	1413742592
	.long	1074340347
	.long	1413742592
	.long	1074340347
	.long	1280075305
	.long	1033276451
	.long	1280075305
	.long	1033276451
	.long	1280075305
	.long	1033276451
	.long	1280075305
	.long	1033276451
	.long	1280075305
	.long	1033276451
	.long	1280075305
	.long	1033276451
	.long	1280075305
	.long	1033276451
	.long	1280075305
	.long	1033276451
	.long	3162727815
	.long	3217380692
	.long	3162727815
	.long	3217380692
	.long	3162727815
	.long	3217380692
	.long	3162727815
	.long	3217380692
	.long	3162727815
	.long	3217380692
	.long	3162727815
	.long	3217380692
	.long	3162727815
	.long	3217380692
	.long	3162727815
	.long	3217380692
	.long	939838102
	.long	1065423085
	.long	939838102
	.long	1065423085
	.long	939838102
	.long	1065423085
	.long	939838102
	.long	1065423085
	.long	939838102
	.long	1065423085
	.long	939838102
	.long	1065423085
	.long	939838102
	.long	1065423085
	.long	939838102
	.long	1065423085
	.long	4003935331
	.long	3207198463
	.long	4003935331
	.long	3207198463
	.long	4003935331
	.long	3207198463
	.long	4003935331
	.long	3207198463
	.long	4003935331
	.long	3207198463
	.long	4003935331
	.long	3207198463
	.long	4003935331
	.long	3207198463
	.long	4003935331
	.long	3207198463
	.long	239893998
	.long	1053154271
	.long	239893998
	.long	1053154271
	.long	239893998
	.long	1053154271
	.long	239893998
	.long	1053154271
	.long	239893998
	.long	1053154271
	.long	239893998
	.long	1053154271
	.long	239893998
	.long	1053154271
	.long	239893998
	.long	1053154271
	.type	__svml_ssin_ha_data_internal,@object
	.size	__svml_ssin_ha_data_internal,5504
	.align 32
.L_2il0floatpacket.36:
	.long	0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000
	.type	.L_2il0floatpacket.36,@object
	.size	.L_2il0floatpacket.36,32
	.align 32
.L_2il0floatpacket.37:
	.long	0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff
	.type	.L_2il0floatpacket.37,@object
	.size	.L_2il0floatpacket.37,32
	.align 32
.L_2il0floatpacket.38:
	.long	0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000
	.type	.L_2il0floatpacket.38,@object
	.size	.L_2il0floatpacket.38,32
	.align 32
.L_2il0floatpacket.39:
	.long	0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff
	.type	.L_2il0floatpacket.39,@object
	.size	.L_2il0floatpacket.39,32
	.align 32
.L_2il0floatpacket.40:
	.long	0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000
	.type	.L_2il0floatpacket.40,@object
	.size	.L_2il0floatpacket.40,32
	.align 32
.L_2il0floatpacket.41:
	.long	0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000
	.type	.L_2il0floatpacket.41,@object
	.size	.L_2il0floatpacket.41,32
	.align 32
.L_2il0floatpacket.42:
	.long	0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000
	.type	.L_2il0floatpacket.42,@object
	.size	.L_2il0floatpacket.42,32
	.align 32
.L_2il0floatpacket.43:
	.long	0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000
	.type	.L_2il0floatpacket.43,@object
	.size	.L_2il0floatpacket.43,32
	.align 32
.L_2il0floatpacket.44:
	.long	0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff
	.type	.L_2il0floatpacket.44,@object
	.size	.L_2il0floatpacket.44,32
	.align 32
.L_2il0floatpacket.45:
	.long	0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000
	.type	.L_2il0floatpacket.45,@object
	.size	.L_2il0floatpacket.45,32
	.align 32
.L_2il0floatpacket.46:
	.long	0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff
	.type	.L_2il0floatpacket.46,@object
	.size	.L_2il0floatpacket.46,32
	.align 32
.L_2il0floatpacket.47:
	.long	0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb
	.type	.L_2il0floatpacket.47,@object
	.size	.L_2il0floatpacket.47,32
	.align 32
.L_2il0floatpacket.48:
	.long	0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e
	.type	.L_2il0floatpacket.48,@object
	.size	.L_2il0floatpacket.48,32
	.align 32
.L_2il0floatpacket.49:
	.long	0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff
	.type	.L_2il0floatpacket.49,@object
	.size	.L_2il0floatpacket.49,32
	.align 32
.L_2il0floatpacket.50:
	.long	0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000
	.type	.L_2il0floatpacket.50,@object
	.size	.L_2il0floatpacket.50,32
	.align 32
.L_2il0floatpacket.51:
	.long	0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff
	.type	.L_2il0floatpacket.51,@object
	.size	.L_2il0floatpacket.51,32
	.align 32
.L_2il0floatpacket.59:
	.long	0x40c91000,0x40c91000,0x40c91000,0x40c91000,0x40c91000,0x40c91000,0x40c91000,0x40c91000
	.type	.L_2il0floatpacket.59,@object
	.size	.L_2il0floatpacket.59,32
	.align 32
.L_2il0floatpacket.60:
	.long	0xb795777a,0xb795777a,0xb795777a,0xb795777a,0xb795777a,0xb795777a,0xb795777a,0xb795777a
	.type	.L_2il0floatpacket.60,@object
	.size	.L_2il0floatpacket.60,32
	.align 32
.L_2il0floatpacket.61:
	.long	0xfffff000,0xfffff000,0xfffff000,0xfffff000,0xfffff000,0xfffff000,0xfffff000,0xfffff000
	.type	.L_2il0floatpacket.61,@object
	.size	.L_2il0floatpacket.61,32
	.align 16
.L_2il0floatpacket.52:
	.long	0x47400000,0x47400000,0x47400000,0x47400000
	.type	.L_2il0floatpacket.52,@object
	.size	.L_2il0floatpacket.52,16
	.align 16
.L_2il0floatpacket.53:
	.long	0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb
	.type	.L_2il0floatpacket.53,@object
	.size	.L_2il0floatpacket.53,16
	.align 16
.L_2il0floatpacket.54:
	.long	0x40c91000,0x40c91000,0x40c91000,0x40c91000
	.type	.L_2il0floatpacket.54,@object
	.size	.L_2il0floatpacket.54,16
	.align 16
.L_2il0floatpacket.55:
	.long	0xb795777a,0xb795777a,0xb795777a,0xb795777a
	.type	.L_2il0floatpacket.55,@object
	.size	.L_2il0floatpacket.55,16
	.align 16
.L_2il0floatpacket.56:
	.long	0xfffff000,0xfffff000,0xfffff000,0xfffff000
	.type	.L_2il0floatpacket.56,@object
	.size	.L_2il0floatpacket.56,16
	.align 16
.L_2il0floatpacket.57:
	.long	0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff
	.type	.L_2il0floatpacket.57,@object
	.size	.L_2il0floatpacket.57,16
	.align 16
.L_2il0floatpacket.58:
	.long	0x35800000,0x35800000,0x35800000,0x35800000
	.type	.L_2il0floatpacket.58,@object
	.size	.L_2il0floatpacket.58,16
	.align 16
.L_2il0floatpacket.62:
	.long	0x7f800000,0x7f800000,0x7f800000,0x7f800000
	.type	.L_2il0floatpacket.62,@object
	.size	.L_2il0floatpacket.62,16
	.align 16
.L_2il0floatpacket.63:
	.long	0x007fffff,0x007fffff,0x007fffff,0x007fffff
	.type	.L_2il0floatpacket.63,@object
	.size	.L_2il0floatpacket.63,16
	.align 16
.L_2il0floatpacket.64:
	.long	0x00800000,0x00800000,0x00800000,0x00800000
	.type	.L_2il0floatpacket.64,@object
	.size	.L_2il0floatpacket.64,16
	.align 16
.L_2il0floatpacket.65:
	.long	0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff
	.type	.L_2il0floatpacket.65,@object
	.size	.L_2il0floatpacket.65,16
	.align 16
.L_2il0floatpacket.66:
	.long	0x80000000,0x80000000,0x80000000,0x80000000
	.type	.L_2il0floatpacket.66,@object
	.size	.L_2il0floatpacket.66,16
	.align 16
.L_2il0floatpacket.67:
	.long	0x3f800000,0x3f800000,0x3f800000,0x3f800000
	.type	.L_2il0floatpacket.67,@object
	.size	.L_2il0floatpacket.67,16
	.align 16
.L_2il0floatpacket.68:
	.long	0x28800000,0x28800000,0x28800000,0x28800000
	.type	.L_2il0floatpacket.68,@object
	.size	.L_2il0floatpacket.68,16
	.align 16
.L_2il0floatpacket.69:
	.long	0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff
	.type	.L_2il0floatpacket.69,@object
	.size	.L_2il0floatpacket.69,16
	.align 16
.L_2il0floatpacket.70:
	.long	0x34000000,0x34000000,0x34000000,0x34000000
	.type	.L_2il0floatpacket.70,@object
	.size	.L_2il0floatpacket.70,16
	.align 16
.L_2il0floatpacket.71:
	.long	0x000001ff,0x000001ff,0x000001ff,0x000001ff
	.type	.L_2il0floatpacket.71,@object
	.size	.L_2il0floatpacket.71,16
	.align 16
.L_2il0floatpacket.72:
	.long	0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e
	.type	.L_2il0floatpacket.72,@object
	.size	.L_2il0floatpacket.72,16
	.align 16
.L_2il0floatpacket.73:
	.long	0x000000ff,0x000000ff,0x000000ff,0x000000ff
	.type	.L_2il0floatpacket.73,@object
	.size	.L_2il0floatpacket.73,16
	.align 16
.L_2il0floatpacket.74:
	.long	0xffffffff,0x00000000,0xffffffff,0x00000000
	.type	.L_2il0floatpacket.74,@object
	.size	.L_2il0floatpacket.74,16
	.align 4
_vmlsSinHATab:
	.long	0
	.long	2139095040
	.type	_vmlsSinHATab,@object
	.size	_vmlsSinHATab,8
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
