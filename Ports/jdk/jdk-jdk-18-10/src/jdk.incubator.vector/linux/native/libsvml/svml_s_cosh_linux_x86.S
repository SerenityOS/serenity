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
	.file "svml_s_cosh.c"
	.text
..TXTST0:
.L_2__routine_start___svml_coshf16_ha_z0_0:
# -- Begin  __svml_coshf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_coshf16_ha_z0
# --- __svml_coshf16_ha_z0(__m512)
__svml_coshf16_ha_z0:
# parameter 1: %zmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #417.1
        .byte     15                                            #497.760
        .byte     30                                            #497.760
        .byte     250                                           #497.760
	.cfi_startproc
..___tag_value___svml_coshf16_ha_z0.1:
..L2:
                                                          #417.1
        pushq     %rbp                                          #417.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #417.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #417.1
        subq      $192, %rsp                                    #417.1
        vmovups   1024+__svml_scosh_ha_data_internal(%rip), %zmm5 #460.48
        vmovups   384+__svml_scosh_ha_data_internal(%rip), %zmm7 #455.51
        vmovups   768+__svml_scosh_ha_data_internal(%rip), %zmm11 #465.17
        vmovups   896+__svml_scosh_ha_data_internal(%rip), %zmm8 #453.49
        vmovups   960+__svml_scosh_ha_data_internal(%rip), %zmm10 #454.49
        vmovups   640+__svml_scosh_ha_data_internal(%rip), %zmm3 #458.49
        vmovups   704+__svml_scosh_ha_data_internal(%rip), %zmm2 #459.49
        vmovups   __svml_scosh_ha_data_internal(%rip), %zmm13   #472.279
        vmovups   256+__svml_scosh_ha_data_internal(%rip), %zmm15 #474.280
        vmovups   128+__svml_scosh_ha_data_internal(%rip), %zmm14 #473.284
        vpternlogd $255, %zmm6, %zmm6, %zmm6                    #463.21
        vmovaps   %zmm0, %zmm4                                  #417.1
        vandnps   %zmm4, %zmm5, %zmm1                           #461.16
        vfmadd213ps {rn-sae}, %zmm7, %zmm1, %zmm11              #465.17
        vpcmpd    $1, 512+__svml_scosh_ha_data_internal(%rip), %zmm1, %k1 #463.88
        vpslld    $18, %zmm11, %zmm12                           #475.19
        vsubps    {rn-sae}, %zmm7, %zmm11, %zmm9                #466.19
        vpermt2ps 64+__svml_scosh_ha_data_internal(%rip), %zmm11, %zmm13 #472.279
        vpermt2ps 320+__svml_scosh_ha_data_internal(%rip), %zmm11, %zmm15 #474.280
        vpermt2ps 192+__svml_scosh_ha_data_internal(%rip), %zmm11, %zmm14 #473.284
        vpandnd   %zmm1, %zmm1, %zmm6{%k1}                      #463.21
        vfnmadd231ps {rn-sae}, %zmm8, %zmm9, %zmm1              #467.17
        vptestmd  %zmm6, %zmm6, %k0                             #464.33
        vfnmadd231ps {rn-sae}, %zmm10, %zmm9, %zmm1             #468.17
        kmovw     %k0, %esi                                     #464.33
        vmulps    {rn-sae}, %zmm1, %zmm1, %zmm0                 #469.20
        vmulps    {rn-sae}, %zmm0, %zmm2, %zmm2                 #489.28
        vmulps    {rn-sae}, %zmm0, %zmm3, %zmm0                 #491.25
        vfmadd213ps {rn-sae}, %zmm1, %zmm1, %zmm2               #490.26
        vpandd    1216+__svml_scosh_ha_data_internal(%rip), %zmm12, %zmm5 #477.17
        vpaddd    %zmm5, %zmm13, %zmm8                          #479.20
        vpsubd    %zmm5, %zmm15, %zmm7                          #485.20
        vpaddd    %zmm5, %zmm14, %zmm14                         #482.24
        vaddps    {rn-sae}, %zmm7, %zmm8, %zmm15                #488.20
        vsubps    {rn-sae}, %zmm7, %zmm8, %zmm6                 #487.20
        vfmadd213ps {rn-sae}, %zmm14, %zmm15, %zmm0             #492.27
        vfmadd213ps {rn-sae}, %zmm0, %zmm6, %zmm2               #493.27
        vaddps    {rn-sae}, %zmm7, %zmm2, %zmm1                 #494.29
        vaddps    {rn-sae}, %zmm8, %zmm1, %zmm0                 #495.28
        testl     %esi, %esi                                    #497.52
        jne       ..B1.3        # Prob 5%                       #497.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm4
..B1.2:                         # Preds ..B1.9 ..B1.7 ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #500.12
        popq      %rbp                                          #500.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #500.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #497.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm4
..B1.4:                         # Preds ..B1.3
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #497.249
        movl      %edx, %eax                                    #497.303
        orl       $8064, %eax                                   #497.303
        cmpl      %eax, %edx                                    #497.332
        je        ..B1.6        # Prob 78%                      #497.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm4
..B1.5:                         # Preds ..B1.4
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #497.344
        vldmxcsr  32(%rsp)                                      #497.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm4
..B1.6:                         # Preds ..B1.5 ..B1.4
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm4, 64(%rsp)                               #497.404
        vmovups   %zmm0, 128(%rsp)                              #497.478
        testl     %esi, %esi                                    #497.586
        jne       ..B1.11       # Prob 5%                       #497.586
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B1.7:                         # Preds ..B1.14 ..B1.6
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #497.932
        je        ..B1.2        # Prob 78%                      #497.932
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B1.8:                         # Preds ..B1.7
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #497.957
        movl      32(%rsp), %eax                                #497.957
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B1.9:                         # Preds ..B1.8
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #497.944
        orl       %edx, %eax                                    #497.944
        movl      %eax, 32(%rsp)                                #497.944
        vldmxcsr  32(%rsp)                                      #497.944
        jmp       ..B1.2        # Prob 100%                     #497.944
                                # LOE rbx r12 r13 r14 r15 zmm0
..B1.11:                        # Preds ..B1.6
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #497.668
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B1.18:                        # Preds ..B1.11
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
..B1.12:                        # Preds ..B1.13 ..B1.18
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #497.731
        jc        ..B1.15       # Prob 5%                       #497.731
                                # LOE rbx r15 r12d r13d r14d
..B1.13:                        # Preds ..B1.15 ..B1.12
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #497.684
        cmpl      $16, %ebx                                     #497.679
        jl        ..B1.12       # Prob 82%                      #497.679
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
        vmovups   128(%rsp), %zmm0                              #497.887
        jmp       ..B1.7        # Prob 100%                     #497.887
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B1.15:                        # Preds ..B1.12
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,4), %rdi                         #497.760
        lea       128(%rsp,%rbx,4), %rsi                        #497.760
..___tag_value___svml_coshf16_ha_z0.29:
#       __svml_scosh_ha_cout_rare_internal(const float *, float *)
        call      __svml_scosh_ha_cout_rare_internal            #497.760
..___tag_value___svml_coshf16_ha_z0.30:
        jmp       ..B1.13       # Prob 100%                     #497.760
        .align    16,0x90
                                # LOE r15 ebx r12d r13d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_coshf16_ha_z0,@function
	.size	__svml_coshf16_ha_z0,.-__svml_coshf16_ha_z0
..LN__svml_coshf16_ha_z0.0:
	.data
# -- End  __svml_coshf16_ha_z0
	.text
.L_2__routine_start___svml_coshf8_ha_e9_1:
# -- Begin  __svml_coshf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_coshf8_ha_e9
# --- __svml_coshf8_ha_e9(__m256)
__svml_coshf8_ha_e9:
# parameter 1: %ymm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #505.1
        .byte     15                                            #562.546
        .byte     30                                            #562.546
        .byte     250                                           #562.546
	.cfi_startproc
