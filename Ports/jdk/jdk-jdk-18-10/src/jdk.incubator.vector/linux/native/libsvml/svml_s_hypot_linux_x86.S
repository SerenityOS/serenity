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
	.file "svml_s_hypot.c"
	.text
..TXTST0:
.L_2__routine_start___svml_hypotf8_ha_l9_0:
# -- Begin  __svml_hypotf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypotf8_ha_l9
# --- __svml_hypotf8_ha_l9(__m256, __m256)
__svml_hypotf8_ha_l9:
# parameter 1: %ymm0
# parameter 2: %ymm1
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #109.1
        .byte     15                                            #200.663
        .byte     30                                            #200.663
        .byte     250                                           #200.663
	.cfi_startproc
..___tag_value___svml_hypotf8_ha_l9.1:
..L2:
                                                          #109.1
        pushq     %rbp                                          #109.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #109.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #109.1
        subq      $256, %rsp                                    #109.1
        vmovups   128+__svml_shypot_ha_data_internal(%rip), %ymm5 #140.53
        vmovups   __svml_shypot_ha_data_internal(%rip), %ymm10  #139.54
        vmovups   192+__svml_shypot_ha_data_internal(%rip), %ymm3 #141.49
        vmovaps   %ymm1, %ymm4                                  #109.1
        vandps    %ymm5, %ymm0, %ymm7                           #143.20
        vandps    %ymm5, %ymm4, %ymm6                           #144.20
        vmaxps    %ymm6, %ymm7, %ymm12                          #145.13
        vminps    %ymm7, %ymm6, %ymm5                           #146.13
        vpsubd    %ymm5, %ymm12, %ymm8                          #157.24
        vandps    %ymm10, %ymm12, %ymm13                        #147.105
        vsubps    %ymm13, %ymm12, %ymm14                        #147.153
        vaddps    %ymm13, %ymm12, %ymm15                        #165.15
        vmulps    %ymm13, %ymm13, %ymm2                         #163.15
        vmulps    %ymm15, %ymm14, %ymm7                         #166.15
        vpcmpgtd  320+__svml_shypot_ha_data_internal(%rip), %ymm8, %ymm9 #158.24
        vandnps   %ymm10, %ymm9, %ymm11                         #160.25
        vandps    %ymm11, %ymm5, %ymm6                          #162.105
        vsubps    %ymm6, %ymm5, %ymm1                           #162.153
        vfmadd231ps %ymm6, %ymm6, %ymm2                         #164.15
        vmovups   384+__svml_shypot_ha_data_internal(%rip), %ymm8 #174.23
        vfmadd231ps %ymm1, %ymm5, %ymm7                         #167.15
        vfmadd213ps %ymm7, %ymm6, %ymm1                         #168.15
        vaddps    %ymm1, %ymm2, %ymm12                          #169.13
        vrsqrtps  %ymm12, %ymm13                                #182.14
        vmulps    %ymm12, %ymm13, %ymm5                         #183.14
        vmulps    %ymm3, %ymm13, %ymm6                          #184.14
        vfnmadd231ps %ymm5, %ymm6, %ymm3                        #185.14
        vfmadd213ps %ymm5, %ymm3, %ymm5                         #187.14
        vfmadd132ps %ymm3, %ymm6, %ymm6                         #186.14
        vandps    64+__svml_shypot_ha_data_internal(%rip), %ymm5, %ymm3 #192.109
        vfmsub231ps %ymm3, %ymm3, %ymm2                         #193.18
        vsubps    %ymm3, %ymm5, %ymm14                          #192.158
        vfmadd213ps %ymm2, %ymm14, %ymm3                        #194.18
        vfmadd231ps %ymm5, %ymm14, %ymm3                        #195.18
        vpcmpgtd  %ymm12, %ymm8, %ymm9                          #177.23
        vpcmpgtd  448+__svml_shypot_ha_data_internal(%rip), %ymm12, %ymm10 #178.24
        vpor      %ymm10, %ymm9, %ymm11                         #179.16
        vsubps    %ymm1, %ymm3, %ymm1                           #196.18
        vmovmskps %ymm11, %edx                                  #180.45
        vfnmadd213ps %ymm5, %ymm1, %ymm6                        #198.14
        testl     %edx, %edx                                    #200.52
        jne       ..B1.3        # Prob 5%                       #200.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm4 ymm6
..B1.2:                         # Preds ..B1.3 ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        vmovaps   %ymm6, %ymm0                                  #203.12
        movq      %rbp, %rsp                                    #203.12
        popq      %rbp                                          #203.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #203.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm0, 64(%rsp)                               #200.239
        vmovups   %ymm4, 128(%rsp)                              #200.313
        vmovups   %ymm6, 192(%rsp)                              #200.387
        je        ..B1.2        # Prob 95%                      #200.491
                                # LOE rbx r12 r13 r14 r15 edx ymm6
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #200.571
                                # LOE rbx r12 r13 r14 r15 eax edx
..B1.13:                        # Preds ..B1.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #200.634
        jc        ..B1.10       # Prob 5%                       #200.634
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #200.587
        cmpl      $8, %r12d                                     #200.582
        jl        ..B1.7        # Prob 82%                      #200.582
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   192(%rsp), %ymm6                              #200.806
        jmp       ..B1.2        # Prob 100%                     #200.806
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm6
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #200.663
        lea       128(%rsp,%r12,4), %rsi                        #200.663
        lea       192(%rsp,%r12,4), %rdx                        #200.663
..___tag_value___svml_hypotf8_ha_l9.19:
#       __svml_shypot_ha_cout_rare_internal(const float *, const float *, float *)
        call      __svml_shypot_ha_cout_rare_internal           #200.663
..___tag_value___svml_hypotf8_ha_l9.20:
        jmp       ..B1.8        # Prob 100%                     #200.663
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_hypotf8_ha_l9,@function
	.size	__svml_hypotf8_ha_l9,.-__svml_hypotf8_ha_l9
..LN__svml_hypotf8_ha_l9.0:
	.data
# -- End  __svml_hypotf8_ha_l9
	.text
.L_2__routine_start___svml_hypotf4_ha_l9_1:
# -- Begin  __svml_hypotf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypotf4_ha_l9
# --- __svml_hypotf4_ha_l9(__m128, __m128)
__svml_hypotf4_ha_l9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #208.1
        .byte     15                                            #299.654
        .byte     30                                            #299.654
        .byte     250                                           #299.654
	.cfi_startproc
..___tag_value___svml_hypotf4_ha_l9.22:
..L23:
                                                         #208.1
        pushq     %rbp                                          #208.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #208.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #208.1
        subq      $256, %rsp                                    #208.1
        vmovaps   %xmm1, %xmm4                                  #208.1
        vmovups   128+__svml_shypot_ha_data_internal(%rip), %xmm5 #239.50
        vandps    %xmm5, %xmm0, %xmm7                           #242.20
        vandps    %xmm5, %xmm4, %xmm6                           #243.20
        vmaxps    %xmm6, %xmm7, %xmm12                          #244.13
        vminps    %xmm7, %xmm6, %xmm5                           #245.13
        vmovups   __svml_shypot_ha_data_internal(%rip), %xmm10  #238.51
        vpsubd    %xmm5, %xmm12, %xmm8                          #256.24
        vandps    %xmm10, %xmm12, %xmm13                        #246.105
        vpcmpgtd  320+__svml_shypot_ha_data_internal(%rip), %xmm8, %xmm9 #257.24
        vsubps    %xmm13, %xmm12, %xmm14                        #246.150
        vmulps    %xmm13, %xmm13, %xmm2                         #262.15
        vaddps    %xmm13, %xmm12, %xmm15                        #264.15
        vandnps   %xmm10, %xmm9, %xmm11                         #259.25
        vandps    %xmm11, %xmm5, %xmm6                          #261.105
        vmulps    %xmm15, %xmm14, %xmm7                         #265.15
        vsubps    %xmm6, %xmm5, %xmm1                           #261.150
        vfmadd231ps %xmm6, %xmm6, %xmm2                         #263.15
        vmovups   192+__svml_shypot_ha_data_internal(%rip), %xmm3 #240.46
        vfmadd231ps %xmm1, %xmm5, %xmm7                         #266.15
        vmovups   384+__svml_shypot_ha_data_internal(%rip), %xmm8 #273.23
        vfmadd213ps %xmm7, %xmm6, %xmm1                         #267.15
        vaddps    %xmm1, %xmm2, %xmm12                          #268.13
        vrsqrtps  %xmm12, %xmm13                                #281.14
        vmulps    %xmm12, %xmm13, %xmm5                         #282.14
        vpcmpgtd  %xmm12, %xmm8, %xmm9                          #276.23
        vmulps    %xmm3, %xmm13, %xmm6                          #283.14
        vfnmadd231ps %xmm5, %xmm6, %xmm3                        #284.14
        vpcmpgtd  448+__svml_shypot_ha_data_internal(%rip), %xmm12, %xmm10 #277.24
        vpor      %xmm10, %xmm9, %xmm11                         #278.16
        vmovmskps %xmm11, %edx                                  #279.44
        vfmadd213ps %xmm5, %xmm3, %xmm5                         #286.14
        vfmadd132ps %xmm3, %xmm6, %xmm6                         #285.14
        vandps    64+__svml_shypot_ha_data_internal(%rip), %xmm5, %xmm3 #291.109
        vfmsub231ps %xmm3, %xmm3, %xmm2                         #292.18
        vsubps    %xmm3, %xmm5, %xmm14                          #291.155
        vfmadd213ps %xmm2, %xmm14, %xmm3                        #293.18
        vfmadd231ps %xmm5, %xmm14, %xmm3                        #294.18
        vsubps    %xmm1, %xmm3, %xmm1                           #295.18
        vfnmadd213ps %xmm5, %xmm1, %xmm6                        #297.14
        testl     %edx, %edx                                    #299.52
        jne       ..B2.3        # Prob 5%                       #299.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm4 xmm6
