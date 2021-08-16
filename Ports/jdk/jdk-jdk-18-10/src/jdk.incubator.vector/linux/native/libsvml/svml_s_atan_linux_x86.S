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
	.file "svml_s_atan.c"
	.text
..TXTST0:
.L_2__routine_start___svml_atanf8_ha_e9_0:
# -- Begin  __svml_atanf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atanf8_ha_e9
# --- __svml_atanf8_ha_e9(__m256)
__svml_atanf8_ha_e9:
# parameter 1: %ymm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #221.1
        .byte     15                                            #402.546
        .byte     30                                            #402.546
        .byte     250                                           #402.546
	.cfi_startproc
..___tag_value___svml_atanf8_ha_e9.1:
..L2:
                                                          #221.1
        pushq     %rbp                                          #221.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #221.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #221.1
        subq      $192, %rsp                                    #221.1
        vmovaps   %ymm0, %ymm9                                  #221.1
        vandps    192+__svml_satan_ha_data_internal(%rip), %ymm9, %ymm7 #290.14
        vmovups   1920+__svml_satan_ha_data_internal(%rip), %xmm10 #294.30
        vmovups   1984+__svml_satan_ha_data_internal(%rip), %xmm11 #295.30
        vmovups   320+__svml_satan_ha_data_internal(%rip), %xmm0 #300.28
        vandps    128+__svml_satan_ha_data_internal(%rip), %ymm9, %ymm8 #292.14
        vpsubd    %xmm10, %xmm7, %xmm1                          #296.22
        vextractf128 $1, %ymm7, %xmm2                           #293.125
        vpcmpgtd  %xmm11, %xmm1, %xmm3                          #297.36
        vpcmpeqd  %xmm11, %xmm1, %xmm4                          #297.95
        vpor      %xmm4, %xmm3, %xmm6                           #297.22
        vpsubd    %xmm10, %xmm2, %xmm5                          #296.103
        vpcmpgtd  %xmm11, %xmm5, %xmm12                         #297.186
        vpcmpeqd  %xmm11, %xmm5, %xmm13                         #297.245
        vpor      %xmm13, %xmm12, %xmm14                        #297.172
        vpxor     %xmm10, %xmm10, %xmm10                        #298.62
        vmovups   256+__svml_satan_ha_data_internal(%rip), %xmm4 #299.28
        vpcmpgtd  %xmm2, %xmm0, %xmm11                          #306.94
        vpackssdw %xmm14, %xmm6, %xmm15                         #298.79
        vpcmpgtd  %xmm7, %xmm4, %xmm5                           #305.26
        vmovups   384+__svml_satan_ha_data_internal(%rip), %xmm6 #301.28
        vpcmpgtd  %xmm2, %xmm4, %xmm4                           #305.94
        vmovups   448+__svml_satan_ha_data_internal(%rip), %xmm3 #302.28
        vpcmpgtd  %xmm7, %xmm6, %xmm13                          #307.26
        vpacksswb %xmm10, %xmm15, %xmm1                         #298.62
        vpcmpgtd  %xmm7, %xmm0, %xmm10                          #306.26
        vpcmpgtd  %xmm2, %xmm6, %xmm14                          #307.94
        vpcmpgtd  %xmm3, %xmm7, %xmm15                          #308.26
        vpmovmskb %xmm1, %edx                                   #298.43
        vpcmpgtd  %xmm3, %xmm2, %xmm1                           #308.94
        vmovups   1728+__svml_satan_ha_data_internal(%rip), %xmm12 #303.30
        vpandn    %xmm11, %xmm4, %xmm3                          #310.97
        vpandn    %xmm14, %xmm11, %xmm11                        #312.97
        vpandn    %xmm10, %xmm5, %xmm2                          #310.27
        vpandn    %xmm13, %xmm10, %xmm10                        #312.27
        vinsertf128 $1, %xmm4, %ymm5, %ymm6                     #315.21
        vinsertf128 $1, %xmm11, %ymm10, %ymm4                   #317.21
        vpxor     %xmm12, %xmm15, %xmm10                        #313.26
        vpxor     %xmm12, %xmm1, %xmm12                         #313.96
        vpandn    %xmm10, %xmm13, %xmm13                        #314.27
        vpandn    %xmm12, %xmm14, %xmm14                        #314.97
        vandps    %ymm6, %ymm7, %ymm12                          #324.18
        vinsertf128 $1, %xmm3, %ymm2, %ymm5                     #316.21
        vinsertf128 $1, %xmm14, %ymm13, %ymm3                   #318.21
        vinsertf128 $1, %xmm1, %ymm15, %ymm1                    #319.21
        vmovups   768+__svml_satan_ha_data_internal(%rip), %ymm15 #321.50
        vmovups   832+__svml_satan_ha_data_internal(%rip), %ymm14 #322.50
        vmovups   896+__svml_satan_ha_data_internal(%rip), %ymm13 #323.50
        vsubps    %ymm15, %ymm7, %ymm0                          #325.18
        vsubps    %ymm14, %ymm7, %ymm10                         #327.18
        vsubps    %ymm13, %ymm7, %ymm2                          #329.18
        vmulps    %ymm15, %ymm7, %ymm15                         #339.33
        vmulps    %ymm14, %ymm7, %ymm14                         #341.33
        vmulps    %ymm13, %ymm7, %ymm13                         #343.33
        vandps    %ymm5, %ymm0, %ymm11                          #326.18
        vandps    %ymm4, %ymm10, %ymm10                         #328.18
        vandps    %ymm3, %ymm2, %ymm2                           #330.18
        vorps     %ymm11, %ymm12, %ymm11                        #333.18
        vorps     %ymm2, %ymm10, %ymm12                         #334.18
        vorps     %ymm12, %ymm11, %ymm10                        #335.18
        vmovups   __svml_satan_ha_data_internal(%rip), %ymm12   #338.18
        vandps    64+__svml_satan_ha_data_internal(%rip), %ymm1, %ymm0 #332.18
        vorps     %ymm0, %ymm10, %ymm10                         #336.17
        vandps    %ymm6, %ymm12, %ymm6                          #338.18
        vcvtps2pd %xmm10, %ymm0                                 #337.27
        vextractf128 $1, %ymm10, %xmm2                          #337.109
        vaddps    %ymm15, %ymm12, %ymm10                        #339.18
        vcvtps2pd %xmm2, %ymm2                                  #337.92
        vandps    %ymm5, %ymm10, %ymm11                         #340.18
        vaddps    %ymm14, %ymm12, %ymm10                        #341.18
        vaddps    %ymm13, %ymm12, %ymm12                        #343.18
        vandps    %ymm4, %ymm10, %ymm10                         #342.18
        vandps    %ymm3, %ymm12, %ymm13                         #344.18
        vandps    %ymm1, %ymm7, %ymm12                          #345.18
        vorps     %ymm11, %ymm6, %ymm7                          #346.18
        vorps     %ymm13, %ymm10, %ymm10                        #347.18
        vorps     %ymm10, %ymm7, %ymm11                         #348.18
        vorps     %ymm12, %ymm11, %ymm6                         #349.17
        vandps    960+__svml_satan_ha_data_internal(%rip), %ymm5, %ymm13 #355.18
        vandps    1088+__svml_satan_ha_data_internal(%rip), %ymm4, %ymm14 #356.18
        vandps    1216+__svml_satan_ha_data_internal(%rip), %ymm3, %ymm10 #357.18
        vandps    1344+__svml_satan_ha_data_internal(%rip), %ymm1, %ymm11 #358.18
        vandps    1024+__svml_satan_ha_data_internal(%rip), %ymm5, %ymm5 #379.18
        vandps    1152+__svml_satan_ha_data_internal(%rip), %ymm4, %ymm4 #380.18
        vorps     %ymm4, %ymm5, %ymm4                           #383.18
        vandps    1280+__svml_satan_ha_data_internal(%rip), %ymm3, %ymm3 #381.18
        vandps    1408+__svml_satan_ha_data_internal(%rip), %ymm1, %ymm1 #382.18
        vorps     %ymm1, %ymm3, %ymm1                           #384.18
        vcvtps2pd %xmm6, %ymm15                                 #350.27
        vextractf128 $1, %ymm6, %xmm7                           #350.109
        vrcpps    %ymm6, %ymm6                                  #362.22
        vcvtps2pd %xmm7, %ymm12                                 #350.92
        vorps     %ymm14, %ymm13, %ymm7                         #359.18
        vorps     %ymm11, %ymm10, %ymm13                        #360.18
        vorps     %ymm13, %ymm7, %ymm10                         #361.17
        vcvtps2pd %xmm6, %ymm14                                 #363.28
        vextractf128 $1, %ymm6, %xmm11                          #363.112
        vmulpd    %ymm14, %ymm15, %ymm15                        #365.53
        vcvtps2pd %xmm11, %ymm7                                 #363.95
        vmovupd   1856+__svml_satan_ha_data_internal(%rip), %ymm11 #364.59
        vmulpd    %ymm7, %ymm12, %ymm12                         #365.130
        vsubpd    %ymm15, %ymm11, %ymm6                         #365.27
        vsubpd    %ymm12, %ymm11, %ymm13                        #365.104
        vmulpd    %ymm6, %ymm14, %ymm15                         #366.42
        vmulpd    %ymm6, %ymm6, %ymm6                           #367.38
        vaddpd    %ymm15, %ymm14, %ymm12                        #366.27
        vaddpd    %ymm6, %ymm11, %ymm6                          #367.23
        vmulpd    %ymm13, %ymm7, %ymm14                         #366.123
        vmulpd    %ymm13, %ymm13, %ymm13                        #367.119
        vmulpd    %ymm6, %ymm12, %ymm12                         #368.23
        vaddpd    %ymm14, %ymm7, %ymm7                          #366.108
        vaddpd    %ymm13, %ymm11, %ymm11                        #367.104
        vmulpd    %ymm11, %ymm7, %ymm7                          #368.72
        vmulpd    %ymm12, %ymm0, %ymm11                         #369.23
        vmulpd    %ymm7, %ymm2, %ymm0                           #369.68
        vmovupd   1792+__svml_satan_ha_data_internal(%rip), %ymm2 #370.62
        vandpd    %ymm2, %ymm11, %ymm7                          #371.21
        vandpd    %ymm2, %ymm0, %ymm12                          #371.77
        vcvtpd2ps %ymm7, %xmm2                                  #372.65
        vcvtpd2ps %ymm12, %xmm6                                 #372.101
        vsubpd    %ymm7, %ymm11, %ymm11                         #373.21
        vinsertf128 $1, %xmm6, %ymm2, %ymm2                     #372.19
        vmulps    %ymm2, %ymm2, %ymm14                          #387.14
        vsubpd    %ymm12, %ymm0, %ymm6                          #373.70
        vcvtpd2ps %ymm11, %xmm0                                 #374.65
        vmulps    %ymm14, %ymm14, %ymm5                         #388.14
        vcvtpd2ps %ymm6, %xmm7                                  #374.101
        vmulps    1472+__svml_satan_ha_data_internal(%rip), %ymm5, %ymm3 #393.29
        vmulps    1536+__svml_satan_ha_data_internal(%rip), %ymm5, %ymm6 #394.29
        vaddps    1664+__svml_satan_ha_data_internal(%rip), %ymm6, %ymm12 #394.14
        vinsertf128 $1, %xmm7, %ymm0, %ymm11                    #374.19
        vaddps    1600+__svml_satan_ha_data_internal(%rip), %ymm3, %ymm7 #393.14
        vmulps    %ymm7, %ymm14, %ymm13                         #395.28
        vaddps    %ymm13, %ymm12, %ymm15                        #395.13
        vmulps    %ymm15, %ymm14, %ymm3                         #396.13
        vorps     %ymm1, %ymm4, %ymm0                           #385.17
        vmulps    %ymm3, %ymm2, %ymm1                           #397.29
        vaddps    %ymm0, %ymm11, %ymm0                          #386.19
        vaddps    %ymm1, %ymm0, %ymm4                           #397.14
        vaddps    %ymm4, %ymm2, %ymm2                           #398.14
        vaddps    %ymm2, %ymm10, %ymm10                         #399.18
        vorps     %ymm8, %ymm10, %ymm0                          #400.14
        testb     %dl, %dl                                      #402.52
        jne       ..B1.3        # Prob 5%                       #402.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm9
