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
	.file "svml_d_cbrt.c"
	.text
..TXTST0:
.L_2__routine_start___svml_cbrt1_ha_e9_0:
# -- Begin  __svml_cbrt1_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrt1_ha_e9
# --- __svml_cbrt1_ha_e9(__m128d)
__svml_cbrt1_ha_e9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #461.1
        .byte     15                                            #582.546
        .byte     30                                            #582.546
        .byte     250                                           #582.546
	.cfi_startproc
..___tag_value___svml_cbrt1_ha_e9.1:
..L2:
                                                          #461.1
        pushq     %rbp                                          #461.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #461.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #461.1
        subq      $128, %rsp                                    #461.1
        movl      $2147483647, %eax                             #523.23
        movl      $1015808, %esi                                #527.22
        movl      $2048, %r9d                                   #544.18
        movl      $682, %r10d                                   #545.16
        movaps    %xmm0, %xmm1                                  #461.1
        pshufd    $85, %xmm1, %xmm0                             #522.15
        movaps    %xmm1, %xmm7                                  #530.17
        movd      %eax, %xmm4                                   #523.23
        psrlq     $52, %xmm7                                    #530.17
        movd      %esi, %xmm10                                  #527.22
        pand      %xmm0, %xmm4                                  #523.23
        pand      %xmm0, %xmm10                                 #527.22
        psrld     $20, %xmm0                                    #543.18
        movd      %r9d, %xmm11                                  #544.18
        movd      %r10d, %xmm12                                 #545.16
        pand      %xmm11, %xmm0                                 #544.18
        psrld     $12, %xmm10                                   #528.22
        por       %xmm12, %xmm0                                 #545.16
        lea       __svml_dcbrt_ha_data_internal(%rip), %r11     #529.11
        movsd     2496+__svml_dcbrt_ha_data_internal(%rip), %xmm12 #551.9
        movl      $-2146435072, %edx                            #524.23
        movsd     2432+__svml_dcbrt_ha_data_internal(%rip), %xmm14 #552.14
        andps     %xmm1, %xmm12                                 #551.9
        orps      %xmm14, %xmm12                                #553.9
        pxor      %xmm11, %xmm11                                #548.16
        movsd     2368+__svml_dcbrt_ha_data_internal(%rip), %xmm14 #554.17
        movl      $-2097153, %ecx                               #525.23
        movq      2624+__svml_dcbrt_ha_data_internal(%rip), %xmm5 #513.23
        andps     %xmm1, %xmm14                                 #555.10
        movsd     2304+__svml_dcbrt_ha_data_internal(%rip), %xmm15 #556.18
        pand      %xmm5, %xmm7                                  #531.17
        movd      %xmm10, %edi                                  #529.81
        orps      %xmm15, %xmm14                                #557.10
        movq      2688+__svml_dcbrt_ha_data_internal(%rip), %xmm6 #532.19
        movd      %edx, %xmm2                                   #524.23
        pshufd    $0, %xmm7, %xmm9                              #533.17
        psubd     %xmm2, %xmm4                                  #524.23
        pmuludq   %xmm7, %xmm6                                  #532.19
        subsd     %xmm14, %xmm12                                #558.9
        movslq    %edi, %rdi                                    #529.11
        movd      %ecx, %xmm3                                   #525.23
        pshufd    $0, %xmm6, %xmm13                             #534.19
        pcmpgtd   %xmm3, %xmm4                                  #525.23
        psrld     $14, %xmm13                                   #535.19
        paddd     %xmm13, %xmm0                                 #547.16
        movdqa    %xmm13, %xmm8                                 #538.17
        mulsd     (%r11,%rdi), %xmm12                           #559.9
        pslld     $20, %xmm0                                    #547.16
        movmskps  %xmm4, %eax                                   #526.40
        punpckldq %xmm0, %xmm11                                 #548.16
        psubd     %xmm13, %xmm9                                 #537.17
        movsd     1792+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #560.10
        paddd     %xmm13, %xmm8                                 #538.17
        mulsd     %xmm12, %xmm0                                 #562.21
        psubd     %xmm8, %xmm9                                  #538.17
        pslld     $8, %xmm9                                     #539.17
        addsd     1856+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #562.9
        mulsd     %xmm12, %xmm0                                 #564.21
        paddd     %xmm9, %xmm10                                 #541.27
        pslld     $1, %xmm10                                    #541.27
        addsd     1920+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #564.9
        movd      %xmm10, %r8d                                  #542.94
        mulsd     %xmm12, %xmm0                                 #566.21
        movslq    %r8d, %r8                                     #542.51
        addsd     1984+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #566.9
        mulsd     %xmm12, %xmm0                                 #568.21
        movups    256(%r11,%r8), %xmm3                          #542.51
        mulsd     %xmm11, %xmm3                                 #575.15
        addsd     2048+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #568.9
        mulsd     %xmm12, %xmm0                                 #570.21
        movaps    %xmm3, %xmm13                                 #577.15
        mulsd     %xmm12, %xmm13                                #577.15
        addsd     2112+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #570.9
        mulsd     %xmm12, %xmm0                                 #572.21
        movsd     264(%r11,%r8), %xmm2                          #542.51
        mulsd     %xmm11, %xmm2                                 #576.15
        addsd     2176+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #572.9
        mulsd     %xmm12, %xmm0                                 #574.21
        addsd     2240+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #574.9
        mulsd     %xmm13, %xmm0                                 #578.9
        addsd     %xmm2, %xmm0                                  #579.9
        addsd     %xmm3, %xmm0                                  #580.10
        andl      $1, %eax                                      #526.94
        jne       ..B1.3        # Prob 5%                       #582.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1
..B1.2:                         # Preds ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #585.12
        popq      %rbp                                          #585.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #585.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm1, (%rsp)                                 #582.160
        movsd     %xmm0, 64(%rsp)                               #582.233
        jne       ..B1.6        # Prob 5%                       #582.374
                                # LOE rbx r12 r13 r14 r15
..B1.4:                         # Preds ..B1.6 ..B1.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #582.626
        movq      %rbp, %rsp                                    #582.626
        popq      %rbp                                          #582.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #582.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B1.6:                         # Preds ..B1.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #582.546
        lea       64(%rsp), %rsi                                #582.546
..___tag_value___svml_cbrt1_ha_e9.14:
#       __svml_dcbrt_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcbrt_ha_cout_rare_internal            #582.546
..___tag_value___svml_cbrt1_ha_e9.15:
        jmp       ..B1.4        # Prob 100%                     #582.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_cbrt1_ha_e9,@function
	.size	__svml_cbrt1_ha_e9,.-__svml_cbrt1_ha_e9
..LN__svml_cbrt1_ha_e9.0:
	.data
# -- End  __svml_cbrt1_ha_e9
	.text
.L_2__routine_start___svml_cbrt4_ha_l9_1:
# -- Begin  __svml_cbrt4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrt4_ha_l9
# --- __svml_cbrt4_ha_l9(__m256d)
__svml_cbrt4_ha_l9:
# parameter 1: %ymm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #590.1
        .byte     15                                            #711.552
        .byte     30                                            #711.552
        .byte     250                                           #711.552
	.cfi_startproc
..___tag_value___svml_cbrt4_ha_l9.17:
..L18:
                                                         #590.1
        pushq     %rbp                                          #590.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #590.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #590.1
        subq      $192, %rsp                                    #590.1
        lea       __svml_dcbrt_ha_data_internal(%rip), %rax     #658.74
        vmovapd   %ymm0, %ymm6                                  #590.1
        vextractf128 $1, %ymm6, %xmm7                           #651.118
        vpsrlq    $52, %ymm6, %ymm0                             #659.13
        vshufps   $221, %xmm7, %xmm6, %xmm5                     #651.29
        vandps    2752+__svml_dcbrt_ha_data_internal(%rip), %xmm5, %xmm11 #656.18
        vpsrld    $12, %xmm11, %xmm4                            #657.18
        vmovd     %xmm4, %ecx                                   #658.144
        vandps    2816+__svml_dcbrt_ha_data_internal(%rip), %xmm5, %xmm8 #652.19
        vpsrld    $20, %xmm5, %xmm5                             #672.14
        vpsubd    3008+__svml_dcbrt_ha_data_internal(%rip), %xmm8, %xmm9 #653.19
        vandps    2624+__svml_dcbrt_ha_data_internal(%rip), %ymm0, %ymm1 #660.13
        vpmuludq  2688+__svml_dcbrt_ha_data_internal(%rip), %ymm1, %ymm7 #661.15
        vpextrd   $2, %xmm4, %edi                               #658.381
        movslq    %ecx, %rcx                                    #658.74
        vpextrd   $1, %xmm4, %esi                               #658.251
        movslq    %edi, %rdi                                    #658.311
        vpextrd   $3, %xmm4, %r8d                               #658.491
        movslq    %esi, %rsi                                    #658.181
        movslq    %r8d, %r8                                     #658.421
        vpcmpgtd  3072+__svml_dcbrt_ha_data_internal(%rip), %xmm9, %xmm10 #654.19
        vmovsd    (%rax,%rcx), %xmm12                           #658.74
        vmovmskps %xmm10, %edx                                  #655.40
        vmovsd    (%rax,%rdi), %xmm14                           #658.311
        vmovhpd   (%rax,%rsi), %xmm12, %xmm13                   #658.57
        vmovhpd   (%rax,%r8), %xmm14, %xmm15                    #658.294
        vextractf128 $1, %ymm7, %xmm8                           #663.126
        vshufps   $136, %xmm8, %xmm7, %xmm9                     #663.33
        vmovups   __VUNPACK_ODD_ind1.1399.0.2(%rip), %ymm8      #677.217
        vextractf128 $1, %ymm1, %xmm2                           #662.122
        vshufps   $136, %xmm2, %xmm1, %xmm10                    #662.31
        vpsrld    $14, %xmm9, %xmm2                             #664.15
        vpsubd    %xmm2, %xmm10, %xmm11                         #666.13
        vpaddd    %xmm2, %xmm2, %xmm12                          #667.13
        vandpd    2496+__svml_dcbrt_ha_data_internal(%rip), %ymm6, %ymm10 #680.9
        vinsertf128 $1, %xmm15, %ymm13, %ymm3                   #658.11
        vpsubd    %xmm12, %xmm11, %xmm13                        #667.13
        vpslld    $8, %xmm13, %xmm14                            #668.13
        vpaddd    %xmm14, %xmm4, %xmm4                          #670.23
        vpslld    $1, %xmm4, %xmm15                             #670.23
        vmovd     %xmm15, %r9d                                  #671.97
        vpextrd   $1, %xmm15, %r10d                             #671.226
        movslq    %r9d, %r9                                     #671.45
        movslq    %r10d, %r10                                   #671.174
        vpextrd   $2, %xmm15, %r11d                             #671.358
        vpextrd   $3, %xmm15, %ecx                              #671.490
        movslq    %r11d, %r11                                   #671.306
        movslq    %ecx, %rcx                                    #671.438
        vmovupd   256(%rax,%r9), %xmm0                          #671.45
        vmovupd   256(%rax,%r10), %xmm1                         #671.174
        vandpd    2368+__svml_dcbrt_ha_data_internal(%rip), %ymm6, %ymm11 #684.10
        vorpd     2432+__svml_dcbrt_ha_data_internal(%rip), %ymm10, %ymm12 #682.9
        vorpd     2304+__svml_dcbrt_ha_data_internal(%rip), %ymm11, %ymm13 #686.10
        vsubpd    %ymm13, %ymm12, %ymm14                        #687.9
        vmulpd    %ymm14, %ymm3, %ymm3                          #688.9
        vinsertf128 $1, 256(%rax,%r11), %ymm0, %ymm4            #671.548
        vinsertf128 $1, 256(%rax,%rcx), %ymm1, %ymm7            #671.629
        vunpcklpd %ymm7, %ymm4, %ymm1                           #671.708
        vunpckhpd %ymm7, %ymm4, %ymm0                           #671.756
        vpand     2880+__svml_dcbrt_ha_data_internal(%rip), %xmm5, %xmm4 #673.14
        vpor      2944+__svml_dcbrt_ha_data_internal(%rip), %xmm4, %xmm5 #674.12
        vpaddd    %xmm2, %xmm5, %xmm2                           #676.12
        vpslld    $20, %xmm2, %xmm7                             #676.12
        vmovupd   1792+__svml_dcbrt_ha_data_internal(%rip), %ymm2 #689.44
        vfmadd213pd 1856+__svml_dcbrt_ha_data_internal(%rip), %ymm3, %ymm2 #691.9
        vfmadd213pd 1920+__svml_dcbrt_ha_data_internal(%rip), %ymm3, %ymm2 #693.9
        vfmadd213pd 1984+__svml_dcbrt_ha_data_internal(%rip), %ymm3, %ymm2 #695.9
        vfmadd213pd 2048+__svml_dcbrt_ha_data_internal(%rip), %ymm3, %ymm2 #697.9
        vfmadd213pd 2112+__svml_dcbrt_ha_data_internal(%rip), %ymm3, %ymm2 #699.9
        vfmadd213pd 2176+__svml_dcbrt_ha_data_internal(%rip), %ymm3, %ymm2 #701.9
        vfmadd213pd 2240+__svml_dcbrt_ha_data_internal(%rip), %ymm3, %ymm2 #703.9
        vpermps   %ymm7, %ymm8, %ymm9                           #677.217
        vandps    __VUNPACK_ODD_mask.1399.0.2(%rip), %ymm9, %ymm15 #677.203
        vmulpd    %ymm15, %ymm1, %ymm1                          #704.15
        vmulpd    %ymm15, %ymm0, %ymm0                          #705.15
        vmulpd    %ymm1, %ymm3, %ymm3                           #706.15
        vmulpd    %ymm3, %ymm2, %ymm15                          #707.9
        vaddpd    %ymm15, %ymm0, %ymm4                          #708.9
        vaddpd    %ymm4, %ymm1, %ymm0                           #709.10
        testl     %edx, %edx                                    #711.52
        jne       ..B2.3        # Prob 5%                       #711.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm6
