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
	.file "svml_s_exp.c"
	.text
..TXTST0:
.L_2__routine_start___svml_expf4_ha_l9_0:
# -- Begin  __svml_expf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expf4_ha_l9
# --- __svml_expf4_ha_l9(__m128)
__svml_expf4_ha_l9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #333.1
        .byte     15                                            #416.540
        .byte     30                                            #416.540
        .byte     250                                           #416.540
	.cfi_startproc
..___tag_value___svml_expf4_ha_l9.1:
..L2:
                                                          #333.1
        pushq     %rbp                                          #333.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #333.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #333.1
        subq      $192, %rsp                                    #333.1
        vmovaps   %xmm0, %xmm1                                  #333.1
        vmovups   64+__svml_sexp_ha_data_internal(%rip), %xmm2  #361.57
        vmovups   __svml_sexp_ha_data_internal(%rip), %xmm8     #363.23
        vfmadd213ps %xmm2, %xmm1, %xmm8                         #363.23
        vmovups   256+__svml_sexp_ha_data_internal(%rip), %xmm5 #373.55
        vmovups   512+__svml_sexp_ha_data_internal(%rip), %xmm15 #379.52
        vmovups   384+__svml_sexp_ha_data_internal(%rip), %xmm6 #377.64
        vandps    128+__svml_sexp_ha_data_internal(%rip), %xmm1, %xmm3 #364.26
        vpermilps %xmm8, %xmm6, %xmm11                          #377.116
        vpslld    $21, %xmm8, %xmm9                             #385.43
        vcmpnleps 192+__svml_sexp_ha_data_internal(%rip), %xmm3, %xmm4 #369.33
        vsubps    %xmm2, %xmm8, %xmm14                          #365.23
        vmovmskps %xmm4, %eax                                   #371.56
        vmovups   448+__svml_sexp_ha_data_internal(%rip), %xmm7 #378.64
        vandps    704+__svml_sexp_ha_data_internal(%rip), %xmm9, %xmm13 #387.25
        vfnmadd213ps %xmm1, %xmm14, %xmm5                       #375.21
        vfnmadd132ps 320+__svml_sexp_ha_data_internal(%rip), %xmm5, %xmm14 #376.21
        vfmadd213ps 576+__svml_sexp_ha_data_internal(%rip), %xmm14, %xmm15 #382.25
        vmulps    %xmm14, %xmm14, %xmm10                        #383.26
        vfmadd213ps 640+__svml_sexp_ha_data_internal(%rip), %xmm14, %xmm15 #384.25
        vfmadd213ps %xmm11, %xmm10, %xmm15                      #388.25
        vpermilps %xmm8, %xmm7, %xmm12                          #378.116
        vaddps    %xmm15, %xmm14, %xmm5                         #390.25
        vmulps    %xmm13, %xmm12, %xmm2                         #389.26
        vfmadd213ps %xmm2, %xmm2, %xmm5                         #391.26
        testl     %eax, %eax                                    #392.52
        jne       ..B1.3        # Prob 5%                       #392.52
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm5
..B1.2:                         # Preds ..B1.4 ..B1.10 ..B1.1 ..B1.3
                                # Execution count [1.00e+00]
        movaps    %xmm5, %xmm0                                  #419.12
        movq      %rbp, %rsp                                    #419.12
        popq      %rbp                                          #419.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #419.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   2560+__svml_sexp_ha_data_internal(%rip), %xmm2 #403.62
        vcmpltps  2624+__svml_sexp_ha_data_internal(%rip), %xmm1, %xmm3 #406.27
        vcmpltps  %xmm1, %xmm2, %xmm0                           #404.27
        vorps     %xmm3, %xmm0, %xmm4                           #409.27
        vmovmskps %xmm4, %edx                                   #411.68
        notl      %edx                                          #412.22
        blendvps  %xmm0, .L_2il0floatpacket.37(%rip), %xmm5     #407.22
        vandnps   %xmm5, %xmm3, %xmm5                           #408.22
        andl      %eax, %edx                                    #412.38
        je        ..B1.2        # Prob 95%                      #416.52
                                # LOE rbx r12 r13 r14 r15 edx xmm1 xmm5
..B1.4:                         # Preds ..B1.3
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm1, 64(%rsp)                               #416.193
        vmovups   %xmm5, 128(%rsp)                              #416.264
        je        ..B1.2        # Prob 95%                      #416.368
                                # LOE rbx r12 r13 r14 r15 edx xmm5
..B1.7:                         # Preds ..B1.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #416.448
        movq      %r12, 8(%rsp)                                 #416.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #416.448
        movq      %r13, (%rsp)                                  #416.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #416.448
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.9 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #416.511
        jc        ..B1.11       # Prob 5%                       #416.511
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.11 ..B1.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #416.464
        cmpl      $4, %r12d                                     #416.459
        jl        ..B1.8        # Prob 82%                      #416.459
                                # LOE rbx r12 r14 r15 r13d
..B1.10:                        # Preds ..B1.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm5                              #416.665
        jmp       ..B1.2        # Prob 100%                     #416.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm5
..B1.11:                        # Preds ..B1.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #416.540
        lea       128(%rsp,%r12,4), %rsi                        #416.540
..___tag_value___svml_expf4_ha_l9.19:
#       __svml_sexp_ha_cout_rare_internal(const float *, float *)
        call      __svml_sexp_ha_cout_rare_internal             #416.540
..___tag_value___svml_expf4_ha_l9.20:
        jmp       ..B1.9        # Prob 100%                     #416.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expf4_ha_l9,@function
	.size	__svml_expf4_ha_l9,.-__svml_expf4_ha_l9
..LN__svml_expf4_ha_l9.0:
	.data
# -- End  __svml_expf4_ha_l9
	.text
.L_2__routine_start___svml_expf4_ha_e9_1:
# -- Begin  __svml_expf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expf4_ha_e9
# --- __svml_expf4_ha_e9(__m128)
__svml_expf4_ha_e9:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #692.1
        .byte     15                                            #776.540
        .byte     30                                            #776.540
        .byte     250                                           #776.540
	.cfi_startproc
..___tag_value___svml_expf4_ha_e9.22:
..L23:
                                                         #692.1
        pushq     %rbp                                          #692.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #692.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #692.1
        subq      $192, %rsp                                    #692.1
        vmovaps   %xmm0, %xmm1                                  #692.1
        vmulps    __svml_sexp_ha_data_internal(%rip), %xmm1, %xmm2 #722.23
        vroundps  $0, %xmm2, %xmm2                              #724.23
        vmulps    256+__svml_sexp_ha_data_internal(%rip), %xmm2, %xmm5 #734.41
        vaddps    64+__svml_sexp_ha_data_internal(%rip), %xmm2, %xmm13 #735.22
        vmulps    320+__svml_sexp_ha_data_internal(%rip), %xmm2, %xmm7 #736.40
        vsubps    %xmm5, %xmm1, %xmm6                           #734.21
        vmovups   384+__svml_sexp_ha_data_internal(%rip), %xmm8 #737.64
        vpslld    $21, %xmm13, %xmm14                           #745.43
        vpermilps %xmm13, %xmm8, %xmm8                          #737.116
        vsubps    %xmm7, %xmm6, %xmm0                           #736.21
        vmulps    512+__svml_sexp_ha_data_internal(%rip), %xmm0, %xmm10 #742.37
        vmulps    %xmm0, %xmm0, %xmm15                          #743.26
        vaddps    576+__svml_sexp_ha_data_internal(%rip), %xmm10, %xmm11 #742.25
        vmulps    %xmm11, %xmm0, %xmm12                         #744.37
        vmovups   448+__svml_sexp_ha_data_internal(%rip), %xmm9 #738.64
        vandps    128+__svml_sexp_ha_data_internal(%rip), %xmm1, %xmm3 #723.26
        vpermilps %xmm13, %xmm9, %xmm2                          #738.116
        vcmpnleps 192+__svml_sexp_ha_data_internal(%rip), %xmm3, %xmm4 #728.33
        vaddps    640+__svml_sexp_ha_data_internal(%rip), %xmm12, %xmm10 #744.25
        vmovmskps %xmm4, %eax                                   #730.56
        vandps    704+__svml_sexp_ha_data_internal(%rip), %xmm14, %xmm4 #747.25
        vmulps    %xmm10, %xmm15, %xmm15                        #748.37
        vaddps    %xmm15, %xmm8, %xmm11                         #748.25
        vaddps    %xmm11, %xmm0, %xmm12                         #749.25
        vmulps    %xmm12, %xmm2, %xmm3                          #750.38
        vaddps    %xmm3, %xmm2, %xmm5                           #750.26
        vmulps    %xmm5, %xmm4, %xmm5                           #751.26
        testl     %eax, %eax                                    #752.52
        jne       ..B2.3        # Prob 5%                       #752.52
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm5
..B2.2:                         # Preds ..B2.4 ..B2.10 ..B2.1 ..B2.3
                                # Execution count [1.00e+00]
        movaps    %xmm5, %xmm0                                  #779.12
        movq      %rbp, %rsp                                    #779.12
        popq      %rbp                                          #779.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #779.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   2560+__svml_sexp_ha_data_internal(%rip), %xmm2 #763.62
        vcmpltps  2624+__svml_sexp_ha_data_internal(%rip), %xmm1, %xmm3 #766.27
        vcmpltps  %xmm1, %xmm2, %xmm0                           #764.27
        vorps     %xmm3, %xmm0, %xmm4                           #769.27
        vmovmskps %xmm4, %edx                                   #771.68
        notl      %edx                                          #772.22
        blendvps  %xmm0, .L_2il0floatpacket.37(%rip), %xmm5     #767.22
        vandnps   %xmm5, %xmm3, %xmm5                           #768.22
        andl      %eax, %edx                                    #772.38
        je        ..B2.2        # Prob 95%                      #776.52
                                # LOE rbx r12 r13 r14 r15 edx xmm1 xmm5
