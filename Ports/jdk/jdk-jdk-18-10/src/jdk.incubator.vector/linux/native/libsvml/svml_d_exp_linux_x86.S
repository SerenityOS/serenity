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
	.file "svml_d_exp.c"
	.text
..TXTST0:
.L_2__routine_start___svml_exp1_ha_e9_0:
# -- Begin  __svml_exp1_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_exp1_ha_e9
# --- __svml_exp1_ha_e9(__m128d)
__svml_exp1_ha_e9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #266.1
        .byte     15                                            #349.546
        .byte     30                                            #349.546
        .byte     250                                           #349.546
	.cfi_startproc
..___tag_value___svml_exp1_ha_e9.1:
..L2:
                                                          #266.1
        pushq     %rbp                                          #266.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #266.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #266.1
        subq      $128, %rsp                                    #266.1
        movaps    %xmm0, %xmm1                                  #266.1
        movaps    %xmm1, %xmm13                                 #296.25
        movaps    %xmm1, %xmm11                                 #313.13
        mulsd     1024+__svml_dexp_ha_data_internal(%rip), %xmm13 #296.25
        movl      $2147483647, %eax                             #302.26
        movsd     1088+__svml_dexp_ha_data_internal(%rip), %xmm2 #289.20
        movl      $1082532650, %edx                             #303.31
        pshufd    $85, %xmm1, %xmm5                             #301.26
        movd      %eax, %xmm3                                   #302.26
        pand      %xmm3, %xmm5                                  #302.26
        lea       __svml_dexp_ha_data_internal(%rip), %rsi      #311.53
        movq      1600+__svml_dexp_ha_data_internal(%rip), %xmm14 #305.31
        addsd     %xmm2, %xmm13                                 #296.13
        movaps    %xmm13, %xmm9                                 #297.13
        movd      %edx, %xmm4                                   #303.31
        pcmpgtd   %xmm4, %xmm5                                  #303.31
        movdqa    %xmm14, %xmm6                                 #307.27
        movmskps  %xmm5, %eax                                   #304.48
        pand      %xmm13, %xmm6                                 #307.27
        movsd     1536+__svml_dexp_ha_data_internal(%rip), %xmm5 #315.25
        pandn     %xmm13, %xmm14                                #309.23
        pshufd    $0, %xmm6, %xmm7                              #308.27
        psllq     $46, %xmm14                                   #312.23
        pslld     $4, %xmm7                                     #310.27
        subsd     %xmm2, %xmm9                                  #297.13
        movd      %xmm7, %ecx                                   #311.89
        movaps    %xmm9, %xmm8                                  #313.33
        mulsd     1216+__svml_dexp_ha_data_internal(%rip), %xmm8 #313.33
        mulsd     1280+__svml_dexp_ha_data_internal(%rip), %xmm9 #314.32
        subsd     %xmm8, %xmm11                                 #313.13
        movslq    %ecx, %rcx                                    #311.53
        subsd     %xmm9, %xmm11                                 #314.13
        mulsd     %xmm11, %xmm5                                 #315.25
        movaps    %xmm11, %xmm10                                #317.14
        mulsd     %xmm11, %xmm10                                #317.14
        addsd     1472+__svml_dexp_ha_data_internal(%rip), %xmm5 #315.13
        mulsd     %xmm11, %xmm5                                 #316.25
        movups    (%rsi,%rcx), %xmm12                           #311.53
        addsd     1408+__svml_dexp_ha_data_internal(%rip), %xmm5 #316.13
        mulsd     %xmm11, %xmm5                                 #318.25
        addsd     1344+__svml_dexp_ha_data_internal(%rip), %xmm5 #318.13
        mulsd     %xmm10, %xmm5                                 #319.25
        addsd     %xmm11, %xmm5                                 #319.13
        mulsd     %xmm12, %xmm5                                 #320.25
        addsd     8(%rsi,%rcx), %xmm5                           #320.13
        addsd     %xmm12, %xmm5                                 #321.13
        paddq     %xmm14, %xmm5                                 #323.19
        testb     $1, %al                                       #325.37
        jne       ..B1.3        # Prob 5%                       #325.52
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm5
..B1.2:                         # Preds ..B1.1 ..B1.5 ..B1.3
                                # Execution count [1.00e+00]
        movaps    %xmm5, %xmm0                                  #352.12
        movq      %rbp, %rsp                                    #352.12
        popq      %rbp                                          #352.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #352.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movsd     1984+__svml_dexp_ha_data_internal(%rip), %xmm2 #336.33
        movaps    %xmm1, %xmm4                                  #339.28
        movaps    %xmm1, %xmm0                                  #337.28
        cmpltsd   %xmm1, %xmm2                                  #337.28
        cmpltsd   2048+__svml_dexp_ha_data_internal(%rip), %xmm4 #339.28
        movsd     %xmm2, %xmm0                                  #337.28
        blendvpd  %xmm0, .L_2il0floatpacket.33(%rip), %xmm5     #340.22
        orps      %xmm4, %xmm0                                  #342.28
        movmskpd  %xmm0, %edx                                   #344.68
        movaps    %xmm5, %xmm3                                  #341.22
        movaps    %xmm4, %xmm5                                  #341.22
        andnps    %xmm3, %xmm5                                  #341.22
        notl      %edx                                          #345.22
        andl      %eax, %edx                                    #345.38
        andl      $1, %edx                                      #345.46
        je        ..B1.2        # Prob 95%                      #349.52
                                # LOE rbx r12 r13 r14 r15 edx xmm1 xmm5
..B1.4:                         # Preds ..B1.3
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm1, (%rsp)                                 #349.160
        movsd     %xmm5, 64(%rsp)                               #349.233
        jne       ..B1.7        # Prob 5%                       #349.374
                                # LOE rbx r12 r13 r14 r15
..B1.5:                         # Preds ..B1.7 ..B1.4
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm5                               #349.625
        jmp       ..B1.2        # Prob 100%                     #349.625
                                # LOE rbx r12 r13 r14 r15 xmm5
..B1.7:                         # Preds ..B1.4
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #349.546
        lea       64(%rsp), %rsi                                #349.546
..___tag_value___svml_exp1_ha_e9.10:
#       __svml_dexp_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexp_ha_cout_rare_internal             #349.546
..___tag_value___svml_exp1_ha_e9.11:
        jmp       ..B1.5        # Prob 100%                     #349.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_exp1_ha_e9,@function
	.size	__svml_exp1_ha_e9,.-__svml_exp1_ha_e9
..LN__svml_exp1_ha_e9.0:
	.data
# -- End  __svml_exp1_ha_e9
	.text
.L_2__routine_start___svml_exp8_ha_z0_1:
# -- Begin  __svml_exp8_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_exp8_ha_z0
# --- __svml_exp8_ha_z0(__m512d)
__svml_exp8_ha_z0:
# parameter 1: %zmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #357.1
        .byte     15                                            #429.12
        .byte     30                                            #429.12
        .byte     250                                           #429.12
	.cfi_startproc
..___tag_value___svml_exp8_ha_z0.13:
..L14:
                                                         #357.1
        vmovups   256+__svml_dexp_ha_data_internal_avx512(%rip), %zmm4 #396.46
        vmovups   320+__svml_dexp_ha_data_internal_avx512(%rip), %zmm1 #397.50
        vmovups   384+__svml_dexp_ha_data_internal_avx512(%rip), %zmm2 #400.46
        vmovups   448+__svml_dexp_ha_data_internal_avx512(%rip), %zmm3 #401.46
        vmovups   576+__svml_dexp_ha_data_internal_avx512(%rip), %zmm5 #409.54
        vmovups   640+__svml_dexp_ha_data_internal_avx512(%rip), %zmm11 #413.14
        vfmadd213pd {rz-sae}, %zmm1, %zmm0, %zmm4               #398.17
        vmovups   704+__svml_dexp_ha_data_internal_avx512(%rip), %zmm6 #411.54
        vmovups   768+__svml_dexp_ha_data_internal_avx512(%rip), %zmm8 #415.14
        vmovups   832+__svml_dexp_ha_data_internal_avx512(%rip), %zmm7 #416.54
        vmovups   896+__svml_dexp_ha_data_internal_avx512(%rip), %zmm10 #418.14
        vmovups   128+__svml_dexp_ha_data_internal_avx512(%rip), %zmm12 #406.286
        vmovups   __svml_dexp_ha_data_internal_avx512(%rip), %zmm13 #405.285
        vsubpd    {rn-sae}, %zmm1, %zmm4, %zmm15                #399.14
        vpermt2pd 192+__svml_dexp_ha_data_internal_avx512(%rip), %zmm4, %zmm12 #406.286
        vpermt2pd 64+__svml_dexp_ha_data_internal_avx512(%rip), %zmm4, %zmm13 #405.285
        vfnmadd231pd {rn-sae}, %zmm15, %zmm2, %zmm0             #402.12
        vfnmadd231pd {rn-sae}, %zmm15, %zmm3, %zmm0             #403.12
        vandpd    512+__svml_dexp_ha_data_internal_avx512(%rip), %zmm0, %zmm14 #408.16
        vmulpd    {rn-sae}, %zmm14, %zmm14, %zmm9               #414.15
        vfmadd231pd {rn-sae}, %zmm14, %zmm5, %zmm11             #413.14
        vfmadd231pd {rn-sae}, %zmm14, %zmm6, %zmm8              #415.14
        vfmadd231pd {rn-sae}, %zmm14, %zmm7, %zmm10             #418.14
        vfmadd213pd {rn-sae}, %zmm8, %zmm9, %zmm11              #419.14
        vfmadd213pd {rn-sae}, %zmm10, %zmm9, %zmm11             #420.14
        vfmadd213pd {rn-sae}, %zmm12, %zmm11, %zmm14            #421.12
        vfmadd213pd {rn-sae}, %zmm13, %zmm13, %zmm14            #423.16
        vscalefpd {rn-sae}, %zmm15, %zmm14, %zmm0               #424.18
        ret                                                     #429.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_exp8_ha_z0,@function
	.size	__svml_exp8_ha_z0,.-__svml_exp8_ha_z0
..LN__svml_exp8_ha_z0.1:
	.data