..B2.2:                         # Preds ..B2.3 ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        movaps    %xmm6, %xmm0                                  #302.12
        movq      %rbp, %rsp                                    #302.12
        popq      %rbp                                          #302.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #302.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm0, 64(%rsp)                               #299.236
        vmovups   %xmm4, 128(%rsp)                              #299.307
        vmovups   %xmm6, 192(%rsp)                              #299.378
        je        ..B2.2        # Prob 95%                      #299.482
                                # LOE rbx r12 r13 r14 r15 edx xmm6
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #299.562
        movq      %r12, 8(%rsp)                                 #299.562[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #299.562
        movq      %r13, (%rsp)                                  #299.562[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #299.562
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #299.625
        jc        ..B2.10       # Prob 5%                       #299.625
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #299.578
        cmpl      $4, %r12d                                     #299.573
        jl        ..B2.7        # Prob 82%                      #299.573
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm6                              #299.794
        jmp       ..B2.2        # Prob 100%                     #299.794
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm6
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #299.654
        lea       128(%rsp,%r12,4), %rsi                        #299.654
        lea       192(%rsp,%r12,4), %rdx                        #299.654
..___tag_value___svml_hypotf4_ha_l9.40:
#       __svml_shypot_ha_cout_rare_internal(const float *, const float *, float *)
        call      __svml_shypot_ha_cout_rare_internal           #299.654
..___tag_value___svml_hypotf4_ha_l9.41:
        jmp       ..B2.8        # Prob 100%                     #299.654
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_hypotf4_ha_l9,@function
	.size	__svml_hypotf4_ha_l9,.-__svml_hypotf4_ha_l9
..LN__svml_hypotf4_ha_l9.1:
	.data
# -- End  __svml_hypotf4_ha_l9
	.text
.L_2__routine_start___svml_hypotf8_ha_e9_2:
# -- Begin  __svml_hypotf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypotf8_ha_e9
# --- __svml_hypotf8_ha_e9(__m256, __m256)
__svml_hypotf8_ha_e9:
# parameter 1: %ymm0
# parameter 2: %ymm1
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #307.1
        .byte     15                                            #398.663
        .byte     30                                            #398.663
        .byte     250                                           #398.663
	.cfi_startproc
..___tag_value___svml_hypotf8_ha_e9.43:
..L44:
                                                         #307.1
        pushq     %rbp                                          #307.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #307.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #307.1
        subq      $256, %rsp                                    #307.1
        vmovups   128+__svml_shypot_ha_data_internal(%rip), %ymm13 #338.53
        vmovups   __svml_shypot_ha_data_internal(%rip), %ymm10  #337.54
        vmovups   320+__svml_shypot_ha_data_internal(%rip), %xmm5 #352.37
        vmovups   192+__svml_shypot_ha_data_internal(%rip), %ymm11 #339.49
        vmovaps   %ymm1, %ymm12                                 #307.1
        vandps    %ymm13, %ymm0, %ymm15                         #341.20
        vandps    %ymm13, %ymm12, %ymm14                        #342.20
        vmaxps    %ymm14, %ymm15, %ymm9                         #343.13
        vminps    %ymm15, %ymm14, %ymm6                         #344.13
        vandps    %ymm10, %ymm9, %ymm7                          #345.105
        vsubps    %ymm7, %ymm9, %ymm8                           #345.153
        vmulps    %ymm7, %ymm7, %ymm15                          #361.15
        vpsubd    %xmm6, %xmm9, %xmm1                           #355.28
        vextractf128 $1, %ymm9, %xmm4                           #353.110
        vpcmpgtd  %xmm5, %xmm1, %xmm3                           #356.28
        vextractf128 $1, %ymm6, %xmm2                           #354.110
        vaddps    %ymm7, %ymm9, %ymm7                           #363.15
        vmovups   384+__svml_shypot_ha_data_internal(%rip), %xmm9 #372.27
        vpsubd    %xmm2, %xmm4, %xmm13                          #355.92
        vpcmpgtd  %xmm5, %xmm13, %xmm5                          #356.111
        vmulps    %ymm7, %ymm8, %ymm8                           #364.15
        vmovups   448+__svml_shypot_ha_data_internal(%rip), %xmm13 #373.28
        vinsertf128 $1, %xmm5, %ymm3, %ymm14                    #357.21
        vandnps   %ymm10, %ymm14, %ymm10                        #358.25
        vandps    %ymm10, %ymm6, %ymm3                          #360.105
        vmulps    %ymm3, %ymm3, %ymm1                           #362.30
        vsubps    %ymm3, %ymm6, %ymm2                           #360.153
        vaddps    %ymm1, %ymm15, %ymm4                          #362.15
        vmulps    %ymm2, %ymm6, %ymm1                           #365.30
        vmulps    %ymm2, %ymm3, %ymm6                           #366.30
        vaddps    %ymm1, %ymm8, %ymm5                           #365.15
        vaddps    %ymm6, %ymm5, %ymm3                           #366.15
        vaddps    %ymm3, %ymm4, %ymm2                           #367.13
        vpcmpgtd  %xmm2, %xmm9, %xmm7                           #375.27
        vextractf128 $1, %ymm2, %xmm10                          #374.106
        vpcmpgtd  %xmm13, %xmm2, %xmm8                          #376.28
        vpor      %xmm8, %xmm7, %xmm1                           #377.20
        vpxor     %xmm7, %xmm7, %xmm7                           #378.62
        vpcmpgtd  %xmm10, %xmm9, %xmm14                         #375.103
        vpcmpgtd  %xmm13, %xmm10, %xmm15                        #376.106
        vrsqrtps  %ymm2, %ymm9                                  #380.14
        vpor      %xmm15, %xmm14, %xmm5                         #377.94
        vpackssdw %xmm5, %xmm1, %xmm6                           #378.79
        vpacksswb %xmm7, %xmm6, %xmm8                           #378.62
        vpmovmskb %xmm8, %edx                                   #378.43
        vmulps    %ymm2, %ymm9, %ymm13                          #381.14
        vmulps    %ymm11, %ymm9, %ymm10                         #382.14
        vmulps    %ymm10, %ymm13, %ymm2                         #383.39
        vsubps    %ymm2, %ymm11, %ymm2                          #383.14
        vmulps    %ymm2, %ymm13, %ymm5                          #385.29
        vmulps    %ymm2, %ymm10, %ymm11                         #384.29
        vaddps    %ymm5, %ymm13, %ymm15                         #385.14
        vaddps    %ymm11, %ymm10, %ymm1                         #384.14
        vandps    64+__svml_shypot_ha_data_internal(%rip), %ymm15, %ymm7 #390.109
        vsubps    %ymm7, %ymm15, %ymm9                          #390.158
        vmulps    %ymm7, %ymm7, %ymm6                           #391.33
        vmulps    %ymm9, %ymm7, %ymm8                           #392.33
        vmulps    %ymm9, %ymm15, %ymm11                         #393.33
        vsubps    %ymm4, %ymm6, %ymm4                           #391.18
        vaddps    %ymm8, %ymm4, %ymm10                          #392.18
        vaddps    %ymm11, %ymm10, %ymm14                        #393.18
        vsubps    %ymm3, %ymm14, %ymm3                          #394.18
        vmulps    %ymm3, %ymm1, %ymm1                           #396.37
        vsubps    %ymm1, %ymm15, %ymm1                          #396.14
        testb     %dl, %dl                                      #398.52
        jne       ..B3.3        # Prob 5%                       #398.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm1 ymm12
..B3.2:                         # Preds ..B3.3 ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        vmovaps   %ymm1, %ymm0                                  #401.12
        movq      %rbp, %rsp                                    #401.12
        popq      %rbp                                          #401.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #401.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm0, 64(%rsp)                               #398.239
        vmovups   %ymm12, 128(%rsp)                             #398.313
        vmovups   %ymm1, 192(%rsp)                              #398.387
        testl     %edx, %edx                                    #398.491
        je        ..B3.2        # Prob 95%                      #398.491
                                # LOE rbx r12 r13 r14 r15 edx ymm1
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #398.571
                                # LOE rbx r12 r13 r14 r15 eax edx
..B3.13:                        # Preds ..B3.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B3.7:                         # Preds ..B3.8 ..B3.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #398.634
        jc        ..B3.10       # Prob 5%                       #398.634
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #398.587
        cmpl      $8, %r12d                                     #398.582
        jl        ..B3.7        # Prob 82%                      #398.582
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   192(%rsp), %ymm1                              #398.806
        jmp       ..B3.2        # Prob 100%                     #398.806
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm1
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #398.663
        lea       128(%rsp,%r12,4), %rsi                        #398.663
        lea       192(%rsp,%r12,4), %rdx                        #398.663
..___tag_value___svml_hypotf8_ha_e9.61:
#       __svml_shypot_ha_cout_rare_internal(const float *, const float *, float *)
        call      __svml_shypot_ha_cout_rare_internal           #398.663
..___tag_value___svml_hypotf8_ha_e9.62:
        jmp       ..B3.8        # Prob 100%                     #398.663
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_hypotf8_ha_e9,@function
	.size	__svml_hypotf8_ha_e9,.-__svml_hypotf8_ha_e9
..LN__svml_hypotf8_ha_e9.2:
	.data
# -- End  __svml_hypotf8_ha_e9
	.text
.L_2__routine_start___svml_hypotf4_ha_ex_3:
# -- Begin  __svml_hypotf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypotf4_ha_ex
# --- __svml_hypotf4_ha_ex(__m128, __m128)
__svml_hypotf4_ha_ex:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1047.1
        .byte     15                                            #1138.654
        .byte     30                                            #1138.654
        .byte     250                                           #1138.654
	.cfi_startproc