..B2.4:                         # Preds ..B2.3
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm1, 64(%rsp)                               #776.193
        vmovups   %xmm5, 128(%rsp)                              #776.264
        je        ..B2.2        # Prob 95%                      #776.368
                                # LOE rbx r12 r13 r14 r15 edx xmm5
..B2.7:                         # Preds ..B2.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #776.448
        movq      %r12, 8(%rsp)                                 #776.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #776.448
        movq      %r13, (%rsp)                                  #776.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #776.448
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.9 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #776.511
        jc        ..B2.11       # Prob 5%                       #776.511
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.11 ..B2.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #776.464
        cmpl      $4, %r12d                                     #776.459
        jl        ..B2.8        # Prob 82%                      #776.459
                                # LOE rbx r12 r14 r15 r13d
..B2.10:                        # Preds ..B2.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm5                              #776.665
        jmp       ..B2.2        # Prob 100%                     #776.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm5
..B2.11:                        # Preds ..B2.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #776.540
        lea       128(%rsp,%r12,4), %rsi                        #776.540
..___tag_value___svml_expf4_ha_e9.40:
#       __svml_sexp_ha_cout_rare_internal(const float *, float *)
        call      __svml_sexp_ha_cout_rare_internal             #776.540
..___tag_value___svml_expf4_ha_e9.41:
        jmp       ..B2.9        # Prob 100%                     #776.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expf4_ha_e9,@function
	.size	__svml_expf4_ha_e9,.-__svml_expf4_ha_e9
..LN__svml_expf4_ha_e9.1:
	.data
# -- End  __svml_expf4_ha_e9
	.text
.L_2__routine_start___svml_expf8_ha_l9_2:
# -- Begin  __svml_expf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expf8_ha_l9
# --- __svml_expf8_ha_l9(__m256)
__svml_expf8_ha_l9:
# parameter 1: %ymm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #784.1
        .byte     15                                            #867.546
        .byte     30                                            #867.546
        .byte     250                                           #867.546
	.cfi_startproc
..___tag_value___svml_expf8_ha_l9.43:
..L44:
                                                         #784.1
        pushq     %rbp                                          #784.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #784.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #784.1
        subq      $192, %rsp                                    #784.1
        vmovups   64+__svml_sexp_ha_data_internal(%rip), %ymm1  #812.60
        vmovups   __svml_sexp_ha_data_internal(%rip), %ymm7     #814.23
        vmovups   256+__svml_sexp_ha_data_internal(%rip), %ymm4 #824.58
        vmovups   512+__svml_sexp_ha_data_internal(%rip), %ymm14 #830.55
        vmovups   384+__svml_sexp_ha_data_internal(%rip), %ymm5 #828.67
        vmovups   448+__svml_sexp_ha_data_internal(%rip), %ymm6 #829.67
        vfmadd213ps %ymm1, %ymm0, %ymm7                         #814.23
        vsubps    %ymm1, %ymm7, %ymm13                          #816.23
        vpslld    $21, %ymm7, %ymm8                             #836.46
        vfnmadd213ps %ymm0, %ymm13, %ymm4                       #826.21
        vfnmadd132ps 320+__svml_sexp_ha_data_internal(%rip), %ymm4, %ymm13 #827.21
        vfmadd213ps 576+__svml_sexp_ha_data_internal(%rip), %ymm13, %ymm14 #833.25
        vmulps    %ymm13, %ymm13, %ymm9                         #834.26
        vfmadd213ps 640+__svml_sexp_ha_data_internal(%rip), %ymm13, %ymm14 #835.25
        vpermilps %ymm7, %ymm5, %ymm10                          #828.119
        vfmadd213ps %ymm10, %ymm9, %ymm14                       #839.25
        vandps    704+__svml_sexp_ha_data_internal(%rip), %ymm8, %ymm12 #838.25
        vpermilps %ymm7, %ymm6, %ymm11                          #829.119
        vandps    128+__svml_sexp_ha_data_internal(%rip), %ymm0, %ymm2 #815.26
        vmulps    %ymm12, %ymm11, %ymm15                        #840.26
        vcmpnle_uqps 192+__svml_sexp_ha_data_internal(%rip), %ymm2, %ymm3 #820.33
        vaddps    %ymm14, %ymm13, %ymm1                         #841.25
        vmovmskps %ymm3, %eax                                   #822.57
        vfmadd213ps %ymm15, %ymm15, %ymm1                       #842.26
        testl     %eax, %eax                                    #843.52
        jne       ..B3.3        # Prob 5%                       #843.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm1
..B3.2:                         # Preds ..B3.4 ..B3.10 ..B3.1 ..B3.3
                                # Execution count [1.00e+00]
        vmovaps   %ymm1, %ymm0                                  #870.12
        movq      %rbp, %rsp                                    #870.12
        popq      %rbp                                          #870.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #870.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vcmpgt_oqps 2560+__svml_sexp_ha_data_internal(%rip), %ymm0, %ymm2 #855.27
        vcmplt_oqps 2624+__svml_sexp_ha_data_internal(%rip), %ymm0, %ymm3 #857.27
        vblendvps %ymm2, .L_2il0floatpacket.38(%rip), %ymm1, %ymm1 #858.22
        vorps     %ymm3, %ymm2, %ymm4                           #860.27
        vmovmskps %ymm4, %edx                                   #862.69
        notl      %edx                                          #863.22
        vandnps   %ymm1, %ymm3, %ymm1                           #859.22
        andl      %eax, %edx                                    #863.38
        je        ..B3.2        # Prob 95%                      #867.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm1
..B3.4:                         # Preds ..B3.3
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm0, 64(%rsp)                               #867.196
        vmovups   %ymm1, 128(%rsp)                              #867.270
        je        ..B3.2        # Prob 95%                      #867.374
                                # LOE rbx r12 r13 r14 r15 edx ymm1
..B3.7:                         # Preds ..B3.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #867.454
                                # LOE rbx r12 r13 r14 r15 eax edx
..B3.14:                        # Preds ..B3.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.9 ..B3.14
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #867.517
        jc        ..B3.11       # Prob 5%                       #867.517
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.11 ..B3.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #867.470
        cmpl      $8, %r12d                                     #867.465
        jl        ..B3.8        # Prob 82%                      #867.465
                                # LOE rbx r12 r14 r15 r13d
..B3.10:                        # Preds ..B3.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm1                              #867.674
        jmp       ..B3.2        # Prob 100%                     #867.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm1
