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
	.file "svml_s_cos.c"
	.text
..TXTST0:
.L_2__routine_start___svml_cosf8_ha_e9_0:
# -- Begin  __svml_cosf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosf8_ha_e9
# --- __svml_cosf8_ha_e9(__m256)
__svml_cosf8_ha_e9:
# parameter 1: %ymm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #666.1
        .byte     15                                            #1008.14
        .byte     30                                            #1008.14
        .byte     250                                           #1008.14
	.cfi_startproc
..___tag_value___svml_cosf8_ha_e9.1:
..L2:
                                                          #666.1
        pushq     %rbp                                          #666.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #666.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #666.1
        pushq     %r15                                          #666.1
        subq      $312, %rsp                                    #666.1
        xorl      %eax, %eax                                    #682.1
        vandps    4096+__svml_scos_ha_data_internal(%rip), %ymm0, %ymm8 #685.10
        vaddps    5504+__svml_scos_ha_data_internal(%rip), %ymm8, %ymm9 #686.9
        vcmpnle_uqps 4160+__svml_scos_ha_data_internal(%rip), %ymm8, %ymm1 #688.26
        vmovups   5440+__svml_scos_ha_data_internal(%rip), %ymm14 #707.57
        vmulps    5376+__svml_scos_ha_data_internal(%rip), %ymm9, %ymm10 #709.32
        vaddps    %ymm10, %ymm14, %ymm11                        #709.17
        vpslld    $31, %xmm11, %xmm12                           #710.84
        vsubps    %ymm14, %ymm11, %ymm2                         #711.17
        vextractf128 $1, %ymm11, %xmm13                         #710.215
        vsubps    5568+__svml_scos_ha_data_internal(%rip), %ymm2, %ymm4 #713.17
        vpslld    $31, %xmm13, %xmm15                           #710.181
        vmovupd   4992+__svml_scos_ha_data_internal(%rip), %ymm13 #716.55
        vextractf128 $1, %ymm4, %xmm3                           #714.105
        vcvtps2pd %xmm4, %ymm5                                  #714.23
        vcvtps2pd %xmm3, %ymm6                                  #714.88
        vmovupd   5056+__svml_scos_ha_data_internal(%rip), %ymm3 #718.55
        vmulpd    %ymm13, %ymm6, %ymm14                         #717.115
        vmulpd    %ymm3, %ymm6, %ymm6                           #719.115
        vinsertf128 $1, %xmm15, %ymm12, %ymm7                   #710.20
        vmulpd    %ymm13, %ymm5, %ymm12                         #717.43
        vmulpd    %ymm3, %ymm5, %ymm5                           #719.43
        vcvtps2pd %xmm8, %ymm10                                 #715.23
        vextractf128 $1, %ymm8, %xmm9                           #715.106
        vsubpd    %ymm12, %ymm10, %ymm2                         #717.19
        vcvtps2pd %xmm9, %ymm15                                 #715.89
        vsubpd    %ymm5, %ymm2, %ymm4                           #719.19
        vsubpd    %ymm14, %ymm15, %ymm9                         #717.91
        vmulpd    %ymm4, %ymm4, %ymm14                          #720.20
        vmovupd   5184+__svml_scos_ha_data_internal(%rip), %ymm15 #724.54
        vsubpd    %ymm6, %ymm9, %ymm2                           #719.91
        vmovupd   5312+__svml_scos_ha_data_internal(%rip), %ymm6 #721.54
        vmovupd   5248+__svml_scos_ha_data_internal(%rip), %ymm9 #722.54
        vmulpd    %ymm2, %ymm2, %ymm3                           #720.66
        vmulpd    %ymm14, %ymm6, %ymm5                          #723.34
        vmulpd    %ymm3, %ymm6, %ymm10                          #723.108
        vaddpd    %ymm5, %ymm9, %ymm11                          #723.19
        vaddpd    %ymm10, %ymm9, %ymm13                         #723.93
        vmulpd    %ymm11, %ymm14, %ymm12                        #725.34
        vmovupd   5120+__svml_scos_ha_data_internal(%rip), %ymm10 #726.54
        vmulpd    %ymm13, %ymm3, %ymm13                         #725.107
        vaddpd    %ymm12, %ymm15, %ymm5                         #725.19
        vaddpd    %ymm13, %ymm15, %ymm9                         #725.92
        vmulpd    %ymm5, %ymm14, %ymm6                          #727.34
        vmulpd    %ymm9, %ymm3, %ymm11                          #727.107
        vaddpd    %ymm6, %ymm10, %ymm12                         #727.19
        vaddpd    %ymm11, %ymm10, %ymm15                        #727.92
        vmulpd    %ymm12, %ymm14, %ymm14                        #728.19
        vmulpd    %ymm15, %ymm3, %ymm5                          #728.65
        vmulpd    %ymm14, %ymm4, %ymm3                          #729.36
        vaddpd    %ymm3, %ymm4, %ymm14                          #729.21
        vmulpd    %ymm5, %ymm2, %ymm4                           #729.109
        vaddpd    %ymm4, %ymm2, %ymm3                           #729.94
        vcvtpd2ps %ymm14, %xmm2                                 #730.64
        vcvtpd2ps %ymm3, %xmm14                                 #730.100
        vpxor     %xmm3, %xmm3, %xmm3                           #734.92
        vinsertf128 $1, %xmm14, %ymm2, %ymm4                    #730.18
        vxorps    %ymm7, %ymm4, %ymm14                          #731.18
        vextractf128 $1, %ymm1, %xmm7                           #733.151
        vpackssdw %xmm7, %xmm1, %xmm2                           #734.109
        vpacksswb %xmm3, %xmm2, %xmm4                           #734.92
        vpmovmskb %xmm4, %edx                                   #734.73
        testb     %dl, %dl                                      #735.62
        jne       ..B1.12       # Prob 5%                       #735.62
	.cfi_escape 0x10, 0x0f, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 eax ymm0 ymm1 ymm8 ymm14
..B1.2:                         # Preds ..B1.16 ..B1.1
                                # Execution count [1.00e+00]
        testb     %al, %al                                      #1011.52
        jne       ..B1.4        # Prob 5%                       #1011.52
                                # LOE rbx r12 r13 r14 eax ymm0 ymm14
..B1.3:                         # Preds ..B1.4 ..B1.10 ..B1.2
                                # Execution count [1.00e+00]
        vmovaps   %ymm14, %ymm0                                 #1014.12
        addq      $312, %rsp                                    #1014.12
	.cfi_restore 15
        popq      %r15                                          #1014.12
        movq      %rbp, %rsp                                    #1014.12
        popq      %rbp                                          #1014.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1014.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
	.cfi_escape 0x10, 0x0f, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE
..B1.4:                         # Preds ..B1.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm0, 64(%rsp)                               #1011.196
        vmovups   %ymm14, 128(%rsp)                             #1011.270
        testl     %eax, %eax                                    #1011.374
        je        ..B1.3        # Prob 95%                      #1011.374
                                # LOE rbx r12 r13 r14 eax ymm14
..B1.7:                         # Preds ..B1.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %r15d, %r15d                                  #1011.454
                                # LOE rbx r12 r13 r14 r15 eax
..B1.15:                        # Preds ..B1.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xc0, 0xfe, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
                                # LOE rbx r13 r14 r15 r12d
..B1.8:                         # Preds ..B1.9 ..B1.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r15d, %r12d                                  #1011.517
        jc        ..B1.11       # Prob 5%                       #1011.517
                                # LOE rbx r13 r14 r15 r12d
..B1.9:                         # Preds ..B1.11 ..B1.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r15d                                         #1011.470
        cmpl      $8, %r15d                                     #1011.465
        jl        ..B1.8        # Prob 82%                      #1011.465
                                # LOE rbx r13 r14 r15 r12d
..B1.10:                        # Preds ..B1.9
                                # Execution count [2.25e-03]: Infreq
        movq      (%rsp), %r12                                  #[spill]
	.cfi_restore 12
        vmovups   128(%rsp), %ymm14                             #1011.674
        jmp       ..B1.3        # Prob 100%                     #1011.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xc0, 0xfe, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 ymm14
..B1.11:                        # Preds ..B1.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r15,4), %rdi                         #1011.546
        lea       128(%rsp,%r15,4), %rsi                        #1011.546
..___tag_value___svml_cosf8_ha_e9.17:
#       __svml_scos_ha_cout_rare_internal(const float *, float *)
        call      __svml_scos_ha_cout_rare_internal             #1011.546
..___tag_value___svml_cosf8_ha_e9.18:
        jmp       ..B1.9        # Prob 100%                     #1011.546
	.cfi_restore 12
                                # LOE rbx r13 r14 r12d r15d
..B1.12:                        # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   4224+__svml_scos_ha_data_internal(%rip), %ymm2 #757.53
        movl      $2139095040, %r15d                            #846.24
        vmovups   %ymm0, (%rsp)                                 #[spill]
        vmovups   %ymm14, 64(%rsp)                              #[spill]
        vpxor     %xmm4, %xmm4, %xmm4                           #761.62
        vmovups   %ymm1, 32(%rsp)                               #[spill]
        vandps    %ymm8, %ymm2, %ymm3                           #758.14
        vmovd     %r15d, %xmm8                                  #846.24
        vcmpeqps  %ymm2, %ymm3, %ymm5                           #759.21
        lea       __svml_scos_ha_reduction_data_internal(%rip), %rdx #849.1256
        vpshufd   $0, %xmm8, %xmm10                             #846.24
        vpand     %xmm0, %xmm10, %xmm9                          #847.22
        vextractf128 $1, %ymm0, %xmm15                          #845.109
        vpsrld    $23, %xmm9, %xmm6                             #848.22
        vpslld    $1, %xmm6, %xmm11                             #849.238
        vpaddd    %xmm6, %xmm11, %xmm1                          #849.543
        vpand     %xmm15, %xmm10, %xmm14                        #847.97
        vpsrld    $23, %xmm14, %xmm2                            #848.85
        vpslld    $1, %xmm2, %xmm3                              #849.304
        vmovups   %xmm0, 96(%rsp)                               #845.37[spill]
        vmovups   %xmm15, 112(%rsp)                             #845.109[spill]
        vextractf128 $1, %ymm5, %xmm13                          #760.128
        vpackssdw %xmm13, %xmm5, %xmm12                         #761.79
        vpaddd    %xmm2, %xmm3, %xmm5                           #849.612
        vpslld    $2, %xmm1, %xmm13                             #849.543
        vpslld    $2, %xmm5, %xmm14                             #849.612
        vmovd     %xmm13, %ecx                                  #849.686
        vmovd     %xmm14, %r9d                                  #849.967
        vpacksswb %xmm4, %xmm12, %xmm7                          #761.62
        vmovd     (%rcx,%rdx), %xmm12                           #849.2344
        vpextrd   $1, %xmm13, %esi                              #849.754
        vpextrd   $2, %xmm13, %edi                              #849.825
        vpextrd   $3, %xmm13, %r8d                              #849.896
        vpextrd   $1, %xmm14, %r11d                             #849.1035
        vpextrd   $2, %xmm14, %r10d                             #849.1106
        vpextrd   $3, %xmm14, %r15d                             #849.1177
        vpmovmskb %xmm7, %eax                                   #761.43
        vmovd     (%rsi,%rdx), %xmm4                            #849.2433
        vmovd     (%rdi,%rdx), %xmm7                            #849.2543
        vmovd     (%r8,%rdx), %xmm8                             #849.2632
        vmovd     (%r9,%rdx), %xmm11                            #849.2785
        vmovd     (%r11,%rdx), %xmm6                            #849.2874
        vmovd     (%r10,%rdx), %xmm1                            #849.2984
        vmovd     (%r15,%rdx), %xmm3                            #849.3073
        vpunpcklqdq %xmm4, %xmm12, %xmm9                        #849.2324
        vpunpcklqdq %xmm8, %xmm7, %xmm10                        #849.2523
        vpunpcklqdq %xmm6, %xmm11, %xmm2                        #849.2765
        vpunpcklqdq %xmm3, %xmm1, %xmm13                        #849.2964
        vmovd     4(%r15,%rdx), %xmm1                           #850.3074
        vmovd     4(%rcx,%rdx), %xmm12                          #850.2345
        vmovd     4(%rsi,%rdx), %xmm4                           #850.2434
        vmovd     4(%rdi,%rdx), %xmm7                           #850.2544
        vmovd     4(%r8,%rdx), %xmm8                            #850.2633
        vmovd     4(%r9,%rdx), %xmm14                           #850.2786
        vmovd     4(%r11,%rdx), %xmm11                          #850.2875
        vmovd     4(%r10,%rdx), %xmm6                           #850.2985
        vshufps   $136, %xmm10, %xmm9, %xmm5                    #849.2304
        vshufps   $136, %xmm13, %xmm2, %xmm2                    #849.2745
        vpunpcklqdq %xmm4, %xmm12, %xmm9                        #850.2325
        vpunpcklqdq %xmm8, %xmm7, %xmm10                        #850.2524
        vpunpcklqdq %xmm11, %xmm14, %xmm3                       #850.2766
        vpunpcklqdq %xmm1, %xmm6, %xmm13                        #850.2965
        vmovd     8(%rcx,%rdx), %xmm12                          #851.2344
        movl      $262143, %ecx                                 #928.26
        vmovd     8(%rsi,%rdx), %xmm4                           #851.2433
        movl      $679477248, %esi                              #926.28
        vshufps   $136, %xmm10, %xmm9, %xmm10                   #850.2305
        vshufps   $136, %xmm13, %xmm3, %xmm13                   #850.2746
        vpunpcklqdq %xmm4, %xmm12, %xmm9                        #851.2324
        vmovd     8(%rdi,%rdx), %xmm7                           #851.2543
        movl      $255, %edi                                    #924.21
        vmovd     8(%r8,%rdx), %xmm8                            #851.2632
        movl      $1065353216, %r8d                             #914.28
        vmovd     8(%r9,%rdx), %xmm11                           #851.2785
        movl      $-2147483648, %r9d                            #912.23
        vmovd     8(%r11,%rdx), %xmm6                           #851.2874
        movl      $65535, %r11d                                 #859.23
        vmovd     8(%r10,%rdx), %xmm3                           #851.2984
        movl      $8388608, %r10d                               #856.26
        vmovd     8(%r15,%rdx), %xmm12                          #851.3073
        movl      $8388607, %edx                                #855.27
        vpunpcklqdq %xmm8, %xmm7, %xmm14                        #851.2523
        movl      $511, %r15d                                   #937.26
        vshufps   $136, %xmm14, %xmm9, %xmm1                    #851.2304
        vpunpcklqdq %xmm6, %xmm11, %xmm4                        #851.2765
        vmovd     %r10d, %xmm9                                  #856.26
        vmovd     %edx, %xmm8                                   #855.27
        movl      $872415232, %edx                              #935.28
        vpshufd   $0, %xmm8, %xmm11                             #855.27
        vpsrld    $16, %xmm13, %xmm8                            #862.70
        vpshufd   $0, %xmm9, %xmm6                              #856.26
        vpand     %xmm0, %xmm11, %xmm0                          #857.25
        vpaddd    %xmm6, %xmm0, %xmm14                          #858.25
        vpand     %xmm15, %xmm11, %xmm15                        #857.106
        vmovd     %r11d, %xmm0                                  #859.23
        vpaddd    %xmm6, %xmm15, %xmm6                          #858.111
        vpunpcklqdq %xmm12, %xmm3, %xmm7                        #851.2964
        vpsrld    $16, %xmm10, %xmm11                           #862.16
        vpshufd   $0, %xmm0, %xmm15                             #859.23
        lea       __svml_scos_ha_data_internal(%rip), %r10      #979.878
        vshufps   $136, %xmm7, %xmm4, %xmm3                     #851.2745
        vpand     %xmm15, %xmm5, %xmm7                          #861.16
        vpand     %xmm15, %xmm14, %xmm9                         #867.16
        vpand     %xmm15, %xmm2, %xmm12                         #861.83
        vmovups   %xmm5, 128(%rsp)                              #849.2304[spill]
        vpand     %xmm15, %xmm10, %xmm5                         #863.16
        vmovups   %xmm2, 144(%rsp)                              #849.2745[spill]
        vpsrld    $16, %xmm1, %xmm2                             #864.16
        vpsrld    $16, %xmm14, %xmm10                           #866.16
        vpand     %xmm15, %xmm6, %xmm14                         #867.90
        vmovdqu   %xmm7, 160(%rsp)                              #861.16[spill]
        vpand     %xmm15, %xmm13, %xmm0                         #863.84
        vpmulld   %xmm7, %xmm9, %xmm7                           #875.17
        vpand     %xmm15, %xmm1, %xmm1                          #865.16
        vmovdqu   %xmm7, 240(%rsp)                              #875.17[spill]
        vpsrld    $16, %xmm6, %xmm13                            #866.76
        vpmulld   %xmm2, %xmm10, %xmm7                          #872.17
        vpsrld    $16, %xmm3, %xmm4                             #864.69
        vpmulld   %xmm2, %xmm9, %xmm2                           #878.17
        vpand     %xmm15, %xmm3, %xmm3                          #865.83
        vmovdqu   %xmm12, 176(%rsp)                             #861.83[spill]
        vpsrld    $16, %xmm2, %xmm2                             #880.17
        vpmulld   %xmm12, %xmm14, %xmm12                        #875.80
        vpaddd    %xmm2, %xmm7, %xmm7                           #884.17
        vmovdqu   %xmm8, 208(%rsp)                              #862.70[spill]
        vmovdqu   %xmm0, 224(%rsp)                              #863.84[spill]
        vmovdqu   %xmm12, 256(%rsp)                             #875.80[spill]
        vpmulld   %xmm8, %xmm14, %xmm12                         #876.80
        vpmulld   %xmm5, %xmm9, %xmm6                           #877.17
        vpmulld   %xmm0, %xmm14, %xmm8                          #877.80
        vpmulld   %xmm1, %xmm10, %xmm0                          #873.17
        vpsrld    $16, %xmm0, %xmm1                             #898.17
        vpand     %xmm15, %xmm6, %xmm0                          #888.17
        vpaddd    %xmm7, %xmm0, %xmm0                           #893.17
        vpaddd    %xmm0, %xmm1, %xmm7                           #899.17
        vpmulld   %xmm4, %xmm13, %xmm1                          #872.80
        vpmulld   %xmm4, %xmm14, %xmm4                          #878.80
        vpmulld   %xmm3, %xmm13, %xmm0                          #873.80
        vpsrld    $16, %xmm4, %xmm2                             #880.70
        vpsrld    $16, %xmm0, %xmm3                             #898.70
        vpand     %xmm15, %xmm8, %xmm0                          #888.84
        vpaddd    %xmm2, %xmm1, %xmm1                           #884.80
        vpsrld    $16, %xmm7, %xmm2                             #900.17
        vpaddd    %xmm1, %xmm0, %xmm0                           #893.80
        vpsrld    $16, %xmm8, %xmm8                             #881.70
        vmovdqu   %xmm11, 192(%rsp)                             #862.16[spill]
        vpaddd    %xmm0, %xmm3, %xmm4                           #899.80
        vpmulld   %xmm11, %xmm9, %xmm11                         #876.17
        vpsrld    $16, %xmm6, %xmm0                             #881.17
        vpmulld   %xmm5, %xmm10, %xmm5                          #871.17
        vpand     %xmm15, %xmm11, %xmm3                         #889.17
        vpaddd    %xmm0, %xmm5, %xmm5                           #885.17
        vpand     %xmm15, %xmm12, %xmm1                         #889.84
        vpaddd    %xmm5, %xmm3, %xmm3                           #894.17
        vpsrld    $16, %xmm4, %xmm0                             #900.70
        vpaddd    %xmm3, %xmm2, %xmm6                           #901.17
        vpsrld    $16, %xmm11, %xmm11                           #882.17
        vpmulld   224(%rsp), %xmm13, %xmm2                      #871.80[spill]
        vpsrld    $16, %xmm12, %xmm12                           #882.70
        vpaddd    %xmm8, %xmm2, %xmm3                           #885.80
        vpand     %xmm15, %xmm7, %xmm7                          #906.17
        vpaddd    %xmm3, %xmm1, %xmm2                           #894.80
        vpsrld    $16, %xmm6, %xmm1                             #902.17
        vmovdqu   240(%rsp), %xmm3                              #890.17[spill]
        vpaddd    %xmm2, %xmm0, %xmm8                           #901.80
        vpmulld   192(%rsp), %xmm10, %xmm0                      #870.17[spill]
        vpand     %xmm15, %xmm3, %xmm5                          #890.17
        vpaddd    %xmm11, %xmm0, %xmm0                          #886.17
        vpsrld    $16, %xmm3, %xmm3                             #883.17
        vpaddd    %xmm0, %xmm5, %xmm2                           #895.17
        vpslld    $16, %xmm6, %xmm6                             #909.23
        vpmulld   208(%rsp), %xmm13, %xmm11                     #870.80[spill]
        vpaddd    %xmm2, %xmm1, %xmm5                           #903.17
        vmovdqu   256(%rsp), %xmm1                              #890.84[spill]
        vpaddd    %xmm12, %xmm11, %xmm11                        #886.80
        vpand     %xmm15, %xmm1, %xmm0                          #890.84
        vpsrld    $16, %xmm8, %xmm2                             #902.70
        vpaddd    %xmm11, %xmm0, %xmm0                          #895.80
        vpsrld    $16, %xmm5, %xmm12                            #904.17
        vmovups   128(%rsp), %xmm11                             #860.16[spill]
        vpaddd    %xmm0, %xmm2, %xmm2                           #903.80
        vpsrld    $16, %xmm11, %xmm0                            #860.16
        vpand     %xmm15, %xmm5, %xmm5                          #907.17
        vpmulld   160(%rsp), %xmm10, %xmm10                     #869.17[spill]
        vpmulld   %xmm0, %xmm9, %xmm9                           #874.17
        vpand     %xmm15, %xmm9, %xmm0                          #891.17
        vpaddd    %xmm3, %xmm10, %xmm9                          #908.22
        vpaddd    %xmm9, %xmm0, %xmm0                           #908.22
        vpsrld    $16, %xmm2, %xmm9                             #904.70
        vpaddd    %xmm0, %xmm12, %xmm12                         #908.22
        vpand     %xmm15, %xmm2, %xmm2                          #907.84
        vmovups   144(%rsp), %xmm0                              #860.69[spill]
        vpslld    $16, %xmm12, %xmm10                           #908.22
        vpsrld    $16, %xmm0, %xmm3                             #860.69
        vpsrld    $16, %xmm1, %xmm0                             #883.70
        vpmulld   176(%rsp), %xmm13, %xmm13                     #869.80[spill]
        vpaddd    %xmm5, %xmm10, %xmm10                         #910.22
        vpmulld   %xmm3, %xmm14, %xmm14                         #874.80
        vpaddd    %xmm0, %xmm13, %xmm5                          #908.80
        vpand     %xmm15, %xmm14, %xmm3                         #891.84
        vpand     %xmm15, %xmm4, %xmm15                         #906.84
        vpaddd    %xmm5, %xmm3, %xmm13                          #908.80
        vpslld    $16, %xmm8, %xmm3                             #909.82
        vpaddd    %xmm13, %xmm9, %xmm12                         #908.80
        vmovd     %r9d, %xmm4                                   #912.23
        vpslld    $16, %xmm12, %xmm9                            #908.80
        vpaddd    %xmm15, %xmm3, %xmm15                         #911.98
        vpshufd   $0, %xmm4, %xmm3                              #912.23
        vmovd     %r8d, %xmm5                                   #914.28
        vpaddd    %xmm2, %xmm9, %xmm0                           #910.95
        vpaddd    %xmm7, %xmm6, %xmm9                           #911.23
        vpand     96(%rsp), %xmm3, %xmm2                        #913.21[spill]
        vpsrld    $9, %xmm10, %xmm13                            #916.24
        vpshufd   $0, %xmm5, %xmm7                              #914.28
        vpsrld    $9, %xmm0, %xmm8                              #916.88
        vmovups   .L_2il0floatpacket.21(%rip), %ymm6            #919.15
        vpxor     %xmm7, %xmm2, %xmm12                          #915.26
        vpand     112(%rsp), %xmm3, %xmm3                       #913.94[spill]
        vpor      %xmm12, %xmm13, %xmm4                         #917.24
        vpxor     %xmm7, %xmm3, %xmm14                          #915.111
        vmovd     %edi, %xmm12                                  #924.21
        vpor      %xmm14, %xmm8, %xmm11                         #917.109
        vmovd     %esi, %xmm14                                  #926.28
        vpshufd   $0, %xmm12, %xmm7                             #924.21
        vinsertf128 $1, %xmm11, %ymm4, %ymm1                    #918.17
        vaddps    %ymm6, %ymm1, %ymm8                           #920.19
        vsubps    %ymm6, %ymm8, %ymm5                           #921.9
        vpshufd   $0, %xmm14, %xmm6                             #926.28
        vpxor     %xmm6, %xmm2, %xmm11                          #927.26
        vpxor     %xmm6, %xmm3, %xmm14                          #927.111
        vsubps    %ymm5, %ymm1, %ymm13                          #922.17
        vmovd     %ecx, %xmm1                                   #928.26
        vpshufd   $0, %xmm1, %xmm1                              #928.26
        vpand     %xmm9, %xmm1, %xmm5                           #929.24
        vpand     %xmm15, %xmm1, %xmm1                          #929.107
        vpslld    $5, %xmm5, %xmm12                             #930.24
        vpslld    $5, %xmm1, %xmm5                              #930.90
        vpor      %xmm11, %xmm12, %xmm6                         #931.24
        vpor      %xmm14, %xmm5, %xmm12                         #931.109
        vpsrld    $18, %xmm9, %xmm9                             #940.23
        vpsrld    $18, %xmm15, %xmm15                           #940.88
        vinsertf128 $1, %xmm12, %ymm6, %ymm1                    #932.17
        vmovd     %edx, %xmm6                                   #935.28
        vinsertf128 $1, %xmm14, %ymm11, %ymm5                   #933.15
        vpshufd   $0, %xmm6, %xmm11                             #935.28
        vsubps    %ymm5, %ymm1, %ymm12                          #934.17
        vpxor     %xmm11, %xmm2, %xmm5                          #936.26
        vpxor     %xmm11, %xmm3, %xmm2                          #936.111
        vmovd     %r15d, %xmm3                                  #937.26
        vpshufd   $0, %xmm3, %xmm3                              #937.26
        vpand     %xmm10, %xmm3, %xmm10                         #938.24
        vpand     %xmm0, %xmm3, %xmm0                           #938.106
        vpslld    $14, %xmm10, %xmm1                            #939.24
        vpslld    $14, %xmm0, %xmm3                             #939.91
        vpor      %xmm9, %xmm1, %xmm1                           #941.24
        vpor      %xmm15, %xmm3, %xmm0                          #941.106
        vpor      %xmm5, %xmm1, %xmm6                           #942.24
        vpor      %xmm2, %xmm0, %xmm3                           #942.109
        vinsertf128 $1, %xmm3, %ymm6, %ymm1                     #943.18
        vinsertf128 $1, %xmm2, %ymm5, %ymm2                     #944.15
        vsubps    %ymm2, %ymm1, %ymm0                           #945.18
        vmovups   .L_2il0floatpacket.24(%rip), %ymm6            #952.21
        vaddps    %ymm0, %ymm13, %ymm5                          #946.11
        vsubps    %ymm5, %ymm13, %ymm3                          #947.17
        vmovups   .L_2il0floatpacket.23(%rip), %ymm13           #951.20
        vaddps    %ymm3, %ymm0, %ymm2                           #948.18
        vaddps    %ymm2, %ymm12, %ymm1                          #949.11
        vandps    .L_2il0floatpacket.25(%rip), %ymm5, %ymm12    #954.20
        vsubps    %ymm12, %ymm5, %ymm9                          #955.21
        vmulps    %ymm12, %ymm6, %ymm11                         #958.16
        vmulps    .L_2il0floatpacket.22(%rip), %ymm1, %ymm14    #959.16
        vmulps    %ymm9, %ymm13, %ymm10                         #957.16
        vmulps    %ymm9, %ymm6, %ymm15                          #960.16
        vmulps    %ymm12, %ymm13, %ymm0                         #956.20
        vmovups   .L_2il0floatpacket.27(%rip), %ymm12           #968.16
        vaddps    %ymm11, %ymm10, %ymm3                         #961.17
        vaddps    %ymm15, %ymm14, %ymm2                         #962.17
        vaddps    %ymm2, %ymm3, %ymm5                           #963.17
        vaddps    %ymm5, %ymm0, %ymm6                           #964.17
        vsubps    %ymm6, %ymm0, %ymm0                           #965.20
        vaddps    %ymm0, %ymm5, %ymm14                          #966.17
        vmovups   (%rsp), %ymm0                                 #969.11[spill]
        vandps    .L_2il0floatpacket.26(%rip), %ymm0, %ymm13    #969.11
        vcmpgt_oqps %ymm12, %ymm13, %ymm11                      #970.17
        vcmple_oqps %ymm12, %ymm13, %ymm1                       #971.20
        vandps    %ymm0, %ymm1, %ymm9                           #972.24
        vandps    %ymm6, %ymm11, %ymm10                         #973.25
        vorps     %ymm10, %ymm9, %ymm3                          #974.9
        vandps    %ymm14, %ymm11, %ymm2                         #975.9
        vmulps    %ymm3, %ymm3, %ymm5                           #978.15
        vextractf128 $1, %ymm8, %xmm4                           #923.118
        vpand     %xmm7, %xmm8, %xmm8                           #925.19
        vpslld    $4, %xmm8, %xmm13                             #979.183
        vmovd     %xmm13, %r11d                                 #979.308
        vpand     %xmm7, %xmm4, %xmm4                           #925.88
        vpslld    $4, %xmm4, %xmm8                              #979.245
        vmovd     %xmm8, %esi                                   #979.589
        vmovd     (%r11,%r10), %xmm7                            #979.1845
        vpextrd   $1, %xmm13, %r9d                              #979.376
        vpextrd   $2, %xmm13, %r8d                              #979.447
        vpextrd   $3, %xmm13, %edi                              #979.518
        vmovd     (%r9,%r10), %xmm13                            #979.1934
        vpextrd   $1, %xmm8, %ecx                               #979.657
        vpextrd   $2, %xmm8, %edx                               #979.728
        vpextrd   $3, %xmm8, %r15d                              #979.799
        vpunpcklqdq %xmm13, %xmm7, %xmm1                        #979.1825
        vmovd     (%r8,%r10), %xmm12                            #979.2044
        vmovd     (%rdi,%r10), %xmm7                            #979.2133
        vmovd     (%rsi,%r10), %xmm9                            #979.2286
        vmovd     (%rcx,%r10), %xmm10                           #979.2375
        vmovd     (%rdx,%r10), %xmm11                           #979.2485
        vmovd     (%r15,%r10), %xmm14                           #979.2574
        vpunpcklqdq %xmm7, %xmm12, %xmm4                        #979.2024
        vpunpcklqdq %xmm10, %xmm9, %xmm15                       #979.2266
        vpunpcklqdq %xmm14, %xmm11, %xmm13                      #979.2465
        vshufps   $136, %xmm4, %xmm1, %xmm6                     #979.1805
        vshufps   $136, %xmm13, %xmm15, %xmm12                  #979.2246
        vmovd     4(%rsi,%r10), %xmm14                          #980.2286
        vmovd     4(%rcx,%r10), %xmm15                          #980.2375
        vmovd     4(%r11,%r10), %xmm1                           #980.1845
        vmovd     4(%r9,%r10), %xmm4                            #980.1934
        vmovd     4(%r8,%r10), %xmm7                            #980.2044
        vmovd     4(%rdi,%r10), %xmm8                           #980.2133
        vpunpcklqdq %xmm15, %xmm14, %xmm15                      #980.2266
        vmovd     4(%r15,%r10), %xmm14                          #980.2574
        vpunpcklqdq %xmm4, %xmm1, %xmm9                         #980.1825
        vpunpcklqdq %xmm8, %xmm7, %xmm10                        #980.2024
        vshufps   $136, %xmm10, %xmm9, %xmm11                   #980.1805
        vmovd     8(%r9,%r10), %xmm7                            #981.1934
        vinsertf128 $1, %xmm12, %ymm6, %ymm13                   #979.1741
        vmovd     4(%rdx,%r10), %xmm12                          #980.2485
        vpunpcklqdq %xmm14, %xmm12, %xmm1                       #980.2465
        vshufps   $136, %xmm1, %xmm15, %xmm4                    #980.2246
        vmovd     8(%r11,%r10), %xmm6                           #981.1845
        vpunpcklqdq %xmm7, %xmm6, %xmm7                         #981.1825
        vmovd     8(%rdi,%r10), %xmm6                           #981.2133
        vmovups   64(%rsp), %ymm14                              #981.2133[spill]
        vmovups   32(%rsp), %ymm1                               #981.2133[spill]
        vinsertf128 $1, %xmm4, %ymm11, %ymm12                   #980.1741
        vmovd     8(%r8,%r10), %xmm4                            #981.2044
                                # LOE rdx rcx rbx rsi rdi r8 r9 r10 r11 r12 r13 r14 r15 eax xmm4 xmm6 xmm7 ymm0 ymm1 ymm2 ymm3 ymm5 ymm12 ymm13 ymm14