..B1.2:                         # Preds ..B1.3 ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #405.12
        popq      %rbp                                          #405.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #405.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm9, 64(%rsp)                               #402.196
        vmovups   %ymm0, 128(%rsp)                              #402.270
        testl     %edx, %edx                                    #402.374
        je        ..B1.2        # Prob 95%                      #402.374
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #402.454
                                # LOE rbx r12 r13 r14 r15 eax edx
..B1.13:                        # Preds ..B1.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #402.517
        jc        ..B1.10       # Prob 5%                       #402.517
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #402.470
        cmpl      $8, %r12d                                     #402.465
        jl        ..B1.7        # Prob 82%                      #402.465
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #402.675
        jmp       ..B1.2        # Prob 100%                     #402.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #402.546
        lea       128(%rsp,%r12,4), %rsi                        #402.546
..___tag_value___svml_atanf8_ha_e9.19:
#       __svml_satan_ha_cout_rare_internal(const float *, float *)
        call      __svml_satan_ha_cout_rare_internal            #402.546
..___tag_value___svml_atanf8_ha_e9.20:
        jmp       ..B1.8        # Prob 100%                     #402.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_atanf8_ha_e9,@function
	.size	__svml_atanf8_ha_e9,.-__svml_atanf8_ha_e9
..LN__svml_atanf8_ha_e9.0:
	.data
# -- End  __svml_atanf8_ha_e9
	.text
.L_2__routine_start___svml_atanf4_ha_e9_1:
# -- Begin  __svml_atanf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atanf4_ha_e9
# --- __svml_atanf4_ha_e9(__m128)
__svml_atanf4_ha_e9:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #410.1
        .byte     15                                            #591.540
        .byte     30                                            #591.540
        .byte     250                                           #591.540
	.cfi_startproc
..___tag_value___svml_atanf4_ha_e9.22:
..L23:
                                                         #410.1
        pushq     %rbp                                          #410.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #410.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #410.1
        subq      $192, %rsp                                    #410.1
        vmovaps   %xmm0, %xmm11                                 #410.1
        vandps    192+__svml_satan_ha_data_internal(%rip), %xmm11, %xmm3 #479.14
        vmovups   1984+__svml_satan_ha_data_internal(%rip), %xmm9 #484.30
        vpsubd    1920+__svml_satan_ha_data_internal(%rip), %xmm3, %xmm7 #485.22
        vpcmpgtd  %xmm9, %xmm7, %xmm6                           #486.36
        vpcmpeqd  %xmm9, %xmm7, %xmm8                           #486.95
        vmovups   256+__svml_satan_ha_data_internal(%rip), %xmm2 #488.28
        vpor      %xmm8, %xmm6, %xmm4                           #486.22
        vmovups   320+__svml_satan_ha_data_internal(%rip), %xmm1 #489.28
        vpcmpgtd  %xmm3, %xmm2, %xmm0                           #494.26
        vmovups   384+__svml_satan_ha_data_internal(%rip), %xmm13 #490.28
        vpcmpgtd  %xmm3, %xmm1, %xmm12                          #495.26
        vpcmpgtd  448+__svml_satan_ha_data_internal(%rip), %xmm3, %xmm7 #497.26
        vpcmpgtd  %xmm3, %xmm13, %xmm9                          #496.26
        vmovmskps %xmm4, %edx                                   #487.44
        vpandn    %xmm9, %xmm12, %xmm6                          #501.27
        vpxor     1728+__svml_satan_ha_data_internal(%rip), %xmm7, %xmm5 #502.26
        vpandn    %xmm12, %xmm0, %xmm8                          #499.27
        vmovups   768+__svml_satan_ha_data_internal(%rip), %xmm1 #510.47
        vpandn    %xmm5, %xmm9, %xmm9                           #503.27
        vmovups   832+__svml_satan_ha_data_internal(%rip), %xmm2 #511.47
        vandps    %xmm0, %xmm3, %xmm14                          #513.18
        vmovups   896+__svml_satan_ha_data_internal(%rip), %xmm4 #512.47
        vsubps    %xmm1, %xmm3, %xmm15                          #514.18
        vsubps    %xmm2, %xmm3, %xmm13                          #516.18
        vmulps    %xmm2, %xmm3, %xmm2                           #530.30
        vsubps    %xmm4, %xmm3, %xmm5                           #518.18
        vmulps    %xmm4, %xmm3, %xmm4                           #532.30
        vandps    %xmm8, %xmm15, %xmm12                         #515.18
        vandps    %xmm6, %xmm13, %xmm13                         #517.18
        vandps    %xmm9, %xmm5, %xmm5                           #519.18
        vorps     %xmm12, %xmm14, %xmm14                        #522.18
        vorps     %xmm5, %xmm13, %xmm12                         #523.18
        vorps     %xmm12, %xmm14, %xmm13                        #524.18
        vmovups   __svml_satan_ha_data_internal(%rip), %xmm14   #527.18
        vandps    %xmm0, %xmm14, %xmm12                         #527.18
        vmulps    %xmm1, %xmm3, %xmm0                           #528.30
        vandps    64+__svml_satan_ha_data_internal(%rip), %xmm7, %xmm15 #521.18
        vaddps    %xmm0, %xmm14, %xmm1                          #528.18
        vorps     %xmm15, %xmm13, %xmm15                        #525.17
        vandps    %xmm8, %xmm1, %xmm13                          #529.18
        vcvtps2pd %xmm15, %ymm5                                 #526.27
        vaddps    %xmm2, %xmm14, %xmm15                         #530.18
        vaddps    %xmm4, %xmm14, %xmm14                         #532.18
        vandps    %xmm6, %xmm15, %xmm15                         #531.18
        vandps    %xmm9, %xmm14, %xmm0                          #533.18
        vandps    %xmm7, %xmm3, %xmm14                          #534.18
        vorps     %xmm13, %xmm12, %xmm3                         #535.18
        vorps     %xmm0, %xmm15, %xmm13                         #536.18
        vorps     %xmm13, %xmm3, %xmm12                         #537.18
        vandps    960+__svml_satan_ha_data_internal(%rip), %xmm8, %xmm0 #544.18
        vorps     %xmm14, %xmm12, %xmm12                        #538.17
        vandps    1088+__svml_satan_ha_data_internal(%rip), %xmm6, %xmm1 #545.18
        vorps     %xmm1, %xmm0, %xmm4                           #548.18
        vrcpps    %xmm12, %xmm0                                 #551.22
        vcvtps2pd %xmm12, %ymm14                                #539.27
        vcvtps2pd %xmm0, %ymm1                                  #552.28
        vandps    1216+__svml_satan_ha_data_internal(%rip), %xmm9, %xmm2 #546.18
        vandps    1344+__svml_satan_ha_data_internal(%rip), %xmm7, %xmm3 #547.18
        vorps     %xmm3, %xmm2, %xmm15                          #549.18
        vmulpd    %ymm1, %ymm14, %ymm14                         #554.49
        vorps     %xmm15, %xmm4, %xmm13                         #550.17
        vmovupd   1856+__svml_satan_ha_data_internal(%rip), %ymm4 #553.57
        vandps    1024+__svml_satan_ha_data_internal(%rip), %xmm8, %xmm8 #568.18
        vandps    1152+__svml_satan_ha_data_internal(%rip), %xmm6, %xmm6 #569.18
        vandps    1280+__svml_satan_ha_data_internal(%rip), %xmm9, %xmm9 #570.18
        vorps     %xmm6, %xmm8, %xmm6                           #572.18
        vandps    1408+__svml_satan_ha_data_internal(%rip), %xmm7, %xmm7 #571.18
        vsubpd    %ymm14, %ymm4, %ymm3                          #554.25
        vorps     %xmm7, %xmm9, %xmm7                           #573.18
        vandps    128+__svml_satan_ha_data_internal(%rip), %xmm11, %xmm10 #481.14
        vmulpd    %ymm3, %ymm1, %ymm2                           #555.40
        vmulpd    %ymm3, %ymm3, %ymm15                          #556.36
        vaddpd    %ymm2, %ymm1, %ymm12                          #555.25
        vaddpd    %ymm15, %ymm4, %ymm0                          #556.21
        vmulpd    %ymm0, %ymm12, %ymm1                          #557.21
        vmulpd    %ymm1, %ymm5, %ymm5                           #558.21
        vandpd    1792+__svml_satan_ha_data_internal(%rip), %ymm5, %ymm0 #560.19
        vcvtpd2ps %ymm0, %xmm12                                 #561.19
        vsubpd    %ymm0, %ymm5, %ymm1                           #562.19
        vorps     %xmm7, %xmm6, %xmm0                           #574.17
        vmulps    %xmm12, %xmm12, %xmm14                        #576.14
        vcvtpd2ps %ymm1, %xmm2                                  #563.19
        vmulps    %xmm14, %xmm14, %xmm3                         #577.14
        vaddps    %xmm0, %xmm2, %xmm0                           #575.19
        vmulps    1472+__svml_satan_ha_data_internal(%rip), %xmm3, %xmm1 #582.26
        vmulps    1536+__svml_satan_ha_data_internal(%rip), %xmm3, %xmm4 #583.26
        vaddps    1600+__svml_satan_ha_data_internal(%rip), %xmm1, %xmm5 #582.14
        vaddps    1664+__svml_satan_ha_data_internal(%rip), %xmm4, %xmm8 #583.14
        vmulps    %xmm5, %xmm14, %xmm9                          #584.25
        vaddps    %xmm9, %xmm8, %xmm15                          #584.13
        vmulps    %xmm15, %xmm14, %xmm1                         #585.13
        vmulps    %xmm1, %xmm12, %xmm2                          #586.26
        vaddps    %xmm2, %xmm0, %xmm3                           #586.14
        vaddps    %xmm3, %xmm12, %xmm12                         #587.14
        vaddps    %xmm12, %xmm13, %xmm13                        #588.18
        vorps     %xmm10, %xmm13, %xmm0                         #589.14
        testl     %edx, %edx                                    #591.52
        jne       ..B2.3        # Prob 5%                       #591.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm11
