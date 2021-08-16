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
	.file "svml_d_log10.c"
	.text
..TXTST0:
.L_2__routine_start___svml_log102_ha_ex_0:
# -- Begin  __svml_log102_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log102_ha_ex
# --- __svml_log102_ha_ex(__m128d)
__svml_log102_ha_ex:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #992.1
        .byte     15                                            #1081.546
        .byte     30                                            #1081.546
        .byte     250                                           #1081.546
	.cfi_startproc
..___tag_value___svml_log102_ha_ex.1:
..L2:
                                                          #992.1
        pushq     %rbp                                          #992.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #992.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #992.1
        subq      $192, %rsp                                    #992.1
        movaps    %xmm0, %xmm5                                  #1038.12
        movups    9024+__svml_dlog10_ha_data_internal(%rip), %xmm1 #1032.15
        psrlq     $20, %xmm5                                    #1038.12
        andps     %xmm0, %xmm1                                  #1032.15
        lea       -4221888+__svml_dlog10_ha_data_internal(%rip), %rsi #1055.67
        orps      9088+__svml_dlog10_ha_data_internal(%rip), %xmm1 #1033.15
        movaps    %xmm0, %xmm8                                  #1040.14
        cvtpd2ps  %xmm1, %xmm2                                  #1036.53
        cmpltpd   9152+__svml_dlog10_ha_data_internal(%rip), %xmm8 #1040.14
        movlhps   %xmm2, %xmm2                                  #1036.37
        movaps    %xmm0, %xmm7                                  #1041.14
        rcpps     %xmm2, %xmm3                                  #1036.26
        cmpnlepd  9216+__svml_dlog10_ha_data_internal(%rip), %xmm7 #1041.14
        cvtps2pd  %xmm3, %xmm11                                 #1036.13
        movups    9344+__svml_dlog10_ha_data_internal(%rip), %xmm9 #1042.45
        orps      %xmm7, %xmm8                                  #1046.14
        mulpd     %xmm9, %xmm11                                 #1043.17
        movmskpd  %xmm8, %edx                                   #1048.40
        movups    .L_2il0floatpacket.28(%rip), %xmm4            #1044.103
        movups    9280+__svml_dlog10_ha_data_internal(%rip), %xmm10 #1049.50
        addpd     %xmm4, %xmm11                                 #1044.293
        subpd     %xmm4, %xmm11                                 #1044.343
        andps     %xmm1, %xmm10                                 #1050.120
        subpd     %xmm10, %xmm1                                 #1050.174
        mulpd     %xmm11, %xmm10                                #1050.227
        mulpd     %xmm11, %xmm1                                 #1050.278
        subpd     %xmm9, %xmm10                                 #1050.324
        pshufd    $221, %xmm5, %xmm6                            #1039.13
        psrlq     $40, %xmm11                                   #1052.16
        cvtdq2pd  %xmm6, %xmm2                                  #1045.20
        addpd     %xmm10, %xmm1                                 #1050.367
        movd      %xmm11, %eax                                  #1055.144
        pshufd    $2, %xmm11, %xmm12                            #1055.331
        movd      %xmm12, %ecx                                  #1055.313
        movups    9472+__svml_dlog10_ha_data_internal(%rip), %xmm14 #1059.25
        movups    8448+__svml_dlog10_ha_data_internal(%rip), %xmm12 #1068.26
        movslq    %eax, %rax                                    #1055.67
        movslq    %ecx, %rcx                                    #1055.236
        mulpd     %xmm2, %xmm14                                 #1059.25
        mulpd     %xmm1, %xmm12                                 #1068.26
        movups    (%rsi,%rax), %xmm4                            #1055.67
        movups    (%rsi,%rcx), %xmm13                           #1055.236
        movaps    %xmm4, %xmm11                                 #1055.397
        unpcklpd  %xmm13, %xmm11                                #1055.397
        unpckhpd  %xmm13, %xmm4                                 #1055.438
        movups    8320+__svml_dlog10_ha_data_internal(%rip), %xmm13 #1067.26
        mulpd     %xmm1, %xmm13                                 #1067.26
        addpd     %xmm14, %xmm11                                #1059.13
        addpd     8384+__svml_dlog10_ha_data_internal(%rip), %xmm13 #1067.14
        addpd     8512+__svml_dlog10_ha_data_internal(%rip), %xmm12 #1068.14
        movaps    %xmm1, %xmm14                                 #1069.13
        mulpd     %xmm1, %xmm14                                 #1069.13
        mulpd     %xmm14, %xmm13                                #1072.25
        movups    9536+__svml_dlog10_ha_data_internal(%rip), %xmm15 #1058.45
        mulpd     %xmm15, %xmm2                                 #1060.25
        addpd     %xmm13, %xmm12                                #1072.13
        addpd     %xmm2, %xmm4                                  #1060.13
        mulpd     %xmm12, %xmm14                                #1074.25
        movups    8576+__svml_dlog10_ha_data_internal(%rip), %xmm5 #1070.26
        movaps    %xmm1, %xmm2                                  #1071.13
        mulpd     %xmm1, %xmm5                                  #1070.26
        addpd     %xmm11, %xmm2                                 #1071.13
        addpd     8640+__svml_dlog10_ha_data_internal(%rip), %xmm5 #1070.14
        movaps    %xmm2, %xmm3                                  #1073.13
        subpd     %xmm11, %xmm3                                 #1073.13
        addpd     %xmm14, %xmm5                                 #1074.13
        mulpd     %xmm1, %xmm5                                  #1075.13
        subpd     %xmm3, %xmm1                                  #1076.13
        addpd     %xmm1, %xmm4                                  #1077.13
        addpd     %xmm4, %xmm5                                  #1078.13
        addpd     %xmm5, %xmm2                                  #1079.14
        testl     %edx, %edx                                    #1081.52
        jne       ..B1.3        # Prob 5%                       #1081.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm2
..B1.2:                         # Preds ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movaps    %xmm2, %xmm0                                  #1084.12
        movq      %rbp, %rsp                                    #1084.12
        popq      %rbp                                          #1084.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1084.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #1081.197
        movups    %xmm2, 128(%rsp)                              #1081.270
                                # LOE rbx r12 r13 r14 r15 edx
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1081.454
        movq      %r12, 8(%rsp)                                 #1081.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1081.454
        movq      %r13, (%rsp)                                  #1081.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1081.454
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1081.517
        jc        ..B1.10       # Prob 5%                       #1081.517
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1081.470
        cmpl      $2, %r12d                                     #1081.465
        jl        ..B1.7        # Prob 82%                      #1081.465
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm2                              #1081.675
        jmp       ..B1.2        # Prob 100%                     #1081.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm2
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1081.546
        lea       128(%rsp,%r12,8), %rsi                        #1081.546
..___tag_value___svml_log102_ha_ex.19:
#       __svml_dlog10_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog10_ha_cout_rare_internal           #1081.546
..___tag_value___svml_log102_ha_ex.20:
        jmp       ..B1.8        # Prob 100%                     #1081.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log102_ha_ex,@function
	.size	__svml_log102_ha_ex,.-__svml_log102_ha_ex
..LN__svml_log102_ha_ex.0:
	.data
# -- End  __svml_log102_ha_ex
	.text
.L_2__routine_start___svml_log101_ha_l9_1:
# -- Begin  __svml_log101_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log101_ha_l9
# --- __svml_log101_ha_l9(__m128d)
__svml_log101_ha_l9:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1089.1
        .byte     15                                            #1178.546
        .byte     30                                            #1178.546
        .byte     250                                           #1178.546
	.cfi_startproc
..___tag_value___svml_log101_ha_l9.22:
..L23:
                                                         #1089.1
        pushq     %rbp                                          #1089.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1089.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1089.1
        subq      $128, %rsp                                    #1089.1
        vmovapd   %xmm0, %xmm2                                  #1089.1
        vmovsd    9024+__svml_dlog10_ha_data_internal(%rip), %xmm3 #1127.14
        vpsrlq    $20, %xmm2, %xmm11                            #1135.14
        vmovsd    9088+__svml_dlog10_ha_data_internal(%rip), %xmm5 #1128.12
        vandpd    %xmm3, %xmm2, %xmm4                           #1129.15
        vorpd     %xmm5, %xmm4, %xmm1                           #1130.15
        lea       -4221880+__svml_dlog10_ha_data_internal(%rip), %rdx #1152.17
        vcvtpd2ps %xmm1, %xmm6                                  #1133.53
        vcmpltsd  9152+__svml_dlog10_ha_data_internal(%rip), %xmm2, %xmm14 #1137.14
        vcmpnlesd 9216+__svml_dlog10_ha_data_internal(%rip), %xmm2, %xmm15 #1138.14
        vmovlhps  %xmm6, %xmm6, %xmm7                           #1133.37
        vrcpps    %xmm7, %xmm8                                  #1133.26
        vcvtps2pd %xmm8, %xmm9                                  #1133.13
        vmovsd    9344+__svml_dlog10_ha_data_internal(%rip), %xmm3 #1139.14
        vmulsd    %xmm3, %xmm9, %xmm10                          #1140.17
        vpshufd   $85, %xmm11, %xmm12                           #1136.15
        vorpd     %xmm15, %xmm14, %xmm11                        #1143.14
        vroundsd  $0, %xmm10, %xmm10, %xmm4                     #1141.13
        vpshufd   $0, %xmm12, %xmm13                            #1142.37
        vpsrlq    $40, %xmm4, %xmm5                             #1149.18
        vcvtdq2pd %xmm13, %xmm0                                 #1142.20
        vfmsub213sd %xmm3, %xmm4, %xmm1                         #1147.13
        vmovd     %xmm5, %eax                                   #1152.117
        vmovsd    9472+__svml_dlog10_ha_data_internal(%rip), %xmm7 #1154.14
        vmovapd   %xmm0, %xmm10                                 #1156.13
        vmovsd    8320+__svml_dlog10_ha_data_internal(%rip), %xmm14 #1158.24
        movslq    %eax, %rax                                    #1152.106
        vmovsd    8448+__svml_dlog10_ha_data_internal(%rip), %xmm9 #1160.24
        vfmadd213sd 8384+__svml_dlog10_ha_data_internal(%rip), %xmm1, %xmm14 #1164.14
        vfmadd213sd 8512+__svml_dlog10_ha_data_internal(%rip), %xmm1, %xmm9 #1165.14
        vfmadd213sd -8(%rdx,%rax), %xmm7, %xmm10                #1156.13
        vmulsd    %xmm1, %xmm1, %xmm12                          #1166.13
        vmovmskpd %xmm11, %ecx                                  #1145.40
        vaddsd    %xmm1, %xmm10, %xmm3                          #1168.13
        vfmadd213sd %xmm9, %xmm12, %xmm14                       #1169.13
        vmovsd    8576+__svml_dlog10_ha_data_internal(%rip), %xmm13 #1162.24
        vsubsd    %xmm10, %xmm3, %xmm15                         #1170.13
        vfmadd213sd 8640+__svml_dlog10_ha_data_internal(%rip), %xmm1, %xmm13 #1167.14
        vmovsd    (%rdx,%rax), %xmm6                            #1152.106
        vfmadd213sd %xmm13, %xmm12, %xmm14                      #1171.13
        vunpcklpd %xmm6, %xmm6, %xmm8                           #1152.162
        vfmadd132sd 9536+__svml_dlog10_ha_data_internal(%rip), %xmm8, %xmm0 #1157.13
        vmulsd    %xmm1, %xmm14, %xmm9                          #1172.13
        vsubsd    %xmm15, %xmm1, %xmm1                          #1173.13
        vaddsd    %xmm1, %xmm0, %xmm0                           #1174.13
        vaddsd    %xmm9, %xmm0, %xmm0                           #1175.13
        vaddsd    %xmm3, %xmm0, %xmm0                           #1176.14
        andl      $1, %ecx                                      #1145.90
        jne       ..B2.3        # Prob 5%                       #1178.52
                                # LOE rbx r12 r13 r14 r15 ecx xmm0 xmm2
..B2.2:                         # Preds ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1181.12
        popq      %rbp                                          #1181.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1181.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm2, (%rsp)                                 #1178.160
        vmovsd    %xmm0, 64(%rsp)                               #1178.233
        jne       ..B2.5        # Prob 5%                       #1178.374
                                # LOE rbx r12 r13 r14 r15 ecx
..B2.4:                         # Preds ..B2.6 ..B2.5 ..B2.3
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm0                               #1178.627
        movq      %rbp, %rsp                                    #1178.627
        popq      %rbp                                          #1178.627
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1178.627
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.5:                         # Preds ..B2.3
                                # Execution count [2.50e-03]: Infreq
        je        ..B2.4        # Prob 95%                      #1178.517
                                # LOE rbx r12 r13 r14 r15
..B2.6:                         # Preds ..B2.5
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1178.546
        lea       64(%rsp), %rsi                                #1178.546
..___tag_value___svml_log101_ha_l9.35:
#       __svml_dlog10_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog10_ha_cout_rare_internal           #1178.546
..___tag_value___svml_log101_ha_l9.36:
        jmp       ..B2.4        # Prob 100%                     #1178.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_log101_ha_l9,@function
	.size	__svml_log101_ha_l9,.-__svml_log101_ha_l9
..LN__svml_log101_ha_l9.1:
	.data
# -- End  __svml_log101_ha_l9
	.text
.L_2__routine_start___svml_log101_ha_e9_2:
# -- Begin  __svml_log101_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log101_ha_e9
# --- __svml_log101_ha_e9(__m128d)
__svml_log101_ha_e9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1186.1
        .byte     15                                            #1275.546
        .byte     30                                            #1275.546
        .byte     250                                           #1275.546
	.cfi_startproc
..___tag_value___svml_log101_ha_e9.38:
..L39:
                                                         #1186.1
        pushq     %rbp                                          #1186.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1186.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1186.1
        subq      $128, %rsp                                    #1186.1
        movaps    %xmm0, %xmm4                                  #1232.18
        movsd     9024+__svml_dlog10_ha_data_internal(%rip), %xmm10 #1226.15
        psrlq     $20, %xmm4                                    #1232.18
        movsd     9088+__svml_dlog10_ha_data_internal(%rip), %xmm1 #1225.12
        andps     %xmm0, %xmm10                                 #1226.15
        orps      %xmm1, %xmm10                                 #1227.15
        lea       -4221880+__svml_dlog10_ha_data_internal(%rip), %rdx #1249.17
        cvtpd2ps  %xmm10, %xmm2                                 #1230.53
        movlhps   %xmm2, %xmm2                                  #1230.37
        movaps    %xmm0, %xmm8                                  #1234.14
        rcpps     %xmm2, %xmm3                                  #1230.26
        cmpltsd   9152+__svml_dlog10_ha_data_internal(%rip), %xmm8 #1234.14
        cvtps2pd  %xmm3, %xmm11                                 #1230.13
        movsd     9344+__svml_dlog10_ha_data_internal(%rip), %xmm9 #1236.14
        movaps    %xmm0, %xmm7                                  #1235.14
        mulsd     %xmm9, %xmm11                                 #1237.17
        cmpnlesd  9216+__svml_dlog10_ha_data_internal(%rip), %xmm7 #1235.14
        movsd     9280+__svml_dlog10_ha_data_internal(%rip), %xmm1 #1244.120
        orps      %xmm7, %xmm8                                  #1240.14
        roundsd   $0, %xmm11, %xmm11                            #1238.13
        andps     %xmm10, %xmm1                                 #1244.120
        pshufd    $85, %xmm4, %xmm5                             #1233.19
        subsd     %xmm1, %xmm10                                 #1244.174
        mulsd     %xmm11, %xmm1                                 #1244.227
        movmskpd  %xmm8, %ecx                                   #1242.40
        mulsd     %xmm11, %xmm10                                #1244.278
        subsd     %xmm9, %xmm1                                  #1244.324
        pshufd    $0, %xmm5, %xmm6                              #1239.37
        psrlq     $40, %xmm11                                   #1246.22
        movsd     8320+__svml_dlog10_ha_data_internal(%rip), %xmm2 #1261.26
        addsd     %xmm10, %xmm1                                 #1244.367
        cvtdq2pd  %xmm6, %xmm4                                  #1239.20
        mulsd     %xmm1, %xmm2                                  #1261.26
        movd      %xmm11, %eax                                  #1249.117
        movsd     8448+__svml_dlog10_ha_data_internal(%rip), %xmm12 #1262.26
        movaps    %xmm1, %xmm14                                 #1263.13
        mulsd     %xmm1, %xmm12                                 #1262.26
        addsd     8384+__svml_dlog10_ha_data_internal(%rip), %xmm2 #1261.14
        mulsd     %xmm1, %xmm14                                 #1263.13
        addsd     8512+__svml_dlog10_ha_data_internal(%rip), %xmm12 #1262.14
        mulsd     %xmm14, %xmm2                                 #1266.25
        movaps    %xmm4, %xmm13                                 #1253.25
        addsd     %xmm12, %xmm2                                 #1266.13
        mulsd     9472+__svml_dlog10_ha_data_internal(%rip), %xmm13 #1253.25
        mulsd     9536+__svml_dlog10_ha_data_internal(%rip), %xmm4 #1254.25
        mulsd     %xmm14, %xmm2                                 #1268.25
        movslq    %eax, %rax                                    #1249.106
        movsd     8576+__svml_dlog10_ha_data_internal(%rip), %xmm15 #1264.26
        mulsd     %xmm1, %xmm15                                 #1264.26
        addsd     -8(%rdx,%rax), %xmm13                         #1253.13
        addsd     (%rdx,%rax), %xmm4                            #1254.13
        addsd     8640+__svml_dlog10_ha_data_internal(%rip), %xmm15 #1264.14
        movaps    %xmm13, %xmm3                                 #1265.13
        addsd     %xmm15, %xmm2                                 #1268.13
        addsd     %xmm1, %xmm3                                  #1265.13
        mulsd     %xmm1, %xmm2                                  #1269.13
        movaps    %xmm3, %xmm12                                 #1267.13
        subsd     %xmm13, %xmm12                                #1267.13
        subsd     %xmm12, %xmm1                                 #1270.13
        addsd     %xmm1, %xmm4                                  #1271.13
        addsd     %xmm2, %xmm4                                  #1272.13
        addsd     %xmm3, %xmm4                                  #1273.14
        andl      $1, %ecx                                      #1242.90
        jne       ..B3.3        # Prob 5%                       #1275.52
                                # LOE rbx r12 r13 r14 r15 ecx xmm0 xmm4
