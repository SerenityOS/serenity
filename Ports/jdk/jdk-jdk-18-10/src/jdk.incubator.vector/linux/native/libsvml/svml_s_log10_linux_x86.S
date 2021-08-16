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
	.file "svml_s_log10.c"
	.text
..TXTST0:
.L_2__routine_start___svml_log10f4_ha_e9_0:
# -- Begin  __svml_log10f4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log10f4_ha_e9
# --- __svml_log10f4_ha_e9(__m128)
__svml_log10f4_ha_e9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #375.1
        .byte     15                                            #450.540
        .byte     30                                            #450.540
        .byte     250                                           #450.540
	.cfi_startproc
..___tag_value___svml_log10f4_ha_e9.1:
..L2:
                                                          #375.1
        pushq     %rbp                                          #375.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #375.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #375.1
        subq      $192, %rsp                                    #375.1
        movaps    %xmm0, %xmm3                                  #416.23
        movups    1280+__svml_slog10_ha_data_internal(%rip), %xmm1 #410.19
        psrld     $23, %xmm3                                    #416.23
        andps     %xmm0, %xmm1                                  #410.19
        lea       -136944+__svml_slog10_ha_data_internal(%rip), %r8 #432.61
        orps      1344+__svml_slog10_ha_data_internal(%rip), %xmm1 #411.19
        movaps    %xmm0, %xmm5                                  #417.18
        rcpps     %xmm1, %xmm2                                  #414.17
        cvtdq2ps  %xmm3, %xmm13                                 #422.18
        cmpltps   1408+__svml_slog10_ha_data_internal(%rip), %xmm5 #417.18
        movups    1600+__svml_slog10_ha_data_internal(%rip), %xmm6 #419.44
        movaps    %xmm0, %xmm4                                  #418.18
        mulps     %xmm6, %xmm2                                  #420.17
        cmpnleps  1472+__svml_slog10_ha_data_internal(%rip), %xmm4 #418.18
        movups    1536+__svml_slog10_ha_data_internal(%rip), %xmm7 #426.49
        orps      %xmm4, %xmm5                                  #423.18
        roundps   $0, %xmm2, %xmm2                              #421.17
        andps     %xmm1, %xmm7                                  #427.118
        movups    1664+__svml_slog10_ha_data_internal(%rip), %xmm11 #436.25
        subps     %xmm7, %xmm1                                  #427.172
        mulps     %xmm2, %xmm7                                  #427.225
        mulps     %xmm13, %xmm11                                #436.25
        mulps     %xmm2, %xmm1                                  #427.276
        subps     %xmm6, %xmm7                                  #427.322
        movmskps  %xmm5, %eax                                   #425.44
        psrld     $13, %xmm2                                    #429.22
        addps     %xmm7, %xmm1                                  #427.365
        movd      %xmm2, %edx                                   #432.183
        movaps    %xmm1, %xmm5                                  #443.17
        movaps    %xmm1, %xmm4                                  #445.13
        pextrd    $1, %xmm2, %ecx                               #432.396
        pextrd    $2, %xmm2, %esi                               #432.612
        pextrd    $3, %xmm2, %edi                               #432.828
        movups    1088+__svml_slog10_ha_data_internal(%rip), %xmm2 #438.54
        movslq    %edx, %rdx                                    #432.61
        movslq    %ecx, %rcx                                    #432.274
        mulps     %xmm1, %xmm2                                  #441.26
        movslq    %esi, %rsi                                    #432.490
        addps     1152+__svml_slog10_ha_data_internal(%rip), %xmm2 #441.14
        movslq    %edi, %rdi                                    #432.706
        movq      (%r8,%rdx), %xmm3                             #432.61
        movq      (%r8,%rcx), %xmm8                             #432.274
        movq      (%r8,%rsi), %xmm10                            #432.490
        movq      (%r8,%rdi), %xmm9                             #432.706
        unpcklps  %xmm8, %xmm3                                  #432.875
        unpcklps  %xmm9, %xmm10                                 #432.910
        movaps    %xmm3, %xmm14                                 #432.950
        movlhps   %xmm10, %xmm14                                #432.950
        mulps     %xmm1, %xmm2                                  #442.25
        addps     %xmm11, %xmm14                                #436.13
        movups    1728+__svml_slog10_ha_data_internal(%rip), %xmm12 #435.44
        addps     %xmm14, %xmm5                                 #443.17
        mulps     %xmm12, %xmm13                                #437.25
        addps     1216+__svml_slog10_ha_data_internal(%rip), %xmm2 #442.13
        shufps    $238, %xmm10, %xmm3                           #432.994
        movaps    %xmm5, %xmm15                                 #444.13
        mulps     %xmm2, %xmm1                                  #446.25
        addps     %xmm13, %xmm3                                 #437.13
        subps     %xmm14, %xmm15                                #444.13
        addps     %xmm1, %xmm3                                  #446.13
        subps     %xmm15, %xmm4                                 #445.13
        addps     %xmm3, %xmm4                                  #447.13
        addps     %xmm4, %xmm5                                  #448.14
        testl     %eax, %eax                                    #450.52
        jne       ..B1.3        # Prob 5%                       #450.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm5
..B1.2:                         # Preds ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movaps    %xmm5, %xmm0                                  #453.12
        movq      %rbp, %rsp                                    #453.12
        popq      %rbp                                          #453.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #453.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #450.193
        movups    %xmm5, 128(%rsp)                              #450.264
                                # LOE rbx r12 r13 r14 r15 eax
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #450.448
        movq      %r12, 8(%rsp)                                 #450.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #450.448
        movq      %r13, (%rsp)                                  #450.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #450.448
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #450.511
        jc        ..B1.10       # Prob 5%                       #450.511
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #450.464
        cmpl      $4, %r12d                                     #450.459
        jl        ..B1.7        # Prob 82%                      #450.459
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm5                              #450.667
        jmp       ..B1.2        # Prob 100%                     #450.667
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm5
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #450.540
        lea       128(%rsp,%r12,4), %rsi                        #450.540
..___tag_value___svml_log10f4_ha_e9.19:
#       __svml_slog10_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog10_ha_cout_rare_internal           #450.540
..___tag_value___svml_log10f4_ha_e9.20:
        jmp       ..B1.8        # Prob 100%                     #450.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log10f4_ha_e9,@function
	.size	__svml_log10f4_ha_e9,.-__svml_log10f4_ha_e9
..LN__svml_log10f4_ha_e9.0:
	.data
# -- End  __svml_log10f4_ha_e9
	.text
.L_2__routine_start___svml_log10f8_ha_l9_1:
# -- Begin  __svml_log10f8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log10f8_ha_l9
# --- __svml_log10f8_ha_l9(__m256)
__svml_log10f8_ha_l9:
# parameter 1: %ymm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #458.1
        .byte     15                                            #533.546
        .byte     30                                            #533.546
        .byte     250                                           #533.546
	.cfi_startproc
