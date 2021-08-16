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
	.file "svml_d_atan2.c"
	.text
..TXTST0:
.L_2__routine_start___svml_atan22_ha_ex_0:
# -- Begin  __svml_atan22_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan22_ha_ex
# --- __svml_atan22_ha_ex(__m128d, __m128d)
__svml_atan22_ha_ex:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #228.1
        .byte     15                                            #551.14
        .byte     30                                            #551.14
        .byte     250                                           #551.14
	.cfi_startproc
..___tag_value___svml_atan22_ha_ex.1:
..L2:
                                                          #228.1
        pushq     %rbp                                          #228.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #228.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #228.1
        subq      $256, %rsp                                    #228.1
        movaps    %xmm1, %xmm12                                 #357.24
        movups    896+__svml_datan2_ha_data_internal(%rip), %xmm9 #344.51
        lea       __svml_datan2_ha_data_internal(%rip), %rsi    #409.408
        movaps    %xmm9, %xmm15                                 #345.14
        andps     %xmm0, %xmm9                                  #346.14
        andps     %xmm1, %xmm15                                 #345.14
        movaps    %xmm9, %xmm14                                 #348.17
        movups    1088+__svml_datan2_ha_data_internal(%rip), %xmm6 #349.59
        movaps    %xmm15, %xmm13                                #347.17
        movups    %xmm1, (%rsp)                                 #228.1[spill]
        pxor      %xmm1, %xmm13                                 #347.17
        pshufd    $221, %xmm15, %xmm7                           #362.14
        movaps    %xmm6, %xmm5                                  #350.19
        pshufd    $221, %xmm9, %xmm1                            #363.14
        andps     %xmm9, %xmm5                                  #350.19
        movups    %xmm9, 48(%rsp)                               #346.14[spill]
        movaps    %xmm9, %xmm4                                  #351.19
        movq      2688+__svml_datan2_ha_data_internal(%rip), %xmm8 #366.15
        movdqa    %xmm7, %xmm10                                 #366.15
        movdqa    %xmm1, %xmm9                                  #371.15
        psubd     %xmm8, %xmm10                                 #366.15
        movq      3200+__svml_datan2_ha_data_internal(%rip), %xmm2 #374.15
        movdqa    %xmm1, %xmm11                                 #367.15
        psubd     %xmm7, %xmm9                                  #371.15
        psubd     %xmm8, %xmm11                                 #367.15
        movq      2752+__svml_datan2_ha_data_internal(%rip), %xmm8 #368.30
        paddd     %xmm2, %xmm9                                  #374.15
        movdqa    %xmm10, %xmm2                                 #368.30
        pcmpeqd   %xmm8, %xmm10                                 #368.76
        pcmpgtd   %xmm8, %xmm2                                  #368.30
        andps     %xmm15, %xmm6                                 #352.19
        por       %xmm10, %xmm2                                 #368.16
        movdqa    %xmm11, %xmm10                                #369.30
        pcmpgtd   %xmm8, %xmm10                                 #369.30
        pcmpeqd   %xmm8, %xmm11                                 #369.76
        por       %xmm11, %xmm10                                #369.16
        movaps    %xmm15, %xmm3                                 #353.19
        movq      3264+__svml_datan2_ha_data_internal(%rip), %xmm11 #375.30
        por       %xmm10, %xmm2                                 #370.17
        movdqa    %xmm11, %xmm10                                #375.30
        pcmpeqd   %xmm9, %xmm11                                 #375.68
        pcmpgtd   %xmm9, %xmm10                                 #375.30
        pxor      %xmm0, %xmm14                                 #348.17
        por       %xmm11, %xmm10                                #375.16
        xorl      %edx, %edx                                    #377.5
        por       %xmm10, %xmm2                                 #376.17
        movdqu    %xmm2, 64(%rsp)                               #376.17[spill]
        movups    1152+__svml_datan2_ha_data_internal(%rip), %xmm2 #384.15
        movups    1216+__svml_datan2_ha_data_internal(%rip), %xmm8 #386.15
        movups    1280+__svml_datan2_ha_data_internal(%rip), %xmm7 #387.15
        movups    1344+__svml_datan2_ha_data_internal(%rip), %xmm10 #385.15
        mulpd     %xmm15, %xmm2                                 #384.15
        mulpd     %xmm15, %xmm10                                #385.15
        mulpd     %xmm15, %xmm8                                 #386.15
        mulpd     %xmm15, %xmm7                                 #387.15
        subpd     %xmm6, %xmm3                                  #353.19
        subpd     %xmm5, %xmm4                                  #351.19
        cmpltpd   2880+__svml_datan2_ha_data_internal(%rip), %xmm12 #357.24
        movups    %xmm15, 32(%rsp)                              #345.14[spill]
        pshufd    $221, %xmm2, %xmm11                           #392.19
        pshufd    $221, %xmm10, %xmm15                          #393.19
        psubd     %xmm1, %xmm11                                 #396.27
        pshufd    $221, %xmm8, %xmm8                            #394.19
        psubd     %xmm1, %xmm15                                 #397.27
        pshufd    $221, %xmm7, %xmm10                           #395.19
        psubd     %xmm1, %xmm8                                  #398.27
        psubd     %xmm1, %xmm10                                 #399.27
        psrad     $31, %xmm11                                   #400.26
        psrad     $31, %xmm15                                   #401.26
        psrad     $31, %xmm8                                    #402.26
        psrad     $31, %xmm10                                   #403.26
        paddd     %xmm15, %xmm11                                #409.181
        paddd     %xmm10, %xmm8                                 #409.181
        movaps    %xmm6, %xmm15                                 #415.42
        movq      1408+__svml_datan2_ha_data_internal(%rip), %xmm9 #407.17
        paddd     %xmm8, %xmm11                                 #409.181
        paddd     %xmm9, %xmm11                                 #409.181
        movaps    %xmm5, %xmm10                                 #413.20
        pslld     $5, %xmm11                                    #409.181
        movaps    %xmm4, %xmm8                                  #414.20
        movd      %xmm11, %eax                                  #409.238
        pshufd    $1, %xmm11, %xmm9                             #409.319
        movups    %xmm0, 16(%rsp)                               #228.1[spill]
        movd      %xmm9, %ecx                                   #409.300
        movq      (%rax,%rsi), %xmm2                            #409.646
        movq      24(%rax,%rsi), %xmm7                          #412.643
        movhpd    (%rcx,%rsi), %xmm2                            #409.614
        mulpd     %xmm2, %xmm15                                 #415.42
        mulpd     %xmm2, %xmm5                                  #425.32
        movhpd    24(%rcx,%rsi), %xmm7                          #412.611
        andps     %xmm7, %xmm10                                 #413.20
        andps     %xmm7, %xmm8                                  #414.20
        subpd     %xmm15, %xmm10                                #415.20
        movaps    %xmm3, %xmm15                                 #416.42
        andps     %xmm7, %xmm6                                  #423.20
        mulpd     %xmm2, %xmm15                                 #416.42
        mulpd     %xmm4, %xmm2                                  #426.32
        subpd     %xmm15, %xmm8                                 #416.20
        addpd     %xmm5, %xmm6                                  #425.20
        andps     %xmm3, %xmm7                                  #424.20
        movaps    %xmm10, %xmm1                                 #417.20
        addpd     %xmm2, %xmm7                                  #426.20
        subpd     %xmm8, %xmm1                                  #417.20
        movaps    %xmm6, %xmm3                                  #427.20
        movaps    %xmm1, %xmm15                                 #418.21
        addpd     %xmm7, %xmm3                                  #427.20
        subpd     %xmm10, %xmm15                                #418.21
        subpd     %xmm3, %xmm6                                  #428.23
        subpd     %xmm15, %xmm8                                 #419.20
        addpd     %xmm6, %xmm7                                  #429.20
        movups    1088+__svml_datan2_ha_data_internal(%rip), %xmm10 #420.19
        andps     %xmm1, %xmm10                                 #420.19
        movups    1088+__svml_datan2_ha_data_internal(%rip), %xmm5 #430.19
        subpd     %xmm10, %xmm1                                 #421.22
        andps     %xmm3, %xmm5                                  #430.19
        subpd     %xmm5, %xmm3                                  #431.22
        addpd     %xmm1, %xmm8                                  #422.19
        addpd     %xmm3, %xmm7                                  #432.19
        pshufd    $221, %xmm5, %xmm4                            #433.1408
        movdqa    %xmm4, %xmm1                                  #433.1572
        movq      1600+__svml_datan2_ha_data_internal(%rip), %xmm2 #433.994
        pslld     $3, %xmm1                                     #433.1572
        movq      1664+__svml_datan2_ha_data_internal(%rip), %xmm3 #433.1093
        pand      %xmm2, %xmm1                                  #433.1610
        por       %xmm3, %xmm1                                  #433.1665
        rcpps     %xmm1, %xmm2                                  #433.1747
        movq      1472+__svml_datan2_ha_data_internal(%rip), %xmm6 #433.795
        psrld     $3, %xmm2                                     #433.1817
        movq      1728+__svml_datan2_ha_data_internal(%rip), %xmm15 #433.1183
        pand      %xmm6, %xmm4                                  #433.1474
        movq      1536+__svml_datan2_ha_data_internal(%rip), %xmm6 #433.1524
        psubd     %xmm15, %xmm2                                 #433.1856
        psubd     %xmm4, %xmm6                                  #433.1524
        paddd     %xmm6, %xmm2                                  #433.1952
        pshufd    $80, %xmm2, %xmm4                             #433.1952
        movaps    %xmm10, %xmm2                                 #442.18
        andps     3008+__svml_datan2_ha_data_internal(%rip), %xmm4 #433.2051
        mulpd     %xmm4, %xmm5                                  #434.31
        mulpd     %xmm4, %xmm7                                  #435.29
        subpd     1792+__svml_datan2_ha_data_internal(%rip), %xmm5 #434.19
        mulpd     %xmm4, %xmm10                                 #445.29
        addpd     %xmm7, %xmm5                                  #435.17
        movaps    %xmm5, %xmm7                                  #436.31
        mulpd     %xmm5, %xmm7                                  #436.31
        subpd     %xmm5, %xmm7                                  #436.19
        mulpd     %xmm5, %xmm7                                  #437.31
        addpd     %xmm5, %xmm7                                  #437.19
        mulpd     %xmm5, %xmm7                                  #438.31
        subpd     %xmm5, %xmm7                                  #438.19
        mulpd     %xmm5, %xmm7                                  #439.31
        addpd     %xmm5, %xmm7                                  #439.19
        mulpd     %xmm5, %xmm7                                  #440.31
        subpd     %xmm5, %xmm7                                  #440.19
        mulpd     %xmm4, %xmm7                                  #441.21
        mulpd     %xmm7, %xmm2                                  #442.18
        mulpd     %xmm8, %xmm7                                  #443.30
        mulpd     %xmm4, %xmm8                                  #444.31
        addpd     %xmm7, %xmm2                                  #443.18
        addpd     %xmm8, %xmm2                                  #444.19
        movaps    %xmm2, %xmm4                                  #445.17
        addpd     %xmm10, %xmm4                                 #445.17
        movaps    %xmm4, %xmm3                                  #458.14
        mulpd     %xmm4, %xmm3                                  #458.14
        movaps    %xmm3, %xmm15                                 #459.14
        mulpd     %xmm3, %xmm15                                 #459.14
        movups    1856+__svml_datan2_ha_data_internal(%rip), %xmm5 #460.26
        mulpd     %xmm15, %xmm5                                 #460.26
        movups    1920+__svml_datan2_ha_data_internal(%rip), %xmm8 #461.26
        addpd     1984+__svml_datan2_ha_data_internal(%rip), %xmm5 #460.14
        mulpd     %xmm15, %xmm8                                 #461.26
        mulpd     %xmm15, %xmm5                                 #462.26
        addpd     2048+__svml_datan2_ha_data_internal(%rip), %xmm8 #461.14
        addpd     2112+__svml_datan2_ha_data_internal(%rip), %xmm5 #462.14
        mulpd     %xmm15, %xmm8                                 #463.26
        mulpd     %xmm15, %xmm5                                 #464.26
        addpd     2176+__svml_datan2_ha_data_internal(%rip), %xmm8 #463.14
        addpd     2240+__svml_datan2_ha_data_internal(%rip), %xmm5 #464.14
        mulpd     %xmm15, %xmm8                                 #465.26
        mulpd     %xmm15, %xmm5                                 #466.26
        addpd     2304+__svml_datan2_ha_data_internal(%rip), %xmm8 #465.14
        addpd     2368+__svml_datan2_ha_data_internal(%rip), %xmm5 #466.14
        mulpd     %xmm15, %xmm8                                 #467.26
        mulpd     %xmm15, %xmm5                                 #468.26
        addpd     2432+__svml_datan2_ha_data_internal(%rip), %xmm8 #467.14
        addpd     2496+__svml_datan2_ha_data_internal(%rip), %xmm5 #468.14
        mulpd     %xmm8, %xmm15                                 #469.26
        mulpd     %xmm3, %xmm5                                  #470.26
        addpd     2560+__svml_datan2_ha_data_internal(%rip), %xmm15 #469.14
        addpd     %xmm5, %xmm15                                 #470.14
        mulpd     %xmm15, %xmm3                                 #471.14
        movq      16(%rax,%rsi), %xmm9                          #411.652
        movhpd    16(%rcx,%rsi), %xmm9                          #411.620
        addpd     %xmm2, %xmm9                                  #473.19
        mulpd     %xmm3, %xmm4                                  #475.26
        movups    960+__svml_datan2_ha_data_internal(%rip), %xmm0 #358.20
        andps     %xmm12, %xmm0                                 #358.20
        andps     1024+__svml_datan2_ha_data_internal(%rip), %xmm12 #359.20
        pxor      %xmm13, %xmm12                                #472.16
        addpd     %xmm9, %xmm12                                 #474.19
        addpd     %xmm4, %xmm12                                 #475.14
        addpd     %xmm12, %xmm10                                #476.18
        movq      8(%rax,%rsi), %xmm11                          #410.652
        movhpd    8(%rcx,%rsi), %xmm11                          #410.620
        addpd     %xmm10, %xmm11                                #477.18
        pxor      %xmm13, %xmm11                                #478.18
        addpd     %xmm11, %xmm0                                 #479.18
        movdqu    64(%rsp), %xmm7                               #378.58[spill]
        orps      %xmm14, %xmm0                                 #480.14
        movmskps  %xmm7, %edi                                   #378.58
        movups    48(%rsp), %xmm2                               #481.52[spill]
        movups    16(%rsp), %xmm8                               #481.52[spill]
        movups    (%rsp), %xmm9                                 #481.52[spill]
        movups    32(%rsp), %xmm12                              #481.52[spill]
        testl     $3, %edi                                      #481.41
        jne       ..B1.12       # Prob 5%                       #481.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm2 xmm7 xmm8 xmm9 xmm12
..B1.2:                         # Preds ..B1.12 ..B1.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #554.52
        jne       ..B1.4        # Prob 5%                       #554.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm8 xmm9
..B1.3:                         # Preds ..B1.4 ..B1.10 ..B1.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #557.12
        popq      %rbp                                          #557.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #557.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.4:                         # Preds ..B1.2
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm8, 64(%rsp)                               #554.241
        movups    %xmm9, 128(%rsp)                              #554.314
        movups    %xmm0, 192(%rsp)                              #554.387
        je        ..B1.3        # Prob 95%                      #554.491
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B1.7:                         # Preds ..B1.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #554.571
        movq      %r12, 8(%rsp)                                 #554.571[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #554.571
        movq      %r13, (%rsp)                                  #554.571[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #554.571
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.9 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #554.634
        jc        ..B1.11       # Prob 5%                       #554.634
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.11 ..B1.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #554.587
        cmpl      $2, %r12d                                     #554.582
        jl        ..B1.8        # Prob 82%                      #554.582
                                # LOE rbx r12 r14 r15 r13d
..B1.10:                        # Preds ..B1.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm0                              #554.805
        jmp       ..B1.3        # Prob 100%                     #554.805
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B1.11:                        # Preds ..B1.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #554.663
        lea       128(%rsp,%r12,8), %rsi                        #554.663
        lea       192(%rsp,%r12,8), %rdx                        #554.663
..___tag_value___svml_atan22_ha_ex.29:
#       __svml_datan2_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_datan2_ha_cout_rare_internal           #554.663
..___tag_value___svml_atan22_ha_ex.30:
        jmp       ..B1.9        # Prob 100%                     #554.663
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B1.12:                        # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movaps    %xmm2, %xmm10                                 #519.21
        movaps    %xmm9, %xmm13                                 #522.19
        cmpnltpd  %xmm12, %xmm10                                #519.21
        cmpordpd  %xmm9, %xmm13                                 #522.19
        movups    3136+__svml_datan2_ha_data_internal(%rip), %xmm4 #520.24
        movaps    %xmm10, %xmm3                                 #521.32
        movups    2880+__svml_datan2_ha_data_internal(%rip), %xmm1 #527.18
        movaps    %xmm8, %xmm11                                 #523.19
        cmpordpd  %xmm8, %xmm11                                 #523.19
        andps     %xmm10, %xmm4                                 #520.24
        andnps    %xmm12, %xmm3                                 #521.32
        cmpeqpd   %xmm1, %xmm12                                 #527.18
        andps     %xmm2, %xmm10                                 #521.64
        andps     %xmm11, %xmm13                                #524.20
        cmpeqpd   %xmm1, %xmm2                                  #528.18
        orps      %xmm10, %xmm3                                 #521.21
        orps      %xmm2, %xmm12                                 #529.20
        cmpeqpd   %xmm1, %xmm3                                  #535.17
        pshufd    $221, %xmm12, %xmm2                           #531.20
        movaps    %xmm3, %xmm10                                 #537.27
        pshufd    $221, %xmm13, %xmm12                          #526.20
        andnps    %xmm4, %xmm10                                 #537.27
        pand      %xmm12, %xmm2                                 #532.26
        andps     %xmm1, %xmm3                                  #537.65
        movdqa    %xmm2, %xmm14                                 #533.19
        orps      %xmm3, %xmm10                                 #537.16
        movups    832+__svml_datan2_ha_data_internal(%rip), %xmm5 #516.56
        pandn     %xmm7, %xmm14                                 #533.19
        pshufd    $221, %xmm9, %xmm7                            #542.16
        movaps    %xmm5, %xmm6                                  #517.18
        pshufd    $221, %xmm1, %xmm15                           #540.16
        andps     %xmm9, %xmm6                                  #517.18
        pcmpgtd   %xmm7, %xmm15                                 #543.20
        orps      %xmm6, %xmm10                                 #538.13
        pshufd    $80, %xmm15, %xmm1                            #544.26
        andps     %xmm8, %xmm5                                  #518.18
        andps     3072+__svml_datan2_ha_data_internal(%rip), %xmm1 #546.18
        addpd     %xmm1, %xmm10                                 #547.17
        movmskps  %xmm14, %edx                                  #534.44
        pshufd    $80, %xmm2, %xmm2                             #549.32
        orps      %xmm5, %xmm10                                 #548.19
        movdqa    %xmm2, %xmm3                                  #551.25
        andps     %xmm2, %xmm10                                 #551.68
        andnps    %xmm0, %xmm3                                  #551.25
        andl      $3, %edx                                      #534.96
        movaps    %xmm3, %xmm0                                  #551.14
        orps      %xmm10, %xmm0                                 #551.14
        jmp       ..B1.2        # Prob 100%                     #551.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm8 xmm9
	.cfi_endproc
# mark_end;
	.type	__svml_atan22_ha_ex,@function
	.size	__svml_atan22_ha_ex,.-__svml_atan22_ha_ex
..LN__svml_atan22_ha_ex.0:
	.data
# -- End  __svml_atan22_ha_ex
	.text
.L_2__routine_start___svml_atan21_ha_ex_1:
# -- Begin  __svml_atan21_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan21_ha_ex
# --- __svml_atan21_ha_ex(__m128d, __m128d)
__svml_atan21_ha_ex:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #563.1
        .byte     15                                            #886.14
        .byte     30                                            #886.14
        .byte     250                                           #886.14
	.cfi_startproc
..___tag_value___svml_atan21_ha_ex.34:
..L35:
                                                         #563.1
        pushq     %rbp                                          #563.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #563.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #563.1
        subq      $192, %rsp                                    #563.1
        movaps    %xmm1, %xmm8                                  #563.1
        movsd     896+__svml_datan2_ha_data_internal(%rip), %xmm6 #679.20
        movl      $-2144337920, %eax                            #699.24
        movaps    %xmm6, %xmm12                                 #680.14
        andps     %xmm0, %xmm6                                  #681.14
        andps     %xmm8, %xmm12                                 #680.14
        movl      $1005584384, %ecx                             #709.15
        movaps    %xmm12, %xmm9                                 #682.17
        movaps    %xmm6, %xmm11                                 #683.17
        movups    %xmm8, (%rsp)                                 #563.1[spill]
        pxor      %xmm8, %xmm9                                  #682.17
        movsd     1088+__svml_datan2_ha_data_internal(%rip), %xmm4 #684.28
        movl      $-36700160, %edx                              #700.24
        movsd     1024+__svml_datan2_ha_data_internal(%rip), %xmm13 #691.16
        pxor      %xmm0, %xmm11                                 #683.17
        movsd     960+__svml_datan2_ha_data_internal(%rip), %xmm10 #693.20
        movaps    %xmm4, %xmm3                                  #685.19
        movups    %xmm0, 16(%rsp)                               #563.1[spill]
        andps     %xmm6, %xmm3                                  #685.19
        pshufd    $85, %xmm6, %xmm0                             #698.14
        movaps    %xmm6, %xmm2                                  #686.19
        movups    %xmm6, 32(%rsp)                               #681.14[spill]
        movd      %eax, %xmm14                                  #699.24
        movdqa    %xmm0, %xmm6                                  #706.15
        movd      %edx, %xmm5                                   #700.24
        movdqa    %xmm0, %xmm7                                  #702.15
        movl      $4, %esi                                      #744.181
        psubd     %xmm14, %xmm7                                 #702.15
        lea       __svml_datan2_ha_data_internal(%rip), %rdx    #744.415
        andps     %xmm12, %xmm4                                 #687.19
        movaps    %xmm12, %xmm1                                 #688.19
        movl      $8388607, %r8d                                #768.1605
        movl      $1065353216, %r9d                             #768.1660
        movl      $-1048576, %r11d                              #768.1469
        movl      $133169152, %r10d                             #768.1851
        xorl      %eax, %eax                                    #712.5
        cmpltsd   2880+__svml_datan2_ha_data_internal(%rip), %xmm8 #692.24
        subsd     %xmm4, %xmm1                                  #688.19
        subsd     %xmm3, %xmm2                                  #686.19
        andps     %xmm8, %xmm10                                 #693.20
        andps     %xmm13, %xmm8                                 #694.20
        pshufd    $85, %xmm12, %xmm13                           #697.14
        pxor      %xmm9, %xmm8                                  #807.16
        movdqa    %xmm13, %xmm15                                #701.15
        psubd     %xmm13, %xmm6                                 #706.15
        psubd     %xmm14, %xmm15                                #701.15
        movd      %ecx, %xmm13                                  #709.15
        paddd     %xmm13, %xmm6                                 #709.15
        movdqa    %xmm15, %xmm13                                #703.30
        pcmpgtd   %xmm5, %xmm13                                 #703.30
        pcmpeqd   %xmm5, %xmm15                                 #703.76
        por       %xmm15, %xmm13                                #703.16
        movdqa    %xmm7, %xmm15                                 #704.30
        pcmpgtd   %xmm5, %xmm15                                 #704.30
        pcmpeqd   %xmm5, %xmm7                                  #704.76
        por       %xmm7, %xmm15                                 #704.16
        movaps    %xmm12, %xmm14                                #722.15
        pxor      %xmm7, %xmm7                                  #710.30
        pxor      %xmm5, %xmm5                                  #710.68
        pcmpgtd   %xmm6, %xmm7                                  #710.30
        pcmpeqd   %xmm6, %xmm5                                  #710.68
        por       %xmm15, %xmm13                                #705.17
        por       %xmm5, %xmm7                                  #710.16
        por       %xmm7, %xmm13                                 #711.17
        movaps    %xmm12, %xmm6                                 #719.15
        movaps    %xmm12, %xmm15                                #720.15
        movaps    %xmm12, %xmm7                                 #721.15
        mulsd     1152+__svml_datan2_ha_data_internal(%rip), %xmm6 #719.15
        mulsd     1216+__svml_datan2_ha_data_internal(%rip), %xmm7 #721.15
        mulsd     1280+__svml_datan2_ha_data_internal(%rip), %xmm14 #722.15
        mulsd     1344+__svml_datan2_ha_data_internal(%rip), %xmm15 #720.15
        movmskps  %xmm13, %ecx                                  #713.58
        pshufd    $85, %xmm6, %xmm5                             #727.19
        pshufd    $85, %xmm15, %xmm6                            #728.19
        psubd     %xmm0, %xmm5                                  #731.27
        pshufd    $85, %xmm7, %xmm7                             #729.19
        psubd     %xmm0, %xmm6                                  #732.27
        pshufd    $85, %xmm14, %xmm14                           #730.19
        psubd     %xmm0, %xmm7                                  #733.27
        psubd     %xmm0, %xmm14                                 #734.27
        psrad     $31, %xmm5                                    #735.26
        psrad     $31, %xmm6                                    #736.26
        psrad     $31, %xmm7                                    #737.26
        psrad     $31, %xmm14                                   #738.26
        paddd     %xmm6, %xmm5                                  #744.181
        paddd     %xmm14, %xmm7                                 #744.181
        movd      %esi, %xmm0                                   #744.181
        paddd     %xmm7, %xmm5                                  #744.181
        paddd     %xmm0, %xmm5                                  #744.181
        pslld     $5, %xmm5                                     #744.181
        movd      %xmm5, %edi                                   #744.238
        movq      (%rdi,%rdx), %xmm7                            #744.415
        movq      24(%rdx,%rdi), %xmm14                         #747.412
        movdqa    %xmm7, %xmm5                                  #750.42
        movdqa    %xmm7, %xmm6                                  #751.42
        movdqa    %xmm14, %xmm0                                 #748.20
        mulsd     %xmm4, %xmm5                                  #750.42
        movdqa    %xmm14, %xmm15                                #749.20
        mulsd     %xmm1, %xmm6                                  #751.42
        andps     %xmm14, %xmm4                                 #758.20
        andps     %xmm1, %xmm14                                 #759.20
        movdqa    %xmm7, %xmm1                                  #760.32
        andps     %xmm3, %xmm0                                  #748.20
        mulsd     %xmm3, %xmm1                                  #760.32
        subsd     %xmm5, %xmm0                                  #750.20
        mulsd     %xmm2, %xmm7                                  #761.32
        addsd     %xmm4, %xmm1                                  #760.20
        addsd     %xmm14, %xmm7                                 #761.20
        andps     %xmm2, %xmm15                                 #749.20
        movaps    %xmm1, %xmm3                                  #762.20
        movsd     1088+__svml_datan2_ha_data_internal(%rip), %xmm2 #765.19
        subsd     %xmm6, %xmm15                                 #751.20
        addsd     %xmm7, %xmm3                                  #762.20
        movaps    %xmm0, %xmm6                                  #752.20
        andps     %xmm3, %xmm2                                  #765.19
        pshufd    $85, %xmm2, %xmm14                            #768.1403
        subsd     %xmm15, %xmm6                                 #752.20
        subsd     %xmm3, %xmm1                                  #763.23
        subsd     %xmm2, %xmm3                                  #766.22
        addsd     %xmm1, %xmm7                                  #764.20
        movaps    %xmm6, %xmm5                                  #753.21
        movdqa    %xmm14, %xmm4                                 #768.1567
        pslld     $3, %xmm4                                     #768.1567
        movd      %r9d, %xmm1                                   #768.1660
        subsd     %xmm0, %xmm5                                  #753.21
        movd      %r8d, %xmm0                                   #768.1605
        addsd     %xmm3, %xmm7                                  #767.19
        subsd     %xmm5, %xmm15                                 #754.20
        pand      %xmm0, %xmm4                                  #768.1605
        movl      $2145386496, %r8d                             #768.1519
        por       %xmm1, %xmm4                                  #768.1660
        movd      %r11d, %xmm0                                  #768.1469
        rcpps     %xmm4, %xmm4                                  #768.1742
        movsd     1088+__svml_datan2_ha_data_internal(%rip), %xmm5 #755.19
        psrld     $3, %xmm4                                     #768.1812
        andps     %xmm6, %xmm5                                  #755.19
        movd      %r10d, %xmm3                                  #768.1851
        pand      %xmm0, %xmm14                                 #768.1469
        movd      %r8d, %xmm1                                   #768.1519
        psubd     %xmm3, %xmm4                                  #768.1851
        psubd     %xmm14, %xmm1                                 #768.1519
        paddd     %xmm1, %xmm4                                  #768.1947
        subsd     %xmm5, %xmm6                                  #756.22
        pshufd    $0, %xmm4, %xmm0                              #768.1947
        addsd     %xmm15, %xmm6                                 #757.19
        movsd     3008+__svml_datan2_ha_data_internal(%rip), %xmm15 #768.1276
        andps     %xmm15, %xmm0                                 #768.2046
        mulsd     %xmm0, %xmm2                                  #769.31
        movaps    %xmm0, %xmm1                                  #779.31
        mulsd     %xmm0, %xmm7                                  #770.29
        mulsd     %xmm6, %xmm1                                  #779.31
        subsd     1792+__svml_datan2_ha_data_internal(%rip), %xmm2 #769.19
        addsd     %xmm2, %xmm7                                  #770.17
        movaps    %xmm7, %xmm2                                  #771.31
        mulsd     %xmm7, %xmm2                                  #771.31
        subsd     %xmm7, %xmm2                                  #771.19
        mulsd     %xmm7, %xmm2                                  #772.31
        addsd     %xmm7, %xmm2                                  #772.19
        mulsd     %xmm7, %xmm2                                  #773.31
        subsd     %xmm7, %xmm2                                  #773.19
        mulsd     %xmm7, %xmm2                                  #774.31
        addsd     %xmm7, %xmm2                                  #774.19
        mulsd     %xmm7, %xmm2                                  #775.31
        subsd     %xmm7, %xmm2                                  #775.19
        mulsd     %xmm0, %xmm2                                  #776.21
        mulsd     %xmm5, %xmm0                                  #780.29
        movaps    %xmm2, %xmm7                                  #777.18
        movaps    %xmm0, %xmm15                                 #780.17
        mulsd     %xmm5, %xmm7                                  #777.18
        mulsd     %xmm6, %xmm2                                  #778.30
        movsd     1920+__svml_datan2_ha_data_internal(%rip), %xmm6 #796.26
        addsd     %xmm7, %xmm2                                  #778.18
        movups    16(%rsp), %xmm7                               #816.52[spill]
        addsd     %xmm2, %xmm1                                  #779.19
        movsd     1856+__svml_datan2_ha_data_internal(%rip), %xmm2 #795.26
        addsd     %xmm1, %xmm15                                 #780.17
        addsd     16(%rdx,%rdi), %xmm1                          #808.19
        movaps    %xmm15, %xmm14                                #793.14
        addsd     %xmm8, %xmm1                                  #809.19
        mulsd     %xmm15, %xmm14                                #793.14
        movaps    %xmm14, %xmm5                                 #794.14
        mulsd     %xmm14, %xmm5                                 #794.14
        mulsd     %xmm5, %xmm2                                  #795.26
        mulsd     %xmm5, %xmm6                                  #796.26
        addsd     1984+__svml_datan2_ha_data_internal(%rip), %xmm2 #795.14
        addsd     2048+__svml_datan2_ha_data_internal(%rip), %xmm6 #796.14
        mulsd     %xmm5, %xmm2                                  #797.26
        mulsd     %xmm5, %xmm6                                  #798.26
        addsd     2112+__svml_datan2_ha_data_internal(%rip), %xmm2 #797.14
        addsd     2176+__svml_datan2_ha_data_internal(%rip), %xmm6 #798.14
        mulsd     %xmm5, %xmm2                                  #799.26
        mulsd     %xmm5, %xmm6                                  #800.26
        addsd     2240+__svml_datan2_ha_data_internal(%rip), %xmm2 #799.14
        addsd     2304+__svml_datan2_ha_data_internal(%rip), %xmm6 #800.14
        mulsd     %xmm5, %xmm2                                  #801.26
        mulsd     %xmm5, %xmm6                                  #802.26
        addsd     2368+__svml_datan2_ha_data_internal(%rip), %xmm2 #801.14
        addsd     2432+__svml_datan2_ha_data_internal(%rip), %xmm6 #802.14
        mulsd     %xmm5, %xmm2                                  #803.26
        mulsd     %xmm5, %xmm6                                  #804.26
        addsd     2496+__svml_datan2_ha_data_internal(%rip), %xmm2 #803.14
        addsd     2560+__svml_datan2_ha_data_internal(%rip), %xmm6 #804.14
        mulsd     %xmm14, %xmm2                                 #805.26
        movups    (%rsp), %xmm8                                 #816.52[spill]
        addsd     %xmm6, %xmm2                                  #805.14
        mulsd     %xmm14, %xmm2                                 #806.14
        mulsd     %xmm15, %xmm2                                 #810.26
        addsd     %xmm1, %xmm2                                  #810.14
        addsd     %xmm2, %xmm0                                  #811.18
        addsd     8(%rdx,%rdi), %xmm0                           #812.18
        pxor      %xmm9, %xmm0                                  #813.18
        addsd     %xmm10, %xmm0                                 #814.18
        orps      %xmm11, %xmm0                                 #815.14
        movups    32(%rsp), %xmm11                              #816.52[spill]
        testb     $1, %cl                                       #816.41
        jne       ..B2.8        # Prob 5%                       #816.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm7 xmm8 xmm11 xmm12 xmm13