..B3.2:                         # Preds ..B3.4 ..B3.1
                                # Execution count [1.00e+00]
        movaps    %xmm4, %xmm0                                  #1278.12
        movq      %rbp, %rsp                                    #1278.12
        popq      %rbp                                          #1278.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1278.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm0, (%rsp)                                 #1275.160
        movsd     %xmm4, 64(%rsp)                               #1275.233
        jne       ..B3.6        # Prob 5%                       #1275.374
                                # LOE rbx r12 r13 r14 r15
..B3.4:                         # Preds ..B3.6 ..B3.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm4                               #1275.627
        jmp       ..B3.2        # Prob 100%                     #1275.627
                                # LOE rbx r12 r13 r14 r15 xmm4
..B3.6:                         # Preds ..B3.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1275.546
        lea       64(%rsp), %rsi                                #1275.546
..___tag_value___svml_log101_ha_e9.47:
#       __svml_dlog10_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog10_ha_cout_rare_internal           #1275.546
..___tag_value___svml_log101_ha_e9.48:
        jmp       ..B3.4        # Prob 100%                     #1275.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_log101_ha_e9,@function
	.size	__svml_log101_ha_e9,.-__svml_log101_ha_e9
..LN__svml_log101_ha_e9.2:
	.data
# -- End  __svml_log101_ha_e9
	.text
.L_2__routine_start___svml_log104_ha_e9_3:
# -- Begin  __svml_log104_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log104_ha_e9
# --- __svml_log104_ha_e9(__m256d)
__svml_log104_ha_e9:
# parameter 1: %ymm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1283.1
        .byte     15                                            #1372.552
        .byte     30                                            #1372.552
        .byte     250                                           #1372.552
	.cfi_startproc
..___tag_value___svml_log104_ha_e9.50:
..L51:
                                                         #1283.1
        pushq     %rbp                                          #1283.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1283.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1283.1
        subq      $192, %rsp                                    #1283.1
        lea       -4221888+__svml_dlog10_ha_data_internal(%rip), %r8 #1346.68
        vmovapd   %ymm0, %ymm8                                  #1283.1
        vandpd    9024+__svml_dlog10_ha_data_internal(%rip), %ymm8, %ymm9 #1323.15
        vorpd     9088+__svml_dlog10_ha_data_internal(%rip), %ymm9, %ymm7 #1324.15
        vcvtpd2ps %ymm7, %xmm10                                 #1327.40
        vmovupd   9344+__svml_dlog10_ha_data_internal(%rip), %ymm6 #1333.48
        vrcpps    %xmm10, %xmm11                                #1327.29
        vcvtps2pd %xmm11, %ymm15                                #1327.13
        vmulpd    %ymm6, %ymm15, %ymm0                          #1334.17
        vroundpd  $0, %ymm0, %ymm4                              #1335.13
        vpsrlq    $20, %xmm8, %xmm13                            #1329.18
        vextractf128 $1, %ymm8, %xmm12                          #1328.107
        vcmplt_oqpd 9152+__svml_dlog10_ha_data_internal(%rip), %ymm8, %ymm3 #1331.14
        vcmpnle_uqpd 9216+__svml_dlog10_ha_data_internal(%rip), %ymm8, %ymm2 #1332.14
        vpsrlq    $20, %xmm12, %xmm14                           #1329.76
        vshufps   $221, %xmm14, %xmm13, %xmm1                   #1330.37
        vandpd    9280+__svml_dlog10_ha_data_internal(%rip), %ymm7, %ymm12 #1341.120
        vsubpd    %ymm12, %ymm7, %ymm7                          #1341.177
        vmulpd    %ymm12, %ymm4, %ymm13                         #1341.233
        vcvtdq2pd %xmm1, %ymm5                                  #1336.20
        vmulpd    %ymm7, %ymm4, %ymm14                          #1341.287
        vsubpd    %ymm6, %ymm13, %ymm6                          #1341.336
        vorpd     %ymm2, %ymm3, %ymm9                           #1337.14
        vaddpd    %ymm6, %ymm14, %ymm3                          #1341.382
        vmulpd    9472+__svml_dlog10_ha_data_internal(%rip), %ymm5, %ymm14 #1350.28
        vmulpd    9536+__svml_dlog10_ha_data_internal(%rip), %ymm5, %ymm5 #1351.28
        vpsrlq    $40, %xmm4, %xmm0                             #1343.22
        vextractf128 $1, %ymm4, %xmm15                          #1342.110
        vmovd     %xmm0, %edx                                   #1346.145
        vpsrlq    $40, %xmm15, %xmm1                            #1343.81
        vmovd     %xmm1, %esi                                   #1346.504
        vpextrd   $2, %xmm1, %edi                               #1346.682
        vpextrd   $2, %xmm0, %ecx                               #1346.323
        movslq    %esi, %rsi                                    #1346.427
        movslq    %edi, %rdi                                    #1346.605
        movslq    %edx, %rdx                                    #1346.68
        movslq    %ecx, %rcx                                    #1346.246
        vmulpd    8320+__svml_dlog10_ha_data_internal(%rip), %ymm3, %ymm0 #1358.29
        vmovupd   (%r8,%rdx), %xmm6                             #1346.68
        vmovupd   (%r8,%rcx), %xmm7                             #1346.246
        vunpcklpd %xmm7, %xmm6, %xmm2                           #1346.783
        vextractf128 $1, %ymm9, %xmm10                          #1338.113
        vshufps   $221, %xmm10, %xmm9, %xmm11                   #1339.57
        vmovmskps %xmm11, %eax                                  #1339.40
        vmovupd   (%r8,%rsi), %xmm10                            #1346.427
        vmovupd   (%r8,%rdi), %xmm11                            #1346.605
        vunpcklpd %xmm11, %xmm10, %xmm4                         #1346.816
        vunpckhpd %xmm11, %xmm10, %xmm12                        #1346.943
        vmulpd    %ymm3, %ymm3, %ymm10                          #1360.13
        vunpckhpd %xmm7, %xmm6, %xmm9                           #1346.910
        vaddpd    8384+__svml_dlog10_ha_data_internal(%rip), %ymm0, %ymm6 #1358.14
        vinsertf128 $1, %xmm4, %ymm2, %ymm13                    #1346.737
        vmulpd    8448+__svml_dlog10_ha_data_internal(%rip), %ymm3, %ymm4 #1359.29
        vaddpd    %ymm14, %ymm13, %ymm2                         #1350.13
        vaddpd    8512+__svml_dlog10_ha_data_internal(%rip), %ymm4, %ymm7 #1359.14
        vaddpd    %ymm2, %ymm3, %ymm0                           #1362.13
        vsubpd    %ymm2, %ymm0, %ymm14                          #1364.13
        vinsertf128 $1, %xmm12, %ymm9, %ymm15                   #1346.864
        vmulpd    %ymm10, %ymm6, %ymm9                          #1363.28
        vaddpd    %ymm5, %ymm15, %ymm1                          #1351.13
        vmulpd    8576+__svml_dlog10_ha_data_internal(%rip), %ymm3, %ymm5 #1361.29
        vaddpd    %ymm9, %ymm7, %ymm11                          #1363.13
        vaddpd    8640+__svml_dlog10_ha_data_internal(%rip), %ymm5, %ymm12 #1361.14
        vmulpd    %ymm11, %ymm10, %ymm2                         #1365.28
        vaddpd    %ymm2, %ymm12, %ymm13                         #1365.13
        vmulpd    %ymm13, %ymm3, %ymm15                         #1366.13
        vsubpd    %ymm14, %ymm3, %ymm3                          #1367.13
        vaddpd    %ymm3, %ymm1, %ymm1                           #1368.13
        vaddpd    %ymm1, %ymm15, %ymm1                          #1369.13
        vaddpd    %ymm1, %ymm0, %ymm0                           #1370.14
        testl     %eax, %eax                                    #1372.52
        jne       ..B4.3        # Prob 5%                       #1372.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm8
..B4.2:                         # Preds ..B4.3 ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1375.12
        popq      %rbp                                          #1375.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1375.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm8, 64(%rsp)                               #1372.200
        vmovupd   %ymm0, 128(%rsp)                              #1372.276
        je        ..B4.2        # Prob 95%                      #1372.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1372.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B4.13:                        # Preds ..B4.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B4.7:                         # Preds ..B4.8 ..B4.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1372.523
        jc        ..B4.10       # Prob 5%                       #1372.523
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1372.476
        cmpl      $4, %r12d                                     #1372.471
        jl        ..B4.7        # Prob 82%                      #1372.471
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1372.684
        jmp       ..B4.2        # Prob 100%                     #1372.684
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1372.552
        lea       128(%rsp,%r12,8), %rsi                        #1372.552
..___tag_value___svml_log104_ha_e9.68:
#       __svml_dlog10_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog10_ha_cout_rare_internal           #1372.552
..___tag_value___svml_log104_ha_e9.69:
        jmp       ..B4.8        # Prob 100%                     #1372.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log104_ha_e9,@function
	.size	__svml_log104_ha_e9,.-__svml_log104_ha_e9
..LN__svml_log104_ha_e9.3:
	.data
# -- End  __svml_log104_ha_e9
	.text
.L_2__routine_start___svml_log108_ha_z0_4:
# -- Begin  __svml_log108_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log108_ha_z0
# --- __svml_log108_ha_z0(__m512d)
__svml_log108_ha_z0:
# parameter 1: %zmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1380.1
        .byte     15                                            #1479.12
        .byte     30                                            #1479.12
        .byte     250                                           #1479.12
	.cfi_startproc
..___tag_value___svml_log108_ha_z0.71:
..L72:
                                                         #1380.1
        vmovaps   %zmm0, %zmm5                                  #1380.1
        vgetmantpd $8, {sae}, %zmm5, %zmm4                      #1429.21
        vmovups   256+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm9 #1436.46
        vmovups   320+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm8 #1434.47
        vgetexppd {sae}, %zmm5, %zmm3                           #1430.18
        vmovups   __svml_dlog10_ha_data_internal_avx512(%rip), %zmm2 #1441.289
        vmovups   128+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm15 #1442.289
        vmovups   512+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm12 #1445.54
        vmovups   1024+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm13 #1450.46
        vmovups   384+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm11 #1443.54
        vmovups   448+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm1 #1447.14
        vmovups   1088+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm14 #1452.46
        vrcp14pd  %zmm4, %zmm6                                  #1432.17
        vfpclasspd $94, %zmm5, %k2                              #1431.24
        vmovups   896+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm5 #1468.55
        vrndscalepd $88, {sae}, %zmm6, %zmm7                    #1433.17
        vmovups   576+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm6 #1449.14
        vfmsub213pd {rn-sae}, %zmm9, %zmm7, %zmm4               #1437.12
        vcmppd    $17, {sae}, %zmm8, %zmm7, %k1                 #1435.19
        vfmadd231pd {rn-sae}, %zmm4, %zmm12, %zmm6              #1449.14
        vmovups   640+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm12 #1454.54
        vfmadd231pd {rn-sae}, %zmm4, %zmm11, %zmm1              #1447.14
        vmulpd    {rn-sae}, %zmm4, %zmm4, %zmm0                 #1448.15
        vpsrlq    $48, %zmm7, %zmm10                            #1438.42
        vmovups   832+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm7 #1459.14
        vmulpd    {rn-sae}, %zmm0, %zmm0, %zmm8                 #1463.15
        vfmadd213pd {rn-sae}, %zmm6, %zmm0, %zmm1               #1460.14
        vaddpd    {rn-sae}, %zmm9, %zmm3, %zmm3{%k1}            #1440.18
        vpermt2pd 64+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm10, %zmm2 #1441.289
        vpermt2pd 192+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm10, %zmm15 #1442.289
        vmovups   960+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm10 #1461.55
        vfmadd231pd {rn-sae}, %zmm3, %zmm13, %zmm2              #1451.13
        vmovups   768+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm13 #1456.54
        vmovups   704+__svml_dlog10_ha_data_internal_avx512(%rip), %zmm9 #1458.14
        vfmadd213pd {rn-sae}, %zmm15, %zmm14, %zmm3             #1453.13
        vfmadd231pd {rn-sae}, %zmm4, %zmm13, %zmm7              #1459.14
        vfmadd231pd {rn-sae}, %zmm4, %zmm12, %zmm9              #1458.14
        vmovaps   %zmm10, %zmm11                                #1462.15
        vfmadd213pd {rn-sae}, %zmm2, %zmm4, %zmm11              #1462.15
        vfmadd213pd {rn-sae}, %zmm7, %zmm0, %zmm9               #1464.14
        vsubpd    {rn-sae}, %zmm2, %zmm11, %zmm0                #1465.19
        vfmadd213pd {rn-sae}, %zmm9, %zmm8, %zmm1               #1467.14
        vxorpd    %zmm0, %zmm0, %zmm0{%k2}                      #1466.21
        vfmadd213pd {rn-sae}, %zmm5, %zmm4, %zmm1               #1469.15
        vfmsub231pd {rn-sae}, %zmm10, %zmm4, %zmm0              #1470.17
        vaddpd    {rn-sae}, %zmm0, %zmm3, %zmm2                 #1471.15
        vfmadd213pd {rn-sae}, %zmm2, %zmm4, %zmm1               #1473.15
        vaddpd    {rn-sae}, %zmm1, %zmm11, %zmm0                #1474.16
        ret                                                     #1479.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_log108_ha_z0,@function
	.size	__svml_log108_ha_z0,.-__svml_log108_ha_z0
..LN__svml_log108_ha_z0.4:
	.data
# -- End  __svml_log108_ha_z0
	.text
.L_2__routine_start___svml_log101_ha_ex_5:
# -- Begin  __svml_log101_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log101_ha_ex
# --- __svml_log101_ha_ex(__m128d)
__svml_log101_ha_ex:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1791.1
        .byte     15                                            #1880.546
        .byte     30                                            #1880.546
        .byte     250                                           #1880.546
	.cfi_startproc
