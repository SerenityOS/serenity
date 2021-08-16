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
	.file "svml_d_atan.c"
	.text
..TXTST0:
.L_2__routine_start___svml_atan2_ha_e9_0:
# -- Begin  __svml_atan2_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan2_ha_e9
# --- __svml_atan2_ha_e9(__m128d)
__svml_atan2_ha_e9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #249.1
        .byte     15                                            #399.546
        .byte     30                                            #399.546
        .byte     250                                           #399.546
	.cfi_startproc
..___tag_value___svml_atan2_ha_e9.1:
..L2:
                                                          #249.1
        pushq     %rbp                                          #249.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #249.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #249.1
        subq      $192, %rsp                                    #249.1
        movaps    %xmm0, %xmm10                                 #249.1
        movups    1152+__svml_datan_ha_data_internal(%rip), %xmm11 #301.51
        lea       __svml_datan_ha_data_internal(%rip), %rsi     #332.393
        movups    1344+__svml_datan_ha_data_internal(%rip), %xmm8 #304.59
        andps     %xmm10, %xmm11                                #302.14
        movaps    %xmm8, %xmm6                                  #305.19
        movaps    %xmm11, %xmm9                                 #303.17
        pshufd    $221, %xmm11, %xmm0                           #308.20
        andps     %xmm11, %xmm6                                 #305.19
        movq      1600+__svml_datan_ha_data_internal(%rip), %xmm3 #320.33
        movaps    %xmm11, %xmm7                                 #306.19
        movq      1536+__svml_datan_ha_data_internal(%rip), %xmm1 #322.33
        psubd     %xmm0, %xmm3                                  #320.33
        movq      1472+__svml_datan_ha_data_internal(%rip), %xmm2 #321.33
        psubd     %xmm0, %xmm1                                  #322.33
        movq      1408+__svml_datan_ha_data_internal(%rip), %xmm11 #319.33
        psubd     %xmm0, %xmm2                                  #321.33
        psubd     %xmm0, %xmm11                                 #319.33
        psrad     $31, %xmm3                                    #324.32
        psrad     $31, %xmm11                                   #323.32
        psrad     $31, %xmm2                                    #325.32
        psrad     $31, %xmm1                                    #326.32
        paddd     %xmm3, %xmm11                                 #332.187
        paddd     %xmm1, %xmm2                                  #332.187
        movdqa    %xmm0, %xmm14                                 #310.21
        movq      1664+__svml_datan_ha_data_internal(%rip), %xmm4 #330.23
        paddd     %xmm2, %xmm11                                 #332.187
        paddd     %xmm4, %xmm11                                 #332.187
        movaps    %xmm8, %xmm1                                  #342.19
        pslld     $5, %xmm11                                    #332.187
        pxor      %xmm10, %xmm9                                 #303.17
        movd      %xmm11, %eax                                  #332.250
        movq      2560+__svml_datan_ha_data_internal(%rip), %xmm12 #309.30
        psubd     %xmm12, %xmm14                                #310.21
        movaps    %xmm6, %xmm12                                 #336.20
        pextrd    $1, %xmm11, %ecx                              #332.318
        movdqa    %xmm14, %xmm15                                #312.36
        movq      (%rax,%rsi), %xmm4                            #332.631
        movhpd    (%rcx,%rsi), %xmm4                            #332.599
        subpd     %xmm6, %xmm7                                  #306.19
        mulpd     %xmm4, %xmm6                                  #346.32
        movq      8(%rax,%rsi), %xmm11                          #333.628
        movaps    %xmm7, %xmm2                                  #338.20
        movhpd    8(%rcx,%rsi), %xmm11                          #333.596
        andps     %xmm11, %xmm12                                #336.20
        andps     %xmm11, %xmm2                                 #338.20
        movups    1728+__svml_datan_ha_data_internal(%rip), %xmm5 #314.46
        subpd     %xmm4, %xmm12                                 #337.20
        mulpd     %xmm7, %xmm4                                  #347.20
        andps     %xmm5, %xmm11                                 #345.20
        addpd     %xmm6, %xmm11                                 #346.20
        movaps    %xmm11, %xmm6                                 #348.20
        addpd     %xmm4, %xmm6                                  #348.20
        andps     %xmm6, %xmm8                                  #351.19
        cvtpd2ps  %xmm8, %xmm7                                  #354.54
        subpd     %xmm6, %xmm11                                 #349.23
        subpd     %xmm8, %xmm6                                  #352.22
        addpd     %xmm11, %xmm4                                 #350.20
        movq      2624+__svml_datan_ha_data_internal(%rip), %xmm13 #311.30
        pcmpgtd   %xmm13, %xmm15                                #312.36
        pcmpeqd   %xmm13, %xmm14                                #312.94
        movlhps   %xmm7, %xmm7                                  #354.38
        por       %xmm14, %xmm15                                #312.22
        movmskps  %xmm15, %edx                                  #313.44
        movaps    %xmm12, %xmm14                                #339.20
        movq      24(%rax,%rsi), %xmm3                          #335.637
        movhpd    24(%rcx,%rsi), %xmm3                          #335.605
        rcpps     %xmm7, %xmm15                                 #354.27
        addpd     %xmm6, %xmm4                                  #353.19
        subpd     %xmm2, %xmm14                                 #339.20
        cvtps2pd  %xmm15, %xmm6                                 #354.14
        mulpd     %xmm6, %xmm8                                  #355.31
        mulpd     %xmm6, %xmm4                                  #356.29
        subpd     %xmm5, %xmm8                                  #355.19
        addpd     %xmm4, %xmm8                                  #356.17
        movaps    %xmm8, %xmm5                                  #357.31
        movaps    %xmm14, %xmm13                                #340.21
        mulpd     %xmm8, %xmm5                                  #357.31
        subpd     %xmm12, %xmm13                                #340.21
        subpd     %xmm8, %xmm5                                  #357.19
        subpd     %xmm13, %xmm2                                 #341.20
        mulpd     %xmm8, %xmm5                                  #358.31
        addpd     %xmm8, %xmm5                                  #358.19
        mulpd     %xmm8, %xmm5                                  #359.31
        subpd     %xmm8, %xmm5                                  #359.19
        mulpd     %xmm8, %xmm5                                  #360.31
        addpd     %xmm8, %xmm5                                  #360.19
        mulpd     %xmm8, %xmm5                                  #361.31
        subpd     %xmm8, %xmm5                                  #361.19
        andps     %xmm14, %xmm1                                 #342.19
        subpd     %xmm1, %xmm14                                 #343.22
        mulpd     %xmm6, %xmm5                                  #362.21
        addpd     %xmm14, %xmm2                                 #344.19
        movaps    %xmm1, %xmm8                                  #363.18
        mulpd     %xmm5, %xmm8                                  #363.18
        mulpd     %xmm2, %xmm5                                  #364.30
        mulpd     %xmm6, %xmm2                                  #365.31
        addpd     %xmm5, %xmm8                                  #364.18
        mulpd     %xmm6, %xmm1                                  #366.29
        addpd     %xmm2, %xmm8                                  #365.19
        movaps    %xmm8, %xmm12                                 #366.17
        addpd     %xmm1, %xmm12                                 #366.17
        addpd     %xmm8, %xmm3                                  #393.19
        movaps    %xmm12, %xmm11                                #367.14
        mulpd     %xmm12, %xmm11                                #367.14
        movaps    %xmm11, %xmm7                                 #368.14
        mulpd     %xmm11, %xmm7                                 #368.14
        movups    1792+__svml_datan_ha_data_internal(%rip), %xmm4 #373.26
        mulpd     %xmm7, %xmm4                                  #373.26
        movups    1856+__svml_datan_ha_data_internal(%rip), %xmm2 #374.26
        addpd     1920+__svml_datan_ha_data_internal(%rip), %xmm4 #373.14
        mulpd     %xmm7, %xmm2                                  #374.26
        mulpd     %xmm7, %xmm4                                  #377.26
        addpd     1984+__svml_datan_ha_data_internal(%rip), %xmm2 #374.14
        addpd     2048+__svml_datan_ha_data_internal(%rip), %xmm4 #377.14
        mulpd     %xmm7, %xmm2                                  #378.26
        mulpd     %xmm7, %xmm4                                  #381.26
        addpd     2112+__svml_datan_ha_data_internal(%rip), %xmm2 #378.14
        addpd     2176+__svml_datan_ha_data_internal(%rip), %xmm4 #381.14
        mulpd     %xmm7, %xmm2                                  #382.26
        mulpd     %xmm7, %xmm4                                  #385.26
        addpd     2240+__svml_datan_ha_data_internal(%rip), %xmm2 #382.14
        addpd     2304+__svml_datan_ha_data_internal(%rip), %xmm4 #385.14
        mulpd     %xmm7, %xmm2                                  #386.26
        mulpd     %xmm7, %xmm4                                  #389.26
        addpd     2368+__svml_datan_ha_data_internal(%rip), %xmm2 #386.14
        addpd     2432+__svml_datan_ha_data_internal(%rip), %xmm4 #389.14
        mulpd     %xmm2, %xmm7                                  #390.26
        mulpd     %xmm11, %xmm4                                 #391.26
        addpd     2496+__svml_datan_ha_data_internal(%rip), %xmm7 #390.14
        addpd     %xmm4, %xmm7                                  #391.14
        mulpd     %xmm7, %xmm11                                 #392.14
        mulpd     %xmm11, %xmm12                                #394.26
        addpd     %xmm12, %xmm3                                 #394.14
        addpd     %xmm3, %xmm1                                  #395.18
        movq      16(%rax,%rsi), %xmm0                          #334.637
        movhpd    16(%rcx,%rsi), %xmm0                          #334.605
        addpd     %xmm1, %xmm0                                  #396.18
        orps      %xmm9, %xmm0                                  #397.14
        andl      $3, %edx                                      #313.93
        jne       ..B1.3        # Prob 5%                       #399.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm10
..B1.2:                         # Preds ..B1.3 ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #402.12
        popq      %rbp                                          #402.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #402.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm10, 64(%rsp)                              #399.197
        movups    %xmm0, 128(%rsp)                              #399.270
        je        ..B1.2        # Prob 95%                      #399.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #399.454
        movq      %r12, 8(%rsp)                                 #399.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #399.454
        movq      %r13, (%rsp)                                  #399.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #399.454
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #399.517
        jc        ..B1.10       # Prob 5%                       #399.517
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #399.470
        cmpl      $2, %r12d                                     #399.465
        jl        ..B1.7        # Prob 82%                      #399.465
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #399.674
        jmp       ..B1.2        # Prob 100%                     #399.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #399.546
        lea       128(%rsp,%r12,8), %rsi                        #399.546
..___tag_value___svml_atan2_ha_e9.19:
#       __svml_datan_ha_cout_rare_internal(const double *, double *)
        call      __svml_datan_ha_cout_rare_internal            #399.546
..___tag_value___svml_atan2_ha_e9.20:
        jmp       ..B1.8        # Prob 100%                     #399.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_atan2_ha_e9,@function
	.size	__svml_atan2_ha_e9,.-__svml_atan2_ha_e9
..LN__svml_atan2_ha_e9.0:
	.data
# -- End  __svml_atan2_ha_e9
	.text
.L_2__routine_start___svml_atan1_ha_e9_1:
# -- Begin  __svml_atan1_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan1_ha_e9
# --- __svml_atan1_ha_e9(__m128d)
__svml_atan1_ha_e9:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #407.1
        .byte     15                                            #557.546
        .byte     30                                            #557.546
        .byte     250                                           #557.546
	.cfi_startproc
..___tag_value___svml_atan1_ha_e9.22:
..L23:
                                                         #407.1
        pushq     %rbp                                          #407.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #407.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #407.1
        subq      $128, %rsp                                    #407.1
        movl      $1071382528, %ecx                             #477.33
        movl      $1072037888, %esi                             #478.33
        movl      $1072889856, %edi                             #479.33
        movl      $1073971200, %r8d                             #480.33
        movaps    %xmm0, %xmm8                                  #407.1
        movsd     1152+__svml_datan_ha_data_internal(%rip), %xmm9 #459.20
        movl      $4, %r9d                                      #490.187
        movsd     1344+__svml_datan_ha_data_internal(%rip), %xmm6 #462.28
        andps     %xmm8, %xmm9                                  #460.14
        movaps    %xmm6, %xmm0                                  #463.19
        movaps    %xmm9, %xmm7                                  #461.17
        pshufd    $85, %xmm9, %xmm14                            #466.20
        andps     %xmm9, %xmm0                                  #463.19
        movaps    %xmm9, %xmm5                                  #464.19
        movd      %ecx, %xmm9                                   #477.33
        movd      %esi, %xmm15                                  #478.33
        movd      %edi, %xmm1                                   #479.33
        movd      %r8d, %xmm3                                   #480.33
        psubd     %xmm14, %xmm9                                 #477.33
        psubd     %xmm14, %xmm15                                #478.33
        psubd     %xmm14, %xmm1                                 #479.33
        psubd     %xmm14, %xmm3                                 #480.33
        psrad     $31, %xmm9                                    #481.32
        psrad     $31, %xmm15                                   #482.32
        psrad     $31, %xmm1                                    #483.32
        psrad     $31, %xmm3                                    #484.32
        paddd     %xmm15, %xmm9                                 #490.187
        paddd     %xmm3, %xmm1                                  #490.187
        movd      %r9d, %xmm2                                   #490.187
        paddd     %xmm1, %xmm9                                  #490.187
        movl      $-2144337920, %eax                            #468.21
        paddd     %xmm2, %xmm9                                  #490.187
        lea       __svml_datan_ha_data_internal(%rip), %r11     #490.433
        pslld     $5, %xmm9                                     #490.187
        movl      $-36700160, %edx                              #469.30
        movd      %xmm9, %r10d                                  #490.250
        movdqa    %xmm14, %xmm12                                #468.21
        movd      %eax, %xmm10                                  #468.21
        movaps    %xmm6, %xmm1                                  #500.19
        psubd     %xmm10, %xmm12                                #468.21
        pxor      %xmm8, %xmm7                                  #461.17
        movq      (%r10,%r11), %xmm2                            #490.433
        movd      %edx, %xmm11                                  #469.30
        movdqa    %xmm2, %xmm14                                 #504.32
        movdqa    %xmm12, %xmm13                                #470.36
        mulsd     %xmm0, %xmm14                                 #504.32
        pcmpgtd   %xmm11, %xmm13                                #470.36
        subsd     %xmm0, %xmm5                                  #464.19
        pcmpeqd   %xmm11, %xmm12                                #470.94
        por       %xmm12, %xmm13                                #470.22
        movmskps  %xmm13, %eax                                  #471.44
        movsd     1728+__svml_datan_ha_data_internal(%rip), %xmm4 #472.15
        movq      8(%r11,%r10), %xmm13                          #491.430
        movdqa    %xmm13, %xmm10                                #494.20
        movdqa    %xmm13, %xmm12                                #496.20
        andps     %xmm4, %xmm13                                 #503.20
        andps     %xmm0, %xmm10                                 #494.20
        andps     %xmm5, %xmm12                                 #496.20
        addsd     %xmm13, %xmm14                                #504.20
        subsd     %xmm2, %xmm10                                 #495.20
        mulsd     %xmm5, %xmm2                                  #505.20
        movaps    %xmm14, %xmm0                                 #506.20
        movaps    %xmm10, %xmm3                                 #497.20
        addsd     %xmm2, %xmm0                                  #506.20
        subsd     %xmm12, %xmm3                                 #497.20
        subsd     %xmm0, %xmm14                                 #507.23
        andps     %xmm0, %xmm6                                  #509.19
        movaps    %xmm3, %xmm11                                 #498.21
        cvtpd2ps  %xmm6, %xmm5                                  #512.54
        addsd     %xmm14, %xmm2                                 #508.20
        subsd     %xmm6, %xmm0                                  #510.22
        subsd     %xmm10, %xmm11                                #498.21
        addsd     %xmm0, %xmm2                                  #511.19
        subsd     %xmm11, %xmm12                                #499.20
        movlhps   %xmm5, %xmm5                                  #512.38
        andps     %xmm3, %xmm1                                  #500.19
        rcpps     %xmm5, %xmm15                                 #512.27
        subsd     %xmm1, %xmm3                                  #501.22
        cvtps2pd  %xmm15, %xmm0                                 #512.14
        addsd     %xmm12, %xmm3                                 #502.19
        mulsd     %xmm0, %xmm6                                  #513.31
        mulsd     %xmm0, %xmm2                                  #514.29
        subsd     %xmm4, %xmm6                                  #513.19
        movaps    %xmm0, %xmm9                                  #523.31
        addsd     %xmm6, %xmm2                                  #514.17
        mulsd     %xmm3, %xmm9                                  #523.31
        movaps    %xmm2, %xmm5                                  #515.31
        mulsd     %xmm2, %xmm5                                  #515.31
        movsd     1792+__svml_datan_ha_data_internal(%rip), %xmm10 #531.26
        subsd     %xmm2, %xmm5                                  #515.19
        mulsd     %xmm2, %xmm5                                  #516.31
        addsd     %xmm2, %xmm5                                  #516.19
        mulsd     %xmm2, %xmm5                                  #517.31
        subsd     %xmm2, %xmm5                                  #517.19
        mulsd     %xmm2, %xmm5                                  #518.31
        addsd     %xmm2, %xmm5                                  #518.19
        mulsd     %xmm2, %xmm5                                  #519.31
        subsd     %xmm2, %xmm5                                  #519.19
        mulsd     %xmm0, %xmm5                                  #520.21
        mulsd     %xmm1, %xmm0                                  #524.29
        movaps    %xmm5, %xmm4                                  #521.18
        movaps    %xmm0, %xmm6                                  #524.17
        mulsd     %xmm1, %xmm4                                  #521.18
        mulsd     %xmm3, %xmm5                                  #522.30
        movsd     1856+__svml_datan_ha_data_internal(%rip), %xmm3 #532.26
        addsd     %xmm4, %xmm5                                  #522.18
        addsd     %xmm5, %xmm9                                  #523.19
        addsd     %xmm9, %xmm6                                  #524.17
        addsd     24(%r11,%r10), %xmm9                          #551.19
        movaps    %xmm6, %xmm2                                  #525.14
        mulsd     %xmm6, %xmm2                                  #525.14
        movaps    %xmm2, %xmm1                                  #526.14
        mulsd     %xmm2, %xmm1                                  #526.14
        mulsd     %xmm1, %xmm10                                 #531.26
        mulsd     %xmm1, %xmm3                                  #532.26
        addsd     1920+__svml_datan_ha_data_internal(%rip), %xmm10 #531.14
        addsd     1984+__svml_datan_ha_data_internal(%rip), %xmm3 #532.14
        mulsd     %xmm1, %xmm10                                 #535.26
        mulsd     %xmm1, %xmm3                                  #536.26
        addsd     2048+__svml_datan_ha_data_internal(%rip), %xmm10 #535.14
        addsd     2112+__svml_datan_ha_data_internal(%rip), %xmm3 #536.14
        mulsd     %xmm1, %xmm10                                 #539.26
        mulsd     %xmm1, %xmm3                                  #540.26
        addsd     2176+__svml_datan_ha_data_internal(%rip), %xmm10 #539.14
        addsd     2240+__svml_datan_ha_data_internal(%rip), %xmm3 #540.14
        mulsd     %xmm1, %xmm10                                 #543.26
        mulsd     %xmm1, %xmm3                                  #544.26
        addsd     2304+__svml_datan_ha_data_internal(%rip), %xmm10 #543.14
        addsd     2368+__svml_datan_ha_data_internal(%rip), %xmm3 #544.14
        mulsd     %xmm1, %xmm10                                 #547.26
        mulsd     %xmm1, %xmm3                                  #548.26
        addsd     2432+__svml_datan_ha_data_internal(%rip), %xmm10 #547.14
        addsd     2496+__svml_datan_ha_data_internal(%rip), %xmm3 #548.14
        mulsd     %xmm2, %xmm10                                 #549.26
        addsd     %xmm3, %xmm10                                 #549.14
        mulsd     %xmm2, %xmm10                                 #550.14
        mulsd     %xmm6, %xmm10                                 #552.26
        addsd     %xmm9, %xmm10                                 #552.14
        addsd     %xmm10, %xmm0                                 #553.18
        addsd     16(%r11,%r10), %xmm0                          #554.18
        orps      %xmm7, %xmm0                                  #555.14
        andl      $1, %eax                                      #471.93
        jne       ..B2.3        # Prob 5%                       #557.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm8
