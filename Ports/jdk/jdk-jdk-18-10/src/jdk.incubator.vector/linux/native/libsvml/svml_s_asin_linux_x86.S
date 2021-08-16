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
	.file "svml_s_asin.c"
	.text
..TXTST0:
.L_2__routine_start___svml_asinf8_ha_l9_0:
# -- Begin  __svml_asinf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asinf8_ha_l9
# --- __svml_asinf8_ha_l9(__m256)
__svml_asinf8_ha_l9:
# parameter 1: %ymm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #135.1
        .byte     15                                            #208.546
        .byte     30                                            #208.546
        .byte     250                                           #208.546
	.cfi_startproc
..___tag_value___svml_asinf8_ha_l9.1:
..L2:
                                                          #135.1
        pushq     %rbp                                          #135.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #135.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #135.1
        subq      $192, %rsp                                    #135.1
        vmovups   __svml_sasin_ha_data_internal(%rip), %ymm8    #166.47
        vmovups   64+__svml_sasin_ha_data_internal(%rip), %ymm12 #167.47
        vmovups   256+__svml_sasin_ha_data_internal(%rip), %ymm9 #168.43
        vmovaps   %ymm0, %ymm7                                  #135.1
        vandps    %ymm7, %ymm8, %ymm6                           #169.8
        vmovaps   %ymm12, %ymm15                                #172.8
        vfnmadd231ps %ymm6, %ymm12, %ymm15                      #172.8
        vmulps    %ymm6, %ymm6, %ymm10                          #173.9
        vcmplt_oqps %ymm6, %ymm9, %ymm11                        #171.13
        vaddps    %ymm15, %ymm15, %ymm0                         #178.735
        vcmplt_oqps 192+__svml_sasin_ha_data_internal(%rip), %ymm15, %ymm13 #178.626
        vminps    %ymm15, %ymm10, %ymm4                         #174.9
        vcmpnlt_uqps %ymm12, %ymm6, %ymm3                       #177.14
        vrsqrtps  %ymm15, %ymm14                                #178.494
        vmovups   448+__svml_sasin_ha_data_internal(%rip), %ymm9 #178.902
        vmulps    %ymm4, %ymm4, %ymm15                          #185.13
        vandps    320+__svml_sasin_ha_data_internal(%rip), %ymm0, %ymm2 #178.997
        vsubps    %ymm2, %ymm0, %ymm1                           #178.1046
        vmovups   768+__svml_sasin_ha_data_internal(%rip), %ymm0 #181.57
        vandnps   %ymm7, %ymm8, %ymm5                           #170.12
        vandnps   %ymm14, %ymm13, %ymm8                         #178.682
        vmulps    %ymm2, %ymm8, %ymm2                           #178.1087
        vmulps    %ymm1, %ymm8, %ymm14                          #178.1128
        vfmadd213ps 832+__svml_sasin_ha_data_internal(%rip), %ymm4, %ymm0 #184.14
        vmovups   512+__svml_sasin_ha_data_internal(%rip), %ymm1 #178.1553
        vfmsub231ps %ymm2, %ymm8, %ymm9                         #178.1172
        vmulps    %ymm14, %ymm8, %ymm10                         #178.1227
        vmovups   640+__svml_sasin_ha_data_internal(%rip), %ymm8 #179.57
        vaddps    %ymm10, %ymm9, %ymm12                         #178.1312
        vfmadd213ps 704+__svml_sasin_ha_data_internal(%rip), %ymm4, %ymm8 #183.15
        vfmadd213ps 576+__svml_sasin_ha_data_internal(%rip), %ymm12, %ymm1 #178.1553
        vfmadd213ps %ymm0, %ymm15, %ymm8                        #187.15
        vfmadd213ps 896+__svml_sasin_ha_data_internal(%rip), %ymm4, %ymm8 #188.15
        vmovmskps %ymm11, %edx                                  #176.41
        vaddps    %ymm14, %ymm2, %ymm11                         #178.1269
        vmulps    %ymm8, %ymm4, %ymm0                           #189.15
        vmovups   960+__svml_sasin_ha_data_internal(%rip), %ymm4 #190.48
        vmulps    %ymm12, %ymm11, %ymm13                        #178.1614
        vsubps    %ymm2, %ymm4, %ymm10                          #192.15
        vfmsub213ps %ymm14, %ymm13, %ymm1                       #178.1658
        vsubps    %ymm10, %ymm4, %ymm4                          #194.14
        vsubps    %ymm2, %ymm1, %ymm8                           #193.13
        vaddps    1024+__svml_sasin_ha_data_internal(%rip), %ymm1, %ymm1 #195.14
        vsubps    %ymm4, %ymm2, %ymm2                           #196.14
        vsubps    %ymm2, %ymm1, %ymm9                           #197.14
        vandps    %ymm3, %ymm8, %ymm11                          #198.13
        vandps    %ymm3, %ymm9, %ymm13                          #199.14
        vandps    %ymm3, %ymm10, %ymm12                         #200.15
        vandnps   %ymm6, %ymm3, %ymm3                           #201.12
        vorps     %ymm3, %ymm11, %ymm6                          #202.13
        vfmadd213ps %ymm13, %ymm6, %ymm0                        #204.15
        vorps     %ymm3, %ymm12, %ymm1                          #203.15
        vaddps    %ymm0, %ymm1, %ymm0                           #205.15
        vxorps    %ymm5, %ymm0, %ymm0                           #206.14
        testl     %edx, %edx                                    #208.52
        jne       ..B1.3        # Prob 5%                       #208.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm7
..B1.2:                         # Preds ..B1.3 ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #211.12
        popq      %rbp                                          #211.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #211.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm7, 64(%rsp)                               #208.196
        vmovups   %ymm0, 128(%rsp)                              #208.270
        je        ..B1.2        # Prob 95%                      #208.374
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #208.454
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
        btl       %r12d, %r13d                                  #208.517
        jc        ..B1.10       # Prob 5%                       #208.517
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #208.470
        cmpl      $8, %r12d                                     #208.465
        jl        ..B1.7        # Prob 82%                      #208.465
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #208.675
        jmp       ..B1.2        # Prob 100%                     #208.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #208.546
        lea       128(%rsp,%r12,4), %rsi                        #208.546
..___tag_value___svml_asinf8_ha_l9.19:
#       __svml_sasin_ha_cout_rare_internal(const float *, float *)
        call      __svml_sasin_ha_cout_rare_internal            #208.546
..___tag_value___svml_asinf8_ha_l9.20:
        jmp       ..B1.8        # Prob 100%                     #208.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_asinf8_ha_l9,@function
	.size	__svml_asinf8_ha_l9,.-__svml_asinf8_ha_l9
..LN__svml_asinf8_ha_l9.0:
	.data
# -- End  __svml_asinf8_ha_l9
	.text
.L_2__routine_start___svml_asinf16_ha_z0_1:
# -- Begin  __svml_asinf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asinf16_ha_z0
# --- __svml_asinf16_ha_z0(__m512)
__svml_asinf16_ha_z0:
# parameter 1: %zmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #945.1
        .byte     15                                            #1019.12
        .byte     30                                            #1019.12
        .byte     250                                           #1019.12
	.cfi_startproc