..B2.13:                        # Preds ..B2.3 ..B2.1
                                # Execution count [9.50e-01]
        vzeroupper                                              #
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.2:                         # Preds ..B2.9 ..B2.13
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #594.12
        popq      %rbp                                          #594.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #594.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm11, 64(%rsp)                              #591.193
        vmovups   %xmm0, 128(%rsp)                              #591.264
        je        ..B2.13       # Prob 95%                      #591.368
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #591.448
                                # LOE rbx r12 r13 r14 r15 eax edx
..B2.15:                        # Preds ..B2.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #591.511
        jc        ..B2.10       # Prob 5%                       #591.511
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #591.464
        cmpl      $4, %r12d                                     #591.459
        jl        ..B2.7        # Prob 82%                      #591.459
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #591.666
        jmp       ..B2.2        # Prob 100%                     #591.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #591.540
        lea       128(%rsp,%r12,4), %rsi                        #591.540
..___tag_value___svml_atanf4_ha_e9.40:
#       __svml_satan_ha_cout_rare_internal(const float *, float *)
        call      __svml_satan_ha_cout_rare_internal            #591.540
..___tag_value___svml_atanf4_ha_e9.41:
        jmp       ..B2.8        # Prob 100%                     #591.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_atanf4_ha_e9,@function
	.size	__svml_atanf4_ha_e9,.-__svml_atanf4_ha_e9
..LN__svml_atanf4_ha_e9.1:
	.data
# -- End  __svml_atanf4_ha_e9
	.text
.L_2__routine_start___svml_atanf4_ha_l9_2:
# -- Begin  __svml_atanf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atanf4_ha_l9
# --- __svml_atanf4_ha_l9(__m128)
__svml_atanf4_ha_l9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #599.1
        .byte     15                                            #775.540
        .byte     30                                            #775.540
        .byte     250                                           #775.540
	.cfi_startproc
..___tag_value___svml_atanf4_ha_l9.43:
..L44:
                                                         #599.1
        pushq     %rbp                                          #599.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #599.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #599.1
        subq      $192, %rsp                                    #599.1
        vmovaps   %xmm0, %xmm12                                 #599.1
        vandps    192+__svml_satan_ha_data_internal(%rip), %xmm12, %xmm3 #668.14
        vmovups   1984+__svml_satan_ha_data_internal(%rip), %xmm6 #673.26
        vpsubd    1920+__svml_satan_ha_data_internal(%rip), %xmm3, %xmm8 #674.18
        vpcmpgtd  %xmm6, %xmm8, %xmm7                           #675.32
        vpcmpeqd  %xmm6, %xmm8, %xmm9                           #675.83
        vmovups   256+__svml_satan_ha_data_internal(%rip), %xmm2 #677.24
        vpor      %xmm9, %xmm7, %xmm4                           #675.18
        vmovups   320+__svml_satan_ha_data_internal(%rip), %xmm1 #678.24
        vpcmpgtd  %xmm3, %xmm2, %xmm0                           #683.22
        vmovups   384+__svml_satan_ha_data_internal(%rip), %xmm14 #679.24
        vpcmpgtd  %xmm3, %xmm1, %xmm13                          #684.22
        vpcmpgtd  448+__svml_satan_ha_data_internal(%rip), %xmm3, %xmm8 #686.22
        vpcmpgtd  %xmm3, %xmm14, %xmm6                          #685.22
        vmovmskps %xmm4, %edx                                   #676.44
        vpandn    %xmm6, %xmm13, %xmm7                          #690.23
        vpxor     1728+__svml_satan_ha_data_internal(%rip), %xmm8, %xmm10 #691.22
        vpandn    %xmm13, %xmm0, %xmm9                          #688.23
        vmovups   768+__svml_satan_ha_data_internal(%rip), %xmm1 #699.47
        vpandn    %xmm10, %xmm6, %xmm6                          #692.23
        vmovups   832+__svml_satan_ha_data_internal(%rip), %xmm2 #700.47
        vandps    %xmm0, %xmm3, %xmm15                          #702.18
        vmovups   896+__svml_satan_ha_data_internal(%rip), %xmm4 #701.47
        vsubps    %xmm1, %xmm3, %xmm5                           #703.18
        vsubps    %xmm2, %xmm3, %xmm14                          #705.18
        vsubps    %xmm4, %xmm3, %xmm10                          #707.18
        vandps    %xmm9, %xmm5, %xmm13                          #704.18
        vandps    %xmm7, %xmm14, %xmm14                         #706.18
        vandps    %xmm6, %xmm10, %xmm10                         #708.18
        vorps     %xmm13, %xmm15, %xmm15                        #711.18
        vorps     %xmm10, %xmm14, %xmm13                        #712.18
        vandps    64+__svml_satan_ha_data_internal(%rip), %xmm8, %xmm5 #710.18
        vorps     %xmm13, %xmm15, %xmm14                        #713.18
        vorps     %xmm5, %xmm14, %xmm10                         #714.17
        vmovups   __svml_satan_ha_data_internal(%rip), %xmm5    #715.18
        vfmadd213ps %xmm5, %xmm3, %xmm1                         #716.18
        vandps    %xmm0, %xmm5, %xmm15                          #715.18
        vfmadd213ps %xmm5, %xmm3, %xmm2                         #718.18
        vfmadd213ps %xmm5, %xmm3, %xmm4                         #720.18
        vandps    128+__svml_satan_ha_data_internal(%rip), %xmm12, %xmm11 #670.14
        vandps    %xmm9, %xmm1, %xmm13                          #717.18
        vandps    %xmm8, %xmm3, %xmm1                           #722.18
        vandps    %xmm7, %xmm2, %xmm14                          #719.18
        vorps     %xmm13, %xmm15, %xmm3                         #723.18
        vandps    %xmm6, %xmm4, %xmm0                           #721.18
        vorps     %xmm0, %xmm14, %xmm14                         #724.18
        vorps     %xmm14, %xmm3, %xmm13                         #725.18
        vandps    960+__svml_satan_ha_data_internal(%rip), %xmm9, %xmm0 #731.18
        vorps     %xmm1, %xmm13, %xmm15                         #726.17
        vandps    1088+__svml_satan_ha_data_internal(%rip), %xmm7, %xmm2 #732.18
        vorps     %xmm2, %xmm0, %xmm14                          #735.18
        vrcpps    %xmm15, %xmm0                                 #738.18
        vfnmadd231ps %xmm0, %xmm15, %xmm5                       #741.21
        vandps    1216+__svml_satan_ha_data_internal(%rip), %xmm6, %xmm3 #733.18
        vandps    1344+__svml_satan_ha_data_internal(%rip), %xmm8, %xmm4 #734.18
        vorps     %xmm4, %xmm3, %xmm13                          #736.18
        vorps     %xmm13, %xmm14, %xmm14                        #737.17
        vfmadd213ps %xmm5, %xmm5, %xmm5                         #742.21
        vmovups   1472+__svml_satan_ha_data_internal(%rip), %xmm4 #762.47
        vmovups   1536+__svml_satan_ha_data_internal(%rip), %xmm1 #763.47
        vandps    1024+__svml_satan_ha_data_internal(%rip), %xmm9, %xmm9 #752.18
        vandps    1152+__svml_satan_ha_data_internal(%rip), %xmm7, %xmm7 #753.18
        vfmadd213ps %xmm0, %xmm5, %xmm0                         #743.22
        vandps    1280+__svml_satan_ha_data_internal(%rip), %xmm6, %xmm6 #754.18
        vandps    1408+__svml_satan_ha_data_internal(%rip), %xmm8, %xmm8 #755.18
        vmulps    %xmm0, %xmm10, %xmm13                         #745.19
        vmulps    %xmm13, %xmm13, %xmm3                         #760.14
        vfnmadd213ps %xmm10, %xmm13, %xmm15                     #746.19
        vorps     %xmm7, %xmm9, %xmm10                          #756.18
        vmulps    %xmm3, %xmm3, %xmm2                           #761.14
        vmulps    %xmm15, %xmm0, %xmm0                          #747.19
        vfmadd213ps 1600+__svml_satan_ha_data_internal(%rip), %xmm2, %xmm4 #766.14
        vfmadd213ps 1664+__svml_satan_ha_data_internal(%rip), %xmm1, %xmm2 #767.14
        vorps     %xmm8, %xmm6, %xmm15                          #757.18
        vorps     %xmm15, %xmm10, %xmm6                         #758.17
        vaddps    %xmm0, %xmm6, %xmm5                           #759.19
        vfmadd213ps %xmm2, %xmm3, %xmm4                         #768.13
        vmulps    %xmm4, %xmm3, %xmm7                           #769.13
        vfmadd213ps %xmm5, %xmm13, %xmm7                        #770.14
        vaddps    %xmm7, %xmm13, %xmm8                          #771.14
        vaddps    %xmm8, %xmm14, %xmm9                          #772.18
        vorps     %xmm11, %xmm9, %xmm0                          #773.14
        testl     %edx, %edx                                    #775.52
        jne       ..B3.3        # Prob 5%                       #775.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm12
..B3.2:                         # Preds ..B3.3 ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #778.12
        popq      %rbp                                          #778.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #778.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm12, 64(%rsp)                              #775.193
        vmovups   %xmm0, 128(%rsp)                              #775.264
        je        ..B3.2        # Prob 95%                      #775.368
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #775.448
        movq      %r12, 8(%rsp)                                 #775.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #775.448
        movq      %r13, (%rsp)                                  #775.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #775.448
                                # LOE rbx r12 r14 r15 r13d