..B2.2:                         # Preds ..B2.3 ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #714.12
        popq      %rbp                                          #714.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #714.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm6, 64(%rsp)                               #711.200
        vmovupd   %ymm0, 128(%rsp)                              #711.276
        je        ..B2.2        # Prob 95%                      #711.380
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #711.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B2.13:                        # Preds ..B2.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #711.523
        jc        ..B2.10       # Prob 5%                       #711.523
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #711.476
        cmpl      $4, %r12d                                     #711.471
        jl        ..B2.7        # Prob 82%                      #711.471
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #711.683
        jmp       ..B2.2        # Prob 100%                     #711.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #711.552
        lea       128(%rsp,%r12,8), %rsi                        #711.552
..___tag_value___svml_cbrt4_ha_l9.35:
#       __svml_dcbrt_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcbrt_ha_cout_rare_internal            #711.552
..___tag_value___svml_cbrt4_ha_l9.36:
        jmp       ..B2.8        # Prob 100%                     #711.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cbrt4_ha_l9,@function
	.size	__svml_cbrt4_ha_l9,.-__svml_cbrt4_ha_l9
..LN__svml_cbrt4_ha_l9.1:
	.section .rodata, "a"
	.align 64
	.align 64
__VUNPACK_ODD_ind1.1399.0.2:
	.long	0
	.long	0
	.long	0
	.long	1
	.long	0
	.long	2
	.long	0
	.long	3
	.space 32, 0x00 	# pad
	.align 64
__VUNPACK_ODD_mask.1399.0.2:
	.long	0
	.long	-1
	.long	0
	.long	-1
	.long	0
	.long	-1
	.long	0
	.long	-1
	.data
# -- End  __svml_cbrt4_ha_l9
	.text
.L_2__routine_start___svml_cbrt8_ha_z0_2:
# -- Begin  __svml_cbrt8_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrt8_ha_z0
# --- __svml_cbrt8_ha_z0(__m512d)
__svml_cbrt8_ha_z0:
# parameter 1: %zmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #719.1
        .byte     15                                            #825.12
        .byte     30                                            #825.12
        .byte     250                                           #825.12
	.cfi_startproc
..___tag_value___svml_cbrt8_ha_z0.38:
..L39:
                                                         #719.1
        vgetexppd {sae}, %zmm0, %zmm9                           #769.25
        vmovups   384+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm10 #770.55
        vgetmantpd $0, {sae}, %zmm0, %zmm2                      #768.28
        vmovups   512+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm11 #775.58
        vmovups   576+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm12 #776.55
        vmovups   640+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm13 #779.55
        vmovups   128+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm1 #789.297
        vmovups   704+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm4 #786.19
        vrcp14pd  %zmm2, %zmm15                                 #774.24
        vaddpd    {rn-sae}, %zmm10, %zmm9, %zmm14               #771.25
        vandpd    448+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm0, %zmm8 #773.21
        vrndscalepd $72, {sae}, %zmm15, %zmm3                   #784.24
        vfmsub231pd {rn-sae}, %zmm14, %zmm11, %zmm12            #777.26
        vmovups   256+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm11 #790.297
        vmovups   768+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm9 #793.63
        vmovups   896+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm10 #796.62
        vfmsub231pd {rn-sae}, %zmm3, %zmm2, %zmm4               #786.19
        vrndscalepd $9, {sae}, %zmm12, %zmm7                    #778.26
        vpsrlq    $49, %zmm3, %zmm0                             #787.46
        vmovups   832+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm3 #795.23
        vfnmadd231pd {rn-sae}, %zmm7, %zmm13, %zmm14            #780.26
        vmovups   1024+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm13 #801.62
        vfmadd231pd {rn-sae}, %zmm4, %zmm9, %zmm3               #795.23
        vmovups   1088+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm9 #803.22
        vpermpd   __svml_dcbrt_ha_data_internal_avx512(%rip), %zmm14, %zmm6 #782.153
        vpermt2pd 192+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm0, %zmm1 #789.297
        vpermt2pd 320+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm0, %zmm11 #790.297
        vpermpd   64+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm14, %zmm5 #783.153
        vmulpd    {rn-sae}, %zmm1, %zmm6, %zmm2                 #791.27
        vmulpd    {rn-sae}, %zmm4, %zmm4, %zmm0                 #799.23
        vmovups   960+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm14 #798.22
        vfmadd231pd {rn-sae}, %zmm4, %zmm13, %zmm9              #803.22
        vfmadd231pd {rn-sae}, %zmm4, %zmm10, %zmm14             #798.22
        vmovups   1216+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm10 #808.22
        vfmadd213pd {rn-sae}, %zmm14, %zmm0, %zmm3              #804.23
        vmovaps   %zmm6, %zmm12                                 #792.22
        vfmsub213pd {rn-sae}, %zmm2, %zmm1, %zmm12              #792.22
        vfmadd213pd {rn-sae}, %zmm9, %zmm0, %zmm3               #809.23
        vfmadd213pd {rn-sae}, %zmm12, %zmm11, %zmm6             #800.22
        vfmadd213pd {rn-sae}, %zmm6, %zmm1, %zmm5               #805.22
        vmovups   1152+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm1 #806.62
        vmovups   1280+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm6 #810.62
        vfmadd231pd {rn-sae}, %zmm4, %zmm1, %zmm10              #808.22
        vmovups   1344+__svml_dcbrt_ha_data_internal_avx512(%rip), %zmm1 #811.62
        vfmadd213pd {rn-sae}, %zmm10, %zmm0, %zmm3              #813.23
        vfmadd231pd {rn-sae}, %zmm4, %zmm6, %zmm1               #812.23
        vmulpd    {rn-sae}, %zmm4, %zmm2, %zmm4                 #814.26
        vfmadd213pd {rn-sae}, %zmm1, %zmm0, %zmm3               #815.23
        vfmadd213pd {rn-sae}, %zmm5, %zmm3, %zmm4               #816.22
        vaddpd    {rn-sae}, %zmm2, %zmm4, %zmm5                 #818.24
        vscalefpd {rn-sae}, %zmm7, %zmm5, %zmm7                 #819.22
        vorpd     %zmm8, %zmm7, %zmm0                           #820.22
        ret                                                     #825.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_cbrt8_ha_z0,@function
	.size	__svml_cbrt8_ha_z0,.-__svml_cbrt8_ha_z0
..LN__svml_cbrt8_ha_z0.2:
	.data
# -- End  __svml_cbrt8_ha_z0
	.text
.L_2__routine_start___svml_cbrt1_ha_l9_3:
# -- Begin  __svml_cbrt1_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrt1_ha_l9
# --- __svml_cbrt1_ha_l9(__m128d)
__svml_cbrt1_ha_l9:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #970.1
        .byte     15                                            #1091.546
        .byte     30                                            #1091.546
        .byte     250                                           #1091.546
	.cfi_startproc
..___tag_value___svml_cbrt1_ha_l9.41:
..L42:
                                                         #970.1
        pushq     %rbp                                          #970.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #970.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #970.1
        subq      $128, %rsp                                    #970.1
        vmovapd   %xmm0, %xmm4                                  #970.1
        vmovq     2624+__svml_dcbrt_ha_data_internal(%rip), %xmm14 #1022.19
        vpsrlq    $52, %xmm4, %xmm13                            #1039.13
        vmovq     2688+__svml_dcbrt_ha_data_internal(%rip), %xmm15 #1023.14
        vpand     %xmm14, %xmm13, %xmm13                        #1040.13
        vpmuludq  %xmm15, %xmm13, %xmm1                         #1041.15
        movl      $2147483647, %eax                             #1032.19
        movl      $-2146435072, %edx                            #1033.19
        movl      $1015808, %esi                                #1036.18
        movl      $-2097153, %ecx                               #1034.19
        movl      $2048, %r9d                                   #1053.14
        vpshufd   $0, %xmm1, %xmm0                              #1043.15
        movl      $682, %r10d                                   #1054.12
        vpshufd   $85, %xmm4, %xmm3                             #1031.11
        vmovd     %eax, %xmm5                                   #1032.19
        vpshufd   $0, %xmm13, %xmm14                            #1042.13
        vpsrld    $14, %xmm0, %xmm2                             #1044.15
        vpand     %xmm5, %xmm3, %xmm6                           #1032.19
        vmovd     %edx, %xmm7                                   #1033.19
        vmovd     %esi, %xmm11                                  #1036.18
        vpsubd    %xmm2, %xmm14, %xmm15                         #1046.13
        vpaddd    %xmm2, %xmm2, %xmm5                           #1047.13
        vpsubd    %xmm7, %xmm6, %xmm8                           #1033.19
        vmovd     %ecx, %xmm9                                   #1034.19
        vpand     %xmm11, %xmm3, %xmm12                         #1036.18
        vpsubd    %xmm5, %xmm15, %xmm6                          #1047.13
        vpsrld    $20, %xmm3, %xmm3                             #1052.14
        vmovd     %r9d, %xmm11                                  #1053.14
        vpcmpgtd  %xmm9, %xmm8, %xmm10                          #1034.19
        vpsrld    $12, %xmm12, %xmm7                            #1037.18
        vpslld    $8, %xmm6, %xmm8                              #1048.13
        vpand     %xmm11, %xmm3, %xmm12                         #1053.14
        vmovd     %r10d, %xmm3                                  #1054.12
        vpaddd    %xmm8, %xmm7, %xmm9                           #1050.23
        vpor      %xmm3, %xmm12, %xmm5                          #1054.12
        vmovmskps %xmm10, %eax                                  #1035.40
        vpslld    $1, %xmm9, %xmm10                             #1050.23
        vmovsd    2368+__svml_dcbrt_ha_data_internal(%rip), %xmm8 #1063.17
        vpaddd    %xmm2, %xmm5, %xmm2                           #1056.12
        vmovsd    2496+__svml_dcbrt_ha_data_internal(%rip), %xmm5 #1059.20
        vandpd    %xmm8, %xmm4, %xmm9                           #1064.10
        vmovd     %xmm7, %edi                                   #1038.81
        vandpd    %xmm5, %xmm4, %xmm6                           #1060.9
        vmovsd    2432+__svml_dcbrt_ha_data_internal(%rip), %xmm7 #1061.14
        vpslld    $20, %xmm2, %xmm3                             #1056.12
        vorpd     %xmm7, %xmm6, %xmm11                          #1062.9
        vmovd     %xmm10, %r8d                                  #1051.94
        lea       __svml_dcbrt_ha_data_internal(%rip), %r11     #1038.11
        vmovsd    2304+__svml_dcbrt_ha_data_internal(%rip), %xmm10 #1065.18
        vpxor     %xmm2, %xmm2, %xmm2                           #1057.35
        vorpd     %xmm10, %xmm9, %xmm12                         #1066.10
        movslq    %edi, %rdi                                    #1038.11
        vsubsd    %xmm12, %xmm11, %xmm13                        #1067.9
        vmulsd    (%r11,%rdi), %xmm13, %xmm15                   #1068.9
        vpunpckldq %xmm3, %xmm2, %xmm14                         #1057.16
        vmovsd    1792+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1069.10
        vfmadd213sd 1856+__svml_dcbrt_ha_data_internal(%rip), %xmm15, %xmm2 #1071.9
        vfmadd213sd 1920+__svml_dcbrt_ha_data_internal(%rip), %xmm15, %xmm2 #1073.9
        vfmadd213sd 1984+__svml_dcbrt_ha_data_internal(%rip), %xmm15, %xmm2 #1075.9
        vfmadd213sd 2048+__svml_dcbrt_ha_data_internal(%rip), %xmm15, %xmm2 #1077.9
        movslq    %r8d, %r8                                     #1051.51
        vfmadd213sd 2112+__svml_dcbrt_ha_data_internal(%rip), %xmm15, %xmm2 #1079.9
        vmovupd   256(%r11,%r8), %xmm0                          #1051.51
        vmovsd    264(%r11,%r8), %xmm1                          #1051.51
        vfmadd213sd 2176+__svml_dcbrt_ha_data_internal(%rip), %xmm15, %xmm2 #1081.9
        vmulsd    %xmm14, %xmm0, %xmm3                          #1084.15
        vmulsd    %xmm14, %xmm1, %xmm14                         #1085.15
        vfmadd213sd 2240+__svml_dcbrt_ha_data_internal(%rip), %xmm15, %xmm2 #1083.9
        vmulsd    %xmm15, %xmm3, %xmm1                          #1086.15
        vmulsd    %xmm1, %xmm2, %xmm0                           #1087.9
        vaddsd    %xmm14, %xmm0, %xmm0                          #1088.9
        vaddsd    %xmm3, %xmm0, %xmm0                           #1089.10
        andl      $1, %eax                                      #1035.94
        jne       ..B4.3        # Prob 5%                       #1091.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm4
..B4.2:                         # Preds ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1094.12
        popq      %rbp                                          #1094.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1094.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm4, (%rsp)                                 #1091.160
        vmovsd    %xmm0, 64(%rsp)                               #1091.233
        jne       ..B4.5        # Prob 5%                       #1091.374
                                # LOE rbx r12 r13 r14 r15 eax
..B4.4:                         # Preds ..B4.6 ..B4.5 ..B4.3
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm0                               #1091.626
        movq      %rbp, %rsp                                    #1091.626
        popq      %rbp                                          #1091.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1091.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.5:                         # Preds ..B4.3
                                # Execution count [2.50e-03]: Infreq
        je        ..B4.4        # Prob 95%                      #1091.517
                                # LOE rbx r12 r13 r14 r15
..B4.6:                         # Preds ..B4.5
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1091.546
        lea       64(%rsp), %rsi                                #1091.546