..___tag_value___svml_log10f8_ha_l9.22:
..L23:
                                                         #458.1
        pushq     %rbp                                          #458.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #458.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #458.1
        subq      $192, %rsp                                    #458.1
        lea       -136944+__svml_slog10_ha_data_internal(%rip), %rax #515.64
        vmovaps   %ymm0, %ymm2                                  #458.1
        vandps    1280+__svml_slog10_ha_data_internal(%rip), %ymm2, %ymm3 #493.19
        vorps     1344+__svml_slog10_ha_data_internal(%rip), %ymm3, %ymm1 #494.19
        vrcpps    %ymm1, %ymm4                                  #497.17
        vmovups   1600+__svml_slog10_ha_data_internal(%rip), %ymm10 #502.47
        vpsrld    $23, %ymm2, %ymm6                             #499.19
        vcmplt_oqps 1408+__svml_slog10_ha_data_internal(%rip), %ymm2, %ymm7 #500.18
        vcmpnle_uqps 1472+__svml_slog10_ha_data_internal(%rip), %ymm2, %ymm8 #501.18
        vcvtdq2ps %ymm6, %ymm0                                  #505.18
        vmulps    %ymm10, %ymm4, %ymm5                          #503.17
        vroundps  $0, %ymm5, %ymm11                             #504.17
        vpsrld    $13, %ymm11, %ymm12                           #512.18
        vfmsub213ps %ymm10, %ymm11, %ymm1                       #510.13
        vorps     %ymm8, %ymm7, %ymm9                           #506.18
        vmovmskps %ymm9, %edx                                   #508.45
        vmovd     %xmm12, %ecx                                  #515.186
        vextractf128 $1, %ymm12, %xmm13                         #515.1190
        movslq    %ecx, %rcx                                    #515.64
        vmovd     %xmm13, %r9d                                  #515.1171
        vpextrd   $2, %xmm13, %r11d                             #515.1672
        movslq    %r9d, %r9                                     #515.1049
        movslq    %r11d, %r11                                   #515.1550
        vpextrd   $1, %xmm12, %esi                              #515.430
        vpextrd   $2, %xmm12, %edi                              #515.677
        vpextrd   $3, %xmm12, %r8d                              #515.924
        vmovq     (%rax,%rcx), %xmm14                           #515.64
        vpextrd   $1, %xmm13, %r10d                             #515.1420
        vpextrd   $3, %xmm13, %ecx                              #515.1924
        movslq    %esi, %rsi                                    #515.308
        movslq    %edi, %rdi                                    #515.555
        movslq    %r8d, %r8                                     #515.802
        movslq    %r10d, %r10                                   #515.1298
        movslq    %ecx, %rcx                                    #515.1802
        vmovq     (%rax,%r9), %xmm5                             #515.1049
        vmovq     (%rax,%r11), %xmm6                            #515.1550
        vunpcklps %xmm6, %xmm5, %xmm10                          #515.2128
        vmovups   1088+__svml_slog10_ha_data_internal(%rip), %ymm6 #521.57
        vmovq     (%rax,%rsi), %xmm3                            #515.308
        vmovq     (%rax,%rdi), %xmm15                           #515.555
        vmovq     (%rax,%r8), %xmm4                             #515.802
        vmovq     (%rax,%r10), %xmm7                            #515.1298
        vmovq     (%rax,%rcx), %xmm8                            #515.1802
        vunpcklps %xmm15, %xmm14, %xmm9                         #515.2018
        vunpcklps %xmm4, %xmm3, %xmm11                          #515.2073
        vunpcklps %xmm8, %xmm7, %xmm12                          #515.2183
        vfmadd213ps 1152+__svml_slog10_ha_data_internal(%rip), %ymm1, %ymm6 #524.14
        vfmadd213ps 1216+__svml_slog10_ha_data_internal(%rip), %ymm1, %ymm6 #525.13
        vinsertf128 $1, %xmm10, %ymm9, %ymm13                   #515.2246
        vinsertf128 $1, %xmm12, %ymm11, %ymm14                  #515.2354
        vunpcklps %ymm14, %ymm13, %ymm3                         #515.2445
        vfmadd231ps 1664+__svml_slog10_ha_data_internal(%rip), %ymm0, %ymm3 #519.13
        vunpckhps %ymm14, %ymm13, %ymm15                        #515.2519
        vfmadd132ps 1728+__svml_slog10_ha_data_internal(%rip), %ymm15, %ymm0 #520.13
        vaddps    %ymm3, %ymm1, %ymm7                           #526.17
        vfmadd213ps %ymm0, %ymm1, %ymm6                         #529.13
        vsubps    %ymm3, %ymm7, %ymm4                           #527.13
        vsubps    %ymm4, %ymm1, %ymm5                           #528.13
        vaddps    %ymm6, %ymm5, %ymm0                           #530.13
        vaddps    %ymm0, %ymm7, %ymm0                           #531.14
        testl     %edx, %edx                                    #533.52
        jne       ..B2.3        # Prob 5%                       #533.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm2
..B2.2:                         # Preds ..B2.3 ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #536.12
        popq      %rbp                                          #536.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #536.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm2, 64(%rsp)                               #533.196
        vmovups   %ymm0, 128(%rsp)                              #533.270
        je        ..B2.2        # Prob 95%                      #533.374
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #533.454
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
        btl       %r12d, %r13d                                  #533.517
        jc        ..B2.10       # Prob 5%                       #533.517
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #533.470
        cmpl      $8, %r12d                                     #533.465
        jl        ..B2.7        # Prob 82%                      #533.465
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #533.676
        jmp       ..B2.2        # Prob 100%                     #533.676
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #533.546
        lea       128(%rsp,%r12,4), %rsi                        #533.546
..___tag_value___svml_log10f8_ha_l9.40:
#       __svml_slog10_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog10_ha_cout_rare_internal           #533.546
..___tag_value___svml_log10f8_ha_l9.41:
        jmp       ..B2.8        # Prob 100%                     #533.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log10f8_ha_l9,@function
	.size	__svml_log10f8_ha_l9,.-__svml_log10f8_ha_l9
..LN__svml_log10f8_ha_l9.1:
	.data
# -- End  __svml_log10f8_ha_l9
	.text
.L_2__routine_start___svml_log10f4_ha_l9_2:
# -- Begin  __svml_log10f4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log10f4_ha_l9
# --- __svml_log10f4_ha_l9(__m128)
__svml_log10f4_ha_l9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #541.1
        .byte     15                                            #616.540
        .byte     30                                            #616.540
        .byte     250                                           #616.540
	.cfi_startproc
..___tag_value___svml_log10f4_ha_l9.43:
..L44:
                                                         #541.1
        pushq     %rbp                                          #541.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #541.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #541.1
        subq      $192, %rsp                                    #541.1
        vmovaps   %xmm0, %xmm2                                  #541.1
        vandps    1280+__svml_slog10_ha_data_internal(%rip), %xmm2, %xmm3 #576.19
        lea       -136944+__svml_slog10_ha_data_internal(%rip), %r8 #598.61
        vorps     1344+__svml_slog10_ha_data_internal(%rip), %xmm3, %xmm1 #577.19
        vpsrld    $23, %xmm2, %xmm6                             #582.19
        vrcpps    %xmm1, %xmm4                                  #580.17
        vcvtdq2ps %xmm6, %xmm0                                  #588.18
        vcmpltps  1408+__svml_slog10_ha_data_internal(%rip), %xmm2, %xmm7 #583.18
        vcmpnleps 1472+__svml_slog10_ha_data_internal(%rip), %xmm2, %xmm8 #584.18
        vmovups   1600+__svml_slog10_ha_data_internal(%rip), %xmm10 #585.44
        vorps     %xmm8, %xmm7, %xmm9                           #589.18
        vmulps    %xmm10, %xmm4, %xmm5                          #586.17
        vmovmskps %xmm9, %eax                                   #591.44
        vmovups   1088+__svml_slog10_ha_data_internal(%rip), %xmm9 #604.54
        vroundps  $0, %xmm5, %xmm5                              #587.17
        vpsrld    $13, %xmm5, %xmm11                            #595.18
        vmovd     %xmm11, %edx                                  #598.183
        vfmsub213ps %xmm10, %xmm5, %xmm1                        #593.13
        vpextrd   $1, %xmm11, %ecx                              #598.396
        vpextrd   $2, %xmm11, %esi                              #598.612
        vpextrd   $3, %xmm11, %edi                              #598.828
        movslq    %edx, %rdx                                    #598.61
        movslq    %ecx, %rcx                                    #598.274
        movslq    %esi, %rsi                                    #598.490
        movslq    %edi, %rdi                                    #598.706
        vmovq     (%r8,%rdx), %xmm12                            #598.61
        vmovq     (%r8,%rcx), %xmm13                            #598.274
        vmovq     (%r8,%rsi), %xmm14                            #598.490
        vmovq     (%r8,%rdi), %xmm15                            #598.706
        vunpcklps %xmm13, %xmm12, %xmm3                         #598.875
        vunpcklps %xmm15, %xmm14, %xmm4                         #598.910
        vmovlhps  %xmm4, %xmm3, %xmm6                           #598.950
        vfmadd231ps 1664+__svml_slog10_ha_data_internal(%rip), %xmm0, %xmm6 #602.13
        vfmadd213ps 1152+__svml_slog10_ha_data_internal(%rip), %xmm1, %xmm9 #607.14
        vshufps   $238, %xmm4, %xmm3, %xmm5                     #598.994
        vfmadd132ps 1728+__svml_slog10_ha_data_internal(%rip), %xmm5, %xmm0 #603.13
        vaddps    %xmm6, %xmm1, %xmm10                          #609.17
        vfmadd213ps 1216+__svml_slog10_ha_data_internal(%rip), %xmm1, %xmm9 #608.13
        vsubps    %xmm6, %xmm10, %xmm7                          #610.13
        vfmadd213ps %xmm0, %xmm1, %xmm9                         #612.13
        vsubps    %xmm7, %xmm1, %xmm8                           #611.13
        vaddps    %xmm9, %xmm8, %xmm0                           #613.13
        vaddps    %xmm0, %xmm10, %xmm0                          #614.14
        testl     %eax, %eax                                    #616.52
        jne       ..B3.3        # Prob 5%                       #616.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm2