..B3.11:                        # Preds ..B3.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #867.546
        lea       128(%rsp,%r12,4), %rsi                        #867.546
..___tag_value___svml_expf8_ha_l9.61:
#       __svml_sexp_ha_cout_rare_internal(const float *, float *)
        call      __svml_sexp_ha_cout_rare_internal             #867.546
..___tag_value___svml_expf8_ha_l9.62:
        jmp       ..B3.9        # Prob 100%                     #867.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expf8_ha_l9,@function
	.size	__svml_expf8_ha_l9,.-__svml_expf8_ha_l9
..LN__svml_expf8_ha_l9.2:
	.data
# -- End  __svml_expf8_ha_l9
	.text
.L_2__routine_start___svml_expf16_ha_z0_3:
# -- Begin  __svml_expf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expf16_ha_z0
# --- __svml_expf16_ha_z0(__m512)
__svml_expf16_ha_z0:
# parameter 1: %zmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #875.1
        .byte     15                                            #941.12
        .byte     30                                            #941.12
        .byte     250                                           #941.12
	.cfi_startproc
..___tag_value___svml_expf16_ha_z0.64:
..L65:
                                                         #875.1
        vmovups   256+__svml_sexp_ha_data_internal_avx512(%rip), %zmm4 #907.46
        vmovups   320+__svml_sexp_ha_data_internal_avx512(%rip), %zmm1 #908.50
        vmovups   384+__svml_sexp_ha_data_internal_avx512(%rip), %zmm2 #911.46
        vmovups   448+__svml_sexp_ha_data_internal_avx512(%rip), %zmm3 #912.46
        vmovups   __svml_sexp_ha_data_internal_avx512(%rip), %zmm6 #916.281
        vmovups   576+__svml_sexp_ha_data_internal_avx512(%rip), %zmm5 #920.54
        vfmadd213ps {rz-sae}, %zmm1, %zmm0, %zmm4               #909.17
        vmovups   640+__svml_sexp_ha_data_internal_avx512(%rip), %zmm8 #921.54
        vmovups   128+__svml_sexp_ha_data_internal_avx512(%rip), %zmm10 #917.280
        vsubps    {rn-sae}, %zmm1, %zmm4, %zmm13                #910.14
        vpermt2ps 64+__svml_sexp_ha_data_internal_avx512(%rip), %zmm4, %zmm6 #916.281
        vpermt2ps 192+__svml_sexp_ha_data_internal_avx512(%rip), %zmm4, %zmm10 #917.280
        vfnmadd231ps {rn-sae}, %zmm13, %zmm2, %zmm0             #913.12
        vfnmadd231ps {rn-sae}, %zmm13, %zmm3, %zmm0             #914.12
        vandps    512+__svml_sexp_ha_data_internal_avx512(%rip), %zmm0, %zmm7 #919.12
        vmulps    {rn-sae}, %zmm7, %zmm7, %zmm11                #923.15
        vfmadd231ps {rn-sae}, %zmm7, %zmm5, %zmm8               #922.14
        vaddps    {rn-sae}, %zmm7, %zmm6, %zmm9                 #924.16
        vfmadd213ps {rn-sae}, %zmm9, %zmm8, %zmm11              #925.12
        vfmadd213ps {rn-sae}, %zmm10, %zmm10, %zmm11            #927.16
        vandps    704+__svml_sexp_ha_data_internal_avx512(%rip), %zmm11, %zmm12 #929.16
        vscalefps {rn-sae}, %zmm13, %zmm12, %zmm0               #930.22
        ret                                                     #941.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_expf16_ha_z0,@function
	.size	__svml_expf16_ha_z0,.-__svml_expf16_ha_z0
..LN__svml_expf16_ha_z0.3:
	.data
# -- End  __svml_expf16_ha_z0
	.text
.L_2__routine_start___svml_expf8_ha_e9_4:
# -- Begin  __svml_expf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expf8_ha_e9
# --- __svml_expf8_ha_e9(__m256)
__svml_expf8_ha_e9:
# parameter 1: %ymm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #946.1
        .byte     15                                            #1026.46
        .byte     30                                            #1026.46
        .byte     250                                           #1026.46
	.cfi_startproc
..___tag_value___svml_expf8_ha_e9.67:
..L68:
                                                         #946.1
        pushq     %rbp                                          #946.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #946.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #946.1
        subq      $192, %rsp                                    #946.1
        vmovaps   %ymm0, %ymm6                                  #946.1
        vmulps    __svml_sexp_ha_data_internal(%rip), %ymm6, %ymm5 #976.23
        vmovups   384+__svml_sexp_ha_data_internal(%rip), %ymm1 #991.67
        vmovups   448+__svml_sexp_ha_data_internal(%rip), %ymm0 #992.67
        vroundps  $0, %ymm5, %ymm13                             #978.23
        vpxor     %xmm5, %xmm5, %xmm5                           #984.55
        vmulps    256+__svml_sexp_ha_data_internal(%rip), %ymm13, %ymm12 #988.44
        vmulps    320+__svml_sexp_ha_data_internal(%rip), %ymm13, %ymm15 #990.43
        vaddps    64+__svml_sexp_ha_data_internal(%rip), %ymm13, %ymm4 #989.22
        vsubps    %ymm12, %ymm6, %ymm14                         #988.21
        vsubps    %ymm15, %ymm14, %ymm3                         #990.21
        vandps    128+__svml_sexp_ha_data_internal(%rip), %ymm6, %ymm7 #977.26
        vcmpnle_uqps 192+__svml_sexp_ha_data_internal(%rip), %ymm7, %ymm8 #982.33
        vmulps    512+__svml_sexp_ha_data_internal(%rip), %ymm3, %ymm7 #996.40
        vpermilps %ymm4, %ymm1, %ymm2                           #991.119
        vpermilps %ymm4, %ymm0, %ymm1                           #992.119
        vmulps    %ymm3, %ymm3, %ymm0                           #997.26
        vextractf128 $1, %ymm8, %xmm9                           #983.140
        vpackssdw %xmm9, %xmm8, %xmm10                          #984.91
        vaddps    576+__svml_sexp_ha_data_internal(%rip), %ymm7, %ymm8 #996.25
        vpacksswb %xmm5, %xmm10, %xmm11                         #984.74
        vpmovmskb %xmm11, %edx                                  #984.55
        vmulps    %ymm8, %ymm3, %ymm9                           #998.40
        vaddps    640+__svml_sexp_ha_data_internal(%rip), %ymm9, %ymm14 #998.25
        vmulps    %ymm14, %ymm0, %ymm15                         #1002.40
        vaddps    %ymm15, %ymm2, %ymm2                          #1002.25
        vaddps    %ymm2, %ymm3, %ymm3                           #1003.25
        vmulps    %ymm3, %ymm1, %ymm0                           #1004.41
        vaddps    %ymm0, %ymm1, %ymm1                           #1004.26
        vpslld    $21, %xmm4, %xmm10                            #999.89
        vextractf128 $1, %ymm4, %xmm11                          #999.222
        vpslld    $21, %xmm11, %xmm12                           #999.188
        vinsertf128 $1, %xmm12, %ymm10, %ymm13                  #999.25
        vandps    704+__svml_sexp_ha_data_internal(%rip), %ymm13, %ymm4 #1001.25
        vmulps    %ymm1, %ymm4, %ymm0                           #1005.26
        testb     %dl, %dl                                      #1006.52
        jne       ..B5.12       # Prob 5%                       #1006.52
                                # LOE rbx r12 r13 r14 r15 edx xmm5 ymm0 ymm6
..B5.2:                         # Preds ..B5.12 ..B5.1
                                # Execution count [1.00e+00]
        testb     %dl, %dl                                      #1030.52
        jne       ..B5.4        # Prob 5%                       #1030.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm6
..B5.3:                         # Preds ..B5.4 ..B5.10 ..B5.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1033.12
        popq      %rbp                                          #1033.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1033.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.4:                         # Preds ..B5.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm6, 64(%rsp)                               #1030.196
        vmovups   %ymm0, 128(%rsp)                              #1030.270
        testl     %edx, %edx                                    #1030.374
        je        ..B5.3        # Prob 95%                      #1030.374
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B5.7:                         # Preds ..B5.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1030.454
                                # LOE rbx r12 r13 r14 r15 eax edx
