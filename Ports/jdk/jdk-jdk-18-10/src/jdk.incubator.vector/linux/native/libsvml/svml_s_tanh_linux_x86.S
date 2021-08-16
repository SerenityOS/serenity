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
	.file "svml_s_tanh.c"
	.text
..TXTST0:
.L_2__routine_start___svml_tanhf4_ha_l9_0:
# -- Begin  __svml_tanhf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanhf4_ha_l9
# --- __svml_tanhf4_ha_l9(__m128)
__svml_tanhf4_ha_l9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #427.1
        .byte     15                                            #490.540
        .byte     30                                            #490.540
        .byte     250                                           #490.540
	.cfi_startproc
..___tag_value___svml_tanhf4_ha_l9.1:
..L2:
                                                          #427.1
        pushq     %rbp                                          #427.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #427.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #427.1
        subq      $192, %rsp                                    #427.1
        vmovaps   %xmm0, %xmm7                                  #427.1
        vpand     3200+__svml_stanh_ha_data_internal(%rip), %xmm7, %xmm11 #466.18
        lea       1392+__svml_stanh_ha_data_internal(%rip), %r8 #475.49
        vpsubd    3328+__svml_stanh_ha_data_internal(%rip), %xmm11, %xmm14 #469.15
        vpxor     %xmm12, %xmm12, %xmm12                        #470.19
        vmovups   3392+__svml_stanh_ha_data_internal(%rip), %xmm15 #461.24
        vpcmpgtd  %xmm12, %xmm14, %xmm13                        #470.19
        vpand     %xmm13, %xmm14, %xmm6                         #472.19
        vpcmpgtd  %xmm15, %xmm14, %xmm0                         #471.19
        blendvps  %xmm0, %xmm15, %xmm6                          #473.37
        vpsrld    $15, %xmm6, %xmm5                             #474.23
        vmovd     %xmm5, %edx                                   #475.117
        vpcmpgtd  3264+__svml_stanh_ha_data_internal(%rip), %xmm11, %xmm10 #467.19
        vandps    3136+__svml_stanh_ha_data_internal(%rip), %xmm7, %xmm8 #463.12
        vpextrd   $1, %xmm5, %ecx                               #475.258
        movslq    %edx, %rdx                                    #475.117
        movslq    %ecx, %rcx                                    #475.190
        vpextrd   $2, %xmm5, %esi                               #475.402
        vpextrd   $3, %xmm5, %edi                               #475.546
        movslq    %esi, %rsi                                    #475.334
        movslq    %edi, %rdi                                    #475.478
        vmovmskps %xmm10, %eax                                  #468.40
        vmovupd   -32(%rdx,%r8), %xmm0                          #476.49
        vmovupd   -32(%rcx,%r8), %xmm10                         #476.190
        vcvtps2pd %xmm8, %ymm8                                  #479.22
        vmovupd   -48(%rcx,%r8), %xmm3                          #475.190
        vmovupd   -48(%rdx,%r8), %xmm4                          #475.49
        vmovupd   -16(%rdx,%r8), %xmm13                         #477.54
        vmovupd   -16(%rcx,%r8), %xmm14                         #477.195
        vandps    3072+__svml_stanh_ha_data_internal(%rip), %xmm7, %xmm9 #464.13
        vinsertf128 $1, -32(%rsi,%r8), %ymm0, %ymm11            #476.600
        vinsertf128 $1, -32(%rdi,%r8), %ymm10, %ymm12           #476.681
        vmovupd   (%rdx,%r8), %xmm0                             #478.54
        vmovupd   (%rcx,%r8), %xmm10                            #478.195
        vinsertf128 $1, -48(%rsi,%r8), %ymm4, %ymm2             #475.600
        vinsertf128 $1, -48(%rdi,%r8), %ymm3, %ymm1             #475.681
        vunpcklpd %ymm12, %ymm11, %ymm4                         #476.764
        vunpckhpd %ymm12, %ymm11, %ymm3                         #476.816
        vinsertf128 $1, (%rdi,%r8), %ymm10, %ymm12              #478.686
        vinsertf128 $1, (%rsi,%r8), %ymm0, %ymm11               #478.605
        vunpckhpd %ymm12, %ymm11, %ymm0                         #478.821
        vunpcklpd %ymm1, %ymm2, %ymm6                           #475.764
        vunpckhpd %ymm1, %ymm2, %ymm5                           #475.816
        vinsertf128 $1, -16(%rsi,%r8), %ymm13, %ymm15           #477.605
        vunpcklpd %ymm12, %ymm11, %ymm13                        #478.769
        vfmadd213pd %ymm13, %ymm8, %ymm0                        #480.20
        vinsertf128 $1, -16(%rdi,%r8), %ymm14, %ymm1            #477.686
        vunpcklpd %ymm1, %ymm15, %ymm2                          #477.769
        vunpckhpd %ymm1, %ymm15, %ymm1                          #477.821
        vfmadd213pd %ymm1, %ymm8, %ymm0                         #481.20
        vfmadd213pd %ymm2, %ymm8, %ymm0                         #482.20
        vfmadd213pd %ymm3, %ymm8, %ymm0                         #483.20
        vfmadd213pd %ymm4, %ymm8, %ymm0                         #484.20
        vfmadd213pd %ymm5, %ymm8, %ymm0                         #485.16
        vfmadd213pd %ymm6, %ymm8, %ymm0                         #486.16
        vcvtpd2ps %ymm0, %xmm1                                  #487.16
        vorps     %xmm9, %xmm1, %xmm0                           #488.10
        testl     %eax, %eax                                    #490.52
        jne       ..B1.3        # Prob 5%                       #490.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm7
..B1.13:                        # Preds ..B1.3 ..B1.1
                                # Execution count [9.50e-01]
        vzeroupper                                              #
                                # LOE rbx r12 r13 r14 r15 xmm0
..B1.2:                         # Preds ..B1.9 ..B1.13
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #493.12
        popq      %rbp                                          #493.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #493.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm7, 64(%rsp)                               #490.193
        vmovups   %xmm0, 128(%rsp)                              #490.264
        je        ..B1.13       # Prob 95%                      #490.368
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #490.448
                                # LOE rbx r12 r13 r14 r15 eax edx
..B1.15:                        # Preds ..B1.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #490.511
        jc        ..B1.10       # Prob 5%                       #490.511
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #490.464
        cmpl      $4, %r12d                                     #490.459
        jl        ..B1.7        # Prob 82%                      #490.459
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #490.666
        jmp       ..B1.2        # Prob 100%                     #490.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #490.540
        lea       128(%rsp,%r12,4), %rsi                        #490.540
..___tag_value___svml_tanhf4_ha_l9.19:
#       __svml_stanh_ha_cout_rare_internal(const float *, float *)
        call      __svml_stanh_ha_cout_rare_internal            #490.540
..___tag_value___svml_tanhf4_ha_l9.20:
        jmp       ..B1.8        # Prob 100%                     #490.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_tanhf4_ha_l9,@function
	.size	__svml_tanhf4_ha_l9,.-__svml_tanhf4_ha_l9
..LN__svml_tanhf4_ha_l9.0:
	.data
# -- End  __svml_tanhf4_ha_l9
	.text
.L_2__routine_start___svml_tanhf4_ha_e9_1:
# -- Begin  __svml_tanhf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanhf4_ha_e9
# --- __svml_tanhf4_ha_e9(__m128)
__svml_tanhf4_ha_e9:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #498.1
        .byte     15                                            #561.540
        .byte     30                                            #561.540
        .byte     250                                           #561.540
	.cfi_startproc