# -- End  __svml_exp8_ha_z0
	.text
.L_2__routine_start___svml_exp1_ha_l9_2:
# -- Begin  __svml_exp1_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_exp1_ha_l9
# --- __svml_exp1_ha_l9(__m128d)
__svml_exp1_ha_l9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #434.1
        .byte     15                                            #516.546
        .byte     30                                            #516.546
        .byte     250                                           #516.546
	.cfi_startproc
..___tag_value___svml_exp1_ha_l9.16:
..L17:
                                                         #434.1
        pushq     %rbp                                          #434.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #434.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #434.1
        subq      $128, %rsp                                    #434.1
        vmovapd   %xmm0, %xmm1                                  #434.1
        vmovsd    1088+__svml_dexp_ha_data_internal(%rip), %xmm3 #457.20
        vmovapd   %xmm1, %xmm2                                  #464.13
        vfmadd132sd 1024+__svml_dexp_ha_data_internal(%rip), %xmm3, %xmm2 #464.13
        lea       __svml_dexp_ha_data_internal(%rip), %rdx      #477.53
        vmovsd    1536+__svml_dexp_ha_data_internal(%rip), %xmm13 #482.13
        vmovq     1664+__svml_dexp_ha_data_internal(%rip), %xmm15 #473.28
        vmovsd    1728+__svml_dexp_ha_data_internal(%rip), %xmm4 #466.23
        vandpd    %xmm4, %xmm1, %xmm5                           #468.20
        vsubsd    %xmm3, %xmm2, %xmm11                          #465.13
        vcmpnlesd 1792+__svml_dexp_ha_data_internal(%rip), %xmm5, %xmm6 #469.25
        vaddsd    1152+__svml_dexp_ha_data_internal(%rip), %xmm11, %xmm0 #474.17
        vmovmskpd %xmm6, %ecx                                   #471.48
        vmovapd   %xmm11, %xmm9                                 #480.13
        vpand     %xmm15, %xmm0, %xmm7                          #476.23
        vfnmadd132sd 1216+__svml_dexp_ha_data_internal(%rip), %xmm1, %xmm9 #480.13
        vpandn    %xmm0, %xmm15, %xmm15                         #478.19
        vpsllq    $42, %xmm15, %xmm3                            #479.19
        vmovd     %xmm7, %eax                                   #477.89
        vfnmadd132sd 1280+__svml_dexp_ha_data_internal(%rip), %xmm9, %xmm11 #481.13
        vfmadd213sd 1472+__svml_dexp_ha_data_internal(%rip), %xmm11, %xmm13 #482.13
        vmulsd    %xmm11, %xmm11, %xmm10                        #484.14
        vfmadd213sd 1408+__svml_dexp_ha_data_internal(%rip), %xmm11, %xmm13 #483.13
        vfmadd213sd 1344+__svml_dexp_ha_data_internal(%rip), %xmm11, %xmm13 #485.13
        movslq    %eax, %rax                                    #477.53
        vfmadd213sd %xmm11, %xmm10, %xmm13                      #486.13
        vmovsd    8(%rdx,%rax), %xmm8                           #477.53
        vmovupd   (%rdx,%rax), %xmm14                           #477.53
        vunpcklpd %xmm8, %xmm8, %xmm12                          #477.135
        vfmadd213sd %xmm12, %xmm14, %xmm13                      #487.13
        vaddsd    %xmm14, %xmm13, %xmm2                         #488.13
        vpaddq    %xmm3, %xmm2, %xmm6                           #490.15
        testb     $1, %cl                                       #492.37
        jne       ..B3.3        # Prob 5%                       #492.52
                                # LOE rbx r12 r13 r14 r15 ecx xmm1 xmm6
..B3.2:                         # Preds ..B3.1 ..B3.5 ..B3.3
                                # Execution count [1.00e+00]
        vmovapd   %xmm6, %xmm0                                  #519.12
        movq      %rbp, %rsp                                    #519.12
        popq      %rbp                                          #519.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #519.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovsd    1984+__svml_dexp_ha_data_internal(%rip), %xmm2 #503.33
        vcmpltsd  %xmm1, %xmm2, %xmm3                           #504.28
        vcmpltsd  2048+__svml_dexp_ha_data_internal(%rip), %xmm1, %xmm4 #506.28
        vmovsd    %xmm3, %xmm1, %xmm0                           #504.28
        vorpd     %xmm4, %xmm0, %xmm5                           #509.28
        vmovmskpd %xmm5, %eax                                   #511.68
        notl      %eax                                          #512.22
        andl      %ecx, %eax                                    #512.38
        blendvpd  %xmm0, .L_2il0floatpacket.33(%rip), %xmm6     #507.22
        vandnpd   %xmm6, %xmm4, %xmm6                           #508.22
        andl      $1, %eax                                      #512.46
        je        ..B3.2        # Prob 95%                      #516.52
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm6
..B3.4:                         # Preds ..B3.3
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm1, (%rsp)                                 #516.160
        vmovsd    %xmm6, 64(%rsp)                               #516.233
        jne       ..B3.6        # Prob 5%                       #516.374
                                # LOE rbx r12 r13 r14 r15 eax
..B3.5:                         # Preds ..B3.7 ..B3.6 ..B3.4
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm6                               #516.625
        jmp       ..B3.2        # Prob 100%                     #516.625
                                # LOE rbx r12 r13 r14 r15 xmm6
..B3.6:                         # Preds ..B3.4
                                # Execution count [2.50e-03]: Infreq
        je        ..B3.5        # Prob 95%                      #516.517
                                # LOE rbx r12 r13 r14 r15
..B3.7:                         # Preds ..B3.6
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #516.546
        lea       64(%rsp), %rsi                                #516.546
..___tag_value___svml_exp1_ha_l9.25:
#       __svml_dexp_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexp_ha_cout_rare_internal             #516.546
..___tag_value___svml_exp1_ha_l9.26:
        jmp       ..B3.5        # Prob 100%                     #516.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_exp1_ha_l9,@function
	.size	__svml_exp1_ha_l9,.-__svml_exp1_ha_l9
..LN__svml_exp1_ha_l9.2:
	.data
# -- End  __svml_exp1_ha_l9
	.text
.L_2__routine_start___svml_exp2_ha_ex_3:
# -- Begin  __svml_exp2_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_exp2_ha_ex
# --- __svml_exp2_ha_ex(__m128d)
__svml_exp2_ha_ex:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #524.1
        .byte     15                                            #607.546
        .byte     30                                            #607.546
        .byte     250                                           #607.546
	.cfi_startproc
..___tag_value___svml_exp2_ha_ex.28:
..L29:
                                                         #524.1
        pushq     %rbp                                          #524.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #524.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #524.1
        subq      $192, %rsp                                    #524.1
        movaps    %xmm0, %xmm3                                  #524.1
        movups    1024+__svml_dexp_ha_data_internal(%rip), %xmm2 #554.25
        lea       __svml_dexp_ha_data_internal(%rip), %rcx      #569.71
        mulpd     %xmm3, %xmm2                                  #554.25
        movups    1088+__svml_dexp_ha_data_internal(%rip), %xmm4 #547.51
        addpd     %xmm4, %xmm2                                  #554.13
        movaps    %xmm2, %xmm1                                  #555.13
        subpd     %xmm4, %xmm1                                  #555.13
        movups    1216+__svml_dexp_ha_data_internal(%rip), %xmm12 #571.33
        mulpd     %xmm1, %xmm12                                 #571.33
        movdqu    1600+__svml_dexp_ha_data_internal(%rip), %xmm4 #563.60
        movdqa    %xmm4, %xmm8                                  #565.21
        pandn     %xmm2, %xmm4                                  #567.17
        pand      %xmm2, %xmm8                                  #565.21
        psllq     $46, %xmm4                                    #570.17
        movups    1280+__svml_dexp_ha_data_internal(%rip), %xmm13 #572.32
        pshufd    $136, %xmm8, %xmm9                            #566.21
        mulpd     %xmm1, %xmm13                                 #572.32
        pslld     $4, %xmm9                                     #568.21
        movd      %xmm9, %eax                                   #569.116
        pshufd    $1, %xmm9, %xmm10                             #569.272
        movaps    %xmm3, %xmm9                                  #571.13
        movups    1536+__svml_dexp_ha_data_internal(%rip), %xmm14 #573.25
        subpd     %xmm12, %xmm9                                 #571.13
        movd      %xmm10, %edx                                  #569.254
        movq      1856+__svml_dexp_ha_data_internal(%rip), %xmm5 #556.23
        subpd     %xmm13, %xmm9                                 #572.13
        mulpd     %xmm9, %xmm14                                 #573.25
        addpd     1472+__svml_dexp_ha_data_internal(%rip), %xmm14 #573.13
        mulpd     %xmm9, %xmm14                                 #574.25
        addpd     1408+__svml_dexp_ha_data_internal(%rip), %xmm14 #574.13
        mulpd     %xmm9, %xmm14                                 #576.25
        movaps    %xmm9, %xmm15                                 #575.14
        mulpd     %xmm9, %xmm15                                 #575.14
        addpd     1344+__svml_dexp_ha_data_internal(%rip), %xmm14 #576.13
        mulpd     %xmm14, %xmm15                                #577.25
        movslq    %eax, %rax                                    #569.71
        movslq    %edx, %rdx                                    #569.209
        addpd     %xmm15, %xmm9                                 #577.13
        movups    (%rcx,%rax), %xmm8                            #569.71
        movups    (%rcx,%rdx), %xmm11                           #569.209
        movaps    %xmm8, %xmm0                                  #569.345
        unpcklpd  %xmm11, %xmm0                                 #569.345
        mulpd     %xmm0, %xmm9                                  #578.25
        unpckhpd  %xmm11, %xmm8                                 #569.392
        addpd     %xmm9, %xmm8                                  #578.13
        pshufd    $221, %xmm3, %xmm7                            #559.20
        addpd     %xmm8, %xmm0                                  #579.13
        movq      1920+__svml_dexp_ha_data_internal(%rip), %xmm6 #557.27
        pand      %xmm5, %xmm7                                  #560.20
        pcmpgtd   %xmm6, %xmm7                                  #561.25
        movmskps  %xmm7, %esi                                   #562.48
        paddq     %xmm4, %xmm0                                  #581.13
        testl     $3, %esi                                      #583.37
        jne       ..B4.3        # Prob 5%                       #583.52
                                # LOE rbx r12 r13 r14 r15 esi xmm0 xmm3
