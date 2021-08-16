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
	.file "svml_s_sinh.c"
	.text
..TXTST0:
.L_2__routine_start___svml_sinhf4_ha_l9_0:
# -- Begin  __svml_sinhf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinhf4_ha_l9
# --- __svml_sinhf4_ha_l9(__m128)
__svml_sinhf4_ha_l9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #426.1
        .byte     15                                            #492.540
        .byte     30                                            #492.540
        .byte     250                                           #492.540
	.cfi_startproc
..___tag_value___svml_sinhf4_ha_l9.1:
..L2:
                                                          #426.1
        pushq     %rbp                                          #426.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #426.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #426.1
        subq      $192, %rsp                                    #426.1
        vmovaps   %xmm0, %xmm11                                 #426.1
        vandps    1152+__svml_ssinh_ha_data_internal(%rip), %xmm11, %xmm10 #455.17
        lea       1484+__svml_ssinh_ha_data_internal(%rip), %r8 #473.509
        vmovups   1280+__svml_ssinh_ha_data_internal(%rip), %xmm0 #448.49
        vxorps    %xmm11, %xmm10, %xmm8                         #456.16
        vmovups   960+__svml_ssinh_ha_data_internal(%rip), %xmm5 #453.48
        vfmadd213ps %xmm0, %xmm8, %xmm5                         #457.13
        vmovups   1344+__svml_ssinh_ha_data_internal(%rip), %xmm3 #450.23
        vpcmpgtd  1408+__svml_ssinh_ha_data_internal(%rip), %xmm8, %xmm12 #459.23
        vmovmskps %xmm12, %eax                                  #460.44
        vmovups   5568+__svml_ssinh_ha_data_internal(%rip), %xmm7 #454.25
        vxorps    %xmm0, %xmm5, %xmm9                           #461.13
        vsubps    %xmm0, %xmm5, %xmm1                           #470.13
        vpsubd    %xmm9, %xmm3, %xmm13                          #463.19
        vpsrld    $28, %xmm13, %xmm14                           #464.19
        vpslld    $4, %xmm14, %xmm15                            #465.19
        vpor      %xmm9, %xmm15, %xmm6                          #466.19
        vmovups   1024+__svml_ssinh_ha_data_internal(%rip), %xmm5 #471.13
        vpand     %xmm3, %xmm6, %xmm6                           #467.19
        vfnmadd213ps %xmm8, %xmm1, %xmm5                        #471.13
        vpslld    $4, %xmm6, %xmm2                              #473.179
        vpsubd    %xmm6, %xmm9, %xmm9                           #468.20
        vmovd     %xmm2, %edx                                   #473.238
        vfnmadd231ps 1088+__svml_ssinh_ha_data_internal(%rip), %xmm1, %xmm5 #472.13
        vmovd     -4(%rdx,%r8), %xmm4                           #473.986
        vmovd     (%rdx,%r8), %xmm0                             #474.986
        vpextrd   $1, %xmm2, %ecx                               #473.302
        vpextrd   $2, %xmm2, %esi                               #473.369
        vpextrd   $3, %xmm2, %edi                               #473.436
        vmovd     -4(%rcx,%r8), %xmm12                          #473.1069
        vmovd     (%rcx,%r8), %xmm1                             #474.1069
        vpunpcklqdq %xmm12, %xmm4, %xmm15                       #473.966
        vmovd     -4(%rsi,%r8), %xmm13                          #473.1173
        vmovd     (%rsi,%r8), %xmm2                             #474.1173
        vmovd     -4(%rdi,%r8), %xmm14                          #473.1256
        vmovd     (%rdi,%r8), %xmm12                            #474.1256
        vpunpcklqdq %xmm14, %xmm13, %xmm3                       #473.1153
        vpunpcklqdq %xmm1, %xmm0, %xmm13                        #474.966
        vpcmpgtd  %xmm6, %xmm7, %xmm1                           #475.15
        vpunpcklqdq %xmm12, %xmm2, %xmm14                       #474.1153
        vshufps   $136, %xmm3, %xmm15, %xmm4                    #473.946
        vshufps   $136, %xmm14, %xmm13, %xmm3                   #474.946
        vaddps    %xmm4, %xmm4, %xmm2                           #477.13
        vfmadd213ps %xmm4, %xmm5, %xmm3                         #478.15
        vmovd     -8(%rdx,%r8), %xmm7                           #479.986
        vmovd     -8(%rcx,%r8), %xmm0                           #479.1069
        vpunpcklqdq %xmm0, %xmm7, %xmm12                        #479.966
        vandps    %xmm1, %xmm8, %xmm0                           #480.16
        vmovd     -8(%rsi,%r8), %xmm4                           #479.1173
        vandnps   %xmm2, %xmm1, %xmm1                           #483.13
        vmovd     -8(%rdi,%r8), %xmm7                           #479.1256
        vorps     %xmm0, %xmm1, %xmm0                           #485.13
        vpunpcklqdq %xmm7, %xmm4, %xmm13                        #479.1153
        vshufps   $136, %xmm13, %xmm12, %xmm14                  #479.946
        vfmadd213ps %xmm14, %xmm5, %xmm3                        #481.15
        vmovd     -12(%rdx,%r8), %xmm8                          #482.986
        vmovd     -12(%rcx,%r8), %xmm4                          #482.1069
        vpunpcklqdq %xmm4, %xmm8, %xmm12                        #482.966
        vmovd     -12(%rsi,%r8), %xmm7                          #482.1173
        vmovd     -12(%rdi,%r8), %xmm8                          #482.1256
        vpunpcklqdq %xmm8, %xmm7, %xmm13                        #482.1153
        vshufps   $136, %xmm13, %xmm12, %xmm14                  #482.946
        vfmadd213ps %xmm14, %xmm5, %xmm3                        #484.15
        vaddps    %xmm3, %xmm0, %xmm2                           #486.15
        vpslld    $19, %xmm9, %xmm0                             #469.20
        vpaddd    %xmm0, %xmm2, %xmm1                           #489.15
        vorps     %xmm1, %xmm10, %xmm0                          #490.14
        testl     %eax, %eax                                    #492.52
        jne       ..B1.3        # Prob 5%                       #492.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm11
..B1.2:                         # Preds ..B1.3 ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #495.12
        popq      %rbp                                          #495.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #495.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm11, 64(%rsp)                              #492.193
        vmovups   %xmm0, 128(%rsp)                              #492.264
        je        ..B1.2        # Prob 95%                      #492.368
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #492.448
        movq      %r12, 8(%rsp)                                 #492.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #492.448
        movq      %r13, (%rsp)                                  #492.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #492.448
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #492.511
        jc        ..B1.10       # Prob 5%                       #492.511
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #492.464
        cmpl      $4, %r12d                                     #492.459
        jl        ..B1.7        # Prob 82%                      #492.459
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #492.666
        jmp       ..B1.2        # Prob 100%                     #492.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #492.540
        lea       128(%rsp,%r12,4), %rsi                        #492.540
..___tag_value___svml_sinhf4_ha_l9.19:
#       __svml_ssinh_ha_cout_rare_internal(const float *, float *)
        call      __svml_ssinh_ha_cout_rare_internal            #492.540
..___tag_value___svml_sinhf4_ha_l9.20:
        jmp       ..B1.8        # Prob 100%                     #492.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_sinhf4_ha_l9,@function
	.size	__svml_sinhf4_ha_l9,.-__svml_sinhf4_ha_l9
..LN__svml_sinhf4_ha_l9.0:
	.data
# -- End  __svml_sinhf4_ha_l9
	.text
.L_2__routine_start___svml_sinhf16_ha_z0_1:
# -- Begin  __svml_sinhf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinhf16_ha_z0
# --- __svml_sinhf16_ha_z0(__m512)
__svml_sinhf16_ha_z0:
# parameter 1: %zmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #500.1
        .byte     15                                            #596.760
        .byte     30                                            #596.760
        .byte     250                                           #596.760
	.cfi_startproc
..___tag_value___svml_sinhf16_ha_z0.22:
..L23:
                                                         #500.1
        pushq     %rbp                                          #500.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #500.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #500.1
        subq      $192, %rsp                                    #500.1
        vmovups   1152+__svml_ssinh_ha_data_internal(%rip), %zmm10 #543.48
        vmovups   960+__svml_ssinh_ha_data_internal(%rip), %zmm9 #549.17
        vmovups   576+__svml_ssinh_ha_data_internal(%rip), %zmm5 #550.61
        vmovups   1024+__svml_ssinh_ha_data_internal(%rip), %zmm1 #536.49
        vmovups   1088+__svml_ssinh_ha_data_internal(%rip), %zmm8 #537.49
        vmovups   896+__svml_ssinh_ha_data_internal(%rip), %zmm12 #542.49
        vmovups   832+__svml_ssinh_ha_data_internal(%rip), %zmm13 #541.49
        vpternlogd $255, %zmm4, %zmm4, %zmm4                    #547.21
        vmovaps   %zmm0, %zmm15                                 #500.1
        vandps    %zmm15, %zmm10, %zmm14                        #544.17
        vmovups   512+__svml_ssinh_ha_data_internal(%rip), %zmm0 #538.51
        vxorps    %zmm15, %zmm14, %zmm11                        #545.16
        vfmadd213ps {rn-sae}, %zmm0, %zmm11, %zmm9              #549.17
        vpcmpd    $2, 704+__svml_ssinh_ha_data_internal(%rip), %zmm11, %k1 #547.88
        vcmpps    $0, {sae}, %zmm9, %zmm5, %k2                  #551.24
        vmovups   256+__svml_ssinh_ha_data_internal(%rip), %zmm5 #562.280
        vblendmps %zmm0, %zmm9, %zmm6{%k2}                      #552.25
        vsubps    {rn-sae}, %zmm0, %zmm6, %zmm3                 #553.19
        vmovups   128+__svml_ssinh_ha_data_internal(%rip), %zmm0 #572.284
        vpandnd   %zmm11, %zmm11, %zmm4{%k1}                    #547.21
        vfnmadd231ps {rn-sae}, %zmm1, %zmm3, %zmm11             #554.17
        vptestmd  %zmm4, %zmm4, %k0                             #548.33
        vmovups   __svml_ssinh_ha_data_internal(%rip), %zmm4    #561.280
        vfnmadd231ps {rn-sae}, %zmm8, %zmm3, %zmm11             #555.17
        kmovw     %k0, %esi                                     #548.33
        vmulps    {rn-sae}, %zmm11, %zmm11, %zmm9               #556.20
        vmulps    {rn-sae}, %zmm9, %zmm12, %zmm12               #588.28
        vfmadd213ps {rn-sae}, %zmm11, %zmm11, %zmm12            #589.26
        vmulps    {rn-sae}, %zmm9, %zmm13, %zmm11               #590.25
        vpandd    640+__svml_ssinh_ha_data_internal(%rip), %zmm6, %zmm7 #559.21
        vpxord    %zmm7, %zmm6, %zmm2                           #560.17
        vpslld    $18, %zmm2, %zmm8                             #563.19
        vmovups   384+__svml_ssinh_ha_data_internal(%rip), %zmm2 #573.284
        vpermt2ps 320+__svml_ssinh_ha_data_internal(%rip), %zmm7, %zmm5 #562.280
        vpermt2ps 64+__svml_ssinh_ha_data_internal(%rip), %zmm7, %zmm4 #561.280
        vpermt2ps 448+__svml_ssinh_ha_data_internal(%rip), %zmm7, %zmm2 #573.284
        vpsubd    %zmm8, %zmm5, %zmm3                           #568.20
        vpaddd    %zmm8, %zmm4, %zmm6                           #565.20
        vpermt2ps 192+__svml_ssinh_ha_data_internal(%rip), %zmm7, %zmm0 #572.284
        vandnps   %zmm2, %zmm10, %zmm7                          #577.26
        vsubps    {rn-sae}, %zmm3, %zmm6, %zmm5                 #571.20
        vpaddd    %zmm8, %zmm0, %zmm1                           #575.24
        vpsubd    %zmm8, %zmm2, %zmm0                           #581.24
        vsubps    {rn-sae}, %zmm6, %zmm5, %zmm10                #584.28
        vaddps    {rn-sae}, %zmm3, %zmm6, %zmm4                 #570.20
        vcmpps    $17, {sae}, %zmm8, %zmm7, %k3                 #579.24
        vaddps    {rn-sae}, %zmm10, %zmm3, %zmm2                #585.28
        vxorps    %zmm0, %zmm0, %zmm0{%k3}                      #583.27
        vsubps    {rn-sae}, %zmm2, %zmm1, %zmm1                 #586.31
        vsubps    {rn-sae}, %zmm0, %zmm1, %zmm0                 #587.31
        vfmadd213ps {rn-sae}, %zmm0, %zmm5, %zmm11              #591.27
        vfmadd213ps {rn-sae}, %zmm11, %zmm12, %zmm4             #592.27
        vaddps    {rn-sae}, %zmm5, %zmm4, %zmm13                #593.29
        vorps     %zmm13, %zmm14, %zmm0                         #594.14
        testl     %esi, %esi                                    #596.52
        jne       ..B2.3        # Prob 5%                       #596.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm15
..B2.2:                         # Preds ..B2.9 ..B2.7 ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #599.12
        popq      %rbp                                          #599.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #599.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #596.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm15
..B2.4:                         # Preds ..B2.3
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #596.249
        movl      %edx, %eax                                    #596.303
        orl       $8064, %eax                                   #596.303
        cmpl      %eax, %edx                                    #596.332
        je        ..B2.6        # Prob 78%                      #596.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm15