..B2.2:                         # Preds ..B2.8 ..B2.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #889.52
        jne       ..B2.4        # Prob 5%                       #889.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm7 xmm8
..B2.3:                         # Preds ..B2.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #892.12
        popq      %rbp                                          #892.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #892.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.4:                         # Preds ..B2.2
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm7, (%rsp)                                 #889.204
        movsd     %xmm8, 64(%rsp)                               #889.277
        movsd     %xmm0, 128(%rsp)                              #889.350
        jne       ..B2.6        # Prob 5%                       #889.491
                                # LOE rbx r12 r13 r14 r15 eax
..B2.5:                         # Preds ..B2.7 ..B2.6 ..B2.4
                                # Execution count [5.00e-02]: Infreq
        movsd     128(%rsp), %xmm0                              #889.757
        movq      %rbp, %rsp                                    #889.757
        popq      %rbp                                          #889.757
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #889.757
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.6:                         # Preds ..B2.4
                                # Execution count [2.50e-03]: Infreq
        je        ..B2.5        # Prob 95%                      #889.634
                                # LOE rbx r12 r13 r14 r15
..B2.7:                         # Preds ..B2.6
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #889.663
        lea       64(%rsp), %rsi                                #889.663
        lea       128(%rsp), %rdx                               #889.663
..___tag_value___svml_atan21_ha_ex.53:
#       __svml_datan2_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_datan2_ha_cout_rare_internal           #889.663
..___tag_value___svml_atan21_ha_ex.54:
        jmp       ..B2.5        # Prob 100%                     #889.663
                                # LOE rbx r12 r13 r14 r15
..B2.8:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        movaps    %xmm11, %xmm9                                 #854.21
        movaps    %xmm8, %xmm14                                 #857.19
        movsd     3136+__svml_datan2_ha_data_internal(%rip), %xmm3 #855.24
        movaps    %xmm7, %xmm10                                 #858.19
        movsd     2880+__svml_datan2_ha_data_internal(%rip), %xmm1 #862.18
        cmpnltsd  %xmm12, %xmm9                                 #854.21
        cmpordsd  %xmm8, %xmm14                                 #857.19
        cmpordsd  %xmm7, %xmm10                                 #858.19
        movaps    %xmm9, %xmm2                                  #856.32
        andps     %xmm9, %xmm3                                  #855.24
        andnps    %xmm12, %xmm2                                 #856.32
        andps     %xmm11, %xmm9                                 #856.64
        orps      %xmm9, %xmm2                                  #856.21
        andps     %xmm10, %xmm14                                #859.20
        movsd     832+__svml_datan2_ha_data_internal(%rip), %xmm4 #851.25
        cmpeqsd   %xmm1, %xmm12                                 #862.18
        cmpeqsd   %xmm1, %xmm11                                 #863.18
        cmpeqsd   %xmm1, %xmm2                                  #870.17
        orps      %xmm11, %xmm12                                #864.20
        movaps    %xmm2, %xmm9                                  #872.27
        pshufd    $85, %xmm12, %xmm12                           #866.20
        andps     %xmm1, %xmm2                                  #872.65
        pshufd    $85, %xmm14, %xmm11                           #861.20
        movaps    %xmm4, %xmm5                                  #852.18
        pand      %xmm11, %xmm12                                #867.26
        andnps    %xmm3, %xmm9                                  #872.27
        movdqa    %xmm12, %xmm15                                #868.19
        andps     %xmm8, %xmm5                                  #852.18
        pshufd    $85, %xmm1, %xmm1                             #875.16
        pandn     %xmm13, %xmm15                                #868.19
        pshufd    $85, %xmm8, %xmm13                            #877.16
        orps      %xmm2, %xmm9                                  #872.16
        pcmpgtd   %xmm13, %xmm1                                 #878.20
        orps      %xmm5, %xmm9                                  #873.13
        movsd     3072+__svml_datan2_ha_data_internal(%rip), %xmm6 #850.18
        andps     %xmm7, %xmm4                                  #853.18
        pshufd    $0, %xmm1, %xmm2                              #879.26
        andps     %xmm6, %xmm2                                  #881.18
        pshufd    $0, %xmm12, %xmm3                             #884.32
        addsd     %xmm2, %xmm9                                  #882.17
        movmskps  %xmm15, %eax                                  #869.44
        orps      %xmm4, %xmm9                                  #883.19
        movdqa    %xmm3, %xmm4                                  #886.25
        andnps    %xmm0, %xmm4                                  #886.25
        andps     %xmm3, %xmm9                                  #886.68
        movaps    %xmm4, %xmm0                                  #886.14
        andl      $1, %eax                                      #869.96
        orps      %xmm9, %xmm0                                  #886.14
        jmp       ..B2.2        # Prob 100%                     #886.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm7 xmm8
	.cfi_endproc
# mark_end;
	.type	__svml_atan21_ha_ex,@function
	.size	__svml_atan21_ha_ex,.-__svml_atan21_ha_ex
..LN__svml_atan21_ha_ex.1:
	.data
# -- End  __svml_atan21_ha_ex
	.text
.L_2__routine_start___svml_atan24_ha_l9_2:
# -- Begin  __svml_atan24_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan24_ha_l9
# --- __svml_atan24_ha_l9(__m256d, __m256d)
__svml_atan24_ha_l9:
# parameter 1: %ymm0
# parameter 2: %ymm1
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #898.1
        .byte     15                                            #1190.14
        .byte     30                                            #1190.14
        .byte     250                                           #1190.14
	.cfi_startproc
..___tag_value___svml_atan24_ha_l9.56:
..L57:
                                                         #898.1
        pushq     %rbp                                          #898.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #898.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #898.1
        subq      $256, %rsp                                    #898.1
        lea       __svml_datan2_ha_data_internal(%rip), %r8     #1068.513
        vmovupd   896+__svml_datan2_ha_data_internal(%rip), %ymm7 #1014.54
        xorl      %eax, %eax                                    #1036.5
        vmovups   2688+__svml_datan2_ha_data_internal(%rip), %xmm15 #1029.26
        vmovupd   %ymm0, (%rsp)                                 #898.1[spill]
        vmovapd   %ymm1, %ymm9                                  #898.1
        vandpd    %ymm7, %ymm9, %ymm8                           #1015.14
        vandpd    %ymm7, %ymm0, %ymm7                           #1016.14
        vxorpd    %ymm8, %ymm9, %ymm3                           #1017.17
        vxorpd    %ymm7, %ymm0, %ymm5                           #1018.17
        vcmplt_oqpd 2880+__svml_datan2_ha_data_internal(%rip), %ymm9, %ymm6 #1022.24
        vandpd    960+__svml_datan2_ha_data_internal(%rip), %ymm6, %ymm4 #1023.20
        vandpd    1024+__svml_datan2_ha_data_internal(%rip), %ymm6, %ymm1 #1024.20
        vmovups   2752+__svml_datan2_ha_data_internal(%rip), %xmm6 #1030.26
        vxorpd    %ymm3, %ymm1, %ymm1                           #1111.16
        vextractf128 $1, %ymm8, %xmm12                          #1027.124
        vextractf128 $1, %ymm7, %xmm2                           #1028.124
        vmulpd    1216+__svml_datan2_ha_data_internal(%rip), %ymm8, %ymm0 #1045.15
        vshufps   $221, %xmm12, %xmm8, %xmm14                   #1027.34
        vshufps   $221, %xmm2, %xmm7, %xmm13                    #1028.34
        vpsubd    %xmm15, %xmm14, %xmm11                        #1031.17
        vpsubd    %xmm15, %xmm13, %xmm2                         #1032.17
        vpcmpgtd  %xmm6, %xmm11, %xmm10                         #1033.32
        vpcmpeqd  %xmm6, %xmm11, %xmm12                         #1033.82
        vpcmpgtd  %xmm6, %xmm2, %xmm14                          #1034.32
        vpcmpeqd  %xmm6, %xmm2, %xmm6                           #1034.82
        vpor      %xmm12, %xmm10, %xmm15                        #1033.18
        vmulpd    1152+__svml_datan2_ha_data_internal(%rip), %ymm8, %ymm10 #1043.15
        vpor      %xmm6, %xmm14, %xmm11                         #1034.18
        vmulpd    1344+__svml_datan2_ha_data_internal(%rip), %ymm8, %ymm14 #1044.15
        vpor      %xmm11, %xmm15, %xmm6                         #1035.19
        vmulpd    1280+__svml_datan2_ha_data_internal(%rip), %ymm8, %ymm15 #1046.15
        vmovupd   1792+__svml_datan2_ha_data_internal(%rip), %ymm2 #1042.49
        vmovmskps %xmm6, %r9d                                   #1037.58
        vextractf128 $1, %ymm10, %xmm12                         #1051.130
        vextractf128 $1, %ymm14, %xmm11                         #1052.130
        vshufps   $221, %xmm12, %xmm10, %xmm10                  #1051.39
        vshufps   $221, %xmm11, %xmm14, %xmm11                  #1052.39
        vpsubd    %xmm13, %xmm10, %xmm10                        #1055.29
        vpsubd    %xmm13, %xmm11, %xmm11                        #1056.29
        vpsrad    $31, %xmm10, %xmm10                           #1059.28
        vextractf128 $1, %ymm0, %xmm12                          #1053.130
        vextractf128 $1, %ymm15, %xmm14                         #1054.130
        vshufps   $221, %xmm12, %xmm0, %xmm12                   #1053.39
        vshufps   $221, %xmm14, %xmm15, %xmm14                  #1054.39
        vpsubd    %xmm13, %xmm12, %xmm0                         #1057.29
        vpsubd    %xmm13, %xmm14, %xmm13                        #1058.29
        vpsrad    $31, %xmm11, %xmm15                           #1060.28
        vpsrad    $31, %xmm0, %xmm12                            #1061.28
        vpsrad    $31, %xmm13, %xmm14                           #1062.28
        vpaddd    %xmm15, %xmm10, %xmm15                        #1068.183
        vpaddd    %xmm14, %xmm12, %xmm12                        #1068.183
        vpaddd    %xmm12, %xmm15, %xmm13                        #1068.183
        vpaddd    1408+__svml_datan2_ha_data_internal(%rip), %xmm13, %xmm11 #1068.183
        vpslld    $5, %xmm11, %xmm12                            #1068.183
        vmovd     %xmm12, %edx                                  #1068.242
        vpextrd   $2, %xmm12, %esi                              #1068.373
        vpextrd   $1, %xmm12, %ecx                              #1068.306
        vmovq     (%rdx,%r8), %xmm10                            #1068.1029
        vpextrd   $3, %xmm12, %edi                              #1068.440
        vmovq     (%rsi,%r8), %xmm14                            #1068.1229
        vmovhpd   (%rcx,%r8), %xmm10, %xmm0                     #1068.997
        vmovhpd   (%rdi,%r8), %xmm14, %xmm15                    #1068.1197
        vmovq     8(%rdx,%r8), %xmm11                           #1069.1035
        vmovq     8(%rsi,%r8), %xmm12                           #1069.1235
        vmovhpd   8(%rcx,%r8), %xmm11, %xmm10                   #1069.1003
        vmovhpd   8(%rdi,%r8), %xmm12, %xmm14                   #1069.1203
        vinsertf128 $1, %xmm15, %ymm0, %ymm13                   #1068.951
        vmovq     16(%rdx,%r8), %xmm15                          #1070.1035
        vmovq     16(%rsi,%r8), %xmm0                           #1070.1235
        vmovhpd   16(%rcx,%r8), %xmm15, %xmm11                  #1070.1003
        vmovq     24(%rdx,%r8), %xmm15                          #1071.1026
        vinsertf128 $1, %xmm14, %ymm10, %ymm12                  #1069.957
        vmovhpd   16(%rdi,%r8), %xmm0, %xmm14                   #1070.1203
        vmovq     24(%rsi,%r8), %xmm0                           #1071.1226
        vmovhpd   24(%rcx,%r8), %xmm15, %xmm10                  #1071.994
        vmovhpd   24(%rdi,%r8), %xmm0, %xmm15                   #1071.1194
        vinsertf128 $1, %xmm14, %ymm11, %ymm14                  #1070.957
        vinsertf128 $1, %xmm15, %ymm10, %ymm11                  #1071.948
        vandpd    %ymm7, %ymm11, %ymm15                         #1072.24
        vandpd    %ymm8, %ymm11, %ymm0                          #1074.24
        vfnmadd231pd %ymm13, %ymm8, %ymm15                      #1073.23
        vfmadd213pd %ymm0, %ymm7, %ymm13                        #1075.23
        vmovups   1536+__svml_datan2_ha_data_internal(%rip), %xmm11 #1076.910
        vextractf128 $1, %ymm13, %xmm10                         #1076.1605
        vshufps   $221, %xmm10, %xmm13, %xmm0                   #1076.1516
        vpslld    $3, %xmm0, %xmm10                             #1076.1795
        vandps    1600+__svml_datan2_ha_data_internal(%rip), %xmm10, %xmm10 #1076.1837
        vorps     1664+__svml_datan2_ha_data_internal(%rip), %xmm10, %xmm10 #1076.1898
        vrcpps    %xmm10, %xmm10                                #1076.1986
        vandps    1472+__svml_datan2_ha_data_internal(%rip), %xmm0, %xmm0 #1076.1685
        vpsrld    $3, %xmm10, %xmm10                            #1076.2060
        vpsubd    1728+__svml_datan2_ha_data_internal(%rip), %xmm10, %xmm10 #1076.2103
        vpsubd    %xmm0, %xmm11, %xmm11                         #1076.1741
        vpaddd    %xmm11, %xmm10, %xmm0                         #1076.2155
        vpshufd   $80, %xmm0, %xmm11                            #1076.2258
        vpshufd   $250, %xmm0, %xmm10                           #1076.2313
        vmovapd   %ymm2, %ymm0                                  #1077.21
        vinsertf128 $1, %xmm10, %ymm11, %ymm11                  #1076.2209
        vandpd    3008+__svml_datan2_ha_data_internal(%rip), %ymm11, %ymm11 #1076.2424
        vfnmadd231pd %ymm13, %ymm11, %ymm0                      #1077.21
        vfmadd213pd %ymm0, %ymm0, %ymm0                         #1078.21
        vfmadd213pd %ymm11, %ymm0, %ymm11                       #1079.18
        vmovupd   1920+__svml_datan2_ha_data_internal(%rip), %ymm0 #1100.14
        vfnmadd231pd %ymm13, %ymm11, %ymm2                      #1080.21
        vfmadd213pd %ymm11, %ymm2, %ymm11                       #1081.22
        vmulpd    %ymm11, %ymm15, %ymm2                         #1082.17
        vfnmadd213pd %ymm15, %ymm2, %ymm13                      #1083.21
        vmulpd    %ymm2, %ymm2, %ymm10                          #1097.14
        vmulpd    %ymm13, %ymm11, %ymm15                        #1084.19
        vmovupd   1856+__svml_datan2_ha_data_internal(%rip), %ymm11 #1099.14
        vmulpd    %ymm10, %ymm10, %ymm13                        #1098.14
        vaddpd    %ymm15, %ymm14, %ymm14                        #1112.19
        vfmadd213pd 1984+__svml_datan2_ha_data_internal(%rip), %ymm13, %ymm11 #1099.14
        vfmadd213pd 2048+__svml_datan2_ha_data_internal(%rip), %ymm13, %ymm0 #1100.14
        vaddpd    %ymm14, %ymm1, %ymm1                          #1113.19
        vfmadd213pd 2112+__svml_datan2_ha_data_internal(%rip), %ymm13, %ymm11 #1101.14
        vfmadd213pd 2176+__svml_datan2_ha_data_internal(%rip), %ymm13, %ymm0 #1102.14
        vfmadd213pd 2240+__svml_datan2_ha_data_internal(%rip), %ymm13, %ymm11 #1103.14
        vfmadd213pd 2304+__svml_datan2_ha_data_internal(%rip), %ymm13, %ymm0 #1104.14
        vfmadd213pd 2368+__svml_datan2_ha_data_internal(%rip), %ymm13, %ymm11 #1105.14
        vfmadd213pd 2432+__svml_datan2_ha_data_internal(%rip), %ymm13, %ymm0 #1106.14
        vfmadd213pd 2496+__svml_datan2_ha_data_internal(%rip), %ymm13, %ymm11 #1107.14
        vfmadd213pd 2560+__svml_datan2_ha_data_internal(%rip), %ymm13, %ymm0 #1108.14
        vfmadd213pd %ymm0, %ymm10, %ymm11                       #1109.14
        vmulpd    %ymm11, %ymm10, %ymm0                         #1110.14
        vfmadd213pd %ymm1, %ymm2, %ymm0                         #1114.14
        vaddpd    %ymm0, %ymm2, %ymm2                           #1115.18
        vaddpd    %ymm2, %ymm12, %ymm10                         #1116.18
        vxorpd    %ymm3, %ymm10, %ymm3                          #1117.18
        vaddpd    %ymm3, %ymm4, %ymm4                           #1118.18
        vorpd     %ymm5, %ymm4, %ymm0                           #1119.14
        testl     %r9d, %r9d                                    #1120.52
        jne       ..B3.12       # Prob 5%                       #1120.52
                                # LOE rbx r12 r13 r14 r15 eax xmm6 ymm0 ymm7 ymm8 ymm9
..B3.2:                         # Preds ..B3.12 ..B3.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #1193.52
        jne       ..B3.4        # Prob 5%                       #1193.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm9
..B3.3:                         # Preds ..B3.4 ..B3.10 ..B3.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1196.12
        popq      %rbp                                          #1196.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1196.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.4:                         # Preds ..B3.2
                                # Execution count [5.00e-02]: Infreq
        vmovupd   (%rsp), %ymm1                                 #1193.244[spill]
        vmovupd   %ymm9, 128(%rsp)                              #1193.320
        vmovupd   %ymm0, 192(%rsp)                              #1193.396
        vmovupd   %ymm1, 64(%rsp)                               #1193.244
        je        ..B3.3        # Prob 95%                      #1193.500
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B3.7:                         # Preds ..B3.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1193.580
                                # LOE rbx r12 r13 r14 r15 eax edx
..B3.15:                        # Preds ..B3.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.9 ..B3.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1193.643
        jc        ..B3.11       # Prob 5%                       #1193.643
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.11 ..B3.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1193.596
        cmpl      $4, %r12d                                     #1193.591
        jl        ..B3.8        # Prob 82%                      #1193.591
                                # LOE rbx r12 r14 r15 r13d
..B3.10:                        # Preds ..B3.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   192(%rsp), %ymm0                              #1193.817
        jmp       ..B3.3        # Prob 100%                     #1193.817
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B3.11:                        # Preds ..B3.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1193.672
        lea       128(%rsp,%r12,8), %rsi                        #1193.672
        lea       192(%rsp,%r12,8), %rdx                        #1193.672
..___tag_value___svml_atan24_ha_l9.76:
#       __svml_datan2_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_datan2_ha_cout_rare_internal           #1193.672
..___tag_value___svml_atan24_ha_l9.77:
        jmp       ..B3.9        # Prob 100%                     #1193.672
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B3.12:                        # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   (%rsp), %ymm13                                #1157.18[spill]
        vmovupd   832+__svml_datan2_ha_data_internal(%rip), %ymm11 #1155.59
        vcmpordpd %ymm9, %ymm9, %ymm14                          #1161.19
        vcmpordpd %ymm13, %ymm13, %ymm15                        #1162.19
        vcmpnlt_uqpd %ymm8, %ymm7, %ymm12                       #1158.21
        vandpd    %ymm15, %ymm14, %ymm2                         #1163.20
        vblendvpd %ymm12, %ymm7, %ymm8, %ymm3                   #1160.21
        vandpd    %ymm11, %ymm9, %ymm5                          #1156.18
        vandpd    %ymm11, %ymm13, %ymm10                        #1157.18
        vandpd    3136+__svml_datan2_ha_data_internal(%rip), %ymm12, %ymm4 #1159.24
        vextractf128 $1, %ymm2, %xmm1                           #1165.136
        vshufps   $221, %xmm1, %xmm2, %xmm1                     #1165.40
        vmovupd   2880+__svml_datan2_ha_data_internal(%rip), %ymm2 #1166.18
        vcmpeqpd  %ymm2, %ymm8, %ymm8                           #1166.18
        vcmpeqpd  %ymm2, %ymm7, %ymm7                           #1167.18
        vorpd     %ymm7, %ymm8, %ymm8                           #1168.20
        vextractf128 $1, %ymm8, %xmm11                          #1170.136
        vshufps   $221, %xmm11, %xmm8, %xmm12                   #1170.40
        vpand     %xmm1, %xmm12, %xmm7                          #1171.28
        vcmpeqpd  %ymm2, %ymm3, %ymm1                           #1174.17
        vpandn    %xmm6, %xmm7, %xmm6                           #1172.21
        vmovmskps %xmm6, %eax                                   #1173.44
        vblendvpd %ymm1, %ymm2, %ymm4, %ymm3                    #1176.16
        vorpd     %ymm5, %ymm3, %ymm6                           #1177.13
        vextractf128 $1, %ymm2, %xmm4                           #1179.128
        vextractf128 $1, %ymm9, %xmm5                           #1181.128
        vshufps   $221, %xmm4, %xmm2, %xmm13                    #1179.36
        vshufps   $221, %xmm5, %xmm9, %xmm14                    #1181.36
        vpcmpgtd  %xmm14, %xmm13, %xmm1                         #1182.22
        vpshufd   $80, %xmm1, %xmm15                            #1183.75
        vpshufd   $250, %xmm1, %xmm2                            #1183.132
        vinsertf128 $1, %xmm2, %ymm15, %ymm3                    #1183.26
        vandpd    3072+__svml_datan2_ha_data_internal(%rip), %ymm3, %ymm4 #1185.18
        vaddpd    %ymm4, %ymm6, %ymm5                           #1186.17
        vorpd     %ymm10, %ymm5, %ymm1                          #1187.19
        vpshufd   $80, %xmm7, %xmm10                            #1188.81
        vpshufd   $250, %xmm7, %xmm7                            #1188.148
        vinsertf128 $1, %xmm7, %ymm10, %ymm2                    #1188.32
        vblendvpd %ymm2, %ymm1, %ymm0, %ymm0                    #1190.14
        jmp       ..B3.2        # Prob 100%                     #1190.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm9
	.cfi_endproc
# mark_end;
	.type	__svml_atan24_ha_l9,@function
	.size	__svml_atan24_ha_l9,.-__svml_atan24_ha_l9
..LN__svml_atan24_ha_l9.2:
	.data
# -- End  __svml_atan24_ha_l9
	.text
.L_2__routine_start___svml_atan24_ha_e9_3:
# -- Begin  __svml_atan24_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan24_ha_e9
# --- __svml_atan24_ha_e9(__m256d, __m256d)
__svml_atan24_ha_e9:
# parameter 1: %ymm0
# parameter 2: %ymm1
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1202.1
        .byte     15                                            #1525.14
        .byte     30                                            #1525.14
        .byte     250                                           #1525.14
	.cfi_startproc