..B3.2:                         # Preds ..B3.3 ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #619.12
        popq      %rbp                                          #619.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #619.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm2, 64(%rsp)                               #616.193
        vmovups   %xmm0, 128(%rsp)                              #616.264
        je        ..B3.2        # Prob 95%                      #616.368
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #616.448
        movq      %r12, 8(%rsp)                                 #616.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #616.448
        movq      %r13, (%rsp)                                  #616.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #616.448
                                # LOE rbx r12 r14 r15 r13d
..B3.7:                         # Preds ..B3.8 ..B3.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #616.511
        jc        ..B3.10       # Prob 5%                       #616.511
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #616.464
        cmpl      $4, %r12d                                     #616.459
        jl        ..B3.7        # Prob 82%                      #616.459
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #616.667
        jmp       ..B3.2        # Prob 100%                     #616.667
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #616.540
        lea       128(%rsp,%r12,4), %rsi                        #616.540
..___tag_value___svml_log10f4_ha_l9.61:
#       __svml_slog10_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog10_ha_cout_rare_internal           #616.540
..___tag_value___svml_log10f4_ha_l9.62:
        jmp       ..B3.8        # Prob 100%                     #616.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log10f4_ha_l9,@function
	.size	__svml_log10f4_ha_l9,.-__svml_log10f4_ha_l9
..LN__svml_log10f4_ha_l9.2:
	.data
# -- End  __svml_log10f4_ha_l9
	.text
.L_2__routine_start___svml_log10f4_ha_ex_3:
# -- Begin  __svml_log10f4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log10f4_ha_ex
# --- __svml_log10f4_ha_ex(__m128)
__svml_log10f4_ha_ex:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #624.1
        .byte     15                                            #699.540
        .byte     30                                            #699.540
        .byte     250                                           #699.540
	.cfi_startproc
..___tag_value___svml_log10f4_ha_ex.64:
..L65:
                                                         #624.1
        pushq     %rbp                                          #624.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #624.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #624.1
        subq      $192, %rsp                                    #624.1
        movaps    %xmm0, %xmm3                                  #624.1
        movups    1280+__svml_slog10_ha_data_internal(%rip), %xmm2 #659.19
        movaps    %xmm3, %xmm5                                  #665.17
        andps     %xmm3, %xmm2                                  #659.19
        psrld     $23, %xmm5                                    #665.17
        orps      1344+__svml_slog10_ha_data_internal(%rip), %xmm2 #660.19
        movaps    %xmm3, %xmm7                                  #666.18
        rcpps     %xmm2, %xmm12                                 #663.17
        cvtdq2ps  %xmm5, %xmm0                                  #671.18
        cmpltps   1408+__svml_slog10_ha_data_internal(%rip), %xmm7 #666.18
        movups    1600+__svml_slog10_ha_data_internal(%rip), %xmm8 #668.44
        movaps    %xmm3, %xmm6                                  #667.18
        mulps     %xmm8, %xmm12                                 #669.17
        lea       -136944+__svml_slog10_ha_data_internal(%rip), %r8 #681.61
        cmpnleps  1472+__svml_slog10_ha_data_internal(%rip), %xmm6 #667.18
        movups    .L_2il0floatpacket.20(%rip), %xmm4            #670.105
        orps      %xmm6, %xmm7                                  #672.18
        movups    1536+__svml_slog10_ha_data_internal(%rip), %xmm9 #675.49
        addps     %xmm4, %xmm12                                 #670.162
        movmskps  %xmm7, %eax                                   #674.44
        andps     %xmm2, %xmm9                                  #676.118
        subps     %xmm4, %xmm12                                 #670.212
        subps     %xmm9, %xmm2                                  #676.172
        mulps     %xmm12, %xmm9                                 #676.225
        mulps     %xmm12, %xmm2                                 #676.276
        subps     %xmm8, %xmm9                                  #676.322
        psrld     $13, %xmm12                                   #678.16
        addps     %xmm9, %xmm2                                  #676.365
        movd      %xmm12, %edx                                  #681.183
        pshufd    $1, %xmm12, %xmm10                            #681.412
        movd      %xmm10, %ecx                                  #681.394
        movaps    %xmm2, %xmm10                                 #694.13
        pshufd    $2, %xmm12, %xmm11                            #681.663
        pshufd    $3, %xmm12, %xmm13                            #681.914
        movd      %xmm11, %esi                                  #681.645
        movd      %xmm13, %edi                                  #681.896
        movups    1088+__svml_slog10_ha_data_internal(%rip), %xmm9 #687.54
        movups    1664+__svml_slog10_ha_data_internal(%rip), %xmm5 #685.25
        movslq    %edx, %rdx                                    #681.61
        movslq    %ecx, %rcx                                    #681.272
        mulps     %xmm2, %xmm9                                  #690.26
        mulps     %xmm0, %xmm5                                  #685.25
        addps     1152+__svml_slog10_ha_data_internal(%rip), %xmm9 #690.14
        movslq    %esi, %rsi                                    #681.523
        movslq    %edi, %rdi                                    #681.774
        movq      (%r8,%rdx), %xmm1                             #681.61
        movq      (%r8,%rcx), %xmm14                            #681.272
        movq      (%r8,%rsi), %xmm4                             #681.523
        movq      (%r8,%rdi), %xmm15                            #681.774
        unpcklps  %xmm14, %xmm1                                 #681.978
        unpcklps  %xmm15, %xmm4                                 #681.1013
        movaps    %xmm1, %xmm7                                  #681.1053
        movups    1728+__svml_slog10_ha_data_internal(%rip), %xmm6 #684.44
        movlhps   %xmm4, %xmm7                                  #681.1053
        mulps     %xmm6, %xmm0                                  #686.25
        addps     %xmm5, %xmm7                                  #685.13
        mulps     %xmm2, %xmm9                                  #691.25
        shufps    $238, %xmm4, %xmm1                            #681.1097
        addps     1216+__svml_slog10_ha_data_internal(%rip), %xmm9 #691.13
        addps     %xmm0, %xmm1                                  #686.13
        movaps    %xmm2, %xmm0                                  #692.17
        mulps     %xmm9, %xmm2                                  #695.25
        addps     %xmm7, %xmm0                                  #692.17
        movaps    %xmm0, %xmm8                                  #693.13
        addps     %xmm2, %xmm1                                  #695.13
        subps     %xmm7, %xmm8                                  #693.13
        subps     %xmm8, %xmm10                                 #694.13
        addps     %xmm1, %xmm10                                 #696.13
        addps     %xmm10, %xmm0                                 #697.14
        testl     %eax, %eax                                    #699.52
        jne       ..B4.3        # Prob 5%                       #699.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm3
..B4.2:                         # Preds ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #702.12
        popq      %rbp                                          #702.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #702.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm3, 64(%rsp)                               #699.193
        movups    %xmm0, 128(%rsp)                              #699.264
                                # LOE rbx r12 r13 r14 r15 eax
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #699.448
        movq      %r12, 8(%rsp)                                 #699.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #699.448
        movq      %r13, (%rsp)                                  #699.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #699.448
                                # LOE rbx r12 r14 r15 r13d
..B4.7:                         # Preds ..B4.8 ..B4.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #699.511
        jc        ..B4.10       # Prob 5%                       #699.511
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #699.464
        cmpl      $4, %r12d                                     #699.459
        jl        ..B4.7        # Prob 82%                      #699.459
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #699.667
        jmp       ..B4.2        # Prob 100%                     #699.667
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #699.540
        lea       128(%rsp,%r12,4), %rsi                        #699.540