..B5.15:                        # Preds ..B5.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.9 ..B5.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1030.517
        jc        ..B5.11       # Prob 5%                       #1030.517
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.11 ..B5.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1030.470
        cmpl      $8, %r12d                                     #1030.465
        jl        ..B5.8        # Prob 82%                      #1030.465
                                # LOE rbx r12 r14 r15 r13d
..B5.10:                        # Preds ..B5.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #1030.674
        jmp       ..B5.3        # Prob 100%                     #1030.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B5.11:                        # Preds ..B5.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1030.546
        lea       128(%rsp,%r12,4), %rsi                        #1030.546
..___tag_value___svml_expf8_ha_e9.85:
#       __svml_sexp_ha_cout_rare_internal(const float *, float *)
        call      __svml_sexp_ha_cout_rare_internal             #1030.546
..___tag_value___svml_expf8_ha_e9.86:
        jmp       ..B5.9        # Prob 100%                     #1030.546
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B5.12:                        # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vcmpgt_oqps 2560+__svml_sexp_ha_data_internal(%rip), %ymm6, %ymm1 #1018.27
        vcmplt_oqps 2624+__svml_sexp_ha_data_internal(%rip), %ymm6, %ymm2 #1020.27
        vblendvps %ymm1, .L_2il0floatpacket.38(%rip), %ymm0, %ymm0 #1021.22
        vorps     %ymm2, %ymm1, %ymm3                           #1023.27
        vandnps   %ymm0, %ymm2, %ymm0                           #1022.22
        vextractf128 $1, %ymm3, %xmm4                           #1024.134
        vpackssdw %xmm4, %xmm3, %xmm7                           #1025.103
        vpacksswb %xmm5, %xmm7, %xmm5                           #1025.86
        vpmovmskb %xmm5, %eax                                   #1025.67
        notl      %eax                                          #1026.22
        andl      %edx, %eax                                    #1026.38
        movzbl    %al, %edx                                     #1026.46
        jmp       ..B5.2        # Prob 100%                     #1026.46
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm6
	.cfi_endproc
# mark_end;
	.type	__svml_expf8_ha_e9,@function
	.size	__svml_expf8_ha_e9,.-__svml_expf8_ha_e9
..LN__svml_expf8_ha_e9.4:
	.data
# -- End  __svml_expf8_ha_e9
	.text
.L_2__routine_start___svml_expf4_ha_ex_5:
# -- Begin  __svml_expf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_expf4_ha_ex
# --- __svml_expf4_ha_ex(__m128)
__svml_expf4_ha_ex:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1038.1
        .byte     15                                            #1127.540
        .byte     30                                            #1127.540
        .byte     250                                           #1127.540
	.cfi_startproc
..___tag_value___svml_expf4_ha_ex.90:
..L91:
                                                         #1038.1
        pushq     %rbp                                          #1038.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1038.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1038.1
        subq      $192, %rsp                                    #1038.1
        movaps    %xmm0, %xmm3                                  #1038.1
        movups    1792+__svml_sexp_ha_data_internal(%rip), %xmm0 #1080.33
        lea       768+__svml_sexp_ha_data_internal(%rip), %r8   #1089.69
        mulps     %xmm3, %xmm0                                  #1080.33
        movups    1856+__svml_sexp_ha_data_internal(%rip), %xmm4 #1071.53
        movdqu    2176+__svml_sexp_ha_data_internal(%rip), %xmm8 #1087.25
        addps     %xmm4, %xmm0                                  #1080.21
        pand      %xmm0, %xmm8                                  #1087.25
        movaps    %xmm0, %xmm2                                  #1081.17
        pslld     $3, %xmm8                                     #1088.25
        subps     %xmm4, %xmm2                                  #1081.17
        movd      %xmm8, %edx                                   #1089.157
        pshufd    $1, %xmm8, %xmm6                              #1089.353
        movd      %xmm6, %ecx                                   #1089.335
        pshufd    $2, %xmm8, %xmm7                              #1089.571
        pshufd    $3, %xmm8, %xmm9                              #1089.789
        movd      %xmm7, %esi                                   #1089.553
        movd      %xmm9, %edi                                   #1089.771
        movups    1920+__svml_sexp_ha_data_internal(%rip), %xmm14 #1096.41
        movups    1984+__svml_sexp_ha_data_internal(%rip), %xmm15 #1097.40
        movslq    %edx, %rdx                                    #1089.69
        movslq    %ecx, %rcx                                    #1089.247
        movslq    %esi, %rsi                                    #1089.465
        movslq    %edi, %rdi                                    #1089.683
        mulps     %xmm2, %xmm14                                 #1096.41
        mulps     %xmm2, %xmm15                                 #1097.40
        movq      (%r8,%rdx), %xmm1                             #1089.69
        movq      (%r8,%rcx), %xmm10                            #1089.247
        movq      (%r8,%rsi), %xmm12                            #1089.465
        movq      (%r8,%rdi), %xmm11                            #1089.683
        unpcklps  %xmm10, %xmm1                                 #1089.854
        unpcklps  %xmm11, %xmm12                                #1089.889
        movaps    %xmm1, %xmm4                                  #1089.929
        movlhps   %xmm12, %xmm4                                 #1089.929
        shufps    $238, %xmm12, %xmm1                           #1089.973
        movaps    %xmm3, %xmm12                                 #1096.21
        movdqu    2432+__svml_sexp_ha_data_internal(%rip), %xmm5 #1083.20
        subps     %xmm14, %xmm12                                #1096.21
        pand      %xmm3, %xmm5                                  #1083.20
        subps     %xmm15, %xmm12                                #1097.21
        movaps    %xmm12, %xmm2                                 #1098.22
        mulps     %xmm12, %xmm2                                 #1098.22
        mulps     2368+__svml_sexp_ha_data_internal(%rip), %xmm2 #1099.33
        movdqu    2048+__svml_sexp_ha_data_internal(%rip), %xmm13 #1093.25
        addps     %xmm2, %xmm12                                 #1099.21
        mulps     %xmm4, %xmm12                                 #1100.37
        paddd     %xmm13, %xmm0                                 #1093.25
        pcmpgtd   2496+__svml_sexp_ha_data_internal(%rip), %xmm5 #1084.25
        pslld     $16, %xmm0                                    #1093.25
        movmskps  %xmm5, %eax                                   #1085.48
        addps     %xmm12, %xmm1                                 #1100.25
        pand      2112+__svml_sexp_ha_data_internal(%rip), %xmm0 #1094.25
        addps     %xmm1, %xmm4                                  #1101.22
        mulps     %xmm4, %xmm0                                  #1102.30
        testl     %eax, %eax                                    #1103.52
        jne       ..B6.3        # Prob 5%                       #1103.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm3
..B6.2:                         # Preds ..B6.4 ..B6.10 ..B6.1 ..B6.3
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1130.12
        popq      %rbp                                          #1130.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1130.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    2560+__svml_sexp_ha_data_internal(%rip), %xmm6 #1114.62
        movaps    %xmm3, %xmm5                                  #1117.27
        movl      $2139095040, %edx                             #1113.41
        cmpltps   2624+__svml_sexp_ha_data_internal(%rip), %xmm5 #1117.27
        cmpltps   %xmm3, %xmm6                                  #1115.27
        movd      %edx, %xmm1                                   #1113.41
        movaps    %xmm6, %xmm4                                  #1118.33
        pshufd    $0, %xmm1, %xmm2                              #1113.41
        andnps    %xmm0, %xmm4                                  #1118.33
        andps     %xmm6, %xmm2                                  #1118.71
        orps      %xmm5, %xmm6                                  #1120.27
        movmskps  %xmm6, %edx                                   #1122.68
        orps      %xmm2, %xmm4                                  #1118.22
        movaps    %xmm5, %xmm0                                  #1119.22
        andnps    %xmm4, %xmm0                                  #1119.22
        notl      %edx                                          #1123.22
        andl      %eax, %edx                                    #1123.38
        je        ..B6.2        # Prob 95%                      #1127.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm3