..___tag_value___svml_hypotf4_ha_ex.64:
..L65:
                                                         #1047.1
        pushq     %rbp                                          #1047.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1047.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1047.1
        subq      $256, %rsp                                    #1047.1
        movaps    %xmm1, %xmm4                                  #1047.1
        movups    128+__svml_shypot_ha_data_internal(%rip), %xmm10 #1078.50
        movaps    %xmm10, %xmm5                                 #1081.20
        andps     %xmm4, %xmm10                                 #1082.20
        andps     %xmm0, %xmm5                                  #1081.20
        movaps    %xmm5, %xmm9                                  #1083.13
        maxps     %xmm10, %xmm9                                 #1083.13
        minps     %xmm5, %xmm10                                 #1084.13
        movaps    %xmm9, %xmm12                                 #1095.22
        movaps    %xmm9, %xmm3                                  #1085.150
        psubd     %xmm10, %xmm12                                #1095.22
        movaps    %xmm10, %xmm11                                #1100.150
        movups    __svml_shypot_ha_data_internal(%rip), %xmm6   #1077.51
        pcmpgtd   320+__svml_shypot_ha_data_internal(%rip), %xmm12 #1096.22
        movaps    %xmm6, %xmm8                                  #1085.105
        andnps    %xmm6, %xmm12                                 #1098.25
        andps     %xmm9, %xmm8                                  #1085.105
        andps     %xmm10, %xmm12                                #1100.105
        movaps    %xmm8, %xmm2                                  #1101.15
        movaps    %xmm12, %xmm7                                 #1102.27
        subps     %xmm8, %xmm3                                  #1085.150
        mulps     %xmm8, %xmm2                                  #1101.15
        subps     %xmm12, %xmm11                                #1100.150
        addps     %xmm8, %xmm9                                  #1103.15
        mulps     %xmm12, %xmm7                                 #1102.27
        mulps     %xmm11, %xmm10                                #1105.27
        mulps     %xmm9, %xmm3                                  #1104.15
        addps     %xmm7, %xmm2                                  #1102.15
        mulps     %xmm11, %xmm12                                #1106.27
        addps     %xmm10, %xmm3                                 #1105.15
        movaps    %xmm2, %xmm10                                 #1107.13
        addps     %xmm12, %xmm3                                 #1106.15
        movdqu    384+__svml_shypot_ha_data_internal(%rip), %xmm14 #1115.21
        addps     %xmm3, %xmm10                                 #1107.13
        rsqrtps   %xmm10, %xmm9                                 #1120.14
        movups    192+__svml_shypot_ha_data_internal(%rip), %xmm1 #1079.46
        pcmpgtd   %xmm10, %xmm14                                #1115.21
        movaps    %xmm10, %xmm13                                #1116.22
        mulps     %xmm9, %xmm10                                 #1121.14
        mulps     %xmm1, %xmm9                                  #1122.14
        movaps    %xmm10, %xmm15                                #1123.36
        mulps     %xmm9, %xmm15                                 #1123.36
        movups    64+__svml_shypot_ha_data_internal(%rip), %xmm6 #1130.109
        subps     %xmm15, %xmm1                                 #1123.14
        movaps    %xmm1, %xmm5                                  #1124.26
        mulps     %xmm10, %xmm1                                 #1125.26
        mulps     %xmm9, %xmm5                                  #1124.26
        addps     %xmm1, %xmm10                                 #1125.14
        addps     %xmm5, %xmm9                                  #1124.14
        andps     %xmm10, %xmm6                                 #1130.109
        movaps    %xmm10, %xmm7                                 #1130.155
        movaps    %xmm6, %xmm8                                  #1131.30
        subps     %xmm6, %xmm7                                  #1130.155
        mulps     %xmm6, %xmm8                                  #1131.30
        mulps     %xmm7, %xmm6                                  #1132.30
        subps     %xmm2, %xmm8                                  #1131.18
        mulps     %xmm10, %xmm7                                 #1133.30
        addps     %xmm6, %xmm8                                  #1132.18
        pcmpgtd   448+__svml_shypot_ha_data_internal(%rip), %xmm13 #1116.22
        addps     %xmm7, %xmm8                                  #1133.18
        por       %xmm13, %xmm14                                #1117.14
        subps     %xmm3, %xmm8                                  #1134.18
        movmskps  %xmm14, %edx                                  #1118.44
        mulps     %xmm8, %xmm9                                  #1136.34
        subps     %xmm9, %xmm10                                 #1136.14
        testl     %edx, %edx                                    #1138.52
        jne       ..B4.3        # Prob 5%                       #1138.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm4 xmm10
..B4.2:                         # Preds ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        movaps    %xmm10, %xmm0                                 #1141.12
        movq      %rbp, %rsp                                    #1141.12
        popq      %rbp                                          #1141.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1141.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #1138.236
        movups    %xmm4, 128(%rsp)                              #1138.307
        movups    %xmm10, 192(%rsp)                             #1138.378
                                # LOE rbx r12 r13 r14 r15 edx
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1138.562
        movq      %r12, 8(%rsp)                                 #1138.562[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1138.562
        movq      %r13, (%rsp)                                  #1138.562[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1138.562
                                # LOE rbx r12 r14 r15 r13d
..B4.7:                         # Preds ..B4.8 ..B4.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1138.625
        jc        ..B4.10       # Prob 5%                       #1138.625
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1138.578
        cmpl      $4, %r12d                                     #1138.573
        jl        ..B4.7        # Prob 82%                      #1138.573
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm10                             #1138.794
        jmp       ..B4.2        # Prob 100%                     #1138.794
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm10
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1138.654
        lea       128(%rsp,%r12,4), %rsi                        #1138.654
        lea       192(%rsp,%r12,4), %rdx                        #1138.654
..___tag_value___svml_hypotf4_ha_ex.82:
#       __svml_shypot_ha_cout_rare_internal(const float *, const float *, float *)
        call      __svml_shypot_ha_cout_rare_internal           #1138.654
..___tag_value___svml_hypotf4_ha_ex.83:
        jmp       ..B4.8        # Prob 100%                     #1138.654
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_hypotf4_ha_ex,@function
	.size	__svml_hypotf4_ha_ex,.-__svml_hypotf4_ha_ex
..LN__svml_hypotf4_ha_ex.3:
	.data
# -- End  __svml_hypotf4_ha_ex
	.text
.L_2__routine_start___svml_hypotf16_ha_z0_4:
# -- Begin  __svml_hypotf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypotf16_ha_z0
# --- __svml_hypotf16_ha_z0(__m512, __m512)
__svml_hypotf16_ha_z0:
# parameter 1: %zmm0
# parameter 2: %zmm1
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1146.1
        .byte     15                                            #1210.878
        .byte     30                                            #1210.878
        .byte     250                                           #1210.878
	.cfi_startproc
..___tag_value___svml_hypotf16_ha_z0.85:
..L86:
                                                         #1146.1
        pushq     %rbp                                          #1146.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1146.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1146.1
        subq      $256, %rsp                                    #1146.1
        vmovaps   %zmm1, %zmm2                                  #1146.1
        vmulps    {rn-sae}, %zmm0, %zmm0, %zmm7                 #1191.18
        vmulps    {rn-sae}, %zmm2, %zmm2, %zmm8                 #1193.18
        vgetexpps {sae}, %zmm0, %zmm3                           #1182.22
        vgetexpps {sae}, %zmm2, %zmm4                           #1183.22
        vaddps    {rn-sae}, %zmm8, %zmm7, %zmm1                 #1195.17
        vmaxps    {sae}, %zmm8, %zmm7, %zmm9                    #1196.17
        vmaxps    {sae}, %zmm4, %zmm3, %zmm5                    #1184.17
        vrsqrt14ps %zmm1, %zmm4                                 #1202.13
        vminps    {sae}, %zmm8, %zmm7, %zmm10                   #1197.17
        vmovups   192+__svml_shypot_ha_data_internal(%rip), %zmm3 #1204.48
        vandps    128+__svml_shypot_ha_data_internal(%rip), %zmm5, %zmm6 #1186.18
        vsubps    {rn-sae}, %zmm9, %zmm1, %zmm11                #1198.15
        vpcmpd    $5, 512+__svml_shypot_ha_data_internal(%rip), %zmm6, %k0 #1189.20
        vmulps    {rn-sae}, %zmm4, %zmm3, %zmm6                 #1205.23
        vsubps    {rn-sae}, %zmm11, %zmm10, %zmm14              #1199.15
        kmovw     %k0, %esi                                     #1190.41
        vmovaps   %zmm0, %zmm12                                 #1192.16
        vfmsub213ps {rn-sae}, %zmm7, %zmm0, %zmm12              #1192.16
        vmulps    {rn-sae}, %zmm4, %zmm1, %zmm7                 #1203.18
        vmovaps   %zmm2, %zmm13                                 #1194.16
        vfmsub213ps {rn-sae}, %zmm8, %zmm2, %zmm13              #1194.16
        vfnmadd231ps {rn-sae}, %zmm7, %zmm7, %zmm1              #1206.14
        vaddps    {rn-sae}, %zmm13, %zmm12, %zmm15              #1200.17
        vaddps    {rn-sae}, %zmm15, %zmm14, %zmm5               #1201.17
        vaddps    {rn-sae}, %zmm5, %zmm1, %zmm1                 #1207.20
        vfmadd213ps {rn-sae}, %zmm7, %zmm6, %zmm1               #1208.14
        testl     %esi, %esi                                    #1210.52
        jne       ..B5.3        # Prob 5%                       #1210.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm1 zmm2