..___tag_value___svml_log101_ha_ex.74:
..L75:
                                                         #1791.1
        pushq     %rbp                                          #1791.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1791.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1791.1
        subq      $128, %rsp                                    #1791.1
        movaps    %xmm0, %xmm5                                  #1837.12
        movsd     9024+__svml_dlog10_ha_data_internal(%rip), %xmm11 #1831.15
        psrlq     $20, %xmm5                                    #1837.12
        movsd     9088+__svml_dlog10_ha_data_internal(%rip), %xmm1 #1830.12
        andps     %xmm0, %xmm11                                 #1831.15
        orps      %xmm1, %xmm11                                 #1832.15
        lea       -4221880+__svml_dlog10_ha_data_internal(%rip), %rdx #1854.17
        cvtpd2ps  %xmm11, %xmm2                                 #1835.53
        movlhps   %xmm2, %xmm2                                  #1835.37
        movaps    %xmm0, %xmm9                                  #1839.14
        rcpps     %xmm2, %xmm3                                  #1835.26
        cmpltsd   9152+__svml_dlog10_ha_data_internal(%rip), %xmm9 #1839.14
        cvtps2pd  %xmm3, %xmm12                                 #1835.13
        movsd     9344+__svml_dlog10_ha_data_internal(%rip), %xmm10 #1841.14
        movaps    %xmm0, %xmm8                                  #1840.14
        mulsd     %xmm10, %xmm12                                #1842.17
        cmpnlesd  9216+__svml_dlog10_ha_data_internal(%rip), %xmm8 #1840.14
        movups    .L_2il0floatpacket.28(%rip), %xmm4            #1843.103
        orps      %xmm8, %xmm9                                  #1845.14
        addpd     %xmm4, %xmm12                                 #1843.293
        movmskpd  %xmm9, %ecx                                   #1847.40
        movsd     9280+__svml_dlog10_ha_data_internal(%rip), %xmm1 #1849.120
        subpd     %xmm4, %xmm12                                 #1843.343
        andps     %xmm11, %xmm1                                 #1849.120
        pshufd    $85, %xmm5, %xmm6                             #1838.13
        subsd     %xmm1, %xmm11                                 #1849.174
        mulsd     %xmm12, %xmm1                                 #1849.227
        mulsd     %xmm12, %xmm11                                #1849.278
        subsd     %xmm10, %xmm1                                 #1849.324
        pshufd    $0, %xmm6, %xmm7                              #1844.37
        psrlq     $40, %xmm12                                   #1851.16
        movsd     8320+__svml_dlog10_ha_data_internal(%rip), %xmm2 #1866.26
        addsd     %xmm11, %xmm1                                 #1849.367
        cvtdq2pd  %xmm7, %xmm4                                  #1844.20
        mulsd     %xmm1, %xmm2                                  #1866.26
        movd      %xmm12, %eax                                  #1854.117
        movsd     8448+__svml_dlog10_ha_data_internal(%rip), %xmm13 #1867.26
        movaps    %xmm1, %xmm15                                 #1868.13
        mulsd     %xmm1, %xmm13                                 #1867.26
        addsd     8384+__svml_dlog10_ha_data_internal(%rip), %xmm2 #1866.14
        mulsd     %xmm1, %xmm15                                 #1868.13
        addsd     8512+__svml_dlog10_ha_data_internal(%rip), %xmm13 #1867.14
        mulsd     %xmm15, %xmm2                                 #1871.25
        movaps    %xmm4, %xmm14                                 #1858.25
        addsd     %xmm13, %xmm2                                 #1871.13
        mulsd     9472+__svml_dlog10_ha_data_internal(%rip), %xmm14 #1858.25
        mulsd     9536+__svml_dlog10_ha_data_internal(%rip), %xmm4 #1859.25
        mulsd     %xmm15, %xmm2                                 #1873.25
        movslq    %eax, %rax                                    #1854.106
        movsd     8576+__svml_dlog10_ha_data_internal(%rip), %xmm10 #1869.26
        mulsd     %xmm1, %xmm10                                 #1869.26
        addsd     -8(%rdx,%rax), %xmm14                         #1858.13
        addsd     (%rdx,%rax), %xmm4                            #1859.13
        addsd     8640+__svml_dlog10_ha_data_internal(%rip), %xmm10 #1869.14
        movaps    %xmm14, %xmm3                                 #1870.13
        addsd     %xmm10, %xmm2                                 #1873.13
        addsd     %xmm1, %xmm3                                  #1870.13
        mulsd     %xmm1, %xmm2                                  #1874.13
        movaps    %xmm3, %xmm13                                 #1872.13
        subsd     %xmm14, %xmm13                                #1872.13
        subsd     %xmm13, %xmm1                                 #1875.13
        addsd     %xmm1, %xmm4                                  #1876.13
        addsd     %xmm2, %xmm4                                  #1877.13
        addsd     %xmm3, %xmm4                                  #1878.14
        andl      $1, %ecx                                      #1847.90
        jne       ..B6.3        # Prob 5%                       #1880.52
                                # LOE rbx r12 r13 r14 r15 ecx xmm0 xmm4
..B6.2:                         # Preds ..B6.4 ..B6.1
                                # Execution count [1.00e+00]
        movaps    %xmm4, %xmm0                                  #1883.12
        movq      %rbp, %rsp                                    #1883.12
        popq      %rbp                                          #1883.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1883.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm0, (%rsp)                                 #1880.160
        movsd     %xmm4, 64(%rsp)                               #1880.233
        jne       ..B6.6        # Prob 5%                       #1880.374
                                # LOE rbx r12 r13 r14 r15
..B6.4:                         # Preds ..B6.6 ..B6.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm4                               #1880.627
        jmp       ..B6.2        # Prob 100%                     #1880.627
                                # LOE rbx r12 r13 r14 r15 xmm4
..B6.6:                         # Preds ..B6.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1880.546
        lea       64(%rsp), %rsi                                #1880.546
..___tag_value___svml_log101_ha_ex.83:
#       __svml_dlog10_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog10_ha_cout_rare_internal           #1880.546
..___tag_value___svml_log101_ha_ex.84:
        jmp       ..B6.4        # Prob 100%                     #1880.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_log101_ha_ex,@function
	.size	__svml_log101_ha_ex,.-__svml_log101_ha_ex
..LN__svml_log101_ha_ex.5:
	.data
# -- End  __svml_log101_ha_ex
	.text
.L_2__routine_start___svml_log102_ha_e9_6:
# -- Begin  __svml_log102_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log102_ha_e9
# --- __svml_log102_ha_e9(__m128d)
__svml_log102_ha_e9:
# parameter 1: %xmm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1888.1
        .byte     15                                            #1977.546
        .byte     30                                            #1977.546
        .byte     250                                           #1977.546
	.cfi_startproc
..___tag_value___svml_log102_ha_e9.86:
..L87:
                                                         #1888.1
        pushq     %rbp                                          #1888.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1888.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1888.1
        subq      $192, %rsp                                    #1888.1
        movaps    %xmm0, %xmm5                                  #1934.18
        movups    9024+__svml_dlog10_ha_data_internal(%rip), %xmm1 #1928.15
        psrlq     $20, %xmm5                                    #1934.18
        andps     %xmm0, %xmm1                                  #1928.15
        lea       -4221888+__svml_dlog10_ha_data_internal(%rip), %rsi #1951.67
        orps      9088+__svml_dlog10_ha_data_internal(%rip), %xmm1 #1929.15
        movaps    %xmm0, %xmm8                                  #1936.14
        cvtpd2ps  %xmm1, %xmm2                                  #1932.53
        cmpltpd   9152+__svml_dlog10_ha_data_internal(%rip), %xmm8 #1936.14
        movlhps   %xmm2, %xmm2                                  #1932.37
        movaps    %xmm0, %xmm7                                  #1937.14
        rcpps     %xmm2, %xmm3                                  #1932.26
        cmpnlepd  9216+__svml_dlog10_ha_data_internal(%rip), %xmm7 #1937.14
        cvtps2pd  %xmm3, %xmm4                                  #1932.13
        movups    9344+__svml_dlog10_ha_data_internal(%rip), %xmm9 #1938.45
        orps      %xmm7, %xmm8                                  #1942.14
        mulpd     %xmm9, %xmm4                                  #1939.17
        movmskpd  %xmm8, %edx                                   #1944.40
        movups    9280+__svml_dlog10_ha_data_internal(%rip), %xmm10 #1945.50
        andps     %xmm1, %xmm10                                 #1946.120
        roundpd   $0, %xmm4, %xmm4                              #1940.13
        subpd     %xmm10, %xmm1                                 #1946.174
        mulpd     %xmm4, %xmm10                                 #1946.227
        mulpd     %xmm4, %xmm1                                  #1946.278
        subpd     %xmm9, %xmm10                                 #1946.324
        pshufd    $221, %xmm5, %xmm6                            #1935.19
        psrlq     $40, %xmm4                                    #1948.22
        cvtdq2pd  %xmm6, %xmm14                                 #1941.20
        addpd     %xmm10, %xmm1                                 #1946.367
        movd      %xmm4, %eax                                   #1951.144
        movups    9472+__svml_dlog10_ha_data_internal(%rip), %xmm12 #1955.25
        movaps    %xmm1, %xmm5                                  #1967.13
        mulpd     %xmm14, %xmm12                                #1955.25
        movslq    %eax, %rax                                    #1951.67
        pextrd    $2, %xmm4, %ecx                               #1951.315
        movslq    %ecx, %rcx                                    #1951.238
        movups    8320+__svml_dlog10_ha_data_internal(%rip), %xmm15 #1963.26
        mulpd     %xmm1, %xmm15                                 #1963.26
        movups    (%rsi,%rax), %xmm2                            #1951.67
        movups    (%rsi,%rcx), %xmm11                           #1951.238
        movaps    %xmm2, %xmm4                                  #1951.364
        unpcklpd  %xmm11, %xmm4                                 #1951.364
        addpd     %xmm12, %xmm4                                 #1955.13
        addpd     8384+__svml_dlog10_ha_data_internal(%rip), %xmm15 #1963.14
        addpd     %xmm4, %xmm5                                  #1967.13
        movaps    %xmm1, %xmm12                                 #1965.13
        mulpd     %xmm1, %xmm12                                 #1965.13
        unpckhpd  %xmm11, %xmm2                                 #1951.405
        movups    8448+__svml_dlog10_ha_data_internal(%rip), %xmm11 #1964.26
        mulpd     %xmm1, %xmm11                                 #1964.26
        mulpd     %xmm12, %xmm15                                #1968.25
        addpd     8512+__svml_dlog10_ha_data_internal(%rip), %xmm11 #1964.14
        movups    8576+__svml_dlog10_ha_data_internal(%rip), %xmm3 #1966.26
        mulpd     %xmm1, %xmm3                                  #1966.26
        addpd     %xmm15, %xmm11                                #1968.13
        addpd     8640+__svml_dlog10_ha_data_internal(%rip), %xmm3 #1966.14
        mulpd     %xmm11, %xmm12                                #1970.25
        movups    9536+__svml_dlog10_ha_data_internal(%rip), %xmm13 #1954.45
        mulpd     %xmm13, %xmm14                                #1956.25
        addpd     %xmm12, %xmm3                                 #1970.13
        addpd     %xmm14, %xmm2                                 #1956.13
        mulpd     %xmm1, %xmm3                                  #1971.13
        movaps    %xmm5, %xmm13                                 #1969.13
        subpd     %xmm4, %xmm13                                 #1969.13
        subpd     %xmm13, %xmm1                                 #1972.13
        addpd     %xmm1, %xmm2                                  #1973.13
        addpd     %xmm2, %xmm3                                  #1974.13
        addpd     %xmm3, %xmm5                                  #1975.14
        testl     %edx, %edx                                    #1977.52
        jne       ..B7.3        # Prob 5%                       #1977.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm5
..B7.2:                         # Preds ..B7.9 ..B7.1
                                # Execution count [1.00e+00]
        movaps    %xmm5, %xmm0                                  #1980.12
        movq      %rbp, %rsp                                    #1980.12
        popq      %rbp                                          #1980.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1980.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.3:                         # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #1977.197
        movups    %xmm5, 128(%rsp)                              #1977.270
                                # LOE rbx r12 r13 r14 r15 edx
..B7.6:                         # Preds ..B7.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1977.454
        movq      %r12, 8(%rsp)                                 #1977.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1977.454
        movq      %r13, (%rsp)                                  #1977.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1977.454
                                # LOE rbx r12 r14 r15 r13d
..B7.7:                         # Preds ..B7.8 ..B7.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1977.517
        jc        ..B7.10       # Prob 5%                       #1977.517
                                # LOE rbx r12 r14 r15 r13d
..B7.8:                         # Preds ..B7.10 ..B7.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1977.470
        cmpl      $2, %r12d                                     #1977.465
        jl        ..B7.7        # Prob 82%                      #1977.465
                                # LOE rbx r12 r14 r15 r13d
..B7.9:                         # Preds ..B7.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm5                              #1977.675
        jmp       ..B7.2        # Prob 100%                     #1977.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm5
..B7.10:                        # Preds ..B7.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1977.546
        lea       128(%rsp,%r12,8), %rsi                        #1977.546
..___tag_value___svml_log102_ha_e9.104:
#       __svml_dlog10_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog10_ha_cout_rare_internal           #1977.546
..___tag_value___svml_log102_ha_e9.105:
        jmp       ..B7.8        # Prob 100%                     #1977.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log102_ha_e9,@function
	.size	__svml_log102_ha_e9,.-__svml_log102_ha_e9
..LN__svml_log102_ha_e9.6:
	.data
# -- End  __svml_log102_ha_e9
	.text
.L_2__routine_start___svml_log102_ha_l9_7:
# -- Begin  __svml_log102_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log102_ha_l9
# --- __svml_log102_ha_l9(__m128d)
__svml_log102_ha_l9:
# parameter 1: %xmm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1985.1
        .byte     15                                            #2074.546
        .byte     30                                            #2074.546
        .byte     250                                           #2074.546
	.cfi_startproc
..___tag_value___svml_log102_ha_l9.107:
..L108:
                                                        #1985.1
        pushq     %rbp                                          #1985.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1985.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1985.1
        subq      $192, %rsp                                    #1985.1
        vmovapd   %xmm0, %xmm2                                  #1985.1
        vandpd    9024+__svml_dlog10_ha_data_internal(%rip), %xmm2, %xmm3 #2025.15
        vpsrlq    $20, %xmm2, %xmm9                             #2031.14
        vorpd     9088+__svml_dlog10_ha_data_internal(%rip), %xmm3, %xmm1 #2026.15
        lea       -4221888+__svml_dlog10_ha_data_internal(%rip), %rsi #2048.67
        vcvtpd2ps %xmm1, %xmm4                                  #2029.53
        vcmpltpd  9152+__svml_dlog10_ha_data_internal(%rip), %xmm2, %xmm11 #2033.14
        vcmpnlepd 9216+__svml_dlog10_ha_data_internal(%rip), %xmm2, %xmm12 #2034.14
        vmovlhps  %xmm4, %xmm4, %xmm5                           #2029.37
        vorpd     %xmm12, %xmm11, %xmm13                        #2039.14
        vrcpps    %xmm5, %xmm6                                  #2029.26
        vmovmskpd %xmm13, %edx                                  #2041.40
        vmovupd   9344+__svml_dlog10_ha_data_internal(%rip), %xmm14 #2035.45
        vpshufd   $221, %xmm9, %xmm10                           #2032.15
        vcvtps2pd %xmm6, %xmm7                                  #2029.13
        vcvtdq2pd %xmm10, %xmm0                                 #2038.20
        vmulpd    %xmm14, %xmm7, %xmm8                          #2036.17
        vroundpd  $0, %xmm8, %xmm8                              #2037.13
        vpsrlq    $40, %xmm8, %xmm15                            #2045.18
        vmovd     %xmm15, %eax                                  #2048.144
        vmovupd   8320+__svml_dlog10_ha_data_internal(%rip), %xmm10 #2054.55
        vmovupd   8448+__svml_dlog10_ha_data_internal(%rip), %xmm6 #2056.55
        vpextrd   $2, %xmm15, %ecx                              #2048.315
        vfmsub213pd %xmm14, %xmm8, %xmm1                        #2043.13
        vmovupd   8576+__svml_dlog10_ha_data_internal(%rip), %xmm9 #2058.55
        movslq    %eax, %rax                                    #2048.67
        movslq    %ecx, %rcx                                    #2048.238
        vmulpd    %xmm1, %xmm1, %xmm8                           #2062.13
        vfmadd213pd 8512+__svml_dlog10_ha_data_internal(%rip), %xmm1, %xmm6 #2061.14
        vmovupd   (%rsi,%rax), %xmm3                            #2048.67
        vmovupd   (%rsi,%rcx), %xmm4                            #2048.238
        vunpcklpd %xmm4, %xmm3, %xmm7                           #2048.364
        vunpckhpd %xmm4, %xmm3, %xmm5                           #2048.405
        vfmadd231pd 9472+__svml_dlog10_ha_data_internal(%rip), %xmm0, %xmm7 #2052.13
        vfmadd213pd 8384+__svml_dlog10_ha_data_internal(%rip), %xmm1, %xmm10 #2060.14
        vfmadd213pd 8640+__svml_dlog10_ha_data_internal(%rip), %xmm1, %xmm9 #2063.14
        vfmadd132pd 9536+__svml_dlog10_ha_data_internal(%rip), %xmm5, %xmm0 #2053.13
        vaddpd    %xmm7, %xmm1, %xmm13                          #2064.13
        vfmadd213pd %xmm6, %xmm8, %xmm10                        #2065.13
        vsubpd    %xmm7, %xmm13, %xmm11                         #2066.13
        vfmadd213pd %xmm9, %xmm8, %xmm10                        #2067.13
        vmulpd    %xmm10, %xmm1, %xmm12                         #2068.13
        vsubpd    %xmm11, %xmm1, %xmm1                          #2069.13
        vaddpd    %xmm1, %xmm0, %xmm0                           #2070.13
        vaddpd    %xmm0, %xmm12, %xmm14                         #2071.13
        vaddpd    %xmm14, %xmm13, %xmm0                         #2072.14
        testl     %edx, %edx                                    #2074.52
        jne       ..B8.3        # Prob 5%                       #2074.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm2
