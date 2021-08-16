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
	.file "svml_d_acos.c"
	.text
..TXTST0:
.L_2__routine_start___svml_acos2_ha_l9_0:
# -- Begin  __svml_acos2_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acos2_ha_l9
# --- __svml_acos2_ha_l9(__m128d)
__svml_acos2_ha_l9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #155.1
        .byte     15                                            #253.546
        .byte     30                                            #253.546
        .byte     250                                           #253.546
	.cfi_startproc
..___tag_value___svml_acos2_ha_l9.1:
..L2:
                                                          #155.1
        pushq     %rbp                                          #155.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #155.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #155.1
        subq      $192, %rsp                                    #155.1
        vmovapd   %xmm0, %xmm7                                  #155.1
        vmovupd   __svml_dacos_ha_data_internal(%rip), %xmm8    #192.44
        vmovupd   64+__svml_dacos_ha_data_internal(%rip), %xmm10 #193.45
        vorpd     %xmm7, %xmm8, %xmm6                           #195.8
        vfmadd231pd %xmm6, %xmm10, %xmm10                       #198.8
        vandpd    %xmm7, %xmm8, %xmm5                           #196.12
        vmovupd   256+__svml_dacos_ha_data_internal(%rip), %xmm9 #194.42
        vmulpd    %xmm6, %xmm6, %xmm11                          #199.9
        vcvtpd2ps %xmm10, %xmm13                                #204.546
        vcmpltpd  128+__svml_dacos_ha_data_internal(%rip), %xmm10, %xmm0 #204.717
        vminpd    %xmm10, %xmm11, %xmm4                         #200.9
        vcmpnlepd %xmm6, %xmm9, %xmm12                          #197.13
        vaddpd    %xmm10, %xmm10, %xmm2                         #204.810
        vcmpnltpd %xmm10, %xmm4, %xmm3                          #203.14
        vmovmskpd %xmm12, %edx                                  #202.40
        vmovlhps  %xmm13, %xmm13, %xmm14                        #204.530
        vandpd    %xmm3, %xmm2, %xmm12                          #204.846
        vrsqrtps  %xmm14, %xmm15                                #204.517
        vcvtps2pd %xmm15, %xmm1                                 #204.504
        vandnpd   %xmm1, %xmm0, %xmm8                           #204.760
        vandnpd   %xmm6, %xmm3, %xmm6                           #241.12
        vmulpd    %xmm8, %xmm8, %xmm9                           #204.1112
        vmulpd    %xmm12, %xmm8, %xmm2                          #204.1207
        vfmsub213pd 384+__svml_dacos_ha_data_internal(%rip), %xmm12, %xmm9 #204.1154
        vmovupd   448+__svml_dacos_ha_data_internal(%rip), %xmm1 #204.1688
        vorpd     %xmm6, %xmm2, %xmm6                           #245.13
        vmovupd   832+__svml_dacos_ha_data_internal(%rip), %xmm13 #210.15
        vmovupd   960+__svml_dacos_ha_data_internal(%rip), %xmm0 #214.14
        vmovupd   1088+__svml_dacos_ha_data_internal(%rip), %xmm14 #217.14
        vfmadd213pd 512+__svml_dacos_ha_data_internal(%rip), %xmm9, %xmm1 #204.1688
        vfmsub213pd %xmm2, %xmm8, %xmm12                        #204.1246
        vfmadd213pd 896+__svml_dacos_ha_data_internal(%rip), %xmm4, %xmm13 #210.15
        vfmadd213pd 1024+__svml_dacos_ha_data_internal(%rip), %xmm4, %xmm0 #214.14
        vfmadd213pd 1152+__svml_dacos_ha_data_internal(%rip), %xmm4, %xmm14 #217.14
        vfmadd213pd 576+__svml_dacos_ha_data_internal(%rip), %xmm9, %xmm1 #204.1792
        vandnpd   1664+__svml_dacos_ha_data_internal(%rip), %xmm3, %xmm10 #240.15
        vmulpd    %xmm4, %xmm4, %xmm8                           #211.13
        vfmadd213pd 640+__svml_dacos_ha_data_internal(%rip), %xmm9, %xmm1 #204.1855
        vfmadd213pd %xmm14, %xmm8, %xmm0                        #219.14
        vmulpd    %xmm2, %xmm9, %xmm11                          #204.1746
        vmulpd    %xmm8, %xmm8, %xmm15                          #220.13
        vfmsub213pd %xmm12, %xmm11, %xmm1                       #204.1913
        vmovupd   704+__svml_dacos_ha_data_internal(%rip), %xmm9 #209.12
        vfmadd213pd 768+__svml_dacos_ha_data_internal(%rip), %xmm4, %xmm9 #209.12
        vsubpd    %xmm1, %xmm6, %xmm2                           #246.12
        vfmadd213pd %xmm13, %xmm8, %xmm9                        #218.12
        vmovupd   1216+__svml_dacos_ha_data_internal(%rip), %xmm13 #223.14
        vfmadd213pd 1280+__svml_dacos_ha_data_internal(%rip), %xmm4, %xmm13 #223.14
        vfmadd213pd %xmm0, %xmm15, %xmm9                        #224.12
        vfmadd213pd %xmm13, %xmm8, %xmm9                        #225.12
        vandnpd   1600+__svml_dacos_ha_data_internal(%rip), %xmm3, %xmm8 #239.15
        vfmadd213pd 1344+__svml_dacos_ha_data_internal(%rip), %xmm4, %xmm9 #228.12
        vfmadd213pd 1408+__svml_dacos_ha_data_internal(%rip), %xmm4, %xmm9 #229.12
        vmulpd    %xmm9, %xmm4, %xmm0                           #230.12
        vcmpnltpd %xmm7, %xmm4, %xmm4                           #231.17
        vandpd    %xmm3, %xmm4, %xmm4                           #234.17
        vandpd    1472+__svml_dacos_ha_data_internal(%rip), %xmm4, %xmm9 #235.14
        vorpd     %xmm9, %xmm8, %xmm3                           #242.15
        vxorpd    %xmm5, %xmm3, %xmm3                           #244.15
        vsubpd    %xmm1, %xmm3, %xmm1                           #247.14
        vfmadd213pd %xmm1, %xmm2, %xmm0                         #248.14
        vandpd    1536+__svml_dacos_ha_data_internal(%rip), %xmm4, %xmm11 #236.14
        vorpd     %xmm11, %xmm10, %xmm4                         #243.15
        vaddpd    %xmm0, %xmm6, %xmm0                           #249.14
        vxorpd    %xmm5, %xmm0, %xmm5                           #250.14
        vaddpd    %xmm5, %xmm4, %xmm0                           #251.14
        testl     %edx, %edx                                    #253.52
        jne       ..B1.3        # Prob 5%                       #253.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
..B1.2:                         # Preds ..B1.3 ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #256.12
        popq      %rbp                                          #256.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #256.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm7, 64(%rsp)                               #253.197
        vmovupd   %xmm0, 128(%rsp)                              #253.270
        je        ..B1.2        # Prob 95%                      #253.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #253.454
        movq      %r12, 8(%rsp)                                 #253.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #253.454
        movq      %r13, (%rsp)                                  #253.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #253.454
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #253.517
        jc        ..B1.10       # Prob 5%                       #253.517
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #253.470
        cmpl      $2, %r12d                                     #253.465
        jl        ..B1.7        # Prob 82%                      #253.465
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #253.674
        jmp       ..B1.2        # Prob 100%                     #253.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #253.546
        lea       128(%rsp,%r12,8), %rsi                        #253.546
..___tag_value___svml_acos2_ha_l9.19:
#       __svml_dacos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dacos_ha_cout_rare_internal            #253.546
..___tag_value___svml_acos2_ha_l9.20:
        jmp       ..B1.8        # Prob 100%                     #253.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_acos2_ha_l9,@function
	.size	__svml_acos2_ha_l9,.-__svml_acos2_ha_l9
..LN__svml_acos2_ha_l9.0:
	.data
# -- End  __svml_acos2_ha_l9
	.text
.L_2__routine_start___svml_acos1_ha_ex_1:
# -- Begin  __svml_acos1_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acos1_ha_ex
# --- __svml_acos1_ha_ex(__m128d)
__svml_acos1_ha_ex:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #261.1
        .byte     15                                            #359.546
        .byte     30                                            #359.546
        .byte     250                                           #359.546
	.cfi_startproc
..___tag_value___svml_acos1_ha_ex.22:
..L23:
                                                         #261.1
        pushq     %rbp                                          #261.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #261.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #261.1
        subq      $128, %rsp                                    #261.1
        movaps    %xmm0, %xmm7                                  #261.1
        movsd     __svml_dacos_ha_data_internal(%rip), %xmm5    #298.13
        movaps    %xmm5, %xmm6                                  #301.8
        andps     %xmm7, %xmm5                                  #302.12
        orps      %xmm7, %xmm6                                  #301.8
        movsd     64+__svml_dacos_ha_data_internal(%rip), %xmm9 #299.14
        movaps    %xmm6, %xmm0                                  #304.20
        mulsd     %xmm9, %xmm0                                  #304.20
        movaps    %xmm6, %xmm4                                  #305.9
        mulsd     %xmm6, %xmm4                                  #305.9
        addsd     %xmm9, %xmm0                                  #304.8
        cvtpd2ps  %xmm0, %xmm12                                 #310.546
        shufps    $0, %xmm12, %xmm12                            #310.530
        movaps    %xmm0, %xmm10                                 #306.72
        rsqrtps   %xmm12, %xmm13                                #310.517
        unpcklpd  %xmm4, %xmm4                                  #306.21
        movaps    %xmm0, %xmm15                                 #310.717
        unpcklpd  %xmm0, %xmm10                                 #306.72
        movaps    %xmm6, %xmm11                                 #303.13
        minpd     %xmm10, %xmm4                                 #306.9
        cvtps2pd  %xmm13, %xmm14                                #310.504
        cmpltsd   128+__svml_dacos_ha_data_internal(%rip), %xmm15 #310.717
        movaps    %xmm4, %xmm3                                  #309.14
        andnps    %xmm14, %xmm15                                #310.760
        movsd     320+__svml_dacos_ha_data_internal(%rip), %xmm2 #310.1157
        movaps    %xmm4, %xmm10                                 #316.27
        movsd     256+__svml_dacos_ha_data_internal(%rip), %xmm8 #300.11
        movaps    %xmm4, %xmm14                                 #317.13
        movaps    %xmm4, %xmm13                                 #320.26
        cmpnltsd  %xmm0, %xmm3                                  #309.14
        mulsd     832+__svml_dacos_ha_data_internal(%rip), %xmm10 #316.27
        addsd     %xmm0, %xmm0                                  #310.810
        cmpnlesd  %xmm6, %xmm8                                  #303.13
        mulsd     %xmm4, %xmm14                                 #317.13
        mulsd     960+__svml_dacos_ha_data_internal(%rip), %xmm13 #320.26
        addsd     896+__svml_dacos_ha_data_internal(%rip), %xmm10 #316.15
        addsd     1024+__svml_dacos_ha_data_internal(%rip), %xmm13 #320.14
        andps     %xmm3, %xmm0                                  #310.846
        movsd     %xmm8, %xmm11                                 #303.13
        andps     %xmm0, %xmm2                                  #310.1157
        movaps    %xmm14, %xmm12                                #326.13
        movmskpd  %xmm11, %eax                                  #308.40
        movaps    %xmm4, %xmm11                                 #323.26
        mulsd     1088+__svml_dacos_ha_data_internal(%rip), %xmm11 #323.26
        subsd     %xmm2, %xmm0                                  #310.1203
        mulsd     %xmm15, %xmm2                                 #310.1241
        mulsd     %xmm14, %xmm13                                #325.26
        addsd     1152+__svml_dacos_ha_data_internal(%rip), %xmm11 #323.14
        mulsd     %xmm15, %xmm0                                 #310.1279
        mulsd     %xmm14, %xmm12                                #326.13
        addsd     %xmm11, %xmm13                                #325.14
        movaps    %xmm2, %xmm8                                  #310.1319
        movaps    %xmm0, %xmm1                                  #310.1359
        mulsd     %xmm15, %xmm8                                 #310.1319
        movaps    %xmm2, %xmm9                                  #310.1488
        addsd     %xmm0, %xmm9                                  #310.1488
        mulsd     %xmm15, %xmm1                                 #310.1359
        subsd     384+__svml_dacos_ha_data_internal(%rip), %xmm8 #310.1400
        movaps    %xmm3, %xmm11                                 #346.15
        addsd     %xmm1, %xmm8                                  #310.1445
        movsd     448+__svml_dacos_ha_data_internal(%rip), %xmm1 #310.1913
        mulsd     %xmm8, %xmm1                                  #310.1913
        mulsd     %xmm8, %xmm9                                  #310.1971
        addsd     512+__svml_dacos_ha_data_internal(%rip), %xmm1 #310.1901
        mulsd     %xmm8, %xmm1                                  #310.2029
        movsd     1664+__svml_dacos_ha_data_internal(%rip), %xmm15 #344.15
        andnps    %xmm15, %xmm11                                #346.15
        addsd     576+__svml_dacos_ha_data_internal(%rip), %xmm1 #310.2017
        mulsd     %xmm8, %xmm1                                  #310.2104
        movaps    %xmm4, %xmm8                                  #329.26
        mulsd     1216+__svml_dacos_ha_data_internal(%rip), %xmm8 #329.26
        addsd     640+__svml_dacos_ha_data_internal(%rip), %xmm1 #310.2092
        addsd     1280+__svml_dacos_ha_data_internal(%rip), %xmm8 #329.14
        mulsd     %xmm9, %xmm1                                  #310.2174
        movsd     1472+__svml_dacos_ha_data_internal(%rip), %xmm9 #341.14
        subsd     %xmm0, %xmm1                                  #310.2162
        movaps    %xmm4, %xmm0                                  #315.24
        mulsd     704+__svml_dacos_ha_data_internal(%rip), %xmm0 #315.24
        addsd     768+__svml_dacos_ha_data_internal(%rip), %xmm0 #315.12
        mulsd     %xmm14, %xmm0                                 #324.24
        addsd     %xmm10, %xmm0                                 #324.12
        movsd     1536+__svml_dacos_ha_data_internal(%rip), %xmm10 #342.14
        mulsd     %xmm12, %xmm0                                 #330.24
        addsd     %xmm13, %xmm0                                 #330.12
        mulsd     %xmm14, %xmm0                                 #331.24
        addsd     %xmm8, %xmm0                                  #331.12
        movaps    %xmm3, %xmm8                                  #345.15
        mulsd     %xmm4, %xmm0                                  #334.24
        addsd     1344+__svml_dacos_ha_data_internal(%rip), %xmm0 #334.12
        mulsd     %xmm4, %xmm0                                  #335.24
        addsd     1408+__svml_dacos_ha_data_internal(%rip), %xmm0 #335.12
        mulsd     %xmm4, %xmm0                                  #336.12
        cmpnltsd  %xmm7, %xmm4                                  #337.17
        andps     %xmm3, %xmm4                                  #340.17
        andnps    %xmm6, %xmm3                                  #347.12
        orps      %xmm3, %xmm2                                  #351.13
        andps     %xmm4, %xmm9                                  #341.14
        movaps    %xmm2, %xmm3                                  #352.12
        andps     %xmm4, %xmm10                                 #342.14
        movsd     1600+__svml_dacos_ha_data_internal(%rip), %xmm4 #343.15
        orps      %xmm10, %xmm11                                #349.15
        andnps    %xmm4, %xmm8                                  #345.15
        subsd     %xmm1, %xmm3                                  #352.12
        orps      %xmm9, %xmm8                                  #348.15
        pxor      %xmm5, %xmm8                                  #350.15
        mulsd     %xmm3, %xmm0                                  #354.26
        subsd     %xmm1, %xmm8                                  #353.14
        addsd     %xmm8, %xmm0                                  #354.14
        addsd     %xmm2, %xmm0                                  #355.14
        pxor      %xmm5, %xmm0                                  #356.14
        addsd     %xmm11, %xmm0                                 #357.14
        andl      $1, %eax                                      #308.91
        jne       ..B2.3        # Prob 5%                       #359.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm7
