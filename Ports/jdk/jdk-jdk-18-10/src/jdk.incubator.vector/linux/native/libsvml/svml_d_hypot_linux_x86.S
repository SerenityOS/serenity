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
	.file "svml_d_hypot.c"
	.text
..TXTST0:
.L_2__routine_start___svml_hypot2_ha_e9_0:
# -- Begin  __svml_hypot2_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypot2_ha_e9
# --- __svml_hypot2_ha_e9(__m128d, __m128d)
__svml_hypot2_ha_e9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #119.1
        .byte     15                                            #221.663
        .byte     30                                            #221.663
        .byte     250                                           #221.663
	.cfi_startproc
..___tag_value___svml_hypot2_ha_e9.1:
..L2:
                                                          #119.1
        pushq     %rbp                                          #119.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #119.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #119.1
        subq      $256, %rsp                                    #119.1
        movaps    %xmm1, %xmm3                                  #119.1
        movups    64+__svml_dhypot_ha_data_internal(%rip), %xmm4 #152.47
        movaps    %xmm4, %xmm5                                  #159.18
        andps     %xmm3, %xmm4                                  #160.18
        andps     %xmm0, %xmm5                                  #159.18
        movaps    %xmm5, %xmm12                                 #161.11
        maxpd     %xmm4, %xmm12                                 #161.11
        minpd     %xmm5, %xmm4                                  #162.11
        pshufd    $221, %xmm12, %xmm8                           #176.20
        movaps    %xmm12, %xmm2                                 #163.150
        pshufd    $221, %xmm4, %xmm6                            #177.20
        movaps    %xmm4, %xmm13                                 #184.150
        movq      512+__svml_dhypot_ha_data_internal(%rip), %xmm7 #173.33
        psubd     %xmm6, %xmm8                                  #178.24
        pcmpgtd   %xmm7, %xmm8                                  #179.24
        movups    __svml_dhypot_ha_data_internal(%rip), %xmm9   #151.48
        pshufd    $80, %xmm8, %xmm14                            #180.24
        movaps    %xmm9, %xmm11                                 #163.105
        andnps    %xmm9, %xmm14                                 #182.21
        andps     %xmm12, %xmm11                                #163.105
        andps     %xmm4, %xmm14                                 #184.105
        movaps    %xmm11, %xmm1                                 #185.13
        subpd     %xmm11, %xmm2                                 #163.150
        subpd     %xmm14, %xmm13                                #184.150
        addpd     %xmm11, %xmm12                                #187.13
        mulpd     %xmm11, %xmm1                                 #185.13
        mulpd     %xmm12, %xmm2                                 #188.13
        mulpd     %xmm13, %xmm4                                 #189.25
        movaps    %xmm14, %xmm10                                #186.25
        mulpd     %xmm14, %xmm10                                #186.25
        mulpd     %xmm13, %xmm14                                #190.25
        addpd     %xmm4, %xmm2                                  #189.13
        addpd     %xmm10, %xmm1                                 #186.13
        addpd     %xmm14, %xmm2                                 #190.13
        movaps    %xmm1, %xmm13                                 #191.11
        movaps    %xmm1, %xmm11                                 #209.23
        addpd     %xmm2, %xmm13                                 #191.11
        cvtpd2ps  %xmm13, %xmm7                                 #207.51
        movlhps   %xmm7, %xmm7                                  #207.35
        rsqrtps   %xmm7, %xmm8                                  #207.22
        cvtps2pd  %xmm8, %xmm12                                 #207.9
        movaps    %xmm12, %xmm9                                 #208.10
        mulpd     %xmm12, %xmm9                                 #208.10
        mulpd     %xmm9, %xmm11                                 #209.23
        mulpd     %xmm2, %xmm9                                  #210.23
        subpd     128+__svml_dhypot_ha_data_internal(%rip), %xmm11 #209.11
        mulpd     %xmm12, %xmm2                                 #218.25
        addpd     %xmm9, %xmm11                                 #210.11
        movups    192+__svml_dhypot_ha_data_internal(%rip), %xmm10 #211.23
        mulpd     %xmm11, %xmm10                                #211.23
        addpd     256+__svml_dhypot_ha_data_internal(%rip), %xmm10 #211.11
        mulpd     %xmm11, %xmm10                                #212.23
        addpd     320+__svml_dhypot_ha_data_internal(%rip), %xmm10 #212.11
        mulpd     %xmm11, %xmm10                                #213.23
        addpd     384+__svml_dhypot_ha_data_internal(%rip), %xmm10 #213.11
        mulpd     %xmm11, %xmm10                                #214.23
        addpd     448+__svml_dhypot_ha_data_internal(%rip), %xmm10 #214.11
        mulpd     %xmm10, %xmm11                                #215.9
        mulpd     %xmm12, %xmm11                                #216.9
        mulpd     %xmm1, %xmm12                                 #219.24
        pshufd    $221, %xmm13, %xmm4                           #200.18
        mulpd     %xmm11, %xmm13                                #217.9
        movq      640+__svml_dhypot_ha_data_internal(%rip), %xmm15 #198.26
        movq      576+__svml_dhypot_ha_data_internal(%rip), %xmm5 #201.25
        pcmpgtd   %xmm4, %xmm5                                  #201.25
        pcmpgtd   %xmm15, %xmm4                                 #202.26
        addpd     %xmm2, %xmm13                                 #218.13
        por       %xmm4, %xmm5                                  #203.18
        pshufd    $80, %xmm5, %xmm6                             #204.18
        movmskpd  %xmm6, %edx                                   #205.42
        addpd     %xmm12, %xmm13                                #219.12
        testl     %edx, %edx                                    #221.52
        jne       ..B1.3        # Prob 5%                       #221.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm3 xmm13
..B1.2:                         # Preds ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movaps    %xmm13, %xmm0                                 #224.12
        movq      %rbp, %rsp                                    #224.12
        popq      %rbp                                          #224.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #224.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #221.241
        movups    %xmm3, 128(%rsp)                              #221.314
        movups    %xmm13, 192(%rsp)                             #221.387
                                # LOE rbx r12 r13 r14 r15 edx
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #221.571
        movq      %r12, 8(%rsp)                                 #221.571[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #221.571
        movq      %r13, (%rsp)                                  #221.571[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #221.571
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #221.634
        jc        ..B1.10       # Prob 5%                       #221.634
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #221.587
        cmpl      $2, %r12d                                     #221.582
        jl        ..B1.7        # Prob 82%                      #221.582
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm13                             #221.805
        jmp       ..B1.2        # Prob 100%                     #221.805
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm13
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #221.663
        lea       128(%rsp,%r12,8), %rsi                        #221.663
        lea       192(%rsp,%r12,8), %rdx                        #221.663
..___tag_value___svml_hypot2_ha_e9.19:
#       __svml_dhypot_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_dhypot_ha_cout_rare_internal           #221.663
..___tag_value___svml_hypot2_ha_e9.20:
        jmp       ..B1.8        # Prob 100%                     #221.663
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_hypot2_ha_e9,@function
	.size	__svml_hypot2_ha_e9,.-__svml_hypot2_ha_e9
..LN__svml_hypot2_ha_e9.0:
	.data
# -- End  __svml_hypot2_ha_e9
	.text
.L_2__routine_start___svml_hypot4_ha_e9_1:
# -- Begin  __svml_hypot4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypot4_ha_e9
# --- __svml_hypot4_ha_e9(__m256d, __m256d)
__svml_hypot4_ha_e9:
# parameter 1: %ymm0
# parameter 2: %ymm1
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #229.1
        .byte     15                                            #331.672
        .byte     30                                            #331.672
        .byte     250                                           #331.672
	.cfi_startproc
..___tag_value___svml_hypot4_ha_e9.22:
..L23:
                                                         #229.1
        pushq     %rbp                                          #229.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #229.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #229.1
        subq      $256, %rsp                                    #229.1
        vmovupd   64+__svml_dhypot_ha_data_internal(%rip), %ymm11 #262.50
        vmovupd   __svml_dhypot_ha_data_internal(%rip), %ymm9   #261.51
        vmovapd   %ymm1, %ymm10                                 #229.1
        vandpd    %ymm11, %ymm0, %ymm13                         #269.18
        vandpd    %ymm11, %ymm10, %ymm12                        #270.18
        vmaxpd    %ymm12, %ymm13, %ymm8                         #271.11
        vminpd    %ymm13, %ymm12, %ymm5                         #272.11
        vandpd    %ymm9, %ymm8, %ymm6                           #273.105
        vsubpd    %ymm6, %ymm8, %ymm7                           #273.153
        vextractf128 $1, %ymm8, %xmm14                          #284.106
        vextractf128 $1, %ymm5, %xmm15                          #285.106
        vshufps   $221, %xmm14, %xmm8, %xmm4                    #286.38
        vshufps   $221, %xmm15, %xmm5, %xmm1                    #287.38
        vpsubd    %xmm1, %xmm4, %xmm3                           #288.24
        vpcmpgtd  512+__svml_dhypot_ha_data_internal(%rip), %xmm3, %xmm11 #289.24
        vpshufd   $80, %xmm11, %xmm2                            #290.24
        vpshufd   $250, %xmm11, %xmm12                          #290.104
        vmulpd    %ymm6, %ymm6, %ymm14                          #295.13
        vaddpd    %ymm6, %ymm8, %ymm6                           #297.13
        vmulpd    %ymm6, %ymm7, %ymm7                           #298.13
        vmovups   576+__svml_dhypot_ha_data_internal(%rip), %xmm6 #307.25
        vinsertf128 $1, %xmm12, %ymm2, %ymm13                   #291.17
        vandnpd   %ymm9, %ymm13, %ymm9                          #292.21
        vandpd    %ymm9, %ymm5, %ymm1                           #294.105
        vsubpd    %ymm1, %ymm5, %ymm3                           #294.153
        vmulpd    %ymm1, %ymm1, %ymm15                          #296.28
        vmulpd    %ymm3, %ymm5, %ymm5                           #299.28
        vmulpd    %ymm3, %ymm1, %ymm1                           #300.28
        vaddpd    %ymm15, %ymm14, %ymm4                         #296.13
        vaddpd    %ymm5, %ymm7, %ymm8                           #299.13
        vaddpd    %ymm1, %ymm8, %ymm3                           #300.13
        vaddpd    %ymm3, %ymm4, %ymm2                           #301.11
        vcvtpd2ps %ymm2, %xmm15                                 #317.38
        vextractf128 $1, %ymm2, %xmm5                           #309.104
        vrsqrtps  %xmm15, %xmm1                                 #317.25
        vcvtps2pd %xmm1, %ymm1                                  #317.9
        vshufps   $221, %xmm5, %xmm2, %xmm7                     #310.36
        vpcmpgtd  %xmm7, %xmm6, %xmm8                           #311.25
        vpcmpgtd  640+__svml_dhypot_ha_data_internal(%rip), %xmm7, %xmm9 #312.26
        vmulpd    %ymm1, %ymm1, %ymm6                           #318.10
        vpor      %xmm9, %xmm8, %xmm11                          #313.18
        vpshufd   $80, %xmm11, %xmm12                           #314.18
        vpshufd   $250, %xmm11, %xmm13                          #314.90
        vshufps   $221, %xmm13, %xmm12, %xmm14                  #315.59
        vmovmskps %xmm14, %edx                                  #315.42
        vmulpd    %ymm6, %ymm4, %ymm5                           #319.26
        vmulpd    %ymm6, %ymm3, %ymm8                           #320.26
        vmulpd    %ymm3, %ymm1, %ymm3                           #328.28
        vmulpd    %ymm4, %ymm1, %ymm4                           #329.27
        vsubpd    128+__svml_dhypot_ha_data_internal(%rip), %ymm5, %ymm7 #319.11
        vaddpd    %ymm8, %ymm7, %ymm5                           #320.11
        vmulpd    192+__svml_dhypot_ha_data_internal(%rip), %ymm5, %ymm9 #321.26
        vaddpd    256+__svml_dhypot_ha_data_internal(%rip), %ymm9, %ymm11 #321.11
        vmulpd    %ymm11, %ymm5, %ymm12                         #322.26
        vaddpd    320+__svml_dhypot_ha_data_internal(%rip), %ymm12, %ymm13 #322.11
        vmulpd    %ymm13, %ymm5, %ymm14                         #323.26
        vaddpd    384+__svml_dhypot_ha_data_internal(%rip), %ymm14, %ymm15 #323.11
        vmulpd    %ymm15, %ymm5, %ymm9                          #324.26
        vaddpd    448+__svml_dhypot_ha_data_internal(%rip), %ymm9, %ymm6 #324.11
        vmulpd    %ymm6, %ymm5, %ymm7                           #325.9
        vmulpd    %ymm7, %ymm1, %ymm8                           #326.9
        vmulpd    %ymm8, %ymm2, %ymm2                           #327.9
        vaddpd    %ymm3, %ymm2, %ymm2                           #328.13
        vaddpd    %ymm4, %ymm2, %ymm1                           #329.12
        testl     %edx, %edx                                    #331.52
        jne       ..B2.3        # Prob 5%                       #331.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm1 ymm10