..B1.16:                        # Preds ..B1.12
                                # Execution count [5.00e-02]: Infreq
        vpunpcklqdq %xmm6, %xmm4, %xmm15                        #981.2024
        vmovd     8(%rsi,%r10), %xmm6                           #981.2286
        vmovd     8(%rcx,%r10), %xmm4                           #981.2375
        vmovd     8(%rdx,%r10), %xmm11                          #981.2485
        vmovd     8(%r15,%r10), %xmm8                           #981.2574
        vshufps   $136, %xmm15, %xmm7, %xmm9                    #981.1805
        vpunpcklqdq %xmm4, %xmm6, %xmm7                         #981.2266
        vpunpcklqdq %xmm8, %xmm11, %xmm10                       #981.2465
        vshufps   $136, %xmm10, %xmm7, %xmm15                   #981.2246
        vmovd     12(%r9,%r10), %xmm6                           #982.1936
        vmovd     12(%r8,%r10), %xmm4                           #982.2046
        vmovd     12(%rdi,%r10), %xmm8                          #982.2135
        vpunpcklqdq %xmm8, %xmm4, %xmm10                        #982.2026
        vmovd     12(%r15,%r10), %xmm4                          #982.2576
        vinsertf128 $1, %xmm15, %ymm9, %ymm11                   #981.1741
        vmovd     12(%r11,%r10), %xmm9                          #982.1847
        vpunpcklqdq %xmm6, %xmm9, %xmm7                         #982.1827
        vmovd     12(%rsi,%r10), %xmm15                         #982.2288
        vmovd     12(%rcx,%r10), %xmm9                          #982.2377
        vmovd     12(%rdx,%r10), %xmm6                          #982.2487
        vshufps   $136, %xmm10, %xmm7, %xmm10                   #982.1807
        vpunpcklqdq %xmm9, %xmm15, %xmm8                        #982.2268
        vpunpcklqdq %xmm4, %xmm6, %xmm7                         #982.2467
        vshufps   $136, %xmm7, %xmm8, %xmm15                    #982.2248
        vmulps    %ymm13, %ymm3, %ymm7                          #983.19
        vinsertf128 $1, %xmm15, %ymm10, %ymm8                   #982.1743
        vmulps    %ymm8, %ymm3, %ymm10                          #984.22
        vaddps    %ymm10, %ymm12, %ymm4                         #985.22
        vaddps    %ymm4, %ymm7, %ymm9                           #986.21
        vsubps    %ymm4, %ymm12, %ymm6                          #987.18
        vaddps    %ymm6, %ymm10, %ymm15                         #988.18
        vsubps    %ymm9, %ymm4, %ymm10                          #989.18
        vmulps    4352+__svml_scos_ha_data_internal(%rip), %ymm5, %ymm4 #994.29
        vaddps    %ymm10, %ymm7, %ymm7                          #990.18
        vaddps    %ymm7, %ymm15, %ymm6                          #991.22
        vaddps    4288+__svml_scos_ha_data_internal(%rip), %ymm4, %ymm7 #994.14
        vmulps    %ymm7, %ymm5, %ymm10                          #995.14
        vaddps    %ymm8, %ymm13, %ymm7                          #997.13
        vmulps    4480+__svml_scos_ha_data_internal(%rip), %ymm5, %ymm13 #1000.29
        vmulps    %ymm10, %ymm3, %ymm4                          #996.14
        vmulps    %ymm12, %ymm3, %ymm3                          #1002.35
        vaddps    4416+__svml_scos_ha_data_internal(%rip), %ymm13, %ymm13 #1000.14
        vsubps    %ymm3, %ymm7, %ymm3                           #1002.13
        vmulps    %ymm13, %ymm5, %ymm5                          #1001.14
        vmulps    %ymm3, %ymm2, %ymm2                           #1003.31
        vmulps    %ymm3, %ymm4, %ymm4                           #1005.33
        vmulps    %ymm5, %ymm12, %ymm12                         #1004.32
        vaddps    %ymm2, %ymm11, %ymm2                          #1003.16
        vaddps    %ymm4, %ymm6, %ymm5                           #1005.18
        vaddps    %ymm12, %ymm2, %ymm2                          #1004.17
        vaddps    %ymm5, %ymm2, %ymm6                           #1006.17
        vaddps    %ymm6, %ymm9, %ymm7                           #1007.20
        vblendvps %ymm1, %ymm7, %ymm14, %ymm14                  #1008.14
        jmp       ..B1.2        # Prob 100%                     #1008.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 eax ymm0 ymm14
	.cfi_endproc
# mark_end;
	.type	__svml_cosf8_ha_e9,@function
	.size	__svml_cosf8_ha_e9,.-__svml_cosf8_ha_e9
..LN__svml_cosf8_ha_e9.0:
	.data
# -- End  __svml_cosf8_ha_e9
	.text
.L_2__routine_start___svml_cosf4_ha_ex_1:
# -- Begin  __svml_cosf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosf4_ha_ex
# --- __svml_cosf4_ha_ex(__m128)
__svml_cosf4_ha_ex:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1020.1
        .byte     15                                            #1362.14
        .byte     30                                            #1362.14
        .byte     250                                           #1362.14
	.cfi_startproc
..___tag_value___svml_cosf4_ha_ex.49:
..L50:
                                                         #1020.1
        pushq     %rbp                                          #1020.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1020.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1020.1
        subq      $192, %rsp                                    #1020.1
        xorl      %eax, %eax                                    #1036.1
        movups    4096+__svml_scos_ha_data_internal(%rip), %xmm8 #1039.10
        movups    5504+__svml_scos_ha_data_internal(%rip), %xmm10 #1037.44
        andps     %xmm0, %xmm8                                  #1039.10
        movups    5440+__svml_scos_ha_data_internal(%rip), %xmm9 #1061.54
        movaps    %xmm8, %xmm11                                 #1069.75
        movups    4992+__svml_scos_ha_data_internal(%rip), %xmm13 #1070.52
        movaps    %xmm8, %xmm7                                  #1042.26
        movaps    %xmm13, %xmm12                                #1071.40
        addps     %xmm8, %xmm10                                 #1040.9
        cvtps2pd  %xmm8, %xmm5                                  #1069.23
        cmpnleps  4160+__svml_scos_ha_data_internal(%rip), %xmm7 #1042.26
        mulps     5376+__svml_scos_ha_data_internal(%rip), %xmm10 #1063.29
        movmskps  %xmm7, %edx                                   #1088.74
        movhlps   %xmm8, %xmm11                                 #1069.75
        addps     %xmm9, %xmm10                                 #1063.17
        cvtps2pd  %xmm11, %xmm4                                 #1069.61
        movaps    %xmm10, %xmm6                                 #1064.38
        subps     %xmm9, %xmm10                                 #1065.17
        movups    5056+__svml_scos_ha_data_internal(%rip), %xmm15 #1072.52
        pslld     $31, %xmm6                                    #1064.38
        movups    5312+__svml_scos_ha_data_internal(%rip), %xmm9 #1075.51
        movups    5184+__svml_scos_ha_data_internal(%rip), %xmm11 #1078.51
        subps     5568+__svml_scos_ha_data_internal(%rip), %xmm10 #1067.17
        cvtps2pd  %xmm10, %xmm14                                #1068.23
        movhlps   %xmm10, %xmm10                                #1068.74
        cvtps2pd  %xmm10, %xmm1                                 #1068.60
        mulpd     %xmm14, %xmm12                                #1071.40
        mulpd     %xmm1, %xmm13                                 #1071.106
        mulpd     %xmm15, %xmm14                                #1073.40
        mulpd     %xmm15, %xmm1                                 #1073.106
        subpd     %xmm12, %xmm5                                 #1071.19
        subpd     %xmm13, %xmm4                                 #1071.85
        subpd     %xmm14, %xmm5                                 #1073.19
        subpd     %xmm1, %xmm4                                  #1073.85
        movaps    %xmm5, %xmm3                                  #1074.20
        movaps    %xmm4, %xmm2                                  #1074.63
        mulpd     %xmm5, %xmm3                                  #1074.20
        mulpd     %xmm4, %xmm2                                  #1074.63
        movaps    %xmm9, %xmm1                                  #1077.31
        mulpd     %xmm3, %xmm1                                  #1077.31
        mulpd     %xmm2, %xmm9                                  #1077.99
        movups    5248+__svml_scos_ha_data_internal(%rip), %xmm10 #1076.51
        addpd     %xmm10, %xmm1                                 #1077.19
        addpd     %xmm9, %xmm10                                 #1077.87
        mulpd     %xmm3, %xmm1                                  #1079.31
        mulpd     %xmm2, %xmm10                                 #1079.98
        addpd     %xmm11, %xmm1                                 #1079.19
        addpd     %xmm10, %xmm11                                #1079.86
        mulpd     %xmm3, %xmm1                                  #1081.31
        mulpd     %xmm2, %xmm11                                 #1081.98
        movups    5120+__svml_scos_ha_data_internal(%rip), %xmm12 #1080.51
        addpd     %xmm12, %xmm1                                 #1081.19
        addpd     %xmm11, %xmm12                                #1081.86
        mulpd     %xmm1, %xmm3                                  #1082.19
        mulpd     %xmm12, %xmm2                                 #1082.62
        mulpd     %xmm5, %xmm3                                  #1083.33
        mulpd     %xmm4, %xmm2                                  #1083.100
        addpd     %xmm3, %xmm5                                  #1083.21
        addpd     %xmm2, %xmm4                                  #1083.88
        cvtpd2ps  %xmm5, %xmm10                                 #1084.34
        cvtpd2ps  %xmm4, %xmm2                                  #1084.65
        movlhps   %xmm2, %xmm10                                 #1084.18
        pxor      %xmm6, %xmm10                                 #1085.18
        testl     %edx, %edx                                    #1089.62
        jne       ..B2.12       # Prob 5%                       #1089.62
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm7 xmm8 xmm10
..B2.2:                         # Preds ..B2.12 ..B2.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #1365.52
        jne       ..B2.4        # Prob 5%                       #1365.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm10