..___tag_value___svml_coshf8_ha_e9.32:
..L33:
                                                         #505.1
        pushq     %rbp                                          #505.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #505.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #505.1
        pushq     %r15                                          #505.1
        subq      $184, %rsp                                    #505.1
        lea       1484+__svml_scosh_ha_data_internal(%rip), %r10 #549.878
        vmovups   1024+__svml_scosh_ha_data_internal(%rip), %ymm14 #525.50
        vpxor     %xmm4, %xmm4, %xmm4                           #536.62
        vmovups   1280+__svml_scosh_ha_data_internal(%rip), %ymm12 #526.52
        vmovups   1408+__svml_scosh_ha_data_internal(%rip), %xmm5 #527.29
        vmovups   1344+__svml_scosh_ha_data_internal(%rip), %xmm11 #528.27
        vmovaps   %ymm0, %ymm13                                 #505.1
        vandnps   %ymm13, %ymm14, %ymm10                        #532.16
        vmulps    768+__svml_scosh_ha_data_internal(%rip), %ymm10, %ymm15 #533.28
        vaddps    %ymm15, %ymm12, %ymm9                         #533.13
        vpcmpgtd  %xmm5, %xmm10, %xmm7                          #535.27
        vextractf128 $1, %ymm10, %xmm8                          #534.107
        vpcmpgtd  %xmm5, %xmm8, %xmm6                           #535.104
        vpackssdw %xmm6, %xmm7, %xmm2                           #536.79
        vxorps    %ymm12, %ymm9, %ymm7                          #537.13
        vpacksswb %xmm4, %xmm2, %xmm0                           #536.62
        vpmovmskb %xmm0, %r11d                                  #536.43
        vpsubd    %xmm7, %xmm11, %xmm1                          #539.23
        vextractf128 $1, %ymm7, %xmm8                           #538.104
        vpsrld    $28, %xmm1, %xmm3                             #540.23
        vpslld    $4, %xmm3, %xmm14                             #541.23
        vpor      %xmm7, %xmm14, %xmm15                         #542.23
        vpsubd    %xmm8, %xmm11, %xmm5                          #539.92
        vpand     %xmm11, %xmm15, %xmm6                         #543.23
        vpsrld    $28, %xmm5, %xmm2                             #540.84
        vpslld    $4, %xmm2, %xmm4                              #541.79
        vpor      %xmm8, %xmm4, %xmm0                           #542.87
        vpand     %xmm11, %xmm0, %xmm5                          #543.96
        vpslld    $4, %xmm6, %xmm0                              #549.183
        vsubps    %ymm12, %ymm9, %ymm11                         #546.13
        vpslld    $4, %xmm5, %xmm15                             #549.245
        vmovd     %xmm0, %r9d                                   #549.308
        vpsubd    %xmm6, %xmm7, %xmm6                           #544.24
        vmovd     %xmm15, %ecx                                  #549.589
        vpsubd    %xmm5, %xmm8, %xmm5                           #544.90
        vpslld    $19, %xmm6, %xmm7                             #545.24
        vpslld    $19, %xmm5, %xmm8                             #545.87
        vmovd     -4(%r9,%r10), %xmm1                           #549.1863
        vmulps    896+__svml_scosh_ha_data_internal(%rip), %ymm11, %ymm9 #547.38
        vmulps    960+__svml_scosh_ha_data_internal(%rip), %ymm11, %ymm2 #548.35
        vsubps    %ymm9, %ymm10, %ymm12                         #547.13
        vpextrd   $1, %xmm0, %r8d                               #549.376
        vpextrd   $2, %xmm0, %edi                               #549.447
        vpextrd   $3, %xmm0, %esi                               #549.518
        vpextrd   $1, %xmm15, %edx                              #549.657
        vpextrd   $2, %xmm15, %eax                              #549.728
	.cfi_escape 0x10, 0x0f, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
        vpextrd   $3, %xmm15, %r15d                             #549.799
        vmovd     -4(%r8,%r10), %xmm3                           #549.1952
        vmovd     -4(%rdi,%r10), %xmm9                          #549.2062
        vmovd     -4(%rsi,%r10), %xmm10                         #549.2151
        vsubps    %ymm2, %ymm12, %ymm4                          #548.13
        vpunpcklqdq %xmm3, %xmm1, %xmm11                        #549.1843
        vmovd     -4(%rcx,%r10), %xmm2                          #549.2304
        vmovd     -4(%rdx,%r10), %xmm0                          #549.2393
        vmovd     -4(%rax,%r10), %xmm1                          #549.2503
        vmovd     -4(%r15,%r10), %xmm3                          #549.2592
        vpunpcklqdq %xmm10, %xmm9, %xmm12                       #549.2042
        vpunpcklqdq %xmm0, %xmm2, %xmm15                        #549.2284
        vpunpcklqdq %xmm3, %xmm1, %xmm9                         #549.2483
        vshufps   $136, %xmm12, %xmm11, %xmm14                  #549.1823
        vshufps   $136, %xmm9, %xmm15, %xmm10                   #549.2264
        vmovd     (%r9,%r10), %xmm11                            #550.1863
        vmovd     (%r8,%r10), %xmm12                            #550.1952
        vmovd     (%rdi,%r10), %xmm2                            #550.2062
        vmovd     (%rsi,%r10), %xmm0                            #550.2151
        vpunpcklqdq %xmm12, %xmm11, %xmm1                       #550.1843
        vmovd     (%rcx,%r10), %xmm11                           #550.2304
        vmovd     (%rdx,%r10), %xmm12                           #550.2393
        vmovd     (%r15,%r10), %xmm15                           #550.2592
        vpunpcklqdq %xmm0, %xmm2, %xmm9                         #550.2042
        vpunpcklqdq %xmm12, %xmm11, %xmm2                       #550.2284
        vinsertf128 $1, %xmm10, %ymm14, %ymm3                   #549.1759
        vmovd     (%rax,%r10), %xmm14                           #550.2503
        vpunpcklqdq %xmm15, %xmm14, %xmm0                       #550.2483
        vshufps   $136, %xmm9, %xmm1, %xmm10                    #550.1823
        vshufps   $136, %xmm0, %xmm2, %xmm1                     #550.2264
        vaddps    %ymm3, %ymm3, %ymm2                           #551.13
        vmovd     -8(%r8,%r10), %xmm0                           #553.1952
        vmovd     -8(%rcx,%r10), %xmm14                         #553.2304
        vmovd     -8(%rdx,%r10), %xmm15                         #553.2393
        vpunpcklqdq %xmm15, %xmm14, %xmm15                      #553.2284
        vmovd     -8(%r15,%r10), %xmm14                         #553.2592
        vinsertf128 $1, %xmm1, %ymm10, %ymm9                    #550.1759
        vmulps    %ymm4, %ymm9, %ymm11                          #552.30
        vmovd     -8(%rsi,%r10), %xmm9                          #553.2151
        vaddps    %ymm11, %ymm3, %ymm1                          #552.15
        vmovd     -8(%r9,%r10), %xmm3                           #553.1863
        vpunpcklqdq %xmm0, %xmm3, %xmm10                        #553.1843
        vmovd     -8(%rdi,%r10), %xmm3                          #553.2062
        vmulps    %ymm1, %ymm4, %ymm1                           #554.30
        vmovd     -8(%rax,%r10), %xmm0                          #553.2503
        vpunpcklqdq %xmm9, %xmm3, %xmm11                        #553.2042
        vpunpcklqdq %xmm14, %xmm0, %xmm3                        #553.2483
        vshufps   $136, %xmm11, %xmm10, %xmm12                  #553.1823
        vshufps   $136, %xmm3, %xmm15, %xmm9                    #553.2264
        vmovd     -12(%r8,%r10), %xmm3                          #555.1952
        vmovd     -12(%rcx,%r10), %xmm15                        #555.2304
        vinsertf128 $1, %xmm9, %ymm12, %ymm10                   #553.1759
        vaddps    %ymm1, %ymm10, %ymm0                          #554.15
        vmovd     -12(%r9,%r10), %xmm1                          #555.1863
        vpunpcklqdq %xmm3, %xmm1, %xmm11                        #555.1843
        vmovd     -12(%rdi,%r10), %xmm9                         #555.2062
        vmovd     -12(%rsi,%r10), %xmm10                        #555.2151
        vmovd     -12(%rdx,%r10), %xmm1                         #555.2393
        vmulps    %ymm0, %ymm4, %ymm4                           #556.30
        vpunpcklqdq %xmm10, %xmm9, %xmm12                       #555.2042
        vpunpcklqdq %xmm1, %xmm15, %xmm9                        #555.2284
        vmovd     -12(%rax,%r10), %xmm15                        #555.2503
        vmovd     -12(%r15,%r10), %xmm3                         #555.2592
        vpunpcklqdq %xmm3, %xmm15, %xmm10                       #555.2483
        vshufps   $136, %xmm12, %xmm11, %xmm14                  #555.1823
        vshufps   $136, %xmm10, %xmm9, %xmm11                   #555.2264
        vinsertf128 $1, %xmm11, %ymm14, %ymm12                  #555.1759
        vaddps    %ymm4, %ymm12, %ymm0                          #556.15
        vaddps    %ymm0, %ymm2, %ymm1                           #557.15
        vextractf128 $1, %ymm1, %xmm2                           #558.106
        vpaddd    %xmm7, %xmm1, %xmm1                           #560.60
        vpaddd    %xmm8, %xmm2, %xmm0                           #560.95
        vinsertf128 $1, %xmm0, %ymm1, %ymm0                     #560.14
        testb     %r11b, %r11b                                  #562.52
        jne       ..B2.3        # Prob 5%                       #562.52
                                # LOE rbx r12 r13 r14 r11d ymm0 ymm13
..B2.2:                         # Preds ..B2.3 ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        addq      $184, %rsp                                    #565.12
	.cfi_restore 15
        popq      %r15                                          #565.12
        movq      %rbp, %rsp                                    #565.12
        popq      %rbp                                          #565.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #565.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
	.cfi_escape 0x10, 0x0f, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm13, 64(%rsp)                              #562.196
        vmovups   %ymm0, 128(%rsp)                              #562.270
        testl     %r11d, %r11d                                  #562.374
        je        ..B2.2        # Prob 95%                      #562.374
                                # LOE rbx r12 r13 r14 r11d ymm0
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %r15d, %r15d                                  #562.454
                                # LOE rbx r12 r13 r14 r15 r11d
..B2.13:                        # Preds ..B2.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %r11d, %r12d                                  #
                                # LOE rbx r13 r14 r15 r12d
..B2.7:                         # Preds ..B2.8 ..B2.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r15d, %r12d                                  #562.517
        jc        ..B2.10       # Prob 5%                       #562.517
                                # LOE rbx r13 r14 r15 r12d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r15d                                         #562.470
        cmpl      $8, %r15d                                     #562.465
        jl        ..B2.7        # Prob 82%                      #562.465
                                # LOE rbx r13 r14 r15 r12d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      (%rsp), %r12                                  #[spill]
	.cfi_restore 12
        vmovups   128(%rsp), %ymm0                              #562.675
        jmp       ..B2.2        # Prob 100%                     #562.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 ymm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r15,4), %rdi                         #562.546
        lea       128(%rsp,%r15,4), %rsi                        #562.546
..___tag_value___svml_coshf8_ha_e9.48:
#       __svml_scosh_ha_cout_rare_internal(const float *, float *)
        call      __svml_scosh_ha_cout_rare_internal            #562.546
..___tag_value___svml_coshf8_ha_e9.49:
        jmp       ..B2.8        # Prob 100%                     #562.546
        .align    16,0x90
                                # LOE rbx r13 r14 r12d r15d
	.cfi_endproc
# mark_end;
	.type	__svml_coshf8_ha_e9,@function
	.size	__svml_coshf8_ha_e9,.-__svml_coshf8_ha_e9
..LN__svml_coshf8_ha_e9.1:
	.data
# -- End  __svml_coshf8_ha_e9
	.text
.L_2__routine_start___svml_coshf4_ha_l9_2:
# -- Begin  __svml_coshf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_coshf4_ha_l9
# --- __svml_coshf4_ha_l9(__m128)
__svml_coshf4_ha_l9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #570.1
        .byte     15                                            #627.540
        .byte     30                                            #627.540
        .byte     250                                           #627.540
	.cfi_startproc
..___tag_value___svml_coshf4_ha_l9.51:
..L52:
                                                         #570.1
        pushq     %rbp                                          #570.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #570.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #570.1
        subq      $192, %rsp                                    #570.1
        vmovaps   %xmm0, %xmm5                                  #570.1
        vmovups   1024+__svml_scosh_ha_data_internal(%rip), %xmm6 #590.47
        lea       1484+__svml_scosh_ha_data_internal(%rip), %r8 #614.509
        vmovups   1280+__svml_scosh_ha_data_internal(%rip), %xmm14 #591.49
        vandnps   %xmm5, %xmm6, %xmm4                           #597.16
        vmovups   768+__svml_scosh_ha_data_internal(%rip), %xmm13 #598.13
        vfmadd213ps %xmm14, %xmm4, %xmm13                       #598.13
        vmovups   1344+__svml_scosh_ha_data_internal(%rip), %xmm12 #593.23
        vmovups   896+__svml_scosh_ha_data_internal(%rip), %xmm15 #594.48
        vpcmpgtd  1408+__svml_scosh_ha_data_internal(%rip), %xmm4, %xmm7 #600.23
        vmovmskps %xmm7, %eax                                   #601.44
        vxorps    %xmm14, %xmm13, %xmm3                         #602.13
        vsubps    %xmm14, %xmm13, %xmm0                         #611.13
        vpsubd    %xmm3, %xmm12, %xmm8                          #604.19
        vpsrld    $28, %xmm8, %xmm9                             #605.19
        vpslld    $4, %xmm9, %xmm10                             #606.19
        vpor      %xmm3, %xmm10, %xmm11                         #607.19
        vfnmadd231ps %xmm15, %xmm0, %xmm4                       #612.13
        vpand     %xmm12, %xmm11, %xmm2                         #608.19
        vpslld    $4, %xmm2, %xmm1                              #614.179
        vpsubd    %xmm2, %xmm3, %xmm2                           #609.20
        vpslld    $19, %xmm2, %xmm3                             #610.20
        vmovd     %xmm1, %edx                                   #614.238
        vfnmadd231ps 960+__svml_scosh_ha_data_internal(%rip), %xmm0, %xmm4 #613.13
        vmovd     -4(%rdx,%r8), %xmm6                           #614.986
        vmovd     (%rdx,%r8), %xmm12                            #615.986
        vpextrd   $1, %xmm1, %ecx                               #614.302
        vpextrd   $2, %xmm1, %esi                               #614.369
        vpextrd   $3, %xmm1, %edi                               #614.436
        vmovd     -4(%rcx,%r8), %xmm7                           #614.1069
        vmovd     (%rcx,%r8), %xmm13                            #615.1069
        vmovd     -4(%rsi,%r8), %xmm8                           #614.1173
        vmovd     (%rsi,%r8), %xmm14                            #615.1173
        vmovd     -4(%rdi,%r8), %xmm9                           #614.1256
        vmovd     (%rdi,%r8), %xmm15                            #615.1256
        vpunpcklqdq %xmm7, %xmm6, %xmm10                        #614.966
        vpunpcklqdq %xmm9, %xmm8, %xmm11                        #614.1153
        vpunpcklqdq %xmm13, %xmm12, %xmm0                       #615.966
        vpunpcklqdq %xmm15, %xmm14, %xmm1                       #615.1153
        vshufps   $136, %xmm11, %xmm10, %xmm6                   #614.946
        vshufps   $136, %xmm1, %xmm0, %xmm0                     #615.946
        vaddps    %xmm6, %xmm6, %xmm1                           #616.13
        vfmadd213ps %xmm6, %xmm4, %xmm0                         #617.15
        vmovd     -8(%rdi,%r8), %xmm10                          #618.1256
        vmovd     -8(%rdx,%r8), %xmm7                           #618.986
        vmovd     -8(%rcx,%r8), %xmm8                           #618.1069
        vmovd     -8(%rsi,%r8), %xmm9                           #618.1173
        vpunpcklqdq %xmm8, %xmm7, %xmm11                        #618.966
        vpunpcklqdq %xmm10, %xmm9, %xmm12                       #618.1153
        vshufps   $136, %xmm12, %xmm11, %xmm13                  #618.946
        vfmadd213ps %xmm13, %xmm4, %xmm0                        #619.15
        vmovd     -12(%rsi,%r8), %xmm6                          #620.1173
        vmovd     -12(%rdx,%r8), %xmm14                         #620.986
        vmovd     -12(%rcx,%r8), %xmm15                         #620.1069
        vmovd     -12(%rdi,%r8), %xmm7                          #620.1256
        vpunpcklqdq %xmm15, %xmm14, %xmm8                       #620.966
        vpunpcklqdq %xmm7, %xmm6, %xmm9                         #620.1153
        vshufps   $136, %xmm9, %xmm8, %xmm10                    #620.946
        vfmadd213ps %xmm10, %xmm4, %xmm0                        #621.15
        vaddps    %xmm0, %xmm1, %xmm4                           #622.15
        vpaddd    %xmm3, %xmm4, %xmm0                           #625.14
        testl     %eax, %eax                                    #627.52
        jne       ..B3.3        # Prob 5%                       #627.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm5