..B2.5:                         # Preds ..B2.4
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #596.344
        vldmxcsr  32(%rsp)                                      #596.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm15
..B2.6:                         # Preds ..B2.5 ..B2.4
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm15, 64(%rsp)                              #596.404
        vmovups   %zmm0, 128(%rsp)                              #596.478
        testl     %esi, %esi                                    #596.586
        jne       ..B2.11       # Prob 5%                       #596.586
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B2.7:                         # Preds ..B2.14 ..B2.6
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #596.932
        je        ..B2.2        # Prob 78%                      #596.932
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B2.8:                         # Preds ..B2.7
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #596.957
        movl      32(%rsp), %eax                                #596.957
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B2.9:                         # Preds ..B2.8
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #596.944
        orl       %edx, %eax                                    #596.944
        movl      %eax, 32(%rsp)                                #596.944
        vldmxcsr  32(%rsp)                                      #596.944
        jmp       ..B2.2        # Prob 100%                     #596.944
                                # LOE rbx r12 r13 r14 r15 zmm0
..B2.11:                        # Preds ..B2.6
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #596.668
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B2.18:                        # Preds ..B2.11
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
..B2.12:                        # Preds ..B2.13 ..B2.18
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #596.731
        jc        ..B2.15       # Prob 5%                       #596.731
                                # LOE rbx r15 r12d r13d r14d
..B2.13:                        # Preds ..B2.15 ..B2.12
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #596.684
        cmpl      $16, %ebx                                     #596.679
        jl        ..B2.12       # Prob 82%                      #596.679
                                # LOE rbx r15 r12d r13d r14d
..B2.14:                        # Preds ..B2.13
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
        vmovups   128(%rsp), %zmm0                              #596.887
        jmp       ..B2.7        # Prob 100%                     #596.887
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B2.15:                        # Preds ..B2.12
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,4), %rdi                         #596.760
        lea       128(%rsp,%rbx,4), %rsi                        #596.760
..___tag_value___svml_sinhf16_ha_z0.50:
#       __svml_ssinh_ha_cout_rare_internal(const float *, float *)
        call      __svml_ssinh_ha_cout_rare_internal            #596.760
..___tag_value___svml_sinhf16_ha_z0.51:
        jmp       ..B2.13       # Prob 100%                     #596.760
        .align    16,0x90
                                # LOE r15 ebx r12d r13d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_sinhf16_ha_z0,@function
	.size	__svml_sinhf16_ha_z0,.-__svml_sinhf16_ha_z0
..LN__svml_sinhf16_ha_z0.1:
	.data
# -- End  __svml_sinhf16_ha_z0
	.text
.L_2__routine_start___svml_sinhf4_ha_e9_2:
# -- Begin  __svml_sinhf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinhf4_ha_e9
# --- __svml_sinhf4_ha_e9(__m128)
__svml_sinhf4_ha_e9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #909.1
        .byte     15                                            #975.540
        .byte     30                                            #975.540
        .byte     250                                           #975.540
	.cfi_startproc
..___tag_value___svml_sinhf4_ha_e9.53:
..L54:
                                                         #909.1
        pushq     %rbp                                          #909.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #909.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #909.1
        subq      $192, %rsp                                    #909.1
        movaps    %xmm0, %xmm9                                  #909.1
        movups    1152+__svml_ssinh_ha_data_internal(%rip), %xmm0 #938.17
        lea       1484+__svml_ssinh_ha_data_internal(%rip), %r8 #956.533
        andps     %xmm9, %xmm0                                  #938.17
        movaps    %xmm0, %xmm8                                  #939.16
        movups    960+__svml_ssinh_ha_data_internal(%rip), %xmm14 #940.25
        pxor      %xmm9, %xmm8                                  #939.16
        mulps     %xmm8, %xmm14                                 #940.25
        movaps    %xmm8, %xmm5                                  #954.13
        movups    1280+__svml_ssinh_ha_data_internal(%rip), %xmm12 #931.49
        movaps    %xmm8, %xmm10                                 #942.27
        movaps    %xmm12, %xmm7                                 #944.13
        addps     %xmm12, %xmm14                                #940.13
        pxor      %xmm14, %xmm7                                 #944.13
        subps     %xmm12, %xmm14                                #953.13
        movups    1344+__svml_ssinh_ha_data_internal(%rip), %xmm11 #933.27
        movaps    %xmm11, %xmm6                                 #946.23
        movups    1024+__svml_ssinh_ha_data_internal(%rip), %xmm13 #954.35
        psubd     %xmm7, %xmm6                                  #946.23
        mulps     %xmm14, %xmm13                                #954.35
        psrld     $28, %xmm6                                    #947.23
        pslld     $4, %xmm6                                     #948.23
        subps     %xmm13, %xmm5                                 #954.13
        por       %xmm7, %xmm6                                  #949.23
        movups    1088+__svml_ssinh_ha_data_internal(%rip), %xmm15 #955.32
        pand      %xmm11, %xmm6                                 #950.23
        mulps     %xmm14, %xmm15                                #955.32
        movdqa    %xmm6, %xmm2                                  #956.183
        pslld     $4, %xmm2                                     #956.183
        psubd     %xmm6, %xmm7                                  #951.24
        movd      %xmm2, %edx                                   #956.246
        pslld     $19, %xmm7                                    #952.24
        pcmpgtd   1408+__svml_ssinh_ha_data_internal(%rip), %xmm10 #942.27
        subps     %xmm15, %xmm5                                 #955.13
        movmskps  %xmm10, %eax                                  #943.44
        movd      (%rdx,%r8), %xmm14                            #957.1010
        pextrd    $1, %xmm2, %ecx                               #956.314
        pextrd    $2, %xmm2, %esi                               #956.385
        pextrd    $3, %xmm2, %edi                               #956.456
        movd      (%rcx,%r8), %xmm11                            #957.1093
        movd      (%rsi,%r8), %xmm13                            #957.1197
        movd      (%rdi,%r8), %xmm12                            #957.1280
        punpcklqdq %xmm11, %xmm14                               #957.990
        punpcklqdq %xmm12, %xmm13                               #957.1177
        shufps    $136, %xmm13, %xmm14                          #957.970
        mulps     %xmm5, %xmm14                                 #961.27
        movd      -4(%rdi,%r8), %xmm1                           #956.1280
        movd      -4(%rdx,%r8), %xmm4                           #956.1010
        movd      -4(%rcx,%r8), %xmm3                           #956.1093
        movd      -4(%rsi,%r8), %xmm10                          #956.1197
        punpcklqdq %xmm3, %xmm4                                 #956.990
        punpcklqdq %xmm1, %xmm10                                #956.1177
        shufps    $136, %xmm10, %xmm4                           #956.970
        movaps    %xmm4, %xmm2                                  #960.13
        movd      -8(%rsi,%r8), %xmm11                          #962.1197
        addps     %xmm4, %xmm2                                  #960.13
        addps     %xmm14, %xmm4                                 #961.15
        mulps     %xmm5, %xmm4                                  #964.27
        movd      -8(%rdx,%r8), %xmm1                           #962.1010
        movd      -8(%rcx,%r8), %xmm15                          #962.1093
        movd      -8(%rdi,%r8), %xmm10                          #962.1280
        punpcklqdq %xmm15, %xmm1                                #962.990
        punpcklqdq %xmm10, %xmm11                               #962.1177
        shufps    $136, %xmm11, %xmm1                           #962.970
        movd      -12(%rdx,%r8), %xmm11                         #965.1010
        addps     %xmm4, %xmm1                                  #964.15
        mulps     %xmm1, %xmm5                                  #967.27
        movd      -12(%rcx,%r8), %xmm4                          #965.1093
        punpcklqdq %xmm4, %xmm11                                #965.990
        movd      -12(%rsi,%r8), %xmm10                         #965.1197
        movd      -12(%rdi,%r8), %xmm4                          #965.1280
        punpcklqdq %xmm4, %xmm10                                #965.1177
        shufps    $136, %xmm10, %xmm11                          #965.970
        movups    5568+__svml_ssinh_ha_data_internal(%rip), %xmm3 #958.19
        addps     %xmm5, %xmm11                                 #967.15
        pcmpgtd   %xmm6, %xmm3                                  #958.19
        andps     %xmm3, %xmm8                                  #963.16
        andnps    %xmm2, %xmm3                                  #966.13
        orps      %xmm8, %xmm3                                  #968.13
        addps     %xmm11, %xmm3                                 #969.15
        paddd     %xmm7, %xmm3                                  #972.15
        orps      %xmm3, %xmm0                                  #973.14
        testl     %eax, %eax                                    #975.52
        jne       ..B3.3        # Prob 5%                       #975.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm9
..B3.2:                         # Preds ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #978.12
        popq      %rbp                                          #978.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #978.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm9, 64(%rsp)                               #975.193
        movups    %xmm0, 128(%rsp)                              #975.264
                                # LOE rbx r12 r13 r14 r15 eax
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #975.448
        movq      %r12, 8(%rsp)                                 #975.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #975.448
        movq      %r13, (%rsp)                                  #975.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #975.448
                                # LOE rbx r12 r14 r15 r13d
..B3.7:                         # Preds ..B3.8 ..B3.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #975.511
        jc        ..B3.10       # Prob 5%                       #975.511
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #975.464
        cmpl      $4, %r12d                                     #975.459
        jl        ..B3.7        # Prob 82%                      #975.459
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #975.666
        jmp       ..B3.2        # Prob 100%                     #975.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #975.540
        lea       128(%rsp,%r12,4), %rsi                        #975.540
..___tag_value___svml_sinhf4_ha_e9.71:
#       __svml_ssinh_ha_cout_rare_internal(const float *, float *)
        call      __svml_ssinh_ha_cout_rare_internal            #975.540
..___tag_value___svml_sinhf4_ha_e9.72:
        jmp       ..B3.8        # Prob 100%                     #975.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_sinhf4_ha_e9,@function
	.size	__svml_sinhf4_ha_e9,.-__svml_sinhf4_ha_e9
..LN__svml_sinhf4_ha_e9.2:
	.data
# -- End  __svml_sinhf4_ha_e9
	.text
.L_2__routine_start___svml_sinhf8_ha_e9_3:
# -- Begin  __svml_sinhf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinhf8_ha_e9
# --- __svml_sinhf8_ha_e9(__m256)
__svml_sinhf8_ha_e9:
# parameter 1: %ymm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #983.1
        .byte     15                                            #1049.546
        .byte     30                                            #1049.546
        .byte     250                                           #1049.546
	.cfi_startproc