..B2.3:                         # Preds ..B2.4 ..B2.10 ..B2.2
                                # Execution count [1.00e+00]
        movaps    %xmm10, %xmm0                                 #1368.12
        movq      %rbp, %rsp                                    #1368.12
        popq      %rbp                                          #1368.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1368.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.4:                         # Preds ..B2.2
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #1365.193
        movups    %xmm10, 128(%rsp)                             #1365.264
        je        ..B2.3        # Prob 95%                      #1365.368
                                # LOE rbx r12 r13 r14 r15 eax xmm10
..B2.7:                         # Preds ..B2.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1365.448
        movq      %r12, 8(%rsp)                                 #1365.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #1365.448
        movq      %r13, (%rsp)                                  #1365.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #1365.448
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.9 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1365.511
        jc        ..B2.11       # Prob 5%                       #1365.511
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.11 ..B2.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1365.464
        cmpl      $4, %r12d                                     #1365.459
        jl        ..B2.8        # Prob 82%                      #1365.459
                                # LOE rbx r12 r14 r15 r13d
..B2.10:                        # Preds ..B2.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm10                             #1365.665
        jmp       ..B2.3        # Prob 100%                     #1365.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm10
..B2.11:                        # Preds ..B2.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1365.540
        lea       128(%rsp,%r12,4), %rsi                        #1365.540
..___tag_value___svml_cosf4_ha_ex.67:
#       __svml_scos_ha_cout_rare_internal(const float *, float *)
        call      __svml_scos_ha_cout_rare_internal             #1365.540
..___tag_value___svml_cosf4_ha_ex.68:
        jmp       ..B2.9        # Prob 100%                     #1365.540
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B2.12:                        # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        movl      $2139095040, %edx                             #1200.18
        lea       __svml_scos_ha_reduction_data_internal(%rip), %r9 #1203.803
        movups    4224+__svml_scos_ha_data_internal(%rip), %xmm15 #1111.50
        movl      $8388607, %r10d                               #1209.21
        andps     %xmm15, %xmm8                                 #1112.14
        movl      $8388608, %r11d                               #1210.20
        movups    %xmm7, 16(%rsp)                               #[spill]
        movd      %edx, %xmm12                                  #1200.18
        movl      $65535, %edx                                  #1213.17
        pshufd    $0, %xmm12, %xmm11                            #1200.18
        pand      %xmm0, %xmm11                                 #1201.16
        psrld     $23, %xmm11                                   #1202.16
        movdqa    %xmm11, %xmm14                                #1203.232
        pslld     $1, %xmm14                                    #1203.232
        paddd     %xmm11, %xmm14                                #1203.355
        movd      %r10d, %xmm11                                 #1209.21
        pslld     $2, %xmm14                                    #1203.355
        movl      $872415232, %r10d                             #1289.22
        pshufd    $2, %xmm14, %xmm7                             #1203.608
        pshufd    $3, %xmm14, %xmm3                             #1203.712
        cmpeqps   %xmm15, %xmm8                                 #1113.21
        movd      %xmm7, %edi                                   #1203.589
        movd      %xmm3, %r8d                                   #1203.693
        movups    %xmm10, 32(%rsp)                              #[spill]
        pshufd    $1, %xmm14, %xmm10                            #1203.504
        movd      %xmm14, %ecx                                  #1203.423
        movd      %xmm10, %esi                                  #1203.485
        movd      (%rdi,%r9), %xmm2                             #1203.1517
        movd      (%r8,%r9), %xmm9                              #1203.1600
        movd      4(%r8,%r9), %xmm5                             #1204.1601
        punpcklqdq %xmm9, %xmm2                                 #1203.1497
        movd      4(%rcx,%r9), %xmm9                            #1204.1331
        movd      4(%rsi,%r9), %xmm4                            #1204.1414
        movd      4(%rdi,%r9), %xmm15                           #1204.1518
        movmskps  %xmm8, %eax                                   #1115.44
        movd      (%rcx,%r9), %xmm8                             #1203.1330
        movd      (%rsi,%r9), %xmm6                             #1203.1413
        punpcklqdq %xmm4, %xmm9                                 #1204.1312
        punpcklqdq %xmm5, %xmm15                                #1204.1498
        pshufd    $0, %xmm11, %xmm14                            #1209.21
        movd      8(%rsi,%r9), %xmm1                            #1205.1413
        pand      %xmm0, %xmm14                                 #1211.19
        punpcklqdq %xmm6, %xmm8                                 #1203.1311
        movl      $1065353216, %esi                             #1268.22
        shufps    $136, %xmm15, %xmm9                           #1204.1292
        movd      8(%rcx,%r9), %xmm4                            #1205.1330
        movl      $-2147483648, %ecx                            #1266.17
        movd      8(%rdi,%r9), %xmm12                           #1205.1517
        movl      $1195376640, %edi                             #1273.33
        movd      8(%r8,%r9), %xmm15                            #1205.1600
        movl      $262143, %r9d                                 #1282.20
        movups    %xmm0, (%rsp)                                 #[spill]
        movd      %r11d, %xmm0                                  #1210.20
        shufps    $136, %xmm2, %xmm8                            #1203.1291
        movl      $511, %r11d                                   #1291.20
        punpcklqdq %xmm1, %xmm4                                 #1205.1311
        movl      $679477248, %r8d                              #1280.22
        punpcklqdq %xmm15, %xmm12                               #1205.1497
        movd      %edx, %xmm15                                  #1213.17
        pshufd    $0, %xmm0, %xmm0                              #1210.20
        movl      $1086918619, %edx                             #1304.38
        shufps    $136, %xmm12, %xmm4                           #1205.1291
        movaps    %xmm8, %xmm12                                 #1214.10
        pshufd    $0, %xmm15, %xmm7                             #1213.17
        paddd     %xmm0, %xmm14                                 #1212.19
        psrld     $16, %xmm12                                   #1214.10
        pand      %xmm7, %xmm8                                  #1215.10
        movdqu    %xmm12, 48(%rsp)                              #1214.10[spill]
        movdqa    %xmm14, %xmm12                                #1220.10
        pand      %xmm7, %xmm14                                 #1221.10
        movaps    %xmm8, %xmm15                                 #1223.135
        movdqa    %xmm14, %xmm10                                #1228.93
        psrlq     $32, %xmm15                                   #1223.135
        psrlq     $32, %xmm10                                   #1228.93
        movdqa    %xmm14, %xmm13                                #1229.177
        movdqa    %xmm10, %xmm6                                 #1229.221
        movaps    %xmm9, %xmm0                                  #1216.10
        pmuludq   %xmm8, %xmm13                                 #1229.177
        psrld     $16, %xmm0                                    #1216.10
        pmuludq   %xmm15, %xmm6                                 #1229.221
        movups    %xmm8, 64(%rsp)                               #1215.10[spill]
        psllq     $32, %xmm6                                    #1229.283
        movdqu    .L_2il0floatpacket.28(%rip), %xmm8            #1229.251
        pand      %xmm7, %xmm9                                  #1217.10
        pand      %xmm8, %xmm13                                 #1229.251
        movdqa    %xmm0, %xmm1                                  #1224.135
        por       %xmm6, %xmm13                                 #1229.318
        psrlq     $32, %xmm1                                    #1224.135
        movdqu    %xmm13, 128(%rsp)                             #1229.318[spill]
        movaps    %xmm9, %xmm5                                  #1225.135
        movdqa    %xmm14, %xmm6                                 #1230.177
        movdqa    %xmm10, %xmm13                                #1230.221
        movdqu    %xmm0, 80(%rsp)                               #1216.10[spill]
        psrlq     $32, %xmm5                                    #1225.135
        movdqu    %xmm1, 112(%rsp)                              #1224.135[spill]
        psrld     $16, %xmm12                                   #1220.10
        pmuludq   %xmm0, %xmm6                                  #1230.177
        movdqa    %xmm10, %xmm0                                 #1231.221
        pmuludq   %xmm1, %xmm13                                 #1230.221
        pmuludq   %xmm5, %xmm0                                  #1231.221
        movdqa    %xmm14, %xmm1                                 #1231.177
        psllq     $32, %xmm0                                    #1231.283
        pmuludq   %xmm9, %xmm1                                  #1231.177
        movaps    %xmm4, %xmm2                                  #1218.10
        pmuludq   %xmm12, %xmm9                                 #1225.177
        pand      %xmm8, %xmm1                                  #1231.251
        pand      %xmm7, %xmm4                                  #1219.10
        movdqa    %xmm12, %xmm11                                #1223.93
        por       %xmm0, %xmm1                                  #1231.318
        movdqa    %xmm12, %xmm0                                 #1227.177
        psrlq     $32, %xmm11                                   #1223.93
        pmuludq   %xmm4, %xmm0                                  #1227.177
        psrlq     $32, %xmm4                                    #1227.135
        pmuludq   %xmm11, %xmm4                                 #1227.221
        pmuludq   %xmm11, %xmm5                                 #1225.221
        psrld     $16, %xmm2                                    #1218.10
        pand      %xmm8, %xmm0                                  #1227.251
        movdqa    %xmm2, %xmm3                                  #1226.135
        psllq     $32, %xmm4                                    #1227.283
        movdqu    %xmm15, 96(%rsp)                              #1223.135[spill]
        psrlq     $32, %xmm3                                    #1226.135
        por       %xmm4, %xmm0                                  #1227.318
        movdqa    %xmm12, %xmm4                                 #1226.177
        movdqa    %xmm11, %xmm15                                #1226.221
        pand      %xmm8, %xmm6                                  #1230.251
        pmuludq   %xmm2, %xmm4                                  #1226.177
        psllq     $32, %xmm13                                   #1230.283
        pmuludq   %xmm3, %xmm15                                 #1226.221
        pmuludq   %xmm14, %xmm2                                 #1232.177
        pmuludq   %xmm10, %xmm3                                 #1232.221
        pand      %xmm8, %xmm2                                  #1232.251
        psllq     $32, %xmm3                                    #1232.283
        pand      %xmm8, %xmm4                                  #1226.251
        psllq     $32, %xmm15                                   #1226.283
        por       %xmm3, %xmm2                                  #1232.318
        por       %xmm13, %xmm6                                 #1230.318
        movdqa    %xmm1, %xmm13                                 #1242.11
        por       %xmm15, %xmm4                                 #1226.318
        psrld     $16, %xmm2                                    #1234.11
        pand      %xmm7, %xmm13                                 #1242.11
        paddd     %xmm2, %xmm4                                  #1238.11
        pand      %xmm8, %xmm9                                  #1225.251
        psllq     $32, %xmm5                                    #1225.283
        psrld     $16, %xmm0                                    #1252.11
        paddd     %xmm4, %xmm13                                 #1247.11
        movdqa    %xmm6, %xmm3                                  #1243.11
        por       %xmm5, %xmm9                                  #1225.318
        psrld     $16, %xmm1                                    #1235.11
        paddd     %xmm13, %xmm0                                 #1253.11
        pand      %xmm7, %xmm3                                  #1243.11
        paddd     %xmm1, %xmm9                                  #1239.11
        movdqa    %xmm0, %xmm13                                 #1254.11
        movdqu    80(%rsp), %xmm2                               #1224.177[spill]
        paddd     %xmm9, %xmm3                                  #1248.11
        movdqu    112(%rsp), %xmm9                              #1224.221[spill]
        psrld     $16, %xmm13                                   #1254.11
        pmuludq   %xmm12, %xmm2                                 #1224.177
        paddd     %xmm3, %xmm13                                 #1255.11
        pmuludq   %xmm11, %xmm9                                 #1224.221
        pmuludq   64(%rsp), %xmm12                              #1223.177[spill]
        pmuludq   96(%rsp), %xmm11                              #1223.221[spill]
        movdqu    48(%rsp), %xmm3                               #1228.177[spill]
        pand      %xmm8, %xmm2                                  #1224.251
        pmuludq   %xmm3, %xmm14                                 #1228.177
        psrlq     $32, %xmm3                                    #1228.135
        pmuludq   %xmm3, %xmm10                                 #1228.221
        movdqu    128(%rsp), %xmm5                              #1244.11[spill]
        psllq     $32, %xmm9                                    #1224.283
        movdqa    %xmm5, %xmm15                                 #1244.11
        por       %xmm9, %xmm2                                  #1224.318
        psrld     $16, %xmm6                                    #1236.11
        movdqa    %xmm13, %xmm1                                 #1256.11
        pand      %xmm7, %xmm15                                 #1244.11
        paddd     %xmm6, %xmm2                                  #1240.11
        psrld     $16, %xmm1                                    #1256.11
        paddd     %xmm2, %xmm15                                 #1249.11
        pand      %xmm8, %xmm14                                 #1228.251
        psllq     $32, %xmm10                                   #1228.283
        pand      %xmm8, %xmm12                                 #1223.251
        psllq     $32, %xmm11                                   #1223.283
        paddd     %xmm15, %xmm1                                 #1257.11
        por       %xmm10, %xmm14                                #1228.318
        por       %xmm11, %xmm12                                #1223.318
        psrld     $16, %xmm5                                    #1237.11
        movdqa    %xmm1, %xmm4                                  #1258.11
        pand      %xmm7, %xmm14                                 #1245.11
        paddd     %xmm5, %xmm12                                 #1262.16
        psrld     $16, %xmm4                                    #1258.11
        paddd     %xmm12, %xmm14                                #1262.16
        pand      %xmm7, %xmm1                                  #1261.11
        paddd     %xmm14, %xmm4                                 #1262.16
        pslld     $16, %xmm13                                   #1263.17
        pslld     $16, %xmm4                                    #1262.16
        pand      %xmm7, %xmm0                                  #1260.11
        movd      %ecx, %xmm8                                   #1266.17
        paddd     %xmm1, %xmm4                                  #1264.16
        pshufd    $0, %xmm8, %xmm5                              #1266.17
        paddd     %xmm0, %xmm13                                 #1265.17
        movups    (%rsp), %xmm0                                 #1267.15[spill]
        movd      %esi, %xmm10                                  #1268.22
        pshufd    $0, %xmm10, %xmm7                             #1268.22
        pand      %xmm0, %xmm5                                  #1267.15
        movdqa    %xmm4, %xmm2                                  #1270.18
        pxor      %xmm5, %xmm7                                  #1269.20
        psrld     $9, %xmm2                                     #1270.18
        movd      %edi, %xmm14                                  #1273.33
        por       %xmm7, %xmm2                                  #1271.18
        movd      %r11d, %xmm6                                  #1291.20
        pshufd    $0, %xmm14, %xmm15                            #1273.33
        movdqa    %xmm2, %xmm3                                  #1274.19
        pshufd    $0, %xmm6, %xmm1                              #1291.20
        movd      %r9d, %xmm11                                  #1282.20
        pshufd    $0, %xmm11, %xmm9                             #1282.20
        movd      %r8d, %xmm12                                  #1280.22
        movd      %r10d, %xmm7                                  #1289.22
        pand      %xmm4, %xmm1                                  #1292.18
        pshufd    $0, %xmm12, %xmm10                            #1280.22
        pand      %xmm13, %xmm9                                 #1283.18
        pshufd    $0, %xmm7, %xmm14                             #1289.22
        pslld     $14, %xmm1                                    #1293.18
        psrld     $18, %xmm13                                   #1294.17
        pxor      %xmm5, %xmm10                                 #1281.20
        pxor      %xmm14, %xmm5                                 #1290.20
        por       %xmm13, %xmm1                                 #1295.18
        por       %xmm5, %xmm1                                  #1296.18
        movl      $-4096, %edi                                  #1307.38
        pslld     $5, %xmm9                                     #1284.18
        movl      $1086918656, %ecx                             #1305.38
        por       %xmm10, %xmm9                                 #1285.18
        movl      $-1214941318, %esi                            #1306.39
        movd      %edx, %xmm13                                  #1304.38
        addps     %xmm15, %xmm3                                 #1274.19
        subps     %xmm5, %xmm1                                  #1299.18
        subps     %xmm10, %xmm9                                 #1288.17
        movaps    %xmm3, %xmm8                                  #1275.9
        movd      %edi, %xmm5                                   #1307.38
        movd      %esi, %xmm4                                   #1306.39
        subps     %xmm15, %xmm8                                 #1275.9
        pshufd    $0, %xmm13, %xmm11                            #1304.38
        subps     %xmm8, %xmm2                                  #1276.17
        movaps    %xmm2, %xmm15                                 #1300.11
        movl      $2147483647, %r8d                             #1321.33
        pshufd    $0, %xmm5, %xmm8                              #1307.38
        movl      $897581056, %r9d                              #1322.34
        pshufd    $0, %xmm4, %xmm12                             #1306.39
        movl      $255, %r10d                                   #1278.15
        lea       __svml_scos_ha_data_internal(%rip), %rdi      #1333.614
        addps     %xmm1, %xmm15                                 #1300.11
        andps     %xmm15, %xmm8                                 #1308.20
        subps     %xmm15, %xmm2                                 #1301.17
        subps     %xmm8, %xmm15                                 #1309.21
        addps     %xmm2, %xmm1                                  #1302.18
        movd      %ecx, %xmm2                                   #1305.38
        addps     %xmm1, %xmm9                                  #1303.11
        pshufd    $0, %xmm2, %xmm6                              #1305.38
        movdqa    %xmm6, %xmm10                                 #1310.20
        mulps     %xmm8, %xmm10                                 #1310.20
        mulps     %xmm15, %xmm6                                 #1311.16
        mulps     %xmm12, %xmm8                                 #1312.16
        mulps     %xmm9, %xmm11                                 #1313.16
        mulps     %xmm15, %xmm12                                #1314.16
        addps     %xmm8, %xmm6                                  #1315.17
        addps     %xmm12, %xmm11                                #1316.17
        movd      %r8d, %xmm9                                   #1321.33
        movd      %r9d, %xmm15                                  #1322.34
        pshufd    $0, %xmm9, %xmm8                              #1321.33
        addps     %xmm11, %xmm6                                 #1317.17
        movaps    %xmm6, %xmm7                                  #1318.17
        andps     %xmm0, %xmm8                                  #1323.11
        pshufd    $0, %xmm15, %xmm11                            #1322.34
        addps     %xmm10, %xmm7                                 #1318.17
        movdqa    %xmm11, %xmm12                                #1324.17
        subps     %xmm7, %xmm10                                 #1319.20
        cmpltps   %xmm8, %xmm12                                 #1324.17
        cmpleps   %xmm11, %xmm8                                 #1325.20
        addps     %xmm10, %xmm6                                 #1320.17
        movd      %r10d, %xmm10                                 #1278.15
        andps     %xmm0, %xmm8                                  #1326.24
        pshufd    $0, %xmm10, %xmm14                            #1278.15
        andps     %xmm12, %xmm7                                 #1327.25
        pand      %xmm14, %xmm3                                 #1279.13
        orps      %xmm7, %xmm8                                  #1328.9
        pslld     $4, %xmm3                                     #1333.177
        movaps    %xmm8, %xmm11                                 #1332.15
        movd      %xmm3, %r11d                                  #1333.234
        andps     %xmm6, %xmm12                                 #1329.9
        pshufd    $1, %xmm3, %xmm13                             #1333.315
        pshufd    $2, %xmm3, %xmm1                              #1333.419
        pshufd    $3, %xmm3, %xmm3                              #1333.523
        movd      %xmm1, %ecx                                   #1333.400
        movd      %xmm3, %esi                                   #1333.504
        movd      %xmm13, %edx                                  #1333.296
        movd      8(%rcx,%rdi), %xmm5                           #1335.1267
        movd      8(%rsi,%rdi), %xmm4                           #1335.1350
        mulps     %xmm8, %xmm11                                 #1332.15
        movd      (%rsi,%rdi), %xmm7                            #1333.1350
        movd      (%r11,%rdi), %xmm10                           #1333.1080
        movd      (%rdx,%rdi), %xmm15                           #1333.1163
        movd      (%rcx,%rdi), %xmm14                           #1333.1267
        punpcklqdq %xmm4, %xmm5                                 #1335.1247
        movups    4352+__svml_scos_ha_data_internal(%rip), %xmm4 #1348.26
        punpcklqdq %xmm15, %xmm10                               #1333.1061
        punpcklqdq %xmm7, %xmm14                                #1333.1247
        movd      4(%r11,%rdi), %xmm7                           #1334.1080
        movd      4(%rdx,%rdi), %xmm13                          #1334.1163
        mulps     %xmm11, %xmm4                                 #1348.26
        movd      8(%rdx,%rdi), %xmm3                           #1335.1163
        movd      12(%rdx,%rdi), %xmm6                          #1336.1165
        shufps    $136, %xmm14, %xmm10                          #1333.1041
        addps     4288+__svml_scos_ha_data_internal(%rip), %xmm4 #1348.14
        punpcklqdq %xmm13, %xmm7                                #1334.1061
        movd      8(%r11,%rdi), %xmm14                          #1335.1080
        movd      12(%r11,%rdi), %xmm13                         #1336.1082
        movd      12(%rcx,%rdi), %xmm15                         #1336.1269
        movd      12(%rsi,%rdi), %xmm9                          #1336.1352
        punpcklqdq %xmm3, %xmm14                                #1335.1061
        movaps    %xmm8, %xmm3                                  #1337.19
        punpcklqdq %xmm6, %xmm13                                #1336.1063
        punpcklqdq %xmm9, %xmm15                                #1336.1249
        shufps    $136, %xmm5, %xmm14                           #1335.1041
        movaps    %xmm8, %xmm5                                  #1338.22
        shufps    $136, %xmm15, %xmm13                          #1336.1043
        mulps     %xmm13, %xmm5                                 #1338.22
        mulps     %xmm10, %xmm3                                 #1337.19
        addps     %xmm13, %xmm10                                #1351.13
        mulps     %xmm11, %xmm4                                 #1349.14
        movd      4(%rsi,%rdi), %xmm1                           #1334.1350
        movd      4(%rcx,%rdi), %xmm2                           #1334.1267
        punpcklqdq %xmm1, %xmm2                                 #1334.1247
        shufps    $136, %xmm2, %xmm7                            #1334.1041
        movaps    %xmm7, %xmm2                                  #1339.22
        movaps    %xmm7, %xmm1                                  #1341.18
        movups    4480+__svml_scos_ha_data_internal(%rip), %xmm13 #1354.26
        addps     %xmm5, %xmm2                                  #1339.22
        mulps     %xmm8, %xmm4                                  #1350.14
        mulps     %xmm11, %xmm13                                #1354.26
        mulps     %xmm7, %xmm8                                  #1356.32
        subps     %xmm2, %xmm1                                  #1341.18
        addps     4416+__svml_scos_ha_data_internal(%rip), %xmm13 #1354.14
        subps     %xmm8, %xmm10                                 #1356.13
        addps     %xmm1, %xmm5                                  #1342.18
        mulps     %xmm13, %xmm11                                #1355.14
        mulps     %xmm10, %xmm12                                #1357.28
        mulps     %xmm10, %xmm4                                 #1359.30
        mulps     %xmm11, %xmm7                                 #1358.29
        addps     %xmm12, %xmm14                                #1357.16
        movaps    %xmm2, %xmm15                                 #1340.21
        addps     %xmm7, %xmm14                                 #1358.17
        addps     %xmm3, %xmm15                                 #1340.21
        movups    16(%rsp), %xmm8                               #1362.25[spill]
        subps     %xmm15, %xmm2                                 #1343.18
        movaps    %xmm8, %xmm10                                 #1362.25
        addps     %xmm2, %xmm3                                  #1344.18
        andnps    32(%rsp), %xmm10                              #1362.25[spill]
        addps     %xmm3, %xmm5                                  #1345.22
        addps     %xmm4, %xmm5                                  #1359.18
        addps     %xmm5, %xmm14                                 #1360.17
        addps     %xmm14, %xmm15                                #1361.20
        andps     %xmm8, %xmm15                                 #1362.74
        orps      %xmm15, %xmm10                                #1362.14
        jmp       ..B2.2        # Prob 100%                     #1362.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm10
	.cfi_endproc