..B2.2:                         # Preds ..B2.3 ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        vmovapd   %ymm1, %ymm0                                  #334.12
        movq      %rbp, %rsp                                    #334.12
        popq      %rbp                                          #334.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #334.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm0, 64(%rsp)                               #331.244
        vmovupd   %ymm10, 128(%rsp)                             #331.320
        vmovupd   %ymm1, 192(%rsp)                              #331.396
        je        ..B2.2        # Prob 95%                      #331.500
                                # LOE rbx r12 r13 r14 r15 edx ymm1
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #331.580
                                # LOE rbx r12 r13 r14 r15 eax edx
..B2.13:                        # Preds ..B2.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #331.643
        jc        ..B2.10       # Prob 5%                       #331.643
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #331.596
        cmpl      $4, %r12d                                     #331.591
        jl        ..B2.7        # Prob 82%                      #331.591
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   192(%rsp), %ymm1                              #331.817
        jmp       ..B2.2        # Prob 100%                     #331.817
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm1
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #331.672
        lea       128(%rsp,%r12,8), %rsi                        #331.672
        lea       192(%rsp,%r12,8), %rdx                        #331.672
..___tag_value___svml_hypot4_ha_e9.40:
#       __svml_dhypot_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_dhypot_ha_cout_rare_internal           #331.672
..___tag_value___svml_hypot4_ha_e9.41:
        jmp       ..B2.8        # Prob 100%                     #331.672
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_hypot4_ha_e9,@function
	.size	__svml_hypot4_ha_e9,.-__svml_hypot4_ha_e9
..LN__svml_hypot4_ha_e9.1:
	.data
# -- End  __svml_hypot4_ha_e9
	.text
.L_2__routine_start___svml_hypot1_ha_l9_2:
# -- Begin  __svml_hypot1_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypot1_ha_l9
# --- __svml_hypot1_ha_l9(__m128d, __m128d)
__svml_hypot1_ha_l9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #339.1
        .byte     15                                            #441.663
        .byte     30                                            #441.663
        .byte     250                                           #441.663
	.cfi_startproc
..___tag_value___svml_hypot1_ha_l9.43:
..L44:
                                                         #339.1
        pushq     %rbp                                          #339.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #339.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #339.1
        subq      $192, %rsp                                    #339.1
        vmovapd   %xmm1, %xmm9                                  #339.1
        vmovsd    64+__svml_dhypot_ha_data_internal(%rip), %xmm10 #372.16
        vmovapd   %xmm0, %xmm8                                  #339.1
        vandpd    %xmm10, %xmm8, %xmm11                         #379.18
        vandpd    %xmm10, %xmm9, %xmm12                         #380.18
        vunpcklpd %xmm11, %xmm11, %xmm14                        #381.23
        movl      $6291456, %eax                                #399.20
        vunpcklpd %xmm12, %xmm12, %xmm13                        #381.88
        movl      $1002438656, %edx                             #421.21
        vmaxpd    %xmm13, %xmm14, %xmm6                         #381.11
        vminpd    %xmm14, %xmm13, %xmm5                         #382.11
        vpshufd   $85, %xmm6, %xmm15                            #396.16
        vmovd     %eax, %xmm1                                   #399.20
        vpshufd   $85, %xmm5, %xmm0                             #397.16
        movl      $1141899264, %ecx                             #422.22
        vmovsd    __svml_dhypot_ha_data_internal(%rip), %xmm7   #371.17
        vpsubd    %xmm0, %xmm15, %xmm2                          #398.20
        vandpd    %xmm7, %xmm6, %xmm3                           #383.105
        vpcmpgtd  %xmm1, %xmm2, %xmm10                          #399.20
        vmulsd    %xmm3, %xmm3, %xmm12                          #405.13
        vsubsd    %xmm3, %xmm6, %xmm4                           #383.150
        vaddsd    %xmm3, %xmm6, %xmm3                           #407.13
        vpshufd   $0, %xmm10, %xmm11                            #400.24
        vmovd     %ecx, %xmm15                                  #422.22
        vandnpd   %xmm7, %xmm11, %xmm7                          #402.21
        vandpd    %xmm7, %xmm5, %xmm13                          #404.105
        vmulsd    %xmm4, %xmm3, %xmm4                           #408.13
        vsubsd    %xmm13, %xmm5, %xmm2                          #404.150
        vmovapd   %xmm2, %xmm6                                  #409.13
        vmovapd   %xmm13, %xmm0                                 #406.13
        vfmadd213sd %xmm4, %xmm5, %xmm6                         #409.13
        vmovd     %edx, %xmm5                                   #421.21
        vfmadd213sd %xmm12, %xmm13, %xmm0                       #406.13
        vfmadd213sd %xmm6, %xmm13, %xmm2                        #410.13
        vmovapd   %xmm0, %xmm13                                 #429.11
        vaddsd    %xmm2, %xmm0, %xmm1                           #411.11
        vcvtpd2ps %xmm1, %xmm7                                  #427.51
        vshufps   $0, %xmm7, %xmm7, %xmm10                      #427.35
        vrsqrtps  %xmm10, %xmm11                                #427.22
        vpshufd   $85, %xmm1, %xmm14                            #420.14
        vpcmpgtd  %xmm14, %xmm5, %xmm3                          #421.21
        vpcmpgtd  %xmm15, %xmm14, %xmm4                         #422.22
        vpor      %xmm4, %xmm3, %xmm5                           #423.14
        vmovapd   %xmm2, %xmm3                                  #430.11
        vcvtps2pd %xmm11, %xmm4                                 #427.9
        vmulsd    %xmm4, %xmm4, %xmm12                          #428.10
        vfmsub213sd 128+__svml_dhypot_ha_data_internal(%rip), %xmm12, %xmm13 #429.11
        vpshufd   $0, %xmm5, %xmm6                              #424.18
        vmovmskpd %xmm6, %eax                                   #425.42
        vfmadd213sd %xmm13, %xmm12, %xmm3                       #430.11
        vmovsd    192+__svml_dhypot_ha_data_internal(%rip), %xmm12 #431.11
        vfmadd213sd 256+__svml_dhypot_ha_data_internal(%rip), %xmm3, %xmm12 #431.11
        vfmadd213sd 320+__svml_dhypot_ha_data_internal(%rip), %xmm3, %xmm12 #432.11
        vfmadd213sd 384+__svml_dhypot_ha_data_internal(%rip), %xmm3, %xmm12 #433.11
        vfmadd213sd 448+__svml_dhypot_ha_data_internal(%rip), %xmm3, %xmm12 #434.11
        vmulsd    %xmm3, %xmm12, %xmm14                         #435.9
        vmulsd    %xmm4, %xmm14, %xmm15                         #436.9
        vmulsd    %xmm1, %xmm15, %xmm1                          #437.9
        vfmadd213sd %xmm1, %xmm4, %xmm2                         #438.13
        vfmadd213sd %xmm2, %xmm4, %xmm0                         #439.12
        andl      $1, %eax                                      #425.89
        jne       ..B3.3        # Prob 5%                       #441.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm8 xmm9
..B3.2:                         # Preds ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #444.12
        popq      %rbp                                          #444.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #444.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm8, (%rsp)                                 #441.204
        vmovsd    %xmm9, 64(%rsp)                               #441.277
        vmovsd    %xmm0, 128(%rsp)                              #441.350
        jne       ..B3.5        # Prob 5%                       #441.491
                                # LOE rbx r12 r13 r14 r15 eax
..B3.4:                         # Preds ..B3.6 ..B3.5 ..B3.3
                                # Execution count [5.00e-02]: Infreq
        vmovsd    128(%rsp), %xmm0                              #441.757
        movq      %rbp, %rsp                                    #441.757
        popq      %rbp                                          #441.757
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #441.757
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B3.5:                         # Preds ..B3.3
                                # Execution count [2.50e-03]: Infreq
        je        ..B3.4        # Prob 95%                      #441.634
                                # LOE rbx r12 r13 r14 r15
..B3.6:                         # Preds ..B3.5
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #441.663
        lea       64(%rsp), %rsi                                #441.663
        lea       128(%rsp), %rdx                               #441.663
..___tag_value___svml_hypot1_ha_l9.56:
#       __svml_dhypot_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_dhypot_ha_cout_rare_internal           #441.663
..___tag_value___svml_hypot1_ha_l9.57:
        jmp       ..B3.4        # Prob 100%                     #441.663
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_hypot1_ha_l9,@function
	.size	__svml_hypot1_ha_l9,.-__svml_hypot1_ha_l9
..LN__svml_hypot1_ha_l9.2:
	.data
# -- End  __svml_hypot1_ha_l9
	.text