..B2.2:                         # Preds ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #560.12
        popq      %rbp                                          #560.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #560.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm8, (%rsp)                                 #557.160
        movsd     %xmm0, 64(%rsp)                               #557.233
        jne       ..B2.6        # Prob 5%                       #557.374
                                # LOE rbx r12 r13 r14 r15
..B2.4:                         # Preds ..B2.6 ..B2.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #557.626
        movq      %rbp, %rsp                                    #557.626
        popq      %rbp                                          #557.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #557.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.6:                         # Preds ..B2.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #557.546
        lea       64(%rsp), %rsi                                #557.546
..___tag_value___svml_atan1_ha_e9.35:
#       __svml_datan_ha_cout_rare_internal(const double *, double *)
        call      __svml_datan_ha_cout_rare_internal            #557.546
..___tag_value___svml_atan1_ha_e9.36:
        jmp       ..B2.4        # Prob 100%                     #557.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_atan1_ha_e9,@function
	.size	__svml_atan1_ha_e9,.-__svml_atan1_ha_e9
..LN__svml_atan1_ha_e9.1:
	.data
# -- End  __svml_atan1_ha_e9
	.text
.L_2__routine_start___svml_atan4_ha_e9_2:
# -- Begin  __svml_atan4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan4_ha_e9
# --- __svml_atan4_ha_e9(__m256d)
__svml_atan4_ha_e9:
# parameter 1: %ymm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #565.1
        .byte     15                                            #715.552
        .byte     30                                            #715.552
        .byte     250                                           #715.552
	.cfi_startproc
..___tag_value___svml_atan4_ha_e9.38:
..L39:
                                                         #565.1
        pushq     %rbp                                          #565.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #565.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #565.1
        subq      $192, %rsp                                    #565.1
        lea       __svml_datan_ha_data_internal(%rip), %r8      #648.537
        vmovupd   1344+__svml_datan_ha_data_internal(%rip), %ymm5 #620.62
        vmovups   2624+__svml_datan_ha_data_internal(%rip), %xmm10 #627.30
        vmovups   1408+__svml_datan_ha_data_internal(%rip), %xmm13 #631.33
        vmovupd   1728+__svml_datan_ha_data_internal(%rip), %ymm7 #630.49
        vmovapd   %ymm0, %ymm3                                  #565.1
        vandpd    1152+__svml_datan_ha_data_internal(%rip), %ymm3, %ymm1 #618.14
        vandpd    %ymm5, %ymm1, %ymm6                           #621.19
        vxorpd    %ymm1, %ymm3, %ymm2                           #619.17
        vsubpd    %ymm6, %ymm1, %ymm8                           #622.19
        vextractf128 $1, %ymm1, %xmm4                           #623.107
        vshufps   $221, %xmm4, %xmm1, %xmm0                     #624.38
        vpsubd    2560+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm12 #626.21
        vpsubd    %xmm0, %xmm13, %xmm15                         #635.33
        vpcmpgtd  %xmm10, %xmm12, %xmm11                        #628.36
        vpcmpeqd  %xmm10, %xmm12, %xmm9                         #628.94
        vmovups   1600+__svml_datan_ha_data_internal(%rip), %xmm1 #634.33
        vpor      %xmm9, %xmm11, %xmm14                         #628.22
        vmovups   1536+__svml_datan_ha_data_internal(%rip), %xmm11 #633.33
        vpsubd    %xmm0, %xmm1, %xmm4                           #636.33
        vmovups   1472+__svml_datan_ha_data_internal(%rip), %xmm12 #632.33
        vpsrad    $31, %xmm15, %xmm15                           #639.32
        vpsubd    %xmm0, %xmm12, %xmm10                         #637.33
        vpsubd    %xmm0, %xmm11, %xmm0                          #638.33
        vmovmskps %xmm14, %eax                                  #629.44
        vpsrad    $31, %xmm4, %xmm9                             #640.32
        vpsrad    $31, %xmm10, %xmm14                           #641.32
        vpsrad    $31, %xmm0, %xmm13                            #642.32
        vpaddd    %xmm9, %xmm15, %xmm15                         #648.187
        vpaddd    %xmm13, %xmm14, %xmm0                         #648.187
        vpaddd    %xmm0, %xmm15, %xmm1                          #648.187
        vpaddd    1664+__svml_datan_ha_data_internal(%rip), %xmm1, %xmm4 #648.187
        vpslld    $5, %xmm4, %xmm11                             #648.187
        vmovd     %xmm11, %edx                                  #648.250
        vpextrd   $2, %xmm11, %esi                              #648.389
        vpextrd   $1, %xmm11, %ecx                              #648.318
        vmovq     (%rdx,%r8), %xmm12                            #648.1053
        vmovq     8(%rdx,%r8), %xmm13                           #649.1050
        vpextrd   $3, %xmm11, %edi                              #648.460
        vmovq     (%rsi,%r8), %xmm9                             #648.1253
        vmovq     8(%rsi,%r8), %xmm0                            #649.1250
        vmovhpd   (%rcx,%r8), %xmm12, %xmm10                    #648.1021
        vmovhpd   8(%rcx,%r8), %xmm13, %xmm15                   #649.1018
        vmovhpd   (%rdi,%r8), %xmm9, %xmm14                     #648.1221
        vmovhpd   8(%rdi,%r8), %xmm0, %xmm1                     #649.1218
        vmovq     16(%rdx,%r8), %xmm4                           #650.1059
        vmovhpd   16(%rcx,%r8), %xmm4, %xmm12                   #650.1027
        vmovq     24(%rdx,%r8), %xmm9                           #651.1059
        vmovq     24(%rsi,%r8), %xmm4                           #651.1259
        vinsertf128 $1, %xmm14, %ymm10, %ymm14                  #648.975
        vmovq     16(%rsi,%r8), %xmm10                          #650.1259
        vinsertf128 $1, %xmm1, %ymm15, %ymm13                   #649.972
        vmovhpd   16(%rdi,%r8), %xmm10, %xmm11                  #650.1227
        vandpd    %ymm6, %ymm13, %ymm10                         #652.20
        vmulpd    %ymm14, %ymm6, %ymm6                          #662.35
        vmovhpd   24(%rcx,%r8), %xmm9, %xmm1                    #651.1027
        vandpd    %ymm8, %ymm13, %ymm9                          #654.20
        vmulpd    %ymm8, %ymm14, %ymm8                          #663.20
        vandpd    %ymm7, %ymm13, %ymm13                         #661.20
        vaddpd    %ymm6, %ymm13, %ymm6                          #662.20
        vinsertf128 $1, %xmm11, %ymm12, %ymm0                   #650.981
        vsubpd    %ymm14, %ymm10, %ymm11                        #653.20
        vmovhpd   24(%rdi,%r8), %xmm4, %xmm12                   #651.1227
        vsubpd    %ymm9, %ymm11, %ymm4                          #655.20
        vsubpd    %ymm11, %ymm4, %ymm15                         #656.21
        vsubpd    %ymm15, %ymm9, %ymm10                         #657.20
        vinsertf128 $1, %xmm12, %ymm1, %ymm1                    #651.981
        vandpd    %ymm5, %ymm4, %ymm12                          #658.19
        vsubpd    %ymm12, %ymm4, %ymm11                         #659.22
        vaddpd    %ymm8, %ymm6, %ymm4                           #664.20
        vaddpd    %ymm11, %ymm10, %ymm11                        #660.19
        vsubpd    %ymm4, %ymm6, %ymm6                           #665.23
        vandpd    %ymm5, %ymm4, %ymm9                           #667.19
        vaddpd    %ymm6, %ymm8, %ymm8                           #666.20
        vsubpd    %ymm9, %ymm4, %ymm5                           #668.22
        vcvtpd2ps %ymm9, %xmm6                                  #670.41
        vaddpd    %ymm5, %ymm8, %ymm4                           #669.19
        vrcpps    %xmm6, %xmm8                                  #670.30
        vcvtps2pd %xmm8, %ymm10                                 #670.14
        vmulpd    %ymm10, %ymm9, %ymm5                          #671.34
        vmulpd    %ymm4, %ymm10, %ymm6                          #672.32
        vsubpd    %ymm7, %ymm5, %ymm7                           #671.19
        vaddpd    %ymm6, %ymm7, %ymm9                           #672.17
        vmulpd    %ymm9, %ymm9, %ymm7                           #673.34
        vsubpd    %ymm9, %ymm7, %ymm8                           #673.19
        vmulpd    %ymm8, %ymm9, %ymm5                           #674.34
        vaddpd    %ymm5, %ymm9, %ymm4                           #674.19
        vmulpd    %ymm4, %ymm9, %ymm13                          #675.34
        vsubpd    %ymm9, %ymm13, %ymm14                         #675.19
        vmulpd    %ymm14, %ymm9, %ymm15                         #676.34
        vaddpd    %ymm15, %ymm9, %ymm7                          #676.19
        vmulpd    %ymm7, %ymm9, %ymm6                           #677.34
        vsubpd    %ymm9, %ymm6, %ymm9                           #677.19
        vmulpd    %ymm9, %ymm10, %ymm7                          #678.21
        vmulpd    %ymm7, %ymm12, %ymm6                          #679.18
        vmulpd    %ymm7, %ymm11, %ymm8                          #680.33
        vmulpd    %ymm11, %ymm10, %ymm11                        #681.34
        vmulpd    %ymm10, %ymm12, %ymm7                         #682.32
        vaddpd    %ymm8, %ymm6, %ymm5                           #680.18
        vaddpd    %ymm11, %ymm5, %ymm8                          #681.19
        vaddpd    %ymm7, %ymm8, %ymm6                           #682.17
        vaddpd    %ymm8, %ymm1, %ymm1                           #709.19
        vmulpd    %ymm6, %ymm6, %ymm5                           #683.14
        vmulpd    %ymm5, %ymm5, %ymm4                           #684.14
        vmulpd    1792+__svml_datan_ha_data_internal(%rip), %ymm4, %ymm10 #689.29
        vmulpd    1856+__svml_datan_ha_data_internal(%rip), %ymm4, %ymm12 #690.29
        vaddpd    1920+__svml_datan_ha_data_internal(%rip), %ymm10, %ymm13 #689.14
        vaddpd    1984+__svml_datan_ha_data_internal(%rip), %ymm12, %ymm9 #690.14
        vmulpd    %ymm13, %ymm4, %ymm14                         #693.29
        vmulpd    %ymm9, %ymm4, %ymm10                          #694.29
        vaddpd    2048+__svml_datan_ha_data_internal(%rip), %ymm14, %ymm11 #693.14
        vaddpd    2112+__svml_datan_ha_data_internal(%rip), %ymm10, %ymm13 #694.14
        vmulpd    %ymm11, %ymm4, %ymm12                         #697.29
        vmulpd    %ymm13, %ymm4, %ymm14                         #698.29
        vaddpd    2176+__svml_datan_ha_data_internal(%rip), %ymm12, %ymm15 #697.14
        vaddpd    2240+__svml_datan_ha_data_internal(%rip), %ymm14, %ymm9 #698.14
        vmulpd    %ymm15, %ymm4, %ymm15                         #701.29
        vmulpd    %ymm9, %ymm4, %ymm10                          #702.29
        vaddpd    2304+__svml_datan_ha_data_internal(%rip), %ymm15, %ymm11 #701.14
        vaddpd    2368+__svml_datan_ha_data_internal(%rip), %ymm10, %ymm13 #702.14
        vmulpd    %ymm11, %ymm4, %ymm12                         #705.29
        vmulpd    %ymm13, %ymm4, %ymm4                          #706.29
        vaddpd    2432+__svml_datan_ha_data_internal(%rip), %ymm12, %ymm14 #705.14
        vaddpd    2496+__svml_datan_ha_data_internal(%rip), %ymm4, %ymm4 #706.14
        vmulpd    %ymm14, %ymm5, %ymm9                          #707.29
        vaddpd    %ymm9, %ymm4, %ymm10                          #707.14
        vmulpd    %ymm10, %ymm5, %ymm5                          #708.14
        vmulpd    %ymm5, %ymm6, %ymm4                           #710.29
        vaddpd    %ymm4, %ymm1, %ymm1                           #710.14
        vaddpd    %ymm1, %ymm7, %ymm5                           #711.18
        vaddpd    %ymm5, %ymm0, %ymm0                           #712.18
        vorpd     %ymm2, %ymm0, %ymm0                           #713.14
        testl     %eax, %eax                                    #715.52
        jne       ..B3.3        # Prob 5%                       #715.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm3
..B3.2:                         # Preds ..B3.3 ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #718.12
        popq      %rbp                                          #718.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #718.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm3, 64(%rsp)                               #715.200
        vmovupd   %ymm0, 128(%rsp)                              #715.276
        je        ..B3.2        # Prob 95%                      #715.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #715.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B3.13:                        # Preds ..B3.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B3.7:                         # Preds ..B3.8 ..B3.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #715.523
        jc        ..B3.10       # Prob 5%                       #715.523
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #715.476
        cmpl      $4, %r12d                                     #715.471
        jl        ..B3.7        # Prob 82%                      #715.471
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #715.683
        jmp       ..B3.2        # Prob 100%                     #715.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #715.552
        lea       128(%rsp,%r12,8), %rsi                        #715.552
..___tag_value___svml_atan4_ha_e9.56:
#       __svml_datan_ha_cout_rare_internal(const double *, double *)
        call      __svml_datan_ha_cout_rare_internal            #715.552
..___tag_value___svml_atan4_ha_e9.57:
        jmp       ..B3.8        # Prob 100%                     #715.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_atan4_ha_e9,@function
	.size	__svml_atan4_ha_e9,.-__svml_atan4_ha_e9
..LN__svml_atan4_ha_e9.2:
	.data
# -- End  __svml_atan4_ha_e9
	.text