..___tag_value___svml_log10f4_ha_ex.82:
#       __svml_slog10_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog10_ha_cout_rare_internal           #699.540
..___tag_value___svml_log10f4_ha_ex.83:
        jmp       ..B4.8        # Prob 100%                     #699.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log10f4_ha_ex,@function
	.size	__svml_log10f4_ha_ex,.-__svml_log10f4_ha_ex
..LN__svml_log10f4_ha_ex.3:
	.data
# -- End  __svml_log10f4_ha_ex
	.text
.L_2__routine_start___svml_log10f16_ha_z0_4:
# -- Begin  __svml_log10f16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log10f16_ha_z0
# --- __svml_log10f16_ha_z0(__m512)
__svml_log10f16_ha_z0:
# parameter 1: %zmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #707.1
        .byte     15                                            #783.12
        .byte     30                                            #783.12
        .byte     250                                           #783.12
	.cfi_startproc
..___tag_value___svml_log10f16_ha_z0.85:
..L86:
                                                         #707.1
        vmovaps   %zmm0, %zmm2                                  #707.1
        vgetmantps $10, {sae}, %zmm2, %zmm4                     #745.33
        vmovups   256+__svml_slog10_ha_data_internal_avx512(%rip), %zmm1 #752.24
        vgetexpps {sae}, %zmm2, %zmm7                           #746.30
        vmovups   __svml_slog10_ha_data_internal_avx512(%rip), %zmm0 #755.298
        vmovups   320+__svml_slog10_ha_data_internal_avx512(%rip), %zmm12 #762.66
        vmovups   640+__svml_slog10_ha_data_internal_avx512(%rip), %zmm9 #758.58
        vmovups   448+__svml_slog10_ha_data_internal_avx512(%rip), %zmm13 #766.66
        vmovups   512+__svml_slog10_ha_data_internal_avx512(%rip), %zmm15 #767.67
        vmovups   128+__svml_slog10_ha_data_internal_avx512(%rip), %zmm11 #756.298
        vmovups   704+__svml_slog10_ha_data_internal_avx512(%rip), %zmm10 #760.58
        vrcp14ps  %zmm4, %zmm3                                  #748.29
        vfpclassps $94, %zmm2, %k1                              #747.36
        vrndscaleps $88, {sae}, %zmm3, %zmm5                    #749.29
        vgetexpps {sae}, %zmm5, %zmm8                           #750.31
        vfmsub231ps {rn-sae}, %zmm5, %zmm4, %zmm1               #752.24
        vmovups   384+__svml_slog10_ha_data_internal_avx512(%rip), %zmm4 #763.66
        vpsrld    $18, %zmm5, %zmm6                             #753.50
        vsubps    {rn-sae}, %zmm8, %zmm7, %zmm3                 #757.30
        vfmadd231ps {rn-sae}, %zmm1, %zmm12, %zmm4              #764.24
        vmovups   576+__svml_slog10_ha_data_internal_avx512(%rip), %zmm12 #769.67
        vfmadd231ps {rn-sae}, %zmm1, %zmm13, %zmm15             #768.26
        vmulps    {rn-sae}, %zmm1, %zmm1, %zmm14                #765.27
        vpermt2ps 64+__svml_slog10_ha_data_internal_avx512(%rip), %zmm6, %zmm0 #755.298
        vpermt2ps 192+__svml_slog10_ha_data_internal_avx512(%rip), %zmm6, %zmm11 #756.298
        vfmadd213ps {rn-sae}, %zmm15, %zmm14, %zmm4             #771.24
        vfmadd231ps {rn-sae}, %zmm3, %zmm9, %zmm0               #759.25
        vfmadd213ps {rn-sae}, %zmm11, %zmm10, %zmm3             #761.25
        vmovaps   %zmm12, %zmm5                                 #770.27
        vfmadd213ps {rn-sae}, %zmm0, %zmm1, %zmm5               #770.27
        vfmadd213ps {rn-sae}, %zmm3, %zmm1, %zmm4               #774.24
        vsubps    {rn-sae}, %zmm0, %zmm5, %zmm0                 #772.27
        vxorps    %zmm0, %zmm0, %zmm0{%k1}                      #773.25
        vfmsub213ps {rn-sae}, %zmm0, %zmm12, %zmm1              #775.25
        vaddps    {rn-sae}, %zmm1, %zmm4, %zmm1                 #777.26
        vaddps    {rn-sae}, %zmm1, %zmm5, %zmm0                 #778.28
        ret                                                     #783.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_log10f16_ha_z0,@function
	.size	__svml_log10f16_ha_z0,.-__svml_log10f16_ha_z0
..LN__svml_log10f16_ha_z0.4:
	.data
# -- End  __svml_log10f16_ha_z0
	.text
.L_2__routine_start___svml_log10f8_ha_e9_5:
# -- Begin  __svml_log10f8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log10f8_ha_e9
# --- __svml_log10f8_ha_e9(__m256)
__svml_log10f8_ha_e9:
# parameter 1: %ymm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1098.1
        .byte     15                                            #1173.546
        .byte     30                                            #1173.546
        .byte     250                                           #1173.546
	.cfi_startproc