..___tag_value___svml_cbrt1_ha_l9.54:
#       __svml_dcbrt_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcbrt_ha_cout_rare_internal            #1091.546
..___tag_value___svml_cbrt1_ha_l9.55:
        jmp       ..B4.4        # Prob 100%                     #1091.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_cbrt1_ha_l9,@function
	.size	__svml_cbrt1_ha_l9,.-__svml_cbrt1_ha_l9
..LN__svml_cbrt1_ha_l9.3:
	.data
# -- End  __svml_cbrt1_ha_l9
	.text
.L_2__routine_start___svml_cbrt2_ha_l9_4:
# -- Begin  __svml_cbrt2_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrt2_ha_l9
# --- __svml_cbrt2_ha_l9(__m128d)
__svml_cbrt2_ha_l9:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1099.1
        .byte     15                                            #1220.546
        .byte     30                                            #1220.546
        .byte     250                                           #1220.546
	.cfi_startproc
..___tag_value___svml_cbrt2_ha_l9.57:
..L58:
                                                         #1099.1
        pushq     %rbp                                          #1099.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1099.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1099.1
        subq      $192, %rsp                                    #1099.1
        vmovapd   %xmm0, %xmm8                                  #1099.1
        vmovq     2816+__svml_dcbrt_ha_data_internal(%rip), %xmm9 #1154.17
        vpsrlq    $52, %xmm8, %xmm2                             #1168.13
        vpshufd   $221, %xmm8, %xmm7                            #1160.11
        lea       __svml_dcbrt_ha_data_internal(%rip), %r8      #1167.28
        vmovq     3008+__svml_dcbrt_ha_data_internal(%rip), %xmm11 #1157.13
        vpand     %xmm9, %xmm7, %xmm10                          #1161.19
        vpsubd    %xmm11, %xmm10, %xmm12                        #1162.19
        vpand     2624+__svml_dcbrt_ha_data_internal(%rip), %xmm2, %xmm11 #1169.13
        vpmuludq  2688+__svml_dcbrt_ha_data_internal(%rip), %xmm11, %xmm9 #1170.15
        vmovq     3072+__svml_dcbrt_ha_data_internal(%rip), %xmm13 #1158.13
        vpshufd   $136, %xmm9, %xmm10                           #1172.15
        vpcmpgtd  %xmm13, %xmm12, %xmm14                        #1163.19
        vpshufd   $136, %xmm11, %xmm12                          #1171.13
        vpsrld    $14, %xmm10, %xmm2                            #1173.15
        vmovq     2752+__svml_dcbrt_ha_data_internal(%rip), %xmm15 #1153.22
        vpsubd    %xmm2, %xmm12, %xmm13                         #1175.13
        vmovmskps %xmm14, %eax                                  #1164.40
        vpaddd    %xmm2, %xmm2, %xmm14                          #1176.13
        vpand     %xmm15, %xmm7, %xmm0                          #1165.18
        vpsubd    %xmm14, %xmm13, %xmm15                        #1176.13
        vpsrld    $12, %xmm0, %xmm3                             #1166.18
        vpslld    $8, %xmm15, %xmm0                             #1177.13
        vmovd     %xmm3, %edx                                   #1167.98
        vpsrld    $20, %xmm7, %xmm7                             #1181.14
        vmovq     2880+__svml_dcbrt_ha_data_internal(%rip), %xmm5 #1155.18
        vmovq     2944+__svml_dcbrt_ha_data_internal(%rip), %xmm6 #1156.14
        vpand     %xmm5, %xmm7, %xmm5                           #1182.14
        vpextrd   $1, %xmm3, %ecx                               #1167.205
        vpaddd    %xmm0, %xmm3, %xmm3                           #1179.23
        vpslld    $1, %xmm3, %xmm0                              #1179.23
        vpor      %xmm6, %xmm5, %xmm6                           #1183.12
        vmovd     %xmm0, %esi                                   #1180.119
        vpaddd    %xmm2, %xmm6, %xmm2                           #1185.12
        vandpd    2496+__svml_dcbrt_ha_data_internal(%rip), %xmm8, %xmm6 #1189.9
        vpslld    $20, %xmm2, %xmm5                             #1185.12
        vandpd    2368+__svml_dcbrt_ha_data_internal(%rip), %xmm8, %xmm7 #1193.10
        vpextrd   $1, %xmm0, %edi                               #1180.270
        movslq    %edx, %rdx                                    #1167.28
        movslq    %esi, %rsi                                    #1180.67
        movslq    %edi, %rdi                                    #1180.218
        movslq    %ecx, %rcx                                    #1167.135
        vmovsd    (%r8,%rdx), %xmm1                             #1167.28
        vmovupd   256(%r8,%rsi), %xmm3                          #1180.67
        vmovupd   256(%r8,%rdi), %xmm9                          #1180.218
        vmovhpd   (%r8,%rcx), %xmm1, %xmm4                      #1167.11
        vunpcklpd %xmm9, %xmm3, %xmm1                           #1180.326
        vunpckhpd %xmm9, %xmm3, %xmm0                           #1180.369
        vorpd     2432+__svml_dcbrt_ha_data_internal(%rip), %xmm6, %xmm9 #1191.9
        vpxor     %xmm3, %xmm3, %xmm3                           #1186.35
        vorpd     2304+__svml_dcbrt_ha_data_internal(%rip), %xmm7, %xmm10 #1195.10
        vsubpd    %xmm10, %xmm9, %xmm11                         #1196.9
        vmulpd    %xmm11, %xmm4, %xmm4                          #1197.9
        vmovupd   1792+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1198.41
        vfmadd213pd 1856+__svml_dcbrt_ha_data_internal(%rip), %xmm4, %xmm2 #1200.9
        vpunpckldq %xmm5, %xmm3, %xmm12                         #1186.16
        vmulpd    %xmm12, %xmm1, %xmm1                          #1213.15
        vfmadd213pd 1920+__svml_dcbrt_ha_data_internal(%rip), %xmm4, %xmm2 #1202.9
        vmulpd    %xmm12, %xmm0, %xmm0                          #1214.15
        vfmadd213pd 1984+__svml_dcbrt_ha_data_internal(%rip), %xmm4, %xmm2 #1204.9
        vfmadd213pd 2048+__svml_dcbrt_ha_data_internal(%rip), %xmm4, %xmm2 #1206.9
        vfmadd213pd 2112+__svml_dcbrt_ha_data_internal(%rip), %xmm4, %xmm2 #1208.9
        vfmadd213pd 2176+__svml_dcbrt_ha_data_internal(%rip), %xmm4, %xmm2 #1210.9
        vfmadd213pd 2240+__svml_dcbrt_ha_data_internal(%rip), %xmm4, %xmm2 #1212.9
        vmulpd    %xmm1, %xmm4, %xmm4                           #1215.15
        vmulpd    %xmm4, %xmm2, %xmm12                          #1216.9
        vaddpd    %xmm12, %xmm0, %xmm3                          #1217.9
        vaddpd    %xmm3, %xmm1, %xmm0                           #1218.10
        andl      $3, %eax                                      #1164.94
        jne       ..B5.3        # Prob 5%                       #1220.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm8
..B5.2:                         # Preds ..B5.3 ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1223.12
        popq      %rbp                                          #1223.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1223.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm8, 64(%rsp)                               #1220.197
        vmovupd   %xmm0, 128(%rsp)                              #1220.270
        je        ..B5.2        # Prob 95%                      #1220.374
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1220.454
        movq      %r12, 8(%rsp)                                 #1220.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #1220.454
        movq      %r13, (%rsp)                                  #1220.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #1220.454
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1220.517
        jc        ..B5.10       # Prob 5%                       #1220.517
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1220.470
        cmpl      $2, %r12d                                     #1220.465
        jl        ..B5.7        # Prob 82%                      #1220.465
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1220.674
        jmp       ..B5.2        # Prob 100%                     #1220.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1220.546
        lea       128(%rsp,%r12,8), %rsi                        #1220.546
..___tag_value___svml_cbrt2_ha_l9.75:
#       __svml_dcbrt_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcbrt_ha_cout_rare_internal            #1220.546
..___tag_value___svml_cbrt2_ha_l9.76:
        jmp       ..B5.8        # Prob 100%                     #1220.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cbrt2_ha_l9,@function
	.size	__svml_cbrt2_ha_l9,.-__svml_cbrt2_ha_l9
..LN__svml_cbrt2_ha_l9.4:
	.data
# -- End  __svml_cbrt2_ha_l9
	.text
.L_2__routine_start___svml_cbrt2_ha_e9_5:
# -- Begin  __svml_cbrt2_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrt2_ha_e9
# --- __svml_cbrt2_ha_e9(__m128d)
__svml_cbrt2_ha_e9:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1228.1
        .byte     15                                            #1349.546
        .byte     30                                            #1349.546
        .byte     250                                           #1349.546
	.cfi_startproc
..___tag_value___svml_cbrt2_ha_e9.78:
..L79:
                                                         #1228.1
        pushq     %rbp                                          #1228.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1228.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1228.1
        subq      $192, %rsp                                    #1228.1
        movaps    %xmm0, %xmm9                                  #1297.17
        psrlq     $52, %xmm9                                    #1297.17
        lea       __svml_dcbrt_ha_data_internal(%rip), %r8      #1296.28
        pand      2624+__svml_dcbrt_ha_data_internal(%rip), %xmm9 #1298.17
        movdqu    2688+__svml_dcbrt_ha_data_internal(%rip), %xmm8 #1299.19
        pmuludq   %xmm9, %xmm8                                  #1299.19
        movq      2752+__svml_dcbrt_ha_data_internal(%rip), %xmm12 #1294.22
        movq      2816+__svml_dcbrt_ha_data_internal(%rip), %xmm7 #1290.23
        pshufd    $221, %xmm0, %xmm3                            #1289.15
        movq      2880+__svml_dcbrt_ha_data_internal(%rip), %xmm1 #1284.22
        pand      %xmm3, %xmm7                                  #1290.23
        pand      %xmm3, %xmm12                                 #1294.22
        psrld     $20, %xmm3                                    #1310.18
        movq      2944+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1285.18
        pand      %xmm1, %xmm3                                  #1311.18
        pshufd    $136, %xmm8, %xmm14                           #1301.19
        por       %xmm2, %xmm3                                  #1312.16
        psrld     $14, %xmm14                                   #1302.19
        psrld     $12, %xmm12                                   #1295.22
        paddd     %xmm14, %xmm3                                 #1314.16
        pxor      %xmm1, %xmm1                                  #1315.16
        pslld     $20, %xmm3                                    #1314.16
        movdqa    %xmm14, %xmm10                                #1305.17
        movd      %xmm12, %edx                                  #1296.98
        paddd     %xmm14, %xmm10                                #1305.17
        punpckldq %xmm3, %xmm1                                  #1315.16
        movups    2496+__svml_dcbrt_ha_data_internal(%rip), %xmm3 #1318.9
        movups    2368+__svml_dcbrt_ha_data_internal(%rip), %xmm15 #1322.10
        andps     %xmm0, %xmm3                                  #1318.9
        andps     %xmm0, %xmm15                                 #1322.10
        orps      2432+__svml_dcbrt_ha_data_internal(%rip), %xmm3 #1320.9
        orps      2304+__svml_dcbrt_ha_data_internal(%rip), %xmm15 #1324.10
        movslq    %edx, %rdx                                    #1296.28
        pextrd    $1, %xmm12, %ecx                              #1296.205
        subpd     %xmm15, %xmm3                                 #1325.9
        movslq    %ecx, %rcx                                    #1296.135
        movsd     (%r8,%rdx), %xmm4                             #1296.28
        movups    1792+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1327.41
        pshufd    $136, %xmm9, %xmm11                           #1300.17
        movhpd    (%r8,%rcx), %xmm4                             #1296.11
        psubd     %xmm14, %xmm11                                #1304.17
        mulpd     %xmm3, %xmm4                                  #1326.9
        mulpd     %xmm4, %xmm2                                  #1329.21
        addpd     1856+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1329.9
        mulpd     %xmm4, %xmm2                                  #1331.21
        addpd     1920+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1331.9
        mulpd     %xmm4, %xmm2                                  #1333.21
        addpd     1984+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1333.9
        mulpd     %xmm4, %xmm2                                  #1335.21
        addpd     2048+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1335.9
        mulpd     %xmm4, %xmm2                                  #1337.21
        addpd     2112+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1337.9
        psubd     %xmm10, %xmm11                                #1305.17
        pslld     $8, %xmm11                                    #1306.17
        paddd     %xmm11, %xmm12                                #1308.27
        mulpd     %xmm4, %xmm2                                  #1339.21
        pslld     $1, %xmm12                                    #1308.27
        movd      %xmm12, %esi                                  #1309.119
        movq      3008+__svml_dcbrt_ha_data_internal(%rip), %xmm5 #1286.17
        movq      3072+__svml_dcbrt_ha_data_internal(%rip), %xmm6 #1287.17
        psubd     %xmm5, %xmm7                                  #1291.23
        movslq    %esi, %rsi                                    #1309.67
        pcmpgtd   %xmm6, %xmm7                                  #1292.23
        pextrd    $1, %xmm12, %edi                              #1309.270
        movslq    %edi, %rdi                                    #1309.218
        addpd     2176+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1339.9
        movmskps  %xmm7, %eax                                   #1293.40
        movups    256(%r8,%rsi), %xmm5                          #1309.67
        movups    256(%r8,%rdi), %xmm13                         #1309.218
        movaps    %xmm5, %xmm6                                  #1309.326
        unpcklpd  %xmm13, %xmm6                                 #1309.326
        mulpd     %xmm1, %xmm6                                  #1342.15
        mulpd     %xmm4, %xmm2                                  #1341.21
        mulpd     %xmm6, %xmm4                                  #1344.15
        addpd     2240+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1341.9
        unpckhpd  %xmm13, %xmm5                                 #1309.369
        mulpd     %xmm1, %xmm5                                  #1343.15
        mulpd     %xmm4, %xmm2                                  #1345.9
        addpd     %xmm2, %xmm5                                  #1346.9
        addpd     %xmm5, %xmm6                                  #1347.10
        andl      $3, %eax                                      #1293.94
        jne       ..B6.3        # Prob 5%                       #1349.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm6