..B4.2:                         # Preds ..B4.4 ..B4.10 ..B4.1 ..B4.3
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #610.12
        popq      %rbp                                          #610.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #610.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        movups    1984+__svml_dexp_ha_data_internal(%rip), %xmm5 #594.64
        movaps    %xmm3, %xmm4                                  #597.28
        cmpltpd   2048+__svml_dexp_ha_data_internal(%rip), %xmm4 #597.28
        cmpltpd   %xmm3, %xmm5                                  #595.28
        movups    .L_2il0floatpacket.33(%rip), %xmm1            #593.24
        movaps    %xmm5, %xmm2                                  #598.33
        andps     %xmm5, %xmm1                                  #598.72
        orps      %xmm4, %xmm5                                  #600.28
        movmskpd  %xmm5, %edx                                   #602.68
        andnps    %xmm0, %xmm2                                  #598.33
        orps      %xmm1, %xmm2                                  #598.22
        movaps    %xmm4, %xmm0                                  #599.22
        andnps    %xmm2, %xmm0                                  #599.22
        notl      %edx                                          #603.22
        andl      %esi, %edx                                    #603.38
        andl      $3, %edx                                      #603.46
        je        ..B4.2        # Prob 95%                      #607.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm3
..B4.4:                         # Preds ..B4.3
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm3, 64(%rsp)                               #607.197
        movups    %xmm0, 128(%rsp)                              #607.270
        je        ..B4.2        # Prob 95%                      #607.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B4.7:                         # Preds ..B4.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #607.454
        movq      %r12, 8(%rsp)                                 #607.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #607.454
        movq      %r13, (%rsp)                                  #607.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #607.454
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.9 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #607.517
        jc        ..B4.11       # Prob 5%                       #607.517
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.11 ..B4.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #607.470
        cmpl      $2, %r12d                                     #607.465
        jl        ..B4.8        # Prob 82%                      #607.465
                                # LOE rbx r12 r14 r15 r13d
..B4.10:                        # Preds ..B4.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #607.673
        jmp       ..B4.2        # Prob 100%                     #607.673
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.11:                        # Preds ..B4.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #607.546
        lea       128(%rsp,%r12,8), %rsi                        #607.546
..___tag_value___svml_exp2_ha_ex.46:
#       __svml_dexp_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexp_ha_cout_rare_internal             #607.546
..___tag_value___svml_exp2_ha_ex.47:
        jmp       ..B4.9        # Prob 100%                     #607.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_exp2_ha_ex,@function
	.size	__svml_exp2_ha_ex,.-__svml_exp2_ha_ex
..LN__svml_exp2_ha_ex.3:
	.data
# -- End  __svml_exp2_ha_ex
	.text
.L_2__routine_start___svml_exp4_ha_e9_4:
# -- Begin  __svml_exp4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_exp4_ha_e9
# --- __svml_exp4_ha_e9(__m256d)
__svml_exp4_ha_e9:
# parameter 1: %ymm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #615.1
        .byte     15                                            #698.552
        .byte     30                                            #698.552
        .byte     250                                           #698.552
	.cfi_startproc
..___tag_value___svml_exp4_ha_e9.49:
..L50:
                                                         #615.1
        pushq     %rbp                                          #615.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #615.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #615.1
        subq      $192, %rsp                                    #615.1
        lea       __svml_dexp_ha_data_internal(%rip), %r8       #660.49
        vmovapd   %ymm0, %ymm6                                  #615.1
        vmovupd   1088+__svml_dexp_ha_data_internal(%rip), %ymm8 #638.54
        vmulpd    1024+__svml_dexp_ha_data_internal(%rip), %ymm6, %ymm7 #645.28
        vmovupd   1600+__svml_dexp_ha_data_internal(%rip), %xmm4 #654.31
        vaddpd    %ymm7, %ymm8, %ymm3                           #645.13
        vsubpd    %ymm8, %ymm3, %ymm5                           #646.13
        vextractf128 $1, %ymm6, %xmm9                           #649.109
        vshufps   $221, %xmm9, %xmm6, %xmm10                    #650.44
        vandps    1856+__svml_dexp_ha_data_internal(%rip), %xmm10, %xmm11 #651.26
        vmulpd    1216+__svml_dexp_ha_data_internal(%rip), %ymm5, %ymm10 #662.36
        vmulpd    1280+__svml_dexp_ha_data_internal(%rip), %ymm5, %ymm5 #663.35
        vpcmpgtd  1920+__svml_dexp_ha_data_internal(%rip), %xmm11, %xmm12 #652.31
        vmovmskps %xmm12, %eax                                  #653.48
        vsubpd    %ymm10, %ymm6, %ymm11                         #662.13
        vsubpd    %ymm5, %ymm11, %ymm5                          #663.13
        vmulpd    1536+__svml_dexp_ha_data_internal(%rip), %ymm5, %ymm12 #664.28
        vandps    %xmm4, %xmm3, %xmm13                          #656.27
        vextractf128 $1, %ymm3, %xmm2                           #655.108
        vpandn    %xmm3, %xmm4, %xmm3                           #658.23
        vpsllq    $46, %xmm3, %xmm3                             #661.23
        vandps    %xmm4, %xmm2, %xmm14                          #656.96
        vpandn    %xmm2, %xmm4, %xmm2                           #658.91
        vshufps   $136, %xmm14, %xmm13, %xmm15                  #657.45
        vpsllq    $46, %xmm2, %xmm4                             #661.76
        vaddpd    1472+__svml_dexp_ha_data_internal(%rip), %ymm12, %ymm13 #664.13
        vpslld    $4, %xmm15, %xmm1                             #659.27
        vmovd     %xmm1, %edx                                   #660.94
        vmulpd    %ymm5, %ymm5, %ymm12                          #666.14
        vmulpd    %ymm13, %ymm5, %ymm14                         #665.28
        vaddpd    1408+__svml_dexp_ha_data_internal(%rip), %ymm14, %ymm15 #665.13
        vmulpd    %ymm15, %ymm5, %ymm15                         #667.28
        vpextrd   $1, %xmm1, %ecx                               #660.212
        movslq    %ecx, %rcx                                    #660.167
        vpextrd   $3, %xmm1, %edi                               #660.454
        movslq    %edi, %rdi                                    #660.409
        vaddpd    1344+__svml_dexp_ha_data_internal(%rip), %ymm15, %ymm13 #667.13
        vmovupd   (%r8,%rcx), %xmm7                             #660.167
        movslq    %edx, %rdx                                    #660.49
        vpextrd   $2, %xmm1, %esi                               #660.333
        movslq    %esi, %rsi                                    #660.288
        vinsertf128 $1, (%r8,%rdi), %ymm7, %ymm9                #660.589
        vmulpd    %ymm13, %ymm12, %ymm7                         #668.28
        vmovupd   (%r8,%rdx), %xmm0                             #660.49
        vinsertf128 $1, (%r8,%rsi), %ymm0, %ymm8                #660.508
        vunpcklpd %ymm9, %ymm8, %ymm1                           #660.666
        vunpckhpd %ymm9, %ymm8, %ymm0                           #660.712
        vaddpd    %ymm7, %ymm5, %ymm8                           #668.13
        vmulpd    %ymm8, %ymm1, %ymm9                           #669.28
        vaddpd    %ymm9, %ymm0, %ymm0                           #669.13
        vaddpd    %ymm0, %ymm1, %ymm1                           #670.13
        vextractf128 $1, %ymm1, %xmm10                          #671.104
        vpaddq    %xmm3, %xmm1, %xmm0                           #672.19
        vpaddq    %xmm4, %xmm10, %xmm1                          #672.76
        vinsertf128 $1, %xmm1, %ymm0, %ymm0                     #673.14
        testl     %eax, %eax                                    #674.52
        jne       ..B5.3        # Prob 5%                       #674.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm6
..B5.2:                         # Preds ..B5.4 ..B5.10 ..B5.1 ..B5.3
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #701.12
        popq      %rbp                                          #701.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #701.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vcmpgt_oqpd 1984+__svml_dexp_ha_data_internal(%rip), %ymm6, %ymm1 #686.28
        vcmplt_oqpd 2048+__svml_dexp_ha_data_internal(%rip), %ymm6, %ymm2 #688.28
        vblendvpd %ymm1, .L_2il0floatpacket.34(%rip), %ymm0, %ymm0 #689.22
        vorpd     %ymm2, %ymm1, %ymm3                           #691.28
        vandnpd   %ymm0, %ymm2, %ymm0                           #690.22
        vextractf128 $1, %ymm3, %xmm4                           #692.135
        vshufps   $221, %xmm4, %xmm3, %xmm5                     #693.85
        vmovmskps %xmm5, %edx                                   #693.68
        notl      %edx                                          #694.22
        andl      %eax, %edx                                    #694.38
        je        ..B5.2        # Prob 95%                      #698.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm6
..B5.4:                         # Preds ..B5.3
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm6, 64(%rsp)                               #698.200
        vmovupd   %ymm0, 128(%rsp)                              #698.276
        je        ..B5.2        # Prob 95%                      #698.380
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B5.7:                         # Preds ..B5.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #698.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B5.14:                        # Preds ..B5.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.9 ..B5.14
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #698.523
        jc        ..B5.11       # Prob 5%                       #698.523
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.11 ..B5.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #698.476
        cmpl      $4, %r12d                                     #698.471
        jl        ..B5.8        # Prob 82%                      #698.471
                                # LOE rbx r12 r14 r15 r13d
..B5.10:                        # Preds ..B5.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #698.682
        jmp       ..B5.2        # Prob 100%                     #698.682
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B5.11:                        # Preds ..B5.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #698.552
        lea       128(%rsp,%r12,8), %rsi                        #698.552