..___tag_value___svml_asinf16_ha_z0.22:
..L23:
                                                         #945.1
        vmovups   __svml_sasin_ha_data_internal(%rip), %zmm5    #977.46
        vmovups   64+__svml_sasin_ha_data_internal(%rip), %zmm7 #978.46
        vmovups   192+__svml_sasin_ha_data_internal(%rip), %zmm10 #990.786
        vmovups   256+__svml_sasin_ha_data_internal(%rip), %zmm8 #979.42
        vandps    %zmm0, %zmm5, %zmm4                           #980.8
        vandnps   %zmm0, %zmm5, %zmm3                           #981.12
        vmovups   448+__svml_sasin_ha_data_internal(%rip), %zmm0 #990.1229
        vmovups   512+__svml_sasin_ha_data_internal(%rip), %zmm5 #990.2147
        vmulps    {rn-sae}, %zmm4, %zmm4, %zmm6                 #984.11
        vcmpps    $17, {sae}, %zmm4, %zmm8, %k1                 #982.13
        vcmpps    $21, {sae}, %zmm7, %zmm4, %k3                 #986.14
        vmovaps   %zmm7, %zmm11                                 #983.8
        vfnmadd231ps {rn-sae}, %zmm4, %zmm7, %zmm11             #983.8
        vrsqrt14ps %zmm11, %zmm9                                #990.580
        vcmpps    $17, {sae}, %zmm10, %zmm11, %k2               #990.847
        vminps    {sae}, %zmm11, %zmm6, %zmm2                   #985.11
        vmovups   640+__svml_sasin_ha_data_internal(%rip), %zmm10 #991.56
        vaddps    {rn-sae}, %zmm11, %zmm11, %zmm12              #990.1005
        vandps    384+__svml_sasin_ha_data_internal(%rip), %zmm9, %zmm15 #990.703
        vorps     1216+__svml_sasin_ha_data_internal(%rip), %zmm2, %zmm2{%k1} #989.11
        vandps    320+__svml_sasin_ha_data_internal(%rip), %zmm12, %zmm13 #990.1327
        vxorps    %zmm15, %zmm15, %zmm15{%k2}                   #990.933
        vmovups   768+__svml_sasin_ha_data_internal(%rip), %zmm11 #993.56
        vmulps    {rn-sae}, %zmm15, %zmm13, %zmm1               #990.1480
        vsubps    {rn-sae}, %zmm13, %zmm12, %zmm14              #990.1378
        vmulps    {rn-sae}, %zmm2, %zmm2, %zmm12                #997.15
        vmulps    {rn-sae}, %zmm15, %zmm14, %zmm9               #990.1582
        vfmsub231ps {rn-sae}, %zmm1, %zmm15, %zmm0              #990.1685
        vmovups   832+__svml_sasin_ha_data_internal(%rip), %zmm13 #996.14
        vmovups   896+__svml_sasin_ha_data_internal(%rip), %zmm14 #998.56
        vmulps    {rn-sae}, %zmm15, %zmm9, %zmm15               #990.1797
        vfmadd231ps {rn-sae}, %zmm2, %zmm11, %zmm13             #996.14
        vaddps    {rn-sae}, %zmm9, %zmm1, %zmm6                 #990.1900
        vaddps    {rn-sae}, %zmm15, %zmm0, %zmm7                #990.2004
        vmovups   576+__svml_sasin_ha_data_internal(%rip), %zmm0 #990.2302
        vmulps    {rn-sae}, %zmm7, %zmm6, %zmm8                 #990.2420
        vfmadd231ps {rn-sae}, %zmm7, %zmm5, %zmm0               #990.2302
        vmovups   704+__svml_sasin_ha_data_internal(%rip), %zmm5 #992.56
        vmovups   1088+__svml_sasin_ha_data_internal(%rip), %zmm6 #1004.46
        vfmsub213ps {rn-sae}, %zmm9, %zmm8, %zmm0               #990.2523
        vfmadd231ps {rn-sae}, %zmm2, %zmm10, %zmm5              #995.15
        vsubps    {rn-sae}, %zmm1, %zmm0, %zmm4{%k3}            #1006.14
        vfmadd213ps {rn-sae}, %zmm13, %zmm12, %zmm5             #999.15
        vfmadd213ps {rn-sae}, %zmm14, %zmm2, %zmm5              #1000.15
        vmulps    {rn-sae}, %zmm2, %zmm5, %zmm8                 #1001.17
        vmovups   960+__svml_sasin_ha_data_internal(%rip), %zmm2 #1002.47
        vmovups   1024+__svml_sasin_ha_data_internal(%rip), %zmm5 #1003.47
        vsubps    {rn-sae}, %zmm1, %zmm2, %zmm7                 #1005.17
        vaddps    {rn-sae}, %zmm0, %zmm5, %zmm0                 #1008.17
        vsubps    {rn-sae}, %zmm7, %zmm2, %zmm2                 #1007.16
        vsubps    {rn-sae}, %zmm2, %zmm1, %zmm1                 #1009.16
        vsubps    {rn-sae}, %zmm1, %zmm0, %zmm6{%k3}            #1010.16
        vfmadd213ps {rn-sae}, %zmm6, %zmm4, %zmm8               #1011.15
        vblendmps %zmm7, %zmm4, %zmm4{%k3}                      #1012.15
        vaddps    {rn-sae}, %zmm8, %zmm4, %zmm0                 #1013.17
        vxorps    %zmm3, %zmm0, %zmm0                           #1014.14
        ret                                                     #1019.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_asinf16_ha_z0,@function
	.size	__svml_asinf16_ha_z0,.-__svml_asinf16_ha_z0
..LN__svml_asinf16_ha_z0.1:
	.data
# -- End  __svml_asinf16_ha_z0
	.text
.L_2__routine_start___svml_asinf8_ha_e9_2:
# -- Begin  __svml_asinf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asinf8_ha_e9
# --- __svml_asinf8_ha_e9(__m256)
__svml_asinf8_ha_e9:
# parameter 1: %ymm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1024.1
        .byte     15                                            #1097.546
        .byte     30                                            #1097.546
        .byte     250                                           #1097.546
	.cfi_startproc
..___tag_value___svml_asinf8_ha_e9.25:
..L26:
                                                         #1024.1
        pushq     %rbp                                          #1024.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1024.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1024.1
        subq      $192, %rsp                                    #1024.1
        vmovups   __svml_sasin_ha_data_internal(%rip), %ymm11   #1055.47
        vpxor     %xmm3, %xmm3, %xmm3                           #1065.58
        vmovups   64+__svml_sasin_ha_data_internal(%rip), %ymm2 #1056.47
        vmovups   256+__svml_sasin_ha_data_internal(%rip), %ymm12 #1057.43
        vmovaps   %ymm0, %ymm10                                 #1024.1
        vandps    %ymm10, %ymm11, %ymm9                         #1058.8
        vmulps    %ymm2, %ymm9, %ymm13                          #1061.35
        vmulps    %ymm9, %ymm9, %ymm14                          #1062.9
        vcmplt_oqps %ymm9, %ymm12, %ymm15                       #1060.13
        vsubps    %ymm13, %ymm2, %ymm6                          #1061.8
        vandnps   %ymm10, %ymm11, %ymm8                         #1059.12
        vrsqrtps  %ymm6, %ymm11                                 #1067.494
        vminps    %ymm6, %ymm14, %ymm7                          #1063.9
        vcmplt_oqps 192+__svml_sasin_ha_data_internal(%rip), %ymm6, %ymm4 #1067.626
        vaddps    %ymm6, %ymm6, %ymm6                           #1067.735
        vmovups   320+__svml_sasin_ha_data_internal(%rip), %ymm14 #1067.814
        vandnps   %ymm11, %ymm4, %ymm4                          #1067.682
        vandps    %ymm14, %ymm6, %ymm12                         #1067.1089
        vsubps    %ymm12, %ymm6, %ymm13                         #1067.1138
        vextractf128 $1, %ymm15, %xmm0                          #1064.114
        vpackssdw %xmm0, %xmm15, %xmm5                          #1065.75
        vpacksswb %xmm3, %xmm5, %xmm1                           #1065.58
        vmulps    %ymm12, %ymm4, %ymm3                          #1067.1179
        vcmpnlt_uqps %ymm2, %ymm9, %ymm5                        #1066.14
        vpmovmskb %xmm1, %edx                                   #1065.39
        vmulps    %ymm13, %ymm4, %ymm2                          #1067.1220
        vandps    %ymm14, %ymm3, %ymm0                          #1067.1263
        vmulps    %ymm0, %ymm4, %ymm6                           #1067.1357
        vmulps    %ymm2, %ymm4, %ymm11                          #1067.1402
        vsubps    %ymm0, %ymm3, %ymm1                           #1067.1313
        vaddps    %ymm2, %ymm3, %ymm0                           #1067.1633
        vmulps    %ymm1, %ymm4, %ymm12                          #1067.1445
        vsubps    448+__svml_sasin_ha_data_internal(%rip), %ymm6, %ymm4 #1067.1491
        vaddps    %ymm4, %ymm11, %ymm13                         #1067.1539
        vaddps    %ymm13, %ymm12, %ymm15                        #1067.1587
        vmulps    512+__svml_sasin_ha_data_internal(%rip), %ymm15, %ymm14 #1067.1884
        vmulps    %ymm0, %ymm15, %ymm4                          #1067.1945
        vmulps    768+__svml_sasin_ha_data_internal(%rip), %ymm7, %ymm0 #1073.29
        vaddps    576+__svml_sasin_ha_data_internal(%rip), %ymm14, %ymm1 #1067.1869
        vmulps    %ymm4, %ymm1, %ymm6                           #1067.2004
        vmulps    %ymm7, %ymm7, %ymm4                           #1074.13
        vsubps    %ymm2, %ymm6, %ymm1                           #1067.1989
        vaddps    832+__svml_sasin_ha_data_internal(%rip), %ymm0, %ymm6 #1073.14
        vmulps    640+__svml_sasin_ha_data_internal(%rip), %ymm7, %ymm2 #1072.30
        vaddps    704+__svml_sasin_ha_data_internal(%rip), %ymm2, %ymm2 #1072.15
        vmulps    %ymm4, %ymm2, %ymm11                          #1076.30
        vsubps    %ymm3, %ymm1, %ymm2                           #1082.13
        vaddps    1024+__svml_sasin_ha_data_internal(%rip), %ymm1, %ymm1 #1084.14
        vaddps    %ymm11, %ymm6, %ymm12                         #1076.15
        vmulps    %ymm12, %ymm7, %ymm13                         #1077.30
        vaddps    896+__svml_sasin_ha_data_internal(%rip), %ymm13, %ymm14 #1077.15
        vmulps    %ymm14, %ymm7, %ymm0                          #1078.15
        vmovups   960+__svml_sasin_ha_data_internal(%rip), %ymm7 #1079.48
        vsubps    %ymm3, %ymm7, %ymm6                           #1081.15
        vsubps    %ymm6, %ymm7, %ymm7                           #1083.14
        vsubps    %ymm7, %ymm3, %ymm3                           #1085.14
        vsubps    %ymm3, %ymm1, %ymm4                           #1086.14
        vandps    %ymm5, %ymm2, %ymm11                          #1087.13
        vandnps   %ymm9, %ymm5, %ymm9                           #1090.12
        vandps    %ymm5, %ymm4, %ymm13                          #1088.14
        vandps    %ymm5, %ymm6, %ymm12                          #1089.15
        vorps     %ymm9, %ymm11, %ymm5                          #1091.13
        vmulps    %ymm0, %ymm5, %ymm0                           #1093.30
        vaddps    %ymm0, %ymm13, %ymm2                          #1093.15
        vorps     %ymm9, %ymm12, %ymm1                          #1092.15
        vaddps    %ymm2, %ymm1, %ymm3                           #1094.15
        vxorps    %ymm8, %ymm3, %ymm0                           #1095.14
        testb     %dl, %dl                                      #1097.52
        jne       ..B3.3        # Prob 5%                       #1097.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm10