..B5.2:                         # Preds ..B5.9 ..B5.7 ..B5.1
                                # Execution count [1.00e+00]
        vmovaps   %zmm1, %zmm0                                  #1213.12
        movq      %rbp, %rsp                                    #1213.12
        popq      %rbp                                          #1213.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1213.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #1210.293
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm1 zmm2
..B5.4:                         # Preds ..B5.3
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #1210.293
        movl      %edx, %eax                                    #1210.347
        orl       $8064, %eax                                   #1210.347
        cmpl      %eax, %edx                                    #1210.376
        je        ..B5.6        # Prob 78%                      #1210.376
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm1 zmm2
..B5.5:                         # Preds ..B5.4
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #1210.388
        vldmxcsr  32(%rsp)                                      #1210.388
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm1 zmm2
..B5.6:                         # Preds ..B5.5 ..B5.4
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm0, 64(%rsp)                               #1210.448
        vmovups   %zmm2, 128(%rsp)                              #1210.522
        vmovups   %zmm1, 192(%rsp)                              #1210.596
        testl     %esi, %esi                                    #1210.704
        jne       ..B5.11       # Prob 5%                       #1210.704
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm1
..B5.7:                         # Preds ..B5.14 ..B5.6
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #1210.1064
        je        ..B5.2        # Prob 78%                      #1210.1064
                                # LOE rbx r12 r13 r14 r15 edx zmm1
..B5.8:                         # Preds ..B5.7
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #1210.1089
        movl      32(%rsp), %eax                                #1210.1089
                                # LOE rbx r12 r13 r14 r15 eax edx zmm1
..B5.9:                         # Preds ..B5.8
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #1210.1076
        orl       %edx, %eax                                    #1210.1076
        movl      %eax, 32(%rsp)                                #1210.1076
        vldmxcsr  32(%rsp)                                      #1210.1076
        jmp       ..B5.2        # Prob 100%                     #1210.1076
                                # LOE rbx r12 r13 r14 r15 zmm1
..B5.11:                        # Preds ..B5.6
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #1210.786
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B5.18:                        # Preds ..B5.11
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
..B5.12:                        # Preds ..B5.13 ..B5.18
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #1210.849
        jc        ..B5.15       # Prob 5%                       #1210.849
                                # LOE rbx r15 r12d r13d r14d
..B5.13:                        # Preds ..B5.15 ..B5.12
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #1210.802
        cmpl      $16, %ebx                                     #1210.797
        jl        ..B5.12       # Prob 82%                      #1210.797
                                # LOE rbx r15 r12d r13d r14d
..B5.14:                        # Preds ..B5.13
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
        vmovups   192(%rsp), %zmm1                              #1210.1019
        jmp       ..B5.7        # Prob 100%                     #1210.1019
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x18, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x10, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm1
..B5.15:                        # Preds ..B5.12
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,4), %rdi                         #1210.878
        lea       128(%rsp,%rbx,4), %rsi                        #1210.878
        lea       192(%rsp,%rbx,4), %rdx                        #1210.878
..___tag_value___svml_hypotf16_ha_z0.113:
#       __svml_shypot_ha_cout_rare_internal(const float *, const float *, float *)
        call      __svml_shypot_ha_cout_rare_internal           #1210.878
..___tag_value___svml_hypotf16_ha_z0.114:
        jmp       ..B5.13       # Prob 100%                     #1210.878
        .align    16,0x90
                                # LOE r15 ebx r12d r13d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_hypotf16_ha_z0,@function
	.size	__svml_hypotf16_ha_z0,.-__svml_hypotf16_ha_z0
..LN__svml_hypotf16_ha_z0.4:
	.data
# -- End  __svml_hypotf16_ha_z0
	.text
.L_2__routine_start___svml_hypotf4_ha_e9_5:
# -- Begin  __svml_hypotf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypotf4_ha_e9
# --- __svml_hypotf4_ha_e9(__m128, __m128)
__svml_hypotf4_ha_e9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1218.1
        .byte     15                                            #1309.654
        .byte     30                                            #1309.654
        .byte     250                                           #1309.654
	.cfi_startproc
..___tag_value___svml_hypotf4_ha_e9.116:
..L117:
                                                        #1218.1
        pushq     %rbp                                          #1218.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1218.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1218.1
        subq      $256, %rsp                                    #1218.1
        movaps    %xmm1, %xmm4                                  #1218.1
        movups    128+__svml_shypot_ha_data_internal(%rip), %xmm10 #1249.50
        movaps    %xmm10, %xmm5                                 #1252.20
        andps     %xmm4, %xmm10                                 #1253.20
        andps     %xmm0, %xmm5                                  #1252.20
        movaps    %xmm5, %xmm9                                  #1254.13
        maxps     %xmm10, %xmm9                                 #1254.13
        minps     %xmm5, %xmm10                                 #1255.13
        movaps    %xmm9, %xmm12                                 #1266.28
        movaps    %xmm9, %xmm3                                  #1256.150
        psubd     %xmm10, %xmm12                                #1266.28
        movaps    %xmm10, %xmm11                                #1271.150
        movups    __svml_shypot_ha_data_internal(%rip), %xmm6   #1248.51
        pcmpgtd   320+__svml_shypot_ha_data_internal(%rip), %xmm12 #1267.28
        movaps    %xmm6, %xmm8                                  #1256.105
        andnps    %xmm6, %xmm12                                 #1269.25
        andps     %xmm9, %xmm8                                  #1256.105
        andps     %xmm10, %xmm12                                #1271.105
        movaps    %xmm8, %xmm2                                  #1272.15
        movaps    %xmm12, %xmm7                                 #1273.27
        subps     %xmm8, %xmm3                                  #1256.150
        mulps     %xmm8, %xmm2                                  #1272.15
        subps     %xmm12, %xmm11                                #1271.150
        addps     %xmm8, %xmm9                                  #1274.15
        mulps     %xmm12, %xmm7                                 #1273.27
        mulps     %xmm11, %xmm10                                #1276.27
        mulps     %xmm9, %xmm3                                  #1275.15
        addps     %xmm7, %xmm2                                  #1273.15
        mulps     %xmm11, %xmm12                                #1277.27
        addps     %xmm10, %xmm3                                 #1276.15
        movaps    %xmm2, %xmm10                                 #1278.13
        addps     %xmm12, %xmm3                                 #1277.15
        movups    384+__svml_shypot_ha_data_internal(%rip), %xmm14 #1283.27
        addps     %xmm3, %xmm10                                 #1278.13
        rsqrtps   %xmm10, %xmm9                                 #1291.14
        movups    192+__svml_shypot_ha_data_internal(%rip), %xmm1 #1250.46
        pcmpgtd   %xmm10, %xmm14                                #1286.27
        movaps    %xmm10, %xmm13                                #1287.28
        mulps     %xmm9, %xmm10                                 #1292.14
        mulps     %xmm1, %xmm9                                  #1293.14
        movaps    %xmm10, %xmm15                                #1294.36
        mulps     %xmm9, %xmm15                                 #1294.36
        movups    64+__svml_shypot_ha_data_internal(%rip), %xmm6 #1301.109
        subps     %xmm15, %xmm1                                 #1294.14
        movaps    %xmm1, %xmm5                                  #1295.26
        mulps     %xmm10, %xmm1                                 #1296.26
        mulps     %xmm9, %xmm5                                  #1295.26
        addps     %xmm1, %xmm10                                 #1296.14
        addps     %xmm5, %xmm9                                  #1295.14
        andps     %xmm10, %xmm6                                 #1301.109
        movaps    %xmm10, %xmm7                                 #1301.155
        movaps    %xmm6, %xmm8                                  #1302.30
        subps     %xmm6, %xmm7                                  #1301.155
        mulps     %xmm6, %xmm8                                  #1302.30
        mulps     %xmm7, %xmm6                                  #1303.30
        subps     %xmm2, %xmm8                                  #1302.18
        mulps     %xmm10, %xmm7                                 #1304.30
        addps     %xmm6, %xmm8                                  #1303.18
        pcmpgtd   448+__svml_shypot_ha_data_internal(%rip), %xmm13 #1287.28
        addps     %xmm7, %xmm8                                  #1304.18
        por       %xmm13, %xmm14                                #1288.20
        subps     %xmm3, %xmm8                                  #1305.18
        movmskps  %xmm14, %edx                                  #1289.44
        mulps     %xmm8, %xmm9                                  #1307.34
        subps     %xmm9, %xmm10                                 #1307.14
        testl     %edx, %edx                                    #1309.52
        jne       ..B6.3        # Prob 5%                       #1309.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm4 xmm10
..B6.2:                         # Preds ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movaps    %xmm10, %xmm0                                 #1312.12
        movq      %rbp, %rsp                                    #1312.12
        popq      %rbp                                          #1312.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1312.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #1309.236
        movups    %xmm4, 128(%rsp)                              #1309.307
        movups    %xmm10, 192(%rsp)                             #1309.378
                                # LOE rbx r12 r13 r14 r15 edx
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1309.562
        movq      %r12, 8(%rsp)                                 #1309.562[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1309.562
        movq      %r13, (%rsp)                                  #1309.562[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1309.562
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1309.625
        jc        ..B6.10       # Prob 5%                       #1309.625
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1309.578
        cmpl      $4, %r12d                                     #1309.573
        jl        ..B6.7        # Prob 82%                      #1309.573
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm10                             #1309.794
        jmp       ..B6.2        # Prob 100%                     #1309.794
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm10
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1309.654
        lea       128(%rsp,%r12,4), %rsi                        #1309.654
        lea       192(%rsp,%r12,4), %rdx                        #1309.654
..___tag_value___svml_hypotf4_ha_e9.134:
#       __svml_shypot_ha_cout_rare_internal(const float *, const float *, float *)
        call      __svml_shypot_ha_cout_rare_internal           #1309.654
..___tag_value___svml_hypotf4_ha_e9.135:
        jmp       ..B6.8        # Prob 100%                     #1309.654
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_hypotf4_ha_e9,@function
	.size	__svml_hypotf4_ha_e9,.-__svml_hypotf4_ha_e9