..B6.2:                         # Preds ..B6.3 ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movaps    %xmm6, %xmm0                                  #1352.12
        movq      %rbp, %rsp                                    #1352.12
        popq      %rbp                                          #1352.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1352.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #1349.197
        movups    %xmm6, 128(%rsp)                              #1349.270
        je        ..B6.2        # Prob 95%                      #1349.374
                                # LOE rbx r12 r13 r14 r15 eax xmm6
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1349.454
        movq      %r12, 8(%rsp)                                 #1349.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #1349.454
        movq      %r13, (%rsp)                                  #1349.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #1349.454
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1349.517
        jc        ..B6.10       # Prob 5%                       #1349.517
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1349.470
        cmpl      $2, %r12d                                     #1349.465
        jl        ..B6.7        # Prob 82%                      #1349.465
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm6                              #1349.674
        jmp       ..B6.2        # Prob 100%                     #1349.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm6
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1349.546
        lea       128(%rsp,%r12,8), %rsi                        #1349.546
..___tag_value___svml_cbrt2_ha_e9.96:
#       __svml_dcbrt_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcbrt_ha_cout_rare_internal            #1349.546
..___tag_value___svml_cbrt2_ha_e9.97:
        jmp       ..B6.8        # Prob 100%                     #1349.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cbrt2_ha_e9,@function
	.size	__svml_cbrt2_ha_e9,.-__svml_cbrt2_ha_e9
..LN__svml_cbrt2_ha_e9.5:
	.data
# -- End  __svml_cbrt2_ha_e9
	.text
.L_2__routine_start___svml_cbrt1_ha_ex_6:
# -- Begin  __svml_cbrt1_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrt1_ha_ex
# --- __svml_cbrt1_ha_ex(__m128d)
__svml_cbrt1_ha_ex:
# parameter 1: %xmm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1357.1
        .byte     15                                            #1478.546
        .byte     30                                            #1478.546
        .byte     250                                           #1478.546
	.cfi_startproc
..___tag_value___svml_cbrt1_ha_ex.99:
..L100:
                                                        #1357.1
        pushq     %rbp                                          #1357.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1357.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1357.1
        subq      $128, %rsp                                    #1357.1
        movl      $2147483647, %eax                             #1419.17
        movl      $1015808, %esi                                #1423.16
        movl      $2048, %r9d                                   #1440.12
        movl      $682, %r10d                                   #1441.10
        movaps    %xmm0, %xmm1                                  #1357.1
        pshufd    $85, %xmm1, %xmm0                             #1418.9
        movaps    %xmm1, %xmm7                                  #1426.11
        movd      %eax, %xmm4                                   #1419.17
        psrlq     $52, %xmm7                                    #1426.11
        movd      %esi, %xmm10                                  #1423.16
        pand      %xmm0, %xmm4                                  #1419.17
        pand      %xmm0, %xmm10                                 #1423.16
        psrld     $20, %xmm0                                    #1439.12
        movd      %r9d, %xmm11                                  #1440.12
        movd      %r10d, %xmm12                                 #1441.10
        pand      %xmm11, %xmm0                                 #1440.12
        psrld     $12, %xmm10                                   #1424.16
        por       %xmm12, %xmm0                                 #1441.10
        lea       __svml_dcbrt_ha_data_internal(%rip), %r11     #1425.17
        movsd     2496+__svml_dcbrt_ha_data_internal(%rip), %xmm12 #1447.9
        movl      $-2146435072, %edx                            #1420.17
        movsd     2432+__svml_dcbrt_ha_data_internal(%rip), %xmm14 #1448.14
        andps     %xmm1, %xmm12                                 #1447.9
        orps      %xmm14, %xmm12                                #1449.9
        pxor      %xmm11, %xmm11                                #1444.16
        movsd     2368+__svml_dcbrt_ha_data_internal(%rip), %xmm14 #1450.17
        movl      $-2097153, %ecx                               #1421.17
        movq      2624+__svml_dcbrt_ha_data_internal(%rip), %xmm5 #1409.17
        andps     %xmm1, %xmm14                                 #1451.10
        movsd     2304+__svml_dcbrt_ha_data_internal(%rip), %xmm15 #1452.18
        pand      %xmm5, %xmm7                                  #1427.11
        movd      %xmm10, %edi                                  #1425.87
        orps      %xmm15, %xmm14                                #1453.10
        movq      2688+__svml_dcbrt_ha_data_internal(%rip), %xmm6 #1428.13
        movd      %edx, %xmm2                                   #1420.17
        pshufd    $0, %xmm7, %xmm9                              #1429.11
        psubd     %xmm2, %xmm4                                  #1420.17
        pmuludq   %xmm7, %xmm6                                  #1428.13
        subsd     %xmm14, %xmm12                                #1454.9
        movslq    %edi, %rdi                                    #1425.17
        movd      %ecx, %xmm3                                   #1421.17
        pshufd    $0, %xmm6, %xmm13                             #1430.13
        pcmpgtd   %xmm3, %xmm4                                  #1421.17
        psrld     $14, %xmm13                                   #1431.13
        paddd     %xmm13, %xmm0                                 #1443.10
        movdqa    %xmm13, %xmm8                                 #1434.11
        mulsd     (%r11,%rdi), %xmm12                           #1455.9
        pslld     $20, %xmm0                                    #1443.10
        movmskps  %xmm4, %eax                                   #1422.40
        punpckldq %xmm0, %xmm11                                 #1444.16
        psubd     %xmm13, %xmm9                                 #1433.11
        movsd     1792+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #1456.10
        paddd     %xmm13, %xmm8                                 #1434.11
        mulsd     %xmm12, %xmm0                                 #1458.21
        psubd     %xmm8, %xmm9                                  #1434.11
        pslld     $8, %xmm9                                     #1435.11
        addsd     1856+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #1458.9
        mulsd     %xmm12, %xmm0                                 #1460.21
        paddd     %xmm9, %xmm10                                 #1437.21
        pslld     $1, %xmm10                                    #1437.21
        addsd     1920+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #1460.9
        movd      %xmm10, %r8d                                  #1438.100
        mulsd     %xmm12, %xmm0                                 #1462.21
        movslq    %r8d, %r8                                     #1438.57
        addsd     1984+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #1462.9
        mulsd     %xmm12, %xmm0                                 #1464.21
        movups    256(%r11,%r8), %xmm3                          #1438.57
        mulsd     %xmm11, %xmm3                                 #1471.15
        addsd     2048+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #1464.9
        mulsd     %xmm12, %xmm0                                 #1466.21
        movaps    %xmm3, %xmm13                                 #1473.15
        mulsd     %xmm12, %xmm13                                #1473.15
        addsd     2112+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #1466.9
        mulsd     %xmm12, %xmm0                                 #1468.21
        movsd     264(%r11,%r8), %xmm2                          #1438.57
        mulsd     %xmm11, %xmm2                                 #1472.15
        addsd     2176+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #1468.9
        mulsd     %xmm12, %xmm0                                 #1470.21
        addsd     2240+__svml_dcbrt_ha_data_internal(%rip), %xmm0 #1470.9
        mulsd     %xmm13, %xmm0                                 #1474.9
        addsd     %xmm2, %xmm0                                  #1475.9
        addsd     %xmm3, %xmm0                                  #1476.10
        andl      $1, %eax                                      #1422.94
        jne       ..B7.3        # Prob 5%                       #1478.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1
..B7.2:                         # Preds ..B7.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1481.12
        popq      %rbp                                          #1481.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1481.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.3:                         # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm1, (%rsp)                                 #1478.160
        movsd     %xmm0, 64(%rsp)                               #1478.233
        jne       ..B7.6        # Prob 5%                       #1478.374
                                # LOE rbx r12 r13 r14 r15
..B7.4:                         # Preds ..B7.6 ..B7.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #1478.626
        movq      %rbp, %rsp                                    #1478.626
        popq      %rbp                                          #1478.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1478.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B7.6:                         # Preds ..B7.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1478.546
        lea       64(%rsp), %rsi                                #1478.546
..___tag_value___svml_cbrt1_ha_ex.112:
#       __svml_dcbrt_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcbrt_ha_cout_rare_internal            #1478.546
..___tag_value___svml_cbrt1_ha_ex.113:
        jmp       ..B7.4        # Prob 100%                     #1478.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_cbrt1_ha_ex,@function
	.size	__svml_cbrt1_ha_ex,.-__svml_cbrt1_ha_ex
..LN__svml_cbrt1_ha_ex.6:
	.data
# -- End  __svml_cbrt1_ha_ex
	.text
.L_2__routine_start___svml_cbrt4_ha_e9_7:
# -- Begin  __svml_cbrt4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrt4_ha_e9
# --- __svml_cbrt4_ha_e9(__m256d)
__svml_cbrt4_ha_e9:
# parameter 1: %ymm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1486.1
        .byte     15                                            #1607.552
        .byte     30                                            #1607.552
        .byte     250                                           #1607.552
	.cfi_startproc
..___tag_value___svml_cbrt4_ha_e9.115:
..L116:
                                                        #1486.1
        pushq     %rbp                                          #1486.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1486.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1486.1
        subq      $192, %rsp                                    #1486.1
        lea       __svml_dcbrt_ha_data_internal(%rip), %rax     #1554.74
        vmovapd   %ymm0, %ymm11                                 #1486.1
        vmovupd   2624+__svml_dcbrt_ha_data_internal(%rip), %xmm8 #1538.23
        vmovupd   2688+__svml_dcbrt_ha_data_internal(%rip), %xmm7 #1539.18
        vpsrlq    $52, %xmm11, %xmm4                            #1555.17
        vextractf128 $1, %ymm11, %xmm9                          #1546.101
        vshufps   $221, %xmm9, %xmm11, %xmm10                   #1547.33
        vpsrlq    $52, %xmm9, %xmm9                             #1555.68
        vandps    2752+__svml_dcbrt_ha_data_internal(%rip), %xmm10, %xmm15 #1552.22
        vpsrld    $12, %xmm15, %xmm6                            #1553.22
        vandps    2816+__svml_dcbrt_ha_data_internal(%rip), %xmm10, %xmm12 #1548.23
        vpsrld    $20, %xmm10, %xmm10                           #1568.18
        vmovd     %xmm6, %ecx                                   #1554.144
        vpsubd    3008+__svml_dcbrt_ha_data_internal(%rip), %xmm12, %xmm13 #1549.23
        vpand     %xmm8, %xmm4, %xmm12                          #1556.17
        vpand     %xmm8, %xmm9, %xmm8                           #1556.86
        vpextrd   $2, %xmm6, %edi                               #1554.381
        vpcmpgtd  3072+__svml_dcbrt_ha_data_internal(%rip), %xmm13, %xmm14 #1550.23
        vpmuludq  %xmm7, %xmm12, %xmm13                         #1557.19
        vpmuludq  %xmm7, %xmm8, %xmm7                           #1557.85
        vmovmskps %xmm14, %edx                                  #1551.40
        vshufps   $136, %xmm7, %xmm13, %xmm14                   #1559.37
        movslq    %ecx, %rcx                                    #1554.74
        vpsrld    $14, %xmm14, %xmm4                            #1560.19
        vpextrd   $1, %xmm6, %esi                               #1554.251
        movslq    %edi, %rdi                                    #1554.311
        vpextrd   $3, %xmm6, %r8d                               #1554.491
        movslq    %esi, %rsi                                    #1554.181
        movslq    %r8d, %r8                                     #1554.421
        vmovsd    (%rax,%rcx), %xmm0                            #1554.74
        vmovsd    (%rax,%rdi), %xmm1                            #1554.311
        vmovhpd   (%rax,%rsi), %xmm0, %xmm2                     #1554.57
        vmovhpd   (%rax,%r8), %xmm1, %xmm3                      #1554.294
        vshufps   $136, %xmm8, %xmm12, %xmm15                   #1558.35
        vpsubd    %xmm4, %xmm15, %xmm0                          #1562.17
        vandpd    2368+__svml_dcbrt_ha_data_internal(%rip), %ymm11, %ymm12 #1580.10
        vorpd     2304+__svml_dcbrt_ha_data_internal(%rip), %ymm12, %ymm14 #1582.10
        vinsertf128 $1, %xmm3, %ymm2, %ymm5                     #1554.11
        vpaddd    %xmm4, %xmm4, %xmm2                           #1563.17
        vpsubd    %xmm2, %xmm0, %xmm1                           #1563.17
        vpslld    $8, %xmm1, %xmm3                              #1564.17
        vpaddd    %xmm3, %xmm6, %xmm6                           #1566.27
        vpslld    $1, %xmm6, %xmm0                              #1566.27
        vpextrd   $1, %xmm0, %r10d                              #1567.226
        vmovd     %xmm0, %r9d                                   #1567.97
        movslq    %r10d, %r10                                   #1567.174
        vpextrd   $2, %xmm0, %r11d                              #1567.358
        vpextrd   $3, %xmm0, %ecx                               #1567.490
        vpand     2880+__svml_dcbrt_ha_data_internal(%rip), %xmm10, %xmm0 #1569.18
        vandpd    2496+__svml_dcbrt_ha_data_internal(%rip), %ymm11, %ymm10 #1576.9
        vorpd     2432+__svml_dcbrt_ha_data_internal(%rip), %ymm10, %ymm13 #1578.9
        movslq    %ecx, %rcx                                    #1567.438
        vsubpd    %ymm14, %ymm13, %ymm15                        #1583.9
        vmovupd   256(%rax,%r10), %xmm1                         #1567.174
        movslq    %r9d, %r9                                     #1567.45
        movslq    %r11d, %r11                                   #1567.306
        vinsertf128 $1, 256(%rax,%rcx), %ymm1, %ymm7            #1567.629
        vpor      2944+__svml_dcbrt_ha_data_internal(%rip), %xmm0, %xmm1 #1570.16
        vmulpd    %ymm15, %ymm5, %ymm0                          #1584.9
        vpaddd    %xmm4, %xmm1, %xmm4                           #1572.16
        vpslld    $20, %xmm4, %xmm8                             #1572.16
        vmovupd   256(%rax,%r9), %xmm2                          #1567.45
        vmulpd    1792+__svml_dcbrt_ha_data_internal(%rip), %ymm0, %ymm5 #1587.24
        vaddpd    1856+__svml_dcbrt_ha_data_internal(%rip), %ymm5, %ymm4 #1587.9
        vmulpd    %ymm4, %ymm0, %ymm5                           #1589.24
        vinsertf128 $1, 256(%rax,%r11), %ymm2, %ymm6            #1567.548
        vunpcklpd %ymm7, %ymm6, %ymm3                           #1567.708
        vunpckhpd %ymm7, %ymm6, %ymm2                           #1567.756
        vpxor     %xmm7, %xmm7, %xmm7                           #1573.35
        vpunpckldq %xmm8, %xmm7, %xmm6                          #1573.16
        vpunpckhdq %xmm8, %xmm7, %xmm9                          #1573.106
        vinsertf128 $1, %xmm9, %ymm6, %ymm1                     #1574.10
        vaddpd    1920+__svml_dcbrt_ha_data_internal(%rip), %ymm5, %ymm6 #1589.9
        vmulpd    %ymm1, %ymm3, %ymm3                           #1600.15
        vmulpd    %ymm1, %ymm2, %ymm1                           #1601.15
        vmulpd    %ymm6, %ymm0, %ymm7                           #1591.24
        vaddpd    1984+__svml_dcbrt_ha_data_internal(%rip), %ymm7, %ymm8 #1591.9
        vmulpd    %ymm8, %ymm0, %ymm9                           #1593.24
        vaddpd    2048+__svml_dcbrt_ha_data_internal(%rip), %ymm9, %ymm10 #1593.9
        vmulpd    %ymm10, %ymm0, %ymm12                         #1595.24
        vaddpd    2112+__svml_dcbrt_ha_data_internal(%rip), %ymm12, %ymm13 #1595.9
        vmulpd    %ymm13, %ymm0, %ymm14                         #1597.24
        vaddpd    2176+__svml_dcbrt_ha_data_internal(%rip), %ymm14, %ymm15 #1597.9
        vmulpd    %ymm15, %ymm0, %ymm4                          #1599.24
        vmulpd    %ymm3, %ymm0, %ymm0                           #1602.15
        vaddpd    2240+__svml_dcbrt_ha_data_internal(%rip), %ymm4, %ymm5 #1599.9
        vmulpd    %ymm0, %ymm5, %ymm2                           #1603.9
        vaddpd    %ymm2, %ymm1, %ymm0                           #1604.9
        vaddpd    %ymm0, %ymm3, %ymm0                           #1605.10
        testl     %edx, %edx                                    #1607.52
        jne       ..B8.3        # Prob 5%                       #1607.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm11
