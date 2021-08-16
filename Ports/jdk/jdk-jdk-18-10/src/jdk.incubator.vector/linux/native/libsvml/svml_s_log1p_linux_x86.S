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
	.file "svml_s_log1p.c"
	.text
..TXTST0:
.L_2__routine_start___svml_log1pf4_ha_e9_0:
# -- Begin  __svml_log1pf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1pf4_ha_e9
# --- __svml_log1pf4_ha_e9(__m128)
__svml_log1pf4_ha_e9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #325.1
        .byte     15                                            #439.540
        .byte     30                                            #439.540
        .byte     250                                           #439.540
	.cfi_startproc
..___tag_value___svml_log1pf4_ha_e9.1:
..L2:
                                                          #325.1
        pushq     %rbp                                          #325.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #325.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #325.1
        subq      $192, %rsp                                    #325.1
        movaps    %xmm0, %xmm8                                  #325.1
        movups    1984+__svml_slog1p_ha_data_internal(%rip), %xmm4 #374.44
        lea       -137216+__svml_slog1p_ha_data_internal(%rip), %r8 #420.61
        movaps    %xmm4, %xmm10                                 #379.12
        movaps    %xmm4, %xmm6                                  #380.12
        maxps     %xmm8, %xmm10                                 #379.12
        minps     %xmm8, %xmm6                                  #380.12
        movups    1088+__svml_slog1p_ha_data_internal(%rip), %xmm9 #375.48
        movaps    %xmm10, %xmm5                                 #384.12
        movaps    %xmm9, %xmm7                                  #378.19
        andps     %xmm8, %xmm9                                  #381.15
        cmpltps   1152+__svml_slog1p_ha_data_internal(%rip), %xmm9 #382.16
        addps     %xmm6, %xmm5                                  #384.12
        orps      1216+__svml_slog1p_ha_data_internal(%rip), %xmm9 #383.17
        movaps    %xmm8, %xmm15                                 #402.18
        movups    1536+__svml_slog1p_ha_data_internal(%rip), %xmm11 #390.19
        andps     %xmm9, %xmm5                                  #385.12
        andps     %xmm5, %xmm11                                 #390.19
        movaps    %xmm5, %xmm13                                 #401.23
        orps      1600+__svml_slog1p_ha_data_internal(%rip), %xmm11 #391.19
        psrld     $23, %xmm13                                   #401.23
        rcpps     %xmm11, %xmm3                                 #396.17
        cvtdq2ps  %xmm13, %xmm0                                 #407.18
        cmpltps   1664+__svml_slog1p_ha_data_internal(%rip), %xmm15 #402.18
        subps     %xmm5, %xmm10                                 #386.13
        movups    1280+__svml_slog1p_ha_data_internal(%rip), %xmm12 #398.21
        movaps    %xmm8, %xmm14                                 #403.18
        movups    1344+__svml_slog1p_ha_data_internal(%rip), %xmm2 #399.21
        andps     %xmm5, %xmm12                                 #398.21
        roundps   $0, %xmm3, %xmm3                              #404.17
        psubd     %xmm12, %xmm2                                 #399.21
        mulps     %xmm3, %xmm2                                  #406.18
        addps     %xmm10, %xmm6                                 #387.13
        cmpnleps  1728+__svml_slog1p_ha_data_internal(%rip), %xmm14 #403.18
        mulps     %xmm2, %xmm5                                  #411.25
        mulps     %xmm2, %xmm6                                  #412.13
        subps     %xmm4, %xmm5                                  #411.13
        movaps    %xmm5, %xmm2                                  #413.12
        psrld     $13, %xmm3                                    #417.22
        movd      %xmm3, %edx                                   #420.181
        orps      %xmm14, %xmm15                                #408.18
        movups    1920+__svml_slog1p_ha_data_internal(%rip), %xmm10 #423.44
        andnps    %xmm8, %xmm7                                  #378.19
        movups    1856+__svml_slog1p_ha_data_internal(%rip), %xmm9 #424.25
        addps     %xmm6, %xmm2                                  #413.12
        mulps     %xmm0, %xmm9                                  #424.25
        mulps     %xmm10, %xmm0                                 #425.25
        movmskps  %xmm15, %eax                                  #410.44
        movaps    %xmm2, %xmm1                                  #414.14
        pextrd    $1, %xmm3, %ecx                               #420.392
        subps     %xmm5, %xmm1                                  #414.14
        pextrd    $2, %xmm3, %esi                               #420.606
        subps     %xmm1, %xmm6                                  #415.13
        pextrd    $3, %xmm3, %edi                               #420.820
        movslq    %edx, %rdx                                    #420.61
        movslq    %ecx, %rcx                                    #420.272
        movslq    %esi, %rsi                                    #420.486
        movslq    %edi, %rdi                                    #420.700
        movups    1408+__svml_slog1p_ha_data_internal(%rip), %xmm11 #426.54
        mulps     %xmm2, %xmm11                                 #428.25
        movq      (%r8,%rdx), %xmm1                             #420.61
        movq      (%r8,%rcx), %xmm4                             #420.272
        movq      (%r8,%rsi), %xmm5                             #420.486
        movq      (%r8,%rdi), %xmm3                             #420.700
        unpcklps  %xmm4, %xmm1                                  #420.867
        addps     1472+__svml_slog1p_ha_data_internal(%rip), %xmm11 #428.13
        unpcklps  %xmm3, %xmm5                                  #420.902
        movaps    %xmm1, %xmm12                                 #420.942
        shufps    $238, %xmm5, %xmm1                            #420.986
        movlhps   %xmm5, %xmm12                                 #420.942
        addps     %xmm0, %xmm1                                  #425.13
        addps     %xmm9, %xmm12                                 #424.13
        movaps    %xmm2, %xmm0                                  #429.14
        mulps     %xmm2, %xmm0                                  #429.14
        mulps     %xmm0, %xmm11                                 #430.25
        movaps    %xmm2, %xmm0                                  #431.17
        addps     %xmm12, %xmm0                                 #431.17
        addps     %xmm11, %xmm6                                 #430.13
        movaps    %xmm0, %xmm13                                 #432.13
        subps     %xmm12, %xmm13                                #432.13
        subps     %xmm13, %xmm2                                 #433.13
        addps     %xmm2, %xmm1                                  #434.13
        addps     %xmm1, %xmm6                                  #435.13
        addps     %xmm6, %xmm0                                  #436.14
        orps      %xmm7, %xmm0                                  #437.14
        testl     %eax, %eax                                    #439.52
        jne       ..B1.3        # Prob 5%                       #439.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm8
..B1.2:                         # Preds ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #442.12
        popq      %rbp                                          #442.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #442.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm8, 64(%rsp)                               #439.193
        movups    %xmm0, 128(%rsp)                              #439.264
                                # LOE rbx r12 r13 r14 r15 eax
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #439.448
        movq      %r12, 8(%rsp)                                 #439.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #439.448
        movq      %r13, (%rsp)                                  #439.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #439.448
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #439.511
        jc        ..B1.10       # Prob 5%                       #439.511
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #439.464
        cmpl      $4, %r12d                                     #439.459
        jl        ..B1.7        # Prob 82%                      #439.459
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #439.667
        jmp       ..B1.2        # Prob 100%                     #439.667
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #439.540
        lea       128(%rsp,%r12,4), %rsi                        #439.540
..___tag_value___svml_log1pf4_ha_e9.19:
#       __svml_slog1p_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog1p_ha_cout_rare_internal           #439.540
..___tag_value___svml_log1pf4_ha_e9.20:
        jmp       ..B1.8        # Prob 100%                     #439.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log1pf4_ha_e9,@function
	.size	__svml_log1pf4_ha_e9,.-__svml_log1pf4_ha_e9
..LN__svml_log1pf4_ha_e9.0:
	.data
# -- End  __svml_log1pf4_ha_e9
	.text
.L_2__routine_start___svml_log1pf8_ha_l9_1:
# -- Begin  __svml_log1pf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1pf8_ha_l9
# --- __svml_log1pf8_ha_l9(__m256)
__svml_log1pf8_ha_l9:
# parameter 1: %ymm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #447.1
        .byte     15                                            #561.546
        .byte     30                                            #561.546
        .byte     250                                           #561.546
	.cfi_startproc