..___tag_value___svml_tanhf4_ha_e9.22:
..L23:
                                                         #498.1
        pushq     %rbp                                          #498.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #498.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #498.1
        subq      $192, %rsp                                    #498.1
        vmovaps   %xmm0, %xmm10                                 #498.1
        vpand     3200+__svml_stanh_ha_data_internal(%rip), %xmm10, %xmm12 #537.22
        lea       1392+__svml_stanh_ha_data_internal(%rip), %r8 #546.49
        vpsubd    3328+__svml_stanh_ha_data_internal(%rip), %xmm12, %xmm15 #540.19
        vpxor     %xmm13, %xmm13, %xmm13                        #541.23
        vmovups   3392+__svml_stanh_ha_data_internal(%rip), %xmm7 #532.28
        vpcmpgtd  %xmm13, %xmm15, %xmm14                        #541.23
        vpand     %xmm14, %xmm15, %xmm1                         #543.23
        vpcmpgtd  %xmm7, %xmm15, %xmm0                          #542.23
        blendvps  %xmm0, %xmm7, %xmm1                           #544.41
        vpsrld    $15, %xmm1, %xmm6                             #545.27
        vmovd     %xmm6, %edx                                   #546.117
        vpcmpgtd  3264+__svml_stanh_ha_data_internal(%rip), %xmm12, %xmm11 #538.23
        vandps    3136+__svml_stanh_ha_data_internal(%rip), %xmm10, %xmm8 #534.12
        vpextrd   $1, %xmm6, %ecx                               #546.258
        movslq    %edx, %rdx                                    #546.117
        movslq    %ecx, %rcx                                    #546.190
        vpextrd   $2, %xmm6, %esi                               #546.402
        vpextrd   $3, %xmm6, %edi                               #546.546
        movslq    %esi, %rsi                                    #546.334
        movslq    %edi, %rdi                                    #546.478
        vmovmskps %xmm11, %eax                                  #539.40
        vmovupd   -32(%rdx,%r8), %xmm11                         #547.49
        vmovupd   -32(%rcx,%r8), %xmm12                         #547.190
        vmovupd   -16(%rcx,%r8), %xmm1                          #548.195
        vmovupd   -48(%rcx,%r8), %xmm4                          #546.190
        vmovupd   -48(%rdx,%r8), %xmm5                          #546.49
        vmovupd   -16(%rdx,%r8), %xmm15                         #548.54
        vandps    3072+__svml_stanh_ha_data_internal(%rip), %xmm10, %xmm9 #535.13
        vinsertf128 $1, -32(%rsi,%r8), %ymm11, %ymm13           #547.600
        vinsertf128 $1, -32(%rdi,%r8), %ymm12, %ymm14           #547.681
        vinsertf128 $1, -16(%rdi,%r8), %ymm1, %ymm0             #548.686
        vcvtps2pd %xmm8, %ymm1                                  #550.22
        vmovupd   (%rdx,%r8), %xmm11                            #549.54
        vmovupd   (%rcx,%r8), %xmm12                            #549.195
        vinsertf128 $1, -48(%rsi,%r8), %ymm5, %ymm3             #546.600
        vinsertf128 $1, -48(%rdi,%r8), %ymm4, %ymm2             #546.681
        vunpcklpd %ymm2, %ymm3, %ymm7                           #546.764
        vunpckhpd %ymm2, %ymm3, %ymm6                           #546.816
        vunpcklpd %ymm14, %ymm13, %ymm5                         #547.764
        vunpckhpd %ymm14, %ymm13, %ymm4                         #547.816
        vinsertf128 $1, -16(%rsi,%r8), %ymm15, %ymm2            #548.605
        vinsertf128 $1, (%rsi,%r8), %ymm11, %ymm13              #549.605
        vinsertf128 $1, (%rdi,%r8), %ymm12, %ymm14              #549.686
        vunpcklpd %ymm0, %ymm2, %ymm3                           #548.769
        vunpckhpd %ymm0, %ymm2, %ymm2                           #548.821
        vunpcklpd %ymm14, %ymm13, %ymm0                         #549.769
        vunpckhpd %ymm14, %ymm13, %ymm13                        #549.821
        vmulpd    %ymm13, %ymm1, %ymm8                          #551.35
        vaddpd    %ymm0, %ymm8, %ymm8                           #551.20
        vmulpd    %ymm8, %ymm1, %ymm11                          #552.35
        vaddpd    %ymm2, %ymm11, %ymm2                          #552.20
        vmulpd    %ymm2, %ymm1, %ymm12                          #553.35
        vaddpd    %ymm3, %ymm12, %ymm3                          #553.20
        vmulpd    %ymm3, %ymm1, %ymm13                          #554.35
        vaddpd    %ymm4, %ymm13, %ymm4                          #554.20
        vmulpd    %ymm4, %ymm1, %ymm14                          #555.35
        vaddpd    %ymm5, %ymm14, %ymm5                          #555.20
        vmulpd    %ymm5, %ymm1, %ymm15                          #556.31
        vaddpd    %ymm6, %ymm15, %ymm6                          #556.16
        vmulpd    %ymm6, %ymm1, %ymm1                           #557.31
        vaddpd    %ymm7, %ymm1, %ymm7                           #557.16
        vcvtpd2ps %ymm7, %xmm0                                  #558.16
        vorps     %xmm9, %xmm0, %xmm0                           #559.10
        testl     %eax, %eax                                    #561.52
        jne       ..B2.3        # Prob 5%                       #561.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm10
..B2.13:                        # Preds ..B2.3 ..B2.1
                                # Execution count [9.50e-01]
        vzeroupper                                              #
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.2:                         # Preds ..B2.9 ..B2.13
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #564.12
        popq      %rbp                                          #564.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #564.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm10, 64(%rsp)                              #561.193
        vmovups   %xmm0, 128(%rsp)                              #561.264
        je        ..B2.13       # Prob 95%                      #561.368
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #561.448
                                # LOE rbx r12 r13 r14 r15 eax edx
..B2.15:                        # Preds ..B2.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #561.511
        jc        ..B2.10       # Prob 5%                       #561.511
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #561.464
        cmpl      $4, %r12d                                     #561.459
        jl        ..B2.7        # Prob 82%                      #561.459
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #561.666
        jmp       ..B2.2        # Prob 100%                     #561.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #561.540
        lea       128(%rsp,%r12,4), %rsi                        #561.540
..___tag_value___svml_tanhf4_ha_e9.40:
#       __svml_stanh_ha_cout_rare_internal(const float *, float *)
        call      __svml_stanh_ha_cout_rare_internal            #561.540
..___tag_value___svml_tanhf4_ha_e9.41:
        jmp       ..B2.8        # Prob 100%                     #561.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_tanhf4_ha_e9,@function
	.size	__svml_tanhf4_ha_e9,.-__svml_tanhf4_ha_e9
..LN__svml_tanhf4_ha_e9.1:
	.data
# -- End  __svml_tanhf4_ha_e9
	.text
.L_2__routine_start___svml_tanhf16_ha_z0_2:
# -- Begin  __svml_tanhf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanhf16_ha_z0
# --- __svml_tanhf16_ha_z0(__m512)
__svml_tanhf16_ha_z0:
# parameter 1: %zmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #589.1
        .byte     15                                            #652.760
        .byte     30                                            #652.760
        .byte     250                                           #652.760
	.cfi_startproc
..___tag_value___svml_tanhf16_ha_z0.43:
..L44:
                                                         #589.1
        pushq     %rbp                                          #589.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #589.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #589.1
        subq      $192, %rsp                                    #589.1
        vmovaps   %zmm0, %zmm1                                  #589.1
        vmovups   __svml_stanh_ha_data_internal(%rip), %zmm9    #632.252
        vmovups   896+__svml_stanh_ha_data_internal(%rip), %zmm10 #639.261
        vmovups   1024+__svml_stanh_ha_data_internal(%rip), %zmm15 #640.261
        vmovups   768+__svml_stanh_ha_data_internal(%rip), %zmm11 #638.261
        vmovups   640+__svml_stanh_ha_data_internal(%rip), %zmm12 #637.257
        vmovups   512+__svml_stanh_ha_data_internal(%rip), %zmm13 #636.257
        vandps    3136+__svml_stanh_ha_data_internal(%rip), %zmm1, %zmm8 #622.12
        vpternlogd $255, %zmm2, %zmm2, %zmm2                    #626.17
        vandps    3072+__svml_stanh_ha_data_internal(%rip), %zmm1, %zmm0 #623.13
        vpandd    1152+__svml_stanh_ha_data_internal(%rip), %zmm1, %zmm3 #625.16
        vpsubd    1216+__svml_stanh_ha_data_internal(%rip), %zmm3, %zmm4 #628.13
        vpcmpd    $2, 3264+__svml_stanh_ha_data_internal(%rip), %zmm3, %k1 #626.84
        vpxord    %zmm5, %zmm5, %zmm5                           #629.17
        vpmaxsd   %zmm5, %zmm4, %zmm6                           #629.17
        vpminsd   1280+__svml_stanh_ha_data_internal(%rip), %zmm6, %zmm7 #630.17
        vpsrld    $21, %zmm7, %zmm14                            #631.23
        vmovups   128+__svml_stanh_ha_data_internal(%rip), %zmm4 #634.257
        vpermt2ps 64+__svml_stanh_ha_data_internal(%rip), %zmm14, %zmm9 #632.252
        vpermt2ps 960+__svml_stanh_ha_data_internal(%rip), %zmm14, %zmm10 #639.261
        vpermt2ps 1088+__svml_stanh_ha_data_internal(%rip), %zmm14, %zmm15 #640.261
        vpermt2ps 832+__svml_stanh_ha_data_internal(%rip), %zmm14, %zmm11 #638.261
        vpermt2ps 704+__svml_stanh_ha_data_internal(%rip), %zmm14, %zmm12 #637.257
        vpermt2ps 576+__svml_stanh_ha_data_internal(%rip), %zmm14, %zmm13 #636.257
        vpermt2ps 192+__svml_stanh_ha_data_internal(%rip), %zmm14, %zmm4 #634.257
        vpandnd   %zmm3, %zmm3, %zmm2{%k1}                      #626.17
        vsubps    {rn-sae}, %zmm9, %zmm8, %zmm3                 #633.18
        vptestmd  %zmm2, %zmm2, %k0                             #627.29
        vmovups   384+__svml_stanh_ha_data_internal(%rip), %zmm2 #635.257
        vfmadd213ps {rn-sae}, %zmm10, %zmm3, %zmm15             #641.20
        vmovups   256+__svml_stanh_ha_data_internal(%rip), %zmm10 #645.265
        vpermt2ps 448+__svml_stanh_ha_data_internal(%rip), %zmm14, %zmm2 #635.257
        vfmadd213ps {rn-sae}, %zmm11, %zmm3, %zmm15             #642.20
        vpermt2ps 320+__svml_stanh_ha_data_internal(%rip), %zmm14, %zmm10 #645.265
        kmovw     %k0, %esi                                     #627.29
        vfmadd213ps {rn-sae}, %zmm12, %zmm3, %zmm15             #643.20
        vfmadd213ps {rn-sae}, %zmm13, %zmm3, %zmm15             #644.20
        vmulps    {rn-sae}, %zmm3, %zmm15, %zmm11               #646.26
        vfmadd213ps {rn-sae}, %zmm10, %zmm3, %zmm11             #647.24
        vfmadd213ps {rn-sae}, %zmm11, %zmm2, %zmm3              #648.24
        vaddps    {rn-sae}, %zmm4, %zmm3, %zmm5                 #649.26
        vorps     %zmm0, %zmm5, %zmm0                           #650.10
        testl     %esi, %esi                                    #652.52
        jne       ..B3.3        # Prob 5%                       #652.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm1