..B8.2:                         # Preds ..B8.3 ..B8.9 ..B8.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #2077.12
        popq      %rbp                                          #2077.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2077.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.3:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm2, 64(%rsp)                               #2074.197
        vmovupd   %xmm0, 128(%rsp)                              #2074.270
        je        ..B8.2        # Prob 95%                      #2074.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B8.6:                         # Preds ..B8.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #2074.454
        movq      %r12, 8(%rsp)                                 #2074.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #2074.454
        movq      %r13, (%rsp)                                  #2074.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #2074.454
                                # LOE rbx r12 r14 r15 r13d
..B8.7:                         # Preds ..B8.8 ..B8.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #2074.517
        jc        ..B8.10       # Prob 5%                       #2074.517
                                # LOE rbx r12 r14 r15 r13d
..B8.8:                         # Preds ..B8.10 ..B8.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #2074.470
        cmpl      $2, %r12d                                     #2074.465
        jl        ..B8.7        # Prob 82%                      #2074.465
                                # LOE rbx r12 r14 r15 r13d
..B8.9:                         # Preds ..B8.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #2074.675
        jmp       ..B8.2        # Prob 100%                     #2074.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B8.10:                        # Preds ..B8.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #2074.546
        lea       128(%rsp,%r12,8), %rsi                        #2074.546
..___tag_value___svml_log102_ha_l9.125:
#       __svml_dlog10_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog10_ha_cout_rare_internal           #2074.546
..___tag_value___svml_log102_ha_l9.126:
        jmp       ..B8.8        # Prob 100%                     #2074.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log102_ha_l9,@function
	.size	__svml_log102_ha_l9,.-__svml_log102_ha_l9
..LN__svml_log102_ha_l9.7:
	.data
# -- End  __svml_log102_ha_l9
	.text
.L_2__routine_start___svml_log104_ha_l9_8:
# -- Begin  __svml_log104_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log104_ha_l9
# --- __svml_log104_ha_l9(__m256d)
__svml_log104_ha_l9:
# parameter 1: %ymm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2082.1
        .byte     15                                            #2171.552
        .byte     30                                            #2171.552
        .byte     250                                           #2171.552
	.cfi_startproc
..___tag_value___svml_log104_ha_l9.128:
..L129:
                                                        #2082.1
        pushq     %rbp                                          #2082.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2082.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2082.1
        subq      $192, %rsp                                    #2082.1
        lea       -4221888+__svml_dlog10_ha_data_internal(%rip), %r8 #2145.68
        vmovapd   %ymm0, %ymm3                                  #2082.1
        vandpd    9024+__svml_dlog10_ha_data_internal(%rip), %ymm3, %ymm4 #2122.15
        vorpd     9088+__svml_dlog10_ha_data_internal(%rip), %ymm4, %ymm2 #2123.15
        vcvtpd2ps %ymm2, %xmm5                                  #2126.40
        vpsrlq    $20, %ymm3, %ymm7                             #2128.14
        vmovupd   9344+__svml_dlog10_ha_data_internal(%rip), %ymm15 #2132.48
        vrcpps    %xmm5, %xmm6                                  #2126.29
        vcmplt_oqpd 9152+__svml_dlog10_ha_data_internal(%rip), %ymm3, %ymm12 #2130.14
        vcmpnle_uqpd 9216+__svml_dlog10_ha_data_internal(%rip), %ymm3, %ymm13 #2131.14
        vcvtps2pd %xmm6, %ymm9                                  #2126.13
        vmulpd    %ymm15, %ymm9, %ymm10                         #2133.17
        vroundpd  $0, %ymm10, %ymm0                             #2134.13
        vpsrlq    $40, %ymm0, %ymm4                             #2142.18
        vfmsub213pd %ymm15, %ymm0, %ymm2                        #2140.13
        vorpd     %ymm13, %ymm12, %ymm14                        #2136.14
        vmulpd    %ymm2, %ymm2, %ymm15                          #2159.13
        vmovmskpd %ymm14, %eax                                  #2138.40
        vextractf128 $1, %ymm4, %xmm5                           #2145.567
        vmovd     %xmm4, %edx                                   #2145.145
        vmovd     %xmm5, %esi                                   #2145.548
        vpextrd   $2, %xmm4, %ecx                               #2145.345
        vpextrd   $2, %xmm5, %edi                               #2145.753
        movslq    %edx, %rdx                                    #2145.68
        movslq    %ecx, %rcx                                    #2145.268
        movslq    %esi, %rsi                                    #2145.471
        movslq    %edi, %rdi                                    #2145.676
        vmovupd   8448+__svml_dlog10_ha_data_internal(%rip), %ymm4 #2153.58
        vmovupd   (%r8,%rcx), %xmm9                             #2145.268
        vmovupd   8576+__svml_dlog10_ha_data_internal(%rip), %ymm5 #2155.58
        vmovupd   (%r8,%rdi), %xmm12                            #2145.676
        vfmadd213pd 8512+__svml_dlog10_ha_data_internal(%rip), %ymm2, %ymm4 #2158.14
        vfmadd213pd 8640+__svml_dlog10_ha_data_internal(%rip), %ymm2, %ymm5 #2160.14
        vextractf128 $1, %ymm7, %xmm8                           #2129.125
        vshufps   $221, %xmm8, %xmm7, %xmm11                    #2129.33
        vmovupd   (%r8,%rdx), %xmm8                             #2145.68
        vcvtdq2pd %xmm11, %ymm1                                 #2135.20
        vmovupd   (%r8,%rsi), %xmm11                            #2145.471
        vunpcklpd %xmm9, %xmm8, %xmm6                           #2145.881
        vunpcklpd %xmm12, %xmm11, %xmm7                         #2145.914
        vunpckhpd %xmm9, %xmm8, %xmm10                          #2145.1008
        vunpckhpd %xmm12, %xmm11, %xmm13                        #2145.1041
        vinsertf128 $1, %xmm7, %ymm6, %ymm0                     #2145.835
        vmovupd   8320+__svml_dlog10_ha_data_internal(%rip), %ymm6 #2151.58
        vfmadd231pd 9472+__svml_dlog10_ha_data_internal(%rip), %ymm1, %ymm0 #2149.13
        vfmadd213pd 8384+__svml_dlog10_ha_data_internal(%rip), %ymm2, %ymm6 #2157.14
        vaddpd    %ymm0, %ymm2, %ymm7                           #2161.13
        vfmadd213pd %ymm4, %ymm15, %ymm6                        #2162.13
        vsubpd    %ymm0, %ymm7, %ymm0                           #2163.13
        vfmadd213pd %ymm5, %ymm15, %ymm6                        #2164.13
        vmulpd    %ymm6, %ymm2, %ymm4                           #2165.13
        vsubpd    %ymm0, %ymm2, %ymm2                           #2166.13
        vinsertf128 $1, %xmm13, %ymm10, %ymm14                  #2145.962
        vfmadd132pd 9536+__svml_dlog10_ha_data_internal(%rip), %ymm14, %ymm1 #2150.13
        vaddpd    %ymm2, %ymm1, %ymm1                           #2167.13
        vaddpd    %ymm1, %ymm4, %ymm0                           #2168.13
        vaddpd    %ymm0, %ymm7, %ymm0                           #2169.14
        testl     %eax, %eax                                    #2171.52
        jne       ..B9.3        # Prob 5%                       #2171.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm3
..B9.2:                         # Preds ..B9.3 ..B9.9 ..B9.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #2174.12
        popq      %rbp                                          #2174.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2174.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.3:                         # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm3, 64(%rsp)                               #2171.200
        vmovupd   %ymm0, 128(%rsp)                              #2171.276
        je        ..B9.2        # Prob 95%                      #2171.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B9.6:                         # Preds ..B9.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #2171.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B9.13:                        # Preds ..B9.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B9.7:                         # Preds ..B9.8 ..B9.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #2171.523
        jc        ..B9.10       # Prob 5%                       #2171.523
                                # LOE rbx r12 r14 r15 r13d
..B9.8:                         # Preds ..B9.10 ..B9.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #2171.476
        cmpl      $4, %r12d                                     #2171.471
        jl        ..B9.7        # Prob 82%                      #2171.471
                                # LOE rbx r12 r14 r15 r13d
..B9.9:                         # Preds ..B9.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #2171.684
        jmp       ..B9.2        # Prob 100%                     #2171.684
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B9.10:                        # Preds ..B9.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #2171.552
        lea       128(%rsp,%r12,8), %rsi                        #2171.552
..___tag_value___svml_log104_ha_l9.146:
#       __svml_dlog10_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog10_ha_cout_rare_internal           #2171.552
..___tag_value___svml_log104_ha_l9.147:
        jmp       ..B9.8        # Prob 100%                     #2171.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log104_ha_l9,@function
	.size	__svml_log104_ha_l9,.-__svml_log104_ha_l9
..LN__svml_log104_ha_l9.8:
	.data
# -- End  __svml_log104_ha_l9
	.text
.L_2__routine_start___svml_dlog10_ha_cout_rare_internal_9:
# -- Begin  __svml_dlog10_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dlog10_ha_cout_rare_internal
	.globl __svml_dlog10_ha_cout_rare_internal
# --- __svml_dlog10_ha_cout_rare_internal(const double *, double *)
__svml_dlog10_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1697.1
        .byte     15                                            #1778.19
        .byte     30                                            #1778.19
        .byte     250                                           #1778.19
	.cfi_startproc
..___tag_value___svml_dlog10_ha_cout_rare_internal.149:
..L150:
                                                        #1697.1
        xorl      %eax, %eax                                    #1708.14
        movzwl    6(%rdi), %edx                                 #1709.35
        andl      $32752, %edx                                  #1709.35
        cmpl      $32752, %edx                                  #1709.61
        je        ..B10.12      # Prob 16%                      #1709.61
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B10.2:                        # Preds ..B10.1
                                # Execution count [8.40e-01]
        movsd     (%rdi), %xmm2                                 #1711.19
        xorl      %ecx, %ecx                                    #1712.13
        movsd     %xmm2, -8(%rsp)                               #1711.13
        movzwl    -2(%rsp), %edx                                #1713.39
        testl     $32752, %edx                                  #1713.39
        jne       ..B10.4       # Prob 50%                      #1713.61
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm2
..B10.3:                        # Preds ..B10.2
                                # Execution count [4.20e-01]
        movsd     1600+_vmldLgHATab(%rip), %xmm0                #1715.49
        movl      $-60, %ecx                                    #1716.17
        mulsd     %xmm0, %xmm2                                  #1715.49
        movsd     %xmm2, -8(%rsp)                               #1715.17
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm2
..B10.4:                        # Preds ..B10.3 ..B10.2
                                # Execution count [8.40e-01]
        movsd     1608+_vmldLgHATab(%rip), %xmm0                #1718.39
        comisd    %xmm0, %xmm2                                  #1718.39
        jbe       ..B10.8       # Prob 50%                      #1718.39
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm0 xmm2
..B10.5:                        # Preds ..B10.4
                                # Execution count [4.20e-01]
        movaps    %xmm2, %xmm1                                  #1720.34
        subsd     .L_2il0floatpacket.102(%rip), %xmm1           #1720.34
        movsd     %xmm1, -16(%rsp)                              #1721.17
        andb      $127, -9(%rsp)                                #1722.38
        movsd     -16(%rsp), %xmm0                              #1723.22
        comisd    1592+_vmldLgHATab(%rip), %xmm0                #1723.48
        jbe       ..B10.7       # Prob 50%                      #1723.48
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm1 xmm2
..B10.6:                        # Preds ..B10.5
                                # Execution count [2.10e-01]
        movsd     %xmm2, -16(%rsp)                              #1729.21
        pxor      %xmm7, %xmm7                                  #1726.35
        movzwl    -10(%rsp), %edi                               #1730.42
        lea       _vmldLgHATab(%rip), %r10                      #1733.44
        andl      $-32753, %edi                                 #1730.42
        addl      $16368, %edi                                  #1730.42
        movw      %di, -10(%rsp)                                #1730.42
        movsd     -16(%rsp), %xmm3                              #1731.38
        movaps    %xmm3, %xmm1                                  #1731.59
        movaps    %xmm3, %xmm2                                  #1738.59
        movsd     1688+_vmldLgHATab(%rip), %xmm5                #1744.126
        movzwl    -2(%rsp), %edx                                #1725.61
        andl      $32752, %edx                                  #1725.61
        addsd     1576+_vmldLgHATab(%rip), %xmm1                #1731.59
        addsd     1584+_vmldLgHATab(%rip), %xmm2                #1738.59
        movsd     %xmm1, -24(%rsp)                              #1731.21
        movl      -24(%rsp), %r8d                               #1732.55
        movsd     %xmm2, -24(%rsp)                              #1738.21
        andl      $127, %r8d                                    #1732.88
        movsd     -24(%rsp), %xmm8                              #1739.38
        movsd     1560+_vmldLgHATab(%rip), %xmm9                #1727.64
        movsd     1568+_vmldLgHATab(%rip), %xmm0                #1728.64
        shrl      $4, %edx                                      #1725.61
        subsd     1584+_vmldLgHATab(%rip), %xmm8                #1739.63
        lea       (%r8,%r8,2), %r9d                             #1733.60
        movsd     (%r10,%r9,8), %xmm6                           #1733.44
        lea       -1023(%rcx,%rdx), %ecx                        #1725.61
        cvtsi2sd  %ecx, %xmm7                                   #1726.35
        subsd     %xmm8, %xmm3                                  #1740.42
        mulsd     %xmm6, %xmm8                                  #1741.53
        mulsd     %xmm7, %xmm9                                  #1727.64
        subsd     1624+_vmldLgHATab(%rip), %xmm8                #1741.80
        mulsd     %xmm3, %xmm6                                  #1742.44
        mulsd     %xmm0, %xmm7                                  #1728.64
        addsd     8(%r10,%r9,8), %xmm9                          #1736.49
        addsd     16(%r10,%r9,8), %xmm7                         #1737.49
        addsd     %xmm8, %xmm9                                  #1745.48
        movaps    %xmm8, %xmm4                                  #1743.42
        addsd     %xmm6, %xmm4                                  #1743.42
        mulsd     %xmm4, %xmm5                                  #1744.126
        addsd     1680+_vmldLgHATab(%rip), %xmm5                #1744.145
        mulsd     %xmm4, %xmm5                                  #1744.166
        addsd     1672+_vmldLgHATab(%rip), %xmm5                #1744.187
        mulsd     %xmm4, %xmm5                                  #1744.208
        addsd     1664+_vmldLgHATab(%rip), %xmm5                #1744.229
        mulsd     %xmm4, %xmm5                                  #1744.250
        addsd     1656+_vmldLgHATab(%rip), %xmm5                #1744.271
        mulsd     %xmm4, %xmm5                                  #1744.292
        addsd     1648+_vmldLgHATab(%rip), %xmm5                #1744.313
        mulsd     %xmm4, %xmm5                                  #1744.334
        addsd     1640+_vmldLgHATab(%rip), %xmm5                #1744.355
        mulsd     %xmm4, %xmm5                                  #1744.376
        addsd     1632+_vmldLgHATab(%rip), %xmm5                #1744.397
        mulsd     %xmm5, %xmm8                                  #1748.52
        mulsd     %xmm6, %xmm5                                  #1746.61
        addsd     %xmm5, %xmm7                                  #1746.61
        addsd     %xmm6, %xmm7                                  #1747.48
        addsd     %xmm7, %xmm8                                  #1748.52
        addsd     %xmm8, %xmm9                                  #1749.48
        movsd     %xmm9, (%rsi)                                 #1749.23
        ret                                                     #1749.23
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.7:                        # Preds ..B10.5
                                # Execution count [2.10e-01]
        movsd     1624+_vmldLgHATab(%rip), %xmm0                #1753.53
        mulsd     %xmm0, %xmm1                                  #1753.53
        movsd     1688+_vmldLgHATab(%rip), %xmm2                #1754.107
        mulsd     %xmm1, %xmm2                                  #1754.126
        addsd     1680+_vmldLgHATab(%rip), %xmm2                #1754.145
        mulsd     %xmm1, %xmm2                                  #1754.166
        addsd     1672+_vmldLgHATab(%rip), %xmm2                #1754.187
        mulsd     %xmm1, %xmm2                                  #1754.208
        addsd     1664+_vmldLgHATab(%rip), %xmm2                #1754.229
        mulsd     %xmm1, %xmm2                                  #1754.250
        addsd     1656+_vmldLgHATab(%rip), %xmm2                #1754.271
        mulsd     %xmm1, %xmm2                                  #1754.292
        addsd     1648+_vmldLgHATab(%rip), %xmm2                #1754.313
        mulsd     %xmm1, %xmm2                                  #1754.334
        addsd     1640+_vmldLgHATab(%rip), %xmm2                #1754.355
        mulsd     %xmm1, %xmm2                                  #1754.376
        addsd     1632+_vmldLgHATab(%rip), %xmm2                #1754.397
        mulsd     %xmm1, %xmm2                                  #1755.40
        addsd     %xmm1, %xmm2                                  #1756.40
        movsd     %xmm2, (%rsi)                                 #1757.23
        ret                                                     #1757.23
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.8:                        # Preds ..B10.4
                                # Execution count [4.20e-01]
        ucomisd   %xmm0, %xmm2                                  #1762.44
        jp        ..B10.9       # Prob 0%                       #1762.44
        je        ..B10.11      # Prob 16%                      #1762.44
                                # LOE rbx rbp rsi r12 r13 r14 r15 xmm0