..___tag_value___svml_log1pf8_ha_l9.22:
..L23:
                                                         #447.1
        pushq     %rbp                                          #447.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #447.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #447.1
        subq      $192, %rsp                                    #447.1
        lea       -137216+__svml_slog1p_ha_data_internal(%rip), %rax #542.64
        vmovups   1984+__svml_slog1p_ha_data_internal(%rip), %ymm8 #496.47
        vmovups   1088+__svml_slog1p_ha_data_internal(%rip), %ymm11 #497.51
        vmovaps   %ymm0, %ymm10                                 #447.1
        vmaxps    %ymm10, %ymm8, %ymm4                          #501.12
        vminps    %ymm10, %ymm8, %ymm3                          #502.12
        vandps    %ymm11, %ymm10, %ymm12                        #503.15
        vcmplt_oqps 1152+__svml_slog1p_ha_data_internal(%rip), %ymm12, %ymm13 #504.16
        vaddps    %ymm3, %ymm4, %ymm14                          #506.12
        vorps     1216+__svml_slog1p_ha_data_internal(%rip), %ymm13, %ymm15 #505.17
        vandps    %ymm15, %ymm14, %ymm7                         #507.12
        vandps    1536+__svml_slog1p_ha_data_internal(%rip), %ymm7, %ymm0 #512.19
        vorps     1600+__svml_slog1p_ha_data_internal(%rip), %ymm0, %ymm1 #513.19
        vrcpps    %ymm1, %ymm5                                  #518.17
        vpsrld    $23, %ymm7, %ymm14                            #523.19
        vsubps    %ymm7, %ymm4, %ymm2                           #508.13
        vroundps  $0, %ymm5, %ymm5                              #526.17
        vcmplt_oqps 1664+__svml_slog1p_ha_data_internal(%rip), %ymm10, %ymm15 #524.18
        vaddps    %ymm2, %ymm3, %ymm6                           #509.13
        vcmpnle_uqps 1728+__svml_slog1p_ha_data_internal(%rip), %ymm10, %ymm3 #525.18
        vcvtdq2ps %ymm14, %ymm4                                 #529.18
        vandnps   %ymm10, %ymm11, %ymm9                         #500.19
        vmovups   1344+__svml_slog1p_ha_data_internal(%rip), %ymm11 #517.21
        vandps    1280+__svml_slog1p_ha_data_internal(%rip), %ymm7, %ymm12 #520.17
        vpsubd    %ymm12, %ymm11, %ymm13                        #521.17
        vmulps    %ymm13, %ymm5, %ymm0                          #528.18
        vfmsub213ps %ymm8, %ymm0, %ymm7                         #533.13
        vmulps    %ymm0, %ymm6, %ymm6                           #534.13
        vpsrld    $13, %ymm5, %ymm8                             #539.18
        vorps     %ymm3, %ymm15, %ymm2                          #530.18
        vaddps    %ymm6, %ymm7, %ymm3                           #535.12
        vsubps    %ymm7, %ymm3, %ymm7                           #536.14
        vmovmskps %ymm2, %edx                                   #532.45
        vsubps    %ymm7, %ymm6, %ymm2                           #537.13
        vmovd     %xmm8, %ecx                                   #542.184
        vextractf128 $1, %ymm8, %xmm5                           #542.1180
        movslq    %ecx, %rcx                                    #542.64
        vmovd     %xmm5, %r9d                                   #542.1161
        vpextrd   $1, %xmm8, %esi                               #542.426
        vpextrd   $2, %xmm8, %edi                               #542.671
        vpextrd   $3, %xmm8, %r8d                               #542.916
        vmovq     (%rax,%rcx), %xmm6                            #542.64
        vpextrd   $1, %xmm5, %r10d                              #542.1408
        vpextrd   $2, %xmm5, %r11d                              #542.1658
        vpextrd   $3, %xmm5, %ecx                               #542.1908
        movslq    %esi, %rsi                                    #542.306
        movslq    %edi, %rdi                                    #542.551
        movslq    %r8d, %r8                                     #542.796
        movslq    %r9d, %r9                                     #542.1041
        movslq    %r10d, %r10                                   #542.1288
        movslq    %r11d, %r11                                   #542.1538
        movslq    %ecx, %rcx                                    #542.1788
        vmovq     (%rax,%rsi), %xmm1                            #542.306
        vmovq     (%rax,%rdi), %xmm7                            #542.551
        vmovq     (%rax,%r8), %xmm11                            #542.796
        vmovq     (%rax,%r9), %xmm12                            #542.1041
        vmovq     (%rax,%r10), %xmm0                            #542.1288
        vmovq     (%rax,%r11), %xmm13                           #542.1538
        vmovq     (%rax,%rcx), %xmm14                           #542.1788
        vunpcklps %xmm7, %xmm6, %xmm15                          #542.2002
        vunpcklps %xmm11, %xmm1, %xmm5                          #542.2057
        vunpcklps %xmm13, %xmm12, %xmm1                         #542.2112
        vunpcklps %xmm14, %xmm0, %xmm0                          #542.2167
        vmovups   1408+__svml_slog1p_ha_data_internal(%rip), %ymm8 #548.57
        vmulps    %ymm3, %ymm3, %ymm6                           #551.14
        vfmadd213ps 1472+__svml_slog1p_ha_data_internal(%rip), %ymm3, %ymm8 #550.13
        vfmadd213ps %ymm2, %ymm6, %ymm8                         #552.13
        vinsertf128 $1, %xmm1, %ymm15, %ymm1                    #542.2230
        vinsertf128 $1, %xmm0, %ymm5, %ymm0                     #542.2338
        vunpcklps %ymm0, %ymm1, %ymm7                           #542.2429
        vfmadd231ps 1856+__svml_slog1p_ha_data_internal(%rip), %ymm4, %ymm7 #546.13
        vunpckhps %ymm0, %ymm1, %ymm5                           #542.2503
        vaddps    %ymm7, %ymm3, %ymm0                           #553.17
        vfmadd132ps 1920+__svml_slog1p_ha_data_internal(%rip), %ymm5, %ymm4 #547.13
        vsubps    %ymm7, %ymm0, %ymm2                           #554.13
        vsubps    %ymm2, %ymm3, %ymm3                           #555.13
        vaddps    %ymm3, %ymm4, %ymm4                           #556.13
        vaddps    %ymm4, %ymm8, %ymm1                           #557.13
        vaddps    %ymm1, %ymm0, %ymm2                           #558.14
        vorps     %ymm9, %ymm2, %ymm0                           #559.14
        testl     %edx, %edx                                    #561.52
        jne       ..B2.3        # Prob 5%                       #561.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm10
..B2.2:                         # Preds ..B2.3 ..B2.9 ..B2.1
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
        vmovups   %ymm10, 64(%rsp)                              #561.196
        vmovups   %ymm0, 128(%rsp)                              #561.270
        je        ..B2.2        # Prob 95%                      #561.374
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #561.454
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
        btl       %r12d, %r13d                                  #561.517
        jc        ..B2.10       # Prob 5%                       #561.517
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #561.470
        cmpl      $8, %r12d                                     #561.465
        jl        ..B2.7        # Prob 82%                      #561.465
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #561.676
        jmp       ..B2.2        # Prob 100%                     #561.676
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #561.546
        lea       128(%rsp,%r12,4), %rsi                        #561.546
..___tag_value___svml_log1pf8_ha_l9.40:
#       __svml_slog1p_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog1p_ha_cout_rare_internal           #561.546
..___tag_value___svml_log1pf8_ha_l9.41:
        jmp       ..B2.8        # Prob 100%                     #561.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log1pf8_ha_l9,@function
	.size	__svml_log1pf8_ha_l9,.-__svml_log1pf8_ha_l9
..LN__svml_log1pf8_ha_l9.1:
	.data
# -- End  __svml_log1pf8_ha_l9
	.text
.L_2__routine_start___svml_log1pf4_ha_l9_2:
# -- Begin  __svml_log1pf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1pf4_ha_l9
# --- __svml_log1pf4_ha_l9(__m128)
__svml_log1pf4_ha_l9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #569.1
        .byte     15                                            #683.540
        .byte     30                                            #683.540
        .byte     250                                           #683.540
	.cfi_startproc
..___tag_value___svml_log1pf4_ha_l9.43:
..L44:
                                                         #569.1
        pushq     %rbp                                          #569.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #569.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #569.1
        subq      $192, %rsp                                    #569.1
        vmovaps   %xmm0, %xmm8                                  #569.1
        vmovups   1984+__svml_slog1p_ha_data_internal(%rip), %xmm6 #618.44
        lea       -137216+__svml_slog1p_ha_data_internal(%rip), %r8 #664.61
        vmaxps    %xmm8, %xmm6, %xmm14                          #623.12
        vminps    %xmm8, %xmm6, %xmm15                          #624.12
        vmovups   1088+__svml_slog1p_ha_data_internal(%rip), %xmm9 #619.48
        vaddps    %xmm15, %xmm14, %xmm12                        #628.12
        vandps    %xmm9, %xmm8, %xmm10                          #625.15
        vandnps   %xmm8, %xmm9, %xmm7                           #622.19
        vcmpltps  1152+__svml_slog1p_ha_data_internal(%rip), %xmm10, %xmm11 #626.16
        vorps     1216+__svml_slog1p_ha_data_internal(%rip), %xmm11, %xmm13 #627.17
        vandps    %xmm13, %xmm12, %xmm5                         #629.12
        vandps    1536+__svml_slog1p_ha_data_internal(%rip), %xmm5, %xmm1 #634.19
        vpsrld    $23, %xmm5, %xmm12                            #645.19
        vorps     1600+__svml_slog1p_ha_data_internal(%rip), %xmm1, %xmm0 #635.19
        vsubps    %xmm5, %xmm14, %xmm2                          #630.13
        vrcpps    %xmm0, %xmm3                                  #640.17
        vcmpltps  1664+__svml_slog1p_ha_data_internal(%rip), %xmm8, %xmm13 #646.18
        vcmpnleps 1728+__svml_slog1p_ha_data_internal(%rip), %xmm8, %xmm14 #647.18
        vaddps    %xmm2, %xmm15, %xmm4                          #631.13
        vcvtdq2ps %xmm12, %xmm2                                 #651.18
        vmovups   1344+__svml_slog1p_ha_data_internal(%rip), %xmm9 #639.21
        vorps     %xmm14, %xmm13, %xmm15                        #652.18
        vandps    1280+__svml_slog1p_ha_data_internal(%rip), %xmm5, %xmm10 #642.17
        vroundps  $0, %xmm3, %xmm3                              #648.17
        vpsubd    %xmm10, %xmm9, %xmm11                         #643.17
        vmulps    %xmm11, %xmm3, %xmm1                          #650.18
        vmovmskps %xmm15, %eax                                  #654.44
        vmovups   1408+__svml_slog1p_ha_data_internal(%rip), %xmm14 #670.54
        vfmsub213ps %xmm6, %xmm1, %xmm5                         #655.13
        vpsrld    $13, %xmm3, %xmm6                             #661.18
        vmulps    %xmm1, %xmm4, %xmm4                           #656.13
        vmovd     %xmm6, %edx                                   #664.181
        vaddps    %xmm4, %xmm5, %xmm1                           #657.12
        vpextrd   $1, %xmm6, %ecx                               #664.392
        vsubps    %xmm5, %xmm1, %xmm5                           #658.14
        vfmadd213ps 1472+__svml_slog1p_ha_data_internal(%rip), %xmm1, %xmm14 #672.13
        vmulps    %xmm1, %xmm1, %xmm12                          #673.14
        vsubps    %xmm5, %xmm4, %xmm0                           #659.13
        vpextrd   $2, %xmm6, %esi                               #664.606
        vpextrd   $3, %xmm6, %edi                               #664.820
        movslq    %edx, %rdx                                    #664.61
        movslq    %ecx, %rcx                                    #664.272
        movslq    %esi, %rsi                                    #664.486
        movslq    %edi, %rdi                                    #664.700
        vmovq     (%r8,%rdx), %xmm3                             #664.61
        vmovq     (%r8,%rcx), %xmm4                             #664.272
        vmovq     (%r8,%rsi), %xmm5                             #664.486
        vmovq     (%r8,%rdi), %xmm6                             #664.700
        vunpcklps %xmm4, %xmm3, %xmm9                           #664.867
        vunpcklps %xmm6, %xmm5, %xmm10                          #664.902
        vmovlhps  %xmm10, %xmm9, %xmm13                         #664.942
        vfmadd231ps 1856+__svml_slog1p_ha_data_internal(%rip), %xmm2, %xmm13 #668.13
        vfmadd213ps %xmm0, %xmm12, %xmm14                       #674.13
        vshufps   $238, %xmm10, %xmm9, %xmm11                   #664.986
        vfmadd132ps 1920+__svml_slog1p_ha_data_internal(%rip), %xmm11, %xmm2 #669.13
        vaddps    %xmm13, %xmm1, %xmm15                         #675.17
        vsubps    %xmm13, %xmm15, %xmm0                         #676.13
        vsubps    %xmm0, %xmm1, %xmm1                           #677.13
        vaddps    %xmm1, %xmm2, %xmm2                           #678.13
        vaddps    %xmm2, %xmm14, %xmm0                          #679.13
        vaddps    %xmm0, %xmm15, %xmm1                          #680.14
        vorps     %xmm7, %xmm1, %xmm0                           #681.14
        testl     %eax, %eax                                    #683.52
        jne       ..B3.3        # Prob 5%                       #683.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm8