..B6.4:                         # Preds ..B6.3
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm3, 64(%rsp)                               #1127.193
        movups    %xmm0, 128(%rsp)                              #1127.264
        je        ..B6.2        # Prob 95%                      #1127.368
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B6.7:                         # Preds ..B6.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1127.448
        movq      %r12, 8(%rsp)                                 #1127.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1127.448
        movq      %r13, (%rsp)                                  #1127.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1127.448
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.9 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1127.511
        jc        ..B6.11       # Prob 5%                       #1127.511
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.11 ..B6.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1127.464
        cmpl      $4, %r12d                                     #1127.459
        jl        ..B6.8        # Prob 82%                      #1127.459
                                # LOE rbx r12 r14 r15 r13d
..B6.10:                        # Preds ..B6.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1127.665
        jmp       ..B6.2        # Prob 100%                     #1127.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B6.11:                        # Preds ..B6.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1127.540
        lea       128(%rsp,%r12,4), %rsi                        #1127.540
..___tag_value___svml_expf4_ha_ex.108:
#       __svml_sexp_ha_cout_rare_internal(const float *, float *)
        call      __svml_sexp_ha_cout_rare_internal             #1127.540
..___tag_value___svml_expf4_ha_ex.109:
        jmp       ..B6.9        # Prob 100%                     #1127.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_expf4_ha_ex,@function
	.size	__svml_expf4_ha_ex,.-__svml_expf4_ha_ex
..LN__svml_expf4_ha_ex.5:
	.data
# -- End  __svml_expf4_ha_ex
	.text
.L_2__routine_start___svml_sexp_ha_cout_rare_internal_6:
# -- Begin  __svml_sexp_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_sexp_ha_cout_rare_internal
	.globl __svml_sexp_ha_cout_rare_internal
# --- __svml_sexp_ha_cout_rare_internal(const float *, float *)
__svml_sexp_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #572.1
        .byte     15                                            #680.19
        .byte     30                                            #680.19
        .byte     250                                           #680.19
	.cfi_startproc
..___tag_value___svml_sexp_ha_cout_rare_internal.111:
..L112:
                                                        #572.1
        xorl      %eax, %eax                                    #580.14
        movzwl    2(%rdi), %edx                                 #582.35
        andl      $32640, %edx                                  #582.35
        shrl      $7, %edx                                      #582.35
        cmpl      $255, %edx                                    #582.61
        je        ..B7.17       # Prob 16%                      #582.61
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax edx
..B7.2:                         # Preds ..B7.1
                                # Execution count [8.40e-01]
        pxor      %xmm7, %xmm7                                  #584.28
        cvtss2sd  (%rdi), %xmm7                                 #584.28
        cmpl      $74, %edx                                     #585.63
        jle       ..B7.15       # Prob 50%                      #585.63
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm7
..B7.3:                         # Preds ..B7.2
                                # Execution count [4.20e-01]
        movsd     1080+_vmldExpHATab(%rip), %xmm0               #587.45
        comisd    %xmm7, %xmm0                                  #587.45
        jb        ..B7.14       # Prob 50%                      #587.45
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm7
..B7.4:                         # Preds ..B7.3
                                # Execution count [2.10e-01]
        comisd    1096+_vmldExpHATab(%rip), %xmm7               #589.49
        jb        ..B7.13       # Prob 50%                      #589.49
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm7
..B7.5:                         # Preds ..B7.4
                                # Execution count [1.05e-01]
        movsd     1024+_vmldExpHATab(%rip), %xmm1               #591.51
        movaps    %xmm7, %xmm6                                  #597.33
        mulsd     %xmm7, %xmm1                                  #591.51
        lea       _vmldExpHATab(%rip), %r10                     #606.47
        movsd     %xmm1, -16(%rsp)                              #591.25
        movsd     -16(%rsp), %xmm2                              #592.30
        movsd     1072+_vmldExpHATab(%rip), %xmm1               #598.50
        movsd     1136+_vmldExpHATab(%rip), %xmm0               #581.5
        movsd     %xmm0, -24(%rsp)                              #581.5
        addsd     1032+_vmldExpHATab(%rip), %xmm2               #592.51
        movsd     %xmm2, -8(%rsp)                               #592.25
        movsd     -8(%rsp), %xmm3                               #593.29
        movl      -8(%rsp), %r8d                                #601.50
        movl      %r8d, %ecx                                    #603.34
        andl      $63, %r8d                                     #602.33
        subsd     1032+_vmldExpHATab(%rip), %xmm3               #593.51
        movsd     %xmm3, -16(%rsp)                              #593.25
        lea       1(%r8,%r8), %r9d                              #605.71
        movsd     -16(%rsp), %xmm5                              #594.31
        lea       (%r8,%r8), %edi                               #605.69
        movsd     -16(%rsp), %xmm4                              #595.31
        mulsd     1112+_vmldExpHATab(%rip), %xmm4               #595.52
        mulsd     1104+_vmldExpHATab(%rip), %xmm5               #594.52
        subsd     %xmm4, %xmm6                                  #597.33
        shrl      $6, %ecx                                      #603.34
        subsd     %xmm5, %xmm6                                  #596.34
        comisd    1088+_vmldExpHATab(%rip), %xmm7               #607.53
        mulsd     %xmm6, %xmm1                                  #598.72
        movsd     (%r10,%rdi,8), %xmm0                          #606.47
        lea       1023(%rcx), %edx                              #604.33
        addsd     1064+_vmldExpHATab(%rip), %xmm1               #598.93
        mulsd     %xmm6, %xmm1                                  #598.116
        addsd     1056+_vmldExpHATab(%rip), %xmm1               #598.137
        mulsd     %xmm6, %xmm1                                  #598.160
        addsd     1048+_vmldExpHATab(%rip), %xmm1               #599.46
        mulsd     %xmm6, %xmm1                                  #599.69
        addsd     1040+_vmldExpHATab(%rip), %xmm1               #599.90
        mulsd     %xmm6, %xmm1                                  #599.113
        mulsd     %xmm6, %xmm1                                  #600.33
        addsd     %xmm6, %xmm1                                  #600.37
        addsd     (%r10,%r9,8), %xmm1                           #605.52
        mulsd     %xmm0, %xmm1                                  #606.25
        jb        ..B7.9        # Prob 50%                      #607.53
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax edx ecx xmm0 xmm1
..B7.6:                         # Preds ..B7.5
                                # Execution count [5.25e-02]
        andl      $2047, %edx                                   #610.37
        addsd     %xmm0, %xmm1                                  #609.54
        cmpl      $2046, %edx                                   #611.39
        ja        ..B7.8        # Prob 50%                      #611.39
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax edx xmm1
..B7.7:                         # Preds ..B7.6
                                # Execution count [2.62e-02]
        movzwl    1142+_vmldExpHATab(%rip), %ecx                #613.54
        shll      $4, %edx                                      #613.54
        andl      $-32753, %ecx                                 #613.54
        orl       %edx, %ecx                                    #613.54
        movw      %cx, -18(%rsp)                                #613.54
        movsd     -24(%rsp), %xmm0                              #614.38
        mulsd     %xmm0, %xmm1                                  #614.33
        cvtsd2ss  %xmm1, %xmm1                                  #615.35
        movss     %xmm1, (%rsi)                                 #615.35
        ret                                                     #615.35
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.8:                         # Preds ..B7.6
                                # Execution count [2.62e-02]
        decl      %edx                                          #619.82
        andl      $2047, %edx                                   #619.54
        movzwl    -18(%rsp), %ecx                               #619.54
        shll      $4, %edx                                      #619.54
        andl      $-32753, %ecx                                 #619.54
        orl       %edx, %ecx                                    #619.54
        movw      %cx, -18(%rsp)                                #619.54
        movsd     -24(%rsp), %xmm0                              #620.41
        mulsd     %xmm0, %xmm1                                  #620.41
        mulsd     1152+_vmldExpHATab(%rip), %xmm1               #621.76
        cvtsd2ss  %xmm1, %xmm1                                  #622.35
        movss     %xmm1, (%rsi)                                 #622.35
        ret                                                     #622.35
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.9:                         # Preds ..B7.5
                                # Execution count [5.25e-02]
        addl      $1083, %ecx                                   #627.37
        andl      $2047, %ecx                                   #628.37
        movl      %ecx, %eax                                    #629.50
        movzwl    -18(%rsp), %edx                               #629.50
        shll      $4, %eax                                      #629.50
        andl      $-32753, %edx                                 #629.50
        orl       %eax, %edx                                    #629.50
        movw      %dx, -18(%rsp)                                #629.50
        movsd     -24(%rsp), %xmm3                              #630.34
        mulsd     %xmm3, %xmm1                                  #630.29
        mulsd     %xmm0, %xmm3                                  #631.61
        movaps    %xmm1, %xmm2                                  #634.41
        addsd     %xmm3, %xmm2                                  #634.41
        cmpl      $50, %ecx                                     #632.39
        ja        ..B7.11       # Prob 50%                      #632.39
                                # LOE rbx rbp rsi r12 r13 r14 r15 xmm1 xmm2 xmm3