..B10.9:                        # Preds ..B10.8
                                # Execution count [3.53e-01]
        divsd     %xmm0, %xmm0                                  #1769.83
        movsd     %xmm0, (%rsi)                                 #1769.23
        movl      $1, %eax                                      #1770.21
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.10:                       # Preds ..B10.9
                                # Execution count [1.00e+00]
        ret                                                     #1786.12
                                # LOE
..B10.11:                       # Preds ..B10.8
                                # Execution count [6.72e-02]: Infreq
        movsd     1616+_vmldLgHATab(%rip), %xmm1                #1764.46
        movl      $2, %eax                                      #1765.21
        xorps     .L_2il0floatpacket.101(%rip), %xmm1           #1764.46
        divsd     %xmm0, %xmm1                                  #1764.84
        movsd     %xmm1, (%rsi)                                 #1764.23
        ret                                                     #1764.23
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.12:                       # Preds ..B10.1
                                # Execution count [1.60e-01]: Infreq
        movb      7(%rdi), %dl                                  #1776.39
        andb      $-128, %dl                                    #1776.39
        cmpb      $-128, %dl                                    #1776.61
        je        ..B10.14      # Prob 16%                      #1776.61
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B10.13:                       # Preds ..B10.15 ..B10.12 ..B10.14
                                # Execution count [1.54e-01]: Infreq
        movsd     (%rdi), %xmm0                                 #1783.26
        mulsd     %xmm0, %xmm0                                  #1783.33
        movsd     %xmm0, (%rsi)                                 #1783.19
        ret                                                     #1783.19
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.14:                       # Preds ..B10.12
                                # Execution count [2.56e-02]: Infreq
        testl     $1048575, 4(%rdi)                             #1776.89
        jne       ..B10.13      # Prob 50%                      #1776.121
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B10.15:                       # Preds ..B10.14
                                # Execution count [1.28e-02]: Infreq
        cmpl      $0, (%rdi)                                    #1776.180
        jne       ..B10.13      # Prob 50%                      #1776.180
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B10.16:                       # Preds ..B10.15
                                # Execution count [6.40e-03]: Infreq
        movsd     1608+_vmldLgHATab(%rip), %xmm0                #1778.79
        movl      $1, %eax                                      #1779.17
        divsd     %xmm0, %xmm0                                  #1778.79
        movsd     %xmm0, (%rsi)                                 #1778.19
        ret                                                     #1778.19
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_dlog10_ha_cout_rare_internal,@function
	.size	__svml_dlog10_ha_cout_rare_internal,.-__svml_dlog10_ha_cout_rare_internal
..LN__svml_dlog10_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dlog10_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_dlog10_ha_data_internal_avx512
	.globl __svml_dlog10_ha_data_internal_avx512
__svml_dlog10_ha_data_internal_avx512:
	.long	0
	.long	0
	.long	721420288
	.long	3214603769
	.long	3590979584
	.long	3215601833
	.long	1438908416
	.long	3216186160
	.long	948305920
	.long	3216559896
	.long	2869821440
	.long	3216915393
	.long	516521984
	.long	3217142759
	.long	2145648640
	.long	3217304702
	.long	733741056
	.long	1069546492
	.long	3513843712
	.long	1069249052
	.long	3459645440
	.long	1068963280
	.long	1085800448
	.long	1068688295
	.long	3613786112
	.long	1068347678
	.long	1803419648
	.long	1067836310
	.long	3436707840
	.long	1067234191
	.long	930611200
	.long	1066155272
	.long	0
	.long	0
	.long	3686878645
	.long	3175924212
	.long	2120733314
	.long	1025513021
	.long	1829252717
	.long	3176941056
	.long	3391913567
	.long	3176268582
	.long	1445614576
	.long	3174694461
	.long	577138096
	.long	1029194402
	.long	717064894
	.long	1025503698
	.long	1625232067
	.long	1029570781
	.long	2107125367
	.long	1029044389
	.long	1740576090
	.long	1029619435
	.long	4015256301
	.long	3177184346
	.long	1535607671
	.long	1029013126
	.long	2000266743
	.long	1028805283
	.long	761604295
	.long	1028127597
	.long	803304759
	.long	1025708071
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
	.long	1072168960
	.long	0
	.long	1072168960
	.long	0
	.long	1072168960
	.long	0
	.long	1072168960
	.long	0
	.long	1072168960
	.long	0
	.long	1072168960
	.long	0
	.long	1072168960
	.long	0
	.long	1072168960
	.long	675808112
	.long	1068024536
	.long	675808112
	.long	1068024536
	.long	675808112
	.long	1068024536
	.long	675808112
	.long	1068024536
	.long	675808112
	.long	1068024536
	.long	675808112
	.long	1068024536
	.long	675808112
	.long	1068024536
	.long	675808112
	.long	1068024536
	.long	2516752404
	.long	3215710221
	.long	2516752404
	.long	3215710221
	.long	2516752404
	.long	3215710221
	.long	2516752404
	.long	3215710221
	.long	2516752404
	.long	3215710221
	.long	2516752404
	.long	3215710221
	.long	2516752404
	.long	3215710221
	.long	2516752404
	.long	3215710221
	.long	4085995682
	.long	1068483574
	.long	4085995682
	.long	1068483574
	.long	4085995682
	.long	1068483574
	.long	4085995682
	.long	1068483574
	.long	4085995682
	.long	1068483574
	.long	4085995682
	.long	1068483574
	.long	4085995682
	.long	1068483574
	.long	4085995682
	.long	1068483574
	.long	879025280
	.long	3216148390
	.long	879025280
	.long	3216148390
	.long	879025280
	.long	3216148390
	.long	879025280
	.long	3216148390
	.long	879025280
	.long	3216148390
	.long	879025280
	.long	3216148390
	.long	879025280
	.long	3216148390
	.long	879025280
	.long	3216148390
	.long	2004821977
	.long	1068907618
	.long	2004821977
	.long	1068907618
	.long	2004821977
	.long	1068907618
	.long	2004821977
	.long	1068907618
	.long	2004821977
	.long	1068907618
	.long	2004821977
	.long	1068907618
	.long	2004821977
	.long	1068907618
	.long	2004821977
	.long	1068907618
	.long	356255395
	.long	3216755579
	.long	356255395
	.long	3216755579
	.long	356255395
	.long	3216755579
	.long	356255395
	.long	3216755579
	.long	356255395
	.long	3216755579
	.long	356255395
	.long	3216755579
	.long	356255395
	.long	3216755579
	.long	356255395
	.long	3216755579
	.long	1668235916
	.long	1069713319
	.long	1668235916
	.long	1069713319
	.long	1668235916
	.long	1069713319
	.long	1668235916
	.long	1069713319
	.long	1668235916
	.long	1069713319
	.long	1668235916
	.long	1069713319
	.long	1668235916
	.long	1069713319
	.long	1668235916
	.long	1069713319
	.long	354870491
	.long	3217804155
	.long	354870491
	.long	3217804155
	.long	354870491
	.long	3217804155
	.long	354870491
	.long	3217804155
	.long	354870491
	.long	3217804155
	.long	354870491
	.long	3217804155
	.long	354870491
	.long	3217804155
	.long	354870491
	.long	3217804155
	.long	2867927077
	.long	1013556733
	.long	2867927077
	.long	1013556733
	.long	2867927077
	.long	1013556733
	.long	2867927077
	.long	1013556733
	.long	2867927077
	.long	1013556733
	.long	2867927077
	.long	1013556733
	.long	2867927077
	.long	1013556733
	.long	2867927077
	.long	1013556733
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	3296479949
	.long	1031700412
	.long	3296479949
	.long	1031700412
	.long	3296479949
	.long	1031700412
	.long	3296479949
	.long	1031700412
	.long	3296479949
	.long	1031700412
	.long	3296479949
	.long	1031700412
	.long	3296479949
	.long	1031700412
	.long	3296479949
	.long	1031700412
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
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
	.type	__svml_dlog10_ha_data_internal_avx512,@object
	.size	__svml_dlog10_ha_data_internal_avx512,1344
	.align 64
	.hidden __svml_dlog10_ha_data_internal
	.globl __svml_dlog10_ha_data_internal