..LN__svml_hypotf4_ha_e9.5:
	.data
# -- End  __svml_hypotf4_ha_e9
	.text
.L_2__routine_start___svml_shypot_ha_cout_rare_internal_6:
# -- Begin  __svml_shypot_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_shypot_ha_cout_rare_internal
	.globl __svml_shypot_ha_cout_rare_internal
# --- __svml_shypot_ha_cout_rare_internal(const float *, const float *, float *)
__svml_shypot_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
# parameter 3: %rdx
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #924.1
        .byte     15                                            #1027.25
        .byte     30                                            #1027.25
        .byte     250                                           #1027.25
	.cfi_startproc
..___tag_value___svml_shypot_ha_cout_rare_internal.137:
..L138:
                                                        #924.1
        movq      %rdx, %r8                                     #924.1
        movzwl    2(%rdi), %eax                                 #941.37
        andl      $32640, %eax                                  #941.37
        cmpl      $32640, %eax                                  #941.63
        je        ..B7.14       # Prob 16%                      #941.63
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15
..B7.2:                         # Preds ..B7.1
                                # Execution count [8.40e-01]
        movzwl    2(%rsi), %eax                                 #941.96
        andl      $32640, %eax                                  #941.96
        cmpl      $32640, %eax                                  #941.122
        je        ..B7.13       # Prob 16%                      #941.122
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15
..B7.3:                         # Preds ..B7.2
                                # Execution count [7.06e-01]
        pxor      %xmm2, %xmm2                                  #943.18
        pxor      %xmm1, %xmm1                                  #946.23
        cvtss2sd  (%rdi), %xmm2                                 #943.18
        cvtss2sd  (%rsi), %xmm1                                 #946.23
        movsd     4096+_vmlsHypotHATab(%rip), %xmm0             #943.44
        movzwl    4102+_vmlsHypotHATab(%rip), %edi              #943.44
        ucomisd   %xmm0, %xmm2                                  #943.44
        jp        ..B7.4        # Prob 0%                       #943.44
        je        ..B7.11       # Prob 16%                      #943.44
                                # LOE rbx rbp r8 r12 r13 r14 r15 edi xmm0 xmm1 xmm2
..B7.4:                         # Preds ..B7.3 ..B7.11
                                # Execution count [6.88e-01]
        movsd     %xmm2, -16(%rsp)                              #945.17
        movsd     %xmm1, -48(%rsp)                              #946.17
        andb      $127, -9(%rsp)                                #947.38
        andb      $127, -41(%rsp)                               #948.38
        movsd     -16(%rsp), %xmm8                              #949.20
        movsd     -48(%rsp), %xmm1                              #949.25
        comisd    %xmm8, %xmm1                                  #949.25
        jbe       ..B7.6        # Prob 50%                      #949.25
                                # LOE rbx rbp r8 r12 r13 r14 r15 edi xmm0 xmm1 xmm8
..B7.5:                         # Preds ..B7.4
                                # Execution count [3.44e-01]
        movaps    %xmm8, %xmm2                                  #951.21
        movaps    %xmm1, %xmm8                                  #952.21
        movsd     %xmm1, -16(%rsp)                              #952.21
        movaps    %xmm2, %xmm1                                  #953.21
                                # LOE rbx rbp r8 r12 r13 r14 r15 edi xmm0 xmm1 xmm8
..B7.6:                         # Preds ..B7.5 ..B7.4
                                # Execution count [6.88e-01]
        movzwl    -10(%rsp), %edx                               #955.56
        andl      $-32753, %edi                                 #958.38
        andl      $32752, %edx                                  #955.56
        shrl      $4, %edx                                      #955.56
        negl      %edx                                          #955.56
        movsd     %xmm0, -56(%rsp)                              #957.17
        movsd     4128+_vmlsHypotHATab(%rip), %xmm3             #961.56
        lea       1025(%rdx), %esi                              #955.56
        negl      %esi                                          #956.17
        addl      $1000, %esi                                   #956.17
        shrl      $31, %esi                                     #956.17
        imull     $-23, %esi, %eax                              #956.45
        lea       1025(%rax,%rdx), %esi                         #956.17
        lea       1023(%rsi), %ecx                              #958.77
        andl      $2047, %ecx                                   #958.38
        shll      $4, %ecx                                      #958.38
        orl       %ecx, %edi                                    #958.38
        movw      %di, -50(%rsp)                                #958.38
        movsd     -56(%rsp), %xmm2                              #959.23
        mulsd     %xmm2, %xmm8                                  #959.17
        mulsd     %xmm2, %xmm1                                  #960.17
        mulsd     %xmm8, %xmm3                                  #961.56
        movsd     %xmm3, -72(%rsp)                              #961.17
        movsd     -72(%rsp), %xmm4                              #961.96
        movsd     %xmm8, -16(%rsp)                              #959.17
        subsd     %xmm8, %xmm4                                  #961.101
        movsd     %xmm4, -64(%rsp)                              #961.82
        movsd     -72(%rsp), %xmm6                              #961.123
        movsd     -64(%rsp), %xmm5                              #961.128
        movsd     %xmm1, -48(%rsp)                              #960.17
        subsd     %xmm5, %xmm6                                  #961.128
        movsd     %xmm6, -72(%rsp)                              #961.109
        movsd     -72(%rsp), %xmm7                              #961.155
        movzwl    -10(%rsp), %r9d                               #962.41
        subsd     %xmm7, %xmm8                                  #961.155
        movzwl    -42(%rsp), %edi                               #962.82
        andl      $32752, %r9d                                  #962.41
        andl      $32752, %edi                                  #962.82
        shrl      $4, %r9d                                      #962.41
        shrl      $4, %edi                                      #962.82
        movsd     %xmm8, -64(%rsp)                              #961.134
        subl      %edi, %r9d                                    #962.41
        movsd     -72(%rsp), %xmm7                              #961.168
        movsd     -64(%rsp), %xmm8                              #961.179
        cmpl      $6, %r9d                                      #962.104
        jle       ..B7.8        # Prob 50%                      #962.104
                                # LOE rbx rbp r8 r12 r13 r14 r15 esi xmm0 xmm1 xmm7 xmm8
..B7.7:                         # Preds ..B7.6
                                # Execution count [3.44e-01]
        movaps    %xmm1, %xmm2                                  #965.21
        jmp       ..B7.9        # Prob 100%                     #965.21
                                # LOE rbx rbp r8 r12 r13 r14 r15 esi xmm0 xmm1 xmm2 xmm7 xmm8
..B7.8:                         # Preds ..B7.6
                                # Execution count [3.44e-01]
        movsd     -48(%rsp), %xmm1                              #969.35
        movsd     4128+_vmlsHypotHATab(%rip), %xmm0             #969.60
        movaps    %xmm1, %xmm6                                  #969.159
        mulsd     %xmm1, %xmm0                                  #969.60
        movsd     %xmm0, -72(%rsp)                              #969.21
        movsd     -72(%rsp), %xmm2                              #969.100
        subsd     -48(%rsp), %xmm2                              #969.105
        movsd     %xmm2, -64(%rsp)                              #969.86
        movsd     -72(%rsp), %xmm4                              #969.127
        movsd     -64(%rsp), %xmm3                              #969.132
        subsd     %xmm3, %xmm4                                  #969.132
        movsd     %xmm4, -72(%rsp)                              #969.113
        movsd     -72(%rsp), %xmm5                              #969.159
        subsd     %xmm5, %xmm6                                  #969.159
        movsd     %xmm6, -64(%rsp)                              #969.138
        movsd     -72(%rsp), %xmm0                              #969.172
        movsd     -64(%rsp), %xmm2                              #969.183
                                # LOE rbx rbp r8 r12 r13 r14 r15 esi xmm0 xmm1 xmm2 xmm7 xmm8