.L_2__routine_start___svml_atan8_ha_z0_3:
# -- Begin  __svml_atan8_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan8_ha_z0
# --- __svml_atan8_ha_z0(__m512d)
__svml_atan8_ha_z0:
# parameter 1: %zmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #723.1
        .byte     15                                            #846.12
        .byte     30                                            #846.12
        .byte     250                                           #846.12
	.cfi_startproc
..___tag_value___svml_atan8_ha_z0.59:
..L60:
                                                         #723.1
        vmovups   128+__svml_datan_ha_data_internal_avx512(%rip), %zmm8 #783.55
        vmovups   320+__svml_datan_ha_data_internal_avx512(%rip), %zmm9 #785.49
        vmovups   64+__svml_datan_ha_data_internal_avx512(%rip), %zmm10 #781.50
        vmovups   256+__svml_datan_ha_data_internal_avx512(%rip), %zmm15 #791.46
        vmovups   192+__svml_datan_ha_data_internal_avx512(%rip), %zmm2 #790.16
        vandpd    __svml_datan_ha_data_internal_avx512(%rip), %zmm0, %zmm3 #780.12
        vcmppd    $17, {sae}, %zmm8, %zmm3, %k1                 #784.20
        vminpd    {sae}, %zmm3, %zmm9, %zmm7                    #786.14
        vmovups   960+__svml_datan_ha_data_internal_avx512(%rip), %zmm8 #810.52
        vmovups   448+__svml_datan_ha_data_internal_avx512(%rip), %zmm9 #808.280
        vreducepd $40, {sae}, %zmm3, %zmm2{%k1}                 #790.16
        vaddpd    {rn-sae}, %zmm10, %zmm3, %zmm5                #782.19
        vxorpd    %zmm0, %zmm3, %zmm6                           #788.15
        knotw     %k1, %k3                                      #800.20
        vsubpd    {rn-sae}, %zmm10, %zmm5, %zmm11               #789.15
        vpermt2pd 512+__svml_datan_ha_data_internal_avx512(%rip), %zmm5, %zmm9 #808.280
        vmovups   576+__svml_datan_ha_data_internal_avx512(%rip), %zmm10 #809.287
        vfmadd213pd {rn-sae}, %zmm15, %zmm11, %zmm7{%k1}        #792.12
        vcmppd    $29, {sae}, %zmm8, %zmm5, %k2                 #811.19
        vmovups   1344+__svml_datan_ha_data_internal_avx512(%rip), %zmm8 #826.14
        vrcp14pd  %zmm7, %zmm0                                  #794.14
        vsubpd    {rn-sae}, %zmm15, %zmm7, %zmm12               #793.17
        vpermt2pd 640+__svml_datan_ha_data_internal_avx512(%rip), %zmm5, %zmm10 #809.287
        vfmsub213pd {rn-sae}, %zmm12, %zmm11, %zmm3             #795.15
        vmovups   704+__svml_datan_ha_data_internal_avx512(%rip), %zmm11 #812.284
        vmovups   832+__svml_datan_ha_data_internal_avx512(%rip), %zmm12 #813.291
        vpermt2pd 768+__svml_datan_ha_data_internal_avx512(%rip), %zmm5, %zmm11 #812.284
        vpermt2pd 896+__svml_datan_ha_data_internal_avx512(%rip), %zmm5, %zmm12 #813.291
        vblendmpd %zmm10, %zmm9, %zmm5{%k2}                     #814.14
        vmovaps   %zmm15, %zmm13                                #796.14
        vfnmadd231pd {rn-sae}, %zmm7, %zmm0, %zmm13             #796.14
        vmulpd    {rn-sae}, %zmm13, %zmm13, %zmm14              #798.16
        vfmadd213pd {rn-sae}, %zmm0, %zmm13, %zmm0              #797.14
        vblendmpd %zmm12, %zmm11, %zmm13{%k2}                   #815.18
        vfmadd213pd {rn-sae}, %zmm0, %zmm14, %zmm0              #799.15
        vmovups   1152+__svml_datan_ha_data_internal_avx512(%rip), %zmm14 #821.51
        vmulpd    {rn-sae}, %zmm2, %zmm0, %zmm4                 #801.14
        vmulpd    {rn-sae}, %zmm0, %zmm3, %zmm1                 #803.18
        vfnmadd213pd {rn-sae}, %zmm15, %zmm0, %zmm7             #802.14
        vmovups   1216+__svml_datan_ha_data_internal_avx512(%rip), %zmm3 #823.14
        vmovups   1280+__svml_datan_ha_data_internal_avx512(%rip), %zmm15 #824.51
        vfmsub213pd {rn-sae}, %zmm4, %zmm0, %zmm2               #804.13
        vfmadd213pd {rn-sae}, %zmm2, %zmm4, %zmm7               #805.13
        vmulpd    {rn-sae}, %zmm4, %zmm4, %zmm2                 #820.15
        vfnmadd231pd {rn-sae}, %zmm4, %zmm1, %zmm7{%k1}         #806.13
        vblendmpd 1024+__svml_datan_ha_data_internal_avx512(%rip), %zmm5, %zmm1{%k3} #818.14
        vblendmpd 1088+__svml_datan_ha_data_internal_avx512(%rip), %zmm13, %zmm5{%k3} #819.18
        vmulpd    {rn-sae}, %zmm2, %zmm2, %zmm9                 #827.15
        vmulpd    {rn-sae}, %zmm4, %zmm2, %zmm11                #830.15
        vfmadd231pd {rn-sae}, %zmm2, %zmm14, %zmm3              #823.14
        vfmadd231pd {rn-sae}, %zmm2, %zmm15, %zmm8              #826.14
        vaddpd    {rn-sae}, %zmm5, %zmm7, %zmm10                #829.15
        vaddpd    {rn-sae}, %zmm4, %zmm1, %zmm0                 #828.17
        vmovups   1408+__svml_datan_ha_data_internal_avx512(%rip), %zmm7 #831.51
        vmovups   1472+__svml_datan_ha_data_internal_avx512(%rip), %zmm5 #832.51
        vfmadd213pd {rn-sae}, %zmm8, %zmm9, %zmm3               #834.14
        vsubpd    {rn-sae}, %zmm1, %zmm0, %zmm1                 #835.15
        vfmadd213pd {rn-sae}, %zmm5, %zmm7, %zmm2               #833.14
        vsubpd    {rn-sae}, %zmm1, %zmm4, %zmm4                 #836.16
        vfmadd213pd {rn-sae}, %zmm2, %zmm9, %zmm3               #837.15
        vaddpd    {rn-sae}, %zmm4, %zmm10, %zmm1                #838.15
        vfmadd213pd {rn-sae}, %zmm1, %zmm11, %zmm3              #839.15
        vaddpd    {rn-sae}, %zmm0, %zmm3, %zmm0                 #840.16
        vxorpd    %zmm6, %zmm0, %zmm0                           #841.14
        ret                                                     #846.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_atan8_ha_z0,@function
	.size	__svml_atan8_ha_z0,.-__svml_atan8_ha_z0
..LN__svml_atan8_ha_z0.3:
	.data
# -- End  __svml_atan8_ha_z0
	.text
.L_2__routine_start___svml_atan2_ha_ex_4:
# -- Begin  __svml_atan2_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan2_ha_ex
# --- __svml_atan2_ha_ex(__m128d)
__svml_atan2_ha_ex:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1203.1
        .byte     15                                            #1353.546
        .byte     30                                            #1353.546
        .byte     250                                           #1353.546
	.cfi_startproc
..___tag_value___svml_atan2_ha_ex.62:
..L63:
                                                         #1203.1
        pushq     %rbp                                          #1203.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1203.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1203.1
        subq      $192, %rsp                                    #1203.1
        movaps    %xmm0, %xmm10                                 #1203.1
        movups    1152+__svml_datan_ha_data_internal(%rip), %xmm11 #1255.51
        lea       __svml_datan_ha_data_internal(%rip), %rsi     #1286.408
        movups    1344+__svml_datan_ha_data_internal(%rip), %xmm8 #1258.59
        andps     %xmm10, %xmm11                                #1256.14
        movaps    %xmm8, %xmm6                                  #1259.19
        movaps    %xmm11, %xmm9                                 #1257.17
        pshufd    $221, %xmm11, %xmm0                           #1262.14
        andps     %xmm11, %xmm6                                 #1259.19
        movq      1600+__svml_datan_ha_data_internal(%rip), %xmm3 #1274.27
        movaps    %xmm11, %xmm7                                 #1260.19
        movq      1536+__svml_datan_ha_data_internal(%rip), %xmm1 #1276.27
        psubd     %xmm0, %xmm3                                  #1274.27
        movq      1472+__svml_datan_ha_data_internal(%rip), %xmm2 #1275.27
        psubd     %xmm0, %xmm1                                  #1276.27
        movq      1408+__svml_datan_ha_data_internal(%rip), %xmm11 #1273.27
        psubd     %xmm0, %xmm2                                  #1275.27
        psubd     %xmm0, %xmm11                                 #1273.27
        psrad     $31, %xmm3                                    #1278.26
        psrad     $31, %xmm11                                   #1277.26
        psrad     $31, %xmm2                                    #1279.26
        psrad     $31, %xmm1                                    #1280.26
        paddd     %xmm3, %xmm11                                 #1286.181
        paddd     %xmm1, %xmm2                                  #1286.181
        movdqa    %xmm0, %xmm14                                 #1264.15
        movq      1664+__svml_datan_ha_data_internal(%rip), %xmm4 #1284.17
        paddd     %xmm2, %xmm11                                 #1286.181
        paddd     %xmm4, %xmm11                                 #1286.181
        movaps    %xmm8, %xmm1                                  #1296.19
        movq      2560+__svml_datan_ha_data_internal(%rip), %xmm12 #1263.24
        pslld     $5, %xmm11                                    #1286.181
        movd      %xmm11, %eax                                  #1286.238
        psubd     %xmm12, %xmm14                                #1264.15
        pshufd    $1, %xmm11, %xmm12                            #1286.319
        movdqa    %xmm14, %xmm15                                #1266.30
        movq      2624+__svml_datan_ha_data_internal(%rip), %xmm13 #1265.24
        pxor      %xmm10, %xmm9                                 #1257.17
        movd      %xmm12, %ecx                                  #1286.300
        pcmpgtd   %xmm13, %xmm15                                #1266.30
        movq      (%rax,%rsi), %xmm4                            #1286.646
        pcmpeqd   %xmm13, %xmm14                                #1266.76
        movaps    %xmm6, %xmm13                                 #1290.20
        por       %xmm14, %xmm15                                #1266.16
        movhpd    (%rcx,%rsi), %xmm4                            #1286.614
        subpd     %xmm6, %xmm7                                  #1260.19
        mulpd     %xmm4, %xmm6                                  #1300.32
        movmskps  %xmm15, %edx                                  #1267.44
        movq      8(%rax,%rsi), %xmm12                          #1287.643
        movaps    %xmm7, %xmm2                                  #1292.20
        movhpd    8(%rcx,%rsi), %xmm12                          #1287.611
        andps     %xmm12, %xmm13                                #1290.20
        andps     %xmm12, %xmm2                                 #1292.20
        movups    1728+__svml_datan_ha_data_internal(%rip), %xmm5 #1268.46
        subpd     %xmm4, %xmm13                                 #1291.20
        mulpd     %xmm7, %xmm4                                  #1301.20
        andps     %xmm5, %xmm12                                 #1299.20
        movaps    %xmm13, %xmm11                                #1293.20
        addpd     %xmm6, %xmm12                                 #1300.20
        subpd     %xmm2, %xmm11                                 #1293.20
        movaps    %xmm12, %xmm6                                 #1302.20
        movaps    %xmm11, %xmm14                                #1294.21
        addpd     %xmm4, %xmm6                                  #1302.20
        subpd     %xmm13, %xmm14                                #1294.21
        subpd     %xmm6, %xmm12                                 #1303.23
        subpd     %xmm14, %xmm2                                 #1295.20
        addpd     %xmm12, %xmm4                                 #1304.20
        andps     %xmm6, %xmm8                                  #1305.19
        andps     %xmm11, %xmm1                                 #1296.19
        cvtpd2ps  %xmm8, %xmm7                                  #1308.54
        subpd     %xmm8, %xmm6                                  #1306.22
        subpd     %xmm1, %xmm11                                 #1297.22
        addpd     %xmm6, %xmm4                                  #1307.19
        addpd     %xmm11, %xmm2                                 #1298.19
        movlhps   %xmm7, %xmm7                                  #1308.38
        rcpps     %xmm7, %xmm15                                 #1308.27
        cvtps2pd  %xmm15, %xmm6                                 #1308.14
        mulpd     %xmm6, %xmm8                                  #1309.31
        mulpd     %xmm6, %xmm4                                  #1310.29
        subpd     %xmm5, %xmm8                                  #1309.19
        addpd     %xmm4, %xmm8                                  #1310.17
        movaps    %xmm8, %xmm5                                  #1311.31
        mulpd     %xmm8, %xmm5                                  #1311.31
        subpd     %xmm8, %xmm5                                  #1311.19
        mulpd     %xmm8, %xmm5                                  #1312.31
        addpd     %xmm8, %xmm5                                  #1312.19
        mulpd     %xmm8, %xmm5                                  #1313.31
        subpd     %xmm8, %xmm5                                  #1313.19
        mulpd     %xmm8, %xmm5                                  #1314.31
        addpd     %xmm8, %xmm5                                  #1314.19
        mulpd     %xmm8, %xmm5                                  #1315.31
        subpd     %xmm8, %xmm5                                  #1315.19
        mulpd     %xmm6, %xmm5                                  #1316.21
        movaps    %xmm1, %xmm8                                  #1317.18
        mulpd     %xmm5, %xmm8                                  #1317.18
        mulpd     %xmm2, %xmm5                                  #1318.30
        mulpd     %xmm6, %xmm2                                  #1319.31
        addpd     %xmm5, %xmm8                                  #1318.18
        mulpd     %xmm6, %xmm1                                  #1320.29
        addpd     %xmm2, %xmm8                                  #1319.19
        movaps    %xmm8, %xmm12                                 #1320.17
        addpd     %xmm1, %xmm12                                 #1320.17
        movaps    %xmm12, %xmm11                                #1321.14
        mulpd     %xmm12, %xmm11                                #1321.14
        movaps    %xmm11, %xmm7                                 #1322.14
        mulpd     %xmm11, %xmm7                                 #1322.14
        movups    1792+__svml_datan_ha_data_internal(%rip), %xmm4 #1327.26
        mulpd     %xmm7, %xmm4                                  #1327.26
        movups    1856+__svml_datan_ha_data_internal(%rip), %xmm2 #1328.26
        addpd     1920+__svml_datan_ha_data_internal(%rip), %xmm4 #1327.14
        mulpd     %xmm7, %xmm2                                  #1328.26
        mulpd     %xmm7, %xmm4                                  #1331.26
        addpd     1984+__svml_datan_ha_data_internal(%rip), %xmm2 #1328.14
        addpd     2048+__svml_datan_ha_data_internal(%rip), %xmm4 #1331.14
        mulpd     %xmm7, %xmm2                                  #1332.26
        mulpd     %xmm7, %xmm4                                  #1335.26
        addpd     2112+__svml_datan_ha_data_internal(%rip), %xmm2 #1332.14
        addpd     2176+__svml_datan_ha_data_internal(%rip), %xmm4 #1335.14
        mulpd     %xmm7, %xmm2                                  #1336.26
        mulpd     %xmm7, %xmm4                                  #1339.26
        addpd     2240+__svml_datan_ha_data_internal(%rip), %xmm2 #1336.14
        addpd     2304+__svml_datan_ha_data_internal(%rip), %xmm4 #1339.14
        mulpd     %xmm7, %xmm2                                  #1340.26
        mulpd     %xmm7, %xmm4                                  #1343.26
        addpd     2368+__svml_datan_ha_data_internal(%rip), %xmm2 #1340.14
        addpd     2432+__svml_datan_ha_data_internal(%rip), %xmm4 #1343.14
        mulpd     %xmm2, %xmm7                                  #1344.26
        mulpd     %xmm11, %xmm4                                 #1345.26
        addpd     2496+__svml_datan_ha_data_internal(%rip), %xmm7 #1344.14
        addpd     %xmm4, %xmm7                                  #1345.14
        mulpd     %xmm7, %xmm11                                 #1346.14
        mulpd     %xmm11, %xmm12                                #1348.26
        movq      24(%rax,%rsi), %xmm3                          #1289.652
        movhpd    24(%rcx,%rsi), %xmm3                          #1289.620
        addpd     %xmm8, %xmm3                                  #1347.19
        addpd     %xmm12, %xmm3                                 #1348.14
        addpd     %xmm3, %xmm1                                  #1349.18
        movq      16(%rax,%rsi), %xmm0                          #1288.652
        movhpd    16(%rcx,%rsi), %xmm0                          #1288.620
        addpd     %xmm1, %xmm0                                  #1350.18
        orps      %xmm9, %xmm0                                  #1351.14
        andl      $3, %edx                                      #1267.93
        jne       ..B5.3        # Prob 5%                       #1353.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm10