..___tag_value___svml_exp4_ha_e9.67:
#       __svml_dexp_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexp_ha_cout_rare_internal             #698.552
..___tag_value___svml_exp4_ha_e9.68:
        jmp       ..B5.9        # Prob 100%                     #698.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_exp4_ha_e9,@function
	.size	__svml_exp4_ha_e9,.-__svml_exp4_ha_e9
..LN__svml_exp4_ha_e9.4:
	.data
# -- End  __svml_exp4_ha_e9
	.text
.L_2__routine_start___svml_exp2_ha_l9_5:
# -- Begin  __svml_exp2_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_exp2_ha_l9
# --- __svml_exp2_ha_l9(__m128d)
__svml_exp2_ha_l9:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #706.1
        .byte     15                                            #788.546
        .byte     30                                            #788.546
        .byte     250                                           #788.546
	.cfi_startproc
..___tag_value___svml_exp2_ha_l9.70:
..L71:
                                                         #706.1
        pushq     %rbp                                          #706.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #706.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #706.1
        subq      $192, %rsp                                    #706.1
        vmovapd   %xmm0, %xmm1                                  #706.1
        vmovupd   1024+__svml_dexp_ha_data_internal(%rip), %xmm2 #728.50
        lea       __svml_dexp_ha_data_internal(%rip), %rcx      #749.71
        vmovupd   1088+__svml_dexp_ha_data_internal(%rip), %xmm3 #729.51
        vfmadd213pd %xmm3, %xmm1, %xmm2                         #736.13
        vmovupd   1216+__svml_dexp_ha_data_internal(%rip), %xmm9 #752.13
        vmovupd   1536+__svml_dexp_ha_data_internal(%rip), %xmm14 #754.13
        vmovdqu   1664+__svml_dexp_ha_data_internal(%rip), %xmm15 #745.63
        vandpd    1728+__svml_dexp_ha_data_internal(%rip), %xmm1, %xmm4 #740.20
        vsubpd    %xmm3, %xmm2, %xmm11                          #737.13
        vcmpnlepd 1792+__svml_dexp_ha_data_internal(%rip), %xmm4, %xmm5 #741.25
        vfnmadd213pd %xmm1, %xmm11, %xmm9                       #752.13
        vaddpd    1152+__svml_dexp_ha_data_internal(%rip), %xmm11, %xmm0 #746.17
        vmovmskpd %xmm5, %esi                                   #743.48
        vfnmadd132pd 1280+__svml_dexp_ha_data_internal(%rip), %xmm9, %xmm11 #753.13
        vpand     %xmm15, %xmm0, %xmm6                          #748.23
        vpandn    %xmm0, %xmm15, %xmm15                         #750.19
        vpsllq    $42, %xmm15, %xmm3                            #751.19
        vmovd     %xmm6, %eax                                   #749.116
        vfmadd213pd 1472+__svml_dexp_ha_data_internal(%rip), %xmm11, %xmm14 #754.13
        vmulpd    %xmm11, %xmm11, %xmm10                        #756.14
        vfmadd213pd 1408+__svml_dexp_ha_data_internal(%rip), %xmm11, %xmm14 #755.13
        vfmadd213pd 1344+__svml_dexp_ha_data_internal(%rip), %xmm11, %xmm14 #757.13
        vpextrd   $2, %xmm6, %edx                               #749.256
        movslq    %eax, %rax                                    #749.71
        movslq    %edx, %rdx                                    #749.211
        vfmadd213pd %xmm11, %xmm10, %xmm14                      #758.13
        vmovupd   (%rcx,%rax), %xmm7                            #749.71
        vmovupd   (%rcx,%rdx), %xmm8                            #749.211
        vunpcklpd %xmm8, %xmm7, %xmm13                          #749.306
        vunpckhpd %xmm8, %xmm7, %xmm12                          #749.347
        vfmadd213pd %xmm12, %xmm13, %xmm14                      #759.13
        vaddpd    %xmm14, %xmm13, %xmm2                         #760.13
        vpaddq    %xmm3, %xmm2, %xmm5                           #762.15
        testl     %esi, %esi                                    #764.52
        jne       ..B6.3        # Prob 5%                       #764.52
                                # LOE rbx r12 r13 r14 r15 esi xmm1 xmm5
..B6.2:                         # Preds ..B6.4 ..B6.10 ..B6.1 ..B6.3
                                # Execution count [1.00e+00]
        movaps    %xmm5, %xmm0                                  #791.12
        movq      %rbp, %rsp                                    #791.12
        popq      %rbp                                          #791.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #791.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   1984+__svml_dexp_ha_data_internal(%rip), %xmm2 #775.64
        vcmpltpd  2048+__svml_dexp_ha_data_internal(%rip), %xmm1, %xmm3 #778.28
        vcmpltpd  %xmm1, %xmm2, %xmm0                           #776.28
        vorpd     %xmm3, %xmm0, %xmm4                           #781.28
        vmovmskpd %xmm4, %edx                                   #783.68
        notl      %edx                                          #784.22
        blendvpd  %xmm0, .L_2il0floatpacket.33(%rip), %xmm5     #779.22
        vandnpd   %xmm5, %xmm3, %xmm5                           #780.22
        andl      %esi, %edx                                    #784.38
        je        ..B6.2        # Prob 95%                      #788.52
                                # LOE rbx r12 r13 r14 r15 edx xmm1 xmm5
..B6.4:                         # Preds ..B6.3
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm1, 64(%rsp)                               #788.197
        vmovupd   %xmm5, 128(%rsp)                              #788.270
        je        ..B6.2        # Prob 95%                      #788.374
                                # LOE rbx r12 r13 r14 r15 edx xmm5
..B6.7:                         # Preds ..B6.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #788.454
        movq      %r12, 8(%rsp)                                 #788.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #788.454
        movq      %r13, (%rsp)                                  #788.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #788.454
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.9 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #788.517
        jc        ..B6.11       # Prob 5%                       #788.517
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.11 ..B6.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #788.470
        cmpl      $2, %r12d                                     #788.465
        jl        ..B6.8        # Prob 82%                      #788.465
                                # LOE rbx r12 r14 r15 r13d
..B6.10:                        # Preds ..B6.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm5                              #788.673
        jmp       ..B6.2        # Prob 100%                     #788.673
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm5
..B6.11:                        # Preds ..B6.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #788.546
        lea       128(%rsp,%r12,8), %rsi                        #788.546
..___tag_value___svml_exp2_ha_l9.88:
#       __svml_dexp_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexp_ha_cout_rare_internal             #788.546
..___tag_value___svml_exp2_ha_l9.89:
        jmp       ..B6.9        # Prob 100%                     #788.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_exp2_ha_l9,@function
	.size	__svml_exp2_ha_l9,.-__svml_exp2_ha_l9
..LN__svml_exp2_ha_l9.5:
	.data
# -- End  __svml_exp2_ha_l9
	.text
.L_2__routine_start___svml_exp1_ha_ex_6:
# -- Begin  __svml_exp1_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_exp1_ha_ex
# --- __svml_exp1_ha_ex(__m128d)
__svml_exp1_ha_ex:
# parameter 1: %xmm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #796.1
        .byte     15                                            #879.546
        .byte     30                                            #879.546
        .byte     250                                           #879.546
	.cfi_startproc
..___tag_value___svml_exp1_ha_ex.91:
..L92:
                                                         #796.1
        pushq     %rbp                                          #796.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #796.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #796.1
        subq      $128, %rsp                                    #796.1
        movaps    %xmm0, %xmm13                                 #826.25
        mulsd     1024+__svml_dexp_ha_data_internal(%rip), %xmm13 #826.25
        movaps    %xmm0, %xmm10                                 #843.13
        movsd     1088+__svml_dexp_ha_data_internal(%rip), %xmm1 #819.20
        lea       __svml_dexp_ha_data_internal(%rip), %rsi      #841.59
        movq      1600+__svml_dexp_ha_data_internal(%rip), %xmm12 #835.25
        movl      $2147483647, %eax                             #832.20
        movdqa    %xmm12, %xmm5                                 #837.21
        movl      $1082532650, %edx                             #833.25
        pshufd    $85, %xmm0, %xmm4                             #831.20
        addsd     %xmm1, %xmm13                                 #826.13
        movaps    %xmm13, %xmm8                                 #827.13
        pand      %xmm13, %xmm5                                 #837.21
        pshufd    $0, %xmm5, %xmm6                              #838.21
        movd      %eax, %xmm2                                   #832.20
        pslld     $4, %xmm6                                     #840.21
        pand      %xmm2, %xmm4                                  #832.20
        movd      %xmm6, %ecx                                   #841.95
        movd      %edx, %xmm3                                   #833.25
        movsd     1536+__svml_dexp_ha_data_internal(%rip), %xmm6 #845.25
        pcmpgtd   %xmm3, %xmm4                                  #833.25
        pandn     %xmm13, %xmm12                                #839.17
        subsd     %xmm1, %xmm8                                  #827.13
        movmskps  %xmm4, %eax                                   #834.48
        movaps    %xmm8, %xmm7                                  #843.33
        psllq     $46, %xmm12                                   #842.17
        mulsd     1216+__svml_dexp_ha_data_internal(%rip), %xmm7 #843.33
        mulsd     1280+__svml_dexp_ha_data_internal(%rip), %xmm8 #844.32
        subsd     %xmm7, %xmm10                                 #843.13
        movslq    %ecx, %rcx                                    #841.59
        subsd     %xmm8, %xmm10                                 #844.13
        mulsd     %xmm10, %xmm6                                 #845.25
        movaps    %xmm10, %xmm9                                 #847.14
        mulsd     %xmm10, %xmm9                                 #847.14
        addsd     1472+__svml_dexp_ha_data_internal(%rip), %xmm6 #845.13
        mulsd     %xmm10, %xmm6                                 #846.25
        movups    (%rsi,%rcx), %xmm11                           #841.59
        addsd     1408+__svml_dexp_ha_data_internal(%rip), %xmm6 #846.13
        mulsd     %xmm10, %xmm6                                 #848.25
        addsd     1344+__svml_dexp_ha_data_internal(%rip), %xmm6 #848.13
        mulsd     %xmm9, %xmm6                                  #849.25
        addsd     %xmm10, %xmm6                                 #849.13
        mulsd     %xmm11, %xmm6                                 #850.25
        addsd     8(%rsi,%rcx), %xmm6                           #850.13
        addsd     %xmm11, %xmm6                                 #851.13
        paddq     %xmm12, %xmm6                                 #853.13
        testb     $1, %al                                       #855.37
        jne       ..B7.3        # Prob 5%                       #855.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm6