..B7.10:                        # Preds ..B7.9
                                # Execution count [2.62e-02]
        movsd     1160+_vmldExpHATab(%rip), %xmm0               #635.58
        mulsd     %xmm2, %xmm0                                  #635.58
        cvtsd2ss  %xmm0, %xmm0                                  #636.35
        movss     %xmm0, (%rsi)                                 #636.35
        jmp       ..B7.12       # Prob 100%                     #636.35
                                # LOE rbx rbp r12 r13 r14 r15
..B7.11:                        # Preds ..B7.9
                                # Execution count [2.62e-02]
        movsd     %xmm2, -72(%rsp)                              #640.34
        movsd     -72(%rsp), %xmm0                              #641.58
        subsd     %xmm0, %xmm3                                  #641.58
        movsd     %xmm3, -64(%rsp)                              #641.34
        movsd     -64(%rsp), %xmm2                              #642.34
        addsd     %xmm1, %xmm2                                  #642.34
        movsd     %xmm2, -64(%rsp)                              #642.34
        movsd     -72(%rsp), %xmm1                              #643.80
        mulsd     1168+_vmldExpHATab(%rip), %xmm1               #643.80
        movsd     %xmm1, -56(%rsp)                              #643.34
        movsd     -72(%rsp), %xmm4                              #644.41
        movsd     -56(%rsp), %xmm3                              #644.58
        addsd     %xmm3, %xmm4                                  #644.58
        movsd     %xmm4, -48(%rsp)                              #644.34
        movsd     -48(%rsp), %xmm6                              #645.45
        movsd     -56(%rsp), %xmm5                              #645.52
        subsd     %xmm5, %xmm6                                  #645.52
        movsd     %xmm6, -40(%rsp)                              #645.34
        movsd     -72(%rsp), %xmm8                              #646.45
        movsd     -40(%rsp), %xmm7                              #646.62
        subsd     %xmm7, %xmm8                                  #646.62
        movsd     %xmm8, -32(%rsp)                              #646.34
        movsd     -64(%rsp), %xmm10                             #647.46
        movsd     -32(%rsp), %xmm9                              #647.34
        addsd     %xmm9, %xmm10                                 #647.34
        movsd     %xmm10, -32(%rsp)                             #647.34
        movsd     -40(%rsp), %xmm11                             #648.45
        mulsd     1160+_vmldExpHATab(%rip), %xmm11              #648.73
        movsd     %xmm11, -40(%rsp)                             #648.34
        movsd     -32(%rsp), %xmm12                             #649.45
        mulsd     1160+_vmldExpHATab(%rip), %xmm12              #649.73
        movsd     %xmm12, -32(%rsp)                             #649.34
        movsd     -40(%rsp), %xmm14                             #650.38
        movsd     -32(%rsp), %xmm13                             #650.49
        addsd     %xmm13, %xmm14                                #650.49
        cvtsd2ss  %xmm14, %xmm14                                #651.36
        movss     %xmm14, (%rsi)                                #651.36
                                # LOE rbx rbp r12 r13 r14 r15
..B7.12:                        # Preds ..B7.10 ..B7.11
                                # Execution count [5.25e-02]
        movl      $4, %eax                                      #653.25
        ret                                                     #653.25
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.13:                        # Preds ..B7.4
                                # Execution count [1.05e-01]
        movsd     1120+_vmldExpHATab(%rip), %xmm0               #658.85
        movl      $4, %eax                                      #660.25
        mulsd     %xmm0, %xmm0                                  #658.85
        cvtsd2ss  %xmm0, %xmm0                                  #659.27
        movss     %xmm0, (%rsi)                                 #659.27
        ret                                                     #659.27
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.14:                        # Preds ..B7.3
                                # Execution count [2.10e-01]
        movsd     1128+_vmldExpHATab(%rip), %xmm0               #665.81
        movl      $3, %eax                                      #667.21
        mulsd     %xmm0, %xmm0                                  #665.81
        cvtsd2ss  %xmm0, %xmm0                                  #666.23
        movss     %xmm0, (%rsi)                                 #666.23
        ret                                                     #666.23
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.15:                        # Preds ..B7.2
                                # Execution count [4.20e-01]
        movsd     1144+_vmldExpHATab(%rip), %xmm0               #672.38
        addsd     %xmm7, %xmm0                                  #672.60
        cvtsd2ss  %xmm0, %xmm0                                  #673.19
        movss     %xmm0, (%rsi)                                 #673.19
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.16:                        # Preds ..B7.15
                                # Execution count [1.00e+00]
        ret                                                     #687.12
                                # LOE
..B7.17:                        # Preds ..B7.1
                                # Execution count [1.60e-01]: Infreq
        movb      3(%rdi), %dl                                  #678.39
        andb      $-128, %dl                                    #678.39
        cmpb      $-128, %dl                                    #678.61
        je        ..B7.19       # Prob 16%                      #678.61
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B7.18:                        # Preds ..B7.19 ..B7.17
                                # Execution count [1.47e-01]: Infreq
        movss     (%rdi), %xmm0                                 #684.26
        mulss     %xmm0, %xmm0                                  #684.33
        movss     %xmm0, (%rsi)                                 #684.19
        ret                                                     #684.19
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.19:                        # Preds ..B7.17
                                # Execution count [2.56e-02]: Infreq
        testl     $8388607, (%rdi)                              #678.88
        jne       ..B7.18       # Prob 50%                      #678.117
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B7.20:                        # Preds ..B7.19
                                # Execution count [1.28e-02]: Infreq
        movsd     1136+_vmldExpHATab(%rip), %xmm0               #680.19
        cvtsd2ss  %xmm0, %xmm0                                  #680.19
        movss     %xmm0, (%rsi)                                 #680.19
        ret                                                     #680.19
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_sexp_ha_cout_rare_internal,@function
	.size	__svml_sexp_ha_cout_rare_internal,.-__svml_sexp_ha_cout_rare_internal
..LN__svml_sexp_ha_cout_rare_internal.6:
	.data
# -- End  __svml_sexp_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_sexp_ha_data_internal_avx512
	.globl __svml_sexp_ha_data_internal_avx512