..___tag_value___svml_log10f8_ha_e9.88:
..L89:
                                                         #1098.1
        pushq     %rbp                                          #1098.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1098.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1098.1
        subq      $192, %rsp                                    #1098.1
        lea       -136944+__svml_slog10_ha_data_internal(%rip), %rax #1155.42
        vmovaps   %ymm0, %ymm5                                  #1098.1
        vandps    1280+__svml_slog10_ha_data_internal(%rip), %ymm5, %ymm6 #1133.19
        vorps     1344+__svml_slog10_ha_data_internal(%rip), %ymm6, %ymm4 #1134.19
        vrcpps    %ymm4, %ymm7                                  #1137.17
        vmovups   1600+__svml_slog10_ha_data_internal(%rip), %ymm3 #1142.47
        vmulps    %ymm3, %ymm7, %ymm8                           #1143.17
        vpxor     %xmm7, %xmm7, %xmm7                           #1148.62
        vroundps  $0, %ymm8, %ymm2                              #1144.17
        vpsrld    $23, %xmm5, %xmm9                             #1139.23
        vextractf128 $1, %ymm5, %xmm10                          #1138.113
        vcmplt_oqps 1408+__svml_slog10_ha_data_internal(%rip), %ymm5, %ymm13 #1140.18
        vcmpnle_uqps 1472+__svml_slog10_ha_data_internal(%rip), %ymm5, %ymm14 #1141.18
        vpsrld    $23, %xmm10, %xmm11                           #1139.80
        vorps     %ymm14, %ymm13, %ymm15                        #1146.18
        vinsertf128 $1, %xmm11, %ymm9, %ymm12                   #1145.38
        vandps    1536+__svml_slog10_ha_data_internal(%rip), %ymm4, %ymm9 #1150.118
        vsubps    %ymm9, %ymm4, %ymm4                           #1150.175
        vcvtdq2ps %ymm12, %ymm1                                 #1145.18
        vmulps    %ymm9, %ymm2, %ymm10                          #1150.231
        vmulps    %ymm4, %ymm2, %ymm11                          #1150.285
        vsubps    %ymm3, %ymm10, %ymm3                          #1150.334
        vpsrld    $13, %xmm2, %xmm13                            #1152.22
        vmovd     %xmm13, %ecx                                  #1155.164
        vextractf128 $1, %ymm2, %xmm12                          #1151.114
        movslq    %ecx, %rcx                                    #1155.42
        vpsrld    $13, %xmm12, %xmm14                           #1152.81
        vmovd     %xmm14, %r9d                                  #1155.973
        vpextrd   $1, %xmm13, %esi                              #1155.364
        vpextrd   $3, %xmm13, %r8d                              #1155.770
        movslq    %esi, %rsi                                    #1155.242
        movslq    %r8d, %r8                                     #1155.648
        vpextrd   $2, %xmm13, %edi                              #1155.567
        vmovq     (%rax,%rcx), %xmm2                            #1155.42
        vpextrd   $1, %xmm14, %r10d                             #1155.1173
        vpextrd   $2, %xmm14, %r11d                             #1155.1376
        vpextrd   $3, %xmm14, %ecx                              #1155.1579
        movslq    %edi, %rdi                                    #1155.445
        movslq    %r9d, %r9                                     #1155.851
        movslq    %r10d, %r10                                   #1155.1051
        movslq    %r11d, %r11                                   #1155.1254
        movslq    %ecx, %rcx                                    #1155.1457
        vmovq     (%rax,%rsi), %xmm4                            #1155.242
        vmovq     (%rax,%r10), %xmm9                            #1155.1051
        vextractf128 $1, %ymm15, %xmm0                          #1147.117
        vmovq     (%rax,%rcx), %xmm10                           #1155.1457
        vunpcklps %xmm10, %xmm9, %xmm14                         #1155.1811
        vpackssdw %xmm0, %xmm15, %xmm6                          #1148.79
        vpacksswb %xmm7, %xmm6, %xmm8                           #1148.62
        vmovq     (%rax,%r8), %xmm6                             #1155.648
        vunpcklps %xmm6, %xmm4, %xmm13                          #1155.1701
        vmulps    1664+__svml_slog10_ha_data_internal(%rip), %ymm1, %ymm4 #1159.28
        vmulps    1728+__svml_slog10_ha_data_internal(%rip), %ymm1, %ymm1 #1160.28
        vmovq     (%rax,%r9), %xmm7                             #1155.851
        vaddps    %ymm3, %ymm11, %ymm0                          #1150.380
        vmovq     (%rax,%rdi), %xmm3                            #1155.445
        vunpcklps %xmm3, %xmm2, %xmm11                          #1155.1646
        vpmovmskb %xmm8, %edx                                   #1148.43
        vmovq     (%rax,%r11), %xmm8                            #1155.1254
        vunpcklps %xmm8, %xmm7, %xmm12                          #1155.1756
        vinsertf128 $1, %xmm12, %ymm11, %ymm15                  #1155.1874
        vinsertf128 $1, %xmm14, %ymm13, %ymm2                   #1155.1982
        vunpckhps %ymm2, %ymm15, %ymm6                          #1155.2147
        vaddps    %ymm1, %ymm6, %ymm8                           #1160.13
        vmulps    1088+__svml_slog10_ha_data_internal(%rip), %ymm0, %ymm1 #1164.29
        vaddps    1152+__svml_slog10_ha_data_internal(%rip), %ymm1, %ymm6 #1164.14
        vunpcklps %ymm2, %ymm15, %ymm3                          #1155.2073
        vaddps    %ymm4, %ymm3, %ymm7                           #1159.13
        vmulps    %ymm6, %ymm0, %ymm2                           #1165.28
        vaddps    %ymm7, %ymm0, %ymm11                          #1166.17
        vaddps    1216+__svml_slog10_ha_data_internal(%rip), %ymm2, %ymm4 #1165.13
        vsubps    %ymm7, %ymm11, %ymm3                          #1167.13
        vsubps    %ymm3, %ymm0, %ymm9                           #1168.13
        vmulps    %ymm4, %ymm0, %ymm0                           #1169.28
        vaddps    %ymm0, %ymm8, %ymm10                          #1169.13
        vaddps    %ymm10, %ymm9, %ymm12                         #1170.13
        vaddps    %ymm12, %ymm11, %ymm0                         #1171.14
        testb     %dl, %dl                                      #1173.52
        jne       ..B6.3        # Prob 5%                       #1173.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm5
..B6.2:                         # Preds ..B6.3 ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1176.12
        popq      %rbp                                          #1176.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1176.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm5, 64(%rsp)                               #1173.196
        vmovups   %ymm0, 128(%rsp)                              #1173.270
        testl     %edx, %edx                                    #1173.374
        je        ..B6.2        # Prob 95%                      #1173.374
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1173.454
                                # LOE rbx r12 r13 r14 r15 eax edx
..B6.13:                        # Preds ..B6.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1173.517
        jc        ..B6.10       # Prob 5%                       #1173.517
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1173.470
        cmpl      $8, %r12d                                     #1173.465
        jl        ..B6.7        # Prob 82%                      #1173.465
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #1173.676
        jmp       ..B6.2        # Prob 100%                     #1173.676
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1173.546
        lea       128(%rsp,%r12,4), %rsi                        #1173.546
..___tag_value___svml_log10f8_ha_e9.106:
#       __svml_slog10_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog10_ha_cout_rare_internal           #1173.546
..___tag_value___svml_log10f8_ha_e9.107:
        jmp       ..B6.8        # Prob 100%                     #1173.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log10f8_ha_e9,@function
	.size	__svml_log10f8_ha_e9,.-__svml_log10f8_ha_e9
..LN__svml_log10f8_ha_e9.5:
	.data
# -- End  __svml_log10f8_ha_e9
	.text
.L_2__routine_start___svml_slog10_ha_cout_rare_internal_6:
# -- Begin  __svml_slog10_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_slog10_ha_cout_rare_internal
	.globl __svml_slog10_ha_cout_rare_internal
# --- __svml_slog10_ha_cout_rare_internal(const float *, float *)
__svml_slog10_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1004.1
        .byte     15                                            #1085.19
        .byte     30                                            #1085.19
        .byte     250                                           #1085.19
	.cfi_startproc
