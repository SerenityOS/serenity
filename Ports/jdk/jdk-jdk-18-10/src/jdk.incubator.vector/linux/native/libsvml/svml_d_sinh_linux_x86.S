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
	.file "svml_d_sinh.c"
	.text
..TXTST0:
.L_2__routine_start___svml_sinh1_ha_e9_0:
# -- Begin  __svml_sinh1_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinh1_ha_e9
# --- __svml_sinh1_ha_e9(__m128d)
__svml_sinh1_ha_e9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #420.1
        .byte     15                                            #527.546
        .byte     30                                            #527.546
        .byte     250                                           #527.546
	.cfi_startproc
..___tag_value___svml_sinh1_ha_e9.1:
..L2:
                                                          #420.1
        pushq     %rbp                                          #420.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #420.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #420.1
        subq      $128, %rsp                                    #420.1
        movaps    %xmm0, %xmm11                                 #420.1
        movsd     1408+__svml_dsinh_ha_data_internal(%rip), %xmm8 #465.16
        movl      $1082453555, %eax                             #471.27
        movaps    %xmm8, %xmm10                                 #466.17
        lea       __svml_dsinh_ha_data_internal(%rip), %rcx     #480.15
        andps     %xmm11, %xmm10                                #466.17
        movaps    %xmm10, %xmm7                                 #467.16
        pxor      %xmm11, %xmm7                                 #467.16
        movd      %eax, %xmm12                                  #471.27
        movaps    %xmm7, %xmm9                                  #468.25
        mulsd     1216+__svml_dsinh_ha_data_internal(%rip), %xmm9 #468.25
        movsd     512+__svml_dsinh_ha_data_internal(%rip), %xmm6 #455.20
        movsd     576+__svml_dsinh_ha_data_internal(%rip), %xmm0 #474.24
        addsd     %xmm6, %xmm9                                  #468.13
        pshufd    $85, %xmm7, %xmm13                            #470.22
        cmpeqsd   %xmm9, %xmm0                                  #474.24
        blendvpd  %xmm0, %xmm6, %xmm9                           #475.21
        pcmpgtd   %xmm12, %xmm13                                #471.27
        movq      704+__svml_dsinh_ha_data_internal(%rip), %xmm12 #477.23
        movaps    %xmm9, %xmm1                                  #484.13
        pand      %xmm9, %xmm12                                 #477.23
        subsd     %xmm6, %xmm1                                  #484.13
        movmskps  %xmm13, %eax                                  #472.44
        movdqa    %xmm12, %xmm14                                #479.27
        pxor      %xmm12, %xmm9                                 #478.19
        psllq     $3, %xmm14                                    #479.27
        psllq     $48, %xmm9                                    #490.19
        movd      %xmm14, %edx                                  #480.107
        movsd     1280+__svml_dsinh_ha_data_internal(%rip), %xmm15 #486.35
        movsd     1344+__svml_dsinh_ha_data_internal(%rip), %xmm6 #488.32
        movslq    %edx, %rdx                                    #480.107
        mulsd     %xmm1, %xmm15                                 #486.35
        mulsd     %xmm1, %xmm6                                  #488.32
        subsd     %xmm15, %xmm7                                 #486.13
        movsd     (%rdx,%rcx), %xmm3                            #480.15
        subsd     %xmm6, %xmm7                                  #488.13
        paddq     %xmm9, %xmm3                                  #492.19
        movsd     256(%rdx,%rcx), %xmm4                         #481.15
        movaps    %xmm3, %xmm14                                 #498.14
        movsd     384(%rdx,%rcx), %xmm2                         #483.19
        movaps    %xmm3, %xmm13                                 #497.14
        psubq     %xmm9, %xmm4                                  #495.19
        andnps    %xmm2, %xmm8                                  #502.22
        movaps    %xmm7, %xmm1                                  #489.14
        psubq     %xmm9, %xmm2                                  #506.23
        mulsd     %xmm7, %xmm1                                  #489.14
        subsd     %xmm4, %xmm14                                 #498.14
        cmpltsd   %xmm9, %xmm8                                  #504.24
        addsd     %xmm4, %xmm13                                 #497.14
        andnps    %xmm2, %xmm8                                  #508.23
        movaps    %xmm14, %xmm2                                 #509.22
        movsd     128(%rdx,%rcx), %xmm5                         #482.19
        subsd     %xmm3, %xmm2                                  #509.22
        mulsd     %xmm13, %xmm7                                 #520.15
        paddq     %xmm9, %xmm5                                  #500.23
        addsd     %xmm2, %xmm4                                  #510.22
        movsd     1088+__svml_dsinh_ha_data_internal(%rip), %xmm3 #513.29
        subsd     %xmm4, %xmm5                                  #511.26
        mulsd     %xmm1, %xmm3                                  #513.29
        subsd     %xmm8, %xmm5                                  #512.26
        addsd     960+__svml_dsinh_ha_data_internal(%rip), %xmm3 #513.17
        movsd     1152+__svml_dsinh_ha_data_internal(%rip), %xmm4 #514.31
        mulsd     %xmm1, %xmm4                                  #514.31
        mulsd     %xmm1, %xmm3                                  #515.29
        addsd     1024+__svml_dsinh_ha_data_internal(%rip), %xmm4 #514.19
        addsd     832+__svml_dsinh_ha_data_internal(%rip), %xmm3 #515.17
        mulsd     %xmm1, %xmm4                                  #516.31
        mulsd     %xmm1, %xmm3                                  #518.13
        addsd     896+__svml_dsinh_ha_data_internal(%rip), %xmm4 #516.19
        mulsd     %xmm7, %xmm3                                  #521.32
        mulsd     %xmm1, %xmm4                                  #517.31
        addsd     %xmm5, %xmm3                                  #521.20
        addsd     768+__svml_dsinh_ha_data_internal(%rip), %xmm4 #517.19
        mulsd     %xmm1, %xmm4                                  #519.15
        mulsd     %xmm14, %xmm4                                 #522.29
        addsd     %xmm3, %xmm4                                  #522.17
        addsd     %xmm4, %xmm7                                  #523.19
        addsd     %xmm14, %xmm7                                 #524.19
        orps      %xmm7, %xmm10                                 #525.14
        andl      $1, %eax                                      #472.98
        jne       ..B1.3        # Prob 5%                       #527.52
                                # LOE rbx r12 r13 r14 r15 eax xmm10 xmm11
..B1.2:                         # Preds ..B1.4 ..B1.1
                                # Execution count [1.00e+00]
        movaps    %xmm10, %xmm0                                 #530.12
        movq      %rbp, %rsp                                    #530.12
        popq      %rbp                                          #530.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #530.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm11, (%rsp)                                #527.160
        movsd     %xmm10, 64(%rsp)                              #527.233
        jne       ..B1.6        # Prob 5%                       #527.374
                                # LOE rbx r12 r13 r14 r15
..B1.4:                         # Preds ..B1.6 ..B1.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm10                              #527.626
        jmp       ..B1.2        # Prob 100%                     #527.626
                                # LOE rbx r12 r13 r14 r15 xmm10
..B1.6:                         # Preds ..B1.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #527.546
        lea       64(%rsp), %rsi                                #527.546
..___tag_value___svml_sinh1_ha_e9.10:
#       __svml_dsinh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsinh_ha_cout_rare_internal            #527.546
..___tag_value___svml_sinh1_ha_e9.11:
        jmp       ..B1.4        # Prob 100%                     #527.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_sinh1_ha_e9,@function
	.size	__svml_sinh1_ha_e9,.-__svml_sinh1_ha_e9
..LN__svml_sinh1_ha_e9.0:
	.data
# -- End  __svml_sinh1_ha_e9
	.text
.L_2__routine_start___svml_sinh4_ha_e9_1:
# -- Begin  __svml_sinh4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinh4_ha_e9
# --- __svml_sinh4_ha_e9(__m256d)
__svml_sinh4_ha_e9:
# parameter 1: %ymm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #535.1
        .byte     15                                            #642.552
        .byte     30                                            #642.552
        .byte     250                                           #642.552
	.cfi_startproc
..___tag_value___svml_sinh4_ha_e9.13:
..L14:
                                                         #535.1
        pushq     %rbp                                          #535.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #535.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #535.1
        subq      $192, %rsp                                    #535.1
        lea       __svml_dsinh_ha_data_internal(%rip), %r8      #595.78
        vmovupd   1408+__svml_dsinh_ha_data_internal(%rip), %ymm3 #580.50
        vmovupd   %ymm0, (%rsp)                                 #535.1[spill]
        vmovupd   512+__svml_dsinh_ha_data_internal(%rip), %ymm15 #570.54
        vmovupd   704+__svml_dsinh_ha_data_internal(%rip), %xmm11 #571.27
        vandpd    %ymm0, %ymm3, %ymm6                           #581.17
        vxorpd    %ymm0, %ymm6, %ymm0                           #582.16
        vmulpd    1216+__svml_dsinh_ha_data_internal(%rip), %ymm0, %ymm5 #583.28
        vmovupd   %ymm6, 32(%rsp)                               #581.17[spill]
        vaddpd    %ymm5, %ymm15, %ymm1                          #583.13
        vcmpeqpd  576+__svml_dsinh_ha_data_internal(%rip), %ymm1, %ymm12 #589.24
        vblendvpd %ymm12, %ymm15, %ymm1, %ymm2                  #590.21
        vpand     %xmm11, %xmm2, %xmm5                          #592.23
        vextractf128 $1, %ymm2, %xmm4                           #591.104
        vpsllq    $3, %xmm5, %xmm10                             #594.27
        vmovd     %xmm10, %edx                                  #595.170
        vandps    %xmm11, %xmm4, %xmm6                          #592.92
        vpsllq    $3, %xmm6, %xmm14                             #594.83
        vxorps    %xmm6, %xmm4, %xmm4                           #593.80
        vmovd     %xmm14, %esi                                  #595.458
        vpsllq    $48, %xmm4, %xmm4                             #605.72
        movslq    %esi, %rsi                                    #595.458
        vpextrd   $2, %xmm14, %edi                              #595.591
        movslq    %edi, %rdi                                    #595.591
        vpextrd   $2, %xmm10, %ecx                              #595.303
        vmovsd    256(%rsi,%r8), %xmm1                          #596.366
        movslq    %edx, %rdx                                    #595.170
        vmovhpd   256(%rdi,%r8), %xmm1, %xmm10                  #596.349
        vsubpd    %ymm15, %ymm2, %ymm1                          #599.13
        vpxor     %xmm5, %xmm2, %xmm2                           #593.19
        vmovsd    (%rsi,%r8), %xmm11                            #595.366
        vpsllq    $48, %xmm2, %xmm2                             #605.19
        vmovsd    128(%rdx,%r8), %xmm12                         #597.82
        vmulpd    1280+__svml_dsinh_ha_data_internal(%rip), %ymm1, %ymm15 #601.38
        vmulpd    1344+__svml_dsinh_ha_data_internal(%rip), %ymm1, %ymm1 #603.35
        vmovsd    128(%rsi,%r8), %xmm14                         #597.370
        vmovsd    (%rdx,%r8), %xmm7                             #595.78
        vpsubq    %xmm4, %xmm10, %xmm10                         #610.76
        movslq    %ecx, %rcx                                    #595.303
        vextractf128 $1, %ymm0, %xmm8                           #584.107
        vmovhpd   (%rcx,%r8), %xmm7, %xmm7                      #595.61
        vpaddq    %xmm2, %xmm7, %xmm7                           #607.19
        vshufps   $221, %xmm8, %xmm0, %xmm13                    #585.40
        vsubpd    %ymm15, %ymm0, %ymm0                          #601.13
        vpcmpgtd  640+__svml_dsinh_ha_data_internal(%rip), %xmm13, %xmm9 #586.27
        vmovhpd   (%rdi,%r8), %xmm11, %xmm8                     #595.349
        vsubpd    %ymm1, %ymm0, %ymm0                           #603.13
        vmovsd    256(%rdx,%r8), %xmm13                         #596.78
        vmovhpd   128(%rcx,%r8), %xmm12, %xmm11                 #597.65
        vmulpd    %ymm0, %ymm0, %ymm1                           #604.14
        vmovhpd   128(%rdi,%r8), %xmm14, %xmm12                 #597.353
        vmovsd    384(%rsi,%r8), %xmm14                         #598.370
        vmovmskps %xmm9, %eax                                   #587.44
        vmovhpd   256(%rcx,%r8), %xmm13, %xmm9                  #596.61
        vmovsd    384(%rdx,%r8), %xmm13                         #598.82
        vmovhpd   384(%rdi,%r8), %xmm14, %xmm14                 #598.353
        vpaddq    %xmm2, %xmm11, %xmm5                          #615.23
        vpaddq    %xmm4, %xmm12, %xmm12                         #615.80
        vpsubq    %xmm2, %xmm9, %xmm9                           #610.19
        vpaddq    %xmm4, %xmm8, %xmm8                           #607.76
        vmulpd    1152+__svml_dsinh_ha_data_internal(%rip), %ymm1, %ymm15 #629.34
        vmovhpd   384(%rcx,%r8), %xmm13, %xmm13                 #598.65
        vmovupd   %xmm14, 64(%rsp)                              #598.353[spill]
        vinsertf128 $1, %xmm14, %ymm13, %ymm14                  #598.19
        vinsertf128 $1, %xmm12, %ymm5, %ymm12                   #616.19
        vinsertf128 $1, %xmm4, %ymm2, %ymm5                     #618.17
        vpsubq    %xmm2, %xmm13, %xmm13                         #621.23
        vmovupd   64(%rsp), %xmm2                               #621.80[spill]
        vpsubq    %xmm4, %xmm2, %xmm4                           #621.80
        vandnpd   %ymm14, %ymm3, %ymm3                          #617.22
        vmulpd    1088+__svml_dsinh_ha_data_internal(%rip), %ymm1, %ymm14 #628.32
        vcmplt_oqpd %ymm5, %ymm3, %ymm3                         #619.24
        vaddpd    960+__svml_dsinh_ha_data_internal(%rip), %ymm14, %ymm2 #628.17
        vinsertf128 $1, %xmm4, %ymm13, %ymm6                    #622.19
        vaddpd    1024+__svml_dsinh_ha_data_internal(%rip), %ymm15, %ymm4 #629.19
        vmulpd    %ymm4, %ymm1, %ymm5                           #631.34
        vandnpd   %ymm6, %ymm3, %ymm11                          #623.23
        vmulpd    %ymm2, %ymm1, %ymm3                           #630.32
        vaddpd    896+__svml_dsinh_ha_data_internal(%rip), %ymm5, %ymm6 #631.19
        vinsertf128 $1, %xmm8, %ymm7, %ymm7                     #608.15
        vinsertf128 $1, %xmm10, %ymm9, %ymm10                   #611.15
        vsubpd    %ymm10, %ymm7, %ymm8                          #613.14
        vaddpd    %ymm10, %ymm7, %ymm9                          #612.14
        vsubpd    %ymm7, %ymm8, %ymm7                           #624.22
        vmulpd    %ymm9, %ymm0, %ymm2                           #635.15
        vaddpd    %ymm7, %ymm10, %ymm10                         #625.22
        vmulpd    %ymm6, %ymm1, %ymm7                           #632.34
        vsubpd    %ymm10, %ymm12, %ymm12                        #626.26
        vaddpd    832+__svml_dsinh_ha_data_internal(%rip), %ymm3, %ymm10 #630.17
        vsubpd    %ymm11, %ymm12, %ymm13                        #627.26
        vaddpd    768+__svml_dsinh_ha_data_internal(%rip), %ymm7, %ymm11 #632.19
        vmulpd    %ymm10, %ymm1, %ymm12                         #633.13
        vmulpd    %ymm11, %ymm1, %ymm1                          #634.15
        vmulpd    %ymm2, %ymm12, %ymm0                          #636.35
        vaddpd    %ymm0, %ymm13, %ymm9                          #636.20
        vmulpd    %ymm1, %ymm8, %ymm0                           #637.32
        vaddpd    %ymm0, %ymm9, %ymm1                           #637.17
        vaddpd    %ymm1, %ymm2, %ymm3                           #638.19
        vaddpd    %ymm3, %ymm8, %ymm8                           #639.19
        vorpd     32(%rsp), %ymm8, %ymm0                        #640.14[spill]
        testl     %eax, %eax                                    #642.52
        jne       ..B2.3        # Prob 5%                       #642.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B2.2:                         # Preds ..B2.3 ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #645.12
        popq      %rbp                                          #645.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #645.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   (%rsp), %ymm1                                 #642.200[spill]
        vmovupd   %ymm0, 128(%rsp)                              #642.276
        vmovupd   %ymm1, 64(%rsp)                               #642.200
        je        ..B2.2        # Prob 95%                      #642.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #642.460
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
        btl       %r12d, %r13d                                  #642.523
        jc        ..B2.10       # Prob 5%                       #642.523
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #642.476
        cmpl      $4, %r12d                                     #642.471
        jl        ..B2.7        # Prob 82%                      #642.471
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #642.683
        jmp       ..B2.2        # Prob 100%                     #642.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #642.552
        lea       128(%rsp,%r12,8), %rsi                        #642.552
..___tag_value___svml_sinh4_ha_e9.37:
#       __svml_dsinh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsinh_ha_cout_rare_internal            #642.552
..___tag_value___svml_sinh4_ha_e9.38:
        jmp       ..B2.8        # Prob 100%                     #642.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_sinh4_ha_e9,@function
	.size	__svml_sinh4_ha_e9,.-__svml_sinh4_ha_e9
..LN__svml_sinh4_ha_e9.1:
	.data
# -- End  __svml_sinh4_ha_e9
	.text
.L_2__routine_start___svml_sinh2_ha_ex_2:
# -- Begin  __svml_sinh2_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinh2_ha_ex
# --- __svml_sinh2_ha_ex(__m128d)
__svml_sinh2_ha_ex:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #956.1
        .byte     15                                            #1063.546
        .byte     30                                            #1063.546
        .byte     250                                           #1063.546
	.cfi_startproc
..___tag_value___svml_sinh2_ha_ex.40:
..L41:
                                                         #956.1
        pushq     %rbp                                          #956.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #956.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #956.1
        subq      $192, %rsp                                    #956.1
        movaps    %xmm0, %xmm11                                 #956.1
        movups    1408+__svml_dsinh_ha_data_internal(%rip), %xmm10 #1001.47
        lea       __svml_dsinh_ha_data_internal(%rip), %rsi     #1016.32
        movaps    %xmm10, %xmm0                                 #1002.17
        andps     %xmm11, %xmm0                                 #1002.17
        movaps    %xmm0, %xmm9                                  #1003.16
        movups    1216+__svml_dsinh_ha_data_internal(%rip), %xmm14 #1004.25
        pxor      %xmm11, %xmm9                                 #1003.16
        mulpd     %xmm9, %xmm14                                 #1004.25
        movups    512+__svml_dsinh_ha_data_internal(%rip), %xmm8 #991.51
        addpd     %xmm8, %xmm14                                 #1004.13
        movups    576+__svml_dsinh_ha_data_internal(%rip), %xmm15 #1010.24
        cmpeqpd   %xmm14, %xmm15                                #1010.24
        movaps    %xmm15, %xmm7                                 #1011.32
        andps     %xmm8, %xmm15                                 #1011.66
        andnps    %xmm14, %xmm7                                 #1011.32
        movq      640+__svml_dsinh_ha_data_internal(%rip), %xmm12 #993.23
        orps      %xmm15, %xmm7                                 #1011.21
        pshufd    $221, %xmm9, %xmm13                           #1006.16
        movdqu    704+__svml_dsinh_ha_data_internal(%rip), %xmm5 #1013.17
        pcmpgtd   %xmm12, %xmm13                                #1007.21
        movaps    %xmm7, %xmm12                                 #1020.13
        pand      %xmm7, %xmm5                                  #1013.17
        subpd     %xmm8, %xmm12                                 #1020.13
        movmskps  %xmm13, %edx                                  #1008.44
        movdqa    %xmm5, %xmm4                                  #1015.21
        pxor      %xmm5, %xmm7                                  #1014.13
        psllq     $3, %xmm4                                     #1015.21
        psllq     $48, %xmm7                                    #1026.13
        movd      %xmm4, %eax                                   #1016.124
        pshufd    $2, %xmm4, %xmm1                              #1016.267
        movups    1280+__svml_dsinh_ha_data_internal(%rip), %xmm8 #1022.35
        movd      %xmm1, %ecx                                   #1016.249
        movups    1344+__svml_dsinh_ha_data_internal(%rip), %xmm13 #1024.32
        mulpd     %xmm12, %xmm8                                 #1022.35
        mulpd     %xmm12, %xmm13                                #1024.32
        subpd     %xmm8, %xmm9                                  #1022.13
        movslq    %eax, %rax                                    #1016.124
        movslq    %ecx, %rcx                                    #1016.249
        subpd     %xmm13, %xmm9                                 #1024.13
        movsd     (%rax,%rsi), %xmm6                            #1016.32
        movaps    %xmm9, %xmm1                                  #1025.14
        movhpd    (%rcx,%rsi), %xmm6                            #1016.15
        movsd     256(%rax,%rsi), %xmm3                         #1017.32
        movsd     384(%rax,%rsi), %xmm2                         #1019.36
        movhpd    256(%rcx,%rsi), %xmm3                         #1017.15
        paddq     %xmm7, %xmm6                                  #1028.13
        psubq     %xmm7, %xmm3                                  #1031.13
        mulpd     %xmm9, %xmm1                                  #1025.14
        movhpd    384(%rcx,%rsi), %xmm2                         #1019.19
        movaps    %xmm6, %xmm14                                 #1034.14
        andnps    %xmm2, %xmm10                                 #1038.22
        movaps    %xmm6, %xmm5                                  #1033.14
        subpd     %xmm3, %xmm14                                 #1034.14
        cmpltpd   %xmm7, %xmm10                                 #1040.24
        addpd     %xmm3, %xmm5                                  #1033.14
        psubq     %xmm7, %xmm2                                  #1042.17
        mulpd     %xmm5, %xmm9                                  #1056.15
        andnps    %xmm2, %xmm10                                 #1044.23
        movaps    %xmm14, %xmm2                                 #1045.22
        subpd     %xmm6, %xmm2                                  #1045.22
        addpd     %xmm2, %xmm3                                  #1046.22
        movsd     128(%rax,%rsi), %xmm4                         #1018.36
        movhpd    128(%rcx,%rsi), %xmm4                         #1018.19
        paddq     %xmm7, %xmm4                                  #1036.17
        subpd     %xmm3, %xmm4                                  #1047.26
        movups    1152+__svml_dsinh_ha_data_internal(%rip), %xmm3 #1050.31
        mulpd     %xmm1, %xmm3                                  #1050.31
        subpd     %xmm10, %xmm4                                 #1048.26
        addpd     1024+__svml_dsinh_ha_data_internal(%rip), %xmm3 #1050.19
        movups    1088+__svml_dsinh_ha_data_internal(%rip), %xmm6 #1049.29
        mulpd     %xmm1, %xmm6                                  #1049.29
        mulpd     %xmm1, %xmm3                                  #1052.31
        addpd     960+__svml_dsinh_ha_data_internal(%rip), %xmm6 #1049.17
        addpd     896+__svml_dsinh_ha_data_internal(%rip), %xmm3 #1052.19
        mulpd     %xmm1, %xmm6                                  #1051.29
        mulpd     %xmm1, %xmm3                                  #1053.31
        addpd     832+__svml_dsinh_ha_data_internal(%rip), %xmm6 #1051.17
        addpd     768+__svml_dsinh_ha_data_internal(%rip), %xmm3 #1053.19
        mulpd     %xmm1, %xmm6                                  #1054.13
        mulpd     %xmm3, %xmm1                                  #1055.15
        mulpd     %xmm9, %xmm6                                  #1057.32
        mulpd     %xmm14, %xmm1                                 #1058.29
        addpd     %xmm6, %xmm4                                  #1057.20
        addpd     %xmm1, %xmm4                                  #1058.17
        addpd     %xmm4, %xmm9                                  #1059.19
        addpd     %xmm9, %xmm14                                 #1060.19
        orps      %xmm14, %xmm0                                 #1061.14
        andl      $3, %edx                                      #1008.98
        jne       ..B3.3        # Prob 5%                       #1063.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm11