..B3.2:                         # Preds ..B3.3 ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1100.12
        popq      %rbp                                          #1100.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1100.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm10, 64(%rsp)                              #1097.196
        vmovups   %ymm0, 128(%rsp)                              #1097.270
        testl     %edx, %edx                                    #1097.374
        je        ..B3.2        # Prob 95%                      #1097.374
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1097.454
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
        btl       %r12d, %r13d                                  #1097.517
        jc        ..B3.10       # Prob 5%                       #1097.517
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1097.470
        cmpl      $8, %r12d                                     #1097.465
        jl        ..B3.7        # Prob 82%                      #1097.465
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #1097.675
        jmp       ..B3.2        # Prob 100%                     #1097.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1097.546
        lea       128(%rsp,%r12,4), %rsi                        #1097.546
..___tag_value___svml_asinf8_ha_e9.43:
#       __svml_sasin_ha_cout_rare_internal(const float *, float *)
        call      __svml_sasin_ha_cout_rare_internal            #1097.546
..___tag_value___svml_asinf8_ha_e9.44:
        jmp       ..B3.8        # Prob 100%                     #1097.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_asinf8_ha_e9,@function
	.size	__svml_asinf8_ha_e9,.-__svml_asinf8_ha_e9
..LN__svml_asinf8_ha_e9.2:
	.data
# -- End  __svml_asinf8_ha_e9
	.text
.L_2__routine_start___svml_asinf4_ha_l9_3:
# -- Begin  __svml_asinf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asinf4_ha_l9
# --- __svml_asinf4_ha_l9(__m128)
__svml_asinf4_ha_l9:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1105.1
        .byte     15                                            #1178.540
        .byte     30                                            #1178.540
        .byte     250                                           #1178.540
	.cfi_startproc
..___tag_value___svml_asinf4_ha_l9.46:
..L47:
                                                         #1105.1
        pushq     %rbp                                          #1105.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1105.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1105.1
        subq      $192, %rsp                                    #1105.1
        vmovaps   %xmm0, %xmm7                                  #1105.1
        vmovups   __svml_sasin_ha_data_internal(%rip), %xmm8    #1136.44
        vmovups   64+__svml_sasin_ha_data_internal(%rip), %xmm12 #1137.44
        vandps    %xmm7, %xmm8, %xmm6                           #1139.8
        vmovaps   %xmm12, %xmm15                                #1142.8
        vandnps   %xmm7, %xmm8, %xmm5                           #1140.12
        vfnmadd231ps %xmm6, %xmm12, %xmm15                      #1142.8
        vmulps    %xmm6, %xmm6, %xmm10                          #1143.9
        vcmpnltps %xmm12, %xmm6, %xmm3                          #1147.14
        vrsqrtps  %xmm15, %xmm14                                #1148.494
        vcmpltps  192+__svml_sasin_ha_data_internal(%rip), %xmm15, %xmm13 #1148.620
        vminps    %xmm15, %xmm10, %xmm4                         #1144.9
        vaddps    %xmm15, %xmm15, %xmm0                         #1148.713
        vmulps    %xmm4, %xmm4, %xmm15                          #1155.13
        vandps    320+__svml_sasin_ha_data_internal(%rip), %xmm0, %xmm2 #1148.966
        vandnps   %xmm14, %xmm13, %xmm8                         #1148.663
        vmovups   256+__svml_sasin_ha_data_internal(%rip), %xmm9 #1138.40
        vsubps    %xmm2, %xmm0, %xmm1                           #1148.1012
        vmulps    %xmm2, %xmm8, %xmm2                           #1148.1050
        vcmpltps  %xmm6, %xmm9, %xmm11                          #1141.13
        vmulps    %xmm1, %xmm8, %xmm14                          #1148.1088
        vmovmskps %xmm11, %edx                                  #1146.40
        vmovups   448+__svml_sasin_ha_data_internal(%rip), %xmm9 #1148.871
        vaddps    %xmm14, %xmm2, %xmm11                         #1148.1220
        vfmsub231ps %xmm2, %xmm8, %xmm9                         #1148.1129
        vmulps    %xmm14, %xmm8, %xmm10                         #1148.1181
        vmovups   640+__svml_sasin_ha_data_internal(%rip), %xmm8 #1149.54
        vaddps    %xmm10, %xmm9, %xmm12                         #1148.1260
        vfmadd213ps 704+__svml_sasin_ha_data_internal(%rip), %xmm4, %xmm8 #1153.15
        vmovups   768+__svml_sasin_ha_data_internal(%rip), %xmm0 #1151.54
        vfmadd213ps 832+__svml_sasin_ha_data_internal(%rip), %xmm4, %xmm0 #1154.14
        vmovups   512+__svml_sasin_ha_data_internal(%rip), %xmm1 #1148.1492
        vfmadd213ps 576+__svml_sasin_ha_data_internal(%rip), %xmm12, %xmm1 #1148.1492
        vmulps    %xmm12, %xmm11, %xmm13                        #1148.1550
        vfmadd213ps %xmm0, %xmm15, %xmm8                        #1157.15
        vfmsub213ps %xmm14, %xmm13, %xmm1                       #1148.1591
        vfmadd213ps 896+__svml_sasin_ha_data_internal(%rip), %xmm4, %xmm8 #1158.15
        vmulps    %xmm8, %xmm4, %xmm0                           #1159.15
        vsubps    %xmm2, %xmm1, %xmm8                           #1163.13
        vaddps    1024+__svml_sasin_ha_data_internal(%rip), %xmm1, %xmm1 #1165.14
        vmovups   960+__svml_sasin_ha_data_internal(%rip), %xmm4 #1160.45
        vandps    %xmm3, %xmm8, %xmm11                          #1168.13
        vsubps    %xmm2, %xmm4, %xmm10                          #1162.15
        vandps    %xmm3, %xmm10, %xmm12                         #1170.15
        vsubps    %xmm10, %xmm4, %xmm4                          #1164.14
        vsubps    %xmm4, %xmm2, %xmm2                           #1166.14
        vsubps    %xmm2, %xmm1, %xmm9                           #1167.14
        vandps    %xmm3, %xmm9, %xmm13                          #1169.14
        vandnps   %xmm6, %xmm3, %xmm3                           #1171.12
        vorps     %xmm3, %xmm11, %xmm6                          #1172.13
        vorps     %xmm3, %xmm12, %xmm1                          #1173.15
        vfmadd213ps %xmm13, %xmm6, %xmm0                        #1174.15
        vaddps    %xmm0, %xmm1, %xmm0                           #1175.15
        vxorps    %xmm5, %xmm0, %xmm0                           #1176.14
        testl     %edx, %edx                                    #1178.52
        jne       ..B4.3        # Prob 5%                       #1178.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
..B4.2:                         # Preds ..B4.3 ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1181.12
        popq      %rbp                                          #1181.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1181.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm7, 64(%rsp)                               #1178.193
        vmovups   %xmm0, 128(%rsp)                              #1178.264
        je        ..B4.2        # Prob 95%                      #1178.368
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1178.448
        movq      %r12, 8(%rsp)                                 #1178.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1178.448
        movq      %r13, (%rsp)                                  #1178.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1178.448
                                # LOE rbx r12 r14 r15 r13d
..B4.7:                         # Preds ..B4.8 ..B4.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1178.511
        jc        ..B4.10       # Prob 5%                       #1178.511
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1178.464
        cmpl      $4, %r12d                                     #1178.459
        jl        ..B4.7        # Prob 82%                      #1178.459
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1178.666
        jmp       ..B4.2        # Prob 100%                     #1178.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1178.540
        lea       128(%rsp,%r12,4), %rsi                        #1178.540
..___tag_value___svml_asinf4_ha_l9.64:
#       __svml_sasin_ha_cout_rare_internal(const float *, float *)
        call      __svml_sasin_ha_cout_rare_internal            #1178.540
..___tag_value___svml_asinf4_ha_l9.65:
        jmp       ..B4.8        # Prob 100%                     #1178.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_asinf4_ha_l9,@function
	.size	__svml_asinf4_ha_l9,.-__svml_asinf4_ha_l9
..LN__svml_asinf4_ha_l9.3:
	.data
# -- End  __svml_asinf4_ha_l9
	.text
.L_2__routine_start___svml_asinf4_ha_e9_4:
# -- Begin  __svml_asinf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asinf4_ha_e9
# --- __svml_asinf4_ha_e9(__m128)
__svml_asinf4_ha_e9:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1186.1
        .byte     15                                            #1259.540
        .byte     30                                            #1259.540
        .byte     250                                           #1259.540
	.cfi_startproc
