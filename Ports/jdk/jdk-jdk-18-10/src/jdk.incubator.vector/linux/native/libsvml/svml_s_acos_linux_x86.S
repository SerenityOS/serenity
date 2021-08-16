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
	.file "svml_s_acos.c"
	.text
..TXTST0:
.L_2__routine_start___svml_acosf16_ha_z0_0:
# -- Begin  __svml_acosf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acosf16_ha_z0
# --- __svml_acosf16_ha_z0(__m512)
__svml_acosf16_ha_z0:
# parameter 1: %zmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #139.1
        .byte     15                                            #224.12
        .byte     30                                            #224.12
        .byte     250                                           #224.12
	.cfi_startproc
..___tag_value___svml_acosf16_ha_z0.1:
..L2:
                                                          #139.1
        vmovups   __svml_sacos_ha_data_internal(%rip), %zmm7    #175.45
        vmovups   64+__svml_sacos_ha_data_internal(%rip), %zmm8 #176.46
        vmovups   192+__svml_sacos_ha_data_internal(%rip), %zmm12 #188.786
        vmovups   256+__svml_sacos_ha_data_internal(%rip), %zmm10 #177.43
        vmovups   448+__svml_sacos_ha_data_internal(%rip), %zmm1 #188.1229
        vmovaps   %zmm0, %zmm6                                  #139.1
        vorps     %zmm6, %zmm7, %zmm5                           #178.8
        vandps    %zmm6, %zmm7, %zmm4                           #179.12
        vfmadd231ps {rn-sae}, %zmm5, %zmm8, %zmm8               #181.8
        vmulps    {rn-sae}, %zmm5, %zmm5, %zmm9                 #182.11
        vrsqrt14ps %zmm8, %zmm11                                #188.580
        vcmpps    $17, {sae}, %zmm12, %zmm8, %k2                #188.847
        vcmpps    $22, {sae}, %zmm5, %zmm10, %k1                #180.13
        vminps    {sae}, %zmm8, %zmm9, %zmm3                    #183.11
        vaddps    {rn-sae}, %zmm8, %zmm8, %zmm13                #188.1005
        vandps    384+__svml_sacos_ha_data_internal(%rip), %zmm11, %zmm0 #188.703
        vcmpps    $21, {sae}, %zmm8, %zmm3, %k4                 #184.14
        vmovups   512+__svml_sacos_ha_data_internal(%rip), %zmm8 #188.2147
        vandps    320+__svml_sacos_ha_data_internal(%rip), %zmm13, %zmm14 #188.1327
        vxorps    %zmm0, %zmm0, %zmm0{%k2}                      #188.933
        vorps     1344+__svml_sacos_ha_data_internal(%rip), %zmm3, %zmm3{%k1} #187.11
        vsubps    {rn-sae}, %zmm14, %zmm13, %zmm15              #188.1378
        vmulps    {rn-sae}, %zmm0, %zmm14, %zmm2                #188.1480
        vmulps    {rn-sae}, %zmm0, %zmm15, %zmm12               #188.1582
        vmulps    {rn-sae}, %zmm3, %zmm3, %zmm15                #195.15
        vcmpps    $21, {sae}, %zmm6, %zmm3, %k0                 #200.17
        vfmsub231ps {rn-sae}, %zmm2, %zmm0, %zmm1               #188.1685
        vmovups   640+__svml_sacos_ha_data_internal(%rip), %zmm13 #189.56
        vmovups   768+__svml_sacos_ha_data_internal(%rip), %zmm14 #191.56
        vmovups   960+__svml_sacos_ha_data_internal(%rip), %zmm6 #203.47
        vmulps    {rn-sae}, %zmm0, %zmm12, %zmm7                #188.1797
        vmovups   832+__svml_sacos_ha_data_internal(%rip), %zmm0 #194.14
        vaddps    {rn-sae}, %zmm12, %zmm2, %zmm9                #188.1900
        vaddps    {rn-sae}, %zmm7, %zmm1, %zmm10                #188.2004
        vxorps    %zmm6, %zmm6, %zmm6{%k4}                      #208.15
        vmovups   704+__svml_sacos_ha_data_internal(%rip), %zmm7 #190.56
        vfmadd231ps {rn-sae}, %zmm3, %zmm14, %zmm0              #194.14
        vmovups   576+__svml_sacos_ha_data_internal(%rip), %zmm1 #188.2302
        vmulps    {rn-sae}, %zmm10, %zmm9, %zmm11               #188.2420
        vfmadd231ps {rn-sae}, %zmm3, %zmm13, %zmm7              #193.15
        vmovups   896+__svml_sacos_ha_data_internal(%rip), %zmm13 #196.56
        vfmadd231ps {rn-sae}, %zmm10, %zmm8, %zmm1              #188.2302
        vfmadd213ps {rn-sae}, %zmm0, %zmm15, %zmm7              #197.15
        vfmsub213ps {rn-sae}, %zmm12, %zmm11, %zmm1             #188.2523
        vfmadd213ps {rn-sae}, %zmm13, %zmm3, %zmm7              #198.15
        kmovw     %k4, %eax                                     #184.14
        kmovw     %k0, %edx                                     #200.17
        vmulps    {rn-sae}, %zmm3, %zmm7, %zmm0                 #199.17
        vmovups   1024+__svml_sacos_ha_data_internal(%rip), %zmm3 #202.47
        vmovups   1216+__svml_sacos_ha_data_internal(%rip), %zmm7 #206.47
        vxorps    %zmm3, %zmm3, %zmm3{%k4}                      #207.15
        vblendmps %zmm1, %zmm7, %zmm1{%k4}                      #212.13
        andl      %eax, %edx                                    #201.29
        kmovw     %edx, %k3                                     #209.15
        vorps     1152+__svml_sacos_ha_data_internal(%rip), %zmm3, %zmm3{%k3} #209.15
        vorps     1088+__svml_sacos_ha_data_internal(%rip), %zmm6, %zmm6{%k3} #210.15
        vxorps    %zmm4, %zmm3, %zmm8                           #211.15
        vblendmps %zmm2, %zmm5, %zmm3{%k4}                      #213.13
        vsubps    {rn-sae}, %zmm1, %zmm8, %zmm5                 #215.17
        vsubps    {rn-sae}, %zmm1, %zmm3, %zmm2                 #214.15
        vfmadd213ps {rn-sae}, %zmm5, %zmm2, %zmm0               #216.14
        vaddps    {rn-sae}, %zmm3, %zmm0, %zmm0                 #217.16
        vxorps    %zmm4, %zmm0, %zmm4                           #218.14
        vaddps    {rn-sae}, %zmm6, %zmm4, %zmm0                 #219.16
        ret                                                     #224.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_acosf16_ha_z0,@function
	.size	__svml_acosf16_ha_z0,.-__svml_acosf16_ha_z0
..LN__svml_acosf16_ha_z0.0:
	.data
# -- End  __svml_acosf16_ha_z0
	.text
.L_2__routine_start___svml_acosf4_ha_ex_1:
# -- Begin  __svml_acosf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acosf4_ha_ex
# --- __svml_acosf4_ha_ex(__m128)
__svml_acosf4_ha_ex:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #229.1
        .byte     15                                            #312.540
        .byte     30                                            #312.540
        .byte     250                                           #312.540
	.cfi_startproc