..___tag_value___svml_slog10_ha_cout_rare_internal.109:
..L110:
                                                        #1004.1
        xorl      %eax, %eax                                    #1015.14
        movzwl    2(%rdi), %edx                                 #1016.35
        andl      $32640, %edx                                  #1016.35
        cmpl      $32640, %edx                                  #1016.61
        je        ..B7.12       # Prob 16%                      #1016.61
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B7.2:                         # Preds ..B7.1
                                # Execution count [8.40e-01]
        movss     (%rdi), %xmm2                                 #1018.19
        xorl      %ecx, %ecx                                    #1019.13
        movss     %xmm2, -16(%rsp)                              #1018.13
        movzwl    -14(%rsp), %edx                               #1020.39
        testl     $32640, %edx                                  #1020.39
        jne       ..B7.4        # Prob 50%                      #1020.61
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm2
..B7.3:                         # Preds ..B7.2
                                # Execution count [4.20e-01]
        mulss     .L_2il0floatpacket.93(%rip), %xmm2            #1022.17
        movl      $-40, %ecx                                    #1023.17
        movss     %xmm2, -16(%rsp)                              #1022.17
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm2
..B7.4:                         # Preds ..B7.3 ..B7.2
                                # Execution count [8.40e-01]
        pxor      %xmm0, %xmm0                                  #1025.13
        comiss    %xmm0, %xmm2                                  #1025.38
        jbe       ..B7.8        # Prob 50%                      #1025.38
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm0 xmm2
..B7.5:                         # Preds ..B7.4
                                # Execution count [4.20e-01]
        movaps    %xmm2, %xmm1                                  #1027.34
        subss     .L_2il0floatpacket.109(%rip), %xmm1           #1027.34
        movss     %xmm1, -20(%rsp)                              #1028.17
        andb      $127, -17(%rsp)                               #1029.38
        movss     -20(%rsp), %xmm0                              #1030.22
        comiss    .L_2il0floatpacket.94(%rip), %xmm0            #1030.46
        jbe       ..B7.7        # Prob 50%                      #1030.46
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm1 xmm2
..B7.6:                         # Preds ..B7.5
                                # Execution count [2.10e-01]
        movss     %xmm2, -20(%rsp)                              #1036.21
        pxor      %xmm8, %xmm8                                  #1033.33
        movzwl    -18(%rsp), %edi                               #1037.42
        lea       _vmldLgHATab(%rip), %r10                      #1040.42
        andl      $-32641, %edi                                 #1037.42
        addl      $16256, %edi                                  #1037.42
        movw      %di, -18(%rsp)                                #1037.42
        movss     -20(%rsp), %xmm3                              #1038.37
        movaps    %xmm3, %xmm0                                  #1038.57
        movss     .L_2il0floatpacket.96(%rip), %xmm2            #1045.57
        movaps    %xmm2, %xmm1                                  #1045.57
        addss     .L_2il0floatpacket.95(%rip), %xmm0            #1038.57
        addss     %xmm3, %xmm1                                  #1045.57
        movss     %xmm0, -24(%rsp)                              #1038.21
        movl      -24(%rsp), %r8d                               #1039.46
        movss     %xmm1, -24(%rsp)                              #1045.21
        andl      $127, %r8d                                    #1039.73
        movss     -24(%rsp), %xmm9                              #1046.37
        movss     .L_2il0floatpacket.105(%rip), %xmm6           #1051.124
        subss     %xmm2, %xmm9                                  #1046.60
        movzwl    -14(%rsp), %edx                               #1032.48
        lea       (%r8,%r8,2), %r9d                             #1040.58
        movss     (%r10,%r9,4), %xmm7                           #1040.42
        andl      $32640, %edx                                  #1032.48
        shrl      $7, %edx                                      #1032.48
        subss     %xmm9, %xmm3                                  #1047.41
        mulss     %xmm7, %xmm9                                  #1048.42
        mulss     %xmm3, %xmm7                                  #1049.42
        subss     .L_2il0floatpacket.97(%rip), %xmm9            #1048.65
        movaps    %xmm9, %xmm4                                  #1050.41
        lea       -127(%rcx,%rdx), %ecx                         #1032.21
        cvtsi2ss  %ecx, %xmm8                                   #1033.33
        addss     %xmm7, %xmm4                                  #1050.41
        mulss     %xmm4, %xmm6                                  #1051.124
        movss     .L_2il0floatpacket.106(%rip), %xmm10          #1034.61
        mulss     %xmm8, %xmm10                                 #1034.61
        addss     .L_2il0floatpacket.104(%rip), %xmm6           #1051.142
        addss     4(%r10,%r9,4), %xmm10                         #1043.47
        mulss     %xmm4, %xmm6                                  #1051.163
        addss     %xmm9, %xmm10                                 #1052.46
        addss     .L_2il0floatpacket.103(%rip), %xmm6           #1051.183
        mulss     %xmm4, %xmm6                                  #1051.204
        movss     .L_2il0floatpacket.107(%rip), %xmm5           #1035.61
        mulss     %xmm5, %xmm8                                  #1035.61
        addss     .L_2il0floatpacket.102(%rip), %xmm6           #1051.224
        addss     8(%r10,%r9,4), %xmm8                          #1044.47
        mulss     %xmm4, %xmm6                                  #1051.245
        addss     .L_2il0floatpacket.101(%rip), %xmm6           #1051.265
        mulss     %xmm4, %xmm6                                  #1051.286
        addss     .L_2il0floatpacket.100(%rip), %xmm6           #1051.306
        mulss     %xmm4, %xmm6                                  #1051.327
        addss     .L_2il0floatpacket.99(%rip), %xmm6            #1051.347
        mulss     %xmm4, %xmm6                                  #1051.368
        addss     .L_2il0floatpacket.98(%rip), %xmm6            #1051.388
        mulss     %xmm6, %xmm9                                  #1055.49
        mulss     %xmm7, %xmm6                                  #1053.49
        addss     %xmm6, %xmm8                                  #1053.49
        addss     %xmm7, %xmm8                                  #1054.46
        addss     %xmm8, %xmm9                                  #1055.49
        addss     %xmm9, %xmm10                                 #1056.47
        movss     %xmm10, (%rsi)                                #1056.23
        ret                                                     #1056.23
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.7:                         # Preds ..B7.5
                                # Execution count [2.10e-01]
        movss     .L_2il0floatpacket.97(%rip), %xmm0            #1060.52
        mulss     %xmm0, %xmm1                                  #1060.52
        movss     .L_2il0floatpacket.105(%rip), %xmm2           #1061.105
        mulss     %xmm1, %xmm2                                  #1061.124
        addss     .L_2il0floatpacket.104(%rip), %xmm2           #1061.142
        mulss     %xmm1, %xmm2                                  #1061.163
        addss     .L_2il0floatpacket.103(%rip), %xmm2           #1061.183
        mulss     %xmm1, %xmm2                                  #1061.204
        addss     .L_2il0floatpacket.102(%rip), %xmm2           #1061.224
        mulss     %xmm1, %xmm2                                  #1061.245
        addss     .L_2il0floatpacket.101(%rip), %xmm2           #1061.265
        mulss     %xmm1, %xmm2                                  #1061.286
        addss     .L_2il0floatpacket.100(%rip), %xmm2           #1061.306
        mulss     %xmm1, %xmm2                                  #1061.327
        addss     .L_2il0floatpacket.99(%rip), %xmm2            #1061.347
        mulss     %xmm1, %xmm2                                  #1061.368
        addss     .L_2il0floatpacket.98(%rip), %xmm2            #1061.388
        mulss     %xmm1, %xmm2                                  #1062.38
        addss     %xmm1, %xmm2                                  #1063.38
        movss     %xmm2, (%rsi)                                 #1064.23
        ret                                                     #1064.23
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.8:                         # Preds ..B7.4
                                # Execution count [4.20e-01]
        ucomiss   %xmm0, %xmm2                                  #1069.43
        jp        ..B7.9        # Prob 0%                       #1069.43
        je        ..B7.11       # Prob 16%                      #1069.43
                                # LOE rbx rbp rsi r12 r13 r14 r15 xmm0
..B7.9:                         # Preds ..B7.8
                                # Execution count [3.53e-01]
        divss     %xmm0, %xmm0                                  #1076.89
        movss     %xmm0, (%rsi)                                 #1076.23
        movl      $1, %eax                                      #1077.21
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.10:                        # Preds ..B7.9
                                # Execution count [1.00e+00]
        ret                                                     #1093.16
                                # LOE
..B7.11:                        # Preds ..B7.8
                                # Execution count [6.72e-02]: Infreq
        movss     .L_2il0floatpacket.108(%rip), %xmm1           #1071.53
        movl      $2, %eax                                      #1072.21
        divss     %xmm0, %xmm1                                  #1071.90
        movss     %xmm1, (%rsi)                                 #1071.23
        ret                                                     #1071.23
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.12:                        # Preds ..B7.1
                                # Execution count [1.60e-01]: Infreq
        movb      3(%rdi), %dl                                  #1083.39
        andb      $-128, %dl                                    #1083.39
        cmpb      $-128, %dl                                    #1083.61
        je        ..B7.14       # Prob 16%                      #1083.61
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B7.13:                        # Preds ..B7.14 ..B7.12
                                # Execution count [1.47e-01]: Infreq
        movss     (%rdi), %xmm0                                 #1090.26
        mulss     %xmm0, %xmm0                                  #1090.33
        movss     %xmm0, (%rsi)                                 #1090.19
        ret                                                     #1090.19
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.14:                        # Preds ..B7.12
                                # Execution count [2.56e-02]: Infreq
        testl     $8388607, (%rdi)                              #1083.88
        jne       ..B7.13       # Prob 50%                      #1083.117
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B7.15:                        # Preds ..B7.14
                                # Execution count [1.28e-02]: Infreq
        movl      $1, %eax                                      #1086.17
        pxor      %xmm1, %xmm1                                  #1085.85
        pxor      %xmm0, %xmm0                                  #1085.85
        divss     %xmm0, %xmm1                                  #1085.85
        movss     %xmm1, (%rsi)                                 #1085.19
        ret                                                     #1085.19
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_slog10_ha_cout_rare_internal,@function
	.size	__svml_slog10_ha_cout_rare_internal,.-__svml_slog10_ha_cout_rare_internal
..LN__svml_slog10_ha_cout_rare_internal.6:
	.data
# -- End  __svml_slog10_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_slog10_ha_data_internal_avx512
	.globl __svml_slog10_ha_data_internal_avx512