# mark_end;
	.type	__svml_cosf4_ha_ex,@function
	.size	__svml_cosf4_ha_ex,.-__svml_cosf4_ha_ex
..LN__svml_cosf4_ha_ex.1:
	.data
# -- End  __svml_cosf4_ha_ex
	.text
.L_2__routine_start___svml_cosf4_ha_l9_2:
# -- Begin  __svml_cosf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosf4_ha_l9
# --- __svml_cosf4_ha_l9(__m128)
__svml_cosf4_ha_l9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1374.1
        .byte     15                                            #1703.14
        .byte     30                                            #1703.14
        .byte     250                                           #1703.14
	.cfi_startproc
..___tag_value___svml_cosf4_ha_l9.90:
..L91:
                                                         #1374.1
        pushq     %rbp                                          #1374.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1374.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1374.1
        subq      $192, %rsp                                    #1374.1
        vmovaps   %xmm0, %xmm14                                 #1374.1
        vandps    4096+__svml_scos_ha_data_internal(%rip), %xmm14, %xmm12 #1393.10
        xorl      %eax, %eax                                    #1390.1
        vmovups   5440+__svml_scos_ha_data_internal(%rip), %xmm2 #1415.54
        vcvtps2pd %xmm12, %ymm5                                 #1423.23
        vmovupd   5312+__svml_scos_ha_data_internal(%rip), %ymm7 #1429.52
        vaddps    5504+__svml_scos_ha_data_internal(%rip), %xmm12, %xmm1 #1394.9
        vcmpnleps 4160+__svml_scos_ha_data_internal(%rip), %xmm12, %xmm0 #1396.26
        vfmadd132ps 5376+__svml_scos_ha_data_internal(%rip), %xmm2, %xmm1 #1417.17
        vmovmskps %xmm0, %edx                                   #1442.74
        vpslld    $31, %xmm1, %xmm11                            #1418.38
        vsubps    %xmm2, %xmm1, %xmm3                           #1419.17
        vsubps    5568+__svml_scos_ha_data_internal(%rip), %xmm3, %xmm4 #1421.17
        vcvtps2pd %xmm4, %ymm8                                  #1422.23
        vfnmadd231pd 4992+__svml_scos_ha_data_internal(%rip), %ymm8, %ymm5 #1425.17
        vfnmadd132pd 5056+__svml_scos_ha_data_internal(%rip), %ymm5, %ymm8 #1427.17
        vmulpd    %ymm8, %ymm8, %ymm6                           #1428.18
        vfmadd213pd 5248+__svml_scos_ha_data_internal(%rip), %ymm6, %ymm7 #1431.17
        vfmadd213pd 5184+__svml_scos_ha_data_internal(%rip), %ymm6, %ymm7 #1433.17
        vfmadd213pd 5120+__svml_scos_ha_data_internal(%rip), %ymm6, %ymm7 #1435.17
        vmulpd    %ymm7, %ymm6, %ymm9                           #1436.17
        vfmadd213pd %ymm8, %ymm8, %ymm9                         #1437.19
        vcvtpd2ps %ymm9, %xmm10                                 #1438.18
        vxorps    %xmm11, %xmm10, %xmm1                         #1439.18
        testl     %edx, %edx                                    #1443.62
        jne       ..B3.12       # Prob 5%                       #1443.62
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1 xmm12 xmm14
..B3.2:                         # Preds ..B3.12 ..B3.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #1706.52
        jne       ..B3.4        # Prob 5%                       #1706.52
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm14
..B3.15:                        # Preds ..B3.4 ..B3.2
                                # Execution count [9.50e-01]
        vzeroupper                                              #
                                # LOE rbx r12 r13 r14 r15 xmm1
..B3.3:                         # Preds ..B3.10 ..B3.15
                                # Execution count [1.00e+00]
        movaps    %xmm1, %xmm0                                  #1709.12
        movq      %rbp, %rsp                                    #1709.12
        popq      %rbp                                          #1709.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1709.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.4:                         # Preds ..B3.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm14, 64(%rsp)                              #1706.193
        vmovups   %xmm1, 128(%rsp)                              #1706.264
        je        ..B3.15       # Prob 95%                      #1706.368
                                # LOE rbx r12 r13 r14 r15 eax xmm1
..B3.7:                         # Preds ..B3.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1706.448
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
        btl       %r12d, %r13d                                  #1706.511
        jc        ..B3.11       # Prob 5%                       #1706.511
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.11 ..B3.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1706.464
        cmpl      $4, %r12d                                     #1706.459
        jl        ..B3.8        # Prob 82%                      #1706.459
                                # LOE rbx r12 r14 r15 r13d
..B3.10:                        # Preds ..B3.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm1                              #1706.665
        jmp       ..B3.3        # Prob 100%                     #1706.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm1
..B3.11:                        # Preds ..B3.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1706.540
        lea       128(%rsp,%r12,4), %rsi                        #1706.540
..___tag_value___svml_cosf4_ha_l9.108:
#       __svml_scos_ha_cout_rare_internal(const float *, float *)
        call      __svml_scos_ha_cout_rare_internal             #1706.540
..___tag_value___svml_cosf4_ha_l9.109:
        jmp       ..B3.9        # Prob 100%                     #1706.540
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B3.12:                        # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vpand     .L_2il0floatpacket.29(%rip), %xmm14, %xmm13   #1555.18
        lea       __svml_scos_ha_reduction_data_internal(%rip), %r8 #1557.708
        vmovups   4224+__svml_scos_ha_data_internal(%rip), %xmm10 #1465.50
        vpsrld    $23, %xmm13, %xmm8                            #1556.18
        vandps    %xmm12, %xmm10, %xmm11                        #1466.14
        vpslld    $1, %xmm8, %xmm7                              #1557.234
        vpaddd    %xmm8, %xmm7, %xmm4                           #1557.367
        vcmpeqps  %xmm10, %xmm11, %xmm9                         #1467.21
        vpslld    $2, %xmm4, %xmm5                              #1557.367
        vmovd     %xmm5, %edx                                   #1557.437
        vmovmskps %xmm9, %eax                                   #1469.44
        vmovups   %xmm1, (%rsp)                                 #[spill]
        vmovd     (%rdx,%r8), %xmm12                            #1557.1236
        vpextrd   $1, %xmm5, %ecx                               #1557.501
        vpextrd   $2, %xmm5, %esi                               #1557.568
        vpextrd   $3, %xmm5, %edi                               #1557.635
        vmovd     (%rcx,%r8), %xmm11                            #1557.1319
        vmovd     (%rsi,%r8), %xmm10                            #1557.1423
        vmovd     (%rdi,%r8), %xmm9                             #1557.1506
        vpunpcklqdq %xmm11, %xmm12, %xmm6                       #1557.1216
        vpunpcklqdq %xmm9, %xmm10, %xmm13                       #1557.1403
        vmovd     4(%rdi,%r8), %xmm3                            #1558.1507
        vmovd     8(%rdi,%r8), %xmm10                           #1559.1506
        vmovd     4(%rdx,%r8), %xmm7                            #1558.1237
        vmovd     8(%rdx,%r8), %xmm1                            #1559.1236
        vmovd     4(%rcx,%r8), %xmm8                            #1558.1320
        vmovd     8(%rcx,%r8), %xmm2                            #1559.1319
        lea       __svml_scos_ha_data_internal(%rip), %rcx      #1676.509
        vmovd     4(%rsi,%r8), %xmm15                           #1558.1424
        vmovd     8(%rsi,%r8), %xmm12                           #1559.1423
        vshufps   $136, %xmm13, %xmm6, %xmm11                   #1557.1196
        vpand     .L_2il0floatpacket.30(%rip), %xmm14, %xmm13   #1565.21
        vpunpcklqdq %xmm8, %xmm7, %xmm4                         #1558.1217
        vpunpcklqdq %xmm3, %xmm15, %xmm5                        #1558.1404
        vpunpcklqdq %xmm2, %xmm1, %xmm9                         #1559.1216
        vpunpcklqdq %xmm10, %xmm12, %xmm6                       #1559.1403
        vpaddd    .L_2il0floatpacket.31(%rip), %xmm13, %xmm15   #1566.21
        vshufps   $136, %xmm5, %xmm4, %xmm7                     #1558.1197
        vpsrld    $16, %xmm15, %xmm10                           #1574.12
        vshufps   $136, %xmm6, %xmm9, %xmm12                    #1559.1196
        vpsrld    $16, %xmm7, %xmm5                             #1570.12
        vmovdqu   .L_2il0floatpacket.32(%rip), %xmm13           #1567.17
        vpsrld    $16, %xmm12, %xmm1                            #1572.12
        vpand     %xmm13, %xmm7, %xmm3                          #1571.12
        vpand     %xmm13, %xmm15, %xmm7                         #1575.12
        vpand     %xmm13, %xmm12, %xmm12                        #1573.12
        vpand     %xmm13, %xmm11, %xmm8                         #1569.12
        vpmulld   %xmm12, %xmm10, %xmm6                         #1581.13
        vpsrld    $16, %xmm11, %xmm11                           #1568.12
        vpmulld   %xmm1, %xmm10, %xmm12                         #1580.13
        vpsrld    $16, %xmm6, %xmm15                            #1606.13
        vpmulld   %xmm1, %xmm7, %xmm1                           #1586.13
        vpmulld   %xmm3, %xmm7, %xmm2                           #1585.13
        vpsrld    $16, %xmm1, %xmm1                             #1588.13
        vpand     %xmm13, %xmm2, %xmm6                          #1596.13
        vpaddd    %xmm1, %xmm12, %xmm12                         #1592.13
        vpmulld   %xmm5, %xmm7, %xmm4                           #1584.13
        vpaddd    %xmm12, %xmm6, %xmm6                          #1601.13
        vpmulld   %xmm3, %xmm10, %xmm3                          #1579.13
        vpsrld    $16, %xmm2, %xmm2                             #1589.13
        vpaddd    %xmm6, %xmm15, %xmm12                         #1607.13
        vpand     %xmm13, %xmm4, %xmm6                          #1597.13
        vpaddd    %xmm2, %xmm3, %xmm3                           #1593.13
        vpsrld    $16, %xmm12, %xmm15                           #1608.13
        vpmulld   %xmm8, %xmm7, %xmm9                           #1583.13
        vpaddd    %xmm3, %xmm6, %xmm6                           #1602.13
        vpmulld   %xmm5, %xmm10, %xmm5                          #1578.13
        vpsrld    $16, %xmm4, %xmm4                             #1590.13
        vpmulld   %xmm11, %xmm7, %xmm11                         #1582.13
        vpaddd    %xmm6, %xmm15, %xmm6                          #1609.13
        vpand     %xmm13, %xmm9, %xmm1                          #1598.13
        vpaddd    %xmm4, %xmm5, %xmm15                          #1594.13
        vpmulld   %xmm8, %xmm10, %xmm8                          #1577.13
        vpsrld    $16, %xmm6, %xmm2                             #1610.13
        vpaddd    %xmm15, %xmm1, %xmm3                          #1603.13
        vpand     %xmm13, %xmm11, %xmm7                         #1599.13
        vpsrld    $16, %xmm9, %xmm11                            #1591.13
        vpaddd    %xmm3, %xmm2, %xmm3                           #1611.13
        vpaddd    %xmm11, %xmm8, %xmm10                         #1616.18
        vpsrld    $16, %xmm3, %xmm15                            #1612.13
        vpaddd    %xmm10, %xmm7, %xmm9                          #1616.18
        vpand     %xmm13, %xmm3, %xmm3                          #1615.13
        vpaddd    %xmm9, %xmm15, %xmm8                          #1616.18
        vpslld    $16, %xmm6, %xmm6                             #1617.19
        vpslld    $16, %xmm8, %xmm15                            #1616.18
        vpand     %xmm13, %xmm12, %xmm12                        #1614.13
        vpand     .L_2il0floatpacket.33(%rip), %xmm14, %xmm2    #1621.17
        vpaddd    %xmm3, %xmm15, %xmm1                          #1618.18
        vpxor     .L_2il0floatpacket.34(%rip), %xmm2, %xmm4     #1623.22
        vpsrld    $9, %xmm1, %xmm13                             #1624.20
        vpaddd    %xmm12, %xmm6, %xmm5                          #1619.19
        vpor      %xmm4, %xmm13, %xmm11                         #1625.20
        vmovups   .L_2il0floatpacket.35(%rip), %xmm12           #1627.15
        vpand     .L_2il0floatpacket.39(%rip), %xmm1, %xmm1     #1646.20
        vaddps    %xmm12, %xmm11, %xmm3                         #1628.19
        vpxor     .L_2il0floatpacket.36(%rip), %xmm2, %xmm7     #1635.22
        vsubps    %xmm12, %xmm3, %xmm10                         #1629.9
        vpand     .L_2il0floatpacket.37(%rip), %xmm5, %xmm9     #1637.20
        vpsrld    $18, %xmm5, %xmm5                             #1648.19
        vpslld    $5, %xmm9, %xmm6                              #1638.20
        vsubps    %xmm10, %xmm11, %xmm4                         #1630.17
        vpxor     .L_2il0floatpacket.38(%rip), %xmm2, %xmm10    #1644.22
        vpslld    $14, %xmm1, %xmm2                             #1647.20
        vpor      %xmm5, %xmm2, %xmm12                          #1649.20
        vpor      %xmm7, %xmm6, %xmm13                          #1639.20
        vpor      %xmm10, %xmm12, %xmm11                        #1650.20
        vsubps    %xmm7, %xmm13, %xmm15                         #1642.17
        vsubps    %xmm10, %xmm11, %xmm9                         #1653.18
        vmovups   .L_2il0floatpacket.40(%rip), %xmm8            #1658.20
        vaddps    %xmm9, %xmm4, %xmm1                           #1654.11
        vmulps    %xmm1, %xmm8, %xmm5                           #1660.17
        vsubps    %xmm1, %xmm4, %xmm6                           #1655.17
        vmovaps   %xmm8, %xmm7                                  #1661.17
        vaddps    %xmm6, %xmm9, %xmm13                          #1656.18
        vfmsub213ps %xmm5, %xmm1, %xmm7                         #1661.17
        vmovups   .L_2il0floatpacket.43(%rip), %xmm2            #1665.16
        vaddps    %xmm13, %xmm15, %xmm9                         #1657.11
        vfmadd132ps .L_2il0floatpacket.41(%rip), %xmm7, %xmm1   #1662.17
        vandps    .L_2il0floatpacket.42(%rip), %xmm14, %xmm15   #1666.11
        vcmpltps  %xmm15, %xmm2, %xmm10                         #1667.17
        vcmpleps  %xmm2, %xmm15, %xmm4                          #1668.20
        vfmadd213ps %xmm1, %xmm8, %xmm9                         #1663.17
        vpand     .L_2il0floatpacket.44(%rip), %xmm3, %xmm3     #1633.15
        vandps    %xmm14, %xmm4, %xmm12                         #1669.24
        vandps    %xmm5, %xmm10, %xmm11                         #1670.25
        vandps    %xmm9, %xmm10, %xmm10                         #1672.9
        vorps     %xmm11, %xmm12, %xmm11                        #1671.9
        vpslld    $4, %xmm3, %xmm12                             #1676.179
        vmovd     %xmm12, %r9d                                  #1676.238
        vmulps    %xmm11, %xmm11, %xmm7                         #1675.15
        vpextrd   $1, %xmm12, %r10d                             #1676.302
        vpextrd   $2, %xmm12, %r11d                             #1676.369
        vpextrd   $3, %xmm12, %edx                              #1676.436
        vmovd     (%r9,%rcx), %xmm9                             #1676.976
        vmovd     4(%r9,%rcx), %xmm2                            #1677.976
        vmovd     (%r10,%rcx), %xmm6                            #1676.1059
        vmovd     4(%r10,%rcx), %xmm3                           #1677.1059
        vmovd     (%r11,%rcx), %xmm13                           #1676.1163
        vmovd     4(%r11,%rcx), %xmm4                           #1677.1163
        vmovd     (%rdx,%rcx), %xmm8                            #1676.1246
        vmovd     4(%rdx,%rcx), %xmm5                           #1677.1246
        vpunpcklqdq %xmm6, %xmm9, %xmm15                        #1676.956
        vpunpcklqdq %xmm8, %xmm13, %xmm1                        #1676.1143
        vpunpcklqdq %xmm3, %xmm2, %xmm12                        #1677.956
        vpunpcklqdq %xmm5, %xmm4, %xmm9                         #1677.1143
        vmovd     8(%r9,%rcx), %xmm6                            #1678.976
        vmovd     8(%r10,%rcx), %xmm13                          #1678.1059
        vmovd     12(%r10,%rcx), %xmm5                          #1679.1061
        vshufps   $136, %xmm1, %xmm15, %xmm8                    #1676.936
        vshufps   $136, %xmm9, %xmm12, %xmm9                    #1677.936
        vpunpcklqdq %xmm13, %xmm6, %xmm2                        #1678.956
        vmovd     8(%r11,%rcx), %xmm15                          #1678.1163
        vmovd     12(%r11,%rcx), %xmm12                         #1679.1165
        vmovd     8(%rdx,%rcx), %xmm1                           #1678.1246
        vmovd     12(%rdx,%rcx), %xmm13                         #1679.1248
        vmovd     12(%r9,%rcx), %xmm4                           #1679.978
        vpunpcklqdq %xmm1, %xmm15, %xmm3                        #1678.1143
        vpunpcklqdq %xmm5, %xmm4, %xmm15                        #1679.958
        vpunpcklqdq %xmm13, %xmm12, %xmm1                       #1679.1145
        vshufps   $136, %xmm3, %xmm2, %xmm6                     #1678.936
        vmovaps   %xmm11, %xmm2                                 #1680.22
        vshufps   $136, %xmm1, %xmm15, %xmm15                   #1679.938
        vfmadd213ps %xmm9, %xmm15, %xmm2                        #1680.22
        vmovups   4352+__svml_scos_ha_data_internal(%rip), %xmm1 #1689.14
        vfmadd213ps 4288+__svml_scos_ha_data_internal(%rip), %xmm7, %xmm1 #1689.14
        vmovaps   %xmm2, %xmm12                                 #1681.21
        vsubps    %xmm2, %xmm9, %xmm3                           #1682.18
        vfmadd231ps %xmm8, %xmm11, %xmm12                       #1681.21
        vmulps    %xmm1, %xmm7, %xmm4                           #1690.14
        vfmadd231ps %xmm11, %xmm15, %xmm3                       #1683.18
        vaddps    %xmm15, %xmm8, %xmm15                         #1692.13
        vsubps    %xmm12, %xmm2, %xmm13                         #1684.18
        vmulps    %xmm4, %xmm11, %xmm2                          #1691.14
        vfmadd231ps %xmm8, %xmm11, %xmm13                       #1685.18
        vmovups   4480+__svml_scos_ha_data_internal(%rip), %xmm8 #1695.14
        vmovups   (%rsp), %xmm1                                 #1703.14[spill]
        vfmadd213ps 4416+__svml_scos_ha_data_internal(%rip), %xmm7, %xmm8 #1695.14
        vfnmadd213ps %xmm15, %xmm9, %xmm11                      #1697.13
        vaddps    %xmm13, %xmm3, %xmm13                         #1686.22
        vmulps    %xmm8, %xmm7, %xmm7                           #1696.14
        vfmadd213ps %xmm6, %xmm11, %xmm10                       #1698.16
        vfmadd213ps %xmm13, %xmm11, %xmm2                       #1700.18
        vfmadd213ps %xmm10, %xmm9, %xmm7                        #1699.17
        vaddps    %xmm2, %xmm7, %xmm10                          #1701.17
        vaddps    %xmm10, %xmm12, %xmm11                        #1702.20
        blendvps  %xmm0, %xmm11, %xmm1                          #1703.14
        jmp       ..B3.2        # Prob 100%                     #1703.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm14
	.cfi_endproc