..B3.2:                         # Preds ..B3.3 ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #630.12
        popq      %rbp                                          #630.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #630.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm5, 64(%rsp)                               #627.193
        vmovups   %xmm0, 128(%rsp)                              #627.264
        je        ..B3.2        # Prob 95%                      #627.368
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #627.448
        movq      %r12, 8(%rsp)                                 #627.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #627.448
        movq      %r13, (%rsp)                                  #627.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #627.448
                                # LOE rbx r12 r14 r15 r13d
..B3.7:                         # Preds ..B3.8 ..B3.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #627.511
        jc        ..B3.10       # Prob 5%                       #627.511
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #627.464
        cmpl      $4, %r12d                                     #627.459
        jl        ..B3.7        # Prob 82%                      #627.459
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #627.666
        jmp       ..B3.2        # Prob 100%                     #627.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #627.540
        lea       128(%rsp,%r12,4), %rsi                        #627.540
..___tag_value___svml_coshf4_ha_l9.69:
#       __svml_scosh_ha_cout_rare_internal(const float *, float *)
        call      __svml_scosh_ha_cout_rare_internal            #627.540
..___tag_value___svml_coshf4_ha_l9.70:
        jmp       ..B3.8        # Prob 100%                     #627.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_coshf4_ha_l9,@function
	.size	__svml_coshf4_ha_l9,.-__svml_coshf4_ha_l9
..LN__svml_coshf4_ha_l9.2:
	.data
# -- End  __svml_coshf4_ha_l9
	.text
.L_2__routine_start___svml_coshf8_ha_l9_3:
# -- Begin  __svml_coshf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_coshf8_ha_l9
# --- __svml_coshf8_ha_l9(__m256)
__svml_coshf8_ha_l9:
# parameter 1: %ymm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #635.1
        .byte     15                                            #692.546
        .byte     30                                            #692.546
        .byte     250                                           #692.546
	.cfi_startproc
..___tag_value___svml_coshf8_ha_l9.72:
..L73:
                                                         #635.1
        pushq     %rbp                                          #635.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #635.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #635.1
        pushq     %r15                                          #635.1
        subq      $184, %rsp                                    #635.1
        lea       1484+__svml_scosh_ha_data_internal(%rip), %r10 #679.1007
        vmovups   1024+__svml_scosh_ha_data_internal(%rip), %ymm7 #655.50
        vmovups   1280+__svml_scosh_ha_data_internal(%rip), %ymm15 #656.52
        vmovups   768+__svml_scosh_ha_data_internal(%rip), %ymm14 #663.13
        vmovups   1344+__svml_scosh_ha_data_internal(%rip), %ymm13 #658.23
        vmovaps   %ymm0, %ymm6                                  #635.1
        vandnps   %ymm6, %ymm7, %ymm5                           #662.16
        vfmadd213ps %ymm15, %ymm5, %ymm14                       #663.13
        vxorps    %ymm15, %ymm14, %ymm4                         #667.13
        vpsubd    %ymm4, %ymm13, %ymm9                          #669.19
        vsubps    %ymm15, %ymm14, %ymm0                         #676.13
        vpsrld    $28, %ymm9, %ymm10                            #670.19
        vpslld    $4, %ymm10, %ymm11                            #671.19
        vpor      %ymm4, %ymm11, %ymm12                         #672.19
        vpand     %ymm13, %ymm12, %ymm3                         #673.19
        vpslld    $4, %ymm3, %ymm1                              #679.179
        vpsubd    %ymm3, %ymm4, %ymm3                           #674.20
        vpcmpgtd  1408+__svml_scosh_ha_data_internal(%rip), %ymm5, %ymm8 #665.23
        vfnmadd231ps 896+__svml_scosh_ha_data_internal(%rip), %ymm0, %ymm5 #677.13
        vpslld    $19, %ymm3, %ymm4                             #675.20
        vfnmadd231ps 960+__svml_scosh_ha_data_internal(%rip), %ymm0, %ymm5 #678.13
        vmovmskps %ymm8, %r11d                                  #666.45
        vextractf128 $1, %ymm1, %xmm13                          #679.629
        vmovd     %xmm1, %r9d                                   #679.241
        vmovd     %xmm13, %ecx                                  #679.610
        vmovd     -4(%r9,%r10), %xmm2                           #679.1992
        vpextrd   $1, %xmm1, %r8d                               #679.331
        vpextrd   $2, %xmm1, %edi                               #679.424
        vpextrd   $3, %xmm1, %esi                               #679.517
        vpextrd   $1, %xmm13, %edx                              #679.705
        vpextrd   $2, %xmm13, %eax                              #679.803
	.cfi_escape 0x10, 0x0f, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
        vpextrd   $3, %xmm13, %r15d                             #679.901
        vmovd     -4(%r8,%r10), %xmm7                           #679.2081
        vmovd     -4(%rdi,%r10), %xmm8                          #679.2191
        vmovd     -4(%rsi,%r10), %xmm9                          #679.2280
        vmovd     -4(%rcx,%r10), %xmm14                         #679.2433
        vmovd     -4(%rdx,%r10), %xmm15                         #679.2522
        vmovd     -4(%rax,%r10), %xmm0                          #679.2632
        vmovd     -4(%r15,%r10), %xmm1                          #679.2721
        vpunpcklqdq %xmm7, %xmm2, %xmm10                        #679.1972
        vpunpcklqdq %xmm9, %xmm8, %xmm11                        #679.2171
        vpunpcklqdq %xmm15, %xmm14, %xmm2                       #679.2413
        vpunpcklqdq %xmm1, %xmm0, %xmm13                        #679.2612
        vshufps   $136, %xmm11, %xmm10, %xmm12                  #679.1952
        vshufps   $136, %xmm13, %xmm2, %xmm7                    #679.2393
        vmovd     (%r9,%r10), %xmm8                             #680.1992
        vmovd     (%r8,%r10), %xmm9                             #680.2081
        vmovd     (%rdi,%r10), %xmm10                           #680.2191
        vmovd     (%rsi,%r10), %xmm11                           #680.2280
        vpunpcklqdq %xmm9, %xmm8, %xmm14                        #680.1972
        vmovd     (%rcx,%r10), %xmm1                            #680.2433
        vmovd     (%rax,%r10), %xmm8                            #680.2632
        vmovd     (%r15,%r10), %xmm9                            #680.2721
        vpunpcklqdq %xmm11, %xmm10, %xmm15                      #680.2171
        vpunpcklqdq %xmm9, %xmm8, %xmm11                        #680.2612
        vshufps   $136, %xmm15, %xmm14, %xmm0                   #680.1952
        vmovd     -8(%r8,%r10), %xmm13                          #683.2081
        vmovd     -8(%rdi,%r10), %xmm14                         #683.2191
        vmovd     -8(%rsi,%r10), %xmm15                         #683.2280
        vmovd     -8(%rcx,%r10), %xmm9                          #683.2433
        vinsertf128 $1, %xmm7, %ymm12, %ymm2                    #679.1888
        vmovd     (%rdx,%r10), %xmm7                            #680.2522
        vpunpcklqdq %xmm7, %xmm1, %xmm10                        #680.2413
        vshufps   $136, %xmm11, %xmm10, %xmm12                  #680.2393
        vaddps    %ymm2, %ymm2, %ymm1                           #681.13
        vmovd     -8(%rdx,%r10), %xmm10                         #683.2522
        vmovd     -8(%rax,%r10), %xmm11                         #683.2632
        vpunpcklqdq %xmm15, %xmm14, %xmm7                       #683.2171
        vinsertf128 $1, %xmm12, %ymm0, %ymm0                    #680.1888
        vfmadd213ps %ymm2, %ymm5, %ymm0                         #682.15
        vmovd     -8(%r9,%r10), %xmm2                           #683.1992
        vmovd     -8(%r15,%r10), %xmm12                         #683.2721
        vpunpcklqdq %xmm13, %xmm2, %xmm2                        #683.1972
        vpunpcklqdq %xmm10, %xmm9, %xmm13                       #683.2413
        vpunpcklqdq %xmm12, %xmm11, %xmm14                      #683.2612
        vshufps   $136, %xmm7, %xmm2, %xmm8                     #683.1952
        vshufps   $136, %xmm14, %xmm13, %xmm15                  #683.2393
        vmovd     -12(%rcx,%r10), %xmm14                        #685.2433
        vmovd     -12(%r9,%r10), %xmm7                          #685.1992
        vmovd     -12(%rdi,%r10), %xmm9                         #685.2191
        vmovd     -12(%rsi,%r10), %xmm10                        #685.2280
        vpunpcklqdq %xmm10, %xmm9, %xmm12                       #685.2171
        vinsertf128 $1, %xmm15, %ymm8, %ymm2                    #683.1888
        vmovd     -12(%rdx,%r10), %xmm15                        #685.2522
        vfmadd213ps %ymm2, %ymm5, %ymm0                         #684.15
        vmovd     -12(%r8,%r10), %xmm8                          #685.2081
        vpunpcklqdq %xmm15, %xmm14, %xmm2                       #685.2413
        vmovd     -12(%rax,%r10), %xmm14                        #685.2632
        vmovd     -12(%r15,%r10), %xmm15                        #685.2721
        vpunpcklqdq %xmm8, %xmm7, %xmm11                        #685.1972
        vpunpcklqdq %xmm15, %xmm14, %xmm7                       #685.2612
        vshufps   $136, %xmm12, %xmm11, %xmm13                  #685.1952
        vshufps   $136, %xmm7, %xmm2, %xmm8                     #685.2393
        vinsertf128 $1, %xmm8, %ymm13, %ymm9                    #685.1888
        vfmadd213ps %ymm9, %ymm5, %ymm0                         #686.15
        vaddps    %ymm0, %ymm1, %ymm5                           #687.15
        vpaddd    %ymm4, %ymm5, %ymm0                           #689.15
        testl     %r11d, %r11d                                  #692.52
        jne       ..B4.3        # Prob 5%                       #692.52
                                # LOE rbx r12 r13 r14 r11d ymm0 ymm6