..B3.2:                         # Preds ..B3.3 ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1066.12
        popq      %rbp                                          #1066.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1066.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm11, 64(%rsp)                              #1063.197
        movups    %xmm0, 128(%rsp)                              #1063.270
        je        ..B3.2        # Prob 95%                      #1063.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1063.454
        movq      %r12, 8(%rsp)                                 #1063.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1063.454
        movq      %r13, (%rsp)                                  #1063.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1063.454
                                # LOE rbx r12 r14 r15 r13d
..B3.7:                         # Preds ..B3.8 ..B3.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1063.517
        jc        ..B3.10       # Prob 5%                       #1063.517
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1063.470
        cmpl      $2, %r12d                                     #1063.465
        jl        ..B3.7        # Prob 82%                      #1063.465
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1063.674
        jmp       ..B3.2        # Prob 100%                     #1063.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1063.546
        lea       128(%rsp,%r12,8), %rsi                        #1063.546
..___tag_value___svml_sinh2_ha_ex.58:
#       __svml_dsinh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsinh_ha_cout_rare_internal            #1063.546
..___tag_value___svml_sinh2_ha_ex.59:
        jmp       ..B3.8        # Prob 100%                     #1063.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_sinh2_ha_ex,@function
	.size	__svml_sinh2_ha_ex,.-__svml_sinh2_ha_ex
..LN__svml_sinh2_ha_ex.2:
	.data
# -- End  __svml_sinh2_ha_ex
	.text
.L_2__routine_start___svml_sinh1_ha_l9_3:
# -- Begin  __svml_sinh1_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinh1_ha_l9
# --- __svml_sinh1_ha_l9(__m128d)
__svml_sinh1_ha_l9:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1071.1
        .byte     15                                            #1177.546
        .byte     30                                            #1177.546
        .byte     250                                           #1177.546
	.cfi_startproc
..___tag_value___svml_sinh1_ha_l9.61:
..L62:
                                                         #1071.1
        pushq     %rbp                                          #1071.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1071.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1071.1
        subq      $128, %rsp                                    #1071.1
        vmovapd   %xmm0, %xmm3                                  #1071.1
        vmovsd    1408+__svml_dsinh_ha_data_internal(%rip), %xmm4 #1116.16
        movl      $1082453555, %eax                             #1122.23
        vandpd    %xmm3, %xmm4, %xmm5                           #1117.17
        lea       __svml_dsinh_ha_data_internal(%rip), %rcx     #1131.15
        vxorpd    %xmm3, %xmm5, %xmm8                           #1118.16
        vmovsd    512+__svml_dsinh_ha_data_internal(%rip), %xmm15 #1106.20
        vmovapd   %xmm8, %xmm9                                  #1119.13
        vfmadd132sd 1216+__svml_dsinh_ha_data_internal(%rip), %xmm15, %xmm9 #1119.13
        vmovd     %eax, %xmm6                                   #1122.23
        vmovsd    576+__svml_dsinh_ha_data_internal(%rip), %xmm10 #1124.26
        vpshufd   $85, %xmm8, %xmm14                            #1121.18
        vpcmpgtd  %xmm6, %xmm14, %xmm7                          #1122.23
        vmovq     704+__svml_dsinh_ha_data_internal(%rip), %xmm2 #1107.23
        vmovmskps %xmm7, %eax                                   #1123.44
        vcmpeqsd  %xmm9, %xmm10, %xmm0                          #1125.24
        blendvpd  %xmm0, %xmm15, %xmm9                          #1126.21
        vpand     %xmm2, %xmm9, %xmm7                           #1128.19
        vsubsd    %xmm15, %xmm9, %xmm14                         #1135.13
        vpsllq    $3, %xmm7, %xmm11                             #1130.23
        vmovsd    1280+__svml_dsinh_ha_data_internal(%rip), %xmm6 #1136.19
        vmovd     %xmm11, %edx                                  #1131.107
        vmovsd    1344+__svml_dsinh_ha_data_internal(%rip), %xmm15 #1139.13
        vfnmadd213sd %xmm8, %xmm14, %xmm6                       #1137.13
        vpxor     %xmm7, %xmm9, %xmm8                           #1129.15
        vpsllq    $48, %xmm8, %xmm11                            #1141.15
        movslq    %edx, %rdx                                    #1131.107
        vfnmadd213sd %xmm6, %xmm14, %xmm15                      #1139.13
        vmovsd    (%rdx,%rcx), %xmm12                           #1131.15
        vmovsd    256(%rdx,%rcx), %xmm13                        #1132.15
        vpaddq    %xmm11, %xmm12, %xmm10                        #1143.15
        vpsubq    %xmm11, %xmm13, %xmm7                         #1146.15
        vmulsd    %xmm15, %xmm15, %xmm14                        #1140.14
        vsubsd    %xmm7, %xmm10, %xmm9                          #1149.14
        vaddsd    %xmm7, %xmm10, %xmm8                          #1148.14
        vsubsd    %xmm10, %xmm9, %xmm13                         #1160.22
        vmovsd    128(%rdx,%rcx), %xmm1                         #1133.19
        vaddsd    %xmm13, %xmm7, %xmm7                          #1161.22
        vpaddq    %xmm11, %xmm1, %xmm6                          #1151.19
        vmovsd    384(%rdx,%rcx), %xmm2                         #1134.19
        vsubsd    %xmm7, %xmm6, %xmm6                           #1162.26
        vpsubq    %xmm11, %xmm2, %xmm12                         #1157.19
        vandnpd   %xmm2, %xmm4, %xmm1                           #1153.22
        vmovsd    1152+__svml_dsinh_ha_data_internal(%rip), %xmm2 #1165.19
        vcmpltsd  %xmm11, %xmm1, %xmm4                          #1155.24
        vfmadd213sd 1024+__svml_dsinh_ha_data_internal(%rip), %xmm14, %xmm2 #1165.19
        vmovsd    1088+__svml_dsinh_ha_data_internal(%rip), %xmm1 #1164.17
        vandnpd   %xmm12, %xmm4, %xmm0                          #1159.23
        vfmadd213sd 960+__svml_dsinh_ha_data_internal(%rip), %xmm14, %xmm1 #1164.17
        vsubsd    %xmm0, %xmm6, %xmm4                           #1163.26
        vfmadd213sd 896+__svml_dsinh_ha_data_internal(%rip), %xmm14, %xmm2 #1167.19
        vfmadd213sd 832+__svml_dsinh_ha_data_internal(%rip), %xmm14, %xmm1 #1166.17
        vfmadd213sd 768+__svml_dsinh_ha_data_internal(%rip), %xmm14, %xmm2 #1168.19
        vmulsd    %xmm14, %xmm1, %xmm10                         #1169.13
        vmulsd    %xmm14, %xmm2, %xmm14                         #1170.15
        vfmadd213sd %xmm15, %xmm15, %xmm10                      #1171.13
        vfmadd213sd %xmm4, %xmm9, %xmm14                        #1172.19
        vfmadd213sd %xmm14, %xmm8, %xmm10                       #1173.19
        vaddsd    %xmm9, %xmm10, %xmm8                          #1174.19
        vorpd     %xmm8, %xmm5, %xmm0                           #1175.14
        andl      $1, %eax                                      #1123.98
        jne       ..B4.3        # Prob 5%                       #1177.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm3
..B4.2:                         # Preds ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1180.12
        popq      %rbp                                          #1180.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1180.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm3, (%rsp)                                 #1177.160
        vmovsd    %xmm0, 64(%rsp)                               #1177.233
        jne       ..B4.5        # Prob 5%                       #1177.374
                                # LOE rbx r12 r13 r14 r15 eax
..B4.4:                         # Preds ..B4.6 ..B4.5 ..B4.3
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm0                               #1177.626
        movq      %rbp, %rsp                                    #1177.626
        popq      %rbp                                          #1177.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1177.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.5:                         # Preds ..B4.3
                                # Execution count [2.50e-03]: Infreq
        je        ..B4.4        # Prob 95%                      #1177.517
                                # LOE rbx r12 r13 r14 r15
..B4.6:                         # Preds ..B4.5
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1177.546
        lea       64(%rsp), %rsi                                #1177.546
..___tag_value___svml_sinh1_ha_l9.74:
#       __svml_dsinh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsinh_ha_cout_rare_internal            #1177.546
..___tag_value___svml_sinh1_ha_l9.75:
        jmp       ..B4.4        # Prob 100%                     #1177.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_sinh1_ha_l9,@function
	.size	__svml_sinh1_ha_l9,.-__svml_sinh1_ha_l9
..LN__svml_sinh1_ha_l9.3:
	.data
# -- End  __svml_sinh1_ha_l9
	.text
.L_2__routine_start___svml_sinh2_ha_l9_4:
# -- Begin  __svml_sinh2_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinh2_ha_l9
# --- __svml_sinh2_ha_l9(__m128d)
__svml_sinh2_ha_l9:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1185.1
        .byte     15                                            #1291.546
        .byte     30                                            #1291.546
        .byte     250                                           #1291.546
	.cfi_startproc
..___tag_value___svml_sinh2_ha_l9.77:
..L78:
                                                         #1185.1
        pushq     %rbp                                          #1185.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1185.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1185.1
        subq      $192, %rsp                                    #1185.1
        vmovapd   %xmm0, %xmm6                                  #1185.1
        vmovupd   1408+__svml_dsinh_ha_data_internal(%rip), %xmm4 #1230.47
        lea       __svml_dsinh_ha_data_internal(%rip), %rsi     #1245.32
        vandpd    %xmm6, %xmm4, %xmm5                           #1231.17
        vmovupd   512+__svml_dsinh_ha_data_internal(%rip), %xmm11 #1220.51
        vxorpd    %xmm6, %xmm5, %xmm7                           #1232.16
        vmovupd   1216+__svml_dsinh_ha_data_internal(%rip), %xmm8 #1233.13
        vfmadd213pd %xmm11, %xmm7, %xmm8                        #1233.13
        vmovq     640+__svml_dsinh_ha_data_internal(%rip), %xmm9 #1222.25
        vpshufd   $221, %xmm7, %xmm2                            #1235.18
        vpcmpgtd  %xmm9, %xmm2, %xmm12                          #1236.23
        vmovmskps %xmm12, %edx                                  #1237.44
        vcmpeqpd  576+__svml_dsinh_ha_data_internal(%rip), %xmm8, %xmm0 #1239.24
        blendvpd  %xmm0, %xmm11, %xmm8                          #1240.21
        vsubpd    %xmm11, %xmm8, %xmm9                          #1249.13
        vpand     704+__svml_dsinh_ha_data_internal(%rip), %xmm8, %xmm10 #1242.19
        vmovupd   1280+__svml_dsinh_ha_data_internal(%rip), %xmm11 #1250.50
        vpsllq    $3, %xmm10, %xmm3                             #1244.23
        vfnmadd231pd %xmm11, %xmm9, %xmm7                       #1251.13
        vpxor     %xmm10, %xmm8, %xmm10                         #1243.15
        vmovd     %xmm3, %eax                                   #1245.124
        vfnmadd231pd 1344+__svml_dsinh_ha_data_internal(%rip), %xmm9, %xmm7 #1253.13
        movslq    %eax, %rax                                    #1245.124
        vpextrd   $2, %xmm3, %ecx                               #1245.251
        movslq    %ecx, %rcx                                    #1245.251
        vmovsd    (%rax,%rsi), %xmm13                           #1245.32
        vmovsd    256(%rax,%rsi), %xmm14                        #1246.32
        vmovsd    384(%rax,%rsi), %xmm2                         #1248.36
        vmovhpd   (%rcx,%rsi), %xmm13, %xmm15                   #1245.15
        vpsllq    $48, %xmm10, %xmm13                           #1255.15
        vmovhpd   256(%rcx,%rsi), %xmm14, %xmm14                #1246.15
        vmovhpd   384(%rcx,%rsi), %xmm2, %xmm3                  #1248.19
        vmulpd    %xmm7, %xmm7, %xmm2                           #1254.14
        vpaddq    %xmm13, %xmm15, %xmm12                        #1257.15
        vpsubq    %xmm13, %xmm14, %xmm9                         #1260.15
        vpsubq    %xmm13, %xmm3, %xmm0                          #1271.19
        vsubpd    %xmm9, %xmm12, %xmm11                         #1263.14
        vaddpd    %xmm9, %xmm12, %xmm10                         #1262.14
        vsubpd    %xmm12, %xmm11, %xmm12                        #1274.22
        vmovsd    128(%rax,%rsi), %xmm1                         #1247.36
        vandnpd   %xmm3, %xmm4, %xmm4                           #1267.22
        vmovhpd   128(%rcx,%rsi), %xmm1, %xmm1                  #1247.19
        vpaddq    %xmm13, %xmm1, %xmm8                          #1265.19
        vcmpltpd  %xmm13, %xmm4, %xmm15                         #1269.24
        vaddpd    %xmm12, %xmm9, %xmm9                          #1275.22
        vmovupd   1152+__svml_dsinh_ha_data_internal(%rip), %xmm13 #1279.19
        vandnpd   %xmm0, %xmm15, %xmm1                          #1273.23
        vfmadd213pd 1024+__svml_dsinh_ha_data_internal(%rip), %xmm2, %xmm13 #1279.19
        vmovupd   1088+__svml_dsinh_ha_data_internal(%rip), %xmm3 #1278.17
        vfmadd213pd 960+__svml_dsinh_ha_data_internal(%rip), %xmm2, %xmm3 #1278.17
        vsubpd    %xmm9, %xmm8, %xmm8                           #1276.26
        vfmadd213pd 896+__svml_dsinh_ha_data_internal(%rip), %xmm2, %xmm13 #1281.19
        vfmadd213pd 832+__svml_dsinh_ha_data_internal(%rip), %xmm2, %xmm3 #1280.17
        vsubpd    %xmm1, %xmm8, %xmm4                           #1277.26
        vfmadd213pd 768+__svml_dsinh_ha_data_internal(%rip), %xmm2, %xmm13 #1282.19
        vmulpd    %xmm3, %xmm2, %xmm14                          #1283.13
        vmulpd    %xmm13, %xmm2, %xmm2                          #1284.15
        vfmadd213pd %xmm7, %xmm7, %xmm14                        #1285.13
        vfmadd213pd %xmm4, %xmm11, %xmm2                        #1286.19
        vfmadd213pd %xmm2, %xmm10, %xmm14                       #1287.19
        vaddpd    %xmm14, %xmm11, %xmm7                         #1288.19
        vorpd     %xmm7, %xmm5, %xmm0                           #1289.14
        andl      $3, %edx                                      #1237.98
        jne       ..B5.3        # Prob 5%                       #1291.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm6
..B5.2:                         # Preds ..B5.3 ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1294.12
        popq      %rbp                                          #1294.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1294.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm6, 64(%rsp)                               #1291.197
        vmovupd   %xmm0, 128(%rsp)                              #1291.270
        je        ..B5.2        # Prob 95%                      #1291.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1291.454
        movq      %r12, 8(%rsp)                                 #1291.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1291.454
        movq      %r13, (%rsp)                                  #1291.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1291.454
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1291.517
        jc        ..B5.10       # Prob 5%                       #1291.517
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1291.470
        cmpl      $2, %r12d                                     #1291.465
        jl        ..B5.7        # Prob 82%                      #1291.465
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1291.674
        jmp       ..B5.2        # Prob 100%                     #1291.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1291.546
        lea       128(%rsp,%r12,8), %rsi                        #1291.546
..___tag_value___svml_sinh2_ha_l9.95:
#       __svml_dsinh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsinh_ha_cout_rare_internal            #1291.546
..___tag_value___svml_sinh2_ha_l9.96:
        jmp       ..B5.8        # Prob 100%                     #1291.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_sinh2_ha_l9,@function
	.size	__svml_sinh2_ha_l9,.-__svml_sinh2_ha_l9
..LN__svml_sinh2_ha_l9.4:
	.data
# -- End  __svml_sinh2_ha_l9
	.text
.L_2__routine_start___svml_sinh1_ha_ex_5:
# -- Begin  __svml_sinh1_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinh1_ha_ex
# --- __svml_sinh1_ha_ex(__m128d)
__svml_sinh1_ha_ex:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1299.1
        .byte     15                                            #1406.546
        .byte     30                                            #1406.546
        .byte     250                                           #1406.546
	.cfi_startproc
..___tag_value___svml_sinh1_ha_ex.98:
..L99:
                                                         #1299.1
        pushq     %rbp                                          #1299.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1299.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1299.1
        subq      $128, %rsp                                    #1299.1
        movaps    %xmm0, %xmm10                                 #1299.1
        movsd     1408+__svml_dsinh_ha_data_internal(%rip), %xmm9 #1344.16
        movl      $1082453555, %eax                             #1350.21
        movaps    %xmm9, %xmm0                                  #1345.17
        lea       __svml_dsinh_ha_data_internal(%rip), %rcx     #1359.15
        andps     %xmm10, %xmm0                                 #1345.17
        movaps    %xmm0, %xmm8                                  #1346.16
        pxor      %xmm10, %xmm8                                 #1346.16
        movd      %eax, %xmm11                                  #1350.21
        movaps    %xmm8, %xmm13                                 #1347.25
        mulsd     1216+__svml_dsinh_ha_data_internal(%rip), %xmm13 #1347.25
        movsd     512+__svml_dsinh_ha_data_internal(%rip), %xmm6 #1334.20
        movsd     576+__svml_dsinh_ha_data_internal(%rip), %xmm14 #1353.24
        addsd     %xmm6, %xmm13                                 #1347.13
        pshufd    $85, %xmm8, %xmm12                            #1349.16
        cmpeqsd   %xmm13, %xmm14                                #1353.24
        movaps    %xmm14, %xmm7                                 #1354.32
        pcmpgtd   %xmm11, %xmm12                                #1350.21
        andnps    %xmm13, %xmm7                                 #1354.32
        andps     %xmm6, %xmm14                                 #1354.66
        movmskps  %xmm12, %eax                                  #1351.44
        orps      %xmm14, %xmm7                                 #1354.21
        movq      704+__svml_dsinh_ha_data_internal(%rip), %xmm12 #1356.17
        movaps    %xmm7, %xmm11                                 #1363.13
        pand      %xmm7, %xmm12                                 #1356.17
        subsd     %xmm6, %xmm11                                 #1363.13
        movdqa    %xmm12, %xmm15                                #1358.21
        pxor      %xmm12, %xmm7                                 #1357.13
        psllq     $3, %xmm15                                    #1358.21
        psllq     $48, %xmm7                                    #1369.13
        movd      %xmm15, %edx                                  #1359.107
        movsd     1280+__svml_dsinh_ha_data_internal(%rip), %xmm1 #1365.35
        movsd     1344+__svml_dsinh_ha_data_internal(%rip), %xmm6 #1367.32
        movslq    %edx, %rdx                                    #1359.107
        mulsd     %xmm11, %xmm1                                 #1365.35
        mulsd     %xmm11, %xmm6                                 #1367.32
        subsd     %xmm1, %xmm8                                  #1365.13
        movsd     (%rdx,%rcx), %xmm3                            #1359.15
        subsd     %xmm6, %xmm8                                  #1367.13
        paddq     %xmm7, %xmm3                                  #1371.13
        movsd     256(%rdx,%rcx), %xmm4                         #1360.15
        movaps    %xmm3, %xmm14                                 #1377.14
        movsd     384(%rdx,%rcx), %xmm2                         #1362.19
        movaps    %xmm3, %xmm13                                 #1376.14
        psubq     %xmm7, %xmm4                                  #1374.13
        andnps    %xmm2, %xmm9                                  #1381.22
        movaps    %xmm8, %xmm1                                  #1368.14
        psubq     %xmm7, %xmm2                                  #1385.17
        mulsd     %xmm8, %xmm1                                  #1368.14
        subsd     %xmm4, %xmm14                                 #1377.14
        cmpltsd   %xmm7, %xmm9                                  #1383.24
        addsd     %xmm4, %xmm13                                 #1376.14
        andnps    %xmm2, %xmm9                                  #1387.23
        movaps    %xmm14, %xmm2                                 #1388.22
        movsd     128(%rdx,%rcx), %xmm5                         #1361.19
        subsd     %xmm3, %xmm2                                  #1388.22
        mulsd     %xmm13, %xmm8                                 #1399.15
        paddq     %xmm7, %xmm5                                  #1379.17
        addsd     %xmm2, %xmm4                                  #1389.22
        movsd     1088+__svml_dsinh_ha_data_internal(%rip), %xmm3 #1392.29
        subsd     %xmm4, %xmm5                                  #1390.26
        mulsd     %xmm1, %xmm3                                  #1392.29
        subsd     %xmm9, %xmm5                                  #1391.26
        addsd     960+__svml_dsinh_ha_data_internal(%rip), %xmm3 #1392.17
        movsd     1152+__svml_dsinh_ha_data_internal(%rip), %xmm4 #1393.31
        mulsd     %xmm1, %xmm4                                  #1393.31
        mulsd     %xmm1, %xmm3                                  #1394.29
        addsd     1024+__svml_dsinh_ha_data_internal(%rip), %xmm4 #1393.19
        addsd     832+__svml_dsinh_ha_data_internal(%rip), %xmm3 #1394.17
        mulsd     %xmm1, %xmm4                                  #1395.31
        mulsd     %xmm1, %xmm3                                  #1397.13
        addsd     896+__svml_dsinh_ha_data_internal(%rip), %xmm4 #1395.19
        mulsd     %xmm8, %xmm3                                  #1400.32
        mulsd     %xmm1, %xmm4                                  #1396.31
        addsd     %xmm5, %xmm3                                  #1400.20
        addsd     768+__svml_dsinh_ha_data_internal(%rip), %xmm4 #1396.19
        mulsd     %xmm1, %xmm4                                  #1398.15
        mulsd     %xmm14, %xmm4                                 #1401.29
        addsd     %xmm3, %xmm4                                  #1401.17
        addsd     %xmm4, %xmm8                                  #1402.19
        addsd     %xmm14, %xmm8                                 #1403.19
        orps      %xmm8, %xmm0                                  #1404.14
        andl      $1, %eax                                      #1351.98
        jne       ..B6.3        # Prob 5%                       #1406.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm10
..B6.2:                         # Preds ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1409.12
        popq      %rbp                                          #1409.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1409.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm10, (%rsp)                                #1406.160
        movsd     %xmm0, 64(%rsp)                               #1406.233
        jne       ..B6.6        # Prob 5%                       #1406.374
                                # LOE rbx r12 r13 r14 r15
..B6.4:                         # Preds ..B6.6 ..B6.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #1406.626
        movq      %rbp, %rsp                                    #1406.626
        popq      %rbp                                          #1406.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1406.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B6.6:                         # Preds ..B6.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1406.546
        lea       64(%rsp), %rsi                                #1406.546
..___tag_value___svml_sinh1_ha_ex.111:
#       __svml_dsinh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsinh_ha_cout_rare_internal            #1406.546
..___tag_value___svml_sinh1_ha_ex.112:
        jmp       ..B6.4        # Prob 100%                     #1406.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_sinh1_ha_ex,@function
	.size	__svml_sinh1_ha_ex,.-__svml_sinh1_ha_ex
..LN__svml_sinh1_ha_ex.5:
	.data
# -- End  __svml_sinh1_ha_ex
	.text
.L_2__routine_start___svml_sinh4_ha_l9_6:
# -- Begin  __svml_sinh4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinh4_ha_l9
# --- __svml_sinh4_ha_l9(__m256d)
__svml_sinh4_ha_l9:
# parameter 1: %ymm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1414.1
        .byte     15                                            #1520.552
        .byte     30                                            #1520.552
        .byte     250                                           #1520.552
	.cfi_startproc