..B5.2:                         # Preds ..B5.3 ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1356.12
        popq      %rbp                                          #1356.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1356.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm10, 64(%rsp)                              #1353.197
        movups    %xmm0, 128(%rsp)                              #1353.270
        je        ..B5.2        # Prob 95%                      #1353.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1353.454
        movq      %r12, 8(%rsp)                                 #1353.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1353.454
        movq      %r13, (%rsp)                                  #1353.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1353.454
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1353.517
        jc        ..B5.10       # Prob 5%                       #1353.517
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1353.470
        cmpl      $2, %r12d                                     #1353.465
        jl        ..B5.7        # Prob 82%                      #1353.465
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1353.674
        jmp       ..B5.2        # Prob 100%                     #1353.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1353.546
        lea       128(%rsp,%r12,8), %rsi                        #1353.546
..___tag_value___svml_atan2_ha_ex.80:
#       __svml_datan_ha_cout_rare_internal(const double *, double *)
        call      __svml_datan_ha_cout_rare_internal            #1353.546
..___tag_value___svml_atan2_ha_ex.81:
        jmp       ..B5.8        # Prob 100%                     #1353.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_atan2_ha_ex,@function
	.size	__svml_atan2_ha_ex,.-__svml_atan2_ha_ex
..LN__svml_atan2_ha_ex.4:
	.data
# -- End  __svml_atan2_ha_ex
	.text
.L_2__routine_start___svml_atan1_ha_l9_5:
# -- Begin  __svml_atan1_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan1_ha_l9
# --- __svml_atan1_ha_l9(__m128d)
__svml_atan1_ha_l9:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1361.1
        .byte     15                                            #1490.546
        .byte     30                                            #1490.546
        .byte     250                                           #1490.546
	.cfi_startproc
..___tag_value___svml_atan1_ha_l9.83:
..L84:
                                                         #1361.1
        pushq     %rbp                                          #1361.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1361.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1361.1
        subq      $128, %rsp                                    #1361.1
        movl      $-2144337920, %eax                            #1419.17
        movl      $1072037888, %esi                             #1429.29
        movl      $-36700160, %edx                              #1420.26
        movl      $1071382528, %ecx                             #1428.29
        movl      $1072889856, %edi                             #1430.29
        movl      $1073971200, %r8d                             #1431.29
        vmovapd   %xmm0, %xmm4                                  #1361.1
        vmovsd    1152+__svml_datan_ha_data_internal(%rip), %xmm5 #1413.20
        vmovd     %eax, %xmm6                                   #1419.17
        vandpd    %xmm5, %xmm4, %xmm2                           #1414.14
        vmovd     %esi, %xmm14                                  #1429.29
        vpshufd   $85, %xmm2, %xmm1                             #1417.16
        vmovd     %edx, %xmm8                                   #1420.26
        vpsubd    %xmm6, %xmm1, %xmm7                           #1419.17
        vmovd     %ecx, %xmm12                                  #1428.29
        vpsubd    %xmm1, %xmm14, %xmm15                         #1429.29
        vmovd     %edi, %xmm14                                  #1430.29
        vmovd     %r8d, %xmm5                                   #1431.29
        movl      $4, %r9d                                      #1441.183
        vpcmpgtd  %xmm8, %xmm7, %xmm9                           #1421.32
        vpcmpeqd  %xmm8, %xmm7, %xmm10                          #1421.82
        vpsubd    %xmm1, %xmm12, %xmm13                         #1428.29
        vpsrad    $31, %xmm15, %xmm7                            #1433.28
        vpsubd    %xmm1, %xmm14, %xmm15                         #1430.29
        vpsubd    %xmm1, %xmm5, %xmm1                           #1431.29
        vpor      %xmm10, %xmm9, %xmm11                         #1421.18
        vpsrad    $31, %xmm13, %xmm6                            #1432.28
        vpsrad    $31, %xmm15, %xmm8                            #1434.28
        vpsrad    $31, %xmm1, %xmm9                             #1435.28
        vmovmskps %xmm11, %eax                                  #1422.44
        vpaddd    %xmm7, %xmm6, %xmm10                          #1441.183
        vpaddd    %xmm9, %xmm8, %xmm11                          #1441.183
        vmovd     %r9d, %xmm13                                  #1441.183
        vpaddd    %xmm11, %xmm10, %xmm12                        #1441.183
        vxorpd    %xmm2, %xmm4, %xmm3                           #1415.17
        vpaddd    %xmm13, %xmm12, %xmm1                         #1441.183
        lea       __svml_datan_ha_data_internal(%rip), %r11     #1441.421
        vpslld    $5, %xmm1, %xmm5                              #1441.183
        vmovd     %xmm5, %r10d                                  #1441.242
        vmovsd    1728+__svml_datan_ha_data_internal(%rip), %xmm0 #1423.15
        vmovq     8(%r11,%r10), %xmm7                           #1442.418
        vmovq     (%r10,%r11), %xmm10                           #1441.421
        vandpd    %xmm2, %xmm7, %xmm6                           #1445.24
        vandpd    %xmm0, %xmm7, %xmm8                           #1447.24
        vsubsd    %xmm10, %xmm6, %xmm9                          #1446.23
        vfmadd213sd %xmm8, %xmm2, %xmm10                        #1448.23
        vcvtpd2ps %xmm10, %xmm2                                 #1449.54
        vmovlhps  %xmm2, %xmm2, %xmm2                           #1449.38
        vmovaps   %xmm10, %xmm8                                 #1450.17
        vrcpps    %xmm2, %xmm7                                  #1449.27
        vcvtps2pd %xmm7, %xmm1                                  #1449.14
        vfnmadd213sd %xmm0, %xmm1, %xmm8                        #1450.17
        vmovaps   %xmm10, %xmm6                                 #1453.21
        vfmadd213sd %xmm8, %xmm8, %xmm8                         #1451.17
        vfmadd213sd %xmm1, %xmm8, %xmm1                         #1452.18
        vfnmadd213sd %xmm0, %xmm1, %xmm6                        #1453.21
        vfmadd213sd %xmm1, %xmm6, %xmm1                         #1454.22
        vmulsd    %xmm9, %xmm1, %xmm5                           #1455.17
        vfnmadd213sd %xmm9, %xmm5, %xmm10                       #1456.21
        vmovsd    1792+__svml_datan_ha_data_internal(%rip), %xmm9 #1460.18
        vmulsd    %xmm1, %xmm10, %xmm2                          #1457.19
        vmulsd    %xmm5, %xmm5, %xmm10                          #1458.14
        vmulsd    %xmm10, %xmm10, %xmm0                         #1459.14
        vmovsd    1856+__svml_datan_ha_data_internal(%rip), %xmm1 #1461.18
        vfmadd213sd 1920+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm9 #1464.14
        vfmadd213sd 1984+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm1 #1465.14
        vfmadd213sd 2048+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm9 #1468.14
        vfmadd213sd 2112+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm1 #1469.14
        vfmadd213sd 2176+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm9 #1472.14
        vfmadd213sd 2240+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm1 #1473.14
        vfmadd213sd 2304+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm9 #1476.14
        vfmadd213sd 2368+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm1 #1477.14
        vfmadd213sd 2432+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm9 #1480.14
        vfmadd213sd 2496+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm1 #1481.14
        vaddsd    24(%r11,%r10), %xmm2, %xmm0                   #1484.19
        vfmadd213sd %xmm1, %xmm10, %xmm9                        #1482.14
        vmulsd    %xmm10, %xmm9, %xmm6                          #1483.14
        vfmadd213sd %xmm0, %xmm5, %xmm6                         #1485.14
        vaddsd    %xmm6, %xmm5, %xmm7                           #1486.18
        vaddsd    16(%r11,%r10), %xmm7, %xmm8                   #1487.18
        vorpd     %xmm3, %xmm8, %xmm0                           #1488.14
        andl      $1, %eax                                      #1422.93
        jne       ..B6.3        # Prob 5%                       #1490.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm4
..B6.2:                         # Preds ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1493.12
        popq      %rbp                                          #1493.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1493.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm4, (%rsp)                                 #1490.160
        vmovsd    %xmm0, 64(%rsp)                               #1490.233
        jne       ..B6.5        # Prob 5%                       #1490.374
                                # LOE rbx r12 r13 r14 r15 eax
..B6.4:                         # Preds ..B6.6 ..B6.5 ..B6.3
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm0                               #1490.626
        movq      %rbp, %rsp                                    #1490.626
        popq      %rbp                                          #1490.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1490.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B6.5:                         # Preds ..B6.3
                                # Execution count [2.50e-03]: Infreq
        je        ..B6.4        # Prob 95%                      #1490.517
                                # LOE rbx r12 r13 r14 r15
..B6.6:                         # Preds ..B6.5
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1490.546
        lea       64(%rsp), %rsi                                #1490.546
..___tag_value___svml_atan1_ha_l9.96:
#       __svml_datan_ha_cout_rare_internal(const double *, double *)
        call      __svml_datan_ha_cout_rare_internal            #1490.546
..___tag_value___svml_atan1_ha_l9.97:
        jmp       ..B6.4        # Prob 100%                     #1490.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_atan1_ha_l9,@function
	.size	__svml_atan1_ha_l9,.-__svml_atan1_ha_l9
..LN__svml_atan1_ha_l9.5:
	.data
# -- End  __svml_atan1_ha_l9
	.text
.L_2__routine_start___svml_atan2_ha_l9_6:
# -- Begin  __svml_atan2_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan2_ha_l9
# --- __svml_atan2_ha_l9(__m128d)
__svml_atan2_ha_l9:
# parameter 1: %xmm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1498.1
        .byte     15                                            #1627.546
        .byte     30                                            #1627.546
        .byte     250                                           #1627.546
	.cfi_startproc
..___tag_value___svml_atan2_ha_l9.99:
..L100:
                                                        #1498.1
        pushq     %rbp                                          #1498.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1498.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1498.1
        subq      $192, %rsp                                    #1498.1
        vmovapd   %xmm0, %xmm8                                  #1498.1
        vandpd    1152+__svml_datan_ha_data_internal(%rip), %xmm8, %xmm6 #1551.14
        lea       __svml_datan_ha_data_internal(%rip), %rsi     #1578.377
        vpshufd   $221, %xmm6, %xmm5                            #1554.16
        vxorpd    %xmm6, %xmm8, %xmm7                           #1552.17
        vmovq     2560+__svml_datan_ha_data_internal(%rip), %xmm9 #1555.26
        vmovq     2624+__svml_datan_ha_data_internal(%rip), %xmm11 #1557.26
        vpsubd    %xmm9, %xmm5, %xmm10                          #1556.17
        vmovq     1408+__svml_datan_ha_data_internal(%rip), %xmm15 #1561.29
        vpcmpgtd  %xmm11, %xmm10, %xmm12                        #1558.32
        vmovq     1472+__svml_datan_ha_data_internal(%rip), %xmm9 #1562.29
        vpcmpeqd  %xmm11, %xmm10, %xmm13                        #1558.82
        vmovq     1536+__svml_datan_ha_data_internal(%rip), %xmm10 #1563.29
        vpor      %xmm13, %xmm12, %xmm14                        #1558.18
        vmovq     1600+__svml_datan_ha_data_internal(%rip), %xmm1 #1564.29
        vpsubd    %xmm5, %xmm15, %xmm2                          #1565.29
        vpsubd    %xmm5, %xmm1, %xmm0                           #1566.29
        vpsubd    %xmm5, %xmm9, %xmm15                          #1567.29
        vpsubd    %xmm5, %xmm10, %xmm5                          #1568.29
        vpsrad    $31, %xmm2, %xmm11                            #1569.28
        vmovmskps %xmm14, %edx                                  #1559.44
        vpsrad    $31, %xmm0, %xmm12                            #1570.28
        vpsrad    $31, %xmm15, %xmm13                           #1571.28
        vpsrad    $31, %xmm5, %xmm14                            #1572.28
        vpaddd    %xmm12, %xmm11, %xmm2                         #1578.183
        vpaddd    %xmm14, %xmm13, %xmm1                         #1578.183
        vmovq     1664+__svml_datan_ha_data_internal(%rip), %xmm3 #1576.19
        vpaddd    %xmm1, %xmm2, %xmm0                           #1578.183
        vpaddd    %xmm3, %xmm0, %xmm3                           #1578.183
        vpslld    $5, %xmm3, %xmm2                              #1578.183
        vmovd     %xmm2, %eax                                   #1578.242
        vmovupd   1728+__svml_datan_ha_data_internal(%rip), %xmm4 #1560.46
        vpextrd   $1, %xmm2, %ecx                               #1578.306
        vmovq     8(%rax,%rsi), %xmm0                           #1579.612
        vmovq     (%rax,%rsi), %xmm1                            #1578.615
        vmovhpd   8(%rcx,%rsi), %xmm0, %xmm10                   #1579.580
        vmovhpd   (%rcx,%rsi), %xmm1, %xmm11                    #1578.583
        vandpd    %xmm4, %xmm10, %xmm12                         #1584.24
        vfmadd231pd %xmm11, %xmm6, %xmm12                       #1585.23
        vandpd    %xmm6, %xmm10, %xmm9                          #1582.24
        vmovq     24(%rax,%rsi), %xmm5                          #1581.621
        vmovhpd   24(%rcx,%rsi), %xmm5, %xmm1                   #1581.589
        vmovq     16(%rax,%rsi), %xmm3                          #1580.621
        vmovhpd   16(%rcx,%rsi), %xmm3, %xmm2                   #1580.589
        vmovapd   %xmm4, %xmm3                                  #1587.17
        vcvtpd2ps %xmm12, %xmm6                                 #1586.54
        vsubpd    %xmm11, %xmm9, %xmm0                          #1583.23
        vmovlhps  %xmm6, %xmm6, %xmm6                           #1586.38
        vrcpps    %xmm6, %xmm11                                 #1586.27
        vcvtps2pd %xmm11, %xmm5                                 #1586.14
        vfnmadd231pd %xmm12, %xmm5, %xmm3                       #1587.17
        vmovupd   1792+__svml_datan_ha_data_internal(%rip), %xmm6 #1597.49
        vfmadd213pd %xmm3, %xmm3, %xmm3                         #1588.17
        vfmadd213pd %xmm5, %xmm3, %xmm5                         #1589.18
        vmovupd   1856+__svml_datan_ha_data_internal(%rip), %xmm3 #1602.14
        vfnmadd231pd %xmm12, %xmm5, %xmm4                       #1590.21
        vfmadd213pd %xmm5, %xmm4, %xmm5                         #1591.22
        vmulpd    %xmm5, %xmm0, %xmm10                          #1592.17
        vmulpd    %xmm10, %xmm10, %xmm4                         #1595.14
        vfnmadd213pd %xmm0, %xmm10, %xmm12                      #1593.21
        vmulpd    %xmm4, %xmm4, %xmm0                           #1596.14
        vmulpd    %xmm12, %xmm5, %xmm9                          #1594.19
        vfmadd213pd 1920+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm6 #1601.14
        vfmadd213pd 1984+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm3 #1602.14
        vaddpd    %xmm9, %xmm1, %xmm1                           #1621.19
        vfmadd213pd 2048+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm6 #1605.14
        vfmadd213pd 2112+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm3 #1606.14
        vfmadd213pd 2176+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm6 #1609.14
        vfmadd213pd 2240+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm3 #1610.14
        vfmadd213pd 2304+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm6 #1613.14
        vfmadd213pd 2368+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm3 #1614.14
        vfmadd213pd 2432+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm6 #1617.14
        vfmadd213pd 2496+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm3 #1618.14
        vfmadd213pd %xmm3, %xmm4, %xmm6                         #1619.14
        vmulpd    %xmm6, %xmm4, %xmm11                          #1620.14
        vfmadd213pd %xmm1, %xmm10, %xmm11                       #1622.14
        vaddpd    %xmm11, %xmm10, %xmm13                        #1623.18
        vaddpd    %xmm13, %xmm2, %xmm2                          #1624.18
        vorpd     %xmm7, %xmm2, %xmm0                           #1625.14
        andl      $3, %edx                                      #1559.93
        jne       ..B7.3        # Prob 5%                       #1627.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm8
..B7.2:                         # Preds ..B7.3 ..B7.9 ..B7.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1630.12
        popq      %rbp                                          #1630.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1630.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.3:                         # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm8, 64(%rsp)                               #1627.197
        vmovupd   %xmm0, 128(%rsp)                              #1627.270
        je        ..B7.2        # Prob 95%                      #1627.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B7.6:                         # Preds ..B7.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1627.454
        movq      %r12, 8(%rsp)                                 #1627.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1627.454
        movq      %r13, (%rsp)                                  #1627.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1627.454
                                # LOE rbx r12 r14 r15 r13d
..B7.7:                         # Preds ..B7.8 ..B7.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1627.517
        jc        ..B7.10       # Prob 5%                       #1627.517
                                # LOE rbx r12 r14 r15 r13d
..B7.8:                         # Preds ..B7.10 ..B7.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1627.470
        cmpl      $2, %r12d                                     #1627.465
        jl        ..B7.7        # Prob 82%                      #1627.465
                                # LOE rbx r12 r14 r15 r13d