..B4.2:                         # Preds ..B4.3 ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        addq      $184, %rsp                                    #695.12
	.cfi_restore 15
        popq      %r15                                          #695.12
        movq      %rbp, %rsp                                    #695.12
        popq      %rbp                                          #695.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #695.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
	.cfi_escape 0x10, 0x0f, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm6, 64(%rsp)                               #692.196
        vmovups   %ymm0, 128(%rsp)                              #692.270
        je        ..B4.2        # Prob 95%                      #692.374
                                # LOE rbx r12 r13 r14 r11d ymm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %r15d, %r15d                                  #692.454
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
        btl       %r15d, %r12d                                  #692.517
        jc        ..B4.10       # Prob 5%                       #692.517
                                # LOE rbx r13 r14 r15 r12d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r15d                                         #692.470
        cmpl      $8, %r15d                                     #692.465
        jl        ..B4.7        # Prob 82%                      #692.465
                                # LOE rbx r13 r14 r15 r12d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      (%rsp), %r12                                  #[spill]
	.cfi_restore 12
        vmovups   128(%rsp), %ymm0                              #692.675
        jmp       ..B4.2        # Prob 100%                     #692.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 ymm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r15,4), %rdi                         #692.546
        lea       128(%rsp,%r15,4), %rsi                        #692.546
..___tag_value___svml_coshf8_ha_l9.88:
#       __svml_scosh_ha_cout_rare_internal(const float *, float *)
        call      __svml_scosh_ha_cout_rare_internal            #692.546
..___tag_value___svml_coshf8_ha_l9.89:
        jmp       ..B4.8        # Prob 100%                     #692.546
        .align    16,0x90
                                # LOE rbx r13 r14 r12d r15d
	.cfi_endproc
# mark_end;
	.type	__svml_coshf8_ha_l9,@function
	.size	__svml_coshf8_ha_l9,.-__svml_coshf8_ha_l9
..LN__svml_coshf8_ha_l9.3:
	.data
# -- End  __svml_coshf8_ha_l9
	.text
.L_2__routine_start___svml_coshf4_ha_ex_4:
# -- Begin  __svml_coshf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_coshf4_ha_ex
# --- __svml_coshf4_ha_ex(__m128)
__svml_coshf4_ha_ex:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #700.1
        .byte     15                                            #757.540
        .byte     30                                            #757.540
        .byte     250                                           #757.540
	.cfi_startproc
..___tag_value___svml_coshf4_ha_ex.91:
..L92:
                                                         #700.1
        pushq     %rbp                                          #700.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #700.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #700.1
        subq      $192, %rsp                                    #700.1
        lea       1484+__svml_scosh_ha_data_internal(%rip), %r8 #744.614
        movups    1024+__svml_scosh_ha_data_internal(%rip), %xmm4 #727.16
        movups    768+__svml_scosh_ha_data_internal(%rip), %xmm9 #728.25
        andnps    %xmm0, %xmm4                                  #727.16
        mulps     %xmm4, %xmm9                                  #728.25
        movaps    %xmm4, %xmm5                                  #730.21
        movups    1280+__svml_scosh_ha_data_internal(%rip), %xmm7 #721.49
        movaps    %xmm7, %xmm3                                  #732.13
        addps     %xmm7, %xmm9                                  #728.13
        pxor      %xmm9, %xmm3                                  #732.13
        subps     %xmm7, %xmm9                                  #741.13
        movdqu    1344+__svml_scosh_ha_data_internal(%rip), %xmm6 #723.56
        movdqa    %xmm6, %xmm2                                  #734.17
        psubd     %xmm3, %xmm2                                  #734.17
        movups    896+__svml_scosh_ha_data_internal(%rip), %xmm8 #742.35
        psrld     $28, %xmm2                                    #735.17
        mulps     %xmm9, %xmm8                                  #742.35
        pslld     $4, %xmm2                                     #736.17
        por       %xmm3, %xmm2                                  #737.17
        subps     %xmm8, %xmm4                                  #742.13
        pand      %xmm6, %xmm2                                  #738.17
        movups    960+__svml_scosh_ha_data_internal(%rip), %xmm10 #743.32
        movdqa    %xmm2, %xmm13                                 #744.177
        mulps     %xmm9, %xmm10                                 #743.32
        pslld     $4, %xmm13                                    #744.177
        movd      %xmm13, %edx                                  #744.234
        pshufd    $1, %xmm13, %xmm11                            #744.315
        psubd     %xmm2, %xmm3                                  #739.18
        pshufd    $2, %xmm13, %xmm12                            #744.419
        pslld     $19, %xmm3                                    #740.18
        pshufd    $3, %xmm13, %xmm14                            #744.523
        subps     %xmm10, %xmm4                                 #743.13
        movd      %xmm11, %ecx                                  #744.296
        movd      %xmm12, %esi                                  #744.400
        movd      %xmm14, %edi                                  #744.504
        movd      (%rdx,%r8), %xmm10                            #745.1090
        movd      (%rcx,%r8), %xmm7                             #745.1173
        movd      (%rsi,%r8), %xmm9                             #745.1277
        movd      (%rdi,%r8), %xmm8                             #745.1360
        punpcklqdq %xmm7, %xmm10                                #745.1071
        punpcklqdq %xmm8, %xmm9                                 #745.1257
        shufps    $136, %xmm9, %xmm10                           #745.1051
        pcmpgtd   1408+__svml_scosh_ha_data_internal(%rip), %xmm5 #730.21
        mulps     %xmm4, %xmm10                                 #747.27
        movmskps  %xmm5, %eax                                   #731.44
        movd      -4(%rdx,%r8), %xmm1                           #744.1090
        movd      -4(%rcx,%r8), %xmm15                          #744.1173
        movd      -4(%rsi,%r8), %xmm6                           #744.1277
        movd      -4(%rdi,%r8), %xmm5                           #744.1360
        punpcklqdq %xmm15, %xmm1                                #744.1071
        punpcklqdq %xmm5, %xmm6                                 #744.1257
        shufps    $136, %xmm6, %xmm1                            #744.1051
        movaps    %xmm1, %xmm7                                  #746.13
        movd      -8(%rdi,%r8), %xmm12                          #748.1360
        addps     %xmm1, %xmm7                                  #746.13
        addps     %xmm10, %xmm1                                 #747.15
        mulps     %xmm4, %xmm1                                  #749.27
        movd      -8(%rdx,%r8), %xmm5                           #748.1090
        movd      -8(%rcx,%r8), %xmm11                          #748.1173
        movd      -8(%rsi,%r8), %xmm13                          #748.1277
        punpcklqdq %xmm11, %xmm5                                #748.1071
        punpcklqdq %xmm12, %xmm13                               #748.1257
        shufps    $136, %xmm13, %xmm5                           #748.1051
        movd      -12(%rdx,%r8), %xmm6                          #750.1090
        addps     %xmm1, %xmm5                                  #749.15
        mulps     %xmm5, %xmm4                                  #751.27
        movd      -12(%rcx,%r8), %xmm1                          #750.1173
        punpcklqdq %xmm1, %xmm6                                 #750.1071
        movd      -12(%rsi,%r8), %xmm1                          #750.1277
        movd      -12(%rdi,%r8), %xmm14                         #750.1360
        punpcklqdq %xmm14, %xmm1                                #750.1257
        shufps    $136, %xmm1, %xmm6                            #750.1051
        addps     %xmm4, %xmm6                                  #751.15
        addps     %xmm6, %xmm7                                  #752.15
        paddd     %xmm3, %xmm7                                  #755.14
        testl     %eax, %eax                                    #757.52
        jne       ..B5.3        # Prob 5%                       #757.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm7
..B5.2:                         # Preds ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movaps    %xmm7, %xmm0                                  #760.12
        movq      %rbp, %rsp                                    #760.12
        popq      %rbp                                          #760.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #760.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #757.193
        movups    %xmm7, 128(%rsp)                              #757.264
                                # LOE rbx r12 r13 r14 r15 eax
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #757.448
        movq      %r12, 8(%rsp)                                 #757.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #757.448
        movq      %r13, (%rsp)                                  #757.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #757.448
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #757.511
        jc        ..B5.10       # Prob 5%                       #757.511
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #757.464
        cmpl      $4, %r12d                                     #757.459
        jl        ..B5.7        # Prob 82%                      #757.459
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm7                              #757.666
        jmp       ..B5.2        # Prob 100%                     #757.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm7
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #757.540
        lea       128(%rsp,%r12,4), %rsi                        #757.540
..___tag_value___svml_coshf4_ha_ex.109:
#       __svml_scosh_ha_cout_rare_internal(const float *, float *)
        call      __svml_scosh_ha_cout_rare_internal            #757.540
..___tag_value___svml_coshf4_ha_ex.110:
        jmp       ..B5.8        # Prob 100%                     #757.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_coshf4_ha_ex,@function
	.size	__svml_coshf4_ha_ex,.-__svml_coshf4_ha_ex
..LN__svml_coshf4_ha_ex.4:
	.data
# -- End  __svml_coshf4_ha_ex
	.text
.L_2__routine_start___svml_coshf4_ha_e9_5:
# -- Begin  __svml_coshf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_coshf4_ha_e9
# --- __svml_coshf4_ha_e9(__m128)
__svml_coshf4_ha_e9:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #765.1
        .byte     15                                            #822.540
        .byte     30                                            #822.540
        .byte     250                                           #822.540
	.cfi_startproc