..B7.2:                         # Preds ..B7.1 ..B7.5 ..B7.3
                                # Execution count [1.00e+00]
        movaps    %xmm6, %xmm0                                  #882.12
        movq      %rbp, %rsp                                    #882.12
        popq      %rbp                                          #882.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #882.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.3:                         # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        movsd     1984+__svml_dexp_ha_data_internal(%rip), %xmm1 #866.33
        movaps    %xmm0, %xmm4                                  #869.28
        movaps    %xmm0, %xmm5                                  #867.28
        cmpltsd   %xmm0, %xmm1                                  #867.28
        cmpltsd   2048+__svml_dexp_ha_data_internal(%rip), %xmm4 #869.28
        movups    .L_2il0floatpacket.33(%rip), %xmm2            #865.24
        movsd     %xmm1, %xmm5                                  #867.28
        movaps    %xmm5, %xmm3                                  #870.33
        andps     %xmm5, %xmm2                                  #870.72
        orps      %xmm4, %xmm5                                  #872.28
        andnps    %xmm6, %xmm3                                  #870.33
        movmskpd  %xmm5, %edx                                   #874.68
        orps      %xmm2, %xmm3                                  #870.22
        movaps    %xmm4, %xmm6                                  #871.22
        andnps    %xmm3, %xmm6                                  #871.22
        notl      %edx                                          #875.22
        andl      %eax, %edx                                    #875.38
        andl      $1, %edx                                      #875.46
        je        ..B7.2        # Prob 95%                      #879.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm6
..B7.4:                         # Preds ..B7.3
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm0, (%rsp)                                 #879.160
        movsd     %xmm6, 64(%rsp)                               #879.233
        jne       ..B7.7        # Prob 5%                       #879.374
                                # LOE rbx r12 r13 r14 r15
..B7.5:                         # Preds ..B7.7 ..B7.4
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm6                               #879.625
        jmp       ..B7.2        # Prob 100%                     #879.625
                                # LOE rbx r12 r13 r14 r15 xmm6
..B7.7:                         # Preds ..B7.4
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #879.546
        lea       64(%rsp), %rsi                                #879.546
..___tag_value___svml_exp1_ha_ex.100:
#       __svml_dexp_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexp_ha_cout_rare_internal             #879.546
..___tag_value___svml_exp1_ha_ex.101:
        jmp       ..B7.5        # Prob 100%                     #879.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_exp1_ha_ex,@function
	.size	__svml_exp1_ha_ex,.-__svml_exp1_ha_ex
..LN__svml_exp1_ha_ex.6:
	.data
# -- End  __svml_exp1_ha_ex
	.text
.L_2__routine_start___svml_exp4_ha_l9_7:
# -- Begin  __svml_exp4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_exp4_ha_l9
# --- __svml_exp4_ha_l9(__m256d)
__svml_exp4_ha_l9:
# parameter 1: %ymm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #887.1
        .byte     15                                            #969.552
        .byte     30                                            #969.552
        .byte     250                                           #969.552
	.cfi_startproc
..___tag_value___svml_exp4_ha_l9.103:
..L104:
                                                        #887.1
        pushq     %rbp                                          #887.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #887.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #887.1
        subq      $192, %rsp                                    #887.1
        lea       __svml_dexp_ha_data_internal(%rip), %r8       #930.72
        vmovupd   1024+__svml_dexp_ha_data_internal(%rip), %ymm4 #909.53
        vmovupd   1088+__svml_dexp_ha_data_internal(%rip), %ymm5 #910.54
        vmovupd   1664+__svml_dexp_ha_data_internal(%rip), %ymm1 #926.28
        vmovapd   %ymm0, %ymm3                                  #887.1
        vfmadd213pd %ymm5, %ymm3, %ymm4                         #917.13
        vsubpd    %ymm5, %ymm4, %ymm2                           #918.13
        vaddpd    1152+__svml_dexp_ha_data_internal(%rip), %ymm2, %ymm0 #927.17
        vandps    %ymm1, %ymm0, %ymm8                           #929.23
        vandpd    1728+__svml_dexp_ha_data_internal(%rip), %ymm3, %ymm6 #921.20
        vcmpnle_uqpd 1792+__svml_dexp_ha_data_internal(%rip), %ymm6, %ymm7 #922.25
        vpandn    %ymm0, %ymm1, %ymm0                           #931.19
        vmovmskpd %ymm7, %eax                                   #924.48
        vmovupd   1536+__svml_dexp_ha_data_internal(%rip), %ymm7 #935.13
        vpsllq    $42, %ymm0, %ymm1                             #932.19
        vmovd     %xmm8, %edx                                   #930.117
        vextractf128 $1, %ymm8, %xmm9                           #930.477
        vpextrd   $2, %xmm8, %ecx                               #930.286
        movslq    %edx, %rdx                                    #930.72
        movslq    %ecx, %rcx                                    #930.241
        vmovd     %xmm9, %esi                                   #930.458
        vmovupd   (%r8,%rdx), %xmm12                            #930.72
        vmovupd   (%r8,%rcx), %xmm13                            #930.241
        vunpcklpd %xmm13, %xmm12, %xmm10                        #930.761
        vunpckhpd %xmm13, %xmm12, %xmm14                        #930.888
        vmovupd   1216+__svml_dexp_ha_data_internal(%rip), %ymm13 #933.13
        vfnmadd213pd %ymm3, %ymm2, %ymm13                       #933.13
        vfnmadd132pd 1280+__svml_dexp_ha_data_internal(%rip), %ymm13, %ymm2 #934.13
        vfmadd213pd 1472+__svml_dexp_ha_data_internal(%rip), %ymm2, %ymm7 #935.13
        vpextrd   $2, %xmm9, %edi                               #930.632
        movslq    %esi, %rsi                                    #930.413
        movslq    %edi, %rdi                                    #930.587
        vfmadd213pd 1408+__svml_dexp_ha_data_internal(%rip), %ymm2, %ymm7 #936.13
        vmovupd   (%r8,%rsi), %xmm15                            #930.413
        vmovupd   (%r8,%rdi), %xmm4                             #930.587
        vunpcklpd %xmm4, %xmm15, %xmm11                         #930.794
        vunpckhpd %xmm4, %xmm15, %xmm12                         #930.921
        vmulpd    %ymm2, %ymm2, %ymm15                          #937.14
        vfmadd213pd 1344+__svml_dexp_ha_data_internal(%rip), %ymm2, %ymm7 #938.13
        vfmadd213pd %ymm2, %ymm15, %ymm7                        #939.13
        vinsertf128 $1, %xmm11, %ymm10, %ymm6                   #930.715
        vinsertf128 $1, %xmm12, %ymm14, %ymm5                   #930.842
        vfmadd213pd %ymm5, %ymm6, %ymm7                         #940.13
        vaddpd    %ymm7, %ymm6, %ymm2                           #941.13
        vpaddq    %ymm1, %ymm2, %ymm0                           #943.15
        testl     %eax, %eax                                    #945.52
        jne       ..B8.3        # Prob 5%                       #945.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm3
..B8.2:                         # Preds ..B8.4 ..B8.10 ..B8.1 ..B8.3
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #972.12
        popq      %rbp                                          #972.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #972.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.3:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        vcmpgt_oqpd 1984+__svml_dexp_ha_data_internal(%rip), %ymm3, %ymm1 #957.28
        vcmplt_oqpd 2048+__svml_dexp_ha_data_internal(%rip), %ymm3, %ymm2 #959.28
        vblendvpd %ymm1, .L_2il0floatpacket.34(%rip), %ymm0, %ymm0 #960.22
        vorpd     %ymm2, %ymm1, %ymm4                           #962.28
        vmovmskpd %ymm4, %edx                                   #964.68
        notl      %edx                                          #965.22
        vandnpd   %ymm0, %ymm2, %ymm0                           #961.22
        andl      %eax, %edx                                    #965.38
        je        ..B8.2        # Prob 95%                      #969.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm3
..B8.4:                         # Preds ..B8.3
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm3, 64(%rsp)                               #969.200
        vmovupd   %ymm0, 128(%rsp)                              #969.276
        je        ..B8.2        # Prob 95%                      #969.380
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B8.7:                         # Preds ..B8.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #969.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B8.14:                        # Preds ..B8.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B8.8:                         # Preds ..B8.9 ..B8.14
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #969.523
        jc        ..B8.11       # Prob 5%                       #969.523
                                # LOE rbx r12 r14 r15 r13d
..B8.9:                         # Preds ..B8.11 ..B8.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #969.476
        cmpl      $4, %r12d                                     #969.471
        jl        ..B8.8        # Prob 82%                      #969.471
                                # LOE rbx r12 r14 r15 r13d
..B8.10:                        # Preds ..B8.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #969.682
        jmp       ..B8.2        # Prob 100%                     #969.682
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B8.11:                        # Preds ..B8.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #969.552
        lea       128(%rsp,%r12,8), %rsi                        #969.552
..___tag_value___svml_exp4_ha_l9.121:
#       __svml_dexp_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexp_ha_cout_rare_internal             #969.552
..___tag_value___svml_exp4_ha_l9.122:
        jmp       ..B8.9        # Prob 100%                     #969.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_exp4_ha_l9,@function
	.size	__svml_exp4_ha_l9,.-__svml_exp4_ha_l9
..LN__svml_exp4_ha_l9.7:
	.data
# -- End  __svml_exp4_ha_l9
	.text
.L_2__routine_start___svml_exp2_ha_e9_8:
# -- Begin  __svml_exp2_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_exp2_ha_e9
# --- __svml_exp2_ha_e9(__m128d)
__svml_exp2_ha_e9:
# parameter 1: %xmm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #977.1
        .byte     15                                            #1060.546
        .byte     30                                            #1060.546
        .byte     250                                           #1060.546
	.cfi_startproc