..B3.2:                         # Preds ..B3.3 ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #686.12
        popq      %rbp                                          #686.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #686.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm8, 64(%rsp)                               #683.193
        vmovups   %xmm0, 128(%rsp)                              #683.264
        je        ..B3.2        # Prob 95%                      #683.368
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #683.448
        movq      %r12, 8(%rsp)                                 #683.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #683.448
        movq      %r13, (%rsp)                                  #683.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #683.448
                                # LOE rbx r12 r14 r15 r13d
..B3.7:                         # Preds ..B3.8 ..B3.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #683.511
        jc        ..B3.10       # Prob 5%                       #683.511
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #683.464
        cmpl      $4, %r12d                                     #683.459
        jl        ..B3.7        # Prob 82%                      #683.459
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #683.667
        jmp       ..B3.2        # Prob 100%                     #683.667
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #683.540
        lea       128(%rsp,%r12,4), %rsi                        #683.540
..___tag_value___svml_log1pf4_ha_l9.61:
#       __svml_slog1p_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog1p_ha_cout_rare_internal           #683.540
..___tag_value___svml_log1pf4_ha_l9.62:
        jmp       ..B3.8        # Prob 100%                     #683.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log1pf4_ha_l9,@function
	.size	__svml_log1pf4_ha_l9,.-__svml_log1pf4_ha_l9
..LN__svml_log1pf4_ha_l9.2:
	.data
# -- End  __svml_log1pf4_ha_l9
	.text
.L_2__routine_start___svml_log1pf4_ha_ex_3:
# -- Begin  __svml_log1pf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1pf4_ha_ex
# --- __svml_log1pf4_ha_ex(__m128)
__svml_log1pf4_ha_ex:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #691.1
        .byte     15                                            #805.540
        .byte     30                                            #805.540
        .byte     250                                           #805.540
	.cfi_startproc
..___tag_value___svml_log1pf4_ha_ex.64:
..L65:
                                                         #691.1
        pushq     %rbp                                          #691.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #691.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #691.1
        subq      $192, %rsp                                    #691.1
        movaps    %xmm0, %xmm8                                  #691.1
        movups    1984+__svml_slog1p_ha_data_internal(%rip), %xmm4 #740.44
        movaps    %xmm8, %xmm2                                  #768.18
        movaps    %xmm4, %xmm10                                 #745.12
        movaps    %xmm4, %xmm6                                  #746.12
        maxps     %xmm8, %xmm10                                 #745.12
        minps     %xmm8, %xmm6                                  #746.12
        cmpltps   1664+__svml_slog1p_ha_data_internal(%rip), %xmm2 #768.18
        movups    1088+__svml_slog1p_ha_data_internal(%rip), %xmm9 #741.48
        movaps    %xmm10, %xmm5                                 #750.12
        movaps    %xmm9, %xmm7                                  #744.19
        andps     %xmm8, %xmm9                                  #747.15
        cmpltps   1152+__svml_slog1p_ha_data_internal(%rip), %xmm9 #748.16
        addps     %xmm6, %xmm5                                  #750.12
        orps      1216+__svml_slog1p_ha_data_internal(%rip), %xmm9 #749.17
        movaps    %xmm8, %xmm15                                 #769.18
        movups    1536+__svml_slog1p_ha_data_internal(%rip), %xmm11 #756.19
        andps     %xmm9, %xmm5                                  #751.12
        andps     %xmm5, %xmm11                                 #756.19
        movaps    %xmm5, %xmm14                                 #767.17
        orps      1600+__svml_slog1p_ha_data_internal(%rip), %xmm11 #757.19
        lea       -137216+__svml_slog1p_ha_data_internal(%rip), %r8 #786.61
        rcpps     %xmm11, %xmm3                                 #762.17
        cmpnleps  1728+__svml_slog1p_ha_data_internal(%rip), %xmm15 #769.18
        subps     %xmm5, %xmm10                                 #752.13
        movups    .L_2il0floatpacket.23(%rip), %xmm13           #770.105
        orps      %xmm15, %xmm2                                 #774.18
        movdqu    1280+__svml_slog1p_ha_data_internal(%rip), %xmm12 #764.15
        psrld     $23, %xmm14                                   #767.17
        movdqu    1344+__svml_slog1p_ha_data_internal(%rip), %xmm1 #765.15
        pand      %xmm5, %xmm12                                 #764.15
        psubd     %xmm12, %xmm1                                 #765.15
        andnps    %xmm8, %xmm7                                  #744.19
        movmskps  %xmm2, %eax                                   #776.44
        addps     %xmm13, %xmm3                                 #770.162
        addps     %xmm10, %xmm6                                 #753.13
        cvtdq2ps  %xmm14, %xmm0                                 #773.18
        subps     %xmm13, %xmm3                                 #770.212
        mulps     %xmm3, %xmm1                                  #772.18
        psrld     $13, %xmm3                                    #783.16
        movd      %xmm3, %edx                                   #786.181
        pshufd    $2, %xmm3, %xmm9                              #786.657
        mulps     %xmm1, %xmm5                                  #777.25
        mulps     %xmm1, %xmm6                                  #778.13
        movd      %xmm9, %esi                                   #786.639
        subps     %xmm4, %xmm5                                  #777.13
        movaps    %xmm5, %xmm2                                  #779.12
        movslq    %edx, %rdx                                    #786.61
        addps     %xmm6, %xmm2                                  #779.12
        movaps    %xmm2, %xmm4                                  #780.14
        movslq    %esi, %rsi                                    #786.519
        subps     %xmm5, %xmm4                                  #780.14
        pshufd    $1, %xmm3, %xmm5                              #786.408
        subps     %xmm4, %xmm6                                  #781.13
        movd      %xmm5, %ecx                                   #786.390
        pshufd    $3, %xmm3, %xmm3                              #786.906
        movd      %xmm3, %edi                                   #786.888
        movq      (%r8,%rdx), %xmm1                             #786.61
        movq      (%r8,%rsi), %xmm12                            #786.519
        movslq    %ecx, %rcx                                    #786.270
        movslq    %edi, %rdi                                    #786.768
        movups    1920+__svml_slog1p_ha_data_internal(%rip), %xmm14 #789.44
        movups    1856+__svml_slog1p_ha_data_internal(%rip), %xmm13 #790.25
        movq      (%r8,%rcx), %xmm10                            #786.270
        movq      (%r8,%rdi), %xmm11                            #786.768
        unpcklps  %xmm10, %xmm1                                 #786.970
        unpcklps  %xmm11, %xmm12                                #786.1005
        movaps    %xmm1, %xmm3                                  #786.1045
        movlhps   %xmm12, %xmm3                                 #786.1045
        shufps    $238, %xmm12, %xmm1                           #786.1089
        mulps     %xmm0, %xmm13                                 #790.25
        mulps     %xmm14, %xmm0                                 #791.25
        addps     %xmm13, %xmm3                                 #790.13
        addps     %xmm0, %xmm1                                  #791.13
        movups    1408+__svml_slog1p_ha_data_internal(%rip), %xmm12 #792.54
        movaps    %xmm2, %xmm0                                  #795.14
        mulps     %xmm2, %xmm12                                 #794.25
        mulps     %xmm2, %xmm0                                  #795.14
        addps     1472+__svml_slog1p_ha_data_internal(%rip), %xmm12 #794.13
        mulps     %xmm0, %xmm12                                 #796.25
        movaps    %xmm2, %xmm0                                  #797.17
        addps     %xmm3, %xmm0                                  #797.17
        addps     %xmm12, %xmm6                                 #796.13
        movaps    %xmm0, %xmm4                                  #798.13
        subps     %xmm3, %xmm4                                  #798.13
        subps     %xmm4, %xmm2                                  #799.13
        addps     %xmm2, %xmm1                                  #800.13
        addps     %xmm1, %xmm6                                  #801.13
        addps     %xmm6, %xmm0                                  #802.14
        orps      %xmm7, %xmm0                                  #803.14
        testl     %eax, %eax                                    #805.52
        jne       ..B4.3        # Prob 5%                       #805.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm8
..B4.2:                         # Preds ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #808.12
        popq      %rbp                                          #808.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #808.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm8, 64(%rsp)                               #805.193
        movups    %xmm0, 128(%rsp)                              #805.264
                                # LOE rbx r12 r13 r14 r15 eax
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #805.448
        movq      %r12, 8(%rsp)                                 #805.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #805.448
        movq      %r13, (%rsp)                                  #805.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #805.448
                                # LOE rbx r12 r14 r15 r13d