..B3.2:                         # Preds ..B3.9 ..B3.7 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #655.12
        popq      %rbp                                          #655.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #655.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #652.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm1
..B3.4:                         # Preds ..B3.3
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #652.249
        movl      %edx, %eax                                    #652.303
        orl       $8064, %eax                                   #652.303
        cmpl      %eax, %edx                                    #652.332
        je        ..B3.6        # Prob 78%                      #652.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm1
..B3.5:                         # Preds ..B3.4
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #652.344
        vldmxcsr  32(%rsp)                                      #652.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm1
..B3.6:                         # Preds ..B3.5 ..B3.4
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm1, 64(%rsp)                               #652.404
        vmovups   %zmm0, 128(%rsp)                              #652.478
        testl     %esi, %esi                                    #652.586
        jne       ..B3.11       # Prob 5%                       #652.586
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B3.7:                         # Preds ..B3.14 ..B3.6
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #652.932
        je        ..B3.2        # Prob 78%                      #652.932
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B3.8:                         # Preds ..B3.7
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #652.957
        movl      32(%rsp), %eax                                #652.957
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B3.9:                         # Preds ..B3.8
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #652.944
        orl       %edx, %eax                                    #652.944
        movl      %eax, 32(%rsp)                                #652.944
        vldmxcsr  32(%rsp)                                      #652.944
        jmp       ..B3.2        # Prob 100%                     #652.944
                                # LOE rbx r12 r13 r14 r15 zmm0
..B3.11:                        # Preds ..B3.6
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #652.668
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
        btl       %ebx, %r14d                                   #652.731
        jc        ..B3.15       # Prob 5%                       #652.731
                                # LOE rbx r15 r12d r13d r14d
..B3.13:                        # Preds ..B3.15 ..B3.12
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #652.684
        cmpl      $16, %ebx                                     #652.679
        jl        ..B3.12       # Prob 82%                      #652.679
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
        vmovups   128(%rsp), %zmm0                              #652.887
        jmp       ..B3.7        # Prob 100%                     #652.887
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B3.15:                        # Preds ..B3.12
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,4), %rdi                         #652.760
        lea       128(%rsp,%rbx,4), %rsi                        #652.760
..___tag_value___svml_tanhf16_ha_z0.71:
#       __svml_stanh_ha_cout_rare_internal(const float *, float *)
        call      __svml_stanh_ha_cout_rare_internal            #652.760
..___tag_value___svml_tanhf16_ha_z0.72:
        jmp       ..B3.13       # Prob 100%                     #652.760
        .align    16,0x90
                                # LOE r15 ebx r12d r13d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_tanhf16_ha_z0,@function
	.size	__svml_tanhf16_ha_z0,.-__svml_tanhf16_ha_z0
..LN__svml_tanhf16_ha_z0.2:
	.data
# -- End  __svml_tanhf16_ha_z0
	.text
.L_2__routine_start___svml_tanhf8_ha_l9_3:
# -- Begin  __svml_tanhf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanhf8_ha_l9
# --- __svml_tanhf8_ha_l9(__m256)
__svml_tanhf8_ha_l9:
# parameter 1: %ymm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #660.1
        .byte     15                                            #723.546
        .byte     30                                            #723.546
        .byte     250                                           #723.546
	.cfi_startproc
..___tag_value___svml_tanhf8_ha_l9.74:
..L75:
                                                         #660.1
        pushq     %rbp                                          #660.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #660.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #660.1
        pushq     %r14                                          #660.1
        subq      $184, %rsp                                    #660.1
        lea       1392+__svml_stanh_ha_data_internal(%rip), %r10 #708.71
        vpand     3200+__svml_stanh_ha_data_internal(%rip), %ymm0, %ymm8 #699.18
        vmovups   3392+__svml_stanh_ha_data_internal(%rip), %ymm3 #694.24
        vmovups   %ymm0, (%rsp)                                 #660.1[spill]
        vpsubd    3328+__svml_stanh_ha_data_internal(%rip), %ymm8, %ymm7 #702.15
        vxorps    %ymm6, %ymm6, %ymm6                           #703.19
        vpcmpgtd  %ymm6, %ymm7, %ymm9                           #703.19
        vpand     %ymm9, %ymm7, %ymm5                           #705.19
        vandps    3136+__svml_stanh_ha_data_internal(%rip), %ymm0, %ymm12 #696.12
        vandps    3072+__svml_stanh_ha_data_internal(%rip), %ymm0, %ymm10 #697.13
        vpcmpgtd  %ymm3, %ymm7, %ymm0                           #704.19
        vmovups   %ymm10, 32(%rsp)                              #697.13[spill]
        vblendvps %ymm0, %ymm3, %ymm5, %ymm10                   #706.40
        vpcmpgtd  3264+__svml_stanh_ha_data_internal(%rip), %ymm8, %ymm11 #700.19
        vmovmskps %ymm11, %r11d                                 #701.41
        vpsrld    $15, %ymm10, %ymm11                           #707.23
        vextractf128 $1, %ymm11, %xmm8                          #708.931
        vmovd     %xmm11, %r9d                                  #708.139
        vmovd     %xmm8, %ecx                                   #708.912
        vpextrd   $1, %xmm11, %r8d                              #708.330
        movslq    %r9d, %r9                                     #708.139
        movslq    %r8d, %r8                                     #708.262
        vpextrd   $1, %xmm8, %edx                               #708.1108
        vpextrd   $2, %xmm11, %edi                              #708.524
        vpextrd   $3, %xmm11, %esi                              #708.718
        movslq    %ecx, %rcx                                    #708.844
        movslq    %edx, %rdx                                    #708.1040
        movslq    %edi, %rdi                                    #708.456
        movslq    %esi, %rsi                                    #708.650
        vpextrd   $2, %xmm8, %eax                               #708.1307
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
        vpextrd   $3, %xmm8, %r14d                              #708.1506
        movslq    %eax, %rax                                    #708.1239
        movslq    %r14d, %r14                                   #708.1438
        vmovupd   -48(%r9,%r10), %xmm6                          #708.71
        vmovupd   -48(%r8,%r10), %xmm9                          #708.262
        vmovupd   -48(%rcx,%r10), %xmm7                         #708.844
        vmovupd   -48(%rdx,%r10), %xmm2                         #708.1040
        vmovupd   -32(%r9,%r10), %xmm15                         #709.71
        vmovupd   -32(%r8,%r10), %xmm0                          #709.262
        vmovupd   -32(%rcx,%r10), %xmm8                         #709.844
        vinsertf128 $1, -48(%rdi,%r10), %ymm6, %ymm5            #708.1593
        vinsertf128 $1, -48(%rsi,%r10), %ymm9, %ymm3            #708.1674
        vunpcklpd %ymm3, %ymm5, %ymm4                           #708.1925
        vmovupd   -32(%rdx,%r10), %xmm6                         #709.1040
        vmovupd   %ymm4, 64(%rsp)                               #708.1925[spill]
        vunpckhpd %ymm3, %ymm5, %ymm10                          #708.1983
        vmovupd   -16(%r9,%r10), %xmm5                          #710.76
        vmovupd   -16(%r8,%r10), %xmm3                          #710.267
        vinsertf128 $1, -48(%rax,%r10), %ymm7, %ymm14           #708.1755
        vinsertf128 $1, -48(%r14,%r10), %ymm2, %ymm1            #708.1836
        vunpcklpd %ymm1, %ymm14, %ymm13                         #708.2041
        vmovupd   %ymm13, 96(%rsp)                              #708.2041[spill]
        vmovupd   -16(%rcx,%r10), %xmm13                        #710.849
        vunpckhpd %ymm1, %ymm14, %ymm11                         #708.2099
        vinsertf128 $1, -32(%rdi,%r10), %ymm15, %ymm9           #709.1593
        vinsertf128 $1, -32(%rsi,%r10), %ymm0, %ymm7            #709.1674
        vinsertf128 $1, -32(%rax,%r10), %ymm8, %ymm2            #709.1755
        vinsertf128 $1, -32(%r14,%r10), %ymm6, %ymm4            #709.1836
        vunpcklpd %ymm7, %ymm9, %ymm8                           #709.1925
        vunpckhpd %ymm7, %ymm9, %ymm6                           #709.1983
        vunpcklpd %ymm4, %ymm2, %ymm9                           #709.2041
        vunpckhpd %ymm4, %ymm2, %ymm7                           #709.2099
        vmovupd   -16(%rdx,%r10), %xmm4                         #710.1045
        vmovupd   (%rcx,%r10), %xmm15                           #711.849
        vinsertf128 $1, -16(%rax,%r10), %ymm13, %ymm1           #710.1760
        vinsertf128 $1, -16(%r14,%r10), %ymm4, %ymm0            #710.1841
        vmovupd   (%r9,%r10), %xmm13                            #711.76
        vinsertf128 $1, -16(%rdi,%r10), %ymm5, %ymm2            #710.1598
        vinsertf128 $1, -16(%rsi,%r10), %ymm3, %ymm14           #710.1679
        vunpcklpd %ymm0, %ymm1, %ymm5                           #710.2046
        vunpckhpd %ymm0, %ymm1, %ymm3                           #710.2104
        vmovupd   (%r8,%r10), %xmm0                             #711.267
        vmovupd   (%rdx,%r10), %xmm1                            #711.1045
        vunpcklpd %ymm14, %ymm2, %ymm4                          #710.1930
        vunpckhpd %ymm14, %ymm2, %ymm2                          #710.1988
        vinsertf128 $1, (%rsi,%r10), %ymm0, %ymm14              #711.1679
        vinsertf128 $1, (%rdi,%r10), %ymm13, %ymm13             #711.1598
        vinsertf128 $1, (%rax,%r10), %ymm15, %ymm15             #711.1760
        vinsertf128 $1, (%r14,%r10), %ymm1, %ymm0               #711.1841
        vunpcklpd %ymm14, %ymm13, %ymm1                         #711.1930
        vunpckhpd %ymm14, %ymm13, %ymm14                        #711.1988
        vunpcklpd %ymm0, %ymm15, %ymm13                         #711.2046
        vunpckhpd %ymm0, %ymm15, %ymm0                          #711.2104
        vcvtps2pd %xmm12, %ymm15                                #712.22
        vextractf128 $1, %ymm12, %xmm12                         #712.110
        vfmadd213pd %ymm1, %ymm15, %ymm14                       #713.22
        vcvtps2pd %xmm12, %ymm12                                #712.93
        vfmadd213pd %ymm2, %ymm15, %ymm14                       #714.22
        vfmadd213pd %ymm13, %ymm12, %ymm0                       #713.90
        vfmadd213pd %ymm4, %ymm15, %ymm14                       #715.22
        vfmadd213pd %ymm3, %ymm12, %ymm0                        #714.90
        vfmadd213pd %ymm6, %ymm15, %ymm14                       #716.22
        vfmadd213pd %ymm5, %ymm12, %ymm0                        #715.90
        vfmadd213pd %ymm8, %ymm15, %ymm14                       #717.22
        vfmadd213pd %ymm7, %ymm12, %ymm0                        #716.90
        vfmadd213pd %ymm10, %ymm15, %ymm14                      #718.18
        vfmadd213pd %ymm9, %ymm12, %ymm0                        #717.90
        vfmadd213pd 64(%rsp), %ymm15, %ymm14                    #719.18[spill]
        vfmadd213pd %ymm11, %ymm12, %ymm0                       #718.86
        vcvtpd2ps %ymm14, %xmm1                                 #720.62
        vfmadd213pd 96(%rsp), %ymm12, %ymm0                     #719.86[spill]
        vcvtpd2ps %ymm0, %xmm0                                  #720.99
        vinsertf128 $1, %xmm0, %ymm1, %ymm2                     #720.16
        vorps     32(%rsp), %ymm2, %ymm0                        #721.10[spill]
        testl     %r11d, %r11d                                  #723.52
        jne       ..B4.3        # Prob 5%                       #723.52
                                # LOE rbx r12 r13 r15 r11d ymm0