..___tag_value___svml_atan24_ha_e9.82:
..L83:
                                                         #1202.1
        pushq     %rbp                                          #1202.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1202.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1202.1
        subq      $256, %rsp                                    #1202.1
        lea       __svml_datan2_ha_data_internal(%rip), %r8     #1383.537
        vmovupd   896+__svml_datan2_ha_data_internal(%rip), %ymm13 #1318.54
        xorl      %eax, %eax                                    #1351.5
        vmovupd   1088+__svml_datan2_ha_data_internal(%rip), %ymm8 #1323.62
        vmovups   2688+__svml_datan2_ha_data_internal(%rip), %xmm2 #1338.30
        vmovupd   %ymm0, (%rsp)                                 #1202.1[spill]
        vmovupd   %ymm1, 32(%rsp)                               #1202.1[spill]
        vandpd    %ymm13, %ymm0, %ymm5                          #1320.14
        vxorpd    %ymm5, %ymm0, %ymm9                           #1322.17
        vmovupd   %ymm9, 128(%rsp)                              #1322.17[spill]
        vmovupd   %ymm5, 96(%rsp)                               #1320.14[spill]
        vcmplt_oqpd 2880+__svml_datan2_ha_data_internal(%rip), %ymm1, %ymm9 #1331.24
        vandpd    %ymm13, %ymm1, %ymm6                          #1319.14
        vandpd    %ymm8, %ymm5, %ymm12                          #1324.19
        vandpd    %ymm8, %ymm6, %ymm7                           #1326.19
        vandpd    960+__svml_datan2_ha_data_internal(%rip), %ymm9, %ymm15 #1332.20
        vmovupd   %ymm6, 64(%rsp)                               #1319.14[spill]
        vmovupd   %ymm15, 160(%rsp)                             #1332.20[spill]
        vsubpd    %ymm12, %ymm5, %ymm11                         #1325.19
        vsubpd    %ymm7, %ymm6, %ymm10                          #1327.19
        vxorpd    %ymm6, %ymm1, %ymm13                          #1321.17
        vandpd    1024+__svml_datan2_ha_data_internal(%rip), %ymm9, %ymm9 #1333.20
        vxorpd    %ymm13, %ymm9, %ymm9                          #1446.16
        vextractf128 $1, %ymm6, %xmm15                          #1334.107
        vextractf128 $1, %ymm5, %xmm4                           #1335.107
        vshufps   $221, %xmm15, %xmm6, %xmm3                    #1336.38
        vshufps   $221, %xmm4, %xmm5, %xmm5                     #1337.38
        vpsubd    %xmm2, %xmm3, %xmm15                          #1340.21
        vpsubd    %xmm3, %xmm5, %xmm4                           #1345.21
        vpsubd    %xmm2, %xmm5, %xmm14                          #1341.21
        vpaddd    3200+__svml_datan2_ha_data_internal(%rip), %xmm4, %xmm0 #1348.21
        vmovups   2752+__svml_datan2_ha_data_internal(%rip), %xmm4 #1342.36
        vpcmpgtd  %xmm4, %xmm15, %xmm2                          #1342.36
        vpcmpeqd  %xmm4, %xmm15, %xmm15                         #1342.94
        vpcmpgtd  %xmm4, %xmm14, %xmm1                          #1343.36
        vpcmpeqd  %xmm4, %xmm14, %xmm14                         #1343.94
        vmovups   3264+__svml_datan2_ha_data_internal(%rip), %xmm4 #1349.36
        vpor      %xmm15, %xmm2, %xmm3                          #1342.22
        vpor      %xmm14, %xmm1, %xmm15                         #1343.22
        vpcmpgtd  %xmm0, %xmm4, %xmm2                           #1349.36
        vpcmpeqd  %xmm0, %xmm4, %xmm1                           #1349.86
        vpor      %xmm15, %xmm3, %xmm14                         #1344.23
        vpor      %xmm1, %xmm2, %xmm0                           #1349.22
        vmulpd    1216+__svml_datan2_ha_data_internal(%rip), %ymm6, %ymm2 #1360.15
        vpor      %xmm0, %xmm14, %xmm15                         #1350.23
        vmulpd    1152+__svml_datan2_ha_data_internal(%rip), %ymm6, %ymm14 #1358.15
        vmulpd    1344+__svml_datan2_ha_data_internal(%rip), %ymm6, %ymm0 #1359.15
        vmulpd    1280+__svml_datan2_ha_data_internal(%rip), %ymm6, %ymm6 #1361.15
        vmovdqu   %xmm15, 192(%rsp)                             #1350.23[spill]
        vextractf128 $1, %ymm14, %xmm15                         #1362.114
        vextractf128 $1, %ymm0, %xmm1                           #1363.114
        vextractf128 $1, %ymm2, %xmm3                           #1364.114
        vextractf128 $1, %ymm6, %xmm4                           #1365.114
        vshufps   $221, %xmm15, %xmm14, %xmm14                  #1366.43
        vshufps   $221, %xmm1, %xmm0, %xmm0                     #1367.43
        vshufps   $221, %xmm3, %xmm2, %xmm2                     #1368.43
        vpsubd    %xmm5, %xmm0, %xmm1                           #1371.33
        vshufps   $221, %xmm4, %xmm6, %xmm6                     #1369.43
        vpsubd    %xmm5, %xmm14, %xmm4                          #1370.33
        vpsubd    %xmm5, %xmm2, %xmm2                           #1372.33
        vpsubd    %xmm5, %xmm6, %xmm6                           #1373.33
        vpsrad    $31, %xmm4, %xmm15                            #1374.32
        vpsrad    $31, %xmm1, %xmm3                             #1375.32
        vpsrad    $31, %xmm2, %xmm4                             #1376.32
        vpsrad    $31, %xmm6, %xmm5                             #1377.32
        vpaddd    %xmm3, %xmm15, %xmm15                         #1383.187
        vpaddd    %xmm5, %xmm4, %xmm6                           #1383.187
        vpaddd    %xmm6, %xmm15, %xmm4                          #1383.187
        vpaddd    1408+__svml_datan2_ha_data_internal(%rip), %xmm4, %xmm2 #1383.187
        vpslld    $5, %xmm2, %xmm3                              #1383.187
        vmovd     %xmm3, %edx                                   #1383.250
        vpextrd   $2, %xmm3, %esi                               #1383.389
        vpextrd   $1, %xmm3, %ecx                               #1383.318
        vmovq     (%rdx,%r8), %xmm5                             #1383.1053
        vpextrd   $3, %xmm3, %edi                               #1383.460
        vmovq     (%rsi,%r8), %xmm0                             #1383.1253
        vmovq     8(%rsi,%r8), %xmm4                            #1384.1259
        vmovhpd   (%rcx,%r8), %xmm5, %xmm1                      #1383.1021
        vmovq     8(%rdx,%r8), %xmm15                           #1384.1059
        vmovq     16(%rdx,%r8), %xmm5                           #1385.1059
        vmovhpd   (%rdi,%r8), %xmm0, %xmm14                     #1383.1221
        vmovq     16(%rsi,%r8), %xmm0                           #1385.1259
        vmovhpd   8(%rcx,%r8), %xmm15, %xmm6                    #1384.1027
        vmovhpd   8(%rdi,%r8), %xmm4, %xmm2                     #1384.1227
        vmovq     24(%rdx,%r8), %xmm4                           #1386.1050
        vinsertf128 $1, %xmm14, %ymm1, %ymm3                    #1383.975
        vmovhpd   16(%rcx,%r8), %xmm5, %xmm1                    #1385.1027
        vmovhpd   16(%rdi,%r8), %xmm0, %xmm14                   #1385.1227
        vmovq     24(%rsi,%r8), %xmm5                           #1386.1250
        vinsertf128 $1, %xmm2, %ymm6, %ymm15                    #1384.981
        vmovhpd   24(%rcx,%r8), %xmm4, %xmm2                    #1386.1018
        vinsertf128 $1, %xmm14, %ymm1, %ymm6                    #1385.981
        vmovhpd   24(%rdi,%r8), %xmm5, %xmm1                    #1386.1218
        vmulpd    %ymm3, %ymm7, %ymm14                          #1389.45
        vinsertf128 $1, %xmm1, %ymm2, %ymm1                     #1386.972
        vmulpd    %ymm10, %ymm3, %ymm2                          #1390.45
        vandpd    %ymm12, %ymm1, %ymm0                          #1387.20
        vandpd    %ymm11, %ymm1, %ymm4                          #1388.20
        vsubpd    %ymm14, %ymm0, %ymm5                          #1389.20
        vsubpd    %ymm2, %ymm4, %ymm0                           #1390.20
        vmulpd    %ymm11, %ymm3, %ymm11                         #1400.35
        vsubpd    %ymm0, %ymm5, %ymm2                           #1391.20
        vsubpd    %ymm5, %ymm2, %ymm14                          #1392.21
        vandpd    %ymm8, %ymm2, %ymm4                           #1394.19
        vsubpd    %ymm14, %ymm0, %ymm5                          #1393.20
        vsubpd    %ymm4, %ymm2, %ymm2                           #1395.22
        vaddpd    %ymm2, %ymm5, %ymm5                           #1396.19
        vandpd    %ymm10, %ymm1, %ymm2                          #1398.20
        vmulpd    %ymm3, %ymm12, %ymm10                         #1399.35
        vmovups   1536+__svml_datan2_ha_data_internal(%rip), %xmm3 #1407.918
        vandpd    %ymm7, %ymm1, %ymm7                           #1397.20
        vaddpd    %ymm10, %ymm7, %ymm12                         #1399.20
        vaddpd    %ymm11, %ymm2, %ymm10                         #1400.20
        vaddpd    %ymm10, %ymm12, %ymm11                        #1401.20
        vsubpd    %ymm11, %ymm12, %ymm7                         #1402.23
        vandpd    %ymm8, %ymm11, %ymm12                         #1404.19
        vsubpd    %ymm12, %ymm11, %ymm8                         #1405.22
        vaddpd    %ymm7, %ymm10, %ymm2                          #1403.20
        vaddpd    %ymm8, %ymm2, %ymm10                          #1406.19
        vextractf128 $1, %ymm12, %xmm7                          #1407.1566
        vshufps   $221, %xmm7, %xmm12, %xmm14                   #1407.1638
        vpslld    $3, %xmm14, %xmm11                            #1407.1896
        vandps    1600+__svml_datan2_ha_data_internal(%rip), %xmm11, %xmm8 #1407.1946
        vorps     1664+__svml_datan2_ha_data_internal(%rip), %xmm8, %xmm2 #1407.2019
        vrcpps    %xmm2, %xmm1                                  #1407.2119
        vandps    1472+__svml_datan2_ha_data_internal(%rip), %xmm14, %xmm7 #1407.1762
        vpsrld    $3, %xmm1, %xmm0                              #1407.2201
        vpsubd    1728+__svml_datan2_ha_data_internal(%rip), %xmm0, %xmm11 #1407.2252
        vpsubd    %xmm7, %xmm3, %xmm3                           #1407.1830
        vpaddd    %xmm3, %xmm11, %xmm11                         #1407.2316
        vpshufd   $80, %xmm11, %xmm7                            #1407.2378
        vpshufd   $250, %xmm11, %xmm8                           #1407.2450
        vinsertf128 $1, %xmm8, %ymm7, %ymm2                     #1407.2517
        vandpd    3008+__svml_datan2_ha_data_internal(%rip), %ymm2, %ymm2 #1407.2648
        vmulpd    %ymm2, %ymm12, %ymm12                         #1408.34
        vmulpd    %ymm10, %ymm2, %ymm10                         #1409.32
        vsubpd    1792+__svml_datan2_ha_data_internal(%rip), %ymm12, %ymm1 #1408.19
        vaddpd    %ymm10, %ymm1, %ymm1                          #1409.17
        vmulpd    %ymm1, %ymm1, %ymm10                          #1410.34
        vsubpd    %ymm1, %ymm10, %ymm12                         #1410.19
        vmulpd    %ymm12, %ymm1, %ymm7                          #1411.34
        vaddpd    %ymm7, %ymm1, %ymm11                          #1411.19
        vmulpd    %ymm11, %ymm1, %ymm8                          #1412.34
        vsubpd    %ymm1, %ymm8, %ymm0                           #1412.19
        vmulpd    %ymm0, %ymm1, %ymm3                           #1413.34
        vaddpd    %ymm3, %ymm1, %ymm14                          #1413.19
        vmulpd    %ymm14, %ymm1, %ymm10                         #1414.34
        vsubpd    %ymm1, %ymm10, %ymm1                          #1414.19
        vmulpd    %ymm1, %ymm2, %ymm10                          #1415.21
        vmulpd    %ymm10, %ymm4, %ymm12                         #1416.18
        vmulpd    %ymm10, %ymm5, %ymm7                          #1417.33
        vmulpd    %ymm5, %ymm2, %ymm5                           #1418.34
        vmulpd    %ymm2, %ymm4, %ymm10                          #1419.32
        vaddpd    %ymm7, %ymm12, %ymm11                         #1417.18
        vaddpd    %ymm5, %ymm11, %ymm7                          #1418.19
        vaddpd    %ymm10, %ymm7, %ymm12                         #1419.17
        vaddpd    %ymm7, %ymm6, %ymm6                           #1447.19
        vmulpd    %ymm12, %ymm12, %ymm11                        #1432.14
        vmulpd    %ymm11, %ymm11, %ymm8                         #1433.14
        vmulpd    1856+__svml_datan2_ha_data_internal(%rip), %ymm8, %ymm4 #1434.29
        vmulpd    1920+__svml_datan2_ha_data_internal(%rip), %ymm8, %ymm0 #1435.29
        vaddpd    1984+__svml_datan2_ha_data_internal(%rip), %ymm4, %ymm1 #1434.14
        vaddpd    2048+__svml_datan2_ha_data_internal(%rip), %ymm0, %ymm3 #1435.14
        vmulpd    %ymm1, %ymm8, %ymm2                           #1436.29
        vmulpd    %ymm3, %ymm8, %ymm4                           #1437.29
        vaddpd    2112+__svml_datan2_ha_data_internal(%rip), %ymm2, %ymm5 #1436.14
        vaddpd    2176+__svml_datan2_ha_data_internal(%rip), %ymm4, %ymm0 #1437.14
        vmulpd    %ymm5, %ymm8, %ymm14                          #1438.29
        vmulpd    %ymm0, %ymm8, %ymm1                           #1439.29
        vaddpd    2240+__svml_datan2_ha_data_internal(%rip), %ymm14, %ymm2 #1438.14
        vaddpd    2304+__svml_datan2_ha_data_internal(%rip), %ymm1, %ymm4 #1439.14
        vmulpd    %ymm2, %ymm8, %ymm3                           #1440.29
        vmulpd    %ymm4, %ymm8, %ymm5                           #1441.29
        vaddpd    2368+__svml_datan2_ha_data_internal(%rip), %ymm3, %ymm14 #1440.14
        vaddpd    2432+__svml_datan2_ha_data_internal(%rip), %ymm5, %ymm0 #1441.14
        vmulpd    %ymm14, %ymm8, %ymm14                         #1442.29
        vmulpd    %ymm0, %ymm8, %ymm8                           #1443.29
        vaddpd    2496+__svml_datan2_ha_data_internal(%rip), %ymm14, %ymm1 #1442.14
        vaddpd    2560+__svml_datan2_ha_data_internal(%rip), %ymm8, %ymm0 #1443.14
        vmulpd    %ymm1, %ymm11, %ymm2                          #1444.29
        vaddpd    %ymm2, %ymm0, %ymm3                           #1444.14
        vaddpd    %ymm6, %ymm9, %ymm0                           #1448.19
        vmovdqu   192(%rsp), %xmm9                              #1352.58[spill]
        vmulpd    %ymm3, %ymm11, %ymm11                         #1445.14
        vmovmskps %xmm9, %r9d                                   #1352.58
        vmulpd    %ymm11, %ymm12, %ymm1                         #1449.29
        vaddpd    %ymm1, %ymm0, %ymm2                           #1449.14
        vmovupd   96(%rsp), %ymm1                               #1455.52[spill]
        vaddpd    %ymm2, %ymm10, %ymm3                          #1450.18
        vmovupd   32(%rsp), %ymm10                              #1455.52[spill]
        vmovupd   64(%rsp), %ymm2                               #1455.52[spill]
        vaddpd    %ymm3, %ymm15, %ymm15                         #1451.18
        vxorpd    %ymm13, %ymm15, %ymm13                        #1452.18
        vaddpd    160(%rsp), %ymm13, %ymm0                      #1453.18[spill]
        vorpd     128(%rsp), %ymm0, %ymm0                       #1454.14[spill]
        testl     %r9d, %r9d                                    #1455.52
        jne       ..B4.12       # Prob 5%                       #1455.52
                                # LOE rbx r12 r13 r14 r15 eax xmm9 ymm0 ymm1 ymm2 ymm10
..B4.2:                         # Preds ..B4.12 ..B4.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #1528.52
        jne       ..B4.4        # Prob 5%                       #1528.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm10
..B4.3:                         # Preds ..B4.4 ..B4.10 ..B4.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1531.12
        popq      %rbp                                          #1531.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1531.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.4:                         # Preds ..B4.2
                                # Execution count [5.00e-02]: Infreq
        vmovupd   (%rsp), %ymm1                                 #1528.244[spill]
        vmovupd   %ymm10, 128(%rsp)                             #1528.320
        vmovupd   %ymm0, 192(%rsp)                              #1528.396
        vmovupd   %ymm1, 64(%rsp)                               #1528.244
        je        ..B4.3        # Prob 95%                      #1528.500
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B4.7:                         # Preds ..B4.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1528.580
                                # LOE rbx r12 r13 r14 r15 eax edx
..B4.15:                        # Preds ..B4.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.9 ..B4.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1528.643
        jc        ..B4.11       # Prob 5%                       #1528.643
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.11 ..B4.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1528.596
        cmpl      $4, %r12d                                     #1528.591
        jl        ..B4.8        # Prob 82%                      #1528.591
                                # LOE rbx r12 r14 r15 r13d
..B4.10:                        # Preds ..B4.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   192(%rsp), %ymm0                              #1528.817
        jmp       ..B4.3        # Prob 100%                     #1528.817
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B4.11:                        # Preds ..B4.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1528.672
        lea       128(%rsp,%r12,8), %rsi                        #1528.672
        lea       192(%rsp,%r12,8), %rdx                        #1528.672
..___tag_value___svml_atan24_ha_e9.114:
#       __svml_datan2_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_datan2_ha_cout_rare_internal           #1528.672
..___tag_value___svml_atan24_ha_e9.115:
        jmp       ..B4.9        # Prob 100%                     #1528.672
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B4.12:                        # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   (%rsp), %ymm13                                #1492.18[spill]
        vmovupd   832+__svml_datan2_ha_data_internal(%rip), %ymm11 #1490.59
        vcmpordpd %ymm10, %ymm10, %ymm14                        #1496.19
        vcmpordpd %ymm13, %ymm13, %ymm15                        #1497.19
        vcmpnlt_uqpd %ymm2, %ymm1, %ymm12                       #1493.21
        vandpd    %ymm15, %ymm14, %ymm4                         #1498.20
        vblendvpd %ymm12, %ymm1, %ymm2, %ymm5                   #1495.21
        vandpd    %ymm11, %ymm10, %ymm7                         #1491.18
        vandpd    %ymm11, %ymm13, %ymm8                         #1492.18
        vandpd    3136+__svml_datan2_ha_data_internal(%rip), %ymm12, %ymm6 #1494.24
        vextractf128 $1, %ymm4, %xmm3                           #1499.125
        vshufps   $221, %xmm3, %xmm4, %xmm3                     #1500.44
        vmovupd   2880+__svml_datan2_ha_data_internal(%rip), %ymm4 #1501.18
        vcmpeqpd  %ymm4, %ymm2, %ymm2                           #1501.18
        vcmpeqpd  %ymm4, %ymm1, %ymm1                           #1502.18
        vorpd     %ymm1, %ymm2, %ymm11                          #1503.20
        vcmpeqpd  %ymm4, %ymm5, %ymm1                           #1509.17
        vextractf128 $1, %ymm11, %xmm12                         #1504.125
        vshufps   $221, %xmm12, %xmm11, %xmm13                  #1505.44
        vpand     %xmm3, %xmm13, %xmm2                          #1506.32
        vblendvpd %ymm1, %ymm4, %ymm6, %ymm3                    #1511.16
        vpandn    %xmm9, %xmm2, %xmm9                           #1507.25
        vmovmskps %xmm9, %eax                                   #1508.44
        vorpd     %ymm7, %ymm3, %ymm1                           #1512.13
        vextractf128 $1, %ymm4, %xmm5                           #1513.113
        vextractf128 $1, %ymm10, %xmm6                          #1515.111
        vshufps   $221, %xmm5, %xmm4, %xmm7                     #1514.40
        vshufps   $221, %xmm6, %xmm10, %xmm9                    #1516.40
        vpcmpgtd  %xmm9, %xmm7, %xmm12                          #1517.26
        vpshufd   $80, %xmm12, %xmm11                           #1518.26
        vpshufd   $250, %xmm12, %xmm13                          #1518.102
        vinsertf128 $1, %xmm13, %ymm11, %ymm14                  #1519.20
        vandpd    3072+__svml_datan2_ha_data_internal(%rip), %ymm14, %ymm15 #1520.18
        vaddpd    %ymm15, %ymm1, %ymm1                          #1521.17
        vorpd     %ymm8, %ymm1, %ymm1                           #1522.19
        vpshufd   $80, %xmm2, %xmm8                             #1523.32
        vpshufd   $250, %xmm2, %xmm2                            #1523.128
        vinsertf128 $1, %xmm2, %ymm8, %ymm2                     #1524.24
        vblendvpd %ymm2, %ymm1, %ymm0, %ymm0                    #1525.14
        jmp       ..B4.2        # Prob 100%                     #1525.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm10
	.cfi_endproc
# mark_end;
	.type	__svml_atan24_ha_e9,@function
	.size	__svml_atan24_ha_e9,.-__svml_atan24_ha_e9
..LN__svml_atan24_ha_e9.3:
	.data
# -- End  __svml_atan24_ha_e9
	.text
.L_2__routine_start___svml_atan22_ha_l9_4:
# -- Begin  __svml_atan22_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan22_ha_l9
# --- __svml_atan22_ha_l9(__m128d, __m128d)
__svml_atan22_ha_l9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1537.1
        .byte     15                                            #1812.96
        .byte     30                                            #1812.96
        .byte     250                                           #1812.96
	.cfi_startproc
..___tag_value___svml_atan22_ha_l9.120:
..L121:
                                                        #1537.1
        pushq     %rbp                                          #1537.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1537.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1537.1
        subq      $256, %rsp                                    #1537.1
        vmovapd   %xmm1, %xmm13                                 #1537.1
        vcmpltpd  2880+__svml_datan2_ha_data_internal(%rip), %xmm13, %xmm12 #1661.24
        vmovupd   896+__svml_datan2_ha_data_internal(%rip), %xmm10 #1653.51
        lea       __svml_datan2_ha_data_internal(%rip), %rsi    #1707.377
        vandpd    %xmm10, %xmm13, %xmm11                        #1654.14
        vandpd    %xmm10, %xmm0, %xmm10                         #1655.14
        vpshufd   $221, %xmm10, %xmm1                           #1667.16
        vxorpd    %xmm10, %xmm0, %xmm9                          #1657.17
        vmovq     2688+__svml_datan2_ha_data_internal(%rip), %xmm15 #1668.26
        vxorpd    %xmm11, %xmm13, %xmm7                         #1656.17
        vpshufd   $221, %xmm11, %xmm5                           #1666.16
        vpsubd    %xmm15, %xmm1, %xmm3                          #1671.17
        vandpd    960+__svml_datan2_ha_data_internal(%rip), %xmm12, %xmm8 #1662.20
        vpsubd    %xmm15, %xmm5, %xmm4                          #1670.17
        vandpd    1024+__svml_datan2_ha_data_internal(%rip), %xmm12, %xmm6 #1663.20
        xorl      %edx, %edx                                    #1675.5
        vmovq     2752+__svml_datan2_ha_data_internal(%rip), %xmm12 #1669.26
        vxorpd    %xmm7, %xmm6, %xmm6                           #1750.16
        vpcmpgtd  %xmm12, %xmm4, %xmm14                         #1672.32
        vpcmpeqd  %xmm12, %xmm4, %xmm2                          #1672.82
        vpcmpgtd  %xmm12, %xmm3, %xmm5                          #1673.32
        vpcmpeqd  %xmm12, %xmm3, %xmm12                         #1673.82
        vpor      %xmm2, %xmm14, %xmm15                         #1672.18
        vpor      %xmm12, %xmm5, %xmm4                          #1673.18
        vmulpd    1152+__svml_datan2_ha_data_internal(%rip), %xmm11, %xmm2 #1682.15
        vmulpd    1216+__svml_datan2_ha_data_internal(%rip), %xmm11, %xmm5 #1684.15
        vmulpd    1344+__svml_datan2_ha_data_internal(%rip), %xmm11, %xmm3 #1683.15
        vpor      %xmm4, %xmm15, %xmm12                         #1674.19
        vmulpd    1280+__svml_datan2_ha_data_internal(%rip), %xmm11, %xmm15 #1685.15
        vmovmskps %xmm12, %edi                                  #1676.58
        vmovupd   %xmm0, (%rsp)                                 #1537.1[spill]
        vpshufd   $221, %xmm2, %xmm0                            #1690.21
        vpshufd   $221, %xmm3, %xmm3                            #1691.21
        vpsubd    %xmm1, %xmm0, %xmm4                           #1694.29
        vpshufd   $221, %xmm5, %xmm5                            #1692.21
        vpsrad    $31, %xmm4, %xmm2                             #1698.28
        vpshufd   $221, %xmm15, %xmm15                          #1693.21
        vpsubd    %xmm1, %xmm3, %xmm4                           #1695.29
        vpsubd    %xmm1, %xmm5, %xmm3                           #1696.29
        vpsubd    %xmm1, %xmm15, %xmm1                          #1697.29
        vpsrad    $31, %xmm4, %xmm4                             #1699.28
        vpsrad    $31, %xmm3, %xmm5                             #1700.28
        vpsrad    $31, %xmm1, %xmm15                            #1701.28
        vpaddd    %xmm4, %xmm2, %xmm4                           #1707.183
        vpaddd    %xmm15, %xmm5, %xmm5                          #1707.183
        vmovq     1408+__svml_datan2_ha_data_internal(%rip), %xmm14 #1705.19
        vpaddd    %xmm5, %xmm4, %xmm2                           #1707.183
        vpaddd    %xmm14, %xmm2, %xmm14                         #1707.183
        vpslld    $5, %xmm14, %xmm5                             #1707.183
        vmovd     %xmm5, %eax                                   #1707.242
        vmovq     1664+__svml_datan2_ha_data_internal(%rip), %xmm1 #1715.1101
        vpextrd   $1, %xmm5, %ecx                               #1707.306
        vmovq     24(%rax,%rsi), %xmm2                          #1710.612
        vmovq     (%rax,%rsi), %xmm15                           #1707.615
        vmovhpd   24(%rcx,%rsi), %xmm2, %xmm3                   #1710.580
        vmovhpd   (%rcx,%rsi), %xmm15, %xmm15                   #1707.583
        vandpd    %xmm10, %xmm3, %xmm2                          #1711.24
        vandpd    %xmm11, %xmm3, %xmm3                          #1713.24
        vfmadd231pd %xmm15, %xmm10, %xmm3                       #1714.23
        vmovq     8(%rax,%rsi), %xmm4                           #1708.621
        vmovq     16(%rax,%rsi), %xmm14                         #1709.621
        vmovhpd   8(%rcx,%rsi), %xmm4, %xmm5                    #1708.589
        vmovhpd   16(%rcx,%rsi), %xmm14, %xmm4                  #1709.589
        vpshufd   $221, %xmm3, %xmm0                            #1715.1422
        vfnmadd231pd %xmm15, %xmm11, %xmm2                      #1712.23
        vpslld    $3, %xmm0, %xmm15                             #1715.1596
        vmovq     1600+__svml_datan2_ha_data_internal(%rip), %xmm14 #1715.1000
        vpand     %xmm14, %xmm15, %xmm15                        #1715.1638
        vpor      %xmm1, %xmm15, %xmm1                          #1715.1699
        vrcpps    %xmm1, %xmm15                                 #1715.1787
        vmovq     1728+__svml_datan2_ha_data_internal(%rip), %xmm1 #1715.1904
        vpsrld    $3, %xmm15, %xmm14                            #1715.1861
        vpsubd    %xmm1, %xmm14, %xmm15                         #1715.1904
        vmovq     1472+__svml_datan2_ha_data_internal(%rip), %xmm14 #1715.1486
        vpand     %xmm14, %xmm0, %xmm14                         #1715.1486
        vmovq     1536+__svml_datan2_ha_data_internal(%rip), %xmm0 #1715.1542
        vpsubd    %xmm14, %xmm0, %xmm0                          #1715.1542
        vpaddd    %xmm0, %xmm15, %xmm15                         #1715.2010
        vpshufd   $80, %xmm15, %xmm1                            #1715.2010
        vmovupd   1792+__svml_datan2_ha_data_internal(%rip), %xmm0 #1716.21
        vandpd    3008+__svml_datan2_ha_data_internal(%rip), %xmm1, %xmm14 #1715.2113
        vmovapd   %xmm0, %xmm15                                 #1716.21
        vfnmadd231pd %xmm3, %xmm14, %xmm15                      #1716.21
        vmovupd   1856+__svml_datan2_ha_data_internal(%rip), %xmm1 #1738.14
        vfmadd213pd %xmm15, %xmm15, %xmm15                      #1717.21
        vfmadd213pd %xmm14, %xmm15, %xmm14                      #1718.18
        vfnmadd231pd %xmm3, %xmm14, %xmm0                       #1719.21
        vfmadd213pd %xmm14, %xmm0, %xmm14                       #1720.22
        vmulpd    %xmm14, %xmm2, %xmm15                         #1721.17
        vmulpd    %xmm15, %xmm15, %xmm0                         #1736.14
        vfnmadd213pd %xmm2, %xmm15, %xmm3                       #1722.21
        vmulpd    %xmm0, %xmm0, %xmm2                           #1737.14
        vmulpd    %xmm3, %xmm14, %xmm14                         #1723.19
        vfmadd213pd 1984+__svml_datan2_ha_data_internal(%rip), %xmm2, %xmm1 #1738.14
        vmovupd   1920+__svml_datan2_ha_data_internal(%rip), %xmm3 #1739.14
        vfmadd213pd 2048+__svml_datan2_ha_data_internal(%rip), %xmm2, %xmm3 #1739.14
        vaddpd    %xmm14, %xmm4, %xmm4                          #1751.19
        vfmadd213pd 2112+__svml_datan2_ha_data_internal(%rip), %xmm2, %xmm1 #1740.14
        vfmadd213pd 2176+__svml_datan2_ha_data_internal(%rip), %xmm2, %xmm3 #1741.14
        vaddpd    %xmm4, %xmm6, %xmm6                           #1752.19
        vfmadd213pd 2240+__svml_datan2_ha_data_internal(%rip), %xmm2, %xmm1 #1742.14
        vfmadd213pd 2304+__svml_datan2_ha_data_internal(%rip), %xmm2, %xmm3 #1743.14
        vfmadd213pd 2368+__svml_datan2_ha_data_internal(%rip), %xmm2, %xmm1 #1744.14
        vfmadd213pd 2432+__svml_datan2_ha_data_internal(%rip), %xmm2, %xmm3 #1745.14
        vfmadd213pd 2496+__svml_datan2_ha_data_internal(%rip), %xmm2, %xmm1 #1746.14
        vfmadd213pd 2560+__svml_datan2_ha_data_internal(%rip), %xmm2, %xmm3 #1747.14
        vfmadd213pd %xmm3, %xmm0, %xmm1                         #1748.14
        vmulpd    %xmm1, %xmm0, %xmm2                           #1749.14
        vfmadd213pd %xmm6, %xmm15, %xmm2                        #1753.14
        vaddpd    %xmm2, %xmm15, %xmm0                          #1754.18
        vaddpd    %xmm0, %xmm5, %xmm1                           #1755.18
        vxorpd    %xmm7, %xmm1, %xmm7                           #1756.18
        vaddpd    %xmm7, %xmm8, %xmm8                           #1757.18
        vorpd     %xmm9, %xmm8, %xmm6                           #1758.14
        testl     $3, %edi                                      #1759.41
        jne       ..B5.12       # Prob 5%                       #1759.52
                                # LOE rbx r12 r13 r14 r15 edx xmm6 xmm10 xmm11 xmm12 xmm13