..___tag_value___svml_sinhf8_ha_e9.74:
..L75:
                                                         #983.1
        pushq     %rbp                                          #983.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #983.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #983.1
        pushq     %r13                                          #983.1
        subq      $184, %rsp                                    #983.1
        lea       1484+__svml_ssinh_ha_data_internal(%rip), %r10 #1030.878
        vmovaps   %ymm0, %ymm7                                  #983.1
        vandps    1152+__svml_ssinh_ha_data_internal(%rip), %ymm7, %ymm6 #1012.17
        vxorps    %ymm7, %ymm6, %ymm0                           #1013.16
        vmovups   1280+__svml_ssinh_ha_data_internal(%rip), %ymm14 #1005.52
        vmulps    960+__svml_ssinh_ha_data_internal(%rip), %ymm0, %ymm5 #1014.28
        vmovups   1408+__svml_ssinh_ha_data_internal(%rip), %xmm2 #1006.29
        vmovups   1344+__svml_ssinh_ha_data_internal(%rip), %xmm3 #1007.27
        vmovups   5568+__svml_ssinh_ha_data_internal(%rip), %xmm8 #1011.29
        vaddps    %ymm5, %ymm14, %ymm1                          #1014.13
        vpxor     %xmm5, %xmm5, %xmm5                           #1017.62
        vpcmpgtd  %xmm2, %xmm0, %xmm12                          #1016.27
        vextractf128 $1, %ymm0, %xmm4                           #1015.107
        vpcmpgtd  %xmm2, %xmm4, %xmm11                          #1016.104
        vpackssdw %xmm11, %xmm12, %xmm15                        #1017.79
        vpacksswb %xmm5, %xmm15, %xmm4                          #1017.62
        vpmovmskb %xmm4, %r11d                                  #1017.43
        vxorps    %ymm14, %ymm1, %ymm4                          #1018.13
        vsubps    %ymm14, %ymm1, %ymm14                         #1027.13
        vmulps    1024+__svml_ssinh_ha_data_internal(%rip), %ymm14, %ymm1 #1028.38
        vpsubd    %xmm4, %xmm3, %xmm2                           #1020.23
        vextractf128 $1, %ymm4, %xmm5                           #1019.104
        vpsrld    $28, %xmm2, %xmm10                            #1021.23
        vpslld    $4, %xmm10, %xmm9                             #1022.23
        vpor      %xmm4, %xmm9, %xmm13                          #1023.23
        vpsubd    %xmm5, %xmm3, %xmm12                          #1020.92
        vpand     %xmm3, %xmm13, %xmm2                          #1024.23
        vpsrld    $28, %xmm12, %xmm11                           #1021.84
        vpslld    $4, %xmm2, %xmm9                              #1030.183
        vpslld    $4, %xmm11, %xmm15                            #1022.79
        vpor      %xmm5, %xmm15, %xmm10                         #1023.87
        vpand     %xmm3, %xmm10, %xmm3                          #1024.96
        vsubps    %ymm1, %ymm0, %ymm10                          #1028.13
        vpslld    $4, %xmm3, %xmm13                             #1030.245
        vmovd     %xmm9, %r9d                                   #1030.308
        vmovd     %xmm13, %ecx                                  #1030.589
        vmulps    1088+__svml_ssinh_ha_data_internal(%rip), %ymm14, %ymm1 #1029.35
        vmovd     -4(%r9,%r10), %xmm14                          #1030.1863
        vpextrd   $1, %xmm9, %r8d                               #1030.376
        vpextrd   $2, %xmm9, %edi                               #1030.447
        vpextrd   $3, %xmm9, %esi                               #1030.518
        vmovd     -4(%r8,%r10), %xmm12                          #1030.1952
        vmovd     -4(%rdi,%r10), %xmm11                         #1030.2062
        vmovd     -4(%rsi,%r10), %xmm15                         #1030.2151
        vpextrd   $1, %xmm13, %edx                              #1030.657
        vsubps    %ymm1, %ymm10, %ymm1                          #1029.13
        vpunpcklqdq %xmm12, %xmm14, %xmm10                      #1030.1843
        vpunpcklqdq %xmm15, %xmm11, %xmm9                       #1030.2042
        vpextrd   $2, %xmm13, %eax                              #1030.728
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
        vpextrd   $3, %xmm13, %r13d                             #1030.799
        vshufps   $136, %xmm9, %xmm10, %xmm9                    #1030.1823
        vmovd     -4(%rcx,%r10), %xmm13                         #1030.2304
        vmovd     -4(%rdx,%r10), %xmm10                         #1030.2393
        vpunpcklqdq %xmm10, %xmm13, %xmm12                      #1030.2284
        vmovd     -4(%rax,%r10), %xmm13                         #1030.2503
        vmovd     -4(%r13,%r10), %xmm14                         #1030.2592
        vpunpcklqdq %xmm14, %xmm13, %xmm11                      #1030.2483
        vshufps   $136, %xmm11, %xmm12, %xmm15                  #1030.2264
        vmovd     (%r8,%r10), %xmm13                            #1031.1952
        vmovd     (%rsi,%r10), %xmm14                           #1031.2151
        vinsertf128 $1, %xmm15, %ymm9, %ymm10                   #1030.1759
        vmovd     (%r9,%r10), %xmm9                             #1031.1863
        vpunpcklqdq %xmm13, %xmm9, %xmm12                       #1031.1843
        vmovd     (%rdi,%r10), %xmm9                            #1031.2062
        vpunpcklqdq %xmm14, %xmm9, %xmm11                       #1031.2042
        vshufps   $136, %xmm11, %xmm12, %xmm15                  #1031.1823
        vmovd     (%rcx,%r10), %xmm9                            #1031.2304
        vmovd     (%rdx,%r10), %xmm13                           #1031.2393
        vmovd     (%rax,%r10), %xmm14                           #1031.2503
        vmovd     (%r13,%r10), %xmm12                           #1031.2592
        vpunpcklqdq %xmm13, %xmm9, %xmm11                       #1031.2284
        vpunpcklqdq %xmm12, %xmm14, %xmm9                       #1031.2483
        vshufps   $136, %xmm9, %xmm11, %xmm13                   #1031.2264
        vaddps    %ymm10, %ymm10, %ymm14                        #1034.13
        vmovd     -8(%r8,%r10), %xmm11                          #1036.1952
        vinsertf128 $1, %xmm13, %ymm15, %ymm9                   #1031.1759
        vpcmpgtd  %xmm2, %xmm8, %xmm15                          #1032.19
        vpcmpgtd  %xmm3, %xmm8, %xmm8                           #1032.92
        vpsubd    %xmm2, %xmm4, %xmm2                           #1025.24
        vpsubd    %xmm3, %xmm5, %xmm3                           #1025.90
        vpslld    $19, %xmm2, %xmm4                             #1026.24
        vpslld    $19, %xmm3, %xmm3                             #1026.87
        vinsertf128 $1, %xmm8, %ymm15, %ymm13                   #1033.16
        vmulps    %ymm1, %ymm9, %ymm8                           #1035.30
        vmovd     -8(%rdi,%r10), %xmm15                         #1036.2062
        vaddps    %ymm8, %ymm10, %ymm12                         #1035.15
        vmovd     -8(%r9,%r10), %xmm10                          #1036.1863
        vpunpcklqdq %xmm11, %xmm10, %xmm9                       #1036.1843
        vmovd     -8(%rsi,%r10), %xmm10                         #1036.2151
        vpunpcklqdq %xmm10, %xmm15, %xmm8                       #1036.2042
        vshufps   $136, %xmm8, %xmm9, %xmm11                    #1036.1823
        vmovd     -8(%rcx,%r10), %xmm15                         #1036.2304
        vmovd     -8(%rdx,%r10), %xmm10                         #1036.2393
        vmovd     -8(%rax,%r10), %xmm9                          #1036.2503
        vmovd     -8(%r13,%r10), %xmm8                          #1036.2592
        vpunpcklqdq %xmm10, %xmm15, %xmm15                      #1036.2284
        vpunpcklqdq %xmm8, %xmm9, %xmm10                        #1036.2483
        vshufps   $136, %xmm10, %xmm15, %xmm9                   #1036.2264
        vandps    %ymm13, %ymm0, %ymm10                         #1037.16
        vmulps    %ymm12, %ymm1, %ymm0                          #1038.30
        vmovd     -12(%r9,%r10), %xmm8                          #1039.1863
        vmovd     -12(%rsi,%r10), %xmm12                        #1039.2151
        vandnps   %ymm14, %ymm13, %ymm13                        #1040.13
        vorps     %ymm10, %ymm13, %ymm10                        #1042.13
        vinsertf128 $1, %xmm9, %ymm11, %ymm11                   #1036.1759
        vaddps    %ymm0, %ymm11, %ymm9                          #1038.15
        vmovd     -12(%r8,%r10), %xmm0                          #1039.1952
        vmovd     -12(%rdi,%r10), %xmm11                        #1039.2062
        vpunpcklqdq %xmm0, %xmm8, %xmm15                        #1039.1843
        vpunpcklqdq %xmm12, %xmm11, %xmm8                       #1039.2042
        vmulps    %ymm9, %ymm1, %ymm1                           #1041.30
        vshufps   $136, %xmm8, %xmm15, %xmm8                    #1039.1823
        vmovd     -12(%rcx,%r10), %xmm0                         #1039.2304
        vmovd     -12(%rdx,%r10), %xmm11                        #1039.2393
        vmovd     -12(%rax,%r10), %xmm12                        #1039.2503
        vmovd     -12(%r13,%r10), %xmm15                        #1039.2592
        vpunpcklqdq %xmm11, %xmm0, %xmm0                        #1039.2284
        vpunpcklqdq %xmm15, %xmm12, %xmm11                      #1039.2483
        vshufps   $136, %xmm11, %xmm0, %xmm12                   #1039.2264
        vinsertf128 $1, %xmm12, %ymm8, %ymm8                    #1039.1759
        vaddps    %ymm1, %ymm8, %ymm9                           #1041.15
        vaddps    %ymm9, %ymm10, %ymm14                         #1043.15
        vpaddd    %xmm4, %xmm14, %xmm2                          #1046.61
        vextractf128 $1, %ymm14, %xmm0                          #1044.106
        vpaddd    %xmm3, %xmm0, %xmm1                           #1046.96
        vinsertf128 $1, %xmm1, %ymm2, %ymm4                     #1046.15
        vorps     %ymm4, %ymm6, %ymm0                           #1047.14
        testb     %r11b, %r11b                                  #1049.52
        jne       ..B4.3        # Prob 5%                       #1049.52
                                # LOE rbx r12 r14 r15 r11d ymm0 ymm7
..B4.2:                         # Preds ..B4.3 ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        addq      $184, %rsp                                    #1052.12
	.cfi_restore 13
        popq      %r13                                          #1052.12
        movq      %rbp, %rsp                                    #1052.12
        popq      %rbp                                          #1052.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1052.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm7, 64(%rsp)                               #1049.196
        vmovups   %ymm0, 128(%rsp)                              #1049.270
        testl     %r11d, %r11d                                  #1049.374
        je        ..B4.2        # Prob 95%                      #1049.374
                                # LOE rbx r12 r14 r15 r11d ymm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %r13d, %r13d                                  #1049.454
                                # LOE rbx r12 r13 r14 r15 r11d
..B4.13:                        # Preds ..B4.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %r11d, %r12d                                  #
                                # LOE rbx r13 r14 r15 r12d
..B4.7:                         # Preds ..B4.8 ..B4.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r13d, %r12d                                  #1049.517
        jc        ..B4.10       # Prob 5%                       #1049.517
                                # LOE rbx r13 r14 r15 r12d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r13d                                         #1049.470
        cmpl      $8, %r13d                                     #1049.465
        jl        ..B4.7        # Prob 82%                      #1049.465
                                # LOE rbx r13 r14 r15 r12d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      (%rsp), %r12                                  #[spill]
	.cfi_restore 12
        vmovups   128(%rsp), %ymm0                              #1049.675
        jmp       ..B4.2        # Prob 100%                     #1049.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r14 r15 ymm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r13,4), %rdi                         #1049.546
        lea       128(%rsp,%r13,4), %rsi                        #1049.546
..___tag_value___svml_sinhf8_ha_e9.90:
#       __svml_ssinh_ha_cout_rare_internal(const float *, float *)
        call      __svml_ssinh_ha_cout_rare_internal            #1049.546
..___tag_value___svml_sinhf8_ha_e9.91:
        jmp       ..B4.8        # Prob 100%                     #1049.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_sinhf8_ha_e9,@function
	.size	__svml_sinhf8_ha_e9,.-__svml_sinhf8_ha_e9
..LN__svml_sinhf8_ha_e9.3:
	.data
# -- End  __svml_sinhf8_ha_e9
	.text
.L_2__routine_start___svml_sinhf8_ha_l9_4:
# -- Begin  __svml_sinhf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinhf8_ha_l9
# --- __svml_sinhf8_ha_l9(__m256)
__svml_sinhf8_ha_l9:
# parameter 1: %ymm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1057.1
        .byte     15                                            #1123.546
        .byte     30                                            #1123.546
        .byte     250                                           #1123.546
	.cfi_startproc