..B4.2:                         # Preds ..B4.3 ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        addq      $184, %rsp                                    #726.12
	.cfi_restore 14
        popq      %r14                                          #726.12
        movq      %rbp, %rsp                                    #726.12
        popq      %rbp                                          #726.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #726.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   (%rsp), %ymm1                                 #723.196[spill]
        vmovups   %ymm0, 128(%rsp)                              #723.270
        vmovups   %ymm1, 64(%rsp)                               #723.196
        je        ..B4.2        # Prob 95%                      #723.374
                                # LOE rbx r12 r13 r15 r11d ymm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %r14d, %r14d                                  #723.454
                                # LOE rbx r12 r13 r14 r15 r11d
..B4.13:                        # Preds ..B4.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %r11d, %r12d                                  #
                                # LOE rbx r13 r14 r15 r12d
..B4.7:                         # Preds ..B4.8 ..B4.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r14d, %r12d                                  #723.517
        jc        ..B4.10       # Prob 5%                       #723.517
                                # LOE rbx r13 r14 r15 r12d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r14d                                         #723.470
        cmpl      $8, %r14d                                     #723.465
        jl        ..B4.7        # Prob 82%                      #723.465
                                # LOE rbx r13 r14 r15 r12d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      (%rsp), %r12                                  #[spill]
	.cfi_restore 12
        vmovups   128(%rsp), %ymm0                              #723.675
        jmp       ..B4.2        # Prob 100%                     #723.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r15 ymm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r14,4), %rdi                         #723.546
        lea       128(%rsp,%r14,4), %rsi                        #723.546
..___tag_value___svml_tanhf8_ha_l9.98:
#       __svml_stanh_ha_cout_rare_internal(const float *, float *)
        call      __svml_stanh_ha_cout_rare_internal            #723.546
..___tag_value___svml_tanhf8_ha_l9.99:
        jmp       ..B4.8        # Prob 100%                     #723.546
        .align    16,0x90
                                # LOE rbx r13 r15 r12d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_tanhf8_ha_l9,@function
	.size	__svml_tanhf8_ha_l9,.-__svml_tanhf8_ha_l9
..LN__svml_tanhf8_ha_l9.3:
	.data
# -- End  __svml_tanhf8_ha_l9
	.text
.L_2__routine_start___svml_tanhf8_ha_e9_4:
# -- Begin  __svml_tanhf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanhf8_ha_e9
# --- __svml_tanhf8_ha_e9(__m256)
__svml_tanhf8_ha_e9:
# parameter 1: %ymm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #731.1
        .byte     15                                            #794.546
        .byte     30                                            #794.546
        .byte     250                                           #794.546
	.cfi_startproc