..B3.7:                         # Preds ..B3.8 ..B3.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #775.511
        jc        ..B3.10       # Prob 5%                       #775.511
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #775.464
        cmpl      $4, %r12d                                     #775.459
        jl        ..B3.7        # Prob 82%                      #775.459
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #775.666
        jmp       ..B3.2        # Prob 100%                     #775.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #775.540
        lea       128(%rsp,%r12,4), %rsi                        #775.540
..___tag_value___svml_atanf4_ha_l9.61:
#       __svml_satan_ha_cout_rare_internal(const float *, float *)
        call      __svml_satan_ha_cout_rare_internal            #775.540
..___tag_value___svml_atanf4_ha_l9.62:
        jmp       ..B3.8        # Prob 100%                     #775.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_atanf4_ha_l9,@function
	.size	__svml_atanf4_ha_l9,.-__svml_atanf4_ha_l9
..LN__svml_atanf4_ha_l9.2:
	.data
# -- End  __svml_atanf4_ha_l9
	.text
.L_2__routine_start___svml_atanf4_ha_ex_3:
# -- Begin  __svml_atanf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atanf4_ha_ex
# --- __svml_atanf4_ha_ex(__m128)
__svml_atanf4_ha_ex:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #783.1
        .byte     15                                            #964.540
        .byte     30                                            #964.540
        .byte     250                                           #964.540
	.cfi_startproc
..___tag_value___svml_atanf4_ha_ex.64:
..L65:
                                                         #783.1
        pushq     %rbp                                          #783.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #783.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #783.1
        subq      $192, %rsp                                    #783.1
        movaps    %xmm0, %xmm10                                 #783.1
        movups    192+__svml_satan_ha_data_internal(%rip), %xmm14 #852.14
        andps     %xmm10, %xmm14                                #852.14
        movaps    %xmm14, %xmm4                                 #858.16
        movaps    %xmm14, %xmm8                                 #887.18
        psubd     1920+__svml_satan_ha_data_internal(%rip), %xmm4 #858.16
        movaps    %xmm14, %xmm0                                 #889.18
        movdqu    1984+__svml_satan_ha_data_internal(%rip), %xmm6 #857.59
        movdqa    %xmm4, %xmm5                                  #859.30
        movups    768+__svml_satan_ha_data_internal(%rip), %xmm12 #883.47
        movaps    %xmm14, %xmm2                                 #891.18
        movups    832+__svml_satan_ha_data_internal(%rip), %xmm15 #884.47
        pcmpgtd   %xmm6, %xmm5                                  #859.30
        movups    896+__svml_satan_ha_data_internal(%rip), %xmm13 #885.47
        pcmpeqd   %xmm6, %xmm4                                  #859.77
        movdqu    256+__svml_satan_ha_data_internal(%rip), %xmm1 #867.20
        por       %xmm4, %xmm5                                  #859.16
        movaps    %xmm14, %xmm6                                 #870.20
        pcmpgtd   %xmm14, %xmm1                                 #867.20
        movmskps  %xmm5, %edx                                   #860.44
        movdqa    %xmm1, %xmm7                                  #872.21
        movdqu    320+__svml_satan_ha_data_internal(%rip), %xmm5 #868.20
        movaps    %xmm14, %xmm11                                #886.18
        movdqu    384+__svml_satan_ha_data_internal(%rip), %xmm4 #869.20
        pcmpgtd   %xmm14, %xmm5                                 #868.20
        pcmpgtd   448+__svml_satan_ha_data_internal(%rip), %xmm6 #870.20
        pcmpgtd   %xmm14, %xmm4                                 #869.20
        movdqu    1728+__svml_satan_ha_data_internal(%rip), %xmm3 #875.20
        pandn     %xmm5, %xmm7                                  #872.21
        pxor      %xmm6, %xmm3                                  #875.20
        pandn     %xmm4, %xmm5                                  #874.21
        pandn     %xmm3, %xmm4                                  #876.21
        andps     %xmm1, %xmm11                                 #886.18
        movups    64+__svml_satan_ha_data_internal(%rip), %xmm3 #894.18
        subps     %xmm12, %xmm8                                 #887.18
        mulps     %xmm14, %xmm12                                #901.30
        subps     %xmm15, %xmm0                                 #889.18
        subps     %xmm13, %xmm2                                 #891.18
        mulps     %xmm14, %xmm15                                #903.30
        mulps     %xmm14, %xmm13                                #905.30
        andps     %xmm7, %xmm8                                  #888.18
        andps     %xmm5, %xmm0                                  #890.18
        andps     %xmm4, %xmm2                                  #892.18
        orps      %xmm8, %xmm11                                 #895.18
        orps      %xmm2, %xmm0                                  #896.18
        andps     %xmm6, %xmm14                                 #907.18
        orps      %xmm0, %xmm11                                 #897.18
        andps     %xmm6, %xmm3                                  #894.18
        movups    __svml_satan_ha_data_internal(%rip), %xmm0    #900.18
        orps      %xmm3, %xmm11                                 #898.17
        andps     %xmm0, %xmm1                                  #900.18
        addps     %xmm0, %xmm12                                 #901.18
        addps     %xmm0, %xmm15                                 #903.18
        addps     %xmm13, %xmm0                                 #905.18
        cvtps2pd  %xmm11, %xmm3                                 #899.27
        andps     %xmm7, %xmm12                                 #902.18
        andps     %xmm5, %xmm15                                 #904.18
        andps     %xmm4, %xmm0                                  #906.18
        orps      %xmm12, %xmm1                                 #908.18
        orps      %xmm0, %xmm15                                 #909.18
        orps      %xmm15, %xmm1                                 #910.18
        orps      %xmm14, %xmm1                                 #911.17
        movaps    %xmm1, %xmm12                                 #912.78
        cvtps2pd  %xmm1, %xmm13                                 #912.27
        movhlps   %xmm1, %xmm12                                 #912.78
        rcpps     %xmm1, %xmm1                                  #924.22
        cvtps2pd  %xmm12, %xmm2                                 #912.64
        movhlps   %xmm11, %xmm11                                #899.78
        cvtps2pd  %xmm11, %xmm8                                 #899.64
        movups    960+__svml_satan_ha_data_internal(%rip), %xmm0 #917.18
        movups    1088+__svml_satan_ha_data_internal(%rip), %xmm11 #918.18
        andps     %xmm7, %xmm0                                  #917.18
        movups    1216+__svml_satan_ha_data_internal(%rip), %xmm15 #919.18
        andps     %xmm5, %xmm11                                 #918.18
        movups    1344+__svml_satan_ha_data_internal(%rip), %xmm12 #920.18
        andps     %xmm4, %xmm15                                 #919.18
        andps     %xmm6, %xmm12                                 #920.18
        orps      %xmm11, %xmm0                                 #921.18
        cvtps2pd  %xmm1, %xmm11                                 #925.28
        movhlps   %xmm1, %xmm1                                  #925.81
        orps      %xmm12, %xmm15                                #922.18
        orps      %xmm15, %xmm0                                 #923.17
        cvtps2pd  %xmm1, %xmm15                                 #925.67
        mulpd     %xmm11, %xmm13                                #927.50
        mulpd     %xmm15, %xmm2                                 #927.121
        movups    1856+__svml_satan_ha_data_internal(%rip), %xmm14 #926.56
        movaps    %xmm14, %xmm12                                #927.27
        subpd     %xmm13, %xmm12                                #927.27
        movaps    %xmm14, %xmm13                                #927.98
        subpd     %xmm2, %xmm13                                 #927.98
        movaps    %xmm12, %xmm2                                 #928.39
        movaps    %xmm13, %xmm1                                 #928.114
        mulpd     %xmm11, %xmm2                                 #928.39
        mulpd     %xmm15, %xmm1                                 #928.114
        mulpd     %xmm12, %xmm12                                #929.35
        mulpd     %xmm13, %xmm13                                #929.110
        addpd     %xmm2, %xmm11                                 #928.27
        addpd     %xmm1, %xmm15                                 #928.102
        addpd     %xmm14, %xmm12                                #929.23
        addpd     %xmm13, %xmm14                                #929.98
        mulpd     %xmm12, %xmm11                                #930.23
        mulpd     %xmm14, %xmm15                                #930.69
        mulpd     %xmm11, %xmm3                                 #931.23
        mulpd     %xmm15, %xmm8                                 #931.65
        movups    1792+__svml_satan_ha_data_internal(%rip), %xmm11 #932.59
        movaps    %xmm11, %xmm15                                #933.21
        andps     %xmm8, %xmm11                                 #933.74
        andps     %xmm3, %xmm15                                 #933.21
        subpd     %xmm15, %xmm3                                 #935.21
        subpd     %xmm11, %xmm8                                 #935.67
        cvtpd2ps  %xmm15, %xmm12                                #934.35
        cvtpd2ps  %xmm11, %xmm13                                #934.66
        cvtpd2ps  %xmm3, %xmm11                                 #936.35
        cvtpd2ps  %xmm8, %xmm3                                  #936.66
        movups    1024+__svml_satan_ha_data_internal(%rip), %xmm8 #941.18
        andps     %xmm7, %xmm8                                  #941.18
        movups    1152+__svml_satan_ha_data_internal(%rip), %xmm7 #942.18
        andps     %xmm5, %xmm7                                  #942.18
        movups    1280+__svml_satan_ha_data_internal(%rip), %xmm5 #943.18
        orps      %xmm7, %xmm8                                  #945.18
        movlhps   %xmm13, %xmm12                                #934.19
        andps     %xmm4, %xmm5                                  #943.18
        movups    1408+__svml_satan_ha_data_internal(%rip), %xmm4 #944.18
        andps     %xmm6, %xmm4                                  #944.18
        movaps    %xmm12, %xmm6                                 #949.14
        mulps     %xmm12, %xmm6                                 #949.14
        orps      %xmm4, %xmm5                                  #946.18
        movaps    %xmm6, %xmm1                                  #950.14
        orps      %xmm5, %xmm8                                  #947.17
        mulps     %xmm6, %xmm1                                  #950.14
        movups    1472+__svml_satan_ha_data_internal(%rip), %xmm2 #955.26
        mulps     %xmm1, %xmm2                                  #955.26
        movlhps   %xmm3, %xmm11                                 #936.19
        addps     1600+__svml_satan_ha_data_internal(%rip), %xmm2 #955.14
        addps     %xmm8, %xmm11                                 #948.19
        mulps     %xmm6, %xmm2                                  #957.25
        movups    1536+__svml_satan_ha_data_internal(%rip), %xmm3 #956.26
        mulps     %xmm1, %xmm3                                  #956.26
        movups    128+__svml_satan_ha_data_internal(%rip), %xmm9 #854.14
        andps     %xmm10, %xmm9                                 #854.14
        addps     1664+__svml_satan_ha_data_internal(%rip), %xmm3 #956.14
        addps     %xmm2, %xmm3                                  #957.13
        mulps     %xmm3, %xmm6                                  #958.13
        mulps     %xmm12, %xmm6                                 #959.26
        addps     %xmm6, %xmm11                                 #959.14
        addps     %xmm11, %xmm12                                #960.14
        addps     %xmm12, %xmm0                                 #961.18
        orps      %xmm9, %xmm0                                  #962.14
        testl     %edx, %edx                                    #964.52
        jne       ..B4.3        # Prob 5%                       #964.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm10