__svml_sexp_ha_data_internal_avx512:
	.long	0
	.long	3007986186
	.long	860277610
	.long	3010384254
	.long	2991457809
	.long	3008462297
	.long	860562562
	.long	3004532446
	.long	856238081
	.long	3001480295
	.long	857441778
	.long	815380209
	.long	3003456168
	.long	3001196762
	.long	2986372182
	.long	3006683458
	.long	848495278
	.long	851809756
	.long	3003311522
	.long	2995654817
	.long	833868005
	.long	3004843819
	.long	835836658
	.long	3003498340
	.long	2994528642
	.long	3002229827
	.long	2981408986
	.long	2983889551
	.long	2983366846
	.long	3000350873
	.long	833659207
	.long	2987748092
	.long	1065353216
	.long	1065536903
	.long	1065724611
	.long	1065916431
	.long	1066112450
	.long	1066312762
	.long	1066517459
	.long	1066726640
	.long	1066940400
	.long	1067158842
	.long	1067382066
	.long	1067610179
	.long	1067843287
	.long	1068081499
	.long	1068324927
	.long	1068573686
	.long	1068827891
	.long	1069087663
	.long	1069353124
	.long	1069624397
	.long	1069901610
	.long	1070184894
	.long	1070474380
	.long	1070770206
	.long	1071072509
	.long	1071381432
	.long	1071697119
	.long	1072019719
	.long	1072349383
	.long	1072686266
	.long	1073030525
	.long	1073382323
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
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	1060205080
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	2969756424
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	3221225471
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1042983923
	.long	1056964854
	.long	1056964854
	.long	1056964854
	.long	1056964854
	.long	1056964854
	.long	1056964854
	.long	1056964854
	.long	1056964854
	.long	1056964854
	.long	1056964854
	.long	1056964854
	.long	1056964854
	.long	1056964854
	.long	1056964854
	.long	1056964854
	.long	1056964854
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
	.long	796917760
	.long	796917760
	.long	796917760
	.long	796917760
	.long	796917760
	.long	796917760
	.long	796917760
	.long	796917760
	.long	796917760
	.long	796917760
	.long	796917760
	.long	796917760
	.long	796917760
	.long	796917760
	.long	796917760
	.long	796917760
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
	.type	__svml_sexp_ha_data_internal_avx512,@object
	.size	__svml_sexp_ha_data_internal_avx512,960
	.align 64
	.hidden __svml_sexp_ha_data_internal
	.globl __svml_sexp_ha_data_internal
__svml_sexp_ha_data_internal:
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1085844027
	.long	1262486012
	.long	1262486012
	.long	1262486012
	.long	1262486012
	.long	1262486012
	.long	1262486012
	.long	1262486012
	.long	1262486012
	.long	1262486012
	.long	1262486012
	.long	1262486012
	.long	1262486012
	.long	1262486012
	.long	1262486012
	.long	1262486012
	.long	1262486012
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
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	1043427328
	.long	923139572
	.long	923139572
	.long	923139572
	.long	923139572
	.long	923139572
	.long	923139572
	.long	923139572
	.long	923139572
	.long	923139572
	.long	923139572
	.long	923139572
	.long	923139572
	.long	923139572
	.long	923139572
	.long	923139572
	.long	923139572
	.long	0
	.long	856238081
	.long	848495278
	.long	2994528642
	.long	0
	.long	856238081
	.long	848495278
	.long	2994528642
	.long	0
	.long	856238081
	.long	848495278
	.long	2994528642
	.long	0
	.long	856238081
	.long	848495278
	.long	2994528642
	.long	1065353216
	.long	1066940400
	.long	1068827891
	.long	1071072509
	.long	1065353216
	.long	1066940400
	.long	1068827891
	.long	1071072509
	.long	1065353216
	.long	1066940400
	.long	1068827891
	.long	1071072509
	.long	1065353216
	.long	1066940400
	.long	1068827891
	.long	1071072509
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1026210578
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1042987794
	.long	1056964607
	.long	1056964607
	.long	1056964607
	.long	1056964607
	.long	1056964607
	.long	1056964607
	.long	1056964607
	.long	1056964607
	.long	1056964607
	.long	1056964607
	.long	1056964607
	.long	1056964607
	.long	1056964607
	.long	1056964607
	.long	1056964607
	.long	1056964607
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
	.long	1065353216
	.long	0
	.long	1065398765
	.long	857715507
	.long	1065444562
	.long	2982428624
	.long	1065490607
	.long	856090616
	.long	1065536903
	.long	3008276393
	.long	1065583449
	.long	857595758
	.long	1065630249
	.long	3002158215
	.long	1065677302
	.long	858550072
	.long	1065724611
	.long	860854417
	.long	1065772177
	.long	859251839
	.long	1065820002
	.long	3008545510
	.long	1065868085
	.long	864021810
	.long	1065916431
	.long	3011435083
	.long	1065965038
	.long	819487640
	.long	1066013909
	.long	862622430
	.long	1066063046
	.long	856316133
	.long	1066112450
	.long	2992679841
	.long	1066162122
	.long	3001970243
	.long	1066212063
	.long	859241655
	.long	1066262276
	.long	847285146
	.long	1066312762
	.long	3010032741
	.long	1066363521
	.long	843003463
	.long	1066414556
	.long	856041677
	.long	1066465869
	.long	3009946167
	.long	1066517459
	.long	862410276
	.long	1066569330
	.long	863709796
	.long	1066621483
	.long	851201948
	.long	1066673919
	.long	838513088
	.long	1066726640
	.long	3006136850
	.long	1066779647
	.long	3010613863
	.long	1066832941
	.long	853311585
	.long	1066886525
	.long	856941301
	.long	1066940400
	.long	857938801
	.long	1066994568
	.long	2993474036
	.long	1067049030
	.long	3009003152
	.long	1067103787
	.long	2967126046
	.long	1067158842
	.long	3003929955
	.long	1067214195
	.long	860904838
	.long	1067269850
	.long	3006375425
	.long	1067325806
	.long	843377209
	.long	1067382066
	.long	859906882
	.long	1067438632
	.long	848662531
	.long	1067495505
	.long	838571797
	.long	1067552687
	.long	3000363757
	.long	1067610179
	.long	818090586
	.long	1067667983
	.long	860999685
	.long	1067726102
	.long	3005475891
	.long	1067784536
	.long	3010626242
	.long	1067843287
	.long	3006045534
	.long	1067902357
	.long	2998944862
	.long	1067961748
	.long	3004575030
	.long	1068021461
	.long	842258832
	.long	1068081499
	.long	3004574472
	.long	1068141862
	.long	858118433
	.long	1068202554
	.long	3004476802
	.long	1068263575
	.long	3006694272
	.long	1068324927
	.long	2989353718
	.long	1068386612
	.long	858100843
	.long	1068448633
	.long	3005558251
	.long	1068510990
	.long	3000050815
	.long	1068573686
	.long	3011271336
	.long	1068636722
	.long	3006477262
	.long	1068700100
	.long	840255625
	.long	1068763823
	.long	3007001961
	.long	1068827891
	.long	852486010
	.long	1068892308
	.long	3006218836
	.long	1068957074
	.long	2993076596
	.long	1069022192
	.long	3000356208
	.long	1069087663
	.long	856606199
	.long	1069153490
	.long	856315927
	.long	1069219675
	.long	3004946819
	.long	1069286218
	.long	863888852
	.long	1069353124
	.long	3007401960
	.long	1069420392
	.long	832069785
	.long	1069488026
	.long	3004369690
	.long	1069556027
	.long	3007061599
	.long	1069624397
	.long	3000395326
	.long	1069693138
	.long	851736822
	.long	1069762253
	.long	2996268450
	.long	1069831743
	.long	2999890733
	.long	1069901610
	.long	839559223
	.long	1069971857
	.long	3004476343
	.long	1070042485
	.long	3000848665
	.long	1070113496
	.long	859381756
	.long	1070184894
	.long	3010667426
	.long	1070256678
	.long	859604257
	.long	1070328853
	.long	2953234114
	.long	1070401420
	.long	3010682756
	.long	1070474380
	.long	841546788
	.long	1070547737
	.long	2999163804
	.long	1070621492
	.long	2996061011
	.long	1070695647
	.long	860136147
	.long	1070770206
	.long	3009158570
	.long	1070845169
	.long	3008165066
	.long	1070920539
	.long	3005767716
	.long	1070996318
	.long	2955948569
	.long	1071072509
	.long	3000280458
	.long	1071149113
	.long	857267046
	.long	1071226134
	.long	3002473793
	.long	1071303572
	.long	861820308
	.long	1071381432
	.long	3008383516
	.long	1071459714
	.long	3010850715
	.long	1071538421
	.long	3011199971
	.long	1071617555
	.long	2995067967
	.long	1071697119
	.long	2988354393
	.long	1071777115
	.long	2889750092
	.long	1071857545
	.long	862289173
	.long	1071938413
	.long	3007287685
	.long	1072019719
	.long	2990808038
	.long	1072101467
	.long	3005883328
	.long	1072183658
	.long	857766040
	.long	1072266296
	.long	855693471
	.long	1072349383
	.long	2990610142
	.long	1072432921
	.long	3004393415
	.long	1072516912
	.long	846646433
	.long	1072601360
	.long	3008357562
	.long	1072686266
	.long	3007858250
	.long	1072771633
	.long	3006309869
	.long	1072857463
	.long	854681599
	.long	1072943760
	.long	2995197552
	.long	1073030525
	.long	841557046
	.long	1073117761
	.long	861464191
	.long	1073205471
	.long	862328226
	.long	1073293658
	.long	3003728983
	.long	1073382323
	.long	2995926872
	.long	1073471469
	.long	861522913
	.long	1073561100
	.long	835668076
	.long	1073651217
	.long	859371295
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1127787067
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1262485504
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	1001488384
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	3051257987
	.long	16256
	.long	16256
	.long	16256
	.long	16256
	.long	16256
	.long	16256
	.long	16256
	.long	16256
	.long	16256
	.long	16256
	.long	16256
	.long	16256
	.long	16256
	.long	16256
	.long	16256
	.long	16256
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
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	127
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	4294967232
	.long	864026624
	.long	864026624
	.long	864026624
	.long	864026624
	.long	864026624
	.long	864026624
	.long	864026624
	.long	864026624
	.long	864026624
	.long	864026624
	.long	864026624
	.long	864026624
	.long	864026624
	.long	864026624
	.long	864026624
	.long	864026624
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
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	1118925335
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.long	3268407732
	.type	__svml_sexp_ha_data_internal,@object
	.size	__svml_sexp_ha_data_internal,2688
	.align 32