__svml_dlog10_ha_data_internal:
	.long	1190572160
	.long	3228777073
	.long	3860447744
	.long	3189665916
	.long	1198987632
	.long	3228777077
	.long	3228397393
	.long	3189668294
	.long	1168821104
	.long	3228777081
	.long	637194908
	.long	3189668066
	.long	1100245360
	.long	3228777085
	.long	133785915
	.long	3189667542
	.long	993432048
	.long	3228777089
	.long	3017828443
	.long	3189667588
	.long	848551680
	.long	3228777093
	.long	560605178
	.long	3189668289
	.long	665773664
	.long	3228777097
	.long	3849538177
	.long	3189667548
	.long	445266208
	.long	3228777101
	.long	3804908986
	.long	3189667886
	.long	187196464
	.long	3228777105
	.long	3516966431
	.long	3189667858
	.long	4186697744
	.long	3228777108
	.long	3052190817
	.long	3189667809
	.long	3854000384
	.long	3228777112
	.long	1554781574
	.long	3189667427
	.long	3484235504
	.long	3228777116
	.long	4153839410
	.long	3189667384
	.long	3077565856
	.long	3228777120
	.long	4000768869
	.long	3189667904
	.long	2634153136
	.long	3228777124
	.long	4244738174
	.long	3189668339
	.long	2154157984
	.long	3228777128
	.long	3014247306
	.long	3189667771
	.long	1637739952
	.long	3228777132
	.long	1906045981
	.long	3189667650
	.long	1085057568
	.long	3228777136
	.long	3987700171
	.long	3189668284
	.long	496268368
	.long	3228777140
	.long	1674280096
	.long	3189667373
	.long	4166496080
	.long	3228777143
	.long	2078815235
	.long	3189667744
	.long	3505961600
	.long	3228777147
	.long	4286802640
	.long	3189667683
	.long	2809786672
	.long	3228777151
	.long	1913564473
	.long	3189667639
	.long	2078124752
	.long	3228777155
	.long	2693682663
	.long	3189667702
	.long	1311128304
	.long	3228777159
	.long	1012063542
	.long	3189668158
	.long	508948832
	.long	3228777163
	.long	2489929809
	.long	3189667978
	.long	3966704144
	.long	3228777166
	.long	2165614611
	.long	3189667408
	.long	3094609184
	.long	3228777170
	.long	2488783422
	.long	3189667472
	.long	2187779856
	.long	3228777174
	.long	2723126436
	.long	3189667428
	.long	1246363792
	.long	3228777178
	.long	3017034520
	.long	3189668359
	.long	270507744
	.long	3228777182
	.long	1972662349
	.long	3189667544
	.long	3555324736
	.long	3228777185
	.long	898998658
	.long	3189668182
	.long	2511025072
	.long	3228777189
	.long	2160314027
	.long	3189667608
	.long	1432719952
	.long	3228777193
	.long	1911887828
	.long	3189668033
	.long	320552432
	.long	3228777197
	.long	3280422502
	.long	3189667818
	.long	3469631920
	.long	3228777200
	.long	3027884278
	.long	3189668109
	.long	2290165072
	.long	3228777204
	.long	3517602960
	.long	3189668197
	.long	1077259536
	.long	3228777208
	.long	3013130823
	.long	3189668046
	.long	4126022080
	.long	3228777211
	.long	3235689898
	.long	3189667740
	.long	2846656704
	.long	3228777215
	.long	87643117
	.long	3189667953
	.long	1534268448
	.long	3228777219
	.long	754951962
	.long	3189668360
	.long	188994208
	.long	3228777223
	.long	3023581101
	.long	3189668100
	.long	3105937296
	.long	3228777226
	.long	1490072819
	.long	3189668276
	.long	1695298320
	.long	3228777230
	.long	3738440902
	.long	3189668304
	.long	252178944
	.long	3228777234
	.long	897194307
	.long	3189667388
	.long	3071679952
	.long	3228777237
	.long	195484330
	.long	3189668024
	.long	1563999488
	.long	3228777241
	.long	3795554301
	.long	3189668248
	.long	24236736
	.long	3228777245
	.long	2858806924
	.long	3189668212
	.long	2747490080
	.long	3228777248
	.long	4259795627
	.long	3189667532
	.long	1143955184
	.long	3228777252
	.long	4279885499
	.long	3189667764
	.long	3803696144
	.long	3228777255
	.long	3997664578
	.long	3189667698
	.long	2136907056
	.long	3228777259
	.long	2176158532
	.long	3189667869
	.long	438683136
	.long	3228777263
	.long	227964261
	.long	3189667893
	.long	3004118816
	.long	3228777266
	.long	2296676690
	.long	3189667896
	.long	1243405872
	.long	3228777270
	.long	2079834385
	.long	3189667894
	.long	3746604496
	.long	3228777273
	.long	3850151037
	.long	3189668185
	.long	1923904960
	.long	3228777277
	.long	574763554
	.long	3189667699
	.long	70398640
	.long	3228777281
	.long	3827982506
	.long	3189667452
	.long	2481176176
	.long	3228777284
	.long	1146808857
	.long	3189667869
	.long	566425600
	.long	3228777288
	.long	4238446104
	.long	3189668130
	.long	2916203392
	.long	3228777291
	.long	3553887815
	.long	3189667582
	.long	940696080
	.long	3228777295
	.long	1221192380
	.long	3189668153
	.long	3229958720
	.long	3228777298
	.long	2271570828
	.long	3189667556
	.long	1194176400
	.long	3228777302
	.long	3002536483
	.long	3189667850
	.long	3423402736
	.long	3228777305
	.long	2434838684
	.long	3189667608
	.long	1327821424
	.long	3228777309
	.long	2687777298
	.long	3189667396
	.long	3497484640
	.long	3228777312
	.long	2749657917
	.long	3189668082
	.long	1342574720
	.long	3228777316
	.long	3002546917
	.long	3189668115
	.long	3453142464
	.long	3228777319
	.long	3498283957
	.long	3189667970
	.long	1239368816
	.long	3228777323
	.long	455121273
	.long	3189667422
	.long	3291303200
	.long	3228777326
	.long	2621841620
	.long	3189667932
	.long	3012883008
	.long	3228777333
	.long	2362501462
	.long	3189667975
	.long	2618787376
	.long	3228777340
	.long	1091754251
	.long	3189667994
	.long	2109911280
	.long	3228777347
	.long	193599334
	.long	3189668101
	.long	1487139360
	.long	3228777354
	.long	3757852586
	.long	3189667728
	.long	751346048
	.long	3228777361
	.long	1167306858
	.long	3189667607
	.long	4198363056
	.long	3228777367
	.long	3727503052
	.long	3189667447
	.long	3239110320
	.long	3228777374
	.long	4101491224
	.long	3189667535
	.long	2169399936
	.long	3228777381
	.long	1682828411
	.long	3189668088
	.long	990067152
	.long	3228777388
	.long	325154957
	.long	3189667406
	.long	3996905008
	.long	3228777394
	.long	333920478
	.long	3189668041
	.long	2600795440
	.long	3228777401
	.long	1868291498
	.long	3189667430
	.long	1097513040
	.long	3228777408
	.long	2092796598
	.long	3189667816
	.long	3782823440
	.long	3228777414
	.long	2342635878
	.long	3189667569
	.long	2067581456
	.long	3228777421
	.long	2489432283
	.long	3189667708
	.long	247535056
	.long	3228777428
	.long	2233925548
	.long	3189668023
	.long	2618423584
	.long	3228777434
	.long	2884435678
	.long	3189668162
	.long	591076000
	.long	3228777441
	.long	2793733516
	.long	3189667544
	.long	2756182032
	.long	3228777447
	.long	726899401
	.long	3189668194
	.long	524554032
	.long	3228777454
	.long	3888914666
	.long	3189668216
	.long	2486865376
	.long	3228777460
	.long	2627594277
	.long	3189667420
	.long	53912208
	.long	3228777467
	.long	2333314786
	.long	3189667677
	.long	1816352000
	.long	3228777473
	.long	860249135
	.long	3189668031
	.long	3479932544
	.long	3228777479
	.long	1333576769
	.long	3189667862
	.long	750393936
	.long	3228777486
	.long	3863808803
	.long	3189667876
	.long	2218370608
	.long	3228777492
	.long	988274049
	.long	3189667895
	.long	3589587648
	.long	3228777498
	.long	1363020542
	.long	3189667588
	.long	569762768
	.long	3228777505
	.long	2860785679
	.long	3189668116
	.long	1749508368
	.long	3228777511
	.long	3454164214
	.long	3189667505
	.long	2834527744
	.long	3228777517
	.long	551781933
	.long	3189668193
	.long	3825484512
	.long	3228777523
	.long	2204991099
	.long	3189668076
	.long	428068000
	.long	3228777530
	.long	330816187
	.long	3189667835
	.long	1232862576
	.long	3228777536
	.long	1875584004
	.long	3189667900
	.long	1945543984
	.long	3228777542
	.long	3147029736
	.long	3189667368
	.long	2566748560
	.long	3228777548
	.long	2759544833
	.long	3189667927
	.long	3097106128
	.long	3228777554
	.long	2378335007
	.long	3189667440
	.long	3537239968
	.long	3228777560
	.long	59134449
	.long	3189667766
	.long	3887767008
	.long	3228777566
	.long	2237380018
	.long	3189668142
	.long	4149297872
	.long	3228777572
	.long	2899689733
	.long	3189667692
	.long	27469632
	.long	3228777579
	.long	2818642709
	.long	3189667832
	.long	112815152
	.long	3228777585
	.long	4092579806
	.long	3189667420
	.long	110959312
	.long	3228777591
	.long	936570796
	.long	3189668056
	.long	22488368
	.long	3228777597
	.long	365917996
	.long	3189668022
	.long	4142949952
	.long	3228777602
	.long	3149756965
	.long	3189667408
	.long	3882983968
	.long	3228777608
	.long	4081670258
	.long	3189668028
	.long	3538126528
	.long	3228777614
	.long	1719873156
	.long	3189668130
	.long	3108940752
	.long	3228777620
	.long	3284322898
	.long	3189668289
	.long	2595984192
	.long	3228777626
	.long	1642430589
	.long	3189668010
	.long	1999808880
	.long	3228777632
	.long	4232900950
	.long	3189667360
	.long	1320961392
	.long	3228777638
	.long	346567365
	.long	3189667513
	.long	559982960
	.long	3228777644
	.long	2138178062
	.long	3189668130
	.long	4012376832
	.long	3228777649
	.long	2536292360
	.long	3189667756
	.long	3088739088
	.long	3228777655
	.long	2310947316
	.long	3189668224
	.long	2084562592
	.long	3228777661
	.long	719337470
	.long	3189667732
	.long	1000367760
	.long	3228777667
	.long	2925814745
	.long	3189668182
	.long	4131637328
	.long	3228777672
	.long	2769058114
	.long	3189668120
	.long	2888947152
	.long	3228777678
	.long	2397746692
	.long	3189667888
	.long	1567770080
	.long	3228777684
	.long	1319666757
	.long	3189667549
	.long	168606816
	.long	3228777690
	.long	3249166601
	.long	3189667815
	.long	2986920608
	.long	3228777695
	.long	3117551188
	.long	3189667844
	.long	1433268080
	.long	3228777701
	.long	2392446108
	.long	3189668088
	.long	4098070400
	.long	3228777706
	.long	1952984424
	.long	3189667965
	.long	2391874944
	.long	3228777712
	.long	3260016359
	.long	3189667565
	.long	610126416
	.long	3228777718
	.long	1206683346
	.long	3189668275
	.long	3048265088
	.long	3228777723
	.long	3467821979
	.long	3189668220
	.long	1116824880
	.long	3228777729
	.long	3688655521
	.long	3189667898
	.long	3406204528
	.long	3228777734
	.long	3255713182
	.long	3189667556
	.long	1326929264
	.long	3228777740
	.long	200527713
	.long	3189667591
	.long	3469389248
	.long	3228777745
	.long	1129326388
	.long	3189667868
	.long	1244101248
	.long	3228777751
	.long	308865650
	.long	3189668017
	.long	3241447056
	.long	3228777756
	.long	3090939005
	.long	3189667668
	.long	871935152
	.long	3228777762
	.long	3265000937
	.long	3189667670
	.long	2725939152
	.long	3228777767
	.long	4229796659
	.long	3189668178
	.long	213959504
	.long	3228777773
	.long	1991139447
	.long	3189667724
	.long	1926361824
	.long	3228777778
	.long	2528808771
	.long	3189668376
	.long	3568606000
	.long	3228777783
	.long	4260639448
	.long	3189667546
	.long	846147968
	.long	3228777789
	.long	806895635
	.long	3189668179
	.long	2349341824
	.long	3228777794
	.long	3346322191
	.long	3189667388
	.long	3783635920
	.long	3228777799
	.long	175610890
	.long	3189667587
	.long	854474928
	.long	3228777805
	.long	2938776958
	.long	3189668104
	.long	2152201728
	.long	3228777810
	.long	2503702909
	.long	3189668036
	.long	3382253648
	.long	3228777815
	.long	990471545
	.long	3189668020
	.long	250064432
	.long	3228777821
	.long	2919909380
	.long	3189667876
	.long	1345966144
	.long	3228777826
	.long	3002840896
	.long	3189668306
	.long	2375385488
	.long	3228777831
	.long	3730416038
	.long	3189667426
	.long	3338712928
	.long	3228777836
	.long	1372953258
	.long	3189667506
	.long	4236335536
	.long	3228777841
	.long	3486643335
	.long	3189668352
	.long	773669728
	.long	3228777847
	.long	3527808638
	.long	3189667828
	.long	1541030400
	.long	3228777852
	.long	1323886332
	.long	3189667450
	.long	2243827264
	.long	3228777857
	.long	3473574637
	.long	3189667708
	.long	2882434080
	.long	3228777862
	.long	2116524762
	.long	3189667453
	.long	3457221328
	.long	3228777867
	.long	3472145296
	.long	3189668357
	.long	3968556352
	.long	3228777872
	.long	1399438939
	.long	3189668087
	.long	121835984
	.long	3228777878
	.long	1630440586
	.long	3189667769
	.long	507355824
	.long	3228777883
	.long	414821867
	.long	3189668165
	.long	830506528
	.long	3228777888
	.long	1651489076
	.long	3189667856
	.long	1091642960
	.long	3228777893
	.long	1914960493
	.long	3189667544
	.long	1291116960
	.long	3228777898
	.long	1585226677
	.long	3189668172
	.long	1429277424
	.long	3228777903
	.long	1458536815
	.long	3189667992
	.long	1506470256
	.long	3228777908
	.long	5177277
	.long	3189667794
	.long	1523038448
	.long	3228777913
	.long	2100423580
	.long	3189667910
	.long	1479322112
	.long	3228777918
	.long	2766296967
	.long	3189668266
	.long	1375658528
	.long	3228777923
	.long	3908110271
	.long	3189667373
	.long	1212382096
	.long	3228777928
	.long	1728734206
	.long	3189667440
	.long	989824464
	.long	3228777933
	.long	2721564364
	.long	3189668188
	.long	708314544
	.long	3228777938
	.long	1548992985
	.long	3189667813
	.long	368178464
	.long	3228777943
	.long	102841028
	.long	3189668008
	.long	4264706992
	.long	3228777947
	.long	975947285
	.long	3189667700
	.long	3808286304
	.long	3228777952
	.long	3025865597
	.long	3189668024
	.long	3294201856
	.long	3228777957
	.long	905114809
	.long	3189668054
	.long	2722769184
	.long	3228777962
	.long	3281428766
	.long	3189667672
	.long	2094301216
	.long	3228777967
	.long	2831959372
	.long	3189668375
	.long	1409108384
	.long	3228777972
	.long	2859726358
	.long	3189667900
	.long	667498528
	.long	3228777977
	.long	3483539044
	.long	3189668146
	.long	4164744336
	.long	3228777981
	.long	643592870
	.long	3189667709
	.long	3311214096
	.long	3228777986
	.long	3888135264
	.long	3189667701
	.long	2402175552
	.long	3228777991
	.long	1565708850
	.long	3189668342
	.long	1437926768
	.long	3228777996
	.long	3880143694
	.long	3189667559
	.long	418763344
	.long	3228778001
	.long	2177554551
	.long	3189667774
	.long	3639945856
	.long	3228778005
	.long	632683322
	.long	3189668317
	.long	2511830656
	.long	3228778010
	.long	1595152623
	.long	3189668068
	.long	1329673632
	.long	3228778015
	.long	1921601627
	.long	3189668075
	.long	93761104
	.long	3228778020
	.long	1757445293
	.long	3189668004
	.long	3099344416
	.long	3228778024
	.long	1435123721
	.long	3189667691
	.long	1756770768
	.long	3228778029
	.long	783914014
	.long	3189667649
	.long	361287040
	.long	3228778034
	.long	2235532382
	.long	3189667506
	.long	3208137904
	.long	3228778038
	.long	1904486204
	.long	3189667498
	.long	1707663968
	.long	3228778043
	.long	41908474
	.long	3189667890
	.long	155105584
	.long	3228778048
	.long	2107716085
	.long	3189668380
	.long	2845700992
	.long	3228778052
	.long	3156927676
	.long	3189667488
	.long	1189784368
	.long	3228778057
	.long	4047204189
	.long	3189668063
	.long	3777557072
	.long	3228778061
	.long	577628716
	.long	3189667418
	.long	2019349136
	.long	3228778066
	.long	2610467089
	.long	3189667920
	.long	210390496
	.long	3228778071
	.long	2766766141
	.long	3189668118
	.long	2645909040
	.long	3228778075
	.long	3243727052
	.long	3189668176
	.long	736228768
	.long	3228778080
	.long	3122320416
	.long	3189668130
	.long	3071540880
	.long	3228778084
	.long	3131821814
	.long	3189668172
	.long	1062165440
	.long	3228778089
	.long	3213377517
	.long	3189667895
	.long	3298289744
	.long	3228778093
	.long	2564668383
	.long	3189667569
	.long	1190229968
	.long	3228778098
	.long	3238426468
	.long	3189668376
	.long	3328169632
	.long	3228778102
	.long	1168702877
	.long	3189667512
	.long	1122421104
	.long	3228778107
	.long	19789062
	.long	3189667604
	.long	3163164128
	.long	3228778111
	.long	1630085837
	.long	3189667685
	.long	860707408
	.long	3228778116
	.long	4114762240
	.long	3189667464
	.long	2805226992
	.long	3228778120
	.long	18835043
	.long	3189667485
	.long	407027936
	.long	3228778125
	.long	1254882039
	.long	3189668241
	.long	2256282720
	.long	3228778129
	.long	1578671063
	.long	3189668264
	.long	4058260128
	.long	3228778133
	.long	2087871538
	.long	3189668330
	.long	1518227216
	.long	3228778138
	.long	1874562670
	.long	3189667466
	.long	3226351136
	.long	3228778142
	.long	1975306748
	.long	3189668156
	.long	592928208
	.long	3228778147
	.long	2027557844
	.long	3189668223
	.long	2208122192
	.long	3228778151
	.long	3385698680
	.long	3189668056
	.long	3777193280
	.long	3228778155
	.long	2370310143
	.long	3189667586
	.long	1005399968
	.long	3228778160
	.long	259325820
	.long	3189668365
	.long	2482901056
	.long	3228778164
	.long	780809723
	.long	3189667443
	.long	3914951728
	.long	3228778168
	.long	212773867
	.long	3189667618
	.long	1006805600
	.long	3228778173
	.long	951469551
	.long	3189668212
	.long	2348616576
	.long	3228778177
	.long	2086655720
	.long	3189668128
	.long	3645635056
	.long	3228778181
	.long	2113468742
	.long	3189667868
	.long	603109872
	.long	3228778186
	.long	1652875995
	.long	3189667443
	.long	1811190160
	.long	3228778190
	.long	2629878773
	.long	3189668368
	.long	2975121696
	.long	3228778194
	.long	346123600
	.long	3189667455
	.long	4095115920
	.long	3228778198
	.long	3526019824
	.long	3189667996
	.long	876415552
	.long	3228778203
	.long	1439312045
	.long	3189667395
	.long	1909163616
	.long	3228778207
	.long	3349683269
	.long	3189668324
	.long	2898599856
	.long	3228778211
	.long	649479362
	.long	3189667358
	.long	3844929728
	.long	3228778215
	.long	2676997587
	.long	3189668158
	.long	453390032
	.long	3228778220
	.long	848895008
	.long	3189668036
	.long	1314117952
	.long	3228778224
	.long	3492148440
	.long	3189668045
	.long	2132347376
	.long	3228778228
	.long	281429102
	.long	3189667715
	.long	2908278048
	.long	3228778232
	.long	2940038679
	.long	3189667953
	.long	3642108336
	.long	3228778236
	.long	3909547671
	.long	3189667808
	.long	39067888
	.long	3228778241
	.long	3920787602
	.long	3189668378
	.long	689286912
	.long	3228778245
	.long	1690865565
	.long	3189667500
	.long	1297992288
	.long	3228778249
	.long	2169866692
	.long	3189667738
	.long	1865376896
	.long	3228778253
	.long	967083596
	.long	3189667983
	.long	2391632256
	.long	3228778257
	.long	1361458469
	.long	3189668348
	.long	2876948576
	.long	3228778261
	.long	32611984
	.long	3189667893
	.long	3321514720
	.long	3228778265
	.long	2193827755
	.long	3189667454
	.long	3725518240
	.long	3228778269
	.long	2245419299
	.long	3189668375
	.long	4089145456
	.long	3228778273
	.long	3497133018
	.long	3189668150
	.long	117614048
	.long	3228778278
	.long	175818393
	.long	3189668353
	.long	401042352
	.long	3228778282
	.long	3839976210
	.long	3189668213
	.long	644645552
	.long	3228778286
	.long	3041010480
	.long	3189667412
	.long	848604848
	.long	3228778290
	.long	2760940853
	.long	3189667832
	.long	1013100256
	.long	3228778294
	.long	2385564198
	.long	3189668140
	.long	1138310560
	.long	3228778298
	.long	2892472831
	.long	3189667574
	.long	1224413296
	.long	3228778302
	.long	781676890
	.long	3189667676
	.long	1271584832
	.long	3228778306
	.long	3578454272
	.long	3189667868
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
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
	.long	2256045239
	.long	3209413770
	.long	2491592457
	.long	3210460574
	.long	1804910333
	.long	3211053321
	.long	1914826022
	.long	3211505620
	.long	3803025062
	.long	3211872665
	.long	1674748349
	.long	3212097941
	.long	1798374224
	.long	3212322782
	.long	2768032015
	.long	3212547190
	.long	3136444289
	.long	3212771167
	.long	2855105875
	.long	3212915789
	.long	2332612951
	.long	3213027349
	.long	1363424900
	.long	3213138696
	.long	3437629661
	.long	3213249830
	.long	3435456176
	.long	3213360753
	.long	512327647
	.long	3213471466
	.long	2393978768
	.long	3213581968
	.long	3901767770
	.long	3213692261
	.long	4132692718
	.long	3213802346
	.long	1082284973
	.long	3213898832
	.long	2823056504
	.long	3213953667
	.long	366152305
	.long	3214008400
	.long	3959556563
	.long	3214063029
	.long	2367110801
	.long	3214117557
	.long	1523289056
	.long	3214171983
	.long	3058430271
	.long	3214226307
	.long	3839215
	.long	3214280531
	.long	2561657861
	.long	3214334653
	.long	3745193976
	.long	3214388675
	.long	853823952
	.long	3214442598
	.long	4062993129
	.long	3214496420
	.long	2064542556
	.long	3214550144
	.long	721479184
	.long	3214603769
	.long	1593203319
	.long	3214657295
	.long	1935571978
	.long	3214710723
	.long	3290896245
	.long	3214764053
	.long	2898068879
	.long	3214817286
	.long	2282560537
	.long	3214870422
	.long	2961513548
	.long	3214923461
	.long	1074417563
	.long	3214955210
	.long	2820079616
	.long	3214981633
	.long	1021551109
	.long	3215008009
	.long	716013687
	.long	3215034337
	.long	2641666110
	.long	3215060617
	.long	3237753164
	.long	3215086850
	.long	3234528909
	.long	3215113036
	.long	3358317786
	.long	3215139175
	.long	36575458
	.long	3215165268
	.long	2577785893
	.long	3215191313
	.long	3106717932
	.long	3215217312
	.long	2334256463
	.long	3215243265
	.long	967494986
	.long	3215269172
	.long	4004729818
	.long	3215295032
	.long	3555617588
	.long	3215320847
	.long	316038151
	.long	3215346617
	.long	3568120799
	.long	3215372340
	.long	1115499161
	.long	3215398019
	.long	2233009907
	.long	3215423652
	.long	3306979906
	.long	3215449240
	.long	720153405
	.long	3215474784
	.long	3736618996
	.long	3215500282
	.long	142096100
	.long	3215525737
	.long	3488599868
	.long	3215551146
	.long	1554858050
	.long	3215576512
	.long	3590975466
	.long	3215601833
	.long	1663752343
	.long	3215627111
	.long	721479184
	.long	3215652345
	.long	1414091172
	.long	3215677535
	.long	93191556
	.long	3215702682
	.long	1696976726
	.long	3215727785
	.long	2570390009
	.long	3215752845
	.long	3350046349
	.long	3215777862
	.long	374320308
	.long	3215802837
	.long	2863237644
	.long	3215827768
	.long	2853726453
	.long	3215852657
	.long	969442960
	.long	3215877504
	.long	2125826056
	.long	3215902308
	.long	2645217056
	.long	3215927070
	.long	3141783046
	.long	3215951790
	.long	4226570871
	.long	3215976468
	.long	3253764118
	.long	3215991848
	.long	999795016
	.long	3216004146
	.long	4242218727
	.long	3216016422
	.long	395520918
	.long	3216028679
	.long	2642482782
	.long	3216040914
	.long	2689550894
	.long	3216053129
	.long	831618312
	.long	3216065324
	.long	1657067230
	.long	3216077498
	.long	1162876950
	.long	3216089652
	.long	3934502846
	.long	3216101785
	.long	1671049587
	.long	3216113899
	.long	3250051821
	.long	3216125992
	.long	367745354
	.long	3216138066
	.long	1898814972
	.long	3216150119
	.long	3831632753
	.long	3216162152
	.long	2153169263
	.long	3216174166
	.long	1438937368
	.long	3216186160
	.long	1968099508
	.long	3216198134
	.long	4018444062
	.long	3216210088
	.long	3571427061
	.long	3216222023
	.long	902115705
	.long	3216233939
	.long	579197221
	.long	3216245835
	.long	2875053055
	.long	3216257711
	.long	3765767585
	.long	3216269568
	.long	3521071355
	.long	3216281406
	.long	2409382351
	.long	3216293225
	.long	697814500
	.long	3216305025
	.long	2947153405
	.long	3216316805
	.long	831995525
	.long	3216328567
	.long	3205527541
	.long	3216340309
	.long	1739796226
	.long	3216352033
	.long	990487678
	.long	3216363738
	.long	1217066237
	.long	3216375424
	.long	2677749815
	.long	3216387091
	.long	1334550573
	.long	3216398740
	.long	1738184714
	.long	3216410370
	.long	4143178441
	.long	3216421981
	.long	212908442
	.long	3216433575
	.long	3084380679
	.long	3216445149
	.long	123597797
	.long	3216456706
	.long	170206973
	.long	3216468244
	.long	3472736381
	.long	3216479763
	.long	1688602678
	.long	3216491265
	.long	3653922191
	.long	3216502748
	.long	1023779914
	.long	3216514214
	.long	2631975180
	.long	3216525661
	.long	131290539
	.long	3216537091
	.long	2353237313
	.long	3216548502
	.long	948324365
	.long	3216559896
	.long	450836243
	.long	3216571272
	.long	1098971024
	.long	3216582630
	.long	3129814575
	.long	3216593970
	.long	2484380169
	.long	3216605293
	.long	3692517238
	.long	3216616598
	.long	2693048988
	.long	3216627886
	.long	4013648344
	.long	3216639156
	.long	3590975466
	.long	3216650409
	.long	1655586284
	.long	3216661645
	.long	2731939102
	.long	3216672863
	.long	2753499254
	.long	3216684064
	.long	1947647495
	.long	3216695248
	.long	540719148
	.long	3216706415
	.long	3052977802
	.long	3216717564
	.long	1118752477
	.long	3216728697
	.long	3551215000
	.long	3216739812
	.long	1982647887
	.long	3216750911
	.long	929221623
	.long	3216761993
	.long	611131637
	.long	3216773058
	.long	1247571711
	.long	3216784106
	.long	3056740043
	.long	3216795137
	.long	1960877976
	.long	3216806152
	.long	2471177857
	.long	3216817150
	.long	507919781
	.long	3216828132
	.long	580346667
	.long	3216839097
	.long	2901768203
	.long	3216850045
	.long	3389566649
	.long	3216860977
	.long	2255137180
	.long	3216871893
	.long	4003893595
	.long	3216882792
	.long	255404805
	.long	3216893676
	.long	4103138823
	.long	3216904542
	.long	2869828094
	.long	3216915393
	.long	1057147997
	.long	3216926228
	.long	3165853160
	.long	3216937046
	.long	810881035
	.long	3216947849
	.long	2786128388
	.long	3216958635
	.long	704718315
	.long	3216969406
	.long	3358743942
	.long	3216980160
	.long	2359535366
	.long	3216990899
	.long	2202435985
	.long	3217001622
	.long	3086938536
	.long	3217012329
	.long	916690285
	.long	3217023021
	.long	2239683676
	.long	3217032432
	.long	2690954374
	.long	3217037762
	.long	4057646264
	.long	3217043084
	.long	2142368519
	.long	3217048399
	.long	1337238495
	.long	3217053706
	.long	1738982326
	.long	3217059005
	.long	3443904681
	.long	3217064296
	.long	2252923910
	.long	3217069580
	.long	2556476367
	.long	3217074856
	.long	154649628
	.long	3217080125
	.long	3732021368
	.long	3217085385
	.long	497923366
	.long	3217090639
	.long	3431084121
	.long	3217095884
	.long	4034990935
	.long	3217101122
	.long	2402663310
	.long	3217106353
	.long	2921687961
	.long	3217111576
	.long	1389319216
	.long	3217116792
	.long	2192350480
	.long	3217122000
	.long	1127247302
	.long	3217127201
	.long	2580018809
	.long	3217132394
	.long	2346350743
	.long	3217137580
	.long	516509563
	.long	3217142759
	.long	1475344637
	.long	3217147930
	.long	1017388536
	.long	3217153094
	.long	3526728381
	.long	3217158250
	.long	502171511
	.long	3217163400
	.long	622018689
	.long	3217168542
	.long	3974262445
	.long	3217173676
	.long	2056589177
	.long	3217178804
	.long	3546185020
	.long	3217183924
	.long	4234966846
	.long	3217189037
	.long	4209453505
	.long	3217194143
	.long	3555800579
	.long	3217199242
	.long	2359802410
	.long	3217204334
	.long	706894115
	.long	3217209419
	.long	2977120889
	.long	3217214496
	.long	665270808
	.long	3217219567
	.long	2445647873
	.long	3217224630
	.long	4107302903
	.long	3217229686
	.long	1438937368
	.long	3217234736
	.long	3113807214
	.long	3217239778
	.long	624953709
	.long	3217244814
	.long	60313751
	.long	1069756916
	.long	2039124234
	.long	1069751894
	.long	3639817142
	.long	1069746879
	.long	485812347
	.long	1069741872
	.long	1085767695
	.long	1069736871
	.long	1063773387
	.long	1069731877
	.long	339219334
	.long	1069726890
	.long	3126793337
	.long	1069721909
	.long	756610100
	.long	1069716936
	.long	1738980508
	.long	1069711969
	.long	1699638774
	.long	1069707009
	.long	559609852
	.long	1069702056
	.long	2535207687
	.long	1069697109
	.long	3253131575
	.long	1069692169
	.long	2635366323
	.long	1069687236
	.long	604213229
	.long	1069682310
	.long	1377255668
	.long	1069677390
	.long	582455508
	.long	1069672477
	.long	2438020609
	.long	1069667570
	.long	2572533958
	.long	1069662670
	.long	909853896
	.long	1069657777
	.long	1669112469
	.long	1069652890
	.long	479811889
	.long	1069648010
	.long	1561692097
	.long	1069643136
	.long	544859953
	.long	1069638269
	.long	1649656813
	.long	1069633408
	.long	506787744
	.long	1069628554
	.long	1337189126
	.long	1069623706
	.long	4067125179
	.long	1069618864
	.long	33219107
	.long	1069614030
	.long	2047222611
	.long	1069609201
	.long	1446341380
	.long	1069604379
	.long	2452971930
	.long	1069599563
	.long	699863591
	.long	1069594754
	.long	409986179
	.long	1069589951
	.long	1511626604
	.long	1069585154
	.long	3933354665
	.long	1069580363
	.long	3309054273
	.long	1069575579
	.long	3862823860
	.long	1069570801
	.long	1229105731
	.long	1069566030
	.long	3927521083
	.long	1069561264
	.long	3298097489
	.long	1069556505
	.long	3566038531
	.long	1069551752
	.long	733771779
	.long	1069546492
	.long	147741522
	.long	1069537011
	.long	941119220
	.long	1069527542
	.long	2976542023
	.long	1069518085
	.long	1822213927
	.long	1069508641
	.long	1636804890
	.long	1069499209
	.long	2284546202
	.long	1069489789
	.long	3630195051
	.long	1069480381
	.long	1244064508
	.long	1069470986
	.long	3581890024
	.long	1069461602
	.long	1920055674
	.long	1069452231
	.long	420362572
	.long	1069442872
	.long	3245124339
	.long	1069433524
	.long	1672262586
	.long	1069424189
	.long	4160075380
	.long	1069414865
	.long	1987496281
	.long	1069405554
	.long	3613830132
	.long	1069396254
	.long	319012134
	.long	1069386967
	.long	563343667
	.long	1069377691
	.long	4217685989
	.long	1069368426
	.long	2563457725
	.long	1069359174
	.long	4062436141
	.long	1069349933
	.long	4291983603
	.long	1069340704
	.long	3124914285
	.long	1069331487
	.long	434524433
	.long	1069322282
	.long	389557226
	.long	1069313088
	.long	2864265763
	.long	1069303905
	.long	3438410662
	.long	1069294734
	.long	1987192267
	.long	1069285575
	.long	2681248272
	.long	1069276427
	.long	1101749478
	.long	1069267291
	.long	1420266631
	.long	1069258166
	.long	3513866211
	.long	1069249052
	.long	2965108111
	.long	1069239950
	.long	3946945232
	.long	1069230859
	.long	2042852013
	.long	1069221780
	.long	1426691339
	.long	1069212712
	.long	1977810406
	.long	1069203655
	.long	3576005764
	.long	1069194609
	.long	1806553800
	.long	1069185575
	.long	845110407
	.long	1069176552
	.long	572806897
	.long	1069167540
	.long	871215109
	.long	1069158539
	.long	1622345234
	.long	1069149549
	.long	2708643653
	.long	1069140570
	.long	4012990793
	.long	1069131602
	.long	1123731696
	.long	1069122646
	.long	2514543080
	.long	1069113700
	.long	3774627456
	.long	1069104765
	.long	493612919
	.long	1069095842
	.long	1146452959
	.long	1069086929
	.long	1323620609
	.long	1069078027
	.long	910975575
	.long	1069069136
	.long	4089762193
	.long	1069060255
	.long	2156738208
	.long	1069051386
	.long	3588943832
	.long	1069042527
	.long	3978928661
	.long	1069033679
	.long	3214618865
	.long	1069024842
	.long	1184347908
	.long	1069016016
	.long	2071821874
	.long	1069007200
	.long	1471215619
	.long	1068998395
	.long	3567040008
	.long	1068989600
	.long	3954270794
	.long	1068980816
	.long	2523248584
	.long	1068972043
	.long	3459676924
	.long	1068963280
	.long	2359718512
	.long	1068954528
	.long	3409862494
	.long	1068945786
	.long	2207053396
	.long	1068937055
	.long	2938558446
	.long	1068928334
	.long	1202096535
	.long	1068919624
	.long	1185705549
	.long	1068910924
	.long	2782838657
	.long	1068902234
	.long	1592362481
	.long	1068893555
	.long	1803457173
	.long	1068884886
	.long	3310712729
	.long	1068876227
	.long	1714127196
	.long	1068867579
	.long	1204006779
	.long	1068858941
	.long	1676062187
	.long	1068850313
	.long	3026374166
	.long	1068841695
	.long	856424459
	.long	1068833088
	.long	3652963247
	.long	1068824490
	.long	2723236352
	.long	1068815903
	.long	2259754591
	.long	1068807326
	.long	2160422882
	.long	1068798759
	.long	2323505847
	.long	1068790202
	.long	2647626118
	.long	1068781655
	.long	3031762665
	.long	1068773118
	.long	3375249122
	.long	1068764591
	.long	3577772136
	.long	1068756074
	.long	3539369713
	.long	1068747567
	.long	3160429578
	.long	1068739070
	.long	2341687551
	.long	1068730583
	.long	984225923
	.long	1068722106
	.long	3284439142
	.long	1068713638
	.long	554163028
	.long	1068705181
	.long	1285444256
	.long	1068696733
	.long	1085767695
	.long	1068688295
	.long	4152891319
	.long	1068679866
	.long	1800008162
	.long	1068671448
	.long	2520515836
	.long	1068663039
	.long	1923243906
	.long	1068654640
	.long	4207288842
	.long	1068646250
	.long	687176001
	.long	1068637871
	.long	4152596484
	.long	1068629500
	.long	1623765359
	.long	1068621140
	.long	1596060423
	.long	1068612789
	.long	3975249632
	.long	1068604447
	.long	77489616
	.long	1068596116
	.long	2694095274
	.long	1068587793
	.long	3141865345
	.long	1068579480
	.long	1327852022
	.long	1068571177
	.long	1454392201
	.long	1068562883
	.long	3429171450
	.long	1068554598
	.long	2865222567
	.long	1068546323
	.long	3965826047
	.long	1068538057
	.long	2344639476
	.long	1068529801
	.long	2205565304
	.long	1068521554
	.long	3457847553
	.long	1068513316
	.long	1716070423
	.long	1068505088
	.long	2370117590
	.long	1068494794
	.long	3549949915
	.long	1068478374
	.long	2497601776
	.long	1068461973
	.long	3329575662
	.long	1068445590
	.long	1573040697
	.long	1068429226
	.long	1345699131
	.long	1068412880
	.long	2470881766
	.long	1068396552
	.long	477545290
	.long	1068380243
	.long	3780138808
	.long	1068363951
	.long	3613830132
	.long	1068347678
	.long	4099274231
	.long	1068331423
	.long	767774141
	.long	1068315187
	.long	2036114853
	.long	1068298968
	.long	3436756955
	.long	1068282767
	.long	502735958
	.long	1068266585
	.long	1652561629
	.long	1068250420
	.long	2420411682
	.long	1068234273
	.long	2635998437
	.long	1068218144
	.long	2129599017
	.long	1068202033
	.long	732052851
	.long	1068185940
	.long	2569726489
	.long	1068169864
	.long	3179609248
	.long	1068153806
	.long	2394212644
	.long	1068137766
	.long	46600658
	.long	1068121744
	.long	265354603
	.long	1068105739
	.long	2884636119
	.long	1068089751
	.long	3444184771
	.long	1068073781
	.long	1779250256
	.long	1068057829
	.long	2020590027
	.long	1068041894
	.long	4004532340
	.long	1068025976
	.long	3272973907
	.long	1068010076
	.long	3958279445
	.long	1067994193
	.long	1603410172
	.long	1067978328
	.long	341790679
	.long	1067962480
	.long	12404745
	.long	1067946649
	.long	454760348
	.long	1067930835
	.long	1508887391
	.long	1067915038
	.long	3015335440
	.long	1067899258
	.long	520204185
	.long	1067883496
	.long	2455010382
	.long	1067867750
	.long	71914560
	.long	1067852022
	.long	1803457173
	.long	1067836310
	.long	3197785334
	.long	1067820615
	.long	4098519806
	.long	1067804937
	.long	54818232
	.long	1067789277
	.long	3796209456
	.long	1067773632
	.long	2282918403
	.long	1067758005
	.long	3950504196
	.long	1067742394
	.long	55152364
	.long	1067726801
	.long	3328378382
	.long	1067711223
	.long	732385311
	.long	1067695663
	.long	704701956
	.long	1067680119
	.long	3093409710
	.long	1067664591
	.long	3452107773
	.long	1067649080
	.long	1629845684
	.long	1067633586
	.long	1771121268
	.long	1067618108
	.long	3725944001
	.long	1067602646
	.long	3049832979
	.long	1067587201
	.long	3888716792
	.long	1067571772
	.long	1799062323
	.long	1067556360
	.long	927741940
	.long	1067540964
	.long	1127129614
	.long	1067525584
	.long	2250066244
	.long	1067510220
	.long	4149857685
	.long	1067494872
	.long	2385305499
	.long	1067479541
	.long	1105606892
	.long	1067464226
	.long	330901792
	.long	1067447486
	.long	3134934760
	.long	1067416919
	.long	1744581096
	.long	1067386385
	.long	166917835
	.long	1067355883
	.long	2409924962
	.long	1067325412
	.long	3892547049
	.long	1067294973
	.long	34624089
	.long	1067264567
	.long	3436756955
	.long	1067234191
	.long	930630721
	.long	1067203848
	.long	823651223
	.long	1067173536
	.long	2834170318
	.long	1067143255
	.long	2386449519
	.long	1067113006
	.long	3495558248
	.long	1067082788
	.long	1587501032
	.long	1067052602
	.long	679083091
	.long	1067022447
	.long	493004867
	.long	1066992323
	.long	752825758
	.long	1066962230
	.long	1182960573
	.long	1066932168
	.long	1508676010
	.long	1066902137
	.long	1456087143
	.long	1066872137
	.long	752153937
	.long	1066842168
	.long	3419645073
	.long	1066812229
	.long	597265304
	.long	1066782322
	.long	604423098
	.long	1066752445
	.long	3171423441
	.long	1066722598
	.long	3734431730
	.long	1066692782
	.long	2025404984
	.long	1066662997
	.long	2072088478
	.long	1066633242
	.long	3608077800
	.long	1066603517
	.long	2072815517
	.long	1066573823
	.long	1496489755
	.long	1066544159
	.long	1615129006
	.long	1066514525
	.long	2165566144
	.long	1066484921
	.long	2885435163
	.long	1066455347
	.long	3513167923
	.long	1066425803
	.long	3281014557
	.long	1066390787
	.long	2604876893
	.long	1066331819
	.long	184567847
	.long	1066272911
	.long	4093204381
	.long	1066214061
	.long	930630721
	.long	1066155272
	.long	3068052312
	.long	1066096541
	.long	1403389294
	.long	1066037870
	.long	4015975920
	.long	1065979257
	.long	1806815988
	.long	1065920704
	.long	2858315029
	.long	1065862209
	.long	2369503088
	.long	1065803773
	.long	4130865093
	.long	1065745395
	.long	3344465584
	.long	1065687076
	.long	3803811842
	.long	1065628815
	.long	713978687
	.long	1065570613
	.long	2166438958
	.long	1065512468
	.long	3369253777
	.long	1065454381
	.long	3826935798
	.long	1065396352
	.long	1795984718
	.long	1065323547
	.long	1064675693
	.long	1065207720
	.long	182837332
	.long	1065092008
	.long	2466218622
	.long	1064976410
	.long	2643543973
	.long	1064860927
	.long	4036370876
	.long	1064745558
	.long	1379209261
	.long	1064630304
	.long	2294346567
	.long	1064515163
	.long	1522032624
	.long	1064400136
	.long	495642370
	.long	1064265805
	.long	3615182787
	.long	1064036203
	.long	1900236729
	.long	1063806828
	.long	2033862474
	.long	1063577678
	.long	2114800432
	.long	1063348753
	.long	494771388
	.long	1062984042
	.long	1967954106
	.long	1062527089
	.long	1170198565
	.long	1061933680
	.long	417502738
	.long	1060884213
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
	.long	368569247
	.long	3223701786
	.long	368569247
	.long	3223701786
	.long	368569247
	.long	3223701786
	.long	368569247
	.long	3223701786
	.long	368569247
	.long	3223701786
	.long	368569247
	.long	3223701786
	.long	368569247
	.long	3223701786
	.long	368569247
	.long	3223701786
	.long	1671522011
	.long	1075227560
	.long	1671522011
	.long	1075227560
	.long	1671522011
	.long	1075227560
	.long	1671522011
	.long	1075227560
	.long	1671522011
	.long	1075227560
	.long	1671522011
	.long	1075227560
	.long	1671522011
	.long	1075227560
	.long	1671522011
	.long	1075227560
	.long	1691548315
	.long	3221787401
	.long	1691548315
	.long	3221787401
	.long	1691548315
	.long	3221787401
	.long	1691548315
	.long	3221787401
	.long	1691548315
	.long	3221787401
	.long	1691548315
	.long	3221787401
	.long	1691548315
	.long	3221787401
	.long	1691548315
	.long	3221787401
	.long	3700771192
	.long	1073506818
	.long	3700771192
	.long	1073506818
	.long	3700771192
	.long	1073506818
	.long	3700771192
	.long	1073506818
	.long	3700771192
	.long	1073506818
	.long	3700771192
	.long	1073506818
	.long	3700771192
	.long	1073506818
	.long	3700771192
	.long	1073506818
	.long	3698831637
	.long	3220339442
	.long	3698831637
	.long	3220339442
	.long	3698831637
	.long	3220339442
	.long	3698831637
	.long	3220339442
	.long	3698831637
	.long	3220339442
	.long	3698831637
	.long	3220339442
	.long	3698831637
	.long	3220339442
	.long	3698831637
	.long	3220339442
	.long	3207479564
	.long	1062894188
	.long	3207479564
	.long	1062894188
	.long	3207479564
	.long	1062894188
	.long	3207479564
	.long	1062894188
	.long	3207479564
	.long	1062894188
	.long	3207479564
	.long	1062894188
	.long	3207479564
	.long	1062894188
	.long	3207479564
	.long	1062894188
	.long	589282582
	.long	1068907621
	.long	589282582
	.long	1068907621
	.long	589282582
	.long	1068907621
	.long	589282582
	.long	1068907621
	.long	589282582
	.long	1068907621
	.long	589282582
	.long	1068907621
	.long	589282582
	.long	1068907621
	.long	589282582
	.long	1068907621
	.long	1325131787
	.long	3216755581
	.long	1325131787
	.long	3216755581
	.long	1325131787
	.long	3216755581
	.long	1325131787
	.long	3216755581
	.long	1325131787
	.long	3216755581
	.long	1325131787
	.long	3216755581
	.long	1325131787
	.long	3216755581
	.long	1325131787
	.long	3216755581
	.long	1668232222
	.long	1069713319
	.long	1668232222
	.long	1069713319
	.long	1668232222
	.long	1069713319
	.long	1668232222
	.long	1069713319
	.long	1668232222
	.long	1069713319
	.long	1668232222
	.long	1069713319
	.long	1668232222
	.long	1069713319
	.long	1668232222
	.long	1069713319
	.long	354868790
	.long	3217804155
	.long	354868790
	.long	3217804155
	.long	354868790
	.long	3217804155
	.long	354868790
	.long	3217804155
	.long	354868790
	.long	3217804155
	.long	354868790
	.long	3217804155
	.long	354868790
	.long	3217804155
	.long	354868790
	.long	3217804155
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	354870542
	.long	1071369083
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	4294967295
	.long	1048575
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1062207488
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	0
	.long	1048576
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
	.long	4294967295
	.long	2146435071
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
	.long	1071366144
	.long	0
	.long	1071366144
	.long	0
	.long	1071366144
	.long	0
	.long	1071366144
	.long	0
	.long	1071366144
	.long	0
	.long	1071366144
	.long	0
	.long	1071366144
	.long	0
	.long	1071366144
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
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	1352597504
	.long	1070810131
	.long	3296460800
	.long	1031700412
	.long	3296460800
	.long	1031700412
	.long	3296460800
	.long	1031700412
	.long	3296460800
	.long	1031700412
	.long	3296460800
	.long	1031700412
	.long	3296460800
	.long	1031700412
	.long	3296460800
	.long	1031700412
	.long	3296460800
	.long	1031700412
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1082564608
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083176960
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
	.long	0
	.long	1083174912
	.long	1352628735
	.long	1070810131
	.long	1352628735
	.long	1070810131
	.long	1352628735
	.long	1070810131
	.long	1352628735
	.long	1070810131
	.long	1352628735
	.long	1070810131
	.long	1352628735
	.long	1070810131
	.long	1352628735
	.long	1070810131
	.long	1352628735
	.long	1070810131
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
	.type	__svml_dlog10_ha_data_internal,@object
	.size	__svml_dlog10_ha_data_internal,10048
	.align 32