.L_2__routine_start___svml_hypot2_ha_l9_3:
# -- Begin  __svml_hypot2_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypot2_ha_l9
# --- __svml_hypot2_ha_l9(__m128d, __m128d)
__svml_hypot2_ha_l9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #449.1
        .byte     15                                            #551.663
        .byte     30                                            #551.663
        .byte     250                                           #551.663
	.cfi_startproc
..___tag_value___svml_hypot2_ha_l9.59:
..L60:
                                                         #449.1
        pushq     %rbp                                          #449.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #449.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #449.1
        subq      $256, %rsp                                    #449.1
        vmovapd   %xmm1, %xmm8                                  #449.1
        vmovupd   64+__svml_dhypot_ha_data_internal(%rip), %xmm9 #482.47
        vmovapd   %xmm0, %xmm7                                  #449.1
        vandpd    %xmm9, %xmm7, %xmm11                          #489.18
        vandpd    %xmm9, %xmm8, %xmm10                          #490.18
        vmaxpd    %xmm10, %xmm11, %xmm6                         #491.11
        vminpd    %xmm11, %xmm10, %xmm5                         #492.11
        vpshufd   $221, %xmm6, %xmm12                           #506.16
        vpshufd   $221, %xmm5, %xmm13                           #507.16
        vmovupd   __svml_dhypot_ha_data_internal(%rip), %xmm1   #481.48
        vpsubd    %xmm13, %xmm12, %xmm14                        #508.20
        vmovq     512+__svml_dhypot_ha_data_internal(%rip), %xmm15 #503.29
        vandpd    %xmm1, %xmm6, %xmm3                           #493.105
        vpcmpgtd  %xmm15, %xmm14, %xmm0                         #509.20
        vsubpd    %xmm3, %xmm6, %xmm4                           #493.150
        vpshufd   $80, %xmm0, %xmm2                             #510.24
        vmulpd    %xmm3, %xmm3, %xmm0                           #515.13
        vaddpd    %xmm3, %xmm6, %xmm3                           #517.13
        vmulpd    %xmm3, %xmm4, %xmm4                           #518.13
        vandnpd   %xmm1, %xmm2, %xmm9                           #512.21
        vandpd    %xmm9, %xmm5, %xmm10                          #514.105
        vsubpd    %xmm10, %xmm5, %xmm2                          #514.150
        vfmadd231pd %xmm10, %xmm10, %xmm0                       #516.13
        vmovq     640+__svml_dhypot_ha_data_internal(%rip), %xmm11 #528.22
        vfmadd231pd %xmm2, %xmm5, %xmm4                         #519.13
        vmovq     576+__svml_dhypot_ha_data_internal(%rip), %xmm5 #527.21
        vmovupd   128+__svml_dhypot_ha_data_internal(%rip), %xmm9 #539.11
        vfmadd213pd %xmm4, %xmm10, %xmm2                        #520.13
        vmovupd   192+__svml_dhypot_ha_data_internal(%rip), %xmm10 #541.11
        vaddpd    %xmm2, %xmm0, %xmm1                           #521.11
        vcvtpd2ps %xmm1, %xmm3                                  #537.51
        vpshufd   $221, %xmm1, %xmm6                            #530.14
        vmovlhps  %xmm3, %xmm3, %xmm4                           #537.35
        vpcmpgtd  %xmm6, %xmm5, %xmm12                          #531.21
        vrsqrtps  %xmm4, %xmm5                                  #537.22
        vpcmpgtd  %xmm11, %xmm6, %xmm13                         #532.22
        vpor      %xmm13, %xmm12, %xmm14                        #533.14
        vcvtps2pd %xmm5, %xmm13                                 #537.9
        vmulpd    %xmm13, %xmm13, %xmm6                         #538.10
        vfmsub231pd %xmm0, %xmm6, %xmm9                         #539.11
        vpshufd   $80, %xmm14, %xmm15                           #534.18
        vmovmskpd %xmm15, %edx                                  #535.42
        vfmadd231pd %xmm2, %xmm6, %xmm9                         #540.11
        vfmadd213pd 256+__svml_dhypot_ha_data_internal(%rip), %xmm9, %xmm10 #541.11
        vfmadd213pd 320+__svml_dhypot_ha_data_internal(%rip), %xmm9, %xmm10 #542.11
        vfmadd213pd 384+__svml_dhypot_ha_data_internal(%rip), %xmm9, %xmm10 #543.11
        vfmadd213pd 448+__svml_dhypot_ha_data_internal(%rip), %xmm9, %xmm10 #544.11
        vmulpd    %xmm10, %xmm9, %xmm11                         #545.9
        vmulpd    %xmm11, %xmm13, %xmm12                        #546.9
        vmulpd    %xmm12, %xmm1, %xmm1                          #547.9
        vfmadd213pd %xmm1, %xmm13, %xmm2                        #548.13
        vfmadd213pd %xmm2, %xmm13, %xmm0                        #549.12
        testl     %edx, %edx                                    #551.52
        jne       ..B4.3        # Prob 5%                       #551.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7 xmm8
..B4.2:                         # Preds ..B4.3 ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #554.12
        popq      %rbp                                          #554.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #554.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm7, 64(%rsp)                               #551.241
        vmovupd   %xmm8, 128(%rsp)                              #551.314
        vmovupd   %xmm0, 192(%rsp)                              #551.387
        je        ..B4.2        # Prob 95%                      #551.491
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #551.571
        movq      %r12, 8(%rsp)                                 #551.571[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #551.571
        movq      %r13, (%rsp)                                  #551.571[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #551.571
                                # LOE rbx r12 r14 r15 r13d
..B4.7:                         # Preds ..B4.8 ..B4.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #551.634
        jc        ..B4.10       # Prob 5%                       #551.634
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #551.587
        cmpl      $2, %r12d                                     #551.582
        jl        ..B4.7        # Prob 82%                      #551.582
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm0                              #551.805
        jmp       ..B4.2        # Prob 100%                     #551.805
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #551.663
        lea       128(%rsp,%r12,8), %rsi                        #551.663
        lea       192(%rsp,%r12,8), %rdx                        #551.663
..___tag_value___svml_hypot2_ha_l9.77:
#       __svml_dhypot_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_dhypot_ha_cout_rare_internal           #551.663
..___tag_value___svml_hypot2_ha_l9.78:
        jmp       ..B4.8        # Prob 100%                     #551.663
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_hypot2_ha_l9,@function
	.size	__svml_hypot2_ha_l9,.-__svml_hypot2_ha_l9
..LN__svml_hypot2_ha_l9.3:
	.data
# -- End  __svml_hypot2_ha_l9
	.text
.L_2__routine_start___svml_hypot4_ha_l9_4:
# -- Begin  __svml_hypot4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypot4_ha_l9
# --- __svml_hypot4_ha_l9(__m256d, __m256d)
__svml_hypot4_ha_l9:
# parameter 1: %ymm0
# parameter 2: %ymm1
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #559.1
        .byte     15                                            #661.672
        .byte     30                                            #661.672
        .byte     250                                           #661.672
	.cfi_startproc
..___tag_value___svml_hypot4_ha_l9.80:
..L81:
                                                         #559.1
        pushq     %rbp                                          #559.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #559.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #559.1
        subq      $256, %rsp                                    #559.1
        vmovupd   64+__svml_dhypot_ha_data_internal(%rip), %ymm10 #592.50
        vmovupd   __svml_dhypot_ha_data_internal(%rip), %ymm6   #591.51
        vmovapd   %ymm1, %ymm9                                  #559.1
        vmovapd   %ymm0, %ymm8                                  #559.1
        vandpd    %ymm10, %ymm8, %ymm12                         #599.18
        vandpd    %ymm10, %ymm9, %ymm11                         #600.18
        vmaxpd    %ymm11, %ymm12, %ymm7                         #601.11
        vminpd    %ymm12, %ymm11, %ymm3                         #602.11
        vandpd    %ymm6, %ymm7, %ymm5                           #603.105
        vsubpd    %ymm5, %ymm7, %ymm4                           #603.153
        vextractf128 $1, %ymm7, %xmm13                          #616.124
        vextractf128 $1, %ymm3, %xmm14                          #617.124
        vshufps   $221, %xmm13, %xmm7, %xmm15                   #616.34
        vshufps   $221, %xmm14, %xmm3, %xmm0                    #617.34
        vpsubd    %xmm0, %xmm15, %xmm2                          #618.20
        vpcmpgtd  512+__svml_dhypot_ha_data_internal(%rip), %xmm2, %xmm10 #619.20
        vpshufd   $80, %xmm10, %xmm1                            #620.73
        vmulpd    %ymm5, %ymm5, %ymm0                           #625.13
        vpshufd   $250, %xmm10, %xmm11                          #620.132
        vaddpd    %ymm5, %ymm7, %ymm5                           #627.13
        vmovups   576+__svml_dhypot_ha_data_internal(%rip), %xmm7 #637.21
        vmulpd    %ymm5, %ymm4, %ymm4                           #628.13
        vinsertf128 $1, %xmm11, %ymm1, %ymm12                   #620.24
        vandnpd   %ymm6, %ymm12, %ymm6                          #622.21
        vandpd    %ymm6, %ymm3, %ymm13                          #624.105
        vsubpd    %ymm13, %ymm3, %ymm2                          #624.153
        vfmadd231pd %ymm13, %ymm13, %ymm0                       #626.13
        vfmadd231pd %ymm2, %ymm3, %ymm4                         #629.13
        vfmadd213pd %ymm4, %ymm13, %ymm2                        #630.13
        vmovupd   128+__svml_dhypot_ha_data_internal(%rip), %ymm13 #649.11
        vaddpd    %ymm2, %ymm0, %ymm1                           #631.11
        vcvtpd2ps %ymm1, %xmm10                                 #647.38
        vextractf128 $1, %ymm1, %xmm3                           #640.122
        vrsqrtps  %xmm10, %xmm11                                #647.25
        vshufps   $221, %xmm3, %xmm1, %xmm14                    #640.32
        vpcmpgtd  640+__svml_dhypot_ha_data_internal(%rip), %xmm14, %xmm3 #642.22
        vpcmpgtd  %xmm14, %xmm7, %xmm15                         #641.21
        vpor      %xmm3, %xmm15, %xmm5                          #643.14
        vcvtps2pd %xmm11, %ymm3                                 #647.9
        vpshufd   $80, %xmm5, %xmm4                             #644.67
        vpshufd   $250, %xmm5, %xmm6                            #644.122
        vmulpd    %ymm3, %ymm3, %ymm12                          #648.10
        vfmsub231pd %ymm0, %ymm12, %ymm13                       #649.11
        vfmadd231pd %ymm2, %ymm12, %ymm13                       #650.11
        vmovupd   192+__svml_dhypot_ha_data_internal(%rip), %ymm12 #651.11
        vfmadd213pd 256+__svml_dhypot_ha_data_internal(%rip), %ymm13, %ymm12 #651.11
        vfmadd213pd 320+__svml_dhypot_ha_data_internal(%rip), %ymm13, %ymm12 #652.11
        vfmadd213pd 384+__svml_dhypot_ha_data_internal(%rip), %ymm13, %ymm12 #653.11
        vfmadd213pd 448+__svml_dhypot_ha_data_internal(%rip), %ymm13, %ymm12 #654.11
        vmulpd    %ymm12, %ymm13, %ymm14                        #655.9
        vmulpd    %ymm14, %ymm3, %ymm15                         #656.9
        vmulpd    %ymm15, %ymm1, %ymm1                          #657.9
        vfmadd213pd %ymm1, %ymm3, %ymm2                         #658.13
        vfmadd213pd %ymm2, %ymm3, %ymm0                         #659.12
        vinsertf128 $1, %xmm6, %ymm4, %ymm7                     #644.18
        vmovmskpd %ymm7, %edx                                   #645.42
        testl     %edx, %edx                                    #661.52
        jne       ..B5.3        # Prob 5%                       #661.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm8 ymm9