..___tag_value___svml_acosf4_ha_ex.4:
..L5:
                                                          #229.1
        pushq     %rbp                                          #229.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #229.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #229.1
        subq      $192, %rsp                                    #229.1
        movaps    %xmm0, %xmm7                                  #229.1
        movups    __svml_sacos_ha_data_internal(%rip), %xmm5    #264.43
        movups    64+__svml_sacos_ha_data_internal(%rip), %xmm4 #265.44
        movaps    %xmm5, %xmm6                                  #267.8
        orps      %xmm7, %xmm6                                  #267.8
        movaps    %xmm4, %xmm8                                  #270.20
        mulps     %xmm6, %xmm8                                  #270.20
        movaps    %xmm6, %xmm0                                  #271.9
        mulps     %xmm6, %xmm0                                  #271.9
        addps     %xmm8, %xmm4                                  #270.8
        minps     %xmm4, %xmm0                                  #272.9
        rsqrtps   %xmm4, %xmm10                                 #276.494
        movaps    %xmm4, %xmm15                                 #276.620
        movaps    %xmm0, %xmm3                                  #275.14
        cmpltps   192+__svml_sacos_ha_data_internal(%rip), %xmm15 #276.620
        cmpnltps  %xmm4, %xmm3                                  #275.14
        addps     %xmm4, %xmm4                                  #276.713
        movups    320+__svml_sacos_ha_data_internal(%rip), %xmm11 #276.786
        andnps    %xmm10, %xmm15                                #276.663
        movaps    %xmm11, %xmm2                                 #276.1058
        movaps    %xmm15, %xmm13                                #276.1350
        andps     %xmm4, %xmm2                                  #276.1058
        andps     %xmm7, %xmm5                                  #268.12
        movups    512+__svml_sacos_ha_data_internal(%rip), %xmm1 #276.1805
        subps     %xmm2, %xmm4                                  #276.1104
        mulps     %xmm15, %xmm2                                 #276.1142
        mulps     %xmm15, %xmm4                                 #276.1180
        andps     %xmm2, %xmm11                                 #276.1220
        movaps    %xmm2, %xmm12                                 #276.1267
        mulps     %xmm4, %xmm13                                 #276.1350
        subps     %xmm11, %xmm12                                #276.1267
        mulps     %xmm15, %xmm11                                #276.1308
        mulps     %xmm12, %xmm15                                #276.1390
        subps     448+__svml_sacos_ha_data_internal(%rip), %xmm11 #276.1433
        movaps    %xmm2, %xmm14                                 #276.1566
        andps     %xmm3, %xmm2                                  #277.13
        movups    640+__svml_sacos_ha_data_internal(%rip), %xmm8 #283.27
        movaps    %xmm3, %xmm12                                 #298.15
        mulps     %xmm0, %xmm8                                  #283.27
        addps     %xmm11, %xmm13                                #276.1478
        addps     %xmm4, %xmm14                                 #276.1566
        addps     %xmm13, %xmm15                                #276.1523
        addps     704+__svml_sacos_ha_data_internal(%rip), %xmm8 #283.15
        mulps     %xmm15, %xmm1                                 #276.1805
        mulps     %xmm14, %xmm15                                #276.1863
        addps     576+__svml_sacos_ha_data_internal(%rip), %xmm1 #276.1793
        mulps     %xmm15, %xmm1                                 #276.1916
        movups    768+__svml_sacos_ha_data_internal(%rip), %xmm11 #284.26
        subps     %xmm4, %xmm1                                  #276.1904
        mulps     %xmm0, %xmm11                                 #284.26
        movaps    %xmm0, %xmm4                                  #285.13
        andps     %xmm3, %xmm1                                  #278.13
        mulps     %xmm0, %xmm4                                  #285.13
        addps     832+__svml_sacos_ha_data_internal(%rip), %xmm11 #284.14
        mulps     %xmm4, %xmm8                                  #287.27
        movups    256+__svml_sacos_ha_data_internal(%rip), %xmm9 #266.41
        addps     %xmm8, %xmm11                                 #287.15
        cmpnleps  %xmm6, %xmm9                                  #269.13
        mulps     %xmm0, %xmm11                                 #288.27
        movmskps  %xmm9, %edx                                   #274.40
        movups    1152+__svml_sacos_ha_data_internal(%rip), %xmm9 #294.14
        movups    1088+__svml_sacos_ha_data_internal(%rip), %xmm10 #295.14
        andnps    1024+__svml_sacos_ha_data_internal(%rip), %xmm12 #298.15
        addps     896+__svml_sacos_ha_data_internal(%rip), %xmm11 #288.15
        mulps     %xmm0, %xmm11                                 #289.15
        cmpnltps  %xmm7, %xmm0                                  #290.17
        andps     %xmm3, %xmm0                                  #293.17
        andps     %xmm0, %xmm9                                  #294.14
        andps     %xmm0, %xmm10                                 #295.14
        movaps    %xmm3, %xmm0                                  #299.15
        andnps    %xmm6, %xmm3                                  #300.12
        orps      %xmm3, %xmm2                                  #304.13
        orps      %xmm9, %xmm12                                 #301.15
        movaps    %xmm2, %xmm3                                  #305.13
        pxor      %xmm5, %xmm12                                 #303.15
        andnps    960+__svml_sacos_ha_data_internal(%rip), %xmm0 #299.15
        subps     %xmm1, %xmm3                                  #305.13
        subps     %xmm1, %xmm12                                 #306.14
        mulps     %xmm3, %xmm11                                 #307.26
        orps      %xmm10, %xmm0                                 #302.15
        addps     %xmm11, %xmm12                                #307.14
        addps     %xmm12, %xmm2                                 #308.14
        pxor      %xmm5, %xmm2                                  #309.14
        addps     %xmm2, %xmm0                                  #310.14
        testl     %edx, %edx                                    #312.52
        jne       ..B2.3        # Prob 5%                       #312.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
..B2.2:                         # Preds ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #315.12
        popq      %rbp                                          #315.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #315.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm7, 64(%rsp)                               #312.193
        movups    %xmm0, 128(%rsp)                              #312.264
                                # LOE rbx r12 r13 r14 r15 edx
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #312.448
        movq      %r12, 8(%rsp)                                 #312.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #312.448
        movq      %r13, (%rsp)                                  #312.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #312.448
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #312.511
        jc        ..B2.10       # Prob 5%                       #312.511
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #312.464
        cmpl      $4, %r12d                                     #312.459
        jl        ..B2.7        # Prob 82%                      #312.459
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #312.666
        jmp       ..B2.2        # Prob 100%                     #312.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #312.540
        lea       128(%rsp,%r12,4), %rsi                        #312.540
..___tag_value___svml_acosf4_ha_ex.22:
#       __svml_sacos_ha_cout_rare_internal(const float *, float *)
        call      __svml_sacos_ha_cout_rare_internal            #312.540
..___tag_value___svml_acosf4_ha_ex.23:
        jmp       ..B2.8        # Prob 100%                     #312.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_acosf4_ha_ex,@function
	.size	__svml_acosf4_ha_ex,.-__svml_acosf4_ha_ex
..LN__svml_acosf4_ha_ex.1:
	.data
# -- End  __svml_acosf4_ha_ex
	.text
.L_2__routine_start___svml_acosf8_ha_e9_2:
# -- Begin  __svml_acosf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acosf8_ha_e9
# --- __svml_acosf8_ha_e9(__m256)
__svml_acosf8_ha_e9:
# parameter 1: %ymm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #320.1
        .byte     15                                            #403.546
        .byte     30                                            #403.546
        .byte     250                                           #403.546
	.cfi_startproc
..___tag_value___svml_acosf8_ha_e9.25:
..L26:
                                                         #320.1
        pushq     %rbp                                          #320.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #320.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #320.1
        subq      $192, %rsp                                    #320.1
        vmovups   __svml_sacos_ha_data_internal(%rip), %ymm11   #355.46
        vpxor     %xmm2, %xmm2, %xmm2                           #365.58
        vmovups   64+__svml_sacos_ha_data_internal(%rip), %ymm12 #356.47
        vmovaps   %ymm0, %ymm10                                 #320.1
        vorps     %ymm10, %ymm11, %ymm9                         #358.8
        vmulps    %ymm12, %ymm9, %ymm13                         #361.23
        vmulps    %ymm9, %ymm9, %ymm14                          #362.9
        vcmpnge_uqps 256+__svml_sacos_ha_data_internal(%rip), %ymm9, %ymm15 #360.13
        vandps    %ymm10, %ymm11, %ymm8                         #359.12
        vaddps    %ymm13, %ymm12, %ymm11                        #361.8
        vrsqrtps  %ymm11, %ymm3                                 #367.494
        vcmplt_oqps 192+__svml_sacos_ha_data_internal(%rip), %ymm11, %ymm4 #367.626
        vaddps    %ymm11, %ymm11, %ymm12                        #367.735
        vminps    %ymm11, %ymm14, %ymm7                         #363.9
        vandnps   %ymm3, %ymm4, %ymm5                           #367.682
        vextractf128 $1, %ymm15, %xmm0                          #364.114
        vpackssdw %xmm0, %xmm15, %xmm1                          #365.75
        vmovups   320+__svml_sacos_ha_data_internal(%rip), %ymm0 #367.814
        vpacksswb %xmm2, %xmm1, %xmm6                           #365.58
        vmulps    768+__svml_sacos_ha_data_internal(%rip), %ymm7, %ymm15 #375.29
        vpmovmskb %xmm6, %edx                                   #365.39
        vcmpnlt_uqps %ymm11, %ymm7, %ymm6                       #366.14
        vandps    %ymm0, %ymm12, %ymm13                         #367.1089
        vmulps    %ymm13, %ymm5, %ymm4                          #367.1179
        vsubps    %ymm13, %ymm12, %ymm14                        #367.1138
        vmulps    %ymm14, %ymm5, %ymm3                          #367.1220
        vandps    %ymm0, %ymm4, %ymm1                           #367.1263
        vmulps    %ymm1, %ymm5, %ymm11                          #367.1357
        vmulps    %ymm3, %ymm5, %ymm12                          #367.1402
        vsubps    %ymm1, %ymm4, %ymm2                           #367.1313
        vmulps    %ymm2, %ymm5, %ymm0                           #367.1445
        vsubps    448+__svml_sacos_ha_data_internal(%rip), %ymm11, %ymm5 #367.1491
        vaddps    %ymm3, %ymm4, %ymm11                          #367.1633
        vaddps    %ymm5, %ymm12, %ymm1                          #367.1539
        vaddps    %ymm1, %ymm0, %ymm5                           #367.1587
        vmulps    512+__svml_sacos_ha_data_internal(%rip), %ymm5, %ymm2 #367.1884
        vmulps    %ymm11, %ymm5, %ymm13                         #367.1945
        vaddps    576+__svml_sacos_ha_data_internal(%rip), %ymm2, %ymm12 #367.1869
        vmulps    %ymm13, %ymm12, %ymm14                        #367.2004
        vsubps    %ymm3, %ymm14, %ymm3                          #367.1989
        vandps    %ymm6, %ymm4, %ymm2                           #368.13
        vmulps    640+__svml_sacos_ha_data_internal(%rip), %ymm7, %ymm4 #374.30
        vandps    %ymm6, %ymm3, %ymm1                           #369.13
        vmulps    %ymm7, %ymm7, %ymm3                           #376.13
        vaddps    704+__svml_sacos_ha_data_internal(%rip), %ymm4, %ymm0 #374.15
        vaddps    832+__svml_sacos_ha_data_internal(%rip), %ymm15, %ymm4 #375.14
        vmulps    %ymm3, %ymm0, %ymm5                           #378.30
        vaddps    %ymm5, %ymm4, %ymm11                          #378.15
        vmulps    %ymm11, %ymm7, %ymm12                         #379.30
        vaddps    896+__svml_sacos_ha_data_internal(%rip), %ymm12, %ymm13 #379.15
        vmulps    %ymm13, %ymm7, %ymm0                          #380.15
        vcmpnlt_uqps %ymm10, %ymm7, %ymm7                       #381.17
        vandps    %ymm6, %ymm7, %ymm3                           #384.17
        vandps    1152+__svml_sacos_ha_data_internal(%rip), %ymm3, %ymm5 #385.14
        vandps    1088+__svml_sacos_ha_data_internal(%rip), %ymm3, %ymm11 #386.14
        vandnps   1024+__svml_sacos_ha_data_internal(%rip), %ymm6, %ymm4 #389.15
        vandnps   %ymm9, %ymm6, %ymm3                           #391.12
        vorps     %ymm5, %ymm4, %ymm9                           #392.15
        vorps     %ymm3, %ymm2, %ymm4                           #395.13
        vsubps    %ymm1, %ymm4, %ymm2                           #396.13
        vmulps    %ymm2, %ymm0, %ymm0                           #398.29
        vxorps    %ymm8, %ymm9, %ymm9                           #394.15
        vsubps    %ymm1, %ymm9, %ymm1                           #397.14
        vaddps    %ymm0, %ymm1, %ymm5                           #398.14
        vandnps   960+__svml_sacos_ha_data_internal(%rip), %ymm6, %ymm7 #390.15
        vaddps    %ymm5, %ymm4, %ymm6                           #399.14
        vorps     %ymm11, %ymm7, %ymm7                          #393.15
        vxorps    %ymm8, %ymm6, %ymm8                           #400.14
        vaddps    %ymm8, %ymm7, %ymm0                           #401.14
        testb     %dl, %dl                                      #403.52
        jne       ..B3.3        # Prob 5%                       #403.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm10