..___tag_value___svml_exp2_ha_e9.124:
..L125:
                                                        #977.1
        pushq     %rbp                                          #977.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #977.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #977.1
        subq      $192, %rsp                                    #977.1
        movaps    %xmm0, %xmm3                                  #977.1
        movups    1024+__svml_dexp_ha_data_internal(%rip), %xmm2 #1007.25
        movaps    %xmm3, %xmm15                                 #1024.13
        mulpd     %xmm3, %xmm2                                  #1007.25
        movups    1088+__svml_dexp_ha_data_internal(%rip), %xmm1 #1000.51
        lea       __svml_dexp_ha_data_internal(%rip), %rcx      #1022.71
        addpd     %xmm1, %xmm2                                  #1007.13
        movaps    %xmm2, %xmm11                                 #1008.13
        subpd     %xmm1, %xmm11                                 #1008.13
        movups    1216+__svml_dexp_ha_data_internal(%rip), %xmm10 #1024.33
        mulpd     %xmm11, %xmm10                                #1024.33
        movups    1280+__svml_dexp_ha_data_internal(%rip), %xmm12 #1025.32
        mulpd     %xmm11, %xmm12                                #1025.32
        subpd     %xmm10, %xmm15                                #1024.13
        subpd     %xmm12, %xmm15                                #1025.13
        movups    1536+__svml_dexp_ha_data_internal(%rip), %xmm13 #1026.25
        movaps    %xmm15, %xmm14                                #1028.14
        mulpd     %xmm15, %xmm13                                #1026.25
        mulpd     %xmm15, %xmm14                                #1028.14
        addpd     1472+__svml_dexp_ha_data_internal(%rip), %xmm13 #1026.13
        mulpd     %xmm15, %xmm13                                #1027.25
        addpd     1408+__svml_dexp_ha_data_internal(%rip), %xmm13 #1027.13
        mulpd     %xmm15, %xmm13                                #1029.25
        addpd     1344+__svml_dexp_ha_data_internal(%rip), %xmm13 #1029.13
        movq      1856+__svml_dexp_ha_data_internal(%rip), %xmm4 #1009.29
        pshufd    $221, %xmm3, %xmm6                            #1012.26
        pand      %xmm4, %xmm6                                  #1013.26
        movdqu    1600+__svml_dexp_ha_data_internal(%rip), %xmm4 #1016.66
        movdqa    %xmm4, %xmm7                                  #1018.27
        pandn     %xmm2, %xmm4                                  #1020.23
        mulpd     %xmm13, %xmm14                                #1030.25
        pand      %xmm2, %xmm7                                  #1018.27
        psllq     $46, %xmm4                                    #1023.23
        pshufd    $136, %xmm7, %xmm8                            #1019.27
        pslld     $4, %xmm8                                     #1021.27
        movd      %xmm8, %eax                                   #1022.116
        movq      1920+__svml_dexp_ha_data_internal(%rip), %xmm5 #1010.33
        pcmpgtd   %xmm5, %xmm6                                  #1014.31
        movslq    %eax, %rax                                    #1022.71
        pextrd    $1, %xmm8, %edx                               #1022.256
        movslq    %edx, %rdx                                    #1022.211
        addpd     %xmm14, %xmm15                                #1030.13
        movmskps  %xmm6, %esi                                   #1015.48
        movups    (%rcx,%rax), %xmm7                            #1022.71
        movups    (%rcx,%rdx), %xmm9                            #1022.211
        movaps    %xmm7, %xmm1                                  #1022.306
        unpcklpd  %xmm9, %xmm1                                  #1022.306
        mulpd     %xmm1, %xmm15                                 #1031.25
        unpckhpd  %xmm9, %xmm7                                  #1022.347
        addpd     %xmm15, %xmm7                                 #1031.13
        addpd     %xmm7, %xmm1                                  #1032.13
        paddq     %xmm4, %xmm1                                  #1034.19
        testl     $3, %esi                                      #1036.37
        jne       ..B9.3        # Prob 5%                       #1036.52
                                # LOE rbx r12 r13 r14 r15 esi xmm1 xmm3
..B9.2:                         # Preds ..B9.4 ..B9.10 ..B9.1 ..B9.3
                                # Execution count [1.00e+00]
        movaps    %xmm1, %xmm0                                  #1063.12
        movq      %rbp, %rsp                                    #1063.12
        popq      %rbp                                          #1063.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1063.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.3:                         # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        movups    1984+__svml_dexp_ha_data_internal(%rip), %xmm0 #1047.64
        movaps    %xmm3, %xmm4                                  #1050.28
        cmpltpd   2048+__svml_dexp_ha_data_internal(%rip), %xmm4 #1050.28
        cmpltpd   %xmm3, %xmm0                                  #1048.28
        blendvpd  %xmm0, .L_2il0floatpacket.33(%rip), %xmm1     #1051.22
        orps      %xmm4, %xmm0                                  #1053.28
        movmskpd  %xmm0, %edx                                   #1055.68
        movaps    %xmm1, %xmm2                                  #1052.22
        movaps    %xmm4, %xmm1                                  #1052.22
        andnps    %xmm2, %xmm1                                  #1052.22
        notl      %edx                                          #1056.22
        andl      %esi, %edx                                    #1056.38
        andl      $3, %edx                                      #1056.46
        je        ..B9.2        # Prob 95%                      #1060.52
                                # LOE rbx r12 r13 r14 r15 edx xmm1 xmm3
..B9.4:                         # Preds ..B9.3
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm3, 64(%rsp)                               #1060.197
        movups    %xmm1, 128(%rsp)                              #1060.270
        je        ..B9.2        # Prob 95%                      #1060.374
                                # LOE rbx r12 r13 r14 r15 edx xmm1
..B9.7:                         # Preds ..B9.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1060.454
        movq      %r12, 8(%rsp)                                 #1060.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1060.454
        movq      %r13, (%rsp)                                  #1060.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1060.454
                                # LOE rbx r12 r14 r15 r13d
..B9.8:                         # Preds ..B9.9 ..B9.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1060.517
        jc        ..B9.11       # Prob 5%                       #1060.517
                                # LOE rbx r12 r14 r15 r13d
..B9.9:                         # Preds ..B9.11 ..B9.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1060.470
        cmpl      $2, %r12d                                     #1060.465
        jl        ..B9.8        # Prob 82%                      #1060.465
                                # LOE rbx r12 r14 r15 r13d
..B9.10:                        # Preds ..B9.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm1                              #1060.673
        jmp       ..B9.2        # Prob 100%                     #1060.673
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm1
..B9.11:                        # Preds ..B9.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1060.546
        lea       128(%rsp,%r12,8), %rsi                        #1060.546
..___tag_value___svml_exp2_ha_e9.142:
#       __svml_dexp_ha_cout_rare_internal(const double *, double *)
        call      __svml_dexp_ha_cout_rare_internal             #1060.546
..___tag_value___svml_exp2_ha_e9.143:
        jmp       ..B9.9        # Prob 100%                     #1060.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_exp2_ha_e9,@function
	.size	__svml_exp2_ha_e9,.-__svml_exp2_ha_e9
..LN__svml_exp2_ha_e9.8:
	.data
# -- End  __svml_exp2_ha_e9
	.text
.L_2__routine_start___svml_dexp_ha_cout_rare_internal_9:
# -- Begin  __svml_dexp_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dexp_ha_cout_rare_internal
	.globl __svml_dexp_ha_cout_rare_internal
# --- __svml_dexp_ha_cout_rare_internal(const double *, double *)
__svml_dexp_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1216.1
        .byte     15                                            #1313.15
        .byte     30                                            #1313.15
        .byte     250                                           #1313.15
	.cfi_startproc