..B5.2:                         # Preds ..B5.3 ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #664.12
        popq      %rbp                                          #664.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #664.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm8, 64(%rsp)                               #661.244
        vmovupd   %ymm9, 128(%rsp)                              #661.320
        vmovupd   %ymm0, 192(%rsp)                              #661.396
        je        ..B5.2        # Prob 95%                      #661.500
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #661.580
                                # LOE rbx r12 r13 r14 r15 eax edx
..B5.13:                        # Preds ..B5.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #661.643
        jc        ..B5.10       # Prob 5%                       #661.643
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #661.596
        cmpl      $4, %r12d                                     #661.591
        jl        ..B5.7        # Prob 82%                      #661.591
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   192(%rsp), %ymm0                              #661.817
        jmp       ..B5.2        # Prob 100%                     #661.817
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #661.672
        lea       128(%rsp,%r12,8), %rsi                        #661.672
        lea       192(%rsp,%r12,8), %rdx                        #661.672
..___tag_value___svml_hypot4_ha_l9.98:
#       __svml_dhypot_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_dhypot_ha_cout_rare_internal           #661.672
..___tag_value___svml_hypot4_ha_l9.99:
        jmp       ..B5.8        # Prob 100%                     #661.672
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_hypot4_ha_l9,@function
	.size	__svml_hypot4_ha_l9,.-__svml_hypot4_ha_l9
..LN__svml_hypot4_ha_l9.4:
	.data
# -- End  __svml_hypot4_ha_l9
	.text
.L_2__routine_start___svml_hypot2_ha_ex_5:
# -- Begin  __svml_hypot2_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypot2_ha_ex
# --- __svml_hypot2_ha_ex(__m128d, __m128d)
__svml_hypot2_ha_ex:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1310.1
        .byte     15                                            #1412.663
        .byte     30                                            #1412.663
        .byte     250                                           #1412.663
	.cfi_startproc
..___tag_value___svml_hypot2_ha_ex.101:
..L102:
                                                        #1310.1
        pushq     %rbp                                          #1310.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1310.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1310.1
        subq      $256, %rsp                                    #1310.1
        movaps    %xmm1, %xmm3                                  #1310.1
        movups    64+__svml_dhypot_ha_data_internal(%rip), %xmm4 #1343.47
        movaps    %xmm4, %xmm5                                  #1350.18
        andps     %xmm3, %xmm4                                  #1351.18
        andps     %xmm0, %xmm5                                  #1350.18
        movaps    %xmm5, %xmm12                                 #1352.11
        maxpd     %xmm4, %xmm12                                 #1352.11
        minpd     %xmm5, %xmm4                                  #1353.11
        pshufd    $221, %xmm12, %xmm8                           #1367.14
        movaps    %xmm12, %xmm2                                 #1354.150
        pshufd    $221, %xmm4, %xmm6                            #1368.14
        movaps    %xmm4, %xmm13                                 #1375.150
        movq      512+__svml_dhypot_ha_data_internal(%rip), %xmm7 #1364.27
        psubd     %xmm6, %xmm8                                  #1369.18
        pcmpgtd   %xmm7, %xmm8                                  #1370.18
        movups    __svml_dhypot_ha_data_internal(%rip), %xmm9   #1342.48
        pshufd    $80, %xmm8, %xmm14                            #1371.24
        movaps    %xmm9, %xmm11                                 #1354.105
        andnps    %xmm9, %xmm14                                 #1373.21
        andps     %xmm12, %xmm11                                #1354.105
        andps     %xmm4, %xmm14                                 #1375.105
        movaps    %xmm11, %xmm1                                 #1376.13
        subpd     %xmm11, %xmm2                                 #1354.150
        subpd     %xmm14, %xmm13                                #1375.150
        addpd     %xmm11, %xmm12                                #1378.13
        mulpd     %xmm11, %xmm1                                 #1376.13
        mulpd     %xmm12, %xmm2                                 #1379.13
        mulpd     %xmm13, %xmm4                                 #1380.25
        movaps    %xmm14, %xmm10                                #1377.25
        mulpd     %xmm14, %xmm10                                #1377.25
        mulpd     %xmm13, %xmm14                                #1381.25
        addpd     %xmm4, %xmm2                                  #1380.13
        addpd     %xmm10, %xmm1                                 #1377.13
        addpd     %xmm14, %xmm2                                 #1381.13
        movaps    %xmm1, %xmm13                                 #1382.11
        movaps    %xmm1, %xmm11                                 #1400.23
        addpd     %xmm2, %xmm13                                 #1382.11
        cvtpd2ps  %xmm13, %xmm7                                 #1398.51
        movlhps   %xmm7, %xmm7                                  #1398.35
        rsqrtps   %xmm7, %xmm8                                  #1398.22
        cvtps2pd  %xmm8, %xmm12                                 #1398.9
        movaps    %xmm12, %xmm9                                 #1399.10
        mulpd     %xmm12, %xmm9                                 #1399.10
        mulpd     %xmm9, %xmm11                                 #1400.23
        mulpd     %xmm2, %xmm9                                  #1401.23
        subpd     128+__svml_dhypot_ha_data_internal(%rip), %xmm11 #1400.11
        mulpd     %xmm12, %xmm2                                 #1409.25
        addpd     %xmm9, %xmm11                                 #1401.11
        movups    192+__svml_dhypot_ha_data_internal(%rip), %xmm10 #1402.23
        mulpd     %xmm11, %xmm10                                #1402.23
        addpd     256+__svml_dhypot_ha_data_internal(%rip), %xmm10 #1402.11
        mulpd     %xmm11, %xmm10                                #1403.23
        addpd     320+__svml_dhypot_ha_data_internal(%rip), %xmm10 #1403.11
        mulpd     %xmm11, %xmm10                                #1404.23
        addpd     384+__svml_dhypot_ha_data_internal(%rip), %xmm10 #1404.11
        mulpd     %xmm11, %xmm10                                #1405.23
        addpd     448+__svml_dhypot_ha_data_internal(%rip), %xmm10 #1405.11
        mulpd     %xmm10, %xmm11                                #1406.9
        mulpd     %xmm12, %xmm11                                #1407.9
        mulpd     %xmm1, %xmm12                                 #1410.24
        pshufd    $221, %xmm13, %xmm4                           #1391.12
        mulpd     %xmm11, %xmm13                                #1408.9
        movq      640+__svml_dhypot_ha_data_internal(%rip), %xmm15 #1389.20
        movq      576+__svml_dhypot_ha_data_internal(%rip), %xmm5 #1392.19
        pcmpgtd   %xmm4, %xmm5                                  #1392.19
        pcmpgtd   %xmm15, %xmm4                                 #1393.20
        addpd     %xmm2, %xmm13                                 #1409.13
        por       %xmm4, %xmm5                                  #1394.12
        pshufd    $80, %xmm5, %xmm6                             #1395.18
        movmskpd  %xmm6, %edx                                   #1396.42
        addpd     %xmm12, %xmm13                                #1410.12
        testl     %edx, %edx                                    #1412.52
        jne       ..B6.3        # Prob 5%                       #1412.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm3 xmm13
..B6.2:                         # Preds ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movaps    %xmm13, %xmm0                                 #1415.12
        movq      %rbp, %rsp                                    #1415.12
        popq      %rbp                                          #1415.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1415.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #1412.241
        movups    %xmm3, 128(%rsp)                              #1412.314
        movups    %xmm13, 192(%rsp)                             #1412.387
                                # LOE rbx r12 r13 r14 r15 edx
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1412.571
        movq      %r12, 8(%rsp)                                 #1412.571[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1412.571
        movq      %r13, (%rsp)                                  #1412.571[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1412.571
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1412.634
        jc        ..B6.10       # Prob 5%                       #1412.634
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1412.587
        cmpl      $2, %r12d                                     #1412.582
        jl        ..B6.7        # Prob 82%                      #1412.582
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm13                             #1412.805
        jmp       ..B6.2        # Prob 100%                     #1412.805
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm13
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1412.663
        lea       128(%rsp,%r12,8), %rsi                        #1412.663
        lea       192(%rsp,%r12,8), %rdx                        #1412.663
..___tag_value___svml_hypot2_ha_ex.119:
#       __svml_dhypot_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_dhypot_ha_cout_rare_internal           #1412.663
..___tag_value___svml_hypot2_ha_ex.120:
        jmp       ..B6.8        # Prob 100%                     #1412.663
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_hypot2_ha_ex,@function
	.size	__svml_hypot2_ha_ex,.-__svml_hypot2_ha_ex
..LN__svml_hypot2_ha_ex.5:
	.data
# -- End  __svml_hypot2_ha_ex
	.text
.L_2__routine_start___svml_hypot1_ha_ex_6:
# -- Begin  __svml_hypot1_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypot1_ha_ex
# --- __svml_hypot1_ha_ex(__m128d, __m128d)
__svml_hypot1_ha_ex:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1420.1
        .byte     15                                            #1522.663
        .byte     30                                            #1522.663
        .byte     250                                           #1522.663
	.cfi_startproc