..B8.2:                         # Preds ..B8.3 ..B8.9 ..B8.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1610.12
        popq      %rbp                                          #1610.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1610.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.3:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm11, 64(%rsp)                              #1607.200
        vmovupd   %ymm0, 128(%rsp)                              #1607.276
        je        ..B8.2        # Prob 95%                      #1607.380
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B8.6:                         # Preds ..B8.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1607.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B8.13:                        # Preds ..B8.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B8.7:                         # Preds ..B8.8 ..B8.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1607.523
        jc        ..B8.10       # Prob 5%                       #1607.523
                                # LOE rbx r12 r14 r15 r13d
..B8.8:                         # Preds ..B8.10 ..B8.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1607.476
        cmpl      $4, %r12d                                     #1607.471
        jl        ..B8.7        # Prob 82%                      #1607.471
                                # LOE rbx r12 r14 r15 r13d
..B8.9:                         # Preds ..B8.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1607.683
        jmp       ..B8.2        # Prob 100%                     #1607.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B8.10:                        # Preds ..B8.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1607.552
        lea       128(%rsp,%r12,8), %rsi                        #1607.552
..___tag_value___svml_cbrt4_ha_e9.133:
#       __svml_dcbrt_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcbrt_ha_cout_rare_internal            #1607.552
..___tag_value___svml_cbrt4_ha_e9.134:
        jmp       ..B8.8        # Prob 100%                     #1607.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cbrt4_ha_e9,@function
	.size	__svml_cbrt4_ha_e9,.-__svml_cbrt4_ha_e9
..LN__svml_cbrt4_ha_e9.7:
	.data
# -- End  __svml_cbrt4_ha_e9
	.text
.L_2__routine_start___svml_cbrt2_ha_ex_8:
# -- Begin  __svml_cbrt2_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrt2_ha_ex
# --- __svml_cbrt2_ha_ex(__m128d)
__svml_cbrt2_ha_ex:
# parameter 1: %xmm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1615.1
        .byte     15                                            #1736.546
        .byte     30                                            #1736.546
        .byte     250                                           #1736.546
	.cfi_startproc
..___tag_value___svml_cbrt2_ha_ex.136:
..L137:
                                                        #1615.1
        pushq     %rbp                                          #1615.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1615.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1615.1
        subq      $192, %rsp                                    #1615.1
        movaps    %xmm0, %xmm10                                 #1684.11
        psrlq     $52, %xmm10                                   #1684.11
        lea       __svml_dcbrt_ha_data_internal(%rip), %r8      #1683.33
        pand      2624+__svml_dcbrt_ha_data_internal(%rip), %xmm10 #1685.11
        movdqu    2688+__svml_dcbrt_ha_data_internal(%rip), %xmm9 #1686.13
        pmuludq   %xmm10, %xmm9                                 #1686.13
        movq      2752+__svml_dcbrt_ha_data_internal(%rip), %xmm13 #1681.16
        movq      2816+__svml_dcbrt_ha_data_internal(%rip), %xmm7 #1677.17
        pshufd    $221, %xmm0, %xmm4                            #1676.9
        movq      2880+__svml_dcbrt_ha_data_internal(%rip), %xmm3 #1671.16
        pand      %xmm4, %xmm7                                  #1677.17
        movq      3008+__svml_dcbrt_ha_data_internal(%rip), %xmm5 #1673.11
        pand      %xmm4, %xmm13                                 #1681.16
        psrld     $20, %xmm4                                    #1697.12
        psubd     %xmm5, %xmm7                                  #1678.17
        movq      2944+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1672.12
        pand      %xmm3, %xmm4                                  #1698.12
        pshufd    $136, %xmm9, %xmm5                            #1688.13
        por       %xmm2, %xmm4                                  #1699.10
        psrld     $14, %xmm5                                    #1689.13
        psrld     $12, %xmm13                                   #1682.16
        paddd     %xmm5, %xmm4                                  #1701.10
        pxor      %xmm3, %xmm3                                  #1702.16
        pslld     $20, %xmm4                                    #1701.10
        movdqa    %xmm5, %xmm11                                 #1692.11
        movd      %xmm13, %edx                                  #1683.103
        paddd     %xmm5, %xmm11                                 #1692.11
        pshufd    $1, %xmm13, %xmm8                             #1683.226
        punpckldq %xmm4, %xmm3                                  #1702.16
        movups    2496+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1705.9
        movups    2368+__svml_dcbrt_ha_data_internal(%rip), %xmm4 #1709.10
        andps     %xmm0, %xmm2                                  #1705.9
        movd      %xmm8, %ecx                                   #1683.208
        andps     %xmm0, %xmm4                                  #1709.10
        orps      2432+__svml_dcbrt_ha_data_internal(%rip), %xmm2 #1707.9
        orps      2304+__svml_dcbrt_ha_data_internal(%rip), %xmm4 #1711.10
        movslq    %edx, %rdx                                    #1683.33
        subpd     %xmm4, %xmm2                                  #1712.9
        movslq    %ecx, %rcx                                    #1683.138
        movsd     (%r8,%rdx), %xmm1                             #1683.33
        pshufd    $136, %xmm10, %xmm12                          #1687.11
        psubd     %xmm5, %xmm12                                 #1691.11
        movhpd    (%r8,%rcx), %xmm1                             #1683.17
        psubd     %xmm11, %xmm12                                #1692.11
        mulpd     %xmm2, %xmm1                                  #1713.9
        pslld     $8, %xmm12                                    #1693.11
        paddd     %xmm12, %xmm13                                #1695.21
        pslld     $1, %xmm13                                    #1695.21
        movd      %xmm13, %esi                                  #1696.119
        pshufd    $1, %xmm13, %xmm14                            #1696.286
        movups    1792+__svml_dcbrt_ha_data_internal(%rip), %xmm13 #1714.41
        mulpd     %xmm1, %xmm13                                 #1716.21
        movd      %xmm14, %edi                                  #1696.268
        movq      3072+__svml_dcbrt_ha_data_internal(%rip), %xmm6 #1674.11
        pcmpgtd   %xmm6, %xmm7                                  #1679.17
        addpd     1856+__svml_dcbrt_ha_data_internal(%rip), %xmm13 #1716.9
        movmskps  %xmm7, %eax                                   #1680.40
        mulpd     %xmm1, %xmm13                                 #1718.21
        addpd     1920+__svml_dcbrt_ha_data_internal(%rip), %xmm13 #1718.9
        mulpd     %xmm1, %xmm13                                 #1720.21
        addpd     1984+__svml_dcbrt_ha_data_internal(%rip), %xmm13 #1720.9
        mulpd     %xmm1, %xmm13                                 #1722.21
        addpd     2048+__svml_dcbrt_ha_data_internal(%rip), %xmm13 #1722.9
        mulpd     %xmm1, %xmm13                                 #1724.21
        addpd     2112+__svml_dcbrt_ha_data_internal(%rip), %xmm13 #1724.9
        mulpd     %xmm1, %xmm13                                 #1726.21
        movslq    %esi, %rsi                                    #1696.67
        movslq    %edi, %rdi                                    #1696.216
        addpd     2176+__svml_dcbrt_ha_data_internal(%rip), %xmm13 #1726.9
        movups    256(%r8,%rsi), %xmm6                          #1696.67
        movups    256(%r8,%rdi), %xmm15                         #1696.216
        movaps    %xmm6, %xmm7                                  #1696.365
        unpcklpd  %xmm15, %xmm7                                 #1696.365
        mulpd     %xmm3, %xmm7                                  #1729.15
        mulpd     %xmm1, %xmm13                                 #1728.21
        mulpd     %xmm7, %xmm1                                  #1731.15
        addpd     2240+__svml_dcbrt_ha_data_internal(%rip), %xmm13 #1728.9
        unpckhpd  %xmm15, %xmm6                                 #1696.414
        mulpd     %xmm3, %xmm6                                  #1730.15
        mulpd     %xmm1, %xmm13                                 #1732.9
        addpd     %xmm13, %xmm6                                 #1733.9
        addpd     %xmm6, %xmm7                                  #1734.10
        andl      $3, %eax                                      #1680.94
        jne       ..B9.3        # Prob 5%                       #1736.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm7
..B9.2:                         # Preds ..B9.3 ..B9.9 ..B9.1
                                # Execution count [1.00e+00]
        movaps    %xmm7, %xmm0                                  #1739.12
        movq      %rbp, %rsp                                    #1739.12
        popq      %rbp                                          #1739.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1739.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.3:                         # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #1736.197
        movups    %xmm7, 128(%rsp)                              #1736.270
        je        ..B9.2        # Prob 95%                      #1736.374
                                # LOE rbx r12 r13 r14 r15 eax xmm7
..B9.6:                         # Preds ..B9.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1736.454
        movq      %r12, 8(%rsp)                                 #1736.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #1736.454
        movq      %r13, (%rsp)                                  #1736.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #1736.454
                                # LOE rbx r12 r14 r15 r13d
..B9.7:                         # Preds ..B9.8 ..B9.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1736.517
        jc        ..B9.10       # Prob 5%                       #1736.517
                                # LOE rbx r12 r14 r15 r13d
..B9.8:                         # Preds ..B9.10 ..B9.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1736.470
        cmpl      $2, %r12d                                     #1736.465
        jl        ..B9.7        # Prob 82%                      #1736.465
                                # LOE rbx r12 r14 r15 r13d
..B9.9:                         # Preds ..B9.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm7                              #1736.674
        jmp       ..B9.2        # Prob 100%                     #1736.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm7
..B9.10:                        # Preds ..B9.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1736.546
        lea       128(%rsp,%r12,8), %rsi                        #1736.546
..___tag_value___svml_cbrt2_ha_ex.154:
#       __svml_dcbrt_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcbrt_ha_cout_rare_internal            #1736.546
..___tag_value___svml_cbrt2_ha_ex.155:
        jmp       ..B9.8        # Prob 100%                     #1736.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cbrt2_ha_ex,@function
	.size	__svml_cbrt2_ha_ex,.-__svml_cbrt2_ha_ex
..LN__svml_cbrt2_ha_ex.8:
	.data
# -- End  __svml_cbrt2_ha_ex
	.text
.L_2__routine_start___svml_dcbrt_ha_cout_rare_internal_9:
# -- Begin  __svml_dcbrt_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dcbrt_ha_cout_rare_internal
	.globl __svml_dcbrt_ha_cout_rare_internal
# --- __svml_dcbrt_ha_cout_rare_internal(const double *, double *)
__svml_dcbrt_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #888.1
        .byte     15                                            #963.15
        .byte     30                                            #963.15
        .byte     250                                           #963.15
	.cfi_startproc