..B7.9:                         # Preds ..B7.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1627.674
        jmp       ..B7.2        # Prob 100%                     #1627.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B7.10:                        # Preds ..B7.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1627.546
        lea       128(%rsp,%r12,8), %rsi                        #1627.546
..___tag_value___svml_atan2_ha_l9.117:
#       __svml_datan_ha_cout_rare_internal(const double *, double *)
        call      __svml_datan_ha_cout_rare_internal            #1627.546
..___tag_value___svml_atan2_ha_l9.118:
        jmp       ..B7.8        # Prob 100%                     #1627.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_atan2_ha_l9,@function
	.size	__svml_atan2_ha_l9,.-__svml_atan2_ha_l9
..LN__svml_atan2_ha_l9.6:
	.data
# -- End  __svml_atan2_ha_l9
	.text
.L_2__routine_start___svml_atan1_ha_ex_7:
# -- Begin  __svml_atan1_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan1_ha_ex
# --- __svml_atan1_ha_ex(__m128d)
__svml_atan1_ha_ex:
# parameter 1: %xmm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1635.1
        .byte     15                                            #1785.546
        .byte     30                                            #1785.546
        .byte     250                                           #1785.546
	.cfi_startproc
..___tag_value___svml_atan1_ha_ex.120:
..L121:
                                                        #1635.1
        pushq     %rbp                                          #1635.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1635.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1635.1
        subq      $128, %rsp                                    #1635.1
        movl      $1071382528, %ecx                             #1705.27
        movl      $1072037888, %esi                             #1706.27
        movl      $1072889856, %edi                             #1707.27
        movl      $1073971200, %r8d                             #1708.27
        movaps    %xmm0, %xmm8                                  #1635.1
        movsd     1152+__svml_datan_ha_data_internal(%rip), %xmm9 #1687.20
        movl      $4, %r9d                                      #1718.181
        movsd     1344+__svml_datan_ha_data_internal(%rip), %xmm6 #1690.28
        andps     %xmm8, %xmm9                                  #1688.14
        movaps    %xmm6, %xmm0                                  #1691.19
        movaps    %xmm9, %xmm7                                  #1689.17
        pshufd    $85, %xmm9, %xmm14                            #1694.14
        andps     %xmm9, %xmm0                                  #1691.19
        movaps    %xmm9, %xmm5                                  #1692.19
        movd      %ecx, %xmm9                                   #1705.27
        movd      %esi, %xmm15                                  #1706.27
        movd      %edi, %xmm1                                   #1707.27
        movd      %r8d, %xmm3                                   #1708.27
        psubd     %xmm14, %xmm9                                 #1705.27
        psubd     %xmm14, %xmm15                                #1706.27
        psubd     %xmm14, %xmm1                                 #1707.27
        psubd     %xmm14, %xmm3                                 #1708.27
        psrad     $31, %xmm9                                    #1709.26
        psrad     $31, %xmm15                                   #1710.26
        psrad     $31, %xmm1                                    #1711.26
        psrad     $31, %xmm3                                    #1712.26
        paddd     %xmm15, %xmm9                                 #1718.181
        paddd     %xmm3, %xmm1                                  #1718.181
        movd      %r9d, %xmm2                                   #1718.181
        paddd     %xmm1, %xmm9                                  #1718.181
        movl      $-2144337920, %eax                            #1696.15
        paddd     %xmm2, %xmm9                                  #1718.181
        lea       __svml_datan_ha_data_internal(%rip), %r11     #1718.415
        pslld     $5, %xmm9                                     #1718.181
        movl      $-36700160, %edx                              #1697.24
        movd      %xmm9, %r10d                                  #1718.238
        movdqa    %xmm14, %xmm12                                #1696.15
        movd      %eax, %xmm10                                  #1696.15
        movaps    %xmm6, %xmm1                                  #1728.19
        psubd     %xmm10, %xmm12                                #1696.15
        pxor      %xmm8, %xmm7                                  #1689.17
        movq      (%r10,%r11), %xmm2                            #1718.415
        movd      %edx, %xmm11                                  #1697.24
        movdqa    %xmm2, %xmm14                                 #1732.32
        movdqa    %xmm12, %xmm13                                #1698.30
        mulsd     %xmm0, %xmm14                                 #1732.32
        pcmpgtd   %xmm11, %xmm13                                #1698.30
        subsd     %xmm0, %xmm5                                  #1692.19
        pcmpeqd   %xmm11, %xmm12                                #1698.76
        por       %xmm12, %xmm13                                #1698.16
        movmskps  %xmm13, %eax                                  #1699.44
        movsd     1728+__svml_datan_ha_data_internal(%rip), %xmm4 #1700.15
        movq      8(%r11,%r10), %xmm13                          #1719.412
        movdqa    %xmm13, %xmm10                                #1722.20
        movdqa    %xmm13, %xmm12                                #1724.20
        andps     %xmm4, %xmm13                                 #1731.20
        andps     %xmm0, %xmm10                                 #1722.20
        andps     %xmm5, %xmm12                                 #1724.20
        addsd     %xmm13, %xmm14                                #1732.20
        subsd     %xmm2, %xmm10                                 #1723.20
        mulsd     %xmm5, %xmm2                                  #1733.20
        movaps    %xmm14, %xmm0                                 #1734.20
        movaps    %xmm10, %xmm3                                 #1725.20
        addsd     %xmm2, %xmm0                                  #1734.20
        subsd     %xmm12, %xmm3                                 #1725.20
        subsd     %xmm0, %xmm14                                 #1735.23
        andps     %xmm0, %xmm6                                  #1737.19
        movaps    %xmm3, %xmm11                                 #1726.21
        cvtpd2ps  %xmm6, %xmm5                                  #1740.54
        addsd     %xmm14, %xmm2                                 #1736.20
        subsd     %xmm6, %xmm0                                  #1738.22
        subsd     %xmm10, %xmm11                                #1726.21
        addsd     %xmm0, %xmm2                                  #1739.19
        subsd     %xmm11, %xmm12                                #1727.20
        movlhps   %xmm5, %xmm5                                  #1740.38
        andps     %xmm3, %xmm1                                  #1728.19
        rcpps     %xmm5, %xmm15                                 #1740.27
        subsd     %xmm1, %xmm3                                  #1729.22
        cvtps2pd  %xmm15, %xmm0                                 #1740.14
        addsd     %xmm12, %xmm3                                 #1730.19
        mulsd     %xmm0, %xmm6                                  #1741.31
        mulsd     %xmm0, %xmm2                                  #1742.29
        subsd     %xmm4, %xmm6                                  #1741.19
        movaps    %xmm0, %xmm9                                  #1751.31
        addsd     %xmm6, %xmm2                                  #1742.17
        mulsd     %xmm3, %xmm9                                  #1751.31
        movaps    %xmm2, %xmm5                                  #1743.31
        mulsd     %xmm2, %xmm5                                  #1743.31
        movsd     1792+__svml_datan_ha_data_internal(%rip), %xmm10 #1759.26
        subsd     %xmm2, %xmm5                                  #1743.19
        mulsd     %xmm2, %xmm5                                  #1744.31
        addsd     %xmm2, %xmm5                                  #1744.19
        mulsd     %xmm2, %xmm5                                  #1745.31
        subsd     %xmm2, %xmm5                                  #1745.19
        mulsd     %xmm2, %xmm5                                  #1746.31
        addsd     %xmm2, %xmm5                                  #1746.19
        mulsd     %xmm2, %xmm5                                  #1747.31
        subsd     %xmm2, %xmm5                                  #1747.19
        mulsd     %xmm0, %xmm5                                  #1748.21
        mulsd     %xmm1, %xmm0                                  #1752.29
        movaps    %xmm5, %xmm4                                  #1749.18
        movaps    %xmm0, %xmm6                                  #1752.17
        mulsd     %xmm1, %xmm4                                  #1749.18
        mulsd     %xmm3, %xmm5                                  #1750.30
        movsd     1856+__svml_datan_ha_data_internal(%rip), %xmm3 #1760.26
        addsd     %xmm4, %xmm5                                  #1750.18
        addsd     %xmm5, %xmm9                                  #1751.19
        addsd     %xmm9, %xmm6                                  #1752.17
        addsd     24(%r11,%r10), %xmm9                          #1779.19
        movaps    %xmm6, %xmm2                                  #1753.14
        mulsd     %xmm6, %xmm2                                  #1753.14
        movaps    %xmm2, %xmm1                                  #1754.14
        mulsd     %xmm2, %xmm1                                  #1754.14
        mulsd     %xmm1, %xmm10                                 #1759.26
        mulsd     %xmm1, %xmm3                                  #1760.26
        addsd     1920+__svml_datan_ha_data_internal(%rip), %xmm10 #1759.14
        addsd     1984+__svml_datan_ha_data_internal(%rip), %xmm3 #1760.14
        mulsd     %xmm1, %xmm10                                 #1763.26
        mulsd     %xmm1, %xmm3                                  #1764.26
        addsd     2048+__svml_datan_ha_data_internal(%rip), %xmm10 #1763.14
        addsd     2112+__svml_datan_ha_data_internal(%rip), %xmm3 #1764.14
        mulsd     %xmm1, %xmm10                                 #1767.26
        mulsd     %xmm1, %xmm3                                  #1768.26
        addsd     2176+__svml_datan_ha_data_internal(%rip), %xmm10 #1767.14
        addsd     2240+__svml_datan_ha_data_internal(%rip), %xmm3 #1768.14
        mulsd     %xmm1, %xmm10                                 #1771.26
        mulsd     %xmm1, %xmm3                                  #1772.26
        addsd     2304+__svml_datan_ha_data_internal(%rip), %xmm10 #1771.14
        addsd     2368+__svml_datan_ha_data_internal(%rip), %xmm3 #1772.14
        mulsd     %xmm1, %xmm10                                 #1775.26
        mulsd     %xmm1, %xmm3                                  #1776.26
        addsd     2432+__svml_datan_ha_data_internal(%rip), %xmm10 #1775.14
        addsd     2496+__svml_datan_ha_data_internal(%rip), %xmm3 #1776.14
        mulsd     %xmm2, %xmm10                                 #1777.26
        addsd     %xmm3, %xmm10                                 #1777.14
        mulsd     %xmm2, %xmm10                                 #1778.14
        mulsd     %xmm6, %xmm10                                 #1780.26
        addsd     %xmm9, %xmm10                                 #1780.14
        addsd     %xmm10, %xmm0                                 #1781.18
        addsd     16(%r11,%r10), %xmm0                          #1782.18
        orps      %xmm7, %xmm0                                  #1783.14
        andl      $1, %eax                                      #1699.93
        jne       ..B8.3        # Prob 5%                       #1785.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm8
..B8.2:                         # Preds ..B8.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1788.12
        popq      %rbp                                          #1788.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1788.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.3:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm8, (%rsp)                                 #1785.160
        movsd     %xmm0, 64(%rsp)                               #1785.233
        jne       ..B8.6        # Prob 5%                       #1785.374
                                # LOE rbx r12 r13 r14 r15
..B8.4:                         # Preds ..B8.6 ..B8.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #1785.626
        movq      %rbp, %rsp                                    #1785.626
        popq      %rbp                                          #1785.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1785.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B8.6:                         # Preds ..B8.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1785.546
        lea       64(%rsp), %rsi                                #1785.546
..___tag_value___svml_atan1_ha_ex.133:
#       __svml_datan_ha_cout_rare_internal(const double *, double *)
        call      __svml_datan_ha_cout_rare_internal            #1785.546
..___tag_value___svml_atan1_ha_ex.134:
        jmp       ..B8.4        # Prob 100%                     #1785.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_atan1_ha_ex,@function
	.size	__svml_atan1_ha_ex,.-__svml_atan1_ha_ex
..LN__svml_atan1_ha_ex.7:
	.data
# -- End  __svml_atan1_ha_ex
	.text
.L_2__routine_start___svml_atan4_ha_l9_8:
# -- Begin  __svml_atan4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan4_ha_l9
# --- __svml_atan4_ha_l9(__m256d)
__svml_atan4_ha_l9:
# parameter 1: %ymm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1793.1
        .byte     15                                            #1922.552
        .byte     30                                            #1922.552
        .byte     250                                           #1922.552
	.cfi_startproc
..___tag_value___svml_atan4_ha_l9.136:
..L137:
                                                        #1793.1
        pushq     %rbp                                          #1793.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1793.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1793.1
        subq      $192, %rsp                                    #1793.1
        lea       __svml_datan_ha_data_internal(%rip), %r8      #1873.513
        vmovapd   %ymm0, %ymm8                                  #1793.1
        vandpd    1152+__svml_datan_ha_data_internal(%rip), %ymm8, %ymm6 #1846.14
        vxorpd    %ymm6, %ymm8, %ymm7                           #1847.17
        vmovups   2624+__svml_datan_ha_data_internal(%rip), %xmm11 #1852.26
        vmovups   1408+__svml_datan_ha_data_internal(%rip), %xmm15 #1856.29
        vmovups   1472+__svml_datan_ha_data_internal(%rip), %xmm2 #1857.29
        vmovups   1600+__svml_datan_ha_data_internal(%rip), %xmm0 #1859.29
        vmovupd   1728+__svml_datan_ha_data_internal(%rip), %ymm4 #1855.49
        vextractf128 $1, %ymm6, %xmm9                           #1849.124
        vshufps   $221, %xmm9, %xmm6, %xmm5                     #1849.34
        vpsubd    2560+__svml_datan_ha_data_internal(%rip), %xmm5, %xmm10 #1851.17
        vpsubd    %xmm5, %xmm15, %xmm1                          #1860.29
        vpcmpgtd  %xmm11, %xmm10, %xmm12                        #1853.32
        vpcmpeqd  %xmm11, %xmm10, %xmm13                        #1853.82
        vmovups   1536+__svml_datan_ha_data_internal(%rip), %xmm10 #1858.29
        vpor      %xmm13, %xmm12, %xmm14                        #1853.18
        vpsubd    %xmm5, %xmm0, %xmm3                           #1861.29
        vpsubd    %xmm5, %xmm2, %xmm9                           #1862.29
        vpsubd    %xmm5, %xmm10, %xmm5                          #1863.29
        vpsrad    $31, %xmm1, %xmm11                            #1864.28
        vmovmskps %xmm14, %eax                                  #1854.44
        vpsrad    $31, %xmm3, %xmm12                            #1865.28
        vpsrad    $31, %xmm9, %xmm13                            #1866.28
        vpsrad    $31, %xmm5, %xmm14                            #1867.28
        vpaddd    %xmm12, %xmm11, %xmm15                        #1873.183
        vpaddd    %xmm14, %xmm13, %xmm1                         #1873.183
        vpaddd    %xmm1, %xmm15, %xmm0                          #1873.183
        vpaddd    1664+__svml_datan_ha_data_internal(%rip), %xmm0, %xmm3 #1873.183
        vpslld    $5, %xmm3, %xmm9                              #1873.183
        vmovd     %xmm9, %edx                                   #1873.242
        vpextrd   $2, %xmm9, %esi                               #1873.373
        vpextrd   $1, %xmm9, %ecx                               #1873.306
        vpextrd   $3, %xmm9, %edi                               #1873.440
        vmovq     8(%rdx,%r8), %xmm12                           #1874.1026
        vmovq     8(%rsi,%r8), %xmm14                           #1874.1226
        vmovq     (%rdx,%r8), %xmm2                             #1873.1029
        vmovhpd   8(%rcx,%r8), %xmm12, %xmm13                   #1874.994
        vmovq     (%rsi,%r8), %xmm10                            #1873.1229
        vmovhpd   8(%rdi,%r8), %xmm14, %xmm15                   #1874.1194
        vmovhpd   (%rcx,%r8), %xmm2, %xmm5                      #1873.997
        vmovhpd   (%rdi,%r8), %xmm10, %xmm11                    #1873.1197
        vmovq     16(%rdx,%r8), %xmm1                           #1875.1035
        vmovhpd   16(%rcx,%r8), %xmm1, %xmm0                    #1875.1003
        vmovq     24(%rdx,%r8), %xmm10                          #1876.1035
        vmovq     24(%rsi,%r8), %xmm12                          #1876.1235
        vinsertf128 $1, %xmm15, %ymm13, %ymm2                   #1874.948
        vandpd    %ymm6, %ymm2, %ymm14                          #1877.24
        vandpd    %ymm4, %ymm2, %ymm2                           #1879.24
        vmovhpd   24(%rdi,%r8), %xmm12, %xmm13                  #1876.1203
        vinsertf128 $1, %xmm11, %ymm5, %ymm3                    #1873.951
        vsubpd    %ymm3, %ymm14, %ymm15                         #1878.23
        vmovq     16(%rsi,%r8), %xmm5                           #1875.1235
        vmovhpd   16(%rdi,%r8), %xmm5, %xmm9                    #1875.1203
        vfmadd213pd %ymm2, %ymm6, %ymm3                         #1880.23
        vmovhpd   24(%rcx,%r8), %xmm10, %xmm11                  #1876.1003
        vcvtpd2ps %ymm3, %xmm6                                  #1881.41
        vrcpps    %xmm6, %xmm2                                  #1881.30
        vmovapd   %ymm4, %ymm5                                  #1882.17
        vinsertf128 $1, %xmm9, %ymm0, %ymm1                     #1875.957
        vcvtps2pd %xmm2, %ymm9                                  #1881.14
        vmovupd   1856+__svml_datan_ha_data_internal(%rip), %ymm2 #1897.14
        vfnmadd231pd %ymm3, %ymm9, %ymm5                        #1882.17
        vfmadd213pd %ymm5, %ymm5, %ymm5                         #1883.17
        vfmadd213pd %ymm9, %ymm5, %ymm9                         #1884.18
        vmovupd   1792+__svml_datan_ha_data_internal(%rip), %ymm5 #1892.52
        vfnmadd231pd %ymm3, %ymm9, %ymm4                        #1885.21
        vfmadd213pd %ymm9, %ymm4, %ymm9                         #1886.22
        vmulpd    %ymm9, %ymm15, %ymm10                         #1887.17
        vfnmadd213pd %ymm15, %ymm10, %ymm3                      #1888.21
        vmulpd    %ymm10, %ymm10, %ymm4                         #1890.14
        vmulpd    %ymm3, %ymm9, %ymm6                           #1889.19
        vmulpd    %ymm4, %ymm4, %ymm3                           #1891.14
        vfmadd213pd 1920+__svml_datan_ha_data_internal(%rip), %ymm3, %ymm5 #1896.14
        vfmadd213pd 1984+__svml_datan_ha_data_internal(%rip), %ymm3, %ymm2 #1897.14
        vfmadd213pd 2048+__svml_datan_ha_data_internal(%rip), %ymm3, %ymm5 #1900.14
        vfmadd213pd 2112+__svml_datan_ha_data_internal(%rip), %ymm3, %ymm2 #1901.14
        vfmadd213pd 2176+__svml_datan_ha_data_internal(%rip), %ymm3, %ymm5 #1904.14
        vfmadd213pd 2240+__svml_datan_ha_data_internal(%rip), %ymm3, %ymm2 #1905.14
        vfmadd213pd 2304+__svml_datan_ha_data_internal(%rip), %ymm3, %ymm5 #1908.14
        vfmadd213pd 2368+__svml_datan_ha_data_internal(%rip), %ymm3, %ymm2 #1909.14
        vfmadd213pd 2432+__svml_datan_ha_data_internal(%rip), %ymm3, %ymm5 #1912.14
        vfmadd213pd 2496+__svml_datan_ha_data_internal(%rip), %ymm3, %ymm2 #1913.14
        vfmadd213pd %ymm2, %ymm4, %ymm5                         #1914.14
        vinsertf128 $1, %xmm13, %ymm11, %ymm0                   #1876.957
        vmulpd    %ymm5, %ymm4, %ymm11                          #1915.14
        vaddpd    %ymm6, %ymm0, %ymm0                           #1916.19
        vfmadd213pd %ymm0, %ymm10, %ymm11                       #1917.14
        vaddpd    %ymm11, %ymm10, %ymm12                        #1918.18
        vaddpd    %ymm12, %ymm1, %ymm1                          #1919.18
        vorpd     %ymm7, %ymm1, %ymm0                           #1920.14
        testl     %eax, %eax                                    #1922.52
        jne       ..B9.3        # Prob 5%                       #1922.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm8