..___tag_value___svml_hypot1_ha_ex.122:
..L123:
                                                        #1420.1
        pushq     %rbp                                          #1420.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1420.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1420.1
        subq      $192, %rsp                                    #1420.1
        movaps    %xmm0, %xmm2                                  #1420.1
        movsd     64+__svml_dhypot_ha_data_internal(%rip), %xmm4 #1453.16
        movaps    %xmm1, %xmm3                                  #1420.1
        movaps    %xmm4, %xmm5                                  #1460.18
        andps     %xmm3, %xmm4                                  #1461.18
        andps     %xmm2, %xmm5                                  #1460.18
        movl      $6291456, %eax                                #1480.18
        unpcklpd  %xmm5, %xmm5                                  #1462.23
        movl      $1002438656, %edx                             #1502.19
        unpcklpd  %xmm4, %xmm4                                  #1462.88
        movaps    %xmm5, %xmm13                                 #1462.11
        maxpd     %xmm4, %xmm13                                 #1462.11
        minpd     %xmm5, %xmm4                                  #1463.11
        pshufd    $85, %xmm13, %xmm8                            #1477.14
        movd      %eax, %xmm7                                   #1480.18
        pshufd    $85, %xmm4, %xmm6                             #1478.14
        movaps    %xmm4, %xmm1                                  #1485.150
        psubd     %xmm6, %xmm8                                  #1479.18
        movaps    %xmm13, %xmm12                                #1464.150
        pcmpgtd   %xmm7, %xmm8                                  #1480.18
        movl      $1141899264, %ecx                             #1503.20
        movsd     __svml_dhypot_ha_data_internal(%rip), %xmm9   #1452.17
        movd      %edx, %xmm6                                   #1502.19
        pshufd    $0, %xmm8, %xmm14                             #1481.24
        movaps    %xmm9, %xmm11                                 #1464.105
        andnps    %xmm9, %xmm14                                 #1483.21
        andps     %xmm13, %xmm11                                #1464.105
        andps     %xmm4, %xmm14                                 #1485.105
        movaps    %xmm11, %xmm10                                #1486.13
        movaps    %xmm14, %xmm0                                 #1487.25
        subsd     %xmm14, %xmm1                                 #1485.150
        mulsd     %xmm11, %xmm10                                #1486.13
        subsd     %xmm11, %xmm12                                #1464.150
        addsd     %xmm11, %xmm13                                #1488.13
        mulsd     %xmm14, %xmm0                                 #1487.25
        mulsd     %xmm12, %xmm13                                #1489.13
        addsd     %xmm10, %xmm0                                 #1487.13
        movaps    %xmm1, %xmm15                                 #1490.25
        movaps    %xmm0, %xmm11                                 #1510.23
        mulsd     %xmm4, %xmm15                                 #1490.25
        movd      %ecx, %xmm4                                   #1503.20
        mulsd     %xmm14, %xmm1                                 #1491.25
        addsd     %xmm13, %xmm15                                #1490.13
        movaps    %xmm0, %xmm13                                 #1492.11
        addsd     %xmm15, %xmm1                                 #1491.13
        movaps    %xmm1, %xmm12                                 #1511.23
        addsd     %xmm1, %xmm13                                 #1492.11
        cvtpd2ps  %xmm13, %xmm8                                 #1508.51
        shufps    $0, %xmm8, %xmm8                              #1508.35
        rsqrtps   %xmm8, %xmm9                                  #1508.22
        cvtps2pd  %xmm9, %xmm15                                 #1508.9
        movaps    %xmm15, %xmm10                                #1509.10
        mulsd     %xmm15, %xmm10                                #1509.10
        mulsd     %xmm15, %xmm1                                 #1519.25
        mulsd     %xmm15, %xmm0                                 #1520.24
        mulsd     %xmm10, %xmm11                                #1510.23
        mulsd     %xmm10, %xmm12                                #1511.23
        subsd     128+__svml_dhypot_ha_data_internal(%rip), %xmm11 #1510.11
        movsd     192+__svml_dhypot_ha_data_internal(%rip), %xmm14 #1512.23
        addsd     %xmm11, %xmm12                                #1511.11
        mulsd     %xmm12, %xmm14                                #1512.23
        pshufd    $85, %xmm13, %xmm5                            #1501.12
        addsd     256+__svml_dhypot_ha_data_internal(%rip), %xmm14 #1512.11
        mulsd     %xmm12, %xmm14                                #1513.23
        pcmpgtd   %xmm5, %xmm6                                  #1502.19
        pcmpgtd   %xmm4, %xmm5                                  #1503.20
        addsd     320+__svml_dhypot_ha_data_internal(%rip), %xmm14 #1513.11
        mulsd     %xmm12, %xmm14                                #1514.23
        por       %xmm5, %xmm6                                  #1504.12
        pshufd    $0, %xmm6, %xmm7                              #1505.18
        addsd     384+__svml_dhypot_ha_data_internal(%rip), %xmm14 #1514.11
        movmskpd  %xmm7, %eax                                   #1506.42
        mulsd     %xmm12, %xmm14                                #1515.23
        addsd     448+__svml_dhypot_ha_data_internal(%rip), %xmm14 #1515.11
        mulsd     %xmm12, %xmm14                                #1516.9
        mulsd     %xmm15, %xmm14                                #1517.9
        mulsd     %xmm13, %xmm14                                #1518.9
        addsd     %xmm14, %xmm1                                 #1519.13
        addsd     %xmm1, %xmm0                                  #1520.12
        andl      $1, %eax                                      #1506.89
        jne       ..B7.3        # Prob 5%                       #1522.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm2 xmm3
..B7.2:                         # Preds ..B7.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1525.12
        popq      %rbp                                          #1525.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1525.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.3:                         # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm2, (%rsp)                                 #1522.204
        movsd     %xmm3, 64(%rsp)                               #1522.277
        movsd     %xmm0, 128(%rsp)                              #1522.350
        jne       ..B7.6        # Prob 5%                       #1522.491
                                # LOE rbx r12 r13 r14 r15
..B7.4:                         # Preds ..B7.6 ..B7.3
                                # Execution count [5.00e-02]: Infreq
        movsd     128(%rsp), %xmm0                              #1522.757
        movq      %rbp, %rsp                                    #1522.757
        popq      %rbp                                          #1522.757
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1522.757
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B7.6:                         # Preds ..B7.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1522.663
        lea       64(%rsp), %rsi                                #1522.663
        lea       128(%rsp), %rdx                               #1522.663
..___tag_value___svml_hypot1_ha_ex.135:
#       __svml_dhypot_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_dhypot_ha_cout_rare_internal           #1522.663
..___tag_value___svml_hypot1_ha_ex.136:
        jmp       ..B7.4        # Prob 100%                     #1522.663
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_hypot1_ha_ex,@function
	.size	__svml_hypot1_ha_ex,.-__svml_hypot1_ha_ex
..LN__svml_hypot1_ha_ex.6:
	.data
# -- End  __svml_hypot1_ha_ex
	.text
.L_2__routine_start___svml_hypot8_ha_z0_7:
# -- Begin  __svml_hypot8_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypot8_ha_z0
# --- __svml_hypot8_ha_z0(__m512d, __m512d)
__svml_hypot8_ha_z0:
# parameter 1: %zmm0
# parameter 2: %zmm1
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1530.1
        .byte     15                                            #1595.878
        .byte     30                                            #1595.878
        .byte     250                                           #1595.878
	.cfi_startproc
..___tag_value___svml_hypot8_ha_z0.138:
..L139:
                                                        #1530.1
        pushq     %rbp                                          #1530.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1530.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1530.1
        subq      $256, %rsp                                    #1530.1
        vmovaps   %zmm1, %zmm2                                  #1530.1
        vmulpd    {rn-sae}, %zmm0, %zmm0, %zmm7                 #1573.18
        vmulpd    {rn-sae}, %zmm2, %zmm2, %zmm8                 #1575.18
        vgetexppd {sae}, %zmm0, %zmm3                           #1564.18
        vgetexppd {sae}, %zmm2, %zmm4                           #1565.18
        vaddpd    {rn-sae}, %zmm8, %zmm7, %zmm1                 #1577.17
        vmaxpd    {sae}, %zmm8, %zmm7, %zmm9                    #1578.17
        vmaxpd    {sae}, %zmm4, %zmm3, %zmm5                    #1566.13
        vrsqrt14pd %zmm1, %zmm3                                 #1584.13
        vminpd    {sae}, %zmm8, %zmm7, %zmm10                   #1579.17
        vandpd    64+__svml_dhypot_ha_data_internal(%rip), %zmm5, %zmm6 #1568.14
        vsubpd    {rn-sae}, %zmm9, %zmm1, %zmm11                #1580.15
        vmulpd    {rn-sae}, %zmm3, %zmm1, %zmm4                 #1585.18
        vpcmpq    $5, 704+__svml_dhypot_ha_data_internal(%rip), %zmm6, %k0 #1571.16
        vsubpd    {rn-sae}, %zmm11, %zmm10, %zmm14              #1581.15
        kmovw     %k0, %esi                                     #1572.37
        vmovaps   %zmm0, %zmm12                                 #1574.16
        vfmsub213pd {rn-sae}, %zmm7, %zmm0, %zmm12              #1574.16
        vmovups   832+__svml_dhypot_ha_data_internal(%rip), %zmm7 #1586.48
        vmulpd    {rn-sae}, %zmm3, %zmm7, %zmm6                 #1587.23
        vfnmadd231pd {rn-sae}, %zmm4, %zmm6, %zmm7              #1588.22
        vmovaps   %zmm2, %zmm13                                 #1576.16
        vfmsub213pd {rn-sae}, %zmm8, %zmm2, %zmm13              #1576.16
        vfmadd231pd {rn-sae}, %zmm7, %zmm6, %zmm6               #1589.29
        vfmadd213pd {rn-sae}, %zmm4, %zmm4, %zmm7               #1590.24
        vaddpd    {rn-sae}, %zmm13, %zmm12, %zmm15              #1582.17
        vfnmadd231pd {rn-sae}, %zmm7, %zmm7, %zmm1              #1591.18
        vaddpd    {rn-sae}, %zmm15, %zmm14, %zmm5               #1583.17
        vaddpd    {rn-sae}, %zmm5, %zmm1, %zmm1                 #1592.24
        vfmadd213pd {rn-sae}, %zmm7, %zmm6, %zmm1               #1593.18
        testl     %esi, %esi                                    #1595.52
        jne       ..B8.3        # Prob 5%                       #1595.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm1 zmm2
..B8.2:                         # Preds ..B8.9 ..B8.7 ..B8.1
                                # Execution count [1.00e+00]
        vmovaps   %zmm1, %zmm0                                  #1598.12
        movq      %rbp, %rsp                                    #1598.12
        popq      %rbp                                          #1598.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1598.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.3:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #1595.293
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm1 zmm2
..B8.4:                         # Preds ..B8.3
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #1595.293
        movl      %edx, %eax                                    #1595.347
        orl       $8064, %eax                                   #1595.347
        cmpl      %eax, %edx                                    #1595.376
        je        ..B8.6        # Prob 78%                      #1595.376
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm1 zmm2
..B8.5:                         # Preds ..B8.4
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #1595.388
        vldmxcsr  32(%rsp)                                      #1595.388
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm1 zmm2
..B8.6:                         # Preds ..B8.5 ..B8.4
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm0, 64(%rsp)                               #1595.450
        vmovups   %zmm2, 128(%rsp)                              #1595.525
        vmovups   %zmm1, 192(%rsp)                              #1595.600
        testl     %esi, %esi                                    #1595.706
        jne       ..B8.11       # Prob 5%                       #1595.706
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm1
..B8.7:                         # Preds ..B8.14 ..B8.6
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #1595.1064
        je        ..B8.2        # Prob 78%                      #1595.1064
                                # LOE rbx r12 r13 r14 r15 edx zmm1