..___tag_value___svml_dcbrt_ha_cout_rare_internal.157:
..L158:
                                                        #888.1
        movq      %rsi, %r8                                     #888.1
        movzwl    6(%rdi), %r9d                                 #902.35
        andl      $32752, %r9d                                  #902.35
        shrl      $4, %r9d                                      #902.35
        movb      7(%rdi), %sil                                 #904.20
        movsd     (%rdi), %xmm1                                 #904.20
        cmpl      $2047, %r9d                                   #902.61
        je        ..B10.9       # Prob 16%                      #902.61
                                # LOE rbx rbp r8 r12 r13 r14 r15 r9d sil xmm1
..B10.2:                        # Preds ..B10.1
                                # Execution count [8.40e-01]
        ucomisd   432+_vmldCbrtHATab(%rip), %xmm1               #904.43
        jp        ..B10.3       # Prob 0%                       #904.43
        je        ..B10.8       # Prob 16%                      #904.43
                                # LOE rbx rbp r8 r12 r13 r14 r15 r9d sil xmm1
..B10.3:                        # Preds ..B10.2
                                # Execution count [7.06e-01]
        movb      %sil, %al                                     #906.84
        lea       440+_vmldCbrtHATab(%rip), %rdx                #906.42
        andb      $-128, %al                                    #906.84
        andb      $127, %sil                                    #908.38
        shrb      $7, %al                                       #906.84
        xorl      %edi, %edi                                    #909.17
        movsd     %xmm1, -56(%rsp)                              #907.17
        movzbl    %al, %ecx                                     #906.42
        movb      %sil, -49(%rsp)                               #908.38
        movsd     (%rdx,%rcx,8), %xmm5                          #906.42
        testl     %r9d, %r9d                                    #910.68
        jne       ..B10.5       # Prob 50%                      #910.68
                                # LOE rbx rbp r8 r12 r13 r14 r15 edi xmm5
..B10.4:                        # Preds ..B10.3
                                # Execution count [3.53e-01]
        movsd     -56(%rsp), %xmm0                              #912.21
        movl      $100, %edi                                    #913.21
        mulsd     360+_vmldCbrtHATab(%rip), %xmm0               #912.21
        movsd     %xmm0, -56(%rsp)                              #912.21
        jmp       ..B10.6       # Prob 100%                     #912.21
                                # LOE rbx rbp r8 r12 r13 r14 r15 edi xmm0 xmm5
..B10.5:                        # Preds ..B10.3
                                # Execution count [3.53e-01]
        movsd     -56(%rsp), %xmm0                              #920.21
                                # LOE rbx rbp r8 r12 r13 r14 r15 edi xmm0 xmm5
..B10.6:                        # Preds ..B10.4 ..B10.5
                                # Execution count [7.06e-01]
        movzwl    -50(%rsp), %esi                               #915.42
        movl      $1431655766, %eax                             #916.29
        andl      $32752, %esi                                  #915.42
        lea       _vmldCbrtHATab(%rip), %r11                    #924.41
        shrl      $4, %esi                                      #915.42
        movsd     %xmm0, -40(%rsp)                              #920.17
        movsd     368+_vmldCbrtHATab(%rip), %xmm14              #931.57
        imull     %esi                                          #916.29
        movl      $1431655766, %eax                             #917.43
        lea       (%rdx,%rdx,2), %ecx                           #916.29
        negl      %ecx                                          #916.29
        addl      %esi, %ecx                                    #916.29
        subl      %ecx, %esi                                    #917.22
        addl      %ecx, %ecx                                    #951.88
        addl      $-1023, %esi                                  #917.34
        imull     %esi                                          #917.43
        sarl      $31, %esi                                     #917.43
        subl      %esi, %edx                                    #917.43
        addl      $1023, %edx                                   #919.68
        subl      %edi, %edx                                    #919.68
        movzwl    -34(%rsp), %edi                               #921.38
        andl      $2047, %edx                                   #919.38
        andl      $-32753, %edi                                 #921.38
        addl      $16368, %edi                                  #921.38
        movw      %di, -34(%rsp)                                #921.38
        movsd     -40(%rsp), %xmm11                             #926.31
        movaps    %xmm11, %xmm6                                 #926.55
        mulsd     376+_vmldCbrtHATab(%rip), %xmm6               #926.55
        movsd     %xmm6, -32(%rsp)                              #926.17
        movsd     -32(%rsp), %xmm7                              #926.93
        movl      -36(%rsp), %r10d                              #922.45
        andl      $1048575, %r10d                               #922.45
        subsd     -40(%rsp), %xmm7                              #926.98
        movsd     %xmm7, -24(%rsp)                              #926.79
        movsd     -32(%rsp), %xmm9                              #926.119
        movsd     -24(%rsp), %xmm8                              #926.124
        shrl      $15, %r10d                                    #923.32
        subsd     %xmm8, %xmm9                                  #926.124
        movsd     %xmm9, -32(%rsp)                              #926.105
        movsd     -32(%rsp), %xmm10                             #926.150
        movsd     (%r11,%r10,8), %xmm4                          #924.41
        subsd     %xmm10, %xmm11                                #926.150
        movaps    %xmm4, %xmm12                                 #925.28
        movaps    %xmm4, %xmm13                                 #929.77
        mulsd     %xmm4, %xmm12                                 #925.28
        movsd     %xmm11, -24(%rsp)                             #926.130
        movsd     -32(%rsp), %xmm2                              #926.162
        mulsd     %xmm12, %xmm2                                 #927.31
        mulsd     %xmm2, %xmm13                                 #929.77
        movsd     440+_vmldCbrtHATab(%rip), %xmm6               #929.77
        movsd     -24(%rsp), %xmm3                              #926.172
        subsd     %xmm13, %xmm6                                 #929.77
        mulsd     %xmm12, %xmm3                                 #928.31
        mulsd     %xmm6, %xmm14                                 #931.57
        mulsd     %xmm3, %xmm4                                  #930.42
        movsd     %xmm14, -32(%rsp)                             #931.17
        movsd     -32(%rsp), %xmm15                             #931.95
        xorps     .L_2il0floatpacket.104(%rip), %xmm4           #930.42
        subsd     %xmm6, %xmm15                                 #931.100
        movsd     %xmm15, -24(%rsp)                             #931.81
        movsd     -32(%rsp), %xmm1                              #931.123
        movsd     -24(%rsp), %xmm0                              #931.128
        movsd     256+_vmldCbrtHATab(%rip), %xmm9               #938.72
        subsd     %xmm0, %xmm1                                  #931.128
        movsd     %xmm1, -32(%rsp)                              #931.109
        movsd     -32(%rsp), %xmm13                             #931.156
        movsd     352+_vmldCbrtHATab(%rip), %xmm0               #934.65
        subsd     %xmm13, %xmm6                                 #931.156
        movsd     %xmm6, -24(%rsp)                              #931.134
        movsd     -32(%rsp), %xmm1                              #931.168
        movsd     -24(%rsp), %xmm7                              #931.177
        movaps    %xmm1, %xmm8                                  #933.27
        movsd     256+_vmldCbrtHATab(%rip), %xmm11              #938.136
        addsd     %xmm7, %xmm4                                  #932.17
        movsd     256+_vmldCbrtHATab(%rip), %xmm7               #938.250
        addsd     %xmm4, %xmm8                                  #933.27
        mulsd     %xmm8, %xmm0                                  #934.65
        movslq    %ecx, %rcx                                    #951.57
        addsd     344+_vmldCbrtHATab(%rip), %xmm0               #934.86
        mulsd     %xmm8, %xmm0                                  #934.109
        movq      440+_vmldCbrtHATab(%rip), %r9                 #918.44
        movq      %r9, -48(%rsp)                                #918.17
        shrq      $48, %r9                                      #919.38
        addsd     336+_vmldCbrtHATab(%rip), %xmm0               #934.130
        mulsd     %xmm8, %xmm0                                  #934.153
        andl      $-32753, %r9d                                 #919.38
        shll      $4, %edx                                      #919.38
        addsd     328+_vmldCbrtHATab(%rip), %xmm0               #934.174
        mulsd     %xmm8, %xmm0                                  #935.32
        orl       %edx, %r9d                                    #919.38
        movw      %r9w, -42(%rsp)                               #919.38
        addsd     320+_vmldCbrtHATab(%rip), %xmm0               #935.54
        mulsd     %xmm8, %xmm0                                  #935.77
        addsd     312+_vmldCbrtHATab(%rip), %xmm0               #935.99
        mulsd     %xmm8, %xmm0                                  #935.122
        addsd     304+_vmldCbrtHATab(%rip), %xmm0               #935.143
        mulsd     %xmm8, %xmm0                                  #936.32
        addsd     296+_vmldCbrtHATab(%rip), %xmm0               #936.54
        mulsd     %xmm8, %xmm0                                  #936.77
        addsd     288+_vmldCbrtHATab(%rip), %xmm0               #936.99
        mulsd     %xmm8, %xmm0                                  #936.122
        addsd     280+_vmldCbrtHATab(%rip), %xmm0               #936.143
        mulsd     %xmm8, %xmm0                                  #937.32
        addsd     272+_vmldCbrtHATab(%rip), %xmm0               #937.54
        mulsd     %xmm8, %xmm0                                  #937.77
        addsd     %xmm0, %xmm9                                  #938.72
        movsd     %xmm9, -32(%rsp)                              #938.17
        movsd     -32(%rsp), %xmm10                             #938.136
        movsd     368+_vmldCbrtHATab(%rip), %xmm9               #940.57
        subsd     %xmm10, %xmm11                                #938.136
        movsd     %xmm11, -24(%rsp)                             #938.81
        movsd     -32(%rsp), %xmm14                             #938.156
        movsd     -24(%rsp), %xmm12                             #938.161
        addsd     %xmm12, %xmm14                                #938.161
        movsd     %xmm14, -16(%rsp)                             #938.142
        movaps    %xmm2, %xmm14                                 #946.74
        movsd     -24(%rsp), %xmm6                              #938.189
        addsd     %xmm0, %xmm6                                  #938.189
        movsd     %xmm6, -24(%rsp)                              #938.167
        movsd     -16(%rsp), %xmm15                             #938.250
        subsd     %xmm15, %xmm7                                 #938.250
        movsd     %xmm7, -16(%rsp)                              #938.195
        movsd     -24(%rsp), %xmm8                              #938.270
        movsd     -16(%rsp), %xmm0                              #938.275
        addsd     %xmm0, %xmm8                                  #938.275
        movsd     %xmm8, -16(%rsp)                              #938.256
        movaps    %xmm1, %xmm8                                  #942.39
        movsd     -32(%rsp), %xmm13                             #938.287
        mulsd     %xmm13, %xmm9                                 #940.57
        movsd     -16(%rsp), %xmm0                              #938.297
        movsd     %xmm9, -32(%rsp)                              #940.17
        movsd     -32(%rsp), %xmm10                             #940.95
        subsd     %xmm13, %xmm10                                #940.100
        addsd     264+_vmldCbrtHATab(%rip), %xmm0               #939.17
        movsd     %xmm10, -24(%rsp)                             #940.81
        movsd     -32(%rsp), %xmm11                             #940.123
        movsd     -24(%rsp), %xmm6                              #940.128
        subsd     %xmm6, %xmm11                                 #940.128
        movsd     %xmm11, -32(%rsp)                             #940.109
        movsd     -32(%rsp), %xmm12                             #940.156
        subsd     %xmm12, %xmm13                                #940.156
        movsd     %xmm13, -24(%rsp)                             #940.134
        movsd     -32(%rsp), %xmm7                              #940.168
        movsd     -24(%rsp), %xmm6                              #940.177
        mulsd     %xmm7, %xmm8                                  #942.39
        addsd     %xmm0, %xmm6                                  #941.17
        mulsd     %xmm4, %xmm7                                  #942.106
        mulsd     %xmm6, %xmm4                                  #942.70
        mulsd     %xmm6, %xmm1                                  #942.142
        addsd     %xmm4, %xmm7                                  #942.106
        movsd     368+_vmldCbrtHATab(%rip), %xmm4               #943.57
        addsd     %xmm1, %xmm7                                  #942.142
        mulsd     %xmm8, %xmm4                                  #943.57
        movsd     %xmm7, -32(%rsp)                              #942.115
        movsd     -32(%rsp), %xmm10                             #942.167
        movsd     %xmm4, -32(%rsp)                              #943.17
        movsd     -32(%rsp), %xmm0                              #943.95
        subsd     %xmm8, %xmm0                                  #943.100
        movsd     %xmm0, -24(%rsp)                              #943.81
        movsd     -32(%rsp), %xmm1                              #943.123
        movsd     -24(%rsp), %xmm4                              #943.128
        subsd     %xmm4, %xmm1                                  #943.128
        movsd     %xmm1, -32(%rsp)                              #943.109
        movsd     -32(%rsp), %xmm6                              #943.156
        subsd     %xmm6, %xmm8                                  #943.156
        movsd     %xmm8, -24(%rsp)                              #943.134
        movsd     -32(%rsp), %xmm9                              #943.168
        movsd     -24(%rsp), %xmm7                              #943.177
        movaps    %xmm9, %xmm1                                  #945.39
        mulsd     %xmm3, %xmm9                                  #945.110
        addsd     %xmm7, %xmm10                                 #944.17
        mulsd     %xmm2, %xmm1                                  #945.39
        movaps    %xmm10, %xmm11                                #945.148
        movaps    %xmm1, %xmm12                                 #946.41
        mulsd     %xmm3, %xmm10                                 #945.72
        addsd     %xmm2, %xmm12                                 #946.41
        mulsd     %xmm2, %xmm11                                 #945.148
        addsd     %xmm9, %xmm10                                 #945.110
        addsd     %xmm10, %xmm11                                #945.148
        movsd     %xmm11, -32(%rsp)                             #945.121
        movsd     -32(%rsp), %xmm0                              #945.175
        movsd     %xmm12, -32(%rsp)                             #946.17
        movsd     -32(%rsp), %xmm13                             #946.74
        subsd     %xmm13, %xmm14                                #946.74
        movsd     %xmm14, -24(%rsp)                             #946.50
        movsd     -32(%rsp), %xmm9                              #946.94
        movsd     -24(%rsp), %xmm15                             #946.99
        addsd     %xmm15, %xmm9                                 #946.99
        movsd     %xmm9, -16(%rsp)                              #946.80
        movsd     -24(%rsp), %xmm10                             #946.127
        addsd     %xmm10, %xmm1                                 #946.127
        movsd     %xmm1, -24(%rsp)                              #946.105
        movsd     -16(%rsp), %xmm4                              #946.157
        subsd     %xmm4, %xmm2                                  #946.157
        movsd     368+_vmldCbrtHATab(%rip), %xmm4               #947.57
        movsd     %xmm2, -16(%rsp)                              #946.133
        movsd     -24(%rsp), %xmm1                              #946.177
        movsd     -16(%rsp), %xmm2                              #946.182
        addsd     %xmm2, %xmm1                                  #946.182
        movsd     %xmm1, -16(%rsp)                              #946.163
        movsd     -32(%rsp), %xmm9                              #946.194
        mulsd     %xmm9, %xmm4                                  #947.57
        movsd     -16(%rsp), %xmm11                             #946.204
        movsd     %xmm4, -32(%rsp)                              #947.17
        movsd     -32(%rsp), %xmm6                              #947.95
        subsd     %xmm9, %xmm6                                  #947.100
        movsd     %xmm6, -24(%rsp)                              #947.81
        movsd     -32(%rsp), %xmm7                              #947.123
        movsd     -24(%rsp), %xmm2                              #947.128
        subsd     %xmm2, %xmm7                                  #947.128
        movsd     %xmm7, -32(%rsp)                              #947.109
        movsd     -32(%rsp), %xmm8                              #947.156
        subsd     %xmm8, %xmm9                                  #947.156
        movsd     %xmm9, -24(%rsp)                              #947.134
        movsd     -32(%rsp), %xmm12                             #947.168
        movsd     -24(%rsp), %xmm10                             #947.177
        addsd     %xmm0, %xmm10                                 #949.17
        addsd     %xmm3, %xmm10                                 #950.17
        movsd     392(%r11,%rcx,8), %xmm3                       #951.218
        movaps    %xmm3, %xmm0                                  #951.135
        addsd     %xmm10, %xmm11                                #948.17
        mulsd     %xmm12, %xmm3                                 #951.218
        mulsd     %xmm11, %xmm0                                 #951.135
        movsd     384(%r11,%rcx,8), %xmm10                      #951.57
        addsd     %xmm3, %xmm0                                  #951.218
        mulsd     %xmm10, %xmm11                                #951.301
        mulsd     %xmm10, %xmm12                                #951.57
        addsd     %xmm11, %xmm0                                 #951.301
        movsd     %xmm0, -32(%rsp)                              #951.256
        movsd     -32(%rsp), %xmm3                              #951.355
        addsd     %xmm3, %xmm12                                 #952.30
        mulsd     -48(%rsp), %xmm12                             #953.17
        mulsd     %xmm12, %xmm5                                 #954.32
        movsd     %xmm5, (%r8)                                  #954.19
                                # LOE rbx rbp r12 r13 r14 r15