..B2.2:                         # Preds ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #362.12
        popq      %rbp                                          #362.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #362.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm7, (%rsp)                                 #359.160
        movsd     %xmm0, 64(%rsp)                               #359.233
        jne       ..B2.6        # Prob 5%                       #359.374
                                # LOE rbx r12 r13 r14 r15
..B2.4:                         # Preds ..B2.6 ..B2.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #359.626
        movq      %rbp, %rsp                                    #359.626
        popq      %rbp                                          #359.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #359.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.6:                         # Preds ..B2.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #359.546
        lea       64(%rsp), %rsi                                #359.546
..___tag_value___svml_acos1_ha_ex.35:
#       __svml_dacos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dacos_ha_cout_rare_internal            #359.546
..___tag_value___svml_acos1_ha_ex.36:
        jmp       ..B2.4        # Prob 100%                     #359.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_acos1_ha_ex,@function
	.size	__svml_acos1_ha_ex,.-__svml_acos1_ha_ex
..LN__svml_acos1_ha_ex.1:
	.data
# -- End  __svml_acos1_ha_ex
	.text
.L_2__routine_start___svml_acos1_ha_e9_2:
# -- Begin  __svml_acos1_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acos1_ha_e9
# --- __svml_acos1_ha_e9(__m128d)
__svml_acos1_ha_e9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1119.1
        .byte     15                                            #1217.546
        .byte     30                                            #1217.546
        .byte     250                                           #1217.546
	.cfi_startproc
..___tag_value___svml_acos1_ha_e9.38:
..L39:
                                                         #1119.1
        pushq     %rbp                                          #1119.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1119.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1119.1
        subq      $128, %rsp                                    #1119.1
        movaps    %xmm0, %xmm7                                  #1119.1
        movsd     __svml_dacos_ha_data_internal(%rip), %xmm5    #1156.13
        movaps    %xmm5, %xmm6                                  #1159.8
        andps     %xmm7, %xmm5                                  #1160.12
        orps      %xmm7, %xmm6                                  #1159.8
        movsd     64+__svml_dacos_ha_data_internal(%rip), %xmm9 #1157.14
        movaps    %xmm6, %xmm0                                  #1162.20
        mulsd     %xmm9, %xmm0                                  #1162.20
        movaps    %xmm6, %xmm4                                  #1163.9
        mulsd     %xmm6, %xmm4                                  #1163.9
        addsd     %xmm9, %xmm0                                  #1162.8
        cvtpd2ps  %xmm0, %xmm12                                 #1168.546
        shufps    $0, %xmm12, %xmm12                            #1168.530
        movaps    %xmm0, %xmm10                                 #1164.72
        rsqrtps   %xmm12, %xmm13                                #1168.517
        unpcklpd  %xmm4, %xmm4                                  #1164.21
        movaps    %xmm0, %xmm15                                 #1168.717
        unpcklpd  %xmm0, %xmm10                                 #1164.72
        movaps    %xmm6, %xmm11                                 #1161.13
        minpd     %xmm10, %xmm4                                 #1164.9
        cvtps2pd  %xmm13, %xmm14                                #1168.504
        cmpltsd   128+__svml_dacos_ha_data_internal(%rip), %xmm15 #1168.717
        movaps    %xmm4, %xmm3                                  #1167.14
        andnps    %xmm14, %xmm15                                #1168.760
        movsd     320+__svml_dacos_ha_data_internal(%rip), %xmm2 #1168.1157
        movaps    %xmm4, %xmm10                                 #1174.27
        movsd     256+__svml_dacos_ha_data_internal(%rip), %xmm8 #1158.11
        movaps    %xmm4, %xmm14                                 #1175.13
        movaps    %xmm4, %xmm13                                 #1178.26
        cmpnltsd  %xmm0, %xmm3                                  #1167.14
        mulsd     832+__svml_dacos_ha_data_internal(%rip), %xmm10 #1174.27
        addsd     %xmm0, %xmm0                                  #1168.810
        cmpnlesd  %xmm6, %xmm8                                  #1161.13
        mulsd     %xmm4, %xmm14                                 #1175.13
        mulsd     960+__svml_dacos_ha_data_internal(%rip), %xmm13 #1178.26
        addsd     896+__svml_dacos_ha_data_internal(%rip), %xmm10 #1174.15
        addsd     1024+__svml_dacos_ha_data_internal(%rip), %xmm13 #1178.14
        andps     %xmm3, %xmm0                                  #1168.846
        movsd     %xmm8, %xmm11                                 #1161.13
        andps     %xmm0, %xmm2                                  #1168.1157
        movaps    %xmm14, %xmm12                                #1184.13
        movmskpd  %xmm11, %eax                                  #1166.40
        movaps    %xmm4, %xmm11                                 #1181.26
        mulsd     1088+__svml_dacos_ha_data_internal(%rip), %xmm11 #1181.26
        subsd     %xmm2, %xmm0                                  #1168.1203
        mulsd     %xmm15, %xmm2                                 #1168.1241
        mulsd     %xmm14, %xmm13                                #1183.26
        addsd     1152+__svml_dacos_ha_data_internal(%rip), %xmm11 #1181.14
        mulsd     %xmm15, %xmm0                                 #1168.1279
        mulsd     %xmm14, %xmm12                                #1184.13
        addsd     %xmm11, %xmm13                                #1183.14
        movaps    %xmm2, %xmm8                                  #1168.1319
        movaps    %xmm0, %xmm1                                  #1168.1359
        mulsd     %xmm15, %xmm8                                 #1168.1319
        movaps    %xmm2, %xmm9                                  #1168.1488
        addsd     %xmm0, %xmm9                                  #1168.1488
        mulsd     %xmm15, %xmm1                                 #1168.1359
        subsd     384+__svml_dacos_ha_data_internal(%rip), %xmm8 #1168.1400
        movaps    %xmm3, %xmm11                                 #1204.15
        addsd     %xmm1, %xmm8                                  #1168.1445
        movsd     448+__svml_dacos_ha_data_internal(%rip), %xmm1 #1168.1913
        mulsd     %xmm8, %xmm1                                  #1168.1913
        mulsd     %xmm8, %xmm9                                  #1168.1971
        addsd     512+__svml_dacos_ha_data_internal(%rip), %xmm1 #1168.1901
        mulsd     %xmm8, %xmm1                                  #1168.2029
        movsd     1664+__svml_dacos_ha_data_internal(%rip), %xmm15 #1202.15
        andnps    %xmm15, %xmm11                                #1204.15
        addsd     576+__svml_dacos_ha_data_internal(%rip), %xmm1 #1168.2017
        mulsd     %xmm8, %xmm1                                  #1168.2104
        movaps    %xmm4, %xmm8                                  #1187.26
        mulsd     1216+__svml_dacos_ha_data_internal(%rip), %xmm8 #1187.26
        addsd     640+__svml_dacos_ha_data_internal(%rip), %xmm1 #1168.2092
        addsd     1280+__svml_dacos_ha_data_internal(%rip), %xmm8 #1187.14
        mulsd     %xmm9, %xmm1                                  #1168.2174
        movsd     1472+__svml_dacos_ha_data_internal(%rip), %xmm9 #1199.14
        subsd     %xmm0, %xmm1                                  #1168.2162
        movaps    %xmm4, %xmm0                                  #1173.24
        mulsd     704+__svml_dacos_ha_data_internal(%rip), %xmm0 #1173.24
        addsd     768+__svml_dacos_ha_data_internal(%rip), %xmm0 #1173.12
        mulsd     %xmm14, %xmm0                                 #1182.24
        addsd     %xmm10, %xmm0                                 #1182.12
        movsd     1536+__svml_dacos_ha_data_internal(%rip), %xmm10 #1200.14
        mulsd     %xmm12, %xmm0                                 #1188.24
        addsd     %xmm13, %xmm0                                 #1188.12
        mulsd     %xmm14, %xmm0                                 #1189.24
        addsd     %xmm8, %xmm0                                  #1189.12
        movaps    %xmm3, %xmm8                                  #1203.15
        mulsd     %xmm4, %xmm0                                  #1192.24
        addsd     1344+__svml_dacos_ha_data_internal(%rip), %xmm0 #1192.12
        mulsd     %xmm4, %xmm0                                  #1193.24
        addsd     1408+__svml_dacos_ha_data_internal(%rip), %xmm0 #1193.12
        mulsd     %xmm4, %xmm0                                  #1194.12
        cmpnltsd  %xmm7, %xmm4                                  #1195.17
        andps     %xmm3, %xmm4                                  #1198.17
        andnps    %xmm6, %xmm3                                  #1205.12
        orps      %xmm3, %xmm2                                  #1209.13
        andps     %xmm4, %xmm9                                  #1199.14
        movaps    %xmm2, %xmm3                                  #1210.12
        andps     %xmm4, %xmm10                                 #1200.14
        movsd     1600+__svml_dacos_ha_data_internal(%rip), %xmm4 #1201.15
        orps      %xmm10, %xmm11                                #1207.15
        andnps    %xmm4, %xmm8                                  #1203.15
        subsd     %xmm1, %xmm3                                  #1210.12
        orps      %xmm9, %xmm8                                  #1206.15
        pxor      %xmm5, %xmm8                                  #1208.15
        mulsd     %xmm3, %xmm0                                  #1212.26
        subsd     %xmm1, %xmm8                                  #1211.14
        addsd     %xmm8, %xmm0                                  #1212.14
        addsd     %xmm2, %xmm0                                  #1213.14
        pxor      %xmm5, %xmm0                                  #1214.14
        addsd     %xmm11, %xmm0                                 #1215.14
        andl      $1, %eax                                      #1166.91
        jne       ..B3.3        # Prob 5%                       #1217.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm7
..B3.2:                         # Preds ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1220.12
        popq      %rbp                                          #1220.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1220.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm7, (%rsp)                                 #1217.160
        movsd     %xmm0, 64(%rsp)                               #1217.233
        jne       ..B3.6        # Prob 5%                       #1217.374
                                # LOE rbx r12 r13 r14 r15
..B3.4:                         # Preds ..B3.6 ..B3.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #1217.626
        movq      %rbp, %rsp                                    #1217.626
        popq      %rbp                                          #1217.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1217.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B3.6:                         # Preds ..B3.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1217.546
        lea       64(%rsp), %rsi                                #1217.546
..___tag_value___svml_acos1_ha_e9.51:
#       __svml_dacos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dacos_ha_cout_rare_internal            #1217.546
..___tag_value___svml_acos1_ha_e9.52:
        jmp       ..B3.4        # Prob 100%                     #1217.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_acos1_ha_e9,@function
	.size	__svml_acos1_ha_e9,.-__svml_acos1_ha_e9
..LN__svml_acos1_ha_e9.2:
	.data
# -- End  __svml_acos1_ha_e9
	.text
.L_2__routine_start___svml_acos4_ha_l9_3:
# -- Begin  __svml_acos4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acos4_ha_l9
# --- __svml_acos4_ha_l9(__m256d)
__svml_acos4_ha_l9:
# parameter 1: %ymm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1225.1
        .byte     15                                            #1323.552
        .byte     30                                            #1323.552
        .byte     250                                           #1323.552
	.cfi_startproc