..B4.7:                         # Preds ..B4.8 ..B4.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #805.511
        jc        ..B4.10       # Prob 5%                       #805.511
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #805.464
        cmpl      $4, %r12d                                     #805.459
        jl        ..B4.7        # Prob 82%                      #805.459
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #805.667
        jmp       ..B4.2        # Prob 100%                     #805.667
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #805.540
        lea       128(%rsp,%r12,4), %rsi                        #805.540
..___tag_value___svml_log1pf4_ha_ex.82:
#       __svml_slog1p_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog1p_ha_cout_rare_internal           #805.540
..___tag_value___svml_log1pf4_ha_ex.83:
        jmp       ..B4.8        # Prob 100%                     #805.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log1pf4_ha_ex,@function
	.size	__svml_log1pf4_ha_ex,.-__svml_log1pf4_ha_ex
..LN__svml_log1pf4_ha_ex.3:
	.data
# -- End  __svml_log1pf4_ha_ex
	.text
.L_2__routine_start___svml_log1pf16_ha_z0_4:
# -- Begin  __svml_log1pf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1pf16_ha_z0
# --- __svml_log1pf16_ha_z0(__m512)
__svml_log1pf16_ha_z0:
# parameter 1: %zmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #813.1
        .byte     15                                            #902.760
        .byte     30                                            #902.760
        .byte     250                                           #902.760
	.cfi_startproc
..___tag_value___svml_log1pf16_ha_z0.85:
..L86:
                                                         #813.1
        pushq     %rbp                                          #813.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #813.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #813.1
        subq      $192, %rsp                                    #813.1
        vmovups   256+__svml_slog1p_ha_data_internal_avx512(%rip), %zmm4 #853.54
        vmovups   320+__svml_slog1p_ha_data_internal_avx512(%rip), %zmm11 #858.57
        vmovaps   %zmm0, %zmm6                                  #813.1
        vmaxps    {sae}, %zmm6, %zmm4, %zmm7                    #855.22
        vminps    {sae}, %zmm6, %zmm4, %zmm9                    #856.22
        vaddps    {rn-sae}, %zmm6, %zmm4, %zmm8                 #854.23
        vandps    %zmm11, %zmm6, %zmm5                          #859.22
        vgetmantps $8, {sae}, %zmm8, %zmm3                      #857.28
        vgetexpps {sae}, %zmm8, %zmm2                           #861.25
        vsubps    {rn-sae}, %zmm7, %zmm8, %zmm10                #860.22
        vrcp14ps  %zmm3, %zmm12                                 #862.24
        vsubps    {rn-sae}, %zmm10, %zmm9, %zmm13               #863.22
        vxorps    %zmm11, %zmm2, %zmm14                         #865.24
        vrndscaleps $104, {sae}, %zmm12, %zmm7                  #866.24
        vmovups   128+__svml_slog1p_ha_data_internal_avx512(%rip), %zmm10 #879.293
        vmovups   640+__svml_slog1p_ha_data_internal_avx512(%rip), %zmm9 #881.53
        vmovups   448+__svml_slog1p_ha_data_internal_avx512(%rip), %zmm12 #884.61
        vscalefps {rn-sae}, %zmm14, %zmm13, %zmm15              #867.20
        vfmsub213ps {rn-sae}, %zmm4, %zmm7, %zmm3               #870.20
        vgetexpps {sae}, %zmm7, %zmm8                           #868.26
        vmovups   576+__svml_slog1p_ha_data_internal_avx512(%rip), %zmm13 #888.61
        vmovups   704+__svml_slog1p_ha_data_internal_avx512(%rip), %zmm14 #891.53
        vmulps    {rn-sae}, %zmm15, %zmm7, %zmm0                #871.22
        vsubps    {rn-sae}, %zmm4, %zmm2, %zmm4                 #874.25
        vpsrld    $18, %zmm7, %zmm2                             #876.45
        vaddps    {rn-sae}, %zmm0, %zmm3, %zmm1                 #872.21
        vpermt2ps 192+__svml_slog1p_ha_data_internal_avx512(%rip), %zmm2, %zmm10 #879.293
        vsubps    {rn-sae}, %zmm3, %zmm1, %zmm3                 #873.23
        vsubps    {rn-sae}, %zmm3, %zmm0, %zmm11                #875.22
        vmovups   __svml_slog1p_ha_data_internal_avx512(%rip), %zmm0 #878.293
        vmovups   512+__svml_slog1p_ha_data_internal_avx512(%rip), %zmm3 #885.61
        vaddps    {rn-sae}, %zmm11, %zmm10, %zmm15              #883.22
        vpermt2ps 64+__svml_slog1p_ha_data_internal_avx512(%rip), %zmm2, %zmm0 #878.293
        vsubps    {rn-sae}, %zmm8, %zmm4, %zmm2                 #880.25
        vfmadd231ps {rn-sae}, %zmm1, %zmm12, %zmm3              #886.19
        vmulps    {rn-sae}, %zmm1, %zmm1, %zmm12                #887.22
        vfmadd231ps {rn-sae}, %zmm2, %zmm9, %zmm0               #882.20
        vfmadd213ps {rn-sae}, %zmm13, %zmm1, %zmm3              #889.19
        vfmadd213ps {rn-sae}, %zmm15, %zmm14, %zmm2             #892.20
        vaddps    {rn-sae}, %zmm1, %zmm0, %zmm4                 #890.24
        vfmadd213ps {rn-sae}, %zmm2, %zmm12, %zmm3              #894.19
        vsubps    {rn-sae}, %zmm0, %zmm4, %zmm0                 #893.22
        vsubps    {rn-sae}, %zmm0, %zmm1, %zmm1                 #895.21
        vcmpps    $4, {sae}, %zmm1, %zmm1, %k0                  #896.27
        vaddps    {rn-sae}, %zmm1, %zmm3, %zmm0                 #898.21
        kmovw     %k0, %esi                                     #897.48
        vaddps    {rn-sae}, %zmm0, %zmm4, %zmm1                 #899.24
        vorps     %zmm5, %zmm1, %zmm0                           #900.21
        testl     %esi, %esi                                    #902.52
        jne       ..B5.3        # Prob 5%                       #902.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm6
..B5.2:                         # Preds ..B5.9 ..B5.7 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #905.12
        popq      %rbp                                          #905.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #905.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #902.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm6
..B5.4:                         # Preds ..B5.3
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #902.249
        movl      %edx, %eax                                    #902.303
        orl       $8064, %eax                                   #902.303
        cmpl      %eax, %edx                                    #902.332
        je        ..B5.6        # Prob 78%                      #902.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm6
..B5.5:                         # Preds ..B5.4
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #902.344
        vldmxcsr  32(%rsp)                                      #902.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm6
..B5.6:                         # Preds ..B5.5 ..B5.4
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm6, 64(%rsp)                               #902.404
        vmovups   %zmm0, 128(%rsp)                              #902.478
        testl     %esi, %esi                                    #902.586
        jne       ..B5.11       # Prob 5%                       #902.586
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B5.7:                         # Preds ..B5.14 ..B5.6
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #902.933
        je        ..B5.2        # Prob 78%                      #902.933
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B5.8:                         # Preds ..B5.7
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #902.958
        movl      32(%rsp), %eax                                #902.958
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B5.9:                         # Preds ..B5.8
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #902.945
        orl       %edx, %eax                                    #902.945
        movl      %eax, 32(%rsp)                                #902.945
        vldmxcsr  32(%rsp)                                      #902.945
        jmp       ..B5.2        # Prob 100%                     #902.945
                                # LOE rbx r12 r13 r14 r15 zmm0
..B5.11:                        # Preds ..B5.6
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #902.668
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B5.18:                        # Preds ..B5.11
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
..B5.12:                        # Preds ..B5.13 ..B5.18
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #902.731
        jc        ..B5.15       # Prob 5%                       #902.731
                                # LOE rbx r15 r12d r13d r14d
..B5.13:                        # Preds ..B5.15 ..B5.12
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #902.684
        cmpl      $16, %ebx                                     #902.679
        jl        ..B5.12       # Prob 82%                      #902.679
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
        vmovups   128(%rsp), %zmm0                              #902.888
        jmp       ..B5.7        # Prob 100%                     #902.888
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B5.15:                        # Preds ..B5.12
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,4), %rdi                         #902.760
        lea       128(%rsp,%rbx,4), %rsi                        #902.760
..___tag_value___svml_log1pf16_ha_z0.113:
#       __svml_slog1p_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog1p_ha_cout_rare_internal           #902.760
..___tag_value___svml_log1pf16_ha_z0.114:
        jmp       ..B5.13       # Prob 100%                     #902.760
        .align    16,0x90
                                # LOE r15 ebx r12d r13d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_log1pf16_ha_z0,@function
	.size	__svml_log1pf16_ha_z0,.-__svml_log1pf16_ha_z0
..LN__svml_log1pf16_ha_z0.4:
	.data
# -- End  __svml_log1pf16_ha_z0
	.text
.L_2__routine_start___svml_log1pf8_ha_e9_5:
# -- Begin  __svml_log1pf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1pf8_ha_e9
# --- __svml_log1pf8_ha_e9(__m256)
__svml_log1pf8_ha_e9:
# parameter 1: %ymm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1234.1
        .byte     15                                            #1348.546
        .byte     30                                            #1348.546
        .byte     250                                           #1348.546
	.cfi_startproc