..B4.2:                         # Preds ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #967.12
        popq      %rbp                                          #967.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #967.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm10, 64(%rsp)                              #964.193
        movups    %xmm0, 128(%rsp)                              #964.264
                                # LOE rbx r12 r13 r14 r15 edx
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #964.448
        movq      %r12, 8(%rsp)                                 #964.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #964.448
        movq      %r13, (%rsp)                                  #964.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #964.448
                                # LOE rbx r12 r14 r15 r13d
..B4.7:                         # Preds ..B4.8 ..B4.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #964.511
        jc        ..B4.10       # Prob 5%                       #964.511
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #964.464
        cmpl      $4, %r12d                                     #964.459
        jl        ..B4.7        # Prob 82%                      #964.459
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #964.666
        jmp       ..B4.2        # Prob 100%                     #964.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #964.540
        lea       128(%rsp,%r12,4), %rsi                        #964.540
..___tag_value___svml_atanf4_ha_ex.82:
#       __svml_satan_ha_cout_rare_internal(const float *, float *)
        call      __svml_satan_ha_cout_rare_internal            #964.540
..___tag_value___svml_atanf4_ha_ex.83:
        jmp       ..B4.8        # Prob 100%                     #964.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_atanf4_ha_ex,@function
	.size	__svml_atanf4_ha_ex,.-__svml_atanf4_ha_ex
..LN__svml_atanf4_ha_ex.3:
	.data
# -- End  __svml_atanf4_ha_ex
	.text
.L_2__routine_start___svml_atanf8_ha_l9_4:
# -- Begin  __svml_atanf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atanf8_ha_l9
# --- __svml_atanf8_ha_l9(__m256)
__svml_atanf8_ha_l9:
# parameter 1: %ymm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #972.1
        .byte     15                                            #1148.546
        .byte     30                                            #1148.546
        .byte     250                                           #1148.546
	.cfi_startproc
..___tag_value___svml_atanf8_ha_l9.85:
..L86:
                                                         #972.1
        pushq     %rbp                                          #972.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #972.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #972.1
        subq      $192, %rsp                                    #972.1
        vmovaps   %ymm0, %ymm12                                 #972.1
        vandps    192+__svml_satan_ha_data_internal(%rip), %ymm12, %ymm3 #1041.14
        vmovups   1984+__svml_satan_ha_data_internal(%rip), %ymm6 #1046.26
        vmovups   256+__svml_satan_ha_data_internal(%rip), %ymm2 #1050.24
        vmovups   320+__svml_satan_ha_data_internal(%rip), %ymm1 #1051.24
        vmovups   384+__svml_satan_ha_data_internal(%rip), %ymm14 #1052.24
        vpsubd    1920+__svml_satan_ha_data_internal(%rip), %ymm3, %ymm8 #1047.18
        vpcmpgtd  %ymm6, %ymm8, %ymm7                           #1048.35
        vpcmpeqd  %ymm6, %ymm8, %ymm9                           #1048.89
        vpor      %ymm9, %ymm7, %ymm4                           #1048.18
        vpcmpgtd  448+__svml_satan_ha_data_internal(%rip), %ymm3, %ymm8 #1059.22
        vmovmskps %ymm4, %edx                                   #1049.45
        vpcmpgtd  %ymm3, %ymm2, %ymm0                           #1056.22
        vpcmpgtd  %ymm3, %ymm1, %ymm13                          #1057.22
        vpxor     1728+__svml_satan_ha_data_internal(%rip), %ymm8, %ymm10 #1064.22
        vpandn    %ymm13, %ymm0, %ymm9                          #1061.23
        vmovups   768+__svml_satan_ha_data_internal(%rip), %ymm1 #1072.50
        vmovups   832+__svml_satan_ha_data_internal(%rip), %ymm2 #1073.50
        vmovups   896+__svml_satan_ha_data_internal(%rip), %ymm4 #1074.50
        vsubps    %ymm1, %ymm3, %ymm5                           #1076.18
        vpcmpgtd  %ymm3, %ymm14, %ymm6                          #1058.22
        vpandn    %ymm6, %ymm13, %ymm7                          #1063.23
        vpandn    %ymm10, %ymm6, %ymm6                          #1065.23
        vsubps    %ymm2, %ymm3, %ymm14                          #1078.18
        vsubps    %ymm4, %ymm3, %ymm10                          #1080.18
        vandps    %ymm0, %ymm3, %ymm15                          #1075.18
        vandps    %ymm9, %ymm5, %ymm13                          #1077.18
        vandps    %ymm7, %ymm14, %ymm14                         #1079.18
        vandps    %ymm6, %ymm10, %ymm10                         #1081.18
        vorps     %ymm13, %ymm15, %ymm15                        #1084.18
        vorps     %ymm10, %ymm14, %ymm13                        #1085.18
        vandps    64+__svml_satan_ha_data_internal(%rip), %ymm8, %ymm5 #1083.18
        vorps     %ymm13, %ymm15, %ymm14                        #1086.18
        vorps     %ymm5, %ymm14, %ymm10                         #1087.17
        vmovups   __svml_satan_ha_data_internal(%rip), %ymm5    #1088.18
        vfmadd213ps %ymm5, %ymm3, %ymm1                         #1089.18
        vfmadd213ps %ymm5, %ymm3, %ymm2                         #1091.18
        vfmadd213ps %ymm5, %ymm3, %ymm4                         #1093.18
        vandps    %ymm0, %ymm5, %ymm15                          #1088.18
        vandps    %ymm9, %ymm1, %ymm13                          #1090.18
        vandps    %ymm7, %ymm2, %ymm14                          #1092.18
        vandps    %ymm6, %ymm4, %ymm0                           #1094.18
        vandps    %ymm8, %ymm3, %ymm1                           #1095.18
        vorps     %ymm13, %ymm15, %ymm3                         #1096.18
        vorps     %ymm0, %ymm14, %ymm14                         #1097.18
        vorps     %ymm14, %ymm3, %ymm13                         #1098.18
        vorps     %ymm1, %ymm13, %ymm15                         #1099.17
        vandps    960+__svml_satan_ha_data_internal(%rip), %ymm9, %ymm0 #1104.18
        vandps    1088+__svml_satan_ha_data_internal(%rip), %ymm7, %ymm2 #1105.18
        vorps     %ymm2, %ymm0, %ymm14                          #1108.18
        vrcpps    %ymm15, %ymm0                                 #1111.18
        vmovups   1536+__svml_satan_ha_data_internal(%rip), %ymm1 #1136.50
        vfnmadd231ps %ymm0, %ymm15, %ymm5                       #1114.21
        vfmadd213ps %ymm5, %ymm5, %ymm5                         #1115.21
        vfmadd213ps %ymm0, %ymm5, %ymm0                         #1116.22
        vandps    1216+__svml_satan_ha_data_internal(%rip), %ymm6, %ymm3 #1106.18
        vandps    1344+__svml_satan_ha_data_internal(%rip), %ymm8, %ymm4 #1107.18
        vorps     %ymm4, %ymm3, %ymm13                          #1109.18
        vorps     %ymm13, %ymm14, %ymm14                        #1110.17
        vmulps    %ymm0, %ymm10, %ymm13                         #1118.19
        vmovups   1472+__svml_satan_ha_data_internal(%rip), %ymm4 #1135.50
        vmulps    %ymm13, %ymm13, %ymm3                         #1133.14
        vfnmadd213ps %ymm10, %ymm13, %ymm15                     #1119.19
        vmulps    %ymm3, %ymm3, %ymm2                           #1134.14
        vmulps    %ymm15, %ymm0, %ymm0                          #1120.19
        vfmadd213ps 1600+__svml_satan_ha_data_internal(%rip), %ymm2, %ymm4 #1139.14
        vfmadd213ps 1664+__svml_satan_ha_data_internal(%rip), %ymm1, %ymm2 #1140.14
        vfmadd213ps %ymm2, %ymm3, %ymm4                         #1141.13
        vandps    1024+__svml_satan_ha_data_internal(%rip), %ymm9, %ymm9 #1125.18
        vandps    1152+__svml_satan_ha_data_internal(%rip), %ymm7, %ymm7 #1126.18
        vandps    1280+__svml_satan_ha_data_internal(%rip), %ymm6, %ymm6 #1127.18
        vandps    1408+__svml_satan_ha_data_internal(%rip), %ymm8, %ymm8 #1128.18
        vorps     %ymm7, %ymm9, %ymm10                          #1129.18
        vorps     %ymm8, %ymm6, %ymm15                          #1130.18
        vmulps    %ymm4, %ymm3, %ymm7                           #1142.13
        vorps     %ymm15, %ymm10, %ymm6                         #1131.17
        vaddps    %ymm0, %ymm6, %ymm5                           #1132.19
        vfmadd213ps %ymm5, %ymm13, %ymm7                        #1143.14
        vaddps    %ymm7, %ymm13, %ymm8                          #1144.14
        vaddps    %ymm8, %ymm14, %ymm9                          #1145.18
        vandps    128+__svml_satan_ha_data_internal(%rip), %ymm12, %ymm11 #1043.14
        vorps     %ymm11, %ymm9, %ymm0                          #1146.14
        testl     %edx, %edx                                    #1148.52
        jne       ..B5.3        # Prob 5%                       #1148.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm12
..B5.2:                         # Preds ..B5.3 ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1151.12
        popq      %rbp                                          #1151.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1151.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm12, 64(%rsp)                              #1148.196
        vmovups   %ymm0, 128(%rsp)                              #1148.270
        je        ..B5.2        # Prob 95%                      #1148.374
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1148.454
                                # LOE rbx r12 r13 r14 r15 eax edx
..B5.13:                        # Preds ..B5.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1148.517
        jc        ..B5.10       # Prob 5%                       #1148.517
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1148.470
        cmpl      $8, %r12d                                     #1148.465
        jl        ..B5.7        # Prob 82%                      #1148.465
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #1148.675
        jmp       ..B5.2        # Prob 100%                     #1148.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1148.546
        lea       128(%rsp,%r12,4), %rsi                        #1148.546