..___tag_value___svml_sinhf8_ha_l9.93:
..L94:
                                                         #1057.1
        pushq     %rbp                                          #1057.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1057.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1057.1
        pushq     %r15                                          #1057.1
        subq      $184, %rsp                                    #1057.1
        lea       1484+__svml_ssinh_ha_data_internal(%rip), %r10 #1104.1007
        vmovaps   %ymm0, %ymm14                                 #1057.1
        vmovups   1280+__svml_ssinh_ha_data_internal(%rip), %ymm0 #1079.52
        vmovups   960+__svml_ssinh_ha_data_internal(%rip), %ymm4 #1084.51
        vmovups   1344+__svml_ssinh_ha_data_internal(%rip), %ymm3 #1081.23
        vmovups   5568+__svml_ssinh_ha_data_internal(%rip), %ymm10 #1085.25
        vandps    1152+__svml_ssinh_ha_data_internal(%rip), %ymm14, %ymm13 #1086.17
        vxorps    %ymm14, %ymm13, %ymm11                        #1087.16
        vfmadd213ps %ymm0, %ymm11, %ymm4                        #1088.13
        vxorps    %ymm0, %ymm4, %ymm12                          #1092.13
        vpsubd    %ymm12, %ymm3, %ymm9                          #1094.19
        vsubps    %ymm0, %ymm4, %ymm2                           #1101.13
        vpsrld    $28, %ymm9, %ymm5                             #1095.19
        vpslld    $4, %ymm5, %ymm8                              #1096.19
        vpor      %ymm12, %ymm8, %ymm1                          #1097.19
        vmovups   1024+__svml_ssinh_ha_data_internal(%rip), %ymm8 #1102.13
        vpand     %ymm3, %ymm1, %ymm9                           #1098.19
        vfnmadd213ps %ymm11, %ymm2, %ymm8                       #1102.13
        vpslld    $4, %ymm9, %ymm6                              #1104.179
        vpsubd    %ymm9, %ymm12, %ymm12                         #1099.20
        vfnmadd231ps 1088+__svml_ssinh_ha_data_internal(%rip), %ymm2, %ymm8 #1103.13
        vpcmpgtd  1408+__svml_ssinh_ha_data_internal(%rip), %ymm11, %ymm15 #1090.23
        vmovmskps %ymm15, %r11d                                 #1091.45
        vextractf128 $1, %ymm6, %xmm7                           #1104.629
        vmovd     %xmm6, %r9d                                   #1104.241
        vmovd     %xmm7, %ecx                                   #1104.610
        vmovd     -4(%r9,%r10), %xmm15                          #1104.1992
        vpextrd   $1, %xmm6, %r8d                               #1104.331
        vpextrd   $2, %xmm6, %edi                               #1104.424
        vpextrd   $3, %xmm6, %esi                               #1104.517
        vpextrd   $1, %xmm7, %edx                               #1104.705
        vpextrd   $2, %xmm7, %eax                               #1104.803
	.cfi_escape 0x10, 0x0f, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
        vpextrd   $3, %xmm7, %r15d                              #1104.901
        vmovd     -4(%r8,%r10), %xmm5                           #1104.2081
        vmovd     -4(%rdi,%r10), %xmm1                          #1104.2191
        vmovd     -4(%rsi,%r10), %xmm3                          #1104.2280
        vpunpcklqdq %xmm5, %xmm15, %xmm4                        #1104.1972
        vpunpcklqdq %xmm3, %xmm1, %xmm0                         #1104.2171
        vmovd     -4(%rcx,%r10), %xmm7                          #1104.2433
        vmovd     -4(%rdx,%r10), %xmm5                          #1104.2522
        vmovd     -4(%rax,%r10), %xmm1                          #1104.2632
        vmovd     -4(%r15,%r10), %xmm3                          #1104.2721
        vshufps   $136, %xmm0, %xmm4, %xmm2                     #1104.1952
        vpunpcklqdq %xmm5, %xmm7, %xmm4                         #1104.2413
        vpunpcklqdq %xmm3, %xmm1, %xmm0                         #1104.2612
        vshufps   $136, %xmm0, %xmm4, %xmm6                     #1104.2393
        vmovd     (%r9,%r10), %xmm7                             #1105.1992
        vmovd     (%r8,%r10), %xmm15                            #1105.2081
        vmovd     (%rdi,%r10), %xmm5                            #1105.2191
        vmovd     (%rsi,%r10), %xmm1                            #1105.2280
        vpunpcklqdq %xmm15, %xmm7, %xmm3                        #1105.1972
        vpunpcklqdq %xmm1, %xmm5, %xmm4                         #1105.2171
        vmovd     (%rdx,%r10), %xmm7                            #1105.2522
        vmovd     (%rax,%r10), %xmm15                           #1105.2632
        vmovd     (%r15,%r10), %xmm5                            #1105.2721
        vshufps   $136, %xmm4, %xmm3, %xmm0                     #1105.1952
        vpunpcklqdq %xmm5, %xmm15, %xmm3                        #1105.2612
        vmovd     -8(%rdx,%r10), %xmm15                         #1110.2522
        vinsertf128 $1, %xmm6, %ymm2, %ymm6                     #1104.1888
        vmovd     (%rcx,%r10), %xmm2                            #1105.2433
        vpunpcklqdq %xmm7, %xmm2, %xmm1                         #1105.2413
        vshufps   $136, %xmm3, %xmm1, %xmm4                     #1105.2393
        vpcmpgtd  %ymm9, %ymm10, %ymm3                          #1106.15
        vmovd     -8(%r9,%r10), %xmm10                          #1110.1992
        vmovd     -8(%r8,%r10), %xmm1                           #1110.2081
        vmovd     -8(%rsi,%r10), %xmm2                          #1110.2280
        vinsertf128 $1, %xmm4, %ymm0, %ymm5                     #1105.1888
        vmovd     -8(%rdi,%r10), %xmm0                          #1110.2191
        vfmadd213ps %ymm6, %ymm8, %ymm5                         #1109.15
        vpunpcklqdq %xmm2, %xmm0, %xmm7                         #1110.2171
        vmovd     -8(%r15,%r10), %xmm0                          #1110.2721
        vaddps    %ymm6, %ymm6, %ymm4                           #1108.13
        vpunpcklqdq %xmm1, %xmm10, %xmm6                        #1110.1972
        vmovd     -8(%rcx,%r10), %xmm10                         #1110.2433
        vmovd     -8(%rax,%r10), %xmm1                          #1110.2632
        vshufps   $136, %xmm7, %xmm6, %xmm2                     #1110.1952
        vpunpcklqdq %xmm15, %xmm10, %xmm6                       #1110.2413
        vpunpcklqdq %xmm0, %xmm1, %xmm7                         #1110.2612
        vshufps   $136, %xmm7, %xmm6, %xmm10                    #1110.2393
        vandps    %ymm3, %ymm11, %ymm1                          #1111.16
        vmovd     -12(%r9,%r10), %xmm11                         #1113.1992
        vmovd     -12(%r8,%r10), %xmm0                          #1113.2081
        vmovd     -12(%rsi,%r10), %xmm6                         #1113.2280
        vpunpcklqdq %xmm0, %xmm11, %xmm7                        #1113.1972
        vmovd     -12(%rcx,%r10), %xmm11                        #1113.2433
        vmovd     -12(%rdx,%r10), %xmm15                        #1113.2522
        vandnps   %ymm4, %ymm3, %ymm3                           #1114.13
        vorps     %ymm1, %ymm3, %ymm1                           #1116.13
        vinsertf128 $1, %xmm10, %ymm2, %ymm2                    #1110.1888
        vfmadd213ps %ymm2, %ymm8, %ymm5                         #1112.15
        vmovd     -12(%rdi,%r10), %xmm2                         #1113.2191
        vpunpcklqdq %xmm6, %xmm2, %xmm10                        #1113.2171
        vmovd     -12(%rax,%r10), %xmm2                         #1113.2632
        vmovd     -12(%r15,%r10), %xmm6                         #1113.2721
        vshufps   $136, %xmm10, %xmm7, %xmm0                    #1113.1952
        vpunpcklqdq %xmm15, %xmm11, %xmm7                       #1113.2413
        vpunpcklqdq %xmm6, %xmm2, %xmm10                        #1113.2612
        vshufps   $136, %xmm10, %xmm7, %xmm11                   #1113.2393
        vinsertf128 $1, %xmm11, %ymm0, %ymm0                    #1113.1888
        vfmadd213ps %ymm0, %ymm8, %ymm5                         #1115.15
        vpslld    $19, %ymm12, %ymm0                            #1100.20
        vaddps    %ymm5, %ymm1, %ymm4                           #1117.15
        vpaddd    %ymm0, %ymm4, %ymm1                           #1119.15
        vorps     %ymm1, %ymm13, %ymm0                          #1121.14
        testl     %r11d, %r11d                                  #1123.52
        jne       ..B5.3        # Prob 5%                       #1123.52
                                # LOE rbx r12 r13 r14 r11d ymm0 ymm14
..B5.2:                         # Preds ..B5.3 ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        addq      $184, %rsp                                    #1126.12
	.cfi_restore 15
        popq      %r15                                          #1126.12
        movq      %rbp, %rsp                                    #1126.12
        popq      %rbp                                          #1126.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1126.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
	.cfi_escape 0x10, 0x0f, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm14, 64(%rsp)                              #1123.196
        vmovups   %ymm0, 128(%rsp)                              #1123.270
        je        ..B5.2        # Prob 95%                      #1123.374
                                # LOE rbx r12 r13 r14 r11d ymm0
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %r15d, %r15d                                  #1123.454
                                # LOE rbx r12 r13 r14 r15 r11d
..B5.13:                        # Preds ..B5.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %r11d, %r12d                                  #
                                # LOE rbx r13 r14 r15 r12d
..B5.7:                         # Preds ..B5.8 ..B5.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r15d, %r12d                                  #1123.517
        jc        ..B5.10       # Prob 5%                       #1123.517
                                # LOE rbx r13 r14 r15 r12d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r15d                                         #1123.470
        cmpl      $8, %r15d                                     #1123.465
        jl        ..B5.7        # Prob 82%                      #1123.465
                                # LOE rbx r13 r14 r15 r12d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      (%rsp), %r12                                  #[spill]
	.cfi_restore 12
        vmovups   128(%rsp), %ymm0                              #1123.675
        jmp       ..B5.2        # Prob 100%                     #1123.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 ymm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r15,4), %rdi                         #1123.546
        lea       128(%rsp,%r15,4), %rsi                        #1123.546
..___tag_value___svml_sinhf8_ha_l9.109:
#       __svml_ssinh_ha_cout_rare_internal(const float *, float *)
        call      __svml_ssinh_ha_cout_rare_internal            #1123.546
..___tag_value___svml_sinhf8_ha_l9.110:
        jmp       ..B5.8        # Prob 100%                     #1123.546
        .align    16,0x90
                                # LOE rbx r13 r14 r12d r15d
	.cfi_endproc
# mark_end;
	.type	__svml_sinhf8_ha_l9,@function
	.size	__svml_sinhf8_ha_l9,.-__svml_sinhf8_ha_l9
..LN__svml_sinhf8_ha_l9.4:
	.data
# -- End  __svml_sinhf8_ha_l9
	.text
.L_2__routine_start___svml_sinhf4_ha_ex_5:
# -- Begin  __svml_sinhf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sinhf4_ha_ex
# --- __svml_sinhf4_ha_ex(__m128)
__svml_sinhf4_ha_ex:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1131.1
        .byte     15                                            #1197.540
        .byte     30                                            #1197.540
        .byte     250                                           #1197.540
	.cfi_startproc
..___tag_value___svml_sinhf4_ha_ex.112:
..L113:
                                                        #1131.1
        pushq     %rbp                                          #1131.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1131.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1131.1
        subq      $192, %rsp                                    #1131.1
        movaps    %xmm0, %xmm9                                  #1131.1
        movups    1152+__svml_ssinh_ha_data_internal(%rip), %xmm0 #1160.17
        lea       1484+__svml_ssinh_ha_data_internal(%rip), %r8 #1178.614
        andps     %xmm9, %xmm0                                  #1160.17
        movaps    %xmm0, %xmm8                                  #1161.16
        movups    960+__svml_ssinh_ha_data_internal(%rip), %xmm14 #1162.25
        pxor      %xmm9, %xmm8                                  #1161.16
        mulps     %xmm8, %xmm14                                 #1162.25
        movaps    %xmm8, %xmm5                                  #1176.13
        movups    1280+__svml_ssinh_ha_data_internal(%rip), %xmm12 #1153.49
        movaps    %xmm8, %xmm10                                 #1164.21
        movaps    %xmm12, %xmm7                                 #1166.13
        addps     %xmm12, %xmm14                                #1162.13
        pxor      %xmm14, %xmm7                                 #1166.13
        subps     %xmm12, %xmm14                                #1175.13
        movdqu    1344+__svml_ssinh_ha_data_internal(%rip), %xmm11 #1155.56
        movdqa    %xmm11, %xmm6                                 #1168.17
        psubd     %xmm7, %xmm6                                  #1168.17
        movups    1024+__svml_ssinh_ha_data_internal(%rip), %xmm13 #1176.35
        psrld     $28, %xmm6                                    #1169.17
        mulps     %xmm14, %xmm13                                #1176.35
        pslld     $4, %xmm6                                     #1170.17
        por       %xmm7, %xmm6                                  #1171.17
        subps     %xmm13, %xmm5                                 #1176.13
        pand      %xmm11, %xmm6                                 #1172.17
        movups    1088+__svml_ssinh_ha_data_internal(%rip), %xmm15 #1177.32
        movdqa    %xmm6, %xmm1                                  #1178.177
        mulps     %xmm14, %xmm15                                #1177.32
        pslld     $4, %xmm1                                     #1178.177
        movd      %xmm1, %edx                                   #1178.234
        pshufd    $1, %xmm1, %xmm2                              #1178.315
        psubd     %xmm6, %xmm7                                  #1173.18
        pshufd    $2, %xmm1, %xmm3                              #1178.419
        pslld     $19, %xmm7                                    #1174.18
        pshufd    $3, %xmm1, %xmm4                              #1178.523
        subps     %xmm15, %xmm5                                 #1177.13
        movd      %xmm2, %ecx                                   #1178.296
        movd      %xmm3, %esi                                   #1178.400
        movd      %xmm4, %edi                                   #1178.504
        movd      (%rdx,%r8), %xmm1                             #1179.1090
        movd      (%rcx,%r8), %xmm13                            #1179.1173
        movd      (%rsi,%r8), %xmm2                             #1179.1277
        movd      (%rdi,%r8), %xmm14                            #1179.1360
        punpcklqdq %xmm13, %xmm1                                #1179.1071
        punpcklqdq %xmm14, %xmm2                                #1179.1257
        shufps    $136, %xmm2, %xmm1                            #1179.1051
        pcmpgtd   1408+__svml_ssinh_ha_data_internal(%rip), %xmm10 #1164.21
        mulps     %xmm5, %xmm1                                  #1183.27
        movmskps  %xmm10, %eax                                  #1165.44
        movd      -4(%rdx,%r8), %xmm4                           #1178.1090
        movd      -4(%rcx,%r8), %xmm10                          #1178.1173
        movd      -4(%rsi,%r8), %xmm12                          #1178.1277
        movd      -4(%rdi,%r8), %xmm11                          #1178.1360
        punpcklqdq %xmm10, %xmm4                                #1178.1071
        punpcklqdq %xmm11, %xmm12                               #1178.1257
        shufps    $136, %xmm12, %xmm4                           #1178.1051
        movaps    %xmm4, %xmm2                                  #1182.13
        movd      -8(%rdi,%r8), %xmm10                          #1184.1360
        addps     %xmm4, %xmm2                                  #1182.13
        addps     %xmm1, %xmm4                                  #1183.15
        mulps     %xmm5, %xmm4                                  #1186.27
        movd      -8(%rdx,%r8), %xmm1                           #1184.1090
        movd      -8(%rcx,%r8), %xmm15                          #1184.1173
        movd      -8(%rsi,%r8), %xmm11                          #1184.1277
        punpcklqdq %xmm15, %xmm1                                #1184.1071
        punpcklqdq %xmm10, %xmm11                               #1184.1257
        shufps    $136, %xmm11, %xmm1                           #1184.1051
        movd      -12(%rsi,%r8), %xmm13                         #1187.1277
        addps     %xmm4, %xmm1                                  #1186.15
        mulps     %xmm1, %xmm5                                  #1189.27
        movd      -12(%rdx,%r8), %xmm10                         #1187.1090
        movd      -12(%rcx,%r8), %xmm4                          #1187.1173
        movd      -12(%rdi,%r8), %xmm12                         #1187.1360
        punpcklqdq %xmm4, %xmm10                                #1187.1071
        punpcklqdq %xmm12, %xmm13                               #1187.1257
        shufps    $136, %xmm13, %xmm10                          #1187.1051
        movdqu    5568+__svml_ssinh_ha_data_internal(%rip), %xmm3 #1180.13
        addps     %xmm5, %xmm10                                 #1189.15
        pcmpgtd   %xmm6, %xmm3                                  #1180.13
        andps     %xmm3, %xmm8                                  #1185.16
        andnps    %xmm2, %xmm3                                  #1188.13
        orps      %xmm8, %xmm3                                  #1190.13
        addps     %xmm10, %xmm3                                 #1191.15
        paddd     %xmm7, %xmm3                                  #1194.15
        orps      %xmm3, %xmm0                                  #1195.14
        testl     %eax, %eax                                    #1197.52
        jne       ..B6.3        # Prob 5%                       #1197.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm9