..B3.2:                         # Preds ..B3.3 ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #406.12
        popq      %rbp                                          #406.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #406.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm10, 64(%rsp)                              #403.196
        vmovups   %ymm0, 128(%rsp)                              #403.270
        testl     %edx, %edx                                    #403.374
        je        ..B3.2        # Prob 95%                      #403.374
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #403.454
                                # LOE rbx r12 r13 r14 r15 eax edx
..B3.13:                        # Preds ..B3.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B3.7:                         # Preds ..B3.8 ..B3.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #403.517
        jc        ..B3.10       # Prob 5%                       #403.517
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #403.470
        cmpl      $8, %r12d                                     #403.465
        jl        ..B3.7        # Prob 82%                      #403.465
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #403.675
        jmp       ..B3.2        # Prob 100%                     #403.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #403.546
        lea       128(%rsp,%r12,4), %rsi                        #403.546
..___tag_value___svml_acosf8_ha_e9.43:
#       __svml_sacos_ha_cout_rare_internal(const float *, float *)
        call      __svml_sacos_ha_cout_rare_internal            #403.546
..___tag_value___svml_acosf8_ha_e9.44:
        jmp       ..B3.8        # Prob 100%                     #403.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_acosf8_ha_e9,@function
	.size	__svml_acosf8_ha_e9,.-__svml_acosf8_ha_e9
..LN__svml_acosf8_ha_e9.2:
	.data
# -- End  __svml_acosf8_ha_e9
	.text
.L_2__routine_start___svml_acosf8_ha_l9_3:
# -- Begin  __svml_acosf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acosf8_ha_l9
# --- __svml_acosf8_ha_l9(__m256)
__svml_acosf8_ha_l9:
# parameter 1: %ymm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #411.1
        .byte     15                                            #494.546
        .byte     30                                            #494.546
        .byte     250                                           #494.546
	.cfi_startproc
..___tag_value___svml_acosf8_ha_l9.46:
..L47:
                                                         #411.1
        pushq     %rbp                                          #411.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #411.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #411.1
        subq      $192, %rsp                                    #411.1
        vmovups   __svml_sacos_ha_data_internal(%rip), %ymm8    #446.46
        vmovups   64+__svml_sacos_ha_data_internal(%rip), %ymm9 #447.47
        vmovups   448+__svml_sacos_ha_data_internal(%rip), %ymm2 #458.902
        vmovaps   %ymm0, %ymm7                                  #411.1
        vorps     %ymm7, %ymm8, %ymm6                           #449.8
        vfmadd231ps %ymm6, %ymm9, %ymm9                         #452.8
        vmulps    %ymm6, %ymm6, %ymm10                          #453.9
        vcmpnge_uqps 256+__svml_sacos_ha_data_internal(%rip), %ymm6, %ymm11 #451.13
        vaddps    %ymm9, %ymm9, %ymm14                          #458.735
        vcmplt_oqps 192+__svml_sacos_ha_data_internal(%rip), %ymm9, %ymm12 #458.626
        vminps    %ymm9, %ymm10, %ymm4                          #454.9
        vrsqrtps  %ymm9, %ymm13                                 #458.494
        vcmpnlt_uqps %ymm9, %ymm4, %ymm3                        #457.14
        vandps    320+__svml_sacos_ha_data_internal(%rip), %ymm14, %ymm15 #458.997
        vsubps    %ymm15, %ymm14, %ymm0                         #458.1046
        vmovups   512+__svml_sacos_ha_data_internal(%rip), %ymm14 #458.1397
        vandnps   %ymm13, %ymm12, %ymm1                         #458.682
        vmulps    %ymm15, %ymm1, %ymm13                         #458.1087
        vmulps    %ymm0, %ymm1, %ymm12                          #458.1128
        vmovups   768+__svml_sacos_ha_data_internal(%rip), %ymm0 #463.57
        vmulps    %ymm4, %ymm4, %ymm15                          #467.13
        vfmsub231ps %ymm13, %ymm1, %ymm2                        #458.1172
        vfmadd213ps 832+__svml_sacos_ha_data_internal(%rip), %ymm4, %ymm0 #466.14
        vaddps    %ymm12, %ymm13, %ymm9                         #458.1269
        vandps    %ymm7, %ymm8, %ymm5                           #450.12
        vmulps    %ymm12, %ymm1, %ymm8                          #458.1227
        vaddps    %ymm8, %ymm2, %ymm10                          #458.1312
        vmovups   640+__svml_sacos_ha_data_internal(%rip), %ymm8 #461.57
        vfmadd213ps 576+__svml_sacos_ha_data_internal(%rip), %ymm10, %ymm14 #458.1553
        vfmadd213ps 704+__svml_sacos_ha_data_internal(%rip), %ymm4, %ymm8 #465.15
        vfmadd213ps %ymm0, %ymm15, %ymm8                        #469.15
        vfmadd213ps 896+__svml_sacos_ha_data_internal(%rip), %ymm4, %ymm8 #470.15
        vmovmskps %ymm11, %edx                                  #456.41
        vmulps    %ymm10, %ymm9, %ymm11                         #458.1614
        vmulps    %ymm8, %ymm4, %ymm0                           #471.15
        vcmpnlt_uqps %ymm7, %ymm4, %ymm4                        #472.17
        vfmsub213ps %ymm12, %ymm11, %ymm14                      #458.1658
        vandps    %ymm3, %ymm4, %ymm4                           #475.17
        vandps    1152+__svml_sacos_ha_data_internal(%rip), %ymm4, %ymm9 #476.14
        vandnps   1024+__svml_sacos_ha_data_internal(%rip), %ymm3, %ymm8 #480.15
        vandnps   960+__svml_sacos_ha_data_internal(%rip), %ymm3, %ymm10 #481.15
        vandps    %ymm3, %ymm13, %ymm2                          #459.13
        vandps    %ymm3, %ymm14, %ymm1                          #460.13
        vandnps   %ymm6, %ymm3, %ymm6                           #482.12
        vorps     %ymm9, %ymm8, %ymm3                           #483.15
        vxorps    %ymm5, %ymm3, %ymm3                           #485.15
        vorps     %ymm6, %ymm2, %ymm6                           #486.13
        vsubps    %ymm1, %ymm6, %ymm2                           #487.13
        vsubps    %ymm1, %ymm3, %ymm1                           #488.14
        vfmadd213ps %ymm1, %ymm2, %ymm0                         #489.14
        vaddps    %ymm0, %ymm6, %ymm0                           #490.14
        vandps    1088+__svml_sacos_ha_data_internal(%rip), %ymm4, %ymm11 #477.14
        vorps     %ymm11, %ymm10, %ymm4                         #484.15
        vxorps    %ymm5, %ymm0, %ymm5                           #491.14
        vaddps    %ymm5, %ymm4, %ymm0                           #492.14
        testl     %edx, %edx                                    #494.52
        jne       ..B4.3        # Prob 5%                       #494.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm7
..B4.2:                         # Preds ..B4.3 ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #497.12
        popq      %rbp                                          #497.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #497.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm7, 64(%rsp)                               #494.196
        vmovups   %ymm0, 128(%rsp)                              #494.270
        je        ..B4.2        # Prob 95%                      #494.374
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #494.454
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
        btl       %r12d, %r13d                                  #494.517
        jc        ..B4.10       # Prob 5%                       #494.517
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #494.470
        cmpl      $8, %r12d                                     #494.465
        jl        ..B4.7        # Prob 82%                      #494.465
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #494.675
        jmp       ..B4.2        # Prob 100%                     #494.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #494.546
        lea       128(%rsp,%r12,4), %rsi                        #494.546
..___tag_value___svml_acosf8_ha_l9.64:
#       __svml_sacos_ha_cout_rare_internal(const float *, float *)
        call      __svml_sacos_ha_cout_rare_internal            #494.546
..___tag_value___svml_acosf8_ha_l9.65:
        jmp       ..B4.8        # Prob 100%                     #494.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_acosf8_ha_l9,@function
	.size	__svml_acosf8_ha_l9,.-__svml_acosf8_ha_l9
..LN__svml_acosf8_ha_l9.3:
	.data
# -- End  __svml_acosf8_ha_l9
	.text
.L_2__routine_start___svml_acosf4_ha_l9_4:
# -- Begin  __svml_acosf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acosf4_ha_l9
# --- __svml_acosf4_ha_l9(__m128)
__svml_acosf4_ha_l9:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #502.1
        .byte     15                                            #585.540
        .byte     30                                            #585.540
        .byte     250                                           #585.540
	.cfi_startproc