..B10.7:                        # Preds ..B10.6 ..B10.8 ..B10.9
                                # Execution count [1.00e+00]
        xorl      %eax, %eax                                    #965.12
        ret                                                     #965.12
                                # LOE
..B10.8:                        # Preds ..B10.2
                                # Execution count [1.34e-01]: Infreq
        movsd     440+_vmldCbrtHATab(%rip), %xmm0               #958.66
        mulsd     %xmm0, %xmm1                                  #958.66
        movsd     %xmm1, (%r8)                                  #958.19
        jmp       ..B10.7       # Prob 100%                     #958.19
                                # LOE rbx rbp r12 r13 r14 r15
..B10.9:                        # Preds ..B10.1
                                # Execution count [1.60e-01]: Infreq
        addsd     %xmm1, %xmm1                                  #963.29
        movsd     %xmm1, (%r8)                                  #963.15
        jmp       ..B10.7       # Prob 100%                     #963.15
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_dcbrt_ha_cout_rare_internal,@function
	.size	__svml_dcbrt_ha_cout_rare_internal,.-__svml_dcbrt_ha_cout_rare_internal
..LN__svml_dcbrt_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dcbrt_ha_cout_rare_internal
	.section .rodata, "a"
	.space 32, 0x00 	# pad
	.align 64
	.hidden __svml_dcbrt_ha_data_internal_avx512
	.globl __svml_dcbrt_ha_data_internal_avx512
__svml_dcbrt_ha_data_internal_avx512:
	.long	0
	.long	1072693248
	.long	4186796683
	.long	1072965794
	.long	2772266557
	.long	1073309182
	.long	0
	.long	0
	.long	0
	.long	3220176896
	.long	4186796683
	.long	3220449442
	.long	2772266557
	.long	3220792830
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1418634270
	.long	3162364962
	.long	2576690953
	.long	3164558313
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1418634270
	.long	1014881314
	.long	2576690953
	.long	1017074665
	.long	0
	.long	0
	.long	4186796683
	.long	1072965794
	.long	1554061055
	.long	1072914931
	.long	3992368458
	.long	1072871093
	.long	3714535808
	.long	1072832742
	.long	954824104
	.long	1072798779
	.long	3256858690
	.long	1072768393
	.long	3858344660
	.long	1072740974
	.long	1027250248
	.long	1072716050
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
	.long	1418634270
	.long	3162364962
	.long	629721892
	.long	1016287007
	.long	1776620500
	.long	3163956186
	.long	648592220
	.long	1016269578
	.long	1295766103
	.long	3161896715
	.long	1348094586
	.long	3164476360
	.long	2407028709
	.long	1015925873
	.long	497428409
	.long	1014435402
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
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
	.long	1431655766
	.long	1070945621
	.long	1431655766
	.long	1070945621
	.long	1431655766
	.long	1070945621
	.long	1431655766
	.long	1070945621
	.long	1431655766
	.long	1070945621
	.long	1431655766
	.long	1070945621
	.long	1431655766
	.long	1070945621
	.long	1431655766
	.long	1070945621
	.long	0
	.long	1126170624
	.long	0
	.long	1126170624
	.long	0
	.long	1126170624
	.long	0
	.long	1126170624
	.long	0
	.long	1126170624
	.long	0
	.long	1126170624
	.long	0
	.long	1126170624
	.long	0
	.long	1126170624
	.long	0
	.long	1074266112
	.long	0
	.long	1074266112
	.long	0
	.long	1074266112
	.long	0
	.long	1074266112
	.long	0
	.long	1074266112
	.long	0
	.long	1074266112
	.long	0
	.long	1074266112
	.long	0
	.long	1074266112
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
	.long	1792985698
	.long	3213372987
	.long	1792985698
	.long	3213372987
	.long	1792985698
	.long	3213372987
	.long	1792985698
	.long	3213372987
	.long	1792985698
	.long	3213372987
	.long	1792985698
	.long	3213372987
	.long	1792985698
	.long	3213372987
	.long	1792985698
	.long	3213372987
	.long	3135539317
	.long	1066129956
	.long	3135539317
	.long	1066129956
	.long	3135539317
	.long	1066129956
	.long	3135539317
	.long	1066129956
	.long	3135539317
	.long	1066129956
	.long	3135539317
	.long	1066129956
	.long	3135539317
	.long	1066129956
	.long	3135539317
	.long	1066129956
	.long	2087834975
	.long	3213899448
	.long	2087834975
	.long	3213899448
	.long	2087834975
	.long	3213899448
	.long	2087834975
	.long	3213899448
	.long	2087834975
	.long	3213899448
	.long	2087834975
	.long	3213899448
	.long	2087834975
	.long	3213899448
	.long	2087834975
	.long	3213899448
	.long	2476259604
	.long	1066628333
	.long	2476259604
	.long	1066628333
	.long	2476259604
	.long	1066628333
	.long	2476259604
	.long	1066628333
	.long	2476259604
	.long	1066628333
	.long	2476259604
	.long	1066628333
	.long	2476259604
	.long	1066628333
	.long	2476259604
	.long	1066628333
	.long	2012366478
	.long	3214412045
	.long	2012366478
	.long	3214412045
	.long	2012366478
	.long	3214412045
	.long	2012366478
	.long	3214412045
	.long	2012366478
	.long	3214412045
	.long	2012366478
	.long	3214412045
	.long	2012366478
	.long	3214412045
	.long	2012366478
	.long	3214412045
	.long	1104999785
	.long	1067378449
	.long	1104999785
	.long	1067378449
	.long	1104999785
	.long	1067378449
	.long	1104999785
	.long	1067378449
	.long	1104999785
	.long	1067378449
	.long	1104999785
	.long	1067378449
	.long	1104999785
	.long	1067378449
	.long	1104999785
	.long	1067378449
	.long	3534763582
	.long	3215266280
	.long	3534763582
	.long	3215266280
	.long	3534763582
	.long	3215266280
	.long	3534763582
	.long	3215266280
	.long	3534763582
	.long	3215266280
	.long	3534763582
	.long	3215266280
	.long	3534763582
	.long	3215266280
	.long	3534763582
	.long	3215266280
	.long	1007386161
	.long	1068473053
	.long	1007386161
	.long	1068473053
	.long	1007386161
	.long	1068473053
	.long	1007386161
	.long	1068473053
	.long	1007386161
	.long	1068473053
	.long	1007386161
	.long	1068473053
	.long	1007386161
	.long	1068473053
	.long	1007386161
	.long	1068473053
	.long	477218625
	.long	3216798151
	.long	477218625
	.long	3216798151
	.long	477218625
	.long	3216798151
	.long	477218625
	.long	3216798151
	.long	477218625
	.long	3216798151
	.long	477218625
	.long	3216798151
	.long	477218625
	.long	3216798151
	.long	477218625
	.long	3216798151
	.long	1431655767
	.long	1070945621
	.long	1431655767
	.long	1070945621
	.long	1431655767
	.long	1070945621
	.long	1431655767
	.long	1070945621
	.long	1431655767
	.long	1070945621
	.long	1431655767
	.long	1070945621
	.long	1431655767
	.long	1070945621
	.long	1431655767
	.long	1070945621
	.type	__svml_dcbrt_ha_data_internal_avx512,@object
	.size	__svml_dcbrt_ha_data_internal_avx512,1408
	.align 64
	.hidden __svml_dcbrt_ha_data_internal
	.globl __svml_dcbrt_ha_data_internal