..B5.2:                         # Preds ..B5.12 ..B5.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #1832.52
        jne       ..B5.4        # Prob 5%                       #1832.52
                                # LOE rbx r12 r13 r14 r15 edx xmm6 xmm13
..B5.3:                         # Preds ..B5.4 ..B5.10 ..B5.2
                                # Execution count [1.00e+00]
        movaps    %xmm6, %xmm0                                  #1835.12
        movq      %rbp, %rsp                                    #1835.12
        popq      %rbp                                          #1835.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1835.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.4:                         # Preds ..B5.2
                                # Execution count [5.00e-02]: Infreq
        vmovupd   (%rsp), %xmm1                                 #1832.241[spill]
        vmovupd   %xmm1, 64(%rsp)                               #1832.241
        vmovupd   %xmm13, 128(%rsp)                             #1832.314
        vmovupd   %xmm6, 192(%rsp)                              #1832.387
        je        ..B5.3        # Prob 95%                      #1832.491
                                # LOE rbx r12 r13 r14 r15 edx xmm6
..B5.7:                         # Preds ..B5.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1832.571
        movq      %r12, 8(%rsp)                                 #1832.571[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1832.571
        movq      %r13, (%rsp)                                  #1832.571[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1832.571
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.9 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1832.634
        jc        ..B5.11       # Prob 5%                       #1832.634
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.11 ..B5.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1832.587
        cmpl      $2, %r12d                                     #1832.582
        jl        ..B5.8        # Prob 82%                      #1832.582
                                # LOE rbx r12 r14 r15 r13d
..B5.10:                        # Preds ..B5.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm6                              #1832.805
        jmp       ..B5.3        # Prob 100%                     #1832.805
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm6
..B5.11:                        # Preds ..B5.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1832.663
        lea       128(%rsp,%r12,8), %rsi                        #1832.663
        lea       192(%rsp,%r12,8), %rdx                        #1832.663
..___tag_value___svml_atan22_ha_l9.140:
#       __svml_datan2_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_datan2_ha_cout_rare_internal           #1832.663
..___tag_value___svml_atan22_ha_l9.141:
        jmp       ..B5.9        # Prob 100%                     #1832.663
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B5.12:                        # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vcmpnltpd %xmm11, %xmm10, %xmm0                         #1797.21
        vcmpordpd %xmm13, %xmm13, %xmm9                         #1800.19
        vmovapd   %xmm11, %xmm2                                 #1799.21
        vmovupd   (%rsp), %xmm8                                 #1796.18[spill]
        blendvpd  %xmm0, %xmm10, %xmm2                          #1799.21
        vmovupd   2880+__svml_datan2_ha_data_internal(%rip), %xmm1 #1805.18
        vandpd    3136+__svml_datan2_ha_data_internal(%rip), %xmm0, %xmm3 #1798.24
        vcmpordpd %xmm8, %xmm8, %xmm14                          #1801.19
        vcmpeqpd  %xmm1, %xmm2, %xmm0                           #1813.17
        vcmpeqpd  %xmm1, %xmm11, %xmm11                         #1805.18
        vcmpeqpd  %xmm1, %xmm10, %xmm15                         #1806.18
        vmovupd   832+__svml_datan2_ha_data_internal(%rip), %xmm7 #1794.56
        vorpd     %xmm15, %xmm11, %xmm10                        #1807.20
        blendvpd  %xmm0, %xmm1, %xmm3                           #1815.16
        vandpd    %xmm7, %xmm13, %xmm5                          #1795.18
        vpshufd   $221, %xmm13, %xmm2                           #1820.18
        vandpd    %xmm7, %xmm8, %xmm4                           #1796.18
        vpshufd   $221, %xmm1, %xmm1                            #1818.18
        vandpd    %xmm14, %xmm9, %xmm7                          #1802.20
        vpshufd   $221, %xmm7, %xmm9                            #1804.22
        vorpd     %xmm5, %xmm3, %xmm7                           #1816.13
        vpcmpgtd  %xmm2, %xmm1, %xmm3                           #1821.22
        vpshufd   $221, %xmm10, %xmm8                           #1809.22
        vpshufd   $80, %xmm3, %xmm5                             #1822.26
        vpand     %xmm9, %xmm8, %xmm10                          #1810.28
        vandpd    3072+__svml_datan2_ha_data_internal(%rip), %xmm5, %xmm8 #1824.18
        vpandn    %xmm12, %xmm10, %xmm12                        #1811.21
        vaddpd    %xmm8, %xmm7, %xmm9                           #1825.17
        vmovmskps %xmm12, %edx                                  #1812.44
        vpshufd   $80, %xmm10, %xmm0                            #1827.32
        vorpd     %xmm4, %xmm9, %xmm4                           #1826.19
        blendvpd  %xmm0, %xmm4, %xmm6                           #1829.14
        andl      $3, %edx                                      #1812.96
        jmp       ..B5.2        # Prob 100%                     #1812.96
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm6 xmm13
	.cfi_endproc
# mark_end;
	.type	__svml_atan22_ha_l9,@function
	.size	__svml_atan22_ha_l9,.-__svml_atan22_ha_l9
..LN__svml_atan22_ha_l9.4:
	.data
# -- End  __svml_atan22_ha_l9
	.text
.L_2__routine_start___svml_atan22_ha_e9_5:
# -- Begin  __svml_atan22_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan22_ha_e9
# --- __svml_atan22_ha_e9(__m128d, __m128d)
__svml_atan22_ha_e9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1841.1
        .byte     15                                            #2147.96
        .byte     30                                            #2147.96
        .byte     250                                           #2147.96
	.cfi_startproc
..___tag_value___svml_atan22_ha_e9.146:
..L147:
                                                        #1841.1
        pushq     %rbp                                          #1841.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1841.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1841.1
        subq      $256, %rsp                                    #1841.1
        movaps    %xmm1, %xmm12                                 #1970.24
        movups    896+__svml_datan2_ha_data_internal(%rip), %xmm9 #1957.51
        lea       __svml_datan2_ha_data_internal(%rip), %rsi    #2022.393
        movaps    %xmm9, %xmm7                                  #1958.14
        andps     %xmm0, %xmm9                                  #1959.14
        andps     %xmm1, %xmm7                                  #1958.14
        movaps    %xmm9, %xmm14                                 #1961.17
        movaps    %xmm7, %xmm13                                 #1960.17
        movaps    %xmm9, %xmm4                                  #1964.19
        movups    %xmm1, 32(%rsp)                               #1841.1[spill]
        pxor      %xmm1, %xmm13                                 #1960.17
        pshufd    $221, %xmm7, %xmm2                            #1975.20
        movaps    %xmm7, %xmm3                                  #1966.19
        pshufd    $221, %xmm9, %xmm1                            #1976.20
        movdqa    %xmm2, %xmm8                                  #1979.21
        movups    1088+__svml_datan2_ha_data_internal(%rip), %xmm6 #1962.59
        movdqa    %xmm1, %xmm11                                 #1980.21
        movq      2688+__svml_datan2_ha_data_internal(%rip), %xmm10 #1977.30
        movaps    %xmm6, %xmm5                                  #1963.19
        psubd     %xmm10, %xmm8                                 #1979.21
        psubd     %xmm10, %xmm11                                #1980.21
        movdqa    %xmm1, %xmm10                                 #1984.21
        andps     %xmm9, %xmm5                                  #1963.19
        movups    %xmm9, 16(%rsp)                               #1959.14[spill]
        psubd     %xmm2, %xmm10                                 #1984.21
        movq      3200+__svml_datan2_ha_data_internal(%rip), %xmm9 #1987.21
        andps     %xmm7, %xmm6                                  #1965.19
        movq      2752+__svml_datan2_ha_data_internal(%rip), %xmm2 #1981.36
        paddd     %xmm9, %xmm10                                 #1987.21
        movdqa    %xmm8, %xmm9                                  #1981.36
        pcmpeqd   %xmm2, %xmm8                                  #1981.94
        pcmpgtd   %xmm2, %xmm9                                  #1981.36
        pxor      %xmm0, %xmm14                                 #1961.17
        por       %xmm8, %xmm9                                  #1981.22
        movdqa    %xmm11, %xmm8                                 #1982.36
        pcmpgtd   %xmm2, %xmm8                                  #1982.36
        pcmpeqd   %xmm2, %xmm11                                 #1982.94
        por       %xmm11, %xmm8                                 #1982.22
        xorl      %edx, %edx                                    #1990.5
        movq      3264+__svml_datan2_ha_data_internal(%rip), %xmm11 #1988.36
        por       %xmm8, %xmm9                                  #1983.23
        movdqa    %xmm11, %xmm8                                 #1988.36
        pcmpeqd   %xmm10, %xmm11                                #1988.86
        pcmpgtd   %xmm10, %xmm8                                 #1988.36
        por       %xmm11, %xmm8                                 #1988.22
        movups    1152+__svml_datan2_ha_data_internal(%rip), %xmm10 #1997.15
        por       %xmm8, %xmm9                                  #1989.23
        movups    1216+__svml_datan2_ha_data_internal(%rip), %xmm11 #1999.15
        movups    1280+__svml_datan2_ha_data_internal(%rip), %xmm8 #2000.15
        movups    1344+__svml_datan2_ha_data_internal(%rip), %xmm2 #1998.15
        mulpd     %xmm7, %xmm10                                 #1997.15
        mulpd     %xmm7, %xmm2                                  #1998.15
        mulpd     %xmm7, %xmm11                                 #1999.15
        mulpd     %xmm7, %xmm8                                  #2000.15
        subpd     %xmm6, %xmm3                                  #1966.19
        subpd     %xmm5, %xmm4                                  #1964.19
        cmpltpd   2880+__svml_datan2_ha_data_internal(%rip), %xmm12 #1970.24
        movups    %xmm7, 48(%rsp)                               #1958.14[spill]
        pshufd    $221, %xmm10, %xmm10                          #2005.25
        pshufd    $221, %xmm2, %xmm7                            #2006.25
        psubd     %xmm1, %xmm10                                 #2009.33
        pshufd    $221, %xmm11, %xmm2                           #2007.25
        psubd     %xmm1, %xmm7                                  #2010.33
        pshufd    $221, %xmm8, %xmm11                           #2008.25
        psubd     %xmm1, %xmm2                                  #2011.33
        psubd     %xmm1, %xmm11                                 #2012.33
        psrad     $31, %xmm10                                   #2013.32
        psrad     $31, %xmm7                                    #2014.32
        psrad     $31, %xmm2                                    #2015.32
        psrad     $31, %xmm11                                   #2016.32
        paddd     %xmm7, %xmm10                                 #2022.187
        paddd     %xmm11, %xmm2                                 #2022.187
        movaps    %xmm3, %xmm1                                  #2029.42
        movdqu    %xmm9, 64(%rsp)                               #1989.23[spill]
        paddd     %xmm2, %xmm10                                 #2022.187
        movq      1408+__svml_datan2_ha_data_internal(%rip), %xmm9 #2020.23
        movaps    %xmm4, %xmm8                                  #2027.20
        paddd     %xmm9, %xmm10                                 #2022.187
        pslld     $5, %xmm10                                    #2022.187
        movd      %xmm10, %eax                                  #2022.250
        movups    %xmm0, (%rsp)                                 #1841.1[spill]
        movaps    %xmm6, %xmm0                                  #2028.42
        movups    960+__svml_datan2_ha_data_internal(%rip), %xmm15 #1971.20
        pextrd    $1, %xmm10, %ecx                              #2022.318
        movaps    %xmm5, %xmm10                                 #2026.20
        movq      (%rax,%rsi), %xmm2                            #2022.631
        andps     %xmm12, %xmm15                                #1971.20
        movhpd    (%rcx,%rsi), %xmm2                            #2022.599
        mulpd     %xmm2, %xmm5                                  #2038.32
        mulpd     %xmm2, %xmm0                                  #2028.42
        mulpd     %xmm2, %xmm1                                  #2029.42
        mulpd     %xmm4, %xmm2                                  #2039.32
        movq      24(%rax,%rsi), %xmm7                          #2025.628
        movhpd    24(%rcx,%rsi), %xmm7                          #2025.596
        andps     %xmm7, %xmm6                                  #2036.20
        andps     %xmm7, %xmm10                                 #2026.20
        addpd     %xmm5, %xmm6                                  #2038.20
        subpd     %xmm0, %xmm10                                 #2028.20
        andps     %xmm7, %xmm8                                  #2027.20
        andps     %xmm3, %xmm7                                  #2037.20
        addpd     %xmm2, %xmm7                                  #2039.20
        subpd     %xmm1, %xmm8                                  #2029.20
        movaps    %xmm6, %xmm5                                  #2040.20
        movaps    %xmm10, %xmm1                                 #2030.20
        addpd     %xmm7, %xmm5                                  #2040.20
        subpd     %xmm8, %xmm1                                  #2030.20
        subpd     %xmm5, %xmm6                                  #2041.23
        movaps    %xmm1, %xmm0                                  #2031.21
        subpd     %xmm10, %xmm0                                 #2031.21
        addpd     %xmm6, %xmm7                                  #2042.20
        subpd     %xmm0, %xmm8                                  #2032.20
        movups    1088+__svml_datan2_ha_data_internal(%rip), %xmm3 #2043.19
        andps     %xmm5, %xmm3                                  #2043.19
        subpd     %xmm3, %xmm5                                  #2044.22
        movups    1088+__svml_datan2_ha_data_internal(%rip), %xmm10 #2033.19
        andps     %xmm1, %xmm10                                 #2033.19
        subpd     %xmm10, %xmm1                                 #2034.22
        addpd     %xmm5, %xmm7                                  #2045.19
        addpd     %xmm1, %xmm8                                  #2035.19
        pshufd    $221, %xmm3, %xmm5                            #2046.1450
        movdqa    %xmm5, %xmm0                                  #2046.1656
        movq      1600+__svml_datan2_ha_data_internal(%rip), %xmm2 #2046.1012
        pslld     $3, %xmm0                                     #2046.1656
        movq      1664+__svml_datan2_ha_data_internal(%rip), %xmm1 #2046.1117
        pand      %xmm2, %xmm0                                  #2046.1706
        por       %xmm1, %xmm0                                  #2046.1779
        rcpps     %xmm0, %xmm2                                  #2046.1879
        movq      1472+__svml_datan2_ha_data_internal(%rip), %xmm4 #2046.801
        psrld     $3, %xmm2                                     #2046.1961
        movq      1728+__svml_datan2_ha_data_internal(%rip), %xmm6 #2046.1213
        pand      %xmm4, %xmm5                                  #2046.1522
        movq      1536+__svml_datan2_ha_data_internal(%rip), %xmm4 #2046.1590
        psubd     %xmm6, %xmm2                                  #2046.2012
        psubd     %xmm5, %xmm4                                  #2046.1590
        paddd     %xmm4, %xmm2                                  #2046.2138
        pshufd    $80, %xmm2, %xmm5                             #2046.2138
        movaps    %xmm10, %xmm2                                 #2055.18
        andps     3008+__svml_datan2_ha_data_internal(%rip), %xmm5 #2046.2249
        mulpd     %xmm5, %xmm3                                  #2047.31
        mulpd     %xmm5, %xmm7                                  #2048.29
        subpd     1792+__svml_datan2_ha_data_internal(%rip), %xmm3 #2047.19
        mulpd     %xmm5, %xmm10                                 #2058.29
        addpd     %xmm7, %xmm3                                  #2048.17
        movaps    %xmm3, %xmm7                                  #2049.31
        mulpd     %xmm3, %xmm7                                  #2049.31
        subpd     %xmm3, %xmm7                                  #2049.19
        mulpd     %xmm3, %xmm7                                  #2050.31
        addpd     %xmm3, %xmm7                                  #2050.19
        mulpd     %xmm3, %xmm7                                  #2051.31
        subpd     %xmm3, %xmm7                                  #2051.19
        mulpd     %xmm3, %xmm7                                  #2052.31
        addpd     %xmm3, %xmm7                                  #2052.19
        mulpd     %xmm3, %xmm7                                  #2053.31
        subpd     %xmm3, %xmm7                                  #2053.19
        mulpd     %xmm5, %xmm7                                  #2054.21
        mulpd     %xmm7, %xmm2                                  #2055.18
        mulpd     %xmm8, %xmm7                                  #2056.30
        mulpd     %xmm5, %xmm8                                  #2057.31
        addpd     %xmm7, %xmm2                                  #2056.18
        addpd     %xmm8, %xmm2                                  #2057.19
        movaps    %xmm2, %xmm6                                  #2058.17
        addpd     %xmm10, %xmm6                                 #2058.17
        movaps    %xmm6, %xmm4                                  #2071.14
        mulpd     %xmm6, %xmm4                                  #2071.14
        movaps    %xmm4, %xmm7                                  #2072.14
        mulpd     %xmm4, %xmm7                                  #2072.14
        movups    1856+__svml_datan2_ha_data_internal(%rip), %xmm5 #2073.26
        mulpd     %xmm7, %xmm5                                  #2073.26
        movups    1920+__svml_datan2_ha_data_internal(%rip), %xmm3 #2074.26
        addpd     1984+__svml_datan2_ha_data_internal(%rip), %xmm5 #2073.14
        mulpd     %xmm7, %xmm3                                  #2074.26
        mulpd     %xmm7, %xmm5                                  #2075.26
        addpd     2048+__svml_datan2_ha_data_internal(%rip), %xmm3 #2074.14
        addpd     2112+__svml_datan2_ha_data_internal(%rip), %xmm5 #2075.14
        mulpd     %xmm7, %xmm3                                  #2076.26
        mulpd     %xmm7, %xmm5                                  #2077.26
        addpd     2176+__svml_datan2_ha_data_internal(%rip), %xmm3 #2076.14
        addpd     2240+__svml_datan2_ha_data_internal(%rip), %xmm5 #2077.14
        mulpd     %xmm7, %xmm3                                  #2078.26
        mulpd     %xmm7, %xmm5                                  #2079.26
        addpd     2304+__svml_datan2_ha_data_internal(%rip), %xmm3 #2078.14
        addpd     2368+__svml_datan2_ha_data_internal(%rip), %xmm5 #2079.14
        mulpd     %xmm7, %xmm3                                  #2080.26
        mulpd     %xmm7, %xmm5                                  #2081.26
        addpd     2432+__svml_datan2_ha_data_internal(%rip), %xmm3 #2080.14
        addpd     2496+__svml_datan2_ha_data_internal(%rip), %xmm5 #2081.14
        mulpd     %xmm3, %xmm7                                  #2082.26
        mulpd     %xmm4, %xmm5                                  #2083.26
        addpd     2560+__svml_datan2_ha_data_internal(%rip), %xmm7 #2082.14
        addpd     %xmm5, %xmm7                                  #2083.14
        mulpd     %xmm7, %xmm4                                  #2084.14
        movq      16(%rax,%rsi), %xmm9                          #2024.637
        movhpd    16(%rcx,%rsi), %xmm9                          #2024.605
        addpd     %xmm2, %xmm9                                  #2086.19
        mulpd     %xmm4, %xmm6                                  #2088.26
        andps     1024+__svml_datan2_ha_data_internal(%rip), %xmm12 #1972.20
        pxor      %xmm13, %xmm12                                #2085.16
        addpd     %xmm9, %xmm12                                 #2087.19
        addpd     %xmm6, %xmm12                                 #2088.14
        addpd     %xmm12, %xmm10                                #2089.18
        movq      8(%rax,%rsi), %xmm11                          #2023.637
        movhpd    8(%rcx,%rsi), %xmm11                          #2023.605
        addpd     %xmm10, %xmm11                                #2090.18
        pxor      %xmm13, %xmm11                                #2091.18
        addpd     %xmm11, %xmm15                                #2092.18
        movdqu    64(%rsp), %xmm4                               #1991.58[spill]
        orps      %xmm14, %xmm15                                #2093.14
        movmskps  %xmm4, %edi                                   #1991.58
        movups    32(%rsp), %xmm6                               #2094.52[spill]
        movups    48(%rsp), %xmm9                               #2094.52[spill]
        testl     $3, %edi                                      #2094.41
        jne       ..B6.12       # Prob 5%                       #2094.52
                                # LOE rbx r12 r13 r14 r15 edx xmm4 xmm6 xmm9 xmm15
..B6.2:                         # Preds ..B6.12 ..B6.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #2167.52
        jne       ..B6.4        # Prob 5%                       #2167.52
                                # LOE rbx r12 r13 r14 r15 edx xmm6 xmm15
..B6.3:                         # Preds ..B6.4 ..B6.10 ..B6.2
                                # Execution count [1.00e+00]
        movaps    %xmm15, %xmm0                                 #2170.12
        movq      %rbp, %rsp                                    #2170.12
        popq      %rbp                                          #2170.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2170.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.4:                         # Preds ..B6.2
                                # Execution count [5.00e-02]: Infreq
        movups    (%rsp), %xmm1                                 #2167.241[spill]
        movups    %xmm1, 64(%rsp)                               #2167.241
        movups    %xmm6, 128(%rsp)                              #2167.314
        movups    %xmm15, 192(%rsp)                             #2167.387
        je        ..B6.3        # Prob 95%                      #2167.491
                                # LOE rbx r12 r13 r14 r15 edx xmm15
..B6.7:                         # Preds ..B6.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #2167.571
        movq      %r12, 8(%rsp)                                 #2167.571[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #2167.571
        movq      %r13, (%rsp)                                  #2167.571[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #2167.571
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.9 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #2167.634
        jc        ..B6.11       # Prob 5%                       #2167.634
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.11 ..B6.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #2167.587
        cmpl      $2, %r12d                                     #2167.582
        jl        ..B6.8        # Prob 82%                      #2167.582
                                # LOE rbx r12 r14 r15 r13d
..B6.10:                        # Preds ..B6.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm15                             #2167.805
        jmp       ..B6.3        # Prob 100%                     #2167.805
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm15
..B6.11:                        # Preds ..B6.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #2167.663
        lea       128(%rsp,%r12,8), %rsi                        #2167.663
        lea       192(%rsp,%r12,8), %rdx                        #2167.663
..___tag_value___svml_atan22_ha_e9.173:
#       __svml_datan2_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_datan2_ha_cout_rare_internal           #2167.663
..___tag_value___svml_atan22_ha_e9.174:
        jmp       ..B6.9        # Prob 100%                     #2167.663
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B6.12:                        # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    16(%rsp), %xmm8                               #2132.21[spill]
        movaps    %xmm9, %xmm1                                  #2134.21
        movaps    %xmm8, %xmm0                                  #2132.21
        movaps    %xmm6, %xmm10                                 #2135.19
        cmpnltpd  %xmm9, %xmm0                                  #2132.21
        cmpordpd  %xmm6, %xmm10                                 #2135.19
        movups    (%rsp), %xmm7                                 #2131.18[spill]
        movups    832+__svml_datan2_ha_data_internal(%rip), %xmm2 #2129.56
        movups    2880+__svml_datan2_ha_data_internal(%rip), %xmm12 #2140.18
        movaps    %xmm2, %xmm5                                  #2130.18
        blendvpd  %xmm0, %xmm8, %xmm1                           #2134.21
        andps     %xmm7, %xmm2                                  #2131.18
        cmpordpd  %xmm7, %xmm7                                  #2136.19
        cmpeqpd   %xmm12, %xmm9                                 #2140.18
        cmpeqpd   %xmm12, %xmm8                                 #2141.18
        cmpeqpd   %xmm12, %xmm1                                 #2148.17
        andps     %xmm7, %xmm10                                 #2137.20
        orps      %xmm8, %xmm9                                  #2142.20
        pshufd    $221, %xmm9, %xmm7                            #2144.26
        andps     %xmm6, %xmm5                                  #2130.18
        pshufd    $221, %xmm10, %xmm9                           #2139.26
        pand      %xmm9, %xmm7                                  #2145.32
        movdqa    %xmm7, %xmm11                                 #2146.25
        movups    3136+__svml_datan2_ha_data_internal(%rip), %xmm3 #2133.24
        pandn     %xmm4, %xmm11                                 #2146.25
        pshufd    $221, %xmm6, %xmm4                            #2155.22
        andps     %xmm0, %xmm3                                  #2133.24
        pshufd    $221, %xmm12, %xmm13                          #2153.22
        movaps    %xmm1, %xmm0                                  #2150.16
        pcmpgtd   %xmm4, %xmm13                                 #2156.26
        blendvpd  %xmm0, %xmm12, %xmm3                          #2150.16
        pshufd    $80, %xmm13, %xmm14                           #2157.26
        orps      %xmm5, %xmm3                                  #2151.13
        andps     3072+__svml_datan2_ha_data_internal(%rip), %xmm14 #2159.18
        addpd     %xmm14, %xmm3                                 #2160.17
        movmskps  %xmm11, %edx                                  #2147.44
        pshufd    $80, %xmm7, %xmm0                             #2162.32
        orps      %xmm2, %xmm3                                  #2161.19
        blendvpd  %xmm0, %xmm3, %xmm15                          #2164.14
        andl      $3, %edx                                      #2147.96
        jmp       ..B6.2        # Prob 100%                     #2147.96
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm6 xmm15
	.cfi_endproc
# mark_end;
	.type	__svml_atan22_ha_e9,@function
	.size	__svml_atan22_ha_e9,.-__svml_atan22_ha_e9
..LN__svml_atan22_ha_e9.5:
	.data
# -- End  __svml_atan22_ha_e9
	.text
.L_2__routine_start___svml_atan28_ha_z0_6:
# -- Begin  __svml_atan28_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan28_ha_z0
# --- __svml_atan28_ha_z0(__m512d, __m512d)
__svml_atan28_ha_z0:
# parameter 1: %zmm0
# parameter 2: %zmm1
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2176.1
        .byte     15                                            #2458.35
        .byte     30                                            #2458.35
        .byte     250                                           #2458.35
	.cfi_startproc