..___tag_value___svml_coshf4_ha_e9.112:
..L113:
                                                        #765.1
        pushq     %rbp                                          #765.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #765.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #765.1
        subq      $192, %rsp                                    #765.1
        lea       1484+__svml_scosh_ha_data_internal(%rip), %r8 #809.533
        movups    1024+__svml_scosh_ha_data_internal(%rip), %xmm4 #792.16
        movups    768+__svml_scosh_ha_data_internal(%rip), %xmm9 #793.25
        andnps    %xmm0, %xmm4                                  #792.16
        mulps     %xmm4, %xmm9                                  #793.25
        movaps    %xmm4, %xmm5                                  #795.27
        movups    1280+__svml_scosh_ha_data_internal(%rip), %xmm7 #786.49
        movaps    %xmm7, %xmm3                                  #797.13
        addps     %xmm7, %xmm9                                  #793.13
        pxor      %xmm9, %xmm3                                  #797.13
        subps     %xmm7, %xmm9                                  #806.13
        movups    1344+__svml_scosh_ha_data_internal(%rip), %xmm6 #788.27
        movaps    %xmm6, %xmm2                                  #799.23
        psubd     %xmm3, %xmm2                                  #799.23
        movups    896+__svml_scosh_ha_data_internal(%rip), %xmm8 #807.35
        psrld     $28, %xmm2                                    #800.23
        mulps     %xmm9, %xmm8                                  #807.35
        pslld     $4, %xmm2                                     #801.23
        por       %xmm3, %xmm2                                  #802.23
        subps     %xmm8, %xmm4                                  #807.13
        pand      %xmm6, %xmm2                                  #803.23
        movups    960+__svml_scosh_ha_data_internal(%rip), %xmm10 #808.32
        movdqa    %xmm2, %xmm11                                 #809.183
        mulps     %xmm9, %xmm10                                 #808.32
        pslld     $4, %xmm11                                    #809.183
        movd      %xmm11, %edx                                  #809.246
        pcmpgtd   1408+__svml_scosh_ha_data_internal(%rip), %xmm5 #795.27
        psubd     %xmm2, %xmm3                                  #804.24
        pextrd    $1, %xmm11, %ecx                              #809.314
        pslld     $19, %xmm3                                    #805.24
        pextrd    $2, %xmm11, %esi                              #809.385
        subps     %xmm10, %xmm4                                 #808.13
        movmskps  %xmm5, %eax                                   #796.44
        movd      (%rdx,%r8), %xmm6                             #810.1010
        pextrd    $3, %xmm11, %edi                              #809.456
        movd      (%rcx,%r8), %xmm15                            #810.1093
        punpcklqdq %xmm15, %xmm6                                #810.990
        movd      (%rsi,%r8), %xmm5                             #810.1197
        movd      (%rdi,%r8), %xmm15                            #810.1280
        punpcklqdq %xmm15, %xmm5                                #810.1177
        shufps    $136, %xmm5, %xmm6                            #810.970
        mulps     %xmm4, %xmm6                                  #812.27
        movd      -4(%rdi,%r8), %xmm13                          #809.1280
        movd      -4(%rdx,%r8), %xmm1                           #809.1010
        movd      -4(%rcx,%r8), %xmm12                          #809.1093
        movd      -4(%rsi,%r8), %xmm14                          #809.1197
        punpcklqdq %xmm12, %xmm1                                #809.990
        punpcklqdq %xmm13, %xmm14                               #809.1177
        shufps    $136, %xmm14, %xmm1                           #809.970
        movaps    %xmm1, %xmm5                                  #811.13
        movd      -8(%rsi,%r8), %xmm9                           #813.1197
        addps     %xmm1, %xmm5                                  #811.13
        addps     %xmm6, %xmm1                                  #812.15
        mulps     %xmm4, %xmm1                                  #814.27
        movd      -8(%rdx,%r8), %xmm12                          #813.1010
        movd      -8(%rcx,%r8), %xmm7                           #813.1093
        movd      -8(%rdi,%r8), %xmm8                           #813.1280
        punpcklqdq %xmm7, %xmm12                                #813.990
        punpcklqdq %xmm8, %xmm9                                 #813.1177
        shufps    $136, %xmm9, %xmm12                           #813.970
        movd      -12(%rdi,%r8), %xmm10                         #815.1280
        addps     %xmm1, %xmm12                                 #814.15
        mulps     %xmm12, %xmm4                                 #816.27
        movd      -12(%rdx,%r8), %xmm13                         #815.1010
        movd      -12(%rcx,%r8), %xmm1                          #815.1093
        movd      -12(%rsi,%r8), %xmm11                         #815.1197
        punpcklqdq %xmm1, %xmm13                                #815.990
        punpcklqdq %xmm10, %xmm11                               #815.1177
        shufps    $136, %xmm11, %xmm13                          #815.970
        addps     %xmm4, %xmm13                                 #816.15
        addps     %xmm13, %xmm5                                 #817.15
        paddd     %xmm3, %xmm5                                  #820.14
        testl     %eax, %eax                                    #822.52
        jne       ..B6.3        # Prob 5%                       #822.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm5
..B6.2:                         # Preds ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movaps    %xmm5, %xmm0                                  #825.12
        movq      %rbp, %rsp                                    #825.12
        popq      %rbp                                          #825.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #825.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #822.193
        movups    %xmm5, 128(%rsp)                              #822.264
                                # LOE rbx r12 r13 r14 r15 eax
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #822.448
        movq      %r12, 8(%rsp)                                 #822.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #822.448
        movq      %r13, (%rsp)                                  #822.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #822.448
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #822.511
        jc        ..B6.10       # Prob 5%                       #822.511
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #822.464
        cmpl      $4, %r12d                                     #822.459
        jl        ..B6.7        # Prob 82%                      #822.459
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm5                              #822.666
        jmp       ..B6.2        # Prob 100%                     #822.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm5
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #822.540
        lea       128(%rsp,%r12,4), %rsi                        #822.540
..___tag_value___svml_coshf4_ha_e9.130:
#       __svml_scosh_ha_cout_rare_internal(const float *, float *)
        call      __svml_scosh_ha_cout_rare_internal            #822.540
..___tag_value___svml_coshf4_ha_e9.131:
        jmp       ..B6.8        # Prob 100%                     #822.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_coshf4_ha_e9,@function
	.size	__svml_coshf4_ha_e9,.-__svml_coshf4_ha_e9
..LN__svml_coshf4_ha_e9.5:
	.data
# -- End  __svml_coshf4_ha_e9
	.text
.L_2__routine_start___svml_scosh_ha_cout_rare_internal_6:
# -- Begin  __svml_scosh_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_scosh_ha_cout_rare_internal
	.globl __svml_scosh_ha_cout_rare_internal
# --- __svml_scosh_ha_cout_rare_internal(const float *, float *)
__svml_scosh_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #975.1
        .byte     15                                            #1078.11
        .byte     30                                            #1078.11
        .byte     250                                           #1078.11
	.cfi_startproc
..___tag_value___svml_scosh_ha_cout_rare_internal.133:
..L134:
                                                        #975.1
        movq      %rsi, %r8                                     #975.1
        movzwl    2(%rdi), %edx                                 #988.31
        xorl      %eax, %eax                                    #986.15
        andl      $32640, %edx                                  #988.31
        cmpl      $32640, %edx                                  #988.57
        je        ..B7.12       # Prob 16%                      #988.57
                                # LOE rbx rbp rdi r8 r12 r13 r14 r15 eax