..___tag_value___svml_acos4_ha_l9.54:
..L55:
                                                         #1225.1
        pushq     %rbp                                          #1225.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1225.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1225.1
        subq      $192, %rsp                                    #1225.1
        vmovupd   __svml_dacos_ha_data_internal(%rip), %ymm8    #1262.47
        vmovupd   64+__svml_dacos_ha_data_internal(%rip), %ymm9 #1263.48
        vmovapd   %ymm0, %ymm7                                  #1225.1
        vorpd     %ymm7, %ymm8, %ymm6                           #1265.8
        vfmadd231pd %ymm6, %ymm9, %ymm9                         #1268.8
        vmulpd    %ymm6, %ymm6, %ymm10                          #1269.9
        vcmpnge_uqpd 256+__svml_dacos_ha_data_internal(%rip), %ymm6, %ymm11 #1267.13
        vcmplt_oqpd 128+__svml_dacos_ha_data_internal(%rip), %ymm9, %ymm14 #1274.666
        vminpd    %ymm9, %ymm10, %ymm4                          #1270.9
        vaddpd    %ymm9, %ymm9, %ymm0                           #1274.775
        vcvtpd2ps %ymm9, %xmm12                                 #1274.533
        vcmpnlt_uqpd %ymm9, %ymm4, %ymm3                        #1273.14
        vrsqrtps  %xmm12, %xmm13                                #1274.520
        vmovupd   832+__svml_dacos_ha_data_internal(%rip), %ymm12 #1280.15
        vcvtps2pd %xmm13, %ymm15                                #1274.504
        vmovupd   1088+__svml_dacos_ha_data_internal(%rip), %ymm13 #1287.14
        vfmadd213pd 896+__svml_dacos_ha_data_internal(%rip), %ymm4, %ymm12 #1280.15
        vfmadd213pd 1152+__svml_dacos_ha_data_internal(%rip), %ymm4, %ymm13 #1287.14
        vmovmskpd %ymm11, %edx                                  #1272.40
        vandnpd   %ymm15, %ymm14, %ymm1                         #1274.722
        vandpd    %ymm3, %ymm0, %ymm11                          #1274.814
        vandpd    %ymm7, %ymm8, %ymm5                           #1266.12
        vmulpd    %ymm1, %ymm1, %ymm8                           #1274.1089
        vmulpd    %ymm11, %ymm1, %ymm2                          #1274.1190
        vmovupd   960+__svml_dacos_ha_data_internal(%rip), %ymm0 #1284.14
        vmulpd    %ymm4, %ymm4, %ymm15                          #1281.13
        vfmsub213pd 384+__svml_dacos_ha_data_internal(%rip), %ymm11, %ymm8 #1274.1134
        vfmsub213pd %ymm2, %ymm1, %ymm11                        #1274.1232
        vmovupd   448+__svml_dacos_ha_data_internal(%rip), %ymm1 #1274.1689
        vfmadd213pd 1024+__svml_dacos_ha_data_internal(%rip), %ymm4, %ymm0 #1284.14
        vmulpd    %ymm15, %ymm15, %ymm14                        #1290.13
        vmulpd    %ymm2, %ymm8, %ymm10                          #1274.1750
        vfmadd213pd 512+__svml_dacos_ha_data_internal(%rip), %ymm8, %ymm1 #1274.1689
        vfmadd213pd %ymm13, %ymm15, %ymm0                       #1289.14
        vfmadd213pd 576+__svml_dacos_ha_data_internal(%rip), %ymm8, %ymm1 #1274.1799
        vfmadd213pd 640+__svml_dacos_ha_data_internal(%rip), %ymm8, %ymm1 #1274.1865
        vmovupd   704+__svml_dacos_ha_data_internal(%rip), %ymm8 #1279.12
        vfmsub213pd %ymm11, %ymm10, %ymm1                       #1274.1926
        vfmadd213pd 768+__svml_dacos_ha_data_internal(%rip), %ymm4, %ymm8 #1279.12
        vfmadd213pd %ymm12, %ymm15, %ymm8                       #1288.12
        vmovupd   1216+__svml_dacos_ha_data_internal(%rip), %ymm12 #1293.14
        vfmadd213pd %ymm0, %ymm14, %ymm8                        #1294.12
        vfmadd213pd 1280+__svml_dacos_ha_data_internal(%rip), %ymm4, %ymm12 #1293.14
        vfmadd213pd %ymm12, %ymm15, %ymm8                       #1295.12
        vfmadd213pd 1344+__svml_dacos_ha_data_internal(%rip), %ymm4, %ymm8 #1298.12
        vfmadd213pd 1408+__svml_dacos_ha_data_internal(%rip), %ymm4, %ymm8 #1299.12
        vmulpd    %ymm8, %ymm4, %ymm0                           #1300.12
        vcmpnlt_uqpd %ymm7, %ymm4, %ymm4                        #1301.17
        vandpd    %ymm3, %ymm4, %ymm4                           #1304.17
        vandpd    1472+__svml_dacos_ha_data_internal(%rip), %ymm4, %ymm9 #1305.14
        vandnpd   1600+__svml_dacos_ha_data_internal(%rip), %ymm3, %ymm8 #1309.15
        vandnpd   1664+__svml_dacos_ha_data_internal(%rip), %ymm3, %ymm10 #1310.15
        vandnpd   %ymm6, %ymm3, %ymm6                           #1311.12
        vorpd     %ymm9, %ymm8, %ymm3                           #1312.15
        vxorpd    %ymm5, %ymm3, %ymm3                           #1314.15
        vorpd     %ymm6, %ymm2, %ymm6                           #1315.13
        vsubpd    %ymm1, %ymm6, %ymm2                           #1316.12
        vsubpd    %ymm1, %ymm3, %ymm1                           #1317.14
        vfmadd213pd %ymm1, %ymm2, %ymm0                         #1318.14
        vaddpd    %ymm0, %ymm6, %ymm0                           #1319.14
        vandpd    1536+__svml_dacos_ha_data_internal(%rip), %ymm4, %ymm11 #1306.14
        vorpd     %ymm11, %ymm10, %ymm4                         #1313.15
        vxorpd    %ymm5, %ymm0, %ymm5                           #1320.14
        vaddpd    %ymm5, %ymm4, %ymm0                           #1321.14
        testl     %edx, %edx                                    #1323.52
        jne       ..B4.3        # Prob 5%                       #1323.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm7
..B4.2:                         # Preds ..B4.3 ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1326.12
        popq      %rbp                                          #1326.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1326.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm7, 64(%rsp)                               #1323.200
        vmovupd   %ymm0, 128(%rsp)                              #1323.276
        je        ..B4.2        # Prob 95%                      #1323.380
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1323.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B4.13:                        # Preds ..B4.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B4.7:                         # Preds ..B4.8 ..B4.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1323.523
        jc        ..B4.10       # Prob 5%                       #1323.523
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1323.476
        cmpl      $4, %r12d                                     #1323.471
        jl        ..B4.7        # Prob 82%                      #1323.471
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1323.683
        jmp       ..B4.2        # Prob 100%                     #1323.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1323.552
        lea       128(%rsp,%r12,8), %rsi                        #1323.552
..___tag_value___svml_acos4_ha_l9.72:
#       __svml_dacos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dacos_ha_cout_rare_internal            #1323.552
..___tag_value___svml_acos4_ha_l9.73:
        jmp       ..B4.8        # Prob 100%                     #1323.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_acos4_ha_l9,@function
	.size	__svml_acos4_ha_l9,.-__svml_acos4_ha_l9
..LN__svml_acos4_ha_l9.3:
	.data
# -- End  __svml_acos4_ha_l9
	.text
.L_2__routine_start___svml_acos2_ha_e9_4:
# -- Begin  __svml_acos2_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acos2_ha_e9
# --- __svml_acos2_ha_e9(__m128d)
__svml_acos2_ha_e9:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1331.1
        .byte     15                                            #1429.546
        .byte     30                                            #1429.546
        .byte     250                                           #1429.546
	.cfi_startproc
..___tag_value___svml_acos2_ha_e9.75:
..L76:
                                                         #1331.1
        pushq     %rbp                                          #1331.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1331.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1331.1
        subq      $192, %rsp                                    #1331.1
        movaps    %xmm0, %xmm7                                  #1331.1
        movups    __svml_dacos_ha_data_internal(%rip), %xmm5    #1368.44
        movups    64+__svml_dacos_ha_data_internal(%rip), %xmm8 #1369.45
        movaps    %xmm5, %xmm6                                  #1371.8
        orps      %xmm7, %xmm6                                  #1371.8
        movaps    %xmm8, %xmm9                                  #1374.20
        mulpd     %xmm6, %xmm9                                  #1374.20
        addpd     %xmm9, %xmm8                                  #1374.8
        movaps    %xmm6, %xmm0                                  #1375.9
        movaps    %xmm8, %xmm1                                  #1380.717
        cvtpd2ps  %xmm8, %xmm11                                 #1380.546
        mulpd     %xmm6, %xmm0                                  #1375.9
        cmpltpd   128+__svml_dacos_ha_data_internal(%rip), %xmm1 #1380.717
        minpd     %xmm8, %xmm0                                  #1376.9
        movlhps   %xmm11, %xmm11                                #1380.530
        movaps    %xmm0, %xmm4                                  #1379.14
        rsqrtps   %xmm11, %xmm12                                #1380.517
        cmpnltpd  %xmm8, %xmm4                                  #1379.14
        addpd     %xmm8, %xmm8                                  #1380.810
        cvtps2pd  %xmm12, %xmm13                                #1380.504
        movups    320+__svml_dacos_ha_data_internal(%rip), %xmm3 #1380.1157
        andps     %xmm4, %xmm8                                  #1380.846
        andnps    %xmm13, %xmm1                                 #1380.760
        andps     %xmm8, %xmm3                                  #1380.1157
        subpd     %xmm3, %xmm8                                  #1380.1203
        mulpd     %xmm1, %xmm3                                  #1380.1241
        mulpd     %xmm1, %xmm8                                  #1380.1279
        movaps    %xmm1, %xmm14                                 #1380.1319
        movaps    %xmm3, %xmm2                                  #1380.1488
        mulpd     %xmm3, %xmm14                                 #1380.1319
        mulpd     %xmm8, %xmm1                                  #1380.1359
        subpd     384+__svml_dacos_ha_data_internal(%rip), %xmm14 #1380.1400
        addpd     %xmm8, %xmm2                                  #1380.1488
        addpd     %xmm14, %xmm1                                 #1380.1445
        movups    448+__svml_dacos_ha_data_internal(%rip), %xmm15 #1380.1913
        movaps    %xmm0, %xmm14                                 #1387.13
        mulpd     %xmm1, %xmm15                                 #1380.1913
        mulpd     %xmm0, %xmm14                                 #1387.13
        addpd     512+__svml_dacos_ha_data_internal(%rip), %xmm15 #1380.1901
        mulpd     %xmm1, %xmm2                                  #1380.1971
        mulpd     %xmm1, %xmm15                                 #1380.2029
        movups    704+__svml_dacos_ha_data_internal(%rip), %xmm9 #1385.24
        movaps    %xmm14, %xmm11                                #1396.13
        mulpd     %xmm0, %xmm9                                  #1385.24
        addpd     576+__svml_dacos_ha_data_internal(%rip), %xmm15 #1380.2017
        addpd     768+__svml_dacos_ha_data_internal(%rip), %xmm9 #1385.12
        mulpd     %xmm14, %xmm11                                #1396.13
        mulpd     %xmm14, %xmm9                                 #1394.24
        mulpd     %xmm15, %xmm1                                 #1380.2104
        movups    256+__svml_dacos_ha_data_internal(%rip), %xmm10 #1370.42
        andps     %xmm7, %xmm5                                  #1372.12
        cmpnlepd  %xmm6, %xmm10                                 #1373.13
        addpd     640+__svml_dacos_ha_data_internal(%rip), %xmm1 #1380.2092
        movmskpd  %xmm10, %edx                                  #1378.40
        movups    832+__svml_dacos_ha_data_internal(%rip), %xmm12 #1386.27
        movups    960+__svml_dacos_ha_data_internal(%rip), %xmm10 #1390.26
        mulpd     %xmm0, %xmm12                                 #1386.27
        mulpd     %xmm0, %xmm10                                 #1390.26
        addpd     896+__svml_dacos_ha_data_internal(%rip), %xmm12 #1386.15
        addpd     1024+__svml_dacos_ha_data_internal(%rip), %xmm10 #1390.14
        mulpd     %xmm1, %xmm2                                  #1380.2174
        mulpd     %xmm14, %xmm10                                #1395.26
        addpd     %xmm9, %xmm12                                 #1394.12
        subpd     %xmm8, %xmm2                                  #1380.2162
        mulpd     %xmm11, %xmm12                                #1400.24
        movups    1088+__svml_dacos_ha_data_internal(%rip), %xmm13 #1393.26
        mulpd     %xmm0, %xmm13                                 #1393.26
        addpd     1152+__svml_dacos_ha_data_internal(%rip), %xmm13 #1393.14
        addpd     %xmm10, %xmm13                                #1395.14
        movups    1216+__svml_dacos_ha_data_internal(%rip), %xmm1 #1399.26
        movaps    %xmm4, %xmm10                                 #1415.15
        mulpd     %xmm0, %xmm1                                  #1399.26
        addpd     %xmm12, %xmm13                                #1400.12
        addpd     1280+__svml_dacos_ha_data_internal(%rip), %xmm1 #1399.14
        mulpd     %xmm13, %xmm14                                #1401.24
        addpd     %xmm14, %xmm1                                 #1401.12
        mulpd     %xmm0, %xmm1                                  #1404.24
        addpd     1344+__svml_dacos_ha_data_internal(%rip), %xmm1 #1404.12
        mulpd     %xmm0, %xmm1                                  #1405.24
        addpd     1408+__svml_dacos_ha_data_internal(%rip), %xmm1 #1405.12
        mulpd     %xmm0, %xmm1                                  #1406.12
        cmpnltpd  %xmm7, %xmm0                                  #1407.17
        movups    1472+__svml_dacos_ha_data_internal(%rip), %xmm8 #1411.14
        andps     %xmm4, %xmm0                                  #1410.17
        movups    1536+__svml_dacos_ha_data_internal(%rip), %xmm9 #1412.14
        andps     %xmm0, %xmm8                                  #1411.14
        andps     %xmm0, %xmm9                                  #1412.14
        movaps    %xmm4, %xmm0                                  #1416.15
        andnps    %xmm6, %xmm4                                  #1417.12
        orps      %xmm4, %xmm3                                  #1421.13
        movaps    %xmm3, %xmm4                                  #1422.12
        subpd     %xmm2, %xmm4                                  #1422.12
        andnps    1600+__svml_dacos_ha_data_internal(%rip), %xmm10 #1415.15
        mulpd     %xmm4, %xmm1                                  #1424.26
        orps      %xmm8, %xmm10                                 #1418.15
        pxor      %xmm5, %xmm10                                 #1420.15
        subpd     %xmm2, %xmm10                                 #1423.14
        addpd     %xmm1, %xmm10                                 #1424.14
        addpd     %xmm10, %xmm3                                 #1425.14
        andnps    1664+__svml_dacos_ha_data_internal(%rip), %xmm0 #1416.15
        pxor      %xmm5, %xmm3                                  #1426.14
        orps      %xmm9, %xmm0                                  #1419.15
        addpd     %xmm3, %xmm0                                  #1427.14
        testl     %edx, %edx                                    #1429.52
        jne       ..B5.3        # Prob 5%                       #1429.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
..B5.2:                         # Preds ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1432.12
        popq      %rbp                                          #1432.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1432.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm7, 64(%rsp)                               #1429.197
        movups    %xmm0, 128(%rsp)                              #1429.270
                                # LOE rbx r12 r13 r14 r15 edx
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1429.454
        movq      %r12, 8(%rsp)                                 #1429.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1429.454
        movq      %r13, (%rsp)                                  #1429.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1429.454
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1429.517
        jc        ..B5.10       # Prob 5%                       #1429.517
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1429.470
        cmpl      $2, %r12d                                     #1429.465
        jl        ..B5.7        # Prob 82%                      #1429.465
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1429.674
        jmp       ..B5.2        # Prob 100%                     #1429.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1429.546
        lea       128(%rsp,%r12,8), %rsi                        #1429.546
..___tag_value___svml_acos2_ha_e9.93:
#       __svml_dacos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dacos_ha_cout_rare_internal            #1429.546
..___tag_value___svml_acos2_ha_e9.94:
        jmp       ..B5.8        # Prob 100%                     #1429.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_acos2_ha_e9,@function
	.size	__svml_acos2_ha_e9,.-__svml_acos2_ha_e9
..LN__svml_acos2_ha_e9.4:
	.data
# -- End  __svml_acos2_ha_e9
	.text
.L_2__routine_start___svml_acos4_ha_e9_5:
# -- Begin  __svml_acos4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acos4_ha_e9
# --- __svml_acos4_ha_e9(__m256d)
__svml_acos4_ha_e9:
# parameter 1: %ymm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1437.1
        .byte     15                                            #1535.552
        .byte     30                                            #1535.552
        .byte     250                                           #1535.552
	.cfi_startproc