..___tag_value___svml_tanhf8_ha_e9.101:
..L102:
                                                        #731.1
        pushq     %rbp                                          #731.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #731.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #731.1
        pushq     %r12                                          #731.1
        subq      $184, %rsp                                    #731.1
        lea       1392+__svml_stanh_ha_data_internal(%rip), %r10 #779.49
        vmovups   %ymm0, (%rsp)                                 #731.1[spill]
        vpxor     %xmm14, %xmm14, %xmm14                        #772.39
        vmovups   3200+__svml_stanh_ha_data_internal(%rip), %xmm9 #763.29
        vmovups   3264+__svml_stanh_ha_data_internal(%rip), %xmm8 #762.21
        vmovups   3328+__svml_stanh_ha_data_internal(%rip), %xmm13 #764.31
        vmovups   3392+__svml_stanh_ha_data_internal(%rip), %xmm12 #765.28
        vandps    3136+__svml_stanh_ha_data_internal(%rip), %ymm0, %ymm1 #767.12
        vandps    3072+__svml_stanh_ha_data_internal(%rip), %ymm0, %ymm11 #768.13
        vmovups   %ymm11, 32(%rsp)                              #768.13[spill]
        vextractf128 $1, %ymm0, %xmm10                          #769.101
        vpand     %xmm9, %xmm0, %xmm0                           #770.22
        vpcmpgtd  %xmm8, %xmm0, %xmm7                           #771.23
        vpand     %xmm9, %xmm10, %xmm11                         #770.96
        vpsubd    %xmm13, %xmm0, %xmm10                         #773.19
        vpcmpgtd  %xmm8, %xmm11, %xmm6                          #771.103
        vpsubd    %xmm13, %xmm11, %xmm9                         #773.99
        vpackssdw %xmm6, %xmm7, %xmm5                           #772.75
        vpcmpgtd  %xmm14, %xmm10, %xmm13                        #774.23
        vpacksswb %xmm14, %xmm5, %xmm2                          #772.58
        vpand     %xmm13, %xmm10, %xmm8                         #776.23
        vpcmpgtd  %xmm12, %xmm10, %xmm0                         #775.23
        vpcmpgtd  %xmm14, %xmm9, %xmm14                         #774.93
        blendvps  %xmm0, %xmm12, %xmm8                          #777.41
        vpand     %xmm14, %xmm9, %xmm7                          #776.92
        vpcmpgtd  %xmm12, %xmm9, %xmm0                          #775.99
        blendvps  %xmm0, %xmm12, %xmm7                          #777.208
        vpsrld    $15, %xmm8, %xmm12                            #778.27
        vmovd     %xmm12, %r9d                                  #779.117
        vpmovmskb %xmm2, %r11d                                  #772.39
        vpsrld    $15, %xmm7, %xmm2                             #778.92
        vmovd     %xmm2, %ecx                                   #779.1009
        vpextrd   $1, %xmm12, %r8d                              #779.266
        movslq    %r9d, %r9                                     #779.117
        movslq    %r8d, %r8                                     #779.198
        vpextrd   $2, %xmm12, %edi                              #779.418
        vpextrd   $3, %xmm12, %esi                              #779.570
        movslq    %edi, %rdi                                    #779.350
        movslq    %esi, %rsi                                    #779.502
        vpextrd   $1, %xmm2, %edx                               #779.1158
        movslq    %ecx, %rcx                                    #779.1009
        movslq    %edx, %rdx                                    #779.1090
        vpextrd   $2, %xmm2, %eax                               #779.1310
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
        vpextrd   $3, %xmm2, %r12d                              #779.1462
        vmovupd   -48(%r9,%r10), %xmm6                          #779.49
        vmovupd   -48(%r8,%r10), %xmm5                          #779.198
        movslq    %eax, %rax                                    #779.1242
        movslq    %r12d, %r12                                   #779.1394
        vmovupd   -48(%rcx,%r10), %xmm0                         #779.941
        vmovupd   -48(%rdx,%r10), %xmm14                        #779.1090
        vmovupd   -32(%r9,%r10), %xmm9                          #780.49
        vmovupd   -32(%r8,%r10), %xmm8                          #780.198
        vmovupd   -16(%r9,%r10), %xmm2                          #781.54
        vinsertf128 $1, -48(%rdi,%r10), %ymm6, %ymm4            #779.632
        vinsertf128 $1, -48(%rsi,%r10), %ymm5, %ymm15           #779.713
        vunpcklpd %ymm15, %ymm4, %ymm3                          #779.804
        vmovupd   %ymm3, 64(%rsp)                               #779.804[spill]
        vmovupd   -32(%rcx,%r10), %xmm5                         #780.941
        vmovupd   -32(%rdx,%r10), %xmm3                         #780.1090
        vunpckhpd %ymm15, %ymm4, %ymm13                         #779.864
        vinsertf128 $1, -48(%rax,%r10), %ymm0, %ymm11           #779.1524
        vinsertf128 $1, -48(%r12,%r10), %ymm14, %ymm10          #779.1605
        vunpcklpd %ymm10, %ymm11, %ymm12                        #779.1696
        vmovupd   -16(%r8,%r10), %xmm0                          #781.203
        vmovupd   %ymm12, 96(%rsp)                              #779.1696[spill]
        vunpckhpd %ymm10, %ymm11, %ymm14                        #779.1756
        vinsertf128 $1, -32(%rax,%r10), %ymm5, %ymm4            #780.1524
        vinsertf128 $1, -32(%r12,%r10), %ymm3, %ymm15           #780.1605
        vunpcklpd %ymm15, %ymm4, %ymm12                         #780.1696
        vunpckhpd %ymm15, %ymm4, %ymm10                         #780.1756
        vmovupd   -16(%rdx,%r10), %xmm4                         #781.1095
        vmovupd   -16(%rcx,%r10), %xmm3                         #781.946
        vinsertf128 $1, -32(%rdi,%r10), %ymm9, %ymm7            #780.632
        vinsertf128 $1, -32(%rsi,%r10), %ymm8, %ymm6            #780.713
        vunpcklpd %ymm6, %ymm7, %ymm11                          #780.804
        vunpckhpd %ymm6, %ymm7, %ymm9                           #780.864
        vinsertf128 $1, -16(%rsi,%r10), %ymm0, %ymm6            #781.718
        vinsertf128 $1, -16(%r12,%r10), %ymm4, %ymm0            #781.1610
        vinsertf128 $1, -16(%rax,%r10), %ymm3, %ymm15           #781.1529
        vmovupd   (%r8,%r10), %xmm4                             #782.203
        vmovupd   (%r9,%r10), %xmm3                             #782.54
        vinsertf128 $1, -16(%rdi,%r10), %ymm2, %ymm8            #781.637
        vunpcklpd %ymm6, %ymm8, %ymm7                           #781.809
        vunpckhpd %ymm6, %ymm8, %ymm5                           #781.869
        vunpcklpd %ymm0, %ymm15, %ymm8                          #781.1701
        vunpckhpd %ymm0, %ymm15, %ymm6                          #781.1761
        vmovupd   (%rcx,%r10), %xmm0                            #782.946
        vinsertf128 $1, (%rsi,%r10), %ymm4, %ymm2               #782.718
        vinsertf128 $1, (%rdi,%r10), %ymm3, %ymm15              #782.637
        vmovupd   (%rdx,%r10), %xmm4                            #782.1095
        vunpcklpd %ymm2, %ymm15, %ymm3                          #782.809
        vunpckhpd %ymm2, %ymm15, %ymm2                          #782.869
        vinsertf128 $1, (%rax,%r10), %ymm0, %ymm15              #782.1529
        vinsertf128 $1, (%r12,%r10), %ymm4, %ymm0               #782.1610
        vunpcklpd %ymm0, %ymm15, %ymm4                          #782.1701
        vunpckhpd %ymm0, %ymm15, %ymm15                         #782.1761
        vcvtps2pd %xmm1, %ymm0                                  #783.22
        vextractf128 $1, %ymm1, %xmm1                           #783.110
        vmulpd    %ymm2, %ymm0, %ymm2                           #784.37
        vcvtps2pd %xmm1, %ymm1                                  #783.93
        vaddpd    %ymm3, %ymm2, %ymm3                           #784.22
        vmulpd    %ymm15, %ymm1, %ymm15                         #784.120
        vmulpd    %ymm3, %ymm0, %ymm3                           #785.37
        vaddpd    %ymm4, %ymm15, %ymm2                          #784.105
        vaddpd    %ymm5, %ymm3, %ymm3                           #785.22
        vmulpd    %ymm2, %ymm1, %ymm5                           #785.120
        vmulpd    %ymm3, %ymm0, %ymm2                           #786.37
        vaddpd    %ymm6, %ymm5, %ymm4                           #785.105
        vaddpd    %ymm7, %ymm2, %ymm2                           #786.22
        vmulpd    %ymm4, %ymm1, %ymm7                           #786.120
        vmulpd    %ymm2, %ymm0, %ymm3                           #787.37
        vaddpd    %ymm8, %ymm7, %ymm4                           #786.105
        vaddpd    %ymm9, %ymm3, %ymm2                           #787.22
        vmulpd    %ymm4, %ymm1, %ymm9                           #787.120
        vaddpd    %ymm10, %ymm9, %ymm3                          #787.105
        vmulpd    %ymm2, %ymm0, %ymm10                          #788.37
        vaddpd    %ymm11, %ymm10, %ymm2                         #788.22
        vmulpd    %ymm3, %ymm1, %ymm11                          #788.120
        vaddpd    %ymm12, %ymm11, %ymm3                         #788.105
        vmulpd    %ymm2, %ymm0, %ymm12                          #789.33
        vaddpd    %ymm13, %ymm12, %ymm2                         #789.18
        vmulpd    %ymm3, %ymm1, %ymm13                          #789.116
        vmulpd    %ymm2, %ymm0, %ymm0                           #790.33
        vaddpd    %ymm14, %ymm13, %ymm3                         #789.101
        vaddpd    64(%rsp), %ymm0, %ymm4                        #790.18[spill]
        vmulpd    %ymm3, %ymm1, %ymm6                           #790.116
        vcvtpd2ps %ymm4, %xmm8                                  #791.62
        vaddpd    96(%rsp), %ymm6, %ymm14                       #790.101[spill]
        vcvtpd2ps %ymm14, %xmm0                                 #791.99
        vinsertf128 $1, %xmm0, %ymm8, %ymm1                     #791.16
        vorps     32(%rsp), %ymm1, %ymm0                        #792.10[spill]
        testb     %r11b, %r11b                                  #794.52
        jne       ..B5.3        # Prob 5%                       #794.52
                                # LOE rbx r13 r14 r15 r11d ymm0
..B5.2:                         # Preds ..B5.3 ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        addq      $184, %rsp                                    #797.12
	.cfi_restore 12
        popq      %r12                                          #797.12
        movq      %rbp, %rsp                                    #797.12
        popq      %rbp                                          #797.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #797.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   (%rsp), %ymm1                                 #794.196[spill]
        vmovups   %ymm0, 128(%rsp)                              #794.270
        vmovups   %ymm1, 64(%rsp)                               #794.196
        testl     %r11d, %r11d                                  #794.374
        je        ..B5.2        # Prob 95%                      #794.374
                                # LOE rbx r13 r14 r15 r11d ymm0
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %r12d, %r12d                                  #794.454
                                # LOE rbx r12 r13 r14 r15 r11d
..B5.13:                        # Preds ..B5.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %r11d, %r13d                                  #
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #794.517
        jc        ..B5.10       # Prob 5%                       #794.517
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #794.470
        cmpl      $8, %r12d                                     #794.465
        jl        ..B5.7        # Prob 82%                      #794.465
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #794.675
        jmp       ..B5.2        # Prob 100%                     #794.675
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r13 r14 r15 ymm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #794.546
        lea       128(%rsp,%r12,4), %rsi                        #794.546
..___tag_value___svml_tanhf8_ha_e9.125:
#       __svml_stanh_ha_cout_rare_internal(const float *, float *)
        call      __svml_stanh_ha_cout_rare_internal            #794.546
..___tag_value___svml_tanhf8_ha_e9.126:
        jmp       ..B5.8        # Prob 100%                     #794.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_tanhf8_ha_e9,@function
	.size	__svml_tanhf8_ha_e9,.-__svml_tanhf8_ha_e9
..LN__svml_tanhf8_ha_e9.4:
	.data
# -- End  __svml_tanhf8_ha_e9
	.text