..___tag_value___svml_atan28_ha_z0.180:
..L181:
                                                        #2176.1
        pushq     %rbp                                          #2176.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2176.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2176.1
        subq      $256, %rsp                                    #2176.1
        xorl      %esi, %esi                                    #2308.5
        vmovups   896+__svml_datan2_ha_data_internal(%rip), %zmm8 #2288.52
        vmovups   2880+__svml_datan2_ha_data_internal(%rip), %zmm10 #2293.48
        vmovdqu   2688+__svml_datan2_ha_data_internal(%rip), %ymm2 #2301.63
        vmovaps   %zmm1, %zmm11                                 #2176.1
        vandpd    %zmm8, %zmm11, %zmm9                          #2289.14
        vandpd    %zmm8, %zmm0, %zmm8                           #2290.14
        vcmppd    $17, {sae}, %zmm10, %zmm11, %k5               #2296.24
        vmovdqu   2752+__svml_datan2_ha_data_internal(%rip), %ymm10 #2302.63
        vmovups   1152+__svml_datan2_ha_data_internal(%rip), %zmm1 #2310.55
        vpsrlq    $32, %zmm9, %zmm5                             #2299.36
        vpsrlq    $32, %zmm8, %zmm13                            #2300.36
        vxorpd    %zmm9, %zmm11, %zmm6                          #2291.17
        vpmovqd   %zmm5, %ymm4                                  #2299.14
        vpmovqd   %zmm13, %ymm3                                 #2300.14
        vxorpd    %zmm8, %zmm0, %zmm7                           #2292.17
        vpsubd    %ymm2, %ymm4, %ymm15                          #2303.15
        vpsubd    %ymm2, %ymm3, %ymm5                           #2304.15
        vmovups   1792+__svml_datan2_ha_data_internal(%rip), %zmm2 #2314.47
        vpcmpgtd  %ymm10, %ymm15, %ymm12                        #2305.35
        vpcmpeqd  %ymm10, %ymm15, %ymm14                        #2305.88
        vpcmpgtd  %ymm10, %ymm5, %ymm4                          #2306.35
        vpcmpeqd  %ymm10, %ymm5, %ymm10                         #2306.88
        vpor      %ymm14, %ymm12, %ymm13                        #2305.18
        vpor      %ymm10, %ymm4, %ymm3                          #2306.18
        vmovups   1344+__svml_datan2_ha_data_internal(%rip), %zmm15 #2311.55
        vmovups   1280+__svml_datan2_ha_data_internal(%rip), %zmm14 #2313.55
        vmovups   1216+__svml_datan2_ha_data_internal(%rip), %zmm12 #2312.55
        vmulpd    {rn-sae}, %zmm1, %zmm9, %zmm4                 #2315.17
        vmulpd    {rn-sae}, %zmm15, %zmm9, %zmm5                #2316.17
        vmulpd    {rn-sae}, %zmm12, %zmm9, %zmm15               #2317.17
        vmovups   768+__svml_datan2_ha_data_internal(%rip), %zmm1 #2328.57
        vmovups   192+__svml_datan2_ha_data_internal(%rip), %zmm12 #2319.57
        vcmppd    $17, {sae}, %zmm8, %zmm5, %k1                 #2330.25
        vcmppd    $17, {sae}, %zmm8, %zmm4, %k4                 #2329.25
        vcmppd    $17, {sae}, %zmm8, %zmm15, %k2                #2331.25
        vmovups   512+__svml_datan2_ha_data_internal(%rip), %zmm15 #2337.25
        vpor      %ymm3, %ymm13, %ymm10                         #2307.17
        vblendmpd 320+__svml_datan2_ha_data_internal(%rip), %zmm12, %zmm5{%k1} #2333.25
        vmulpd    {rn-sae}, %zmm14, %zmm9, %zmm3                #2318.17
        vmovups   704+__svml_datan2_ha_data_internal(%rip), %zmm13 #2327.52
        vmovups   256+__svml_datan2_ha_data_internal(%rip), %zmm14 #2320.57
        vcmppd    $17, {sae}, %zmm8, %zmm3, %k3                 #2332.25
        vblendmpd %zmm2, %zmm13, %zmm3{%k1}                     #2335.21
        vblendmpd 384+__svml_datan2_ha_data_internal(%rip), %zmm14, %zmm4{%k1} #2334.25
        vmovups   448+__svml_datan2_ha_data_internal(%rip), %zmm13 #2336.25
        vblendmpd %zmm2, %zmm1, %zmm1{%k3}                      #2338.21
        vblendmpd 576+__svml_datan2_ha_data_internal(%rip), %zmm13, %zmm14{%k3} #2336.25
        vblendmpd %zmm1, %zmm3, %zmm3{%k2}                      #2341.20
        vblendmpd 640+__svml_datan2_ha_data_internal(%rip), %zmm15, %zmm13{%k3} #2337.25
        vmovups   1984+__svml_datan2_ha_data_internal(%rip), %zmm1 #2370.14
        vblendmpd %zmm14, %zmm5, %zmm5{%k2}                     #2339.26
        vblendmpd %zmm13, %zmm4, %zmm4{%k2}                     #2340.26
        vmovaps   %zmm9, %zmm12                                 #2346.23
        vxorpd    %zmm9, %zmm9, %zmm12{%k3}                     #2346.23
        vfmadd231pd {rn-sae}, %zmm8, %zmm3, %zmm12{%k4}         #2347.23
        vrcp14pd  %zmm12, %zmm14                                #2348.16
        vmovaps   %zmm2, %zmm15                                 #2349.21
        vfnmadd231pd {rn-sae}, %zmm12, %zmm14, %zmm15           #2349.21
        vfmadd213pd {rn-sae}, %zmm14, %zmm15, %zmm14            #2350.18
        vfnmadd231pd {rn-sae}, %zmm12, %zmm14, %zmm2            #2351.21
        vmovaps   %zmm8, %zmm13                                 #2343.23
        vxorpd    %zmm8, %zmm8, %zmm13{%k3}                     #2343.23
        vfmadd213pd {rn-sae}, %zmm14, %zmm2, %zmm14             #2352.22
        vfnmadd231pd {rn-sae}, %zmm9, %zmm3, %zmm13{%k4}        #2344.23
        vmulpd    {rn-sae}, %zmm13, %zmm14, %zmm3               #2353.19
        vmulpd    {rn-sae}, %zmm3, %zmm3, %zmm15                #2368.16
        vfnmadd213pd {rn-sae}, %zmm13, %zmm3, %zmm12            #2354.21
        vmulpd    {rn-sae}, %zmm15, %zmm15, %zmm2               #2369.16
        vmulpd    {rn-sae}, %zmm14, %zmm12, %zmm13              #2355.21
        vmovups   1856+__svml_datan2_ha_data_internal(%rip), %zmm12 #2356.52
        vmovups   1920+__svml_datan2_ha_data_internal(%rip), %zmm14 #2357.52
        vaddpd    {rn-sae}, %zmm4, %zmm13, %zmm13{%k4}          #2383.21
        vfmadd231pd {rn-sae}, %zmm2, %zmm12, %zmm1              #2370.14
        vmovups   2048+__svml_datan2_ha_data_internal(%rip), %zmm12 #2371.14
        vfmadd231pd {rn-sae}, %zmm2, %zmm14, %zmm12             #2371.14
        vmovups   2112+__svml_datan2_ha_data_internal(%rip), %zmm14 #2372.14
        vfmadd213pd {rn-sae}, %zmm14, %zmm2, %zmm1              #2372.14
        vmovups   2176+__svml_datan2_ha_data_internal(%rip), %zmm14 #2373.14
        vfmadd213pd {rn-sae}, %zmm14, %zmm2, %zmm12             #2373.14
        vmovups   2240+__svml_datan2_ha_data_internal(%rip), %zmm14 #2374.14
        vfmadd213pd {rn-sae}, %zmm14, %zmm2, %zmm1              #2374.14
        vmovups   2304+__svml_datan2_ha_data_internal(%rip), %zmm14 #2375.14
        vfmadd213pd {rn-sae}, %zmm14, %zmm2, %zmm12             #2375.14
        vmovups   2368+__svml_datan2_ha_data_internal(%rip), %zmm14 #2376.14
        vfmadd213pd {rn-sae}, %zmm14, %zmm2, %zmm1              #2376.14
        vmovups   2432+__svml_datan2_ha_data_internal(%rip), %zmm14 #2377.14
        vfmadd213pd {rn-sae}, %zmm14, %zmm2, %zmm12             #2377.14
        vmovups   2496+__svml_datan2_ha_data_internal(%rip), %zmm14 #2378.14
        vfmadd213pd {rn-sae}, %zmm14, %zmm2, %zmm1              #2378.14
        vmovups   2560+__svml_datan2_ha_data_internal(%rip), %zmm14 #2379.14
        vfmadd213pd {rn-sae}, %zmm14, %zmm2, %zmm12             #2379.14
        vfmadd213pd {rn-sae}, %zmm12, %zmm15, %zmm1             #2380.14
        vmulpd    {rn-sae}, %zmm15, %zmm1, %zmm15               #2381.16
        vxorpd    1024+__svml_datan2_ha_data_internal(%rip), %zmm6, %zmm1 #2382.16
        vaddpd    {rn-sae}, %zmm1, %zmm13, %zmm13{%k5}          #2384.21
        vfmadd213pd {rn-sae}, %zmm13, %zmm3, %zmm15             #2385.14
        vaddpd    {rn-sae}, %zmm15, %zmm3, %zmm3                #2386.20
        vaddpd    {rn-sae}, %zmm5, %zmm3, %zmm3{%k4}            #2387.20
        vxorpd    %zmm6, %zmm3, %zmm1                           #2388.18
        vmovups   960+__svml_datan2_ha_data_internal(%rip), %zmm6 #2389.20
        vaddpd    {rn-sae}, %zmm6, %zmm1, %zmm1{%k5}            #2389.20
        vmovmskps %ymm10, %eax                                  #2309.58
        vorpd     %zmm7, %zmm1, %zmm7                           #2390.14
        testl     %eax, %eax                                    #2391.52
        jne       ..B7.17       # Prob 5%                       #2391.52
                                # LOE rbx r12 r13 r14 r15 esi ymm10 zmm0 zmm7 zmm8 zmm9 zmm11
..B7.2:                         # Preds ..B7.17 ..B7.1
                                # Execution count [1.00e+00]
        testl     %esi, %esi                                    #2461.52
        jne       ..B7.4        # Prob 5%                       #2461.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm7 zmm11
..B7.3:                         # Preds ..B7.10 ..B7.8 ..B7.2
                                # Execution count [1.00e+00]
        vmovaps   %zmm7, %zmm0                                  #2464.12
        movq      %rbp, %rsp                                    #2464.12
        popq      %rbp                                          #2464.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2464.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.4:                         # Preds ..B7.2
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #2461.293
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm7 zmm11
..B7.5:                         # Preds ..B7.4
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #2461.293
        movl      %edx, %eax                                    #2461.347
        orl       $8064, %eax                                   #2461.347
        cmpl      %eax, %edx                                    #2461.376
        je        ..B7.7        # Prob 78%                      #2461.376
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm7 zmm11
..B7.6:                         # Preds ..B7.5
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #2461.388
        vldmxcsr  32(%rsp)                                      #2461.388
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm7 zmm11
..B7.7:                         # Preds ..B7.6 ..B7.5
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm0, 64(%rsp)                               #2461.450
        vmovups   %zmm11, 128(%rsp)                             #2461.525
        vmovups   %zmm7, 192(%rsp)                              #2461.600
        testl     %esi, %esi                                    #2461.706
        jne       ..B7.12       # Prob 5%                       #2461.706
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm7
..B7.8:                         # Preds ..B7.15 ..B7.7
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #2461.1064
        je        ..B7.3        # Prob 78%                      #2461.1064
                                # LOE rbx r12 r13 r14 r15 edx zmm7
..B7.9:                         # Preds ..B7.8
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #2461.1089
        movl      32(%rsp), %eax                                #2461.1089
                                # LOE rbx r12 r13 r14 r15 eax edx zmm7
..B7.10:                        # Preds ..B7.9
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #2461.1076
        orl       %edx, %eax                                    #2461.1076
        movl      %eax, 32(%rsp)                                #2461.1076
        vldmxcsr  32(%rsp)                                      #2461.1076
        jmp       ..B7.3        # Prob 100%                     #2461.1076
                                # LOE rbx r12 r13 r14 r15 zmm7
..B7.12:                        # Preds ..B7.7
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #2461.786
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B7.20:                        # Preds ..B7.12
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
..B7.13:                        # Preds ..B7.14 ..B7.20
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #2461.849
        jc        ..B7.16       # Prob 5%                       #2461.849
                                # LOE rbx r15 r12d r13d r14d
..B7.14:                        # Preds ..B7.16 ..B7.13
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #2461.802
        cmpl      $8, %ebx                                      #2461.797
        jl        ..B7.13       # Prob 82%                      #2461.797
                                # LOE rbx r15 r12d r13d r14d
..B7.15:                        # Preds ..B7.14
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
        vmovups   192(%rsp), %zmm7                              #2461.1020
        jmp       ..B7.8        # Prob 100%                     #2461.1020
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x18, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x10, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm7
..B7.16:                        # Preds ..B7.13
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,8), %rdi                         #2461.878
        lea       128(%rsp,%rbx,8), %rsi                        #2461.878
        lea       192(%rsp,%rbx,8), %rdx                        #2461.878
..___tag_value___svml_atan28_ha_z0.208:
#       __svml_datan2_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_datan2_ha_cout_rare_internal           #2461.878
..___tag_value___svml_atan28_ha_z0.209:
        jmp       ..B7.14       # Prob 100%                     #2461.878
	.cfi_restore 3
	.cfi_restore 12
	.cfi_restore 13
	.cfi_restore 14
                                # LOE r15 ebx r12d r13d r14d
..B7.17:                        # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        vcmppd    $3, {sae}, %zmm11, %zmm11, %k2                #2432.116
        vcmppd    $3, {sae}, %zmm0, %zmm0, %k3                  #2433.116
        vcmppd    $17, {sae}, %zmm9, %zmm8, %k1                 #2429.21
        vmovups   832+__svml_datan2_ha_data_internal(%rip), %zmm14 #2426.57
        vmovups   3136+__svml_datan2_ha_data_internal(%rip), %zmm4 #2424.52
        vmovups   3072+__svml_datan2_ha_data_internal(%rip), %zmm12 #2425.50
        vblendmpd %zmm9, %zmm8, %zmm3{%k1}                      #2431.21
        vpbroadcastq .L_2il0floatpacket.51(%rip), %zmm5         #2432.65
        vandpd    %zmm14, %zmm11, %zmm13                        #2427.18
        vandpd    %zmm14, %zmm0, %zmm6                          #2428.18
        vxorpd    %zmm4, %zmm4, %zmm4{%k1}                      #2430.24
        vmovaps   %zmm5, %zmm15                                 #2432.40
        vmovaps   %zmm5, %zmm2                                  #2433.40
        vmovaps   %zmm5, %zmm14                                 #2437.39
        vpandnq   %zmm11, %zmm11, %zmm15{%k2}                   #2432.40
        vpandnq   %zmm0, %zmm0, %zmm2{%k3}                      #2433.40
        vandpd    %zmm2, %zmm15, %zmm1                          #2434.20
        vmovups   2880+__svml_datan2_ha_data_internal(%rip), %zmm2 #2437.115
        vpsrlq    $32, %zmm1, %zmm1                             #2436.42
        vcmppd    $4, {sae}, %zmm2, %zmm9, %k4                  #2437.115
        vcmppd    $4, {sae}, %zmm2, %zmm8, %k5                  #2438.115
        vcmppd    $4, {sae}, %zmm2, %zmm3, %k6                  #2445.114
        vpcmpgtq  %zmm11, %zmm2, %k7                            #2453.20
        vpmovqd   %zmm1, %ymm1                                  #2436.20
        vpandnq   %zmm9, %zmm9, %zmm14{%k4}                     #2437.39
        vmovaps   %zmm5, %zmm9                                  #2438.39
        vpandnq   %zmm8, %zmm8, %zmm9{%k5}                      #2438.39
        vorpd     %zmm9, %zmm14, %zmm8                          #2439.20
        vpsrlq    $32, %zmm8, %zmm9                             #2441.42
        vpmovqd   %zmm9, %ymm14                                 #2441.20
        vpand     %ymm1, %ymm14, %ymm8                          #2442.26
        vpandn    %ymm10, %ymm8, %ymm10                         #2443.19
        vpmovzxdq %ymm8, %zmm8                                  #2456.22
        vmovmskps %ymm10, %esi                                  #2444.44
        vpandnq   %zmm3, %zmm3, %zmm5{%k6}                      #2445.38
        vpandnq   %zmm4, %zmm5, %zmm1                           #2447.54
        vpandq    %zmm5, %zmm2, %zmm3                           #2447.144
        vporq     %zmm3, %zmm1, %zmm4                           #2447.37
        vorpd     %zmm13, %zmm4, %zmm9                          #2448.13
        vpsllq    $32, %zmm8, %zmm2                             #2456.80
        vaddpd    {rn-sae}, %zmm12, %zmm9, %zmm9{%k7}           #2454.19
        vorpd     %zmm6, %zmm9, %zmm6                           #2455.19
        vpord     %zmm2, %zmm8, %zmm10                          #2456.135
        vpandnq   %zmm7, %zmm10, %zmm7                          #2458.52
        vpandq    %zmm10, %zmm6, %zmm1                          #2458.147
        vporq     %zmm1, %zmm7, %zmm7                           #2458.35
        jmp       ..B7.2        # Prob 100%                     #2458.35
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm7 zmm11
	.cfi_endproc
# mark_end;
	.type	__svml_atan28_ha_z0,@function
	.size	__svml_atan28_ha_z0,.-__svml_atan28_ha_z0
..LN__svml_atan28_ha_z0.6:
	.data
# -- End  __svml_atan28_ha_z0
	.text
.L_2__routine_start___svml_atan21_ha_e9_7:
# -- Begin  __svml_atan21_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan21_ha_e9
# --- __svml_atan21_ha_e9(__m128d, __m128d)
__svml_atan21_ha_e9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2997.1
        .byte     15                                            #3303.96
        .byte     30                                            #3303.96
        .byte     250                                           #3303.96
	.cfi_startproc
..___tag_value___svml_atan21_ha_e9.215:
..L216:
                                                        #2997.1
        pushq     %rbp                                          #2997.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2997.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2997.1
        subq      $192, %rsp                                    #2997.1
        movaps    %xmm1, %xmm8                                  #2997.1
        movsd     896+__svml_datan2_ha_data_internal(%rip), %xmm6 #3113.20
        movl      $-2144337920, %eax                            #3133.30
        movaps    %xmm6, %xmm12                                 #3114.14
        andps     %xmm0, %xmm6                                  #3115.14
        andps     %xmm8, %xmm12                                 #3114.14
        movl      $1005584384, %ecx                             #3143.21
        movaps    %xmm12, %xmm9                                 #3116.17
        movaps    %xmm6, %xmm11                                 #3117.17
        movups    %xmm8, (%rsp)                                 #2997.1[spill]
        pxor      %xmm8, %xmm9                                  #3116.17
        movsd     1088+__svml_datan2_ha_data_internal(%rip), %xmm4 #3118.28
        movl      $-36700160, %edx                              #3134.30
        movsd     1024+__svml_datan2_ha_data_internal(%rip), %xmm13 #3125.16
        pxor      %xmm0, %xmm11                                 #3117.17
        movsd     960+__svml_datan2_ha_data_internal(%rip), %xmm10 #3127.20
        movaps    %xmm4, %xmm3                                  #3119.19
        movups    %xmm0, 16(%rsp)                               #2997.1[spill]
        andps     %xmm6, %xmm3                                  #3119.19
        pshufd    $85, %xmm6, %xmm0                             #3132.20
        movaps    %xmm6, %xmm2                                  #3120.19
        movups    %xmm6, 32(%rsp)                               #3115.14[spill]
        movd      %eax, %xmm14                                  #3133.30
        movdqa    %xmm0, %xmm6                                  #3140.21
        movd      %edx, %xmm15                                  #3134.30
        movdqa    %xmm0, %xmm7                                  #3136.21
        movl      $4, %esi                                      #3178.187
        psubd     %xmm14, %xmm7                                 #3136.21
        lea       __svml_datan2_ha_data_internal(%rip), %rdx    #3178.433
        andps     %xmm12, %xmm4                                 #3121.19
        movaps    %xmm12, %xmm1                                 #3122.19
        movl      $8388607, %r8d                                #3202.1701
        movl      $1065353216, %r9d                             #3202.1774
        movl      $-1048576, %r11d                              #3202.1517
        movl      $133169152, %r10d                             #3202.2007
        xorl      %eax, %eax                                    #3146.5
        cmpltsd   2880+__svml_datan2_ha_data_internal(%rip), %xmm8 #3126.24
        subsd     %xmm4, %xmm1                                  #3122.19
        subsd     %xmm3, %xmm2                                  #3120.19
        andps     %xmm8, %xmm10                                 #3127.20
        andps     %xmm13, %xmm8                                 #3128.20
        pshufd    $85, %xmm12, %xmm13                           #3131.20
        pxor      %xmm9, %xmm8                                  #3241.16
        movdqa    %xmm13, %xmm5                                 #3135.21
        psubd     %xmm13, %xmm6                                 #3140.21
        psubd     %xmm14, %xmm5                                 #3135.21
        movd      %ecx, %xmm13                                  #3143.21
        paddd     %xmm13, %xmm6                                 #3143.21
        movdqa    %xmm5, %xmm13                                 #3137.36
        pcmpgtd   %xmm15, %xmm13                                #3137.36
        pcmpeqd   %xmm15, %xmm5                                 #3137.94
        por       %xmm5, %xmm13                                 #3137.22
        movdqa    %xmm7, %xmm5                                  #3138.36
        pcmpgtd   %xmm15, %xmm5                                 #3138.36
        pcmpeqd   %xmm15, %xmm7                                 #3138.94
        por       %xmm7, %xmm5                                  #3138.22
        movaps    %xmm12, %xmm14                                #3156.15
        pxor      %xmm15, %xmm15                                #3144.36
        pxor      %xmm7, %xmm7                                  #3144.86
        pcmpgtd   %xmm6, %xmm15                                 #3144.36
        pcmpeqd   %xmm6, %xmm7                                  #3144.86
        por       %xmm5, %xmm13                                 #3139.23
        por       %xmm7, %xmm15                                 #3144.22
        por       %xmm15, %xmm13                                #3145.23
        movaps    %xmm12, %xmm6                                 #3153.15
        movaps    %xmm12, %xmm15                                #3154.15
        movaps    %xmm12, %xmm5                                 #3155.15
        mulsd     1152+__svml_datan2_ha_data_internal(%rip), %xmm6 #3153.15
        mulsd     1216+__svml_datan2_ha_data_internal(%rip), %xmm5 #3155.15
        mulsd     1280+__svml_datan2_ha_data_internal(%rip), %xmm14 #3156.15
        mulsd     1344+__svml_datan2_ha_data_internal(%rip), %xmm15 #3154.15
        movmskps  %xmm13, %ecx                                  #3147.58
        pshufd    $85, %xmm6, %xmm7                             #3161.25
        pshufd    $85, %xmm15, %xmm6                            #3162.25
        psubd     %xmm0, %xmm7                                  #3165.33
        pshufd    $85, %xmm5, %xmm15                            #3163.25
        psubd     %xmm0, %xmm6                                  #3166.33
        pshufd    $85, %xmm14, %xmm5                            #3164.25
        psubd     %xmm0, %xmm15                                 #3167.33
        psubd     %xmm0, %xmm5                                  #3168.33
        psrad     $31, %xmm7                                    #3169.32
        psrad     $31, %xmm6                                    #3170.32
        psrad     $31, %xmm15                                   #3171.32
        psrad     $31, %xmm5                                    #3172.32
        paddd     %xmm6, %xmm7                                  #3178.187
        paddd     %xmm5, %xmm15                                 #3178.187
        movd      %esi, %xmm14                                  #3178.187
        paddd     %xmm15, %xmm7                                 #3178.187
        paddd     %xmm14, %xmm7                                 #3178.187
        pslld     $5, %xmm7                                     #3178.187
        movd      %xmm7, %edi                                   #3178.250
        movq      (%rdi,%rdx), %xmm7                            #3178.433
        movdqa    %xmm7, %xmm6                                  #3184.42
        movdqa    %xmm7, %xmm15                                 #3185.42
        mulsd     %xmm4, %xmm6                                  #3184.42
        mulsd     %xmm1, %xmm15                                 #3185.42
        movq      24(%rdx,%rdi), %xmm14                         #3181.430
        movdqa    %xmm14, %xmm5                                 #3182.20
        movdqa    %xmm14, %xmm0                                 #3183.20
        andps     %xmm3, %xmm5                                  #3182.20
        andps     %xmm14, %xmm4                                 #3192.20
        andps     %xmm1, %xmm14                                 #3193.20
        movdqa    %xmm7, %xmm1                                  #3194.32
        mulsd     %xmm3, %xmm1                                  #3194.32
        subsd     %xmm6, %xmm5                                  #3184.20
        mulsd     %xmm2, %xmm7                                  #3195.32
        addsd     %xmm4, %xmm1                                  #3194.20
        addsd     %xmm14, %xmm7                                 #3195.20
        andps     %xmm2, %xmm0                                  #3183.20
        movaps    %xmm5, %xmm6                                  #3186.20
        movaps    %xmm1, %xmm3                                  #3196.20
        subsd     %xmm15, %xmm0                                 #3185.20
        addsd     %xmm7, %xmm3                                  #3196.20
        subsd     %xmm0, %xmm6                                  #3186.20
        subsd     %xmm3, %xmm1                                  #3197.23
        movaps    %xmm6, %xmm15                                 #3187.21
        addsd     %xmm1, %xmm7                                  #3198.20
        subsd     %xmm5, %xmm15                                 #3187.21
        movsd     1088+__svml_datan2_ha_data_internal(%rip), %xmm5 #3189.19
        movd      %r8d, %xmm1                                   #3202.1701
        andps     %xmm6, %xmm5                                  #3189.19
        movl      $2145386496, %r8d                             #3202.1585
        movsd     1088+__svml_datan2_ha_data_internal(%rip), %xmm2 #3199.19
        subsd     %xmm15, %xmm0                                 #3188.20
        subsd     %xmm5, %xmm6                                  #3190.22
        andps     %xmm3, %xmm2                                  #3199.19
        addsd     %xmm0, %xmm6                                  #3191.19
        subsd     %xmm2, %xmm3                                  #3200.22
        pshufd    $85, %xmm2, %xmm14                            #3202.1445
        movd      %r9d, %xmm0                                   #3202.1774
        movdqa    %xmm14, %xmm4                                 #3202.1651
        addsd     %xmm3, %xmm7                                  #3201.19
        pslld     $3, %xmm4                                     #3202.1651
        movd      %r10d, %xmm3                                  #3202.2007
        pand      %xmm1, %xmm4                                  #3202.1701
        movd      %r11d, %xmm1                                  #3202.1517
        por       %xmm0, %xmm4                                  #3202.1774
        pand      %xmm1, %xmm14                                 #3202.1517
        rcpps     %xmm4, %xmm4                                  #3202.1874
        psrld     $3, %xmm4                                     #3202.1956
        movd      %r8d, %xmm0                                   #3202.1585
        psubd     %xmm3, %xmm4                                  #3202.2007
        psubd     %xmm14, %xmm0                                 #3202.1585
        paddd     %xmm0, %xmm4                                  #3202.2133
        movsd     3008+__svml_datan2_ha_data_internal(%rip), %xmm15 #3202.1306
        pshufd    $0, %xmm4, %xmm1                              #3202.2133
        andps     %xmm15, %xmm1                                 #3202.2244
        mulsd     %xmm1, %xmm2                                  #3203.31
        movaps    %xmm1, %xmm3                                  #3213.31
        mulsd     %xmm1, %xmm7                                  #3204.29
        mulsd     %xmm6, %xmm3                                  #3213.31
        subsd     1792+__svml_datan2_ha_data_internal(%rip), %xmm2 #3203.19
        addsd     %xmm2, %xmm7                                  #3204.17
        movaps    %xmm7, %xmm2                                  #3205.31
        mulsd     %xmm7, %xmm2                                  #3205.31
        subsd     %xmm7, %xmm2                                  #3205.19
        mulsd     %xmm7, %xmm2                                  #3206.31
        addsd     %xmm7, %xmm2                                  #3206.19
        mulsd     %xmm7, %xmm2                                  #3207.31
        subsd     %xmm7, %xmm2                                  #3207.19
        mulsd     %xmm7, %xmm2                                  #3208.31
        addsd     %xmm7, %xmm2                                  #3208.19
        mulsd     %xmm7, %xmm2                                  #3209.31
        subsd     %xmm7, %xmm2                                  #3209.19
        mulsd     %xmm1, %xmm2                                  #3210.21
        mulsd     %xmm5, %xmm1                                  #3214.29
        movaps    %xmm2, %xmm7                                  #3211.18
        movaps    %xmm1, %xmm15                                 #3214.17
        mulsd     %xmm5, %xmm7                                  #3211.18
        mulsd     %xmm6, %xmm2                                  #3212.30
        movsd     1920+__svml_datan2_ha_data_internal(%rip), %xmm6 #3230.26
        addsd     %xmm7, %xmm2                                  #3212.18
        addsd     %xmm2, %xmm3                                  #3213.19
        movsd     1856+__svml_datan2_ha_data_internal(%rip), %xmm2 #3229.26
        addsd     %xmm3, %xmm15                                 #3214.17
        addsd     16(%rdx,%rdi), %xmm3                          #3242.19
        movaps    %xmm15, %xmm14                                #3227.14
        addsd     %xmm8, %xmm3                                  #3243.19
        mulsd     %xmm15, %xmm14                                #3227.14
        movaps    %xmm14, %xmm5                                 #3228.14
        mulsd     %xmm14, %xmm5                                 #3228.14
        mulsd     %xmm5, %xmm2                                  #3229.26
        mulsd     %xmm5, %xmm6                                  #3230.26
        addsd     1984+__svml_datan2_ha_data_internal(%rip), %xmm2 #3229.14
        addsd     2048+__svml_datan2_ha_data_internal(%rip), %xmm6 #3230.14
        mulsd     %xmm5, %xmm2                                  #3231.26
        mulsd     %xmm5, %xmm6                                  #3232.26
        addsd     2112+__svml_datan2_ha_data_internal(%rip), %xmm2 #3231.14
        addsd     2176+__svml_datan2_ha_data_internal(%rip), %xmm6 #3232.14
        mulsd     %xmm5, %xmm2                                  #3233.26
        mulsd     %xmm5, %xmm6                                  #3234.26
        addsd     2240+__svml_datan2_ha_data_internal(%rip), %xmm2 #3233.14
        addsd     2304+__svml_datan2_ha_data_internal(%rip), %xmm6 #3234.14
        mulsd     %xmm5, %xmm2                                  #3235.26
        mulsd     %xmm5, %xmm6                                  #3236.26
        addsd     2368+__svml_datan2_ha_data_internal(%rip), %xmm2 #3235.14
        addsd     2432+__svml_datan2_ha_data_internal(%rip), %xmm6 #3236.14
        mulsd     %xmm5, %xmm2                                  #3237.26
        mulsd     %xmm5, %xmm6                                  #3238.26
        addsd     2496+__svml_datan2_ha_data_internal(%rip), %xmm2 #3237.14
        addsd     2560+__svml_datan2_ha_data_internal(%rip), %xmm6 #3238.14
        mulsd     %xmm14, %xmm2                                 #3239.26
        addsd     %xmm6, %xmm2                                  #3239.14
        movups    32(%rsp), %xmm6                               #3250.52[spill]
        mulsd     %xmm14, %xmm2                                 #3240.14
        mulsd     %xmm15, %xmm2                                 #3244.26
        addsd     %xmm3, %xmm2                                  #3244.14
        addsd     %xmm2, %xmm1                                  #3245.18
        addsd     8(%rdx,%rdi), %xmm1                           #3246.18
        pxor      %xmm9, %xmm1                                  #3247.18
        movups    (%rsp), %xmm9                                 #3250.52[spill]
        addsd     %xmm10, %xmm1                                 #3248.18
        orps      %xmm11, %xmm1                                 #3249.14
        testb     $1, %cl                                       #3250.41
        jne       ..B8.8        # Prob 5%                       #3250.52
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm6 xmm9 xmm12 xmm13
..B8.2:                         # Preds ..B8.8 ..B8.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #3323.52
        jne       ..B8.4        # Prob 5%                       #3323.52
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm9
..B8.3:                         # Preds ..B8.5 ..B8.2
                                # Execution count [1.00e+00]
        movaps    %xmm1, %xmm0                                  #3326.12
        movq      %rbp, %rsp                                    #3326.12
        popq      %rbp                                          #3326.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #3326.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.4:                         # Preds ..B8.2
                                # Execution count [5.00e-02]: Infreq
        movups    16(%rsp), %xmm2                               #3323.204[spill]
        movsd     %xmm2, (%rsp)                                 #3323.204
        movsd     %xmm9, 64(%rsp)                               #3323.277
        movsd     %xmm1, 128(%rsp)                              #3323.350
        jne       ..B8.6        # Prob 5%                       #3323.491
                                # LOE rbx r12 r13 r14 r15 eax