..___tag_value___svml_sinh4_ha_l9.114:
..L115:
                                                        #1414.1
        pushq     %rbp                                          #1414.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1414.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1414.1
        subq      $192, %rsp                                    #1414.1
        lea       __svml_dsinh_ha_data_internal(%rip), %r8      #1474.78
        vmovupd   1408+__svml_dsinh_ha_data_internal(%rip), %ymm1 #1459.50
        vmovupd   512+__svml_dsinh_ha_data_internal(%rip), %ymm10 #1449.54
        vmovupd   1216+__svml_dsinh_ha_data_internal(%rip), %ymm11 #1462.13
        vmovapd   %ymm0, %ymm3                                  #1414.1
        vandpd    %ymm3, %ymm1, %ymm2                           #1460.17
        vxorpd    %ymm3, %ymm2, %ymm5                           #1461.16
        vfmadd213pd %ymm10, %ymm5, %ymm11                       #1462.13
        vcmpeqpd  576+__svml_dsinh_ha_data_internal(%rip), %ymm11, %ymm4 #1468.24
        vblendvpd %ymm4, %ymm10, %ymm11, %ymm9                  #1469.21
        vandps    704+__svml_dsinh_ha_data_internal(%rip), %ymm9, %ymm15 #1471.19
        vpsllq    $3, %ymm15, %ymm12                            #1473.23
        vxorps    %ymm15, %ymm9, %ymm15                         #1472.15
        vextractf128 $1, %ymm5, %xmm6                           #1464.125
        vshufps   $221, %xmm6, %xmm5, %xmm7                     #1464.36
        vpcmpgtd  640+__svml_dsinh_ha_data_internal(%rip), %xmm7, %xmm8 #1465.23
        vmovmskps %xmm8, %eax                                   #1466.44
        vextractf128 $1, %ymm12, %xmm6                          #1474.521
        vmovd     %xmm12, %edx                                  #1474.170
        vmovd     %xmm6, %esi                                   #1474.502
        movslq    %edx, %rdx                                    #1474.170
        vpextrd   $2, %xmm12, %ecx                              #1474.325
        movslq    %esi, %rsi                                    #1474.502
        vpextrd   $2, %xmm6, %edi                               #1474.662
        movslq    %ecx, %rcx                                    #1474.325
        movslq    %edi, %rdi                                    #1474.662
        vmovsd    (%rdx,%r8), %xmm14                            #1474.78
        vmovsd    256(%rdx,%r8), %xmm11                         #1475.78
        vmovsd    (%rsi,%r8), %xmm7                             #1474.410
        vmovsd    256(%rsi,%r8), %xmm0                          #1475.410
        vmovhpd   (%rcx,%r8), %xmm14, %xmm13                    #1474.61
        vmovhpd   256(%rcx,%r8), %xmm11, %xmm4                  #1475.61
        vmovhpd   (%rdi,%r8), %xmm7, %xmm8                      #1474.393
        vmovhpd   256(%rdi,%r8), %xmm0, %xmm6                   #1475.393
        vmovsd    128(%rdx,%r8), %xmm7                          #1476.82
        vmovsd    128(%rsi,%r8), %xmm11                         #1476.414
        vmovhpd   128(%rdi,%r8), %xmm11, %xmm12                 #1476.397
        vinsertf128 $1, %xmm8, %ymm13, %ymm14                   #1474.15
        vinsertf128 $1, %xmm6, %ymm4, %ymm13                    #1475.15
        vmovsd    384(%rdx,%r8), %xmm6                          #1477.82
        vmovhpd   128(%rcx,%r8), %xmm7, %xmm8                   #1476.65
        vmovhpd   384(%rcx,%r8), %xmm6, %xmm7                   #1477.65
        vsubpd    %ymm10, %ymm9, %ymm6                          #1478.13
        vmovupd   1280+__svml_dsinh_ha_data_internal(%rip), %ymm10 #1479.53
        vfnmadd231pd %ymm10, %ymm6, %ymm5                       #1480.13
        vfnmadd231pd 1344+__svml_dsinh_ha_data_internal(%rip), %ymm6, %ymm5 #1482.13
        vmulpd    %ymm5, %ymm5, %ymm6                           #1483.14
        vinsertf128 $1, %xmm12, %ymm8, %ymm0                    #1476.19
        vpsllq    $48, %ymm15, %ymm12                           #1484.15
        vmovsd    384(%rsi,%r8), %xmm8                          #1477.414
        vmovhpd   384(%rdi,%r8), %xmm8, %xmm11                  #1477.397
        vpsubq    %ymm12, %ymm13, %ymm8                         #1489.15
        vmovupd   1152+__svml_dsinh_ha_data_internal(%rip), %ymm13 #1508.19
        vfmadd213pd 1024+__svml_dsinh_ha_data_internal(%rip), %ymm6, %ymm13 #1508.19
        vfmadd213pd 896+__svml_dsinh_ha_data_internal(%rip), %ymm6, %ymm13 #1510.19
        vfmadd213pd 768+__svml_dsinh_ha_data_internal(%rip), %ymm6, %ymm13 #1511.19
        vinsertf128 $1, %xmm11, %ymm7, %ymm4                    #1477.19
        vandnpd   %ymm4, %ymm1, %ymm1                           #1496.22
        vpaddq    %ymm12, %ymm14, %ymm11                        #1486.15
        vpaddq    %ymm12, %ymm0, %ymm7                          #1494.19
        vcmplt_oqpd %ymm12, %ymm1, %ymm0                        #1498.24
        vsubpd    %ymm8, %ymm11, %ymm10                         #1492.14
        vaddpd    %ymm8, %ymm11, %ymm9                          #1491.14
        vpsubq    %ymm12, %ymm4, %ymm1                          #1500.19
        vmovupd   1088+__svml_dsinh_ha_data_internal(%rip), %ymm12 #1507.17
        vsubpd    %ymm11, %ymm10, %ymm11                        #1503.22
        vfmadd213pd 960+__svml_dsinh_ha_data_internal(%rip), %ymm6, %ymm12 #1507.17
        vaddpd    %ymm11, %ymm8, %ymm8                          #1504.22
        vfmadd213pd 832+__svml_dsinh_ha_data_internal(%rip), %ymm6, %ymm12 #1509.17
        vsubpd    %ymm8, %ymm7, %ymm7                           #1505.26
        vmulpd    %ymm12, %ymm6, %ymm15                         #1512.13
        vmulpd    %ymm13, %ymm6, %ymm6                          #1513.15
        vfmadd213pd %ymm5, %ymm5, %ymm15                        #1514.13
        vandnpd   %ymm1, %ymm0, %ymm4                           #1502.23
        vsubpd    %ymm4, %ymm7, %ymm14                          #1506.26
        vfmadd213pd %ymm14, %ymm10, %ymm6                       #1515.19
        vfmadd213pd %ymm6, %ymm9, %ymm15                        #1516.19
        vaddpd    %ymm15, %ymm10, %ymm5                         #1517.19
        vorpd     %ymm5, %ymm2, %ymm0                           #1518.14
        testl     %eax, %eax                                    #1520.52
        jne       ..B7.3        # Prob 5%                       #1520.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm3
..B7.2:                         # Preds ..B7.3 ..B7.9 ..B7.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1523.12
        popq      %rbp                                          #1523.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1523.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.3:                         # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm3, 64(%rsp)                               #1520.200
        vmovupd   %ymm0, 128(%rsp)                              #1520.276
        je        ..B7.2        # Prob 95%                      #1520.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B7.6:                         # Preds ..B7.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1520.460
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
        btl       %r12d, %r13d                                  #1520.523
        jc        ..B7.10       # Prob 5%                       #1520.523
                                # LOE rbx r12 r14 r15 r13d
..B7.8:                         # Preds ..B7.10 ..B7.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1520.476
        cmpl      $4, %r12d                                     #1520.471
        jl        ..B7.7        # Prob 82%                      #1520.471
                                # LOE rbx r12 r14 r15 r13d
..B7.9:                         # Preds ..B7.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1520.683
        jmp       ..B7.2        # Prob 100%                     #1520.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B7.10:                        # Preds ..B7.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1520.552
        lea       128(%rsp,%r12,8), %rsi                        #1520.552
..___tag_value___svml_sinh4_ha_l9.132:
#       __svml_dsinh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsinh_ha_cout_rare_internal            #1520.552
..___tag_value___svml_sinh4_ha_l9.133:
        jmp       ..B7.8        # Prob 100%                     #1520.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_sinh4_ha_l9,@function
	.size	__svml_sinh4_ha_l9,.-__svml_sinh4_ha_l9
..LN__svml_sinh4_ha_l9.6:
	.data
# -- End  __svml_sinh4_ha_l9
	.text
.L_2__routine_start___svml_sinh2_ha_e9_7:
# -- Begin  __svml_sinh2_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinh2_ha_e9
# --- __svml_sinh2_ha_e9(__m128d)
__svml_sinh2_ha_e9:
# parameter 1: %xmm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1528.1
        .byte     15                                            #1635.546
        .byte     30                                            #1635.546
        .byte     250                                           #1635.546
	.cfi_startproc
..___tag_value___svml_sinh2_ha_e9.135:
..L136:
                                                        #1528.1
        pushq     %rbp                                          #1528.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1528.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1528.1
        subq      $192, %rsp                                    #1528.1
        movaps    %xmm0, %xmm11                                 #1528.1
        movups    1408+__svml_dsinh_ha_data_internal(%rip), %xmm8 #1573.47
        lea       __svml_dsinh_ha_data_internal(%rip), %rsi     #1588.32
        movaps    %xmm8, %xmm10                                 #1574.17
        andps     %xmm11, %xmm10                                #1574.17
        movaps    %xmm10, %xmm7                                 #1575.16
        movups    1216+__svml_dsinh_ha_data_internal(%rip), %xmm9 #1576.25
        pxor      %xmm11, %xmm7                                 #1575.16
        mulpd     %xmm7, %xmm9                                  #1576.25
        movups    512+__svml_dsinh_ha_data_internal(%rip), %xmm6 #1563.51
        addpd     %xmm6, %xmm9                                  #1576.13
        movups    576+__svml_dsinh_ha_data_internal(%rip), %xmm0 #1582.24
        cmpeqpd   %xmm9, %xmm0                                  #1582.24
        movq      640+__svml_dsinh_ha_data_internal(%rip), %xmm12 #1565.29
        pshufd    $221, %xmm7, %xmm13                           #1578.22
        blendvpd  %xmm0, %xmm6, %xmm9                           #1583.21
        pcmpgtd   %xmm12, %xmm13                                #1579.27
        movdqu    704+__svml_dsinh_ha_data_internal(%rip), %xmm12 #1585.23
        movaps    %xmm9, %xmm1                                  #1592.13
        subpd     %xmm6, %xmm1                                  #1592.13
        movmskps  %xmm13, %edx                                  #1580.44
        pand      %xmm9, %xmm12                                 #1585.23
        movdqa    %xmm12, %xmm14                                #1587.27
        pxor      %xmm12, %xmm9                                 #1586.19
        psllq     $3, %xmm14                                    #1587.27
        psllq     $48, %xmm9                                    #1598.19
        movd      %xmm14, %eax                                  #1588.124
        movups    1280+__svml_dsinh_ha_data_internal(%rip), %xmm15 #1594.35
        movups    1344+__svml_dsinh_ha_data_internal(%rip), %xmm6 #1596.32
        mulpd     %xmm1, %xmm15                                 #1594.35
        mulpd     %xmm1, %xmm6                                  #1596.32
        subpd     %xmm15, %xmm7                                 #1594.13
        movslq    %eax, %rax                                    #1588.124
        pextrd    $2, %xmm14, %ecx                              #1588.251
        movslq    %ecx, %rcx                                    #1588.251
        subpd     %xmm6, %xmm7                                  #1596.13
        movsd     (%rax,%rsi), %xmm5                            #1588.32
        movaps    %xmm7, %xmm1                                  #1597.14
        movhpd    (%rcx,%rsi), %xmm5                            #1588.15
        movsd     256(%rax,%rsi), %xmm3                         #1589.32
        movsd     384(%rax,%rsi), %xmm2                         #1591.36
        movhpd    256(%rcx,%rsi), %xmm3                         #1589.15
        paddq     %xmm9, %xmm5                                  #1600.19
        psubq     %xmm9, %xmm3                                  #1603.19
        mulpd     %xmm7, %xmm1                                  #1597.14
        movhpd    384(%rcx,%rsi), %xmm2                         #1591.19
        movaps    %xmm5, %xmm14                                 #1606.14
        andnps    %xmm2, %xmm8                                  #1610.22
        movaps    %xmm5, %xmm13                                 #1605.14
        subpd     %xmm3, %xmm14                                 #1606.14
        cmpltpd   %xmm9, %xmm8                                  #1612.24
        addpd     %xmm3, %xmm13                                 #1605.14
        psubq     %xmm9, %xmm2                                  #1614.23
        mulpd     %xmm13, %xmm7                                 #1628.15
        andnps    %xmm2, %xmm8                                  #1616.23
        movaps    %xmm14, %xmm2                                 #1617.22
        subpd     %xmm5, %xmm2                                  #1617.22
        addpd     %xmm2, %xmm3                                  #1618.22
        movsd     128(%rax,%rsi), %xmm4                         #1590.36
        movhpd    128(%rcx,%rsi), %xmm4                         #1590.19
        paddq     %xmm9, %xmm4                                  #1608.23
        subpd     %xmm3, %xmm4                                  #1619.26
        movups    1152+__svml_dsinh_ha_data_internal(%rip), %xmm3 #1622.31
        mulpd     %xmm1, %xmm3                                  #1622.31
        subpd     %xmm8, %xmm4                                  #1620.26
        addpd     1024+__svml_dsinh_ha_data_internal(%rip), %xmm3 #1622.19
        movups    1088+__svml_dsinh_ha_data_internal(%rip), %xmm5 #1621.29
        mulpd     %xmm1, %xmm5                                  #1621.29
        mulpd     %xmm1, %xmm3                                  #1624.31
        addpd     960+__svml_dsinh_ha_data_internal(%rip), %xmm5 #1621.17
        addpd     896+__svml_dsinh_ha_data_internal(%rip), %xmm3 #1624.19
        mulpd     %xmm1, %xmm5                                  #1623.29
        mulpd     %xmm1, %xmm3                                  #1625.31
        addpd     832+__svml_dsinh_ha_data_internal(%rip), %xmm5 #1623.17
        addpd     768+__svml_dsinh_ha_data_internal(%rip), %xmm3 #1625.19
        mulpd     %xmm1, %xmm5                                  #1626.13
        mulpd     %xmm3, %xmm1                                  #1627.15
        mulpd     %xmm7, %xmm5                                  #1629.32
        mulpd     %xmm14, %xmm1                                 #1630.29
        addpd     %xmm5, %xmm4                                  #1629.20
        addpd     %xmm1, %xmm4                                  #1630.17
        addpd     %xmm4, %xmm7                                  #1631.19
        addpd     %xmm7, %xmm14                                 #1632.19
        orps      %xmm14, %xmm10                                #1633.14
        andl      $3, %edx                                      #1580.98
        jne       ..B8.3        # Prob 5%                       #1635.52
                                # LOE rbx r12 r13 r14 r15 edx xmm10 xmm11
..B8.2:                         # Preds ..B8.3 ..B8.9 ..B8.1
                                # Execution count [1.00e+00]
        movaps    %xmm10, %xmm0                                 #1638.12
        movq      %rbp, %rsp                                    #1638.12
        popq      %rbp                                          #1638.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1638.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.3:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm11, 64(%rsp)                              #1635.197
        movups    %xmm10, 128(%rsp)                             #1635.270
        je        ..B8.2        # Prob 95%                      #1635.374
                                # LOE rbx r12 r13 r14 r15 edx xmm10
..B8.6:                         # Preds ..B8.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1635.454
        movq      %r12, 8(%rsp)                                 #1635.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1635.454
        movq      %r13, (%rsp)                                  #1635.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1635.454
                                # LOE rbx r12 r14 r15 r13d
..B8.7:                         # Preds ..B8.8 ..B8.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1635.517
        jc        ..B8.10       # Prob 5%                       #1635.517
                                # LOE rbx r12 r14 r15 r13d
..B8.8:                         # Preds ..B8.10 ..B8.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1635.470
        cmpl      $2, %r12d                                     #1635.465
        jl        ..B8.7        # Prob 82%                      #1635.465
                                # LOE rbx r12 r14 r15 r13d
..B8.9:                         # Preds ..B8.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm10                             #1635.674
        jmp       ..B8.2        # Prob 100%                     #1635.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm10
..B8.10:                        # Preds ..B8.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1635.546
        lea       128(%rsp,%r12,8), %rsi                        #1635.546
..___tag_value___svml_sinh2_ha_e9.153:
#       __svml_dsinh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsinh_ha_cout_rare_internal            #1635.546
..___tag_value___svml_sinh2_ha_e9.154:
        jmp       ..B8.8        # Prob 100%                     #1635.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_sinh2_ha_e9,@function
	.size	__svml_sinh2_ha_e9,.-__svml_sinh2_ha_e9
..LN__svml_sinh2_ha_e9.7:
	.data
# -- End  __svml_sinh2_ha_e9
	.text
.L_2__routine_start___svml_sinh8_ha_z0_8:
# -- Begin  __svml_sinh8_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinh8_ha_z0
# --- __svml_sinh8_ha_z0(__m512d)
__svml_sinh8_ha_z0:
# parameter 1: %zmm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1643.1
        .byte     15                                            #1748.759
        .byte     30                                            #1748.759
        .byte     250                                           #1748.759
	.cfi_startproc
..___tag_value___svml_sinh8_ha_z0.156:
..L157:
                                                        #1643.1
        pushq     %rbp                                          #1643.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1643.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1643.1
        subq      $192, %rsp                                    #1643.1
        vmovaps   %zmm0, %zmm6                                  #1643.1
        vmovups   1408+__svml_dsinh_ha_data_internal(%rip), %zmm0 #1688.48
        vmovups   512+__svml_dsinh_ha_data_internal(%rip), %zmm9 #1678.52
        vmovups   1216+__svml_dsinh_ha_data_internal(%rip), %zmm7 #1691.13
        vmovups   576+__svml_dsinh_ha_data_internal(%rip), %zmm14 #1696.58
        vmovups   __svml_dsinh_ha_data_internal(%rip), %zmm12   #1702.281
        vmovups   128+__svml_dsinh_ha_data_internal(%rip), %zmm15 #1704.285
        vmovups   1088+__svml_dsinh_ha_data_internal(%rip), %zmm3 #1686.49
        vmovups   1152+__svml_dsinh_ha_data_internal(%rip), %zmm4 #1687.49
        vmovups   832+__svml_dsinh_ha_data_internal(%rip), %zmm1 #1682.49
        vmovups   896+__svml_dsinh_ha_data_internal(%rip), %zmm2 #1683.49
        vandpd    %zmm6, %zmm0, %zmm5                           #1689.17
        vxorpd    %zmm6, %zmm5, %zmm8                           #1690.16
        vfmadd213pd {rn-sae}, %zmm9, %zmm8, %zmm7               #1691.13
        vpsrlq    $32, %zmm8, %zmm10                            #1693.38
        vcmppd    $0, {sae}, %zmm7, %zmm14, %k1                 #1697.24
        vpmovqd   %zmm10, %ymm11                                #1693.16
        vpcmpgtd  640+__svml_dsinh_ha_data_internal(%rip), %ymm11, %ymm13 #1694.23
        vblendmpd %zmm9, %zmm7, %zmm11{%k1}                     #1698.21
        vmovups   384+__svml_dsinh_ha_data_internal(%rip), %zmm7 #1705.285
        vmovmskps %ymm13, %esi                                  #1695.44
        vmovups   256+__svml_dsinh_ha_data_internal(%rip), %zmm13 #1703.281
        vpandq    704+__svml_dsinh_ha_data_internal(%rip), %zmm11, %zmm10 #1700.17
        vpxorq    %zmm10, %zmm11, %zmm14                        #1701.13
        vpermt2pd 64+__svml_dsinh_ha_data_internal(%rip), %zmm10, %zmm12 #1702.281
        vpermt2pd 320+__svml_dsinh_ha_data_internal(%rip), %zmm10, %zmm13 #1703.281
        vpermt2pd 192+__svml_dsinh_ha_data_internal(%rip), %zmm10, %zmm15 #1704.285
        vpermt2pd 448+__svml_dsinh_ha_data_internal(%rip), %zmm10, %zmm7 #1705.285
        vsubpd    {rn-sae}, %zmm9, %zmm11, %zmm10               #1706.15
        vmovups   1280+__svml_dsinh_ha_data_internal(%rip), %zmm9 #1707.51
        vandnpd   %zmm7, %zmm0, %zmm0                           #1724.22
        vfnmadd231pd {rn-sae}, %zmm9, %zmm10, %zmm8             #1708.13
        vmovups   1344+__svml_dsinh_ha_data_internal(%rip), %zmm9 #1709.51
        vfnmadd231pd {rn-sae}, %zmm9, %zmm10, %zmm8             #1710.13
        vpsllq    $48, %zmm14, %zmm10                           #1712.15
        vmulpd    {rn-sae}, %zmm8, %zmm8, %zmm9                 #1711.16
        vcmppd    $17, {sae}, %zmm10, %zmm0, %k2                #1726.24
        vpaddq    %zmm10, %zmm12, %zmm11                        #1714.13
        vpsubq    %zmm10, %zmm13, %zmm12                        #1717.13
        vpaddq    %zmm10, %zmm15, %zmm15                        #1722.17
        vpsubq    %zmm10, %zmm7, %zmm7                          #1728.17
        vsubpd    {rn-sae}, %zmm12, %zmm11, %zmm14              #1720.16
        vaddpd    {rn-sae}, %zmm12, %zmm11, %zmm13              #1719.16
        vxorpd    %zmm7, %zmm7, %zmm7{%k2}                      #1730.23
        vsubpd    {rn-sae}, %zmm11, %zmm14, %zmm0               #1731.24
        vaddpd    {rn-sae}, %zmm0, %zmm12, %zmm12               #1732.24
        vmovups   960+__svml_dsinh_ha_data_internal(%rip), %zmm0 #1735.17
        vsubpd    {rn-sae}, %zmm12, %zmm15, %zmm15              #1733.28
        vfmadd231pd {rn-sae}, %zmm9, %zmm3, %zmm0               #1735.17
        vmovups   1024+__svml_dsinh_ha_data_internal(%rip), %zmm3 #1736.19
        vsubpd    {rn-sae}, %zmm7, %zmm15, %zmm7                #1734.28
        vfmadd213pd {rn-sae}, %zmm1, %zmm9, %zmm0               #1737.17
        vfmadd231pd {rn-sae}, %zmm9, %zmm4, %zmm3               #1736.19
        vmovups   768+__svml_dsinh_ha_data_internal(%rip), %zmm1 #1739.19
        vmulpd    {rn-sae}, %zmm9, %zmm0, %zmm4                 #1740.15
        vfmadd213pd {rn-sae}, %zmm2, %zmm9, %zmm3               #1738.19
        vfmadd213pd {rn-sae}, %zmm8, %zmm8, %zmm4               #1742.13
        vfmadd213pd {rn-sae}, %zmm1, %zmm9, %zmm3               #1739.19
        vmulpd    {rn-sae}, %zmm9, %zmm3, %zmm2                 #1741.17
        vfmadd213pd {rn-sae}, %zmm7, %zmm14, %zmm2              #1743.19
        vfmadd213pd {rn-sae}, %zmm2, %zmm13, %zmm4              #1744.19
        vaddpd    {rn-sae}, %zmm14, %zmm4, %zmm8                #1745.21
        vorpd     %zmm8, %zmm5, %zmm0                           #1746.14
        testl     %esi, %esi                                    #1748.52
        jne       ..B9.3        # Prob 5%                       #1748.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm6
..B9.2:                         # Preds ..B9.9 ..B9.7 ..B9.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1751.12
        popq      %rbp                                          #1751.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1751.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.3:                         # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #1748.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm6
..B9.4:                         # Preds ..B9.3
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #1748.249
        movl      %edx, %eax                                    #1748.303
        orl       $8064, %eax                                   #1748.303
        cmpl      %eax, %edx                                    #1748.332
        je        ..B9.6        # Prob 78%                      #1748.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm6