..B8.8:                         # Preds ..B8.7
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #1595.1089
        movl      32(%rsp), %eax                                #1595.1089
                                # LOE rbx r12 r13 r14 r15 eax edx zmm1
..B8.9:                         # Preds ..B8.8
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #1595.1076
        orl       %edx, %eax                                    #1595.1076
        movl      %eax, 32(%rsp)                                #1595.1076
        vldmxcsr  32(%rsp)                                      #1595.1076
        jmp       ..B8.2        # Prob 100%                     #1595.1076
                                # LOE rbx r12 r13 r14 r15 zmm1
..B8.11:                        # Preds ..B8.6
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #1595.786
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B8.18:                        # Preds ..B8.11
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
..B8.12:                        # Preds ..B8.13 ..B8.18
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #1595.849
        jc        ..B8.15       # Prob 5%                       #1595.849
                                # LOE rbx r15 r12d r13d r14d
..B8.13:                        # Preds ..B8.15 ..B8.12
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #1595.802
        cmpl      $8, %ebx                                      #1595.797
        jl        ..B8.12       # Prob 82%                      #1595.797
                                # LOE rbx r15 r12d r13d r14d
..B8.14:                        # Preds ..B8.13
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
        vmovups   192(%rsp), %zmm1                              #1595.1020
        jmp       ..B8.7        # Prob 100%                     #1595.1020
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x18, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x10, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm1
..B8.15:                        # Preds ..B8.12
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,8), %rdi                         #1595.878
        lea       128(%rsp,%rbx,8), %rsi                        #1595.878
        lea       192(%rsp,%rbx,8), %rdx                        #1595.878
..___tag_value___svml_hypot8_ha_z0.166:
#       __svml_dhypot_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_dhypot_ha_cout_rare_internal           #1595.878
..___tag_value___svml_hypot8_ha_z0.167:
        jmp       ..B8.13       # Prob 100%                     #1595.878
        .align    16,0x90
                                # LOE r15 ebx r12d r13d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_hypot8_ha_z0,@function
	.size	__svml_hypot8_ha_z0,.-__svml_hypot8_ha_z0
..LN__svml_hypot8_ha_z0.7:
	.data
# -- End  __svml_hypot8_ha_z0
	.text
.L_2__routine_start___svml_hypot1_ha_e9_8:
# -- Begin  __svml_hypot1_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_hypot1_ha_e9
# --- __svml_hypot1_ha_e9(__m128d, __m128d)
__svml_hypot1_ha_e9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1603.1
        .byte     15                                            #1705.663
        .byte     30                                            #1705.663
        .byte     250                                           #1705.663
	.cfi_startproc
..___tag_value___svml_hypot1_ha_e9.169:
..L170:
                                                        #1603.1
        pushq     %rbp                                          #1603.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1603.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1603.1
        subq      $192, %rsp                                    #1603.1
        movaps    %xmm0, %xmm2                                  #1603.1
        movsd     64+__svml_dhypot_ha_data_internal(%rip), %xmm4 #1636.16
        movaps    %xmm1, %xmm3                                  #1603.1
        movaps    %xmm4, %xmm5                                  #1643.18
        andps     %xmm3, %xmm4                                  #1644.18
        andps     %xmm2, %xmm5                                  #1643.18
        movl      $6291456, %eax                                #1663.24
        unpcklpd  %xmm5, %xmm5                                  #1645.23
        movl      $1002438656, %edx                             #1685.25
        unpcklpd  %xmm4, %xmm4                                  #1645.88
        movaps    %xmm5, %xmm13                                 #1645.11
        maxpd     %xmm4, %xmm13                                 #1645.11
        minpd     %xmm5, %xmm4                                  #1646.11
        pshufd    $85, %xmm13, %xmm8                            #1660.20
        movd      %eax, %xmm7                                   #1663.24
        pshufd    $85, %xmm4, %xmm6                             #1661.20
        movaps    %xmm4, %xmm1                                  #1668.150
        psubd     %xmm6, %xmm8                                  #1662.24
        movaps    %xmm13, %xmm12                                #1647.150
        pcmpgtd   %xmm7, %xmm8                                  #1663.24
        movl      $1141899264, %ecx                             #1686.26
        movsd     __svml_dhypot_ha_data_internal(%rip), %xmm9   #1635.17
        movd      %edx, %xmm6                                   #1685.25
        pshufd    $0, %xmm8, %xmm14                             #1664.24
        movaps    %xmm9, %xmm11                                 #1647.105
        andnps    %xmm9, %xmm14                                 #1666.21
        andps     %xmm13, %xmm11                                #1647.105
        andps     %xmm4, %xmm14                                 #1668.105
        movaps    %xmm11, %xmm10                                #1669.13
        movaps    %xmm14, %xmm0                                 #1670.25
        subsd     %xmm14, %xmm1                                 #1668.150
        mulsd     %xmm11, %xmm10                                #1669.13
        subsd     %xmm11, %xmm12                                #1647.150
        addsd     %xmm11, %xmm13                                #1671.13
        mulsd     %xmm14, %xmm0                                 #1670.25
        mulsd     %xmm12, %xmm13                                #1672.13
        addsd     %xmm10, %xmm0                                 #1670.13
        movaps    %xmm1, %xmm15                                 #1673.25
        movaps    %xmm0, %xmm11                                 #1693.23
        mulsd     %xmm4, %xmm15                                 #1673.25
        movd      %ecx, %xmm4                                   #1686.26
        mulsd     %xmm14, %xmm1                                 #1674.25
        addsd     %xmm13, %xmm15                                #1673.13
        movaps    %xmm0, %xmm13                                 #1675.11
        addsd     %xmm15, %xmm1                                 #1674.13
        movaps    %xmm1, %xmm12                                 #1694.23
        addsd     %xmm1, %xmm13                                 #1675.11
        cvtpd2ps  %xmm13, %xmm8                                 #1691.51
        shufps    $0, %xmm8, %xmm8                              #1691.35
        rsqrtps   %xmm8, %xmm9                                  #1691.22
        cvtps2pd  %xmm9, %xmm15                                 #1691.9
        movaps    %xmm15, %xmm10                                #1692.10
        mulsd     %xmm15, %xmm10                                #1692.10
        mulsd     %xmm15, %xmm1                                 #1702.25
        mulsd     %xmm15, %xmm0                                 #1703.24
        mulsd     %xmm10, %xmm11                                #1693.23
        mulsd     %xmm10, %xmm12                                #1694.23
        subsd     128+__svml_dhypot_ha_data_internal(%rip), %xmm11 #1693.11
        movsd     192+__svml_dhypot_ha_data_internal(%rip), %xmm14 #1695.23
        addsd     %xmm11, %xmm12                                #1694.11
        mulsd     %xmm12, %xmm14                                #1695.23
        pshufd    $85, %xmm13, %xmm5                            #1684.18
        addsd     256+__svml_dhypot_ha_data_internal(%rip), %xmm14 #1695.11
        mulsd     %xmm12, %xmm14                                #1696.23
        pcmpgtd   %xmm5, %xmm6                                  #1685.25
        pcmpgtd   %xmm4, %xmm5                                  #1686.26
        addsd     320+__svml_dhypot_ha_data_internal(%rip), %xmm14 #1696.11
        mulsd     %xmm12, %xmm14                                #1697.23
        por       %xmm5, %xmm6                                  #1687.18
        pshufd    $0, %xmm6, %xmm7                              #1688.18
        addsd     384+__svml_dhypot_ha_data_internal(%rip), %xmm14 #1697.11
        movmskpd  %xmm7, %eax                                   #1689.42
        mulsd     %xmm12, %xmm14                                #1698.23
        addsd     448+__svml_dhypot_ha_data_internal(%rip), %xmm14 #1698.11
        mulsd     %xmm12, %xmm14                                #1699.9
        mulsd     %xmm15, %xmm14                                #1700.9
        mulsd     %xmm13, %xmm14                                #1701.9
        addsd     %xmm14, %xmm1                                 #1702.13
        addsd     %xmm1, %xmm0                                  #1703.12
        andl      $1, %eax                                      #1689.89
        jne       ..B9.3        # Prob 5%                       #1705.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm2 xmm3
..B9.2:                         # Preds ..B9.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1708.12
        popq      %rbp                                          #1708.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1708.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.3:                         # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm2, (%rsp)                                 #1705.204
        movsd     %xmm3, 64(%rsp)                               #1705.277
        movsd     %xmm0, 128(%rsp)                              #1705.350
        jne       ..B9.6        # Prob 5%                       #1705.491
                                # LOE rbx r12 r13 r14 r15
..B9.4:                         # Preds ..B9.6 ..B9.3
                                # Execution count [5.00e-02]: Infreq
        movsd     128(%rsp), %xmm0                              #1705.757
        movq      %rbp, %rsp                                    #1705.757
        popq      %rbp                                          #1705.757
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1705.757
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B9.6:                         # Preds ..B9.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1705.663
        lea       64(%rsp), %rsi                                #1705.663
        lea       128(%rsp), %rdx                               #1705.663
..___tag_value___svml_hypot1_ha_e9.182:
#       __svml_dhypot_ha_cout_rare_internal(const double *, const double *, double *)
        call      __svml_dhypot_ha_cout_rare_internal           #1705.663
..___tag_value___svml_hypot1_ha_e9.183:
        jmp       ..B9.4        # Prob 100%                     #1705.663
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_hypot1_ha_e9,@function
	.size	__svml_hypot1_ha_e9,.-__svml_hypot1_ha_e9
..LN__svml_hypot1_ha_e9.8:
	.data
# -- End  __svml_hypot1_ha_e9
	.text
.L_2__routine_start___svml_dhypot_ha_cout_rare_internal_9:
# -- Begin  __svml_dhypot_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dhypot_ha_cout_rare_internal
	.globl __svml_dhypot_ha_cout_rare_internal
# --- __svml_dhypot_ha_cout_rare_internal(const double *, const double *, double *)
__svml_dhypot_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
# parameter 3: %rdx
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1187.1
        .byte     15                                            #1293.145
        .byte     30                                            #1293.145
        .byte     250                                           #1293.145
	.cfi_startproc
..___tag_value___svml_dhypot_ha_cout_rare_internal.185:
..L186:
                                                        #1187.1
        movq      %rdx, %r8                                     #1187.1
        movzwl    6(%rdi), %eax                                 #1204.37
        andl      $32752, %eax                                  #1204.37
        cmpl      $32752, %eax                                  #1204.63
        je        ..B10.14      # Prob 16%                      #1204.63
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15
..B10.2:                        # Preds ..B10.1
                                # Execution count [8.40e-01]
        movzwl    6(%rsi), %eax                                 #1204.97
        andl      $32752, %eax                                  #1204.97
        cmpl      $32752, %eax                                  #1204.123
        je        ..B10.13      # Prob 16%                      #1204.123
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15
..B10.3:                        # Preds ..B10.2
                                # Execution count [7.06e-01]
        movsd     (%rdi), %xmm2                                 #1206.18
        movsd     4096+_vmldHypotHATab(%rip), %xmm0             #1206.44
        movb      7(%rdi), %dl                                  #1206.18
        movb      7(%rsi), %al                                  #1209.23
        movsd     (%rsi), %xmm1                                 #1209.23
        ucomisd   %xmm0, %xmm2                                  #1206.44
        jp        ..B10.4       # Prob 0%                       #1206.44
        je        ..B10.11      # Prob 16%                      #1206.44
                                # LOE rbx rbp r8 r12 r13 r14 r15 al dl xmm0 xmm1 xmm2