..B6.2:                         # Preds ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1200.12
        popq      %rbp                                          #1200.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1200.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm9, 64(%rsp)                               #1197.193
        movups    %xmm0, 128(%rsp)                              #1197.264
                                # LOE rbx r12 r13 r14 r15 eax
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1197.448
        movq      %r12, 8(%rsp)                                 #1197.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #1197.448
        movq      %r13, (%rsp)                                  #1197.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #1197.448
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1197.511
        jc        ..B6.10       # Prob 5%                       #1197.511
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1197.464
        cmpl      $4, %r12d                                     #1197.459
        jl        ..B6.7        # Prob 82%                      #1197.459
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1197.666
        jmp       ..B6.2        # Prob 100%                     #1197.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1197.540
        lea       128(%rsp,%r12,4), %rsi                        #1197.540
..___tag_value___svml_sinhf4_ha_ex.130:
#       __svml_ssinh_ha_cout_rare_internal(const float *, float *)
        call      __svml_ssinh_ha_cout_rare_internal            #1197.540
..___tag_value___svml_sinhf4_ha_ex.131:
        jmp       ..B6.8        # Prob 100%                     #1197.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_sinhf4_ha_ex,@function
	.size	__svml_sinhf4_ha_ex,.-__svml_sinhf4_ha_ex
..LN__svml_sinhf4_ha_ex.5:
	.data
# -- End  __svml_sinhf4_ha_ex
	.text
.L_2__routine_start___svml_ssinh_ha_cout_rare_internal_6:
# -- Begin  __svml_ssinh_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_ssinh_ha_cout_rare_internal
	.globl __svml_ssinh_ha_cout_rare_internal
# --- __svml_ssinh_ha_cout_rare_internal(const float *, float *)
__svml_ssinh_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #754.1
        .byte     15                                            #902.11
        .byte     30                                            #902.11
        .byte     250                                           #902.11
	.cfi_startproc
..___tag_value___svml_ssinh_ha_cout_rare_internal.133:
..L134:
                                                        #754.1
        movq      %rsi, %r9                                     #754.1
        movzwl    2(%rdi), %edx                                 #773.31
        xorl      %eax, %eax                                    #771.15
        andl      $32640, %edx                                  #773.31
        movss     (%rdi), %xmm2                                 #775.34
        cmpl      $32640, %edx                                  #773.57
        je        ..B7.17       # Prob 16%                      #773.57
                                # LOE rbx rbp rdi r9 r12 r13 r14 r15 eax xmm2
..B7.2:                         # Preds ..B7.1
                                # Execution count [8.40e-01]
        cvtss2sd  %xmm2, %xmm2                                  #775.34
        movsd     %xmm2, -8(%rsp)                               #775.9
        movzwl    -2(%rsp), %edx                                #776.36
        andl      $32752, %edx                                  #776.36
        movsd     %xmm2, -32(%rsp)                              #775.15
        shrl      $4, %edx                                      #776.36
        andb      $127, -25(%rsp)                               #777.30
        testl     %edx, %edx                                    #778.20
        jle       ..B7.16       # Prob 16%                      #778.20
                                # LOE rbx rbp rdi r9 r12 r13 r14 r15 eax edx xmm2