..___tag_value___svml_asinf4_ha_e9.67:
..L68:
                                                         #1186.1
        pushq     %rbp                                          #1186.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1186.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1186.1
        subq      $192, %rsp                                    #1186.1
        movaps    %xmm0, %xmm7                                  #1186.1
        movups    __svml_sasin_ha_data_internal(%rip), %xmm5    #1217.44
        movups    64+__svml_sasin_ha_data_internal(%rip), %xmm10 #1218.44
        movaps    %xmm5, %xmm6                                  #1220.8
        andps     %xmm7, %xmm6                                  #1220.8
        movaps    %xmm10, %xmm8                                 #1223.32
        mulps     %xmm6, %xmm8                                  #1223.32
        movaps    %xmm10, %xmm0                                 #1223.8
        movaps    %xmm6, %xmm4                                  #1224.9
        movaps    %xmm6, %xmm3                                  #1228.14
        mulps     %xmm6, %xmm4                                  #1224.9
        subps     %xmm8, %xmm0                                  #1223.8
        cmpnltps  %xmm10, %xmm3                                 #1228.14
        rsqrtps   %xmm0, %xmm11                                 #1229.494
        minps     %xmm0, %xmm4                                  #1225.9
        movaps    %xmm0, %xmm8                                  #1229.620
        andnps    %xmm7, %xmm5                                  #1221.12
        cmpltps   192+__svml_sasin_ha_data_internal(%rip), %xmm8 #1229.620
        addps     %xmm0, %xmm0                                  #1229.713
        movups    320+__svml_sasin_ha_data_internal(%rip), %xmm12 #1229.786
        andnps    %xmm11, %xmm8                                 #1229.663
        movaps    %xmm12, %xmm2                                 #1229.1058
        movaps    %xmm8, %xmm14                                 #1229.1350
        andps     %xmm0, %xmm2                                  #1229.1058
        movups    512+__svml_sasin_ha_data_internal(%rip), %xmm1 #1229.1805
        subps     %xmm2, %xmm0                                  #1229.1104
        mulps     %xmm8, %xmm2                                  #1229.1142
        mulps     %xmm8, %xmm0                                  #1229.1180
        andps     %xmm2, %xmm12                                 #1229.1220
        movaps    %xmm2, %xmm13                                 #1229.1267
        mulps     %xmm0, %xmm14                                 #1229.1350
        subps     %xmm12, %xmm13                                #1229.1267
        mulps     %xmm8, %xmm12                                 #1229.1308
        mulps     %xmm13, %xmm8                                 #1229.1390
        subps     448+__svml_sasin_ha_data_internal(%rip), %xmm12 #1229.1433
        movaps    %xmm2, %xmm15                                 #1229.1566
        addps     %xmm12, %xmm14                                #1229.1478
        addps     %xmm0, %xmm15                                 #1229.1566
        addps     %xmm14, %xmm8                                 #1229.1523
        mulps     %xmm8, %xmm1                                  #1229.1805
        mulps     %xmm15, %xmm8                                 #1229.1863
        addps     576+__svml_sasin_ha_data_internal(%rip), %xmm1 #1229.1793
        movups    256+__svml_sasin_ha_data_internal(%rip), %xmm9 #1219.40
        cmpltps   %xmm6, %xmm9                                  #1222.13
        mulps     %xmm8, %xmm1                                  #1229.1916
        movmskps  %xmm9, %edx                                   #1227.40
        movups    640+__svml_sasin_ha_data_internal(%rip), %xmm9 #1234.27
        subps     %xmm0, %xmm1                                  #1229.1904
        mulps     %xmm4, %xmm9                                  #1234.27
        movups    768+__svml_sasin_ha_data_internal(%rip), %xmm10 #1235.26
        movaps    %xmm4, %xmm0                                  #1236.13
        mulps     %xmm4, %xmm10                                 #1235.26
        addps     704+__svml_sasin_ha_data_internal(%rip), %xmm9 #1234.15
        mulps     %xmm4, %xmm0                                  #1236.13
        addps     832+__svml_sasin_ha_data_internal(%rip), %xmm10 #1235.14
        mulps     %xmm0, %xmm9                                  #1238.27
        movups    960+__svml_sasin_ha_data_internal(%rip), %xmm11 #1241.45
        movaps    %xmm1, %xmm12                                 #1244.13
        movaps    %xmm11, %xmm0                                 #1243.15
        addps     %xmm9, %xmm10                                 #1238.15
        subps     %xmm2, %xmm0                                  #1243.15
        subps     %xmm2, %xmm12                                 #1244.13
        mulps     %xmm4, %xmm10                                 #1239.27
        subps     %xmm0, %xmm11                                 #1245.14
        addps     896+__svml_sasin_ha_data_internal(%rip), %xmm10 #1239.15
        subps     %xmm11, %xmm2                                 #1247.14
        mulps     %xmm10, %xmm4                                 #1240.15
        movups    1024+__svml_sasin_ha_data_internal(%rip), %xmm13 #1246.14
        andps     %xmm3, %xmm12                                 #1249.13
        andps     %xmm3, %xmm0                                  #1251.15
        addps     %xmm1, %xmm13                                 #1246.14
        subps     %xmm2, %xmm13                                 #1248.14
        andps     %xmm3, %xmm13                                 #1250.14
        andnps    %xmm6, %xmm3                                  #1252.12
        orps      %xmm3, %xmm12                                 #1253.13
        orps      %xmm3, %xmm0                                  #1254.15
        mulps     %xmm4, %xmm12                                 #1255.27
        addps     %xmm12, %xmm13                                #1255.15
        addps     %xmm13, %xmm0                                 #1256.15
        pxor      %xmm5, %xmm0                                  #1257.14
        testl     %edx, %edx                                    #1259.52
        jne       ..B5.3        # Prob 5%                       #1259.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
..B5.2:                         # Preds ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1262.12
        popq      %rbp                                          #1262.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1262.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm7, 64(%rsp)                               #1259.193
        movups    %xmm0, 128(%rsp)                              #1259.264
                                # LOE rbx r12 r13 r14 r15 edx
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1259.448
        movq      %r12, 8(%rsp)                                 #1259.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1259.448
        movq      %r13, (%rsp)                                  #1259.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1259.448
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1259.511
        jc        ..B5.10       # Prob 5%                       #1259.511
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1259.464
        cmpl      $4, %r12d                                     #1259.459
        jl        ..B5.7        # Prob 82%                      #1259.459
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1259.666
        jmp       ..B5.2        # Prob 100%                     #1259.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1259.540
        lea       128(%rsp,%r12,4), %rsi                        #1259.540
..___tag_value___svml_asinf4_ha_e9.85:
#       __svml_sasin_ha_cout_rare_internal(const float *, float *)
        call      __svml_sasin_ha_cout_rare_internal            #1259.540
..___tag_value___svml_asinf4_ha_e9.86:
        jmp       ..B5.8        # Prob 100%                     #1259.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_asinf4_ha_e9,@function
	.size	__svml_asinf4_ha_e9,.-__svml_asinf4_ha_e9
..LN__svml_asinf4_ha_e9.4:
	.data
# -- End  __svml_asinf4_ha_e9
	.text
.L_2__routine_start___svml_asinf4_ha_ex_5:
# -- Begin  __svml_asinf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asinf4_ha_ex
# --- __svml_asinf4_ha_ex(__m128)
__svml_asinf4_ha_ex:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1267.1
        .byte     15                                            #1340.540
        .byte     30                                            #1340.540
        .byte     250                                           #1340.540
	.cfi_startproc