..___tag_value___svml_acosf4_ha_l9.67:
..L68:
                                                         #502.1
        pushq     %rbp                                          #502.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #502.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #502.1
        subq      $192, %rsp                                    #502.1
        vmovaps   %xmm0, %xmm7                                  #502.1
        vmovups   __svml_sacos_ha_data_internal(%rip), %xmm8    #537.43
        vmovups   64+__svml_sacos_ha_data_internal(%rip), %xmm10 #538.44
        vorps     %xmm7, %xmm8, %xmm6                           #540.8
        vfmadd231ps %xmm6, %xmm10, %xmm10                       #543.8
        vandps    %xmm7, %xmm8, %xmm5                           #541.12
        vmulps    %xmm6, %xmm6, %xmm11                          #544.9
        vrsqrtps  %xmm10, %xmm14                                #549.494
        vcmpltps  192+__svml_sacos_ha_data_internal(%rip), %xmm10, %xmm13 #549.620
        vminps    %xmm10, %xmm11, %xmm4                         #545.9
        vaddps    %xmm10, %xmm10, %xmm15                        #549.713
        vcmpnltps %xmm10, %xmm4, %xmm3                          #548.14
        vandps    320+__svml_sacos_ha_data_internal(%rip), %xmm15, %xmm0 #549.966
        vandnps   %xmm14, %xmm13, %xmm2                         #549.663
        vmulps    %xmm0, %xmm2, %xmm14                          #549.1050
        vsubps    %xmm0, %xmm15, %xmm1                          #549.1012
        vmulps    %xmm4, %xmm4, %xmm15                          #558.13
        vmulps    %xmm1, %xmm2, %xmm13                          #549.1088
        vmovups   256+__svml_sacos_ha_data_internal(%rip), %xmm9 #539.41
        vaddps    %xmm13, %xmm14, %xmm10                        #549.1220
        vcmpnleps %xmm6, %xmm9, %xmm12                          #542.13
        vmulps    %xmm13, %xmm2, %xmm9                          #549.1181
        vmovmskps %xmm12, %edx                                  #547.40
        vmovups   448+__svml_sacos_ha_data_internal(%rip), %xmm8 #549.871
        vandnps   %xmm6, %xmm3, %xmm6                           #573.12
        vmovups   512+__svml_sacos_ha_data_internal(%rip), %xmm0 #549.1339
        vfmsub231ps %xmm14, %xmm2, %xmm8                        #549.1129
        vandps    %xmm3, %xmm14, %xmm2                          #550.13
        vorps     %xmm6, %xmm2, %xmm6                           #577.13
        vaddps    %xmm9, %xmm8, %xmm11                          #549.1260
        vmovups   640+__svml_sacos_ha_data_internal(%rip), %xmm9 #552.54
        vfmadd213ps 704+__svml_sacos_ha_data_internal(%rip), %xmm4, %xmm9 #556.15
        vmovups   768+__svml_sacos_ha_data_internal(%rip), %xmm8 #554.54
        vfmadd213ps 832+__svml_sacos_ha_data_internal(%rip), %xmm4, %xmm8 #557.14
        vfmadd213ps 576+__svml_sacos_ha_data_internal(%rip), %xmm11, %xmm0 #549.1492
        vmulps    %xmm11, %xmm10, %xmm12                        #549.1550
        vfmadd213ps %xmm8, %xmm15, %xmm9                        #560.15
        vandnps   1024+__svml_sacos_ha_data_internal(%rip), %xmm3, %xmm8 #571.15
        vandnps   960+__svml_sacos_ha_data_internal(%rip), %xmm3, %xmm10 #572.15
        vfmsub213ps %xmm13, %xmm12, %xmm0                       #549.1591
        vfmadd213ps 896+__svml_sacos_ha_data_internal(%rip), %xmm4, %xmm9 #561.15
        vandps    %xmm3, %xmm0, %xmm1                           #551.13
        vmulps    %xmm9, %xmm4, %xmm0                           #562.15
        vsubps    %xmm1, %xmm6, %xmm2                           #578.13
        vcmpnltps %xmm7, %xmm4, %xmm4                           #563.17
        vandps    %xmm3, %xmm4, %xmm4                           #566.17
        vandps    1152+__svml_sacos_ha_data_internal(%rip), %xmm4, %xmm9 #567.14
        vorps     %xmm9, %xmm8, %xmm3                           #574.15
        vxorps    %xmm5, %xmm3, %xmm3                           #576.15
        vandps    1088+__svml_sacos_ha_data_internal(%rip), %xmm4, %xmm11 #568.14
        vsubps    %xmm1, %xmm3, %xmm1                           #579.14
        vfmadd213ps %xmm1, %xmm2, %xmm0                         #580.14
        vorps     %xmm11, %xmm10, %xmm4                         #575.15
        vaddps    %xmm0, %xmm6, %xmm0                           #581.14
        vxorps    %xmm5, %xmm0, %xmm5                           #582.14
        vaddps    %xmm5, %xmm4, %xmm0                           #583.14
        testl     %edx, %edx                                    #585.52
        jne       ..B5.3        # Prob 5%                       #585.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
..B5.2:                         # Preds ..B5.3 ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #588.12
        popq      %rbp                                          #588.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #588.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm7, 64(%rsp)                               #585.193
        vmovups   %xmm0, 128(%rsp)                              #585.264
        je        ..B5.2        # Prob 95%                      #585.368
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #585.448
        movq      %r12, 8(%rsp)                                 #585.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #585.448
        movq      %r13, (%rsp)                                  #585.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #585.448
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #585.511
        jc        ..B5.10       # Prob 5%                       #585.511
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #585.464
        cmpl      $4, %r12d                                     #585.459
        jl        ..B5.7        # Prob 82%                      #585.459
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #585.666
        jmp       ..B5.2        # Prob 100%                     #585.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #585.540
        lea       128(%rsp,%r12,4), %rsi                        #585.540
..___tag_value___svml_acosf4_ha_l9.85:
#       __svml_sacos_ha_cout_rare_internal(const float *, float *)
        call      __svml_sacos_ha_cout_rare_internal            #585.540
..___tag_value___svml_acosf4_ha_l9.86:
        jmp       ..B5.8        # Prob 100%                     #585.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_acosf4_ha_l9,@function
	.size	__svml_acosf4_ha_l9,.-__svml_acosf4_ha_l9
..LN__svml_acosf4_ha_l9.4:
	.data
# -- End  __svml_acosf4_ha_l9
	.text
.L_2__routine_start___svml_acosf4_ha_e9_5:
# -- Begin  __svml_acosf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_acosf4_ha_e9
# --- __svml_acosf4_ha_e9(__m128)
__svml_acosf4_ha_e9:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1344.1
        .byte     15                                            #1427.540
        .byte     30                                            #1427.540
        .byte     250                                           #1427.540
	.cfi_startproc
..___tag_value___svml_acosf4_ha_e9.88:
..L89:
                                                         #1344.1
        pushq     %rbp                                          #1344.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1344.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1344.1
        subq      $192, %rsp                                    #1344.1
        movaps    %xmm0, %xmm7                                  #1344.1
        movups    __svml_sacos_ha_data_internal(%rip), %xmm5    #1379.43
        movups    64+__svml_sacos_ha_data_internal(%rip), %xmm4 #1380.44
        movaps    %xmm5, %xmm6                                  #1382.8
        orps      %xmm7, %xmm6                                  #1382.8
        movaps    %xmm4, %xmm8                                  #1385.20
        mulps     %xmm6, %xmm8                                  #1385.20
        movaps    %xmm6, %xmm0                                  #1386.9
        mulps     %xmm6, %xmm0                                  #1386.9
        addps     %xmm8, %xmm4                                  #1385.8
        minps     %xmm4, %xmm0                                  #1387.9
        rsqrtps   %xmm4, %xmm10                                 #1391.494
        movaps    %xmm4, %xmm15                                 #1391.620
        movaps    %xmm0, %xmm3                                  #1390.14
        cmpltps   192+__svml_sacos_ha_data_internal(%rip), %xmm15 #1391.620
        cmpnltps  %xmm4, %xmm3                                  #1390.14
        addps     %xmm4, %xmm4                                  #1391.713
        movups    320+__svml_sacos_ha_data_internal(%rip), %xmm11 #1391.786
        andnps    %xmm10, %xmm15                                #1391.663
        movaps    %xmm11, %xmm2                                 #1391.1058
        movaps    %xmm15, %xmm13                                #1391.1350
        andps     %xmm4, %xmm2                                  #1391.1058
        andps     %xmm7, %xmm5                                  #1383.12
        movups    512+__svml_sacos_ha_data_internal(%rip), %xmm1 #1391.1805
        subps     %xmm2, %xmm4                                  #1391.1104
        mulps     %xmm15, %xmm2                                 #1391.1142
        mulps     %xmm15, %xmm4                                 #1391.1180
        andps     %xmm2, %xmm11                                 #1391.1220
        movaps    %xmm2, %xmm12                                 #1391.1267
        mulps     %xmm4, %xmm13                                 #1391.1350
        subps     %xmm11, %xmm12                                #1391.1267
        mulps     %xmm15, %xmm11                                #1391.1308
        mulps     %xmm12, %xmm15                                #1391.1390
        subps     448+__svml_sacos_ha_data_internal(%rip), %xmm11 #1391.1433
        movaps    %xmm2, %xmm14                                 #1391.1566
        andps     %xmm3, %xmm2                                  #1392.13
        movups    640+__svml_sacos_ha_data_internal(%rip), %xmm8 #1398.27
        movaps    %xmm3, %xmm12                                 #1413.15
        mulps     %xmm0, %xmm8                                  #1398.27
        addps     %xmm11, %xmm13                                #1391.1478
        addps     %xmm4, %xmm14                                 #1391.1566
        addps     %xmm13, %xmm15                                #1391.1523
        addps     704+__svml_sacos_ha_data_internal(%rip), %xmm8 #1398.15
        mulps     %xmm15, %xmm1                                 #1391.1805
        mulps     %xmm14, %xmm15                                #1391.1863
        addps     576+__svml_sacos_ha_data_internal(%rip), %xmm1 #1391.1793
        mulps     %xmm15, %xmm1                                 #1391.1916
        movups    768+__svml_sacos_ha_data_internal(%rip), %xmm11 #1399.26
        subps     %xmm4, %xmm1                                  #1391.1904
        mulps     %xmm0, %xmm11                                 #1399.26
        movaps    %xmm0, %xmm4                                  #1400.13
        andps     %xmm3, %xmm1                                  #1393.13
        mulps     %xmm0, %xmm4                                  #1400.13
        addps     832+__svml_sacos_ha_data_internal(%rip), %xmm11 #1399.14
        mulps     %xmm4, %xmm8                                  #1402.27
        movups    256+__svml_sacos_ha_data_internal(%rip), %xmm9 #1381.41
        addps     %xmm8, %xmm11                                 #1402.15
        cmpnleps  %xmm6, %xmm9                                  #1384.13
        mulps     %xmm0, %xmm11                                 #1403.27
        movmskps  %xmm9, %edx                                   #1389.40
        movups    1152+__svml_sacos_ha_data_internal(%rip), %xmm9 #1409.14
        movups    1088+__svml_sacos_ha_data_internal(%rip), %xmm10 #1410.14
        andnps    1024+__svml_sacos_ha_data_internal(%rip), %xmm12 #1413.15
        addps     896+__svml_sacos_ha_data_internal(%rip), %xmm11 #1403.15
        mulps     %xmm0, %xmm11                                 #1404.15
        cmpnltps  %xmm7, %xmm0                                  #1405.17
        andps     %xmm3, %xmm0                                  #1408.17
        andps     %xmm0, %xmm9                                  #1409.14
        andps     %xmm0, %xmm10                                 #1410.14
        movaps    %xmm3, %xmm0                                  #1414.15
        andnps    %xmm6, %xmm3                                  #1415.12
        orps      %xmm3, %xmm2                                  #1419.13
        orps      %xmm9, %xmm12                                 #1416.15
        movaps    %xmm2, %xmm3                                  #1420.13
        pxor      %xmm5, %xmm12                                 #1418.15
        andnps    960+__svml_sacos_ha_data_internal(%rip), %xmm0 #1414.15
        subps     %xmm1, %xmm3                                  #1420.13
        subps     %xmm1, %xmm12                                 #1421.14
        mulps     %xmm3, %xmm11                                 #1422.26
        orps      %xmm10, %xmm0                                 #1417.15
        addps     %xmm11, %xmm12                                #1422.14
        addps     %xmm12, %xmm2                                 #1423.14
        pxor      %xmm5, %xmm2                                  #1424.14
        addps     %xmm2, %xmm0                                  #1425.14
        testl     %edx, %edx                                    #1427.52
        jne       ..B6.3        # Prob 5%                       #1427.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