..___tag_value___svml_atanf8_ha_l9.103:
#       __svml_satan_ha_cout_rare_internal(const float *, float *)
        call      __svml_satan_ha_cout_rare_internal            #1148.546
..___tag_value___svml_atanf8_ha_l9.104:
        jmp       ..B5.8        # Prob 100%                     #1148.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_atanf8_ha_l9,@function
	.size	__svml_atanf8_ha_l9,.-__svml_atanf8_ha_l9
..LN__svml_atanf8_ha_l9.4:
	.data
# -- End  __svml_atanf8_ha_l9
	.text
.L_2__routine_start___svml_atanf16_ha_z0_5:
# -- Begin  __svml_atanf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atanf16_ha_z0
# --- __svml_atanf16_ha_z0(__m512)
__svml_atanf16_ha_z0:
# parameter 1: %zmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1509.1
        .byte     15                                            #1617.12
        .byte     30                                            #1617.12
        .byte     250                                           #1617.12
	.cfi_startproc
..___tag_value___svml_atanf16_ha_z0.106:
..L107:
                                                        #1509.1
        vmovups   128+__svml_satan_ha_data_internal_avx512(%rip), %zmm4 #1569.55
        vmovups   320+__svml_satan_ha_data_internal_avx512(%rip), %zmm5 #1571.49
        vmovups   64+__svml_satan_ha_data_internal_avx512(%rip), %zmm6 #1567.50
        vmovups   256+__svml_satan_ha_data_internal_avx512(%rip), %zmm10 #1577.46
        vmovups   192+__svml_satan_ha_data_internal_avx512(%rip), %zmm13 #1576.16
        vmovups   448+__svml_satan_ha_data_internal_avx512(%rip), %zmm15 #1592.277
        vandps    __svml_satan_ha_data_internal_avx512(%rip), %zmm0, %zmm11 #1566.12
        vcmpps    $17, {sae}, %zmm4, %zmm11, %k1                #1570.20
        vminps    {sae}, %zmm11, %zmm5, %zmm2                   #1572.14
        vmovups   576+__svml_satan_ha_data_internal_avx512(%rip), %zmm4 #1593.281
        vaddps    {rn-sae}, %zmm6, %zmm11, %zmm3                #1568.19
        vxorps    %zmm0, %zmm11, %zmm1                          #1574.15
        vreduceps $40, {sae}, %zmm11, %zmm13{%k1}               #1576.16
        vsubps    {rn-sae}, %zmm6, %zmm3, %zmm7                 #1575.15
        vpermt2ps 512+__svml_satan_ha_data_internal_avx512(%rip), %zmm3, %zmm15 #1592.277
        vpermt2ps 640+__svml_satan_ha_data_internal_avx512(%rip), %zmm3, %zmm4 #1593.281
        vfmadd213ps {rn-sae}, %zmm10, %zmm7, %zmm2{%k1}         #1578.12
        vmovups   832+__svml_satan_ha_data_internal_avx512(%rip), %zmm3 #1599.51
        vmovups   896+__svml_satan_ha_data_internal_avx512(%rip), %zmm6 #1600.51
        vsubps    {rn-sae}, %zmm10, %zmm2, %zmm8                #1579.17
        vrcp14ps  %zmm2, %zmm12                                 #1580.15
        vfmsub213ps {rn-sae}, %zmm8, %zmm7, %zmm11              #1581.15
        knotw     %k1, %k2                                      #1584.20
        vmovaps   %zmm10, %zmm9                                 #1582.14
        vfnmadd231ps {rn-sae}, %zmm2, %zmm12, %zmm9             #1582.14
        vblendmps 704+__svml_satan_ha_data_internal_avx512(%rip), %zmm15, %zmm7{%k2} #1596.14
        vblendmps 768+__svml_satan_ha_data_internal_avx512(%rip), %zmm4, %zmm5{%k2} #1597.18
        vfmadd213ps {rn-sae}, %zmm12, %zmm9, %zmm12             #1583.15
        vmulps    {rn-sae}, %zmm13, %zmm12, %zmm0               #1585.14
        vmulps    {rn-sae}, %zmm12, %zmm11, %zmm14              #1587.18
        vfnmadd213ps {rn-sae}, %zmm10, %zmm12, %zmm2            #1586.14
        vfmsub213ps {rn-sae}, %zmm0, %zmm12, %zmm13             #1588.13
        vmulps    {rn-sae}, %zmm0, %zmm0, %zmm12                #1598.15
        vfmadd213ps {rn-sae}, %zmm13, %zmm0, %zmm2              #1589.13
        vaddps    {rn-sae}, %zmm0, %zmm7, %zmm13                #1602.17
        vmulps    {rn-sae}, %zmm0, %zmm12, %zmm10               #1604.15
        vfnmadd231ps {rn-sae}, %zmm0, %zmm14, %zmm2{%k1}        #1590.13
        vfmadd231ps {rn-sae}, %zmm12, %zmm3, %zmm6              #1601.15
        vsubps    {rn-sae}, %zmm7, %zmm13, %zmm8                #1607.15
        vaddps    {rn-sae}, %zmm5, %zmm2, %zmm9                 #1603.15
        vmovups   960+__svml_satan_ha_data_internal_avx512(%rip), %zmm2 #1605.51
        vsubps    {rn-sae}, %zmm8, %zmm0, %zmm0                 #1608.16
        vfmadd213ps {rn-sae}, %zmm2, %zmm6, %zmm12              #1606.15
        vaddps    {rn-sae}, %zmm0, %zmm9, %zmm11                #1609.15
        vfmadd213ps {rn-sae}, %zmm11, %zmm10, %zmm12            #1610.15
        vaddps    {rn-sae}, %zmm13, %zmm12, %zmm14              #1611.16
        vxorps    %zmm1, %zmm14, %zmm0                          #1612.14
        ret                                                     #1617.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_atanf16_ha_z0,@function
	.size	__svml_atanf16_ha_z0,.-__svml_atanf16_ha_z0
..LN__svml_atanf16_ha_z0.5:
	.data
# -- End  __svml_atanf16_ha_z0
	.text
.L_2__routine_start___svml_satan_ha_cout_rare_internal_6:
# -- Begin  __svml_satan_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_satan_ha_cout_rare_internal
	.globl __svml_satan_ha_cout_rare_internal
# --- __svml_satan_ha_cout_rare_internal(const float *, float *)
__svml_satan_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1399.1
        .byte     15                                            #1501.15
        .byte     30                                            #1501.15
        .byte     250                                           #1501.15
	.cfi_startproc
..___tag_value___svml_satan_ha_cout_rare_internal.109:
..L110:
                                                        #1399.1
        movzwl    2(%rdi), %edx                                 #1408.31
        andl      $32640, %edx                                  #1408.31
        shrl      $7, %edx                                      #1408.31
        cmpl      $255, %edx                                    #1408.57
        je        ..B7.12       # Prob 16%                      #1408.57
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 edx
..B7.2:                         # Preds ..B7.1
                                # Execution count [8.40e-01]
        movss     (%rdi), %xmm1                                 #1411.18
        pxor      %xmm0, %xmm0                                  #1411.18
        cvtss2sd  %xmm1, %xmm0                                  #1411.18
        movsd     %xmm0, -8(%rsp)                               #1411.9
        andb      $127, -1(%rsp)                                #1412.30
        movsd     -8(%rsp), %xmm0                               #1413.14
        movb      3(%rdi), %al                                  #1410.37
        shrb      $7, %al                                       #1410.37
        comisd    1888+_vmlsAtanHATab(%rip), %xmm0              #1413.38
        movl      -4(%rsp), %ecx                                #1413.14
        jb        ..B7.6        # Prob 50%                      #1413.38
                                # LOE rbx rbp rsi r12 r13 r14 r15 edx ecx al xmm0 xmm1
..B7.3:                         # Preds ..B7.2
                                # Execution count [4.20e-01]
        movsd     1896+_vmlsAtanHATab(%rip), %xmm1              #1415.41
        comisd    %xmm0, %xmm1                                  #1415.41
        jbe       ..B7.5        # Prob 50%                      #1415.41
                                # LOE rbx rbp rsi r12 r13 r14 r15 ecx al xmm0