# mark_end;
	.type	__svml_cosf4_ha_l9,@function
	.size	__svml_cosf4_ha_l9,.-__svml_cosf4_ha_l9
..LN__svml_cosf4_ha_l9.2:
	.data
# -- End  __svml_cosf4_ha_l9
	.text
.L_2__routine_start___svml_cosf4_ha_e9_3:
# -- Begin  __svml_cosf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosf4_ha_e9
# --- __svml_cosf4_ha_e9(__m128)
__svml_cosf4_ha_e9:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1747.1
        .byte     15                                            #2089.14
        .byte     30                                            #2089.14
        .byte     250                                           #2089.14
	.cfi_startproc
..___tag_value___svml_cosf4_ha_e9.115:
..L116:
                                                        #1747.1
        pushq     %rbp                                          #1747.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1747.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1747.1
        subq      $192, %rsp                                    #1747.1
        vmovaps   %xmm0, %xmm14                                 #1747.1
        vandps    4096+__svml_scos_ha_data_internal(%rip), %xmm14, %xmm2 #1766.10
        xorl      %eax, %eax                                    #1763.1
        vmovups   5440+__svml_scos_ha_data_internal(%rip), %xmm6 #1788.54
        vcvtps2pd %xmm2, %ymm9                                  #1796.23
        vcmpnleps 4160+__svml_scos_ha_data_internal(%rip), %xmm2, %xmm0 #1769.26
        vaddps    5504+__svml_scos_ha_data_internal(%rip), %xmm2, %xmm3 #1767.9
        vmovmskps %xmm0, %edx                                   #1815.74
        vmulps    5376+__svml_scos_ha_data_internal(%rip), %xmm3, %xmm4 #1790.29
        vaddps    %xmm4, %xmm6, %xmm5                           #1790.17
        vpslld    $31, %xmm5, %xmm1                             #1791.38
        vsubps    %xmm6, %xmm5, %xmm7                           #1792.17
        vsubps    5568+__svml_scos_ha_data_internal(%rip), %xmm7, %xmm8 #1794.17
        vcvtps2pd %xmm8, %ymm11                                 #1795.23
        vmulpd    4992+__svml_scos_ha_data_internal(%rip), %ymm11, %ymm10 #1798.39
        vmulpd    5056+__svml_scos_ha_data_internal(%rip), %ymm11, %ymm13 #1800.39
        vsubpd    %ymm10, %ymm9, %ymm12                         #1798.17
        vsubpd    %ymm13, %ymm12, %ymm9                         #1800.17
        vmulpd    %ymm9, %ymm9, %ymm6                           #1801.18
        vmulpd    5312+__svml_scos_ha_data_internal(%rip), %ymm6, %ymm15 #1804.32
        vaddpd    5248+__svml_scos_ha_data_internal(%rip), %ymm15, %ymm15 #1804.17
        vmulpd    %ymm15, %ymm6, %ymm3                          #1806.32
        vaddpd    5184+__svml_scos_ha_data_internal(%rip), %ymm3, %ymm4 #1806.17
        vmulpd    %ymm4, %ymm6, %ymm5                           #1808.32
        vaddpd    5120+__svml_scos_ha_data_internal(%rip), %ymm5, %ymm7 #1808.17
        vmulpd    %ymm7, %ymm6, %ymm8                           #1809.17
        vmulpd    %ymm8, %ymm9, %ymm10                          #1810.34
        vaddpd    %ymm10, %ymm9, %ymm11                         #1810.19
        vcvtpd2ps %ymm11, %xmm12                                #1811.18
        vxorps    %xmm1, %xmm12, %xmm1                          #1812.18
        testl     %edx, %edx                                    #1816.62
        jne       ..B4.12       # Prob 5%                       #1816.62
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1 xmm2 xmm14
..B4.2:                         # Preds ..B4.12 ..B4.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #2092.52
        jne       ..B4.4        # Prob 5%                       #2092.52
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm14
..B4.15:                        # Preds ..B4.4 ..B4.2
                                # Execution count [9.50e-01]
        vzeroupper                                              #
                                # LOE rbx r12 r13 r14 r15 xmm1
..B4.3:                         # Preds ..B4.10 ..B4.15
                                # Execution count [1.00e+00]
        movaps    %xmm1, %xmm0                                  #2095.12
        movq      %rbp, %rsp                                    #2095.12
        popq      %rbp                                          #2095.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2095.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.4:                         # Preds ..B4.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm14, 64(%rsp)                              #2092.193
        vmovups   %xmm1, 128(%rsp)                              #2092.264
        je        ..B4.15       # Prob 95%                      #2092.368
                                # LOE rbx r12 r13 r14 r15 eax xmm1
..B4.7:                         # Preds ..B4.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #2092.448
                                # LOE rbx r12 r13 r14 r15 eax edx
..B4.17:                        # Preds ..B4.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.9 ..B4.17
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #2092.511
        jc        ..B4.11       # Prob 5%                       #2092.511
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.11 ..B4.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #2092.464
        cmpl      $4, %r12d                                     #2092.459
        jl        ..B4.8        # Prob 82%                      #2092.459
                                # LOE rbx r12 r14 r15 r13d
..B4.10:                        # Preds ..B4.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm1                              #2092.665
        jmp       ..B4.3        # Prob 100%                     #2092.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm1
..B4.11:                        # Preds ..B4.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #2092.540
        lea       128(%rsp,%r12,4), %rsi                        #2092.540
..___tag_value___svml_cosf4_ha_e9.133:
#       __svml_scos_ha_cout_rare_internal(const float *, float *)
        call      __svml_scos_ha_cout_rare_internal             #2092.540
..___tag_value___svml_cosf4_ha_e9.134:
        jmp       ..B4.9        # Prob 100%                     #2092.540
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B4.12:                        # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        movl      $2139095040, %edx                             #1927.24
        lea       __svml_scos_ha_reduction_data_internal(%rip), %r9 #1930.752
        vmovups   4224+__svml_scos_ha_data_internal(%rip), %xmm8 #1838.50
        movl      $8388607, %r10d                               #1936.27
        vandps    %xmm2, %xmm8, %xmm13                          #1839.14
        movl      $8388608, %r11d                               #1937.26
        vcmpeqps  %xmm8, %xmm13, %xmm7                          #1840.21
        vmovd     %edx, %xmm11                                  #1927.24
        movl      $65535, %edx                                  #1940.23
        vpshufd   $0, %xmm11, %xmm10                            #1927.24
        vpand     %xmm14, %xmm10, %xmm5                         #1928.22
        vpsrld    $23, %xmm5, %xmm13                            #1929.22
        vpslld    $1, %xmm13, %xmm6                             #1930.238
        vpaddd    %xmm13, %xmm6, %xmm15                         #1930.391
        vpslld    $2, %xmm15, %xmm8                             #1930.391
        vmovd     %xmm8, %ecx                                   #1930.465
        vmovups   %xmm1, (%rsp)                                 #[spill]
        vmovmskps %xmm7, %eax                                   #1842.44
        vmovd     4(%rcx,%r9), %xmm1                            #1931.1281
        vmovd     (%rcx,%r9), %xmm9                             #1930.1280
        vpextrd   $1, %xmm8, %esi                               #1930.533
        vpextrd   $2, %xmm8, %edi                               #1930.604
        vpextrd   $3, %xmm8, %r8d                               #1930.675
        vmovd     4(%rsi,%r9), %xmm5                            #1931.1364
        vmovd     (%rsi,%r9), %xmm7                             #1930.1363
        vmovd     (%rdi,%r9), %xmm4                             #1930.1467
        vmovd     4(%rdi,%r9), %xmm2                            #1931.1468
        vmovd     (%r8,%r9), %xmm11                             #1930.1550
        vmovd     4(%r8,%r9), %xmm3                             #1931.1551
        vpunpcklqdq %xmm5, %xmm1, %xmm6                         #1931.1261
        vmovd     %r10d, %xmm1                                  #1936.27
        vpunpcklqdq %xmm7, %xmm9, %xmm10                        #1930.1260
        movl      $511, %r10d                                   #2018.26
        vpunpcklqdq %xmm11, %xmm4, %xmm12                       #1930.1447
        vmovd     8(%r8,%r9), %xmm4                             #1932.1550
        movl      $262143, %r8d                                 #2009.26
        vpunpcklqdq %xmm3, %xmm2, %xmm13                        #1931.1448
        vmovd     %r11d, %xmm2                                  #1937.26
        vmovd     8(%rcx,%r9), %xmm15                           #1932.1280
        movl      $-2147483648, %ecx                            #1993.23
        vmovd     8(%rsi,%r9), %xmm8                            #1932.1363
        movl      $1065353216, %esi                             #1995.28
        vmovd     8(%rdi,%r9), %xmm7                            #1932.1467
        movl      $679477248, %edi                              #2007.28
        vpshufd   $0, %xmm1, %xmm5                              #1936.27
        movl      $872415232, %r9d                              #2016.28
        vshufps   $136, %xmm12, %xmm10, %xmm11                  #1930.1240
        vpand     %xmm14, %xmm5, %xmm3                          #1938.25
        vshufps   $136, %xmm13, %xmm6, %xmm9                    #1931.1241
        vmovd     %edx, %xmm13                                  #1940.23
        vpunpcklqdq %xmm8, %xmm15, %xmm10                       #1932.1260
        vpsrld    $16, %xmm9, %xmm5                             #1943.16
        vpunpcklqdq %xmm4, %xmm7, %xmm12                        #1932.1447
        movl      $255, %r11d                                   #2005.21
        vpshufd   $0, %xmm2, %xmm6                              #1937.26
        vshufps   $136, %xmm12, %xmm10, %xmm12                  #1932.1240
        vpaddd    %xmm6, %xmm3, %xmm15                          #1939.25
        vpshufd   $0, %xmm13, %xmm13                            #1940.23
        vpsrld    $16, %xmm12, %xmm1                            #1945.16
        vpsrld    $16, %xmm15, %xmm10                           #1947.16
        vpand     %xmm13, %xmm15, %xmm7                         #1948.16
        vpand     %xmm13, %xmm12, %xmm12                        #1946.16
        vpand     %xmm13, %xmm9, %xmm3                          #1944.16
        vpmulld   %xmm12, %xmm10, %xmm6                         #1954.17
        vpand     %xmm13, %xmm11, %xmm8                         #1942.16
        vpmulld   %xmm1, %xmm10, %xmm12                         #1953.17
        vpsrld    $16, %xmm6, %xmm15                            #1979.17
        vpmulld   %xmm1, %xmm7, %xmm1                           #1959.17
        vpsrld    $16, %xmm11, %xmm11                           #1941.16
        vpmulld   %xmm3, %xmm7, %xmm2                           #1958.17
        vpsrld    $16, %xmm1, %xmm1                             #1961.17
        vpand     %xmm13, %xmm2, %xmm6                          #1969.17
        vpaddd    %xmm1, %xmm12, %xmm12                         #1965.17
        vpmulld   %xmm5, %xmm7, %xmm4                           #1957.17
        vpaddd    %xmm12, %xmm6, %xmm6                          #1974.17
        vpmulld   %xmm3, %xmm10, %xmm3                          #1952.17
        vpsrld    $16, %xmm2, %xmm2                             #1962.17
        vpaddd    %xmm6, %xmm15, %xmm12                         #1980.17
        vpand     %xmm13, %xmm4, %xmm6                          #1970.17
        vpaddd    %xmm2, %xmm3, %xmm1                           #1966.17
        vpsrld    $16, %xmm12, %xmm15                           #1981.17
        vpmulld   %xmm8, %xmm7, %xmm9                           #1956.17
        vpaddd    %xmm1, %xmm6, %xmm6                           #1975.17
        vpmulld   %xmm5, %xmm10, %xmm5                          #1951.17
        vpsrld    $16, %xmm4, %xmm4                             #1963.17
        vpaddd    %xmm6, %xmm15, %xmm6                          #1982.17
        vpand     %xmm13, %xmm9, %xmm2                          #1971.17
        vpaddd    %xmm4, %xmm5, %xmm15                          #1967.17
        vpsrld    $16, %xmm6, %xmm3                             #1983.17
        vpmulld   %xmm8, %xmm10, %xmm8                          #1950.17
        vpaddd    %xmm15, %xmm2, %xmm4                          #1976.17
        vpmulld   %xmm11, %xmm7, %xmm15                         #1955.17
        vpsrld    $16, %xmm9, %xmm9                             #1964.17
        vpaddd    %xmm4, %xmm3, %xmm1                           #1984.17
        vpand     %xmm13, %xmm15, %xmm7                         #1972.17
        vpaddd    %xmm9, %xmm8, %xmm15                          #1989.22
        vpsrld    $16, %xmm1, %xmm4                             #1985.17
        vpaddd    %xmm15, %xmm7, %xmm8                          #1989.22
        vmovd     %ecx, %xmm10                                  #1993.23
        vpaddd    %xmm8, %xmm4, %xmm9                           #1989.22
        vpand     %xmm13, %xmm1, %xmm11                         #1988.17
        vpslld    $16, %xmm9, %xmm4                             #1989.22
        vpand     %xmm13, %xmm12, %xmm13                        #1987.17
        vpshufd   $0, %xmm10, %xmm12                            #1993.23
        vmovd     %esi, %xmm1                                   #1995.28
        vpshufd   $0, %xmm1, %xmm5                              #1995.28
        vpaddd    %xmm11, %xmm4, %xmm15                         #1991.22
        vpslld    $16, %xmm6, %xmm6                             #1990.23
        vpand     %xmm14, %xmm12, %xmm3                         #1994.21
        vpaddd    %xmm13, %xmm6, %xmm6                          #1992.23
        vpsrld    $9, %xmm15, %xmm2                             #1997.24
        vpxor     %xmm5, %xmm3, %xmm13                          #1996.26
        vmovd     %r8d, %xmm10                                  #2009.26
        vmovups   .L_2il0floatpacket.35(%rip), %xmm8            #2000.15
        vpor      %xmm13, %xmm2, %xmm9                          #1998.24
        vpshufd   $0, %xmm10, %xmm12                            #2009.26
        vmovd     %edi, %xmm4                                   #2007.28
        vpshufd   $0, %xmm4, %xmm11                             #2007.28
        vpand     %xmm6, %xmm12, %xmm5                          #2010.24
        vpslld    $5, %xmm5, %xmm13                             #2011.24
        vpsrld    $18, %xmm6, %xmm6                             #2021.23
        lea       __svml_scos_ha_data_internal(%rip), %r8       #2060.533
        vaddps    %xmm8, %xmm9, %xmm1                           #2001.19
        vsubps    %xmm8, %xmm1, %xmm7                           #2002.9
        vsubps    %xmm7, %xmm9, %xmm2                           #2003.17
        vmovd     %r9d, %xmm7                                   #2016.28
        vpshufd   $0, %xmm7, %xmm4                              #2016.28
        vpxor     %xmm11, %xmm3, %xmm9                          #2008.26
        vpxor     %xmm4, %xmm3, %xmm4                           #2017.26
        vmovd     %r10d, %xmm3                                  #2018.26
        vpor      %xmm9, %xmm13, %xmm8                          #2012.24
        vpshufd   $0, %xmm3, %xmm13                             #2018.26
        vsubps    %xmm9, %xmm8, %xmm5                           #2015.17
        vpand     %xmm15, %xmm13, %xmm15                        #2019.24
        vpslld    $14, %xmm15, %xmm8                            #2020.24
        vpor      %xmm6, %xmm8, %xmm9                           #2022.24
        vpor      %xmm4, %xmm9, %xmm7                           #2023.24
        vmovups   .L_2il0floatpacket.46(%rip), %xmm13           #2033.21
        vsubps    %xmm4, %xmm7, %xmm11                          #2026.18
        vaddps    %xmm11, %xmm2, %xmm3                          #2027.11
        vsubps    %xmm3, %xmm2, %xmm10                          #2028.17
        vmovups   .L_2il0floatpacket.45(%rip), %xmm2            #2032.20
        vaddps    %xmm10, %xmm11, %xmm12                        #2029.18
        vaddps    %xmm12, %xmm5, %xmm6                          #2030.11
        vandps    .L_2il0floatpacket.47(%rip), %xmm3, %xmm5     #2035.20
        vmulps    %xmm5, %xmm13, %xmm7                          #2039.16
        vsubps    %xmm5, %xmm3, %xmm8                           #2036.21
        vmulps    %xmm5, %xmm2, %xmm15                          #2037.20
        vmulps    .L_2il0floatpacket.40(%rip), %xmm6, %xmm4     #2040.16
        vmulps    %xmm8, %xmm2, %xmm9                           #2038.16
        vmulps    %xmm8, %xmm13, %xmm11                         #2041.16
        vaddps    %xmm7, %xmm9, %xmm10                          #2042.17
        vaddps    %xmm11, %xmm4, %xmm12                         #2043.17
        vmovups   .L_2il0floatpacket.43(%rip), %xmm5            #2049.16
        vaddps    %xmm12, %xmm10, %xmm2                         #2044.17
        vandps    .L_2il0floatpacket.42(%rip), %xmm14, %xmm3    #2050.11
        vmovd     %r11d, %xmm10                                 #2005.21
        vcmpleps  %xmm5, %xmm3, %xmm6                           #2052.20
        vcmpltps  %xmm3, %xmm5, %xmm9                           #2051.17
        vaddps    %xmm2, %xmm15, %xmm13                         #2045.17
        vpshufd   $0, %xmm10, %xmm12                            #2005.21
        vandps    %xmm13, %xmm9, %xmm8                          #2054.25
        vpand     %xmm12, %xmm1, %xmm1                          #2006.19
        vsubps    %xmm13, %xmm15, %xmm15                        #2046.20
        vaddps    %xmm15, %xmm2, %xmm7                          #2047.17
        vandps    %xmm14, %xmm6, %xmm15                         #2053.24
        vpslld    $4, %xmm1, %xmm6                              #2060.183
        vorps     %xmm8, %xmm15, %xmm4                          #2055.9
        vmovd     %xmm6, %edx                                   #2060.246
        vandps    %xmm7, %xmm9, %xmm7                           #2056.9
        vmulps    %xmm4, %xmm4, %xmm11                          #2059.15
        vpextrd   $1, %xmm6, %ecx                               #2060.314
        vpextrd   $2, %xmm6, %esi                               #2060.385
        vpextrd   $3, %xmm6, %edi                               #2060.456
        vmovd     (%rdx,%r8), %xmm13                            #2060.1000
        vmovd     4(%rdx,%r8), %xmm1                            #2061.1000
        vmovd     (%rcx,%r8), %xmm15                            #2060.1083
        vmovd     4(%rcx,%r8), %xmm2                            #2061.1083
        vmovd     (%rsi,%r8), %xmm8                             #2060.1187
        vmovd     4(%rsi,%r8), %xmm3                            #2061.1187
        vmovd     (%rdi,%r8), %xmm9                             #2060.1270
        vmovd     4(%rdi,%r8), %xmm5                            #2061.1270
        vpunpcklqdq %xmm15, %xmm13, %xmm10                      #2060.980
        vpunpcklqdq %xmm9, %xmm8, %xmm12                        #2060.1167
        vpunpcklqdq %xmm2, %xmm1, %xmm6                         #2061.980
        vpunpcklqdq %xmm5, %xmm3, %xmm13                        #2061.1167
        vmovd     8(%rdx,%r8), %xmm15                           #2062.1000
        vmovd     8(%rcx,%r8), %xmm9                            #2062.1083
        vshufps   $136, %xmm12, %xmm10, %xmm10                  #2060.960
        vshufps   $136, %xmm13, %xmm6, %xmm8                    #2061.960
        vmovd     12(%rcx,%r8), %xmm6                           #2063.1085
        vpunpcklqdq %xmm9, %xmm15, %xmm2                        #2062.980
        vmovd     8(%rsi,%r8), %xmm12                           #2062.1187
        vmovd     12(%rsi,%r8), %xmm13                          #2063.1189
        vmovd     8(%rdi,%r8), %xmm1                            #2062.1270
        vmovd     12(%rdi,%r8), %xmm15                          #2063.1272
        vmovd     12(%rdx,%r8), %xmm5                           #2063.1002
        vpunpcklqdq %xmm1, %xmm12, %xmm3                        #2062.1167
        vpunpcklqdq %xmm6, %xmm5, %xmm12                        #2063.982
        vpunpcklqdq %xmm15, %xmm13, %xmm1                       #2063.1169
        vshufps   $136, %xmm1, %xmm12, %xmm12                   #2063.962
        vshufps   $136, %xmm3, %xmm2, %xmm9                     #2062.960
        vmulps    %xmm12, %xmm4, %xmm2                          #2065.22
        vmulps    %xmm10, %xmm4, %xmm13                         #2064.19
        vaddps    %xmm2, %xmm8, %xmm5                           #2066.22
        vsubps    %xmm5, %xmm8, %xmm3                           #2068.18
        vaddps    %xmm5, %xmm13, %xmm6                          #2067.21
        vaddps    %xmm3, %xmm2, %xmm1                           #2069.18
        vmulps    4352+__svml_scos_ha_data_internal(%rip), %xmm11, %xmm3 #2075.26
        vsubps    %xmm6, %xmm5, %xmm15                          #2070.18
        vaddps    4288+__svml_scos_ha_data_internal(%rip), %xmm3, %xmm5 #2075.14
        vaddps    %xmm15, %xmm13, %xmm2                         #2071.18
        vmulps    %xmm5, %xmm11, %xmm15                         #2076.14
        vaddps    %xmm2, %xmm1, %xmm13                          #2072.22
        vaddps    %xmm12, %xmm10, %xmm1                         #2078.13
        vmulps    4480+__svml_scos_ha_data_internal(%rip), %xmm11, %xmm10 #2081.26
        vmulps    %xmm15, %xmm4, %xmm15                         #2077.14
        vmulps    %xmm8, %xmm4, %xmm4                           #2083.32
        vaddps    4416+__svml_scos_ha_data_internal(%rip), %xmm10, %xmm10 #2081.14
        vmulps    %xmm10, %xmm11, %xmm11                        #2082.14
        vsubps    %xmm4, %xmm1, %xmm1                           #2083.13
        vmulps    %xmm1, %xmm7, %xmm7                           #2084.28
        vmulps    %xmm11, %xmm8, %xmm8                          #2085.29
        vmulps    %xmm1, %xmm15, %xmm15                         #2086.30
        vaddps    %xmm7, %xmm9, %xmm7                           #2084.16
        vaddps    %xmm15, %xmm13, %xmm3                         #2086.18
        vaddps    %xmm8, %xmm7, %xmm2                           #2085.17
        vmovups   (%rsp), %xmm1                                 #2089.14[spill]
        vaddps    %xmm3, %xmm2, %xmm4                           #2087.17
        vaddps    %xmm4, %xmm6, %xmm5                           #2088.20
        blendvps  %xmm0, %xmm5, %xmm1                           #2089.14
        jmp       ..B4.2        # Prob 100%                     #2089.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm14
	.cfi_endproc