..B9.5:                         # Preds ..B9.4
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #1748.344
        vldmxcsr  32(%rsp)                                      #1748.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm6
..B9.6:                         # Preds ..B9.5 ..B9.4
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm6, 64(%rsp)                               #1748.406
        vmovups   %zmm0, 128(%rsp)                              #1748.481
        testl     %esi, %esi                                    #1748.587
        jne       ..B9.11       # Prob 5%                       #1748.587
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B9.7:                         # Preds ..B9.14 ..B9.6
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #1748.931
        je        ..B9.2        # Prob 78%                      #1748.931
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B9.8:                         # Preds ..B9.7
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #1748.956
        movl      32(%rsp), %eax                                #1748.956
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B9.9:                         # Preds ..B9.8
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #1748.943
        orl       %edx, %eax                                    #1748.943
        movl      %eax, 32(%rsp)                                #1748.943
        vldmxcsr  32(%rsp)                                      #1748.943
        jmp       ..B9.2        # Prob 100%                     #1748.943
                                # LOE rbx r12 r13 r14 r15 zmm0
..B9.11:                        # Preds ..B9.6
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #1748.667
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B9.18:                        # Preds ..B9.11
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
..B9.12:                        # Preds ..B9.13 ..B9.18
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #1748.730
        jc        ..B9.15       # Prob 5%                       #1748.730
                                # LOE rbx r15 r12d r13d r14d
..B9.13:                        # Preds ..B9.15 ..B9.12
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #1748.683
        cmpl      $8, %ebx                                      #1748.678
        jl        ..B9.12       # Prob 82%                      #1748.678
                                # LOE rbx r15 r12d r13d r14d
..B9.14:                        # Preds ..B9.13
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
        vmovups   128(%rsp), %zmm0                              #1748.887
        jmp       ..B9.7        # Prob 100%                     #1748.887
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B9.15:                        # Preds ..B9.12
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,8), %rdi                         #1748.759
        lea       128(%rsp,%rbx,8), %rsi                        #1748.759
..___tag_value___svml_sinh8_ha_z0.184:
#       __svml_dsinh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsinh_ha_cout_rare_internal            #1748.759
..___tag_value___svml_sinh8_ha_z0.185:
        jmp       ..B9.13       # Prob 100%                     #1748.759
        .align    16,0x90
                                # LOE r15 ebx r12d r13d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_sinh8_ha_z0,@function
	.size	__svml_sinh8_ha_z0,.-__svml_sinh8_ha_z0
..LN__svml_sinh8_ha_z0.8:
	.data
# -- End  __svml_sinh8_ha_z0
	.text
.L_2__routine_start___svml_dsinh_ha_cout_rare_internal_9:
# -- Begin  __svml_dsinh_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dsinh_ha_cout_rare_internal
	.globl __svml_dsinh_ha_cout_rare_internal
# --- __svml_dsinh_ha_cout_rare_internal(const double *, double *)
__svml_dsinh_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #800.1
        .byte     15                                            #949.15
        .byte     30                                            #949.15
        .byte     250                                           #949.15
	.cfi_startproc
..___tag_value___svml_dsinh_ha_cout_rare_internal.187:
..L188:
                                                        #800.1
        movq      %rsi, %r9                                     #800.1
        movzwl    6(%rdi), %ecx                                 #819.15
        xorl      %eax, %eax                                    #801.14
        andl      $32752, %ecx                                  #820.34
        shrl      $4, %ecx                                      #820.34
        movsd     (%rdi), %xmm2                                 #819.15
        movb      7(%rdi), %dl                                  #819.15
        movsd     %xmm2, -8(%rsp)                               #819.6
        cmpl      $2047, %ecx                                   #820.59
        je        ..B10.17      # Prob 16%                      #820.59
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax ecx dl xmm2
..B10.2:                        # Preds ..B10.1
                                # Execution count [8.40e-01]
        testl     %ecx, %ecx                                    #823.24
        jle       ..B10.16      # Prob 16%                      #823.24
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax ecx dl xmm2
..B10.3:                        # Preds ..B10.2
                                # Execution count [7.06e-01]
        andb      $127, %dl                                     #826.39
        movsd     %xmm2, -32(%rsp)                              #825.18
        movb      %dl, -25(%rsp)                                #826.39
        cmpl      $969, %ecx                                    #827.29
        jle       ..B10.14      # Prob 50%                      #827.29
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax xmm2
..B10.4:                        # Preds ..B10.3
                                # Execution count [3.53e-01]
        movsd     -32(%rsp), %xmm0                              #829.28
        movsd     1136+_vmldSinhHATab(%rip), %xmm1              #829.53
        comisd    %xmm0, %xmm1                                  #829.53
        jbe       ..B10.13      # Prob 50%                      #829.53
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax xmm0 xmm2
..B10.5:                        # Preds ..B10.4
                                # Execution count [1.76e-01]
        movsd     1184+_vmldSinhHATab(%rip), %xmm1              #831.59
        comisd    %xmm0, %xmm1                                  #831.59
        jbe       ..B10.9       # Prob 50%                      #831.59
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax xmm0
..B10.6:                        # Preds ..B10.5
                                # Execution count [8.82e-02]
        comisd    1176+_vmldSinhHATab(%rip), %xmm0              #833.65
        jb        ..B10.8       # Prob 50%                      #833.65
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax xmm0
..B10.7:                        # Preds ..B10.6
                                # Execution count [4.41e-02]
        movsd     1112+_vmldSinhHATab(%rip), %xmm1              #835.81
        lea       _vmldSinhHATab(%rip), %rcx                    #851.63
        mulsd     %xmm0, %xmm1                                  #835.81
        movsd     1144+_vmldSinhHATab(%rip), %xmm4              #838.85
        movq      8+_vmldSinhHATab(%rip), %r10                  #818.5
        movq      %r10, %rsi                                    #850.60
        shrq      $48, %rsi                                     #850.60
        addsd     1120+_vmldSinhHATab(%rip), %xmm1              #836.80
        movsd     %xmm1, -40(%rsp)                              #836.39
        andl      $-32753, %esi                                 #850.60
        movsd     -40(%rsp), %xmm10                             #837.56
        movl      -40(%rsp), %r8d                               #845.64
        movl      %r8d, %r11d                                   #847.48
        shrl      $6, %r11d                                     #847.48
        andl      $63, %r8d                                     #846.48
        movq      %r10, -16(%rsp)                               #818.5
        subsd     1120+_vmldSinhHATab(%rip), %xmm10             #837.78
        mulsd     %xmm10, %xmm4                                 #838.85
        lea       1023(%r11), %edi                              #848.47
        xorps     .L_2il0floatpacket.105(%rip), %xmm10          #839.64
        addl      $1022, %r11d                                  #850.88
        mulsd     1152+_vmldSinhHATab(%rip), %xmm10             #839.85
        subsd     %xmm4, %xmm0                                  #838.85
        movaps    %xmm10, %xmm2                                 #840.62
        movaps    %xmm0, %xmm8                                  #840.95
        andl      $2047, %r11d                                  #850.60
        lea       (%r8,%r8), %edx                               #851.81
        negl      %edi                                          #853.53
        lea       1(%r8,%r8), %r8d                              #852.87
        movsd     (%rcx,%rdx,8), %xmm9                          #851.63
        negl      %edx                                          #855.85
        shll      $4, %r11d                                     #850.60
        addl      $-4, %edi                                     #854.88
        orl       %r11d, %esi                                   #850.60
        andl      $2047, %edi                                   #854.60
        movw      %si, -10(%rsp)                                #850.60
        andl      $-32753, %esi                                 #854.60
        shll      $4, %edi                                      #854.60
        addsd     %xmm0, %xmm2                                  #840.62
        movsd     %xmm2, -24(%rsp)                              #840.39
        orl       %edi, %esi                                    #854.60
        movsd     -24(%rsp), %xmm6                              #840.95
        movsd     1128+_vmldSinhHATab(%rip), %xmm2              #841.77
        subsd     %xmm6, %xmm8                                  #840.95
        movsd     %xmm8, -56(%rsp)                              #840.72
        movsd     -24(%rsp), %xmm12                             #840.115
        movsd     -56(%rsp), %xmm11                             #840.120
        movsd     (%rcx,%r8,8), %xmm7                           #852.63
        addsd     %xmm11, %xmm12                                #840.120
        movsd     %xmm12, -48(%rsp)                             #840.101
        movsd     -56(%rsp), %xmm5                              #840.149
        addsd     %xmm5, %xmm10                                 #840.149
        movsd     -16(%rsp), %xmm5                              #851.88
        mulsd     %xmm5, %xmm9                                  #851.88
        mulsd     %xmm5, %xmm7                                  #852.92
        movsd     %xmm10, -56(%rsp)                             #840.126
        movaps    %xmm9, %xmm10                                 #857.53
        movsd     -48(%rsp), %xmm13                             #840.178
        movw      %si, -10(%rsp)                                #854.60
        lea       128(%rdx), %esi                               #855.85
        movsd     -16(%rsp), %xmm14                             #855.93
        addl      $129, %edx                                    #856.85
        subsd     %xmm13, %xmm0                                 #840.178
        movsd     %xmm0, -48(%rsp)                              #840.155
        movsd     -56(%rsp), %xmm1                              #840.198
        movsd     -48(%rsp), %xmm15                             #840.203
        addsd     %xmm15, %xmm1                                 #840.203
        movsd     %xmm1, -48(%rsp)                              #840.184
        movsd     -24(%rsp), %xmm4                              #840.213
        mulsd     %xmm4, %xmm2                                  #841.77
        movaps    %xmm4, %xmm3                                  #842.55
        mulsd     %xmm4, %xmm3                                  #842.55
        movsd     -48(%rsp), %xmm1                              #840.223
        movaps    %xmm4, %xmm0                                  #841.173
        movsd     %xmm2, -24(%rsp)                              #841.39
        movsd     -24(%rsp), %xmm6                              #841.116
        movsd     1064+_vmldSinhHATab(%rip), %xmm2              #843.98
        subsd     %xmm4, %xmm6                                  #841.121
        mulsd     %xmm3, %xmm2                                  #843.98
        movsd     %xmm6, -56(%rsp)                              #841.102
        movsd     -24(%rsp), %xmm11                             #841.142
        movsd     -56(%rsp), %xmm8                              #841.147
        subsd     %xmm8, %xmm11                                 #841.147
        addsd     1048+_vmldSinhHATab(%rip), %xmm2              #843.121
        movsd     %xmm11, -24(%rsp)                             #841.128
        movsd     1072+_vmldSinhHATab(%rip), %xmm11             #844.98
        mulsd     %xmm3, %xmm11                                 #844.98
        mulsd     %xmm3, %xmm2                                  #843.145
        addsd     1056+_vmldSinhHATab(%rip), %xmm11             #844.121
        mulsd     %xmm4, %xmm2                                  #843.153
        mulsd     %xmm3, %xmm11                                 #844.145
        movsd     -24(%rsp), %xmm12                             #841.173
        addsd     1040+_vmldSinhHATab(%rip), %xmm11             #844.169
        subsd     %xmm12, %xmm0                                 #841.173
        mulsd     %xmm3, %xmm11                                 #844.194
        movsd     (%rcx,%rsi,8), %xmm3                          #855.63
        movaps    %xmm9, %xmm12                                 #857.98
        mulsd     %xmm14, %xmm3                                 #855.93
        movsd     %xmm0, -56(%rsp)                              #841.153
        subsd     %xmm3, %xmm10                                 #857.53
        movsd     -24(%rsp), %xmm8                              #841.185
        movsd     -56(%rsp), %xmm6                              #841.196
        movsd     %xmm10, -24(%rsp)                             #857.39
        movsd     -24(%rsp), %xmm13                             #857.98
        movsd     (%rcx,%rdx,8), %xmm0                          #856.63
        subsd     %xmm13, %xmm12                                #857.98
        mulsd     %xmm14, %xmm0                                 #856.97
        subsd     %xmm3, %xmm12                                 #857.118
        movsd     %xmm12, -56(%rsp)                             #857.104
        movsd     -24(%rsp), %xmm14                             #857.142
        movsd     -56(%rsp), %xmm12                             #857.152
        movaps    %xmm14, %xmm5                                 #860.61
        movb      -1(%rsp), %cl                                 #885.97
        addsd     %xmm7, %xmm12                                 #859.39
        addsd     %xmm0, %xmm7                                  #862.63
        subsd     %xmm0, %xmm12                                 #858.39
        andb      $-128, %cl                                    #885.60
        addsd     %xmm12, %xmm5                                 #860.61
        movsd     %xmm5, -24(%rsp)                              #860.39
        movaps    %xmm9, %xmm5                                  #861.62
        movsd     -24(%rsp), %xmm15                             #860.94
        addsd     %xmm3, %xmm5                                  #861.62
        subsd     %xmm15, %xmm14                                #860.94
        addsd     %xmm14, %xmm12                                #860.121
        movsd     %xmm12, -56(%rsp)                             #860.100
        movsd     -24(%rsp), %xmm10                             #860.136
        movsd     -56(%rsp), %xmm12                             #860.146
        movsd     %xmm5, -24(%rsp)                              #861.39
        movsd     -24(%rsp), %xmm13                             #861.97
        subsd     %xmm13, %xmm9                                 #861.97
        addsd     %xmm9, %xmm3                                  #861.124
        movsd     %xmm3, -56(%rsp)                              #861.103
        movsd     -24(%rsp), %xmm3                              #861.140
        movsd     -56(%rsp), %xmm5                              #861.150
        addsd     %xmm7, %xmm5                                  #863.39
        movaps    %xmm3, %xmm7                                  #864.61
        addsd     %xmm5, %xmm7                                  #864.61
        movsd     %xmm7, -24(%rsp)                              #864.39
        movsd     -24(%rsp), %xmm9                              #864.94
        movsd     1128+_vmldSinhHATab(%rip), %xmm7              #865.79
        subsd     %xmm9, %xmm3                                  #864.94
        addsd     %xmm3, %xmm5                                  #864.121
        movsd     %xmm5, -56(%rsp)                              #864.100
        movsd     -24(%rsp), %xmm0                              #864.136
        mulsd     %xmm0, %xmm7                                  #865.79
        movsd     -56(%rsp), %xmm3                              #864.146
        movsd     %xmm7, -24(%rsp)                              #865.39
        movaps    %xmm0, %xmm7                                  #865.179
        movsd     -24(%rsp), %xmm14                             #865.118
        mulsd     %xmm3, %xmm4                                  #869.63
        subsd     %xmm0, %xmm14                                 #865.123
        movsd     %xmm14, -56(%rsp)                             #865.104
        movsd     -24(%rsp), %xmm13                             #865.146
        movsd     -56(%rsp), %xmm9                              #865.151
        subsd     %xmm9, %xmm13                                 #865.151
        movsd     %xmm13, -24(%rsp)                             #865.132
        movaps    %xmm0, %xmm13                                 #872.63
        movsd     -24(%rsp), %xmm15                             #865.179
        mulsd     %xmm2, %xmm13                                 #872.63
        subsd     %xmm15, %xmm7                                 #865.179
        mulsd     %xmm1, %xmm0                                  #870.63
        mulsd     %xmm3, %xmm1                                  #866.63
        mulsd     %xmm3, %xmm2                                  #867.63
        movaps    %xmm10, %xmm15                                #873.63
        movaps    %xmm13, %xmm5                                 #874.60
        mulsd     %xmm11, %xmm15                                #873.63
        mulsd     %xmm12, %xmm11                                #868.63
        addsd     %xmm15, %xmm5                                 #874.60
        addsd     %xmm12, %xmm11                                #867.39
        movsd     %xmm7, -56(%rsp)                              #865.157
        addsd     %xmm0, %xmm11                                 #868.39
        movsd     -24(%rsp), %xmm9                              #865.192
        addsd     %xmm1, %xmm11                                 #869.39
        movsd     -56(%rsp), %xmm7                              #865.203
        addsd     %xmm2, %xmm11                                 #870.39
        movsd     %xmm5, -24(%rsp)                              #874.39
        addsd     %xmm4, %xmm11                                 #871.39
        movsd     -24(%rsp), %xmm14                             #874.91
        subsd     %xmm14, %xmm13                                #874.91
        addsd     %xmm13, %xmm15                                #874.118
        movsd     %xmm15, -56(%rsp)                             #874.97
        movaps    %xmm8, %xmm15                                 #879.62
        mulsd     %xmm9, %xmm15                                 #879.62
        mulsd     %xmm6, %xmm9                                  #878.62
        mulsd     %xmm7, %xmm6                                  #876.62
        mulsd     %xmm7, %xmm8                                  #877.62
        movsd     -24(%rsp), %xmm14                             #874.132
        movaps    %xmm15, %xmm13                                #880.61
        movsd     -56(%rsp), %xmm5                              #874.141
        addsd     %xmm14, %xmm13                                #880.61
        addsd     %xmm9, %xmm5                                  #876.39
        movsd     %xmm13, -24(%rsp)                             #880.39
        addsd     %xmm6, %xmm5                                  #877.39
        movsd     -24(%rsp), %xmm13                             #880.94
        addsd     %xmm8, %xmm5                                  #878.39
        subsd     %xmm13, %xmm15                                #880.94
        addsd     %xmm5, %xmm11                                 #875.39
        addsd     %xmm15, %xmm14                                #880.121
        movsd     %xmm14, -56(%rsp)                             #880.100
        movaps    %xmm10, %xmm15                                #882.61
        movsd     -24(%rsp), %xmm13                             #880.136
        movsd     -56(%rsp), %xmm14                             #880.145
        addsd     %xmm13, %xmm15                                #882.61
        addsd     %xmm14, %xmm11                                #881.39
        movsd     %xmm15, -24(%rsp)                             #882.39
        movsd     -24(%rsp), %xmm15                             #882.94
        subsd     %xmm15, %xmm10                                #882.94
        addsd     %xmm10, %xmm13                                #882.121
        movsd     %xmm13, -56(%rsp)                             #882.100
        movsd     -24(%rsp), %xmm13                             #882.136
        movsd     -56(%rsp), %xmm10                             #882.145
        addsd     %xmm10, %xmm11                                #883.39
        addsd     %xmm11, %xmm13                                #884.63
        movsd     %xmm13, -32(%rsp)                             #884.39
        movb      -25(%rsp), %dil                               #885.60
        andb      $127, %dil                                    #885.60
        orb       %cl, %dil                                     #885.60
        movb      %dil, -25(%rsp)                               #885.60
        movq      -32(%rsp), %r10                               #886.46
        movq      %r10, (%r9)                                   #886.41
        ret                                                     #886.41
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.8:                        # Preds ..B10.6
                                # Execution count [4.41e-02]
        movaps    %xmm0, %xmm2                                  #890.62
        mulsd     %xmm0, %xmm2                                  #890.62
        movsd     1104+_vmldSinhHATab(%rip), %xmm1              #891.217
        mulsd     %xmm2, %xmm1                                  #891.217
        movb      -1(%rsp), %dl                                 #893.97
        andb      $-128, %dl                                    #893.60
        addsd     1096+_vmldSinhHATab(%rip), %xmm1              #891.217
        mulsd     %xmm2, %xmm1                                  #891.217
        addsd     1088+_vmldSinhHATab(%rip), %xmm1              #891.217
        mulsd     %xmm2, %xmm1                                  #891.217
        addsd     1080+_vmldSinhHATab(%rip), %xmm1              #891.217
        mulsd     %xmm1, %xmm2                                  #891.217
        mulsd     %xmm0, %xmm2                                  #891.217
        addsd     %xmm2, %xmm0                                  #892.39
        movsd     %xmm0, -32(%rsp)                              #892.39
        movb      -25(%rsp), %cl                                #893.60
        andb      $127, %cl                                     #893.60
        orb       %dl, %cl                                      #893.60
        movb      %cl, -25(%rsp)                                #893.60
        movq      -32(%rsp), %rsi                               #894.46
        movq      %rsi, (%r9)                                   #894.41
        ret                                                     #894.41
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.9:                        # Preds ..B10.5
                                # Execution count [8.82e-02]
        movsd     1112+_vmldSinhHATab(%rip), %xmm1              #899.66
        lea       _vmldSinhHATab(%rip), %r8                     #911.56
        mulsd     %xmm0, %xmm1                                  #899.66
        movsd     1144+_vmldSinhHATab(%rip), %xmm2              #902.79
        movsd     1152+_vmldSinhHATab(%rip), %xmm3              #903.79
        movq      8+_vmldSinhHATab(%rip), %rdx                  #818.5
        movq      %rdx, -16(%rsp)                               #818.5
        addsd     1120+_vmldSinhHATab(%rip), %xmm1              #900.74
        movsd     %xmm1, -40(%rsp)                              #900.34
        movsd     -40(%rsp), %xmm4                              #901.50
        movsd     1072+_vmldSinhHATab(%rip), %xmm1              #904.67
        movl      -40(%rsp), %edx                               #907.60
        movl      %edx, %esi                                    #908.48
        andl      $63, %esi                                     #908.48
        subsd     1120+_vmldSinhHATab(%rip), %xmm4              #901.72
        mulsd     %xmm4, %xmm2                                  #902.79
        lea       (%rsi,%rsi), %ecx                             #911.74
        mulsd     %xmm3, %xmm4                                  #903.79
        subsd     %xmm2, %xmm0                                  #902.79
        movsd     (%r8,%rcx,8), %xmm5                           #911.56
        lea       1(%rsi,%rsi), %edi                            #912.80
        shrl      $6, %edx                                      #909.44
        subsd     %xmm4, %xmm0                                  #903.34
        mulsd     %xmm0, %xmm1                                  #904.90
        addl      $1022, %edx                                   #914.43
        andl      $2047, %edx                                   #914.48
        addsd     1064+_vmldSinhHATab(%rip), %xmm1              #904.111
        mulsd     %xmm0, %xmm1                                  #904.135
        addsd     1056+_vmldSinhHATab(%rip), %xmm1              #904.156
        mulsd     %xmm0, %xmm1                                  #904.180
        addsd     1048+_vmldSinhHATab(%rip), %xmm1              #904.201
        mulsd     %xmm0, %xmm1                                  #904.225
        addsd     1040+_vmldSinhHATab(%rip), %xmm1              #904.246
        mulsd     %xmm0, %xmm1                                  #905.51
        mulsd     %xmm0, %xmm1                                  #906.51
        addsd     %xmm0, %xmm1                                  #906.55
        mulsd     %xmm5, %xmm1                                  #911.34
        addsd     (%r8,%rdi,8), %xmm1                           #912.34
        addsd     %xmm5, %xmm1                                  #913.34
        cmpl      $2046, %edx                                   #915.44
        ja        ..B10.11      # Prob 50%                      #915.44
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax edx xmm1
..B10.10:                       # Preds ..B10.9
                                # Execution count [4.41e-02]
        movq      8+_vmldSinhHATab(%rip), %rcx                  #917.60
        shrq      $48, %rcx                                     #917.60
        shll      $4, %edx                                      #917.60
        andl      $-32753, %ecx                                 #917.60
        orl       %edx, %ecx                                    #917.60
        movw      %cx, -10(%rsp)                                #917.60
        movsd     -16(%rsp), %xmm0                              #918.54
        mulsd     %xmm0, %xmm1                                  #918.54
        movsd     %xmm1, -32(%rsp)                              #918.39
        jmp       ..B10.12      # Prob 100%                     #918.39
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax
..B10.11:                       # Preds ..B10.9
                                # Execution count [4.41e-02]
        decl      %edx                                          #922.88
        andl      $2047, %edx                                   #922.60
        movzwl    -10(%rsp), %ecx                               #922.60
        shll      $4, %edx                                      #922.60
        andl      $-32753, %ecx                                 #922.60
        orl       %edx, %ecx                                    #922.60
        movw      %cx, -10(%rsp)                                #922.60
        movsd     -16(%rsp), %xmm0                              #923.54
        mulsd     %xmm0, %xmm1                                  #923.54
        mulsd     1024+_vmldSinhHATab(%rip), %xmm1              #924.71
        movsd     %xmm1, -32(%rsp)                              #924.39
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax
..B10.12:                       # Preds ..B10.10 ..B10.11
                                # Execution count [8.82e-02]
        movb      -25(%rsp), %cl                                #926.55
        movb      -1(%rsp), %dl                                 #926.92
        andb      $127, %cl                                     #926.55
        andb      $-128, %dl                                    #926.55
        orb       %dl, %cl                                      #926.55
        movb      %cl, -25(%rsp)                                #926.55
        movq      -32(%rsp), %rsi                               #927.41
        movq      %rsi, (%r9)                                   #927.36
        ret                                                     #927.36
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.13:                       # Preds ..B10.4
                                # Execution count [1.76e-01]
        movsd     1168+_vmldSinhHATab(%rip), %xmm0              #932.53
        movl      $3, %eax                                      #933.29
        mulsd     %xmm2, %xmm0                                  #932.76
        movsd     %xmm0, (%r9)                                  #932.31
        ret                                                     #932.31
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.14:                       # Preds ..B10.3
                                # Execution count [3.53e-01]
        movsd     _vmldSinhHATab(%rip), %xmm0                   #938.48
        addsd     1160+_vmldSinhHATab(%rip), %xmm0              #938.86
        mulsd     %xmm2, %xmm0                                  #938.110
        movsd     %xmm0, (%r9)                                  #938.25
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.15:                       # Preds ..B10.14
                                # Execution count [1.00e+00]
        ret                                                     #951.13
                                # LOE