..___tag_value___svml_log1pf8_ha_e9.116:
..L117:
                                                        #1234.1
        pushq     %rbp                                          #1234.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1234.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1234.1
        subq      $192, %rsp                                    #1234.1
        lea       -137216+__svml_slog1p_ha_data_internal(%rip), %rax #1329.42
        vmovups   1984+__svml_slog1p_ha_data_internal(%rip), %ymm15 #1283.47
        vmovups   1088+__svml_slog1p_ha_data_internal(%rip), %ymm4 #1284.51
        vmovups   1280+__svml_slog1p_ha_data_internal(%rip), %xmm7 #1303.25
        vmovups   1344+__svml_slog1p_ha_data_internal(%rip), %xmm9 #1304.25
        vmaxps    %ymm0, %ymm15, %ymm14                         #1288.12
        vminps    %ymm0, %ymm15, %ymm6                          #1289.12
        vandps    %ymm4, %ymm0, %ymm3                           #1290.15
        vcmplt_oqps 1152+__svml_slog1p_ha_data_internal(%rip), %ymm3, %ymm5 #1291.16
        vaddps    %ymm6, %ymm14, %ymm2                          #1293.12
        vorps     1216+__svml_slog1p_ha_data_internal(%rip), %ymm5, %ymm13 #1292.17
        vandps    %ymm13, %ymm2, %ymm8                          #1294.12
        vandps    1536+__svml_slog1p_ha_data_internal(%rip), %ymm8, %ymm12 #1299.19
        vorps     1600+__svml_slog1p_ha_data_internal(%rip), %ymm12, %ymm10 #1300.19
        vandnps   %ymm0, %ymm4, %ymm1                           #1287.19
        vrcpps    %ymm10, %ymm4                                 #1305.17
        vsubps    %ymm8, %ymm14, %ymm11                         #1295.13
        vcmplt_oqps 1664+__svml_slog1p_ha_data_internal(%rip), %ymm0, %ymm12 #1311.18
        vroundps  $0, %ymm4, %ymm13                             #1313.17
        vaddps    %ymm11, %ymm6, %ymm14                         #1296.13
        vcmpnle_uqps 1728+__svml_slog1p_ha_data_internal(%rip), %ymm0, %ymm11 #1312.18
        vpand     %xmm7, %xmm8, %xmm3                           #1307.21
        vextractf128 $1, %ymm8, %xmm10                          #1306.109
        vpsubd    %xmm3, %xmm9, %xmm5                           #1308.21
        vpsrld    $23, %xmm8, %xmm3                             #1310.23
        vandps    %xmm7, %xmm10, %xmm7                          #1307.89
        vpsubd    %xmm7, %xmm9, %xmm9                           #1308.88
        vorps     %ymm11, %ymm12, %ymm7                         #1317.18
        vpxor     %xmm11, %xmm11, %xmm11                        #1319.62
        vinsertf128 $1, %xmm9, %ymm5, %ymm4                     #1314.17
        vpsrld    $23, %xmm10, %xmm5                            #1310.80
        vmulps    %ymm4, %ymm13, %ymm6                          #1315.18
        vmulps    %ymm6, %ymm8, %ymm8                           #1320.28
        vsubps    %ymm15, %ymm8, %ymm15                         #1320.13
        vextractf128 $1, %ymm7, %xmm9                           #1318.117
        vpackssdw %xmm9, %xmm7, %xmm10                          #1319.79
        vpacksswb %xmm11, %xmm10, %xmm12                        #1319.62
        vpmovmskb %xmm12, %edx                                  #1319.43
        vpsrld    $13, %xmm13, %xmm7                            #1326.22
        vmovd     %xmm7, %ecx                                   #1329.162
        movslq    %ecx, %rcx                                    #1329.42
        vpextrd   $1, %xmm7, %esi                               #1329.360
        vpextrd   $3, %xmm7, %r8d                               #1329.762
        movslq    %esi, %rsi                                    #1329.240
        movslq    %r8d, %r8                                     #1329.642
        vpextrd   $2, %xmm7, %edi                               #1329.561
        vmovq     (%rax,%rcx), %xmm9                            #1329.42
        movslq    %edi, %rdi                                    #1329.441
        vmovq     (%rax,%rsi), %xmm11                           #1329.240
        vmovq     (%rax,%r8), %xmm12                            #1329.642
        vinsertf128 $1, %xmm5, %ymm3, %ymm2                     #1316.38
        vmulps    %ymm6, %ymm14, %ymm3                          #1321.13
        vmovq     (%rax,%rdi), %xmm10                           #1329.441
        vunpcklps %xmm10, %xmm9, %xmm7                          #1329.1630
        vextractf128 $1, %ymm13, %xmm6                          #1325.114
        vcvtdq2ps %ymm2, %ymm5                                  #1316.18
        vaddps    %ymm3, %ymm15, %ymm4                          #1322.12
        vpsrld    $13, %xmm6, %xmm8                             #1326.81
        vmovd     %xmm8, %r9d                                   #1329.963
        vsubps    %ymm15, %ymm4, %ymm2                          #1323.14
        vpextrd   $1, %xmm8, %r10d                              #1329.1161
        vpextrd   $2, %xmm8, %r11d                              #1329.1362
        vpextrd   $3, %xmm8, %ecx                               #1329.1563
        movslq    %r9d, %r9                                     #1329.843
        movslq    %r10d, %r10                                   #1329.1041
        movslq    %r11d, %r11                                   #1329.1242
        movslq    %ecx, %rcx                                    #1329.1443
        vunpcklps %xmm12, %xmm11, %xmm8                         #1329.1685
        vmulps    1856+__svml_slog1p_ha_data_internal(%rip), %ymm5, %ymm12 #1333.28
        vmulps    1920+__svml_slog1p_ha_data_internal(%rip), %ymm5, %ymm5 #1334.28
        vmovq     (%rax,%r9), %xmm14                            #1329.843
        vmovq     (%rax,%r11), %xmm15                           #1329.1242
        vmovq     (%rax,%rcx), %xmm6                            #1329.1443
        vunpcklps %xmm15, %xmm14, %xmm14                        #1329.1740
        vsubps    %ymm2, %ymm3, %ymm3                           #1324.13
        vmovq     (%rax,%r10), %xmm2                            #1329.1041
        vunpcklps %xmm6, %xmm2, %xmm2                           #1329.1795
        vinsertf128 $1, %xmm14, %ymm7, %ymm9                    #1329.1858
        vinsertf128 $1, %xmm2, %ymm8, %ymm10                    #1329.1966
        vunpckhps %ymm10, %ymm9, %ymm13                         #1329.2131
        vaddps    %ymm5, %ymm13, %ymm7                          #1334.13
        vmulps    1408+__svml_slog1p_ha_data_internal(%rip), %ymm4, %ymm5 #1337.28
        vmulps    %ymm4, %ymm4, %ymm2                           #1338.14
        vaddps    1472+__svml_slog1p_ha_data_internal(%rip), %ymm5, %ymm13 #1337.13
        vunpcklps %ymm10, %ymm9, %ymm11                         #1329.2057
        vaddps    %ymm12, %ymm11, %ymm15                        #1333.13
        vmulps    %ymm2, %ymm13, %ymm6                          #1339.28
        vaddps    %ymm15, %ymm4, %ymm9                          #1340.17
        vaddps    %ymm6, %ymm3, %ymm8                           #1339.13
        vsubps    %ymm15, %ymm9, %ymm3                          #1341.13
        vsubps    %ymm3, %ymm4, %ymm4                           #1342.13
        vaddps    %ymm4, %ymm7, %ymm3                           #1343.13
        vaddps    %ymm3, %ymm8, %ymm2                           #1344.13
        vaddps    %ymm2, %ymm9, %ymm4                           #1345.14
        vorps     %ymm1, %ymm4, %ymm1                           #1346.14
        testb     %dl, %dl                                      #1348.52
        jne       ..B6.3        # Prob 5%                       #1348.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm1
..B6.2:                         # Preds ..B6.3 ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        vmovaps   %ymm1, %ymm0                                  #1351.12
        movq      %rbp, %rsp                                    #1351.12
        popq      %rbp                                          #1351.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1351.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm0, 64(%rsp)                               #1348.196
        vmovups   %ymm1, 128(%rsp)                              #1348.270
        testl     %edx, %edx                                    #1348.374
        je        ..B6.2        # Prob 95%                      #1348.374
                                # LOE rbx r12 r13 r14 r15 edx ymm1
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1348.454
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
        btl       %r12d, %r13d                                  #1348.517
        jc        ..B6.10       # Prob 5%                       #1348.517
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1348.470
        cmpl      $8, %r12d                                     #1348.465
        jl        ..B6.7        # Prob 82%                      #1348.465
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm1                              #1348.676
        jmp       ..B6.2        # Prob 100%                     #1348.676
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm1
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1348.546
        lea       128(%rsp,%r12,4), %rsi                        #1348.546
..___tag_value___svml_log1pf8_ha_e9.134:
#       __svml_slog1p_ha_cout_rare_internal(const float *, float *)
        call      __svml_slog1p_ha_cout_rare_internal           #1348.546
..___tag_value___svml_log1pf8_ha_e9.135:
        jmp       ..B6.8        # Prob 100%                     #1348.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log1pf8_ha_e9,@function
	.size	__svml_log1pf8_ha_e9,.-__svml_log1pf8_ha_e9
..LN__svml_log1pf8_ha_e9.5:
	.data
# -- End  __svml_log1pf8_ha_e9
	.text
.L_2__routine_start___svml_slog1p_ha_cout_rare_internal_6:
# -- Begin  __svml_slog1p_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_slog1p_ha_cout_rare_internal
	.globl __svml_slog1p_ha_cout_rare_internal
# --- __svml_slog1p_ha_cout_rare_internal(const float *, float *)
__svml_slog1p_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1121.1
        .byte     15                                            #1221.13
        .byte     30                                            #1221.13
        .byte     250                                           #1221.13
	.cfi_startproc