__svml_slog10_ha_data_internal_avx512:
	.long	1050288128
	.long	1049839616
	.long	1049405440
	.long	1048981504
	.long	1048567808
	.long	1047769088
	.long	1046990848
	.long	1046233088
	.long	1045495808
	.long	1044779008
	.long	1044074496
	.long	1043390464
	.long	1042718720
	.long	1042063360
	.long	1041424384
	.long	1040797696
	.long	1040179200
	.long	1038974976
	.long	1037803520
	.long	1036648448
	.long	1035509760
	.long	1034403840
	.long	1033314304
	.long	1032241152
	.long	1030586368
	.long	1028521984
	.long	1026490368
	.long	1024507904
	.long	1021673472
	.long	1017839616
	.long	1013055488
	.long	1004535808
	.long	916096252
	.long	922116432
	.long	3080225825
	.long	937573077
	.long	3035959522
	.long	907149878
	.long	932083293
	.long	937852153
	.long	932873468
	.long	3083585687
	.long	923506544
	.long	3080111720
	.long	922618327
	.long	929046259
	.long	3073359178
	.long	3075170456
	.long	3027570914
	.long	932146099
	.long	3086063431
	.long	3082882946
	.long	937977627
	.long	3064614024
	.long	3065085585
	.long	934503987
	.long	923757491
	.long	927716856
	.long	937794272
	.long	3074368796
	.long	929297206
	.long	3083361151
	.long	3065901602
	.long	912917177
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
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	3185469915
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	1041515222
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	3193854937
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	2990071104
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	1050288128
	.long	916096252
	.long	916096252
	.long	916096252
	.long	916096252
	.long	916096252
	.long	916096252
	.long	916096252
	.long	916096252
	.long	916096252
	.long	916096252
	.long	916096252
	.long	916096252
	.long	916096252
	.long	916096252
	.long	916096252
	.long	916096252
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
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
	.type	__svml_slog10_ha_data_internal_avx512,@object
	.size	__svml_slog10_ha_data_internal_avx512,960
	.align 64
	.hidden __svml_slog10_ha_data_internal
	.globl __svml_slog10_ha_data_internal
__svml_slog10_ha_data_internal:
	.long	3256334460
	.long	969394728
	.long	3256334974
	.long	969699354
	.long	3256335486
	.long	970041911
	.long	3256335982
	.long	968584717
	.long	3256336494
	.long	969519438
	.long	3256336990
	.long	968649168
	.long	3256337502
	.long	970165641
	.long	3256337998
	.long	969872020
	.long	3256338494
	.long	969862955
	.long	3256338990
	.long	970135976
	.long	3256339470
	.long	968591496
	.long	3256339966
	.long	969421411
	.long	3256340462
	.long	970526195
	.long	3256340942
	.long	969806353
	.long	3256341422
	.long	969356721
	.long	3256341902
	.long	969175014
	.long	3256342382
	.long	969258977
	.long	3256342862
	.long	969606381
	.long	3256343342
	.long	970215027
	.long	3256343806
	.long	968985590
	.long	3256344286
	.long	970110228
	.long	3256344750
	.long	969392519
	.long	3256345214
	.long	968927522
	.long	3256345678
	.long	968713169
	.long	3256346142
	.long	968747418
	.long	3256346606
	.long	969028253
	.long	3256347070
	.long	969553681
	.long	3256347534
	.long	970321733
	.long	3256347982
	.long	969233313
	.long	3256348446
	.long	970480802
	.long	3256348894
	.long	969867999
	.long	3256349342
	.long	969490179
	.long	3256349790
	.long	969345487
	.long	3256350238
	.long	969432093
	.long	3256350686
	.long	969748186
	.long	3256351566
	.long	968964541
	.long	3256352446
	.long	969077639
	.long	3256353326
	.long	970073736
	.long	3256354190
	.long	969842251
	.long	3256355054
	.long	970467209
	.long	3256355902
	.long	969838628
	.long	3256356750
	.long	970041115
	.long	3256357582
	.long	968965252
	.long	3256358414
	.long	968696192
	.long	3256359246
	.long	969222201
	.long	3256360078
	.long	970531798
	.long	3256360894
	.long	970516595
	.long	3256361694
	.long	969165599
	.long	3256362494
	.long	968565200
	.long	3256363294
	.long	968704863
	.long	3256364094
	.long	969574273
	.long	3256364878
	.long	969066179
	.long	3256365662
	.long	969267840
	.long	3256366446
	.long	970169567
	.long	3256367214
	.long	969664714
	.long	3256367982
	.long	969841134
	.long	3256368734
	.long	968592559
	.long	3256369502
	.long	970104362
	.long	3256370254
	.long	970173483
	.long	3256370990
	.long	968791339
	.long	3256371742
	.long	970143819
	.long	3256372478
	.long	970028365
	.long	3256373214
	.long	970534037
	.long	3256373934
	.long	969555746
	.long	3256374654
	.long	969182856
	.long	3256375374
	.long	969407729
	.long	3256376094
	.long	970222869
	.long	3256376798
	.long	969523769
	.long	3256377502
	.long	969400361
	.long	3256378206
	.long	969845561
	.long	3256378894
	.long	968755260
	.long	3256379598
	.long	970316935
	.long	3256380286
	.long	970329575
	.long	3256380958
	.long	968786598
	.long	3256381646
	.long	969875841
	.long	3256382318
	.long	969396649
	.long	3256382990
	.long	969439935
	.long	3256383662
	.long	969999571
	.long	3256384318
	.long	968972383
	.long	3256384990
	.long	970546758
	.long	3256385646
	.long	970522577
	.long	3256386286
	.long	968894127
	.long	3256386942
	.long	969850093
	.long	3256387582
	.long	969190651
	.long	3256388222
	.long	969007524
	.long	3256388862
	.long	969295375
	.long	3256389502
	.long	970048957
	.long	3256390126
	.long	969165958
	.long	3256390750
	.long	968738453
	.long	3256391374
	.long	968761452
	.long	3256391998
	.long	969230043
	.long	3256392622
	.long	970139398
	.long	3256393230
	.long	969387611
	.long	3256393838
	.long	969067159
	.long	3256394446
	.long	969173441
	.long	3256395054
	.long	969701929
	.long	3256395646
	.long	968551015
	.long	3256396254
	.long	969910618
	.long	3256396846
	.long	969582116
	.long	3256397438
	.long	969658411
	.long	3256398030
	.long	970135319
	.long	3256398606
	.long	968911569
	.long	3256399198
	.long	970177409
	.long	3256399774
	.long	969734540
	.long	3256400350
	.long	969676181
	.long	3256400926
	.long	969998457
	.long	3256401486
	.long	968600401
	.long	3256402062
	.long	969672558
	.long	3256402622
	.long	969016921
	.long	3256403182
	.long	968726993
	.long	3256403742
	.long	968799182
	.long	3256404302
	.long	969229944
	.long	3256404862
	.long	970015791
	.long	3256405406
	.long	969056131
	.long	3256405966
	.long	970541879
	.long	3256406510
	.long	970275391
	.long	3256407054
	.long	970350525
	.long	3256407582
	.long	968666884
	.long	3256408126
	.long	969415571
	.long	3256408670
	.long	970496282
	.long	3256409198
	.long	969808752
	.long	3256409726
	.long	969447067
	.long	3256410254
	.long	969408203
	.long	3256410782
	.long	969689179
	.long	3256411310
	.long	970287052
	.long	3256411822
	.long	969101770
	.long	3256412350
	.long	970324777
	.long	3256412862
	.long	969758945
	.long	3256413374
	.long	969498644
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
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	1071862019
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	3214137316
	.long	986960742
	.long	986960742
	.long	986960742
	.long	986960742
	.long	986960742
	.long	986960742
	.long	986960742
	.long	986960742
	.long	986960742
	.long	986960742
	.long	986960742
	.long	986960742
	.long	986960742
	.long	986960742
	.long	986960742
	.long	986960742
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
	.long	989855744
	.long	989855744
	.long	989855744
	.long	989855744
	.long	989855744
	.long	989855744
	.long	989855744
	.long	989855744
	.long	989855744
	.long	989855744
	.long	989855744
	.long	989855744
	.long	989855744
	.long	989855744
	.long	989855744
	.long	989855744
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	8388608
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	2139095039
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	4294967040
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1054736384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	1050288384
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	3058365952
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
	.long	1059760811
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
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	1031824308
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	3179872371
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	1031230231
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	3180460839
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	1035078550
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	3185471008
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	1041513701
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	3193854917
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1054759897
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	1036894503
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	3186945393
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	1041464766
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	3193833762
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1054760110
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	1050288283
	.long	2139095040
	.long	4286578688
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
	.long	1065353216
	.long	3212836864
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
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.type	__svml_slog10_ha_data_internal,@object
	.size	__svml_slog10_ha_data_internal,3136
	.align 32