..B7.2:                         # Preds ..B7.1
                                # Execution count [8.40e-01]
        pxor      %xmm0, %xmm0                                  #990.28
        cvtss2sd  (%rdi), %xmm0                                 #990.28
        movsd     %xmm0, -8(%rsp)                               #990.9
        andb      $127, -1(%rsp)                                #991.30
        movzwl    -2(%rsp), %edx                                #992.35
        andl      $32752, %edx                                  #992.35
        cmpl      $15504, %edx                                  #992.60
        jle       ..B7.10       # Prob 50%                      #992.60
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax
..B7.3:                         # Preds ..B7.2
                                # Execution count [4.20e-01]
        movsd     -8(%rsp), %xmm0                               #994.19
        movsd     1096+_vmldCoshHATab(%rip), %xmm1              #994.44
        comisd    %xmm0, %xmm1                                  #994.44
        jbe       ..B7.9        # Prob 50%                      #994.44
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax xmm0
..B7.4:                         # Preds ..B7.3
                                # Execution count [2.10e-01]
        movq      1128+_vmldCoshHATab(%rip), %rdx               #987.5
        movq      %rdx, -8(%rsp)                                #987.5
        comisd    1144+_vmldCoshHATab(%rip), %xmm0              #996.50
        jb        ..B7.8        # Prob 50%                      #996.50
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax xmm0
..B7.5:                         # Preds ..B7.4
                                # Execution count [1.05e-01]
        movsd     1040+_vmldCoshHATab(%rip), %xmm1              #998.65
        lea       _vmldCoshHATab(%rip), %r9                     #1013.59
        mulsd     %xmm0, %xmm1                                  #998.65
        addsd     1048+_vmldCoshHATab(%rip), %xmm1              #999.62
        movsd     %xmm1, -40(%rsp)                              #999.25
        movsd     -40(%rsp), %xmm2                              #1000.38
        movsd     1088+_vmldCoshHATab(%rip), %xmm1              #1005.59
        movl      -40(%rsp), %edx                               #1007.50
        movl      %edx, %esi                                    #1008.33
        andl      $63, %esi                                     #1008.33
        subsd     1048+_vmldCoshHATab(%rip), %xmm2              #1000.60
        movsd     %xmm2, -32(%rsp)                              #1000.25
        lea       (%rsi,%rsi), %ecx                             #1011.73
        movsd     -32(%rsp), %xmm3                              #1001.40
        lea       1(%rsi,%rsi), %edi                            #1012.83
        mulsd     1104+_vmldCoshHATab(%rip), %xmm3              #1001.61
        movsd     -32(%rsp), %xmm4                              #1002.40
        subsd     %xmm3, %xmm0                                  #1003.46
        mulsd     1112+_vmldCoshHATab(%rip), %xmm4              #1002.61
        shrl      $6, %edx                                      #1009.43
        subsd     %xmm4, %xmm0                                  #1004.42
        mulsd     %xmm0, %xmm1                                  #1005.82
        addl      $1022, %edx                                   #1014.34
        andl      $2047, %edx                                   #1014.39
        addsd     1080+_vmldCoshHATab(%rip), %xmm1              #1005.103
        mulsd     %xmm0, %xmm1                                  #1005.127
        addsd     1072+_vmldCoshHATab(%rip), %xmm1              #1005.148
        mulsd     %xmm0, %xmm1                                  #1005.172
        addsd     1064+_vmldCoshHATab(%rip), %xmm1              #1005.193
        mulsd     %xmm0, %xmm1                                  #1005.217
        addsd     1056+_vmldCoshHATab(%rip), %xmm1              #1005.238
        mulsd     %xmm0, %xmm1                                  #1005.262
        mulsd     %xmm0, %xmm1                                  #1006.42
        addsd     %xmm0, %xmm1                                  #1006.46
        movsd     (%r9,%rcx,8), %xmm0                           #1011.55
        mulsd     %xmm0, %xmm1                                  #1011.80
        addsd     (%r9,%rdi,8), %xmm1                           #1012.59
        addsd     %xmm0, %xmm1                                  #1013.59
        cmpl      $2046, %edx                                   #1015.35
        ja        ..B7.7        # Prob 50%                      #1015.35
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax edx xmm1
..B7.6:                         # Preds ..B7.5
                                # Execution count [5.25e-02]
        movq      1128+_vmldCoshHATab(%rip), %rcx               #1017.51
        shrq      $48, %rcx                                     #1017.51
        shll      $4, %edx                                      #1017.51
        andl      $-32753, %ecx                                 #1017.51
        orl       %edx, %ecx                                    #1017.51
        movw      %cx, -2(%rsp)                                 #1017.51
        movsd     -8(%rsp), %xmm0                               #1018.54
        mulsd     %xmm1, %xmm0                                  #1018.62
        cvtsd2ss  %xmm0, %xmm0                                  #1018.32
        movss     %xmm0, (%r8)                                  #1018.32
        ret                                                     #1018.32
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.7:                         # Preds ..B7.5
                                # Execution count [5.25e-02]
        decl      %edx                                          #1022.79
        andl      $2047, %edx                                   #1022.51
        movzwl    -2(%rsp), %ecx                                #1022.51
        shll      $4, %edx                                      #1022.51
        andl      $-32753, %ecx                                 #1022.51
        orl       %edx, %ecx                                    #1022.51
        movw      %cx, -2(%rsp)                                 #1022.51
        movsd     -8(%rsp), %xmm0                               #1023.38
        mulsd     %xmm0, %xmm1                                  #1023.38
        mulsd     1024+_vmldCoshHATab(%rip), %xmm1              #1024.85
        cvtsd2ss  %xmm1, %xmm1                                  #1024.32
        movss     %xmm1, (%r8)                                  #1024.32
        ret                                                     #1024.32
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.8:                         # Preds ..B7.4
                                # Execution count [1.05e-01]
        movsd     1040+_vmldCoshHATab(%rip), %xmm1              #1029.56
        lea       _vmldCoshHATab(%rip), %rcx                    #1045.49
        movzwl    -2(%rsp), %esi                                #1044.46
        andl      $-32753, %esi                                 #1044.46
        movsd     1080+_vmldCoshHATab(%rip), %xmm14             #1037.85
        mulsd     %xmm0, %xmm1                                  #1029.56
        addsd     1048+_vmldCoshHATab(%rip), %xmm1              #1030.62
        movsd     %xmm1, -40(%rsp)                              #1030.25
        movsd     -40(%rsp), %xmm2                              #1031.38
        movl      -40(%rsp), %r10d                              #1039.50
        movl      %r10d, %r9d                                   #1041.43
        shrl      $6, %r9d                                      #1041.43
        subsd     1048+_vmldCoshHATab(%rip), %xmm2              #1031.60
        movsd     %xmm2, -32(%rsp)                              #1031.25
        lea       1023(%r9), %edi                               #1042.42
        movsd     -32(%rsp), %xmm3                              #1032.40
        addl      $1022, %r9d                                   #1044.74
        mulsd     1104+_vmldCoshHATab(%rip), %xmm3              #1032.61
        andl      $63, %r10d                                    #1040.42
        movsd     -32(%rsp), %xmm4                              #1033.40
        lea       (%r10,%r10), %edx                             #1045.67
        mulsd     1112+_vmldCoshHATab(%rip), %xmm4              #1033.61
        subsd     %xmm3, %xmm0                                  #1034.46
        andl      $2047, %r9d                                   #1044.46
        negl      %edi                                          #1047.39
        movsd     (%rcx,%rdx,8), %xmm15                         #1045.49
        negl      %edx                                          #1049.71
        shll      $4, %r9d                                      #1044.46
        addl      $-4, %edi                                     #1048.74
        orl       %r9d, %esi                                    #1044.46
        andl      $2047, %edi                                   #1048.46
        movw      %si, -2(%rsp)                                 #1044.46
        andl      $-32753, %esi                                 #1048.46
        shll      $4, %edi                                      #1048.46
        lea       1(%r10,%r10), %r11d                           #1046.73
        movsd     -8(%rsp), %xmm6                               #1045.74
        orl       %edi, %esi                                    #1048.46
        movw      %si, -2(%rsp)                                 #1048.46
        lea       128(%rdx), %esi                               #1049.71
        addl      $129, %edx                                    #1050.71
        subsd     %xmm4, %xmm0                                  #1035.42
        mulsd     %xmm6, %xmm15                                 #1045.74
        movaps    %xmm0, %xmm5                                  #1036.44
        movaps    %xmm15, %xmm8                                 #1051.51
        mulsd     %xmm0, %xmm5                                  #1036.44
        movaps    %xmm15, %xmm10                                #1051.85
        movsd     (%rcx,%r11,8), %xmm2                          #1046.49
        mulsd     %xmm6, %xmm2                                  #1046.78
        mulsd     %xmm5, %xmm14                                 #1037.85
        movsd     -8(%rsp), %xmm7                               #1049.79
        movaps    %xmm2, %xmm12                                 #1053.51
        movsd     (%rcx,%rdx,8), %xmm13                         #1050.49
        mulsd     %xmm7, %xmm13                                 #1050.83
        addsd     1064+_vmldCoshHATab(%rip), %xmm14             #1037.108
        movsd     1088+_vmldCoshHATab(%rip), %xmm1              #1038.85
        subsd     %xmm13, %xmm12                                #1053.51
        mulsd     %xmm5, %xmm1                                  #1038.85
        mulsd     %xmm5, %xmm14                                 #1037.132
        mulsd     %xmm0, %xmm12                                 #1053.51
        addsd     1072+_vmldCoshHATab(%rip), %xmm1              #1038.108
        mulsd     %xmm0, %xmm14                                 #1037.139
        addsd     %xmm12, %xmm2                                 #1054.25
        mulsd     %xmm5, %xmm1                                  #1038.132
        addsd     %xmm13, %xmm2                                 #1055.25
        addsd     1056+_vmldCoshHATab(%rip), %xmm1              #1038.156
        movsd     (%rcx,%rsi,8), %xmm11                         #1049.49
        mulsd     %xmm7, %xmm11                                 #1049.79
        mulsd     %xmm5, %xmm1                                  #1038.180
        addsd     %xmm11, %xmm8                                 #1051.51
        subsd     %xmm11, %xmm15                                #1052.47
        movsd     %xmm8, -24(%rsp)                              #1051.25
        movsd     -24(%rsp), %xmm9                              #1051.85
        mulsd     %xmm15, %xmm14                                #1057.46
        subsd     %xmm9, %xmm10                                 #1051.85
        mulsd     %xmm15, %xmm0                                 #1059.43
        addsd     %xmm11, %xmm10                                #1051.117
        addsd     %xmm14, %xmm2                                 #1056.25
        movsd     %xmm10, -16(%rsp)                             #1051.94
        addsd     %xmm0, %xmm2                                  #1057.25
        movsd     -24(%rsp), %xmm3                              #1051.133
        mulsd     %xmm3, %xmm1                                  #1058.47
        movsd     -16(%rsp), %xmm6                              #1051.146
        addsd     %xmm1, %xmm2                                  #1058.25
        addsd     %xmm6, %xmm2                                  #1059.25
        movsd     %xmm2, -24(%rsp)                              #1060.25
        movsd     -24(%rsp), %xmm0                              #1061.38
        addsd     %xmm0, %xmm3                                  #1061.46
        cvtsd2ss  %xmm3, %xmm3                                  #1062.27
        movss     %xmm3, (%r8)                                  #1062.27
        ret                                                     #1062.27
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.9:                         # Preds ..B7.3
                                # Execution count [2.10e-01]
        movsd     1120+_vmldCoshHATab(%rip), %xmm0              #1067.91
        movl      $3, %eax                                      #1068.21
        mulsd     %xmm0, %xmm0                                  #1067.91
        cvtsd2ss  %xmm0, %xmm0                                  #1067.21
        movss     %xmm0, (%r8)                                  #1067.21
        ret                                                     #1067.21
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.10:                        # Preds ..B7.2
                                # Execution count [4.20e-01]
        movsd     1136+_vmldCoshHATab(%rip), %xmm0              #1073.46
        addsd     -8(%rsp), %xmm0                               #1073.69
        cvtsd2ss  %xmm0, %xmm0                                  #1073.16
        movss     %xmm0, (%r8)                                  #1073.16
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.11:                        # Preds ..B7.10
                                # Execution count [1.00e+00]
        ret                                                     #1080.12
                                # LOE
..B7.12:                        # Preds ..B7.1
                                # Execution count [1.60e-01]: Infreq
        movss     (%rdi), %xmm0                                 #1078.26
        mulss     %xmm0, %xmm0                                  #1078.33
        movss     %xmm0, (%r8)                                  #1078.11
        ret                                                     #1078.11
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_scosh_ha_cout_rare_internal,@function
	.size	__svml_scosh_ha_cout_rare_internal,.-__svml_scosh_ha_cout_rare_internal
..LN__svml_scosh_ha_cout_rare_internal.6:
	.data
# -- End  __svml_scosh_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_scosh_ha_data_internal
	.globl __svml_scosh_ha_data_internal