..___tag_value___svml_asinf4_ha_ex.88:
..L89:
                                                         #1267.1
        pushq     %rbp                                          #1267.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1267.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1267.1
        subq      $192, %rsp                                    #1267.1
        movaps    %xmm0, %xmm7                                  #1267.1
        movups    __svml_sasin_ha_data_internal(%rip), %xmm5    #1298.44
        movups    64+__svml_sasin_ha_data_internal(%rip), %xmm10 #1299.44
        movaps    %xmm5, %xmm6                                  #1301.8
        andps     %xmm7, %xmm6                                  #1301.8
        movaps    %xmm10, %xmm8                                 #1304.32
        mulps     %xmm6, %xmm8                                  #1304.32
        movaps    %xmm10, %xmm0                                 #1304.8
        movaps    %xmm6, %xmm4                                  #1305.9
        movaps    %xmm6, %xmm3                                  #1309.14
        mulps     %xmm6, %xmm4                                  #1305.9
        subps     %xmm8, %xmm0                                  #1304.8
        cmpnltps  %xmm10, %xmm3                                 #1309.14
        rsqrtps   %xmm0, %xmm11                                 #1310.494
        minps     %xmm0, %xmm4                                  #1306.9
        movaps    %xmm0, %xmm8                                  #1310.620
        andnps    %xmm7, %xmm5                                  #1302.12
        cmpltps   192+__svml_sasin_ha_data_internal(%rip), %xmm8 #1310.620
        addps     %xmm0, %xmm0                                  #1310.713
        movups    320+__svml_sasin_ha_data_internal(%rip), %xmm12 #1310.786
        andnps    %xmm11, %xmm8                                 #1310.663
        movaps    %xmm12, %xmm2                                 #1310.1058
        movaps    %xmm8, %xmm14                                 #1310.1350
        andps     %xmm0, %xmm2                                  #1310.1058
        movups    512+__svml_sasin_ha_data_internal(%rip), %xmm1 #1310.1805
        subps     %xmm2, %xmm0                                  #1310.1104
        mulps     %xmm8, %xmm2                                  #1310.1142
        mulps     %xmm8, %xmm0                                  #1310.1180
        andps     %xmm2, %xmm12                                 #1310.1220
        movaps    %xmm2, %xmm13                                 #1310.1267
        mulps     %xmm0, %xmm14                                 #1310.1350
        subps     %xmm12, %xmm13                                #1310.1267
        mulps     %xmm8, %xmm12                                 #1310.1308
        mulps     %xmm13, %xmm8                                 #1310.1390
        subps     448+__svml_sasin_ha_data_internal(%rip), %xmm12 #1310.1433
        movaps    %xmm2, %xmm15                                 #1310.1566
        addps     %xmm12, %xmm14                                #1310.1478
        addps     %xmm0, %xmm15                                 #1310.1566
        addps     %xmm14, %xmm8                                 #1310.1523
        mulps     %xmm8, %xmm1                                  #1310.1805
        mulps     %xmm15, %xmm8                                 #1310.1863
        addps     576+__svml_sasin_ha_data_internal(%rip), %xmm1 #1310.1793
        movups    256+__svml_sasin_ha_data_internal(%rip), %xmm9 #1300.40
        cmpltps   %xmm6, %xmm9                                  #1303.13
        mulps     %xmm8, %xmm1                                  #1310.1916
        movmskps  %xmm9, %edx                                   #1308.40
        movups    640+__svml_sasin_ha_data_internal(%rip), %xmm9 #1315.27
        subps     %xmm0, %xmm1                                  #1310.1904
        mulps     %xmm4, %xmm9                                  #1315.27
        movups    768+__svml_sasin_ha_data_internal(%rip), %xmm10 #1316.26
        movaps    %xmm4, %xmm0                                  #1317.13
        mulps     %xmm4, %xmm10                                 #1316.26
        addps     704+__svml_sasin_ha_data_internal(%rip), %xmm9 #1315.15
        mulps     %xmm4, %xmm0                                  #1317.13
        addps     832+__svml_sasin_ha_data_internal(%rip), %xmm10 #1316.14
        mulps     %xmm0, %xmm9                                  #1319.27
        movups    960+__svml_sasin_ha_data_internal(%rip), %xmm11 #1322.45
        movaps    %xmm1, %xmm12                                 #1325.13
        movaps    %xmm11, %xmm0                                 #1324.15
        addps     %xmm9, %xmm10                                 #1319.15
        subps     %xmm2, %xmm0                                  #1324.15
        subps     %xmm2, %xmm12                                 #1325.13
        mulps     %xmm4, %xmm10                                 #1320.27
        subps     %xmm0, %xmm11                                 #1326.14
        addps     896+__svml_sasin_ha_data_internal(%rip), %xmm10 #1320.15
        subps     %xmm11, %xmm2                                 #1328.14
        mulps     %xmm10, %xmm4                                 #1321.15
        movups    1024+__svml_sasin_ha_data_internal(%rip), %xmm13 #1327.14
        andps     %xmm3, %xmm12                                 #1330.13
        andps     %xmm3, %xmm0                                  #1332.15
        addps     %xmm1, %xmm13                                 #1327.14
        subps     %xmm2, %xmm13                                 #1329.14
        andps     %xmm3, %xmm13                                 #1331.14
        andnps    %xmm6, %xmm3                                  #1333.12
        orps      %xmm3, %xmm12                                 #1334.13
        orps      %xmm3, %xmm0                                  #1335.15
        mulps     %xmm4, %xmm12                                 #1336.27
        addps     %xmm12, %xmm13                                #1336.15
        addps     %xmm13, %xmm0                                 #1337.15
        pxor      %xmm5, %xmm0                                  #1338.14
        testl     %edx, %edx                                    #1340.52
        jne       ..B6.3        # Prob 5%                       #1340.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
..B6.2:                         # Preds ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1343.12
        popq      %rbp                                          #1343.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1343.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm7, 64(%rsp)                               #1340.193
        movups    %xmm0, 128(%rsp)                              #1340.264
                                # LOE rbx r12 r13 r14 r15 edx
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1340.448
        movq      %r12, 8(%rsp)                                 #1340.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1340.448
        movq      %r13, (%rsp)                                  #1340.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1340.448
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1340.511
        jc        ..B6.10       # Prob 5%                       #1340.511
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1340.464
        cmpl      $4, %r12d                                     #1340.459
        jl        ..B6.7        # Prob 82%                      #1340.459
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1340.666
        jmp       ..B6.2        # Prob 100%                     #1340.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1340.540
        lea       128(%rsp,%r12,4), %rsi                        #1340.540
..___tag_value___svml_asinf4_ha_ex.106:
#       __svml_sasin_ha_cout_rare_internal(const float *, float *)
        call      __svml_sasin_ha_cout_rare_internal            #1340.540
..___tag_value___svml_asinf4_ha_ex.107:
        jmp       ..B6.8        # Prob 100%                     #1340.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_asinf4_ha_ex,@function
	.size	__svml_asinf4_ha_ex,.-__svml_asinf4_ha_ex
..LN__svml_asinf4_ha_ex.5:
	.data
# -- End  __svml_asinf4_ha_ex
	.text
.L_2__routine_start___svml_sasin_ha_cout_rare_internal_6:
# -- Begin  __svml_sasin_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_sasin_ha_cout_rare_internal
	.globl __svml_sasin_ha_cout_rare_internal
# --- __svml_sasin_ha_cout_rare_internal(const float *, float *)
__svml_sasin_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #780.1
        .byte     15                                            #937.17
        .byte     30                                            #937.17
        .byte     250                                           #937.17
	.cfi_startproc