_vmldLgHATab:
	.long	1121868800
	.long	0
	.long	0
	.long	1121641104
	.long	1004535808
	.long	912917177
	.long	1121413408
	.long	1013055488
	.long	3065901602
	.long	1121185712
	.long	1017839616
	.long	3083361151
	.long	1120958016
	.long	1021673472
	.long	929297206
	.long	1120844168
	.long	1023524864
	.long	3077496589
	.long	1120616472
	.long	1025499136
	.long	3070500046
	.long	1120388776
	.long	1027506176
	.long	912271551
	.long	1120274928
	.long	1028521984
	.long	927716856
	.long	1120047232
	.long	1030586368
	.long	923757491
	.long	1119933384
	.long	1031634944
	.long	3056752848
	.long	1119705688
	.long	1032775680
	.long	917029265
	.long	1119591840
	.long	1033314304
	.long	3065085585
	.long	1119364144
	.long	1034403840
	.long	3064614024
	.long	1119250296
	.long	1034954752
	.long	921091539
	.long	1119136448
	.long	1035513856
	.long	3057436454
	.long	1118908752
	.long	1036644352
	.long	922468856
	.long	1118794904
	.long	1037219840
	.long	3049155845
	.long	1118681056
	.long	1037799424
	.long	904301451
	.long	1118567208
	.long	1038385152
	.long	908617625
	.long	1118453360
	.long	1038977024
	.long	905362229
	.long	1118225664
	.long	1040179200
	.long	3027570914
	.long	1118111816
	.long	1040488448
	.long	882280038
	.long	1117997968
	.long	1040796672
	.long	911375775
	.long	1117884120
	.long	1041108480
	.long	904500572
	.long	1117770272
	.long	1041423872
	.long	3057579304
	.long	1117656424
	.long	1041742336
	.long	3053334705
	.long	1117542576
	.long	1042064384
	.long	3053389931
	.long	1117428728
	.long	1042390016
	.long	3051561465
	.long	1117314880
	.long	1042719232
	.long	3011187895
	.long	1117201032
	.long	1043052544
	.long	3059907089
	.long	1117087184
	.long	1043389440
	.long	3057005374
	.long	1116973336
	.long	1043729920
	.long	911932638
	.long	1116859488
	.long	1044075008
	.long	892958461
	.long	1116859488
	.long	1044075008
	.long	892958461
	.long	1116745640
	.long	1044424192
	.long	3048660547
	.long	1116631792
	.long	1044777472
	.long	3049032043
	.long	1116517944
	.long	1045134848
	.long	906867152
	.long	1116404096
	.long	1045496832
	.long	911484894
	.long	1116404096
	.long	1045496832
	.long	911484894
	.long	1116290248
	.long	1045863424
	.long	912580963
	.long	1116176400
	.long	1046235136
	.long	3058440244
	.long	1116062552
	.long	1046610944
	.long	895945194
	.long	1116062552
	.long	1046610944
	.long	895945194
	.long	1115948704
	.long	1046991872
	.long	904357324
	.long	1115834856
	.long	1047377920
	.long	902293870
	.long	1115721008
	.long	1047769088
	.long	907149878
	.long	1115721008
	.long	1047769088
	.long	907149878
	.long	1115529456
	.long	1048165888
	.long	3052029263
	.long	1115301760
	.long	1048567808
	.long	3035959522
	.long	1115301760
	.long	1048567808
	.long	3035959522
	.long	1115074064
	.long	1048775680
	.long	892998645
	.long	1115074064
	.long	1048775680
	.long	892998645
	.long	1114846368
	.long	1048982400
	.long	881767775
	.long	1114618672
	.long	1049192064
	.long	893839142
	.long	1114618672
	.long	1049192064
	.long	893839142
	.long	1114390976
	.long	1049404800
	.long	896498651
	.long	1114390976
	.long	1049404800
	.long	896498651
	.long	1114163280
	.long	1049620736
	.long	3033695903
	.long	1114163280
	.long	1049620736
	.long	3033695903
	.long	1113935584
	.long	1049839872
	.long	3029986056
	.long	1113935584
	.long	1049839872
	.long	3029986056
	.long	1113707888
	.long	1050062336
	.long	884671939
	.long	1113707888
	.long	1050062336
	.long	884671939
	.long	1113480192
	.long	1050288256
	.long	894707678
	.long	1050279936
	.long	964848148
	.long	1207959616
	.long	1174405120
	.long	1002438656
	.long	1400897536
	.long	0
	.long	1065353216
	.long	1121868800
	.long	3212771328
	.long	3079888218
	.long	870463078
	.long	2957202361
	.long	749987585
	.long	2838272395
	.long	631921661
	.long	2720751022
	.type	_vmldLgHATab,@object
	.size	_vmldLgHATab,848
	.align 16
.L_2il0floatpacket.20:
	.long	0x4b400000,0x4b400000,0x4b400000,0x4b400000
	.type	.L_2il0floatpacket.20,@object
	.size	.L_2il0floatpacket.20,16
	.align 4
.L_2il0floatpacket.93:
	.long	0x53800000
	.type	.L_2il0floatpacket.93,@object
	.size	.L_2il0floatpacket.93,4
	.align 4
.L_2il0floatpacket.94:
	.long	0x3bc00000
	.type	.L_2il0floatpacket.94,@object
	.size	.L_2il0floatpacket.94,4
	.align 4
.L_2il0floatpacket.95:
	.long	0x48000040
	.type	.L_2il0floatpacket.95,@object
	.size	.L_2il0floatpacket.95,4
	.align 4
.L_2il0floatpacket.96:
	.long	0x46000000
	.type	.L_2il0floatpacket.96,@object
	.size	.L_2il0floatpacket.96,4
	.align 4
.L_2il0floatpacket.97:
	.long	0x42de5c00
	.type	.L_2il0floatpacket.97,@object
	.size	.L_2il0floatpacket.97,4
	.align 4
.L_2il0floatpacket.98:
	.long	0xbf7f0000
	.type	.L_2il0floatpacket.98,@object
	.size	.L_2il0floatpacket.98,4
	.align 4
.L_2il0floatpacket.99:
	.long	0xb7935d5a
	.type	.L_2il0floatpacket.99,@object
	.size	.L_2il0floatpacket.99,4
	.align 4
.L_2il0floatpacket.100:
	.long	0x33e23666
	.type	.L_2il0floatpacket.100,@object
	.size	.L_2il0floatpacket.100,4
	.align 4
.L_2il0floatpacket.101:
	.long	0xb04353b9
	.type	.L_2il0floatpacket.101,@object
	.size	.L_2il0floatpacket.101,4
	.align 4
.L_2il0floatpacket.102:
	.long	0x2cb3e701
	.type	.L_2il0floatpacket.102,@object
	.size	.L_2il0floatpacket.102,4
	.align 4
.L_2il0floatpacket.103:
	.long	0xa92c998b
	.type	.L_2il0floatpacket.103,@object
	.size	.L_2il0floatpacket.103,4
	.align 4
.L_2il0floatpacket.104:
	.long	0x25aa5bfd
	.type	.L_2il0floatpacket.104,@object
	.size	.L_2il0floatpacket.104,4
	.align 4
.L_2il0floatpacket.105:
	.long	0xa22b5dae
	.type	.L_2il0floatpacket.105,@object
	.size	.L_2il0floatpacket.105,4
	.align 4
.L_2il0floatpacket.106:
	.long	0x3e9a0000
	.type	.L_2il0floatpacket.106,@object
	.size	.L_2il0floatpacket.106,4
	.align 4
.L_2il0floatpacket.107:
	.long	0x39826a14
	.type	.L_2il0floatpacket.107,@object
	.size	.L_2il0floatpacket.107,4
	.align 4
.L_2il0floatpacket.108:
	.long	0xbf800000
	.type	.L_2il0floatpacket.108,@object
	.size	.L_2il0floatpacket.108,4
	.align 4
.L_2il0floatpacket.109:
	.long	0x3f800000
	.type	.L_2il0floatpacket.109,@object
	.size	.L_2il0floatpacket.109,4
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