..B7.4:                         # Preds ..B7.3
                                # Execution count [2.10e-01]
        movl      %ecx, %edx                                    #1418.48
        movl      %ecx, %edi                                    #1423.107
        andl      $-524288, %ecx                                #1423.174
        andl      $-1048576, %edi                               #1423.107
        addl      $262144, %ecx                                 #1423.185
        movaps    %xmm0, %xmm9                                  #1424.30
        andl      $1048575, %ecx                                #1423.196
        movaps    %xmm0, %xmm10                                 #1426.62
        movsd     %xmm0, -40(%rsp)                              #1421.17
        orl       %ecx, %edi                                    #1423.196
        movl      $0, -40(%rsp)                                 #1422.38
        andl      $1048575, %edx                                #1418.48
        movl      %edi, -36(%rsp)                               #1423.38
        lea       _vmlsAtanHATab(%rip), %rcx                    #1440.54
        movsd     1928+_vmlsAtanHATab(%rip), %xmm4              #1425.62
        movsd     -40(%rsp), %xmm15                             #1427.35
        movzwl    -2(%rsp), %r8d                                #1417.43
        mulsd     1928+_vmlsAtanHATab(%rip), %xmm10             #1426.62
        andl      $32752, %r8d                                  #1417.43
        subsd     -40(%rsp), %xmm9                              #1424.30
        shlb      $7, %al                                       #1448.38
        mulsd     %xmm9, %xmm4                                  #1425.62
        movsd     %xmm4, -32(%rsp)                              #1425.17
        movsd     -32(%rsp), %xmm5                              #1425.106
        shrl      $4, %r8d                                      #1417.43
        subsd     %xmm9, %xmm5                                  #1425.116
        movsd     %xmm5, -24(%rsp)                              #1425.87
        movsd     -32(%rsp), %xmm7                              #1425.144
        movsd     -24(%rsp), %xmm6                              #1425.154
        shll      $20, %r8d                                     #1417.67
        subsd     %xmm6, %xmm7                                  #1425.154
        movsd     %xmm7, -32(%rsp)                              #1425.125
        orl       %edx, %r8d                                    #1418.48
        movsd     -32(%rsp), %xmm8                              #1425.192
        addl      $-1069547520, %r8d                            #1419.27
        movsd     1904+_vmlsAtanHATab(%rip), %xmm6              #1430.78
        subsd     %xmm8, %xmm9                                  #1425.192
        movsd     %xmm9, -24(%rsp)                              #1425.165
        movsd     -32(%rsp), %xmm2                              #1425.211
        movsd     -24(%rsp), %xmm3                              #1425.228
        movsd     %xmm10, -32(%rsp)                             #1426.17
        movsd     -32(%rsp), %xmm11                             #1426.106
        movsd     1904+_vmlsAtanHATab(%rip), %xmm8              #1430.151
        sarl      $18, %r8d                                     #1420.28
        subsd     -8(%rsp), %xmm11                              #1426.116
        movsd     %xmm11, -24(%rsp)                             #1426.87
        andl      $-2, %r8d                                     #1440.74
        movsd     -32(%rsp), %xmm13                             #1426.144
        movsd     -24(%rsp), %xmm12                             #1426.154
        movslq    %r8d, %r8                                     #1440.54
        subsd     %xmm12, %xmm13                                #1426.154
        movsd     %xmm13, -32(%rsp)                             #1426.125
        movsd     -32(%rsp), %xmm14                             #1426.192
        subsd     %xmm14, %xmm0                                 #1426.192
        movsd     1904+_vmlsAtanHATab(%rip), %xmm14             #1430.304
        movsd     %xmm0, -24(%rsp)                              #1426.165
        movsd     -32(%rsp), %xmm5                              #1426.211
        movsd     -24(%rsp), %xmm4                              #1426.228
        mulsd     %xmm15, %xmm5                                 #1427.35
        mulsd     %xmm15, %xmm4                                 #1428.35
        movaps    %xmm5, %xmm1                                  #1429.48
        addsd     %xmm4, %xmm1                                  #1429.48
        movsd     %xmm1, -32(%rsp)                              #1429.17
        movsd     -32(%rsp), %xmm0                              #1429.91
        subsd     %xmm0, %xmm5                                  #1429.91
        addsd     %xmm4, %xmm5                                  #1429.130
        movsd     1928+_vmlsAtanHATab(%rip), %xmm4              #1432.65
        movsd     %xmm5, -24(%rsp)                              #1429.102
        movsd     -32(%rsp), %xmm11                             #1429.152
        movsd     -24(%rsp), %xmm1                              #1429.170
        addsd     %xmm11, %xmm6                                 #1430.78
        movsd     %xmm6, -32(%rsp)                              #1430.17
        movsd     -32(%rsp), %xmm7                              #1430.151
        subsd     %xmm7, %xmm8                                  #1430.151
        movsd     %xmm8, -24(%rsp)                              #1430.90
        movsd     -32(%rsp), %xmm10                             #1430.181
        movsd     -24(%rsp), %xmm9                              #1430.191
        addsd     %xmm9, %xmm10                                 #1430.191
        movsd     %xmm10, -16(%rsp)                             #1430.162
        movsd     -24(%rsp), %xmm12                             #1430.232
        movsd     1928+_vmlsAtanHATab(%rip), %xmm10             #1434.129
        addsd     %xmm11, %xmm12                                #1430.232
        movsd     %xmm12, -24(%rsp)                             #1430.202
        movsd     -16(%rsp), %xmm13                             #1430.304
        movsd     1904+_vmlsAtanHATab(%rip), %xmm11             #1434.75
        subsd     %xmm13, %xmm14                                #1430.304
        movsd     %xmm14, -16(%rsp)                             #1430.243
        movsd     -24(%rsp), %xmm0                              #1430.334
        movsd     -16(%rsp), %xmm15                             #1430.344
        addsd     %xmm15, %xmm0                                 #1430.344
        movsd     %xmm0, -16(%rsp)                              #1430.315
        movsd     -32(%rsp), %xmm9                              #1430.364
        mulsd     %xmm9, %xmm4                                  #1432.65
        movsd     -16(%rsp), %xmm0                              #1430.382
        movsd     %xmm4, -32(%rsp)                              #1432.17
        addsd     %xmm1, %xmm0                                  #1431.36
        movsd     -32(%rsp), %xmm5                              #1432.109
        subsd     %xmm9, %xmm5                                  #1432.119
        movsd     %xmm5, -24(%rsp)                              #1432.90
        movsd     -32(%rsp), %xmm7                              #1432.150
        movsd     -24(%rsp), %xmm6                              #1432.160
        subsd     %xmm6, %xmm7                                  #1432.160
        movsd     1904+_vmlsAtanHATab(%rip), %xmm6              #1434.327
        movsd     %xmm7, -32(%rsp)                              #1432.131
        movsd     -32(%rsp), %xmm8                              #1432.201
        subsd     %xmm8, %xmm9                                  #1432.201
        movsd     %xmm9, -24(%rsp)                              #1432.171
        movsd     -32(%rsp), %xmm4                              #1432.220
        divsd     %xmm4, %xmm11                                 #1434.75
        mulsd     %xmm11, %xmm10                                #1434.129
        movsd     -24(%rsp), %xmm5                              #1432.238
        movsd     %xmm10, -24(%rsp)                             #1434.84
        addsd     %xmm0, %xmm5                                  #1433.34
        movsd     -24(%rsp), %xmm12                             #1434.172
        subsd     %xmm11, %xmm12                                #1434.182
        movsd     %xmm12, -16(%rsp)                             #1434.153
        movsd     -24(%rsp), %xmm10                             #1434.211
        movsd     -16(%rsp), %xmm13                             #1434.221
        subsd     %xmm13, %xmm10                                #1434.221
        movsd     %xmm10, -16(%rsp)                             #1434.192
        movsd     -16(%rsp), %xmm14                             #1434.258
        mulsd     %xmm14, %xmm4                                 #1434.258
        movsd     -16(%rsp), %xmm15                             #1434.364
        subsd     %xmm4, %xmm6                                  #1434.327
        mulsd     %xmm15, %xmm5                                 #1434.364
        movsd     %xmm5, -24(%rsp)                              #1434.337
        movsd     -24(%rsp), %xmm1                              #1434.403
        subsd     %xmm1, %xmm6                                  #1434.403
        movsd     %xmm6, -24(%rsp)                              #1434.375
        movsd     -24(%rsp), %xmm4                              #1434.472
        movsd     -16(%rsp), %xmm5                              #1434.491
        movsd     -24(%rsp), %xmm0                              #1434.527
        movaps    %xmm5, %xmm7                                  #1435.45
        movsd     -16(%rsp), %xmm1                              #1434.564
        mulsd     %xmm3, %xmm5                                  #1435.132
        addsd     1904+_vmlsAtanHATab(%rip), %xmm4              #1434.472
        mulsd     %xmm2, %xmm7                                  #1435.45
        mulsd     %xmm0, %xmm4                                  #1434.527
        mulsd     %xmm1, %xmm4                                  #1434.564
        mulsd     %xmm4, %xmm3                                  #1435.84
        mulsd     %xmm4, %xmm2                                  #1435.181
        addsd     %xmm3, %xmm5                                  #1435.132
        movsd     1872+_vmlsAtanHATab(%rip), %xmm6              #1438.75
        addsd     %xmm2, %xmm5                                  #1435.181
        movsd     %xmm5, -32(%rsp)                              #1435.143
        movaps    %xmm7, %xmm2                                  #1436.47
        movsd     -32(%rsp), %xmm4                              #1435.218
        addsd     %xmm4, %xmm2                                  #1436.47
        movsd     %xmm2, -32(%rsp)                              #1436.17
        movsd     -32(%rsp), %xmm3                              #1436.88
        movsd     (%rcx,%r8,8), %xmm2                           #1440.54
        subsd     %xmm3, %xmm7                                  #1436.88
        addsd     %xmm4, %xmm7                                  #1436.127
        movsd     %xmm7, -24(%rsp)                              #1436.99
        movsd     -32(%rsp), %xmm3                              #1436.147
        movaps    %xmm3, %xmm5                                  #1437.31
        movaps    %xmm3, %xmm0                                  #1440.85
        mulsd     %xmm3, %xmm5                                  #1437.31
        addsd     %xmm2, %xmm0                                  #1440.85
        mulsd     %xmm5, %xmm6                                  #1438.75
        movsd     -24(%rsp), %xmm10                             #1436.164
        movsd     %xmm0, -32(%rsp)                              #1440.17
        movsd     -32(%rsp), %xmm1                              #1440.163
        addsd     1864+_vmlsAtanHATab(%rip), %xmm6              #1438.98
        subsd     %xmm1, %xmm2                                  #1440.163
        mulsd     %xmm5, %xmm6                                  #1438.120
        addsd     %xmm3, %xmm2                                  #1440.202
        addsd     1856+_vmlsAtanHATab(%rip), %xmm6              #1438.143
        mulsd     %xmm5, %xmm6                                  #1438.165
        movsd     %xmm2, -24(%rsp)                              #1440.174
        movsd     -32(%rsp), %xmm9                              #1440.221
        movsd     -24(%rsp), %xmm8                              #1440.238
        addsd     1848+_vmlsAtanHATab(%rip), %xmm6              #1439.54
        mulsd     %xmm5, %xmm6                                  #1439.76
        addsd     1840+_vmlsAtanHATab(%rip), %xmm6              #1439.99
        mulsd     %xmm5, %xmm6                                  #1439.121
        addsd     1832+_vmlsAtanHATab(%rip), %xmm6              #1439.144
        mulsd     %xmm5, %xmm6                                  #1439.166
        addsd     1824+_vmlsAtanHATab(%rip), %xmm6              #1439.189
        mulsd     %xmm5, %xmm6                                  #1442.40
        mulsd     %xmm3, %xmm6                                  #1443.36
        addsd     %xmm6, %xmm10                                 #1444.36
        addsd     8(%rcx,%r8,8), %xmm10                         #1445.53
        addsd     %xmm8, %xmm10                                 #1447.35
        addsd     %xmm9, %xmm10                                 #1446.36
        movsd     %xmm10, -48(%rsp)                             #1447.17
        movb      -41(%rsp), %r9b                               #1448.38
        andb      $127, %r9b                                    #1448.38
        orb       %al, %r9b                                     #1448.38
        movb      %r9b, -41(%rsp)                               #1448.38
        movsd     -48(%rsp), %xmm11                             #1449.19
        cvtsd2ss  %xmm11, %xmm11                                #1449.19
        movss     %xmm11, (%rsi)                                #1449.19
        jmp       ..B7.11       # Prob 100%                     #1449.19
                                # LOE rbx rbp r12 r13 r14 r15