..B10.4:                        # Preds ..B10.3 ..B10.11
                                # Execution count [6.88e-01]
        movsd     %xmm2, -16(%rsp)                              #1208.17
        andb      $127, %dl                                     #1210.38
        movsd     %xmm1, -48(%rsp)                              #1209.17
        andb      $127, %al                                     #1211.38
        movb      %dl, -9(%rsp)                                 #1210.38
        movb      %al, -41(%rsp)                                #1211.38
        movsd     -16(%rsp), %xmm8                              #1212.20
        movsd     -48(%rsp), %xmm1                              #1212.25
        comisd    %xmm8, %xmm1                                  #1212.25
        jbe       ..B10.6       # Prob 50%                      #1212.25
                                # LOE rbx rbp r8 r12 r13 r14 r15 xmm0 xmm1 xmm8
..B10.5:                        # Preds ..B10.4
                                # Execution count [3.44e-01]
        movaps    %xmm8, %xmm2                                  #1214.21
        movaps    %xmm1, %xmm8                                  #1215.21
        movsd     %xmm1, -16(%rsp)                              #1215.21
        movaps    %xmm2, %xmm1                                  #1216.21
                                # LOE rbx rbp r8 r12 r13 r14 r15 xmm0 xmm1 xmm8
..B10.6:                        # Preds ..B10.5 ..B10.4
                                # Execution count [6.88e-01]
        movzwl    -10(%rsp), %edx                               #1218.56
        andl      $32752, %edx                                  #1218.56
        shrl      $4, %edx                                      #1218.56
        negl      %edx                                          #1218.56
        movzwl    4102+_vmldHypotHATab(%rip), %edi              #1221.38
        andl      $-32753, %edi                                 #1221.38
        movsd     %xmm0, -56(%rsp)                              #1220.17
        movsd     4128+_vmldHypotHATab(%rip), %xmm3             #1224.56
        lea       1025(%rdx), %esi                              #1218.56
        negl      %esi                                          #1219.17
        addl      $1000, %esi                                   #1219.17
        shrl      $31, %esi                                     #1219.17
        imull     $-23, %esi, %eax                              #1219.45
        lea       1025(%rax,%rdx), %esi                         #1219.17
        lea       1023(%rsi), %ecx                              #1221.77
        andl      $2047, %ecx                                   #1221.38
        shll      $4, %ecx                                      #1221.38
        orl       %ecx, %edi                                    #1221.38
        movw      %di, -50(%rsp)                                #1221.38
        movsd     -56(%rsp), %xmm2                              #1222.23
        mulsd     %xmm2, %xmm8                                  #1222.17
        mulsd     %xmm2, %xmm1                                  #1223.17
        mulsd     %xmm8, %xmm3                                  #1224.56
        movsd     %xmm3, -72(%rsp)                              #1224.17
        movsd     -72(%rsp), %xmm4                              #1224.96
        movsd     %xmm8, -16(%rsp)                              #1222.17
        subsd     %xmm8, %xmm4                                  #1224.101
        movsd     %xmm4, -64(%rsp)                              #1224.82
        movsd     -72(%rsp), %xmm6                              #1224.123
        movsd     -64(%rsp), %xmm5                              #1224.128
        movsd     %xmm1, -48(%rsp)                              #1223.17
        subsd     %xmm5, %xmm6                                  #1224.128
        movsd     %xmm6, -72(%rsp)                              #1224.109
        movsd     -72(%rsp), %xmm7                              #1224.155
        movzwl    -10(%rsp), %r10d                              #1225.41
        subsd     %xmm7, %xmm8                                  #1224.155
        movzwl    -42(%rsp), %r9d                               #1225.82
        andl      $32752, %r10d                                 #1225.41
        andl      $32752, %r9d                                  #1225.82
        shrl      $4, %r10d                                     #1225.41
        shrl      $4, %r9d                                      #1225.82
        movsd     %xmm8, -64(%rsp)                              #1224.134
        subl      %r9d, %r10d                                   #1225.41
        movsd     -72(%rsp), %xmm8                              #1224.168
        movsd     -64(%rsp), %xmm4                              #1224.179
        cmpl      $6, %r10d                                     #1225.104
        jle       ..B10.8       # Prob 50%                      #1225.104
                                # LOE rbx rbp r8 r12 r13 r14 r15 esi xmm0 xmm1 xmm4 xmm8
..B10.7:                        # Preds ..B10.6
                                # Execution count [3.44e-01]
        movaps    %xmm1, %xmm2                                  #1228.21
        jmp       ..B10.9       # Prob 100%                     #1228.21
                                # LOE rbx rbp r8 r12 r13 r14 r15 esi xmm0 xmm1 xmm2 xmm4 xmm8
..B10.8:                        # Preds ..B10.6
                                # Execution count [3.44e-01]
        movsd     -48(%rsp), %xmm1                              #1232.35
        movsd     4128+_vmldHypotHATab(%rip), %xmm0             #1232.60
        movaps    %xmm1, %xmm7                                  #1232.159
        mulsd     %xmm1, %xmm0                                  #1232.60
        movsd     %xmm0, -72(%rsp)                              #1232.21
        movsd     -72(%rsp), %xmm2                              #1232.100
        subsd     -48(%rsp), %xmm2                              #1232.105
        movsd     %xmm2, -64(%rsp)                              #1232.86
        movsd     -72(%rsp), %xmm5                              #1232.127
        movsd     -64(%rsp), %xmm3                              #1232.132
        subsd     %xmm3, %xmm5                                  #1232.132
        movsd     %xmm5, -72(%rsp)                              #1232.113
        movsd     -72(%rsp), %xmm6                              #1232.159
        subsd     %xmm6, %xmm7                                  #1232.159
        movsd     %xmm7, -64(%rsp)                              #1232.138
        movsd     -72(%rsp), %xmm0                              #1232.172
        movsd     -64(%rsp), %xmm2                              #1232.183
                                # LOE rbx rbp r8 r12 r13 r14 r15 esi xmm0 xmm1 xmm2 xmm4 xmm8
..B10.9:                        # Preds ..B10.7 ..B10.8
                                # Execution count [6.88e-01]
        movsd     -16(%rsp), %xmm6                              #1235.44
        movaps    %xmm8, %xmm3                                  #1234.36
        mulsd     %xmm2, %xmm1                                  #1235.68
        addsd     %xmm8, %xmm6                                  #1235.47
        mulsd     %xmm8, %xmm3                                  #1234.36
        mulsd     %xmm6, %xmm4                                  #1235.47
        movaps    %xmm0, %xmm5                                  #1234.57
        negl      %esi                                          #1248.77
        mulsd     %xmm0, %xmm5                                  #1234.57
        addsd     %xmm1, %xmm4                                  #1235.68
        mulsd     %xmm2, %xmm0                                  #1235.88
        addsd     %xmm5, %xmm3                                  #1234.57
        addsd     %xmm0, %xmm4                                  #1235.88
        movaps    %xmm3, %xmm7                                  #1236.31
        addl      $1023, %esi                                   #1248.77
        movq      4112+_vmldHypotHATab(%rip), %r11              #1243.44
        movq      %r11, %r9                                     #1244.38
        lea       _vmldHypotHATab(%rip), %rdx                   #1242.50
        addsd     %xmm4, %xmm7                                  #1236.31
        movsd     %xmm7, -56(%rsp)                              #1236.17
        andl      $2047, %esi                                   #1248.38
        movzwl    -50(%rsp), %ecx                               #1237.42
        andl      $32752, %ecx                                  #1237.42
        shrl      $4, %ecx                                      #1237.42
        addl      $-1023, %ecx                                  #1237.67
        movl      %ecx, %eax                                    #1240.26
        andl      $1, %eax                                      #1240.26
        subl      %eax, %ecx                                    #1241.22
        shrl      $1, %ecx                                      #1241.32
        movsd     %xmm7, -48(%rsp)                              #1238.17
        movzwl    -42(%rsp), %edi                               #1239.38
        andl      $-32753, %edi                                 #1239.38
        shrq      $48, %r9                                      #1244.38
        lea       1023(%rcx), %r10d                             #1244.68
        addl      %ecx, %ecx                                    #1246.69
        addl      $16368, %edi                                  #1239.38
        negl      %ecx                                          #1246.69
        andl      $2047, %r10d                                  #1244.38
        addl      $1023, %ecx                                   #1246.73
        andl      $2047, %ecx                                   #1246.38
        andl      $-32753, %r9d                                 #1244.38
        movw      %di, -42(%rsp)                                #1239.38
        shll      $4, %r10d                                     #1244.38
        shll      $4, %ecx                                      #1246.38
        orl       %r9d, %r10d                                   #1244.38
        shll      $4, %esi                                      #1248.38
        orl       %r9d, %ecx                                    #1246.38
        movsd     -48(%rsp), %xmm2                              #1242.22
        orl       %esi, %r9d                                    #1248.38
        movl      -44(%rsp), %esi                               #1249.42
        mulsd     4112(%rdx,%rax,8), %xmm2                      #1242.50
        andl      $1048575, %esi                                #1249.42
        shrl      $12, %esi                                     #1249.77
        shll      $8, %eax                                      #1250.31
        addl      %eax, %esi                                    #1250.31
        movsd     (%rdx,%rsi,8), %xmm8                          #1251.79
        movsd     4104+_vmldHypotHATab(%rip), %xmm1             #1251.79
        mulsd     %xmm8, %xmm2                                  #1252.42
        mulsd     %xmm8, %xmm1                                  #1251.79
        movaps    %xmm2, %xmm9                                  #1253.66
        mulsd     %xmm1, %xmm9                                  #1253.66
        movsd     4104+_vmldHypotHATab(%rip), %xmm11            #1253.66
        movsd     4104+_vmldHypotHATab(%rip), %xmm14            #1256.74
        subsd     %xmm9, %xmm11                                 #1253.66
        movaps    %xmm11, %xmm10                                #1254.37
        mulsd     %xmm2, %xmm11                                 #1255.37
        mulsd     %xmm1, %xmm10                                 #1254.37
        addsd     %xmm11, %xmm2                                 #1255.37
        addsd     %xmm10, %xmm1                                 #1254.37
        movaps    %xmm2, %xmm12                                 #1256.74
        movaps    %xmm1, %xmm13                                 #1257.37
        mulsd     %xmm1, %xmm12                                 #1256.74
        movsd     4104+_vmldHypotHATab(%rip), %xmm0             #1259.66
        subsd     %xmm12, %xmm14                                #1256.74
        mulsd     %xmm14, %xmm13                                #1257.37
        mulsd     %xmm2, %xmm14                                 #1258.37
        addsd     %xmm13, %xmm1                                 #1257.37
        addsd     %xmm14, %xmm2                                 #1258.37
        movaps    %xmm2, %xmm15                                 #1259.66
        movaps    %xmm2, %xmm5                                  #1261.37
        mulsd     %xmm1, %xmm15                                 #1259.66
        movsd     4128+_vmldHypotHATab(%rip), %xmm6             #1262.55
        subsd     %xmm15, %xmm0                                 #1259.66
        mulsd     %xmm0, %xmm5                                  #1261.37
        mulsd     %xmm1, %xmm0                                  #1260.37
        addsd     %xmm5, %xmm2                                  #1261.37
        addsd     %xmm0, %xmm1                                  #1260.37
        mulsd     %xmm2, %xmm6                                  #1262.55
        movsd     %xmm6, -72(%rsp)                              #1262.17
        movaps    %xmm2, %xmm11                                 #1262.152
        movsd     -72(%rsp), %xmm7                              #1262.95
        movq      %r11, -32(%rsp)                               #1245.17
        subsd     %xmm2, %xmm7                                  #1262.100
        movsd     %xmm7, -64(%rsp)                              #1262.81
        movsd     -72(%rsp), %xmm9                              #1262.121
        movsd     -64(%rsp), %xmm8                              #1262.126
        movw      %cx, -26(%rsp)                                #1246.38
        subsd     %xmm8, %xmm9                                  #1262.126
        movsd     %xmm9, -72(%rsp)                              #1262.107
        movsd     -72(%rsp), %xmm10                             #1262.152
        movsd     -32(%rsp), %xmm15                             #1263.36
        subsd     %xmm10, %xmm11                                #1262.152
        mulsd     %xmm15, %xmm3                                 #1263.36
        mulsd     %xmm15, %xmm4                                 #1266.36
        movsd     %xmm11, -64(%rsp)                             #1262.132
        movsd     -72(%rsp), %xmm13                             #1262.164
        movsd     4120+_vmldHypotHATab(%rip), %xmm14            #1264.47
        movaps    %xmm13, %xmm12                                #1263.63
        mulsd     %xmm13, %xmm12                                #1263.63
        mulsd     %xmm13, %xmm14                                #1264.75
        subsd     %xmm12, %xmm3                                 #1263.63
        movsd     -64(%rsp), %xmm5                              #1262.174
        mulsd     %xmm5, %xmm14                                 #1264.82
        mulsd     %xmm5, %xmm5                                  #1265.36
        subsd     %xmm14, %xmm3                                 #1264.17
        movq      %r11, -40(%rsp)                               #1243.17
        subsd     %xmm5, %xmm3                                  #1265.17
        movw      %r10w, -34(%rsp)                              #1244.38
        addsd     %xmm4, %xmm3                                  #1266.17
        mulsd     %xmm1, %xmm3                                  #1267.37
        movq      %r11, -24(%rsp)                               #1247.17
        addsd     %xmm3, %xmm2                                  #1267.37
        mulsd     -40(%rsp), %xmm2                              #1268.36
        movw      %r9w, -18(%rsp)                               #1248.38
        mulsd     -24(%rsp), %xmm2                              #1268.47
        movsd     %xmm2, (%r8)                                  #1268.17
                                # LOE rbx rbp r12 r13 r14 r15