..___tag_value___svml_dexp_ha_cout_rare_internal.145:
..L146:
                                                        #1216.1
        xorl      %eax, %eax                                    #1217.14
        movzwl    6(%rdi), %edx                                 #1224.14
        andl      $32752, %edx                                  #1225.31
        shrl      $4, %edx                                      #1225.31
        movsd     (%rdi), %xmm6                                 #1224.14
        movsd     %xmm6, -8(%rsp)                               #1224.5
        cmpl      $2047, %edx                                   #1225.56
        je        ..B10.17      # Prob 16%                      #1225.56
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax edx xmm6
..B10.2:                        # Preds ..B10.1
                                # Execution count [8.40e-01]
        cmpl      $970, %edx                                    #1227.58
        jle       ..B10.15      # Prob 50%                      #1227.58
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm6
..B10.3:                        # Preds ..B10.2
                                # Execution count [4.20e-01]
        movsd     1080+_imldExpHATab(%rip), %xmm0               #1229.42
        comisd    %xmm6, %xmm0                                  #1229.42
        jb        ..B10.14      # Prob 50%                      #1229.42
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm6
..B10.4:                        # Preds ..B10.3
                                # Execution count [2.10e-01]
        comisd    1096+_imldExpHATab(%rip), %xmm6               #1231.46
        jb        ..B10.13      # Prob 50%                      #1231.46
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm6
..B10.5:                        # Preds ..B10.4
                                # Execution count [1.05e-01]
        movsd     1024+_imldExpHATab(%rip), %xmm0               #1233.49
        movaps    %xmm6, %xmm5                                  #1238.41
        mulsd     %xmm6, %xmm0                                  #1233.49
        lea       _imldExpHATab(%rip), %r10                     #1246.98
        movsd     %xmm0, -24(%rsp)                              #1233.21
        movsd     -24(%rsp), %xmm1                              #1234.35
        movq      1136+_imldExpHATab(%rip), %rdx                #1223.5
        movq      %rdx, -8(%rsp)                                #1223.5
        addsd     1032+_imldExpHATab(%rip), %xmm1               #1234.56
        movsd     %xmm1, -16(%rsp)                              #1234.21
        movsd     -16(%rsp), %xmm2                              #1235.34
        movl      -16(%rsp), %r8d                               #1242.46
        movl      %r8d, %ecx                                    #1244.30
        andl      $63, %r8d                                     #1243.29
        subsd     1032+_imldExpHATab(%rip), %xmm2               #1235.56
        movsd     %xmm2, -24(%rsp)                              #1235.21
        lea       1(%r8,%r8), %r9d                              #1246.74
        movsd     -24(%rsp), %xmm3                              #1236.36
        lea       (%r8,%r8), %edi                               #1246.72
        mulsd     1104+_imldExpHATab(%rip), %xmm3               #1236.57
        movsd     -24(%rsp), %xmm4                              #1237.36
        subsd     %xmm3, %xmm5                                  #1238.41
        mulsd     1112+_imldExpHATab(%rip), %xmm4               #1237.57
        movsd     1072+_imldExpHATab(%rip), %xmm2               #1240.55
        subsd     %xmm4, %xmm5                                  #1239.38
        mulsd     %xmm5, %xmm2                                  #1240.77
        shrl      $6, %ecx                                      #1244.30
        addsd     1064+_imldExpHATab(%rip), %xmm2               #1240.98
        comisd    1088+_imldExpHATab(%rip), %xmm6               #1247.50
        mulsd     %xmm5, %xmm2                                  #1240.121
        movsd     (%r10,%rdi,8), %xmm0                          #1246.98
        lea       1023(%rcx), %edx                              #1245.29
        addsd     1056+_imldExpHATab(%rip), %xmm2               #1240.142
        mulsd     %xmm5, %xmm2                                  #1240.165
        addsd     1048+_imldExpHATab(%rip), %xmm2               #1240.186
        mulsd     %xmm5, %xmm2                                  #1240.209
        addsd     1040+_imldExpHATab(%rip), %xmm2               #1240.230
        mulsd     %xmm5, %xmm2                                  #1240.253
        mulsd     %xmm5, %xmm2                                  #1241.47
        addsd     %xmm5, %xmm2                                  #1241.53
        addsd     (%r10,%r9,8), %xmm2                           #1246.55
        mulsd     %xmm0, %xmm2                                  #1246.98
        jb        ..B10.9       # Prob 50%                      #1247.50
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax edx ecx xmm0 xmm2
..B10.6:                        # Preds ..B10.5
                                # Execution count [5.25e-02]
        andl      $2047, %edx                                   #1250.33
        addsd     %xmm0, %xmm2                                  #1249.59
        cmpl      $2046, %edx                                   #1251.34
        ja        ..B10.8       # Prob 50%                      #1251.34
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax edx xmm2
..B10.7:                        # Preds ..B10.6
                                # Execution count [2.62e-02]
        movq      1136+_imldExpHATab(%rip), %rcx                #1253.50
        shrq      $48, %rcx                                     #1253.50
        shll      $4, %edx                                      #1253.50
        andl      $-32753, %ecx                                 #1253.50
        orl       %edx, %ecx                                    #1253.50
        movw      %cx, -2(%rsp)                                 #1253.50
        movsd     -8(%rsp), %xmm0                               #1254.45
        mulsd     %xmm0, %xmm2                                  #1254.53
        movsd     %xmm2, (%rsi)                                 #1254.31
        ret                                                     #1254.31
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.8:                        # Preds ..B10.6
                                # Execution count [2.62e-02]
        decl      %edx                                          #1258.78
        andl      $2047, %edx                                   #1258.50
        movzwl    -2(%rsp), %ecx                                #1258.50
        shll      $4, %edx                                      #1258.50
        andl      $-32753, %ecx                                 #1258.50
        orl       %edx, %ecx                                    #1258.50
        movw      %cx, -2(%rsp)                                 #1258.50
        movsd     -8(%rsp), %xmm0                               #1259.46
        mulsd     %xmm0, %xmm2                                  #1259.46
        mulsd     1152+_imldExpHATab(%rip), %xmm2               #1260.84
        movsd     %xmm2, (%rsi)                                 #1260.31
        ret                                                     #1260.31
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.9:                        # Preds ..B10.5
                                # Execution count [5.25e-02]
        addl      $1083, %ecx                                   #1265.42
        andl      $2047, %ecx                                   #1266.33
        movl      %ecx, %eax                                    #1267.46
        movzwl    -2(%rsp), %edx                                #1267.46
        shll      $4, %eax                                      #1267.46
        andl      $-32753, %edx                                 #1267.46
        orl       %eax, %edx                                    #1267.46
        movw      %dx, -2(%rsp)                                 #1267.46
        movsd     -8(%rsp), %xmm1                               #1268.30
        mulsd     %xmm1, %xmm2                                  #1268.25
        mulsd     %xmm0, %xmm1                                  #1269.66
        movaps    %xmm1, %xmm0                                  #1272.74
        addsd     %xmm2, %xmm0                                  #1272.74
        cmpl      $50, %ecx                                     #1270.34
        ja        ..B10.11      # Prob 50%                      #1270.34
                                # LOE rbx rbp rsi r12 r13 r14 r15 xmm0 xmm1 xmm2
..B10.10:                       # Preds ..B10.9
                                # Execution count [2.62e-02]
        mulsd     1160+_imldExpHATab(%rip), %xmm0               #1273.94
        movsd     %xmm0, (%rsi)                                 #1273.59
        jmp       ..B10.12      # Prob 100%                     #1273.59
                                # LOE rbx rbp r12 r13 r14 r15
..B10.11:                       # Preds ..B10.9
                                # Execution count [2.62e-02]
        movsd     %xmm0, -72(%rsp)                              #1277.29
        movsd     -72(%rsp), %xmm0                              #1278.62
        subsd     %xmm0, %xmm1                                  #1278.62
        movsd     %xmm1, -64(%rsp)                              #1278.29
        movsd     -64(%rsp), %xmm1                              #1279.29
        addsd     %xmm2, %xmm1                                  #1279.29
        movsd     %xmm1, -64(%rsp)                              #1279.29
        movsd     -72(%rsp), %xmm2                              #1280.112
        mulsd     1168+_imldExpHATab(%rip), %xmm2               #1280.112
        movsd     %xmm2, -56(%rsp)                              #1280.57
        movsd     -72(%rsp), %xmm4                              #1281.45
        movsd     -56(%rsp), %xmm3                              #1281.62
        addsd     %xmm3, %xmm4                                  #1281.62
        movsd     %xmm4, -48(%rsp)                              #1281.29
        movsd     -48(%rsp), %xmm6                              #1282.49
        movsd     -56(%rsp), %xmm5                              #1282.56
        subsd     %xmm5, %xmm6                                  #1282.56
        movsd     %xmm6, -40(%rsp)                              #1282.29
        movsd     -72(%rsp), %xmm8                              #1283.49
        movsd     -40(%rsp), %xmm7                              #1283.66
        subsd     %xmm7, %xmm8                                  #1283.66
        movsd     %xmm8, -32(%rsp)                              #1283.29
        movsd     -64(%rsp), %xmm10                             #1284.41
        movsd     -32(%rsp), %xmm9                              #1284.29
        addsd     %xmm9, %xmm10                                 #1284.29
        movsd     %xmm10, -32(%rsp)                             #1284.29
        movsd     -40(%rsp), %xmm11                             #1285.49
        mulsd     1160+_imldExpHATab(%rip), %xmm11              #1285.77
        movsd     %xmm11, -40(%rsp)                             #1285.29
        movsd     -32(%rsp), %xmm12                             #1286.49
        mulsd     1160+_imldExpHATab(%rip), %xmm12              #1286.77
        movsd     %xmm12, -32(%rsp)                             #1286.29
        movsd     -40(%rsp), %xmm14                             #1287.45
        movsd     -32(%rsp), %xmm13                             #1287.56
        addsd     %xmm13, %xmm14                                #1287.56
        movsd     %xmm14, (%rsi)                                #1287.31
                                # LOE rbx rbp r12 r13 r14 r15
..B10.12:                       # Preds ..B10.10 ..B10.11
                                # Execution count [5.25e-02]
        movl      $4, %eax                                      #1289.25
        ret                                                     #1289.25
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.13:                       # Preds ..B10.4
                                # Execution count [1.05e-01]
        movsd     1120+_imldExpHATab(%rip), %xmm0               #1294.84
        movl      $4, %eax                                      #1295.21
        mulsd     %xmm0, %xmm0                                  #1294.84
        movsd     %xmm0, (%rsi)                                 #1294.23
        ret                                                     #1294.23
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.14:                       # Preds ..B10.3
                                # Execution count [2.10e-01]
        movsd     1128+_imldExpHATab(%rip), %xmm0               #1300.80
        movl      $3, %eax                                      #1301.17
        mulsd     %xmm0, %xmm0                                  #1300.80
        movsd     %xmm0, (%rsi)                                 #1300.19
        ret                                                     #1300.19
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.15:                       # Preds ..B10.2
                                # Execution count [4.20e-01]
        movsd     1144+_imldExpHATab(%rip), %xmm0               #1306.37
        addsd     %xmm6, %xmm0                                  #1306.59
        movsd     %xmm0, (%rsi)                                 #1306.15
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.16:                       # Preds ..B10.15
                                # Execution count [1.00e+00]
        ret                                                     #1320.12
                                # LOE
..B10.17:                       # Preds ..B10.1
                                # Execution count [1.60e-01]: Infreq
        movb      -1(%rsp), %dl                                 #1311.36
        andb      $-128, %dl                                    #1311.36
        cmpb      $-128, %dl                                    #1311.57
        je        ..B10.19      # Prob 16%                      #1311.57
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm6
..B10.18:                       # Preds ..B10.20 ..B10.17 ..B10.19
                                # Execution count [1.54e-01]: Infreq
        mulsd     %xmm6, %xmm6                                  #1317.27
        movsd     %xmm6, (%rsi)                                 #1317.15
        ret                                                     #1317.15
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.19:                       # Preds ..B10.17
                                # Execution count [2.56e-02]: Infreq
        testl     $1048575, -4(%rsp)                            #1311.86
        jne       ..B10.18      # Prob 50%                      #1311.117
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm6
..B10.20:                       # Preds ..B10.19
                                # Execution count [1.28e-02]: Infreq
        cmpl      $0, -8(%rsp)                                  #1311.176
        jne       ..B10.18      # Prob 50%                      #1311.176
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm6
..B10.21:                       # Preds ..B10.20
                                # Execution count [6.40e-03]: Infreq
        movq      1136+_imldExpHATab(%rip), %rdx                #1313.15
        movq      %rdx, (%rsi)                                  #1313.15
        ret                                                     #1313.15
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_dexp_ha_cout_rare_internal,@function
	.size	__svml_dexp_ha_cout_rare_internal,.-__svml_dexp_ha_cout_rare_internal