..___tag_value___svml_acos4_ha_e9.96:
..L97:
                                                         #1437.1
        pushq     %rbp                                          #1437.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1437.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1437.1
        subq      $192, %rsp                                    #1437.1
        vmovupd   __svml_dacos_ha_data_internal(%rip), %ymm10   #1474.47
        vmovupd   64+__svml_dacos_ha_data_internal(%rip), %ymm11 #1475.48
        vmovapd   %ymm0, %ymm9                                  #1437.1
        vorpd     %ymm9, %ymm10, %ymm8                          #1477.8
        vmulpd    %ymm11, %ymm8, %ymm12                         #1480.23
        vmulpd    %ymm8, %ymm8, %ymm13                          #1481.9
        vcmpnge_uqpd 256+__svml_dacos_ha_data_internal(%rip), %ymm8, %ymm14 #1479.13
        vandpd    %ymm9, %ymm10, %ymm7                          #1478.12
        vaddpd    %ymm12, %ymm11, %ymm10                        #1480.8
        vcvtpd2ps %ymm10, %xmm2                                 #1486.533
        vminpd    %ymm10, %ymm13, %ymm6                         #1482.9
        vaddpd    %ymm10, %ymm10, %ymm11                        #1486.775
        vcmplt_oqpd 128+__svml_dacos_ha_data_internal(%rip), %ymm10, %ymm1 #1486.666
        vcmpnlt_uqpd %ymm10, %ymm6, %ymm5                       #1485.14
        vrsqrtps  %xmm2, %xmm4                                  #1486.520
        vcvtps2pd %xmm4, %ymm3                                  #1486.504
        vandpd    %ymm5, %ymm11, %ymm12                         #1486.814
        vandpd    320+__svml_dacos_ha_data_internal(%rip), %ymm12, %ymm13 #1486.1134
        vandnpd   %ymm8, %ymm5, %ymm8                           #1523.12
        vextractf128 $1, %ymm14, %xmm15                         #1483.114
        vshufps   $221, %xmm15, %xmm14, %xmm0                   #1484.57
        vandnpd   %ymm3, %ymm1, %ymm15                          #1486.722
        vsubpd    %ymm13, %ymm12, %ymm14                        #1486.1183
        vmovmskps %xmm0, %edx                                   #1484.40
        vmulpd    %ymm13, %ymm15, %ymm4                         #1486.1224
        vmulpd    %ymm14, %ymm15, %ymm3                         #1486.1265
        vmulpd    %ymm4, %ymm15, %ymm0                          #1486.1308
        vmulpd    %ymm3, %ymm15, %ymm2                          #1486.1351
        vaddpd    %ymm3, %ymm4, %ymm11                          #1486.1489
        vsubpd    384+__svml_dacos_ha_data_internal(%rip), %ymm0, %ymm1 #1486.1395
        vaddpd    %ymm1, %ymm2, %ymm14                          #1486.1443
        vmulpd    448+__svml_dacos_ha_data_internal(%rip), %ymm14, %ymm10 #1486.1932
        vmulpd    %ymm11, %ymm14, %ymm2                         #1486.1993
        vaddpd    512+__svml_dacos_ha_data_internal(%rip), %ymm10, %ymm12 #1486.1917
        vmulpd    %ymm12, %ymm14, %ymm13                        #1486.2057
        vaddpd    576+__svml_dacos_ha_data_internal(%rip), %ymm13, %ymm15 #1486.2042
        vmulpd    %ymm15, %ymm14, %ymm0                         #1486.2138
        vaddpd    640+__svml_dacos_ha_data_internal(%rip), %ymm0, %ymm1 #1486.2123
        vmulpd    832+__svml_dacos_ha_data_internal(%rip), %ymm6, %ymm0 #1492.30
        vmulpd    %ymm1, %ymm2, %ymm10                          #1486.2214
        vmulpd    %ymm6, %ymm6, %ymm1                           #1493.13
        vaddpd    896+__svml_dacos_ha_data_internal(%rip), %ymm0, %ymm12 #1492.15
        vsubpd    %ymm3, %ymm10, %ymm2                          #1486.2199
        vmulpd    704+__svml_dacos_ha_data_internal(%rip), %ymm6, %ymm3 #1491.27
        vmulpd    1088+__svml_dacos_ha_data_internal(%rip), %ymm6, %ymm10 #1499.29
        vaddpd    768+__svml_dacos_ha_data_internal(%rip), %ymm3, %ymm11 #1491.12
        vaddpd    1152+__svml_dacos_ha_data_internal(%rip), %ymm10, %ymm15 #1499.14
        vmulpd    960+__svml_dacos_ha_data_internal(%rip), %ymm6, %ymm3 #1496.29
        vmulpd    %ymm1, %ymm11, %ymm13                         #1500.27
        vmulpd    %ymm1, %ymm1, %ymm10                          #1502.13
        vaddpd    1024+__svml_dacos_ha_data_internal(%rip), %ymm3, %ymm14 #1496.14
        vaddpd    %ymm13, %ymm12, %ymm3                         #1500.12
        vmulpd    %ymm14, %ymm1, %ymm0                          #1501.29
        vmulpd    %ymm10, %ymm3, %ymm12                         #1506.27
        vmulpd    1216+__svml_dacos_ha_data_internal(%rip), %ymm6, %ymm14 #1505.29
        vaddpd    %ymm0, %ymm15, %ymm11                         #1501.14
        vaddpd    1280+__svml_dacos_ha_data_internal(%rip), %ymm14, %ymm15 #1505.14
        vaddpd    %ymm12, %ymm11, %ymm13                        #1506.12
        vmulpd    %ymm13, %ymm1, %ymm1                          #1507.27
        vaddpd    %ymm1, %ymm15, %ymm0                          #1507.12
        vmulpd    %ymm0, %ymm6, %ymm1                           #1510.27
        vaddpd    1344+__svml_dacos_ha_data_internal(%rip), %ymm1, %ymm3 #1510.12
        vmulpd    %ymm3, %ymm6, %ymm10                          #1511.27
        vaddpd    1408+__svml_dacos_ha_data_internal(%rip), %ymm10, %ymm11 #1511.12
        vmulpd    %ymm11, %ymm6, %ymm0                          #1512.12
        vcmpnlt_uqpd %ymm9, %ymm6, %ymm6                        #1513.17
        vorpd     %ymm8, %ymm4, %ymm8                           #1527.13
        vsubpd    %ymm2, %ymm8, %ymm4                           #1528.12
        vandpd    %ymm5, %ymm6, %ymm1                           #1516.17
        vandpd    1472+__svml_dacos_ha_data_internal(%rip), %ymm1, %ymm6 #1517.14
        vandnpd   1600+__svml_dacos_ha_data_internal(%rip), %ymm5, %ymm3 #1521.15
        vandnpd   1664+__svml_dacos_ha_data_internal(%rip), %ymm5, %ymm10 #1522.15
        vorpd     %ymm6, %ymm3, %ymm5                           #1524.15
        vmulpd    %ymm4, %ymm0, %ymm0                           #1530.29
        vxorpd    %ymm7, %ymm5, %ymm5                           #1526.15
        vsubpd    %ymm2, %ymm5, %ymm2                           #1529.14
        vandpd    1536+__svml_dacos_ha_data_internal(%rip), %ymm1, %ymm11 #1518.14
        vaddpd    %ymm0, %ymm2, %ymm1                           #1530.14
        vaddpd    %ymm1, %ymm8, %ymm3                           #1531.14
        vorpd     %ymm11, %ymm10, %ymm6                         #1525.15
        vxorpd    %ymm7, %ymm3, %ymm7                           #1532.14
        vaddpd    %ymm7, %ymm6, %ymm0                           #1533.14
        testl     %edx, %edx                                    #1535.52
        jne       ..B6.3        # Prob 5%                       #1535.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm9
..B6.2:                         # Preds ..B6.3 ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1538.12
        popq      %rbp                                          #1538.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1538.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm9, 64(%rsp)                               #1535.200
        vmovupd   %ymm0, 128(%rsp)                              #1535.276
        je        ..B6.2        # Prob 95%                      #1535.380
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1535.460
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
        btl       %r12d, %r13d                                  #1535.523
        jc        ..B6.10       # Prob 5%                       #1535.523
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1535.476
        cmpl      $4, %r12d                                     #1535.471
        jl        ..B6.7        # Prob 82%                      #1535.471
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1535.683
        jmp       ..B6.2        # Prob 100%                     #1535.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1535.552
        lea       128(%rsp,%r12,8), %rsi                        #1535.552
..___tag_value___svml_acos4_ha_e9.114:
#       __svml_dacos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dacos_ha_cout_rare_internal            #1535.552
..___tag_value___svml_acos4_ha_e9.115:
        jmp       ..B6.8        # Prob 100%                     #1535.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_acos4_ha_e9,@function
	.size	__svml_acos4_ha_e9,.-__svml_acos4_ha_e9
..LN__svml_acos4_ha_e9.5:
	.data
# -- End  __svml_acos4_ha_e9
	.text
.L_2__routine_start___svml_acos8_ha_z0_6:
# -- Begin  __svml_acos8_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acos8_ha_z0
# --- __svml_acos8_ha_z0(__m512d)
__svml_acos8_ha_z0:
# parameter 1: %zmm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1543.1
        .byte     15                                            #1645.12
        .byte     30                                            #1645.12
        .byte     250                                           #1645.12
	.cfi_startproc
..___tag_value___svml_acos8_ha_z0.117:
..L118:
                                                        #1543.1
        vmovups   __svml_dacos_ha_data_internal(%rip), %zmm8    #1581.45
        vmovups   64+__svml_dacos_ha_data_internal(%rip), %zmm9 #1582.46
        vmovups   128+__svml_dacos_ha_data_internal(%rip), %zmm12 #1594.575
        vmovups   384+__svml_dacos_ha_data_internal(%rip), %zmm1 #1594.1222
        vmovups   448+__svml_dacos_ha_data_internal(%rip), %zmm15 #1594.1599
        vmovups   512+__svml_dacos_ha_data_internal(%rip), %zmm2 #1594.1940
        vmovups   256+__svml_dacos_ha_data_internal(%rip), %zmm11 #1583.43
        vmovaps   %zmm0, %zmm7                                  #1543.1
        vorpd     %zmm7, %zmm8, %zmm6                           #1584.8
        vandpd    %zmm7, %zmm8, %zmm5                           #1585.12
        vmovups   576+__svml_dacos_ha_data_internal(%rip), %zmm0 #1594.1785
        vfmadd231pd {rn-sae}, %zmm6, %zmm9, %zmm9               #1587.8
        vmulpd    {rn-sae}, %zmm6, %zmm6, %zmm10                #1588.11
        vrsqrt14pd %zmm9, %zmm14                                #1594.505
        vcmppd    $17, {sae}, %zmm12, %zmm9, %k2                #1594.636
        vcmppd    $17, {sae}, %zmm11, %zmm6, %k1                #1586.13
        vmovups   960+__svml_dacos_ha_data_internal(%rip), %zmm11 #1602.56
        vmovups   1088+__svml_dacos_ha_data_internal(%rip), %zmm12 #1605.56
        vminpd    {sae}, %zmm9, %zmm10, %zmm4                   #1589.11
        vmovups   832+__svml_dacos_ha_data_internal(%rip), %zmm10 #1597.57
        vxorpd    %zmm14, %zmm14, %zmm14{%k2}                   #1594.722
        vaddpd    {rn-sae}, %zmm9, %zmm9, %zmm8                 #1594.794
        vcmppd    $21, {sae}, %zmm9, %zmm4, %k4                 #1590.14
        vmulpd    {rn-sae}, %zmm14, %zmm14, %zmm13              #1594.1118
        vmulpd    {rn-sae}, %zmm14, %zmm8, %zmm3                #1594.1335
        vmovups   704+__svml_dacos_ha_data_internal(%rip), %zmm9 #1595.57
        vorpd     1856+__svml_dacos_ha_data_internal(%rip), %zmm4, %zmm4{%k1} #1593.11
        vfmsub231pd {rn-sae}, %zmm8, %zmm13, %zmm1              #1594.1222
        vmovups   640+__svml_dacos_ha_data_internal(%rip), %zmm13 #1594.1878
        vfmsub213pd {rn-sae}, %zmm3, %zmm14, %zmm8              #1594.1436
        vcmppd    $21, {sae}, %zmm7, %zmm4, %k0                 #1621.17
        vmulpd    {rn-sae}, %zmm1, %zmm3, %zmm14                #1594.2058
        vfmadd231pd {rn-sae}, %zmm1, %zmm15, %zmm2              #1594.1940
        vmovups   1216+__svml_dacos_ha_data_internal(%rip), %zmm15 #1611.56
        vmovups   1664+__svml_dacos_ha_data_internal(%rip), %zmm7 #1624.47
        vfmadd213pd {rn-sae}, %zmm0, %zmm1, %zmm2               #1594.2166
        vmovups   896+__svml_dacos_ha_data_internal(%rip), %zmm0 #1600.15
        vxorpd    %zmm7, %zmm7, %zmm7{%k4}                      #1629.15
        vfmadd213pd {rn-sae}, %zmm13, %zmm1, %zmm2              #1594.2287
        vmovups   768+__svml_dacos_ha_data_internal(%rip), %zmm1 #1599.12
        vfmadd231pd {rn-sae}, %zmm4, %zmm10, %zmm0              #1600.15
        vmovups   1280+__svml_dacos_ha_data_internal(%rip), %zmm10 #1613.14
        vfmsub213pd {rn-sae}, %zmm8, %zmm14, %zmm2              #1594.2403
        vfmadd231pd {rn-sae}, %zmm4, %zmm9, %zmm1               #1599.12
        vmovups   1024+__svml_dacos_ha_data_internal(%rip), %zmm8 #1604.14
        vfmadd231pd {rn-sae}, %zmm4, %zmm15, %zmm10             #1613.14
        vmulpd    {rn-sae}, %zmm4, %zmm4, %zmm9                 #1601.15
        vfmadd231pd {rn-sae}, %zmm4, %zmm11, %zmm8              #1604.14
        vmovups   1152+__svml_dacos_ha_data_internal(%rip), %zmm11 #1607.14
        vfmadd213pd {rn-sae}, %zmm0, %zmm9, %zmm1               #1608.12
        vmulpd    {rn-sae}, %zmm9, %zmm9, %zmm0                 #1610.15
        vfmadd231pd {rn-sae}, %zmm4, %zmm12, %zmm11             #1607.14
        vmovups   1408+__svml_dacos_ha_data_internal(%rip), %zmm12 #1617.56
        kmovw     %k4, %eax                                     #1590.14
        kmovw     %k0, %edx                                     #1621.17
        vfmadd213pd {rn-sae}, %zmm11, %zmm9, %zmm8              #1609.14
        vmovups   1344+__svml_dacos_ha_data_internal(%rip), %zmm11 #1616.56
        vfmadd213pd {rn-sae}, %zmm8, %zmm0, %zmm1               #1614.12
        vfmadd213pd {rn-sae}, %zmm10, %zmm9, %zmm1              #1615.12
        andl      %eax, %edx                                    #1622.29
        kmovw     %edx, %k3                                     #1630.15
        vfmadd213pd {rn-sae}, %zmm11, %zmm4, %zmm1              #1618.12
        vorpd     1536+__svml_dacos_ha_data_internal(%rip), %zmm7, %zmm7{%k3} #1631.15
        vfmadd213pd {rn-sae}, %zmm12, %zmm4, %zmm1              #1619.12
        vmulpd    {rn-sae}, %zmm4, %zmm1, %zmm0                 #1620.14
        vmovups   1600+__svml_dacos_ha_data_internal(%rip), %zmm4 #1623.47
        vmovups   1728+__svml_dacos_ha_data_internal(%rip), %zmm1 #1627.47
        vxorpd    %zmm4, %zmm4, %zmm4{%k4}                      #1628.15
        vblendmpd %zmm2, %zmm1, %zmm2{%k4}                      #1633.13
        vblendmpd %zmm3, %zmm6, %zmm1{%k4}                      #1634.13
        vorpd     1472+__svml_dacos_ha_data_internal(%rip), %zmm4, %zmm4{%k3} #1630.15
        vsubpd    {rn-sae}, %zmm2, %zmm1, %zmm3                 #1635.14
        vxorpd    %zmm5, %zmm4, %zmm8                           #1632.15
        vsubpd    {rn-sae}, %zmm2, %zmm8, %zmm6                 #1636.17
        vfmadd213pd {rn-sae}, %zmm6, %zmm3, %zmm0               #1637.14
        vaddpd    {rn-sae}, %zmm1, %zmm0, %zmm0                 #1638.16
        vxorpd    %zmm5, %zmm0, %zmm5                           #1639.14
        vaddpd    {rn-sae}, %zmm7, %zmm5, %zmm0                 #1640.16
        ret                                                     #1645.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_acos8_ha_z0,@function
	.size	__svml_acos8_ha_z0,.-__svml_acos8_ha_z0