.L_2__routine_start___svml_tanhf4_ha_ex_5:
# -- Begin  __svml_tanhf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanhf4_ha_ex
# --- __svml_tanhf4_ha_ex(__m128)
__svml_tanhf4_ha_ex:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #802.1
        .byte     15                                            #865.540
        .byte     30                                            #865.540
        .byte     250                                           #865.540
	.cfi_startproc
..___tag_value___svml_tanhf4_ha_ex.128:
..L129:
                                                        #802.1
        pushq     %rbp                                          #802.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #802.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #802.1
        subq      $192, %rsp                                    #802.1
        movaps    %xmm0, %xmm12                                 #802.1
        movdqu    3200+__svml_stanh_ha_data_internal(%rip), %xmm10 #841.16
        lea       1392+__svml_stanh_ha_data_internal(%rip), %r8 #850.71
        pand      %xmm12, %xmm10                                #841.16
        pxor      %xmm8, %xmm8                                  #845.17
        movdqa    %xmm10, %xmm7                                 #842.17
        psubd     3328+__svml_stanh_ha_data_internal(%rip), %xmm10 #844.13
        movdqu    3392+__svml_stanh_ha_data_internal(%rip), %xmm3 #836.57
        movdqa    %xmm10, %xmm4                                 #846.17
        movdqa    %xmm10, %xmm9                                 #845.17
        pcmpgtd   %xmm3, %xmm4                                  #846.17
        pcmpgtd   %xmm8, %xmm9                                  #845.17
        movdqa    %xmm4, %xmm1                                  #848.46
        pand      %xmm9, %xmm10                                 #847.17
        andps     %xmm4, %xmm3                                  #848.123
        andnps    %xmm10, %xmm1                                 #848.46
        orps      %xmm3, %xmm1                                  #848.35
        psrld     $15, %xmm1                                    #849.21
        pshufd    $2, %xmm1, %xmm6                              #850.519
        movd      %xmm6, %esi                                   #850.501
        pshufd    $3, %xmm1, %xmm13                             #850.720
        pshufd    $1, %xmm1, %xmm5                              #850.318
        movd      %xmm13, %edi                                  #850.702
        movd      %xmm1, %edx                                   #850.139
        movd      %xmm5, %ecx                                   #850.300
        movups    3136+__svml_stanh_ha_data_internal(%rip), %xmm0 #838.12
        movups    3072+__svml_stanh_ha_data_internal(%rip), %xmm11 #839.13
        andps     %xmm12, %xmm0                                 #838.12
        movslq    %esi, %rsi                                    #850.433
        andps     %xmm12, %xmm11                                #839.13
        movslq    %edi, %rdi                                    #850.634
        movslq    %edx, %rdx                                    #850.139
        movups    %xmm12, (%rsp)                                #802.1[spill]
        pcmpgtd   3264+__svml_stanh_ha_data_internal(%rip), %xmm7 #842.17
        movslq    %ecx, %rcx                                    #850.232
        movups    -48(%rsi,%r8), %xmm12                         #850.433
        movmskps  %xmm7, %eax                                   #843.40
        movaps    %xmm12, %xmm7                                 #850.905
        movups    -48(%rdi,%r8), %xmm8                          #850.634
        unpcklpd  %xmm8, %xmm7                                  #850.905
        movups    %xmm7, 48(%rsp)                               #850.905[spill]
        movups    -32(%rdx,%r8), %xmm7                          #851.71
        unpckhpd  %xmm8, %xmm12                                 #850.958
        movaps    %xmm7, %xmm9                                  #851.799
        movups    -32(%rcx,%r8), %xmm10                         #851.232
        movups    -32(%rsi,%r8), %xmm8                          #851.433
        movups    -32(%rdi,%r8), %xmm3                          #851.634
        unpcklpd  %xmm10, %xmm9                                 #851.799
        unpckhpd  %xmm10, %xmm7                                 #851.852
        movaps    %xmm8, %xmm10                                 #851.905
        unpcklpd  %xmm3, %xmm10                                 #851.905
        unpckhpd  %xmm3, %xmm8                                  #851.958
        movups    -16(%rdx,%r8), %xmm3                          #852.76
        movups    %xmm11, 16(%rsp)                              #839.13[spill]
        movaps    %xmm3, %xmm5                                  #852.804
        movups    -48(%rdx,%r8), %xmm11                         #850.71
        movups    -16(%rcx,%r8), %xmm6                          #852.237
        movaps    %xmm11, %xmm14                                #850.799
        movups    -16(%rsi,%r8), %xmm4                          #852.438
        movups    -48(%rcx,%r8), %xmm2                          #850.232
        movups    -16(%rdi,%r8), %xmm1                          #852.639
        unpcklpd  %xmm6, %xmm5                                  #852.804
        unpckhpd  %xmm6, %xmm3                                  #852.857
        movaps    %xmm4, %xmm6                                  #852.910
        movups    (%rdx,%r8), %xmm13                            #853.76
        unpcklpd  %xmm2, %xmm14                                 #850.799
        movups    %xmm14, 32(%rsp)                              #850.799[spill]
        unpckhpd  %xmm2, %xmm11                                 #850.852
        unpcklpd  %xmm1, %xmm6                                  #852.910
        unpckhpd  %xmm1, %xmm4                                  #852.963
        movaps    %xmm13, %xmm1                                 #853.804
        movups    (%rcx,%r8), %xmm2                             #853.237
        movups    (%rsi,%r8), %xmm14                            #853.438
        movups    (%rdi,%r8), %xmm15                            #853.639
        unpcklpd  %xmm2, %xmm1                                  #853.804
        unpckhpd  %xmm2, %xmm13                                 #853.857
        movaps    %xmm14, %xmm2                                 #853.910
        unpcklpd  %xmm15, %xmm2                                 #853.910
        unpckhpd  %xmm15, %xmm14                                #853.963
        cvtps2pd  %xmm0, %xmm15                                 #854.22
        movhlps   %xmm0, %xmm0                                  #854.79
        cvtps2pd  %xmm0, %xmm0                                  #854.65
        mulpd     %xmm15, %xmm13                                #855.34
        mulpd     %xmm0, %xmm14                                 #855.111
        addpd     %xmm1, %xmm13                                 #855.22
        addpd     %xmm2, %xmm14                                 #855.99
        mulpd     %xmm15, %xmm13                                #856.34
        mulpd     %xmm0, %xmm14                                 #856.111
        addpd     %xmm3, %xmm13                                 #856.22
        addpd     %xmm4, %xmm14                                 #856.99
        mulpd     %xmm15, %xmm13                                #857.34
        mulpd     %xmm0, %xmm14                                 #857.111
        addpd     %xmm5, %xmm13                                 #857.22
        addpd     %xmm6, %xmm14                                 #857.99
        mulpd     %xmm15, %xmm13                                #858.34
        mulpd     %xmm0, %xmm14                                 #858.111
        addpd     %xmm7, %xmm13                                 #858.22
        addpd     %xmm8, %xmm14                                 #858.99
        mulpd     %xmm15, %xmm13                                #859.34
        mulpd     %xmm0, %xmm14                                 #859.111
        addpd     %xmm9, %xmm13                                 #859.22
        addpd     %xmm10, %xmm14                                #859.99
        mulpd     %xmm15, %xmm13                                #860.30
        mulpd     %xmm0, %xmm14                                 #860.107
        addpd     %xmm11, %xmm13                                #860.18
        addpd     %xmm12, %xmm14                                #860.95
        mulpd     %xmm13, %xmm15                                #861.30
        mulpd     %xmm14, %xmm0                                 #861.107
        addpd     32(%rsp), %xmm15                              #861.18[spill]
        addpd     48(%rsp), %xmm0                               #861.95[spill]
        cvtpd2ps  %xmm15, %xmm2                                 #862.32
        cvtpd2ps  %xmm0, %xmm4                                  #862.64
        movlhps   %xmm4, %xmm2                                  #862.16
        orps      16(%rsp), %xmm2                               #863.10[spill]
        testl     %eax, %eax                                    #865.52
        jne       ..B6.3        # Prob 5%                       #865.52
                                # LOE rbx r12 r13 r14 r15 eax xmm2
..B6.2:                         # Preds ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movaps    %xmm2, %xmm0                                  #868.12
        movq      %rbp, %rsp                                    #868.12
        popq      %rbp                                          #868.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #868.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    (%rsp), %xmm1                                 #865.193[spill]
        movups    %xmm1, 64(%rsp)                               #865.193
        movups    %xmm2, 128(%rsp)                              #865.264
                                # LOE rbx r12 r13 r14 r15 eax
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #865.448
        movq      %r12, 8(%rsp)                                 #865.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #865.448
        movq      %r13, (%rsp)                                  #865.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #865.448
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #865.511
        jc        ..B6.10       # Prob 5%                       #865.511
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #865.464
        cmpl      $4, %r12d                                     #865.459
        jl        ..B6.7        # Prob 82%                      #865.459
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm2                              #865.666
        jmp       ..B6.2        # Prob 100%                     #865.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm2
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #865.540
        lea       128(%rsp,%r12,4), %rsi                        #865.540
..___tag_value___svml_tanhf4_ha_ex.154:
#       __svml_stanh_ha_cout_rare_internal(const float *, float *)
        call      __svml_stanh_ha_cout_rare_internal            #865.540
..___tag_value___svml_tanhf4_ha_ex.155:
        jmp       ..B6.8        # Prob 100%                     #865.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_tanhf4_ha_ex,@function
	.size	__svml_tanhf4_ha_ex,.-__svml_tanhf4_ha_ex