..___tag_value___svml_slog1p_ha_cout_rare_internal.137:
..L138:
                                                        #1121.1
        xorl      %eax, %eax                                    #1132.14
        movss     .L_2il0floatpacket.107(%rip), %xmm1           #1134.6
        xorb      %r8b, %r8b                                    #1133.19
        movss     (%rdi), %xmm5                                 #1134.14
        addss     %xmm1, %xmm5                                  #1134.19
        movss     %xmm5, -20(%rsp)                              #1122.23
        movzwl    -18(%rsp), %edx                               #1135.32
        andl      $32640, %edx                                  #1135.32
        cmpl      $32640, %edx                                  #1135.59
        je        ..B7.15       # Prob 16%                      #1135.59
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax r8b xmm1 xmm5
..B7.2:                         # Preds ..B7.1
                                # Execution count [8.40e-01]
        movss     %xmm5, -16(%rsp)                              #1137.9
        xorl      %ecx, %ecx                                    #1138.9
        movzwl    -14(%rsp), %edx                               #1139.35
        testl     $32640, %edx                                  #1139.35
        jne       ..B7.4        # Prob 50%                      #1139.57
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx r8b xmm1 xmm5
..B7.3:                         # Preds ..B7.2
                                # Execution count [4.20e-01]
        mulss     .L_2il0floatpacket.92(%rip), %xmm5            #1142.13
        movb      $1, %r8b                                      #1141.13
        movss     %xmm5, -16(%rsp)                              #1142.13
        movl      $-40, %ecx                                    #1143.13
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx r8b xmm1 xmm5
..B7.4:                         # Preds ..B7.3 ..B7.2
                                # Execution count [8.40e-01]
        pxor      %xmm3, %xmm3                                  #1145.9
        comiss    %xmm3, %xmm5                                  #1145.34
        jbe       ..B7.10       # Prob 50%                      #1145.34
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx r8b xmm1 xmm3 xmm5
..B7.5:                         # Preds ..B7.4
                                # Execution count [4.20e-01]
        movaps    %xmm5, %xmm2                                  #1147.21
        subss     %xmm1, %xmm2                                  #1147.21
        movss     %xmm2, -20(%rsp)                              #1148.13
        andb      $127, -17(%rsp)                               #1149.34
        movss     -20(%rsp), %xmm0                              #1150.18
        comiss    .L_2il0floatpacket.93(%rip), %xmm0            #1150.42
        jbe       ..B7.9        # Prob 50%                      #1150.42
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx r8b xmm1 xmm2 xmm3 xmm5
..B7.6:                         # Preds ..B7.5
                                # Execution count [2.10e-01]
        movzwl    -14(%rsp), %edx                               #1152.44
        pxor      %xmm6, %xmm6                                  #1153.29
        andl      $32640, %edx                                  #1152.44
        shrl      $7, %edx                                      #1152.44
        lea       -127(%rcx,%rdx), %ecx                         #1152.17
        cvtsi2ss  %ecx, %xmm6                                   #1153.29
        cmpb      $1, %r8b                                      #1154.34
        je        ..B7.13       # Prob 16%                      #1154.34
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm1 xmm3 xmm5 xmm6
..B7.7:                         # Preds ..B7.6
                                # Execution count [1.76e-01]
        movss     .L_2il0floatpacket.103(%rip), %xmm4           #1161.61
        movss     .L_2il0floatpacket.104(%rip), %xmm0           #1162.61
        mulss     %xmm6, %xmm4                                  #1161.61
        mulss     %xmm0, %xmm6                                  #1162.61
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm1 xmm4 xmm5 xmm6
..B7.8:                         # Preds ..B7.13 ..B7.7
                                # Execution count [2.10e-01]
        movss     %xmm5, -20(%rsp)                              #1164.17
        movaps    %xmm4, %xmm9                                  #1171.43
        movzwl    -18(%rsp), %edx                               #1165.38
        lea       _vmldLnHATab(%rip), %r8                       #1168.42
        andl      $-32641, %edx                                 #1165.38
        addl      $16256, %edx                                  #1165.38
        movw      %dx, -18(%rsp)                                #1165.38
        movss     -20(%rsp), %xmm8                              #1166.33
        movaps    %xmm8, %xmm2                                  #1166.53
        movss     .L_2il0floatpacket.106(%rip), %xmm7           #1176.53
        addss     .L_2il0floatpacket.105(%rip), %xmm2           #1166.53
        movss     %xmm2, -24(%rsp)                              #1166.17
        movl      -24(%rsp), %ecx                               #1167.42
        andl      $127, %ecx                                    #1167.69
        lea       (%rcx,%rcx,2), %edi                           #1168.58
        movss     4(%r8,%rdi,4), %xmm5                          #1169.46
        movss     (%r8,%rdi,4), %xmm0                           #1168.42
        addss     %xmm5, %xmm9                                  #1171.43
        addss     8(%r8,%rdi,4), %xmm6                          #1174.43
        movaps    %xmm9, %xmm3                                  #1172.40
        subss     %xmm4, %xmm3                                  #1172.40
        movss     %xmm3, -24(%rsp)                              #1172.17
        movss     -24(%rsp), %xmm4                              #1173.46
        subss     %xmm4, %xmm5                                  #1173.46
        movss     %xmm5, -24(%rsp)                              #1173.17
        movss     -24(%rsp), %xmm10                             #1175.33
        addss     %xmm6, %xmm10                                 #1175.40
        movaps    %xmm7, %xmm6                                  #1176.53
        addss     %xmm8, %xmm6                                  #1176.53
        movss     %xmm6, -24(%rsp)                              #1176.17
        movss     -24(%rsp), %xmm12                             #1177.33
        subss     %xmm7, %xmm12                                 #1177.56
        subss     %xmm12, %xmm8                                 #1178.37
        mulss     %xmm0, %xmm12                                 #1179.42
        subss     %xmm1, %xmm12                                 #1179.49
        mulss     %xmm8, %xmm0                                  #1180.42
        movaps    %xmm0, %xmm15                                 #1185.42
        movaps    %xmm12, %xmm2                                 #1181.37
        addss     %xmm10, %xmm15                                #1185.42
        addss     %xmm9, %xmm12                                 #1184.42
        addss     %xmm0, %xmm2                                  #1181.37
        movaps    %xmm15, %xmm1                                 #1186.42
        movaps    %xmm12, %xmm13                                #1189.42
        subss     %xmm10, %xmm1                                 #1186.42
        addss     %xmm15, %xmm13                                #1189.42
        movss     %xmm1, -24(%rsp)                              #1186.17
        movss     -24(%rsp), %xmm11                             #1187.40
        subss     %xmm11, %xmm0                                 #1187.40
        movss     %xmm0, -24(%rsp)                              #1187.17
        movss     -24(%rsp), %xmm0                              #1188.31
        movss     %xmm13, (%rsi)                                #1189.17
        subss     %xmm12, %xmm13                                #1190.40
        movss     .L_2il0floatpacket.100(%rip), %xmm12          #1182.92
        mulss     %xmm2, %xmm12                                 #1182.111
        movss     %xmm13, -24(%rsp)                             #1190.17
        movss     -24(%rsp), %xmm14                             #1191.42
        addss     .L_2il0floatpacket.99(%rip), %xmm12           #1182.129
        subss     %xmm14, %xmm15                                #1191.42
        mulss     %xmm2, %xmm12                                 #1182.150
        movss     %xmm15, -24(%rsp)                             #1191.17
        movss     -24(%rsp), %xmm1                              #1192.33
        addss     .L_2il0floatpacket.98(%rip), %xmm12           #1182.170
        mulss     %xmm2, %xmm12                                 #1182.191
        addss     .L_2il0floatpacket.97(%rip), %xmm12           #1182.211
        mulss     %xmm2, %xmm12                                 #1182.232
        addss     .L_2il0floatpacket.96(%rip), %xmm12           #1182.252
        mulss     %xmm2, %xmm12                                 #1182.273
        addss     .L_2il0floatpacket.95(%rip), %xmm12           #1182.293
        mulss     %xmm2, %xmm12                                 #1182.314
        addss     .L_2il0floatpacket.94(%rip), %xmm12           #1182.334
        mulss     %xmm2, %xmm12                                 #1183.34
        mulss     %xmm2, %xmm12                                 #1183.36
        addss     %xmm12, %xmm0                                 #1188.38
        addss     %xmm0, %xmm1                                  #1192.40
        movss     %xmm1, -24(%rsp)                              #1192.17
        movss     -24(%rsp), %xmm3                              #1193.40
        addss     (%rsi), %xmm3                                 #1193.40
        movss     %xmm3, (%rsi)                                 #1193.17
        ret                                                     #1193.17
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.9:                         # Preds ..B7.5
                                # Execution count [2.10e-01]
        movss     .L_2il0floatpacket.100(%rip), %xmm0           #1197.92
        mulss     %xmm2, %xmm0                                  #1197.111
        addss     .L_2il0floatpacket.99(%rip), %xmm0            #1197.131
        mulss     %xmm2, %xmm0                                  #1197.152
        addss     .L_2il0floatpacket.98(%rip), %xmm0            #1197.172
        mulss     %xmm2, %xmm0                                  #1197.193
        addss     .L_2il0floatpacket.97(%rip), %xmm0            #1197.213
        mulss     %xmm2, %xmm0                                  #1197.234
        addss     .L_2il0floatpacket.96(%rip), %xmm0            #1197.254
        mulss     %xmm2, %xmm0                                  #1197.275
        addss     .L_2il0floatpacket.95(%rip), %xmm0            #1197.295
        mulss     %xmm2, %xmm0                                  #1197.316
        addss     .L_2il0floatpacket.94(%rip), %xmm0            #1197.336
        mulss     %xmm2, %xmm0                                  #1198.34
        mulss     %xmm2, %xmm0                                  #1198.36
        addss     %xmm2, %xmm0                                  #1199.36
        movss     %xmm0, (%rsi)                                 #1200.17
        ret                                                     #1200.17
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.10:                        # Preds ..B7.4
                                # Execution count [4.20e-01]
        ucomiss   %xmm3, %xmm5                                  #1205.39
        jp        ..B7.11       # Prob 0%                       #1205.39
        je        ..B7.14       # Prob 16%                      #1205.39
                                # LOE rbx rbp rsi r12 r13 r14 r15 xmm3
..B7.11:                        # Preds ..B7.10
                                # Execution count [3.53e-01]
        divss     %xmm3, %xmm3                                  #1212.77
        movss     %xmm3, (%rsi)                                 #1212.17
        movl      $1, %eax                                      #1213.17
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.12:                        # Preds ..B7.11
                                # Execution count [1.00e+00]
        ret                                                     #1229.12
                                # LOE