..B6.2:                         # Preds ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1430.12
        popq      %rbp                                          #1430.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1430.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm7, 64(%rsp)                               #1427.193
        movups    %xmm0, 128(%rsp)                              #1427.264
                                # LOE rbx r12 r13 r14 r15 edx
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1427.448
        movq      %r12, 8(%rsp)                                 #1427.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1427.448
        movq      %r13, (%rsp)                                  #1427.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1427.448
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1427.511
        jc        ..B6.10       # Prob 5%                       #1427.511
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1427.464
        cmpl      $4, %r12d                                     #1427.459
        jl        ..B6.7        # Prob 82%                      #1427.459
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1427.666
        jmp       ..B6.2        # Prob 100%                     #1427.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1427.540
        lea       128(%rsp,%r12,4), %rsi                        #1427.540
..___tag_value___svml_acosf4_ha_e9.106:
#       __svml_sacos_ha_cout_rare_internal(const float *, float *)
        call      __svml_sacos_ha_cout_rare_internal            #1427.540
..___tag_value___svml_acosf4_ha_e9.107:
        jmp       ..B6.8        # Prob 100%                     #1427.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_acosf4_ha_e9,@function
	.size	__svml_acosf4_ha_e9,.-__svml_acosf4_ha_e9
..LN__svml_acosf4_ha_e9.5:
	.data
# -- End  __svml_acosf4_ha_e9
	.text
.L_2__routine_start___svml_sacos_ha_cout_rare_internal_6:
# -- Begin  __svml_sacos_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_sacos_ha_cout_rare_internal
	.globl __svml_sacos_ha_cout_rare_internal
# --- __svml_sacos_ha_cout_rare_internal(const float *, float *)
__svml_sacos_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1159.1
        .byte     15                                            #1336.17
        .byte     30                                            #1336.17
        .byte     250                                           #1336.17
	.cfi_startproc