..B7.3:                         # Preds ..B7.2
                                # Execution count [7.06e-01]
        cmpl      $969, %edx                                    #780.25
        jle       ..B7.14       # Prob 50%                      #780.25
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax xmm2
..B7.4:                         # Preds ..B7.3
                                # Execution count [3.53e-01]
        movsd     -32(%rsp), %xmm0                              #782.24
        movsd     1136+_vmldSinhHATab(%rip), %xmm1              #782.49
        comisd    %xmm0, %xmm1                                  #782.49
        jbe       ..B7.13       # Prob 50%                      #782.49
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax xmm0 xmm2
..B7.5:                         # Preds ..B7.4
                                # Execution count [1.76e-01]
        movsd     1184+_vmldSinhHATab(%rip), %xmm1              #784.55
        comisd    %xmm0, %xmm1                                  #784.55
        jbe       ..B7.9        # Prob 50%                      #784.55
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax xmm0
..B7.6:                         # Preds ..B7.5
                                # Execution count [8.82e-02]
        comisd    1176+_vmldSinhHATab(%rip), %xmm0              #786.61
        jb        ..B7.8        # Prob 50%                      #786.61
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax xmm0
..B7.7:                         # Preds ..B7.6
                                # Execution count [4.41e-02]
        movsd     1112+_vmldSinhHATab(%rip), %xmm3              #788.68
        lea       _vmldSinhHATab(%rip), %rcx                    #804.59
        mulsd     %xmm0, %xmm3                                  #788.68
        movsd     1144+_vmldSinhHATab(%rip), %xmm10             #791.81
        movq      8+_vmldSinhHATab(%rip), %r10                  #772.5
        movq      %r10, %rsi                                    #803.56
        shrq      $48, %rsi                                     #803.56
        addsd     1120+_vmldSinhHATab(%rip), %xmm3              #789.76
        movsd     %xmm3, -40(%rsp)                              #789.35
        andl      $-32753, %esi                                 #803.56
        movsd     -40(%rsp), %xmm13                             #790.52
        movl      -40(%rsp), %r8d                               #798.60
        movl      %r8d, %r11d                                   #800.53
        shrl      $6, %r11d                                     #800.53
        andl      $63, %r8d                                     #799.52
        movq      %r10, -16(%rsp)                               #772.5
        subsd     1120+_vmldSinhHATab(%rip), %xmm13             #790.74
        mulsd     %xmm13, %xmm10                                #791.81
        lea       1023(%r11), %edi                              #801.52
        xorps     .L_2il0floatpacket.101(%rip), %xmm13          #792.51
        addl      $1022, %r11d                                  #803.84
        mulsd     1152+_vmldSinhHATab(%rip), %xmm13             #792.72
        subsd     %xmm10, %xmm0                                 #791.81
        movaps    %xmm0, %xmm5                                  #793.58
        movaps    %xmm0, %xmm11                                 #793.91
        andl      $2047, %r11d                                  #803.56
        lea       (%r8,%r8), %edx                               #804.77
        negl      %edi                                          #806.49
        lea       1(%r8,%r8), %r8d                              #805.83
        movsd     (%rcx,%rdx,8), %xmm8                          #804.59
        negl      %edx                                          #808.81
        shll      $4, %r11d                                     #803.56
        addl      $-4, %edi                                     #807.84
        orl       %r11d, %esi                                   #803.56
        andl      $2047, %edi                                   #807.56
        movw      %si, -10(%rsp)                                #803.56
        andl      $-32753, %esi                                 #807.56
        shll      $4, %edi                                      #807.56
        addsd     %xmm13, %xmm5                                 #793.58
        movsd     %xmm5, -24(%rsp)                              #793.35
        orl       %edi, %esi                                    #807.56
        movsd     -24(%rsp), %xmm7                              #793.91
        movsd     1128+_vmldSinhHATab(%rip), %xmm5              #794.73
        subsd     %xmm7, %xmm11                                 #793.91
        movsd     %xmm11, -56(%rsp)                             #793.68
        movsd     -24(%rsp), %xmm4                              #793.111
        movsd     -56(%rsp), %xmm12                             #793.116
        movsd     (%rcx,%r8,8), %xmm6                           #805.59
        addsd     %xmm12, %xmm4                                 #793.116
        movsd     %xmm4, -48(%rsp)                              #793.97
        movsd     -56(%rsp), %xmm9                              #793.145
        movsd     -16(%rsp), %xmm4                              #804.84
        addsd     %xmm9, %xmm13                                 #793.145
        mulsd     %xmm4, %xmm8                                  #804.84
        mulsd     %xmm4, %xmm6                                  #805.88
        movsd     %xmm13, -56(%rsp)                             #793.122
        movaps    %xmm8, %xmm9                                  #810.49
        movsd     -48(%rsp), %xmm15                             #793.174
        movw      %si, -10(%rsp)                                #807.56
        lea       128(%rdx), %esi                               #808.81
        movsd     -16(%rsp), %xmm14                             #808.89
        addl      $129, %edx                                    #809.81
        subsd     %xmm15, %xmm0                                 #793.174
        movaps    %xmm8, %xmm15                                 #810.94
        movsd     %xmm0, -48(%rsp)                              #793.151
        movsd     -56(%rsp), %xmm3                              #793.194
        movsd     -48(%rsp), %xmm0                              #793.199
        addsd     %xmm0, %xmm3                                  #793.199
        movsd     %xmm3, -48(%rsp)                              #793.180
        movsd     -24(%rsp), %xmm10                             #793.209
        mulsd     %xmm10, %xmm5                                 #794.73
        movaps    %xmm10, %xmm2                                 #795.60
        mulsd     %xmm10, %xmm2                                 #795.60
        movsd     -48(%rsp), %xmm3                              #793.219
        movaps    %xmm10, %xmm1                                 #794.169
        movsd     %xmm5, -24(%rsp)                              #794.35
        movsd     -24(%rsp), %xmm7                              #794.112
        subsd     %xmm10, %xmm7                                 #794.117
        movsd     %xmm7, -56(%rsp)                              #794.98
        movsd     -24(%rsp), %xmm12                             #794.138
        movsd     -56(%rsp), %xmm11                             #794.143
        subsd     %xmm11, %xmm12                                #794.143
        movsd     1064+_vmldSinhHATab(%rip), %xmm11             #796.95
        mulsd     %xmm2, %xmm11                                 #796.95
        movsd     %xmm12, -24(%rsp)                             #794.124
        movsd     1072+_vmldSinhHATab(%rip), %xmm12             #797.95
        mulsd     %xmm2, %xmm12                                 #797.95
        addsd     1048+_vmldSinhHATab(%rip), %xmm11             #796.118
        mulsd     %xmm2, %xmm11                                 #796.142
        addsd     1056+_vmldSinhHATab(%rip), %xmm12             #797.118
        mulsd     %xmm2, %xmm12                                 #797.142
        mulsd     %xmm10, %xmm11                                #796.149
        addsd     1040+_vmldSinhHATab(%rip), %xmm12             #797.166
        addsd     %xmm11, %xmm10                                #837.59
        mulsd     %xmm2, %xmm12                                 #797.190
        movsd     (%rcx,%rsi,8), %xmm2                          #808.59
        mulsd     %xmm14, %xmm2                                 #808.89
        movsd     -24(%rsp), %xmm0                              #794.169
        subsd     %xmm2, %xmm9                                  #810.49
        subsd     %xmm0, %xmm1                                  #794.169
        movsd     %xmm1, -56(%rsp)                              #794.149
        movsd     -24(%rsp), %xmm7                              #794.181
        movsd     -56(%rsp), %xmm5                              #794.192
        movsd     %xmm9, -24(%rsp)                              #810.35
        movsd     -24(%rsp), %xmm13                             #810.94
        movsd     (%rcx,%rdx,8), %xmm1                          #809.59
        subsd     %xmm13, %xmm15                                #810.94
        mulsd     %xmm14, %xmm1                                 #809.93
        subsd     %xmm2, %xmm15                                 #810.114
        movsd     %xmm15, -56(%rsp)                             #810.100
        movaps    %xmm8, %xmm13                                 #814.58
        movsd     -24(%rsp), %xmm14                             #810.138
        addsd     %xmm2, %xmm13                                 #814.58
        movsd     -56(%rsp), %xmm9                              #810.148
        movaps    %xmm14, %xmm0                                 #813.57
        movb      -1(%rsp), %cl                                 #838.93
        addsd     %xmm6, %xmm9                                  #812.35
        addsd     %xmm1, %xmm6                                  #815.50
        subsd     %xmm1, %xmm9                                  #811.35
        andb      $-128, %cl                                    #838.56
        addsd     %xmm9, %xmm0                                  #813.57
        movsd     %xmm0, -24(%rsp)                              #813.35
        movsd     -24(%rsp), %xmm4                              #813.90
        subsd     %xmm4, %xmm14                                 #813.90
        addsd     %xmm14, %xmm9                                 #813.117
        movsd     %xmm9, -56(%rsp)                              #813.96
        movsd     -24(%rsp), %xmm9                              #813.132
        movsd     -56(%rsp), %xmm0                              #813.142
        movsd     %xmm13, -24(%rsp)                             #814.35
        movsd     -24(%rsp), %xmm15                             #814.93
        subsd     %xmm15, %xmm8                                 #814.93
        addsd     %xmm8, %xmm2                                  #814.120
        movsd     %xmm2, -56(%rsp)                              #814.99
        movsd     -24(%rsp), %xmm2                              #814.136
        movsd     -56(%rsp), %xmm4                              #814.146
        addsd     %xmm6, %xmm4                                  #816.35
        movaps    %xmm2, %xmm6                                  #817.57
        addsd     %xmm4, %xmm6                                  #817.57
        movsd     %xmm6, -24(%rsp)                              #817.35
        movsd     -24(%rsp), %xmm8                              #817.90
        movsd     1128+_vmldSinhHATab(%rip), %xmm6              #818.75
        subsd     %xmm8, %xmm2                                  #817.90
        addsd     %xmm2, %xmm4                                  #817.117
        movsd     %xmm4, -56(%rsp)                              #817.96
        movsd     -24(%rsp), %xmm1                              #817.132
        mulsd     %xmm1, %xmm6                                  #818.75
        movsd     -56(%rsp), %xmm2                              #817.142
        movsd     %xmm6, -24(%rsp)                              #818.35
        movaps    %xmm1, %xmm6                                  #818.175
        movsd     -24(%rsp), %xmm14                             #818.114
        mulsd     %xmm2, %xmm10                                 #837.59
        subsd     %xmm1, %xmm14                                 #818.119
        movsd     %xmm14, -56(%rsp)                             #818.100
        movsd     -24(%rsp), %xmm13                             #818.142
        movsd     -56(%rsp), %xmm8                              #818.147
        subsd     %xmm8, %xmm13                                 #818.147
        movsd     %xmm13, -24(%rsp)                             #818.128
        movaps    %xmm11, %xmm13                                #825.50
        movsd     -24(%rsp), %xmm15                             #818.175
        mulsd     %xmm1, %xmm13                                 #825.50
        subsd     %xmm15, %xmm6                                 #818.175
        mulsd     %xmm3, %xmm1                                  #823.59
        mulsd     %xmm2, %xmm3                                  #819.59
        movaps    %xmm12, %xmm15                                #826.50
        movaps    %xmm13, %xmm4                                 #827.56
        mulsd     %xmm9, %xmm15                                 #826.50
        mulsd     %xmm0, %xmm12                                 #821.59
        addsd     %xmm15, %xmm4                                 #827.56
        addsd     %xmm0, %xmm12                                 #820.35
        movsd     %xmm6, -56(%rsp)                              #818.153
        addsd     %xmm1, %xmm12                                 #821.35
        movsd     -24(%rsp), %xmm8                              #818.188
        addsd     %xmm3, %xmm12                                 #822.35
        movsd     -56(%rsp), %xmm6                              #818.199
        movsd     %xmm4, -24(%rsp)                              #827.35
        movsd     -24(%rsp), %xmm14                             #827.87
        subsd     %xmm14, %xmm13                                #827.87
        addsd     %xmm13, %xmm15                                #827.114
        movsd     %xmm15, -56(%rsp)                             #827.93
        movaps    %xmm7, %xmm15                                 #832.49
        mulsd     %xmm8, %xmm15                                 #832.49
        mulsd     %xmm5, %xmm8                                  #831.58
        mulsd     %xmm6, %xmm5                                  #829.58
        mulsd     %xmm6, %xmm7                                  #830.58
        movsd     -24(%rsp), %xmm14                             #827.128
        movaps    %xmm14, %xmm13                                #833.57
        movsd     -56(%rsp), %xmm4                              #827.137
        addsd     %xmm15, %xmm13                                #833.57
        addsd     %xmm8, %xmm4                                  #829.35
        movsd     %xmm13, -24(%rsp)                             #833.35
        addsd     %xmm5, %xmm4                                  #830.35
        movsd     -24(%rsp), %xmm13                             #833.90
        addsd     %xmm7, %xmm4                                  #831.35
        subsd     %xmm13, %xmm15                                #833.90
        addsd     %xmm4, %xmm12                                 #828.35
        addsd     %xmm15, %xmm14                                #833.117
        movsd     %xmm14, -56(%rsp)                             #833.96
        movaps    %xmm9, %xmm15                                 #835.57
        movsd     -24(%rsp), %xmm13                             #833.132
        movsd     -56(%rsp), %xmm14                             #833.141
        addsd     %xmm13, %xmm15                                #835.57
        addsd     %xmm14, %xmm12                                #834.35
        movsd     %xmm15, -24(%rsp)                             #835.35
        movsd     -24(%rsp), %xmm15                             #835.90
        subsd     %xmm15, %xmm9                                 #835.90
        addsd     %xmm9, %xmm13                                 #835.117
        movsd     %xmm13, -56(%rsp)                             #835.96
        movsd     -24(%rsp), %xmm13                             #835.132
        movsd     -56(%rsp), %xmm9                              #835.141
        addsd     %xmm9, %xmm12                                 #836.35
        addsd     %xmm12, %xmm13                                #837.59
        addsd     %xmm13, %xmm10                                #837.59
        movsd     %xmm10, -32(%rsp)                             #837.35
        movb      -25(%rsp), %dil                               #838.56
        andb      $127, %dil                                    #838.56
        orb       %cl, %dil                                     #838.56
        movb      %dil, -25(%rsp)                               #838.56
        movsd     -32(%rsp), %xmm10                             #839.37
        cvtsd2ss  %xmm10, %xmm10                                #839.37
        movss     %xmm10, (%r9)                                 #839.37
        ret                                                     #839.37
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.8:                         # Preds ..B7.6
                                # Execution count [4.41e-02]
        movaps    %xmm0, %xmm2                                  #843.49
        mulsd     %xmm0, %xmm2                                  #843.49
        movsd     1104+_vmldSinhHATab(%rip), %xmm1              #844.213
        mulsd     %xmm2, %xmm1                                  #844.213
        movb      -1(%rsp), %dl                                 #846.93
        andb      $-128, %dl                                    #846.56
        addsd     1096+_vmldSinhHATab(%rip), %xmm1              #844.213
        mulsd     %xmm2, %xmm1                                  #844.213
        addsd     1088+_vmldSinhHATab(%rip), %xmm1              #844.213
        mulsd     %xmm2, %xmm1                                  #844.213
        addsd     1080+_vmldSinhHATab(%rip), %xmm1              #844.213
        mulsd     %xmm1, %xmm2                                  #844.213
        mulsd     %xmm0, %xmm2                                  #844.213
        addsd     %xmm2, %xmm0                                  #845.35
        movsd     %xmm0, -32(%rsp)                              #845.35
        movb      -25(%rsp), %cl                                #846.56
        andb      $127, %cl                                     #846.56
        orb       %dl, %cl                                      #846.56
        movb      %cl, -25(%rsp)                                #846.56
        movsd     -32(%rsp), %xmm0                              #847.37
        cvtsd2ss  %xmm0, %xmm0                                  #847.37
        movss     %xmm0, (%r9)                                  #847.37
        ret                                                     #847.37
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.9:                         # Preds ..B7.5
                                # Execution count [8.82e-02]
        movsd     1112+_vmldSinhHATab(%rip), %xmm1              #852.71
        lea       _vmldSinhHATab(%rip), %r8                     #864.52
        mulsd     %xmm0, %xmm1                                  #852.71
        movsd     1144+_vmldSinhHATab(%rip), %xmm2              #855.75
        movsd     1152+_vmldSinhHATab(%rip), %xmm3              #856.66
        movq      8+_vmldSinhHATab(%rip), %rdx                  #772.5
        movq      %rdx, -16(%rsp)                               #772.5
        addsd     1120+_vmldSinhHATab(%rip), %xmm1              #853.70
        movsd     %xmm1, -40(%rsp)                              #853.30
        movsd     -40(%rsp), %xmm4                              #854.46
        movsd     1072+_vmldSinhHATab(%rip), %xmm1              #857.64
        movl      -40(%rsp), %edx                               #860.56
        movl      %edx, %esi                                    #861.48
        andl      $63, %esi                                     #861.48
        subsd     1120+_vmldSinhHATab(%rip), %xmm4              #854.68
        mulsd     %xmm4, %xmm2                                  #855.75
        lea       (%rsi,%rsi), %ecx                             #864.70
        mulsd     %xmm3, %xmm4                                  #856.66
        subsd     %xmm2, %xmm0                                  #855.75
        movsd     (%r8,%rcx,8), %xmm5                           #864.52
        lea       1(%rsi,%rsi), %edi                            #865.76
        shrl      $6, %edx                                      #862.49
        subsd     %xmm4, %xmm0                                  #856.30
        mulsd     %xmm0, %xmm1                                  #857.87
        addl      $1022, %edx                                   #867.39
        andl      $2047, %edx                                   #867.44
        addsd     1064+_vmldSinhHATab(%rip), %xmm1              #857.108
        mulsd     %xmm0, %xmm1                                  #857.132
        addsd     1056+_vmldSinhHATab(%rip), %xmm1              #857.153
        mulsd     %xmm0, %xmm1                                  #857.177
        addsd     1048+_vmldSinhHATab(%rip), %xmm1              #857.198
        mulsd     %xmm0, %xmm1                                  #857.222
        addsd     1040+_vmldSinhHATab(%rip), %xmm1              #857.243
        mulsd     %xmm0, %xmm1                                  #858.47
        mulsd     %xmm0, %xmm1                                  #859.47
        addsd     %xmm0, %xmm1                                  #859.51
        mulsd     %xmm5, %xmm1                                  #864.30
        addsd     (%r8,%rdi,8), %xmm1                           #865.30
        addsd     %xmm5, %xmm1                                  #866.30
        cmpl      $2046, %edx                                   #868.40
        ja        ..B7.11       # Prob 50%                      #868.40
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax edx xmm1
..B7.10:                        # Preds ..B7.9
                                # Execution count [4.41e-02]
        movq      8+_vmldSinhHATab(%rip), %rcx                  #870.56
        shrq      $48, %rcx                                     #870.56
        shll      $4, %edx                                      #870.56
        andl      $-32753, %ecx                                 #870.56
        orl       %edx, %ecx                                    #870.56
        movw      %cx, -10(%rsp)                                #870.56
        movsd     -16(%rsp), %xmm0                              #871.40
        mulsd     %xmm1, %xmm0                                  #871.35
        movsd     %xmm0, -32(%rsp)                              #871.35
        jmp       ..B7.12       # Prob 100%                     #871.35
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax
..B7.11:                        # Preds ..B7.9
                                # Execution count [4.41e-02]
        decl      %edx                                          #875.84
        andl      $2047, %edx                                   #875.56
        movzwl    -10(%rsp), %ecx                               #875.56
        shll      $4, %edx                                      #875.56
        andl      $-32753, %ecx                                 #875.56
        orl       %edx, %ecx                                    #875.56
        movw      %cx, -10(%rsp)                                #875.56
        movsd     -16(%rsp), %xmm0                              #876.40
        mulsd     %xmm1, %xmm0                                  #876.35
        mulsd     1024+_vmldSinhHATab(%rip), %xmm0              #877.35
        movsd     %xmm0, -32(%rsp)                              #877.35
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax
..B7.12:                        # Preds ..B7.10 ..B7.11
                                # Execution count [8.82e-02]
        movb      -25(%rsp), %cl                                #879.51
        movb      -1(%rsp), %dl                                 #879.88
        andb      $127, %cl                                     #879.51
        andb      $-128, %dl                                    #879.51
        orb       %dl, %cl                                      #879.51
        movb      %cl, -25(%rsp)                                #879.51
        movsd     -32(%rsp), %xmm0                              #880.32
        cvtsd2ss  %xmm0, %xmm0                                  #880.32
        movss     %xmm0, (%r9)                                  #880.32
        ret                                                     #880.32
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.13:                        # Preds ..B7.4
                                # Execution count [1.76e-01]
        movsd     1168+_vmldSinhHATab(%rip), %xmm0              #885.57
        movl      $3, %eax                                      #886.25
        mulsd     %xmm2, %xmm0                                  #885.82
        cvtsd2ss  %xmm0, %xmm0                                  #885.27
        movss     %xmm0, (%r9)                                  #885.27
        ret                                                     #885.27
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.14:                        # Preds ..B7.3
                                # Execution count [3.53e-01]
        movsd     _vmldSinhHATab(%rip), %xmm0                   #891.52
        addsd     1160+_vmldSinhHATab(%rip), %xmm0              #891.90
        mulsd     %xmm2, %xmm0                                  #891.116
        cvtsd2ss  %xmm0, %xmm0                                  #891.21
        movss     %xmm0, (%r9)                                  #891.21
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.15:                        # Preds ..B7.14
                                # Execution count [1.00e+00]
        ret                                                     #904.12
                                # LOE
..B7.16:                        # Preds ..B7.2
                                # Execution count [1.34e-01]: Infreq
        movsd     1160+_vmldSinhHATab(%rip), %xmm0              #896.45
        mulsd     %xmm0, %xmm2                                  #896.45
        movsd     %xmm2, -24(%rsp)                              #896.14
        pxor      %xmm2, %xmm2                                  #897.31
        cvtss2sd  (%rdi), %xmm2                                 #897.31
        movsd     -24(%rsp), %xmm1                              #897.38
        movq      8+_vmldSinhHATab(%rip), %rdx                  #772.5
        addsd     %xmm1, %xmm2                                  #897.38
        cvtsd2ss  %xmm2, %xmm2                                  #897.16
        movq      %rdx, -16(%rsp)                               #772.5
        movss     %xmm2, (%r9)                                  #897.16
        ret                                                     #897.16
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.17:                        # Preds ..B7.1
                                # Execution count [1.60e-01]: Infreq
        addss     %xmm2, %xmm2                                  #902.33
        movss     %xmm2, (%r9)                                  #902.11
        ret                                                     #902.11
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_ssinh_ha_cout_rare_internal,@function
	.size	__svml_ssinh_ha_cout_rare_internal,.-__svml_ssinh_ha_cout_rare_internal