..B7.13:                        # Preds ..B7.6
                                # Execution count [3.36e-02]: Infreq
        movss     .L_2il0floatpacket.102(%rip), %xmm0           #1156.111
        mulss     %xmm0, %xmm6                                  #1156.111
        movaps    %xmm6, %xmm4                                  #1156.111
        movaps    %xmm3, %xmm6                                  #1157.25
        jmp       ..B7.8        # Prob 100%                     #1157.25
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm1 xmm4 xmm5 xmm6
..B7.14:                        # Preds ..B7.10
                                # Execution count [6.72e-02]: Infreq
        movss     .L_2il0floatpacket.101(%rip), %xmm0           #1207.41
        movl      $2, %eax                                      #1208.17
        divss     %xmm3, %xmm0                                  #1207.78
        movss     %xmm0, (%rsi)                                 #1207.17
        ret                                                     #1207.17
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.15:                        # Preds ..B7.1
                                # Execution count [1.60e-01]: Infreq
        movb      -17(%rsp), %dl                                #1219.36
        andb      $-128, %dl                                    #1219.36
        cmpb      $-128, %dl                                    #1219.59
        je        ..B7.17       # Prob 16%                      #1219.59
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm5
..B7.16:                        # Preds ..B7.17 ..B7.15
                                # Execution count [1.47e-01]: Infreq
        mulss     %xmm5, %xmm5                                  #1226.38
        movss     %xmm5, (%rsi)                                 #1226.13
        ret                                                     #1226.13
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.17:                        # Preds ..B7.15
                                # Execution count [2.56e-02]: Infreq
        testl     $8388607, -20(%rsp)                           #1219.87
        jne       ..B7.16       # Prob 50%                      #1219.117
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm5
..B7.18:                        # Preds ..B7.17
                                # Execution count [1.28e-02]: Infreq
        movl      $1, %eax                                      #1222.13
        pxor      %xmm1, %xmm1                                  #1221.73
        pxor      %xmm0, %xmm0                                  #1221.73
        divss     %xmm0, %xmm1                                  #1221.73
        movss     %xmm1, (%rsi)                                 #1221.13
        ret                                                     #1221.13
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_slog1p_ha_cout_rare_internal,@function
	.size	__svml_slog1p_ha_cout_rare_internal,.-__svml_slog1p_ha_cout_rare_internal
..LN__svml_slog1p_ha_cout_rare_internal.6:
	.data
# -- End  __svml_slog1p_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_slog1p_ha_data_internal_avx512
	.globl __svml_slog1p_ha_data_internal_avx512
__svml_slog1p_ha_data_internal_avx512:
	.long	1060205056
	.long	1059688960
	.long	1059187712
	.long	1058701824
	.long	1058229248
	.long	1057769472
	.long	1057321984
	.long	1056807936
	.long	1055958016
	.long	1055129600
	.long	1054320640
	.long	1053531136
	.long	1052760064
	.long	1052006400
	.long	1051268096
	.long	1050547200
	.long	1049840640
	.long	1049148416
	.long	1048365056
	.long	1047035904
	.long	1045733376
	.long	1044455424
	.long	1043200000
	.long	1041969152
	.long	1040760832
	.long	1038958592
	.long	1036623872
	.long	1034330112
	.long	1032073216
	.long	1027907584
	.long	1023541248
	.long	1015087104
	.long	901758606
	.long	3071200204
	.long	931108809
	.long	3074069268
	.long	3077535321
	.long	3071146094
	.long	3063010043
	.long	3072147991
	.long	908173938
	.long	3049723733
	.long	925190435
	.long	923601997
	.long	3048768765
	.long	3076457870
	.long	926424291
	.long	3073778483
	.long	3069146713
	.long	912794238
	.long	912483742
	.long	920635797
	.long	3054902185
	.long	3069864633
	.long	922801832
	.long	3033791132
	.long	3076717488
	.long	3076037756
	.long	3072434855
	.long	3077481184
	.long	3066991812
	.long	917116064
	.long	925811956
	.long	900509991
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
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	3196061712
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
	.long	1051373854
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
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.type	__svml_slog1p_ha_data_internal_avx512,@object
	.size	__svml_slog1p_ha_data_internal_avx512,768
	.align 64
	.hidden __svml_slog1p_ha_data_internal
	.globl __svml_slog1p_ha_data_internal
__svml_slog1p_ha_data_internal:
	.long	3266227256
	.long	3107766024
	.long	3266228276
	.long	3107776882
	.long	3266229284
	.long	3109949545
	.long	3266230292
	.long	3108055846
	.long	3266231292
	.long	3106351937
	.long	3266232276
	.long	3109092567
	.long	3266233260
	.long	3107948216
	.long	3266234236
	.long	3107170960
	.long	3266235204
	.long	3106817287
	.long	3266236164
	.long	3106942449
	.long	3266237116
	.long	3107600489
	.long	3266238060
	.long	3108844279
	.long	3266239004
	.long	3106531253
	.long	3266239932
	.long	3109100650
	.long	3266240860
	.long	3108213420
	.long	3266241780
	.long	3108112381
	.long	3266242692
	.long	3108845034
	.long	3266243604
	.long	3106263589
	.long	3266244500
	.long	3108802209
	.long	3266245396
	.long	3108116909
	.long	3266246284
	.long	3108445707
	.long	3266247164
	.long	3109831435
	.long	3266248044
	.long	3108121760
	.long	3266248916
	.long	3107552123
	.long	3266249780
	.long	3108162844
	.long	3266250644
	.long	3105799146
	.long	3266251492
	.long	3108888393
	.long	3266252340
	.long	3109079979
	.long	3266253188
	.long	3106411173
	.long	3266254020
	.long	3109307139
	.long	3266254852
	.long	3109415127
	.long	3266255684
	.long	3106770317
	.long	3266256500
	.long	3109795834
	.long	3266257324
	.long	3105942641
	.long	3266258132
	.long	3107826892
	.long	3266258940
	.long	3107092610
	.long	3266259740
	.long	3107966131
	.long	3266260540
	.long	3106284596
	.long	3266261332
	.long	3106273188
	.long	3266262116
	.long	3107962226
	.long	3266262900
	.long	3107187186
	.long	3266263676
	.long	3108171617
	.long	3266264452
	.long	3106749947
	.long	3266265220
	.long	3107144703
	.long	3266265980
	.long	3109383615
	.long	3266266740
	.long	3109299629
	.long	3266267500
	.long	3106919521
	.long	3266268252
	.long	3106463913
	.long	3266268996
	.long	3107958670
	.long	3266269740
	.long	3107234917
	.long	3266270476
	.long	3108511954
	.long	3266271212
	.long	3107620056
	.long	3266271940
	.long	3108777693
	.long	3266272668
	.long	3107814325
	.long	3266273388
	.long	3108947630
	.long	3266274108
	.long	3108006290
	.long	3266274820
	.long	3109207222
	.long	3266275532
	.long	3108378366
	.long	3266276236
	.long	3109735912
	.long	3266276940
	.long	3109107087
	.long	3266277644
	.long	3106513079
	.long	3266278340
	.long	3106169044
	.long	3266279028
	.long	3108095503
	.long	3266279716
	.long	3108118349
	.long	3266280404
	.long	3106257463
	.long	3266281084
	.long	3106726720
	.long	3266281756
	.long	3109545389
	.long	3266282436
	.long	3106343833
	.long	3266283100
	.long	3109723642
	.long	3266283772
	.long	3107120300
	.long	3266284436
	.long	3106940529
	.long	3266285092
	.long	3109202170
	.long	3266285748
	.long	3109728494
	.long	3266286404
	.long	3108536808
	.long	3266287052
	.long	3109838471
	.long	3266287700
	.long	3109455977
	.long	3266288348
	.long	3107405879
	.long	3266288988
	.long	3107898790
	.long	3266289628
	.long	3106756477
	.long	3266290260
	.long	3108189081
	.long	3266290892
	.long	3108017907
	.long	3266291524
	.long	3106258339
	.long	3266292148
	.long	3107119845
	.long	3266292772
	.long	3106423069
	.long	3266293388
	.long	3108377050
	.long	3266294004
	.long	3108802011
	.long	3266294620
	.long	3107712277
	.long	3266295228
	.long	3109316274
	.long	3266295836
	.long	3109433625
	.long	3266296444
	.long	3108078064
	.long	3266297044
	.long	3109457438
	.long	3266297644
	.long	3109390801
	.long	3266298244
	.long	3107891329
	.long	3266298836
	.long	3109166323
	.long	3266299428
	.long	3109034299
	.long	3266300020
	.long	3107507904
	.long	3266300604
	.long	3108793919
	.long	3266301188
	.long	3108710352
	.long	3266301772
	.long	3107269350
	.long	3266302348
	.long	3108677203
	.long	3266302924
	.long	3108751436
	.long	3266303500
	.long	3107503720
	.long	3266304068
	.long	3109139881
	.long	3266304636
	.long	3109476985
	.long	3266305204
	.long	3108526254
	.long	3266305772
	.long	3106298768
	.long	3266306332
	.long	3106999765
	.long	3266306892
	.long	3106445739
	.long	3266307444
	.long	3108841650
	.long	3266308004
	.long	3105809415
	.long	3266308548
	.long	3109942336
	.long	3266309100
	.long	3108667760
	.long	3266309652
	.long	3106190122
	.long	3266310196
	.long	3106713732
	.long	3266310740
	.long	3106054165
	.long	3266311276
	.long	3108415484
	.long	3266311812
	.long	3109613023
	.long	3266312348
	.long	3109656301
	.long	3266312884
	.long	3108554723
	.long	3266313420
	.long	3106317576
	.long	3266313948
	.long	3107148341
	.long	3266314476
	.long	3106861780
	.long	3266314996
	.long	3109661153
	.long	3266315524
	.long	3107166702
	.long	3266316044
	.long	3107775778
	.long	3266316564
	.long	3107302717
	.long	3266317076
	.long	3109950361
	.long	3266317596
	.long	3107338539
	.long	3266318108
	.long	3107864196
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
	.long	964689920
	.long	964689920
	.long	964689920
	.long	964689920
	.long	964689920
	.long	964689920
	.long	964689920
	.long	964689920
	.long	964689920
	.long	964689920
	.long	964689920
	.long	964689920
	.long	964689920
	.long	964689920
	.long	964689920
	.long	964689920
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
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	2063597568
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	1051372345
	.long	3204448310
	.long	3204448310
	.long	3204448310
	.long	3204448310
	.long	3204448310
	.long	3204448310
	.long	3204448310
	.long	3204448310
	.long	3204448310
	.long	3204448310
	.long	3204448310
	.long	3204448310
	.long	3204448310
	.long	3204448310
	.long	3204448310
	.long	3204448310
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
	.long	998244352
	.long	998244352
	.long	998244352
	.long	998244352
	.long	998244352
	.long	998244352
	.long	998244352
	.long	998244352
	.long	998244352
	.long	998244352
	.long	998244352
	.long	998244352
	.long	998244352
	.long	998244352
	.long	998244352
	.long	998244352
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	3212836863
	.long	2055208960
	.long	2055208960
	.long	2055208960
	.long	2055208960
	.long	2055208960
	.long	2055208960
	.long	2055208960
	.long	2055208960
	.long	2055208960
	.long	2055208960
	.long	2055208960
	.long	2055208960
	.long	2055208960
	.long	2055208960
	.long	2055208960
	.long	2055208960
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
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	901758464
	.long	901758464
	.long	901758464
	.long	901758464
	.long	901758464
	.long	901758464
	.long	901758464
	.long	901758464
	.long	901758464
	.long	901758464
	.long	901758464
	.long	901758464
	.long	901758464
	.long	901758464
	.long	901758464
	.long	901758464
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
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	3196059527
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	1045220287
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	3190475908
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	1041398342
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	3187262718
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	1037844266
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	3187902610
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	1039525906
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	16777216
	.long	25165824
	.long	25165824
	.long	25165824
	.long	25165824
	.long	25165824
	.long	25165824
	.long	25165824
	.long	25165824
	.long	25165824
	.long	25165824
	.long	25165824
	.long	25165824
	.long	25165824
	.long	25165824
	.long	25165824
	.long	25165824
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
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	1060205056
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
	.long	901758606
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
	.type	__svml_slog1p_ha_data_internal,@object
	.size	__svml_slog1p_ha_data_internal,3264
	.align 32