.L_2il0floatpacket.38:
	.long	0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000
	.type	.L_2il0floatpacket.38,@object
	.size	.L_2il0floatpacket.38,32
	.align 32
_vmldExpHATab:
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	1048019041
	.long	1072704666
	.long	2631457885
	.long	3161546771
	.long	3541402996
	.long	1072716208
	.long	896005651
	.long	1015861842
	.long	410360776
	.long	1072727877
	.long	1642514529
	.long	1012987726
	.long	1828292879
	.long	1072739672
	.long	1568897901
	.long	1016568486
	.long	852742562
	.long	1072751596
	.long	1882168529
	.long	1010744893
	.long	3490863953
	.long	1072763649
	.long	707771662
	.long	3163903570
	.long	2930322912
	.long	1072775834
	.long	3117806614
	.long	3163670819
	.long	1014845819
	.long	1072788152
	.long	3936719688
	.long	3162512149
	.long	3949972341
	.long	1072800603
	.long	1058231231
	.long	1015777676
	.long	828946858
	.long	1072813191
	.long	1044000608
	.long	1016786167
	.long	2288159958
	.long	1072825915
	.long	1151779725
	.long	1015705409
	.long	1853186616
	.long	1072838778
	.long	3819481236
	.long	1016499965
	.long	1709341917
	.long	1072851781
	.long	2552227826
	.long	1015039787
	.long	4112506593
	.long	1072864925
	.long	1829350193
	.long	1015216097
	.long	2799960843
	.long	1072878213
	.long	1913391796
	.long	1015756674
	.long	171030293
	.long	1072891646
	.long	1303423926
	.long	1015238005
	.long	2992903935
	.long	1072905224
	.long	1574172746
	.long	1016061241
	.long	926591435
	.long	1072918951
	.long	3427487848
	.long	3163704045
	.long	887463927
	.long	1072932827
	.long	1049900754
	.long	3161575912
	.long	1276261410
	.long	1072946854
	.long	2804567149
	.long	1015390024
	.long	569847338
	.long	1072961034
	.long	1209502043
	.long	3159926671
	.long	1617004845
	.long	1072975368
	.long	1623370769
	.long	1011049453
	.long	3049340112
	.long	1072989858
	.long	3667985273
	.long	1013894369
	.long	3577096743
	.long	1073004506
	.long	3145379760
	.long	1014403278
	.long	1990012071
	.long	1073019314
	.long	7447438
	.long	3163526196
	.long	1453150082
	.long	1073034283
	.long	3171891295
	.long	3162037958
	.long	917841882
	.long	1073049415
	.long	419288974
	.long	1016280325
	.long	3712504873
	.long	1073064711
	.long	3793507337
	.long	1016095713
	.long	363667784
	.long	1073080175
	.long	728023093
	.long	1016345318
	.long	2956612997
	.long	1073095806
	.long	1005538728
	.long	3163304901
	.long	2186617381
	.long	1073111608
	.long	2018924632
	.long	3163803357
	.long	1719614413
	.long	1073127582
	.long	3210617384
	.long	3163796463
	.long	1013258799
	.long	1073143730
	.long	3094194670
	.long	3160631279
	.long	3907805044
	.long	1073160053
	.long	2119843535
	.long	3161988964
	.long	1447192521
	.long	1073176555
	.long	508946058
	.long	3162904882
	.long	1944781191
	.long	1073193236
	.long	3108873501
	.long	3162190556
	.long	919555682
	.long	1073210099
	.long	2882956373
	.long	1013312481
	.long	2571947539
	.long	1073227145
	.long	4047189812
	.long	3163777462
	.long	2604962541
	.long	1073244377
	.long	3631372142
	.long	3163870288
	.long	1110089947
	.long	1073261797
	.long	3253791412
	.long	1015920431
	.long	2568320822
	.long	1073279406
	.long	1509121860
	.long	1014756995
	.long	2966275557
	.long	1073297207
	.long	2339118633
	.long	3160254904
	.long	2682146384
	.long	1073315202
	.long	586480042
	.long	3163702083
	.long	2191782032
	.long	1073333393
	.long	730975783
	.long	1014083580
	.long	2069751141
	.long	1073351782
	.long	576856675
	.long	3163014404
	.long	2990417245
	.long	1073370371
	.long	3552361237
	.long	3163667409
	.long	1434058175
	.long	1073389163
	.long	1853053619
	.long	1015310724
	.long	2572866477
	.long	1073408159
	.long	2462790535
	.long	1015814775
	.long	3092190715
	.long	1073427362
	.long	1457303226
	.long	3159737305
	.long	4076559943
	.long	1073446774
	.long	950899508
	.long	3160987380
	.long	2420883922
	.long	1073466398
	.long	174054861
	.long	1014300631
	.long	3716502172
	.long	1073486235
	.long	816778419
	.long	1014197934
	.long	777507147
	.long	1073506289
	.long	3507050924
	.long	1015341199
	.long	3706687593
	.long	1073526560
	.long	1821514088
	.long	1013410604
	.long	1242007932
	.long	1073547053
	.long	1073740399
	.long	3163532637
	.long	3707479175
	.long	1073567768
	.long	2789017511
	.long	1014276997
	.long	64696965
	.long	1073588710
	.long	3586233004
	.long	1015962192
	.long	863738719
	.long	1073609879
	.long	129252895
	.long	3162690849
	.long	3884662774
	.long	1073631278
	.long	1614448851
	.long	1014281732
	.long	2728693978
	.long	1073652911
	.long	2413007344
	.long	3163551506
	.long	3999357479
	.long	1073674779
	.long	1101668360
	.long	1015989180
	.long	1533953344
	.long	1073696886
	.long	835814894
	.long	1015702697
	.long	2174652632
	.long	1073719233
	.long	1301400989
	.long	1014466875
	.long	1697350398
	.long	1079448903
	.long	0
	.long	1127743488
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
	.long	3758096384
	.long	1079389762
	.long	3758096384
	.long	3226850697
	.long	2147483648
	.long	3227123254
	.long	4277796864
	.long	1065758274
	.long	3164486458
	.long	1025308570
	.long	1
	.long	1048576
	.long	4294967295
	.long	2146435071
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	0
	.long	1073741824
	.long	0
	.long	1009778688
	.long	0
	.long	1106771968
	.type	_vmldExpHATab,@object
	.size	_vmldExpHATab,1176
	.space 8, 0x00 	# pad
	.align 16
.L_2il0floatpacket.37:
	.long	0x7f800000,0x7f800000,0x7f800000,0x7f800000
	.type	.L_2il0floatpacket.37,@object
	.size	.L_2il0floatpacket.37,16
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