..LN__svml_dexp_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dexp_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_dexp_ha_data_internal_avx512
	.globl __svml_dexp_ha_data_internal_avx512
__svml_dexp_ha_data_internal_avx512:
	.long	0
	.long	1072693248
	.long	1828292879
	.long	1072739672
	.long	1014845819
	.long	1072788152
	.long	1853186616
	.long	1072838778
	.long	171030293
	.long	1072891646
	.long	1276261410
	.long	1072946854
	.long	3577096743
	.long	1073004506
	.long	3712504873
	.long	1073064711
	.long	1719614413
	.long	1073127582
	.long	1944781191
	.long	1073193236
	.long	1110089947
	.long	1073261797
	.long	2191782032
	.long	1073333393
	.long	2572866477
	.long	1073408159
	.long	3716502172
	.long	1073486235
	.long	3707479175
	.long	1073567768
	.long	2728693978
	.long	1073652911
	.long	0
	.long	0
	.long	1568897901
	.long	1016568486
	.long	3936719688
	.long	3162512149
	.long	3819481236
	.long	1016499965
	.long	1303423926
	.long	1015238005
	.long	2804567149
	.long	1015390024
	.long	3145379760
	.long	1014403278
	.long	3793507337
	.long	1016095713
	.long	3210617384
	.long	3163796463
	.long	3108873501
	.long	3162190556
	.long	3253791412
	.long	1015920431
	.long	730975783
	.long	1014083580
	.long	2462790535
	.long	1015814775
	.long	816778419
	.long	1014197934
	.long	2789017511
	.long	1014276997
	.long	2413007344
	.long	3163551506
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	1697350398
	.long	1073157447
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	16368
	.long	1123549184
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	4277811695
	.long	1072049730
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	993624127
	.long	1014676638
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	4294967295
	.long	3221225471
	.long	915672068
	.long	1062682904
	.long	915672068
	.long	1062682904
	.long	915672068
	.long	1062682904
	.long	915672068
	.long	1062682904
	.long	915672068
	.long	1062682904
	.long	915672068
	.long	1062682904
	.long	915672068
	.long	1062682904
	.long	915672068
	.long	1062682904
	.long	3150079424
	.long	1065422876
	.long	3150079424
	.long	1065422876
	.long	3150079424
	.long	1065422876
	.long	3150079424
	.long	1065422876
	.long	3150079424
	.long	1065422876
	.long	3150079424
	.long	1065422876
	.long	3150079424
	.long	1065422876
	.long	3150079424
	.long	1065422876
	.long	606955774
	.long	1067799895
	.long	606955774
	.long	1067799895
	.long	606955774
	.long	1067799895
	.long	606955774
	.long	1067799895
	.long	606955774
	.long	1067799895
	.long	606955774
	.long	1067799895
	.long	606955774
	.long	1067799895
	.long	606955774
	.long	1067799895
	.long	1402181426
	.long	1069897045
	.long	1402181426
	.long	1069897045
	.long	1402181426
	.long	1069897045
	.long	1402181426
	.long	1069897045
	.long	1402181426
	.long	1069897045
	.long	1402181426
	.long	1069897045
	.long	1402181426
	.long	1069897045
	.long	1402181426
	.long	1069897045
	.long	53256
	.long	1071644672
	.long	53256
	.long	1071644672
	.long	53256
	.long	1071644672
	.long	53256
	.long	1071644672
	.long	53256
	.long	1071644672
	.long	53256
	.long	1071644672
	.long	53256
	.long	1071644672
	.long	53256
	.long	1071644672
	.long	4294967152
	.long	1072693247
	.long	4294967152
	.long	1072693247
	.long	4294967152
	.long	1072693247
	.long	4294967152
	.long	1072693247
	.long	4294967152
	.long	1072693247
	.long	4294967152
	.long	1072693247
	.long	4294967152
	.long	1072693247
	.long	4294967152
	.long	1072693247
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
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	1287323204
	.long	1082531232
	.long	0
	.long	1005584384
	.long	0
	.long	1005584384
	.long	0
	.long	1005584384
	.long	0
	.long	1005584384
	.long	0
	.long	1005584384
	.long	0
	.long	1005584384
	.long	0
	.long	1005584384
	.long	0
	.long	1005584384
	.long	120
	.long	0
	.long	120
	.long	0
	.long	120
	.long	0
	.long	120
	.long	0
	.long	120
	.long	0
	.long	120
	.long	0
	.long	120
	.long	0
	.long	120
	.long	0
	.long	32752
	.long	0
	.long	32752
	.long	0
	.long	32752
	.long	0
	.long	32752
	.long	0
	.long	32752
	.long	0
	.long	32752
	.long	0
	.long	32752
	.long	0
	.long	32752
	.long	0
	.type	__svml_dexp_ha_data_internal_avx512,@object
	.size	__svml_dexp_ha_data_internal_avx512,1280
	.align 64
	.hidden __svml_dexp_ha_data_internal
	.globl __svml_dexp_ha_data_internal
__svml_dexp_ha_data_internal:
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
	.long	1697350398
	.long	1079448903
	.long	1697350398
	.long	1079448903
	.long	1697350398
	.long	1079448903
	.long	1697350398
	.long	1079448903
	.long	1697350398
	.long	1079448903
	.long	1697350398
	.long	1079448903
	.long	1697350398
	.long	1079448903
	.long	1697350398
	.long	1079448903
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
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	0
	.long	1123549184
	.long	4277796864
	.long	1065758274
	.long	4277796864
	.long	1065758274
	.long	4277796864
	.long	1065758274
	.long	4277796864
	.long	1065758274
	.long	4277796864
	.long	1065758274
	.long	4277796864
	.long	1065758274
	.long	4277796864
	.long	1065758274
	.long	4277796864
	.long	1065758274
	.long	3164486458
	.long	1025308570
	.long	3164486458
	.long	1025308570
	.long	3164486458
	.long	1025308570
	.long	3164486458
	.long	1025308570
	.long	3164486458
	.long	1025308570
	.long	3164486458
	.long	1025308570
	.long	3164486458
	.long	1025308570
	.long	3164486458
	.long	1025308570
	.long	4294957883
	.long	1071644671
	.long	4294957883
	.long	1071644671
	.long	4294957883
	.long	1071644671
	.long	4294957883
	.long	1071644671
	.long	4294957883
	.long	1071644671
	.long	4294957883
	.long	1071644671
	.long	4294957883
	.long	1071644671
	.long	4294957883
	.long	1071644671
	.long	1431659838
	.long	1069897045
	.long	1431659838
	.long	1069897045
	.long	1431659838
	.long	1069897045
	.long	1431659838
	.long	1069897045
	.long	1431659838
	.long	1069897045
	.long	1431659838
	.long	1069897045
	.long	1431659838
	.long	1069897045
	.long	1431659838
	.long	1069897045
	.long	1059163027
	.long	1067799895
	.long	1059163027
	.long	1067799895
	.long	1059163027
	.long	1067799895
	.long	1059163027
	.long	1067799895
	.long	1059163027
	.long	1067799895
	.long	1059163027
	.long	1067799895
	.long	1059163027
	.long	1067799895
	.long	1059163027
	.long	1067799895
	.long	765416603
	.long	1065423121
	.long	765416603
	.long	1065423121
	.long	765416603
	.long	1065423121
	.long	765416603
	.long	1065423121
	.long	765416603
	.long	1065423121
	.long	765416603
	.long	1065423121
	.long	765416603
	.long	1065423121
	.long	765416603
	.long	1065423121
	.long	63
	.long	0
	.long	63
	.long	0
	.long	63
	.long	0
	.long	63
	.long	0
	.long	63
	.long	0
	.long	63
	.long	0
	.long	63
	.long	0
	.long	63
	.long	0
	.long	1023
	.long	0
	.long	1023
	.long	0
	.long	1023
	.long	0
	.long	1023
	.long	0
	.long	1023
	.long	0
	.long	1023
	.long	0
	.long	1023
	.long	0
	.long	1023
	.long	0
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
	.long	4294967295
	.long	1082532650
	.long	4294967295
	.long	1082532650
	.long	4294967295
	.long	1082532650
	.long	4294967295
	.long	1082532650
	.long	4294967295
	.long	1082532650
	.long	4294967295
	.long	1082532650
	.long	4294967295
	.long	1082532650
	.long	4294967295
	.long	1082532650
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
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	1082532650
	.long	4277811695
	.long	1082535490
	.long	4277811695
	.long	1082535490
	.long	4277811695
	.long	1082535490
	.long	4277811695
	.long	1082535490
	.long	4277811695
	.long	1082535490
	.long	4277811695
	.long	1082535490
	.long	4277811695
	.long	1082535490
	.long	4277811695
	.long	1082535490
	.long	3576508497
	.long	3230091536
	.long	3576508497
	.long	3230091536
	.long	3576508497
	.long	3230091536
	.long	3576508497
	.long	3230091536
	.long	3576508497
	.long	3230091536
	.long	3576508497
	.long	3230091536
	.long	3576508497
	.long	3230091536
	.long	3576508497
	.long	3230091536
	.type	__svml_dexp_ha_data_internal,@object
	.size	__svml_dexp_ha_data_internal,2112
	.align 32
.L_2il0floatpacket.34:
	.long	0x00000000,0x7ff00000,0x00000000,0x7ff00000,0x00000000,0x7ff00000,0x00000000,0x7ff00000
	.type	.L_2il0floatpacket.34,@object
	.size	.L_2il0floatpacket.34,32
	.align 32
_imldExpHATab:
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
	.long	4277811695
	.long	1082535490
	.long	3715808466
	.long	3230016299
	.long	3576508497
	.long	3230091536
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
	.type	_imldExpHATab,@object
	.size	_imldExpHATab,1176
	.space 8, 0x00 	# pad
	.align 16
.L_2il0floatpacket.33:
	.long	0x00000000,0x7ff00000,0x00000000,0x7ff00000
	.type	.L_2il0floatpacket.33,@object
	.size	.L_2il0floatpacket.33,16
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