..___tag_value___svml_sasin_ha_cout_rare_internal.109:
..L110:
                                                        #780.1
        xorl      %eax, %eax                                    #793.14
        movzwl    2(%rdi), %edx                                 #794.35
        andl      $32640, %edx                                  #794.35
        cmpl      $32640, %edx                                  #794.59
        je        ..B7.12       # Prob 16%                      #794.59
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B7.2:                         # Preds ..B7.1
                                # Execution count [8.40e-01]
        pxor      %xmm0, %xmm0                                  #796.23
        cvtss2sd  (%rdi), %xmm0                                 #796.23
        movsd     %xmm0, -32(%rsp)                              #796.13
        andb      $127, -25(%rsp)                               #797.34
        movsd     -32(%rsp), %xmm12                             #798.18
        movsd     4160+_vmldASinHATab(%rip), %xmm0              #798.45
        comisd    %xmm12, %xmm0                                 #798.45
        jb        ..B7.10       # Prob 50%                      #798.45
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm12
..B7.3:                         # Preds ..B7.2
                                # Execution count [4.20e-01]
        movsd     4168+_vmldASinHATab(%rip), %xmm1              #800.48
        comisd    %xmm12, %xmm1                                 #800.48
        jbe       ..B7.9        # Prob 50%                      #800.48
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm1 xmm12
..B7.4:                         # Preds ..B7.3
                                # Execution count [2.10e-01]
        comisd    4136+_vmldASinHATab(%rip), %xmm12             #802.52
        jbe       ..B7.6        # Prob 50%                      #802.52
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm12
..B7.5:                         # Preds ..B7.4
                                # Execution count [1.05e-01]
        movsd     4104+_vmldASinHATab(%rip), %xmm13             #804.68
        movaps    %xmm12, %xmm5                                 #804.174
        mulsd     %xmm12, %xmm13                                #804.68
        movaps    %xmm12, %xmm7                                 #806.44
        movsd     %xmm13, -48(%rsp)                             #804.25
        movsd     -48(%rsp), %xmm14                             #804.107
        movsd     4104+_vmldASinHATab(%rip), %xmm1              #811.68
        subsd     -32(%rsp), %xmm14                             #804.112
        movsd     %xmm14, -56(%rsp)                             #804.93
        movsd     -48(%rsp), %xmm8                              #804.138
        movsd     -56(%rsp), %xmm15                             #804.143
        subsd     %xmm15, %xmm8                                 #804.143
        movsd     %xmm8, -48(%rsp)                              #804.124
        movsd     -48(%rsp), %xmm6                              #804.174
        movsd     4296+_vmldASinHATab(%rip), %xmm8              #808.81
        subsd     %xmm6, %xmm5                                  #804.174
        movsd     %xmm5, -56(%rsp)                              #804.149
        movaps    %xmm12, %xmm5                                 #813.48
        movsd     -48(%rsp), %xmm9                              #804.188
        movaps    %xmm9, %xmm13                                 #805.40
        addsd     %xmm9, %xmm7                                  #806.44
        mulsd     %xmm9, %xmm13                                 #805.40
        movsd     -56(%rsp), %xmm11                             #804.200
        movaps    %xmm13, %xmm10                                #807.39
        mulsd     %xmm11, %xmm7                                 #806.53
        mulsd     %xmm13, %xmm1                                 #811.68
        addsd     %xmm7, %xmm10                                 #807.39
        mulsd     %xmm10, %xmm8                                 #808.81
        movsd     %xmm1, -48(%rsp)                              #811.25
        movsd     -48(%rsp), %xmm2                              #811.107
        movsd     4104+_vmldASinHATab(%rip), %xmm1              #814.67
        subsd     %xmm13, %xmm2                                 #811.112
        addsd     4288+_vmldASinHATab(%rip), %xmm8              #808.103
        mulsd     %xmm10, %xmm8                                 #808.125
        movsd     %xmm2, -56(%rsp)                              #811.93
        movsd     -48(%rsp), %xmm3                              #811.138
        movsd     -56(%rsp), %xmm0                              #811.143
        addsd     4280+_vmldASinHATab(%rip), %xmm8              #808.147
        subsd     %xmm0, %xmm3                                  #811.143
        mulsd     %xmm10, %xmm8                                 #808.169
        movsd     %xmm3, -48(%rsp)                              #811.124
        movsd     -48(%rsp), %xmm4                              #811.174
        addsd     4272+_vmldASinHATab(%rip), %xmm8              #808.191
        subsd     %xmm4, %xmm13                                 #811.174
        mulsd     %xmm10, %xmm8                                 #808.213
        movsd     %xmm13, -56(%rsp)                             #811.149
        movsd     -48(%rsp), %xmm6                              #811.188
        mulsd     %xmm6, %xmm9                                  #812.39
        addsd     4264+_vmldASinHATab(%rip), %xmm8              #809.49
        mulsd     %xmm6, %xmm11                                 #813.63
        mulsd     %xmm10, %xmm8                                 #809.71
        mulsd     %xmm9, %xmm1                                  #814.67
        addsd     4256+_vmldASinHATab(%rip), %xmm8              #809.93
        mulsd     %xmm10, %xmm8                                 #809.115
        movaps    %xmm9, %xmm3                                  #814.171
        movsd     -56(%rsp), %xmm14                             #811.200
        movaps    %xmm6, %xmm4                                  #815.39
        movsd     %xmm1, -48(%rsp)                              #814.25
        addsd     %xmm14, %xmm7                                 #813.40
        addsd     4248+_vmldASinHATab(%rip), %xmm8              #809.137
        mulsd     %xmm7, %xmm5                                  #813.48
        mulsd     %xmm10, %xmm8                                 #809.159
        addsd     %xmm5, %xmm11                                 #813.63
        addsd     4240+_vmldASinHATab(%rip), %xmm8              #809.181
        movsd     -48(%rsp), %xmm2                              #814.106
        mulsd     %xmm10, %xmm8                                 #809.203
        subsd     %xmm9, %xmm2                                  #814.111
        addsd     %xmm11, %xmm9                                 #816.77
        addsd     4232+_vmldASinHATab(%rip), %xmm8              #809.225
        mulsd     %xmm9, %xmm7                                  #816.77
        mulsd     %xmm10, %xmm8                                 #809.247
        movsd     %xmm2, -56(%rsp)                              #814.92
        movsd     -48(%rsp), %xmm0                              #814.136
        movsd     -56(%rsp), %xmm15                             #814.141
        subsd     %xmm15, %xmm0                                 #814.141
        addsd     4224+_vmldASinHATab(%rip), %xmm8              #810.49
        movsd     %xmm0, -48(%rsp)                              #814.122
        movsd     -48(%rsp), %xmm5                              #814.171
        mulsd     %xmm10, %xmm8                                 #810.71
        subsd     %xmm5, %xmm3                                  #814.171
        movsd     %xmm3, -56(%rsp)                              #814.147
        movsd     -48(%rsp), %xmm3                              #814.185
        movsd     -56(%rsp), %xmm5                              #814.197
        movaps    %xmm5, %xmm13                                 #816.46
        addsd     4216+_vmldASinHATab(%rip), %xmm8              #810.93
        mulsd     %xmm3, %xmm4                                  #815.39
        addsd     %xmm11, %xmm13                                #816.46
        mulsd     %xmm10, %xmm8                                 #810.115
        mulsd     %xmm13, %xmm6                                 #816.46
        addsd     4208+_vmldASinHATab(%rip), %xmm8              #810.137
        addsd     %xmm7, %xmm6                                  #816.77
        mulsd     %xmm10, %xmm8                                 #818.25
        movsd     4104+_vmldASinHATab(%rip), %xmm7              #817.67
        movaps    %xmm4, %xmm13                                 #817.171
        mulsd     %xmm4, %xmm7                                  #817.67
        addsd     4200+_vmldASinHATab(%rip), %xmm8              #820.25
        movsd     %xmm7, -48(%rsp)                              #817.25
        movsd     -48(%rsp), %xmm1                              #817.106
        movsd     4184+_vmldASinHATab(%rip), %xmm10             #819.80
        subsd     %xmm4, %xmm1                                  #817.111
        mulsd     %xmm9, %xmm10                                 #819.80
        addsd     %xmm6, %xmm4                                  #819.134
        movsd     %xmm1, -56(%rsp)                              #817.92
        movaps    %xmm12, %xmm9                                 #823.50
        movsd     -48(%rsp), %xmm0                              #817.136
        movsd     -56(%rsp), %xmm2                              #817.141
        movsd     4176+_vmldASinHATab(%rip), %xmm1              #820.59
        subsd     %xmm2, %xmm0                                  #817.141
        mulsd     %xmm1, %xmm3                                  #821.55
        mulsd     %xmm1, %xmm11                                 #820.59
        mulsd     %xmm4, %xmm8                                  #820.25
        addsd     %xmm3, %xmm9                                  #823.50
        mulsd     %xmm1, %xmm5                                  #822.59
        addsd     %xmm11, %xmm10                                #820.25
        movsd     %xmm0, -48(%rsp)                              #817.122
        addsd     %xmm10, %xmm8                                 #820.25
        movsd     -48(%rsp), %xmm7                              #817.171
        addsd     %xmm5, %xmm8                                  #822.25
        subsd     %xmm7, %xmm13                                 #817.171
        movsd     %xmm13, -56(%rsp)                             #817.147
        movsd     -48(%rsp), %xmm0                              #817.185
        movsd     -56(%rsp), %xmm2                              #817.197
        movsd     %xmm9, -48(%rsp)                              #823.25
        addsd     %xmm2, %xmm6                                  #831.43
        movsd     -48(%rsp), %xmm11                             #823.88
        movsd     4192+_vmldASinHATab(%rip), %xmm4              #825.59
        subsd     %xmm11, %xmm12                                #823.88
        mulsd     %xmm4, %xmm0                                  #826.55
        mulsd     %xmm4, %xmm6                                  #831.43
        addsd     %xmm12, %xmm3                                 #823.116
        movsd     %xmm3, -56(%rsp)                              #823.94
        movsd     -48(%rsp), %xmm3                              #823.133
        movaps    %xmm3, %xmm12                                 #828.48
        movsd     -56(%rsp), %xmm7                              #823.144
        addsd     %xmm0, %xmm12                                 #828.48
        addsd     %xmm7, %xmm8                                  #824.25
        movsd     %xmm12, -48(%rsp)                             #828.25
        movsd     -48(%rsp), %xmm12                             #828.84
        subsd     %xmm12, %xmm3                                 #828.84
        addsd     %xmm3, %xmm0                                  #828.112
        movsd     %xmm0, -56(%rsp)                              #828.90
        movsd     -48(%rsp), %xmm9                              #828.129
        movsd     -56(%rsp), %xmm0                              #828.140
        movb      3(%rdi), %dil                                 #832.86
        addsd     %xmm0, %xmm8                                  #830.25
        andb      $-128, %dil                                   #832.46
        addsd     %xmm8, %xmm9                                  #831.43
        addsd     %xmm9, %xmm6                                  #831.43
        movsd     %xmm6, -40(%rsp)                              #831.25
        movb      -33(%rsp), %dl                                #832.46
        andb      $127, %dl                                     #832.46
        orb       %dil, %dl                                     #832.46
        movb      %dl, -33(%rsp)                                #832.46
        movsd     -40(%rsp), %xmm1                              #833.26
        cvtsd2ss  %xmm1, %xmm1                                  #833.26
        movss     %xmm1, (%rsi)                                 #833.26
        ret                                                     #833.26
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.6:                         # Preds ..B7.4
                                # Execution count [1.05e-01]
        comisd    4128+_vmldASinHATab(%rip), %xmm12             #837.57
        jb        ..B7.8        # Prob 50%                      #837.57
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm12
..B7.7:                         # Preds ..B7.6
                                # Execution count [5.25e-02]
        addsd     %xmm0, %xmm12                                 #839.74
        movsd     %xmm12, -48(%rsp)                             #839.29
        movsd     -48(%rsp), %xmm0                              #840.46
        mulsd     -32(%rsp), %xmm0                              #840.46
        movsd     %xmm0, -40(%rsp)                              #840.29
        movb      -33(%rsp), %dl                                #841.50
        movb      3(%rdi), %dil                                 #841.90
        andb      $127, %dl                                     #841.50
        andb      $-128, %dil                                   #841.50
        orb       %dil, %dl                                     #841.50
        movb      %dl, -33(%rsp)                                #841.50
        movsd     -40(%rsp), %xmm1                              #842.30
        cvtsd2ss  %xmm1, %xmm1                                  #842.30
        movss     %xmm1, (%rsi)                                 #842.30
        ret                                                     #842.30
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.8:                         # Preds ..B7.6
                                # Execution count [5.25e-02]
        mulsd     %xmm12, %xmm12                                #846.41
        movsd     %xmm12, -48(%rsp)                             #846.29
        movsd     -48(%rsp), %xmm0                              #847.46
        movb      3(%rdi), %dil                                 #848.90
        andb      $-128, %dil                                   #848.50
        addsd     -32(%rsp), %xmm0                              #847.46
        movsd     %xmm0, -40(%rsp)                              #847.29
        movb      -33(%rsp), %dl                                #848.50
        andb      $127, %dl                                     #848.50
        orb       %dil, %dl                                     #848.50
        movb      %dl, -33(%rsp)                                #848.50
        movsd     -40(%rsp), %xmm1                              #849.30
        cvtsd2ss  %xmm1, %xmm1                                  #849.30
        movss     %xmm1, (%rsi)                                 #849.30
        ret                                                     #849.30
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.9:                         # Preds ..B7.3
                                # Execution count [2.10e-01]
        movaps    %xmm0, %xmm9                                  #855.66
        movzwl    4166+_vmldASinHATab(%rip), %r9d               #859.42
        pxor      %xmm15, %xmm15                                #882.56
        andl      $-32753, %r9d                                 #859.42
        subsd     %xmm12, %xmm9                                 #855.66
        subsd     %xmm0, %xmm15                                 #882.56
        mulsd     %xmm9, %xmm1                                  #856.45
        movsd     %xmm1, -32(%rsp)                              #856.21
        movzwl    -26(%rsp), %ecx                               #857.48
        andl      $32752, %ecx                                  #857.48
        shrl      $4, %ecx                                      #857.48
        addl      $-1023, %ecx                                  #857.70
        movl      %ecx, %r8d                                    #859.81
        movl      %ecx, %edx                                    #861.32
        negl      %r8d                                          #859.81
        addl      $1023, %r8d                                   #859.81
        andl      $2047, %r8d                                   #859.42
        shll      $4, %r8d                                      #859.42
        movsd     %xmm0, -24(%rsp)                              #858.21
        orl       %r8d, %r9d                                    #859.42
        movw      %r9w, -18(%rsp)                               #859.42
        andl      $1, %edx                                      #861.32
        movsd     -24(%rsp), %xmm10                             #860.33
        movl      %edx, %r10d                                   #878.40
        mulsd     %xmm10, %xmm1                                 #860.33
        lea       _vmldASinHATab(%rip), %r8                     #879.45
        movaps    %xmm1, %xmm6                                  #865.39
        movsd     4112+_vmldASinHATab(%rip), %xmm7              #871.53
        addsd     %xmm1, %xmm6                                  #865.39
        jne       ..L111        # Prob 50%                      #869.25
        movaps    %xmm1, %xmm6                                  #869.25