_vmldLgHATab:
	.long	0
	.long	1071366144
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1071337728
	.long	184549376
	.long	1065092008
	.long	2099961998
	.long	3178897324
	.long	0
	.long	1071309312
	.long	931135488
	.long	1066155272
	.long	2365712557
	.long	3178155773
	.long	0
	.long	1071280896
	.long	603979776
	.long	1066752445
	.long	709057215
	.long	1031474920
	.long	0
	.long	1071252480
	.long	3437232128
	.long	1067234191
	.long	1515412199
	.long	3179085970
	.long	0
	.long	1071238272
	.long	1105723392
	.long	1067464226
	.long	153915826
	.long	3178000698
	.long	0
	.long	1071209856
	.long	3328442368
	.long	1067711223
	.long	3899912278
	.long	3177135692
	.long	0
	.long	1071181440
	.long	341835776
	.long	1067962480
	.long	2509208190
	.long	3176531222
	.long	0
	.long	1071167232
	.long	2884632576
	.long	1068089751
	.long	1030636902
	.long	1025224143
	.long	0
	.long	1071138816
	.long	3613917184
	.long	1068347678
	.long	3527163461
	.long	3177529532
	.long	0
	.long	1071124608
	.long	3549954048
	.long	1068478374
	.long	3498894081
	.long	3173000425
	.long	0
	.long	1071096192
	.long	1623785472
	.long	1068621140
	.long	2889825554
	.long	3176375375
	.long	0
	.long	1071081984
	.long	1085800448
	.long	1068688295
	.long	4015256301
	.long	3177184346
	.long	0
	.long	1071053568
	.long	3652976640
	.long	1068824490
	.long	3600693529
	.long	3175753877
	.long	0
	.long	1071039360
	.long	1592393728
	.long	1068893555
	.long	231073830
	.long	3177087939
	.long	0
	.long	1071025152
	.long	3459645440
	.long	1068963280
	.long	1740576090
	.long	1029619435
	.long	0
	.long	1070996736
	.long	3774611456
	.long	1069104765
	.long	3858552785
	.long	1028603845
	.long	0
	.long	1070982528
	.long	845086720
	.long	1069176552
	.long	3138879731
	.long	1029120443
	.long	0
	.long	1070968320
	.long	3513843712
	.long	1069249052
	.long	2107125367
	.long	1029044389
	.long	0
	.long	1070954112
	.long	434503680
	.long	1069322282
	.long	3827602229
	.long	1028932700
	.long	0
	.long	1070939904
	.long	3613851648
	.long	1069396254
	.long	1223751955
	.long	3176465139
	.long	0
	.long	1070911488
	.long	733741056
	.long	1069546492
	.long	1625232067
	.long	1029570781
	.long	0
	.long	1070897280
	.long	1511620608
	.long	1069585154
	.long	3044605139
	.long	1028090775
	.long	0
	.long	1070883072
	.long	1337196544
	.long	1069623706
	.long	2602639001
	.long	3175938675
	.long	0
	.long	1070868864
	.long	2572533760
	.long	1069662670
	.long	3067107955
	.long	1022933137
	.long	0
	.long	1070854656
	.long	559611904
	.long	1069702056
	.long	764145786
	.long	3174041535
	.long	0
	.long	1070840448
	.long	485818368
	.long	1069741872
	.long	2037567072
	.long	3175580956
	.long	0
	.long	1070826240
	.long	259604480
	.long	1069782128
	.long	4012068429
	.long	1027865895
	.long	0
	.long	1070812032
	.long	3454042112
	.long	1069822833
	.long	2867680007
	.long	3174202478
	.long	0
	.long	1070797824
	.long	2188754944
	.long	1069863999
	.long	2538655286
	.long	3175840981
	.long	0
	.long	1070783616
	.long	2965241856
	.long	1069905635
	.long	1338936972
	.long	3176093950
	.long	0
	.long	1070769408
	.long	966279168
	.long	1069947753
	.long	1774547674
	.long	3175051484
	.long	0
	.long	1070755200
	.long	1604042752
	.long	1069990363
	.long	2557470738
	.long	3174667448
	.long	0
	.long	1070740992
	.long	3417833472
	.long	1070033477
	.long	2268255117
	.long	3175678264
	.long	0
	.long	1070740992
	.long	3417833472
	.long	1070033477
	.long	2268255117
	.long	3175678264
	.long	0
	.long	1070726784
	.long	2451292160
	.long	1070077108
	.long	3757728941
	.long	1027943275
	.long	0
	.long	1070712576
	.long	929644544
	.long	1070121268
	.long	899045708
	.long	1027944939
	.long	0
	.long	1070698368
	.long	3057254400
	.long	1070165969
	.long	3880649376
	.long	3172972504
	.long	0
	.long	1070684160
	.long	2231091200
	.long	1070211226
	.long	521319256
	.long	1027600177
	.long	0
	.long	1070684160
	.long	2231091200
	.long	1070211226
	.long	521319256
	.long	1027600177
	.long	0
	.long	1070669952
	.long	2620162048
	.long	1070257052
	.long	1385613369
	.long	3176104036
	.long	0
	.long	1070655744
	.long	2096726016
	.long	1070303462
	.long	3138305819
	.long	3173646777
	.long	0
	.long	1070641536
	.long	944717824
	.long	1070350471
	.long	1065120110
	.long	1027539054
	.long	0
	.long	1070641536
	.long	944717824
	.long	1070350471
	.long	1065120110
	.long	1027539054
	.long	0
	.long	1070627328
	.long	1985789952
	.long	1070398094
	.long	3635943864
	.long	3173136490
	.long	0
	.long	1070613120
	.long	2123825152
	.long	1070446348
	.long	1125219725
	.long	3175615738
	.long	0
	.long	1070598912
	.long	1078378496
	.long	1070495250
	.long	603852726
	.long	3174570526
	.long	0
	.long	1070598912
	.long	1078378496
	.long	1070495250
	.long	603852726
	.long	3174570526
	.long	0
	.long	1070573312
	.long	1537933312
	.long	1070544817
	.long	998069198
	.long	1026662908
	.long	0
	.long	1070544896
	.long	733773824
	.long	1070595068
	.long	4061058002
	.long	3174036009
	.long	0
	.long	1070544896
	.long	733773824
	.long	1070595068
	.long	4061058002
	.long	3174036009
	.long	0
	.long	1070516480
	.long	3897544704
	.long	1070621058
	.long	951856294
	.long	1026731877
	.long	0
	.long	1070516480
	.long	3897544704
	.long	1070621058
	.long	951856294
	.long	1026731877
	.long	0
	.long	1070488064
	.long	493535232
	.long	1070646897
	.long	3852369308
	.long	3173264746
	.long	0
	.long	1070459648
	.long	463249408
	.long	1070673107
	.long	2853152111
	.long	3174564937
	.long	0
	.long	1070459648
	.long	463249408
	.long	1070673107
	.long	2853152111
	.long	3174564937
	.long	0
	.long	1070431232
	.long	3186585600
	.long	1070699699
	.long	1874718356
	.long	3174139933
	.long	0
	.long	1070431232
	.long	3186585600
	.long	1070699699
	.long	1874718356
	.long	3174139933
	.long	0
	.long	1070402816
	.long	1525858304
	.long	1070726686
	.long	3039843523
	.long	1024724665
	.long	0
	.long	1070402816
	.long	1525858304
	.long	1070726686
	.long	3039843523
	.long	1024724665
	.long	0
	.long	1070374400
	.long	3425300480
	.long	1070754078
	.long	1303046649
	.long	1022401701
	.long	0
	.long	1070374400
	.long	3425300480
	.long	1070754078
	.long	1303046649
	.long	1022401701
	.long	0
	.long	1070345984
	.long	1980465152
	.long	1070781889
	.long	3188656319
	.long	1027271390
	.long	0
	.long	1070345984
	.long	1980465152
	.long	1070781889
	.long	3188656319
	.long	1027271390
	.long	0
	.long	1070317568
	.long	1352630272
	.long	1070810131
	.long	3090895658
	.long	3174564915
	.long	1352630272
	.long	1070810131
	.long	3090895658
	.long	3174564915
	.long	64
	.long	1120927744
	.long	0
	.long	1096810496
	.long	0
	.long	1064828928
	.long	0
	.long	1135607808
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	0
	.long	1071366144
	.long	3207479559
	.long	1062894188
	.long	3698831637
	.long	3220339442
	.long	3700832817
	.long	1073506818
	.long	1691624569
	.long	3221787401
	.long	2065628764
	.long	1075227551
	.long	1770847080
	.long	3223701774
	.long	3786517112
	.long	1077250450
	.long	1316351650
	.long	3225793313
	.type	_vmldLgHATab,@object
	.size	_vmldLgHATab,1696
	.align 16
.L_2il0floatpacket.28:
	.long	0x00000000,0x43380000,0x00000000,0x43380000
	.type	.L_2il0floatpacket.28,@object
	.size	.L_2il0floatpacket.28,16
	.align 16
.L_2il0floatpacket.101:
	.long	0x00000000,0x80000000,0x00000000,0x00000000
	.type	.L_2il0floatpacket.101,@object
	.size	.L_2il0floatpacket.101,16
	.align 8
.L_2il0floatpacket.102:
	.long	0x00000000,0x3ff00000
	.type	.L_2il0floatpacket.102,@object
	.size	.L_2il0floatpacket.102,8
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