..B9.2:                         # Preds ..B9.3 ..B9.9 ..B9.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1925.12
        popq      %rbp                                          #1925.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1925.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.3:                         # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm8, 64(%rsp)                               #1922.200
        vmovupd   %ymm0, 128(%rsp)                              #1922.276
        je        ..B9.2        # Prob 95%                      #1922.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B9.6:                         # Preds ..B9.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1922.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B9.13:                        # Preds ..B9.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B9.7:                         # Preds ..B9.8 ..B9.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1922.523
        jc        ..B9.10       # Prob 5%                       #1922.523
                                # LOE rbx r12 r14 r15 r13d
..B9.8:                         # Preds ..B9.10 ..B9.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1922.476
        cmpl      $4, %r12d                                     #1922.471
        jl        ..B9.7        # Prob 82%                      #1922.471
                                # LOE rbx r12 r14 r15 r13d
..B9.9:                         # Preds ..B9.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1922.683
        jmp       ..B9.2        # Prob 100%                     #1922.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B9.10:                        # Preds ..B9.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1922.552
        lea       128(%rsp,%r12,8), %rsi                        #1922.552
..___tag_value___svml_atan4_ha_l9.154:
#       __svml_datan_ha_cout_rare_internal(const double *, double *)
        call      __svml_datan_ha_cout_rare_internal            #1922.552
..___tag_value___svml_atan4_ha_l9.155:
        jmp       ..B9.8        # Prob 100%                     #1922.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_atan4_ha_l9,@function
	.size	__svml_atan4_ha_l9,.-__svml_atan4_ha_l9
..LN__svml_atan4_ha_l9.8:
	.data
# -- End  __svml_atan4_ha_l9
	.text
.L_2__routine_start___svml_datan_ha_cout_rare_internal_9:
# -- Begin  __svml_datan_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_datan_ha_cout_rare_internal
	.globl __svml_datan_ha_cout_rare_internal
# --- __svml_datan_ha_cout_rare_internal(const double *, double *)
__svml_datan_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1094.1
        .byte     15                                            #1195.15
        .byte     30                                            #1195.15
        .byte     250                                           #1195.15
	.cfi_startproc
..___tag_value___svml_datan_ha_cout_rare_internal.157:
..L158:
                                                        #1094.1
        movzwl    6(%rdi), %r8d                                 #1102.31
        andl      $32752, %r8d                                  #1102.31
        shrl      $4, %r8d                                      #1102.31
        cmpl      $2047, %r8d                                   #1102.57
        je        ..B10.12      # Prob 16%                      #1102.57
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 r8d
..B10.2:                        # Preds ..B10.1
                                # Execution count [8.40e-01]
        movq      (%rdi), %rdx                                  #1105.18
        movq      %rdx, -16(%rsp)                               #1105.9
        shrq      $56, %rdx                                     #1106.30
        movb      7(%rdi), %al                                  #1104.37
        andl      $127, %edx                                    #1106.30
        movb      %dl, -9(%rsp)                                 #1106.30
        movsd     -16(%rsp), %xmm0                              #1107.14
        shrb      $7, %al                                       #1104.37
        comisd    1888+_vmldAtanHATab(%rip), %xmm0              #1107.38
        movl      -12(%rsp), %ecx                               #1107.14
        jb        ..B10.6       # Prob 50%                      #1107.38
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 ecx r8d al xmm0
..B10.3:                        # Preds ..B10.2
                                # Execution count [4.20e-01]
        movsd     1896+_vmldAtanHATab(%rip), %xmm1              #1109.41
        comisd    %xmm0, %xmm1                                  #1109.41
        jbe       ..B10.5       # Prob 50%                      #1109.41
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 ecx r8d al xmm0
..B10.4:                        # Preds ..B10.3
                                # Execution count [2.10e-01]
        movl      4(%rdi), %edx                                 #1112.47
        movl      %ecx, %edi                                    #1117.107
        andl      $-524288, %ecx                                #1117.174
        andl      $-1048576, %edi                               #1117.107
        addl      $262144, %ecx                                 #1117.185
        movaps    %xmm0, %xmm9                                  #1118.30
        andl      $1048575, %ecx                                #1117.196
        movaps    %xmm0, %xmm10                                 #1120.62
        movsd     %xmm0, -56(%rsp)                              #1115.17
        orl       %ecx, %edi                                    #1117.196
        movl      $0, -56(%rsp)                                 #1116.38
        andl      $1048575, %edx                                #1112.47
        movl      %edi, -52(%rsp)                               #1117.38
        lea       _vmldAtanHATab(%rip), %rcx                    #1134.54
        movsd     1928+_vmldAtanHATab(%rip), %xmm4              #1119.62
        movsd     -56(%rsp), %xmm15                             #1121.35
        shll      $20, %r8d                                     #1111.68
        subsd     -56(%rsp), %xmm9                              #1118.30
        mulsd     1928+_vmldAtanHATab(%rip), %xmm10             #1120.62
        shlb      $7, %al                                       #1142.38
        mulsd     %xmm9, %xmm4                                  #1119.62
        movsd     %xmm4, -48(%rsp)                              #1119.17
        orl       %edx, %r8d                                    #1112.47
        movsd     -48(%rsp), %xmm5                              #1119.106
        addl      $-1069547520, %r8d                            #1113.27
        sarl      $18, %r8d                                     #1114.28
        subsd     %xmm9, %xmm5                                  #1119.116
        movsd     %xmm5, -40(%rsp)                              #1119.87
        andl      $-2, %r8d                                     #1134.74
        movsd     -48(%rsp), %xmm7                              #1119.144
        movsd     -40(%rsp), %xmm6                              #1119.154
        movslq    %r8d, %r8                                     #1134.54
        subsd     %xmm6, %xmm7                                  #1119.154
        movsd     %xmm7, -48(%rsp)                              #1119.125
        movsd     -48(%rsp), %xmm8                              #1119.192
        movsd     1904+_vmldAtanHATab(%rip), %xmm6              #1124.78
        subsd     %xmm8, %xmm9                                  #1119.192
        movsd     %xmm9, -40(%rsp)                              #1119.165
        movsd     -48(%rsp), %xmm2                              #1119.211
        movsd     -40(%rsp), %xmm3                              #1119.228
        movsd     %xmm10, -48(%rsp)                             #1120.17
        movsd     -48(%rsp), %xmm11                             #1120.106
        movsd     1904+_vmldAtanHATab(%rip), %xmm8              #1124.151
        subsd     -16(%rsp), %xmm11                             #1120.116
        movsd     %xmm11, -40(%rsp)                             #1120.87
        movsd     -48(%rsp), %xmm13                             #1120.144
        movsd     -40(%rsp), %xmm12                             #1120.154
        subsd     %xmm12, %xmm13                                #1120.154
        movsd     %xmm13, -48(%rsp)                             #1120.125
        movsd     -48(%rsp), %xmm14                             #1120.192
        subsd     %xmm14, %xmm0                                 #1120.192
        movsd     1904+_vmldAtanHATab(%rip), %xmm14             #1124.304
        movsd     %xmm0, -40(%rsp)                              #1120.165
        movsd     -48(%rsp), %xmm5                              #1120.211
        movsd     -40(%rsp), %xmm4                              #1120.228
        mulsd     %xmm15, %xmm5                                 #1121.35
        mulsd     %xmm15, %xmm4                                 #1122.35
        movaps    %xmm5, %xmm1                                  #1123.48
        addsd     %xmm4, %xmm1                                  #1123.48
        movsd     %xmm1, -48(%rsp)                              #1123.17
        movsd     -48(%rsp), %xmm0                              #1123.91
        subsd     %xmm0, %xmm5                                  #1123.91
        addsd     %xmm4, %xmm5                                  #1123.130
        movsd     1928+_vmldAtanHATab(%rip), %xmm4              #1126.65
        movsd     %xmm5, -40(%rsp)                              #1123.102
        movsd     -48(%rsp), %xmm11                             #1123.152
        movsd     -40(%rsp), %xmm1                              #1123.170
        addsd     %xmm11, %xmm6                                 #1124.78
        movsd     %xmm6, -48(%rsp)                              #1124.17
        movsd     -48(%rsp), %xmm7                              #1124.151
        subsd     %xmm7, %xmm8                                  #1124.151
        movsd     %xmm8, -40(%rsp)                              #1124.90
        movsd     -48(%rsp), %xmm10                             #1124.181
        movsd     -40(%rsp), %xmm9                              #1124.191
        addsd     %xmm9, %xmm10                                 #1124.191
        movsd     %xmm10, -32(%rsp)                             #1124.162
        movsd     -40(%rsp), %xmm12                             #1124.232
        movsd     1928+_vmldAtanHATab(%rip), %xmm10             #1128.129
        addsd     %xmm11, %xmm12                                #1124.232
        movsd     %xmm12, -40(%rsp)                             #1124.202
        movsd     -32(%rsp), %xmm13                             #1124.304
        movsd     1904+_vmldAtanHATab(%rip), %xmm11             #1128.75
        subsd     %xmm13, %xmm14                                #1124.304
        movsd     %xmm14, -32(%rsp)                             #1124.243
        movsd     -40(%rsp), %xmm0                              #1124.334
        movsd     -32(%rsp), %xmm15                             #1124.344
        addsd     %xmm15, %xmm0                                 #1124.344
        movsd     %xmm0, -32(%rsp)                              #1124.315
        movsd     -48(%rsp), %xmm9                              #1124.364
        mulsd     %xmm9, %xmm4                                  #1126.65
        movsd     -32(%rsp), %xmm0                              #1124.382
        movsd     %xmm4, -48(%rsp)                              #1126.17
        addsd     %xmm1, %xmm0                                  #1125.36
        movsd     -48(%rsp), %xmm5                              #1126.109
        subsd     %xmm9, %xmm5                                  #1126.119
        movsd     %xmm5, -40(%rsp)                              #1126.90
        movsd     -48(%rsp), %xmm7                              #1126.150
        movsd     -40(%rsp), %xmm6                              #1126.160
        subsd     %xmm6, %xmm7                                  #1126.160
        movsd     1904+_vmldAtanHATab(%rip), %xmm6              #1128.327
        movsd     %xmm7, -48(%rsp)                              #1126.131
        movsd     -48(%rsp), %xmm8                              #1126.201
        subsd     %xmm8, %xmm9                                  #1126.201
        movsd     %xmm9, -40(%rsp)                              #1126.171
        movsd     -48(%rsp), %xmm4                              #1126.220
        divsd     %xmm4, %xmm11                                 #1128.75
        mulsd     %xmm11, %xmm10                                #1128.129
        movsd     -40(%rsp), %xmm5                              #1126.238
        movsd     %xmm10, -40(%rsp)                             #1128.84
        addsd     %xmm0, %xmm5                                  #1127.34
        movsd     -40(%rsp), %xmm12                             #1128.172
        subsd     %xmm11, %xmm12                                #1128.182
        movsd     %xmm12, -32(%rsp)                             #1128.153
        movsd     -40(%rsp), %xmm10                             #1128.211
        movsd     -32(%rsp), %xmm13                             #1128.221
        subsd     %xmm13, %xmm10                                #1128.221
        movsd     %xmm10, -32(%rsp)                             #1128.192
        movsd     -32(%rsp), %xmm14                             #1128.258
        mulsd     %xmm14, %xmm4                                 #1128.258
        movsd     -32(%rsp), %xmm15                             #1128.364
        subsd     %xmm4, %xmm6                                  #1128.327
        mulsd     %xmm15, %xmm5                                 #1128.364
        movsd     %xmm5, -40(%rsp)                              #1128.337
        movsd     -40(%rsp), %xmm1                              #1128.403
        subsd     %xmm1, %xmm6                                  #1128.403
        movsd     %xmm6, -40(%rsp)                              #1128.375
        movsd     -40(%rsp), %xmm4                              #1128.472
        movsd     -32(%rsp), %xmm5                              #1128.491
        movsd     -40(%rsp), %xmm0                              #1128.527
        movaps    %xmm5, %xmm7                                  #1129.45
        movsd     -32(%rsp), %xmm1                              #1128.564
        mulsd     %xmm3, %xmm5                                  #1129.132
        addsd     1904+_vmldAtanHATab(%rip), %xmm4              #1128.472
        mulsd     %xmm2, %xmm7                                  #1129.45
        mulsd     %xmm0, %xmm4                                  #1128.527
        mulsd     %xmm1, %xmm4                                  #1128.564
        mulsd     %xmm4, %xmm3                                  #1129.84
        mulsd     %xmm4, %xmm2                                  #1129.181
        addsd     %xmm3, %xmm5                                  #1129.132
        movsd     1872+_vmldAtanHATab(%rip), %xmm6              #1132.75
        addsd     %xmm2, %xmm5                                  #1129.181
        movsd     %xmm5, -48(%rsp)                              #1129.143
        movaps    %xmm7, %xmm2                                  #1130.47
        movsd     -48(%rsp), %xmm4                              #1129.218
        addsd     %xmm4, %xmm2                                  #1130.47
        movsd     %xmm2, -48(%rsp)                              #1130.17
        movsd     -48(%rsp), %xmm3                              #1130.88
        movsd     (%rcx,%r8,8), %xmm2                           #1134.54
        subsd     %xmm3, %xmm7                                  #1130.88
        addsd     %xmm4, %xmm7                                  #1130.127
        movsd     %xmm7, -40(%rsp)                              #1130.99
        movsd     -48(%rsp), %xmm3                              #1130.147
        movaps    %xmm3, %xmm5                                  #1131.31
        movaps    %xmm3, %xmm0                                  #1134.85
        mulsd     %xmm3, %xmm5                                  #1131.31
        addsd     %xmm2, %xmm0                                  #1134.85
        mulsd     %xmm5, %xmm6                                  #1132.75
        movsd     -40(%rsp), %xmm10                             #1130.164
        movsd     %xmm0, -48(%rsp)                              #1134.17
        movsd     -48(%rsp), %xmm1                              #1134.163
        addsd     1864+_vmldAtanHATab(%rip), %xmm6              #1132.98
        subsd     %xmm1, %xmm2                                  #1134.163
        mulsd     %xmm5, %xmm6                                  #1132.120
        addsd     %xmm3, %xmm2                                  #1134.202
        addsd     1856+_vmldAtanHATab(%rip), %xmm6              #1132.143
        mulsd     %xmm5, %xmm6                                  #1132.165
        movsd     %xmm2, -40(%rsp)                              #1134.174
        movsd     -48(%rsp), %xmm9                              #1134.221
        movsd     -40(%rsp), %xmm8                              #1134.238
        addsd     1848+_vmldAtanHATab(%rip), %xmm6              #1133.54
        mulsd     %xmm5, %xmm6                                  #1133.76
        addsd     1840+_vmldAtanHATab(%rip), %xmm6              #1133.99
        mulsd     %xmm5, %xmm6                                  #1133.121
        addsd     1832+_vmldAtanHATab(%rip), %xmm6              #1133.144
        mulsd     %xmm5, %xmm6                                  #1133.166
        addsd     1824+_vmldAtanHATab(%rip), %xmm6              #1133.189
        mulsd     %xmm5, %xmm6                                  #1136.40
        mulsd     %xmm3, %xmm6                                  #1137.36
        addsd     %xmm6, %xmm10                                 #1138.36
        addsd     8(%rcx,%r8,8), %xmm10                         #1139.53
        addsd     %xmm8, %xmm10                                 #1141.35
        addsd     %xmm9, %xmm10                                 #1140.36
        movsd     %xmm10, -24(%rsp)                             #1141.17
        movb      -17(%rsp), %r9b                               #1142.38
        andb      $127, %r9b                                    #1142.38
        orb       %al, %r9b                                     #1142.38
        movb      %r9b, -17(%rsp)                               #1142.38
        movq      -24(%rsp), %rax                               #1143.24
        movq      %rax, (%rsi)                                  #1143.19
        jmp       ..B10.11      # Prob 100%                     #1143.19
                                # LOE rbx rbp r12 r13 r14 r15