..LN__svml_acos8_ha_z0.6:
	.data
# -- End  __svml_acos8_ha_z0
	.text
.L_2__routine_start___svml_acos2_ha_ex_7:
# -- Begin  __svml_acos2_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acos2_ha_ex
# --- __svml_acos2_ha_ex(__m128d)
__svml_acos2_ha_ex:
# parameter 1: %xmm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1650.1
        .byte     15                                            #1748.546
        .byte     30                                            #1748.546
        .byte     250                                           #1748.546
	.cfi_startproc
..___tag_value___svml_acos2_ha_ex.120:
..L121:
                                                        #1650.1
        pushq     %rbp                                          #1650.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1650.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1650.1
        subq      $192, %rsp                                    #1650.1
        movaps    %xmm0, %xmm7                                  #1650.1
        movups    __svml_dacos_ha_data_internal(%rip), %xmm5    #1687.44
        movups    64+__svml_dacos_ha_data_internal(%rip), %xmm8 #1688.45
        movaps    %xmm5, %xmm6                                  #1690.8
        orps      %xmm7, %xmm6                                  #1690.8
        movaps    %xmm8, %xmm9                                  #1693.20
        mulpd     %xmm6, %xmm9                                  #1693.20
        addpd     %xmm9, %xmm8                                  #1693.8
        movaps    %xmm6, %xmm0                                  #1694.9
        movaps    %xmm8, %xmm1                                  #1699.717
        cvtpd2ps  %xmm8, %xmm11                                 #1699.546
        mulpd     %xmm6, %xmm0                                  #1694.9
        cmpltpd   128+__svml_dacos_ha_data_internal(%rip), %xmm1 #1699.717
        minpd     %xmm8, %xmm0                                  #1695.9
        movlhps   %xmm11, %xmm11                                #1699.530
        movaps    %xmm0, %xmm4                                  #1698.14
        rsqrtps   %xmm11, %xmm12                                #1699.517
        cmpnltpd  %xmm8, %xmm4                                  #1698.14
        addpd     %xmm8, %xmm8                                  #1699.810
        cvtps2pd  %xmm12, %xmm13                                #1699.504
        movups    320+__svml_dacos_ha_data_internal(%rip), %xmm3 #1699.1157
        andps     %xmm4, %xmm8                                  #1699.846
        andnps    %xmm13, %xmm1                                 #1699.760
        andps     %xmm8, %xmm3                                  #1699.1157
        subpd     %xmm3, %xmm8                                  #1699.1203
        mulpd     %xmm1, %xmm3                                  #1699.1241
        mulpd     %xmm1, %xmm8                                  #1699.1279
        movaps    %xmm1, %xmm14                                 #1699.1319
        movaps    %xmm3, %xmm2                                  #1699.1488
        mulpd     %xmm3, %xmm14                                 #1699.1319
        mulpd     %xmm8, %xmm1                                  #1699.1359
        subpd     384+__svml_dacos_ha_data_internal(%rip), %xmm14 #1699.1400
        addpd     %xmm8, %xmm2                                  #1699.1488
        addpd     %xmm14, %xmm1                                 #1699.1445
        movups    448+__svml_dacos_ha_data_internal(%rip), %xmm15 #1699.1913
        movaps    %xmm0, %xmm14                                 #1706.13
        mulpd     %xmm1, %xmm15                                 #1699.1913
        mulpd     %xmm0, %xmm14                                 #1706.13
        addpd     512+__svml_dacos_ha_data_internal(%rip), %xmm15 #1699.1901
        mulpd     %xmm1, %xmm2                                  #1699.1971
        mulpd     %xmm1, %xmm15                                 #1699.2029
        movups    704+__svml_dacos_ha_data_internal(%rip), %xmm9 #1704.24
        movaps    %xmm14, %xmm11                                #1715.13
        mulpd     %xmm0, %xmm9                                  #1704.24
        addpd     576+__svml_dacos_ha_data_internal(%rip), %xmm15 #1699.2017
        addpd     768+__svml_dacos_ha_data_internal(%rip), %xmm9 #1704.12
        mulpd     %xmm14, %xmm11                                #1715.13
        mulpd     %xmm14, %xmm9                                 #1713.24
        mulpd     %xmm15, %xmm1                                 #1699.2104
        movups    256+__svml_dacos_ha_data_internal(%rip), %xmm10 #1689.42
        andps     %xmm7, %xmm5                                  #1691.12
        cmpnlepd  %xmm6, %xmm10                                 #1692.13
        addpd     640+__svml_dacos_ha_data_internal(%rip), %xmm1 #1699.2092
        movmskpd  %xmm10, %edx                                  #1697.40
        movups    832+__svml_dacos_ha_data_internal(%rip), %xmm12 #1705.27
        movups    960+__svml_dacos_ha_data_internal(%rip), %xmm10 #1709.26
        mulpd     %xmm0, %xmm12                                 #1705.27
        mulpd     %xmm0, %xmm10                                 #1709.26
        addpd     896+__svml_dacos_ha_data_internal(%rip), %xmm12 #1705.15
        addpd     1024+__svml_dacos_ha_data_internal(%rip), %xmm10 #1709.14
        mulpd     %xmm1, %xmm2                                  #1699.2174
        mulpd     %xmm14, %xmm10                                #1714.26
        addpd     %xmm9, %xmm12                                 #1713.12
        subpd     %xmm8, %xmm2                                  #1699.2162
        mulpd     %xmm11, %xmm12                                #1719.24
        movups    1088+__svml_dacos_ha_data_internal(%rip), %xmm13 #1712.26
        mulpd     %xmm0, %xmm13                                 #1712.26
        addpd     1152+__svml_dacos_ha_data_internal(%rip), %xmm13 #1712.14
        addpd     %xmm10, %xmm13                                #1714.14
        movups    1216+__svml_dacos_ha_data_internal(%rip), %xmm1 #1718.26
        movaps    %xmm4, %xmm10                                 #1734.15
        mulpd     %xmm0, %xmm1                                  #1718.26
        addpd     %xmm12, %xmm13                                #1719.12
        addpd     1280+__svml_dacos_ha_data_internal(%rip), %xmm1 #1718.14
        mulpd     %xmm13, %xmm14                                #1720.24
        addpd     %xmm14, %xmm1                                 #1720.12
        mulpd     %xmm0, %xmm1                                  #1723.24
        addpd     1344+__svml_dacos_ha_data_internal(%rip), %xmm1 #1723.12
        mulpd     %xmm0, %xmm1                                  #1724.24
        addpd     1408+__svml_dacos_ha_data_internal(%rip), %xmm1 #1724.12
        mulpd     %xmm0, %xmm1                                  #1725.12
        cmpnltpd  %xmm7, %xmm0                                  #1726.17
        movups    1472+__svml_dacos_ha_data_internal(%rip), %xmm8 #1730.14
        andps     %xmm4, %xmm0                                  #1729.17
        movups    1536+__svml_dacos_ha_data_internal(%rip), %xmm9 #1731.14
        andps     %xmm0, %xmm8                                  #1730.14
        andps     %xmm0, %xmm9                                  #1731.14
        movaps    %xmm4, %xmm0                                  #1735.15
        andnps    %xmm6, %xmm4                                  #1736.12
        orps      %xmm4, %xmm3                                  #1740.13
        movaps    %xmm3, %xmm4                                  #1741.12
        subpd     %xmm2, %xmm4                                  #1741.12
        andnps    1600+__svml_dacos_ha_data_internal(%rip), %xmm10 #1734.15
        mulpd     %xmm4, %xmm1                                  #1743.26
        orps      %xmm8, %xmm10                                 #1737.15
        pxor      %xmm5, %xmm10                                 #1739.15
        subpd     %xmm2, %xmm10                                 #1742.14
        addpd     %xmm1, %xmm10                                 #1743.14
        addpd     %xmm10, %xmm3                                 #1744.14
        andnps    1664+__svml_dacos_ha_data_internal(%rip), %xmm0 #1735.15
        pxor      %xmm5, %xmm3                                  #1745.14
        orps      %xmm9, %xmm0                                  #1738.15
        addpd     %xmm3, %xmm0                                  #1746.14
        testl     %edx, %edx                                    #1748.52
        jne       ..B8.3        # Prob 5%                       #1748.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
..B8.2:                         # Preds ..B8.9 ..B8.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1751.12
        popq      %rbp                                          #1751.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1751.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.3:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm7, 64(%rsp)                               #1748.197
        movups    %xmm0, 128(%rsp)                              #1748.270
                                # LOE rbx r12 r13 r14 r15 edx
..B8.6:                         # Preds ..B8.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1748.454
        movq      %r12, 8(%rsp)                                 #1748.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1748.454
        movq      %r13, (%rsp)                                  #1748.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1748.454
                                # LOE rbx r12 r14 r15 r13d
..B8.7:                         # Preds ..B8.8 ..B8.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1748.517
        jc        ..B8.10       # Prob 5%                       #1748.517
                                # LOE rbx r12 r14 r15 r13d
..B8.8:                         # Preds ..B8.10 ..B8.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1748.470
        cmpl      $2, %r12d                                     #1748.465
        jl        ..B8.7        # Prob 82%                      #1748.465
                                # LOE rbx r12 r14 r15 r13d
..B8.9:                         # Preds ..B8.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1748.674
        jmp       ..B8.2        # Prob 100%                     #1748.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B8.10:                        # Preds ..B8.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1748.546
        lea       128(%rsp,%r12,8), %rsi                        #1748.546
..___tag_value___svml_acos2_ha_ex.138:
#       __svml_dacos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dacos_ha_cout_rare_internal            #1748.546
..___tag_value___svml_acos2_ha_ex.139:
        jmp       ..B8.8        # Prob 100%                     #1748.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_acos2_ha_ex,@function
	.size	__svml_acos2_ha_ex,.-__svml_acos2_ha_ex
..LN__svml_acos2_ha_ex.7:
	.data
# -- End  __svml_acos2_ha_ex
	.text
.L_2__routine_start___svml_acos1_ha_l9_8:
# -- Begin  __svml_acos1_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acos1_ha_l9
# --- __svml_acos1_ha_l9(__m128d)
__svml_acos1_ha_l9:
# parameter 1: %xmm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1756.1
        .byte     15                                            #1854.546
        .byte     30                                            #1854.546
        .byte     250                                           #1854.546
	.cfi_startproc