__svml_dcbrt_ha_data_internal:
	.long	528611360
	.long	3220144632
	.long	2884679527
	.long	3220082993
	.long	1991868891
	.long	3220024928
	.long	2298714891
	.long	3219970134
	.long	58835168
	.long	3219918343
	.long	3035110223
	.long	3219869313
	.long	1617585086
	.long	3219822831
	.long	2500867033
	.long	3219778702
	.long	4241943008
	.long	3219736752
	.long	258732970
	.long	3219696825
	.long	404232216
	.long	3219658776
	.long	2172167368
	.long	3219622476
	.long	1544257904
	.long	3219587808
	.long	377579543
	.long	3219554664
	.long	1616385542
	.long	3219522945
	.long	813783277
	.long	3219492562
	.long	3940743189
	.long	3219463431
	.long	2689777499
	.long	3219435478
	.long	1700977147
	.long	3219408632
	.long	3169102082
	.long	3219382828
	.long	327235604
	.long	3219358008
	.long	1244336319
	.long	3219334115
	.long	1300311200
	.long	3219311099
	.long	3095471925
	.long	3219288912
	.long	2166487928
	.long	3219267511
	.long	2913108253
	.long	3219246854
	.long	293672978
	.long	3219226904
	.long	288737297
	.long	3219207624
	.long	1810275472
	.long	3219188981
	.long	174592167
	.long	3219170945
	.long	3539053052
	.long	3219153485
	.long	2164392968
	.long	3219136576
	.long	572345495
	.long	1072698681
	.long	831114197
	.long	1014426140
	.long	1998204467
	.long	1072709382
	.long	2676852344
	.long	1016444374
	.long	3861501553
	.long	1072719872
	.long	2448152898
	.long	1017271343
	.long	2268192434
	.long	1072730162
	.long	571097351
	.long	1016882671
	.long	2981979308
	.long	1072740260
	.long	4262088991
	.long	1013165739
	.long	270859143
	.long	1072750176
	.long	4129526850
	.long	1017541534
	.long	2958651392
	.long	1072759916
	.long	3359808165
	.long	1017217531
	.long	313113243
	.long	1072769490
	.long	483947492
	.long	1017266572
	.long	919449400
	.long	1072778903
	.long	3198298149
	.long	1017689293
	.long	2809328903
	.long	1072788162
	.long	427389337
	.long	1018030605
	.long	2222981587
	.long	1072797274
	.long	1956982634
	.long	1014089381
	.long	2352530781
	.long	1072806244
	.long	1432693183
	.long	1016413476
	.long	594152517
	.long	1072815078
	.long	4116689778
	.long	1016313239
	.long	1555767199
	.long	1072823780
	.long	4199324994
	.long	1014195323
	.long	4282421314
	.long	1072832355
	.long	1593346102
	.long	1017825413
	.long	2355578597
	.long	1072840809
	.long	1274262487
	.long	1016625594
	.long	1162590619
	.long	1072849145
	.long	334784993
	.long	1017096972
	.long	797864051
	.long	1072857367
	.long	568813162
	.long	1017168973
	.long	431273680
	.long	1072865479
	.long	3584554625
	.long	1017710626
	.long	2669831148
	.long	1072873484
	.long	3971702574
	.long	1017159584
	.long	733477752
	.long	1072881387
	.long	218128012
	.long	1018019915
	.long	4280220604
	.long	1072889189
	.long	75187235
	.long	1015924824
	.long	801961634
	.long	1072896896
	.long	3536670393
	.long	1016038493
	.long	2915370760
	.long	1072904508
	.long	839578387
	.long	1018086148
	.long	1159613482
	.long	1072912030
	.long	3759814061
	.long	1017352914
	.long	2689944798
	.long	1072919463
	.long	3143542556
	.long	1017717014
	.long	1248687822
	.long	1072926811
	.long	3006519911
	.long	1012519288
	.long	2967951030
	.long	1072934075
	.long	3521851764
	.long	1017830454
	.long	630170432
	.long	1072941259
	.long	1718375358
	.long	1017387255
	.long	3760898254
	.long	1072948363
	.long	85748770
	.long	1017604842
	.long	0
	.long	1072955392
	.long	0
	.long	0
	.long	2370273294
	.long	1072962345
	.long	1629859066
	.long	1013153035
	.long	1261754802
	.long	1072972640
	.long	113977743
	.long	1015673025
	.long	546334065
	.long	1072986123
	.long	1008348646
	.long	1016413877
	.long	1054893830
	.long	1072999340
	.long	2186484259
	.long	1018078778
	.long	1571187597
	.long	1073012304
	.long	1446107332
	.long	1015291210
	.long	1107975175
	.long	1073025027
	.long	69773316
	.long	1016481646
	.long	3606909377
	.long	1073037519
	.long	466887756
	.long	1017226688
	.long	1113616747
	.long	1073049792
	.long	3265289890
	.long	1017622569
	.long	4154744632
	.long	1073061853
	.long	2567589881
	.long	1015245005
	.long	3358931423
	.long	1073073713
	.long	343132434
	.long	1015771654
	.long	4060702372
	.long	1073085379
	.long	4013097658
	.long	1017904433
	.long	747576176
	.long	1073096860
	.long	1061470230
	.long	1014919738
	.long	3023138255
	.long	1073108161
	.long	1212724758
	.long	1018027762
	.long	1419988548
	.long	1073119291
	.long	3844345246
	.long	1014188976
	.long	1914185305
	.long	1073130255
	.long	303260851
	.long	1016023904
	.long	294389948
	.long	1073141060
	.long	3001102400
	.long	1016940338
	.long	3761802570
	.long	1073151710
	.long	41769798
	.long	1015538023
	.long	978281566
	.long	1073162213
	.long	1018481845
	.long	1017509529
	.long	823148820
	.long	1073172572
	.long	4070910954
	.long	1017208735
	.long	2420954441
	.long	1073182792
	.long	1209676399
	.long	1017337941
	.long	3815449908
	.long	1073192878
	.long	786936659
	.long	1017169506
	.long	2046058587
	.long	1073202835
	.long	3809068538
	.long	1017278211
	.long	1807524753
	.long	1073212666
	.long	2107909763
	.long	1017749298
	.long	2628681401
	.long	1073222375
	.long	638724993
	.long	1018143910
	.long	3225667357
	.long	1073231966
	.long	2846951590
	.long	1016668860
	.long	1555307421
	.long	1073241443
	.long	3233424021
	.long	1017889915
	.long	3454043099
	.long	1073250808
	.long	3181654301
	.long	1017329873
	.long	1208137896
	.long	1073260066
	.long	1842943805
	.long	1010293654
	.long	3659916772
	.long	1073269218
	.long	3049005729
	.long	1015940862
	.long	1886261264
	.long	1073278269
	.long	4113039774
	.long	1016737581
	.long	3593647839
	.long	1073287220
	.long	557134306
	.long	1017388941
	.long	3086012205
	.long	1073296075
	.long	2731128411
	.long	1017337242
	.long	2769796922
	.long	1073304836
	.long	3165647905
	.long	1017152170
	.long	888716057
	.long	1073317807
	.long	2912601025
	.long	1013105161
	.long	2201465623
	.long	1073334794
	.long	1739805577
	.long	1014678056
	.long	164369365
	.long	1073351447
	.long	563195703
	.long	1016553181
	.long	3462666733
	.long	1073367780
	.long	1021961283
	.long	1014782652
	.long	2773905457
	.long	1073383810
	.long	2309706734
	.long	1017182395
	.long	1342879088
	.long	1073399550
	.long	3777265738
	.long	1016157963
	.long	2543933975
	.long	1073415012
	.long	3047211052
	.long	1016886343
	.long	1684477781
	.long	1073430209
	.long	3956587805
	.long	1016191198
	.long	3532178543
	.long	1073445151
	.long	3446811632
	.long	1016964431
	.long	1147747300
	.long	1073459850
	.long	1052894694
	.long	1009560267
	.long	1928031793
	.long	1073474314
	.long	2233937521
	.long	1015025921
	.long	2079717015
	.long	1073488553
	.long	1170546599
	.long	1017990098
	.long	4016765315
	.long	1073502575
	.long	1501504581
	.long	1015834847
	.long	3670431139
	.long	1073516389
	.long	276476277
	.long	1015832488
	.long	3549227225
	.long	1073530002
	.long	3492674696
	.long	1017962706
	.long	11637607
	.long	1073543422
	.long	2597602399
	.long	1017405654
	.long	588220169
	.long	1073556654
	.long	2120795824
	.long	1017959346
	.long	2635407503
	.long	1073569705
	.long	229478739
	.long	1017912059
	.long	2042029317
	.long	1073582582
	.long	2247123382
	.long	1013256945
	.long	1925128962
	.long	1073595290
	.long	2868813801
	.long	1017582195
	.long	4136375664
	.long	1073607834
	.long	2715618536
	.long	1016959120
	.long	759964600
	.long	1073620221
	.long	3782520422
	.long	1017711264
	.long	4257606771
	.long	1073632453
	.long	3973324070
	.long	1015361972
	.long	297278907
	.long	1073644538
	.long	1732402144
	.long	1017675273
	.long	3655053093
	.long	1073656477
	.long	1558073476
	.long	1015468392
	.long	2442253172
	.long	1073668277
	.long	2080372965
	.long	1017517768
	.long	1111876799
	.long	1073679941
	.long	350080266
	.long	1017397321
	.long	3330973139
	.long	1073691472
	.long	2701328568
	.long	1016749585
	.long	3438879452
	.long	1073702875
	.long	2694623891
	.long	1017925557
	.long	3671565478
	.long	1073714153
	.long	2048173000
	.long	1016319877
	.long	1317849547
	.long	1073725310
	.long	180890
	.long	1017408398
	.long	1642364115
	.long	1073736348
	.long	1694598884
	.long	1017060392
	.long	1553778919
	.long	3213899486
	.long	1553778919
	.long	3213899486
	.long	1553778919
	.long	3213899486
	.long	1553778919
	.long	3213899486
	.long	1553778919
	.long	3213899486
	.long	1553778919
	.long	3213899486
	.long	1553778919
	.long	3213899486
	.long	1553778919
	.long	3213899486
	.long	3582521621
	.long	1066628362
	.long	3582521621
	.long	1066628362
	.long	3582521621
	.long	1066628362
	.long	3582521621
	.long	1066628362
	.long	3582521621
	.long	1066628362
	.long	3582521621
	.long	1066628362
	.long	3582521621
	.long	1066628362
	.long	3582521621
	.long	1066628362
	.long	1646371399
	.long	3214412045
	.long	1646371399
	.long	3214412045
	.long	1646371399
	.long	3214412045
	.long	1646371399
	.long	3214412045
	.long	1646371399
	.long	3214412045
	.long	1646371399
	.long	3214412045
	.long	1646371399
	.long	3214412045
	.long	1646371399
	.long	3214412045
	.long	889629714
	.long	1067378449
	.long	889629714
	.long	1067378449
	.long	889629714
	.long	1067378449
	.long	889629714
	.long	1067378449
	.long	889629714
	.long	1067378449
	.long	889629714
	.long	1067378449
	.long	889629714
	.long	1067378449
	.long	889629714
	.long	1067378449
	.long	3534952507
	.long	3215266280
	.long	3534952507
	.long	3215266280
	.long	3534952507
	.long	3215266280
	.long	3534952507
	.long	3215266280
	.long	3534952507
	.long	3215266280
	.long	3534952507
	.long	3215266280
	.long	3534952507
	.long	3215266280
	.long	3534952507
	.long	3215266280
	.long	1007461464
	.long	1068473053
	.long	1007461464
	.long	1068473053
	.long	1007461464
	.long	1068473053
	.long	1007461464
	.long	1068473053
	.long	1007461464
	.long	1068473053
	.long	1007461464
	.long	1068473053
	.long	1007461464
	.long	1068473053
	.long	1007461464
	.long	1068473053
	.long	477218588
	.long	3216798151
	.long	477218588
	.long	3216798151
	.long	477218588
	.long	3216798151
	.long	477218588
	.long	3216798151
	.long	477218588
	.long	3216798151
	.long	477218588
	.long	3216798151
	.long	477218588
	.long	3216798151
	.long	477218588
	.long	3216798151
	.long	1431655765
	.long	1070945621
	.long	1431655765
	.long	1070945621
	.long	1431655765
	.long	1070945621
	.long	1431655765
	.long	1070945621
	.long	1431655765
	.long	1070945621
	.long	1431655765
	.long	1070945621
	.long	1431655765
	.long	1070945621
	.long	1431655765
	.long	1070945621
	.long	0
	.long	3220193280
	.long	0
	.long	3220193280
	.long	0
	.long	3220193280
	.long	0
	.long	3220193280
	.long	0
	.long	3220193280
	.long	0
	.long	3220193280
	.long	0
	.long	3220193280
	.long	0
	.long	3220193280
	.long	0
	.long	1032192
	.long	0
	.long	1032192
	.long	0
	.long	1032192
	.long	0
	.long	1032192
	.long	0
	.long	1032192
	.long	0
	.long	1032192
	.long	0
	.long	1032192
	.long	0
	.long	1032192
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
	.long	4293918720
	.long	0
	.long	4293918720
	.long	0
	.long	4293918720
	.long	0
	.long	4293918720
	.long	0
	.long	4293918720
	.long	0
	.long	4293918720
	.long	0
	.long	4293918720
	.long	0
	.long	4293918720
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
	.long	5462
	.long	0
	.long	5462
	.long	0
	.long	5462
	.long	0
	.long	5462
	.long	0
	.long	5462
	.long	0
	.long	5462
	.long	0
	.long	5462
	.long	0
	.long	5462
	.long	0
	.long	1015808
	.long	1015808
	.long	1015808
	.long	1015808
	.long	1015808
	.long	1015808
	.long	1015808
	.long	1015808
	.long	1015808
	.long	1015808
	.long	1015808
	.long	1015808
	.long	1015808
	.long	1015808
	.long	1015808
	.long	1015808
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
	.long	2048
	.long	2048
	.long	2048
	.long	2048
	.long	2048
	.long	2048
	.long	2048
	.long	2048
	.long	2048
	.long	2048
	.long	2048
	.long	2048
	.long	2048
	.long	2048
	.long	2048
	.long	2048
	.long	682
	.long	682
	.long	682
	.long	682
	.long	682
	.long	682
	.long	682
	.long	682
	.long	682
	.long	682
	.long	682
	.long	682
	.long	682
	.long	682
	.long	682
	.long	682
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	2148532224
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.long	4292870143
	.type	__svml_dcbrt_ha_data_internal,@object
	.size	__svml_dcbrt_ha_data_internal,3136
	.align 32
_vmldCbrtHATab:
	.long	0
	.long	1072693248
	.long	0
	.long	1072668672
	.long	0
	.long	1072644096
	.long	0
	.long	1072627712
	.long	0
	.long	1072611328
	.long	0
	.long	1072586752
	.long	0
	.long	1072570368
	.long	0
	.long	1072553984
	.long	0
	.long	1072537600
	.long	0
	.long	1072521216
	.long	0
	.long	1072504832
	.long	0
	.long	1072488448
	.long	0
	.long	1072480256
	.long	0
	.long	1072463872
	.long	0
	.long	1072447488
	.long	0
	.long	1072439296
	.long	0
	.long	1072422912
	.long	0
	.long	1072414720
	.long	0
	.long	1072398336
	.long	0
	.long	1072390144
	.long	0
	.long	1072373760
	.long	0
	.long	1072365568
	.long	0
	.long	1072357376
	.long	0
	.long	1072340992
	.long	0
	.long	1072332800
	.long	0
	.long	1072324608
	.long	0
	.long	1072308224
	.long	0
	.long	1072300032
	.long	0
	.long	1072291840
	.long	0
	.long	1072283648
	.long	0
	.long	1072275456
	.long	0
	.long	1072267264
	.long	1431655765
	.long	1071994197
	.long	1431655765
	.long	1015371093
	.long	1908874354
	.long	1071761180
	.long	1007461464
	.long	1071618781
	.long	565592401
	.long	1071446176
	.long	241555088
	.long	1071319599
	.long	943963244
	.long	1071221150
	.long	2330668378
	.long	1071141453
	.long	2770428108
	.long	1071075039
	.long	3622256836
	.long	1071018464
	.long	1497196870
	.long	1070969433
	.long	280472551
	.long	1070926345
	.long	1585032765
	.long	1070888044
	.long	0
	.long	1387266048
	.long	33554432
	.long	1101004800
	.long	512
	.long	1117782016
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	4160749568
	.long	1072965794
	.long	2921479643
	.long	1043912488
	.long	2684354560
	.long	1073309182
	.long	4060791142
	.long	1045755320
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	0
	.long	3220176896
	.type	_vmldCbrtHATab,@object
	.size	_vmldCbrtHATab,456
	.space 8, 0x00 	# pad
	.align 16
.L_2il0floatpacket.104:
	.long	0x00000000,0x80000000,0x00000000,0x00000000
	.type	.L_2il0floatpacket.104,@object
	.size	.L_2il0floatpacket.104,16
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