..B8.5:                         # Preds ..B8.7 ..B8.6 ..B8.4
                                # Execution count [5.00e-02]: Infreq
        movsd     128(%rsp), %xmm1                              #3323.757
        jmp       ..B8.3        # Prob 100%                     #3323.757
                                # LOE rbx r12 r13 r14 r15 xmm1
..B8.6:                         # Preds ..B8.4
                                # Execution count [2.50e-03]: Infreq
        je        ..B8.5        # Prob 95%                      #3323.634
                                # LOE rbx r12 r13 r14 r15
..B8.7:                         # Preds ..B8.6
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #3323.663
        lea       64(%rsp), %rsi                                #3323.663
        lea       128(%rsp), %rdx                               #3323.663
..___tag_value___svml_atan21_ha_e9.230:
#       __svml_datan2_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_datan2_ha_cout_rare_internal           #3323.663
..___tag_value___svml_atan21_ha_e9.231:
        jmp       ..B8.5        # Prob 100%                     #3323.663
                                # LOE rbx r12 r13 r14 r15
..B8.8:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        movaps    %xmm6, %xmm0                                  #3288.21
        movaps    %xmm12, %xmm3                                 #3290.21
        movsd     832+__svml_datan2_ha_data_internal(%rip), %xmm5 #3285.25
        movaps    %xmm9, %xmm11                                 #3291.19
        movups    16(%rsp), %xmm10                              #3287.18[spill]
        movaps    %xmm5, %xmm7                                  #3286.18
        movsd     2880+__svml_datan2_ha_data_internal(%rip), %xmm2 #3296.18
        andps     %xmm10, %xmm5                                 #3287.18
        movsd     3136+__svml_datan2_ha_data_internal(%rip), %xmm4 #3289.24
        andps     %xmm9, %xmm7                                  #3286.18
        pshufd    $85, %xmm2, %xmm15                            #3309.22
        cmpnltsd  %xmm12, %xmm0                                 #3288.21
        cmpordsd  %xmm9, %xmm11                                 #3291.19
        cmpordsd  %xmm10, %xmm10                                #3292.19
        cmpeqsd   %xmm2, %xmm12                                 #3296.18
        blendvpd  %xmm0, %xmm6, %xmm3                           #3290.21
        andps     %xmm10, %xmm11                                #3293.20
        andps     %xmm0, %xmm4                                  #3289.24
        cmpeqsd   %xmm2, %xmm6                                  #3297.18
        cmpeqsd   %xmm2, %xmm3                                  #3304.17
        orps      %xmm6, %xmm12                                 #3298.20
        movaps    %xmm3, %xmm0                                  #3306.16
        pshufd    $85, %xmm12, %xmm12                           #3300.26
        pshufd    $85, %xmm11, %xmm6                            #3295.26
        pand      %xmm6, %xmm12                                 #3301.32
        movdqa    %xmm12, %xmm14                                #3302.25
        pandn     %xmm13, %xmm14                                #3302.25
        pshufd    $85, %xmm9, %xmm13                            #3311.22
        pcmpgtd   %xmm13, %xmm15                                #3312.26
        movsd     3072+__svml_datan2_ha_data_internal(%rip), %xmm8 #3284.18
        blendvpd  %xmm0, %xmm2, %xmm4                           #3306.16
        pshufd    $0, %xmm15, %xmm2                             #3313.26
        orps      %xmm7, %xmm4                                  #3307.13
        andps     %xmm8, %xmm2                                  #3315.18
        movmskps  %xmm14, %eax                                  #3303.44
        addsd     %xmm2, %xmm4                                  #3316.17
        pshufd    $0, %xmm12, %xmm0                             #3318.32
        orps      %xmm5, %xmm4                                  #3317.19
        blendvpd  %xmm0, %xmm4, %xmm1                           #3320.14
        andl      $1, %eax                                      #3303.96
        jmp       ..B8.2        # Prob 100%                     #3303.96
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm9
	.cfi_endproc
# mark_end;
	.type	__svml_atan21_ha_e9,@function
	.size	__svml_atan21_ha_e9,.-__svml_atan21_ha_e9
..LN__svml_atan21_ha_e9.7:
	.data
# -- End  __svml_atan21_ha_e9
	.text
.L_2__routine_start___svml_atan21_ha_l9_8:
# -- Begin  __svml_atan21_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan21_ha_l9
# --- __svml_atan21_ha_l9(__m128d, __m128d)
__svml_atan21_ha_l9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #3332.1
        .byte     15                                            #3607.96
        .byte     30                                            #3607.96
        .byte     250                                           #3607.96
	.cfi_startproc
..___tag_value___svml_atan21_ha_l9.234:
..L235:
                                                        #3332.1
        pushq     %rbp                                          #3332.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #3332.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #3332.1
        subq      $192, %rsp                                    #3332.1
        vmovapd   %xmm1, %xmm15                                 #3332.1
        vmovsd    2880+__svml_datan2_ha_data_internal(%rip), %xmm13 #3453.16
        movl      $-2144337920, %eax                            #3463.26
        movl      $-36700160, %edx                              #3464.26
        vmovapd   %xmm0, %xmm14                                 #3332.1
        vmovsd    896+__svml_datan2_ha_data_internal(%rip), %xmm11 #3448.20
        movl      $4, %ecx                                      #3502.183
        vandpd    %xmm11, %xmm15, %xmm10                        #3449.14
        vandpd    %xmm11, %xmm14, %xmm11                        #3450.14
        vmovsd    960+__svml_datan2_ha_data_internal(%rip), %xmm12 #3454.16
        vmovd     %eax, %xmm2                                   #3463.26
        vpshufd   $85, %xmm11, %xmm1                            #3462.16
        vmovd     %edx, %xmm3                                   #3464.26
        vpshufd   $85, %xmm10, %xmm5                            #3461.16
        lea       __svml_datan2_ha_data_internal(%rip), %rdx    #3502.421
        vmovsd    1024+__svml_datan2_ha_data_internal(%rip), %xmm6 #3455.16
        vpsubd    %xmm2, %xmm5, %xmm0                           #3465.17
        vpcmpeqd  %xmm3, %xmm0, %xmm5                           #3467.82
        movl      $8388607, %edi                                #3510.1633
        movl      $1065353216, %r8d                             #3510.1694
        movl      $133169152, %r9d                              #3510.1899
        movl      $-1048576, %r10d                              #3510.1481
        movl      $2145386496, %r11d                            #3510.1537
        vxorpd    %xmm10, %xmm15, %xmm7                         #3451.17
        vxorpd    %xmm11, %xmm14, %xmm9                         #3452.17
        xorl      %eax, %eax                                    #3470.5
        vcmpltsd  %xmm13, %xmm15, %xmm4                         #3456.24
        vandpd    %xmm12, %xmm4, %xmm8                          #3457.20
        vpsubd    %xmm2, %xmm1, %xmm12                          #3466.17
        vandpd    %xmm6, %xmm4, %xmm6                           #3458.20
        vpcmpgtd  %xmm3, %xmm0, %xmm4                           #3467.32
        vpcmpgtd  %xmm3, %xmm12, %xmm2                          #3468.32
        vpcmpeqd  %xmm3, %xmm12, %xmm12                         #3468.82
        vpor      %xmm5, %xmm4, %xmm0                           #3467.18
        vpor      %xmm12, %xmm2, %xmm3                          #3468.18
        vmulsd    1344+__svml_datan2_ha_data_internal(%rip), %xmm10, %xmm4 #3478.15
        vpor      %xmm3, %xmm0, %xmm12                          #3469.19
        vmulsd    1280+__svml_datan2_ha_data_internal(%rip), %xmm10, %xmm2 #3480.15
        vmulsd    1216+__svml_datan2_ha_data_internal(%rip), %xmm10, %xmm0 #3479.15
        vmulsd    1152+__svml_datan2_ha_data_internal(%rip), %xmm10, %xmm3 #3477.15
        vpshufd   $85, %xmm4, %xmm4                             #3486.21
        vpshufd   $85, %xmm3, %xmm5                             #3485.21
        vpsubd    %xmm1, %xmm4, %xmm4                           #3490.29
        vpshufd   $85, %xmm0, %xmm0                             #3487.21
        vpsubd    %xmm1, %xmm5, %xmm3                           #3489.29
        vpshufd   $85, %xmm2, %xmm2                             #3488.21
        vpsrad    $31, %xmm4, %xmm5                             #3494.28
        vpsubd    %xmm1, %xmm0, %xmm4                           #3491.29
        vpsubd    %xmm1, %xmm2, %xmm1                           #3492.29
        vpsrad    $31, %xmm3, %xmm3                             #3493.28
        vpsrad    $31, %xmm4, %xmm4                             #3495.28
        vpsrad    $31, %xmm1, %xmm1                             #3496.28
        vpaddd    %xmm5, %xmm3, %xmm3                           #3502.183
        vpaddd    %xmm1, %xmm4, %xmm4                           #3502.183
        vmovd     %ecx, %xmm2                                   #3502.183
        vpaddd    %xmm4, %xmm3, %xmm5                           #3502.183
        vpaddd    %xmm2, %xmm5, %xmm0                           #3502.183
        vpslld    $5, %xmm0, %xmm3                              #3502.183
        vmovd     %xmm3, %esi                                   #3502.242
        vmovsd    3008+__svml_datan2_ha_data_internal(%rip), %xmm3 #3510.1286
        vmovmskps %xmm12, %ecx                                  #3471.58
        vmovq     24(%rdx,%rsi), %xmm2                          #3505.418
        vmovq     (%rsi,%rdx), %xmm5                            #3502.421
        vandpd    %xmm10, %xmm2, %xmm0                          #3508.24
        vmovdqa   %xmm5, %xmm4                                  #3507.23
        vandpd    %xmm11, %xmm2, %xmm1                          #3506.24
        vfmadd213sd %xmm0, %xmm11, %xmm5                        #3509.23
        vmovd     %edi, %xmm2                                   #3510.1633
        vfnmadd213sd %xmm1, %xmm10, %xmm4                       #3507.23
        vpshufd   $85, %xmm5, %xmm0                             #3510.1417
        vpslld    $3, %xmm0, %xmm1                              #3510.1591
        vpand     %xmm2, %xmm1, %xmm1                           #3510.1633
        vmovd     %r8d, %xmm2                                   #3510.1694
        vpor      %xmm2, %xmm1, %xmm1                           #3510.1694
        vrcpps    %xmm1, %xmm2                                  #3510.1782
        vpsrld    $3, %xmm2, %xmm1                              #3510.1856
        vmovd     %r9d, %xmm2                                   #3510.1899
        vpsubd    %xmm2, %xmm1, %xmm2                           #3510.1899
        vmovd     %r10d, %xmm1                                  #3510.1481
        vpand     %xmm1, %xmm0, %xmm1                           #3510.1481
        vmovd     %r11d, %xmm0                                  #3510.1537
        vpsubd    %xmm1, %xmm0, %xmm0                           #3510.1537
        vpaddd    %xmm0, %xmm2, %xmm2                           #3510.2005
        vpshufd   $0, %xmm2, %xmm1                              #3510.2005
        vmovaps   %xmm5, %xmm2                                  #3514.21
        vmovsd    1792+__svml_datan2_ha_data_internal(%rip), %xmm0 #3511.21
        vandpd    %xmm3, %xmm1, %xmm1                           #3510.2108
        vmovaps   %xmm5, %xmm3                                  #3511.21
        vfnmadd213sd %xmm0, %xmm1, %xmm3                        #3511.21
        vfmadd213sd %xmm3, %xmm3, %xmm3                         #3512.21
        vfmadd213sd %xmm1, %xmm3, %xmm1                         #3513.18
        vfnmadd213sd %xmm0, %xmm1, %xmm2                        #3514.21
        vmovsd    1920+__svml_datan2_ha_data_internal(%rip), %xmm0 #3534.14
        vfmadd213sd %xmm1, %xmm2, %xmm1                         #3515.22
        vmulsd    %xmm4, %xmm1, %xmm3                           #3516.17
        vfnmadd213sd %xmm4, %xmm3, %xmm5                        #3517.21
        vmulsd    %xmm3, %xmm3, %xmm2                           #3531.14
        vmulsd    %xmm1, %xmm5, %xmm4                           #3518.19
        vmulsd    %xmm2, %xmm2, %xmm5                           #3532.14
        vmovsd    1856+__svml_datan2_ha_data_internal(%rip), %xmm1 #3533.14
        vfmadd213sd 1984+__svml_datan2_ha_data_internal(%rip), %xmm5, %xmm1 #3533.14
        vfmadd213sd 2048+__svml_datan2_ha_data_internal(%rip), %xmm5, %xmm0 #3534.14
        vfmadd213sd 2112+__svml_datan2_ha_data_internal(%rip), %xmm5, %xmm1 #3535.14
        vfmadd213sd 2176+__svml_datan2_ha_data_internal(%rip), %xmm5, %xmm0 #3536.14
        vfmadd213sd 2240+__svml_datan2_ha_data_internal(%rip), %xmm5, %xmm1 #3537.14
        vfmadd213sd 2304+__svml_datan2_ha_data_internal(%rip), %xmm5, %xmm0 #3538.14
        vfmadd213sd 2368+__svml_datan2_ha_data_internal(%rip), %xmm5, %xmm1 #3539.14
        vfmadd213sd 2432+__svml_datan2_ha_data_internal(%rip), %xmm5, %xmm0 #3540.14
        vfmadd213sd 2496+__svml_datan2_ha_data_internal(%rip), %xmm5, %xmm1 #3541.14
        vfmadd213sd 2560+__svml_datan2_ha_data_internal(%rip), %xmm5, %xmm0 #3542.14
        vfmadd213sd %xmm0, %xmm2, %xmm1                         #3543.14
        vxorpd    %xmm7, %xmm6, %xmm0                           #3545.16
        vaddsd    16(%rdx,%rsi), %xmm4, %xmm6                   #3546.19
        vmulsd    %xmm2, %xmm1, %xmm1                           #3544.14
        vaddsd    %xmm0, %xmm6, %xmm4                           #3547.19
        vfmadd213sd %xmm4, %xmm3, %xmm1                         #3548.14
        vaddsd    %xmm1, %xmm3, %xmm2                           #3549.18
        vaddsd    8(%rdx,%rsi), %xmm2, %xmm3                    #3550.18
        vxorpd    %xmm7, %xmm3, %xmm7                           #3551.18
        vaddsd    %xmm8, %xmm7, %xmm8                           #3552.18
        vorpd     %xmm9, %xmm8, %xmm5                           #3553.14
        testb     $1, %cl                                       #3554.41
        jne       ..B9.8        # Prob 5%                       #3554.52
                                # LOE rbx r12 r13 r14 r15 eax xmm5 xmm10 xmm11 xmm12 xmm13 xmm14 xmm15
..B9.2:                         # Preds ..B9.8 ..B9.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #3627.52
        jne       ..B9.4        # Prob 5%                       #3627.52
                                # LOE rbx r12 r13 r14 r15 eax xmm5 xmm14 xmm15
..B9.3:                         # Preds ..B9.5 ..B9.2
                                # Execution count [1.00e+00]
        vmovapd   %xmm5, %xmm0                                  #3630.12
        movq      %rbp, %rsp                                    #3630.12
        popq      %rbp                                          #3630.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #3630.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.4:                         # Preds ..B9.2
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm14, (%rsp)                                #3627.204
        vmovsd    %xmm15, 64(%rsp)                              #3627.277
        vmovsd    %xmm5, 128(%rsp)                              #3627.350
        jne       ..B9.6        # Prob 5%                       #3627.491
                                # LOE rbx r12 r13 r14 r15 eax
..B9.5:                         # Preds ..B9.7 ..B9.6 ..B9.4
                                # Execution count [5.00e-02]: Infreq
        vmovsd    128(%rsp), %xmm5                              #3627.757
        jmp       ..B9.3        # Prob 100%                     #3627.757
                                # LOE rbx r12 r13 r14 r15 xmm5
..B9.6:                         # Preds ..B9.4
                                # Execution count [2.50e-03]: Infreq
        je        ..B9.5        # Prob 95%                      #3627.634
                                # LOE rbx r12 r13 r14 r15
..B9.7:                         # Preds ..B9.6
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #3627.663
        lea       64(%rsp), %rsi                                #3627.663
        lea       128(%rsp), %rdx                               #3627.663
..___tag_value___svml_atan21_ha_l9.243:
#       __svml_datan2_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_datan2_ha_cout_rare_internal           #3627.663
..___tag_value___svml_atan21_ha_l9.244:
        jmp       ..B9.5        # Prob 100%                     #3627.663
                                # LOE rbx r12 r13 r14 r15
..B9.8:                         # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        vmovsd    832+__svml_datan2_ha_data_internal(%rip), %xmm8 #3589.25
        vmovapd   %xmm10, %xmm2                                 #3594.21
        vandpd    %xmm8, %xmm15, %xmm7                          #3590.18
        vandpd    %xmm8, %xmm14, %xmm4                          #3591.18
        vmovsd    3136+__svml_datan2_ha_data_internal(%rip), %xmm9 #3587.20
        vcmpnltsd %xmm10, %xmm11, %xmm0                         #3592.21
        vcmpordsd %xmm15, %xmm15, %xmm1                         #3595.19
        vcmpordsd %xmm14, %xmm14, %xmm8                         #3596.19
        vcmpeqsd  %xmm13, %xmm10, %xmm10                        #3600.18
        blendvpd  %xmm0, %xmm11, %xmm2                          #3594.21
        vandpd    %xmm9, %xmm0, %xmm3                           #3593.24
        vandpd    %xmm8, %xmm1, %xmm1                           #3597.20
        vcmpeqsd  %xmm13, %xmm11, %xmm11                        #3601.18
        vcmpeqsd  %xmm13, %xmm2, %xmm0                          #3608.17
        vorpd     %xmm11, %xmm10, %xmm8                         #3602.20
        vpshufd   $85, %xmm8, %xmm9                             #3604.22
        vpshufd   $85, %xmm1, %xmm1                             #3599.22
        blendvpd  %xmm0, %xmm13, %xmm3                          #3610.16
        vpand     %xmm1, %xmm9, %xmm11                          #3605.28
        vpshufd   $85, %xmm15, %xmm1                            #3615.18
        vorpd     %xmm7, %xmm3, %xmm3                           #3611.13
        vpshufd   $85, %xmm13, %xmm13                           #3613.18
        vpandn    %xmm12, %xmm11, %xmm12                        #3606.21
        vpcmpgtd  %xmm1, %xmm13, %xmm13                         #3616.22
        vmovsd    3072+__svml_datan2_ha_data_internal(%rip), %xmm6 #3588.18
        vpshufd   $0, %xmm13, %xmm2                             #3617.26
        vandpd    %xmm6, %xmm2, %xmm6                           #3619.18
        vmovmskps %xmm12, %eax                                  #3607.44
        vaddsd    %xmm6, %xmm3, %xmm7                           #3620.17
        vpshufd   $0, %xmm11, %xmm0                             #3622.32
        vorpd     %xmm4, %xmm7, %xmm4                           #3621.19
        blendvpd  %xmm0, %xmm4, %xmm5                           #3624.14
        andl      $1, %eax                                      #3607.96
        jmp       ..B9.2        # Prob 100%                     #3607.96
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm5 xmm14 xmm15
	.cfi_endproc
# mark_end;
	.type	__svml_atan21_ha_l9,@function
	.size	__svml_atan21_ha_l9,.-__svml_atan21_ha_l9
..LN__svml_atan21_ha_l9.8:
	.data
# -- End  __svml_atan21_ha_l9
	.text
.L_2__routine_start___svml_datan2_ha_cout_rare_internal_9:
# -- Begin  __svml_datan2_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_datan2_ha_cout_rare_internal
	.globl __svml_datan2_ha_cout_rare_internal
# --- __svml_datan2_ha_cout_rare_internal(const double *, const double *, double *)
__svml_datan2_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
# parameter 3: %rdx
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2722.1
        .byte     15                                            #2742.139
        .byte     30                                            #2742.139
        .byte     250                                           #2742.139
	.cfi_startproc
..___tag_value___svml_datan2_ha_cout_rare_internal.246:
..L247:
                                                        #2722.1
        movq      %rdx, %rcx                                    #2722.1
        movsd     1888+_vmldAtanHATab(%rip), %xmm1              #2732.37
        movsd     (%rdi), %xmm2                                 #2733.27
        movsd     (%rsi), %xmm0                                 #2735.27
        mulsd     %xmm1, %xmm2                                  #2733.27
        mulsd     %xmm0, %xmm1                                  #2735.27
        movsd     %xmm2, -48(%rsp)                              #2733.5
        movsd     %xmm1, -40(%rsp)                              #2735.5
        movzwl    -42(%rsp), %r9d                               #2738.32
        andl      $32752, %r9d                                  #2738.32
        movb      -33(%rsp), %al                                #2737.35
        movzwl    -34(%rsp), %r8d                               #2739.32
        andb      $-128, %al                                    #2737.35
        andl      $32752, %r8d                                  #2739.32
        shrl      $4, %r9d                                      #2738.32
        movb      -41(%rsp), %dl                                #2736.35
        shrb      $7, %dl                                       #2736.35
        shrb      $7, %al                                       #2737.35
        shrl      $4, %r8d                                      #2739.32
        cmpl      $2047, %r9d                                   #2740.56
        je        ..B10.49      # Prob 16%                      #2740.56
                                # LOE rcx rbx rbp r12 r13 r14 r15 r8d r9d al dl xmm1 xmm2
..B10.2:                        # Preds ..B10.1
                                # Execution count [8.40e-01]
        cmpl      $2047, %r8d                                   #2740.112
        je        ..B10.38      # Prob 16%                      #2740.112
                                # LOE rcx rbx rbp r12 r13 r14 r15 r8d r9d al dl xmm1 xmm2
..B10.3:                        # Preds ..B10.2
                                # Execution count [7.06e-01]
        testl     %r9d, %r9d                                    #2742.21
        jne       ..B10.6       # Prob 50%                      #2742.21
                                # LOE rcx rbx rbp r12 r13 r14 r15 r8d r9d al dl xmm1 xmm2
..B10.4:                        # Preds ..B10.3
                                # Execution count [3.53e-01]
        testl     $1048575, -44(%rsp)                           #2742.51
        jne       ..B10.6       # Prob 50%                      #2742.81
                                # LOE rcx rbx rbp r12 r13 r14 r15 r8d r9d al dl xmm1 xmm2
..B10.5:                        # Preds ..B10.4
                                # Execution count [1.76e-01]
        cmpl      $0, -48(%rsp)                                 #2742.139
        je        ..B10.31      # Prob 50%                      #2742.139
                                # LOE rcx rbx rbp r12 r13 r14 r15 r8d r9d al dl xmm1 xmm2
..B10.6:                        # Preds ..B10.3 ..B10.4 ..B10.5
                                # Execution count [6.17e-01]
        testl     %r8d, %r8d                                    #2743.24
        jne       ..B10.9       # Prob 50%                      #2743.24
                                # LOE rcx rbx rbp r12 r13 r14 r15 r8d r9d al dl xmm1 xmm2
..B10.7:                        # Preds ..B10.6
                                # Execution count [3.09e-01]
        testl     $1048575, -36(%rsp)                           #2743.54
        jne       ..B10.9       # Prob 50%                      #2743.84
                                # LOE rcx rbx rbp r12 r13 r14 r15 r8d r9d al dl xmm1 xmm2
..B10.8:                        # Preds ..B10.7
                                # Execution count [1.54e-01]
        cmpl      $0, -40(%rsp)                                 #2743.142
        je        ..B10.29      # Prob 50%                      #2743.142
                                # LOE rcx rbx rbp r12 r13 r14 r15 r8d r9d al dl xmm1 xmm2
..B10.9:                        # Preds ..B10.8 ..B10.7 ..B10.6
                                # Execution count [5.40e-01]
        negl      %r8d                                          #2749.18
        movsd     %xmm2, -48(%rsp)                              #2745.13
        addl      %r9d, %r8d                                    #2749.18
        movsd     %xmm1, -40(%rsp)                              #2747.13
        movb      -41(%rsp), %dil                               #2746.34
        movb      -33(%rsp), %sil                               #2748.34
        andb      $127, %dil                                    #2746.34
        andb      $127, %sil                                    #2748.34
        cmpl      $-54, %r8d                                    #2749.30
        jle       ..B10.24      # Prob 50%                      #2749.30
                                # LOE rcx rbx rbp r12 r13 r14 r15 r8d r9d al dl sil dil xmm1
..B10.10:                       # Preds ..B10.9
                                # Execution count [2.70e-01]
        cmpl      $54, %r8d                                     #2751.34
        jge       ..B10.21      # Prob 50%                      #2751.34
                                # LOE rcx rbx rbp r12 r13 r14 r15 r8d r9d al dl sil dil xmm1
..B10.11:                       # Preds ..B10.10
                                # Execution count [1.35e-01]
        movb      %sil, -33(%rsp)                               #2748.34
        movb      %dil, -41(%rsp)                               #2746.34
        testb     %al, %al                                      #2753.36
        jne       ..B10.13      # Prob 50%                      #2753.36
                                # LOE rcx rbx rbp r12 r13 r14 r15 r9d al dl
..B10.12:                       # Preds ..B10.11
                                # Execution count [6.75e-02]
        movsd     1976+_vmldAtanHATab(%rip), %xmm1              #2755.51
        movaps    %xmm1, %xmm0                                  #2756.25
        jmp       ..B10.14      # Prob 100%                     #2756.25
                                # LOE rcx rbx rbp r12 r13 r14 r15 r9d al dl xmm0 xmm1