..___tag_value___svml_acos1_ha_l9.141:
..L142:
                                                        #1756.1
        pushq     %rbp                                          #1756.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1756.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1756.1
        subq      $128, %rsp                                    #1756.1
        vmovapd   %xmm0, %xmm11                                 #1756.1
        vmovsd    __svml_dacos_ha_data_internal(%rip), %xmm12   #1793.13
        vorpd     %xmm11, %xmm12, %xmm10                        #1796.8
        vandpd    %xmm11, %xmm12, %xmm9                         #1797.12
        vmovsd    64+__svml_dacos_ha_data_internal(%rip), %xmm15 #1794.14
        vmovapd   %xmm10, %xmm6                                 #1799.8
        vfmadd213sd %xmm15, %xmm15, %xmm6                       #1799.8
        vmulsd    %xmm10, %xmm10, %xmm0                         #1800.9
        vcvtpd2ps %xmm6, %xmm3                                  #1805.546
        vcmpltsd  128+__svml_dacos_ha_data_internal(%rip), %xmm6, %xmm12 #1805.717
        vshufps   $0, %xmm3, %xmm3, %xmm1                       #1805.530
        vrsqrtps  %xmm1, %xmm2                                  #1805.517
        vmovsd    256+__svml_dacos_ha_data_internal(%rip), %xmm13 #1795.11
        vunpcklpd %xmm0, %xmm0, %xmm4                           #1801.21
        vcmpnlesd %xmm10, %xmm13, %xmm14                        #1798.13
        vcvtps2pd %xmm2, %xmm13                                 #1805.504
        vunpcklpd %xmm6, %xmm6, %xmm5                           #1801.72
        vmovsd    %xmm14, %xmm10, %xmm7                         #1798.13
        vminpd    %xmm5, %xmm4, %xmm8                           #1801.9
        vmovmskpd %xmm7, %eax                                   #1803.40
        vandnpd   %xmm13, %xmm12, %xmm15                        #1805.760
        vcmpnltsd %xmm6, %xmm8, %xmm7                           #1804.14
        vmulsd    %xmm15, %xmm15, %xmm14                        #1805.1112
        vaddsd    %xmm6, %xmm6, %xmm6                           #1805.810
        vandpd    %xmm7, %xmm6, %xmm1                           #1805.846
        vmovapd   %xmm1, %xmm0                                  #1805.1154
        vfmsub213sd 384+__svml_dacos_ha_data_internal(%rip), %xmm14, %xmm0 #1805.1154
        vmovsd    448+__svml_dacos_ha_data_internal(%rip), %xmm4 #1805.1688
        vmovsd    832+__svml_dacos_ha_data_internal(%rip), %xmm6 #1808.25
        vmovsd    704+__svml_dacos_ha_data_internal(%rip), %xmm2 #1806.25
        vmovsd    960+__svml_dacos_ha_data_internal(%rip), %xmm12 #1813.24
        vfmadd213sd 512+__svml_dacos_ha_data_internal(%rip), %xmm0, %xmm4 #1805.1688
        vmulsd    %xmm15, %xmm1, %xmm5                          #1805.1207
        vfmadd213sd 576+__svml_dacos_ha_data_internal(%rip), %xmm0, %xmm4 #1805.1792
        vfmsub213sd %xmm5, %xmm15, %xmm1                        #1805.1246
        vmulsd    %xmm0, %xmm5, %xmm3                           #1805.1746
        vfmadd213sd 640+__svml_dacos_ha_data_internal(%rip), %xmm0, %xmm4 #1805.1855
        vmovapd   %xmm8, %xmm0                                  #1811.15
        vmovsd    1088+__svml_dacos_ha_data_internal(%rip), %xmm13 #1816.24
        vmovsd    1216+__svml_dacos_ha_data_internal(%rip), %xmm14 #1822.24
        vfmsub213sd %xmm1, %xmm3, %xmm4                         #1805.1913
        vmovapd   %xmm8, %xmm3                                  #1810.12
        vfmadd213sd 896+__svml_dacos_ha_data_internal(%rip), %xmm6, %xmm0 #1811.15
        vmovapd   %xmm8, %xmm1                                  #1815.14
        vmovapd   %xmm8, %xmm6                                  #1818.14
        vfmadd213sd 768+__svml_dacos_ha_data_internal(%rip), %xmm2, %xmm3 #1810.12
        vmulsd    %xmm8, %xmm8, %xmm2                           #1812.13
        vfmadd213sd 1024+__svml_dacos_ha_data_internal(%rip), %xmm12, %xmm1 #1815.14
        vfmadd213sd 1152+__svml_dacos_ha_data_internal(%rip), %xmm13, %xmm6 #1818.14
        vmovapd   %xmm8, %xmm12                                 #1824.14
        vfmadd213sd %xmm0, %xmm2, %xmm3                         #1819.12
        vfmadd213sd %xmm6, %xmm2, %xmm1                         #1820.14
        vmulsd    %xmm2, %xmm2, %xmm15                          #1821.13
        vfmadd213sd 1280+__svml_dacos_ha_data_internal(%rip), %xmm14, %xmm12 #1824.14
        vmovsd    1600+__svml_dacos_ha_data_internal(%rip), %xmm6 #1838.15
        vfmadd213sd %xmm1, %xmm15, %xmm3                        #1825.12
        vmovsd    1472+__svml_dacos_ha_data_internal(%rip), %xmm1 #1833.14
        vfmadd213sd %xmm12, %xmm2, %xmm3                        #1826.12
        vandnpd   %xmm6, %xmm7, %xmm12                          #1840.15
        vmovsd    1536+__svml_dacos_ha_data_internal(%rip), %xmm2 #1834.14
        vfmadd213sd 1344+__svml_dacos_ha_data_internal(%rip), %xmm8, %xmm3 #1829.12
        vfmadd213sd 1408+__svml_dacos_ha_data_internal(%rip), %xmm8, %xmm3 #1830.12
        vmulsd    %xmm8, %xmm3, %xmm0                           #1831.12
        vcmpnltsd %xmm11, %xmm8, %xmm8                          #1832.17
        vandpd    %xmm7, %xmm8, %xmm3                           #1835.17
        vandpd    %xmm3, %xmm1, %xmm13                          #1836.14
        vandnpd   %xmm10, %xmm7, %xmm1                          #1842.12
        vorpd     %xmm13, %xmm12, %xmm10                        #1843.15
        vandpd    %xmm3, %xmm2, %xmm15                          #1837.14
        vxorpd    %xmm9, %xmm10, %xmm10                         #1845.15
        vorpd     %xmm1, %xmm5, %xmm2                           #1846.13
        vmovsd    1664+__svml_dacos_ha_data_internal(%rip), %xmm8 #1839.15
        vsubsd    %xmm4, %xmm2, %xmm5                           #1847.12
        vsubsd    %xmm4, %xmm10, %xmm4                          #1848.14
        vfmadd213sd %xmm4, %xmm5, %xmm0                         #1849.14
        vandnpd   %xmm8, %xmm7, %xmm14                          #1841.15
        vorpd     %xmm15, %xmm14, %xmm3                         #1844.15
        vaddsd    %xmm2, %xmm0, %xmm0                           #1850.14
        vxorpd    %xmm9, %xmm0, %xmm9                           #1851.14
        vaddsd    %xmm3, %xmm9, %xmm0                           #1852.14
        andl      $1, %eax                                      #1803.91
        jne       ..B9.3        # Prob 5%                       #1854.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm11
..B9.2:                         # Preds ..B9.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1857.12
        popq      %rbp                                          #1857.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1857.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.3:                         # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm11, (%rsp)                                #1854.160
        vmovsd    %xmm0, 64(%rsp)                               #1854.233
        jne       ..B9.5        # Prob 5%                       #1854.374
                                # LOE rbx r12 r13 r14 r15 eax
..B9.4:                         # Preds ..B9.6 ..B9.5 ..B9.3
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm0                               #1854.626
        movq      %rbp, %rsp                                    #1854.626
        popq      %rbp                                          #1854.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1854.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B9.5:                         # Preds ..B9.3
                                # Execution count [2.50e-03]: Infreq
        je        ..B9.4        # Prob 95%                      #1854.517
                                # LOE rbx r12 r13 r14 r15
..B9.6:                         # Preds ..B9.5
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1854.546
        lea       64(%rsp), %rsi                                #1854.546
..___tag_value___svml_acos1_ha_l9.154:
#       __svml_dacos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dacos_ha_cout_rare_internal            #1854.546
..___tag_value___svml_acos1_ha_l9.155:
        jmp       ..B9.4        # Prob 100%                     #1854.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_acos1_ha_l9,@function
	.size	__svml_acos1_ha_l9,.-__svml_acos1_ha_l9
..LN__svml_acos1_ha_l9.8:
	.data
# -- End  __svml_acos1_ha_l9
	.text
.L_2__routine_start___svml_dacos_ha_cout_rare_internal_9:
# -- Begin  __svml_dacos_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dacos_ha_cout_rare_internal
	.globl __svml_dacos_ha_cout_rare_internal
# --- __svml_dacos_ha_cout_rare_internal(const double *, double *)
__svml_dacos_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #934.1
        .byte     15                                            #1111.17
        .byte     30                                            #1111.17
        .byte     250                                           #1111.17
	.cfi_startproc