# mark_end;
	.type	__svml_cosf4_ha_e9,@function
	.size	__svml_cosf4_ha_e9,.-__svml_cosf4_ha_e9
..LN__svml_cosf4_ha_e9.3:
	.data
# -- End  __svml_cosf4_ha_e9
	.text
.L_2__routine_start___svml_cosf8_ha_l9_4:
# -- Begin  __svml_cosf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosf8_ha_l9
# --- __svml_cosf8_ha_l9(__m256)
__svml_cosf8_ha_l9:
# parameter 1: %ymm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2101.1
        .byte     15                                            #2430.14
        .byte     30                                            #2430.14
        .byte     250                                           #2430.14
	.cfi_startproc
..___tag_value___svml_cosf8_ha_l9.140:
..L141:
                                                        #2101.1
        pushq     %rbp                                          #2101.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2101.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2101.1
        pushq     %r12                                          #2101.1
        subq      $184, %rsp                                    #2101.1
        xorl      %eax, %eax                                    #2117.1
        vmovaps   %ymm0, %ymm14                                 #2101.1
        vmovups   5440+__svml_scos_ha_data_internal(%rip), %ymm5 #2142.57
        vmovupd   4992+__svml_scos_ha_data_internal(%rip), %ymm10 #2151.55
        vmovupd   5056+__svml_scos_ha_data_internal(%rip), %ymm12 #2153.55
        vandps    4096+__svml_scos_ha_data_internal(%rip), %ymm14, %ymm2 #2120.10
        vaddps    5504+__svml_scos_ha_data_internal(%rip), %ymm2, %ymm4 #2121.9
        vcmpnle_uqps 4160+__svml_scos_ha_data_internal(%rip), %ymm2, %ymm13 #2123.26
        vfmadd132ps 5376+__svml_scos_ha_data_internal(%rip), %ymm5, %ymm4 #2144.17
        vsubps    %ymm5, %ymm4, %ymm6                           #2146.17
        vpslld    $31, %ymm4, %ymm3                             #2145.41
        vmovupd   5248+__svml_scos_ha_data_internal(%rip), %ymm4 #2157.54
        vmovupd   5312+__svml_scos_ha_data_internal(%rip), %ymm5 #2156.54
        vsubps    5568+__svml_scos_ha_data_internal(%rip), %ymm6, %ymm7 #2148.17
        vmovupd   5184+__svml_scos_ha_data_internal(%rip), %ymm6 #2159.54
        vmovmskps %ymm13, %edx                                  #2169.75
        vextractf128 $1, %ymm7, %xmm8                           #2149.105
        vextractf128 $1, %ymm2, %xmm9                           #2150.106
        vcvtps2pd %xmm7, %ymm1                                  #2149.23
        vcvtps2pd %xmm2, %ymm11                                 #2150.23
        vcvtps2pd %xmm8, %ymm0                                  #2149.88
        vcvtps2pd %xmm9, %ymm15                                 #2150.89
        vmovupd   5120+__svml_scos_ha_data_internal(%rip), %ymm7 #2161.54
        vfnmadd231pd %ymm1, %ymm10, %ymm11                      #2152.19
        vfnmadd231pd %ymm0, %ymm10, %ymm15                      #2152.78
        vfnmadd213pd %ymm11, %ymm12, %ymm1                      #2154.19
        vfnmadd213pd %ymm15, %ymm12, %ymm0                      #2154.78
        vmulpd    %ymm1, %ymm1, %ymm8                           #2155.20
        vmulpd    %ymm0, %ymm0, %ymm10                          #2155.66
        vmovapd   %ymm4, %ymm9                                  #2158.19
        vfmadd231pd %ymm8, %ymm5, %ymm9                         #2158.19
        vfmadd231pd %ymm10, %ymm5, %ymm4                        #2158.78
        vfmadd213pd %ymm6, %ymm8, %ymm9                         #2160.19
        vfmadd213pd %ymm6, %ymm10, %ymm4                        #2160.77
        vfmadd213pd %ymm7, %ymm8, %ymm9                         #2162.19
        vfmadd213pd %ymm7, %ymm10, %ymm4                        #2162.77
        vmulpd    %ymm9, %ymm8, %ymm11                          #2163.19
        vmulpd    %ymm4, %ymm10, %ymm12                         #2163.65
        vfmadd213pd %ymm1, %ymm1, %ymm11                        #2164.21
        vfmadd213pd %ymm0, %ymm0, %ymm12                        #2164.79
        vcvtpd2ps %ymm11, %xmm0                                 #2165.64
        vcvtpd2ps %ymm12, %xmm1                                 #2165.100
        vinsertf128 $1, %xmm1, %ymm0, %ymm15                    #2165.18
        vxorps    %ymm3, %ymm15, %ymm0                          #2166.18
        testl     %edx, %edx                                    #2170.62
        jne       ..B5.12       # Prob 5%                       #2170.62
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r13 r14 r15 eax ymm0 ymm2 ymm13 ymm14
..B5.2:                         # Preds ..B5.12 ..B5.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #2433.52
        jne       ..B5.4        # Prob 5%                       #2433.52
                                # LOE rbx r13 r14 r15 eax ymm0 ymm14
..B5.3:                         # Preds ..B5.4 ..B5.10 ..B5.2
                                # Execution count [1.00e+00]
        addq      $184, %rsp                                    #2436.12
	.cfi_restore 12
        popq      %r12                                          #2436.12
        movq      %rbp, %rsp                                    #2436.12
        popq      %rbp                                          #2436.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2436.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE
..B5.4:                         # Preds ..B5.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm14, 64(%rsp)                              #2433.196
        vmovups   %ymm0, 128(%rsp)                              #2433.270
        je        ..B5.3        # Prob 95%                      #2433.374
                                # LOE rbx r13 r14 r15 eax ymm0
..B5.7:                         # Preds ..B5.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %r12d, %r12d                                  #2433.454
                                # LOE rbx r12 r13 r14 r15 eax
..B5.15:                        # Preds ..B5.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.9 ..B5.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #2433.517
        jc        ..B5.11       # Prob 5%                       #2433.517
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.11 ..B5.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #2433.470
        cmpl      $8, %r12d                                     #2433.465
        jl        ..B5.8        # Prob 82%                      #2433.465
                                # LOE rbx r12 r14 r15 r13d
..B5.10:                        # Preds ..B5.9
                                # Execution count [2.25e-03]: Infreq
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #2433.674
        jmp       ..B5.3        # Prob 100%                     #2433.674
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r13 r14 r15 ymm0
..B5.11:                        # Preds ..B5.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #2433.546
        lea       128(%rsp,%r12,4), %rsi                        #2433.546
..___tag_value___svml_cosf8_ha_l9.156:
#       __svml_scos_ha_cout_rare_internal(const float *, float *)
        call      __svml_scos_ha_cout_rare_internal             #2433.546
..___tag_value___svml_cosf8_ha_l9.157:
        jmp       ..B5.9        # Prob 100%                     #2433.546
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B5.12:                        # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vpand     .L_2il0floatpacket.48(%rip), %ymm14, %ymm9    #2282.18
        lea       __svml_scos_ha_reduction_data_internal(%rip), %rcx #2284.1212
        vmovups   4224+__svml_scos_ha_data_internal(%rip), %ymm8 #2192.53
        vmovups   %ymm0, (%rsp)                                 #[spill]
        vpsrld    $23, %ymm9, %ymm3                             #2283.18
        vpslld    $1, %ymm3, %ymm7                              #2284.234
        vpaddd    %ymm3, %ymm7, %ymm4                           #2284.296
        vpslld    $2, %ymm4, %ymm11                             #2284.373
        vandps    %ymm2, %ymm8, %ymm10                          #2193.14
        vcmpeqps  %ymm8, %ymm10, %ymm12                         #2194.21
        vmovmskps %ymm12, %eax                                  #2196.45
        vextractf128 $1, %ymm11, %xmm15                         #2284.834
        vmovd     %xmm11, %esi                                  #2284.446
        vmovd     %xmm15, %r11d                                 #2284.815
        vmovd     (%rsi,%rcx), %xmm10                           #2284.2300
        vpextrd   $1, %xmm11, %edi                              #2284.536
        vpextrd   $2, %xmm11, %r8d                              #2284.629
        vpextrd   $3, %xmm11, %r9d                              #2284.722
        vpextrd   $1, %xmm15, %r10d                             #2284.910
        vpextrd   $2, %xmm15, %r12d                             #2284.1008
        vpextrd   $3, %xmm15, %edx                              #2284.1106
        vmovd     (%rdi,%rcx), %xmm6                            #2284.2389
        vmovd     (%r8,%rcx), %xmm8                             #2284.2499
        vmovd     (%r9,%rcx), %xmm5                             #2284.2588
        vmovd     (%r11,%rcx), %xmm2                            #2284.2741
        vmovd     (%r10,%rcx), %xmm3                            #2284.2830
        vmovd     (%r12,%rcx), %xmm4                            #2284.2940
        vmovd     (%rdx,%rcx), %xmm0                            #2284.3029
        vpunpcklqdq %xmm6, %xmm10, %xmm12                       #2284.2280
        vpunpcklqdq %xmm5, %xmm8, %xmm9                         #2284.2479
        vpunpcklqdq %xmm3, %xmm2, %xmm1                         #2284.2721
        vpunpcklqdq %xmm0, %xmm4, %xmm11                        #2284.2920
        vshufps   $136, %xmm9, %xmm12, %xmm7                    #2284.2260
        vmovd     4(%rsi,%rcx), %xmm6                           #2285.2301
        vmovd     4(%rdi,%rcx), %xmm8                           #2285.2390
        vmovd     4(%r8,%rcx), %xmm5                            #2285.2500
        vmovd     4(%r9,%rcx), %xmm12                           #2285.2589
        vshufps   $136, %xmm11, %xmm1, %xmm10                   #2284.2701
        vmovd     4(%r11,%rcx), %xmm3                           #2285.2742
        vmovd     4(%r10,%rcx), %xmm4                           #2285.2831
        vmovd     4(%r12,%rcx), %xmm0                           #2285.2941
        vmovd     4(%rdx,%rcx), %xmm1                           #2285.3030
        vpunpcklqdq %xmm8, %xmm6, %xmm9                         #2285.2281
        vpunpcklqdq %xmm12, %xmm5, %xmm15                       #2285.2480
        vpunpcklqdq %xmm4, %xmm3, %xmm11                        #2285.2722
        vpunpcklqdq %xmm1, %xmm0, %xmm6                         #2285.2921
        vshufps   $136, %xmm15, %xmm9, %xmm2                    #2285.2261
        vmovd     8(%r8,%rcx), %xmm9                            #2286.2499
        vshufps   $136, %xmm6, %xmm11, %xmm8                    #2285.2702
        vmovd     8(%rsi,%rcx), %xmm5                           #2286.2300
        vmovd     8(%rdi,%rcx), %xmm12                          #2286.2389
        vmovd     8(%r11,%rcx), %xmm0                           #2286.2741
        lea       __svml_scos_ha_data_internal(%rip), %r11      #2403.1007
        vmovd     8(%r10,%rcx), %xmm1                           #2286.2830
        vmovd     8(%r12,%rcx), %xmm11                          #2286.2940
        vpunpcklqdq %xmm12, %xmm5, %xmm15                       #2286.2280
        vpunpcklqdq %xmm1, %xmm0, %xmm5                         #2286.2721
        vinsertf128 $1, %xmm10, %ymm7, %ymm10                   #2284.2196
        vmovd     8(%r9,%rcx), %xmm7                            #2286.2588
        vpunpcklqdq %xmm7, %xmm9, %xmm3                         #2286.2479
        vpand     .L_2il0floatpacket.49(%rip), %ymm14, %ymm7    #2292.21
        vshufps   $136, %xmm3, %xmm15, %xmm4                    #2286.2260
        vpaddd    .L_2il0floatpacket.50(%rip), %ymm7, %ymm15    #2293.21
        vinsertf128 $1, %xmm8, %ymm2, %ymm6                     #2285.2197
        vmovd     8(%rdx,%rcx), %xmm8                           #2286.3029
        vpunpcklqdq %xmm8, %xmm11, %xmm12                       #2286.2920
        vshufps   $136, %xmm12, %xmm5, %xmm9                    #2286.2701
        vmovdqu   .L_2il0floatpacket.51(%rip), %ymm12           #2294.17
        vpand     %ymm12, %ymm6, %ymm2                          #2298.12
        vpand     %ymm12, %ymm10, %ymm7                         #2296.12
        vpsrld    $16, %ymm10, %ymm10                           #2295.12
        vinsertf128 $1, %xmm9, %ymm4, %ymm11                    #2286.2196
        vpsrld    $16, %ymm6, %ymm4                             #2297.12
        vpsrld    $16, %ymm11, %ymm0                            #2299.12
        vpsrld    $16, %ymm15, %ymm9                            #2301.12
        vpand     %ymm12, %ymm15, %ymm6                         #2302.12
        vpand     %ymm12, %ymm11, %ymm11                        #2300.12
        vpmulld   %ymm11, %ymm9, %ymm5                          #2308.13
        vpmulld   %ymm0, %ymm9, %ymm11                          #2307.13
        vpmulld   %ymm0, %ymm6, %ymm0                           #2313.13
        vpsrld    $16, %ymm0, %ymm0                             #2315.13
        vpsrld    $16, %ymm5, %ymm15                            #2333.13
        vpaddd    %ymm0, %ymm11, %ymm11                         #2319.13
        vpmulld   %ymm2, %ymm6, %ymm1                           #2312.13
        vpand     %ymm12, %ymm1, %ymm5                          #2323.13
        vpsrld    $16, %ymm1, %ymm1                             #2316.13
        vpaddd    %ymm11, %ymm5, %ymm5                          #2328.13
        vpmulld   %ymm4, %ymm6, %ymm3                           #2311.13
        vpmulld   %ymm2, %ymm9, %ymm2                           #2306.13
        vpaddd    %ymm5, %ymm15, %ymm11                         #2334.13
        vpand     %ymm12, %ymm3, %ymm5                          #2324.13
        vpaddd    %ymm1, %ymm2, %ymm2                           #2320.13
        vpsrld    $16, %ymm3, %ymm3                             #2317.13
        vpsrld    $16, %ymm11, %ymm15                           #2335.13
        vpand     %ymm12, %ymm11, %ymm11                        #2341.13
        vpaddd    %ymm2, %ymm5, %ymm5                           #2329.13
        vpmulld   %ymm7, %ymm6, %ymm8                           #2310.13
        vpaddd    %ymm5, %ymm15, %ymm5                          #2336.13
        vpand     %ymm12, %ymm8, %ymm0                          #2325.13
        vpsrld    $16, %ymm8, %ymm8                             #2318.13
        vpsrld    $16, %ymm5, %ymm1                             #2337.13
        vpslld    $16, %ymm5, %ymm5                             #2344.19
        vpmulld   %ymm4, %ymm9, %ymm15                          #2305.13
        vpaddd    %ymm3, %ymm15, %ymm15                         #2321.13
        vpaddd    %ymm15, %ymm0, %ymm2                          #2330.13
        vpmulld   %ymm10, %ymm6, %ymm10                         #2309.13
        vpmulld   %ymm7, %ymm9, %ymm6                           #2304.13
        vpaddd    %ymm2, %ymm1, %ymm2                           #2338.13
        vpand     %ymm12, %ymm10, %ymm3                         #2326.13
        vpaddd    %ymm8, %ymm6, %ymm9                           #2322.13
        vpand     .L_2il0floatpacket.52(%rip), %ymm14, %ymm1    #2348.17
        vpsrld    $16, %ymm2, %ymm15                            #2339.13
        vpand     %ymm12, %ymm2, %ymm2                          #2342.13
        vpaddd    %ymm9, %ymm3, %ymm7                           #2331.13
        vmovups   .L_2il0floatpacket.21(%rip), %ymm10           #2354.15
        vpaddd    %ymm7, %ymm15, %ymm15                         #2340.13
        vpxor     .L_2il0floatpacket.54(%rip), %ymm1, %ymm7     #2362.22
        vpslld    $16, %ymm15, %ymm4                            #2343.18
        vpaddd    %ymm2, %ymm4, %ymm0                           #2345.18
        vpaddd    %ymm11, %ymm5, %ymm4                          #2346.19
        vpxor     .L_2il0floatpacket.53(%rip), %ymm1, %ymm11    #2350.22
        vpsrld    $9, %ymm0, %ymm12                             #2351.20
        vpand     .L_2il0floatpacket.57(%rip), %ymm0, %ymm0     #2373.20
        vpand     .L_2il0floatpacket.55(%rip), %ymm4, %ymm5     #2364.20
        vpor      %ymm11, %ymm12, %ymm6                         #2352.20
        vpsrld    $18, %ymm4, %ymm11                            #2375.19
        vpslld    $5, %ymm5, %ymm12                             #2365.20
        vaddps    %ymm10, %ymm6, %ymm2                          #2355.19
        vpor      %ymm7, %ymm12, %ymm9                          #2366.20
        vsubps    %ymm10, %ymm2, %ymm8                          #2356.9
        vpand     .L_2il0floatpacket.59(%rip), %ymm2, %ymm2     #2360.15
        vsubps    %ymm8, %ymm6, %ymm3                           #2357.17
        vsubps    %ymm7, %ymm9, %ymm15                          #2369.17
        vpxor     .L_2il0floatpacket.56(%rip), %ymm1, %ymm8     #2371.22
        vpslld    $14, %ymm0, %ymm1                             #2374.20
        vpor      %ymm11, %ymm1, %ymm10                         #2376.20
        vpor      %ymm8, %ymm10, %ymm6                          #2377.20
        vsubps    %ymm8, %ymm6, %ymm5                           #2380.18
        vaddps    %ymm5, %ymm3, %ymm0                           #2381.11
        vsubps    %ymm0, %ymm3, %ymm12                          #2382.17
        vmovups   .L_2il0floatpacket.27(%rip), %ymm3            #2392.16
        vaddps    %ymm12, %ymm5, %ymm9                          #2383.18
        vaddps    %ymm9, %ymm15, %ymm5                          #2384.11
        vmovups   .L_2il0floatpacket.22(%rip), %ymm15           #2385.20
        vmulps    %ymm0, %ymm15, %ymm11                         #2387.17
        vmovaps   %ymm15, %ymm7                                 #2388.17
        vfmsub213ps %ymm11, %ymm0, %ymm7                        #2388.17
        vandps    .L_2il0floatpacket.26(%rip), %ymm14, %ymm1    #2393.11
        vcmpgt_oqps %ymm3, %ymm1, %ymm8                         #2394.17
        vcmple_oqps %ymm3, %ymm1, %ymm4                         #2395.20
        vfmadd132ps .L_2il0floatpacket.58(%rip), %ymm7, %ymm0   #2389.17
        vandps    %ymm11, %ymm8, %ymm6                          #2397.25
        vpslld    $4, %ymm2, %ymm11                             #2403.179
        vfmadd213ps %ymm0, %ymm15, %ymm5                        #2390.17
        vandps    %ymm14, %ymm4, %ymm10                         #2396.24
        vorps     %ymm6, %ymm10, %ymm10                         #2398.9
        vandps    %ymm5, %ymm8, %ymm6                           #2399.9
        vmulps    %ymm10, %ymm10, %ymm9                         #2402.15
        vextractf128 $1, %ymm11, %xmm2                          #2403.629
        vmovd     %xmm11, %r10d                                 #2403.241
        vmovd     %xmm2, %esi                                   #2403.610
        vmovd     (%r10,%r11), %xmm8                            #2403.1974
        vpextrd   $1, %xmm11, %r9d                              #2403.331
        vpextrd   $2, %xmm11, %r8d                              #2403.424
        vpextrd   $3, %xmm11, %edi                              #2403.517
        vpextrd   $1, %xmm2, %ecx                               #2403.705
        vpextrd   $2, %xmm2, %edx                               #2403.803
        vpextrd   $3, %xmm2, %r12d                              #2403.901
        vmovd     (%r9,%r11), %xmm5                             #2403.2063
        vmovd     (%r8,%r11), %xmm12                            #2403.2173
        vmovd     (%rdi,%r11), %xmm7                            #2403.2262
        vpunpcklqdq %xmm5, %xmm8, %xmm15                        #2403.1954
        vpunpcklqdq %xmm7, %xmm12, %xmm0                        #2403.2153
        vmovd     (%rsi,%r11), %xmm3                            #2403.2415
        vmovd     (%rcx,%r11), %xmm4                            #2403.2504
        vmovd     (%rdx,%r11), %xmm11                           #2403.2614
        vmovd     (%r12,%r11), %xmm8                            #2403.2703
        vshufps   $136, %xmm0, %xmm15, %xmm1                    #2403.1934
        vpunpcklqdq %xmm4, %xmm3, %xmm5                         #2403.2395
        vpunpcklqdq %xmm8, %xmm11, %xmm12                       #2403.2594
        vmovd     4(%r10,%r11), %xmm15                          #2404.1974
        vmovd     4(%r9,%r11), %xmm0                            #2404.2063
        vmovd     4(%r8,%r11), %xmm2                            #2404.2173
        vmovd     4(%rdi,%r11), %xmm3                           #2404.2262
        vshufps   $136, %xmm12, %xmm5, %xmm7                    #2403.2375
        vpunpcklqdq %xmm0, %xmm15, %xmm4                        #2404.1954
        vmovd     4(%rsi,%r11), %xmm5                           #2404.2415
        vmovd     4(%rcx,%r11), %xmm12                          #2404.2504
        vmovd     4(%rdx,%r11), %xmm15                          #2404.2614
        vmovd     4(%r12,%r11), %xmm0                           #2404.2703
        vpunpcklqdq %xmm3, %xmm2, %xmm11                        #2404.2153
        vpunpcklqdq %xmm0, %xmm15, %xmm2                        #2404.2594
        vshufps   $136, %xmm11, %xmm4, %xmm8                    #2404.1934
        vmovd     8(%r10,%r11), %xmm4                           #2405.1974
        vmovd     8(%r9,%r11), %xmm11                           #2405.2063
        vpunpcklqdq %xmm11, %xmm4, %xmm15                       #2405.1954
        vmovd     8(%rdx,%r11), %xmm4                           #2405.2614
        vmovd     8(%r12,%r11), %xmm11                          #2405.2703
        vinsertf128 $1, %xmm7, %ymm1, %ymm7                     #2403.1870
        vpunpcklqdq %xmm12, %xmm5, %xmm1                        #2404.2395
        vshufps   $136, %xmm2, %xmm1, %xmm3                     #2404.2375
        vmovd     8(%r8,%r11), %xmm5                            #2405.2173
        vmovd     8(%rdi,%r11), %xmm12                          #2405.2262
        vmovd     8(%rsi,%r11), %xmm2                           #2405.2415
        vpunpcklqdq %xmm12, %xmm5, %xmm0                        #2405.2153
        vpunpcklqdq %xmm11, %xmm4, %xmm12                       #2405.2594
        vshufps   $136, %xmm0, %xmm15, %xmm1                    #2405.1934
        vmovd     12(%r10,%r11), %xmm0                          #2406.1976
        vmovd     12(%rdi,%r11), %xmm4                          #2406.2264
        vinsertf128 $1, %xmm3, %ymm8, %ymm8                     #2404.1870
        vmovd     8(%rcx,%r11), %xmm3                           #2405.2504
        vpunpcklqdq %xmm3, %xmm2, %xmm5                         #2405.2395
        vshufps   $136, %xmm12, %xmm5, %xmm15                   #2405.2375
        vmovd     12(%r9,%r11), %xmm2                           #2406.2065
        vmovd     12(%r8,%r11), %xmm3                           #2406.2175
        vpunpcklqdq %xmm2, %xmm0, %xmm11                        #2406.1956
        vpunpcklqdq %xmm4, %xmm3, %xmm12                        #2406.2155
        vmovd     12(%rsi,%r11), %xmm0                          #2406.2417
        vmovd     12(%rdx,%r11), %xmm2                          #2406.2616
        vmovd     12(%r12,%r11), %xmm3                          #2406.2705
        vinsertf128 $1, %xmm15, %ymm1, %ymm5                    #2405.1870
        vmovd     12(%rcx,%r11), %xmm1                          #2406.2506
        vshufps   $136, %xmm12, %xmm11, %xmm15                  #2406.1936
        vpunpcklqdq %xmm1, %xmm0, %xmm4                         #2406.2397
        vpunpcklqdq %xmm3, %xmm2, %xmm11                        #2406.2596
        vshufps   $136, %xmm11, %xmm4, %xmm12                   #2406.2377
        vmovaps   %ymm10, %ymm0                                 #2407.22
        vmovups   4352+__svml_scos_ha_data_internal(%rip), %ymm2 #2416.14
        vmovaps   %ymm7, %ymm11                                 #2408.21
        vfmadd213ps 4288+__svml_scos_ha_data_internal(%rip), %ymm9, %ymm2 #2416.14
        vmulps    %ymm2, %ymm9, %ymm3                           #2417.14
        vmulps    %ymm3, %ymm10, %ymm4                          #2418.14
        vinsertf128 $1, %xmm12, %ymm15, %ymm15                  #2406.1872
        vfmadd213ps %ymm8, %ymm15, %ymm0                        #2407.22
        vfmadd213ps %ymm0, %ymm10, %ymm11                       #2408.21
        vsubps    %ymm0, %ymm8, %ymm12                          #2409.18
        vsubps    %ymm11, %ymm0, %ymm1                          #2411.18
        vfmadd231ps %ymm10, %ymm15, %ymm12                      #2410.18
        vmovups   (%rsp), %ymm0                                 #2430.14[spill]
        vfmadd231ps %ymm7, %ymm10, %ymm1                        #2412.18
        vaddps    %ymm15, %ymm7, %ymm15                         #2419.13
        vmovups   4480+__svml_scos_ha_data_internal(%rip), %ymm7 #2422.14
        vfnmadd213ps %ymm15, %ymm8, %ymm10                      #2424.13
        vaddps    %ymm1, %ymm12, %ymm12                         #2413.22
        vfmadd213ps 4416+__svml_scos_ha_data_internal(%rip), %ymm9, %ymm7 #2422.14
        vfmadd213ps %ymm5, %ymm10, %ymm6                        #2425.16
        vfmadd213ps %ymm12, %ymm10, %ymm4                       #2427.18
        vmulps    %ymm7, %ymm9, %ymm9                           #2423.14
        vfmadd213ps %ymm6, %ymm8, %ymm9                         #2426.17
        vaddps    %ymm4, %ymm9, %ymm6                           #2428.17
        vaddps    %ymm6, %ymm11, %ymm10                         #2429.20
        vblendvps %ymm13, %ymm10, %ymm0, %ymm0                  #2430.14
        jmp       ..B5.2        # Prob 100%                     #2430.14
        .align    16,0x90
                                # LOE rbx r13 r14 r15 eax ymm0 ymm14
	.cfi_endproc