..B10.10:                       # Preds ..B10.9 ..B10.12 ..B10.13 ..B10.15 ..B10.20
                                #
                                # Execution count [1.00e+00]
        xorl      %eax, %eax                                    #1305.12
        ret                                                     #1305.12
                                # LOE
..B10.11:                       # Preds ..B10.3
                                # Execution count [1.13e-01]: Infreq
        ucomisd   %xmm0, %xmm1                                  #1206.96
        jne       ..B10.4       # Prob 84%                      #1206.96
        jp        ..B10.4       # Prob 0%                       #1206.96
                                # LOE rbx rbp r8 r12 r13 r14 r15 al dl xmm0 xmm1 xmm2
..B10.12:                       # Preds ..B10.11
                                # Execution count [1.81e-02]: Infreq
        movsd     %xmm0, (%r8)                                  #1272.17
        jmp       ..B10.10      # Prob 100%                     #1272.17
                                # LOE rbx rbp r12 r13 r14 r15
..B10.13:                       # Preds ..B10.21 ..B10.22 ..B10.2
                                # Execution count [2.47e-01]: Infreq
        movsd     (%rsi), %xmm0                                 #1279.25
        mulsd     %xmm0, %xmm0                                  #1279.34
        movsd     %xmm0, (%r8)                                  #1279.17
        jmp       ..B10.10      # Prob 100%                     #1279.17
                                # LOE rbx rbp r12 r13 r14 r15
..B10.14:                       # Preds ..B10.1
                                # Execution count [4.71e-02]: Infreq
        movzwl    6(%rsi), %eax                                 #1283.44
        andl      $32752, %eax                                  #1283.44
        cmpl      $32752, %eax                                  #1283.70
        je        ..B10.16      # Prob 16%                      #1283.70
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15
..B10.15:                       # Preds ..B10.22 ..B10.14
                                # Execution count [3.96e-02]: Infreq
        movsd     (%rdi), %xmm0                                 #1285.29
        mulsd     %xmm0, %xmm0                                  #1285.38
        movsd     %xmm0, (%r8)                                  #1285.21
        jmp       ..B10.10      # Prob 100%                     #1285.21
                                # LOE rbx rbp r12 r13 r14 r15
..B10.16:                       # Preds ..B10.14
                                # Execution count [7.54e-03]: Infreq
        movl      4(%rdi), %eax                                 #1288.50
        andl      $1048575, %eax                                #1288.50
        jne       ..B10.18      # Prob 50%                      #1288.82
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15 eax
..B10.17:                       # Preds ..B10.16
                                # Execution count [3.77e-03]: Infreq
        cmpl      $0, (%rdi)                                    #1288.143
        je        ..B10.22      # Prob 50%                      #1288.143
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15 eax
..B10.18:                       # Preds ..B10.16 ..B10.17
                                # Execution count [5.65e-03]: Infreq
        testl     $1048575, 4(%rsi)                             #1288.175
        jne       ..B10.20      # Prob 50%                      #1288.207
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15 eax
..B10.19:                       # Preds ..B10.18
                                # Execution count [2.83e-03]: Infreq
        cmpl      $0, (%rsi)                                    #1288.268
        je        ..B10.21      # Prob 50%                      #1288.268
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15 eax
..B10.20:                       # Preds ..B10.19 ..B10.18
                                # Execution count [4.24e-03]: Infreq
        movsd     (%rdi), %xmm0                                 #1290.33
        mulsd     (%rsi), %xmm0                                 #1290.42
        movsd     %xmm0, (%r8)                                  #1290.25
        jmp       ..B10.10      # Prob 100%                     #1290.25
                                # LOE rbx rbp r12 r13 r14 r15
..B10.21:                       # Preds ..B10.19
                                # Execution count [1.41e-03]: Infreq
        testl     %eax, %eax                                    #1293.84
        jne       ..B10.13      # Prob 50%                      #1293.84
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15
..B10.27:                       # Preds ..B10.21
                                # Execution count [7.07e-04]: Infreq
        cmpl      $0, (%rdi)                                    #1288.143
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15
..B10.22:                       # Preds ..B10.17 ..B10.27
                                # Execution count [1.65e-03]: Infreq
        je        ..B10.15      # Prob 50%                      #1293.145
        jmp       ..B10.13      # Prob 100%                     #1293.145
        .align    16,0x90
                                # LOE rbx rbp rsi rdi r8 r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_dhypot_ha_cout_rare_internal,@function
	.size	__svml_dhypot_ha_cout_rare_internal,.-__svml_dhypot_ha_cout_rare_internal
..LN__svml_dhypot_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dhypot_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_dhypot_ha_data_internal
	.globl __svml_dhypot_ha_data_internal
__svml_dhypot_ha_data_internal:
	.long	0
	.long	4294950912
	.long	0
	.long	4294950912
	.long	0
	.long	4294950912
	.long	0
	.long	4294950912
	.long	0
	.long	4294950912
	.long	0
	.long	4294950912
	.long	0
	.long	4294950912
	.long	0
	.long	4294950912
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
	.long	3218046976
	.long	0
	.long	3218046976
	.long	0
	.long	3218046976
	.long	0
	.long	3218046976
	.long	0
	.long	3218046976
	.long	0
	.long	3218046976
	.long	0
	.long	3218046976
	.long	0
	.long	3218046976
	.long	0
	.long	1070694400
	.long	0
	.long	1070694400
	.long	0
	.long	1070694400
	.long	0
	.long	1070694400
	.long	0
	.long	1070694400
	.long	0
	.long	1070694400
	.long	0
	.long	1070694400
	.long	0
	.long	1070694400
	.long	0
	.long	3218341888
	.long	0
	.long	3218341888
	.long	0
	.long	3218341888
	.long	0
	.long	3218341888
	.long	0
	.long	3218341888
	.long	0
	.long	3218341888
	.long	0
	.long	3218341888
	.long	0
	.long	3218341888
	.long	0
	.long	1071120384
	.long	0
	.long	1071120384
	.long	0
	.long	1071120384
	.long	0
	.long	1071120384
	.long	0
	.long	1071120384
	.long	0
	.long	1071120384
	.long	0
	.long	1071120384
	.long	0
	.long	1071120384
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	6291456
	.long	6291456
	.long	6291456
	.long	6291456
	.long	6291456
	.long	6291456
	.long	6291456
	.long	6291456
	.long	6291456
	.long	6291456
	.long	6291456
	.long	6291456
	.long	6291456
	.long	6291456
	.long	6291456
	.long	6291456
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1002438656
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	1141899264
	.long	0
	.long	1082126336
	.long	0
	.long	1082126336
	.long	0
	.long	1082126336
	.long	0
	.long	1082126336
	.long	0
	.long	1082126336
	.long	0
	.long	1082126336
	.long	0
	.long	1082126336
	.long	0
	.long	1082126336
	.long	0
	.long	1078951936
	.long	0
	.long	1078951936
	.long	0
	.long	1078951936
	.long	0
	.long	1078951936
	.long	0
	.long	1078951936
	.long	0
	.long	1078951936
	.long	0
	.long	1078951936
	.long	0
	.long	1078951936
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
	.type	__svml_dhypot_ha_data_internal,@object
	.size	__svml_dhypot_ha_data_internal,896
	.align 32
_vmldHypotHATab:
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
	.long	0
	.long	0
	.long	1071644672
	.long	0
	.long	1072693248
	.long	0
	.long	1073741824
	.long	33554432
	.long	1101004800
	.type	_vmldHypotHATab,@object
	.size	_vmldHypotHATab,4136
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