..___tag_value___svml_dacos_ha_cout_rare_internal.157:
..L158:
                                                        #934.1
        xorl      %eax, %eax                                    #947.14
        movzwl    6(%rdi), %edx                                 #948.35
        andl      $32752, %edx                                  #948.35
        movb      7(%rdi), %cl                                  #950.23
        movsd     (%rdi), %xmm1                                 #950.23
        cmpl      $32752, %edx                                  #948.59
        je        ..B10.20      # Prob 16%                      #948.59
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax cl xmm1
..B10.2:                        # Preds ..B10.1
                                # Execution count [8.40e-01]
        movsd     %xmm1, -32(%rsp)                              #950.13
        andb      $127, %cl                                     #951.34
        movb      %cl, -25(%rsp)                                #951.34
        movsd     -32(%rsp), %xmm12                             #952.18
        movsd     4168+_vmldACosHATab(%rip), %xmm0              #952.44
        comisd    %xmm12, %xmm0                                 #952.44
        jbe       ..B10.14      # Prob 50%                      #952.44
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm1 xmm12
..B10.3:                        # Preds ..B10.2
                                # Execution count [4.20e-01]
        movsd     4176+_vmldACosHATab(%rip), %xmm1              #954.48
        comisd    %xmm12, %xmm1                                 #954.48
        jbe       ..B10.10      # Prob 50%                      #954.48
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm1 xmm12
..B10.4:                        # Preds ..B10.3
                                # Execution count [2.10e-01]
        comisd    4128+_vmldACosHATab(%rip), %xmm12             #956.52
        jbe       ..B10.9       # Prob 50%                      #956.52
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm12
..B10.5:                        # Preds ..B10.4
                                # Execution count [1.05e-01]
        movsd     4104+_vmldACosHATab(%rip), %xmm6              #958.68
        movaps    %xmm12, %xmm8                                 #958.174
        mulsd     %xmm12, %xmm6                                 #958.68
        movaps    %xmm12, %xmm7                                 #960.44
        movsd     %xmm6, -40(%rsp)                              #958.25
        movsd     -40(%rsp), %xmm13                             #958.107
        movsd     4104+_vmldACosHATab(%rip), %xmm5              #965.68
        subsd     -32(%rsp), %xmm13                             #958.112
        movsd     %xmm13, -48(%rsp)                             #958.93
        movsd     -40(%rsp), %xmm15                             #958.138
        movsd     -48(%rsp), %xmm14                             #958.143
        subsd     %xmm14, %xmm15                                #958.143
        movaps    %xmm12, %xmm14                                #967.48
        movsd     %xmm15, -40(%rsp)                             #958.124
        movsd     -40(%rsp), %xmm6                              #958.174
        subsd     %xmm6, %xmm8                                  #958.174
        movsd     %xmm8, -48(%rsp)                              #958.149
        movsd     -40(%rsp), %xmm9                              #958.188
        movaps    %xmm9, %xmm4                                  #959.40
        addsd     %xmm9, %xmm7                                  #960.44
        mulsd     %xmm9, %xmm4                                  #959.40
        movsd     -48(%rsp), %xmm10                             #958.200
        movaps    %xmm4, %xmm11                                 #961.39
        mulsd     %xmm10, %xmm7                                 #960.53
        mulsd     %xmm4, %xmm5                                  #965.68
        addsd     %xmm7, %xmm11                                 #961.39
        movsd     4312+_vmldACosHATab(%rip), %xmm8              #962.81
        mulsd     %xmm11, %xmm8                                 #962.81
        movsd     %xmm5, -40(%rsp)                              #965.25
        movsd     -40(%rsp), %xmm1                              #965.107
        movsd     4104+_vmldACosHATab(%rip), %xmm5              #968.67
        subsd     %xmm4, %xmm1                                  #965.112
        addsd     4304+_vmldACosHATab(%rip), %xmm8              #962.103
        mulsd     %xmm11, %xmm8                                 #962.125
        movsd     %xmm1, -48(%rsp)                              #965.93
        movsd     -40(%rsp), %xmm0                              #965.138
        movsd     -48(%rsp), %xmm2                              #965.143
        addsd     4296+_vmldACosHATab(%rip), %xmm8              #962.147
        subsd     %xmm2, %xmm0                                  #965.143
        mulsd     %xmm11, %xmm8                                 #962.169
        movsd     %xmm0, -40(%rsp)                              #965.124
        movsd     -40(%rsp), %xmm3                              #965.174
        addsd     4288+_vmldACosHATab(%rip), %xmm8              #962.191
        subsd     %xmm3, %xmm4                                  #965.174
        mulsd     %xmm11, %xmm8                                 #962.213
        movsd     %xmm4, -48(%rsp)                              #965.149
        movsd     -40(%rsp), %xmm6                              #965.188
        mulsd     %xmm6, %xmm9                                  #966.39
        addsd     4280+_vmldACosHATab(%rip), %xmm8              #963.49
        mulsd     %xmm6, %xmm10                                 #967.63
        mulsd     %xmm11, %xmm8                                 #963.71
        mulsd     %xmm9, %xmm5                                  #968.67
        addsd     4272+_vmldACosHATab(%rip), %xmm8              #963.93
        mulsd     %xmm11, %xmm8                                 #963.115
        movaps    %xmm9, %xmm0                                  #968.171
        movsd     -48(%rsp), %xmm13                             #965.200
        movaps    %xmm6, %xmm4                                  #969.39
        movsd     %xmm5, -40(%rsp)                              #968.25
        addsd     %xmm13, %xmm7                                 #967.40
        addsd     4264+_vmldACosHATab(%rip), %xmm8              #963.137
        mulsd     %xmm7, %xmm14                                 #967.48
        mulsd     %xmm11, %xmm8                                 #963.159
        addsd     %xmm14, %xmm10                                #967.63
        addsd     4256+_vmldACosHATab(%rip), %xmm8              #963.181
        movsd     -40(%rsp), %xmm1                              #968.106
        mulsd     %xmm11, %xmm8                                 #963.203
        subsd     %xmm9, %xmm1                                  #968.111
        addsd     %xmm10, %xmm9                                 #970.77
        addsd     4248+_vmldACosHATab(%rip), %xmm8              #963.225
        mulsd     %xmm9, %xmm7                                  #970.77
        mulsd     %xmm11, %xmm8                                 #963.247
        movsd     %xmm1, -48(%rsp)                              #968.92
        movsd     -40(%rsp), %xmm2                              #968.136
        movsd     -48(%rsp), %xmm15                             #968.141
        subsd     %xmm15, %xmm2                                 #968.141
        addsd     4240+_vmldACosHATab(%rip), %xmm8              #964.49
        movsd     %xmm2, -40(%rsp)                              #968.122
        movsd     -40(%rsp), %xmm5                              #968.171
        mulsd     %xmm11, %xmm8                                 #964.71
        subsd     %xmm5, %xmm0                                  #968.171
        movsd     %xmm0, -48(%rsp)                              #968.147
        movsd     -40(%rsp), %xmm3                              #968.185
        movsd     -48(%rsp), %xmm5                              #968.197
        movaps    %xmm5, %xmm13                                 #970.46
        addsd     4232+_vmldACosHATab(%rip), %xmm8              #964.93
        mulsd     %xmm3, %xmm4                                  #969.39
        addsd     %xmm10, %xmm13                                #970.46
        mulsd     %xmm11, %xmm8                                 #964.115
        mulsd     %xmm13, %xmm6                                 #970.46
        addsd     4224+_vmldACosHATab(%rip), %xmm8              #964.137
        addsd     %xmm7, %xmm6                                  #970.77
        mulsd     %xmm11, %xmm8                                 #972.25
        movsd     4104+_vmldACosHATab(%rip), %xmm7              #971.67
        movaps    %xmm4, %xmm13                                 #971.171
        mulsd     %xmm4, %xmm7                                  #971.67
        addsd     4216+_vmldACosHATab(%rip), %xmm8              #974.25
        movsd     %xmm7, -40(%rsp)                              #971.25
        movsd     -40(%rsp), %xmm1                              #971.106
        movsd     4200+_vmldACosHATab(%rip), %xmm11             #973.80
        subsd     %xmm4, %xmm1                                  #971.111
        mulsd     %xmm9, %xmm11                                 #973.80
        addsd     %xmm6, %xmm4                                  #973.134
        movsd     %xmm1, -48(%rsp)                              #971.92
        movaps    %xmm12, %xmm9                                 #977.50
        movsd     -40(%rsp), %xmm0                              #971.136
        movsd     -48(%rsp), %xmm2                              #971.141
        movsd     4192+_vmldACosHATab(%rip), %xmm1              #974.59
        subsd     %xmm2, %xmm0                                  #971.141
        mulsd     %xmm1, %xmm3                                  #975.55
        mulsd     %xmm1, %xmm10                                 #974.59
        mulsd     %xmm4, %xmm8                                  #974.25
        addsd     %xmm3, %xmm9                                  #977.50
        mulsd     %xmm1, %xmm5                                  #976.59
        addsd     %xmm10, %xmm11                                #974.25
        movsd     %xmm0, -40(%rsp)                              #971.122
        addsd     %xmm11, %xmm8                                 #974.25
        movsd     -40(%rsp), %xmm7                              #971.171
        addsd     %xmm5, %xmm8                                  #976.25
        subsd     %xmm7, %xmm13                                 #971.171
        movsd     %xmm13, -48(%rsp)                             #971.147
        movsd     -40(%rsp), %xmm0                              #971.185
        movsd     -48(%rsp), %xmm2                              #971.197
        movsd     %xmm9, -40(%rsp)                              #977.25
        addsd     %xmm2, %xmm6                                  #983.25
        movsd     -40(%rsp), %xmm10                             #977.88
        movsd     %xmm3, -56(%rsp)                              #975.25
        subsd     %xmm10, %xmm12                                #977.88
        movsd     4208+_vmldACosHATab(%rip), %xmm4              #979.59
        addsd     %xmm12, %xmm3                                 #977.116
        mulsd     %xmm4, %xmm0                                  #980.55
        mulsd     %xmm4, %xmm6                                  #983.25
        movsd     %xmm3, -48(%rsp)                              #977.94
        movsd     -40(%rsp), %xmm3                              #977.133
        movaps    %xmm3, %xmm12                                 #982.48
        movsd     -48(%rsp), %xmm7                              #977.144
        addsd     %xmm0, %xmm12                                 #982.48
        addsd     %xmm7, %xmm8                                  #978.25
        movsd     %xmm12, -40(%rsp)                             #982.25
        movsd     -40(%rsp), %xmm12                             #982.84
        subsd     %xmm12, %xmm3                                 #982.84
        addsd     %xmm3, %xmm0                                  #982.112
        movsd     %xmm0, -48(%rsp)                              #982.90
        movsd     -40(%rsp), %xmm3                              #982.129
        movsd     -48(%rsp), %xmm0                              #982.140
        movsd     (%rdi), %xmm1                                 #984.31
        addsd     %xmm8, %xmm0                                  #983.25
        comisd    4184+_vmldACosHATab(%rip), %xmm1              #984.52
        addsd     %xmm0, %xmm6                                  #983.25
        jbe       ..B10.7       # Prob 50%                      #984.52
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm3 xmm6
..B10.6:                        # Preds ..B10.5
                                # Execution count [5.25e-02]
        movsd     4136+_vmldACosHATab(%rip), %xmm2              #986.61
        movaps    %xmm2, %xmm0                                  #986.61
        subsd     %xmm3, %xmm0                                  #986.61
        movsd     %xmm0, -40(%rsp)                              #986.29
        movsd     -40(%rsp), %xmm1                              #986.155
        movsd     4144+_vmldACosHATab(%rip), %xmm0              #987.56
        subsd     %xmm1, %xmm2                                  #986.155
        subsd     %xmm6, %xmm0                                  #987.79
        subsd     %xmm3, %xmm2                                  #986.175
        movsd     %xmm2, -48(%rsp)                              #986.161
        movsd     -40(%rsp), %xmm1                              #986.204
        movsd     -48(%rsp), %xmm3                              #986.215
        addsd     %xmm3, %xmm0                                  #988.29
        jmp       ..B10.8       # Prob 100%                     #988.29
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm0 xmm1
..B10.7:                        # Preds ..B10.5
                                # Execution count [5.25e-02]
        movsd     4136+_vmldACosHATab(%rip), %xmm2              #992.61
        movaps    %xmm3, %xmm0                                  #992.85
        addsd     %xmm2, %xmm0                                  #992.85
        movsd     %xmm0, -40(%rsp)                              #992.29
        movsd     -40(%rsp), %xmm1                              #992.154
        subsd     %xmm1, %xmm2                                  #992.154
        addsd     %xmm2, %xmm3                                  #992.182
        movsd     %xmm3, -48(%rsp)                              #992.160
        movsd     4144+_vmldACosHATab(%rip), %xmm3              #993.56
        movsd     -40(%rsp), %xmm1                              #992.202
        addsd     %xmm3, %xmm6                                  #993.79
        movsd     -48(%rsp), %xmm0                              #992.213
        addsd     %xmm6, %xmm0                                  #994.29
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm0 xmm1
..B10.8:                        # Preds ..B10.6 ..B10.7
                                # Execution count [1.05e-01]
        addsd     %xmm0, %xmm1                                  #996.43
        movsd     %xmm1, (%rsi)                                 #997.26
        ret                                                     #997.26
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.9:                        # Preds ..B10.4
                                # Execution count [1.05e-01]
        movsd     4144+_vmldACosHATab(%rip), %xmm1              #1004.52
        addsd     %xmm0, %xmm12                                 #1001.70
        movsd     %xmm12, -40(%rsp)                             #1001.25
        movsd     -40(%rsp), %xmm0                              #1002.41
        mulsd     -32(%rsp), %xmm0                              #1002.41
        movsd     %xmm0, -56(%rsp)                              #1002.25
        movb      -49(%rsp), %dl                                #1003.46
        movb      7(%rdi), %dil                                 #1003.85
        andb      $127, %dl                                     #1003.46
        andb      $-128, %dil                                   #1003.46
        orb       %dil, %dl                                     #1003.46
        movb      %dl, -49(%rsp)                                #1003.46
        subsd     -56(%rsp), %xmm1                              #1004.75
        addsd     4136+_vmldACosHATab(%rip), %xmm1              #1005.73
        movsd     %xmm1, (%rsi)                                 #1006.26
        ret                                                     #1006.26
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.10:                       # Preds ..B10.3
                                # Execution count [2.10e-01]
        movaps    %xmm0, %xmm8                                  #1011.66
        movzwl    4174+_vmldACosHATab(%rip), %r9d               #1015.42
        pxor      %xmm13, %xmm13                                #1038.56
        andl      $-32753, %r9d                                 #1015.42
        subsd     %xmm12, %xmm8                                 #1011.66
        subsd     %xmm0, %xmm13                                 #1038.56
        mulsd     %xmm8, %xmm1                                  #1012.45
        movsd     %xmm1, -56(%rsp)                              #1012.21
        movzwl    -50(%rsp), %ecx                               #1013.48
        andl      $32752, %ecx                                  #1013.48
        shrl      $4, %ecx                                      #1013.48
        addl      $-1023, %ecx                                  #1013.70
        movl      %ecx, %r8d                                    #1015.81
        movl      %ecx, %edx                                    #1017.32
        negl      %r8d                                          #1015.81
        addl      $1023, %r8d                                   #1015.81
        andl      $2047, %r8d                                   #1015.42
        shll      $4, %r8d                                      #1015.42
        movsd     %xmm0, -32(%rsp)                              #1014.21
        orl       %r8d, %r9d                                    #1015.42
        movw      %r9w, -26(%rsp)                               #1015.42
        andl      $1, %edx                                      #1017.32
        movsd     -32(%rsp), %xmm4                              #1016.33
        lea       _vmldACosHATab(%rip), %r8                     #1035.45
        mulsd     %xmm4, %xmm1                                  #1016.33
        movl      %edx, %r10d                                   #1034.40
        movaps    %xmm1, %xmm15                                 #1021.39
        movsd     4112+_vmldACosHATab(%rip), %xmm6              #1027.53
        addsd     %xmm1, %xmm15                                 #1021.39
        jne       ..L159        # Prob 50%                      #1025.25
        movaps    %xmm1, %xmm15                                 #1025.25