..B10.5:                        # Preds ..B10.3
                                # Execution count [2.10e-01]
        movsd     1912+_vmldAtanHATab(%rip), %xmm0              #1147.42
        shlb      $7, %al                                       #1148.38
        addsd     1920+_vmldAtanHATab(%rip), %xmm0              #1147.82
        movsd     %xmm0, -24(%rsp)                              #1147.17
        movb      -17(%rsp), %dl                                #1148.38
        andb      $127, %dl                                     #1148.38
        orb       %al, %dl                                      #1148.38
        movb      %dl, -17(%rsp)                                #1148.38
        movq      -24(%rsp), %rax                               #1149.24
        movq      %rax, (%rsi)                                  #1149.19
        jmp       ..B10.11      # Prob 100%                     #1149.19
                                # LOE rbx rbp r12 r13 r14 r15
..B10.6:                        # Preds ..B10.2
                                # Execution count [4.20e-01]
        comisd    1880+_vmldAtanHATab(%rip), %xmm0              #1154.42
        jb        ..B10.8       # Prob 50%                      #1154.42
                                # LOE rbx rbp rsi r12 r13 r14 r15 al xmm0
..B10.7:                        # Preds ..B10.6
                                # Execution count [2.10e-01]
        movaps    %xmm0, %xmm1                                  #1156.29
        mulsd     %xmm0, %xmm1                                  #1156.29
        shlb      $7, %al                                       #1162.38
        movsd     1872+_vmldAtanHATab(%rip), %xmm2              #1157.53
        mulsd     %xmm1, %xmm2                                  #1157.75
        addsd     1864+_vmldAtanHATab(%rip), %xmm2              #1157.98
        mulsd     %xmm1, %xmm2                                  #1157.120
        addsd     1856+_vmldAtanHATab(%rip), %xmm2              #1157.143
        mulsd     %xmm1, %xmm2                                  #1157.165
        addsd     1848+_vmldAtanHATab(%rip), %xmm2              #1158.54
        mulsd     %xmm1, %xmm2                                  #1158.76
        addsd     1840+_vmldAtanHATab(%rip), %xmm2              #1158.99
        mulsd     %xmm1, %xmm2                                  #1158.121
        addsd     1832+_vmldAtanHATab(%rip), %xmm2              #1158.144
        mulsd     %xmm1, %xmm2                                  #1158.166
        addsd     1824+_vmldAtanHATab(%rip), %xmm2              #1158.189
        mulsd     %xmm1, %xmm2                                  #1159.38
        mulsd     %xmm0, %xmm2                                  #1160.33
        addsd     %xmm0, %xmm2                                  #1161.33
        movsd     %xmm2, -24(%rsp)                              #1161.17
        movb      -17(%rsp), %dl                                #1162.38
        andb      $127, %dl                                     #1162.38
        orb       %al, %dl                                      #1162.38
        movb      %dl, -17(%rsp)                                #1162.38
        movq      -24(%rsp), %rax                               #1163.24
        movq      %rax, (%rsi)                                  #1163.19
        jmp       ..B10.11      # Prob 100%                     #1163.19
                                # LOE rbx rbp r12 r13 r14 r15
..B10.8:                        # Preds ..B10.6
                                # Execution count [2.10e-01]
        movzwl    -10(%rsp), %edx                               #1167.43
        testl     $32752, %edx                                  #1167.43
        je        ..B10.10      # Prob 50%                      #1167.67
                                # LOE rbx rbp rsi r12 r13 r14 r15 al xmm0
..B10.9:                        # Preds ..B10.8
                                # Execution count [1.05e-01]
        movsd     1904+_vmldAtanHATab(%rip), %xmm1              #1169.48
        shlb      $7, %al                                       #1171.42
        addsd     %xmm0, %xmm1                                  #1169.71
        movsd     %xmm1, -48(%rsp)                              #1169.21
        movsd     -48(%rsp), %xmm0                              #1170.29
        mulsd     -16(%rsp), %xmm0                              #1170.39
        movsd     %xmm0, -24(%rsp)                              #1170.21
        movb      -17(%rsp), %dl                                #1171.42
        andb      $127, %dl                                     #1171.42
        orb       %al, %dl                                      #1171.42
        movb      %dl, -17(%rsp)                                #1171.42
        movq      -24(%rsp), %rax                               #1172.28
        movq      %rax, (%rsi)                                  #1172.23
        jmp       ..B10.11      # Prob 100%                     #1172.23
                                # LOE rbx rbp r12 r13 r14 r15
..B10.10:                       # Preds ..B10.8
                                # Execution count [1.05e-01]
        mulsd     %xmm0, %xmm0                                  #1176.37
        shlb      $7, %al                                       #1178.42
        movsd     %xmm0, -48(%rsp)                              #1176.21
        movsd     -48(%rsp), %xmm0                              #1177.35
        addsd     -16(%rsp), %xmm0                              #1177.35
        movsd     %xmm0, -24(%rsp)                              #1177.21
        movb      -17(%rsp), %dl                                #1178.42
        andb      $127, %dl                                     #1178.42
        orb       %al, %dl                                      #1178.42
        movb      %dl, -17(%rsp)                                #1178.42
        movq      -24(%rsp), %rax                               #1179.28
        movq      %rax, (%rsi)                                  #1179.23
                                # LOE rbx rbp r12 r13 r14 r15
..B10.11:                       # Preds ..B10.4 ..B10.5 ..B10.7 ..B10.9 ..B10.10
                                #       ..B10.14 ..B10.15
                                # Execution count [1.00e+00]
        xorl      %eax, %eax                                    #1198.12
        ret                                                     #1198.12
                                # LOE
..B10.12:                       # Preds ..B10.1
                                # Execution count [1.60e-01]: Infreq
        testl     $1048575, 4(%rdi)                             #1186.36
        jne       ..B10.15      # Prob 50%                      #1186.68
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15
..B10.13:                       # Preds ..B10.12
                                # Execution count [8.00e-02]: Infreq
        cmpl      $0, (%rdi)                                    #1186.127
        jne       ..B10.15      # Prob 50%                      #1186.127
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15
..B10.14:                       # Preds ..B10.13
                                # Execution count [4.00e-02]: Infreq
        movsd     1912+_vmldAtanHATab(%rip), %xmm0              #1188.38
        movb      7(%rdi), %al                                  #1189.41
        andb      $-128, %al                                    #1190.34
        addsd     1920+_vmldAtanHATab(%rip), %xmm0              #1188.78
        movsd     %xmm0, -24(%rsp)                              #1188.13
        movb      -17(%rsp), %dl                                #1190.34
        andb      $127, %dl                                     #1190.34
        orb       %al, %dl                                      #1190.34
        movb      %dl, -17(%rsp)                                #1190.34
        movq      -24(%rsp), %rcx                               #1191.20
        movq      %rcx, (%rsi)                                  #1191.15
        jmp       ..B10.11      # Prob 100%                     #1191.15
                                # LOE rbx rbp r12 r13 r14 r15
..B10.15:                       # Preds ..B10.13 ..B10.12
                                # Execution count [1.20e-01]: Infreq
        movsd     (%rdi), %xmm0                                 #1195.22
        addsd     %xmm0, %xmm0                                  #1195.29
        movsd     %xmm0, (%rsi)                                 #1195.15
        jmp       ..B10.11      # Prob 100%                     #1195.15
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_datan_ha_cout_rare_internal,@function
	.size	__svml_datan_ha_cout_rare_internal,.-__svml_datan_ha_cout_rare_internal
..LN__svml_datan_ha_cout_rare_internal.9:
	.data
# -- End  __svml_datan_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_datan_ha_data_internal_avx512
	.globl __svml_datan_ha_data_internal_avx512
__svml_datan_ha_data_internal_avx512:
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	0
	.long	1125646336
	.long	0
	.long	1125646336
	.long	0
	.long	1125646336
	.long	0
	.long	1125646336
	.long	0
	.long	1125646336
	.long	0
	.long	1125646336
	.long	0
	.long	1125646336
	.long	0
	.long	1125646336
	.long	0
	.long	1075806208
	.long	0
	.long	1075806208
	.long	0
	.long	1075806208
	.long	0
	.long	1075806208
	.long	0
	.long	1075806208
	.long	0
	.long	1075806208
	.long	0
	.long	1075806208
	.long	0
	.long	1075806208
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
	.long	0
	.long	1206910976
	.long	0
	.long	1206910976
	.long	0
	.long	1206910976
	.long	0
	.long	1206910976
	.long	0
	.long	1206910976
	.long	0
	.long	1206910976
	.long	0
	.long	1206910976
	.long	0
	.long	1206910976
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	4180443357
	.long	1070553973
	.long	90291023
	.long	1071492199
	.long	2737217249
	.long	1071945615
	.long	1413754136
	.long	1072243195
	.long	1468297118
	.long	1072475260
	.long	3531732635
	.long	1072657163
	.long	744202399
	.long	1072747407
	.long	2464923204
	.long	1072805601
	.long	1436891685
	.long	1072853231
	.long	2037009832
	.long	1072892781
	.long	1826698067
	.long	1072926058
	.long	1803191648
	.long	1072954391
	.long	2205372832
	.long	1072978772
	.long	4234512805
	.long	1072999952
	.long	3932628503
	.long	1073018509
	.long	2501811453
	.long	1073034892
	.long	866379431
	.long	1073049455
	.long	1376865888
	.long	1073062480
	.long	3290094269
	.long	1073074195
	.long	354764887
	.long	1073084787
	.long	3332975497
	.long	1073094406
	.long	1141460092
	.long	1073103181
	.long	745761286
	.long	1073111216
	.long	1673304509
	.long	1073118600
	.long	983388243
	.long	1073125409
	.long	3895509104
	.long	1073131706
	.long	2128523669
	.long	1073137548
	.long	2075485693
	.long	1073142981
	.long	121855980
	.long	1073148047
	.long	4181733783
	.long	1073152780
	.long	2887813284
	.long	1073157214
	.long	0
	.long	0
	.long	1022865341
	.long	1013492590
	.long	573531618
	.long	1014639487
	.long	2280825944
	.long	1014120858
	.long	856972295
	.long	1015129638
	.long	986810987
	.long	1015077601
	.long	2062601149
	.long	1013974920
	.long	589036912
	.long	3164328156
	.long	1787331214
	.long	1016798022
	.long	2942272763
	.long	3164235441
	.long	2956702105
	.long	1016472908
	.long	3903328092
	.long	3162582135
	.long	3175026820
	.long	3158589859
	.long	787328196
	.long	1014621351
	.long	2317874517
	.long	3163795518
	.long	4071621134
	.long	1016673529
	.long	2492111345
	.long	3164172103
	.long	3606178875
	.long	3162371821
	.long	3365790232
	.long	1014547152
	.long	2710887773
	.long	1017086651
	.long	2755350986
	.long	3162706257
	.long	198095269
	.long	3162802133
	.long	2791076759
	.long	3164364640
	.long	4214434319
	.long	3162164074
	.long	773754012
	.long	3164190653
	.long	139561443
	.long	3164313657
	.long	2197796619
	.long	3164066219
	.long	3592486882
	.long	1016669082
	.long	1148791015
	.long	3163724934
	.long	386789398
	.long	3163117479
	.long	2518816264
	.long	3162291736
	.long	2545101323
	.long	3164592727
	.long	16
	.long	1125646336
	.long	16
	.long	1125646336
	.long	16
	.long	1125646336
	.long	16
	.long	1125646336
	.long	16
	.long	1125646336
	.long	16
	.long	1125646336
	.long	16
	.long	1125646336
	.long	16
	.long	1125646336
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	4123328151
	.long	1068689849
	.long	4123328151
	.long	1068689849
	.long	4123328151
	.long	1068689849
	.long	4123328151
	.long	1068689849
	.long	4123328151
	.long	1068689849
	.long	4123328151
	.long	1068689849
	.long	4123328151
	.long	1068689849
	.long	4123328151
	.long	1068689849
	.long	3295121612
	.long	3216458327
	.long	3295121612
	.long	3216458327
	.long	3295121612
	.long	3216458327
	.long	3295121612
	.long	3216458327
	.long	3295121612
	.long	3216458327
	.long	3295121612
	.long	3216458327
	.long	3295121612
	.long	3216458327
	.long	3295121612
	.long	3216458327
	.long	4026078880
	.long	1069314495
	.long	4026078880
	.long	1069314495
	.long	4026078880
	.long	1069314495
	.long	4026078880
	.long	1069314495
	.long	4026078880
	.long	1069314495
	.long	4026078880
	.long	1069314495
	.long	4026078880
	.long	1069314495
	.long	4026078880
	.long	1069314495
	.long	2398029018
	.long	3217180964
	.long	2398029018
	.long	3217180964
	.long	2398029018
	.long	3217180964
	.long	2398029018
	.long	3217180964
	.long	2398029018
	.long	3217180964
	.long	2398029018
	.long	3217180964
	.long	2398029018
	.long	3217180964
	.long	2398029018
	.long	3217180964
	.long	2576905246
	.long	1070176665
	.long	2576905246
	.long	1070176665
	.long	2576905246
	.long	1070176665
	.long	2576905246
	.long	1070176665
	.long	2576905246
	.long	1070176665
	.long	2576905246
	.long	1070176665
	.long	2576905246
	.long	1070176665
	.long	2576905246
	.long	1070176665
	.long	1431655757
	.long	3218429269
	.long	1431655757
	.long	3218429269
	.long	1431655757
	.long	3218429269
	.long	1431655757
	.long	3218429269
	.long	1431655757
	.long	3218429269
	.long	1431655757
	.long	3218429269
	.long	1431655757
	.long	3218429269
	.long	1431655757
	.long	3218429269
	.type	__svml_datan_ha_data_internal_avx512,@object
	.size	__svml_datan_ha_data_internal_avx512,1536
	.align 64
	.hidden __svml_datan_ha_data_internal
	.globl __svml_datan_ha_data_internal