..LN__svml_ssinh_ha_cout_rare_internal.6:
	.data
# -- End  __svml_ssinh_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_ssinh_ha_data_internal
	.globl __svml_ssinh_ha_data_internal
__svml_ssinh_ha_data_internal:
	.long	1056964608
	.long	1057148295
	.long	1057336003
	.long	1057527823
	.long	1057723842
	.long	1057924154
	.long	1058128851
	.long	1058338032
	.long	1058551792
	.long	1058770234
	.long	1058993458
	.long	1059221571
	.long	1059454679
	.long	1059692891
	.long	1059936319
	.long	1060185078
	.long	1060439283
	.long	1060699055
	.long	1060964516
	.long	1061235789
	.long	1061513002
	.long	1061796286
	.long	1062085772
	.long	1062381598
	.long	1062683901
	.long	1062992824
	.long	1063308511
	.long	1063631111
	.long	1063960775
	.long	1064297658
	.long	1064641917
	.long	1064993715
	.long	0
	.long	2999887785
	.long	852465809
	.long	3003046475
	.long	2984291233
	.long	3001644133
	.long	854021668
	.long	2997748242
	.long	849550193
	.long	2995541347
	.long	851518274
	.long	809701978
	.long	2997656926
	.long	2996185864
	.long	2980965110
	.long	3002882728
	.long	844097402
	.long	848217591
	.long	2999013352
	.long	2992006718
	.long	831170615
	.long	3002278818
	.long	833158180
	.long	3000769962
	.long	2991891850
	.long	2999994908
	.long	2979965785
	.long	2982419430
	.long	2982221534
	.long	2999469642
	.long	833168438
	.long	2987538264
	.long	1056964608
	.long	1056605107
	.long	1056253309
	.long	1055909050
	.long	1055572167
	.long	1055242503
	.long	1054919903
	.long	1054604216
	.long	1054295293
	.long	1053992990
	.long	1053697164
	.long	1053407678
	.long	1053124394
	.long	1052847181
	.long	1052575908
	.long	1052310447
	.long	1052050675
	.long	1051796470
	.long	1051547711
	.long	1051304283
	.long	1051066071
	.long	1050832963
	.long	1050604850
	.long	1050381626
	.long	1050163184
	.long	1049949424
	.long	1049740243
	.long	1049535546
	.long	1049335234
	.long	1049139215
	.long	1048947395
	.long	1048759687
	.long	0
	.long	2979149656
	.long	824779830
	.long	2991081034
	.long	2973832926
	.long	2974030822
	.long	2971577177
	.long	2991606300
	.long	2983503242
	.long	2992381354
	.long	824769572
	.long	2993890210
	.long	822782007
	.long	2983618110
	.long	2990624744
	.long	839828983
	.long	835708794
	.long	2994494120
	.long	2972576502
	.long	2987797256
	.long	2989268318
	.long	801313370
	.long	843129666
	.long	2987152739
	.long	841161585
	.long	2989359634
	.long	845633060
	.long	2993255525
	.long	2975902625
	.long	2994657867
	.long	844077201
	.long	2991499177
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542464
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	1220542465
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	31
	.long	1118743631
	.long	1118743631
	.long	1118743631
	.long	1118743631
	.long	1118743631
	.long	1118743631
	.long	1118743631
	.long	1118743631
	.long	1118743631
	.long	1118743631
	.long	1118743631
	.long	1118743631
	.long	1118743631
	.long	1118743631
	.long	1118743631
	.long	1118743631
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
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1056964676
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1042983605
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1069066811
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	1060204544
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
	.long	939916788
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
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
	.long	1228931072
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
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	1118922496
	.long	0
	.long	687887406
	.long	2915115070
	.long	1042983726
	.long	929258934
	.long	980813922
	.long	1018266026
	.long	1042992474
	.long	954428950
	.long	997598593
	.long	1026665631
	.long	1043023968
	.long	968883188
	.long	1007325380
	.long	1032156897
	.long	1043076692
	.long	979611573
	.long	1014406071
	.long	1035098005
	.long	1043150518
	.long	987662767
	.long	1019277074
	.long	1038061036
	.long	1043245588
	.long	994080931
	.long	1024140990
	.long	1040619474
	.long	1043361758
	.long	999883086
	.long	1027459341
	.long	1042131339
	.long	1043501068
	.long	1004844750
	.long	1031304911
	.long	1043662546
	.long	1043660792
	.long	1008932267
	.long	1033741849
	.long	1045216058
	.long	1043844218
	.long	1012931568
	.long	1036203181
	.long	1046794669
	.long	1044047389
	.long	1016426573
	.long	1038940459
	.long	1048401520
	.long	1044276784
	.long	1019375189
	.long	1041073109
	.long	1049307698
	.long	1044526186
	.long	1022871607
	.long	1042725668
	.long	1050143823
	.long	1044801107
	.long	1025188112
	.long	1044524705
	.long	1050998533
	.long	1045100274
	.long	1027560014
	.long	1046473595
	.long	1051873886
	.long	1045430084
	.long	1030282880
	.long	1048576000
	.long	1052770896
	.long	1045780079
	.long	1032591216
	.long	1049705931
	.long	1053691782
	.long	1046163095
	.long	1034344377
	.long	1050916716
	.long	1054637154
	.long	1046554568
	.long	1036314518
	.long	1052210623
	.long	1055610247
	.long	1046982038
	.long	1038516252
	.long	1053590081
	.long	1056612417
	.long	1047444104
	.long	1040576009
	.long	1055057680
	.long	1057305094
	.long	1047938203
	.long	1041931271
	.long	1056616175
	.long	1057838147
	.long	1048464140
	.long	1043425610
	.long	1057616551
	.long	1058387990
	.long	1048791818
	.long	1045067287
	.long	1058491172
	.long	1058956436
	.long	1049080411
	.long	1046864840
	.long	1059415895
	.long	1059545021
	.long	1049393183
	.long	1048701551
	.long	1060392458
	.long	1060153606
	.long	1049717660
	.long	1049769606
	.long	1061422692
	.long	1060784342
	.long	1050066322
	.long	1050929319
	.long	1062508534
	.long	1061437519
	.long	1050422447
	.long	1052185595
	.long	1063652019
	.long	1062114959
	.long	1050803760
	.long	1053543521
	.long	1064855295
	.long	1062817471
	.long	1051202252
	.long	1055008374
	.long	1065736918
	.long	1063547051
	.long	1051622601
	.long	3097084200
	.long	1074266112
	.long	1064305255
	.long	1052071435
	.long	3097592230
	.long	1074615279
	.long	1065092533
	.long	1052543428
	.long	3098127090
	.long	1074981832
	.long	1065632015
	.long	1053027915
	.long	3098657586
	.long	1075366458
	.long	1066057424
	.long	1053547140
	.long	3099216842
	.long	1075769880
	.long	1066499901
	.long	1054080955
	.long	3099820420
	.long	1076192855
	.long	1066960277
	.long	1054635449
	.long	3100431607
	.long	1076636176
	.long	1067439415
	.long	1055231108
	.long	3101072121
	.long	1077100676
	.long	1067938215
	.long	1055851490
	.long	3101734019
	.long	1077587227
	.long	1068457613
	.long	1056495628
	.long	3102420416
	.long	1078096742
	.long	1068998584
	.long	1057067604
	.long	3103151062
	.long	1078630177
	.long	1069562144
	.long	1057425150
	.long	3103842417
	.long	1079188534
	.long	1070149350
	.long	1057796175
	.long	3104239345
	.long	1079772860
	.long	1070761305
	.long	1058192335
	.long	3104632042
	.long	1080384254
	.long	1071399156
	.long	1058592040
	.long	3105065708
	.long	1081023861
	.long	1072064103
	.long	1059022895
	.long	3105522352
	.long	1081692883
	.long	1072757393
	.long	1059471212
	.long	3105980727
	.long	1082261504
	.long	1073480326
	.long	1059935747
	.long	3106458228
	.long	1082627342
	.long	1073988042
	.long	1060431367
	.long	3106985545
	.long	1083009859
	.long	1074381218
	.long	1060942660
	.long	3107497595
	.long	1083409773
	.long	1074791339
	.long	1061470753
	.long	3108033911
	.long	1083827834
	.long	1075219176
	.long	1062030223
	.long	3108625747
	.long	1084264827
	.long	1075665533
	.long	1062616535
	.long	3109213903
	.long	1084721573
	.long	1076131246
	.long	1063215716
	.long	3109826597
	.long	1085198928
	.long	1076617190
	.long	1063856328
	.long	3110492915
	.long	1085697789
	.long	1077124278
	.long	1064519640
	.long	3111153932
	.long	1086219092
	.long	1077653460
	.long	1065214942
	.long	3111866338
	.long	1086763816
	.long	1078205731
	.long	1065643458
	.long	3112375523
	.long	1087332983
	.long	1078782126
	.long	1066020158
	.long	3112765050
	.long	1087927661
	.long	1079383729
	.long	1066418599
	.long	3113168833
	.long	1088548967
	.long	1080011668
	.long	1066831834
	.long	3113609244
	.long	1089198066
	.long	1080667123
	.long	1067273229
	.long	3114032535
	.long	1089876179
	.long	1081351321
	.long	1067717011
	.long	3114484025
	.long	1090551808
	.long	1082065549
	.long	1068193280
	.long	3114970280
	.long	1090921814
	.long	1082470790
	.long	1068689694
	.long	3115467036
	.long	1091308322
	.long	1082859974
	.long	1069207685
	.long	3115974474
	.long	1091712058
	.long	1083266273
	.long	1069737995
	.long	3116537826
	.long	1092133779
	.long	1083690451
	.long	1070298768
	.long	3117085761
	.long	1092574277
	.long	1084133302
	.long	1070882802
	.long	3117709126
	.long	1093034378
	.long	1084595660
	.long	1071508439
	.long	3118314866
	.long	1093514947
	.long	1085078390
	.long	1072148994
	.long	3118933130
	.long	1094016886
	.long	1085582399
	.long	1072806866
	.long	3119628767
	.long	1094541136
	.long	1086108635
	.long	1073507255
	.long	3120312034
	.long	1095088682
	.long	1086658083
	.long	1073986932
	.long	3120816642
	.long	1095660551
	.long	1087231777
	.long	1074370169
	.long	3121187932
	.long	1096257817
	.long	1087830791
	.long	1074769178
	.long	3121594488
	.long	1096881601
	.long	1088456252
	.long	1075185795
	.long	3122020198
	.long	1097533074
	.long	1089109333
	.long	1075619595
	.long	3122451537
	.long	1098213459
	.long	1089791259
	.long	1076069917
	.long	3122905402
	.long	1098915840
	.long	1090503311
	.long	1076549119
	.long	3123389748
	.long	1099286888
	.long	1090882933
	.long	1077045731
	.long	3123878864
	.long	1099674394
	.long	1091271119
	.long	1077560283
	.long	3124401536
	.long	1100079085
	.long	1091676463
	.long	1078101378
	.long	3124930682
	.long	1100501721
	.long	1092099725
	.long	1078662472
	.long	3125516800
	.long	1100943095
	.long	1092541701
	.long	1079251056
	.long	3126075229
	.long	1101404036
	.long	1093003218
	.long	1079857728
	.long	3126728388
	.long	1101885408
	.long	1093485146
	.long	1080508502
	.long	3127359219
	.long	1102388116
	.long	1093988386
	.long	1081175245
	.long	3128014352
	.long	1102913103
	.long	1094513884
	.long	1081871787
	.long	3128747686
	.long	1103461354
	.long	1095062628
	.long	1082369373
	.long	3129206088
	.long	1104033899
	.long	1095635645
	.long	1082749126
	.long	3129593301
	.long	1104631812
	.long	1096234013
	.long	1083148279
	.long	3130008743
	.long	1105256215
	.long	1096858855
	.long	1083570858
	.long	3130406199
	.long	1105908282
	.long	1097511341
	.long	1083997642
	.long	3130855937
	.long	1106589234
	.long	1098192700
	.long	1084459829
	.long	3131310395
	.long	1107298304
	.long	1098904208
	.long	1084929536
	.long	3131761492
	.long	1107669613
	.long	1099277424
	.long	1085415965
	.long	3132265084
	.long	1108057368
	.long	1099665361
	.long	1085939887
	.long	3132783371
	.long	1108462298
	.long	1100070466
	.long	1086478564
	.long	3133369511
	.long	1108885162
	.long	1100493501
	.long	1087055088
	.long	3133891436
	.long	1109326756
	.long	1100935256
	.long	1087624344
	.long	3134507369
	.long	1109787906
	.long	1101396565
	.long	1088246740
	.long	3135123225
	.long	1110269479
	.long	1101878291
	.long	1088894950
	.long	3135765391
	.long	1110772379
	.long	1102381339
	.long	1089569026
	.long	3136459557
	.long	1111297550
	.long	1102906654
	.long	1090269725
	.long	3137139863
	.long	1111845978
	.long	1103455220
	.long	1090756438
	.long	3137594905
	.long	1112418692
	.long	1104028068
	.long	1091135322
	.long	3137977906
	.long	1113016767
	.long	1104626274
	.long	1091531952
	.long	3138391473
	.long	1113641325
	.long	1105250961
	.long	1091953464
	.long	3138794156
	.long	1114293540
	.long	1105903299
	.long	1092383610
	.long	3139244396
	.long	1114974634
	.long	1106584516
	.long	1092846205
	.long	3139699003
	.long	1115685376
	.long	1107295888
	.long	1093316096
	.long	3140154077
	.long	1116056750
	.long	1107667503
	.long	1093805095
	.long	3140669482
	.long	1116444567
	.long	1108055378
	.long	1094336475
	.long	3141178479
	.long	1116849557
	.long	1108460423
	.long	1094869431
	.long	3141737901
	.long	1117272479
	.long	1108883400
	.long	1095429351
	.long	3142284745
	.long	1117714127
	.long	1109325101
	.long	1096014237
	.long	3142915054
	.long	1118175329
	.long	1109786358
	.long	1096645678
	.long	3143505197
	.long	1118656953
	.long	1110268033
	.long	1097277902
	.long	3144150196
	.long	1119159901
	.long	1110771033
	.long	1097953811
	.long	3144845928
	.long	1119685118
	.long	1111296302
	.long	1098655549
	.long	3145529363
	.long	1120233590
	.long	1111844824
	.long	1099144661
	.long	3145987662
	.long	1120806346
	.long	1112417630
	.long	1099525884
	.long	3146377804
	.long	1121404461
	.long	1113015796
	.long	1099927000
	.long	3146786805
	.long	1122029058
	.long	1113640444
	.long	1100345687
	.long	3147190794
	.long	1122681310
	.long	1114292745
	.long	1100776673
	.long	3147632967
	.long	1123362440
	.long	1114973926
	.long	1101234255
	.long	3148087611
	.long	1124073600
	.long	1115685064
	.long	1101704192
	.long	3148551873
	.long	1124444990
	.long	1116056479
	.long	1102198949
	.long	3149053844
	.long	1124832823
	.long	1116444338
	.long	1102721963
	.long	3149560519
	.long	1125237828
	.long	1116849368
	.long	1103253489
	.long	3150129648
	.long	1125660764
	.long	1117272331
	.long	1103819489
	.long	3150699108
	.long	1126102425
	.long	1117714019
	.long	1104418512
	.long	3151300238
	.long	1126563641
	.long	1118175262
	.long	1105031754
	.long	3151908533
	.long	1127045277
	.long	1118656925
	.long	1105675327
	.long	3152521467
	.long	1127548238
	.long	1119159912
	.long	1106331233
	.long	3153233976
	.long	1128073466
	.long	1119685170
	.long	1107043461
	.long	3153918194
	.long	1128621949
	.long	1120233681
	.long	1107533172
	.long	3154369114
	.long	1129194716
	.long	1120806476
	.long	1107909865
	.long	3154761041
	.long	1129792841
	.long	1121404632
	.long	1108312102
	.long	3155164804
	.long	1130417448
	.long	1122029270
	.long	1108727526
	.long	3155573216
	.long	1131069709
	.long	1122681562
	.long	1109161279
	.long	3156013372
	.long	1131750848
	.long	1123362734
	.long	1109617608
	.long	3156476219
	.long	1132462112
	.long	1124073768
	.long	1110092672
	.long	3156942778
	.long	1132833506
	.long	1124445179
	.long	1110588868
	.long	3157441390
	.long	1133221343
	.long	1124833034
	.long	1111109791
	.long	3157939291
	.long	1133626352
	.long	1125238060
	.long	1111635844
	.long	3158527234
	.long	1134049291
	.long	1125661020
	.long	1112213594
	.long	3159077768
	.long	1134490956
	.long	1126102704
	.long	1112800807
	.long	3159687990
	.long	1134952175
	.long	1126563944
	.long	1113419729
	.long	3160268049
	.long	1135433815
	.long	1127045603
	.long	1114045678
	.long	3160913934
	.long	1135936778
	.long	1127548588
	.long	1114722160
	.long	3161622444
	.long	1136462009
	.long	1128073843
	.long	1115431895
	.long	3162298664
	.long	1137010495
	.long	1128622351
	.long	1115919199
	.long	3162764127
	.long	1137583264
	.long	1129195144
	.long	1116302432
	.long	3163148306
	.long	1138181392
	.long	1129793297
	.long	1116699834
	.long	3163558953
	.long	1138806001
	.long	1130417933
	.long	1117119556
	.long	3163972568
	.long	1139458264
	.long	1131070223
	.long	1117556560
	.long	3164399930
	.long	1140139406
	.long	1131751392
	.long	1118004903
	.long	3164864827
	.long	1140850696
	.long	1132462400
	.long	1118481248
	.long	3165331960
	.long	1141222091
	.long	1132833810
	.long	1118977804
	.long	3165829733
	.long	1141609929
	.long	1133221664
	.long	1119498204
	.long	3166325440
	.long	1142014939
	.long	1133626689
	.long	1120022889
	.long	3166909893
	.long	1142437879
	.long	1134049648
	.long	1120598461
	.long	3167455696
	.long	1142879545
	.long	1134491331
	.long	1121182721
	.long	3168059997
	.long	1143340765
	.long	1134952570
	.long	1121797948
	.long	3168665771
	.long	1143822405
	.long	1135434229
	.long	1122439952
	.long	3169303507
	.long	1144325369
	.long	1135937213
	.long	1123111348
	.long	3170002824
	.long	1144850601
	.long	1136462467
	.long	1123815345
	.long	3170701624
	.long	1145399087
	.long	1137010975
	.long	1124312276
	.long	3171154336
	.long	1145971857
	.long	1137583767
	.long	1124692030
	.long	3171532482
	.long	1146569986
	.long	1138181919
	.long	1125085665
	.long	3171936657
	.long	1147194596
	.long	1138806554
	.long	1125501347
	.long	3172359765
	.long	1147846859
	.long	1139458844
	.long	1125944278
	.long	3172796218
	.long	1148528001
	.long	1140140013
	.long	1126398298
	.long	3173253435
	.long	1149239298
	.long	1140851014
	.long	1126869848
	.long	3173728905
	.long	1149610693
	.long	1141222424
	.long	1127371609
	.long	3174201888
	.long	1149998532
	.long	1141610277
	.long	1127876532
	.long	3174738014
	.long	1150403541
	.long	1142015303
	.long	1128426452
	.long	3175297014
	.long	1150826482
	.long	1142438261
	.long	1128986134
	.long	3175849827
	.long	1151268148
	.long	1142879944
	.long	1129574771
	.long	3176460842
	.long	1151729368
	.long	1143341183
	.long	1130194189
	.long	3177073044
	.long	1152211008
	.long	1143822842
	.long	1130840207
	.long	3177684163
	.long	1152713973
	.long	1144325825
	.long	1131494986
	.long	3178389375
	.long	1153239205
	.long	1144851079
	.long	1132202663
	.long	3179093821
	.long	1153787691
	.long	1145399587
	.long	1132702002
	.long	3179547441
	.long	1154360461
	.long	1145972379
	.long	1133083443
	.long	3179928175
	.long	1154958590
	.long	1146570531
	.long	1133478694
	.long	3180334828
	.long	1155583200
	.long	1147195166
	.long	1133895924
	.long	3180743924
	.long	1156235464
	.long	1147847455
	.long	1134330106
	.long	3181182650
	.long	1156916606
	.long	1148528624
	.long	1134785545
	.long	3181625656
	.long	1157627905
	.long	1149239623
	.long	1135248223
	.long	3182103210
	.long	1157999300
	.long	1149611033
	.long	1135751286
	.long	3182610963
	.long	1158387138
	.long	1149998887
	.long	1136277916
	.long	3183116226
	.long	1158792148
	.long	1150403912
	.long	1136808568
	.long	3183677057
	.long	1159215089
	.long	1150826870
	.long	1137369393
	.long	3184231622
	.long	1159656755
	.long	1151268553
	.long	1137959124
	.long	3184844315
	.long	1160117975
	.long	1151729792
	.long	1138579590
	.long	3185458125
	.long	1160599615
	.long	1152211451
	.long	1139226612
	.long	3186070783
	.long	1161102580
	.long	1152714434
	.long	1139882351
	.long	3186777469
	.long	1161627812
	.long	1153239688
	.long	1140590948
	.long	3187483326
	.long	1162176298
	.long	1153788196
	.long	1141090889
	.long	3187937173
	.long	1162749068
	.long	1154360988
	.long	1141472752
	.long	3188318554
	.long	1163347197
	.long	1154959140
	.long	1141868407
	.long	3188725827
	.long	1163971807
	.long	1155583775
	.long	1142286024
	.long	3189135516
	.long	1164624071
	.long	1156236064
	.long	1142720577
	.long	3189574810
	.long	1165305213
	.long	1156917233
	.long	1143176370
	.long	3190034748
	.long	1166016512
	.long	1157628232
	.long	1143649619
	.long	3190480049
	.long	1166387908
	.long	1157999641
	.long	1144132546
	.long	3190988301
	.long	1166775746
	.long	1158387495
	.long	1144659488
	.long	3191494042
	.long	1167180756
	.long	1158792520
	.long	1145190438
	.long	3192088104
	.long	1167603696
	.long	1159215479
	.long	1145772009
	.long	3192610334
	.long	1168045363
	.long	1159657161
	.long	1146341553
	.long	3193223446
	.long	1168506583
	.long	1160118400
	.long	1146962281
	.long	3193837658
	.long	1168988223
	.long	1160600059
	.long	1147609554
	.long	3194483474
	.long	1169491187
	.long	1161103043
	.long	1148285994
	.long	3195157755
	.long	1170016420
	.long	1161628296
	.long	1148974361
	.long	3195863964
	.long	1170564906
	.long	1162176804
	.long	1149477009
	.long	3196321965
	.long	1171137676
	.long	1162749596
	.long	1149858978
	.long	3196703508
	.long	1171735805
	.long	1163347748
	.long	1150254734
	.long	3197110936
	.long	1172360415
	.long	1163972383
	.long	1150672447
	.long	3197520774
	.long	1173012679
	.long	1164624672
	.long	1151107093
	.long	3197960210
	.long	1173693821
	.long	1165305841
	.long	1151562975
	.long	3198420283
	.long	1174405120
	.long	1166016840
	.long	1152036309
	.long	3198898489
	.long	1174776515
	.long	1166388250
	.long	1152539778
	.long	3199374091
	.long	1175164354
	.long	1166776103
	.long	1153046337
	.long	3199879952
	.long	1175569364
	.long	1167181128
	.long	1153577361
	.long	3200474128
	.long	1175992304
	.long	1167604087
	.long	1154159004
	.long	3201029241
	.long	1176433970
	.long	1168045770
	.long	1154749077
	.long	3201609685
	.long	1176895191
	.long	1168507008
	.long	1155349410
	.long	3202223997
	.long	1177376831
	.long	1168988667
	.long	1155996745
	.long	3202869909
	.long	1177879795
	.long	1169491651
	.long	1156673246
	.long	3203544282
	.long	1178405028
	.long	1170016904
	.long	1157361670
	.long	3204250580
	.long	1178953514
	.long	1170565412
	.long	1157864996
	.long	3204709619
	.long	1179526284
	.long	1171138204
	.long	1158246990
	.long	3205091203
	.long	1180124413
	.long	1171736356
	.long	1158642772
	.long	3205498670
	.long	1180749023
	.long	1172360991
	.long	1159060509
	.long	3205908544
	.long	1181401287
	.long	1173013280
	.long	1159495178
	.long	3206348016
	.long	1182082429
	.long	1173694449
	.long	1159951082
	.long	3206808123
	.long	1182793728
	.long	1174405448
	.long	1160424437
	.long	3207286361
	.long	1183165123
	.long	1174776858
	.long	1160927927
	.long	3207761995
	.long	1183552962
	.long	1175164711
	.long	1161434505
	.long	3208300659
	.long	1183957971
	.long	1175569737
	.long	1161986009
	.long	3208862090
	.long	1184380912
	.long	1175992695
	.long	1162547209
	.long	3209417231
	.long	1184822578
	.long	1176434378
	.long	1163137299
	.long	3209997701
	.long	1185283799
	.long	1176895616
	.long	1163737648
	.long	3210612038
	.long	1185765439
	.long	1177377275
	.long	1164384999
	.long	3211257974
	.long	1186268403
	.long	1177880259
	.long	1165061514
	.long	3211932370
	.long	1186793636
	.long	1178405512
	.long	1165749953
	.long	3212638690
	.long	1187342122
	.long	1178954020
	.long	1166253448
	.long	3213097989
	.long	1187914892
	.long	1179526812
	.long	1166635449
	.long	3213479583
	.long	1188513021
	.long	1180124964
	.long	1167031237
	.long	3213887059
	.long	1189137631
	.long	1180749599
	.long	1167448981
	.long	3214296943
	.long	1189789895
	.long	1181401888
	.long	1167883655
	.long	3214736423
	.long	1190471037
	.long	1182083057
	.long	1168339565
	.long	32
	.long	32
	.long	32
	.long	32
	.long	32
	.long	32
	.long	32
	.long	32
	.long	32
	.long	32
	.long	32
	.long	32
	.long	32
	.long	32
	.long	32
	.long	32
	.type	__svml_ssinh_ha_data_internal,@object
	.size	__svml_ssinh_ha_data_internal,5632
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
	.long	2684354560
	.long	1079401119
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
.L_2il0floatpacket.101:
	.long	0x00000000,0x80000000,0x00000000,0x00000000
	.type	.L_2il0floatpacket.101,@object
	.size	.L_2il0floatpacket.101,16
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