..L159:                                                         #
        mulsd     %xmm15, %xmm6                                 #1027.53
        movaps    %xmm15, %xmm7                                 #1030.36
        movaps    %xmm6, %xmm9                                  #1028.37
        subl      %edx, %ecx                                    #1018.27
        movsd     4120+_vmldACosHATab(%rip), %xmm11             #1032.54
        subsd     %xmm15, %xmm9                                 #1028.37
        addsd     %xmm1, %xmm11                                 #1032.54
        movsd     %xmm9, -48(%rsp)                              #1028.21
        movsd     -48(%rsp), %xmm10                             #1029.37
        movsd     %xmm11, -24(%rsp)                             #1032.21
        subsd     %xmm10, %xmm6                                 #1029.37
        movl      -24(%rsp), %r11d                              #1033.48
        movaps    %xmm6, %xmm14                                 #1040.36
        shll      $8, %r10d                                     #1034.40
        andl      $511, %r11d                                   #1033.80
        addl      %r10d, %r11d                                  #1034.40
        subsd     %xmm6, %xmm7                                  #1030.36
        movsd     (%r8,%r11,8), %xmm5                           #1035.45
        addsd     %xmm7, %xmm14                                 #1040.36
        mulsd     %xmm5, %xmm6                                  #1045.40
        movaps    %xmm5, %xmm12                                 #1036.35
        mulsd     %xmm5, %xmm12                                 #1036.35
        mulsd     %xmm12, %xmm14                                #1040.36
        movsd     4512+_vmldACosHATab(%rip), %xmm4              #1041.71
        addsd     %xmm13, %xmm14                                #1040.36
        mulsd     %xmm14, %xmm4                                 #1041.71
        shrl      $1, %ecx                                      #1018.38
        addsd     4504+_vmldACosHATab(%rip), %xmm4              #1041.94
        mulsd     %xmm14, %xmm4                                 #1041.116
        addl      $1023, %ecx                                   #1044.73
        andl      $2047, %ecx                                   #1044.42
        addsd     4496+_vmldACosHATab(%rip), %xmm4              #1041.139
        mulsd     %xmm14, %xmm4                                 #1041.161
        movzwl    4174+_vmldACosHATab(%rip), %r9d               #1044.42
        shll      $4, %ecx                                      #1044.42
        andl      $-32753, %r9d                                 #1044.42
        movsd     %xmm0, -16(%rsp)                              #1043.21
        orl       %ecx, %r9d                                    #1044.42
        movw      %r9w, -10(%rsp)                               #1044.42
        movsd     -16(%rsp), %xmm9                              #1046.41
        mulsd     %xmm9, %xmm6                                  #1046.41
        addsd     4488+_vmldACosHATab(%rip), %xmm4              #1041.184
        mulsd     %xmm14, %xmm4                                 #1041.206
        movsd     4104+_vmldACosHATab(%rip), %xmm3              #1054.66
        mulsd     %xmm6, %xmm3                                  #1054.66
        addsd     4480+_vmldACosHATab(%rip), %xmm4              #1042.57
        mulsd     %xmm14, %xmm4                                 #1042.79
        movsd     %xmm3, -40(%rsp)                              #1054.21
        movsd     -40(%rsp), %xmm1                              #1054.105
        movsd     4352+_vmldACosHATab(%rip), %xmm11             #1060.256
        subsd     %xmm6, %xmm1                                  #1054.110
        addsd     4472+_vmldACosHATab(%rip), %xmm4              #1042.102
        addsd     4360+_vmldACosHATab(%rip), %xmm11             #1060.256
        mulsd     %xmm14, %xmm4                                 #1042.124
        mulsd     %xmm8, %xmm11                                 #1060.278
        addsd     4464+_vmldACosHATab(%rip), %xmm4              #1042.147
        mulsd     %xmm14, %xmm4                                 #1047.39
        mulsd     %xmm15, %xmm4                                 #1050.41
        movsd     %xmm1, -48(%rsp)                              #1054.91
        addsd     %xmm4, %xmm7                                  #1052.41
        mulsd     %xmm5, %xmm7                                  #1052.41
        movsd     4456+_vmldACosHATab(%rip), %xmm5              #1057.77
        mulsd     %xmm8, %xmm5                                  #1057.77
        mulsd     %xmm9, %xmm7                                  #1053.41
        addsd     4448+_vmldACosHATab(%rip), %xmm5              #1057.98
        mulsd     %xmm8, %xmm5                                  #1057.120
        movsd     -40(%rsp), %xmm2                              #1054.138
        movsd     -48(%rsp), %xmm0                              #1054.143
        movsd     4104+_vmldACosHATab(%rip), %xmm4              #1062.59
        subsd     %xmm0, %xmm2                                  #1054.143
        mulsd     %xmm8, %xmm4                                  #1062.59
        addsd     4440+_vmldACosHATab(%rip), %xmm5              #1057.141
        mulsd     %xmm8, %xmm5                                  #1057.163
        movaps    %xmm8, %xmm0                                  #1062.155
        movsd     %xmm2, -40(%rsp)                              #1054.124
        movsd     -40(%rsp), %xmm14                             #1054.176
        movsd     4104+_vmldACosHATab(%rip), %xmm2              #1065.63
        subsd     %xmm14, %xmm6                                 #1054.176
        addsd     4432+_vmldACosHATab(%rip), %xmm5              #1057.184
        mulsd     %xmm8, %xmm5                                  #1057.206
        movsd     %xmm6, -48(%rsp)                              #1054.149
        movsd     -40(%rsp), %xmm6                              #1054.190
        movsd     -48(%rsp), %xmm10                             #1054.202
        movsd     %xmm4, -40(%rsp)                              #1062.21
        addsd     %xmm10, %xmm7                                 #1056.21
        addsd     4424+_vmldACosHATab(%rip), %xmm5              #1058.45
        mulsd     %xmm8, %xmm5                                  #1058.67
        movsd     -40(%rsp), %xmm3                              #1062.98
        movsd     4336+_vmldACosHATab(%rip), %xmm12             #1060.176
        subsd     %xmm8, %xmm3                                  #1062.103
        addsd     4416+_vmldACosHATab(%rip), %xmm5              #1058.88
        addsd     4344+_vmldACosHATab(%rip), %xmm12             #1060.176
        mulsd     %xmm8, %xmm5                                  #1058.110
        addsd     %xmm11, %xmm12                                #1060.278
        addsd     4408+_vmldACosHATab(%rip), %xmm5              #1058.131
        mulsd     %xmm8, %xmm12                                 #1060.281
        mulsd     %xmm8, %xmm5                                  #1058.153
        movsd     %xmm3, -48(%rsp)                              #1062.84
        movsd     -40(%rsp), %xmm1                              #1062.124
        movsd     -48(%rsp), %xmm15                             #1062.129
        movsd     4320+_vmldACosHATab(%rip), %xmm13             #1060.96
        subsd     %xmm15, %xmm1                                 #1062.129
        addsd     4400+_vmldACosHATab(%rip), %xmm5              #1058.174
        addsd     4328+_vmldACosHATab(%rip), %xmm13             #1060.96
        mulsd     %xmm8, %xmm5                                  #1058.196
        addsd     %xmm12, %xmm13                                #1060.281
        addsd     4392+_vmldACosHATab(%rip), %xmm5              #1058.217
        movsd     %xmm1, -40(%rsp)                              #1062.110
        mulsd     %xmm8, %xmm5                                  #1058.239
        movsd     -40(%rsp), %xmm4                              #1062.155
        subsd     %xmm4, %xmm0                                  #1062.155
        addsd     4384+_vmldACosHATab(%rip), %xmm5              #1058.260
        movsd     %xmm0, -48(%rsp)                              #1062.135
        movsd     -40(%rsp), %xmm4                              #1062.169
        movaps    %xmm4, %xmm14                                 #1063.35
        mulsd     %xmm4, %xmm14                                 #1063.35
        mulsd     %xmm8, %xmm5                                  #1058.282
        mulsd     %xmm14, %xmm2                                 #1065.63
        addsd     4376+_vmldACosHATab(%rip), %xmm5              #1059.45
        movsd     -48(%rsp), %xmm3                              #1062.181
        movsd     %xmm2, -40(%rsp)                              #1065.21
        movsd     -40(%rsp), %xmm9                              #1065.102
        mulsd     %xmm8, %xmm5                                  #1059.67
        subsd     %xmm14, %xmm9                                 #1065.107
        movsd     %xmm9, -48(%rsp)                              #1065.88
        movsd     -40(%rsp), %xmm11                             #1065.132
        movsd     -48(%rsp), %xmm10                             #1065.137
        movsd     4336+_vmldACosHATab(%rip), %xmm0              #1066.68
        subsd     %xmm10, %xmm11                                #1065.137
        mulsd     %xmm4, %xmm0                                  #1066.68
        addsd     4368+_vmldACosHATab(%rip), %xmm5              #1059.88
        addsd     %xmm8, %xmm4                                  #1064.45
        mulsd     %xmm8, %xmm5                                  #1059.110
        mulsd     %xmm3, %xmm4                                  #1064.45
        mulsd     %xmm8, %xmm5                                  #1059.112
        movsd     %xmm11, -40(%rsp)                             #1065.118
        movaps    %xmm0, %xmm1                                  #1068.45
        movsd     -40(%rsp), %xmm12                             #1065.167
        mulsd     %xmm8, %xmm5                                  #1059.114
        subsd     %xmm12, %xmm14                                #1065.167
        movsd     %xmm14, -48(%rsp)                             #1065.143
        addsd     %xmm5, %xmm13                                 #1061.25
        movsd     -40(%rsp), %xmm9                              #1065.181
        mulsd     4352+_vmldACosHATab(%rip), %xmm9              #1067.68
        mulsd     %xmm13, %xmm7                                 #1061.25
        addsd     %xmm9, %xmm1                                  #1068.45
        movsd     -48(%rsp), %xmm2                              #1065.193
        movsd     %xmm1, -40(%rsp)                              #1068.21
        addsd     %xmm2, %xmm4                                  #1071.155
        movsd     -40(%rsp), %xmm13                             #1068.85
        movsd     %xmm9, -24(%rsp)                              #1067.21
        subsd     %xmm13, %xmm0                                 #1068.85
        mulsd     4352+_vmldACosHATab(%rip), %xmm4              #1071.155
        addsd     %xmm0, %xmm9                                  #1068.114
        movsd     %xmm9, -48(%rsp)                              #1068.91
        movsd     -40(%rsp), %xmm12                             #1068.133
        movsd     4320+_vmldACosHATab(%rip), %xmm10             #1069.77
        movsd     -48(%rsp), %xmm1                              #1068.144
        addsd     %xmm12, %xmm10                                #1069.77
        movsd     %xmm10, -40(%rsp)                             #1069.21
        movsd     -40(%rsp), %xmm15                             #1069.146
        movsd     4320+_vmldACosHATab(%rip), %xmm11             #1069.146
        movsd     4104+_vmldACosHATab(%rip), %xmm9              #1070.62
        subsd     %xmm15, %xmm11                                #1069.146
        movsd     4336+_vmldACosHATab(%rip), %xmm2              #1071.102
        addsd     %xmm11, %xmm12                                #1069.174
        mulsd     %xmm3, %xmm2                                  #1071.102
        movsd     %xmm12, -48(%rsp)                             #1069.152
        movsd     -40(%rsp), %xmm15                             #1069.191
        mulsd     %xmm15, %xmm9                                 #1070.62
        movsd     -48(%rsp), %xmm0                              #1069.202
        movsd     %xmm9, -40(%rsp)                              #1070.21
        movsd     -40(%rsp), %xmm10                             #1070.101
        movsd     4360+_vmldACosHATab(%rip), %xmm3              #1072.140
        subsd     %xmm15, %xmm10                                #1070.106
        mulsd     %xmm8, %xmm3                                  #1072.161
        movsd     %xmm10, -48(%rsp)                             #1070.87
        movsd     -40(%rsp), %xmm11                             #1070.130
        movsd     -48(%rsp), %xmm13                             #1070.135
        subsd     %xmm13, %xmm11                                #1070.135
        addsd     4344+_vmldACosHATab(%rip), %xmm3              #1072.161
        movsd     %xmm11, -40(%rsp)                             #1070.116
        movsd     -40(%rsp), %xmm14                             #1070.164
        mulsd     %xmm8, %xmm3                                  #1072.164
        subsd     %xmm14, %xmm15                                #1070.164
        movsd     %xmm15, -48(%rsp)                             #1070.141
        movsd     -40(%rsp), %xmm10                             #1070.178
        movsd     -48(%rsp), %xmm9                              #1070.190
        addsd     %xmm9, %xmm4                                  #1071.50
        addsd     4328+_vmldACosHATab(%rip), %xmm3              #1072.164
        addsd     %xmm2, %xmm4                                  #1071.57
        addsd     %xmm5, %xmm3                                  #1072.168
        addsd     %xmm1, %xmm4                                  #1071.102
        addsd     %xmm0, %xmm4                                  #1071.155
        addsd     %xmm3, %xmm4                                  #1072.21
        mulsd     %xmm6, %xmm4                                  #1072.21
        mulsd     %xmm10, %xmm6                                 #1073.39
        addsd     %xmm7, %xmm4                                  #1072.21
        movsd     (%rdi), %xmm7                                 #1074.27
        comisd    4184+_vmldACosHATab(%rip), %xmm7              #1074.48
        ja        ..B10.13      # Prob 50%                      #1074.48
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm4 xmm6
..B10.12:                       # Preds ..B10.10
                                # Execution count [1.05e-01]
        movsd     4152+_vmldACosHATab(%rip), %xmm2              #1081.57
        movaps    %xmm2, %xmm0                                  #1081.57
        movsd     4160+_vmldACosHATab(%rip), %xmm5              #1082.52
        subsd     %xmm6, %xmm0                                  #1081.57
        subsd     %xmm4, %xmm5                                  #1082.75
        movsd     %xmm0, -40(%rsp)                              #1081.25
        movsd     -40(%rsp), %xmm1                              #1081.151
        movsd     %xmm6, -56(%rsp)                              #1073.21
        subsd     %xmm1, %xmm2                                  #1081.151
        subsd     %xmm6, %xmm2                                  #1081.171
        movsd     %xmm2, -48(%rsp)                              #1081.157
        movsd     -40(%rsp), %xmm6                              #1081.200
        movsd     -48(%rsp), %xmm3                              #1081.211
        movaps    %xmm3, %xmm4                                  #1083.25
        addsd     %xmm5, %xmm4                                  #1083.25
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm4 xmm6
..B10.13:                       # Preds ..B10.10 ..B10.12
                                # Execution count [2.10e-01]
        addsd     %xmm4, %xmm6                                  #1085.39
        movsd     %xmm6, (%rsi)                                 #1086.22
        ret                                                     #1086.22
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.14:                       # Preds ..B10.2
                                # Execution count [4.20e-01]
        ucomisd   %xmm0, %xmm1                                  #1091.45
        jp        ..B10.15      # Prob 0%                       #1091.45
        je        ..B10.19      # Prob 16%                      #1091.45
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm0 xmm1
..B10.15:                       # Preds ..B10.14
                                # Execution count [3.53e-01]
        xorps     .L_2il0floatpacket.202(%rip), %xmm0           #1095.51
        ucomisd   %xmm0, %xmm1                                  #1095.51
        jp        ..B10.16      # Prob 0%                       #1095.51
        je        ..B10.18      # Prob 16%                      #1095.51
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax
..B10.16:                       # Preds ..B10.15
                                # Execution count [2.96e-01]
        movl      $1, %eax                                      #1102.21
        pxor      %xmm1, %xmm1                                  #1101.45
        pxor      %xmm0, %xmm0                                  #1101.45
        divsd     %xmm0, %xmm1                                  #1101.45
        movsd     %xmm1, (%rsi)                                 #1101.22
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.17:                       # Preds ..B10.16 ..B10.20
                                # Execution count [1.00e+00]
        ret                                                     #1114.12
                                # LOE
..B10.18:                       # Preds ..B10.15
                                # Execution count [5.64e-02]: Infreq
        movsd     4152+_vmldACosHATab(%rip), %xmm0              #1097.43
        addsd     4160+_vmldACosHATab(%rip), %xmm0              #1097.81
        movsd     %xmm0, (%rsi)                                 #1097.22
        ret                                                     #1097.22
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.19:                       # Preds ..B10.14
                                # Execution count [6.72e-02]: Infreq
        movq      4184+_vmldACosHATab(%rip), %rdx               #1093.43
        movq      %rdx, (%rsi)                                  #1093.22
        ret                                                     #1093.22
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.20:                       # Preds ..B10.1
                                # Execution count [1.60e-01]: Infreq
        divsd     %xmm1, %xmm1                                  #1108.24
        movsd     %xmm1, (%rsi)                                 #1108.14
        testl     $1048575, 4(%rdi)                             #1109.40
        jne       ..B10.17      # Prob 50%                      #1109.70
                                # LOE rbx rbp rdi r12 r13 r14 r15 eax
..B10.21:                       # Preds ..B10.20
                                # Execution count [8.00e-02]: Infreq
        cmpl      $1, (%rdi)                                    #1111.17
        setb      %al                                           #1111.17
        ret                                                     #1111.17
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_dacos_ha_cout_rare_internal,@function
	.size	__svml_dacos_ha_cout_rare_internal,.-__svml_dacos_ha_cout_rare_internal
..LN__svml_dacos_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dacos_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_dacos_ha_data_internal
	.globl __svml_dacos_ha_data_internal
__svml_dacos_ha_data_internal:
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
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
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
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
	.long	4227858432
	.long	4294967295
	.long	0
	.long	1073741824
	.long	0
	.long	1073741824
	.long	0
	.long	1073741824
	.long	0
	.long	1073741824
	.long	0
	.long	1073741824
	.long	0
	.long	1073741824
	.long	0
	.long	1073741824
	.long	0
	.long	1073741824
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	856972295
	.long	1017226790
	.long	856972295
	.long	1017226790
	.long	856972295
	.long	1017226790
	.long	856972295
	.long	1017226790
	.long	856972295
	.long	1017226790
	.long	856972295
	.long	1017226790
	.long	856972295
	.long	1017226790
	.long	856972295
	.long	1017226790
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
	.long	4294705152
	.long	0
	.long	4294705152
	.long	0
	.long	4294705152
	.long	0
	.long	4294705152
	.long	0
	.long	4294705152
	.long	0
	.long	4294705152
	.long	0
	.long	4294705152
	.long	0
	.long	4294705152
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
	.long	0
	.long	2146435072
	.long	0
	.long	4293918720
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
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
	.long	1072693248
	.long	0
	.long	3220176896
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
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
	.long	0
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
	.type	__svml_dacos_ha_data_internal,@object
	.size	__svml_dacos_ha_data_internal,2496
	.align 32
_vmldACosHATab:
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
	.long	1071644672
	.long	0
	.long	1101004800
	.long	1073741824
	.long	1095761920
	.long	256
	.long	1118830592
	.long	0
	.long	1017118720
	.long	1413754136
	.long	1073291771
	.long	856972295
	.long	1016178214
	.long	1413754136
	.long	1074340347
	.long	856972295
	.long	1017226790
	.long	0
	.long	1072693248
	.long	0
	.long	1071644672
	.long	0
	.long	0
	.long	1476395008
	.long	1069897045
	.long	1768958041
	.long	3189069141
	.long	805306368
	.long	1068708659
	.long	3580333578
	.long	1040816593
	.long	3067382784
	.long	1067899757
	.long	3397590151
	.long	1067392113
	.long	2939529726
	.long	1066854585
	.long	1423429166
	.long	1066517752
	.long	1775218934
	.long	1066178574
	.long	1185392460
	.long	1065859647
	.long	289998670
	.long	1065577550
	.long	3179807072
	.long	1065648121
	.long	3781007284
	.long	1061576176
	.long	2482106687
	.long	1067019199
	.long	763519713
	.long	3214591591
	.long	3695107454
	.long	1067530646
	.long	0
	.long	1073741824
	.long	1124791109
	.long	1006764147
	.long	1476395008
	.long	1069897045
	.long	1953913876
	.long	3189069141
	.long	805306368
	.long	1067660083
	.long	165110192
	.long	1039768033
	.long	3067304082
	.long	1065802605
	.long	3404727379
	.long	1064246385
	.long	2737480376
	.long	1062660281
	.long	933797922
	.long	1061274873
	.long	1475716730
	.long	1059887095
	.long	1511619763
	.long	1058519827
	.long	556024211
	.long	1057187555
	.long	3482101045
	.long	1056217350
	.long	1174622859
	.long	1050762633
	.long	899668651
	.long	1055506366
	.long	1081094694
	.long	3202035365
	.long	2559814773
	.long	1053906576
	.long	0
	.long	3219128320
	.long	0
	.long	1071120384
	.long	0
	.long	3218341888
	.long	0
	.long	1070694400
	.long	0
	.long	3218046976
	.long	0
	.long	1070391296
	.long	0
	.long	3217739776
	.type	_vmldACosHATab,@object
	.size	_vmldACosHATab,4520
	.space 88, 0x00 	# pad
	.align 16
.L_2il0floatpacket.202:
	.long	0x00000000,0x80000000,0x00000000,0x00000000
	.type	.L_2il0floatpacket.202,@object
	.size	.L_2il0floatpacket.202,16
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