# mark_end;
	.type	__svml_cosf8_ha_l9,@function
	.size	__svml_cosf8_ha_l9,.-__svml_cosf8_ha_l9
..LN__svml_cosf8_ha_l9.4:
	.data
# -- End  __svml_cosf8_ha_l9
	.text
.L_2__routine_start___svml_cosf16_ha_z0_5:
# -- Begin  __svml_cosf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cosf16_ha_z0
# --- __svml_cosf16_ha_z0(__m512)
__svml_cosf16_ha_z0:
# parameter 1: %zmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2442.1
        .byte     15                                            #2771.35
        .byte     30                                            #2771.35
        .byte     250                                           #2771.35
	.cfi_startproc
..___tag_value___svml_cosf16_ha_z0.162:
..L163:
                                                        #2442.1
        pushq     %rbp                                          #2442.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2442.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2442.1
        subq      $192, %rsp                                    #2442.1
        xorl      %esi, %esi                                    #2458.1
        vmovups   5504+__svml_scos_ha_data_internal(%rip), %zmm10 #2459.46
        vmovups   5440+__svml_scos_ha_data_internal(%rip), %zmm7 #2483.56
        vmovups   5376+__svml_scos_ha_data_internal(%rip), %zmm5 #2484.53
        vmovups   4160+__svml_scos_ha_data_internal(%rip), %zmm4 #2463.57
        vmovups   5568+__svml_scos_ha_data_internal(%rip), %zmm9 #2488.55
        vmovups   4992+__svml_scos_ha_data_internal(%rip), %zmm15 #2492.53
        vmovaps   %zmm0, %zmm11                                 #2442.1
        vandps    4096+__svml_scos_ha_data_internal(%rip), %zmm11, %zmm3 #2461.10
        vaddps    {rn-sae}, %zmm10, %zmm3, %zmm6                #2462.11
        vpternlogd $255, %zmm10, %zmm10, %zmm10                 #2464.47
        vcmpps    $18, {sae}, %zmm4, %zmm3, %k1                 #2464.115
        vfmadd213ps {rn-sae}, %zmm7, %zmm5, %zmm6               #2485.17
        vsubps    {rn-sae}, %zmm7, %zmm6, %zmm8                 #2487.19
        vpslld    $31, %zmm6, %zmm2                             #2486.41
        vmovups   5312+__svml_scos_ha_data_internal(%rip), %zmm6 #2497.52
        vmovups   5184+__svml_scos_ha_data_internal(%rip), %zmm7 #2500.52
        vsubps    {rn-sae}, %zmm9, %zmm8, %zmm12                #2489.19
        vmovups   5120+__svml_scos_ha_data_internal(%rip), %zmm8 #2502.52
        vpandnd   %zmm3, %zmm3, %zmm10{%k1}                     #2464.47
        vextractf32x8 $1, %zmm12, %ymm13                        #2490.136
        vextractf32x8 $1, %zmm3, %ymm14                         #2491.137
        vcvtps2pd {sae}, %ymm12, %zmm1                          #2490.23
        vcvtps2pd {sae}, %ymm13, %zmm0                          #2490.113
        vcvtps2pd {sae}, %ymm3, %zmm4                           #2491.23
        vcvtps2pd {sae}, %ymm14, %zmm5                          #2491.114
        vptestmd  %zmm10, %zmm10, %k0                           #2510.63
        vmovups   5248+__svml_scos_ha_data_internal(%rip), %zmm13 #2498.52
        vfnmadd231pd {rn-sae}, %zmm1, %zmm15, %zmm4             #2493.19
        vfnmadd231pd {rn-sae}, %zmm0, %zmm15, %zmm5             #2493.133
        vmovups   5056+__svml_scos_ha_data_internal(%rip), %zmm15 #2494.53
        kmovw     %k0, %eax                                     #2510.63
        vfnmadd213pd {rn-sae}, %zmm5, %zmm15, %zmm0             #2495.133
        vfnmadd213pd {rn-sae}, %zmm4, %zmm15, %zmm1             #2495.19
        vmulpd    {rn-sae}, %zmm0, %zmm0, %zmm14                #2496.129
        vmulpd    {rn-sae}, %zmm1, %zmm1, %zmm12                #2496.22
        vmovaps   %zmm13, %zmm9                                 #2499.19
        vfmadd231pd {rn-sae}, %zmm14, %zmm6, %zmm13             #2499.133
        vfmadd231pd {rn-sae}, %zmm12, %zmm6, %zmm9              #2499.19
        vfmadd213pd {rn-sae}, %zmm7, %zmm14, %zmm13             #2501.132
        vfmadd213pd {rn-sae}, %zmm7, %zmm12, %zmm9              #2501.19
        vfmadd213pd {rn-sae}, %zmm8, %zmm14, %zmm13             #2503.132
        vfmadd213pd {rn-sae}, %zmm8, %zmm12, %zmm9              #2503.19
        vmulpd    {rn-sae}, %zmm14, %zmm13, %zmm5               #2504.128
        vmulpd    {rn-sae}, %zmm12, %zmm9, %zmm4                #2504.21
        vfmadd213pd {rn-sae}, %zmm0, %zmm0, %zmm5               #2505.134
        vfmadd213pd {rn-sae}, %zmm1, %zmm1, %zmm4               #2505.21
        vcvtpd2ps {rn-sae}, %zmm5, %ymm0                        #2506.165
        vcvtpd2ps {rn-sae}, %zmm4, %ymm1                        #2506.18
        vinsertf32x8 $1, %ymm0, %zmm1, %zmm1                    #2506.116
        vxorps    %zmm2, %zmm1, %zmm0                           #2507.18
        testl     %eax, %eax                                    #2511.62
        jne       ..B6.17       # Prob 5%                       #2511.62
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm3 zmm10 zmm11
..B6.2:                         # Preds ..B6.17 ..B6.1
                                # Execution count [1.00e+00]
        testl     %esi, %esi                                    #2774.52
        jne       ..B6.4        # Prob 5%                       #2774.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11
..B6.3:                         # Preds ..B6.10 ..B6.8 ..B6.2
                                # Execution count [1.00e+00]
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
        vstmxcsr  32(%rsp)                                      #2774.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11
..B6.5:                         # Preds ..B6.4
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #2774.249
        movl      %edx, %eax                                    #2774.303
        orl       $8064, %eax                                   #2774.303
        cmpl      %eax, %edx                                    #2774.332
        je        ..B6.7        # Prob 78%                      #2774.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm11
..B6.6:                         # Preds ..B6.5
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #2774.344
        vldmxcsr  32(%rsp)                                      #2774.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm11
..B6.7:                         # Preds ..B6.6 ..B6.5
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm11, 64(%rsp)                              #2774.404
        vmovups   %zmm0, 128(%rsp)                              #2774.478
        testl     %esi, %esi                                    #2774.586
        jne       ..B6.12       # Prob 5%                       #2774.586
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B6.8:                         # Preds ..B6.15 ..B6.7
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #2774.931
        je        ..B6.3        # Prob 78%                      #2774.931
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B6.9:                         # Preds ..B6.8
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #2774.956
        movl      32(%rsp), %eax                                #2774.956
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B6.10:                        # Preds ..B6.9
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #2774.943
        orl       %edx, %eax                                    #2774.943
        movl      %eax, 32(%rsp)                                #2774.943
        vldmxcsr  32(%rsp)                                      #2774.943
        jmp       ..B6.3        # Prob 100%                     #2774.943
                                # LOE rbx r12 r13 r14 r15 zmm0
..B6.12:                        # Preds ..B6.7
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #2774.668
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B6.20:                        # Preds ..B6.12
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
..B6.13:                        # Preds ..B6.14 ..B6.20
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #2774.731
        jc        ..B6.16       # Prob 5%                       #2774.731
                                # LOE rbx r15 r12d r13d r14d
..B6.14:                        # Preds ..B6.16 ..B6.13
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #2774.684
        cmpl      $16, %ebx                                     #2774.679
        jl        ..B6.13       # Prob 82%                      #2774.679
                                # LOE rbx r15 r12d r13d r14d
..B6.15:                        # Preds ..B6.14
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
        vmovups   128(%rsp), %zmm0                              #2774.886
        jmp       ..B6.8        # Prob 100%                     #2774.886
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B6.16:                        # Preds ..B6.13
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,4), %rdi                         #2774.760
        lea       128(%rsp,%rbx,4), %rsi                        #2774.760
..___tag_value___svml_cosf16_ha_z0.190:
#       __svml_scos_ha_cout_rare_internal(const float *, float *)
        call      __svml_scos_ha_cout_rare_internal             #2774.760
..___tag_value___svml_cosf16_ha_z0.191:
        jmp       ..B6.14       # Prob 100%                     #2774.760
	.cfi_restore 3
	.cfi_restore 12
	.cfi_restore 13
	.cfi_restore 14
                                # LOE r15 ebx r12d r13d r14d