..LN__svml_tanhf4_ha_ex.5:
	.data
# -- End  __svml_tanhf4_ha_ex
	.text
.L_2__routine_start___svml_stanh_ha_cout_rare_internal_6:
# -- Begin  __svml_stanh_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_stanh_ha_cout_rare_internal
	.globl __svml_stanh_ha_cout_rare_internal
# --- __svml_stanh_ha_cout_rare_internal(const float *, float *)
__svml_stanh_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #572.1
        .byte     15                                            #582.9
        .byte     30                                            #582.9
        .byte     250                                           #582.9
	.cfi_startproc
..___tag_value___svml_stanh_ha_cout_rare_internal.157:
..L158:
                                                        #572.1
        lea       _imlsTanhHATab(%rip), %rdx                    #575.29
        movb      3(%rdi), %al                                  #575.67
        andb      $-128, %al                                    #575.67
        shrb      $7, %al                                       #575.67
        movzbl    %al, %ecx                                     #575.29
        movzwl    2(%rdi), %r8d                                 #576.32
        andl      $32640, %r8d                                  #576.32
        movl      (%rdx,%rcx,4), %eax                           #575.29
        cmpl      $32640, %r8d                                  #576.57
        je        ..B7.4        # Prob 16%                      #576.57
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B7.2:                         # Preds ..B7.4 ..B7.1
                                # Execution count [9.20e-01]
        movl      %eax, (%rsi)                                  #578.9
                                # LOE rbx rbp r12 r13 r14 r15
..B7.3:                         # Preds ..B7.2 ..B7.5
                                # Execution count [1.00e+00]
        xorl      %eax, %eax                                    #584.12
        ret                                                     #584.12
                                # LOE
..B7.4:                         # Preds ..B7.1
                                # Execution count [1.60e-01]: Infreq
        testl     $8388607, (%rdi)                              #576.88
        je        ..B7.2        # Prob 50%                      #576.116
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B7.5:                         # Preds ..B7.4
                                # Execution count [8.00e-02]: Infreq
        movss     (%rdi), %xmm0                                 #582.16
        addss     %xmm0, %xmm0                                  #582.23
        movss     %xmm0, (%rsi)                                 #582.9
        jmp       ..B7.3        # Prob 100%                     #582.9
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_stanh_ha_cout_rare_internal,@function
	.size	__svml_stanh_ha_cout_rare_internal,.-__svml_stanh_ha_cout_rare_internal
..LN__svml_stanh_ha_cout_rare_internal.6:
	.data
# -- End  __svml_stanh_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_stanh_ha_data_internal
	.globl __svml_stanh_ha_data_internal