..___tag_value___svml_sacos_ha_cout_rare_internal.109:
..L110:
                                                        #1159.1
        xorl      %eax, %eax                                    #1172.14
        movzwl    2(%rdi), %edx                                 #1173.35
        andl      $32640, %edx                                  #1173.35
        movss     (%rdi), %xmm1                                 #1175.23
        cmpl      $32640, %edx                                  #1173.59
        je        ..B7.20       # Prob 16%                      #1173.59
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm1
..B7.2:                         # Preds ..B7.1
                                # Execution count [8.40e-01]
        cvtss2sd  %xmm1, %xmm1                                  #1175.23
        movsd     %xmm1, -32(%rsp)                              #1175.13
        andb      $127, -25(%rsp)                               #1176.34
        movsd     -32(%rsp), %xmm12                             #1177.18
        movsd     4168+_vmldACosHATab(%rip), %xmm0              #1177.44
        comisd    %xmm12, %xmm0                                 #1177.44
        jbe       ..B7.14       # Prob 50%                      #1177.44
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm1 xmm12
..B7.3:                         # Preds ..B7.2
                                # Execution count [4.20e-01]
        movsd     4176+_vmldACosHATab(%rip), %xmm1              #1179.48
        comisd    %xmm12, %xmm1                                 #1179.48
        jbe       ..B7.10       # Prob 50%                      #1179.48
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm1 xmm12
..B7.4:                         # Preds ..B7.3
                                # Execution count [2.10e-01]
        comisd    4128+_vmldACosHATab(%rip), %xmm12             #1181.52
        jbe       ..B7.9        # Prob 50%                      #1181.52
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm12
..B7.5:                         # Preds ..B7.4
                                # Execution count [1.05e-01]
        movsd     4104+_vmldACosHATab(%rip), %xmm6              #1183.68
        movaps    %xmm12, %xmm8                                 #1183.174
        mulsd     %xmm12, %xmm6                                 #1183.68
        movaps    %xmm12, %xmm7                                 #1185.44
        movsd     %xmm6, -40(%rsp)                              #1183.25
        movsd     -40(%rsp), %xmm13                             #1183.107
        movsd     4104+_vmldACosHATab(%rip), %xmm5              #1190.68
        subsd     -32(%rsp), %xmm13                             #1183.112
        movsd     %xmm13, -48(%rsp)                             #1183.93
        movsd     -40(%rsp), %xmm15                             #1183.138
        movsd     -48(%rsp), %xmm14                             #1183.143
        subsd     %xmm14, %xmm15                                #1183.143
        movaps    %xmm12, %xmm14                                #1192.48
        movsd     %xmm15, -40(%rsp)                             #1183.124
        movsd     -40(%rsp), %xmm6                              #1183.174
        subsd     %xmm6, %xmm8                                  #1183.174
        movsd     %xmm8, -48(%rsp)                              #1183.149
        movsd     -40(%rsp), %xmm9                              #1183.188
        movaps    %xmm9, %xmm4                                  #1184.40
        addsd     %xmm9, %xmm7                                  #1185.44
        mulsd     %xmm9, %xmm4                                  #1184.40
        movsd     -48(%rsp), %xmm10                             #1183.200
        movaps    %xmm4, %xmm11                                 #1186.39
        mulsd     %xmm10, %xmm7                                 #1185.53
        mulsd     %xmm4, %xmm5                                  #1190.68
        addsd     %xmm7, %xmm11                                 #1186.39
        movsd     4312+_vmldACosHATab(%rip), %xmm8              #1187.81
        mulsd     %xmm11, %xmm8                                 #1187.81
        movsd     %xmm5, -40(%rsp)                              #1190.25
        movsd     -40(%rsp), %xmm1                              #1190.107
        movsd     4104+_vmldACosHATab(%rip), %xmm5              #1193.67
        subsd     %xmm4, %xmm1                                  #1190.112
        addsd     4304+_vmldACosHATab(%rip), %xmm8              #1187.103
        mulsd     %xmm11, %xmm8                                 #1187.125
        movsd     %xmm1, -48(%rsp)                              #1190.93
        movsd     -40(%rsp), %xmm0                              #1190.138
        movsd     -48(%rsp), %xmm2                              #1190.143
        addsd     4296+_vmldACosHATab(%rip), %xmm8              #1187.147
        subsd     %xmm2, %xmm0                                  #1190.143
        mulsd     %xmm11, %xmm8                                 #1187.169
        movsd     %xmm0, -40(%rsp)                              #1190.124
        movsd     -40(%rsp), %xmm3                              #1190.174
        addsd     4288+_vmldACosHATab(%rip), %xmm8              #1187.191
        subsd     %xmm3, %xmm4                                  #1190.174
        mulsd     %xmm11, %xmm8                                 #1187.213
        movsd     %xmm4, -48(%rsp)                              #1190.149
        movsd     -40(%rsp), %xmm6                              #1190.188
        mulsd     %xmm6, %xmm9                                  #1191.39
        addsd     4280+_vmldACosHATab(%rip), %xmm8              #1188.49
        mulsd     %xmm6, %xmm10                                 #1192.63
        mulsd     %xmm11, %xmm8                                 #1188.71
        mulsd     %xmm9, %xmm5                                  #1193.67
        addsd     4272+_vmldACosHATab(%rip), %xmm8              #1188.93
        mulsd     %xmm11, %xmm8                                 #1188.115
        movaps    %xmm9, %xmm0                                  #1193.171
        movsd     -48(%rsp), %xmm13                             #1190.200
        movaps    %xmm6, %xmm4                                  #1194.39
        movsd     %xmm5, -40(%rsp)                              #1193.25
        addsd     %xmm13, %xmm7                                 #1192.40
        addsd     4264+_vmldACosHATab(%rip), %xmm8              #1188.137
        mulsd     %xmm7, %xmm14                                 #1192.48
        mulsd     %xmm11, %xmm8                                 #1188.159
        addsd     %xmm14, %xmm10                                #1192.63
        addsd     4256+_vmldACosHATab(%rip), %xmm8              #1188.181
        movsd     -40(%rsp), %xmm1                              #1193.106
        mulsd     %xmm11, %xmm8                                 #1188.203
        subsd     %xmm9, %xmm1                                  #1193.111
        addsd     %xmm10, %xmm9                                 #1195.77
        addsd     4248+_vmldACosHATab(%rip), %xmm8              #1188.225
        mulsd     %xmm9, %xmm7                                  #1195.77
        mulsd     %xmm11, %xmm8                                 #1188.247
        movsd     %xmm1, -48(%rsp)                              #1193.92
        movsd     -40(%rsp), %xmm2                              #1193.136
        movsd     -48(%rsp), %xmm15                             #1193.141
        subsd     %xmm15, %xmm2                                 #1193.141
        addsd     4240+_vmldACosHATab(%rip), %xmm8              #1189.49
        movsd     %xmm2, -40(%rsp)                              #1193.122
        movsd     -40(%rsp), %xmm5                              #1193.171
        mulsd     %xmm11, %xmm8                                 #1189.71
        subsd     %xmm5, %xmm0                                  #1193.171
        movsd     %xmm0, -48(%rsp)                              #1193.147
        movsd     -40(%rsp), %xmm3                              #1193.185
        movsd     -48(%rsp), %xmm5                              #1193.197
        movaps    %xmm5, %xmm13                                 #1195.46
        addsd     4232+_vmldACosHATab(%rip), %xmm8              #1189.93
        mulsd     %xmm3, %xmm4                                  #1194.39
        addsd     %xmm10, %xmm13                                #1195.46
        mulsd     %xmm11, %xmm8                                 #1189.115
        mulsd     %xmm13, %xmm6                                 #1195.46
        addsd     4224+_vmldACosHATab(%rip), %xmm8              #1189.137
        addsd     %xmm7, %xmm6                                  #1195.77
        mulsd     %xmm11, %xmm8                                 #1197.25
        movsd     4104+_vmldACosHATab(%rip), %xmm7              #1196.67
        movaps    %xmm4, %xmm13                                 #1196.171
        mulsd     %xmm4, %xmm7                                  #1196.67
        addsd     4216+_vmldACosHATab(%rip), %xmm8              #1199.25
        movsd     %xmm7, -40(%rsp)                              #1196.25
        movsd     -40(%rsp), %xmm1                              #1196.106
        movsd     4200+_vmldACosHATab(%rip), %xmm11             #1198.80
        subsd     %xmm4, %xmm1                                  #1196.111
        mulsd     %xmm9, %xmm11                                 #1198.80
        addsd     %xmm6, %xmm4                                  #1198.134
        movsd     %xmm1, -48(%rsp)                              #1196.92
        movaps    %xmm12, %xmm9                                 #1202.50
        movsd     -40(%rsp), %xmm0                              #1196.136
        movsd     -48(%rsp), %xmm2                              #1196.141
        movsd     4192+_vmldACosHATab(%rip), %xmm1              #1199.59
        subsd     %xmm2, %xmm0                                  #1196.141
        mulsd     %xmm1, %xmm3                                  #1200.55
        mulsd     %xmm1, %xmm10                                 #1199.59
        mulsd     %xmm4, %xmm8                                  #1199.25
        addsd     %xmm3, %xmm9                                  #1202.50
        mulsd     %xmm1, %xmm5                                  #1201.59
        addsd     %xmm10, %xmm11                                #1199.25
        movsd     %xmm0, -40(%rsp)                              #1196.122
        pxor      %xmm1, %xmm1                                  #1209.31
        movsd     -40(%rsp), %xmm7                              #1196.171
        addsd     %xmm11, %xmm8                                 #1199.25
        subsd     %xmm7, %xmm13                                 #1196.171
        addsd     %xmm5, %xmm8                                  #1201.25
        movsd     %xmm13, -48(%rsp)                             #1196.147
        movsd     -40(%rsp), %xmm0                              #1196.185
        movsd     -48(%rsp), %xmm2                              #1196.197
        movsd     %xmm9, -40(%rsp)                              #1202.25
        addsd     %xmm2, %xmm6                                  #1208.25
        movsd     -40(%rsp), %xmm10                             #1202.88
        movsd     %xmm3, -56(%rsp)                              #1200.25
        subsd     %xmm10, %xmm12                                #1202.88
        movsd     4208+_vmldACosHATab(%rip), %xmm4              #1204.59
        addsd     %xmm12, %xmm3                                 #1202.116
        mulsd     %xmm4, %xmm0                                  #1205.55
        mulsd     %xmm4, %xmm6                                  #1208.25
        movsd     %xmm3, -48(%rsp)                              #1202.94
        movsd     -40(%rsp), %xmm3                              #1202.133
        movaps    %xmm3, %xmm12                                 #1207.48
        movsd     -48(%rsp), %xmm7                              #1202.144
        addsd     %xmm0, %xmm12                                 #1207.48
        addsd     %xmm7, %xmm8                                  #1203.25
        movsd     %xmm12, -40(%rsp)                             #1207.25
        movsd     -40(%rsp), %xmm12                             #1207.84
        subsd     %xmm12, %xmm3                                 #1207.84
        addsd     %xmm3, %xmm0                                  #1207.112
        movsd     %xmm0, -48(%rsp)                              #1207.90
        movsd     -40(%rsp), %xmm3                              #1207.129
        movsd     -48(%rsp), %xmm0                              #1207.140
        cvtss2sd  (%rdi), %xmm1                                 #1209.31
        addsd     %xmm8, %xmm0                                  #1208.25
        comisd    4184+_vmldACosHATab(%rip), %xmm1              #1209.52
        addsd     %xmm0, %xmm6                                  #1208.25
        jbe       ..B7.7        # Prob 50%                      #1209.52
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm3 xmm6
..B7.6:                         # Preds ..B7.5
                                # Execution count [5.25e-02]
        movsd     4136+_vmldACosHATab(%rip), %xmm2              #1211.61
        movaps    %xmm2, %xmm0                                  #1211.61
        subsd     %xmm3, %xmm0                                  #1211.61
        movsd     %xmm0, -40(%rsp)                              #1211.29
        movsd     -40(%rsp), %xmm1                              #1211.155
        movsd     4144+_vmldACosHATab(%rip), %xmm0              #1212.56
        subsd     %xmm1, %xmm2                                  #1211.155
        subsd     %xmm6, %xmm0                                  #1212.79
        subsd     %xmm3, %xmm2                                  #1211.175
        movsd     %xmm2, -48(%rsp)                              #1211.161
        movsd     -40(%rsp), %xmm1                              #1211.204
        movsd     -48(%rsp), %xmm3                              #1211.215
        addsd     %xmm3, %xmm0                                  #1213.29
        jmp       ..B7.8        # Prob 100%                     #1213.29
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm0 xmm1
..B7.7:                         # Preds ..B7.5
                                # Execution count [5.25e-02]
        movsd     4136+_vmldACosHATab(%rip), %xmm2              #1217.61
        movaps    %xmm3, %xmm0                                  #1217.85
        addsd     %xmm2, %xmm0                                  #1217.85
        movsd     %xmm0, -40(%rsp)                              #1217.29
        movsd     -40(%rsp), %xmm1                              #1217.154
        subsd     %xmm1, %xmm2                                  #1217.154
        addsd     %xmm2, %xmm3                                  #1217.182
        movsd     %xmm3, -48(%rsp)                              #1217.160
        movsd     4144+_vmldACosHATab(%rip), %xmm3              #1218.56
        movsd     -40(%rsp), %xmm1                              #1217.202
        addsd     %xmm3, %xmm6                                  #1218.79
        movsd     -48(%rsp), %xmm0                              #1217.213
        addsd     %xmm6, %xmm0                                  #1219.29
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm0 xmm1
..B7.8:                         # Preds ..B7.6 ..B7.7
                                # Execution count [1.05e-01]
        addsd     %xmm0, %xmm1                                  #1221.43
        cvtsd2ss  %xmm1, %xmm1                                  #1222.26
        movss     %xmm1, (%rsi)                                 #1222.26
        ret                                                     #1222.26
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.9:                         # Preds ..B7.4
                                # Execution count [1.05e-01]
        movsd     4144+_vmldACosHATab(%rip), %xmm1              #1229.52
        addsd     %xmm0, %xmm12                                 #1226.70
        movsd     %xmm12, -40(%rsp)                             #1226.25
        movsd     -40(%rsp), %xmm0                              #1227.41
        mulsd     -32(%rsp), %xmm0                              #1227.41
        movsd     %xmm0, -56(%rsp)                              #1227.25
        movb      -49(%rsp), %dl                                #1228.46
        movb      3(%rdi), %dil                                 #1228.85
        andb      $127, %dl                                     #1228.46
        andb      $-128, %dil                                   #1228.46
        orb       %dil, %dl                                     #1228.46
        movb      %dl, -49(%rsp)                                #1228.46
        subsd     -56(%rsp), %xmm1                              #1229.75
        addsd     4136+_vmldACosHATab(%rip), %xmm1              #1230.73
        cvtsd2ss  %xmm1, %xmm1                                  #1231.26
        movss     %xmm1, (%rsi)                                 #1231.26
        ret                                                     #1231.26
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.10:                        # Preds ..B7.3
                                # Execution count [2.10e-01]
        movaps    %xmm0, %xmm8                                  #1236.66
        movzwl    4174+_vmldACosHATab(%rip), %r9d               #1240.42
        pxor      %xmm13, %xmm13                                #1263.56
        andl      $-32753, %r9d                                 #1240.42
        subsd     %xmm12, %xmm8                                 #1236.66
        subsd     %xmm0, %xmm13                                 #1263.56
        mulsd     %xmm8, %xmm1                                  #1237.45
        movsd     %xmm1, -56(%rsp)                              #1237.21
        movzwl    -50(%rsp), %ecx                               #1238.48
        andl      $32752, %ecx                                  #1238.48
        shrl      $4, %ecx                                      #1238.48
        addl      $-1023, %ecx                                  #1238.70
        movl      %ecx, %r8d                                    #1240.81
        movl      %ecx, %edx                                    #1242.32
        negl      %r8d                                          #1240.81
        addl      $1023, %r8d                                   #1240.81
        andl      $2047, %r8d                                   #1240.42
        shll      $4, %r8d                                      #1240.42
        movsd     %xmm0, -32(%rsp)                              #1239.21
        orl       %r8d, %r9d                                    #1240.42
        movw      %r9w, -26(%rsp)                               #1240.42
        andl      $1, %edx                                      #1242.32
        movsd     -32(%rsp), %xmm4                              #1241.33
        lea       _vmldACosHATab(%rip), %r8                     #1260.45
        mulsd     %xmm4, %xmm1                                  #1241.33
        movl      %edx, %r10d                                   #1259.40
        movaps    %xmm1, %xmm15                                 #1246.39
        movsd     4112+_vmldACosHATab(%rip), %xmm6              #1252.53
        addsd     %xmm1, %xmm15                                 #1246.39
        jne       ..L111        # Prob 50%                      #1250.25
        movaps    %xmm1, %xmm15                                 #1250.25