..B7.9:                         # Preds ..B7.7 ..B7.8
                                # Execution count [6.88e-01]
        movsd     -16(%rsp), %xmm5                              #972.29
        movaps    %xmm0, %xmm4                                  #971.40
        mulsd     %xmm0, %xmm4                                  #971.40
        addsd     %xmm1, %xmm0                                  #972.55
        addsd     %xmm7, %xmm5                                  #972.32
        mulsd     %xmm2, %xmm0                                  #972.55
        mulsd     %xmm5, %xmm8                                  #972.32
        movaps    %xmm7, %xmm3                                  #971.28
        negl      %esi                                          #985.77
        mulsd     %xmm7, %xmm3                                  #971.28
        addsd     %xmm8, %xmm0                                  #972.55
        movq      4112+_vmlsHypotHATab(%rip), %r11              #980.44
        movq      %r11, %r9                                     #981.38
        lea       _vmlsHypotHATab(%rip), %rdx                   #979.50
        addsd     %xmm4, %xmm3                                  #971.40
        addl      $1023, %esi                                   #985.77
        addsd     %xmm0, %xmm3                                  #973.31
        movsd     %xmm3, -56(%rsp)                              #973.17
        andl      $2047, %esi                                   #985.38
        movzwl    -50(%rsp), %ecx                               #974.42
        andl      $32752, %ecx                                  #974.42
        shrl      $4, %ecx                                      #974.42
        addl      $-1023, %ecx                                  #974.67
        movl      %ecx, %eax                                    #977.27
        andl      $1, %eax                                      #977.27
        subl      %eax, %ecx                                    #978.23
        shrl      $1, %ecx                                      #978.33
        movsd     %xmm3, -48(%rsp)                              #975.17
        movzwl    -42(%rsp), %edi                               #976.38
        andl      $-32753, %edi                                 #976.38
        shrq      $48, %r9                                      #981.38
        lea       1023(%rcx), %r10d                             #981.68
        addl      %ecx, %ecx                                    #983.69
        addl      $16368, %edi                                  #976.38
        negl      %ecx                                          #983.69
        andl      $2047, %r10d                                  #981.38
        addl      $1023, %ecx                                   #983.73
        andl      $2047, %ecx                                   #983.38
        andl      $-32753, %r9d                                 #981.38
        movw      %di, -42(%rsp)                                #976.38
        shll      $4, %r10d                                     #981.38
        shll      $4, %ecx                                      #983.38
        orl       %r9d, %r10d                                   #981.38
        shll      $4, %esi                                      #985.38
        orl       %r9d, %ecx                                    #983.38
        movsd     -48(%rsp), %xmm2                              #979.22
        orl       %esi, %r9d                                    #985.38
        movl      -44(%rsp), %esi                               #986.42
        mulsd     4112(%rdx,%rax,8), %xmm2                      #979.50
        andl      $1048575, %esi                                #986.42
        shrl      $12, %esi                                     #986.77
        shll      $8, %eax                                      #987.31
        addl      %eax, %esi                                    #987.31
        movsd     (%rdx,%rsi,8), %xmm6                          #988.79
        movsd     4104+_vmlsHypotHATab(%rip), %xmm1             #988.79
        mulsd     %xmm6, %xmm2                                  #989.42
        mulsd     %xmm6, %xmm1                                  #988.79
        movaps    %xmm2, %xmm7                                  #990.66
        mulsd     %xmm1, %xmm7                                  #990.66
        movsd     4104+_vmlsHypotHATab(%rip), %xmm9             #990.66
        movsd     4104+_vmlsHypotHATab(%rip), %xmm12            #993.66
        subsd     %xmm7, %xmm9                                  #990.66
        movaps    %xmm9, %xmm8                                  #991.29
        mulsd     %xmm2, %xmm9                                  #992.29
        mulsd     %xmm1, %xmm8                                  #991.29
        addsd     %xmm9, %xmm2                                  #992.29
        addsd     %xmm8, %xmm1                                  #991.29
        movaps    %xmm2, %xmm10                                 #993.66
        movaps    %xmm1, %xmm11                                 #994.29
        mulsd     %xmm1, %xmm10                                 #993.66
        movsd     4104+_vmlsHypotHATab(%rip), %xmm0             #996.66
        subsd     %xmm10, %xmm12                                #993.66
        mulsd     %xmm12, %xmm11                                #994.29
        mulsd     %xmm2, %xmm12                                 #995.29
        addsd     %xmm11, %xmm1                                 #994.29
        addsd     %xmm12, %xmm2                                 #995.29
        movaps    %xmm2, %xmm13                                 #996.66
        movaps    %xmm2, %xmm14                                 #998.29
        mulsd     %xmm1, %xmm13                                 #996.66
        movsd     4128+_vmlsHypotHATab(%rip), %xmm15            #999.55
        subsd     %xmm13, %xmm0                                 #996.66
        mulsd     %xmm0, %xmm14                                 #998.29
        mulsd     %xmm1, %xmm0                                  #997.29
        addsd     %xmm14, %xmm2                                 #998.29
        addsd     %xmm0, %xmm1                                  #997.29
        mulsd     %xmm2, %xmm15                                 #999.55
        movsd     %xmm15, -72(%rsp)                             #999.17
        movaps    %xmm2, %xmm8                                  #999.152
        movsd     -72(%rsp), %xmm4                              #999.95
        movsd     4120+_vmlsHypotHATab(%rip), %xmm10            #1001.39
        subsd     %xmm2, %xmm4                                  #999.100
        movsd     %xmm4, -64(%rsp)                              #999.81
        movsd     -72(%rsp), %xmm6                              #999.121
        movsd     -64(%rsp), %xmm5                              #999.126
        movq      %r11, -32(%rsp)                               #982.17
        subsd     %xmm5, %xmm6                                  #999.126
        movsd     %xmm6, -72(%rsp)                              #999.107
        movsd     -72(%rsp), %xmm7                              #999.152
        movw      %cx, -26(%rsp)                                #983.38
        subsd     %xmm7, %xmm8                                  #999.152
        movsd     %xmm8, -64(%rsp)                              #999.132
        movsd     -72(%rsp), %xmm11                             #999.164
        movsd     -64(%rsp), %xmm12                             #999.174
        movaps    %xmm11, %xmm13                                #1002.17
        mulsd     %xmm12, %xmm10                                #1001.73
        mulsd     %xmm12, %xmm12                                #1002.28
        xorps     .L_2il0floatpacket.50(%rip), %xmm13           #1002.17
        xorps     .L_2il0floatpacket.50(%rip), %xmm12           #1002.17
        subsd     %xmm10, %xmm13                                #1002.17
        mulsd     %xmm11, %xmm13                                #1002.17
        movsd     -32(%rsp), %xmm9                              #1000.28
        addsd     %xmm12, %xmm13                                #1002.17
        mulsd     %xmm9, %xmm3                                  #1003.17
        movq      %r11, -40(%rsp)                               #980.17
        addsd     %xmm13, %xmm3                                 #1003.17
        mulsd     %xmm1, %xmm3                                  #1004.29
        movw      %r10w, -34(%rsp)                              #981.38
        addsd     %xmm3, %xmm2                                  #1004.29
        mulsd     -40(%rsp), %xmm2                              #1005.28
        movq      %r11, -24(%rsp)                               #984.17
        movw      %r9w, -18(%rsp)                               #985.38
        mulsd     -24(%rsp), %xmm2                              #1005.38
        cvtsd2ss  %xmm2, %xmm2                                  #1005.17
        movss     %xmm2, (%r8)                                  #1005.17
                                # LOE rbx rbp r12 r13 r14 r15
..B7.10:                        # Preds ..B7.9 ..B7.12 ..B7.13 ..B7.15 ..B7.18
                                #
                                # Execution count [1.00e+00]
        xorl      %eax, %eax                                    #1042.12
        ret                                                     #1042.12
                                # LOE
..B7.11:                        # Preds ..B7.3
                                # Execution count [1.13e-01]: Infreq
        ucomisd   %xmm0, %xmm1                                  #943.96
        jne       ..B7.4        # Prob 84%                      #943.96
        jp        ..B7.4        # Prob 0%                       #943.96
                                # LOE rbx rbp r8 r12 r13 r14 r15 edi xmm0 xmm1 xmm2
..B7.12:                        # Preds ..B7.11
                                # Execution count [1.81e-02]: Infreq
        cvtsd2ss  %xmm0, %xmm0                                  #1009.17
        movss     %xmm0, (%r8)                                  #1009.17
        jmp       ..B7.10       # Prob 100%                     #1009.17
                                # LOE rbx rbp r12 r13 r14 r15
..B7.13:                        # Preds ..B7.17 ..B7.2
                                # Execution count [2.47e-01]: Infreq
        movss     (%rsi), %xmm0                                 #1016.25
        mulss     %xmm0, %xmm0                                  #1016.34
        movss     %xmm0, (%r8)                                  #1016.17
        jmp       ..B7.10       # Prob 100%                     #1016.17
                                # LOE rbx rbp r12 r13 r14 r15
..B7.14:                        # Preds ..B7.1
                                # Execution count [4.71e-02]: Infreq
        movzwl    2(%rsi), %eax                                 #1020.44
        andl      $32640, %eax                                  #1020.44
        cmpl      $32640, %eax                                  #1020.70
        je        ..B7.16       # Prob 16%                      #1020.70
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15
..B7.15:                        # Preds ..B7.16 ..B7.14
                                # Execution count [3.96e-02]: Infreq
        movss     (%rdi), %xmm0                                 #1022.29
        mulss     %xmm0, %xmm0                                  #1022.38
        movss     %xmm0, (%r8)                                  #1022.21
        jmp       ..B7.10       # Prob 100%                     #1022.21
                                # LOE rbx rbp r12 r13 r14 r15
..B7.16:                        # Preds ..B7.14
                                # Execution count [7.54e-03]: Infreq
        testl     $8388607, (%rdi)                              #1025.49
        je        ..B7.15       # Prob 50%                      #1025.78
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15
..B7.17:                        # Preds ..B7.16
                                # Execution count [3.77e-03]: Infreq
        testl     $8388607, (%rsi)                              #1025.108
        je        ..B7.13       # Prob 50%                      #1025.137
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15
..B7.18:                        # Preds ..B7.17
                                # Execution count [1.88e-03]: Infreq
        movss     (%rdi), %xmm0                                 #1027.33
        mulss     (%rsi), %xmm0                                 #1027.42
        movss     %xmm0, (%r8)                                  #1027.25
        jmp       ..B7.10       # Prob 100%                     #1027.25
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_shypot_ha_cout_rare_internal,@function
	.size	__svml_shypot_ha_cout_rare_internal,.-__svml_shypot_ha_cout_rare_internal
..LN__svml_shypot_ha_cout_rare_internal.6:
	.data
# -- End  __svml_shypot_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_shypot_ha_data_internal
	.globl __svml_shypot_ha_data_internal
__svml_shypot_ha_data_internal:
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
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
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
	.long	33554432
	.long	33554432
	.long	33554432
	.long	33554432
	.long	33554432
	.long	33554432
	.long	33554432
	.long	33554432
	.long	33554432
	.long	33554432
	.long	33554432
	.long	33554432
	.long	33554432
	.long	33554432
	.long	33554432
	.long	33554432
	.long	506462208
	.long	506462208
	.long	506462208
	.long	506462208
	.long	506462208
	.long	506462208
	.long	506462208
	.long	506462208
	.long	506462208
	.long	506462208
	.long	506462208
	.long	506462208
	.long	506462208
	.long	506462208
	.long	506462208
	.long	506462208
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1621098496
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.long	1115422720
	.type	__svml_shypot_ha_data_internal,@object
	.size	__svml_shypot_ha_data_internal,576
	.align 32