_vmldLnHATab:
	.long	1065353216
	.long	0
	.long	0
	.long	1065091072
	.long	1015087104
	.long	900509991
	.long	1064828928
	.long	1023541248
	.long	925811956
	.long	1064566784
	.long	1027915776
	.long	3084221144
	.long	1064304640
	.long	1032073216
	.long	3066991812
	.long	1064173568
	.long	1033195520
	.long	882149603
	.long	1063911424
	.long	1035468800
	.long	928189163
	.long	1063649280
	.long	1037783040
	.long	927501741
	.long	1063518208
	.long	1038958592
	.long	3076037756
	.long	1063256064
	.long	1040759808
	.long	904405630
	.long	1063124992
	.long	1041361920
	.long	3052231524
	.long	1062862848
	.long	1042581504
	.long	922094799
	.long	1062731776
	.long	1043201024
	.long	3070120623
	.long	1062469632
	.long	1044455424
	.long	3069864633
	.long	1062338560
	.long	1045091328
	.long	3063188516
	.long	1062207488
	.long	1045733376
	.long	3054902185
	.long	1061945344
	.long	1047035904
	.long	920635797
	.long	1061814272
	.long	1047697408
	.long	904920689
	.long	1061683200
	.long	1048365056
	.long	912483742
	.long	1061552128
	.long	1048807936
	.long	3052664405
	.long	1061421056
	.long	1049148416
	.long	912794238
	.long	1061158912
	.long	1049840384
	.long	889474359
	.long	1061027840
	.long	1050191872
	.long	3059868362
	.long	1060896768
	.long	1050546944
	.long	3059256525
	.long	1060765696
	.long	1050905600
	.long	912008988
	.long	1060634624
	.long	1051268352
	.long	912290698
	.long	1060503552
	.long	1051635200
	.long	3037211048
	.long	1060372480
	.long	1052005888
	.long	906226119
	.long	1060241408
	.long	1052380928
	.long	3052480305
	.long	1060110336
	.long	1052760064
	.long	3048768765
	.long	1059979264
	.long	1053143552
	.long	3049975450
	.long	1059848192
	.long	1053531392
	.long	894485718
	.long	1059717120
	.long	1053923840
	.long	897598623
	.long	1059586048
	.long	1054320896
	.long	907355277
	.long	1059586048
	.long	1054320896
	.long	907355277
	.long	1059454976
	.long	1054722816
	.long	881705073
	.long	1059323904
	.long	1055129600
	.long	3049723733
	.long	1059192832
	.long	1055541248
	.long	890353599
	.long	1059061760
	.long	1055958016
	.long	908173938
	.long	1059061760
	.long	1055958016
	.long	908173938
	.long	1058930688
	.long	1056380160
	.long	883644938
	.long	1058799616
	.long	1056807680
	.long	3052015799
	.long	1058668544
	.long	1057102592
	.long	884897284
	.long	1058668544
	.long	1057102592
	.long	884897284
	.long	1058537472
	.long	1057321920
	.long	3037632470
	.long	1058406400
	.long	1057544128
	.long	865017195
	.long	1058275328
	.long	1057769344
	.long	3042936546
	.long	1058275328
	.long	1057769344
	.long	3042936546
	.long	1058144256
	.long	1057997568
	.long	903344518
	.long	1058013184
	.long	1058228992
	.long	897862967
	.long	1058013184
	.long	1058228992
	.long	897862967
	.long	1057882112
	.long	1058463680
	.long	3047822280
	.long	1057882112
	.long	1058463680
	.long	3047822280
	.long	1057751040
	.long	1058701632
	.long	883793293
	.long	1057619968
	.long	1058943040
	.long	851667963
	.long	1057619968
	.long	1058943040
	.long	851667963
	.long	1057488896
	.long	1059187968
	.long	3000004036
	.long	1057488896
	.long	1059187968
	.long	3000004036
	.long	1057357824
	.long	1059436544
	.long	3047430717
	.long	1057357824
	.long	1059436544
	.long	3047430717
	.long	1057226752
	.long	1059688832
	.long	3043802308
	.long	1057226752
	.long	1059688832
	.long	3043802308
	.long	1057095680
	.long	1059944960
	.long	876113044
	.long	1057095680
	.long	1059944960
	.long	876113044
	.long	1056964608
	.long	1060205056
	.long	901758606
	.long	1060205056
	.long	901758606
	.long	1207959616
	.long	1174405120
	.long	1008730112
	.long	1400897536
	.long	0
	.long	1065353216
	.long	3204448256
	.long	1051372203
	.long	3196059648
	.long	1045220557
	.long	3190467243
	.long	1041387009
	.long	3187672480
	.type	_vmldLnHATab,@object
	.size	_vmldLnHATab,840
	.space 8, 0x00 	# pad
	.align 16
.L_2il0floatpacket.23:
	.long	0x4b400000,0x4b400000,0x4b400000,0x4b400000
	.type	.L_2il0floatpacket.23,@object
	.size	.L_2il0floatpacket.23,16
	.align 4
.L_2il0floatpacket.92:
	.long	0x53800000
	.type	.L_2il0floatpacket.92,@object
	.size	.L_2il0floatpacket.92,4
	.align 4
.L_2il0floatpacket.93:
	.long	0x3c200000
	.type	.L_2il0floatpacket.93,@object
	.size	.L_2il0floatpacket.93,4
	.align 4
.L_2il0floatpacket.94:
	.long	0xbf000000
	.type	.L_2il0floatpacket.94,@object
	.size	.L_2il0floatpacket.94,4
	.align 4
.L_2il0floatpacket.95:
	.long	0x3eaaaaab
	.type	.L_2il0floatpacket.95,@object
	.size	.L_2il0floatpacket.95,4
	.align 4
.L_2il0floatpacket.96:
	.long	0xbe800000
	.type	.L_2il0floatpacket.96,@object
	.size	.L_2il0floatpacket.96,4
	.align 4
.L_2il0floatpacket.97:
	.long	0x3e4ccccd
	.type	.L_2il0floatpacket.97,@object
	.size	.L_2il0floatpacket.97,4
	.align 4
.L_2il0floatpacket.98:
	.long	0xbe2aaaab
	.type	.L_2il0floatpacket.98,@object
	.size	.L_2il0floatpacket.98,4
	.align 4
.L_2il0floatpacket.99:
	.long	0x3e124e01
	.type	.L_2il0floatpacket.99,@object
	.size	.L_2il0floatpacket.99,4
	.align 4
.L_2il0floatpacket.100:
	.long	0xbe0005a0
	.type	.L_2il0floatpacket.100,@object
	.size	.L_2il0floatpacket.100,4
	.align 4
.L_2il0floatpacket.101:
	.long	0xbf800000
	.type	.L_2il0floatpacket.101,@object
	.size	.L_2il0floatpacket.101,4
	.align 4
.L_2il0floatpacket.102:
	.long	0x3f317218
	.type	.L_2il0floatpacket.102,@object
	.size	.L_2il0floatpacket.102,4
	.align 4
.L_2il0floatpacket.103:
	.long	0x3f317200
	.type	.L_2il0floatpacket.103,@object
	.size	.L_2il0floatpacket.103,4
	.align 4
.L_2il0floatpacket.104:
	.long	0x35bfbe8e
	.type	.L_2il0floatpacket.104,@object
	.size	.L_2il0floatpacket.104,4
	.align 4
.L_2il0floatpacket.105:
	.long	0x48000040
	.type	.L_2il0floatpacket.105,@object
	.size	.L_2il0floatpacket.105,4
	.align 4
.L_2il0floatpacket.106:
	.long	0x46000000
	.type	.L_2il0floatpacket.106,@object
	.size	.L_2il0floatpacket.106,4
	.align 4
.L_2il0floatpacket.107:
	.long	0x3f800000
	.type	.L_2il0floatpacket.107,@object
	.size	.L_2il0floatpacket.107,4
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