__svml_scosh_ha_data_internal:
	.long	1056964608
	.long	1057148295
	.long	1057336003
	.long	1057527823
	.long	1057723842
	.long	1057924154
	.long	1058128851
	.long	1058338032
	.long	1058551792
	.long	1058770234
	.long	1058993458
	.long	1059221571
	.long	1059454679
	.long	1059692891
	.long	1059936319
	.long	1060185078
	.long	1060439283
	.long	1060699055
	.long	1060964516
	.long	1061235789
	.long	1061513002
	.long	1061796286
	.long	1062085772
	.long	1062381598
	.long	1062683901
	.long	1062992824
	.long	1063308511
	.long	1063631111
	.long	1063960775
	.long	1064297658
	.long	1064641917
	.long	1064993715
	.long	0
	.long	2999887785
	.long	852465809
	.long	3003046475
	.long	2984291233
	.long	3001644133
	.long	854021668
	.long	2997748242
	.long	849550193
	.long	2995541347
	.long	851518274
	.long	809701978
	.long	2997656926
	.long	2996185864
	.long	2980965110
	.long	3002882728
	.long	844097402
	.long	848217591
	.long	2999013352
	.long	2992006718
	.long	831170615
	.long	3002278818
	.long	833158180
	.long	3000769962
	.long	2991891850
	.long	2999994908
	.long	2979965785
	.long	2982419430
	.long	2982221534
	.long	2999469642
	.long	833168438
	.long	2987538264
	.long	1056964608
	.long	1056605107
	.long	1056253309
	.long	1055909050
	.long	1055572167
	.long	1055242503
	.long	1054919903
	.long	1054604216
	.long	1054295293
	.long	1053992990
	.long	1053697164
	.long	1053407678
	.long	1053124394
	.long	1052847181
	.long	1052575908
	.long	1052310447
	.long	1052050675
	.long	1051796470
	.long	1051547711
	.long	1051304283
	.long	1051066071
	.long	1050832963
	.long	1050604850
	.long	1050381626
	.long	1050163184
	.long	1049949424
	.long	1049740243
	.long	1049535546
	.long	1049335234
	.long	1049139215
	.long	1048947395
	.long	1048759687
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	1118743630
	.long	1118743630
	.long	1118743630
	.long	1118743630
	.long	1118743630
	.long	1118743630
	.long	1118743630
	.long	1118743630
	.long	1118743630
	.long	1118743630
	.long	1118743630
	.long	1118743630
	.long	1118743630
	.long	1118743630
	.long	1118743630
	.long	1118743630
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
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1056964879
	.long	1042983629
	.long	1042983629
	.long	1042983629
	.long	1042983629
	.long	1042983629
	.long	1042983629
	.long	1042983629
	.long	1042983629
	.long	1042983629
	.long	1042983629
	.long	1042983629
	.long	1042983629
	.long	1042983629
	.long	1042983629
	.long	1042983629
	.long	1042983629
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
	.long	849703008
	.long	849703008
	.long	849703008
	.long	849703008
	.long	849703008
	.long	849703008
	.long	849703008
	.long	849703008
	.long	849703008
	.long	849703008
	.long	849703008
	.long	849703008
	.long	849703008
	.long	849703008
	.long	849703008
	.long	849703008
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
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
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	944570348
	.long	870537889
	.long	1056963788
	.long	988584323
	.long	3089368227
	.long	1026654286
	.long	1056972809
	.long	1005362723
	.long	3089410886
	.long	1035053812
	.long	1056996444
	.long	1013759196
	.long	3089450701
	.long	1040545168
	.long	1057035884
	.long	1018294210
	.long	3089519489
	.long	1043486152
	.long	1057091204
	.long	1022210002
	.long	3089622651
	.long	1046449073
	.long	1057162508
	.long	1024792095
	.long	3089732783
	.long	1049007747
	.long	1057249929
	.long	1026787500
	.long	3089879760
	.long	1050519514
	.long	1057353632
	.long	1028802193
	.long	3090009552
	.long	1052050675
	.long	1057473810
	.long	1030843673
	.long	3090201654
	.long	1053604104
	.long	1057610691
	.long	1032358162
	.long	3090393038
	.long	1055182718
	.long	1057764530
	.long	1033401816
	.long	3090624519
	.long	1056789478
	.long	1057935617
	.long	1034476232
	.long	3090859136
	.long	1057696005
	.long	1058124272
	.long	1035562860
	.long	3091126256
	.long	1058532085
	.long	1058330850
	.long	1036689182
	.long	3091401474
	.long	1059386854
	.long	1058555738
	.long	1037824061
	.long	3091713853
	.long	1060261915
	.long	1058799359
	.long	1038999406
	.long	3092054410
	.long	1061158912
	.long	1059062170
	.long	1040187520
	.long	3092413532
	.long	1062079528
	.long	1059344664
	.long	1040796570
	.long	3092816174
	.long	1063025490
	.long	1059647372
	.long	1041432479
	.long	3093223701
	.long	1063998575
	.long	1059970861
	.long	1042082428
	.long	3093662789
	.long	1065000609
	.long	1060315739
	.long	1042753182
	.long	3094122539
	.long	1065693345
	.long	1060682653
	.long	1043434554
	.long	3094645738
	.long	1066226161
	.long	1061072293
	.long	1044155985
	.long	3095155406
	.long	1066776362
	.long	1061485388
	.long	1044890780
	.long	3095550555
	.long	1067344981
	.long	1061922715
	.long	1045635453
	.long	3095847386
	.long	1067933084
	.long	1062385095
	.long	1046418690
	.long	3096168298
	.long	1068541775
	.long	1062873396
	.long	1047240047
	.long	3096488137
	.long	1069172198
	.long	1063388533
	.long	1048071426
	.long	3096841182
	.long	1069825535
	.long	1063931475
	.long	1048758942
	.long	3097209475
	.long	1070503013
	.long	1064503240
	.long	1049207926
	.long	3097589791
	.long	1071205903
	.long	1065104901
	.long	1049678351
	.long	3097993402
	.long	1071935525
	.long	1065545402
	.long	1050164645
	.long	3098411341
	.long	1072693248
	.long	1065877852
	.long	1050673310
	.long	3098859808
	.long	1073480495
	.long	1066227033
	.long	1051198081
	.long	3099325394
	.long	1074020284
	.long	1066593600
	.long	1051736997
	.long	3099839474
	.long	1074445677
	.long	1066978242
	.long	1052300332
	.long	3100370328
	.long	1074888136
	.long	1067381680
	.long	1052909383
	.long	3100909820
	.long	1075348494
	.long	1067804671
	.long	1053514627
	.long	3101459594
	.long	1075827613
	.long	1068248009
	.long	1054160592
	.long	3102047769
	.long	1076326394
	.long	1068712527
	.long	1054814464
	.long	3102677758
	.long	1076845772
	.long	1069199097
	.long	1055502910
	.long	3103340170
	.long	1077386722
	.long	1069708632
	.long	1056225281
	.long	3103903569
	.long	1077950259
	.long	1070242088
	.long	1056977834
	.long	3104249593
	.long	1078537443
	.long	1070800466
	.long	1057360587
	.long	3104632246
	.long	1079149373
	.long	1071384816
	.long	1057776467
	.long	3105038122
	.long	1079787200
	.long	1071996234
	.long	1058202023
	.long	3105440616
	.long	1080452121
	.long	1072635866
	.long	1058640522
	.long	3105862938
	.long	1081145383
	.long	1073304914
	.long	1059104028
	.long	3106308416
	.long	1081868288
	.long	1073873229
	.long	1059586215
	.long	3106787412
	.long	1082376312
	.long	1074239082
	.long	1060097588
	.long	3107276928
	.long	1082769472
	.long	1074621614
	.long	1060619929
	.long	3107776680
	.long	1083179578
	.long	1075021543
	.long	1061153935
	.long	3108330475
	.long	1083607398
	.long	1075439621
	.long	1061737331
	.long	3108881710
	.long	1084053737
	.long	1075876631
	.long	1062331214
	.long	3109487286
	.long	1084519432
	.long	1076333395
	.long	1062953203
	.long	3110070509
	.long	1085005358
	.long	1076810768
	.long	1063586843
	.long	3110728850
	.long	1085512425
	.long	1077309649
	.long	1064276575
	.long	3111383871
	.long	1086041587
	.long	1077830972
	.long	1064978612
	.long	3112084118
	.long	1086593836
	.long	1078375717
	.long	1065536743
	.long	3112493703
	.long	1087170210
	.long	1078944906
	.long	1065913820
	.long	3112867371
	.long	1087771789
	.long	1079539607
	.long	1066317189
	.long	3113278547
	.long	1088399703
	.long	1080160938
	.long	1066739445
	.long	3113690682
	.long	1089055131
	.long	1080810063
	.long	1067177635
	.long	3114113585
	.long	1089739304
	.long	1081488201
	.long	1067625214
	.long	3114565947
	.long	1090453504
	.long	1082163529
	.long	1068105897
	.long	3115052575
	.long	1090859057
	.long	1082533550
	.long	1068596020
	.long	3115539880
	.long	1091248226
	.long	1082920073
	.long	1069111659
	.long	3116077017
	.long	1091654509
	.long	1083323825
	.long	1069663909
	.long	3116603774
	.long	1092078670
	.long	1083745562
	.long	1070225544
	.long	3117166138
	.long	1092521504
	.long	1084186077
	.long	1070821702
	.long	3117769278
	.long	1092983843
	.long	1084646197
	.long	1071437696
	.long	3118359457
	.long	1093466555
	.long	1085126784
	.long	1072071392
	.long	3119000307
	.long	1093970545
	.long	1085628742
	.long	1072746100
	.long	3119686251
	.long	1094496760
	.long	1086153013
	.long	1073443058
	.long	3120382865
	.long	1095046187
	.long	1086700580
	.long	1073960254
	.long	3120829800
	.long	1095619858
	.long	1087272471
	.long	1074341025
	.long	3121221705
	.long	1096218849
	.long	1087869761
	.long	1074743826
	.long	3121630109
	.long	1096844285
	.long	1088493570
	.long	1075162699
	.long	3122040558
	.long	1097497340
	.long	1089145068
	.long	1075598254
	.long	3122471799
	.long	1098179240
	.long	1089825479
	.long	1076049525
	.long	3122921786
	.long	1098891264
	.long	1090527560
	.long	1076527273
	.long	3123410322
	.long	1099271199
	.long	1090898623
	.long	1077017199
	.long	3123905268
	.long	1099659370
	.long	1091286144
	.long	1077536277
	.long	3124427171
	.long	1100064698
	.long	1091690851
	.long	1078077742
	.long	3124955362
	.long	1100487944
	.long	1092113503
	.long	1078639053
	.long	3125512315
	.long	1100929902
	.long	1092554894
	.long	1079230664
	.long	3126114846
	.long	1101391402
	.long	1093015853
	.long	1079845159
	.long	3126723150
	.long	1101873310
	.long	1093497244
	.long	1080489100
	.long	3127384205
	.long	1102376531
	.long	1093999972
	.long	1081154940
	.long	3128045109
	.long	1102902009
	.long	1094524979
	.long	1081855739
	.long	3128757202
	.long	1103450730
	.long	1095073252
	.long	1082365260
	.long	3129233957
	.long	1104023725
	.long	1095645820
	.long	1082749515
	.long	3129593552
	.long	1104622070
	.long	1096243755
	.long	1083141940
	.long	3130009456
	.long	1105246886
	.long	1096868184
	.long	1083565083
	.long	3130431772
	.long	1105899348
	.long	1097520276
	.long	1083997423
	.long	3130861002
	.long	1106580680
	.long	1098201255
	.long	1084447059
	.long	3131310395
	.long	1107292160
	.long	1098910024
	.long	1084924074
	.long	3131783023
	.long	1107665690
	.long	1099281347
	.long	1085424177
	.long	3132296264
	.long	1108053612
	.long	1099669118
	.long	1085933889
	.long	3132789780
	.long	1108458701
	.long	1100074063
	.long	1086477769
	.long	3133359295
	.long	1108881718
	.long	1100496945
	.long	1087044117
	.long	3133914895
	.long	1109323457
	.long	1100938555
	.long	1087634592
	.long	3134525467
	.long	1109784747
	.long	1101399724
	.long	1088253827
	.long	3135105529
	.long	1110266455
	.long	1101881315
	.long	1088879869
	.long	3135755251
	.long	1110769483
	.long	1102384235
	.long	1089558833
	.long	3136442666
	.long	1111294777
	.long	1102909427
	.long	1090255482
	.long	3137142241
	.long	1111843322
	.long	1103457876
	.long	1090755410
	.long	3137605970
	.long	1112416148
	.long	1104030612
	.long	1091140533
	.long	3137986162
	.long	1113014331
	.long	1104628710
	.long	1091535483
	.long	3138387555
	.long	1113638993
	.long	1105253293
	.long	1091949463
	.long	3138804646
	.long	1114291306
	.long	1105905533
	.long	1092388670
	.long	3139233372
	.long	1114972496
	.long	1106586654
	.long	1092837897
	.long	3139699003
	.long	1115683840
	.long	1107297096
	.long	1093314730
	.long	3140167653
	.long	1116055769
	.long	1107668484
	.long	1093812263
	.long	3140669084
	.long	1116443628
	.long	1108056317
	.long	1094334974
	.long	3141171888
	.long	1116848658
	.long	1108461322
	.long	1094864117
	.long	3141735347
	.long	1117271618
	.long	1108884261
	.long	1095426609
	.long	3142298803
	.long	1117713302
	.long	1109325926
	.long	1096021914
	.long	3142894998
	.long	1118174540
	.long	1109787147
	.long	1096632105
	.long	3143500773
	.long	1118656197
	.long	1110268789
	.long	1097274132
	.long	3144147662
	.long	1119159177
	.long	1110771757
	.long	1097951263
	.long	3144833512
	.long	1119684425
	.long	1111296995
	.long	1098646873
	.long	3145529957
	.long	1120232926
	.long	1111845488
	.long	1099144404
	.long	3145990428
	.long	1120805710
	.long	1112418266
	.long	1099527187
	.long	3146379868
	.long	1121403852
	.long	1113016405
	.long	1099927882
	.long	3146785826
	.long	1122028475
	.long	1113641027
	.long	1100344686
	.long	3147185223
	.long	1122680752
	.long	1114293303
	.long	1100772823
	.long	3147622018
	.long	1123361906
	.long	1114974460
	.long	1101227063
	.long	3148087611
	.long	1124073216
	.long	1115685320
	.long	1101703851
	.long	3148547074
	.long	1124444745
	.long	1116056724
	.long	1102195626
	.long	3149061936
	.long	1124832589
	.long	1116444573
	.long	1102706245
	.long	3149567064
	.long	1125237603
	.long	1116849593
	.long	1103257276
	.long	3150120816
	.long	1125660549
	.long	1117272546
	.long	1103813688
	.long	3150694429
	.long	1126102219
	.long	1117714225
	.long	1104415316
	.long	3151287031
	.long	1126563444
	.long	1118175459
	.long	1105023245
	.long	3151907427
	.long	1127045088
	.long	1118657114
	.long	1105674384
	.long	3152520833
	.long	1127548057
	.long	1119160093
	.long	1106330596
	.long	3153222679
	.long	1128073293
	.long	1119685343
	.long	1107036177
	.long	3153918342
	.long	1128621783
	.long	1120233847
	.long	1107533108
	.long	3154369806
	.long	1129194557
	.long	1120806635
	.long	1107910191
	.long	3154757460
	.long	1129792689
	.long	1121404784
	.long	1108309765
	.long	3155168656
	.long	1130417302
	.long	1122029416
	.long	1108729833
	.long	3155580017
	.long	1131069569
	.long	1122681702
	.long	1109165432
	.long	3156018828
	.long	1131750714
	.long	1123362868
	.long	1109620926
	.long	3156476219
	.long	1132462016
	.long	1124073832
	.long	1110092587
	.long	3156933385
	.long	1132833445
	.long	1124445240
	.long	1110582922
	.long	3157451606
	.long	1133221285
	.long	1124833093
	.long	1111095633
	.long	3157965508
	.long	1133626295
	.long	1125238117
	.long	1111652137
	.long	3158533220
	.long	1134049237
	.long	1125661074
	.long	1112217259
	.long	3159060211
	.long	1134490905
	.long	1126102755
	.long	1112789777
	.long	3159676495
	.long	1134952126
	.long	1126563993
	.long	1113412486
	.long	3160292353
	.long	1135433767
	.long	1127045651
	.long	1114060788
	.long	3160905582
	.long	1135936733
	.long	1127548633
	.long	1114716886
	.long	3161611427
	.long	1136461966
	.long	1128073886
	.long	1115424959
	.long	3162315088
	.long	1137010453
	.long	1128622393
	.long	1115924298
	.long	3162768396
	.long	1137583224
	.long	1129195184
	.long	1116305071
	.long	3163147411
	.long	1138181354
	.long	1129793335
	.long	1116699250
	.long	3163551723
	.long	1138805965
	.long	1130417969
	.long	1117115018
	.long	3163974268
	.long	1139458229
	.long	1131070258
	.long	1117557598
	.long	3164409487
	.long	1140139372
	.long	1131751426
	.long	1118010847
	.long	3164864827
	.long	1140850672
	.long	1132462416
	.long	1118481227
	.long	3165321418
	.long	1141222076
	.long	1132833825
	.long	1118971202
	.long	3165840479
	.long	1141609915
	.long	1133221679
	.long	1119484436
	.long	3166356575
	.long	1142014924
	.long	1133626704
	.long	1120042308
	.long	3166895003
	.long	1142437866
	.long	1134049661
	.long	1120589147
	.long	3167459500
	.long	1142879532
	.long	1134491344
	.long	1121185079
	.long	3168048930
	.long	1143340753
	.long	1134952582
	.long	1121791022
	.long	3168671847
	.long	1143822393
	.long	1135434241
	.long	1122443730
	.long	3169293226
	.long	1144325358
	.long	1135937224
	.long	1123104914
	.long	3170008263
	.long	1144850590
	.long	1136462478
	.long	1123818726
	.long	3170689344
	.long	1145399077
	.long	1137010985
	.long	1124308436
	.long	3171155403
	.long	1145971847
	.long	1137583777
	.long	1124692689
	.long	3171540451
	.long	1146569976
	.long	1138181929
	.long	1125090634
	.long	3171951236
	.long	1147194586
	.long	1138806564
	.long	1125510443
	.long	3172347900
	.long	1147846851
	.long	1139458852
	.long	1125936865
	.long	3172790414
	.long	1148527993
	.long	1140140021
	.long	1126394668
	.long	3173253435
	.long	1149239292
	.long	1140851018
	.long	1126869843
	.long	3173701689
	.long	1149610690
	.long	1141222427
	.long	1127354613
	.long	3174212768
	.long	1149998528
	.long	1141610281
	.long	1127883320
	.long	3174721217
	.long	1150403538
	.long	1142015306
	.long	1128415961
	.long	3175285098
	.long	1150826479
	.long	1142438264
	.long	1128978690
	.long	3175842584
	.long	1151268145
	.long	1142879947
	.long	1129570245
	.long	3176458075
	.long	1151729365
	.long	1143341186
	.long	1130192458
	.long	3177074563
	.long	1152211005
	.long	1143822845
	.long	1130841152
	.long	3177689786
	.long	1152713970
	.long	1144325828
	.long	1131498492
	.long	3178398928
	.long	1153239202
	.long	1144851082
	.long	1132208623
	.long	3179074364
	.long	1153787689
	.long	1145399589
	.long	1132695927
	.long	3179539514
	.long	1154360459
	.long	1145972381
	.long	1133078492
	.long	3179921974
	.long	1154958588
	.long	1146570533
	.long	1133474821
	.long	3180330280
	.long	1155583198
	.long	1147195168
	.long	1133893083
	.long	3180740958
	.long	1156235462
	.long	1147847457
	.long	1134328253
	.long	3181181199
	.long	1156916604
	.long	1148528626
	.long	1134784637
	.long	3181625657
	.long	1157627903
	.long	1149239624
	.long	1135258451
	.long	3182104600
	.long	1157999299
	.long	1149611034
	.long	1135752152
	.long	3182613683
	.long	1158387137
	.long	1149998888
	.long	1136279613
	.long	3183120221
	.long	1158792147
	.long	1150403913
	.long	1136811061
	.long	3183682271
	.long	1159215088
	.long	1150826871
	.long	1137372647
	.long	3184238005
	.long	1159656754
	.long	1151268554
	.long	1137963108
	.long	3184851817
	.long	1160117974
	.long	1151729793
	.long	1138584273
	.long	3185433925
	.long	1160599615
	.long	1152211451
	.long	1139211502
	.long	3186080382
	.long	1161102579
	.long	1152714435
	.long	1139888343
	.long	3186788050
	.long	1161627811
	.long	1153239689
	.long	1140597554
	.long	3187462075
	.long	1162176298
	.long	1153788196
	.long	1141084255
	.long	3187926998
	.long	1162749068
	.long	1154360988
	.long	1141466399
	.long	3188308811
	.long	1163347197
	.long	1154959140
	.long	1141862324
	.long	3188716497
	.long	1163971807
	.long	1155583775
	.long	1142280199
	.long	3189126581
	.long	1164624071
	.long	1156236064
	.long	1142714999
	.long	3189566254
	.long	1165305213
	.long	1156917233
	.long	1143171028
	.long	3190026555
	.long	1166016512
	.long	1157628232
	.long	1143644503
	.long	3190504977
	.long	1166387907
	.long	1157999642
	.long	1144148108
	.long	3190980787
	.long	1166775746
	.long	1158387495
	.long	1144654797
	.long	3191519621
	.long	1167180755
	.long	1158792521
	.long	1145206407
	.long	3192081214
	.long	1167603696
	.long	1159215479
	.long	1145767708
	.long	3192636510
	.long	1168045362
	.long	1159657162
	.long	1146357895
	.long	3193217128
	.long	1168506583
	.long	1160118400
	.long	1146958337
	.long	3193831608
	.long	1168988223
	.long	1160600059
	.long	1147605777
	.long	3194477680
	.long	1169491187
	.long	1161103043
	.long	1148282377
	.long	3195152207
	.long	1170016420
	.long	1161628296
	.long	1148970897
	.long	3195858652
	.long	1170564906
	.long	1162176804
	.long	1149475351
	.long	3196319422
	.long	1171137676
	.long	1162749596
	.long	1149857389
	.long	3196701072
	.long	1171735805
	.long	1163347748
	.long	1150253213
	.long	3197108604
	.long	1172360415
	.long	1163972383
	.long	1150670991
	.long	3197518540
	.long	1173012679
	.long	1164624672
	.long	1151105698
	.long	3197958071
	.long	1173693821
	.long	1165305841
	.long	1151561639
	.long	3198418235
	.long	1174405120
	.long	1166016840
	.long	1152035030
	.long	3198896527
	.long	1174776515
	.long	1166388250
	.long	1152538553
	.long	3199372213
	.long	1175164354
	.long	1166776103
	.long	1153045164
	.long	3199910927
	.long	1175569363
	.long	1167181129
	.long	1153596699
	.long	3200472406
	.long	1175992304
	.long	1167604087
	.long	1154157929
	.long	3201027592
	.long	1176433970
	.long	1168045770
	.long	1154748047
	.long	3201608106
	.long	1176895191
	.long	1168507008
	.long	1155348424
	.long	3202222485
	.long	1177376831
	.long	1168988667
	.long	1155995801
	.long	3202868461
	.long	1177879795
	.long	1169491651
	.long	1156672341
	.long	3203542895
	.long	1178405028
	.long	1170016904
	.long	1157360804
	.long	3204249252
	.long	1178953514
	.long	1170565412
	.long	1157864581
	.long	3204708983
	.long	1179526284
	.long	1171138204
	.long	1158246593
	.long	3205090594
	.long	1180124413
	.long	1171736356
	.long	1158642392
	.long	3205498087
	.long	1180749023
	.long	1172360991
	.long	1159060145
	.long	3205907986
	.long	1181401287
	.long	1173013280
	.long	1159494829
	.long	3206347481
	.long	1182082429
	.long	1173694449
	.long	1159950748
	.long	3206807611
	.long	1182793728
	.long	1174405448
	.long	1160424117
	.long	3207285871
	.long	1183165123
	.long	1174776858
	.long	1160927621
	.long	3207761525
	.long	1183552962
	.long	1175164711
	.long	1161434212
	.long	3208300209
	.long	1183957971
	.long	1175569737
	.long	1161985728
	.long	3208861660
	.long	1184380912
	.long	1175992695
	.long	1162546940
	.long	3209416818
	.long	1184822578
	.long	1176434378
	.long	1163137042
	.long	3209997306
	.long	1185283799
	.long	1176895616
	.long	1163737402
	.long	3210611660
	.long	1185765439
	.long	1177377275
	.long	1164384763
	.long	3211257612
	.long	1186268403
	.long	1177880259
	.long	1165061288
	.long	3211932023
	.long	1186793636
	.long	1178405512
	.long	1165749736
	.long	3212638358
	.long	1187342122
	.long	1178954020
	.long	1166253344
	.long	3213097830
	.long	1187914892
	.long	1179526812
	.long	1166635350
	.long	3213479430
	.long	1188513021
	.long	1180124964
	.long	1167031142
	.long	3213886913
	.long	1189137631
	.long	1180749599
	.long	1167448890
	.long	3214296803
	.long	1189789895
	.long	1181401888
	.long	1167883568
	.long	3214736289
	.long	1190471037
	.long	1182083057
	.long	1168339481
	.type	__svml_scosh_ha_data_internal,@object
	.size	__svml_scosh_ha_data_internal,5568
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
	.long	2684354560
	.long	1079401119
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