..L111:                                                         #
        mulsd     %xmm6, %xmm7                                  #871.53
        movaps    %xmm6, %xmm8                                  #874.36
        movaps    %xmm7, %xmm11                                 #872.37
        subl      %edx, %ecx                                    #862.27
        movsd     4120+_vmldASinHATab(%rip), %xmm13             #876.54
        subsd     %xmm6, %xmm11                                 #872.37
        addsd     %xmm1, %xmm13                                 #876.54
        movsd     %xmm11, -56(%rsp)                             #872.21
        movsd     -56(%rsp), %xmm12                             #873.37
        movsd     %xmm13, -16(%rsp)                             #876.21
        subsd     %xmm12, %xmm7                                 #873.37
        movl      -16(%rsp), %r11d                              #877.48
        movaps    %xmm7, %xmm4                                  #884.36
        shll      $8, %r10d                                     #878.40
        andl      $511, %r11d                                   #877.80
        addl      %r10d, %r11d                                  #878.40
        subsd     %xmm7, %xmm8                                  #874.36
        movsd     (%r8,%r11,8), %xmm3                           #879.45
        addsd     %xmm8, %xmm4                                  #884.36
        mulsd     %xmm3, %xmm7                                  #889.40
        movaps    %xmm3, %xmm14                                 #880.35
        mulsd     %xmm3, %xmm14                                 #880.35
        mulsd     %xmm14, %xmm4                                 #884.36
        movsd     4496+_vmldASinHATab(%rip), %xmm5              #885.71
        addsd     %xmm15, %xmm4                                 #884.36
        mulsd     %xmm4, %xmm5                                  #885.71
        shrl      $1, %ecx                                      #862.38
        addsd     4488+_vmldASinHATab(%rip), %xmm5              #885.94
        mulsd     %xmm4, %xmm5                                  #885.116
        addl      $1023, %ecx                                   #888.73
        andl      $2047, %ecx                                   #888.42
        addsd     4480+_vmldASinHATab(%rip), %xmm5              #885.139
        mulsd     %xmm4, %xmm5                                  #885.161
        movzwl    4166+_vmldASinHATab(%rip), %r9d               #888.42
        shll      $4, %ecx                                      #888.42
        andl      $-32753, %r9d                                 #888.42
        movsd     %xmm0, -8(%rsp)                               #887.21
        orl       %ecx, %r9d                                    #888.42
        movw      %r9w, -2(%rsp)                                #888.42
        movsd     -8(%rsp), %xmm12                              #890.41
        mulsd     %xmm12, %xmm7                                 #890.41
        addsd     4472+_vmldASinHATab(%rip), %xmm5              #885.184
        mulsd     %xmm4, %xmm5                                  #885.206
        movsd     4104+_vmldASinHATab(%rip), %xmm1              #898.66
        mulsd     %xmm7, %xmm1                                  #898.66
        addsd     4464+_vmldASinHATab(%rip), %xmm5              #886.57
        mulsd     %xmm4, %xmm5                                  #886.79
        movsd     %xmm1, -48(%rsp)                              #898.21
        movsd     -48(%rsp), %xmm0                              #898.105
        movsd     4336+_vmldASinHATab(%rip), %xmm13             #904.256
        subsd     %xmm7, %xmm0                                  #898.110
        addsd     4456+_vmldASinHATab(%rip), %xmm5              #886.102
        addsd     4344+_vmldASinHATab(%rip), %xmm13             #904.256
        mulsd     %xmm4, %xmm5                                  #886.124
        mulsd     %xmm9, %xmm13                                 #904.278
        addsd     4448+_vmldASinHATab(%rip), %xmm5              #886.147
        mulsd     %xmm4, %xmm5                                  #891.39
        mulsd     %xmm6, %xmm5                                  #894.41
        movsd     %xmm0, -56(%rsp)                              #898.91
        addsd     %xmm5, %xmm8                                  #896.41
        movsd     4440+_vmldASinHATab(%rip), %xmm5              #901.77
        mulsd     %xmm9, %xmm5                                  #901.77
        mulsd     %xmm3, %xmm8                                  #896.41
        addsd     4432+_vmldASinHATab(%rip), %xmm5              #901.98
        mulsd     %xmm12, %xmm8                                 #897.41
        mulsd     %xmm9, %xmm5                                  #901.120
        movsd     -48(%rsp), %xmm10                             #898.138
        movsd     -56(%rsp), %xmm2                              #898.143
        movsd     4320+_vmldASinHATab(%rip), %xmm14             #904.176
        subsd     %xmm2, %xmm10                                 #898.143
        addsd     4424+_vmldASinHATab(%rip), %xmm5              #901.141
        addsd     4328+_vmldASinHATab(%rip), %xmm14             #904.176
        mulsd     %xmm9, %xmm5                                  #901.163
        addsd     %xmm13, %xmm14                                #904.278
        addsd     4416+_vmldASinHATab(%rip), %xmm5              #901.184
        mulsd     %xmm9, %xmm14                                 #904.281
        mulsd     %xmm9, %xmm5                                  #901.206
        movsd     %xmm10, -48(%rsp)                             #898.124
        movaps    %xmm9, %xmm2                                  #906.155
        movsd     -48(%rsp), %xmm11                             #898.176
        movsd     4104+_vmldASinHATab(%rip), %xmm3              #906.59
        subsd     %xmm11, %xmm7                                 #898.176
        mulsd     %xmm9, %xmm3                                  #906.59
        addsd     4408+_vmldASinHATab(%rip), %xmm5              #902.45
        mulsd     %xmm9, %xmm5                                  #902.67
        movsd     4304+_vmldASinHATab(%rip), %xmm4              #904.96
        movsd     %xmm7, -56(%rsp)                              #898.149
        movsd     -48(%rsp), %xmm6                              #898.190
        movsd     -56(%rsp), %xmm7                              #898.202
        movsd     %xmm3, -48(%rsp)                              #906.21
        addsd     %xmm7, %xmm8                                  #900.21
        addsd     4400+_vmldASinHATab(%rip), %xmm5              #902.88
        addsd     4312+_vmldASinHATab(%rip), %xmm4              #904.96
        mulsd     %xmm9, %xmm5                                  #902.110
        addsd     %xmm14, %xmm4                                 #904.281
        addsd     4392+_vmldASinHATab(%rip), %xmm5              #902.131
        mulsd     %xmm9, %xmm5                                  #902.153
        movsd     -48(%rsp), %xmm1                              #906.98
        movsd     4104+_vmldASinHATab(%rip), %xmm7              #909.63
        subsd     %xmm9, %xmm1                                  #906.103
        addsd     4384+_vmldASinHATab(%rip), %xmm5              #902.174
        mulsd     %xmm9, %xmm5                                  #902.196
        movsd     %xmm1, -56(%rsp)                              #906.84
        movsd     -48(%rsp), %xmm0                              #906.124
        movsd     -56(%rsp), %xmm15                             #906.129
        addsd     4376+_vmldASinHATab(%rip), %xmm5              #902.217
        subsd     %xmm15, %xmm0                                 #906.129
        mulsd     %xmm9, %xmm5                                  #902.239
        movsd     %xmm0, -48(%rsp)                              #906.110
        movsd     4320+_vmldASinHATab(%rip), %xmm0              #910.68
        addsd     4368+_vmldASinHATab(%rip), %xmm5              #902.260
        mulsd     %xmm9, %xmm5                                  #902.282
        addsd     4360+_vmldASinHATab(%rip), %xmm5              #903.45
        mulsd     %xmm9, %xmm5                                  #903.67
        addsd     4352+_vmldASinHATab(%rip), %xmm5              #903.88
        mulsd     %xmm9, %xmm5                                  #903.110
        mulsd     %xmm9, %xmm5                                  #903.112
        mulsd     %xmm9, %xmm5                                  #903.114
        addsd     %xmm5, %xmm4                                  #905.25
        mulsd     %xmm4, %xmm8                                  #905.25
        movsd     -48(%rsp), %xmm4                              #906.155
        subsd     %xmm4, %xmm2                                  #906.155
        movsd     %xmm2, -56(%rsp)                              #906.135
        movsd     -48(%rsp), %xmm4                              #906.169
        movaps    %xmm4, %xmm13                                 #907.35
        mulsd     %xmm4, %xmm13                                 #907.35
        mulsd     %xmm4, %xmm0                                  #910.68
        addsd     %xmm9, %xmm4                                  #908.45
        mulsd     %xmm13, %xmm7                                 #909.63
        movsd     -56(%rsp), %xmm3                              #906.181
        movsd     %xmm7, -48(%rsp)                              #909.21
        movsd     -48(%rsp), %xmm10                             #909.102
        mulsd     %xmm3, %xmm4                                  #908.45
        subsd     %xmm13, %xmm10                                #909.107
        movsd     %xmm10, -56(%rsp)                             #909.88
        movsd     -48(%rsp), %xmm11                             #909.132
        movsd     -56(%rsp), %xmm1                              #909.137
        movsd     4304+_vmldASinHATab(%rip), %xmm10             #913.77
        subsd     %xmm1, %xmm11                                 #909.137
        movsd     %xmm11, -48(%rsp)                             #909.118
        movaps    %xmm0, %xmm1                                  #912.45
        movsd     -48(%rsp), %xmm12                             #909.167
        movsd     4304+_vmldASinHATab(%rip), %xmm11             #913.146
        subsd     %xmm12, %xmm13                                #909.167
        movsd     %xmm13, -56(%rsp)                             #909.143
        movsd     -48(%rsp), %xmm7                              #909.181
        mulsd     4336+_vmldASinHATab(%rip), %xmm7              #911.68
        movsd     -56(%rsp), %xmm2                              #909.193
        addsd     %xmm7, %xmm1                                  #912.45
        addsd     %xmm2, %xmm4                                  #915.155
        movsd     %xmm1, -48(%rsp)                              #912.21
        movsd     -48(%rsp), %xmm14                             #912.85
        movsd     %xmm7, -16(%rsp)                              #911.21
        subsd     %xmm14, %xmm0                                 #912.85
        mulsd     4336+_vmldASinHATab(%rip), %xmm4              #915.155
        addsd     %xmm0, %xmm7                                  #912.114
        movsd     %xmm7, -56(%rsp)                              #912.91
        movsd     -48(%rsp), %xmm12                             #912.133
        movsd     -56(%rsp), %xmm1                              #912.144
        addsd     %xmm12, %xmm10                                #913.77
        movsd     %xmm10, -48(%rsp)                             #913.21
        movsd     -48(%rsp), %xmm15                             #913.146
        movsd     4104+_vmldASinHATab(%rip), %xmm7              #914.62
        subsd     %xmm15, %xmm11                                #913.146
        movsd     4320+_vmldASinHATab(%rip), %xmm2              #915.102
        addsd     %xmm11, %xmm12                                #913.174
        mulsd     %xmm3, %xmm2                                  #915.102
        movsd     %xmm12, -56(%rsp)                             #913.152
        movsd     -48(%rsp), %xmm15                             #913.191
        mulsd     %xmm15, %xmm7                                 #914.62
        movsd     -56(%rsp), %xmm0                              #913.202
        movsd     %xmm7, -48(%rsp)                              #914.21
        movsd     -48(%rsp), %xmm10                             #914.101
        movsd     4344+_vmldASinHATab(%rip), %xmm3              #916.140
        subsd     %xmm15, %xmm10                                #914.106
        mulsd     %xmm9, %xmm3                                  #916.161
        movsd     %xmm10, -56(%rsp)                             #914.87
        movsd     -48(%rsp), %xmm11                             #914.130
        movsd     -56(%rsp), %xmm13                             #914.135
        subsd     %xmm13, %xmm11                                #914.135
        addsd     4328+_vmldASinHATab(%rip), %xmm3              #916.161
        movsd     %xmm11, -48(%rsp)                             #914.116
        movsd     -48(%rsp), %xmm14                             #914.164
        mulsd     %xmm9, %xmm3                                  #916.164
        subsd     %xmm14, %xmm15                                #914.164
        movsd     %xmm15, -56(%rsp)                             #914.141
        movsd     -48(%rsp), %xmm10                             #914.178
        movsd     -56(%rsp), %xmm7                              #914.190
        addsd     %xmm7, %xmm4                                  #915.50
        addsd     4312+_vmldASinHATab(%rip), %xmm3              #916.164
        addsd     %xmm2, %xmm4                                  #915.57
        addsd     %xmm5, %xmm3                                  #916.168
        addsd     %xmm1, %xmm4                                  #915.102
        addsd     %xmm0, %xmm4                                  #915.155
        movsd     4144+_vmldASinHATab(%rip), %xmm0              #918.53
        addsd     %xmm3, %xmm4                                  #916.21
        mulsd     %xmm6, %xmm4                                  #916.21
        mulsd     %xmm10, %xmm6                                 #917.39
        addsd     %xmm8, %xmm4                                  #916.21
        movaps    %xmm0, %xmm8                                  #918.53
        subsd     %xmm6, %xmm8                                  #918.53
        movsd     %xmm8, -48(%rsp)                              #918.21
        movsd     -48(%rsp), %xmm9                              #918.147
        subsd     %xmm9, %xmm0                                  #918.147
        subsd     %xmm6, %xmm0                                  #918.167
        movsd     %xmm0, -56(%rsp)                              #918.153
        movsd     -48(%rsp), %xmm2                              #918.196
        movsd     -56(%rsp), %xmm1                              #918.207
        movb      3(%rdi), %dil                                 #922.82
        andb      $-128, %dil                                   #922.42
        addsd     4152+_vmldASinHATab(%rip), %xmm1              #920.21
        subsd     %xmm4, %xmm1                                  #919.71
        addsd     %xmm1, %xmm2                                  #921.39
        movsd     %xmm2, -40(%rsp)                              #921.21
        movb      -33(%rsp), %dl                                #922.42
        andb      $127, %dl                                     #922.42
        orb       %dil, %dl                                     #922.42
        movb      %dl, -33(%rsp)                                #922.42
        movsd     -40(%rsp), %xmm3                              #923.22
        cvtsd2ss  %xmm3, %xmm3                                  #923.22
        movss     %xmm3, (%rsi)                                 #923.22
        ret                                                     #923.22
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.10:                        # Preds ..B7.2
                                # Execution count [4.20e-01]
        movl      $1, %eax                                      #929.17
        pxor      %xmm1, %xmm1                                  #928.41
        pxor      %xmm0, %xmm0                                  #928.41
        divss     %xmm0, %xmm1                                  #928.41
        movss     %xmm1, (%rsi)                                 #928.18
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.11:                        # Preds ..B7.10
                                # Execution count [1.00e+00]
        ret                                                     #940.16
                                # LOE