_vmlsHypotHATab:
	.long	0
	.long	1072693248
	.long	0
	.long	1072689152
	.long	0
	.long	1072685056
	.long	0
	.long	1072680960
	.long	0
	.long	1072676864
	.long	0
	.long	1072672768
	.long	0
	.long	1072668672
	.long	0
	.long	1072665600
	.long	0
	.long	1072661504
	.long	0
	.long	1072657408
	.long	0
	.long	1072653312
	.long	0
	.long	1072649216
	.long	0
	.long	1072646144
	.long	0
	.long	1072642048
	.long	0
	.long	1072637952
	.long	0
	.long	1072634880
	.long	0
	.long	1072630784
	.long	0
	.long	1072626688
	.long	0
	.long	1072623616
	.long	0
	.long	1072619520
	.long	0
	.long	1072615424
	.long	0
	.long	1072612352
	.long	0
	.long	1072608256
	.long	0
	.long	1072605184
	.long	0
	.long	1072601088
	.long	0
	.long	1072598016
	.long	0
	.long	1072593920
	.long	0
	.long	1072590848
	.long	0
	.long	1072586752
	.long	0
	.long	1072583680
	.long	0
	.long	1072580608
	.long	0
	.long	1072576512
	.long	0
	.long	1072573440
	.long	0
	.long	1072570368
	.long	0
	.long	1072566272
	.long	0
	.long	1072563200
	.long	0
	.long	1072560128
	.long	0
	.long	1072556032
	.long	0
	.long	1072552960
	.long	0
	.long	1072549888
	.long	0
	.long	1072546816
	.long	0
	.long	1072542720
	.long	0
	.long	1072539648
	.long	0
	.long	1072536576
	.long	0
	.long	1072533504
	.long	0
	.long	1072530432
	.long	0
	.long	1072527360
	.long	0
	.long	1072523264
	.long	0
	.long	1072520192
	.long	0
	.long	1072517120
	.long	0
	.long	1072514048
	.long	0
	.long	1072510976
	.long	0
	.long	1072507904
	.long	0
	.long	1072504832
	.long	0
	.long	1072501760
	.long	0
	.long	1072498688
	.long	0
	.long	1072495616
	.long	0
	.long	1072492544
	.long	0
	.long	1072489472
	.long	0
	.long	1072486400
	.long	0
	.long	1072483328
	.long	0
	.long	1072480256
	.long	0
	.long	1072478208
	.long	0
	.long	1072475136
	.long	0
	.long	1072472064
	.long	0
	.long	1072468992
	.long	0
	.long	1072465920
	.long	0
	.long	1072462848
	.long	0
	.long	1072459776
	.long	0
	.long	1072457728
	.long	0
	.long	1072454656
	.long	0
	.long	1072451584
	.long	0
	.long	1072448512
	.long	0
	.long	1072446464
	.long	0
	.long	1072443392
	.long	0
	.long	1072440320
	.long	0
	.long	1072437248
	.long	0
	.long	1072435200
	.long	0
	.long	1072432128
	.long	0
	.long	1072429056
	.long	0
	.long	1072427008
	.long	0
	.long	1072423936
	.long	0
	.long	1072420864
	.long	0
	.long	1072418816
	.long	0
	.long	1072415744
	.long	0
	.long	1072412672
	.long	0
	.long	1072410624
	.long	0
	.long	1072407552
	.long	0
	.long	1072405504
	.long	0
	.long	1072402432
	.long	0
	.long	1072400384
	.long	0
	.long	1072397312
	.long	0
	.long	1072395264
	.long	0
	.long	1072392192
	.long	0
	.long	1072390144
	.long	0
	.long	1072387072
	.long	0
	.long	1072385024
	.long	0
	.long	1072381952
	.long	0
	.long	1072379904
	.long	0
	.long	1072376832
	.long	0
	.long	1072374784
	.long	0
	.long	1072371712
	.long	0
	.long	1072369664
	.long	0
	.long	1072366592
	.long	0
	.long	1072364544
	.long	0
	.long	1072362496
	.long	0
	.long	1072359424
	.long	0
	.long	1072357376
	.long	0
	.long	1072355328
	.long	0
	.long	1072352256
	.long	0
	.long	1072350208
	.long	0
	.long	1072347136
	.long	0
	.long	1072345088
	.long	0
	.long	1072343040
	.long	0
	.long	1072340992
	.long	0
	.long	1072337920
	.long	0
	.long	1072335872
	.long	0
	.long	1072333824
	.long	0
	.long	1072330752
	.long	0
	.long	1072328704
	.long	0
	.long	1072326656
	.long	0
	.long	1072324608
	.long	0
	.long	1072321536
	.long	0
	.long	1072319488
	.long	0
	.long	1072317440
	.long	0
	.long	1072315392
	.long	0
	.long	1072313344
	.long	0
	.long	1072310272
	.long	0
	.long	1072308224
	.long	0
	.long	1072306176
	.long	0
	.long	1072304128
	.long	0
	.long	1072302080
	.long	0
	.long	1072300032
	.long	0
	.long	1072296960
	.long	0
	.long	1072294912
	.long	0
	.long	1072292864
	.long	0
	.long	1072290816
	.long	0
	.long	1072288768
	.long	0
	.long	1072286720
	.long	0
	.long	1072284672
	.long	0
	.long	1072282624
	.long	0
	.long	1072280576
	.long	0
	.long	1072278528
	.long	0
	.long	1072275456
	.long	0
	.long	1072273408
	.long	0
	.long	1072271360
	.long	0
	.long	1072269312
	.long	0
	.long	1072267264
	.long	0
	.long	1072265216
	.long	0
	.long	1072263168
	.long	0
	.long	1072261120
	.long	0
	.long	1072259072
	.long	0
	.long	1072257024
	.long	0
	.long	1072254976
	.long	0
	.long	1072252928
	.long	0
	.long	1072250880
	.long	0
	.long	1072248832
	.long	0
	.long	1072246784
	.long	0
	.long	1072244736
	.long	0
	.long	1072243712
	.long	0
	.long	1072241664
	.long	0
	.long	1072239616
	.long	0
	.long	1072237568
	.long	0
	.long	1072235520
	.long	0
	.long	1072233472
	.long	0
	.long	1072231424
	.long	0
	.long	1072229376
	.long	0
	.long	1072227328
	.long	0
	.long	1072225280
	.long	0
	.long	1072223232
	.long	0
	.long	1072222208
	.long	0
	.long	1072220160
	.long	0
	.long	1072218112
	.long	0
	.long	1072216064
	.long	0
	.long	1072214016
	.long	0
	.long	1072211968
	.long	0
	.long	1072210944
	.long	0
	.long	1072208896
	.long	0
	.long	1072206848
	.long	0
	.long	1072204800
	.long	0
	.long	1072202752
	.long	0
	.long	1072201728
	.long	0
	.long	1072199680
	.long	0
	.long	1072197632
	.long	0
	.long	1072195584
	.long	0
	.long	1072193536
	.long	0
	.long	1072192512
	.long	0
	.long	1072190464
	.long	0
	.long	1072188416
	.long	0
	.long	1072186368
	.long	0
	.long	1072185344
	.long	0
	.long	1072183296
	.long	0
	.long	1072181248
	.long	0
	.long	1072179200
	.long	0
	.long	1072178176
	.long	0
	.long	1072176128
	.long	0
	.long	1072174080
	.long	0
	.long	1072173056
	.long	0
	.long	1072171008
	.long	0
	.long	1072168960
	.long	0
	.long	1072167936
	.long	0
	.long	1072165888
	.long	0
	.long	1072163840
	.long	0
	.long	1072161792
	.long	0
	.long	1072160768
	.long	0
	.long	1072158720
	.long	0
	.long	1072157696
	.long	0
	.long	1072155648
	.long	0
	.long	1072153600
	.long	0
	.long	1072152576
	.long	0
	.long	1072150528
	.long	0
	.long	1072148480
	.long	0
	.long	1072147456
	.long	0
	.long	1072145408
	.long	0
	.long	1072143360
	.long	0
	.long	1072142336
	.long	0
	.long	1072140288
	.long	0
	.long	1072139264
	.long	0
	.long	1072137216
	.long	0
	.long	1072135168
	.long	0
	.long	1072134144
	.long	0
	.long	1072132096
	.long	0
	.long	1072131072
	.long	0
	.long	1072129024
	.long	0
	.long	1072128000
	.long	0
	.long	1072125952
	.long	0
	.long	1072124928
	.long	0
	.long	1072122880
	.long	0
	.long	1072120832
	.long	0
	.long	1072119808
	.long	0
	.long	1072117760
	.long	0
	.long	1072116736
	.long	0
	.long	1072114688
	.long	0
	.long	1072113664
	.long	0
	.long	1072111616
	.long	0
	.long	1072110592
	.long	0
	.long	1072108544
	.long	0
	.long	1072107520
	.long	0
	.long	1072105472
	.long	0
	.long	1072104448
	.long	0
	.long	1072102400
	.long	0
	.long	1072101376
	.long	0
	.long	1072099328
	.long	0
	.long	1072098304
	.long	0
	.long	1072096256
	.long	0
	.long	1072095232
	.long	0
	.long	1072094208
	.long	0
	.long	1072092160
	.long	0
	.long	1072091136
	.long	0
	.long	1072089088
	.long	0
	.long	1072088064
	.long	0
	.long	1072086016
	.long	0
	.long	1072084992
	.long	0
	.long	1072082944
	.long	0
	.long	1072081920
	.long	0
	.long	1072080896
	.long	0
	.long	1072078848
	.long	0
	.long	1072075776
	.long	0
	.long	1072073728
	.long	0
	.long	1072070656
	.long	0
	.long	1072067584
	.long	0
	.long	1072064512
	.long	0
	.long	1072061440
	.long	0
	.long	1072059392
	.long	0
	.long	1072056320
	.long	0
	.long	1072053248
	.long	0
	.long	1072051200
	.long	0
	.long	1072048128
	.long	0
	.long	1072045056
	.long	0
	.long	1072043008
	.long	0
	.long	1072039936
	.long	0
	.long	1072037888
	.long	0
	.long	1072034816
	.long	0
	.long	1072031744
	.long	0
	.long	1072029696
	.long	0
	.long	1072026624
	.long	0
	.long	1072024576
	.long	0
	.long	1072021504
	.long	0
	.long	1072019456
	.long	0
	.long	1072016384
	.long	0
	.long	1072014336
	.long	0
	.long	1072011264
	.long	0
	.long	1072009216
	.long	0
	.long	1072006144
	.long	0
	.long	1072004096
	.long	0
	.long	1072002048
	.long	0
	.long	1071998976
	.long	0
	.long	1071996928
	.long	0
	.long	1071993856
	.long	0
	.long	1071991808
	.long	0
	.long	1071989760
	.long	0
	.long	1071986688
	.long	0
	.long	1071984640
	.long	0
	.long	1071982592
	.long	0
	.long	1071979520
	.long	0
	.long	1071977472
	.long	0
	.long	1071975424
	.long	0
	.long	1071972352
	.long	0
	.long	1071970304
	.long	0
	.long	1071968256
	.long	0
	.long	1071966208
	.long	0
	.long	1071964160
	.long	0
	.long	1071961088
	.long	0
	.long	1071959040
	.long	0
	.long	1071956992
	.long	0
	.long	1071954944
	.long	0
	.long	1071952896
	.long	0
	.long	1071949824
	.long	0
	.long	1071947776
	.long	0
	.long	1071945728
	.long	0
	.long	1071943680
	.long	0
	.long	1071941632
	.long	0
	.long	1071939584
	.long	0
	.long	1071937536
	.long	0
	.long	1071935488
	.long	0
	.long	1071933440
	.long	0
	.long	1071930368
	.long	0
	.long	1071928320
	.long	0
	.long	1071926272
	.long	0
	.long	1071924224
	.long	0
	.long	1071922176
	.long	0
	.long	1071920128
	.long	0
	.long	1071918080
	.long	0
	.long	1071916032
	.long	0
	.long	1071913984
	.long	0
	.long	1071911936
	.long	0
	.long	1071909888
	.long	0
	.long	1071907840
	.long	0
	.long	1071905792
	.long	0
	.long	1071903744
	.long	0
	.long	1071901696
	.long	0
	.long	1071900672
	.long	0
	.long	1071898624
	.long	0
	.long	1071896576
	.long	0
	.long	1071894528
	.long	0
	.long	1071892480
	.long	0
	.long	1071890432
	.long	0
	.long	1071888384
	.long	0
	.long	1071886336
	.long	0
	.long	1071884288
	.long	0
	.long	1071883264
	.long	0
	.long	1071881216
	.long	0
	.long	1071879168
	.long	0
	.long	1071877120
	.long	0
	.long	1071875072
	.long	0
	.long	1071873024
	.long	0
	.long	1071872000
	.long	0
	.long	1071869952
	.long	0
	.long	1071867904
	.long	0
	.long	1071865856
	.long	0
	.long	1071864832
	.long	0
	.long	1071862784
	.long	0
	.long	1071860736
	.long	0
	.long	1071858688
	.long	0
	.long	1071856640
	.long	0
	.long	1071855616
	.long	0
	.long	1071853568
	.long	0
	.long	1071851520
	.long	0
	.long	1071850496
	.long	0
	.long	1071848448
	.long	0
	.long	1071846400
	.long	0
	.long	1071844352
	.long	0
	.long	1071843328
	.long	0
	.long	1071841280
	.long	0
	.long	1071839232
	.long	0
	.long	1071838208
	.long	0
	.long	1071836160
	.long	0
	.long	1071834112
	.long	0
	.long	1071833088
	.long	0
	.long	1071831040
	.long	0
	.long	1071830016
	.long	0
	.long	1071827968
	.long	0
	.long	1071825920
	.long	0
	.long	1071824896
	.long	0
	.long	1071822848
	.long	0
	.long	1071821824
	.long	0
	.long	1071819776
	.long	0
	.long	1071817728
	.long	0
	.long	1071816704
	.long	0
	.long	1071814656
	.long	0
	.long	1071813632
	.long	0
	.long	1071811584
	.long	0
	.long	1071810560
	.long	0
	.long	1071808512
	.long	0
	.long	1071806464
	.long	0
	.long	1071805440
	.long	0
	.long	1071803392
	.long	0
	.long	1071802368
	.long	0
	.long	1071800320
	.long	0
	.long	1071799296
	.long	0
	.long	1071797248
	.long	0
	.long	1071796224
	.long	0
	.long	1071794176
	.long	0
	.long	1071793152
	.long	0
	.long	1071791104
	.long	0
	.long	1071790080
	.long	0
	.long	1071788032
	.long	0
	.long	1071787008
	.long	0
	.long	1071784960
	.long	0
	.long	1071783936
	.long	0
	.long	1071782912
	.long	0
	.long	1071780864
	.long	0
	.long	1071779840
	.long	0
	.long	1071777792
	.long	0
	.long	1071776768
	.long	0
	.long	1071774720
	.long	0
	.long	1071773696
	.long	0
	.long	1071772672
	.long	0
	.long	1071770624
	.long	0
	.long	1071769600
	.long	0
	.long	1071767552
	.long	0
	.long	1071766528
	.long	0
	.long	1071765504
	.long	0
	.long	1071763456
	.long	0
	.long	1071762432
	.long	0
	.long	1071760384
	.long	0
	.long	1071759360
	.long	0
	.long	1071758336
	.long	0
	.long	1071756288
	.long	0
	.long	1071755264
	.long	0
	.long	1071754240
	.long	0
	.long	1071752192
	.long	0
	.long	1071751168
	.long	0
	.long	1071750144
	.long	0
	.long	1071748096
	.long	0
	.long	1071747072
	.long	0
	.long	1071746048
	.long	0
	.long	1071744000
	.long	0
	.long	1071742976
	.long	0
	.long	1071741952
	.long	0
	.long	1071739904
	.long	0
	.long	1071738880
	.long	0
	.long	1071737856
	.long	0
	.long	1071736832
	.long	0
	.long	1071734784
	.long	0
	.long	1071733760
	.long	0
	.long	1071732736
	.long	0
	.long	1071730688
	.long	0
	.long	1071729664
	.long	0
	.long	1071728640
	.long	0
	.long	1071727616
	.long	0
	.long	1071725568
	.long	0
	.long	1071724544
	.long	0
	.long	1071723520
	.long	0
	.long	1071722496
	.long	0
	.long	1071720448
	.long	0
	.long	1071719424
	.long	0
	.long	1071718400
	.long	0
	.long	1071717376
	.long	0
	.long	1071715328
	.long	0
	.long	1071714304
	.long	0
	.long	1071713280
	.long	0
	.long	1071712256
	.long	0
	.long	1071711232
	.long	0
	.long	1071709184
	.long	0
	.long	1071708160
	.long	0
	.long	1071707136
	.long	0
	.long	1071706112
	.long	0
	.long	1071705088
	.long	0
	.long	1071704064
	.long	0
	.long	1071702016
	.long	0
	.long	1071700992
	.long	0
	.long	1071699968
	.long	0
	.long	1071698944
	.long	0
	.long	1071697920
	.long	0
	.long	1071696896
	.long	0
	.long	1071694848
	.long	0
	.long	1071693824
	.long	0
	.long	1071692800
	.long	0
	.long	1071691776
	.long	0
	.long	1071690752
	.long	0
	.long	1071689728
	.long	0
	.long	1071688704
	.long	0
	.long	1071686656
	.long	0
	.long	1071685632
	.long	0
	.long	1071684608
	.long	0
	.long	1071683584
	.long	0
	.long	1071682560
	.long	0
	.long	1071681536
	.long	0
	.long	1071680512
	.long	0
	.long	1071679488
	.long	0
	.long	1071677440
	.long	0
	.long	1071676416
	.long	0
	.long	1071675392
	.long	0
	.long	1071674368
	.long	0
	.long	1071673344
	.long	0
	.long	1071672320
	.long	0
	.long	1071671296
	.long	0
	.long	1071670272
	.long	0
	.long	1071669248
	.long	0
	.long	1071668224
	.long	0
	.long	1071667200
	.long	0
	.long	1071666176
	.long	0
	.long	1071665152
	.long	0
	.long	1071663104
	.long	0
	.long	1071662080
	.long	0
	.long	1071661056
	.long	0
	.long	1071660032
	.long	0
	.long	1071659008
	.long	0
	.long	1071657984
	.long	0
	.long	1071656960
	.long	0
	.long	1071655936
	.long	0
	.long	1071654912
	.long	0
	.long	1071653888
	.long	0
	.long	1071652864
	.long	0
	.long	1071651840
	.long	0
	.long	1071650816
	.long	0
	.long	1071649792
	.long	0
	.long	1071648768
	.long	0
	.long	1071647744
	.long	0
	.long	1071646720
	.long	0
	.long	1071645696
	.long	0
	.long	0
	.long	0
	.long	1071644672
	.long	0
	.long	1072693248
	.long	0
	.long	1073741824
	.long	33554432
	.long	1101004800
	.type	_vmlsHypotHATab,@object
	.size	_vmlsHypotHATab,4136
	.space 472, 0x00 	# pad
	.align 16
.L_2il0floatpacket.50:
	.long	0x00000000,0x80000000,0x00000000,0x00000000
	.type	.L_2il0floatpacket.50,@object
	.size	.L_2il0floatpacket.50,16
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