..B6.17:                        # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   4224+__svml_scos_ha_data_internal(%rip), %zmm6 #2533.52
        lea       __svml_scos_ha_reduction_data_internal(%rip), %rax #2625.328
        vmovups   %zmm0, (%rsp)                                 #[spill]
        vpternlogd $255, %zmm7, %zmm7, %zmm7                    #2535.42
        vandps    %zmm3, %zmm6, %zmm4                           #2534.14
        vcmpps    $4, {sae}, %zmm6, %zmm4, %k1                  #2535.110
        lea       __svml_scos_ha_data_internal(%rip), %rdx      #2744.125
        vmovups   .L_2il0floatpacket.63(%rip), %zmm6            #2635.15
        vpandd    .L_2il0floatpacket.60(%rip), %zmm11, %zmm1    #2623.16
        vpsrld    $23, %zmm1, %zmm8                             #2624.18
        vpslld    $1, %zmm8, %zmm12                             #2625.140
        vpaddd    %zmm8, %zmm12, %zmm2                          #2625.200
        vpslld    $2, %zmm2, %zmm5                              #2625.268
        vpandnd   %zmm4, %zmm4, %zmm7{%k1}                      #2535.42
        vptestmd  %zmm7, %zmm7, %k0                             #2537.33
        kxnorw    %k0, %k0, %k2                                 #2625.328
        kxnorw    %k0, %k0, %k3                                 #2626.329
        kxnorw    %k0, %k0, %k4                                 #2627.328
        kxnorw    %k0, %k0, %k1                                 #2745.125
        kxnorw    %k0, %k0, %k7                                 #2744.125
        kmovw     %k0, %esi                                     #2537.33
        vpxord    %zmm4, %zmm4, %zmm4                           #2625.328
        vpxord    %zmm7, %zmm7, %zmm7                           #2626.329
        vpxord    %zmm14, %zmm14, %zmm14                        #2627.328
        vgatherdps (%rax,%zmm5), %zmm4{%k2}                     #2625.328
        vgatherdps 4(%rax,%zmm5), %zmm7{%k3}                    #2626.329
        vgatherdps 8(%rax,%zmm5), %zmm14{%k4}                   #2627.328
        vpsrld    $16, %zmm4, %zmm9                             #2636.12
        vpsrld    $16, %zmm7, %zmm15                            #2638.12
        vpsrld    $16, %zmm14, %zmm13                           #2640.12
        kxnorw    %k0, %k0, %k3                                 #2747.127
        kxnorw    %k0, %k0, %k2                                 #2746.125
        vpandd    .L_2il0floatpacket.61(%rip), %zmm11, %zmm3    #2633.19
        vpaddd    .L_2il0floatpacket.62(%rip), %zmm3, %zmm1     #2634.19
        vpsrld    $16, %zmm1, %zmm8                             #2642.12
        vpandd    %zmm6, %zmm14, %zmm5                          #2641.10
        vpandd    %zmm6, %zmm1, %zmm14                          #2643.10
        vpandd    %zmm6, %zmm7, %zmm12                          #2639.10
        vpmulld   %zmm15, %zmm8, %zmm3                          #2646.13
        vpmulld   %zmm9, %zmm14, %zmm7                          #2650.13
        vpmulld   %zmm15, %zmm14, %zmm9                         #2652.13
        vpmulld   %zmm13, %zmm14, %zmm15                        #2654.13
        vpmulld   %zmm13, %zmm8, %zmm1                          #2648.13
        vpsrld    $16, %zmm15, %zmm13                           #2656.13
        vpsrld    $16, %zmm9, %zmm15                            #2658.13
        vpaddd    %zmm13, %zmm1, %zmm1                          #2660.11
        vpandd    %zmm6, %zmm4, %zmm0                           #2637.10
        vpmulld   %zmm0, %zmm8, %zmm4                           #2645.13
        vpmulld   %zmm12, %zmm8, %zmm2                          #2647.13
        vpmulld   %zmm5, %zmm8, %zmm8                           #2649.13
        vpmulld   %zmm0, %zmm14, %zmm5                          #2651.13
        vpmulld   %zmm12, %zmm14, %zmm0                         #2653.13
        vpsrld    $16, %zmm0, %zmm14                            #2657.13
        vpsrld    $16, %zmm5, %zmm12                            #2659.13
        vpsrld    $16, %zmm8, %zmm8                             #2674.13
        vpaddd    %zmm14, %zmm2, %zmm13                         #2661.11
        vpaddd    %zmm15, %zmm3, %zmm14                         #2662.11
        vpaddd    %zmm12, %zmm4, %zmm3                          #2663.11
        vpandd    %zmm6, %zmm0, %zmm0                           #2664.11
        vpandd    %zmm6, %zmm5, %zmm12                          #2666.11
        vpaddd    %zmm1, %zmm0, %zmm5                           #2669.11
        vpaddd    %zmm14, %zmm12, %zmm0                         #2671.11
        vpaddd    %zmm5, %zmm8, %zmm15                          #2675.11
        vpandd    %zmm6, %zmm7, %zmm2                           #2667.11
        vpaddd    %zmm3, %zmm2, %zmm14                          #2672.11
        vpsrld    $16, %zmm15, %zmm3                            #2676.13
        vpandd    %zmm6, %zmm9, %zmm9                           #2665.11
        vpaddd    %zmm13, %zmm9, %zmm9                          #2670.11
        vpaddd    %zmm9, %zmm3, %zmm12                          #2677.11
        vpsrld    $16, %zmm12, %zmm4                            #2678.13
        vpslld    $16, %zmm12, %zmm2                            #2685.19
        vpaddd    %zmm0, %zmm4, %zmm13                          #2679.11
        vmovups   .L_2il0floatpacket.66(%rip), %zmm4            #2695.15
        vpsrld    $16, %zmm13, %zmm7                            #2680.13
        vpaddd    %zmm14, %zmm7, %zmm1                          #2681.11
        vpandd    %zmm6, %zmm15, %zmm3                          #2682.11
        vpandd    %zmm6, %zmm13, %zmm8                          #2683.11
        vpslld    $16, %zmm1, %zmm6                             #2684.18
        vpaddd    %zmm8, %zmm6, %zmm5                           #2686.16
        vpaddd    %zmm3, %zmm2, %zmm8                           #2687.17
        vpsrld    $9, %zmm5, %zmm9                              #2692.20
        vpandd    .L_2il0floatpacket.64(%rip), %zmm11, %zmm2    #2689.15
        vpxord    .L_2il0floatpacket.65(%rip), %zmm2, %zmm6     #2691.20
        vpord     %zmm6, %zmm9, %zmm0                           #2693.18
        vaddps    {rn-sae}, %zmm0, %zmm4, %zmm14                #2696.21
        vsubps    {rn-sae}, %zmm4, %zmm14, %zmm7                #2697.11
        vsubps    {rn-sae}, %zmm7, %zmm0, %zmm13                #2698.19
        vpternlogd $255, %zmm0, %zmm0, %zmm0                    #2736.41
        vpandd    .L_2il0floatpacket.69(%rip), %zmm8, %zmm15    #2705.18
        vpandd    .L_2il0floatpacket.71(%rip), %zmm5, %zmm5     #2714.18
        vpxord    .L_2il0floatpacket.68(%rip), %zmm2, %zmm3     #2703.20
        vpslld    $5, %zmm15, %zmm1                             #2706.20
        vpsrld    $18, %zmm8, %zmm8                             #2716.19
        vpxord    .L_2il0floatpacket.70(%rip), %zmm2, %zmm9     #2712.20
        vpslld    $14, %zmm5, %zmm2                             #2715.20
        vpord     %zmm3, %zmm1, %zmm12                          #2707.18
        vpord     %zmm8, %zmm2, %zmm5                           #2717.18
        vsubps    {rn-sae}, %zmm3, %zmm12, %zmm15               #2710.19
        vmovups   .L_2il0floatpacket.73(%rip), %zmm2            #2727.21
        vpord     %zmm9, %zmm5, %zmm3                           #2718.18
        vsubps    {rn-sae}, %zmm9, %zmm3, %zmm6                 #2721.20
        vmovups   .L_2il0floatpacket.72(%rip), %zmm5            #2726.20
        vmovups   .L_2il0floatpacket.75(%rip), %zmm9            #2733.16
        vaddps    {rn-sae}, %zmm6, %zmm13, %zmm4                #2722.13
        vsubps    {rn-sae}, %zmm4, %zmm13, %zmm13               #2723.19
        vmulps    {rn-sae}, %zmm5, %zmm4, %zmm7                 #2728.19
        vaddps    {rn-sae}, %zmm13, %zmm6, %zmm8                #2724.20
        vandps    .L_2il0floatpacket.74(%rip), %zmm11, %zmm6    #2734.11
        vaddps    {rn-sae}, %zmm15, %zmm8, %zmm8                #2725.13
        vpternlogd $255, %zmm15, %zmm15, %zmm15                 #2735.38
        vcmpps    $26, {sae}, %zmm9, %zmm6, %k5                 #2735.106
        vcmpps    $22, {sae}, %zmm9, %zmm6, %k6                 #2736.109
        vmovaps   %zmm5, %zmm3                                  #2729.17
        vfmsub213ps {rn-sae}, %zmm7, %zmm4, %zmm3               #2729.17
        vfmadd213ps {rn-sae}, %zmm3, %zmm2, %zmm4               #2730.17
        vfmadd213ps {rn-sae}, %zmm4, %zmm5, %zmm8               #2731.17
        vpandd    .L_2il0floatpacket.67(%rip), %zmm14, %zmm14   #2701.13
        vpslld    $4, %zmm14, %zmm13                            #2744.77
        vpandnd   %zmm6, %zmm6, %zmm15{%k5}                     #2735.38
        vandps    %zmm7, %zmm15, %zmm12                         #2738.25
        vandps    %zmm8, %zmm15, %zmm3                          #2740.9
        vpxord    %zmm9, %zmm9, %zmm9                           #2745.125
        vpxord    %zmm7, %zmm7, %zmm7                           #2747.127
        vgatherdps 4(%rdx,%zmm13), %zmm9{%k1}                   #2745.125
        vgatherdps 12(%rdx,%zmm13), %zmm7{%k3}                  #2747.127
        vpandnd   %zmm6, %zmm6, %zmm0{%k6}                      #2736.41
        vandps    %zmm11, %zmm0, %zmm1                          #2737.24
        vorps     %zmm12, %zmm1, %zmm2                          #2739.9
        vmovups   4352+__svml_scos_ha_data_internal(%rip), %zmm12 #2755.46
        vmulps    {rn-sae}, %zmm2, %zmm2, %zmm4                 #2743.17
        vmovaps   %zmm2, %zmm14                                 #2748.22
        vfmadd213ps {rn-sae}, %zmm9, %zmm7, %zmm14              #2748.22
        vmovaps   %zmm2, %zmm8                                  #2749.21
        vsubps    {rn-sae}, %zmm14, %zmm9, %zmm1                #2750.20
        vfmadd231ps {rn-sae}, %zmm2, %zmm7, %zmm1               #2751.18
        vpxord    %zmm0, %zmm0, %zmm0                           #2744.125
        vgatherdps (%rdx,%zmm13), %zmm0{%k7}                    #2744.125
        vfmadd132ps {rn-sae}, %zmm0, %zmm14, %zmm8              #2749.21
        vaddps    {rn-sae}, %zmm7, %zmm0, %zmm15                #2760.15
        vmovups   4416+__svml_scos_ha_data_internal(%rip), %zmm7 #2762.46
        vsubps    {rn-sae}, %zmm8, %zmm14, %zmm6                #2752.20
        vfmadd231ps {rn-sae}, %zmm0, %zmm2, %zmm6               #2753.18
        vmovups   4480+__svml_scos_ha_data_internal(%rip), %zmm0 #2761.46
        vaddps    {rn-sae}, %zmm1, %zmm6, %zmm6                 #2754.24
        vfmadd231ps {rn-sae}, %zmm4, %zmm0, %zmm7               #2763.14
        vpxord    %zmm5, %zmm5, %zmm5                           #2746.125
        vgatherdps 8(%rdx,%zmm13), %zmm5{%k2}                   #2746.125
        vmovups   4288+__svml_scos_ha_data_internal(%rip), %zmm13 #2756.46
        vfmadd231ps {rn-sae}, %zmm4, %zmm12, %zmm13             #2757.14
        vmulps    {rn-sae}, %zmm4, %zmm13, %zmm14               #2758.16
        vmulps    {rn-sae}, %zmm4, %zmm7, %zmm4                 #2764.16
        vmulps    {rn-sae}, %zmm2, %zmm14, %zmm1                #2759.16
        vfnmadd213ps {rn-sae}, %zmm15, %zmm9, %zmm2             #2765.13
        vfmadd213ps {rn-sae}, %zmm5, %zmm2, %zmm3               #2766.16
        vfmadd213ps {rn-sae}, %zmm6, %zmm2, %zmm1               #2768.18
        vfmadd213ps {rn-sae}, %zmm3, %zmm9, %zmm4               #2767.17
        vaddps    {rn-sae}, %zmm1, %zmm4, %zmm2                 #2769.19
        vaddps    {rn-sae}, %zmm2, %zmm8, %zmm3                 #2770.22
        vpandnd   (%rsp), %zmm10, %zmm5                         #2771.52[spill]
        vpandd    %zmm10, %zmm3, %zmm10                         #2771.153
        vpord     %zmm10, %zmm5, %zmm0                          #2771.35
        jmp       ..B6.2        # Prob 100%                     #2771.35
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11
	.cfi_endproc
# mark_end;
	.type	__svml_cosf16_ha_z0,@function
	.size	__svml_cosf16_ha_z0,.-__svml_cosf16_ha_z0
..LN__svml_cosf16_ha_z0.5:
	.data
# -- End  __svml_cosf16_ha_z0
	.text
.L_2__routine_start___svml_scos_ha_cout_rare_internal_6:
# -- Begin  __svml_scos_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_scos_ha_cout_rare_internal
	.globl __svml_scos_ha_cout_rare_internal
# --- __svml_scos_ha_cout_rare_internal(const float *, float *)
__svml_scos_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1721.1
        .byte     15                                            #1740.12
        .byte     30                                            #1740.12
        .byte     250                                           #1740.12
	.cfi_startproc
..___tag_value___svml_scos_ha_cout_rare_internal.199:
..L200:
                                                        #1721.1
        movl      (%rdi), %edx                                  #1724.19
        movzwl    2(%rdi), %eax                                 #1726.36
        movl      %edx, -8(%rsp)                                #1724.9
        andl      $32640, %eax                                  #1726.36
        shrl      $24, %edx                                     #1725.30
        andl      $127, %edx                                    #1725.30
        cmpl      $32640, %eax                                  #1726.62
        jne       ..B7.6        # Prob 67%                      #1726.62
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 edx
..B7.2:                         # Preds ..B7.1
                                # Execution count [3.29e-01]
        movb      %dl, -5(%rsp)                                 #1725.30
        cmpl      $2139095040, -8(%rsp)                         #1728.91
        jne       ..B7.4        # Prob 50%                      #1728.91
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15
..B7.3:                         # Preds ..B7.2
                                # Execution count [1.64e-01]
        movss     (%rdi), %xmm0                                 #1730.34
        movl      $1, %eax                                      #1732.24
        mulss     _vmlsCosHATab(%rip), %xmm0                    #1730.53
        movss     %xmm0, (%rsi)                                 #1730.19
        ret                                                     #1730.19
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.4:                         # Preds ..B7.2
                                # Execution count [1.64e-01]
        movss     (%rdi), %xmm0                                 #1736.34
        xorl      %eax, %eax                                    #1737.24
        mulss     (%rdi), %xmm0                                 #1736.41
        movss     %xmm0, (%rsi)                                 #1736.19
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.5:                         # Preds ..B7.4
                                # Execution count [3.29e-01]
        ret                                                     #1732.24
                                # LOE
..B7.6:                         # Preds ..B7.1
                                # Execution count [6.71e-01]
        xorl      %eax, %eax                                    #1740.12
        ret                                                     #1740.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_scos_ha_cout_rare_internal,@function
	.size	__svml_scos_ha_cout_rare_internal,.-__svml_scos_ha_cout_rare_internal
..LN__svml_scos_ha_cout_rare_internal.6:
	.data
# -- End  __svml_scos_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
.L_2il0floatpacket.60:
	.long	0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000
	.type	.L_2il0floatpacket.60,@object
	.size	.L_2il0floatpacket.60,64
	.align 64
.L_2il0floatpacket.61:
	.long	0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff
	.type	.L_2il0floatpacket.61,@object
	.size	.L_2il0floatpacket.61,64
	.align 64
.L_2il0floatpacket.62:
	.long	0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000
	.type	.L_2il0floatpacket.62,@object
	.size	.L_2il0floatpacket.62,64
	.align 64
.L_2il0floatpacket.63:
	.long	0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff
	.type	.L_2il0floatpacket.63,@object
	.size	.L_2il0floatpacket.63,64
	.align 64
.L_2il0floatpacket.64:
	.long	0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000
	.type	.L_2il0floatpacket.64,@object
	.size	.L_2il0floatpacket.64,64
	.align 64
.L_2il0floatpacket.65:
	.long	0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000
	.type	.L_2il0floatpacket.65,@object
	.size	.L_2il0floatpacket.65,64
	.align 64
.L_2il0floatpacket.66:
	.long	0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000
	.type	.L_2il0floatpacket.66,@object
	.size	.L_2il0floatpacket.66,64
	.align 64
.L_2il0floatpacket.67:
	.long	0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff
	.type	.L_2il0floatpacket.67,@object
	.size	.L_2il0floatpacket.67,64
	.align 64
.L_2il0floatpacket.68:
	.long	0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000
	.type	.L_2il0floatpacket.68,@object
	.size	.L_2il0floatpacket.68,64
	.align 64
.L_2il0floatpacket.69:
	.long	0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff
	.type	.L_2il0floatpacket.69,@object
	.size	.L_2il0floatpacket.69,64
	.align 64
.L_2il0floatpacket.70:
	.long	0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000
	.type	.L_2il0floatpacket.70,@object
	.size	.L_2il0floatpacket.70,64
	.align 64
.L_2il0floatpacket.71:
	.long	0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff
	.type	.L_2il0floatpacket.71,@object
	.size	.L_2il0floatpacket.71,64
	.align 64
.L_2il0floatpacket.72:
	.long	0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb
	.type	.L_2il0floatpacket.72,@object
	.size	.L_2il0floatpacket.72,64
	.align 64
.L_2il0floatpacket.73:
	.long	0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e
	.type	.L_2il0floatpacket.73,@object
	.size	.L_2il0floatpacket.73,64
	.align 64
.L_2il0floatpacket.74:
	.long	0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff
	.type	.L_2il0floatpacket.74,@object
	.size	.L_2il0floatpacket.74,64
	.align 64
.L_2il0floatpacket.75:
	.long	0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000
	.type	.L_2il0floatpacket.75,@object
	.size	.L_2il0floatpacket.75,64
	.align 64
	.hidden __svml_scos_ha_reduction_data_internal
	.globl __svml_scos_ha_reduction_data_internal
__svml_scos_ha_reduction_data_internal:
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
	.type	__svml_scos_ha_reduction_data_internal,@object
	.size	__svml_scos_ha_reduction_data_internal,3072
	.align 64
	.hidden __svml_scos_ha_data_internal
	.globl __svml_scos_ha_data_internal
__svml_scos_ha_data_internal:
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
	.long	1070141403
	.long	1070141403
	.long	1070141403
	.long	1070141403
	.long	1070141403
	.long	1070141403
	.long	1070141403
	.long	1070141403
	.long	1070141403
	.long	1070141403
	.long	1070141403
	.long	1070141403
	.long	1070141403
	.long	1070141403
	.long	1070141403
	.long	1070141403
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
	.type	__svml_scos_ha_data_internal,@object
	.size	__svml_scos_ha_data_internal,5696
	.align 32
.L_2il0floatpacket.21:
	.long	0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000
	.type	.L_2il0floatpacket.21,@object
	.size	.L_2il0floatpacket.21,32
	.align 32
.L_2il0floatpacket.22:
	.long	0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb
	.type	.L_2il0floatpacket.22,@object
	.size	.L_2il0floatpacket.22,32
	.align 32
.L_2il0floatpacket.23:
	.long	0x40c91000,0x40c91000,0x40c91000,0x40c91000,0x40c91000,0x40c91000,0x40c91000,0x40c91000
	.type	.L_2il0floatpacket.23,@object
	.size	.L_2il0floatpacket.23,32
	.align 32
.L_2il0floatpacket.24:
	.long	0xb795777a,0xb795777a,0xb795777a,0xb795777a,0xb795777a,0xb795777a,0xb795777a,0xb795777a
	.type	.L_2il0floatpacket.24,@object
	.size	.L_2il0floatpacket.24,32
	.align 32
.L_2il0floatpacket.25:
	.long	0xfffff000,0xfffff000,0xfffff000,0xfffff000,0xfffff000,0xfffff000,0xfffff000,0xfffff000
	.type	.L_2il0floatpacket.25,@object
	.size	.L_2il0floatpacket.25,32
	.align 32
.L_2il0floatpacket.26:
	.long	0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff
	.type	.L_2il0floatpacket.26,@object
	.size	.L_2il0floatpacket.26,32
	.align 32
.L_2il0floatpacket.27:
	.long	0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000
	.type	.L_2il0floatpacket.27,@object
	.size	.L_2il0floatpacket.27,32
	.align 32
.L_2il0floatpacket.48:
	.long	0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000
	.type	.L_2il0floatpacket.48,@object
	.size	.L_2il0floatpacket.48,32
	.align 32
.L_2il0floatpacket.49:
	.long	0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff
	.type	.L_2il0floatpacket.49,@object
	.size	.L_2il0floatpacket.49,32
	.align 32
.L_2il0floatpacket.50:
	.long	0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000
	.type	.L_2il0floatpacket.50,@object
	.size	.L_2il0floatpacket.50,32
	.align 32
.L_2il0floatpacket.51:
	.long	0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff
	.type	.L_2il0floatpacket.51,@object
	.size	.L_2il0floatpacket.51,32
	.align 32
.L_2il0floatpacket.52:
	.long	0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000
	.type	.L_2il0floatpacket.52,@object
	.size	.L_2il0floatpacket.52,32
	.align 32
.L_2il0floatpacket.53:
	.long	0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000
	.type	.L_2il0floatpacket.53,@object
	.size	.L_2il0floatpacket.53,32
	.align 32
.L_2il0floatpacket.54:
	.long	0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000
	.type	.L_2il0floatpacket.54,@object
	.size	.L_2il0floatpacket.54,32
	.align 32
.L_2il0floatpacket.55:
	.long	0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff
	.type	.L_2il0floatpacket.55,@object
	.size	.L_2il0floatpacket.55,32
	.align 32
.L_2il0floatpacket.56:
	.long	0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000
	.type	.L_2il0floatpacket.56,@object
	.size	.L_2il0floatpacket.56,32
	.align 32
.L_2il0floatpacket.57:
	.long	0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff
	.type	.L_2il0floatpacket.57,@object
	.size	.L_2il0floatpacket.57,32
	.align 32
.L_2il0floatpacket.58:
	.long	0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e
	.type	.L_2il0floatpacket.58,@object
	.size	.L_2il0floatpacket.58,32
	.align 32
.L_2il0floatpacket.59:
	.long	0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff
	.type	.L_2il0floatpacket.59,@object
	.size	.L_2il0floatpacket.59,32
	.align 16
.L_2il0floatpacket.28:
	.long	0xffffffff,0x00000000,0xffffffff,0x00000000
	.type	.L_2il0floatpacket.28,@object
	.size	.L_2il0floatpacket.28,16
	.align 16
.L_2il0floatpacket.29:
	.long	0x7f800000,0x7f800000,0x7f800000,0x7f800000
	.type	.L_2il0floatpacket.29,@object
	.size	.L_2il0floatpacket.29,16
	.align 16
.L_2il0floatpacket.30:
	.long	0x007fffff,0x007fffff,0x007fffff,0x007fffff
	.type	.L_2il0floatpacket.30,@object
	.size	.L_2il0floatpacket.30,16
	.align 16
.L_2il0floatpacket.31:
	.long	0x00800000,0x00800000,0x00800000,0x00800000
	.type	.L_2il0floatpacket.31,@object
	.size	.L_2il0floatpacket.31,16
	.align 16
.L_2il0floatpacket.32:
	.long	0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff
	.type	.L_2il0floatpacket.32,@object
	.size	.L_2il0floatpacket.32,16
	.align 16
.L_2il0floatpacket.33:
	.long	0x80000000,0x80000000,0x80000000,0x80000000
	.type	.L_2il0floatpacket.33,@object
	.size	.L_2il0floatpacket.33,16
	.align 16
.L_2il0floatpacket.34:
	.long	0x3f800000,0x3f800000,0x3f800000,0x3f800000
	.type	.L_2il0floatpacket.34,@object
	.size	.L_2il0floatpacket.34,16
	.align 16
.L_2il0floatpacket.35:
	.long	0x47400000,0x47400000,0x47400000,0x47400000
	.type	.L_2il0floatpacket.35,@object
	.size	.L_2il0floatpacket.35,16
	.align 16
.L_2il0floatpacket.36:
	.long	0x28800000,0x28800000,0x28800000,0x28800000
	.type	.L_2il0floatpacket.36,@object
	.size	.L_2il0floatpacket.36,16
	.align 16
.L_2il0floatpacket.37:
	.long	0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff
	.type	.L_2il0floatpacket.37,@object
	.size	.L_2il0floatpacket.37,16
	.align 16
.L_2il0floatpacket.38:
	.long	0x34000000,0x34000000,0x34000000,0x34000000
	.type	.L_2il0floatpacket.38,@object
	.size	.L_2il0floatpacket.38,16
	.align 16
.L_2il0floatpacket.39:
	.long	0x000001ff,0x000001ff,0x000001ff,0x000001ff
	.type	.L_2il0floatpacket.39,@object
	.size	.L_2il0floatpacket.39,16
	.align 16
.L_2il0floatpacket.40:
	.long	0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb
	.type	.L_2il0floatpacket.40,@object
	.size	.L_2il0floatpacket.40,16
	.align 16
.L_2il0floatpacket.41:
	.long	0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e
	.type	.L_2il0floatpacket.41,@object
	.size	.L_2il0floatpacket.41,16
	.align 16
.L_2il0floatpacket.42:
	.long	0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff
	.type	.L_2il0floatpacket.42,@object
	.size	.L_2il0floatpacket.42,16
	.align 16
.L_2il0floatpacket.43:
	.long	0x35800000,0x35800000,0x35800000,0x35800000
	.type	.L_2il0floatpacket.43,@object
	.size	.L_2il0floatpacket.43,16
	.align 16
.L_2il0floatpacket.44:
	.long	0x000000ff,0x000000ff,0x000000ff,0x000000ff
	.type	.L_2il0floatpacket.44,@object
	.size	.L_2il0floatpacket.44,16
	.align 16
.L_2il0floatpacket.45:
	.long	0x40c91000,0x40c91000,0x40c91000,0x40c91000
	.type	.L_2il0floatpacket.45,@object
	.size	.L_2il0floatpacket.45,16
	.align 16
.L_2il0floatpacket.46:
	.long	0xb795777a,0xb795777a,0xb795777a,0xb795777a
	.type	.L_2il0floatpacket.46,@object
	.size	.L_2il0floatpacket.46,16
	.align 16
.L_2il0floatpacket.47:
	.long	0xfffff000,0xfffff000,0xfffff000,0xfffff000
	.type	.L_2il0floatpacket.47,@object
	.size	.L_2il0floatpacket.47,16
	.align 8
_vmlsCosHATab:
	.long	0
	.long	2139095040
	.long	0
	.long	0
	.type	_vmlsCosHATab,@object
	.size	_vmlsCosHATab,16
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