..B10.13:                       # Preds ..B10.11
                                # Execution count [6.75e-02]
        movsd     1936+_vmldAtanHATab(%rip), %xmm1              #2760.51
        movsd     1944+_vmldAtanHATab(%rip), %xmm0              #2761.51
                                # LOE rcx rbx rbp r12 r13 r14 r15 r9d al dl xmm0 xmm1
..B10.14:                       # Preds ..B10.12 ..B10.13
                                # Execution count [1.35e-01]
        movsd     -48(%rsp), %xmm4                              #2763.29
        movsd     -40(%rsp), %xmm2                              #2763.36
        movaps    %xmm4, %xmm5                                  #2763.36
        divsd     %xmm2, %xmm5                                  #2763.36
        movzwl    -42(%rsp), %esi                               #2763.29
        movsd     %xmm5, -16(%rsp)                              #2763.21
        testl     %r9d, %r9d                                    #2764.32
        jle       ..B10.37      # Prob 16%                      #2764.32
                                # LOE rcx rbx rbp r12 r13 r14 r15 esi r9d al dl xmm0 xmm1 xmm2 xmm4 xmm5
..B10.15:                       # Preds ..B10.14
                                # Execution count [1.13e-01]
        cmpl      $2046, %r9d                                   #2766.36
        jge       ..B10.17      # Prob 50%                      #2766.36
                                # LOE rcx rbx rbp r12 r13 r14 r15 esi r9d al dl xmm0 xmm1 xmm2 xmm4 xmm5
..B10.16:                       # Preds ..B10.15
                                # Execution count [5.67e-02]
        andl      $-32753, %esi                                 #2769.50
        addl      $-1023, %r9d                                  #2770.42
        movsd     %xmm4, -48(%rsp)                              #2768.29
        addl      $16368, %esi                                  #2769.50
        movw      %si, -42(%rsp)                                #2769.50
        jmp       ..B10.18      # Prob 100%                     #2769.50
                                # LOE rcx rbx rbp r12 r13 r14 r15 r9d al dl xmm0 xmm1 xmm2 xmm5
..B10.17:                       # Preds ..B10.15
                                # Execution count [5.67e-02]
        movsd     1992+_vmldAtanHATab(%rip), %xmm3              #2774.61
        movl      $1022, %r9d                                   #2775.29
        mulsd     %xmm3, %xmm4                                  #2774.61
        movsd     %xmm4, -48(%rsp)                              #2774.29
                                # LOE rcx rbx rbp r12 r13 r14 r15 r9d al dl xmm0 xmm1 xmm2 xmm5
..B10.18:                       # Preds ..B10.16 ..B10.17 ..B10.37
                                # Execution count [1.35e-01]
        negl      %r9d                                          #2784.78
        addl      $1023, %r9d                                   #2784.78
        andl      $2047, %r9d                                   #2784.42
        movzwl    1894+_vmldAtanHATab(%rip), %esi               #2784.42
        movsd     1888+_vmldAtanHATab(%rip), %xmm3              #2783.21
        andl      $-32753, %esi                                 #2784.42
        shll      $4, %r9d                                      #2784.42
        movsd     %xmm3, -40(%rsp)                              #2783.21
        orl       %r9d, %esi                                    #2784.42
        movw      %si, -34(%rsp)                                #2784.42
        movsd     -40(%rsp), %xmm4                              #2785.36
        mulsd     %xmm4, %xmm2                                  #2785.36
        comisd    1880+_vmldAtanHATab(%rip), %xmm5              #2786.52
        jb        ..B10.20      # Prob 50%                      #2786.52
                                # LOE rcx rbx rbp r12 r13 r14 r15 al dl xmm0 xmm1 xmm2 xmm5
..B10.19:                       # Preds ..B10.18
                                # Execution count [6.75e-02]
        movsd     2000+_vmldAtanHATab(%rip), %xmm12             #2797.72
        movaps    %xmm2, %xmm3                                  #2797.206
        mulsd     %xmm2, %xmm12                                 #2797.72
        movsd     %xmm12, -72(%rsp)                             #2797.25
        movsd     -72(%rsp), %xmm13                             #2797.116
        movsd     %xmm5, -24(%rsp)                              #2794.25
        subsd     %xmm2, %xmm13                                 #2797.126
        movsd     %xmm13, -64(%rsp)                             #2797.97
        movsd     -72(%rsp), %xmm15                             #2797.156
        movsd     -64(%rsp), %xmm14                             #2797.166
        movl      -20(%rsp), %r8d                               #2796.91
        movl      %r8d, %r9d                                    #2796.115
        andl      $-524288, %r8d                                #2796.182
        andl      $-1048576, %r9d                               #2796.115
        addl      $262144, %r8d                                 #2796.193
        subsd     %xmm14, %xmm15                                #2797.166
        movsd     %xmm15, -72(%rsp)                             #2797.137
        andl      $1048575, %r8d                                #2796.204
        movsd     -72(%rsp), %xmm4                              #2797.206
        orl       %r8d, %r9d                                    #2796.204
        movl      $0, -24(%rsp)                                 #2795.46
        subsd     %xmm4, %xmm3                                  #2797.206
        movl      %r9d, -20(%rsp)                               #2796.46
        movsd     %xmm3, -64(%rsp)                              #2797.177
        movsd     -72(%rsp), %xmm5                              #2797.225
        movsd     -24(%rsp), %xmm11                             #2798.42
        movsd     -64(%rsp), %xmm9                              #2797.242
        mulsd     %xmm11, %xmm5                                 #2798.42
        mulsd     %xmm11, %xmm9                                 #2799.42
        movsd     1968+_vmldAtanHATab(%rip), %xmm8              #2800.60
        mulsd     %xmm8, %xmm5                                  #2800.60
        mulsd     %xmm8, %xmm9                                  #2801.60
        movaps    %xmm5, %xmm7                                  #2802.55
        movzwl    -10(%rsp), %edi                               #2788.51
        addsd     %xmm9, %xmm7                                  #2802.55
        movsd     %xmm7, -72(%rsp)                              #2802.25
        andl      $32752, %edi                                  #2788.51
        movsd     -72(%rsp), %xmm6                              #2802.96
        shrl      $4, %edi                                      #2788.51
        subsd     %xmm6, %xmm5                                  #2802.96
        movl      -12(%rsp), %esi                               #2790.56
        addsd     %xmm5, %xmm9                                  #2802.135
        movsd     %xmm9, -64(%rsp)                              #2802.107
        andl      $1048575, %esi                                #2790.56
        movsd     -48(%rsp), %xmm9                              #2803.44
        movsd     -72(%rsp), %xmm3                              #2802.156
        movaps    %xmm9, %xmm12                                 #2803.54
        movsd     -64(%rsp), %xmm10                             #2802.174
        movaps    %xmm9, %xmm14                                 #2803.95
        movaps    %xmm9, %xmm6                                  #2803.216
        addsd     %xmm3, %xmm12                                 #2803.54
        movsd     %xmm12, -72(%rsp)                             #2803.25
        movsd     -72(%rsp), %xmm13                             #2803.95
        shll      $20, %edi                                     #2789.36
        subsd     %xmm13, %xmm14                                #2803.95
        movsd     %xmm14, -64(%rsp)                             #2803.66
        orl       %esi, %edi                                    #2790.56
        movsd     -72(%rsp), %xmm4                              #2803.125
        addl      $-1069547520, %edi                            #2791.35
        movsd     -64(%rsp), %xmm15                             #2803.135
        movl      $113, %esi                                    #2793.25
        movsd     2000+_vmldAtanHATab(%rip), %xmm13             #2805.72
        addsd     %xmm15, %xmm4                                 #2803.135
        movsd     %xmm4, -56(%rsp)                              #2803.106
        movsd     -64(%rsp), %xmm8                              #2803.176
        sarl      $19, %edi                                     #2792.36
        addsd     %xmm3, %xmm8                                  #2803.176
        movsd     %xmm8, -64(%rsp)                              #2803.146
        cmpl      $113, %edi                                    #2793.25
        movsd     -56(%rsp), %xmm7                              #2803.216
        cmovl     %edi, %esi                                    #2793.25
        subsd     %xmm7, %xmm6                                  #2803.216
        movsd     %xmm6, -56(%rsp)                              #2803.187
        addl      %esi, %esi                                    #2830.92
        movsd     -64(%rsp), %xmm12                             #2803.246
        lea       _vmldAtanHATab(%rip), %rdi                    #2830.72
        movsd     -56(%rsp), %xmm5                              #2803.256
        movslq    %esi, %rsi                                    #2830.72
        addsd     %xmm5, %xmm12                                 #2803.256
        movsd     %xmm12, -56(%rsp)                             #2803.227
        movsd     -72(%rsp), %xmm7                              #2803.275
        mulsd     %xmm7, %xmm13                                 #2805.72
        movsd     -56(%rsp), %xmm8                              #2803.292
        movsd     %xmm13, -72(%rsp)                             #2805.25
        addsd     %xmm10, %xmm8                                 #2804.41
        movsd     -72(%rsp), %xmm4                              #2805.116
        movaps    %xmm9, %xmm10                                 #2807.72
        mulsd     2000+_vmldAtanHATab(%rip), %xmm10             #2807.72
        subsd     %xmm7, %xmm4                                  #2805.126
        movsd     %xmm4, -64(%rsp)                              #2805.97
        movsd     -72(%rsp), %xmm3                              #2805.156
        movsd     -64(%rsp), %xmm14                             #2805.166
        subsd     %xmm14, %xmm3                                 #2805.166
        movsd     %xmm3, -72(%rsp)                              #2805.137
        movsd     -72(%rsp), %xmm15                             #2805.206
        subsd     %xmm15, %xmm7                                 #2805.206
        movsd     %xmm7, -64(%rsp)                              #2805.177
        movsd     -72(%rsp), %xmm7                              #2805.225
        movsd     -64(%rsp), %xmm4                              #2805.243
        movsd     %xmm10, -72(%rsp)                             #2807.25
        movaps    %xmm2, %xmm10                                 #2811.95
        addsd     %xmm4, %xmm8                                  #2806.41
        movsd     -72(%rsp), %xmm4                              #2807.116
        subsd     -48(%rsp), %xmm4                              #2807.126
        movsd     %xmm4, -64(%rsp)                              #2807.97
        movsd     -72(%rsp), %xmm6                              #2807.156
        movsd     -64(%rsp), %xmm3                              #2807.166
        subsd     %xmm3, %xmm6                                  #2807.166
        movaps    %xmm2, %xmm3                                  #2811.54
        movsd     %xmm6, -72(%rsp)                              #2807.137
        movsd     -72(%rsp), %xmm5                              #2807.206
        subsd     %xmm5, %xmm9                                  #2807.206
        movsd     %xmm9, -64(%rsp)                              #2807.177
        movsd     -72(%rsp), %xmm12                             #2807.225
        movsd     -64(%rsp), %xmm9                              #2807.242
        mulsd     %xmm11, %xmm12                                #2808.42
        mulsd     %xmm11, %xmm9                                 #2809.42
        movaps    %xmm12, %xmm11                                #2810.55
        addsd     %xmm9, %xmm11                                 #2810.55
        movsd     %xmm11, -72(%rsp)                             #2810.25
        movsd     -72(%rsp), %xmm4                              #2810.96
        subsd     %xmm4, %xmm12                                 #2810.96
        addsd     %xmm9, %xmm12                                 #2810.135
        movsd     %xmm12, -64(%rsp)                             #2810.107
        movsd     -72(%rsp), %xmm15                             #2810.156
        movsd     -64(%rsp), %xmm6                              #2810.174
        addsd     %xmm15, %xmm3                                 #2811.54
        movsd     %xmm3, -72(%rsp)                              #2811.25
        movsd     -72(%rsp), %xmm5                              #2811.95
        movsd     2000+_vmldAtanHATab(%rip), %xmm3              #2813.72
        subsd     %xmm5, %xmm10                                 #2811.95
        movsd     %xmm10, -64(%rsp)                             #2811.66
        movsd     -72(%rsp), %xmm13                             #2811.125
        movsd     -64(%rsp), %xmm11                             #2811.135
        addsd     %xmm11, %xmm13                                #2811.135
        movsd     %xmm13, -56(%rsp)                             #2811.106
        movsd     -64(%rsp), %xmm14                             #2811.176
        movsd     2000+_vmldAtanHATab(%rip), %xmm13             #2815.137
        addsd     %xmm14, %xmm15                                #2811.176
        movsd     %xmm15, -64(%rsp)                             #2811.146
        movsd     -56(%rsp), %xmm4                              #2811.216
        movsd     1888+_vmldAtanHATab(%rip), %xmm14             #2815.83
        subsd     %xmm4, %xmm2                                  #2811.216
        movsd     %xmm2, -56(%rsp)                              #2811.187
        movsd     -64(%rsp), %xmm4                              #2811.246
        movsd     -56(%rsp), %xmm2                              #2811.256
        addsd     %xmm2, %xmm4                                  #2811.256
        movsd     %xmm4, -56(%rsp)                              #2811.227
        movsd     -72(%rsp), %xmm12                             #2811.275
        mulsd     %xmm12, %xmm3                                 #2813.72
        movsd     -56(%rsp), %xmm5                              #2811.292
        movsd     %xmm3, -72(%rsp)                              #2813.25
        addsd     %xmm6, %xmm5                                  #2812.41
        movsd     -72(%rsp), %xmm9                              #2813.116
        subsd     %xmm12, %xmm9                                 #2813.126
        movsd     %xmm9, -64(%rsp)                              #2813.97
        movsd     -72(%rsp), %xmm10                             #2813.156
        movsd     -64(%rsp), %xmm2                              #2813.166
        subsd     %xmm2, %xmm10                                 #2813.166
        movsd     %xmm10, -72(%rsp)                             #2813.137
        movsd     -72(%rsp), %xmm11                             #2813.206
        subsd     %xmm11, %xmm12                                #2813.206
        movsd     %xmm12, -64(%rsp)                             #2813.177
        movsd     -72(%rsp), %xmm9                              #2813.225
        divsd     %xmm9, %xmm14                                 #2815.83
        mulsd     %xmm14, %xmm13                                #2815.137
        movsd     -64(%rsp), %xmm10                             #2813.243
        movsd     %xmm13, -64(%rsp)                             #2815.92
        addsd     %xmm10, %xmm5                                 #2814.41
        movsd     -64(%rsp), %xmm15                             #2815.180
        movsd     1888+_vmldAtanHATab(%rip), %xmm12             #2815.335
        subsd     %xmm14, %xmm15                                #2815.190
        movsd     %xmm15, -56(%rsp)                             #2815.161
        movsd     -64(%rsp), %xmm2                              #2815.219
        movsd     -56(%rsp), %xmm4                              #2815.229
        movsd     2000+_vmldAtanHATab(%rip), %xmm13             #2825.77
        subsd     %xmm4, %xmm2                                  #2815.229
        movsd     %xmm2, -56(%rsp)                              #2815.200
        movsd     -56(%rsp), %xmm3                              #2815.266
        mulsd     %xmm3, %xmm9                                  #2815.266
        movsd     -56(%rsp), %xmm11                             #2815.372
        subsd     %xmm9, %xmm12                                 #2815.335
        mulsd     %xmm11, %xmm5                                 #2815.372
        movsd     %xmm5, -64(%rsp)                              #2815.345
        movsd     -64(%rsp), %xmm5                              #2815.411
        subsd     %xmm5, %xmm12                                 #2815.411
        movsd     %xmm12, -64(%rsp)                             #2815.383
        movsd     -64(%rsp), %xmm2                              #2815.480
        movq      -56(%rsp), %r10                               #2815.499
        movsd     -64(%rsp), %xmm6                              #2815.535
        movsd     -56(%rsp), %xmm4                              #2815.572
        movq      %r10, -40(%rsp)                               #2815.491
        movsd     -40(%rsp), %xmm3                              #2816.53
        movaps    %xmm3, %xmm5                                  #2816.53
        addsd     1888+_vmldAtanHATab(%rip), %xmm2              #2815.480
        mulsd     %xmm7, %xmm5                                  #2816.53
        mulsd     %xmm6, %xmm2                                  #2815.535
        mulsd     %xmm4, %xmm2                                  #2815.572
        mulsd     %xmm2, %xmm7                                  #2816.140
        mulsd     %xmm8, %xmm2                                  #2816.92
        mulsd     %xmm3, %xmm8                                  #2816.189
        addsd     %xmm2, %xmm7                                  #2816.140
        movsd     1872+_vmldAtanHATab(%rip), %xmm3              #2819.83
        addsd     %xmm8, %xmm7                                  #2816.189
        movsd     %xmm7, -72(%rsp)                              #2816.151
        movaps    %xmm5, %xmm7                                  #2817.54
        movsd     -72(%rsp), %xmm4                              #2816.224
        movsd     2000+_vmldAtanHATab(%rip), %xmm6              #2823.72
        addsd     %xmm4, %xmm7                                  #2817.54
        movsd     %xmm7, -72(%rsp)                              #2817.25
        movsd     -72(%rsp), %xmm8                              #2817.93
        subsd     %xmm8, %xmm5                                  #2817.93
        addsd     %xmm4, %xmm5                                  #2817.132
        movsd     %xmm5, -64(%rsp)                              #2817.104
        movsd     -72(%rsp), %xmm11                             #2817.151
        movaps    %xmm11, %xmm2                                 #2818.38
        mulsd     %xmm11, %xmm2                                 #2818.38
        mulsd     %xmm11, %xmm6                                 #2823.72
        mulsd     %xmm2, %xmm3                                  #2819.83
        movsd     -64(%rsp), %xmm4                              #2817.168
        movsd     %xmm6, -72(%rsp)                              #2823.25
        movsd     -72(%rsp), %xmm7                              #2823.116
        addsd     1864+_vmldAtanHATab(%rip), %xmm3              #2819.107
        subsd     %xmm11, %xmm7                                 #2823.126
        mulsd     %xmm2, %xmm3                                  #2819.129
        movsd     %xmm7, -64(%rsp)                              #2823.97
        movsd     -72(%rsp), %xmm9                              #2823.156
        movsd     -64(%rsp), %xmm8                              #2823.166
        addsd     1856+_vmldAtanHATab(%rip), %xmm3              #2820.62
        subsd     %xmm8, %xmm9                                  #2823.166
        mulsd     %xmm2, %xmm3                                  #2820.84
        movsd     %xmm9, -72(%rsp)                              #2823.137
        movsd     -72(%rsp), %xmm10                             #2823.206
        addsd     1848+_vmldAtanHATab(%rip), %xmm3              #2820.108
        subsd     %xmm10, %xmm11                                #2823.206
        mulsd     %xmm2, %xmm3                                  #2820.130
        movsd     %xmm11, -64(%rsp)                             #2823.177
        addsd     1840+_vmldAtanHATab(%rip), %xmm3              #2821.62
        mulsd     %xmm2, %xmm3                                  #2821.84
        addsd     1832+_vmldAtanHATab(%rip), %xmm3              #2821.108
        mulsd     %xmm2, %xmm3                                  #2821.130
        addsd     1824+_vmldAtanHATab(%rip), %xmm3              #2821.154
        mulsd     %xmm2, %xmm3                                  #2822.51
        mulsd     %xmm3, %xmm13                                 #2825.77
        movsd     -72(%rsp), %xmm2                              #2823.225
        movsd     -64(%rsp), %xmm12                             #2823.243
        movsd     %xmm13, -72(%rsp)                             #2825.25
        addsd     %xmm12, %xmm4                                 #2824.41
        movsd     -72(%rsp), %xmm14                             #2825.121
        subsd     %xmm3, %xmm14                                 #2825.131
        movsd     %xmm14, -64(%rsp)                             #2825.102
        movsd     -72(%rsp), %xmm5                              #2825.166
        movsd     -64(%rsp), %xmm15                             #2825.176
        subsd     %xmm15, %xmm5                                 #2825.176
        movsd     %xmm5, -72(%rsp)                              #2825.147
        movsd     -72(%rsp), %xmm6                              #2825.221
        subsd     %xmm6, %xmm3                                  #2825.221
        movsd     %xmm3, -64(%rsp)                              #2825.187
        movsd     -72(%rsp), %xmm6                              #2825.247
        movsd     -64(%rsp), %xmm5                              #2825.271
        movaps    %xmm6, %xmm12                                 #2826.60
        movaps    %xmm5, %xmm3                                  #2826.106
        mulsd     %xmm4, %xmm6                                  #2826.161
        mulsd     %xmm4, %xmm3                                  #2826.106
        mulsd     %xmm2, %xmm5                                  #2826.217
        mulsd     %xmm2, %xmm12                                 #2826.60
        addsd     %xmm3, %xmm6                                  #2826.161
        movaps    %xmm12, %xmm7                                 #2827.54
        movaps    %xmm12, %xmm8                                 #2827.94
        addsd     %xmm5, %xmm6                                  #2826.217
        addsd     %xmm2, %xmm7                                  #2827.54
        movsd     %xmm6, -72(%rsp)                              #2826.172
        movsd     -72(%rsp), %xmm5                              #2826.252
        movsd     %xmm7, -72(%rsp)                              #2827.25
        movsd     -72(%rsp), %xmm3                              #2827.94
        subsd     %xmm3, %xmm8                                  #2827.94
        movsd     %xmm8, -64(%rsp)                              #2827.65
        movsd     -72(%rsp), %xmm10                             #2827.124
        movsd     -64(%rsp), %xmm9                              #2827.134
        addsd     %xmm9, %xmm10                                 #2827.134
        movsd     %xmm10, -56(%rsp)                             #2827.105
        movsd     -64(%rsp), %xmm11                             #2827.174
        addsd     %xmm11, %xmm2                                 #2827.174
        movsd     %xmm2, -64(%rsp)                              #2827.145
        movsd     -56(%rsp), %xmm2                              #2827.214
        subsd     %xmm2, %xmm12                                 #2827.214
        movsd     %xmm12, -56(%rsp)                             #2827.185
        movsd     -64(%rsp), %xmm14                             #2827.244
        movsd     -56(%rsp), %xmm13                             #2827.254
        addsd     %xmm13, %xmm14                                #2827.254
        movsd     %xmm14, -56(%rsp)                             #2827.225
        movq      -72(%rsp), %r11                               #2827.273
        movsd     -56(%rsp), %xmm15                             #2827.290
        movq      %r11, -40(%rsp)                               #2827.265
        addsd     %xmm15, %xmm4                                 #2828.41
        movsd     -40(%rsp), %xmm8                              #2830.44
        addsd     %xmm5, %xmm4                                  #2829.41
        movsd     %xmm4, -32(%rsp)                              #2829.25
        movaps    %xmm8, %xmm4                                  #2830.72
        movaps    %xmm8, %xmm2                                  #2830.133
        addsd     (%rdi,%rsi,8), %xmm4                          #2830.72
        movsd     %xmm4, -72(%rsp)                              #2830.25
        movsd     -72(%rsp), %xmm4                              #2830.133
        subsd     %xmm4, %xmm2                                  #2830.133
        movsd     %xmm2, -64(%rsp)                              #2830.104
        movsd     -72(%rsp), %xmm5                              #2830.163
        movsd     -64(%rsp), %xmm3                              #2830.173
        addsd     %xmm3, %xmm5                                  #2830.173
        movsd     %xmm5, -56(%rsp)                              #2830.144
        movsd     -64(%rsp), %xmm6                              #2830.252
        addsd     (%rdi,%rsi,8), %xmm6                          #2830.252
        movsd     %xmm6, -64(%rsp)                              #2830.184
        movsd     -56(%rsp), %xmm7                              #2830.292
        subsd     %xmm7, %xmm8                                  #2830.292
        movsd     %xmm8, -56(%rsp)                              #2830.263
        movsd     -64(%rsp), %xmm10                             #2830.322
        movsd     -56(%rsp), %xmm9                              #2830.332
        addsd     %xmm9, %xmm10                                 #2830.332
        movsd     %xmm10, -56(%rsp)                             #2830.303
        movq      -72(%rsp), %r8                                #2830.351
        movq      %r8, -40(%rsp)                                #2830.343
                                # LOE rcx rbx rbp rsi rdi r8 r12 r13 r14 r15 al dl xmm0 xmm1
..B10.56:                       # Preds ..B10.19
                                # Execution count [6.75e-02]
        movsd     -56(%rsp), %xmm2                              #2830.369
        movaps    %xmm1, %xmm3                                  #2836.54
        shrq      $56, %r8                                      #2833.46
        addsd     -32(%rsp), %xmm2                              #2831.41
        shlb      $7, %dl                                       #2840.46
        addsd     8(%rdi,%rsi,8), %xmm2                         #2832.58
        movb      %al, %sil                                     #2833.46
        andb      $127, %r8b                                    #2833.46
        shlb      $7, %sil                                      #2833.46
        movsd     %xmm2, -32(%rsp)                              #2832.25
        orb       %sil, %r8b                                    #2833.46
        movb      %r8b, -33(%rsp)                               #2833.46
        movsd     -40(%rsp), %xmm9                              #2836.44
        movaps    %xmm9, %xmm5                                  #2836.95
        addsd     %xmm9, %xmm3                                  #2836.54
        movsd     %xmm3, -72(%rsp)                              #2836.25
        movsd     -72(%rsp), %xmm4                              #2836.95
        movb      -25(%rsp), %dil                               #2835.46
        movb      %dil, %r9b                                    #2835.46
        shrb      $7, %dil                                      #2834.63
        subsd     %xmm4, %xmm5                                  #2836.95
        movsd     %xmm5, -64(%rsp)                              #2836.66
        movsd     -72(%rsp), %xmm7                              #2836.125
        movsd     -64(%rsp), %xmm6                              #2836.135
        xorb      %dil, %al                                     #2834.63
        andb      $127, %r9b                                    #2835.46
        shlb      $7, %al                                       #2835.46
        addsd     %xmm6, %xmm7                                  #2836.135
        movsd     %xmm7, -56(%rsp)                              #2836.106
        movsd     -64(%rsp), %xmm8                              #2836.176
        addsd     %xmm8, %xmm1                                  #2836.176
        movsd     %xmm1, -64(%rsp)                              #2836.146
        orb       %al, %r9b                                     #2835.46
        movsd     -56(%rsp), %xmm1                              #2836.216
        movb      %r9b, -25(%rsp)                               #2835.46
        subsd     %xmm1, %xmm9                                  #2836.216
        movsd     %xmm9, -56(%rsp)                              #2836.187
        movsd     -64(%rsp), %xmm11                             #2836.246
        movsd     -56(%rsp), %xmm10                             #2836.256
        addsd     %xmm10, %xmm11                                #2836.256
        movsd     %xmm11, -56(%rsp)                             #2836.227
        movq      -72(%rsp), %rax                               #2836.275
        movsd     -56(%rsp), %xmm12                             #2836.293
        movq      %rax, -40(%rsp)                               #2836.267
        addsd     %xmm12, %xmm0                                 #2838.41
        movsd     -40(%rsp), %xmm13                             #2839.33
        addsd     -32(%rsp), %xmm0                              #2837.41
        movsd     %xmm0, -32(%rsp)                              #2838.25
        addsd     %xmm0, %xmm13                                 #2839.41
        movsd     %xmm13, -24(%rsp)                             #2839.25
        movb      -17(%rsp), %r10b                              #2840.46
        andb      $127, %r10b                                   #2840.46
        orb       %dl, %r10b                                    #2840.46
        movb      %r10b, -17(%rsp)                              #2840.46
        movq      -24(%rsp), %rdx                               #2841.32
        movq      %rdx, (%rcx)                                  #2841.27
        jmp       ..B10.36      # Prob 100%                     #2841.27
                                # LOE rbx rbp r12 r13 r14 r15
