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
	.file "svml_d_tan.c"
	.text
..TXTST0:
.L_2__routine_start___svml_tan1_ha_ex_0:
# -- Begin  __svml_tan1_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tan1_ha_ex
# --- __svml_tan1_ha_ex(__m128d)
__svml_tan1_ha_ex:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #6047.1
        .byte     15                                            #6476.22
        .byte     30                                            #6476.22
        .byte     250                                           #6476.22
	.cfi_startproc
..___tag_value___svml_tan1_ha_ex.1:
..L2:
                                                          #6047.1
        pushq     %rbp                                          #6047.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #6047.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #6047.1
        subq      $128, %rsp                                    #6047.1
        xorl      %edx, %edx                                    #6106.9
        movsd     1024+__svml_dtan_ha_data_internal(%rip), %xmm10 #6107.23
        movaps    %xmm10, %xmm1                                 #6108.20
        andnps    %xmm0, %xmm10                                 #6109.21
        andps     %xmm0, %xmm1                                  #6108.20
        movaps    %xmm1, %xmm11                                 #6116.29
        movaps    %xmm1, %xmm3                                  #6119.18
        mulsd     1472+__svml_dtan_ha_data_internal(%rip), %xmm11 #6116.29
        movaps    %xmm1, %xmm2                                  #6111.34
        cmpnlesd  28160+__svml_dtan_ha_data_internal(%rip), %xmm2 #6111.34
        movsd     1152+__svml_dtan_ha_data_internal(%rip), %xmm5 #6115.22
        movq      1408+__svml_dtan_ha_data_internal(%rip), %xmm8 #6128.25
        addsd     %xmm5, %xmm11                                 #6116.17
        movmskpd  %xmm2, %esi                                   #6113.82
        movaps    %xmm11, %xmm6                                 #6117.17
        pand      %xmm8, %xmm11                                 #6129.21
        pand      .L_2il0floatpacket.26(%rip), %xmm11           #6134.21
        subsd     %xmm5, %xmm6                                  #6117.17
        movd      %xmm11, %eax                                  #6135.136
        movaps    %xmm6, %xmm9                                  #6119.40
        movaps    %xmm6, %xmm4                                  #6121.23
        mulsd     1216+__svml_dtan_ha_data_internal(%rip), %xmm9 #6119.40
        imull     $104, %eax, %ecx                              #6135.234
        mulsd     1280+__svml_dtan_ha_data_internal(%rip), %xmm4 #6121.23
        mulsd     1344+__svml_dtan_ha_data_internal(%rip), %xmm6 #6126.36
        subsd     %xmm9, %xmm3                                  #6119.18
        movaps    %xmm3, %xmm7                                  #6122.22
        lea       1632+__svml_dtan_ha_data_internal(%rip), %rax #6135.378
        movups    .L_2il0floatpacket.27(%rip), %xmm15           #6149.24
        subsd     %xmm4, %xmm7                                  #6122.22
        movaps    %xmm7, %xmm5                                  #6130.17
        subsd     %xmm7, %xmm3                                  #6123.21
        movq      -96(%rcx,%rax), %xmm11                        #6135.378
        subsd     %xmm4, %xmm3                                  #6124.21
        movdqa    %xmm11, %xmm12                                #6148.22
        subsd     %xmm6, %xmm3                                  #6126.17
        movups    %xmm0, 16(%rsp)                               #6047.1[spill]
        movaps    %xmm15, %xmm0                                 #6150.20
        movq      -56(%rax,%rcx), %xmm9                         #6140.379
        addsd     %xmm3, %xmm5                                  #6130.17
        movq      -72(%rax,%rcx), %xmm6                         #6138.379
        subsd     %xmm5, %xmm12                                 #6148.22
        subsd     %xmm5, %xmm7                                  #6131.18
        subsd     %xmm12, %xmm11                                #6151.20
        addsd     %xmm7, %xmm3                                  #6132.17
        subsd     %xmm5, %xmm11                                 #6152.20
        andps     %xmm12, %xmm0                                 #6150.20
        subsd     %xmm3, %xmm11                                 #6153.20
        cvtpd2ps  %xmm0, %xmm14                                 #6157.64
        subsd     %xmm0, %xmm12                                 #6154.22
        movlhps   %xmm14, %xmm14                                #6157.48
        addsd     -88(%rax,%rcx), %xmm12                        #6155.22
        rcpps     %xmm14, %xmm13                                #6157.37
        addsd     %xmm12, %xmm11                                #6156.20
        cvtps2pd  %xmm13, %xmm12                                #6157.24
        andps     %xmm15, %xmm12                                #6158.24
        mulsd     %xmm12, %xmm0                                 #6160.39
        movups    .L_2il0floatpacket.28(%rip), %xmm13           #6159.37
        movaps    %xmm13, %xmm14                                #6160.18
        movq      -24(%rax,%rcx), %xmm7                         #6144.376
        subsd     %xmm0, %xmm14                                 #6160.18
        mulsd     %xmm5, %xmm7                                  #6180.30
        movaps    %xmm12, %xmm0                                 #6161.32
        movaps    %xmm14, %xmm15                                #6162.30
        mulsd     %xmm14, %xmm0                                 #6161.32
        addsd     -32(%rax,%rcx), %xmm7                         #6180.18
        mulsd     %xmm14, %xmm15                                #6162.30
        addsd     %xmm12, %xmm0                                 #6161.20
        mulsd     -80(%rax,%rcx), %xmm12                        #6168.28
        addsd     %xmm13, %xmm15                                #6162.18
        mulsd     %xmm15, %xmm0                                 #6163.24
        mulsd     %xmm0, %xmm11                                 #6164.20
        movaps    %xmm11, %xmm13                                #6165.19
        mulsd     %xmm11, %xmm11                                #6166.36
        subsd     %xmm14, %xmm13                                #6165.19
        movdqa    %xmm9, %xmm14                                 #6170.22
        subsd     %xmm13, %xmm11                                #6166.24
        mulsd     %xmm5, %xmm14                                 #6170.22
        addsd     -48(%rax,%rcx), %xmm9                         #6183.18
        mulsd     %xmm0, %xmm11                                 #6167.24
        mulsd     %xmm3, %xmm9                                  #6187.31
        mulsd     -80(%rax,%rcx), %xmm11                        #6169.28
        movdqa    %xmm6, %xmm13                                 #6171.22
        movq      -8(%rax,%rcx), %xmm8                          #6146.376
        addsd     %xmm14, %xmm13                                #6171.22
        mulsd     %xmm5, %xmm8                                  #6181.30
        subsd     %xmm13, %xmm6                                 #6172.22
        addsd     -16(%rax,%rcx), %xmm8                         #6181.18
        addsd     %xmm14, %xmm6                                 #6174.22
        movaps    %xmm13, %xmm0                                 #6173.22
        addsd     %xmm11, %xmm6                                 #6176.22
        addsd     %xmm12, %xmm0                                 #6173.22
        movaps    %xmm5, %xmm11                                 #6182.18
        subsd     %xmm0, %xmm12                                 #6175.22
        mulsd     %xmm5, %xmm11                                 #6182.18
        addsd     %xmm13, %xmm12                                #6177.22
        mulsd     %xmm11, %xmm7                                 #6185.31
        addsd     %xmm12, %xmm6                                 #6178.22
        movq      -40(%rax,%rcx), %xmm4                         #6142.376
        addsd     %xmm6, %xmm9                                  #6187.19
        mulsd     %xmm5, %xmm4                                  #6179.30
        movq      (%rax,%rcx), %xmm12                           #6184.30
        mulsd     %xmm11, %xmm12                                #6184.30
        addsd     -48(%rax,%rcx), %xmm4                         #6179.18
        mulsd     %xmm11, %xmm11                                #6186.18
        addsd     %xmm8, %xmm12                                 #6184.18
        addsd     %xmm4, %xmm7                                  #6185.19
        mulsd     %xmm11, %xmm12                                #6188.31
        movq      -64(%rax,%rcx), %xmm3                         #6189.19
        addsd     %xmm7, %xmm12                                 #6188.19
        addsd     %xmm9, %xmm3                                  #6189.19
        mulsd     %xmm5, %xmm12                                 #6190.31
        movups    %xmm10, 32(%rsp)                              #6109.21[spill]
        addsd     %xmm3, %xmm12                                 #6190.19
        addsd     %xmm12, %xmm0                                 #6191.18
        pxor      %xmm10, %xmm0                                 #6192.18
        testl     $1, %esi                                      #6193.59
        jne       ..B1.8        # Prob 5%                       #6193.70
                                # LOE rax rbx r12 r13 r14 r15 edx xmm0 xmm1 xmm2
..B1.2:                         # Preds ..B1.8 ..B1.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #6479.52
        jne       ..B1.4        # Prob 5%                       #6479.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B1.3:                         # Preds ..B1.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #6482.12
        popq      %rbp                                          #6482.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #6482.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.4:                         # Preds ..B1.2
                                # Execution count [5.00e-02]: Infreq
        movups    16(%rsp), %xmm1                               #6479.160[spill]
        movsd     %xmm1, (%rsp)                                 #6479.160
        movsd     %xmm0, 64(%rsp)                               #6479.233
        jne       ..B1.6        # Prob 5%                       #6479.374
                                # LOE rbx r12 r13 r14 r15 edx
..B1.5:                         # Preds ..B1.7 ..B1.6 ..B1.4
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #6479.625
        movq      %rbp, %rsp                                    #6479.625
        popq      %rbp                                          #6479.625
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #6479.625
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B1.6:                         # Preds ..B1.4
                                # Execution count [2.50e-03]: Infreq
        je        ..B1.5        # Prob 95%                      #6479.517
                                # LOE rbx r12 r13 r14 r15
..B1.7:                         # Preds ..B1.6
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #6479.546
        lea       64(%rsp), %rsi                                #6479.546
..___tag_value___svml_tan1_ha_ex.17:
#       __svml_dtan_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtan_ha_cout_rare_internal             #6479.546
..___tag_value___svml_tan1_ha_ex.18:
        jmp       ..B1.5        # Prob 100%                     #6479.546
                                # LOE rbx r12 r13 r14 r15
..B1.8:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movdqu    .L_2il0floatpacket.29(%rip), %xmm10           #6286.16
        movaps    %xmm1, %xmm8                                  #6197.21
        pand      %xmm1, %xmm10                                 #6286.16
        lea       __svml_dtan_ha_reduction_data_internal(%rip), %rdi #6288.377
        psrlq     $52, %xmm10                                   #6287.16
        movd      %xmm10, %ecx                                  #6288.124
        movdqu    .L_2il0floatpacket.30(%rip), %xmm14           #6296.19
        pand      %xmm1, %xmm14                                 #6296.19
        paddq     .L_2il0floatpacket.31(%rip), %xmm14           #6297.19
        movdqa    %xmm14, %xmm13                                #6305.10
        lea       (%rcx,%rcx,2), %esi                           #6288.224
        shll      $3, %esi                                      #6288.224
        psrlq     $32, %xmm13                                   #6305.10
        movq      16(%rdi,%rsi), %xmm11                         #6290.377
        movdqu    .L_2il0floatpacket.32(%rip), %xmm12           #6298.15
        movdqa    %xmm11, %xmm3                                 #6303.10
        movsd     1088+__svml_dtan_ha_data_internal(%rip), %xmm6 #6196.28
        psrlq     $32, %xmm3                                    #6303.10
        movups    %xmm0, (%rsp)                                 #[spill]
        pand      %xmm12, %xmm14                                #6306.10
        movdqa    %xmm13, %xmm0                                 #6311.11
        andps     %xmm6, %xmm8                                  #6197.21
        movq      8(%rdi,%rsi), %xmm9                           #6289.378
        movdqa    %xmm14, %xmm7                                 #6316.11
        pmuludq   %xmm3, %xmm0                                  #6311.11
        cmpeqsd   %xmm6, %xmm8                                  #6198.29
        pmuludq   %xmm14, %xmm3                                 #6317.11
        movmskpd  %xmm8, %edx                                   #6200.52
        movdqa    %xmm9, %xmm10                                 #6301.10
        pand      %xmm12, %xmm9                                 #6302.10
        pmuludq   %xmm9, %xmm7                                  #6316.11
        pand      %xmm12, %xmm11                                #6304.10
        pmuludq   %xmm13, %xmm11                                #6312.11
        pmuludq   %xmm13, %xmm9                                 #6310.11
        psrlq     $32, %xmm3                                    #6319.11
        psrlq     $32, %xmm10                                   #6301.10
        movdqa    %xmm14, %xmm8                                 #6315.11
        movdqa    %xmm12, %xmm6                                 #6327.11
        paddq     %xmm3, %xmm0                                  #6323.11
        pmuludq   %xmm10, %xmm8                                 #6315.11
        pmuludq   %xmm13, %xmm10                                #6309.11
        pand      %xmm7, %xmm6                                  #6327.11
        movdqa    %xmm12, %xmm15                                #6300.10
        paddq     %xmm0, %xmm6                                  #6332.11
        movq      (%rsi,%rdi), %xmm4                            #6288.377
        psrlq     $32, %xmm11                                   #6337.11
        psrlq     $32, %xmm7                                    #6320.11
        pand      %xmm4, %xmm15                                 #6300.10
        movdqa    %xmm14, %xmm5                                 #6314.11
        movdqa    %xmm12, %xmm3                                 #6328.11
        paddq     %xmm6, %xmm11                                 #6338.11
        paddq     %xmm7, %xmm9                                  #6324.11
        pmuludq   %xmm15, %xmm5                                 #6314.11
        pmuludq   %xmm15, %xmm13                                #6308.11
        pand      %xmm8, %xmm3                                  #6328.11
        movdqa    %xmm11, %xmm0                                 #6339.11
        paddq     %xmm9, %xmm3                                  #6333.11
        psrlq     $32, %xmm0                                    #6339.11
        psrlq     $32, %xmm8                                    #6321.11
        psrlq     $32, %xmm4                                    #6299.10
        movdqa    %xmm12, %xmm7                                 #6329.11
        paddq     %xmm3, %xmm0                                  #6340.11
        paddq     %xmm8, %xmm10                                 #6325.11
        pmuludq   %xmm4, %xmm14                                 #6313.11
        pand      %xmm5, %xmm7                                  #6329.11
        movdqa    %xmm0, %xmm9                                  #6341.11
        paddq     %xmm10, %xmm7                                 #6334.11
        psrlq     $32, %xmm9                                    #6341.11
        psrlq     $32, %xmm5                                    #6322.11
        paddq     %xmm7, %xmm9                                  #6342.11
        paddq     %xmm5, %xmm13                                 #6326.11
        pand      %xmm12, %xmm14                                #6330.11
        movdqa    %xmm9, %xmm6                                  #6343.11
        paddq     %xmm13, %xmm14                                #6335.11
        psrlq     $32, %xmm6                                    #6343.11
        pand      %xmm12, %xmm9                                 #6346.11
        paddq     %xmm14, %xmm6                                 #6344.11
        psllq     $32, %xmm6                                    #6347.17
        pand      %xmm12, %xmm11                                #6345.11
        paddq     %xmm9, %xmm6                                  #6349.17
        movups    .L_2il0floatpacket.33(%rip), %xmm7            #6352.15
        psllq     $32, %xmm0                                    #6348.17
        movups    .L_2il0floatpacket.28(%rip), %xmm12           #6354.20
        andps     %xmm1, %xmm7                                  #6352.15
        movdqa    %xmm6, %xmm8                                  #6355.18
        movaps    %xmm12, %xmm15                                #6354.20
        paddq     %xmm11, %xmm0                                 #6350.17
        movups    .L_2il0floatpacket.34(%rip), %xmm11           #6358.15
        psrlq     $12, %xmm8                                    #6355.18
        pxor      %xmm7, %xmm15                                 #6354.20
        movaps    %xmm11, %xmm5                                 #6359.19
        orps      %xmm15, %xmm8                                 #6356.18
        andl      $1, %edx                                      #6200.106
        movdqu    .L_2il0floatpacket.38(%rip), %xmm14           #6377.18
        addsd     %xmm8, %xmm5                                  #6359.19
        movaps    %xmm5, %xmm13                                 #6360.9
        pand      %xmm6, %xmm14                                 #6377.18
        movdqu    .L_2il0floatpacket.36(%rip), %xmm10           #6368.18
        psllq     $40, %xmm14                                   #6378.18
        movups    .L_2il0floatpacket.35(%rip), %xmm4            #6366.20
        pand      %xmm0, %xmm10                                 #6368.18
        psrlq     $24, %xmm0                                    #6379.17
        pxor      %xmm7, %xmm4                                  #6366.20
        xorps     .L_2il0floatpacket.37(%rip), %xmm7            #6375.20
        por       %xmm0, %xmm14                                 #6380.18
        orps      %xmm7, %xmm14                                 #6381.18
        psllq     $28, %xmm10                                   #6369.18
        orps      %xmm4, %xmm10                                 #6370.18
        subsd     %xmm11, %xmm13                                #6360.9
        subsd     %xmm7, %xmm14                                 #6384.18
        subsd     %xmm4, %xmm10                                 #6373.17
        subsd     %xmm13, %xmm8                                 #6361.17
        movaps    %xmm8, %xmm13                                 #6385.11
        movups    .L_2il0floatpacket.27(%rip), %xmm3            #6393.16
        addsd     %xmm14, %xmm13                                #6385.11
        movaps    %xmm3, %xmm6                                  #6393.16
        subsd     %xmm13, %xmm8                                 #6386.17
        andps     %xmm13, %xmm6                                 #6393.16
        addsd     %xmm8, %xmm14                                 #6387.18
        subsd     %xmm6, %xmm13                                 #6394.17
        addsd     %xmm10, %xmm14                                #6388.11
        movups    .L_2il0floatpacket.40(%rip), %xmm15           #6390.16
        movups    .L_2il0floatpacket.41(%rip), %xmm9            #6391.17
        movaps    %xmm15, %xmm4                                 #6395.16
        movaps    %xmm9, %xmm0                                  #6397.12
        mulsd     %xmm6, %xmm4                                  #6395.16
        mulsd     %xmm6, %xmm0                                  #6397.12
        mulsd     %xmm13, %xmm15                                #6396.12
        mulsd     %xmm13, %xmm9                                 #6399.12
        addsd     %xmm0, %xmm15                                 #6400.13
        movups    .L_2il0floatpacket.39(%rip), %xmm6            #6398.12
        movaps    %xmm4, %xmm8                                  #6403.13
        mulsd     %xmm14, %xmm6                                 #6398.12
        pand      .L_2il0floatpacket.44(%rip), %xmm5            #6364.13
        addsd     %xmm9, %xmm6                                  #6401.13
        pand      .L_2il0floatpacket.26(%rip), %xmm5            #6417.25
        addsd     %xmm6, %xmm15                                 #6402.13
        movd      %xmm5, %r8d                                   #6418.140
        movups    .L_2il0floatpacket.43(%rip), %xmm11           #6407.16
        addsd     %xmm15, %xmm8                                 #6403.13
        imull     $104, %r8d, %r9d                              #6418.238
        movups    .L_2il0floatpacket.42(%rip), %xmm14           #6408.11
        movaps    %xmm11, %xmm7                                 #6409.17
        andps     %xmm1, %xmm14                                 #6408.11
        subsd     %xmm8, %xmm4                                  #6404.16
        cmpltsd   %xmm14, %xmm7                                 #6409.17
        addsd     %xmm15, %xmm4                                 #6405.13
        movaps    %xmm14, %xmm13                                #6409.17
        movaps    %xmm3, %xmm15                                 #6433.24
        movsd     %xmm7, %xmm13                                 #6409.17
        cmplesd   %xmm11, %xmm14                                #6410.20
        movq      -96(%r9,%rax), %xmm11                         #6418.382
        andps     %xmm1, %xmm14                                 #6411.24
        andps     %xmm13, %xmm8                                 #6412.25
        movdqa    %xmm11, %xmm1                                 #6431.26
        orps      %xmm8, %xmm14                                 #6413.9
        andps     %xmm4, %xmm13                                 #6414.9
        movq      -56(%rax,%r9), %xmm10                         #6423.383
        subsd     %xmm14, %xmm1                                 #6431.26
        andps     %xmm1, %xmm15                                 #6433.24
        subsd     %xmm1, %xmm11                                 #6434.24
        subsd     %xmm15, %xmm1                                 #6437.26
        cvtpd2ps  %xmm15, %xmm0                                 #6440.68
        subsd     %xmm14, %xmm11                                #6435.24
        addsd     -88(%rax,%r9), %xmm1                          #6438.26
        subsd     %xmm13, %xmm11                                #6436.24
        movlhps   %xmm0, %xmm0                                  #6440.52
        addsd     %xmm1, %xmm11                                 #6439.24
        rcpps     %xmm0, %xmm1                                  #6440.41
        cvtps2pd  %xmm1, %xmm1                                  #6440.28
        andps     %xmm3, %xmm1                                  #6441.28
        movaps    %xmm12, %xmm0                                 #6443.22
        mulsd     %xmm1, %xmm15                                 #6443.43
        movaps    %xmm1, %xmm3                                  #6444.36
        movq      -72(%rax,%r9), %xmm7                          #6421.383
        subsd     %xmm15, %xmm0                                 #6443.22
        movaps    %xmm0, %xmm15                                 #6445.34
        mulsd     %xmm0, %xmm3                                  #6444.36
        mulsd     %xmm0, %xmm15                                 #6445.34
        addsd     %xmm1, %xmm3                                  #6444.24
        mulsd     -80(%rax,%r9), %xmm1                          #6451.32
        addsd     %xmm12, %xmm15                                #6445.22
        mulsd     %xmm15, %xmm3                                 #6446.28
        mulsd     %xmm3, %xmm11                                 #6447.24
        movaps    %xmm11, %xmm12                                #6448.23
        mulsd     %xmm11, %xmm11                                #6449.40
        subsd     %xmm0, %xmm12                                 #6448.23
        movdqa    %xmm10, %xmm0                                 #6453.26
        subsd     %xmm12, %xmm11                                #6449.28
        mulsd     %xmm14, %xmm0                                 #6453.26
        addsd     -48(%rax,%r9), %xmm10                         #6466.22
        mulsd     %xmm3, %xmm11                                 #6450.28
        mulsd     %xmm13, %xmm10                                #6470.35
        mulsd     -80(%rax,%r9), %xmm11                         #6452.32
        movdqa    %xmm7, %xmm3                                  #6454.26
        movq      -24(%rax,%r9), %xmm4                          #6427.380
        addsd     %xmm0, %xmm3                                  #6454.26
        mulsd     %xmm14, %xmm4                                 #6463.34
        subsd     %xmm3, %xmm7                                  #6455.26
        addsd     -32(%rax,%r9), %xmm4                          #6463.22
        addsd     %xmm0, %xmm7                                  #6457.26
        movq      -8(%rax,%r9), %xmm8                           #6429.380
        movaps    %xmm3, %xmm15                                 #6456.26
        mulsd     %xmm14, %xmm8                                 #6464.34
        addsd     %xmm11, %xmm7                                 #6459.26
        addsd     %xmm1, %xmm15                                 #6456.26
        addsd     -16(%rax,%r9), %xmm8                          #6464.22
        subsd     %xmm15, %xmm1                                 #6458.26
        movaps    %xmm14, %xmm11                                #6465.22
        movaps    %xmm2, %xmm0                                  #6476.33
        mulsd     %xmm14, %xmm11                                #6465.22
        addsd     %xmm3, %xmm1                                  #6460.26
        movq      -40(%rax,%r9), %xmm5                          #6425.380
        addsd     %xmm1, %xmm7                                  #6461.26
        mulsd     %xmm14, %xmm5                                 #6462.34
        mulsd     %xmm11, %xmm4                                 #6468.35
        addsd     %xmm7, %xmm10                                 #6470.23
        addsd     -48(%rax,%r9), %xmm5                          #6462.22
        movq      (%rax,%r9), %xmm6                             #6430.380
        addsd     %xmm5, %xmm4                                  #6468.23
        mulsd     %xmm11, %xmm6                                 #6467.34
        mulsd     %xmm11, %xmm11                                #6469.22
        addsd     %xmm8, %xmm6                                  #6467.22
        mulsd     %xmm11, %xmm6                                 #6471.35
        movq      -64(%rax,%r9), %xmm9                          #6422.383
        addsd     %xmm4, %xmm6                                  #6471.23
        addsd     %xmm10, %xmm9                                 #6472.23
        mulsd     %xmm14, %xmm6                                 #6473.35
        andnps    (%rsp), %xmm0                                 #6476.33[spill]
        addsd     %xmm9, %xmm6                                  #6473.23
        addsd     %xmm6, %xmm15                                 #6474.28
        xorps     32(%rsp), %xmm15                              #6475.28[spill]
        andps     %xmm2, %xmm15                                 #6476.82
        orps      %xmm15, %xmm0                                 #6476.22
        jmp       ..B1.2        # Prob 100%                     #6476.22
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm0
	.cfi_endproc
# mark_end;
	.type	__svml_tan1_ha_ex,@function
	.size	__svml_tan1_ha_ex,.-__svml_tan1_ha_ex
..LN__svml_tan1_ha_ex.0:
	.data
# -- End  __svml_tan1_ha_ex
	.text
.L_2__routine_start___svml_tan1_ha_e9_1:
# -- Begin  __svml_tan1_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tan1_ha_e9
# --- __svml_tan1_ha_e9(__m128d)
__svml_tan1_ha_e9:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #6487.1
        .byte     15                                            #6916.22
        .byte     30                                            #6916.22
        .byte     250                                           #6916.22
	.cfi_startproc
..___tag_value___svml_tan1_ha_e9.23:
..L24:
                                                         #6487.1
        pushq     %rbp                                          #6487.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #6487.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #6487.1
        subq      $128, %rsp                                    #6487.1
        xorl      %edx, %edx                                    #6546.9
        movsd     1024+__svml_dtan_ha_data_internal(%rip), %xmm9 #6547.23
        movaps    %xmm9, %xmm1                                  #6548.20
        andnps    %xmm0, %xmm9                                  #6549.21
        andps     %xmm0, %xmm1                                  #6548.20
        movaps    %xmm1, %xmm10                                 #6556.29
        movaps    %xmm1, %xmm2                                  #6559.18
        mulsd     1472+__svml_dtan_ha_data_internal(%rip), %xmm10 #6556.29
        movsd     1152+__svml_dtan_ha_data_internal(%rip), %xmm4 #6555.22
        movq      1408+__svml_dtan_ha_data_internal(%rip), %xmm7 #6568.31
        addsd     %xmm4, %xmm10                                 #6556.17
        movaps    %xmm10, %xmm5                                 #6557.17
        pand      %xmm7, %xmm10                                 #6569.27
        pand      .L_2il0floatpacket.26(%rip), %xmm10           #6574.27
        subsd     %xmm4, %xmm5                                  #6557.17
        movd      %xmm10, %eax                                  #6575.136
        movaps    %xmm5, %xmm8                                  #6559.40
        movaps    %xmm5, %xmm3                                  #6561.23
        mulsd     1216+__svml_dtan_ha_data_internal(%rip), %xmm8 #6559.40
        imull     $104, %eax, %ecx                              #6575.240
        mulsd     1280+__svml_dtan_ha_data_internal(%rip), %xmm3 #6561.23
        mulsd     1344+__svml_dtan_ha_data_internal(%rip), %xmm5 #6566.36
        subsd     %xmm8, %xmm2                                  #6559.18
        movaps    %xmm2, %xmm6                                  #6562.22
        lea       1632+__svml_dtan_ha_data_internal(%rip), %rax #6575.384
        movups    .L_2il0floatpacket.27(%rip), %xmm15           #6589.24
        subsd     %xmm3, %xmm6                                  #6562.22
        movaps    %xmm6, %xmm4                                  #6570.17
        movaps    %xmm15, %xmm12                                #6590.20
        movq      -96(%rcx,%rax), %xmm10                        #6575.384
        subsd     %xmm6, %xmm2                                  #6563.21
        movdqa    %xmm10, %xmm11                                #6588.22
        subsd     %xmm3, %xmm2                                  #6564.21
        movq      -56(%rax,%rcx), %xmm8                         #6580.385
        subsd     %xmm5, %xmm2                                  #6566.17
        movq      -72(%rax,%rcx), %xmm5                         #6578.385
        addsd     %xmm2, %xmm4                                  #6570.17
        movq      -8(%rax,%rcx), %xmm7                          #6586.382
        subsd     %xmm4, %xmm11                                 #6588.22
        mulsd     %xmm4, %xmm7                                  #6621.30
        subsd     %xmm4, %xmm6                                  #6571.18
        subsd     %xmm11, %xmm10                                #6591.20
        addsd     %xmm6, %xmm2                                  #6572.17
        addsd     -16(%rax,%rcx), %xmm7                         #6621.18
        subsd     %xmm4, %xmm10                                 #6592.20
        andps     %xmm11, %xmm12                                #6590.20
        subsd     %xmm2, %xmm10                                 #6593.20
        cvtpd2ps  %xmm12, %xmm14                                #6597.64
        subsd     %xmm12, %xmm11                                #6594.22
        movlhps   %xmm14, %xmm14                                #6597.48
        addsd     -88(%rax,%rcx), %xmm11                        #6595.22
        rcpps     %xmm14, %xmm13                                #6597.37
        addsd     %xmm11, %xmm10                                #6596.20
        cvtps2pd  %xmm13, %xmm11                                #6597.24
        andps     %xmm15, %xmm11                                #6598.24
        mulsd     %xmm11, %xmm12                                #6600.39
        movups    .L_2il0floatpacket.28(%rip), %xmm13           #6599.36
        movaps    %xmm13, %xmm14                                #6600.18
        movq      -24(%rax,%rcx), %xmm6                         #6584.382
        subsd     %xmm12, %xmm14                                #6600.18
        mulsd     %xmm4, %xmm6                                  #6620.30
        movaps    %xmm11, %xmm12                                #6601.32
        movaps    %xmm14, %xmm15                                #6602.30
        mulsd     %xmm14, %xmm12                                #6601.32
        addsd     -32(%rax,%rcx), %xmm6                         #6620.18
        mulsd     %xmm14, %xmm15                                #6602.30
        addsd     %xmm11, %xmm12                                #6601.20
        mulsd     -80(%rax,%rcx), %xmm11                        #6608.28
        addsd     %xmm13, %xmm15                                #6602.18
        mulsd     %xmm15, %xmm12                                #6603.24
        mulsd     %xmm12, %xmm10                                #6604.20
        movaps    %xmm10, %xmm13                                #6605.19
        mulsd     %xmm10, %xmm10                                #6606.36
        subsd     %xmm14, %xmm13                                #6605.19
        movdqa    %xmm8, %xmm14                                 #6610.22
        subsd     %xmm13, %xmm10                                #6606.24
        mulsd     %xmm4, %xmm14                                 #6610.22
        addsd     -48(%rax,%rcx), %xmm8                         #6623.18
        mulsd     %xmm12, %xmm10                                #6607.24
        mulsd     %xmm2, %xmm8                                  #6627.31
        mulsd     -80(%rax,%rcx), %xmm10                        #6609.28
        movdqa    %xmm5, %xmm13                                 #6611.22
        movq      -40(%rax,%rcx), %xmm3                         #6582.382
        addsd     %xmm14, %xmm13                                #6611.22
        mulsd     %xmm4, %xmm3                                  #6619.30
        subsd     %xmm13, %xmm5                                 #6612.22
        addsd     -48(%rax,%rcx), %xmm3                         #6619.18
        addsd     %xmm14, %xmm5                                 #6614.22
        movaps    %xmm13, %xmm12                                #6613.22
        addsd     %xmm10, %xmm5                                 #6616.22
        addsd     %xmm11, %xmm12                                #6613.22
        movaps    %xmm4, %xmm10                                 #6622.18
        subsd     %xmm12, %xmm11                                #6615.22
        mulsd     %xmm4, %xmm10                                 #6622.18
        addsd     %xmm13, %xmm11                                #6617.22
        mulsd     %xmm10, %xmm6                                 #6625.31
        addsd     %xmm11, %xmm5                                 #6618.22
        addsd     %xmm3, %xmm6                                  #6625.19
        addsd     %xmm5, %xmm8                                  #6627.19
        movq      (%rax,%rcx), %xmm11                           #6624.30
        mulsd     %xmm10, %xmm11                                #6624.30
        mulsd     %xmm10, %xmm10                                #6626.18
        addsd     %xmm7, %xmm11                                 #6624.18
        mulsd     %xmm10, %xmm11                                #6628.31
        movq      -64(%rax,%rcx), %xmm2                         #6629.19
        addsd     %xmm6, %xmm11                                 #6628.19
        addsd     %xmm8, %xmm2                                  #6629.19
        mulsd     %xmm4, %xmm11                                 #6630.31
        movups    %xmm0, 16(%rsp)                               #6487.1[spill]
        movaps    %xmm1, %xmm0                                  #6551.34
        movups    %xmm9, 32(%rsp)                               #6549.21[spill]
        addsd     %xmm2, %xmm11                                 #6630.19
        cmpnlesd  28160+__svml_dtan_ha_data_internal(%rip), %xmm0 #6551.34
        addsd     %xmm11, %xmm12                                #6631.18
        movmskpd  %xmm0, %esi                                   #6553.82
        pxor      %xmm9, %xmm12                                 #6632.18
        testl     $1, %esi                                      #6633.59
        jne       ..B2.8        # Prob 5%                       #6633.70
                                # LOE rax rbx r12 r13 r14 r15 edx xmm0 xmm1 xmm12
..B2.2:                         # Preds ..B2.8 ..B2.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #6919.52
        jne       ..B2.4        # Prob 5%                       #6919.52
                                # LOE rbx r12 r13 r14 r15 edx xmm12
..B2.3:                         # Preds ..B2.5 ..B2.2
                                # Execution count [1.00e+00]
        movaps    %xmm12, %xmm0                                 #6922.12
        movq      %rbp, %rsp                                    #6922.12
        popq      %rbp                                          #6922.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #6922.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.4:                         # Preds ..B2.2
                                # Execution count [5.00e-02]: Infreq
        movups    16(%rsp), %xmm1                               #6919.160[spill]
        movsd     %xmm1, (%rsp)                                 #6919.160
        movsd     %xmm12, 64(%rsp)                              #6919.233
        jne       ..B2.6        # Prob 5%                       #6919.374
                                # LOE rbx r12 r13 r14 r15 edx
..B2.5:                         # Preds ..B2.7 ..B2.6 ..B2.4
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm12                              #6919.625
        jmp       ..B2.3        # Prob 100%                     #6919.625
                                # LOE rbx r12 r13 r14 r15 xmm12
..B2.6:                         # Preds ..B2.4
                                # Execution count [2.50e-03]: Infreq
        je        ..B2.5        # Prob 95%                      #6919.517
                                # LOE rbx r12 r13 r14 r15
..B2.7:                         # Preds ..B2.6
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #6919.546
        lea       64(%rsp), %rsi                                #6919.546
..___tag_value___svml_tan1_ha_e9.35:
#       __svml_dtan_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtan_ha_cout_rare_internal             #6919.546
..___tag_value___svml_tan1_ha_e9.36:
        jmp       ..B2.5        # Prob 100%                     #6919.546
                                # LOE rbx r12 r13 r14 r15
..B2.8:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        movdqu    .L_2il0floatpacket.29(%rip), %xmm10           #6726.22
        movaps    %xmm1, %xmm8                                  #6637.21
        pand      %xmm1, %xmm10                                 #6726.22
        lea       __svml_dtan_ha_reduction_data_internal(%rip), %rdi #6728.383
        psrlq     $52, %xmm10                                   #6727.22
        movd      %xmm10, %ecx                                  #6728.124
        movdqu    .L_2il0floatpacket.30(%rip), %xmm15           #6736.25
        pand      %xmm1, %xmm15                                 #6736.25
        paddq     .L_2il0floatpacket.31(%rip), %xmm15           #6737.25
        movdqa    %xmm15, %xmm4                                 #6745.16
        lea       (%rcx,%rcx,2), %esi                           #6728.230
        shll      $3, %esi                                      #6728.230
        psrlq     $32, %xmm4                                    #6745.16
        movq      16(%rdi,%rsi), %xmm11                         #6730.383
        movdqa    %xmm4, %xmm2                                  #6751.17
        movups    %xmm12, (%rsp)                                #[spill]
        movdqa    %xmm11, %xmm3                                 #6743.16
        movdqu    .L_2il0floatpacket.32(%rip), %xmm12           #6738.21
        psrlq     $32, %xmm3                                    #6743.16
        movsd     1088+__svml_dtan_ha_data_internal(%rip), %xmm6 #6636.28
        pand      %xmm12, %xmm15                                #6746.16
        andps     %xmm6, %xmm8                                  #6637.21
        movdqa    %xmm15, %xmm7                                 #6756.17
        movq      8(%rdi,%rsi), %xmm9                           #6729.384
        pand      %xmm12, %xmm11                                #6744.16
        pmuludq   %xmm3, %xmm2                                  #6751.17
        cmpeqsd   %xmm6, %xmm8                                  #6638.29
        pmuludq   %xmm15, %xmm3                                 #6757.17
        pmuludq   %xmm4, %xmm11                                 #6752.17
        movmskpd  %xmm8, %edx                                   #6640.52
        movdqa    %xmm9, %xmm10                                 #6741.16
        pand      %xmm12, %xmm9                                 #6742.16
        pmuludq   %xmm9, %xmm7                                  #6756.17
        psrlq     $32, %xmm3                                    #6759.17
        pmuludq   %xmm4, %xmm9                                  #6750.17
        paddq     %xmm3, %xmm2                                  #6763.17
        psrlq     $32, %xmm10                                   #6741.16
        movdqa    %xmm15, %xmm8                                 #6755.17
        movdqa    %xmm12, %xmm6                                 #6767.17
        movdqa    %xmm12, %xmm5                                 #6740.16
        pmuludq   %xmm10, %xmm8                                 #6755.17
        pand      %xmm7, %xmm6                                  #6767.17
        pmuludq   %xmm4, %xmm10                                 #6749.17
        paddq     %xmm2, %xmm6                                  #6772.17
        movq      (%rsi,%rdi), %xmm14                           #6728.383
        psrlq     $32, %xmm11                                   #6777.17
        psrlq     $32, %xmm7                                    #6760.17
        pand      %xmm14, %xmm5                                 #6740.16
        movdqa    %xmm15, %xmm13                                #6754.17
        movdqa    %xmm12, %xmm3                                 #6768.17
        paddq     %xmm6, %xmm11                                 #6778.17
        paddq     %xmm7, %xmm9                                  #6764.17
        pmuludq   %xmm5, %xmm13                                 #6754.17
        pmuludq   %xmm5, %xmm4                                  #6748.17
        pand      %xmm8, %xmm3                                  #6768.17
        movdqa    %xmm11, %xmm2                                 #6779.17
        paddq     %xmm9, %xmm3                                  #6773.17
        psrlq     $32, %xmm2                                    #6779.17
        psrlq     $32, %xmm8                                    #6761.17
        psrlq     $32, %xmm14                                   #6739.16
        movdqa    %xmm12, %xmm7                                 #6769.17
        paddq     %xmm3, %xmm2                                  #6780.17
        paddq     %xmm8, %xmm10                                 #6765.17
        pmuludq   %xmm14, %xmm15                                #6753.17
        pand      %xmm13, %xmm7                                 #6769.17
        movdqa    %xmm2, %xmm9                                  #6781.17
        paddq     %xmm10, %xmm7                                 #6774.17
        psrlq     $32, %xmm9                                    #6781.17
        psrlq     $32, %xmm13                                   #6762.17
        paddq     %xmm7, %xmm9                                  #6782.17
        paddq     %xmm13, %xmm4                                 #6766.17
        pand      %xmm12, %xmm15                                #6770.17
        movdqa    %xmm9, %xmm6                                  #6783.17
        paddq     %xmm4, %xmm15                                 #6775.17
        psrlq     $32, %xmm6                                    #6783.17
        pand      %xmm12, %xmm9                                 #6786.17
        paddq     %xmm15, %xmm6                                 #6784.17
        psllq     $32, %xmm6                                    #6787.23
        pand      %xmm12, %xmm11                                #6785.17
        paddq     %xmm9, %xmm6                                  #6789.23
        movups    .L_2il0floatpacket.33(%rip), %xmm7            #6792.21
        psllq     $32, %xmm2                                    #6788.23
        movups    .L_2il0floatpacket.28(%rip), %xmm12           #6794.26
        andps     %xmm1, %xmm7                                  #6792.21
        movdqa    %xmm6, %xmm8                                  #6795.24
        movaps    %xmm12, %xmm5                                 #6794.26
        paddq     %xmm11, %xmm2                                 #6790.23
        movups    .L_2il0floatpacket.34(%rip), %xmm11           #6798.15
        psrlq     $12, %xmm8                                    #6795.24
        pxor      %xmm7, %xmm5                                  #6794.26
        andl      $1, %edx                                      #6640.106
        orps      %xmm5, %xmm8                                  #6796.24
        movaps    %xmm11, %xmm5                                 #6799.19
        movdqu    .L_2il0floatpacket.38(%rip), %xmm14           #6817.24
        addsd     %xmm8, %xmm5                                  #6799.19
        movaps    %xmm5, %xmm13                                 #6800.9
        pand      %xmm6, %xmm14                                 #6817.24
        movdqu    .L_2il0floatpacket.36(%rip), %xmm10           #6808.24
        psllq     $40, %xmm14                                   #6818.24
        movups    .L_2il0floatpacket.35(%rip), %xmm4            #6806.26
        pand      %xmm2, %xmm10                                 #6808.24
        psrlq     $24, %xmm2                                    #6819.23
        pxor      %xmm7, %xmm4                                  #6806.26
        xorps     .L_2il0floatpacket.37(%rip), %xmm7            #6815.26
        por       %xmm2, %xmm14                                 #6820.24
        orps      %xmm7, %xmm14                                 #6821.24
        psllq     $28, %xmm10                                   #6809.24
        orps      %xmm4, %xmm10                                 #6810.24
        subsd     %xmm11, %xmm13                                #6800.9
        subsd     %xmm7, %xmm14                                 #6824.18
        subsd     %xmm4, %xmm10                                 #6813.17
        subsd     %xmm13, %xmm8                                 #6801.17
        movaps    %xmm8, %xmm13                                 #6825.11
        movups    .L_2il0floatpacket.27(%rip), %xmm3            #6833.16
        addsd     %xmm14, %xmm13                                #6825.11
        movaps    %xmm3, %xmm15                                 #6833.16
        subsd     %xmm13, %xmm8                                 #6826.17
        andps     %xmm13, %xmm15                                #6833.16
        addsd     %xmm8, %xmm14                                 #6827.18
        subsd     %xmm15, %xmm13                                #6834.17
        addsd     %xmm10, %xmm14                                #6828.11
        movups    .L_2il0floatpacket.41(%rip), %xmm9            #6831.17
        movups    .L_2il0floatpacket.40(%rip), %xmm6            #6830.16
        movaps    %xmm9, %xmm2                                  #6837.12
        movups    .L_2il0floatpacket.39(%rip), %xmm7            #6838.12
        movaps    %xmm6, %xmm4                                  #6835.16
        mulsd     %xmm13, %xmm6                                 #6836.12
        mulsd     %xmm15, %xmm2                                 #6837.12
        mulsd     %xmm14, %xmm7                                 #6838.12
        mulsd     %xmm13, %xmm9                                 #6839.12
        addsd     %xmm2, %xmm6                                  #6840.13
        mulsd     %xmm15, %xmm4                                 #6835.16
        addsd     %xmm9, %xmm7                                  #6841.13
        pand      .L_2il0floatpacket.44(%rip), %xmm5            #6804.19
        movaps    %xmm4, %xmm10                                 #6843.13
        pand      .L_2il0floatpacket.26(%rip), %xmm5            #6857.31
        movaps    %xmm3, %xmm15                                 #6873.24
        movups    .L_2il0floatpacket.43(%rip), %xmm11           #6847.16
        addsd     %xmm7, %xmm6                                  #6842.13
        movd      %xmm5, %r8d                                   #6858.140
        movups    .L_2il0floatpacket.42(%rip), %xmm14           #6848.11
        movaps    %xmm11, %xmm8                                 #6849.17
        andps     %xmm1, %xmm14                                 #6848.11
        addsd     %xmm6, %xmm10                                 #6843.13
        imull     $104, %r8d, %r9d                              #6858.244
        cmpltsd   %xmm14, %xmm8                                 #6849.17
        subsd     %xmm10, %xmm4                                 #6844.16
        movaps    %xmm14, %xmm13                                #6849.17
        cmplesd   %xmm11, %xmm14                                #6850.20
        addsd     %xmm6, %xmm4                                  #6845.13
        movsd     %xmm8, %xmm13                                 #6849.17
        andps     %xmm1, %xmm14                                 #6851.24
        movq      -96(%r9,%rax), %xmm11                         #6858.388
        andps     %xmm13, %xmm10                                #6852.25
        orps      %xmm10, %xmm14                                #6853.9
        movdqa    %xmm11, %xmm1                                 #6871.26
        andps     %xmm4, %xmm13                                 #6854.9
        subsd     %xmm14, %xmm1                                 #6871.26
        andps     %xmm1, %xmm15                                 #6873.24
        subsd     %xmm1, %xmm11                                 #6874.24
        subsd     %xmm15, %xmm1                                 #6877.26
        cvtpd2ps  %xmm15, %xmm2                                 #6880.68
        subsd     %xmm14, %xmm11                                #6875.24
        addsd     -88(%rax,%r9), %xmm1                          #6878.26
        subsd     %xmm13, %xmm11                                #6876.24
        movlhps   %xmm2, %xmm2                                  #6880.52
        addsd     %xmm1, %xmm11                                 #6879.24
        rcpps     %xmm2, %xmm1                                  #6880.41
        cvtps2pd  %xmm1, %xmm1                                  #6880.28
        andps     %xmm3, %xmm1                                  #6881.28
        movaps    %xmm12, %xmm2                                 #6883.22
        mulsd     %xmm1, %xmm15                                 #6883.43
        movaps    %xmm1, %xmm3                                  #6884.36
        movq      -56(%rax,%r9), %xmm10                         #6863.389
        subsd     %xmm15, %xmm2                                 #6883.22
        movaps    %xmm2, %xmm15                                 #6885.34
        mulsd     %xmm2, %xmm3                                  #6884.36
        mulsd     %xmm2, %xmm15                                 #6885.34
        addsd     %xmm1, %xmm3                                  #6884.24
        mulsd     -80(%rax,%r9), %xmm1                          #6891.32
        addsd     %xmm12, %xmm15                                #6885.22
        mulsd     %xmm15, %xmm3                                 #6886.28
        mulsd     %xmm3, %xmm11                                 #6887.24
        movaps    %xmm11, %xmm12                                #6888.23
        mulsd     %xmm11, %xmm11                                #6889.40
        subsd     %xmm2, %xmm12                                 #6888.23
        movdqa    %xmm10, %xmm2                                 #6893.26
        subsd     %xmm12, %xmm11                                #6889.28
        mulsd     %xmm14, %xmm2                                 #6893.26
        addsd     -48(%rax,%r9), %xmm10                         #6906.22
        mulsd     %xmm3, %xmm11                                 #6890.28
        mulsd     %xmm13, %xmm10                                #6910.35
        mulsd     -80(%rax,%r9), %xmm11                         #6892.32
        movq      -72(%rax,%r9), %xmm7                          #6861.389
        movdqa    %xmm7, %xmm3                                  #6894.26
        movq      -24(%rax,%r9), %xmm4                          #6867.386
        addsd     %xmm2, %xmm3                                  #6894.26
        mulsd     %xmm14, %xmm4                                 #6903.34
        subsd     %xmm3, %xmm7                                  #6895.26
        addsd     -32(%rax,%r9), %xmm4                          #6903.22
        addsd     %xmm2, %xmm7                                  #6897.26
        movq      -8(%rax,%r9), %xmm8                           #6869.386
        movaps    %xmm3, %xmm15                                 #6896.26
        mulsd     %xmm14, %xmm8                                 #6904.34
        addsd     %xmm11, %xmm7                                 #6899.26
        addsd     %xmm1, %xmm15                                 #6896.26
        addsd     -16(%rax,%r9), %xmm8                          #6904.22
        subsd     %xmm15, %xmm1                                 #6898.26
        movaps    %xmm14, %xmm11                                #6905.22
        addsd     %xmm3, %xmm1                                  #6900.26
        mulsd     %xmm14, %xmm11                                #6905.22
        addsd     %xmm1, %xmm7                                  #6901.26
        mulsd     %xmm11, %xmm4                                 #6908.35
        addsd     %xmm7, %xmm10                                 #6910.23
        movq      -40(%rax,%r9), %xmm5                          #6865.386
        movq      (%rax,%r9), %xmm6                             #6870.386
        mulsd     %xmm14, %xmm5                                 #6902.34
        mulsd     %xmm11, %xmm6                                 #6907.34
        mulsd     %xmm11, %xmm11                                #6909.22
        addsd     -48(%rax,%r9), %xmm5                          #6902.22
        addsd     %xmm8, %xmm6                                  #6907.22
        addsd     %xmm5, %xmm4                                  #6908.23
        mulsd     %xmm11, %xmm6                                 #6911.35
        movq      -64(%rax,%r9), %xmm9                          #6862.389
        addsd     %xmm4, %xmm6                                  #6911.23
        addsd     %xmm10, %xmm9                                 #6912.23
        mulsd     %xmm14, %xmm6                                 #6913.35
        movups    (%rsp), %xmm12                                #6916.22[spill]
        addsd     %xmm9, %xmm6                                  #6913.23
        addsd     %xmm6, %xmm15                                 #6914.28
        xorps     32(%rsp), %xmm15                              #6915.28[spill]
        blendvpd  %xmm0, %xmm15, %xmm12                         #6916.22
        jmp       ..B2.2        # Prob 100%                     #6916.22
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm12
	.cfi_endproc
# mark_end;
	.type	__svml_tan1_ha_e9,@function
	.size	__svml_tan1_ha_e9,.-__svml_tan1_ha_e9
..LN__svml_tan1_ha_e9.1:
	.data
# -- End  __svml_tan1_ha_e9
	.text
.L_2__routine_start___svml_tan4_ha_e9_2:
# -- Begin  __svml_tan4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tan4_ha_e9
# --- __svml_tan4_ha_e9(__m256d)
__svml_tan4_ha_e9:
# parameter 1: %ymm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #6927.1
        .byte     15                                            #7356.22
        .byte     30                                            #7356.22
        .byte     250                                           #7356.22
	.cfi_startproc
..___tag_value___svml_tan4_ha_e9.41:
..L42:
                                                         #6927.1
        pushq     %rbp                                          #6927.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #6927.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #6927.1
        subq      $448, %rsp                                    #6927.1
        lea       1632+__svml_dtan_ha_data_internal(%rip), %rsi #7015.625
        vmovupd   1024+__svml_dtan_ha_data_internal(%rip), %ymm15 #6987.57
        xorl      %eax, %eax                                    #6986.9
        vmovupd   1152+__svml_dtan_ha_data_internal(%rip), %ymm9 #6995.56
        vmovupd   1408+__svml_dtan_ha_data_internal(%rip), %xmm1 #7008.31
        vmovupd   %ymm0, 320(%rsp)                              #6927.1[spill]
        vandpd    %ymm15, %ymm0, %ymm5                          #6988.20
        vmulpd    1472+__svml_dtan_ha_data_internal(%rip), %ymm5, %ymm10 #6996.32
        vmovupd   %ymm5, 416(%rsp)                              #6988.20[spill]
        vcmpnle_uqpd 28160+__svml_dtan_ha_data_internal(%rip), %ymm5, %ymm12 #6991.34
        vaddpd    %ymm10, %ymm9, %ymm10                         #6996.17
        vmovupd   %ymm12, 352(%rsp)                             #6991.34[spill]
        vandnpd   %ymm0, %ymm15, %ymm13                         #6989.21
        vmovupd   %ymm13, 384(%rsp)                             #6989.21[spill]
        vextractf128 $1, %ymm12, %xmm11                         #6992.159
        vshufps   $221, %xmm11, %xmm12, %xmm14                  #6993.99
        vsubpd    %ymm9, %ymm10, %ymm12                         #6997.17
        vmovups   %xmm14, 96(%rsp)                              #6993.99[spill]
        vmulpd    1216+__svml_dtan_ha_data_internal(%rip), %ymm12, %ymm15 #6999.43
        vmulpd    1280+__svml_dtan_ha_data_internal(%rip), %ymm12, %ymm13 #7001.23
        vmulpd    1344+__svml_dtan_ha_data_internal(%rip), %ymm12, %ymm14 #7006.39
        vsubpd    %ymm15, %ymm5, %ymm7                          #6999.18
        vsubpd    %ymm13, %ymm7, %ymm9                          #7002.22
        vsubpd    %ymm9, %ymm7, %ymm8                           #7003.21
        vmovdqu   .L_2il0floatpacket.26(%rip), %xmm7            #7013.37
        vsubpd    %ymm13, %ymm8, %ymm11                         #7004.21
        vsubpd    %ymm14, %ymm11, %ymm4                         #7006.17
        vaddpd    %ymm4, %ymm9, %ymm15                          #7010.17
        vsubpd    %ymm15, %ymm9, %ymm6                          #7011.18
        vaddpd    %ymm6, %ymm4, %ymm5                           #7012.17
        vmovupd   %ymm5, (%rsp)                                 #7012.17[spill]
        vandps    %xmm1, %xmm10, %xmm3                          #7009.27
        vextractf128 $1, %ymm10, %xmm2                          #7007.108
        vpand     %xmm7, %xmm3, %xmm8                           #7014.27
        vmovd     %xmm8, %edi                                   #7015.136
        vandps    %xmm1, %xmm2, %xmm0                           #7009.96
        vpand     %xmm7, %xmm0, %xmm11                          #7014.106
        vmovd     %xmm11, %r9d                                  #7015.263
        imull     $104, %edi, %ecx                              #7015.433
        imull     $104, %r9d, %r10d                             #7015.537
        vmovq     -96(%rcx,%rsi), %xmm13                        #7015.1137
        vmovq     -88(%rcx,%rsi), %xmm9                         #7016.1137
        vpextrd   $2, %xmm8, %r8d                               #7015.198
        vpextrd   $2, %xmm11, %r11d                             #7015.324
        imull     $104, %r8d, %edx                              #7015.485
        imull     $104, %r11d, %r11d                            #7015.589
        vmovq     -96(%r10,%rsi), %xmm14                        #7015.1337
        vmovq     -88(%r10,%rsi), %xmm3                         #7016.1337
        vmovq     -80(%rcx,%rsi), %xmm1                         #7017.1136
        vmovhpd   -96(%rdx,%rsi), %xmm13, %xmm12                #7015.1105
        vmovhpd   -96(%r11,%rsi), %xmm14, %xmm10                #7015.1305
        vmovhpd   -88(%rdx,%rsi), %xmm9, %xmm6                  #7016.1105
        vmovhpd   -88(%r11,%rsi), %xmm3, %xmm2                  #7016.1305
        vmovq     -80(%r10,%rsi), %xmm8                         #7017.1336
        vmovq     -72(%r10,%rsi), %xmm14                        #7018.1338
        vmovq     -64(%rcx,%rsi), %xmm9                         #7019.1138
        vmovhpd   -80(%rdx,%rsi), %xmm1, %xmm7                  #7017.1104
        vmovhpd   -80(%r11,%rsi), %xmm8, %xmm13                 #7017.1304
        vmovhpd   -64(%rdx,%rsi), %xmm9, %xmm3                  #7019.1106
        vinsertf128 $1, %xmm10, %ymm12, %ymm4                   #7015.1059
        vmovq     -72(%rcx,%rsi), %xmm12                        #7018.1138
        vmovhpd   -72(%rdx,%rsi), %xmm12, %xmm11                #7018.1106
        vmovhpd   -72(%r11,%rsi), %xmm14, %xmm10                #7018.1306
        vinsertf128 $1, %xmm2, %ymm6, %ymm0                     #7016.1059
        vmovq     -64(%r10,%rsi), %xmm2                         #7019.1338
        vmovhpd   -64(%r11,%rsi), %xmm2, %xmm1                  #7019.1306
        vinsertf128 $1, %xmm13, %ymm7, %ymm6                    #7017.1058
        vinsertf128 $1, %xmm10, %ymm11, %ymm7                   #7018.1060
        vmovq     -56(%rcx,%rsi), %xmm13                        #7020.1138
        vmovq     -56(%r10,%rsi), %xmm11                        #7020.1338
        vmovq     -48(%rcx,%rsi), %xmm10                        #7021.1138
        vmovhpd   -56(%rdx,%rsi), %xmm13, %xmm12                #7020.1106
        vmovhpd   -56(%r11,%rsi), %xmm11, %xmm14                #7020.1306
        vmovq     -40(%r10,%rsi), %xmm11                        #7022.1335
        vmovhpd   -48(%rdx,%rsi), %xmm10, %xmm9                 #7021.1106
        vmovq     -32(%rcx,%rsi), %xmm10                        #7023.1135
        vinsertf128 $1, %xmm1, %ymm3, %ymm8                     #7019.1060
        vmovq     -40(%rcx,%rsi), %xmm1                         #7022.1135
        vmovq     -48(%r10,%rsi), %xmm3                         #7021.1338
        vmovups   %ymm8, 32(%rsp)                               #7019.1060[spill]
        vmovhpd   -48(%r11,%rsi), %xmm3, %xmm2                  #7021.1306
        vmovhpd   -40(%rdx,%rsi), %xmm1, %xmm8                  #7022.1103
        vmovq     -32(%r10,%rsi), %xmm3                         #7023.1335
        vmovq     -24(%rcx,%rsi), %xmm1                         #7024.1135
        vinsertf128 $1, %xmm14, %ymm12, %ymm13                  #7020.1060
        vmovhpd   -40(%r11,%rsi), %xmm11, %xmm14                #7022.1303
        vmovhpd   -24(%rdx,%rsi), %xmm1, %xmm11                 #7024.1103
        vmovq     -16(%r10,%rsi), %xmm1                         #7025.1337
        vinsertf128 $1, %xmm2, %ymm9, %ymm12                    #7021.1060
        vmovhpd   -32(%rdx,%rsi), %xmm10, %xmm9                 #7023.1103
        vmovhpd   -32(%r11,%rsi), %xmm3, %xmm2                  #7023.1303
        vmovq     -16(%rcx,%rsi), %xmm3                         #7025.1135
        vinsertf128 $1, %xmm14, %ymm8, %ymm8                    #7022.1057
        vmovq     -24(%r10,%rsi), %xmm14                        #7024.1335
        vmulpd    %ymm15, %ymm8, %ymm8                          #7059.33
        vaddpd    %ymm8, %ymm12, %ymm8                          #7059.18
        vinsertf128 $1, %xmm2, %ymm9, %ymm10                    #7023.1057
        vmovhpd   -24(%r11,%rsi), %xmm14, %xmm9                 #7024.1303
        vmovq     -8(%rcx,%rsi), %xmm14                         #7026.1135
        vmovhpd   -16(%rdx,%rsi), %xmm3, %xmm2                  #7025.1103
        vmovhpd   -8(%rdx,%rsi), %xmm14, %xmm3                  #7026.1103
        vinsertf128 $1, %xmm9, %ymm11, %ymm9                    #7024.1057
        vmovhpd   -16(%r11,%rsi), %xmm1, %xmm11                 #7025.1305
        vmovq     -8(%r10,%rsi), %xmm1                          #7026.1337
        vmovhpd   -8(%r11,%rsi), %xmm1, %xmm14                  #7026.1305
        vmulpd    %ymm15, %ymm9, %ymm9                          #7060.33
        vaddpd    %ymm9, %ymm10, %ymm10                         #7060.18
        vinsertf128 $1, %xmm11, %ymm2, %ymm11                   #7025.1057
        vmovq     (%rcx,%rsi), %xmm2                            #7027.1135
        vmovhpd   (%rdx,%rsi), %xmm2, %xmm1                     #7027.1103
        vinsertf128 $1, %xmm14, %ymm3, %ymm14                   #7026.1057
        vmovq     (%r10,%rsi), %xmm3                            #7027.1337
        vmovhpd   (%r11,%rsi), %xmm3, %xmm2                     #7027.1305
        vmulpd    %ymm15, %ymm14, %ymm14                        #7061.33
        vaddpd    %ymm14, %ymm11, %ymm11                        #7061.18
        vinsertf128 $1, %xmm2, %ymm1, %ymm3                     #7027.1057
        vsubpd    %ymm15, %ymm4, %ymm1                          #7028.22
        vmovupd   .L_2il0floatpacket.45(%rip), %ymm2            #7029.24
        vmovups   %ymm3, 64(%rsp)                               #7027.1057[spill]
        vsubpd    %ymm1, %ymm4, %ymm4                           #7031.20
        vsubpd    %ymm15, %ymm4, %ymm4                          #7032.20
        vandpd    %ymm2, %ymm1, %ymm3                           #7030.20
        vsubpd    %ymm5, %ymm4, %ymm5                           #7033.20
        vsubpd    %ymm3, %ymm1, %ymm1                           #7034.22
        vcvtpd2ps %ymm3, %xmm4                                  #7037.51
        vaddpd    %ymm1, %ymm0, %ymm0                           #7035.22
        vrcpps    %xmm4, %xmm1                                  #7037.40
        vaddpd    %ymm0, %ymm5, %ymm5                           #7036.20
        vcvtps2pd %xmm1, %ymm0                                  #7037.24
        vandpd    %ymm2, %ymm0, %ymm4                           #7038.24
        vmovupd   .L_2il0floatpacket.46(%rip), %ymm2            #7039.19
        vmulpd    %ymm4, %ymm3, %ymm3                           #7040.42
        vsubpd    %ymm3, %ymm2, %ymm1                           #7040.18
        vmulpd    %ymm1, %ymm4, %ymm3                           #7041.35
        vaddpd    %ymm3, %ymm4, %ymm0                           #7041.20
        vmulpd    %ymm1, %ymm1, %ymm3                           #7042.33
        vaddpd    %ymm3, %ymm2, %ymm2                           #7042.18
        vmulpd    %ymm2, %ymm0, %ymm2                           #7043.24
        vmulpd    %ymm2, %ymm5, %ymm5                           #7044.20
        vsubpd    %ymm1, %ymm5, %ymm3                           #7045.19
        vmulpd    %ymm5, %ymm5, %ymm5                           #7046.39
        vsubpd    %ymm3, %ymm5, %ymm0                           #7046.24
        vmulpd    %ymm4, %ymm6, %ymm3                           #7048.28
        vmulpd    %ymm0, %ymm2, %ymm1                           #7047.24
        vmulpd    %ymm15, %ymm13, %ymm2                         #7050.22
        vmulpd    %ymm1, %ymm6, %ymm6                           #7049.28
        vaddpd    %ymm2, %ymm7, %ymm4                           #7051.22
        vaddpd    %ymm12, %ymm13, %ymm1                         #7063.18
        vsubpd    %ymm4, %ymm7, %ymm7                           #7052.22
        vaddpd    %ymm4, %ymm3, %ymm5                           #7053.22
        vaddpd    %ymm7, %ymm2, %ymm7                           #7054.22
        vsubpd    %ymm5, %ymm3, %ymm0                           #7055.22
        vaddpd    %ymm7, %ymm6, %ymm6                           #7056.22
        vaddpd    %ymm0, %ymm4, %ymm7                           #7057.22
        vmulpd    %ymm15, %ymm15, %ymm0                         #7062.18
        vaddpd    %ymm7, %ymm6, %ymm7                           #7058.22
        vmulpd    64(%rsp), %ymm0, %ymm12                       #7064.33[spill]
        vmulpd    %ymm0, %ymm0, %ymm3                           #7066.18
        vmulpd    %ymm0, %ymm10, %ymm13                         #7065.34
        vaddpd    %ymm12, %ymm11, %ymm2                         #7064.18
        vaddpd    %ymm13, %ymm8, %ymm4                          #7065.19
        vmulpd    %ymm3, %ymm2, %ymm6                           #7068.34
        vmulpd    (%rsp), %ymm1, %ymm8                          #7067.34[spill]
        vmovups   96(%rsp), %xmm2                               #6993.82[spill]
        vmovmskps %xmm2, %edx                                   #6993.82
        vaddpd    %ymm6, %ymm4, %ymm9                           #7068.19
        vaddpd    %ymm8, %ymm7, %ymm7                           #7067.19
        vmulpd    %ymm9, %ymm15, %ymm15                         #7070.34
        vaddpd    32(%rsp), %ymm7, %ymm10                       #7069.19[spill]
        vmovupd   416(%rsp), %ymm7                              #7073.70[spill]
        vaddpd    %ymm15, %ymm10, %ymm0                         #7070.19
        vaddpd    %ymm0, %ymm5, %ymm1                           #7071.18
        vxorpd    384(%rsp), %ymm1, %ymm0                       #7072.18[spill]
        testl     %edx, %edx                                    #7073.70
        jne       ..B3.12       # Prob 5%                       #7073.70
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm7
..B3.2:                         # Preds ..B3.12 ..B3.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #7359.52
        jne       ..B3.4        # Prob 5%                       #7359.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B3.3:                         # Preds ..B3.4 ..B3.10 ..B3.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #7362.12
        popq      %rbp                                          #7362.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #7362.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.4:                         # Preds ..B3.2
                                # Execution count [5.00e-02]: Infreq
        vmovupd   320(%rsp), %ymm1                              #7359.200[spill]
        vmovupd   %ymm0, 128(%rsp)                              #7359.276
        vmovupd   %ymm1, 64(%rsp)                               #7359.200
        je        ..B3.3        # Prob 95%                      #7359.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B3.7:                         # Preds ..B3.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #7359.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B3.15:                        # Preds ..B3.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xfe, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xfe, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.9 ..B3.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #7359.523
        jc        ..B3.11       # Prob 5%                       #7359.523
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.11 ..B3.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #7359.476
        cmpl      $4, %r12d                                     #7359.471
        jl        ..B3.8        # Prob 82%                      #7359.471
                                # LOE rbx r12 r14 r15 r13d
..B3.10:                        # Preds ..B3.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #7359.682
        jmp       ..B3.3        # Prob 100%                     #7359.682
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xfe, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xfe, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B3.11:                        # Preds ..B3.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #7359.552
        lea       128(%rsp,%r12,8), %rsi                        #7359.552
..___tag_value___svml_tan4_ha_e9.74:
#       __svml_dtan_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtan_ha_cout_rare_internal             #7359.552
..___tag_value___svml_tan4_ha_e9.75:
        jmp       ..B3.9        # Prob 100%                     #7359.552
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B3.12:                        # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   1088+__svml_dtan_ha_data_internal(%rip), %ymm14 #7076.62
        lea       __svml_dtan_ha_reduction_data_internal(%rip), %r9 #7168.621
        vmovupd   %ymm7, 416(%rsp)                              #[spill]
        vmovdqu   .L_2il0floatpacket.29(%rip), %xmm6            #7165.22
        vmovupd   %ymm0, (%rsp)                                 #[spill]
        vandpd    %ymm7, %ymm14, %ymm12                         #7077.21
        vcmpeqpd  %ymm14, %ymm12, %ymm8                         #7078.29
        vpand     %xmm7, %xmm6, %xmm15                          #7166.22
        vextractf128 $1, %ymm7, %xmm11                          #7164.111
        vpsrlq    $52, %xmm15, %xmm0                            #7167.22
        vmovd     %xmm0, %r10d                                  #7168.124
        vmovups   %xmm7, 128(%rsp)                              #7164.37[spill]
        vpand     %xmm11, %xmm6, %xmm3                          #7166.97
        vpsrlq    $52, %xmm3, %xmm1                             #7167.85
        vmovd     %xmm1, %ecx                                   #7168.257
        lea       (%r10,%r10,2), %r8d                           #7168.432
        vmovups   %xmm11, 144(%rsp)                             #7164.111[spill]
        vmovdqu   .L_2il0floatpacket.31(%rip), %xmm15           #7175.24
        vpextrd   $2, %xmm0, %r11d                              #7168.189
        vpextrd   $2, %xmm1, %edx                               #7168.321
        lea       (%rcx,%rcx,2), %esi                           #7168.534
        shll      $3, %r8d                                      #7168.432
        shll      $3, %esi                                      #7168.534
        vmovq     16(%r8,%r9), %xmm14                           #7170.1169
        lea       (%r11,%r11,2), %edi                           #7168.483
        shll      $3, %edi                                      #7168.483
        lea       (%rdx,%rdx,2), %r10d                          #7168.585
        shll      $3, %r10d                                     #7168.585
        vmovq     8(%r8,%r9), %xmm9                             #7169.1170
        vmovq     (%rsi,%r9), %xmm4                             #7168.1369
        vmovhpd   16(%rdi,%r9), %xmm14, %xmm1                   #7170.1137
        vmovhpd   8(%rdi,%r9), %xmm9, %xmm9                     #7169.1138
        vmovq     (%r8,%r9), %xmm5                              #7168.1169
        vmovq     8(%rsi,%r9), %xmm12                           #7169.1370
        vmovhpd   (%rdi,%r9), %xmm5, %xmm2                      #7168.1137
        vmovupd   %xmm2, 160(%rsp)                              #7168.1137[spill]
        vextractf128 $1, %ymm8, %xmm13                          #7079.136
        vshufps   $221, %xmm13, %xmm8, %xmm10                   #7080.69
        vmovmskps %xmm10, %eax                                  #7080.52
        vmovdqu   .L_2il0floatpacket.30(%rip), %xmm10           #7174.25
        vpand     %xmm7, %xmm10, %xmm7                          #7176.25
        vpand     %xmm11, %xmm10, %xmm11                        #7176.106
        vpaddq    %xmm15, %xmm7, %xmm14                         #7177.25
        vpaddq    %xmm15, %xmm11, %xmm6                         #7177.111
        vmovhpd   (%r10,%r9), %xmm4, %xmm13                     #7168.1337
        vpsrlq    $32, %xmm14, %xmm10                           #7185.16
        vmovq     16(%rsi,%r9), %xmm8                           #7170.1369
        lea       1632+__svml_dtan_ha_data_internal(%rip), %rsi #7298.629
        vmovdqu   .L_2il0floatpacket.32(%rip), %xmm15           #7178.21
        vmovhpd   8(%r10,%r9), %xmm12, %xmm4                    #7169.1338
        vpand     %xmm15, %xmm13, %xmm12                        #7180.83
        vmovhpd   16(%r10,%r9), %xmm8, %xmm3                    #7170.1337
        vpsrlq    $32, %xmm9, %xmm8                             #7181.16
        vpand     %xmm15, %xmm9, %xmm5                          #7182.16
        vpand     %xmm15, %xmm14, %xmm9                         #7186.16
        vpand     %xmm15, %xmm6, %xmm14                         #7186.90
        vpand     %xmm15, %xmm2, %xmm11                         #7180.16
        vmovdqu   %xmm12, 208(%rsp)                             #7180.83[spill]
        vpsrlq    $32, %xmm4, %xmm7                             #7181.70
        vpmuludq  %xmm12, %xmm14, %xmm12                        #7194.78
        vpand     %xmm15, %xmm4, %xmm0                          #7182.84
        vmovdqu   %xmm11, 192(%rsp)                             #7180.16[spill]
        vpsrlq    $32, %xmm1, %xmm2                             #7183.16
        vpmuludq  %xmm11, %xmm9, %xmm11                         #7194.17
        vpsrlq    $32, %xmm3, %xmm4                             #7183.69
        vmovdqu   %xmm7, 240(%rsp)                              #7181.70[spill]
        vpand     %xmm15, %xmm3, %xmm3                          #7184.83
        vmovdqu   %xmm12, 288(%rsp)                             #7194.78[spill]
        vpmuludq  %xmm7, %xmm14, %xmm12                         #7195.78
        vpand     %xmm15, %xmm1, %xmm7                          #7184.16
        vmovdqu   %xmm8, 224(%rsp)                              #7181.16[spill]
        vmovdqu   %xmm0, 256(%rsp)                              #7182.84[spill]
        vmovdqu   %xmm11, 272(%rsp)                             #7194.17[spill]
        vpmuludq  %xmm8, %xmm9, %xmm11                          #7195.17
        vpmuludq  %xmm0, %xmm14, %xmm8                          #7196.78
        vpmuludq  %xmm7, %xmm10, %xmm0                          #7192.17
        vpmuludq  %xmm2, %xmm10, %xmm7                          #7191.17
        vpmuludq  %xmm2, %xmm9, %xmm2                           #7197.17
        vmovupd   %xmm13, 176(%rsp)                             #7168.1337[spill]
        vpsrlq    $32, %xmm6, %xmm13                            #7185.76
        vpmuludq  %xmm5, %xmm9, %xmm6                           #7196.17
        vpsrlq    $32, %xmm2, %xmm2                             #7199.17
        vpmuludq  %xmm5, %xmm10, %xmm5                          #7190.17
        vpaddq    %xmm2, %xmm7, %xmm7                           #7203.17
        vpmuludq  %xmm4, %xmm13, %xmm2                          #7191.78
        vpmuludq  %xmm4, %xmm14, %xmm4                          #7197.78
        vpsrlq    $32, %xmm0, %xmm1                             #7217.17
        vpand     %xmm15, %xmm6, %xmm0                          #7207.17
        vpaddq    %xmm7, %xmm0, %xmm0                           #7212.17
        vpaddq    %xmm0, %xmm1, %xmm7                           #7218.17
        vpmuludq  %xmm3, %xmm13, %xmm1                          #7192.78
        vpsrlq    $32, %xmm4, %xmm4                             #7199.70
        vpand     %xmm15, %xmm8, %xmm3                          #7207.84
        vpaddq    %xmm4, %xmm2, %xmm2                           #7203.80
        vpaddq    %xmm2, %xmm3, %xmm3                           #7212.80
        vpsrlq    $32, %xmm1, %xmm0                             #7217.70
        vpsrlq    $32, %xmm6, %xmm6                             #7200.17
        vpaddq    %xmm3, %xmm0, %xmm4                           #7218.80
        vpaddq    %xmm6, %xmm5, %xmm2                           #7204.17
        vpmuludq  256(%rsp), %xmm13, %xmm3                      #7190.78[spill]
        vpand     %xmm15, %xmm11, %xmm0                         #7208.17
        vpsrlq    $32, %xmm7, %xmm1                             #7219.17
        vpaddq    %xmm2, %xmm0, %xmm6                           #7213.17
        vpsrlq    $32, %xmm8, %xmm8                             #7200.70
        vpsrlq    $32, %xmm4, %xmm5                             #7219.70
        vpaddq    %xmm6, %xmm1, %xmm0                           #7220.17
        vpaddq    %xmm8, %xmm3, %xmm2                           #7204.80
        vpmuludq  224(%rsp), %xmm10, %xmm6                      #7189.17[spill]
        vpmuludq  192(%rsp), %xmm10, %xmm10                     #7188.17[spill]
        vpand     %xmm15, %xmm12, %xmm1                         #7208.84
        vpsrlq    $32, %xmm11, %xmm11                           #7201.17
        vpaddq    %xmm2, %xmm1, %xmm8                           #7213.80
        vpaddq    %xmm11, %xmm6, %xmm2                          #7205.17
        vpaddq    %xmm8, %xmm5, %xmm1                           #7220.80
        vmovdqu   272(%rsp), %xmm5                              #7209.17[spill]
        vpsrlq    $32, %xmm0, %xmm3                             #7221.17
        vpand     %xmm15, %xmm5, %xmm8                          #7209.17
        vpsrlq    $32, %xmm12, %xmm12                           #7201.70
        vpaddq    %xmm2, %xmm8, %xmm11                          #7214.17
        vpaddq    %xmm11, %xmm3, %xmm3                          #7222.17
        vpmuludq  240(%rsp), %xmm13, %xmm11                     #7189.78[spill]
        vpmuludq  208(%rsp), %xmm13, %xmm13                     #7188.78[spill]
        vpaddq    %xmm12, %xmm11, %xmm11                        #7205.80
        vmovdqu   288(%rsp), %xmm8                              #7209.84[spill]
        vpsrlq    $32, %xmm1, %xmm6                             #7221.70
        vpand     %xmm15, %xmm8, %xmm2                          #7209.84
        vpsrlq    $32, %xmm3, %xmm12                            #7223.17
        vpaddq    %xmm11, %xmm2, %xmm2                          #7214.80
        vpaddq    %xmm2, %xmm6, %xmm11                          #7222.80
        vmovupd   160(%rsp), %xmm6                              #7179.16[spill]
        vpand     %xmm15, %xmm3, %xmm3                          #7226.17
        vpsrlq    $32, %xmm6, %xmm2                             #7179.16
        vpand     %xmm15, %xmm7, %xmm7                          #7225.17
        vpmuludq  %xmm2, %xmm9, %xmm9                           #7193.17
        vpand     %xmm15, %xmm4, %xmm4                          #7225.84
        vpand     %xmm15, %xmm9, %xmm2                          #7210.17
        vpsrlq    $32, %xmm5, %xmm9                             #7202.17
        vmovupd   176(%rsp), %xmm5                              #7179.69[spill]
        vpaddq    %xmm9, %xmm10, %xmm10                         #7206.17
        vpsrlq    $32, %xmm5, %xmm9                             #7179.69
        vpmuludq  %xmm9, %xmm14, %xmm14                         #7193.78
        vpsrlq    $32, %xmm8, %xmm9                             #7202.70
        vpaddq    %xmm10, %xmm2, %xmm2                          #7215.17
        vpaddq    %xmm2, %xmm12, %xmm12                         #7224.17
        vpsllq    $32, %xmm12, %xmm6                            #7227.23
        vpand     %xmm15, %xmm14, %xmm12                        #7210.84
        vpaddq    %xmm9, %xmm13, %xmm14                         #7206.80
        vpaddq    %xmm3, %xmm6, %xmm2                           #7229.23
        vpaddq    %xmm14, %xmm12, %xmm8                         #7215.80
        vpsrlq    $32, %xmm11, %xmm10                           #7223.70
        vpand     %xmm15, %xmm11, %xmm11                        #7226.84
        vpaddq    %xmm8, %xmm10, %xmm13                         #7224.80
        vpsllq    $32, %xmm13, %xmm10                           #7227.82
        vpsllq    $32, %xmm1, %xmm3                             #7228.82
        vmovdqu   .L_2il0floatpacket.33(%rip), %xmm15           #7231.21
        vpsllq    $32, %xmm0, %xmm6                             #7228.23
        vpaddq    %xmm11, %xmm10, %xmm11                        #7229.98
        vpaddq    %xmm4, %xmm3, %xmm13                          #7230.98
        vpaddq    %xmm7, %xmm6, %xmm9                           #7230.23
        vmovupd   .L_2il0floatpacket.47(%rip), %ymm0            #7238.15
        vpand     128(%rsp), %xmm15, %xmm3                      #7232.21[spill]
        vpsrlq    $12, %xmm2, %xmm4                             #7235.24
        vmovdqu   .L_2il0floatpacket.28(%rip), %xmm14           #7233.26
        vpsrlq    $12, %xmm11, %xmm8                            #7235.90
        vpand     144(%rsp), %xmm15, %xmm6                      #7232.94[spill]
        vpxor     %xmm14, %xmm3, %xmm7                          #7234.26
        vpxor     %xmm14, %xmm6, %xmm10                         #7234.111
        vpor      %xmm7, %xmm4, %xmm12                          #7236.24
        vpor      %xmm10, %xmm8, %xmm15                         #7236.109
        vmovdqu   .L_2il0floatpacket.35(%rip), %xmm7            #7245.26
        vpxor     %xmm7, %xmm3, %xmm8                           #7246.26
        vpxor     %xmm7, %xmm6, %xmm14                          #7246.111
        vinsertf128 $1, %xmm15, %ymm12, %ymm5                   #7237.17
        vaddpd    %ymm0, %ymm5, %ymm1                           #7239.19
        vmovdqu   .L_2il0floatpacket.36(%rip), %xmm12           #7247.24
        vsubpd    %ymm0, %ymm1, %ymm4                           #7240.9
        vmovdqu   .L_2il0floatpacket.44(%rip), %xmm0            #7243.19
        vsubpd    %ymm4, %ymm5, %ymm10                          #7241.17
        vpand     %xmm9, %xmm12, %xmm5                          #7248.24
        vpsllq    $28, %xmm5, %xmm4                             #7249.24
        vpand     %xmm13, %xmm12, %xmm12                        #7248.107
        vpor      %xmm8, %xmm4, %xmm7                           #7250.24
        vpsllq    $28, %xmm12, %xmm4                            #7249.91
        vpor      %xmm14, %xmm4, %xmm12                         #7250.109
        vpsrlq    $24, %xmm9, %xmm9                             #7259.23
        vinsertf128 $1, %xmm12, %ymm7, %ymm7                    #7251.17
        vinsertf128 $1, %xmm14, %ymm8, %ymm14                   #7252.15
        vmovdqu   .L_2il0floatpacket.38(%rip), %xmm12           #7256.24
        vsubpd    %ymm14, %ymm7, %ymm4                          #7253.17
        vpand     %xmm2, %xmm12, %xmm2                          #7257.24
        vmovdqu   .L_2il0floatpacket.37(%rip), %xmm7            #7254.26
        vpand     %xmm11, %xmm12, %xmm11                        #7257.107
        vpxor     %xmm7, %xmm6, %xmm14                          #7255.111
        vpsllq    $40, %xmm2, %xmm6                             #7258.24
        vpxor     %xmm7, %xmm3, %xmm8                           #7255.26
        vpor      %xmm9, %xmm6, %xmm2                           #7260.24
        vpor      %xmm8, %xmm2, %xmm7                           #7261.24
        vpsllq    $40, %xmm11, %xmm2                            #7258.91
        vpsrlq    $24, %xmm13, %xmm12                           #7259.88
        vpor      %xmm12, %xmm2, %xmm13                         #7260.106
        vpor      %xmm14, %xmm13, %xmm3                         #7261.109
        vmovupd   .L_2il0floatpacket.49(%rip), %ymm9            #7270.16
        vinsertf128 $1, %xmm3, %ymm7, %ymm5                     #7262.18
        vinsertf128 $1, %xmm14, %ymm8, %ymm14                   #7263.15
        vsubpd    %ymm14, %ymm5, %ymm2                          #7264.18
        vmovupd   .L_2il0floatpacket.45(%rip), %ymm3            #7273.16
        vmovupd   .L_2il0floatpacket.50(%rip), %ymm14           #7271.17
        vaddpd    %ymm2, %ymm10, %ymm7                          #7265.11
        vsubpd    %ymm7, %ymm10, %ymm10                         #7266.17
        vaddpd    %ymm10, %ymm2, %ymm2                          #7267.18
        vandpd    %ymm3, %ymm7, %ymm11                          #7273.16
        vaddpd    %ymm2, %ymm4, %ymm12                          #7268.11
        vsubpd    %ymm11, %ymm7, %ymm8                          #7274.17
        vmulpd    %ymm11, %ymm14, %ymm10                        #7277.12
        vmulpd    .L_2il0floatpacket.48(%rip), %ymm12, %ymm6    #7278.12
        vmulpd    %ymm8, %ymm9, %ymm13                          #7276.12
        vmulpd    %ymm8, %ymm14, %ymm5                          #7279.12
        vmulpd    %ymm11, %ymm9, %ymm4                          #7275.16
        vaddpd    %ymm10, %ymm13, %ymm2                         #7280.13
        vaddpd    %ymm5, %ymm6, %ymm7                           #7281.13
        vaddpd    %ymm7, %ymm2, %ymm9                           #7282.13
        vaddpd    %ymm9, %ymm4, %ymm11                          #7283.13
        vsubpd    %ymm11, %ymm4, %ymm4                          #7284.16
        vaddpd    %ymm4, %ymm9, %ymm13                          #7285.13
        vmovupd   416(%rsp), %ymm9                              #7288.11[spill]
        vmovupd   .L_2il0floatpacket.52(%rip), %ymm4            #7287.16
        vandpd    .L_2il0floatpacket.51(%rip), %ymm9, %ymm2     #7288.11
        vcmple_oqpd %ymm4, %ymm2, %ymm7                         #7290.20
        vcmpgt_oqpd %ymm4, %ymm2, %ymm8                         #7289.17
        vmovdqu   .L_2il0floatpacket.26(%rip), %xmm2            #7297.31
        vandpd    %ymm9, %ymm7, %ymm12                          #7291.24
        vandpd    %ymm11, %ymm8, %ymm14                         #7292.25
        vorpd     %ymm14, %ymm12, %ymm4                         #7293.9
        vandpd    %ymm13, %ymm8, %ymm5                          #7294.9
        vmovupd   %ymm5, 32(%rsp)                               #7294.9[spill]
        vextractf128 $1, %ymm1, %xmm15                          #7242.118
        vpand     %xmm0, %xmm1, %xmm1                           #7244.19
        vpand     %xmm2, %xmm1, %xmm7                           #7297.31
        vmovd     %xmm7, %r8d                                   #7298.140
        vpand     %xmm0, %xmm15, %xmm15                         #7244.88
        vpand     %xmm2, %xmm15, %xmm9                          #7297.110
        vmovd     %xmm9, %r11d                                  #7298.267
        imull     $104, %r8d, %ecx                              #7298.437
        imull     $104, %r11d, %edi                             #7298.541
        vmovq     -88(%rcx,%rsi), %xmm12                        #7299.1141
        vmovq     -96(%rcx,%rsi), %xmm0                         #7298.1141
        vpextrd   $2, %xmm7, %r9d                               #7298.202
        vpextrd   $2, %xmm9, %r8d                               #7298.328
        imull     $104, %r9d, %edx                              #7298.489
        imull     $104, %r8d, %r9d                              #7298.593
        vmovq     -88(%rdi,%rsi), %xmm8                         #7299.1341
        vmovq     -96(%rdi,%rsi), %xmm7                         #7298.1341
        vmovq     -80(%rcx,%rsi), %xmm10                        #7300.1140
        vmovhpd   -88(%rdx,%rsi), %xmm12, %xmm14                #7299.1109
        vmovhpd   -88(%r9,%rsi), %xmm8, %xmm13                  #7299.1309
        vmovhpd   -96(%rdx,%rsi), %xmm0, %xmm2                  #7298.1109
        vmovhpd   -96(%r9,%rsi), %xmm7, %xmm11                  #7298.1309
        vmovq     -80(%rdi,%rsi), %xmm6                         #7300.1340
        vmovhpd   -80(%rdx,%rsi), %xmm10, %xmm15                #7300.1108
        vmovq     -72(%rcx,%rsi), %xmm7                         #7301.1142
        vmovhpd   -80(%r9,%rsi), %xmm6, %xmm0                   #7300.1308
        vmovhpd   -72(%rdx,%rsi), %xmm7, %xmm9                  #7301.1110
        vinsertf128 $1, %xmm13, %ymm14, %ymm1                   #7299.1063
        vmovq     -64(%rcx,%rsi), %xmm14                        #7302.1142
        vmovq     -64(%rdi,%rsi), %xmm13                        #7302.1342
        vmovhpd   -64(%rdx,%rsi), %xmm14, %xmm8                 #7302.1110
        vmovhpd   -64(%r9,%rsi), %xmm13, %xmm10                 #7302.1310
        vmovq     -48(%rcx,%rsi), %xmm14                        #7304.1142
        vmovq     -48(%rdi,%rsi), %xmm13                        #7304.1342
        vinsertf128 $1, %xmm11, %ymm2, %ymm2                    #7298.1063
        vmovq     -72(%rdi,%rsi), %xmm11                        #7301.1342
        vmovhpd   -72(%r9,%rsi), %xmm11, %xmm12                 #7301.1310
        vmovq     -56(%rdi,%rsi), %xmm11                        #7303.1342
        vinsertf128 $1, %xmm0, %ymm15, %ymm6                    #7300.1062
        vmovq     -56(%rcx,%rsi), %xmm0                         #7303.1142
        vinsertf128 $1, %xmm10, %ymm8, %ymm15                   #7302.1064
        vinsertf128 $1, %xmm12, %ymm9, %ymm7                    #7301.1064
        vmovhpd   -56(%rdx,%rsi), %xmm0, %xmm9                  #7303.1110
        vmovhpd   -56(%r9,%rsi), %xmm11, %xmm12                 #7303.1310
        vmovups   %ymm15, 64(%rsp)                              #7302.1064[spill]
        vmovhpd   -48(%rdx,%rsi), %xmm14, %xmm8                 #7304.1110
        vmovq     -40(%rcx,%rsi), %xmm15                        #7305.1139
        vmovhpd   -48(%r9,%rsi), %xmm13, %xmm10                 #7304.1310
        vmovhpd   -40(%rdx,%rsi), %xmm15, %xmm0                 #7305.1107
        vinsertf128 $1, %xmm12, %ymm9, %ymm11                   #7303.1064
        vmovq     -40(%rdi,%rsi), %xmm9                         #7305.1339
        vmovhpd   -40(%r9,%rsi), %xmm9, %xmm14                  #7305.1307
        vinsertf128 $1, %xmm10, %ymm8, %ymm12                   #7304.1064
        vmovq     -32(%rcx,%rsi), %xmm8                         #7306.1139
        vmovq     -32(%rdi,%rsi), %xmm10                        #7306.1339
        vmovhpd   -32(%rdx,%rsi), %xmm8, %xmm13                 #7306.1107
        vmovhpd   -32(%r9,%rsi), %xmm10, %xmm15                 #7306.1307
        vmovq     -24(%rdi,%rsi), %xmm10                        #7307.1339
        vinsertf128 $1, %xmm14, %ymm0, %ymm9                    #7305.1061
        vmovq     -24(%rcx,%rsi), %xmm14                        #7307.1139
        vmovhpd   -24(%rdx,%rsi), %xmm14, %xmm8                 #7307.1107
        vmovq     -16(%rcx,%rsi), %xmm0                         #7308.1139
        vmovhpd   -16(%rdx,%rsi), %xmm0, %xmm14                 #7308.1107
        vmovq     -8(%rcx,%rsi), %xmm0                          #7309.1139
        vmulpd    %ymm9, %ymm4, %ymm9                           #7342.37
        vaddpd    %ymm9, %ymm12, %ymm9                          #7342.22
        vinsertf128 $1, %xmm15, %ymm13, %ymm13                  #7306.1061
        vmovhpd   -24(%r9,%rsi), %xmm10, %xmm15                 #7307.1307
        vinsertf128 $1, %xmm15, %ymm8, %ymm10                   #7307.1061
        vmovq     -16(%rdi,%rsi), %xmm8                         #7308.1341
        vmovhpd   -16(%r9,%rsi), %xmm8, %xmm15                  #7308.1309
        vmovhpd   -8(%rdx,%rsi), %xmm0, %xmm8                   #7309.1107
        vinsertf128 $1, %xmm15, %ymm14, %ymm14                  #7308.1061
        vmovq     -8(%rdi,%rsi), %xmm15                         #7309.1341
        vmovhpd   -8(%r9,%rsi), %xmm15, %xmm0                   #7309.1309
        vmovq     (%rcx,%rsi), %xmm15                           #7310.1139
        vmovhpd   (%rdx,%rsi), %xmm15, %xmm15                   #7310.1107
        vinsertf128 $1, %xmm0, %ymm8, %ymm8                     #7309.1061
        vmovq     (%rdi,%rsi), %xmm0                            #7310.1341
        vmovhpd   (%r9,%rsi), %xmm0, %xmm0                      #7310.1309
        vmulpd    %ymm8, %ymm4, %ymm8                           #7344.37
        vaddpd    %ymm8, %ymm14, %ymm14                         #7344.22
        vinsertf128 $1, %xmm0, %ymm15, %ymm15                   #7310.1061
        vsubpd    %ymm4, %ymm2, %ymm0                           #7311.26
        vmovups   %ymm15, 96(%rsp)                              #7310.1061[spill]
        vsubpd    %ymm0, %ymm2, %ymm2                           #7314.24
        vsubpd    %ymm4, %ymm2, %ymm2                           #7315.24
        vandpd    %ymm3, %ymm0, %ymm15                          #7313.24
        vsubpd    %ymm5, %ymm2, %ymm5                           #7316.24
        vsubpd    %ymm15, %ymm0, %ymm0                          #7317.26
        vcvtpd2ps %ymm15, %xmm2                                 #7320.55
        vaddpd    %ymm0, %ymm1, %ymm1                           #7318.26
        vrcpps    %xmm2, %xmm0                                  #7320.44
        vaddpd    %ymm1, %ymm5, %ymm5                           #7319.24
        vcvtps2pd %xmm0, %ymm1                                  #7320.28
        vandpd    %ymm3, %ymm1, %ymm3                           #7321.28
        vmulpd    %ymm3, %ymm15, %ymm15                         #7323.46
        vmovupd   .L_2il0floatpacket.46(%rip), %ymm1            #7323.22
        vsubpd    %ymm15, %ymm1, %ymm0                          #7323.22
        vmulpd    %ymm0, %ymm3, %ymm2                           #7324.39
        vaddpd    %ymm2, %ymm3, %ymm15                          #7324.24
        vmulpd    %ymm0, %ymm0, %ymm2                           #7325.37
        vmulpd    %ymm3, %ymm6, %ymm3                           #7331.32
        vaddpd    %ymm2, %ymm1, %ymm1                           #7325.22
        vmulpd    %ymm1, %ymm15, %ymm2                          #7326.28
        vmulpd    %ymm2, %ymm5, %ymm5                           #7327.24
        vmulpd    %ymm5, %ymm5, %ymm15                          #7329.43
        vsubpd    %ymm0, %ymm5, %ymm0                           #7328.23
        vmulpd    %ymm11, %ymm4, %ymm5                          #7333.26
        vsubpd    %ymm0, %ymm15, %ymm1                          #7329.28
        vaddpd    %ymm5, %ymm7, %ymm15                          #7334.26
        vmulpd    %ymm1, %ymm2, %ymm2                           #7330.28
        vmulpd    %ymm10, %ymm4, %ymm1                          #7343.37
        vsubpd    %ymm15, %ymm7, %ymm7                          #7335.26
        vmulpd    %ymm2, %ymm6, %ymm6                           #7332.32
        vaddpd    %ymm7, %ymm5, %ymm7                           #7337.26
        vaddpd    %ymm15, %ymm3, %ymm2                          #7336.26
        vmulpd    %ymm4, %ymm4, %ymm5                           #7345.22
        vsubpd    %ymm2, %ymm3, %ymm3                           #7338.26
        vaddpd    %ymm7, %ymm6, %ymm7                           #7339.26
        vaddpd    %ymm12, %ymm11, %ymm6                         #7346.22
        vaddpd    %ymm3, %ymm15, %ymm0                          #7340.26
        vaddpd    %ymm1, %ymm13, %ymm3                          #7343.22
        vmulpd    96(%rsp), %ymm5, %ymm11                       #7347.37[spill]
        vmulpd    %ymm5, %ymm5, %ymm15                          #7349.22
        vmulpd    %ymm5, %ymm3, %ymm12                          #7348.38
        vmulpd    32(%rsp), %ymm6, %ymm10                       #7350.38[spill]
        vaddpd    %ymm11, %ymm14, %ymm13                        #7347.22
        vaddpd    %ymm0, %ymm7, %ymm7                           #7341.26
        vaddpd    %ymm12, %ymm9, %ymm0                          #7348.23
        vmulpd    %ymm15, %ymm13, %ymm13                        #7351.38
        vaddpd    %ymm10, %ymm7, %ymm1                          #7350.23
        vaddpd    %ymm13, %ymm0, %ymm3                          #7351.23
        vaddpd    64(%rsp), %ymm1, %ymm5                        #7352.23[spill]
        vmulpd    %ymm3, %ymm4, %ymm4                           #7353.38
        vmovupd   352(%rsp), %ymm3                              #7356.22[spill]
        vaddpd    %ymm4, %ymm5, %ymm0                           #7353.23
        vaddpd    %ymm0, %ymm2, %ymm1                           #7354.28
        vmovupd   (%rsp), %ymm0                                 #7356.22[spill]
        vxorpd    384(%rsp), %ymm1, %ymm2                       #7355.28[spill]
        vblendvpd %ymm3, %ymm2, %ymm0, %ymm0                    #7356.22
        jmp       ..B3.2        # Prob 100%                     #7356.22
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax ymm0
	.cfi_endproc
# mark_end;
	.type	__svml_tan4_ha_e9,@function
	.size	__svml_tan4_ha_e9,.-__svml_tan4_ha_e9
..LN__svml_tan4_ha_e9.2:
	.data
# -- End  __svml_tan4_ha_e9
	.text
.L_2__routine_start___svml_tan1_ha_l9_3:
# -- Begin  __svml_tan1_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tan1_ha_l9
# --- __svml_tan1_ha_l9(__m128d)
__svml_tan1_ha_l9:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #7367.1
        .byte     15                                            #7687.18
        .byte     30                                            #7687.18
        .byte     250                                           #7687.18
	.cfi_startproc
..___tag_value___svml_tan1_ha_l9.113:
..L114:
                                                        #7367.1
        pushq     %rbp                                          #7367.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #7367.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #7367.1
        subq      $128, %rsp                                    #7367.1
        vmovapd   %xmm0, %xmm14                                 #7367.1
        vmovsd    1024+__svml_dtan_ha_data_internal(%rip), %xmm1 #7428.19
        xorl      %esi, %esi                                    #7427.5
        vmovsd    1152+__svml_dtan_ha_data_internal(%rip), %xmm4 #7439.24
        vandpd    %xmm1, %xmm14, %xmm11                         #7429.16
        vmovapd   %xmm14, %xmm1                                 #7440.17
        vcmpnlesd 28160+__svml_dtan_ha_data_internal(%rip), %xmm11, %xmm0 #7433.30
        vfmadd132sd __svml_dtan_ha_data_internal(%rip), %xmm4, %xmm1 #7440.17
        vmovsd    256+__svml_dtan_ha_data_internal(%rip), %xmm5 #7443.23
        vmovsd    320+__svml_dtan_ha_data_internal(%rip), %xmm7 #7444.23
        vsubsd    %xmm4, %xmm1, %xmm10                          #7441.17
        vmovmskpd %xmm0, %eax                                   #7435.78
        vmovapd   %xmm10, %xmm2                                 #7445.22
        vmovapd   %xmm10, %xmm3                                 #7446.22
        vfnmadd132sd 64+__svml_dtan_ha_data_internal(%rip), %xmm14, %xmm2 #7445.22
        vmovapd   %xmm10, %xmm4                                 #7447.21
        vmovapd   %xmm10, %xmm9                                 #7450.22
        vfnmadd213sd %xmm2, %xmm5, %xmm3                        #7446.22
        vfnmadd213sd %xmm3, %xmm7, %xmm4                        #7447.21
        vsubsd    %xmm3, %xmm2, %xmm6                           #7448.23
        vfnmadd213sd %xmm6, %xmm5, %xmm9                        #7450.22
        vsubsd    %xmm3, %xmm4, %xmm8                           #7449.23
        vfmadd213sd %xmm8, %xmm7, %xmm10                        #7451.23
        vsubsd    %xmm10, %xmm9, %xmm10                         #7452.22
        testb     $1, %al                                       #7454.55
        jne       ..B4.8        # Prob 5%                       #7454.66
                                # LOE rbx r12 r13 r14 r15 esi xmm0 xmm1 xmm4 xmm10 xmm11 xmm14
..B4.2:                         # Preds ..B4.8 ..B4.1
                                # Execution count [1.00e+00]
        vmulsd    %xmm4, %xmm4, %xmm8                           #7692.14
        vpsllq    $3, %xmm1, %xmm5                              #7689.226
        vmovsd    896+__svml_dtan_ha_data_internal(%rip), %xmm7 #7693.21
        lea       512+__svml_dtan_ha_data_internal(%rip), %rdx  #7690.14
        vfmadd213sd 832+__svml_dtan_ha_data_internal(%rip), %xmm8, %xmm7 #7695.20
        vpand     .L_2il0floatpacket.57(%rip), %xmm5, %xmm6     #7689.275
        vmovsd    960+__svml_dtan_ha_data_internal(%rip), %xmm5 #7712.15
        vmovd     %xmm6, %eax                                   #7690.104
        vfmadd213sd 768+__svml_dtan_ha_data_internal(%rip), %xmm8, %xmm7 #7697.20
        vfmadd213sd 704+__svml_dtan_ha_data_internal(%rip), %xmm8, %xmm7 #7699.16
        vfmadd213sd 640+__svml_dtan_ha_data_internal(%rip), %xmm8, %xmm7 #7701.16
        vmulsd    %xmm4, %xmm7, %xmm9                           #7702.16
        vfnmsub213sd %xmm10, %xmm8, %xmm9                       #7703.20
        movslq    %eax, %rax                                    #7690.104
        vsubsd    %xmm9, %xmm4, %xmm0                           #7704.17
        vmovsd    -128(%rax,%rdx), %xmm2                        #7690.14
        vsubsd    %xmm0, %xmm4, %xmm4                           #7705.20
        vaddsd    %xmm2, %xmm0, %xmm1                           #7707.21
        vsubsd    %xmm9, %xmm4, %xmm15                          #7706.20
        vsubsd    %xmm2, %xmm1, %xmm10                          #7708.20
        vmovapd   %xmm2, %xmm6                                  #7713.23
        vmovapd   %xmm2, %xmm4                                  #7715.20
        vfnmadd213sd %xmm5, %xmm0, %xmm6                        #7713.23
        vsubsd    %xmm10, %xmm0, %xmm11                         #7709.20
        vdivsd    %xmm6, %xmm5, %xmm8                           #7718.23
        vmovsd    (%rax,%rdx), %xmm3                            #7691.18
        vsubsd    %xmm5, %xmm6, %xmm13                          #7714.20
        vaddsd    %xmm3, %xmm11, %xmm12                         #7710.20
        vfmadd213sd %xmm13, %xmm0, %xmm4                        #7715.20
        vaddsd    %xmm15, %xmm12, %xmm7                         #7711.20
        vfmadd213sd %xmm4, %xmm15, %xmm2                        #7716.20
        vfmadd213sd %xmm2, %xmm0, %xmm3                         #7717.20
        vmovapd   %xmm8, %xmm0                                  #7719.21
        vfnmadd213sd %xmm5, %xmm6, %xmm0                        #7719.21
        vfmadd213sd %xmm8, %xmm0, %xmm8                         #7720.27
        vmovapd   %xmm8, %xmm2                                  #7721.25
        vmovapd   %xmm8, %xmm13                                 #7722.25
        vfnmadd213sd %xmm5, %xmm6, %xmm2                        #7721.25
        vfmadd213sd %xmm2, %xmm3, %xmm13                        #7722.25
        vfmadd213sd %xmm8, %xmm13, %xmm8                        #7723.23
        vmulsd    %xmm1, %xmm8, %xmm9                           #7724.24
        vfmsub213sd %xmm1, %xmm9, %xmm6                         #7725.18
        vfnmadd213sd %xmm6, %xmm9, %xmm3                        #7726.22
        vsubsd    %xmm7, %xmm3, %xmm0                           #7727.22
        vfnmadd213sd %xmm9, %xmm8, %xmm0                        #7728.18
        testl     %esi, %esi                                    #7730.52
        jne       ..B4.4        # Prob 5%                       #7730.52
                                # LOE rbx r12 r13 r14 r15 esi xmm0 xmm14
..B4.3:                         # Preds ..B4.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #7733.12
        popq      %rbp                                          #7733.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #7733.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.4:                         # Preds ..B4.2
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm14, (%rsp)                                #7730.160
        vmovsd    %xmm0, 64(%rsp)                               #7730.233
        jne       ..B4.6        # Prob 5%                       #7730.374
                                # LOE rbx r12 r13 r14 r15 esi
..B4.5:                         # Preds ..B4.7 ..B4.6 ..B4.4
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm0                               #7730.625
        movq      %rbp, %rsp                                    #7730.625
        popq      %rbp                                          #7730.625
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #7730.625
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.6:                         # Preds ..B4.4
                                # Execution count [2.50e-03]: Infreq
        je        ..B4.5        # Prob 95%                      #7730.517
                                # LOE rbx r12 r13 r14 r15
..B4.7:                         # Preds ..B4.6
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #7730.546
        lea       64(%rsp), %rsi                                #7730.546
..___tag_value___svml_tan1_ha_l9.126:
#       __svml_dtan_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtan_ha_cout_rare_internal             #7730.546
..___tag_value___svml_tan1_ha_l9.127:
        jmp       ..B4.5        # Prob 100%                     #7730.546
                                # LOE rbx r12 r13 r14 r15
..B4.8:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm1, (%rsp)                                 #[spill]
        lea       __svml_dtan_ha_reduction_data_internal(%rip), %rcx #7548.383
        vmovsd    1088+__svml_dtan_ha_data_internal(%rip), %xmm1 #7456.24
        vandpd    %xmm11, %xmm1, %xmm7                          #7457.17
        vmovupd   %xmm4, 16(%rsp)                               #[spill]
        vcmpeqsd  %xmm1, %xmm7, %xmm11                          #7458.25
        vpand     .L_2il0floatpacket.29(%rip), %xmm14, %xmm1    #7546.18
        vpsrlq    $52, %xmm1, %xmm15                            #7547.18
        vmovd     %xmm15, %eax                                  #7548.124
        vpand     .L_2il0floatpacket.30(%rip), %xmm14, %xmm4    #7556.21
        vpaddq    .L_2il0floatpacket.31(%rip), %xmm4, %xmm9     #7557.21
        vmovmskpd %xmm11, %esi                                  #7460.48
        vmovdqu   .L_2il0floatpacket.32(%rip), %xmm13           #7558.17
        vmovupd   %xmm10, 32(%rsp)                              #[spill]
        vpsrlq    $32, %xmm9, %xmm10                            #7565.12
        vpand     %xmm13, %xmm9, %xmm7                          #7566.12
        lea       (%rax,%rax,2), %edx                           #7548.230
        shll      $3, %edx                                      #7548.230
        andl      $1, %esi                                      #7460.102
        vmovq     16(%rcx,%rdx), %xmm12                         #7550.383
        vpand     %xmm13, %xmm12, %xmm15                        #7564.12
        vpsrlq    $32, %xmm12, %xmm1                            #7563.12
        vpmuludq  %xmm15, %xmm10, %xmm12                        #7572.13
        vmovq     8(%rcx,%rdx), %xmm6                           #7549.384
        vpsrlq    $32, %xmm12, %xmm15                           #7597.13
        vpmuludq  %xmm1, %xmm10, %xmm12                         #7571.13
        vpand     %xmm13, %xmm6, %xmm3                          #7562.12
        vpmuludq  %xmm1, %xmm7, %xmm1                           #7577.13
        vpmuludq  %xmm3, %xmm7, %xmm2                           #7576.13
        vpmuludq  %xmm3, %xmm10, %xmm3                          #7570.13
        vpsrlq    $32, %xmm1, %xmm1                             #7579.13
        vpsrlq    $32, %xmm6, %xmm5                             #7561.12
        vpaddq    %xmm1, %xmm12, %xmm12                         #7583.13
        vpmuludq  %xmm5, %xmm7, %xmm4                           #7575.13
        vpmuludq  %xmm5, %xmm10, %xmm5                          #7569.13
        vpand     %xmm13, %xmm2, %xmm6                          #7587.13
        vpsrlq    $32, %xmm2, %xmm2                             #7580.13
        vpaddq    %xmm12, %xmm6, %xmm6                          #7592.13
        vmovq     (%rdx,%rcx), %xmm11                           #7548.383
        vpand     %xmm13, %xmm4, %xmm1                          #7588.13
        vpaddq    %xmm6, %xmm15, %xmm12                         #7598.13
        vpaddq    %xmm2, %xmm3, %xmm6                           #7584.13
        vpand     %xmm13, %xmm11, %xmm8                         #7560.12
        vpsrlq    $32, %xmm12, %xmm15                           #7599.13
        vpmuludq  %xmm8, %xmm7, %xmm9                           #7574.13
        vpsrlq    $32, %xmm11, %xmm11                           #7559.12
        vpmuludq  %xmm8, %xmm10, %xmm8                          #7568.13
        vpmuludq  %xmm11, %xmm7, %xmm7                          #7573.13
        vpaddq    %xmm6, %xmm1, %xmm1                           #7593.13
        vpaddq    %xmm1, %xmm15, %xmm6                          #7600.13
        vpsrlq    $32, %xmm4, %xmm1                             #7581.13
        vpand     %xmm13, %xmm9, %xmm2                          #7589.13
        vpaddq    %xmm1, %xmm5, %xmm15                          #7585.13
        vpaddq    %xmm15, %xmm2, %xmm4                          #7594.13
        vpsrlq    $32, %xmm9, %xmm10                            #7582.13
        vpsrlq    $32, %xmm6, %xmm3                             #7601.13
        vpaddq    %xmm10, %xmm8, %xmm15                         #7586.13
        vpaddq    %xmm4, %xmm3, %xmm5                           #7602.13
        vpand     %xmm13, %xmm7, %xmm1                          #7590.13
        vpsrlq    $32, %xmm5, %xmm4                             #7603.13
        vpaddq    %xmm15, %xmm1, %xmm8                          #7595.13
        vpaddq    %xmm8, %xmm4, %xmm4                           #7604.13
        vpsllq    $32, %xmm4, %xmm10                            #7607.19
        vpand     %xmm13, %xmm5, %xmm9                          #7606.13
        vpaddq    %xmm9, %xmm10, %xmm10                         #7609.19
        vpand     .L_2il0floatpacket.33(%rip), %xmm14, %xmm9    #7612.17
        vpsllq    $32, %xmm6, %xmm6                             #7608.19
        vpxor     .L_2il0floatpacket.28(%rip), %xmm9, %xmm15    #7614.22
        vpand     %xmm13, %xmm12, %xmm13                        #7605.13
        vpsrlq    $12, %xmm10, %xmm1                            #7615.20
        vpaddq    %xmm13, %xmm6, %xmm6                          #7610.19
        vmovups   .L_2il0floatpacket.34(%rip), %xmm4            #7618.15
        vpor      %xmm15, %xmm1, %xmm13                         #7616.20
        vpand     .L_2il0floatpacket.38(%rip), %xmm10, %xmm10   #7637.20
        vaddsd    %xmm13, %xmm4, %xmm8                          #7619.19
        vpxor     .L_2il0floatpacket.35(%rip), %xmm9, %xmm7     #7626.22
        vsubsd    %xmm4, %xmm8, %xmm12                          #7620.9
        vpand     .L_2il0floatpacket.36(%rip), %xmm6, %xmm2     #7628.20
        vpsrlq    $24, %xmm6, %xmm6                             #7639.19
        vpxor     .L_2il0floatpacket.37(%rip), %xmm9, %xmm4     #7635.22
        vpsllq    $40, %xmm10, %xmm9                            #7638.20
        vpor      %xmm6, %xmm9, %xmm11                          #7640.20
        vpsllq    $28, %xmm2, %xmm3                             #7629.20
        vpor      %xmm4, %xmm11, %xmm1                          #7641.20
        vpor      %xmm7, %xmm3, %xmm5                           #7630.20
        vmovups   .L_2il0floatpacket.39(%rip), %xmm3            #7649.16
        vsubsd    %xmm12, %xmm13, %xmm12                        #7621.17
        vsubsd    %xmm4, %xmm1, %xmm13                          #7644.18
        vsubsd    %xmm7, %xmm5, %xmm15                          #7633.17
        vaddsd    %xmm13, %xmm12, %xmm5                         #7645.11
        vmulsd    %xmm3, %xmm5, %xmm11                          #7651.13
        vsubsd    %xmm5, %xmm12, %xmm12                         #7646.17
        vmovups   .L_2il0floatpacket.43(%rip), %xmm9            #7656.16
        vaddsd    %xmm12, %xmm13, %xmm2                         #7647.18
        vandpd    .L_2il0floatpacket.42(%rip), %xmm14, %xmm7    #7657.11
        vaddsd    %xmm15, %xmm2, %xmm2                          #7648.11
        vcmpltsd  %xmm7, %xmm9, %xmm6                           #7658.17
        vcmplesd  %xmm9, %xmm7, %xmm10                          #7659.20
        vmovapd   %xmm5, %xmm15                                 #7652.13
        vmovsd    %xmm6, %xmm7, %xmm4                           #7658.17
        vfmsub213sd %xmm11, %xmm3, %xmm15                       #7652.13
        vandpd    %xmm14, %xmm10, %xmm1                         #7660.24
        vpand     .L_2il0floatpacket.44(%rip), %xmm8, %xmm8     #7624.15
        vpshufd   $0, %xmm8, %xmm8                              #7665.23
        vpxor     %xmm6, %xmm6, %xmm6                           #7673.58
        vmovups   .L_2il0floatpacket.55(%rip), %xmm12           #7678.19
        vfmadd132sd .L_2il0floatpacket.53(%rip), %xmm15, %xmm5  #7653.13
        vandpd    %xmm11, %xmm4, %xmm15                         #7661.25
        vorpd     %xmm15, %xmm1, %xmm13                         #7662.9
        vpshufd   $85, %xmm13, %xmm15                           #7668.22
        vfmadd213sd %xmm5, %xmm3, %xmm2                         #7654.13
        vandpd    %xmm2, %xmm4, %xmm1                           #7663.9
        vpsrld    $31, %xmm15, %xmm2                            #7669.22
        vmovdqu   .L_2il0floatpacket.54(%rip), %xmm4            #7670.23
        vpsubd    %xmm2, %xmm4, %xmm3                           #7670.23
        vpaddd    %xmm3, %xmm8, %xmm5                           #7672.23
        vpsrld    $4, %xmm5, %xmm7                              #7672.23
        vpslld    $4, %xmm7, %xmm9                              #7675.23
        vpsubd    %xmm9, %xmm8, %xmm10                          #7676.23
        vpshufd   $0, %xmm10, %xmm11                            #7677.40
        vcvtdq2pd %xmm11, %xmm2                                 #7677.23
        vmovapd   %xmm2, %xmm3                                  #7680.26
        vmovapd   %xmm2, %xmm4                                  #7681.27
        vfmadd213sd %xmm13, %xmm12, %xmm3                       #7680.26
        vpunpckldq %xmm6, %xmm7, %xmm15                         #7673.27
        vmovupd   32(%rsp), %xmm10                              #7687.18[spill]
        vfnmadd213sd %xmm3, %xmm12, %xmm4                       #7681.27
        vsubsd    %xmm4, %xmm13, %xmm13                         #7682.27
        vmovupd   16(%rsp), %xmm4                               #7686.17[spill]
        vfmadd132sd .L_2il0floatpacket.56(%rip), %xmm13, %xmm2  #7683.27
        blendvpd  %xmm0, %xmm3, %xmm4                           #7686.17
        vaddsd    %xmm1, %xmm2, %xmm5                           #7684.27
        vmovupd   (%rsp), %xmm1                                 #7685.17[spill]
        blendvpd  %xmm0, %xmm15, %xmm1                          #7685.17
        blendvpd  %xmm0, %xmm5, %xmm10                          #7687.18
        jmp       ..B4.2        # Prob 100%                     #7687.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi xmm1 xmm4 xmm10 xmm14
	.cfi_endproc
# mark_end;
	.type	__svml_tan1_ha_l9,@function
	.size	__svml_tan1_ha_l9,.-__svml_tan1_ha_l9
..LN__svml_tan1_ha_l9.3:
	.data
# -- End  __svml_tan1_ha_l9
	.text
.L_2__routine_start___svml_tan8_ha_z0_4:
# -- Begin  __svml_tan8_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tan8_ha_z0
# --- __svml_tan8_ha_z0(__m512d)
__svml_tan8_ha_z0:
# parameter 1: %zmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #7738.1
        .byte     15                                            #8054.18
        .byte     30                                            #8054.18
        .byte     250                                           #8054.18
	.cfi_startproc
..___tag_value___svml_tan8_ha_z0.135:
..L136:
                                                        #7738.1
        pushq     %rbp                                          #7738.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #7738.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #7738.1
        subq      $192, %rsp                                    #7738.1
        xorl      %esi, %esi                                    #7798.5
        vmovaps   %zmm0, %zmm11                                 #7738.1
        vmovups   1152+__svml_dtan_ha_data_internal(%rip), %zmm1 #7806.56
        vmovups   __svml_dtan_ha_data_internal(%rip), %zmm0     #7807.17
        vmovups   64+__svml_dtan_ha_data_internal(%rip), %zmm2  #7809.51
        vmovups   256+__svml_dtan_ha_data_internal(%rip), %zmm5 #7810.55
        vmovups   320+__svml_dtan_ha_data_internal(%rip), %zmm6 #7811.55
        vmovups   28160+__svml_dtan_ha_data_internal(%rip), %zmm3 #7801.61
        vfmadd213pd {rn-sae}, %zmm1, %zmm11, %zmm0              #7807.17
        vandpd    1024+__svml_dtan_ha_data_internal(%rip), %zmm11, %zmm10 #7800.16
        vsubpd    {rn-sae}, %zmm1, %zmm0, %zmm9                 #7808.19
        vcmppd    $22, {sae}, %zmm3, %zmm10, %k1                #7802.30
        vfnmadd213pd {rn-sae}, %zmm11, %zmm9, %zmm2             #7812.22
        kmovw     %k1, %eax                                     #7803.75
        vmovaps   %zmm5, %zmm4                                  #7813.22
        vfnmadd213pd {rn-sae}, %zmm2, %zmm9, %zmm4              #7813.22
        vmovaps   %zmm6, %zmm3                                  #7814.21
        vfnmadd213pd {rn-sae}, %zmm4, %zmm9, %zmm3              #7814.21
        vsubpd    {rn-sae}, %zmm4, %zmm2, %zmm8                 #7815.25
        vsubpd    {rn-sae}, %zmm4, %zmm3, %zmm7                 #7816.25
        vfnmadd231pd {rn-sae}, %zmm9, %zmm5, %zmm8              #7817.22
        vfmadd213pd {rn-sae}, %zmm7, %zmm6, %zmm9               #7818.23
        vsubpd    {rn-sae}, %zmm9, %zmm8, %zmm12                #7819.24
        testl     %eax, %eax                                    #7821.66
        jne       ..B5.17       # Prob 5%                       #7821.66
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm3 zmm10 zmm11 zmm12 k1
..B5.2:                         # Preds ..B5.17 ..B5.1
                                # Execution count [1.00e+00]
        vmulpd    {rn-sae}, %zmm3, %zmm3, %zmm8                 #8059.16
        vmovups   384+__svml_dtan_ha_data_internal(%rip), %zmm1 #8057.276
        vmovups   512+__svml_dtan_ha_data_internal(%rip), %zmm2 #8058.280
        vmovups   832+__svml_dtan_ha_data_internal(%rip), %zmm7 #8061.53
        vmovups   768+__svml_dtan_ha_data_internal(%rip), %zmm4 #8063.53
        vmovups   704+__svml_dtan_ha_data_internal(%rip), %zmm5 #8065.49
        vmovups   640+__svml_dtan_ha_data_internal(%rip), %zmm6 #8067.49
        vpermt2pd 576+__svml_dtan_ha_data_internal(%rip), %zmm0, %zmm2 #8058.280
        vpermt2pd 448+__svml_dtan_ha_data_internal(%rip), %zmm0, %zmm1 #8057.276
        vmovups   896+__svml_dtan_ha_data_internal(%rip), %zmm0 #8060.53
        vfmadd231pd {rn-sae}, %zmm8, %zmm0, %zmm7               #8062.20
        vfmadd213pd {rn-sae}, %zmm4, %zmm8, %zmm7               #8064.20
        vfmadd213pd {rn-sae}, %zmm5, %zmm8, %zmm7               #8066.16
        vfmadd213pd {rn-sae}, %zmm6, %zmm8, %zmm7               #8068.16
        vmulpd    {rn-sae}, %zmm3, %zmm7, %zmm9                 #8069.18
        vfnmsub213pd {rn-sae}, %zmm12, %zmm8, %zmm9             #8070.20
        vsubpd    {rn-sae}, %zmm9, %zmm3, %zmm0                 #8071.19
        vsubpd    {rn-sae}, %zmm0, %zmm3, %zmm3                 #8072.22
        vsubpd    {rn-sae}, %zmm9, %zmm3, %zmm14                #8073.22
        vaddpd    {rn-sae}, %zmm1, %zmm0, %zmm3                 #8074.23
        vsubpd    {rn-sae}, %zmm1, %zmm3, %zmm10                #8075.22
        vsubpd    {rn-sae}, %zmm10, %zmm0, %zmm12               #8076.22
        vmovups   960+__svml_dtan_ha_data_internal(%rip), %zmm10 #8079.47
        vaddpd    {rn-sae}, %zmm2, %zmm12, %zmm13               #8077.22
        vmovaps   %zmm10, %zmm4                                 #8080.23
        vfnmadd231pd {rn-sae}, %zmm1, %zmm0, %zmm4              #8080.23
        vaddpd    {rn-sae}, %zmm14, %zmm13, %zmm5               #8078.22
        vsubpd    {rn-sae}, %zmm10, %zmm4, %zmm15               #8081.22
        vfmadd231pd {rn-sae}, %zmm1, %zmm0, %zmm15              #8082.20
        vfmadd213pd {rn-sae}, %zmm15, %zmm14, %zmm1             #8083.20
        vfmadd213pd {rn-sae}, %zmm1, %zmm0, %zmm2               #8084.20
        vrcp14pd  %zmm4, %zmm1                                  #8085.23
        vfnmadd231pd {rn-sae}, %zmm1, %zmm4, %zmm10             #8086.21
        vfmadd231pd {rn-sae}, %zmm1, %zmm2, %zmm10              #8087.25
        vfmadd213pd {rn-sae}, %zmm1, %zmm10, %zmm1              #8088.23
        vmulpd    {rn-sae}, %zmm3, %zmm1, %zmm6                 #8089.26
        vfmsub213pd {rn-sae}, %zmm3, %zmm6, %zmm4               #8090.18
        vfnmadd213pd {rn-sae}, %zmm4, %zmm6, %zmm2              #8091.22
        vsubpd    {rn-sae}, %zmm5, %zmm2, %zmm0                 #8092.24
        vfnmadd213pd {rn-sae}, %zmm6, %zmm1, %zmm0              #8093.18
        testl     %esi, %esi                                    #8095.52
        jne       ..B5.4        # Prob 5%                       #8095.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11
..B5.3:                         # Preds ..B5.10 ..B5.8 ..B5.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #8098.12
        popq      %rbp                                          #8098.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #8098.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.4:                         # Preds ..B5.2
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #8095.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11
..B5.5:                         # Preds ..B5.4
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #8095.249
        movl      %edx, %eax                                    #8095.303
        orl       $8064, %eax                                   #8095.303
        cmpl      %eax, %edx                                    #8095.332
        je        ..B5.7        # Prob 78%                      #8095.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm11
..B5.6:                         # Preds ..B5.5
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #8095.344
        vldmxcsr  32(%rsp)                                      #8095.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm11
..B5.7:                         # Preds ..B5.6 ..B5.5
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm11, 64(%rsp)                              #8095.406
        vmovups   %zmm0, 128(%rsp)                              #8095.481
        testl     %esi, %esi                                    #8095.587
        jne       ..B5.12       # Prob 5%                       #8095.587
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B5.8:                         # Preds ..B5.15 ..B5.7
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #8095.930
        je        ..B5.3        # Prob 78%                      #8095.930
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B5.9:                         # Preds ..B5.8
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #8095.955
        movl      32(%rsp), %eax                                #8095.955
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B5.10:                        # Preds ..B5.9
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #8095.942
        orl       %edx, %eax                                    #8095.942
        movl      %eax, 32(%rsp)                                #8095.942
        vldmxcsr  32(%rsp)                                      #8095.942
        jmp       ..B5.3        # Prob 100%                     #8095.942
                                # LOE rbx r12 r13 r14 r15 zmm0
..B5.12:                        # Preds ..B5.7
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #8095.667
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B5.20:                        # Preds ..B5.12
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
..B5.13:                        # Preds ..B5.14 ..B5.20
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #8095.730
        jc        ..B5.16       # Prob 5%                       #8095.730
                                # LOE rbx r15 r12d r13d r14d
..B5.14:                        # Preds ..B5.16 ..B5.13
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #8095.683
        cmpl      $8, %ebx                                      #8095.678
        jl        ..B5.13       # Prob 82%                      #8095.678
                                # LOE rbx r15 r12d r13d r14d
..B5.15:                        # Preds ..B5.14
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
        vmovups   128(%rsp), %zmm0                              #8095.886
        jmp       ..B5.8        # Prob 100%                     #8095.886
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B5.16:                        # Preds ..B5.13
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,8), %rdi                         #8095.759
        lea       128(%rsp,%rbx,8), %rsi                        #8095.759
..___tag_value___svml_tan8_ha_z0.163:
#       __svml_dtan_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtan_ha_cout_rare_internal             #8095.759
..___tag_value___svml_tan8_ha_z0.164:
        jmp       ..B5.14       # Prob 100%                     #8095.759
	.cfi_restore 3
	.cfi_restore 12
	.cfi_restore 13
	.cfi_restore 14
                                # LOE r15 ebx r12d r13d r14d
..B5.17:                        # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   1088+__svml_dtan_ha_data_internal(%rip), %zmm14 #7823.56
        lea       __svml_dtan_ha_reduction_data_internal(%rip), %rax #7915.406
        vmovups   %zmm0, (%rsp)                                 #[spill]
        vmovups   %zmm3, 64(%rsp)                               #[spill]
        vmovups   %zmm12, 128(%rsp)                             #[spill]
        vandpd    %zmm10, %zmm14, %zmm0                         #7824.17
        vpbroadcastq .L_2il0floatpacket.58(%rip), %zmm10        #7825.71
        vcmppd    $4, {sae}, %zmm14, %zmm0, %k2                 #7825.122
        vmovaps   %zmm10, %zmm3                                 #7825.46
        vpandq    .L_2il0floatpacket.59(%rip){1to8}, %zmm11, %zmm7 #7913.16
        vpsrlq    $52, %zmm7, %zmm8                             #7914.18
        vpsllq    $1, %zmm8, %zmm6                              #7915.134
        vpaddq    %zmm8, %zmm6, %zmm5                           #7915.194
        vpbroadcastq .L_2il0floatpacket.62(%rip), %zmm6         #7925.15
        vpsllq    $3, %zmm5, %zmm4                              #7915.262
        vpmovqd   %zmm4, %ymm9                                  #7915.355
        vpandnq   %zmm0, %zmm0, %zmm3{%k2}                      #7825.46
        vcmppd    $3, {sae}, %zmm3, %zmm3, %k0                  #7827.37
        kxnorw    %k0, %k0, %k3                                 #7915.406
        kxnorw    %k0, %k0, %k4                                 #7916.407
        kxnorw    %k0, %k0, %k5                                 #7917.406
        kmovw     %k0, %esi                                     #7827.37
        vpxord    %zmm2, %zmm2, %zmm2                           #7915.406
        vpxord    %zmm1, %zmm1, %zmm1                           #7916.407
        vpxord    %zmm3, %zmm3, %zmm3                           #7917.406
        vgatherdpd (%rax,%ymm9), %zmm2{%k3}                     #7915.406
        vgatherdpd 8(%rax,%ymm9), %zmm1{%k4}                    #7916.407
        vgatherdpd 16(%rax,%ymm9), %zmm3{%k5}                   #7917.406
        vpsrlq    $32, %zmm2, %zmm0                             #7926.12
        vpsrlq    $32, %zmm1, %zmm9                             #7928.12
        vpsrlq    $32, %zmm3, %zmm13                            #7930.12
        vpandq    .L_2il0floatpacket.60(%rip){1to8}, %zmm11, %zmm15 #7923.19
        vpaddq    .L_2il0floatpacket.61(%rip){1to8}, %zmm15, %zmm14 #7924.19
        vpsrlq    $32, %zmm14, %zmm7                            #7932.12
        vpandq    %zmm6, %zmm2, %zmm5                           #7927.10
        vpandq    %zmm6, %zmm1, %zmm12                          #7929.10
        vpandq    %zmm6, %zmm3, %zmm8                           #7931.10
        vpandq    %zmm6, %zmm14, %zmm14                         #7933.10
        vpmullq   %zmm5, %zmm7, %zmm4                           #7935.13
        vpmullq   %zmm9, %zmm7, %zmm3                           #7936.13
        vpmullq   %zmm12, %zmm7, %zmm2                          #7937.13
        vpmullq   %zmm13, %zmm7, %zmm1                          #7938.13
        vpmullq   %zmm8, %zmm7, %zmm8                           #7939.13
        vpmullq   %zmm0, %zmm14, %zmm7                          #7940.13
        vpmullq   %zmm5, %zmm14, %zmm5                          #7941.13
        vpmullq   %zmm9, %zmm14, %zmm9                          #7942.13
        vpmullq   %zmm12, %zmm14, %zmm0                         #7943.13
        vpmullq   %zmm13, %zmm14, %zmm12                        #7944.13
        vpsrlq    $32, %zmm9, %zmm15                            #7948.13
        vpsrlq    $32, %zmm0, %zmm13                            #7947.13
        vpsrlq    $32, %zmm12, %zmm14                           #7946.13
        vpsrlq    $32, %zmm5, %zmm12                            #7949.13
        vpaddq    %zmm15, %zmm3, %zmm15                         #7952.11
        vpaddq    %zmm14, %zmm1, %zmm1                          #7950.11
        vpaddq    %zmm12, %zmm4, %zmm3                          #7953.11
        vpaddq    %zmm13, %zmm2, %zmm2                          #7951.11
        vpandq    %zmm6, %zmm0, %zmm13                          #7954.11
        vpandq    %zmm6, %zmm7, %zmm7                           #7957.11
        vpaddq    %zmm1, %zmm13, %zmm4                          #7959.11
        vpaddq    %zmm3, %zmm7, %zmm13                          #7962.11
        vpsrlq    $32, %zmm8, %zmm3                             #7964.13
        vpaddq    %zmm4, %zmm3, %zmm14                          #7965.11
        vpsrlq    $32, %zmm14, %zmm8                            #7966.13
        vpandq    %zmm6, %zmm9, %zmm0                           #7955.11
        vpaddq    %zmm2, %zmm0, %zmm9                           #7960.11
        vpaddq    %zmm9, %zmm8, %zmm3                           #7967.11
        vpsrlq    $32, %zmm3, %zmm1                             #7968.13
        vpandq    %zmm6, %zmm5, %zmm5                           #7956.11
        vpaddq    %zmm15, %zmm5, %zmm2                          #7961.11
        vpaddq    %zmm2, %zmm1, %zmm15                          #7969.11
        vpbroadcastq .L_2il0floatpacket.65(%rip), %zmm1         #7985.36
        vpsrlq    $32, %zmm15, %zmm12                           #7970.13
        vpaddq    %zmm13, %zmm12, %zmm0                         #7971.11
        vpandq    %zmm6, %zmm14, %zmm8                          #7972.11
        vpandq    %zmm6, %zmm15, %zmm7                          #7973.11
        vpsllq    $32, %zmm0, %zmm6                             #7974.19
        vpsllq    $32, %zmm3, %zmm0                             #7975.19
        vpaddq    %zmm7, %zmm6, %zmm4                           #7976.17
        vpaddq    %zmm8, %zmm0, %zmm5                           #7977.17
        vpsrlq    $12, %zmm4, %zmm3                             #7982.20
        vpandq    .L_2il0floatpacket.63(%rip){1to8}, %zmm11, %zmm9 #7979.15
        vpxorq    .L_2il0floatpacket.64(%rip){1to8}, %zmm9, %zmm6 #7981.20
        vporq     %zmm6, %zmm3, %zmm2                           #7983.18
        vaddpd    {rn-sae}, %zmm2, %zmm1, %zmm13                #7986.21
        vsubpd    {rn-sae}, %zmm1, %zmm13, %zmm12               #7987.11
        vsubpd    {rn-sae}, %zmm12, %zmm2, %zmm8                #7988.19
        vpandq    .L_2il0floatpacket.68(%rip){1to8}, %zmm5, %zmm14 #7995.18
        vpsllq    $28, %zmm14, %zmm15                           #7996.20
        vpsrlq    $24, %zmm5, %zmm5                             #8006.19
        vandpd    .L_2il0floatpacket.73(%rip){1to8}, %zmm11, %zmm14 #8024.11
        vpandq    .L_2il0floatpacket.70(%rip){1to8}, %zmm4, %zmm4 #8004.18
        vpxorq    .L_2il0floatpacket.67(%rip){1to8}, %zmm9, %zmm3 #7993.20
        vpxorq    .L_2il0floatpacket.69(%rip){1to8}, %zmm9, %zmm1 #8002.20
        vpsllq    $40, %zmm4, %zmm9                             #8005.20
        vporq     %zmm3, %zmm15, %zmm0                          #7997.18
        vsubpd    {rn-sae}, %zmm3, %zmm0, %zmm6                 #8000.19
        vporq     %zmm5, %zmm9, %zmm0                           #8007.18
        vporq     %zmm1, %zmm0, %zmm3                           #8008.18
        vsubpd    {rn-sae}, %zmm1, %zmm3, %zmm2                 #8011.20
        vpbroadcastq .L_2il0floatpacket.71(%rip), %zmm5         #8016.37
        vaddpd    {rn-sae}, %zmm2, %zmm8, %zmm12                #8012.13
        vmulpd    {rn-sae}, %zmm5, %zmm12, %zmm15               #8018.15
        vsubpd    {rn-sae}, %zmm12, %zmm8, %zmm8                #8013.19
        vaddpd    {rn-sae}, %zmm8, %zmm2, %zmm4                 #8014.20
        vmovaps   %zmm5, %zmm9                                  #8019.13
        vfmsub213pd {rn-sae}, %zmm15, %zmm12, %zmm9             #8019.13
        vaddpd    {rn-sae}, %zmm6, %zmm4, %zmm1                 #8015.13
        vpbroadcastq .L_2il0floatpacket.72(%rip), %zmm6         #8017.38
        vmovaps   %zmm10, %zmm3                                 #8025.38
        vfmadd213pd {rn-sae}, %zmm9, %zmm6, %zmm12              #8020.13
        vpbroadcastq .L_2il0floatpacket.76(%rip), %zmm9         #8045.40
        vfmadd213pd {rn-sae}, %zmm12, %zmm5, %zmm1              #8021.13
        vpbroadcastq .L_2il0floatpacket.77(%rip), %zmm12        #8046.40
        vpandq    .L_2il0floatpacket.66(%rip){1to8}, %zmm13, %zmm7 #7991.13
        vpbroadcastq .L_2il0floatpacket.74(%rip), %zmm13        #8023.37
        vpmovqd   %zmm7, %ymm5                                  #8032.21
        vcmppd    $26, {sae}, %zmm13, %zmm14, %k6               #8025.114
        vcmppd    $22, {sae}, %zmm13, %zmm14, %k7               #8026.117
        vpandnq   %zmm14, %zmm14, %zmm3{%k6}                    #8025.38
        vpandnq   %zmm14, %zmm14, %zmm10{%k7}                   #8026.41
        vandpd    %zmm11, %zmm10, %zmm10                        #8027.24
        vandpd    %zmm15, %zmm3, %zmm0                          #8028.25
        vorpd     %zmm0, %zmm10, %zmm0                          #8029.9
        vandpd    %zmm1, %zmm3, %zmm10                          #8030.9
        vpsrlq    $32, %zmm0, %zmm7                             #8035.42
        vpmovqd   %zmm7, %ymm3                                  #8035.20
        vmovdqu   .L_2il0floatpacket.75(%rip), %ymm7            #8037.21
        vpsrld    $31, %ymm3, %ymm1                             #8036.22
        vmovups   64(%rsp), %zmm3                               #8053.17[spill]
        vpsubd    %ymm1, %ymm7, %ymm2                           #8037.21
        vpaddd    %ymm2, %ymm5, %ymm4                           #8038.21
        vpsrld    $4, %ymm4, %ymm13                             #8039.23
        vpslld    $4, %ymm13, %ymm6                             #8042.23
        vpsubd    %ymm6, %ymm5, %ymm8                           #8043.21
        vmovaps   %zmm0, %zmm14                                 #8047.26
        vcvtdq2pd %ymm8, %zmm15                                 #8044.23
        vfmadd231pd {rn-sae}, %zmm15, %zmm9, %zmm14             #8047.26
        vfnmadd213pd {rn-sae}, %zmm14, %zmm15, %zmm9            #8048.27
        vblendmpd %zmm14, %zmm3, %zmm3{%k1}                     #8053.17
        vsubpd    {rn-sae}, %zmm9, %zmm0, %zmm0                 #8049.29
        vfmadd213pd {rn-sae}, %zmm0, %zmm12, %zmm15             #8050.27
        vmovups   (%rsp), %zmm0                                 #8052.17[spill]
        vmovups   128(%rsp), %zmm12                             #8054.18[spill]
        vpmovzxdq %ymm13, %zmm0{%k1}                            #8052.17
        vaddpd    {rn-sae}, %zmm10, %zmm15, %zmm12{%k1}         #8054.18
        jmp       ..B5.2        # Prob 100%                     #8054.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm3 zmm11 zmm12
	.cfi_endproc
# mark_end;
	.type	__svml_tan8_ha_z0,@function
	.size	__svml_tan8_ha_z0,.-__svml_tan8_ha_z0
..LN__svml_tan8_ha_z0.4:
	.data
# -- End  __svml_tan8_ha_z0
	.text
.L_2__routine_start___svml_tan2_ha_ex_5:
# -- Begin  __svml_tan2_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tan2_ha_ex
# --- __svml_tan2_ha_ex(__m128d)
__svml_tan2_ha_ex:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #8103.1
        .byte     15                                            #8532.22
        .byte     30                                            #8532.22
        .byte     250                                           #8532.22
	.cfi_startproc
..___tag_value___svml_tan2_ha_ex.176:
..L177:
                                                        #8103.1
        pushq     %rbp                                          #8103.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #8103.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #8103.1
        subq      $192, %rsp                                    #8103.1
        xorl      %edx, %edx                                    #8162.9
        movups    1024+__svml_dtan_ha_data_internal(%rip), %xmm8 #8163.54
        movaps    %xmm8, %xmm14                                 #8164.20
        andnps    %xmm0, %xmm8                                  #8165.21
        movups    1472+__svml_dtan_ha_data_internal(%rip), %xmm6 #8172.29
        andps     %xmm0, %xmm14                                 #8164.20
        mulpd     %xmm14, %xmm6                                 #8172.29
        movups    1152+__svml_dtan_ha_data_internal(%rip), %xmm7 #8171.53
        movaps    %xmm14, %xmm3                                 #8167.34
        addpd     %xmm7, %xmm6                                  #8172.17
        cmpnlepd  28160+__svml_dtan_ha_data_internal(%rip), %xmm3 #8167.34
        movaps    %xmm6, %xmm4                                  #8173.17
        subpd     %xmm7, %xmm4                                  #8173.17
        movups    1216+__svml_dtan_ha_data_internal(%rip), %xmm5 #8175.40
        mulpd     %xmm4, %xmm5                                  #8175.40
        movups    1280+__svml_dtan_ha_data_internal(%rip), %xmm9 #8177.23
        mulpd     %xmm4, %xmm9                                  #8177.23
        movups    %xmm14, (%rsp)                                #8164.20[spill]
        subpd     %xmm5, %xmm14                                 #8175.18
        movaps    %xmm14, %xmm12                                #8178.22
        subpd     %xmm9, %xmm12                                 #8178.22
        subpd     %xmm12, %xmm14                                #8179.21
        movups    1344+__svml_dtan_ha_data_internal(%rip), %xmm11 #8182.36
        mulpd     %xmm4, %xmm11                                 #8182.36
        subpd     %xmm9, %xmm14                                 #8180.21
        pand      1408+__svml_dtan_ha_data_internal(%rip), %xmm6 #8185.21
        pand      .L_2il0floatpacket.26(%rip), %xmm6            #8190.21
        subpd     %xmm11, %xmm14                                #8182.17
        pextrw    $4, %xmm6, %esi                               #8191.192
        movd      %xmm6, %eax                                   #8191.136
        movups    %xmm8, 64(%rsp)                               #8165.21[spill]
        movaps    %xmm12, %xmm8                                 #8186.17
        imull     $104, %eax, %ecx                              #8191.294
        lea       1632+__svml_dtan_ha_data_internal(%rip), %rax #8191.380
        imull     $104, %esi, %edi                              #8191.346
        movups    %xmm0, 80(%rsp)                               #8103.1[spill]
        movups    %xmm3, 96(%rsp)                               #8167.34[spill]
        addpd     %xmm14, %xmm8                                 #8186.17
        movq      -96(%rcx,%rax), %xmm1                         #8191.616
        movhpd    -96(%rdi,%rax), %xmm1                         #8191.584
        movaps    %xmm1, %xmm15                                 #8204.22
        subpd     %xmm8, %xmm15                                 #8204.22
        subpd     %xmm8, %xmm12                                 #8187.18
        subpd     %xmm15, %xmm1                                 #8207.20
        addpd     %xmm12, %xmm14                                #8188.17
        subpd     %xmm8, %xmm1                                  #8208.20
        movq      (%rcx,%rax), %xmm13                           #8203.614
        movhpd    (%rdi,%rax), %xmm13                           #8203.582
        movups    %xmm13, 48(%rsp)                              #8203.582[spill]
        movups    .L_2il0floatpacket.27(%rip), %xmm13           #8205.24
        movaps    %xmm13, %xmm0                                 #8206.20
        andps     %xmm15, %xmm0                                 #8206.20
        subpd     %xmm0, %xmm15                                 #8210.22
        subpd     %xmm14, %xmm1                                 #8209.20
        movq      -88(%rcx,%rax), %xmm2                         #8192.616
        movhpd    -88(%rdi,%rax), %xmm2                         #8192.584
        addpd     %xmm15, %xmm2                                 #8211.22
        addpd     %xmm2, %xmm1                                  #8212.20
        cvtpd2ps  %xmm0, %xmm2                                  #8213.64
        movlhps   %xmm2, %xmm2                                  #8213.48
        rcpps     %xmm2, %xmm15                                 #8213.37
        cvtps2pd  %xmm15, %xmm2                                 #8213.24
        andps     %xmm13, %xmm2                                 #8214.24
        mulpd     %xmm2, %xmm0                                  #8216.39
        movups    %xmm14, 16(%rsp)                              #8188.17[spill]
        movaps    %xmm2, %xmm13                                 #8217.32
        movups    .L_2il0floatpacket.28(%rip), %xmm14           #8215.37
        movaps    %xmm14, %xmm15                                #8216.18
        subpd     %xmm0, %xmm15                                 #8216.18
        movaps    %xmm15, %xmm0                                 #8218.30
        mulpd     %xmm15, %xmm13                                #8217.32
        mulpd     %xmm15, %xmm0                                 #8218.30
        addpd     %xmm2, %xmm13                                 #8217.20
        addpd     %xmm14, %xmm0                                 #8218.18
        mulpd     %xmm0, %xmm13                                 #8219.24
        mulpd     %xmm13, %xmm1                                 #8220.20
        movaps    %xmm1, %xmm14                                 #8221.19
        mulpd     %xmm1, %xmm1                                  #8222.36
        subpd     %xmm15, %xmm14                                #8221.19
        subpd     %xmm14, %xmm1                                 #8222.24
        movq      -56(%rcx,%rax), %xmm9                         #8196.617
        movhpd    -56(%rdi,%rax), %xmm9                         #8196.585
        mulpd     %xmm1, %xmm13                                 #8223.24
        movaps    %xmm8, %xmm1                                  #8226.22
        mulpd     %xmm9, %xmm1                                  #8226.22
        movq      -80(%rcx,%rax), %xmm3                         #8193.615
        movq      -72(%rcx,%rax), %xmm12                        #8194.617
        movhpd    -80(%rdi,%rax), %xmm3                         #8193.583
        movhpd    -72(%rdi,%rax), %xmm12                        #8194.585
        mulpd     %xmm3, %xmm2                                  #8224.28
        mulpd     %xmm13, %xmm3                                 #8225.28
        movaps    %xmm12, %xmm13                                #8227.22
        addpd     %xmm1, %xmm13                                 #8227.22
        movaps    %xmm13, %xmm0                                 #8229.22
        addpd     %xmm2, %xmm0                                  #8229.22
        subpd     %xmm13, %xmm12                                #8228.22
        subpd     %xmm0, %xmm2                                  #8231.22
        addpd     %xmm12, %xmm1                                 #8230.22
        addpd     %xmm2, %xmm13                                 #8233.22
        addpd     %xmm1, %xmm3                                  #8232.22
        movq      -40(%rcx,%rax), %xmm7                         #8198.614
        movaps    %xmm8, %xmm2                                  #8238.18
        movq      -24(%rcx,%rax), %xmm6                         #8200.614
        movhpd    -40(%rdi,%rax), %xmm7                         #8198.582
        movq      -64(%rcx,%rax), %xmm10                        #8195.617
        movhpd    -24(%rdi,%rax), %xmm6                         #8200.582
        mulpd     %xmm8, %xmm7                                  #8235.30
        mulpd     %xmm8, %xmm6                                  #8236.30
        mulpd     %xmm8, %xmm2                                  #8238.18
        addpd     %xmm13, %xmm3                                 #8234.22
        movhpd    -64(%rdi,%rax), %xmm10                        #8195.585
        movups    %xmm10, 32(%rsp)                              #8195.585[spill]
        movq      -8(%rcx,%rax), %xmm10                         #8202.614
        movhpd    -8(%rdi,%rax), %xmm10                         #8202.582
        mulpd     %xmm8, %xmm10                                 #8237.30
        movq      -48(%rcx,%rax), %xmm11                        #8197.617
        movq      -32(%rcx,%rax), %xmm4                         #8199.614
        movhpd    -48(%rdi,%rax), %xmm11                        #8197.585
        movhpd    -32(%rdi,%rax), %xmm4                         #8199.582
        addpd     %xmm11, %xmm7                                 #8235.18
        addpd     %xmm11, %xmm9                                 #8239.18
        addpd     %xmm6, %xmm4                                  #8236.18
        movups    48(%rsp), %xmm11                              #8240.30[spill]
        mulpd     %xmm2, %xmm11                                 #8240.30
        mulpd     %xmm2, %xmm4                                  #8241.31
        mulpd     %xmm2, %xmm2                                  #8242.18
        addpd     %xmm4, %xmm7                                  #8241.19
        movq      -16(%rcx,%rax), %xmm5                         #8201.614
        movhpd    -16(%rdi,%rax), %xmm5                         #8201.582
        addpd     %xmm10, %xmm5                                 #8237.18
        addpd     %xmm11, %xmm5                                 #8240.18
        mulpd     %xmm2, %xmm5                                  #8244.31
        movups    16(%rsp), %xmm4                               #8243.31[spill]
        mulpd     %xmm9, %xmm4                                  #8243.31
        addpd     %xmm5, %xmm7                                  #8244.19
        addpd     %xmm4, %xmm3                                  #8243.19
        mulpd     %xmm7, %xmm8                                  #8246.31
        movups    32(%rsp), %xmm5                               #8245.19[spill]
        addpd     %xmm3, %xmm5                                  #8245.19
        addpd     %xmm8, %xmm5                                  #8246.19
        addpd     %xmm5, %xmm0                                  #8247.18
        movups    96(%rsp), %xmm1                               #8169.82[spill]
        movmskpd  %xmm1, %r8d                                   #8169.82
        xorps     64(%rsp), %xmm0                               #8248.18[spill]
        movups    (%rsp), %xmm4                                 #8249.70[spill]
        testl     %r8d, %r8d                                    #8249.70
        jne       ..B6.12       # Prob 5%                       #8249.70
                                # LOE rax rbx r12 r13 r14 r15 edx xmm0 xmm1 xmm4
..B6.2:                         # Preds ..B6.12 ..B6.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #8535.52
        jne       ..B6.4        # Prob 5%                       #8535.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B6.3:                         # Preds ..B6.4 ..B6.10 ..B6.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #8538.12
        popq      %rbp                                          #8538.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #8538.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.4:                         # Preds ..B6.2
                                # Execution count [5.00e-02]: Infreq
        movups    80(%rsp), %xmm1                               #8535.197[spill]
        movups    %xmm1, 64(%rsp)                               #8535.197
        movups    %xmm0, 128(%rsp)                              #8535.270
        je        ..B6.3        # Prob 95%                      #8535.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B6.7:                         # Preds ..B6.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #8535.454
        movq      %r12, 8(%rsp)                                 #8535.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #8535.454
        movq      %r13, (%rsp)                                  #8535.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #8535.454
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.9 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #8535.517
        jc        ..B6.11       # Prob 5%                       #8535.517
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.11 ..B6.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #8535.470
        cmpl      $2, %r12d                                     #8535.465
        jl        ..B6.8        # Prob 82%                      #8535.465
                                # LOE rbx r12 r14 r15 r13d
..B6.10:                        # Preds ..B6.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #8535.673
        jmp       ..B6.3        # Prob 100%                     #8535.673
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B6.11:                        # Preds ..B6.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #8535.546
        lea       128(%rsp,%r12,8), %rsi                        #8535.546
..___tag_value___svml_tan2_ha_ex.208:
#       __svml_dtan_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtan_ha_cout_rare_internal             #8535.546
..___tag_value___svml_tan2_ha_ex.209:
        jmp       ..B6.9        # Prob 100%                     #8535.546
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B6.12:                        # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, (%rsp)                                 #[spill]
        lea       __svml_dtan_ha_reduction_data_internal(%rip), %r9 #8344.372
        movdqu    .L_2il0floatpacket.29(%rip), %xmm0            #8342.16
        pand      %xmm4, %xmm0                                  #8342.16
        psrlq     $52, %xmm0                                    #8343.16
        movd      %xmm0, %ecx                                   #8344.124
        movups    %xmm1, 96(%rsp)                               #[spill]
        movdqu    .L_2il0floatpacket.30(%rip), %xmm1            #8352.19
        pextrw    $4, %xmm0, %edi                               #8344.183
        pand      %xmm4, %xmm1                                  #8352.19
        lea       (%rcx,%rcx,2), %esi                           #8344.287
        shll      $3, %esi                                      #8344.287
        lea       (%rdi,%rdi,2), %r8d                           #8344.338
        movups    1088+__svml_dtan_ha_data_internal(%rip), %xmm15 #8252.59
        paddq     .L_2il0floatpacket.31(%rip), %xmm1            #8353.19
        shll      $3, %r8d                                      #8344.338
        movaps    %xmm15, %xmm3                                 #8253.21
        movq      16(%rsi,%r9), %xmm11                          #8346.626
        andps     %xmm4, %xmm3                                  #8253.21
        movhpd    16(%r8,%r9), %xmm11                           #8346.594
        movdqa    %xmm1, %xmm7                                  #8361.10
        cmpeqpd   %xmm15, %xmm3                                 #8254.29
        movdqu    .L_2il0floatpacket.32(%rip), %xmm15           #8354.15
        movaps    %xmm11, %xmm10                                #8359.10
        psrlq     $32, %xmm7                                    #8361.10
        psrlq     $32, %xmm10                                   #8359.10
        movq      8(%rsi,%r9), %xmm9                            #8345.627
        pand      %xmm15, %xmm1                                 #8362.10
        movdqa    %xmm7, %xmm5                                  #8367.11
        movdqa    %xmm1, %xmm14                                 #8372.11
        movhpd    8(%r8,%r9), %xmm9                             #8345.595
        pand      %xmm15, %xmm11                                #8360.10
        pmuludq   %xmm10, %xmm5                                 #8367.11
        movaps    %xmm9, %xmm8                                  #8357.10
        pmuludq   %xmm1, %xmm10                                 #8373.11
        pmuludq   %xmm7, %xmm11                                 #8368.11
        movmskpd  %xmm3, %edx                                   #8256.52
        pand      %xmm15, %xmm9                                 #8358.10
        psrlq     $32, %xmm10                                   #8375.11
        pmuludq   %xmm9, %xmm14                                 #8372.11
        psrlq     $32, %xmm8                                    #8357.10
        pmuludq   %xmm7, %xmm9                                  #8366.11
        paddq     %xmm10, %xmm5                                 #8379.11
        movdqa    %xmm1, %xmm12                                 #8371.11
        movdqa    %xmm15, %xmm3                                 #8383.11
        pmuludq   %xmm8, %xmm12                                 #8371.11
        pand      %xmm14, %xmm3                                 #8383.11
        pmuludq   %xmm7, %xmm8                                  #8365.11
        paddq     %xmm5, %xmm3                                  #8388.11
        movq      (%rsi,%r9), %xmm13                            #8344.626
        movdqa    %xmm15, %xmm6                                 #8356.10
        movhpd    (%r8,%r9), %xmm13                             #8344.594
        psrlq     $32, %xmm11                                   #8393.11
        psrlq     $32, %xmm14                                   #8376.11
        pand      %xmm13, %xmm6                                 #8356.10
        movdqa    %xmm1, %xmm2                                  #8370.11
        movdqa    %xmm15, %xmm0                                 #8384.11
        paddq     %xmm3, %xmm11                                 #8394.11
        paddq     %xmm14, %xmm9                                 #8380.11
        pmuludq   %xmm6, %xmm2                                  #8370.11
        pmuludq   %xmm6, %xmm7                                  #8364.11
        pand      %xmm12, %xmm0                                 #8384.11
        movaps    %xmm11, %xmm3                                 #8395.11
        paddq     %xmm9, %xmm0                                  #8389.11
        psrlq     $32, %xmm3                                    #8395.11
        psrlq     $32, %xmm12                                   #8377.11
        psrlq     $32, %xmm13                                   #8355.10
        movdqa    %xmm15, %xmm14                                #8385.11
        paddq     %xmm0, %xmm3                                  #8396.11
        paddq     %xmm12, %xmm8                                 #8381.11
        pmuludq   %xmm13, %xmm1                                 #8369.11
        pand      %xmm2, %xmm14                                 #8385.11
        movdqa    %xmm3, %xmm9                                  #8397.11
        paddq     %xmm8, %xmm14                                 #8390.11
        psrlq     $32, %xmm9                                    #8397.11
        psrlq     $32, %xmm2                                    #8378.11
        paddq     %xmm14, %xmm9                                 #8398.11
        paddq     %xmm2, %xmm7                                  #8382.11
        pand      %xmm15, %xmm1                                 #8386.11
        movdqa    %xmm9, %xmm0                                  #8399.11
        paddq     %xmm7, %xmm1                                  #8391.11
        psrlq     $32, %xmm0                                    #8399.11
        pand      %xmm15, %xmm9                                 #8402.11
        paddq     %xmm1, %xmm0                                  #8400.11
        psllq     $32, %xmm0                                    #8403.17
        pand      %xmm15, %xmm11                                #8401.11
        paddq     %xmm9, %xmm0                                  #8405.17
        movups    .L_2il0floatpacket.33(%rip), %xmm15           #8408.15
        movdqa    %xmm0, %xmm14                                 #8411.18
        movups    .L_2il0floatpacket.28(%rip), %xmm2            #8410.20
        andps     %xmm4, %xmm15                                 #8408.15
        movaps    %xmm2, %xmm12                                 #8410.20
        psrlq     $12, %xmm14                                   #8411.18
        movups    .L_2il0floatpacket.34(%rip), %xmm8            #8414.15
        pxor      %xmm15, %xmm12                                #8410.20
        orps      %xmm12, %xmm14                                #8412.18
        movaps    %xmm8, %xmm13                                 #8415.19
        addpd     %xmm14, %xmm13                                #8415.19
        psllq     $32, %xmm3                                    #8404.17
        paddq     %xmm11, %xmm3                                 #8406.17
        movaps    %xmm13, %xmm11                                #8416.9
        subpd     %xmm8, %xmm11                                 #8416.9
        movdqu    .L_2il0floatpacket.38(%rip), %xmm9            #8433.18
        movdqu    .L_2il0floatpacket.36(%rip), %xmm1            #8424.18
        pand      %xmm0, %xmm9                                  #8433.18
        movups    .L_2il0floatpacket.35(%rip), %xmm7            #8422.20
        pand      %xmm3, %xmm1                                  #8424.18
        psllq     $40, %xmm9                                    #8434.18
        psrlq     $24, %xmm3                                    #8435.17
        pxor      %xmm15, %xmm7                                 #8422.20
        por       %xmm3, %xmm9                                  #8436.18
        xorps     .L_2il0floatpacket.37(%rip), %xmm15           #8431.20
        psllq     $28, %xmm1                                    #8425.18
        subpd     %xmm11, %xmm14                                #8417.17
        orps      %xmm15, %xmm9                                 #8437.18
        movaps    %xmm14, %xmm3                                 #8441.11
        subpd     %xmm15, %xmm9                                 #8440.18
        addpd     %xmm9, %xmm3                                  #8441.11
        subpd     %xmm3, %xmm14                                 #8442.17
        orps      %xmm7, %xmm1                                  #8426.18
        subpd     %xmm7, %xmm1                                  #8429.17
        addpd     %xmm14, %xmm9                                 #8443.18
        movups    .L_2il0floatpacket.27(%rip), %xmm5            #8449.16
        movaps    %xmm5, %xmm0                                  #8449.16
        andps     %xmm3, %xmm0                                  #8449.16
        addpd     %xmm9, %xmm1                                  #8444.11
        subpd     %xmm0, %xmm3                                  #8450.17
        mulpd     .L_2il0floatpacket.39(%rip), %xmm1            #8454.12
        movups    .L_2il0floatpacket.40(%rip), %xmm6            #8446.16
        movaps    %xmm0, %xmm12                                 #8451.16
        movups    .L_2il0floatpacket.41(%rip), %xmm7            #8447.17
        mulpd     %xmm6, %xmm12                                 #8451.16
        mulpd     %xmm3, %xmm6                                  #8452.12
        mulpd     %xmm7, %xmm0                                  #8453.12
        mulpd     %xmm3, %xmm7                                  #8455.12
        addpd     %xmm0, %xmm6                                  #8456.13
        addpd     %xmm7, %xmm1                                  #8457.13
        addpd     %xmm1, %xmm6                                  #8458.13
        movaps    %xmm6, %xmm8                                  #8459.13
        addpd     %xmm12, %xmm8                                 #8459.13
        subpd     %xmm8, %xmm12                                 #8460.16
        pand      .L_2il0floatpacket.44(%rip), %xmm13           #8420.13
        pand      .L_2il0floatpacket.26(%rip), %xmm13           #8473.25
        movups    .L_2il0floatpacket.43(%rip), %xmm15           #8463.16
        movups    .L_2il0floatpacket.42(%rip), %xmm1            #8464.11
        movaps    %xmm15, %xmm0                                 #8465.17
        movd      %xmm13, %r10d                                 #8474.140
        andps     %xmm4, %xmm1                                  #8464.11
        pextrw    $4, %xmm13, %ecx                              #8474.196
        addpd     %xmm12, %xmm6                                 #8461.13
        cmpltpd   %xmm1, %xmm0                                  #8465.17
        cmplepd   %xmm15, %xmm1                                 #8466.20
        imull     $104, %r10d, %r11d                            #8474.298
        imull     $104, %ecx, %esi                              #8474.350
        andps     %xmm0, %xmm8                                  #8468.25
        andps     %xmm6, %xmm0                                  #8470.9
        movq      -96(%r11,%rax), %xmm7                         #8474.620
        andps     %xmm4, %xmm1                                  #8467.24
        movq      (%r11,%rax), %xmm6                            #8486.618
        orps      %xmm8, %xmm1                                  #8469.9
        movhpd    -96(%rsi,%rax), %xmm7                         #8474.588
        movhpd    (%rsi,%rax), %xmm6                            #8486.586
        movups    %xmm6, 48(%rsp)                               #8486.586[spill]
        movaps    %xmm7, %xmm6                                  #8487.26
        subpd     %xmm1, %xmm6                                  #8487.26
        subpd     %xmm6, %xmm7                                  #8490.24
        movq      -64(%r11,%rax), %xmm4                         #8478.621
        movhpd    -64(%rsi,%rax), %xmm4                         #8478.589
        movups    %xmm4, 16(%rsp)                               #8478.589[spill]
        movaps    %xmm5, %xmm4                                  #8489.24
        andps     %xmm6, %xmm4                                  #8489.24
        subpd     %xmm1, %xmm7                                  #8491.24
        subpd     %xmm4, %xmm6                                  #8493.26
        subpd     %xmm0, %xmm7                                  #8492.24
        movq      -88(%r11,%rax), %xmm10                        #8475.620
        movhpd    -88(%rsi,%rax), %xmm10                        #8475.588
        addpd     %xmm6, %xmm10                                 #8494.26
        addpd     %xmm10, %xmm7                                 #8495.24
        cvtpd2ps  %xmm4, %xmm10                                 #8496.68
        movlhps   %xmm10, %xmm10                                #8496.52
        movaps    %xmm2, %xmm6                                  #8499.22
        rcpps     %xmm10, %xmm10                                #8496.41
        cvtps2pd  %xmm10, %xmm10                                #8496.28
        andps     %xmm5, %xmm10                                 #8497.28
        mulpd     %xmm10, %xmm4                                 #8499.43
        subpd     %xmm4, %xmm6                                  #8499.22
        movaps    %xmm10, %xmm4                                 #8500.36
        movaps    %xmm6, %xmm5                                  #8501.34
        mulpd     %xmm6, %xmm4                                  #8500.36
        mulpd     %xmm6, %xmm5                                  #8501.34
        addpd     %xmm10, %xmm4                                 #8500.24
        addpd     %xmm5, %xmm2                                  #8501.22
        mulpd     %xmm2, %xmm4                                  #8502.28
        mulpd     %xmm4, %xmm7                                  #8503.24
        movaps    %xmm7, %xmm5                                  #8504.23
        movaps    %xmm1, %xmm2                                  #8509.26
        mulpd     %xmm7, %xmm7                                  #8505.40
        subpd     %xmm6, %xmm5                                  #8504.23
        subpd     %xmm5, %xmm7                                  #8505.28
        movq      -56(%r11,%rax), %xmm13                        #8479.621
        movhpd    -56(%rsi,%rax), %xmm13                        #8479.589
        mulpd     %xmm7, %xmm4                                  #8506.28
        mulpd     %xmm13, %xmm2                                 #8509.26
        movq      -80(%r11,%rax), %xmm15                        #8476.619
        movq      -72(%r11,%rax), %xmm8                         #8477.621
        movhpd    -80(%rsi,%rax), %xmm15                        #8476.587
        movhpd    -72(%rsi,%rax), %xmm8                         #8477.589
        mulpd     %xmm15, %xmm10                                #8507.32
        mulpd     %xmm4, %xmm15                                 #8508.32
        movaps    %xmm8, %xmm4                                  #8510.26
        addpd     %xmm2, %xmm4                                  #8510.26
        subpd     %xmm4, %xmm8                                  #8511.26
        movq      -40(%r11,%rax), %xmm3                         #8481.618
        movaps    %xmm4, %xmm7                                  #8512.26
        movq      -24(%r11,%rax), %xmm11                        #8483.618
        movhpd    -40(%rsi,%rax), %xmm3                         #8481.586
        movq      -16(%r11,%rax), %xmm12                        #8484.618
        movhpd    -24(%rsi,%rax), %xmm11                        #8483.586
        addpd     %xmm8, %xmm2                                  #8513.26
        addpd     %xmm10, %xmm7                                 #8512.26
        mulpd     %xmm1, %xmm3                                  #8518.34
        mulpd     %xmm1, %xmm11                                 #8519.34
        subpd     %xmm7, %xmm10                                 #8514.26
        addpd     %xmm2, %xmm15                                 #8515.26
        addpd     %xmm10, %xmm4                                 #8516.26
        movaps    %xmm1, %xmm8                                  #8521.22
        movhpd    -16(%rsi,%rax), %xmm12                        #8484.586
        mulpd     %xmm1, %xmm8                                  #8521.22
        addpd     %xmm4, %xmm15                                 #8517.26
        movups    %xmm12, 32(%rsp)                              #8484.586[spill]
        movq      -8(%r11,%rax), %xmm12                         #8485.618
        movhpd    -8(%rsi,%rax), %xmm12                         #8485.586
        mulpd     %xmm1, %xmm12                                 #8520.34
        movq      -48(%r11,%rax), %xmm9                         #8480.621
        movq      -32(%r11,%rax), %xmm14                        #8482.618
        movhpd    -48(%rsi,%rax), %xmm9                         #8480.589
        movhpd    -32(%rsi,%rax), %xmm14                        #8482.586
        addpd     %xmm9, %xmm3                                  #8518.22
        addpd     %xmm9, %xmm13                                 #8522.22
        addpd     %xmm11, %xmm14                                #8519.22
        mulpd     %xmm13, %xmm0                                 #8526.35
        mulpd     %xmm8, %xmm14                                 #8524.35
        addpd     %xmm0, %xmm15                                 #8526.23
        addpd     %xmm14, %xmm3                                 #8524.23
        movups    48(%rsp), %xmm9                               #8523.34[spill]
        mulpd     %xmm8, %xmm9                                  #8523.34
        mulpd     %xmm8, %xmm8                                  #8525.22
        movups    32(%rsp), %xmm11                              #8520.22[spill]
        addpd     %xmm12, %xmm11                                #8520.22
        addpd     %xmm9, %xmm11                                 #8523.22
        mulpd     %xmm8, %xmm11                                 #8527.35
        addpd     %xmm11, %xmm3                                 #8527.23
        mulpd     %xmm3, %xmm1                                  #8529.35
        movups    16(%rsp), %xmm0                               #8528.23[spill]
        addpd     %xmm15, %xmm0                                 #8528.23
        addpd     %xmm1, %xmm0                                  #8529.23
        addpd     %xmm0, %xmm7                                  #8530.28
        movups    96(%rsp), %xmm1                               #8532.33[spill]
        xorps     64(%rsp), %xmm7                               #8531.28[spill]
        movaps    %xmm1, %xmm0                                  #8532.33
        andnps    (%rsp), %xmm0                                 #8532.33[spill]
        andps     %xmm1, %xmm7                                  #8532.82
        orps      %xmm7, %xmm0                                  #8532.22
        jmp       ..B6.2        # Prob 100%                     #8532.22
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm0
	.cfi_endproc
# mark_end;
	.type	__svml_tan2_ha_ex,@function
	.size	__svml_tan2_ha_ex,.-__svml_tan2_ha_ex
..LN__svml_tan2_ha_ex.5:
	.data
# -- End  __svml_tan2_ha_ex
	.text
.L_2__routine_start___svml_tan2_ha_l9_6:
# -- Begin  __svml_tan2_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tan2_ha_l9
# --- __svml_tan2_ha_l9(__m128d)
__svml_tan2_ha_l9:
# parameter 1: %xmm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #8543.1
        .byte     15                                            #8863.18
        .byte     30                                            #8863.18
        .byte     250                                           #8863.18
	.cfi_startproc
..___tag_value___svml_tan2_ha_l9.224:
..L225:
                                                        #8543.1
        pushq     %rbp                                          #8543.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #8543.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #8543.1
        subq      $192, %rsp                                    #8543.1
        vmovapd   %xmm0, %xmm14                                 #8543.1
        vmovupd   1152+__svml_dtan_ha_data_internal(%rip), %xmm3 #8615.55
        xorl      %eax, %eax                                    #8603.5
        vmovupd   __svml_dtan_ha_data_internal(%rip), %xmm2     #8616.17
        vfmadd213pd %xmm3, %xmm14, %xmm2                        #8616.17
        vmovupd   64+__svml_dtan_ha_data_internal(%rip), %xmm1  #8618.50
        vmovupd   256+__svml_dtan_ha_data_internal(%rip), %xmm5 #8619.54
        vmovupd   320+__svml_dtan_ha_data_internal(%rip), %xmm6 #8620.54
        vandpd    1024+__svml_dtan_ha_data_internal(%rip), %xmm14, %xmm10 #8605.16
        vsubpd    %xmm3, %xmm2, %xmm9                           #8617.17
        vcmpnlepd 28160+__svml_dtan_ha_data_internal(%rip), %xmm10, %xmm0 #8609.30
        vfnmadd213pd %xmm14, %xmm9, %xmm1                       #8621.22
        vmovmskpd %xmm0, %edx                                   #8611.78
        vmovapd   %xmm1, %xmm4                                  #8622.22
        vfnmadd231pd %xmm9, %xmm5, %xmm4                        #8622.22
        vmovapd   %xmm4, %xmm3                                  #8623.21
        vfnmadd231pd %xmm9, %xmm6, %xmm3                        #8623.21
        vsubpd    %xmm4, %xmm1, %xmm8                           #8624.23
        vsubpd    %xmm4, %xmm3, %xmm7                           #8625.23
        vfnmadd231pd %xmm9, %xmm5, %xmm8                        #8626.22
        vfmadd213pd %xmm7, %xmm6, %xmm9                         #8627.23
        vsubpd    %xmm9, %xmm8, %xmm1                           #8628.22
        testl     %edx, %edx                                    #8630.66
        jne       ..B7.12       # Prob 5%                       #8630.66
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1 xmm2 xmm3 xmm10 xmm14
..B7.2:                         # Preds ..B7.12 ..B7.1
                                # Execution count [1.00e+00]
        vmulpd    %xmm3, %xmm3, %xmm9                           #8868.14
        vmovupd   896+__svml_dtan_ha_data_internal(%rip), %xmm8 #8869.52
        vpsllq    $3, %xmm2, %xmm4                              #8865.226
        vfmadd213pd 832+__svml_dtan_ha_data_internal(%rip), %xmm9, %xmm8 #8871.20
        lea       512+__svml_dtan_ha_data_internal(%rip), %rsi  #8866.31
        vpand     .L_2il0floatpacket.57(%rip), %xmm4, %xmm5     #8865.275
        vmovd     %xmm5, %edx                                   #8866.121
        vfmadd213pd 768+__svml_dtan_ha_data_internal(%rip), %xmm9, %xmm8 #8873.20
        vfmadd213pd 704+__svml_dtan_ha_data_internal(%rip), %xmm9, %xmm8 #8875.16
        vfmadd213pd 640+__svml_dtan_ha_data_internal(%rip), %xmm9, %xmm8 #8877.16
        vmulpd    %xmm8, %xmm3, %xmm10                          #8878.16
        vfnmsub213pd %xmm1, %xmm9, %xmm10                       #8879.20
        movslq    %edx, %rdx                                    #8866.121
        vpextrd   $2, %xmm5, %ecx                               #8866.246
        vsubpd    %xmm10, %xmm3, %xmm4                          #8880.17
        movslq    %ecx, %rcx                                    #8866.246
        vmovsd    -128(%rdx,%rsi), %xmm6                        #8866.31
        vmovupd   960+__svml_dtan_ha_data_internal(%rip), %xmm5 #8888.46
        vsubpd    %xmm4, %xmm3, %xmm3                           #8881.20
        vmovhpd   -128(%rcx,%rsi), %xmm6, %xmm2                 #8866.14
        vmovapd   %xmm5, %xmm6                                  #8889.23
        vfnmadd231pd %xmm2, %xmm4, %xmm6                        #8889.23
        vmovsd    (%rdx,%rsi), %xmm7                            #8867.35
        vmovhpd   (%rcx,%rsi), %xmm7, %xmm0                     #8867.18
        vaddpd    %xmm4, %xmm2, %xmm1                           #8883.21
        vsubpd    %xmm10, %xmm3, %xmm15                         #8882.20
        vsubpd    %xmm5, %xmm6, %xmm3                           #8890.20
        vdivpd    %xmm6, %xmm5, %xmm8                           #8894.23
        vsubpd    %xmm2, %xmm1, %xmm11                          #8884.20
        vfmadd231pd %xmm2, %xmm4, %xmm3                         #8891.20
        vsubpd    %xmm11, %xmm4, %xmm12                         #8885.20
        vfmadd213pd %xmm3, %xmm15, %xmm2                        #8892.20
        vaddpd    %xmm12, %xmm0, %xmm13                         #8886.20
        vfmadd213pd %xmm2, %xmm4, %xmm0                         #8893.20
        vmovapd   %xmm5, %xmm2                                  #8895.21
        vfnmadd231pd %xmm8, %xmm6, %xmm2                        #8895.21
        vaddpd    %xmm13, %xmm15, %xmm7                         #8887.20
        vfmadd213pd %xmm8, %xmm2, %xmm8                         #8896.27
        vfnmadd231pd %xmm8, %xmm6, %xmm5                        #8897.25
        vfmadd231pd %xmm8, %xmm0, %xmm5                         #8898.25
        vfmadd213pd %xmm8, %xmm5, %xmm8                         #8899.23
        vmulpd    %xmm8, %xmm1, %xmm9                           #8900.24
        vfmsub213pd %xmm1, %xmm9, %xmm6                         #8901.18
        vfnmadd213pd %xmm6, %xmm9, %xmm0                        #8902.22
        vsubpd    %xmm7, %xmm0, %xmm0                           #8903.22
        vfnmadd213pd %xmm9, %xmm8, %xmm0                        #8904.18
        testl     %eax, %eax                                    #8906.52
        jne       ..B7.4        # Prob 5%                       #8906.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm14
..B7.3:                         # Preds ..B7.4 ..B7.10 ..B7.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #8909.12
        popq      %rbp                                          #8909.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #8909.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.4:                         # Preds ..B7.2
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm14, 64(%rsp)                              #8906.197
        vmovupd   %xmm0, 128(%rsp)                              #8906.270
        je        ..B7.3        # Prob 95%                      #8906.374
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B7.7:                         # Preds ..B7.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #8906.454
        movq      %r12, 8(%rsp)                                 #8906.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #8906.454
        movq      %r13, (%rsp)                                  #8906.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #8906.454
                                # LOE rbx r12 r14 r15 r13d
..B7.8:                         # Preds ..B7.9 ..B7.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #8906.517
        jc        ..B7.11       # Prob 5%                       #8906.517
                                # LOE rbx r12 r14 r15 r13d
..B7.9:                         # Preds ..B7.11 ..B7.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #8906.470
        cmpl      $2, %r12d                                     #8906.465
        jl        ..B7.8        # Prob 82%                      #8906.465
                                # LOE rbx r12 r14 r15 r13d
..B7.10:                        # Preds ..B7.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #8906.673
        jmp       ..B7.3        # Prob 100%                     #8906.673
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B7.11:                        # Preds ..B7.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #8906.546
        lea       128(%rsp,%r12,8), %rsi                        #8906.546
..___tag_value___svml_tan2_ha_l9.242:
#       __svml_dtan_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtan_ha_cout_rare_internal             #8906.546
..___tag_value___svml_tan2_ha_l9.243:
        jmp       ..B7.9        # Prob 100%                     #8906.546
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B7.12:                        # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        vpand     .L_2il0floatpacket.29(%rip), %xmm14, %xmm11   #8722.18
        lea       __svml_dtan_ha_reduction_data_internal(%rip), %r8 #8724.385
        vmovupd   %xmm2, (%rsp)                                 #[spill]
        vpsrlq    $52, %xmm11, %xmm2                            #8723.18
        vmovd     %xmm2, %edx                                   #8724.124
        vmovupd   1088+__svml_dtan_ha_data_internal(%rip), %xmm7 #8632.55
        vandpd    %xmm10, %xmm7, %xmm8                          #8633.17
        vpextrd   $2, %xmm2, %esi                               #8724.189
        vcmpeqpd  %xmm7, %xmm8, %xmm10                          #8634.25
        vpand     .L_2il0floatpacket.30(%rip), %xmm14, %xmm13   #8732.21
        lea       (%rdx,%rdx,2), %ecx                           #8724.300
        shll      $3, %ecx                                      #8724.300
        lea       (%rsi,%rsi,2), %edi                           #8724.351
        shll      $3, %edi                                      #8724.351
        vmovq     16(%rcx,%r8), %xmm15                          #8726.639
        vpaddq    .L_2il0floatpacket.31(%rip), %xmm13, %xmm12   #8733.21
        vmovmskpd %xmm10, %eax                                  #8636.48
        vmovhpd   16(%rdi,%r8), %xmm15, %xmm6                   #8726.607
        vpsrlq    $32, %xmm12, %xmm10                           #8741.12
        vmovdqu   .L_2il0floatpacket.32(%rip), %xmm13           #8734.17
        vmovupd   %xmm1, 32(%rsp)                               #[spill]
        vpand     %xmm13, %xmm6, %xmm15                         #8740.12
        vmovq     8(%rcx,%r8), %xmm1                            #8725.640
        vpand     %xmm13, %xmm12, %xmm7                         #8742.12
        vmovhpd   8(%rdi,%r8), %xmm1, %xmm9                     #8725.608
        vpsrlq    $32, %xmm6, %xmm1                             #8739.12
        vpmuludq  %xmm15, %xmm10, %xmm6                         #8748.13
        vpsrlq    $32, %xmm9, %xmm5                             #8737.12
        vpmuludq  %xmm5, %xmm7, %xmm4                           #8751.13
        vpmuludq  %xmm5, %xmm10, %xmm5                          #8745.13
        vmovupd   %xmm3, 16(%rsp)                               #[spill]
        vpsrlq    $32, %xmm6, %xmm15                            #8773.13
        vmovq     (%rcx,%r8), %xmm3                             #8724.639
        vpmuludq  %xmm1, %xmm10, %xmm6                          #8747.13
        vpmuludq  %xmm1, %xmm7, %xmm1                           #8753.13
        vmovhpd   (%rdi,%r8), %xmm3, %xmm11                     #8724.607
        vpand     %xmm13, %xmm9, %xmm3                          #8738.12
        vpmuludq  %xmm3, %xmm7, %xmm2                           #8752.13
        vpsrlq    $32, %xmm1, %xmm1                             #8755.13
        vpmuludq  %xmm3, %xmm10, %xmm3                          #8746.13
        vpaddq    %xmm1, %xmm6, %xmm6                           #8759.13
        vpand     %xmm13, %xmm2, %xmm12                         #8763.13
        vpand     %xmm13, %xmm11, %xmm8                         #8736.12
        vpaddq    %xmm6, %xmm12, %xmm12                         #8768.13
        vpmuludq  %xmm8, %xmm7, %xmm9                           #8750.13
        vpmuludq  %xmm8, %xmm10, %xmm8                          #8744.13
        vpaddq    %xmm12, %xmm15, %xmm12                        #8774.13
        vpsrlq    $32, %xmm2, %xmm2                             #8756.13
        vpand     %xmm13, %xmm4, %xmm1                          #8764.13
        vpaddq    %xmm2, %xmm3, %xmm2                           #8760.13
        vpaddq    %xmm2, %xmm1, %xmm3                           #8769.13
        vpsrlq    $32, %xmm4, %xmm4                             #8757.13
        vpsrlq    $32, %xmm11, %xmm11                           #8735.12
        vpsrlq    $32, %xmm12, %xmm15                           #8775.13
        vpsrlq    $32, %xmm9, %xmm10                            #8758.13
        vpaddq    %xmm4, %xmm5, %xmm1                           #8761.13
        vpaddq    %xmm3, %xmm15, %xmm6                          #8776.13
        vpmuludq  %xmm11, %xmm7, %xmm7                          #8749.13
        vpand     %xmm13, %xmm9, %xmm3                          #8765.13
        vpsrlq    $32, %xmm6, %xmm2                             #8777.13
        vpaddq    %xmm1, %xmm3, %xmm3                           #8770.13
        vpaddq    %xmm3, %xmm2, %xmm5                           #8778.13
        vpaddq    %xmm10, %xmm8, %xmm3                          #8762.13
        vpand     %xmm13, %xmm7, %xmm2                          #8766.13
        vpsrlq    $32, %xmm5, %xmm4                             #8779.13
        vpaddq    %xmm3, %xmm2, %xmm1                           #8771.13
        vpaddq    %xmm1, %xmm4, %xmm15                          #8780.13
        vpsllq    $32, %xmm15, %xmm9                            #8783.19
        vpand     %xmm13, %xmm5, %xmm4                          #8782.13
        vpaddq    %xmm4, %xmm9, %xmm9                           #8785.19
        vpand     %xmm13, %xmm12, %xmm13                        #8781.13
        vpsrlq    $12, %xmm9, %xmm5                             #8791.20
        vpand     .L_2il0floatpacket.33(%rip), %xmm14, %xmm12   #8788.17
        vpsllq    $32, %xmm6, %xmm6                             #8784.19
        vpxor     .L_2il0floatpacket.28(%rip), %xmm12, %xmm7    #8790.22
        vmovups   .L_2il0floatpacket.34(%rip), %xmm8            #8794.15
        vpor      %xmm7, %xmm5, %xmm10                          #8792.20
        vpaddq    %xmm13, %xmm6, %xmm13                         #8786.19
        vaddpd    %xmm8, %xmm10, %xmm6                          #8795.19
        vpand     .L_2il0floatpacket.36(%rip), %xmm13, %xmm2    #8804.20
        vpxor     .L_2il0floatpacket.35(%rip), %xmm12, %xmm4    #8802.22
        vpsllq    $28, %xmm2, %xmm3                             #8805.20
        vpor      %xmm4, %xmm3, %xmm1                           #8806.20
        vpsrlq    $24, %xmm13, %xmm3                            #8815.19
        vsubpd    %xmm8, %xmm6, %xmm11                          #8796.9
        vsubpd    %xmm4, %xmm1, %xmm1                           #8809.17
        vsubpd    %xmm11, %xmm10, %xmm15                        #8797.17
        vpxor     .L_2il0floatpacket.37(%rip), %xmm12, %xmm4    #8811.22
        vpand     .L_2il0floatpacket.38(%rip), %xmm9, %xmm12    #8813.20
        vpsllq    $40, %xmm12, %xmm2                            #8814.20
        vpor      %xmm3, %xmm2, %xmm9                           #8816.20
        vpor      %xmm4, %xmm9, %xmm12                          #8817.20
        vsubpd    %xmm4, %xmm12, %xmm5                          #8820.18
        vaddpd    %xmm5, %xmm15, %xmm10                         #8821.11
        vsubpd    %xmm10, %xmm15, %xmm15                        #8822.17
        vmovups   .L_2il0floatpacket.39(%rip), %xmm8            #8825.16
        vmulpd    %xmm10, %xmm8, %xmm3                          #8827.13
        vaddpd    %xmm15, %xmm5, %xmm7                          #8823.18
        vaddpd    %xmm7, %xmm1, %xmm4                           #8824.11
        vmovaps   %xmm8, %xmm1                                  #8828.13
        vpxor     %xmm7, %xmm7, %xmm7                           #8849.58
        vfmsub213pd %xmm3, %xmm10, %xmm1                        #8828.13
        vmovups   .L_2il0floatpacket.43(%rip), %xmm13           #8832.16
        vandpd    .L_2il0floatpacket.42(%rip), %xmm14, %xmm11   #8833.11
        vpand     .L_2il0floatpacket.44(%rip), %xmm6, %xmm6     #8800.15
        vpshufd   $136, %xmm6, %xmm9                            #8841.23
        vfmadd132pd .L_2il0floatpacket.53(%rip), %xmm1, %xmm10  #8829.13
        vmovups   .L_2il0floatpacket.55(%rip), %xmm12           #8854.19
        vcmplepd  %xmm13, %xmm11, %xmm2                         #8835.20
        vfmadd213pd %xmm10, %xmm8, %xmm4                        #8830.13
        vcmpltpd  %xmm11, %xmm13, %xmm8                         #8834.17
        vandpd    %xmm14, %xmm2, %xmm1                          #8836.24
        vandpd    %xmm3, %xmm8, %xmm15                          #8837.25
        vorpd     %xmm15, %xmm1, %xmm2                          #8838.9
        vandpd    %xmm4, %xmm8, %xmm15                          #8839.9
        vpshufd   $221, %xmm2, %xmm3                            #8844.22
        vmovdqu   .L_2il0floatpacket.54(%rip), %xmm1            #8846.23
        vpsrld    $31, %xmm3, %xmm4                             #8845.22
        vpsubd    %xmm4, %xmm1, %xmm5                           #8846.23
        vmovapd   %xmm2, %xmm1                                  #8856.26
        vpaddd    %xmm5, %xmm9, %xmm6                           #8848.23
        vpsrld    $4, %xmm6, %xmm8                              #8848.23
        vpslld    $4, %xmm8, %xmm10                             #8851.23
        vpsubd    %xmm10, %xmm9, %xmm11                         #8852.23
        vcvtdq2pd %xmm11, %xmm13                                #8853.23
        vfmadd231pd %xmm13, %xmm12, %xmm1                       #8856.26
        vpunpckldq %xmm7, %xmm8, %xmm3                          #8849.27
        vfnmadd213pd %xmm1, %xmm13, %xmm12                      #8857.27
        vsubpd    %xmm12, %xmm2, %xmm2                          #8858.27
        vfmadd132pd .L_2il0floatpacket.56(%rip), %xmm2, %xmm13  #8859.27
        vmovupd   (%rsp), %xmm2                                 #8861.17[spill]
        vaddpd    %xmm13, %xmm15, %xmm15                        #8860.27
        blendvpd  %xmm0, %xmm3, %xmm2                           #8861.17
        vmovupd   16(%rsp), %xmm3                               #8862.17[spill]
        blendvpd  %xmm0, %xmm1, %xmm3                           #8862.17
        vmovupd   32(%rsp), %xmm1                               #8863.18[spill]
        blendvpd  %xmm0, %xmm15, %xmm1                          #8863.18
        jmp       ..B7.2        # Prob 100%                     #8863.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm2 xmm3 xmm14
	.cfi_endproc
# mark_end;
	.type	__svml_tan2_ha_l9,@function
	.size	__svml_tan2_ha_l9,.-__svml_tan2_ha_l9
..LN__svml_tan2_ha_l9.6:
	.data
# -- End  __svml_tan2_ha_l9
	.text
.L_2__routine_start___svml_tan2_ha_e9_7:
# -- Begin  __svml_tan2_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tan2_ha_e9
# --- __svml_tan2_ha_e9(__m128d)
__svml_tan2_ha_e9:
# parameter 1: %xmm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #8914.1
        .byte     15                                            #9343.22
        .byte     30                                            #9343.22
        .byte     250                                           #9343.22
	.cfi_startproc
..___tag_value___svml_tan2_ha_e9.253:
..L254:
                                                        #8914.1
        pushq     %rbp                                          #8914.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #8914.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #8914.1
        subq      $192, %rsp                                    #8914.1
        xorl      %edx, %edx                                    #8973.9
        movups    1024+__svml_dtan_ha_data_internal(%rip), %xmm8 #8974.54
        movaps    %xmm8, %xmm14                                 #8975.20
        andnps    %xmm0, %xmm8                                  #8976.21
        movups    1472+__svml_dtan_ha_data_internal(%rip), %xmm12 #8983.29
        andps     %xmm0, %xmm14                                 #8975.20
        mulpd     %xmm14, %xmm12                                #8983.29
        movups    1152+__svml_dtan_ha_data_internal(%rip), %xmm3 #8982.53
        movaps    %xmm14, %xmm1                                 #8978.34
        addpd     %xmm3, %xmm12                                 #8983.17
        cmpnlepd  28160+__svml_dtan_ha_data_internal(%rip), %xmm1 #8978.34
        movaps    %xmm12, %xmm9                                 #8984.17
        subpd     %xmm3, %xmm9                                  #8984.17
        movups    1216+__svml_dtan_ha_data_internal(%rip), %xmm7 #8986.40
        mulpd     %xmm9, %xmm7                                  #8986.40
        movups    1280+__svml_dtan_ha_data_internal(%rip), %xmm5 #8988.23
        mulpd     %xmm9, %xmm5                                  #8988.23
        movups    %xmm14, (%rsp)                                #8975.20[spill]
        subpd     %xmm7, %xmm14                                 #8986.18
        movaps    %xmm14, %xmm11                                #8989.22
        subpd     %xmm5, %xmm11                                 #8989.22
        subpd     %xmm11, %xmm14                                #8990.21
        movups    1344+__svml_dtan_ha_data_internal(%rip), %xmm4 #8993.36
        mulpd     %xmm9, %xmm4                                  #8993.36
        subpd     %xmm5, %xmm14                                 #8991.21
        pand      1408+__svml_dtan_ha_data_internal(%rip), %xmm12 #8996.27
        pand      .L_2il0floatpacket.26(%rip), %xmm12           #9001.27
        subpd     %xmm4, %xmm14                                 #8993.17
        movd      %xmm12, %eax                                  #9002.136
        movups    %xmm8, 64(%rsp)                               #8976.21[spill]
        movaps    %xmm11, %xmm8                                 #8997.17
        imull     $104, %eax, %ecx                              #9002.307
        lea       1632+__svml_dtan_ha_data_internal(%rip), %rax #9002.393
        movups    %xmm1, 96(%rsp)                               #8978.34[spill]
        movups    %xmm0, 80(%rsp)                               #8914.1[spill]
        addpd     %xmm14, %xmm8                                 #8997.17
        pextrd    $2, %xmm12, %esi                              #9002.198
        imull     $104, %esi, %edi                              #9002.359
        movq      -96(%rcx,%rax), %xmm1                         #9002.629
        movq      (%rcx,%rax), %xmm13                           #9014.627
        movq      -88(%rcx,%rax), %xmm2                         #9003.629
        movq      -80(%rcx,%rax), %xmm3                         #9004.628
        movhpd    -96(%rdi,%rax), %xmm1                         #9002.597
        movaps    %xmm1, %xmm15                                 #9015.22
        subpd     %xmm8, %xmm15                                 #9015.22
        subpd     %xmm8, %xmm11                                 #8998.18
        subpd     %xmm15, %xmm1                                 #9018.20
        addpd     %xmm11, %xmm14                                #8999.17
        subpd     %xmm8, %xmm1                                  #9019.20
        movhpd    (%rdi,%rax), %xmm13                           #9014.595
        movups    %xmm13, 48(%rsp)                              #9014.595[spill]
        movups    .L_2il0floatpacket.27(%rip), %xmm13           #9016.24
        movaps    %xmm13, %xmm0                                 #9017.20
        andps     %xmm15, %xmm0                                 #9017.20
        subpd     %xmm0, %xmm15                                 #9021.22
        subpd     %xmm14, %xmm1                                 #9020.20
        movhpd    -88(%rdi,%rax), %xmm2                         #9003.597
        addpd     %xmm15, %xmm2                                 #9022.22
        addpd     %xmm2, %xmm1                                  #9023.20
        cvtpd2ps  %xmm0, %xmm2                                  #9024.64
        movlhps   %xmm2, %xmm2                                  #9024.48
        rcpps     %xmm2, %xmm15                                 #9024.37
        cvtps2pd  %xmm15, %xmm2                                 #9024.24
        andps     %xmm13, %xmm2                                 #9025.24
        mulpd     %xmm2, %xmm0                                  #9027.39
        movups    %xmm14, 16(%rsp)                              #8999.17[spill]
        movaps    %xmm2, %xmm13                                 #9028.32
        movups    .L_2il0floatpacket.28(%rip), %xmm14           #9026.36
        movaps    %xmm14, %xmm15                                #9027.18
        subpd     %xmm0, %xmm15                                 #9027.18
        movaps    %xmm15, %xmm0                                 #9029.30
        mulpd     %xmm15, %xmm13                                #9028.32
        mulpd     %xmm15, %xmm0                                 #9029.30
        addpd     %xmm2, %xmm13                                 #9028.20
        addpd     %xmm14, %xmm0                                 #9029.18
        mulpd     %xmm0, %xmm13                                 #9030.24
        mulpd     %xmm13, %xmm1                                 #9031.20
        movaps    %xmm1, %xmm14                                 #9032.19
        mulpd     %xmm1, %xmm1                                  #9033.36
        subpd     %xmm15, %xmm14                                #9032.19
        subpd     %xmm14, %xmm1                                 #9033.24
        mulpd     %xmm1, %xmm13                                 #9034.24
        movhpd    -80(%rdi,%rax), %xmm3                         #9004.596
        movq      -56(%rcx,%rax), %xmm9                         #9007.630
        movhpd    -56(%rdi,%rax), %xmm9                         #9007.598
        mulpd     %xmm3, %xmm2                                  #9035.28
        mulpd     %xmm13, %xmm3                                 #9036.28
        movaps    %xmm8, %xmm13                                 #9037.22
        mulpd     %xmm9, %xmm13                                 #9037.22
        movq      -72(%rcx,%rax), %xmm12                        #9005.630
        movhpd    -72(%rdi,%rax), %xmm12                        #9005.598
        movaps    %xmm12, %xmm14                                #9038.22
        addpd     %xmm13, %xmm14                                #9038.22
        movaps    %xmm14, %xmm1                                 #9040.22
        addpd     %xmm2, %xmm1                                  #9040.22
        subpd     %xmm14, %xmm12                                #9039.22
        subpd     %xmm1, %xmm2                                  #9042.22
        addpd     %xmm12, %xmm13                                #9041.22
        addpd     %xmm2, %xmm14                                 #9044.22
        addpd     %xmm13, %xmm3                                 #9043.22
        movq      -64(%rcx,%rax), %xmm6                         #9006.630
        movaps    %xmm8, %xmm2                                  #9049.18
        movhpd    -64(%rdi,%rax), %xmm6                         #9006.598
        movq      -8(%rcx,%rax), %xmm10                         #9013.627
        movups    %xmm6, 32(%rsp)                               #9006.598[spill]
        movq      -24(%rcx,%rax), %xmm6                         #9011.627
        movhpd    -8(%rdi,%rax), %xmm10                         #9013.595
        movhpd    -24(%rdi,%rax), %xmm6                         #9011.595
        mulpd     %xmm8, %xmm10                                 #9048.30
        mulpd     %xmm8, %xmm6                                  #9047.30
        mulpd     %xmm8, %xmm2                                  #9049.18
        addpd     %xmm14, %xmm3                                 #9045.22
        movq      -16(%rcx,%rax), %xmm5                         #9012.627
        movq      -32(%rcx,%rax), %xmm4                         #9010.627
        movhpd    -16(%rdi,%rax), %xmm5                         #9012.595
        movhpd    -32(%rdi,%rax), %xmm4                         #9010.595
        addpd     %xmm10, %xmm5                                 #9048.18
        addpd     %xmm6, %xmm4                                  #9047.18
        movups    48(%rsp), %xmm10                              #9051.30[spill]
        movq      -40(%rcx,%rax), %xmm7                         #9009.627
        mulpd     %xmm2, %xmm10                                 #9051.30
        mulpd     %xmm2, %xmm4                                  #9052.31
        mulpd     %xmm2, %xmm2                                  #9053.18
        addpd     %xmm10, %xmm5                                 #9051.18
        movhpd    -40(%rdi,%rax), %xmm7                         #9009.595
        mulpd     %xmm8, %xmm7                                  #9046.30
        mulpd     %xmm2, %xmm5                                  #9055.31
        movq      -48(%rcx,%rax), %xmm11                        #9008.630
        movhpd    -48(%rdi,%rax), %xmm11                        #9008.598
        addpd     %xmm11, %xmm7                                 #9046.18
        addpd     %xmm11, %xmm9                                 #9050.18
        addpd     %xmm4, %xmm7                                  #9052.19
        movups    16(%rsp), %xmm4                               #9054.31[spill]
        mulpd     %xmm9, %xmm4                                  #9054.31
        addpd     %xmm5, %xmm7                                  #9055.19
        addpd     %xmm4, %xmm3                                  #9054.19
        mulpd     %xmm7, %xmm8                                  #9057.31
        movups    32(%rsp), %xmm5                               #9056.19[spill]
        addpd     %xmm3, %xmm5                                  #9056.19
        addpd     %xmm8, %xmm5                                  #9057.19
        addpd     %xmm5, %xmm1                                  #9058.18
        movups    96(%rsp), %xmm2                               #8980.82[spill]
        movmskpd  %xmm2, %r8d                                   #8980.82
        xorps     64(%rsp), %xmm1                               #9059.18[spill]
        movups    (%rsp), %xmm4                                 #9060.70[spill]
        testl     %r8d, %r8d                                    #9060.70
        jne       ..B8.12       # Prob 5%                       #9060.70
                                # LOE rax rbx r12 r13 r14 r15 edx xmm1 xmm2 xmm4
..B8.2:                         # Preds ..B8.12 ..B8.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #9346.52
        jne       ..B8.4        # Prob 5%                       #9346.52
                                # LOE rbx r12 r13 r14 r15 edx xmm1
..B8.3:                         # Preds ..B8.4 ..B8.10 ..B8.2
                                # Execution count [1.00e+00]
        movaps    %xmm1, %xmm0                                  #9349.12
        movq      %rbp, %rsp                                    #9349.12
        popq      %rbp                                          #9349.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #9349.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.4:                         # Preds ..B8.2
                                # Execution count [5.00e-02]: Infreq
        movups    80(%rsp), %xmm2                               #9346.197[spill]
        movups    %xmm2, 64(%rsp)                               #9346.197
        movups    %xmm1, 128(%rsp)                              #9346.270
        je        ..B8.3        # Prob 95%                      #9346.374
                                # LOE rbx r12 r13 r14 r15 edx xmm1
..B8.7:                         # Preds ..B8.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #9346.454
        movq      %r12, 8(%rsp)                                 #9346.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #9346.454
        movq      %r13, (%rsp)                                  #9346.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #9346.454
                                # LOE rbx r12 r14 r15 r13d
..B8.8:                         # Preds ..B8.9 ..B8.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #9346.517
        jc        ..B8.11       # Prob 5%                       #9346.517
                                # LOE rbx r12 r14 r15 r13d
..B8.9:                         # Preds ..B8.11 ..B8.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #9346.470
        cmpl      $2, %r12d                                     #9346.465
        jl        ..B8.8        # Prob 82%                      #9346.465
                                # LOE rbx r12 r14 r15 r13d
..B8.10:                        # Preds ..B8.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm1                              #9346.673
        jmp       ..B8.3        # Prob 100%                     #9346.673
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm1
..B8.11:                        # Preds ..B8.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #9346.546
        lea       128(%rsp,%r12,8), %rsi                        #9346.546
..___tag_value___svml_tan2_ha_e9.285:
#       __svml_dtan_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtan_ha_cout_rare_internal             #9346.546
..___tag_value___svml_tan2_ha_e9.286:
        jmp       ..B8.9        # Prob 100%                     #9346.546
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B8.12:                        # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        movdqu    .L_2il0floatpacket.29(%rip), %xmm14           #9153.22
        lea       __svml_dtan_ha_reduction_data_internal(%rip), %r9 #9155.385
        pand      %xmm4, %xmm14                                 #9153.22
        psrlq     $52, %xmm14                                   #9154.22
        pextrd    $2, %xmm14, %edi                              #9155.189
        movd      %xmm14, %ecx                                  #9155.124
        movdqu    .L_2il0floatpacket.30(%rip), %xmm0            #9163.25
        pand      %xmm4, %xmm0                                  #9163.25
        movups    1088+__svml_dtan_ha_data_internal(%rip), %xmm15 #9063.59
        lea       (%rdi,%rdi,2), %r8d                           #9155.351
        paddq     .L_2il0floatpacket.31(%rip), %xmm0            #9164.25
        shll      $3, %r8d                                      #9155.351
        lea       (%rcx,%rcx,2), %esi                           #9155.300
        shll      $3, %esi                                      #9155.300
        movaps    %xmm15, %xmm3                                 #9064.21
        movq      16(%rsi,%r9), %xmm6                           #9157.639
        andps     %xmm4, %xmm3                                  #9064.21
        movhpd    16(%r8,%r9), %xmm6                            #9157.607
        movdqa    %xmm0, %xmm7                                  #9172.16
        cmpeqpd   %xmm15, %xmm3                                 #9065.29
        movdqu    .L_2il0floatpacket.32(%rip), %xmm15           #9165.21
        movaps    %xmm6, %xmm10                                 #9170.16
        psrlq     $32, %xmm7                                    #9172.16
        psrlq     $32, %xmm10                                   #9170.16
        movq      8(%rsi,%r9), %xmm12                           #9156.640
        pand      %xmm15, %xmm0                                 #9173.16
        movdqa    %xmm7, %xmm5                                  #9178.17
        movdqa    %xmm0, %xmm9                                  #9183.17
        movhpd    8(%r8,%r9), %xmm12                            #9156.608
        pand      %xmm15, %xmm6                                 #9171.16
        pmuludq   %xmm10, %xmm5                                 #9178.17
        movaps    %xmm12, %xmm11                                #9168.16
        pmuludq   %xmm0, %xmm10                                 #9184.17
        pmuludq   %xmm7, %xmm6                                  #9179.17
        movmskpd  %xmm3, %edx                                   #9067.52
        pand      %xmm15, %xmm12                                #9169.16
        psrlq     $32, %xmm10                                   #9186.17
        pmuludq   %xmm12, %xmm9                                 #9183.17
        psrlq     $32, %xmm11                                   #9168.16
        pmuludq   %xmm7, %xmm12                                 #9177.17
        paddq     %xmm10, %xmm5                                 #9190.17
        movdqa    %xmm0, %xmm8                                  #9182.17
        movdqa    %xmm15, %xmm3                                 #9194.17
        pmuludq   %xmm11, %xmm8                                 #9182.17
        pand      %xmm9, %xmm3                                  #9194.17
        pmuludq   %xmm7, %xmm11                                 #9176.17
        paddq     %xmm5, %xmm3                                  #9199.17
        movq      (%rsi,%r9), %xmm13                            #9155.639
        psrlq     $32, %xmm6                                    #9204.17
        movups    %xmm2, 96(%rsp)                               #[spill]
        movdqa    %xmm15, %xmm2                                 #9167.16
        movhpd    (%r8,%r9), %xmm13                             #9155.607
        psrlq     $32, %xmm9                                    #9187.17
        movups    %xmm1, (%rsp)                                 #[spill]
        pand      %xmm13, %xmm2                                 #9167.16
        movdqa    %xmm0, %xmm1                                  #9181.17
        movdqa    %xmm15, %xmm14                                #9195.17
        paddq     %xmm3, %xmm6                                  #9205.17
        paddq     %xmm9, %xmm12                                 #9191.17
        pmuludq   %xmm2, %xmm1                                  #9181.17
        pmuludq   %xmm2, %xmm7                                  #9175.17
        pand      %xmm8, %xmm14                                 #9195.17
        movaps    %xmm6, %xmm3                                  #9206.17
        paddq     %xmm12, %xmm14                                #9200.17
        psrlq     $32, %xmm3                                    #9206.17
        psrlq     $32, %xmm8                                    #9188.17
        psrlq     $32, %xmm13                                   #9166.16
        movdqa    %xmm15, %xmm9                                 #9196.17
        paddq     %xmm14, %xmm3                                 #9207.17
        paddq     %xmm8, %xmm11                                 #9192.17
        pmuludq   %xmm13, %xmm0                                 #9180.17
        pand      %xmm1, %xmm9                                  #9196.17
        movdqa    %xmm3, %xmm12                                 #9208.17
        paddq     %xmm11, %xmm9                                 #9201.17
        psrlq     $32, %xmm12                                   #9208.17
        psrlq     $32, %xmm1                                    #9189.17
        paddq     %xmm9, %xmm12                                 #9209.17
        paddq     %xmm1, %xmm7                                  #9193.17
        pand      %xmm15, %xmm0                                 #9197.17
        movdqa    %xmm12, %xmm14                                #9210.17
        paddq     %xmm7, %xmm0                                  #9202.17
        psrlq     $32, %xmm14                                   #9210.17
        pand      %xmm15, %xmm12                                #9213.17
        paddq     %xmm0, %xmm14                                 #9211.17
        psllq     $32, %xmm14                                   #9214.23
        pand      %xmm15, %xmm6                                 #9212.17
        paddq     %xmm12, %xmm14                                #9216.23
        movups    .L_2il0floatpacket.33(%rip), %xmm15           #9219.21
        movdqa    %xmm14, %xmm0                                 #9222.24
        movups    .L_2il0floatpacket.28(%rip), %xmm2            #9221.26
        andps     %xmm4, %xmm15                                 #9219.21
        movaps    %xmm2, %xmm8                                  #9221.26
        psrlq     $12, %xmm0                                    #9222.24
        movups    .L_2il0floatpacket.34(%rip), %xmm11           #9225.15
        pxor      %xmm15, %xmm8                                 #9221.26
        orps      %xmm8, %xmm0                                  #9223.24
        movaps    %xmm11, %xmm13                                #9226.19
        addpd     %xmm0, %xmm13                                 #9226.19
        movaps    %xmm13, %xmm1                                 #9227.9
        psllq     $32, %xmm3                                    #9215.23
        subpd     %xmm11, %xmm1                                 #9227.9
        paddq     %xmm6, %xmm3                                  #9217.23
        subpd     %xmm1, %xmm0                                  #9228.17
        movdqu    .L_2il0floatpacket.38(%rip), %xmm9            #9244.24
        movdqu    .L_2il0floatpacket.36(%rip), %xmm1            #9235.24
        pand      %xmm14, %xmm9                                 #9244.24
        movups    .L_2il0floatpacket.35(%rip), %xmm7            #9233.26
        pand      %xmm3, %xmm1                                  #9235.24
        psllq     $40, %xmm9                                    #9245.24
        psrlq     $24, %xmm3                                    #9246.23
        pxor      %xmm15, %xmm7                                 #9233.26
        por       %xmm3, %xmm9                                  #9247.24
        xorps     .L_2il0floatpacket.37(%rip), %xmm15           #9242.26
        movaps    %xmm0, %xmm3                                  #9252.11
        orps      %xmm15, %xmm9                                 #9248.24
        psllq     $28, %xmm1                                    #9236.24
        subpd     %xmm15, %xmm9                                 #9251.18
        addpd     %xmm9, %xmm3                                  #9252.11
        subpd     %xmm3, %xmm0                                  #9253.17
        orps      %xmm7, %xmm1                                  #9237.24
        subpd     %xmm7, %xmm1                                  #9240.17
        addpd     %xmm0, %xmm9                                  #9254.18
        movups    .L_2il0floatpacket.27(%rip), %xmm5            #9260.16
        movaps    %xmm5, %xmm14                                 #9260.16
        andps     %xmm3, %xmm14                                 #9260.16
        addpd     %xmm9, %xmm1                                  #9255.11
        subpd     %xmm14, %xmm3                                 #9261.17
        mulpd     .L_2il0floatpacket.39(%rip), %xmm1            #9265.12
        movups    .L_2il0floatpacket.40(%rip), %xmm6            #9257.16
        movaps    %xmm14, %xmm12                                #9262.16
        movups    .L_2il0floatpacket.41(%rip), %xmm7            #9258.17
        mulpd     %xmm6, %xmm12                                 #9262.16
        mulpd     %xmm3, %xmm6                                  #9263.12
        mulpd     %xmm7, %xmm14                                 #9264.12
        mulpd     %xmm3, %xmm7                                  #9266.12
        addpd     %xmm14, %xmm6                                 #9267.13
        addpd     %xmm7, %xmm1                                  #9268.13
        addpd     %xmm1, %xmm6                                  #9269.13
        movaps    %xmm6, %xmm8                                  #9270.13
        addpd     %xmm12, %xmm8                                 #9270.13
        subpd     %xmm8, %xmm12                                 #9271.16
        pand      .L_2il0floatpacket.44(%rip), %xmm13           #9231.19
        pand      .L_2il0floatpacket.26(%rip), %xmm13           #9284.31
        movups    .L_2il0floatpacket.43(%rip), %xmm15           #9274.16
        movups    .L_2il0floatpacket.42(%rip), %xmm1            #9275.11
        movaps    %xmm15, %xmm0                                 #9276.17
        movd      %xmm13, %r10d                                 #9285.140
        andps     %xmm4, %xmm1                                  #9275.11
        addpd     %xmm12, %xmm6                                 #9272.13
        cmpltpd   %xmm1, %xmm0                                  #9276.17
        cmplepd   %xmm15, %xmm1                                 #9277.20
        imull     $104, %r10d, %r11d                            #9285.311
        andps     %xmm0, %xmm8                                  #9279.25
        andps     %xmm6, %xmm0                                  #9281.9
        pextrd    $2, %xmm13, %ecx                              #9285.202
        andps     %xmm4, %xmm1                                  #9278.24
        imull     $104, %ecx, %esi                              #9285.363
        orps      %xmm8, %xmm1                                  #9280.9
        movq      -96(%r11,%rax), %xmm7                         #9285.633
        movq      (%r11,%rax), %xmm6                            #9297.631
        movq      -64(%r11,%rax), %xmm4                         #9289.634
        movq      -88(%r11,%rax), %xmm10                        #9286.633
        movhpd    -96(%rsi,%rax), %xmm7                         #9285.601
        movhpd    (%rsi,%rax), %xmm6                            #9297.599
        movups    %xmm6, 48(%rsp)                               #9297.599[spill]
        movaps    %xmm7, %xmm6                                  #9298.26
        subpd     %xmm1, %xmm6                                  #9298.26
        subpd     %xmm6, %xmm7                                  #9301.24
        movhpd    -64(%rsi,%rax), %xmm4                         #9289.602
        movups    %xmm4, 16(%rsp)                               #9289.602[spill]
        movaps    %xmm5, %xmm4                                  #9300.24
        andps     %xmm6, %xmm4                                  #9300.24
        subpd     %xmm1, %xmm7                                  #9302.24
        subpd     %xmm4, %xmm6                                  #9304.26
        subpd     %xmm0, %xmm7                                  #9303.24
        movhpd    -88(%rsi,%rax), %xmm10                        #9286.601
        addpd     %xmm6, %xmm10                                 #9305.26
        addpd     %xmm10, %xmm7                                 #9306.24
        cvtpd2ps  %xmm4, %xmm10                                 #9307.68
        movlhps   %xmm10, %xmm10                                #9307.52
        movaps    %xmm2, %xmm6                                  #9310.22
        rcpps     %xmm10, %xmm10                                #9307.41
        cvtps2pd  %xmm10, %xmm10                                #9307.28
        andps     %xmm5, %xmm10                                 #9308.28
        mulpd     %xmm10, %xmm4                                 #9310.43
        subpd     %xmm4, %xmm6                                  #9310.22
        movaps    %xmm10, %xmm4                                 #9311.36
        movaps    %xmm6, %xmm5                                  #9312.34
        mulpd     %xmm6, %xmm4                                  #9311.36
        mulpd     %xmm6, %xmm5                                  #9312.34
        addpd     %xmm10, %xmm4                                 #9311.24
        addpd     %xmm5, %xmm2                                  #9312.22
        mulpd     %xmm2, %xmm4                                  #9313.28
        mulpd     %xmm4, %xmm7                                  #9314.24
        movaps    %xmm7, %xmm5                                  #9315.23
        movaps    %xmm1, %xmm2                                  #9320.26
        mulpd     %xmm7, %xmm7                                  #9316.40
        subpd     %xmm6, %xmm5                                  #9315.23
        subpd     %xmm5, %xmm7                                  #9316.28
        movq      -56(%r11,%rax), %xmm13                        #9290.634
        movhpd    -56(%rsi,%rax), %xmm13                        #9290.602
        mulpd     %xmm7, %xmm4                                  #9317.28
        mulpd     %xmm13, %xmm2                                 #9320.26
        movq      -80(%r11,%rax), %xmm15                        #9287.632
        movq      -72(%r11,%rax), %xmm8                         #9288.634
        movhpd    -80(%rsi,%rax), %xmm15                        #9287.600
        movhpd    -72(%rsi,%rax), %xmm8                         #9288.602
        mulpd     %xmm15, %xmm10                                #9318.32
        mulpd     %xmm4, %xmm15                                 #9319.32
        movaps    %xmm8, %xmm4                                  #9321.26
        addpd     %xmm2, %xmm4                                  #9321.26
        subpd     %xmm4, %xmm8                                  #9322.26
        movq      -40(%r11,%rax), %xmm3                         #9292.631
        movaps    %xmm4, %xmm7                                  #9323.26
        movq      -24(%r11,%rax), %xmm11                        #9294.631
        movhpd    -40(%rsi,%rax), %xmm3                         #9292.599
        movq      -16(%r11,%rax), %xmm12                        #9295.631
        movhpd    -24(%rsi,%rax), %xmm11                        #9294.599
        addpd     %xmm8, %xmm2                                  #9324.26
        addpd     %xmm10, %xmm7                                 #9323.26
        mulpd     %xmm1, %xmm3                                  #9329.34
        mulpd     %xmm1, %xmm11                                 #9330.34
        subpd     %xmm7, %xmm10                                 #9325.26
        addpd     %xmm2, %xmm15                                 #9326.26
        addpd     %xmm10, %xmm4                                 #9327.26
        movaps    %xmm1, %xmm8                                  #9332.22
        movhpd    -16(%rsi,%rax), %xmm12                        #9295.599
        mulpd     %xmm1, %xmm8                                  #9332.22
        addpd     %xmm4, %xmm15                                 #9328.26
        movups    %xmm12, 32(%rsp)                              #9295.599[spill]
        movq      -8(%r11,%rax), %xmm12                         #9296.631
        movhpd    -8(%rsi,%rax), %xmm12                         #9296.599
        mulpd     %xmm1, %xmm12                                 #9331.34
        movq      -48(%r11,%rax), %xmm9                         #9291.634
        movq      -32(%r11,%rax), %xmm14                        #9293.631
        movhpd    -48(%rsi,%rax), %xmm9                         #9291.602
        movhpd    -32(%rsi,%rax), %xmm14                        #9293.599
        addpd     %xmm9, %xmm3                                  #9329.22
        addpd     %xmm9, %xmm13                                 #9333.22
        addpd     %xmm11, %xmm14                                #9330.22
        mulpd     %xmm13, %xmm0                                 #9337.35
        mulpd     %xmm8, %xmm14                                 #9335.35
        addpd     %xmm0, %xmm15                                 #9337.23
        addpd     %xmm14, %xmm3                                 #9335.23
        movups    48(%rsp), %xmm9                               #9334.34[spill]
        mulpd     %xmm8, %xmm9                                  #9334.34
        mulpd     %xmm8, %xmm8                                  #9336.22
        movups    32(%rsp), %xmm11                              #9331.22[spill]
        addpd     %xmm12, %xmm11                                #9331.22
        addpd     %xmm9, %xmm11                                 #9334.22
        mulpd     %xmm8, %xmm11                                 #9338.35
        addpd     %xmm11, %xmm3                                 #9338.23
        mulpd     %xmm3, %xmm1                                  #9340.35
        movups    16(%rsp), %xmm0                               #9339.23[spill]
        addpd     %xmm15, %xmm0                                 #9339.23
        addpd     %xmm1, %xmm0                                  #9340.23
        addpd     %xmm0, %xmm7                                  #9341.28
        xorps     64(%rsp), %xmm7                               #9342.28[spill]
        movups    96(%rsp), %xmm0                               #9343.22[spill]
        movups    (%rsp), %xmm1                                 #9343.22[spill]
        blendvpd  %xmm0, %xmm7, %xmm1                           #9343.22
        jmp       ..B8.2        # Prob 100%                     #9343.22
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm1
	.cfi_endproc
# mark_end;
	.type	__svml_tan2_ha_e9,@function
	.size	__svml_tan2_ha_e9,.-__svml_tan2_ha_e9
..LN__svml_tan2_ha_e9.7:
	.data
# -- End  __svml_tan2_ha_e9
	.text
.L_2__routine_start___svml_tan4_ha_l9_8:
# -- Begin  __svml_tan4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tan4_ha_l9
# --- __svml_tan4_ha_l9(__m256d)
__svml_tan4_ha_l9:
# parameter 1: %ymm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #9380.1
        .byte     15                                            #9698.17
        .byte     30                                            #9698.17
        .byte     250                                           #9698.17
	.cfi_startproc
..___tag_value___svml_tan4_ha_l9.301:
..L302:
                                                        #9380.1
        pushq     %rbp                                          #9380.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #9380.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #9380.1
        subq      $192, %rsp                                    #9380.1
        xorl      %esi, %esi                                    #9440.5
        vmovapd   %ymm0, %ymm14                                 #9380.1
        vmovupd   1152+__svml_dtan_ha_data_internal(%rip), %ymm4 #9452.58
        vmovupd   __svml_dtan_ha_data_internal(%rip), %ymm0     #9453.17
        vmovupd   64+__svml_dtan_ha_data_internal(%rip), %ymm1  #9455.53
        vmovupd   256+__svml_dtan_ha_data_internal(%rip), %ymm3 #9456.57
        vmovupd   320+__svml_dtan_ha_data_internal(%rip), %ymm5 #9457.57
        vfmadd213pd %ymm4, %ymm14, %ymm0                        #9453.17
        vsubpd    %ymm4, %ymm0, %ymm8                           #9454.17
        vfnmadd213pd %ymm14, %ymm8, %ymm1                       #9458.22
        vmovapd   %ymm3, %ymm2                                  #9459.22
        vfnmadd213pd %ymm1, %ymm8, %ymm2                        #9459.22
        vmovapd   %ymm5, %ymm4                                  #9460.21
        vfnmadd213pd %ymm2, %ymm8, %ymm4                        #9460.21
        vsubpd    %ymm2, %ymm1, %ymm7                           #9461.23
        vsubpd    %ymm2, %ymm4, %ymm6                           #9462.23
        vfnmadd231pd %ymm8, %ymm3, %ymm7                        #9463.22
        vfmadd213pd %ymm6, %ymm5, %ymm8                         #9464.23
        vandpd    1024+__svml_dtan_ha_data_internal(%rip), %ymm14, %ymm9 #9442.16
        vcmpnle_uqpd 28160+__svml_dtan_ha_data_internal(%rip), %ymm9, %ymm13 #9446.30
        vsubpd    %ymm8, %ymm7, %ymm3                           #9465.22
        vmovmskpd %ymm13, %eax                                  #9448.78
        testl     %eax, %eax                                    #9467.66
        jne       ..B9.12       # Prob 5%                       #9467.66
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm3 ymm4 ymm9 ymm13 ymm14
..B9.2:                         # Preds ..B9.12 ..B9.1
                                # Execution count [1.00e+00]
        vpsllq    $3, %ymm0, %ymm5                              #9702.232
        lea       512+__svml_dtan_ha_data_internal(%rip), %r8   #9703.77
        vmovupd   896+__svml_dtan_ha_data_internal(%rip), %ymm0 #9706.55
        vpand     .L_2il0floatpacket.92(%rip), %ymm5, %ymm6     #9702.284
        vmulpd    %ymm4, %ymm4, %ymm5                           #9705.14
        vfmadd213pd 832+__svml_dtan_ha_data_internal(%rip), %ymm5, %ymm0 #9708.20
        vfmadd213pd 768+__svml_dtan_ha_data_internal(%rip), %ymm5, %ymm0 #9710.20
        vfmadd213pd 704+__svml_dtan_ha_data_internal(%rip), %ymm5, %ymm0 #9712.16
        vfmadd213pd 640+__svml_dtan_ha_data_internal(%rip), %ymm5, %ymm0 #9714.16
        vextractf128 $1, %ymm6, %xmm9                           #9703.514
        vmovd     %xmm6, %eax                                   #9703.167
        vmovd     %xmm9, %ecx                                   #9703.495
        vpextrd   $2, %xmm6, %edx                               #9703.320
        vmulpd    %ymm0, %ymm4, %ymm6                           #9715.16
        movslq    %eax, %rax                                    #9703.167
        movslq    %ecx, %rcx                                    #9703.495
        vpextrd   $2, %xmm9, %edi                               #9703.653
        movslq    %edx, %rdx                                    #9703.320
        movslq    %edi, %rdi                                    #9703.653
        vmovsd    -128(%rax,%r8), %xmm7                         #9703.77
        vmovsd    -128(%rcx,%r8), %xmm10                        #9703.405
        vfnmsub213pd %ymm3, %ymm5, %ymm6                        #9716.20
        vmovhpd   -128(%rdx,%r8), %xmm7, %xmm8                  #9703.60
        vmovhpd   -128(%rdi,%r8), %xmm10, %xmm11                #9703.388
        vmovsd    (%rax,%r8), %xmm12                            #9704.81
        vmovsd    (%rcx,%r8), %xmm15                            #9704.409
        vsubpd    %ymm6, %ymm4, %ymm7                           #9717.17
        vmovhpd   (%rdx,%r8), %xmm12, %xmm13                    #9704.64
        vmovhpd   (%rdi,%r8), %xmm15, %xmm1                     #9704.392
        vsubpd    %ymm7, %ymm4, %ymm4                           #9718.20
        vsubpd    %ymm6, %ymm4, %ymm6                           #9719.20
        vinsertf128 $1, %xmm11, %ymm8, %ymm2                    #9703.14
        vmovupd   960+__svml_dtan_ha_data_internal(%rip), %ymm8 #9725.49
        vaddpd    %ymm7, %ymm2, %ymm0                           #9720.21
        vmovapd   %ymm8, %ymm9                                  #9726.23
        vfnmadd231pd %ymm2, %ymm7, %ymm9                        #9726.23
        vsubpd    %ymm2, %ymm0, %ymm3                           #9721.20
        vdivpd    %ymm9, %ymm8, %ymm11                          #9731.23
        vsubpd    %ymm8, %ymm9, %ymm5                           #9727.20
        vsubpd    %ymm3, %ymm7, %ymm3                           #9722.20
        vfmadd231pd %ymm2, %ymm7, %ymm5                         #9728.20
        vfmadd213pd %ymm5, %ymm6, %ymm2                         #9729.20
        vinsertf128 $1, %xmm1, %ymm13, %ymm1                    #9704.18
        vaddpd    %ymm3, %ymm1, %ymm4                           #9723.20
        vfmadd213pd %ymm2, %ymm7, %ymm1                         #9730.20
        vaddpd    %ymm4, %ymm6, %ymm10                          #9724.20
        vmovapd   %ymm8, %ymm2                                  #9732.21
        vfnmadd231pd %ymm11, %ymm9, %ymm2                       #9732.21
        vfmadd213pd %ymm11, %ymm2, %ymm11                       #9733.27
        vfnmadd231pd %ymm11, %ymm9, %ymm8                       #9734.25
        vfmadd231pd %ymm11, %ymm1, %ymm8                        #9735.25
        vfmadd213pd %ymm11, %ymm8, %ymm11                       #9736.23
        vmulpd    %ymm11, %ymm0, %ymm12                         #9737.24
        vfmsub213pd %ymm0, %ymm12, %ymm9                        #9738.18
        vfnmadd213pd %ymm9, %ymm12, %ymm1                       #9739.22
        vsubpd    %ymm10, %ymm1, %ymm0                          #9740.22
        vfnmadd213pd %ymm12, %ymm11, %ymm0                      #9741.18
        testl     %esi, %esi                                    #9743.52
        jne       ..B9.4        # Prob 5%                       #9743.52
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm14
..B9.3:                         # Preds ..B9.4 ..B9.10 ..B9.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #9746.12
        popq      %rbp                                          #9746.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #9746.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.4:                         # Preds ..B9.2
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm14, 64(%rsp)                              #9743.200
        vmovupd   %ymm0, 128(%rsp)                              #9743.276
        je        ..B9.3        # Prob 95%                      #9743.380
                                # LOE rbx r12 r13 r14 r15 esi ymm0
..B9.7:                         # Preds ..B9.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #9743.460
                                # LOE rbx r12 r13 r14 r15 eax esi
..B9.15:                        # Preds ..B9.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %esi, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B9.8:                         # Preds ..B9.9 ..B9.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #9743.523
        jc        ..B9.11       # Prob 5%                       #9743.523
                                # LOE rbx r12 r14 r15 r13d
..B9.9:                         # Preds ..B9.11 ..B9.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #9743.476
        cmpl      $4, %r12d                                     #9743.471
        jl        ..B9.8        # Prob 82%                      #9743.471
                                # LOE rbx r12 r14 r15 r13d
..B9.10:                        # Preds ..B9.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #9743.682
        jmp       ..B9.3        # Prob 100%                     #9743.682
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B9.11:                        # Preds ..B9.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #9743.552
        lea       128(%rsp,%r12,8), %rsi                        #9743.552
..___tag_value___svml_tan4_ha_l9.319:
#       __svml_dtan_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtan_ha_cout_rare_internal             #9743.552
..___tag_value___svml_tan4_ha_l9.320:
        jmp       ..B9.9        # Prob 100%                     #9743.552
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B9.12:                        # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm4, 32(%rsp)                               #[spill]
        lea       __svml_dtan_ha_reduction_data_internal(%rip), %rcx #9561.736
        vmovupd   %ymm3, 64(%rsp)                               #[spill]
        vmovupd   1088+__svml_dtan_ha_data_internal(%rip), %ymm10 #9469.58
        vmovupd   %ymm0, (%rsp)                                 #[spill]
        vpand     .L_2il0floatpacket.78(%rip), %ymm14, %ymm4    #9559.18
        vpsrlq    $52, %ymm4, %ymm3                             #9560.18
        vandpd    %ymm9, %ymm10, %ymm6                          #9470.17
        vcmpeqpd  %ymm10, %ymm6, %ymm0                          #9471.25
        vmovmskpd %ymm0, %esi                                   #9473.48
        vmovd     %xmm3, %edi                                   #9561.124
        vextractf128 $1, %ymm3, %xmm5                           #9561.328
        vpextrd   $2, %xmm3, %r8d                               #9561.215
        lea       (%rdi,%rdi,2), %edx                           #9561.547
        vmovd     %xmm5, %r9d                                   #9561.309
        shll      $3, %edx                                      #9561.547
        lea       (%r8,%r8,2), %eax                             #9561.598
        vpextrd   $2, %xmm5, %r11d                              #9561.405
        lea       (%r9,%r9,2), %r10d                            #9561.649
        shll      $3, %eax                                      #9561.598
        vmovq     (%rdx,%rcx), %xmm12                           #9561.1284
        vmovhpd   (%rax,%rcx), %xmm12, %xmm15                   #9561.1252
        vpand     .L_2il0floatpacket.79(%rip), %ymm14, %ymm12   #9569.21
        lea       (%r11,%r11,2), %edi                           #9561.700
        vmovq     16(%rdx,%rcx), %xmm1                          #9563.1284
        vmovhpd   16(%rax,%rcx), %xmm1, %xmm0                   #9563.1252
        vmovq     8(%rdx,%rcx), %xmm9                           #9562.1285
        shll      $3, %r10d                                     #9561.649
        shll      $3, %edi                                      #9561.700
        vmovq     (%r10,%rcx), %xmm11                           #9561.1484
        vmovhpd   (%rdi,%rcx), %xmm11, %xmm8                    #9561.1452
        vpaddq    .L_2il0floatpacket.80(%rip), %ymm12, %ymm11   #9570.21
        vmovq     16(%r10,%rcx), %xmm4                          #9563.1484
        vmovhpd   16(%rdi,%rcx), %xmm4, %xmm3                   #9563.1452
        vmovq     8(%r10,%rcx), %xmm6                           #9562.1485
        vmovdqu   .L_2il0floatpacket.81(%rip), %ymm12           #9571.17
        vmovhpd   8(%rax,%rcx), %xmm9, %xmm7                    #9562.1253
        vmovhpd   8(%rdi,%rcx), %xmm6, %xmm2                    #9562.1453
        vpsrlq    $32, %ymm11, %ymm9                            #9578.12
        vpand     %ymm12, %ymm11, %ymm6                         #9579.12
        vinsertf128 $1, %xmm3, %ymm0, %ymm5                     #9563.1206
        vpsrlq    $32, %ymm5, %ymm0                             #9576.12
        vpand     %ymm12, %ymm5, %ymm5                          #9577.12
        vinsertf128 $1, %xmm8, %ymm15, %ymm10                   #9561.1206
        vinsertf128 $1, %xmm2, %ymm7, %ymm15                    #9562.1207
        vpsrlq    $32, %ymm15, %ymm4                            #9574.12
        vpand     %ymm12, %ymm15, %ymm2                         #9575.12
        vpand     %ymm12, %ymm10, %ymm7                         #9573.12
        vpsrlq    $32, %ymm10, %ymm10                           #9572.12
        vpmuludq  %ymm5, %ymm9, %ymm15                          #9585.13
        vpmuludq  %ymm0, %ymm9, %ymm5                           #9584.13
        vpmuludq  %ymm0, %ymm6, %ymm0                           #9590.13
        vpsrlq    $32, %ymm0, %ymm0                             #9592.13
        vpsrlq    $32, %ymm15, %ymm15                           #9610.13
        vpaddq    %ymm0, %ymm5, %ymm5                           #9596.13
        vpmuludq  %ymm2, %ymm6, %ymm1                           #9589.13
        vpand     %ymm12, %ymm1, %ymm11                         #9600.13
        vpsrlq    $32, %ymm1, %ymm1                             #9593.13
        vpaddq    %ymm5, %ymm11, %ymm11                         #9605.13
        vpmuludq  %ymm4, %ymm6, %ymm3                           #9588.13
        vpmuludq  %ymm2, %ymm9, %ymm2                           #9583.13
        vpaddq    %ymm11, %ymm15, %ymm11                        #9611.13
        vpand     %ymm12, %ymm3, %ymm0                          #9601.13
        vpaddq    %ymm1, %ymm2, %ymm5                           #9597.13
        vpsrlq    $32, %ymm11, %ymm15                           #9612.13
        vpaddq    %ymm5, %ymm0, %ymm0                           #9606.13
        vpaddq    %ymm0, %ymm15, %ymm5                          #9613.13
        vpsrlq    $32, %ymm3, %ymm0                             #9594.13
        vpsrlq    $32, %ymm5, %ymm2                             #9614.13
        vpsllq    $32, %ymm5, %ymm5                             #9621.19
        vpmuludq  %ymm7, %ymm6, %ymm8                           #9587.13
        vpmuludq  %ymm4, %ymm9, %ymm4                           #9582.13
        vpand     %ymm12, %ymm8, %ymm1                          #9602.13
        vpaddq    %ymm0, %ymm4, %ymm4                           #9598.13
        vpsrlq    $32, %ymm8, %ymm8                             #9595.13
        vpaddq    %ymm4, %ymm1, %ymm3                           #9607.13
        vpmuludq  %ymm10, %ymm6, %ymm0                          #9586.13
        vpmuludq  %ymm7, %ymm9, %ymm15                          #9581.13
        vpaddq    %ymm3, %ymm2, %ymm4                           #9615.13
        vpand     %ymm12, %ymm0, %ymm1                          #9603.13
        vpaddq    %ymm8, %ymm15, %ymm9                          #9599.13
        vmovupd   .L_2il0floatpacket.47(%rip), %ymm10           #9631.15
        vpsrlq    $32, %ymm4, %ymm3                             #9616.13
        vpaddq    %ymm9, %ymm1, %ymm7                           #9608.13
        vpand     %ymm12, %ymm4, %ymm4                          #9619.13
        vpand     %ymm12, %ymm11, %ymm12                        #9618.13
        vpaddq    %ymm7, %ymm3, %ymm3                           #9617.13
        vpand     .L_2il0floatpacket.82(%rip), %ymm14, %ymm7    #9625.17
        vpaddq    %ymm12, %ymm5, %ymm8                          #9623.19
        vpsllq    $32, %ymm3, %ymm2                             #9620.19
        vpxor     .L_2il0floatpacket.83(%rip), %ymm7, %ymm6     #9627.22
        vpand     .L_2il0floatpacket.86(%rip), %ymm8, %ymm12    #9641.20
        vpsrlq    $24, %ymm8, %ymm8                             #9652.19
        vpaddq    %ymm4, %ymm2, %ymm9                           #9622.19
        vpxor     .L_2il0floatpacket.85(%rip), %ymm7, %ymm2     #9639.22
        vpsllq    $28, %ymm12, %ymm15                           #9642.20
        vpsrlq    $12, %ymm9, %ymm11                            #9628.20
        vpand     .L_2il0floatpacket.88(%rip), %ymm9, %ymm9     #9650.20
        vpor      %ymm2, %ymm15, %ymm1                          #9643.20
        vpor      %ymm6, %ymm11, %ymm0                          #9629.20
        vpsllq    $40, %ymm9, %ymm6                             #9651.20
        vsubpd    %ymm2, %ymm1, %ymm15                          #9646.17
        vaddpd    %ymm10, %ymm0, %ymm3                          #9632.19
        vmovupd   .L_2il0floatpacket.52(%rip), %ymm1            #9669.16
        vsubpd    %ymm10, %ymm3, %ymm4                          #9633.9
        vsubpd    %ymm4, %ymm0, %ymm11                          #9634.17
        vpxor     .L_2il0floatpacket.87(%rip), %ymm7, %ymm0     #9648.22
        vpor      %ymm8, %ymm6, %ymm7                           #9653.20
        vpor      %ymm0, %ymm7, %ymm10                          #9654.20
        vsubpd    %ymm0, %ymm10, %ymm4                          #9657.18
        vandps    .L_2il0floatpacket.84(%rip), %ymm3, %ymm5     #9637.15
        vmovupd   .L_2il0floatpacket.48(%rip), %ymm3            #9662.16
        vaddpd    %ymm4, %ymm11, %ymm12                         #9658.11
        vmulpd    %ymm12, %ymm3, %ymm6                          #9664.13
        vsubpd    %ymm12, %ymm11, %ymm11                        #9659.17
        vaddpd    %ymm11, %ymm4, %ymm0                          #9660.18
        vmovapd   %ymm3, %ymm4                                  #9665.13
        vfmsub213pd %ymm6, %ymm12, %ymm4                        #9665.13
        vaddpd    %ymm0, %ymm15, %ymm10                         #9661.11
        vfmadd132pd .L_2il0floatpacket.89(%rip), %ymm4, %ymm12  #9666.13
        vandpd    .L_2il0floatpacket.51(%rip), %ymm14, %ymm15   #9670.11
        vcmpgt_oqpd %ymm1, %ymm15, %ymm9                        #9671.17
        vcmple_oqpd %ymm1, %ymm15, %ymm2                        #9672.20
        vfmadd213pd %ymm12, %ymm3, %ymm10                       #9667.13
        vandpd    %ymm14, %ymm2, %ymm7                          #9673.24
        vandpd    %ymm6, %ymm9, %ymm8                           #9674.25
        vorpd     %ymm8, %ymm7, %ymm12                          #9675.9
        vmovdqu   .L_2il0floatpacket.54(%rip), %xmm2            #9683.23
        vmovups   __VUNPACK_EVEN_ind1.2022.0.10(%rip), %ymm8    #9686.230
        vandpd    %ymm10, %ymm9, %ymm0                          #9676.9
        vextractf128 $1, %ymm12, %xmm3                          #9681.132
        vshufps   $221, %xmm3, %xmm12, %xmm1                    #9681.40
        vextractf128 $1, %ymm5, %xmm4                           #9678.134
        vshufps   $136, %xmm4, %xmm5, %xmm15                    #9678.41
        vpsrld    $31, %xmm1, %xmm5                             #9682.22
        vpsubd    %xmm5, %xmm2, %xmm6                           #9683.23
        vpaddd    %xmm6, %xmm15, %xmm7                          #9685.23
        vpsrld    $4, %xmm7, %xmm9                              #9685.23
        vmovupd   .L_2il0floatpacket.90(%rip), %ymm4            #9691.19
        vmovapd   %ymm12, %ymm2                                 #9693.26
        vpslld    $4, %xmm9, %xmm11                             #9688.23
        vpsubd    %xmm11, %xmm15, %xmm15                        #9689.23
        vpermps   %ymm9, %ymm8, %ymm10                          #9686.230
        vcvtdq2pd %xmm15, %ymm9                                 #9690.23
        vfmadd231pd %ymm9, %ymm4, %ymm2                         #9693.26
        vfnmadd213pd %ymm2, %ymm9, %ymm4                        #9694.27
        vsubpd    %ymm4, %ymm12, %ymm3                          #9695.27
        vmovupd   32(%rsp), %ymm4                               #9699.17[spill]
        vfmadd132pd .L_2il0floatpacket.91(%rip), %ymm3, %ymm9   #9696.27
        vmovupd   64(%rsp), %ymm3                               #9700.18[spill]
        vblendvpd %ymm13, %ymm2, %ymm4, %ymm4                   #9699.17
        vaddpd    %ymm9, %ymm0, %ymm5                           #9697.27
        vmovupd   (%rsp), %ymm0                                 #9698.17[spill]
        vblendvpd %ymm13, %ymm5, %ymm3, %ymm3                   #9700.18
        vandps    __VUNPACK_EVEN_mask.2022.0.10(%rip), %ymm10, %ymm1 #9686.216
        vblendvpd %ymm13, %ymm1, %ymm0, %ymm0                   #9698.17
        jmp       ..B9.2        # Prob 100%                     #9698.17
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm3 ymm4 ymm14
	.cfi_endproc
# mark_end;
	.type	__svml_tan4_ha_l9,@function
	.size	__svml_tan4_ha_l9,.-__svml_tan4_ha_l9
..LN__svml_tan4_ha_l9.8:
	.section .rodata, "a"
	.align 64
	.align 64
__VUNPACK_EVEN_ind1.2022.0.10:
	.long	0
	.long	0
	.long	1
	.long	0
	.long	2
	.long	0
	.long	3
	.long	0
	.space 32, 0x00 	# pad
	.align 64
__VUNPACK_EVEN_mask.2022.0.10:
	.long	-1
	.long	0
	.long	-1
	.long	0
	.long	-1
	.long	0
	.long	-1
	.long	0
	.data
# -- End  __svml_tan4_ha_l9
	.text
.L_2__routine_start___svml_dtan_ha_cout_rare_internal_9:
# -- Begin  __svml_dtan_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dtan_ha_cout_rare_internal
	.globl __svml_dtan_ha_cout_rare_internal
# --- __svml_dtan_ha_cout_rare_internal(const double *, double *)
__svml_dtan_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #9358.1
        .byte     15                                            #9372.19
        .byte     30                                            #9372.19
        .byte     250                                           #9372.19
	.cfi_startproc
..___tag_value___svml_dtan_ha_cout_rare_internal.330:
..L331:
                                                        #9358.1
        xorl      %eax, %eax                                    #9359.14
        movzwl    6(%rdi), %ecx                                 #9363.36
        movsd     (%rdi), %xmm1                                 #9361.18
        andl      $32752, %ecx                                  #9363.36
        movb      7(%rdi), %dl                                  #9361.18
        andb      $127, %dl                                     #9362.30
        movsd     %xmm1, -8(%rsp)                               #9361.9
        movb      %dl, -1(%rsp)                                 #9362.30
        cmpl      $32752, %ecx                                  #9363.62
        je        ..B10.3       # Prob 16%                      #9363.62
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm1
..B10.2:                        # Preds ..B10.1
                                # Execution count [1.00e+00]
        ret                                                     #9375.16
                                # LOE
..B10.3:                        # Preds ..B10.1
                                # Execution count [1.60e-01]: Infreq
        cmpl      $0, -8(%rsp)                                  #9365.92
        jne       ..B10.6       # Prob 50%                      #9365.92
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm1
..B10.4:                        # Preds ..B10.3
                                # Execution count [8.00e-02]: Infreq
        cmpl      $2146435072, -4(%rsp)                         #9365.200
        jne       ..B10.6       # Prob 50%                      #9365.200
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm1
..B10.5:                        # Preds ..B10.4
                                # Execution count [4.00e-02]: Infreq
        movsd     _vmldTanHATab(%rip), %xmm0                    #9367.46
        movl      $1, %eax                                      #9368.17
        mulsd     %xmm0, %xmm1                                  #9367.46
        movsd     %xmm1, (%rsi)                                 #9367.19
        ret                                                     #9367.19
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.6:                        # Preds ..B10.4 ..B10.3
                                # Execution count [1.20e-01]: Infreq
        mulsd     %xmm1, %xmm1                                  #9372.33
        movsd     %xmm1, (%rsi)                                 #9372.19
        ret                                                     #9372.19
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_dtan_ha_cout_rare_internal,@function
	.size	__svml_dtan_ha_cout_rare_internal,.-__svml_dtan_ha_cout_rare_internal
..LN__svml_dtan_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dtan_ha_cout_rare_internal
	.section .rodata, "a"
	.space 32, 0x00 	# pad
	.align 64
	.hidden __svml_dtan_ha_reduction_data_internal
	.globl __svml_dtan_ha_reduction_data_internal
__svml_dtan_ha_reduction_data_internal:
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
	.long	1
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	5
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	10
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	20
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	40
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	81
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	162
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	325
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	651
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1303
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2607
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	5215
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	10430
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	20860
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	41721
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	83443
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	166886
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	333772
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	667544
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1335088
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2670176
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	5340353
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	10680707
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	21361414
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	42722829
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	85445659
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	170891318
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	341782637
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	683565275
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1367130551
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2734261102
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1173554908
	.long	1
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2347109817
	.long	2
	.long	0
	.long	0
	.long	0
	.long	0
	.long	399252338
	.long	5
	.long	0
	.long	0
	.long	0
	.long	0
	.long	798504676
	.long	10
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1597009353
	.long	20
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3194018707
	.long	40
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2093070119
	.long	81
	.long	0
	.long	0
	.long	0
	.long	0
	.long	4186140238
	.long	162
	.long	0
	.long	0
	.long	0
	.long	0
	.long	4077313180
	.long	325
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3859659065
	.long	651
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3424350834
	.long	1303
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2553734372
	.long	2607
	.long	0
	.long	0
	.long	0
	.long	0
	.long	812501448
	.long	5215
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1625002897
	.long	10430
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3250005794
	.long	20860
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2205044292
	.long	41721
	.long	0
	.long	0
	.long	0
	.long	0
	.long	115121288
	.long	83443
	.long	0
	.long	0
	.long	0
	.long	0
	.long	230242576
	.long	166886
	.long	0
	.long	0
	.long	0
	.long	0
	.long	460485152
	.long	333772
	.long	0
	.long	0
	.long	0
	.long	0
	.long	920970305
	.long	667544
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1841940610
	.long	1335088
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3683881221
	.long	2670176
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3072795146
	.long	5340353
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1850622997
	.long	10680707
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3701245994
	.long	21361414
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3107524692
	.long	42722829
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1920082089
	.long	85445659
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3840164178
	.long	170891318
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3385361061
	.long	341782637
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2475754826
	.long	683565275
	.long	0
	.long	0
	.long	0
	.long	0
	.long	656542356
	.long	1367130551
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1313084713
	.long	2734261102
	.long	0
	.long	0
	.long	1
	.long	0
	.long	2626169427
	.long	1173554908
	.long	0
	.long	0
	.long	2
	.long	0
	.long	957371559
	.long	2347109817
	.long	0
	.long	0
	.long	5
	.long	0
	.long	1914743119
	.long	399252338
	.long	0
	.long	0
	.long	10
	.long	0
	.long	3829486239
	.long	798504676
	.long	0
	.long	0
	.long	20
	.long	0
	.long	3364005183
	.long	1597009353
	.long	0
	.long	0
	.long	40
	.long	0
	.long	2433043071
	.long	3194018707
	.long	0
	.long	0
	.long	81
	.long	0
	.long	571118846
	.long	2093070119
	.long	0
	.long	0
	.long	162
	.long	0
	.long	1142237692
	.long	4186140238
	.long	0
	.long	0
	.long	325
	.long	0
	.long	2284475384
	.long	4077313180
	.long	0
	.long	0
	.long	651
	.long	0
	.long	273983472
	.long	3859659065
	.long	0
	.long	0
	.long	1303
	.long	0
	.long	547966945
	.long	3424350834
	.long	0
	.long	0
	.long	2607
	.long	0
	.long	1095933890
	.long	2553734372
	.long	0
	.long	0
	.long	5215
	.long	0
	.long	2191867780
	.long	812501448
	.long	0
	.long	0
	.long	10430
	.long	0
	.long	88768265
	.long	1625002897
	.long	0
	.long	0
	.long	20860
	.long	0
	.long	177536531
	.long	3250005794
	.long	0
	.long	0
	.long	41721
	.long	0
	.long	355073063
	.long	2205044292
	.long	0
	.long	0
	.long	83443
	.long	0
	.long	710146126
	.long	115121288
	.long	0
	.long	0
	.long	166886
	.long	0
	.long	1420292253
	.long	230242576
	.long	0
	.long	0
	.long	333772
	.long	0
	.long	2840584506
	.long	460485152
	.long	0
	.long	0
	.long	667544
	.long	0
	.long	1386201717
	.long	920970305
	.long	0
	.long	0
	.long	1335088
	.long	0
	.long	2772403434
	.long	1841940610
	.long	0
	.long	0
	.long	2670176
	.long	0
	.long	1249839573
	.long	3683881221
	.long	0
	.long	0
	.long	5340353
	.long	0
	.long	2499679147
	.long	3072795146
	.long	0
	.long	0
	.long	10680707
	.long	0
	.long	704390999
	.long	1850622997
	.long	0
	.long	0
	.long	21361414
	.long	0
	.long	1408781999
	.long	3701245994
	.long	0
	.long	0
	.long	42722829
	.long	0
	.long	2817563999
	.long	3107524692
	.long	0
	.long	0
	.long	85445659
	.long	0
	.long	1340160702
	.long	1920082089
	.long	0
	.long	0
	.long	170891318
	.long	0
	.long	2680321405
	.long	3840164178
	.long	0
	.long	0
	.long	341782637
	.long	0
	.long	1065675514
	.long	3385361061
	.long	0
	.long	0
	.long	683565275
	.long	0
	.long	2131351028
	.long	2475754826
	.long	0
	.long	0
	.long	1367130551
	.long	0
	.long	4262702056
	.long	656542356
	.long	0
	.long	0
	.long	2734261102
	.long	0
	.long	4230436817
	.long	1313084713
	.long	0
	.long	0
	.long	1173554908
	.long	1
	.long	4165906339
	.long	2626169427
	.long	0
	.long	0
	.long	2347109817
	.long	2
	.long	4036845383
	.long	957371559
	.long	0
	.long	0
	.long	399252338
	.long	5
	.long	3778723471
	.long	1914743119
	.long	0
	.long	0
	.long	798504676
	.long	10
	.long	3262479647
	.long	3829486239
	.long	0
	.long	0
	.long	1597009353
	.long	20
	.long	2229991998
	.long	3364005183
	.long	0
	.long	0
	.long	3194018707
	.long	40
	.long	165016701
	.long	2433043071
	.long	0
	.long	0
	.long	2093070119
	.long	81
	.long	330033402
	.long	571118846
	.long	0
	.long	0
	.long	4186140238
	.long	162
	.long	660066805
	.long	1142237692
	.long	0
	.long	0
	.long	4077313180
	.long	325
	.long	1320133610
	.long	2284475384
	.long	0
	.long	0
	.long	3859659065
	.long	651
	.long	2640267220
	.long	273983472
	.long	0
	.long	0
	.long	3424350834
	.long	1303
	.long	985567145
	.long	547966945
	.long	0
	.long	0
	.long	2553734372
	.long	2607
	.long	1971134291
	.long	1095933890
	.long	0
	.long	0
	.long	812501448
	.long	5215
	.long	3942268582
	.long	2191867780
	.long	0
	.long	0
	.long	1625002897
	.long	10430
	.long	3589569869
	.long	88768265
	.long	0
	.long	0
	.long	3250005794
	.long	20860
	.long	2884172442
	.long	177536531
	.long	0
	.long	0
	.long	2205044292
	.long	41721
	.long	1473377588
	.long	355073063
	.long	0
	.long	0
	.long	115121288
	.long	83443
	.long	2946755177
	.long	710146126
	.long	0
	.long	0
	.long	230242576
	.long	166886
	.long	1598543059
	.long	1420292253
	.long	0
	.long	0
	.long	460485152
	.long	333772
	.long	3197086118
	.long	2840584506
	.long	0
	.long	0
	.long	920970305
	.long	667544
	.long	2099204941
	.long	1386201717
	.long	0
	.long	0
	.long	1841940610
	.long	1335088
	.long	4198409883
	.long	2772403434
	.long	0
	.long	0
	.long	3683881221
	.long	2670176
	.long	4101852471
	.long	1249839573
	.long	0
	.long	0
	.long	3072795146
	.long	5340353
	.long	3908737646
	.long	2499679147
	.long	0
	.long	0
	.long	1850622997
	.long	10680707
	.long	3522507997
	.long	704390999
	.long	0
	.long	0
	.long	3701245994
	.long	21361414
	.long	2750048699
	.long	1408781999
	.long	0
	.long	0
	.long	3107524692
	.long	42722829
	.long	1205130103
	.long	2817563999
	.long	0
	.long	0
	.long	1920082089
	.long	85445659
	.long	2410260206
	.long	1340160702
	.long	0
	.long	0
	.long	3840164178
	.long	170891318
	.long	525553116
	.long	2680321405
	.long	0
	.long	0
	.long	3385361061
	.long	341782637
	.long	1051106232
	.long	1065675514
	.long	0
	.long	0
	.long	2475754826
	.long	683565275
	.long	2102212464
	.long	2131351028
	.long	0
	.long	0
	.long	656542356
	.long	1367130551
	.long	4204424928
	.long	4262702056
	.long	0
	.long	0
	.long	1313084713
	.long	2734261102
	.long	4113882560
	.long	4230436817
	.long	1
	.long	0
	.long	2626169427
	.long	1173554908
	.long	3932797825
	.long	4165906339
	.long	2
	.long	0
	.long	957371559
	.long	2347109817
	.long	3570628355
	.long	4036845383
	.long	5
	.long	0
	.long	1914743119
	.long	399252338
	.long	2846289414
	.long	3778723471
	.long	10
	.long	0
	.long	3829486239
	.long	798504676
	.long	1397611533
	.long	3262479647
	.long	20
	.long	0
	.long	3364005183
	.long	1597009353
	.long	2795223067
	.long	2229991998
	.long	40
	.long	0
	.long	2433043071
	.long	3194018707
	.long	1295478838
	.long	165016701
	.long	81
	.long	0
	.long	571118846
	.long	2093070119
	.long	2590957677
	.long	330033402
	.long	162
	.long	0
	.long	1142237692
	.long	4186140238
	.long	886948059
	.long	660066805
	.long	325
	.long	0
	.long	2284475384
	.long	4077313180
	.long	1773896118
	.long	1320133610
	.long	651
	.long	0
	.long	273983472
	.long	3859659065
	.long	3547792237
	.long	2640267220
	.long	1303
	.long	0
	.long	547966945
	.long	3424350834
	.long	2800617179
	.long	985567145
	.long	2607
	.long	0
	.long	1095933890
	.long	2553734372
	.long	1306267062
	.long	1971134291
	.long	5215
	.long	0
	.long	2191867780
	.long	812501448
	.long	2612534124
	.long	3942268582
	.long	10430
	.long	0
	.long	88768265
	.long	1625002897
	.long	930100952
	.long	3589569869
	.long	20860
	.long	0
	.long	177536531
	.long	3250005794
	.long	1860201905
	.long	2884172442
	.long	41721
	.long	0
	.long	355073063
	.long	2205044292
	.long	3720403810
	.long	1473377588
	.long	83443
	.long	0
	.long	710146126
	.long	115121288
	.long	3145840325
	.long	2946755177
	.long	166886
	.long	0
	.long	1420292253
	.long	230242576
	.long	1996713354
	.long	1598543059
	.long	333772
	.long	0
	.long	2840584506
	.long	460485152
	.long	3993426708
	.long	3197086118
	.long	667544
	.long	0
	.long	1386201717
	.long	920970305
	.long	3691886121
	.long	2099204941
	.long	1335088
	.long	0
	.long	2772403434
	.long	1841940610
	.long	3088804946
	.long	4198409883
	.long	2670176
	.long	0
	.long	1249839573
	.long	3683881221
	.long	1882642597
	.long	4101852471
	.long	5340353
	.long	0
	.long	2499679147
	.long	3072795146
	.long	3765285194
	.long	3908737646
	.long	10680707
	.long	0
	.long	704390999
	.long	1850622997
	.long	3235603093
	.long	3522507997
	.long	21361414
	.long	0
	.long	1408781999
	.long	3701245994
	.long	2176238891
	.long	2750048699
	.long	42722829
	.long	0
	.long	2817563999
	.long	3107524692
	.long	57510486
	.long	1205130103
	.long	85445659
	.long	0
	.long	1340160702
	.long	1920082089
	.long	115020972
	.long	2410260206
	.long	170891318
	.long	0
	.long	2680321405
	.long	3840164178
	.long	230041945
	.long	525553116
	.long	341782637
	.long	0
	.long	1065675514
	.long	3385361061
	.long	460083891
	.long	1051106232
	.long	683565275
	.long	0
	.long	2131351028
	.long	2475754826
	.long	920167782
	.long	2102212464
	.long	1367130551
	.long	0
	.long	4262702056
	.long	656542356
	.long	1840335564
	.long	4204424928
	.long	2734261102
	.long	0
	.long	4230436817
	.long	1313084713
	.long	3680671129
	.long	4113882560
	.long	1173554908
	.long	1
	.long	4165906339
	.long	2626169427
	.long	3066374962
	.long	3932797825
	.long	2347109817
	.long	2
	.long	4036845383
	.long	957371559
	.long	1837782628
	.long	3570628355
	.long	399252338
	.long	5
	.long	3778723471
	.long	1914743119
	.long	3675565257
	.long	2846289414
	.long	798504676
	.long	10
	.long	3262479647
	.long	3829486239
	.long	3056163219
	.long	1397611533
	.long	1597009353
	.long	20
	.long	2229991998
	.long	3364005183
	.long	1817359143
	.long	2795223067
	.long	3194018707
	.long	40
	.long	165016701
	.long	2433043071
	.long	3634718287
	.long	1295478838
	.long	2093070119
	.long	81
	.long	330033402
	.long	571118846
	.long	2974469278
	.long	2590957677
	.long	4186140238
	.long	162
	.long	660066805
	.long	1142237692
	.long	1653971260
	.long	886948059
	.long	4077313180
	.long	325
	.long	1320133610
	.long	2284475384
	.long	3307942520
	.long	1773896118
	.long	3859659065
	.long	651
	.long	2640267220
	.long	273983472
	.long	2320917745
	.long	3547792237
	.long	3424350834
	.long	1303
	.long	985567145
	.long	547966945
	.long	346868194
	.long	2800617179
	.long	2553734372
	.long	2607
	.long	1971134291
	.long	1095933890
	.long	693736388
	.long	1306267062
	.long	812501448
	.long	5215
	.long	3942268582
	.long	2191867780
	.long	1387472776
	.long	2612534124
	.long	1625002897
	.long	10430
	.long	3589569869
	.long	88768265
	.long	2774945552
	.long	930100952
	.long	3250005794
	.long	20860
	.long	2884172442
	.long	177536531
	.long	1254923809
	.long	1860201905
	.long	2205044292
	.long	41721
	.long	1473377588
	.long	355073063
	.long	2509847619
	.long	3720403810
	.long	115121288
	.long	83443
	.long	2946755177
	.long	710146126
	.long	724727943
	.long	3145840325
	.long	230242576
	.long	166886
	.long	1598543059
	.long	1420292253
	.long	1449455886
	.long	1996713354
	.long	460485152
	.long	333772
	.long	3197086118
	.long	2840584506
	.long	2898911772
	.long	3993426708
	.long	920970305
	.long	667544
	.long	2099204941
	.long	1386201717
	.long	1502856249
	.long	3691886121
	.long	1841940610
	.long	1335088
	.long	4198409883
	.long	2772403434
	.long	3005712498
	.long	3088804946
	.long	3683881221
	.long	2670176
	.long	4101852471
	.long	1249839573
	.long	1716457700
	.long	1882642597
	.long	3072795146
	.long	5340353
	.long	3908737646
	.long	2499679147
	.long	3432915400
	.long	3765285194
	.long	1850622997
	.long	10680707
	.long	3522507997
	.long	704390999
	.long	2570863504
	.long	3235603093
	.long	3701245994
	.long	21361414
	.long	2750048699
	.long	1408781999
	.long	846759712
	.long	2176238891
	.long	3107524692
	.long	42722829
	.long	1205130103
	.long	2817563999
	.long	1693519425
	.long	57510486
	.long	1920082089
	.long	85445659
	.long	2410260206
	.long	1340160702
	.long	3387038850
	.long	115020972
	.long	3840164178
	.long	170891318
	.long	525553116
	.long	2680321405
	.long	2479110404
	.long	230041945
	.long	3385361061
	.long	341782637
	.long	1051106232
	.long	1065675514
	.long	663253512
	.long	460083891
	.long	2475754826
	.long	683565275
	.long	2102212464
	.long	2131351028
	.long	1326507024
	.long	920167782
	.long	656542356
	.long	1367130551
	.long	4204424928
	.long	4262702056
	.long	2653014048
	.long	1840335564
	.long	1313084713
	.long	2734261102
	.long	4113882560
	.long	4230436817
	.long	1011060801
	.long	3680671129
	.long	2626169427
	.long	1173554908
	.long	3932797825
	.long	4165906339
	.long	2022121603
	.long	3066374962
	.long	957371559
	.long	2347109817
	.long	3570628355
	.long	4036845383
	.long	4044243207
	.long	1837782628
	.long	1914743119
	.long	399252338
	.long	2846289414
	.long	3778723471
	.long	3793519119
	.long	3675565257
	.long	3829486239
	.long	798504676
	.long	1397611533
	.long	3262479647
	.long	3292070943
	.long	3056163219
	.long	3364005183
	.long	1597009353
	.long	2795223067
	.long	2229991998
	.long	2289174591
	.long	1817359143
	.long	2433043071
	.long	3194018707
	.long	1295478838
	.long	165016701
	.long	283381887
	.long	3634718287
	.long	571118846
	.long	2093070119
	.long	2590957677
	.long	330033402
	.long	566763775
	.long	2974469278
	.long	1142237692
	.long	4186140238
	.long	886948059
	.long	660066805
	.long	1133527550
	.long	1653971260
	.long	2284475384
	.long	4077313180
	.long	1773896118
	.long	1320133610
	.long	2267055100
	.long	3307942520
	.long	273983472
	.long	3859659065
	.long	3547792237
	.long	2640267220
	.long	239142905
	.long	2320917745
	.long	547966945
	.long	3424350834
	.long	2800617179
	.long	985567145
	.long	478285810
	.long	346868194
	.long	1095933890
	.long	2553734372
	.long	1306267062
	.long	1971134291
	.long	956571621
	.long	693736388
	.long	2191867780
	.long	812501448
	.long	2612534124
	.long	3942268582
	.long	1913143242
	.long	1387472776
	.long	88768265
	.long	1625002897
	.long	930100952
	.long	3589569869
	.long	3826286484
	.long	2774945552
	.long	177536531
	.long	3250005794
	.long	1860201905
	.long	2884172442
	.long	3357605672
	.long	1254923809
	.long	355073063
	.long	2205044292
	.long	3720403810
	.long	1473377588
	.long	2420244049
	.long	2509847619
	.long	710146126
	.long	115121288
	.long	3145840325
	.long	2946755177
	.long	545520802
	.long	724727943
	.long	1420292253
	.long	230242576
	.long	1996713354
	.long	1598543059
	.long	1091041605
	.long	1449455886
	.long	2840584506
	.long	460485152
	.long	3993426708
	.long	3197086118
	.long	2182083211
	.long	2898911772
	.long	1386201717
	.long	920970305
	.long	3691886121
	.long	2099204941
	.long	69199126
	.long	1502856249
	.long	2772403434
	.long	1841940610
	.long	3088804946
	.long	4198409883
	.long	138398252
	.long	3005712498
	.long	1249839573
	.long	3683881221
	.long	1882642597
	.long	4101852471
	.long	276796504
	.long	1716457700
	.long	2499679147
	.long	3072795146
	.long	3765285194
	.long	3908737646
	.long	553593009
	.long	3432915400
	.long	704390999
	.long	1850622997
	.long	3235603093
	.long	3522507997
	.long	1107186019
	.long	2570863504
	.long	1408781999
	.long	3701245994
	.long	2176238891
	.long	2750048699
	.long	2214372039
	.long	846759712
	.long	2817563999
	.long	3107524692
	.long	57510486
	.long	1205130103
	.long	133776782
	.long	1693519425
	.long	1340160702
	.long	1920082089
	.long	115020972
	.long	2410260206
	.long	267553565
	.long	3387038850
	.long	2680321405
	.long	3840164178
	.long	230041945
	.long	525553116
	.long	535107130
	.long	2479110404
	.long	1065675514
	.long	3385361061
	.long	460083891
	.long	1051106232
	.long	1070214261
	.long	663253512
	.long	2131351028
	.long	2475754826
	.long	920167782
	.long	2102212464
	.long	2140428522
	.long	1326507024
	.long	4262702056
	.long	656542356
	.long	1840335564
	.long	4204424928
	.long	4280857045
	.long	2653014048
	.long	4230436817
	.long	1313084713
	.long	3680671129
	.long	4113882560
	.long	4266746795
	.long	1011060801
	.long	4165906339
	.long	2626169427
	.long	3066374962
	.long	3932797825
	.long	4238526295
	.long	2022121603
	.long	4036845383
	.long	957371559
	.long	1837782628
	.long	3570628355
	.long	4182085295
	.long	4044243207
	.long	3778723471
	.long	1914743119
	.long	3675565257
	.long	2846289414
	.long	4069203294
	.long	3793519119
	.long	3262479647
	.long	3829486239
	.long	3056163219
	.long	1397611533
	.long	3843439293
	.long	3292070943
	.long	2229991998
	.long	3364005183
	.long	1817359143
	.long	2795223067
	.long	3391911291
	.long	2289174591
	.long	165016701
	.long	2433043071
	.long	3634718287
	.long	1295478838
	.long	2488855287
	.long	283381887
	.long	330033402
	.long	571118846
	.long	2974469278
	.long	2590957677
	.long	682743279
	.long	566763775
	.long	660066805
	.long	1142237692
	.long	1653971260
	.long	886948059
	.long	1365486558
	.long	1133527550
	.long	1320133610
	.long	2284475384
	.long	3307942520
	.long	1773896118
	.long	2730973117
	.long	2267055100
	.long	2640267220
	.long	273983472
	.long	2320917745
	.long	3547792237
	.long	1166978938
	.long	239142905
	.long	985567145
	.long	547966945
	.long	346868194
	.long	2800617179
	.long	2333957877
	.long	478285810
	.long	1971134291
	.long	1095933890
	.long	693736388
	.long	1306267062
	.long	372948459
	.long	956571621
	.long	3942268582
	.long	2191867780
	.long	1387472776
	.long	2612534124
	.long	745896919
	.long	1913143242
	.long	3589569869
	.long	88768265
	.long	2774945552
	.long	930100952
	.long	1491793838
	.long	3826286484
	.long	2884172442
	.long	177536531
	.long	1254923809
	.long	1860201905
	.long	2983587677
	.long	3357605672
	.long	1473377588
	.long	355073063
	.long	2509847619
	.long	3720403810
	.long	1672208059
	.long	2420244049
	.long	2946755177
	.long	710146126
	.long	724727943
	.long	3145840325
	.long	3344416119
	.long	545520802
	.long	1598543059
	.long	1420292253
	.long	1449455886
	.long	1996713354
	.long	2393864943
	.long	1091041605
	.long	3197086118
	.long	2840584506
	.long	2898911772
	.long	3993426708
	.long	492762590
	.long	2182083211
	.long	2099204941
	.long	1386201717
	.long	1502856249
	.long	3691886121
	.long	985525180
	.long	69199126
	.long	4198409883
	.long	2772403434
	.long	3005712498
	.long	3088804946
	.long	1971050360
	.long	138398252
	.long	4101852471
	.long	1249839573
	.long	1716457700
	.long	1882642597
	.long	3942100721
	.long	276796504
	.long	3908737646
	.long	2499679147
	.long	3432915400
	.long	3765285194
	.long	3589234146
	.long	553593009
	.long	3522507997
	.long	704390999
	.long	2570863504
	.long	3235603093
	.long	2883500997
	.long	1107186019
	.long	2750048699
	.long	1408781999
	.long	846759712
	.long	2176238891
	.long	1472034698
	.long	2214372039
	.long	1205130103
	.long	2817563999
	.long	1693519425
	.long	57510486
	.long	2944069397
	.long	133776782
	.long	2410260206
	.long	1340160702
	.long	3387038850
	.long	115020972
	.long	1593171499
	.long	267553565
	.long	525553116
	.long	2680321405
	.long	2479110404
	.long	230041945
	.long	3186342998
	.long	535107130
	.long	1051106232
	.long	1065675514
	.long	663253512
	.long	460083891
	.long	2077718700
	.long	1070214261
	.long	2102212464
	.long	2131351028
	.long	1326507024
	.long	920167782
	.long	4155437400
	.long	2140428522
	.long	4204424928
	.long	4262702056
	.long	2653014048
	.long	1840335564
	.long	4015907504
	.long	4280857045
	.long	4113882560
	.long	4230436817
	.long	1011060801
	.long	3680671129
	.long	3736847713
	.long	4266746795
	.long	3932797825
	.long	4165906339
	.long	2022121603
	.long	3066374962
	.long	3178728131
	.long	4238526295
	.long	3570628355
	.long	4036845383
	.long	4044243207
	.long	1837782628
	.long	2062488966
	.long	4182085295
	.long	2846289414
	.long	3778723471
	.long	3793519119
	.long	3675565257
	.long	4124977933
	.long	4069203294
	.long	1397611533
	.long	3262479647
	.long	3292070943
	.long	3056163219
	.long	3954988571
	.long	3843439293
	.long	2795223067
	.long	2229991998
	.long	2289174591
	.long	1817359143
	.long	3615009846
	.long	3391911291
	.long	1295478838
	.long	165016701
	.long	283381887
	.long	3634718287
	.long	2935052397
	.long	2488855287
	.long	2590957677
	.long	330033402
	.long	566763775
	.long	2974469278
	.long	1575137499
	.long	682743279
	.long	886948059
	.long	660066805
	.long	1133527550
	.long	1653971260
	.long	3150274999
	.long	1365486558
	.long	1773896118
	.long	1320133610
	.long	2267055100
	.long	3307942520
	.long	2005582702
	.long	2730973117
	.long	3547792237
	.long	2640267220
	.long	239142905
	.long	2320917745
	.long	4011165404
	.long	1166978938
	.long	2800617179
	.long	985567145
	.long	478285810
	.long	346868194
	.long	3727363513
	.long	2333957877
	.long	1306267062
	.long	1971134291
	.long	956571621
	.long	693736388
	.long	3159759730
	.long	372948459
	.long	2612534124
	.long	3942268582
	.long	1913143242
	.long	1387472776
	.long	2024552164
	.long	745896919
	.long	930100952
	.long	3589569869
	.long	3826286484
	.long	2774945552
	.long	4049104329
	.long	1491793838
	.long	1860201905
	.long	2884172442
	.long	3357605672
	.long	1254923809
	.long	3803241362
	.long	2983587677
	.long	3720403810
	.long	1473377588
	.long	2420244049
	.long	2509847619
	.long	3311515428
	.long	1672208059
	.long	3145840325
	.long	2946755177
	.long	545520802
	.long	724727943
	.long	2328063560
	.long	3344416119
	.long	1996713354
	.long	1598543059
	.long	1091041605
	.long	1449455886
	.long	361159825
	.long	2393864943
	.long	3993426708
	.long	3197086118
	.long	2182083211
	.long	2898911772
	.long	722319651
	.long	492762590
	.long	3691886121
	.long	2099204941
	.long	69199126
	.long	1502856249
	.long	1444639302
	.long	985525180
	.long	3088804946
	.long	4198409883
	.long	138398252
	.long	3005712498
	.long	2889278605
	.long	1971050360
	.long	1882642597
	.long	4101852471
	.long	276796504
	.long	1716457700
	.long	1483589915
	.long	3942100721
	.long	3765285194
	.long	3908737646
	.long	553593009
	.long	3432915400
	.long	2967179831
	.long	3589234146
	.long	3235603093
	.long	3522507997
	.long	1107186019
	.long	2570863504
	.long	1639392366
	.long	2883500997
	.long	2176238891
	.long	2750048699
	.long	2214372039
	.long	846759712
	.long	3278784732
	.long	1472034698
	.long	57510486
	.long	1205130103
	.long	133776782
	.long	1693519425
	.long	2262602168
	.long	2944069397
	.long	115020972
	.long	2410260206
	.long	267553565
	.long	3387038850
	.long	230237041
	.long	1593171499
	.long	230041945
	.long	525553116
	.long	535107130
	.long	2479110404
	.long	460474083
	.long	3186342998
	.long	460083891
	.long	1051106232
	.long	1070214261
	.long	663253512
	.long	920948167
	.long	2077718700
	.long	920167782
	.long	2102212464
	.long	2140428522
	.long	1326507024
	.long	1841896334
	.long	4155437400
	.long	1840335564
	.long	4204424928
	.long	4280857045
	.long	2653014048
	.long	3683792669
	.long	4015907504
	.long	3680671129
	.long	4113882560
	.long	4266746795
	.long	1011060801
	.long	3072618042
	.long	3736847713
	.long	3066374962
	.long	3932797825
	.long	4238526295
	.long	2022121603
	.long	1850268788
	.long	3178728131
	.long	1837782628
	.long	3570628355
	.long	4182085295
	.long	4044243207
	.long	3700537577
	.long	2062488966
	.long	3675565257
	.long	2846289414
	.long	4069203294
	.long	3793519119
	.long	3106107858
	.long	4124977933
	.long	3056163219
	.long	1397611533
	.long	3843439293
	.long	3292070943
	.long	1917248420
	.long	3954988571
	.long	1817359143
	.long	2795223067
	.long	3391911291
	.long	2289174591
	.long	3834496840
	.long	3615009846
	.long	3634718287
	.long	1295478838
	.long	2488855287
	.long	283381887
	.long	3374026384
	.long	2935052397
	.long	2974469278
	.long	2590957677
	.long	682743279
	.long	566763775
	.long	2453085473
	.long	1575137499
	.long	1653971260
	.long	886948059
	.long	1365486558
	.long	1133527550
	.long	611203650
	.long	3150274999
	.long	3307942520
	.long	1773896118
	.long	2730973117
	.long	2267055100
	.long	1222407300
	.long	2005582702
	.long	2320917745
	.long	3547792237
	.long	1166978938
	.long	239142905
	.long	2444814601
	.long	4011165404
	.long	346868194
	.long	2800617179
	.long	2333957877
	.long	478285810
	.long	594661906
	.long	3727363513
	.long	693736388
	.long	1306267062
	.long	372948459
	.long	956571621
	.long	1189323812
	.long	3159759730
	.long	1387472776
	.long	2612534124
	.long	745896919
	.long	1913143242
	.long	2378647625
	.long	2024552164
	.long	2774945552
	.long	930100952
	.long	1491793838
	.long	3826286484
	.long	462327955
	.long	4049104329
	.long	1254923809
	.long	1860201905
	.long	2983587677
	.long	3357605672
	.long	924655910
	.long	3803241362
	.long	2509847619
	.long	3720403810
	.long	1672208059
	.long	2420244049
	.long	1849311821
	.long	3311515428
	.long	724727943
	.long	3145840325
	.long	3344416119
	.long	545520802
	.long	3698623643
	.long	2328063560
	.long	1449455886
	.long	1996713354
	.long	2393864943
	.long	1091041605
	.long	3102279991
	.long	361159825
	.long	2898911772
	.long	3993426708
	.long	492762590
	.long	2182083211
	.long	1909592686
	.long	722319651
	.long	1502856249
	.long	3691886121
	.long	985525180
	.long	69199126
	.long	3819185373
	.long	1444639302
	.long	3005712498
	.long	3088804946
	.long	1971050360
	.long	138398252
	.long	3343403450
	.long	2889278605
	.long	1716457700
	.long	1882642597
	.long	3942100721
	.long	276796504
	.long	2391839604
	.long	1483589915
	.long	3432915400
	.long	3765285194
	.long	3589234146
	.long	553593009
	.long	488711913
	.long	2967179831
	.long	2570863504
	.long	3235603093
	.long	2883500997
	.long	1107186019
	.long	977423826
	.long	1639392366
	.long	846759712
	.long	2176238891
	.long	1472034698
	.long	2214372039
	.long	1954847653
	.long	3278784732
	.long	1693519425
	.long	57510486
	.long	2944069397
	.long	133776782
	.long	3909695307
	.long	2262602168
	.long	3387038850
	.long	115020972
	.long	1593171499
	.long	267553565
	.long	3524423319
	.long	230237041
	.long	2479110404
	.long	230041945
	.long	3186342998
	.long	535107130
	.long	2753879342
	.long	460474083
	.long	663253512
	.long	460083891
	.long	2077718700
	.long	1070214261
	.long	1212791388
	.long	920948167
	.long	1326507024
	.long	920167782
	.long	4155437400
	.long	2140428522
	.long	2425582776
	.long	1841896334
	.long	2653014048
	.long	1840335564
	.long	4015907504
	.long	4280857045
	.long	556198256
	.long	3683792669
	.long	1011060801
	.long	3680671129
	.long	3736847713
	.long	4266746795
	.long	1112396512
	.long	3072618042
	.long	2022121603
	.long	3066374962
	.long	3178728131
	.long	4238526295
	.long	2224793024
	.long	1850268788
	.long	4044243207
	.long	1837782628
	.long	2062488966
	.long	4182085295
	.long	154618752
	.long	3700537577
	.long	3793519119
	.long	3675565257
	.long	4124977933
	.long	4069203294
	.long	309237504
	.long	3106107858
	.long	3292070943
	.long	3056163219
	.long	3954988571
	.long	3843439293
	.long	618475008
	.long	1917248420
	.long	2289174591
	.long	1817359143
	.long	3615009846
	.long	3391911291
	.long	1236950016
	.long	3834496840
	.long	283381887
	.long	3634718287
	.long	2935052397
	.long	2488855287
	.long	2473900033
	.long	3374026384
	.long	566763775
	.long	2974469278
	.long	1575137499
	.long	682743279
	.long	652832771
	.long	2453085473
	.long	1133527550
	.long	1653971260
	.long	3150274999
	.long	1365486558
	.long	1305665542
	.long	611203650
	.long	2267055100
	.long	3307942520
	.long	2005582702
	.long	2730973117
	.long	2611331084
	.long	1222407300
	.long	239142905
	.long	2320917745
	.long	4011165404
	.long	1166978938
	.long	927694873
	.long	2444814601
	.long	478285810
	.long	346868194
	.long	3727363513
	.long	2333957877
	.long	1855389746
	.long	594661906
	.long	956571621
	.long	693736388
	.long	3159759730
	.long	372948459
	.long	3710779492
	.long	1189323812
	.long	1913143242
	.long	1387472776
	.long	2024552164
	.long	745896919
	.long	3126591689
	.long	2378647625
	.long	3826286484
	.long	2774945552
	.long	4049104329
	.long	1491793838
	.long	1958216082
	.long	462327955
	.long	3357605672
	.long	1254923809
	.long	3803241362
	.long	2983587677
	.long	3916432164
	.long	924655910
	.long	2420244049
	.long	2509847619
	.long	3311515428
	.long	1672208059
	.long	3537897033
	.long	1849311821
	.long	545520802
	.long	724727943
	.long	2328063560
	.long	3344416119
	.long	2780826770
	.long	3698623643
	.long	1091041605
	.long	1449455886
	.long	361159825
	.long	2393864943
	.long	1266686244
	.long	3102279991
	.long	2182083211
	.long	2898911772
	.long	722319651
	.long	492762590
	.long	2533372489
	.long	1909592686
	.long	69199126
	.long	1502856249
	.long	1444639302
	.long	985525180
	.long	771777682
	.long	3819185373
	.long	138398252
	.long	3005712498
	.long	2889278605
	.long	1971050360
	.long	1543555365
	.long	3343403450
	.long	276796504
	.long	1716457700
	.long	1483589915
	.long	3942100721
	.long	3087110731
	.long	2391839604
	.long	553593009
	.long	3432915400
	.long	2967179831
	.long	3589234146
	.long	1879254167
	.long	488711913
	.long	1107186019
	.long	2570863504
	.long	1639392366
	.long	2883500997
	.long	3758508334
	.long	977423826
	.long	2214372039
	.long	846759712
	.long	3278784732
	.long	1472034698
	.long	3222049373
	.long	1954847653
	.long	133776782
	.long	1693519425
	.long	2262602168
	.long	2944069397
	.long	2149131451
	.long	3909695307
	.long	267553565
	.long	3387038850
	.long	230237041
	.long	1593171499
	.long	3295607
	.long	3524423319
	.long	535107130
	.long	2479110404
	.long	460474083
	.long	3186342998
	.long	6591214
	.long	2753879342
	.long	1070214261
	.long	663253512
	.long	920948167
	.long	2077718700
	.long	13182429
	.long	1212791388
	.long	2140428522
	.long	1326507024
	.long	1841896334
	.long	4155437400
	.long	26364858
	.long	2425582776
	.long	4280857045
	.long	2653014048
	.long	3683792669
	.long	4015907504
	.long	52729717
	.long	556198256
	.long	4266746795
	.long	1011060801
	.long	3072618042
	.long	3736847713
	.long	105459434
	.long	1112396512
	.long	4238526295
	.long	2022121603
	.long	1850268788
	.long	3178728131
	.long	210918868
	.long	2224793024
	.long	4182085295
	.long	4044243207
	.long	3700537577
	.long	2062488966
	.long	421837736
	.long	154618752
	.long	4069203294
	.long	3793519119
	.long	3106107858
	.long	4124977933
	.long	843675472
	.long	309237504
	.long	3843439293
	.long	3292070943
	.long	1917248420
	.long	3954988571
	.long	1687350944
	.long	618475008
	.long	3391911291
	.long	2289174591
	.long	3834496840
	.long	3615009846
	.long	3374701889
	.long	1236950016
	.long	2488855287
	.long	283381887
	.long	3374026384
	.long	2935052397
	.long	2454436482
	.long	2473900033
	.long	682743279
	.long	566763775
	.long	2453085473
	.long	1575137499
	.long	613905668
	.long	652832771
	.long	1365486558
	.long	1133527550
	.long	611203650
	.long	3150274999
	.long	1227811337
	.long	1305665542
	.long	2730973117
	.long	2267055100
	.long	1222407300
	.long	2005582702
	.long	2455622675
	.long	2611331084
	.long	1166978938
	.long	239142905
	.long	2444814601
	.long	4011165404
	.long	616278055
	.long	927694873
	.long	2333957877
	.long	478285810
	.long	594661906
	.long	3727363513
	.long	1232556110
	.long	1855389746
	.long	372948459
	.long	956571621
	.long	1189323812
	.long	3159759730
	.long	2465112221
	.long	3710779492
	.long	745896919
	.long	1913143242
	.long	2378647625
	.long	2024552164
	.long	635257146
	.long	3126591689
	.long	1491793838
	.long	3826286484
	.long	462327955
	.long	4049104329
	.long	1270514292
	.long	1958216082
	.long	2983587677
	.long	3357605672
	.long	924655910
	.long	3803241362
	.long	2541028584
	.long	3916432164
	.long	1672208059
	.long	2420244049
	.long	1849311821
	.long	3311515428
	.long	787089873
	.long	3537897033
	.long	3344416119
	.long	545520802
	.long	3698623643
	.long	2328063560
	.long	1574179747
	.long	2780826770
	.long	2393864943
	.long	1091041605
	.long	3102279991
	.long	361159825
	.long	3148359494
	.long	1266686244
	.long	492762590
	.long	2182083211
	.long	1909592686
	.long	722319651
	.long	2001751692
	.long	2533372489
	.long	985525180
	.long	69199126
	.long	3819185373
	.long	1444639302
	.long	4003503385
	.long	771777682
	.long	1971050360
	.long	138398252
	.long	3343403450
	.long	2889278605
	.long	3712039474
	.long	1543555365
	.long	3942100721
	.long	276796504
	.long	2391839604
	.long	1483589915
	.long	3129111652
	.long	3087110731
	.long	3589234146
	.long	553593009
	.long	488711913
	.long	2967179831
	.long	1963256009
	.long	1879254167
	.long	2883500997
	.long	1107186019
	.long	977423826
	.long	1639392366
	.long	3926512018
	.long	3758508334
	.long	1472034698
	.long	2214372039
	.long	1954847653
	.long	3278784732
	.long	3558056740
	.long	3222049373
	.long	2944069397
	.long	133776782
	.long	3909695307
	.long	2262602168
	.long	2821146184
	.long	2149131451
	.long	1593171499
	.long	267553565
	.long	3524423319
	.long	230237041
	.long	1347325072
	.long	3295607
	.long	3186342998
	.long	535107130
	.long	2753879342
	.long	460474083
	.long	2694650145
	.long	6591214
	.long	2077718700
	.long	1070214261
	.long	1212791388
	.long	920948167
	.long	1094332995
	.long	13182429
	.long	4155437400
	.long	2140428522
	.long	2425582776
	.long	1841896334
	.long	2188665991
	.long	26364858
	.long	4015907504
	.long	4280857045
	.long	556198256
	.long	3683792669
	.long	82364686
	.long	52729717
	.long	3736847713
	.long	4266746795
	.long	1112396512
	.long	3072618042
	.long	164729372
	.long	105459434
	.long	3178728131
	.long	4238526295
	.long	2224793024
	.long	1850268788
	.long	329458745
	.long	210918868
	.long	2062488966
	.long	4182085295
	.long	154618752
	.long	3700537577
	.long	658917491
	.long	421837736
	.long	4124977933
	.long	4069203294
	.long	309237504
	.long	3106107858
	.long	1317834983
	.long	843675472
	.long	3954988571
	.long	3843439293
	.long	618475008
	.long	1917248420
	.long	2635669967
	.long	1687350944
	.long	3615009846
	.long	3391911291
	.long	1236950016
	.long	3834496840
	.long	976372639
	.long	3374701889
	.long	2935052397
	.long	2488855287
	.long	2473900033
	.long	3374026384
	.long	1952745279
	.long	2454436482
	.long	1575137499
	.long	682743279
	.long	652832771
	.long	2453085473
	.long	3905490559
	.long	613905668
	.long	3150274999
	.long	1365486558
	.long	1305665542
	.long	611203650
	.long	3516013822
	.long	1227811337
	.long	2005582702
	.long	2730973117
	.long	2611331084
	.long	1222407300
	.long	2737060348
	.long	2455622675
	.long	4011165404
	.long	1166978938
	.long	927694873
	.long	2444814601
	.long	1179153400
	.long	616278055
	.long	3727363513
	.long	2333957877
	.long	1855389746
	.long	594661906
	.long	2358306800
	.long	1232556110
	.long	3159759730
	.long	372948459
	.long	3710779492
	.long	1189323812
	.long	421646305
	.long	2465112221
	.long	2024552164
	.long	745896919
	.long	3126591689
	.long	2378647625
	.long	843292611
	.long	635257146
	.long	4049104329
	.long	1491793838
	.long	1958216082
	.long	462327955
	.long	1686585223
	.long	1270514292
	.long	3803241362
	.long	2983587677
	.long	3916432164
	.long	924655910
	.long	3373170446
	.long	2541028584
	.long	3311515428
	.long	1672208059
	.long	3537897033
	.long	1849311821
	.long	2451373597
	.long	787089873
	.long	2328063560
	.long	3344416119
	.long	2780826770
	.long	3698623643
	.long	607779899
	.long	1574179747
	.long	361159825
	.long	2393864943
	.long	1266686244
	.long	3102279991
	.long	1215559799
	.long	3148359494
	.long	722319651
	.long	492762590
	.long	2533372489
	.long	1909592686
	.long	2431119599
	.long	2001751692
	.long	1444639302
	.long	985525180
	.long	771777682
	.long	3819185373
	.long	567271902
	.long	4003503385
	.long	2889278605
	.long	1971050360
	.long	1543555365
	.long	3343403450
	.long	1134543805
	.long	3712039474
	.long	1483589915
	.long	3942100721
	.long	3087110731
	.long	2391839604
	.long	2269087610
	.long	3129111652
	.long	2967179831
	.long	3589234146
	.long	1879254167
	.long	488711913
	.long	243207925
	.long	1963256009
	.long	1639392366
	.long	2883500997
	.long	3758508334
	.long	977423826
	.long	486415851
	.long	3926512018
	.long	3278784732
	.long	1472034698
	.long	3222049373
	.long	1954847653
	.long	972831702
	.long	3558056740
	.long	2262602168
	.long	2944069397
	.long	2149131451
	.long	3909695307
	.long	1945663404
	.long	2821146184
	.long	230237041
	.long	1593171499
	.long	3295607
	.long	3524423319
	.long	3891326808
	.long	1347325072
	.long	460474083
	.long	3186342998
	.long	6591214
	.long	2753879342
	.long	3487686321
	.long	2694650145
	.long	920948167
	.long	2077718700
	.long	13182429
	.long	1212791388
	.long	2680405347
	.long	1094332995
	.long	1841896334
	.long	4155437400
	.long	26364858
	.long	2425582776
	.long	1065843399
	.long	2188665991
	.long	3683792669
	.long	4015907504
	.long	52729717
	.long	556198256
	.long	2131686798
	.long	82364686
	.long	3072618042
	.long	3736847713
	.long	105459434
	.long	1112396512
	.long	4263373596
	.long	164729372
	.long	1850268788
	.long	3178728131
	.long	210918868
	.long	2224793024
	.long	4231779897
	.long	329458745
	.long	3700537577
	.long	2062488966
	.long	421837736
	.long	154618752
	.long	4168592498
	.long	658917491
	.long	3106107858
	.long	4124977933
	.long	843675472
	.long	309237504
	.long	4042217701
	.long	1317834983
	.long	1917248420
	.long	3954988571
	.long	1687350944
	.long	618475008
	.long	3789468107
	.long	2635669967
	.long	3834496840
	.long	3615009846
	.long	3374701889
	.long	1236950016
	.long	3283968918
	.long	976372639
	.long	3374026384
	.long	2935052397
	.long	2454436482
	.long	2473900033
	.long	2272970540
	.long	1952745279
	.long	2453085473
	.long	1575137499
	.long	613905668
	.long	652832771
	.long	250973784
	.long	3905490559
	.long	611203650
	.long	3150274999
	.long	1227811337
	.long	1305665542
	.long	501947569
	.long	3516013822
	.long	1222407300
	.long	2005582702
	.long	2455622675
	.long	2611331084
	.long	1003895138
	.long	2737060348
	.long	2444814601
	.long	4011165404
	.long	616278055
	.long	927694873
	.long	2007790276
	.long	1179153400
	.long	594661906
	.long	3727363513
	.long	1232556110
	.long	1855389746
	.long	4015580553
	.long	2358306800
	.long	1189323812
	.long	3159759730
	.long	2465112221
	.long	3710779492
	.long	3736193810
	.long	421646305
	.long	2378647625
	.long	2024552164
	.long	635257146
	.long	3126591689
	.long	3177420325
	.long	843292611
	.long	462327955
	.long	4049104329
	.long	1270514292
	.long	1958216082
	.long	2059873354
	.long	1686585223
	.long	924655910
	.long	3803241362
	.long	2541028584
	.long	3916432164
	.long	4119746708
	.long	3373170446
	.long	1849311821
	.long	3311515428
	.long	787089873
	.long	3537897033
	.long	3944526121
	.long	2451373597
	.long	3698623643
	.long	2328063560
	.long	1574179747
	.long	2780826770
	.long	3594084947
	.long	607779899
	.long	3102279991
	.long	361159825
	.long	3148359494
	.long	1266686244
	.long	2893202598
	.long	1215559799
	.long	1909592686
	.long	722319651
	.long	2001751692
	.long	2533372489
	.long	1491437901
	.long	2431119599
	.long	3819185373
	.long	1444639302
	.long	4003503385
	.long	771777682
	.long	2982875802
	.long	567271902
	.long	3343403450
	.long	2889278605
	.long	3712039474
	.long	1543555365
	.long	1670784308
	.long	1134543805
	.long	2391839604
	.long	1483589915
	.long	3129111652
	.long	3087110731
	.long	3341568617
	.long	2269087610
	.long	488711913
	.long	2967179831
	.long	1963256009
	.long	1879254167
	.long	2388169939
	.long	243207925
	.long	977423826
	.long	1639392366
	.long	3926512018
	.long	3758508334
	.long	481372583
	.long	486415851
	.long	1954847653
	.long	3278784732
	.long	3558056740
	.long	3222049373
	.long	962745166
	.long	972831702
	.long	3909695307
	.long	2262602168
	.long	2821146184
	.long	2149131451
	.long	1925490332
	.long	1945663404
	.long	3524423319
	.long	230237041
	.long	1347325072
	.long	3295607
	.long	3850980665
	.long	3891326808
	.long	2753879342
	.long	460474083
	.long	2694650145
	.long	6591214
	.long	3406994035
	.long	3487686321
	.long	1212791388
	.long	920948167
	.long	1094332995
	.long	13182429
	.long	2519020775
	.long	2680405347
	.long	2425582776
	.long	1841896334
	.long	2188665991
	.long	26364858
	.long	743074255
	.long	1065843399
	.long	556198256
	.long	3683792669
	.long	82364686
	.long	52729717
	.long	1486148511
	.long	2131686798
	.long	1112396512
	.long	3072618042
	.long	164729372
	.long	105459434
	.long	2972297022
	.long	4263373596
	.long	2224793024
	.long	1850268788
	.long	329458745
	.long	210918868
	.long	1649626749
	.long	4231779897
	.long	154618752
	.long	3700537577
	.long	658917491
	.long	421837736
	.long	3299253499
	.long	4168592498
	.long	309237504
	.long	3106107858
	.long	1317834983
	.long	843675472
	.long	2303539703
	.long	4042217701
	.long	618475008
	.long	1917248420
	.long	2635669967
	.long	1687350944
	.long	312112110
	.long	3789468107
	.long	1236950016
	.long	3834496840
	.long	976372639
	.long	3374701889
	.long	624224221
	.long	3283968918
	.long	2473900033
	.long	3374026384
	.long	1952745279
	.long	2454436482
	.long	1248448442
	.long	2272970540
	.long	652832771
	.long	2453085473
	.long	3905490559
	.long	613905668
	.long	2496896884
	.long	250973784
	.long	1305665542
	.long	611203650
	.long	3516013822
	.long	1227811337
	.long	698826472
	.long	501947569
	.long	2611331084
	.long	1222407300
	.long	2737060348
	.long	2455622675
	.long	1397652945
	.long	1003895138
	.long	927694873
	.long	2444814601
	.long	1179153400
	.long	616278055
	.long	2795305890
	.long	2007790276
	.long	1855389746
	.long	594661906
	.long	2358306800
	.long	1232556110
	.long	1295644484
	.long	4015580553
	.long	3710779492
	.long	1189323812
	.long	421646305
	.long	2465112221
	.long	2591288968
	.long	3736193810
	.long	3126591689
	.long	2378647625
	.long	843292611
	.long	635257146
	.long	887610640
	.long	3177420325
	.long	1958216082
	.long	462327955
	.long	1686585223
	.long	1270514292
	.long	1775221280
	.long	2059873354
	.long	3916432164
	.long	924655910
	.long	3373170446
	.long	2541028584
	.long	3550442561
	.long	4119746708
	.long	3537897033
	.long	1849311821
	.long	2451373597
	.long	787089873
	.long	2805917826
	.long	3944526121
	.long	2780826770
	.long	3698623643
	.long	607779899
	.long	1574179747
	.long	1316868356
	.long	3594084947
	.long	1266686244
	.long	3102279991
	.long	1215559799
	.long	3148359494
	.long	2633736712
	.long	2893202598
	.long	2533372489
	.long	1909592686
	.long	2431119599
	.long	2001751692
	.long	972506129
	.long	1491437901
	.long	771777682
	.long	3819185373
	.long	567271902
	.long	4003503385
	.long	1945012259
	.long	2982875802
	.long	1543555365
	.long	3343403450
	.long	1134543805
	.long	3712039474
	.long	3890024518
	.long	1670784308
	.long	3087110731
	.long	2391839604
	.long	2269087610
	.long	3129111652
	.long	3485081741
	.long	3341568617
	.long	1879254167
	.long	488711913
	.long	243207925
	.long	1963256009
	.long	2675196186
	.long	2388169939
	.long	3758508334
	.long	977423826
	.long	486415851
	.long	3926512018
	.long	1055425077
	.long	481372583
	.long	3222049373
	.long	1954847653
	.long	972831702
	.long	3558056740
	.long	2110850155
	.long	962745166
	.long	2149131451
	.long	3909695307
	.long	1945663404
	.long	2821146184
	.long	4221700311
	.long	1925490332
	.long	3295607
	.long	3524423319
	.long	3891326808
	.long	1347325072
	.long	4148433327
	.long	3850980665
	.long	6591214
	.long	2753879342
	.long	3487686321
	.long	2694650145
	.long	4001899359
	.long	3406994035
	.long	13182429
	.long	1212791388
	.long	2680405347
	.long	1094332995
	.long	3708831422
	.long	2519020775
	.long	26364858
	.long	2425582776
	.long	1065843399
	.long	2188665991
	.long	3122695549
	.long	743074255
	.long	52729717
	.long	556198256
	.long	2131686798
	.long	82364686
	.long	1950423802
	.long	1486148511
	.long	105459434
	.long	1112396512
	.long	4263373596
	.long	164729372
	.long	3900847605
	.long	2972297022
	.long	210918868
	.long	2224793024
	.long	4231779897
	.long	329458745
	.long	3506727914
	.long	1649626749
	.long	421837736
	.long	154618752
	.long	4168592498
	.long	658917491
	.long	2718488532
	.long	3299253499
	.long	843675472
	.long	309237504
	.long	4042217701
	.long	1317834983
	.long	1142009769
	.long	2303539703
	.long	1687350944
	.long	618475008
	.long	3789468107
	.long	2635669967
	.long	2284019538
	.long	312112110
	.long	3374701889
	.long	1236950016
	.long	3283968918
	.long	976372639
	.long	273071781
	.long	624224221
	.long	2454436482
	.long	2473900033
	.long	2272970540
	.long	1952745279
	.long	546143563
	.long	1248448442
	.long	613905668
	.long	652832771
	.long	250973784
	.long	3905490559
	.long	1092287127
	.long	2496896884
	.long	1227811337
	.long	1305665542
	.long	501947569
	.long	3516013822
	.long	2184574254
	.long	698826472
	.long	2455622675
	.long	2611331084
	.long	1003895138
	.long	2737060348
	.long	74181213
	.long	1397652945
	.long	616278055
	.long	927694873
	.long	2007790276
	.long	1179153400
	.long	148362426
	.long	2795305890
	.long	1232556110
	.long	1855389746
	.long	4015580553
	.long	2358306800
	.long	296724853
	.long	1295644484
	.long	2465112221
	.long	3710779492
	.long	3736193810
	.long	421646305
	.long	593449707
	.long	2591288968
	.long	635257146
	.long	3126591689
	.long	3177420325
	.long	843292611
	.long	1186899415
	.long	887610640
	.long	1270514292
	.long	1958216082
	.long	2059873354
	.long	1686585223
	.long	2373798830
	.long	1775221280
	.long	2541028584
	.long	3916432164
	.long	4119746708
	.long	3373170446
	.long	452630365
	.long	3550442561
	.long	787089873
	.long	3537897033
	.long	3944526121
	.long	2451373597
	.long	905260731
	.long	2805917826
	.long	1574179747
	.long	2780826770
	.long	3594084947
	.long	607779899
	.long	1810521462
	.long	1316868356
	.long	3148359494
	.long	1266686244
	.long	2893202598
	.long	1215559799
	.long	3621042925
	.long	2633736712
	.long	2001751692
	.long	2533372489
	.long	1491437901
	.long	2431119599
	.long	2947118554
	.long	972506129
	.long	4003503385
	.long	771777682
	.long	2982875802
	.long	567271902
	.long	1599269812
	.long	1945012259
	.long	3712039474
	.long	1543555365
	.long	1670784308
	.long	1134543805
	.long	3198539624
	.long	3890024518
	.long	3129111652
	.long	3087110731
	.long	3341568617
	.long	2269087610
	.long	2102111953
	.long	3485081741
	.long	1963256009
	.long	1879254167
	.long	2388169939
	.long	243207925
	.long	4204223906
	.long	2675196186
	.long	3926512018
	.long	3758508334
	.long	481372583
	.long	486415851
	.long	4113480516
	.long	1055425077
	.long	3558056740
	.long	3222049373
	.long	962745166
	.long	972831702
	.long	3931993737
	.long	2110850155
	.long	2821146184
	.long	2149131451
	.long	1925490332
	.long	1945663404
	.long	3569020178
	.long	4221700311
	.long	1347325072
	.long	3295607
	.long	3850980665
	.long	3891326808
	.long	2843073060
	.long	4148433327
	.long	2694650145
	.long	6591214
	.long	3406994035
	.long	3487686321
	.long	1391178824
	.long	4001899359
	.long	1094332995
	.long	13182429
	.long	2519020775
	.long	2680405347
	.long	2782357648
	.long	3708831422
	.long	2188665991
	.long	26364858
	.long	743074255
	.long	1065843399
	.long	1269748001
	.long	3122695549
	.long	82364686
	.long	52729717
	.long	1486148511
	.long	2131686798
	.long	2539496002
	.long	1950423802
	.long	164729372
	.long	105459434
	.long	2972297022
	.long	4263373596
	.long	784024708
	.long	3900847605
	.long	329458745
	.long	210918868
	.long	1649626749
	.long	4231779897
	.long	1568049417
	.long	3506727914
	.long	658917491
	.long	421837736
	.long	3299253499
	.long	4168592498
	.long	3136098835
	.long	2718488532
	.long	1317834983
	.long	843675472
	.long	2303539703
	.long	4042217701
	.long	1977230375
	.long	1142009769
	.long	2635669967
	.long	1687350944
	.long	312112110
	.long	3789468107
	.long	3954460750
	.long	2284019538
	.long	976372639
	.long	3374701889
	.long	624224221
	.long	3283968918
	.long	3613954205
	.long	273071781
	.long	1952745279
	.long	2454436482
	.long	1248448442
	.long	2272970540
	.long	2932941114
	.long	546143563
	.long	3905490559
	.long	613905668
	.long	2496896884
	.long	250973784
	.long	1570914932
	.long	1092287127
	.long	3516013822
	.long	1227811337
	.long	698826472
	.long	501947569
	.long	3141829865
	.long	2184574254
	.long	2737060348
	.long	2455622675
	.long	1397652945
	.long	1003895138
	.long	1988692435
	.long	74181213
	.long	1179153400
	.long	616278055
	.long	2795305890
	.long	2007790276
	.long	3977384870
	.long	148362426
	.long	2358306800
	.long	1232556110
	.long	1295644484
	.long	4015580553
	.long	3659802444
	.long	296724853
	.long	421646305
	.long	2465112221
	.long	2591288968
	.long	3736193810
	.long	3024637593
	.long	593449707
	.long	843292611
	.long	635257146
	.long	887610640
	.long	3177420325
	.long	1754307891
	.long	1186899415
	.long	1686585223
	.long	1270514292
	.long	1775221280
	.long	2059873354
	.long	3508615783
	.long	2373798830
	.long	3373170446
	.long	2541028584
	.long	3550442561
	.long	4119746708
	.long	2722264270
	.long	452630365
	.long	2451373597
	.long	787089873
	.long	2805917826
	.long	3944526121
	.long	1149561244
	.long	905260731
	.long	607779899
	.long	1574179747
	.long	1316868356
	.long	3594084947
	.long	2299122488
	.long	1810521462
	.long	1215559799
	.long	3148359494
	.long	2633736712
	.long	2893202598
	.long	303277681
	.long	3621042925
	.long	2431119599
	.long	2001751692
	.long	972506129
	.long	1491437901
	.long	606555363
	.long	2947118554
	.long	567271902
	.long	4003503385
	.long	1945012259
	.long	2982875802
	.long	1213110727
	.long	1599269812
	.long	1134543805
	.long	3712039474
	.long	3890024518
	.long	1670784308
	.long	2426221454
	.long	3198539624
	.long	2269087610
	.long	3129111652
	.long	3485081741
	.long	3341568617
	.long	557475612
	.long	2102111953
	.long	243207925
	.long	1963256009
	.long	2675196186
	.long	2388169939
	.long	1114951224
	.long	4204223906
	.long	486415851
	.long	3926512018
	.long	1055425077
	.long	481372583
	.long	2229902448
	.long	4113480516
	.long	972831702
	.long	3558056740
	.long	2110850155
	.long	962745166
	.long	164837600
	.long	3931993737
	.long	1945663404
	.long	2821146184
	.long	4221700311
	.long	1925490332
	.long	329675200
	.long	3569020178
	.long	3891326808
	.long	1347325072
	.long	4148433327
	.long	3850980665
	.long	659350401
	.long	2843073060
	.long	3487686321
	.long	2694650145
	.long	4001899359
	.long	3406994035
	.long	1318700802
	.long	1391178824
	.long	2680405347
	.long	1094332995
	.long	3708831422
	.long	2519020775
	.long	2637401604
	.long	2782357648
	.long	1065843399
	.long	2188665991
	.long	3122695549
	.long	743074255
	.long	979835913
	.long	1269748001
	.long	2131686798
	.long	82364686
	.long	1950423802
	.long	1486148511
	.long	1959671827
	.long	2539496002
	.long	4263373596
	.long	164729372
	.long	3900847605
	.long	2972297022
	.long	3919343654
	.long	784024708
	.long	4231779897
	.long	329458745
	.long	3506727914
	.long	1649626749
	.long	3543720013
	.long	1568049417
	.long	4168592498
	.long	658917491
	.long	2718488532
	.long	3299253499
	.long	2792472730
	.long	3136098835
	.long	4042217701
	.long	1317834983
	.long	1142009769
	.long	2303539703
	.long	1289978165
	.long	1977230375
	.long	3789468107
	.long	2635669967
	.long	2284019538
	.long	312112110
	.long	2579956331
	.long	3954460750
	.long	3283968918
	.long	976372639
	.long	273071781
	.long	624224221
	.long	864945366
	.long	3613954205
	.long	2272970540
	.long	1952745279
	.long	546143563
	.long	1248448442
	.long	1729890733
	.long	2932941114
	.long	250973784
	.long	3905490559
	.long	1092287127
	.long	2496896884
	.long	3459781466
	.long	1570914932
	.long	501947569
	.long	3516013822
	.long	2184574254
	.long	698826472
	.long	2624595636
	.long	3141829865
	.long	1003895138
	.long	2737060348
	.long	74181213
	.long	1397652945
	.long	954223976
	.long	1988692435
	.long	2007790276
	.long	1179153400
	.long	148362426
	.long	2795305890
	.long	1908447953
	.long	3977384870
	.long	4015580553
	.long	2358306800
	.long	296724853
	.long	1295644484
	.long	3816895906
	.long	3659802444
	.long	3736193810
	.long	421646305
	.long	593449707
	.long	2591288968
	.long	3338824517
	.long	3024637593
	.long	3177420325
	.long	843292611
	.long	1186899415
	.long	887610640
	.long	2382681739
	.long	1754307891
	.long	2059873354
	.long	1686585223
	.long	2373798830
	.long	1775221280
	.long	470396183
	.long	3508615783
	.long	4119746708
	.long	3373170446
	.long	452630365
	.long	3550442561
	.long	940792367
	.long	2722264270
	.long	3944526121
	.long	2451373597
	.long	905260731
	.long	2805917826
	.long	1881584735
	.long	1149561244
	.long	3594084947
	.long	607779899
	.long	1810521462
	.long	1316868356
	.long	3763169470
	.long	2299122488
	.long	2893202598
	.long	1215559799
	.long	3621042925
	.long	2633736712
	.long	3231371645
	.long	303277681
	.long	1491437901
	.long	2431119599
	.long	2947118554
	.long	972506129
	.long	2167775995
	.long	606555363
	.long	2982875802
	.long	567271902
	.long	1599269812
	.long	1945012259
	.long	40584695
	.long	1213110727
	.long	1670784308
	.long	1134543805
	.long	3198539624
	.long	3890024518
	.long	81169391
	.long	2426221454
	.long	3341568617
	.long	2269087610
	.long	2102111953
	.long	3485081741
	.long	162338783
	.long	557475612
	.long	2388169939
	.long	243207925
	.long	4204223906
	.long	2675196186
	.long	324677567
	.long	1114951224
	.long	481372583
	.long	486415851
	.long	4113480516
	.long	1055425077
	.long	649355134
	.long	2229902448
	.long	962745166
	.long	972831702
	.long	3931993737
	.long	2110850155
	.long	1298710268
	.long	164837600
	.long	1925490332
	.long	1945663404
	.long	3569020178
	.long	4221700311
	.long	2597420537
	.long	329675200
	.long	3850980665
	.long	3891326808
	.long	2843073060
	.long	4148433327
	.long	899873778
	.long	659350401
	.long	3406994035
	.long	3487686321
	.long	1391178824
	.long	4001899359
	.long	1799747556
	.long	1318700802
	.long	2519020775
	.long	2680405347
	.long	2782357648
	.long	3708831422
	.long	3599495112
	.long	2637401604
	.long	743074255
	.long	1065843399
	.long	1269748001
	.long	3122695549
	.long	2904022928
	.long	979835913
	.long	1486148511
	.long	2131686798
	.long	2539496002
	.long	1950423802
	.long	1513078560
	.long	1959671827
	.long	2972297022
	.long	4263373596
	.long	784024708
	.long	3900847605
	.long	3026157121
	.long	3919343654
	.long	1649626749
	.long	4231779897
	.long	1568049417
	.long	3506727914
	.long	1757346946
	.long	3543720013
	.long	3299253499
	.long	4168592498
	.long	3136098835
	.long	2718488532
	.long	3514693892
	.long	2792472730
	.long	2303539703
	.long	4042217701
	.long	1977230375
	.long	1142009769
	.long	2734420489
	.long	1289978165
	.long	312112110
	.long	3789468107
	.long	3954460750
	.long	2284019538
	.long	1173873683
	.long	2579956331
	.long	624224221
	.long	3283968918
	.long	3613954205
	.long	273071781
	.long	2347747367
	.long	864945366
	.long	1248448442
	.long	2272970540
	.long	2932941114
	.long	546143563
	.long	400527438
	.long	1729890733
	.long	2496896884
	.long	250973784
	.long	1570914932
	.long	1092287127
	.long	801054876
	.long	3459781466
	.long	698826472
	.long	501947569
	.long	3141829865
	.long	2184574254
	.long	1602109753
	.long	2624595636
	.long	1397652945
	.long	1003895138
	.long	1988692435
	.long	74181213
	.long	3204219507
	.long	954223976
	.long	2795305890
	.long	2007790276
	.long	3977384870
	.long	148362426
	.long	2113471718
	.long	1908447953
	.long	1295644484
	.long	4015580553
	.long	3659802444
	.long	296724853
	.long	4226943436
	.long	3816895906
	.long	2591288968
	.long	3736193810
	.long	3024637593
	.long	593449707
	.long	4158919577
	.long	3338824517
	.long	887610640
	.long	3177420325
	.long	1754307891
	.long	1186899415
	.long	4022871858
	.long	2382681739
	.long	1775221280
	.long	2059873354
	.long	3508615783
	.long	2373798830
	.long	3750776420
	.long	470396183
	.long	3550442561
	.long	4119746708
	.long	2722264270
	.long	452630365
	.long	3206585544
	.long	940792367
	.long	2805917826
	.long	3944526121
	.long	1149561244
	.long	905260731
	.long	2118203793
	.long	1881584735
	.long	1316868356
	.long	3594084947
	.long	2299122488
	.long	1810521462
	.long	4236407587
	.long	3763169470
	.long	2633736712
	.long	2893202598
	.long	303277681
	.long	3621042925
	.long	4177847879
	.long	3231371645
	.long	972506129
	.long	1491437901
	.long	606555363
	.long	2947118554
	.long	4060728462
	.long	2167775995
	.long	1945012259
	.long	2982875802
	.long	1213110727
	.long	1599269812
	.long	3826489629
	.long	40584695
	.long	3890024518
	.long	1670784308
	.long	2426221454
	.long	3198539624
	.long	3358011962
	.long	81169391
	.long	3485081741
	.long	3341568617
	.long	557475612
	.long	2102111953
	.long	2421056629
	.long	162338783
	.long	2675196186
	.long	2388169939
	.long	1114951224
	.long	4204223906
	.long	547145963
	.long	324677567
	.long	1055425077
	.long	481372583
	.long	2229902448
	.long	4113480516
	.long	1094291926
	.long	649355134
	.long	2110850155
	.long	962745166
	.long	164837600
	.long	3931993737
	.long	2188583852
	.long	1298710268
	.long	4221700311
	.long	1925490332
	.long	329675200
	.long	3569020178
	.long	82200408
	.long	2597420537
	.long	4148433327
	.long	3850980665
	.long	659350401
	.long	2843073060
	.long	164400817
	.long	899873778
	.long	4001899359
	.long	3406994035
	.long	1318700802
	.long	1391178824
	.long	328801635
	.long	1799747556
	.long	3708831422
	.long	2519020775
	.long	2637401604
	.long	2782357648
	.long	657603271
	.long	3599495112
	.long	3122695549
	.long	743074255
	.long	979835913
	.long	1269748001
	.long	1315206542
	.long	2904022928
	.long	1950423802
	.long	1486148511
	.long	1959671827
	.long	2539496002
	.long	2630413084
	.long	1513078560
	.long	3900847605
	.long	2972297022
	.long	3919343654
	.long	784024708
	.long	965858873
	.long	3026157121
	.long	3506727914
	.long	1649626749
	.long	3543720013
	.long	1568049417
	.long	1931717747
	.long	1757346946
	.long	2718488532
	.long	3299253499
	.long	2792472730
	.long	3136098835
	.long	3863435494
	.long	3514693892
	.long	1142009769
	.long	2303539703
	.long	1289978165
	.long	1977230375
	.long	3431903692
	.long	2734420489
	.long	2284019538
	.long	312112110
	.long	2579956331
	.long	3954460750
	.long	2568840088
	.long	1173873683
	.long	273071781
	.long	624224221
	.long	864945366
	.long	3613954205
	.long	842712880
	.long	2347747367
	.long	546143563
	.long	1248448442
	.long	1729890733
	.long	2932941114
	.long	1685425760
	.long	400527438
	.long	1092287127
	.long	2496896884
	.long	3459781466
	.long	1570914932
	.long	3370851521
	.long	801054876
	.long	2184574254
	.long	698826472
	.long	2624595636
	.long	3141829865
	.long	2446735747
	.long	1602109753
	.long	74181213
	.long	1397652945
	.long	954223976
	.long	1988692435
	.long	598504198
	.long	3204219507
	.long	148362426
	.long	2795305890
	.long	1908447953
	.long	3977384870
	.long	1197008397
	.long	2113471718
	.long	296724853
	.long	1295644484
	.long	3816895906
	.long	3659802444
	.long	2394016794
	.long	4226943436
	.long	593449707
	.long	2591288968
	.long	3338824517
	.long	3024637593
	.long	493066293
	.long	4158919577
	.long	1186899415
	.long	887610640
	.long	2382681739
	.long	1754307891
	.long	986132586
	.long	4022871858
	.long	2373798830
	.long	1775221280
	.long	470396183
	.long	3508615783
	.long	1972265172
	.long	3750776420
	.long	452630365
	.long	3550442561
	.long	940792367
	.long	2722264270
	.long	3944530345
	.long	3206585544
	.long	905260731
	.long	2805917826
	.long	1881584735
	.long	1149561244
	.long	3594093395
	.long	2118203793
	.long	1810521462
	.long	1316868356
	.long	3763169470
	.long	2299122488
	.long	2893219494
	.long	4236407587
	.long	3621042925
	.long	2633736712
	.long	3231371645
	.long	303277681
	.long	1491471692
	.long	4177847879
	.long	2947118554
	.long	972506129
	.long	2167775995
	.long	606555363
	.long	2982943385
	.long	4060728462
	.long	1599269812
	.long	1945012259
	.long	40584695
	.long	1213110727
	.long	1670919475
	.long	3826489629
	.long	3198539624
	.long	3890024518
	.long	81169391
	.long	2426221454
	.long	3341838951
	.long	3358011962
	.long	2102111953
	.long	3485081741
	.long	162338783
	.long	557475612
	.long	2388710606
	.long	2421056629
	.long	4204223906
	.long	2675196186
	.long	324677567
	.long	1114951224
	.long	482453916
	.long	547145963
	.long	4113480516
	.long	1055425077
	.long	649355134
	.long	2229902448
	.long	964907833
	.long	1094291926
	.long	3931993737
	.long	2110850155
	.long	1298710268
	.long	164837600
	.long	1929815667
	.long	2188583852
	.long	3569020178
	.long	4221700311
	.long	2597420537
	.long	329675200
	.long	3859631335
	.long	82200408
	.long	2843073060
	.long	4148433327
	.long	899873778
	.long	659350401
	.long	3424295375
	.long	164400817
	.long	1391178824
	.long	4001899359
	.long	1799747556
	.long	1318700802
	.long	2553623455
	.long	328801635
	.long	2782357648
	.long	3708831422
	.long	3599495112
	.long	2637401604
	.long	812279614
	.long	657603271
	.long	1269748001
	.long	3122695549
	.long	2904022928
	.long	979835913
	.long	1624559229
	.long	1315206542
	.long	2539496002
	.long	1950423802
	.long	1513078560
	.long	1959671827
	.long	3249118458
	.long	2630413084
	.long	784024708
	.long	3900847605
	.long	3026157121
	.long	3919343654
	.long	2203269620
	.long	965858873
	.long	1568049417
	.long	3506727914
	.long	1757346946
	.long	3543720013
	.long	111571945
	.long	1931717747
	.long	3136098835
	.long	2718488532
	.long	3514693892
	.long	2792472730
	.long	223143890
	.long	3863435494
	.long	1977230375
	.long	1142009769
	.long	2734420489
	.long	1289978165
	.long	446287780
	.long	3431903692
	.long	3954460750
	.long	2284019538
	.long	1173873683
	.long	2579956331
	.long	892575561
	.long	2568840088
	.long	3613954205
	.long	273071781
	.long	2347747367
	.long	864945366
	.long	1785151123
	.long	842712880
	.long	2932941114
	.long	546143563
	.long	400527438
	.long	1729890733
	.long	3570302247
	.long	1685425760
	.long	1570914932
	.long	1092287127
	.long	801054876
	.long	3459781466
	.long	2845637198
	.long	3370851521
	.long	3141829865
	.long	2184574254
	.long	1602109753
	.long	2624595636
	.long	1396307100
	.long	2446735747
	.long	1988692435
	.long	74181213
	.long	3204219507
	.long	954223976
	.long	2792614201
	.long	598504198
	.long	3977384870
	.long	148362426
	.long	2113471718
	.long	1908447953
	.long	1290261106
	.long	1197008397
	.long	3659802444
	.long	296724853
	.long	4226943436
	.long	3816895906
	.long	2580522212
	.long	2394016794
	.long	3024637593
	.long	593449707
	.long	4158919577
	.long	3338824517
	.long	866077128
	.long	493066293
	.long	1754307891
	.long	1186899415
	.long	4022871858
	.long	2382681739
	.long	1732154256
	.long	986132586
	.long	3508615783
	.long	2373798830
	.long	3750776420
	.long	470396183
	.long	3464308513
	.long	1972265172
	.long	2722264270
	.long	452630365
	.long	3206585544
	.long	940792367
	.long	2633649730
	.long	3944530345
	.long	1149561244
	.long	905260731
	.long	2118203793
	.long	1881584735
	.long	972332164
	.long	3594093395
	.long	2299122488
	.long	1810521462
	.long	4236407587
	.long	3763169470
	.long	1944664328
	.long	2893219494
	.long	303277681
	.long	3621042925
	.long	4177847879
	.long	3231371645
	.long	3889328657
	.long	1491471692
	.long	606555363
	.long	2947118554
	.long	4060728462
	.long	2167775995
	.long	3483690018
	.long	2982943385
	.long	1213110727
	.long	1599269812
	.long	3826489629
	.long	40584695
	.long	2672412741
	.long	1670919475
	.long	2426221454
	.long	3198539624
	.long	3358011962
	.long	81169391
	.long	1049858187
	.long	3341838951
	.long	557475612
	.long	2102111953
	.long	2421056629
	.long	162338783
	.long	2099716375
	.long	2388710606
	.long	1114951224
	.long	4204223906
	.long	547145963
	.long	324677567
	.long	4199432751
	.long	482453916
	.long	2229902448
	.long	4113480516
	.long	1094291926
	.long	649355134
	.long	4103898207
	.long	964907833
	.long	164837600
	.long	3931993737
	.long	2188583852
	.long	1298710268
	.long	3912829119
	.long	1929815667
	.long	329675200
	.long	3569020178
	.long	82200408
	.long	2597420537
	.long	3530690942
	.long	3859631335
	.long	659350401
	.long	2843073060
	.long	164400817
	.long	899873778
	.long	2766414588
	.long	3424295375
	.long	1318700802
	.long	1391178824
	.long	328801635
	.long	1799747556
	.long	1237861880
	.long	2553623455
	.long	2637401604
	.long	2782357648
	.long	657603271
	.long	3599495112
	.long	2475723761
	.long	812279614
	.long	979835913
	.long	1269748001
	.long	1315206542
	.long	2904022928
	.long	656480226
	.long	1624559229
	.long	1959671827
	.long	2539496002
	.long	2630413084
	.long	1513078560
	.long	1312960453
	.long	3249118458
	.long	3919343654
	.long	784024708
	.long	965858873
	.long	3026157121
	.long	2625920907
	.long	2203269620
	.long	3543720013
	.long	1568049417
	.long	1931717747
	.long	1757346946
	.long	956874519
	.long	111571945
	.long	2792472730
	.long	3136098835
	.long	3863435494
	.long	3514693892
	.long	1913749038
	.long	223143890
	.long	1289978165
	.long	1977230375
	.long	3431903692
	.long	2734420489
	.long	3827498077
	.long	446287780
	.long	2579956331
	.long	3954460750
	.long	2568840088
	.long	1173873683
	.long	3360028859
	.long	892575561
	.long	864945366
	.long	3613954205
	.long	842712880
	.long	2347747367
	.long	2425090423
	.long	1785151123
	.long	1729890733
	.long	2932941114
	.long	1685425760
	.long	400527438
	.long	555213551
	.long	3570302247
	.long	3459781466
	.long	1570914932
	.long	3370851521
	.long	801054876
	.long	1110427102
	.long	2845637198
	.long	2624595636
	.long	3141829865
	.long	2446735747
	.long	1602109753
	.long	2220854205
	.long	1396307100
	.long	954223976
	.long	1988692435
	.long	598504198
	.long	3204219507
	.long	146741115
	.long	2792614201
	.long	1908447953
	.long	3977384870
	.long	1197008397
	.long	2113471718
	.long	293482231
	.long	1290261106
	.long	3816895906
	.long	3659802444
	.long	2394016794
	.long	4226943436
	.long	586964463
	.long	2580522212
	.long	3338824517
	.long	3024637593
	.long	493066293
	.long	4158919577
	.long	1173928927
	.long	866077128
	.long	2382681739
	.long	1754307891
	.long	986132586
	.long	4022871858
	.long	2347857855
	.long	1732154256
	.long	470396183
	.long	3508615783
	.long	1972265172
	.long	3750776420
	.long	400748414
	.long	3464308513
	.long	940792367
	.long	2722264270
	.long	3944530345
	.long	3206585544
	.long	801496828
	.long	2633649730
	.long	1881584735
	.long	1149561244
	.long	3594093395
	.long	2118203793
	.long	1602993657
	.long	972332164
	.long	3763169470
	.long	2299122488
	.long	2893219494
	.long	4236407587
	.long	3205987314
	.long	1944664328
	.long	3231371645
	.long	303277681
	.long	1491471692
	.long	4177847879
	.long	2117007332
	.long	3889328657
	.long	2167775995
	.long	606555363
	.long	2982943385
	.long	4060728462
	.long	4234014665
	.long	3483690018
	.long	40584695
	.long	1213110727
	.long	1670919475
	.long	3826489629
	.long	4173062034
	.long	2672412741
	.long	81169391
	.long	2426221454
	.long	3341838951
	.long	3358011962
	.long	4051156773
	.long	1049858187
	.long	162338783
	.long	557475612
	.long	2388710606
	.long	2421056629
	.long	3807346250
	.long	2099716375
	.long	324677567
	.long	1114951224
	.long	482453916
	.long	547145963
	.long	3319725204
	.long	4199432751
	.long	649355134
	.long	2229902448
	.long	964907833
	.long	1094291926
	.long	2344483112
	.long	4103898207
	.long	1298710268
	.long	164837600
	.long	1929815667
	.long	2188583852
	.long	393998928
	.long	3912829119
	.long	2597420537
	.long	329675200
	.long	3859631335
	.long	82200408
	.long	787997856
	.long	3530690942
	.long	899873778
	.long	659350401
	.long	3424295375
	.long	164400817
	.long	1575995713
	.long	2766414588
	.long	1799747556
	.long	1318700802
	.long	2553623455
	.long	328801635
	.long	3151991427
	.long	1237861880
	.long	3599495112
	.long	2637401604
	.long	812279614
	.long	657603271
	.long	2009015559
	.long	2475723761
	.long	2904022928
	.long	979835913
	.long	1624559229
	.long	1315206542
	.long	4018031118
	.long	656480226
	.long	1513078560
	.long	1959671827
	.long	3249118458
	.long	2630413084
	.long	3741094941
	.long	1312960453
	.long	3026157121
	.long	3919343654
	.long	2203269620
	.long	965858873
	.long	3187222587
	.long	2625920907
	.long	1757346946
	.long	3543720013
	.long	111571945
	.long	1931717747
	.long	2079477878
	.long	956874519
	.long	3514693892
	.long	2792472730
	.long	223143890
	.long	3863435494
	.long	4158955756
	.long	1913749038
	.long	2734420489
	.long	1289978165
	.long	446287780
	.long	3431903692
	.long	4022944216
	.long	3827498077
	.long	1173873683
	.long	2579956331
	.long	892575561
	.long	2568840088
	.long	3750921137
	.long	3360028859
	.long	2347747367
	.long	864945366
	.long	1785151123
	.long	842712880
	.long	3206874979
	.long	2425090423
	.long	400527438
	.long	1729890733
	.long	3570302247
	.long	1685425760
	.long	2118782663
	.long	555213551
	.long	801054876
	.long	3459781466
	.long	2845637198
	.long	3370851521
	.long	4237565327
	.long	1110427102
	.long	1602109753
	.long	2624595636
	.long	1396307100
	.long	2446735747
	.long	4180163359
	.long	2220854205
	.long	3204219507
	.long	954223976
	.long	2792614201
	.long	598504198
	.long	4065359423
	.long	146741115
	.long	2113471718
	.long	1908447953
	.long	1290261106
	.long	1197008397
	.long	3835751551
	.long	293482231
	.long	4226943436
	.long	3816895906
	.long	2580522212
	.long	2394016794
	.long	3376535807
	.long	586964463
	.long	4158919577
	.long	3338824517
	.long	866077128
	.long	493066293
	.long	2458104319
	.long	1173928927
	.long	4022871858
	.long	2382681739
	.long	1732154256
	.long	986132586
	.long	621241343
	.long	2347857855
	.long	3750776420
	.long	470396183
	.long	3464308513
	.long	1972265172
	.long	1242482686
	.long	400748414
	.long	3206585544
	.long	940792367
	.long	2633649730
	.long	3944530345
	.long	2484965372
	.long	801496828
	.long	2118203793
	.long	1881584735
	.long	972332164
	.long	3594093395
	.long	674963448
	.long	1602993657
	.long	4236407587
	.long	3763169470
	.long	1944664328
	.long	2893219494
	.long	1349926897
	.long	3205987314
	.long	4177847879
	.long	3231371645
	.long	3889328657
	.long	1491471692
	.long	2699853794
	.long	2117007332
	.long	4060728462
	.long	2167775995
	.long	3483690018
	.long	2982943385
	.long	1104740292
	.long	4234014665
	.long	3826489629
	.long	40584695
	.long	2672412741
	.long	1670919475
	.long	2209480585
	.long	4173062034
	.long	3358011962
	.long	81169391
	.long	1049858187
	.long	3341838951
	.long	123993874
	.long	4051156773
	.long	2421056629
	.long	162338783
	.long	2099716375
	.long	2388710606
	.long	247987749
	.long	3807346250
	.long	547145963
	.long	324677567
	.long	4199432751
	.long	482453916
	.long	495975499
	.long	3319725204
	.long	1094291926
	.long	649355134
	.long	4103898207
	.long	964907833
	.long	991950999
	.long	2344483112
	.long	2188583852
	.long	1298710268
	.long	3912829119
	.long	1929815667
	.long	1983901999
	.long	393998928
	.long	82200408
	.long	2597420537
	.long	3530690942
	.long	3859631335
	.long	3967803999
	.long	787997856
	.long	164400817
	.long	899873778
	.long	2766414588
	.long	3424295375
	.long	3640640703
	.long	1575995713
	.long	328801635
	.long	1799747556
	.long	1237861880
	.long	2553623455
	.long	2986314111
	.long	3151991427
	.long	657603271
	.long	3599495112
	.long	2475723761
	.long	812279614
	.long	1677660927
	.long	2009015559
	.long	1315206542
	.long	2904022928
	.long	656480226
	.long	1624559229
	.long	3355321855
	.long	4018031118
	.long	2630413084
	.long	1513078560
	.long	1312960453
	.long	3249118458
	.long	2415676415
	.long	3741094941
	.long	965858873
	.long	3026157121
	.long	2625920907
	.long	2203269620
	.long	536385535
	.long	3187222587
	.long	1931717747
	.long	1757346946
	.long	956874519
	.long	111571945
	.long	1072771071
	.long	2079477878
	.long	3863435494
	.long	3514693892
	.long	1913749038
	.long	223143890
	.long	2145542143
	.long	4158955756
	.long	3431903692
	.long	2734420489
	.long	3827498077
	.long	446287780
	.long	4291084286
	.long	4022944216
	.long	2568840088
	.long	1173873683
	.long	3360028859
	.long	892575561
	.long	4287201277
	.long	3750921137
	.long	842712880
	.long	2347747367
	.long	2425090423
	.long	1785151123
	.long	4279435259
	.long	3206874979
	.long	1685425760
	.long	400527438
	.long	555213551
	.long	3570302247
	.long	4263903223
	.long	2118782663
	.long	3370851521
	.long	801054876
	.long	1110427102
	.long	2845637198
	.long	4232839151
	.long	4237565327
	.long	2446735747
	.long	1602109753
	.long	2220854205
	.long	1396307100
	.long	4170711006
	.long	4180163359
	.long	598504198
	.long	3204219507
	.long	146741115
	.long	2792614201
	.long	4046454716
	.long	4065359423
	.long	1197008397
	.long	2113471718
	.long	293482231
	.long	1290261106
	.long	3797942136
	.long	3835751551
	.long	2394016794
	.long	4226943436
	.long	586964463
	.long	2580522212
	.long	3300916976
	.long	3376535807
	.long	493066293
	.long	4158919577
	.long	1173928927
	.long	866077128
	.long	2306866656
	.long	2458104319
	.long	986132586
	.long	4022871858
	.long	2347857855
	.long	1732154256
	.long	318766016
	.long	621241343
	.long	1972265172
	.long	3750776420
	.long	400748414
	.long	3464308513
	.long	637532033
	.long	1242482686
	.long	3944530345
	.long	3206585544
	.long	801496828
	.long	2633649730
	.long	1275064066
	.long	2484965372
	.long	3594093395
	.long	2118203793
	.long	1602993657
	.long	972332164
	.long	2550128133
	.long	674963448
	.long	2893219494
	.long	4236407587
	.long	3205987314
	.long	1944664328
	.long	805288971
	.long	1349926897
	.long	1491471692
	.long	4177847879
	.long	2117007332
	.long	3889328657
	.long	1610577942
	.long	2699853794
	.long	2982943385
	.long	4060728462
	.long	4234014665
	.long	3483690018
	.long	3221155884
	.long	1104740292
	.long	1670919475
	.long	3826489629
	.long	4173062034
	.long	2672412741
	.long	2147344473
	.long	2209480585
	.long	3341838951
	.long	3358011962
	.long	4051156773
	.long	1049858187
	.long	4294688947
	.long	123993874
	.long	2388710606
	.long	2421056629
	.long	3807346250
	.long	2099716375
	.long	4294410598
	.long	247987749
	.long	482453916
	.long	547145963
	.long	3319725204
	.long	4199432751
	.long	4293853900
	.long	495975499
	.long	964907833
	.long	1094291926
	.long	2344483112
	.long	4103898207
	.long	4292740504
	.long	991950999
	.long	1929815667
	.long	2188583852
	.long	393998928
	.long	3912829119
	.long	4290513712
	.long	1983901999
	.long	3859631335
	.long	82200408
	.long	787997856
	.long	3530690942
	.long	4286060128
	.long	3967803999
	.long	3424295375
	.long	164400817
	.long	1575995713
	.long	2766414588
	.long	4277152960
	.long	3640640703
	.long	2553623455
	.long	328801635
	.long	3151991427
	.long	1237861880
	.long	4259338624
	.long	2986314111
	.long	812279614
	.long	657603271
	.long	2009015559
	.long	2475723761
	.long	4223709953
	.long	1677660927
	.long	1624559229
	.long	1315206542
	.long	4018031118
	.long	656480226
	.long	4152452611
	.long	3355321855
	.long	3249118458
	.long	2630413084
	.long	3741094941
	.long	1312960453
	.long	4009937927
	.long	2415676415
	.long	2203269620
	.long	965858873
	.long	3187222587
	.long	2625920907
	.long	3724908559
	.long	536385535
	.long	111571945
	.long	1931717747
	.long	2079477878
	.long	956874519
	.long	3154849823
	.long	1072771071
	.long	223143890
	.long	3863435494
	.long	4158955756
	.long	1913749038
	.long	2014732351
	.long	2145542143
	.long	446287780
	.long	3431903692
	.long	4022944216
	.long	3827498077
	.long	4029464703
	.long	4291084286
	.long	892575561
	.long	2568840088
	.long	3750921137
	.long	3360028859
	.long	3763962110
	.long	4287201277
	.long	1785151123
	.long	842712880
	.long	3206874979
	.long	2425090423
	.long	3232956925
	.long	4279435259
	.long	3570302247
	.long	1685425760
	.long	2118782663
	.long	555213551
	.long	2170946555
	.long	4263903223
	.long	2845637198
	.long	3370851521
	.long	4237565327
	.long	1110427102
	.long	46925815
	.long	4232839151
	.long	1396307100
	.long	2446735747
	.long	4180163359
	.long	2220854205
	.long	93851631
	.long	4170711006
	.long	2792614201
	.long	598504198
	.long	4065359423
	.long	146741115
	.long	187703262
	.long	4046454716
	.long	1290261106
	.long	1197008397
	.long	3835751551
	.long	293482231
	.long	375406524
	.long	3797942136
	.long	2580522212
	.long	2394016794
	.long	3376535807
	.long	586964463
	.long	750813049
	.long	3300916976
	.long	866077128
	.long	493066293
	.long	2458104319
	.long	1173928927
	.long	1501626098
	.long	2306866656
	.long	1732154256
	.long	986132586
	.long	621241343
	.long	2347857855
	.long	3003252197
	.long	318766016
	.long	3464308513
	.long	1972265172
	.long	1242482686
	.long	400748414
	.long	1711537099
	.long	637532033
	.long	2633649730
	.long	3944530345
	.long	2484965372
	.long	801496828
	.long	3423074199
	.long	1275064066
	.long	972332164
	.long	3594093395
	.long	674963448
	.long	1602993657
	.long	2551181103
	.long	2550128133
	.long	1944664328
	.long	2893219494
	.long	1349926897
	.long	3205987314
	.long	807394910
	.long	805288971
	.long	3889328657
	.long	1491471692
	.long	2699853794
	.long	2117007332
	.long	1614789820
	.long	1610577942
	.long	3483690018
	.long	2982943385
	.long	1104740292
	.long	4234014665
	.long	3229579640
	.long	3221155884
	.long	2672412741
	.long	1670919475
	.long	2209480585
	.long	4173062034
	.long	2164191985
	.long	2147344473
	.long	1049858187
	.long	3341838951
	.long	123993874
	.long	4051156773
	.long	33416674
	.long	4294688947
	.long	2099716375
	.long	2388710606
	.long	247987749
	.long	3807346250
	.long	66833348
	.long	4294410598
	.long	4199432751
	.long	482453916
	.long	495975499
	.long	3319725204
	.long	133666696
	.long	4293853900
	.long	4103898207
	.long	964907833
	.long	991950999
	.long	2344483112
	.long	267333393
	.long	4292740504
	.long	3912829119
	.long	1929815667
	.long	1983901999
	.long	393998928
	.long	534666787
	.long	4290513712
	.long	3530690942
	.long	3859631335
	.long	3967803999
	.long	787997856
	.long	1069333574
	.long	4286060128
	.long	2766414588
	.long	3424295375
	.long	3640640703
	.long	1575995713
	.long	2138667148
	.long	4277152960
	.long	1237861880
	.long	2553623455
	.long	2986314111
	.long	3151991427
	.long	4277334296
	.long	4259338624
	.long	2475723761
	.long	812279614
	.long	1677660927
	.long	2009015559
	.long	4259701297
	.long	4223709953
	.long	656480226
	.long	1624559229
	.long	3355321855
	.long	4018031118
	.long	4224435298
	.long	4152452611
	.long	1312960453
	.long	3249118458
	.long	2415676415
	.long	3741094941
	.long	4153903301
	.long	4009937927
	.long	2625920907
	.long	2203269620
	.long	536385535
	.long	3187222587
	.long	4012839307
	.long	3724908559
	.long	956874519
	.long	111571945
	.long	1072771071
	.long	2079477878
	.long	3730711318
	.long	3154849823
	.long	1913749038
	.long	223143890
	.long	2145542143
	.long	4158955756
	.long	3166455341
	.long	2014732351
	.long	3827498077
	.long	446287780
	.long	4291084286
	.long	4022944216
	.long	2037943386
	.long	4029464703
	.long	3360028859
	.long	892575561
	.long	4287201277
	.long	3750921137
	.long	4075886773
	.long	3763962110
	.long	2425090423
	.long	1785151123
	.long	4279435259
	.long	3206874979
	.long	3856806251
	.long	3232956925
	.long	555213551
	.long	3570302247
	.long	4263903223
	.long	2118782663
	.long	3418645206
	.long	2170946555
	.long	1110427102
	.long	2845637198
	.long	4232839151
	.long	4237565327
	.long	2542323117
	.long	46925815
	.long	2220854205
	.long	1396307100
	.long	4170711006
	.long	4180163359
	.long	789678938
	.long	93851631
	.long	146741115
	.long	2792614201
	.long	4046454716
	.long	4065359423
	.long	1579357876
	.long	187703262
	.long	293482231
	.long	1290261106
	.long	3797942136
	.long	3835751551
	.long	3158715752
	.long	375406524
	.long	586964463
	.long	2580522212
	.long	3300916976
	.long	3376535807
	.long	2022464208
	.long	750813049
	.long	1173928927
	.long	866077128
	.long	2306866656
	.long	2458104319
	.long	4044928416
	.long	1501626098
	.long	2347857855
	.long	1732154256
	.long	318766016
	.long	621241343
	.long	3794889537
	.long	3003252197
	.long	400748414
	.long	3464308513
	.long	637532033
	.long	1242482686
	.long	3294811778
	.long	1711537099
	.long	801496828
	.long	2633649730
	.long	1275064066
	.long	2484965372
	.long	2294656261
	.long	3423074199
	.long	1602993657
	.long	972332164
	.long	2550128133
	.long	674963448
	.long	294345226
	.long	2551181103
	.long	3205987314
	.long	1944664328
	.long	805288971
	.long	1349926897
	.long	588690452
	.long	807394910
	.long	2117007332
	.long	3889328657
	.long	1610577942
	.long	2699853794
	.long	1177380905
	.long	1614789820
	.long	4234014665
	.long	3483690018
	.long	3221155884
	.long	1104740292
	.long	2354761811
	.long	3229579640
	.long	4173062034
	.long	2672412741
	.long	2147344473
	.long	2209480585
	.long	414556326
	.long	2164191985
	.long	4051156773
	.long	1049858187
	.long	4294688947
	.long	123993874
	.long	829112653
	.long	33416674
	.long	3807346250
	.long	2099716375
	.long	4294410598
	.long	247987749
	.long	1658225307
	.long	66833348
	.long	3319725204
	.long	4199432751
	.long	4293853900
	.long	495975499
	.long	3316450614
	.long	133666696
	.long	2344483112
	.long	4103898207
	.long	4292740504
	.long	991950999
	.long	2337933933
	.long	267333393
	.long	393998928
	.long	3912829119
	.long	4290513712
	.long	1983901999
	.long	380900570
	.long	534666787
	.long	787997856
	.long	3530690942
	.long	4286060128
	.long	3967803999
	.long	761801140
	.long	1069333574
	.long	1575995713
	.long	2766414588
	.long	4277152960
	.long	3640640703
	.long	1523602280
	.long	2138667148
	.long	3151991427
	.long	1237861880
	.long	4259338624
	.long	2986314111
	.long	3047204561
	.long	4277334296
	.long	2009015559
	.long	2475723761
	.long	4223709953
	.long	1677660927
	.long	1799441827
	.long	4259701297
	.long	4018031118
	.long	656480226
	.long	4152452611
	.long	3355321855
	.long	3598883655
	.long	4224435298
	.long	3741094941
	.long	1312960453
	.long	4009937927
	.long	2415676415
	.long	2902800015
	.long	4153903301
	.long	3187222587
	.long	2625920907
	.long	3724908559
	.long	536385535
	.long	1510632735
	.long	4012839307
	.long	2079477878
	.long	956874519
	.long	3154849823
	.long	1072771071
	.long	3021265470
	.long	3730711318
	.long	4158955756
	.long	1913749038
	.long	2014732351
	.long	2145542143
	.long	1747563645
	.long	3166455341
	.long	4022944216
	.long	3827498077
	.long	4029464703
	.long	4291084286
	.long	3495127291
	.long	2037943386
	.long	3750921137
	.long	3360028859
	.long	3763962110
	.long	4287201277
	.long	2695287286
	.long	4075886773
	.long	3206874979
	.long	2425090423
	.long	3232956925
	.long	4279435259
	.long	1095607277
	.long	3856806251
	.long	2118782663
	.long	555213551
	.long	2170946555
	.long	4263903223
	.long	2191214555
	.long	3418645206
	.long	4237565327
	.long	1110427102
	.long	46925815
	.long	4232839151
	.long	87461814
	.long	2542323117
	.long	4180163359
	.long	2220854205
	.long	93851631
	.long	4170711006
	.long	174923629
	.long	789678938
	.long	4065359423
	.long	146741115
	.long	187703262
	.long	4046454716
	.long	349847258
	.long	1579357876
	.long	3835751551
	.long	293482231
	.long	375406524
	.long	3797942136
	.long	699694516
	.long	3158715752
	.long	3376535807
	.long	586964463
	.long	750813049
	.long	3300916976
	.long	1399389033
	.long	2022464208
	.long	2458104319
	.long	1173928927
	.long	1501626098
	.long	2306866656
	.long	2798778067
	.long	4044928416
	.long	621241343
	.long	2347857855
	.long	3003252197
	.long	318766016
	.long	1302588838
	.long	3794889537
	.long	1242482686
	.long	400748414
	.long	1711537099
	.long	637532033
	.long	2605177677
	.long	3294811778
	.long	2484965372
	.long	801496828
	.long	3423074199
	.long	1275064066
	.long	915388059
	.long	2294656261
	.long	674963448
	.long	1602993657
	.long	2551181103
	.long	2550128133
	.long	1830776118
	.long	294345226
	.long	1349926897
	.long	3205987314
	.long	807394910
	.long	805288971
	.long	3661552236
	.long	588690452
	.long	2699853794
	.long	2117007332
	.long	1614789820
	.long	1610577942
	.long	3028137177
	.long	1177380905
	.long	1104740292
	.long	4234014665
	.long	3229579640
	.long	3221155884
	.long	1761307059
	.long	2354761811
	.long	2209480585
	.long	4173062034
	.long	2164191985
	.long	2147344473
	.long	3522614119
	.long	414556326
	.long	123993874
	.long	4051156773
	.long	33416674
	.long	4294688947
	.long	2750260943
	.long	829112653
	.long	247987749
	.long	3807346250
	.long	66833348
	.long	4294410598
	.long	1205554591
	.long	1658225307
	.long	495975499
	.long	3319725204
	.long	133666696
	.long	4293853900
	.long	2411109183
	.long	3316450614
	.long	991950999
	.long	2344483112
	.long	267333393
	.long	4292740504
	.long	527251070
	.long	2337933933
	.long	1983901999
	.long	393998928
	.long	534666787
	.long	4290513712
	.long	1054502141
	.long	380900570
	.long	3967803999
	.long	787997856
	.long	1069333574
	.long	4286060128
	.long	2109004283
	.long	761801140
	.long	3640640703
	.long	1575995713
	.long	2138667148
	.long	4277152960
	.long	4218008566
	.long	1523602280
	.long	2986314111
	.long	3151991427
	.long	4277334296
	.long	4259338624
	.long	4141049836
	.long	3047204561
	.long	1677660927
	.long	2009015559
	.long	4259701297
	.long	4223709953
	.long	3987132377
	.long	1799441827
	.long	3355321855
	.long	4018031118
	.long	4224435298
	.long	4152452611
	.long	3679297459
	.long	3598883655
	.long	2415676415
	.long	3741094941
	.long	4153903301
	.long	4009937927
	.long	3063627623
	.long	2902800015
	.long	536385535
	.long	3187222587
	.long	4012839307
	.long	3724908559
	.long	1832287951
	.long	1510632735
	.long	1072771071
	.long	2079477878
	.long	3730711318
	.long	3154849823
	.long	3664575902
	.long	3021265470
	.long	2145542143
	.long	4158955756
	.long	3166455341
	.long	2014732351
	.long	3034184508
	.long	1747563645
	.long	4291084286
	.long	4022944216
	.long	2037943386
	.long	4029464703
	.long	1773401721
	.long	3495127291
	.long	4287201277
	.long	3750921137
	.long	4075886773
	.long	3763962110
	.long	3546803442
	.long	2695287286
	.long	4279435259
	.long	3206874979
	.long	3856806251
	.long	3232956925
	.long	2798639588
	.long	1095607277
	.long	4263903223
	.long	2118782663
	.long	3418645206
	.long	2170946555
	.long	1302311881
	.long	2191214555
	.long	4232839151
	.long	4237565327
	.long	2542323117
	.long	46925815
	.long	2604623763
	.long	87461814
	.long	4170711006
	.long	4180163359
	.long	789678938
	.long	93851631
	.long	914280231
	.long	174923629
	.long	4046454716
	.long	4065359423
	.long	1579357876
	.long	187703262
	.long	1828560463
	.long	349847258
	.long	3797942136
	.long	3835751551
	.long	3158715752
	.long	375406524
	.long	3657120927
	.long	699694516
	.long	3300916976
	.long	3376535807
	.long	2022464208
	.long	750813049
	.long	3019274558
	.long	1399389033
	.long	2306866656
	.long	2458104319
	.long	4044928416
	.long	1501626098
	.long	1743581820
	.long	2798778067
	.long	318766016
	.long	621241343
	.long	3794889537
	.long	3003252197
	.long	3487163641
	.long	1302588838
	.long	637532033
	.long	1242482686
	.long	3294811778
	.long	1711537099
	.long	2679359986
	.long	2605177677
	.long	1275064066
	.long	2484965372
	.long	2294656261
	.long	3423074199
	.long	1063752677
	.long	915388059
	.long	2550128133
	.long	674963448
	.long	294345226
	.long	2551181103
	.long	2127505355
	.long	1830776118
	.long	805288971
	.long	1349926897
	.long	588690452
	.long	807394910
	.long	4255010710
	.long	3661552236
	.long	1610577942
	.long	2699853794
	.long	1177380905
	.long	1614789820
	.long	4215054124
	.long	3028137177
	.long	3221155884
	.long	1104740292
	.long	2354761811
	.long	3229579640
	.long	4135140952
	.long	1761307059
	.long	2147344473
	.long	2209480585
	.long	414556326
	.long	2164191985
	.long	3975314608
	.long	3522614119
	.long	4294688947
	.long	123993874
	.long	829112653
	.long	33416674
	.long	3655661921
	.long	2750260943
	.long	4294410598
	.long	247987749
	.long	1658225307
	.long	66833348
	.long	3016356546
	.long	1205554591
	.long	4293853900
	.long	495975499
	.long	3316450614
	.long	133666696
	.long	1737745796
	.long	2411109183
	.long	4292740504
	.long	991950999
	.long	2337933933
	.long	267333393
	.long	3475491593
	.long	527251070
	.long	4290513712
	.long	1983901999
	.long	380900570
	.long	534666787
	.long	2656015891
	.long	1054502141
	.long	4286060128
	.long	3967803999
	.long	761801140
	.long	1069333574
	.long	1017064486
	.long	2109004283
	.long	4277152960
	.long	3640640703
	.long	1523602280
	.long	2138667148
	.long	2034128973
	.long	4218008566
	.long	4259338624
	.long	2986314111
	.long	3047204561
	.long	4277334296
	.long	4068257947
	.long	4141049836
	.long	4223709953
	.long	1677660927
	.long	1799441827
	.long	4259701297
	.long	3841548598
	.long	3987132377
	.long	4152452611
	.long	3355321855
	.long	3598883655
	.long	4224435298
	.long	3388129901
	.long	3679297459
	.long	4009937927
	.long	2415676415
	.long	2902800015
	.long	4153903301
	.long	2481292507
	.long	3063627623
	.long	3724908559
	.long	536385535
	.long	1510632735
	.long	4012839307
	.long	667617719
	.long	1832287951
	.long	3154849823
	.long	1072771071
	.long	3021265470
	.long	3730711318
	.long	1335235438
	.long	3664575902
	.long	2014732351
	.long	2145542143
	.long	1747563645
	.long	3166455341
	.long	2670470877
	.long	3034184508
	.long	4029464703
	.long	4291084286
	.long	3495127291
	.long	2037943386
	.long	1045974458
	.long	1773401721
	.long	3763962110
	.long	4287201277
	.long	2695287286
	.long	4075886773
	.long	2091948916
	.long	3546803442
	.long	3232956925
	.long	4279435259
	.long	1095607277
	.long	3856806251
	.long	4183897833
	.long	2798639588
	.long	2170946555
	.long	4263903223
	.long	2191214555
	.long	3418645206
	.long	4072828371
	.long	1302311881
	.long	46925815
	.long	4232839151
	.long	87461814
	.long	2542323117
	.long	3850689447
	.long	2604623763
	.long	93851631
	.long	4170711006
	.long	174923629
	.long	789678938
	.long	3406411599
	.long	914280231
	.long	187703262
	.long	4046454716
	.long	349847258
	.long	1579357876
	.long	2517855902
	.long	1828560463
	.long	375406524
	.long	3797942136
	.long	699694516
	.long	3158715752
	.long	740744509
	.long	3657120927
	.long	750813049
	.long	3300916976
	.long	1399389033
	.long	2022464208
	.long	1481489018
	.long	3019274558
	.long	1501626098
	.long	2306866656
	.long	2798778067
	.long	4044928416
	.long	2962978036
	.long	1743581820
	.long	3003252197
	.long	318766016
	.long	1302588838
	.long	3794889537
	.long	1630988776
	.long	3487163641
	.long	1711537099
	.long	637532033
	.long	2605177677
	.long	3294811778
	.long	3261977553
	.long	2679359986
	.long	3423074199
	.long	1275064066
	.long	915388059
	.long	2294656261
	.long	2228987811
	.long	1063752677
	.long	2551181103
	.long	2550128133
	.long	1830776118
	.long	294345226
	.long	163008326
	.long	2127505355
	.long	807394910
	.long	805288971
	.long	3661552236
	.long	588690452
	.long	326016652
	.long	4255010710
	.long	1614789820
	.long	1610577942
	.long	3028137177
	.long	1177380905
	.long	652033304
	.long	4215054124
	.long	3229579640
	.long	3221155884
	.long	1761307059
	.long	2354761811
	.long	1304066609
	.long	4135140952
	.long	2164191985
	.long	2147344473
	.long	3522614119
	.long	414556326
	.long	2608133219
	.long	3975314608
	.long	33416674
	.long	4294688947
	.long	2750260943
	.long	829112653
	.long	921299143
	.long	3655661921
	.long	66833348
	.long	4294410598
	.long	1205554591
	.long	1658225307
	.long	1842598287
	.long	3016356546
	.long	133666696
	.long	4293853900
	.long	2411109183
	.long	3316450614
	.long	3685196575
	.long	1737745796
	.long	267333393
	.long	4292740504
	.long	527251070
	.long	2337933933
	.long	3075425855
	.long	3475491593
	.long	534666787
	.long	4290513712
	.long	1054502141
	.long	380900570
	.long	1855884414
	.long	2656015891
	.long	1069333574
	.long	4286060128
	.long	2109004283
	.long	761801140
	.long	3711768829
	.long	1017064486
	.long	2138667148
	.long	4277152960
	.long	4218008566
	.long	1523602280
	.long	3128570363
	.long	2034128973
	.long	4277334296
	.long	4259338624
	.long	4141049836
	.long	3047204561
	.long	1962173430
	.long	4068257947
	.long	4259701297
	.long	4223709953
	.long	3987132377
	.long	1799441827
	.long	3924346860
	.long	3841548598
	.long	4224435298
	.long	4152452611
	.long	3679297459
	.long	3598883655
	.long	3553726425
	.long	3388129901
	.long	4153903301
	.long	4009937927
	.long	3063627623
	.long	2902800015
	.long	2812485555
	.long	2481292507
	.long	4012839307
	.long	3724908559
	.long	1832287951
	.long	1510632735
	.long	1330003814
	.long	667617719
	.long	3730711318
	.long	3154849823
	.long	3664575902
	.long	3021265470
	.long	2660007629
	.long	1335235438
	.long	3166455341
	.long	2014732351
	.long	3034184508
	.long	1747563645
	.long	1025047962
	.long	2670470877
	.long	2037943386
	.long	4029464703
	.long	1773401721
	.long	3495127291
	.long	2050095924
	.long	1045974458
	.long	4075886773
	.long	3763962110
	.long	3546803442
	.long	2695287286
	.long	4100191849
	.long	2091948916
	.long	3856806251
	.long	3232956925
	.long	2798639588
	.long	1095607277
	.long	3905416403
	.long	4183897833
	.long	3418645206
	.long	2170946555
	.long	1302311881
	.long	2191214555
	.long	3515865511
	.long	4072828371
	.long	2542323117
	.long	46925815
	.long	2604623763
	.long	87461814
	.long	2736763727
	.long	3850689447
	.long	789678938
	.long	93851631
	.long	914280231
	.long	174923629
	.long	1178560158
	.long	3406411599
	.long	1579357876
	.long	187703262
	.long	1828560463
	.long	349847258
	.long	2357120316
	.long	2517855902
	.long	3158715752
	.long	375406524
	.long	3657120927
	.long	699694516
	.long	419273337
	.long	740744509
	.long	2022464208
	.long	750813049
	.long	3019274558
	.long	1399389033
	.long	838546674
	.long	1481489018
	.long	4044928416
	.long	1501626098
	.long	1743581820
	.long	2798778067
	.long	1677093349
	.long	2962978036
	.long	3794889537
	.long	3003252197
	.long	3487163641
	.long	1302588838
	.long	3354186699
	.long	1630988776
	.long	3294811778
	.long	1711537099
	.long	2679359986
	.long	2605177677
	.long	2413406103
	.long	3261977553
	.long	2294656261
	.long	3423074199
	.long	1063752677
	.long	915388059
	.long	531844911
	.long	2228987811
	.long	294345226
	.long	2551181103
	.long	2127505355
	.long	1830776118
	.long	1063689823
	.long	163008326
	.long	588690452
	.long	807394910
	.long	4255010710
	.long	3661552236
	.long	2127379647
	.long	326016652
	.long	1177380905
	.long	1614789820
	.long	4215054124
	.long	3028137177
	.long	4254759295
	.long	652033304
	.long	2354761811
	.long	3229579640
	.long	4135140952
	.long	1761307059
	.long	4214551295
	.long	1304066609
	.long	414556326
	.long	2164191985
	.long	3975314608
	.long	3522614119
	.long	4134135294
	.long	2608133219
	.long	829112653
	.long	33416674
	.long	3655661921
	.long	2750260943
	.long	3973303293
	.long	921299143
	.long	1658225307
	.long	66833348
	.long	3016356546
	.long	1205554591
	.long	3651639290
	.long	1842598287
	.long	3316450614
	.long	133666696
	.long	1737745796
	.long	2411109183
	.long	3008311285
	.long	3685196575
	.long	2337933933
	.long	267333393
	.long	3475491593
	.long	527251070
	.long	1721655274
	.long	3075425855
	.long	380900570
	.long	534666787
	.long	2656015891
	.long	1054502141
	.long	3443310548
	.long	1855884414
	.long	761801140
	.long	1069333574
	.long	1017064486
	.long	2109004283
	.long	2591653800
	.long	3711768829
	.long	1523602280
	.long	2138667148
	.long	2034128973
	.long	4218008566
	.long	888340305
	.long	3128570363
	.long	3047204561
	.long	4277334296
	.long	4068257947
	.long	4141049836
	.long	1776680610
	.long	1962173430
	.long	1799441827
	.long	4259701297
	.long	3841548598
	.long	3987132377
	.long	3553361221
	.long	3924346860
	.long	3598883655
	.long	4224435298
	.long	3388129901
	.long	3679297459
	.long	2811755147
	.long	3553726425
	.long	2902800015
	.long	4153903301
	.long	2481292507
	.long	3063627623
	.long	1328542998
	.long	2812485555
	.long	1510632735
	.long	4012839307
	.long	667617719
	.long	1832287951
	.long	2657085997
	.long	1330003814
	.long	3021265470
	.long	3730711318
	.long	1335235438
	.long	3664575902
	.long	1019204698
	.long	2660007629
	.long	1747563645
	.long	3166455341
	.long	2670470877
	.long	3034184508
	.long	2038409397
	.long	1025047962
	.long	3495127291
	.long	2037943386
	.long	1045974458
	.long	1773401721
	.long	4076818795
	.long	2050095924
	.long	2695287286
	.long	4075886773
	.long	2091948916
	.long	3546803442
	.long	3858670295
	.long	4100191849
	.long	1095607277
	.long	3856806251
	.long	4183897833
	.long	2798639588
	.long	3422373294
	.long	3905416403
	.long	2191214555
	.long	3418645206
	.long	4072828371
	.long	1302311881
	.long	2549779293
	.long	3515865511
	.long	87461814
	.long	2542323117
	.long	3850689447
	.long	2604623763
	.long	804591290
	.long	2736763727
	.long	174923629
	.long	789678938
	.long	3406411599
	.long	914280231
	.long	1609182581
	.long	1178560158
	.long	349847258
	.long	1579357876
	.long	2517855902
	.long	1828560463
	.long	3218365162
	.long	2357120316
	.long	699694516
	.long	3158715752
	.long	740744509
	.long	3657120927
	.long	2141763028
	.long	419273337
	.long	1399389033
	.long	2022464208
	.long	1481489018
	.long	3019274558
	.long	4283526057
	.long	838546674
	.long	2798778067
	.long	4044928416
	.long	2962978036
	.long	1743581820
	.long	4272084818
	.long	1677093349
	.long	1302588838
	.long	3794889537
	.long	1630988776
	.long	3487163641
	.long	4249202340
	.long	3354186699
	.long	2605177677
	.long	3294811778
	.long	3261977553
	.long	2679359986
	.long	4203437385
	.long	2413406103
	.long	915388059
	.long	2294656261
	.long	2228987811
	.long	1063752677
	.long	4111907475
	.long	531844911
	.long	1830776118
	.long	294345226
	.long	163008326
	.long	2127505355
	.long	3928847655
	.long	1063689823
	.long	3661552236
	.long	588690452
	.long	326016652
	.long	4255010710
	.long	3562728015
	.long	2127379647
	.long	3028137177
	.long	1177380905
	.long	652033304
	.long	4215054124
	.long	2830488734
	.long	4254759295
	.long	1761307059
	.long	2354761811
	.long	1304066609
	.long	4135140952
	.long	1366010173
	.long	4214551295
	.long	3522614119
	.long	414556326
	.long	2608133219
	.long	3975314608
	.long	2732020347
	.long	4134135294
	.long	2750260943
	.long	829112653
	.long	921299143
	.long	3655661921
	.long	1169073399
	.long	3973303293
	.long	1205554591
	.long	1658225307
	.long	1842598287
	.long	3016356546
	.long	2338146798
	.long	3651639290
	.long	2411109183
	.long	3316450614
	.long	3685196575
	.long	1737745796
	.long	381326301
	.long	3008311285
	.long	527251070
	.long	2337933933
	.long	3075425855
	.long	3475491593
	.long	762652602
	.long	1721655274
	.long	1054502141
	.long	380900570
	.long	1855884414
	.long	2656015891
	.long	1525305205
	.long	3443310548
	.long	2109004283
	.long	761801140
	.long	3711768829
	.long	1017064486
	.long	3050610411
	.long	2591653800
	.long	4218008566
	.long	1523602280
	.long	3128570363
	.long	2034128973
	.long	1806253526
	.long	888340305
	.long	4141049836
	.long	3047204561
	.long	1962173430
	.long	4068257947
	.long	3612507052
	.long	1776680610
	.long	3987132377
	.long	1799441827
	.long	3924346860
	.long	3841548598
	.long	2930046808
	.long	3553361221
	.long	3679297459
	.long	3598883655
	.long	3553726425
	.long	3388129901
	.long	1565126321
	.long	2811755147
	.long	3063627623
	.long	2902800015
	.long	2812485555
	.long	2481292507
	.long	3130252643
	.long	1328542998
	.long	1832287951
	.long	1510632735
	.long	1330003814
	.long	667617719
	.long	1965537991
	.long	2657085997
	.long	3664575902
	.long	3021265470
	.long	2660007629
	.long	1335235438
	.long	3931075983
	.long	1019204698
	.long	3034184508
	.long	1747563645
	.long	1025047962
	.long	2670470877
	.long	3567184671
	.long	2038409397
	.long	1773401721
	.long	3495127291
	.long	2050095924
	.long	1045974458
	.long	2839402047
	.long	4076818795
	.long	3546803442
	.long	2695287286
	.long	4100191849
	.long	2091948916
	.long	1383836798
	.long	3858670295
	.long	2798639588
	.long	1095607277
	.long	3905416403
	.long	4183897833
	.long	2767673597
	.long	3422373294
	.long	1302311881
	.long	2191214555
	.long	3515865511
	.long	4072828371
	.long	1240379898
	.long	2549779293
	.long	2604623763
	.long	87461814
	.long	2736763727
	.long	3850689447
	.long	2480759797
	.long	804591290
	.long	914280231
	.long	174923629
	.long	1178560158
	.long	3406411599
	.long	666552299
	.long	1609182581
	.long	1828560463
	.long	349847258
	.long	2357120316
	.long	2517855902
	.long	1333104599
	.long	3218365162
	.long	3657120927
	.long	699694516
	.long	419273337
	.long	740744509
	.long	2666209199
	.long	2141763028
	.long	3019274558
	.long	1399389033
	.long	838546674
	.long	1481489018
	.long	1037451103
	.long	4283526057
	.long	1743581820
	.long	2798778067
	.long	1677093349
	.long	2962978036
	.long	2074902206
	.long	4272084818
	.long	3487163641
	.long	1302588838
	.long	3354186699
	.long	1630988776
	.long	4149804412
	.long	4249202340
	.long	2679359986
	.long	2605177677
	.long	2413406103
	.long	3261977553
	.long	4004641529
	.long	4203437385
	.long	1063752677
	.long	915388059
	.long	531844911
	.long	2228987811
	.long	3714315762
	.long	4111907475
	.long	2127505355
	.long	1830776118
	.long	1063689823
	.long	163008326
	.long	3133664229
	.long	3928847655
	.long	4255010710
	.long	3661552236
	.long	2127379647
	.long	326016652
	.long	1972361163
	.long	3562728015
	.long	4215054124
	.long	3028137177
	.long	4254759295
	.long	652033304
	.long	3944722327
	.long	2830488734
	.long	4135140952
	.long	1761307059
	.long	4214551295
	.long	1304066609
	.long	3594477359
	.long	1366010173
	.long	3975314608
	.long	3522614119
	.long	4134135294
	.long	2608133219
	.long	2893987423
	.long	2732020347
	.long	3655661921
	.long	2750260943
	.long	3973303293
	.long	921299143
	.long	1493007550
	.long	1169073399
	.long	3016356546
	.long	1205554591
	.long	3651639290
	.long	1842598287
	.long	2986015100
	.long	2338146798
	.long	1737745796
	.long	2411109183
	.long	3008311285
	.long	3685196575
	.long	1677062904
	.long	381326301
	.long	3475491593
	.long	527251070
	.long	1721655274
	.long	3075425855
	.long	3354125809
	.long	762652602
	.long	2656015891
	.long	1054502141
	.long	3443310548
	.long	1855884414
	.long	2413284322
	.long	1525305205
	.long	1017064486
	.long	2109004283
	.long	2591653800
	.long	3711768829
	.long	531601349
	.long	3050610411
	.long	2034128973
	.long	4218008566
	.long	888340305
	.long	3128570363
	.long	1063202699
	.long	1806253526
	.long	4068257947
	.long	4141049836
	.long	1776680610
	.long	1962173430
	.long	2126405399
	.long	3612507052
	.long	3841548598
	.long	3987132377
	.long	3553361221
	.long	3924346860
	.long	4252810799
	.long	2930046808
	.long	3388129901
	.long	3679297459
	.long	2811755147
	.long	3553726425
	.long	4210654302
	.long	1565126321
	.long	2481292507
	.long	3063627623
	.long	1328542998
	.long	2812485555
	.long	4126341309
	.long	3130252643
	.long	667617719
	.long	1832287951
	.long	2657085997
	.long	1330003814
	.long	3957715323
	.long	1965537991
	.long	1335235438
	.long	3664575902
	.long	1019204698
	.long	2660007629
	.long	3620463350
	.long	3931075983
	.long	2670470877
	.long	3034184508
	.long	2038409397
	.long	1025047962
	.long	2945959404
	.long	3567184671
	.long	1045974458
	.long	1773401721
	.long	4076818795
	.long	2050095924
	.long	1596951513
	.long	2839402047
	.long	2091948916
	.long	3546803442
	.long	3858670295
	.long	4100191849
	.long	3193903027
	.long	1383836798
	.long	4183897833
	.long	2798639588
	.long	3422373294
	.long	3905416403
	.long	2092838759
	.long	2767673597
	.long	4072828371
	.long	1302311881
	.long	2549779293
	.long	3515865511
	.long	4185677519
	.long	1240379898
	.long	3850689447
	.long	2604623763
	.long	804591290
	.long	2736763727
	.long	4076387742
	.long	2480759797
	.long	3406411599
	.long	914280231
	.long	1609182581
	.long	1178560158
	.long	3857808189
	.long	666552299
	.long	2517855902
	.long	1828560463
	.long	3218365162
	.long	2357120316
	.long	3420649082
	.long	1333104599
	.long	740744509
	.long	3657120927
	.long	2141763028
	.long	419273337
	.long	2546330868
	.long	2666209199
	.long	1481489018
	.long	3019274558
	.long	4283526057
	.long	838546674
	.long	797694440
	.long	1037451103
	.long	2962978036
	.long	1743581820
	.long	4272084818
	.long	1677093349
	.long	1595388880
	.long	2074902206
	.long	1630988776
	.long	3487163641
	.long	4249202340
	.long	3354186699
	.long	3190777760
	.long	4149804412
	.long	3261977553
	.long	2679359986
	.long	4203437385
	.long	2413406103
	.long	2086588225
	.long	4004641529
	.long	2228987811
	.long	1063752677
	.long	4111907475
	.long	531844911
	.long	4173176451
	.long	3714315762
	.long	163008326
	.long	2127505355
	.long	3928847655
	.long	1063689823
	.long	4051385607
	.long	3133664229
	.long	326016652
	.long	4255010710
	.long	3562728015
	.long	2127379647
	.long	3807803918
	.long	1972361163
	.long	652033304
	.long	4215054124
	.long	2830488734
	.long	4254759295
	.long	3320640540
	.long	3944722327
	.long	1304066609
	.long	4135140952
	.long	1366010173
	.long	4214551295
	.long	2346313785
	.long	3594477359
	.long	2608133219
	.long	3975314608
	.long	2732020347
	.long	4134135294
	.long	397660275
	.long	2893987423
	.long	921299143
	.long	3655661921
	.long	1169073399
	.long	3973303293
	.long	795320551
	.long	1493007550
	.long	1842598287
	.long	3016356546
	.long	2338146798
	.long	3651639290
	.long	1590641102
	.long	2986015100
	.long	3685196575
	.long	1737745796
	.long	381326301
	.long	3008311285
	.long	3181282204
	.long	1677062904
	.long	3075425855
	.long	3475491593
	.long	762652602
	.long	1721655274
	.long	2067597113
	.long	3354125809
	.long	1855884414
	.long	2656015891
	.long	1525305205
	.long	3443310548
	.long	4135194227
	.long	2413284322
	.long	3711768829
	.long	1017064486
	.long	3050610411
	.long	2591653800
	.long	3975421159
	.long	531601349
	.long	3128570363
	.long	2034128973
	.long	1806253526
	.long	888340305
	.long	3655875023
	.long	1063202699
	.long	1962173430
	.long	4068257947
	.long	3612507052
	.long	1776680610
	.long	3016782751
	.long	2126405399
	.long	3924346860
	.long	3841548598
	.long	2930046808
	.long	3553361221
	.long	1738598206
	.long	4252810799
	.long	3553726425
	.long	3388129901
	.long	1565126321
	.long	2811755147
	.long	3477196413
	.long	4210654302
	.long	2812485555
	.long	2481292507
	.long	3130252643
	.long	1328542998
	.long	2659425531
	.long	4126341309
	.long	1330003814
	.long	667617719
	.long	1965537991
	.long	2657085997
	.long	1023883767
	.long	3957715323
	.long	2660007629
	.long	1335235438
	.long	3931075983
	.long	1019204698
	.long	2047767535
	.long	3620463350
	.long	1025047962
	.long	2670470877
	.long	3567184671
	.long	2038409397
	.long	4095535070
	.long	2945959404
	.long	2050095924
	.long	1045974458
	.long	2839402047
	.long	4076818795
	.long	3896102844
	.long	1596951513
	.long	4100191849
	.long	2091948916
	.long	1383836798
	.long	3858670295
	.long	3497238392
	.long	3193903027
	.long	3905416403
	.long	4183897833
	.long	2767673597
	.long	3422373294
	.long	2699509489
	.long	2092838759
	.long	3515865511
	.long	4072828371
	.long	1240379898
	.long	2549779293
	.long	1104051682
	.long	4185677519
	.long	2736763727
	.long	3850689447
	.long	2480759797
	.long	804591290
	.long	2208103365
	.long	4076387742
	.long	1178560158
	.long	3406411599
	.long	666552299
	.long	1609182581
	.long	121239434
	.long	3857808189
	.long	2357120316
	.long	2517855902
	.long	1333104599
	.long	3218365162
	.long	242478868
	.long	3420649082
	.long	419273337
	.long	740744509
	.long	2666209199
	.long	2141763028
	.long	484957737
	.long	2546330868
	.long	838546674
	.long	1481489018
	.long	1037451103
	.long	4283526057
	.long	969915474
	.long	797694440
	.long	1677093349
	.long	2962978036
	.long	2074902206
	.long	4272084818
	.long	1939830949
	.long	1595388880
	.long	3354186699
	.long	1630988776
	.long	4149804412
	.long	4249202340
	.long	3879661898
	.long	3190777760
	.long	2413406103
	.long	3261977553
	.long	4004641529
	.long	4203437385
	.long	3464356500
	.long	2086588225
	.long	531844911
	.long	2228987811
	.long	3714315762
	.long	4111907475
	.long	2633745705
	.long	4173176451
	.long	1063689823
	.long	163008326
	.long	3133664229
	.long	3928847655
	.long	972524114
	.long	4051385607
	.long	2127379647
	.long	326016652
	.long	1972361163
	.long	3562728015
	.long	1945048229
	.long	3807803918
	.long	4254759295
	.long	652033304
	.long	3944722327
	.long	2830488734
	.long	3890096458
	.long	3320640540
	.long	4214551295
	.long	1304066609
	.long	3594477359
	.long	1366010173
	.long	3485225620
	.long	2346313785
	.long	4134135294
	.long	2608133219
	.long	2893987423
	.long	2732020347
	.long	2675483945
	.long	397660275
	.long	3973303293
	.long	921299143
	.long	1493007550
	.long	1169073399
	.long	1056000594
	.long	795320551
	.long	3651639290
	.long	1842598287
	.long	2986015100
	.long	2338146798
	.long	2112001188
	.long	1590641102
	.long	3008311285
	.long	3685196575
	.long	1677062904
	.long	381326301
	.long	4224002377
	.long	3181282204
	.long	1721655274
	.long	3075425855
	.long	3354125809
	.long	762652602
	.long	4153037458
	.long	2067597113
	.long	3443310548
	.long	1855884414
	.long	2413284322
	.long	1525305205
	.long	4011107621
	.long	4135194227
	.long	2591653800
	.long	3711768829
	.long	531601349
	.long	3050610411
	.long	3727247947
	.long	3975421159
	.long	888340305
	.long	3128570363
	.long	1063202699
	.long	1806253526
	.long	3159528599
	.long	3655875023
	.long	1776680610
	.long	1962173430
	.long	2126405399
	.long	3612507052
	.long	2024089902
	.long	3016782751
	.long	3553361221
	.long	3924346860
	.long	4252810799
	.long	2930046808
	.long	4048179805
	.long	1738598206
	.long	2811755147
	.long	3553726425
	.long	4210654302
	.long	1565126321
	.long	3801392314
	.long	3477196413
	.long	1328542998
	.long	2812485555
	.long	4126341309
	.long	3130252643
	.long	3307817333
	.long	2659425531
	.long	2657085997
	.long	1330003814
	.long	3957715323
	.long	1965537991
	.long	2320667370
	.long	1023883767
	.long	1019204698
	.long	2660007629
	.long	3620463350
	.long	3931075983
	.long	346367444
	.long	2047767535
	.long	2038409397
	.long	1025047962
	.long	2945959404
	.long	3567184671
	.long	692734889
	.long	4095535070
	.long	4076818795
	.long	2050095924
	.long	1596951513
	.long	2839402047
	.long	1385469779
	.long	3896102844
	.long	3858670295
	.long	4100191849
	.long	3193903027
	.long	1383836798
	.long	2770939558
	.long	3497238392
	.long	3422373294
	.long	3905416403
	.long	2092838759
	.long	2767673597
	.long	1246911821
	.long	2699509489
	.long	2549779293
	.long	3515865511
	.long	4185677519
	.long	1240379898
	.long	2493823642
	.long	1104051682
	.long	804591290
	.long	2736763727
	.long	4076387742
	.long	2480759797
	.long	692679989
	.long	2208103365
	.long	1609182581
	.long	1178560158
	.long	3857808189
	.long	666552299
	.long	1385359979
	.long	121239434
	.long	3218365162
	.long	2357120316
	.long	3420649082
	.long	1333104599
	.long	2770719959
	.long	242478868
	.long	2141763028
	.long	419273337
	.long	2546330868
	.long	2666209199
	.long	1246472623
	.long	484957737
	.long	4283526057
	.long	838546674
	.long	797694440
	.long	1037451103
	.long	2492945247
	.long	969915474
	.long	4272084818
	.long	1677093349
	.long	1595388880
	.long	2074902206
	.long	690923199
	.long	1939830949
	.long	4249202340
	.long	3354186699
	.long	3190777760
	.long	4149804412
	.long	1381846399
	.long	3879661898
	.long	4203437385
	.long	2413406103
	.long	2086588225
	.long	4004641529
	.long	2763692798
	.long	3464356500
	.long	4111907475
	.long	531844911
	.long	4173176451
	.long	3714315762
	.long	1232418301
	.long	2633745705
	.long	3928847655
	.long	1063689823
	.long	4051385607
	.long	3133664229
	.long	2464836603
	.long	972524114
	.long	3562728015
	.long	2127379647
	.long	3807803918
	.long	1972361163
	.long	634705910
	.long	1945048229
	.long	2830488734
	.long	4254759295
	.long	3320640540
	.long	3944722327
	.long	1269411821
	.long	3890096458
	.long	1366010173
	.long	4214551295
	.long	2346313785
	.long	3594477359
	.long	2538823642
	.long	3485225620
	.long	2732020347
	.long	4134135294
	.long	397660275
	.long	2893987423
	.long	782679989
	.long	2675483945
	.long	1169073399
	.long	3973303293
	.long	795320551
	.long	1493007550
	.long	1565359979
	.long	1056000594
	.long	2338146798
	.long	3651639290
	.long	1590641102
	.long	2986015100
	.long	3130719959
	.long	2112001188
	.long	381326301
	.long	3008311285
	.long	3181282204
	.long	1677062904
	.long	1966472623
	.long	4224002377
	.long	762652602
	.long	1721655274
	.long	2067597113
	.long	3354125809
	.long	3932945247
	.long	4153037458
	.long	1525305205
	.long	3443310548
	.long	4135194227
	.long	2413284322
	.long	3570923199
	.long	4011107621
	.long	3050610411
	.long	2591653800
	.long	3975421159
	.long	531601349
	.long	2846879102
	.long	3727247947
	.long	1806253526
	.long	888340305
	.long	3655875023
	.long	1063202699
	.long	1398790909
	.long	3159528599
	.long	3612507052
	.long	1776680610
	.long	3016782751
	.long	2126405399
	.long	2797581819
	.long	2024089902
	.long	2930046808
	.long	3553361221
	.long	1738598206
	.long	4252810799
	.long	1300196342
	.long	4048179805
	.long	1565126321
	.long	2811755147
	.long	3477196413
	.long	4210654302
	.long	2600392684
	.long	3801392314
	.long	3130252643
	.long	1328542998
	.long	2659425531
	.long	4126341309
	.long	905818072
	.long	3307817333
	.long	1965537991
	.long	2657085997
	.long	1023883767
	.long	3957715323
	.long	1811636145
	.long	2320667370
	.long	3931075983
	.long	1019204698
	.long	2047767535
	.long	3620463350
	.long	3623272290
	.long	346367444
	.long	3567184671
	.long	2038409397
	.long	4095535070
	.long	2945959404
	.long	2951577284
	.long	692734889
	.long	2839402047
	.long	4076818795
	.long	3896102844
	.long	1596951513
	.long	1608187272
	.long	1385469779
	.long	1383836798
	.long	3858670295
	.long	3497238392
	.long	3193903027
	.long	3216374545
	.long	2770939558
	.long	2767673597
	.long	3422373294
	.long	2699509489
	.long	2092838759
	.long	2137781795
	.long	1246911821
	.long	1240379898
	.long	2549779293
	.long	1104051682
	.long	4185677519
	.long	4275563591
	.long	2493823642
	.long	2480759797
	.long	804591290
	.long	2208103365
	.long	4076387742
	.long	4256159887
	.long	692679989
	.long	666552299
	.long	1609182581
	.long	121239434
	.long	3857808189
	.long	4217352479
	.long	1385359979
	.long	1333104599
	.long	3218365162
	.long	242478868
	.long	3420649082
	.long	4139737663
	.long	2770719959
	.long	2666209199
	.long	2141763028
	.long	484957737
	.long	2546330868
	.long	3984508030
	.long	1246472623
	.long	1037451103
	.long	4283526057
	.long	969915474
	.long	797694440
	.long	3674048764
	.long	2492945247
	.long	2074902206
	.long	4272084818
	.long	1939830949
	.long	1595388880
	.long	3053130232
	.long	690923199
	.long	4149804412
	.long	4249202340
	.long	3879661898
	.long	3190777760
	.long	1811293169
	.long	1381846399
	.long	4004641529
	.long	4203437385
	.long	3464356500
	.long	2086588225
	.long	3622586339
	.long	2763692798
	.long	3714315762
	.long	4111907475
	.long	2633745705
	.long	4173176451
	.long	2950205382
	.long	1232418301
	.long	3133664229
	.long	3928847655
	.long	972524114
	.long	4051385607
	.long	1605443469
	.long	2464836603
	.long	1972361163
	.long	3562728015
	.long	1945048229
	.long	3807803918
	.long	3210886938
	.long	634705910
	.long	3944722327
	.long	2830488734
	.long	3890096458
	.long	3320640540
	.long	2126806581
	.long	1269411821
	.long	3594477359
	.long	1366010173
	.long	3485225620
	.long	2346313785
	.long	4253613162
	.long	2538823642
	.long	2893987423
	.long	2732020347
	.long	2675483945
	.long	397660275
	.long	4212259029
	.long	782679989
	.long	1493007550
	.long	1169073399
	.long	1056000594
	.long	795320551
	.long	4129550763
	.long	1565359979
	.long	2986015100
	.long	2338146798
	.long	2112001188
	.long	1590641102
	.long	3964134231
	.long	3130719959
	.long	1677062904
	.long	381326301
	.long	4224002377
	.long	3181282204
	.long	3633301166
	.long	1966472623
	.long	3354125809
	.long	762652602
	.long	4153037458
	.long	2067597113
	.long	2971635037
	.long	3932945247
	.long	2413284322
	.long	1525305205
	.long	4011107621
	.long	4135194227
	.long	1648302778
	.long	3570923199
	.long	531601349
	.long	3050610411
	.long	3727247947
	.long	3975421159
	.long	3296605556
	.long	2846879102
	.long	1063202699
	.long	1806253526
	.long	3159528599
	.long	3655875023
	.long	2298243816
	.long	1398790909
	.long	2126405399
	.long	3612507052
	.long	2024089902
	.long	3016782751
	.long	301520336
	.long	2797581819
	.long	4252810799
	.long	2930046808
	.long	4048179805
	.long	1738598206
	.long	603040673
	.long	1300196342
	.long	4210654302
	.long	1565126321
	.long	3801392314
	.long	3477196413
	.long	1206081346
	.long	2600392684
	.long	4126341309
	.long	3130252643
	.long	3307817333
	.long	2659425531
	.long	2412162692
	.long	905818072
	.long	3957715323
	.long	1965537991
	.long	2320667370
	.long	1023883767
	.long	529358088
	.long	1811636145
	.long	3620463350
	.long	3931075983
	.long	346367444
	.long	2047767535
	.long	1058716176
	.long	3623272290
	.long	2945959404
	.long	3567184671
	.long	692734889
	.long	4095535070
	.long	2117432353
	.long	2951577284
	.long	1596951513
	.long	2839402047
	.long	1385469779
	.long	3896102844
	.long	4234864706
	.long	1608187272
	.long	3193903027
	.long	1383836798
	.long	2770939558
	.long	3497238392
	.long	4174762117
	.long	3216374545
	.long	2092838759
	.long	2767673597
	.long	1246911821
	.long	2699509489
	.long	4054556938
	.long	2137781795
	.long	4185677519
	.long	1240379898
	.long	2493823642
	.long	1104051682
	.long	3814146581
	.long	4275563591
	.long	4076387742
	.long	2480759797
	.long	692679989
	.long	2208103365
	.long	3333325867
	.long	4256159887
	.long	3857808189
	.long	666552299
	.long	1385359979
	.long	121239434
	.long	2371684438
	.long	4217352479
	.long	3420649082
	.long	1333104599
	.long	2770719959
	.long	242478868
	.long	448401580
	.long	4139737663
	.long	2546330868
	.long	2666209199
	.long	1246472623
	.long	484957737
	.long	896803160
	.long	3984508030
	.type	__svml_dtan_ha_reduction_data_internal,@object
	.size	__svml_dtan_ha_reduction_data_internal,49152
	.space 512, 0x00 	# pad
	.align 64
	.hidden __svml_dtan_ha_data_internal
	.globl __svml_dtan_ha_data_internal
__svml_dtan_ha_data_internal:
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	856972294
	.long	1013032486
	.long	856972294
	.long	1013032486
	.long	856972294
	.long	1013032486
	.long	856972294
	.long	1013032486
	.long	856972294
	.long	1013032486
	.long	856972294
	.long	1013032486
	.long	856972294
	.long	1013032486
	.long	856972294
	.long	1013032486
	.long	688016905
	.long	958143697
	.long	688016905
	.long	958143697
	.long	688016905
	.long	958143697
	.long	688016905
	.long	958143697
	.long	688016905
	.long	958143697
	.long	688016905
	.long	958143697
	.long	688016905
	.long	958143697
	.long	688016905
	.long	958143697
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
	.long	0
	.long	2147483648
	.long	3763679576
	.long	1070167541
	.long	2583490354
	.long	1071284857
	.long	716700048
	.long	1071997368
	.long	0
	.long	1072693248
	.long	3797578849
	.long	1073213976
	.long	859807206
	.long	1073958991
	.long	3795994481
	.long	1075059710
	.long	4294967295
	.long	4293918719
	.long	3795994481
	.long	3222543358
	.long	859807206
	.long	3221442639
	.long	3797578849
	.long	3220697624
	.long	0
	.long	3220176896
	.long	716700048
	.long	3219481016
	.long	2583490354
	.long	3218768505
	.long	3763679576
	.long	3217651189
	.long	0
	.long	2147483648
	.long	1732516166
	.long	1009710547
	.long	2973134505
	.long	1014008623
	.long	561165941
	.long	1015523525
	.long	0
	.long	0
	.long	1767150223
	.long	1016338938
	.long	4129738197
	.long	1017254245
	.long	4275273486
	.long	1015088902
	.long	0
	.long	4238344192
	.long	4275273486
	.long	3162572550
	.long	4129738197
	.long	3164737893
	.long	1767150223
	.long	3163822586
	.long	0
	.long	0
	.long	561165941
	.long	3163007173
	.long	2973134505
	.long	3161492271
	.long	1732516166
	.long	3157194195
	.long	1431655900
	.long	1070945621
	.long	1431655900
	.long	1070945621
	.long	1431655900
	.long	1070945621
	.long	1431655900
	.long	1070945621
	.long	1431655900
	.long	1070945621
	.long	1431655900
	.long	1070945621
	.long	1431655900
	.long	1070945621
	.long	1431655900
	.long	1070945621
	.long	285935618
	.long	1069617425
	.long	285935618
	.long	1069617425
	.long	285935618
	.long	1069617425
	.long	285935618
	.long	1069617425
	.long	285935618
	.long	1069617425
	.long	285935618
	.long	1069617425
	.long	285935618
	.long	1069617425
	.long	285935618
	.long	1069617425
	.long	1218258378
	.long	1068212666
	.long	1218258378
	.long	1068212666
	.long	1218258378
	.long	1068212666
	.long	1218258378
	.long	1068212666
	.long	1218258378
	.long	1068212666
	.long	1218258378
	.long	1068212666
	.long	1218258378
	.long	1068212666
	.long	1218258378
	.long	1068212666
	.long	1716452265
	.long	1066820779
	.long	1716452265
	.long	1066820779
	.long	1716452265
	.long	1066820779
	.long	1716452265
	.long	1066820779
	.long	1716452265
	.long	1066820779
	.long	1716452265
	.long	1066820779
	.long	1716452265
	.long	1066820779
	.long	1716452265
	.long	1066820779
	.long	3351902813
	.long	1065508044
	.long	3351902813
	.long	1065508044
	.long	3351902813
	.long	1065508044
	.long	3351902813
	.long	1065508044
	.long	3351902813
	.long	1065508044
	.long	3351902813
	.long	1065508044
	.long	3351902813
	.long	1065508044
	.long	3351902813
	.long	1065508044
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
	.long	2146435072
	.long	0
	.long	2146435072
	.long	0
	.long	2146435072
	.long	0
	.long	2146435072
	.long	0
	.long	2146435072
	.long	0
	.long	2146435072
	.long	0
	.long	2146435072
	.long	0
	.long	2146435072
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
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	511
	.long	0
	.long	511
	.long	0
	.long	511
	.long	0
	.long	511
	.long	0
	.long	511
	.long	0
	.long	511
	.long	0
	.long	511
	.long	0
	.long	511
	.long	0
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
	.long	1413754136
	.long	1073291771
	.long	856972295
	.long	1016178214
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1431655769
	.long	1070945621
	.long	0
	.long	0
	.long	285114973
	.long	1069617425
	.long	0
	.long	0
	.long	3223786787
	.long	1068212718
	.long	1570481342
	.long	1073278903
	.long	2595802427
	.long	1014369127
	.long	0
	.long	0
	.long	75380366
	.long	1065951822
	.long	2999349512
	.long	3155830414
	.long	0
	.long	1072693248
	.long	2911696896
	.long	1059306942
	.long	451505506
	.long	1065952070
	.long	3089290734
	.long	1070946463
	.long	3705470160
	.long	1065403174
	.long	2568117119
	.long	1069618857
	.long	643394266
	.long	1064501452
	.long	501385640
	.long	1068202863
	.long	1727208548
	.long	1073266035
	.long	2531251621
	.long	3162145225
	.long	0
	.long	0
	.long	612012528
	.long	1067000646
	.long	549540416
	.long	3153143513
	.long	0
	.long	1072693248
	.long	2064191488
	.long	1061404484
	.long	4042886527
	.long	1067001638
	.long	149923164
	.long	1070948993
	.long	104448276
	.long	1066453158
	.long	2663431480
	.long	1069623160
	.long	4100741303
	.long	1065552806
	.long	3253087105
	.long	1068220130
	.long	1883935754
	.long	1073253167
	.long	2988318241
	.long	3163734974
	.long	0
	.long	0
	.long	3919601072
	.long	1067637674
	.long	2391050108
	.long	3158536696
	.long	0
	.long	1072693248
	.long	1803946752
	.long	1062615560
	.long	2247013512
	.long	1067639351
	.long	3277043521
	.long	1070953215
	.long	578207594
	.long	1067006653
	.long	1619551617
	.long	1069630354
	.long	2647612950
	.long	1066183733
	.long	2691224773
	.long	1068193529
	.long	2040662960
	.long	1073240299
	.long	4270081753
	.long	3164530442
	.long	0
	.long	0
	.long	2319126882
	.long	1068050215
	.long	2052183488
	.long	1006305725
	.long	0
	.long	1072693248
	.long	2962101888
	.long	1063503197
	.long	3966050657
	.long	1068054193
	.long	3178024141
	.long	1070959142
	.long	1146831000
	.long	1067507381
	.long	1630003540
	.long	1069640468
	.long	2204487828
	.long	1066612393
	.long	783402153
	.long	1068243248
	.long	2197390165
	.long	1073227431
	.long	3038089327
	.long	1016395176
	.long	0
	.long	0
	.long	3930011523
	.long	1068463253
	.long	3574549514
	.long	3158343440
	.long	0
	.long	1072693248
	.long	930814464
	.long	1064233867
	.long	3218126771
	.long	1068471034
	.long	873740287
	.long	1070966789
	.long	1580060562
	.long	1067789097
	.long	1505280051
	.long	1069653548
	.long	2221338604
	.long	1066937592
	.long	2641117162
	.long	1068312400
	.long	2354117371
	.long	1073214563
	.long	3512651629
	.long	1015129272
	.long	0
	.long	0
	.long	3439278129
	.long	1068687929
	.long	1073016494
	.long	1012431341
	.long	0
	.long	1072693248
	.long	2934847296
	.long	1064716672
	.long	2625704508
	.long	1068694663
	.long	3692939630
	.long	1070976174
	.long	3072165553
	.long	1068074393
	.long	223733198
	.long	1069669653
	.long	1158528257
	.long	1067269817
	.long	3197805108
	.long	1068357327
	.long	2510844577
	.long	1073201695
	.long	498468876
	.long	3161098111
	.long	0
	.long	0
	.long	1972895212
	.long	1068895135
	.long	3208334872
	.long	1010638399
	.long	0
	.long	1072693248
	.long	4107853760
	.long	1065246718
	.long	2229347510
	.long	1068905849
	.long	4172638464
	.long	1070987323
	.long	212469442
	.long	1068364021
	.long	1989165656
	.long	1069688852
	.long	4131454312
	.long	1067530480
	.long	3453538591
	.long	1068469531
	.long	2667571783
	.long	1073188827
	.long	1614402419
	.long	3163482232
	.long	0
	.long	0
	.long	2354785698
	.long	1069102779
	.long	959196066
	.long	1012432163
	.long	0
	.long	1072693248
	.long	2828230112
	.long	1065606626
	.long	3693284264
	.long	1069118808
	.long	1996210616
	.long	1071000265
	.long	3053541878
	.long	1068578846
	.long	1133334569
	.long	1069711236
	.long	977677893
	.long	1067705915
	.long	347264115
	.long	1068436198
	.long	2824298989
	.long	1073175959
	.long	2088964722
	.long	3164313480
	.long	0
	.long	0
	.long	931121154
	.long	1069310926
	.long	1121692745
	.long	3159637885
	.long	0
	.long	1072693248
	.long	709357472
	.long	1065955296
	.long	29972165
	.long	1069333808
	.long	254360647
	.long	1071015033
	.long	3732687627
	.long	1068729157
	.long	3224370683
	.long	1069736900
	.long	202592779
	.long	1067887231
	.long	1420423314
	.long	1068491719
	.long	2981026194
	.long	1073163091
	.long	924239062
	.long	1016612139
	.long	0
	.long	0
	.long	982366697
	.long	1069519640
	.long	1160213218
	.long	3158385836
	.long	0
	.long	1072693248
	.long	404678272
	.long	1066346403
	.long	1419607756
	.long	1069549319
	.long	1800406832
	.long	1071031665
	.long	3144361055
	.long	1068882825
	.long	1744148040
	.long	1069765960
	.long	3211597674
	.long	1068075289
	.long	404952187
	.long	1068609434
	.long	3137753400
	.long	1073150223
	.long	3579918397
	.long	1015563197
	.long	0
	.long	0
	.long	2242453104
	.long	1069638253
	.long	1143545012
	.long	3159361608
	.long	0
	.long	1072693248
	.long	1016752608
	.long	1066591110
	.long	1270001991
	.long	1069659269
	.long	1410785275
	.long	1071050206
	.long	1103929722
	.long	1069040275
	.long	3268015215
	.long	1069798554
	.long	957665168
	.long	1068270980
	.long	1067662813
	.long	1068610012
	.long	3294480606
	.long	1073137355
	.long	1489607747
	.long	3151418907
	.long	0
	.long	0
	.long	1940260416
	.long	1069743276
	.long	4144715194
	.long	1012060053
	.long	0
	.long	1072693248
	.long	1135818128
	.long	1066829850
	.long	867512158
	.long	1069770656
	.long	3821819258
	.long	1071070704
	.long	2890864845
	.long	1069201948
	.long	918459419
	.long	1069834828
	.long	797472495
	.long	1068475275
	.long	4093958433
	.long	1068635569
	.long	3451207812
	.long	1073124487
	.long	1547135652
	.long	3163048307
	.long	0
	.long	0
	.long	1959743063
	.long	1069848682
	.long	607727474
	.long	1013558026
	.long	0
	.long	1072693248
	.long	225834432
	.long	1067090614
	.long	732340398
	.long	1069883626
	.long	3221152670
	.long	1071093215
	.long	2810588359
	.long	1069368307
	.long	612200458
	.long	1069874948
	.long	645427023
	.long	1068594071
	.long	1966294363
	.long	1068694214
	.long	3607935018
	.long	1073111619
	.long	4202814986
	.long	3164096517
	.long	0
	.long	0
	.long	2429422902
	.long	1069954505
	.long	3297515896
	.long	3160006592
	.long	0
	.long	1072693248
	.long	528565440
	.long	1067373727
	.long	2395999282
	.long	1069998329
	.long	1512953454
	.long	1071117799
	.long	2702142222
	.long	1069539835
	.long	3515899576
	.long	1069919100
	.long	2767905785
	.long	1068706391
	.long	3127207959
	.long	1068808800
	.long	3764662223
	.long	1073098751
	.long	3105356094
	.long	1016829101
	.long	0
	.long	0
	.long	1425758568
	.long	1070060780
	.long	1224256500
	.long	1011856982
	.long	0
	.long	1072693248
	.long	2086713664
	.long	1067564956
	.long	1688303979
	.long	1070114921
	.long	938619730
	.long	1071144522
	.long	526120622
	.long	1069632280
	.long	3819295667
	.long	1069967496
	.long	2059466202
	.long	1068824653
	.long	4035080204
	.long	1068903334
	.long	3921389429
	.long	1073085883
	.long	3647185164
	.long	1015997122
	.long	0
	.long	0
	.long	3763679576
	.long	1070167541
	.long	448465424
	.long	1009708707
	.long	0
	.long	1072693248
	.long	64931152
	.long	1067729411
	.long	3593250320
	.long	1070233561
	.long	1127364104
	.long	1071173457
	.long	21320967
	.long	1069723988
	.long	4036384112
	.long	1070020367
	.long	3394693835
	.long	1068949482
	.long	4171096556
	.long	1068939101
	.long	4078116635
	.long	1073073015
	.long	39665264
	.long	1013608617
	.long	0
	.long	0
	.long	4023957346
	.long	1070274825
	.long	2652053984
	.long	1012417284
	.long	0
	.long	1072693248
	.long	1548193280
	.long	1067905620
	.long	2849460080
	.long	1070354416
	.long	586099284
	.long	1071204684
	.long	2426038050
	.long	1069819083
	.long	1209882932
	.long	1070077962
	.long	204929667
	.long	1069081561
	.long	2951798640
	.long	1069077118
	.long	4234843841
	.long	1073060147
	.long	1479868884
	.long	3162614382
	.long	0
	.long	0
	.long	3947580658
	.long	1070382668
	.long	4153467944
	.long	1013228820
	.long	0
	.long	1072693248
	.long	1306970432
	.long	1068093810
	.long	3320658510
	.long	1070477657
	.long	2638377394
	.long	1071238289
	.long	1218095092
	.long	1069917865
	.long	506153462
	.long	1070140566
	.long	2096772755
	.long	1069221614
	.long	2058474785
	.long	1069148484
	.long	96603751
	.long	1073047280
	.long	2021697954
	.long	3163879555
	.long	0
	.long	0
	.long	4068253482
	.long	1070491107
	.long	3098804354
	.long	1013932712
	.long	0
	.long	1072693248
	.long	3555466384
	.long	1068294223
	.long	3714292293
	.long	1070599779
	.long	58004546
	.long	1071274368
	.long	2889916307
	.long	1070020649
	.long	2725223781
	.long	1070208483
	.long	1960889140
	.long	1069370437
	.long	2173066945
	.long	1069259864
	.long	253330956
	.long	1073034412
	.long	991505830
	.long	1017046064
	.long	0
	.long	0
	.long	1971725998
	.long	1070598138
	.long	2628060598
	.long	1013294571
	.long	0
	.long	1072693248
	.long	3850616508
	.long	1068503032
	.long	715485429
	.long	1070664058
	.long	2831870887
	.long	1071313021
	.long	276112813
	.long	1070127773
	.long	4071473622
	.long	1070282053
	.long	3678820497
	.long	1069528889
	.long	928174949
	.long	1069392578
	.long	410058162
	.long	1073021544
	.long	4004709614
	.long	1016250595
	.long	0
	.long	0
	.long	1197423420
	.long	1070653011
	.long	176404688
	.long	1013428940
	.long	0
	.long	1072693248
	.long	3614211808
	.long	1068615863
	.long	2638984897
	.long	1070729808
	.long	1695604536
	.long	1071354361
	.long	2372043566
	.long	1070239593
	.long	136909610
	.long	1070361652
	.long	3640039434
	.long	1069622710
	.long	4088329288
	.long	1069449448
	.long	566785368
	.long	1073008676
	.long	2301849815
	.long	1014658654
	.long	0
	.long	0
	.long	1026922473
	.long	1070708240
	.long	4275552176
	.long	1013759157
	.long	0
	.long	1072693248
	.long	865263972
	.long	1068735226
	.long	1189261043
	.long	1070797132
	.long	602895285
	.long	1071398507
	.long	457371263
	.long	1070356493
	.long	1846858239
	.long	1070447677
	.long	3060498109
	.long	1069713009
	.long	4267979072
	.long	1069596604
	.long	723512574
	.long	1072995808
	.long	2825204233
	.long	3161855698
	.long	0
	.long	0
	.long	2664093517
	.long	1070763845
	.long	3574550848
	.long	1009790086
	.long	0
	.long	1072693248
	.long	2465054196
	.long	1068861279
	.long	3495559184
	.long	1070866134
	.long	2180799467
	.long	1071445588
	.long	3760731789
	.long	1070478879
	.long	3108073741
	.long	1070540584
	.long	1955989611
	.long	1069809654
	.long	166740423
	.long	1069654305
	.long	880239780
	.long	1072982940
	.long	4135548218
	.long	3163662592
	.long	0
	.long	0
	.long	895247324
	.long	1070819848
	.long	3063999364
	.long	3161289029
	.long	0
	.long	1072693248
	.long	729983844
	.long	1068994194
	.long	3631919317
	.long	1070936926
	.long	2031343772
	.long	1071495745
	.long	1505593195
	.long	1070601643
	.long	519428998
	.long	1070618477
	.long	3710689648
	.long	1069913254
	.long	3156018952
	.long	1069741343
	.long	1036966986
	.long	1072970072
	.long	1122344434
	.long	3164458061
	.long	0
	.long	0
	.long	1412148370
	.long	1070876269
	.long	2689479793
	.long	3161477235
	.long	0
	.long	1072693248
	.long	1375255072
	.long	1069134151
	.long	1493266594
	.long	1071009624
	.long	487968719
	.long	1071549129
	.long	4010395236
	.long	1070668994
	.long	2875570533
	.long	1070672565
	.long	2596803931
	.long	1070024476
	.long	2843491938
	.long	1069843051
	.long	1193694191
	.long	1072957204
	.long	1890859349
	.long	1016467558
	.long	0
	.long	0
	.long	3782220567
	.long	1070933130
	.long	2488122455
	.long	1014753484
	.long	0
	.long	1072693248
	.long	91372748
	.long	1069281344
	.long	318776714
	.long	1071084349
	.long	1791786485
	.long	1071605902
	.long	245863368
	.long	1070739794
	.long	3306699314
	.long	1070730899
	.long	492155886
	.long	1070144051
	.long	504705110
	.long	1069961586
	.long	1350421397
	.long	1072944336
	.long	1218191675
	.long	1015274036
	.long	0
	.long	0
	.long	1606100959
	.long	1070990455
	.long	3385800912
	.long	1013630535
	.long	0
	.long	1072693248
	.long	2255302008
	.long	1069435977
	.long	54085333
	.long	1071161229
	.long	2504974906
	.long	1071655456
	.long	4169947836
	.long	1070814310
	.long	2891827400
	.long	1070793803
	.long	1030744926
	.long	1070272780
	.long	2378131433
	.long	1070036506
	.long	1507148603
	.long	1072931468
	.long	1086374101
	.long	3160519057
	.long	0
	.long	0
	.long	4158096969
	.long	1071048265
	.long	3224523546
	.long	1013419306
	.long	0
	.long	1072693248
	.long	4240313622
	.long	1069572894
	.long	3745046477
	.long	1071240398
	.long	1955120038
	.long	1071687503
	.long	2661530988
	.long	1070892836
	.long	2615081511
	.long	1070861626
	.long	3172686013
	.long	1070411548
	.long	3468537064
	.long	1070173373
	.long	1663875809
	.long	1072918600
	.long	3908862373
	.long	3163337468
	.long	0
	.long	0
	.long	2200768388
	.long	1071106586
	.long	302579070
	.long	3162432935
	.long	0
	.long	1072693248
	.long	2338248308
	.long	1069657986
	.long	2617403810
	.long	1071322000
	.long	60078074
	.long	1071721530
	.long	2753634668
	.long	1070975684
	.long	626020947
	.long	1070934758
	.long	25846105
	.long	1070561329
	.long	1123371528
	.long	1070307499
	.long	1820603015
	.long	1072905732
	.long	3236194699
	.long	3164241098
	.long	0
	.long	0
	.long	2528366626
	.long	1071165441
	.long	3808697588
	.long	3160880158
	.long	0
	.long	1072693248
	.long	1938261590
	.long	1069747146
	.long	4282399603
	.long	1071406183
	.long	1922568281
	.long	1071757646
	.long	2428166646
	.long	1071063193
	.long	717464826
	.long	1071013623
	.long	2205664064
	.long	1070659645
	.long	1525962144
	.long	1070438229
	.long	1977330220
	.long	1072892864
	.long	4071976381
	.long	1016684520
	.long	0
	.long	0
	.long	2390846448
	.long	1071224856
	.long	3510510824
	.long	1014225707
	.long	0
	.long	1072693248
	.long	1208409702
	.long	1069840505
	.long	1006199412
	.long	1071493107
	.long	2504246873
	.long	1071795971
	.long	1040653613
	.long	1071155729
	.long	1713268218
	.long	1071098686
	.long	462276291
	.long	1070747214
	.long	4239134769
	.long	1070603989
	.long	2134057426
	.long	1072879996
	.long	1285458442
	.long	1015707961
	.long	0
	.long	0
	.long	2583490354
	.long	1071284857
	.long	27154665
	.long	1014008337
	.long	0
	.long	1072693248
	.long	2550940470
	.long	1069938201
	.long	2616040293
	.long	1071582937
	.long	2529278955
	.long	1071836633
	.long	2244405849
	.long	1071253687
	.long	2064814125
	.long	1071190460
	.long	1527853242
	.long	1070842074
	.long	354304617
	.long	1070708112
	.long	2290784632
	.long	1072867128
	.long	2660353234
	.long	1011979525
	.long	0
	.long	0
	.long	3368427099
	.long	1071345471
	.long	2187847358
	.long	3161070788
	.long	0
	.long	1072693248
	.long	1708021028
	.long	1070040382
	.long	459972195
	.long	1071660262
	.long	3210514347
	.long	1071879770
	.long	955408873
	.long	1071357497
	.long	1911386086
	.long	1071289511
	.long	3563284025
	.long	1070944951
	.long	547392146
	.long	1070790031
	.long	2447511838
	.long	1072854260
	.long	3841595606
	.long	3162903543
	.long	0
	.long	0
	.long	408840118
	.long	1071406727
	.long	4048150272
	.long	1011870625
	.long	0
	.long	1072693248
	.long	2794370512
	.long	1070147203
	.long	1550993351
	.long	1071708355
	.long	1305093678
	.long	1071925532
	.long	1748211158
	.long	1071467623
	.long	3244212116
	.long	1071396455
	.long	2145665988
	.long	1071056651
	.long	1098394636
	.long	1070891096
	.long	2604239044
	.long	1072841392
	.long	1055077667
	.long	3164024136
	.long	0
	.long	0
	.long	2191706112
	.long	1071468652
	.long	333457838
	.long	1013847700
	.long	0
	.long	1072693248
	.long	3435408456
	.long	1070258830
	.long	487319719
	.long	1071758184
	.long	1713711059
	.long	1071974079
	.long	130753286
	.long	1071584571
	.long	909725129
	.long	1071511974
	.long	2762442355
	.long	1071178067
	.long	932699057
	.long	1071027067
	.long	2760966249
	.long	1072828524
	.long	1958126117
	.long	1016901483
	.long	0
	.long	0
	.long	220675706
	.long	1071531278
	.long	3158460941
	.long	1014240139
	.long	0
	.long	1072693248
	.long	1565030890
	.long	1070375439
	.long	335263687
	.long	1071809854
	.long	419816567
	.long	1072025586
	.long	2098048102
	.long	1071676780
	.long	257229334
	.long	1071636821
	.long	3464457175
	.long	1071310192
	.long	2745951745
	.long	1071158056
	.long	2917693455
	.long	1072815656
	.long	676362605
	.long	1016106015
	.long	0
	.long	0
	.long	3649427950
	.long	1071594634
	.long	4113913432
	.long	1011178545
	.long	0
	.long	1072693248
	.long	2424145432
	.long	1070497215
	.long	2715404890
	.long	1071863477
	.long	2238283673
	.long	1072080240
	.long	1186867211
	.long	1071742923
	.long	660318792
	.long	1071708248
	.long	2091427899
	.long	1071454132
	.long	2591062578
	.long	1071291190
	.long	3074420661
	.long	1072802788
	.long	1873363667
	.long	1014080331
	.long	0
	.long	0
	.long	2606398711
	.long	1071651713
	.long	2533078623
	.long	1015070425
	.long	0
	.long	1072693248
	.long	2151361100
	.long	1070610226
	.long	1532348096
	.long	1071919174
	.long	408604530
	.long	1072138246
	.long	1039538740
	.long	1071813375
	.long	1234598850
	.long	1071781284
	.long	692507560
	.long	1071611119
	.long	4090315823
	.long	1071451983
	.long	3231147867
	.long	1072789920
	.long	3253690381
	.long	3162434021
	.long	0
	.long	0
	.long	1505561933
	.long	1071684172
	.long	2813788960
	.long	3158646164
	.long	0
	.long	1072693248
	.long	2997217365
	.long	1070676584
	.long	2681788575
	.long	1071977071
	.long	3989126937
	.long	1072199822
	.long	1049115510
	.long	1071888491
	.long	511642057
	.long	1071860360
	.long	4034728042
	.long	1071713598
	.long	1788921063
	.long	1071639550
	.long	3387875073
	.long	1072777052
	.long	3168927931
	.long	3163807173
	.long	0
	.long	0
	.long	419968236
	.long	1071717047
	.long	131821922
	.long	3163445114
	.long	0
	.long	1072693248
	.long	2960267235
	.long	1070745841
	.long	1037049094
	.long	1072037305
	.long	1447436626
	.long	1072265209
	.long	3197232138
	.long	1071968658
	.long	4004685083
	.long	1071946034
	.long	1412816411
	.long	1071807280
	.long	3184767140
	.long	1071740610
	.long	3544602278
	.long	1072764184
	.long	4139243149
	.long	1017118445
	.long	0
	.long	0
	.long	4270735453
	.long	1071750355
	.long	3565353881
	.long	3162826177
	.long	0
	.long	1072693248
	.long	2915760230
	.long	1070818115
	.long	638580392
	.long	1072100020
	.long	2252948193
	.long	1072334663
	.long	1194731830
	.long	1072054301
	.long	1353002943
	.long	1072038927
	.long	422364777
	.long	1071909798
	.long	3669605864
	.long	1071842023
	.long	3701329484
	.long	1072751316
	.long	2857479637
	.long	1016322977
	.long	0
	.long	0
	.long	4010047525
	.long	1071784117
	.long	3339961616
	.long	1012589046
	.long	0
	.long	1072693248
	.long	966450668
	.long	1070893532
	.long	803989702
	.long	1072165371
	.long	1934453966
	.long	1072408465
	.long	1455077850
	.long	1072145882
	.long	2709264343
	.long	1072139721
	.long	1918064545
	.long	1072022114
	.long	1476733491
	.long	1071960574
	.long	3858056690
	.long	1072738448
	.long	2007897202
	.long	1014948181
	.long	0
	.long	0
	.long	2591355529
	.long	1071818352
	.long	2351346752
	.long	1014424505
	.long	0
	.long	1072693248
	.long	3206845590
	.long	1070972224
	.long	4023349050
	.long	1072233523
	.long	3838822434
	.long	1072486918
	.long	3122427157
	.long	1072243909
	.long	2261703628
	.long	1072249178
	.long	1764870085
	.long	1072145307
	.long	3201774398
	.long	1072093301
	.long	4014783896
	.long	1072725580
	.long	3119156846
	.long	3161566171
	.long	0
	.long	0
	.long	2298773501
	.long	1071853080
	.long	4027796943
	.long	3162574088
	.long	0
	.long	1072693248
	.long	312748411
	.long	1071054335
	.long	2723913091
	.long	1072304655
	.long	3207063211
	.long	1072570353
	.long	1845780128
	.long	1072348940
	.long	752659336
	.long	1072368142
	.long	1097207438
	.long	1072280589
	.long	407427378
	.long	1072246732
	.long	4171511102
	.long	1072712712
	.long	987810900
	.long	3163590211
	.long	0
	.long	0
	.long	681498355
	.long	1071888323
	.long	3542613740
	.long	3163311347
	.long	0
	.long	1072693248
	.long	168222332
	.long	1071140014
	.long	776736583
	.long	1072378956
	.long	2405401100
	.long	1072659128
	.long	430546584
	.long	1072461586
	.long	2974083787
	.long	1072497552
	.long	849442051
	.long	1072429322
	.long	2727422945
	.long	1072409680
	.long	33271012
	.long	1072699845
	.long	2269574412
	.long	3164385679
	.long	0
	.long	0
	.long	3684492763
	.long	1071924102
	.long	2905877726
	.long	3163193209
	.long	0
	.long	1072693248
	.long	585640690
	.long	1071229422
	.long	1068301595
	.long	1072456629
	.long	2953900489
	.long	1072723440
	.long	950237889
	.long	1072582519
	.long	109735426
	.long	1072638455
	.long	792610960
	.long	1072593041
	.long	2904161704
	.long	1072594405
	.long	379996435
	.long	1072680706
	.long	2807708551
	.long	3162614199
	.long	0
	.long	0
	.long	142288477
	.long	1071960443
	.long	1256321232
	.long	3162016428
	.long	0
	.long	1072693248
	.long	925074332
	.long	1071322730
	.long	2134728327
	.long	1072537893
	.long	1886408555
	.long	1072773770
	.long	101893267
	.long	1072702864
	.long	2020085224
	.long	1072742631
	.long	2955803851
	.long	1072733362
	.long	343310803
	.long	1072748161
	.long	693450846
	.long	1072654970
	.long	3218699017
	.long	1015418799
	.long	0
	.long	0
	.long	716700048
	.long	1071997368
	.long	1043856658
	.long	1015522810
	.long	0
	.long	1072693248
	.long	1097907398
	.long	1071420120
	.long	3912524876
	.long	1072622983
	.long	4107784306
	.long	1072827408
	.long	937056065
	.long	1072772766
	.long	4128945134
	.long	1072826390
	.long	1032262383
	.long	1072832917
	.long	1825923576
	.long	1072864423
	.long	1006905258
	.long	1072629234
	.long	3348558652
	.long	3159471942
	.long	0
	.long	0
	.long	3841229102
	.long	1072034903
	.long	1782837269
	.long	1016032992
	.long	0
	.long	1072693248
	.long	866896685
	.long	1071521786
	.long	2743435231
	.long	1072702700
	.long	3017984361
	.long	1072884608
	.long	1491919678
	.long	1072848040
	.long	1507535811
	.long	1072917857
	.long	1966124961
	.long	1072942914
	.long	381628626
	.long	1072992589
	.long	1320359670
	.long	1072603498
	.long	1908355032
	.long	3163192705
	.long	0
	.long	0
	.long	343219638
	.long	1072073077
	.long	2238302242
	.long	1014248160
	.long	0
	.long	1072693248
	.long	3768132253
	.long	1071627934
	.long	3895546569
	.long	1072749460
	.long	1613056473
	.long	1072945645
	.long	3409294908
	.long	1072929183
	.long	3138966349
	.long	1073017844
	.long	3799067273
	.long	1073064607
	.long	359470462
	.long	1073133087
	.long	1633814081
	.long	1072577762
	.long	3941137777
	.long	1014659019
	.long	0
	.long	0
	.long	3860971117
	.long	1072111915
	.long	2891637204
	.long	1015730508
	.long	0
	.long	1073741824
	.long	80822128
	.long	3219034205
	.long	2077635078
	.long	1072798543
	.long	1552870456
	.long	1073010820
	.long	3607700650
	.long	1073016746
	.long	249538440
	.long	1073127265
	.long	2233833066
	.long	1073199420
	.long	2436428201
	.long	1073293556
	.long	1947268493
	.long	1072552026
	.long	1185916272
	.long	3161855333
	.long	0
	.long	0
	.long	3978924239
	.long	1072151449
	.long	680748782
	.long	3163204529
	.long	0
	.long	1073741824
	.long	3528982175
	.long	3218918413
	.long	3018376188
	.long	1072850106
	.long	2093086652
	.long	1073080463
	.long	451029217
	.long	1073111339
	.long	1097121536
	.long	1073247142
	.long	2277789226
	.long	1073348969
	.long	149166906
	.long	1073478822
	.long	2260722904
	.long	1072526290
	.long	3285965784
	.long	1015852724
	.long	0
	.long	0
	.long	997287281
	.long	1072191710
	.long	3612277486
	.long	1015833408
	.long	0
	.long	1073741824
	.long	3037674984
	.long	3218797431
	.long	2624215131
	.long	1072904321
	.long	464779768
	.long	1073154936
	.long	3700763630
	.long	1073213636
	.long	1200926910
	.long	1073378628
	.long	2934576429
	.long	1073515096
	.long	3287041855
	.long	1073681673
	.long	2574177316
	.long	1072500554
	.long	2889755040
	.long	1013031024
	.long	0
	.long	0
	.long	2079874795
	.long	1072232729
	.long	1885505696
	.long	3161954077
	.long	0
	.long	1073741824
	.long	1656469901
	.long	3218670989
	.long	823761676
	.long	1072961373
	.long	2491445454
	.long	1073234634
	.long	1584341555
	.long	1073324391
	.long	1976688438
	.long	1073523018
	.long	3918313340
	.long	1073699902
	.long	3178717687
	.long	1073829019
	.long	2887631728
	.long	1072474818
	.long	1841088264
	.long	3162758780
	.long	0
	.long	0
	.long	258163604
	.long	1072274542
	.long	3299923807
	.long	1015128357
	.long	0
	.long	1073741824
	.long	2865277319
	.long	3218538798
	.long	3110066496
	.long	1073021461
	.long	723074889
	.long	1073319994
	.long	1618819009
	.long	1073444438
	.long	1176243936
	.long	1073681775
	.long	1223848987
	.long	1073823806
	.long	2369274010
	.long	1073957581
	.long	3201086139
	.long	1072449082
	.long	4185319304
	.long	1015274218
	.long	0
	.long	0
	.long	719595600
	.long	1072317184
	.long	114895218
	.long	3162143748
	.long	0
	.long	1073741824
	.long	3811788216
	.long	3218400550
	.long	1264738948
	.long	1073084804
	.long	1958933439
	.long	1073411493
	.long	4246310163
	.long	1073574708
	.long	1367842247
	.long	1073799186
	.long	4112596162
	.long	1073938662
	.long	2100954442
	.long	1074105254
	.long	3514540551
	.long	1072423346
	.long	2102765473
	.long	3160518326
	.long	0
	.long	0
	.long	4189730214
	.long	1072360693
	.long	873070368
	.long	1011168520
	.long	0
	.long	1073741824
	.long	760634709
	.long	3218255915
	.long	2067827628
	.long	1073151636
	.long	1386756408
	.long	1073509659
	.long	404552634
	.long	1073716242
	.long	1143062645
	.long	1073895515
	.long	555342321
	.long	1074067009
	.long	3470170865
	.long	1074271418
	.long	3827994963
	.long	1072397610
	.long	941734744
	.long	3163337286
	.long	0
	.long	0
	.long	3247184626
	.long	1072405111
	.long	1245412628
	.long	1014426137
	.long	0
	.long	1073741824
	.long	2381906911
	.long	3218104536
	.long	1010924947
	.long	1073222214
	.long	2481381005
	.long	1073615072
	.long	1356661294
	.long	1073806011
	.long	1527070582
	.long	1074001844
	.long	3061693388
	.long	1074210655
	.long	1993183195
	.long	1074463435
	.long	4141449374
	.long	1072371874
	.long	1579411055
	.long	1014369858
	.long	0
	.long	0
	.long	166164867
	.long	1072450480
	.long	2304058808
	.long	3159574198
	.long	0
	.long	1073741824
	.long	2798667120
	.long	3217812323
	.long	4203596829
	.long	1073296815
	.long	1473339147
	.long	1073728374
	.long	4243021652
	.long	1073889850
	.long	1638747017
	.long	1074119375
	.long	1512673096
	.long	1074371687
	.long	2338684787
	.long	1074678259
	.long	159936490
	.long	1072346139
	.long	3547642993
	.long	3162144494
	.long	0
	.long	0
	.long	3342355758
	.long	1072496843
	.long	1667048854
	.long	1014547708
	.long	0
	.long	1073741824
	.long	2271288998
	.long	3217480250
	.long	4111284847
	.long	1073375744
	.long	2796272966
	.long	1073796048
	.long	1730314448
	.long	1073981280
	.long	1813001552
	.long	1074249474
	.long	603251414
	.long	1074552509
	.long	3079042420
	.long	1074856525
	.long	473390901
	.long	1072320403
	.long	4252586071
	.long	1015708143
	.long	0
	.long	0
	.long	2308259791
	.long	1072544250
	.long	4116261952
	.long	1009282152
	.long	0
	.long	1073741824
	.long	411343470
	.long	3217132228
	.long	3760686807
	.long	1073459331
	.long	4171367426
	.long	1073861688
	.long	1878500541
	.long	1074081117
	.long	108767834
	.long	1074393698
	.long	2824899539
	.long	1074755901
	.long	3321235986
	.long	1074997439
	.long	786845313
	.long	1072294667
	.long	627570489
	.long	1011980987
	.long	0
	.long	0
	.long	2949191430
	.long	1072592750
	.long	3989207545
	.long	1015613028
	.long	0
	.long	1073741824
	.long	2909112164
	.long	3216503445
	.long	3682557528
	.long	1073547937
	.long	4184421718
	.long	1073932454
	.long	380803270
	.long	1074190282
	.long	3399744364
	.long	1074553823
	.long	3842864947
	.long	1074887742
	.long	748075417
	.long	1075157395
	.long	1100299725
	.long	1072268931
	.long	874467977
	.long	3162903361
	.long	0
	.long	0
	.long	2668510354
	.long	1072642397
	.long	1701265160
	.long	3159690220
	.long	0
	.long	1073741824
	.long	1404962112
	.long	3215492924
	.long	3327263454
	.long	1073641957
	.long	1987673213
	.long	1074008829
	.long	231222583
	.long	1074309811
	.long	1756671652
	.long	1074731884
	.long	3921518313
	.long	1075017100
	.long	2691193077
	.long	1075341426
	.long	1413754136
	.long	1072243195
	.long	856972295
	.long	1015129638
	.long	0
	.long	1072693248
	.long	3072795147
	.long	3218177217
	.long	2531204756
	.long	1014677625
	.long	0
	.long	1071644672
	.long	573338107
	.long	3216966390
	.long	1757081539
	.long	3216009405
	.long	1043378852
	.long	1067696161
	.long	2698850995
	.long	3213513876
	.long	495882814
	.long	1064911053
	.long	3842541443
	.long	3210988571
	.long	2725665262
	.long	1061135526
	.long	1727208548
	.long	1072217459
	.long	2531251621
	.long	3161096649
	.long	0
	.long	1072693248
	.long	963359913
	.long	3218157757
	.long	2464811394
	.long	1013284229
	.long	0
	.long	1071644672
	.long	1498613974
	.long	3216992493
	.long	4138103782
	.long	3215985771
	.long	2840947366
	.long	1067675293
	.long	209537977
	.long	3213463826
	.long	3590357530
	.long	1064864171
	.long	73959547
	.long	3210940132
	.long	1511606549
	.long	1062390487
	.long	2040662960
	.long	1072191723
	.long	4270081753
	.long	3163481866
	.long	0
	.long	1072693248
	.long	4002124759
	.long	3218138375
	.long	3453342566
	.long	3160907529
	.long	0
	.long	1071644672
	.long	774562305
	.long	3217018021
	.long	377617675
	.long	3215942442
	.long	2495478050
	.long	1067655031
	.long	1508050213
	.long	3213415186
	.long	1921336534
	.long	1064819067
	.long	975923672
	.long	3210893594
	.long	2289965811
	.long	1060505011
	.long	2354117371
	.long	1072165987
	.long	3512651629
	.long	1014080696
	.long	0
	.long	1072693248
	.long	373017919
	.long	3218119072
	.long	3647199073
	.long	3162144529
	.long	0
	.long	1070596096
	.long	933359764
	.long	1069535705
	.long	1391441088
	.long	3215896667
	.long	53106651
	.long	1067635358
	.long	4157835043
	.long	3213367904
	.long	4055868050
	.long	1064775611
	.long	1529330423
	.long	3210848884
	.long	325278885
	.long	1062325273
	.long	2667571783
	.long	1072140251
	.long	1614402419
	.long	3162433656
	.long	0
	.long	1072693248
	.long	4148643959
	.long	3218099843
	.long	2887822595
	.long	3161698583
	.long	0
	.long	1070596096
	.long	4101770849
	.long	1069511300
	.long	1167052501
	.long	3215851606
	.long	2569727718
	.long	1067616256
	.long	1681502493
	.long	3213321931
	.long	3685967947
	.long	1064733780
	.long	2973319847
	.long	3210805911
	.long	472683240
	.long	1062549181
	.long	2981026194
	.long	1072114515
	.long	924239062
	.long	1015563563
	.long	0
	.long	1072693248
	.long	3747221382
	.long	3218080689
	.long	1912303920
	.long	3161550059
	.long	0
	.long	1070596096
	.long	1727686788
	.long	1069487445
	.long	681802874
	.long	3215807238
	.long	2531789465
	.long	1067597711
	.long	331202638
	.long	3213277217
	.long	2627596737
	.long	1064693515
	.long	566979211
	.long	3210764596
	.long	2424396143
	.long	1061741710
	.long	3294480606
	.long	1072088779
	.long	1489607747
	.long	3150370331
	.long	0
	.long	1072693248
	.long	1167858038
	.long	3218043472
	.long	2180016084
	.long	3158357424
	.long	0
	.long	1070596096
	.long	772792549
	.long	1069464130
	.long	3356872770
	.long	3215763542
	.long	3524844777
	.long	1067579707
	.long	1521756467
	.long	3213233715
	.long	3931969740
	.long	1064654734
	.long	2908114613
	.long	3210710011
	.long	3523476919
	.long	1061374592
	.long	3607935018
	.long	1072063043
	.long	4202814986
	.long	3163047941
	.long	0
	.long	1072693248
	.long	955408364
	.long	3218005450
	.long	2659929740
	.long	1013295819
	.long	0
	.long	1070596096
	.long	659174847
	.long	1069441347
	.long	2081794994
	.long	3215720500
	.long	2956616392
	.long	1067562230
	.long	1461867065
	.long	3213191381
	.long	879427918
	.long	1064617384
	.long	209598599
	.long	3210633576
	.long	60948554
	.long	1061529630
	.long	3921389429
	.long	1072037307
	.long	3647185164
	.long	1014948546
	.long	0
	.long	1072693248
	.long	1526324265
	.long	3217967566
	.long	4056331296
	.long	1010311990
	.long	0
	.long	1070596096
	.long	1830915900
	.long	1069419088
	.long	602185706
	.long	3215678092
	.long	555210277
	.long	1067545266
	.long	3690136761
	.long	3213150171
	.long	4115917121
	.long	1064581405
	.long	589230607
	.long	3210560017
	.long	3446641745
	.long	1062382364
	.long	4234843841
	.long	1072011571
	.long	1479868884
	.long	3161565806
	.long	0
	.long	1072693248
	.long	2049842017
	.long	3217929817
	.long	2491067243
	.long	1013757722
	.long	0
	.long	1070596096
	.long	1431833764
	.long	1069397346
	.long	551048935
	.long	3215636299
	.long	2575917112
	.long	1067528800
	.long	1559777140
	.long	3213110045
	.long	743322344
	.long	1064546764
	.long	1974991822
	.long	3210489213
	.long	3456487690
	.long	1062460308
	.long	253330956
	.long	1071985836
	.long	991505830
	.long	1015997488
	.long	0
	.long	1072693248
	.long	1894214069
	.long	3217892200
	.long	2182661104
	.long	3159506151
	.long	0
	.long	1070596096
	.long	1869133310
	.long	1069376113
	.long	1368144043
	.long	3215595103
	.long	242783364
	.long	1067512821
	.long	3692616565
	.long	3213070961
	.long	2890683542
	.long	1064513419
	.long	2407947192
	.long	3210421028
	.long	3811448635
	.long	1060561671
	.long	566785368
	.long	1071960100
	.long	2301849815
	.long	1013610078
	.long	0
	.long	1072693248
	.long	620845047
	.long	3217854712
	.long	314567190
	.long	3160767875
	.long	0
	.long	1070596096
	.long	2198158867
	.long	1069355382
	.long	222370857
	.long	3215554487
	.long	4028822361
	.long	1067497314
	.long	3610397327
	.long	3213032882
	.long	171354848
	.long	1064481283
	.long	693369877
	.long	3210355362
	.long	164303632
	.long	1061632559
	.long	880239780
	.long	1071934364
	.long	4135548218
	.long	3162614016
	.long	0
	.long	1072693248
	.long	2273547475
	.long	3217817349
	.long	4162051772
	.long	3158862098
	.long	0
	.long	1070596096
	.long	2392983423
	.long	1069335146
	.long	526970473
	.long	3215514433
	.long	628322840
	.long	1067482270
	.long	924033682
	.long	3212995771
	.long	854746898
	.long	1064450346
	.long	4004963123
	.long	3210292086
	.long	1474471147
	.long	1060747995
	.long	1193694191
	.long	1071908628
	.long	1890859349
	.long	1015418982
	.long	0
	.long	1072693248
	.long	2488075121
	.long	3217780109
	.long	1856288899
	.long	1013767581
	.long	0
	.long	1070596096
	.long	3322930631
	.long	1069315398
	.long	3277607933
	.long	3215474924
	.long	130253043
	.long	1067467675
	.long	1057196725
	.long	3212959591
	.long	3278604843
	.long	1064420531
	.long	4197429676
	.long	3210231108
	.long	1627850508
	.long	1062399550
	.long	1507148603
	.long	1071882892
	.long	1086374101
	.long	3159470481
	.long	0
	.long	1072693248
	.long	1371601499
	.long	3217742989
	.long	3050542164
	.long	3159667536
	.long	0
	.long	1070596096
	.long	2434996648
	.long	1069296132
	.long	2688049294
	.long	3215435945
	.long	1586964757
	.long	1067453518
	.long	2102156419
	.long	3212924308
	.long	2974407249
	.long	1064391847
	.long	249545848
	.long	3210172310
	.long	118754132
	.long	1061128535
	.long	1820603015
	.long	1071857156
	.long	3236194699
	.long	3163192522
	.long	0
	.long	1072693248
	.long	3497432648
	.long	3217705985
	.long	3403982176
	.long	3159219350
	.long	0
	.long	1070596096
	.long	2322009458
	.long	1069277341
	.long	3303184211
	.long	3215397479
	.long	3079386980
	.long	1067429210
	.long	3158396776
	.long	3212889889
	.long	500122468
	.long	1064364204
	.long	3226667041
	.long	3210115604
	.long	1839377740
	.long	1062167069
	.long	2134057426
	.long	1071831420
	.long	1285458442
	.long	1014659385
	.long	0
	.long	1072693248
	.long	719981842
	.long	3217669096
	.long	3396799415
	.long	1013109014
	.long	0
	.long	1070596096
	.long	2111724540
	.long	1069259019
	.long	2754706540
	.long	3215359511
	.long	790037588
	.long	1067402587
	.long	4097286835
	.long	3212856302
	.long	2918356813
	.long	1064337614
	.long	980543884
	.long	3210060878
	.long	1759882364
	.long	3204685716
	.long	2447511838
	.long	1071805684
	.long	3841595606
	.long	3161854967
	.long	0
	.long	1072693248
	.long	2234510834
	.long	3217632317
	.long	1077830624
	.long	3156276711
	.long	0
	.long	1070596096
	.long	1741599179
	.long	1069241160
	.long	288887764
	.long	3215322026
	.long	438316229
	.long	1067376778
	.long	2074161178
	.long	3212810170
	.long	2132352512
	.long	1064311981
	.long	321000190
	.long	3210008077
	.long	2574541397
	.long	1061549566
	.long	2760966249
	.long	1071779948
	.long	1958126117
	.long	1015852907
	.long	0
	.long	1072693248
	.long	212483252
	.long	3217595647
	.long	3496985358
	.long	1013615952
	.long	0
	.long	1070596096
	.long	1939348811
	.long	1069223758
	.long	1001569645
	.long	3215285008
	.long	764537997
	.long	1067351764
	.long	2326690580
	.long	3212746141
	.long	60750538
	.long	1064270005
	.long	2745665118
	.long	3209957083
	.long	1586280401
	.long	1060807371
	.long	3074420661
	.long	1071754212
	.long	1873363667
	.long	1013031755
	.long	0
	.long	1072693248
	.long	4156511795
	.long	3217559081
	.long	1791782369
	.long	3160930544
	.long	0
	.long	1070596096
	.long	4204226438
	.long	1069206807
	.long	2895376501
	.long	3215248443
	.long	3600185398
	.long	1067327526
	.long	2087275384
	.long	3212683597
	.long	347703885
	.long	1064222543
	.long	301495371
	.long	3209907826
	.long	876688468
	.long	3206713553
	.long	3387875073
	.long	1071728476
	.long	3168927931
	.long	3162758597
	.long	0
	.long	1072693248
	.long	2240974125
	.long	3217522619
	.long	3786179077
	.long	1012998644
	.long	0
	.long	1070596096
	.long	2199065489
	.long	1069190303
	.long	1118792048
	.long	3215212318
	.long	875809397
	.long	1067304048
	.long	2927616918
	.long	3212622482
	.long	3494337727
	.long	1064176822
	.long	4219965681
	.long	3209860241
	.long	4249954841
	.long	1060574506
	.long	3701329484
	.long	1071702740
	.long	2857479637
	.long	1015274401
	.long	0
	.long	1072693248
	.long	4257115870
	.long	3217486256
	.long	39728399
	.long	3160493923
	.long	0
	.long	1070596096
	.long	3207767847
	.long	1069174239
	.long	797077063
	.long	3215176618
	.long	287980713
	.long	1067281311
	.long	1596666478
	.long	3212562743
	.long	2775364
	.long	1064132847
	.long	3823518214
	.long	3209814240
	.long	3101392498
	.long	1060290467
	.long	4014783896
	.long	1071677004
	.long	3119156846
	.long	3160517595
	.long	0
	.long	1072693248
	.long	2953879462
	.long	3217449991
	.long	651182982
	.long	3159737324
	.long	0
	.long	1070596096
	.long	2348802841
	.long	1069158611
	.long	4095204310
	.long	3215141329
	.long	3730066556
	.long	1067259298
	.long	2541564470
	.long	3212504327
	.long	1012137407
	.long	1064090541
	.long	3007493349
	.long	3209769752
	.long	304719606
	.long	1058732349
	.long	33271012
	.long	1071651269
	.long	2269574412
	.long	3163337103
	.long	0
	.long	1072693248
	.long	4098310206
	.long	3217413820
	.long	678867490
	.long	1013738598
	.long	0
	.long	1070596096
	.long	4033975882
	.long	1069143413
	.long	1577500332
	.long	3215106440
	.long	2904242360
	.long	1067237995
	.long	383406633
	.long	3212447185
	.long	511745182
	.long	1064049810
	.long	3472404602
	.long	3209726723
	.long	3493824410
	.long	1061932631
	.long	693450846
	.long	1071606394
	.long	3218699017
	.long	1014370223
	.long	0
	.long	1072693248
	.long	701484222
	.long	3217377742
	.long	1189134796
	.long	1013549080
	.long	0
	.long	1070596096
	.long	478132658
	.long	1069128642
	.long	2223654597
	.long	3215071936
	.long	706118045
	.long	1067217386
	.long	1066336370
	.long	3212391267
	.long	3310393795
	.long	1064010705
	.long	1920669086
	.long	3209678984
	.long	1209448644
	.long	3206743753
	.long	1320359670
	.long	1071554922
	.long	1908355032
	.long	3162144129
	.long	0
	.long	1072693248
	.long	3079104278
	.long	3217341752
	.long	2593237472
	.long	3159688198
	.long	0
	.long	1070596096
	.long	1749059456
	.long	1069114291
	.long	3316773722
	.long	3215037805
	.long	2555119580
	.long	1067197455
	.long	59306268
	.long	3212336527
	.long	1944001738
	.long	1063973040
	.long	3607966944
	.long	3209598336
	.long	1548002341
	.long	1061515569
	.long	1947268493
	.long	1071503450
	.long	1185916272
	.long	3160806757
	.long	0
	.long	1072693248
	.long	192708373
	.long	3217305850
	.long	1013564964
	.long	3160609117
	.long	0
	.long	1070596096
	.long	1393443609
	.long	1069100357
	.long	3282624502
	.long	3215004035
	.long	3423023493
	.long	1067178189
	.long	3539283610
	.long	3212282918
	.long	1358653293
	.long	1063936902
	.long	1383848795
	.long	3209520160
	.long	1170879540
	.long	3207439782
	.long	2574177316
	.long	1071451978
	.long	2889755040
	.long	1011982448
	.long	0
	.long	1072693248
	.long	2595342993
	.long	3217270031
	.long	1222182906
	.long	3159411259
	.long	0
	.long	1070596096
	.long	2192927435
	.long	1069086835
	.long	1645422722
	.long	3214970614
	.long	648269084
	.long	1067159575
	.long	2319737250
	.long	3212230398
	.long	1121494038
	.long	1063902126
	.long	897314833
	.long	3209444434
	.long	2659165416
	.long	1060259181
	.long	3201086139
	.long	1071400506
	.long	4185319304
	.long	1014225642
	.long	0
	.long	1072693248
	.long	3772940467
	.long	3217234294
	.long	3998164722
	.long	1013347606
	.long	0
	.long	1070596096
	.long	3265990537
	.long	1069073721
	.long	3279971426
	.long	3214937529
	.long	2447085124
	.long	1067141598
	.long	1649985592
	.long	3212178923
	.long	3334399491
	.long	1063868754
	.long	454991634
	.long	3209370985
	.long	1938913543
	.long	3204215126
	.long	3827994963
	.long	1071349034
	.long	941734744
	.long	3162288710
	.long	0
	.long	1072693248
	.long	1615317960
	.long	3217198637
	.long	1400808192
	.long	1007479474
	.long	0
	.long	1070596096
	.long	55251407
	.long	1069061012
	.long	2380564598
	.long	3214875524
	.long	2078095393
	.long	1067124247
	.long	3759475360
	.long	3212128451
	.long	800747756
	.long	1063836662
	.long	3361780736
	.long	3209299766
	.long	3643416391
	.long	1062249457
	.long	159936490
	.long	1071297563
	.long	3547642993
	.long	3161095918
	.long	0
	.long	1072693248
	.long	2707383656
	.long	3217163056
	.long	554608178
	.long	3160521551
	.long	0
	.long	1070596096
	.long	1495139838
	.long	1069048702
	.long	183291521
	.long	3214810633
	.long	3540584774
	.long	1067107509
	.long	3317896890
	.long	3212078943
	.long	3844035447
	.long	1063805952
	.long	3695033041
	.long	3209230570
	.long	3731872264
	.long	3207727795
	.long	786845313
	.long	1071246091
	.long	627570489
	.long	1010932411
	.long	0
	.long	1072693248
	.long	850612498
	.long	3217127550
	.long	3957837480
	.long	1012739461
	.long	0
	.long	1070596096
	.long	4230382869
	.long	1069036788
	.long	2678394493
	.long	3214746346
	.long	42048419
	.long	1067091374
	.long	61549345
	.long	3212030360
	.long	3326100968
	.long	1063776440
	.long	377176410
	.long	3209163411
	.long	1539171840
	.long	3206122484
	.long	1413754136
	.long	1071194619
	.long	856972295
	.long	1014081062
	.long	0
	.long	1072693248
	.long	2829232582
	.long	3217092115
	.long	1037840994
	.long	1012412582
	.long	0
	.long	1070596096
	.long	1194701220
	.long	1069025268
	.long	1485063562
	.long	3214682643
	.long	3996610205
	.long	1067075828
	.long	2865183303
	.long	3211982662
	.long	2185691626
	.long	1063748117
	.long	435906073
	.long	3209098183
	.long	1583986254
	.long	1061922957
	.long	2040662960
	.long	1071143147
	.long	4270081753
	.long	3162433290
	.long	0
	.long	1072693248
	.long	2636871548
	.long	3217056750
	.long	3635907996
	.long	3159234439
	.long	0
	.long	1070596096
	.long	3074865967
	.long	1069014136
	.long	2827594877
	.long	3214619501
	.long	4018616278
	.long	1067060863
	.long	3744630852
	.long	3211935814
	.long	967547233
	.long	1063721046
	.long	1523268333
	.long	3209034706
	.long	370739821
	.long	1057912410
	.long	2667571783
	.long	1071091675
	.long	1614402419
	.long	3161385080
	.long	0
	.long	1072693248
	.long	1600848691
	.long	3217011737
	.long	3799011534
	.long	3158825865
	.long	0
	.long	1070596096
	.long	3940625627
	.long	1069003390
	.long	1694054617
	.long	3214556900
	.long	3525212904
	.long	1067046468
	.long	3400170401
	.long	3211889780
	.long	2025409789
	.long	1063695095
	.long	1861695371
	.long	3208972985
	.long	2099431114
	.long	1058280175
	.long	3294480606
	.long	1071040203
	.long	1489607747
	.long	3149321755
	.long	0
	.long	1072693248
	.long	163325898
	.long	3216941271
	.long	272531312
	.long	3158872717
	.long	0
	.long	1070596096
	.long	2709640876
	.long	1068993027
	.long	3241569416
	.long	3214494818
	.long	3499770285
	.long	1067032633
	.long	2428916378
	.long	3211844525
	.long	796870242
	.long	1063670282
	.long	2401385551
	.long	3208912885
	.long	417337253
	.long	3207953615
	.long	3921389429
	.long	1070988731
	.long	3647185164
	.long	1013899970
	.long	0
	.long	1072693248
	.long	2357811071
	.long	3216870929
	.long	1815527340
	.long	3157476592
	.long	0
	.long	1070596096
	.long	3138003653
	.long	1068983043
	.long	1253835798
	.long	3214433236
	.long	2444914170
	.long	1067019349
	.long	1113961907
	.long	3211800015
	.long	1725681990
	.long	1063646503
	.long	2521427548
	.long	3208854407
	.long	2469316955
	.long	1061668474
	.long	253330956
	.long	1070937260
	.long	991505830
	.long	1014948912
	.long	0
	.long	1072693248
	.long	1152452440
	.long	3216800708
	.long	2988871182
	.long	1011946816
	.long	0
	.long	1070596096
	.long	926263936
	.long	1068973436
	.long	3845070713
	.long	3214372132
	.long	626397172
	.long	1067006607
	.long	2777085683
	.long	3211724145
	.long	2172777456
	.long	1063623822
	.long	3188732906
	.long	3208797386
	.long	250185439
	.long	1062033866
	.long	880239780
	.long	1070885788
	.long	4135548218
	.long	3161565440
	.long	0
	.long	1072693248
	.long	2555242223
	.long	3216730602
	.long	1132402578
	.long	1012350135
	.long	0
	.long	1070596096
	.long	890626350
	.long	1068964202
	.long	3331100648
	.long	3214311488
	.long	30508518
	.long	1066994398
	.long	397665884
	.long	3211637908
	.long	1048931291
	.long	1063602233
	.long	4152525626
	.long	3208741715
	.long	51864355
	.long	3208005790
	.long	1507148603
	.long	1070834316
	.long	1086374101
	.long	3158421905
	.long	0
	.long	1072693248
	.long	4132779814
	.long	3216660607
	.long	3895880616
	.long	1012226511
	.long	0
	.long	1070596096
	.long	4069764223
	.long	1068955338
	.long	1936654463
	.long	3214251284
	.long	27137259
	.long	1066982714
	.long	112447739
	.long	3211552967
	.long	3681308921
	.long	1063581561
	.long	3611917249
	.long	3208687484
	.long	2182292750
	.long	1061868473
	.long	2134057426
	.long	1070782844
	.long	1285458442
	.long	1013610809
	.long	0
	.long	1072693248
	.long	3593905368
	.long	3216590719
	.long	2989246530
	.long	1011901384
	.long	0
	.long	1070596096
	.long	3421947019
	.long	1068946843
	.long	3144465174
	.long	3214191500
	.long	1618007405
	.long	1066971547
	.long	584787122
	.long	3211469261
	.long	1465845707
	.long	1063561956
	.long	939979903
	.long	3208626315
	.long	1695262346
	.long	3204506663
	.long	2760966249
	.long	1070731372
	.long	1958126117
	.long	1014804331
	.long	0
	.long	1072693248
	.long	3078460826
	.long	3216520933
	.long	4285081068
	.long	3159473614
	.long	0
	.long	1070596096
	.long	997373752
	.long	1068938714
	.long	3045840714
	.long	3214132118
	.long	3110377520
	.long	1066960890
	.long	2323415564
	.long	3211386730
	.long	18309569
	.long	1063543250
	.long	551124926
	.long	3208522770
	.long	249182975
	.long	1061811122
	.long	3387875073
	.long	1070679900
	.long	3168927931
	.long	3161710021
	.long	0
	.long	1072693248
	.long	561239310
	.long	3216451245
	.long	210497392
	.long	3156201161
	.long	0
	.long	1070596096
	.long	3636030687
	.long	1068930947
	.long	3167463735
	.long	3214073119
	.long	73152643
	.long	1066950737
	.long	3108019154
	.long	3211305316
	.long	2100498319
	.long	1063525559
	.long	282708930
	.long	3208421353
	.long	2551775895
	.long	3207605450
	.long	4014783896
	.long	1070628428
	.long	3119156846
	.long	3159469019
	.long	0
	.long	1072693248
	.long	2730856872
	.long	3216381649
	.long	4254293712
	.long	1009711154
	.long	0
	.long	1070596096
	.long	1486032260
	.long	1068923542
	.long	1824674436
	.long	3214014485
	.long	2488677748
	.long	1066941079
	.long	1925770461
	.long	3211224962
	.long	2531500940
	.long	1063508710
	.long	3007508061
	.long	3208322267
	.long	994458799
	.long	1061806090
	.long	693450846
	.long	1070557818
	.long	3218699017
	.long	1013321647
	.long	0
	.long	1072693248
	.long	3508967486
	.long	3216312142
	.long	3021155724
	.long	1011969550
	.long	0
	.long	1070596096
	.long	2356881417
	.long	1068916495
	.long	2655870529
	.long	3213956197
	.long	352865498
	.long	1066931912
	.long	3008575053
	.long	3211145611
	.long	2622088932
	.long	1063492845
	.long	3834913422
	.long	3208225014
	.long	1497815285
	.long	3207511002
	.long	1947268493
	.long	1070454874
	.long	1185916272
	.long	3159758181
	.long	0
	.long	1072693248
	.long	1224261120
	.long	3216242720
	.long	3122470664
	.long	3159031070
	.long	0
	.long	1070596096
	.long	1648557593
	.long	1068909805
	.long	1683250226
	.long	3213898238
	.long	2306165388
	.long	1066923228
	.long	2600821843
	.long	3211067209
	.long	3406795398
	.long	1063477798
	.long	3582359304
	.long	3208129845
	.long	3199024961
	.long	1061038571
	.long	3201086139
	.long	1070351930
	.long	4185319304
	.long	1013177066
	.long	0
	.long	1072693248
	.long	2901633254
	.long	3216173377
	.long	2788221280
	.long	1009319756
	.long	0
	.long	1070596096
	.long	3820542338
	.long	1068903469
	.long	288942435
	.long	3213795740
	.long	1237884341
	.long	1066915023
	.long	1606294444
	.long	3210989702
	.long	223992189
	.long	1063463656
	.long	1499707213
	.long	3208036378
	.long	3416758850
	.long	1060053511
	.long	159936490
	.long	1070248987
	.long	3547642993
	.long	3160047342
	.long	0
	.long	1072693248
	.long	2781619933
	.long	3216104110
	.long	3145354644
	.long	1011963772
	.long	0
	.long	1070596096
	.long	911495412
	.long	1068897487
	.long	290976546
	.long	3213681030
	.long	33241894
	.long	1066907291
	.long	977031819
	.long	3210913037
	.long	119357659
	.long	1063450345
	.long	1846248446
	.long	3207944648
	.long	4276662282
	.long	1061269448
	.long	1413754136
	.long	1070146043
	.long	856972295
	.long	1013032486
	.long	0
	.long	1072693248
	.long	3789571175
	.long	3216034914
	.long	2061677164
	.long	3158802525
	.long	0
	.long	1070596096
	.long	2598855658
	.long	1068891855
	.long	628750565
	.long	3213566872
	.long	2954249465
	.long	1066900026
	.long	1468236324
	.long	3210837162
	.long	3701994667
	.long	1063437912
	.long	310421843
	.long	3207854373
	.long	393637210
	.long	3207421006
	.long	2667571783
	.long	1070043099
	.long	1614402419
	.long	3160336504
	.long	0
	.long	1072693248
	.long	700360492
	.long	3215948980
	.long	4067889956
	.long	3158309941
	.long	0
	.long	1070596096
	.long	1834243786
	.long	1068886573
	.long	92010622
	.long	3213453232
	.long	2733460853
	.long	1066893225
	.long	3349627764
	.long	3210762026
	.long	2326014508
	.long	1063426279
	.long	2491557920
	.long	3207765637
	.long	2159546056
	.long	3208108974
	.long	3921389429
	.long	1069940155
	.long	3647185164
	.long	1012851394
	.long	0
	.long	1072693248
	.long	4008519973
	.long	3215810847
	.long	1156806192
	.long	3157002565
	.long	0
	.long	1070596096
	.long	608713188
	.long	1068881639
	.long	3200221055
	.long	3213340075
	.long	2024185290
	.long	1066886883
	.long	3737414605
	.long	3210635448
	.long	4102026093
	.long	1063415403
	.long	3274310324
	.long	3207678452
	.long	4280744879
	.long	1061910091
	.long	880239780
	.long	1069837212
	.long	4135548218
	.long	3160516864
	.long	0
	.long	1072693248
	.long	2381996637
	.long	3215672832
	.long	639270832
	.long	1008532593
	.long	0
	.long	1070596096
	.long	1358563144
	.long	1068877051
	.long	2925458633
	.long	3213227369
	.long	492833071
	.long	1066880996
	.long	4104628456
	.long	3210487835
	.long	3054991409
	.long	1063405423
	.long	2594255061
	.long	3207590366
	.long	2100320188
	.long	3208479772
	.long	2134057426
	.long	1069734268
	.long	1285458442
	.long	1012562233
	.long	0
	.long	1072693248
	.long	2319612510
	.long	3215534925
	.long	1111071278
	.long	3158888695
	.long	0
	.long	1070596096
	.long	2666415304
	.long	1068872808
	.long	2071510147
	.long	3213115080
	.long	3395467108
	.long	1066875559
	.long	2423946868
	.long	3210341405
	.long	80612299
	.long	1063396131
	.long	2656867111
	.long	3207420929
	.long	3004001956
	.long	1061836610
	.long	3387875073
	.long	1069631324
	.long	3168927931
	.long	3160661445
	.long	0
	.long	1072693248
	.long	1865079417
	.long	3215397118
	.long	2937833612
	.long	1011350737
	.long	0
	.long	1070596096
	.long	3552516355
	.long	1068868909
	.long	294617109
	.long	3213003175
	.long	76221622
	.long	1066870571
	.long	3376425735
	.long	3210196061
	.long	2723275475
	.long	1063387705
	.long	3315521969
	.long	3207252871
	.long	3590370444
	.long	3207679867
	.long	693450846
	.long	1069509242
	.long	3218699017
	.long	1012273071
	.long	0
	.long	1072693248
	.long	3481201957
	.long	3215259402
	.long	1636311970
	.long	3157709699
	.long	0
	.long	1070596096
	.long	3471360849
	.long	1068865353
	.long	2600335524
	.long	3212891620
	.long	2613792462
	.long	1066866026
	.long	3847698423
	.long	3210051709
	.long	663967578
	.long	1063380003
	.long	493409710
	.long	3207087119
	.long	1127477260
	.long	3207199264
	.long	3201086139
	.long	1069303354
	.long	4185319304
	.long	1012128490
	.long	0
	.long	1072693248
	.long	1154356116
	.long	3215121770
	.long	1840040117
	.long	3159097207
	.long	0
	.long	1070596096
	.long	2308597781
	.long	1068862139
	.long	4143524575
	.long	3212723904
	.long	2850915343
	.long	1066861923
	.long	1191668407
	.long	3209908256
	.long	2952843839
	.long	1063373033
	.long	3172237721
	.long	3206923432
	.long	2684115468
	.long	1061237060
	.long	1413754136
	.long	1069097467
	.long	856972295
	.long	1011983910
	.long	0
	.long	1072693248
	.long	1858778712
	.long	3214984212
	.long	552307712
	.long	1007653492
	.long	0
	.long	1070596096
	.long	378217452
	.long	1068859266
	.long	2507068679
	.long	3212502004
	.long	2446302718
	.long	1066858259
	.long	1361087111
	.long	3209765608
	.long	761263112
	.long	1063366881
	.long	2488333367
	.long	3206760542
	.long	3134826447
	.long	3208438003
	.long	3921389429
	.long	1068891579
	.long	3647185164
	.long	1011802818
	.long	0
	.long	1072693248
	.long	3847531748
	.long	3214759425
	.long	2484055616
	.long	1005679197
	.long	0
	.long	1070596096
	.long	2714982805
	.long	1068856732
	.long	2649074978
	.long	3212280611
	.long	4275478210
	.long	1066855031
	.long	2325700054
	.long	3209556212
	.long	3568629394
	.long	1063361434
	.long	1419097007
	.long	3206599479
	.long	3667282369
	.long	3208404843
	.long	2134057426
	.long	1068685692
	.long	1285458442
	.long	1011513657
	.long	0
	.long	1072693248
	.long	4111272116
	.long	3214484558
	.long	3215369808
	.long	3153450047
	.long	0
	.long	1070596096
	.long	1892298670
	.long	1068854538
	.long	4017792039
	.long	3212059661
	.long	3825916209
	.long	1066852238
	.long	1674419437
	.long	3209273590
	.long	2761556401
	.long	1063356717
	.long	4123887123
	.long	3206334117
	.long	39803485
	.long	3207166447
	.long	693450846
	.long	1068460666
	.long	3218699017
	.long	1011224495
	.long	0
	.long	1072693248
	.long	1769817487
	.long	3214209791
	.long	226900428
	.long	3157084142
	.long	0
	.long	1070596096
	.long	3790025843
	.long	1068852682
	.long	2975452701
	.long	3211839091
	.long	2074002222
	.long	1066849878
	.long	2835406423
	.long	3208992033
	.long	2160688582
	.long	1063352737
	.long	438342636
	.long	3206016666
	.long	598064701
	.long	3205441692
	.long	1413754136
	.long	1068048891
	.long	856972295
	.long	1010935334
	.long	0
	.long	1072693248
	.long	2796861084
	.long	3213935106
	.long	4160910146
	.long	1008936729
	.long	0
	.long	1070596096
	.long	1822957307
	.long	1068851165
	.long	2426092900
	.long	3211449386
	.long	1181651297
	.long	1066847949
	.long	11113416
	.long	3208711363
	.long	2350522541
	.long	1063349490
	.long	1110749705
	.long	3205700769
	.long	1529834450
	.long	3204806002
	.long	2134057426
	.long	1067637116
	.long	1285458442
	.long	1010465081
	.long	0
	.long	1072693248
	.long	766684177
	.long	3213435536
	.long	4183922317
	.long	3156428395
	.long	0
	.long	1070596096
	.long	2709169804
	.long	1068849985
	.long	2562652885
	.long	3211009382
	.long	194304310
	.long	1066846450
	.long	285423324
	.long	3208220240
	.long	4164698302
	.long	1063346958
	.long	490316583
	.long	3205276877
	.long	1161999017
	.long	1059948714
	.long	1413754136
	.long	1067000315
	.long	856972295
	.long	1009886758
	.long	0
	.long	1072693248
	.long	1373484687
	.long	3212886398
	.long	3463806808
	.long	3156987378
	.long	0
	.long	1070596096
	.long	699034059
	.long	1068849143
	.long	4196075177
	.long	3210399801
	.long	3624794075
	.long	1066845379
	.long	1212360239
	.long	3207661374
	.long	4225594870
	.long	1063345136
	.long	4142831282
	.long	3204654696
	.long	3419780486
	.long	1061860029
	.long	1413754136
	.long	1065951739
	.long	856972295
	.long	1008838182
	.long	0
	.long	1072693248
	.long	1048210722
	.long	3211837789
	.long	322286546
	.long	3154320084
	.long	0
	.long	1070596096
	.long	3344097458
	.long	1068848637
	.long	4078525744
	.long	3209350973
	.long	4268457311
	.long	1066844737
	.long	1752199129
	.long	3206612445
	.long	2933973771
	.long	1063344076
	.long	1552329397
	.long	3203604713
	.long	4286470131
	.long	1060555865
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1070596096
	.long	1431655765
	.long	1068848469
	.long	0
	.long	0
	.long	381986942
	.long	1066844524
	.long	0
	.long	0
	.long	3364860459
	.long	1063343704
	.long	0
	.long	0
	.long	3669941704
	.long	1061551801
	.long	1413754136
	.long	3213435387
	.long	856972295
	.long	3156321830
	.long	0
	.long	1072693248
	.long	1048210722
	.long	1064354141
	.long	310222654
	.long	1006836436
	.long	0
	.long	1070596096
	.long	3344097458
	.long	1068848637
	.long	4078525744
	.long	1061867325
	.long	4268457311
	.long	1066844737
	.long	1752199185
	.long	1059128797
	.long	2933974085
	.long	1063344076
	.long	1541627965
	.long	1056121065
	.long	4213857106
	.long	1060555865
	.long	1413754136
	.long	3214483963
	.long	856972295
	.long	3157370406
	.long	0
	.long	1072693248
	.long	1373484687
	.long	1065402750
	.long	3463778643
	.long	1009503730
	.long	0
	.long	1070596096
	.long	699034059
	.long	1068849143
	.long	4196075177
	.long	1062916153
	.long	3624794075
	.long	1066845379
	.long	1212360238
	.long	1060177726
	.long	4225594899
	.long	1063345136
	.long	4143059094
	.long	1057171048
	.long	3416473992
	.long	1061860029
	.long	2134057426
	.long	3215120764
	.long	1285458442
	.long	3157948729
	.long	0
	.long	1072693248
	.long	766684177
	.long	1065951888
	.long	4184103028
	.long	1008944747
	.long	0
	.long	1070596096
	.long	2709169804
	.long	1068849985
	.long	2562652885
	.long	1063525734
	.long	194304310
	.long	1066846450
	.long	285423316
	.long	1060736592
	.long	4164698149
	.long	1063346958
	.long	492080240
	.long	1057793229
	.long	1233082996
	.long	1059948714
	.long	1413754136
	.long	3215532539
	.long	856972295
	.long	3158418982
	.long	0
	.long	1072693248
	.long	2796861084
	.long	1066451458
	.long	4163800696
	.long	3156420377
	.long	0
	.long	1070596096
	.long	1822957307
	.long	1068851165
	.long	2426092900
	.long	1063965738
	.long	1181651297
	.long	1066847949
	.long	11113467
	.long	1061227715
	.long	2350524377
	.long	1063349490
	.long	1099629520
	.long	1058217121
	.long	647421766
	.long	3204806003
	.long	693450846
	.long	3215944314
	.long	3218699017
	.long	3158708143
	.long	0
	.long	1072693248
	.long	1769817487
	.long	1066726143
	.long	226750770
	.long	1009600494
	.long	0
	.long	1070596096
	.long	3790025843
	.long	1068852682
	.long	2975452701
	.long	1064355443
	.long	2074002222
	.long	1066849878
	.long	2835406424
	.long	1061508385
	.long	2160688720
	.long	1063352737
	.long	438143169
	.long	1058533018
	.long	848462310
	.long	3205441692
	.long	2134057426
	.long	3216169340
	.long	1285458442
	.long	3158997305
	.long	0
	.long	1072693248
	.long	4111272116
	.long	1067000910
	.long	3213429568
	.long	1005966399
	.long	0
	.long	1070596096
	.long	1892298670
	.long	1068854538
	.long	4017792039
	.long	1064576013
	.long	3825916209
	.long	1066852238
	.long	1674419438
	.long	1061789942
	.long	2761556545
	.long	1063356717
	.long	4123711083
	.long	1058850469
	.long	104595699
	.long	3207166447
	.long	3921389429
	.long	3216375227
	.long	3647185164
	.long	3159286466
	.long	0
	.long	1072693248
	.long	3847531748
	.long	1067275777
	.long	2487971824
	.long	3153162845
	.long	0
	.long	1070596096
	.long	2714982805
	.long	1068856732
	.long	2649074978
	.long	1064796963
	.long	4275478210
	.long	1066855031
	.long	2325700056
	.long	1062072564
	.long	3568629551
	.long	1063361434
	.long	1418922081
	.long	1059115831
	.long	3702148884
	.long	3208404843
	.long	1413754136
	.long	3216581115
	.long	856972295
	.long	3159467558
	.long	0
	.long	1072693248
	.long	1858778712
	.long	1067500564
	.long	554450480
	.long	3155137140
	.long	0
	.long	1070596096
	.long	378217452
	.long	1068859266
	.long	2507068679
	.long	1065018356
	.long	2446302718
	.long	1066858259
	.long	1361087112
	.long	1062281960
	.long	761263211
	.long	1063366881
	.long	2488165471
	.long	1059276894
	.long	3156241770
	.long	3208438003
	.long	3201086139
	.long	3216787002
	.long	4185319304
	.long	3159612138
	.long	0
	.long	1072693248
	.long	1154356116
	.long	1067638122
	.long	1840130764
	.long	1011613559
	.long	0
	.long	1070596096
	.long	2308597781
	.long	1068862139
	.long	4143524575
	.long	1065240256
	.long	2850915343
	.long	1066861923
	.long	1191668406
	.long	1062424608
	.long	2952843813
	.long	1063373033
	.long	3172428186
	.long	1059439784
	.long	2686776959
	.long	1061237060
	.long	693450846
	.long	3216992890
	.long	3218699017
	.long	3159756719
	.long	0
	.long	1072693248
	.long	3481201957
	.long	1067775754
	.long	1635996196
	.long	1010226051
	.long	0
	.long	1070596096
	.long	3471360849
	.long	1068865353
	.long	2600335524
	.long	1065407972
	.long	2613792462
	.long	1066866026
	.long	3847698425
	.long	1062568061
	.long	663967590
	.long	1063380003
	.long	493016801
	.long	1059603471
	.long	1131084659
	.long	3207199264
	.long	3387875073
	.long	3217114972
	.long	3168927931
	.long	1013177797
	.long	0
	.long	1072693248
	.long	1865079417
	.long	1067913470
	.long	2938003329
	.long	3158834385
	.long	0
	.long	1070596096
	.long	3552516355
	.long	1068868909
	.long	294617109
	.long	1065519527
	.long	76221622
	.long	1066870571
	.long	3376425737
	.long	1062712413
	.long	2723275461
	.long	1063387705
	.long	3315136619
	.long	1059769223
	.long	3585984604
	.long	3207679867
	.long	2134057426
	.long	3217217916
	.long	1285458442
	.long	3160045881
	.long	0
	.long	1072693248
	.long	2319612510
	.long	1068051277
	.long	1111303258
	.long	1011405047
	.long	0
	.long	1070596096
	.long	2666415304
	.long	1068872808
	.long	2071510147
	.long	1065631432
	.long	3395467108
	.long	1066875559
	.long	2423946865
	.long	1062857757
	.long	80612341
	.long	1063396131
	.long	2657457026
	.long	1059937281
	.long	2998464421
	.long	1061836610
	.long	880239780
	.long	3217320860
	.long	4135548218
	.long	1013033216
	.long	0
	.long	1072693248
	.long	2381996637
	.long	1068189184
	.long	639224640
	.long	3156016241
	.long	0
	.long	1070596096
	.long	1358563144
	.long	1068877051
	.long	2925458633
	.long	1065743721
	.long	492833071
	.long	1066880996
	.long	4104628456
	.long	1063004187
	.long	3054991518
	.long	1063405423
	.long	2594252722
	.long	1060106718
	.long	2125241622
	.long	3208479772
	.long	3921389429
	.long	3217423803
	.long	3647185164
	.long	3160335042
	.long	0
	.long	1072693248
	.long	4008519973
	.long	1068327199
	.long	1156786808
	.long	1009518917
	.long	0
	.long	1070596096
	.long	608713188
	.long	1068881639
	.long	3200221055
	.long	1065856427
	.long	2024185290
	.long	1066886883
	.long	3737414605
	.long	1063151800
	.long	4102026005
	.long	1063415403
	.long	3274311301
	.long	1060194804
	.long	4290805031
	.long	1061910091
	.long	2667571783
	.long	3217526747
	.long	1614402419
	.long	1012852856
	.long	0
	.long	1072693248
	.long	700360492
	.long	1068465332
	.long	4067897658
	.long	1010826293
	.long	0
	.long	1070596096
	.long	1834243786
	.long	1068886573
	.long	92010622
	.long	1065969584
	.long	2733460853
	.long	1066893225
	.long	3349627764
	.long	1063278378
	.long	2326014613
	.long	1063426279
	.long	2491557128
	.long	1060281989
	.long	2183550903
	.long	3208108974
	.long	1413754136
	.long	3217629691
	.long	856972295
	.long	3160516134
	.long	0
	.long	1072693248
	.long	3789571175
	.long	1068551266
	.long	2061680020
	.long	1011318877
	.long	0
	.long	1070596096
	.long	2598855658
	.long	1068891855
	.long	628750565
	.long	1066083224
	.long	2954249465
	.long	1066900026
	.long	1468236324
	.long	1063353514
	.long	3701994745
	.long	1063437912
	.long	310421258
	.long	1060370725
	.long	429301190
	.long	3207421006
	.long	159936490
	.long	3217732635
	.long	3547642993
	.long	1012563694
	.long	0
	.long	1072693248
	.long	2781619933
	.long	1068620462
	.long	3145355490
	.long	3159447420
	.long	0
	.long	1070596096
	.long	911495412
	.long	1068897487
	.long	290976546
	.long	1066197382
	.long	33241894
	.long	1066907291
	.long	977031819
	.long	1063429389
	.long	119357612
	.long	1063450345
	.long	1846248792
	.long	1060461000
	.long	4282034711
	.long	1061269448
	.long	3201086139
	.long	3217835578
	.long	4185319304
	.long	3160660714
	.long	0
	.long	1072693248
	.long	2901633254
	.long	1068689729
	.long	2788220944
	.long	3156803404
	.long	0
	.long	1070596096
	.long	3820542338
	.long	1068903469
	.long	288942435
	.long	1066312092
	.long	1237884341
	.long	1066915023
	.long	1606294444
	.long	1063506054
	.long	223992191
	.long	1063463656
	.long	1499707196
	.long	1060552730
	.long	3415844307
	.long	1060053511
	.long	1947268493
	.long	3217938522
	.long	1185916272
	.long	1012274533
	.long	0
	.long	1072693248
	.long	1224261120
	.long	1068759072
	.long	3122469562
	.long	1011547422
	.long	0
	.long	1070596096
	.long	1648557593
	.long	1068909805
	.long	1683250226
	.long	1066414590
	.long	2306165388
	.long	1066923228
	.long	2600821843
	.long	1063583561
	.long	3406795366
	.long	1063477798
	.long	3582359528
	.long	1060646197
	.long	3206340638
	.long	1061038571
	.long	693450846
	.long	3218041466
	.long	3218699017
	.long	3160805295
	.long	0
	.long	1072693248
	.long	3508967486
	.long	1068828494
	.long	3021154406
	.long	3159453198
	.long	0
	.long	1070596096
	.long	2356881417
	.long	1068916495
	.long	2655870529
	.long	1066472549
	.long	352865498
	.long	1066931912
	.long	3008575053
	.long	1063661963
	.long	2622089007
	.long	1063492845
	.long	3834912888
	.long	1060741366
	.long	1532107502
	.long	3207511002
	.long	4014783896
	.long	3218112076
	.long	3119156846
	.long	1011985371
	.long	0
	.long	1072693248
	.long	2730856872
	.long	1068898001
	.long	4254304408
	.long	3157194802
	.long	0
	.long	1070596096
	.long	1486032260
	.long	1068923542
	.long	1824674436
	.long	1066530837
	.long	2488677748
	.long	1066941079
	.long	1925770461
	.long	1063741314
	.long	2531500863
	.long	1063508710
	.long	3007508602
	.long	1060838619
	.long	1003260451
	.long	1061806090
	.long	3387875073
	.long	3218163548
	.long	3168927931
	.long	1014226373
	.long	0
	.long	1072693248
	.long	561239310
	.long	1068967597
	.long	210518352
	.long	1008717513
	.long	0
	.long	1070596096
	.long	3636030687
	.long	1068930947
	.long	3167463735
	.long	1066589471
	.long	73152643
	.long	1066950737
	.long	3108019154
	.long	1063821668
	.long	2100498394
	.long	1063525559
	.long	282708402
	.long	1060937705
	.long	2568921764
	.long	3207605450
	.long	2760966249
	.long	3218215020
	.long	1958126117
	.long	3162287979
	.long	0
	.long	1072693248
	.long	3078460826
	.long	1069037285
	.long	4285079825
	.long	1011989966
	.long	0
	.long	1070596096
	.long	997373752
	.long	1068938714
	.long	3045840714
	.long	1066648470
	.long	3110377520
	.long	1066960890
	.long	2323415564
	.long	1063903082
	.long	18309497
	.long	1063543250
	.long	551125427
	.long	1061039122
	.long	257413032
	.long	1061811122
	.long	2134057426
	.long	3218266492
	.long	1285458442
	.long	3161094457
	.long	0
	.long	1072693248
	.long	3593905368
	.long	1069107071
	.long	2989245764
	.long	3159385032
	.long	0
	.long	1070596096
	.long	3421947019
	.long	1068946843
	.long	3144465174
	.long	1066707852
	.long	1618007405
	.long	1066971547
	.long	584787122
	.long	1063985613
	.long	1465845750
	.long	1063561956
	.long	939979596
	.long	1061142667
	.long	1773905530
	.long	3204506663
	.long	1507148603
	.long	3218317964
	.long	1086374101
	.long	1010938257
	.long	0
	.long	1072693248
	.long	4132779814
	.long	1069176959
	.long	3895881848
	.long	3159710159
	.long	0
	.long	1070596096
	.long	4069764223
	.long	1068955338
	.long	1936654463
	.long	1066767636
	.long	27137259
	.long	1066982714
	.long	112447739
	.long	1064069319
	.long	3681308850
	.long	1063581561
	.long	3611917496
	.long	1061203836
	.long	2190408434
	.long	1061868473
	.long	880239780
	.long	3218369436
	.long	4135548218
	.long	1014081792
	.long	0
	.long	1072693248
	.long	2555242223
	.long	1069246954
	.long	1132401098
	.long	3159833783
	.long	0
	.long	1070596096
	.long	890626350
	.long	1068964202
	.long	3331100648
	.long	1066827840
	.long	30508518
	.long	1066994398
	.long	397665884
	.long	1064154260
	.long	1048931375
	.long	1063602233
	.long	4152525330
	.long	1061258067
	.long	71067649
	.long	3208005790
	.long	253330956
	.long	3218420908
	.long	991505830
	.long	3162432560
	.long	0
	.long	1072693248
	.long	1152452440
	.long	1069317060
	.long	2988872515
	.long	3159430464
	.long	0
	.long	1070596096
	.long	926263936
	.long	1068973436
	.long	3845070713
	.long	1066888484
	.long	626397172
	.long	1067006607
	.long	2777085683
	.long	1064240497
	.long	2172777379
	.long	1063623822
	.long	3188733172
	.long	1061313738
	.long	258986845
	.long	1062033866
	.long	3921389429
	.long	3218472379
	.long	3647185164
	.long	3161383618
	.long	0
	.long	1072693248
	.long	2357811071
	.long	1069387281
	.long	1815523692
	.long	1009992944
	.long	0
	.long	1070596096
	.long	3138003653
	.long	1068983043
	.long	1253835798
	.long	1066949588
	.long	2444914170
	.long	1067019349
	.long	1113961907
	.long	1064316367
	.long	1725681937
	.long	1063646503
	.long	2521427730
	.long	1061370759
	.long	2475375033
	.long	1061668474
	.long	3294480606
	.long	3218523851
	.long	1489607747
	.long	1001838107
	.long	0
	.long	1072693248
	.long	163325898
	.long	1069457623
	.long	272534188
	.long	1011389069
	.long	0
	.long	1070596096
	.long	2709640876
	.long	1068993027
	.long	3241569416
	.long	1067011170
	.long	3499770285
	.long	1067032633
	.long	2428916378
	.long	1064360877
	.long	796870323
	.long	1063670282
	.long	2401385265
	.long	1061429237
	.long	435854613
	.long	3207953615
	.long	2667571783
	.long	3218575323
	.long	1614402419
	.long	1013901432
	.long	0
	.long	1072693248
	.long	1600848691
	.long	1069528089
	.long	3799012648
	.long	1011342217
	.long	0
	.long	1070596096
	.long	3940625627
	.long	1069003390
	.long	1694054617
	.long	1067073252
	.long	3525212904
	.long	1067046468
	.long	3400170401
	.long	1064406132
	.long	2025409820
	.long	1063695095
	.long	1861695260
	.long	1061489337
	.long	2071083481
	.long	1058280175
	.long	2040662960
	.long	3218626795
	.long	4270081753
	.long	1014949642
	.long	0
	.long	1072693248
	.long	2636871548
	.long	1069573102
	.long	3635909220
	.long	1011750791
	.long	0
	.long	1070596096
	.long	3074865967
	.long	1069014136
	.long	2827594877
	.long	1067135853
	.long	4018616278
	.long	1067060863
	.long	3744630852
	.long	1064452166
	.long	967547267
	.long	1063721046
	.long	1523268212
	.long	1061551058
	.long	308558327
	.long	1057912410
	.long	1413754136
	.long	3218678267
	.long	856972295
	.long	3161564710
	.long	0
	.long	1072693248
	.long	2829232582
	.long	1069608467
	.long	1037842028
	.long	3159896230
	.long	0
	.long	1070596096
	.long	1194701220
	.long	1069025268
	.long	1485063562
	.long	1067198995
	.long	3996610205
	.long	1067075828
	.long	2865183303
	.long	1064499014
	.long	2185691567
	.long	1063748117
	.long	435906278
	.long	1061614535
	.long	1590730193
	.long	1061922957
	.long	786845313
	.long	3218729739
	.long	627570489
	.long	3158416059
	.long	0
	.long	1072693248
	.long	850612498
	.long	1069643902
	.long	3957836518
	.long	3160223109
	.long	0
	.long	1070596096
	.long	4230382869
	.long	1069036788
	.long	2678394493
	.long	1067262698
	.long	42048419
	.long	1067091374
	.long	61549345
	.long	1064546712
	.long	3326101021
	.long	1063776440
	.long	377176219
	.long	1061679763
	.long	1587637306
	.long	3206122484
	.long	159936490
	.long	3218781211
	.long	3547642993
	.long	1013612270
	.long	0
	.long	1072693248
	.long	2707383656
	.long	1069679408
	.long	554608849
	.long	1013037903
	.long	0
	.long	1070596096
	.long	1495139838
	.long	1069048702
	.long	183291521
	.long	1067326985
	.long	3540584774
	.long	1067107509
	.long	3317896890
	.long	1064595295
	.long	3844035522
	.long	1063805952
	.long	3695032775
	.long	1061746922
	.long	3749017783
	.long	3207727795
	.long	3827994963
	.long	3218832682
	.long	941734744
	.long	1014805062
	.long	0
	.long	1072693248
	.long	1615317960
	.long	1069714989
	.long	1400849280
	.long	3154963122
	.long	0
	.long	1070596096
	.long	55251407
	.long	1069061012
	.long	2380564598
	.long	1067391876
	.long	2078095393
	.long	1067124247
	.long	3759475360
	.long	1064644803
	.long	800747683
	.long	1063836662
	.long	3361780990
	.long	1061816118
	.long	3647588455
	.long	1062249457
	.long	3201086139
	.long	3218884154
	.long	4185319304
	.long	3161709290
	.long	0
	.long	1072693248
	.long	3772940467
	.long	1069750646
	.long	3998164292
	.long	3160831254
	.long	0
	.long	1070596096
	.long	3265990537
	.long	1069073721
	.long	3279971426
	.long	1067453881
	.long	2447085124
	.long	1067141598
	.long	1649985592
	.long	1064695275
	.long	3334399538
	.long	1063868754
	.long	454991464
	.long	1061887337
	.long	2110827584
	.long	3204215126
	.long	2574177316
	.long	3218935626
	.long	2889755040
	.long	3159466096
	.long	0
	.long	1072693248
	.long	2595342993
	.long	1069786383
	.long	1222183174
	.long	1011927611
	.long	0
	.long	1070596096
	.long	2192927435
	.long	1069086835
	.long	1645422722
	.long	1067486966
	.long	648269084
	.long	1067159575
	.long	2319737250
	.long	1064746750
	.long	1121494052
	.long	1063902126
	.long	897314780
	.long	1061960786
	.long	2655964904
	.long	1060259181
	.long	1947268493
	.long	3218987098
	.long	1185916272
	.long	1013323109
	.long	0
	.long	1072693248
	.long	192708373
	.long	1069822202
	.long	1013565611
	.long	1013125469
	.long	0
	.long	1070596096
	.long	1393443609
	.long	1069100357
	.long	3282624502
	.long	1067520387
	.long	3423023493
	.long	1067178189
	.long	3539283610
	.long	1064799270
	.long	1358653364
	.long	1063936902
	.long	1383848540
	.long	1062036512
	.long	1203341716
	.long	3207439782
	.long	1320359670
	.long	3219038570
	.long	1908355032
	.long	1014660481
	.long	0
	.long	1072693248
	.long	3079104278
	.long	1069858104
	.long	2593237048
	.long	1012204550
	.long	0
	.long	1070596096
	.long	1749059456
	.long	1069114291
	.long	3316773722
	.long	1067554157
	.long	2555119580
	.long	1067197455
	.long	59306268
	.long	1064852879
	.long	1944001714
	.long	1063973040
	.long	3607967027
	.long	1062114688
	.long	1550745665
	.long	1061515569
	.long	693450846
	.long	3219090042
	.long	3218699017
	.long	3161853871
	.long	0
	.long	1072693248
	.long	701484222
	.long	1069894094
	.long	1189134214
	.long	3161032728
	.long	0
	.long	1070596096
	.long	478132658
	.long	1069128642
	.long	2223654597
	.long	1067588288
	.long	706118045
	.long	1067217386
	.long	1066336370
	.long	1064907619
	.long	3310393859
	.long	1064010705
	.long	1920668857
	.long	1062195336
	.long	1238710124
	.long	3206743753
	.long	33271012
	.long	3219134917
	.long	2269574412
	.long	1015853455
	.long	0
	.long	1072693248
	.long	4098310206
	.long	1069930172
	.long	678867859
	.long	3161222246
	.long	0
	.long	1070596096
	.long	4033975882
	.long	1069143413
	.long	1577500332
	.long	1067622792
	.long	2904242360
	.long	1067237995
	.long	383406633
	.long	1064963537
	.long	511745140
	.long	1064049810
	.long	3472404675
	.long	1062243075
	.long	3498625096
	.long	1061932631
	.long	4014783896
	.long	3219160652
	.long	3119156846
	.long	1013033947
	.long	0
	.long	1072693248
	.long	2953879462
	.long	1069966343
	.long	651183718
	.long	1012253676
	.long	0
	.long	1070596096
	.long	2348802841
	.long	1069158611
	.long	4095204310
	.long	1067657681
	.long	3730066556
	.long	1067259298
	.long	2541564470
	.long	1065020679
	.long	1012137447
	.long	1064090541
	.long	3007493277
	.long	1062286104
	.long	268143184
	.long	1058732349
	.long	3701329484
	.long	3219186388
	.long	2857479637
	.long	3162758049
	.long	0
	.long	1072693248
	.long	4257115870
	.long	1070002608
	.long	39728604
	.long	1013010275
	.long	0
	.long	1070596096
	.long	3207767847
	.long	1069174239
	.long	797077063
	.long	1067692970
	.long	287980713
	.long	1067281311
	.long	1596666478
	.long	1065079095
	.long	2775385
	.long	1064132847
	.long	3823518174
	.long	1062330592
	.long	3096591665
	.long	1060290467
	.long	3387875073
	.long	3219212124
	.long	3168927931
	.long	1015274949
	.long	0
	.long	1072693248
	.long	2240974125
	.long	1070038971
	.long	3786178918
	.long	3160482292
	.long	0
	.long	1070596096
	.long	2199065489
	.long	1069190303
	.long	1118792048
	.long	1067728670
	.long	875809397
	.long	1067304048
	.long	2927616918
	.long	1065138834
	.long	3494337743
	.long	1064176822
	.long	4219965650
	.long	1062376593
	.long	4246297104
	.long	1060574506
	.long	3074420661
	.long	3219237860
	.long	1873363667
	.long	3160515403
	.long	0
	.long	1072693248
	.long	4156511795
	.long	1070075433
	.long	1791783009
	.long	1013446896
	.long	0
	.long	1070596096
	.long	4204226438
	.long	1069206807
	.long	2895376501
	.long	1067764795
	.long	3600185398
	.long	1067327526
	.long	2087275384
	.long	1065199949
	.long	347703954
	.long	1064222543
	.long	301495246
	.long	1062424178
	.long	908235914
	.long	3206713553
	.long	2760966249
	.long	3219263596
	.long	1958126117
	.long	3163336555
	.long	0
	.long	1072693248
	.long	212483252
	.long	1070111999
	.long	3496985219
	.long	3161099600
	.long	0
	.long	1070596096
	.long	1939348811
	.long	1069223758
	.long	1001569645
	.long	1067801360
	.long	764537997
	.long	1067351764
	.long	2326690580
	.long	1065262493
	.long	60750552
	.long	1064270005
	.long	2745665091
	.long	1062473435
	.long	1583079917
	.long	1060807371
	.long	2447511838
	.long	3219289332
	.long	3841595606
	.long	1014371319
	.long	0
	.long	1072693248
	.long	2234510834
	.long	1070148669
	.long	1077829152
	.long	1008793063
	.long	0
	.long	1070596096
	.long	1741599179
	.long	1069241160
	.long	288887764
	.long	1067838378
	.long	438316229
	.long	1067376778
	.long	2074161178
	.long	1065326522
	.long	2132352506
	.long	1064311981
	.long	321000208
	.long	1062524429
	.long	2575912902
	.long	1061549566
	.long	2134057426
	.long	3219315068
	.long	1285458442
	.long	3162143033
	.long	0
	.long	1072693248
	.long	719981842
	.long	1070185448
	.long	3396798827
	.long	3160592662
	.long	0
	.long	1070596096
	.long	2111724540
	.long	1069259019
	.long	2754706540
	.long	1067875863
	.long	790037588
	.long	1067402587
	.long	4097286835
	.long	1065372654
	.long	2918356845
	.long	1064337614
	.long	980543769
	.long	1062577230
	.long	1876925371
	.long	3204685716
	.long	1820603015
	.long	3219340804
	.long	3236194699
	.long	1015708874
	.long	0
	.long	1072693248
	.long	3497432648
	.long	1070222337
	.long	3403980908
	.long	1011735702
	.long	0
	.long	1070596096
	.long	2322009458
	.long	1069277341
	.long	3303184211
	.long	1067913831
	.long	3079386980
	.long	1067429210
	.long	3158396776
	.long	1065406241
	.long	500122450
	.long	1064364204
	.long	3226667103
	.long	1062631956
	.long	1843492548
	.long	1062167069
	.long	1507148603
	.long	3219366540
	.long	1086374101
	.long	1011986833
	.long	0
	.long	1072693248
	.long	1371601499
	.long	1070259341
	.long	3050542448
	.long	1012183888
	.long	0
	.long	1070596096
	.long	2434996648
	.long	1069296132
	.long	2688049294
	.long	1067952297
	.long	1586964757
	.long	1067453518
	.long	2102156419
	.long	1065440660
	.long	2974407256
	.long	1064391847
	.long	249545820
	.long	1062688662
	.long	115553579
	.long	1061128535
	.long	1193694191
	.long	3219392276
	.long	1890859349
	.long	3162902630
	.long	0
	.long	1072693248
	.long	2488075121
	.long	1070296461
	.long	1856289345
	.long	3161251229
	.long	0
	.long	1070596096
	.long	3322930631
	.long	1069315398
	.long	3277607933
	.long	1067991276
	.long	130253043
	.long	1067467675
	.long	1057196725
	.long	1065475943
	.long	3278604818
	.long	1064420531
	.long	4197429763
	.long	1062747460
	.long	1630708049
	.long	1062399550
	.long	880239780
	.long	3219418012
	.long	4135548218
	.long	1015130368
	.long	0
	.long	1072693248
	.long	2273547475
	.long	1070333701
	.long	4162052844
	.long	1011378450
	.long	0
	.long	1070596096
	.long	2392983423
	.long	1069335146
	.long	526970473
	.long	1068030785
	.long	628322840
	.long	1067482270
	.long	924033682
	.long	1065512123
	.long	854746912
	.long	1064450346
	.long	4004963071
	.long	1062808438
	.long	1468070334
	.long	1060747995
	.long	566785368
	.long	3219443748
	.long	2301849815
	.long	3161093726
	.long	0
	.long	1072693248
	.long	620845047
	.long	1070371064
	.long	314567191
	.long	1013284227
	.long	0
	.long	1070596096
	.long	2198158867
	.long	1069355382
	.long	222370857
	.long	1068070839
	.long	4028822361
	.long	1067497314
	.long	3610397327
	.long	1065549234
	.long	171354847
	.long	1064481283
	.long	693369877
	.long	1062871714
	.long	164532117
	.long	1061632559
	.long	253330956
	.long	3219469484
	.long	991505830
	.long	3163481136
	.long	0
	.long	1072693248
	.long	1894214069
	.long	1070408552
	.long	2182661802
	.long	1012022503
	.long	0
	.long	1070596096
	.long	1869133310
	.long	1069376113
	.long	1368144043
	.long	1068111455
	.long	242783364
	.long	1067512821
	.long	3692616565
	.long	1065587313
	.long	2890683560
	.long	1064513419
	.long	2407947124
	.long	1062937380
	.long	3803218812
	.long	1060561671
	.long	4234843841
	.long	3219495219
	.long	1479868884
	.long	1014082158
	.long	0
	.long	1072693248
	.long	2049842017
	.long	1070446169
	.long	2491067638
	.long	3161241370
	.long	0
	.long	1070596096
	.long	1431833764
	.long	1069397346
	.long	551048935
	.long	1068152651
	.long	2575917112
	.long	1067528800
	.long	1559777140
	.long	1065626397
	.long	743322322
	.long	1064546764
	.long	1974991899
	.long	1063005565
	.long	3459002328
	.long	1062460308
	.long	3921389429
	.long	3219520955
	.long	3647185164
	.long	3162432194
	.long	0
	.long	1072693248
	.long	1526324265
	.long	1070483918
	.long	4056333712
	.long	3157795638
	.long	0
	.long	1070596096
	.long	1830915900
	.long	1069419088
	.long	602185706
	.long	1068194444
	.long	555210277
	.long	1067545266
	.long	3690136761
	.long	1065666523
	.long	4115917104
	.long	1064581405
	.long	589230666
	.long	1063076369
	.long	3448584873
	.long	1062382364
	.long	3607935018
	.long	3219546691
	.long	4202814986
	.long	1015564293
	.long	0
	.long	1072693248
	.long	955408364
	.long	1070521802
	.long	2659929592
	.long	3160779467
	.long	0
	.long	1070596096
	.long	659174847
	.long	1069441347
	.long	2081794994
	.long	1068236852
	.long	2956616392
	.long	1067562230
	.long	1461867065
	.long	1065707733
	.long	879427925
	.long	1064617384
	.long	209598570
	.long	1063149928
	.long	59348277
	.long	1061529630
	.long	3294480606
	.long	3219572427
	.long	1489607747
	.long	1002886683
	.long	0
	.long	1072693248
	.long	1167858038
	.long	1070559824
	.long	2180017056
	.long	1010873776
	.long	0
	.long	1070596096
	.long	772792549
	.long	1069464130
	.long	3356872770
	.long	1068279894
	.long	3524844777
	.long	1067579707
	.long	1521756467
	.long	1065750067
	.long	3931969752
	.long	1064654734
	.long	2908114566
	.long	1063226363
	.long	3520733649
	.long	1061374592
	.long	2981026194
	.long	3219598163
	.long	924239062
	.long	3163047211
	.long	0
	.long	1072693248
	.long	3747221382
	.long	1070597041
	.long	1912303979
	.long	1014066411
	.long	0
	.long	1070596096
	.long	1727686788
	.long	1069487445
	.long	681802874
	.long	1068323590
	.long	2531789465
	.long	1067597711
	.long	331202638
	.long	1065793569
	.long	2627596742
	.long	1064693515
	.long	566979200
	.long	1063280948
	.long	2423252998
	.long	1061741710
	.long	2667571783
	.long	3219623899
	.long	1614402419
	.long	1014950008
	.long	0
	.long	1072693248
	.long	4148643959
	.long	1070616195
	.long	2887822437
	.long	1014214935
	.long	0
	.long	1070596096
	.long	4101770849
	.long	1069511300
	.long	1167052501
	.long	1068367958
	.long	2569727718
	.long	1067616256
	.long	1681502493
	.long	1065838283
	.long	3685967929
	.long	1064733780
	.long	2973319878
	.long	1063322263
	.long	474740638
	.long	1062549181
	.long	2354117371
	.long	3219649635
	.long	3512651629
	.long	3161564344
	.long	0
	.long	1072693248
	.long	373017919
	.long	1070635424
	.long	3647199022
	.long	1014660881
	.long	0
	.long	1070596096
	.long	933359764
	.long	1069535705
	.long	1391441088
	.long	1068413019
	.long	53106651
	.long	1067635358
	.long	4157835043
	.long	1065884256
	.long	4055868043
	.long	1064775611
	.long	1529330433
	.long	1063365236
	.long	326078913
	.long	1062325273
	.long	2040662960
	.long	3219675371
	.long	4270081753
	.long	1015998218
	.long	0
	.long	1072693248
	.long	4002124759
	.long	1070654727
	.long	3453343154
	.long	1013423881
	.long	0
	.long	1071644672
	.long	774562305
	.long	3217018021
	.long	377617675
	.long	1068458794
	.long	2495478050
	.long	1067655031
	.long	1508050213
	.long	1065931538
	.long	1921336565
	.long	1064819067
	.long	975923615
	.long	1063409946
	.long	2275792800
	.long	1060505011
	.long	1727208548
	.long	3219701107
	.long	2531251621
	.long	1013613001
	.long	0
	.long	1072693248
	.long	963359913
	.long	1070674109
	.long	2464811476
	.long	3160767877
	.long	0
	.long	1071644672
	.long	1498613974
	.long	3216992493
	.long	4138103782
	.long	1068502123
	.long	2840947366
	.long	1067675293
	.long	209537977
	.long	1065980178
	.long	3590357525
	.long	1064864171
	.long	73959555
	.long	1063456484
	.long	1512178095
	.long	1062390487
	.long	1413754136
	.long	3219726843
	.long	856972295
	.long	3162613286
	.long	0
	.long	0
	.long	0
	.long	3220176896
	.long	1994327296
	.long	1006932657
	.long	0
	.long	1073741824
	.long	0
	.long	0
	.long	266
	.long	3221225472
	.long	1431655956
	.long	1074091349
	.long	2828070733
	.long	3221924522
	.long	268536947
	.long	1074860305
	.long	4220652043
	.long	3222647048
	.long	3597047153
	.long	1075552901
	.long	1100299725
	.long	3219752579
	.long	874467977
	.long	1015419713
	.long	0
	.long	0
	.long	2668510354
	.long	3220126045
	.long	1701998120
	.long	1012206572
	.long	0
	.long	1073741824
	.long	1404962112
	.long	3215492924
	.long	3327263454
	.long	3221125605
	.long	1987673213
	.long	1074008829
	.long	231222583
	.long	3221793459
	.long	1756671652
	.long	1074731884
	.long	3921518330
	.long	3222500748
	.long	2691193109
	.long	1075341426
	.long	786845313
	.long	3219778315
	.long	627570489
	.long	3159464635
	.long	0
	.long	0
	.long	2949191430
	.long	3220076398
	.long	3989126963
	.long	3163096676
	.long	0
	.long	1073741824
	.long	2909112164
	.long	3216503445
	.long	3682557528
	.long	3221031585
	.long	4184421718
	.long	1073932454
	.long	380803270
	.long	3221673930
	.long	3399744364
	.long	1074553823
	.long	3842864962
	.long	3222371390
	.long	748075446
	.long	1075157395
	.long	473390901
	.long	3219804051
	.long	4252586071
	.long	3163191791
	.long	0
	.long	0
	.long	2308259791
	.long	3220027898
	.long	4111771200
	.long	3156765800
	.long	0
	.long	1073741824
	.long	411343470
	.long	3217132228
	.long	3760686807
	.long	3220942979
	.long	4171367426
	.long	1073861688
	.long	1878500541
	.long	3221564765
	.long	108767834
	.long	1074393698
	.long	2824899565
	.long	3222239549
	.long	3321236009
	.long	1074997439
	.long	159936490
	.long	3219829787
	.long	3547642993
	.long	1014660846
	.long	0
	.long	0
	.long	3342355758
	.long	3219980491
	.long	1666924640
	.long	3162031356
	.long	0
	.long	1073741824
	.long	2271288998
	.long	3217480250
	.long	4111284847
	.long	3220859392
	.long	2796272966
	.long	1073796048
	.long	1730314448
	.long	3221464928
	.long	1813001552
	.long	1074249474
	.long	603251437
	.long	3222036157
	.long	3079042443
	.long	1074856525
	.long	4141449374
	.long	3219855522
	.long	1579411055
	.long	3161853506
	.long	0
	.long	0
	.long	166164867
	.long	3219934128
	.long	2304492376
	.long	1012090550
	.long	0
	.long	1073741824
	.long	2798667120
	.long	3217812323
	.long	4203596829
	.long	3220780463
	.long	1473339147
	.long	1073728374
	.long	4243021652
	.long	3221373498
	.long	1638747017
	.long	1074119375
	.long	1512673116
	.long	3221855335
	.long	2338684822
	.long	1074678259
	.long	3827994963
	.long	3219881258
	.long	941734744
	.long	1015853638
	.long	0
	.long	0
	.long	3247184626
	.long	3219888759
	.long	1245319390
	.long	3161909785
	.long	0
	.long	1073741824
	.long	2381906911
	.long	3218104536
	.long	1010924947
	.long	3220705862
	.long	2481381005
	.long	1073615072
	.long	1356661294
	.long	3221289659
	.long	1527070583
	.long	1074001844
	.long	3061693405
	.long	3221694303
	.long	1993154673
	.long	1074463435
	.long	3514540551
	.long	3219906994
	.long	2102765473
	.long	1013034678
	.long	0
	.long	0
	.long	4189730214
	.long	3219844341
	.long	872357632
	.long	3158652168
	.long	0
	.long	1073741824
	.long	760634709
	.long	3218255915
	.long	2067827628
	.long	3220635284
	.long	1386756408
	.long	1073509659
	.long	404552634
	.long	3221199890
	.long	1143062644
	.long	1073895515
	.long	555342338
	.long	3221550657
	.long	3470199450
	.long	1074271418
	.long	3201086139
	.long	3219932730
	.long	4185319304
	.long	3162757866
	.long	0
	.long	0
	.long	719595600
	.long	3219800832
	.long	114970824
	.long	1014660100
	.long	0
	.long	1073741824
	.long	3811788216
	.long	3218400550
	.long	1264738948
	.long	3220568452
	.long	1958933439
	.long	1073411493
	.long	4246310163
	.long	3221058356
	.long	1367842247
	.long	1073799186
	.long	4112596176
	.long	3221422310
	.long	2100954469
	.long	1074105254
	.long	2887631728
	.long	3219958466
	.long	1841088264
	.long	1015275132
	.long	0
	.long	0
	.long	258163604
	.long	3219758190
	.long	3299889334
	.long	3162612005
	.long	0
	.long	1073741824
	.long	2865277319
	.long	3218538798
	.long	3110066496
	.long	3220505109
	.long	723074889
	.long	1073319994
	.long	1618819009
	.long	3220928086
	.long	1176243935
	.long	1073681775
	.long	1223849000
	.long	3221307454
	.long	2369288310
	.long	1073957581
	.long	2574177316
	.long	3219984202
	.long	2889755040
	.long	3160514672
	.long	0
	.long	0
	.long	2079874795
	.long	3219716377
	.long	1885564844
	.long	1014470429
	.long	0
	.long	1073741824
	.long	1656469901
	.long	3218670989
	.long	823761676
	.long	3220445021
	.long	2491445454
	.long	1073234634
	.long	1584341555
	.long	3220808039
	.long	1976688438
	.long	1073523018
	.long	3918313362
	.long	3221183550
	.long	3178717701
	.long	1073829019
	.long	2260722904
	.long	3220009938
	.long	3285965784
	.long	3163336372
	.long	0
	.long	0
	.long	997287281
	.long	3219675358
	.long	3612251184
	.long	3163317056
	.long	0
	.long	1073741824
	.long	3037674984
	.long	3218797431
	.long	2624215131
	.long	3220387969
	.long	464779768
	.long	1073154936
	.long	3700763630
	.long	3220697284
	.long	1200926910
	.long	1073378628
	.long	2934576448
	.long	3220998744
	.long	3287041894
	.long	1073681673
	.long	1947268493
	.long	3220035674
	.long	1185916272
	.long	1014371685
	.long	0
	.long	0
	.long	3978924239
	.long	3219635097
	.long	680771999
	.long	1015720881
	.long	0
	.long	1073741824
	.long	3528982175
	.long	3218918413
	.long	3018376188
	.long	3220333754
	.long	2093086652
	.long	1073080463
	.long	451029217
	.long	3220594987
	.long	1097121537
	.long	1073247142
	.long	2277789243
	.long	3220832617
	.long	149138386
	.long	1073478822
	.long	1633814081
	.long	3220061410
	.long	3941137777
	.long	3162142667
	.long	0
	.long	0
	.long	3860971117
	.long	3219595563
	.long	2891614774
	.long	3163214156
	.long	0
	.long	1073741824
	.long	80822128
	.long	3219034205
	.long	2077635078
	.long	3220282191
	.long	1552870456
	.long	1073010820
	.long	3607700650
	.long	3220500394
	.long	249538439
	.long	1073127265
	.long	2233833083
	.long	3220683068
	.long	2436456793
	.long	1073293556
	.long	1320359670
	.long	3220087146
	.long	1908355032
	.long	1015709057
	.long	0
	.long	0
	.long	343219638
	.long	3219556725
	.long	2238262736
	.long	3161731808
	.long	0
	.long	1072693248
	.long	3768132253
	.long	1071627934
	.long	3895546569
	.long	3220233108
	.long	1613056473
	.long	1072945645
	.long	3409294908
	.long	3220412831
	.long	3138966349
	.long	1073017844
	.long	3799067288
	.long	3220548255
	.long	359470498
	.long	1073133087
	.long	1006905258
	.long	3220112882
	.long	3348558652
	.long	1011988294
	.long	0
	.long	0
	.long	3841229102
	.long	3219518551
	.long	1782820117
	.long	3163516640
	.long	0
	.long	1072693248
	.long	866896685
	.long	1071521786
	.long	2743435231
	.long	3220186348
	.long	3017984361
	.long	1072884608
	.long	1491919678
	.long	3220331688
	.long	1507535811
	.long	1072917857
	.long	1966124974
	.long	3220426562
	.long	381628641
	.long	1072992589
	.long	693450846
	.long	3220138618
	.long	3218699017
	.long	3162902447
	.long	0
	.long	0
	.long	716700048
	.long	3219481016
	.long	1043841701
	.long	3163006458
	.long	0
	.long	1072693248
	.long	1097907398
	.long	1071420120
	.long	3912524876
	.long	3220106631
	.long	4107784306
	.long	1072827408
	.long	937056065
	.long	3220256414
	.long	4128945134
	.long	1072826390
	.long	1032262394
	.long	3220316565
	.long	1825923585
	.long	1072864423
	.long	379996435
	.long	3220164354
	.long	2807708551
	.long	1015130551
	.long	0
	.long	0
	.long	142288477
	.long	3219444091
	.long	1256348500
	.long	1014532780
	.long	0
	.long	1072693248
	.long	925074332
	.long	1071322730
	.long	2134728327
	.long	3220021541
	.long	1886408555
	.long	1072773770
	.long	101893267
	.long	3220186512
	.long	2020085224
	.long	1072742631
	.long	2955803861
	.long	3220217010
	.long	343310816
	.long	1072748161
	.long	33271012
	.long	3220183493
	.long	2269574412
	.long	1016902031
	.long	0
	.long	0
	.long	3684492763
	.long	3219407750
	.long	2905890526
	.long	1015709561
	.long	0
	.long	1072693248
	.long	585640690
	.long	1071229422
	.long	1068301595
	.long	3219940277
	.long	2953900489
	.long	1072723440
	.long	950237889
	.long	3220066167
	.long	109735425
	.long	1072638455
	.long	792610979
	.long	3220076689
	.long	2904190292
	.long	1072594405
	.long	4171511102
	.long	3220196360
	.long	987810900
	.long	1016106563
	.long	0
	.long	0
	.long	681498355
	.long	3219371971
	.long	3542625299
	.long	1015827699
	.long	0
	.long	1072693248
	.long	168222332
	.long	1071140014
	.long	776736583
	.long	3219862604
	.long	2405401100
	.long	1072659128
	.long	430546584
	.long	3219945234
	.long	2974083786
	.long	1072497552
	.long	849442068
	.long	3219912970
	.long	2727451532
	.long	1072409680
	.long	4014783896
	.long	3220209228
	.long	3119156846
	.long	1014082523
	.long	0
	.long	0
	.long	2298773501
	.long	3219336728
	.long	4027807132
	.long	1015090440
	.long	0
	.long	1072693248
	.long	312748411
	.long	1071054335
	.long	2723913091
	.long	3219788303
	.long	3207063211
	.long	1072570353
	.long	1845780128
	.long	3219832588
	.long	752659336
	.long	1072368142
	.long	1097207453
	.long	3219764237
	.long	407427403
	.long	1072246732
	.long	3858056690
	.long	3220222096
	.long	2007897202
	.long	3162431829
	.long	0
	.long	0
	.long	2591355529
	.long	3219302000
	.long	2351326818
	.long	3161908153
	.long	0
	.long	1072693248
	.long	3206845590
	.long	1070972224
	.long	4023349050
	.long	3219717171
	.long	3838822434
	.long	1072486918
	.long	3122427157
	.long	3219727557
	.long	2261703627
	.long	1072249178
	.long	1764870100
	.long	3219628955
	.long	3201802997
	.long	1072093301
	.long	3701329484
	.long	3220234964
	.long	2857479637
	.long	3163806625
	.long	0
	.long	0
	.long	4010047525
	.long	3219267765
	.long	3339891512
	.long	3160072694
	.long	0
	.long	1072693248
	.long	966450668
	.long	1070893532
	.long	803989702
	.long	3219649019
	.long	1934453966
	.long	1072408465
	.long	1455077850
	.long	3219629530
	.long	2709264343
	.long	1072139721
	.long	1918064558
	.long	3219505762
	.long	1476733535
	.long	1071960574
	.long	3544602278
	.long	3220247832
	.long	4139243149
	.long	3164602093
	.long	0
	.long	0
	.long	4270735453
	.long	3219234003
	.long	3565361953
	.long	1015342529
	.long	0
	.long	1072693248
	.long	2915760230
	.long	1070818115
	.long	638580392
	.long	3219583668
	.long	2252948193
	.long	1072334663
	.long	1194731830
	.long	3219537949
	.long	1353002942
	.long	1072038927
	.long	422364789
	.long	3219393446
	.long	3669634446
	.long	1071842023
	.long	3387875073
	.long	3220260700
	.long	3168927931
	.long	1016323525
	.long	0
	.long	0
	.long	419968236
	.long	3219200695
	.long	131828542
	.long	1015961466
	.long	0
	.long	1072693248
	.long	2960267235
	.long	1070745841
	.long	1037049094
	.long	3219520953
	.long	1447436626
	.long	1072265209
	.long	3197232138
	.long	3219452306
	.long	4004685084
	.long	1071946034
	.long	1412816421
	.long	3219290928
	.long	3184738590
	.long	1071740610
	.long	3231147867
	.long	3220273568
	.long	3253690381
	.long	1014950373
	.long	0
	.long	0
	.long	1505561933
	.long	3219167820
	.long	2813885200
	.long	1011162516
	.long	0
	.long	1072693248
	.long	2997217365
	.long	1070676584
	.long	2681788575
	.long	3219460719
	.long	3989126937
	.long	1072199822
	.long	1049115510
	.long	3219372139
	.long	511642058
	.long	1071860360
	.long	4034728051
	.long	3219197246
	.long	1788863965
	.long	1071639550
	.long	3074420661
	.long	3220286436
	.long	1873363667
	.long	3161563979
	.long	0
	.long	0
	.long	2606398711
	.long	3219135361
	.long	2533072598
	.long	3162554073
	.long	0
	.long	1072693248
	.long	2151361100
	.long	1070610226
	.long	1532348096
	.long	3219402822
	.long	408604530
	.long	1072138246
	.long	1039538740
	.long	3219297023
	.long	1234598850
	.long	1071781284
	.long	692507578
	.long	3219094767
	.long	4090315877
	.long	1071451983
	.long	2917693455
	.long	3220299304
	.long	676362605
	.long	3163589663
	.long	0
	.long	0
	.long	3649427950
	.long	3219078282
	.long	4113825640
	.long	3158662193
	.long	0
	.long	1072693248
	.long	2424145432
	.long	1070497215
	.long	2715404890
	.long	3219347125
	.long	2238283673
	.long	1072080240
	.long	1186867211
	.long	3219226571
	.long	660318792
	.long	1071708248
	.long	2091427915
	.long	3218937780
	.long	2591062635
	.long	1071291190
	.long	2760966249
	.long	3220312172
	.long	1958126117
	.long	3164385131
	.long	0
	.long	0
	.long	220675706
	.long	3219014926
	.long	3158451653
	.long	3161723787
	.long	0
	.long	1072693248
	.long	1565030890
	.long	1070375439
	.long	335263687
	.long	3219293502
	.long	419816567
	.long	1072025586
	.long	2098048102
	.long	3219160428
	.long	257229335
	.long	1071636821
	.long	3464457189
	.long	3218793840
	.long	2745923191
	.long	1071158056
	.long	2604239044
	.long	3220325040
	.long	1055077667
	.long	1016540488
	.long	0
	.long	0
	.long	2191706112
	.long	3218952300
	.long	333441366
	.long	3161331348
	.long	0
	.long	1072693248
	.long	3435408456
	.long	1070258830
	.long	487319719
	.long	3219241832
	.long	1713711059
	.long	1071974079
	.long	130753286
	.long	3219068219
	.long	909725130
	.long	1071511974
	.long	2762442367
	.long	3218661715
	.long	932670499
	.long	1071027067
	.long	2447511838
	.long	3220337908
	.long	3841595606
	.long	1015419895
	.long	0
	.long	0
	.long	408840118
	.long	3218890375
	.long	4048072104
	.long	3159354273
	.long	0
	.long	1072693248
	.long	2794370512
	.long	1070147203
	.long	1550993351
	.long	3219192003
	.long	1305093678
	.long	1071925532
	.long	1748211158
	.long	3218951271
	.long	3244212114
	.long	1071396455
	.long	2145666003
	.long	3218540299
	.long	1098451822
	.long	1070891096
	.long	2290784632
	.long	3220350776
	.long	2660353234
	.long	3159463173
	.long	0
	.long	0
	.long	3368427099
	.long	3218829119
	.long	2187864186
	.long	1013587140
	.long	0
	.long	1072693248
	.long	1708021028
	.long	1070040382
	.long	459972195
	.long	3219143910
	.long	3210514347
	.long	1071879770
	.long	955408873
	.long	3218841145
	.long	1911386085
	.long	1071289511
	.long	3563284038
	.long	3218428599
	.long	547420745
	.long	1070790031
	.long	2134057426
	.long	3220363644
	.long	1285458442
	.long	3163191609
	.long	0
	.long	0
	.long	2583490354
	.long	3218768505
	.long	27148750
	.long	3161491985
	.long	0
	.long	1072693248
	.long	2550940470
	.long	1069938201
	.long	2616040293
	.long	3219066585
	.long	2529278955
	.long	1071836633
	.long	2244405849
	.long	3218737335
	.long	2064814127
	.long	1071190460
	.long	1527853251
	.long	3218325722
	.long	354247476
	.long	1070708112
	.long	1977330220
	.long	3220376512
	.long	4071976381
	.long	3164168168
	.long	0
	.long	0
	.long	2390846448
	.long	3218708504
	.long	3510503948
	.long	3161709355
	.long	0
	.long	1072693248
	.long	1208409702
	.long	1069840505
	.long	1006199412
	.long	3218976755
	.long	2504246873
	.long	1071795971
	.long	1040653613
	.long	3218639377
	.long	1713268217
	.long	1071098686
	.long	462276301
	.long	3218230862
	.long	4239163357
	.long	1070603989
	.long	1820603015
	.long	3220389380
	.long	3236194699
	.long	1016757450
	.long	0
	.long	0
	.long	2528366626
	.long	3218649089
	.long	3808712102
	.long	1013396510
	.long	0
	.long	1072693248
	.long	1938261590
	.long	1069747146
	.long	4282399603
	.long	3218889831
	.long	1922568281
	.long	1071757646
	.long	2428166646
	.long	3218546841
	.long	717464824
	.long	1071013623
	.long	2205664075
	.long	3218143293
	.long	1526076492
	.long	1070438229
	.long	1663875809
	.long	3220402248
	.long	3908862373
	.long	1015853820
	.long	0
	.long	0
	.long	2200768388
	.long	3218590234
	.long	302584884
	.long	1014949287
	.long	0
	.long	1072693248
	.long	2338248308
	.long	1069657986
	.long	2617403810
	.long	3218805648
	.long	60078074
	.long	1071721530
	.long	2753634668
	.long	3218459332
	.long	626020946
	.long	1070934758
	.long	25846122
	.long	3218044977
	.long	1123428672
	.long	1070307499
	.long	1507148603
	.long	3220415116
	.long	1086374101
	.long	1013035409
	.long	0
	.long	0
	.long	4158096969
	.long	3218531913
	.long	3224512900
	.long	3160902954
	.long	0
	.long	1072693248
	.long	4240313622
	.long	1069572894
	.long	3745046477
	.long	3218724046
	.long	1955120038
	.long	1071687503
	.long	2661530988
	.long	3218376484
	.long	2615081511
	.long	1070861626
	.long	3172686029
	.long	3217895196
	.long	3468537113
	.long	1070173373
	.long	1350421397
	.long	3220427984
	.long	1218191675
	.long	3162757684
	.long	0
	.long	0
	.long	1606100959
	.long	3218474103
	.long	3385789004
	.long	3161114183
	.long	0
	.long	1072693248
	.long	2255302008
	.long	1069435977
	.long	54085333
	.long	3218644877
	.long	2504974906
	.long	1071655456
	.long	4169947836
	.long	3218297958
	.long	2891827398
	.long	1070793803
	.long	1030744944
	.long	3217756428
	.long	2378245768
	.long	1070036506
	.long	1193694191
	.long	3220440852
	.long	1890859349
	.long	3163951206
	.long	0
	.long	0
	.long	3782220567
	.long	3218416778
	.long	2488119274
	.long	3162237132
	.long	0
	.long	1072693248
	.long	91372748
	.long	1069281344
	.long	318776714
	.long	3218567997
	.long	1791786485
	.long	1071605902
	.long	245863368
	.long	3218223442
	.long	3306699316
	.long	1070730899
	.long	492155895
	.long	3217627699
	.long	504590813
	.long	1069961586
	.long	1036966986
	.long	3220453720
	.long	1122344434
	.long	1016974413
	.long	0
	.long	0
	.long	1412148370
	.long	3218359917
	.long	2689483850
	.long	1013993587
	.long	0
	.long	1072693248
	.long	1375255072
	.long	1069134151
	.long	1493266594
	.long	3218493272
	.long	487968719
	.long	1071549129
	.long	4010395236
	.long	3218152642
	.long	2875570533
	.long	1070672565
	.long	2596803943
	.long	3217508124
	.long	2843491945
	.long	1069843051
	.long	880239780
	.long	3220466588
	.long	4135548218
	.long	1016178944
	.long	0
	.long	0
	.long	895247324
	.long	3218303496
	.long	3064008070
	.long	1013805381
	.long	0
	.long	1072693248
	.long	729983844
	.long	1068994194
	.long	3631919317
	.long	3218420574
	.long	2031343772
	.long	1071495745
	.long	1505593195
	.long	3218085291
	.long	519428997
	.long	1070618477
	.long	3710689661
	.long	3217396902
	.long	3156076115
	.long	1069741343
	.long	723512574
	.long	3220479456
	.long	2825204233
	.long	1014372050
	.long	0
	.long	0
	.long	2664093517
	.long	3218247493
	.long	3574482624
	.long	3157273734
	.long	0
	.long	1072693248
	.long	2465054196
	.long	1068861279
	.long	3495559184
	.long	3218349782
	.long	2180799467
	.long	1071445588
	.long	3760731789
	.long	3217962527
	.long	3108073739
	.long	1070540584
	.long	1955989624
	.long	3217293302
	.long	166797605
	.long	1069654305
	.long	566785368
	.long	3220492324
	.long	2301849815
	.long	3162142302
	.long	0
	.long	0
	.long	1026922473
	.long	3218191888
	.long	4275547180
	.long	3161242805
	.long	0
	.long	1072693248
	.long	865263972
	.long	1068735226
	.long	1189261043
	.long	3218280780
	.long	602895285
	.long	1071398507
	.long	457371263
	.long	3217840141
	.long	1846858242
	.long	1070447677
	.long	3060498116
	.long	3217196657
	.long	4267893365
	.long	1069596604
	.long	410058162
	.long	3220505192
	.long	4004709614
	.long	3163734243
	.long	0
	.long	0
	.long	1197423420
	.long	3218136659
	.long	176395860
	.long	3160912588
	.long	0
	.long	1072693248
	.long	3614211808
	.long	1068615863
	.long	2638984897
	.long	3218213456
	.long	1695604536
	.long	1071354361
	.long	2372043566
	.long	3217723241
	.long	136909605
	.long	1070361652
	.long	3640039447
	.long	3217106358
	.long	4088615056
	.long	1069449448
	.long	253330956
	.long	3220518060
	.long	991505830
	.long	3164529712
	.long	0
	.long	0
	.long	1971725998
	.long	3218081786
	.long	2628057466
	.long	3160778219
	.long	0
	.long	1072693248
	.long	3850616508
	.long	1068503032
	.long	715485429
	.long	3218147706
	.long	2831870887
	.long	1071313021
	.long	276112813
	.long	3217611421
	.long	4071473627
	.long	1070282053
	.long	3678820506
	.long	3217012537
	.long	927889203
	.long	1069392578
	.long	96603751
	.long	3220530928
	.long	2021697954
	.long	1016395907
	.long	0
	.long	0
	.long	4068253482
	.long	3217974755
	.long	3098800802
	.long	3161416360
	.long	0
	.long	1072693248
	.long	3555466384
	.long	1068294223
	.long	3714292293
	.long	3218083427
	.long	58004546
	.long	1071274368
	.long	2889916307
	.long	3217504297
	.long	2725223785
	.long	1070208483
	.long	1960889150
	.long	3216854085
	.long	2172838405
	.long	1069259864
	.long	4234843841
	.long	3220543795
	.long	1479868884
	.long	1015130734
	.long	0
	.long	0
	.long	3947580658
	.long	3217866316
	.long	4153464200
	.long	3160712468
	.long	0
	.long	1072693248
	.long	1306970432
	.long	1068093810
	.long	3320658510
	.long	3217961305
	.long	2638377394
	.long	1071238289
	.long	1218095092
	.long	3217401513
	.long	506153465
	.long	1070140566
	.long	2096772766
	.long	3216705262
	.long	2058303373
	.long	1069148484
	.long	4078116635
	.long	3220556663
	.long	39665264
	.long	3161092265
	.long	0
	.long	0
	.long	4023957346
	.long	3217758473
	.long	2652051644
	.long	3159900932
	.long	0
	.long	1072693248
	.long	1548193280
	.long	1067905620
	.long	2849460080
	.long	3217838064
	.long	586099284
	.long	1071204684
	.long	2426038050
	.long	3217302731
	.long	1209882939
	.long	1070077962
	.long	204929670
	.long	3216565209
	.long	2951398611
	.long	1069077118
	.long	3921389429
	.long	3220569531
	.long	3647185164
	.long	3163480770
	.long	0
	.long	0
	.long	3763679576
	.long	3217651189
	.long	448385104
	.long	3157192355
	.long	0
	.long	1072693248
	.long	64931152
	.long	1067729411
	.long	3593250320
	.long	3217717209
	.long	1127364104
	.long	1071173457
	.long	21320967
	.long	3217207636
	.long	4036384111
	.long	1070020367
	.long	3394693850
	.long	3216433130
	.long	4171153724
	.long	1068939101
	.long	3764662223
	.long	3220582399
	.long	3105356094
	.long	3164312749
	.long	0
	.long	0
	.long	1425758568
	.long	3217544428
	.long	1224254624
	.long	3159340630
	.long	0
	.long	1072693248
	.long	2086713664
	.long	1067564956
	.long	1688303979
	.long	3217598569
	.long	938619730
	.long	1071144522
	.long	526120622
	.long	3217115928
	.long	3819295674
	.long	1069967496
	.long	2059466203
	.long	3216308301
	.long	4034680199
	.long	1068903334
	.long	3607935018
	.long	3220595267
	.long	4202814986
	.long	1016612869
	.long	0
	.long	0
	.long	2429422902
	.long	3217438153
	.long	3297520416
	.long	1012522944
	.long	0
	.long	1072693248
	.long	528565440
	.long	1067373727
	.long	2395999282
	.long	3217481977
	.long	1512953454
	.long	1071117799
	.long	2702142222
	.long	3217023483
	.long	3515899579
	.long	1069919100
	.long	2767905791
	.long	3216190039
	.long	3127036520
	.long	1068808800
	.long	3451207812
	.long	3220608135
	.long	1547135652
	.long	1015564659
	.long	0
	.long	0
	.long	1959743063
	.long	3217332330
	.long	607720656
	.long	3161041674
	.long	0
	.long	1072693248
	.long	225834432
	.long	1067090614
	.long	732340398
	.long	3217367274
	.long	3221152670
	.long	1071093215
	.long	2810588359
	.long	3216851955
	.long	612200454
	.long	1069874948
	.long	645427044
	.long	3216077719
	.long	1966523063
	.long	1068694214
	.long	3294480606
	.long	3220621003
	.long	1489607747
	.long	1003935259
	.long	0
	.long	0
	.long	1940260416
	.long	3217226924
	.long	4144841612
	.long	3159543701
	.long	0
	.long	1072693248
	.long	1135818128
	.long	1066829850
	.long	867512158
	.long	3217254304
	.long	3821819258
	.long	1071070704
	.long	2890864845
	.long	3216685596
	.long	918459421
	.long	1069834828
	.long	797471869
	.long	3215958923
	.long	4093823457
	.long	1068635569
	.long	3137753400
	.long	3220633871
	.long	3579918397
	.long	3163046845
	.long	0
	.long	0
	.long	2242453104
	.long	3217121901
	.long	1143544320
	.long	1011877960
	.long	0
	.long	1072693248
	.long	1016752608
	.long	1066591110
	.long	1270001991
	.long	3217142917
	.long	1410785275
	.long	1071050206
	.long	1103929722
	.long	3216523923
	.long	3268015214
	.long	1069798554
	.long	957665123
	.long	3215754628
	.long	1067716675
	.long	1068610012
	.long	2981026194
	.long	3220646739
	.long	924239062
	.long	3164095787
	.long	0
	.long	0
	.long	982366697
	.long	3217003288
	.long	1160619554
	.long	1010902188
	.long	0
	.long	1072693248
	.long	404678272
	.long	1066346403
	.long	1419607756
	.long	3217032967
	.long	1800406832
	.long	1071031665
	.long	3144361055
	.long	3216366473
	.long	1744148039
	.long	1069765960
	.long	3211598528
	.long	3215558937
	.long	405031618
	.long	1068609434
	.long	2824298989
	.long	3220659607
	.long	2088964722
	.long	1016829832
	.long	0
	.long	0
	.long	931121154
	.long	3216794574
	.long	1121583044
	.long	1012154237
	.long	0
	.long	1072693248
	.long	709357472
	.long	1065955296
	.long	29972165
	.long	3216817456
	.long	254360647
	.long	1071015033
	.long	3732687627
	.long	3216212805
	.long	3224370681
	.long	1069736900
	.long	202592280
	.long	3215370879
	.long	1420621447
	.long	1068491719
	.long	2667571783
	.long	3220672475
	.long	1614402419
	.long	1015998584
	.long	0
	.long	0
	.long	2354785698
	.long	3216586427
	.long	959264268
	.long	3159915811
	.long	0
	.long	1072693248
	.long	2828230112
	.long	1065606626
	.long	3693284264
	.long	3216602456
	.long	1996210616
	.long	1071000265
	.long	3053541878
	.long	3216062494
	.long	1133334567
	.long	1069711236
	.long	977677585
	.long	3215189563
	.long	347473669
	.long	1068436198
	.long	2510844577
	.long	3220685343
	.long	498468876
	.long	1013614463
	.long	0
	.long	0
	.long	1972895212
	.long	3216378783
	.long	3207896256
	.long	3158122047
	.long	0
	.long	1072693248
	.long	4107853760
	.long	1065246718
	.long	2229347510
	.long	3216389497
	.long	4172638464
	.long	1070987323
	.long	212469442
	.long	3215847669
	.long	1989165661
	.long	1069688852
	.long	4131454738
	.long	3215014128
	.long	3452987325
	.long	1068469531
	.long	2354117371
	.long	3220698211
	.long	3512651629
	.long	3162612920
	.long	0
	.long	0
	.long	3439278129
	.long	3216171577
	.long	1073008748
	.long	3159914989
	.long	0
	.long	1072693248
	.long	2934847296
	.long	1064716672
	.long	2625704508
	.long	3216178311
	.long	3692939630
	.long	1070976174
	.long	3072165553
	.long	3215558041
	.long	223733198
	.long	1069669653
	.long	1158528288
	.long	3214753465
	.long	3197803998
	.long	1068357327
	.long	2197390165
	.long	3220711079
	.long	3038089327
	.long	3163878824
	.long	0
	.long	0
	.long	3930011523
	.long	3215946901
	.long	3574565577
	.long	1010859792
	.long	0
	.long	1072693248
	.long	930814464
	.long	1064233867
	.long	3218126771
	.long	3215954682
	.long	873740287
	.long	1070966789
	.long	1580060562
	.long	3215272745
	.long	1505280051
	.long	1069653548
	.long	2221338643
	.long	3214421240
	.long	2641116540
	.long	1068312400
	.long	2040662960
	.long	3220723947
	.long	4270081753
	.long	1017046794
	.long	0
	.long	0
	.long	2319126882
	.long	3215533863
	.long	2132624928
	.long	3153789373
	.long	0
	.long	1072693248
	.long	2962101888
	.long	1063503197
	.long	3966050657
	.long	3215537841
	.long	3178024141
	.long	1070959142
	.long	1146830997
	.long	3214991029
	.long	1630003424
	.long	1069640468
	.long	2204651989
	.long	3214096041
	.long	796867276
	.long	1068243248
	.long	1883935754
	.long	3220736815
	.long	2988318241
	.long	1016251326
	.long	0
	.long	0
	.long	3919601072
	.long	3215121322
	.long	2384023593
	.long	1011053048
	.long	0
	.long	1072693248
	.long	1803946752
	.long	1062615560
	.long	2247013512
	.long	3215122999
	.long	3277043521
	.long	1070953215
	.long	578207576
	.long	3214490301
	.long	1619551293
	.long	1069630354
	.long	2648603598
	.long	3213667381
	.long	2728840488
	.long	1068193529
	.long	1727208548
	.long	3220749683
	.long	2531251621
	.long	1014661577
	.long	0
	.long	0
	.long	612012528
	.long	3214484294
	.long	549859680
	.long	1005659865
	.long	0
	.long	1072693248
	.long	2064191488
	.long	1061404484
	.long	4042886527
	.long	3214485286
	.long	149923164
	.long	1070948993
	.long	104448276
	.long	3213936806
	.long	2663431480
	.long	1069623160
	.long	4100741337
	.long	3213036454
	.long	3253086979
	.long	1068220130
	.long	1570481342
	.long	3220762551
	.long	2595802427
	.long	3161852775
	.long	0
	.long	0
	.long	75380366
	.long	3213435470
	.long	3015416698
	.long	1008346766
	.long	0
	.long	1072693248
	.long	2911696896
	.long	1059306942
	.long	451505506
	.long	3213435718
	.long	3089290734
	.long	1070946463
	.long	3705470162
	.long	3212886822
	.long	2568117130
	.long	1069618857
	.long	643302148
	.long	3211985100
	.long	500114129
	.long	1068202863
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
	.type	__svml_dtan_ha_data_internal,@object
	.size	__svml_dtan_ha_data_internal,28224
	.space 960, 0x00 	# pad
	.align 32
.L_2il0floatpacket.45:
	.long	0xf8000000,0xffffffff,0xf8000000,0xffffffff,0xf8000000,0xffffffff,0xf8000000,0xffffffff
	.type	.L_2il0floatpacket.45,@object
	.size	.L_2il0floatpacket.45,32
	.align 32
.L_2il0floatpacket.46:
	.long	0x00000000,0x3ff00000,0x00000000,0x3ff00000,0x00000000,0x3ff00000,0x00000000,0x3ff00000
	.type	.L_2il0floatpacket.46,@object
	.size	.L_2il0floatpacket.46,32
	.align 32
.L_2il0floatpacket.47:
	.long	0x00000000,0x42a80000,0x00000000,0x42a80000,0x00000000,0x42a80000,0x00000000,0x42a80000
	.type	.L_2il0floatpacket.47,@object
	.size	.L_2il0floatpacket.47,32
	.align 32
.L_2il0floatpacket.48:
	.long	0x54442d18,0x401921fb,0x54442d18,0x401921fb,0x54442d18,0x401921fb,0x54442d18,0x401921fb
	.type	.L_2il0floatpacket.48,@object
	.size	.L_2il0floatpacket.48,32
	.align 32
.L_2il0floatpacket.49:
	.long	0x54000000,0x401921fb,0x54000000,0x401921fb,0x54000000,0x401921fb,0x54000000,0x401921fb
	.type	.L_2il0floatpacket.49,@object
	.size	.L_2il0floatpacket.49,32
	.align 32
.L_2il0floatpacket.50:
	.long	0x11a62633,0x3e310b46,0x11a62633,0x3e310b46,0x11a62633,0x3e310b46,0x11a62633,0x3e310b46
	.type	.L_2il0floatpacket.50,@object
	.size	.L_2il0floatpacket.50,32
	.align 32
.L_2il0floatpacket.51:
	.long	0xffffffff,0x7fffffff,0xffffffff,0x7fffffff,0xffffffff,0x7fffffff,0xffffffff,0x7fffffff
	.type	.L_2il0floatpacket.51,@object
	.size	.L_2il0floatpacket.51,32
	.align 32
.L_2il0floatpacket.52:
	.long	0x00000000,0x3eb00000,0x00000000,0x3eb00000,0x00000000,0x3eb00000,0x00000000,0x3eb00000
	.type	.L_2il0floatpacket.52,@object
	.size	.L_2il0floatpacket.52,32
	.align 32
.L_2il0floatpacket.75:
	.long	0x00000008,0x00000008,0x00000008,0x00000008,0x00000008,0x00000008,0x00000008,0x00000008
	.type	.L_2il0floatpacket.75,@object
	.size	.L_2il0floatpacket.75,32
	.align 32
.L_2il0floatpacket.78:
	.long	0x00000000,0x7ff00000,0x00000000,0x7ff00000,0x00000000,0x7ff00000,0x00000000,0x7ff00000
	.type	.L_2il0floatpacket.78,@object
	.size	.L_2il0floatpacket.78,32
	.align 32
.L_2il0floatpacket.79:
	.long	0xffffffff,0x000fffff,0xffffffff,0x000fffff,0xffffffff,0x000fffff,0xffffffff,0x000fffff
	.type	.L_2il0floatpacket.79,@object
	.size	.L_2il0floatpacket.79,32
	.align 32
.L_2il0floatpacket.80:
	.long	0x00000000,0x00100000,0x00000000,0x00100000,0x00000000,0x00100000,0x00000000,0x00100000
	.type	.L_2il0floatpacket.80,@object
	.size	.L_2il0floatpacket.80,32
	.align 32
.L_2il0floatpacket.81:
	.long	0xffffffff,0x00000000,0xffffffff,0x00000000,0xffffffff,0x00000000,0xffffffff,0x00000000
	.type	.L_2il0floatpacket.81,@object
	.size	.L_2il0floatpacket.81,32
	.align 32
.L_2il0floatpacket.82:
	.long	0x00000000,0x80000000,0x00000000,0x80000000,0x00000000,0x80000000,0x00000000,0x80000000
	.type	.L_2il0floatpacket.82,@object
	.size	.L_2il0floatpacket.82,32
	.align 32
.L_2il0floatpacket.83:
	.long	0x00000000,0x3ff00000,0x00000000,0x3ff00000,0x00000000,0x3ff00000,0x00000000,0x3ff00000
	.type	.L_2il0floatpacket.83,@object
	.size	.L_2il0floatpacket.83,32
	.align 32
.L_2il0floatpacket.84:
	.long	0x000001ff,0x00000000,0x000001ff,0x00000000,0x000001ff,0x00000000,0x000001ff,0x00000000
	.type	.L_2il0floatpacket.84,@object
	.size	.L_2il0floatpacket.84,32
	.align 32
.L_2il0floatpacket.85:
	.long	0x00000000,0x39700000,0x00000000,0x39700000,0x00000000,0x39700000,0x00000000,0x39700000
	.type	.L_2il0floatpacket.85,@object
	.size	.L_2il0floatpacket.85,32
	.align 32
.L_2il0floatpacket.86:
	.long	0x00ffffff,0x00000000,0x00ffffff,0x00000000,0x00ffffff,0x00000000,0x00ffffff,0x00000000
	.type	.L_2il0floatpacket.86,@object
	.size	.L_2il0floatpacket.86,32
	.align 32
.L_2il0floatpacket.87:
	.long	0x00000000,0x3cb00000,0x00000000,0x3cb00000,0x00000000,0x3cb00000,0x00000000,0x3cb00000
	.type	.L_2il0floatpacket.87,@object
	.size	.L_2il0floatpacket.87,32
	.align 32
.L_2il0floatpacket.88:
	.long	0x00000fff,0x00000000,0x00000fff,0x00000000,0x00000fff,0x00000000,0x00000fff,0x00000000
	.type	.L_2il0floatpacket.88,@object
	.size	.L_2il0floatpacket.88,32
	.align 32
.L_2il0floatpacket.89:
	.long	0x33145c07,0x3cb1a626,0x33145c07,0x3cb1a626,0x33145c07,0x3cb1a626,0x33145c07,0x3cb1a626
	.type	.L_2il0floatpacket.89,@object
	.size	.L_2il0floatpacket.89,32
	.align 32
.L_2il0floatpacket.90:
	.long	0x54442d18,0x3f8921fb,0x54442d18,0x3f8921fb,0x54442d18,0x3f8921fb,0x54442d18,0x3f8921fb
	.type	.L_2il0floatpacket.90,@object
	.size	.L_2il0floatpacket.90,32
	.align 32
.L_2il0floatpacket.91:
	.long	0x33145c07,0x3c21a626,0x33145c07,0x3c21a626,0x33145c07,0x3c21a626,0x33145c07,0x3c21a626
	.type	.L_2il0floatpacket.91,@object
	.size	.L_2il0floatpacket.91,32
	.align 32
.L_2il0floatpacket.92:
	.long	0x00000078,0x00000000,0x00000078,0x00000000,0x00000078,0x00000000,0x00000078,0x00000000
	.type	.L_2il0floatpacket.92,@object
	.size	.L_2il0floatpacket.92,32
	.align 16
.L_2il0floatpacket.26:
	.long	0x000000ff,0x00000000,0x000000ff,0x00000000
	.type	.L_2il0floatpacket.26,@object
	.size	.L_2il0floatpacket.26,16
	.align 16
.L_2il0floatpacket.27:
	.long	0xf8000000,0xffffffff,0xf8000000,0xffffffff
	.type	.L_2il0floatpacket.27,@object
	.size	.L_2il0floatpacket.27,16
	.align 16
.L_2il0floatpacket.28:
	.long	0x00000000,0x3ff00000,0x00000000,0x3ff00000
	.type	.L_2il0floatpacket.28,@object
	.size	.L_2il0floatpacket.28,16
	.align 16
.L_2il0floatpacket.29:
	.long	0x00000000,0x7ff00000,0x00000000,0x7ff00000
	.type	.L_2il0floatpacket.29,@object
	.size	.L_2il0floatpacket.29,16
	.align 16
.L_2il0floatpacket.30:
	.long	0xffffffff,0x000fffff,0xffffffff,0x000fffff
	.type	.L_2il0floatpacket.30,@object
	.size	.L_2il0floatpacket.30,16
	.align 16
.L_2il0floatpacket.31:
	.long	0x00000000,0x00100000,0x00000000,0x00100000
	.type	.L_2il0floatpacket.31,@object
	.size	.L_2il0floatpacket.31,16
	.align 16
.L_2il0floatpacket.32:
	.long	0xffffffff,0x00000000,0xffffffff,0x00000000
	.type	.L_2il0floatpacket.32,@object
	.size	.L_2il0floatpacket.32,16
	.align 16
.L_2il0floatpacket.33:
	.long	0x00000000,0x80000000,0x00000000,0x80000000
	.type	.L_2il0floatpacket.33,@object
	.size	.L_2il0floatpacket.33,16
	.align 16
.L_2il0floatpacket.34:
	.long	0x00000000,0x42a80000,0x00000000,0x42a80000
	.type	.L_2il0floatpacket.34,@object
	.size	.L_2il0floatpacket.34,16
	.align 16
.L_2il0floatpacket.35:
	.long	0x00000000,0x39700000,0x00000000,0x39700000
	.type	.L_2il0floatpacket.35,@object
	.size	.L_2il0floatpacket.35,16
	.align 16
.L_2il0floatpacket.36:
	.long	0x00ffffff,0x00000000,0x00ffffff,0x00000000
	.type	.L_2il0floatpacket.36,@object
	.size	.L_2il0floatpacket.36,16
	.align 16
.L_2il0floatpacket.37:
	.long	0x00000000,0x3cb00000,0x00000000,0x3cb00000
	.type	.L_2il0floatpacket.37,@object
	.size	.L_2il0floatpacket.37,16
	.align 16
.L_2il0floatpacket.38:
	.long	0x00000fff,0x00000000,0x00000fff,0x00000000
	.type	.L_2il0floatpacket.38,@object
	.size	.L_2il0floatpacket.38,16
	.align 16
.L_2il0floatpacket.39:
	.long	0x54442d18,0x401921fb,0x54442d18,0x401921fb
	.type	.L_2il0floatpacket.39,@object
	.size	.L_2il0floatpacket.39,16
	.align 16
.L_2il0floatpacket.40:
	.long	0x54000000,0x401921fb,0x54000000,0x401921fb
	.type	.L_2il0floatpacket.40,@object
	.size	.L_2il0floatpacket.40,16
	.align 16
.L_2il0floatpacket.41:
	.long	0x11a62633,0x3e310b46,0x11a62633,0x3e310b46
	.type	.L_2il0floatpacket.41,@object
	.size	.L_2il0floatpacket.41,16
	.align 16
.L_2il0floatpacket.42:
	.long	0xffffffff,0x7fffffff,0xffffffff,0x7fffffff
	.type	.L_2il0floatpacket.42,@object
	.size	.L_2il0floatpacket.42,16
	.align 16
.L_2il0floatpacket.43:
	.long	0x00000000,0x3eb00000,0x00000000,0x3eb00000
	.type	.L_2il0floatpacket.43,@object
	.size	.L_2il0floatpacket.43,16
	.align 16
.L_2il0floatpacket.44:
	.long	0x000001ff,0x00000000,0x000001ff,0x00000000
	.type	.L_2il0floatpacket.44,@object
	.size	.L_2il0floatpacket.44,16
	.align 16
.L_2il0floatpacket.53:
	.long	0x33145c07,0x3cb1a626,0x33145c07,0x3cb1a626
	.type	.L_2il0floatpacket.53,@object
	.size	.L_2il0floatpacket.53,16
	.align 16
.L_2il0floatpacket.54:
	.long	0x00000008,0x00000008,0x00000008,0x00000008
	.type	.L_2il0floatpacket.54,@object
	.size	.L_2il0floatpacket.54,16
	.align 16
.L_2il0floatpacket.55:
	.long	0x54442d18,0x3f8921fb,0x54442d18,0x3f8921fb
	.type	.L_2il0floatpacket.55,@object
	.size	.L_2il0floatpacket.55,16
	.align 16
.L_2il0floatpacket.56:
	.long	0x33145c07,0x3c21a626,0x33145c07,0x3c21a626
	.type	.L_2il0floatpacket.56,@object
	.size	.L_2il0floatpacket.56,16
	.align 16
.L_2il0floatpacket.57:
	.long	0x00000078,0x00000000,0x00000078,0x00000000
	.type	.L_2il0floatpacket.57,@object
	.size	.L_2il0floatpacket.57,16
	.align 8
.L_2il0floatpacket.58:
	.long	0xffffffff,0xffffffff
	.type	.L_2il0floatpacket.58,@object
	.size	.L_2il0floatpacket.58,8
	.align 8
.L_2il0floatpacket.59:
	.long	0x00000000,0x7ff00000
	.type	.L_2il0floatpacket.59,@object
	.size	.L_2il0floatpacket.59,8
	.align 8
.L_2il0floatpacket.60:
	.long	0xffffffff,0x000fffff
	.type	.L_2il0floatpacket.60,@object
	.size	.L_2il0floatpacket.60,8
	.align 8
.L_2il0floatpacket.61:
	.long	0x00000000,0x00100000
	.type	.L_2il0floatpacket.61,@object
	.size	.L_2il0floatpacket.61,8
	.align 8
.L_2il0floatpacket.62:
	.long	0xffffffff,0x00000000
	.type	.L_2il0floatpacket.62,@object
	.size	.L_2il0floatpacket.62,8
	.align 8
.L_2il0floatpacket.63:
	.long	0x00000000,0x80000000
	.type	.L_2il0floatpacket.63,@object
	.size	.L_2il0floatpacket.63,8
	.align 8
.L_2il0floatpacket.64:
	.long	0x00000000,0x3ff00000
	.type	.L_2il0floatpacket.64,@object
	.size	.L_2il0floatpacket.64,8
	.align 8
.L_2il0floatpacket.65:
	.long	0x00000000,0x42a80000
	.type	.L_2il0floatpacket.65,@object
	.size	.L_2il0floatpacket.65,8
	.align 8
.L_2il0floatpacket.66:
	.long	0x000001ff,0x00000000
	.type	.L_2il0floatpacket.66,@object
	.size	.L_2il0floatpacket.66,8
	.align 8
.L_2il0floatpacket.67:
	.long	0x00000000,0x39700000
	.type	.L_2il0floatpacket.67,@object
	.size	.L_2il0floatpacket.67,8
	.align 8
.L_2il0floatpacket.68:
	.long	0x00ffffff,0x00000000
	.type	.L_2il0floatpacket.68,@object
	.size	.L_2il0floatpacket.68,8
	.align 8
.L_2il0floatpacket.69:
	.long	0x00000000,0x3cb00000
	.type	.L_2il0floatpacket.69,@object
	.size	.L_2il0floatpacket.69,8
	.align 8
.L_2il0floatpacket.70:
	.long	0x00000fff,0x00000000
	.type	.L_2il0floatpacket.70,@object
	.size	.L_2il0floatpacket.70,8
	.align 8
.L_2il0floatpacket.71:
	.long	0x54442d18,0x401921fb
	.type	.L_2il0floatpacket.71,@object
	.size	.L_2il0floatpacket.71,8
	.align 8
.L_2il0floatpacket.72:
	.long	0x33145c07,0x3cb1a626
	.type	.L_2il0floatpacket.72,@object
	.size	.L_2il0floatpacket.72,8
	.align 8
.L_2il0floatpacket.73:
	.long	0xffffffff,0x7fffffff
	.type	.L_2il0floatpacket.73,@object
	.size	.L_2il0floatpacket.73,8
	.align 8
.L_2il0floatpacket.74:
	.long	0x00000000,0x3eb00000
	.type	.L_2il0floatpacket.74,@object
	.size	.L_2il0floatpacket.74,8
	.align 8
.L_2il0floatpacket.76:
	.long	0x54442d18,0x3f8921fb
	.type	.L_2il0floatpacket.76,@object
	.size	.L_2il0floatpacket.76,8
	.align 8
.L_2il0floatpacket.77:
	.long	0x33145c07,0x3c21a626
	.type	.L_2il0floatpacket.77,@object
	.size	.L_2il0floatpacket.77,8
	.align 8
_vmldTanHATab:
	.long	0
	.long	0
	.long	0
	.long	2146435072
	.type	_vmldTanHATab,@object
	.size	_vmldTanHATab,16
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