..B10.16:                       # Preds ..B10.2
                                # Execution count [1.34e-01]: Infreq
        movsd     1160+_vmldSinhHATab(%rip), %xmm0              #943.49
        mulsd     %xmm0, %xmm2                                  #943.49
        movsd     %xmm2, -24(%rsp)                              #943.18
        movsd     -24(%rsp), %xmm1                              #944.34
        movq      8+_vmldSinhHATab(%rip), %rdx                  #818.5
        movq      %rdx, -16(%rsp)                               #818.5
        addsd     -8(%rsp), %xmm1                               #944.34
        movsd     %xmm1, (%r9)                                  #944.20
        ret                                                     #944.20
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.17:                       # Preds ..B10.1
                                # Execution count [1.60e-01]: Infreq
        addsd     %xmm2, %xmm2                                  #949.27
        movsd     %xmm2, (%r9)                                  #949.15
        ret                                                     #949.15
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_dsinh_ha_cout_rare_internal,@function
	.size	__svml_dsinh_ha_cout_rare_internal,.-__svml_dsinh_ha_cout_rare_internal
..LN__svml_dsinh_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dsinh_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_dsinh_ha_data_internal
	.globl __svml_dsinh_ha_data_internal
__svml_dsinh_ha_data_internal:
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
	.long	0
	.long	396109971
	.long	3162414115
	.long	3613079303
	.long	1013116162
	.long	2303740125
	.long	1012994149
	.long	878562433
	.long	1014473165
	.long	2960257726
	.long	1012694086
	.long	1451641639
	.long	1014426097
	.long	3993278767
	.long	3160675703
	.long	330458198
	.long	3162234164
	.long	88491949
	.long	1014379084
	.long	2951496418
	.long	1012745111
	.long	300981948
	.long	1013635593
	.long	3526460132
	.long	1013380202
	.long	3066496371
	.long	1014607998
	.long	3117910646
	.long	3160510529
	.long	1255956747
	.long	1014539822
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
	.long	1
	.long	1123549184
	.long	1
	.long	1123549184
	.long	1
	.long	1123549184
	.long	1
	.long	1123549184
	.long	1
	.long	1123549184
	.long	1
	.long	1123549184
	.long	1
	.long	1123549184
	.long	1
	.long	1123549184
	.long	1082453555
	.long	1082453555
	.long	1082453555
	.long	1082453555
	.long	1082453555
	.long	1082453555
	.long	1082453555
	.long	1082453555
	.long	1082453555
	.long	1082453555
	.long	1082453555
	.long	1082453555
	.long	1082453555
	.long	1082453555
	.long	1082453555
	.long	1082453555
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
	.long	1431657638
	.long	1069897045
	.long	1431657638
	.long	1069897045
	.long	1431657638
	.long	1069897045
	.long	1431657638
	.long	1069897045
	.long	1431657638
	.long	1069897045
	.long	1431657638
	.long	1069897045
	.long	1431657638
	.long	1069897045
	.long	1431657638
	.long	1069897045
	.long	1431653196
	.long	1067799893
	.long	1431653196
	.long	1067799893
	.long	1431653196
	.long	1067799893
	.long	1431653196
	.long	1067799893
	.long	1431653196
	.long	1067799893
	.long	1431653196
	.long	1067799893
	.long	1431653196
	.long	1067799893
	.long	1431653196
	.long	1067799893
	.long	249327322
	.long	1065423121
	.long	249327322
	.long	1065423121
	.long	249327322
	.long	1065423121
	.long	249327322
	.long	1065423121
	.long	249327322
	.long	1065423121
	.long	249327322
	.long	1065423121
	.long	249327322
	.long	1065423121
	.long	249327322
	.long	1065423121
	.long	419584011
	.long	1062650220
	.long	419584011
	.long	1062650220
	.long	419584011
	.long	1062650220
	.long	419584011
	.long	1062650220
	.long	419584011
	.long	1062650220
	.long	419584011
	.long	1062650220
	.long	419584011
	.long	1062650220
	.long	419584011
	.long	1062650220
	.long	100753094
	.long	1059717741
	.long	100753094
	.long	1059717741
	.long	100753094
	.long	1059717741
	.long	100753094
	.long	1059717741
	.long	100753094
	.long	1059717741
	.long	100753094
	.long	1059717741
	.long	100753094
	.long	1059717741
	.long	100753094
	.long	1059717741
	.long	461398617
	.long	1056571820
	.long	461398617
	.long	1056571820
	.long	461398617
	.long	1056571820
	.long	461398617
	.long	1056571820
	.long	461398617
	.long	1056571820
	.long	461398617
	.long	1056571820
	.long	461398617
	.long	1056571820
	.long	461398617
	.long	1056571820
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
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	1431655765
	.long	1069897045
	.long	286334782
	.long	1065423121
	.long	381082983
	.long	1059717536
	.long	2235373439
	.long	1053236796
	.long	971595069
	.long	1045573268
	.long	1394307292
	.long	1048884517
	.long	2766897055
	.long	997797591
	.long	0
	.long	1072693248
	.long	1431655765
	.long	1069897045
	.long	286333517
	.long	1065423121
	.long	407944761
	.long	1059717536
	.long	3876143447
	.long	1053236746
	.long	1085026835
	.long	1045917151
	.long	3838336303
	.long	1047437007
	.long	4128322810
	.long	1069959864
	.long	3949957238
	.long	1072709030
	.long	4128323011
	.long	1068911288
	.long	971639678
	.long	1069918089
	.long	2624579608
	.long	1065203958
	.long	822211925
	.long	1065439956
	.long	1030168058
	.long	1060091065
	.long	56186104
	.long	1059743035
	.long	1749136243
	.long	1070650064
	.long	3139812683
	.long	1072728870
	.long	1749135426
	.long	1069601488
	.long	1323087056
	.long	1069944542
	.long	904573411
	.long	1065774699
	.long	3069035452
	.long	1065461118
	.long	2025435543
	.long	1060629881
	.long	4176073593
	.long	1059774207
	.long	1719614413
	.long	1071030430
	.long	3437194458
	.long	1072756854
	.long	1719611085
	.long	1069981854
	.long	1719593135
	.long	1069981854
	.long	3767935290
	.long	1066281853
	.long	1746815117
	.long	1065490968
	.long	635147253
	.long	1061164846
	.long	4090120640
	.long	1059819266
	.long	2938050448
	.long	1071421935
	.long	1449319160
	.long	1072793193
	.long	2938047542
	.long	1070373359
	.long	3364062362
	.long	1070030305
	.long	1975058862
	.long	1066602826
	.long	2155069416
	.long	1065529729
	.long	450571058
	.long	1061443274
	.long	485907960
	.long	1059878574
	.long	2487351331
	.long	1071736096
	.long	1329190813
	.long	1072838159
	.long	2487349750
	.long	1070687520
	.long	1772223462
	.long	1070090260
	.long	3333182165
	.long	1066873216
	.long	1054730831
	.long	1065577693
	.long	284500812
	.long	1061731702
	.long	3231799560
	.long	1059950957
	.long	3200056266
	.long	1071947452
	.long	2107451043
	.long	1072892090
	.long	3200054203
	.long	1070898876
	.long	2809911099
	.long	1070162168
	.long	4289767168
	.long	1067155024
	.long	3501442301
	.long	1065635219
	.long	598006601
	.long	1062032296
	.long	2038415185
	.long	1060039020
	.long	0
	.long	1072168960
	.long	0
	.long	1072955392
	.long	415
	.long	1071120384
	.long	2863310733
	.long	1070246570
	.long	4273731122
	.long	1067450367
	.long	1446964110
	.long	1065702741
	.long	1489023795
	.long	1062277465
	.long	1171118343
	.long	1060126972
	.long	1102821844
	.long	1072402282
	.long	1478280716
	.long	1073028539
	.long	1102821797
	.long	1071353706
	.long	1971033038
	.long	1070344100
	.long	727485122
	.long	1067605916
	.long	845580336
	.long	1065780765
	.long	4255115201
	.long	1062443383
	.long	3620060089
	.long	1060186176
	.long	772585791
	.long	1072649172
	.long	4250734987
	.long	1073112081
	.long	772586472
	.long	1071600596
	.long	2804330614
	.long	1070455490
	.long	1931665979
	.long	1067770509
	.long	1452324392
	.long	1065869877
	.long	1391866018
	.long	1062618974
	.long	3013494930
	.long	1060254211
	.long	753980940
	.long	1072802366
	.long	2105245252
	.long	1073206647
	.long	753978640
	.long	1071753790
	.long	4238603591
	.long	1070581577
	.long	1034108977
	.long	1067945384
	.long	1560353218
	.long	1065970747
	.long	4175683972
	.long	1062805415
	.long	3375885113
	.long	1060329560
	.long	2578404435
	.long	1072942218
	.long	860824390
	.long	1073312946
	.long	2578404727
	.long	1071893642
	.long	3437195612
	.long	1070659702
	.long	557897458
	.long	1068131854
	.long	2035738069
	.long	1066084132
	.long	3990308259
	.long	1063004428
	.long	1264618213
	.long	1060417649
	.long	3884178326
	.long	1073091817
	.long	2630968863
	.long	1073431776
	.long	3884182706
	.long	1072043241
	.long	322344088
	.long	1070738923
	.long	3659529551
	.long	1068331319
	.long	3252816661
	.long	1066210884
	.long	3282148564
	.long	1063217362
	.long	698592167
	.long	1060515757
	.long	3625229515
	.long	1073252287
	.long	1520503441
	.long	1073564031
	.long	3625227566
	.long	1072203711
	.long	3876964346
	.long	1070827092
	.long	3857858996
	.long	1068522111
	.long	3302636947
	.long	1066351956
	.long	1934225563
	.long	1063350709
	.long	1172945773
	.long	1060620467
	.long	3580623448
	.long	1073424833
	.long	3834334903
	.long	1073710703
	.long	3580622127
	.long	1072376257
	.long	2556214130
	.long	1070924874
	.long	2390331068
	.long	1068637142
	.long	3061732144
	.long	1066455099
	.long	132457325
	.long	1063473428
	.long	77267609
	.long	1060740146
	.long	0
	.long	1073610752
	.long	0
	.long	1073807360
	.long	519
	.long	1072562176
	.long	2863310855
	.long	1071033002
	.long	4281691395
	.long	1068761087
	.long	585618131
	.long	1066541602
	.long	2974320400
	.long	1063605679
	.long	678988630
	.long	1060872621
	.long	1972728919
	.long	1073776631
	.long	56962720
	.long	1073896825
	.long	1972726129
	.long	1072728055
	.long	1507574747
	.long	1071152289
	.long	1236474596
	.long	1068894879
	.long	2596242240
	.long	1066637031
	.long	2551117528
	.long	1063748280
	.long	2378695948
	.long	1061015700
	.long	224845944
	.long	1073885113
	.long	117214643
	.long	1073995331
	.long	224844225
	.long	1072836537
	.long	3019581943
	.long	1071283630
	.long	1750030683
	.long	1069039521
	.long	2695514049
	.long	1066742104
	.long	3624170218
	.long	1063902615
	.long	1107335813
	.long	1061167917
	.long	2871317792
	.long	1074002547
	.long	4282875693
	.long	1074103617
	.long	2871320350
	.long	1072953971
	.long	1415562586
	.long	1071428013
	.long	3773014827
	.long	1069196100
	.long	2382174280
	.long	1066857610
	.long	129163764
	.long	1064069809
	.long	3640675859
	.long	1061257602
	.long	1934311918
	.long	1074129817
	.long	1504916907
	.long	1074222499
	.long	1934313764
	.long	1073081241
	.long	3438212916
	.long	1071586521
	.long	3966326130
	.long	1069365793
	.long	1845065342
	.long	1066984417
	.long	3389728721
	.long	1064250797
	.long	3527403372
	.long	1061353326
	.long	1803612042
	.long	1074267878
	.long	416567852
	.long	1074352868
	.long	1803613615
	.long	1073219302
	.long	1709375870
	.long	1071702509
	.long	2613564055
	.long	1069548697
	.long	1615778159
	.long	1067123477
	.long	4047280426
	.long	1064375895
	.long	706407907
	.long	1061459709
	.long	2835957237
	.long	1074417767
	.long	2309775719
	.long	1074495703
	.long	2835955196
	.long	1073369191
	.long	108179025
	.long	1071797733
	.long	3331934321
	.long	1069648623
	.long	1549280459
	.long	1067275835
	.long	231503331
	.long	1064482420
	.long	4215494979
	.long	1061574077
	.long	454539771
	.long	1074580611
	.long	2665451282
	.long	1074652078
	.long	454542900
	.long	1073532035
	.long	345326668
	.long	1071901983
	.long	267850325
	.long	1069757186
	.long	924938247
	.long	1067442635
	.long	2202586481
	.long	1064598326
	.long	500518451
	.long	1061703394
	.long	0
	.long	1074757632
	.long	0
	.long	1074806784
	.long	545
	.long	1073709056
	.long	2863310885
	.long	1072016042
	.long	4281026953
	.long	1069875199
	.long	1444035183
	.long	1067537749
	.long	1197943083
	.long	1064724165
	.long	1077022065
	.long	1061841306
	.long	180574770
	.long	1074870280
	.long	1849116868
	.long	1074900328
	.long	180573421
	.long	1073821704
	.long	2465482804
	.long	1072140768
	.long	253611511
	.long	1070003552
	.long	385242732
	.long	1067637530
	.long	1129514441
	.long	1064861019
	.long	2031501120
	.long	1061992766
	.long	3405709678
	.long	1074975020
	.long	1231318205
	.long	1075002575
	.long	3405708510
	.long	1073926444
	.long	3073397463
	.long	1072277097
	.long	1686448159
	.long	1070143206
	.long	1867096400
	.long	1067746593
	.long	1953490747
	.long	1065009999
	.long	3754852836
	.long	1062158409
	.long	179426533
	.long	1075089025
	.long	2679799656
	.long	1075114292
	.long	179430305
	.long	1074040449
	.long	709782363
	.long	1072426054
	.long	162786634
	.long	1070295212
	.long	1813467522
	.long	1067865758
	.long	1117834422
	.long	1065172339
	.long	1589567099
	.long	1062275417
	.long	699546965
	.long	1075213149
	.long	2739681860
	.long	1075236319
	.long	699547067
	.long	1074164573
	.long	3652911786
	.long	1072588756
	.long	3781056825
	.long	1070460710
	.long	2893910397
	.long	1067995920
	.long	2126742205
	.long	1065348739
	.long	4146131465
	.long	1062373562
	.long	2357212295
	.long	1075348325
	.long	4157934896
	.long	1075369572
	.long	2357213166
	.long	1074299749
	.long	4203608045
	.long	1072729837
	.long	125162258
	.long	1070618521
	.long	2552197783
	.long	1068138057
	.long	2468663921
	.long	1065447123
	.long	1434477412
	.long	1062481574
	.long	2638639167
	.long	1075495569
	.long	2507093787
	.long	1075515053
	.long	2638641368
	.long	1074446993
	.long	239748246
	.long	1072826825
	.long	3164087932
	.long	1070716683
	.long	3550045066
	.long	1068293236
	.long	916187271
	.long	1065551862
	.long	3816237083
	.long	1062600652
	.long	1820502500
	.long	1075655987
	.long	1299488554
	.long	1075673854
	.long	1820504609
	.long	1074607411
	.long	866328203
	.long	1072932692
	.long	4050380138
	.long	1070823628
	.long	1285083134
	.long	1068462624
	.long	3773794448
	.long	1065665941
	.long	3992431453
	.long	1062729361
	.long	0
	.long	1075830784
	.long	0
	.long	1075843072
	.long	551
	.long	1074782208
	.long	2863310890
	.long	1073048234
	.long	4280866933
	.long	1070940159
	.long	584957920
	.long	1068573218
	.long	2832093524
	.long	1065790218
	.long	888085363
	.long	1062869909
	.long	806278057
	.long	1075930124
	.long	1223413582
	.long	1075937636
	.long	806274936
	.long	1074881548
	.long	1631187813
	.long	1073174320
	.long	1117996735
	.long	1071067152
	.long	3539687907
	.long	1068674086
	.long	1879685196
	.long	1065925553
	.long	1160881408
	.long	1063021332
	.long	3127183787
	.long	1076033929
	.long	1509844094
	.long	1076040818
	.long	3127187021
	.long	1074985353
	.long	2013156894
	.long	1073311896
	.long	2671582213
	.long	1071205559
	.long	1953547779
	.long	1068784147
	.long	1400782066
	.long	1066073442
	.long	3995115867
	.long	1063193683
	.long	1653937367
	.long	1076147076
	.long	1205288824
	.long	1076153393
	.long	1653935046
	.long	1075098500
	.long	1607033566
	.long	1073461996
	.long	3664268179
	.long	1071356421
	.long	2469699602
	.long	1068904227
	.long	1532116762
	.long	1066234156
	.long	2977079648
	.long	1063314464
	.long	390855727
	.long	1076270414
	.long	3048373099
	.long	1076276206
	.long	390853261
	.long	1075221838
	.long	2632821043
	.long	1073625747
	.long	549867025
	.long	1071520872
	.long	2471230975
	.long	1069035228
	.long	956813323
	.long	1066405683
	.long	3149969443
	.long	1063414189
	.long	1421870534
	.long	1076404869
	.long	798309360
	.long	1076410181
	.long	1421873362
	.long	1075356293
	.long	532213845
	.long	1073773102
	.long	3779846496
	.long	1071672408
	.long	2302200646
	.long	1069178134
	.long	3166036895
	.long	1066501403
	.long	121344502
	.long	1063524534
	.long	441826002
	.long	1076551452
	.long	408939657
	.long	1076556323
	.long	441824999
	.long	1075502876
	.long	272623207
	.long	1073870530
	.long	3158485477
	.long	1071770130
	.long	1424774778
	.long	1069334019
	.long	1119737346
	.long	1066605572
	.long	2920460864
	.long	1063642371
	.long	2161993182
	.long	1076711263
	.long	957997871
	.long	1076715730
	.long	2161997168
	.long	1075662687
	.long	2070335460
	.long	1073976801
	.long	2830352426
	.long	1071876671
	.long	1964908623
	.long	1069504053
	.long	4115624093
	.long	1066719318
	.long	3794372293
	.long	1063773346
	.long	0
	.long	1076885504
	.long	0
	.long	1076888576
	.long	553
	.long	1075836928
	.long	2863310894
	.long	1074092714
	.long	4280820838
	.long	1071992831
	.long	1443864459
	.long	1069618517
	.long	88635046
	.long	1066843164
	.long	1252176869
	.long	1063913492
	.long	962703878
	.long	1076981517
	.long	1066987759
	.long	1076983395
	.long	962706711
	.long	1075932941
	.long	1422665967
	.long	1074219140
	.long	1225009594
	.long	1072119484
	.long	3437844028
	.long	1069719657
	.long	2037998457
	.long	1066978366
	.long	2730254678
	.long	1064068959
	.long	4131294139
	.long	1077085088
	.long	505733744
	.long	1077086811
	.long	4131292077
	.long	1076036512
	.long	674296617
	.long	1074357028
	.long	4100678826
	.long	1072257579
	.long	811946276
	.long	1069829968
	.long	2503873247
	.long	1067125487
	.long	3711859275
	.long	1064234493
	.long	948823251
	.long	1077198021
	.long	1910402939
	.long	1077199600
	.long	948823671
	.long	1076149445
	.long	3978864649
	.long	1074507413
	.long	1245700928
	.long	1072408156
	.long	3113056604
	.long	1069950276
	.long	2574888035
	.long	1067286207
	.long	4135901481
	.long	1064362011
	.long	1387424741
	.long	1077321162
	.long	2051804084
	.long	1077322610
	.long	1387425898
	.long	1076272586
	.long	1304084744
	.long	1074671427
	.long	1816822804
	.long	1072572344
	.long	1865285038
	.long	1070081487
	.long	429722136
	.long	1067455874
	.long	2577837837
	.long	1064461937
	.long	1188035094
	.long	1077455437
	.long	1032144801
	.long	1077456765
	.long	1188036279
	.long	1076406861
	.long	688092076
	.long	1074820350
	.long	3637955290
	.long	1072722312
	.long	3797092908
	.long	1070224585
	.long	2283066980
	.long	1067551364
	.long	3220833869
	.long	1064570645
	.long	3113848182
	.long	1077601854
	.long	2031884771
	.long	1077603072
	.long	3113852775
	.long	1076553278
	.long	1354609713
	.long	1074917888
	.long	2028801321
	.long	1072819924
	.long	3224671877
	.long	1070380646
	.long	2229652729
	.long	1067655555
	.long	687475109
	.long	1064691259
	.long	2247365853
	.long	1077761514
	.long	872625202
	.long	1077762631
	.long	2247366044
	.long	1076712938
	.long	3445052219
	.long	1075024260
	.long	1487959742
	.long	1072926364
	.long	4081984271
	.long	1070550842
	.long	3194887604
	.long	1067769012
	.long	1428358124
	.long	1064818848
	.long	0
	.long	1077935616
	.long	0
	.long	1077936384
	.long	553
	.long	1076887040
	.long	2863310893
	.long	1075140266
	.long	4280815405
	.long	1073042431
	.long	584898172
	.long	1070666274
	.long	1481010731
	.long	1067892832
	.long	3449978604
	.long	1064960819
	.long	2075552158
	.long	1078030797
	.long	4249106777
	.long	1078031266
	.long	2075550082
	.long	1076982221
	.long	1370477227
	.long	1075266777
	.long	1360858426
	.long	1073168999
	.long	3322122933
	.long	1070767482
	.long	894790874
	.long	1068027754
	.long	313386866
	.long	1065112862
	.long	3308579903
	.long	1078134310
	.long	1328447981
	.long	1078134741
	.long	3308576517
	.long	1077085734
	.long	339568777
	.long	1075404743
	.long	162985674
	.long	1073307017
	.long	1786761066
	.long	1070877855
	.long	3853563277
	.long	1068174930
	.long	630026056
	.long	1065280361
	.long	1846286546
	.long	1078247189
	.long	1012939644
	.long	1078247584
	.long	1846287652
	.long	1077198613
	.long	1350590555
	.long	1075555200
	.long	3862266317
	.long	1073457521
	.long	146086404
	.long	1070998221
	.long	1969531125
	.long	1068335652
	.long	2173124931
	.long	1065410138
	.long	1636566995
	.long	1078370281
	.long	1802661831
	.long	1078370643
	.long	1636564793
	.long	1077321705
	.long	971872480
	.long	1075719279
	.long	2206286067
	.long	1073621644
	.long	1139809035
	.long	1071129484
	.long	1369988394
	.long	1068504771
	.long	3343567141
	.long	1065509148
	.long	1129576234
	.long	1078504511
	.long	1090603661
	.long	1078504843
	.long	1129577008
	.long	1077455935
	.long	727064831
	.long	1075868594
	.long	3602485534
	.long	1073771220
	.long	3003617391
	.long	1071272630
	.long	3101442371
	.long	1068600286
	.long	255129745
	.long	1065618798
	.long	1634370080
	.long	1078650887
	.long	3511362876
	.long	1078651191
	.long	1634367542
	.long	1077602311
	.long	3772549016
	.long	1075966159
	.long	3966594334
	.long	1073868804
	.long	3973919909
	.long	1071428735
	.long	1360835337
	.long	1068704318
	.long	3229532341
	.long	1065736828
	.long	1194967197
	.long	1078810509
	.long	1925023859
	.long	1078810788
	.long	1194964307
	.long	1077761933
	.long	2714984140
	.long	1076072557
	.long	2244282950
	.long	1073975219
	.long	1592981707
	.long	1071598972
	.long	4054940274
	.long	1068817826
	.long	2174666341
	.long	1065866173
	.long	0
	.long	1078984576
	.long	0
	.long	1078984768
	.long	554
	.long	1077936000
	.long	2863310896
	.long	1076188586
	.long	4280801865
	.long	1074091263
	.long	1443817676
	.long	1071714645
	.long	1967595988
	.long	1068941681
	.long	489391739
	.long	1066009084
	.long	2353764228
	.long	1079079549
	.long	3970894707
	.long	1079079666
	.long	2353760925
	.long	1078030973
	.long	2431184657
	.long	1076315118
	.long	321072719
	.long	1074217810
	.long	4179967377
	.long	1071815870
	.long	678236900
	.long	1069076533
	.long	2141802380
	.long	1066161039
	.long	955417696
	.long	1079183048
	.long	3681610188
	.long	1079183155
	.long	955413979
	.long	1078134472
	.long	3477118682
	.long	1076453103
	.long	1326046035
	.long	1074355808
	.long	863299713
	.long	1071926259
	.long	3117225129
	.long	1069223723
	.long	2396986094
	.long	1066328644
	.long	2070652370
	.long	1079295913
	.long	788573821
	.long	1079296012
	.long	2070651514
	.long	1078247337
	.long	3914723815
	.long	1076603578
	.long	1331571755
	.long	1074506295
	.long	868203065
	.long	1072046639
	.long	431241892
	.long	1069384363
	.long	2246818571
	.long	1066457734
	.long	625110734
	.long	1079418993
	.long	2814118091
	.long	1079419083
	.long	625111957
	.long	1078370417
	.long	888847596
	.long	1076767674
	.long	2230927568
	.long	1074670401
	.long	1532605793
	.long	1072177915
	.long	532982727
	.long	1069553510
	.long	3846699990
	.long	1066558540
	.long	3262445167
	.long	1079553211
	.long	3252702024
	.long	1079553294
	.long	3262445839
	.long	1078504635
	.long	736808022
	.long	1076917087
	.long	3593608959
	.long	1074819879
	.long	3878877473
	.long	1072321073
	.long	1262415275
	.long	1069648949
	.long	1893888386
	.long	1066667268
	.long	2338242378
	.long	1079699577
	.long	2807490577
	.long	1079699653
	.long	2338242322
	.long	1078651001
	.long	3303312501
	.long	1077014659
	.long	119713135
	.long	1074917457
	.long	253619889
	.long	1072477190
	.long	2219021147
	.long	1069753023
	.long	4162054000
	.long	1066786194
	.long	4153093004
	.long	1079859189
	.long	3261865345
	.long	1079859259
	.long	4153097873
	.long	1078810613
	.long	3606246712
	.long	1077121063
	.long	1286900661
	.long	1075023865
	.long	1838975761
	.long	1072647436
	.long	2091142296
	.long	1069866627
	.long	3637235953
	.long	1066917327
	.long	0
	.long	1080033248
	.long	0
	.long	1080033296
	.long	554
	.long	1078984672
	.long	2863310896
	.long	1077237098
	.long	4280801526
	.long	1075139903
	.long	584824499
	.long	1072763170
	.long	3128350829
	.long	1069990325
	.long	388973629
	.long	1067057582
	.long	2423317245
	.long	1080128169
	.long	3901341689
	.long	1080128198
	.long	2423317900
	.long	1079079593
	.long	3770125126
	.long	1077363635
	.long	3209627449
	.long	1075266444
	.long	3988311033
	.long	1072864399
	.long	3848672452
	.long	1070125324
	.long	3118379075
	.long	1067211445
	.long	2514610792
	.long	1080231664
	.long	2122417091
	.long	1080231691
	.long	2514609124
	.long	1079183088
	.long	4261529832
	.long	1077501625
	.long	506722372
	.long	1075404438
	.long	242677220
	.long	1072974792
	.long	1687936003
	.long	1070272436
	.long	3170278816
	.long	1067378880
	.long	2126743826
	.long	1080344526
	.long	732482365
	.long	1080344551
	.long	2126742480
	.long	1079295950
	.long	2408279879
	.long	1077652105
	.long	2846369583
	.long	1075554920
	.long	3102695869
	.long	1073095175
	.long	3406344199
	.long	1070432972
	.long	334424787
	.long	1067506258
	.long	372246669
	.long	1080467603
	.long	3066982156
	.long	1080467625
	.long	372246615
	.long	1079419027
	.long	4089312338
	.long	1077816204
	.long	3347213243
	.long	1075719022
	.long	666900058
	.long	1073226455
	.long	2349197098
	.long	1070602085
	.long	939743989
	.long	1067607030
	.long	3795662400
	.long	1080601818
	.long	2719484790
	.long	1080601839
	.long	3795665178
	.long	1079553242
	.long	1812997480
	.long	1077965642
	.long	2499477572
	.long	1075868476
	.long	2634225501
	.long	1073369616
	.long	682305439
	.long	1070697588
	.long	2631503722
	.long	1067716684
	.long	366726804
	.long	1080748182
	.long	484038853
	.long	1080748201
	.long	366731634
	.long	1079699606
	.long	3186023857
	.long	1078063216
	.long	195363368
	.long	1075966052
	.long	1858310082
	.long	1073525735
	.long	1465317937
	.long	1070801714
	.long	2313583288
	.long	1067836510
	.long	597657160
	.long	1080907792
	.long	2522333893
	.long	1080907809
	.long	597661836
	.long	1079859216
	.long	1681570062
	.long	1078169622
	.long	3213224383
	.long	1076072458
	.long	2197034374
	.long	1073695984
	.long	1547434940
	.long	1070915218
	.long	1991814041
	.long	1067965873
	.long	0
	.long	1081081848
	.long	0
	.long	1081081860
	.long	554
	.long	1080033272
	.long	2863310896
	.long	1078285658
	.long	4280801441
	.long	1076188495
	.long	1443818027
	.long	1073811733
	.long	3418544190
	.long	1071038918
	.long	2511418032
	.long	1068106138
	.long	2440705499
	.long	1081176756
	.long	3883953434
	.long	1081176763
	.long	2440709276
	.long	1080128180
	.long	883652058
	.long	1078412197
	.long	1747917283
	.long	1076315035
	.long	422689944
	.long	1073912964
	.long	1456304861
	.long	1071174037
	.long	3459587548
	.long	1068261828
	.long	2904409066
	.long	1081280250
	.long	1732618817
	.long	1081280257
	.long	2904407911
	.long	1080231674
	.long	2310142585
	.long	1078550188
	.long	2449356831
	.long	1076453027
	.long	1254527275
	.long	1074023357
	.long	2612104548
	.long	1071321046
	.long	169532989
	.long	1068427488
	.long	3214508514
	.long	1081393111
	.long	3939684973
	.long	1081393117
	.long	3214507046
	.long	1080344535
	.long	957927075
	.long	1078700669
	.long	3831387
	.long	1076603509
	.long	3661221253
	.long	1074143741
	.long	1067371691
	.long	1071481557
	.long	971005243
	.long	1068554821
	.long	2456514301
	.long	1081516187
	.long	982714525
	.long	1081516193
	.long	2456511796
	.long	1080467611
	.long	2741921202
	.long	1078864769
	.long	441406119
	.long	1076767610
	.long	840224812
	.long	1074275022
	.long	741498441
	.long	1071650620
	.long	3872266127
	.long	1068654717
	.long	2855224885
	.long	1081650402
	.long	3659922307
	.long	1081650407
	.long	2855221793
	.long	1080601826
	.long	1008277094
	.long	1079014213
	.long	3354222170
	.long	1076917057
	.long	2138587760
	.long	1074418184
	.long	621363220
	.long	1071746056
	.long	3621085838
	.long	1068763446
	.long	947589735
	.long	1081796765
	.long	4198143219
	.long	1081796769
	.long	947591521
	.long	1080748189
	.long	4230426231
	.long	1079111787
	.long	3471872690
	.long	1077014632
	.long	3926297451
	.long	1074574303
	.long	2245132281
	.long	1071850236
	.long	2649906091
	.long	1068884171
	.long	2930023671
	.long	1081956374
	.long	189967382
	.long	1081956379
	.long	2930028299
	.long	1080907798
	.long	126659077
	.long	1079218194
	.long	473576796
	.long	1077121039
	.long	2286477392
	.long	1074744553
	.long	372390273
	.long	1071963798
	.long	236850944
	.long	1069014442
	.long	0
	.long	1082130430
	.long	0
	.long	1082130433
	.long	554
	.long	1081081854
	.long	2863310896
	.long	1079334230
	.long	4280801420
	.long	1077237075
	.long	584824587
	.long	1074860306
	.long	269862407
	.long	1072087499
	.long	3041954663
	.long	1069154709
	.long	1371310739
	.long	1082225335
	.long	658380899
	.long	1082225337
	.long	1371311032
	.long	1081176759
	.long	2309489262
	.long	1079460769
	.long	381466140
	.long	1077363615
	.long	1104482025
	.long	1074961537
	.long	1997887223
	.long	1072222482
	.long	2422405383
	.long	1069308542
	.long	854374986
	.long	1082328829
	.long	3782652895
	.long	1082328830
	.long	854378224
	.long	1081280253
	.long	748601306
	.long	1079598761
	.long	3936032960
	.long	1077501606
	.long	1801490377
	.long	1075071930
	.long	698907663
	.long	1072369796
	.long	3117308375
	.long	1069479539
	.long	265224214
	.long	1082441690
	.long	2594001977
	.long	1082441691
	.long	265222715
	.long	1081393114
	.long	595338870
	.long	1079749242
	.long	366950843
	.long	1077652088
	.long	1653466738
	.long	1075192315
	.long	1417893004
	.long	1072530135
	.long	3237255031
	.long	1069603393
	.long	2977581209
	.long	1082564765
	.long	461647618
	.long	1082564767
	.long	2977578091
	.long	1081516189
	.long	3478796063
	.long	1079913342
	.long	4009927721
	.long	1077816188
	.long	90082880
	.long	1075323596
	.long	3526213041
	.long	1072699185
	.long	4038648974
	.long	1069702494
	.long	2620115506
	.long	1082698980
	.long	3895031686
	.long	1082698981
	.long	2620113079
	.long	1081650404
	.long	4028327917
	.long	1080062787
	.long	328500247
	.long	1077965635
	.long	1811589807
	.long	1075466758
	.long	1697998849
	.long	1072794646
	.long	4063787973
	.long	1069812051
	.long	19063644
	.long	1082845343
	.long	831702015
	.long	1082845344
	.long	19062537
	.long	1081796767
	.long	3417776359
	.long	1080160362
	.long	14209235
	.long	1078063210
	.long	444716692
	.long	1075622878
	.long	3564927921
	.long	1072898757
	.long	1585452687
	.long	1069931844
	.long	1365631651
	.long	1083004952
	.long	1754359403
	.long	1083004953
	.long	1365634135
	.long	1081956376
	.long	4032886789
	.long	1080266768
	.long	880583233
	.long	1078169616
	.long	3772380090
	.long	1075793127
	.long	3350968953
	.long	1073012333
	.long	1825857751
	.long	1070062149
	.long	2147483648
	.long	1083179007
	.long	1073741824
	.long	1083179008
	.long	2147484202
	.long	1082130431
	.long	2863310896
	.long	1080382805
	.long	4280801414
	.long	1078285652
	.long	1443818045
	.long	1075908881
	.long	556447737
	.long	1073136076
	.long	2101070372
	.long	1070203284
	.long	30220225
	.long	1083273912
	.long	1999471413
	.long	1083273912
	.long	30219647
	.long	1082225336
	.long	2665954955
	.long	1080509344
	.long	39853355
	.long	1078412192
	.long	2255272812
	.long	1076010112
	.long	3207010299
	.long	1073271025
	.long	2006307835
	.long	1070357037
	.long	3563091938
	.long	1083377405
	.long	1073935943
	.long	1083377406
	.long	3563096274
	.long	1082328829
	.long	1431945023
	.long	1080647336
	.long	3233966257
	.long	1078550183
	.long	3198848380
	.long	1076120505
	.long	2298886726
	.long	1073418415
	.long	1364133258
	.long	1070528215
	.long	2749128611
	.long	1083490266
	.long	110097580
	.long	1083490267
	.long	2749127104
	.long	1082441690
	.long	1578433641
	.long	1080797817
	.long	3678962269
	.long	1078700664
	.long	3299054964
	.long	1076240890
	.long	3583777588
	.long	1073578711
	.long	3308485264
	.long	1070651968
	.long	960364288
	.long	1083613342
	.long	2478864539
	.long	1083613342
	.long	960361017
	.long	1082564766
	.long	441795701
	.long	1080961918
	.long	2754568384
	.long	1078864765
	.long	1956566063
	.long	1076372171
	.long	1035776069
	.long	1073747759
	.long	2596065895
	.long	1070751063
	.long	413854513
	.long	1083747557
	.long	1806325382
	.long	1083747557
	.long	413854384
	.long	1082698981
	.long	2635868811
	.long	1081111363
	.long	1701379949
	.long	1079014211
	.long	3487595464
	.long	1076515333
	.long	807696162
	.long	1073843267
	.long	3490667642
	.long	1070861501
	.long	1934415769
	.long	1083893919
	.long	3211317186
	.long	1083893919
	.long	1934416071
	.long	1082845343
	.long	2140874317
	.long	1081208938
	.long	1279097463
	.long	1079111786
	.long	2685929170
	.long	1076671453
	.long	1730921990
	.long	1073947361
	.long	2936264088
	.long	1070980484
	.long	3122017294
	.long	1084053528
	.long	4292941056
	.long	1084053528
	.long	3122019241
	.long	1083004952
	.long	2861963261
	.long	1081315344
	.long	2056088848
	.long	1079218192
	.long	1903168657
	.long	1076841703
	.long	2883380270
	.long	1074060899
	.long	4210992368
	.long	1071110699
	.long	3758096384
	.long	1084227583
	.long	268435456
	.long	1084227584
	.long	3758096938
	.long	1083179007
	.long	1789569072
	.long	1081431381
	.long	1059575941
	.long	1079334229
	.long	584824589
	.long	1076957457
	.long	1701826592
	.long	1074184652
	.long	791976915
	.long	1071251860
	.long	768689420
	.long	1084322488
	.long	1261002217
	.long	1084322488
	.long	768692889
	.long	1083273912
	.long	1681351342
	.long	1081557920
	.long	955467668
	.long	1079460768
	.long	1063106328
	.long	1077058688
	.long	2438888433
	.long	1074319758
	.long	368525864
	.long	1071407523
	.long	4240271176
	.long	1084425981
	.long	396756705
	.long	1084425982
	.long	4240275786
	.long	1083377405
	.long	529039124
	.long	1081695912
	.long	4132203586
	.long	1079598759
	.long	2474549528
	.long	1077169081
	.long	1486666934
	.long	1074467002
	.long	2869201860
	.long	1071576815
	.long	3370104710
	.long	1084538842
	.long	3784088776
	.long	1084538842
	.long	3370105334
	.long	1083490266
	.long	750489193
	.long	1081846393
	.long	175620448
	.long	1079749241
	.long	2246720186
	.long	1077289466
	.long	2152491105
	.long	1074627370
	.long	3465128389
	.long	1071701411
	.long	1529801881
	.long	1084661918
	.long	1909426943
	.long	1084661918
	.long	1529804969
	.long	1083613342
	.long	3977583942
	.long	1082010493
	.long	3405380863
	.long	1079913341
	.long	179822948
	.long	1077420747
	.long	1506659931
	.long	1074796458
	.long	2985677691
	.long	1071802238
	.long	936031089
	.long	1084796133
	.long	1284148807
	.long	1084796133
	.long	936029403
	.long	1083747557
	.long	2287739003
	.long	1082159939
	.long	2062770294
	.long	1080062787
	.long	3315945067
	.long	1077563909
	.long	705469102
	.long	1074891813
	.long	2396211301
	.long	1071909237
	.long	2413253800
	.long	1084942495
	.long	2732479154
	.long	1084942495
	.long	2413256587
	.long	1083893919
	.long	1821660648
	.long	1082257514
	.long	1577133874
	.long	1080160362
	.long	1782509562
	.long	1077720029
	.long	2398915295
	.long	1074995985
	.long	66228759
	.long	1072029944
	.long	3561113705
	.long	1085102104
	.long	3853844646
	.long	1085102104
	.long	3561113386
	.long	1084053528
	.long	2569220542
	.long	1082363920
	.long	2368138716
	.long	1080266768
	.long	1825628382
	.long	1077890279
	.long	3925963295
	.long	1075109431
	.long	4047704299
	.long	1072158402
	.long	4160749568
	.long	1085276159
	.long	67108864
	.long	1085276160
	.long	4160750122
	.long	1084227583
	.long	1521133616
	.long	1082479957
	.long	1328011397
	.long	1080382805
	.long	370076228
	.long	1078006033
	.long	1988166656
	.long	1075233228
	.long	464592383
	.long	1072300436
	.long	953306719
	.long	1085371064
	.long	1076384918
	.long	1085371064
	.long	953309067
	.long	1084322488
	.long	1435195934
	.long	1082606496
	.long	1220742541
	.long	1080509344
	.long	874797354
	.long	1078107264
	.long	1067557893
	.long	1075368291
	.long	2716721913
	.long	1072455995
	.long	114598690
	.long	1085474558
	.long	227461896
	.long	1085474558
	.long	114599104
	.long	1084425982
	.long	303290865
	.long	1082744488
	.long	134513846
	.long	1080647336
	.long	2699514174
	.long	1078217657
	.long	275778966
	.long	1075515416
	.long	1264773403
	.long	1072623468
	.long	3525348735
	.long	1085587418
	.long	3628844752
	.long	1085587418
	.long	3525347759
	.long	1084538842
	.long	543473007
	.long	1082894969
	.long	409898107
	.long	1080797817
	.long	2466993696
	.long	1078338042
	.long	2762938322
	.long	1075675884
	.long	595062760
	.long	1072749145
	.long	1672161280
	.long	1085710494
	.long	1767067546
	.long	1085710494
	.long	1672159560
	.long	1084661918
	.long	3787724531
	.long	1083059069
	.long	3677185676
	.long	1080961917
	.long	811952329
	.long	1078469323
	.long	1535376778
	.long	1075844941
	.long	2275213375
	.long	1072849055
	.long	1066575233
	.long	1085844709
	.long	1153604663
	.long	1085844709
	.long	1066573157
	.long	1084796133
	.long	2200709744
	.long	1083208515
	.long	2153127016
	.long	1081111363
	.long	3179780105
	.long	1078612485
	.long	2723537752
	.long	1075940381
	.long	1446496431
	.long	1072957795
	.long	2532963308
	.long	1085991071
	.long	2612769647
	.long	1085991071
	.long	2532964584
	.long	1084942495
	.long	1741842196
	.long	1083306090
	.long	1669822531
	.long	1081208938
	.long	2039902703
	.long	1078768605
	.long	1508680971
	.long	1076044532
	.long	3045798495
	.long	1073077681
	.long	3670887808
	.long	1086150680
	.long	3744070544
	.long	1086150680
	.long	3670884790
	.long	1085102104
	.long	2496026219
	.long	1083412496
	.long	2464333784
	.long	1081315344
	.long	2102719453
	.long	1078938855
	.long	3094728764
	.long	1076157955
	.long	4014581870
	.long	1073206085
	.long	4261412864
	.long	1086324735
	.long	16777216
	.long	1086324736
	.long	4261413418
	.long	1085276159
	.long	1454024752
	.long	1083528533
	.long	1395120260
	.long	1081431381
	.long	316389129
	.long	1079054609
	.long	2059770273
	.long	1076281804
	.long	383071603
	.long	1073349012
	.long	999461044
	.long	1086419640
	.long	1030230594
	.long	1086419640
	.long	999460980
	.long	1085371064
	.long	1373633409
	.long	1083655072
	.long	1323408188
	.long	1081557920
	.long	1217459964
	.long	1079155840
	.long	4117422823
	.long	1076416773
	.long	2277811950
	.long	1073502812
	.long	156922392
	.long	1086523134
	.long	185138193
	.long	1086523134
	.long	156926022
	.long	1085474558
	.long	246881981
	.long	1083793064
	.long	136102831
	.long	1081695912
	.long	2256207783
	.long	1079266233
	.long	2193094862
	.long	1076564116
	.long	4088070846
	.long	1073673877
	.long	3564159741
	.long	1086635994
	.long	3590033745
	.long	1086635994
	.long	3564160497
	.long	1085587418
	.long	491749028
	.long	1083943545
	.long	432114504
	.long	1081846393
	.long	2038862927
	.long	1079386618
	.long	2813288435
	.long	1076724527
	.long	2065802453
	.long	1073798569
	.long	1707751129
	.long	1086759070
	.long	1731477695
	.long	1086759070
	.long	1707754605
	.long	1085710494
	.long	3740317935
	.long	1084107645
	.long	3636029094
	.long	1082010493
	.long	4281998316
	.long	1079517898
	.long	3813677248
	.long	1076893617
	.long	2344318102
	.long	1073899408
	.long	1099211269
	.long	1086893285
	.long	1120968627
	.long	1086893285
	.long	1099209096
	.long	1085844709
	.long	2178952431
	.long	1084257091
	.long	2175710106
	.long	1082159939
	.long	3145652441
	.long	1079661061
	.long	3297293607
	.long	1076988955
	.long	52434176
	.long	1074006367
	.long	2562890685
	.long	1087039647
	.long	2582842270
	.long	1087039647
	.long	2562891583
	.long	1085991071
	.long	1721890778
	.long	1084354666
	.long	1692997742
	.long	1082257514
	.long	2010900862
	.long	1079817181
	.long	177733124
	.long	1077093101
	.long	1007048313
	.long	1074126240
	.long	3698331333
	.long	1087199256
	.long	3716627017
	.long	1087199256
	.long	3698334037
	.long	1086150680
	.long	2477753566
	.long	1084461072
	.long	2433843884
	.long	1082363920
	.long	1282665025
	.long	1079987431
	.long	1763731007
	.long	1077206642
	.long	2805159360
	.long	1074256462
	.long	4286578688
	.long	1087373311
	.long	4194304
	.long	1087373312
	.long	4286579242
	.long	1086324735
	.long	1437247536
	.long	1084577109
	.long	1411897476
	.long	1082479957
	.long	302967356
	.long	1080103185
	.long	2077666527
	.long	1077330380
	.long	362626136
	.long	1074397588
	.long	1010999625
	.long	1087468216
	.long	1018692012
	.long	1087468216
	.long	1011001090
	.long	1086419640
	.long	1358272845
	.long	1084703648
	.long	1312721582
	.long	1082606496
	.long	819932153
	.long	1080204416
	.long	482660068
	.long	1077465409
	.long	3200054132
	.long	1074553066
	.long	167503318
	.long	1087571710
	.long	174557269
	.long	1087571710
	.long	167503487
	.long	1086523134
	.long	232726010
	.long	1084841640
	.long	209230477
	.long	1082744488
	.long	3018553297
	.long	1080314809
	.long	2599950208
	.long	1077612558
	.long	3730748960
	.long	1074719059
	.long	3573862493
	.long	1087684570
	.long	3580330995
	.long	1087684570
	.long	3573859418
	.long	1086635994
	.long	478764291
	.long	1084992121
	.long	510374640
	.long	1082894969
	.long	2804812444
	.long	1080435194
	.long	1956633881
	.long	1077772955
	.long	3531840445
	.long	1074845431
	.long	1716648592
	.long	1087807646
	.long	1722580234
	.long	1087807646
	.long	1716646969
	.long	1086759070
	.long	3728408029
	.long	1085156221
	.long	3734847734
	.long	1083059069
	.long	763760032
	.long	1080566475
	.long	1038386958
	.long	1077942095
	.long	4200661441
	.long	1074946211
	.long	1107370278
	.long	1087941861
	.long	1112809618
	.long	1087941861
	.long	1107368080
	.long	1086893285
	.long	2173513100
	.long	1085305667
	.long	2181365015
	.long	1083208515
	.long	3137255856
	.long	1080709637
	.long	1189376933
	.long	1078037531
	.long	2450715105
	.long	1075054941
	.long	2570372529
	.long	1088088223
	.long	2575360425
	.long	1088088223
	.long	2570375465
	.long	1087039647
	.long	1716914763
	.long	1085403242
	.long	1680608944
	.long	1083306090
	.long	1613763855
	.long	1080865757
	.long	2010613428
	.long	1078141716
	.long	3755500550
	.long	1075175678
	.long	3705192215
	.long	1088247832
	.long	3709766136
	.long	1088247832
	.long	3705189953
	.long	1087199256
	.long	2473169065
	.long	1085509648
	.long	2480757032
	.long	1083412496
	.long	1686687141
	.long	1081036007
	.long	1515024379
	.long	1078255122
	.long	3444278726
	.long	1075304041
	.long	4292870144
	.long	1088421887
	.long	1048576
	.long	1088421888
	.long	4292870698
	.long	1087373311
	.long	1433053232
	.long	1085625685
	.long	1416091780
	.long	1083528533
	.long	299611913
	.long	1081151761
	.long	2082140591
	.long	1078378956
	.long	357505597
	.long	1075446164
	.long	1013884270
	.long	1088516792
	.long	1015807366
	.long	1088516792
	.long	1013888250
	.long	1087468216
	.long	1354449993
	.long	1085752224
	.long	1273678638
	.long	1083655072
	.long	424013800
	.long	1081252992
	.long	1826968645
	.long	1078514082
	.long	481040262
	.long	1075603412
	.long	170148549
	.long	1088620286
	.long	171912036
	.long	1088620286
	.long	170152118
	.long	1087571710
	.long	229247162
	.long	1085890216
	.long	154775900
	.long	1083793064
	.long	2242502590
	.long	1081363385
	.long	695859775
	.long	1078661266
	.long	2809642103
	.long	1075771024
	.long	3576288180
	.long	1088733146
	.long	3577905305
	.long	1088733146
	.long	3576292676
	.long	1087684570
	.long	475600029
	.long	1086040697
	.long	384509323
	.long	1083943545
	.long	1623790536
	.long	1081483770
	.long	1541299999
	.long	1078821824
	.long	1479151397
	.long	1075896662
	.long	1718872957
	.long	1088856222
	.long	1720355867
	.long	1088856222
	.long	1718876457
	.long	1087807646
	.long	3725488808
	.long	1086204797
	.long	3650450699
	.long	1084107645
	.long	4269987770
	.long	1081615050
	.long	1507326798
	.long	1078990770
	.long	2391922729
	.long	1075996561
	.long	1109410030
	.long	1088990437
	.long	1110769865
	.long	1088990437
	.long	1109409959
	.long	1087941861
	.long	2172165110
	.long	1086354243
	.long	2164587006
	.long	1084257091
	.long	2745112118
	.long	1081758213
	.long	1858126594
	.long	1079086148
	.long	2734186292
	.long	1076104384
	.long	2572242990
	.long	1089136799
	.long	2573489964
	.long	1089136799
	.long	2572246435
	.long	1088088223
	.long	1715667561
	.long	1086451818
	.long	1677517834
	.long	1084354666
	.long	1607987786
	.long	1081914333
	.long	1325871784
	.long	1079190302
	.long	62862654
	.long	1076224278
	.long	3706907435
	.long	1089296408
	.long	3708050915
	.long	1089296408
	.long	3706908196
	.long	1088247832
	.long	2472037028
	.long	1086558224
	.long	2456126208
	.long	1084461072
	.long	1288248598
	.long	1082084583
	.long	3567360497
	.long	1079303756
	.long	2429673483
	.long	1076353525
	.long	4294443008
	.long	1089470463
	.long	262144
	.long	1089470464
	.long	4294443562
	.long	1088421887
	.long	1432004656
	.long	1086674261
	.long	1417140356
	.long	1084577109
	.long	298773052
	.long	1082200337
	.long	2083259107
	.long	1079427532
	.long	356234643
	.long	1076494740
	.long	1014605432
	.long	1089565368
	.long	1015086207
	.long	1089565368
	.long	1014603643
	.long	1088516792
	.long	1353416848
	.long	1086800800
	.long	1373019593
	.long	1084703648
	.long	1588164491
	.long	1082301568
	.long	911338360
	.long	1079562435
	.long	2425809233
	.long	1076646843
	.long	170809857
	.long	1089668862
	.long	171250729
	.long	1089668862
	.long	170812143
	.long	1088620286
	.long	228340982
	.long	1086938792
	.long	177539634
	.long	1084841640
	.long	2625295036
	.long	1082411961
	.long	2192656029
	.long	1079709792
	.long	1173511898
	.long	1076817944
	.long	3576894602
	.long	1089781722
	.long	3577298883
	.long	1089781722
	.long	3576898859
	.long	1088733146
	.long	474791683
	.long	1087089273
	.long	389389924
	.long	1084992121
	.long	1624864514
	.long	1082532346
	.long	535168506
	.long	1079870391
	.long	2644939051
	.long	1076945227
	.long	1719429049
	.long	1089904798
	.long	1719799777
	.long	1089904798
	.long	1719427432
	.long	1088856222
	.long	3724700747
	.long	1087253373
	.long	3738453135
	.long	1085156221
	.long	760763076
	.long	1082663627
	.long	1535543495
	.long	1080039247
	.long	929725033
	.long	1077043364
	.long	1109919968
	.long	1090039013
	.long	1110259927
	.long	1090039013
	.long	1109920428
	.long	1088990437
	.long	2171824913
	.long	1087402819
	.long	2160401639
	.long	1085305667
	.long	2740644634
	.long	1082806789
	.long	2995207291
	.long	1080134734
	.long	3113481135
	.long	1077152984
	.long	2572710606
	.long	1090185375
	.long	2573022350
	.long	1090185375
	.long	2572707781
	.long	1089136799
	.long	1715333027
	.long	1087500394
	.long	1731289815
	.long	1085403242
	.long	2402558359
	.long	1082962909
	.long	61134361
	.long	1080238757
	.long	3566035781
	.long	1077271027
	.long	3707336240
	.long	1090344984
	.long	3707622110
	.long	1090344984
	.long	3707337757
	.long	1089296408
	.long	2471750824
	.long	1087606800
	.long	2449965456
	.long	1085509648
	.long	1281977735
	.long	1083133159
	.long	1967594370
	.long	1080352347
	.long	1861523814
	.long	1077402136
	.long	4294836224
	.long	1090519039
	.long	65536
	.long	1090519040
	.long	4294836778
	.long	1089470463
	.long	1431742512
	.long	1087722837
	.long	1417402500
	.long	1085625685
	.long	298563337
	.long	1083248913
	.long	2083538736
	.long	1080476108
	.long	355907724
	.long	1077543316
	.long	1014785722
	.long	1090613944
	.long	1014905916
	.long	1090613944
	.long	1014786756
	.long	1089565368
	.long	1353199530
	.long	1087849376
	.long	1325118339
	.long	1085752224
	.long	1192773544
	.long	1083350144
	.long	1897932215
	.long	1080611120
	.long	1232933086
	.long	1077697217
	.long	170975184
	.long	1090717438
	.long	171085402
	.long	1090717438
	.long	170977150
	.long	1089668862
	.long	228120836
	.long	1087987368
	.long	183212298
	.long	1085890216
	.long	2627424704
	.long	1083460537
	.long	627049929
	.long	1080758356
	.long	2294847832
	.long	1077866491
	.long	3577046208
	.long	1090830298
	.long	3577147278
	.long	1090830298
	.long	3577046140
	.long	1089781722
	.long	474567805
	.long	1088137849
	.long	463346570
	.long	1086040697
	.long	2031347468
	.long	1083580922
	.long	3363044532
	.long	1080918799
	.long	3187696535
	.long	1077992835
	.long	1719568072
	.long	1090953374
	.long	1719660754
	.long	1090953374
	.long	1719565176
	.long	1089904798
	.long	3724516519
	.long	1088301949
	.long	3760447655
	.long	1086204797
	.long	770323340
	.long	1083712203
	.long	2650944499
	.long	1081087798
	.long	2345789816
	.long	1078091881
	.long	1110047453
	.long	1091087589
	.long	1110132443
	.long	1091087589
	.long	1110043781
	.long	1090039013
	.long	2171728970
	.long	1088451395
	.long	2195717454
	.long	1086354243
	.long	3145644424
	.long	1083855365
	.long	3277819497
	.long	1081183230
	.long	3500208489
	.long	1078200601
	.long	2572827509
	.long	1091233951
	.long	2572905445
	.long	1091233951
	.long	2572831646
	.long	1090185375
	.long	1715277573
	.long	1088548970
	.long	1672008496
	.long	1086451818
	.long	1602169577
	.long	1084011485
	.long	2969504976
	.long	1081287467
	.long	2627268862
	.long	1078321461
	.long	3707443441
	.long	1091393560
	.long	3707514908
	.long	1091393560
	.long	3707447279
	.long	1090344984
	.long	2471691110
	.long	1088655376
	.long	2430248758
	.long	1086558224
	.long	890643997
	.long	1084181735
	.long	1516543114
	.long	1081400968
	.long	3616768931
	.long	1078451587
	.long	4294934528
	.long	1091567615
	.long	16384
	.long	1091567616
	.long	4294935082
	.long	1090519039
	.long	1431676976
	.long	1088771413
	.long	1417468036
	.long	1086674261
	.long	298510908
	.long	1084297489
	.long	2083608643
	.long	1081524684
	.long	355835171
	.long	1078591892
	.long	1014830795
	.long	1091662520
	.long	1014860844
	.long	1091662520
	.long	1014828270
	.long	1090613944
	.long	1353117021
	.long	1088897952
	.long	1385867339
	.long	1086800800
	.long	1593441513
	.long	1084398720
	.long	3215006955
	.long	1081659558
	.long	1675787922
	.long	1078743928
	.long	171016516
	.long	1091766014
	.long	171044071
	.long	1091766014
	.long	171016269
	.long	1090717438
	.long	228042115
	.long	1089035944
	.long	221007845
	.long	1086938792
	.long	3017975541
	.long	1084509113
	.long	2208415334
	.long	1081806846
	.long	2754871653
	.long	1078913325
	.long	3577084109
	.long	1091878874
	.long	3577109376
	.long	1091878874
	.long	3577087225
	.long	1090830298
	.long	474540019
	.long	1089186425
	.long	409099237
	.long	1087089273
	.long	1633354712
	.long	1084629498
	.long	4211806470
	.long	1081967498
	.long	845776843
	.long	1079042327
	.long	1719602827
	.long	1092001950
	.long	1719625997
	.long	1092001950
	.long	1719606009
	.long	1090953374
	.long	3724515932
	.long	1089350525
	.long	3656844589
	.long	1087253373
	.long	4271591071
	.long	1084760778
	.long	871341381
	.long	1082136492
	.long	214436730
	.long	1079142275
	.long	1110079324
	.long	1092136165
	.long	1110100572
	.long	1092136165
	.long	1110076752
	.long	1091087589
	.long	2171707238
	.long	1089499971
	.long	2186357716
	.long	1087402819
	.long	3137095798
	.long	1084903941
	.long	214629270
	.long	1082231828
	.long	2959808010
	.long	1079249228
	.long	2572856735
	.long	1092282527
	.long	2572876219
	.long	1092282527
	.long	2572860481
	.long	1091233951
	.long	1715258267
	.long	1089597546
	.long	1675358587
	.long	1087500394
	.long	1604919815
	.long	1085060061
	.long	595696934
	.long	1082336036
	.long	624673770
	.long	1079370020
	.long	3707470242
	.long	1092442136
	.long	3707488109
	.long	1092442136
	.long	3707468263
	.long	1091393560
	.long	2471650253
	.long	1089703952
	.long	2479864341
	.long	1087606800
	.long	1682178719
	.long	1085230311
	.long	3531472556
	.long	1082449431
	.long	2611633522
	.long	1079498358
	.long	4294959104
	.long	1092616191
	.long	4096
	.long	1092616192
	.long	4294959658
	.long	1091567615
	.long	1431660592
	.long	1089819989
	.long	1417484420
	.long	1087722837
	.long	298497801
	.long	1085346065
	.long	2083626120
	.long	1082573260
	.long	355807856
	.long	1079640468
	.long	1014842063
	.long	1092711096
	.long	1014849575
	.long	1092711096
	.long	1014840780
	.long	1091662520
	.long	1353126459
	.long	1089946528
	.long	1364707661
	.long	1087849376
	.long	1210463930
	.long	1085447296
	.long	3372789207
	.long	1082708182
	.long	3852029519
	.long	1079794155
	.long	171026849
	.long	1092814590
	.long	171033738
	.long	1092814590
	.long	171026049
	.long	1091766014
	.long	228028830
	.long	1090084520
	.long	230450642
	.long	1087987368
	.long	3022136903
	.long	1085557689
	.long	525497389
	.long	1082855401
	.long	148126673
	.long	1079961851
	.long	3577093585
	.long	1092927450
	.long	3577099902
	.long	1092927450
	.long	3577091100
	.long	1091878874
	.long	474481216
	.long	1090235001
	.long	504620827
	.long	1088137849
	.long	2423209871
	.long	1085678074
	.long	1232450644
	.long	1083015858
	.long	77442837
	.long	1080089108
	.long	1719611516
	.long	1093050526
	.long	1719617308
	.long	1093050526
	.long	1719614085
	.long	1092001950
	.long	3724504891
	.long	1090399101
	.long	3667309025
	.long	1088301949
	.long	4276237595
	.long	1085809354
	.long	1482039118
	.long	1083185056
	.long	3967229838
	.long	1080190822
	.long	1110087291
	.long	1093184741
	.long	1110092603
	.long	1093184741
	.long	1110091391
	.long	1092136165
	.long	2171724537
	.long	1090548547
	.long	2129476069
	.long	1088451395
	.long	2339015690
	.long	1085952517
	.long	507750428
	.long	1083280533
	.long	815659794
	.long	1080299649
	.long	2572864042
	.long	1093331103
	.long	2572868913
	.long	1093331103
	.long	2572863425
	.long	1092282527
	.long	1715242544
	.long	1090646122
	.long	1712564357
	.long	1088548970
	.long	2011855921
	.long	1086108637
	.long	3152573735
	.long	1083384527
	.long	2153072651
	.long	1080417626
	.long	3707476942
	.long	1093490712
	.long	3707481409
	.long	1093490712
	.long	3707475642
	.long	1092442136
	.long	2471645489
	.long	1090752528
	.long	2474076499
	.long	1088655376
	.long	1676838604
	.long	1086278887
	.long	4157220213
	.long	1083498020
	.long	881611026
	.long	1080546966
	.long	4294965248
	.long	1093664767
	.long	1024
	.long	1093664768
	.long	4294965802
	.long	1092616191
	.long	1431656496
	.long	1090868565
	.long	1417488516
	.long	1088771413
	.long	298494524
	.long	1086394641
	.long	2083630489
	.long	1083621836
	.long	355810204
	.long	1080689044
	.long	1014844880
	.long	1093759672
	.long	1014846758
	.long	1093759672
	.long	1014843908
	.long	1092711096
	.long	1353122430
	.long	1090995104
	.long	1359405560
	.long	1088897952
	.long	1208032152
	.long	1086495872
	.long	3550721845
	.long	1083756770
	.long	3313961802
	.long	1080842760
	.long	171029432
	.long	1093863166
	.long	171031154
	.long	1093863166
	.long	171030626
	.long	1092814590
	.long	228049186
	.long	1091133096
	.long	196452232
	.long	1089035944
	.long	2633333922
	.long	1086606265
	.long	1145505718
	.long	1083904054
	.long	726399779
	.long	1081012148
	.long	3577095953
	.long	1093976026
	.long	3577097532
	.long	1093976026
	.long	3577100597
	.long	1092927450
	.long	474522872
	.long	1091283577
	.long	383058688
	.long	1089186425
	.long	1621696826
	.long	1086726650
	.long	425004090
	.long	1084064710
	.long	1746787218
	.long	1081139549
	.long	1719613688
	.long	1094099102
	.long	1719615136
	.long	1094099102
	.long	1719618236
	.long	1093050526
	.long	3724500239
	.long	1091447677
	.long	3633566020
	.long	1089350525
	.long	4261174779
	.long	1086857930
	.long	2691998327
	.long	1084233670
	.long	3441503036
	.long	1081239489
	.long	1110089283
	.long	1094233317
	.long	1110090611
	.long	1094233317
	.long	1110092918
	.long	1093184741
	.long	2171723412
	.long	1091597123
	.long	2133444349
	.long	1089499971
	.long	2342693282
	.long	1087001093
	.long	493648718
	.long	1084329100
	.long	2069066397
	.long	1081348203
	.long	2572865868
	.long	1094379679
	.long	2572867085
	.long	1094379679
	.long	2572870557
	.long	1093331103
	.long	1715264541
	.long	1091694698
	.long	1667327133
	.long	1089597546
	.long	1224262760
	.long	1087157213
	.long	1594852686
	.long	1084433206
	.long	260135111
	.long	1081467984
	.long	3707478617
	.long	1094539288
	.long	3707479734
	.long	1094539288
	.long	3707477486
	.long	1093490712
	.long	2471644294
	.long	1091801104
	.long	2472638675
	.long	1089703952
	.long	1675695734
	.long	1087327463
	.long	988559666
	.long	1084546600
	.long	2579358114
	.long	1081595549
	.long	4294966784
	.long	1094713343
	.long	256
	.long	1094713344
	.long	21
	.long	1093664768
	.long	1431655472
	.long	1091917141
	.long	1417489540
	.long	1089819989
	.long	298493705
	.long	1087443217
	.long	2083631581
	.long	1084670412
	.long	355801606
	.long	1081737620
	.long	1014845584
	.long	1094808248
	.long	1014846053
	.long	1094808248
	.long	1014846822
	.long	1093759672
	.long	1353145100
	.long	1092043680
	.long	1321720924
	.long	1089946528
	.long	817580875
	.long	1087544448
	.long	1414745249
	.long	1084805432
	.long	3336072390
	.long	1081893077
	.long	171030078
	.long	1094911742
	.long	171030509
	.long	1094911742
	.long	171029638
	.long	1093863166
	.long	228024203
	.long	1092181672
	.long	224317828
	.long	1090084520
	.long	3019441476
	.long	1087654841
	.long	190554934
	.long	1084952567
	.long	3683266053
	.long	1082059035
	.long	3577096546
	.long	1095024602
	.long	3577096941
	.long	1095024602
	.long	3577094443
	.long	1093976026
	.long	474476929
	.long	1092332153
	.long	498110689
	.long	1090235001
	.long	2420301076
	.long	1087775226
	.long	285764000
	.long	1085113025
	.long	2388641430
	.long	1082186277
	.long	1719614232
	.long	1095147678
	.long	1719614595
	.long	1095147678
	.long	1719610745
	.long	1094099102
	.long	3724429930
	.long	1092496253
	.long	3770584984
	.long	1090399101
	.long	1148342889
	.long	1087906507
	.long	809090904
	.long	1085282091
	.long	2331282844
	.long	1082285389
	.long	1110089781
	.long	1095281893
	.long	1110090113
	.long	1095281893
	.long	1110093300
	.long	1094233317
	.long	2171723132
	.long	1092645699
	.long	2134433374
	.long	1090548547
	.long	2343552424
	.long	1088049669
	.long	3745963458
	.long	1085377673
	.long	915246751
	.long	1082396774
	.long	2572866325
	.long	1095428255
	.long	2572866629
	.long	1095428255
	.long	2572868076
	.long	1094379679
	.long	1715252755
	.long	1092743274
	.long	1692376938
	.long	1090646122
	.long	1620253050
	.long	1088205789
	.long	2312135315
	.long	1085481725
	.long	546660203
	.long	1082515656
	.long	3707479036
	.long	1095587864
	.long	3707479316
	.long	1095587864
	.long	3707475815
	.long	1094539288
	.long	2471632157
	.long	1092849680
	.long	2490458773
	.long	1090752528
	.long	2065236278
	.long	1088376039
	.long	3434133632
	.long	1085595135
	.long	2573995247
	.long	1082643260
	.long	4294967168
	.long	1095761919
	.long	64
	.long	1095761920
	.long	213
	.long	1094713344
	.long	1431655216
	.long	1092965717
	.long	1417489796
	.long	1090868565
	.long	298493500
	.long	1088491793
	.long	2083631854
	.long	1085718988
	.long	355808637
	.long	1082786196
	.long	1014845760
	.long	1095856824
	.long	1014845877
	.long	1095856824
	.long	1014847550
	.long	1094808248
	.long	1353144371
	.long	1093092256
	.long	1312311946
	.long	1090995104
	.long	813476255
	.long	1088593024
	.long	2889757753
	.long	1085854029
	.long	4245160544
	.long	1082941703
	.long	171030239
	.long	1095960318
	.long	171030346
	.long	1095960318
	.long	171033655
	.long	1094911742
	.long	228071704
	.long	1093230248
	.long	158566009
	.long	1091133096
	.long	2242876999
	.long	1088703417
	.long	959541354
	.long	1086001292
	.long	3627031457
	.long	1083111041
	.long	3577096694
	.long	1096073178
	.long	3577096793
	.long	1096073178
	.long	3577095036
	.long	1095024602
	.long	474476331
	.long	1093380729
	.long	490526759
	.long	1091283577
	.long	2417038778
	.long	1088823802
	.long	1153252389
	.long	1086161618
	.long	2911272706
	.long	1083234873
	.long	1719614367
	.long	1096196254
	.long	1719614457
	.long	1096196254
	.long	1719617401
	.long	1095147678
	.long	3724500675
	.long	1093544829
	.long	3659385013
	.long	1091447677
	.long	4272740135
	.long	1088955082
	.long	1449988639
	.long	1086330793
	.long	710311906
	.long	1083336572
	.long	1110089906
	.long	1096330469
	.long	1110089989
	.long	1096330469
	.long	1110089132
	.long	1095281893
	.long	2171712172
	.long	1093694275
	.long	2171033651
	.long	1091597123
	.long	2749703086
	.long	1089098245
	.long	3587505039
	.long	1086426166
	.long	1886197718
	.long	1083444384
	.long	2572866439
	.long	1096476831
	.long	2572866515
	.long	1096476831
	.long	2572869588
	.long	1095428255
	.long	1715252060
	.long	1093791850
	.long	1680456788
	.long	1091694698
	.long	1609561203
	.long	1089254365
	.long	2509599475
	.long	1086530328
	.long	1964978055
	.long	1083564296
	.long	3707479140
	.long	1096636440
	.long	3707479210
	.long	1096636440
	.long	3707481794
	.long	1095587864
	.long	2471655053
	.long	1093898256
	.long	2440365996
	.long	1091801104
	.long	1273170287
	.long	1089424615
	.long	1952430961
	.long	1086643825
	.long	2795738593
	.long	1083693644
	.long	4294967264
	.long	1096810495
	.long	16
	.long	1096810496
	.long	261
	.long	1095761920
	.long	1431655152
	.long	1094014293
	.long	1417489860
	.long	1091917141
	.long	298493449
	.long	1089540369
	.long	2083631923
	.long	1086767564
	.long	355801227
	.long	1083834772
	.long	1014845804
	.long	1096905400
	.long	1014845833
	.long	1096905400
	.long	1014847733
	.long	1095856824
	.long	1353144197
	.long	1094140832
	.long	1309935339
	.long	1092043680
	.long	812248835
	.long	1089641600
	.long	314249476
	.long	1086902611
	.long	2610079324
	.long	1083990293
	.long	171030280
	.long	1097008894
	.long	171030307
	.long	1097008894
	.long	171028263
	.long	1095960318
	.long	228025330
	.long	1094278824
	.long	251211477
	.long	1092181672
	.long	3031505317
	.long	1089751993
	.long	107734986
	.long	1087049658
	.long	3034747437
	.long	1084156042
	.long	3577096731
	.long	1097121754
	.long	3577096756
	.long	1097121754
	.long	3577095185
	.long	1096073178
	.long	474476188
	.long	1094429305
	.long	488612505
	.long	1092332153
	.long	2416065154
	.long	1089872378
	.long	2651582387
	.long	1087210198
	.long	541517163
	.long	1084283455
	.long	1719614401
	.long	1097244830
	.long	1719614423
	.long	1097244830
	.long	1719616933
	.long	1096196254
	.long	3724501077
	.long	1094593405
	.long	3667944131
	.long	1092496253
	.long	4276519179
	.long	1090003658
	.long	2700442756
	.long	1087379359
	.long	931613300
	.long	1084385125
	.long	1110089937
	.long	1097379045
	.long	1110089958
	.long	1097379045
	.long	1110090221
	.long	1096330469
	.long	2171711677
	.long	1094742851
	.long	2162016345
	.long	1092645699
	.long	2741863994
	.long	1090146821
	.long	1245458209
	.long	1087474763
	.long	2097929072
	.long	1084493008
	.long	2572866468
	.long	1097525407
	.long	2572866487
	.long	1097525407
	.long	2572865702
	.long	1096476831
	.long	1715240994
	.long	1094840426
	.long	1713835862
	.long	1092743274
	.long	2012933276
	.long	1090302941
	.long	3665668716
	.long	1087578828
	.long	3415169568
	.long	1084611923
	.long	3707479166
	.long	1097685016
	.long	3707479183
	.long	1097685016
	.long	3707483288
	.long	1096636440
	.long	2471667167
	.long	1094946832
	.long	2427851938
	.long	1092849680
	.long	888456928
	.long	1090473191
	.long	3625621214
	.long	1087692429
	.long	3663259990
	.long	1084743056
	.long	4294967288
	.long	1097859071
	.long	4
	.long	1097859072
	.long	273
	.long	1096810496
	.long	1431655136
	.long	1095062869
	.long	1417489876
	.long	1092965717
	.long	298493436
	.long	1090588945
	.long	2083631940
	.long	1087816140
	.long	355808550
	.long	1084883348
	.long	1014845815
	.long	1097953976
	.long	1014845822
	.long	1097953976
	.long	1014847778
	.long	1096905400
	.long	1353144148
	.long	1095189408
	.long	1309359459
	.long	1093092256
	.long	812077300
	.long	1090690176
	.long	1610135328
	.long	1087951188
	.long	1252860071
	.long	1085038872
	.long	171030290
	.long	1098057470
	.long	171030297
	.long	1098057470
	.long	171029048
	.long	1097008894
	.long	228024638
	.long	1095327400
	.long	237989369
	.long	1093230248
	.long	3025415875
	.long	1090800569
	.long	138804687
	.long	1088098264
	.long	3643470356
	.long	1085204690
	.long	3577096740
	.long	1098170330
	.long	3577096746
	.long	1098170330
	.long	3577097354
	.long	1097121754
	.long	474499828
	.long	1095477881
	.long	451780922
	.long	1093380729
	.long	2026015950
	.long	1090920954
	.long	776452488
	.long	1088258858
	.long	1204131141
	.long	1085332899
	.long	1719614410
	.long	1098293406
	.long	1719614416
	.long	1098293406
	.long	1719612551
	.long	1097244830
	.long	3724453814
	.long	1095641981
	.long	3742826493
	.long	1093544829
	.long	762401611
	.long	1091052235
	.long	2907564309
	.long	1088427850
	.long	502993765
	.long	1085431961
	.long	1110089945
	.long	1098427621
	.long	1110089951
	.long	1098427621
	.long	1110088362
	.long	1097379045
	.long	2171699718
	.long	1095791427
	.long	2177932438
	.long	1093694275
	.long	3129583820
	.long	1091195397
	.long	780294918
	.long	1088523303
	.long	170683064
	.long	1085540730
	.long	2572866475
	.long	1098573983
	.long	2572866480
	.long	1098573983
	.long	2572866862
	.long	1097525407
	.long	1715240475
	.long	1095889002
	.long	1704007165
	.long	1093791850
	.long	2004279021
	.long	1091351517
	.long	647746018
	.long	1088627427
	.long	1886180147
	.long	1085660552
	.long	3707479173
	.long	1098733592
	.long	3707479178
	.long	1098733592
	.long	3707479398
	.long	1097685016
	.long	2471643324
	.long	1095995408
	.long	2461076443
	.long	1093898256
	.long	1665277865
	.long	1091521767
	.long	1998664073
	.long	1088740930
	.long	498206358
	.long	1085789916
	.long	4294967294
	.long	1098907647
	.long	1
	.long	1098907648
	.long	276
	.long	1097859072
	.long	1431655132
	.long	1096111445
	.long	1417489880
	.long	1094014293
	.long	298493433
	.long	1091637521
	.long	2083631944
	.long	1088864716
	.long	355801196
	.long	1085931924
	.long	1014845818
	.long	1099002552
	.long	1014845820
	.long	1099002552
	.long	1014845657
	.long	1097953976
	.long	1353120456
	.long	1096237984
	.long	1345580689
	.long	1094140832
	.long	1201937999
	.long	1091738752
	.long	824125633
	.long	1088999682
	.long	3691484490
	.long	1086085714
	.long	171030292
	.long	1099106046
	.long	171030293
	.long	1099106046
	.long	171033508
	.long	1098057470
	.long	228071817
	.long	1096375976
	.long	161971713
	.long	1094278824
	.long	2244267125
	.long	1091849145
	.long	2158827651
	.long	1089147012
	.long	600071589
	.long	1086256752
	.long	3577096742
	.long	1099218906
	.long	3577096743
	.long	1099218906
	.long	3577100028
	.long	1098170330
	.long	474523020
	.long	1096526457
	.long	406220006
	.long	1094429305
	.long	1632136669
	.long	1091969530
	.long	2352939251
	.long	1089307537
	.long	3345746864
	.long	1086382366
	.long	1719614412
	.long	1099341982
	.long	1719614414
	.long	1099341982
	.long	1719613588
	.long	1098293406
	.long	3724452896
	.long	1096690557
	.long	3725175790
	.long	1094593405
	.long	754459631
	.long	1092100811
	.long	3012112000
	.long	1089476446
	.long	3479767501
	.long	1086480584
	.long	1110089947
	.long	1099476197
	.long	1110089949
	.long	1099476197
	.long	1110087897
	.long	1098427621
	.long	2171699924
	.long	1096840003
	.long	2181914507
	.long	1094742851
	.long	3133129538
	.long	1092243973
	.long	629375254
	.long	1089571870
	.long	2644154789
	.long	1086589284
	.long	2572866477
	.long	1099622559
	.long	2572866479
	.long	1099622559
	.long	2572865020
	.long	1098573983
	.long	1715228507
	.long	1096937578
	.long	1719729545
	.long	1094840426
	.long	2391941724
	.long	1092400093
	.long	2057257972
	.long	1089675967
	.long	3221519396
	.long	1086708274
	.long	3707479175
	.long	1099782168
	.long	3707479177
	.long	1099782168
	.long	3707476293
	.long	1098733592
	.long	2471631915
	.long	1097043984
	.long	2487568215
	.long	1094946832
	.long	2062609080
	.long	1092570343
	.long	1539176086
	.long	1089789446
	.long	1226576577
	.long	1086837580
	.long	4294967295
	.long	1099956223
	.long	0
	.long	1099956224
	.long	2409
	.long	1098907648
	.long	1431666026
	.long	1097160021
	.long	1381124679
	.long	1095062869
	.long	95404972
	.long	1092686097
	.long	4267392907
	.long	1089913374
	.long	744273338
	.long	1086981465
	.long	1014845819
	.long	1100051128
	.long	1014845820
	.long	1100051128
	.long	1014842995
	.long	1099002552
	.long	1353097250
	.long	1097286560
	.long	1390989017
	.long	1095189408
	.long	1595792887
	.long	1092787328
	.long	729832970
	.long	1090048155
	.long	2003384808
	.long	1087132508
	.long	171030293
	.long	1100154622
	.long	171030294
	.long	1100154622
	.long	171030359
	.long	1099106046
	.long	228023473
	.long	1097424552
	.long	215685517
	.long	1095327400
	.long	3015470348
	.long	1092897721
	.long	2729851489
	.long	1090195466
	.long	4013155943
	.long	1087301962
	.long	3577096743
	.long	1100267482
	.long	3577096744
	.long	1100267482
	.long	3577096433
	.long	1099218906
	.long	474475076
	.long	1097575033
	.long	467535814
	.long	1095477881
	.long	2406643378
	.long	1093018106
	.long	1877814210
	.long	1090355974
	.long	3967364979
	.long	1087429239
	.long	1719614412
	.long	1100390558
	.long	1719614412
	.long	1100390558
	.long	1719618112
	.long	1099341982
	.long	3724500027
	.long	1097739133
	.long	3648026623
	.long	1095641981
	.long	4267608097
	.long	1093149386
	.long	961630931
	.long	1090525110
	.long	3904090335
	.long	1087530906
	.long	1110089947
	.long	1100524773
	.long	1110089947
	.long	1100524773
	.long	1110092045
	.long	1099476197
	.long	2171723653
	.long	1097888579
	.long	2146547869
	.long	1095791427
	.long	2354314515
	.long	1093292549
	.long	1667016450
	.long	1090620526
	.long	384884403
	.long	1087639589
	.long	2572866477
	.long	1100671135
	.long	2572866477
	.long	1100671135
	.long	2572868824
	.long	1099622559
	.long	1715252390
	.long	1097986154
	.long	1687294941
	.long	1095889002
	.long	1615685301
	.long	1093448669
	.long	298372850
	.long	1090724617
	.long	2476072344
	.long	1087758563
	.long	3707479175
	.long	1100830744
	.long	3707479175
	.long	1100830744
	.long	3707479781
	.long	1099782168
	.long	2471655937
	.long	1098092560
	.long	2457829004
	.long	1095995408
	.long	1288807310
	.long	1093618919
	.long	3573393579
	.long	1090838089
	.long	2043174239
	.long	1087887854
	.long	0
	.long	1101004800
	.long	0
	.long	1101004800
	.long	4294966783
	.long	1099956223
	.long	1431655600
	.long	1098208597
	.long	1426590319
	.long	1096111445
	.long	302639379
	.long	1093734673
	.long	3581289218
	.long	1090961847
	.long	2803312407
	.long	1088029026
	.long	1014845819
	.long	1101099704
	.long	1014845819
	.long	1101099704
	.long	1014844462
	.long	1100051128
	.long	1353121523
	.long	1098335136
	.long	1365969809
	.long	1096237984
	.long	1210888042
	.long	1093835904
	.long	4032953612
	.long	1091096787
	.long	3351349617
	.long	1088182757
	.long	171030293
	.long	1101203198
	.long	171030293
	.long	1101203198
	.long	171031703
	.long	1100154622
	.long	228047842
	.long	1098473128
	.long	192773132
	.long	1096375976
	.long	2631782123
	.long	1093946297
	.long	2631854033
	.long	1091244094
	.long	2315550364
	.long	1088352199
	.long	3577096743
	.long	1101316058
	.long	3577096743
	.long	1101316058
	.long	3577097666
	.long	1100267482
	.long	474499550
	.long	1098623609
	.long	446511749
	.long	1096526457
	.long	2023660504
	.long	1094066682
	.long	583015094
	.long	1091404598
	.long	2060630322
	.long	1088478641
	.long	1719614413
	.long	1101439134
	.long	1719614414
	.long	1101439134
	.long	1719610714
	.long	1100390558
	.long	3724429877
	.long	1098787709
	.long	3774200135
	.long	1096690557
	.long	1149951221
	.long	1094197963
	.long	376496729
	.long	1091573543
	.long	3520625071
	.long	1088576835
	.long	1110089947
	.long	1101573349
	.long	1110089947
	.long	1101573349
	.long	1110093082
	.long	1100524773
	.long	2171723193
	.long	1098937155
	.long	2137706208
	.long	1096840003
	.long	2346420191
	.long	1094341125
	.long	1926455040
	.long	1091669122
	.long	3075683983
	.long	1088688212
	.long	2572866477
	.long	1101719711
	.long	2572866477
	.long	1101719711
	.long	2572869775
	.long	1100671135
	.long	1715251969
	.long	1099034730
	.long	1679186288
	.long	1096937578
	.long	1608407888
	.long	1094497245
	.long	2006172809
	.long	1091773211
	.long	1497125046
	.long	1088807183
	.long	3707479175
	.long	1101879320
	.long	3707479175
	.long	1101879320
	.long	3707480653
	.long	1100830744
	.long	2471655550
	.long	1099141136
	.long	2450394200
	.long	1097043984
	.long	1282177669
	.long	1094667495
	.long	3008229769
	.long	1091886682
	.long	2158328475
	.long	1088936470
	.long	0
	.long	1102053376
	.long	0
	.long	1102053376
	.long	143
	.long	1101004800
	.long	1431655243
	.long	1099257173
	.long	1419780217
	.long	1097160021
	.long	299652155
	.long	1094783249
	.long	1211207709
	.long	1092010439
	.long	329765955
	.long	1089077639
	.long	1014845819
	.long	1102148280
	.long	1014845819
	.long	1102148280
	.long	1014844828
	.long	1101099704
	.long	1353121192
	.long	1099383712
	.long	1359733278
	.long	1097286560
	.long	1208235969
	.long	1094884480
	.long	356060089
	.long	1092145378
	.long	3107959578
	.long	1089231366
	.long	171030293
	.long	1102251774
	.long	171030293
	.long	1102251774
	.long	171032040
	.long	1101203198
	.long	228047551
	.long	1099521704
	.long	187020672
	.long	1097424552
	.long	2629046472
	.long	1094994873
	.long	2884342378
	.long	1092292683
	.long	1506755341
	.long	1089400807
	.long	3577096743
	.long	1102364634
	.long	3577096743
	.long	1102364634
	.long	3577097974
	.long	1101316058
	.long	474499274
	.long	1099672185
	.long	441261822
	.long	1097575033
	.long	2021374080
	.long	1095115258
	.long	190100264
	.long	1092453186
	.long	2056125696
	.long	1089527231
	.long	1719614413
	.long	1102487710
	.long	1719614414
	.long	1102487710
	.long	1719610997
	.long	1101439134
	.long	3724429628
	.long	1099836285
	.long	3769372221
	.long	1097739133
	.long	1147753795
	.long	1095246539
	.long	2430449986
	.long	1092622124
	.long	3974077509
	.long	1089625424
	.long	1110089947
	.long	1102621925
	.long	1110089947
	.long	1102621925
	.long	1110093341
	.long	1101573349
	.long	2171723077
	.long	1099985731
	.long	2135498839
	.long	1097888579
	.long	2344495521
	.long	1095389701
	.long	1956676740
	.long	1092717703
	.long	2117256085
	.long	1089736800
	.long	2572866477
	.long	1102768287
	.long	2572866477
	.long	1102768287
	.long	2572870013
	.long	1101719711
	.long	1715251864
	.long	1100083306
	.long	1677156080
	.long	1097986154
	.long	1606573745
	.long	1095545821
	.long	320249197
	.long	1092821792
	.long	1439390188
	.long	1089855770
	.long	3707479175
	.long	1102927896
	.long	3707479175
	.long	1102927896
	.long	3707480871
	.long	1101879320
	.long	2471655453
	.long	1100189712
	.long	2448535499
	.long	1098092560
	.long	1280531624
	.long	1095716071
	.long	3940680640
	.long	1092935262
	.long	2063813777
	.long	1089985056
	.long	0
	.long	1103101952
	.long	0
	.long	1103101952
	.long	243
	.long	1102053376
	.long	1431655155
	.long	1100305749
	.long	1418074647
	.long	1098208597
	.long	298875225
	.long	1095831825
	.long	653292726
	.long	1093059019
	.long	1465284600
	.long	1090126224
	.long	1014845819
	.long	1103196856
	.long	1014845819
	.long	1103196856
	.long	1014844920
	.long	1102148280
	.long	1353121114
	.long	1100432288
	.long	1358161964
	.long	1098335136
	.long	1207458112
	.long	1095933056
	.long	2796544215
	.long	1093193957
	.long	2424182090
	.long	1090279951
	.long	171030293
	.long	1103300350
	.long	171030293
	.long	1103300350
	.long	171032124
	.long	1102251774
	.long	228047476
	.long	1100570280
	.long	185588648
	.long	1098473128
	.long	2628417140
	.long	1096043449
	.long	3951958296
	.long	1093341262
	.long	66960268
	.long	1090449391
	.long	3577096743
	.long	1103413210
	.long	3577096743
	.long	1103413210
	.long	3577098051
	.long	1102364634
	.long	474499205
	.long	1100720761
	.long	439949341
	.long	1098623609
	.long	2020802481
	.long	1096163834
	.long	91857605
	.long	1093501765
	.long	4202353141
	.long	1090575810
	.long	1719614413
	.long	1103536286
	.long	1719614414
	.long	1103536286
	.long	1719611067
	.long	1102487710
	.long	3724429560
	.long	1100884861
	.long	3768183514
	.long	1098787709
	.long	1147345439
	.long	1096295115
	.long	3913822085
	.long	1093670701
	.long	3551604025
	.long	1090674003
	.long	1110089947
	.long	1103670501
	.long	1110089947
	.long	1103670501
	.long	1110093406
	.long	1102621925
	.long	2171723049
	.long	1101034307
	.long	2134943951
	.long	1098937155
	.long	2343965451
	.long	1096438277
	.long	3072602636
	.long	1093766280
	.long	2434876993
	.long	1090785379
	.long	2572866477
	.long	1103816863
	.long	2572866477
	.long	1103816863
	.long	2572870072
	.long	1102768287
	.long	1715251836
	.long	1101131882
	.long	1676654618
	.long	1099034730
	.long	1606201624
	.long	1096594397
	.long	903280726
	.long	1093870369
	.long	3655494515
	.long	1090904348
	.long	3707479175
	.long	1103976472
	.long	3707479175
	.long	1103976472
	.long	3707480926
	.long	1102927896
	.long	2471655431
	.long	1101238288
	.long	2448064733
	.long	1099141136
	.long	1280010955
	.long	1096764647
	.long	4243036701
	.long	1093983839
	.long	1130227930
	.long	1091033635
	.long	0
	.long	1104150528
	.long	0
	.long	1104150528
	.long	268
	.long	1103101952
	.long	1431655133
	.long	1101354325
	.long	1417648254
	.long	1099257173
	.long	298680988
	.long	1096880401
	.long	513823282
	.long	1094107596
	.long	2823073373
	.long	1091174802
	.long	1014845819
	.long	1104245432
	.long	1014845819
	.long	1104245432
	.long	1014844943
	.long	1103196856
	.long	1353121094
	.long	1101480864
	.long	1357769135
	.long	1099383712
	.long	1207275009
	.long	1096981632
	.long	2332932724
	.long	1094242534
	.long	3080521966
	.long	1091328529
	.long	171030293
	.long	1104348926
	.long	171030293
	.long	1104348926
	.long	171032145
	.long	1103300350
	.long	228047457
	.long	1101618856
	.long	185230641
	.long	1099521704
	.long	2628265482
	.long	1097092025
	.long	3145139053
	.long	1094389839
	.long	3878959594
	.long	1091497968
	.long	3577096743
	.long	1104461786
	.long	3577096743
	.long	1104461786
	.long	3577098071
	.long	1103413210
	.long	474499194
	.long	1101769337
	.long	439602948
	.long	1099672185
	.long	2020507204
	.long	1097212410
	.long	3496261741
	.long	1094550341
	.long	1103296215
	.long	1091624388
	.long	1719614413
	.long	1104584862
	.long	1719614414
	.long	1104584862
	.long	1719611085
	.long	1103536286
	.long	3724429547
	.long	1101933437
	.long	3767874156
	.long	1099836285
	.long	1147145559
	.long	1097343691
	.long	1132680656
	.long	1094719278
	.long	3486439013
	.long	1091722580
	.long	1110089947
	.long	1104719077
	.long	1110089947
	.long	1104719077
	.long	1110093422
	.long	1103670501
	.long	2171723041
	.long	1102082883
	.long	2134808274
	.long	1099985731
	.long	2343881823
	.long	1097486853
	.long	95743942
	.long	1094814857
	.long	883554237
	.long	1091833956
	.long	2572866477
	.long	1104865439
	.long	2572866477
	.long	1104865439
	.long	2572870087
	.long	1103816863
	.long	1715251830
	.long	1102180458
	.long	1676526208
	.long	1100083306
	.long	1606059712
	.long	1097642973
	.long	2157385827
	.long	1094918945
	.long	2618878781
	.long	1091952925
	.long	3707479175
	.long	1105025048
	.long	3707479175
	.long	1105025048
	.long	3707480939
	.long	1103976472
	.long	2471655422
	.long	1102286864
	.long	2447956178
	.long	1100189712
	.long	1280050214
	.long	1097813223
	.long	993528255
	.long	1095032416
	.long	2199985578
	.long	1092082211
	.long	0
	.long	1124597760
	.long	0
	.long	1124597760
	.long	0
	.long	1124597760
	.long	0
	.long	1124597760
	.long	0
	.long	1124597760
	.long	0
	.long	1124597760
	.long	0
	.long	1124597760
	.long	0
	.long	1124597760
	.long	1082535401
	.long	1082535401
	.long	1082535401
	.long	1082535401
	.long	1082535401
	.long	1082535401
	.long	1082535401
	.long	1082535401
	.long	1082535401
	.long	1082535401
	.long	1082535401
	.long	1082535401
	.long	1082535401
	.long	1082535401
	.long	1082535401
	.long	1082535401
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
	.long	1
	.long	1124597760
	.long	1
	.long	1124597760
	.long	1
	.long	1124597760
	.long	1
	.long	1124597760
	.long	1
	.long	1124597760
	.long	1
	.long	1124597760
	.long	1
	.long	1124597760
	.long	1
	.long	1124597760
	.type	__svml_dsinh_ha_data_internal,@object
	.size	__svml_dsinh_ha_data_internal,18240
	.align 32
_vmldSinhHATab:
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
	.long	1431655765
	.long	1069897045
	.long	286331153
	.long	1065423121
	.long	436314138
	.long	1059717536
	.long	2773927732
	.long	1053236707
	.long	1697350398
	.long	1079448903
	.long	0
	.long	1127743488
	.long	33554432
	.long	1101004800
	.long	2411329662
	.long	1082536910
	.long	4277796864
	.long	1065758274
	.long	3164486458
	.long	1025308570
	.long	1
	.long	1048576
	.long	4294967295
	.long	2146435071
	.long	3671843104
	.long	1067178892
	.long	3875694624
	.long	1077247184
	.type	_vmldSinhHATab,@object
	.size	_vmldSinhHATab,1192
	.space 8, 0x00 	# pad
	.align 16
.L_2il0floatpacket.105:
	.long	0x00000000,0x80000000,0x00000000,0x00000000
	.type	.L_2il0floatpacket.105,@object
	.size	.L_2il0floatpacket.105,16
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