..B7.12:                        # Preds ..B7.1
                                # Execution count [1.60e-01]: Infreq
        movss     (%rdi), %xmm0                                 #934.19
        divss     %xmm0, %xmm0                                  #934.24
        xorl      %eax, %eax                                    #935.39
        testl     $8388607, (%rdi)                              #935.39
        movss     %xmm0, (%rsi)                                 #934.14
        sete      %al                                           #937.17
        ret                                                     #937.17
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_sasin_ha_cout_rare_internal,@function
	.size	__svml_sasin_ha_cout_rare_internal,.-__svml_sasin_ha_cout_rare_internal
..LN__svml_sasin_ha_cout_rare_internal.6:
	.data
# -- End  __svml_sasin_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_sasin_ha_data_internal
	.globl __svml_sasin_ha_data_internal
__svml_sasin_ha_data_internal:
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
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
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
	.type	__svml_sasin_ha_data_internal,@object
	.size	__svml_sasin_ha_data_internal,1472
	.align 32
_vmldASinHATab:
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
	.long	1048576
	.long	0
	.long	1017118720
	.long	1413754136
	.long	1073291771
	.long	856972295
	.long	1016178214
	.long	0
	.long	1072693248
	.long	0
	.long	1071644672
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
	.type	_vmldASinHATab,@object
	.size	_vmldASinHATab,4504
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