..B7.5:                         # Preds ..B7.3
                                # Execution count [2.10e-01]
        movsd     1912+_vmlsAtanHATab(%rip), %xmm0              #1453.42
        shlb      $7, %al                                       #1454.38
        addsd     1920+_vmlsAtanHATab(%rip), %xmm0              #1453.82
        movsd     %xmm0, -48(%rsp)                              #1453.17
        movb      -41(%rsp), %dl                                #1454.38
        andb      $127, %dl                                     #1454.38
        orb       %al, %dl                                      #1454.38
        movb      %dl, -41(%rsp)                                #1454.38
        movsd     -48(%rsp), %xmm1                              #1455.19
        cvtsd2ss  %xmm1, %xmm1                                  #1455.19
        movss     %xmm1, (%rsi)                                 #1455.19
        jmp       ..B7.11       # Prob 100%                     #1455.19
                                # LOE rbx rbp r12 r13 r14 r15
..B7.6:                         # Preds ..B7.2
                                # Execution count [4.20e-01]
        comisd    1880+_vmlsAtanHATab(%rip), %xmm0              #1460.42
        jb        ..B7.8        # Prob 50%                      #1460.42
                                # LOE rbx rbp rsi r12 r13 r14 r15 edx al xmm0 xmm1
..B7.7:                         # Preds ..B7.6
                                # Execution count [2.10e-01]
        movaps    %xmm0, %xmm1                                  #1462.29
        mulsd     %xmm0, %xmm1                                  #1462.29
        shlb      $7, %al                                       #1468.38
        movsd     1872+_vmlsAtanHATab(%rip), %xmm2              #1463.53
        mulsd     %xmm1, %xmm2                                  #1463.75
        addsd     1864+_vmlsAtanHATab(%rip), %xmm2              #1463.98
        mulsd     %xmm1, %xmm2                                  #1463.120
        addsd     1856+_vmlsAtanHATab(%rip), %xmm2              #1463.143
        mulsd     %xmm1, %xmm2                                  #1463.165
        addsd     1848+_vmlsAtanHATab(%rip), %xmm2              #1464.54
        mulsd     %xmm1, %xmm2                                  #1464.76
        addsd     1840+_vmlsAtanHATab(%rip), %xmm2              #1464.99
        mulsd     %xmm1, %xmm2                                  #1464.121
        addsd     1832+_vmlsAtanHATab(%rip), %xmm2              #1464.144
        mulsd     %xmm1, %xmm2                                  #1464.166
        addsd     1824+_vmlsAtanHATab(%rip), %xmm2              #1464.189
        mulsd     %xmm1, %xmm2                                  #1465.38
        mulsd     %xmm0, %xmm2                                  #1466.33
        addsd     %xmm0, %xmm2                                  #1467.33
        movsd     %xmm2, -48(%rsp)                              #1467.17
        movb      -41(%rsp), %dl                                #1468.38
        andb      $127, %dl                                     #1468.38
        orb       %al, %dl                                      #1468.38
        movb      %dl, -41(%rsp)                                #1468.38
        movsd     -48(%rsp), %xmm0                              #1469.19
        cvtsd2ss  %xmm0, %xmm0                                  #1469.19
        movss     %xmm0, (%rsi)                                 #1469.19
        jmp       ..B7.11       # Prob 100%                     #1469.19
                                # LOE rbx rbp r12 r13 r14 r15
..B7.8:                         # Preds ..B7.6
                                # Execution count [2.10e-01]
        testl     %edx, %edx                                    #1473.68
        je        ..B7.10       # Prob 50%                      #1473.68
                                # LOE rbx rbp rsi r12 r13 r14 r15 al xmm0 xmm1
..B7.9:                         # Preds ..B7.8
                                # Execution count [1.05e-01]
        movsd     1904+_vmlsAtanHATab(%rip), %xmm1              #1475.48
        shlb      $7, %al                                       #1477.42
        addsd     %xmm0, %xmm1                                  #1475.71
        movsd     %xmm1, -32(%rsp)                              #1475.21
        movsd     -32(%rsp), %xmm0                              #1476.29
        mulsd     -8(%rsp), %xmm0                               #1476.39
        movsd     %xmm0, -48(%rsp)                              #1476.21
        movb      -41(%rsp), %dl                                #1477.42
        andb      $127, %dl                                     #1477.42
        orb       %al, %dl                                      #1477.42
        movb      %dl, -41(%rsp)                                #1477.42
        movsd     -48(%rsp), %xmm2                              #1478.23
        cvtsd2ss  %xmm2, %xmm2                                  #1478.23
        movss     %xmm2, (%rsi)                                 #1478.23
        jmp       ..B7.11       # Prob 100%                     #1478.23
                                # LOE rbx rbp r12 r13 r14 r15
..B7.10:                        # Preds ..B7.8
                                # Execution count [1.05e-01]
        mulss     %xmm1, %xmm1                                  #1482.40
        shlb      $7, %al                                       #1484.42
        movss     %xmm1, -56(%rsp)                              #1482.21
        movss     -56(%rsp), %xmm0                              #1483.35
        cvtss2sd  %xmm0, %xmm0                                  #1483.35
        addsd     -8(%rsp), %xmm0                               #1483.35
        movsd     %xmm0, -48(%rsp)                              #1483.21
        movb      -41(%rsp), %dl                                #1484.42
        andb      $127, %dl                                     #1484.42
        orb       %al, %dl                                      #1484.42
        movb      %dl, -41(%rsp)                                #1484.42
        movsd     -48(%rsp), %xmm1                              #1485.23
        cvtsd2ss  %xmm1, %xmm1                                  #1485.23
        movss     %xmm1, (%rsi)                                 #1485.23
                                # LOE rbx rbp r12 r13 r14 r15
..B7.11:                        # Preds ..B7.4 ..B7.5 ..B7.7 ..B7.9 ..B7.10
                                #       ..B7.13 ..B7.14
                                # Execution count [1.00e+00]
        xorl      %eax, %eax                                    #1504.12
        ret                                                     #1504.12
                                # LOE
..B7.12:                        # Preds ..B7.1
                                # Execution count [1.60e-01]: Infreq
        testl     $8388607, (%rdi)                              #1492.35
        jne       ..B7.14       # Prob 50%                      #1492.64
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15
..B7.13:                        # Preds ..B7.12
                                # Execution count [8.00e-02]: Infreq
        movsd     1912+_vmlsAtanHATab(%rip), %xmm0              #1494.38
        movb      3(%rdi), %al                                  #1495.41
        andb      $-128, %al                                    #1496.34
        addsd     1920+_vmlsAtanHATab(%rip), %xmm0              #1494.78
        movsd     %xmm0, -48(%rsp)                              #1494.13
        movb      -41(%rsp), %dl                                #1496.34
        andb      $127, %dl                                     #1496.34
        orb       %al, %dl                                      #1496.34
        movb      %dl, -41(%rsp)                                #1496.34
        movsd     -48(%rsp), %xmm1                              #1497.15
        cvtsd2ss  %xmm1, %xmm1                                  #1497.15
        movss     %xmm1, (%rsi)                                 #1497.15
        jmp       ..B7.11       # Prob 100%                     #1497.15
                                # LOE rbx rbp r12 r13 r14 r15
..B7.14:                        # Preds ..B7.12
                                # Execution count [8.00e-02]: Infreq
        movss     (%rdi), %xmm0                                 #1501.22
        addss     %xmm0, %xmm0                                  #1501.29
        movss     %xmm0, (%rsi)                                 #1501.15
        jmp       ..B7.11       # Prob 100%                     #1501.15
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_satan_ha_cout_rare_internal,@function
	.size	__svml_satan_ha_cout_rare_internal,.-__svml_satan_ha_cout_rare_internal
..LN__svml_satan_ha_cout_rare_internal.6:
	.data
# -- End  __svml_satan_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_satan_ha_data_internal_avx512
	.globl __svml_satan_ha_data_internal_avx512
__svml_satan_ha_data_internal_avx512:
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
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1241513984
	.long	1089994752
	.long	1089994752
	.long	1089994752
	.long	1089994752
	.long	1089994752
	.long	1089994752
	.long	1089994752
	.long	1089994752
	.long	1089994752
	.long	1089994752
	.long	1089994752
	.long	1089994752
	.long	1089994752
	.long	1089994752
	.long	1089994752
	.long	1089994752
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
	.long	1333788672
	.long	1333788672
	.long	1333788672
	.long	1333788672
	.long	1333788672
	.long	1333788672
	.long	1333788672
	.long	1333788672
	.long	1333788672
	.long	1333788672
	.long	1333788672
	.long	1333788672
	.long	1333788672
	.long	1333788672
	.long	1333788672
	.long	1333788672
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
	.long	1048239024
	.long	1055744824
	.long	1059372157
	.long	1061752795
	.long	1063609315
	.long	1065064543
	.long	1065786489
	.long	1066252045
	.long	1066633083
	.long	1066949484
	.long	1067215699
	.long	1067442363
	.long	1067637412
	.long	1067806856
	.long	1067955311
	.long	1068086373
	.long	1068202874
	.long	1068307075
	.long	1068400798
	.long	1068485529
	.long	1068562486
	.long	1068632682
	.long	1068696961
	.long	1068756035
	.long	1068810506
	.long	1068860887
	.long	1068907620
	.long	1068951084
	.long	1068991608
	.long	1069029480
	.long	1069064949
	.long	0
	.long	2975494116
	.long	833369962
	.long	835299256
	.long	2998648110
	.long	2995239174
	.long	3000492182
	.long	860207626
	.long	3008447516
	.long	3005590622
	.long	3000153675
	.long	860754741
	.long	859285590
	.long	844944488
	.long	2993069463
	.long	858157665
	.long	3006142000
	.long	3007693206
	.long	3009342234
	.long	847469400
	.long	3006114683
	.long	852829553
	.long	847325583
	.long	860305056
	.long	846145135
	.long	2997638646
	.long	855837703
	.long	2979047222
	.long	2995344192
	.long	854092798
	.long	3000498637
	.long	859965578
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
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3007036718
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	3188697310
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	1045219554
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.long	3198855850
	.type	__svml_satan_ha_data_internal_avx512,@object
	.size	__svml_satan_ha_data_internal_avx512,1024
	.align 64
	.hidden __svml_satan_ha_data_internal
	.globl __svml_satan_ha_data_internal
__svml_satan_ha_data_internal:
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
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
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
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	3758096384
	.long	4294967295
	.long	3758096384
	.long	4294967295
	.long	3758096384
	.long	4294967295
	.long	3758096384
	.long	4294967295
	.long	3758096384
	.long	4294967295
	.long	3758096384
	.long	4294967295
	.long	3758096384
	.long	4294967295
	.long	3758096384
	.long	4294967295
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
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	2164260864
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.long	4227858432
	.type	__svml_satan_ha_data_internal,@object
	.size	__svml_satan_ha_data_internal,2048
	.align 32
_vmlsAtanHATab:
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
	.type	_vmlsAtanHATab,@object
	.size	_vmlsAtanHATab,1936
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