__svml_datan_ha_data_internal:
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	1413754136
	.long	1073291771
	.long	856972295
	.long	1016178214
	.long	0
	.long	1073217536
	.long	4294967295
	.long	4294967295
	.long	3531732635
	.long	1072657163
	.long	2062601149
	.long	1013974920
	.long	0
	.long	1072693248
	.long	4294967295
	.long	4294967295
	.long	1413754136
	.long	1072243195
	.long	856972295
	.long	1015129638
	.long	0
	.long	1071644672
	.long	4294967295
	.long	4294967295
	.long	90291023
	.long	1071492199
	.long	573531618
	.long	1014639487
	.long	0
	.long	0
	.long	4294967295
	.long	4294967295
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1071382528
	.long	0
	.long	1071382528
	.long	0
	.long	1071382528
	.long	0
	.long	1071382528
	.long	0
	.long	1071382528
	.long	0
	.long	1071382528
	.long	0
	.long	1071382528
	.long	0
	.long	1071382528
	.long	0
	.long	1072889856
	.long	0
	.long	1072889856
	.long	0
	.long	1072889856
	.long	0
	.long	1072889856
	.long	0
	.long	1072889856
	.long	0
	.long	1072889856
	.long	0
	.long	1072889856
	.long	0
	.long	1072889856
	.long	0
	.long	1073971200
	.long	0
	.long	1073971200
	.long	0
	.long	1073971200
	.long	0
	.long	1073971200
	.long	0
	.long	1073971200
	.long	0
	.long	1073971200
	.long	0
	.long	1073971200
	.long	0
	.long	1073971200
	.long	0
	.long	1072037888
	.long	0
	.long	1072037888
	.long	0
	.long	1072037888
	.long	0
	.long	1072037888
	.long	0
	.long	1072037888
	.long	0
	.long	1072037888
	.long	0
	.long	1072037888
	.long	0
	.long	1072037888
	.long	90291023
	.long	1071492199
	.long	90291023
	.long	1071492199
	.long	90291023
	.long	1071492199
	.long	90291023
	.long	1071492199
	.long	90291023
	.long	1071492199
	.long	90291023
	.long	1071492199
	.long	90291023
	.long	1071492199
	.long	90291023
	.long	1071492199
	.long	573531618
	.long	1014639487
	.long	573531618
	.long	1014639487
	.long	573531618
	.long	1014639487
	.long	573531618
	.long	1014639487
	.long	573531618
	.long	1014639487
	.long	573531618
	.long	1014639487
	.long	573531618
	.long	1014639487
	.long	573531618
	.long	1014639487
	.long	1413754136
	.long	1072243195
	.long	1413754136
	.long	1072243195
	.long	1413754136
	.long	1072243195
	.long	1413754136
	.long	1072243195
	.long	1413754136
	.long	1072243195
	.long	1413754136
	.long	1072243195
	.long	1413754136
	.long	1072243195
	.long	1413754136
	.long	1072243195
	.long	856972295
	.long	1015129638
	.long	856972295
	.long	1015129638
	.long	856972295
	.long	1015129638
	.long	856972295
	.long	1015129638
	.long	856972295
	.long	1015129638
	.long	856972295
	.long	1015129638
	.long	856972295
	.long	1015129638
	.long	856972295
	.long	1015129638
	.long	3531732635
	.long	1072657163
	.long	3531732635
	.long	1072657163
	.long	3531732635
	.long	1072657163
	.long	3531732635
	.long	1072657163
	.long	3531732635
	.long	1072657163
	.long	3531732635
	.long	1072657163
	.long	3531732635
	.long	1072657163
	.long	3531732635
	.long	1072657163
	.long	2062601149
	.long	1013974920
	.long	2062601149
	.long	1013974920
	.long	2062601149
	.long	1013974920
	.long	2062601149
	.long	1013974920
	.long	2062601149
	.long	1013974920
	.long	2062601149
	.long	1013974920
	.long	2062601149
	.long	1013974920
	.long	2062601149
	.long	1013974920
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
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
	.long	1073217536
	.long	0
	.long	1073217536
	.long	0
	.long	1073217536
	.long	0
	.long	1073217536
	.long	0
	.long	1073217536
	.long	0
	.long	1073217536
	.long	0
	.long	1073217536
	.long	0
	.long	1073217536
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
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	4294967295
	.long	2147483647
	.long	1413754136
	.long	1074340347
	.long	1413754136
	.long	1074340347
	.long	1413754136
	.long	1074340347
	.long	1413754136
	.long	1074340347
	.long	1413754136
	.long	1074340347
	.long	1413754136
	.long	1074340347
	.long	1413754136
	.long	1074340347
	.long	1413754136
	.long	1074340347
	.long	0
	.long	1017226816
	.long	0
	.long	1017226816
	.long	0
	.long	1017226816
	.long	0
	.long	1017226816
	.long	0
	.long	1017226816
	.long	0
	.long	1017226816
	.long	0
	.long	1017226816
	.long	0
	.long	1017226816
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	4160749568
	.long	4294967295
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1071382528
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1072889856
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1073971200
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	1072037888
	.long	4
	.long	4
	.long	4
	.long	4
	.long	4
	.long	4
	.long	4
	.long	4
	.long	4
	.long	4
	.long	4
	.long	4
	.long	4
	.long	4
	.long	4
	.long	4
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
	.long	3866310424
	.long	1066132731
	.long	3866310424
	.long	1066132731
	.long	3866310424
	.long	1066132731
	.long	3866310424
	.long	1066132731
	.long	3866310424
	.long	1066132731
	.long	3866310424
	.long	1066132731
	.long	3866310424
	.long	1066132731
	.long	3866310424
	.long	1066132731
	.long	529668190
	.long	3214953687
	.long	529668190
	.long	3214953687
	.long	529668190
	.long	3214953687
	.long	529668190
	.long	3214953687
	.long	529668190
	.long	3214953687
	.long	529668190
	.long	3214953687
	.long	529668190
	.long	3214953687
	.long	529668190
	.long	3214953687
	.long	1493047753
	.long	1067887957
	.long	1493047753
	.long	1067887957
	.long	1493047753
	.long	1067887957
	.long	1493047753
	.long	1067887957
	.long	1493047753
	.long	1067887957
	.long	1493047753
	.long	1067887957
	.long	1493047753
	.long	1067887957
	.long	1493047753
	.long	1067887957
	.long	1554070819
	.long	3215629941
	.long	1554070819
	.long	3215629941
	.long	1554070819
	.long	3215629941
	.long	1554070819
	.long	3215629941
	.long	1554070819
	.long	3215629941
	.long	1554070819
	.long	3215629941
	.long	1554070819
	.long	3215629941
	.long	1554070819
	.long	3215629941
	.long	3992437651
	.long	1068372721
	.long	3992437651
	.long	1068372721
	.long	3992437651
	.long	1068372721
	.long	3992437651
	.long	1068372721
	.long	3992437651
	.long	1068372721
	.long	3992437651
	.long	1068372721
	.long	3992437651
	.long	1068372721
	.long	3992437651
	.long	1068372721
	.long	845965549
	.long	3216052365
	.long	845965549
	.long	3216052365
	.long	845965549
	.long	3216052365
	.long	845965549
	.long	3216052365
	.long	845965549
	.long	3216052365
	.long	845965549
	.long	3216052365
	.long	845965549
	.long	3216052365
	.long	845965549
	.long	3216052365
	.long	3073500986
	.long	1068740914
	.long	3073500986
	.long	1068740914
	.long	3073500986
	.long	1068740914
	.long	3073500986
	.long	1068740914
	.long	3073500986
	.long	1068740914
	.long	3073500986
	.long	1068740914
	.long	3073500986
	.long	1068740914
	.long	3073500986
	.long	1068740914
	.long	426211919
	.long	3216459217
	.long	426211919
	.long	3216459217
	.long	426211919
	.long	3216459217
	.long	426211919
	.long	3216459217
	.long	426211919
	.long	3216459217
	.long	426211919
	.long	3216459217
	.long	426211919
	.long	3216459217
	.long	426211919
	.long	3216459217
	.long	435789718
	.long	1069314503
	.long	435789718
	.long	1069314503
	.long	435789718
	.long	1069314503
	.long	435789718
	.long	1069314503
	.long	435789718
	.long	1069314503
	.long	435789718
	.long	1069314503
	.long	435789718
	.long	1069314503
	.long	435789718
	.long	1069314503
	.long	2453936673
	.long	3217180964
	.long	2453936673
	.long	3217180964
	.long	2453936673
	.long	3217180964
	.long	2453936673
	.long	3217180964
	.long	2453936673
	.long	3217180964
	.long	2453936673
	.long	3217180964
	.long	2453936673
	.long	3217180964
	.long	2453936673
	.long	3217180964
	.long	2576977731
	.long	1070176665
	.long	2576977731
	.long	1070176665
	.long	2576977731
	.long	1070176665
	.long	2576977731
	.long	1070176665
	.long	2576977731
	.long	1070176665
	.long	2576977731
	.long	1070176665
	.long	2576977731
	.long	1070176665
	.long	2576977731
	.long	1070176665
	.long	1431655762
	.long	3218429269
	.long	1431655762
	.long	3218429269
	.long	1431655762
	.long	3218429269
	.long	1431655762
	.long	3218429269
	.long	1431655762
	.long	3218429269
	.long	1431655762
	.long	3218429269
	.long	1431655762
	.long	3218429269
	.long	1431655762
	.long	3218429269
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	2150629376
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.long	4258267136
	.type	__svml_datan_ha_data_internal,@object
	.size	__svml_datan_ha_data_internal,2688
	.align 32
_vmldAtanHATab:
	.long	3892314112
	.long	1069799150
	.long	2332892550
	.long	1039715405
	.long	1342177280
	.long	1070305495
	.long	270726690
	.long	1041535749
	.long	939524096
	.long	1070817911
	.long	2253973841
	.long	3188654726
	.long	3221225472
	.long	1071277294
	.long	3853927037
	.long	1043226911
	.long	2818572288
	.long	1071767563
	.long	2677759107
	.long	1044314101
	.long	3355443200
	.long	1072103591
	.long	1636578514
	.long	3191094734
	.long	1476395008
	.long	1072475260
	.long	1864703685
	.long	3188646936
	.long	805306368
	.long	1072747407
	.long	192551812
	.long	3192726267
	.long	2013265920
	.long	1072892781
	.long	2240369452
	.long	1043768538
	.long	0
	.long	1072999953
	.long	3665168337
	.long	3192705970
	.long	402653184
	.long	1073084787
	.long	1227953434
	.long	3192313277
	.long	2013265920
	.long	1073142981
	.long	3853283127
	.long	1045277487
	.long	805306368
	.long	1073187261
	.long	1676192264
	.long	3192868861
	.long	134217728
	.long	1073217000
	.long	4290763938
	.long	1042034855
	.long	671088640
	.long	1073239386
	.long	994303084
	.long	3189643768
	.long	402653184
	.long	1073254338
	.long	1878067156
	.long	1042652475
	.long	1610612736
	.long	1073265562
	.long	670314820
	.long	1045138554
	.long	3221225472
	.long	1073273048
	.long	691126919
	.long	3189987794
	.long	3489660928
	.long	1073278664
	.long	1618990832
	.long	3188194509
	.long	1207959552
	.long	1073282409
	.long	2198872939
	.long	1044806069
	.long	3489660928
	.long	1073285217
	.long	2633982383
	.long	1042307894
	.long	939524096
	.long	1073287090
	.long	1059367786
	.long	3189114230
	.long	2281701376
	.long	1073288494
	.long	3158525533
	.long	1044484961
	.long	3221225472
	.long	1073289430
	.long	286581777
	.long	1044893263
	.long	4026531840
	.long	1073290132
	.long	2000245215
	.long	3191647611
	.long	134217728
	.long	1073290601
	.long	4205071590
	.long	1045035927
	.long	536870912
	.long	1073290952
	.long	2334392229
	.long	1043447393
	.long	805306368
	.long	1073291186
	.long	2281458177
	.long	3188885569
	.long	3087007744
	.long	1073291361
	.long	691611507
	.long	1044733832
	.long	3221225472
	.long	1073291478
	.long	1816229550
	.long	1044363390
	.long	2281701376
	.long	1073291566
	.long	1993843750
	.long	3189837440
	.long	134217728
	.long	1073291625
	.long	3654754496
	.long	1044970837
	.long	4026531840
	.long	1073291668
	.long	3224300229
	.long	3191935390
	.long	805306368
	.long	1073291698
	.long	2988777976
	.long	3188950659
	.long	536870912
	.long	1073291720
	.long	1030371341
	.long	1043402665
	.long	3221225472
	.long	1073291734
	.long	1524463765
	.long	1044361356
	.long	3087007744
	.long	1073291745
	.long	2754295320
	.long	1044731036
	.long	134217728
	.long	1073291753
	.long	3099629057
	.long	1044970710
	.long	2281701376
	.long	1073291758
	.long	962914160
	.long	3189838838
	.long	805306368
	.long	1073291762
	.long	3543908206
	.long	3188950786
	.long	4026531840
	.long	1073291764
	.long	1849909620
	.long	3191935434
	.long	3221225472
	.long	1073291766
	.long	1641333636
	.long	1044361352
	.long	536870912
	.long	1073291768
	.long	1373968792
	.long	1043402654
	.long	134217728
	.long	1073291769
	.long	2033191599
	.long	1044970710
	.long	3087007744
	.long	1073291769
	.long	4117947437
	.long	1044731035
	.long	805306368
	.long	1073291770
	.long	315378368
	.long	3188950787
	.long	2281701376
	.long	1073291770
	.long	2428571750
	.long	3189838838
	.long	3221225472
	.long	1073291770
	.long	1608007466
	.long	1044361352
	.long	4026531840
	.long	1073291770
	.long	1895711420
	.long	3191935434
	.long	134217728
	.long	1073291771
	.long	2031108713
	.long	1044970710
	.long	536870912
	.long	1073291771
	.long	1362518342
	.long	1043402654
	.long	805306368
	.long	1073291771
	.long	317461253
	.long	3188950787
	.long	939524096
	.long	1073291771
	.long	4117231784
	.long	1044731035
	.long	1073741824
	.long	1073291771
	.long	1607942376
	.long	1044361352
	.long	1207959552
	.long	1073291771
	.long	2428929577
	.long	3189838838
	.long	1207959552
	.long	1073291771
	.long	2031104645
	.long	1044970710
	.long	1342177280
	.long	1073291771
	.long	1895722602
	.long	3191935434
	.long	1342177280
	.long	1073291771
	.long	317465322
	.long	3188950787
	.long	1342177280
	.long	1073291771
	.long	1362515546
	.long	1043402654
	.long	1342177280
	.long	1073291771
	.long	1607942248
	.long	1044361352
	.long	1342177280
	.long	1073291771
	.long	4117231610
	.long	1044731035
	.long	1342177280
	.long	1073291771
	.long	2031104637
	.long	1044970710
	.long	1342177280
	.long	1073291771
	.long	1540251232
	.long	1045150466
	.long	1342177280
	.long	1073291771
	.long	2644671394
	.long	1045270303
	.long	1342177280
	.long	1073291771
	.long	2399244691
	.long	1045360181
	.long	1342177280
	.long	1073291771
	.long	803971124
	.long	1045420100
	.long	1476395008
	.long	1073291771
	.long	3613709523
	.long	3192879152
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192849193
	.long	1476395008
	.long	1073291771
	.long	177735686
	.long	3192826724
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192811744
	.long	1476395008
	.long	1073291771
	.long	2754716064
	.long	3192800509
	.long	1476395008
	.long	1073291771
	.long	3490996172
	.long	3192793019
	.long	1476395008
	.long	1073291771
	.long	1895722605
	.long	3192787402
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192783657
	.long	1476395008
	.long	1073291771
	.long	3613709523
	.long	3192780848
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192778976
	.long	1476395008
	.long	1073291771
	.long	177735686
	.long	3192777572
	.long	1476395008
	.long	1073291771
	.long	3490996172
	.long	3192776635
	.long	1476395008
	.long	1073291771
	.long	2754716064
	.long	3192775933
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192775465
	.long	1476395008
	.long	1073291771
	.long	1895722605
	.long	3192775114
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192774880
	.long	1476395008
	.long	1073291771
	.long	3613709523
	.long	3192774704
	.long	1476395008
	.long	1073291771
	.long	3490996172
	.long	3192774587
	.long	1476395008
	.long	1073291771
	.long	177735686
	.long	3192774500
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192774441
	.long	1476395008
	.long	1073291771
	.long	2754716064
	.long	3192774397
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192774368
	.long	1476395008
	.long	1073291771
	.long	1895722605
	.long	3192774346
	.long	1476395008
	.long	1073291771
	.long	3490996172
	.long	3192774331
	.long	1476395008
	.long	1073291771
	.long	3613709523
	.long	3192774320
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192774313
	.long	1476395008
	.long	1073291771
	.long	177735686
	.long	3192774308
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192774304
	.long	1476395008
	.long	1073291771
	.long	2754716064
	.long	3192774301
	.long	1476395008
	.long	1073291771
	.long	3490996172
	.long	3192774299
	.long	1476395008
	.long	1073291771
	.long	1895722605
	.long	3192774298
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192774297
	.long	1476395008
	.long	1073291771
	.long	3613709523
	.long	3192774296
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192774296
	.long	1476395008
	.long	1073291771
	.long	177735686
	.long	3192774296
	.long	1476395008
	.long	1073291771
	.long	3490996172
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	2754716064
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1895722605
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1466225875
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1343512524
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1251477510
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1190120835
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1144103328
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1113424990
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1090416237
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1075077068
	.long	3192774295
	.long	1431655765
	.long	3218429269
	.long	2576978363
	.long	1070176665
	.long	2453154343
	.long	3217180964
	.long	4189149139
	.long	1069314502
	.long	1775019125
	.long	3216459198
	.long	273199057
	.long	1068739452
	.long	874748308
	.long	3215993277
	.long	0
	.long	1017118720
	.long	0
	.long	1069547520
	.long	0
	.long	1129316352
	.long	0
	.long	1072693248
	.long	1413754136
	.long	1073291771
	.long	856972295
	.long	1016178214
	.long	33554432
	.long	1101004800
	.type	_vmldAtanHATab,@object
	.size	_vmldAtanHATab,1936
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