..B10.20:                       # Preds ..B10.18
                                # Execution count [6.75e-02]
        movsd     -48(%rsp), %xmm12                             #2845.44
        movb      %al, %r8b                                     #2862.46
        movaps    %xmm12, %xmm7                                 #2845.72
        mulsd     2000+_vmldAtanHATab(%rip), %xmm7              #2845.72
        shlb      $7, %r8b                                      #2862.46
        shlb      $7, %dl                                       #2869.46
        movsd     %xmm7, -72(%rsp)                              #2845.25
        movsd     -72(%rsp), %xmm8                              #2845.116
        movsd     2000+_vmldAtanHATab(%rip), %xmm13             #2846.72
        movsd     1888+_vmldAtanHATab(%rip), %xmm7              #2847.83
        mulsd     %xmm2, %xmm13                                 #2846.72
        subsd     -48(%rsp), %xmm8                              #2845.126
        movsd     %xmm8, -64(%rsp)                              #2845.97
        movsd     -72(%rsp), %xmm10                             #2845.156
        movsd     -64(%rsp), %xmm9                              #2845.166
        subsd     %xmm9, %xmm10                                 #2845.166
        movsd     %xmm10, -72(%rsp)                             #2845.137
        movsd     -72(%rsp), %xmm11                             #2845.206
        subsd     %xmm11, %xmm12                                #2845.206
        movsd     %xmm12, -64(%rsp)                             #2845.177
        movsd     -72(%rsp), %xmm6                              #2845.225
        movsd     -64(%rsp), %xmm5                              #2845.242
        movsd     %xmm13, -72(%rsp)                             #2846.25
        movsd     -72(%rsp), %xmm14                             #2846.116
        subsd     %xmm2, %xmm14                                 #2846.126
        movsd     %xmm14, -64(%rsp)                             #2846.97
        movsd     -72(%rsp), %xmm4                              #2846.156
        movsd     -64(%rsp), %xmm15                             #2846.166
        subsd     %xmm15, %xmm4                                 #2846.166
        movsd     %xmm4, -72(%rsp)                              #2846.137
        movsd     -72(%rsp), %xmm3                              #2846.206
        movsd     1888+_vmldAtanHATab(%rip), %xmm4              #2847.335
        subsd     %xmm3, %xmm2                                  #2846.206
        movsd     %xmm2, -64(%rsp)                              #2846.177
        movsd     -72(%rsp), %xmm12                             #2846.225
        divsd     %xmm12, %xmm7                                 #2847.83
        movsd     2000+_vmldAtanHATab(%rip), %xmm2              #2847.137
        mulsd     %xmm7, %xmm2                                  #2847.137
        movsd     -64(%rsp), %xmm14                             #2846.242
        movsd     %xmm2, -64(%rsp)                              #2847.92
        movsd     -64(%rsp), %xmm8                              #2847.180
        subsd     %xmm7, %xmm8                                  #2847.190
        movsd     %xmm8, -56(%rsp)                              #2847.161
        movsd     -64(%rsp), %xmm10                             #2847.219
        movsd     -56(%rsp), %xmm9                              #2847.229
        subsd     %xmm9, %xmm10                                 #2847.229
        movsd     %xmm10, -56(%rsp)                             #2847.200
        movsd     -56(%rsp), %xmm11                             #2847.266
        mulsd     %xmm11, %xmm12                                #2847.266
        movsd     -56(%rsp), %xmm13                             #2847.372
        subsd     %xmm12, %xmm4                                 #2847.335
        mulsd     %xmm13, %xmm14                                #2847.372
        movsd     %xmm14, -64(%rsp)                             #2847.345
        movsd     -64(%rsp), %xmm15                             #2847.411
        movsd     2000+_vmldAtanHATab(%rip), %xmm13             #2857.77
        subsd     %xmm15, %xmm4                                 #2847.411
        movsd     %xmm4, -64(%rsp)                              #2847.383
        movsd     -64(%rsp), %xmm7                              #2847.480
        movq      -56(%rsp), %rsi                               #2847.499
        movsd     -64(%rsp), %xmm2                              #2847.535
        movsd     -56(%rsp), %xmm3                              #2847.572
        movq      %rsi, -40(%rsp)                               #2847.491
        movsd     -40(%rsp), %xmm8                              #2848.53
        movaps    %xmm8, %xmm9                                  #2848.53
        addsd     1888+_vmldAtanHATab(%rip), %xmm7              #2847.480
        mulsd     %xmm6, %xmm9                                  #2848.53
        mulsd     %xmm5, %xmm8                                  #2848.189
        mulsd     %xmm2, %xmm7                                  #2847.535
        movsd     -16(%rsp), %xmm2                              #2850.32
        mulsd     %xmm2, %xmm2                                  #2850.38
        mulsd     %xmm3, %xmm7                                  #2847.572
        movsd     1872+_vmldAtanHATab(%rip), %xmm3              #2851.83
        mulsd     %xmm2, %xmm3                                  #2851.83
        mulsd     %xmm7, %xmm6                                  #2848.140
        mulsd     %xmm5, %xmm7                                  #2848.92
        addsd     1864+_vmldAtanHATab(%rip), %xmm3              #2851.107
        addsd     %xmm7, %xmm6                                  #2848.140
        mulsd     %xmm2, %xmm3                                  #2851.129
        addsd     %xmm8, %xmm6                                  #2848.189
        addsd     1856+_vmldAtanHATab(%rip), %xmm3              #2852.62
        mulsd     %xmm2, %xmm3                                  #2852.84
        movaps    %xmm9, %xmm5                                  #2849.54
        movsd     %xmm6, -72(%rsp)                              #2848.151
        movsd     -72(%rsp), %xmm4                              #2848.224
        addsd     1848+_vmldAtanHATab(%rip), %xmm3              #2852.108
        addsd     %xmm4, %xmm5                                  #2849.54
        mulsd     %xmm2, %xmm3                                  #2852.130
        movsd     %xmm5, -72(%rsp)                              #2849.25
        movsd     -72(%rsp), %xmm6                              #2849.93
        movsd     2000+_vmldAtanHATab(%rip), %xmm5              #2855.72
        subsd     %xmm6, %xmm9                                  #2849.93
        addsd     1840+_vmldAtanHATab(%rip), %xmm3              #2853.62
        addsd     %xmm4, %xmm9                                  #2849.132
        mulsd     %xmm2, %xmm3                                  #2853.84
        movsd     %xmm9, -64(%rsp)                              #2849.104
        movsd     -72(%rsp), %xmm11                             #2849.151
        mulsd     %xmm11, %xmm5                                 #2855.72
        addsd     1832+_vmldAtanHATab(%rip), %xmm3              #2853.108
        movsd     -64(%rsp), %xmm4                              #2849.168
        movsd     %xmm5, -72(%rsp)                              #2855.25
        movsd     -72(%rsp), %xmm7                              #2855.116
        mulsd     %xmm2, %xmm3                                  #2853.130
        subsd     %xmm11, %xmm7                                 #2855.126
        movsd     %xmm7, -64(%rsp)                              #2855.97
        movsd     -72(%rsp), %xmm8                              #2855.156
        movsd     -64(%rsp), %xmm6                              #2855.166
        addsd     1824+_vmldAtanHATab(%rip), %xmm3              #2853.154
        subsd     %xmm6, %xmm8                                  #2855.166
        mulsd     %xmm2, %xmm3                                  #2854.51
        movsd     %xmm8, -72(%rsp)                              #2855.137
        movsd     -72(%rsp), %xmm10                             #2855.206
        mulsd     %xmm3, %xmm13                                 #2857.77
        subsd     %xmm10, %xmm11                                #2855.206
        movsd     %xmm11, -64(%rsp)                             #2855.177
        movsd     -72(%rsp), %xmm2                              #2855.225
        movsd     -64(%rsp), %xmm12                             #2855.243
        movsd     %xmm13, -72(%rsp)                             #2857.25
        addsd     %xmm12, %xmm4                                 #2856.41
        movsd     -72(%rsp), %xmm14                             #2857.121
        subsd     %xmm3, %xmm14                                 #2857.131
        movsd     %xmm14, -64(%rsp)                             #2857.102
        movsd     -72(%rsp), %xmm5                              #2857.166
        movsd     -64(%rsp), %xmm15                             #2857.176
        subsd     %xmm15, %xmm5                                 #2857.176
        movsd     %xmm5, -72(%rsp)                              #2857.147
        movsd     -72(%rsp), %xmm6                              #2857.221
        subsd     %xmm6, %xmm3                                  #2857.221
        movsd     %xmm3, -64(%rsp)                              #2857.187
        movsd     -72(%rsp), %xmm6                              #2857.247
        movsd     -64(%rsp), %xmm5                              #2857.271
        movaps    %xmm6, %xmm12                                 #2858.60
        movaps    %xmm5, %xmm3                                  #2858.106
        mulsd     %xmm4, %xmm6                                  #2858.161
        mulsd     %xmm4, %xmm3                                  #2858.106
        mulsd     %xmm2, %xmm5                                  #2858.217
        mulsd     %xmm2, %xmm12                                 #2858.60
        addsd     %xmm3, %xmm6                                  #2858.161
        movaps    %xmm12, %xmm7                                 #2859.54
        movaps    %xmm12, %xmm8                                 #2859.94
        addsd     %xmm5, %xmm6                                  #2858.217
        addsd     %xmm2, %xmm7                                  #2859.54
        movsd     %xmm6, -72(%rsp)                              #2858.172
        movsd     -72(%rsp), %xmm5                              #2858.252
        movsd     %xmm7, -72(%rsp)                              #2859.25
        movsd     -72(%rsp), %xmm3                              #2859.94
        subsd     %xmm3, %xmm8                                  #2859.94
        movsd     %xmm8, -64(%rsp)                              #2859.65
        movsd     -72(%rsp), %xmm10                             #2859.124
        movsd     -64(%rsp), %xmm9                              #2859.134
        addsd     %xmm9, %xmm10                                 #2859.134
        movsd     %xmm10, -56(%rsp)                             #2859.105
        movsd     -64(%rsp), %xmm11                             #2859.174
        addsd     %xmm11, %xmm2                                 #2859.174
        movsd     %xmm2, -64(%rsp)                              #2859.145
        movsd     -56(%rsp), %xmm2                              #2859.214
        subsd     %xmm2, %xmm12                                 #2859.214
        movsd     %xmm12, -56(%rsp)                             #2859.185
        movsd     -64(%rsp), %xmm14                             #2859.244
        movsd     -56(%rsp), %xmm13                             #2859.254
        addsd     %xmm13, %xmm14                                #2859.254
        movsd     %xmm14, -56(%rsp)                             #2859.225
        movq      -72(%rsp), %rdi                               #2859.273
        movsd     -56(%rsp), %xmm15                             #2859.290
        movq      %rdi, -40(%rsp)                               #2859.265
        addsd     %xmm15, %xmm4                                 #2860.41
        shrq      $56, %rdi                                     #2862.46
        addsd     %xmm5, %xmm4                                  #2861.41
        andb      $127, %dil                                    #2862.46
        orb       %r8b, %dil                                    #2862.46
        movb      %dil, -33(%rsp)                               #2862.46
        movsd     %xmm4, -32(%rsp)                              #2861.25
        movaps    %xmm1, %xmm4                                  #2865.54
        movsd     -40(%rsp), %xmm7                              #2865.44
        movaps    %xmm7, %xmm2                                  #2865.95
        addsd     %xmm7, %xmm4                                  #2865.54
        movsd     %xmm4, -72(%rsp)                              #2865.25
        movsd     -72(%rsp), %xmm4                              #2865.95
        movb      -25(%rsp), %r9b                               #2864.46
        movb      %r9b, %r10b                                   #2864.46
        shrb      $7, %r9b                                      #2863.63
        subsd     %xmm4, %xmm2                                  #2865.95
        movsd     %xmm2, -64(%rsp)                              #2865.66
        movsd     -72(%rsp), %xmm5                              #2865.125
        movsd     -64(%rsp), %xmm3                              #2865.135
        xorb      %r9b, %al                                     #2863.63
        andb      $127, %r10b                                   #2864.46
        shlb      $7, %al                                       #2864.46
        addsd     %xmm3, %xmm5                                  #2865.135
        movsd     %xmm5, -56(%rsp)                              #2865.106
        movsd     -64(%rsp), %xmm6                              #2865.176
        addsd     %xmm6, %xmm1                                  #2865.176
        movsd     %xmm1, -64(%rsp)                              #2865.146
        orb       %al, %r10b                                    #2864.46
        movsd     -56(%rsp), %xmm1                              #2865.216
        movb      %r10b, -25(%rsp)                              #2864.46
        subsd     %xmm1, %xmm7                                  #2865.216
        movsd     %xmm7, -56(%rsp)                              #2865.187
        movsd     -64(%rsp), %xmm2                              #2865.246
        movsd     -56(%rsp), %xmm1                              #2865.256
        addsd     %xmm1, %xmm2                                  #2865.256
        movsd     %xmm2, -56(%rsp)                              #2865.227
        movq      -72(%rsp), %rax                               #2865.275
        movsd     -56(%rsp), %xmm3                              #2865.293
        movq      %rax, -40(%rsp)                               #2865.267
        addsd     %xmm3, %xmm0                                  #2867.41
        movsd     -40(%rsp), %xmm4                              #2868.33
        addsd     -32(%rsp), %xmm0                              #2866.41
        movsd     %xmm0, -32(%rsp)                              #2867.25
        addsd     %xmm0, %xmm4                                  #2868.41
        movsd     %xmm4, -24(%rsp)                              #2868.25
        movb      -17(%rsp), %r11b                              #2869.46
        andb      $127, %r11b                                   #2869.46
        orb       %dl, %r11b                                    #2869.46
        movb      %r11b, -17(%rsp)                              #2869.46
        movq      -24(%rsp), %rdx                               #2870.32
        movq      %rdx, (%rcx)                                  #2870.27
        jmp       ..B10.36      # Prob 100%                     #2870.27
                                # LOE rbx rbp r12 r13 r14 r15
..B10.21:                       # Preds ..B10.10
                                # Execution count [1.35e-01]
        cmpl      $74, %r8d                                     #2873.39
        jge       ..B10.53      # Prob 50%                      #2873.39
                                # LOE rcx rbx rbp r12 r13 r14 r15 dl dil xmm1
..B10.22:                       # Preds ..B10.21
                                # Execution count [6.75e-02]
        movb      %dil, -41(%rsp)                               #2746.34
        divsd     -48(%rsp), %xmm1                              #2875.36
        movsd     1928+_vmldAtanHATab(%rip), %xmm0              #2876.46
        shlb      $7, %dl                                       #2878.42
        subsd     %xmm1, %xmm0                                  #2876.69
        addsd     1920+_vmldAtanHATab(%rip), %xmm0              #2877.54
        movsd     %xmm0, -24(%rsp)                              #2877.21
        movb      -17(%rsp), %al                                #2878.42
        andb      $127, %al                                     #2878.42
        orb       %dl, %al                                      #2878.42
        movb      %al, -17(%rsp)                                #2878.42
        movq      -24(%rsp), %rdx                               #2879.28
        movq      %rdx, (%rcx)                                  #2879.23
        jmp       ..B10.36      # Prob 100%                     #2879.23
                                # LOE rbx rbp r12 r13 r14 r15
..B10.24:                       # Preds ..B10.9
                                # Execution count [2.70e-01]
        testb     %al, %al                                      #2890.32
        jne       ..B10.35      # Prob 50%                      #2890.32
                                # LOE rcx rbx rbp r12 r13 r14 r15 dl sil dil
..B10.25:                       # Preds ..B10.24
                                # Execution count [1.35e-01]
        movb      %dil, -41(%rsp)                               #2746.34
        movb      %sil, -33(%rsp)                               #2748.34
        movsd     -48(%rsp), %xmm2                              #2892.29
        divsd     -40(%rsp), %xmm2                              #2892.36
        movsd     %xmm2, -24(%rsp)                              #2892.21
        movzwl    -18(%rsp), %eax                               #2893.47
        testl     $32752, %eax                                  #2893.47
        je        ..B10.27      # Prob 50%                      #2893.73
                                # LOE rcx rbx rbp r12 r13 r14 r15 dl xmm2
..B10.26:                       # Preds ..B10.25
                                # Execution count [6.75e-02]
        movsd     1888+_vmldAtanHATab(%rip), %xmm0              #2895.75
        shlb      $7, %dl                                       #2897.46
        addsd     %xmm2, %xmm0                                  #2895.75
        movsd     %xmm0, -72(%rsp)                              #2895.25
        movsd     -72(%rsp), %xmm1                              #2896.41
        mulsd     %xmm1, %xmm2                                  #2896.41
        movsd     %xmm2, -24(%rsp)                              #2896.25
        movb      -17(%rsp), %al                                #2897.46
        andb      $127, %al                                     #2897.46
        orb       %dl, %al                                      #2897.46
        movb      %al, -17(%rsp)                                #2897.46
        movq      -24(%rsp), %rdx                               #2898.32
        movq      %rdx, (%rcx)                                  #2898.27
        jmp       ..B10.36      # Prob 100%                     #2898.27
                                # LOE rbx rbp r12 r13 r14 r15
..B10.27:                       # Preds ..B10.25
                                # Execution count [6.75e-02]
        mulsd     %xmm2, %xmm2                                  #2902.43
        shlb      $7, %dl                                       #2904.46
        movsd     %xmm2, -72(%rsp)                              #2902.25
        movsd     -72(%rsp), %xmm0                              #2903.41
        addsd     -24(%rsp), %xmm0                              #2903.41
        movsd     %xmm0, -24(%rsp)                              #2903.25
        movb      -17(%rsp), %al                                #2904.46
        andb      $127, %al                                     #2904.46
        orb       %dl, %al                                      #2904.46
        movb      %al, -17(%rsp)                                #2904.46
        movq      -24(%rsp), %rdx                               #2905.32
        movq      %rdx, (%rcx)                                  #2905.27
        jmp       ..B10.36      # Prob 100%                     #2905.27
                                # LOE rbx rbp r12 r13 r14 r15
..B10.29:                       # Preds ..B10.8
                                # Execution count [7.72e-02]
        testl     %r9d, %r9d                                    #2918.24
        jne       ..B10.53      # Prob 50%                      #2918.24
                                # LOE rcx rbx rbp r12 r13 r14 r15 al dl
..B10.30:                       # Preds ..B10.29
                                # Execution count [3.86e-02]
        testl     $1048575, -44(%rsp)                           #2918.54
        jne       ..B10.53      # Prob 50%                      #2918.84
        jmp       ..B10.57      # Prob 100%                     #2918.84
                                # LOE rcx rbx rbp r12 r13 r14 r15 al dl
..B10.31:                       # Preds ..B10.5
                                # Execution count [4.13e-02]
        jne       ..B10.53      # Prob 50%                      #2918.142
                                # LOE rcx rbx rbp r12 r13 r14 r15 al dl
..B10.33:                       # Preds ..B10.57 ..B10.31
                                # Execution count [2.07e-02]
        testb     %al, %al                                      #2926.32
        jne       ..B10.35      # Prob 50%                      #2926.32
                                # LOE rcx rbx rbp r12 r13 r14 r15 dl
..B10.34:                       # Preds ..B10.43 ..B10.33
                                # Execution count [1.03e-02]
        shlb      $7, %dl                                       #2929.42
        movq      1976+_vmldAtanHATab(%rip), %rax               #2928.46
        movq      %rax, -24(%rsp)                               #2928.21
        shrq      $56, %rax                                     #2929.42
        andb      $127, %al                                     #2929.42
        orb       %dl, %al                                      #2929.42
        movb      %al, -17(%rsp)                                #2929.42
        movq      -24(%rsp), %rdx                               #2930.28
        movq      %rdx, (%rcx)                                  #2930.23
        jmp       ..B10.36      # Prob 100%                     #2930.23
                                # LOE rbx rbp r12 r13 r14 r15
..B10.35:                       # Preds ..B10.43 ..B10.24 ..B10.33
                                # Execution count [1.03e-02]
        movsd     1936+_vmldAtanHATab(%rip), %xmm0              #2934.46
        shlb      $7, %dl                                       #2935.42
        addsd     1944+_vmldAtanHATab(%rip), %xmm0              #2934.86
        movsd     %xmm0, -24(%rsp)                              #2934.21
        movb      -17(%rsp), %al                                #2935.42
        andb      $127, %al                                     #2935.42
        orb       %dl, %al                                      #2935.42
        movb      %al, -17(%rsp)                                #2935.42
        movq      -24(%rsp), %rdx                               #2936.28
        movq      %rdx, (%rcx)                                  #2936.23
                                # LOE rbx rbp r12 r13 r14 r15
..B10.36:                       # Preds ..B10.56 ..B10.20 ..B10.22 ..B10.26 ..B10.27
                                #       ..B10.34 ..B10.35 ..B10.41 ..B10.53 ..B10.47
                                #       ..B10.48
                                # Execution count [1.00e+00]
        xorl      %eax, %eax                                    #2991.12
        ret                                                     #2991.12
                                # LOE
..B10.37:                       # Preds ..B10.14
                                # Execution count [2.16e-02]: Infreq
        movsd     1984+_vmldAtanHATab(%rip), %xmm3              #2780.57
        movl      $-1022, %r9d                                  #2781.25
        mulsd     %xmm3, %xmm4                                  #2780.57
        movsd     %xmm4, -48(%rsp)                              #2780.25
        jmp       ..B10.18      # Prob 100%                     #2780.25
                                # LOE rcx rbx rbp r12 r13 r14 r15 r9d al dl xmm0 xmm1 xmm2 xmm5
..B10.38:                       # Preds ..B10.2
                                # Execution count [1.34e-01]: Infreq
        cmpl      $2047, %r9d                                   #2943.20
        je        ..B10.49      # Prob 16%                      #2943.20
                                # LOE rcx rbx rbp r12 r13 r14 r15 r8d r9d al dl xmm1 xmm2
..B10.39:                       # Preds ..B10.38 ..B10.51
                                # Execution count [4.15e-02]: Infreq
        testl     $1048575, -36(%rsp)                           #2944.57
        jne       ..B10.41      # Prob 50%                      #2944.87
                                # LOE rcx rbx rbp r12 r13 r14 r15 r9d al dl xmm1 xmm2
..B10.40:                       # Preds ..B10.39
                                # Execution count [2.07e-02]: Infreq
        cmpl      $0, -40(%rsp)                                 #2944.145
        je        ..B10.42      # Prob 50%                      #2944.145
                                # LOE rcx rbx rbp r12 r13 r14 r15 r9d al dl xmm1 xmm2
..B10.41:                       # Preds ..B10.49 ..B10.50 ..B10.39 ..B10.40
                                # Execution count [6.64e-02]: Infreq
        addsd     %xmm1, %xmm2                                  #2946.26
        movsd     %xmm2, (%rcx)                                 #2946.15
        jmp       ..B10.36      # Prob 100%                     #2946.15
                                # LOE rbx rbp r12 r13 r14 r15
..B10.42:                       # Preds ..B10.40
                                # Execution count [3.65e-02]: Infreq
        cmpl      $2047, %r9d                                   #2958.68
        je        ..B10.46      # Prob 16%                      #2958.68
                                # LOE rcx rbx rbp r12 r13 r14 r15 al dl
..B10.43:                       # Preds ..B10.42
                                # Execution count [3.06e-02]: Infreq
        testb     %al, %al                                      #2960.36
        je        ..B10.34      # Prob 50%                      #2960.36
        jmp       ..B10.35      # Prob 100%                     #2960.36
                                # LOE rcx rbx rbp r12 r13 r14 r15 dl
..B10.46:                       # Preds ..B10.42
                                # Execution count [5.84e-03]: Infreq
        testb     %al, %al                                      #2975.36
        jne       ..B10.48      # Prob 50%                      #2975.36
                                # LOE rcx rbx rbp r12 r13 r14 r15 dl
..B10.47:                       # Preds ..B10.46
                                # Execution count [2.92e-03]: Infreq
        movsd     1904+_vmldAtanHATab(%rip), %xmm0              #2977.50
        shlb      $7, %dl                                       #2978.46
        addsd     1912+_vmldAtanHATab(%rip), %xmm0              #2977.90
        movsd     %xmm0, -24(%rsp)                              #2977.25
        movb      -17(%rsp), %al                                #2978.46
        andb      $127, %al                                     #2978.46
        orb       %dl, %al                                      #2978.46
        movb      %al, -17(%rsp)                                #2978.46
        movq      -24(%rsp), %rdx                               #2979.32
        movq      %rdx, (%rcx)                                  #2979.27
        jmp       ..B10.36      # Prob 100%                     #2979.27
                                # LOE rbx rbp r12 r13 r14 r15
..B10.48:                       # Preds ..B10.46
                                # Execution count [2.92e-03]: Infreq
        movsd     1952+_vmldAtanHATab(%rip), %xmm0              #2983.50
        shlb      $7, %dl                                       #2984.46
        addsd     1960+_vmldAtanHATab(%rip), %xmm0              #2983.90
        movsd     %xmm0, -24(%rsp)                              #2983.25
        movb      -17(%rsp), %al                                #2984.46
        andb      $127, %al                                     #2984.46
        orb       %dl, %al                                      #2984.46
        movb      %al, -17(%rsp)                                #2984.46
        movq      -24(%rsp), %rdx                               #2985.32
        movq      %rdx, (%rcx)                                  #2985.27
        jmp       ..B10.36      # Prob 100%                     #2985.27
                                # LOE rbx rbp r12 r13 r14 r15
..B10.49:                       # Preds ..B10.1 ..B10.38
                                # Execution count [4.71e-02]: Infreq
        testl     $1048575, -44(%rsp)                           #2943.54
        jne       ..B10.41      # Prob 50%                      #2943.84
                                # LOE rcx rbx rbp r12 r13 r14 r15 r8d r9d al dl xmm1 xmm2
..B10.50:                       # Preds ..B10.49
                                # Execution count [2.36e-02]: Infreq
        cmpl      $0, -48(%rsp)                                 #2943.142
        jne       ..B10.41      # Prob 50%                      #2943.142
                                # LOE rcx rbx rbp r12 r13 r14 r15 r8d r9d al dl xmm1 xmm2
..B10.51:                       # Preds ..B10.50
                                # Execution count [1.46e-01]: Infreq
        cmpl      $2047, %r8d                                   #2944.23
        je        ..B10.39      # Prob 16%                      #2944.23
                                # LOE rcx rbx rbp r12 r13 r14 r15 r9d al dl xmm1 xmm2
..B10.53:                       # Preds ..B10.29 ..B10.30 ..B10.31 ..B10.57 ..B10.21
                                #       ..B10.51
                                # Execution count [1.92e-01]: Infreq
        movsd     1920+_vmldAtanHATab(%rip), %xmm0              #2952.42
        shlb      $7, %dl                                       #2953.38
        addsd     1928+_vmldAtanHATab(%rip), %xmm0              #2952.82
        movsd     %xmm0, -24(%rsp)                              #2952.17
        movb      -17(%rsp), %al                                #2953.38
        andb      $127, %al                                     #2953.38
        orb       %dl, %al                                      #2953.38
        movb      %al, -17(%rsp)                                #2953.38
        movq      -24(%rsp), %rdx                               #2954.24
        movq      %rdx, (%rcx)                                  #2954.19
        jmp       ..B10.36      # Prob 100%                     #2954.19
                                # LOE rbx rbp r12 r13 r14 r15
..B10.57:                       # Preds ..B10.30
                                # Execution count [1.93e-02]: Infreq
        cmpl      $0, -48(%rsp)                                 #2742.139
        jne       ..B10.53      # Prob 50%                      #2742.139
        jmp       ..B10.33      # Prob 100%                     #2742.139
        .align    16,0x90
                                # LOE rcx rbx rbp r12 r13 r14 r15 al dl
	.cfi_endproc
# mark_end;
	.type	__svml_datan2_ha_cout_rare_internal,@function
	.size	__svml_datan2_ha_cout_rare_internal,.-__svml_datan2_ha_cout_rare_internal
..LN__svml_datan2_ha_cout_rare_internal.9:
	.data
# -- End  __svml_datan2_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_datan2_ha_data_internal
	.globl __svml_datan2_ha_data_internal
__svml_datan2_ha_data_internal:
	.long	0
	.long	1072693248
	.long	1413754136
	.long	1073291771
	.long	856972295
	.long	1016178214
	.long	0
	.long	0
	.long	0
	.long	1073217536
	.long	3531732635
	.long	1072657163
	.long	2062601149
	.long	1013974920
	.long	4294967295
	.long	4294967295
	.long	0
	.long	1072693248
	.long	1413754136
	.long	1072243195
	.long	856972295
	.long	1015129638
	.long	4294967295
	.long	4294967295
	.long	0
	.long	1071644672
	.long	90291023
	.long	1071492199
	.long	573531618
	.long	1014639487
	.long	4294967295
	.long	4294967295
	.long	0
	.long	0
	.long	0
	.long	0
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
	.long	4293918720
	.long	4293918720
	.long	4293918720
	.long	4293918720
	.long	4293918720
	.long	4293918720
	.long	4293918720
	.long	4293918720
	.long	4293918720
	.long	4293918720
	.long	4293918720
	.long	4293918720
	.long	4293918720
	.long	4293918720
	.long	4293918720
	.long	4293918720
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
	.long	133169152
	.long	133169152
	.long	133169152
	.long	133169152
	.long	133169152
	.long	133169152
	.long	133169152
	.long	133169152
	.long	133169152
	.long	133169152
	.long	133169152
	.long	133169152
	.long	133169152
	.long	133169152
	.long	133169152
	.long	133169152
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
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
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
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
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
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	1005584384
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.type	__svml_datan2_ha_data_internal,@object
	.size	__svml_datan2_ha_data_internal,3328
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
	.long	1069547520
	.long	0
	.long	1072693248
	.long	0
	.long	1073741824
	.long	1413754136
	.long	1072243195
	.long	856972295
	.long	1015129638
	.long	1413754136
	.long	1073291771
	.long	856972295
	.long	1016178214
	.long	1413754136
	.long	1074340347
	.long	856972295
	.long	1017226790
	.long	2134057426
	.long	1073928572
	.long	1285458442
	.long	1016756537
	.long	0
	.long	3220176896
	.long	0
	.long	0
	.long	0
	.long	2144337920
	.long	0
	.long	1048576
	.long	33554432
	.long	1101004800
	.type	_vmldAtanHATab,@object
	.size	_vmldAtanHATab,2008
	.align 8
.L_2il0floatpacket.51:
	.long	0xffffffff,0xffffffff
	.type	.L_2il0floatpacket.51,@object
	.size	.L_2il0floatpacket.51,8
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