..L111:                                                         #
        mulsd     %xmm15, %xmm6                                 #1252.53
        movaps    %xmm15, %xmm7                                 #1255.36
        movaps    %xmm6, %xmm9                                  #1253.37
        subl      %edx, %ecx                                    #1243.27
        movsd     4120+_vmldACosHATab(%rip), %xmm11             #1257.54
        subsd     %xmm15, %xmm9                                 #1253.37
        addsd     %xmm1, %xmm11                                 #1257.54
        movsd     %xmm9, -48(%rsp)                              #1253.21
        movsd     -48(%rsp), %xmm10                             #1254.37
        movsd     %xmm11, -24(%rsp)                             #1257.21
        subsd     %xmm10, %xmm6                                 #1254.37
        movl      -24(%rsp), %r11d                              #1258.48
        movaps    %xmm6, %xmm14                                 #1265.36
        shll      $8, %r10d                                     #1259.40
        andl      $511, %r11d                                   #1258.80
        addl      %r10d, %r11d                                  #1259.40
        subsd     %xmm6, %xmm7                                  #1255.36
        movsd     (%r8,%r11,8), %xmm5                           #1260.45
        addsd     %xmm7, %xmm14                                 #1265.36
        mulsd     %xmm5, %xmm6                                  #1270.40
        movaps    %xmm5, %xmm12                                 #1261.35
        mulsd     %xmm5, %xmm12                                 #1261.35
        mulsd     %xmm12, %xmm14                                #1265.36
        movsd     4512+_vmldACosHATab(%rip), %xmm4              #1266.71
        addsd     %xmm13, %xmm14                                #1265.36
        mulsd     %xmm14, %xmm4                                 #1266.71
        shrl      $1, %ecx                                      #1243.38
        addsd     4504+_vmldACosHATab(%rip), %xmm4              #1266.94
        mulsd     %xmm14, %xmm4                                 #1266.116
        addl      $1023, %ecx                                   #1269.73
        andl      $2047, %ecx                                   #1269.42
        addsd     4496+_vmldACosHATab(%rip), %xmm4              #1266.139
        mulsd     %xmm14, %xmm4                                 #1266.161
        movzwl    4174+_vmldACosHATab(%rip), %r9d               #1269.42
        shll      $4, %ecx                                      #1269.42
        andl      $-32753, %r9d                                 #1269.42
        movsd     %xmm0, -16(%rsp)                              #1268.21
        orl       %ecx, %r9d                                    #1269.42
        movw      %r9w, -10(%rsp)                               #1269.42
        movsd     -16(%rsp), %xmm9                              #1271.41
        mulsd     %xmm9, %xmm6                                  #1271.41
        addsd     4488+_vmldACosHATab(%rip), %xmm4              #1266.184
        mulsd     %xmm14, %xmm4                                 #1266.206
        movsd     4104+_vmldACosHATab(%rip), %xmm3              #1279.66
        mulsd     %xmm6, %xmm3                                  #1279.66
        addsd     4480+_vmldACosHATab(%rip), %xmm4              #1267.57
        mulsd     %xmm14, %xmm4                                 #1267.79
        movsd     %xmm3, -40(%rsp)                              #1279.21
        movsd     -40(%rsp), %xmm1                              #1279.105
        movsd     4352+_vmldACosHATab(%rip), %xmm11             #1285.256
        subsd     %xmm6, %xmm1                                  #1279.110
        addsd     4472+_vmldACosHATab(%rip), %xmm4              #1267.102
        addsd     4360+_vmldACosHATab(%rip), %xmm11             #1285.256
        mulsd     %xmm14, %xmm4                                 #1267.124
        mulsd     %xmm8, %xmm11                                 #1285.278
        addsd     4464+_vmldACosHATab(%rip), %xmm4              #1267.147
        mulsd     %xmm14, %xmm4                                 #1272.39
        mulsd     %xmm15, %xmm4                                 #1275.41
        movsd     %xmm1, -48(%rsp)                              #1279.91
        addsd     %xmm4, %xmm7                                  #1277.41
        mulsd     %xmm5, %xmm7                                  #1277.41
        movsd     4456+_vmldACosHATab(%rip), %xmm5              #1282.77
        mulsd     %xmm8, %xmm5                                  #1282.77
        mulsd     %xmm9, %xmm7                                  #1278.41
        addsd     4448+_vmldACosHATab(%rip), %xmm5              #1282.98
        mulsd     %xmm8, %xmm5                                  #1282.120
        movsd     -40(%rsp), %xmm2                              #1279.138
        movsd     -48(%rsp), %xmm0                              #1279.143
        movsd     4104+_vmldACosHATab(%rip), %xmm4              #1287.59
        subsd     %xmm0, %xmm2                                  #1279.143
        mulsd     %xmm8, %xmm4                                  #1287.59
        addsd     4440+_vmldACosHATab(%rip), %xmm5              #1282.141
        mulsd     %xmm8, %xmm5                                  #1282.163
        movaps    %xmm8, %xmm0                                  #1287.155
        movsd     %xmm2, -40(%rsp)                              #1279.124
        movsd     -40(%rsp), %xmm14                             #1279.176
        movsd     4104+_vmldACosHATab(%rip), %xmm2              #1290.63
        subsd     %xmm14, %xmm6                                 #1279.176
        addsd     4432+_vmldACosHATab(%rip), %xmm5              #1282.184
        mulsd     %xmm8, %xmm5                                  #1282.206
        movsd     %xmm6, -48(%rsp)                              #1279.149
        movsd     -40(%rsp), %xmm6                              #1279.190
        movsd     -48(%rsp), %xmm10                             #1279.202
        movsd     %xmm4, -40(%rsp)                              #1287.21
        addsd     %xmm10, %xmm7                                 #1281.21
        addsd     4424+_vmldACosHATab(%rip), %xmm5              #1283.45
        mulsd     %xmm8, %xmm5                                  #1283.67
        movsd     -40(%rsp), %xmm3                              #1287.98
        movsd     4336+_vmldACosHATab(%rip), %xmm12             #1285.176
        subsd     %xmm8, %xmm3                                  #1287.103
        addsd     4416+_vmldACosHATab(%rip), %xmm5              #1283.88
        addsd     4344+_vmldACosHATab(%rip), %xmm12             #1285.176
        mulsd     %xmm8, %xmm5                                  #1283.110
        addsd     %xmm11, %xmm12                                #1285.278
        addsd     4408+_vmldACosHATab(%rip), %xmm5              #1283.131
        mulsd     %xmm8, %xmm12                                 #1285.281
        mulsd     %xmm8, %xmm5                                  #1283.153
        movsd     %xmm3, -48(%rsp)                              #1287.84
        movsd     -40(%rsp), %xmm1                              #1287.124
        movsd     -48(%rsp), %xmm15                             #1287.129
        movsd     4320+_vmldACosHATab(%rip), %xmm13             #1285.96
        subsd     %xmm15, %xmm1                                 #1287.129
        addsd     4400+_vmldACosHATab(%rip), %xmm5              #1283.174
        addsd     4328+_vmldACosHATab(%rip), %xmm13             #1285.96
        mulsd     %xmm8, %xmm5                                  #1283.196
        addsd     %xmm12, %xmm13                                #1285.281
        addsd     4392+_vmldACosHATab(%rip), %xmm5              #1283.217
        movsd     %xmm1, -40(%rsp)                              #1287.110
        mulsd     %xmm8, %xmm5                                  #1283.239
        movsd     -40(%rsp), %xmm4                              #1287.155
        subsd     %xmm4, %xmm0                                  #1287.155
        addsd     4384+_vmldACosHATab(%rip), %xmm5              #1283.260
        movsd     %xmm0, -48(%rsp)                              #1287.135
        movsd     -40(%rsp), %xmm4                              #1287.169
        movaps    %xmm4, %xmm14                                 #1288.35
        mulsd     %xmm4, %xmm14                                 #1288.35
        mulsd     %xmm8, %xmm5                                  #1283.282
        mulsd     %xmm14, %xmm2                                 #1290.63
        addsd     4376+_vmldACosHATab(%rip), %xmm5              #1284.45
        movsd     -48(%rsp), %xmm3                              #1287.181
        movsd     %xmm2, -40(%rsp)                              #1290.21
        movsd     -40(%rsp), %xmm9                              #1290.102
        mulsd     %xmm8, %xmm5                                  #1284.67
        subsd     %xmm14, %xmm9                                 #1290.107
        movsd     %xmm9, -48(%rsp)                              #1290.88
        movsd     -40(%rsp), %xmm11                             #1290.132
        movsd     -48(%rsp), %xmm10                             #1290.137
        movsd     4336+_vmldACosHATab(%rip), %xmm0              #1291.68
        subsd     %xmm10, %xmm11                                #1290.137
        mulsd     %xmm4, %xmm0                                  #1291.68
        addsd     4368+_vmldACosHATab(%rip), %xmm5              #1284.88
        addsd     %xmm8, %xmm4                                  #1289.45
        mulsd     %xmm8, %xmm5                                  #1284.110
        mulsd     %xmm3, %xmm4                                  #1289.45
        mulsd     %xmm8, %xmm5                                  #1284.112
        movsd     %xmm11, -40(%rsp)                             #1290.118
        movaps    %xmm0, %xmm1                                  #1293.45
        movsd     -40(%rsp), %xmm12                             #1290.167
        mulsd     %xmm8, %xmm5                                  #1284.114
        subsd     %xmm12, %xmm14                                #1290.167
        movsd     %xmm14, -48(%rsp)                             #1290.143
        addsd     %xmm5, %xmm13                                 #1286.25
        movsd     -40(%rsp), %xmm9                              #1290.181
        mulsd     4352+_vmldACosHATab(%rip), %xmm9              #1292.68
        mulsd     %xmm13, %xmm7                                 #1286.25
        addsd     %xmm9, %xmm1                                  #1293.45
        movsd     -48(%rsp), %xmm2                              #1290.193
        movsd     %xmm1, -40(%rsp)                              #1293.21
        addsd     %xmm2, %xmm4                                  #1296.155
        movsd     -40(%rsp), %xmm13                             #1293.85
        movsd     %xmm9, -24(%rsp)                              #1292.21
        subsd     %xmm13, %xmm0                                 #1293.85
        mulsd     4352+_vmldACosHATab(%rip), %xmm4              #1296.155
        addsd     %xmm0, %xmm9                                  #1293.114
        movsd     %xmm9, -48(%rsp)                              #1293.91
        movsd     -40(%rsp), %xmm12                             #1293.133
        movsd     4320+_vmldACosHATab(%rip), %xmm10             #1294.77
        movsd     -48(%rsp), %xmm1                              #1293.144
        addsd     %xmm12, %xmm10                                #1294.77
        movsd     %xmm10, -40(%rsp)                             #1294.21
        movsd     -40(%rsp), %xmm15                             #1294.146
        movsd     4320+_vmldACosHATab(%rip), %xmm11             #1294.146
        movsd     4104+_vmldACosHATab(%rip), %xmm9              #1295.62
        subsd     %xmm15, %xmm11                                #1294.146
        movsd     4336+_vmldACosHATab(%rip), %xmm2              #1296.102
        addsd     %xmm11, %xmm12                                #1294.174
        mulsd     %xmm3, %xmm2                                  #1296.102
        movsd     %xmm12, -48(%rsp)                             #1294.152
        movsd     -40(%rsp), %xmm15                             #1294.191
        mulsd     %xmm15, %xmm9                                 #1295.62
        movsd     -48(%rsp), %xmm0                              #1294.202
        movsd     %xmm9, -40(%rsp)                              #1295.21
        movsd     -40(%rsp), %xmm10                             #1295.101
        movsd     4360+_vmldACosHATab(%rip), %xmm3              #1297.140
        subsd     %xmm15, %xmm10                                #1295.106
        mulsd     %xmm8, %xmm3                                  #1297.161
        movsd     %xmm10, -48(%rsp)                             #1295.87
        movsd     -40(%rsp), %xmm11                             #1295.130
        movsd     -48(%rsp), %xmm13                             #1295.135
        subsd     %xmm13, %xmm11                                #1295.135
        addsd     4344+_vmldACosHATab(%rip), %xmm3              #1297.161
        movsd     %xmm11, -40(%rsp)                             #1295.116
        movsd     -40(%rsp), %xmm14                             #1295.164
        mulsd     %xmm8, %xmm3                                  #1297.164
        subsd     %xmm14, %xmm15                                #1295.164
        movsd     %xmm15, -48(%rsp)                             #1295.141
        movsd     -40(%rsp), %xmm10                             #1295.178
        movsd     -48(%rsp), %xmm9                              #1295.190
        addsd     %xmm9, %xmm4                                  #1296.50
        addsd     4328+_vmldACosHATab(%rip), %xmm3              #1297.164
        addsd     %xmm2, %xmm4                                  #1296.57
        addsd     %xmm5, %xmm3                                  #1297.168
        addsd     %xmm1, %xmm4                                  #1296.102
        addsd     %xmm0, %xmm4                                  #1296.155
        addsd     %xmm3, %xmm4                                  #1297.21
        mulsd     %xmm6, %xmm4                                  #1297.21
        mulsd     %xmm10, %xmm6                                 #1298.39
        addsd     %xmm7, %xmm4                                  #1297.21
        pxor      %xmm7, %xmm7                                  #1299.27
        cvtss2sd  (%rdi), %xmm7                                 #1299.27
        comisd    4184+_vmldACosHATab(%rip), %xmm7              #1299.48
        ja        ..B7.13       # Prob 50%                      #1299.48
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm4 xmm6
..B7.12:                        # Preds ..B7.10
                                # Execution count [1.05e-01]
        movsd     4152+_vmldACosHATab(%rip), %xmm2              #1306.57
        movaps    %xmm2, %xmm0                                  #1306.57
        movsd     4160+_vmldACosHATab(%rip), %xmm5              #1307.52
        subsd     %xmm6, %xmm0                                  #1306.57
        subsd     %xmm4, %xmm5                                  #1307.75
        movsd     %xmm0, -40(%rsp)                              #1306.25
        movsd     -40(%rsp), %xmm1                              #1306.151
        movsd     %xmm6, -56(%rsp)                              #1298.21
        subsd     %xmm1, %xmm2                                  #1306.151
        subsd     %xmm6, %xmm2                                  #1306.171
        movsd     %xmm2, -48(%rsp)                              #1306.157
        movsd     -40(%rsp), %xmm6                              #1306.200
        movsd     -48(%rsp), %xmm3                              #1306.211
        movaps    %xmm3, %xmm4                                  #1308.25
        addsd     %xmm5, %xmm4                                  #1308.25
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm4 xmm6
..B7.13:                        # Preds ..B7.10 ..B7.12
                                # Execution count [2.10e-01]
        addsd     %xmm4, %xmm6                                  #1310.39
        cvtsd2ss  %xmm6, %xmm6                                  #1311.22
        movss     %xmm6, (%rsi)                                 #1311.22
        ret                                                     #1311.22
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.14:                        # Preds ..B7.2
                                # Execution count [4.20e-01]
        ucomisd   %xmm0, %xmm1                                  #1316.45
        jp        ..B7.15       # Prob 0%                       #1316.45
        je        ..B7.19       # Prob 16%                      #1316.45
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm0 xmm1
..B7.15:                        # Preds ..B7.14
                                # Execution count [3.53e-01]
        xorps     .L_2il0floatpacket.205(%rip), %xmm0           #1320.51
        ucomisd   %xmm0, %xmm1                                  #1320.51
        jp        ..B7.16       # Prob 0%                       #1320.51
        je        ..B7.18       # Prob 16%                      #1320.51
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax
..B7.16:                        # Preds ..B7.15
                                # Execution count [2.96e-01]
        movl      $1, %eax                                      #1327.21
        pxor      %xmm1, %xmm1                                  #1326.45
        pxor      %xmm0, %xmm0                                  #1326.45
        divss     %xmm0, %xmm1                                  #1326.45
        movss     %xmm1, (%rsi)                                 #1326.22
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.17:                        # Preds ..B7.16
                                # Execution count [1.00e+00]
        ret                                                     #1339.12
                                # LOE