__svml_stanh_ha_data_internal:
	.long	0
	.long	1030750208
	.long	1032847360
	.long	1034944512
	.long	1037041664
	.long	1039138816
	.long	1041235968
	.long	1043333120
	.long	1045430272
	.long	1047527424
	.long	1049624576
	.long	1051721728
	.long	1053818880
	.long	1055916032
	.long	1058013184
	.long	1060110336
	.long	1062207488
	.long	1064304640
	.long	1066401792
	.long	1068498944
	.long	1070596096
	.long	1072693248
	.long	1074790400
	.long	1076887552
	.long	1078984704
	.long	1081081856
	.long	1083179008
	.long	1085276160
	.long	1087373312
	.long	1089470464
	.long	1091567616
	.long	0
	.long	0
	.long	1030732233
	.long	1032831839
	.long	1034916201
	.long	1036994987
	.long	1039067209
	.long	1041174248
	.long	1043220868
	.long	1045245838
	.long	1047245614
	.long	1049383373
	.long	1051287907
	.long	1053115377
	.long	1054857013
	.long	1057129528
	.long	1058581488
	.long	1059832960
	.long	1060891676
	.long	1062153819
	.long	1063337043
	.long	1064100733
	.long	1064582223
	.long	1064984555
	.long	1065216645
	.long	1065302845
	.long	1065334668
	.long	1065349076
	.long	1065352656
	.long	1065353140
	.long	1065353206
	.long	1065353215
	.long	1065353216
	.long	0
	.long	2963361822
	.long	2971470750
	.long	2945658640
	.long	821708412
	.long	824483568
	.long	824941280
	.long	2984085072
	.long	2957298688
	.long	838449816
	.long	2966046080
	.long	2988320324
	.long	2989804564
	.long	842626356
	.long	3000013710
	.long	2972725824
	.long	3002017674
	.long	853753500
	.long	2987104448
	.long	3000350914
	.long	855535800
	.long	852410906
	.long	851608946
	.long	2988641656
	.long	2997011000
	.long	2989576736
	.long	3000884068
	.long	2999984336
	.long	840950056
	.long	2995215280
	.long	855269702
	.long	0
	.long	1065353216
	.long	1065295748
	.long	1065270545
	.long	1065229919
	.long	1065181343
	.long	1065124909
	.long	1065025765
	.long	1064867200
	.long	1064679597
	.long	1064464345
	.long	1064093083
	.long	1063517074
	.long	1062862743
	.long	1062146519
	.long	1060992371
	.long	1059386208
	.long	1057800167
	.long	1055660649
	.long	1051764737
	.long	1046959010
	.long	1041444634
	.long	1035462611
	.long	1026689093
	.long	1015337940
	.long	1002731447
	.long	990958554
	.long	973168670
	.long	948705851
	.long	924299482
	.long	899955662
	.long	864224966
	.long	0
	.long	2956213371
	.long	3178161821
	.long	3180268967
	.long	3182315389
	.long	3184339487
	.long	3186337805
	.long	3188474939
	.long	3190373619
	.long	3192189570
	.long	3193910865
	.long	3196176320
	.long	3197556682
	.long	3198679950
	.long	3199536798
	.long	3200331518
	.long	3200564882
	.long	3200049264
	.long	3199029518
	.long	3197040598
	.long	3192620804
	.long	3188208183
	.long	3182392393
	.long	3173916356
	.long	3162750726
	.long	3150176437
	.long	3138431708
	.long	3120650203
	.long	3096189170
	.long	3071783062
	.long	3047439278
	.long	3011707180
	.long	0
	.long	3198855845
	.long	3198879250
	.long	3198677023
	.long	3198476576
	.long	3198388151
	.long	3198245218
	.long	3197982711
	.long	3197594458
	.long	3197117197
	.long	3196587519
	.long	3195304371
	.long	3192667528
	.long	3189843074
	.long	3186330810
	.long	3177085101
	.long	1013669486
	.long	1032032579
	.long	1036132065
	.long	1038305199
	.long	1036774550
	.long	1033498413
	.long	1028927137
	.long	1021175553
	.long	1009568359
	.long	998361895
	.long	985691041
	.long	967585842
	.long	943363289
	.long	919210013
	.long	895139148
	.long	858471606
	.long	0
	.long	3077428921
	.long	3189516141
	.long	1008586543
	.long	1036101517
	.long	1033304453
	.long	1034073627
	.long	1036071831
	.long	1037235824
	.long	1039436298
	.long	1040631208
	.long	1041906362
	.long	1042793477
	.long	1043232976
	.long	1043086916
	.long	1042100375
	.long	1039444212
	.long	1034126600
	.long	1026638186
	.long	995501655
	.long	3165579977
	.long	3167654937
	.long	3165317828
	.long	3158960080
	.long	3148291549
	.long	3137354510
	.long	3124730373
	.long	3106670759
	.long	3082457650
	.long	3058305807
	.long	3034235241
	.long	2997581996
	.long	0
	.long	1040781545
	.long	1131811139
	.long	1097198812
	.long	3247503190
	.long	3230402941
	.long	3224086547
	.long	3212798938
	.long	1059790272
	.long	1053691997
	.long	1061317268
	.long	3134918084
	.long	1034173207
	.long	3176246152
	.long	3165561405
	.long	3174788493
	.long	3178015405
	.long	3178847213
	.long	3177176538
	.long	3171127099
	.long	3155996003
	.long	985352038
	.long	999682315
	.long	998398067
	.long	989522534
	.long	977926264
	.long	966355955
	.long	948911724
	.long	924561635
	.long	900244966
	.long	875993879
	.long	841254832
	.long	0
	.long	3155046246
	.long	1175181842
	.long	1138112751
	.long	3286309950
	.long	3267011817
	.long	3259619885
	.long	3246758786
	.long	1088248663
	.long	1078543936
	.long	1086795944
	.long	3205436942
	.long	1043392367
	.long	3198686087
	.long	3182586396
	.long	3174374999
	.long	3142320544
	.long	1008565243
	.long	1014115537
	.long	1016545052
	.long	1010017051
	.long	998649588
	.long	975680464
	.long	3124451591
	.long	3121544226
	.long	3112148751
	.long	3100159824
	.long	3082673659
	.long	3058641232
	.long	3034613169
	.long	3010665978
	.long	2975473412
	.long	0
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	1027604480
	.long	65011712
	.long	65011712
	.long	65011712
	.long	65011712
	.long	65011712
	.long	65011712
	.long	65011712
	.long	65011712
	.long	65011712
	.long	65011712
	.long	65011712
	.long	65011712
	.long	65011712
	.long	65011712
	.long	65011712
	.long	65011712
	.long	0
	.long	0
	.long	434973
	.long	1072693248
	.long	2462381979
	.long	3194875870
	.long	1922363613
	.long	3218429247
	.long	3436033793
	.long	3207031997
	.long	1164049177
	.long	1069640674
	.long	2766840751
	.long	3214171076
	.long	0
	.long	0
	.long	1640345390
	.long	3194496887
	.long	1662417413
	.long	1072693250
	.long	3716526453
	.long	3205098446
	.long	1853512117
	.long	3218426760
	.long	3156517427
	.long	3211911078
	.long	3184519871
	.long	1069779119
	.long	3280598482
	.long	3215502338
	.long	0
	.long	0
	.long	1962245523
	.long	3196442075
	.long	366239963
	.long	1072693255
	.long	3337913224
	.long	3206521562
	.long	1328356923
	.long	3218424220
	.long	1470245799
	.long	3212690354
	.long	922782103
	.long	1069841533
	.long	705136934
	.long	3215769608
	.long	0
	.long	0
	.long	917120191
	.long	3198018856
	.long	2270262052
	.long	1072693264
	.long	21785897
	.long	3207561752
	.long	3853373788
	.long	3218420654
	.long	3959915849
	.long	3213220134
	.long	839274685
	.long	1069902529
	.long	3478609944
	.long	3215984949
	.long	0
	.long	0
	.long	321264669
	.long	3199232231
	.long	3507921756
	.long	1072693279
	.long	855596455
	.long	3208292995
	.long	4197403487
	.long	3218416395
	.long	1260192796
	.long	3213688235
	.long	509545499
	.long	1069956190
	.long	4001843557
	.long	3216067072
	.long	0
	.long	0
	.long	2572895834
	.long	3200373196
	.long	4238319527
	.long	1072693307
	.long	1589084946
	.long	3209032256
	.long	323547252
	.long	3218410632
	.long	129829396
	.long	3214058556
	.long	2665301683
	.long	1070009663
	.long	3805267410
	.long	3216137363
	.long	0
	.long	0
	.long	1373918637
	.long	3199925337
	.long	2391440540
	.long	1072693299
	.long	3494583150
	.long	3208925835
	.long	2192964039
	.long	3218411256
	.long	579095213
	.long	3214044622
	.long	3432431090
	.long	1070009041
	.long	3870858437
	.long	3216138421
	.long	0
	.long	0
	.long	3062447777
	.long	1055926683
	.long	3334650904
	.long	1072692790
	.long	3497776375
	.long	1062371871
	.long	4014660983
	.long	3218436927
	.long	1708666466
	.long	3212333537
	.long	648260668
	.long	1069902577
	.long	1156520282
	.long	3216044909
	.long	0
	.long	0
	.long	4186264729
	.long	1058462985
	.long	3883474621
	.long	1072690745
	.long	4001630278
	.long	1065042042
	.long	484659484
	.long	3218507007
	.long	301873053
	.long	1066864880
	.long	2426783364
	.long	1069685380
	.long	3518509994
	.long	3215777524
	.long	0
	.long	0
	.long	1324317639
	.long	1061009204
	.long	1677646538
	.long	1072681882
	.long	781584286
	.long	1067165904
	.long	3649499968
	.long	3218726741
	.long	2264952365
	.long	1069102871
	.long	2344790854
	.long	1068835622
	.long	4047770869
	.long	3215138580
	.long	0
	.long	0
	.long	70848422
	.long	1063287485
	.long	1930391614
	.long	1072650795
	.long	586495590
	.long	1068891644
	.long	2415479819
	.long	3219189888
	.long	2049892606
	.long	1070582148
	.long	1783689851
	.long	3213584996
	.long	2396151379
	.long	3213355995
	.long	0
	.long	0
	.long	2764829776
	.long	1064683280
	.long	95861817
	.long	1072595436
	.long	350241294
	.long	1069957747
	.long	1429983818
	.long	3219518543
	.long	2046078110
	.long	1071248730
	.long	2818409407
	.long	3216573116
	.long	351621961
	.long	1065184929
	.long	0
	.long	0
	.long	818345493
	.long	1065579544
	.long	47166764
	.long	1072535009
	.long	2931635641
	.long	1070624305
	.long	2472163867
	.long	3219785146
	.long	898647657
	.long	1071677167
	.long	2840881315
	.long	3217227676
	.long	1213275070
	.long	1066490976
	.long	0
	.long	0
	.long	3770339094
	.long	1065664250
	.long	4021094867
	.long	1072525054
	.long	3250137669
	.long	1070683759
	.long	3067647579
	.long	3219831010
	.long	706412794
	.long	1071716084
	.long	3457985438
	.long	3217296958
	.long	693681995
	.long	1066592455
	.long	0
	.long	0
	.long	794345931
	.long	3214229553
	.long	674007974
	.long	1072761769
	.long	1339296402
	.long	3213866766
	.long	2063412275
	.long	3219199437
	.long	3042293216
	.long	1071038746
	.long	1218111703
	.long	3216613854
	.long	1828949834
	.long	1065778789
	.long	0
	.long	0
	.long	3709362262
	.long	3216572138
	.long	1704472411
	.long	1073083731
	.long	334125080
	.long	3219185499
	.long	3643953259
	.long	3216245823
	.long	972935809
	.long	1069563300
	.long	4262764539
	.long	3215188513
	.long	3947124972
	.long	1064363655
	.long	0
	.long	0
	.long	684725320
	.long	3217602215
	.long	2059930851
	.long	1073428282
	.long	6923247
	.long	3220175349
	.long	1962536238
	.long	1070738118
	.long	2626892535
	.long	3214818472
	.long	1541908021
	.long	3211168932
	.long	1264782098
	.long	1061514036
	.long	0
	.long	0
	.long	4193183898
	.long	3218211722
	.long	2527318106
	.long	1073704783
	.long	1779267795
	.long	3220520390
	.long	2178062862
	.long	1071649373
	.long	2371270354
	.long	3216802466
	.long	214503718
	.long	1066134183
	.long	2527651537
	.long	3209129722
	.long	0
	.long	0
	.long	1145099230
	.long	3217848868
	.long	1219675578
	.long	1073564173
	.long	3377824792
	.long	3220387400
	.long	1294161399
	.long	1071386209
	.long	535756989
	.long	3216499614
	.long	3414431292
	.long	1065769858
	.long	3872552752
	.long	3208765586
	.long	0
	.long	0
	.long	3432152680
	.long	3212471108
	.long	3481247728
	.long	1073111648
	.long	2087872556
	.long	3219843286
	.long	1539630695
	.long	1070713931
	.long	2045031161
	.long	3215666774
	.long	1438917333
	.long	1064738520
	.long	2997200424
	.long	3207590169
	.long	0
	.long	0
	.long	157024952
	.long	1070614475
	.long	1896115811
	.long	1072588717
	.long	1533634146
	.long	3219167457
	.long	3479089950
	.long	1069795336
	.long	294041664
	.long	3214609167
	.long	3323703207
	.long	1063520882
	.long	1200470279
	.long	3206092743
	.long	0
	.long	0
	.long	780145450
	.long	1071804775
	.long	3436973384
	.long	1071541223
	.long	1373298557
	.long	3217881162
	.long	616458359
	.long	1068360186
	.long	1012488256
	.long	3212939359
	.long	3381328826
	.long	1061569412
	.long	3619594050
	.long	3203906531
	.long	0
	.long	0
	.long	3555024088
	.long	1072352823
	.long	703965661
	.long	1069801815
	.long	68876051
	.long	3215985072
	.long	4285546012
	.long	1066131701
	.long	1692571309
	.long	3210444434
	.long	2250664999
	.long	1058874117
	.long	2757518980
	.long	3200902424
	.long	0
	.long	0
	.long	4088530245
	.long	1072580854
	.long	2571880719
	.long	1067895848
	.long	4091013897
	.long	3213873796
	.long	4246435429
	.long	1063770948
	.long	92905889
	.long	3207872058
	.long	248987709
	.long	1056074614
	.long	2369951583
	.long	3197898922
	.long	0
	.long	0
	.long	3580076556
	.long	1072660066
	.long	1353576036
	.long	1065860878
	.long	2410885661
	.long	3211602990
	.long	2989427096
	.long	1061369430
	.long	3886685439
	.long	3205273864
	.long	529712074
	.long	1053215589
	.long	3764845364
	.long	3194905549
	.long	0
	.long	0
	.long	660908647
	.long	1072688177
	.long	2675542798
	.long	1062777930
	.long	772498083
	.long	3208233517
	.long	377295306
	.long	1057798793
	.long	162648032
	.long	3201438006
	.long	623489458
	.long	1049119366
	.long	3651746243
	.long	3190506519
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
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
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	2130706432
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	1038090240
	.long	54525952
	.long	54525952
	.long	54525952
	.long	54525952
	.long	54525952
	.long	54525952
	.long	54525952
	.long	54525952
	.long	54525952
	.long	54525952
	.long	54525952
	.long	54525952
	.long	54525952
	.long	54525952
	.long	54525952
	.long	54525952
	.type	__svml_stanh_ha_data_internal,@object
	.size	__svml_stanh_ha_data_internal,3456
	.align 4
_imlsTanhHATab:
	.long	1065353216
	.long	3212836864
	.type	_imlsTanhHATab,@object
	.size	_imlsTanhHATab,8
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