..B7.18:                        # Preds ..B7.15
                                # Execution count [5.64e-02]: Infreq
        movsd     4152+_vmldACosHATab(%rip), %xmm0              #1322.43
        addsd     4160+_vmldACosHATab(%rip), %xmm0              #1322.81
        cvtsd2ss  %xmm0, %xmm0                                  #1322.22
        movss     %xmm0, (%rsi)                                 #1322.22
        ret                                                     #1322.22
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.19:                        # Preds ..B7.14
                                # Execution count [6.72e-02]: Infreq
        movsd     4184+_vmldACosHATab(%rip), %xmm0              #1318.22
        cvtsd2ss  %xmm0, %xmm0                                  #1318.22
        movss     %xmm0, (%rsi)                                 #1318.22
        ret                                                     #1318.22
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.20:                        # Preds ..B7.1
                                # Execution count [1.60e-01]: Infreq
        divss     %xmm1, %xmm1                                  #1333.24
        xorl      %eax, %eax                                    #1334.39
        testl     $8388607, (%rdi)                              #1334.39
        movss     %xmm1, (%rsi)                                 #1333.14
        sete      %al                                           #1336.17
        ret                                                     #1336.17
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_sacos_ha_cout_rare_internal,@function
	.size	__svml_sacos_ha_cout_rare_internal,.-__svml_sacos_ha_cout_rare_internal
..LN__svml_sacos_ha_cout_rare_internal.6:
	.data
# -- End  __svml_sacos_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_sacos_ha_data_internal
	.globl __svml_sacos_ha_data_internal
__svml_sacos_ha_data_internal:
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
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
	.long	4294963200
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
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294959104
	.long	4294965248
	.long	4294965248
	.long	4294965248
	.long	4294965248
	.long	4294965248
	.long	4294965248
	.long	4294965248
	.long	4294965248
	.long	4294965248
	.long	4294965248
	.long	4294965248
	.long	4294965248
	.long	4294965248
	.long	4294965248
	.long	4294965248
	.long	4294965248
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
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	3183476740
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1048576001
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1026481159
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1019423339
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1027250868
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1033468946
	.long	1042983679
	.long	1042983679
	.long	1042983679
	.long	1042983679
	.long	1042983679
	.long	1042983679
	.long	1042983679
	.long	1042983679
	.long	1042983679
	.long	1042983679
	.long	1042983679
	.long	1042983679
	.long	1042983679
	.long	1042983679
	.long	1042983679
	.long	1042983679
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
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	1078530011
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	3015425326
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
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
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	4290772992
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1036306094
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	1042839218
	.long	939876771
	.long	939876771
	.long	939876771
	.long	939876771
	.long	939876771
	.long	939876771
	.long	939876771
	.long	939876771
	.long	939876771
	.long	939876771
	.long	939876771
	.long	939876771
	.long	939876771
	.long	939876771
	.long	939876771
	.long	939876771
	.type	__svml_sacos_ha_data_internal,@object
	.size	__svml_sacos_ha_data_internal,1600
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
.L_2il0floatpacket.205:
	.long	0x00000000,0x80000000,0x00000000,0x00000000
	.type	.L_2il0floatpacket.205,@object
	.size	.L_2il0floatpacket.205,16
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
